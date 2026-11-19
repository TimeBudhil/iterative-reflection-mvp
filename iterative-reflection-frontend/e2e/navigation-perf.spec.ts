import { expect, test } from "@playwright/test";

// N1.2: client-side navigation between portal pages shows a skeleton or content within 500 ms. Local run; the
// budget is generous on a quiet machine, so a noisy one is allowed to take the median of a few trips.
const BUDGET_MS = 500;

test("portal navigation renders a heading or skeleton in under 500 ms (median)", async ({ page }) => {
  await page.goto("/signin");
  await page.getByLabel("Email").fill("student1@demo.example");
  await page.getByLabel("Password").fill("demo");
  await page.getByRole("button", { name: "Sign in", exact: true }).click();
  await expect(page).toHaveURL(/\/student\/feed$/);
  await expect(page.getByRole("heading", { name: "Feed" })).toBeVisible();

  const targets = [
    { link: "My Journey", path: "/student/journey" },
    { link: "Feed", path: "/student/feed" },
    { link: "Settings", path: "/student/settings" },
    { link: "Feed", path: "/student/feed" },
  ];
  const samples: number[] = [];
  for (const t of targets) {
    const ms = await page.evaluate(
      async ({ link, path }) => {
        const a = [...document.querySelectorAll("a")].find((el) => el.textContent?.trim() === link);
        if (!a) throw new Error(`no link ${link}`);
        const start = performance.now();
        a.click();
        return await new Promise<number>((resolve) => {
          const tick = () => {
            const painted = location.pathname === path && document.querySelector("main h1, main [aria-label='Loading'], main [data-slot='skeleton'], main h2");
            if (painted) return resolve(performance.now() - start);
            requestAnimationFrame(tick);
          };
          tick();
        });
      },
      t,
    );
    samples.push(ms);
    await page.waitForLoadState("networkidle");
  }
  console.log("navigation ms:", samples.map((s) => Math.round(s)).join(", "));
  const sorted = [...samples].sort((a, b) => a - b);
  expect(sorted[Math.floor(sorted.length / 2)]).toBeLessThan(BUDGET_MS);
});
