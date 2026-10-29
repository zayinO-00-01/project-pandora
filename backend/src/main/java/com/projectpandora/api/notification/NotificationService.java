package com.projectpandora.api.notification;
import org.springframework.stereotype.Service;
@Service
public class NotificationService {
    private final NotificationRepository messages;
    public NotificationService(NotificationRepository messages){this.messages=messages;}
    public void send(Long recipient,String title,String message,String type,Long reference) {
        var n=new Notification();n.recipientId=recipient;n.title=title;n.message=message;n.type=type;n.referenceId=reference;n.createdAt=java.time.Instant.now();messages.save(n);
    }
}
