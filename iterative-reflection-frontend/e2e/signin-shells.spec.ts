import { expect, test, type Page } from "@playwright/test";

// Needs the dev backend (demo accounts, password "demo").
const ACCOUNTS = {
  student: { email: "student1@demo.example", home: /\/student\/feed$/, nav: ["Feed", "My Journey", "Settings"] },
  counselor: { email: "counselor@demo.example", home: /\/counselor\/students$/, nav: ["Students", "Settings"] },
  admin: { email: "admin@demo.example", home: /\/admin\/metrics$/, nav: ["Metrics", "Counselors", "Audit log", "Settings"] },
} as const;

async function signIn(page: Page, email: string, password = "demo") {
  await page.goto("/signin");
  await page.getByLabel("Email").fill(email);
  await page.getByLabel("Password").fill(password);
  await page.getByRole("button", { name: "Sign in", exact: true }).click();
}

test("/signin?mode=signup selects Sign up, and tab switches update the URL without a reload", async ({ page }) => {
  await page.goto("/signin?mode=signup");
  const signUp = page.getByRole("tab", { name: "Sign up" });
  await expect(signUp).toHaveAttribute("aria-selected", "true");
  await page.evaluate(() => ((window as unknown as { __marker: boolean }).__marker = true));
  await page.getByRole("tab", { name: "Sign in" }).click();
  await expect(page).toHaveURL(/\/signin$/);
  await signUp.click();
  await expect(page).toHaveURL(/\/signin\?mode=signup$/);
  expect(await page.evaluate(() => (window as unknown as { __marker?: boolean }).__marker)).toBe(true);
});

for (const [role, a] of Object.entries(ACCOUNTS)) {
  test(`${role} signs in, lands on their home with the right sidebar, and signs out`, async ({ page }) => {
    await signIn(page, a.email);
    await expect(page).toHaveURL(a.home);
    const nav = page.getByRole("navigation", { name: "Main" });
    await expect(nav.getByRole("link")).toHaveText([...a.nav]);
    await expect(nav.locator("[aria-current=page]")).toHaveCount(1);

    // Signed-in visits to / and /signin go to the role home.
    await page.goto("/signin");
    await expect(page).toHaveURL(a.home);
    await page.goto("/");
    await expect(page).toHaveURL(a.home);

    // User menu: display name, Settings, Sign out.
    await page.getByTestId("user-menu-name").click();
    await expect(page.getByRole("menuitem", { name: "Settings" })).toBeVisible();
    await page.getByRole("menuitem", { name: "Sign out" }).click();
    await expect(page).toHaveURL(/\/signin$/);
    await page.goBack();
    // Back must not show protected data: it ends on the sign-in page again.
    await expect(page).toHaveURL(/\/signin$/);
    await expect(page.getByRole("navigation", { name: "Main" })).toHaveCount(0);
  });
}

test("every role/prefix mismatch redirects to the user's home without flashing protected content", async ({ page }) => {
  const prefixes: Record<string, string> = {
    student: "/student/feed",
    counselor: "/counselor/students",
    admin: "/admin/metrics",
  };
  for (const [role, a] of Object.entries(ACCOUNTS)) {
    await page.context().clearCookies();
    await signIn(page, a.email);
    await expect(page).toHaveURL(a.home);
    for (const [other, path] of Object.entries(prefixes)) {
      if (other === role) continue;
      // Watch every DOM mutation for the other portal's heading ever appearing.
      await page.addInitScript(() => {
        (window as unknown as { __seen: string[] }).__seen = [];
        new MutationObserver(() => {
          const h = document.querySelector("h1")?.textContent;
          if (h) (window as unknown as { __seen: string[] }).__seen.push(h);
        }).observe(document, { childList: true, subtree: true, characterData: true });
      });
      await page.goto(path);
      await expect(page).toHaveURL(a.home);
      const seen = await page.evaluate(() => (window as unknown as { __seen: string[] }).__seen);
      const otherHeading = { student: "Feed", counselor: "Students", admin: "Metrics" }[other]!;
      expect(seen).not.toContain(otherHeading);
    }
  }
});

test("wrong credentials show the generic message", async ({ page }) => {
  await signIn(page, "student1@demo.example", "not-the-password");
  await expect(page.locator("form [role=alert]")).toHaveText("Email or password is incorrect.");
  await expect(page.getByRole("button", { name: "Sign in", exact: true })).toBeEnabled();
});

test("a 429 shows the throttle message and leaves the button enabled", async ({ page }) => {
  await page.route("**/api/auth/signin", (route) =>
    route.fulfill({ status: 429, contentType: "application/problem+json", body: JSON.stringify({ status: 429, title: "Too Many Requests" }) }),
  );
  await signIn(page, "student1@demo.example");
  await expect(page.locator("form [role=alert]")).toHaveText("Too many attempts. Try again in 15 minutes.");
  await expect(page.getByRole("button", { name: "Sign in", exact: true })).toBeEnabled();
});
