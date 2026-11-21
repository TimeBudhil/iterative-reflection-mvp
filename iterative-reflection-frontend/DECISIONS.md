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
- **The feed card also fetches the experience (R3.2):** the card lacks `lastViewedPage`. The GET has no side effects. _(Superseded: see "Feed card (ticket 09)", since the real card does carry `lastViewedPage`.)_
- **Concurrent-edit copy (R5.6):** the backend's 409 is an optimistic-lock collision. Requests carry no version, so "changed in another tab" can't be promised.
- **CSRF bootstrap (R11.3):** `csrf.spa()` may not have issued `XSRF-TOKEN` before the first sign-in POST, and the token may rotate at login. The client primes it with `/api/me` and retries once on a CSRF 403. _(Superseded: see "Typed API client (ticket 02)" for the verified behaviour. The cookie is cleared, not rotated, and priming happens whenever it is missing.)_
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
- **Paged queries use one client-level `querySerializer`:** the generated schema models Spring Pageable as one object param, which openapi-fetch serialises in a form the backend ignores (page 2 returned page 1). `serializeQuery` in `src/lib/api/client.ts` flattens `pageable` into `page=N&size=M` for every call, so callers just pass `params.query.pageable` and never their own serializer.
- **Rotate returns 200 `{joinCode}`**, and the profile query is invalidated so the new code appears.

## Admin counselor management (ticket 19)

- **Reusable list hook:** `useAdminCounselors(page, size)` in `src/lib/admin/counselors.ts` (pageable flattened by the client-level `serializeQuery`). Ticket 21 can call it with `size=100` and build an id-to-name map with `counselorNameMap` (`counselor-model.ts`). Mutations invalidate `queryKeys.adminCounselorsAll`.
- **Create dialog** swaps to a "Counselor created" state carrying the share-the-temporary-password notice; the list jumps to page 1 (newest first) so the new row is visible. 409 maps to the email field, 400 `errors[]` to their fields.
- **Forced-change e2e** only asserts the new counselor lands on `/change-password`; the full flow stays in `forced-password-change.spec.ts` (its sign-out assertion was loosened to accept the `?next=...&expired=1` that ticket 04 adds).
- **Dev gotcha:** a scratch H2 file that outlived a long pause started rejecting `users` inserts (`users_role_ck`, surfaced as a misleading 409 "email exists"); deleting the DB fixed it.

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
- **403 handled in the page:** the page shows "This student hasn't shared their progress with you." itself (the backend 403s identically for unlinked, revoked, deactivated counselor and nonexistent student). `SessionEvents` still refetches `/api/me` on that 403 (a changed role still re-runs the guards) but skips the generic no-access panel for `/counselor/students/[id]`. _(Superseded: see "Code review fixes". The page now opts out with `useOwnForbiddenCopy()`, and the session module knows no routes.)_
- **Affinity scores are not shown:** R9.5 asks for tag + category only.
- **E2E seeding:** `e2e/support/seed-student.ts` builds a linked student with reflections, an abandoned career and intake through the real API (the demo students have no activity).

## Student counselor links (ticket 16)

- **Link errors are mapped by status, not backend text:** the backend says "Unknown join code." / "Already linked to this counselor." / "Too many failed link attempts. Try again later."; the UI shows the spec wording (R8.5) from `src/lib/counselor-links/model.ts`.
- **Codes are normalized client-side** (whitespace stripped, upper-cased) since the backend is case-insensitive.
- **List is one request with `size=100`** and the client-level pageable serializer (no pager in the card); link and revoke invalidate `queryKeys.counselorLinks`.
- The 429 and Inactive-badge e2e cases mock the response so the real 10-failure limiter and demo data stay untouched.


## Experience viewer (ticket 10)

- **Mode-driven viewer:** `src/lib/experience/model.ts` holds the pure rules (`ViewerMode = "in-progress" | "review"`, `modeFor`, `startPage`, `viewerState`). `viewerState` already exposes `showEditReflection` and `freeNavigation` (review), so tickets 11 and 14 add their buttons in the marked "mode actions" block of `src/components/experience/viewer.tsx` without restructuring.
- **Mode needs both queries:** the viewer waits for the experience and the reflections list (`queryKeys.reflections`, size=100) before rendering, so it never flashes the wrong mode. Review mode starts at page 1 and makes no `viewed` calls (the in-progress guard would 409 them).
- **Pageable on the wire:** the generated schema types `pageable` as one object, but Spring wants flat `?page=0&size=100`, which the client-level `serializeQuery` (`src/lib/api/client.ts`) now handles. Other tickets reading that list should reuse this hook and key.
- **Unavailable state** covers a 404/409 from the experience GET and from `viewed`.

## Student signup and terms (ticket 06)

- **Signup error mapping (R1.6):** the backend's under-13 400 has only `detail` (no `errors[]`) and a bad counselor code is a 404 with no field, so the form maps by status: 400+`errors[]` to fields, 400 mentioning "13" to birthdate (the `isUnder13` predicate), 404 to the code field, 409 to email. Under-13 is also checked client-side. Signup does not start a session, so the form signs in right after the 201.

## Session expiry and error routing (ticket 04)

- App-level 401/403 reactions live in `src/lib/session/events.tsx` (plugged into the API client hooks); the pure rule `identityChanged` is in `model.ts`.
- Signing out sets a short-lived flag so the cache-clear refetch 401s are not reported as an expiry. Back after sign-out lands on `/signin?next=...&expired=1` (the guard's normal 401 path); the ticket 03 e2e was relaxed to accept the query.
- Error copy never shows backend `detail` (problem bodies have none); 5xx/network show a retry panel with `X-Request-Id` when present.
- Playwright `expect` timeout raised to 15s: first-hit dev compiles on this machine exceed 5s.

## Intake and intake prompt (ticket 15)

- **New vs edit** is decided by `GET /api/intake`: the 404 (it has no `detail`) maps to `null` in `useIntake`, so a missing intake is data, not an error. `null` means POST and then the feed; otherwise PUT and then Settings.
- **PUT replaces every answer**, so `buildRequest` always sends the whole form (blank answers dropped); clearing a field in edit mode removes that answer.
- **409 handling:** only the "Reflect on at least 3 careers..." detail swaps the form for "The intake opens after your 3rd reflection."; the other 409 ("already submitted") shows the written `statusCopy(409)` message ("That can't be done right now."), never the backend `detail`.
- **Prompt modal** (`IntakePromptModal` on the feed) is driven by `intakePromptDue` from `/api/me`. Both "Not now" and "Fill it in" call `POST /api/intake/dismiss` first (R3.5: it must not return after either); Esc/overlay counts as "Not now".
- **Interests card** is a child of `<SettingsPage>` in `src/app/student/settings/page.tsx`, after "My counselors".

## My Journey (ticket 13)

- The shared `useReflectedCareers` query now walks every page at `size=100` (stops at `page.totalPages`) and returns `{ content }`. My Journey, review mode and the reflect page all use it; there is no second reflections query.
- Pure model in `src/lib/journey/model.ts`; `reflectionFor(careerId)` is for ticket 14. Averages are per answer `key` in first-seen order, shown to one decimal.
- "Edit reflection" links to `/student/careers/[id]/reflect` (ticket 14 owns prefill and PUT there).
- The >100 reflections pagination loop is not covered by e2e (would need 101 reflections).

## Admin audit log (ticket 21)

- `useAuditLog(page, studentId?)` (`src/lib/admin/audit.ts`) sends `page=N&size=20[&studentId=N]` via the client-level `serializeQuery` (the generated Pageable object param is ignored by Spring). Key: `queryKeys.adminAudit`.
- Counselor names come from `useAdminCounselors(0, 100)` + `counselorNameMap`; ids missing from the first 100 counselors (or while the list is unavailable) render as `#id` (`counselorLabel`).
- The student-ID filter is applied on submit (not per keystroke), validated client-side as a positive integer (leading `#` tolerated), and resets to page 1. Timestamps use the browser-local `toLocaleString()`.

## Ticket 14: edit reflection

- `/student/careers/[id]/reflect` decides create vs edit once on mount from `reflectionFor(careerId)`, so a first submit does not flip into edit mode while navigating away. Edit PUTs and returns to My Journey; the PUT 409 "changed concurrently" shows the R5.6 copy, refetches the reflections list and remounts the form (keyed by `updatedAt`) with the latest values.

## Code review fixes

- **Server field messages are shown, `detail` never is (R1.5):** the per-field `fieldErrors` messages from a 400 render verbatim under their inputs (profile, password, change-password, sign-up, admin counselor form). R1.5 explicitly asks for each `errors[]` field under its input, so this is accepted. The free-text `detail` is never rendered; everything else uses `statusCopy`.
- **Viewing a page refreshes the feed and the experience:** `useMarkViewed` invalidates `queryKeys.feed` and `queryKeys.experience(careerId)`, so "Continue (page n of m)" and the resume page are never stale.
- **One of each:** `useChangePassword` (session, refetches `/api/me`), `validatePasswordChange` and `mapPasswordChangeError` (`src/lib/validation/rules.ts`), `mapFieldErrors`, `EMAIL_TAKEN` and the 429 copy (`src/lib/api/error-copy.ts`), `fetchMe` (`src/lib/session/session.tsx`). Password length is counted in bytes (72) everywhere; the confirm-mismatch copy is "Passwords don't match."
- **Pages with their own 403 copy** call `useOwnForbiddenCopy()` (`src/lib/session/access.ts`) while mounted; the global 403 handler skips the no-access panel then. The session module knows no routes.
- **Backend-text matching** lives in named, unit-tested predicates in the models: `isNotYetAvailable` (intake), `isUnviewedLastPage` (reflection), `isConcurrentEdit` (reflection), `isUnder13` (signup).
- **E2E helpers** (`signIn`, `xsrf`/`csrfHeaders`, `signUpStudent`, `freshStudent`) live in `e2e/support/helpers.ts`. Specs use Playwright's `baseURL` and relative `/api/...` through the Next rewrite, so they run on any `NEXT_PORT`/`BACKEND_PORT`; the seeding helpers read `baseURL` from the running project.

## Tooling skeleton (ticket 01)

- **Fonts come from the `geist` npm package** (`geist/font/sans`, `geist/font/mono`), bundled and served from the app's origin. No Google Fonts fetch (N2.6).
- **`engines.node` is `>=24`, not strict:** `.nvmrc` pins 24 for CI and deploys, but local Node 25 still works.
- **`typecheck` runs `next typegen` first,** because `LayoutProps`/route types are generated. `start` runs the standalone `server.js`.
- **shadcn set up by hand** (`components.json`, `cn`, primitives copied in), not via its CLI. Regenerating a primitive with the CLI can undo local fixes (see the dialog focus fix under ticket 24).

## Forced password change (ticket 07) and sign-in landing

- **`mustChangePassword` takes priority over `?next=`:** `postSignInPath(me, next)` (`src/lib/session/model.ts`) sends a user on a temporary password to `/change-password` whatever `next` says. Otherwise a safe `next` is honoured, and then the role home. `landingFor(me)` is the same rule without `next`, used by `/`, signed-in `/signin` and signup's auto sign-in. Decided while merging tickets 04 and 07.
- While the flag is set, the guard allows only `/change-password` (and sign-out).

## Abandon a career (ticket 11)

- `useAbandonCareer` invalidates `queryKeys.feed` and removes the experience cache entry, so the new card shows at once. A failed abandon closes the dialog and shows the `statusCopy` text as a toast.

## Submit a reflection (ticket 12)

- **Double submit is blocked on the client (R5.5):** a repeated `POST` doesn't return "already reflected". It returns the in-progress guard's 409 ("Finish your current career..."). So a ref lock plus the disabled button stop the second request.
- **409s on submit:** the unviewed-last-page 409 (`isUnviewedLastPage`) sends the student back to the experience (R5.6). Any other 409 shows "not open for reflection" with a link to the feed.
- The form is one pure model (`src/lib/reflection/form.ts`) plus `ReflectionForm`, shared by create (POST) and edit (PUT, ticket 14). The 1–10 scale is a radio-based `SegmentedControl`, so arrow keys work natively.

## Admin metrics (ticket 20)

- **Plain CSS bar list, no chart library:** keeps third-party code out (N2.6) and is readable by screen readers as a labelled list with counts.
- **Date filter lives in the URL** via `router.replace` (no history entry per edit). Empty dates are left out, so clearing both returns to all-time. A ref tracks the latest range so quick successive edits don't overwrite each other.

## Full smoke suite (ticket 22)

- `smoke/full-flow.spec.ts` drives admin → create counselor → forced change → student signup with the join code → feed card, all through the UI.
- **`Secure` cookie is asserted only when the base URL is https,** so the same spec can dry-run on plain HTTP.
- **Not run against the rehearsal:** no docker/mkcert/Firefox on the dev machine. It passed a dry run against the dev stack (`SMOKE_BASE_URL=http://localhost:<port>`, demo admin standing in for the bootstrap admin) on Chromium and WebKit only.

## Per-role E2E (ticket 23)

- **Sign-in throttle and repeat runs:** the backend locks an IP out for 15 minutes after 5 failed sign-ins, and full suites trigger it. Restart the backend between full runs (or raise `identity.rate-limit.*` via dev run args). `pnpm e2e:reset` (`rm -rf ../mvp/data`) wipes the dev DB. Unique `e2e+<timestamp>` emails keep runs from colliding.

## Accessibility, keyboard and responsive (ticket 24)

- **Sidebar collapses below 1280 px (N4.2):** `portal-shell.tsx` shows a top bar with a "Menu" button (`aria-expanded`) holding the nav and user menu, and it closes after a link is picked. 1280 was chosen because the fixed sidebar crowded the content at 1024 and 768.
- **Dialogs restore focus to their opener:** dialogs are opened from plain buttons, not `DialogTrigger`, so Radix had nowhere to return focus. `src/components/ui/dialog.tsx` handles `onCloseAutoFocus` to restore it. Don't overwrite it by regenerating the shadcn primitive.
- axe runs WCAG 2 A/AA and fails on serious/critical only. Keyboard and layout checks ran on Chromium only.

## Performance, third-party and error copy (ticket 25)

- **Lighthouse is measured on the production standalone build.** For signed-in pages, the session cookie is set in a Playwright Chromium and Lighthouse attaches to it (`--port`, `--disable-storage-reset`), because passing the cookie as a header didn't work.
- The third-party check allows cross-origin requests only from inside embed iframes (the CSP `frame-src` hosts).
- The error-copy sweep replaced three places that rendered raw `detail` (admin metrics, change-password, intake 409) with `statusCopy`. `e2e/error-copy.spec.ts` injects a poison `detail` on every page's requests to keep it that way.
- The shared `RetryPanel` (`src/components/shared/retry-panel.tsx`) is the merged version of tickets 04 and 17. Its button reads "Try again", and the sign-in form shows the request ID only for 5xx and network errors.

## Visual identity: Grove (update 1)

- **Palette and slots:** Grove is `background #edefeb`, `surface #ffffff`, `foreground #121511`, `primary #315c3b` (+ white text), `highlight #e5c96b`, `highlight-text #7f6716`, `positive #d6e4e8` (+ `#315c3b` text) (primary and highlight revised in update 2; update 1 shipped `#163300` / `#f08952` / `#a25a0e`), neutral `muted`/`muted-foreground`, and `destructive` red (`#e7000b`) with `destructive-foreground` `#ffffff`. Slots: background, surface, foreground, muted, muted-foreground, border, primary, primary-foreground, highlight, highlight-text, positive, positive-foreground, destructive, destructive-foreground, ring. Components use slot names only; raw values live in `globals.css`, and `tokens.test.ts` guards both the AA pairs and the raw-color rule. Keep the slot names stable: update 2 presets must define every slot.
- **Lime dropped as a leading color:** forest green with lime is a well-known fintech brand's signature. Leading with orange keeps Grove distinct.
- **NO isn't red:** a NO verdict is a legitimate, useful answer ("not for me"), not an error. It's neutral `muted`; red is kept for destructive actions and Denied.
- **`highlight` is fill-only:** the highlight (update 1 `#f08952` about 2.5:1 on white; now gold `#e5c96b`, about 1.6:1) is, below the 3:1 bar for meaningful graphics and far below 4.5:1 for text. It is used for bars and always paired with text; orange text uses `highlight-text`.
- **MAYBE chip is a real tint, painted over an opaque surface:** a translucent `highlight` tint fails AA over the page `background` (10% gives 4.21:1) but passes over white (10% gives 4.81:1). The chip therefore stacks `bg-surface` with a `highlight/10` gradient layer and a `highlight` border, so it reads about 4.8:1 wherever it sits. `tokens.test.ts` asserts highlight-text on that blended tint. (This replaces the earlier outlined-chip workaround.) The check also covers the `muted-foreground` pairs used by NO chips.
- **Selected verdict is marked without color alone:** in single-select segmented controls the selected option gets a 2px `foreground` outline and a check icon on top of its verdict fill (selected NO is only `muted`, about 1.1:1 on the page, so the fill alone was not enough). The outline is `foreground` on `background`/`surface`, asserted at 3:1 or better (WCAG 1.4.11). The outline is separate from the focus ring so both show.
- **`destructive-foreground` slot:** destructive buttons used `primary-foreground`, which only works while the two happen to match. A separate slot (Grove `#ffffff`) is added, with a contrast pair in the token check. Update 2 presets must define it.
- **Review-mode page jumper:** the current page button is `outline` with a heavier `foreground` border and `aria-current="page"`, not primary-filled, so "Edit reflection" is the screen's one primary button.
- **One verdict type:** `src/lib/verdict.ts` exports `VERDICTS`, `Verdict`, `VerdictCounts` and `isVerdict`; `verdictStyle` takes a `Verdict`, and `VerdictBadge` renders nothing for unknown values instead of falling back to NO.
- **ProgressBar takes a fraction:** the bar fill comes from the model's `progress` / `fraction`; `value` and `max` are only the ARIA numbers.
- **Explored count has no total:** no endpoint exposes the number of careers, and the backend is frozen. "n careers explored" counts reflections (abandons are invisible to students) and is hidden at 0. An "of N" total waits for update 3.

## Theme presets (update 2)

- **Four value sets, same 15 slots:** all share `surface #ffffff` and `destructive #e7000b` / `destructive-foreground #ffffff`. Each also passes the token check pairs and the MAYBE-chip tint check.

  | Preset | background | foreground | primary | highlight | highlight-text | positive (+ text) | muted / muted-foreground | border |
  |--------|-----------|-----------|---------|-----------|----------------|-------------------|--------------------------|--------|
  | grove (default) | `#edefeb` | `#121511` | `#315c3b` | `#e5c96b` | `#7f6716` | `#d6e4e8` (`#315c3b`) | `#e2e5df` / `#565c53` | `#d3d7d0` |
  | harbor | `#ebeff1` | `#10181d` | `#12344d` | `#3aa99f` | `#17716a` | `#efe5d2` (`#12344d`) | `#dfe5e8` / `#4f5d65` | `#ced7dc` |
  | plum | `#f1edf0` | `#1a1219` | `#63316b` | `#e9b96e` | `#916116` | `#e4e0ef` (`#63316b`) | `#e7e0e5` / `#5e5260` | `#dacfd7` |
  | slate | `#eeeff1` | `#15171a` | `#2b3038` | `#6f9fd8` | `#2f5f9a` | `#dfe8df` (`#2b3038`) | `#e2e4e7` / `#555a61` | `#d2d5da` |

  `ring` equals `primary` in every preset; `primary-foreground` is white. Grove and Plum `primary` / `highlight` were chosen by the developer on 2026-11-20 (Grove `#315c3b` + `#e5c96b`, Plum `#63316b` + `#e9b96e`); their `highlight-text` values are the same hue darkened until they pass AA on the background, surface and MAYBE tint. The backend stores only the id, so palettes live in `globals.css` alone. The hex question in `REQUIREMENTS.md` is closed: these are the values.
- **Theme loading:** `GET /api/settings/theme` is fetched once per full page load (`staleTime`/`gcTime` infinite, no retry, no focus or reconnect refetch, no polling). `applyTheme()` in `src/lib/theme/presets.ts` is the only place that sets `data-theme`.
- **`localStorage` hint (`ir-theme`):** an inline head script (`THEME_HEAD_SCRIPT`) applies the last-seen id before first paint; the server answer then overwrites attribute and hint. The hint holds only a preset id, never anything auth-related, and unknown or blocked storage counts as no hint.
- **Fallback to grove:** a failed request, an unknown id or a missing value resolves to `grove` with no toast, error or retry panel.
- **Saves apply immediately:** on a successful save the admin's page updates with no reload (attribute, hint and both theme queries). Other users see it on their next full load; an open tab keeps its theme.
- **Denied badge is a solid fill (found in close-out):** the Denied outcome badge used `bg-destructive/10 text-destructive`, which failed color contrast on `/admin/audit` (destructive text on a faint tint over the tinted page background). It now uses `bg-destructive text-destructive-foreground`, a pair the token check already asserts for all four presets.

### N5.3 brand check

**Draft, awaiting developer sign-off.** Drafted from general knowledge of well-known brand palettes (no live lookup), so treat the "nearest brands" column as a prompt for review rather than a legal opinion. Already avoided: plum + orange, plum + gold, charcoal + yellow, and forest green + lime.

| Preset | `primary` + `highlight` | Nearest well-known pairings | Why it isn't a look-alike |
|--------|-------------------------|-----------------------------|---------------------------|
| grove | forest green `#315c3b` + soft gold `#e5c96b` | Green Bay Packers (dark green `#203731` + gold `#ffb612`); John Deere (green `#367c2b` + yellow `#ffde00`); Oregon Ducks (green + yellow); Wise (dark green + lime, dropped in update 1) | **Needs a look (developer's choice, 2026-11-20).** Green + gold is a well-known sports and agriculture pairing. Grove's gold is muted and used as a fill only, and the green is grayer than Deere's, so it reads as earthy rather than branded, but it is closer to the Packers than the old orange was |
| harbor | navy `#12344d` + teal `#3aa99f` (sand `positive`) | Corporate navy + red (Capital One, Citi); navy + bright blue (many banks and SaaS); teal-forward brands such as Xero | The accent is a muted teal on a dark steel navy, with a warm sand `positive`; no red, no bright blue, and no brand uses this trio |
| plum | plum `#63316b` + amber-gold `#e9b96e` (lavender `positive`) | Cadbury (purple + gold); Crown Royal (purple + gold); LSU and the Minnesota Vikings (purple + gold); Slack (aubergine `#4a154b`) | **Needs a look (developer's choice, 2026-11-20).** This is the plum + gold pairing listed above as already avoided. The plum is lighter and redder than Cadbury's purple, and the gold is a soft amber fill, not a metallic logo gold. Moving `primary` away from `#4a1f4f` also clears the Slack resemblance |
| slate | charcoal-slate `#2b3038` + sky blue `#6f9fd8` (sage `positive`) | GitHub (dark gray with blue links); Linear and Vercel-style dark neutrals; Facebook and X (blue alone) | The slate is a neutral and the blue is a soft, light sky blue used as a fill only. Neutral-dark plus blue is a generic pairing, not a distinctive brand mark |

Sign-off: _pending developer._
