# Deploy rehearsal

Runs the production setup locally on `https://localhost`: nginx (TLS) -> Next `standalone` + Spring (`prod` profile) -> Postgres.
`/api/*` goes straight to Spring, everything else to Next. Nothing in `../mvp` is changed.

Needs Docker (with compose), [mkcert](https://github.com/FiloSottile/mkcert), Node 24 + pnpm (for the smoke test).

## 1. Certificate (once)

```bash
mkcert -install
mkcert -cert-file deploy/certs/localhost.pem -key-file deploy/certs/localhost-key.pem localhost
```

HTTPS matters: the prod session cookie is `Secure`, which WebKit refuses on plain `http://localhost`.
`deploy/certs/*` and `deploy/.env` are git-ignored.

## 2. Configuration

```bash
cp deploy/.env.example deploy/.env   # then edit; ADMIN_PASSWORD must be 12..72 characters
```

## 3. Build the backend image

```bash
deploy/build-backend.sh   # ./mvnw spring-boot:build-image (Paketo buildpacks), tag iterative-reflection/mvp:rehearsal
```

## 4. Start

```bash
docker compose -f deploy/docker-compose.yml --env-file deploy/.env up --build
```

Open <https://localhost> and sign in with `ADMIN_EMAIL` / `ADMIN_PASSWORD`. Flyway loads the career content into Postgres on first start.
Reset the database with `docker compose -f deploy/docker-compose.yml down -v`.

## 5. Smoke test

```bash
cd iterative-reflection-frontend
pnpm exec playwright install chromium firefox webkit   # once
pnpm e2e:smoke                                         # reads ADMIN_EMAIL/ADMIN_PASSWORD from deploy/.env
```

`SMOKE_BASE_URL` overrides `https://localhost`. The browsers ignore certificate errors, since Playwright's Firefox and WebKit do not trust the mkcert CA.

## Why nginx is configured the way it is

The backend rejects a request with `403 Invalid CORS request` when `Origin` does not match the request's own scheme/host/port
(and `app.frontend-origin`). nginx therefore forwards `Host $host` (`localhost`), `X-Forwarded-Proto https` and
`X-Forwarded-For $remote_addr`, and compose sets `APP_FRONTEND_ORIGIN=https://localhost` on the backend. The forwarded address also
feeds the backend's per-IP login limiter.
