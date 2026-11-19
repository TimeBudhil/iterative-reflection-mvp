import { expect, test, type Page } from "@playwright/test";

// Needs the dev backend. Each test signs up a fresh student through the API (the sign-up UI is another ticket).
const PASSWORD = "correct-horse-battery";

async function freshStudent(page: Page) {
  const email = `feed-${Date.now()}-${Math.random().toString(36).slice(2, 8)}@e2e.example`;
  await page.request.get("/api/me"); // primes the XSRF-TOKEN cookie
  const xsrf = (await page.context().cookies()).find((c) => c.name === "XSRF-TOKEN")!.value;
  const res = await page.request.post("/api/auth/signup", {
    headers: { "X-XSRF-TOKEN": xsrf },
    data: { email, password: PASSWORD, displayName: "Feed Tester", birthdate: "2000-01-01", acceptedTermsVersion: "2026-11-18" },
  });
  expect(res.status()).toBe(201);
  return email;
}

async function signIn(page: Page, email: string) {
  await page.goto("/signin");
  await page.getByLabel("Email").fill(email);
  await page.getByLabel("Password").fill(PASSWORD);
  await page.getByRole("button", { name: "Sign in", exact: true }).click();
  await expect(page).toHaveURL(/\/student\/feed$/);
}

test("shows exactly one card with title, summary, tag chips and minutes; the same card after reload and re-sign-in", async ({ page }) => {
  const email = await freshStudent(page);
  await signIn(page, email);

  const card = page.getByTestId("career-card");
  await expect(card).toHaveCount(1);
  const title = (await card.locator("[data-slot=card-title]").textContent())!.trim();
  expect(title.length).toBeGreaterThan(0);
  await expect(card).toContainText(/~\d+ min/);
  expect(await card.getByRole("list", { name: "Tags" }).getByRole("listitem").count()).toBeGreaterThan(0);
  const start = card.getByRole("link", { name: "Start", exact: true });
  await expect(start).toHaveAttribute("href", /^\/student\/careers\/\d+$/);

  await page.reload();
  await expect(page.getByTestId("career-card").locator("[data-slot=card-title]")).toHaveText(title);

  await page.getByTestId("user-menu-name").click();
  await page.getByRole("menuitem", { name: "Sign out" }).click();
  await expect(page).toHaveURL(/\/signin$/);
  await signIn(page, email);
  await expect(page.getByTestId("career-card").locator("[data-slot=card-title]")).toHaveText(title);
});

test("with progress the button reads Continue (page n of m) and opens the career", async ({ page }) => {
  const email = await freshStudent(page);
  await signIn(page, email);
  const careerId = Number((await page.getByRole("link", { name: "Start", exact: true }).getAttribute("href"))!.split("/").pop());

  const xsrf = (await page.context().cookies()).find((c) => c.name === "XSRF-TOKEN")!.value;
  const viewed = await page.request.post(`/api/careers/${careerId}/pages/1/viewed`, { headers: { "X-XSRF-TOKEN": xsrf } });
  expect(viewed.status()).toBe(200);
  const experience = await (await page.request.get(`/api/careers/${careerId}/experience`)).json();

  await page.reload();
  const link = page.getByRole("link", { name: `Continue (page 1 of ${experience.lastPage})`, exact: true });
  await expect(link).toBeVisible();
  await link.click();
  await expect(page).toHaveURL(new RegExp(`/student/careers/${careerId}$`));
});

test("exhausted shows the finished message and a link to My Journey", async ({ page }) => {
  const email = await freshStudent(page);
  await page.route("**/api/feed/next", (route) =>
    route.fulfill({ contentType: "application/json", body: JSON.stringify({ career: null, exhausted: true }) }),
  );
  await signIn(page, email);
  await expect(page.getByText("You've explored every career.")).toBeVisible();
  await expect(page.getByTestId("career-card")).toHaveCount(0);
  await page.getByTestId("feed-exhausted").getByRole("link", { name: "My Journey" }).click();
  await expect(page).toHaveURL(/\/student\/journey$/);
});
