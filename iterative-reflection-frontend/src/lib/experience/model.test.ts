import { describe, expect, it } from "vitest";
import { clampPage, isUnavailable, modeFor, startPage, viewerState } from "./model";

describe("startPage", () => {
  it("resumes at lastViewedPage when in progress", () => {
    expect(startPage("in-progress", { lastViewedPage: 2, lastPage: 3 })).toBe(2);
  });
  it("starts at 1 with no progress, or in review", () => {
    expect(startPage("in-progress", { lastViewedPage: 0, lastPage: 3 })).toBe(1);
    expect(startPage("in-progress", { lastPage: 3 })).toBe(1);
    expect(startPage("review", { lastViewedPage: 3, lastPage: 3 })).toBe(1);
  });
  it("never exceeds lastPage", () => {
    expect(startPage("in-progress", { lastViewedPage: 9, lastPage: 3 })).toBe(3);
  });
});

describe("modeFor", () => {
  it("is review when the career id is in the reflected list", () => {
    expect(modeFor(7, [{ careerId: 7 }, { careerId: 8 }])).toBe("review");
    expect(modeFor(9, [{ careerId: 7 }])).toBe("in-progress");
  });
});

describe("viewerState", () => {
  it("in progress: reflect only on the last page, no free navigation", () => {
    expect(viewerState("in-progress", 2, 3)).toMatchObject({ label: "Page 2 of 3", hasPrev: true, hasNext: true, showReflect: false, freeNavigation: false, showEditReflection: false });
    expect(viewerState("in-progress", 3, 3)).toMatchObject({ hasNext: false, showReflect: true });
    expect(viewerState("in-progress", 1, 3).hasPrev).toBe(false);
  });
  it("review: free navigation, edit reflection, never reflect", () => {
    expect(viewerState("review", 3, 3)).toMatchObject({ freeNavigation: true, showEditReflection: true, showReflect: false });
  });
});

describe("viewerState progress", () => {
  it("is page / lastPage: first page, last page, clamped", () => {
    expect(viewerState("in-progress", 1, 4).progress).toBe(0.25);
    expect(viewerState("review", 4, 4).progress).toBe(1);
    expect(viewerState("review", 9, 4).progress).toBe(1);
    expect(viewerState("review", 0, 4).progress).toBe(0.25);
    expect(viewerState("in-progress", 1, 0).progress).toBe(1);
  });
});

describe("clampPage / isUnavailable", () => {
  it("clamps into 1..last", () => {
    expect(clampPage(0, 3)).toBe(1);
    expect(clampPage(5, 3)).toBe(3);
  });
  it("treats 404 and 409 as unavailable", () => {
    expect(isUnavailable({ status: 404 })).toBe(true);
    expect(isUnavailable({ status: 409 })).toBe(true);
    expect(isUnavailable({ status: 500 })).toBe(false);
    expect(isUnavailable(null)).toBe(false);
  });
});
