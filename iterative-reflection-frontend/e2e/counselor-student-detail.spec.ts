import { expect, test, type Page } from "@playwright/test";
import { seedLinkedStudent, type SeededStudent } from "./support/seed-student";

// Needs the dev backend (demo accounts, password "demo"); seeds a real linked student through the API.
async function signInCounselor(page: Page) {
  await page.goto("/signin");
  await page.getByLabel("Email").fill("counselor@demo.example");
  await page.getByLabel("Password").fill("demo");
  await page.getByRole("button", { name: "Sign in", exact: true }).click();
  await expect(page).toHaveURL(/\/counselor\/students$/);
}

let rich: SeededStudent;
test.beforeAll(async () => {
  rich = await seedLinkedStudent({ reflections: 3, abandon: true, intake: true });
});

test("a student row opens the detail with all four read-only sections", async ({ page }) => {
  await signInCounselor(page);
  await page.getByRole("link", { name: rich.displayName }).click();
  await expect(page).toHaveURL(new RegExp(`/counselor/students/${rich.id}$`));
  await expect(page.getByRole("heading", { level: 1, name: rich.displayName })).toBeVisible();

  const api = await page.evaluate(async (id) => (await fetch(`/api/counselor/students/${id}`)).json(), rich.id);

  // Top interests: tag + category, in API order.
  const interests = page.getByRole("region", { name: "Top interests" });
  await expect(interests.getByRole("listitem")).toHaveCount(api.topAffinities.length);
  for (const a of api.topAffinities) {
    await expect(interests.getByText(a.tagName, { exact: false }).first()).toBeVisible();
    await expect(interests.getByText(`(${a.category})`).first()).toBeVisible();
  }

  // Reflections: career, verdict, a score per dimension (5), open answer, date.
  const reflections = page.getByTestId("reflection");
  await expect(reflections).toHaveCount(3);
  const first = reflections.first();
  await expect(first.getByRole("heading", { name: api.reflections[0].careerTitle })).toBeVisible();
  await expect(first.getByText(api.reflections[0].verdict, { exact: true })).toBeVisible();
  await expect(first.locator("dd")).toHaveCount(5);
  await expect(first.getByText("Open answer 3")).toBeVisible();
  await expect(first.getByText(/\b20\d\d\b/)).toBeVisible();

  // Abandoned careers: career, last page, count.
  const ab = api.abandonedCareers[0];
  await expect(page.getByText(`reached page ${ab.lastViewedPage}, abandoned ${ab.abandonCount} time`)).toBeVisible();
  await expect(page.getByText(ab.title, { exact: true })).toBeVisible();

  // Intake answers.
  await expect(page.getByRole("heading", { name: "Intake answers" })).toBeVisible();
  await expect(page.getByText("Intake text for goals")).toBeVisible();

  // Read-only: nothing to type into or submit.
  await expect(page.getByRole("main").locator("input, textarea, select, form, button")).toHaveCount(0);
});

test("a student with no activity shows empty states", async ({ page }) => {
  const blank = await seedLinkedStudent({ reflections: 0, abandon: false, intake: false });
  await signInCounselor(page);
  await page.goto(`/counselor/students/${blank.id}`);
  await expect(page.getByText("No reflections yet.")).toBeVisible();
  await expect(page.getByText("No abandoned careers.")).toBeVisible();
  await expect(page.getByText("The student hasn't filled in the intake yet.")).toBeVisible();
});

test("a 403 shows the not-shared message", async ({ page }) => {
  await signInCounselor(page);
  await page.goto("/counselor/students/999999");
  await expect(page.getByText("This student hasn't shared their progress with you.")).toBeVisible();
});

test("a server error shows the retry panel with the request id", async ({ page }) => {
  await signInCounselor(page);
  await page.route("**/api/counselor/students/*", (route) =>
    route.fulfill({
      status: 500,
      headers: { "X-Request-Id": "req-detail" },
      contentType: "application/problem+json",
      body: '{"status":500,"title":"Internal Server Error"}',
    }),
  );
  await page.goto(`/counselor/students/${rich.id}`);
  await expect(page.getByText("Request ID: req-detail")).toBeVisible();
  await expect(page.getByRole("button", { name: "Retry" })).toBeVisible();
});
