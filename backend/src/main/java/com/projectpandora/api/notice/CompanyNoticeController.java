package com.projectpandora.api.notice;

import com.projectpandora.api.security.AccessService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/company-notices")
public class CompanyNoticeController {

    private final CompanyNoticeService service;
    private final AccessService access;

    public CompanyNoticeController(CompanyNoticeService service, AccessService access) {
        this.service = service;
        this.access = access;
    }

    @GetMapping
    public List<NoticeResponse> list() {
        Long me = access.assertBusiness().getId();
        return service.enabled().stream().map(notice -> NoticeResponse.from(notice, service.noteOf(notice.getId(), me))).toList();
    }

    @GetMapping("/{id}")
    public NoticeResponse detail(@PathVariable Long id) {
        Long me = access.assertBusiness().getId();
        var notice = service.requireEnabled(id);
        return NoticeResponse.from(notice, service.noteOf(notice.getId(), me));
    }

    @PutMapping("/{id}/note")
    public NoticeResponse saveNote(@PathVariable Long id, @Valid @RequestBody NoteRequest request) {
        Long me = access.assertBusiness().getId();
        var notice = service.requireEnabled(id);
        String note = service.saveNote(id, me, request.content());
        return NoticeResponse.from(notice, note);
    }

    public record NoteRequest(@NotBlank @Size(max = 2000) String content) {}

    public record NoticeResponse(Long id, String title, String content, Integer sortOrder, boolean enabled,
                                 String updatedAt, String myNote) {
        static NoticeResponse from(CompanyNoticeEntity notice, String myNote) {
            return new NoticeResponse(notice.getId(), notice.getTitle(), notice.getContent(), notice.getSortOrder(),
                    notice.isEnabled(), notice.getUpdatedAt().toString(), myNote);
        }
    }
}
