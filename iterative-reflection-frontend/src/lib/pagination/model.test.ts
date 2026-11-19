import { describe, expect, it } from "vitest";
import { pageSummary } from "./model";

describe("pageSummary", () => {
  it("describes a middle page", () => {
    const s = pageSummary({ size: 20, number: 1, totalElements: 45, totalPages: 3 });
    expect(s).toEqual({ from: 21, to: 40, total: 45, hasPrev: true, hasNext: true, pageNumber: 2, totalPages: 3 });
  });
  it("clamps the last page's end to the total", () => {
    const s = pageSummary({ size: 20, number: 2, totalElements: 45, totalPages: 3 });
    expect(s).toMatchObject({ from: 41, to: 45, hasPrev: true, hasNext: false });
  });
  it("handles an empty result", () => {
    const s = pageSummary({ size: 20, number: 0, totalElements: 0, totalPages: 0 });
    expect(s).toMatchObject({ from: 0, to: 0, total: 0, hasPrev: false, hasNext: false, totalPages: 1 });
  });
});
