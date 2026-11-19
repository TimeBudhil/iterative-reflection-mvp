# Iterative Reflection MVP: Spec

_Written 2026-11-18. Source of truth for requirement IDs: `REQUIREMENTS.md`. IDs in brackets (e.g. [R4.7]) point there._

## Problem Statement

High-school students don't know which careers fit them, and their school counselors, at roughly one counselor per 328 students, can't give each of them individual guidance. A student has no structured, self-directed way to "try on" a career and check it against their own interests, values and lifestyle. A counselor can't see what a student has already discovered about themselves, so the first conversation starts from zero instead of from the student's real leanings.

## Solution

A backend REST API (consumed later by a separate Next.js frontend) that runs a self-directed career-discovery loop:

1. A student signs up (13+), optionally linking to their counselor with a short join code.
2. A personalized feed serves one career at a time. The student works through a short multimedia "experience" (ordered pages of text, video, audio, images and links), then reflects on it: 1–10 scales across fixed dimensions, optional open answers, and a YES / MAYBE / NO verdict on "Would I genuinely enjoy this?". If a career clearly isn't for them, they can abandon it after opening it.
3. Every reflection, abandon and the optional intake form tunes per-tag affinity scores, which rank the next careers, plus a periodic random exploration card.
4. Counselors who were linked by the student get a read-only dashboard of the student's progress, reflections, verdicts, top interests and intake answers. Every detail read is audit-logged.
5. A minimal admin creates and manages counselor accounts, and reads pilot success metrics and the audit log.

The phase runs locally on H2 with seed data and stays portable to Postgres on a single small EC2 instance later.

## User Stories

### Student: account

1. As a student, I want to sign up with my email, password, display name and birthdate, so that I can start exploring careers on my own.
2. As a student under 13, I want to be clearly refused at signup, so that the product doesn't collect my data.
3. As a student, I want my birthdate to be used only to verify my age and then discarded, so that the app stores as little about me as possible.
4. As a student, I want to accept the current terms at signup, so that my consent is on record.
5. As a student, I want to enter my counselor's join code during signup, so that I'm linked from the start.
6. As a student who mistyped a join code at signup, I want the whole signup to fail with a clear "code not found" error, so that I can fix it without ending up with an unlinked account.
7. As a student, I want my email to be treated case-insensitively, so that I can't accidentally create two accounts.
8. As a student, I want a minimum password length of 12, so that my account is reasonably safe.
9. As a student, I want to sign in and stay signed in through a secure cookie, so that I don't have to re-enter my password on every page.
10. As a student, I want a wrong password to give a generic error, so that nobody can learn which emails are registered.
11. As a student, I want repeated failed logins to be throttled, so that nobody can guess my password.
12. As a student, I want to sign out and have my session truly invalidated, so that a shared computer is safe.
13. As a student, I want my session to expire after 30 min idle or 12 h total, so that a forgotten session doesn't stay open.
14. As a student, I want my session to survive a server restart, so that a deploy doesn't kick me out mid-reflection.
15. As a student, I want to change my password while signed in, so that I can recover from a weak or leaked password.
16. As a student, I want to change my display name, so that I can pick a name I'm comfortable with counselors seeing.
17. As a student, I want a "me" endpoint that tells the frontend who I am and whether the intake prompt is due, so that the app shows the right screen.

### Student: counselor links

18. As a student, I want to link to a counselor after signup by entering their code, so that I can share my progress whenever I'm ready.
19. As a student, I want to link to more than one counselor, so that, for example, both my school counselor and a program mentor can see my journey.
20. As a student, I want to see the list of counselors I'm linked to, including whether each one is still active, so that I know who can see my data.
21. As a student, I want to revoke a counselor link, so that I control who sees my reflections.
22. As a student, I want linking the same counselor twice to be rejected, so that I don't create duplicates.
23. As a student, I want relinking after a revoke to restore the counselor's view, so that changing my mind is painless.

### Student: careers and the feed

24. As a student, I want the feed to give me exactly one career at a time, so that I focus on it instead of endlessly browsing.
25. As a student, I want the career I'm currently on to keep coming back until I finish or abandon it, so that refreshing doesn't lose my place.
26. As a student, I want each career card to show a title, an appealing 3–5 sentence summary, tags and an estimated duration of 5–10 min, so that I know what I'm getting into.
27. As a student, I want each career experience to be a short sequence of pages with text, video, audio, images and links, so that it feels immersive.
28. As a student, I want my page progress saved, so that I can resume where I left off.
29. As a student, I want my progress to stay valid even if the career content is edited, so that I never land on a page that no longer exists.
30. As a student, I want to abandon a career after opening it, so that I'm not forced to click through and rate something I already know I dislike.
31. As a student, I want abandoned careers to come back only after I've seen everything else, so that I get a second look without them crowding the feed.
32. As a student, I want careers I've reflected on to never reappear, so that the feed always moves forward.
33. As a student, I want the feed to rank careers by what my reflections suggest I like, so that each career is more relevant than the last.
34. As a student, I want a random career mixed in regularly (every 5th card by default), so that I discover things outside my current interests.
35. As a new student with no history, I want careers in a sensible default order, so that the first experience is good.
36. As a student who has gone through everything, I want a clear "you've seen it all" state, so that the frontend can celebrate rather than show an error.
37. As a student, I want retired careers to disappear from my feed, so that I never see outdated content.

### Student: reflection

38. As a student, I want to reflect only after I've seen the whole experience, so that my reflection is informed.
39. As a student, I want to rate the career 1–10 on interest, values alignment, lifestyle fit, everyday responsibilities and sacrifices/trade-offs, so that I think about it from several angles.
40. As a student, I want optional open-ended prompts next to each scale, so that I can capture my thinking in words.
41. As a student, I want a required final YES / MAYBE / NO verdict, so that I narrow down careers explicitly.
42. As a student, I want clear per-field validation errors, so that I know exactly what to fix.
43. As a student, I want to edit a reflection later, so that my answers can change as I learn more.
44. As a student, I want earlier versions of my reflection kept with timestamps, so that my change of mind is part of my story.
45. As a student, I want a list of all my reflections, newest first and paginated, so that I can review what I've thought so far.
46. As a student, I want a double-tapped submit to count once, so that the feed doesn't over-weight one career.

### Student: intake

47. As a student, I want to be offered a short intake form after my 3rd reflection, so that I've had a taste of the app before being asked about myself.
48. As a student, I want to pick interests from the same tag taxonomy used for careers, so that my answers directly improve my feed.
49. As a student, I want to answer questions about favorite subjects, goals, why I'm using the app and what I expect from it, so that my counselor understands my context.
50. As a student, I want to dismiss the intake prompt, so that I'm not nagged, while still being able to fill it in later.
51. As a student, I want to edit my intake answers, so that my feed reflects my current interests without double counting.

### Counselor

52. As a counselor, I want an account created for me by an admin with a temporary password, so that I can get started without email infrastructure.
53. As a counselor, I want to be forced to set my own password on first sign-in, so that the admin doesn't know my credentials.
54. As a counselor, I want to see my join code, so that I can hand it to students.
55. As a counselor, I want to rotate my join code if it leaks, without breaking existing links, so that only the students I intend can link.
56. As a counselor, I want a paginated list of my linked students, showing careers seen, reflected and abandoned, verdict counts, intake status and last activity, sorted by most recently active, so that I can triage who to talk to.
57. As a counselor, I want a student detail view with every reflection (scores, open answers, verdict), abandoned careers with how far they got, top 5 tag affinities and intake answers, so that I can prepare a meaningful conversation.
58. As a counselor, I want to see a student's full history even from before they linked to me, so that I get the whole picture.
59. As a counselor, I want to be blocked from students who haven't linked to me or have revoked the link, so that student consent is respected.
60. As a counselor, I want the dashboard to be strictly read-only, so that I can't accidentally change a student's data.

### Admin

61. As an operator, I want the first admin created from environment variables on startup, exactly once, so that a fresh install is usable.
62. As an operator, I want startup to fail if the bootstrap admin password is weak, so that a misconfiguration is caught immediately.
63. As an admin, I want to create counselor accounts with a temporary password, so that I can onboard a school.
64. As an admin, I want a paginated list of counselors with their join code, active flag and number of linked students, so that I can manage them.
65. As an admin, I want to deactivate a counselor who left, so that they immediately lose sign-in and access, while their links are kept for history.
66. As an admin, I want to reactivate a counselor, so that a mistaken deactivation is fully reversible.
67. As an admin, I want a pilot-metrics endpoint (student count, % with ≥3 reflections, % with intake, verdict distribution), optionally filtered by signup date and excluding demo accounts, so that I can judge pilot success.
68. As an operator, I want the same pilot metrics as Prometheus gauges, so that I can graph them over time.
69. As an admin, I want to read the audit log of counselor views per student, so that I can answer "who looked at this student's data?".

### Developer and operator

70. As a developer, I want `./mvnw spring-boot:run` to start with schema, seed careers and demo accounts on H2, so that the frontend team can work against a realistic API.
71. As a developer, I want seed migrations to run once, so that restarts don't duplicate data.
72. As a developer, I want migrations that also run on Postgres, so that moving to EC2 needs no rewrite.
73. As a frontend developer, I want an OpenAPI document and Swagger UI in dev, so that I can generate a client and explore the API.
74. As a frontend developer, I want every error in RFC 7807 problem+json with per-field validation details, so that I can render errors uniformly.
75. As a frontend developer, I want consistent pagination (`page`/`size`, default 20, max 100) on every list, so that I can reuse one list component.
76. As an operator, I want JSON logs with a request ID echoed in `X-Request-Id`, so that I can trace a user's complaint.
77. As an operator, I want public health but admin-only metrics, so that internals aren't exposed.
78. As an operator, I want the app to run within 512 MB of heap, so that it fits a small cheap instance.
79. As a developer, I want module boundaries enforced by a test and CI to run the full build on every push and PR, so that the monolith stays modular.

## Implementation Decisions

### Modules (Spring Modulith)

Each module exposes a small public API (a service interface plus DTOs/events). Everything else lives in its `internal` package. Cross-module calls go only through those APIs or application events [N4.1, N4.2].

- **identity**: users, roles, password hashing and policy, signup, sign-in/out, sessions, `me`, password change, display-name change, the `mustChangePassword` gate, terms record, age check, login rate limiting, bootstrap admin. Public API: look up a user by id, create a counselor user, set a user active/inactive (which invalidates sessions), and the current-user principal.
- **counselor**: counselor profile, join codes (generation without ambiguous characters, rotation), student↔counselor links (create, revoke, list), link-attempt rate limiting, the read-only dashboard, and the audit log of detail reads. Public API: "is counselor C allowed to see student S?", resolve a join code, link a student with a code (used by identity during signup, in the same transaction).
- **career**: read-only catalog of careers, pages, content blocks and the tag taxonomy, the `active` flag, the embed-host allowlist validation, and per-student page progress (with clamping). Public API: career summary and tags, last page number, a student's `lastViewedPage`, and active careers in default order.
- **feed**: the stored in-progress card per student, ranking, the exploration slot, abandon, abandoned-career resurfacing, the exhausted state, and **tag affinity** (affinity lives here because ranking is its only consumer). Public API: `next(student)`, `abandon(student, career)`, top-N affinities for a student.
- **reflection**: versioned global question set, one reflection per (student, career) with version history, validation, unlock check (last page viewed). Publishes a "reflection saved" event carrying the student, career, new scale mean and reflection id.
- **intake**: DB-defined intake questions, submission, edit, dismissal, `intakePromptDue` computation. Publishes an "intake saved" event carrying the selected interest tags.
- **admin**: admin-only endpoints for counselors, pilot metrics and the audit log, plus the Micrometer gauges. It owns no tables of its own except what pilot metrics need to read through other modules' public APIs.

### Affinity model

- Affinity is stored as **contributions**, not a running total: one row per (student, tag, source type, source id) with a delta, where the source type is REFLECTION, INTAKE or ABANDON. A tag's score is the sum of its contributions.
- Replacing a reflection's or the intake's contribution [R6.3, R7.4] is "delete that source's rows, insert new ones". This makes "no double counting" structural rather than arithmetic.
- Reflection delta = (mean of scale answers − `affinity.neutral-score`) / `affinity.scale` [R6.2]. Intake = `affinity.intake-boost` per selected interest tag. Abandon = `affinity.abandon-penalty` (default −0.5) per career tag [R6.7]. All of these are configuration properties [R6.6].
- The verdict doesn't affect affinity (X15). Tag changes on a career don't trigger recomputation (R6.8, X14).
- **Transactions:** the feed module applies affinity in a **synchronous, same-transaction** event listener for "reflection saved" and "intake saved", so a failure rolls back the reflection or intake too [R6.5, N3.2]. Async after-commit Modulith listeners must **not** be used for these.

### Feed behaviour

- `next(student)`:
  1. If a stored in-progress card exists, and its career is still active and not reflected on, return it.
  2. Otherwise pick a candidate pool: unseen active careers, or, when none are left, active careers abandoned once [R4.8].
  3. Pick a card. When (reflections + abandons) mod `feed.exploration-every` equals the exploration position, pick at random (from an injectable, seedable random source). Otherwise pick the highest summed tag affinity, with ties broken by catalog default order [R4.4, R4.5].
  4. Store the picked card as in progress. If the pool is empty, return `{career: null, exhausted: true}`.
- Starting or requesting a different career while one is in progress returns 409 [R4.2].
- Abandon requires `lastViewedPage ≥ 1` (otherwise 409), records the abandon with its `lastViewedPage`, applies the penalty and clears the in-progress card.
- **Re-abandon (assumed; still listed as an open question in REQUIREMENTS.md):** abandoning a career that resurfaced is final. The career leaves the pool for good and the penalty applies again, so the feed always reaches `exhausted`.

### Schema (logical)

- **users:** id, email (stored lower-cased, unique), password hash, display name, role, active, must-change-password, demo flag, age-verified-at, accepted terms version and accepted-at, created-at, optimistic version. No birthdate column [R1.13].
- **counselor_profiles:** user id and join code (unique, 6–8 chars from an unambiguous alphabet).
- **counselor_links:** student, counselor, linked-at, revoked-at. A relink reactivates the existing row, and only one active row per pair is allowed.
- **audit_log:** counselor, student, outcome (allowed/denied), at.
- **careers:** title, summary, estimated minutes, default order, active.
- **career_pages** (ordered) and **content_blocks** (ordered, typed, URL validated against the allowlist).
- **tag_categories, tags and career_tags.**
- **page_progress:** student, career, last viewed page, updated-at.
- **feed_state:** student and current career, served-at.
- **abandons:** student, career, last viewed page, abandon count, at.
- **reflection_questions:** key, version, prompt text, open prompt, order, active.
- **reflections:** student, career (unique pair), current version number, optimistic version.
- **reflection_versions:** reflection, version number, verdict, created-at, with **reflection_answers** pointing to the question version, an integer score and optional text.
- **intake_questions, intake_responses and intake_dismissals.**
- **affinity_contributions:** student, tag, source type, source id, delta.
- **Spring Session JDBC tables.**
- All timestamps are UTC `TIMESTAMP WITH TIME ZONE` [N7.2]. All DDL and seed SQL must run on both H2 (PostgreSQL mode) and Postgres [R10.4].
- Content is read-only via the API and arrives only through Flyway migrations [R3.6]. Career seed content (≥20 careers, ≥5 clusters, plus the taxonomy) is generated after the career module exists, and the taxonomy is approved by the project owner before that migration is committed [R3.5, R10.2].

### API contract (summary)

- **Auth:** `POST /api/auth/signup`, `POST /api/auth/signin`, `POST /api/auth/signout`.
- **Me:** `GET /api/me`, `PATCH /api/me` (display name only), `PUT /api/me/password`.
- **Links:** `GET|POST /api/students/me/counselors`, `DELETE /api/students/me/counselors/{id}`.
- **Careers:** `GET /api/careers/{id}/experience`, `POST /api/careers/{id}/pages/{n}/viewed`, `POST /api/careers/{id}/abandon`.
- **Feed:** `GET /api/feed/next`.
- **Reflections:** `GET /api/reflection-questions`, a submit/edit endpoint per career, `GET /api/students/me/reflections`, and version history per reflection.
- **Intake:** `GET /api/intake/questions`, `POST /api/intake`, `PUT /api/intake`, `POST /api/intake/dismiss`.
- **Counselor:** `GET /api/counselor/me`, `POST /api/counselor/me/join-code/rotate`, `GET /api/counselor/students`, `GET /api/counselor/students/{id}`.
- **Admin:** `POST|GET /api/admin/counselors`, `PATCH /api/admin/counselors/{id}`, `GET /api/admin/pilot-metrics`, `GET /api/admin/audit`.
- **Conventions:**
  - JSON bodies; errors as RFC 7807 problem+json with per-field errors and no internals [N3.1, N2.7].
  - Every list is pageable with `page`/`size` (default 20, max 100, clamped) and a defined default sort [N8].
  - Stale optimistic-lock writes return 409 [N3.4].
  - OpenAPI via springdoc in dev [N4.4].

### Security

- Spring Security with sessions in Spring Session JDBC. The cookie is HttpOnly, SameSite=Lax and Secure in prod. Session IDs rotate on sign-in and on password change. Timeouts are 30 min idle and 12 h absolute [N2.1, N2.2].
- CSRF is on for state-changing requests. The frontend obtains the token through a cookie-to-header pattern [N2.3].
- CORS allows only the configured frontend origin [N2.4].
- Role rules: `/api/admin/**` is ADMIN only. `/api/counselor/**` is COUNSELOR only and GET-only, except the explicit allowlist (join-code rotation and the user's own password change). Student routes check ownership [N2.6, R8.4].
- The `mustChangePassword` gate is a filter that allows only `GET /api/me`, `PUT /api/me/password` and sign-out [R1.10].
- Rate limiting: in-memory, fixed 15-minute windows using an injectable clock. Login: the 6th failure per account or IP returns 429. Linking: the 11th failure per student returns 429 [N2.5, R2.6].
- Deactivating a user deletes their sessions from the session store. Deactivated users get the generic 401 at sign-in.
- Actuator: health is public. Prometheus and the other endpoints are ADMIN-only or not exposed in prod [N5.1].

### Configuration

- **Properties:** `feed.exploration-every` (5), `affinity.neutral-score` (5.5), `affinity.scale` (4.5), `affinity.intake-boost` (0.5), `affinity.abandon-penalty` (−0.5).
- **Environment:** frontend origin, `ADMIN_EMAIL`/`ADMIN_PASSWORD`, and Postgres connection settings in the `prod` profile [N6.3].
- **Profiles:** `dev` runs on H2 with seed data and demo accounts flagged `demo` [R10.1].

## Testing Decisions

- **What a good test is:** it exercises external behaviour through the highest seam and asserts on what a client can observe (status codes, response bodies, and what later requests return), not on repository calls or internal method interactions. Rewriting a module's internals shouldn't break its tests.
- **Primary seam: the HTTP API.** Integration tests boot the full context on H2 with Flyway, sign in as real seeded or test-created users with real session cookies and CSRF tokens, and drive the REST endpoints. Nearly every acceptance criterion in `REQUIREMENTS.md` is written at this level and is tested here, including:
  - the feed sequence and exploration slot
  - 409, 403 and 429 rules
  - pagination and sort orders
  - audit rows, checked through the admin endpoint
  - pilot metrics
  - deactivation effects
  - session expiry
  - "a session survives restart" (by re-reading the session store)
- **Seams required by the requirements:**
  - The `ApplicationModules.verify()` boundary test [N4.1, N4.2].
  - At least one `@ApplicationModuleTest` per module [N4.3].
  - A pure unit test of the affinity formula, including tests with overridden properties [R6.2, R6.6].
- **Injected test seams:**
  - A replaceable `Clock` bean, which drives session timeouts and rate-limit windows without sleeping.
  - A seedable random source for the exploration slot [R4.4].

  Both are ordinary beans overridden in tests, not test-only hooks in production code.
- **Rollback tests:** inside the HTTP-level tests, a module's public bean is replaced with one that throws (e.g. the affinity applier) to prove the reflection, intake or abandon is not saved [R6.5, N3.2].
- **Security tests:**
  - An ownership matrix: a student can't read another student, a counselor gets 403 on unlinked or revoked students, and non-admins get 403 on admin routes [N2.6].
  - A route-inventory test asserting that every counselor route is GET except the allowlist [R8.4].
  - A response-body check for the absence of stack traces and SQL [N2.7].
- **Concurrency:** two concurrent reflection submits for the same career result in one reflection and one affinity contribution [N3.4].
- **Seed and migration:** a test asserts the seed passes R3 validation (summary length, duration, ≥3 pages, ≥1 allowlisted embed, ≥2 taxonomy tags) and that restarting doesn't duplicate rows [R10.2, R10.3]. Postgres runs via Testcontainers only if S5 is picked up.
- **Performance:** a separate, non-CI check seeds 300 students × 20 reflections and verifies p95 < 300 ms overall and < 100 ms for the feed, with `-Xmx512m` [N1, N6.1].
- **Prior art:** none beyond the scaffold's context-load test. The first module's HTTP-level test becomes the pattern for the rest.
- **CI:** a GitHub Actions workflow runs `./mvnw verify` on Java 25 for every push and PR [N4.5].

## Out of Scope

Everything listed in `REQUIREMENTS.md` under Out-of-Scope (X1–X19) and Stretch (S1–S5) is out of scope for this spec. That includes:
- Phase 2 and Phase 3 assignments and workflows.
- Any email (verification, reset, notifications) and email change.
- An admin content-management UI.
- Media uploads.
- AI or ML recommendations, semantic-embedding re-tagging, and verdict-weighted affinity.
- i18n, a native mobile app, payments, the frontend itself and the EC2/Postgres deployment.
- Forced terms re-acceptance, an audit-retention policy and a student-visible access log.
- Stretch items: static guides, admin password reset, data export and account deletion, a student self-insight summary, and Testcontainers.

## Further Notes

- **Assumption to confirm:** re-abandon finality (see Feed behaviour). If the project owner decides otherwise, only the feed module's candidate-pool rule changes.
- **FERPA / district data agreements** are assumed not required for this local phase. Revisit them before a real school pilot. Audit-log retention (X17) depends on that review.
- **Suggested build order**, so each step has a demoable HTTP-level test:
  1. identity
  2. counselor links
  3. career (schema plus a small hand-written fixture)
  4. feed without affinity
  5. reflection plus affinity
  6. abandon
  7. intake
  8. counselor dashboard plus audit
  9. admin plus metrics
  10. seed-content generation
  11. performance check
