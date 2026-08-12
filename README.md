# iknow-youknow

Iteration 0 backend skeleton for a knowledge-base RAG service. Requires Java 21 and Maven.

```bash
mvn spring-boot:run
```

HTTP Basic development users: `user/changeit` and `admin/changeit`. The public liveness probe is `/actuator/health`; API contracts are under `/api/v1`. The default data store is H2 for the MVP; production should use PostgreSQL with pgvector and replace `DeterministicModelAdapter` with a Spring AI 2.x provider adapter.
