import java.io.*;
import java.nio.file.*;
import java.sql.*;
import java.time.*;
import java.util.*;
import org.flywaydb.core.Flyway;
import org.h2.tools.RunScript;

/** Standalone integration checks against migrated, disposable H2 databases only. */
public class ResetDemoDataTest {
    static int passed;
    static void check(boolean ok,String name) {if(!ok)throw new AssertionError(name);System.out.println("PASS "+(++passed)+": "+name);}
    static Connection database() throws Exception {
        String url="jdbc:h2:mem:reset_"+UUID.randomUUID()+";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";
        Flyway.configure().dataSource(url,"sa","").locations("classpath:db/migration","classpath:db/migration-h2").load().migrate();
        return DriverManager.getConnection(url,"sa","");
    }
    static Map<String,String> fullSnapshot(Connection c) throws Exception {
        var tables=new ArrayList<>(ResetDemoData.DELETE_ORDER);tables.addAll(ResetDemoData.PRESERVED);
        var result=ResetDemoData.snapshots(c,tables);
        try(var s=c.createStatement();var r=s.executeQuery("select * from users order by id")) {
            var rows=new StringBuilder();while(r.next())for(var column:ResetDemoData.PROFILE_COLUMNS)rows.append(column).append('=').append(r.getString(column)).append('|');
            result.put("all_profiles",rows.toString());
        }
        return result;
    }
    public static void main(String[] args) throws Exception {
        Path backups=Files.createTempDirectory("pandora-reset-test-");
        LocalDate day=LocalDate.of(2026,11,20);
        try(var c=database()) {
            long founder=ResetDemoData.insert(c,"insert into users(username,password_hash,role,display_name) values(?,?,?,?)","fixture_founder","keep-this-hash","FOUNDER","创始人");
            long leader=ResetDemoData.ids(c,"select id from users where username='leader'").get(0);
            ResetDemoData.update(c,"update users set manager_id=? where id=?",founder,leader);
            ResetDemoData.insert(c,"insert into users(username,password_hash,role,manager_id) values(?,?,?,?)","fixture_team","keep-team-hash","TEAM_LEAD",leader);
            ResetDemoData.insert(c,"insert into users(username,password_hash,role,disabled) values(?,?,?,?)","fixture_disabled","keep-disabled-hash","STAFF",true);
            long staff=ResetDemoData.ids(c,"select id from users where username='staff'").get(0);
            long label=ResetDemoData.insert(c,"insert into work_labels(name,color) values(?,?)","验收","#507070");
            ResetDemoData.insert(c,"insert into work_labels(name,color) values(?,?)","跟进","#705050");
            long company=ResetDemoData.insert(c,"insert into company_notices(title,content,publisher_id) values(?,?,?)","原公司事项","保留内容",founder);
            ResetDemoData.update(c,"insert into company_notice_notes(notice_id,user_id,content) values(?,?,?)",company,staff,"原公司事项私密备注");
            ResetDemoData.update(c,"insert into content_label_sets(kind,content_id,label_ids) values(?,?,?)","company",company,"["+label+"]");
            ResetDemoData.insert(c,"insert into view_requests(requester_id,target_id,reason,status,updated_at) values(?,?,?,?,?)",staff,founder,"保留查看授权","approved",Instant.now());
            // Populate every reset area before the measured reset, so restore checks are non-empty.
            ResetDemoData.execute(c,backups,day,true);
            String assignments=String.join(",",ResetDemoData.PROFILE_COLUMNS.stream().map(column->column+"=NULL").toList());
            ResetDemoData.update(c,"update users set "+assignments);
            long oldTask=ResetDemoData.ids(c,"select id from dispatch_tasks order by id").get(0);
            ResetDemoData.update(c,"insert into task_reviews(task_id,applicant_id,reviewer_id,operation,payload,status,base_version,created_at) values(?,?,?,?,?,?,?,?)",oldTask,staff,founder,"change","{}","pending",0L,Instant.now());
            try(var s=c.createStatement();var r=s.executeQuery("select id from personal_todos where owner_id="+staff+" order by id limit 1")) {
                r.next();ResetDemoData.update(c,"insert into personal_todo_requests(owner_id,request_id,todo_id) values(?,?,?)",staff,UUID.randomUUID().toString(),r.getString(1));
            }
            ResetDemoData.update(c,"update users set display_name=?,phone=?,id_card=?,address=? where id=?","原姓名","19900000001","000000199001010001","已有地址：O'Reilly\\资料",staff);
            ResetDemoData.update(c,"insert into work_logs(user_id,log_date,content,status) values(?,?,?,?)",staff,day.minusDays(20),"原日志 O'Reilly\\资料\n中文","submitted");
            ResetDemoData.update(c,"insert into notifications(user_id,type,title,body) values(?,?,?,?)",staff,"view_request_approved","保留通知","查看授权仍有效");
            var original=fullSnapshot(c);
            var preserved=ResetDemoData.snapshots(c,ResetDemoData.PRESERVED);
            ResetDemoData.execute(c,null,day,false);
            check(original.equals(fullSnapshot(c)),"preview changes no rows");
            var result=ResetDemoData.execute(c,backups,day,true);
            check(result.tasks()==result.members()*12 && result.logs()==result.members()*7,"apply exact per-member counts");
            check(preserved.equals(ResetDemoData.snapshots(c,ResetDemoData.PRESERVED)),"accounts, hashes, organization and retained tables unchanged");
            try(var s=c.createStatement();var r=s.executeQuery("select * from users order by id")) {
                while(r.next())for(var column:ResetDemoData.PROFILE_COLUMNS)check(r.getString(column)!=null&&!r.getString(column).isBlank(),"profile "+r.getLong("id")+" "+column+" populated");
            }
            try(var q=c.prepareStatement("select display_name,phone,id_card,address from users where id=?")) {
                q.setLong(1,staff);try(var r=q.executeQuery()){r.next();check(r.getString(1).equals("原姓名")&&r.getString(2).equals("19900000001")&&r.getString(3).equals("000000199001010001")&&r.getString(4).equals("已有地址：O'Reilly\\资料"),"existing profile values retained");}
            }
            try(var s=c.createStatement();var r=s.executeQuery("select count(*) from dispatch_tasks where approval_status <> 'rejected'")){r.next();check(ResetDemoData.count(c,"notifications")==r.getLong(1),"task notifications generated (rejected tasks have none)");}
            try(var s=c.createStatement();var r=s.executeQuery("select count(*) from notifications where task_id is null")){r.next();check(r.getInt(1)==1,"unrelated notification retained");}
            try(var s=c.createStatement();var r=s.executeQuery("select count(*) from users where phone='19900000001'")){r.next();check(r.getInt(1)==1,"generated phones avoid existing unique values");}
            check(ResetDemoData.count(c,"personal_todo_labels")==result.todos(),"generated todo labels reference existing dictionary");
            try(var s=c.createStatement();var r=s.executeQuery("select label_ids from content_label_sets where kind='company' and content_id="+company)){check(r.next()&&r.getString(1).equals("["+label+"]"),"company content labels retained");}
            try(var s=c.createStatement();var r=s.executeQuery("select count(*) from content_label_sets where kind='task' and label_ids like '%,%'")){r.next();check(r.getInt(1)==result.tasks(),"generated task multi-label sets are populated");}
            try(var reader=Files.newBufferedReader(result.backup())){RunScript.execute(c,reader);}
            check(original.equals(fullSnapshot(c)),"backup restores all affected rows and original profile fields");
            ResetDemoData.execute(c,backups,day,true);
            var once=ResetDemoData.snapshots(c,List.of("users"));
            var again=ResetDemoData.execute(c,backups,day,true);
            check(again.tasks()==result.tasks()&&once.equals(ResetDemoData.snapshots(c,List.of("users"))),"repeated reset does not accumulate rows or change account columns");
            var beforeFailure=fullSnapshot(c);
            ResetDemoData.update(c,"alter table work_logs add constraint force_demo_failure check (content not like '%【%') nocheck");
            boolean failed=false;
            try{ResetDemoData.execute(c,backups,day,true);}catch(SQLException expected){failed=true;}
            check(failed&&beforeFailure.equals(fullSnapshot(c)),"failure after deletes rolls back the whole transaction");
        }
        try(var c=database()) {
            ResetDemoData.update(c,"update users set disabled=true");
            var before=fullSnapshot(c);boolean failed=false;
            try{ResetDemoData.execute(c,backups,day,true);}catch(IllegalStateException expected){failed=true;}
            check(failed&&before.equals(fullSnapshot(c)),"no active business members refuses reset without deletion");
        }
        try(var c=database()) {
            ResetDemoData.update(c,"delete from flyway_schema_history where version='26'");
            var before=fullSnapshot(c);boolean failed=false;
            try{ResetDemoData.execute(c,backups,day,true);}catch(IllegalStateException expected){failed=true;}
            check(failed&&before.equals(fullSnapshot(c)),"outdated schema refuses reset without deletion");
        }
        System.out.println("ALL "+passed+" CHECKS PASSED; disposable backups: "+backups);
    }
}
