import { basename } from "node:path";
import * as vscode from "vscode";

import { integer, parseAddress, parseRegisterPayload, record, requiredString } from "./dap-values";
import { DapLifecycle, parseLaunch, type DapOutput } from "./dap-lifecycle";
import type { DebugMapIndex } from "./debug-map";
import { DebugConfigurationError, ProtocolError } from "./errors";
import { DebugRuntime } from "./runtime";

type Request = { readonly seq: number; readonly command: string; readonly arguments: Record<string, unknown> };

function request(message: vscode.DebugProtocolMessage): Request {
  const raw: unknown = message;
  if (!record(raw) || raw["type"] !== "request" || typeof raw["seq"] !== "number" || typeof raw["command"] !== "string") {
    throw new ProtocolError("invalid DAP request");
  }
  const args = raw["arguments"];
  if (args !== undefined && !record(args)) throw new ProtocolError("invalid DAP arguments");
  return { seq: raw["seq"], command: raw["command"], arguments: args ?? {} };
}

export class LncpuDebugAdapter implements vscode.DebugAdapter {
  private readonly emitter = new vscode.EventEmitter<vscode.DebugProtocolMessage>();
  readonly onDidSendMessage = this.emitter.event;
  private readonly runtime = new DebugRuntime();
  private readonly lifecycle: DapLifecycle;
  private queue: Promise<void> = Promise.resolve();
  private sequence = 1;
  private map?: DebugMapIndex;
  private pc = 0;
  private stopOnEntry = true;
  private entryPending = false;
  private readonly breakpoints = new Map<string, Set<number>>();

  constructor() {
    this.lifecycle = new DapLifecycle({
      launch: async (settings) => (await this.runtime.launch({ ...settings, output: (category, text) => this.event("output", { category, output: text }) })).map,
      command: (command) => this.runtime.command(command),
    }, (output) => this.lifecycleOutput(output));
    this.runtime.onEvent((event) => {
      if (event.kind === "stopped") {
        this.pc = event.pc;
        if (event.reason === "entry" && this.entryPending) return;
        const reason = event.reason === "halt" ? "exception" : event.reason;
        this.event("stopped", { reason, threadId: 1, allThreadsStopped: true });
      } else {
        this.event("exited", { exitCode: event.code });
        this.event("terminated");
      }
    });
  }

  handleMessage(message: vscode.DebugProtocolMessage): void {
    let incoming: Request;
    try {
      incoming = request(message);
    } catch (error: unknown) {
      this.event("output", { category: "stderr", output: `${error instanceof Error ? error.message : "Invalid DAP message"}\n` });
      return;
    }
    const current = this.queue.then(() => this.dispatch(incoming)).catch((error: unknown) => {
      this.respond(incoming, undefined, error instanceof Error ? error.message : "Debug request failed");
    });
    this.queue = current;
  }

  dispose(): void {
    void this.runtime.terminate();
    this.emitter.dispose();
  }

  private async dispatch(incoming: Request): Promise<void> {
    const args = incoming.arguments;
    switch (incoming.command) {
      case "initialize":
        this.respond(incoming, { supportsConfigurationDoneRequest: true, supportsSetVariable: true, supportsReadMemoryRequest: true, supportsWriteMemoryRequest: true, supportsTerminateRequest: true });
        return;
      case "launch": {
        const launch = parseLaunch(args);
        this.stopOnEntry = launch.stopOnEntry;
        this.entryPending = true;
        await this.lifecycle.handle(incoming);
        this.map = this.lifecycle.map;
        return;
      }
      case "configurationDone": await this.configurationDone(incoming); return;
      case "threads": this.respond(incoming, { threads: [{ id: 1, name: "LNCPU" }] }); return;
      case "stackTrace": this.stackTrace(incoming); return;
      case "scopes": this.respond(incoming, { scopes: [{ name: "Registers", variablesReference: 1, expensive: false }] }); return;
      case "variables": await this.variables(incoming); return;
      case "setVariable": await this.setVariable(incoming); return;
      case "setBreakpoints": await this.setSourceBreakpoints(incoming); return;
      case "continue": await this.control(incoming, "continue", { allThreadsContinued: true }); return;
      case "pause": await this.control(incoming, "pause"); return;
      case "next": await this.control(incoming, "stepover"); return;
      case "stepIn": await this.control(incoming, "step"); return;
      case "stepOut": await this.control(incoming, "stepout"); return;
      case "readMemory": await this.readMemory(incoming); return;
      case "writeMemory": await this.writeMemory(incoming); return;
      case "disconnect":
      case "terminate": await this.runtime.terminate(); this.respond(incoming); return;
      default: this.respond(incoming, undefined, `unsupported request: ${incoming.command}`);
    }
  }

  private async configurationDone(incoming: Request): Promise<void> {
    if (!this.entryPending) {
      this.respond(incoming);
      return;
    }
    this.entryPending = false;
    if (this.stopOnEntry) {
      await this.lifecycle.handle(incoming);
      this.event("stopped", { reason: "entry", threadId: 1, allThreadsStopped: true });
      return;
    }
    await this.lifecycle.handle(incoming);
  }

  private stackTrace(incoming: Request): void {
    const location = this.map?.location(this.pc);
    const frame = location === undefined
      ? { id: 1, name: `0x${this.pc.toString(16).padStart(4, "0")}`, line: 1, column: 1, instructionPointerReference: `0x${this.pc.toString(16)}` }
      : { id: 1, name: location.label ?? basename(location.path), source: { name: basename(location.path), path: location.path }, line: location.line, column: location.column, instructionPointerReference: `0x${this.pc.toString(16)}` };
    this.respond(incoming, { stackFrames: [frame], totalFrames: 1 });
  }

  private async variables(incoming: Request): Promise<void> {
    const registers = parseRegisterPayload(await this.runtime.command("regs"));
    this.respond(incoming, { variables: registers.map((register) => ({ ...register, variablesReference: 0, evaluateName: register.name })) });
  }

  private async setVariable(incoming: Request): Promise<void> {
    const name = requiredString(incoming.arguments, "name");
    const raw = requiredString(incoming.arguments, "value");
    const value = parseAddress(raw).toString(16);
    await this.runtime.command("setreg", [name, value]);
    this.respond(incoming, { value: `0x${value}` });
  }

  private async setSourceBreakpoints(incoming: Request): Promise<void> {
    const source = incoming.arguments["source"];
    if (!record(source)) throw new DebugConfigurationError("breakpoint source is required");
    const path = requiredString(source, "path");
    const requested = incoming.arguments["breakpoints"];
    if (!Array.isArray(requested)) throw new DebugConfigurationError("breakpoints must be an array");
    const old = this.breakpoints.get(path) ?? new Set<number>();
    for (const address of old) await this.runtime.command("bp", ["clear", address.toString(16)]);
    const active = new Set<number>();
    const breakpoints = [];
    for (const item of requested) {
      if (!record(item)) throw new DebugConfigurationError("invalid source breakpoint");
      const requestedLine = integer(item, "line");
      const location = this.map?.breakpoint(path, requestedLine);
      if (location === undefined) breakpoints.push({ verified: false, line: requestedLine, message: "No executable code at or after this line" });
      else {
        await this.runtime.command("bp", ["set", location.address.toString(16)]);
        active.add(location.address);
        breakpoints.push({ verified: true, line: location.line, column: location.column, instructionReference: `0x${location.address.toString(16)}` });
      }
    }
    this.breakpoints.set(path, active);
    this.respond(incoming, { breakpoints });
  }

  private async control(incoming: Request, command: string, body?: Record<string, unknown>): Promise<void> {
    await this.runtime.command(command);
    this.respond(incoming, body);
    if (command === "continue") this.event("continued", { threadId: 1, allThreadsContinued: true });
  }

  private async readMemory(incoming: Request): Promise<void> {
    const start = parseAddress(requiredString(incoming.arguments, "memoryReference")) + integer(incoming.arguments, "offset", 0);
    const count = integer(incoming.arguments, "count");
    if (start < 0 || count < 0 || start + count > 0x10000) throw new ProtocolError("invalid memory range");
    const payload = await this.runtime.command("readmem", [start.toString(16), String(count)]);
    this.respond(incoming, { address: `0x${start.toString(16)}`, data: Buffer.from(payload, "hex").toString("base64") });
  }

  private async writeMemory(incoming: Request): Promise<void> {
    const start = parseAddress(requiredString(incoming.arguments, "memoryReference")) + integer(incoming.arguments, "offset", 0);
    const bytes = Buffer.from(requiredString(incoming.arguments, "data"), "base64");
    if (start < 0 || start + bytes.length > 0x10000) throw new ProtocolError("invalid memory write");
    await this.runtime.command("writemem", [start.toString(16), bytes.toString("hex")]);
    this.respond(incoming, { bytesWritten: bytes.length });
  }

  private respond(incoming: Request, body?: unknown, message?: string): void {
    const response = { seq: this.sequence++, type: "response", request_seq: incoming.seq, command: incoming.command, success: message === undefined, ...(body === undefined ? {} : { body }), ...(message === undefined ? {} : { message }) };
    this.emitter.fire(response);
  }

  private event(event: string, body?: unknown): void {
    this.emitter.fire({ seq: this.sequence++, type: "event", event, ...(body === undefined ? {} : { body }) });
  }

  private lifecycleOutput(output: DapOutput): void {
    if (output.kind === "event") this.event(output.event, output.body);
    else this.emitter.fire({ seq: this.sequence++, type: "response", request_seq: output.requestSeq, command: output.command, success: output.success, ...(output.message === undefined ? {} : { message: output.message }) });
  }
}
