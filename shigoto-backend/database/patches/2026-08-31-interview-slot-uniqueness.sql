-- Prevent concurrent writes from assigning one interviewer to multiple non-canceled interviews at the same time.
BEGIN;

CREATE UNIQUE INDEX IF NOT EXISTS uk_interviews_active_interviewer_slot
    ON public.interviews (interviewer_id, scheduled_at)
    WHERE status <> 'CANCELED';

COMMIT;
