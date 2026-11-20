# Iterative Reflection Frontend: Visual Identity & Quality of Life (Update 1)

_Written 2026-11-20. This is the first of three planned updates: (1) visual identity and quality of life, (2) admin-selectable theme presets, (3) engagement features. Updates 2 and 3 get their own specs. `REQUIREMENTS.md` describes the app as it currently is; update it once this spec ships, not before._

## Problem Statement

The app works, but it has no visual identity. Every screen is stock shadcn grayscale, so it reads as a generic admin template rather than a place a high-school student wants to explore careers. The few colors that do appear are Tailwind defaults chosen ad hoc: green, amber and red verdict badges, and emerald and red admin badges. A NO verdict shows in red, which makes an honest answer look like a mistake.

Students also get little sense of progress. The experience viewer shows only "Page n of m" as text. Nothing tells a student how close they are to unlocking the interests form (after the 3rd reflection) or how many careers they've explored. The 1–10 reflection scale is a row of identical buttons, so it gives no visual sense of "how much". My Journey shows verdict counts as numbers only.

## Solution

Give the app a calm, minimal identity built on a small set of named color slots. The default palette is called **Grove**. Its personality is **curious, supportive, grounded**: deep forest green as the primary color, warm orange as the one highlight, and a warm off-white page with white panels. Color is an accent, never the main event, and no palette may resemble a specific company's brand.

On top of that, add four small quality-of-life improvements that use data the app already has:

1. A progress bar in the career experience.
2. "n of 3 reflections" progress toward unlocking the interests form, shown on the feed.
3. A filled-range look for the 1–10 reflection scale.
4. A proportional YES / MAYBE / NO bar on My Journey.

The feed also shows "n careers explored".

The only changes are visual and client-side. No backend changes, no new endpoints, no new fonts.

## User Stories

### Identity and look

1. As a student, I want the app to have a warm, consistent look, so that it feels made for me and not like a school admin tool.
2. As a student, I want color used sparingly, so that the screen feels calm and I can focus on the career content.
3. As a student, I want the one important action on a screen to stand out, so that I always know what to do next.
4. As a student, I want pages separated by soft background tones instead of boxes and borders, so that the interface feels light.
5. As any user, I want to see the app's name at the top of the navigation and on the sign-in page, so that I know where I am.
6. As any user, I want the active navigation item clearly marked in the app's primary color, so that I know which section I'm in.
7. As any user, I want buttons, tabs, tables, inputs, dialogs and menus to share one look, so that the app feels like one product.
8. As a counselor, I want my portal to use the same identity but with fewer accents, so that the student tables stay easy to scan.
9. As an admin, I want the metrics chart and badges to use the app's palette, so that the admin portal looks like part of the same product.
10. As a student on a 1024 px or 768 px screen, I want the restyled sidebar to still collapse into the top bar, so that nothing breaks on smaller laptops.

### Verdicts

11. As a student, I want my NO verdicts shown in a neutral color, not red, so that saying "no" to a career doesn't feel like failing.
12. As a student, I want YES and MAYBE in distinct, friendly colors, so that I can tell my saved careers apart at a glance.
13. As a student with color-vision differences, I want every verdict to show its word too, so that I never have to rely on color.
14. As a counselor, I want verdicts in the student detail to use the same colors and labels as the student sees, so that we're talking about the same thing.
15. As an admin, I want the YES / MAYBE / NO distribution chart to use the same verdict colors, so that it reads consistently with the student portal.

### Status badges

16. As an admin, I want Active/Inactive counselor badges and Allowed/Denied audit badges in the app palette, so that they fit the identity. Denied stays clearly marked as a problem.

### Progress

17. As a student reading a career, I want a progress bar showing how far through the pages I am, so that I know how much is left before I can reflect.
18. As a student reading a career, I want the "Page n of m" text kept next to the bar, so that the exact position is still clear.
19. As a student in review mode, I want the progress bar to show the page I'm on, so that free navigation still tells me where I am.
20. As a new student, I want to see "n of 3 reflections until your interests form unlocks" on the feed, so that I know reflecting leads to something.
21. As a student who has reflected 3 or more times, I want that progress line to disappear, so that the feed doesn't show finished goals.
22. As a student, I want the feed to show how many careers I've explored, so that I feel my exploration adding up.
23. As a student with no reflections yet, I want the explored count to read sensibly ("0 careers explored" or hidden), so that the feed doesn't feel discouraging. See Implementation Decisions.

### Reflection scale

24. As a student answering a 1–10 question, I want choosing a number to fill every segment up to it, so that I can see how strongly I'm answering.
25. As a keyboard user, I want arrow keys to move along the scale and the fill to follow, so that the scale works without a mouse.
26. As a student editing a reflection, I want my saved answers to show as filled scales when the form loads, so that I can see what I said before.
27. As a student, I want the YES / MAYBE / NO verdict choice to keep its single-selection look with verdict colors, so that it's clearly a different kind of answer from the scales.

### My Journey

28. As a student, I want a bar split into YES / MAYBE / NO by proportion, with each count written on it or beside it, so that I can see at a glance where I'm leaning.
29. As a student with only one kind of verdict, I want the bar to show a single segment cleanly, so that it doesn't look broken.
30. As a student with no reflections, I want no bar, just the existing empty state, so that I'm not shown an empty chart.

### Accessibility and quality

31. As a low-vision user, I want all text in the new palette to meet WCAG AA contrast, so that I can read everything.
32. As a screen-reader user, I want progress bars announced with their values ("Page 2 of 3", "1 of 3 reflections"), so that the progress isn't visual-only.
33. As a developer, I want components to use only named color slots, so that update 2 can swap the whole palette without touching components.
34. As a developer, I want a test that fails when someone adds a raw color value to a component, so that the slot rule doesn't decay.

## Implementation Decisions

### Color slots (the contract for updates 1 and 2)

- **Named slots.** The theme layer defines these slots, mapped onto the existing shadcn CSS variables so the shadcn primitives keep working: `background`, `surface`, `foreground`, `muted`, `muted-foreground`, `border`, `primary`, `primary-foreground`, `highlight`, `highlight-text`, `positive`, `positive-foreground`, `destructive`, `ring`.
- **Grove palette values** (the default; the other presets come in update 2):

  | Slot | Value | Use |
  |---|---|---|
  | `background` | `#edefeb` | page background |
  | `surface` | `#ffffff` | panels, cards, inputs, dialogs |
  | `foreground` | `#121511` | body text |
  | `primary` | `#163300` | primary buttons, active nav, wordmark, selected scale segments |
  | `primary-foreground` | `#ffffff` | text on primary |
  | `highlight` | `#f08952` | fills only: progress bars, current step. Never used as text |
  | `highlight-text` | `#a25a0e` | orange-family text and links |
  | `positive` | `#D6E4E8` | YES chips, Active badges |
  | `positive-foreground` | `#163300` | text on positive |
  | `muted` / `muted-foreground` | neutral grays derived from the off-white family | NO verdict, secondary text |
  | `destructive` | unchanged red | destructive actions and Denied badges only |

- **Usage rules:**
  - Every screen is `background` with `surface` panels on top.
  - Cards drop their default border and shadow.
  - Each screen has at most one `primary`-filled button. Secondary actions use outline or ghost variants.
  - `highlight` appears only as a fill and is always paired with text, because `#f08952` on white is about 2.5:1 and below the 3:1 WCAG 1.4.11 bar for graphics that carry meaning on their own.
- **Verdict colors:** YES → `positive`, MAYBE → `highlight` tint with `highlight-text` text, NO → `muted`. Red is reserved for destructive actions. A single shared verdict-style mapping is used by My Journey, the counselor student detail and the admin metrics chart, which today each hard-code their own Tailwind colors.
- **Status badges:**
  - Counselor Active uses `positive`. Inactive uses `muted`.
  - Audit Allowed uses `positive`. Denied uses `destructive`.
- **Dark mode:** none in this update. Dark mode stays a stretch item. Its future shape is a dark variant of the slots, with no component changes.

### Typography and wordmark

- **Font:** Geist only, self-hosted as today. Headings get their hierarchy from weight and size, with no new font.
- **Wordmark:** the text "Iterative Reflection" in semibold Geist in `primary`, at the top of the sidebar/top bar and above the sign-in form. No logo image.

### Restyled primitives and shell

- **Primitives:** the shared UI primitives (button, card, tabs, table, input, label, dialog, dropdown menu, skeleton, toast) are restyled against the slots. Their props and variants stay the same, so call sites don't change.
- **Portal shell:** the sidebar, the top bar below 1280 px and the user menu use `surface` with the active item in `primary`. The collapse behavior and the "Menu" button stay as they are.
- **Raw colors:** every hard-coded Tailwind palette color in pages and components is replaced by slot-based classes or the shared verdict mapping.

### Quality-of-life features

- **Experience progress bar:** the viewer's page indicator becomes a bar filled to page / lastPage in `highlight`, with the existing "Page n of m" text kept next to it. It works the same in in-progress and review mode. The page-jump buttons in review mode stay as they are. The fraction comes from the existing pure experience view-state model, extended with a progress value.
- **Intake unlock progress (feed):**
  - **Where:** on the feed page, under the card.
  - **Content:** "n of 3 reflections until your interests form unlocks" plus a bar.
  - **Data:** n comes from the existing student reflections query (the one My Journey and review mode already share). The threshold of 3 is a frontend constant that mirrors the backend rule.
  - **When it hides:** at n ≥ 3. It also never shows while the reflections query is loading or has failed, so it never flashes wrong numbers.
- **Explored count (feed):** "n careers explored", where n is the number of reflections. Abandoned careers don't count, because students can't see their abandons. The line is hidden while n is 0 (the intake progress line already covers a new student) and while loading. There's no "of N" total: no endpoint exposes the career count, and adding one is deferred to update 3.
- **Filled 1–10 scale:**
  - **What changes:** the shared segmented control gets a "range" display mode. Selecting n styles segments 1..n as filled (`primary`) and the rest as empty.
  - **What stays:** it's still built on native radio inputs, so Tab, arrow keys, Space and the form state work as they do today.
  - **Verdict control:** keeps the single-selection mode with verdict colors.
- **Journey verdict bar:**
  - **Layout:** the stat strip gains one horizontal bar split into YES / MAYBE / NO, with widths proportional to the counts and in the verdict colors. Each count shows as text.
  - **Edge cases:** zero-count segments aren't rendered, and the whole bar is hidden with 0 reflections.
  - **Data:** proportions come from the existing pure journey model, extended with a distribution function.
- **Accessibility of bars:** progress bars use the `progressbar` role with current/max values and a text label. The verdict bar is labelled with its counts. Decorative fills are hidden from assistive tech where text already says the same thing.

### Not changing

- No backend changes and no new API calls. All counts come from queries the app already makes.
- No routes, copy or behavior changes beyond what's listed here. Existing tests that rely on visible text keep passing because the texts stay the same.

## Testing Decisions

- **What makes a good test:** it checks what a user can see or do (text, roles, values, keyboard behavior), not class names or component internals. The token check below is the one exception: it's a deliberate guard on the color-slot rule.
- **Seam 1, Playwright e2e (main seam):** extend the existing specs against the dev backend instead of adding new files where possible.
  - **Feed card spec:** a fresh student sees "0 of 3 reflections…" and no explored count. After reflecting, they see "1 of 3" and "1 career explored". After the 3rd reflection the intake progress line is gone.
  - **Experience viewer spec:** the progress bar reports the current page and the total, on both Next and Previous, and in review mode.
  - **Submit/edit reflection specs:** choosing a scale value marks it as selected, arrow keys move the selection, and an edited reflection loads with its saved values selected.
  - **My Journey spec:** the verdict bar's labelled counts match the stat strip, and with 0 reflections there's no bar.
  - **Sign-in/shell spec:** the wordmark is visible on sign-in and in each portal shell.
  - **Existing axe scan:** it already runs on every page, so it now enforces color contrast for the Grove palette with no new test.
- **Seam 2, Vitest on pure models:** extend the existing model tests:
  - the experience view-state progress value
  - the journey distribution (proportions, zero-count segments, empty state)
  - a small feed-progress helper (intake progress and explored count, including hidden states)
- **Seam 3, one new token check (Vitest):** this is the only new seam. It reads the slot values from the global stylesheet and:
  - asserts the WCAG AA text contrast pairs for Grove: `foreground` on `background` and on `surface`, `primary-foreground` on `primary`, `highlight-text` on `background` and on `surface`, and `positive-foreground` on `positive`, all ≥ 4.5:1. `highlight-text` on `background` is about 4.53:1, so this is the pair most likely to break if the background is darkened.
  - fails if any component or page source contains a raw color value: a hex value, an `rgb(` or `oklch(` value, or a Tailwind palette color class such as `bg-green-100` or `text-red-900`.
  - is the check that update 2 extends to every preset.
- **Prior art:**
  - the existing `model.test.ts` files beside the experience, feed and journey models
  - the axe-based a11y e2e spec
  - the feed card, experience viewer, submit/edit reflection and My Journey e2e specs
- **CI:** the token check and the model tests run in the existing frontend CI job (lint, typecheck, test). E2E runs locally against the dev backend, as today.

## Out of Scope

- **Admin-selectable theme presets** (Harbor, Plum, Slate), the admin theme picker, and the backend settings module: update 2.
- **Engagement features:** the feed card flip/hook, the gut check, flip-card blocks, page thumbs, milestones, the post-reflection reveal, and the "of N careers" total. All of these are update 3.
- **Dark mode:** stays a stretch item.
- **Layout restructuring of pages:** the experience viewer, reflection form and tables keep their structure. Only their styling and the listed additions change.
- **New fonts, logo artwork or illustrations.**
- **Flashcards, graded knowledge checks, streaks, points, leaderboards and star ratings.** These were rejected as conflicting with the "supportive" identity.
- **Any backend change.**

## Further Notes

- **Why this palette:** forest green says growth and grounding, warm orange says curiosity and warmth, and the off-white keeps it calm. Lime-on-forest-green was dropped as a pairing because it's a well-known fintech brand's signature. Leading with orange keeps Grove distinct. Update 2 will record a brand look-alike check for every preset.
- **Update 2 depends on the slot list above.** Keep the slot names stable. Adding a slot later means every preset must define it.
- **Draft requirements for updates 2 and 3:** the requirements written for all three updates earlier in this session are saved (git-ignored) at `.scratch/iterative-reflection-frontend/notes/requirements-updates-1-3.diff`, for reference in those sessions.
- **After this ships:** update `REQUIREMENTS.md` (new rows for the progress features and verdict colors, and N2.6 still holds) and record the palette and slot decisions in `DECISIONS.md`.
