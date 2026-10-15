import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.zip.*;

/** Offline, H2-only upgrade of the known zayin V3 task database. Never repairs unknown history. */
class UpgradeDemoDatabase {
    static final String URL_OPTIONS = ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;IFEXISTS=TRUE;FILE_LOCK=FS";
    static Connection open(Path db) throws SQLException {
        return DriverManager.getConnection("jdbc:h2:file:" + db.toString().replace('\\','/') + URL_OPTIONS, "sa", "");
    }
    static int checksum(Path script) throws Exception {
        CRC32 crc = new CRC32();
        for (String line : Files.readAllLines(script, StandardCharsets.UTF_8))
            crc.update(line.replace("\uFEFF", "").getBytes(StandardCharsets.UTF_8));
        return (int) crc.getValue();
    }
    static String dataDigest(Connection c, String table) throws Exception {
        var digest = java.security.MessageDigest.getInstance("SHA-256");
        String order=table.equals("flyway_schema_history") ? "installed_rank" : "id";
        try (var s=c.createStatement(); var r=s.executeQuery("SELECT * FROM " + table + " ORDER BY " + order)) {
            int columns=r.getMetaData().getColumnCount();
            while(r.next()) for(int i=1;i<=columns;i++) {
                String value=r.getString(i);
                digest.update((value==null ? "-1:" : value.length()+":"+value).getBytes(StandardCharsets.UTF_8));
            }
        }
        return HexFormat.of().formatHex(digest.digest());
    }
    static boolean legacy(Connection c, Path migration) throws Exception {
        List<String> versions=new ArrayList<>(); boolean legacy=false;
        try(var s=c.createStatement();var r=s.executeQuery("SELECT version,script,checksum,success FROM flyway_schema_history WHERE version IS NOT NULL ORDER BY installed_rank")) {
            while(r.next()) {
                if(!r.getBoolean(4)) throw new IllegalStateException("数据库含失败迁移，请先人工检查；不会自动 repair。");
                String v=r.getString(1),script=r.getString(2); versions.add(v);
                if(v.equals("3") && script.equals("V3__dispatch_tasks.sql")) {
                    if(r.getInt(3)!=checksum(migration.resolve("V4__dispatch_tasks.sql")))
                        throw new IllegalStateException("旧 V3 tasks 校验值未知，停止升级。");
                    legacy=true;
                } else if(v.equals("1") || v.equals("2")) {
                    Path file=migration.resolve(script);
                    if(!Files.isRegularFile(file) || r.getInt(3)!=checksum(file))
                        throw new IllegalStateException("基础迁移校验值不匹配，停止升级。");
                }
            }
        }
        if(legacy && !versions.equals(List.of("1","2","3"))) throw new IllegalStateException("旧库迁移顺序未知，停止升级。");
        if(!legacy && versions.contains("3")) {
            try(var s=c.createStatement();var r=s.executeQuery("SELECT script,checksum FROM flyway_schema_history WHERE version='3'")) {
                r.next();
                if(!r.getString(1).equals("V3__roles_disabled.sql") || r.getInt(2)!=checksum(migration.resolve("V3__roles_disabled.sql")))
                    throw new IllegalStateException("无法识别 V3 迁移，停止升级。");
            }
        }
        return legacy;
    }
    static void upgrade(Connection c, Path migration) throws Exception {
            String roles=Files.readString(migration.resolve("V3__roles_disabled.sql"),StandardCharsets.UTF_8);
            for(String sql:roles.split(";")) if(!sql.isBlank()) try(var s=c.createStatement()){s.execute(sql);}
            c.setAutoCommit(false);
            try(var s=c.createStatement()) {
                if(s.executeUpdate("UPDATE flyway_schema_history SET version='4',script='V4__dispatch_tasks.sql' WHERE version='3' AND script='V3__dispatch_tasks.sql'")!=1)
                    throw new IllegalStateException("旧迁移记录已改变。");
            }
            try(var s=c.prepareStatement("INSERT INTO flyway_schema_history (installed_rank,version,description,type,script,checksum,installed_by,execution_time,success) VALUES (4,'3','roles disabled','SQL','V3__roles_disabled.sql',?,'SA',0,TRUE)")) {
                s.setInt(1,checksum(migration.resolve("V3__roles_disabled.sql")));s.executeUpdate();
            }
            c.commit();
    }
    public static void main(String[] args) throws Exception {
        Path db=Path.of(args[0]).toAbsolutePath().normalize(), migration=Path.of(args[1]).toAbsolutePath();
        if(!Files.exists(Path.of(db+".mv.db"))) {System.out.println("数据库尚不存在，首次启动由 Flyway 创建。");return;}
        String stamp=LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"))+"-"+UUID.randomUUID().toString().substring(0,8);
        Path backupZip=Path.of(db+".pre-five-role-"+stamp+".zip");
        Path staged=Path.of(db+".upgrade-"+stamp), stagedFile=Path.of(staged+".mv.db");
        Path backupFile=Path.of(db+".pre-five-role-"+stamp+".mv.db");
        Map<String,String> before=new LinkedHashMap<>();
        try(var c=open(db)) {
            if(!legacy(c,migration)) {System.out.println("数据库不是旧 zayin V3 tasks，无需兼容转换。");return;}
            try(var statement=c.createStatement()) {statement.execute("SET EXCLUSIVE 2");}
            for(String table:List.of("users","work_logs","dispatch_tasks","flyway_schema_history"))
                before.put(table,dataDigest(c,table));
            try(var statement=c.createStatement()){statement.execute("BACKUP TO '"+backupZip.toString().replace("'","''")+"'");}
        }
        try(var zip=new ZipInputStream(Files.newInputStream(backupZip))) {
            boolean found=false; ZipEntry entry;
            while((entry=zip.getNextEntry())!=null) if(entry.getName().endsWith(".mv.db")) {
                if(found) throw new IllegalStateException("备份包含多份数据库，停止升级。");
                Files.copy(zip,stagedFile);found=true;
            }
            if(!found) throw new IllegalStateException("备份未找到数据库，停止升级。");
        }
        Files.copy(stagedFile,backupFile);
        try(var c=open(staged)) {
            if(!legacy(c,migration)) throw new IllegalStateException("副本迁移历史不匹配。");
            upgrade(c,migration);
            for(String table:List.of("work_logs","dispatch_tasks"))
                if(!before.get(table).equals(dataDigest(c,table))) throw new IllegalStateException("副本数据校验不一致，原库不变。");
        }
        // Reacquire exclusive access and compare ALL original data/history before writing.
        // Apply in place while holding that access: never replace a database with an earlier snapshot.
        try(var c=open(db)) {
            try(var statement=c.createStatement()){statement.execute("SET EXCLUSIVE 2");}
            if(!legacy(c,migration)) throw new IllegalStateException("原库历史已变化，停止升级。");
            for(var entry:before.entrySet()) if(!entry.getValue().equals(dataDigest(c,entry.getKey())))
                throw new IllegalStateException("预演期间原库有写入，未升级原库；请停止服务后重试。");
            upgrade(c,migration);
            for(String table:List.of("work_logs","dispatch_tasks"))
                if(!before.get(table).equals(dataDigest(c,table))) throw new IllegalStateException("原库升级后校验异常，请停服务并恢复备份："+backupFile);
        }
        Files.delete(stagedFile);
        System.out.println("旧库升级完成；日志/任务内容校验一致。升级前备份："+backupFile);
        System.out.println("一致性快照："+backupZip);
    }
}
