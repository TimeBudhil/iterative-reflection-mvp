import { randomUUID } from "node:crypto";
import { request, type APIRequestContext } from "@playwright/test";

const baseURL = `http://localhost:${process.env.NEXT_PORT ?? 5173}`;

export async function apiContext(): Promise<APIRequestContext> {
  return request.newContext({ baseURL, extraHTTPHeaders: { Origin: baseURL } });
}

async function xsrf(c: APIRequestContext): Promise<Record<string, string>> {
  await c.get("/api/me");
  const cookie = (await c.storageState()).cookies.find((k) => k.name === "XSRF-TOKEN");
  return { "X-XSRF-TOKEN": decodeURIComponent(cookie?.value ?? "") };
}

export async function send(c: APIRequestContext, url: string, data?: unknown) {
  const res = await c.post(url, { data, headers: await xsrf(c) });
  if (!res.ok()) throw new Error(`POST ${url} -> ${res.status()} ${await res.text()}`);
  return res;
}

/** Signs in as the demo counselor in a fresh API context. */
export async function counselorContext(): Promise<APIRequestContext> {
  const c = await apiContext();
  await send(c, "/api/auth/signin", { email: "counselor@demo.example", password: "demo" });
  return c;
}

export type SeededStudent = { email: string; password: string; displayName: string; id: number };

/**
 * Creates a student linked to the demo counselor who reflected on `reflections` careers (verdicts cycle
 * YES, MAYBE, NO; the first dimension gets an open answer), optionally abandoned one more career and
 * filled in the intake. Uses the real backend only.
 */
export async function seedLinkedStudent(opts: { reflections: number; abandon: boolean; intake: boolean }): Promise<SeededStudent> {
  const counselor = await counselorContext();
  const { joinCode } = await (await counselor.get("/api/counselor/me")).json();
  const stamp = `${Date.now()}${randomUUID().slice(0, 6)}`;
  const student = { email: `seed${stamp}@e2e.example`, password: "password12345", displayName: `Seed ${stamp}` };
  const c = await apiContext();
  await send(c, "/api/auth/signup", {
    ...student,
    birthdate: "2000-01-01",
    acceptedTermsVersion: "1",
    counselorCode: joinCode,
  });
  await send(c, "/api/auth/signin", { email: student.email, password: student.password });
  const questions: { key: string }[] = await (await c.get("/api/reflection-questions")).json();
  const verdicts = ["YES", "MAYBE", "NO"];

  async function openCard() {
    const feed = await (await c.get("/api/feed/next")).json();
    const exp = await (await c.get(`/api/careers/${feed.career.id}/experience`)).json();
    return { id: feed.career.id as number, pages: exp.pages.length as number };
  }
  async function view(id: number, upTo: number) {
    for (let n = 1; n <= upTo; n++) await send(c, `/api/careers/${id}/pages/${n}/viewed`);
  }

  for (let i = 0; i < opts.reflections; i++) {
    const { id, pages } = await openCard();
    await view(id, pages);
    const answers: Record<string, { score: number; openText?: string }> = {};
    questions.forEach((q, j) => {
      answers[q.key] = { score: ((i + j) % 10) + 1, ...(j === 0 ? { openText: `Open answer ${i + 1}` } : {}) };
    });
    await send(c, `/api/careers/${id}/reflection`, { answers, verdict: verdicts[i % 3] });
  }
  if (opts.abandon) {
    const { id } = await openCard();
    await view(id, 1);
    await send(c, `/api/careers/${id}/abandon`);
  }
  if (opts.intake) {
    const qs: { key: string; type: string; options?: { id: number }[] }[] = await (await c.get("/api/intake/questions")).json();
    const answers: Record<string, unknown> = {};
    for (const q of qs) answers[q.key] = q.type === "TAGS" ? { tagIds: q.options!.slice(0, 2).map((o) => o.id) } : { text: `Intake text for ${q.key}` };
    await send(c, "/api/intake", { answers });
  }
  await c.dispose();

  const list = await (await counselor.get("/api/counselor/students?page=0&size=100")).json();
  const row = list.content.find((r: { displayName: string }) => r.displayName === student.displayName);
  await counselor.dispose();
  return { ...student, id: row.studentId };
}
