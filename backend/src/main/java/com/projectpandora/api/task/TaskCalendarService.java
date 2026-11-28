package com.projectpandora.api.task;

import com.projectpandora.api.common.ApiException;
import com.projectpandora.api.label.ContentLabelStore;
import com.projectpandora.api.label.LabelResponse;
import com.projectpandora.api.security.AccessService;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.hibernate.Session;
import org.hibernate.query.NativeQuery;
import org.hibernate.type.StandardBasicTypes;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class TaskCalendarService {
    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");
    private static final int PAGE_SIZE = 30;
    private static final int MAX_DAYS = 42;
    // The company calendar inherits the creator/responsible scope of GET /tasks,
    // not the broader department or temporary review detail scopes.
    private static final String VISIBLE = """
        t.deleted_at IS NULL AND t.approval_status='approved'
        AND t.due_at >= :startAt AND t.due_at < :endAt
        AND (t.created_by=:viewer OR t.assignee_id=:viewer OR EXISTS (
            SELECT 1 FROM task_assignees a WHERE a.task_id=t.id AND a.user_id=:viewer
        ))
        """;
    private static final String ORDER = """
        CASE WHEN status='done' THEN 1 ELSE 0 END,
        CASE priority WHEN 'high' THEN 0 WHEN 'low' THEN 2 ELSE 1 END, due_at, id
        """;
    private static final String BLOCKED = """
        CASE WHEN t.review_status IN ('pending_done','pending_change','pending_delete')
          OR EXISTS (SELECT 1 FROM task_reviews r WHERE r.task_id=t.id AND r.status='pending')
          THEN 1 ELSE 0 END
        """;

    private final EntityManager em;
    private final TaskRepository tasks;
    private final AccessService access;
    private final ContentLabelStore labels;

    public TaskCalendarService(EntityManager em, TaskRepository tasks, AccessService access, ContentLabelStore labels) {
        this.em = em;
        this.tasks = tasks;
        this.access = access;
        this.labels = labels;
    }

    public record Preview(long id, String title, Instant dueAt, Instant startAt, boolean canReschedule,
                          List<LabelResponse> labels) {}
    public record DaySummary(LocalDate date, long total, List<Preview> previews) {}
    public record AgendaItem(TaskResponse task, boolean canReschedule, List<LabelResponse> labels) {}
    public record Agenda(long total, List<AgendaItem> items, Integer nextPage) {}

    public List<DaySummary> summary(LocalDate from, LocalDate to) {
        long viewer = access.assertBusiness().getId();
        validateDate(from);
        validateDate(to);
        long count = ChronoUnit.DAYS.between(from, to) + 1;
        if (count < 1 || count > MAX_DAYS) throw new ApiException(400, "日历查询范围必须为 1 至 42 天");

        // Bind Shanghai midnight instants using the same Hibernate type as TaskEntity.dueAt.
        // Avoid SQL timezone conversion and timezone-table requirements on MySQL.
        StringBuilder bucket = new StringBuilder("CASE");
        for (int i = 1; i < count; i++) bucket.append(" WHEN t.due_at < :day").append(i).append(" THEN ").append(i - 1);
        String dayIndex = count == 1 ? "0" : bucket.append(" ELSE ").append(count - 1).append(" END").toString();
        String sql = """
            SELECT id,title,due_at,start_at,label_id,day_index,day_total FROM (
              SELECT candidates.*, COUNT(*) OVER (PARTITION BY day_index) AS day_total,
                     ROW_NUMBER() OVER (PARTITION BY day_index ORDER BY %s) AS position_in_day
              FROM (
                SELECT t.id,t.title,t.due_at,t.start_at,t.label_id,t.priority,t.status,%s AS day_index
                FROM dispatch_tasks t WHERE %s
              ) candidates
            ) ranked WHERE position_in_day <= 2 ORDER BY day_index,position_in_day
            """.formatted(ORDER, dayIndex, VISIBLE);
        var query = session().createNativeQuery(sql, Object[].class);
        query.addScalar("id", Long.class).addScalar("title", String.class)
            .addScalar("due_at", StandardBasicTypes.INSTANT).addScalar("start_at", StandardBasicTypes.INSTANT)
            .addScalar("label_id", Long.class).addScalar("day_index", Integer.class).addScalar("day_total", Long.class);
        bind(query, viewer, from, to.plusDays(1));
        for (int i = 1; i < count; i++) query.setParameter("day" + i, start(from.plusDays(i)), StandardBasicTypes.INSTANT);
        var rows = query.getResultList();
        var legacy = new LinkedHashMap<Long, Long>();
        for (var row : rows) legacy.put((Long) row[0], (Long) row[4]);
        var tags = labels.taskLabels(legacy);
        var blocked = blocked(legacy.keySet().stream().toList());
        var previews = new LinkedHashMap<LocalDate, List<Preview>>();
        var totals = new HashMap<LocalDate, Long>();
        for (var row : rows) {
            long id = (Long) row[0];
            var date = from.plusDays((Integer) row[5]);
            totals.put(date, (Long) row[6]);
            previews.computeIfAbsent(date, ignored -> new ArrayList<>())
                .add(new Preview(id, (String) row[1], (Instant) row[2], (Instant) row[3],
                    !blocked.contains(id), tags.get(id)));
        }
        return previews.entrySet().stream()
            .map(entry -> new DaySummary(entry.getKey(), totals.get(entry.getKey()), entry.getValue())).toList();
    }

    public Agenda agenda(LocalDate date, int page) {
        long viewer = access.assertBusiness().getId();
        validateDate(date);
        if (page < 0 || page > Integer.MAX_VALUE / PAGE_SIZE)
            throw new ApiException(400, "日历页码超出范围");
        var end = date.plusDays(1);
        var count = session().createNativeQuery("SELECT COUNT(*) FROM dispatch_tasks t WHERE " + VISIBLE, Long.class);
        long total = bind(count, viewer, date, end).getSingleResult();
        int offset = page * PAGE_SIZE;
        if (total <= offset) return new Agenda(total, List.of(), null);
        var query = session().createNativeQuery("SELECT t.id FROM dispatch_tasks t WHERE " + VISIBLE + " ORDER BY " + ORDER, Long.class);
        var ids = bind(query, viewer, date, end).setFirstResult(offset).setMaxResults(PAGE_SIZE).getResultList();
        var byId = new HashMap<Long, TaskResponse>();
        var legacy = new LinkedHashMap<Long, Long>();
        for (var task : tasks.findAllById(ids)) {
            var view = TaskResponse.from(task);
            byId.put(view.id(), view);
            legacy.put(view.id(), view.label() == null ? null : view.label().id());
        }
        var tags = labels.taskLabels(legacy);
        var blocked = blocked(ids);
        var entries = ids.stream().filter(byId::containsKey)
            .map(id -> new AgendaItem(byId.get(id), !blocked.contains(id), tags.get(id))).toList();
        return new Agenda(total, entries, (long) offset + PAGE_SIZE < total ? page + 1 : null);
    }

    private Set<Long> blocked(List<Long> ids) {
        if (ids.isEmpty()) return Set.of();
        var query = session().createNativeQuery(
            "SELECT t.id FROM dispatch_tasks t WHERE t.id IN (:ids) AND (" + BLOCKED + ") = 1", Long.class);
        return new HashSet<>(query.setParameterList("ids", ids).getResultList());
    }

    private Session session() { return em.unwrap(Session.class); }
    private static void validateDate(LocalDate date) {
        if (date.getYear() < 1000 || date.getYear() > 9998) throw new ApiException(400, "日历日期超出范围");
    }
    private static Instant start(LocalDate date) { return date.atStartOfDay(ZONE).toInstant(); }
    private static <T> NativeQuery<T> bind(NativeQuery<T> query, long viewer, LocalDate from, LocalDate toExclusive) {
        query.setParameter("viewer", viewer);
        query.setParameter("startAt", start(from), StandardBasicTypes.INSTANT);
        query.setParameter("endAt", start(toExclusive), StandardBasicTypes.INSTANT);
        return query;
    }
}
