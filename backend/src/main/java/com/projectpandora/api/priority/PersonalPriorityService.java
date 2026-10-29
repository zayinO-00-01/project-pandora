package com.projectpandora.api.priority;

import com.projectpandora.api.common.ApiException;
import com.projectpandora.api.panel.PanelItemResponse;
import com.projectpandora.api.user.UserEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PersonalPriorityService {
    private final EntityManager em;

    public PersonalPriorityService(EntityManager em) { this.em = em; }

    @Transactional
    public PersonalPriorityResponse get(Long ownerId) {
        lockOwner(ownerId);
        var state = em.find(PersonalPriorityListEntity.class, ownerId);
        return response(state == null ? 0 : state.revision, items(ownerId));
    }

    @Transactional
    public PersonalPriorityResponse save(Long ownerId, PersonalPriorityRequest request) {
        lockOwner(ownerId);
        var state = em.find(PersonalPriorityListEntity.class, ownerId);
        long revision = state == null ? 0 : state.revision;
        if (request.revision() != revision) {
            throw new ApiException(409, "个人事项已变化，请刷新后重试");
        }
        var existing = items(ownerId);
        Map<Long, PersonalPriorityEntity> byId = new HashMap<>();
        existing.forEach(item -> byId.put(item.id, item));
        Set<Long> retained = new HashSet<>();
        // Validate the whole replacement before mutating any managed entity.
        for (var item : request.items()) {
            if (item.title().trim().isEmpty() || item.title().trim().length() > 120) {
                throw new ApiException(400, "标题不能为空且最多 120 字");
            }
            if (item.id() != null && item.id() >= 0) {
                if (!byId.containsKey(item.id()) || !retained.add(item.id())) {
                    throw new ApiException(400, "个人事项不存在或重复");
                }
            }
        }
        existing.stream().filter(item -> !retained.contains(item.id)).forEach(em::remove);
        var replacement = new ArrayList<PersonalPriorityEntity>();
        for (int i = 0; i < request.items().size(); i++) {
            var item = request.items().get(i);
            var entity = item.id() == null || item.id() < 0 ? new PersonalPriorityEntity() : byId.get(item.id());
            entity.ownerId = ownerId;
            entity.title = item.title().trim();
            entity.detail = item.detail();
            entity.sortOrder = i;
            if (entity.id == null) em.persist(entity);
            replacement.add(entity);
        }
        if (state == null) {
            state = new PersonalPriorityListEntity();
            state.ownerId = ownerId;
            em.persist(state);
        }
        state.revision = revision + 1;
        em.flush();
        return response(state.revision, replacement);
    }

    private void lockOwner(Long ownerId) {
        // Lock an always-existing row, including the first save of an empty list.
        // Reads share this lock so revision and items describe one committed snapshot.
        if (em.find(UserEntity.class, ownerId, LockModeType.PESSIMISTIC_WRITE) == null) {
            throw new ApiException(401, "账号不可用，请重新登录");
        }
    }

    private List<PersonalPriorityEntity> items(Long ownerId) {
        return em.createQuery("select p from PersonalPriorityEntity p where p.ownerId = :ownerId order by p.sortOrder, p.id",
                PersonalPriorityEntity.class).setParameter("ownerId", ownerId).getResultList();
    }

    private PersonalPriorityResponse response(long revision, List<PersonalPriorityEntity> items) {
        return new PersonalPriorityResponse(revision, items.stream()
                .map(item -> new PanelItemResponse(item.id, item.title, item.sortOrder, null, item.detail)).toList());
    }
}
