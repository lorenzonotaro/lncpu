import { strict as assert } from "node:assert";
import { test } from "node:test";

import { planCompilation } from "../debug/artifacts";

test("injects omitted binary and map outputs and plans requested device artifacts", () => {
  // Given / When
  const plan = planCompilation({
    cwd: "/work",
    outputDirectory: "/work/.lncpu-debug",
    lncPath: "/tools/lnc.jar",
    javaPath: "/jdk/bin/java",
    sourceFiles: ["src/main.lnc"],
    compilerOptions: ["-oD=ROM,RAM,D2"],
  });

  // Then
  assert.deepEqual(plan.command, "/jdk/bin/java");
  assert.deepEqual(plan.args.slice(0, 2), ["-jar", "/tools/lnc.jar"]);
  assert.ok(plan.args.includes("-oB=/work/.lncpu-debug/program.bin"));
  assert.ok(plan.args.includes("-oG=/work/.lncpu-debug/program.lndbg.json"));
  assert.ok(plan.args.includes("-oI=/work/.lncpu-debug/program.immediate.txt"));
  assert.equal(plan.immediatePath, "/work/.lncpu-debug/program.immediate.txt");
  assert.deepEqual(plan.artifacts, [
    { target: "ROM", path: "/work/.lncpu-debug/program.bin" },
    { target: "RAM", path: "/work/.lncpu-debug/program_RAM.bin" },
    { target: "D2", path: "/work/.lncpu-debug/program_D2.bin" },
  ]);
});

test("honors split output options and rejects suppressed binary output", () => {
  // Given / When
  const plan = planCompilation({
    cwd: "/work",
    outputDirectory: "/work/out",
    lncPath: "/tools/lnc",
    javaPath: "java",
    sourceFiles: ["main.lnasam"],
    compilerOptions: ["-oB", "custom.bin", "-oG=custom.map", "-oI", "custom.immediate", "-oD", "D0,D5"],
  });

  // Then
  assert.equal(plan.binaryPath, "/work/custom.bin");
  assert.equal(plan.debugMapPath, "/work/custom.map");
  assert.equal(plan.immediatePath, "/work/custom.immediate");
  assert.throws(() => planCompilation({
    cwd: "/work",
    outputDirectory: "/work/out",
    lncPath: "/tools/lnc",
    javaPath: "java",
    sourceFiles: ["main.lnc"],
    compilerOptions: ["-s"],
  }), /-s/);
});

test("rejects duplicate and empty managed compiler output options", () => {
  // Given
  const base = {
    cwd: "/work",
    outputDirectory: "/work/out",
    lncPath: "/tools/lnc",
    javaPath: "java",
    sourceFiles: ["main.lnc"],
  };

  // When / Then
  assert.throws(() => planCompilation({ ...base, compilerOptions: ["-oB=a.bin", "-oB", "b.bin"] }), /duplicate -oB/);
  assert.throws(() => planCompilation({ ...base, compilerOptions: ["-oG="] }), /-oG requires a value/);
  assert.throws(() => planCompilation({ ...base, compilerOptions: ["-oD=ROM", "-oD=RAM"] }), /duplicate -oD/);
  assert.throws(() => planCompilation({ ...base, compilerOptions: ["-oI=a.txt", "-oI", "b.txt"] }), /duplicate -oI/);
  assert.throws(() => planCompilation({ ...base, compilerOptions: ["-oI="] }), /-oI requires a value/);
});

test("uses the configured Java command unchanged for a JAR compiler", () => {
  // Given
  const input = { cwd: "/work", outputDirectory: "/work/out", lncPath: "lnc.jar", javaPath: "java", sourceFiles: ["main.lnasm"], compilerOptions: [] };

  // When
  const plan = planCompilation(input);

  // Then
  assert.equal(plan.command, "java");
});
