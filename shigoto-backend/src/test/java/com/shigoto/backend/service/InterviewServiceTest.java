package com.shigoto.backend.service;

import com.shigoto.backend.entity.Application;
import com.shigoto.backend.entity.Interview;
import com.shigoto.backend.entity.InterviewStatus;
import com.shigoto.backend.entity.InterviewType;
import com.shigoto.backend.entity.User;
import com.shigoto.backend.entity.Role;
import com.shigoto.backend.exception.ResourceNotFoundException;
import com.shigoto.backend.repository.ApplicationRepository;
import com.shigoto.backend.repository.InterviewRepository;
import com.shigoto.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InterviewServiceTest {

    private InterviewRepository interviewRepository;
    private ApplicationRepository applicationRepository;
    private InterviewService interviewService;

    @BeforeEach
    void setUp() {
        interviewRepository = mock(InterviewRepository.class);
        applicationRepository = mock(ApplicationRepository.class);
        interviewService = new InterviewService(
                interviewRepository, applicationRepository, mock(UserRepository.class));
    }

    @Test
    void returnsCandidateSafeInterviewsInRepositoryOrder() {
        User candidate = candidate(3L);
        Application application = Application.builder().id(7L).candidate(candidate).build();
        User interviewer = User.builder().firstName("Dana").lastName("Levi").build();
        LocalDateTime firstTime = LocalDateTime.of(2026, 8, 25, 10, 0);
        LocalDateTime secondTime = LocalDateTime.of(2026, 8, 27, 14, 30);
        Interview first = interview(1L, application, interviewer, firstTime, "https://zoom.us/j/first");
        Interview second = interview(2L, application, interviewer, secondTime, null);

        when(applicationRepository.findById(7L)).thenReturn(Optional.of(application));
        when(interviewRepository.findByApplicationIdOrderByScheduledAtAsc(7L))
                .thenReturn(List.of(first, second));

        var response = interviewService.getCandidateInterviews(7L, candidate);

        assertEquals(List.of(1L, 2L), response.stream().map(item -> item.id()).toList());
        assertEquals("Dana Levi", response.getFirst().interviewerName());
        assertEquals("https://zoom.us/j/first", response.getFirst().meetingLink());
        assertEquals(firstTime, response.getFirst().scheduledAt());
    }

    @Test
    void returnsEmptyListWhenApplicationHasNoInterviews() {
        User candidate = candidate(3L);
        Application application = Application.builder().id(7L).candidate(candidate).build();
        when(applicationRepository.findById(7L)).thenReturn(Optional.of(application));
        when(interviewRepository.findByApplicationIdOrderByScheduledAtAsc(7L))
                .thenReturn(List.of());

        assertTrue(interviewService.getCandidateInterviews(7L, candidate).isEmpty());
    }

    @Test
    void rejectsMissingApplicationBeforeQueryingInterviews() {
        when(applicationRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> interviewService.getCandidateInterviews(99L, candidate(3L)));
        verify(interviewRepository, never()).findByApplicationIdOrderByScheduledAtAsc(99L);
    }

    @Test
    void rejectsInterviewsForAnotherCandidatesApplication() {
        Application application = Application.builder().id(7L).candidate(candidate(3L)).build();
        when(applicationRepository.findById(7L)).thenReturn(Optional.of(application));

        assertThrows(AccessDeniedException.class,
                () -> interviewService.getCandidateInterviews(7L, candidate(4L)));
        verify(interviewRepository, never()).findByApplicationIdOrderByScheduledAtAsc(7L);
    }

    private Interview interview(
            Long id,
            Application application,
            User interviewer,
            LocalDateTime scheduledAt,
            String meetingLink) {
        return Interview.builder()
                .id(id)
                .application(application)
                .interviewer(interviewer)
                .scheduledAt(scheduledAt)
                .meetingLink(meetingLink)
                .type(InterviewType.TECHNICAL)
                .status(InterviewStatus.SCHEDULED)
                .feedback("candidate must never receive this")
                .build();
    }

    private User candidate(Long id) {
        return User.builder().id(id).role(Role.CANDIDATE).build();
    }
}
