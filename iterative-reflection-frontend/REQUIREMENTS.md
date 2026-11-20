# Iterative Reflection Frontend: Requirements

_Last updated: 2026-11-20 (added R13: admin-selectable theme presets, update 2)_

> Codebase status: R1–R12 are built on `spec/frontend` (tickets 01–25) and update 1 (visual identity) is merged. The compose rehearsal (R12.3–R12.5) and CI (N6.3) are written but unverified; R13.10 verifies them in the update 2 PR. Decisions from the gap review are logged in `DECISIONS.md`. R1–R12 work with the backend's 31 original endpoints as they are. R13 depends on the backend addition B-R11 (settings + admin activity); the backend is otherwise still frozen. IDs like [B-R4.7] point to `../mvp/REQUIREMENTS.md`.
>
> **Delivery order:** update 1 (look + quality of life) is folded into R3.6, R4.7, R6.6, N3.4 and N6.5. R13 is update 2 (theme presets, with B-R11), on its own branch and PR. Update 3 (engagement) gets its own grilling session; its draft lives in `.scratch/iterative-reflection-frontend/notes/`.

## Summary

**Problem:** The Spring Boot API for the career-discovery loop is done, but there's nothing a student, counselor or admin can actually use. Students need a web app to work through one career at a time and reflect on it. Counselors need a read-only view of the students linked to them. Admins need a way to onboard counselors and track pilot success.
**Users:** High-school students (13+), mostly on school Chromebooks or laptops. Counselors and one admin, on desktop.
**Success looks like:** A student can go from sign-up through 3 reflections, the intake prompt, and seeing YES/MAYBE careers in My Journey, with no help and no dead ends. A counselor can see that student's detail. An admin can create that counselor and read the pilot metrics. All three are covered by Playwright e2e tests against the dev backend.
**Constraints:** Solo developer, no deadline. Runs locally, and is deploy-ready: a Node `standalone` build plus a local docker-compose rehearsal of prod (R12). Next.js 16 App Router + React 19 + TypeScript, Tailwind + shadcn/ui, TanStack Query. Backend changes only where B-R11 lists them; any other gap gets filled on the client or cut. Desktop-first for every portal.
**Visual identity:** curious, supportive, grounded. Minimal: color is an accent, never the main event, and no palette may resemble a specific company's brand. Update 2 success: the admin switches to `harbor`, and a student's next page load (sign-in included) is harbor with no Grove flash, every preset passes AA, and the brand check is recorded.

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
  /admin/audit                 Counselor views · Admin activity (tabs)             R10.6, R13.9
  /admin/settings              Profile · Password · App theme                      R10.7, R13.5
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
| R1.6 | Signup error cases have clear copy | Under 13 (a 400 with only `detail`, no `errors[]`; it is also checked client-side): "You must be 13 or older to use this app." under birthdate. Existing email (409): "An account with this email already exists." Unknown counselor code (404): error shown on the code field, the rest of the form keeps its values, and no account is created |
| R1.7 | Terms consent | The checkbox links to `/terms`, which shows the current `NEXT_PUBLIC_TERMS_VERSION`. Until real terms are written, the page holds placeholder text clearly marked "Draft — not legal text". Submit stays disabled until the box is ticked. The payload sends `acceptedTermsVersion = NEXT_PUBLIC_TERMS_VERSION` |
| R1.8 | A signed-in user visiting `/signin` is redirected | With a valid session, `/signin` redirects to the role's home |

### R2: Routing, guards & session handling

| ID | Requirement | Acceptance criterion |
|----|-------------|----------------------|
| R2.1 | Root and role homes | `/` redirects based on `GET /api/me`: 401 → `/signin`, STUDENT → `/student/feed`, COUNSELOR → `/counselor/students`, ADMIN → `/admin/metrics` |
| R2.2 | Role guards per prefix | Guards run client-side in each role layout from the `/api/me` query (no Next middleware). A student opening `/admin/counselors` or `/counselor/students` is redirected to `/student/feed`, and likewise for every role/prefix pair. Only a skeleton renders until the role is confirmed, so no protected page content flashes before the redirect |
| R2.3 | Expired session or 401 mid-use | Any 401 from the API (except on `/signin`) redirects to `/signin?next=<current path>` with the notice "Your session expired. Please sign in again." After sign-in the user returns to `next`, but only if it's a same-site path under their own role prefix. `mustChangePassword` (R2.4) takes priority over `next` |
| R2.4 | Forced password change | While `/api/me` has `mustChangePassword: true`, every route except `/change-password` and sign-out redirects to `/change-password`. A successful `PUT /api/me/password` sends the user to their role's home |
| R2.5 | Sign out | The user menu's "Sign out" calls `POST /api/auth/signout`, clears the query cache and goes to `/signin`. The browser Back button doesn't show cached protected data |
| R2.6 | Unknown routes | Any unmatched path renders a 404 page with a link to the user's home |
| R2.7 | Unexpected 403 | A 403 not covered by page copy (e.g. R9.5) refetches `/api/me` and re-runs the guards, so a changed role or `mustChangePassword` redirects. If nothing changed, the page shows "You don't have access to this." with a link home |

### R3: Student Career Feed (`/student/feed`)

| ID | Requirement | Acceptance criterion |
|----|-------------|----------------------|
| R3.1 | Shows exactly one career card from `GET /api/feed/next` | The card shows the title, summary, tags (as chips) and "~N min". No other careers are listed or browsable |
| R3.2 | Start / Continue | The feed card carries both `lastPage` and `lastViewedPage`, so the label comes from the card alone, with no extra fetch. The button reads "Start" when there's no progress yet, and "Continue (page n of m)" otherwise. Viewing a page refreshes the feed, so the label is never stale. It opens `/student/careers/[id]` |
| R3.3 | The card is stable | Refreshing or signing out and back in shows the same card until it's reflected on or abandoned (backend [B-R4.1]) |
| R3.4 | Exhausted state | `{career: null, exhausted: true}` shows "You've explored every career." with a link to My Journey, not an error |
| R3.5 | Intake prompt modal | When `/api/me` returns `intakePromptDue: true` on the feed, a modal offers "Fill it in" (→ `/student/intake`) and "Not now" (`POST /api/intake/dismiss`). After either choice the modal doesn't come back on reload |

| R3.6 | Progress | While the student has fewer than 3 reflections, the card shows a progress bar and "n of 3 reflections" toward unlocking the intake, and the line goes away after the 3rd. Once n > 0 the feed shows "n career(s) explored" (n = reflections; hidden at 0). There is no "of N" total yet: no endpoint exposes the career count |

### R4: Career experience (`/student/careers/[id]`)

| ID | Requirement | Acceptance criterion |
|----|-------------|----------------------|
| R4.1 | Renders one page at a time from `GET /api/careers/{id}/experience`, with Previous/Next and a "Page n of m" indicator | Every block type renders: TEXT (paragraphs), VIDEO_EMBED and AUDIO_EMBED (sandboxed `iframe`), IMAGE_URL (`img` with alt text), EXTERNAL_LINK (opens in a new tab with `rel="noopener noreferrer"`) |
| R4.2 | Progress is saved per page | Showing page n calls `POST /api/careers/{id}/pages/{n}/viewed`. Reopening the career resumes at `lastViewedPage` |
| R4.3 | Reflection unlocks on the last page | The "Reflect on this career" button appears only on page `lastPage` and goes to `/student/careers/[id]/reflect` |
| R4.4 | Abandon | From page 1 on, an "This isn't for me" button opens a confirm dialog. Confirming calls `POST /api/careers/{id}/abandon` (204), then returns to `/student/feed`, where a new card appears |
| R4.5 | Review mode for reflected careers | The experience response doesn't say whether a career is reflected on, so the page checks `GET /api/students/me/reflections` (shared TanStack Query cache) for its `careerId`. A reflected career shows every page with free navigation and no Abandon button, plus an "Edit reflection" button on every page. This works from My Journey and from a bookmarked URL alike |
| R4.6 | Blocked careers | A 409 or 404 (the career isn't the in-progress card, isn't reflected on, or is retired) shows "This career isn't available right now." with a link to the feed |

| R4.7 | Experience progress bar | A bar under the page header fills in proportion to "Page n of m" (n/m), on Next, Previous and in review mode. It has `role="progressbar"` with the current page and total, and the text indicator stays |

### R5: Reflection form (`/student/careers/[id]/reflect`)

| ID | Requirement | Acceptance criterion |
|----|-------------|----------------------|
| R5.1 | One scrolling form built from `GET /api/reflection-questions` | Each active question shows its prompt, a 1–10 segmented control (min/max taken from the API) and its optional open prompt as a collapsible textarea (≤1000 chars, with a live counter). The final "Would I genuinely enjoy this?" verdict is YES / MAYBE / NO |
| R5.2 | Client and server validation | Submit is disabled until every scale and the verdict are set. Any 400 per-field errors show next to the matching question |
| R5.3 | Submit a new reflection | `POST /api/careers/{id}/reflection` succeeds, then `/api/me` is refetched and the student goes to `/student/feed` (where R3.5 can show the intake modal) |
| R5.4 | Edit an existing reflection | There's no single-reflection GET. When the career's entry exists in `GET /api/students/me/reflections`, the form is prefilled from its `answers` and `verdict` and saves with `PUT`. Success returns to My Journey |
| R5.5 | No double submit | The submit button is disabled while the request is pending, and a client-side lock blocks a second send. A double-click sends exactly one request. (The backend can't catch duplicates: a repeated POST returns its in-progress 409, not "already reflected") |
| R5.6 | Stale or locked states | A 409 because the last page wasn't viewed sends the student back to the experience. Any other 409 on submit shows "not open for reflection" with a link to the feed. A 409 from a concurrent `PUT` (the backend's optimistic lock; requests carry no version, so stale tabs can't be detected) shows "Someone saved this reflection at the same time. Reload and try again." and refetches the reflection |

| R5.7 | Filled scale and verdict control | The 1–10 scale fills segments 1 to n in `primary` and leaves the rest empty (native radios, so keyboard and form state are unchanged). The verdict control picks its selected color from the shared verdict mapping (R6.6) |

### R6: My Journey (`/student/journey`): Saved Careers + Progress

| ID | Requirement | Acceptance criterion |
|----|-------------|----------------------|
| R6.1 | My Journey's data comes only from `GET /api/students/me/reflections` | The page fetches every page at `size=100` and computes everything client-side. My Journey calls no other endpoint (other pages, e.g. R4.5 and R5.4, may reuse the same query) |
| R6.2 | Progress stat strip | Shows the number of careers reflected on, YES / MAYBE / NO counts, and the average score per dimension. The numbers match the reflection list exactly |
| R6.3 | "Saved" list = YES + MAYBE by default | Filter tabs are Saved (YES+MAYBE, default), YES, MAYBE, NO and All. Each row shows the career title, verdict badge and last-updated date, newest first |
| R6.4 | Rows open the career | Clicking a row opens `/student/careers/[id]` in review mode (R4.5). Each row also has an "Edit reflection" action |
| R6.5 | Empty state | With 0 reflections: "Reflect on your first career to see your journey here." with a link to the feed |

| R6.6 | Verdict colors and bar | One shared mapping styles every verdict chip, bar and badge: YES `positive`, MAYBE a `highlight` tint chip (painted over an opaque `surface`), NO neutral `muted` (red is reserved for destructive actions and Denied). The stat strip adds a horizontal bar with segment widths proportional to the YES / MAYBE / NO counts, with an accessible label listing them. The counts stay as text. One verdict kind renders a single full-width segment, and 0 reflections shows no bar |

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
| R11.2 | Same-origin API | In dev, `next.config.ts` rewrites `/api/*` to `BACKEND_URL` (default `http://localhost:8080`). In compose/prod, nginx sends `/api/*` straight to Spring and everything else to Next, so the rewrite isn't used (Next fixes rewrite destinations at build time, and an extra hop would hide the client IP from the login limiter). Either way the browser only ever sees one origin. The backend's CORS check only accepts `Origin: http://localhost:5173` in dev (`app.frontend-origin`), so `pnpm dev` runs on port 5173. In prod, nginx forwards `Host`, `X-Forwarded-Proto` and `X-Forwarded-For`, and the backend gets `APP_FRONTEND_ORIGIN=https://localhost` |
| R11.3 | One typed fetch client | Every request goes through `src/lib/api`, which sends `credentials: 'include'`, copies the `XSRF-TOKEN` cookie into the `X-XSRF-TOKEN` header on POST/PUT/PATCH/DELETE, and parses problem+json into `{status, detail, fieldErrors, requestId}`. Sign-in, sign-out and password change clear the `XSRF-TOKEN` cookie instead of rotating it, so before any mutation where the cookie is missing, the client runs `GET /api/me` to get it back. A CSRF 403 looks the same as a role denial, so after a 403 the client refetches the token and retries once, but only if the token changed |
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

### R13: Admin-selectable theme presets (update 2) `🟡 partial`

| ID | Requirement | Acceptance criterion | Status |
|----|-------------|----------------------|--------|
| R13.1 | Four presets fill the same slots | `grove` (default: `:root`, also `[data-theme="grove"]`), `harbor` (cool, steady), `plum` (soft, creative) and `slate` (neutral) each define all 15 slots from N6.5 in `globals.css`. The token check's `PRESETS` list has all four and fails if any preset is missing a slot. No component changes per preset | `🟡` token check already loops `PRESETS`, but only `grove` exists |
| R13.2 | The theme applies to every page | A theme query on `GET /api/settings/theme` [B-R11.1], mounted in the root providers, sets `data-theme` on `<html>` on every page, including `/signin`, `/terms` and `/change-password`. It is fetched once per full page load (no focus refetch, no polling). Students and counselors have no theme control | `⬜` |
| R13.3 | No flash of the wrong theme | An inline `<head>` script applies the last theme id from `localStorage` before first paint, and the server value then overwrites both the attribute and the stored hint. Playwright check: with `harbor` saved and the theme request delayed, a reload has `data-theme="harbor"` at DOMContentLoaded | `⬜` |
| R13.4 | Fallbacks | If the theme request fails, returns an unknown id, or `localStorage` throws, the app uses `grove` and shows no error, toast or retry panel | `⬜` |
| R13.5 | App theme card on `/admin/settings` | A card after Password: a native radio group with one option per preset showing its name, swatches and a mini mock card (a feed card with a `primary` Start button, YES/MAYBE/NO chips and a progress bar) drawn in that preset via a scoped `data-theme`. The saved preset is preselected from `GET /api/admin/settings/theme` [B-R11.3]. Selecting a radio changes only the mock cards, not the page | `⬜` |
| R13.6 | Saving the theme | Save is disabled while the selection equals the saved preset and while a save is pending. Save sends `PUT /api/admin/settings/theme` [B-R11.2]. On 200: a success toast, `<html data-theme>` and the stored hint update immediately with no reload, and R13.7's line updates. On an error: an error toast (N5.2 copy), the radio reverts to the saved preset, and the page theme doesn't change | `⬜` |
| R13.7 | "Last changed" line | The card shows "Last changed by <display name> on <date>" (local time) from B-R11.3. With no saved theme (`updatedAt` null), the line is hidden | `⬜` |
| R13.8 | Change propagation | Other users get the new theme on their next full page load. An already-open tab keeps its theme until reloaded. There is no live push | `⬜` |
| R13.9 | Admin activity tab on `/admin/audit` | `/admin/audit` gets two tabs: "Counselor views" (R10.6, unchanged) and "Admin activity", kept in the URL as `?tab=activity`. The activity tab is a paginated table (20 per page, newest first) from `GET /api/admin/activity` [B-R11.8] with When (local time), Admin, Action and Details (e.g. "Theme: Grove → Harbor", "Created counselor jane@school.org", "Deactivated counselor …"). With no rows it shows "No admin activity yet." | `⬜` |
| R13.10 | Carried-over verification | Ships in the same PR: tickets 01, 05 and 22 are verified. A GitHub Actions run of the root CI is green (N6.3). Following the deploy README, `https://localhost` serves the app on Postgres (R12.3, R12.4). `pnpm e2e:smoke` passes on Chromium, Firefox and WebKit (R12.5). The ticket checkboxes are ticked, and anything that had to change is noted in `DECISIONS.md` | `🟡` written, never run |

## Stretch

- **S1: Reflection version history**: a "See earlier versions" drawer on reviewed careers using `GET /api/careers/{id}/reflection/versions`. _Why stretch:_ the API exists, but the student loop works without it.
- **S2: Celebratory "seen it all" screen**: a summary of verdicts plus some celebration instead of the plain R3.4 empty state. _Why stretch:_ polish.
- **S3: Dark mode**: follow the system theme, as a `.dark` variant of each R13 preset's slots (no component changes). _Why stretch:_ not chosen as out, but not needed for the pilot.
- **S4: "Forgot password?" help**: a link on sign-in explaining that students should ask their counselor and counselors should ask the admin. _Why stretch:_ there's no reset API (backend X5, S2), so this can only be a help message.
- **S5: Backfilled admin activity**: show "Created counselor" rows for counselors created before update 2, from the backend backfill [B-S6]. _Why stretch:_ the tab starting empty is fine for a pilot.

## Out-of-Scope

- **X1: Backend changes beyond B-R11**: the `../mvp` API is otherwise frozen. Other gaps (saved careers, student stats) are derived on the client or cut.
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
- **X14: Per-user theme choice**: one admin-chosen theme keeps a shared identity (R13.2).
- **X15: Free-form theme colors**: admins pick from contrast-checked presets only (R13.1), so unreadable or brand-copying palettes can't be entered.
- **X16: Live theme push (websockets/SSE) or theme refetching**: the theme changes rarely; next full load is enough (R13.8).
- **X17: Per-school or per-counselor themes**: one pilot, one admin, one theme.
- **X18: More than four presets**: not now. Adding one later is data plus a token-check entry, not code.
- **X19: Admin activity export, retention, or other admin actions**: the log holds theme changes and counselor create/(de)activate only. Retention is decided with the FERPA review (backend X17); sign-ins and profile/password changes aren't logged.
- **X20: New fonts**: Geist only; weight and size carry the hierarchy.

## Non-functional

### N1: Performance

| ID | Requirement | Acceptance criterion |
|----|-------------|----------------------|
| N1.1 | Fast first load | Lighthouse desktop performance ≥90 on `/signin` and `/student/feed` (production `standalone` build) |
| N1.2 | Fast in-app navigation | Client-side navigation between portal pages renders the new page (skeleton or content) in <500 ms against the local dev backend |

### N2: Security & privacy

| ID | Requirement | Acceptance criterion |
|----|-------------|----------------------|
| N2.1 | No credentials or tokens in JS-readable storage | Nothing auth-related goes in `localStorage`/`sessionStorage`. The session stays in the backend's HttpOnly cookie. Only the CSRF cookie is read by JS |
| N2.2 | CSRF header on every state-changing call | A Vitest test asserts that the client adds `X-XSRF-TOKEN` to POST/PUT/PATCH/DELETE. Playwright flows succeed with CSRF enabled on the backend |
| N2.3 | Guards are UX, not security | The backend enforces every permission. The frontend never hides data it received; it just doesn't request what the role can't see |
| N2.4 | Safe embeds and links | Embed iframes use `sandbox` with only the permissions players need (`allow-scripts allow-same-origin allow-presentation`). External links use `rel="noopener noreferrer"`. A CSP set in `next.config.ts` `headers()` (same in dev and prod) contains `frame-src` limited to the 8 hosts in the backend's `career.embed-hosts`, `object-src 'none'`, `base-uri 'self'`, `form-action 'self'` and `frame-ancestors 'none'`. It has no `script-src`, because per-request nonces would make every page dynamic; N2.6 covers scripts |
| N2.5 | Open-redirect safe `next` | `/signin?next=https://evil.com` and `next=//evil.com` both land on the role's home |
| N2.6 | No third-party scripts | The production build loads no scripts or fonts from third-party origins (fonts are self-hosted from the bundled `geist` package; the metrics chart is plain CSS, with no chart library) |

### N3: Accessibility

| ID | Requirement | Acceptance criterion |
|----|-------------|----------------------|
| N3.1 | WCAG 2.1 AA basics | `@axe-core/playwright` reports zero serious or critical violations on every page in the Page map |
| N3.2 | Keyboard operable | Each role's e2e happy path (N6.2) also passes with keyboard only (Tab/Enter/Space/arrow keys), including the 1–10 scale and dialogs. Focus is visible and trapped inside open dialogs |
| N3.3 | Labelled inputs and errors | Every input has a label. Field errors are tied to their input via `aria-describedby` and announced |
| N3.4 | Color contrast, every preset | For each of the four presets, the slot pairs that carry text (foreground, primary-foreground, highlight-text, positive-foreground, destructive-foreground, muted-foreground on their grounds, and the MAYBE chip tint) meet 4.5:1, enforced by the token check (N6.5). `highlight` is fill-only and never text. The axe scan (N3.1) also runs `/signin`, `/student/feed` and `/admin/settings` under each preset |
| N3.5 | Theme card is accessible | The R13.5 radios are keyboard operable (arrow keys move, Space selects) and each one's accessible name is the preset name. Swatches and mock cards are `aria-hidden`. Save success/failure toasts are announced, and focus stays on Save |

### N4: Compatibility

| ID | Requirement | Acceptance criterion |
|----|-------------|----------------------|
| N4.1 | Browsers | Last 2 versions of Chrome, Edge, Firefox and Safari. Playwright runs on Chromium, Firefox and WebKit (WebKit needs HTTPS for the prod `Secure` cookie, see R12.4) |
| N4.2 | Screen sizes | Designed for ≥1280 px. At 1024 px and 768 px wide, no horizontal page scroll and every action still works (below 1280 px the sidebar collapses into a top bar with a "Menu" button) |

### N5: Usability

| ID | Requirement | Acceptance criterion |
|----|-------------|----------------------|
| N5.1 | A student finishes the core loop unaided | From a fresh signup, the student reaches a submitted reflection with at most these clicks on top of reading pages: Start → Next × pages → Reflect → Submit |
| N5.2 | Consistent error copy | Every backend status a page can receive (400/401/403/404/409/429/5xx) has written copy (R1–R10). A raw JSON or stack text never shows, and neither does the backend's free-text `detail`. The one exception is per-field `errors[]` messages, which show under their inputs as R1.5 requires |
| N5.3 | No brand look-alikes | Before R13 merges, `DECISIONS.md` records, for each preset, its `primary` + `highlight` pair, the nearest well-known brands that use a similar pairing, and why it isn't a look-alike. Drafted by Claude, signed off by the developer |

### N6: Maintainability

| ID | Requirement | Acceptance criterion |
|----|-------------|----------------------|
| N6.1 | Unit/component tests | Vitest + Testing Library cover the API client (CSRF, problem+json parsing, 401 handling), the R6 stat computation and the reflection form validation. `pnpm test` is green |
| N6.2 | E2E happy path per role | Playwright, against `./mvnw spring-boot:run` (dev) with the demo accounts (password `demo`). Every run signs up with a unique `e2e+<timestamp>@…` email, and `pnpm e2e:reset` deletes `../mvp/data` for a clean start. Includes one flow from a clean browser with no cookies (CSRF bootstrap, R11.3): a student signs up, reflects ×3, handles the intake modal, sees My Journey. A counselor views the student's detail. An admin creates a counselor, and that counselor's first sign-in forces a password change |
| N6.3 | CI | A frontend job in the root `.github/workflows/ci.yml` (Node 24, pnpm through corepack) runs `pnpm lint`, `pnpm typecheck` and `pnpm test` on every push and PR. E2E and smoke run locally (they need the backend) |
| N6.4 | Config by env | Public values are fixed at build time: `NEXT_PUBLIC_TERMS_VERSION` (default `2026-11-18`). `BACKEND_URL` (default `http://localhost:8080`) is only used by the dev rewrite. The production server needs no runtime env vars, and nothing is hard-coded per environment |
| N6.5 | Color-slot rule and token check | Components and pages use only named color slots (`background`, `surface`, `foreground`, `muted`, `muted-foreground`, `border`, `primary`, `primary-foreground`, `highlight`, `highlight-text`, `positive`, `positive-foreground`, `destructive`, `destructive-foreground`, `ring`) and never raw values. Raw values live only in `globals.css`. A Vitest token check (`src/lib/theme/tokens.test.ts`) reads the slot values per preset, asserts WCAG AA on the text/ground pairs, and fails on hex, `rgb()`, `oklch()`, `hsl()` or Tailwind palette classes in source |
| N6.6 | Theme e2e | Against the dev stack: the admin selects `harbor`, saves, sees the toast, `<html data-theme="harbor">` and the "Last changed" line, and the activity tab lists the change. A fresh signed-out context then opens `/signin` with `data-theme="harbor"`. The test resets the theme to `grove` afterwards |

### Look: Grove (update 1)

The default palette is **Grove**: a warm off-white `background` with white `surface` panels, forest green `primary` (buttons, active nav, wordmark, filled scale), a single orange `highlight` used only as a fill, and a pale blue-gray `positive` for YES and Active. Cards have no border or shadow. Each screen has at most one `primary`-filled button. The "Iterative Reflection" wordmark (semibold Geist, `primary`) sits at the top of each portal shell and above the sign-in form. Font stays Geist only. Update 2 (R13) adds three more presets that fill the same slots; Grove stays the default.

_Skipped areas:_ reliability/availability (the frontend is a stateless, client-rendered Node server, so uptime belongs to hosting and the backend), scalability (300-student pilot, and the backend is the bottleneck), cost (no paid services; the real host is picked later, see Open Questions), data retention (the frontend stores no data, only the backend does).

## Open Questions

- **Real terms text (R1.7):** a placeholder ships with `NEXT_PUBLIC_TERMS_VERSION=2026-11-18`. Real terms must be written, and the version bumped, before any school uses the app.
- **Hex values for `harbor`, `plum` and `slate` (R13.1):** no drafts exist yet (the earlier notes said there were, but the diff has none). Claude drafts a full slot set per preset that passes N3.4, and the developer picks the final values from the R13.5 mock cards, alongside the N5.3 brand check. Blocks R13.1.
- **Production host:** the frontend is deploy-ready (R12), and the compose rehearsal is the reference setup. The real host (same EC2 behind nginx, or another Node host) gets picked with the backend's EC2 phase. Either way `/api` must stay same-origin and served over HTTPS.
