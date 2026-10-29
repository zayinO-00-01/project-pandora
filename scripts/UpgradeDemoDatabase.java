import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.zip.*;

/** Offline H2 compatibility for the two published migration orders. No blanket Flyway repair. */
class UpgradeDemoDatabase {
    static final String URL_OPTIONS=";MODE=MySQL;DATABASE_TO_LOWER=TRUE;IFEXISTS=TRUE;FILE_LOCK=FS";
    // Flyway CRC32 values of the exact scripts in published zayin commit f91516e.
    // Renaming scripts and adding an empty final line do not change Flyway's line-based CRC32.
    static final Map<Integer,Integer> KNOWN=Map.of(1,-388269689,2,786655355,3,1267405111,4,-1473287969,5,-469620002,6,1292317567);
    static final Map<Integer,String> MAIN=Map.of(1,"V1__init.sql",2,"V2__seed_demo_users.sql",3,"V3__dispatch_tasks.sql",4,"V4__view_requests.sql",5,"V5__roles_disabled.sql",6,"V6__task_status_optional_progress.sql");
    static final Map<Integer,String> OLD=Map.of(1,"V1__init.sql",2,"V2__seed_demo_users.sql",3,"V3__roles_disabled.sql",4,"V4__dispatch_tasks.sql",5,"V5__view_requests.sql",6,"V6__task_status_optional_progress.sql");
    record Entry(int rank,int version,String description,String script,int checksum) {}
    record History(boolean old,List<Entry> entries) {}
    static Connection open(Path db) throws SQLException {return DriverManager.getConnection("jdbc:h2:file:"+db.toString().replace('\\','/')+URL_OPTIONS,"sa","");}
    static String quote(String s){return "\""+s.replace("\"","\"\"")+"\"";}
    static IllegalStateException refuse(String reason){return new IllegalStateException(reason+"; stopped without automatic Flyway repair. Preserve the database and backups.");}
    static int checksum(Path script) throws Exception {
        CRC32 crc=new CRC32();
        for(String line:Files.readAllLines(script,StandardCharsets.UTF_8))crc.update(line.replace("\uFEFF","").getBytes(StandardCharsets.UTF_8));
        return (int)crc.getValue();
    }
    static SortedSet<String> tables(Connection c) throws Exception {
        SortedSet<String> result=new TreeSet<>();
        try(var s=c.createStatement();var r=s.executeQuery("SELECT table_name FROM information_schema.tables WHERE table_schema='public' AND table_type='BASE TABLE'")){while(r.next())result.add(r.getString(1));}
        return result;
    }
    static String dataDigest(Connection c,String table) throws Exception {
        var digest=java.security.MessageDigest.getInstance("SHA-256");List<String> rows=new ArrayList<>();
        try(var s=c.createStatement();var r=s.executeQuery("SELECT * FROM "+quote(table))) {
            var metadata=r.getMetaData();int columns=metadata.getColumnCount();
            for(int i=1;i<=columns;i++)digest.update((metadata.getColumnName(i)+":"+metadata.getColumnTypeName(i)+";").getBytes(StandardCharsets.UTF_8));
            while(r.next()) {StringBuilder row=new StringBuilder();for(int i=1;i<=columns;i++){String value=r.getString(i);row.append(value==null?"-1:":value.length()+":"+value);}rows.add(row.toString());}
        }
        Collections.sort(rows);for(String row:rows)digest.update((row.length()+":"+row).getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(digest.digest());
    }
    static Map<String,String> snapshot(Connection c) throws Exception {Map<String,String> result=new TreeMap<>();for(String t:tables(c))result.put(t,dataDigest(c,t));return result;}
    static void preserved(Connection c,Map<String,String> before) throws Exception {
        for(var e:before.entrySet())if(!e.getKey().equals("flyway_schema_history")&&!e.getValue().equals(dataDigest(c,e.getKey())))throw refuse("Business rows changed: "+e.getKey());
        for(String table:tables(c))if(!before.containsKey(table)) {
            if(!Set.of("dispatch_tasks","view_requests").contains(table))throw refuse("Unexpected new table: "+table);
            try(var s=c.createStatement();var r=s.executeQuery("SELECT COUNT(*) FROM "+quote(table))){r.next();if(r.getLong(1)!=0)throw refuse("New table is not empty: "+table);}
        }
    }
    static History inspect(Connection c,Path migration) throws Exception {
        if(!tables(c).contains("flyway_schema_history")) {
            if(!tables(c).isEmpty())throw refuse("Existing tables have no Flyway history");
            return new History(false,List.of());
        }
        List<Entry> entries=new ArrayList<>();boolean old=false;
        try(var s=c.createStatement();var r=s.executeQuery("SELECT installed_rank,version,description,type,script,checksum,success FROM flyway_schema_history ORDER BY installed_rank")) {
            while(r.next()) {
                if(!r.getBoolean(7))throw refuse("Failed migration found");
                if(r.getString(2)==null) {
                    if(r.getInt(1)!=-1||!"TABLE".equals(r.getString(4))||!"".equals(r.getString(5))||r.getObject(6)!=null)throw refuse("Unknown unversioned history row");
                    continue;
                }
                if(!"SQL".equals(r.getString(4))||r.getObject(6)==null||r.getInt(1)<1)throw refuse("Unknown migration type or checksum");
                int version;try{version=Integer.parseInt(r.getString(2));}catch(NumberFormatException e){throw refuse("Unknown migration version");}
                if(!Integer.toString(version).equals(r.getString(2)))throw refuse("Noncanonical migration version");
                if(version==3&&"V3__roles_disabled.sql".equals(r.getString(5)))old=true;
                entries.add(new Entry(r.getInt(1),version,r.getString(3),r.getString(5),r.getInt(6)));
            }
        }
        Set<Integer> versions=new TreeSet<>();
        for(Entry e:entries) {
            if(!versions.add(e.version))throw refuse("Duplicate migration version");
            if(old&&e.version>6)throw refuse("Unknown extension of roles-first history");
            int target=old?switch(e.version){case 3->5;case 4->3;case 5->4;default->e.version;}:e.version;
            String expected=old?OLD.get(e.version):MAIN.get(target);
            if(target>6) {
                try(var paths=Files.list(migration)){var candidates=paths.filter(p->p.getFileName().toString().startsWith("V"+target+"__")&&p.toString().endsWith(".sql")).toList();if(candidates.size()!=1)throw refuse("Unknown migration version "+target);expected=candidates.get(0).getFileName().toString();}
            }
            Path file=expected==null?null:migration.resolve(old?MAIN.get(target):expected);
            if(expected==null||!expected.equals(e.script)||file==null||!Files.isRegularFile(file)||e.checksum!=checksum(file)||(KNOWN.containsKey(target)&&e.checksum!=KNOWN.get(target)))throw refuse("Unknown script/checksum for V"+e.version+" ("+e.script+")");
            String expectedDescription=expected.substring(expected.indexOf("__")+2,expected.length()-4).replace('_',' ');
            if(!expectedDescription.equals(e.description))throw refuse("Unknown description for V"+e.version);
        }
        for(int version=1;version<=versions.size();version++)if(!versions.contains(version))throw refuse("Noncontiguous migration history");
        return new History(old,List.copyOf(entries));
    }
    static void insert(Connection c,int rank,int version,Path migration) throws Exception {
        String script=MAIN.get(version),description=script.substring(script.indexOf("__")+2,script.length()-4).replace('_',' ');
        try(var s=c.prepareStatement("INSERT INTO flyway_schema_history(installed_rank,version,description,type,script,checksum,installed_by,execution_time,success) VALUES (?,?,?,'SQL',?,?,'SA',0,TRUE)")) {
            s.setInt(1,rank);s.setString(2,""+version);s.setString(3,description);s.setString(4,script);s.setInt(5,checksum(migration.resolve(script)));s.executeUpdate();
        }
    }
    static void convert(Connection c,Path migration) throws Exception {
        History history=inspect(c,migration);if(!history.old)throw refuse("History changed before conversion");
        Set<Integer> targets=new HashSet<>();int rank=0;
        for(Entry e:history.entries){targets.add(switch(e.version){case 3->5;case 4->3;case 5->4;default->e.version;});rank=Math.max(rank,e.rank);}
        // Early roles-first databases need empty task/approval tables before their role record can become V5.
        for(int missing:List.of(3,4))if(!targets.contains(missing)) {
            Path script=migration.resolve(MAIN.get(missing));if(checksum(script)!=KNOWN.get(missing))throw refuse("Changed compatibility SQL");
            for(String sql:Files.readString(script,StandardCharsets.UTF_8).split(";"))if(!sql.isBlank())try(var s=c.createStatement()){s.execute(sql);}
        }
        c.setAutoCommit(false);
        try {
            // Installed rank stays intact. Flyway orders versioned migrations by version, not installation rank.
            for(Entry e:history.entries)if(e.version>=3&&e.version<=5) {
                int target=switch(e.version){case 3->5;case 4->3;default->4;};String script=MAIN.get(target);
                try(var s=c.prepareStatement("UPDATE flyway_schema_history SET version=?,description=?,script=?,checksum=? WHERE installed_rank=? AND version=? AND script=? AND checksum=?")) {
                    s.setString(1,""+target);s.setString(2,script.substring(script.indexOf("__")+2,script.length()-4).replace('_',' '));s.setString(3,script);s.setInt(4,checksum(migration.resolve(script)));s.setInt(5,e.rank);s.setString(6,""+e.version);s.setString(7,e.script);s.setInt(8,e.checksum);if(s.executeUpdate()!=1)throw refuse("History changed during conversion");
                }
            }
            for(int missing:List.of(3,4))if(!targets.contains(missing))insert(c,++rank,missing,migration);
            c.commit();
        }catch(Exception e){c.rollback();throw e;}finally{c.setAutoCommit(true);}
        if(inspect(c,migration).old)throw refuse("Conversion did not produce main history");
    }
    static void protectSeedNames(Connection c) throws Exception {
        if(!tables(c).contains("users"))return;
        Map<String,Set<String>> baseline=Map.of("admin",Set.of("管理员","系统管理员"),"leader",Set.of("部门领导","部门老总"));
        try(var s=c.createStatement();var r=s.executeQuery("SELECT username,display_name FROM users WHERE username IN ('admin','leader')")) {
            while(r.next())if(r.getString(2)==null||!baseline.get(r.getString(1)).contains(r.getString(2)))throw refuse("Pending V5 would overwrite custom display_name for "+r.getString(1)+"; review migration explicitly before startup");
        }
    }
    static void exclusive(Connection c) throws Exception {try(var s=c.createStatement()){s.execute("SET EXCLUSIVE 1");}}
    public static void main(String[] args) throws Exception {
        if(args.length!=2)throw new IllegalArgumentException("Usage: UpgradeDemoDatabase <database path without .mv.db> <migration directory>");
        Path db=Path.of(args[0]).toAbsolutePath().normalize(),migration=Path.of(args[1]).toAbsolutePath().normalize();
        if(!Files.exists(Path.of(db+".mv.db"))){System.out.println("Database absent; Flyway will create it on first startup.");return;}
        String stamp=LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"))+"-"+UUID.randomUUID().toString().substring(0,8);
        Path backupZip=Path.of(db+".pre-history-"+stamp+".zip"),backupFile=Path.of(db+".pre-history-"+stamp+".mv.db"),staged=Path.of(db+".rehearsal-"+stamp),stagedFile=Path.of(staged+".mv.db");
        Map<String,String> before;boolean pendingRoles;
        try(var c=open(db)) {
            exclusive(c);History h=inspect(c,migration);
            pendingRoles=!h.old&&!h.entries.isEmpty()&&h.entries.stream().noneMatch(e->e.version==5);
            if(!h.old&&!pendingRoles){System.out.println("Verified main migration history; no compatibility writes.");return;}
            if(pendingRoles)protectSeedNames(c);
            before=snapshot(c);
            try(var s=c.createStatement()){s.execute("SCRIPT TO '"+backupZip.toString().replace("'","''")+"' COMPRESSION \"ZIP\"");}
        }
        // H2 BACKUP scans sibling .mv.db files and can collide with existing historical backups.
        // SCRIPT exports this connection's complete schema/data consistently; restore and verify it first.
        String restoreUrl="jdbc:h2:file:"+staged.toString().replace(java.io.File.separatorChar,'/')+URL_OPTIONS.replace(";IFEXISTS=TRUE","");
        try(var c=DriverManager.getConnection(restoreUrl,"sa","");var s=c.createStatement()) {
            s.execute("RUNSCRIPT FROM '"+backupZip.toString().replace("'","''")+"' COMPRESSION \"ZIP\"");
            if(!before.equals(snapshot(c)))throw refuse("Restored backup data/history differs from source");
        }
        Files.copy(stagedFile,backupFile);
        if(pendingRoles) {
            try(var c=open(db)){exclusive(c);if(!before.equals(snapshot(c)))throw refuse("Original early database changed while backing up");protectSeedNames(c);}
            Files.delete(stagedFile);
            System.out.println("Verified backup retained before standard Flyway V5: "+backupFile+" and logical export "+backupZip);
            System.out.println("Pending standard V5 explicitly maps LEADER to DEPT_HEAD and updates baseline admin/leader display names; custom seed names are refused.");
            return;
        }
        System.out.println("Known roles-first history: rehearsing exact V3/V4/V5 history conversion; no role, organization, or business row updates. Backups: "+backupFile+" and "+backupZip);
        try(var c=open(staged)){convert(c,migration);preserved(c,before);}
        try(var c=open(db)) {
            exclusive(c);if(!inspect(c,migration).old||!before.equals(snapshot(c)))throw refuse("Original database changed during rehearsal; stop the service and retry");
            convert(c,migration);preserved(c,before);
        }
        Files.delete(stagedFile);
        System.out.println("Compatibility conversion complete; every original business table digest matches. Backups retained: "+backupFile+" and "+backupZip);
    }
}