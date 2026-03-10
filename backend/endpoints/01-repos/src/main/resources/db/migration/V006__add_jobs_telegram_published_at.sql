ALTER TABLE jobs
    ADD COLUMN IF NOT EXISTS telegram_published_at TIMESTAMPTZ;

UPDATE jobs
SET telegram_published_at = published.latest_published_at
FROM (
    SELECT job_id, MAX(published_at) AS latest_published_at
    FROM job_channel_posts
    GROUP BY job_id
) AS published
WHERE jobs.id = published.job_id
  AND (
      jobs.telegram_published_at IS NULL
      OR jobs.telegram_published_at < published.latest_published_at
  );
