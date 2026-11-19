import { expect, test, type Page } from "@playwright/test";
import { seedLinkedStudent } from "./support/seed-student";

// N3.3: field errors are tied to their input via aria-describedby and announced; N4.2: the sidebar collapses.

/** Every invalid input must point (aria-describedby) at non-empty text that is announced (role=alert). */
async function expectErrorsWired(page: Page, minInvalid = 1) {
  const invalid = page.locator('[aria-invalid="true"]');
  await expect(invalid.first()).toBeVisible();
  const n = await invalid.count();
  expect(n).toBeGreaterThanOrEqual(minInvalid);
  for (let i = 0; i < n; i++) {
    const info = await invalid.nth(i).evaluate((el) => {
      const ids = (el.getAttribute("aria-describedby") ?? "").split(/\s+/).filter(Boolean);
      const targets = ids.map((id) => document.getElementById(id)).filter((t): t is HTMLElement => !!t);
      return {
        resolved: targets.length === ids.length && ids.length > 0,
        announced: targets.some((t) => t.getAttribute("role") === "alert" && (t.textContent ?? "").trim().length > 0),
      };
    });
    expect(info, `invalid field #${i}`).toEqual({ resolved: true, announced: true });
  }
}

test("sign-up field errors are described and announced", async ({ page }) => {
  await page.goto("/signin?mode=signup");
  await page.getByLabel("Password").fill("short");
  await page.getByLabel(/I agree to the/).check();
  await page.getByRole("button", { name: "Create account" }).click();
  await expectErrorsWired(page, 3);
});

test("settings profile error is described and announced", async ({ page }) => {
  const s = await seedLinkedStudent({ reflections: 0, abandon: false, intake: false });
  await page.goto("/signin");
  await page.getByLabel("Email").fill(s.email);
  await page.getByLabel("Password").fill(s.password);
  await page.getByRole("button", { name: "Sign in", exact: true }).click();
  await expect(page).toHaveURL(/\/student\/feed$/);
  await page.goto("/student/settings");
  await page.getByLabel("Display name").fill("");
  await page.getByRole("button", { name: /save/i }).first().click();
  await expectErrorsWired(page);
});

test("the sidebar collapses behind a menu button at 1024 and 768 px", async ({ page }) => {
  await page.goto("/signin");
  await page.getByLabel("Email").fill("counselor@demo.example");
  await page.getByLabel("Password").fill("demo");
  await page.getByRole("button", { name: "Sign in", exact: true }).click();
  await expect(page).toHaveURL(/\/counselor\/students$/);
  for (const width of [1280, 1024, 768]) {
    await page.setViewportSize({ width, height: 800 });
    const menu = page.getByRole("button", { name: "Menu" });
    const nav = page.getByRole("navigation", { name: "Main" });
    if (width >= 1280) {
      await expect(menu).toBeHidden();
      await expect(nav).toBeVisible();
      continue;
    }
    await expect(menu).toBeVisible();
    await expect(nav).toBeHidden();
    await menu.focus();
    await page.keyboard.press("Enter");
    await expect(menu).toHaveAttribute("aria-expanded", "true");
    await page.getByRole("link", { name: "Settings", exact: true }).click();
    await expect(page).toHaveURL(/\/counselor\/settings$/);
    await expect(nav).toBeHidden();
    await page.getByRole("button", { name: "Menu" }).click();
    await page.getByRole("link", { name: "Students", exact: true }).click();
    await expect(page).toHaveURL(/\/counselor\/students$/);
  }
});
