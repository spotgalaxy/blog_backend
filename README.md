# spotgalaxy 博客后端（Spring Boot + PostgreSQL）

个人博客站点的后端服务：REST API（文章 / 作品 / 评论 / 统计 / AI 摘要），供 [blog_frontend](../blog_frontend)（Nuxt 4 SSR）通过代理调用。

## 架构

```
浏览器 → Nuxt 4 (:3000, SSR + /admin 管理后台)
              └─ server/api/[...].ts 代理 → Spring Boot (:8080)
                                              └─ MyBatis-Plus → PostgreSQL (:5432)
```

## 功能

- **内容**：文章（草稿/发布、标签、分页）、作品（精选、排序）CRUD
- **互动**：访客匿名评论（提交即 pending，后台审核后可见，同 IP 10 秒限频）、阅读量（同日同 IP 去重）
- **统计**：前台访问上报 `visit_log`，每小时定时聚合 `daily_stats`；看板提供近 N 天 PV/UV 与热门文章
- **AI 摘要**：DeepSeek（OpenAI 兼容格式）生成，一篇文章一份缓存，前台只读缓存
- **认证**：JWT（7 天有效），唯一管理员账号由首启自动创建；登录失败限流
- Swagger：`http://localhost:8080/swagger-ui.html`

## 环境要求

- JDK 21+、Maven 3.9+
- PostgreSQL 14+（库需预先建好，表结构见 `src/main/resources/db/migration/V1__init.sql`；Flyway baseline 跳过 V1，后续用 V2+ 演进）

## 运行

1. 复制 `.env.example` 为 `.env`，填写数据库连接、`JWT_SECRET`（≥32 字符）、管理员账号。`LLM_API_KEY` 不填时 AI 摘要接口返回 400，不影响其他功能。
2. 启动：

   ```bash
   mvn spring-boot:run          # 开发
   # 或
   mvn -DskipTests package && java -jar target/blog-backend-*.jar
   ```

3. 测试（直接运行在配置的 blog 库上，`@Transactional` 回滚不残留数据）：

   ```bash
   mvn test
   ```

## Docker

仓库自带 `Dockerfile`（多阶段构建），可单独容器化后端：

```bash
docker build -t blog-backend .
docker run -d -p 8080:8080 --env DB_URL=... --env DB_USER=... --env DB_PASSWORD=... --env JWT_SECRET=... --env ADMIN_USERNAME=... --env ADMIN_PASSWORD=... blog-backend
```

前端为独立仓库（`../blog_frontend`），构建 `npm run build` 后 `node .output/server/index.mjs`，用 nginx 反代 `/` 到 3000、`/api/` 到 8080。

## 环境变量

| 变量 | 说明 |
|---|---|
| `DB_URL` / `DB_USER` / `DB_PASSWORD` | PostgreSQL 连接（必填） |
| `JWT_SECRET` | JWT 签名密钥，≥32 字符（必填） |
| `ADMIN_USERNAME` / `ADMIN_PASSWORD` | 初始管理员，仅首启创建时使用（必填） |
| `LLM_BASE_URL` / `LLM_API_KEY` / `LLM_MODEL` | AI 摘要，默认 DeepSeek |
| `SERVER_PORT` | 监听端口，默认 8080 |
