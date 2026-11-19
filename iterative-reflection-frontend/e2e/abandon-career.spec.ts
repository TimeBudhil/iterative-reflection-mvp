import { expect, test, type Page } from "@playwright/test";
import { freshStudent as freshStudentFor } from "./support/helpers";

// Needs the dev backend. Each test signs up a fresh student through the API.
const freshStudent = (page: Page) => freshStudentFor(page, "abandon", "Abandon Tester");

test("the button shows on every page, cancel keeps the career, confirm abandons and lands on a different card", async ({ page }) => {
  const id = await freshStudent(page);
  await page.getByRole("link", { name: "Start", exact: true }).click();
  await expect(page).toHaveURL(new RegExp(`/student/careers/${id}$`), { timeout: 30_000 });

  const button = page.getByRole("button", { name: "This isn't for me" });
  const indicator = page.getByTestId("page-indicator");
  await expect(indicator).toHaveText(/^Page 1 of (\d+)$/);
  const last = Number((await indicator.textContent())!.match(/of (\d+)/)![1]);
  for (let n = 1; n <= last; n++) {
    await expect(indicator).toHaveText(`Page ${n} of ${last}`);
    await expect(button).toBeVisible();
    if (n < last) await page.getByRole("button", { name: "Next", exact: true }).click();
  }

  await button.click();
  const dialog = page.getByRole("dialog");
  await expect(dialog).toBeVisible();
  await dialog.getByRole("button", { name: "Keep reading" }).click();
  await expect(dialog).toBeHidden();
  await expect(page).toHaveURL(new RegExp(`/student/careers/${id}$`));

  const abandon = page.waitForResponse((r) => r.url().endsWith(`/api/careers/${id}/abandon`) && r.request().method() === "POST");
  await button.click();
  await dialog.getByRole("button", { name: "Yes, skip it" }).click();
  expect((await abandon).status()).toBe(204);

  await expect(page).toHaveURL(/\/student\/feed$/);
  await expect(page.getByRole("link", { name: "Start", exact: true })).not.toHaveAttribute("href", new RegExp(`/${id}$`));
});

test("a fresh student can abandon from page 1", async ({ page }) => {
  const id = await freshStudent(page);
  await page.goto(`/student/careers/${id}`);
  await expect(page.getByTestId("page-indicator")).toHaveText(/^Page 1 of/);
  await page.getByRole("button", { name: "This isn't for me" }).click();
  await page.getByRole("dialog").getByRole("button", { name: "Yes, skip it" }).click();
  await expect(page).toHaveURL(/\/student\/feed$/);
  await expect(page.getByRole("link", { name: "Start", exact: true })).not.toHaveAttribute("href", new RegExp(`/${id}$`));
});
