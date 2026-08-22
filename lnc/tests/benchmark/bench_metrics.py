from __future__ import annotations

import re
import statistics
from dataclasses import dataclass
from typing import Final, Sequence


@dataclass(frozen=True, slots=True)
class DumpMetrics:
    instructions: int
    cycles: int


@dataclass(frozen=True, slots=True)
class CodegenMetrics:
    spill_loads: int
    spill_stores: int
    frame_size: int
    saved_register_ops: int


@dataclass(frozen=True, slots=True)
class MetricPair:
    baseline: float
    candidate: float


@dataclass(frozen=True, slots=True)
class Aggregate:
    wins: int
    ties: int
    losses: int


@dataclass(frozen=True, slots=True)
class SampleSummary:
    median: float
    minimum: float


_TOTALS: Final = re.compile(r"(?m)^INSTRUCTIONS\s+CYCLES\s*$\n\s*(\d+)\s+(\d+)\s*$")
_SECTION: Final = re.compile(r"(?ms)^\s*\.section\s+LNCCODE\s*$\n(.*?)(?=^\s*\.section\s+|\Z)")
_BP_LOAD: Final = re.compile(r"(?im)^\s*mov\s+[^,\n]+,\s*\[\s*BP\s*[+-]")
_BP_STORE: Final = re.compile(r"(?im)^\s*mov\s+\[\s*BP\s*[+-][^\n]*,\s*[^\n]+$")
_FRAME: Final = re.compile(r"(?im)^\s*add\s+SP\s*,\s*(0x[0-9a-f]+|\d+)\s*$")
_SAVE_RESTORE: Final = re.compile(r"(?im)^\s*(?:push|pop)\s+R[ABCD]\s*$")


def parse_dumpstatus(status: str) -> DumpMetrics:
    match = _TOTALS.search(status)
    if match is None:
        raise ValueError("dump status has no INSTRUCTIONS/CYCLES totals")
    return DumpMetrics(instructions=int(match.group(1)), cycles=int(match.group(2)))


def parse_lnc_codegen(assembly: str) -> CodegenMetrics:
    sections = _SECTION.findall(assembly)
    code = "\n".join(sections)
    frames = tuple(int(value, 0) for value in _FRAME.findall(code))
    return CodegenMetrics(
        spill_loads=len(_BP_LOAD.findall(code)),
        spill_stores=len(_BP_STORE.findall(code)),
        frame_size=max(frames, default=0),
        saved_register_ops=len(_SAVE_RESTORE.findall(code)),
    )


def aggregate(pairs: Sequence[MetricPair]) -> Aggregate:
    wins = sum(pair.candidate < pair.baseline for pair in pairs)
    ties = sum(pair.candidate == pair.baseline for pair in pairs)
    return Aggregate(wins=wins, ties=ties, losses=len(pairs) - wins - ties)


def summarize_samples(samples: Sequence[float]) -> SampleSummary:
    if not samples:
        raise ValueError("at least one sample is required")
    return SampleSummary(median=statistics.median(samples), minimum=min(samples))
