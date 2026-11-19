import { describe, expect, it } from "vitest";
import { isRetryable, statusCopy } from "./error-copy";

describe("statusCopy", () => {
  it("has written copy for every status a page can receive", () => {
    expect(statusCopy(400)).toBe("Please check what you entered and try again.");
    expect(statusCopy(401)).toBe("Your session expired. Please sign in again.");
    expect(statusCopy(403)).toBe("You don't have access to this.");
    expect(statusCopy(404)).toBe("We couldn't find that.");
    expect(statusCopy(409)).toBe("That can't be done right now.");
    expect(statusCopy(429)).toBe("Too many attempts. Try again in 15 minutes.");
    expect(statusCopy(500)).toBe("Something went wrong on our side. Please try again.");
    expect(statusCopy(503)).toBe("Something went wrong on our side. Please try again.");
    expect(statusCopy(0)).toBe("We couldn't reach the server. Check your connection and try again.");
  });
  it("never leaks backend text for unknown statuses", () => {
    expect(statusCopy(418)).toBe("Something went wrong. Please try again.");
  });
});

describe("isRetryable", () => {
  it("is true for 5xx and network failures only", () => {
    expect(isRetryable({ status: 0 })).toBe(true);
    expect(isRetryable({ status: 502 })).toBe(true);
    expect(isRetryable({ status: 404 })).toBe(false);
    expect(isRetryable({ status: 403 })).toBe(false);
    expect(isRetryable(new Error("boom"))).toBe(false);
    expect(isRetryable(null)).toBe(false);
  });
});
