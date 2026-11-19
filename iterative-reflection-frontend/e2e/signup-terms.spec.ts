import { expect, test, type Page } from "@playwright/test";

// Needs the dev backend. Every run uses a unique email; demo counselor code is read at runtime.
const PASSWORD = "correct-horse-battery";
const unique = () => `e2e+${Date.now()}${Math.floor(Math.random() * 1e4)}@example.com`;

async function fillSignup(page: Page, o: { email?: string; password?: string; birthdate?: string; code?: string } = {}) {
  await page.goto("/signin?mode=signup");
  await page.getByLabel("Email").fill(o.email ?? unique());
  await page.getByLabel("Password").fill(o.password ?? PASSWORD);
  await page.getByLabel("Display name").fill("E2E Student");
  await page.getByLabel("Birthdate").fill(o.birthdate ?? "2005-05-05");
  if (o.code) await page.getByLabel("Counselor code (optional)").fill(o.code);
}

const submit = (page: Page) => page.getByRole("button", { name: "Create account" });

test("valid signup from a clean browser signs in and lands on the student feed", async ({ page }) => {
  await page.context().clearCookies();
  let payload: Record<string, unknown> = {};
  page.on("request", (r) => {
    if (r.url().endsWith("/api/auth/signup")) payload = r.postDataJSON();
  });
  await fillSignup(page);
  await expect(submit(page)).toBeDisabled();
  await page.getByLabel(/I agree to the/).check();
  await expect(submit(page)).toBeEnabled();
  await submit(page).click();
  await expect(page).toHaveURL(/\/student\/feed$/);
  expect(payload.acceptedTermsVersion).toBe(process.env.NEXT_PUBLIC_TERMS_VERSION ?? "2026-11-18");
  expect(payload).not.toHaveProperty("counselorCode");
});

test("password hint is shown and an 11-char password is blocked client-side", async ({ page }) => {
  await fillSignup(page, { password: "elevenchars" });
  await expect(page.getByText("12+ characters")).toBeVisible();
  await page.getByLabel(/I agree to the/).check();
  let posted = false;
  page.on("request", (r) => {
    if (r.url().endsWith("/api/auth/signup")) posted = true;
  });
  await submit(page).click();
  await expect(page.locator("#password-error")).toContainText("at least 12");
  expect(posted).toBe(false);
});

test("under 13 shows the age copy (backend 400 with no errors[])", async ({ page }) => {
  await page.route("**/api/auth/signup", (route) =>
    route.fulfill({
      status: 400,
      contentType: "application/problem+json",
      body: JSON.stringify({ status: 400, title: "Bad Request", detail: "You must be at least 13 years old to sign up." }),
    }),
  );
  await fillSignup(page);
  await page.getByLabel(/I agree to the/).check();
  await submit(page).click();
  await expect(page.locator("#birthdate-error")).toHaveText("You must be 13 or older to use this app.");
});

test("a recent birthdate is blocked client-side with the same copy", async ({ page }) => {
  const year = new Date().getFullYear() - 10;
  await fillSignup(page, { birthdate: `${year}-01-01` });
  await page.getByLabel(/I agree to the/).check();
  await submit(page).click();
  await expect(page.locator("#birthdate-error")).toHaveText("You must be 13 or older to use this app.");
});

test("backend errors[] fields show under their inputs", async ({ page }) => {
  await page.route("**/api/auth/signup", (route) =>
    route.fulfill({
      status: 400,
      contentType: "application/problem+json",
      body: JSON.stringify({
        status: 400,
        title: "Bad Request",
        detail: "Validation failed.",
        errors: [
          { field: "displayName", message: "must not be blank" },
          { field: "email", message: "must be a well-formed email address" },
        ],
      }),
    }),
  );
  await fillSignup(page);
  await page.getByLabel(/I agree to the/).check();
  await submit(page).click();
  await expect(page.locator("#displayName-error")).toHaveText("must not be blank");
  await expect(page.locator("#email-error")).toHaveText("must be a well-formed email address");
});

test("duplicate email shows the 409 copy", async ({ page }) => {
  await fillSignup(page, { email: "student1@demo.example" });
  await page.getByLabel(/I agree to the/).check();
  await submit(page).click();
  await expect(page.locator("#email-error")).toHaveText("An account with this email already exists.");
});

test("unknown counselor code errors on the code field, keeps values, and creates no account", async ({ page }) => {
  const email = unique();
  await fillSignup(page, { email, code: "ZZZZZZZZ" });
  await page.getByLabel(/I agree to the/).check();
  await submit(page).click();
  await expect(page.locator("#counselorCode-error")).toHaveText("Unknown join code.");
  await expect(page.getByLabel("Email")).toHaveValue(email);
  await expect(page.getByLabel("Display name")).toHaveValue("E2E Student");
  await expect(page.getByLabel("Counselor code (optional)")).toHaveValue("ZZZZZZZZ");
  await expect(page).toHaveURL(/\/signin\?mode=signup$/);

  // No account was created: signing in with those credentials fails.
  await page.getByRole("tab", { name: "Sign in" }).click();
  await page.getByLabel("Email").fill(email);
  await page.getByLabel("Password").fill(PASSWORD);
  await page.getByRole("button", { name: "Sign in", exact: true }).click();
  await expect(page.locator("form [role=alert]")).toHaveText("Email or password is incorrect.");
});

test("terms link opens /terms, marked draft, showing the version", async ({ page }) => {
  await fillSignup(page);
  const link = page.getByRole("link", { name: "terms" });
  await expect(link).toHaveAttribute("href", "/terms");
  await page.goto("/terms");
  await expect(page.getByText("Draft — not legal text")).toBeVisible();
  await expect(page.getByTestId("terms-version")).toHaveText(process.env.NEXT_PUBLIC_TERMS_VERSION ?? "2026-11-18");
});
