import { describe, expect, it } from "vitest";
import { actionLabel, detailText, tabFromParam, tabToSearch } from "./activity-model";

describe("tabFromParam", () => {
  it("selects activity only for ?tab=activity", () => {
    expect(tabFromParam("activity")).toBe("activity");
    expect(tabFromParam(null)).toBe("views");
    expect(tabFromParam("nope")).toBe("views");
  });
});

describe("tabToSearch", () => {
  it("sets and clears the tab param, keeping others", () => {
    expect(tabToSearch("activity")).toBe("?tab=activity");
    expect(tabToSearch("views", "tab=activity")).toBe("");
    expect(tabToSearch("activity", "x=1")).toBe("?x=1&tab=activity");
  });
});

describe("detailText", () => {
  it("maps theme ids to display names", () => {
    expect(detailText("THEME_CHANGED", { from: "grove", to: "harbor" })).toBe("Theme: Grove → Harbor");
  });
  it("keeps an unknown id as is", () => {
    expect(detailText("THEME_CHANGED", { from: "grove", to: "neon" })).toBe("Theme: Grove → neon");
  });
  it("words the counselor actions", () => {
    expect(detailText("COUNSELOR_CREATED", { email: "a@b.c" })).toBe("Created counselor a@b.c");
    expect(detailText("COUNSELOR_DEACTIVATED", { email: "a@b.c" })).toBe("Deactivated counselor a@b.c");
    expect(detailText("COUNSELOR_REACTIVATED", { email: "a@b.c" })).toBe("Reactivated counselor a@b.c");
  });
  it("falls back for an unknown action", () => {
    expect(detailText("OTHER", { k: "v" })).toBe("v");
    expect(detailText(undefined, undefined)).toBe("–");
  });
});

describe("actionLabel", () => {
  it("is readable", () => {
    expect(actionLabel("COUNSELOR_CREATED")).toBe("Counselor created");
    expect(actionLabel("WEIRD")).toBe("WEIRD");
  });
});
