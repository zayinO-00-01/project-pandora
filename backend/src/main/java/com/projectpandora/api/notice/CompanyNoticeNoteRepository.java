package com.projectpandora.api.notice;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanyNoticeNoteRepository extends JpaRepository<CompanyNoticeNoteEntity, Long> {
    Optional<CompanyNoticeNoteEntity> findByNoticeIdAndUserId(Long noticeId, Long userId);

    List<CompanyNoticeNoteEntity> findByUserId(Long userId);
}
