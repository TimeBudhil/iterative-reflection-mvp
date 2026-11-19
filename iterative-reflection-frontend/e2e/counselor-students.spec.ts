import { expect, test, type Page } from "@playwright/test";
import { signIn } from "./support/helpers";

// Needs the dev backend (demo accounts, password "demo"). The demo counselor has 2 linked students.
test.use({ permissions: ["clipboard-read", "clipboard-write"] });

async function signInCounselor(page: Page) {
  await signIn(page, "counselor@demo.example", "demo", /\/counselor\/students$/);
}

test("copy puts the code on the clipboard and toasts; rotate confirms and swaps the code", async ({ page }) => {
  await signInCounselor(page);
  const code = page.getByTestId("join-code");
  await expect(code).toHaveText(/^[A-Z2-9]{8}$/);
  const oldCode = (await code.textContent())!;

  await page.getByRole("button", { name: "Copy" }).click();
  await expect(page.getByText("Join code copied")).toBeVisible();
  expect(await page.evaluate(() => navigator.clipboard.readText())).toBe(oldCode);

  // Cancel keeps the code.
  await page.getByRole("button", { name: "Rotate" }).click();
  const dialog = page.getByRole("dialog");
  await expect(dialog.getByText("The old code stops working. Linked students stay linked.")).toBeVisible();
  await dialog.getByRole("button", { name: "Cancel" }).click();
  await expect(dialog).toHaveCount(0);
  await expect(code).toHaveText(oldCode);

  await page.getByRole("button", { name: "Rotate" }).click();
  await page.getByRole("dialog").getByRole("button", { name: "Rotate" }).click();
  await expect(page.getByRole("dialog")).toHaveCount(0);
  await expect(code).not.toHaveText(oldCode);
  await expect(code).toHaveText(/^[A-Z2-9]{8}$/);
  // Linked students stay linked.
  await expect(page.getByRole("table", { name: "Linked students" }).getByRole("row")).not.toHaveCount(1);
});

test("the students table shows the columns and rows in API order", async ({ page }) => {
  await signInCounselor(page);
  const table = page.getByRole("table", { name: "Linked students" });
  await expect(table.getByRole("columnheader")).toHaveText([
    "Name",
    "Seen",
    "Reflected",
    "Abandoned",
    "YES / MAYBE / NO",
    "Intake",
    "Last active",
  ]);
  const apiOrder = await page.evaluate(async () => {
    const res = await fetch("/api/counselor/students?page=0&size=20");
    return ((await res.json()) as { content: { displayName: string }[] }).content.map((r) => r.displayName);
  });
  expect(apiOrder.length).toBeGreaterThan(0);
  const names = await table.locator("tbody td:first-child").allTextContents();
  expect(names).toEqual(apiOrder);
});

test("pages through 20 per page with page controls", async ({ page }) => {
  const row = (i: number) => ({
    studentId: i,
    displayName: `Student ${i}`,
    careersSeen: 1,
    reflected: 1,
    abandoned: 0,
    yes: 1,
    maybe: 0,
    no: 0,
    intakeCompleted: i % 2 === 0,
    lastActive: new Date(Date.now() - i * 3_600_000).toISOString(),
  });
  const requested: string[] = [];
  await signInCounselor(page);
  await page.route("**/api/counselor/students?*", (route) => {
    const url = new URL(route.request().url());
    requested.push(`${url.searchParams.get("page")}/${url.searchParams.get("size")}`);
    const n = Number(url.searchParams.get("page"));
    const content = Array.from({ length: n === 0 ? 20 : 5 }, (_, i) => row(n * 20 + i + 1));
    return route.fulfill({ json: { content, page: { size: 20, number: n, totalElements: 25, totalPages: 2 } } });
  });
  await page.reload();
  const table = page.getByRole("table", { name: "Linked students" });
  await expect(table.locator("tbody tr")).toHaveCount(20);
  await expect(page.getByText("1–20 of 25")).toBeVisible();
  await expect(page.getByRole("button", { name: "Previous" })).toBeDisabled();
  await expect(table.getByText("1 hour ago")).toBeVisible();
  await page.getByRole("button", { name: "Next", exact: true }).click();
  await expect(table.locator("tbody tr")).toHaveCount(5);
  await expect(page.getByText("21–25 of 25")).toBeVisible();
  await expect(page.getByRole("button", { name: "Next", exact: true })).toBeDisabled();
  expect(requested).toContain("0/20");
  expect(requested).toContain("1/20");
});

test("zero students shows the empty message", async ({ page }) => {
  await signInCounselor(page);
  await page.route("**/api/counselor/students?*", (route) =>
    route.fulfill({ json: { content: [], page: { size: 20, number: 0, totalElements: 0, totalPages: 0 } } }),
  );
  await page.reload();
  await expect(page.getByText("No students yet. Share your join code above.")).toBeVisible();
  await expect(page.getByRole("table")).toHaveCount(0);
});

test("a server error shows the retry panel with the request id", async ({ page }) => {
  await signInCounselor(page);
  await page.route("**/api/counselor/students?*", (route) =>
    route.fulfill({
      status: 500,
      headers: { "X-Request-Id": "req-abc" },
      contentType: "application/problem+json",
      body: '{"status":500,"title":"Internal Server Error"}',
    }),
  );
  await page.reload();
  await expect(page.getByText("Request ID: req-abc")).toBeVisible();
  await expect(page.getByRole("button", { name: "Try again" })).toBeVisible();
});
