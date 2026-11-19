import { expect, test, type APIRequestContext } from "@playwright/test";
import { csrfHeaders, signIn } from "./support/helpers";

// Needs the dev backend (demo admin admin@demo.example / "demo"). Talks to it through the Next `/api` rewrite.
const TEMP = "temporary-pass-1";

/** Creates a counselor (mustChangePassword: true) through the admin API. */
async function createCounselor(request: APIRequestContext): Promise<string> {
  const email = `e2e+${Date.now()}-${Math.floor(Math.random() * 1e6)}@example.com`;
  let res = await request.post("/api/auth/signin", {
    data: { email: "admin@demo.example", password: "demo" },
    headers: await csrfHeaders(request),
  });
  expect(res.ok()).toBeTruthy();
  res = await request.post("/api/admin/counselors", {
    data: { email, displayName: "E2E Counselor", temporaryPassword: TEMP },
    headers: await csrfHeaders(request), // signin cleared the CSRF cookie, so re-prime
  });
  expect(res.status()).toBe(201);
  return email;
}

test("a user on a temporary password is held at /change-password until they pick a new one", async ({ page, request }) => {
  const email = await createCounselor(request);

  await signIn(page, email, TEMP);
  await expect(page).toHaveURL(/\/change-password$/);

  // Every other route redirects back.
  for (const path of ["/counselor/students", "/student/feed", "/admin/metrics", "/signin", "/"]) {
    await page.goto(path);
    await expect(page).toHaveURL(/\/change-password$/);
  }

  const submit = page.getByRole("button", { name: "Change password" });
  const current = page.getByLabel("Current password");
  const next = page.getByLabel("New password", { exact: true });
  const confirm = page.getByLabel("Confirm new password");

  // Client-side rules: 12+ chars and matching confirm.
  await current.fill(TEMP);
  await next.fill("short");
  await confirm.fill("different");
  await submit.click();
  await expect(page.getByText("Use at least 12 characters.")).toBeVisible();
  await expect(page.getByText("Passwords don't match.")).toBeVisible();

  // A server-side 400 (wrong current password) shows on its field.
  await current.fill("not-the-temp-password");
  await next.fill("a-brand-new-password");
  await confirm.fill("a-brand-new-password");
  await submit.click();
  await expect(page.getByText("Current password is incorrect.")).toBeVisible();
  await expect(page).toHaveURL(/\/change-password$/);

  // Success lands on the role home, and the guard no longer interferes (CSRF cookie was cleared by the PUT).
  await current.fill(TEMP);
  await submit.click();
  await expect(page).toHaveURL(/\/counselor\/students$/);
  await page.goto("/change-password");
  await expect(page).toHaveURL(/\/counselor\/students$/);
});

test("sign out still works from /change-password", async ({ page, request }) => {
  const email = await createCounselor(request);

  await signIn(page, email, TEMP);
  await expect(page).toHaveURL(/\/change-password$/);
  await page.getByRole("button", { name: "Sign out" }).click();
  await expect(page).toHaveURL(/\/signin$/);
  await page.goto("/counselor/students");
  await expect(page).toHaveURL(/\/signin(\?|$)/); // ticket 04 appends ?next=...&expired=1
});
