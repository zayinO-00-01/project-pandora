package com.projectpandora.api.task;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
public interface TaskReviewRepository extends JpaRepository<TaskReview,Long> {
    List<TaskReview> findByTaskIdAndStatus(Long taskId,String status);
    boolean existsByTaskIdAndStatus(Long taskId,String status);
    List<TaskReview> findByReviewerIdAndStatus(Long reviewerId,String status);
    List<TaskReview> findByApplicantIdOrReviewerIdOrderByCreatedAtDescIdDesc(Long applicantId,Long reviewerId);
}
