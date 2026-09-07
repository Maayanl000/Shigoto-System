package com.shigoto.backend.repository;

import com.shigoto.backend.entity.Interview;
import com.shigoto.backend.entity.InterviewStatus;
import com.shigoto.backend.entity.InterviewType;
import com.shigoto.backend.entity.Company;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

/**
 * Declares application-specific persistence queries for interview data.
 */
@Repository
public interface InterviewRepository extends JpaRepository<Interview, Long> {

    /**
     * Lists an application's interviews in scheduled-time order.
     * @param applicationId the application identifier
     * @return application interviews ordered by scheduled time ascending
     */
    List<Interview> findByApplicationIdOrderByScheduledAtAsc(Long applicationId);

    /**
     * Finds the most recently scheduled interview in a given status for an application.
     * @param applicationId the application identifier
     * @param status the requested domain status
     * @return the latest matching interview, if present
     */
    Optional<Interview> findFirstByApplicationIdAndStatusOrderByScheduledAtDesc(
            Long applicationId, InterviewStatus status);

    /**
     * Finds the earliest-created interview of a given type for an application.
     * @param applicationId the application identifier
     * @param type the requested domain type
     * @return the first matching interview by identifier, if present
     */
    Optional<Interview> findFirstByApplicationIdAndTypeOrderByIdAsc(
            Long applicationId, InterviewType type);

    /**
     * Lists interviews across a candidate's applications in scheduled-time order.
     * @param candidateId the candidate identifier
     * @return candidate interviews ordered by scheduled time ascending
     */
    List<Interview> findByApplicationCandidateIdOrderByScheduledAtAsc(Long candidateId);

    /**
     * Lists interviews assigned to an interviewer in scheduled-time order.
     * @param interviewerId the interviewer id
     * @return assigned interviews ordered by scheduled time ascending
     */
    List<Interview> findByInterviewerIdOrderByScheduledAtAsc(Long interviewerId);

    /**
     * Finds an interview only when it is assigned to the supplied interviewer.
     * @param id the entity identifier
     * @param interviewerId the interviewer id
     * @return the assigned interview, if present
     */
    Optional<Interview> findByIdAndInterviewerId(Long id, Long interviewerId);

    /**
     * Checks whether an interviewer is already assigned to the application.
     * @param applicationId the application identifier
     * @param interviewerId the interviewer id
     * @return {@code true} when the interviewer is assigned to any interview for the application; otherwise {@code false}
     */
    boolean existsByApplicationIdAndInterviewerId(Long applicationId, Long interviewerId);

    /**
     * Checks whether the application has any interview records.
     * @param applicationId the application identifier
     * @return {@code true} when at least one interview exists for the application; otherwise {@code false}
     */
    boolean existsByApplicationId(Long applicationId);

    /**
     * Checks for a non-excluded interview at the same application, interviewer, and time slot.
     * @param applicationId the application identifier
     * @param interviewerId the interviewer id
     * @param scheduledAt the scheduled at
     * @param status the requested domain status
     * @return {@code true} when a conflicting application interview slot exists; otherwise {@code false}
     */
    boolean existsByApplicationIdAndInterviewerIdAndScheduledAtAndStatusNot(
            Long applicationId, Long interviewerId, java.time.LocalDateTime scheduledAt, InterviewStatus status);

    /**
     * Checks whether an interviewer has a non-excluded interview at the supplied time.
     * @param interviewerId the interviewer id
     * @param scheduledAt the scheduled at
     * @param status the requested domain status
     * @return {@code true} when the interviewer has a conflicting active slot; otherwise {@code false}
     */
    boolean existsByInterviewerIdAndScheduledAtAndStatusNot(
            Long interviewerId, java.time.LocalDateTime scheduledAt, InterviewStatus status);

    /**
     * Finds an interview only when its application job belongs to the supplied company.
     * @param id the entity identifier
     * @param company the company that scopes the operation
     * @return the company-scoped interview, if present
     */
    Optional<Interview> findByIdAndApplicationJobCompany(Long id, Company company);

    /**
     * Checks for another non-excluded interview at the same application, interviewer, and time slot.
     * @param applicationId the application identifier
     * @param interviewerId the interviewer id
     * @param scheduledAt the scheduled at
     * @param status the requested domain status
     * @param id the entity identifier
     * @return {@code true} when a different interview conflicts with the replacement slot; otherwise {@code false}
     */
    boolean existsByApplicationIdAndInterviewerIdAndScheduledAtAndStatusNotAndIdNot(
            Long applicationId, Long interviewerId, java.time.LocalDateTime scheduledAt,
            InterviewStatus status, Long id);

    /**
     * Checks whether an interviewer has another non-excluded interview at the supplied time.
     * @param interviewerId the interviewer id
     * @param scheduledAt the scheduled at
     * @param status the requested domain status
     * @param id the entity identifier
     * @return {@code true} when a different interview occupies the replacement slot; otherwise {@code false}
     */
    boolean existsByInterviewerIdAndScheduledAtAndStatusNotAndIdNot(
            Long interviewerId, java.time.LocalDateTime scheduledAt, InterviewStatus status, Long id);

    /**
     * Checks whether another interview of the given type and status exists for the application.
     * @param applicationId the application identifier
     * @param type the requested domain type
     * @param status the requested domain status
     * @param id the entity identifier
     * @return {@code true} when a different matching interview exists; otherwise {@code false}
     */
    boolean existsByApplicationIdAndTypeAndStatusAndIdNot(
            Long applicationId, InterviewType type, InterviewStatus status, Long id);

    /**
     * Checks whether the application has an interview of the given type outside the excluded status.
     * @param applicationId the application identifier
     * @param type the requested domain type
     * @param status the requested domain status
     * @return {@code true} when an interview of the type exists in another status; otherwise {@code false}
     */
    boolean existsByApplicationIdAndTypeAndStatusNot(
            Long applicationId, InterviewType type, InterviewStatus status);

    /**
     * Checks whether the application has an interview with the supplied type and status.
     * @param applicationId the application identifier
     * @param type the requested domain type
     * @param status the requested domain status
     * @return {@code true} when an interview matches both the type and status; otherwise {@code false}
     */
    boolean existsByApplicationIdAndTypeAndStatus(
            Long applicationId, InterviewType type, InterviewStatus status);
}
