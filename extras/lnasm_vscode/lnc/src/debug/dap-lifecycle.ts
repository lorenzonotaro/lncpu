import { optionalString, record, requiredString, stringArray } from "./dap-values";
import { TARGETS, type DeviceImage } from "./artifacts";
import type { DebugMapIndex, RamMemoryVariable } from "./debug-map";
import { DebugConfigurationError } from "./errors";
import type { LaunchSettings } from "./runtime";

export type DapRequest = { readonly seq: number; readonly command: string; readonly arguments: Record<string, unknown> };
export type DapOutput =
  | { readonly kind: "response"; readonly requestSeq: number; readonly command: string; readonly success: boolean; readonly message?: string }
  | { readonly kind: "event"; readonly event: string; readonly body?: Record<string, unknown> };

export interface DapRuntime {
  launch(settings: LaunchSettings): Promise<DebugSession>;
  command(command: string): Promise<string>;
}

export type DebugSession = {
  readonly map: DebugMapIndex;
  readonly ramMemoryVariables: readonly RamMemoryVariable[];
};

export type ParsedLaunch = { readonly settings: LaunchSettings; readonly stopOnEntry: boolean };

export function parseDeviceImages(value: unknown): readonly DeviceImage[] {
  if (value === undefined) return [];
  if (!record(value) || Array.isArray(value)) throw new DebugConfigurationError("deviceImages must be an object");
  const images = new Map<DeviceImage["target"], string>();
  for (const [rawTarget, path] of Object.entries(value)) {
    const normalized = rawTarget.toUpperCase();
    const target = TARGETS.find((candidate) => candidate === normalized);
    if (target === undefined) throw new DebugConfigurationError(`unsupported deviceImages target: ${rawTarget}`);
    if (images.has(target)) throw new DebugConfigurationError(`duplicate deviceImages target: ${target}`);
    if (typeof path !== "string" || path.trim().length === 0) throw new DebugConfigurationError(`deviceImages.${rawTarget} must be a non-empty string`);
    images.set(target, path);
  }
  return TARGETS.flatMap((target): readonly DeviceImage[] => {
    const path = images.get(target);
    return path === undefined ? [] : [{ target, path }];
  });
}

export function parseLaunch(args: Record<string, unknown>): ParsedLaunch {
  const stopOnEntry = args["stopOnEntry"];
  if (stopOnEntry !== undefined && typeof stopOnEntry !== "boolean") throw new DebugConfigurationError("stopOnEntry must be a boolean");
  return {
    settings: {
      cwd: requiredString(args, "cwd"),
      lncPath: requiredString(args, "lncPath"),
      javaPath: optionalString(args, "javaPath", "java"),
      emulatorPath: requiredString(args, "emulatorPath"),
      sourceFiles: stringArray(args, "sourceFiles"),
      compilerOptions: stringArray(args, "compilerOptions"),
      emulatorOptions: stringArray(args, "emulatorOptions"),
      deviceImages: parseDeviceImages(args["deviceImages"]),
    },
    stopOnEntry: stopOnEntry ?? true,
  };
}

export class DapLifecycle {
  private queue: Promise<void> = Promise.resolve();
  private stopOnEntry = true;
  private launched = false;
  map?: DebugMapIndex;
  ramMemoryVariables: readonly RamMemoryVariable[] = [];

  constructor(private readonly runtime: DapRuntime, private readonly output: (message: DapOutput) => void) {}

  handle(request: DapRequest): Promise<void> {
    const current = this.queue.then(() => this.dispatch(request));
    this.queue = current.catch(() => undefined);
    return current;
  }

  private async dispatch(request: DapRequest): Promise<void> {
    if (request.command === "launch") {
      const launch = parseLaunch(request.arguments);
      this.stopOnEntry = launch.stopOnEntry;
      const session = await this.runtime.launch(launch.settings);
      this.map = session.map;
      this.ramMemoryVariables = session.ramMemoryVariables;
      this.launched = true;
      this.respond(request);
      this.output({ kind: "event", event: "initialized" });
      return;
    }
    if (request.command === "configurationDone") {
      if (!this.launched) throw new DebugConfigurationError("launch has not completed");
      if (!this.stopOnEntry) {
        await this.runtime.command("continue");
        this.output({ kind: "event", event: "continued", body: { threadId: 1, allThreadsContinued: true } });
      }
      this.respond(request);
      return;
    }
    throw new DebugConfigurationError(`unsupported lifecycle request: ${request.command}`);
  }

  private respond(request: DapRequest): void {
    this.output({ kind: "response", requestSeq: request.seq, command: request.command, success: true });
  }
}
