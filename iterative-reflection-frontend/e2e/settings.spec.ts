import { expect, test, type Page } from "@playwright/test";

// Needs the dev backend. Student flows use a freshly signed-up user so demo passwords never change.
const NEW_PASSWORD = "a-brand-new-password-1";
const PASSWORD = "initial-password-123";

async function signUpFreshStudent(page: Page) {
  const email = `settings-${Date.now()}-${Math.floor(Math.random() * 1e6)}@test.example`;
  // GET /api/me primes the XSRF-TOKEN cookie in the request context.
  await page.request.get("/api/me");
  const cookies = await page.context().cookies();
  const xsrf = cookies.find((c) => c.name === "XSRF-TOKEN")?.value ?? "";
  const res = await page.request.post("/api/auth/signup", {
    headers: { "X-XSRF-TOKEN": xsrf },
    data: { email, password: PASSWORD, displayName: "Fresh Student", birthdate: "2008-01-01", acceptedTermsVersion: "1" },
  });
  expect(res.status()).toBe(201);
  return email;
}

async function signIn(page: Page, email: string, password: string) {
  await page.goto("/signin");
  await page.getByLabel("Email").fill(email);
  await page.getByLabel("Password").fill(password);
  await page.getByRole("button", { name: "Sign in", exact: true }).click();
  await expect(page).not.toHaveURL(/\/signin/, { timeout: 30_000 });
}

const ROLES = [
  { role: "student", email: "student1@demo.example", path: "/student/settings" },
  { role: "counselor", email: "counselor@demo.example", path: "/counselor/settings" },
  { role: "admin", email: "admin@demo.example", path: "/admin/settings" },
];

for (const r of ROLES) {
  test(`${r.role} settings shows Profile and Password cards reachable by anchor`, async ({ page }) => {
    await signIn(page, r.email, "demo");
    for (const anchor of ["profile", "password"]) {
      await page.goto(`${r.path}#${anchor}`);
      const card = page.locator(`#${anchor}`);
      await expect(card).toBeVisible();
      await expect(card).toBeInViewport();
    }
    await expect(page.getByLabel("Email", { exact: true })).toHaveValue(r.email);
    await expect(page.getByLabel("Email", { exact: true })).toBeDisabled();
  });
}

test("changing the display name updates the user menu without a reload", async ({ page }) => {
  const email = await signUpFreshStudent(page);
  await signIn(page, email, PASSWORD);
  await page.goto("/student/settings");
  await page.evaluate(() => ((window as unknown as { __m: boolean }).__m = true));
  await page.getByLabel("Display name").fill("Renamed Student");
  await page.getByRole("button", { name: "Save" }).click();
  await expect(page.getByTestId("user-menu-name")).toHaveText("Renamed Student");
  await expect(page.getByText("Display name updated.")).toBeVisible();
  expect(await page.evaluate(() => (window as unknown as { __m?: boolean }).__m)).toBe(true);
});

test("password change: mismatch blocked, wrong current on that field, success toasts and stays signed in", async ({ page }) => {
  const email = await signUpFreshStudent(page);
  await signIn(page, email, PASSWORD);
  await page.goto("/student/settings");

  let puts = 0;
  page.on("request", (req) => req.method() === "PUT" && req.url().includes("/api/me/password") && puts++);

  await page.getByLabel("Current password").fill(PASSWORD);
  await page.getByLabel("New password", { exact: true }).fill(NEW_PASSWORD);
  await page.getByLabel("Confirm new password").fill("something-else-entirely");
  await page.getByRole("button", { name: "Change password" }).click();
  await expect(page.getByText("Passwords do not match.")).toBeVisible();
  expect(puts).toBe(0);

  await page.getByLabel("Confirm new password").fill(NEW_PASSWORD);
  await page.getByLabel("Current password").fill("wrong-current-password");
  await page.getByRole("button", { name: "Change password" }).click();
  await expect(page.locator("#current-password-error")).toHaveText("Current password is incorrect.");

  await page.getByLabel("Current password").fill(PASSWORD);
  await page.getByRole("button", { name: "Change password" }).click();
  await expect(page.getByText("Password changed.")).toBeVisible();
  await expect(page).toHaveURL(/\/student\/settings$/);
  await page.reload();
  await expect(page.getByRole("heading", { name: "Settings" })).toBeVisible();

  // The new password works for a fresh sign-in.
  await page.context().clearCookies();
  await signIn(page, email, NEW_PASSWORD);
  await expect(page).toHaveURL(/\/student\/feed$/);
});
