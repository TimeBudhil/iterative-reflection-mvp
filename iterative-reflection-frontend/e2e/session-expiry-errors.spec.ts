import { expect, test, type Page } from "@playwright/test";

// Needs the dev backend (demo accounts, password "demo").
async function signIn(page: Page, email = "student1@demo.example") {
  await page.goto("/signin");
  await page.getByLabel("Email").fill(email);
  await page.getByLabel("Password").fill("demo");
  await page.getByRole("button", { name: "Sign in", exact: true }).click();
}

const NOTICE = "Your session expired. Please sign in again.";

test("an expired session goes to /signin?next=<path> with the notice, and sign-in returns there", async ({ page }) => {
  await signIn(page);
  await expect(page).toHaveURL(/\/student\/feed$/);
  await page.goto("/student/journey");
  await expect(page.getByRole("navigation", { name: "Main" })).toBeVisible();

  await page.context().clearCookies({ name: "SESSION" }); // the server no longer knows this session
  await page.reload();

  await expect(page).toHaveURL(/\/signin\?next=%2Fstudent%2Fjourney&expired=1$/);
  await expect(page.getByRole("status")).toHaveText(NOTICE);

  await page.getByLabel("Email").fill("student1@demo.example");
  await page.getByLabel("Password").fill("demo");
  await page.getByRole("button", { name: "Sign in", exact: true }).click();
  await expect(page).toHaveURL(/\/student\/journey$/);
});

test("a 401 on sign-in itself is wrong credentials, not an expiry", async ({ page }) => {
  await page.goto("/signin");
  await page.getByLabel("Email").fill("student1@demo.example");
  await page.getByLabel("Password").fill("nope");
  await page.getByRole("button", { name: "Sign in", exact: true }).click();
  await expect(page.locator("form [role=alert]")).toHaveText("Email or password is incorrect.");
  await expect(page).toHaveURL(/\/signin$/);
});

for (const [label, next] of [
  ["absolute URL", "https://evil.com"],
  ["protocol-relative URL", "//evil.com"],
  ["another role's prefix", "/admin/metrics"],
] as const) {
  test(`next=${label} lands on the role home`, async ({ page }) => {
    await page.goto(`/signin?next=${encodeURIComponent(next)}`);
    await page.getByLabel("Email").fill("student1@demo.example");
    await page.getByLabel("Password").fill("demo");
    await page.getByRole("button", { name: "Sign in", exact: true }).click();
    await expect(page).toHaveURL(/localhost:\d+\/student\/feed$/);
  });
}

test("an unmatched path renders a 404 with a link to the user's home", async ({ page }) => {
  await signIn(page);
  await expect(page).toHaveURL(/\/student\/feed$/);
  await page.goto("/no/such/page");
  await expect(page.getByRole("heading", { name: "Page not found" })).toBeVisible();
  await page.getByTestId("not-found-home").click();
  await expect(page).toHaveURL(/\/student\/feed$/);
});

test("an unexpected 403 with nothing changed shows the no-access panel with a link home", async ({ page }) => {
  await signIn(page);
  await expect(page).toHaveURL(/\/student\/feed$/);
  await page.route("**/api/auth/signout", (route) =>
    route.fulfill({ status: 403, contentType: "application/problem+json", body: JSON.stringify({ status: 403, title: "Forbidden", detail: "Access is denied." }) }),
  );
  await page.getByTestId("user-menu-name").click();
  await page.getByRole("menuitem", { name: "Sign out" }).click();
  const panel = page.getByTestId("no-access");
  await expect(panel).toHaveText(/You don't have access to this\./);
  await expect(page.getByText("Access is denied")).toHaveCount(0);
  await panel.getByRole("link").click();
  await expect(page).toHaveURL(/\/student\/feed$/);
  await expect(page.getByTestId("no-access")).toHaveCount(0);
});

test("an unexpected 403 after the role changed re-runs the guards instead", async ({ page }) => {
  await signIn(page);
  await expect(page).toHaveURL(/\/student\/feed$/);
  await page.route("**/api/auth/signout", (route) =>
    route.fulfill({ status: 403, contentType: "application/problem+json", body: JSON.stringify({ status: 403, title: "Forbidden" }) }),
  );
  await page.route("**/api/me", (route) =>
    route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({ id: 3, email: "student1@demo.example", displayName: "Student", role: "ADMIN", mustChangePassword: false, intakePromptDue: false }),
    }),
  );
  await page.getByTestId("user-menu-name").click();
  await page.getByRole("menuitem", { name: "Sign out" }).click();
  await expect(page).toHaveURL(/\/admin\/metrics$/);
});

test("a 5xx shows the retry panel with the request ID and Try again refetches", async ({ page }) => {
  await signIn(page);
  await expect(page).toHaveURL(/\/student\/feed$/);
  let failing = true;
  await page.route("**/api/me", (route) =>
    failing
      ? route.fulfill({
          status: 503,
          headers: { "X-Request-Id": "req-abc-123" },
          contentType: "application/json",
          body: JSON.stringify({ status: 503, trace: "java.lang.NullPointerException at x.y.Z" }),
        })
      : route.continue(),
  );
  await page.goto("/student/journey");
  const panel = page.getByTestId("retry-panel");
  await expect(panel).toBeVisible();
  await expect(page.getByTestId("request-id")).toHaveText("req-abc-123");
  await expect(page.getByText("NullPointerException")).toHaveCount(0);

  failing = false;
  await panel.getByRole("button", { name: "Try again" }).click();
  await expect(page.getByRole("heading", { name: "My Journey" })).toBeVisible();
  await expect(page.getByTestId("retry-panel")).toHaveCount(0);
});

test("a network failure also shows the retry panel", async ({ page }) => {
  await signIn(page);
  await expect(page).toHaveURL(/\/student\/feed$/);
  await page.route("**/api/me", (route) => route.abort());
  await page.goto("/student/journey");
  await expect(page.getByTestId("retry-panel")).toContainText("We couldn't reach the server");
  await expect(page.getByTestId("request-id")).toHaveCount(0);
});

test("data pages show a skeleton while loading", async ({ page }) => {
  await signIn(page);
  await expect(page).toHaveURL(/\/student\/feed$/);
  await page.route("**/api/me", async (route) => {
    await new Promise((r) => setTimeout(r, 1500));
    await route.continue();
  });
  await page.goto("/student/journey");
  await expect(page.getByLabel("Loading")).toBeVisible();
  await expect(page.getByRole("heading", { name: "My Journey" })).toBeVisible();
});
