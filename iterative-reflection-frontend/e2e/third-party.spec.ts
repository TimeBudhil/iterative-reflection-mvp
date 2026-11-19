import { expect, test, type Page } from "@playwright/test";
import { DEMO_PASSWORD, signIn as signInTo } from "./support/helpers";

// N2.6 / X4: every script, font and stylesheet the app loads comes from its own origin. Iframes on experience
// pages (the allowlisted embeds) are the only allowed third-party content, and their inner requests are skipped.

async function signIn(page: Page, email: string) {
  await page.context().clearCookies();
  await signInTo(page, email, DEMO_PASSWORD, "away");
}

function watch(page: Page, baseURL: string | undefined) {
  const base = new URL(baseURL!);
  const offenders: string[] = [];
  let seen = 0;
  page.on("request", (r) => {
    if (r.frame().parentFrame()) return; // inside an embed iframe
    const type = r.resourceType();
    if (type !== "script" && type !== "font" && type !== "stylesheet") return;
    seen++;
    const url = new URL(r.url());
    if (url.origin !== base.origin) offenders.push(`${type} ${r.url()}`);
  });
  return { offenders, count: () => seen };
}

async function walk(page: Page, paths: string[]) {
  for (const p of paths) {
    await page.goto(p);
    await page.waitForLoadState("networkidle");
  }
}

test("public pages load no third-party scripts, fonts or styles", async ({ page, baseURL }) => {
  const w = watch(page, baseURL);
  await page.context().clearCookies();
  await walk(page, ["/signin", "/signin?mode=signup", "/terms"]);
  expect(w.count()).toBeGreaterThan(0);
  expect(w.offenders).toEqual([]);
});

test("student pages load no third-party scripts, fonts or styles", async ({ page, baseURL }) => {
  const w = watch(page, baseURL);
  await signIn(page, "student1@demo.example");
  await walk(page, ["/student/feed", "/student/journey", "/student/settings", "/student/intake"]);
  await page.goto("/student/feed");
  const href = await page.getByRole("link", { name: /^(Start|Continue)/ }).getAttribute("href");
  if (href) await walk(page, [href]);
  expect(w.offenders).toEqual([]);
});

test("counselor pages load no third-party scripts, fonts or styles", async ({ page, baseURL }) => {
  const w = watch(page, baseURL);
  await signIn(page, "counselor@demo.example");
  await walk(page, ["/counselor", "/counselor/students", "/counselor/settings"]);
  expect(w.offenders).toEqual([]);
});

test("admin pages load no third-party scripts, fonts or styles", async ({ page, baseURL }) => {
  const w = watch(page, baseURL);
  await signIn(page, "admin@demo.example");
  await walk(page, ["/admin", "/admin/counselors", "/admin/metrics", "/admin/audit", "/admin/settings"]);
  expect(w.offenders).toEqual([]);
});
