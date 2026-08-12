# LNASM / LNC for Visual Studio Code

Language support for LNASM and LNC, plus an inline `lncpu` debugger that compiles source files and controls `lncpu-emu` through LNDBG v1.

## Language features

- Syntax highlighting for `.lnasm`, `.lnc`, and `.lnh` files.
- LNASM label, sublabel, macro, and section indexing.
- Definitions, hovers, workspace symbols, and opcode documentation.
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

Before each launch, the extension compiles `sourceFiles`. It uses `.lncpu-debug/program.bin` and `.lncpu-debug/program.lndbg.json` unless `compilerOptions` supplies `-oB` and `-oG`. Both split (`-oB file.bin`) and equals (`-oB=file.bin`) forms are supported. If `-oD` is omitted, `ROM` is requested. `ROM`, `RAM`, and `D0` through `D5` outputs are passed to the corresponding emulator options; non-ROM output names use lnc's `_RAM` or `_D0`-`_D5` suffix. `-s` is rejected because debugging requires a binary.

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
