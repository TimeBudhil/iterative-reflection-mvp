import { expect, type APIRequestContext, type Page } from "@playwright/test";

/** Shared e2e helpers. Everything goes through the page's baseURL and the Next `/api` rewrite, never a fixed port. */

export const DEMO_PASSWORD = "demo";
export const STUDENT_PASSWORD = "correct-horse-battery";
export const TERMS_VERSION = "2026-11-18";

/**
 * Signs in through the sign-in form. `landing` optionally waits for the result: a URL pattern, or "away" for
 * "anywhere but /signin".
 */
export async function signIn(page: Page, email: string, password = DEMO_PASSWORD, landing?: RegExp | "away") {
  await page.goto("/signin");
  await page.getByLabel("Email").fill(email);
  await page.getByLabel("Password").fill(password);
  await page.getByRole("button", { name: "Sign in", exact: true }).click();
  if (landing === "away") await expect(page).not.toHaveURL(/\/signin/, { timeout: 30_000 });
  else if (landing) await expect(page).toHaveURL(landing);
}

/** Primes the XSRF-TOKEN cookie (GET /api/me re-issues it, e.g. after sign-in cleared it) and returns the header. */
export async function csrfHeaders(ctx: APIRequestContext): Promise<Record<string, string>> {
  await ctx.get("/api/me");
  const cookie = (await ctx.storageState()).cookies.find((k) => k.name === "XSRF-TOKEN");
  return { "X-XSRF-TOKEN": decodeURIComponent(cookie?.value ?? "") };
}

/** The current XSRF token of the page's cookie jar. */
export async function xsrf(page: Page): Promise<string> {
  return (await csrfHeaders(page.request))["X-XSRF-TOKEN"];
}

export type NewStudent = { prefix: string; displayName?: string; password?: string; birthdate?: string; termsVersion?: string };

/** Signs a unique student up through the API (no sign-in) and returns the email. */
export async function signUpStudent(page: Page, opts: NewStudent): Promise<string> {
  const email = `${opts.prefix}-${Date.now()}-${Math.random().toString(36).slice(2, 8)}@e2e.example`;
  const res = await page.request.post("/api/auth/signup", {
    headers: await csrfHeaders(page.request),
    data: {
      email,
      password: opts.password ?? STUDENT_PASSWORD,
      displayName: opts.displayName ?? "E2E Student",
      birthdate: opts.birthdate ?? "2000-01-01",
      acceptedTermsVersion: opts.termsVersion ?? TERMS_VERSION,
    },
  });
  expect(res.status()).toBe(201);
  return email;
}

/** Signs up and signs in a fresh student; resolves to the id of the career on their feed card. */
export async function freshStudent(page: Page, prefix: string, displayName = "E2E Student"): Promise<number> {
  const email = await signUpStudent(page, { prefix, displayName });
  await signIn(page, email, STUDENT_PASSWORD, /\/student\/feed$/);
  return Number((await page.getByRole("link", { name: "Start", exact: true }).getAttribute("href"))!.split("/").pop());
}
