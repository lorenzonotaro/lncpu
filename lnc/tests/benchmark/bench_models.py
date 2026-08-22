from __future__ import annotations

from dataclasses import dataclass
from pathlib import Path
import re
from typing import Final


VARIANT_LABEL_PATTERN: Final = re.compile(r"^[A-Za-z0-9][A-Za-z0-9_.-]*$")


@dataclass(frozen=True, slots=True)
class VariantLabels:
    baseline: str
    candidate: str

    def __post_init__(self) -> None:
        for role, label in (("baseline", self.baseline), ("candidate", self.candidate)):
            if not VARIANT_LABEL_PATTERN.fullmatch(label):
                message = f"{role} label must match {VARIANT_LABEL_PATTERN.pattern}: {label!r}"
                raise ValueError(message)
        if self.baseline == self.candidate:
            message = f"variant labels must be distinct: {self.baseline!r}"
            raise ValueError(message)


def resolve_variant_label(jar: Path, supplied: str | None) -> str:
    if supplied is not None:
        return supplied
    return jar.stem.removeprefix("lnc_")


@dataclass(frozen=True, slots=True)
class CaseSpec:
    name: str
    directory: Path
    kind: str
    standalone: bool
    compile_args: tuple[str, ...]
    asm_must_match: str | None
    asm_must_not_match: str | None
    metadata_supported: bool
    metadata_note: str | None


@dataclass(frozen=True, slots=True)
class BenchmarkTools:
    jar: Path
    emulator: Path
    timeout: int


@dataclass(frozen=True, slots=True)
class WorkProducts:
    directory: Path
    binary: Path


@dataclass(frozen=True, slots=True)
class SampleResult:
    sample: int
    compile_ms: float
    compile_status: str
    compile_detail: str
    correctness: str
    correctness_detail: str
    asm_assertion: str
    binary_bytes: int | None
    assembly_bytes: int | None
    ir_bytes: int | None
    binary_sha256: str | None
    assembly_sha256: str | None
    ir_sha256: str | None
    spill_loads: int | None
    spill_stores: int | None
    frame_size: int | None
    saved_register_ops: int | None
    instructions: int | None
    cycles: int | None
    emulator_dump_sha256: str | None
    emulator_repeats_identical: bool | None


@dataclass(frozen=True, slots=True)
class VariantCaseResult:
    name: str
    kind: str
    metadata_supported: bool
    metadata_note: str | None
    samples: tuple[SampleResult, ...]
