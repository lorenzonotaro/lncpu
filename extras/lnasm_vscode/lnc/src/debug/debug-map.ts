import { normalize, resolve } from "node:path";

import type { Target } from "./artifacts";
import { ProtocolError } from "./errors";

const PHYSICAL_TARGETS: readonly Target[] = ["ROM", "RAM", "D0", "D1", "D2", "D3", "D4", "D5"];
const TARGET_SIZE = 0x2000;

type LineEntry = {
  readonly a: number;
  readonly s: number;
  readonly f: number;
  readonly l: number;
  readonly c: number;
  readonly sec: string;
};
type LabelEntry = { readonly name: string; readonly a: number; readonly sec: string };
type SectionEntry = { readonly name: string; readonly target: Target; readonly a: number; readonly s: number };
export type BreakpointLocation = { readonly address: number; readonly line: number; readonly column: number };
export type SourceLocation = { readonly path: string; readonly line: number; readonly column: number; readonly label?: string };
export type RamMemoryVariable = { readonly name: string; readonly address: number; readonly byteLength: number };

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

function parseSection(value: unknown): SectionEntry {
  if (!record(value)) throw new ProtocolError("invalid debug map section");
  const name = stringField(value, "name");
  const targetName = stringField(value, "target");
  const target = PHYSICAL_TARGETS.find((candidate) => candidate === targetName);
  const address = numberField(value, "a");
  const size = numberField(value, "s");
  const targetIndex = target === undefined ? undefined : PHYSICAL_TARGETS.indexOf(target);
  const targetStart = targetIndex === undefined ? undefined : targetIndex * TARGET_SIZE;
  const targetLimit = targetStart === undefined ? undefined : targetStart + TARGET_SIZE;
  if (name.length === 0 || target === undefined || targetStart === undefined || targetLimit === undefined || address < targetStart || address >= targetLimit || size < 0 || address + size > targetLimit) {
    throw new ProtocolError("invalid debug map section");
  }
  return { name, target, a: address, s: size };
}

function compareNames(left: string, right: string): number {
  return left < right ? -1 : left > right ? 1 : 0;
}

export class DebugMapIndex {
  private constructor(
    private readonly files: readonly string[],
    private readonly lines: readonly LineEntry[],
    private readonly labels: readonly LabelEntry[],
    private readonly sections: readonly SectionEntry[] | undefined,
  ) {}

  static parse(value: unknown, cwd = process.cwd()): DebugMapIndex {
    if (!record(value) || value["version"] !== 1 || !Array.isArray(value["files"]) || !Array.isArray(value["lines"]) || !Array.isArray(value["labels"])) {
      throw new ProtocolError("unsupported LNCPU debug map");
    }
    const files = value["files"].map((file) => {
      if (typeof file !== "string") throw new ProtocolError("invalid debug map file");
      return normalize(resolve(cwd, file));
    });
    const rawSections = value["sections"];
    if (rawSections !== undefined && !Array.isArray(rawSections)) throw new ProtocolError("invalid debug map sections");
    return new DebugMapIndex(
      files,
      value["lines"].map(parseLine).sort((left, right) => left.a - right.a),
      value["labels"].map(parseLabel).sort((left, right) => left.a - right.a),
      rawSections?.map(parseSection),
    );
  }

  symbolsForTargets(loadedTargets: ReadonlySet<Target>): ReadonlyMap<string, number> | undefined {
    if (this.sections === undefined) return undefined;
    const symbols = new Map<string, number>();
    const includedSections = new Set<string>();
    for (const section of this.sections) {
      if (!loadedTargets.has(section.target)) continue;
      includedSections.add(section.name);
      symbols.set(section.name, section.a);
    }
    for (const label of this.labels) {
      if (includedSections.has(label.sec)) symbols.set(label.name, label.a);
    }
    return symbols;
  }

  ramMemoryVariables(loadedTargets: ReadonlySet<Target>): readonly RamMemoryVariable[] {
    if (this.sections === undefined || !loadedTargets.has("RAM")) return [];
    const variables: RamMemoryVariable[] = [];
    const ramSections = this.sections
      .filter((section) => section.target === "RAM" && section.s > 0)
      .sort((left, right) => left.a - right.a || compareNames(left.name, right.name));
    for (const section of ramSections) {
      const sectionEnd = section.a + section.s;
      const labels = this.labels
        .filter((label) => label.sec === section.name && !label.name.includes("$") && section.a <= label.a && label.a < sectionEnd)
        .sort((left, right) => left.a - right.a || compareNames(left.name, right.name));
      const distinct = labels.filter((label, index) => index === 0 || label.a !== labels[index - 1]?.a);
      for (let index = 0; index < distinct.length; index += 1) {
        const label = distinct[index];
        if (label === undefined) continue;
        const end = distinct[index + 1]?.a ?? sectionEnd;
        variables.push({ name: label.name, address: label.a, byteLength: end - label.a });
      }
    }
    return variables;
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
