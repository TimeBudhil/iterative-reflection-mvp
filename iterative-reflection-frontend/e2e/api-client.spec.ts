import { expect, test } from "@playwright/test";

// Needs the backend running at BACKEND_URL (see README); dev demo accounts use password "demo".
test("debug page shows the anonymous /api/me result as a 401 ApiError", async ({ page }) => {
  await page.goto("/debug/me");
  await expect(page.getByTestId("me-result")).toContainText('"status":401');
});

test("from a clean browser, a first mutation primes CSRF itself and signs in", async ({ page }) => {
  await page.goto("/debug/me");
  await expect(page.getByTestId("me-result")).toContainText('"status":401');
  await page.context().clearCookies();
  await page.getByRole("button", { name: "Sign in as demo admin" }).click();
  await expect(page.getByTestId("me-result")).toContainText("admin@demo.example");
});
