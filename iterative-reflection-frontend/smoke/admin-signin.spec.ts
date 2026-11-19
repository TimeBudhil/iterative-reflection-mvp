import { expect, test } from "@playwright/test";

// Run against the compose rehearsal: `pnpm e2e:smoke`. Uses the bootstrap admin from deploy/.env
// (ADMIN_EMAIL / ADMIN_PASSWORD), which the prod backend creates at startup (no demo accounts in prod).
const email = process.env.SMOKE_ADMIN_EMAIL ?? process.env.ADMIN_EMAIL;
const password = process.env.SMOKE_ADMIN_PASSWORD ?? process.env.ADMIN_PASSWORD;

test("the bootstrap admin signs in over HTTPS and reaches /admin", async ({ page, context, baseURL }) => {
  expect(email, "set ADMIN_EMAIL in deploy/.env").toBeTruthy();
  expect(password, "set ADMIN_PASSWORD in deploy/.env").toBeTruthy();

  await page.goto("/signin");
  await page.getByLabel("Email").fill(email!);
  await page.getByLabel("Password").fill(password!);
  await page.getByRole("button", { name: "Sign in", exact: true }).click();

  // /admin redirects to the metrics home for an admin.
  await expect(page).toHaveURL(/\/admin(\/metrics)?$/);
  await expect(page.getByRole("navigation", { name: "Main" }).getByRole("link", { name: "Counselors" })).toBeVisible();

  // Secure session cookie (prod profile) and CSRF cookie are set and the session survives a reload.
  const cookies = await context.cookies();
  expect(cookies.find((c) => c.name === "SESSION")).toMatchObject({ secure: baseURL!.startsWith("https:"), httpOnly: true });
  await page.reload();
  await expect(page).toHaveURL(/\/admin(\/metrics)?$/);
});
