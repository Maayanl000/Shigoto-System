DO $$
DECLARE
    duplicate_companies TEXT;
BEGIN
    SELECT string_agg(company_id::TEXT || ' (' || hr_count || ' HR users)', ', ')
    INTO duplicate_companies
    FROM (
        SELECT company_id, COUNT(*) AS hr_count
        FROM public.users
        WHERE role = 'HR' AND company_id IS NOT NULL
        GROUP BY company_id
        HAVING COUNT(*) > 1
    ) duplicates;

    IF duplicate_companies IS NOT NULL THEN
        RAISE EXCEPTION 'Cannot enforce one HR per company. Duplicate company IDs: %. Resolve these users manually before applying this migration.',
            duplicate_companies;
    END IF;
END $$;

CREATE UNIQUE INDEX IF NOT EXISTS uk_users_one_hr_per_company
    ON public.users (company_id)
    WHERE role = 'HR' AND company_id IS NOT NULL;
