// @vitest-environment node
import { readFileSync } from "node:fs";
import { fileURLToPath } from "node:url";
import { describe, expect, it } from "vitest";

const css = readFileSync(
  fileURLToPath(new URL("../../app/globals.css", import.meta.url)),
  "utf8",
);

const SLOTS = [
  "background",
  "surface",
  "foreground",
  "muted",
  "muted-foreground",
  "border",
  "primary",
  "primary-foreground",
  "highlight",
  "highlight-text",
  "positive",
  "positive-foreground",
  "destructive",
  "ring",
] as const;
type Slot = (typeof SLOTS)[number];

/** Each preset is a selector whose block defines the slot values. Update 2 adds [data-theme] entries. */
const PRESETS: { name: string; selector: string }[] = [
  { name: "grove", selector: ":root" },
];

/** Pairs (text, ground) that must reach WCAG AA for normal text. */
const AA_PAIRS: [Slot, Slot][] = [
  ["foreground", "background"],
  ["foreground", "surface"],
  ["primary-foreground", "primary"],
  ["highlight-text", "background"],
  ["highlight-text", "surface"],
  ["positive-foreground", "positive"],
];

function blockFor(selector: string): string {
  const escaped = selector.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
  const match = new RegExp(`(?:^|\\n)${escaped}\\s*\\{([^}]*)\\}`).exec(css);
  if (!match) throw new Error(`No ${selector} block in globals.css`);
  return match[1];
}

function slotValues(selector: string): Record<Slot, string> {
  const block = blockFor(selector);
  const out = {} as Record<Slot, string>;
  for (const slot of SLOTS) {
    const m = new RegExp(`--${slot}:\\s*([^;]+);`).exec(block);
    if (!m) throw new Error(`Slot --${slot} is not defined in ${selector}`);
    out[slot] = m[1].trim();
  }
  return out;
}

function luminance(hex: string): number {
  const [r, g, b] = [1, 3, 5].map((i) => {
    const c = parseInt(hex.slice(i, i + 2), 16) / 255;
    return c <= 0.03928 ? c / 12.92 : ((c + 0.055) / 1.055) ** 2.4;
  });
  return 0.2126 * r + 0.7152 * g + 0.0722 * b;
}

function contrast(a: string, b: string): number {
  const [hi, lo] = [luminance(a), luminance(b)].sort((x, y) => y - x);
  return (hi + 0.05) / (lo + 0.05);
}

describe.each(PRESETS)("$name palette tokens", ({ selector }) => {
  const values = slotValues(selector);

  it.each(AA_PAIRS)("%s on %s meets WCAG AA (4.5:1)", (fg, bg) => {
    expect(contrast(values[fg], values[bg])).toBeGreaterThanOrEqual(4.5);
  });
});

describe("Grove values", () => {
  it("uses the specified brand colors", () => {
    const v = slotValues(":root");
    expect(v.background.toLowerCase()).toBe("#edefeb");
    expect(v.surface.toLowerCase()).toBe("#ffffff");
    expect(v.foreground.toLowerCase()).toBe("#121511");
    expect(v.primary).toBe("#163300");
    expect(v["primary-foreground"].toLowerCase()).toBe("#ffffff");
    expect(v.highlight.toLowerCase()).toBe("#f08952");
    expect(v["highlight-text"].toLowerCase()).toBe("#a25a0e");
    expect(v.positive.toLowerCase()).toBe("#d6e4e8");
    expect(v["positive-foreground"].toLowerCase()).toBe("#163300");
  });
});
