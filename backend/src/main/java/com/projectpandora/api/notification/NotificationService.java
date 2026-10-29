package com.projectpandora.api.notification;

import com.projectpandora.api.common.ApiException;
import java.time.Instant;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {
    private final NotificationRepository repo;
    public NotificationService(NotificationRepository repo) { this.repo=repo; }

    @Transactional
    public void notifyUser(Long userId,String type,String title,String body,Long taskId) {
        if(userId==null) return;
        String safeTitle=title.length()>200 ? title.substring(0,200) : title;
        String safeBody=body.length()>500 ? body.substring(0,500) : body;
        repo.save(new NotificationEntity(userId,type,safeTitle,safeBody,taskId));
    }

    @Transactional(readOnly=true)
    public NotificationList list(Long userId) {
        var items=repo.findByUserIdOrderByCreatedAtDescIdDesc(userId,PageRequest.of(0,200))
            .stream().map(NotificationResponse::from).toList();
        return new NotificationList(items,repo.countUnread(userId));
    }

    @Transactional
    public void markRead(Long userId,Long id) {
        var n=repo.findById(id).orElseThrow(()->new ApiException(404,"消息不存在"));
        if(!n.getUserId().equals(userId)) throw new ApiException(403,"无权操作该消息");
        if(n.getReadAt()==null) {n.setReadAt(Instant.now());repo.save(n);}
    }

    @Transactional
    public void markAllRead(Long userId) {
        var list=repo.findByUserIdAndReadAtIsNull(userId);
        var now=Instant.now();
        list.forEach(n->n.setReadAt(now));
        repo.saveAll(list);
    }
}
