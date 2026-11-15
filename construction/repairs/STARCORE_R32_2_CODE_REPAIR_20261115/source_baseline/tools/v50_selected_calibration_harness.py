#!/usr/bin/env python3
"""Validate the selection-conditioned isotonic artifact and ECE evidence."""
from __future__ import annotations

import hashlib
import struct

from v50_harness_common import (
    close,
    emit,
    java_double,
    java_double_array,
    java_json,
    java_string,
    load_report,
    read_java,
    require,
)


def main() -> int:
    report = load_report()
    calibration = report["selected_calibration"]
    source = read_java("V50SelectedCalibrationArtifact.java")
    artifact_report = java_json(source, "OOS_METRICS_JSON")
    require(artifact_report == calibration, "CALIBRATION_REPORT_ARTIFACT_MISMATCH")
    require(sum(int(fold["test"]) for fold in calibration["folds"])
            == int(calibration["oos_count"]), "CALIBRATION_OOS_COUNT_MISMATCH")
    require(close(java_double(source, "OOS_ECE"), calibration["oos_ece"]),
            "CALIBRATION_ECE_CONSTANT_MISMATCH")
    require(close(java_double(source, "OOS_HIT_RATE"), calibration["oos_rate"]),
            "CALIBRATION_HIT_CONSTANT_MISMATCH")

    x = java_double_array(source, "X")
    y = java_double_array(source, "Y")
    require(len(x) == len(y) and len(x) >= 2, "ISOTONIC_KNOT_SHAPE_INVALID")
    require(all(x[i] <= x[i + 1] for i in range(len(x) - 1)),
            "ISOTONIC_X_NOT_MONOTONE")
    require(all(y[i] <= y[i + 1] for i in range(len(y) - 1)),
            "ISOTONIC_Y_NOT_MONOTONE")
    require(all(0.0 <= value <= 1.0 for value in y),
            "ISOTONIC_Y_OUT_OF_RANGE")
    payload = struct.pack("<%dd" % (len(x) + len(y)), *(x + y))
    require(hashlib.sha256(payload).hexdigest()
            == java_string(source, "PARAMETER_SHA256"),
            "CALIBRATION_PARAMETER_HASH_MISMATCH")

    policy = report["selective_policy"]
    policy_source = read_java("V50SelectivePolicyArtifact.java")
    require(
        "GLOBAL_RANK_SCORE_TIE_THRESHOLD" in policy_source,
        "POLICY_RANK_SCORE_TIE_THRESHOLD_MISSING",
    )
    require(
        all("selected_p_threshold" in fold
            and "rank_score_tie_threshold" in fold
            for fold in policy["folds"]),
        "POLICY_TWO_KEY_FOLD_THRESHOLD_MISSING",
    )
    require(
        all(float(fold["coverage"]) <= 0.30 for fold in policy["folds"]),
        "ISOTONIC_PLATEAU_COVERAGE_EXPLOSION",
    )
    require(
        not bool(policy["model_gate_passed"])
        or float(calibration["oos_ece"])
        <= float(policy["locked_requirements"]["ece_max"]),
        "MODEL_PROMOTED_WITH_UNCALIBRATED_RANK1",
    )
    return emit("v50_selected_calibration", {
        "oos_count": calibration["oos_count"],
        "oos_hit_rate": calibration["oos_rate"],
        "rank1_oos_ece": calibration["oos_ece"],
        "isotonic_knots": len(x),
        "ece_gate_passed": float(calibration["oos_ece"]) <= 0.04,
    })


if __name__ == "__main__":
    raise SystemExit(main())
