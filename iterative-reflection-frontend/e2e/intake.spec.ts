import { expect, test, type Page } from "@playwright/test";
import { seedLinkedStudent } from "./support/seed-student";

// Needs the dev backend. Students are created and brought to N reflections through the real API.

async function signIn(page: Page, email: string, password: string) {
  await page.goto("/signin");
  await page.getByLabel("Email").fill(email);
  await page.getByLabel("Password").fill(password);
  await page.getByRole("button", { name: "Sign in", exact: true }).click();
  await expect(page).toHaveURL(/\/student\/feed$/);
}

test("new intake: chips grouped by category, POST, toast, back to the feed; then edit via Settings with PUT", async ({ page }) => {
  const s = await seedLinkedStudent({ reflections: 3, abandon: false, intake: false });
  await signIn(page, s.email, s.password);
  // The prompt modal appears; "Fill it in" opens the intake.
  await page.getByRole("button", { name: "Fill it in" }).click();
  await expect(page).toHaveURL(/\/student\/intake$/);

  await expect(page.getByRole("group", { name: "Cluster" })).toBeVisible();
  await expect(page.getByRole("group", { name: "Education Path" })).toBeVisible();
  await expect(page.getByTestId("intake-interests").getByRole("button", { pressed: false })).toHaveCount(31);
  const save = page.getByRole("button", { name: "Save interests" });
  await expect(save).toBeDisabled();

  await page.getByRole("button", { name: "Health & Medicine" }).click();
  await page.getByRole("button", { name: "Team-Based" }).click();
  await page.getByLabel("What are your goals for after school?").fill("Become a nurse");
  const methods: string[] = [];
  page.on("request", (r) => r.url().endsWith("/api/intake") && r.method() !== "GET" && methods.push(r.method()));
  await save.click();
  await expect(page.getByText("Thanks for sharing your interests.")).toBeVisible();
  await expect(page).toHaveURL(/\/student\/feed$/);
  expect(methods).toEqual(["POST"]);

  // Settings Interests card says Completed; edit prefills and saves with PUT, replacing everything.
  await page.goto("/student/settings");
  await expect(page.getByTestId("interests-status")).toHaveText("Completed");
  await page.getByRole("link", { name: "Edit interests" }).click();
  await expect(page.getByRole("button", { name: "Health & Medicine" })).toHaveAttribute("aria-pressed", "true");
  await expect(page.getByLabel("What are your goals for after school?")).toHaveValue("Become a nurse");
  await page.getByRole("button", { name: "Health & Medicine" }).click(); // deselect
  await page.getByLabel("What are your goals for after school?").fill("");
  await page.getByLabel("What are your favorite school subjects?").fill("Biology");
  methods.length = 0;
  await page.getByRole("button", { name: "Save changes" }).click();
  await expect(page.getByText("Your interests are updated.")).toBeVisible();
  await expect(page).toHaveURL(/\/student\/settings$/);
  expect(methods).toEqual(["PUT"]);

  const mine = await (await page.request.get("/api/intake")).json();
  const keys = mine.answers.map((a: { key: string }) => a.key).sort();
  expect(keys).toEqual(["interests", "subjects"]); // goals was cleared, i.e. replaced not merged
});

test("the prompt modal: 'Not now' dismisses and does not come back on reload", async ({ page }) => {
  const s = await seedLinkedStudent({ reflections: 3, abandon: false, intake: false });
  await signIn(page, s.email, s.password);
  await expect(page.getByTestId("intake-prompt")).toBeVisible();
  await page.getByRole("button", { name: "Not now" }).click();
  await expect(page.getByTestId("intake-prompt")).toBeHidden();
  await page.reload();
  await expect(page.getByTestId("career-card")).toBeVisible();
  await expect(page.getByTestId("intake-prompt")).toHaveCount(0);
});

test("the prompt modal: 'Fill it in' also stops the prompt returning", async ({ page }) => {
  const s = await seedLinkedStudent({ reflections: 3, abandon: false, intake: false });
  await signIn(page, s.email, s.password);
  await page.getByRole("button", { name: "Fill it in" }).click();
  await expect(page).toHaveURL(/\/student\/intake$/);
  await page.goto("/student/feed");
  await expect(page.getByTestId("career-card")).toBeVisible();
  await expect(page.getByTestId("intake-prompt")).toHaveCount(0);
});

test("a student with fewer than 3 reflections gets no modal; submitting shows the not-yet-available panel (409)", async ({ page }) => {
  const s = await seedLinkedStudent({ reflections: 1, abandon: false, intake: false });
  await signIn(page, s.email, s.password);
  await expect(page.getByTestId("career-card")).toBeVisible();
  await expect(page.getByTestId("intake-prompt")).toHaveCount(0);

  await page.goto("/student/settings");
  await expect(page.getByTestId("interests-status")).toHaveText("Not filled in");
  await page.getByRole("link", { name: "Fill in interests" }).click();
  await page.getByRole("button", { name: "Health & Medicine" }).click();
  await page.getByRole("button", { name: "Save interests" }).click();
  await expect(page.getByTestId("intake-unavailable")).toHaveText("The intake opens after your 3rd reflection.");
  await expect(page.getByRole("button", { name: "Save interests" })).toHaveCount(0);
});

test("a 400 from the server shows next to the question", async ({ page }) => {
  const s = await seedLinkedStudent({ reflections: 3, abandon: false, intake: false });
  await signIn(page, s.email, s.password);
  await page.getByRole("button", { name: "Not now" }).click();
  await page.goto("/student/intake");
  await page.route("**/api/intake", (route) =>
    route.request().method() === "POST"
      ? route.fulfill({
          status: 400,
          contentType: "application/problem+json",
          body: JSON.stringify({ title: "Bad Request", status: 400, detail: "Validation failed.", errors: [{ field: "answers[goals].text", message: "Must be at most 2000 characters." }] }),
        })
      : route.fallback(),
  );
  await page.getByLabel("What are your goals for after school?").fill("x");
  await page.getByRole("button", { name: "Save interests" }).click();
  await expect(page.getByTestId("intake-goals").getByRole("alert")).toHaveText("Must be at most 2000 characters.");
});
