# Iterative Reflection MVP: Requirements

_Last updated: 2026-11-18_

> Codebase status: fresh Spring Boot 4.1.2 / Java 25 / Spring Modulith 2.1.1 scaffold (JPA, H2, Security, Actuator, Prometheus). No features built yet, so every requirement below is `⬜ new`; the status column is omitted.

## Summary

**Problem:** Students don't know what careers fit them, and school counselors (≈1:328 ratio) can't give individualized guidance. Students need a self-directed way to test careers against their own interests, values and lifestyle, and counselors need a summary of that self-discovery so they can step in with real opportunities.
**Users:** High-school students (13+) who work through immersive career "experiences" on their own and reflect on each one. Counselors get a read-only view of the students who linked to them. A minimal admin manages counselor accounts.
**Success looks like:** In a pilot of 15–300 students, ≥60% of signed-up students reflect on ≥3 careers and ≥20% complete the intake form (measured by R9.5). Each student narrows down at least one career at a time with a Yes/Maybe/No verdict.
**Constraints:** No deadline. Backend only: a REST API consumed by a Next.js + React frontend built afterward. Spring Boot modular monolith (Spring Modulith) with Spring Security. This phase runs locally on H2 with seed data. The later production target is a single AWS EC2 instance with Postgres, and it has to stay cost-efficient.

## In-Scope

### R1: Accounts & authentication

| ID | Requirement | Acceptance criterion |
|----|-------------|----------------------|
| R1.1 | Student self-signup with email, password, display name, birthdate, `acceptedTermsVersion` (required) and `counselorCode` (optional, see R2.2) | `POST /api/auth/signup` returns 201 and creates a STUDENT user. A missing `acceptedTermsVersion` returns 400 |
| R1.2 | Block signup for users under 13. The birthdate is discarded after the check | A birthdate making the user <13 returns 400 and creates no user. Only an `ageVerifiedAt` timestamp is stored, never the birthdate |
| R1.3 | Password policy: ≥12 chars, BCrypt-hashed | A password of 11 chars returns 400. The DB stores a BCrypt hash, never plaintext |
| R1.4 | Email is unique (case-insensitive) | Signing up with an existing email (any case) returns 409 |
| R1.5 | Sign in with email + password creates a server-side session | `POST /api/auth/signin` returns 200 and sets an HttpOnly, Secure, SameSite cookie. A wrong password returns 401 with a generic message |
| R1.6 | Sign out invalidates the session | After `POST /api/auth/signout`, reusing the old cookie returns 401 |
| R1.7 | Current-user endpoint | `GET /api/me` returns id, email, display name, role and `intakePromptDue` (see R7.1). Returns 401 when anonymous |
| R1.8 | Roles: STUDENT, COUNSELOR, ADMIN; signup can only create STUDENT | A signup payload that includes a role field still yields a STUDENT |
| R1.9 | Any signed-in user can change their password | `PUT /api/me/password {currentPassword, newPassword}` returns 204, enforces R1.3 and rotates the session. A wrong current password returns 401 |
| R1.10 | Admin-created accounts must change their temporary password | A counselor created by R9.2 has `mustChangePassword: true`. Until they call R1.9, every other endpoint except `GET /api/me` and sign-out returns 403 |
| R1.11 | A user can edit their display name (only) | `PATCH /api/me {displayName}` returns 200. Any other field in the payload is ignored. Email changes are not supported (X5) |
| R1.12 | Terms acceptance is recorded | The accepted terms version and a UTC timestamp are stored per user. A new terms version does not force re-acceptance (X16) |
| R1.13 | Deactivated users can't sign in | Sign-in for a deactivated account returns the same generic 401 as a wrong password |

### R2: Counselor linking (student-consented access)

| ID | Requirement | Acceptance criterion |
|----|-------------|----------------------|
| R2.1 | Each counselor has a unique, short join code | Every counselor record has a unique code of 6–8 chars with no ambiguous chars (0/O, 1/I) |
| R2.2 | A student can link to a counselor by entering the code at signup or later | `POST /api/students/me/counselors {code}` returns 201, and the student then appears in that counselor's list. At signup, an invalid `counselorCode` returns 404 and creates no account (atomic) |
| R2.3 | Invalid code is rejected | An unknown code, or the code of a deactivated counselor, returns 404. Linking the same counselor twice returns 409 |
| R2.4 | A student can see and revoke their counselor links | `GET` lists the links, each with `counselorActive`. After `DELETE /api/students/me/counselors/{id}`, that counselor gets 403 on the student |
| R2.5 | A student may link to multiple counselors | Linking a second counselor's code returns 201, and both counselors see the student. Seed data links each demo student to exactly one counselor |
| R2.6 | A linked counselor sees the student's full history | Reflections made before the link are visible. After revoke + relink, the full history is visible again |
| R2.7 | Link attempts are rate-limited | The 11th failed link attempt by one student within 15 min returns 429 |
| R2.8 | A counselor can rotate their join code | `POST /api/counselor/me/join-code/rotate` returns the new code. The old code returns 404 immediately. Existing links are unaffected |

### R3: Career catalog & multimedia experience

| ID | Requirement | Acceptance criterion |
|----|-------------|----------------------|
| R3.1 | A career has a title, a 3–5 sentence appealing summary, tags, an estimated duration (5–10 min) and ordered pages | Seeded careers expose all fields via the API. Summary and duration are validated on load |
| R3.2 | A page contains ordered content blocks: TEXT, VIDEO_EMBED, AUDIO_EMBED, IMAGE_URL, EXTERNAL_LINK | `GET /api/careers/{id}/experience` returns pages with typed blocks in order |
| R3.3 | Embed URLs are restricted to an allowlist of hosts (e.g. YouTube, Vimeo, SoundCloud, Spotify) | Loading a block with a non-allowlisted host fails validation and the app refuses to seed it |
| R3.4 | Page progress is tracked per student | `POST /api/careers/{id}/pages/{n}/viewed` records progress. Re-opening the experience returns `lastViewedPage` |
| R3.5 | Tags come from a controlled taxonomy (e.g. cluster, work style, environment). The seed agent drafts the taxonomy along with the career content, and you approve it before the seed migration is committed | A tag not in the taxonomy can't be attached to a career |
| R3.6 | Career content is read-only via the API | No create/update/delete career endpoints exist. Content arrives only through Flyway seed migrations (R10) |
| R3.7 | Careers can be retired through a migration | A career with `active = false` never appears in the feed. Existing reflections on it stay visible to the student and counselors |
| R3.8 | Page progress survives content changes | If a migration reduces a career's page count below a student's `lastViewedPage`, the API returns it clamped to the last page |

### R4: Career feed

| ID | Requirement | Acceptance criterion |
|----|-------------|----------------------|
| R4.1 | `GET /api/feed/next` returns the student's in-progress career if one exists, otherwise the next ranked unseen career. A career becomes in-progress as soon as the feed serves it, and the served card is stored per student | Repeated calls return the same career, including an exploration-slot card, until it is reflected on or abandoned |
| R4.2 | A student can't start a new career until the current one is reflected on or abandoned (R4.7) | Requesting or starting a different career while one is in progress returns 409 |
| R4.3 | Reflected careers never reappear in the feed | After reflecting on all but one career, the feed returns only the remaining one |
| R4.4 | Ranking is by summed tag affinity (R6), with an exploration slot. The ratio is configurable (`feed.exploration-every`, default 5). The slot position is `(reflections + abandons) mod feed.exploration-every`, so no counter is stored. Ties break by the catalog's default order | By default, 4 of every 5 cards are the highest-affinity unseen career, and the 5th is a random unseen career. Tests use a fixed random seed to make this deterministic |
| R4.5 | Cold start uses the catalog's default order | A new student with no reflections or intake gets careers in the seeded default order (exploration slot still applies) |
| R4.6 | Exhausted feed returns an empty state | When no unseen or returnable abandoned career is left (R4.8), the response is 200 with `{career: null, exhausted: true}` |
| R4.7 | A student can abandon the in-progress career after opening its experience | `POST /api/careers/{id}/abandon` returns 204 once page 1 has been viewed, and the career leaves the feed. Before any page is viewed, it returns 409 |
| R4.8 | Abandoned careers can come back | Abandoned careers are excluded from ranking until every unseen active career is used up, then they are served again. A second abandon is final |

### R5: Reflection

| ID | Requirement | Acceptance criterion |
|----|-------------|----------------------|
| R5.1 | The reflection question set is global and stored in the DB. Each question has a 1–10 scale and an optional open-ended prompt. Questions are versioned | Seeded with ≥5 dimensions: genuine interest, values alignment, lifestyle fit, everyday responsibilities, sacrifices/trade-offs (e.g. "What sacrifices would I have to make?"). Rewording creates a new version, and old answers keep their original version |
| R5.2 | `GET /api/reflection-questions` returns the active set | Returns the active questions in order with their scale and prompt text |
| R5.3 | A reflection unlocks only after the last page of the experience was viewed | Submitting before the last page is viewed returns 409 |
| R5.4 | Every scale answer is required and must be an integer 1–10. Open answers are optional, ≤1000 chars | A missing scale, a 0 or 11, or text over 1000 chars returns 400 with a per-field message |
| R5.5 | A final verdict "Would I genuinely enjoy this?" with the answers YES / MAYBE / NO is required | Submitting without a verdict returns 400 |
| R5.6 | One reflection per student per career. It's editable, and edits keep the history (latest wins) | Editing updates the current answers. Earlier versions stay queryable with timestamps |
| R5.7 | A student can list their own reflections | `GET /api/students/me/reflections` returns career, verdict, scores, open answers and timestamps |
| R5.8 | Edits use the currently active question set | An edit made after a question-set change is validated against, and stored with, the active version. The R6.2 mean covers whatever scale answers that version contains |

### R6: Tag affinity (feed tuning)

| ID | Requirement | Acceptance criterion |
|----|-------------|----------------------|
| R6.1 | Each student has a per-tag affinity score | Stored per (student, tag). Starts at 0 |
| R6.2 | A reflection contributes `delta = (mean of scale answers − 5.5) / 4.5` (range −1..+1) to every tag on that career | All 10s gives +1.0 per tag, all 1s gives −1.0, all 5.5 gives 0. Covered by unit tests |
| R6.3 | Editing a reflection replaces its earlier contribution (no double counting) | Changing from all 10s to all 1s moves each tag from +1.0 to −1.0, not to 0 |
| R6.4 | Intake interests seed affinity: +0.5 for each selected interest tag | After intake with 2 interest tags, those 2 tags are at +0.5 |
| R6.6 | Affinity weights are configurable properties (`affinity.neutral-score` default 5.5, `affinity.scale` default 4.5, `affinity.intake-boost` default 0.5) | Changing a property in config changes the computed delta, verified by a test with overridden properties |
| R6.5 | Saving a reflection and updating affinity happen in one transaction | A forced failure in the affinity update leaves no reflection saved |
| R6.7 | An abandon contributes a fixed penalty (`affinity.abandon-penalty`, default −0.5) to every tag on that career | Each (student, career) holds a single contribution: repeating the abandon doesn't stack, and a later reflection replaces the penalty with its R6.2 delta |
| R6.8 | Affinity uses scale answers only. Verdicts are stored but don't affect it | A YES verdict with all-4 scores yields the same delta as a NO with all-4 scores (verdict weighting is X15) |

### R7: Intake form

| ID | Requirement | Acceptance criterion |
|----|-------------|----------------------|
| R7.1 | The intake is offered after the student's 3rd reflection | `GET /api/me` returns `intakePromptDue: true` with ≥3 reflections, no intake and no dismissal. `POST /api/intake` returns 409 with <3 reflections. Abandons don't count |
| R7.2 | Intake questions are DB-defined: interests (multi-select from the tag taxonomy), favorite subjects, free-text goals, "Why are you using this app?", "What do you expect from it?" | `GET /api/intake/questions` returns the seeded set. You can add questions without code changes |
| R7.3 | The intake is optional. The student can dismiss the prompt | After `POST /api/intake/dismiss`, `intakePromptDue` is false, and the intake can still be submitted later |
| R7.4 | The intake is editable after submission | `PUT /api/intake` updates the answers and re-applies the R6.4 interest seeding (replacing the previous seeding, not adding to it) |

### R8: Counselor dashboard (read-only)

| ID | Requirement | Acceptance criterion |
|----|-------------|----------------------|
| R8.1 | List of linked students with progress summary | `GET /api/counselor/students` returns, per student: careers seen, reflected, abandoned, YES/MAYBE/NO counts, intake completed (bool), last active timestamp. "Last active" is the latest page view, reflection or intake action (not sign-in) |
| R8.2 | Student detail view | `GET /api/counselor/students/{id}` returns all reflections (scores, open answers, verdict), abandoned careers with their `lastViewedPage`, top 5 tag affinities and intake answers |
| R8.3 | A counselor can only see students linked to them | Requesting an unlinked student returns 403, and the list never includes unlinked students. Links to a deactivated counselor grant no access |
| R8.4 | Counselor access is read-only | No counselor endpoint changes student data. A security test asserts that every counselor route is GET, except the R2.8 join-code rotation (which only changes the counselor's own record) |
| R8.5 | A counselor can see their own join code | `GET /api/counselor/me` returns the profile and join code |
| R8.6 | Counselor reads of student data are audit-logged | Every counselor `GET` on a student detail writes an audit row (counselor, student, UTC timestamp). Students can't see audit rows. Rows are kept indefinitely (X17) |

### R9: Admin

| ID | Requirement | Acceptance criterion |
|----|-------------|----------------------|
| R9.1 | A bootstrap admin is created from config/env on first start | With `ADMIN_EMAIL`/`ADMIN_PASSWORD` set, startup creates the admin once. Restarting doesn't duplicate it. If `ADMIN_PASSWORD` fails the R1.3 policy, startup fails. The env password is trusted (no `mustChangePassword`) |
| R9.2 | The admin creates counselor accounts with a temporary password | `POST /api/admin/counselors` returns 201 with a counselor that has a join code and `mustChangePassword: true` (R1.10). A non-admin gets 403 |
| R9.3 | The admin lists counselors | `GET /api/admin/counselors` returns each counselor with their active status and linked-student count |
| R9.4 | The admin deactivates and reactivates counselors (no hard delete) | `PATCH /api/admin/counselors/{id} {active:false}` invalidates the counselor's sessions and keeps their links, which grant no access while inactive. `{active:true}` restores access through the existing links |
| R9.5 | Pilot metrics | `GET /api/admin/pilot-metrics` returns total students, % with ≥3 reflections, % with intake completed and the verdict distribution. It counts active, non-`demo` STUDENT accounts, all-time by default, with optional `from`/`to` filters on signup date |
| R9.6 | The admin can read the counselor audit log | `GET /api/admin/audit?studentId=` returns that student's R8.6 rows, newest first |

### R10: Local dev environment & seed data

| ID | Requirement | Acceptance criterion |
|----|-------------|----------------------|
| R10.1 | The `dev` profile runs on H2 with Flyway migrations and seed data | `./mvnw spring-boot:run` starts with the schema and seed in place, plus demo accounts (1 admin, 1 counselor, 2 students, each student linked to that one counselor). Seed accounts are flagged `demo` so R9.5 excludes them |
| R10.2 | Seed career content is generated by an agent after the Career module is built, so it matches the real schema/endpoints | A seed migration with ≥20 careers covering ≥5 clusters, each with a 3–5 sentence summary, ≥3 pages, ≥1 media embed and ≥2 taxonomy tags, passes R3 validation |
| R10.3 | Seed migrations are idempotent / run once | Restarting the app doesn't duplicate any seed rows |
| R10.4 | The same migrations run on Postgres (the later `prod` profile) | Migrations use only SQL portable between H2 (PostgreSQL mode) and Postgres |

## Stretch

- **S1: Static guides (Phase 2 preview)**: read-only guide content such as the LinkedIn/networking guide, delivered as a Guide module. _Why stretch:_ it's cheap, read-only content, but the core loop doesn't need it.
- **S2: Password reset (manual)**: an admin endpoint to reset a user's password. _Why stretch:_ there's no email in the MVP, and it's only needed if pilot users get locked out.
- **S3: Student data export & account deletion**: a student downloads their data as JSON and can delete their account. _Why stretch:_ good privacy hygiene, but not required for a pilot.
- **S4: Student self-insight summary**: a student sees their own top tags and their YES/MAYBE/NO history. _Why stretch:_ useful for self-awareness, but secondary to the feed + reflection loop.
- **S5: Testcontainers Postgres test**: run the integration suite against real Postgres. _Why stretch:_ a safety net for the later EC2 move.

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
- **X14: Recomputing affinity when a career's tags change**: _Later phase:_ easier once semantic embeddings exist. For now, retagging a career doesn't change affinity that was already applied.
- **X15: Verdict-weighted affinity**: _Later phase:_ verdicts are stored (R6.8), so the weighting can be tuned once pilot data exists.
- **X16: Forced re-acceptance of new terms versions**: _Later phase:_ the MVP only records the accepted version (R1.12).
- **X17: Audit-log retention policy and student-visible access history**: _Later phase:_ decided together with the FERPA / district data-agreement review.
- **X18: Restoring an abandoned career on demand**: _Later phase:_ abandoned careers come back automatically once the unseen pool is used up (R4.8).

## Non-functional

### N1: Performance

| ID | Requirement | Acceptance criterion |
|----|-------------|----------------------|
| N1.1 | API latency | p95 < 300 ms for every endpoint, measured locally with seed data plus a synthetic load of 300 students × 20 reflections |
| N1.2 | Feed query latency | `GET /api/feed/next` p95 < 100 ms under the N1.1 dataset |

### N2: Security & privacy

| ID | Requirement | Acceptance criterion |
|----|-------------|----------------------|
| N2.1 | Session auth via Spring Security, with sessions stored server-side in Spring Session JDBC (portable H2/Postgres tables via Flyway) | Sessions survive an app restart. The cookie is HttpOnly, Secure (prod) and SameSite=Lax. The session ID rotates on login (prevents session fixation) |
| N2.2 | Session timeouts | Idle 30 min and absolute 12 h. Expired sessions return 401 |
| N2.3 | CSRF protection on state-changing requests | A POST/PUT/DELETE without a valid CSRF token returns 403 |
| N2.4 | CORS restricted to the configured frontend origin | A request from any other origin is blocked by CORS. The origin comes from config |
| N2.5 | Login rate limiting | The 6th failed attempt within 15 min for the same account or IP returns 429. Counters (this and R2.7) are in memory and reset on restart |
| N2.6 | Role- and ownership-based authorization on every endpoint | Tests cover: a student can't read another student's data, a counselor gets 403 on unlinked students, and non-admins get 403 on `/api/admin/**` |
| N2.7 | Error responses don't leak internals | No stack traces or SQL in any response body |
| N2.8 | Minors' data minimization | Only the fields in R1–R9 are stored. Birthdate is used only for the age gate and never persisted (R1.2). No third-party tracking |

### N3: Reliability

| ID | Requirement | Acceptance criterion |
|----|-------------|----------------------|
| N3.1 | Consistent error format | Every error is RFC 7807 `application/problem+json`. Validation errors list each failing field |
| N3.2 | No partial writes on multi-step operations | Reflection + affinity (R6.5) and intake + seeding (R7.4) are transactional, and tests force a failure to prove the rollback |
| N3.3 | Reflection history is never hard-deleted | Edits create new versions, and no endpoint deletes reflection history |
| N3.4 | Concurrent writes don't corrupt state | Reflections, affinity rows and the stored feed card use optimistic locking (`@Version`). A stale edit returns 409. A unique (student, career) constraint makes a double-submitted reflection create only one |

### N4: Maintainability & extensibility

| ID | Requirement | Acceptance criterion |
|----|-------------|----------------------|
| N4.1 | Modular monolith boundaries enforced | The `ApplicationModules.of(MvpApplication.class).verify()` test passes. Modules: auth/identity, student, counselor, career, feed, reflection, intake, admin, with later ones (guide, assignment) added without touching the existing internals |
| N4.2 | Cross-module communication only via public APIs or application events | The Modulith verify test catches any access to another module's `internal` package |
| N4.3 | Integration tests per module | Each module has ≥1 `@ApplicationModuleTest`. `./mvnw verify` is green |
| N4.4 | API contract published for the frontend | springdoc serves `/v3/api-docs` and Swagger UI in `dev`, and every endpoint has request/response schemas |
| N4.5 | List endpoints are paginated | Every list endpoint takes `page`/`size` (default 20, max 100). Counselor students sort by last active, newest first. Reflections sort by `updatedAt`, newest first |
| N4.6 | CI runs the build | A GitHub Actions workflow runs `./mvnw verify` on Java 25 for every push and PR |

### N5: Operability

| ID | Requirement | Acceptance criterion |
|----|-------------|----------------------|
| N5.1 | Health and metrics | `/actuator/health` is public. `/actuator/prometheus` and the other actuator endpoints require ADMIN (or aren't exposed in prod) |
| N5.2 | Structured logs with a request ID | Every log line is JSON with a `requestId`, and the request ID is returned in an `X-Request-Id` response header |
| N5.3 | Pilot metrics as gauges | The R9.5 values are exposed as Micrometer gauges on `/actuator/prometheus` |

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

_Skipped areas:_ accessibility and browser compatibility (backend only, so they belong to the Next.js frontend), usability (frontend concern; the API supports it through clear errors and `intakePromptDue`), i18n (the pilot is English only, X11), scalability beyond the pilot (a single instance covers 300 students; revisit in the EC2 phase).

## Open Questions

- None at this time.
