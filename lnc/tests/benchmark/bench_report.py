from __future__ import annotations

import statistics
from dataclasses import asdict
from typing import Sequence

from bench_metrics import MetricPair, aggregate
from bench_models import SampleResult, VariantCaseResult, VariantLabels


METRICS = (
    "compile_ms", "binary_bytes", "assembly_bytes", "ir_bytes", "spill_loads",
    "spill_stores", "frame_size", "saved_register_ops", "instructions", "cycles",
)


def case_record(
    baseline: VariantCaseResult, candidate: VariantCaseResult, labels: VariantLabels,
) -> dict[str, object]:
    baseline_summary = _summary(baseline.samples)
    candidate_summary = _summary(candidate.samples)
    deltas = {
        metric: _delta(baseline_summary.get(metric), candidate_summary.get(metric)) for metric in METRICS
    }
    return {
        "name": baseline.name,
        "kind": baseline.kind,
        "metadata_supported": baseline.metadata_supported,
        "metadata_note": baseline.metadata_note,
        "variants": {
            labels.baseline: {"summary": baseline_summary, "samples": [asdict(item) for item in baseline.samples]},
            labels.candidate: {
                "summary": candidate_summary,
                "samples": [asdict(item) for item in candidate.samples],
            },
        },
        "comparison": {
            "candidate_minus_baseline": deltas,
            "binary_identical": baseline_summary.get("binary_sha256") == candidate_summary.get("binary_sha256"),
            "assembly_identical": baseline_summary.get("assembly_sha256") == candidate_summary.get("assembly_sha256"),
            "ir_identical": baseline_summary.get("ir_sha256") == candidate_summary.get("ir_sha256"),
            "correctness_diverged": baseline_summary.get("correctness") != candidate_summary.get("correctness"),
        },
    }


def _summary(samples: Sequence[SampleResult]) -> dict[str, object]:
    successful = tuple(sample for sample in samples if sample.compile_status == "success")
    first = successful[0] if successful else samples[0]
    values: dict[str, object] = {
        "compile_ms": statistics.median(sample.compile_ms for sample in samples),
        "compile_min_ms": min(sample.compile_ms for sample in samples),
        "compile_status": _uniform(sample.compile_status for sample in samples),
        "correctness": _uniform(sample.correctness for sample in samples),
        "asm_assertion": _uniform(sample.asm_assertion for sample in samples),
        "binary_deterministic": hashes_are_stable(tuple(sample.binary_sha256 for sample in successful)),
        "assembly_deterministic": hashes_are_stable(tuple(sample.assembly_sha256 for sample in successful)),
        "ir_deterministic": hashes_are_stable(tuple(sample.ir_sha256 for sample in successful)),
        "emulator_repeats_identical": bool(successful) and all(
            sample.emulator_repeats_identical is not False for sample in successful
        ),
    }
    for metric in METRICS[1:]:
        values[metric] = getattr(first, metric) if successful else None
    values["binary_sha256"] = first.binary_sha256 if successful else None
    values["assembly_sha256"] = first.assembly_sha256 if successful else None
    values["ir_sha256"] = first.ir_sha256 if successful else None
    return values


def hashes_are_stable(values: Sequence[str | None]) -> bool:
    return bool(values) and len(set(values)) == 1


def _uniform(values: Sequence[str]) -> str:
    distinct = tuple(dict.fromkeys(values))
    return distinct[0] if len(distinct) == 1 else "mixed: " + ", ".join(distinct)


def _delta(baseline: object | None, candidate: object | None) -> float | int | None:
    if isinstance(baseline, (int, float)) and isinstance(candidate, (int, float)):
        return candidate - baseline
    return None


def aggregate_record(cases: Sequence[dict[str, object]], labels: VariantLabels) -> dict[str, object]:
    metrics: dict[str, object] = {}
    for metric in METRICS:
        pairs = _metric_pairs(cases, metric, labels)
        counts = aggregate(pairs)
        metrics[metric] = {
            "comparable_cases": len(pairs), "candidate_wins": counts.wins,
            "ties": counts.ties, "baseline_wins": counts.losses,
            "baseline_total": sum(pair.baseline for pair in pairs),
            "candidate_total": sum(pair.candidate for pair in pairs),
            "candidate_minus_baseline": sum(pair.candidate - pair.baseline for pair in pairs),
        }
    divergences = sum(bool(_comparison(case)["correctness_diverged"]) for case in cases)
    unavailable = sum(_case_has_unavailable(case) for case in cases)
    asm_incompatibilities = sum(_case_has_asm_failure(case) for case in cases)
    binary_identical = sum(bool(_comparison(case)["binary_identical"]) for case in cases)
    variant_records = tuple(variant for case in cases for variant in _variants(case).values())
    verdict = _verdict(metrics, divergences, labels)
    return {
        "case_count": len(cases), "correctness_divergences": divergences,
        "correctness_or_emulator_unavailable": unavailable,
        "assembly_incompatibilities": asm_incompatibilities,
        "binary_identical_cases": binary_identical,
        "binary_different_cases": len(cases) - binary_identical,
        "binary_nondeterministic_variant_cases": sum(
            not _variant_hashes_stable(item, "binary_sha256") for item in variant_records
        ),
        "assembly_nondeterministic_variant_cases": sum(
            not _variant_hashes_stable(item, "assembly_sha256") for item in variant_records
        ),
        "ir_nondeterministic_variant_cases": sum(
            not _variant_hashes_stable(item, "ir_sha256") for item in variant_records
        ),
        "metrics": metrics, "verdict": verdict,
    }


def _metric_pairs(
    cases: Sequence[dict[str, object]], metric: str, labels: VariantLabels,
) -> tuple[MetricPair, ...]:
    pairs: list[MetricPair] = []
    for case in cases:
        variants = _variants(case)
        baseline = _summary_record(variants[labels.baseline]).get(metric)
        candidate = _summary_record(variants[labels.candidate]).get(metric)
        if isinstance(baseline, (int, float)) and isinstance(candidate, (int, float)):
            pairs.append(MetricPair(float(baseline), float(candidate)))
    return tuple(pairs)


def _verdict(metrics: dict[str, object], divergences: int, labels: VariantLabels) -> str:
    if divergences:
        return "no winner: correctness diverged"
    cycles = metrics["cycles"]
    instructions = metrics["instructions"]
    if isinstance(cycles, dict) and isinstance(instructions, dict):
        cycle_delta = cycles.get("candidate_minus_baseline")
        instruction_delta = instructions.get("candidate_minus_baseline")
        if isinstance(cycle_delta, (int, float)) and isinstance(instruction_delta, (int, float)):
            if cycle_delta > 0 and instruction_delta > 0:
                return f"{labels.baseline} is better overall for available executable performance; {labels.candidate} has compile-time and IR-size tradeoffs"
            if cycle_delta < 0 and instruction_delta < 0:
                return f"{labels.candidate} is better overall for available executable performance; static and compile-time tradeoffs remain"
    return "no single overall winner: executable performance metrics are tied or mixed"


def markdown_report(result: dict[str, object]) -> str:
    labels = _variant_labels(result)
    aggregate_data = result["aggregate"]
    if not isinstance(aggregate_data, dict):
        raise TypeError("aggregate record must be an object")
    lines = [
        f"# LNC JAR benchmark: {labels.baseline} vs {labels.candidate}", "", f"**Conclusion:** {aggregate_data['verdict']}.", "",
        f"Lower is better for every reported numeric metric. Deltas are `{labels.candidate} - {labels.baseline}`.", "",
        "## Reproducibility", "",
        f"- Command: `{result['command']}`", f"- Samples per variant/case: {result['sample_count']}",
        f"- Cases: {aggregate_data['case_count']}",
        f"- Correctness divergences: {aggregate_data['correctness_divergences']}",
        f"- Correctness/emulator unavailable (includes compile-only): {aggregate_data['correctness_or_emulator_unavailable']}",
        f"- Assembly assertion incompatibilities: {aggregate_data['assembly_incompatibilities']}",
        f"- Binary-identical/different cases: {aggregate_data['binary_identical_cases']}/{aggregate_data['binary_different_cases']}",
        f"- Nondeterministic binary/assembly/IR variant-cases: {aggregate_data['binary_nondeterministic_variant_cases']}/{aggregate_data['assembly_nondeterministic_variant_cases']}/{aggregate_data['ir_nondeterministic_variant_cases']}", "",
        "## Aggregate evidence", "", f"| Metric | Cases | {labels.baseline} total | {labels.candidate} total | Delta | {labels.candidate} wins/ties/{labels.baseline} wins |",
        "|---|---:|---:|---:|---:|---:|",
    ]
    metric_records = aggregate_data["metrics"]
    if not isinstance(metric_records, dict):
        raise TypeError("metric records must be an object")
    for metric in METRICS:
        row = metric_records[metric]
        if isinstance(row, dict):
            lines.append(
                f"| {metric} | {row['comparable_cases']} | {_number(row['baseline_total'])} | "
                f"{_number(row['candidate_total'])} | {_number(row['candidate_minus_baseline'])} | "
                f"{row['candidate_wins']}/{row['ties']}/{row['baseline_wins']} |"
            )
    lines.extend(("", "## Per-case evidence", "", f"| Case | Kind | Δ compile ms | Δ bin B | Δ asm B | Δ IR B | Δ spills | Δ cycles | Binary same | Correctness ({labels.baseline} / {labels.candidate}) |", "|---|---|---:|---:|---:|---:|---:|---:|:---:|---|"))
    cases = result["cases"]
    if not isinstance(cases, list):
        raise TypeError("cases must be a list")
    for case in cases:
        if isinstance(case, dict):
            lines.append(_case_row(case, labels))
    lines.extend(("", "## Interpretation", "", _interpretation(aggregate_data, labels), ""))
    return "\n".join(lines)


def _case_row(case: dict[str, object], labels: VariantLabels) -> str:
    comparison = _comparison(case)
    deltas = comparison["candidate_minus_baseline"]
    variants = _variants(case)
    correctness = f"{_summary_record(variants[labels.baseline])['correctness']} / {_summary_record(variants[labels.candidate])['correctness']}"
    spills = _sum_delta(deltas, "spill_loads", "spill_stores")
    return (
        f"| {case['name']} | {case['kind']} | {_number(deltas['compile_ms'])} | {_number(deltas['binary_bytes'])} | "
        f"{_number(deltas['assembly_bytes'])} | {_number(deltas['ir_bytes'])} | {_number(spills)} | "
        f"{_number(deltas['cycles'])} | {'yes' if comparison['binary_identical'] else 'no'} | {correctness} |"
    )


def _interpretation(aggregate_data: dict[str, object], labels: VariantLabels) -> str:
    if aggregate_data["correctness_divergences"]:
        return "No variant winner is claimed because correctness differs. Compile-only and unavailable cases retain compile/static metrics but do not contribute emulator metrics."
    return f"{labels.baseline} and {labels.candidate} agree on available correctness. Compile-only cases contribute compile/static metrics only; unavailable emulator cases are explicitly excluded from instruction/cycle totals. The win/tie/loss columns expose tradeoffs hidden by totals."


def _variant_labels(result: dict[str, object]) -> VariantLabels:
    value = result["variant_labels"]
    if not isinstance(value, dict):
        raise TypeError("variant_labels must be an object")
    baseline, candidate = value.get("baseline"), value.get("candidate")
    if not isinstance(baseline, str) or not isinstance(candidate, str):
        raise TypeError("variant labels must be strings")
    return VariantLabels(baseline=baseline, candidate=candidate)


def _variants(case: dict[str, object]) -> dict[str, object]:
    value = case["variants"]
    if not isinstance(value, dict):
        raise TypeError("variants must be an object")
    return value


def _summary_record(variant: object) -> dict[str, object]:
    if not isinstance(variant, dict) or not isinstance(variant.get("summary"), dict):
        raise TypeError("variant summary must be an object")
    return variant["summary"]


def _variant_hashes_stable(variant: object, field: str) -> bool:
    if not isinstance(variant, dict) or not isinstance(variant.get("samples"), list):
        raise TypeError("variant samples must be a list")
    values = tuple(sample.get(field) for sample in variant["samples"] if isinstance(sample, dict))
    return hashes_are_stable(values)


def _comparison(case: dict[str, object]) -> dict[str, object]:
    value = case["comparison"]
    if not isinstance(value, dict):
        raise TypeError("comparison must be an object")
    return value


def _case_has_unavailable(case: dict[str, object]) -> bool:
    return any(_summary_record(value)["correctness"] in {"unavailable", "compile_only"} for value in _variants(case).values())


def _case_has_asm_failure(case: dict[str, object]) -> bool:
    return any(_summary_record(value)["asm_assertion"] not in {"passed", "not_applicable"} for value in _variants(case).values())


def _sum_delta(values: object, first: str, second: str) -> float | int | None:
    if not isinstance(values, dict):
        return None
    left, right = values.get(first), values.get(second)
    return left + right if isinstance(left, (int, float)) and isinstance(right, (int, float)) else None


def _number(value: object) -> str:
    return f"{value:.3f}" if isinstance(value, float) else str(value) if isinstance(value, int) else "—"
