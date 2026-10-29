package com.projectpandora.api;

import static org.assertj.core.api.Assertions.assertThat;
import java.sql.DriverManager;
import java.sql.Connection;
import java.util.*;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;

class TaskApprovalMigrationTests {
    @Test void v9UpgradeApprovesLegacyTasksAndPreservesProgressAndAudit() throws Exception {
        String url="jdbc:h2:mem:approval_upgrade_"+System.nanoTime()+";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";
        Flyway.configure().dataSource(url,"sa","").target("9").load().migrate();
        try(var c=DriverManager.getConnection(url,"sa","");var s=c.createStatement()) {
            s.executeUpdate("INSERT INTO dispatch_tasks(title,detail,priority,status,progress,progress_note,created_by,assignee_id) SELECT 'legacy','details','high','doing',40,'original feedback',l.id,u.id FROM users l,users u WHERE l.username='leader' AND u.username='staff'");
        }
        Flyway.configure().dataSource(url,"sa","").load().migrate();
        try(var c=DriverManager.getConnection(url,"sa","");var s=c.createStatement();var rows=s.executeQuery("SELECT approval_status,approval_note,reviewer_id,reviewed_at,status,progress,progress_note FROM dispatch_tasks WHERE title='legacy'")) {
            assertThat(rows.next()).isTrue();assertThat(rows.getString(1)).isEqualTo("approved");assertThat(rows.getString(2)).isEmpty();
            assertThat(rows.getObject(3)).isNull();assertThat(rows.getObject(4)).isNull();assertThat(rows.getString(5)).isEqualTo("doing");
            assertThat(rows.getInt(6)).isEqualTo(40);assertThat(rows.getString(7)).isEqualTo("original feedback");
        }
        Flyway.configure().dataSource(url,"sa","").load().validate();
    }
    @Test void v10CloudUpgradePreservesEveryExistingColumnAndApprovalAudit() throws Exception {
        String url="jdbc:h2:mem:cloud_upgrade_"+System.nanoTime()+";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";
        Flyway.configure().dataSource(url,"sa","").target("10").load().migrate();
        Map<String,List<String>> columns=new LinkedHashMap<>();
        Map<String,List<List<String>>> before=new LinkedHashMap<>();
        try(var c=DriverManager.getConnection(url,"sa","");var s=c.createStatement()) {
            s.executeUpdate("UPDATE users SET display_name='cloud edited staff',phone='13911112222',disabled=TRUE WHERE username='staff'");
            s.executeUpdate("INSERT INTO work_logs(user_id,log_date,content,status,created_at,updated_at) SELECT id,'2026-10-21','cloud history','submitted','2026-10-21 01:02:03','2026-10-21 04:05:06' FROM users WHERE username='staff'");
            s.executeUpdate("INSERT INTO dispatch_tasks(title,detail,priority,status,progress,progress_note,created_by,assignee_id,approval_status,approval_note,reviewer_id,reviewed_at,created_at,updated_at) SELECT 'cloud selftask','original detail','high','doing',45,'original feedback',u.id,u.id,'approved','original approval',r.id,'2026-10-22 01:02:03.123456','2026-10-21 01:02:03','2026-10-22 04:05:06' FROM users u,users r WHERE u.username='staff' AND r.username='leader'");
            try(var tables=c.getMetaData().getTables(null,"public","%",new String[]{"TABLE"})) {
                while(tables.next()) {
                    String table=tables.getString("TABLE_NAME");
                    if(table.equals("flyway_schema_history")) continue;
                    var names=new ArrayList<String>();
                    try(var metadata=c.getMetaData().getColumns(null,"public",table,"%")) {
                        while(metadata.next()) names.add(metadata.getString("COLUMN_NAME"));
                    }
                    columns.put(table,names);before.put(table,snapshot(c,table,names));
                }
            }
        }
        assertThat(columns.keySet()).contains("users","work_logs","dispatch_tasks");
        Flyway.configure().dataSource(url,"sa","").load().migrate();
        try(var c=DriverManager.getConnection(url,"sa","");var s=c.createStatement()) {
            for(var entry:columns.entrySet()) assertThat(snapshot(c,entry.getKey(),entry.getValue())).as(entry.getKey()).isEqualTo(before.get(entry.getKey()));
            try(var rows=s.executeQuery("SELECT reviewed_at,lifecycle_reviewed_at,review_status FROM dispatch_tasks WHERE title='cloud selftask'")) {
                assertThat(rows.next()).isTrue();assertThat(rows.getTimestamp(1).toLocalDateTime().toString()).isEqualTo("2026-10-22T01:02:03.123456");
                assertThat(rows.getObject(2)).isNull();assertThat(rows.getString(3)).isEqualTo("none");
            }
        }
        Flyway.configure().dataSource(url,"sa","").load().validate();
    }
    private List<List<String>> snapshot(Connection c,String table,List<String> columns) throws Exception {
        var result=new ArrayList<List<String>>();
        String projection=columns.stream().map(name->"\""+name+"\"").collect(java.util.stream.Collectors.joining(","));
        try(var s=c.createStatement();var rows=s.executeQuery("SELECT "+projection+" FROM \""+table+"\" ORDER BY 1")) {
            while(rows.next()) {
                var row=new ArrayList<String>();
                for(int i=1;i<=columns.size();i++) row.add(rows.getString(i));
                result.add(row);
            }
        }
        return result;
    }
}
