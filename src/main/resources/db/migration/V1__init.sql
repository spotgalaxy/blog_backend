-- =====================================================================
-- V1__init.sql · blog 库表结构（与 scripts/init.sql 的 blog 库 DDL 一致）
--
-- 7 张表：users / posts / projects / comments / visit_log / daily_stats / ai_summaries + 索引
-- 权限模型：单管理员（users 表无 role 字段，posts/projects 无 author_id 字段）
-- 本文件只含 DDL，不含 CREATE DATABASE / \connect（由 Flyway 在 blog 库上执行）
-- =====================================================================

-- 管理员（本网站只有这一个账号体系，能登录即为站长）
CREATE TABLE users (
  id            BIGSERIAL PRIMARY KEY,
  username      VARCHAR(50)  NOT NULL UNIQUE,
  password_hash VARCHAR(100) NOT NULL,
  nickname      VARCHAR(50),
  created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
  updated_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- 博客文章
CREATE TABLE posts (
  id           BIGSERIAL PRIMARY KEY,
  slug         VARCHAR(200) NOT NULL UNIQUE,
  title        VARCHAR(200) NOT NULL,
  summary      VARCHAR(500),
  content      TEXT         NOT NULL DEFAULT '',
  tags         JSONB        NOT NULL DEFAULT '[]',
  cover        VARCHAR(500),
  draft        BOOLEAN      NOT NULL DEFAULT true,
  published_at TIMESTAMPTZ,
  view_count   BIGINT       NOT NULL DEFAULT 0,
  created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
  updated_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_posts_published_at ON posts (published_at DESC);

-- 作品
CREATE TABLE projects (
  id         BIGSERIAL PRIMARY KEY,
  slug       VARCHAR(200) NOT NULL UNIQUE,
  name       VARCHAR(200) NOT NULL,
  role       VARCHAR(100),
  year       INT,
  summary    VARCHAR(500),
  content    TEXT         NOT NULL DEFAULT '',
  cover      VARCHAR(500),
  cover_dark VARCHAR(500),
  letter     VARCHAR(10),
  link       VARCHAR(500),
  featured   BOOLEAN      NOT NULL DEFAULT false,
  sort_order INT          NOT NULL DEFAULT 99,
  created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- 评论
CREATE TABLE comments (
  id         BIGSERIAL PRIMARY KEY,
  post_id    BIGINT       NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
  parent_id  BIGINT       REFERENCES comments(id) ON DELETE CASCADE,
  author     VARCHAR(50)  NOT NULL,
  email      VARCHAR(200),
  content    TEXT         NOT NULL,
  status     VARCHAR(20)  NOT NULL DEFAULT 'pending',
  ip         VARCHAR(45),
  user_agent TEXT,
  created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_comments_post ON comments (post_id, status);

-- 访问流水（原始数据）
CREATE TABLE visit_log (
  id         BIGSERIAL PRIMARY KEY,
  path       VARCHAR(500) NOT NULL,
  ip         VARCHAR(45),
  ua         TEXT,
  referer    VARCHAR(500),
  created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_visit_log_created ON visit_log (created_at);

-- 按日聚合统计（看板用）
CREATE TABLE daily_stats (
  id         BIGSERIAL PRIMARY KEY,
  stat_date  DATE         NOT NULL UNIQUE,
  pv         BIGINT       NOT NULL DEFAULT 0,
  uv         BIGINT       NOT NULL DEFAULT 0,
  updated_at TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- AI 摘要缓存
CREATE TABLE ai_summaries (
  id         BIGSERIAL PRIMARY KEY,
  post_id    BIGINT       NOT NULL UNIQUE REFERENCES posts(id) ON DELETE CASCADE,
  summary    TEXT         NOT NULL,
  model      VARCHAR(50),
  created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
