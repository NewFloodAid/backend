-- Manual migration for existing databases.
-- Removes legacy image category schema that is no longer used by the application.

DO $$
DECLARE
    fk_name text;
BEGIN
    IF to_regclass('public.image_categories') IS NOT NULL THEN
        FOR fk_name IN
            SELECT c.conname
            FROM pg_constraint c
            JOIN pg_class t ON t.oid = c.conrelid
            WHERE t.relname = 'images'
              AND c.contype = 'f'
              AND c.confrelid = 'public.image_categories'::regclass
        LOOP
            EXECUTE format('ALTER TABLE images DROP CONSTRAINT IF EXISTS %I', fk_name);
        END LOOP;
    END IF;
END $$;

ALTER TABLE IF EXISTS images
    DROP COLUMN IF EXISTS image_category_id;

DROP TABLE IF EXISTS image_categories;
