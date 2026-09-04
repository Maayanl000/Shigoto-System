ALTER TABLE public.jobs
    ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE public.applications
    ADD COLUMN IF NOT EXISTS task_reviewer_id BIGINT;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_application_task_reviewer'
    ) THEN
        ALTER TABLE public.applications
            ADD CONSTRAINT fk_application_task_reviewer
            FOREIGN KEY (task_reviewer_id) REFERENCES public.users(id);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS ix_applications_task_reviewer_status
    ON public.applications (task_reviewer_id, status);
