#!/usr/bin/env -S uv run --script
# /// script
# requires-python = ">=3.11"
# dependencies = []
# ///

# ─── How to run ───
# 1. Install uv (if not installed):
#      curl -LsSf https://astral.sh/uv/install.sh | sh
# 2. Run directly (no venv, no pip install needed):
#      uv run run_lowering_benchmark.py --samples 3
# 3. Or make executable and run:
#      chmod +x run_lowering_benchmark.py && ./run_lowering_benchmark.py --samples 3
# ──────────────────

from __future__ import annotations

import argparse
import json
import platform
import shlex
import sys
from collections import defaultdict
from datetime import UTC, datetime
from pathlib import Path

from bench_models import BenchmarkTools, VariantCaseResult, VariantLabels, resolve_variant_label
from bench_report import aggregate_record, case_record, markdown_report
from bench_runner import discover_cases, run_sample, sha256_file


def main() -> int:
    tests_root = Path(__file__).resolve().parent.parent
    repo_root = tests_root.parent.parent
    parser = argparse.ArgumentParser(description="Compare two supplied LNC compiler JARs.")
    parser.add_argument("--samples", type=int, default=3)
    parser.add_argument("--timeout", type=int, default=30)
    parser.add_argument("--baseline-jar", dest="baseline_jar", type=Path, default = repo_root / "lnc" / "target" / "lnc.jar")
    parser.add_argument("--candidate-jar", dest="candidate_jar", type=Path, default = repo_root / "lnc" / "test" / "lnc_candidate.jar")
    parser.add_argument("--baseline-label")
    parser.add_argument("--candidate-label")
    parser.add_argument("--emulator", type=Path, default=repo_root / "lncpu-emu" / "build" / "lncpu_emu")
    parser.add_argument("--output-dir", type=Path, default=tests_root / "bench_results")
    args = parser.parse_args()


    if args.samples < 3:
        parser.error("--samples must be at least 3")

    try:
        labels = VariantLabels(
            baseline=resolve_variant_label(args.baseline_jar, args.baseline_label),
            candidate=resolve_variant_label(args.candidate_jar, args.candidate_label),
        )
    except ValueError as exc:
        parser.error(str(exc))
    variants = ((labels.baseline, args.baseline_jar.resolve()), (labels.candidate, args.candidate_jar.resolve()))
    emulator = args.emulator.resolve()
    for _, jar in variants:
        if not jar.is_file():
            parser.error(f"JAR not found: {jar}")
    if not emulator.is_file():
        parser.error(f"emulator not found: {emulator}")
    cases = discover_cases(tests_root)
    hashes_before = {label: sha256_file(path) for label, path in variants} | {"emulator": sha256_file(emulator)}
    observations: dict[tuple[str, str], list] = defaultdict(list)

    for case_index, case in enumerate(cases):
        for sample in range(args.samples):
            order = variants if (case_index + sample) % 2 == 0 else tuple(reversed(variants))
            for label, jar in order:
                print(f"[{case.name}] sample {sample + 1}/{args.samples} {label}", flush=True)
                tools = BenchmarkTools(jar=jar, emulator=emulator, timeout=args.timeout)
                observations[(case.name, label)].append(run_sample(tools, case, sample))

    records = []
    for case in cases:
        baseline = VariantCaseResult(
            case.name, case.kind, case.metadata_supported, case.metadata_note,
            tuple(observations[(case.name, labels.baseline)]),
        )
        candidate = VariantCaseResult(
            case.name, case.kind, case.metadata_supported, case.metadata_note,
            tuple(observations[(case.name, labels.candidate)]),
        )
        records.append(case_record(baseline, candidate, labels))

    hashes_after = {label: sha256_file(path) for label, path in variants} | {"emulator": sha256_file(emulator)}
    command = " ".join(shlex.quote(part) for part in sys.argv)
    result: dict[str, object] = {
        "schema_version": 2,
        "generated_at_utc": datetime.now(UTC).isoformat(),
        "command": command,
        "sample_count": args.samples,
        "variant_labels": {"baseline": labels.baseline, "candidate": labels.candidate},
        "execution": {"serial": True, "alternating_order": True, "emulator_repeats_per_binary": 2},
        "environment": {"python": sys.version, "platform": platform.platform()},
        "tools": {
            labels.baseline: {"path": str(variants[0][1]), "sha256_before": hashes_before[labels.baseline], "sha256_after": hashes_after[labels.baseline]},
            labels.candidate: {"path": str(variants[1][1]), "sha256_before": hashes_before[labels.candidate], "sha256_after": hashes_after[labels.candidate]},
            "emulator": {"path": str(emulator), "sha256_before": hashes_before["emulator"], "sha256_after": hashes_after["emulator"]},
        },
        "cases": records,
        "aggregate": aggregate_record(records, labels),
    }
    result["tool_hashes_unchanged"] = hashes_before == hashes_after
    args.output_dir.mkdir(parents=True, exist_ok=True)
    json_path = args.output_dir / "benchmark.json"
    report_path = args.output_dir / "benchmark.md"
    json_path.write_text(json.dumps(result, indent=2, sort_keys=True) + "\n", encoding="utf-8")
    report_path.write_text(markdown_report(result), encoding="utf-8")
    print(f"wrote {json_path}")
    print(f"wrote {report_path}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
