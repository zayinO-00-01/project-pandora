package com.projectpandora.api;

import static org.assertj.core.api.Assertions.assertThat;
import java.sql.DriverManager;
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
}
