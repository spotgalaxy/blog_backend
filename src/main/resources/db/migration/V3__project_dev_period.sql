-- 作品年份改为开发时间区间(年-月,如 2024-03)
ALTER TABLE projects ADD COLUMN dev_start VARCHAR(7);
ALTER TABLE projects ADD COLUMN dev_end   VARCHAR(7);

-- 老数据回填:year = 2025 → 2025-01 ~ 2025-12
UPDATE projects
SET dev_start = year::text || '-01',
    dev_end   = year::text || '-12'
WHERE year IS NOT NULL;

ALTER TABLE projects DROP COLUMN year;
