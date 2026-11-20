import { expect, test, type Page } from "@playwright/test";
import { freshStudent as freshStudentFor, xsrf } from "./support/helpers";

// Needs the dev backend. Each test signs up a fresh student through the API.
const freshStudent = (page: Page) => freshStudentFor(page, "reflect", "Reflect Tester");

/** Marks every page viewed through the API so the career can be reflected on. */
async function viewAll(page: Page, id: number) {
  const exp = await (await page.request.get(`/api/careers/${id}/experience`)).json();
  for (let n = 1; n <= exp.lastPage; n++) {
    const r = await page.request.post(`/api/careers/${id}/pages/${n}/viewed`, { headers: { "X-XSRF-TOKEN": await xsrf(page) } });
    expect(r.status()).toBe(200);
  }
}

test("fills the form with the keyboard, submits once on a double-click, lands on the feed", async ({ page }) => {
  const id = await freshStudent(page);
  await viewAll(page, id);
  const posts: string[] = [];
  page.on("request", (r) => r.method() === "POST" && r.url().endsWith("/reflection") && posts.push(r.url()));

  await page.goto(`/student/careers/${id}`);
  await page.getByRole("link", { name: "Reflect on this career" }).click();
  await expect(page).toHaveURL(new RegExp(`/student/careers/${id}/reflect$`));

  const submit = page.getByRole("button", { name: "Submit reflection" });
  await expect(submit).toBeDisabled();

  // Keyboard: focus the first radio group and use arrows.
  const first = page.getByTestId("question-interest").getByRole("radio", { name: "1", exact: true });
  await first.focus();
  await page.keyboard.press("ArrowRight");
  await page.keyboard.press("ArrowRight");
  await expect(page.getByTestId("question-interest").getByRole("radio", { name: "3", exact: true })).toBeChecked();
  await expect(first).not.toBeChecked(); // the selection moved, it did not add
  await page.keyboard.press("ArrowLeft");
  await expect(page.getByTestId("question-interest").getByRole("radio", { name: "2", exact: true })).toBeChecked();
  await page.keyboard.press("ArrowRight");

  for (const key of ["values", "lifestyle", "responsibilities", "sacrifices"]) {
    await page.getByTestId(`question-${key}`).getByText("7", { exact: true }).click();
    await expect(page.getByTestId(`question-${key}`).getByRole("radio", { name: "7", exact: true })).toBeChecked();
  }
  await expect(submit).toBeDisabled(); // verdict missing
  await page.getByTestId("question-interest").locator("summary").click();
  await page.getByTestId("question-interest").getByRole("textbox").fill("hello");
  await expect(page.getByTestId("interest-counter")).toHaveText("5/1000");
  const verdict = page.getByRole("radiogroup", { name: "Would I genuinely enjoy this?" });
  await expect(verdict.getByRole("radio", { checked: true })).toHaveCount(0);
  await verdict.getByText("MAYBE", { exact: true }).click();
  await expect(verdict.getByRole("radio", { name: "MAYBE" })).toBeChecked();
  await expect(verdict.getByRole("radio", { name: "YES" })).not.toBeChecked();
  await expect(submit).toBeEnabled();

  await submit.dblclick();
  await expect(page).toHaveURL(/\/student\/feed$/);
  expect(posts).toHaveLength(1);

  const me = await (await page.request.get("/api/me")).json();
  expect(me.role).toBe("STUDENT");
  const list = await (await page.request.get("/api/students/me/reflections?page=0&size=100")).json();
  expect(list.content.map((r: { careerId: number }) => r.careerId)).toContain(id);
  expect(list.content[0].verdict).toBe("MAYBE");
});

test("a 400 shows the error next to the matching question", async ({ page }) => {
  const id = await freshStudent(page);
  await viewAll(page, id);
  await page.route("**/api/careers/*/reflection", (route) =>
    route.fulfill({
      status: 400,
      contentType: "application/problem+json",
      body: JSON.stringify({ title: "Bad Request", status: 400, detail: "Validation failed.", errors: [{ field: "answers[values].score", message: "Score must be between 1 and 10." }] }),
    }),
  );
  await page.goto(`/student/careers/${id}/reflect`);
  for (const key of ["interest", "values", "lifestyle", "responsibilities", "sacrifices"]) {
    await page.getByTestId(`question-${key}`).getByText("5", { exact: true }).click();
  }
  await page.getByText("YES", { exact: true }).click();
  await page.getByRole("button", { name: "Submit reflection" }).click();
  await expect(page.getByTestId("question-values").getByRole("alert")).toHaveText("Score must be between 1 and 10.");
  await expect(page.getByTestId("question-interest").getByRole("alert")).toHaveCount(0);
});

test("a 409 for an unviewed last page sends the student back to the experience", async ({ page }) => {
  const id = await freshStudent(page);
  await page.goto(`/student/careers/${id}/reflect`);
  for (const key of ["interest", "values", "lifestyle", "responsibilities", "sacrifices"]) {
    await page.getByTestId(`question-${key}`).getByText("5", { exact: true }).click();
  }
  await page.getByText("NO", { exact: true }).click();
  await page.getByRole("button", { name: "Submit reflection" }).click();
  await expect(page).toHaveURL(new RegExp(`/student/careers/${id}$`));
});
