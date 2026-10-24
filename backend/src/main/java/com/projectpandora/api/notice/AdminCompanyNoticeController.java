package com.projectpandora.api.notice;

import com.projectpandora.api.security.AccessService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/company-notices")
public class AdminCompanyNoticeController {

    private final CompanyNoticeService service;
    private final AccessService access;

    public AdminCompanyNoticeController(CompanyNoticeService service, AccessService access) {
        this.service = service;
        this.access = access;
    }

    @GetMapping
    public List<AdminNoticeResponse> list() {
        access.assertAdmin();
        return service.all().stream().map(AdminNoticeResponse::from).toList();
    }

    @PostMapping
    public ResponseEntity<AdminNoticeResponse> create(@Valid @RequestBody UpsertRequest request) {
        access.assertAdmin();
        Long admin = access.currentUser().getId();
        var notice = service.create(admin, request.title(), request.content(), request.sortOrder(), request.enabled());
        return ResponseEntity.status(201).body(AdminNoticeResponse.from(notice));
    }

    @PatchMapping("/{id}")
    public AdminNoticeResponse update(@PathVariable Long id, @RequestBody UpdateRequest request) {
        access.assertAdmin();
        var notice = service.update(id, request.title(), request.content(), request.sortOrder(), request.enabled());
        return AdminNoticeResponse.from(notice);
    }

    /**
     * ADMIN 自己的个人备注。业务端 {@code /company-notices/{id}/note} 会以 403 拒绝 ADMIN
     * （系统管理员不办理业务），但 ADMIN 同样是「用户」，也要能对事项写本人备注。
     */
    @GetMapping("/{id}/note")
    public AdminNoteResponse myNote(@PathVariable Long id) {
        access.assertAdmin();
        Long me = access.currentUser().getId();
        service.require(id);
        return new AdminNoteResponse(id, service.noteOf(id, me));
    }

    @PutMapping("/{id}/note")
    public AdminNoteResponse saveNote(@PathVariable Long id, @Valid @RequestBody CompanyNoticeController.NoteRequest request) {
        access.assertAdmin();
        Long me = access.currentUser().getId();
        service.require(id);
        return new AdminNoteResponse(id, service.saveNote(id, me, request.content()));
    }

    public record AdminNoteResponse(Long id, String myNote) {}

    public record UpsertRequest(@NotBlank @Size(max = 128) String title, @NotBlank String content,
                                Integer sortOrder, Boolean enabled) {}

    public record UpdateRequest(@Size(max = 128) String title, String content, Integer sortOrder, Boolean enabled) {}

    public record AdminNoticeResponse(Long id, String title, String content, Integer sortOrder, boolean enabled,
                                      Long publisherId, String updatedAt) {
        static AdminNoticeResponse from(CompanyNoticeEntity notice) {
            return new AdminNoticeResponse(notice.getId(), notice.getTitle(), notice.getContent(), notice.getSortOrder(),
                    notice.isEnabled(), notice.getPublisherId(), notice.getUpdatedAt().toString());
        }
    }
}
