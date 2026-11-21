<!--
AGENTS / AI ASSISTANTS: DO NOT READ THIS FILE.
It is a human-facing history of scoping decisions. Every decision here is already
reflected in REQUIREMENTS.md, which is the source of truth. Reading this file only
consumes extra tokens. Open it only if the user explicitly asks about it.
-->

# Decisions Log

_Last updated: 2026-11-18_

> **Note for agents:** do not read this file unless the user explicitly asks. REQUIREMENTS.md already reflects every decision below.

- **Multiple counselors per student:** allowed (R2.5). Seed data uses a single counselor.
- **Exploration ratio and affinity weights:** configurable properties (R4.4, R6.6).
- **Tag taxonomy:** drafted by the seed agent and approved by the project owner (R3.5).
- **FERPA / district data agreements:** assumed not required for now. Revisit before a real school pilot on EC2.
- **Success criteria lowered (2026-11-18):** ≥60% reflect on ≥3 careers (was ≥5), ≥20% complete intake (was ≥30%).
- **Abandon instead of forced reflection (R4.7, R4.8):** only after opening the experience. Applies a configurable penalty. Comes back after the unseen pool runs out. Doesn't count toward the intake trigger or pilot metric.
- **Feed card stored when served (R4.1):** stops refreshes from re-rolling the exploration slot.
- **Affinity is score-only and isn't recomputed when tags change:** both deferred (X14 semantic embeddings, X15 verdict weighting).
- **Counselors get full history, reads are audit-logged (R2.8, R8.6):** student-visible log and retention policy deferred (X17, X19).
- **Birthdate not stored (R1.13):** only `ageVerifiedAt`, for minors' data minimization.
- **Sessions in Spring Session JDBC (N2.1):** restarts don't log users out. Rate-limit counters stay in memory.

## Implementation decisions (2026-11-18, `spec/mvp` build)

_These came up while the spec was being built. They aren't in REQUIREMENTS.md yet._

- **Taxonomy approved with a trade-school cluster (R3.5):** 9 clusters including "Skilled Trades (Trade School)" (Electrician, Welder, Plumber), with Education Path kept. 31 tags, 27 careers. Every tag, clusters included, can be picked in the intake form.
- **Layered packages inside each module:** `internal.{domain, repository, services, integration, controller, dto}`, with dependencies flowing controller → services → repository → domain. Enforced by an ArchUnit test. Each module's root package stays its public API.
- **Thin career service kept:** `CareerService` only passes calls through. It's there because controllers may not touch repositories.
- **Editing or viewing the history of an unreflected career returns 409, not 404:** this follows from the "one career at a time" rule (R4.2). A student can act only on the in-progress card, plus careers they've already reflected on.
- **Abandon only applies to the in-progress card:** a second tap returns 409 instead of making the career final.
- **Re-abandon is final (assumption, still open in REQUIREMENTS.md):** abandoning a resurfaced career removes it for good and applies the penalty again (penalty × 2).
- **`GET /api/intake` added (beyond the spec):** returns the student's current intake so the frontend can pre-fill the edit form.
- **Intake threshold configurable (beyond the spec):** `intake.min-reflections`, default 3 (R7.1).
- **No default profile:** the app refuses to start without one. `./mvnw spring-boot:run` uses `dev` through the Maven plugin, so prod can't fall into dev with demo accounts.
- **Dev uses file-based H2 (`./data/`, git-ignored):** dev sessions survive a restart. Tests stay in memory.
- **Prod trusts forwarded headers (`FORWARD_HEADERS_STRATEGY`, default `native`):** the per-IP login limit sees the real client address. The proxy must send `X-Forwarded-For`.
- **Password change signs out the user's other sessions:** the current session is kept and rotated.
- **Demo accounts use password `demo`:** dev only, flagged `demo` and left out of pilot metrics.
- **Pilot-metrics endpoint is computed fresh, while the Prometheus gauges are cached:** `pilot.metrics.cache-ttl`, default 30 s.
- **Deny by default:** any new route is STUDENT-only until it's opened explicitly in `SecurityConfig`.
- **Seed content to check by hand:** the 27 bls.gov links were written from memory. Three videos are loose matches: Robotics Engineer, Marine Biologist and Small Business Owner.
- **Admin activity log is best-effort and written after commit (update 2):** `AdminActivity` swallows a failed insert and logs it at WARN with the request ID. Rows are written in their own transaction after the action commits (a `SettingChanged` AFTER_COMMIT listener for `THEME_CHANGED`; an after-commit hook in the admin counselor controller for the counselor actions). This deliberately differs from the same-transaction listener rule of `ReflectionSaved`/`IntakeSaved`: an audit row matters less than the admin action. The cost is a rare action with no row.
- **`admin` module's first table (update 2):** `admin_activity` (V20_1) is the first table owned by `admin`, which until now only orchestrated other modules. It stays in `admin.internal`; `settings` has no dependency on `admin`, so the theme row is written by a listener on `settings.SettingChanged`. `app_settings` (V19_1) is generic key/JSON storage owned by `settings`, with `"key"` quoted because it is reserved in H2.
