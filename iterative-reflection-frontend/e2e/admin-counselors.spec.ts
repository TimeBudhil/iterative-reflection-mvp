import { expect, test, type Page } from "@playwright/test";

// Needs the dev backend (demo admin admin@demo.example / "demo"). Counselors are created with unique e2e+<ts> emails.
const TEMP = "temporary-pass-1";
const uniqueEmail = () => `e2e+${Date.now()}-${Math.floor(Math.random() * 1e6)}@example.com`;

async function signIn(page: Page, email: string, password: string) {
  await page.goto("/signin");
  await page.getByLabel("Email").fill(email);
  await page.getByLabel("Password").fill(password);
  await page.getByRole("button", { name: "Sign in", exact: true }).click();
}

async function signInAdmin(page: Page) {
  await signIn(page, "admin@demo.example", "demo");
  await expect(page).toHaveURL(/\/admin\/metrics$/);
  await page.goto("/admin/counselors");
}

async function createViaDialog(page: Page, email: string, name: string) {
  await page.getByRole("button", { name: "Add counselor" }).click();
  const dialog = page.getByRole("dialog");
  await dialog.getByLabel("Email").fill(email);
  await dialog.getByLabel("Display name").fill(name);
  await dialog.getByLabel("Temporary password").fill(TEMP);
  await dialog.getByRole("button", { name: "Create counselor" }).click();
  return dialog;
}

test("create shows the share notice and the row; the new counselor is forced to change password; deactivate/reactivate", async ({
  page,
  browser,
}) => {
  const email = uniqueEmail();
  const name = `E2E Counselor ${Date.now()}`;
  await signInAdmin(page);

  const table = page.getByRole("table", { name: "Counselors" });
  await expect(table.getByRole("columnheader")).toHaveText(["Name", "Email", "Join code", "Status", "Students", "Actions"]);

  const dialog = await createViaDialog(page, email, name);
  await expect(dialog.getByText("Share this temporary password with the counselor. They must change it on first sign-in.")).toBeVisible();
  await dialog.getByRole("button", { name: "Done" }).click();
  await expect(dialog).toHaveCount(0);

  // Newest first, so the new counselor is on page 1.
  const row = table.getByRole("row", { name: new RegExp(name) });
  await expect(row).toBeVisible();
  await expect(row.getByText("Active", { exact: true })).toBeVisible();
  await expect(row.getByRole("cell").nth(2)).toHaveText(/^[A-Z2-9]{8}$/);
  await expect(row.getByRole("cell").nth(4)).toHaveText("0");

  // Ticket 07's flow: a first sign-in lands on /change-password (the full flow is in forced-password-change.spec.ts).
  const ctx = await browser.newContext();
  const other = await ctx.newPage();
  await signIn(other, email, TEMP);
  await expect(other).toHaveURL(/\/change-password$/);

  // Cancel keeps the counselor active.
  await row.getByRole("button", { name: "Deactivate" }).click();
  const confirm = page.getByRole("dialog");
  await expect(confirm.getByText("They are signed out immediately")).toBeVisible();
  await confirm.getByRole("button", { name: "Cancel" }).click();
  await expect(confirm).toHaveCount(0);
  await expect(row.getByText("Active", { exact: true })).toBeVisible();

  await row.getByRole("button", { name: "Deactivate" }).click();
  await page.getByRole("dialog").getByRole("button", { name: "Deactivate", exact: true }).click();
  await expect(row.getByText("Inactive")).toBeVisible();

  // The deactivated counselor's session is gone at once.
  await other.reload();
  await expect(other).toHaveURL(/\/signin/);
  await ctx.close();

  await row.getByRole("button", { name: "Reactivate" }).click();
  await page.getByRole("dialog").getByRole("button", { name: "Reactivate", exact: true }).click();
  await expect(row.getByText("Active", { exact: true })).toBeVisible();
});

test("a duplicate email shows on the email field; short password is caught per field", async ({ page }) => {
  const email = uniqueEmail();
  await signInAdmin(page);
  let dialog = await createViaDialog(page, email, "First");
  await dialog.getByRole("button", { name: "Done" }).click();

  dialog = await createViaDialog(page, email, "Second");
  await expect(dialog.getByText("An account with this email already exists.")).toBeVisible();

  await dialog.getByLabel("Temporary password").fill("short");
  await dialog.getByLabel("Display name").fill("");
  await dialog.getByRole("button", { name: "Create counselor" }).click();
  await expect(dialog.getByText("Use at least 12 characters.")).toBeVisible();
  await expect(dialog.getByText("Enter a display name.")).toBeVisible();
});

test("pages through counselors 20 per page (flat page/size query)", async ({ page }) => {
  const row = (i: number) => ({
    id: 1000 + i,
    email: `c${i}@example.com`,
    displayName: `Counselor ${i}`,
    joinCode: "ABCDEFGH",
    active: i % 2 === 0,
    linkedStudentCount: i,
  });
  const requested: string[] = [];
  await signInAdmin(page);
  await page.route("**/api/admin/counselors?*", (route) => {
    const url = new URL(route.request().url());
    requested.push(`${url.searchParams.get("page")}/${url.searchParams.get("size")}`);
    const n = Number(url.searchParams.get("page"));
    const content = Array.from({ length: n === 0 ? 20 : 5 }, (_, i) => row(n * 20 + i + 1));
    return route.fulfill({ json: { content, page: { size: 20, number: n, totalElements: 25, totalPages: 2 } } });
  });
  await page.reload();
  const table = page.getByRole("table", { name: "Counselors" });
  await expect(table.locator("tbody tr")).toHaveCount(20);
  await page.getByRole("button", { name: "Next", exact: true }).click();
  await expect(table.locator("tbody tr")).toHaveCount(5);
  await expect(page.getByText("21–25 of 25")).toBeVisible();
  expect(requested).toContain("0/20");
  expect(requested).toContain("1/20");
});
