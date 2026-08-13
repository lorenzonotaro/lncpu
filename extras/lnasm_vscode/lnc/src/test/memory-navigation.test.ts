import { strict as assert } from "node:assert";
import { test } from "node:test";

import { openMemoryAtSymbol, type MemoryNavigationHost } from "../memory-navigation";

test("opens a Variables-style memory reference for the active lncpu session", async () => {
  // Given
  const calls: Array<{ readonly command: string; readonly argument: unknown }> = [];
  const host: MemoryNavigationHost = {
    activeSession: () => ({ id: "session-1", type: "lncpu" }),
    execute: (command, argument) => { calls.push({ command, argument }); return Promise.resolve(); },
  };

  // When
  await openMemoryAtSymbol(host, { symbol: "entry", address: 0x1234 });

  // Then
  assert.deepEqual(calls, [
    {
      command: "memory-inspector.show",
      argument: { sessionId: "session-1", memoryReference: "0x1234" },
    },
    {
      command: "memory-inspector.show-variable",
      argument: {
        sessionId: "session-1",
        variable: { name: "entry", value: "0x1234", type: "address", variablesReference: 0, memoryReference: "0x1234" },
      },
    },
  ]);
});

test("rejects navigation when the active session is not lncpu", async () => {
  // Given
  const host: MemoryNavigationHost = {
    activeSession: () => ({ id: "session-2", type: "other" }),
    execute: () => Promise.resolve(),
  };

  // When / Then
  await assert.rejects(openMemoryAtSymbol(host, { symbol: "entry", address: 1 }), /active LNCPU/);
});
