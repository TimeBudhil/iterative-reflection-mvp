# Iterative Reflection frontend

Next.js 16 (App Router), React 19, Tailwind + shadcn/ui, TanStack Query. See `SPEC.md` for the design.

## Running dev

Requires Node 24 (`.nvmrc`) and pnpm via corepack (`corepack enable`).

1. Start the backend (frozen, in `../mvp`):

   ```bash
   cd ../mvp && ./mvnw spring-boot:run
   ```

   It listens on `http://localhost:8080`. To use another port add
   `-Dspring-boot.run.arguments=--server.port=<port>`.

2. Start the frontend:

   ```bash
   pnpm install
   BACKEND_URL=http://localhost:8080 pnpm dev   # BACKEND_URL defaults to this value
   ```

   The dev server rewrites `/api/*` to `BACKEND_URL`, so the browser only talks to one origin.
   Check with `curl -i localhost:3000/api/me` (expect a 401 problem+json).

## Scripts

| Script           | What it does                                                         |
| ---------------- | -------------------------------------------------------------------- |
| `pnpm dev`       | Next dev server                                                      |
| `pnpm build`     | Production build (`output: 'standalone'`)                            |
| `pnpm start`     | Run the standalone server (`node .next/standalone/server.js`)        |
| `pnpm lint`      | ESLint                                                               |
| `pnpm typecheck` | `next typegen` then `tsc --noEmit`                                   |
| `pnpm test`      | Vitest                                                               |
| `pnpm e2e`       | Playwright; starts `next dev` on `$NEXT_PORT` (default 3000) and needs the backend running at `BACKEND_URL` (run `pnpm exec playwright install chromium` once) |

## Notes

- The production server needs no runtime env vars. The CSP in `next.config.ts` lists the backend's `career.embed-hosts`; keep both in sync.
