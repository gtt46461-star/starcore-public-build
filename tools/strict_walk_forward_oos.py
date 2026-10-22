#!/usr/bin/env python3
"""Audit the time ordering and locked V50 model-promotion calculation."""
from __future__ import annotations

from v50_harness_common import ROOT, emit, load_report, require


def _ordered_folds(folds: list[dict], train_key: str) -> None:
    require(len(folds) >= 4, "OOS_FOLD_COUNT_LT_4", str(len(folds)))
    previous_test_end = -1
    for index, fold in enumerate(folds):
        train = int(fold[train_key])
        test_start = int(fold["test_start_index"])
        test_end = int(fold["test_end_index_exclusive"])
        require(train == test_start, "TRAIN_TEST_BOUNDARY_GAP", str(index))
        require(0 < train < test_end, "INVALID_FOLD_BOUNDARY", str(fold))
        require(
            str(fold["train_last_issue"]) < str(fold["test_first_issue"])
            <= str(fold["test_last_issue"]),
            "ISSUE_ORDER_VIOLATION",
            str(fold),
        )
        require(
            previous_test_end < 0 or test_start == previous_test_end,
            "NON_CONTIGUOUS_EXPANDING_TEST_BLOCK",
            str(fold),
        )
        previous_test_end = test_end


def main() -> int:
    report = load_report()
    contract = report.get("evaluation_contract", {})
    require(report.get("strict_point_in_time") is True, "PIT_FLAG_FALSE")
    require(contract.get("target_unit") == "target_issue", "TARGET_UNIT_NOT_ISSUE")
    require(contract.get("within_issue_action_group_size") == 10,
            "ACTION_GROUP_NOT_TEN")
    require(contract.get("random_split_used") is False, "RANDOM_SPLIT_FORBIDDEN")
    require(contract.get("future_labels_used_as_features") is False,
            "FUTURE_LABEL_FEATURE_FORBIDDEN")
    require(contract.get("target_block_used_as_feature") is False,
            "TARGET_BLOCK_FEATURE_FORBIDDEN")
    require(contract.get("target_hash_used_as_feature") is False,
            "TARGET_HASH_FEATURE_FORBIDDEN")
    require(contract.get("selection_tie_breaker")
            == "earlier_oos_rank_score_only",
            "POLICY_TIE_BREAKER_NOT_PRIOR_RANK_SCORE")
    require(contract.get("existing_dataset_is_independent_confirmation")
            is False, "REUSED_DATA_MISLABELED_INDEPENDENT")
    require(contract.get("policy_threshold_source")
            == "earlier_calibrated_rank1_actions_only",
            "POLICY_THRESHOLD_NOT_PRIOR_ONLY")

    number_folds = report["number_model"]["strict_oos_folds"]
    _ordered_folds(number_folds, "train_targets")
    for model in report["action_rankers"].values():
        _ordered_folds(model["folds"], "train_groups")

    source = (ROOT / "training" / "v50_train_pipeline.py").read_text("utf-8")
    for required in (
        "x[:train_end]", "y[:train_end]",
        "x[train_end:test_end]", "action_x[:train_end]",
        "action_hit[:train_end]", "raw_score[:train_end]",
        "label[:train_end]", "indices[:cuts[i]]",
    ):
        require(required in source, "EXPANDING_SLICE_MISSING", required)
    for forbidden in ("train_test_split(", "KFold(shuffle=True", "random_split("):
        require(forbidden not in source, "RANDOM_SPLIT_SOURCE_FOUND", forbidden)

    policy = report["selective_policy"]
    locked = policy["locked_requirements"]
    computed_gate = (
        float(policy["coverage"]) >= float(locked["coverage_min"])
        and float(policy["hit_rate"]) >= float(locked["hit_rate_min"])
        and len(policy["four_blocks"]) >= 4
        and all(float(block["rate"]) >= float(locked["each_block_min"])
                for block in policy["four_blocks"])
        and float(policy["ece"]) <= float(locked["ece_max"])
        and float(policy["rank1_rate"]) > float(policy["rank2_rate"])
        and float(policy["rank1_rate"]) > float(policy["rank3_rate"])
        and int(policy["rank_stable_folds"])
        >= int(locked["rank_stable_fold_min"])
        and float(policy["rolling_300"]["latest_rate"])
        >= float(locked["rolling_300_min"])
    )
    require(computed_gate is bool(policy["quantitative_gate_passed"]),
            "QUANTITATIVE_GATE_CALCULATION_MISMATCH")
    independent = policy["independent_confirmation"]
    require(independent["required"] is True,
            "INDEPENDENT_CONFIRMATION_NOT_REQUIRED")
    final_gate = computed_gate and bool(independent["passed"])
    require(final_gate is bool(policy["model_gate_passed"]),
            "MODEL_GATE_CALCULATION_MISMATCH")
    expected_release = (
        "V50_MODEL_GATE_PASSED_REALTIME_NOT_PROVEN"
        if final_gate else "V50_ENGINEERING_COMPLETE_MODEL_NOT_PROVEN"
    )
    require(report["release_state"] == expected_release,
            "REPORT_RELEASE_STATE_MISMATCH", report["release_state"])
    return emit("strict_walk_forward_oos", {
        "number_folds": len(number_folds),
        "action_folds": len(next(iter(report["action_rankers"].values()))["folds"]),
        "quantitative_gate_passed": computed_gate,
        "independent_confirmation_passed": bool(independent["passed"]),
        "model_gate_passed": final_gate,
        "release_state": expected_release,
    })


if __name__ == "__main__":
    raise SystemExit(main())
