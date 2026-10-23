package com.projectpandora.api.notification;

import com.projectpandora.api.security.AccessService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {
    private final NotificationService service;
    private final AccessService access;
    public NotificationController(NotificationService service,AccessService access) {this.service=service;this.access=access;}
    @GetMapping
    public NotificationList list() {return service.list(access.assertBusiness().getId());}
    @PostMapping("/{id}/read")
    public ResponseEntity<Void> read(@PathVariable Long id) {
        service.markRead(access.assertBusiness().getId(),id);
        return ResponseEntity.noContent().build();
    }
    @PostMapping("/read-all")
    public ResponseEntity<Void> readAll() {
        service.markAllRead(access.assertBusiness().getId());
        return ResponseEntity.noContent().build();
    }
}
