import { expect, test, type Page } from "@playwright/test";
import { resetThemeToGrove, signIn as signInTo, signUpStudent } from "./support/helpers";

// Needs the dev backend. Student flows use a freshly signed-up user so demo passwords never change.
const NEW_PASSWORD = "a-brand-new-password-1";
const PASSWORD = "initial-password-123";

const signUpFreshStudent = (page: Page) =>
  signUpStudent(page, { prefix: "settings", displayName: "Fresh Student", password: PASSWORD, birthdate: "2008-01-01", termsVersion: "1" });

const signIn = (page: Page, email: string, password: string) => signInTo(page, email, password, "away");

const ROLES = [
  { role: "student", email: "student1@demo.example", path: "/student/settings" },
  { role: "counselor", email: "counselor@demo.example", path: "/counselor/settings" },
  { role: "admin", email: "admin@demo.example", path: "/admin/settings" },
];

for (const r of ROLES) {
  test(`${r.role} settings shows Profile and Password cards reachable by anchor`, async ({ page }) => {
    await signIn(page, r.email, "demo");
    for (const anchor of ["profile", "password"]) {
      await page.goto(`${r.path}#${anchor}`);
      const card = page.locator(`#${anchor}`);
      await expect(card).toBeVisible();
      await expect(card).toBeInViewport();
    }
    await expect(page.getByLabel("Email", { exact: true })).toHaveValue(r.email);
    await expect(page.getByLabel("Email", { exact: true })).toBeDisabled();
  });
}

test("changing the display name updates the user menu without a reload", async ({ page }) => {
  const email = await signUpFreshStudent(page);
  await signIn(page, email, PASSWORD);
  await page.goto("/student/settings");
  await page.evaluate(() => ((window as unknown as { __m: boolean }).__m = true));
  await page.getByLabel("Display name").fill("Renamed Student");
  await page.getByRole("button", { name: "Save" }).click();
  await expect(page.getByTestId("user-menu-name")).toHaveText("Renamed Student");
  await expect(page.getByText("Display name updated.")).toBeVisible();
  expect(await page.evaluate(() => (window as unknown as { __m?: boolean }).__m)).toBe(true);
});

test("password change: mismatch blocked, wrong current on that field, success toasts and stays signed in", async ({ page }) => {
  const email = await signUpFreshStudent(page);
  await signIn(page, email, PASSWORD);
  await page.goto("/student/settings");

  let puts = 0;
  page.on("request", (req) => req.method() === "PUT" && req.url().includes("/api/me/password") && puts++);

  await page.getByLabel("Current password").fill(PASSWORD);
  await page.getByLabel("New password", { exact: true }).fill(NEW_PASSWORD);
  await page.getByLabel("Confirm new password").fill("something-else-entirely");
  await page.getByRole("button", { name: "Change password" }).click();
  await expect(page.getByText("Passwords don't match.")).toBeVisible();
  expect(puts).toBe(0);

  await page.getByLabel("Confirm new password").fill(NEW_PASSWORD);
  await page.getByLabel("Current password").fill("wrong-current-password");
  await page.getByRole("button", { name: "Change password" }).click();
  await expect(page.locator("#current-password-error")).toHaveText("Current password is incorrect.");

  await page.getByLabel("Current password").fill(PASSWORD);
  await page.getByRole("button", { name: "Change password" }).click();
  await expect(page.getByText("Password changed.")).toBeVisible();
  await expect(page).toHaveURL(/\/student\/settings$/);
  await page.reload();
  await expect(page.getByRole("heading", { name: "Settings" })).toBeVisible();

  // The new password works for a fresh sign-in.
  await page.context().clearCookies();
  await signIn(page, email, NEW_PASSWORD);
  await expect(page).toHaveURL(/\/student\/feed$/);
});

// App theme card (update 2): the theme is one global setting, so these run serially and reset to Grove afterwards.
test.describe("app theme card", () => {
  test.describe.configure({ mode: "serial" });
  const html = (page: Page) => page.locator("html");
  const ADMIN = "admin@demo.example";

  test.afterAll(async ({ browser }) => {
    await resetThemeToGrove(browser);
  });

  test("students and counselors have no theme card", async ({ page }) => {
    for (const r of ROLES.filter((x) => x.role !== "admin")) {
      await signIn(page, r.email, "demo");
      await page.goto(r.path);
      await expect(page.getByRole("heading", { name: "Settings" })).toBeVisible();
      await expect(page.getByRole("radio")).toHaveCount(0);
      await expect(page.getByText("App theme")).toHaveCount(0);
      await page.context().clearCookies();
    }
  });

  test("admin previews, saves and re-themes without a reload; a signed-out visitor then gets it", async ({ page, browser }) => {
    await resetThemeToGrove(browser);
    await signIn(page, ADMIN, "demo");
    await page.goto("/admin/settings");

    const card = page.locator("#app-theme");
    await expect(card).toBeVisible();
    // Card sits after Password.
    const ids = await page.locator("main [id]").evaluateAll((els) => els.map((e) => e.id));
    expect(ids.indexOf("app-theme")).toBeGreaterThan(ids.indexOf("password"));

    await expect(card.getByRole("radio")).toHaveCount(4);
    for (const name of ["Grove", "Harbor", "Plum", "Slate"]) await expect(card.getByRole("radio", { name })).toBeVisible();
    await expect(card.getByRole("radio", { name: "Grove" })).toBeChecked();
    const save = card.getByRole("button", { name: "Save" });
    await expect(save).toBeDisabled();
    await expect(card.getByTestId("theme-last-changed")).toHaveCount(0);
    // Swatches and mock cards are hidden from assistive tech.
    await expect(card.locator("[data-testid^=mock-][aria-hidden=true]")).toHaveCount(4);

    // Preview only: the mock cards change, the page does not.
    await card.getByRole("radio", { name: "Harbor" }).check();
    await expect(html(page)).toHaveAttribute("data-theme", "grove");
    await expect(save).toBeEnabled();

    await page.evaluate(() => ((window as unknown as { __m: boolean }).__m = true));
    await save.click();
    await expect(page.getByText("Theme saved.")).toBeVisible();
    await expect(html(page)).toHaveAttribute("data-theme", "harbor");
    await expect(save).toBeFocused();
    await expect(save).toBeDisabled();
    await expect(card.getByTestId("theme-last-changed")).toContainText("Last changed by");
    expect(await page.evaluate(() => (window as unknown as { __m?: boolean }).__m)).toBe(true);

    // After a reload the saved preset is preselected.
    await page.reload();
    await expect(page.locator("#app-theme").getByRole("radio", { name: "Harbor" })).toBeChecked();

    // A fresh signed-out context sees Harbor on /signin.
    const anon = await browser.newContext();
    const anonPage = await anon.newPage();
    await anonPage.goto("/signin");
    await expect(html(anonPage)).toHaveAttribute("data-theme", "harbor");
    await anon.close();
  });

  test("keyboard: arrows move between presets and Space selects", async ({ page, browser }) => {
    await resetThemeToGrove(browser);
    await signIn(page, ADMIN, "demo");
    await page.goto("/admin/settings");
    const card = page.locator("#app-theme");
    await card.getByRole("radio", { name: "Grove" }).focus();
    await page.keyboard.press("ArrowDown");
    await expect(card.getByRole("radio", { name: "Harbor" })).toBeFocused();
    await expect(card.getByRole("radio", { name: "Harbor" })).toBeChecked();
    await page.keyboard.press("ArrowDown");
    await expect(card.getByRole("radio", { name: "Plum" })).toBeChecked();
    await page.keyboard.press("ArrowUp");
    await page.keyboard.press("ArrowUp");
    await expect(card.getByRole("radio", { name: "Grove" })).toBeChecked();
    await page.keyboard.press("ArrowRight");
    await page.keyboard.press("Space");
    await expect(card.getByRole("radio", { name: "Harbor" })).toBeChecked();
  });

  test("a failed save toasts the standard copy, reverts the radio and leaves the page theme alone", async ({ page, browser }) => {
    await resetThemeToGrove(browser);
    await signIn(page, ADMIN, "demo");
    await page.goto("/admin/settings");
    await page.route("**/api/admin/settings/theme", (route) =>
      route.request().method() === "PUT" ? route.fulfill({ status: 500, contentType: "application/problem+json", body: "{}" }) : route.continue(),
    );
    const card = page.locator("#app-theme");
    await card.getByRole("radio", { name: "Plum" }).check();
    await card.getByRole("button", { name: "Save" }).click();
    await expect(page.getByText("Something went wrong on our side. Please try again.")).toBeVisible();
    await expect(card.getByRole("radio", { name: "Grove" })).toBeChecked();
    await expect(html(page)).toHaveAttribute("data-theme", "grove");
  });
});
