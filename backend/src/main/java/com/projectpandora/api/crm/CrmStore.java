package com.projectpandora.api.crm;

import com.projectpandora.api.common.ApiException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.springframework.jdbc.core.DataClassRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Component;

@Component
class CrmStore {
    static final String ZONE = "Asia/Shanghai";
    static final long MAX_CENTS = 100_000_000_00L;
    private final JdbcTemplate jdbc;

    CrmStore(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    // All CRM mutations acquire this row first, including imports, cash, and card use.
    // That makes check-and-write rules atomic on both H2 and MySQL.
    void lock() { jdbc.queryForObject("SELECT id FROM crm_write_guard WHERE id=1 FOR UPDATE", Integer.class); }

    <T> List<T> rows(Class<T> type, String sql, Object... params) {
        return jdbc.query(sql, DataClassRowMapper.newInstance(type), params);
    }

    <T> Optional<T> one(Class<T> type, String sql, Object... params) {
        return rows(type, sql, params).stream().findFirst();
    }

    <T> List<T> values(Class<T> type, String sql, Object... params) {
        return jdbc.queryForList(sql, type, params);
    }

    long number(String sql, Object... params) { return jdbc.queryForObject(sql, Long.class, params); }
    int write(String sql, Object... params) { return jdbc.update(sql, params); }

    long insert(String sql, Object... params) {
        var keys = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            var statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            for (int i = 0; i < params.length; i++) statement.setObject(i + 1, params[i]);
            return statement;
        }, keys);
        return keys.getKey().longValue();
    }

    void audit(long actor, String type, String detail) {
        write("INSERT INTO crm_audit (actor_id,event_type,detail,recorded_at) VALUES (?,?,?,?)",
            actor, type, detail, now());
    }

    static LocalDate today() { return LocalDate.now(ZoneId.of(ZONE)); }
    static LocalDateTime now() { return LocalDateTime.now(ZoneId.of(ZONE)).withNano(0); }
    static ApiException missing() { return new ApiException(404, "记录不存在或无权查看"); }
    static ApiException denied() { return new ApiException(403, "没有此项业务权限"); }
    static ApiException bad(String message) { return new ApiException(400, message); }
    static ApiException conflict(String message) { return new ApiException(409, message); }
    static String optional(String value, int max, String label) {
        String text = value == null ? "" : value.strip();
        if (text.length() > max) throw bad(label + "最多 " + max + " 字");
        return text;
    }
    static String required(String value, int max, String label) {
        String text = optional(value, max, label);
        if (text.isEmpty()) throw bad("请填写" + label);
        return text;
    }
    static String key(String value) { return value.strip().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT); }
}
