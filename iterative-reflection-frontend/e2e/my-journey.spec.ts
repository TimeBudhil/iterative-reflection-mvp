import { expect, test, type Page } from "@playwright/test";
import { seedLinkedStudent, type SeededStudent } from "./support/seed-student";
import { signIn as signInTo } from "./support/helpers";

// Needs the dev backend. Students are seeded through the real API (fresh each run).
const signIn = (page: Page, s: SeededStudent) => signInTo(page, s.email, s.password, /\/student\/feed$/);

type Reflection = { id: number; careerId: number; careerTitle: string; verdict: string; updatedAt: string; answers: { key: string; score: number }[] };

test("shows stats, tabs and rows from the reflections list and calls nothing else", async ({ page }) => {
  const student = await seedLinkedStudent({ reflections: 4, abandon: false, intake: false }); // YES, MAYBE, NO, YES
  await signIn(page, student);

  const apis: string[] = [];
  page.on("request", (r) => {
    const u = new URL(r.url());
    if (u.pathname.startsWith("/api/")) apis.push(`${r.method()} ${u.pathname}`);
  });
  await page.goto("/student/journey");
  await expect(page.getByTestId("journey-row")).toHaveCount(3); // Saved = YES + MAYBE

  const list: Reflection[] = await page.evaluate(async () => (await (await fetch("/api/students/me/reflections?page=0&size=100")).json()).content);
  expect(list).toHaveLength(4);
  const count = (v: string) => list.filter((r) => r.verdict === v).length;

  await expect(page.getByTestId("stat-total")).toContainText("4");
  await expect(page.getByTestId("stat-YES")).toContainText(String(count("YES")));
  await expect(page.getByTestId("stat-MAYBE")).toContainText(String(count("MAYBE")));
  await expect(page.getByTestId("stat-NO")).toContainText(String(count("NO")));
  const keys = [...new Set(list.flatMap((r) => r.answers.map((a) => a.key)))];
  expect(keys.length).toBeGreaterThan(0);
  for (const k of keys) {
    const scores = list.flatMap((r) => r.answers.filter((a) => a.key === k).map((a) => a.score));
    const avg = (scores.reduce((a, b) => a + b, 0) / scores.length).toFixed(1);
    await expect(page.getByTestId(`stat-avg-${k}`)).toContainText(avg);
  }

  // Newest first: the last career reflected on (highest id) leads the Saved list.
  const newest = list.reduce((a, b) => (b.id > a.id ? b : a));
  const saved = list.filter((r) => r.verdict !== "NO");
  const expectedSaved = [...saved].sort((a, b) => b.updatedAt.localeCompare(a.updatedAt) || b.id - a.id);
  const rows = page.getByTestId("journey-row");
  await expect(rows.first()).toContainText(newest.careerTitle);
  for (let i = 0; i < expectedSaved.length; i++) {
    await expect(rows.nth(i)).toContainText(expectedSaved[i].careerTitle);
    await expect(rows.nth(i)).toContainText(expectedSaved[i].verdict);
    await expect(rows.nth(i).locator("time")).toHaveText(new Date(expectedSaved[i].updatedAt).toLocaleDateString());
  }

  for (const [tab, n] of [["YES", count("YES")], ["MAYBE", count("MAYBE")], ["NO", count("NO")], ["All", 4], ["Saved", 3]] as const) {
    await page.getByRole("tab", { name: tab, exact: true }).click();
    await expect(rows).toHaveCount(n);
  }

  // The page itself called nothing but the reflections list (and the session probe /api/me).
  expect(apis.filter((c) => c !== "GET /api/me" && c !== "GET /api/students/me/reflections")).toEqual([]);

  await rows.first().getByRole("link").first().click();
  await expect(page).toHaveURL(/\/student\/careers\/\d+$/);

});

test("zero reflections shows the empty state linking to the feed", async ({ page }) => {
  const student = await seedLinkedStudent({ reflections: 0, abandon: false, intake: false });
  await signIn(page, student);
  await page.goto("/student/journey");
  await expect(page.getByText("Reflect on your first career to see your journey here.")).toBeVisible();
  await page.getByRole("link", { name: "Go to the feed" }).click();
  await expect(page).toHaveURL(/\/student\/feed$/);
});
