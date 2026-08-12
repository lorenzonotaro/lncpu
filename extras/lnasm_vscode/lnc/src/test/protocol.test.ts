import { strict as assert } from "node:assert";
import { createServer } from "node:net";
import { test } from "node:test";

import { LineFramer, LnDbgClient, parseLnDbgFrame } from "../debug/protocol";

test("emits complete correlated frames when TCP chunks split lines", () => {
  // Given
  const framer = new LineFramer();

  // When
  const first = framer.push(Buffer.from("1 ok LNDBG"));
  const second = framer.push(Buffer.from(" 1\r\n! stopped step 00af\n2 err target is running\n"));

  // Then
  assert.deepEqual(first, []);
  assert.deepEqual(second.map(parseLnDbgFrame), [
    { kind: "reply", id: 1, ok: true, payload: "LNDBG 1" },
    { kind: "stopped", reason: "step", pc: 0x00af },
    { kind: "reply", id: 2, ok: false, payload: "target is running" },
  ]);
});

test("emits the exited event", () => {
  // Given / When
  const frame = parseLnDbgFrame("! exited 0");

  // Then
  assert.deepEqual(frame, { kind: "exited", code: 0 });
});

test("turns malformed server frames into a fatal client error", async (context) => {
  // Given
  const server = createServer((socket) => socket.once("data", () => socket.write("malformed\n")));
  await new Promise<void>((resolve) => server.listen(0, "127.0.0.1", resolve));
  context.after(() => server.close());
  const address = server.address();
  if (address === null || typeof address === "string") throw new Error("test server did not bind TCP");
  const client = await LnDbgClient.connect(address.port, { connectTimeoutMs: 100, requestTimeoutMs: 100 });
  context.after(() => client.close());

  // When / Then
  await assert.rejects(client.command("hello"), /invalid LNDBG frame/);
});

test("bounds unanswered LNDBG requests", async (context) => {
  // Given
  const server = createServer();
  server.on("connection", () => undefined);
  await new Promise<void>((resolve) => server.listen(0, "127.0.0.1", resolve));
  context.after(() => server.close());
  const address = server.address();
  if (address === null || typeof address === "string") throw new Error("test server did not bind TCP");
  const client = await LnDbgClient.connect(address.port, { connectTimeoutMs: 100, requestTimeoutMs: 20 });
  context.after(() => client.close());

  // When / Then
  await assert.rejects(client.command("hello"), /timed out/);
});

test("rejects an oversized incomplete frame", () => {
  // Given
  const framer = new LineFramer(8);

  // When / Then
  assert.throws(() => framer.push(Buffer.from("123456789")), /frame exceeds/);
});
