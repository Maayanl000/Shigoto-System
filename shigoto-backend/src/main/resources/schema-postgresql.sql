-- Backfill versioned Job updates safely on existing databases before requests are served.
ALTER TABLE public.jobs
    ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE public.applications
    ADD COLUMN IF NOT EXISTS task_reviewer_id BIGINT;

-- Prevent concurrent writes from assigning one interviewer to multiple non-canceled interviews at the same time.
CREATE UNIQUE INDEX IF NOT EXISTS uk_interviews_active_interviewer_slot
    ON public.interviews (interviewer_id, scheduled_at)
    WHERE status <> 'CANCELED';

CREATE INDEX IF NOT EXISTS ix_applications_task_reviewer_status
    ON public.applications (task_reviewer_id, status);

CREATE UNIQUE INDEX IF NOT EXISTS uk_users_one_hr_per_company
    ON public.users (company_id)
    WHERE role = 'HR' AND company_id IS NOT NULL;

-- Hibernate does not update an existing enum check constraint when Java enum values change.
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
