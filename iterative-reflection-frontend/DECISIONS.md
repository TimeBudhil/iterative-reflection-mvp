<!--
AGENTS / AI ASSISTANTS: DO NOT READ THIS FILE.
It is a human-facing history of scoping decisions. Every decision here is already
reflected in REQUIREMENTS.md, which is the source of truth. Reading this file only
consumes extra tokens. Open it only if the user explicitly asks about it.
-->

# Decisions Log

_Last updated: 2026-11-18_

> **Note for agents:** do not read this file unless the user explicitly asks. REQUIREMENTS.md already reflects every decision below.

## Gap review against the frozen backend (2026-11-18)

- **Review mode comes from the reflections list (R4.5):** the experience response has no "reflected" flag. A `?mode=review` param was rejected because a bookmarked URL would lose it.
- **Edit prefill reuses the reflections list (R5.4):** there's no single-reflection GET, and `ReflectionView` already carries the answers and verdict.
- **The feed card also fetches the experience (R3.2):** the card lacks `lastViewedPage`. The GET has no side effects.
- **Concurrent-edit copy (R5.6):** the backend's 409 is an optimistic-lock collision. Requests carry no version, so "changed in another tab" can't be promised.
- **CSRF bootstrap (R11.3):** `csrf.spa()` may not have issued `XSRF-TOKEN` before the first sign-in POST, and the token may rotate at login. The client primes it with `/api/me` and retries once on a CSRF 403.
- **Client-side guards, no middleware (R2.2):** middleware would need a backend call per navigation. The backend enforces permissions anyway (N2.3).
- **Unexpected 403s re-run the guards (R2.7):** N5.2 needed copy for every status, and only R9.5 covered 403.
- **Placeholder terms (R1.7):** version `2026-11-18`, marked as a draft. Real text before any school pilot.
- **"Deployable" means deploy-ready plus a local rehearsal (R12):** the backend has no deploy setup yet, so a real host waits for its EC2 phase.
- **Node `standalone`, not static export (R12.1):** static export can't use `headers()` (CSP) or plain `[id]` dynamic routes.
- **nginx splits `/api` in prod, the Next rewrite is dev-only (R11.2):** Next fixes rewrites at build time, and the extra hop would hide client IPs from the backend's login limiter.
- **Public config is build-time (N6.4):** `NEXT_PUBLIC_TERMS_VERSION` is written into the bundle. New terms mean new text, so a rebuild happens anyway.
- **HTTPS in the rehearsal via mkcert (R12.4):** the prod session cookie is `Secure`, and WebKit doesn't accept that on `http://localhost`.
- **Backend image via buildpacks (R12.3):** keeps `../mvp` frozen, with no Dockerfile added there.
- **Smoke test against the rehearsal (R12.5):** dev demo accounts don't exist in prod, so N6.2 can't run there.
- **Scoped CSP without nonces (N2.4):** a strict `script-src` needs per-request nonces, which would make every page dynamic.
- **Node 24 LTS (R12.1, N6.3).**
- **E2E data hygiene (N6.2):** unique emails per run plus `pnpm e2e:reset`, because dev H2 is file-based and keeps data.
- **Generated schema is committed (R11.4):** OpenAPI is only on in dev.
- **Repo layout:** the frontend lives in the root repo as a sibling of `mvp/`, on `spec/frontend`, with its own job in the root CI workflow.

## Typed API client (ticket 02, 2026-11-18)

- **CSRF behaviour, verified on the running backend (R11.3):** `XSRF-TOKEN` is issued on any GET that loads the token, including the anonymous 401 from `GET /api/me`; a POST never issues it. Sign-in, sign-out and `PUT /api/me/password` clear the cookie instead of rotating it, so the next mutation has no token until another GET. The client therefore primes with `GET /api/me` whenever the cookie is missing before a mutation, not just the first one.
- **A CSRF 403 is identical to a role-denial 403** (`Access is denied.`). The client re-primes after a mutation 403 and retries once only if the token changed; otherwise it is a real denial and `onForbidden` fires. `onForbidden` also fires if the retry still returns 403.
- **Sign-in/sign-up 401s don't fire `onUnauthorized`:** there it means wrong credentials, not an expired session.
- **CORS forces the dev port (R11.2):** the backend only accepts `Origin: http://localhost:5173` (`app.frontend-origin`); anything else gets a text/plain 403 "Invalid CORS request" through the dev rewrite. `pnpm dev` and Playwright default to port 5173. Other ports need the backend started with `--app.frontend-origin`.
- **Client built on `openapi-fetch`** with a custom `fetch` that holds the CSRF/error logic; errors are thrown as `ApiError` (network failure is `status: 0`).
- `vitest.config.ts` renamed to `.mts` (ESM in a CommonJS package).

## Session and portal shells (ticket 03)

- `POST /api/auth/signout` is a Spring logout filter, absent from OpenAPI, so `src/lib/api/client.ts` declares it in `ManualPaths`.
- The temporary `/debug/me` page is gone; the clean-browser CSRF e2e now signs in through `/signin`.
- Session code: pure rules in `src/lib/session/model.ts` (role home, prefix guard, nav), React in `session.tsx` (`useMe`, `useSignIn`, `useSignOut`, `RoleGuard`). Query keys live in `src/lib/query/keys.ts`. Role layouts wrap `PortalShell`.
- The 429 sign-in test mocks the response, because a real 429 would lock the shared demo accounts/IP for the other specs.

## Counselor join code and students (ticket 17)

- **Shared UI introduced here:** `PaginatedTable` (bound to `{content, page}`, 0-based page, skeleton, empty slot, retry panel), `ConfirmDialog`, `RetryPanel` (shows `X-Request-Id`), shadcn `dialog`/`table`/`sonner` (`Toaster` mounted in `providers.tsx`), `relativeTime`, `pageSummary`.
- **Paged queries need a custom `querySerializer`:** the generated schema models Spring Pageable as one object param, which openapi-fetch serialises in a form the backend ignores (page 2 returned page 1). Send flat `page=N&size=20` instead.
- **Rotate returns 200 `{joinCode}`**, and the profile query is invalidated so the new code appears.

## Settings: Profile and Password (ticket 08)

- **One shared `SettingsPage`** (`src/components/settings/settings-page.tsx`) backs all three role pages. It renders Profile then Password, then its `children`. Later cards (student My counselors, Interests) are built on `SettingsCard id title` and passed as children in `src/app/student/settings/page.tsx`; the `id` is the `#anchor`. Anchor scrolling is done in an effect because the cards mount after `/api/me` loads.
- **Toasts:** `sonner`, mounted once in `src/app/providers.tsx` via `src/components/ui/sonner.tsx` (no next-themes). Use `toast.success(...)` from `"sonner"`.
- **Wrong current password:** the backend's 400 has no `errors[]` field for it (only `detail`), so any 400 without a `newPassword` field error is shown on the current-password field.
- The `PATCH /api/me` response replaces the cached `/api/me`, so the user menu updates without a reload. A password change keeps the session; the client re-primes the cleared XSRF cookie on the next mutation.
- E2E uses a freshly signed-up student (API signup) so demo passwords never change; counselor and admin specs only check the cards and anchors.

## Deploy rehearsal (ticket 05)

- **Backend image built by `deploy/build-backend.sh`, not compose:** buildpacks (`spring-boot:build-image`) is a Maven goal, so compose just runs the tagged image. `BP_JVM_VERSION=25` matches the pom. `mvp/` is untouched (only the git-ignored `mvp/target/` is written).
- **`APP_FRONTEND_ORIGIN=https://localhost` set in compose:** the prod profile leaves `app.frontend-origin` at the dev default. nginx also sends `Host $host`, `X-Forwarded-Proto` and `X-Forwarded-For $remote_addr`, otherwise the backend's CORS check 403s every mutation.
- **`X-Forwarded-For` is `$remote_addr`, not appended:** a client cannot spoof its address to the login limiter.
- **Dockerfile copies `public/` and `.next/static` by hand:** `output: 'standalone'` omits both. The builder stage runs `mkdir -p public` since the repo has no `public/` yet.
- **Smoke config is separate (`playwright.smoke.config.ts`, specs in `smoke/`):** so `pnpm e2e` (dev backend, demo accounts) never picks them up. It uses `ignoreHTTPSErrors` because Playwright's Firefox/WebKit ignore the system trust store holding the mkcert CA. Credentials come from `deploy/.env`.
- **Not run on the dev machine:** no docker/mkcert, so the compose stack and `pnpm e2e:smoke` are unverified until someone runs the README steps.

## Feed card (ticket 09)

- **No parallel experience fetch (diverges from R3.2):** the real `GET /api/feed/next` card already carries `lastViewedPage` (and the generated schema has it), so "Start" / "Continue (page n of m)" is computed from the card alone (`src/lib/feed/model.ts`). One request, no loading flash of "Start". `queryKeys.experience(id)` is added for the career page ticket.
- **"n" is `lastViewedPage`:** Continue shows how many pages are already viewed, out of `lastPage`.

## Counselor student detail (ticket 18)

- **Reflection answers typed locally:** the generated schema types `reflections[].answers` as the intake `Answer`; the real wire shape is `{key, questionVersion, prompt, score, openText|null}`. `src/lib/counselor/student-detail.ts` declares it and maps the response to a view model, keeping API order.
- **403 handled in the page:** the global `onForbidden` hook isn't wired yet, so the page shows "This student hasn't shared their progress with you." itself (the backend 403s identically for unlinked, revoked, deactivated counselor and nonexistent student).
- **Affinity scores are not shown:** R9.5 asks for tag + category only.
- **E2E seeding:** `e2e/support/seed-student.ts` builds a linked student with reflections, an abandoned career and intake through the real API (the demo students have no activity).
