CREATE TABLE IF NOT EXISTS job_channel_posts (
    id UUID PRIMARY KEY,
    job_id UUID NOT NULL REFERENCES jobs (id) ON DELETE CASCADE,
    channel_chat_id TEXT NOT NULL,
    telegram_message_id BIGINT NOT NULL,
    caption TEXT NOT NULL,
    banner_image_path TEXT NOT NULL,
    published_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_job_channel_posts_job_channel
    ON job_channel_posts (job_id, channel_chat_id);

CREATE INDEX IF NOT EXISTS idx_job_channel_posts_job_id
    ON job_channel_posts (job_id);

CREATE INDEX IF NOT EXISTS idx_job_channel_posts_published_at
    ON job_channel_posts (published_at DESC);
