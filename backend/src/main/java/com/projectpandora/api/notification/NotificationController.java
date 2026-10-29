package com.projectpandora.api.notification;
import com.projectpandora.api.common.ApiException;
import com.projectpandora.api.security.AccessService;
import java.util.List;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1/notifications") @Transactional
public class NotificationController {
    private final NotificationRepository messages;private final AccessService access;
    public NotificationController(NotificationRepository messages,AccessService access){this.messages=messages;this.access=access;}
    @GetMapping @Transactional(readOnly=true) public List<Notification> list(){return messages.findByRecipientIdOrderByCreatedAtDescIdDesc(access.assertBusiness().getId());}
    @PutMapping("/{id}/read") public Notification read(@PathVariable Long id){var me=access.assertBusiness();var n=messages.findById(id).orElseThrow(()->new ApiException(404,"通知不存在"));if(!n.recipientId.equals(me.getId()))throw new ApiException(403,"不能操作他人的通知");if(n.readAt==null)n.readAt=java.time.Instant.now();return messages.saveAndFlush(n);}
}
