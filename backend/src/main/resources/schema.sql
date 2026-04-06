CREATE TABLE IF NOT EXISTS news (
  id           BIGSERIAL PRIMARY KEY,
  title        TEXT NOT NULL,
  url          TEXT NOT NULL UNIQUE,
  source       VARCHAR(50) NOT NULL,
  summary      TEXT,
  thumbnail    TEXT,
  published_at TIMESTAMPTZ NOT NULL,
  created_at   TIMESTAMPTZ DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_news_published ON news(published_at DESC);
CREATE INDEX IF NOT EXISTS idx_news_source ON news(source);
