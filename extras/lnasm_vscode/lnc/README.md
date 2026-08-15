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
      "deviceImages": {
        "D0": "assets/d0.bin",
        "D2": "assets/d2.bin"
      },
      "emulatorOptions": []
    }
  ]
}
```

All relative paths are resolved from `cwd`. `lncPath` may name a JAR, launched with `javaPath`, or a directly executable compiler.

Before each launch, the extension compiles `sourceFiles`. It uses `.lncpu-debug/program.bin`, `.lncpu-debug/program.lndbg.json`, and `.lncpu-debug/program.immediate.txt` unless `compilerOptions` supplies `-oB`, `-oG`, and `-oI`. Split (`-oI file.txt`) and equals (`-oI=file.txt`) forms are supported. If `-oD` is omitted, `ROM` is requested. `ROM`, `RAM`, and `D0` through `D5` outputs are passed to the corresponding emulator options; non-ROM output names use lnc's `_RAM` or `_D0`-`_D5` suffix. `-s` is rejected because debugging requires a binary.

`deviceImages` accepts explicit `ROM`, `RAM`, and `D0` through `D5` image paths, resolved relative to `cwd`. An external image replaces the emulator image for the same `-oD` target without changing compiler output generation; a target absent from `-oD` is added after compiler targets in `ROM`/`RAM`/`D0`-`D5` order. The emulator receives exactly one image option per target. Raw image options such as `--rom`, `--ram`, and `--d0` remain invalid in `emulatorOptions`.

### Memory Inspector symbol navigation

Install the Eclipse CDT Memory Inspector extension, then add `lncpu` to its supported debug types in user settings. Preserve any existing entries:

```json
"memory-inspector.debugTypes": ["lncpu"]
```

During an active `lncpu` launch, the version 1 `-oG` debug map supplies complete physical section and label metadata across `ROM`, `RAM`, and `D0` through `D5`, independently of `-oD`. Navigation exposes compiler symbols only for `-oD` targets not replaced by `deviceImages`. External images never infer source maps: replaced or externally added targets expose no compiler symbols, legacy immediate-listing symbols, or RAM Variables, while unaffected compiler targets remain navigable. Virtual sections, labels attached to virtual or unknown sections, and symbols in unloaded targets retain normal source-definition navigation. Labels take precedence over same-named sections.

When RAM is loaded, eligible top-level RAM labels also appear under the debugger's `RAM` Variables scope. Each variable spans from its label address to the next distinct top-level label in the same physical section, or to that section's emitted end. Sublabels are omitted. These Variables entries include memory references and byte lengths so Memory Inspector can open them from its Variables context menu.

For compatibility with older version 1 debug maps that have no `sections` field, the extension falls back to `-oI` and reads the base listing plus each requested target's `_RAM` or `_D0`-`_D5` suffixed listing. A present `sections` table, including an empty one, is authoritative and does not require immediate-listing files. Navigation also falls back to the source definition when no `lncpu` session is active. A Memory Inspector installation or configuration error is reported rather than silently falling back.

The emulator is always launched with `--debug-server --stop-on-entry --nopauseonhalt`. The extension waits for `LNDBG-LISTEN`, connects only to localhost, and requires LNDBG protocol version 1 or newer.

Supported debugger operations:

- Exact-or-next-line source breakpoints from debug map v1.
- Continue, pause, step in, step over, and step out. These actions are hardware instruction-level operations because LNDBG v1 exposes CPU instruction stepping, not source-level stepping.
- One synthetic LNCPU thread and source-mapped stack frame.
- Register inspection and editing.
- Top-level labels for loaded RAM images in the RAM Variables scope.
- Memory reads and writes from the VS Code memory viewer.
- Clean terminate and disconnect.

Attach sessions, conditional breakpoints, expression evaluation, and source locals are not supported by LNDBG v1 and are intentionally omitted.

## Development

```sh
npm install
npm test
```

`npm test` compiles the strict CommonJS TypeScript project and runs focused protocol, debug-map, artifact-planning, and hermetic lifecycle tests.
