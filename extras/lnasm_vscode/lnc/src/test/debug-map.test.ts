import { strict as assert } from "node:assert";
import { test } from "node:test";

import { DebugMapIndex } from "../debug/debug-map";

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
