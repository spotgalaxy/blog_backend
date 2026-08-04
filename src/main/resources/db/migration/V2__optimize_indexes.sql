-- 列表页常用过滤：draft=false + published_at 排序，复合索引替代单列索引
DROP INDEX IF EXISTS idx_posts_published_at;
CREATE INDEX idx_posts_draft_published ON posts (draft, published_at DESC);

-- 标签筛选（jsonb_exists / jsonb_array_elements_text）使用 GIN 索引
CREATE INDEX idx_posts_tags_gin ON posts USING gin (tags);
