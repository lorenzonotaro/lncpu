from __future__ import annotations

import hashlib
import json
import re
import subprocess
import tempfile
import time
from pathlib import Path

from bench_metrics import parse_dumpstatus, parse_lnc_codegen
from bench_models import BenchmarkTools, CaseSpec, SampleResult, WorkProducts


LINKER_CONFIG = "SECTIONS[ DUMMY: mode = fixed, start = 0x1fff; ]"
SUPPORTED_METADATA = frozenset(
    {"kind", "standalone", "compile_args", "asm_must_match", "asm_must_not_match"}
)


def sha256_bytes(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def sha256_file(path: Path) -> str:
    return sha256_bytes(path.read_bytes())


def discover_cases(tests_root: Path) -> tuple[CaseSpec, ...]:
    return tuple(_load_case(path.parent, tests_root) for path in sorted(tests_root.glob("**/test.lnc")))


def _load_case(case_dir: Path, tests_root: Path) -> CaseSpec:
    metadata_path = case_dir / "test.json"
    raw = json.loads(metadata_path.read_text(encoding="utf-8")) if metadata_path.exists() else {}
    if not isinstance(raw, dict):
        return CaseSpec(
            str(case_dir.relative_to(tests_root)), case_dir, "run", True, (), None, None,
            False, "test.json is not an object",
        )
    unknown = sorted(str(key) for key in raw if key not in SUPPORTED_METADATA)
    kind = raw.get("kind", "run")
    standalone = raw.get("standalone", True)
    compile_args = raw.get("compile_args", [])
    must = raw.get("asm_must_match")
    must_not = raw.get("asm_must_not_match")
    valid = (
        kind in {"run", "compile", "compile_fail"}
        and isinstance(standalone, bool)
        and isinstance(compile_args, list)
        and all(isinstance(value, str) for value in compile_args)
        and (must is None or isinstance(must, str))
        and (must_not is None or isinstance(must_not, str))
        and not unknown
    )
    note = None if valid else "unsupported or invalid metadata" + (f": {', '.join(unknown)}" if unknown else "")
    return CaseSpec(
        name=str(case_dir.relative_to(tests_root)),
        directory=case_dir,
        kind=str(kind) if kind in {"run", "compile", "compile_fail"} else "run",
        standalone=standalone if isinstance(standalone, bool) else True,
        compile_args=tuple(value for value in compile_args if isinstance(value, str)) if isinstance(compile_args, list) else (),
        asm_must_match=must if isinstance(must, str) else None,
        asm_must_not_match=must_not if isinstance(must_not, str) else None,
        metadata_supported=valid,
        metadata_note=note,
    )


def run_sample(tools: BenchmarkTools, case: CaseSpec, sample: int) -> SampleResult:
    with tempfile.TemporaryDirectory(prefix="lnc-lowering-bench-") as temporary:
        work = Path(temporary)
        command = ["java", "-jar", str(tools.jar), str(case.directory / "test.lnc")]
        if case.standalone:
            command.append("--standalone")
        if "-lc" not in case.compile_args:
            command.extend(("-lc", LINKER_CONFIG))
        command.extend(case.compile_args)
        command.extend((
            "-oB", str(work / "program.bin"), "-oA", str(work / "program.lnasm"),
            "-oM", str(work / "program.ir"), "-oS", str(work / "program.sym"),
        ))
        started = time.perf_counter_ns()
        compiled = subprocess.run(
            command, cwd=case.directory, text=True, capture_output=True, timeout=tools.timeout, check=False,
        )
        compile_ms = (time.perf_counter_ns() - started) / 1_000_000
        detail = (compiled.stdout + compiled.stderr).strip()[-2000:]
        if compiled.returncode != 0:
            expected = case.kind == "compile_fail"
            return _failed_sample(sample, compile_ms, "expected_failure" if expected else "compile_error", detail)
        if case.kind == "compile_fail":
            return _failed_sample(sample, compile_ms, "unexpected_success", "expected compilation failure")

        binary = work / "program.bin"
        assembly = work / "program.lnasm"
        ir = work / "program.ir"
        asm_text = assembly.read_text(encoding="utf-8")
        codegen = parse_lnc_codegen(asm_text)
        asm_status = _check_assembly(case, asm_text)
        products = WorkProducts(directory=work, binary=binary)
        correctness, correctness_detail, dump, repeat_ok, instructions, cycles = _run_executable(tools, case, products)
        return SampleResult(
            sample=sample, compile_ms=compile_ms, compile_status="success", compile_detail=detail,
            correctness=correctness, correctness_detail=correctness_detail, asm_assertion=asm_status,
            binary_bytes=binary.stat().st_size, assembly_bytes=assembly.stat().st_size,
            ir_bytes=ir.stat().st_size, binary_sha256=sha256_file(binary),
            assembly_sha256=sha256_file(assembly), ir_sha256=sha256_file(ir),
            spill_loads=codegen.spill_loads, spill_stores=codegen.spill_stores,
            frame_size=codegen.frame_size, saved_register_ops=codegen.saved_register_ops,
            instructions=instructions, cycles=cycles, emulator_dump_sha256=dump,
            emulator_repeats_identical=repeat_ok,
        )


def _failed_sample(sample: int, compile_ms: float, status: str, detail: str) -> SampleResult:
    correctness = "unavailable" if status == "compile_error" else status
    return SampleResult(
        sample, compile_ms, status, detail, correctness, detail, "unavailable",
        None, None, None, None, None, None, None, None, None, None, None,
        None, None, None, None,
    )


def _check_assembly(case: CaseSpec, assembly: str) -> str:
    try:
        if case.asm_must_match is not None and re.search(case.asm_must_match, assembly) is None:
            return "missing_required_pattern"
        if case.asm_must_not_match is not None and re.search(case.asm_must_not_match, assembly) is not None:
            return "matched_forbidden_pattern"
    except re.error as error:
        return f"invalid_regex: {error}"
    return "passed" if case.asm_must_match is not None or case.asm_must_not_match is not None else "not_applicable"


def _run_executable(
    tools: BenchmarkTools, case: CaseSpec, products: WorkProducts,
) -> tuple[str, str, str | None, bool | None, int | None, int | None]:
    if case.kind == "compile":
        return "compile_only", "kind: compile", None, None, None, None
    pass_file = case.directory / "pass.txt"
    if not case.metadata_supported or not pass_file.exists():
        reason = case.metadata_note or "missing pass.txt"
        return "unavailable", reason, None, None, None, None
    dumps: list[bytes] = []
    spaces: list[bytes] = []
    metrics = None
    for repeat in range(2):
        status = products.directory / f"status-{repeat}.txt"
        address_space = products.directory / f"address-space-{repeat}.bin"
        process = subprocess.run([
            str(tools.emulator), f"--rom={products.binary}", f"--expect={pass_file}",
            f"--dumpstatus={status}", f"--dumpaddrspace={address_space}", "--nopauseonhalt",
        ], cwd=case.directory, text=True, capture_output=True, timeout=tools.timeout, check=False)
        if process.returncode != 0:
            return "failed", (process.stdout + process.stderr).strip(), None, False, None, None
        dumps.append(status.read_bytes())
        spaces.append(address_space.read_bytes())
        metrics = parse_dumpstatus(dumps[-1].decode("utf-8"))
    identical = dumps[0] == dumps[1] and spaces[0] == spaces[1]
    if metrics is None:
        return "unavailable", "missing emulator metrics", None, identical, None, None
    return "passed", "expectations satisfied", sha256_bytes(dumps[0]), identical, metrics.instructions, metrics.cycles
