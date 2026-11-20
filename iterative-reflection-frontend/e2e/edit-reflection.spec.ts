import { expect, test, type Page } from "@playwright/test";
import { seedLinkedStudent, type SeededStudent } from "./support/seed-student";
import { signIn as signInTo } from "./support/helpers";

// Needs the dev backend. Students are seeded through the real API (fresh each run).
const signIn = (page: Page, s: SeededStudent) => signInTo(page, s.email, s.password, /\/student\/feed$/);

type Reflection = { careerId: number; verdict: string; versionNumber: number; answers: { key: string; score: number; openText: string | null }[] };
const reflections = (page: Page): Promise<Reflection[]> =>
  page.evaluate(async () => (await (await fetch("/api/students/me/reflections?page=0&size=100")).json()).content);

test("review mode from a bookmark: free navigation, no Abandon, Edit reflection on every page", async ({ page }) => {
  const student = await seedLinkedStudent({ reflections: 1, abandon: false, intake: false });
  await signIn(page, student);
  const [r] = await reflections(page);

  await page.goto(`/student/careers/${r.careerId}`); // cold load, as from a bookmark
  await expect(page.locator("article")).toHaveAttribute("data-mode", "review");
  await expect(page.getByRole("button", { name: /isn't for me|abandon/i })).toHaveCount(0);
  const jump = page.getByRole("list", { name: "Jump to page" });
  await expect(jump).toBeVisible();
  const pages = await jump.getByRole("button").count();
  for (let n = 1; n <= pages; n++) {
    await jump.getByRole("button", { name: String(n), exact: true }).click();
    await expect(page.getByTestId("page-indicator")).toHaveText(`Page ${n} of ${pages}`);
    await expect(page.getByRole("link", { name: "Edit reflection" })).toBeVisible();
  }
});

test("Edit reflection is prefilled, saves with PUT and returns to My Journey", async ({ page }) => {
  const student = await seedLinkedStudent({ reflections: 1, abandon: false, intake: false });
  await signIn(page, student);
  const [before] = await reflections(page);

  await page.goto("/student/journey");
  await page.getByRole("tab", { name: "All", exact: true }).click();
  await page.getByTestId("journey-row").first().getByRole("link", { name: "Edit reflection" }).click();
  await expect(page).toHaveURL(new RegExp(`/student/careers/${before.careerId}/reflect$`));

  // Prefilled from the entry.
  for (const a of before.answers) {
    await expect(page.getByTestId(`question-${a.key}`).getByRole("radio", { name: String(a.score), exact: true })).toBeChecked();
    if (a.openText) await expect(page.getByTestId(`question-${a.key}`).getByRole("textbox")).toHaveValue(a.openText);
  }
  await expect(page.getByRole("radio", { name: before.verdict, exact: true })).toBeChecked();

  const newVerdict = before.verdict === "NO" ? "YES" : "NO";
  const key = before.answers[1].key;
  const newScore = before.answers[1].score === 10 ? 9 : 10;
  await page.getByTestId(`question-${key}`).getByText(String(newScore), { exact: true }).click();
  await page.getByText(newVerdict, { exact: true }).click();
  await expect(page.getByTestId(`question-${key}`).getByRole("radio", { name: String(newScore), exact: true })).toBeChecked();
  await expect(page.getByRole("radio", { name: newVerdict, exact: true })).toBeChecked();
  await expect(page.getByRole("radio", { name: before.verdict, exact: true })).not.toBeChecked();
  const put = page.waitForRequest((r) => r.method() === "PUT" && r.url().endsWith(`/api/careers/${before.careerId}/reflection`));
  await page.getByRole("button", { name: "Save changes" }).click();
  await put;
  await expect(page).toHaveURL(/\/student\/journey$/);

  const [after] = await reflections(page);
  expect(after.versionNumber).toBe(before.versionNumber + 1);
  expect(after.verdict).toBe(newVerdict);
  expect(after.answers.find((a) => a.key === key)!.score).toBe(newScore);
});

test("a concurrent-edit 409 shows the reload message and refetches the reflections", async ({ page }) => {
  const student = await seedLinkedStudent({ reflections: 1, abandon: false, intake: false });
  await signIn(page, student);
  const [r] = await reflections(page);

  await page.goto(`/student/careers/${r.careerId}/reflect`);
  await expect(page.getByRole("button", { name: "Save changes" })).toBeVisible();
  await page.route(`**/api/careers/${r.careerId}/reflection`, (route) =>
    route.request().method() === "PUT"
      ? route.fulfill({
          status: 409,
          contentType: "application/problem+json",
          body: JSON.stringify({ status: 409, title: "Conflict", detail: "The reflection was changed concurrently; retry." }),
        })
      : route.continue(),
  );
  let refetches = 0;
  page.on("request", (q) => q.url().includes("/api/students/me/reflections") && refetches++);
  await page.getByRole("button", { name: "Save changes" }).click();
  await expect(page.getByTestId("concurrent-edit")).toHaveText("Someone saved this reflection at the same time. Reload and try again.");
  await expect.poll(() => refetches).toBeGreaterThan(0);
  await expect(page).toHaveURL(/\/reflect$/);
});
