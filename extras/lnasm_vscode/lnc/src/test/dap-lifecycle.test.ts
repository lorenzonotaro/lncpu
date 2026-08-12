import { strict as assert } from "node:assert";
import { test } from "node:test";

import { DapLifecycle, type DapOutput, type DapRuntime } from "../debug/dap-lifecycle";
import { DebugMapIndex } from "../debug/debug-map";
import type { LaunchSettings } from "../debug/runtime";

class ControlledRuntime implements DapRuntime {
  readonly commands: string[] = [];
  private launchResolve?: (map: DebugMapIndex) => void;

  launch(_settings: LaunchSettings): Promise<DebugMapIndex> {
    return new Promise((resolve) => { this.launchResolve = resolve; });
  }

  completeLaunch(): void {
    this.launchResolve?.(DebugMapIndex.parse({ version: 1, files: ["/work/main.lnc"], lines: [{ a: 0, s: 1, f: 0, l: 1, c: 1, sec: "text" }], labels: [] }));
  }

  command(command: string): Promise<string> {
    this.commands.push(command);
    return Promise.resolve("");
  }
}

function launchArguments(stopOnEntry: boolean): Record<string, unknown> {
  return { cwd: "/work", lncPath: "/tools/lnc", javaPath: "java", emulatorPath: "/tools/emu", sourceFiles: ["main.lnc"], compilerOptions: [], emulatorOptions: [], stopOnEntry };
}

function outputNames(output: readonly DapOutput[]): readonly string[] {
  return output.map((message) => message.kind === "event" ? message.event : message.command);
}

function isResponse(output: DapOutput): output is Extract<DapOutput, { readonly kind: "response" }> {
  return output.kind === "response";
}

test("does not emit initialized before launch succeeds", async () => {
  // Given
  const runtime = new ControlledRuntime();
  const output: DapOutput[] = [];
  const lifecycle = new DapLifecycle(runtime, (message) => output.push(message));

  // When
  const launch = lifecycle.handle({ seq: 1, command: "launch", arguments: launchArguments(true) });
  await Promise.resolve();

  // Then
  assert.deepEqual(output, []);
  runtime.completeLaunch();
  await launch;
  assert.deepEqual(outputNames(output), ["launch", "initialized"]);
});

test("queues configurationDone behind launch and responds once after auto-continue", async () => {
  // Given
  const runtime = new ControlledRuntime();
  const output: DapOutput[] = [];
  const lifecycle = new DapLifecycle(runtime, (message) => output.push(message));

  // When
  const launch = lifecycle.handle({ seq: 1, command: "launch", arguments: launchArguments(false) });
  const configured = lifecycle.handle({ seq: 2, command: "configurationDone", arguments: {} });
  await Promise.resolve();
  runtime.completeLaunch();
  await Promise.all([launch, configured]);

  // Then
  assert.deepEqual(runtime.commands, ["continue"]);
  const configurationResponses = output.filter(isResponse).filter((message) => message.command === "configurationDone");
  assert.equal(configurationResponses.length, 1);
  assert.equal(configurationResponses[0]?.success, true);
});
