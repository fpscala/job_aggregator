WITH ranked_by_dedup_hash AS (
    SELECT
        id,
        ROW_NUMBER() OVER (
            PARTITION BY dedup_hash
            ORDER BY posted_at DESC, created_at DESC, id DESC
        ) AS row_number
    FROM jobs
),
deleted_duplicates AS (
    DELETE FROM jobs
    WHERE id IN (
        SELECT id
        FROM ranked_by_dedup_hash
        WHERE row_number > 1
    )
    RETURNING id
)
SELECT COUNT(*) FROM deleted_duplicates;

DROP INDEX IF EXISTS idx_jobs_dedup_hash;

CREATE UNIQUE INDEX IF NOT EXISTS idx_jobs_dedup_hash ON jobs (dedup_hash);
