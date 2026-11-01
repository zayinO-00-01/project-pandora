package com.projectpandora.api;

import java.sql.DriverManager;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class WorkflowNotesMigrationTests {
    @Test void v18UpgradePreservesExistingNotificationsAndTaskDataAndEnforcesPrivateNoteUniqueness() throws Exception {
        String url="jdbc:h2:mem:notes_upgrade_"+System.nanoTime()+";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";
        Flyway.configure().dataSource(url,"sa","").target("18").load().migrate();
        try(var c=DriverManager.getConnection(url,"sa","");var s=c.createStatement()) {
            s.executeUpdate("INSERT INTO dispatch_tasks(title,detail,priority,status,progress,progress_note,created_by,assignee_id) SELECT 'legacy notes task','original detail','high','doing',40,'original feedback',id,id FROM users WHERE username='staff'");
            s.executeUpdate("INSERT INTO notifications(user_id,type,title,body,task_id,read_at) SELECT id,'task','original notification','original body',(SELECT id FROM dispatch_tasks WHERE title='legacy notes task'),'2026-10-29 01:02:03' FROM users WHERE username='staff'");
        }
        Flyway.configure().dataSource(url,"sa","").load().migrate();
        try(var c=DriverManager.getConnection(url,"sa","");var s=c.createStatement()) {
            try(var rows=s.executeQuery("SELECT n.title,n.body,n.task_id,n.read_at,n.reference_id,t.detail,t.progress,t.progress_note FROM notifications n JOIN dispatch_tasks t ON t.id=n.task_id")) {
                assertThat(rows.next()).isTrue();assertThat(rows.getString(1)).isEqualTo("original notification");assertThat(rows.getString(2)).isEqualTo("original body");
                assertThat(rows.getObject(3)).isNotNull();assertThat(rows.getTimestamp(4).toLocalDateTime().toString()).isEqualTo("2026-10-29T01:02:03");
                assertThat(rows.getObject(5)).isNull();assertThat(rows.getString(6)).isEqualTo("original detail");assertThat(rows.getInt(7)).isEqualTo(40);assertThat(rows.getString(8)).isEqualTo("original feedback");
            }
            String insert="INSERT INTO task_notes(task_id,user_id,content,updated_at) SELECT t.id,u.id,'private',CURRENT_TIMESTAMP FROM dispatch_tasks t,users u WHERE t.title='legacy notes task' AND u.username='staff'";
            assertThat(s.executeUpdate(insert)).isEqualTo(1);
            assertThatThrownBy(()->s.executeUpdate(insert)).isInstanceOf(java.sql.SQLException.class);
            assertThat(s.executeUpdate(insert.replace("u.username='staff'","u.username='leader'"))).isEqualTo(1);
        }
        Flyway.configure().dataSource(url,"sa","").load().validate();
    }
}
