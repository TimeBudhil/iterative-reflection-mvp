import { expect, test, type Page } from "@playwright/test";

// Needs the dev backend. Each test signs up a fresh student through the API.
const PASSWORD = "correct-horse-battery";

async function xsrf(page: Page) {
  return (await page.context().cookies()).find((c) => c.name === "XSRF-TOKEN")!.value;
}

async function freshStudent(page: Page) {
  const email = `viewer-${Date.now()}-${Math.random().toString(36).slice(2, 8)}@e2e.example`;
  await page.request.get("/api/me");
  const res = await page.request.post("/api/auth/signup", {
    headers: { "X-XSRF-TOKEN": await xsrf(page) },
    data: { email, password: PASSWORD, displayName: "Viewer Tester", birthdate: "2000-01-01", acceptedTermsVersion: "2026-11-18" },
  });
  expect(res.status()).toBe(201);
  await page.goto("/signin");
  await page.getByLabel("Email").fill(email);
  await page.getByLabel("Password").fill(PASSWORD);
  await page.getByRole("button", { name: "Sign in", exact: true }).click();
  await expect(page).toHaveURL(/\/student\/feed$/);
  return Number((await page.getByRole("link", { name: "Start", exact: true }).getAttribute("href"))!.split("/").pop());
}

test("pages through the career, saves progress, shows Reflect only on the last page", async ({ page }) => {
  const id = await freshStudent(page);
  const viewed: string[] = [];
  page.on("request", (r) => {
    const m = r.url().match(/\/pages\/(\d+)\/viewed$/);
    if (m && r.method() === "POST") viewed.push(m[1]);
  });
  await page.getByRole("link", { name: "Start", exact: true }).click();
  await expect(page).toHaveURL(new RegExp(`/student/careers/${id}$`), { timeout: 30_000 }); // first dev compile of the route

  const indicator = page.getByTestId("page-indicator");
  await expect(indicator).toHaveText(/^Page 1 of (\d+)$/);
  const last = Number((await indicator.textContent())!.match(/of (\d+)/)![1]);
  await expect(page.getByRole("button", { name: "Previous" })).toBeDisabled();
  await expect(page.getByRole("link", { name: "Reflect on this career" })).toHaveCount(0);

  // Page 2 holds the video embed, the last page the external link.
  await page.getByRole("button", { name: "Next", exact: true }).click();
  await expect(indicator).toHaveText(`Page 2 of ${last}`);
  const frame = page.locator("iframe");
  await expect(frame).toHaveAttribute("sandbox", "allow-scripts allow-same-origin allow-presentation");
  expect(await frame.getAttribute("title")).toBeTruthy();

  for (let n = 3; n <= last; n++) await page.getByRole("button", { name: "Next", exact: true }).click();
  await expect(indicator).toHaveText(`Page ${last} of ${last}`);
  const link = page.getByRole("link", { name: "Reflect on this career" });
  await expect(link).toHaveAttribute("href", `/student/careers/${id}/reflect`);
  await expect(page.getByRole("button", { name: "Next", exact: true })).toBeDisabled();
  const external = page.locator('a[target="_blank"]');
  await expect(external).toHaveAttribute("rel", "noopener noreferrer");

  await page.getByRole("button", { name: "Previous" }).click();
  await expect(page.getByRole("link", { name: "Reflect on this career" })).toHaveCount(0);

  expect(viewed).toEqual(expect.arrayContaining(["1", "2", String(last)]));
});

test("reopening resumes at lastViewedPage", async ({ page }) => {
  const id = await freshStudent(page);
  const res = await page.request.post(`/api/careers/${id}/pages/2/viewed`, { headers: { "X-XSRF-TOKEN": await xsrf(page) } });
  expect(res.status()).toBe(200);
  await page.goto(`/student/careers/${id}`);
  await expect(page.getByTestId("page-indicator")).toHaveText(/^Page 2 of/);
});

test("a career that isn't the in-progress card shows the unavailable state", async ({ page }) => {
  const id = await freshStudent(page);
  await page.goto(`/student/careers/${id + 1}`);
  await expect(page.getByText("This career isn't available right now.")).toBeVisible();
  await page.getByRole("link", { name: "Back to the feed" }).click();
  await expect(page).toHaveURL(/\/student\/feed$/);

  await page.goto("/student/careers/999999");
  await expect(page.getByText("This career isn't available right now.")).toBeVisible();
});

test("renders IMAGE_URL, AUDIO_EMBED and EXTERNAL_LINK blocks safely", async ({ page }) => {
  const id = await freshStudent(page);
  await page.route(`**/api/careers/${id}/experience`, (route) =>
    route.fulfill({
      contentType: "application/json",
      body: JSON.stringify({
        id,
        title: "Mock",
        lastPage: 1,
        lastViewedPage: 0,
        pages: [
          {
            number: 1,
            title: "Blocks",
            blocks: [
              { type: "TEXT", text: "Hello\n\nWorld" },
              { type: "IMAGE_URL", url: "https://example.com/x.png", label: "A diagram" },
              { type: "AUDIO_EMBED", url: "https://w.soundcloud.com/player/?x=1", label: "A podcast" },
              { type: "EXTERNAL_LINK", url: "https://www.bls.gov/ooh/", label: "More info" },
            ],
          },
        ],
      }),
    }),
  );
  await page.goto(`/student/careers/${id}`);
  await expect(page.getByRole("img", { name: "A diagram" })).toBeVisible();
  await expect(page.locator('iframe[title="A podcast"]')).toHaveAttribute("sandbox", "allow-scripts allow-same-origin allow-presentation");
  const link = page.getByRole("link", { name: "More info" });
  await expect(link).toHaveAttribute("target", "_blank");
  await expect(link).toHaveAttribute("rel", "noopener noreferrer");
  await expect(page.getByText("World")).toBeVisible();
});

test("a reflected career opens in review mode: free navigation, no reflect button, no progress calls", async ({ page }) => {
  const id = await freshStudent(page);
  await page.route("**/api/students/me/reflections*", (route) =>
    route.fulfill({
      contentType: "application/json",
      body: JSON.stringify({ content: [{ careerId: id }], page: { size: 100, number: 0, totalElements: 1, totalPages: 1 } }),
    }),
  );
  let viewedCalls = 0;
  page.on("request", (r) => {
    if (/\/viewed$/.test(r.url())) viewedCalls++;
  });
  await page.goto(`/student/careers/${id}`);
  await expect(page.getByTestId("page-indicator")).toHaveText(/^Page 1 of/);
  await page.getByRole("list", { name: "Jump to page" }).getByRole("button", { name: "3", exact: true }).click();
  await expect(page.getByTestId("page-indicator")).toHaveText(/^Page 3 of/);
  await expect(page.getByRole("link", { name: "Reflect on this career" })).toHaveCount(0);
  expect(viewedCalls).toBe(0);
});
