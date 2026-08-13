import { strict as assert } from "node:assert";
import { test } from "node:test";

import { scopesForRamVariables, variablesForReference, RAM_VARIABLES_REFERENCE, REGISTERS_VARIABLES_REFERENCE } from "../debug/dap-variables";

const RAM_VARIABLES = [{ name: "buffer", address: 0x2000, byteLength: 12 }] as const;

test("adds the RAM scope only when mapped RAM variables exist", () => {
  // Given / When / Then
  assert.deepEqual(scopesForRamVariables([]), [
    { name: "Registers", variablesReference: REGISTERS_VARIABLES_REFERENCE, expensive: false },
  ]);
  assert.deepEqual(scopesForRamVariables(RAM_VARIABLES), [
    { name: "Registers", variablesReference: REGISTERS_VARIABLES_REFERENCE, expensive: false },
    { name: "RAM", variablesReference: RAM_VARIABLES_REFERENCE, expensive: false },
  ]);
});

test("models RAM labels as DAP memory variables without loading registers", async () => {
  // Given
  let registerLoads = 0;

  // When
  const variables = await variablesForReference(RAM_VARIABLES_REFERENCE, RAM_VARIABLES, () => {
    registerLoads += 1;
    return Promise.resolve("RA=01");
  });

  // Then
  assert.equal(registerLoads, 0);
  assert.deepEqual(variables, [{
    name: "buffer",
    value: "0x2000",
    type: "RAM",
    memoryReference: "0x2000",
    byteLength: 12,
    variablesReference: 0,
  }]);
});

test("loads emulator registers only for the Registers reference", async () => {
  // Given
  let registerLoads = 0;

  // When
  const variables = await variablesForReference(REGISTERS_VARIABLES_REFERENCE, RAM_VARIABLES, () => {
    registerLoads += 1;
    return Promise.resolve("RA=01 RB=ff");
  });

  // Then
  assert.equal(registerLoads, 1);
  assert.deepEqual(variables, [
    { name: "RA", value: "0x01", variablesReference: 0, evaluateName: "RA" },
    { name: "RB", value: "0xff", variablesReference: 0, evaluateName: "RB" },
  ]);
});

test("rejects an unknown Variables reference", async () => {
  // Given / When / Then
  await assert.rejects(
    variablesForReference(99, RAM_VARIABLES, () => Promise.resolve("RA=01")),
    /unknown variables reference/,
  );
});
