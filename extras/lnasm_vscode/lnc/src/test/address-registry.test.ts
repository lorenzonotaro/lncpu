import { strict as assert } from "node:assert";
import { test } from "node:test";

import { AddressRegistry } from "../debug/address-registry";

test("replaces active symbols and clears them at session end", () => {
  // Given
  const registry = new AddressRegistry();
  registry.replace(new Map([["first", 0x10]]));

  // When
  registry.replace(new Map([["second", 0x20]]));

  // Then
  assert.equal(registry.resolve("first"), undefined);
  assert.equal(registry.resolve("second"), 0x20);
  registry.clear();
  assert.equal(registry.resolve("second"), undefined);
});
