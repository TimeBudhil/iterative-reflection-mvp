import { expect, test, type Page } from "@playwright/test";

// Needs the dev backend. Each test signs up a fresh student through the API.
const PASSWORD = "correct-horse-battery";

async function xsrf(page: Page) {
  return (await page.context().cookies()).find((c) => c.name === "XSRF-TOKEN")!.value;
}

async function freshStudent(page: Page) {
  const email = `abandon-${Date.now()}-${Math.random().toString(36).slice(2, 8)}@e2e.example`;
  await page.request.get("/api/me");
  const res = await page.request.post("/api/auth/signup", {
    headers: { "X-XSRF-TOKEN": await xsrf(page) },
    data: { email, password: PASSWORD, displayName: "Abandon Tester", birthdate: "2000-01-01", acceptedTermsVersion: "2026-11-18" },
  });
  expect(res.status()).toBe(201);
  await page.goto("/signin");
  await page.getByLabel("Email").fill(email);
  await page.getByLabel("Password").fill(PASSWORD);
  await page.getByRole("button", { name: "Sign in", exact: true }).click();
  await expect(page).toHaveURL(/\/student\/feed$/);
  return Number((await page.getByRole("link", { name: "Start", exact: true }).getAttribute("href"))!.split("/").pop());
}

test("the button shows on every page, cancel keeps the career, confirm abandons and lands on a different card", async ({ page }) => {
  const id = await freshStudent(page);
  await page.getByRole("link", { name: "Start", exact: true }).click();
  await expect(page).toHaveURL(new RegExp(`/student/careers/${id}$`), { timeout: 30_000 });

  const button = page.getByRole("button", { name: "This isn't for me" });
  const indicator = page.getByTestId("page-indicator");
  await expect(indicator).toHaveText(/^Page 1 of (\d+)$/);
  const last = Number((await indicator.textContent())!.match(/of (\d+)/)![1]);
  for (let n = 1; n <= last; n++) {
    await expect(indicator).toHaveText(`Page ${n} of ${last}`);
    await expect(button).toBeVisible();
    if (n < last) await page.getByRole("button", { name: "Next", exact: true }).click();
  }

  await button.click();
  const dialog = page.getByRole("dialog");
  await expect(dialog).toBeVisible();
  await dialog.getByRole("button", { name: "Keep reading" }).click();
  await expect(dialog).toBeHidden();
  await expect(page).toHaveURL(new RegExp(`/student/careers/${id}$`));

  const abandon = page.waitForResponse((r) => r.url().endsWith(`/api/careers/${id}/abandon`) && r.request().method() === "POST");
  await button.click();
  await dialog.getByRole("button", { name: "Yes, skip it" }).click();
  expect((await abandon).status()).toBe(204);

  await expect(page).toHaveURL(/\/student\/feed$/);
  await expect(page.getByRole("link", { name: "Start", exact: true })).not.toHaveAttribute("href", new RegExp(`/${id}$`));
});

test("a fresh student can abandon from page 1", async ({ page }) => {
  const id = await freshStudent(page);
  await page.goto(`/student/careers/${id}`);
  await expect(page.getByTestId("page-indicator")).toHaveText(/^Page 1 of/);
  await page.getByRole("button", { name: "This isn't for me" }).click();
  await page.getByRole("dialog").getByRole("button", { name: "Yes, skip it" }).click();
  await expect(page).toHaveURL(/\/student\/feed$/);
  await expect(page.getByRole("link", { name: "Start", exact: true })).not.toHaveAttribute("href", new RegExp(`/${id}$`));
});
