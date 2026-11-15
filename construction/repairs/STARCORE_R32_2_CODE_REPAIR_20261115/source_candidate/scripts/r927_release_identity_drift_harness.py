#!/usr/bin/env python3
"""Batch release identity drift fault harness.

The canonical manifest remains the single editable identity source. This harness deliberately mutates
one generated compatibility property in memory and proves exact generated-output comparison rejects it.
It is version-agnostic so each intermediate build (927..932) remains testable without stale literals.
"""
from __future__ import annotations
import json
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "scripts"))
import r926_release_manifest as release_manifest  # noqa: E402


def main() -> None:
    manifest, digest, expected_props, _, _ = release_manifest.expected()
    version_code = int(manifest["versionCode"])
    marker = f"versionCode={version_code}\n"
    if marker not in expected_props:
        raise SystemExit("R927_DRIFT_TEST_SETUP_VERSION_MARKER_MISSING")
    mutated = expected_props.replace(marker, f"versionCode={version_code - 1}\n", 1)
    if mutated == expected_props:
        raise SystemExit("R927_DRIFT_TEST_FALSE_NEGATIVE")
    actual = release_manifest.PROPS.read_text(encoding="utf-8")
    if actual != expected_props:
        raise SystemExit("R927_DRIFT_TEST_PRECONDITION_GENERATED_OUTPUT_ALREADY_DRIFTED")
    if mutated == actual:
        raise SystemExit("R927_DRIFT_TEST_MUTATION_NOT_REJECTED")
    print(json.dumps({
        "status": "PASS",
        "mutation": "versionCode-minus-one",
        "currentVersionCode": version_code,
        "mutatedVersionCode": version_code - 1,
        "manifestSha256": digest,
        "expectedFailure": "PROPERTIES_EXACT_MISMATCH"
    }, ensure_ascii=False, sort_keys=True))


if __name__ == "__main__":
    main()
