-- Keep PostgreSQL enum check constraints aligned with the current Java enums for existing databases.
BEGIN;

ALTER TABLE public.applications
    DROP CONSTRAINT IF EXISTS applications_status_check;

ALTER TABLE public.applications
    ADD CONSTRAINT applications_status_check
    CHECK (status IN (
        'APPLIED',
        'HR_INTERVIEW',
        'TASK_SENT',
        'TASK_SUBMITTED',
        'TASK_APPROVED',
        'TECH_INTERVIEW_SCHEDULED',
        'OFFER',
        'HIRED',
        'REJECTED'
    ));

ALTER TABLE public.notifications
    DROP CONSTRAINT IF EXISTS notifications_type_check;

ALTER TABLE public.notifications
    ADD CONSTRAINT notifications_type_check
    CHECK (type IN (
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
