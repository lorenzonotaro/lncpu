# LNASM / LNC for Visual Studio Code

Language support for LNASM and LNC, plus an inline `lncpu` debugger that compiles source files and controls `lncpu-emu` through LNDBG v1.

## Language features

- Syntax highlighting for `.lnasm`, `.lnc`, and `.lnh` files.
- LNASM label, sublabel, macro, and section indexing.
- Definitions, hovers, workspace symbols, and opcode documentation.
- Ctrl/Cmd-click navigation from current-build labels and sections to their resolved memory addresses.
- Source breakpoints in LNASM and LNC files.

## Debugging

Create `.vscode/launch.json`:

```json
{
  "version": "0.2.0",
  "configurations": [
    {
      "type": "lncpu",
      "request": "launch",
      "name": "Debug LNCPU program",
      "cwd": "${workspaceFolder}",
      "lncPath": "${workspaceFolder}/tools/lnc.jar",
      "javaPath": "java",
      "emulatorPath": "${workspaceFolder}/build/lncpu-emu",
      "sourceFiles": [
        "src/main.lnc",
        "src/start.lnasm"
      ],
      "compilerOptions": ["-lf", "linker.cfg", "-oD=ROM,RAM,D0"],
      "emulatorOptions": []
    }
  ]
}
```

All relative paths are resolved from `cwd`. `lncPath` may name a JAR, launched with `javaPath`, or a directly executable compiler.

Before each launch, the extension compiles `sourceFiles`. It uses `.lncpu-debug/program.bin`, `.lncpu-debug/program.lndbg.json`, and `.lncpu-debug/program.immediate.txt` unless `compilerOptions` supplies `-oB`, `-oG`, and `-oI`. Split (`-oI file.txt`) and equals (`-oI=file.txt`) forms are supported. The ROM immediate-listing path is loaded because debugger memory references are flat absolute addresses. If `-oD` is omitted, `ROM` is requested. `ROM`, `RAM`, and `D0` through `D5` outputs are passed to the corresponding emulator options; non-ROM output names use lnc's `_RAM` or `_D0`-`_D5` suffix. `-s` is rejected because debugging requires a binary.

### Memory Inspector symbol navigation

Install the Eclipse CDT Memory Inspector extension, then add `lncpu` to its supported debug types in user settings. Preserve any existing entries:

```json
"memory-inspector.debugTypes": ["lncpu"]
```

During an active `lncpu` launch, Ctrl/Cmd-clicking a LNASM label, sublabel, or section found in the current build's `-oI` immediate listing opens Memory Inspector at its final address. Symbols omitted from that listing, including virtual or unused symbols, retain normal source-definition navigation. Navigation also falls back to the source definition when no `lncpu` session is active. A Memory Inspector installation or configuration error is reported rather than silently falling back.

The emulator is always launched with `--debug-server --stop-on-entry --nopauseonhalt`. The extension waits for `LNDBG-LISTEN`, connects only to localhost, and requires LNDBG protocol version 1 or newer.

Supported debugger operations:

- Exact-or-next-line source breakpoints from debug map v1.
- Continue, pause, step in, step over, and step out. These actions are hardware instruction-level operations because LNDBG v1 exposes CPU instruction stepping, not source-level stepping.
- One synthetic LNCPU thread and source-mapped stack frame.
- Register inspection and editing.
- Memory reads and writes from the VS Code memory viewer.
- Clean terminate and disconnect.

Attach sessions, conditional breakpoints, expression evaluation, and source locals are not supported by LNDBG v1 and are intentionally omitted.

## Development

```sh
npm install
npm test
```

`npm test` compiles the strict CommonJS TypeScript project and runs focused protocol, debug-map, artifact-planning, and hermetic lifecycle tests.
