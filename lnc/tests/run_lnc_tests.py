#!/usr/bin/env python3
from __future__ import annotations

import argparse
import json
import re
import subprocess
import sys
import tempfile
from dataclasses import dataclass
from pathlib import Path


LINKER_CONFIG = "SECTIONS[ DUMMY: mode = fixed, start = 0x1fff; ]"


@dataclass
class CaseResult:
    name: str
    status: str
    details: str = ""


def run(cmd: list[str], cwd: Path, timeout: int) -> subprocess.CompletedProcess[str]:
    return subprocess.run(cmd, cwd=cwd, text=True, capture_output=True, timeout=timeout)


def build_lnc(lnc_root: Path) -> None:
    proc = run(["mvn", "-q", "-DskipTests", "package"], lnc_root, 120)
    if proc.returncode != 0:
        raise RuntimeError("lnc build failed\n" + proc.stdout + proc.stderr)


def build_emulator(repo_root: Path) -> Path:
    emu_root = repo_root / "lncpu-emu"
    gen = run([sys.executable, "gen_opcodes_h.py"], emu_root, 30)
    if gen.returncode != 0:
        raise RuntimeError("opcode header generation failed\n" + gen.stdout + gen.stderr)

    configure = run(["cmake", "-S", str(emu_root), "-B", str(emu_root / "build")], repo_root, 120)
    if configure.returncode != 0:
        raise RuntimeError("emulator configure failed\n" + configure.stdout + configure.stderr)

    build = run(["cmake", "--build", str(emu_root / "build")], repo_root, 120)
    if build.returncode != 0:
        raise RuntimeError("emulator build failed\n" + build.stdout + build.stderr)

    candidates = [
        emu_root / "build" / "lncpu_emu",
        emu_root / "build" / "lncpu_emu.exe",
        emu_root / "build" / "Release" / "lncpu_emu",
        emu_root / "build" / "Release" / "lncpu_emu.exe",
    ]
    for candidate in candidates:
        if candidate.exists():
            return candidate
    raise RuntimeError("emulator executable not found after build")


def discover_cases(tests_root: Path) -> list[Path]:
    return sorted(path.parent for path in tests_root.glob("**/test.lnc"))


def load_metadata(case_dir: Path) -> dict[str, object]:
    meta = case_dir / "test.json"
    if not meta.exists():
        return {"kind": "run"}
    with meta.open("r", encoding="utf-8") as f:
        return json.load(f)


def compile_case(jar: Path, case_dir: Path, work_dir: Path, timeout: int) -> subprocess.CompletedProcess[str]:
    return run([
        "java", "-jar", str(jar), str(case_dir / "test.lnc"),
        "--standalone",
        "-lc", LINKER_CONFIG,
        "-oB", str(work_dir / "program.bin"),
        "-oA", str(work_dir / "program.lnasm"),
        "-oM", str(work_dir / "program.ir"),
        "-oS", str(work_dir / "program.sym"),
    ], case_dir, timeout)


def run_case(jar: Path, emulator: Path, tests_root: Path, case_dir: Path, timeout: int) -> CaseResult:
    name = str(case_dir.relative_to(tests_root))
    metadata = load_metadata(case_dir)
    kind = str(metadata.get("kind", "run"))

    with tempfile.TemporaryDirectory(prefix="lnc-test-") as td:
        work_dir = Path(td)
        compile_proc = compile_case(jar, case_dir, work_dir, timeout)
        compile_output = (compile_proc.stdout + compile_proc.stderr).strip()

        if kind == "compile_fail":
            if compile_proc.returncode == 0:
                return CaseResult(name, "FAILED", "expected compilation to fail, but it succeeded")
            expected = str(metadata.get("expect_stderr_contains", ""))
            if expected and expected not in compile_output:
                return CaseResult(name, "FAILED", "missing expected diagnostic: " + expected + "\n" + compile_output)
            return CaseResult(name, "PASSED")

        if compile_proc.returncode != 0:
            return CaseResult(name, "COMPILE_ERROR", compile_output)

        assembly = (work_dir / "program.lnasm").read_text(encoding="utf-8")
        for key, requires_match in (("asm_must_match", True), ("asm_must_not_match", False)):
            pattern = metadata.get(key)
            if pattern is None:
                continue
            if not isinstance(pattern, str) or not pattern:
                return CaseResult(name, "FAILED", f"{key} must be a non-empty regex string")
            try:
                matched = re.search(pattern, assembly) is not None
            except re.error as exc:
                return CaseResult(name, "FAILED", f"invalid {key} regex: {exc}")
            if requires_match and not matched:
                return CaseResult(name, "FAILED", f"assembly missing required pattern: {pattern}")
            if not requires_match and matched:
                return CaseResult(name, "FAILED", f"assembly matched forbidden pattern: {pattern}")

        pass_file = case_dir / "pass.txt"
        if not pass_file.exists():
            return CaseResult(name, "FAILED", "missing pass.txt")

        emu_proc = run([
            str(emulator),
            "--rom", str(work_dir / "program.bin"),
            "--expect", str(pass_file),
            "--nopauseonhalt",
        ], case_dir, timeout)
        emu_output = (emu_proc.stdout + emu_proc.stderr).strip()
        if emu_proc.returncode != 0:
            return CaseResult(name, "FAILED", emu_output)

    return CaseResult(name, "PASSED")


def main() -> int:
    parser = argparse.ArgumentParser(description="Build and run the lnc compiler stability suite.")
    parser.add_argument("--no-build", action="store_true", help="skip building lnc and lncpu-emu")
    parser.add_argument("--timeout", type=int, default=20, help="per-command timeout in seconds")
    args = parser.parse_args()

    tests_root = Path(__file__).resolve().parent
    lnc_root = tests_root.parent
    repo_root = lnc_root.parent
    jar = lnc_root / "target" / "lnc.jar"

    try:
        if not args.no_build:
            build_lnc(lnc_root)
            emulator = build_emulator(repo_root)
        else:
            emulator_candidates = [repo_root / "lncpu-emu" / "build" / "lncpu_emu", repo_root / "lncpu-emu" / "build" / "lncpu_emu.exe"]
            emulator = next((candidate for candidate in emulator_candidates if candidate.exists()), emulator_candidates[0])
    except RuntimeError as exc:
        print(exc, file=sys.stderr)
        return 2

    cases = discover_cases(tests_root)
    if not cases:
        print("No test cases found.")
        return 2

    results = [run_case(jar, emulator, tests_root, case, args.timeout) for case in cases]
    counts: dict[str, int] = {}
    for result in results:
        counts[result.status] = counts.get(result.status, 0) + 1
        print(f"[{result.status:<13}] {result.name}")
        if result.details:
            print("  " + result.details.replace("\n", "\n  "))

    print("=" * 72)
    print("TOTAL: {total}  PASSED: {passed}  FAILED: {failed}  COMPILE_ERROR: {compile_error}".format(
        total=len(results),
        passed=counts.get("PASSED", 0),
        failed=counts.get("FAILED", 0),
        compile_error=counts.get("COMPILE_ERROR", 0),
    ))
    print("=" * 72)

    return 0 if all(result.status == "PASSED" for result in results) else 1


if __name__ == "__main__":
    raise SystemExit(main())
