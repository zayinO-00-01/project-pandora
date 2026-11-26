import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.sql.*;
import java.time.*;
import java.util.*;

/** Explicit, offline demo-data reset. Never called by application startup or deployment. */
public class ResetDemoData {
    static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");
    static final List<String> DELETE_ORDER = List.of("task_notes", "task_reviews", "task_keywords", "task_events",
            "notifications", "personal_todo_labels", "personal_todo_requests", "personal_todos", "personal_priorities",
            "personal_priority_lists", "content_label_sets", "work_logs", "task_assignees", "dispatch_tasks");
    static final List<String> PROFILE_COLUMNS = List.of("display_name", "phone", "id_card", "birth_date", "gender",
            "hire_date", "emergency_contact", "emergency_phone", "address", "avatar");
    static final List<String> PRESERVED = List.of("users", "work_labels", "company_notices", "company_notice_notes",
            "view_requests", "company_notice_publish_lock", "flyway_schema_history");
    record Member(long id, String name, String role, Long manager) {}
    record Result(int members, int tasks, int logs, int todos, int priorities, Path backup) {}
    public static void main(String[] args) throws Exception {
        Set<String> flags = new HashSet<>(); Map<String,String> values = new HashMap<>();
        for (int i=0;i<args.length;i++) {
            if (Set.of("--apply", "--preview").contains(args[i])) flags.add(args[i]);
            else if (Set.of("--confirm", "--backup-dir").contains(args[i]) && i+1<args.length) values.put(args[i],args[++i]);
            else throw new IllegalArgumentException("Use --preview OR --apply --confirm RESET-DEMO-DATA --backup-dir <private-directory>");
        }
        boolean apply=flags.contains("--apply");
        if (flags.contains("--preview") && apply) throw new IllegalArgumentException("Choose preview or apply, not both");
        if (apply && (!"RESET-DEMO-DATA".equals(values.get("--confirm")) || !values.containsKey("--backup-dir")))
            throw new IllegalArgumentException("Apply requires exact confirmation and a backup directory");
        String url=System.getenv("PANDORA_DEMO_JDBC_URL");
        if (url==null || !(url.startsWith("jdbc:h2:") || url.startsWith("jdbc:mysql:")))
            throw new IllegalArgumentException("Set PANDORA_DEMO_JDBC_URL explicitly (H2 or MySQL)");
        if (url.toLowerCase(Locale.ROOT).matches(".*(?:password|user)=.*"))
            throw new IllegalArgumentException("Keep credentials in environment variables, not the JDBC URL");
        if(url.startsWith("jdbc:h2:")) {
            String h2Path=url.substring("jdbc:h2:".length()).split(";",2)[0];
            if(!h2Path.startsWith("file:") || !Path.of(h2Path.substring("file:".length())).isAbsolute())
                throw new IllegalArgumentException("H2 reset CLI requires jdbc:h2:file: with an absolute existing database path");
            if(!url.toUpperCase(Locale.ROOT).contains("IFEXISTS=TRUE"))url+=";IFEXISTS=TRUE";
        }
        String user=Objects.requireNonNullElse(System.getenv("PANDORA_DEMO_JDBC_USER"),"sa");
        String password=Objects.requireNonNullElse(System.getenv("PANDORA_DEMO_JDBC_PASSWORD"),"");
        try (var c=DriverManager.getConnection(url,user,password)) {
            execute(c,apply?Path.of(values.get("--backup-dir")):null,LocalDate.now(ZONE),apply);
        }
    }
    static Result execute(Connection c,Path backupDir,LocalDate day,boolean apply) throws Exception {
        boolean originalAuto=c.getAutoCommit(); int originalIsolation=c.getTransactionIsolation();
        c.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE); c.setAutoCommit(false);
        Path backup=null;
        try {
            verifySchema(c);
            List<Member> members=members(c,apply);
            if (members.isEmpty()) throw new IllegalStateException("No active business members; nothing was cleared");
            if (members.size()>200) throw new IllegalStateException("More than 200 business members; review scope before resetting");
            var counts=new LinkedHashMap<String,Long>();
            for (var table:DELETE_ORDER) counts.put(table,count(c,table));
            System.out.println("MODE="+(apply?"APPLY":"PREVIEW")+" DATE="+day+" DATABASE="+c.getMetaData().getDatabaseProductName());
            System.out.println("EXISTING_AFFECTED_ROWS="+counts);
            System.out.println("PLAN: active business members="+members.size()+"; tasks="+members.size()*12+"; logs="+members.size()*7+"; todos="+members.size()*8+"; priorities="+members.size()*5);
            System.out.println("PROFILES: fill blank fields for all accounts; preserve existing values and login/organization columns");
            if (!apply) { c.rollback(); return new Result(members.size(),0,0,0,0,null); }
            Map<String,String> before=snapshots(c,PRESERVED);
            // A complete restore script is durably written before the first DELETE.
            backup=backup(c,backupDir);
            fillProfiles(c,day);
            for (var table:DELETE_ORDER) update(c,"DELETE FROM "+quote(c,table)+where(table));
            var labels=ids(c,"select id from work_labels where active=true order by id");
            for (var member:members) seedMember(c,member,members,labels,day);
            if (!before.equals(snapshots(c,PRESERVED))) throw new IllegalStateException("Preserved data changed; transaction rolled back");
            verifyCounts(c,members);
            c.commit();
            System.out.println("COMMITTED: blank profile fields filled; account IDs, login names, password hashes, organization, labels, company notices and view grants unchanged");
            System.out.println("BACKUP="+backup.toAbsolutePath());
            return new Result(members.size(),members.size()*12,members.size()*7,members.size()*8,members.size()*5,backup);
        } catch (Exception e) {
            c.rollback(); System.err.println("ROLLED BACK: no reset was committed"); throw e;
        } finally { c.setAutoCommit(originalAuto); c.setTransactionIsolation(originalIsolation); }
    }
    static String where(String table) {
        return switch(table) {
            case "notifications" -> " WHERE task_id IS NOT NULL";
            case "content_label_sets" -> " WHERE kind IN ('task','log','personal')";
            default -> "";
        };
    }
    static void verifySchema(Connection c) throws Exception {
        try(var s=c.createStatement();var r=s.executeQuery("select version,success from flyway_schema_history where version is not null")) {
            boolean latest=false;
            while(r.next()) { if(!r.getBoolean(2))throw new IllegalStateException("Failed migration exists"); if("28".equals(r.getString(1)))latest=true;
                if(Double.parseDouble(r.getString(1).replace('_','.'))>29)throw new IllegalStateException("Schema newer than V29; review this tool before use"); }
            if(!latest)throw new IllegalStateException("Deploy and migrate through V28 before using this tool");
        }
        for(var table:DELETE_ORDER) count(c,table);
        if(c.getMetaData().getDatabaseProductName().equalsIgnoreCase("MySQL")) {
            var tables=new HashSet<>(DELETE_ORDER);tables.add("users");
            try(var s=c.createStatement();var r=s.executeQuery("select table_name,engine from information_schema.tables where table_schema=database()")) {
                while(r.next())if(tables.remove(r.getString(1)) && !"InnoDB".equalsIgnoreCase(r.getString(2)))
                    throw new IllegalStateException("Reset requires InnoDB transactional tables");
            }
            if(!tables.isEmpty())throw new IllegalStateException("Required transactional table missing");
        }
        try(var s=c.createStatement()) {
            s.executeQuery("select start_at,approval_status,version from dispatch_tasks where 1=0").close();
            s.executeQuery("select end_date,revision from work_logs where 1=0").close();
        }
    }
    static List<Member> members(Connection c,boolean lock) throws Exception {
        var result=new ArrayList<Member>();
        try(var s=c.createStatement();var r=s.executeQuery("select id,username,role,manager_id,disabled from users order by id"+(lock?" for update":""))) {
            while(r.next()) if(!r.getBoolean(5) && !"ADMIN".equals(r.getString(3))) {
                Object manager=r.getObject(4);
                result.add(new Member(r.getLong(1),r.getString(2),r.getString(3),manager==null?null:((Number)manager).longValue()));
            }
        }
        return result;
    }
    static void seedMember(Connection c,Member m,List<Member> members,List<Long> labels,LocalDate day) throws Exception {
        String area=switch(m.role()) {case "FOUNDER"->"经营计划";case "DEPT_HEAD"->"部门交付";case "TEAM_LEAD"->"团队协作";default->"项目执行";};
        String[] titles={"梳理本周交付清单","完成客户需求核对","整理接口联调记录","跟进质量问题修复","提交阶段进展说明","复核测试用例边界","核对验收演示流程","整理下周工作计划","确认资料归档目录","评审任务拆分方案","补充历史工作记录","准备中期验收材料"};
        Member manager=members.stream().filter(x->Objects.equals(x.id(),m.manager()) && rank(x.role())>rank(m.role())).findFirst().orElse(null);
        Instant now=Instant.now();
        for(int i=0;i<12;i++) {
            boolean pending=manager!=null && (i==8 || i==9);
            boolean rejected=manager!=null && i==10;
            String approval=pending?"pending":rejected?"rejected":"approved";
            String state=pending||rejected?"todo":i%4==0?"done":i%4==1?"doing":"todo";
            Integer progress=state.equals("done")?Integer.valueOf(100):state.equals("doing")?Integer.valueOf(40+i):null;
            int dueOffset=i<3?i-3:i-2;
            Instant start=day.minusDays(i<3?7:1).atTime(9,0).atZone(ZONE).toInstant();
            Instant due=day.plusDays(dueOffset).atTime(18,0).atZone(ZONE).toInstant();
            Instant created=day.minusDays(8).atTime(8,0).atZone(ZONE).toInstant().plusSeconds(i);
            Long single=labels.isEmpty()?null:labels.get(i%labels.size());
            long task=insert(c,"insert into dispatch_tasks(title,detail,priority,due_at,start_at,status,progress,progress_note,created_by,assignee_id,created_at,updated_at,approval_status,approval_note,reviewer_id,reviewed_at,review_status,label_id,version) values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                    area+" · "+titles[i],"演示任务："+titles[i]+"。\n目标：明确交付范围，记录过程与结果。\n完成标准：资料可追溯、问题有结论，相关负责人确认。\n下一步：按计划检查进度，并在任务中补充反馈。",
                    i%3==0?"high":i%3==1?"normal":"low",due,start,state,progress,state.equals("todo")?"":"已完成资料梳理与初步核对，剩余问题按清单跟进。",m.id(),m.id(),created,now,approval,
                    rejected?"请补充验收标准后重新提交":pending?"":"演示数据中已完成初审",pending?null:manager==null?null:manager.id(),pending?null:created.plusSeconds(1800),"none",single,0L);
            update(c,"insert into task_assignees(task_id,user_id) values(?,?)",task,m.id());
            update(c,"insert into task_events(task_id,event_type,actor_id,note,created_at) values(?,?,?,?,?)",task,"created",m.id(),"演示数据：创建任务",created);
            if(pending) notify(c,manager.id(),task,"task_approval_pending","任务创建待审批",titles[i],now);
            else if("approved".equals(approval)) notify(c,m.id(),task,"task_assigned","演示任务已生效",titles[i],now);
            update(c,"insert into task_keywords(task_id,created_by,text) values(?,?,?)",task,m.id(),area);
            update(c,"insert into task_keywords(task_id,created_by,text) values(?,?,?)",task,m.id(),i%2==0?"验收":"跟进");
            saveLabels(c,"task",task,labels,i);
            if(i==5 || i==7) update(c,"insert into task_notes(task_id,user_id,content,updated_at) values(?,?,?,?)",task,m.id(),"本人备注：关注依赖资料到齐情况，确认后继续推进。",now);
        }
        for(int i=0;i<7;i++) {
            LocalDate date=day.minusDays(i); Instant created=date.atTime(9,0).atZone(ZONE).toInstant();
            if(created.isAfter(now))created=now.minusSeconds(60);
            String content="【"+area+"日报】\n今日完成："+titles[i]+"，同步核对相关需求和资料。\n工作过程：按清单检查输入、处理步骤和交付结果，整理需要协同的问题。\n问题与风险：个别资料仍需补充，已明确跟进责任人和计划时间。\n下一步："+titles[(i+1)%titles.length]+"，更新进度并复核验收标准。";
            long log=insert(c,"insert into work_logs(user_id,log_date,end_date,content,status,created_at,updated_at,label_id,revision) values(?,?,?,?,?,?,?,?,?)",
                    m.id(),date,date,content,i==0?"draft":"submitted",created,created,labels.isEmpty()?null:labels.get(i%labels.size()),0L);
            saveLabels(c,"log",log,labels,i);
        }
        for(int i=0;i<8;i++) {
            String id=UUID.randomUUID().toString();
            update(c,"insert into personal_todos(id,owner_id,title,todo_date,completed) values(?,?,?,?,?)",id,m.id(),titles[i],day.plusDays(i-2),i<2);
            if(!labels.isEmpty())update(c,"insert into personal_todo_labels(todo_id,label_id,sort_order) values(?,?,?)",id,labels.get(i%labels.size()),0);
        }
        update(c,"insert into personal_priority_lists(owner_id,revision) values(?,?)",m.id(),1L);
        for(int i=0;i<5;i++) {
            long id=insert(c,"insert into personal_priorities(owner_id,title,detail,sort_order) values(?,?,?,?)",m.id(),titles[i],"本人的重点事项，随工作进展调整排序并补充说明。",i);
            saveLabels(c,"personal",id,labels,i);
        }
    }
    static int rank(String role) {return switch(role){case "FOUNDER"->4;case "DEPT_HEAD"->3;case "TEAM_LEAD"->2;default->1;};}
    static void fillProfiles(Connection c,LocalDate day) throws Exception {
        Set<String> phones=new HashSet<>(),cards=new HashSet<>();
        try(var s=c.createStatement();var r=s.executeQuery("select phone,id_card from users")) {
            while(r.next()){phones.add(r.getString(1));cards.add(r.getString(2));}
        }
        int index=0;
        try(var s=c.createStatement();var r=s.executeQuery("select * from users order by id")) {
            while(r.next()) {
                int ordinal=++index; long id=r.getLong("id");
                LocalDate birth=day.minusYears(26+ordinal%15).withMonth(1+ordinal%12).withDayOfMonth(1+ordinal%20);
                String phone=unique(phones,"199",8),card=unique(cards,"000000"+birth.toString().replace("-",""),4);
                Map<String,Object> values=new LinkedHashMap<>();
                values.put("display_name","演示成员"+ordinal);values.put("phone",phone);values.put("id_card",card);
                values.put("birth_date",birth);values.put("gender",ordinal%2==0?"FEMALE":"MALE");
                values.put("hire_date",day.minusYears(1+ordinal%4).minusDays(ordinal%90));
                values.put("emergency_contact","演示联系人"+ordinal);values.put("emergency_phone",phone);
                values.put("address","演示资料（虚构） · 示例市协作路"+ordinal+"号");values.put("avatar",ordinal%2==0?"🌿":"🌤️");
                for(var entry:values.entrySet()) {
                    Object old=r.getObject(entry.getKey());
                    if(old!=null && (!(old instanceof String text)||!text.isBlank()))continue;
                    if(entry.getKey().equals("avatar") && ((r.getString("avatar_data")!=null && !r.getString("avatar_data").isBlank()) ||
                            (r.getString("avatar_image")!=null && !r.getString("avatar_image").isBlank())))continue;
                    update(c,"update users set "+entry.getKey()+"=? where id=?",entry.getValue(),id);
                }
            }
        }
    }
    static String unique(Set<String> used,String prefix,int digits) {
        for(int i=1;i<Integer.MAX_VALUE;i++) {
            String value=prefix+String.format(Locale.ROOT,"%0"+digits+"d",i);
            if(value.length()!=prefix.length()+digits)break;
            if(used.add(value))return value;
        }
        throw new IllegalStateException("No unused demo profile value available");
    }
    static void notify(Connection c,long owner,long task,String type,String title,String body,Instant now) throws Exception {
        update(c,"insert into notifications(user_id,type,title,body,task_id,created_at) values(?,?,?,?,?,?)",owner,type,title,body,task,now);
    }
    static void saveLabels(Connection c,String kind,long id,List<Long> labels,int offset) throws Exception {
        if(labels.isEmpty())return;
        var chosen=new LinkedHashSet<Long>(); chosen.add(labels.get(offset%labels.size()));
        if(labels.size()>1)chosen.add(labels.get((offset+1)%labels.size()));
        update(c,"insert into content_label_sets(kind,content_id,label_ids) values(?,?,?)",kind,id,chosen.toString());
    }
    static void verifyCounts(Connection c,List<Member> members) throws Exception {
        for(var m:members) for(var entry:Map.of("dispatch_tasks.assignee_id",12,"work_logs.user_id",7,"personal_todos.owner_id",8,"personal_priorities.owner_id",5).entrySet()) {
            var parts=entry.getKey().split("\\.");
            try(var q=c.prepareStatement("select count(*) from "+parts[0]+" where "+parts[1]+"=?")) {
                q.setLong(1,m.id());try(var r=q.executeQuery()) {r.next();if(r.getInt(1)!=entry.getValue())throw new IllegalStateException("Generated row count mismatch");}
            }
        }
        try(var s=c.createStatement();var r=s.executeQuery("select user_id,log_date,count(*) from work_logs group by user_id,log_date having count(*)>1")) {
            if(r.next())throw new IllegalStateException("Duplicate daily logs");
        }
    }
    static Path backup(Connection c,Path directory) throws Exception {
        Files.createDirectories(directory); privatePermissions(directory,true);
        Path file=directory.resolve("pandora-before-demo-reset-"+Instant.now().toString().replace(':','-')+"-"+UUID.randomUUID()+".sql");
        try(var writer=Files.newBufferedWriter(file,StandardCharsets.UTF_8,StandardOpenOption.CREATE_NEW)) {
            privatePermissions(file,false);
            writer.write("-- Pandora demo reset rollback data. Stop the application before restoring.\n-- Preserve this private file on the server; never upload it to GitHub.\n");
            writer.write(c.getMetaData().getDatabaseProductName().equalsIgnoreCase("MySQL")?"START TRANSACTION;\n":"SET AUTOCOMMIT FALSE;\n");
            try(var s=c.createStatement();var r=s.executeQuery("select * from users order by id")) {
                while(r.next()) {
                    var assignments=new ArrayList<String>();
                    for(var column:PROFILE_COLUMNS)assignments.add(quote(c,column)+"="+literal(c,r.getObject(column)));
                    writer.write("UPDATE "+quote(c,"users")+" SET "+String.join(",",assignments)+" WHERE "+quote(c,"id")+"="+r.getLong("id")+";\n");
                }
            }
            for(var table:DELETE_ORDER)writer.write("DELETE FROM "+quote(c,table)+where(table)+";\n");
            var insertOrder=new ArrayList<>(DELETE_ORDER);Collections.reverse(insertOrder);
            for(var table:insertOrder)try(var s=c.createStatement();var r=s.executeQuery("SELECT * FROM "+quote(c,table)+where(table))) {
                var meta=r.getMetaData();int n=meta.getColumnCount();var columns=new ArrayList<String>();
                for(int i=1;i<=n;i++)columns.add(quote(c,meta.getColumnName(i)));
                while(r.next()) {var values=new ArrayList<String>();for(int i=1;i<=n;i++)values.add(literal(c,r.getObject(i)));writer.write("INSERT INTO "+quote(c,table)+" ("+String.join(",",columns)+") VALUES ("+String.join(",",values)+");\n");}
            }
            writer.write("COMMIT;\n");
            if(!c.getMetaData().getDatabaseProductName().equalsIgnoreCase("MySQL"))writer.write("SET AUTOCOMMIT TRUE;\n");
        }
        try(var channel=FileChannel.open(file,StandardOpenOption.WRITE)){channel.force(true);}
        Files.writeString(Path.of(file+".sha256"),HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(file)))+"\n",StandardCharsets.UTF_8,StandardOpenOption.CREATE_NEW);
        return file;
    }
    static void privatePermissions(Path path,boolean directory) throws Exception {
        if(Files.getFileStore(path).supportsFileAttributeView("posix"))Files.setPosixFilePermissions(path,java.nio.file.attribute.PosixFilePermissions.fromString(directory?"rwx------":"rw-------"));
    }
    static String literal(Connection c,Object value) throws Exception {
        if(value==null)return "NULL";
        if(value instanceof Number)return value.toString();
        if(value instanceof Boolean)return Boolean.TRUE.equals(value)?"TRUE":"FALSE";
        if(value instanceof byte[] bytes)return "X'"+HexFormat.of().formatHex(bytes)+"'";
        String text=value instanceof Clob clob?clob.getSubString(1,(int)clob.length()):value.toString();
        // Hex UTF-8 expressions are independent of MySQL NO_BACKSLASH_ESCAPES and client encoding.
        if(c.getMetaData().getDatabaseProductName().equalsIgnoreCase("MySQL"))
            return "CONVERT(X'"+HexFormat.of().formatHex(text.getBytes(StandardCharsets.UTF_8))+"' USING utf8mb4)";
        return "'"+text.replace("'","''")+"'";
    }
    static String quote(Connection c,String name) throws Exception {String q=c.getMetaData().getIdentifierQuoteString().trim();return q+name+q;}
    static long count(Connection c,String table) throws Exception {try(var s=c.createStatement();var r=s.executeQuery("select count(*) from "+quote(c,table)+where(table))){r.next();return r.getLong(1);}}
    static Map<String,String> snapshots(Connection c,List<String> tables) throws Exception {
        var result=new TreeMap<String,String>();
        for(var table:tables) {var rows=new ArrayList<String>();try(var s=c.createStatement();var r=s.executeQuery("select * from "+quote(c,table))){int n=r.getMetaData().getColumnCount();while(r.next()){var row=new StringBuilder();for(int i=1;i<=n;i++){if(table.equals("users")&&PROFILE_COLUMNS.contains(r.getMetaData().getColumnName(i).toLowerCase(Locale.ROOT)))continue;String text=r.getString(i);row.append(text==null?"-1:":text.length()+":"+text);}rows.add(row.toString());}}
            Collections.sort(rows);result.put(table,HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(rows.toString().getBytes(StandardCharsets.UTF_8))));}
        return result;
    }
    static List<Long> ids(Connection c,String sql) throws Exception {var ids=new ArrayList<Long>();try(var s=c.createStatement();var r=s.executeQuery(sql)){while(r.next())ids.add(r.getLong(1));}return ids;}
    static long insert(Connection c,String sql,Object... args) throws Exception {try(var s=c.prepareStatement(sql,Statement.RETURN_GENERATED_KEYS)){bind(s,args);s.executeUpdate();try(var r=s.getGeneratedKeys()){if(!r.next())throw new SQLException("No generated key");return r.getLong(1);}}}
    static void update(Connection c,String sql,Object... args) throws Exception {try(var s=c.prepareStatement(sql)){bind(s,args);s.executeUpdate();}}
    static void bind(PreparedStatement s,Object[] args) throws Exception {for(int i=0;i<args.length;i++){Object v=args[i];
        if(v instanceof Instant instant){s.setTimestamp(i+1,Timestamp.from(instant),Calendar.getInstance(TimeZone.getTimeZone(ZONE)));continue;}
        if(v instanceof LocalDate date)v=java.sql.Date.valueOf(date);s.setObject(i+1,v);}}
}
