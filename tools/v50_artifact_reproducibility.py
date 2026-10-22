#!/usr/bin/env python3
"""Compare regenerated V50 artifacts without assuming identical BLAS rounding.

Training and evaluation identities, shapes, labels, counts, booleans and hashes
remain exact. Floating-point payloads must agree within a deliberately small
cross-platform tolerance. Each artifact's own parameter SHA-256 is also
recomputed from its serialized Java doubles so a stale or hand-edited artifact
cannot pass merely because it is numerically close to another file.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import math
import re
import struct
from pathlib import Path
from typing import Any


ARTIFACTS = (
    "V50NumberModelArtifact.java",
    "V50ActionRankerArtifact.java",
    "V50SelectedCalibrationArtifact.java",
    "V50SelectivePolicyArtifact.java",
)


def require(condition: bool, code: str, detail: str = "") -> None:
    if not condition:
        raise AssertionError(code + (": " + detail if detail else ""))


def java_string(source: str, name: str) -> str:
    match = re.search(
        r"public\s+static\s+final\s+String\s+%s\s*=\s*"
        r"(\"(?:\\.|[^\"\\])*\")\s*;" % re.escape(name),
        source,
        re.DOTALL,
    )
    require(match is not None, "JAVA_STRING_MISSING", name)
    return str(json.loads(match.group(1)))


def java_double(source: str, name: str) -> float:
    match = re.search(
        r"public\s+static\s+final\s+double\s+%s\s*=\s*"
        r"([-+0-9.eE]+)d?\s*;" % re.escape(name),
        source,
    )
    require(match is not None, "JAVA_DOUBLE_MISSING", name)
    return float(match.group(1))


def java_int(source: str, name: str) -> int:
    match = re.search(
        r"public\s+static\s+final\s+int\s+%s\s*=\s*([0-9]+)\s*;"
        % re.escape(name),
        source,
    )
    require(match is not None, "JAVA_INT_MISSING", name)
    return int(match.group(1))


def java_bool(source: str, name: str) -> bool:
    match = re.search(
        r"public\s+static\s+final\s+boolean\s+%s\s*=\s*(true|false)\s*;"
        % re.escape(name),
        source,
    )
    require(match is not None, "JAVA_BOOLEAN_MISSING", name)
    return match.group(1) == "true"


def java_double_array(source: str, name: str) -> list[float]:
    match = re.search(
        r"public\s+static\s+final\s+double\[\]\s+%s\s*=\s*"
        r"new\s+double\[\]\s*\{(.*?)\}\s*;" % re.escape(name),
        source,
        re.DOTALL,
    )
    require(match is not None, "JAVA_DOUBLE_ARRAY_MISSING", name)
    value = match.group(1).strip()
    return [] if not value else [
        float(item.strip().rstrip("dD")) for item in value.split(",")
    ]


def java_double_matrix(source: str, name: str) -> list[list[float]]:
    match = re.search(
        r"public\s+static\s+final\s+double\[\]\[\]\s+%s\s*=\s*"
        r"new\s+double\[\]\[\]\s*\{(.*?)\}\s*;" % re.escape(name),
        source,
        re.DOTALL,
    )
    require(match is not None, "JAVA_DOUBLE_MATRIX_MISSING", name)
    return [
        [float(item.strip().rstrip("dD"))
         for item in row.split(",") if item.strip()]
        for row in re.findall(r"\{([^{}]*)\}", match.group(1))
    ]


def java_string_array(source: str, name: str) -> list[str]:
    match = re.search(
        r"public\s+static\s+final\s+String\[\]\s+%s\s*=\s*"
        r"new\s+String\[\]\s*\{(.*?)\}\s*;" % re.escape(name),
        source,
        re.DOTALL,
    )
    require(match is not None, "JAVA_STRING_ARRAY_MISSING", name)
    return [
        str(json.loads(item))
        for item in re.findall(r"\"(?:\\.|[^\"\\])*\"", match.group(1))
    ]


def compare_value(left: Any, right: Any, path: str, tolerance: float,
                  maximum: list[float]) -> None:
    if isinstance(left, bool) or isinstance(right, bool):
        require(left is right, "BOOLEAN_DRIFT", path)
        return
    if isinstance(left, (int, float)) and isinstance(right, (int, float)):
        if isinstance(left, int) and isinstance(right, int):
            require(left == right, "INTEGER_DRIFT",
                    "%s: %r != %r" % (path, left, right))
            return
        delta = abs(float(left) - float(right))
        maximum[0] = max(maximum[0], delta)
        require(
            math.isclose(float(left), float(right),
                         rel_tol=tolerance, abs_tol=tolerance),
            "FLOAT_DRIFT",
            "%s: %.17g != %.17g (delta=%.3g)"
            % (path, float(left), float(right), delta),
        )
        return
    require(type(left) is type(right), "TYPE_DRIFT", path)
    if isinstance(left, dict):
        require(set(left) == set(right), "KEY_DRIFT", path)
        for key in sorted(left):
            compare_value(left[key], right[key], path + "." + str(key),
                          tolerance, maximum)
        return
    if isinstance(left, list):
        require(len(left) == len(right), "LENGTH_DRIFT", path)
        for index, (a, b) in enumerate(zip(left, right)):
            compare_value(a, b, "%s[%d]" % (path, index),
                          tolerance, maximum)
        return
    require(left == right, "VALUE_DRIFT",
            "%s: %r != %r" % (path, left, right))


def compare_float_list(left: list[float], right: list[float], path: str,
                       tolerance: float, maximum: list[float]) -> None:
    compare_value(left, right, path, tolerance, maximum)


def parameter_hash(values: list[float]) -> str:
    payload = struct.pack("<%dd" % len(values), *values)
    return hashlib.sha256(payload).hexdigest()


def verify_parameter_hash(source: str, artifact: str) -> None:
    if artifact == "V50NumberModelArtifact.java":
        values = (
            java_double_array(source, "MEAN")
            + java_double_array(source, "SCALE")
            + java_double_array(source, "WEIGHT")
        )
        for row in java_double_matrix(source, "POSITION_NUMBER_BIAS"):
            values.extend(row)
        values.extend(java_double_array(source, "NUMBER_BIAS"))
    elif artifact == "V50ActionRankerArtifact.java":
        values = (
            java_double_array(source, "MEAN")
            + java_double_array(source, "SCALE")
            + java_double_array(source, "WEIGHT")
            + [java_double(source, "BIAS")]
        )
    elif artifact == "V50SelectedCalibrationArtifact.java":
        values = (
            java_double_array(source, "X")
            + java_double_array(source, "Y")
        )
    else:
        return
    expected = java_string(source, "PARAMETER_SHA256")
    require(parameter_hash(values) == expected,
            "PARAMETER_HASH_INVALID", artifact)


def compare_artifact(name: str, committed: str, generated: str,
                     tolerance: float, maximum: list[float]) -> None:
    verify_parameter_hash(committed, name)
    verify_parameter_hash(generated, name)
    if name == "V50NumberModelArtifact.java":
        for field in (
            "MODEL_ID", "DATA_SHA256", "FEATURE_CONTRACT",
            "TRAINING_PERIOD_START", "TRAINING_PERIOD_END",
        ):
            require(java_string(committed, field) == java_string(generated, field),
                    "STRING_DRIFT", name + "." + field)
        for field in ("TRAINING_TARGET_COUNT", "FEATURE_COUNT"):
            require(java_int(committed, field) == java_int(generated, field),
                    "INTEGER_DRIFT", name + "." + field)
        require(java_string_array(committed, "FEATURE_NAMES")
                == java_string_array(generated, "FEATURE_NAMES"),
                "FEATURE_NAME_DRIFT", name)
        compare_value(
            json.loads(java_string(committed, "OOS_METRICS_JSON")),
            json.loads(java_string(generated, "OOS_METRICS_JSON")),
            name + ".OOS_METRICS_JSON", tolerance, maximum,
        )
        for field in ("MEAN", "SCALE", "WEIGHT", "NUMBER_BIAS"):
            compare_float_list(
                java_double_array(committed, field),
                java_double_array(generated, field),
                name + "." + field, tolerance, maximum,
            )
        compare_value(
            java_double_matrix(committed, "POSITION_NUMBER_BIAS"),
            java_double_matrix(generated, "POSITION_NUMBER_BIAS"),
            name + ".POSITION_NUMBER_BIAS", tolerance, maximum,
        )
        return
    if name == "V50ActionRankerArtifact.java":
        for field in ("ARTIFACT_ID", "DATA_SHA256"):
            require(java_string(committed, field) == java_string(generated, field),
                    "STRING_DRIFT", name + "." + field)
        for field in ("FEATURE_COUNT", "SCORE_DECIMALS"):
            require(java_int(committed, field) == java_int(generated, field),
                    "INTEGER_DRIFT", name + "." + field)
        require(java_bool(committed, "OOS_RANK_ORDER_PASSED")
                == java_bool(generated, "OOS_RANK_ORDER_PASSED"),
                "BOOLEAN_DRIFT", name + ".OOS_RANK_ORDER_PASSED")
        require(java_string_array(committed, "FEATURE_NAMES")
                == java_string_array(generated, "FEATURE_NAMES"),
                "FEATURE_NAME_DRIFT", name)
        compare_value(
            json.loads(java_string(committed, "OOS_METRICS_JSON")),
            json.loads(java_string(generated, "OOS_METRICS_JSON")),
            name + ".OOS_METRICS_JSON", tolerance, maximum,
        )
        for field in ("OOS_RANK1_RATE", "OOS_RANK2_RATE", "OOS_RANK3_RATE",
                      "SCORE_SCALE", "BIAS"):
            compare_value(java_double(committed, field),
                          java_double(generated, field),
                          name + "." + field, tolerance, maximum)
        for field in ("MEAN", "SCALE", "WEIGHT"):
            compare_float_list(
                java_double_array(committed, field),
                java_double_array(generated, field),
                name + "." + field, tolerance, maximum,
            )
        return
    if name == "V50SelectedCalibrationArtifact.java":
        for field in ("ARTIFACT_ID", "DATA_SHA256"):
            require(java_string(committed, field) == java_string(generated, field),
                    "STRING_DRIFT", name + "." + field)
        compare_value(
            json.loads(java_string(committed, "OOS_METRICS_JSON")),
            json.loads(java_string(generated, "OOS_METRICS_JSON")),
            name + ".OOS_METRICS_JSON", tolerance, maximum,
        )
        for field in ("OOS_ECE", "OOS_HIT_RATE"):
            compare_value(java_double(committed, field),
                          java_double(generated, field),
                          name + "." + field, tolerance, maximum)
        for field in ("X", "Y"):
            compare_float_list(
                java_double_array(committed, field),
                java_double_array(generated, field),
                name + "." + field, tolerance, maximum,
            )
        return
    for field in ("ARTIFACT_ID", "DATA_SHA256"):
        require(java_string(committed, field) == java_string(generated, field),
                "STRING_DRIFT", name + "." + field)
    compare_value(
        json.loads(java_string(committed, "OOS_REPORT_JSON")),
        json.loads(java_string(generated, "OOS_REPORT_JSON")),
        name + ".OOS_REPORT_JSON", tolerance, maximum,
    )
    for field in (
        "GLOBAL_SELECTED_P_THRESHOLD", "GLOBAL_RANK_SCORE_TIE_THRESHOLD",
        "MIN_COVERAGE",
        "TARGET_COVERAGE_LOW", "TARGET_COVERAGE_HIGH", "MIN_HIT_RATE",
        "MIN_BLOCK_HIT_RATE", "MAX_ECE", "OOS_COVERAGE",
        "OOS_HIT_RATE", "OOS_ECE",
    ):
        compare_value(java_double(committed, field),
                      java_double(generated, field),
                      name + "." + field, tolerance, maximum)
    require(java_bool(committed, "MODEL_GATE_PASSED")
            == java_bool(generated, "MODEL_GATE_PASSED"),
            "BOOLEAN_DRIFT", name + ".MODEL_GATE_PASSED")


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--committed-dir", required=True)
    parser.add_argument("--generated-dir", required=True)
    parser.add_argument("--committed-report", required=True)
    parser.add_argument("--generated-report", required=True)
    parser.add_argument("--tolerance", type=float, default=1.0e-7)
    args = parser.parse_args()
    maximum = [0.0]
    committed_dir = Path(args.committed_dir)
    generated_dir = Path(args.generated_dir)
    for artifact in ARTIFACTS:
        compare_artifact(
            artifact,
            (committed_dir / artifact).read_text("utf-8"),
            (generated_dir / artifact).read_text("utf-8"),
            args.tolerance,
            maximum,
        )
    committed_report = json.loads(Path(args.committed_report).read_text("utf-8"))
    generated_report = json.loads(Path(args.generated_report).read_text("utf-8"))
    committed_report.pop("toolchain", None)
    generated_report.pop("toolchain", None)
    compare_value(committed_report, generated_report, "V50_MODEL_LAB",
                  args.tolerance, maximum)
    print(json.dumps({
        "status": "PASS",
        "artifacts": len(ARTIFACTS),
        "exact_identity_and_discrete_fields": True,
        "parameter_hashes_self_verified": True,
        "float_tolerance": args.tolerance,
        "maximum_float_delta": maximum[0],
    }, sort_keys=True))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
