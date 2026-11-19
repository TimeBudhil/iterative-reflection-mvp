/**
 * View model for the counselor's read-only student page (R9.5).
 *
 * The generated schema types reflection answers as the intake `Answer` (a springdoc quirk), so the real
 * wire shape is declared here. See backend-api.md "Counselor portal".
 */
export type RawStudentDetail = {
  studentId?: number;
  displayName?: string;
  reflections?: {
    id?: number;
    careerId?: number;
    careerTitle?: string;
    verdict?: string;
    createdAt?: string;
    updatedAt?: string;
    answers?: unknown[];
  }[];
  abandonedCareers?: { careerId?: number; title?: string; lastViewedPage?: number; abandonCount?: number }[];
  topAffinities?: { tagId?: number; tagName?: string; category?: string }[];
  intake?: { answers?: { key?: string; prompt?: string; type?: string; text?: string | null; tags?: { id?: number; category?: string; name?: string }[] }[] } | null;
};

type ReflectionAnswer = { key?: string; prompt?: string; score?: number; openText?: string | null };

export type StudentDetailView = {
  name: string;
  affinities: { id: number; tag: string; category: string }[];
  reflections: {
    id: number;
    career: string;
    verdict: string;
    date: string | undefined;
    dimensions: { prompt: string; score: number }[];
    openAnswers: { prompt: string; text: string }[];
  }[];
  abandoned: { career: string; lastPage: number; count: number }[];
  /** null when the student hasn't submitted an intake. */
  intake: { prompt: string; answer: string }[] | null;
};

export function toStudentDetailView(raw: RawStudentDetail): StudentDetailView {
  return {
    name: raw.displayName ?? "",
    affinities: (raw.topAffinities ?? []).map((a) => ({ id: a.tagId ?? 0, tag: a.tagName ?? "", category: a.category ?? "" })),
    reflections: (raw.reflections ?? []).map((r) => {
      const answers = (r.answers ?? []) as ReflectionAnswer[];
      return {
        id: r.id ?? 0,
        career: r.careerTitle ?? "",
        verdict: r.verdict ?? "",
        date: r.createdAt,
        dimensions: answers.map((a) => ({ prompt: a.prompt ?? a.key ?? "", score: a.score ?? 0 })),
        openAnswers: answers.filter((a) => a.openText).map((a) => ({ prompt: a.prompt ?? a.key ?? "", text: a.openText! })),
      };
    }),
    abandoned: (raw.abandonedCareers ?? []).map((c) => ({ career: c.title ?? "", lastPage: c.lastViewedPage ?? 0, count: c.abandonCount ?? 0 })),
    intake: raw.intake
      ? (raw.intake.answers ?? [])
          .map((a) => ({ prompt: a.prompt ?? "", answer: a.text || (a.tags ?? []).map((t) => t.name).join(", ") }))
          .filter((a) => a.answer)
      : null,
  };
}
