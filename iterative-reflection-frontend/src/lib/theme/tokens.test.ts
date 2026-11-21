// @vitest-environment node
import { readdirSync, readFileSync } from "node:fs";
import { join } from "node:path";
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
  "destructive-foreground",
  "ring",
] as const;
type Slot = (typeof SLOTS)[number];

/** Each preset is a selector whose block defines the slot values. Update 2 adds [data-theme] entries. */
const PRESETS: { name: string; selector: string }[] = [
  { name: "grove", selector: ":root" },
  { name: "grove (by name)", selector: '[data-theme="grove"]' },
  { name: "harbor", selector: '[data-theme="harbor"]' },
  { name: "plum", selector: '[data-theme="plum"]' },
  { name: "slate", selector: '[data-theme="slate"]' },
];

/** Pairs (text, ground) that must reach WCAG AA for normal text. */
const AA_PAIRS: [Slot, Slot][] = [
  ["foreground", "background"],
  ["foreground", "surface"],
  ["primary-foreground", "primary"],
  ["highlight-text", "background"],
  ["highlight-text", "surface"],
  ["positive-foreground", "positive"],
  ["destructive-foreground", "destructive"], // also the Denied outcome badge
  // Verdict chips: YES = positive pair above, MAYBE = highlight-text on a highlight tint (below), NO = muted pair.
  ["muted-foreground", "muted"],
  ["muted-foreground", "background"],
  ["muted-foreground", "surface"],
];

function blockFor(selector: string): string {
  const escaped = selector.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
  const match = new RegExp(`(?:^|\\n)(?:[^{}]*,\\s*)?${escaped}\\s*(?:,[^{}]*)?\\{([^}]*)\\}`).exec(css);
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

/** Composites `fg` at `alpha` over opaque `bg`; returns hex. */
function blend(fg: string, bg: string, alpha: number): string {
  const ch = (i: number) => {
    const f = parseInt(fg.slice(i, i + 2), 16);
    const b = parseInt(bg.slice(i, i + 2), 16);
    return Math.round(f * alpha + b * (1 - alpha)).toString(16).padStart(2, "0");
  };
  return `#${ch(1)}${ch(3)}${ch(5)}`;
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

describe.each(PRESETS)("$name MAYBE chip", ({ selector }) => {
  const v = slotValues(selector);
  // The chip paints highlight at 10% over an opaque surface layer (not over whatever is behind it).
  it("highlight-text on the highlight tint over surface meets WCAG AA (4.5:1)", () => {
    expect(contrast(v["highlight-text"], blend(v.highlight, v.surface, 0.1))).toBeGreaterThanOrEqual(4.5);
  });
  it("a selected verdict's ring (foreground) is a 3:1 indicator against background and surface", () => {
    expect(contrast(v.foreground, v.background)).toBeGreaterThanOrEqual(3);
    expect(contrast(v.foreground, v.surface)).toBeGreaterThanOrEqual(3);
  });
});

describe("raw color guard", () => {
  const srcRoot = fileURLToPath(new URL("../..", import.meta.url));
  const PALETTES =
    "slate|gray|zinc|neutral|stone|red|orange|amber|yellow|lime|green|emerald|teal|cyan|sky|blue|indigo|violet|purple|fuchsia|pink|rose";
  const RAW = [
    { name: "hex value", re: /#[0-9a-fA-F]{3,8}\b/ },
    { name: "rgb()/rgba()", re: /\brgba?\(/ },
    { name: "oklch()", re: /\boklch\(/ },
    { name: "hsl()/hsla()", re: /\bhsla?\(/ },
    {
      name: "Tailwind palette class",
      re: new RegExp(
        `\\b[a-z-]*(?:bg|text|border|ring|fill|stroke|from|via|to|divide|outline|decoration|accent|caret|shadow)-(?:${PALETTES})-\\d{2,3}\\b`,
      ),
    },
  ];

  function sources(dir: string): string[] {
    return readdirSync(dir, { withFileTypes: true }).flatMap((e) => {
      const p = join(dir, e.name);
      if (e.isDirectory()) return sources(p);
      if (!/\.tsx?$/.test(e.name) || /\.(test|spec)\.tsx?$/.test(e.name) || e.name.endsWith(".d.ts")) return [];
      return [p];
    });
  }

  it("finds no raw colors in component or page source (globals.css is exempt)", () => {
    const offenders: string[] = [];
    for (const file of sources(srcRoot)) {
      readFileSync(file, "utf8")
        .split("\n")
        .forEach((line, i) => {
          for (const { name, re } of RAW) {
            if (re.test(line)) offenders.push(`${file.slice(srcRoot.length)}:${i + 1} ${name}: ${line.trim()}`);
          }
        });
    }
    expect(offenders).toEqual([]);
  });
});

describe("Grove values", () => {
  it("uses the specified brand colors", () => {
    const v = slotValues(":root");
    expect(v.background.toLowerCase()).toBe("#edefeb");
    expect(v.surface.toLowerCase()).toBe("#ffffff");
    expect(v.foreground.toLowerCase()).toBe("#121511");
    expect(v.primary).toBe("#315c3b");
    expect(v["primary-foreground"].toLowerCase()).toBe("#ffffff");
    expect(v.highlight.toLowerCase()).toBe("#e5c96b");
    expect(v["highlight-text"].toLowerCase()).toBe("#7f6716");
    expect(v.positive.toLowerCase()).toBe("#d6e4e8");
    expect(v["positive-foreground"].toLowerCase()).toBe("#315c3b");
    expect(v["destructive-foreground"].toLowerCase()).toBe("#ffffff");
  });
});
