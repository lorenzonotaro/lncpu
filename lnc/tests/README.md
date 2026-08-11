# lnc compiler stability tests

Each executable test lives in its own directory and contains:

- `test.lnc`: the source program compiled with `--standalone`.
- `pass.txt`: final emulator pass conditions, consumed directly by `lncpu-emu --expect`.

`pass.txt` accepts the emulator expectation format:

```text
RA = 0x12
CSPC = 0x0010
HALTED = 1
[0x2000] = 42
```

Values may be decimal, `0x` hexadecimal, or `0b` binary. Comments start with `#`, `;`, or `//`.

Compile-failure tests add `test.json`:

```json
{
  "kind": "compile_fail",
  "expect_stderr_contains": "diagnostic substring"
}
```

Executable tests may also add assembly-shape assertions to `test.json`. Both
values are non-empty Python regular-expression strings evaluated against the
emitted `program.lnasm` after compilation and before emulation:

```json
{
  "asm_must_match": "(?m)^\\s*add\\s+(R[ABCD]),\\s*\\1\\s*$",
  "asm_must_not_match": "(?m)^\\s*shl\\b"
}
```

`asm_must_match` must match at least once; `asm_must_not_match` must not
match. An invalid or empty regex fails the case.

Run the full suite from this directory or the repository root:

```sh
python3 lnc/tests/run_lnc_tests.py
```

The runner builds `lnc` and `lncpu-emu`, compiles each test, runs executable cases on the emulator, and prints a summary.
