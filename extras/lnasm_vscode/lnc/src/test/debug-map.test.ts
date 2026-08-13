import { strict as assert } from "node:assert";
import { test } from "node:test";

import { DebugMapIndex } from "../debug/debug-map";

const BASE_MAP = {
  version: 1,
  files: ["/work/main.lnc"],
  lines: [],
  labels: [],
};

test("resolves a breakpoint to the exact or next executable source line", () => {
  // Given
  const index = DebugMapIndex.parse({
    version: 1,
    files: ["/work/main.lnc"],
    lines: [
      { a: 0x10, s: 2, f: 0, l: 4, c: 1, sec: "text" },
      { a: 0x12, s: 3, f: 0, l: 8, c: 3, sec: "text" },
    ],
    labels: [{ name: "main", a: 0x10, sec: "text" }],
  });

  // When
  const exact = index.breakpoint("/work/main.lnc", 4);
  const next = index.breakpoint("/work/main.lnc", 5);

  // Then
  assert.deepEqual(exact, { address: 0x10, line: 4, column: 1 });
  assert.deepEqual(next, { address: 0x12, line: 8, column: 3 });
});

test("maps a program counter to source and its nearest label", () => {
  // Given
  const index = DebugMapIndex.parse({
    version: 1,
    files: ["/work/main.lnc"],
    lines: [{ a: 0x20, s: 4, f: 0, l: 12, c: 2, sec: "text" }],
    labels: [{ name: "loop", a: 0x20, sec: "text" }],
  });

  // When
  const location = index.location(0x22);

  // Then
  assert.deepEqual(location, { path: "/work/main.lnc", line: 12, column: 2, label: "loop" });
});

test("builds symbols for loaded physical targets with labels winning collisions", () => {
  // Given
  const index = DebugMapIndex.parse({
    ...BASE_MAP,
    sections: [
      { name: "TEXT", target: "ROM", a: 0x1000, s: 0x20 },
      { name: "DATA", target: "RAM", a: 0x2200, s: 0x10 },
      { name: "DEVICE", target: "D0", a: 0x4040, s: 0x08 },
    ],
    labels: [
      { name: "ram_value", a: 0x2204, sec: "DATA" },
      { name: "device_value", a: 0x4042, sec: "DEVICE" },
      { name: "TEXT", a: 0x1002, sec: "TEXT" },
    ],
  });

  // When
  const symbols = index.symbolsForTargets(new Set(["ROM", "RAM", "D0"]));

  // Then
  assert.deepEqual(symbols === undefined ? undefined : [...symbols], [
    ["TEXT", 0x1002],
    ["DATA", 0x2200],
    ["DEVICE", 0x4040],
    ["ram_value", 0x2204],
    ["device_value", 0x4042],
  ]);
});

test("filters sections and labels to targets loaded by the launch plan", () => {
  // Given
  const index = DebugMapIndex.parse({
    ...BASE_MAP,
    sections: [
      { name: "TEXT", target: "ROM", a: 0x1000, s: 1 },
      { name: "DATA", target: "RAM", a: 0x2200, s: 1 },
    ],
    labels: [
      { name: "rom_label", a: 0x1000, sec: "TEXT" },
      { name: "ram_label", a: 0x2200, sec: "DATA" },
      { name: "virtual_label", a: 0x3000, sec: "__VIRTUAL__" },
      { name: "missing_section", a: 0x4000, sec: "MISSING" },
    ],
  });

  // When
  const symbols = index.symbolsForTargets(new Set(["ROM"]));

  // Then
  assert.deepEqual(symbols === undefined ? undefined : [...symbols], [
    ["TEXT", 0x1000],
    ["rom_label", 0x1000],
  ]);
});

test("derives deterministic positive RAM label ranges within emitted sections", () => {
  // Given
  const index = DebugMapIndex.parse({
    ...BASE_MAP,
    sections: [
      { name: "LATE", target: "RAM", a: 0x2300, s: 0x08 },
      { name: "DATA", target: "RAM", a: 0x2200, s: 0x20 },
      { name: "TEXT", target: "ROM", a: 0x1000, s: 0x20 },
    ],
    labels: [
      { name: "second_alias", a: 0x2208, sec: "DATA" },
      { name: "first", a: 0x2200, sec: "DATA" },
      { name: "first$member", a: 0x2204, sec: "DATA" },
      { name: "second", a: 0x2208, sec: "DATA" },
      { name: "outside", a: 0x2220, sec: "DATA" },
      { name: "late", a: 0x2302, sec: "LATE" },
      { name: "rom", a: 0x1000, sec: "TEXT" },
    ],
  });

  // When
  const variables = index.ramMemoryVariables(new Set(["ROM", "RAM"]));

  // Then
  assert.deepEqual(variables, [
    { name: "first", address: 0x2200, byteLength: 0x08 },
    { name: "second", address: 0x2208, byteLength: 0x18 },
    { name: "late", address: 0x2302, byteLength: 0x06 },
  ]);
});

test("omits RAM variables for unloaded RAM and legacy maps", () => {
  // Given
  const current = DebugMapIndex.parse({
    ...BASE_MAP,
    sections: [{ name: "DATA", target: "RAM", a: 0x2000, s: 4 }],
    labels: [{ name: "value", a: 0x2000, sec: "DATA" }],
  });
  const legacy = DebugMapIndex.parse({
    ...BASE_MAP,
    labels: [{ name: "value", a: 0x2000, sec: "DATA" }],
  });

  // When
  const unloaded = current.ramMemoryVariables(new Set(["ROM"]));
  const metadataAbsent = legacy.ramMemoryVariables(new Set(["RAM"]));

  // Then
  assert.deepEqual(unloaded, []);
  assert.deepEqual(metadataAbsent, []);
});

test("distinguishes an empty sections table from a legacy version 1 map", () => {
  // Given
  const current = DebugMapIndex.parse({ ...BASE_MAP, sections: [] });
  const legacy = DebugMapIndex.parse(BASE_MAP);

  // When
  const currentSymbols = current.symbolsForTargets(new Set(["ROM"]));
  const legacySymbols = legacy.symbolsForTargets(new Set(["ROM"]));

  // Then
  assert.deepEqual(currentSymbols === undefined ? undefined : [...currentSymbols], []);
  assert.equal(legacySymbols, undefined);
});

test("rejects a present malformed sections table", () => {
  // Given
  const malformedSections: readonly unknown[] = [
    "not-an-array",
    [{ name: "", target: "ROM", a: 0, s: 1 }],
    [{ name: "TEXT", target: "__VIRTUAL__", a: 0, s: 1 }],
    [{ name: "TEXT", target: "VRAM", a: 0, s: 1 }],
    [{ name: "TEXT", target: "ROM", a: -1, s: 1 }],
    [{ name: "TEXT", target: "ROM", a: 0x10000, s: 0 }],
    [{ name: "TEXT", target: "ROM", a: 0xffff, s: 2 }],
    [{ name: "TEXT", target: "ROM", a: 0, s: -1 }],
  ];

  // When / Then
  for (const sections of malformedSections) {
    assert.throws(() => DebugMapIndex.parse({ ...BASE_MAP, sections }), /debug map/);
  }
});

test("rejects sections outside their declared physical target range", () => {
  // Given
  const invalidTargetRanges = [
    [{ name: "RAM_IN_ROM", target: "RAM", a: 0x1000, s: 1 }],
    [{ name: "D0_OVERFLOW", target: "D0", a: 0x5fff, s: 2 }],
  ];

  // When / Then
  for (const sections of invalidTargetRanges) {
    assert.throws(() => DebugMapIndex.parse({ ...BASE_MAP, sections }), /debug map/);
  }
});
