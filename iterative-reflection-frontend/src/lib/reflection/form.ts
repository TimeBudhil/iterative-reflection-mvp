/**
 * Pure reflection form model (R5.1, R5.2). Used to submit (ticket 12) and, given an `initial` value, to edit (ticket 14).
 */
export type Verdict = "YES" | "MAYBE" | "NO";
export const VERDICTS: Verdict[] = ["YES", "MAYBE", "NO"];
export const MAX_OPEN_TEXT = 1000;

export type Question = { key: string; prompt: string; openPrompt: string; minScore: number; maxScore: number };
export type Draft = { scores: Record<string, number | undefined>; texts: Record<string, string | undefined>; verdict?: Verdict };
/** The shape of an entry in `GET /api/students/me/reflections`. */
export type ExistingReflection = {
  verdict?: string;
  answers?: { key?: string; score?: number; openText?: string | null }[];
};
export type SubmitRequest = { answers: Record<string, { score: number; openText?: string }>; verdict: Verdict };
export type FormErrors = {
  questions: Record<string, { score?: string; openText?: string }>;
  verdict?: string;
  /** Server messages that don't belong to a visible field. */
  other: string[];
};

export function initialDraft(_questions: Question[], initial?: ExistingReflection): Draft {
  const draft: Draft = { scores: {}, texts: {} };
  if (!initial) return draft;
  for (const a of initial.answers ?? []) {
    if (!a.key) continue;
    if (typeof a.score === "number") draft.scores[a.key] = a.score;
    if (a.openText) draft.texts[a.key] = a.openText;
  }
  if (initial.verdict && (VERDICTS as string[]).includes(initial.verdict)) draft.verdict = initial.verdict as Verdict;
  return draft;
}

export function clientErrors(questions: Question[], draft: Draft): FormErrors {
  const errors: FormErrors = { questions: {}, other: [] };
  for (const q of questions) {
    const score = draft.scores[q.key];
    if (score !== undefined && (!Number.isInteger(score) || score < q.minScore || score > q.maxScore)) {
      (errors.questions[q.key] ??= {}).score = `Pick a score from ${q.minScore} to ${q.maxScore}.`;
    }
    if ((draft.texts[q.key] ?? "").trim().length > MAX_OPEN_TEXT) {
      (errors.questions[q.key] ??= {}).openText = `Keep this to ${MAX_OPEN_TEXT} characters or fewer.`;
    }
  }
  return errors;
}

export function canSubmit(questions: Question[], draft: Draft): boolean {
  if (!draft.verdict) return false;
  if (!questions.every((q) => draft.scores[q.key] !== undefined)) return false;
  return Object.keys(clientErrors(questions, draft).questions).length === 0;
}

/** Only call when `canSubmit` is true. Blank open texts are left out. */
export function toRequest(questions: Question[], draft: Draft): SubmitRequest {
  const answers: SubmitRequest["answers"] = {};
  for (const q of questions) {
    const openText = (draft.texts[q.key] ?? "").trim();
    answers[q.key] = { score: draft.scores[q.key] as number, ...(openText ? { openText } : {}) };
  }
  return { answers, verdict: draft.verdict as Verdict };
}

/** Server `fieldErrors` (`answers[key].score`, `answers[key].openText`, `verdict`) onto the questions. */
export function mergeServerErrors(fieldErrors: Record<string, string>): FormErrors {
  const errors: FormErrors = { questions: {}, other: [] };
  for (const [field, message] of Object.entries(fieldErrors)) {
    const m = field.match(/^answers\[([^\]]+)\]\.(score|openText)$/);
    if (m) (errors.questions[m[1]] ??= {})[m[2] as "score" | "openText"] = message;
    else if (field === "verdict") errors.verdict = message;
    else errors.other.push(message);
  }
  return errors;
}

export const CONCURRENT_EDIT_MESSAGE = "Someone saved this reflection at the same time. Reload and try again.";

/** PUT answers 409 "The reflection was changed concurrently; retry." on an optimistic-lock collision (R5.6). */
export function isConcurrentEdit(error: unknown): boolean {
  const e = error as { status?: number; detail?: string } | null;
  return e?.status === 409 && /concurrent/i.test(e.detail ?? "");
}
