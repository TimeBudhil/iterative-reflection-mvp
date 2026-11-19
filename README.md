# Iterative Reflection

A career-discovery app for high-school students. Students get one career card at a time, work through its pages, reflect on it (1–10 scales plus a YES / MAYBE / NO verdict), and see where they're leaning. Counselors see the students who linked to them, and an admin manages counselors and reads the pilot metrics.

The repo holds two applications and a production rehearsal:

```
iterative-reflection-mvp/
├── mvp/                            Backend: Spring Boot 4 (Java 25), REST API under /api
├── iterative-reflection-frontend/  Frontend: Next.js 16 (React 19), student / counselor / admin portals
├── deploy/                         Production rehearsal: docker compose (nginx + Next + Spring + Postgres)
└── .github/workflows/ci.yml        CI: backend `mvnw verify` + frontend lint / typecheck / unit tests
```

## `mvp/`: backend

A Spring Modulith application. Each feature is a top-level module under `src/main/java/iterative_reflection/mvp/`, and its internals live in `internal/`:

| Module       | What it owns                                                          |
| ------------ | --------------------------------------------------------------------- |
| `platform`   | Security (session cookie, CSRF, CORS), problem+json errors, logging   |
| `identity`   | Sign-up, sign-in, `/api/me`, password change, login rate limiting     |
| `career`     | Career catalog and the page-by-page experience content                |
| `feed`       | The student's current card and what comes next                        |
| `reflection` | Reflection questions, submitting and editing reflections              |
| `intake`     | The interests questionnaire, unlocked after the 3rd reflection        |
| `counselor`  | Join codes, student links, and the counselor dashboard                |
| `admin`      | Counselor management, pilot metrics, audit log, demo data             |

- Database migrations and seed content: `src/main/resources/db/migration` (Flyway).
- Profiles: `dev` uses file-based H2 in `mvp/data/`, demo accounts and Swagger UI. `prod` uses Postgres from env vars and has no demo data. See [`mvp/README.md`](mvp/README.md).
- Design docs: `REQUIREMENTS.md`, `SPEC.md` and `DECISIONS.md`. The backend is **frozen**, so the frontend works around its gaps instead of changing it.

## `iterative-reflection-frontend/`: frontend

Client-rendered Next.js with three portals. The browser talks to the backend only through `/api/*` on the same origin.

```
src/
├── app/            Routes: (public)/signin, terms, change-password, student/*, counselor/*, admin/*
├── components/     UI: portal shell, shared table / dialog / retry panel, experience viewer, forms
└── lib/            Logic: typed API client (lib/api), session and guards, query keys, pure models
e2e/                Playwright tests against the real dev backend
smoke/              Playwright smoke tests against the deploy rehearsal (https://localhost)
```

- Tests: Vitest covers the API client and the pure models (`src/lib/**/*.test.ts`), and Playwright covers everything visual.
- Design docs: `REQUIREMENTS.md`, `SPEC.md` and `DECISIONS.md`. DECISIONS.md records where the real backend differs from the spec. See [`iterative-reflection-frontend/README.md`](iterative-reflection-frontend/README.md) for every script.

## Running it locally

### Prerequisites

- **Java 25** (the Maven wrapper downloads Maven itself)
- **Node 24** (`iterative-reflection-frontend/.nvmrc`) and **pnpm** via corepack: `corepack enable`

### Step 1: start the backend

```bash
cd mvp
./mvnw spring-boot:run
```

It starts on <http://localhost:8080> with the `dev` profile. The first start creates the H2 database in `mvp/data/` and seeds the career content and demo accounts. Leave it running.

### Step 2: start the frontend

In a second terminal:

```bash
cd iterative-reflection-frontend
pnpm install
pnpm dev
```

It starts on <http://localhost:5173> and forwards `/api/*` to the backend on 8080.

> **Keep port 5173.** The backend's CORS setting only accepts the browser origin `http://localhost:5173`. Run the frontend on any other port and every sign-in, sign-up or save fails with `403 Invalid CORS request`. To use other ports, start the backend with
> `./mvnw spring-boot:run -Dspring-boot.run.arguments="--server.port=<backend> --app.frontend-origin=http://localhost:<frontend>"`
> and the frontend with `BACKEND_URL=http://localhost:<backend> pnpm dev -p <frontend>`.

### Step 3: sign in

Open <http://localhost:5173>. All demo accounts use the password `demo`:

| Email                    | Role      | Lands on              |
| ------------------------ | --------- | --------------------- |
| `student1@demo.example`  | Student   | `/student/feed`       |
| `student2@demo.example`  | Student   | `/student/feed`       |
| `counselor@demo.example` | Counselor | `/counselor/students` |
| `admin@demo.example`     | Admin     | `/admin/metrics`      |

You can also create a new student from the **Sign up** tab (13+ and a 12+ character password). To link them to the demo counselor, sign in as the counselor and copy the join code from `/counselor/students`.

### Step 4 (optional): run the tests

```bash
cd iterative-reflection-frontend
pnpm lint && pnpm typecheck && pnpm test     # no backend needed

pnpm exec playwright install chromium        # once
pnpm e2e                                     # needs the backend from step 1 running
```

The backend locks an IP out for 15 minutes after 5 failed sign-ins, and repeated E2E runs trigger that. Restart the backend between full runs.

### Resetting local data

Stop the backend and run `pnpm e2e:reset` from `iterative-reflection-frontend/`, which deletes `mvp/data/`. The next start reseeds everything.

## Production rehearsal (optional)

`deploy/` runs the production setup on `https://localhost`: nginx terminates TLS and sends `/api/*` to Spring (prod profile, Postgres) and everything else to the Next standalone server. It needs Docker and mkcert. Follow [`deploy/README.md`](deploy/README.md): create a certificate, write `.env`, build the backend image, run `docker compose up`, then run `pnpm e2e:smoke`.

Prod has no demo accounts. You sign in with the bootstrap admin from `deploy/.env`.
