package com.projectpandora.api.notice;

import com.projectpandora.api.common.ApiException;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CompanyNoticeService {

    static final int MAX_ENABLED = 10;

    private final CompanyNoticeRepository notices;
    private final CompanyNoticeNoteRepository notes;

    public CompanyNoticeService(CompanyNoticeRepository notices, CompanyNoticeNoteRepository notes) {
        this.notices = notices;
        this.notes = notes;
    }

    public List<CompanyNoticeEntity> enabled() {
        return notices.findByEnabledTrueOrderBySortOrderAscIdAsc();
    }

    public List<CompanyNoticeEntity> all() {
        return notices.findAllByOrderBySortOrderAscIdAsc();
    }

    public CompanyNoticeEntity requireEnabled(Long id) {
        return notices.findByIdAndEnabledTrue(id).orElseThrow(() -> new ApiException(404, "公司事项不存在"));
    }

    /** ADMIN 视角：停用的事项同样可读、可写本人备注。 */
    public CompanyNoticeEntity require(Long id) {
        return notices.findById(id).orElseThrow(() -> new ApiException(404, "公司事项不存在"));
    }

    @Transactional
    public CompanyNoticeEntity create(Long publisherId, String title, String content, Integer sortOrder, Boolean enabled) {
        String cleanTitle = requireText(title, 128, "标题不能为空");
        String cleanContent = requireText(content, 20000, "内容不能为空");
        boolean targetEnabled = enabled == null || enabled;
        if (targetEnabled && notices.countByEnabledTrue() >= MAX_ENABLED) {
            throw new ApiException(400, "公司十大重要事项最多只能有 10 条启用");
        }
        var notice = new CompanyNoticeEntity();
        notice.setTitle(cleanTitle);
        notice.setContent(cleanContent);
        notice.setSortOrder(sortOrder == null ? 0 : sortOrder);
        notice.setEnabled(targetEnabled);
        notice.setPublisherId(publisherId);
        notice.setCreatedAt(Instant.now());
        notice.setUpdatedAt(Instant.now());
        return notices.saveAndFlush(notice);
    }

    @Transactional
    public CompanyNoticeEntity update(Long id, String title, String content, Integer sortOrder, Boolean enabled) {
        var notice = notices.findById(id).orElseThrow(() -> new ApiException(404, "公司事项不存在"));
        if (title != null) {
            notice.setTitle(requireText(title, 128, "标题不能为空"));
        }
        if (content != null) {
            notice.setContent(requireText(content, 20000, "内容不能为空"));
        }
        if (sortOrder != null) {
            notice.setSortOrder(sortOrder);
        }
        if (enabled != null) {
            if (enabled && !notice.isEnabled() && notices.countByEnabledTrue() >= MAX_ENABLED) {
                throw new ApiException(400, "公司十大重要事项最多只能有 10 条启用");
            }
            notice.setEnabled(enabled);
        }
        notice.setUpdatedAt(Instant.now());
        return notices.saveAndFlush(notice);
    }

    @Transactional
    public String saveNote(Long noticeId, Long userId, String content) {
        if (content == null || content.length() > 2000) {
            throw new ApiException(400, "备注不能为空值且最多 2000 字");
        }
        String clean = content.trim();
        var note = notes.findByNoticeIdAndUserId(noticeId, userId).orElseGet(() -> {
            var created = new CompanyNoticeNoteEntity();
            created.setNoticeId(noticeId);
            created.setUserId(userId);
            return created;
        });
        note.setContent(clean);
        note.setUpdatedAt(Instant.now());
        notes.saveAndFlush(note);
        return clean;
    }

    public String noteOf(Long noticeId, Long userId) {
        return notes.findByNoticeIdAndUserId(noticeId, userId).map(CompanyNoticeNoteEntity::getContent).orElse(null);
    }

    private static String requireText(String value, int max, String emptyMessage) {
        if (value == null || value.isBlank()) {
            throw new ApiException(400, emptyMessage);
        }
        String trimmed = value.trim();
        if (trimmed.length() > max) {
            throw new ApiException(400, "内容过长");
        }
        return trimmed;
    }
}
