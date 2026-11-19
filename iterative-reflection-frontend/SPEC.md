# Iterative Reflection Frontend: Spec

_Written 2026-11-18. Source of truth for requirement IDs: `REQUIREMENTS.md`. IDs in brackets (e.g. [R4.5]) point there; [B-…] IDs point to `../mvp/REQUIREMENTS.md`._

## Problem Statement

The career-discovery API in `../mvp` is finished and frozen, but nobody can use it. A high-school student has no screen to sign up on, get a career card, work through its pages, reflect on it and see where they're leaning. A counselor can't see the students who linked to them, and the admin can't create counselors or read the pilot metrics. On top of that there's no way to run the whole product the way it would run in production (HTTPS, a real database, one origin), so nobody knows whether it would actually work once deployed.

## Solution

A Next.js web app with three portals (student, counselor, admin) that drives the existing 31 endpoints as they are, filling any API gaps on the client:

1. A student signs up (13+), is signed in automatically, and gets one career card at a time. They step through the career's pages, reflect on it with 1–10 scales and a YES / MAYBE / NO verdict, or abandon it. After 3 reflections they're asked to fill in the intake. My Journey shows their saved (YES + MAYBE) careers and progress stats.
2. A counselor shares a join code, sees a table of linked students, and opens a read-only detail page for each one.
3. The admin reads the pilot metrics, creates and (de)activates counselors, and reads the audit log.
4. Locally it runs with `pnpm dev` next to `./mvnw spring-boot:run`. For production it builds into a Node `standalone` server, and a root `deploy/` docker-compose rehearses the prod setup on `https://localhost`: nginx, Next, the Spring backend with the prod profile, and Postgres.

## User Stories

### Visitor and account

1. As a new student, I want to sign up with email, password, display name and birthdate, so that I can start exploring careers.
2. As a new student, I want to be signed in automatically after signing up, so that I land straight on my first career.
3. As a new student, I want a "12+ characters" hint and an early block on short passwords, so that I don't submit a form that's bound to fail.
4. As a student under 13, I want a clear message saying I must be 13 or older, so that I understand why I can't sign up.
5. As a student whose email is already registered, I want to be told that, so that I sign in instead.
6. As a student with a counselor code, I want to enter it at signup, so that my counselor is linked from the start.
7. As a student who mistyped the counselor code, I want the error on the code field and my other answers kept, so that I fix only the code.
8. As a student, I want to read the terms before accepting them, and the submit button to stay disabled until I do, so that my consent is meaningful.
9. As any user, I want to sign in with email and password, so that I reach my own portal.
10. As any user, I want a wrong email or password to give one generic message, so that nobody learns which emails are registered.
11. As any user who has been throttled, I want to be told to wait 15 minutes, so that I stop retrying in vain.
12. As a signed-in user, I want `/` and `/signin` to take me to my home, so that I never see a sign-in form I don't need.
13. As a user whose session expired, I want to be sent to sign-in with a notice and returned to the page I was on, so that I don't lose my place.
14. As a user, I want a malicious `next` link to be ignored, so that sign-in can't redirect me to another site.
15. As a counselor or admin on a temporary password, I want to be forced to change it before anything else, so that the temporary password stops working.
16. As any user, I want to sign out from the user menu, so that a shared Chromebook is safe, and Back doesn't show my data.
17. As any user, I want an unknown URL to show a 404 with a link home, so that I'm never stuck.
18. As any user who opens another role's pages, I want to be redirected to my own home without seeing a flash of their content, so that the portals feel separate.
19. As any user who hits an unexpected "access denied", I want the app to recheck my account and either redirect me or explain, so that I'm not shown a broken page.
20. As a user with a placeholder terms page, I want it clearly marked as a draft, so that nobody mistakes it for legal text.

### Student: feed and experience

21. As a student, I want my feed to show exactly one career card with title, summary, tags and estimated minutes, so that I focus on one career.
22. As a student, I want the button to say "Continue (page n of m)" when I've started, so that I know how far I got.
23. As a student, I want the same card after a refresh or a new sign-in, so that I don't lose my place.
24. As a student who has seen every career, I want a friendly "You've explored every career" message linking to My Journey, so that the end doesn't look like an error.
25. As a student who has 3 reflections, I want a one-time prompt to fill in the intake, with "Not now" respected, so that I can personalize my feed when I choose.
26. As a student, I want to read a career one page at a time with Previous / Next and a page indicator, so that the experience is paced.
27. As a student, I want text, video, audio, images and links to all render properly, so that the experience feels rich.
28. As a student, I want my page progress saved as I go, so that I resume where I left off.
29. As a student, I want the reflect button only on the last page, so that I reflect after seeing everything.
30. As a student, I want to say "This isn't for me" after a confirm, so that I can skip a career I already dislike and get a new card.
31. As a student, I want to reopen a career I've reflected on in review mode with free navigation, so that I can revisit it without being pushed to abandon it.
32. As a student, I want review mode to work from a bookmarked URL too, so that links don't break.
33. As a student who opens a career that isn't available, I want a clear message and a link to the feed, so that I'm not stuck.

### Student: reflection

34. As a student, I want one scrolling form with a 1–10 scale per question and optional open answers with a character counter, so that reflecting is quick.
35. As a student, I want to give a YES / MAYBE / NO verdict on "Would I genuinely enjoy this?", so that the app knows where I'm leaning.
36. As a student, I want submit disabled until every scale and the verdict are set, so that I can't send an incomplete reflection.
37. As a student, I want server errors shown next to the matching question, so that I know what to fix.
38. As a student, I want a double-click to send only one reflection, so that I don't create duplicates.
39. As a student, I want to edit an earlier reflection with my previous answers prefilled, so that I can change my mind.
40. As a student who reflects before viewing the last page, I want to be sent back to the experience, so that I finish it.
41. As a student whose edit collided with another save, I want to be told and given fresh data, so that I don't overwrite something blindly.

### Student: My Journey, intake and settings

42. As a student, I want My Journey to list my YES and MAYBE careers by default, newest first, so that I see my saved careers.
43. As a student, I want filter tabs for YES, MAYBE, NO and All, so that I can look at any group.
44. As a student, I want a stats strip with counts per verdict and my average per dimension, so that I see my patterns.
45. As a student with no reflections, I want an empty state pointing to the feed, so that I know how to start.
46. As a student, I want the intake to show tag questions as grouped chips and text questions as textareas, so that it's quick to fill in.
47. As a student, I want to edit my intake later, prefilled, so that my interests stay current.
48. As a student with fewer than 3 reflections, I want to be told the intake opens after the 3rd, so that I'm not confused by a refusal.
49. As a student, I want to change my display name and see it update right away, so that I control how counselors see me.
50. As a student, I want to change my password and stay signed in, so that security doesn't cost me my session.
51. As a student, I want to see my linked counselors, including inactive ones, so that I know who can see my data.
52. As a student, I want to link a counselor by code with clear copy for each failure, so that linking later is easy.
53. As a student, I want to revoke a counselor link after a confirm naming them, so that I control who sees my reflections.
54. As a student, I want Settings to show whether my intake is filled in, so that I know whether my feed is personalized.

### Counselor

55. As a counselor, I want to see and copy my join code, so that I can give it to students.
56. As a counselor, I want to rotate my join code after a confirm, so that a leaked code stops working while students stay linked.
57. As a counselor, I want a paginated table of linked students with their counts, intake status and last-active time, so that I see who's engaged.
58. As a counselor with no students, I want an empty state telling me to share my code, so that I know the next step.
59. As a counselor, I want a read-only detail page per student (top interests, reflections, abandoned careers, intake answers), so that I can prepare for our conversation.
60. As a counselor whose student revoked access, I want a clear "hasn't shared their progress with you" message, so that I understand why.
61. As a counselor, I want to change my name and password, so that my account is mine.

### Admin

62. As the admin, I want tiles for total students, % with ≥3 reflections and % intake completed, plus a verdict chart, so that I can judge the pilot.
63. As the admin, I want a signup date filter kept in the URL, so that I can compare cohorts and share the view.
64. As the admin, I want a paginated counselor list with status and linked-student counts, so that I can manage counselors.
65. As the admin, I want to create a counselor with a temporary password and be reminded to share it, so that onboarding is simple.
66. As the admin, I want to deactivate or reactivate a counselor after a confirm that warns of immediate sign-out, so that I can cut access safely.
67. As the admin, I want an audit log with counselor names, student IDs, allowed/denied badges, local times and a student filter, so that I can check who viewed what.
68. As the admin, I want to change my name and password, so that my account is mine.

### Everyone: quality

69. As any user, I want skeletons while pages load and a retry panel with a request ID on unexpected errors, so that failures are recoverable and reportable.
70. As any user, I want every action to update the screen without a manual reload, so that the app feels live.
71. As a keyboard or screen-reader user, I want every flow, including the 1–10 scale and dialogs, to work and announce errors, so that the app is accessible.
72. As a user on a 1024 px or 768 px screen, I want no horizontal scroll and every action reachable, so that smaller laptops work.
73. As a student, I want no third-party trackers, scripts or fonts, so that my data stays with the school's service.

### Developer and operator

74. As the developer, I want `pnpm dev` to work against `./mvnw spring-boot:run` with no extra setup, so that local work is quick.
75. As the developer, I want API types generated from the dev backend's OpenAPI and committed, so that a contract drift breaks `pnpm typecheck`.
76. As the developer, I want E2E runs that don't collide with old data and a reset script, so that tests are repeatable.
77. As the developer, I want lint, typecheck and unit tests in CI on every push, so that regressions are caught early.
78. As the operator, I want a `standalone` build that needs no runtime env vars, so that one image runs anywhere.
79. As the operator, I want a docker-compose rehearsal with nginx, HTTPS, the prod backend profile and Postgres, so that I know the prod setup works before the EC2 phase.
80. As the operator, I want a smoke test against the rehearsal on all three browser engines, so that cookies, CSRF, routing and HTTPS are proven together.
81. As the operator, I want the backend image built without changing `../mvp`, so that the frozen backend stays frozen.

## Implementation Decisions

### Modules

- **API client (deep module).** The only way the app talks to the backend. Callers ask for typed operations (based on the generated OpenAPI schema) and get back data or a single `ApiError` shape: `{status, detail, fieldErrors, requestId}`. Inside it hides: `credentials: 'include'`; copying `XSRF-TOKEN` into `X-XSRF-TOKEN` on POST/PUT/PATCH/DELETE; making sure the CSRF cookie exists before the first mutation (via `GET /api/me`); retrying once after a CSRF 403; parsing problem+json, including `errors[]` into per-field errors; reading `X-Request-Id`. It has two hooks the app layer plugs into: `onUnauthorized` (401 outside sign-in → session-expired redirect, R2.3) and `onForbidden` (unexpected 403 → refetch me and re-guard, R2.7).
- **Session module.** Owns the `/api/me` query and everything derived from it: role → home path, the role guard used by each portal layout (skeleton until the role is confirmed), the forced-password-change redirect, sign-out (clears the query cache), and a pure **safe-return-path** function: `(next, role) → path`. It only accepts same-site paths under the user's own role prefix, and otherwise returns the role home (R2.3, N2.5).
- **Query layer.** One set of query keys and hooks per resource (me, feed, experience, reflections, reflection questions, intake, counselor links, counselor dashboard, admin lists). Each mutation declares which keys it invalidates (R11.5). The reflections query (all pages at `size=100`) is shared by My Journey, review-mode detection (R4.5) and edit prefill (R5.4).
- **Journey model (pure).** `ReflectionView[] → {counts per verdict, average per dimension, filtered and sorted lists per tab}` (R6.2, R6.3). Also exposes `reflectionFor(careerId)` for review mode and prefill.
- **Reflection form model (pure).** Takes the active questions and a draft, and returns `canSubmit`, the client-side field errors, and the `SubmitRequest` payload (`answers` keyed by question key, plus `verdict`). Merges server `fieldErrors` back onto questions (R5.1, R5.2).
- **Experience viewer.** Renders the blocks (TEXT, VIDEO_EMBED, AUDIO_EMBED, IMAGE_URL, EXTERNAL_LINK) with sandboxed iframes and safe links. Handles paging, the "viewed" calls, resuming at `lastViewedPage`, and its mode: in-progress (abandon allowed, reflect on the last page) or review (free navigation, edit reflection).
- **Status-copy mapping.** Each page declares its copy per backend status (400/401/403/404/409/429/5xx), so raw backend text never shows (N5.2).
- **Shared UI.** shadcn/ui primitives, the portal shell (sidebar, user menu, collapsing at ≤1024 px), a paginated table bound to the backend's `{content, page: {size, number, totalElements, totalPages}}` shape, confirm dialog, skeletons, the retry panel and the 1–10 segmented control.
- **Deploy (root `deploy/`).** A compose file with four services: nginx, the Next `standalone` image, the backend (built with `spring-boot:build-image`, prod profile) and Postgres. Also an nginx config, `.env.example` (DB credentials, `ADMIN_EMAIL`, `ADMIN_PASSWORD`) and a README with the mkcert step.

### Architecture and contracts

- Next.js 16 App Router, React 19 with the React Compiler, TypeScript, Tailwind + shadcn/ui, TanStack Query. Pages are client-rendered and have no server-side data fetching; the Node server only serves the app shell and sets headers.
- `output: 'standalone'`, Node 24 LTS, pnpm through corepack.
- **Same origin everywhere.** In dev, the Next rewrite sends `/api/*` to `BACKEND_URL` (default `http://localhost:8080`). In compose/prod, nginx sends `/api/*` to Spring (setting `X-Forwarded-For` and `X-Forwarded-Proto`) and `/*` to Next, on `https://localhost` with a mkcert certificate. The backend's CORS setting is never relied on.
- **Config.** `NEXT_PUBLIC_TERMS_VERSION` is fixed at build time (default `2026-11-18`). `BACKEND_URL` is only for the dev rewrite. The production server has no runtime env vars.
- **CSP** in `headers()`: `frame-src` limited to the backend's 8 `career.embed-hosts`, plus `object-src 'none'`, `base-uri 'self'`, `form-action 'self'` and `frame-ancestors 'none'`. No `script-src` nonces.
- **Auth.** The session lives only in the backend's HttpOnly cookie, which is `Secure` in prod. Nothing auth-related goes in web storage. Signup doesn't start a session, so the client signs in with the same credentials straight after a 201.
- **API gaps filled on the client.** The feed card lacks `lastViewedPage`, so the feed also fetches the experience (R3.2). The experience lacks a "reflected" flag, so review mode checks the reflections list (R4.5). There's no single-reflection GET, so the edit form prefills from the same list (R5.4). The concurrent-edit 409 is an optimistic-lock collision, and its copy says so (R5.6). Counselor names in the audit log come from the admin counselor list (R10.6).
- **Generated types.** `pnpm gen:api` reads the dev backend's `/v3/api-docs`, and its output is committed.

## Testing Decisions

**A good test** drives the public interface and checks what a user or caller observes: what's on screen, which request went out, and what error shape came back. It never checks internal state, hook calls or component structure. Tests target the highest seam that can observe the behavior.

**Seams (three, highest first):**

1. **Browser against a real backend (Playwright).** This is the main seam. It's used for:
   - the per-role happy paths against the dev backend (N6.2), each also run keyboard-only (N3.2), with an axe scan of every page (N3.1);
   - one flow from a clean browser with no cookies (CSRF bootstrap);
   - the `e2e:smoke` suite against the compose rehearsal on Chromium, Firefox and WebKit (R12.5).

   Dev runs use unique `e2e+<timestamp>@…` emails, and `pnpm e2e:reset` deletes `../mvp/data`.
2. **API client's public interface (Vitest, `fetch` stubbed).** Covers CSRF header on mutations, CSRF priming and a single retry, problem+json → `ApiError` including `fieldErrors` and `requestId`, and the 401/403 hooks firing.
3. **Pure models (Vitest).** Covers journey stats and filters, the reflection form model (submit gating, payload shape, server-error merge) and the safe-return-path function (open-redirect cases).

Components are not tested on their own: anything visual is covered by seam 1.

**Prior art:** the frontend has no tests yet. The backend's HTTP-level black-box tests (the shared `HttpIntegrationTest` base and per-feature `*HttpTest` classes) set the style: drive the public API, assert observable results.

## Out of Scope

- Any change to `../mvp` (code, config or Dockerfile). Gaps are filled on the client or cut (X1).
- Real save/bookmark for careers (X2), and a student view of their top interests or abandoned careers (X3).
- Analytics or tracking (X4), PWA/offline (X5), a marketing landing page (X6), mobile-first design (X7), draft autosave (X8), counselor student search (X9).
- Email change, account deletion, data export and admin password reset (X10), a content management UI (X11), i18n (X12), phase 2/3 features (X13).
- Deploying to a real host, and the final terms text. Both are open questions in `REQUIREMENTS.md`.
- A strict `script-src` CSP with nonces.
- E2E in CI (it needs the backend). CI runs lint, typecheck and unit tests only.

## Further Notes

- Stretch items S1–S4 (reflection version history, a celebratory "seen it all" screen, dark mode, "Forgot password?" help) come after the in-scope work.
- Check the CSRF cookie behavior of `csrf.spa()` against the running backend before writing the API client's priming logic: when `XSRF-TOKEN` is first issued, and whether it rotates on sign-in.
- The prod backend profile has no demo accounts. The compose rehearsal starts with only the bootstrap admin from `.env`, which is what the smoke test relies on.
- Login throttling is in memory and per IP. Through nginx the backend sees the real client IP because of `X-Forwarded-For`, so E2E sign-in failures from one machine count against one IP.
