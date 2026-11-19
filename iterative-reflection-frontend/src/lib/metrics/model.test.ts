import { describe, expect, it } from "vitest";
import { barWidths, rangeFromParams, rangeToSearch, rangeToQuery } from "./model";

describe("rangeFromParams", () => {
  it("reads valid from/to and ignores junk", () => {
    expect(rangeFromParams(new URLSearchParams("from=2026-01-02&to=2026-02-03"))).toEqual({ from: "2026-01-02", to: "2026-02-03" });
    expect(rangeFromParams(new URLSearchParams("from=nope&to="))).toEqual({ from: "", to: "" });
    expect(rangeFromParams(new URLSearchParams("from=2026-13-40"))).toEqual({ from: "", to: "" });
  });
});

describe("rangeToSearch / rangeToQuery", () => {
  it("omits empty bounds so clearing returns to all-time", () => {
    expect(rangeToSearch({ from: "", to: "" })).toBe("");
    expect(rangeToSearch({ from: "2026-01-02", to: "" })).toBe("?from=2026-01-02");
    expect(rangeToSearch({ from: "2026-01-02", to: "2026-02-03" })).toBe("?from=2026-01-02&to=2026-02-03");
    expect(rangeToQuery({ from: "", to: "2026-02-03" })).toEqual({ to: "2026-02-03" });
    expect(rangeToQuery({ from: "", to: "" })).toEqual({});
  });
});

describe("barWidths", () => {
  it("scales to the largest count and handles all zeros", () => {
    expect(barWidths({ YES: 4, MAYBE: 2, NO: 0 })).toEqual({ YES: 100, MAYBE: 50, NO: 0 });
    expect(barWidths({ YES: 0, MAYBE: 0, NO: 0 })).toEqual({ YES: 0, MAYBE: 0, NO: 0 });
  });
});
