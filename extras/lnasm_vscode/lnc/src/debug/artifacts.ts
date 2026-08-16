import { isAbsolute, join, parse, resolve } from "node:path";

import { DebugConfigurationError } from "./errors";

export const TARGETS = ["ROM", "RAM", "D0", "D1", "D2", "D3", "D4", "D5"] as const;
export type Target = (typeof TARGETS)[number];
export type DeviceImage = { readonly target: Target; readonly path: string };

export type CompilationInput = {
  readonly cwd: string;
  readonly outputDirectory: string;
  readonly lncPath: string;
  readonly javaPath: string;
  readonly sourceFiles: readonly string[];
  readonly compilerOptions: readonly string[];
  readonly deviceImages: readonly DeviceImage[];
};

export type Artifact = { readonly target: Target; readonly path: string };
export type CompilationPlan = {
  readonly command: string;
  readonly args: readonly string[];
  readonly binaryPath: string;
  readonly debugMapPath: string;
  readonly immediatePath: string;
  readonly artifacts: readonly Artifact[];
  readonly debugTargets: readonly Target[];
};

type ParsedOption = { readonly value: string };

function option(options: readonly string[], name: string): ParsedOption | undefined {
  let found: ParsedOption | undefined;
  for (let index = 0; index < options.length; index += 1) {
    const current = options[index];
    let value: string | undefined;
    if (current === name) {
      value = options[index + 1];
      if (value === undefined || value.startsWith("-")) {
        throw new DebugConfigurationError(`${name} requires a value`);
      }
      index += 1;
    }
    if (current?.startsWith(`${name}=`)) {
      value = current.slice(name.length + 1);
      if (value.length === 0) throw new DebugConfigurationError(`${name} requires a value`);
    }
    if (value === undefined) continue;
    if (found !== undefined) throw new DebugConfigurationError(`duplicate ${name}`);
    found = { value };
  }
  return found;
}

function absolute(cwd: string, path: string): string {
  return isAbsolute(path) ? path : resolve(cwd, path);
}

export function targetPath(binaryPath: string, target: Target): string {
  if (target === "ROM") return binaryPath;
  const parts = parse(binaryPath);
  return join(parts.dir, `${parts.name}_${target}${parts.ext}`);
}

export function immediateArtifactPaths(plan: CompilationPlan): readonly string[] {
  return plan.debugTargets.map((target) => targetPath(plan.immediatePath, target));
}

function parseTargets(raw: string): readonly Target[] {
  const values = raw.split(",").map((value) => value.trim().toUpperCase()).filter((value) => value.length > 0);
  if (values.length === 0) throw new DebugConfigurationError("-oD requires at least one target");
  const targets = values.map((value) => {
    const target = TARGETS.find((candidate) => candidate === value);
    if (target === undefined) throw new DebugConfigurationError(`unsupported -oD target: ${value}`);
    return target;
  });
  if (new Set(targets).size !== targets.length) throw new DebugConfigurationError("duplicate -oD target");
  return targets;
}

export function planCompilation(input: CompilationInput): CompilationPlan {
  if (input.sourceFiles.length === 0) throw new DebugConfigurationError("sourceFiles must not be empty");
  if (input.compilerOptions.some((value) => value === "-s" || value.startsWith("-s="))) {
    throw new DebugConfigurationError("-s suppresses the binary required for debugging");
  }
  const binaryOption = option(input.compilerOptions, "-oB");
  const mapOption = option(input.compilerOptions, "-oG");
  const immediateOption = option(input.compilerOptions, "-oI");
  const targetsOption = option(input.compilerOptions, "-oD");
  const binaryPath = absolute(input.cwd, binaryOption?.value ?? join(input.outputDirectory, "program.bin"));
  const debugMapPath = absolute(input.cwd, mapOption?.value ?? join(input.outputDirectory, "program.lndbg.json"));
  const immediatePath = absolute(input.cwd, immediateOption?.value ?? join(input.outputDirectory, "program.immediate.txt"));
  const compilerTargets = parseTargets(targetsOption?.value ?? "ROM");
  const externalByTarget = new Map(input.deviceImages.map((image) => [image.target, absolute(input.cwd, image.path)]));
  const debugTargets = compilerTargets.filter((target) => !externalByTarget.has(target));
  const compilerArtifacts = compilerTargets.map((target) => ({ target, path: externalByTarget.get(target) ?? targetPath(binaryPath, target) }));
  const externalArtifacts = TARGETS.flatMap((target): readonly Artifact[] => {
    const path = externalByTarget.get(target);
    return path === undefined || compilerTargets.includes(target) ? [] : [{ target, path }];
  });
  const injected = [...input.compilerOptions];
  if (binaryOption === undefined) injected.push(`-oB=${binaryPath}`);
  if (mapOption === undefined) injected.push(`-oG=${debugMapPath}`);
  if (immediateOption === undefined) injected.push(`-oI=${immediatePath}`);
  if (targetsOption === undefined) injected.push("-oD=ROM");
  const compiler = absolute(input.cwd, input.lncPath);
  const prefix = compiler.toLowerCase().endsWith(".jar") ? ["-jar", compiler] : [];
  const command = prefix.length === 0 || !input.javaPath.includes("/") ? (prefix.length === 0 ? compiler : input.javaPath) : absolute(input.cwd, input.javaPath);
  return {
    command,
    args: [...prefix, ...injected, ...input.sourceFiles.map((path) => absolute(input.cwd, path))],
    binaryPath,
    debugMapPath,
    immediatePath,
    artifacts: [...compilerArtifacts, ...externalArtifacts],
    debugTargets,
  };
}

export function emulatorArtifactArgs(artifacts: readonly Artifact[]): readonly string[] {
  return artifacts.flatMap((artifact) => [`--${artifact.target.toLowerCase()}`, artifact.path]);
}

export function validateEmulatorOptions(options: readonly string[]): void {
  const managed = new Set(["--debug-server", "--stop-on-entry", "--nopauseonhalt", "--rom", "--ram", "--d0", "--d1", "--d2", "--d3", "--d4", "--d5", "-p", "--pause"]);
  const conflict = options.find((option) => managed.has(option.split("=", 1)[0] ?? option));
  if (conflict !== undefined) throw new DebugConfigurationError(`${conflict} is managed by the debugger`);
}
