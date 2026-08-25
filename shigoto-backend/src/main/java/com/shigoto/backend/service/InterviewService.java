package com.shigoto.backend.service;

import com.shigoto.backend.dto.CandidateInterviewResponseDTO;
import com.shigoto.backend.dto.InterviewRequestDTO;
import com.shigoto.backend.dto.InterviewResponseDTO;
import com.shigoto.backend.entity.*;
import com.shigoto.backend.exception.ResourceNotFoundException;
import com.shigoto.backend.repository.ApplicationRepository;
import com.shigoto.backend.repository.InterviewRepository;
import com.shigoto.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class InterviewService {

    private final InterviewRepository interviewRepository;
    private final ApplicationRepository applicationRepository;
    private final UserRepository userRepository;

    @Transactional
    public InterviewResponseDTO scheduleInterview(InterviewRequestDTO request) {

        // 1. שליפת המועמדות מולטי-ואלידציה
        Application application = applicationRepository.findById(request.applicationId())
                .orElseThrow(() -> new RuntimeException("Application not found with id: " + request.applicationId()));

        // 2. שליפת המראיין (המשתמש) מהמערכת
        User interviewer = userRepository.findById(request.interviewerId())
                .orElseThrow(() -> new RuntimeException("Interviewer not found with id: " + request.interviewerId()));
        if (interviewer.getRole() != Role.INTERVIEWER) {
            throw new IllegalArgumentException("Selected user is not an interviewer");
        }

        // 3. בניית ישות הראיון בעזרת ה-Builder של Lombok
        Interview interview = Interview.builder()
                .application(application)
                .interviewer(interviewer)
                .scheduledAt(request.scheduledAt())
                .meetingLink(request.meetingLink())
                .type(request.type())
                .status(InterviewStatus.SCHEDULED) // סטטוס התחלתי אוטומטי
                .build();

        // 4. עדכון הסטטוס של המועמדות לפי סוג הראיון
        if (request.type() == InterviewType.TECHNICAL) {
            application.setStatus(ApplicationStatus.TECH_INTERVIEW_SCHEDULED);
        } else if (request.type() == InterviewType.HR) {
            application.setStatus(ApplicationStatus.HR_INTERVIEW);
        }

        // שמירת המועמדות המעודכנת
        applicationRepository.save(application);

        // 5. שמירת הראיון במסד הנתונים
        Interview savedInterview = interviewRepository.save(interview);

        // 6. המרה ל-DTO והחזרה ללקוח (נניח שלמשתמש יש שדה אימייל לזיהוי, אם יש לך FirstName/LastName שחקי עם זה)
        return new InterviewResponseDTO(
                savedInterview.getId(),
                savedInterview.getApplication().getId(),
                savedInterview.getInterviewer().getEmail(), // כאן אנחנו מחזירים רק את האימייל/שם כדי לא לחשוף סיסמא
                savedInterview.getScheduledAt(),
                savedInterview.getMeetingLink(),
                savedInterview.getFeedback(),
                savedInterview.getType(),
                savedInterview.getStatus()
        );
    }

    @Transactional(readOnly = true)
    public List<CandidateInterviewResponseDTO> getCandidateInterviews(Long applicationId, User candidate) {
        if (candidate == null || candidate.getRole() != Role.CANDIDATE) {
            throw new AccessDeniedException("Candidate access is required");
        }
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Application not found with id: " + applicationId));
        if (!Objects.equals(application.getCandidate().getId(), candidate.getId())) {
            throw new AccessDeniedException("Application does not belong to the authenticated candidate");
        }
        return interviewRepository.findByApplicationIdOrderByScheduledAtAsc(applicationId)
                .stream()
                .map(this::toCandidateResponseDTO)
                .toList();
    }

    private CandidateInterviewResponseDTO toCandidateResponseDTO(Interview interview) {
        User interviewer = interview.getInterviewer();
        String interviewerName = (interviewer.getFirstName() + " " + interviewer.getLastName()).trim();

        return new CandidateInterviewResponseDTO(
                interview.getId(),
                interview.getApplication().getId(),
                interviewerName,
                interview.getScheduledAt(),
                interview.getMeetingLink(),
                interview.getType(),
                interview.getStatus()
        );
    }
}
