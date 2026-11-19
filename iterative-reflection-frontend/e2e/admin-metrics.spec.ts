import { expect, test } from "@playwright/test";

// Needs the dev backend (demo admin, password "demo").
test.beforeEach(async ({ page }) => {
  await page.goto("/signin");
  await page.waitForLoadState("networkidle"); // dev server: let hydration finish before submitting
  await page.getByLabel("Email").fill("admin@demo.example");
  await page.getByLabel("Password").fill("demo");
  await page.getByRole("button", { name: "Sign in", exact: true }).click();
  await expect(page).toHaveURL(/\/admin\/metrics$/);
});

test("tiles, verdict chart and the demo-exclusion note render", async ({ page }) => {
  await expect(page.getByRole("heading", { name: "Metrics" })).toBeVisible();
  await expect(page.getByTestId("tile-total")).toBeVisible();
  await expect(page.getByTestId("tile-reflections")).toHaveText(/^\d+(\.\d)?%$/);
  await expect(page.getByTestId("tile-intake")).toHaveText(/^\d+(\.\d)?%$/);
  const chart = page.getByRole("list", { name: "Verdict counts" });
  await expect(chart.getByRole("listitem")).toHaveText([/Yes/, /Maybe/, /No/]);
  await expect(page.getByText("Demo accounts are excluded")).toBeVisible();
});

test("From/To are sent as from/to, live in the URL, and clearing returns to all-time", async ({ page }) => {
  const urls: string[] = [];
  page.on("request", (r) => {
    if (r.url().includes("/api/admin/pilot-metrics")) urls.push(r.url());
  });
  await page.getByLabel("From", { exact: true }).fill("2030-01-01");
  await expect(page).toHaveURL(/\?from=2030-01-01$/);
  await page.getByLabel("To", { exact: true }).fill("2030-12-31");
  await expect(page).toHaveURL(/\?from=2030-01-01&to=2030-12-31$/);
  await expect(page.getByTestId("tile-total")).toHaveText("0");
  expect(urls.some((u) => u.includes("from=2030-01-01") && u.includes("to=2030-12-31"))).toBe(true);

  // Reload keeps the filter.
  await page.reload();
  await expect(page.getByLabel("From", { exact: true })).toHaveValue("2030-01-01");

  await page.getByLabel("From", { exact: true }).fill("");
  await page.getByLabel("To", { exact: true }).fill("");
  await expect(page).toHaveURL(/\/admin\/metrics$/);
  const last = urls[urls.length - 1];
  expect(last).not.toContain("from=");
  expect(last).not.toContain("to=");
});
