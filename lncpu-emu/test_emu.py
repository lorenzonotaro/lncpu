#!/usr/bin/env python3
"""
lncpu emulator test runner

- Discovers tests//*/ containing:
  * test.lnasm
  * pass.txt
- Compiles with lnc and a shared linker.cfg
- Runs emulator to dump status.txt and aspace.bin
- Evaluates pass conditions (registers or [addr] reads)
- Prints per-test result + summary (passed, failed, compile errors, pass-parse errors)

Author: you :)
"""

from __future__ import annotations

import os
import sys
import re
import shlex
import platform
import subprocess
from dataclasses import dataclass, field
from pathlib import Path
from typing import Dict, List, Tuple, Optional

# =========================
# CONFIGURABLE CONSTANTS
# =========================
# Paths
TESTS_ROOT: Path = Path("C:\\Users\\loryn\\Desktop\\Progetti\\lncpu\\tests")               # folder containing one subfolder per test
LINKER_CFG: Path = TESTS_ROOT / "linker.cfg"  # linker config path (parent of tests/, as requested)
TEST_FILE_NAME: str = "test.lnasm"
PASS_FILE_NAME: str = "pass.txt"

# Tools (can be absolute or on PATH)
LNC_CMD: str = os.environ.get("LNC_CMD", "lnc")                   # or e.g. "java -jar /path/to/lnc.jar"
EMU_CMD: str = os.environ.get("LNC_EMU_CMD", "C:\\Users\\loryn\\Desktop\\Progetti\\lncpu\\lncpu-emu\\cmake-build-debug\\lncpu_emu.exe")         # your emulator executable name

# Filenames produced/used inside each test folder
OUT_BIN: str = "a.out"
STATUS_TXT: str = "status.txt"
ASPACE_BIN: str = "aspace.bin"

# Timeouts (seconds)
COMPILE_TIMEOUT: int = 5
EMU_TIMEOUT: int = 5

# OS launcher (required by spec: call via bash -c / cmd /c)
IS_WINDOWS: bool = platform.system().lower().startswith("win")
SHELL_LAUNCHER: List[str] = ["cmd", "/c"] if IS_WINDOWS else ["bash", "-c"]

# =========================
# INTERNAL TYPES
# =========================
@dataclass
class PassCondition:
    is_memory: bool                 # True if [address], False if register
    key: str                        # register name (upper) or string form of address
    addr: Optional[int] = None      # address if is_memory
    expected: int = 0               # expected value (int)
    width_bits: int = 8             # 8 (registers/memory) or 16 (CS_PC)
    raw_line: str = ""              # original line for messages

@dataclass
class TestResult:
    name: str
    status: str                     # PASSED | FAILED | COMPILE_ERROR | PASS_PARSE_ERROR
    details: str = ""
    mismatches: List[str] = field(default_factory=list)

# =========================
# HELPERS
# =========================

def run_shell(cmd: list, cwd: Path, timeout: int) -> Tuple[int, str, str]:
    """Run a shell command via bash -c / cmd /c, returning (code, stdout, stderr)."""
    proc = subprocess.Popen(SHELL_LAUNCHER + cmd,
                            cwd=str(cwd),
                            stdout=subprocess.PIPE,
                            stderr=subprocess.PIPE,
                            text=True)
    try:
        out, err = proc.communicate(timeout=timeout)
    except subprocess.TimeoutExpired:
        proc.kill()
        out, err = proc.communicate()
        return 124, out, err + "\n[TIMEOUT]"
    return proc.returncode, out, err

def parse_int_literal(s: str) -> int:
    s = s.strip().lower()
    if s.startswith("0x"):
        return int(s, 16)
    if s.startswith("0b"):
        return int(s, 2)
    # decimal (allow leading + or - just in case)
    return int(s, 10)

def parse_pass_file(pass_path: Path) -> Tuple[List[PassCondition], Optional[str]]:
    """
    Returns (conditions, error_message). If error_message is not None, parsing failed.
    Lines format:
      ELEM = VALUE
    ELEM: register (RA,RB,RC,RD,SS,SP,BP,DS,CS_PC,FLAGS) or [ADDRESS]
    VALUE: decimal, 0x.., or 0b..
    """
    if not pass_path.exists():
        return [], f"Missing {pass_path.name}"

    conds: List[PassCondition] = []
    comments = ("#", ";", "//")

    with pass_path.open("r", encoding="utf-8") as f:
        for ln, raw in enumerate(f, start=1):
            line = raw.strip()
            if not line:
                continue
            # strip trailing comments
            cut = len(line)
            for c in comments:
                idx = line.find(c)
                if idx != -1:
                    cut = min(cut, idx)
            line = line[:cut].strip()
            if not line:
                continue

            m = re.match(r'^\s*(?P<elem>\[[^\]]+\]|[A-Za-z_]+)\s*=\s*(?P<val>.+?)\s*$', line)
            if not m:
                return [], f"Syntax error in {pass_path.name}:{ln}: {raw.rstrip()}"

            elem = m.group("elem").strip()
            val_str = m.group("val").strip()
            try:
                expected = parse_int_literal(val_str)
            except Exception as e:
                return [], f"Value parse error in {pass_path.name}:{ln}: {val_str} ({e})"

            if elem.startswith("[") and elem.endswith("]"):
                # memory address
                a_str = elem[1:-1].strip()
                try:
                    addr = parse_int_literal(a_str)
                except Exception as e:
                    return [], f"Address parse error in {pass_path.name}:{ln}: {a_str} ({e})"
                # If address < 0x2000, map to RAM space by adding 0x2000
                if addr < 0x2000:
                    addr = addr + 0x2000
                conds.append(PassCondition(
                    is_memory=True, key=f"[{a_str}]", addr=addr,
                    expected=expected & 0xFF, width_bits=8, raw_line=raw.rstrip()
                ))
            else:
                reg = elem.upper()
                width = 16 if reg in ("CS_PC", "CS:PC", "CS-PC", "CSPC") else 8
                conds.append(PassCondition(
                    is_memory=False, key=reg, addr=None,
                    expected=expected & (0xFFFF if width == 16 else 0xFF),
                    width_bits=width, raw_line=raw.rstrip()
                ))

    return conds, None

def parse_status_file(status_path: Path) -> Optional[Dict[str, int]]:
    """
    Parses the emulator's status.txt (format given) and returns a dict of registers:
    RA,RB,RC,RD,DS,SS,SP,BP,CSPC,CS_PC (alias),FLAGS
    """
    if not status_path.exists():
        return None
    text = status_path.read_text(encoding="utf-8", errors="replace")
    lines = text.splitlines()

    regs: Dict[str, int] = {}

    def next_values_after(header_pred, count: int, base: int) -> Optional[List[int]]:
        for i, line in enumerate(lines):
            if header_pred(line):
                if i + 1 < len(lines):
                    vals_line = lines[i + 1]
                    hexes = re.findall(r'\b[0-9A-Fa-f]{2,4}\b', vals_line)
                    # We accept 2-digit (8-bit) or 4-digit (16-bit) tokens—will slice below
                    # Make sure we grab exactly as many as needed.
                    # If too many, take first count.
                    if len(hexes) >= count:
                        return [int(x, 16) for x in hexes[:count]]
        return None

    # RA RB RC RD
    vals = next_values_after(lambda s: "RA" in s and "RB" in s and "RC" in s and "RD" in s, 4, 16)
    if not vals:
        return None
    regs["RA"], regs["RB"], regs["RC"], regs["RD"] = [v & 0xFF for v in vals]

    # DS SS SP BP
    vals = next_values_after(lambda s: "DS" in s and "SS" in s and "SP" in s and "BP" in s, 4, 16)
    if not vals:
        return None
    regs["DS"], regs["SS"], regs["SP"], regs["BP"] = [v & 0xFF for v in vals]

    # CSPC and FLAGS
    # look for line containing 'CSPC' then parse next line for 4-hex, 2-hex
    cspc = None
    flags = None
    for i, line in enumerate(lines):
        if "CSPC" in line and "FLAGS" in line:
            if i + 1 < len(lines):
                m = re.search(r'^\s*([0-9A-Fa-f]{4})\s+([0-9A-Fa-f]{2})', lines[i + 1])
                if m:
                    cspc = int(m.group(1), 16) & 0xFFFF
                    flags = int(m.group(2), 16) & 0xFF
            break
    if cspc is None or flags is None:
        return None

    regs["CSPC"] = cspc
    regs["CS_PC"] = cspc  # alias for conditions
    regs["FLAGS"] = flags
    return regs

def read_aspace(aspace_path: Path) -> Optional[bytes]:
    if not aspace_path.exists():
        return None
    return aspace_path.read_bytes()

def evaluate_conditions(conds: List[PassCondition], regs: Dict[str, int], aspace: bytes) -> List[str]:
    """
    Returns a list of mismatch messages; empty list means ALL conditions satisfied.
    """
    mismatches: List[str] = []
    for c in conds:
        try:
            if c.is_memory:
                if c.addr is None:
                    mismatches.append(f"Invalid memory cond: {c.raw_line}")
                    continue
                if c.addr < 0 or c.addr >= len(aspace):
                    mismatches.append(f"Address out of range: 0x{c.addr:04X} (len={len(aspace)})")
                    continue
                actual = aspace[c.addr]
                exp = c.expected & 0xFF
                if actual != exp:
                    mismatches.append(f"{c.raw_line}   -> got [0x{c.addr:04X}] = 0x{actual:02X}")
            else:
                key = c.key
                # normalize CS_PC variants
                if key in ("CS:PC", "CS-PC", "CSPC"):
                    key = "CS_PC"
                if key not in regs:
                    mismatches.append(f"Unknown register in condition: {c.raw_line}")
                    continue
                actual = regs[key] & (0xFFFF if c.width_bits == 16 else 0xFF)
                exp = c.expected & (0xFFFF if c.width_bits == 16 else 0xFF)
                if actual != exp:
                    if c.width_bits == 16:
                        mismatches.append(f"{c.raw_line}   -> got CS_PC = 0x{actual:04X}")
                    else:
                        mismatches.append(f"{c.raw_line}   -> got {key} = 0x{actual:02X}")
        except Exception as e:
            mismatches.append(f"Error evaluating: {c.raw_line} ({e})")
    return mismatches

def discover_tests(tests_root: Path) -> List[Path]:
    if not tests_root.exists():
        return []
    return sorted([p for p in tests_root.iterdir() if p.is_dir()])

def compile_test(test_dir: Path) -> Tuple[int, str]:
    """Compile test.lnasm -> a.out. Returns (rc, message)."""
    src = test_dir / TEST_FILE_NAME
    if not src.exists():
        return 2, f"Missing {TEST_FILE_NAME}"
    if not LINKER_CFG.exists():
        return 2, f"Missing linker cfg: {LINKER_CFG}"

    # lnc {{filename}} -lf {{tests parent folder}}/linker.cfg -oB a.out
    cmd = [LNC_CMD, src, "-lf", LINKER_CFG, "-oB", test_dir / OUT_BIN]
    # cmd_str = f'{LNC_CMD} {q(src)} -lf {q(LINKER_CFG)} -oB {q(test_dir / OUT_BIN)}'
    rc, out, err = run_shell(cmd, cwd=test_dir, timeout=COMPILE_TIMEOUT)
    if rc != 0:
        return rc, f"[compile error]\n{out}\n{err}".strip()
    return 0, out.strip() or "ok"

def run_emulator(test_dir: Path) -> Tuple[int, str]:
    """Run emulator with --rom a.out --dumpstatus status.txt --dumpaddrspace aspace.bin"""
    rom = test_dir / OUT_BIN
    if not rom.exists():
        return 2, f"Missing ROM {OUT_BIN} (compile step failed?)"

    cmd = [EMU_CMD, "--rom", rom, "--dumpstatus", test_dir / STATUS_TXT, "--dumpaddrspace", test_dir / ASPACE_BIN, "--nopauseonhalt"]

    rc, out, err = run_shell(cmd, cwd=test_dir, timeout=EMU_TIMEOUT)
    if rc != 0:
        return rc, f"[emu error]\n{out}\n{err}".strip()
    return 0, out.strip() or "ok"

def run_one_test(test_dir: Path) -> TestResult:
    name = test_dir.name

    # Parse pass file first (so we can classify PASS_PARSE_ERROR distinctly)
    conds, perr = parse_pass_file(test_dir / PASS_FILE_NAME)
    if perr:
        return TestResult(name=name, status="PASS_PARSE_ERROR", details=perr)

    # Compile
    rc, msg = compile_test(test_dir)
    if rc != 0:
        return TestResult(name=name, status="COMPILE_ERROR", details=msg)

    # Emulate
    rc, msg = run_emulator(test_dir)
    if rc != 0:
        # Spec does not call out a special bucket; count it as FAILED
        return TestResult(name=name, status="FAILED", details=msg)

    # Read outputs
    regs = parse_status_file(test_dir / STATUS_TXT)
    if regs is None:
        return TestResult(name=name, status="FAILED", details=f"Could not parse {STATUS_TXT}")

    aspace = read_aspace(test_dir / ASPACE_BIN)
    if aspace is None:
        return TestResult(name=name, status="FAILED", details=f"Missing {ASPACE_BIN}")

    # Evaluate
    mismatches = evaluate_conditions(conds, regs, aspace)
    if mismatches:
        return TestResult(name=name, status="FAILED", mismatches=mismatches)
    return TestResult(name=name, status="PASSED")

def main() -> int:
    tests = discover_tests(TESTS_ROOT)
    if not tests:
        print(f"No tests found in {TESTS_ROOT.resolve()}")
        return 2

    counts = {"PASSED": 0, "FAILED": 0, "COMPILE_ERROR": 0, "PASS_PARSE_ERROR": 0}
    results: List[TestResult] = []

    print(f"Running {len(tests)} test(s) under {TESTS_ROOT} with linker {LINKER_CFG}")
    print()

    for td in tests:
        res = run_one_test(td)
        results.append(res)
        counts[res.status] = counts.get(res.status, 0) + 1

        print(f"[{res.status:<16}] {res.name}")
        if res.details:
            print("  ", res.details.replace("\n", "\n   "))
        for m in res.mismatches:
            print("   -", m)
        print()

    total = sum(counts.values())
    print("=" * 60)
    print(f"TOTAL: {total}  PASSED: {counts['PASSED']}  FAILED: {counts['FAILED']}  "
          f"COMPILE_ERROR: {counts['COMPILE_ERROR']}  PASS_PARSE_ERROR: {counts['PASS_PARSE_ERROR']}")
    print("=" * 60)

    # Non-zero exit if anything not passed
    return 0 if counts["FAILED"] == 0 and counts["COMPILE_ERROR"] == 0 and counts["PASS_PARSE_ERROR"] == 0 else 1

if __name__ == "__main__":
    sys.exit(main())
