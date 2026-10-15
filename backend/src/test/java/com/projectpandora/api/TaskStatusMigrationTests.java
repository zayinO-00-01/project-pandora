package com.projectpandora.api;

import static org.assertj.core.api.Assertions.assertThat;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.List;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;

class TaskStatusMigrationTests {
    @Test void v5UpgradeKeepsOriginalDataAndAllowsUnknownPercentage() throws Exception {
        String url="jdbc:h2:mem:status_upgrade_"+System.nanoTime()+";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";
        Flyway.configure().dataSource(url,"sa","").target("5").load().migrate();
        var before=new ArrayList<List<String>>();
        try(var c=DriverManager.getConnection(url,"sa","");var s=c.createStatement()) {
            for(int value:new int[]{0,40,100})s.executeUpdate("INSERT INTO dispatch_tasks(title,detail,priority,due_at,progress,progress_note,created_by,assignee_id) SELECT '旧任务"+value+"','保留详情','high',TIMESTAMP '2026-10-16 10:30:00',"+value+",'原反馈"+value+"',l.id,u.id FROM users l,users u WHERE l.username='leader' AND u.username='staff'");
            try(var rows=s.executeQuery("SELECT id,title,detail,priority,due_at,progress,progress_note,created_by,assignee_id,created_at,updated_at FROM dispatch_tasks ORDER BY id")) {
                while(rows.next()) {var values=new ArrayList<String>();for(int i=1;i<=11;i++)values.add(rows.getString(i));before.add(values);}
            }
        }
        Flyway.configure().dataSource(url,"sa","").load().migrate();
        try(var c=DriverManager.getConnection(url,"sa","");var s=c.createStatement()) {
            try(var rows=s.executeQuery("SELECT id,title,detail,priority,due_at,progress,progress_note,created_by,assignee_id,created_at,updated_at,status FROM dispatch_tasks ORDER BY id")) {
                int index=0;
                while(rows.next()) {
                    var values=new ArrayList<String>();for(int i=1;i<=11;i++)values.add(rows.getString(i));
                    assertThat(values).isEqualTo(before.get(index));
                    assertThat(rows.getString(12)).isEqualTo(List.of("todo","doing","done").get(index++));
                }
                assertThat(index).isEqualTo(3);
            }
            s.executeUpdate("UPDATE dispatch_tasks SET status='doing',progress=NULL WHERE title='旧任务0'");
            try(var rows=s.executeQuery("SELECT status,progress FROM dispatch_tasks WHERE title='旧任务0'")) {
                assertThat(rows.next()).isTrue();assertThat(rows.getString(1)).isEqualTo("doing");assertThat(rows.getObject(2)).isNull();
            }
        }
        Flyway.configure().dataSource(url,"sa","").load().validate();
    }
}
