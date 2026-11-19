import AxeBuilder from "@axe-core/playwright";
import { randomUUID } from "node:crypto";
import { expect, test, type Page } from "@playwright/test";
import { apiContext, seedLinkedStudent, send } from "./support/seed-student";
import { signIn } from "./support/helpers";

// N3.1 + N4.2: axe scan (serious/critical) and horizontal-scroll check on every page of the Page map, per role.
const WIDTHS = [1280, 1024, 768];

async function expectAccessible(page: Page, label: string) {
  await expect(page.locator("main, form, article").first()).toBeVisible();
  await expect(page.locator("[aria-busy='true']")).toHaveCount(0, { timeout: 20_000 });
  const { violations } = await new AxeBuilder({ page }).withTags(["wcag2a", "wcag2aa", "wcag21a", "wcag21aa"]).analyze();
  const bad = violations.filter((v) => v.impact === "serious" || v.impact === "critical");
  expect(
    bad.map((v) => `${label}: ${v.id} (${v.impact}) ${v.nodes.map((n) => n.target.join(" ")).slice(0, 3).join(" | ")}`),
  ).toEqual([]);
}

async function expectNoHorizontalScroll(page: Page, label: string) {
  for (const width of WIDTHS) {
    await page.setViewportSize({ width, height: 800 });
    const overflow = await page.evaluate(() => document.documentElement.scrollWidth - document.documentElement.clientWidth);
    expect(overflow, `${label} at ${width}px`).toBeLessThanOrEqual(0);
  }
  await page.setViewportSize({ width: 1280, height: 800 });
}

async function check(page: Page, label: string) {
  await expectAccessible(page, label);
  await expectNoHorizontalScroll(page, label);
}

async function sweep(page: Page, paths: string[]) {
  for (const path of paths) {
    await page.goto(path);
    await check(page, path);
  }
}

test.describe("public pages", () => {
  test("signin, signup, terms", async ({ page }) => {
    await sweep(page, ["/signin", "/signin?mode=signup", "/terms"]);
  });

  test("change-password (forced)", async ({ page }) => {
    const admin = await apiContext();
    await send(admin, "/api/auth/signin", { email: "admin@demo.example", password: "demo" });
    const email = `a11y+${randomUUID().slice(0, 8)}@example.com`;
    await send(admin, "/api/admin/counselors", { email, displayName: "A11y Counselor", temporaryPassword: "temporary-pass-1" });
    await signIn(page, email, "temporary-pass-1");
    await expect(page).toHaveURL(/\/change-password$/);
    await check(page, "/change-password");
  });
});

test.describe("student pages", () => {
  test("every student page", async ({ page }) => {
    const s = await seedLinkedStudent({ reflections: 3, abandon: false, intake: true });
    await signIn(page, s.email, s.password);
    await expect(page).toHaveURL(/\/student\/feed$/);
    await check(page, "/student/feed");
    await page.getByRole("link", { name: "Start", exact: true }).click();
    await expect(page).toHaveURL(/\/student\/careers\/\d+$/);
    const careerUrl = new URL(page.url()).pathname;
    await check(page, careerUrl);
    const indicator = page.getByTestId("page-indicator");
    const last = Number((await indicator.textContent())!.match(/of (\d+)/)![1]);
    for (let n = 2; n <= last; n++) await page.getByRole("button", { name: "Next", exact: true }).click();
    await check(page, `${careerUrl} (last page)`);
    await page.goto(`${careerUrl}/reflect`);
    await check(page, "reflect");
    await sweep(page, ["/student/journey", "/student/intake", "/student/settings"]);
  });
});

test.describe("counselor pages", () => {
  test("every counselor page", async ({ page }) => {
    const s = await seedLinkedStudent({ reflections: 2, abandon: false, intake: false });
    await signIn(page, "counselor@demo.example", "demo");
    await expect(page).toHaveURL(/\/counselor\/students$/);
    await sweep(page, ["/counselor/students", `/counselor/students/${s.id}`, "/counselor/settings"]);
  });
});

test.describe("admin pages", () => {
  test("every admin page and the add-counselor dialog", async ({ page }) => {
    await signIn(page, "admin@demo.example", "demo");
    await expect(page).toHaveURL(/\/admin\/metrics$/);
    await sweep(page, ["/admin/metrics", "/admin/counselors", "/admin/audit", "/admin/settings"]);
    await page.goto("/admin/counselors");
    await page.getByRole("button", { name: "Add counselor" }).click();
    await expect(page.getByRole("dialog")).toBeVisible();
    await expectAccessible(page, "add counselor dialog");
  });
});
