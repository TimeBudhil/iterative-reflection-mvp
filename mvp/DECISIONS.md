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
