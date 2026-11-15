#!/usr/bin/env python3
"""Verify grouped ranking metrics, artifact identity, and fail-closed promotion."""
from __future__ import annotations

from v50_harness_common import (
    close,
    emit,
    java_bool,
    java_double,
    java_json,
    java_string,
    load_report,
    read_java,
    require,
)


def main() -> int:
    report = load_report()
    champion = report["action_champion"]
    metrics = report["action_rankers"][champion]
    folds = metrics["folds"]
    issue_count = sum(int(fold["issues"]) for fold in folds)
    require(issue_count == int(metrics["issues"]),
            "RANK_ISSUE_AGGREGATE_MISMATCH")
    for rank in ("rank1_rate", "rank2_rate", "rank3_rate"):
        weighted = sum(float(fold[rank]) * int(fold["issues"]) for fold in folds)
        weighted /= max(1, issue_count)
        require(close(weighted, metrics[rank]), "RANK_RATE_AGGREGATE_MISMATCH", rank)

    source = read_java("V50ActionRankerArtifact.java")
    require(java_string(source, "ARTIFACT_ID")
            == "V50_ACTION_RANKER_" + champion, "RANK_ARTIFACT_ID_MISMATCH")
    artifact_metrics = java_json(source, "OOS_METRICS_JSON")
    require(artifact_metrics == report["action_rankers"],
            "RANK_ARTIFACT_REPORT_MISMATCH")
    for name, key in (
        ("OOS_RANK1_RATE", "rank1_rate"),
        ("OOS_RANK2_RATE", "rank2_rate"),
        ("OOS_RANK3_RATE", "rank3_rate"),
    ):
        require(close(java_double(source, name), metrics[key]),
                "RANK_ARTIFACT_CONSTANT_MISMATCH", name)

    rank_order = (
        float(metrics["rank1_rate"]) > float(metrics["rank2_rate"])
        and float(metrics["rank1_rate"]) > float(metrics["rank3_rate"])
    )
    require(java_bool(source, "OOS_RANK_ORDER_PASSED") is rank_order,
            "RANK_ORDER_FLAG_MISMATCH")
    require(
        bool(report["selective_policy"]["model_gate_passed"]) is False
        or rank_order,
        "MODEL_PROMOTED_WITH_FAILED_RANK_ORDER",
    )
    return emit("v50_action_rank", {
        "champion": champion,
        "issues": issue_count,
        "rank1_rate": metrics["rank1_rate"],
        "rank2_rate": metrics["rank2_rate"],
        "rank3_rate": metrics["rank3_rate"],
        "rank_order_passed": rank_order,
        "promotion_blocked": not rank_order,
    })


if __name__ == "__main__":
    raise SystemExit(main())

