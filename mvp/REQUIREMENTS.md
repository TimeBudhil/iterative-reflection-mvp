# Iterative Reflection MVP: Requirements

_Last updated: 2026-11-20 (added R11: settings module and admin activity log, update 2)_

> Codebase status: R1–R10 are built (the backend was frozen after the MVP, and the frontend in `../iterative-reflection-frontend` was built against it). R11 is the only planned addition, made for the frontend's theme presets (frontend R13, update 2). Everything else stays frozen. The status column is omitted: R1–R10 are `✅ existing`, R11 is `⬜ new`.

## Summary

**Problem:** Students don't know what careers fit them, and school counselors (≈1:328 ratio) can't give individualized guidance. Students need a self-directed way to test careers against their own interests, values and lifestyle, and counselors need a summary of that self-discovery so they can step in with real opportunities.
**Users:** High-school students (13+) who work through immersive career "experiences" on their own and reflect on each one. Counselors get a read-only view of the students who linked to them. A minimal admin manages counselor accounts.
**Success looks like:** In a pilot of 15–300 students, ≥60% of signed-up students reflect on ≥3 careers and ≥20% complete the intake form (measured by R9.5). Each student narrows down at least one career at a time with a Yes/Maybe/No verdict.
**Constraints:** No deadline. Backend only: a REST API consumed by a Next.js + React frontend built afterward. Spring Boot modular monolith (Spring Modulith) with Spring Security. This phase runs locally on H2 with seed data. The later production target is a single AWS EC2 instance with Postgres, and it has to stay cost-efficient.

## In-Scope

### R1: Accounts & authentication

| ID | Requirement | Acceptance criterion |
|----|-------------|----------------------|
| R1.1 | Student self-signup with email, password, display name, birthdate, `acceptedTermsVersion` and an optional `counselorCode` (R2.2) | `POST /api/auth/signup` returns 201 and creates a STUDENT user |
| R1.2 | Block signup for users under 13 | A birthdate making the user <13 returns 400 and creates no user |
| R1.12 | Terms acceptance is recorded at signup | A signup without `acceptedTermsVersion` returns 400. The accepted version and a UTC timestamp are stored on the user. Existing users are not forced to re-accept when the version changes (X16) |
| R1.13 | The birthdate is not persisted | After the R1.2 check, only an `ageVerifiedAt` timestamp is stored. No birthdate column exists in the schema |
| R1.3 | Password policy: ≥12 chars, BCrypt-hashed | A password of 11 chars returns 400. The DB stores a BCrypt hash, never plaintext |
| R1.4 | Email is unique (case-insensitive) | Signing up with an existing email (any case) returns 409 |
| R1.5 | Sign in with email + password creates a server-side session | `POST /api/auth/signin` returns 200 and sets an HttpOnly, Secure, SameSite cookie. A wrong password returns 401 with a generic message |
| R1.6 | Sign out invalidates the session | After `POST /api/auth/signout`, reusing the old cookie returns 401 |
| R1.7 | Current-user endpoint | `GET /api/me` returns id, email, display name, role and `intakePromptDue` (see R7.1). Returns 401 when anonymous |
| R1.8 | Roles: STUDENT, COUNSELOR, ADMIN; signup can only create STUDENT | A signup payload that includes a role field still yields a STUDENT |
| R1.9 | Any signed-in user can change their own password | `PUT /api/me/password {currentPassword, newPassword}` returns 204. A wrong current password returns 400. The new password follows R1.3. The session ID rotates |
| R1.10 | Forced password change on a temporary password | A user flagged `mustChangePassword` (counselors created by R9.2) gets 403 on every endpoint except `GET /api/me`, `PUT /api/me/password` and `POST /api/auth/signout`. A successful change clears the flag |
| R1.11 | A student can change their display name | `PATCH /api/me {displayName}` returns 200. Other fields in the payload (email, role) are ignored. Changing the email is out of scope (X18) |

### R2: Counselor linking (student-consented access)

| ID | Requirement | Acceptance criterion |
|----|-------------|----------------------|
| R2.1 | Each counselor has a unique, short join code | Every counselor record has a unique code of 6–8 chars with no ambiguous chars (0/O, 1/I) |
| R2.2 | A student can link to a counselor by entering the code at signup or later | `POST /api/students/me/counselors {code}` returns 201, and the student then appears in that counselor's list. At signup, an invalid `counselorCode` returns 404 and creates no account (signup and linking happen in one transaction) |
| R2.3 | Invalid code is rejected | An unknown code returns 404. Linking the same counselor twice returns 409 |
| R2.4 | A student can see and revoke their counselor links | `GET` lists the links. After `DELETE /api/students/me/counselors/{id}`, that counselor gets 403 on the student |
| R2.5 | A student may link to multiple counselors | Linking a second counselor's code returns 201, and both counselors see the student. Seed data links each demo student to exactly one counselor |
| R2.6 | Link attempts are rate-limited | The 11th failed link attempt by the same student within 15 min returns 429 |
| R2.7 | A counselor can rotate their join code | `POST /api/counselor/me/join-code/rotate` returns the new code. The old code then returns 404 on linking. Existing links are unaffected |
| R2.8 | A linked counselor sees the student's full history | A counselor linked after the student's reflections and intake were submitted still sees all of them. After a revoke and relink, access is fully restored |
| R2.9 | Links to deactivated counselors (R9.4) grant no access but are kept | The student's link list shows the link with `counselorActive: false`. Linking with a deactivated counselor's code returns 404. Reactivating the counselor restores the existing links |

### R3: Career catalog & multimedia experience

| ID | Requirement | Acceptance criterion |
|----|-------------|----------------------|
| R3.1 | A career has a title, a 3–5 sentence appealing summary, tags, an estimated duration (5–10 min) and ordered pages | Seeded careers expose all fields via the API. Summary and duration are validated on load |
| R3.2 | A page contains ordered content blocks: TEXT, VIDEO_EMBED, AUDIO_EMBED, IMAGE_URL, EXTERNAL_LINK | `GET /api/careers/{id}/experience` returns pages with typed blocks in order |
| R3.3 | Embed URLs are restricted to an allowlist of hosts (e.g. YouTube, Vimeo, SoundCloud, Spotify) | Loading a block with a non-allowlisted host fails validation and the app refuses to seed it |
| R3.4 | Page progress is tracked per student | `POST /api/careers/{id}/pages/{n}/viewed` records progress. Re-opening the experience returns `lastViewedPage` |
| R3.5 | Tags come from a controlled taxonomy (e.g. cluster, work style, environment). The seed agent drafts the taxonomy along with the career content, and you approve it before the seed migration is committed | A tag not in the taxonomy can't be attached to a career |
| R3.6 | Career content is read-only via the API | No create/update/delete career endpoints exist. Content arrives only through Flyway seed migrations (R10) |
| R3.7 | Careers can be retired | A career with `active = false` is never served by the feed. Existing reflections, abandons and counselor views of it stay intact |
| R3.8 | Page progress survives content changes | If a migration lowers a career's page count below a student's `lastViewedPage`, the experience returns `lastViewedPage` clamped to the new last page |

### R4: Career feed

| ID | Requirement | Acceptance criterion |
|----|-------------|----------------------|
| R4.1 | `GET /api/feed/next` returns the student's in-progress career if one exists, otherwise the next ranked unseen career. A career becomes in progress as soon as the feed serves it, and the served card is stored per student | With an unreflected career served, repeated calls (including after sign-out/in) return that same career, so the exploration slot can't be re-rolled by refreshing |
| R4.2 | A student can't start a new career until the current one is reflected on or abandoned (R4.7) | Requesting or starting a different career while one is in progress returns 409 |
| R4.3 | Reflected careers never reappear in the feed | After reflecting on all but one career, the feed returns only the remaining one |
| R4.4 | Ranking is by summed tag affinity (R6), with an exploration slot. The ratio is configurable (`feed.exploration-every`, default 5) | By default, 4 of every 5 cards are the highest-affinity unseen career, and the 5th is a random unseen career. Tests use a fixed random seed to make this deterministic. The slot position is computed as (reflections + abandons) mod `feed.exploration-every`, with no extra stored counter. Ties in affinity are broken by the catalog's default order |
| R4.5 | Cold start uses the catalog's default order | A new student with no reflections or intake gets careers in the seeded default order (exploration slot still applies) |
| R4.6 | Exhausted feed returns an empty state | When no active career is left to serve (see R4.8), the response is 200 with `{career: null, exhausted: true}` |
| R4.7 | A student can abandon an opened career without reflecting | `POST /api/careers/{id}/abandon` returns 204 once `lastViewedPage ≥ 1`, and returns 409 before any page is viewed (no skipping from the summary card). It clears the in-progress card and applies the R6.7 penalty |
| R4.8 | Abandoned careers come back after the unseen pool runs out | Ranked and exploration picks skip abandoned careers while unseen ones remain. Once none are unseen, abandoned careers are served (by affinity) before `exhausted: true` is returned |

### R5: Reflection

| ID | Requirement | Acceptance criterion |
|----|-------------|----------------------|
| R5.1 | The reflection question set is global and stored in the DB. Each question has a 1–10 scale and an optional open-ended prompt. Questions are versioned | Seeded with ≥5 dimensions: genuine interest, values alignment, lifestyle fit, everyday responsibilities, sacrifices/trade-offs (e.g. "What sacrifices would I have to make?"). Rewording creates a new version, and old answers keep their original version |
| R5.2 | `GET /api/reflection-questions` returns the active set | Returns the active questions in order with their scale and prompt text |
| R5.3 | A reflection unlocks only after the last page of the experience was viewed | Submitting before the last page is viewed returns 409 |
| R5.4 | Every scale answer is required and must be an integer 1–10. Open answers are optional, ≤1000 chars | A missing scale, a 0 or 11, or text over 1000 chars returns 400 with a per-field message |
| R5.5 | A final verdict "Would I genuinely enjoy this?" with the answers YES / MAYBE / NO is required | Submitting without a verdict returns 400 |
| R5.6 | One reflection per student per career. It's editable, and edits keep the history (latest wins) | Editing updates the current answers. Earlier versions stay queryable with timestamps. An edit is answered against the currently active question set (R5.1), even if the original used an older version |
| R5.7 | A student can list their own reflections | `GET /api/students/me/reflections` returns career, verdict, scores, open answers and timestamps |

### R6: Tag affinity (feed tuning)

| ID | Requirement | Acceptance criterion |
|----|-------------|----------------------|
| R6.1 | Each student has a per-tag affinity score | Stored per (student, tag). Starts at 0 |
| R6.2 | A reflection contributes `delta = (mean of scale answers − 5.5) / 4.5` (range −1..+1) to every tag on that career | All 10s gives +1.0 per tag, all 1s gives −1.0, all 5.5 gives 0. The mean covers whatever scale answers that reflection version contains, with no normalization across question-set versions. The verdict doesn't affect the delta (X15). Covered by unit tests |
| R6.3 | Editing a reflection replaces its earlier contribution (no double counting) | Changing from all 10s to all 1s moves each tag from +1.0 to −1.0, not to 0 |
| R6.4 | Intake interests seed affinity: +0.5 for each selected interest tag | After intake with 2 interest tags, those 2 tags are at +0.5 |
| R6.6 | Affinity weights are configurable properties (`affinity.neutral-score` default 5.5, `affinity.scale` default 4.5, `affinity.intake-boost` default 0.5) | Changing a property in config changes the computed delta, verified by a test with overridden properties |
| R6.5 | Saving a reflection and updating affinity happen in one transaction | A forced failure in the affinity update leaves no reflection saved |
| R6.7 | Abandoning a career applies a configurable penalty | Each tag on the abandoned career moves by `affinity.abandon-penalty` (default −0.5), in the same transaction as the abandon. Covered by a test with an overridden property |
| R6.8 | Affinity isn't recomputed when a career's tags change | A migration that changes a career's tags leaves existing affinity scores untouched. Recomputing them is a later phase (X14) |

### R7: Intake form

| ID | Requirement | Acceptance criterion |
|----|-------------|----------------------|
| R7.1 | The intake is offered after the student's 3rd reflection | `GET /api/me` returns `intakePromptDue: true` with ≥3 reflections, no intake and no dismissal. `POST /api/intake` returns 409 with <3 reflections. Abandons (R4.7) don't count toward the 3 |
| R7.2 | Intake questions are DB-defined: interests (multi-select from the tag taxonomy), favorite subjects, free-text goals, "Why are you using this app?", "What do you expect from it?" | `GET /api/intake/questions` returns the seeded set. You can add questions without code changes |
| R7.3 | The intake is optional. The student can dismiss the prompt | After `POST /api/intake/dismiss`, `intakePromptDue` is false, and the intake can still be submitted later |
| R7.4 | The intake is editable after submission | `PUT /api/intake` updates the answers and re-applies the R6.4 interest seeding (replacing the previous seeding, not adding to it) |

### R8: Counselor dashboard (read-only)

| ID | Requirement | Acceptance criterion |
|----|-------------|----------------------|
| R8.1 | List of linked students with progress summary | `GET /api/counselor/students` returns, per student: careers seen, reflected, abandoned, YES/MAYBE/NO counts, intake completed (bool), last active timestamp. Last active = the latest page view, reflection, abandon or intake action (signing in doesn't count). The list is paginated per N8.1 and sorted by last active, newest first |
| R8.2 | Student detail view | `GET /api/counselor/students/{id}` returns all reflections (scores, open answers, verdict), abandoned careers with their `lastViewedPage`, top 5 tag affinities and intake answers |
| R8.3 | A counselor can only see students linked to them | Requesting an unlinked student returns 403, and the list never includes unlinked students |
| R8.4 | Counselor access is read-only | No counselor endpoint changes student data. A security test asserts that every counselor route is GET, except the explicit allowlist: join-code rotation (R2.7) and the user's own password change (R1.9) |
| R8.5 | A counselor can see their own join code | `GET /api/counselor/me` returns the profile and join code |
| R8.6 | Counselor reads of student detail are audit-logged | Every `GET /api/counselor/students/{id}` writes an audit row (counselor, student, UTC timestamp), including on 403. Students can't see audit rows (X19) |

### R9: Admin

| ID | Requirement | Acceptance criterion |
|----|-------------|----------------------|
| R9.1 | A bootstrap admin is created from config/env on first start | With `ADMIN_EMAIL`/`ADMIN_PASSWORD` set, startup creates the admin once. Restarting doesn't duplicate it. Startup fails if `ADMIN_PASSWORD` doesn't meet R1.3. The env password is trusted (no forced change) |
| R9.2 | The admin creates counselor accounts with a temporary password | `POST /api/admin/counselors {email, displayName, temporaryPassword}` returns 201 with a counselor that has a join code and `mustChangePassword: true` (R1.10). A non-admin gets 403 |
| R9.3 | The admin lists counselors | `GET /api/admin/counselors` returns each counselor's email, display name, join code, active flag and linked-student count, paginated per N8.1 |
| R9.4 | The admin can deactivate and reactivate a counselor (no hard delete) | `PATCH /api/admin/counselors/{id} {active:false}` invalidates the counselor's sessions. Their sign-in then returns the generic 401, and their links grant no access (R2.9). `{active:true}` restores sign-in and existing links |
| R9.5 | Pilot-metrics endpoint | `GET /api/admin/pilot-metrics` returns total students, % with ≥3 reflections, % intake completed and the YES/MAYBE/NO distribution. It counts only active STUDENT accounts not flagged `demo`, all-time by default, with optional `from`/`to` filters on signup date |
| R9.6 | The admin can view the counselor audit log | `GET /api/admin/audit?studentId=` returns the R8.6 rows, newest first, paginated per N8.1. Rows are kept indefinitely (retention policy: X17) |

### R10: Local dev environment & seed data

| ID | Requirement | Acceptance criterion |
|----|-------------|----------------------|
| R10.1 | The `dev` profile runs on H2 with Flyway migrations and seed data | `./mvnw spring-boot:run` starts with the schema and seed in place, plus demo accounts (1 admin, 1 counselor, 2 students, each student linked to that one counselor). Seeded accounts are flagged `demo` |
| R10.2 | Seed career content is generated by an agent after the Career module is built, so it matches the real schema/endpoints | A seed migration with ≥20 careers covering ≥5 clusters, each with a 3–5 sentence summary, ≥3 pages, ≥1 media embed and ≥2 taxonomy tags, passes R3 validation |
| R10.3 | Seed migrations are idempotent / run once | Restarting the app doesn't duplicate any seed rows |
| R10.4 | The same migrations run on Postgres (the later `prod` profile) | Migrations use only SQL portable between H2 (PostgreSQL mode) and Postgres |

### R11: Settings module and admin activity log (update 2)

A new Spring Modulith module, `settings`, owns storage of app-wide settings and nothing else. Each setting is exposed through its own small typed service, so a future setting is a new service, not a schema change. The `admin` module owns a log of admin actions.

| ID | Requirement | Acceptance criterion |
|----|-------------|----------------------|
| R11.1 | Anyone can read the app theme | `GET /api/settings/theme` returns `{presetId}` without authentication (it's needed on `/signin`), with `Cache-Control: no-cache`. With nothing saved it returns `grove`. It never returns who changed it or when. Only GET is `permitAll` |
| R11.2 | Only an admin can change the theme | `PUT /api/admin/settings/theme {presetId}` returns 200 with `{presetId, updatedAt, updatedByName}`. An id outside `grove`, `harbor`, `plum`, `slate` returns 400 with a field error. A non-admin gets 403. CSRF applies (N2.3). Saving the current value returns 200 but publishes no event, writes no activity row and leaves `updatedAt` unchanged |
| R11.3 | Admin view of the theme | `GET /api/admin/settings/theme` returns `{presetId, updatedAt, updatedByName}`, with `updatedAt`/`updatedByName` null when nothing has been saved. Served by `admin`, which combines `ThemeSettings` with the display name from `identity`. A non-admin gets 403 |
| R11.4 | Generic storage, typed access | One `app_settings` table (`key` primary key, `value_json`, `updated_by`, `updated_at` in UTC), created by a Flyway migration that's portable per R10.4. The persistence class is in `settings.internal`. A public `ThemeSettings` service validates the preset id and reads/writes through it. No other module touches the table |
| R11.5 | Changes publish an event | Every save that changes the value publishes a `SettingChanged` event (key, old value, new value, admin id, timestamp). An `@ApplicationModuleTest` asserts it's published on a change and not on a same-value save |
| R11.6 | Admin activity log, owned by `admin` | An `admin_activity` table in `admin.internal` (id, admin id, action, detail JSON, `created_at` UTC; portable per R10.4). Actions: `THEME_CHANGED` (from a listener on `SettingChanged`, detail old → new), `COUNSELOR_CREATED`, `COUNSELOR_DEACTIVATED` and `COUNSELOR_REACTIVATED` (written in the admin counselor flow, detail = counselor email; a PATCH that doesn't change `active` logs nothing). `settings` has no dependency on `admin` (N4.1). Separate from the counselor-view audit log (R8.6) |
| R11.7 | The log is best-effort | A failed activity write never fails the action that caused it: the theme save or counselor action still commits and returns its normal status, and the failure is logged at WARN with the request ID. The theme row is written after commit. A test forces the insert to fail and asserts the action stuck. (A deliberate exception to the same-transaction listener rule of `ReflectionSaved`/`IntakeSaved`) |
| R11.8 | The admin reads the activity log | `GET /api/admin/activity` returns rows `{createdAt, adminName, action, detail}`, newest first, paginated per N8.1. A non-admin gets 403 |
| R11.9 | The backend stores only the preset id | No colors are stored or returned. Palettes live in the frontend |

## Stretch

- **S1: Static guides (Phase 2 preview)**: read-only guide content such as the LinkedIn/networking guide, delivered as a Guide module. _Why stretch:_ it's cheap, read-only content, but the core loop doesn't need it.
- **S2: Password reset (manual)**: an admin endpoint to reset a user's password. _Why stretch:_ there's no email in the MVP, and it's only needed if pilot users get locked out.
- **S3: Student data export & account deletion**: a student downloads their data as JSON and can delete their account. _Why stretch:_ good privacy hygiene, but not required for a pilot.
- **S4: Student self-insight summary**: a student sees their own top tags and their YES/MAYBE/NO history. _Why stretch:_ useful for self-awareness, but secondary to the feed + reflection loop.
- **S5: Testcontainers Postgres test**: run the integration suite against real Postgres. _Why stretch:_ a safety net for the later EC2 move.
- **S6: Backfill `COUNSELOR_CREATED` activity rows**: a one-off migration adds rows for counselors that existed before R11.6, attributed to the bootstrap admin at the counselor's creation time. _Why stretch:_ the log starting empty is fine for a pilot, and the attribution is a guess.

## Out-of-Scope

All of these are planned for **later phases** of the application, not dropped.

- **X1: Phase 2 assignments**: research-a-professional, interview assignments, reflect & iterate. _Later phase:_ next release after the career-feed loop is proven.
- **X2: Phase 3 counselor-required workflows**: skill analysis, resume creation, internship/shadowing search, counselor guidance/approval. _Later phase:_ needs a full counselor workflow.
- **X3: "Assignments in progress" on the counselor dashboard**: _Later phase:_ depends on X1.
- **X4: Admin UI for content management**: _Later phase:_ the seed agent and Flyway migrations cover content for now.
- **X5: Email (verification, password-reset links, notifications)**: _Later phase:_ avoids provider cost/setup in the MVP.
- **X6: Connecting students to real professionals**: _Later phase:_ the long-term vision, built once the content model is proven.
- **X7: Native mobile app**: _Later phase:_ the Next.js web frontend comes first.
- **X8: Payments / subscriptions**: _Later phase:_ not needed for a pilot.
- **X9: Media upload / self-hosted media**: _Later phase:_ the MVP uses external embeds only, at $0 cost.
- **X10: AI-generated content / ML recommendations**: _Later phase:_ rule-based tag affinity is enough for MVP data volumes.
- **X11: Internationalization**: _Later phase:_ the pilot is English only.
- **X12: AWS EC2 deployment + Postgres**: _Later phase:_ the next phase after the MVP. This phase keeps the app Postgres-ready (R10.4, N6).
- **X13: Frontend (Next.js + React)**: _Later phase:_ built separately against this API's OpenAPI contract.
- **X14: Recomputing affinity when career tags change**: _Later phase:_ easier once semantic embeddings replace the tag model (see R6.8).
- **X15: Verdict-weighted affinity**: _Later phase:_ verdicts are stored now, so the weighting can be tuned from pilot data.
- **X16: Forced re-acceptance of new terms versions**: _Later phase:_ the MVP only records the accepted version (R1.12).
- **X17: Audit-log retention policy**: _Later phase:_ decide alongside the FERPA / district data review before a real school pilot. Covers the R11.6 admin activity log too, as do exports of either log.
- **X18: Email change**: _Later phase:_ needs email verification (X5).
- **X19: Student-visible "who viewed my data" log**: _Later phase:_ the audit rows (R8.6) already exist, so this is just an endpoint later.
- **X20: Storing theme colors, per-user or per-school themes**: _Not planned:_ the backend stores one app-wide preset id (R11.9).
- **X21: Logging other admin actions**: _Not planned for now:_ sign-ins and profile/password changes aren't logged. The R11.6 action list can grow later.

## Non-functional

### N1: Performance

| ID | Requirement | Acceptance criterion |
|----|-------------|----------------------|
| N1.1 | API latency | p95 < 300 ms for every endpoint, measured locally with seed data plus a synthetic load of 300 students × 20 reflections |
| N1.2 | Feed query latency | `GET /api/feed/next` p95 < 100 ms under the N1.1 dataset |

### N2: Security & privacy

| ID | Requirement | Acceptance criterion |
|----|-------------|----------------------|
| N2.1 | Session auth via Spring Security, with sessions stored server-side in Spring Session JDBC | The cookie is HttpOnly, Secure (prod) and SameSite=Lax. The session ID rotates on login (prevents session fixation). A session survives an app restart. The session tables come from Flyway migrations that are portable per R10.4 |
| N2.2 | Session timeouts | Idle 30 min and absolute 12 h. Expired sessions return 401 |
| N2.3 | CSRF protection on state-changing requests | A POST/PUT/DELETE without a valid CSRF token returns 403 |
| N2.4 | CORS restricted to the configured frontend origin | A request from any other origin is blocked by CORS. The origin comes from config |
| N2.5 | Login rate limiting | The 6th failed attempt within 15 min for the same account or IP returns 429. Rate-limit counters (here and R2.6) are kept in memory and may reset on restart |
| N2.6 | Role- and ownership-based authorization on every endpoint | Tests cover: a student can't read another student's data, a counselor gets 403 on unlinked students, and non-admins get 403 on `/api/admin/**` |
| N2.7 | Error responses don't leak internals | No stack traces or SQL in any response body |
| N2.8 | Minors' data minimization | Only the fields in R1–R9 and R11 are stored (R11 holds no student data). The birthdate is used only for the age gate and never persisted (R1.13). No third-party tracking |

### N3: Reliability

| ID | Requirement | Acceptance criterion |
|----|-------------|----------------------|
| N3.1 | Consistent error format | Every error is RFC 7807 `application/problem+json`. Validation errors list each failing field |
| N3.2 | No partial writes on multi-step operations | Reflection + affinity (R6.5) and intake + seeding (R7.4) are transactional, and tests force a failure to prove the rollback |
| N3.3 | Reflection history is never hard-deleted | Edits create new versions, and no endpoint deletes reflection history |
| N3.4 | Concurrent edits don't double-count | Reflections and affinity rows use optimistic locking (`@Version`), so a stale edit returns 409. A unique (student, career) constraint means a double-submitted reflection can't create two reflections. A test fires two concurrent submits and asserts that only one counts toward affinity |

### N4: Maintainability & extensibility

| ID | Requirement | Acceptance criterion |
|----|-------------|----------------------|
| N4.1 | Modular monolith boundaries enforced | The `ApplicationModules.of(MvpApplication.class).verify()` test passes. Modules: auth/identity, student, counselor, career, feed, reflection, intake, admin, settings, with later ones (guide, assignment) added without touching the existing internals. `settings` depends on no other feature module |
| N4.2 | Cross-module communication only via public APIs or application events | The Modulith verify test catches any access to another module's `internal` package |
| N4.3 | Integration tests per module | Each module has ≥1 `@ApplicationModuleTest`. `./mvnw verify` is green |
| N4.4 | API contract published for the frontend | springdoc serves `/v3/api-docs` and Swagger UI in `dev`, and every endpoint has request/response schemas |
| N4.5 | Continuous integration | A GitHub Actions workflow runs `./mvnw verify` on Java 25 for every push and PR, and a failing build blocks the merge |

### N5: Operability

| ID | Requirement | Acceptance criterion |
|----|-------------|----------------------|
| N5.1 | Health and metrics | `/actuator/health` is public. `/actuator/prometheus` and the other actuator endpoints require ADMIN (or aren't exposed in prod) |
| N5.2 | Structured logs with a request ID | Every log line is JSON with a `requestId`, and the request ID is returned in an `X-Request-Id` response header |
| N5.3 | Pilot metrics exported to Prometheus | `/actuator/prometheus` exposes gauges matching R9.5 (student count, % ≥3 reflections, % intake completed) |

### N6: Cost & portability

| ID | Requirement | Acceptance criterion |
|----|-------------|----------------------|
| N6.1 | Runs on a single small instance | The app starts and passes N1 with `-Xmx512m`, so it fits a t4g.small-class EC2 instance (2 GB) |
| N6.2 | No paid third-party services in this phase | The dependency list has no paid SaaS. Media is external embeds |
| N6.3 | Profile-based DB switch | The `dev` profile uses H2. The `prod` profile reads Postgres connection settings from env vars, with no code changes |

### N7: Data

| ID | Requirement | Acceptance criterion |
|----|-------------|----------------------|
| N7.1 | Seed data is idempotent | See R10.3 |
| N7.2 | Timestamps are stored in UTC | Every timestamp column is `TIMESTAMP WITH TIME ZONE`, or an Instant serialized as ISO-8601 UTC |

### N8: API conventions

| ID | Requirement | Acceptance criterion |
|----|-------------|----------------------|
| N8.1 | List endpoints are paginated | Every list endpoint accepts `page`/`size` (default 20, max 100, larger values clamped) and returns page metadata. This covers R2.4, R5.6 history, R5.7, R8.1, R9.3, R9.6 and R11.8 |
| N8.2 | List endpoints have a defined default sort | Reflections sort by `updatedAt`, newest first. Counselor students sort by last active, newest first (R8.1). The audit log and the admin activity log (R11.8) sort newest first |

_Skipped areas:_ accessibility and browser compatibility (backend only, so they belong to the Next.js frontend), usability (frontend concern; the API supports it through clear errors and `intakePromptDue`), i18n (the pilot is English only, X11), scalability beyond the pilot (a single instance covers 300 students; revisit in the EC2 phase).

## Open Questions

- **Re-abandoning a resurfaced career (R4.8):** if a student abandons a career that came back through R4.8, is it then permanently gone, and does the R6.7 penalty apply again? _Recommendation:_ yes to both (a second abandon is final, so R4.6 can eventually return `exhausted: true` and the feed can't loop forever).
