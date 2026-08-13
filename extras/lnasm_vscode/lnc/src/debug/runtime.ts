import { spawn, type ChildProcessWithoutNullStreams } from "node:child_process";
import { mkdir, readFile } from "node:fs/promises";
import { isAbsolute, join, resolve } from "node:path";

import { emulatorArtifactArgs, immediateArtifactPaths, planCompilation, validateEmulatorOptions, type CompilationPlan } from "./artifacts";
import { activeAddresses } from "./address-registry";
import { DebugMapIndex } from "./debug-map";
import { ProcessFailureError, ProtocolError } from "./errors";
import { parseImmediateListing } from "./immediate-listing";
import { LineFramer, LnDbgClient, type LnDbgEvent } from "./protocol";

export type LaunchSettings = {
  readonly cwd: string;
  readonly lncPath: string;
  readonly javaPath: string;
  readonly emulatorPath: string;
  readonly sourceFiles: readonly string[];
  readonly compilerOptions: readonly string[];
  readonly emulatorOptions: readonly string[];
  readonly output?: (category: "stdout" | "stderr", text: string) => void;
};
export type LaunchResult = {
  readonly plan: CompilationPlan;
  readonly map: DebugMapIndex;
  readonly ramMemoryVariables: ReturnType<DebugMapIndex["ramMemoryVariables"]>;
};

function runCompiler(plan: CompilationPlan, settings: LaunchSettings): Promise<void> {
  return new Promise((resolveRun, rejectRun) => {
    const child = spawn(plan.command, plan.args, { cwd: settings.cwd });
    child.stdout.on("data", (chunk: Buffer) => settings.output?.("stdout", chunk.toString("utf8")));
    child.stderr.on("data", (chunk: Buffer) => settings.output?.("stderr", chunk.toString("utf8")));
    child.once("error", rejectRun);
    child.once("exit", (code) => code === 0 ? resolveRun() : rejectRun(new ProcessFailureError(plan.command, code)));
  });
}

function waitForPort(child: ChildProcessWithoutNullStreams, settings: LaunchSettings): Promise<number> {
  return new Promise((resolvePort, rejectPort) => {
    const framer = new LineFramer();
    const timer = setTimeout(() => rejectPort(new ProtocolError("emulator did not publish LNDBG-LISTEN")), 10_000);
    child.stdout.on("data", (chunk: Buffer) => {
      for (const line of framer.push(chunk)) {
        const match = /^LNDBG-LISTEN (\d+)$/.exec(line);
        if (match !== null) {
          clearTimeout(timer);
          resolvePort(Number(match[1]));
        } else settings.output?.("stdout", `${line}\n`);
      }
    });
    child.stderr.on("data", (chunk: Buffer) => settings.output?.("stderr", chunk.toString("utf8")));
    child.once("error", (error) => { clearTimeout(timer); rejectPort(error); });
    child.once("exit", (code) => { clearTimeout(timer); rejectPort(new ProcessFailureError(settings.emulatorPath, code)); });
  });
}

export class DebugRuntime {
  private client?: LnDbgClient;
  private emulator?: ChildProcessWithoutNullStreams;
  private readonly eventListeners: Array<(event: LnDbgEvent) => void> = [];
  private terminalEmitted = false;

  onEvent(listener: (event: LnDbgEvent) => void): void {
    this.eventListeners.push(listener);
  }

  async launch(settings: LaunchSettings): Promise<LaunchResult> {
    activeAddresses.clear();
    this.terminalEmitted = false;
    validateEmulatorOptions(settings.emulatorOptions);
    const outputDirectory = join(settings.cwd, ".lncpu-debug");
    await mkdir(outputDirectory, { recursive: true });
    const plan = planCompilation({ ...settings, outputDirectory });
    await runCompiler(plan, settings);
    const rawMap: unknown = JSON.parse(await readFile(plan.debugMapPath, "utf8"));
    const map = DebugMapIndex.parse(rawMap, settings.cwd);
    const loadedTargets = new Set(plan.artifacts.map((artifact) => artifact.target));
    const mapAddresses = map.symbolsForTargets(loadedTargets);
    const addresses = mapAddresses ?? new Map((await Promise.all(immediateArtifactPaths(plan).map(async (path) =>
      [...parseImmediateListing(await readFile(path, "utf8"))]
    ))).flat());
    const emulatorPath = isAbsolute(settings.emulatorPath) ? settings.emulatorPath : resolve(settings.cwd, settings.emulatorPath);
    const args = [
      ...emulatorArtifactArgs(plan.artifacts),
      "--debug-server",
      "--stop-on-entry",
      "--nopauseonhalt",
      ...settings.emulatorOptions,
    ];
    this.emulator = spawn(emulatorPath, args, { cwd: settings.cwd });
    this.emulator.once("exit", (code) => this.emitTerminal(code ?? 1));
    try {
      const port = await waitForPort(this.emulator, settings);
      this.client = await LnDbgClient.connect(port);
      this.client.onEvent((event) => {
        if (event.kind === "exited") this.emitTerminal(event.code);
        else for (const listener of this.eventListeners) listener(event);
      });
      this.client.onFatal(() => this.emitTerminal(1));
      await this.client.hello();
      activeAddresses.replace(addresses);
      return { plan, map, ramMemoryVariables: map.ramMemoryVariables(loadedTargets) };
    } catch (error: unknown) {
      await this.terminate();
      throw error;
    }
  }

  command(command: string, args: readonly string[] = []): Promise<string> {
    if (this.client === undefined) return Promise.reject(new ProtocolError("debug target is not connected"));
    return this.client.command(command, args);
  }

  async terminate(): Promise<void> {
    activeAddresses.clear();
    const client = this.client;
    this.client = undefined;
    if (client !== undefined) {
      try {
        await client.command("quit");
      } catch (error: unknown) {
        if (!(error instanceof ProtocolError)) throw error;
      }
      client.close();
    }
    const emulator = this.emulator;
    this.emulator = undefined;
    if (emulator !== undefined && emulator.exitCode === null) emulator.kill();
  }

  private emitTerminal(code: number): void {
    if (this.terminalEmitted) return;
    this.terminalEmitted = true;
    activeAddresses.clear();
    for (const listener of this.eventListeners) listener({ kind: "exited", code });
  }
}
