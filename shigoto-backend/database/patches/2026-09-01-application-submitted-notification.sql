-- Add the application-submission notification to existing PostgreSQL databases.
BEGIN;

ALTER TABLE public.notifications
    DROP CONSTRAINT IF EXISTS notifications_type_check;

ALTER TABLE public.notifications
    ADD CONSTRAINT notifications_type_check
    CHECK (type IN (
        'APPLICATION_SUBMITTED',
        'APPLICATION_REJECTED',
        'APPLICATION_OFFERED',
        'APPLICATION_HIRED',
        'HOME_TASK_ASSIGNED',
        'HOME_TASK_UPDATED',
        'INTERVIEW_SCHEDULED',
        'INTERVIEW_RESCHEDULED',
        'INTERVIEW_CANCELED'
    ));

COMMIT;
