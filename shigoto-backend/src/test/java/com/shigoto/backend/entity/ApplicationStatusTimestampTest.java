package com.shigoto.backend.entity;

import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

class ApplicationStatusTimestampTest {
    @Test void initializesStatusTimestampWithApplicationCreationTime() {
        Application application = new Application();
        application.onCreate();

        assertNotNull(application.getAppliedAt());
        assertEquals(application.getAppliedAt(), application.getStatusChangedAt());
        assertEquals(ApplicationStatus.APPLIED, application.getStatus());
    }

    @Test void changesTimestampOnlyForAGenuineStatusChange() {
        LocalDateTime original = LocalDateTime.of(2026, 8, 20, 10, 0);
        Application application = Application.builder().status(ApplicationStatus.APPLIED)
                .statusChangedAt(original).build();

        application.transitionTo(ApplicationStatus.HR_INTERVIEW);
        LocalDateTime changed = application.getStatusChangedAt();
        assertTrue(changed.isAfter(original));

        application.transitionTo(ApplicationStatus.HR_INTERVIEW);
        assertEquals(changed, application.getStatusChangedAt());

        application.setHrNotes("Unrelated note edit");
        application.setCandidateFeedback("Unrelated feedback edit");
        assertEquals(changed, application.getStatusChangedAt());
    }

    @Test void preservesExplicitDemoTimestampsOnCreation() {
        LocalDateTime appliedAt = LocalDateTime.of(2026, 8, 10, 9, 0);
        LocalDateTime statusChangedAt = LocalDateTime.of(2026, 8, 12, 14, 0);
        Application application = Application.builder()
                .status(ApplicationStatus.HR_INTERVIEW)
                .appliedAt(appliedAt)
                .statusChangedAt(statusChangedAt)
                .build();

        application.onCreate();

        assertEquals(appliedAt, application.getAppliedAt());
        assertEquals(statusChangedAt, application.getStatusChangedAt());
        assertEquals(ApplicationStatus.HR_INTERVIEW, application.getStatus());
    }
}
