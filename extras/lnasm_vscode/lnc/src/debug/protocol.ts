import { connect, type Socket } from "node:net";

import { ProtocolError } from "./errors";

export type LnDbgFrame =
  | { readonly kind: "reply"; readonly id: number; readonly ok: boolean; readonly payload: string }
  | { readonly kind: "stopped"; readonly reason: string; readonly pc: number }
  | { readonly kind: "exited"; readonly code: number };
export type LnDbgEvent = Exclude<LnDbgFrame, { readonly kind: "reply" }>;
type Pending = { readonly resolve: (payload: string) => void; readonly reject: (error: Error) => void; readonly timer: NodeJS.Timeout };
export type ClientTimeouts = { readonly connectTimeoutMs: number; readonly requestTimeoutMs: number };

const DEFAULT_TIMEOUTS: ClientTimeouts = { connectTimeoutMs: 5_000, requestTimeoutMs: 5_000 };

export class LineFramer {
  private pending = "";

  constructor(private readonly maximumLength = 1_048_576) {}

  push(chunk: Buffer): readonly string[] {
    this.pending += chunk.toString("utf8");
    if (this.pending.length > this.maximumLength && !this.pending.includes("\n")) {
      throw new ProtocolError(`LNDBG frame exceeds ${this.maximumLength} bytes`);
    }
    const lines: string[] = [];
    for (;;) {
      const newline = this.pending.indexOf("\n");
      if (newline < 0) return lines;
      const line = this.pending.slice(0, newline).replace(/\r$/, "");
      this.pending = this.pending.slice(newline + 1);
      if (line.length > 0) lines.push(line);
    }
  }
}

export function parseLnDbgFrame(line: string): LnDbgFrame {
  const reply = /^(\d+) (ok|err)(?: (.*))?$/.exec(line);
  if (reply !== null) {
    return { kind: "reply", id: Number(reply[1]), ok: reply[2] === "ok", payload: reply[3] ?? "" };
  }
  const stopped = /^! stopped (entry|pause|breakpoint|step|halt) ([0-9a-fA-F]{1,4})$/.exec(line);
  if (stopped !== null) return { kind: "stopped", reason: stopped[1], pc: Number.parseInt(stopped[2], 16) };
  const exited = /^! exited (-?\d+)$/.exec(line);
  if (exited !== null) return { kind: "exited", code: Number(exited[1]) };
  throw new ProtocolError(`invalid LNDBG frame: ${line}`);
}

export class LnDbgClient {
  private nextId = 1;
  private readonly pending = new Map<number, Pending>();
  private readonly eventListeners: Array<(event: LnDbgEvent) => void> = [];
  private readonly fatalListeners: Array<(error: Error) => void> = [];
  private readonly queuedEvents: LnDbgEvent[] = [];

  private failed = false;

  private constructor(private readonly socket: Socket, private readonly requestTimeoutMs: number) {
    const framer = new LineFramer();
    socket.on("data", (chunk: Buffer) => {
      try {
        for (const line of framer.push(chunk)) this.receive(parseLnDbgFrame(line));
      } catch (error: unknown) {
        this.fatal(error instanceof Error ? error : new ProtocolError("LNDBG parsing failed"));
      }
    });
    socket.on("error", (error: Error) => this.fatal(error));
    socket.on("close", () => this.fatal(new ProtocolError("LNDBG connection closed")));
  }

  static connect(port: number, timeouts: ClientTimeouts = DEFAULT_TIMEOUTS): Promise<LnDbgClient> {
    return new Promise((resolveConnection, rejectConnection) => {
      const socket = connect({ host: "127.0.0.1", port });
      const timer = setTimeout(() => {
        socket.destroy();
        rejectConnection(new ProtocolError("LNDBG connection timed out"));
      }, timeouts.connectTimeoutMs);
      socket.once("connect", () => {
        clearTimeout(timer);
        resolveConnection(new LnDbgClient(socket, timeouts.requestTimeoutMs));
      });
      socket.once("error", (error) => { clearTimeout(timer); rejectConnection(error); });
    });
  }

  onEvent(listener: (event: LnDbgEvent) => void): void {
    this.eventListeners.push(listener);
    for (const event of this.queuedEvents.splice(0)) listener(event);
  }

  onFatal(listener: (error: Error) => void): void {
    this.fatalListeners.push(listener);
  }

  async hello(): Promise<void> {
    const payload = await this.command("hello");
    const match = /^LNDBG (\d+)$/.exec(payload);
    if (match === null || Number(match[1]) < 1) throw new ProtocolError(`unsupported LNDBG handshake: ${payload}`);
  }

  command(command: string, args: readonly string[] = []): Promise<string> {
    const id = this.nextId;
    this.nextId += 1;
    return new Promise((resolveReply, rejectReply) => {
      const timer = setTimeout(() => {
        this.pending.delete(id);
        rejectReply(new ProtocolError(`LNDBG request ${id} timed out`));
        this.socket.destroy();
      }, this.requestTimeoutMs);
      this.pending.set(id, { resolve: resolveReply, reject: rejectReply, timer });
      this.socket.write(`${id} ${command}${args.length === 0 ? "" : ` ${args.join(" ")}`}\n`);
    });
  }

  close(): void {
    this.socket.destroy();
  }

  private receive(frame: LnDbgFrame): void {
    if (frame.kind !== "reply") {
      if (this.eventListeners.length === 0) this.queuedEvents.push(frame);
      else for (const listener of this.eventListeners) listener(frame);
      return;
    }
    const pending = this.pending.get(frame.id);
    if (pending === undefined) {
      this.fatal(new ProtocolError(`unexpected LNDBG reply: ${frame.id}`));
      return;
    }
    this.pending.delete(frame.id);
    clearTimeout(pending.timer);
    if (frame.ok) pending.resolve(frame.payload);
    else pending.reject(new ProtocolError(frame.payload));
  }

  private fatal(error: Error): void {
    if (this.failed) return;
    this.failed = true;
    for (const request of this.pending.values()) {
      clearTimeout(request.timer);
      request.reject(error);
    }
    this.pending.clear();
    for (const listener of this.fatalListeners) listener(error);
    this.socket.destroy();
  }
}
