# iKnow · youKnow

Vue 3 + TypeScript + Vite MVP for a knowledge-base Q&A system.

## Run

```bash
npm install
npm run dev
```

The Vite dev server proxies `/api` to `http://localhost:8080`; production deployments should configure the API gateway accordingly.

## API conventions

- REST requests target `/api/v1` with an optional Bearer session token.
- Chat uses `POST /api/v1/chat/stream` and consumes `token`, `citation`, `done`, and `error` SSE events.
- The current conversation cursor is sent on subsequent requests; stopping uses `AbortController` and can reconnect with that cursor.

No source prototypes were provided, so this implementation deliberately supplies a functional responsive information architecture rather than pixel-level visual reproduction.
