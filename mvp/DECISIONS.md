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
- **Success criteria lowered:** ≥3 reflections (was ≥5) and ≥20% intake (was ≥30%), measured by an admin metrics endpoint + Micrometer (R9.5, N5.3).
- **Abandoning careers:** allowed only after opening the experience, with a configurable affinity penalty. Abandoned careers return once the unseen pool is used up (R4.7, R4.8, R6.7). The served feed card is stored, so refreshing can't dodge it (R4.1).
- **No email, so passwords are handled in-app:** admin sets temporary counselor passwords with a forced change. Everyone can change their own password (R1.9, R1.10).
- **Minors' privacy:** birthdate discarded after the age gate, terms version recorded, counselor reads audit-logged (R1.2, R1.12, R8.6).
- **Deferred:** affinity recompute on retagging (semantic embeddings later), verdict-weighted affinity, terms re-acceptance, audit retention (X14–X17).
