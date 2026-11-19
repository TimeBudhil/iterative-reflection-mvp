import { expect, test } from "@playwright/test";

// Needs the backend running at BACKEND_URL (see README).
test("GET /api/me reaches the backend through the rewrite", async ({ request }) => {
  const res = await request.get("/api/me");
  expect(res.status()).toBe(401);
  expect(res.headers()["content-type"]).toContain("problem+json");
});
