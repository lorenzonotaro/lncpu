import { normalize, resolve } from "node:path";

import { ProtocolError } from "./errors";

type LineEntry = {
  readonly a: number;
  readonly s: number;
  readonly f: number;
  readonly l: number;
  readonly c: number;
  readonly sec: string;
};
type LabelEntry = { readonly name: string; readonly a: number; readonly sec: string };
export type BreakpointLocation = { readonly address: number; readonly line: number; readonly column: number };
export type SourceLocation = { readonly path: string; readonly line: number; readonly column: number; readonly label?: string };

function record(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null;
}

function numberField(value: Record<string, unknown>, key: string): number {
  const field = value[key];
  if (typeof field !== "number" || !Number.isInteger(field)) throw new ProtocolError(`invalid debug map field: ${key}`);
  return field;
}

function stringField(value: Record<string, unknown>, key: string): string {
  const field = value[key];
  if (typeof field !== "string") throw new ProtocolError(`invalid debug map field: ${key}`);
  return field;
}

function parseLine(value: unknown): LineEntry {
  if (!record(value)) throw new ProtocolError("invalid debug map line");
  return { a: numberField(value, "a"), s: numberField(value, "s"), f: numberField(value, "f"), l: numberField(value, "l"), c: numberField(value, "c"), sec: stringField(value, "sec") };
}

function parseLabel(value: unknown): LabelEntry {
  if (!record(value)) throw new ProtocolError("invalid debug map label");
  return { name: stringField(value, "name"), a: numberField(value, "a"), sec: stringField(value, "sec") };
}

export class DebugMapIndex {
  private constructor(
    private readonly files: readonly string[],
    private readonly lines: readonly LineEntry[],
    private readonly labels: readonly LabelEntry[],
  ) {}

  static parse(value: unknown, cwd = process.cwd()): DebugMapIndex {
    if (!record(value) || value["version"] !== 1 || !Array.isArray(value["files"]) || !Array.isArray(value["lines"]) || !Array.isArray(value["labels"])) {
      throw new ProtocolError("unsupported LNCPU debug map");
    }
    const files = value["files"].map((file) => {
      if (typeof file !== "string") throw new ProtocolError("invalid debug map file");
      return normalize(resolve(cwd, file));
    });
    return new DebugMapIndex(
      files,
      value["lines"].map(parseLine).sort((left, right) => left.a - right.a),
      value["labels"].map(parseLabel).sort((left, right) => left.a - right.a),
    );
  }

  breakpoint(sourcePath: string, requestedLine: number): BreakpointLocation | undefined {
    const fileIndex = this.files.indexOf(normalize(resolve(sourcePath)));
    if (fileIndex < 0) return undefined;
    const found = this.lines.filter((line) => line.f === fileIndex && line.l >= requestedLine)
      .sort((left, right) => left.l - right.l || left.a - right.a)[0];
    return found === undefined ? undefined : { address: found.a, line: found.l, column: found.c };
  }

  location(pc: number): SourceLocation | undefined {
    const line = this.lines.find((candidate) => candidate.f >= 0 && candidate.a <= pc && pc < candidate.a + candidate.s);
    if (line === undefined) return undefined;
    const path = this.files[line.f];
    if (path === undefined) return undefined;
    const label = this.labels.filter((candidate) => candidate.a <= pc).at(-1)?.name;
    return label === undefined
      ? { path, line: line.l, column: line.c }
      : { path, line: line.l, column: line.c, label };
  }
}
