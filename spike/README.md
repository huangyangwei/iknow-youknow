# M0 技术验证（HYW-19）

两个 Spike 的最小可运行验证代码，位于 `spike/` 目录。

## Spike ① `m0-spike1-mbplus` — Spring Boot 4 + MyBatis-Plus 联通性

验证点：
- `mybatis-plus-spring-boot4-starter` 3.5.17 + Spring Boot 4.0.7 正常启动
- `@MapperScan` 跨模块（iknow-common → iknow-server）扫描
- CRUD + `PaginationInnerInterceptor(DbType.POSTGRE_SQL)` 分页（真实 PostgreSQL 16）
- **关键发现**：3.5.9+ 起分页等 JSqlParser 拦截器拆分为独立 artifact `mybatis-plus-jsqlparser`，必须显式引入

运行：
```bash
cd spike/m0-spike1-mbplus
mvn -o test   # 使用 embedded-postgres 16.14（Windows amd64 二进制），无需外部 PG
```

## Spike ② `m0-spike2-rag` — Spring AI 2.0 RAG 链路

验证点：
- 四模型 ChatClient 注册/路由（Anthropic / OpenAI / Gemini / DeepSeek），`ModelRouter` 按 key 切换
- `PgVectorStore` → `vectorTableName: kb_chunk` 自建表，`vector(1024)` + HNSW `vector_cosine_ops`
- `RetrievalAugmentationAdvisor`（Modular RAG：`VectorStoreDocumentRetriever` + `ContextualQueryAugmenter`）
- `ChatClient.stream()` → SSE（`POST /api/chat/ask`，text/event-stream）
- 多模型下自动配置的 `ChatClient.Builder` 因 `getIfUnique` 不会创建，需手动 `ChatClient.builder(model).build()`

运行（需本地 PostgreSQL 16 + pgvector 0.8，端口 5433，库 `iknow`）：
```bash
cd spike/m0-spike2-rag
mvn -o test                    # 集成测试（写 kb_chunk + 相似度检索 + advisor 装配）
RUN_DEMO=true mvn spring-boot:run -Dspring-boot.run.arguments="--spring.main.web-application-type=none"  # 端到端 Demo（真实 LLM 流式）
mvn spring-boot:run            # 启动 Web 服务，POST /api/chat/ask 体验 SSE
```

模型 key 全部从环境变量读取（`ANTHROPIC_AUTH_TOKEN` / `DEEPSEEK_API_KEY` / `OPENAI_API_KEY` / `GEMINI_API_KEY`），**不落库、不提交**。

## 版本锁定结论（2026-08-13 实测）

| 组件 | 版本 | 结论 |
|------|------|------|
| Spring Boot | 4.0.7 | 可用 |
| MyBatis-Plus | 3.5.17 (`mybatis-plus-spring-boot4-starter` + `mybatis-plus-jsqlparser`) | 可用 |
| Spring AI | 2.0.0 (BOM) | 可用 |
| PostgreSQL | 16.14 | 可用 |
| pgvector | 0.8.6 | 可用 |
