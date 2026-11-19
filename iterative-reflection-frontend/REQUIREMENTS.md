# Iterative Reflection Frontend: Requirements

_Last updated: 2026-11-18 (gap review)_

> Codebase status: fresh `create-next-app` scaffold (Next.js 16, React 19, TypeScript, React Compiler). No pages are built yet, so every requirement below is `⬜ new` and the status column is left out. Decisions from the gap review are logged in `DECISIONS.md`. The backend in `../mvp` is **frozen**: this frontend must work with its 31 existing endpoints as they are. IDs like [B-R4.7] point to `../mvp/REQUIREMENTS.md`.

## Summary

**Problem:** The Spring Boot API for the career-discovery loop is done, but there's nothing a student, counselor or admin can actually use. Students need a web app to work through one career at a time and reflect on it. Counselors need a read-only view of the students linked to them. Admins need a way to onboard counselors and track pilot success.
**Users:** High-school students (13+), mostly on school Chromebooks or laptops. Counselors and one admin, on desktop.
**Success looks like:** A student can go from sign-up through 3 reflections, the intake prompt, and seeing YES/MAYBE careers in My Journey, with no help and no dead ends. A counselor can see that student's detail. An admin can create that counselor and read the pilot metrics. All three are covered by Playwright e2e tests against the dev backend.
**Constraints:** Solo developer, no deadline. Runs locally, and is deploy-ready: a Node `standalone` build plus a local docker-compose rehearsal of prod (R12). Next.js 16 App Router + React 19 + TypeScript, Tailwind + shadcn/ui, TanStack Query. **No backend code changes**, so gaps get filled on the client or cut. Desktop-first for every portal.

## Page map

Every page sits under a role prefix with its own layout (sidebar + user menu) and role guard. `(public)` pages have no sidebar.

```
/                              → redirect: signed out → /signin, otherwise → the role's home
(public)
  /signin                      Sign in / Sign up tabs (?mode=signup)              R1
  /terms                       Static terms text (version shown)                  R1.7
  /change-password             Forced first-sign-in password change (any role)    R2.4

/student                       → /student/feed
  /student/feed                Current career card, or the "seen it all" state    R3   ← home
  /student/careers/[id]        Experience: one page at a time                      R4
  /student/careers/[id]/reflect  Reflection form (new or edit)                     R5
  /student/journey             My Journey = Saved Careers + Progress               R6
  /student/intake              Intake form (new or edit)                           R7
  /student/settings            Profile · Password · My counselors · Interests      R8
  sidebar: Feed · My Journey · Settings

/counselor                     → /counselor/students
  /counselor/students          Join-code card + linked-students table              R9   ← home
  /counselor/students/[id]     Read-only student detail                            R9
  /counselor/settings          Profile · Password                                  R9.6
  sidebar: Students · Settings

/admin                         → /admin/metrics
  /admin/metrics               Pilot metrics                                       R10  ← home
  /admin/counselors            Counselor list, create, (de)activate                R10
  /admin/audit                 Counselor-view audit log                            R10
  /admin/settings              Profile · Password                                  R10.7
  sidebar: Metrics · Counselors · Audit log · Settings
```

## In-Scope

### R1: Sign in / Sign up page (`/signin`)

| ID | Requirement | Acceptance criterion |
|----|-------------|----------------------|
| R1.1 | One route with two tabs, Sign in and Sign up. `?mode=signup` opens the Sign up tab | `/signin?mode=signup` loads with Sign up selected. Switching tabs updates the URL without a full reload |
| R1.2 | Sign in with email + password for every role | Valid demo credentials (`POST /api/auth/signin`) land the user on their role's home (R2.1). A 401 shows the single generic message "Email or password is incorrect." with no hint about which one |
| R1.3 | Sign-in throttling is surfaced | A 429 shows "Too many attempts. Try again in 15 minutes." and the submit button stays enabled |
| R1.4 | Sign up with email, password, display name, birthdate, terms checkbox and an optional counselor code | A valid submit gets 201 from `POST /api/auth/signup`, then the app **automatically signs in** with the same credentials (signup doesn't start a session) and lands on `/student/feed` |
| R1.5 | Signup validation mirrors the backend and shows its per-field errors inline | The password field shows a "12+ characters" hint. An 11-char password is blocked client-side. Every `errors[]` field in a 400 problem+json response shows under its own input |
| R1.6 | Signup error cases have clear copy | Under 13 (400 on birthdate): "You must be 13 or older to use this app." Existing email (409): "An account with this email already exists." Unknown counselor code (404): error shown on the code field, the rest of the form keeps its values, and no account is created |
| R1.7 | Terms consent | The checkbox links to `/terms`, which shows the current `NEXT_PUBLIC_TERMS_VERSION`. Until real terms are written, the page holds placeholder text clearly marked "Draft — not legal text". Submit stays disabled until the box is ticked. The payload sends `acceptedTermsVersion = NEXT_PUBLIC_TERMS_VERSION` |
| R1.8 | A signed-in user visiting `/signin` is redirected | With a valid session, `/signin` redirects to the role's home |

### R2: Routing, guards & session handling

| ID | Requirement | Acceptance criterion |
|----|-------------|----------------------|
| R2.1 | Root and role homes | `/` redirects based on `GET /api/me`: 401 → `/signin`, STUDENT → `/student/feed`, COUNSELOR → `/counselor/students`, ADMIN → `/admin/metrics` |
| R2.2 | Role guards per prefix | Guards run client-side in each role layout from the `/api/me` query (no Next middleware). A student opening `/admin/counselors` or `/counselor/students` is redirected to `/student/feed`, and likewise for every role/prefix pair. Only a skeleton renders until the role is confirmed, so no protected page content flashes before the redirect |
| R2.3 | Expired session or 401 mid-use | Any 401 from the API (except on `/signin`) redirects to `/signin?next=<current path>` with the notice "Your session expired. Please sign in again." After sign-in the user returns to `next`, but only if it's a same-site path under their own role prefix |
| R2.4 | Forced password change | While `/api/me` has `mustChangePassword: true`, every route except `/change-password` and sign-out redirects to `/change-password`. A successful `PUT /api/me/password` sends the user to their role's home |
| R2.5 | Sign out | The user menu's "Sign out" calls `POST /api/auth/signout`, clears the query cache and goes to `/signin`. The browser Back button doesn't show cached protected data |
| R2.6 | Unknown routes | Any unmatched path renders a 404 page with a link to the user's home |
| R2.7 | Unexpected 403 | A 403 not covered by page copy (e.g. R9.5) refetches `/api/me` and re-runs the guards, so a changed role or `mustChangePassword` redirects. If nothing changed, the page shows "You don't have access to this." with a link home |

### R3: Student Career Feed (`/student/feed`)

| ID | Requirement | Acceptance criterion |
|----|-------------|----------------------|
| R3.1 | Shows exactly one career card from `GET /api/feed/next` | The card shows the title, summary, tags (as chips) and "~N min". No other careers are listed or browsable |
| R3.2 | Start / Continue | The feed card has `lastPage` but not `lastViewedPage`, so the feed also fetches `GET /api/careers/{id}/experience` in parallel (it has no side effects). The button reads "Start" while that loads or when there's no progress yet, and "Continue (page n of m)" otherwise. It opens `/student/careers/[id]` |
| R3.3 | The card is stable | Refreshing or signing out and back in shows the same card until it's reflected on or abandoned (backend [B-R4.1]) |
| R3.4 | Exhausted state | `{career: null, exhausted: true}` shows "You've explored every career." with a link to My Journey, not an error |
| R3.5 | Intake prompt modal | When `/api/me` returns `intakePromptDue: true` on the feed, a modal offers "Fill it in" (→ `/student/intake`) and "Not now" (`POST /api/intake/dismiss`). After either choice the modal doesn't come back on reload |

### R4: Career experience (`/student/careers/[id]`)

| ID | Requirement | Acceptance criterion |
|----|-------------|----------------------|
| R4.1 | Renders one page at a time from `GET /api/careers/{id}/experience`, with Previous/Next and a "Page n of m" indicator | Every block type renders: TEXT (paragraphs), VIDEO_EMBED and AUDIO_EMBED (sandboxed `iframe`), IMAGE_URL (`img` with alt text), EXTERNAL_LINK (opens in a new tab with `rel="noopener noreferrer"`) |
| R4.2 | Progress is saved per page | Showing page n calls `POST /api/careers/{id}/pages/{n}/viewed`. Reopening the career resumes at `lastViewedPage` |
| R4.3 | Reflection unlocks on the last page | The "Reflect on this career" button appears only on page `lastPage` and goes to `/student/careers/[id]/reflect` |
| R4.4 | Abandon | From page 1 on, an "This isn't for me" button opens a confirm dialog. Confirming calls `POST /api/careers/{id}/abandon` (204), then returns to `/student/feed`, where a new card appears |
| R4.5 | Review mode for reflected careers | The experience response doesn't say whether a career is reflected on, so the page checks `GET /api/students/me/reflections` (shared TanStack Query cache) for its `careerId`. A reflected career shows every page with free navigation and no Abandon button, plus an "Edit reflection" button on every page. This works from My Journey and from a bookmarked URL alike |
| R4.6 | Blocked careers | A 409 or 404 (the career isn't the in-progress card, isn't reflected on, or is retired) shows "This career isn't available right now." with a link to the feed |

### R5: Reflection form (`/student/careers/[id]/reflect`)

| ID | Requirement | Acceptance criterion |
|----|-------------|----------------------|
| R5.1 | One scrolling form built from `GET /api/reflection-questions` | Each active question shows its prompt, a 1–10 segmented control (min/max taken from the API) and its optional open prompt as a collapsible textarea (≤1000 chars, with a live counter). The final "Would I genuinely enjoy this?" verdict is YES / MAYBE / NO |
| R5.2 | Client and server validation | Submit is disabled until every scale and the verdict are set. Any 400 per-field errors show next to the matching question |
| R5.3 | Submit a new reflection | `POST /api/careers/{id}/reflection` succeeds, then `/api/me` is refetched and the student goes to `/student/feed` (where R3.5 can show the intake modal) |
| R5.4 | Edit an existing reflection | There's no single-reflection GET. When the career's entry exists in `GET /api/students/me/reflections`, the form is prefilled from its `answers` and `verdict` and saves with `PUT`. Success returns to My Journey |
| R5.5 | No double submit | The submit button is disabled while the request is pending. A double-click sends exactly one request |
| R5.6 | Stale or locked states | A 409 because the last page wasn't viewed sends the student back to the experience. A 409 from a concurrent `PUT` (the backend's optimistic lock; requests carry no version, so stale tabs can't be detected) shows "Someone saved this reflection at the same time. Reload and try again." and refetches the reflection |

### R6: My Journey (`/student/journey`): Saved Careers + Progress

| ID | Requirement | Acceptance criterion |
|----|-------------|----------------------|
| R6.1 | My Journey's data comes only from `GET /api/students/me/reflections` | The page fetches every page at `size=100` and computes everything client-side. My Journey calls no other endpoint (other pages, e.g. R4.5 and R5.4, may reuse the same query) |
| R6.2 | Progress stat strip | Shows the number of careers reflected on, YES / MAYBE / NO counts, and the average score per dimension. The numbers match the reflection list exactly |
| R6.3 | "Saved" list = YES + MAYBE by default | Filter tabs are Saved (YES+MAYBE, default), YES, MAYBE, NO and All. Each row shows the career title, verdict badge and last-updated date, newest first |
| R6.4 | Rows open the career | Clicking a row opens `/student/careers/[id]` in review mode (R4.5). Each row also has an "Edit reflection" action |
| R6.5 | Empty state | With 0 reflections: "Reflect on your first career to see your journey here." with a link to the feed |

### R7: Intake (`/student/intake`)

| ID | Requirement | Acceptance criterion |
|----|-------------|----------------------|
| R7.1 | Form built from `GET /api/intake/questions` | TAGS questions render as multi-select chips grouped by tag category. TEXT questions render as textareas. Adding a question in the DB shows up without frontend changes |
| R7.2 | New vs edit | When `GET /api/intake` returns 200, the form is prefilled and saves with `PUT`. On 404 it's empty and saves with `POST`. Success shows a toast and goes to the feed (new) or Settings (edit) |
| R7.3 | Not yet available | With fewer than 3 reflections (409 on submit), the page shows "The intake opens after your 3rd reflection." instead of the form |

### R8: Student Settings (`/student/settings`)

| ID | Requirement | Acceptance criterion |
|----|-------------|----------------------|
| R8.1 | One page with anchored cards: Profile, Password, My counselors, Interests | Each card can be reached by its `#anchor` |
| R8.2 | Change the display name | `PATCH /api/me` updates the name, and the user menu shows the new name without a reload. Email is shown read-only |
| R8.3 | Change the password | The form has current, new (≥12) and confirm fields. Mismatched new/confirm is blocked client-side. A wrong current password (400) shows on that field. On success: a toast, and the user stays signed in |
| R8.4 | List counselor links | Shows each link's counselor name, linked date, and an "Inactive" badge when `counselorActive: false` |
| R8.5 | Link a counselor by code | `POST /api/students/me/counselors`: 201 adds it to the list. 404 shows "Code not found." 409 shows "You're already linked to this counselor." 429 shows "Too many attempts. Try again in 15 minutes." |
| R8.6 | Revoke a link | A confirm dialog ("<name> will no longer see your reflections") calls `DELETE …/{id}`, and the row disappears |
| R8.7 | Interests card | Shows "Completed" or "Not filled in" (from `GET /api/intake` returning 200 or 404) and a button to `/student/intake` |

### R9: Counselor portal

| ID | Requirement | Acceptance criterion |
|----|-------------|----------------------|
| R9.1 | Join-code card on `/counselor/students` | Shows the code from `GET /api/counselor/me` and a Copy button that puts it on the clipboard and confirms with a toast |
| R9.2 | Rotate the join code | "Rotate" opens a confirm dialog ("The old code stops working. Linked students stay linked."). `POST /api/counselor/me/join-code/rotate` swaps in the new code |
| R9.3 | Linked-students table | From `GET /api/counselor/students`, paginated (20 per page, page controls). Columns: name, seen, reflected, abandoned, YES/MAYBE/NO, intake ✓, last active (relative time). Sorted by last active as the API returns it |
| R9.4 | Empty state | With 0 students: "No students yet. Share your join code above." |
| R9.5 | Student detail `/counselor/students/[id]` | Read-only sections: top 5 interests (tag + category), reflections (career, verdict, score per dimension, open answers, date), abandoned careers (career, last page reached, count) and intake answers. The page has no inputs that change student data. A 403 shows "This student hasn't shared their progress with you." |
| R9.6 | Counselor Settings | `/counselor/settings` reuses the Profile and Password cards from R8.2–R8.3 |

### R10: Admin portal

| ID | Requirement | Acceptance criterion |
|----|-------------|----------------------|
| R10.1 | Pilot metrics `/admin/metrics` | Tiles for total students, % with ≥3 reflections and % intake completed, plus a YES/MAYBE/NO bar chart, all from `GET /api/admin/pilot-metrics`. A note says demo accounts are excluded |
| R10.2 | Signup date filter | From/To date inputs send `from`/`to`. Clearing them returns to all-time. The filter is kept in the URL query |
| R10.3 | Counselor list `/admin/counselors` | Paginated table: name, email, join code, Active/Inactive badge, linked-student count |
| R10.4 | Create a counselor | A dialog with email, display name and temporary password (≥12) sends `POST /api/admin/counselors`. On 201 the new row appears and a notice says "Share this temporary password with the counselor. They must change it on first sign-in." 400/409 errors show per field |
| R10.5 | Deactivate / reactivate | A row action with a confirm dialog sends `PATCH /api/admin/counselors/{id} {active}`. The badge updates. The deactivate confirm warns that the counselor is signed out immediately |
| R10.6 | Audit log `/admin/audit` | Paginated table, newest first: counselor **name** (resolved from the admin counselor list, falling back to `#id`), student `#id`, an Allowed/Denied badge and the timestamp in local time. A student-id filter input sends `?studentId=` |
| R10.7 | Admin Settings | `/admin/settings` reuses the Profile and Password cards |

### R11: Shared shell & API client

| ID | Requirement | Acceptance criterion |
|----|-------------|----------------------|
| R11.1 | Portal layout | Each role prefix has a left sidebar with its nav items (see Page map), the active item highlighted, and a user menu (display name, Settings, Sign out) |
| R11.2 | Same-origin API | In dev, `next.config.ts` rewrites `/api/*` to `BACKEND_URL` (default `http://localhost:8080`). In compose/prod, nginx sends `/api/*` straight to Spring and everything else to Next, so the rewrite isn't used (Next fixes rewrite destinations at build time, and an extra hop would hide the client IP from the login limiter). Either way the browser only ever sees one origin |
| R11.3 | One typed fetch client | Every request goes through `src/lib/api`, which sends `credentials: 'include'`, copies the `XSRF-TOKEN` cookie into the `X-XSRF-TOKEN` header on POST/PUT/PATCH/DELETE, and parses problem+json into `{status, detail, fieldErrors}`. Before the first mutation it makes sure `GET /api/me` has run, so the `XSRF-TOKEN` cookie exists, and a CSRF 403 refetches the token and retries once |
| R11.4 | Generated API types | `pnpm gen:api` runs openapi-typescript against the **dev** backend's `/v3/api-docs` (OpenAPI is off in prod) and writes `src/lib/api/schema.d.ts`, which is committed. `pnpm typecheck` fails if a call doesn't match the schema |
| R11.5 | Server state through TanStack Query | Lists use one reusable paginated-table component (page/size and page metadata). Mutations invalidate the queries they affect, so no manual reload is needed after any action |
| R11.6 | Loading and error states | Every data page shows a skeleton while loading. An unexpected error (5xx or network) shows a retry panel with the `X-Request-Id` value from the response for support |

### R12: Deployment & local prod rehearsal

| ID | Requirement | Acceptance criterion |
|----|-------------|----------------------|
| R12.1 | Production build | `next.config.ts` sets `output: 'standalone'`. `pnpm build` produces a server that runs with `node server.js` on Node 24 LTS. A `node:24-alpine` image runs it, and needs no runtime env vars |
| R12.2 | Deploy folder at the repo root | A root `deploy/` folder holds the compose file, nginx config, `.env.example` and a README. Nothing in `../mvp` changes |
| R12.3 | Backend image without a Dockerfile | The backend image is built with `./mvnw spring-boot:build-image` (buildpacks). Compose runs it with `SPRING_PROFILES_ACTIVE=prod` against a Postgres service, with `ADMIN_EMAIL`/`ADMIN_PASSWORD` from `.env`. Careers come from Flyway, so the rehearsal has content |
| R12.4 | nginx front door | nginx serves `https://localhost` with a mkcert certificate (git-ignored, setup step in the README). It sends `/api/*` to Spring and `/*` to Next, and sets `X-Forwarded-For` and `X-Forwarded-Proto` as the backend README requires |
| R12.5 | Smoke test against the rehearsal | `pnpm e2e:smoke` against `https://localhost`: the bootstrap admin signs in and sees metrics, creates a counselor, the counselor's first sign-in forces a password change, a student signs up with the counselor's code and sees a feed card. It runs on Chromium, Firefox and WebKit |

## Stretch

- **S1: Reflection version history**: a "See earlier versions" drawer on reviewed careers using `GET /api/careers/{id}/reflection/versions`. _Why stretch:_ the API exists, but the student loop works without it.
- **S2: Celebratory "seen it all" screen**: a summary of verdicts plus some celebration instead of the plain R3.4 empty state. _Why stretch:_ polish.
- **S3: Dark mode**: follow the system theme using shadcn's CSS variables. _Why stretch:_ not chosen as out, but not needed for the pilot.
- **S4: "Forgot password?" help**: a link on sign-in explaining that students should ask their counselor and counselors should ask the admin. _Why stretch:_ there's no reset API (backend X5, S2), so this can only be a help message.

## Out-of-Scope

- **X1: Backend changes**: the `../mvp` API is frozen. Gaps (saved careers, student stats) are derived on the client or cut.
- **X2: Real "save/bookmark" for careers**: no endpoint. Saved = YES/MAYBE reflections (R6.3). Browser-only bookmarks were rejected because they don't follow the student across devices.
- **X3: Student view of top interests and abandoned careers**: only the counselor API exposes them. Needs a backend change (backend S4).
- **X4: Third-party analytics / tracking**: conflicts with the backend's minors' data minimization (B-N2.8).
- **X5: PWA / offline support**: the pilot runs online on school devices.
- **X6: Marketing landing page**: `/` only redirects (R2.1). Students arrive through a counselor or a direct link.
- **X7: Mobile-first design**: desktop-first everywhere. Mobile only has to stay usable (N4.2).
- **X8: Reflection/intake draft autosave**: not chosen as stretch. Forms are short.
- **X9: Counselor student search/filter**: not chosen as stretch. Pilot counselors have ≤300 students and the list is sorted by activity.
- **X10: Email change, account deletion, data export, admin password reset**: no API (backend X18, S2, S3).
- **X11: Content management UI**: content arrives through Flyway migrations (backend X4).
- **X12: Internationalization**: the pilot is English only (backend X11).
- **X13: Phase 2/3 features (assignments, guides)**: later backend phases (backend X1–X3, S1).

## Non-functional

### N1: Performance

| ID | Requirement | Acceptance criterion |
|----|-------------|----------------------|
| N1.1 | Fast first load | Lighthouse desktop performance ≥90 on `/signin` and `/student/feed` (production build, `next start`) |
| N1.2 | Fast in-app navigation | Client-side navigation between portal pages renders the new page (skeleton or content) in <500 ms against the local dev backend |

### N2: Security & privacy

| ID | Requirement | Acceptance criterion |
|----|-------------|----------------------|
| N2.1 | No credentials or tokens in JS-readable storage | Nothing auth-related goes in `localStorage`/`sessionStorage`. The session stays in the backend's HttpOnly cookie. Only the CSRF cookie is read by JS |
| N2.2 | CSRF header on every state-changing call | A Vitest test asserts that the client adds `X-XSRF-TOKEN` to POST/PUT/PATCH/DELETE. Playwright flows succeed with CSRF enabled on the backend |
| N2.3 | Guards are UX, not security | The backend enforces every permission. The frontend never hides data it received; it just doesn't request what the role can't see |
| N2.4 | Safe embeds and links | Embed iframes use `sandbox` with only the permissions players need (`allow-scripts allow-same-origin allow-presentation`). External links use `rel="noopener noreferrer"`. A CSP set in `next.config.ts` `headers()` (same in dev and prod) contains `frame-src` limited to the 8 hosts in the backend's `career.embed-hosts`, `object-src 'none'`, `base-uri 'self'`, `form-action 'self'` and `frame-ancestors 'none'`. It has no `script-src`, because per-request nonces would make every page dynamic; N2.6 covers scripts |
| N2.5 | Open-redirect safe `next` | `/signin?next=https://evil.com` and `next=//evil.com` both land on the role's home |
| N2.6 | No third-party scripts | The production build loads no scripts or fonts from third-party origins (fonts are self-hosted with `next/font`) |

### N3: Accessibility

| ID | Requirement | Acceptance criterion |
|----|-------------|----------------------|
| N3.1 | WCAG 2.1 AA basics | `@axe-core/playwright` reports zero serious or critical violations on every page in the Page map |
| N3.2 | Keyboard operable | Each role's e2e happy path (N6.2) also passes with keyboard only (Tab/Enter/Space/arrow keys), including the 1–10 scale and dialogs. Focus is visible and trapped inside open dialogs |
| N3.3 | Labelled inputs and errors | Every input has a label. Field errors are tied to their input via `aria-describedby` and announced |

### N4: Compatibility

| ID | Requirement | Acceptance criterion |
|----|-------------|----------------------|
| N4.1 | Browsers | Last 2 versions of Chrome, Edge, Firefox and Safari. Playwright runs on Chromium, Firefox and WebKit (WebKit needs HTTPS for the prod `Secure` cookie, see R12.4) |
| N4.2 | Screen sizes | Designed for ≥1280 px. At 1024 px and 768 px wide, no horizontal page scroll and every action still works (the sidebar collapses to an icon rail or a menu) |

### N5: Usability

| ID | Requirement | Acceptance criterion |
|----|-------------|----------------------|
| N5.1 | A student finishes the core loop unaided | From a fresh signup, the student reaches a submitted reflection with at most these clicks on top of reading pages: Start → Next × pages → Reflect → Submit |
| N5.2 | Consistent error copy | Every backend status a page can receive (400/401/403/404/409/429/5xx) has written copy (R1–R10). A raw JSON or stack text never shows |

### N6: Maintainability

| ID | Requirement | Acceptance criterion |
|----|-------------|----------------------|
| N6.1 | Unit/component tests | Vitest + Testing Library cover the API client (CSRF, problem+json parsing, 401 handling), the R6 stat computation and the reflection form validation. `pnpm test` is green |
| N6.2 | E2E happy path per role | Playwright, against `./mvnw spring-boot:run` (dev) with the demo accounts (password `demo`). Every run signs up with a unique `e2e+<timestamp>@…` email, and `pnpm e2e:reset` deletes `../mvp/data` for a clean start. Includes one flow from a clean browser with no cookies (CSRF bootstrap, R11.3): a student signs up, reflects ×3, handles the intake modal, sees My Journey. A counselor views the student's detail. An admin creates a counselor, and that counselor's first sign-in forces a password change |
| N6.3 | CI | A frontend job in the root `.github/workflows/ci.yml` (Node 24, pnpm through corepack) runs `pnpm lint`, `pnpm typecheck` and `pnpm test` on every push and PR. E2E and smoke run locally (they need the backend) |
| N6.4 | Config by env | Public values are fixed at build time: `NEXT_PUBLIC_TERMS_VERSION` (default `2026-11-18`). `BACKEND_URL` (default `http://localhost:8080`) is only used by the dev rewrite. The production server needs no runtime env vars, and nothing is hard-coded per environment |

_Skipped areas:_ reliability/availability (the frontend is a stateless, client-rendered Node server, so uptime belongs to hosting and the backend), scalability (300-student pilot, and the backend is the bottleneck), cost (no paid services; the real host is picked later, see Open Questions), data retention (the frontend stores no data, only the backend does).

## Open Questions

- **Real terms text (R1.7):** a placeholder ships with `NEXT_PUBLIC_TERMS_VERSION=2026-11-18`. Real terms must be written, and the version bumped, before any school uses the app.
- **Production host:** the frontend is deploy-ready (R12), and the compose rehearsal is the reference setup. The real host (same EC2 behind nginx, or another Node host) gets picked with the backend's EC2 phase. Either way `/api` must stay same-origin and served over HTTPS.
