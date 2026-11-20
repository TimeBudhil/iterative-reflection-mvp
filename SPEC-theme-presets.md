# Iterative Reflection: Admin-Selectable Theme Presets (Update 2)

_Written 2026-11-20. Status: **ready-for-agent**. This is the second of three planned updates: (1) visual identity and quality of life (shipped), (2) admin-selectable theme presets, (3) engagement features. It covers both the backend (`mvp`, requirements B-R11) and the frontend (`iterative-reflection-frontend`, requirements R13). Both `REQUIREMENTS.md` files already list these requirements as new._

## Problem Statement

Update 1 gave the app one palette, Grove. It was designed carefully, but it's still a single fixed look chosen by the developer. A school that runs the app has no say: a conservative district may want something more neutral, and another may want a cooler tone. Grove also started from a palette close to a well-known fintech brand. Locking every school into one palette makes that kind of resemblance more costly.

The admin has no way to set anything app-wide. The backend is frozen, and it has nowhere to store a choice that every user sees. Browser storage would only change the admin's own browser, and an environment variable can't be changed from the admin portal.

The admin also has no record of what admins have done. The audit log only records counselors viewing student data. Counselor accounts being created, deactivated or reactivated, and now theme changes, leave no visible trace.

Three tickets from the original build (CI, the deploy rehearsal and the cross-role smoke suite) are marked done but were never run. Update 2 adds a new backend module and a public endpoint, which is exactly what those checks are meant to catch.

## Solution

The admin picks the app's look from four curated presets: **Grove** (the default, earthy and warm), **Harbor** (cool, steady), **Plum** (soft, creative) and **Slate** (neutral). Every preset fills the same color slots that update 1 introduced, so components never change. A preset is just data. Each preset passes WCAG AA contrast and a recorded brand look-alike check. Admins can't type in their own colors.

On `/admin/settings`, an "App theme" card shows the four presets as a radio group. Each option has swatches and a small mock feed card drawn in that preset's colors. When the admin saves, their own page switches right away, and the card shows "Last changed by <name> on <date>". Everyone else gets the new theme on their next full page load, including signed-out visitors on the sign-in page. Pages never flash the wrong theme while loading, and a theme problem never breaks the app or shows an error: it quietly falls back to Grove.

`/admin/audit` gains an "Admin activity" tab that lists theme changes and counselor create/deactivate/reactivate actions, newest first.

On the backend, a new `settings` module stores app-wide settings (only the preset id, never colors), and the `admin` module keeps the activity log. The three unverified tickets are verified in the same PR.

## User Stories

### Choosing a theme (admin)

1. As an admin, I want to choose the app's theme from a few curated presets, so that the app suits my school without me designing anything.
2. As an admin, I want each preset to show its name, swatches and a small mock feed card in its own colors, so that I can judge how it will look on real screens before saving.
3. As an admin, I want the mock card to show a primary button, YES/MAYBE/NO verdict chips and a progress bar, so that I see every kind of color the app uses.
4. As an admin, I want choosing a radio to change only the mock cards and not the whole page, so that browsing presets doesn't disorient me.
5. As an admin, I want the currently saved preset preselected when I open settings, so that I know what's active.
6. As an admin, I want the Save button disabled until I pick a different preset, so that I can't make an accidental no-op save.
7. As an admin, I want Save disabled while a save is in progress, so that I can't send it twice.
8. As an admin, I want a success toast and my own page to re-theme immediately after saving, so that I know it worked without reloading.
9. As an admin, I want a failed save to show an error toast and put the radio back on the saved preset, so that the card never claims a theme that isn't active.
10. As an admin, I want to see "Last changed by <name> on <date>", so that I know who changed the look and when.
11. As an admin, I want that line hidden when nobody has ever saved a theme, so that the card doesn't show an empty or misleading line.
12. As an admin, I want only contrast-checked presets on offer, so that I can't accidentally make text unreadable for students.
13. As an admin, I want presets that don't look like any well-known company's brand, so that the school's app isn't mistaken for something else.

### Seeing the theme (everyone)

14. As a student, I want the whole app in the school's chosen theme, so that it feels like one consistent product.
15. As a signed-out student, I want the sign-in page already in the chosen theme, so that the identity starts at the first screen I see.
16. As any user, I want `/terms` and `/change-password` in the theme too, so that no page looks out of place.
17. As any user, I want pages never to flash Grove before switching to the real theme, so that loading looks clean.
18. As any user, I want the app to keep working in Grove if the theme can't be loaded, so that a theme problem never blocks me.
19. As any user in a private window where browser storage is blocked, I want the app to still load normally, so that storage restrictions don't break anything.
20. As a student or counselor, I want no theme control in my settings, so that the shared identity stays consistent.
21. As a signed-in user, I want a theme change to apply on my next full page load, so that I'm not re-themed in the middle of something.

### Admin activity

22. As an admin, I want an "Admin activity" tab on the audit page, so that I can see what admins have changed.
23. As an admin, I want theme changes listed as "Theme: Grove → Harbor", so that I can see the before and after.
24. As an admin, I want counselor creation, deactivation and reactivation listed with the counselor's email, so that account changes are traceable.
25. As an admin, I want each row to show when (in my local time), which admin, the action and the details, so that the log reads at a glance.
26. As an admin, I want the log newest first and paginated, so that recent activity is on top and long logs stay fast.
27. As an admin, I want the selected tab kept in the URL, so that I can link to or reload the activity tab.
28. As an admin, I want a clear "No admin activity yet." message on an empty log, so that an empty table doesn't look broken.
29. As an admin, I want a PATCH that doesn't actually change a counselor's active state to log nothing, so that the log only records real changes.
30. As an admin, I want a failed log write never to undo the action I took, so that logging problems don't block counselor management or theme changes.
31. As an admin, I want the existing "Counselor views" audit tab unchanged, so that the counselor-view audit still works as before.

### Accessibility

32. As a keyboard user, I want to move between presets with the arrow keys and select with Space, so that I can choose a theme without a mouse.
33. As a screen-reader user, I want each preset announced by its name, so that I know which option I'm on.
34. As a screen-reader user, I want the swatches and mock cards hidden, so that fake feed content isn't read out as real.
35. As a screen-reader user, I want the save result announced while focus stays on Save, so that I know what happened without losing my place.
36. As a low-vision user, I want every preset to meet WCAG AA text contrast, so that I can read the app whichever theme is chosen.

### Developer and operations

37. As a developer, I want each preset to define every color slot, with a test that fails if one is missing, so that a preset can never half-apply.
38. As a developer, I want the contrast check to run for every preset, so that changing a preset can't silently break AA.
39. As a developer, I want the axe scan to run under each preset, so that real pages are checked in every theme.
40. As a developer, I want the backend to store only a preset id, so that colors stay in one place, next to their contrast tests.
41. As a developer, I want the settings module independent of every other feature module, so that future settings don't create tangled dependencies.
42. As a developer, I want a future setting to be a new small service, not a schema change, so that the module stays easy to extend.
43. As a developer, I want the new routes covered automatically by the route inventory test, so that their CSRF and role rules can't regress.
44. As a developer, I want the brand look-alike check recorded in the decisions log, so that "no brand look-alikes" has evidence behind it.
45. As a developer, I want CI, the deploy rehearsal and the cross-role smoke suite verified green in this PR, so that the new public endpoint and module are proven in a production-like setup.

## Implementation Decisions

### Presets (frontend)

- **Four presets, one slot contract.** `grove`, `harbor`, `plum` and `slate` each define all 15 slots from update 1: `background`, `surface`, `foreground`, `muted`, `muted-foreground`, `border`, `primary`, `primary-foreground`, `highlight`, `highlight-text`, `positive`, `positive-foreground`, `destructive`, `destructive-foreground`, `ring`.
- **Where the values live.** Grove stays the root default and is also addressable as `data-theme="grove"`. The other presets are `[data-theme="<id>"]` blocks in the global stylesheet, the only place raw color values may appear.
- **Personalities:**
  - **Harbor:** cool, steady (navy/teal family).
  - **Plum:** soft, creative (muted violet).
  - **Slate:** neutral, near-grayscale charcoal. The safe choice for conservative schools.
- **The rules stay the same for every preset:**
  - `highlight` is a fill only, never text.
  - Verdict colors come from the shared verdict mapping.
  - `destructive` stays a clear red.
- **The hex values are not decided yet.** Claude drafts a full slot set per preset that passes the contrast pairs. The developer picks the final values from the admin card's mock cards. This blocks the preset work, but not the backend or the theme plumbing.
- **Brand look-alike check:** recorded in the frontend decisions log before merge. For each preset it lists the `primary` + `highlight` pair, the nearest well-known brands with a similar pairing, and why the preset isn't a look-alike. Claude drafts it, and the developer signs it off.

### Applying the theme (frontend)

- **A theme query in the root providers** reads the public theme endpoint and sets `data-theme` on `<html>` on every page, signed in or out. It's fetched once per full page load: no refetch on window focus, and no polling.
- **No flash.** A small inline script in the document head reads the last theme id from `localStorage` and sets `data-theme` before first paint. When the server value arrives, it overwrites both the attribute and the stored hint. The stored value is only a hint, never a source of truth, and it holds nothing auth-related (N2.1 still holds). The CSP has no `script-src`, so an inline first-party script is allowed.
- **Fallbacks.** These all mean `grove`, with no toast, no error copy and no retry panel:
  - a failed request;
  - an unknown id (the stored hint is replaced too);
  - `localStorage` throwing.
- **Propagation.** Other users see a new theme only on their next full page load. An open tab keeps its theme until reloaded.

### App theme card (frontend)

- It plugs into the shared settings page as an admin-only card after Password, the same way the student's extra cards do.
- **Layout:** a native radio group. Each option shows the preset name (its accessible name), swatches and a mini mock feed card: a title, a primary Start button, YES/MAYBE/NO chips and a progress bar. The card has a scoped `data-theme`, so it draws in that preset while the page stays in the saved theme. Swatches and mock cards are `aria-hidden`.
- **Initial state:** comes from the admin theme endpoint, which also feeds the "Last changed" line.
- **Save:**
  - Disabled while the selection equals the saved preset, and while a save is pending.
  - On success: a toast, `<html data-theme>` and the stored hint update without a reload, and the theme queries are updated or invalidated.
  - On error: an error toast with the standard error copy, the radio reverts, and the page theme doesn't change. Focus stays on Save, and toasts use the existing announced toast region.
- **"Last changed by <display name> on <date>":** shown in local time, and hidden when `updatedAt` is null.

### Admin activity tab (frontend)

- `/admin/audit` becomes two tabs: "Counselor views" (the existing log, unchanged) and "Admin activity". The tab is kept in the URL (`?tab=activity`).
- **Table:** the existing reusable paginated table, 20 per page, newest first.
- **Columns:** When (local time), Admin, Action, Details.
- **Details copy:**
  - "Theme: Grove → Harbor" (preset ids mapped to display names);
  - "Created counselor <email>";
  - "Deactivated counselor <email>";
  - "Reactivated counselor <email>".
- **Empty state:** "No admin activity yet."

### Backend: `settings` module

- **What it is:** a new Spring Modulith module that stores app-wide settings and nothing else. It depends on no other feature module.
- **Storage:** one generic `app_settings` table (`key` as the primary key, `value_json`, `updated_by`, `updated_at` in UTC). It's created by a Flyway migration portable between H2 and Postgres. Persistence is internal to the module.
- **Typed access:** a public `ThemeSettings` service is the only way in. It validates the preset id against the allowed set (`grove`, `harbor`, `plum`, `slate`), reads the current value (default `grove`), and saves it. A future setting is a new small typed service over the same table.
- **Events:** a save that changes the value publishes `SettingChanged` (key, old value, new value, admin id, timestamp). A same-value save publishes nothing and leaves `updatedAt` unchanged.
- **Only the preset id is stored.** No colors are stored or returned.

### Backend: API contract

| Method & path | Auth | Request | Response |
|---|---|---|---|
| `GET /api/settings/theme` | public (GET only is `permitAll`) | none | `200 {presetId}`, `Cache-Control: no-cache`. `grove` when nothing is saved. Never includes who changed it or when |
| `PUT /api/admin/settings/theme` | ADMIN, CSRF | `{presetId}` | `200 {presetId, updatedAt, updatedByName}`. Unknown id: `400` problem+json with a field error. Non-admin: `403`. Same value: `200`, no event, no activity row |
| `GET /api/admin/settings/theme` | ADMIN | none | `200 {presetId, updatedAt, updatedByName}`, with `updatedAt`/`updatedByName` null when never saved |
| `GET /api/admin/activity` | ADMIN | `page`, `size` (N8.1) | Paginated `{createdAt, adminName, action, detail}`, newest first |

- The two admin GETs live in the `admin` module, which combines `ThemeSettings` with display names from `identity`. That keeps `settings` free of identity lookups.
- The PUT can live in either module, as long as `settings` doesn't depend on `admin` or `identity`. Its `updatedByName` comes from the same lookup.

### Backend: admin activity log (`admin` module)

- **Table:** an `admin_activity` table internal to `admin`: id, admin id, action, detail JSON, `created_at` UTC. The migration is portable. This is the `admin` module's first table of its own.
- **Actions:**
  - `THEME_CHANGED`: written by a listener on `SettingChanged`; the detail holds the old and new id.
  - `COUNSELOR_CREATED`, `COUNSELOR_DEACTIVATED`, `COUNSELOR_REACTIVATED`: written directly in the admin counselor flow, which already runs in `admin`, so no new event is needed and the counselor module is untouched. The detail holds the counselor's email. A PATCH that doesn't change `active` logs nothing.
- **Best-effort.** A failed activity write never fails or rolls back the action:
  - The theme row is written after commit.
  - The counselor rows are written so that an insert failure doesn't affect the counselor change.
  - Failures are logged at WARN with the request ID.
  - This deliberately differs from the same-transaction listener rule used by `ReflectionSaved`/`IntakeSaved`. Record it in the backend decisions log.
- **Separate from the counselor-view audit log** (R8.6): those columns (counselor, student, allowed/denied) don't fit an admin action.

### Carried-over verification

These tickets ship in the same PR, and the PR doesn't merge until they're green:
- **Ticket 01:** a real GitHub Actions run of the root CI (frontend lint, typecheck and test, plus the backend verify).
- **Ticket 05:** following the deploy README, `https://localhost` serves the app on Postgres with Flyway content, and nginx sends `/api/*` to Spring with the forwarded headers.
- **Ticket 22:** `pnpm e2e:smoke` passes on Chromium, Firefox and WebKit.

Afterwards, tick the ticket checkboxes and note anything that had to change in the decisions logs.

## Testing Decisions

- **What makes a good test:** it checks what a client or user can observe (status codes, bodies, later responses, visible text, roles, attributes on `<html>`, keyboard behavior), not internals. The token check stays the one deliberate exception on the frontend. The forced log-write failure is the one deliberate exception on the backend.
- **Backend seam 1, HTTP integration tests (the existing primary seam).** They extend the existing HTTP integration base, which boots the full app in `dev` and drives it over real HTTP.
  - **Public GET:** returns `grove` by default and includes the no-cache header. It works signed out, and its body has no admin name or time.
  - **Admin PUT:** saves and returns the new value. An unknown id gives 400 with a field error. A student or counselor gets 403. The same value returns 200 with `updatedAt` unchanged and no new activity row. The public GET reflects the save.
  - **Admin GET:** returns nulls before any save, and name/time after.
  - **Activity:**
    - A theme change shows up as `THEME_CHANGED` with old and new ids.
    - Counselor create, deactivate and reactivate each show up once.
    - A no-op PATCH adds nothing.
    - Rows are newest first and paginated.
  - **Best-effort failure:** a `@MockitoBean` replaces the admin module's activity writer with one that throws. A theme save still returns 200 and the public GET shows the new value. A counselor deactivation still takes effect. This follows the existing pattern of replacing the Clock/Random beans.
- **Backend seam 2, existing guards updated (not new seams):**
  - **Route inventory test:** its access table gains the four routes, so CSRF (PUT) and the role rules are enforced automatically.
  - **Modularity verification:** covers the new module and the "settings depends on no feature module" rule.
  - **One small `@ApplicationModuleTest` for `settings`** (N4.3 requires one per module): it asserts that `SettingChanged` is published on a change and not on a same-value save.
- **Frontend seam 1, Playwright e2e against the dev backend (the existing primary seam).** Extend the existing specs rather than adding files.
  - **Settings spec:**
    - The admin sees four presets with the saved one selected, and Save is disabled.
    - Selecting Harbor leaves `<html data-theme>` unchanged.
    - Save shows the toast, sets `data-theme="harbor"` and shows "Last changed by".
    - A failed save (forced by route interception) shows an error toast and reverts the radio.
    - Arrow keys and Space work.
    - Students and counselors have no theme card.
    - The test resets to `grove` afterwards.
  - **Admin audit spec:** the activity tab lists the theme change and a counselor creation, and `?tab=activity` survives a reload. The existing counselor-views assertions still pass.
  - **Sign-in/shell spec:**
    - After a save, a fresh signed-out context opens `/signin` with `data-theme="harbor"`.
    - No flash: with the theme request delayed, a reload already has the saved theme at DOMContentLoaded.
    - Fallbacks: an aborted request, an unknown id, and `localStorage` throwing (via an init script) all give `grove` with no error shown.
  - **Axe spec:** the existing scan also runs `/signin`, the feed and `/admin/settings` under each preset.
- **Frontend seam 2, the token check (existing).** Its preset list grows to all four. It already checks every AA pair, the MAYBE chip tint and the raw-color rule, and it fails if any preset is missing a slot. The Grove-values assertions stay.
- **Prior art:**
  - **Backend:** the admin audit, admin counselor management and pagination HTTP tests; the route inventory test; the test-seams test (bean replacement); the modularity test.
  - **Frontend:** the settings, admin audit, sign-in/shells and axe e2e specs; the update 1 token check.
- **CI:** the token check and the backend suite run in the existing CI jobs. E2E and smoke run locally, as today. Smoke is run once over HTTPS for the carried-over verification.

## Out of Scope

- **Per-user theme choice:** it would undo the shared identity.
- **Per-school or per-counselor themes:** one pilot, one admin, one theme.
- **Free-form theme colors:** they would allow unreadable or brand-copying palettes.
- **More than four presets:** adding one later is data plus a token-check entry.
- **Live theme push (websockets/SSE) and refetching:** next full load is enough.
- **A live whole-page preview:** the mock cards are the preview.
- **Dark mode:** stays a stretch item. Its future shape is a dark variant of each preset's slots.
- **Admin activity export or retention:** decided with the FERPA review, along with the counselor-view audit log's retention.
- **Logging other admin actions:** sign-ins and profile/password changes aren't logged.
- **Backfilling activity rows** for counselors created before update 2: a stretch item.
- **Storing colors in the backend.**
- **New fonts, logo artwork or illustrations.**
- **Any update 3 engagement feature.**

## Further Notes

- **Why update 2 exists:** the starting palette was close to a well-known fintech brand. Curated, contrast-tested presets let a school choose a look while the app keeps one identity, and they make the "no look-alikes" rule enforceable. Update 1's slot contract makes this cheap.
- **Open question that blocks the preset work:** the final hex values for Harbor, Plum and Slate. Earlier notes said drafts existed, but none do. Draft them first so the admin card can show them, then pick and record the brand check.
- **Keep the slot names stable.** Adding a slot later means every preset must define it.
- **After this ships:** in both `REQUIREMENTS.md` files, mark R13 / B-R11 as existing. Record the preset values, the brand check and the best-effort logging decision in the decisions logs. Tick the carried-over ticket checkboxes.
