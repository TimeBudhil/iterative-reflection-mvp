import { expect, test, type Locator, type Page } from "@playwright/test";
import { counselorContext, seedLinkedStudent } from "./support/seed-student";
import { signIn } from "./support/helpers";

// Per-role happy paths through the UI only, against the dev backend (N6.2). Emails are unique per run.
const PASSWORD = "correct-horse-battery";
const TEMP = "temporary-pass-1";
const stamp = `${Date.now()}-${Math.floor(Math.random() * 1e6)}`;
const student = { email: `e2e+student-${stamp}@example.com`, name: `E2E Student ${stamp}` };

/** One trip round the loop: Start, Next to the last page, Reflect, answer, Submit. Nothing else is clicked. */
async function reflectOnce(page: Page, verdict: "YES" | "MAYBE" | "NO") {
  await page.getByRole("link", { name: "Start", exact: true }).click();
  await expect(page).toHaveURL(/\/student\/careers\/\d+$/, { timeout: 30_000 });
  const indicator = page.getByTestId("page-indicator");
  await expect(indicator).toHaveText(/^Page 1 of \d+$/);
  const last = Number((await indicator.textContent())!.match(/of (\d+)/)![1]);
  for (let n = 2; n <= last; n++) {
    await page.getByRole("button", { name: "Next", exact: true }).click();
    await expect(indicator).toHaveText(`Page ${n} of ${last}`);
  }
  // N5.1: no dialog or extra step stands between the last page and the reflection form.
  await expect(page.getByRole("dialog")).toHaveCount(0);
  await page.getByRole("link", { name: "Reflect on this career" }).click();
  await expect(page).toHaveURL(/\/student\/careers\/\d+\/reflect$/);
  for (const key of ["interest", "values", "lifestyle", "responsibilities", "sacrifices"]) {
    await page.getByTestId(`question-${key}`).getByText("7", { exact: true }).click();
  }
  await page.getByText(verdict, { exact: true }).click();
  await page.getByRole("button", { name: "Submit reflection" }).click();
  await expect(page).toHaveURL(/\/student\/feed$/);
}

test.describe.serial("per-role happy paths", () => {
  test("student signs up, reflects x3, handles the intake modal and sees YES/MAYBE in My Journey", async ({ page }) => {
    const { joinCode } = await (await (await counselorContext()).get("/api/counselor/me")).json();
    await page.context().clearCookies();
    await page.goto("/signin?mode=signup");
    await page.getByLabel("Email").fill(student.email);
    await page.getByLabel("Password").fill(PASSWORD);
    await page.getByLabel("Display name").fill(student.name);
    await page.getByLabel("Birthdate").fill("2005-05-05");
    await page.getByLabel("Counselor code (optional)").fill(joinCode);
    await page.getByLabel(/I agree to the/).check();
    await page.getByRole("button", { name: "Create account" }).click();
    await expect(page).toHaveURL(/\/student\/feed$/, { timeout: 30_000 });

    const calls: string[] = [];
    page.on("request", (r) => {
      const p = new URL(r.url()).pathname;
      if (r.method() !== "GET" && p.startsWith("/api/")) calls.push(`${r.method()} ${p.replace(/\d+/g, "N")}`);
    });

    await reflectOnce(page, "YES");
    await expect(page.getByTestId("intake-prompt")).toHaveCount(0);
    await reflectOnce(page, "MAYBE");
    await expect(page.getByTestId("intake-prompt")).toHaveCount(0);
    // N5.1: the only writes were page views and the reflection, never anything else.
    expect([...new Set(calls)].sort()).toEqual(["POST /api/careers/N/pages/N/viewed", "POST /api/careers/N/reflection"]);
    await reflectOnce(page, "NO");

    // The third reflection opens the intake prompt.
    await expect(page.getByTestId("intake-prompt")).toBeVisible();
    await page.getByRole("button", { name: "Fill it in" }).click();
    await expect(page).toHaveURL(/\/student\/intake$/);
    await page.getByRole("button", { name: "Health & Medicine" }).click();
    await page.getByRole("button", { name: "Save interests" }).click();
    await expect(page).toHaveURL(/\/student\/feed$/);

    await page.goto("/student/journey");
    const rows = page.getByTestId("journey-row");
    await expect(rows).toHaveCount(2); // Saved = YES + MAYBE
    await expect(page.getByTestId("stat-total")).toContainText("3");
    await expect(rows.filter({ hasText: "YES" })).toHaveCount(1);
    await expect(rows.filter({ hasText: "MAYBE" })).toHaveCount(1);
  });

  test("the linked demo counselor views that student's detail", async ({ page }) => {
    await signIn(page, "counselor@demo.example", "demo");
    await expect(page).toHaveURL(/\/counselor\/students$/);
    await page.getByRole("link", { name: student.name }).click();
    await expect(page).toHaveURL(/\/counselor\/students\/\d+$/);
    await expect(page.getByRole("heading", { level: 1, name: student.name })).toBeVisible();
    await expect(page.getByTestId("reflection")).toHaveCount(3);
    await expect(page.getByRole("region", { name: "Top interests" })).toBeVisible();
  });

  test("an admin creates a counselor whose first sign-in forces a password change", async ({ page, browser }) => {
    const email = `e2e+counselor-${stamp}@example.com`;
    const name = `E2E Counselor ${stamp}`;
    await signIn(page, "admin@demo.example", "demo");
    await expect(page).toHaveURL(/\/admin\/metrics$/);
    await page.goto("/admin/counselors");
    await page.getByRole("button", { name: "Add counselor" }).click();
    const dialog = page.getByRole("dialog");
    await dialog.getByLabel("Email").fill(email);
    await dialog.getByLabel("Display name").fill(name);
    await dialog.getByLabel("Temporary password").fill(TEMP);
    await dialog.getByRole("button", { name: "Create counselor" }).click();
    await dialog.getByRole("button", { name: "Done" }).click();
    await expect(page.getByRole("table", { name: "Counselors" }).getByRole("row", { name: new RegExp(name) })).toBeVisible();

    const other = await (await browser.newContext()).newPage();
    await signIn(other, email, TEMP);
    await expect(other).toHaveURL(/\/change-password$/);
    await other.getByLabel("Current password").fill(TEMP);
    await other.getByLabel("New password", { exact: true }).fill("a-brand-new-password");
    await other.getByLabel("Confirm new password").fill("a-brand-new-password");
    await other.getByRole("button", { name: "Change password" }).click();
    await expect(other).toHaveURL(/\/counselor\/students$/);
    await other.context().close();
  });
});

// N3.2: the same journeys with the keyboard only (Tab / Shift+Tab / Enter / Space / arrows / Escape). No click(), no fill().

/** Tabs forward until `target` has focus. */
async function tabTo(page: Page, target: Locator, max = 80) {
  for (let i = 0; i < max; i++) {
    if (await target.evaluate((el) => el === document.activeElement)) return;
    await page.keyboard.press("Tab");
  }
  throw new Error("never focused the target by Tab");
}

async function typeInto(page: Page, field: Locator, text: string) {
  await tabTo(page, field);
  await page.keyboard.type(text);
}

async function keyboardSignIn(page: Page, email: string, password: string) {
  await page.goto("/signin");
  await typeInto(page, page.getByLabel("Email"), email);
  await typeInto(page, page.getByLabel("Password"), password);
  await page.keyboard.press("Enter");
}

/** Focus is visible when the focused element draws an outline or a box-shadow ring (or its label does). */
async function expectFocusVisible(page: Page) {
  const visible = await page.evaluate(() => {
    const draws = (el: Element | null) => {
      if (!el) return false;
      const cs = getComputedStyle(el);
      return (cs.outlineStyle !== "none" && parseFloat(cs.outlineWidth) > 0) || cs.boxShadow !== "none";
    };
    const el = document.activeElement;
    return draws(el) || draws(el?.closest("label") ?? null);
  });
  expect(visible).toBe(true);
}

async function expectFocusInside(page: Page, container: Locator) {
  expect(await container.evaluate((el) => el.contains(document.activeElement))).toBe(true);
}

test.describe("keyboard-only happy paths", () => {
  test("student: read, reflect with the 1-10 scale, verdict and submit; the abandon dialog traps focus", async ({ page }) => {
    const s = await seedLinkedStudent({ reflections: 0, abandon: false, intake: false });
    await keyboardSignIn(page, s.email, s.password);
    await expect(page).toHaveURL(/\/student\/feed$/, { timeout: 30_000 });

    // Abandon dialog first: open, focus is trapped, Escape closes and returns focus to its trigger.
    await tabTo(page, page.getByRole("link", { name: "Start", exact: true }));
    await expectFocusVisible(page);
    await page.keyboard.press("Enter");
    await expect(page).toHaveURL(/\/student\/careers\/\d+$/, { timeout: 30_000 });
    const trigger = page.getByRole("button", { name: "This isn't for me" });
    await tabTo(page, trigger);
    await page.keyboard.press("Enter");
    const dialog = page.getByRole("dialog");
    await expect(dialog).toBeVisible();
    for (let i = 0; i < 8; i++) {
      await page.keyboard.press("Tab");
      await expectFocusInside(page, dialog);
    }
    for (let i = 0; i < 3; i++) {
      await page.keyboard.press("Shift+Tab");
      await expectFocusInside(page, dialog);
    }
    await expectFocusVisible(page);
    await page.keyboard.press("Escape");
    await expect(dialog).toHaveCount(0);
    await expect(trigger).toBeFocused();

    // Read to the last page with Next + Enter.
    const indicator = page.getByTestId("page-indicator");
    const last = Number((await indicator.textContent())!.match(/of (\d+)/)![1]);
    for (let n = 2; n <= last; n++) {
      await tabTo(page, page.getByRole("button", { name: "Next", exact: true }));
      await page.keyboard.press("Enter");
      await expect(indicator).toHaveText(`Page ${n} of ${last}`);
    }
    await tabTo(page, page.getByRole("link", { name: "Reflect on this career" }));
    await page.keyboard.press("Enter");
    await expect(page).toHaveURL(/\/reflect$/);

    // Each 1-10 scale: Tab into the group (lands on 1), ArrowRight x6 selects 7.
    for (const key of ["interest", "values", "lifestyle", "responsibilities", "sacrifices"]) {
      const radios = page.getByTestId(`question-${key}`).getByRole("radio");
      await tabTo(page, radios.first());
      await expectFocusVisible(page);
      for (let i = 0; i < 6; i++) await page.keyboard.press("ArrowRight");
      await expect(radios.nth(6)).toBeChecked();
    }
    await tabTo(page, page.getByRole("radio", { name: "YES", exact: true }));
    await page.keyboard.press("ArrowRight");
    await expect(page.getByRole("radio", { name: "MAYBE", exact: true })).toBeChecked();
    await tabTo(page, page.getByRole("button", { name: "Submit reflection" }));
    await page.keyboard.press("Enter");
    await expect(page).toHaveURL(/\/student\/feed$/);
  });

  test("counselor: sign in and open a student's detail", async ({ page }) => {
    const s = await seedLinkedStudent({ reflections: 1, abandon: false, intake: false });
    await keyboardSignIn(page, "counselor@demo.example", "demo");
    await expect(page).toHaveURL(/\/counselor\/students$/);
    await tabTo(page, page.getByRole("link", { name: s.displayName }));
    await expectFocusVisible(page);
    await page.keyboard.press("Enter");
    await expect(page.getByRole("heading", { level: 1, name: s.displayName })).toBeVisible();
  });

  test("admin: add-counselor dialog opens, traps focus, closes on Escape and creates by keyboard", async ({ page }) => {
    await keyboardSignIn(page, "admin@demo.example", "demo");
    await expect(page).toHaveURL(/\/admin\/metrics$/);
    await tabTo(page, page.getByRole("link", { name: "Counselors", exact: true }));
    await page.keyboard.press("Enter");
    await expect(page).toHaveURL(/\/admin\/counselors$/);
    const add = page.getByRole("button", { name: "Add counselor" });
    await tabTo(page, add);
    await page.keyboard.press("Enter");
    const dialog = page.getByRole("dialog");
    await expect(dialog).toBeVisible();
    for (let i = 0; i < 10; i++) {
      await page.keyboard.press("Tab");
      await expectFocusInside(page, dialog);
    }
    await page.keyboard.press("Escape");
    await expect(dialog).toHaveCount(0);
    await expect(add).toBeFocused();

    const stampKb = Date.now();
    await page.keyboard.press("Enter");
    await expect(dialog).toBeVisible();
    await typeInto(page, dialog.getByLabel("Email"), `kb+${stampKb}@example.com`);
    await typeInto(page, dialog.getByLabel("Display name"), `KB Counselor ${stampKb}`);
    await typeInto(page, dialog.getByLabel("Temporary password"), TEMP);
    await tabTo(page, dialog.getByRole("button", { name: "Create counselor" }));
    await page.keyboard.press("Enter");
    await tabTo(page, dialog.getByRole("button", { name: "Done" }));
    await page.keyboard.press("Enter");
    await expect(dialog).toHaveCount(0);
    await expect(page.getByRole("table", { name: "Counselors" }).getByRole("row", { name: new RegExp(`KB Counselor ${stampKb}`) })).toBeVisible();
  });
});
