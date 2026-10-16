package com.projectpandora.api.notice;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanyNoticeRepository extends JpaRepository<CompanyNoticeEntity, Long> {
    List<CompanyNoticeEntity> findByEnabledTrueOrderBySortOrderAscIdAsc();

    List<CompanyNoticeEntity> findAllByOrderBySortOrderAscIdAsc();

    long countByEnabledTrue();

    Optional<CompanyNoticeEntity> findByIdAndEnabledTrue(Long id);
}
