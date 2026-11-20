import { randomUUID } from "node:crypto";
import { expect, test, type Page } from "@playwright/test";
import { apiContext, counselorContext, seedLinkedStudent, send } from "./support/seed-student";
import { csrfHeaders, resetThemeToGrove, signIn } from "./support/helpers";

// Needs the dev backend (demo accounts, password "demo"). Real audit rows come from the demo counselor
// opening a linked student's detail (ALLOWED) and an unlinked student's (DENIED).
let linkedId: number;
let unlinkedId: number;

test.beforeAll(async () => {
  linkedId = (await seedLinkedStudent({ reflections: 0, abandon: false, intake: false })).id;
  const c = await apiContext();
  const stamp = randomUUID().slice(0, 8);
  const me = await (
    await send(c, "/api/auth/signup", {
      email: `unlinked${stamp}@e2e.example`,
      password: "password12345",
      displayName: `Unlinked ${stamp}`,
      birthdate: "2000-01-01",
      acceptedTermsVersion: "1",
    })
  ).json();
  unlinkedId = me.id;
  const counselor = await counselorContext();
  expect((await counselor.get(`/api/counselor/students/${linkedId}`)).status()).toBe(200);
  expect((await counselor.get(`/api/counselor/students/${unlinkedId}`)).status()).toBe(403);
});

async function signInAdmin(page: Page) {
  await signIn(page, "admin@demo.example", "demo", /\/admin\/metrics$/);
  await page.goto("/admin/audit");
}

test("rows show counselor name, student id, outcome badge and local time, newest first; filter sends studentId", async ({ page }) => {
  await signInAdmin(page);
  const table = page.getByRole("table", { name: "Audit log" });
  await expect(table.getByRole("columnheader")).toHaveText(["Counselor", "Student", "Outcome", "When"]);

  const denied = table.getByRole("row", { name: new RegExp(`#${unlinkedId}\\b`) });
  await expect(denied).toHaveCount(1);
  await expect(denied.getByRole("cell").nth(0)).toHaveText("Demo Counselor");
  await expect(denied.getByText("Denied", { exact: true })).toBeVisible();

  // Newest first: the denied row (last call) precedes the allowed one.
  const rows = table.getByRole("row");
  await expect(rows.nth(1)).toContainText(`#${unlinkedId}`);
  await expect(rows.nth(2)).toContainText(`#${linkedId}`);
  await expect(rows.nth(2).getByText("Allowed", { exact: true })).toBeVisible();

  // Timestamp is the browser-local rendering of the API instant.
  const api = await page.evaluate(async () => (await fetch("/api/admin/audit?page=0&size=20")).json());
  const expected = await page.evaluate((iso: string) => new Date(iso).toLocaleString(), api.content[0].at);
  await expect(rows.nth(1).getByRole("cell").nth(3)).toHaveText(expected);

  // Filter.
  const request = page.waitForRequest((r) => r.url().includes("/api/admin/audit") && r.url().includes(`studentId=${linkedId}`));
  await page.getByLabel("Student ID").fill(String(linkedId));
  await page.getByRole("button", { name: "Filter" }).click();
  expect((await request).url()).toContain(`studentId=${linkedId}`);
  await expect(table.getByRole("row")).toHaveCount(2);
  await expect(table.getByText(`#${unlinkedId}`)).toHaveCount(0);

  await page.getByRole("button", { name: "Clear" }).click();
  await expect(table.getByText(`#${unlinkedId}`)).toBeVisible();
});

test("a student with no audit entries shows the empty state; bad input is rejected", async ({ page }) => {
  await signInAdmin(page);
  await page.getByLabel("Student ID").fill("abc");
  await page.getByRole("button", { name: "Filter" }).click();
  await expect(page.locator("#audit-student-id-error")).toContainText("whole number");
  await page.getByLabel("Student ID").fill("987654");
  await page.getByRole("button", { name: "Filter" }).click();
  await expect(page.getByText("No audit entries for student #987654.")).toBeVisible();
});

test.describe("Admin activity tab", () => {
  test.describe.configure({ mode: "serial" });
  test.afterAll(async ({ browser }) => {
    await resetThemeToGrove(browser);
  });

  test("lists a theme change and a counselor creation, and ?tab=activity survives a reload", async ({ page }) => {
    await signInAdmin(page);
    const headers = await csrfHeaders(page.request);
    const email = `activity+${Date.now()}@e2e.example`;
    expect(
      (await page.request.post("/api/admin/counselors", { headers, data: { email, displayName: "Activity Counselor", temporaryPassword: "temporary-pass-1" } })).status(),
    ).toBe(201);
    expect((await page.request.put("/api/admin/settings/theme", { headers, data: { presetId: "harbor" } })).status()).toBe(200);

    // The counselor views tab is still the default, with its own filter.
    await expect(page.getByRole("tab", { name: "Counselor views" })).toHaveAttribute("aria-selected", "true");
    await expect(page.getByLabel("Student ID")).toBeVisible();

    await page.getByRole("tab", { name: "Admin activity" }).click();
    await expect(page).toHaveURL(/\/admin\/audit\?tab=activity$/);
    const table = page.getByRole("table", { name: "Admin activity" });
    await expect(table.getByRole("columnheader")).toHaveText(["When", "Admin", "Action", "Details"]);
    await expect(table.getByRole("row", { name: new RegExp(`Created counselor ${email.replace(/[+.]/g, "\\$&")}`) })).toBeVisible();
    await expect(table.getByRole("row", { name: /Theme: \w+ → Harbor/ }).first()).toBeVisible();
    await expect(table.getByRole("row", { name: /Created counselor/ }).first().getByRole("cell").nth(1)).toHaveText("Demo Admin");

    await page.reload();
    await expect(page).toHaveURL(/tab=activity/);
    await expect(page.getByRole("tab", { name: "Admin activity" })).toHaveAttribute("aria-selected", "true");
    await expect(page.getByRole("table", { name: "Admin activity" })).toBeVisible();

    await page.getByRole("tab", { name: "Counselor views" }).click();
    await expect(page).toHaveURL(/\/admin\/audit$/);
  });
});
