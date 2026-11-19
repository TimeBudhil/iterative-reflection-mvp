import { expect, test } from "@playwright/test";

// Needs the backend running at BACKEND_URL (see README); dev demo accounts use password "demo".
test("from a clean browser, the first mutation primes CSRF itself and signs in", async ({ page }) => {
  await page.goto("/signin");
  await expect(page.getByRole("button", { name: "Sign in", exact: true })).toBeVisible();
  await page.context().clearCookies();
  await page.getByLabel("Email").fill("admin@demo.example");
  await page.getByLabel("Password").fill("demo");
  await page.getByRole("button", { name: "Sign in", exact: true }).click();
  await expect(page).toHaveURL(/\/admin\/metrics$/);
});
