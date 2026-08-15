import { strict as assert } from "node:assert";
import { test } from "node:test";

import { immediateArtifactPaths, planCompilation } from "../debug/artifacts";

test("injects omitted binary and map outputs and plans requested device artifacts", () => {
  // Given / When
  const plan = planCompilation({
    cwd: "/work",
    outputDirectory: "/work/.lncpu-debug",
    lncPath: "/tools/lnc.jar",
    javaPath: "/jdk/bin/java",
    sourceFiles: ["src/main.lnc"],
    compilerOptions: ["-oD=ROM,RAM,D2"],
    deviceImages: [],
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
    deviceImages: [],
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
    deviceImages: [],
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
    deviceImages: [],
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
  const input = { cwd: "/work", outputDirectory: "/work/out", lncPath: "lnc.jar", javaPath: "java", sourceFiles: ["main.lnasm"], compilerOptions: [], deviceImages: [] };

  // When
  const plan = planCompilation(input);

  // Then
  assert.equal(plan.command, "java");
});

test("derives legacy immediate listing paths for every planned artifact", () => {
  // Given
  const plan = planCompilation({
    cwd: "/work",
    outputDirectory: "/work/out",
    lncPath: "/tools/lnc",
    javaPath: "java",
    sourceFiles: ["main.lnasm"],
    compilerOptions: ["-oI=maps/program.listing.txt", "-oD=ROM,RAM,D5"],
    deviceImages: [],
  });

  // When
  const paths = immediateArtifactPaths(plan);

  // Then
  assert.deepEqual(paths, [
    "/work/maps/program.listing.txt",
    "/work/maps/program.listing_RAM.txt",
    "/work/maps/program.listing_D5.txt",
  ]);
});

test("replaces compiler artifacts and appends external targets in stable target order", () => {
  // Given / When
  const plan = planCompilation({
    cwd: "/work",
    outputDirectory: "/work/out",
    lncPath: "/tools/lnc",
    javaPath: "java",
    sourceFiles: ["main.lnasm"],
    compilerOptions: ["-oD=D2,ROM,D0"],
    deviceImages: [
      { target: "RAM", path: "images/ram.bin" },
      { target: "D0", path: "images/external-d0.bin" },
      { target: "D1", path: "/shared/d1.bin" },
    ],
  });

  // Then
  assert.deepEqual(plan.artifacts, [
    { target: "D2", path: "/work/out/program_D2.bin" },
    { target: "ROM", path: "/work/out/program.bin" },
    { target: "D0", path: "/work/images/external-d0.bin" },
    { target: "RAM", path: "/work/images/ram.bin" },
    { target: "D1", path: "/shared/d1.bin" },
  ]);
  assert.deepEqual(plan.debugTargets, ["D2", "ROM"]);
});

test("omits external replacements from legacy immediate listings", () => {
  // Given
  const plan = planCompilation({
    cwd: "/work",
    outputDirectory: "/work/out",
    lncPath: "/tools/lnc",
    javaPath: "java",
    sourceFiles: ["main.lnasm"],
    compilerOptions: ["-oI=maps/program.listing.txt", "-oD=ROM,RAM,D5"],
    deviceImages: [{ target: "RAM", path: "external/ram.bin" }],
  });

  // When
  const paths = immediateArtifactPaths(plan);

  // Then
  assert.deepEqual(paths, [
    "/work/maps/program.listing.txt",
    "/work/maps/program.listing_D5.txt",
  ]);
});
