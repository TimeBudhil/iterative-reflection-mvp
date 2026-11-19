import { expect, test, type Page } from "@playwright/test";

// Cross-role flow against the compose rehearsal (`pnpm e2e:smoke`), driven entirely through the UI:
// admin signs in and sees metrics, creates a counselor; the counselor's first sign-in forces a password change;
// a student signs up with that counselor's join code and sees a feed card; the counselor sees the student.
const adminEmail = process.env.SMOKE_ADMIN_EMAIL ?? process.env.ADMIN_EMAIL;
const adminPassword = process.env.SMOKE_ADMIN_PASSWORD ?? process.env.ADMIN_PASSWORD;

const TEMP = "temporary-pass-1";
const COUNSELOR_PASSWORD = "a-brand-new-password";
const STUDENT_PASSWORD = "correct-horse-battery";

async function signIn(page: Page, email: string, password: string) {
  await page.goto("/signin");
  await page.waitForLoadState("networkidle");
  await page.getByLabel("Email").fill(email);
  await page.getByLabel("Password").fill(password);
  await page.getByRole("button", { name: "Sign in", exact: true }).click();
}

test("admin creates a counselor, the counselor changes password, a student joins with the code and sees a card", async ({
  browser,
  baseURL,
}) => {
  expect(adminEmail, "set ADMIN_EMAIL in deploy/.env").toBeTruthy();
  expect(adminPassword, "set ADMIN_PASSWORD in deploy/.env").toBeTruthy();
  const stamp = `${Date.now()}-${Math.floor(Math.random() * 1e6)}`;
  const counselorEmail = `e2e+counselor-${stamp}@example.com`;
  const counselorName = `Smoke Counselor ${stamp}`;
  const studentEmail = `e2e+student-${stamp}@example.com`;
  const studentName = `Smoke Student ${stamp}`;

  // 1. Admin: sign in, see metrics, create a counselor, read the join code from the table.
  const adminCtx = await browser.newContext();
  const admin = await adminCtx.newPage();
  await signIn(admin, adminEmail!, adminPassword!);
  await expect(admin).toHaveURL(/\/admin\/metrics$/);
  await expect(admin.getByRole("heading", { name: "Metrics" })).toBeVisible();
  await expect(admin.getByTestId("tile-total")).toBeVisible();
  const session = (await adminCtx.cookies()).find((c) => c.name === "SESSION");
  expect(session).toMatchObject({ secure: baseURL!.startsWith("https:"), httpOnly: true });

  await admin.getByRole("navigation", { name: "Main" }).getByRole("link", { name: "Counselors" }).click();
  await expect(admin).toHaveURL(/\/admin\/counselors$/);
  await admin.getByRole("button", { name: "Add counselor" }).click();
  const dialog = admin.getByRole("dialog");
  await dialog.getByLabel("Email").fill(counselorEmail);
  await dialog.getByLabel("Display name").fill(counselorName);
  await dialog.getByLabel("Temporary password").fill(TEMP);
  await dialog.getByRole("button", { name: "Create counselor" }).click();
  await dialog.getByRole("button", { name: "Done" }).click();
  const row = admin.getByRole("table", { name: "Counselors" }).getByRole("row", { name: new RegExp(counselorName) });
  await expect(row).toBeVisible();
  const joinCodeCell = row.getByRole("cell").nth(2);
  await expect(joinCodeCell).toHaveText(/^[A-Z2-9]{8}$/);
  const joinCode = (await joinCodeCell.textContent())!.trim();
  await adminCtx.close();

  // 2. Counselor: first sign-in is held at /change-password; after changing it they land on their home.
  const counselorCtx = await browser.newContext();
  const counselor = await counselorCtx.newPage();
  await signIn(counselor, counselorEmail, TEMP);
  await expect(counselor).toHaveURL(/\/change-password$/);
  await counselor.getByLabel("Current password").fill(TEMP);
  await counselor.getByLabel("New password", { exact: true }).fill(COUNSELOR_PASSWORD);
  await counselor.getByLabel("Confirm new password").fill(COUNSELOR_PASSWORD);
  await counselor.getByRole("button", { name: "Change password" }).click();
  await expect(counselor).toHaveURL(/\/counselor\/students$/);

  // 3. Student: sign up with the counselor's code, land on the feed and see a card.
  const studentCtx = await browser.newContext();
  const student = await studentCtx.newPage();
  await student.goto("/signin?mode=signup");
  await student.waitForLoadState("networkidle");
  await student.getByLabel("Email").fill(studentEmail);
  await student.getByLabel("Password").fill(STUDENT_PASSWORD);
  await student.getByLabel("Display name").fill(studentName);
  await student.getByLabel("Birthdate").fill("2005-05-05");
  await student.getByLabel("Counselor code (optional)").fill(joinCode);
  await student.getByLabel(/I agree to the/).check();
  await student.getByRole("button", { name: "Create account" }).click();
  await expect(student).toHaveURL(/\/student\/feed$/, { timeout: 30_000 });
  const card = student.getByTestId("career-card");
  await expect(card).toHaveCount(1);
  await expect(card.locator("[data-slot=card-title]")).not.toBeEmpty();
  await expect(card.getByRole("link", { name: "Start", exact: true })).toBeVisible();

  // 4. The counselor now sees the linked student.
  await counselor.reload();
  await expect(counselor.getByText(studentName)).toBeVisible();

  await studentCtx.close();
  await counselorCtx.close();
});
