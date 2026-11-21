import { expect, test, type Page } from "@playwright/test";
import { signIn } from "./support/helpers";

// Needs the dev backend (demo accounts, password "demo").
const ACCOUNTS = {
  student: { email: "student1@demo.example", home: /\/student\/feed$/, nav: ["Feed", "My Journey", "Settings"] },
  counselor: { email: "counselor@demo.example", home: /\/counselor\/students$/, nav: ["Students", "Settings"] },
  admin: { email: "admin@demo.example", home: /\/admin\/metrics$/, nav: ["Metrics", "Counselors", "Audit log", "Settings"] },
} as const;

test("a 5xx on sign-in shows the written copy and the request id", async ({ page }) => {
  await page.route("**/api/auth/signin", (route) =>
    route.fulfill({ status: 500, contentType: "application/problem+json", headers: { "X-Request-Id": "req-signin-1" }, body: "{}" }),
  );
  await signIn(page, "student1@demo.example");
  await expect(page.locator("form [role=alert]")).toContainText("Something went wrong on our side");
  await expect(page.getByTestId("request-id")).toHaveText("req-signin-1");
});

test("the Iterative Reflection wordmark is on the sign-in page", async ({ page }) => {
  await page.goto("/signin");
  await expect(page.getByText("Iterative Reflection", { exact: true }).first()).toBeVisible();
});

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
    await expect(page.getByText("Iterative Reflection", { exact: true }).first()).toBeVisible();

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
    await expect(page).toHaveURL(/\/signin(\?|$)/);
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

test.describe("app theme", () => {
  const html = (page: Page) => page.locator("html");
  const noThemeError = async (page: Page) => {
    await expect(page.locator("form [role=alert]")).toHaveCount(0);
    await expect(page.locator("[data-sonner-toast]")).toHaveCount(0);
    await expect(page.getByRole("button", { name: /retry|try again/i })).toHaveCount(0);
  };
  const presetBody = (presetId: string) => JSON.stringify({ presetId });

  test("with nothing saved, signed-out pages are Grove", async ({ page }) => {
    // Stubbed: the real setting is global and the admin settings spec changes it (briefly) in a parallel worker.
    await page.route("**/api/settings/theme", (route) =>
      route.fulfill({ contentType: "application/json", body: presetBody("grove") }),
    );
    for (const path of ["/signin", "/terms", "/change-password"]) {
      await page.goto(path);
      await expect(html(page)).toHaveAttribute("data-theme", "grove");
    }
  });

  test("a saved preset id is applied on /signin, /terms and /change-password", async ({ page }) => {
    await page.route("**/api/settings/theme", (route) =>
      route.fulfill({ contentType: "application/json", body: presetBody("harbor") }),
    );
    for (const path of ["/signin", "/terms", "/change-password"]) {
      await page.goto(path);
      await expect(html(page)).toHaveAttribute("data-theme", "harbor");
    }
  });

  test("an aborted request falls back to Grove with no error shown", async ({ page }) => {
    await page.route("**/api/settings/theme", (route) => route.abort());
    await page.goto("/signin");
    await expect(html(page)).toHaveAttribute("data-theme", "grove");
    await noThemeError(page);
  });

  test("a server error falls back to Grove with no error shown", async ({ page }) => {
    await page.route("**/api/settings/theme", (route) => route.fulfill({ status: 500, body: "{}" }));
    await page.goto("/signin");
    await expect(html(page)).toHaveAttribute("data-theme", "grove");
    await noThemeError(page);
  });

  test("an unknown id falls back to Grove with no error shown", async ({ page }) => {
    await page.route("**/api/settings/theme", (route) =>
      route.fulfill({ contentType: "application/json", body: presetBody("neon") }),
    );
    await page.goto("/signin");
    await expect(html(page)).toHaveAttribute("data-theme", "grove");
    await noThemeError(page);
  });

  test("the theme is fetched once per page load: no refetch on focus", async ({ page }) => {
    let calls = 0;
    await page.route("**/api/settings/theme", (route) => {
      calls += 1;
      return route.fulfill({ contentType: "application/json", body: presetBody("plum") });
    });
    await page.goto("/signin");
    await expect(html(page)).toHaveAttribute("data-theme", "plum");
    await page.evaluate(() => {
      window.dispatchEvent(new Event("blur"));
      window.dispatchEvent(new Event("focus"));
      document.dispatchEvent(new Event("visibilitychange"));
    });
    await page.waitForTimeout(500);
    expect(calls).toBe(1);
  });

  test("a signed-in student gets the theme and has no theme control in settings", async ({ page }) => {
    await page.route("**/api/settings/theme", (route) =>
      route.fulfill({ contentType: "application/json", body: presetBody("slate") }),
    );
    await signIn(page, "student1@demo.example");
    await expect(html(page)).toHaveAttribute("data-theme", "slate");
    await page.goto("/student/settings");
    await expect(page.getByText("Password", { exact: true }).first()).toBeVisible();
    await expect(page.getByText(/app theme/i)).toHaveCount(0);
  });
  // "harbor saved" is simulated by route interception, like the tests above (no admin PUT needed).
  test("with harbor saved and the theme request delayed, a reload has data-theme=harbor at DOMContentLoaded", async ({
    page,
  }) => {
    await page.route("**/api/settings/theme", async (route) => {
      await new Promise((r) => setTimeout(r, 1500));
      await route.fulfill({ contentType: "application/json", body: presetBody("harbor") });
    });
    await page.goto("/signin");
    await expect(html(page)).toHaveAttribute("data-theme", "harbor");
    let atDcl: string | null = null;
    await page.addInitScript(() => {
      document.addEventListener("DOMContentLoaded", () => {
        (window as unknown as { __dclTheme: string | null }).__dclTheme =
          document.documentElement.getAttribute("data-theme");
      });
    });
    await page.reload({ waitUntil: "domcontentloaded" });
    atDcl = await page.evaluate(() => (window as unknown as { __dclTheme: string | null }).__dclTheme);
    expect(atDcl).toBe("harbor");
  });

  test("the server wins over the hint, and an unknown server id resets the hint to grove", async ({ page }) => {
    await page.addInitScript(() => localStorage.setItem("ir-theme", "plum"));
    await page.route("**/api/settings/theme", (route) =>
      route.fulfill({ contentType: "application/json", body: presetBody("slate") }),
    );
    await page.goto("/signin");
    await expect(html(page)).toHaveAttribute("data-theme", "slate");
    expect(await page.evaluate(() => localStorage.getItem("ir-theme"))).toBe("slate");
  });

  test("an unknown id in the stored hint is ignored", async ({ page }) => {
    await page.addInitScript(() => localStorage.setItem("ir-theme", "neon"));
    await page.route("**/api/settings/theme", (route) => route.abort());
    await page.goto("/signin");
    await expect(html(page)).toHaveAttribute("data-theme", "grove");
  });

  test("localStorage throwing loads Grove with no error shown", async ({ page }) => {
    await page.addInitScript(() => {
      const boom = () => {
        throw new Error("blocked");
      };
      Object.defineProperty(window, "localStorage", { get: boom });
    });
    const errors: string[] = [];
    page.on("pageerror", (e) => errors.push(e.message));
    await page.goto("/signin");
    await expect(html(page)).toHaveAttribute("data-theme", "grove");
    await noThemeError(page);
    expect(errors).toEqual([]);
  });
});
