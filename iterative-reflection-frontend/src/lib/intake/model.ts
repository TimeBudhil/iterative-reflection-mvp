/** Pure intake form model (R7). Questions always come from `GET /api/intake/questions`. */
export const MAX_TEXT = 2000;

export type TagOption = { id: number; category: string; name: string };
export type IntakeQuestion = { key: string; prompt: string; type: "TAGS" | "TEXT"; options?: TagOption[] | null };
export type ExistingIntake = { answers?: { key?: string; text?: string | null; tags?: { id?: number }[] }[] };
export type IntakeDraft = { tags: Record<string, number[]>; texts: Record<string, string> };
export type IntakeRequest = { answers: Record<string, { text?: string; tagIds?: number[] }> };

/** Groups by `category`, in the order each category first appears (the API orders by category). */
export function groupByCategory(options: TagOption[]): { category: string; options: TagOption[] }[] {
  const groups: { category: string; options: TagOption[] }[] = [];
  for (const o of options) {
    let g = groups.find((x) => x.category === o.category);
    if (!g) groups.push((g = { category: o.category, options: [] }));
    g.options.push(o);
  }
  return groups;
}

export function initialDraft(questions: IntakeQuestion[], existing?: ExistingIntake | null): IntakeDraft {
  const draft: IntakeDraft = { tags: {}, texts: {} };
  for (const a of existing?.answers ?? []) {
    const q = questions.find((x) => x.key === a.key);
    if (!q || !a.key) continue;
    if (q.type === "TAGS") draft.tags[a.key] = (a.tags ?? []).map((t) => t.id).filter((id): id is number => id !== undefined);
    else if (a.text) draft.texts[a.key] = a.text;
  }
  return draft;
}

export function toggleTag(ids: number[], id: number): number[] {
  return ids.includes(id) ? ids.filter((x) => x !== id) : [...ids, id];
}

/**
 * The full request body (PUT replaces every answer, so the whole form is always sent). Blank answers are
 * dropped; returns null when nothing is answered or a text is over the limit.
 */
export function buildRequest(questions: IntakeQuestion[], draft: IntakeDraft): IntakeRequest | null {
  const answers: IntakeRequest["answers"] = {};
  for (const q of questions) {
    if (q.type === "TAGS") {
      const ids = draft.tags[q.key] ?? [];
      if (ids.length) answers[q.key] = { tagIds: ids };
    } else {
      const text = (draft.texts[q.key] ?? "").trim();
      if (text.length > MAX_TEXT) return null;
      if (text) answers[q.key] = { text };
    }
  }
  return Object.keys(answers).length ? { answers } : null;
}

/** The 409 for "fewer than 3 reflections" (the other 409, "already submitted", is a different case). */
export function isNotYetAvailable(error: { status?: number; detail?: string } | null | undefined): boolean {
  return error?.status === 409 && (error.detail ?? "").startsWith("Reflect on at least 3");
}

export function interestsStatus(completed: boolean): string {
  return completed ? "Completed" : "Not filled in";
}

/** Splits `ApiError.fieldErrors` into per-question messages and a form-level one. */
export function mergeFieldErrors(fieldErrors: Record<string, string>): { byKey: Record<string, string>; form?: string } {
  const byKey: Record<string, string> = {};
  let form: string | undefined;
  for (const [field, message] of Object.entries(fieldErrors)) {
    const m = /^answers\[([^\]]+)\]/.exec(field);
    if (m) byKey[m[1]] ??= message;
    else form ??= message;
  }
  return { byKey, form };
}
