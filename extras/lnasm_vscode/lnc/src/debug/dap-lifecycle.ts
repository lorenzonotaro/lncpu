import { optionalString, record, requiredString, stringArray } from "./dap-values";
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
