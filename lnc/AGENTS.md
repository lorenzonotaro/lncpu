# AGENTS.md

## Big Picture
- `lnc` is a single CLI that can compile `.lnc/.lnh` to `lnasm` and then assemble/link to device binaries.
- Main orchestration is in `src/main/java/com/lnc/LNC.java` (`runFromSourceFiles()`): parse args -> compile `lnc` inputs -> assemble/link all inputs.
- Compiler pipeline is staged in `src/main/java/com/lnc/cc/Compiler.java`: lexer -> preprocessor -> parser -> AST analysis -> IR gen/analysis/lowering -> IR opt -> codegen -> asm opt.
- Assembler pipeline is in `src/main/java/com/lnc/assembler/Assembler.java`: line lexer -> linker config parse -> preprocessor -> parser -> binary linker -> file outputs/disassembly.
- `CompilerOutput` objects are the bridge between compiler and assembler; assembler merges compiler-generated section metadata into user linker config.

## Key Areas
- `src/main/java/com/lnc/cc/**`: lnc frontend, IR, optimizers, codegen.
- `src/main/java/com/lnc/assembler/**`: lnasm parser, linker, disassembler, symbol-table I/O.
- `src/main/java/com/lnc/common/frontend/**`: shared lexer/token/preprocessor infrastructure for both languages.
- `src/main/resources/default-settings.json`: canonical CLI option registry (types + defaults + help text).
- `lib/*.lnasm`: shipped assembly includes; include path auto-adds `<jar dir>/lib` plus `-I` entries.

## Build, Test, Run
- Build (verified): `mvn -q -DskipTests package` from repo root.
- Tests (verified): `mvn -q test` (currently no substantial `src/test` suite; most validation is integration-style runs).
- Run jar: `java -jar target/lnc.jar <sources...> [options]`.
- Default output behavior: if neither `-oI` nor `-oB` is provided, `LNC.parseArgs()` sets `-oB=a.out`.
- Linker config resolution in `LNC.getLinkerConfig()`: `-lc` XOR `-lf`; otherwise fallback to `./linker.cfg` if present.

## Project-Specific Conventions
- Options are data-driven: add/update flags in `default-settings.json`, then consume via `LNC.settings.get(...)`.
- Error reporting relies on pipeline state labels (`Logger.setProgramState("parser")`, etc.); keep stage strings meaningful when adding passes.
- Include-path separator is semicolon (`;`) on CLI (`-I`) and symbol-table list (`-S`).
- Preprocessor include resolution order (`Preprocessor.resolvePath`): includer dir -> CWD -> `LNC.includeDirs`.
- Standalone mode is strict: `--standalone` requires `void main()` with no params and injects `_START` bootstrap from `CodeSnippets.STANDALONE_START_CODE_OUTPUT`.

## Integration Points and Data Contracts
- Instruction metadata is resource-driven: `OpcodeMap` loads `src/main/resources/opcodes.tsv` at class init.
- Link targets are enum-based (`ROM`, `RAM`, `D0`..`D5`) and parsed from `-oD` (comma-separated).
- Multi-target outputs append suffixes for non-ROM targets via `Assembler.appendTargetToFilename()`.
- External symbol tables are imported with `-S` and exported with `-oS` (`ExternalSymbolTableIO` path in assembler linker flow).
- Lnasm section semantics depend on linker script properties (`mode`, `target`, `datapage`, `virtual`, `multi`) documented in `readme.md` and enforced by linker config parser.

## Practical Agent Tips
- When debugging failures, reproduce with `-s` (syntax only) and inspect reported stage in `lnc(<stage>): error: ...`.
- For compiler changes, validate both intermediate outputs (`-oM`, `-oA`) and final binaries (`-oB`/`-oI`).
- For preprocessor/linker changes, test `%include`, `%ifdef SECTION ...`, and mixed `.lnc + .lnasm` inputs together.
