import { expect, test, type Page } from "@playwright/test";

// N5.2: every status a page can receive renders written copy, never the backend's `detail` text.
const LEAK = "RAWLEAK-do-not-render";
const STATUSES = [400, 404, 409, 429, 500, 503];

async function signIn(page: Page, email: string) {
  await page.context().clearCookies();
  await page.goto("/signin");
  await page.getByLabel("Email").fill(email);
  await page.getByLabel("Password").fill("demo");
  await page.getByRole("button", { name: "Sign in", exact: true }).click();
  await page.waitForURL((u) => !u.pathname.startsWith("/signin"));
}

/** Fails every /api call of the given methods except the session probe with a problem+json carrying the poison detail. */
async function failApi(page: Page, status: number, methods: string[]) {
  await page.unroute("**/api/**");
  await page.route("**/api/**", (route) => {
    const req = route.request();
    const path = new URL(req.url()).pathname;
    if (path === "/api/me" || !methods.includes(req.method())) return route.continue();
    return route.fulfill({
      status,
      contentType: "application/problem+json",
      headers: { "X-Request-Id": "req-test" },
      body: JSON.stringify({ title: LEAK, detail: LEAK, status }),
    });
  });
}

const pages: Record<string, string[]> = {
  "student1@demo.example": ["/student/feed", "/student/journey", "/student/settings", "/student/intake", "/student/careers/1", "/student/careers/1/reflect"],
  "counselor@demo.example": ["/counselor", "/counselor/students", "/counselor/students/3", "/counselor/settings"],
  "admin@demo.example": ["/admin", "/admin/counselors", "/admin/metrics", "/admin/audit", "/admin/settings"],
};

for (const [email, paths] of Object.entries(pages)) {
  test(`${email} pages never render raw backend text on read failures`, async ({ page }) => {
    test.setTimeout(180_000);
    await signIn(page, email);
    for (const status of STATUSES) {
      await failApi(page, status, ["GET"]);
      for (const path of paths) {
        await page.goto(path);
        await page.waitForTimeout(400);
        const text = await page.locator("body").innerText();
        expect(text, `${path} @ ${status}`).not.toContain(LEAK);
        expect(text.trim().length, `${path} @ ${status} is blank`).toBeGreaterThan(0);
      }
    }
  });
}

test("a failed password change shows written copy only", async ({ page }) => {
  await signIn(page, "student1@demo.example");
  for (const status of [400, 409, 429, 500]) {
    await failApi(page, status, ["PUT", "POST", "PATCH", "DELETE"]);
    await page.goto("/student/settings");
    await page.getByLabel("Current password").fill("demo");
    await page.getByLabel("New password", { exact: true }).fill("a-long-enough-password");
    await page.getByLabel("Confirm new password").fill("a-long-enough-password");
    await page.getByRole("button", { name: /change password/i }).click();
    await page.waitForTimeout(500);
    expect(await page.locator("body").innerText(), `status ${status}`).not.toContain(LEAK);
  }
});

test("a failed intake save shows written copy only", async ({ page }) => {
  await signIn(page, "student1@demo.example");
  for (const status of [400, 409, 429, 500]) {
    await failApi(page, status, ["PUT", "POST"]);
    await page.goto("/student/intake");
    await page.locator("fieldset textarea").first().fill("I like building things.");
    await page.getByRole("button", { name: /save (interests|changes)/i }).click();
    await expect(page.getByRole("alert").first()).toBeVisible();
    await page.waitForTimeout(500);
    expect(await page.locator("body").innerText(), `status ${status}`).not.toContain(LEAK);
  }
});
