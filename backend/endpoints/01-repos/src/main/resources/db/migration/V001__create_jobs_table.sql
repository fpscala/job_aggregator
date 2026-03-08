CREATE TABLE IF NOT EXISTS jobs (
    id UUID PRIMARY KEY,
    title TEXT NOT NULL,
    company TEXT,
    description TEXT NOT NULL,
    salary TEXT,
    location TEXT,
    source TEXT NOT NULL,
    source_url TEXT NOT NULL,
    posted_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    dedup_hash TEXT NOT NULL UNIQUE
);

CREATE INDEX IF NOT EXISTS idx_jobs_location ON jobs (location);
CREATE INDEX IF NOT EXISTS idx_jobs_posted_at ON jobs (posted_at DESC);
