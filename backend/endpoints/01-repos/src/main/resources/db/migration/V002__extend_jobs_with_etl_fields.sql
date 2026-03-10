CREATE EXTENSION IF NOT EXISTS pgcrypto;

ALTER TABLE jobs DROP CONSTRAINT IF EXISTS jobs_dedup_hash_key;

ALTER TABLE jobs
    ADD COLUMN IF NOT EXISTS requirements TEXT,
    ADD COLUMN IF NOT EXISTS work_schedule TEXT,
    ADD COLUMN IF NOT EXISTS contact_text TEXT,
    ADD COLUMN IF NOT EXISTS contact_phone_numbers TEXT[] NOT NULL DEFAULT '{}',
    ADD COLUMN IF NOT EXISTS contact_telegram_usernames TEXT[] NOT NULL DEFAULT '{}',
    ADD COLUMN IF NOT EXISTS source_post_hash TEXT;

UPDATE jobs
SET source_post_hash = encode(
    digest(
        lower(trim(source)) || '|' || lower(trim(source_url)),
        'sha256'
    ),
    'hex'
)
WHERE source_post_hash IS NULL;

ALTER TABLE jobs
    ALTER COLUMN source_post_hash SET NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS idx_jobs_source_post_hash ON jobs (source_post_hash);
CREATE INDEX IF NOT EXISTS idx_jobs_dedup_hash ON jobs (dedup_hash);
