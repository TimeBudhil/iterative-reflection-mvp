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
