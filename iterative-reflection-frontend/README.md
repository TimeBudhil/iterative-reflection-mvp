# Iterative Reflection frontend

Next.js 16 (App Router), React 19, Tailwind + shadcn/ui, TanStack Query. See `SPEC.md` for the design.

## Running dev

Requires Node 24 (`.nvmrc`) and pnpm via corepack (`corepack enable`).

1. Start the backend (frozen, in `../mvp`):

   ```bash
   cd ../mvp && ./mvnw spring-boot:run
   ```

   It listens on `http://localhost:8080` and only accepts the browser origin `http://localhost:5173`
   (its CORS setting, `app.frontend-origin`). Any other origin gets a text/plain 403 "Invalid CORS request"
   on every mutation, so `pnpm dev` runs on **5173**. To use other ports pass both:
   `-Dspring-boot.run.arguments="--server.port=<backend> --app.frontend-origin=http://localhost:<next>"`.

2. Start the frontend:

   ```bash
   pnpm install
   BACKEND_URL=http://localhost:8080 pnpm dev   # BACKEND_URL defaults to this value
   ```

   The dev server rewrites `/api/*` to `BACKEND_URL`, so the browser only talks to one origin.
   Check with `curl -i localhost:5173/api/me` (expect a 401 problem+json).

## Scripts

| Script           | What it does                                                         |
| ---------------- | -------------------------------------------------------------------- |
| `pnpm dev`       | Next dev server on :5173                                                    |
| `pnpm build`     | Production build (`output: 'standalone'`)                            |
| `pnpm start`     | Run the standalone server (`node .next/standalone/server.js`)        |
| `pnpm lint`      | ESLint                                                               |
| `pnpm typecheck` | `next typegen` then `tsc --noEmit`                                   |
| `pnpm test`      | Vitest                                                               |
| `pnpm e2e`       | Playwright; starts `next dev` on `$NEXT_PORT` (default 5173) and needs the backend running at `BACKEND_URL` (run `pnpm exec playwright install chromium` once) |

- `pnpm gen:api` regenerates `src/lib/api/schema.d.ts` from the running dev backend
  (`OPENAPI_URL` defaults to `http://localhost:8080/v3/api-docs`); the output is committed.

## Notes

- The production server needs no runtime env vars. The CSP in `next.config.ts` lists the backend's `career.embed-hosts`; keep both in sync.
