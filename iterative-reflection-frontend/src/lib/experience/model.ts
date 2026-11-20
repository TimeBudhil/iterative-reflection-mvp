/**
 * Pure rules of the experience viewer. The viewer is driven by a `ViewerMode`; ticket 11 (abandon) hangs off
 * "in-progress" and ticket 14 (edit reflection) off "review" through the flags in `viewerState`.
 */
export type ViewerMode = "in-progress" | "review";

export type Block = { type?: string; text?: string; url?: string; label?: string };
export type ExperiencePage = { number?: number; title?: string; blocks?: Block[] };
export type Experience = {
  id?: number;
  title?: string;
  summary?: string;
  estimatedMinutes?: number;
  lastPage?: number;
  lastViewedPage?: number;
  pages?: ExperiencePage[];
};

/** A career already in the reflections list is reviewed, otherwise it is the in-progress card (R4.5). */
export function modeFor(careerId: number, reflections: { careerId?: number }[]): ViewerMode {
  return reflections.some((r) => r.careerId === careerId) ? "review" : "in-progress";
}

export function clampPage(n: number, lastPage: number): number {
  return Math.min(Math.max(n, 1), Math.max(lastPage, 1));
}

/** In progress resumes at `lastViewedPage`; review (and no progress) starts at page 1 (R4.2). */
export function startPage(mode: ViewerMode, e: { lastViewedPage?: number; lastPage?: number }): number {
  if (mode === "review") return 1;
  return clampPage(e.lastViewedPage ?? 0, e.lastPage ?? 1);
}

export function viewerState(mode: ViewerMode, page: number, lastPage: number) {
  return {
    progress: clampPage(page, lastPage) / Math.max(lastPage, 1),
    label: `Page ${page} of ${lastPage}`,
    hasPrev: page > 1,
    hasNext: page < lastPage,
    freeNavigation: mode === "review",
    showReflect: mode === "in-progress" && page === lastPage,
    showEditReflection: mode === "review",
  };
}

/** 404 (unknown/retired) and 409 (not the in-progress card) both mean "not available right now" (R4.6). */
export function isUnavailable(error: unknown): boolean {
  const status = (error as { status?: number } | null)?.status;
  return status === 404 || status === 409;
}
