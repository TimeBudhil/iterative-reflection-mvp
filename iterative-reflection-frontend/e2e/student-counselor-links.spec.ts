import { expect, test, type Page } from "@playwright/test";

// Needs the dev backend. Each test signs up a fresh student; the real join code comes from the demo counselor.
const PASSWORD = "initial-password-123";

async function xsrf(page: Page) {
  await page.request.get("/api/me");
  return (await page.context().cookies()).find((c) => c.name === "XSRF-TOKEN")?.value ?? "";
}

async function signIn(page: Page, email: string, password: string) {
  await page.goto("/signin");
  await page.getByLabel("Email").fill(email);
  await page.getByLabel("Password").fill(password);
  await page.getByRole("button", { name: "Sign in", exact: true }).click();
  await expect(page).not.toHaveURL(/\/signin/, { timeout: 30_000 });
}

async function signUpFreshStudent(page: Page) {
  const email = `links-${Date.now()}-${Math.floor(Math.random() * 1e6)}@test.example`;
  const res = await page.request.post("/api/auth/signup", {
    headers: { "X-XSRF-TOKEN": await xsrf(page) },
    data: { email, password: PASSWORD, displayName: "Link Student", birthdate: "2008-01-01", acceptedTermsVersion: "1" },
  });
  expect(res.status()).toBe(201);
  return email;
}

/** Signs in as the demo counselor in an isolated context and reads the join code and name. */
async function demoCounselor(browser: import("@playwright/test").Browser, baseURL: string) {
  const ctx = await browser.newContext({ baseURL });
  const page = await ctx.newPage();
  const token = await xsrf(page);
  const signin = await page.request.post("/api/auth/signin", {
    headers: { "X-XSRF-TOKEN": token },
    data: { email: "counselor@demo.example", password: "demo" },
  });
  expect(signin.status()).toBe(200);
  const me = await (await page.request.get("/api/counselor/me")).json();
  await ctx.close();
  return me as { joinCode: string; displayName: string };
}

test("link by code, reject duplicate, then revoke through the confirm dialog", async ({ page, browser, baseURL }) => {
  const counselor = await demoCounselor(browser, baseURL!);
  const email = await signUpFreshStudent(page);
  await signIn(page, email, PASSWORD);
  await page.goto("/student/settings#my-counselors");
  const card = page.locator("#my-counselors");
  await expect(card).toBeVisible();
  await expect(card.getByText("You have not linked a counselor yet.")).toBeVisible();

  // Unknown code -> 404 message.
  await page.getByLabel("Join code").fill("ZZZZZZZZ");
  await page.getByRole("button", { name: "Link counselor" }).click();
  await expect(page.locator("#counselor-code-error")).toHaveText("Code not found.");

  // Real code (lowercase is fine) -> row with name and date.
  await page.getByLabel("Join code").fill(counselor.joinCode.toLowerCase());
  await page.getByRole("button", { name: "Link counselor" }).click();
  const row = card.getByTestId("counselor-link");
  await expect(row).toHaveCount(1);
  await expect(row).toContainText(counselor.displayName);
  await expect(row).toContainText(/Linked [A-Z][a-z]{2} \d{1,2}, \d{4}/);
  await expect(row).not.toContainText("Inactive");

  // Same code again -> 409 message.
  await page.getByLabel("Join code").fill(counselor.joinCode);
  await page.getByRole("button", { name: "Link counselor" }).click();
  await expect(page.locator("#counselor-code-error")).toHaveText("You're already linked to this counselor.");
  await expect(row).toHaveCount(1);

  // Revoke: cancel keeps the row, confirm removes it.
  await row.getByRole("button", { name: "Revoke" }).click();
  const dialog = page.getByRole("dialog");
  await expect(dialog).toContainText(`${counselor.displayName} will no longer see your reflections`);
  await dialog.getByRole("button", { name: "Cancel" }).click();
  await expect(row).toHaveCount(1);
  await row.getByRole("button", { name: "Revoke" }).click();
  await page.getByRole("dialog").getByRole("button", { name: "Revoke" }).click();
  await expect(row).toHaveCount(0);
  await expect(card.getByText("You have not linked a counselor yet.")).toBeVisible();
});

test("429 shows the throttle message (mocked, so the real limiter is untouched)", async ({ page }) => {
  const email = await signUpFreshStudent(page);
  await signIn(page, email, PASSWORD);
  await page.route("**/api/students/me/counselors", (route) =>
    route.request().method() === "POST"
      ? route.fulfill({
          status: 429,
          contentType: "application/problem+json",
          body: JSON.stringify({ title: "Too Many Requests", status: 429, detail: "Too many failed link attempts. Try again later." }),
        })
      : route.continue(),
  );
  await page.goto("/student/settings");
  await page.getByLabel("Join code").fill("ABCD2345");
  await page.getByRole("button", { name: "Link counselor" }).click();
  await expect(page.locator("#counselor-code-error")).toHaveText("Too many attempts. Try again in 15 minutes.");
});

test("an inactive counselor shows an Inactive badge (mocked list)", async ({ page }) => {
  const email = await signUpFreshStudent(page);
  await signIn(page, email, PASSWORD);
  await page.route("**/api/students/me/counselors?*", (route) =>
    route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({
        content: [{ id: 99, counselorId: 7, displayName: "Mr. Gone", counselorActive: false, linkedAt: "2026-11-01T10:00:00Z" }],
        page: { size: 100, number: 0, totalElements: 1, totalPages: 1 },
      }),
    }),
  );
  await page.goto("/student/settings");
  const row = page.getByTestId("counselor-link");
  await expect(row).toContainText("Mr. Gone");
  await expect(row).toContainText("Inactive");
  await expect(row).toContainText("Nov 1, 2026");
});
