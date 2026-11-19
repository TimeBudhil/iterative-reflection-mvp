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
