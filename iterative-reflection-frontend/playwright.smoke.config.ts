import { defineConfig, devices } from "@playwright/test";

// Smoke suite against the docker-compose rehearsal (deploy/README.md). No webServer: the stack must be up.
// Credentials come from the environment or, if unset, from deploy/.env (the bootstrap admin).
try {
  process.loadEnvFile("../deploy/.env" /* relative to the frontend folder, where pnpm runs */);
} catch {
  // no deploy/.env: rely on the environment
}

export default defineConfig({
  testDir: "./smoke",
  fullyParallel: false,
  workers: 1,
  use: {
    baseURL: process.env.SMOKE_BASE_URL ?? "https://localhost",
    // Playwright's Firefox and WebKit builds do not read the system trust store, so the mkcert CA is not trusted there.
    ignoreHTTPSErrors: true,
  },
  projects: [
    { name: "chromium", use: { ...devices["Desktop Chrome"] } },
    { name: "firefox", use: { ...devices["Desktop Firefox"] } },
    { name: "webkit", use: { ...devices["Desktop Safari"] } },
  ],
});
