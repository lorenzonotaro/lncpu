import { strict as assert } from "node:assert";
import { test } from "node:test";

import { parseImmediateListing } from "../debug/immediate-listing";

test("resolves section origins and labels emitted on instruction lines", () => {
  // Given
  const listing = [
    "######## Section 'TEXT', origin at 0x001000 (size = 0x00020)",
    "                         entry:  001002:\t01 (hlt)",
    "             entry$_loop:       001003:\t01 (hlt)",
  ].join("\n");

  // When
  const symbols = parseImmediateListing(listing);

  // Then
  assert.equal(symbols.get("TEXT"), 0x1000);
  assert.equal(symbols.get("entry"), 0x1002);
  assert.equal(symbols.get("entry$_loop"), 0x1003);
});

test("assigns one instruction address to every comma-separated label", () => {
  // Given / When
  const symbols = parseImmediateListing("RAM_SIGNATURE, DDI_MAGIC_BYTES:  0003ec:\t4c (mov_rd_ird)");

  // Then
  assert.equal(symbols.get("RAM_SIGNATURE"), 0x3ec);
  assert.equal(symbols.get("DDI_MAGIC_BYTES"), 0x3ec);
});

test("keeps malformed, unlisted, and out-of-range symbols absent", () => {
  // Given
  const listing = [
    "virtual_only:",
    "short_address: 1234: 01",
    "bad_hex: 0000xz: 01",
    "too_large: 1000000: 01",
    "######## Section 'BROKEN', origin at 0x1000000 (size = 0x1)",
  ].join("\n");

  // When / Then
  assert.deepEqual([...parseImmediateListing(listing)], []);
});
