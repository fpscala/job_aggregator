ALTER TABLE jobs
    ADD COLUMN IF NOT EXISTS responsibilities TEXT,
    ADD COLUMN IF NOT EXISTS benefits TEXT,
    ADD COLUMN IF NOT EXISTS additional TEXT,
    ADD COLUMN IF NOT EXISTS contact_links TEXT[] NOT NULL DEFAULT '{}';

WITH ranked_by_source_post_hash AS (
    SELECT
        id,
        ROW_NUMBER() OVER (
            PARTITION BY source_post_hash
            ORDER BY posted_at DESC, created_at DESC, id DESC
        ) AS row_number
    FROM jobs
    WHERE source_post_hash IS NOT NULL
),
deleted_source_duplicates AS (
    DELETE FROM jobs
    WHERE id IN (
        SELECT id
        FROM ranked_by_source_post_hash
        WHERE row_number > 1
    )
    RETURNING id
)
SELECT COUNT(*) FROM deleted_source_duplicates;

WITH ranked_by_dedup_hash AS (
    SELECT
        id,
        ROW_NUMBER() OVER (
            PARTITION BY dedup_hash
            ORDER BY posted_at DESC, created_at DESC, id DESC
        ) AS row_number
    FROM jobs
),
deleted_dedup_duplicates AS (
    DELETE FROM jobs
    WHERE id IN (
        SELECT id
        FROM ranked_by_dedup_hash
        WHERE row_number > 1
    )
    RETURNING id
)
SELECT COUNT(*) FROM deleted_dedup_duplicates;

DROP INDEX IF EXISTS idx_jobs_source_post_hash;
CREATE UNIQUE INDEX IF NOT EXISTS idx_jobs_source_post_hash ON jobs (source_post_hash);

DROP INDEX IF EXISTS idx_jobs_dedup_hash;
CREATE UNIQUE INDEX IF NOT EXISTS idx_jobs_dedup_hash ON jobs (dedup_hash);
