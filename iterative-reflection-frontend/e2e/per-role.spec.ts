import { expect, test, type Page } from "@playwright/test";
import { counselorContext } from "./support/seed-student";

// Per-role happy paths through the UI only, against the dev backend (N6.2). Emails are unique per run.
const PASSWORD = "correct-horse-battery";
const TEMP = "temporary-pass-1";
const stamp = `${Date.now()}-${Math.floor(Math.random() * 1e6)}`;
const student = { email: `e2e+student-${stamp}@example.com`, name: `E2E Student ${stamp}` };

async function signIn(page: Page, email: string, password: string) {
  await page.goto("/signin");
  await page.getByLabel("Email").fill(email);
  await page.getByLabel("Password").fill(password);
  await page.getByRole("button", { name: "Sign in", exact: true }).click();
}

/** One trip round the loop: Start, Next to the last page, Reflect, answer, Submit. Nothing else is clicked. */
async function reflectOnce(page: Page, verdict: "YES" | "MAYBE" | "NO") {
  await page.getByRole("link", { name: "Start", exact: true }).click();
  await expect(page).toHaveURL(/\/student\/careers\/\d+$/, { timeout: 30_000 });
  const indicator = page.getByTestId("page-indicator");
  await expect(indicator).toHaveText(/^Page 1 of \d+$/);
  const last = Number((await indicator.textContent())!.match(/of (\d+)/)![1]);
  for (let n = 2; n <= last; n++) {
    await page.getByRole("button", { name: "Next", exact: true }).click();
    await expect(indicator).toHaveText(`Page ${n} of ${last}`);
  }
  // N5.1: no dialog or extra step stands between the last page and the reflection form.
  await expect(page.getByRole("dialog")).toHaveCount(0);
  await page.getByRole("link", { name: "Reflect on this career" }).click();
  await expect(page).toHaveURL(/\/student\/careers\/\d+\/reflect$/);
  for (const key of ["interest", "values", "lifestyle", "responsibilities", "sacrifices"]) {
    await page.getByTestId(`question-${key}`).getByText("7", { exact: true }).click();
  }
  await page.getByText(verdict, { exact: true }).click();
  await page.getByRole("button", { name: "Submit reflection" }).click();
  await expect(page).toHaveURL(/\/student\/feed$/);
}

test.describe.serial("per-role happy paths", () => {
  test("student signs up, reflects x3, handles the intake modal and sees YES/MAYBE in My Journey", async ({ page }) => {
    const { joinCode } = await (await (await counselorContext()).get("/api/counselor/me")).json();
    await page.context().clearCookies();
    await page.goto("/signin?mode=signup");
    await page.getByLabel("Email").fill(student.email);
    await page.getByLabel("Password").fill(PASSWORD);
    await page.getByLabel("Display name").fill(student.name);
    await page.getByLabel("Birthdate").fill("2005-05-05");
    await page.getByLabel("Counselor code (optional)").fill(joinCode);
    await page.getByLabel(/I agree to the/).check();
    await page.getByRole("button", { name: "Create account" }).click();
    await expect(page).toHaveURL(/\/student\/feed$/, { timeout: 30_000 });

    const calls: string[] = [];
    page.on("request", (r) => {
      const p = new URL(r.url()).pathname;
      if (r.method() !== "GET" && p.startsWith("/api/")) calls.push(`${r.method()} ${p.replace(/\d+/g, "N")}`);
    });

    await reflectOnce(page, "YES");
    await expect(page.getByTestId("intake-prompt")).toHaveCount(0);
    await reflectOnce(page, "MAYBE");
    await expect(page.getByTestId("intake-prompt")).toHaveCount(0);
    // N5.1: the only writes were page views and the reflection, never anything else.
    expect([...new Set(calls)].sort()).toEqual(["POST /api/careers/N/pages/N/viewed", "POST /api/careers/N/reflection"]);
    await reflectOnce(page, "NO");

    // The third reflection opens the intake prompt.
    await expect(page.getByTestId("intake-prompt")).toBeVisible();
    await page.getByRole("button", { name: "Fill it in" }).click();
    await expect(page).toHaveURL(/\/student\/intake$/);
    await page.getByRole("button", { name: "Health & Medicine" }).click();
    await page.getByRole("button", { name: "Save interests" }).click();
    await expect(page).toHaveURL(/\/student\/feed$/);

    await page.goto("/student/journey");
    const rows = page.getByTestId("journey-row");
    await expect(rows).toHaveCount(2); // Saved = YES + MAYBE
    await expect(page.getByTestId("stat-total")).toContainText("3");
    await expect(rows.filter({ hasText: "YES" })).toHaveCount(1);
    await expect(rows.filter({ hasText: "MAYBE" })).toHaveCount(1);
  });

  test("the linked demo counselor views that student's detail", async ({ page }) => {
    await signIn(page, "counselor@demo.example", "demo");
    await expect(page).toHaveURL(/\/counselor\/students$/);
    await page.getByRole("link", { name: student.name }).click();
    await expect(page).toHaveURL(/\/counselor\/students\/\d+$/);
    await expect(page.getByRole("heading", { level: 1, name: student.name })).toBeVisible();
    await expect(page.getByTestId("reflection")).toHaveCount(3);
    await expect(page.getByRole("region", { name: "Top interests" })).toBeVisible();
  });

  test("an admin creates a counselor whose first sign-in forces a password change", async ({ page, browser }) => {
    const email = `e2e+counselor-${stamp}@example.com`;
    const name = `E2E Counselor ${stamp}`;
    await signIn(page, "admin@demo.example", "demo");
    await expect(page).toHaveURL(/\/admin\/metrics$/);
    await page.goto("/admin/counselors");
    await page.getByRole("button", { name: "Add counselor" }).click();
    const dialog = page.getByRole("dialog");
    await dialog.getByLabel("Email").fill(email);
    await dialog.getByLabel("Display name").fill(name);
    await dialog.getByLabel("Temporary password").fill(TEMP);
    await dialog.getByRole("button", { name: "Create counselor" }).click();
    await dialog.getByRole("button", { name: "Done" }).click();
    await expect(page.getByRole("table", { name: "Counselors" }).getByRole("row", { name: new RegExp(name) })).toBeVisible();

    const other = await (await browser.newContext()).newPage();
    await signIn(other, email, TEMP);
    await expect(other).toHaveURL(/\/change-password$/);
    await other.getByLabel("Current password").fill(TEMP);
    await other.getByLabel("New password", { exact: true }).fill("a-brand-new-password");
    await other.getByLabel("Confirm new password").fill("a-brand-new-password");
    await other.getByRole("button", { name: "Change password" }).click();
    await expect(other).toHaveURL(/\/counselor\/students$/);
    await other.context().close();
  });
});
