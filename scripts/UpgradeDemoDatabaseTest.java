import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.*;
import org.flywaydb.core.Flyway;

/** Offline fixtures only: never connects to backend/data. Run with Test-DatabaseCompatibility.ps1. */
class UpgradeDemoDatabaseTest {
    static Path repo, root, current;
    static String url(Path db) { return "jdbc:h2:file:"+db.toString().replace('\\','/')+";MODE=MySQL;DATABASE_TO_LOWER=TRUE"; }
    static void migrate(Path db, Path location) { Flyway.configure().dataSource(url(db),"sa","").locations("filesystem:"+location).load().migrate(); }
    static void check(boolean condition,String message) { if(!condition) throw new AssertionError(message); }
    static void sql(Path db,String sql) throws Exception { try(var c=DriverManager.getConnection(url(db),"sa","");var s=c.createStatement()){s.execute(sql);} }
    static Map<String,String> snapshot(Path db) throws Exception {
        Map<String,String> result=new TreeMap<>();
        try(var c=UpgradeDemoDatabase.open(db);var s=c.createStatement();var rows=s.executeQuery("SELECT table_name FROM information_schema.tables WHERE table_schema='public' AND table_type='BASE TABLE'")) {
            while(rows.next()) {String t=rows.getString(1);result.put(t,UpgradeDemoDatabase.dataDigest(c,t));}
        } return result;
    }
    static Path fixture(String name,boolean old,int limit) throws Exception {
        Path scripts=Files.createDirectories(root.resolve(name+"-scripts"));
        if(old) {
            for(String file:List.of("V1__init.sql","V2__seed_demo_users.sql","V3__roles_disabled.sql","V4__dispatch_tasks.sql","V5__view_requests.sql","V6__task_status_optional_progress.sql")) {
                if(Integer.parseInt(file.substring(1,2))>limit)continue;
                Process p=new ProcessBuilder("git","show","f91516e:backend/src/main/resources/db/migration/"+file).directory(repo.toFile()).start();
                byte[] bytes=p.getInputStream().readAllBytes();check(p.waitFor()==0,"git fixture extraction");Files.write(scripts.resolve(file),bytes);
            }
        } else try(var paths=Files.list(current)) {
            for(Path p:paths.toList()) if(p.getFileName().toString().matches("V[0-9]+__.*\\.sql") && Integer.parseInt(p.getFileName().toString().split("__")[0].substring(1))<=limit)Files.copy(p,scripts.resolve(p.getFileName()));
        }
        Path db=root.resolve(name);migrate(db,scripts);
        sql(db,"INSERT INTO work_logs(user_id,log_date,content,status) SELECT id,DATE '2026-10-01','keep log','draft' FROM users WHERE username='staff'");
        if(limit>=3 && (!old || limit>=4)) sql(db,"INSERT INTO dispatch_tasks(title,detail,progress_note,created_by,assignee_id) SELECT 'keep task','custom detail','custom note',a.id,b.id FROM users a,users b WHERE a.username='admin' AND b.username='staff'");
        if(limit>=4 && (!old || limit>=5)) sql(db,"INSERT INTO view_requests(requester_id,target_id,reason,status,updated_at) SELECT a.id,b.id,'keep approval','approved',CURRENT_TIMESTAMP FROM users a,users b WHERE a.username='leader' AND b.username='staff'");
        sql(db,"UPDATE users SET display_name='custom preserved name' WHERE username='admin'");
        return db;
    }
    static void run(Path db) throws Exception { UpgradeDemoDatabase.main(new String[]{db.toString(),current.toString()}); }
    static void runWrapper(Path db) throws Exception {
        String h2=Path.of(org.h2.Driver.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toString();
        Process p=new ProcessBuilder("powershell.exe","-NoProfile","-ExecutionPolicy","Bypass","-File",repo.resolve("scripts/Upgrade-DemoDatabase.ps1").toString(),"-DatabasePath",db.toString(),"-JavaHome",System.getProperty("java.home"),"-H2Jar",h2).inheritIO().start();
        check(p.waitFor()==0,"PowerShell wrapper failed");
    }
    static void rejectCopy(Path source,String name,String mutation) throws Exception {
        Path db=root.resolve(name);Files.copy(Path.of(source+".mv.db"),Path.of(db+".mv.db"));sql(db,mutation);
        var before=snapshot(db);boolean rejected=false;try{run(db);}catch(IllegalStateException e){rejected=true;}
        check(rejected,"Unknown "+name+" accepted");check(before.equals(snapshot(db)),"Unknown "+name+" changed");
    }
    static void unchangedBusiness(Map<String,String> before,Map<String,String> after) { before.forEach((k,v)->{if(!k.equals("flyway_schema_history"))check(v.equals(after.get(k)),"business rows changed: "+k);}); }
    static void backupSiblingAndNames() throws Exception {
        Path db=fixture("pandora",true,6);var before=snapshot(db);
        Path sibling=root.resolve("pandora.pre-five-role-existing.mv.db");Files.copy(Path.of(db+".mv.db"),sibling);
        byte[] siblingBytes=Files.readAllBytes(sibling);run(db);unchangedBusiness(before,snapshot(db));
        check(Arrays.equals(siblingBytes,Files.readAllBytes(sibling)),"existing backup modified");
        Path early=fixture("custom-main-v3",false,3);var custom=snapshot(early);boolean rejected=false;
        try{run(early);}catch(IllegalStateException e){rejected=true;}
        check(rejected,"V5 would overwrite custom admin display_name but upgrader accepted it");check(custom.equals(snapshot(early)),"custom early DB changed");
        sql(early,"UPDATE users SET display_name='管理员' WHERE username='admin'");
        before=snapshot(early);run(early);check(before.equals(snapshot(early)),"baseline early DB compatibility changed data");
        try(var paths=Files.list(root)){
            Path backup=paths.filter(p->p.getFileName().toString().startsWith("custom-main-v3.pre-history-")&&p.toString().endsWith(".mv.db")).findFirst().orElseThrow();
            String base=backup.toString();check(before.equals(snapshot(Path.of(base.substring(0,base.length()-6)))),"early baseline backup differs");
        }
        migrate(early,current);run(early);
        try(var c=UpgradeDemoDatabase.open(early);var s=c.createStatement();var r=s.executeQuery("SELECT display_name FROM users WHERE username='admin'")){r.next();check("系统管理员".equals(r.getString(1)),"baseline V5 migration did not run");}
        System.out.println("PASS: sibling backups unchanged, isolated consistent export restored, custom names refuse before Flyway, baseline early database backed up and migrates. Fixtures: "+root);
    }
    public static void main(String[] args) throws Exception {
        repo=Path.of(args[0]).toAbsolutePath();current=repo.resolve("backend/src/main/resources/db/migration");root=Files.createTempDirectory(repo.resolve(".tools"),"compat-fixtures-");
        if(args.length>1&&args[1].equals("safety-only")){backupSiblingAndNames();return;}
        Path old=fixture("zayin-v6",true,6);var before=snapshot(old);runWrapper(old);unchangedBusiness(before,snapshot(old));migrate(old,current);run(old);var done=snapshot(old);run(old);check(done.equals(snapshot(old)),"conversion not idempotent");
        try(var paths=Files.list(root)){check(paths.anyMatch(p->p.toString().endsWith(".zip")),"conversion backup missing");}
        try(var paths=Files.list(root)){
            Path backup=paths.filter(p->p.getFileName().toString().startsWith("zayin-v6.pre-history-")&&p.toString().endsWith(".mv.db")).findFirst().orElseThrow();
            String base=backup.toString();check(before.equals(snapshot(Path.of(base.substring(0,base.length()-6)))),"backup differs from original data/history");
        }
        Path partial=fixture("zayin-v4",true,4);before=snapshot(partial);run(partial);unchangedBusiness(before,snapshot(partial));migrate(partial,current);run(partial);
        Path early=fixture("zayin-v3",true,3);before=snapshot(early);run(early);unchangedBusiness(before,snapshot(early));migrate(early,current);run(early);
        Path main=fixture("main-v6",false,6);before=snapshot(main);run(main);check(before.equals(snapshot(main)),"main V6 changed");migrate(main,current);run(main);
        Path tasks=fixture("three-role-v3",false,3);before=snapshot(tasks);boolean protectedName=false;try{run(tasks);}catch(IllegalStateException e){protectedName=true;}check(protectedName,"custom name silently accepted before V5");check(before.equals(snapshot(tasks)),"refused three-role DB changed");sql(tasks,"UPDATE users SET display_name='管理员' WHERE username='admin'");before=snapshot(tasks);run(tasks);check(before.equals(snapshot(tasks)),"baseline three-role compatibility changed rows");migrate(tasks,current);run(tasks);
        Path unknown=fixture("unknown",false,6);sql(unknown,"UPDATE flyway_schema_history SET checksum=checksum+1 WHERE version='6'");before=snapshot(unknown);boolean rejected=false;try{run(unknown);}catch(IllegalStateException e){rejected=true;}check(rejected,"unknown checksum accepted");check(before.equals(snapshot(unknown)),"unknown history changed");
        rejectCopy(main,"unknown-description","UPDATE flyway_schema_history SET description='surprise' WHERE version='3'");
        rejectCopy(main,"unknown-version","UPDATE flyway_schema_history SET version='3.1' WHERE version='3'");
        rejectCopy(main,"noncanonical-version","UPDATE flyway_schema_history SET version='03' WHERE version='3'");
        rejectCopy(main,"unknown-type","UPDATE flyway_schema_history SET type='JDBC' WHERE version='3'");
        rejectCopy(main,"failed-history","UPDATE flyway_schema_history SET success=FALSE WHERE version='3'");
        Path fresh=fixture("main-full",false,99);before=snapshot(fresh);run(fresh);check(before.equals(snapshot(fresh)),"current full history changed");
        run(root.resolve("nonexistent"));check(!Files.exists(root.resolve("nonexistent.mv.db")),"fresh path created DB");
        System.out.println("PASS: PowerShell wrapper, both histories, partial V3/V4 roles, three-role tasks, full Flyway migration, all business rows, backup contents, idempotence, unknown checksum/description/version/type/failed history refusal, absent DB. Fixtures: "+root);
    }
}