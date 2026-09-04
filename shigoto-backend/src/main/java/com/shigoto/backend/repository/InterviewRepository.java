package com.shigoto.backend.repository;

import com.shigoto.backend.entity.Interview;
import com.shigoto.backend.entity.InterviewStatus;
import com.shigoto.backend.entity.InterviewType;
import com.shigoto.backend.entity.Company;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface InterviewRepository extends JpaRepository<Interview, Long> {

    // פעולה שתעזור לנו בהמשך לשלוף את כל הראיונות שנקבעו למועמדות מסוימת
    List<Interview> findByApplicationIdOrderByScheduledAtAsc(Long applicationId);

    Optional<Interview> findFirstByApplicationIdAndStatusOrderByScheduledAtDesc(
            Long applicationId, InterviewStatus status);

    Optional<Interview> findFirstByApplicationIdAndTypeOrderByIdAsc(
            Long applicationId, InterviewType type);

    List<Interview> findByApplicationCandidateIdOrderByScheduledAtAsc(Long candidateId);

    List<Interview> findByInterviewerIdOrderByScheduledAtAsc(Long interviewerId);

    Optional<Interview> findByIdAndInterviewerId(Long id, Long interviewerId);

    boolean existsByApplicationIdAndInterviewerId(Long applicationId, Long interviewerId);

    boolean existsByApplicationId(Long applicationId);

    boolean existsByApplicationIdAndInterviewerIdAndScheduledAtAndStatusNot(
            Long applicationId, Long interviewerId, java.time.LocalDateTime scheduledAt, InterviewStatus status);

    boolean existsByInterviewerIdAndScheduledAtAndStatusNot(
            Long interviewerId, java.time.LocalDateTime scheduledAt, InterviewStatus status);

    Optional<Interview> findByIdAndApplicationJobCompany(Long id, Company company);

    boolean existsByApplicationIdAndInterviewerIdAndScheduledAtAndStatusNotAndIdNot(
            Long applicationId, Long interviewerId, java.time.LocalDateTime scheduledAt,
            InterviewStatus status, Long id);

    boolean existsByInterviewerIdAndScheduledAtAndStatusNotAndIdNot(
            Long interviewerId, java.time.LocalDateTime scheduledAt, InterviewStatus status, Long id);

    boolean existsByApplicationIdAndTypeAndStatusAndIdNot(
            Long applicationId, InterviewType type, InterviewStatus status, Long id);

    boolean existsByApplicationIdAndTypeAndStatusNot(
            Long applicationId, InterviewType type, InterviewStatus status);

    boolean existsByApplicationIdAndTypeAndStatus(
            Long applicationId, InterviewType type, InterviewStatus status);
}
