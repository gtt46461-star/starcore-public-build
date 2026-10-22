#!/usr/bin/env python3
"""Perform numeric number/action feature ablations as a serving smoke benchmark.

This benchmark intentionally marks itself non-OOS and non-promotional. Strict
promotion evidence remains exclusively in V50_MODEL_LAB.json.
"""
from __future__ import annotations

import importlib.util
import math

import numpy as np

from v50_deterministic_replay import action_features, softmax
from v50_harness_common import (
    ROOT,
    emit,
    java_double,
    java_double_array,
    java_double_matrix,
    java_string_array,
    read_java,
    require,
)


def load_dataset_module():
    path = ROOT / "training" / "build_dataset.py"
    spec = importlib.util.spec_from_file_location("v50_ablation_dataset", path)
    require(spec is not None and spec.loader is not None, "DATASET_IMPORT_FAILED")
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


def number_probability(x: np.ndarray, weight: np.ndarray) -> np.ndarray:
    source = read_java("V50NumberModelArtifact.java")
    mean = np.asarray(java_double_array(source, "MEAN"))
    scale = np.asarray(java_double_array(source, "SCALE"))
    number_bias = np.asarray(java_double_array(source, "NUMBER_BIAS"))
    position_bias = np.asarray(java_double_matrix(source, "POSITION_NUMBER_BIAS"))
    score = np.einsum("tpnf,f->tpn", (x - mean) / scale, weight)
    shifted = score + position_bias[None, :, :] + number_bias[None, None, :]
    shifted -= shifted.max(axis=2, keepdims=True)
    value = np.exp(np.clip(shifted, -40.0, 40.0))
    return value / value.sum(axis=2, keepdims=True)


def number_metrics(probability: np.ndarray, label: np.ndarray) -> tuple[float, float]:
    chosen = probability[
        np.arange(len(label))[:, None],
        np.arange(10)[None, :],
        label,
    ]
    log_loss = float(-np.log(np.maximum(chosen, 1.0e-12)).mean())
    top3 = np.argsort(-probability, axis=2, kind="stable")[:, :, :3]
    hit = float((top3 == label[:, :, None]).any(axis=2).mean())
    return log_loss, hit


def main() -> int:
    dataset = load_dataset_module()
    _, draws, _ = dataset.load(
        ROOT / "research_data" / "ethpk10_draws_20260316_201059.csv"
    )
    x_all = dataset.build_dataset(draws[:800], 96).astype(np.float64)
    x = x_all[-256:]
    label = (draws[800 - 256:800] - 1).astype(np.int16)

    number_source = read_java("V50NumberModelArtifact.java")
    number_names = java_string_array(number_source, "FEATURE_NAMES")
    number_weight = np.asarray(java_double_array(number_source, "WEIGHT"))
    baseline_probability = number_probability(x, number_weight)
    baseline_log_loss, baseline_top3 = number_metrics(
        baseline_probability, label
    )
    number_ablation = []
    for index, name in enumerate(number_names):
        removed = number_weight.copy()
        removed[index] = 0.0
        probability = number_probability(x, removed)
        log_loss, top3 = number_metrics(probability, label)
        number_ablation.append({
            "feature": name,
            "delta_log_loss": log_loss - baseline_log_loss,
            "delta_top3_rate": top3 - baseline_top3,
            "probability_changed": not np.array_equal(
                probability, baseline_probability
            ),
        })

    action_rows = []
    action_hit = []
    for target in range(len(x)):
        meta, top3 = action_features(baseline_probability[target], x[target])
        action_rows.append(meta)
        action_hit.append(
            (top3 == (label[target, :, None] + 1)).any(axis=1).astype(np.int8)
        )
    action_x = np.asarray(action_rows)
    action_y = np.asarray(action_hit)
    rank_source = read_java("V50ActionRankerArtifact.java")
    action_names = java_string_array(rank_source, "FEATURE_NAMES")
    rank_mean = np.asarray(java_double_array(rank_source, "MEAN"))
    rank_scale = np.asarray(java_double_array(rank_source, "SCALE"))
    rank_weight = np.asarray(java_double_array(rank_source, "WEIGHT"))
    rank_bias = java_double(rank_source, "BIAS")
    z = (action_x - rank_mean) / rank_scale
    base_score = np.einsum("tpf,f->tp", z, rank_weight) + rank_bias
    base_position = np.argmax(base_score, axis=1)
    base_hit_rate = float(
        action_y[np.arange(len(action_y)), base_position].mean()
    )
    action_ablation = []
    for index, name in enumerate(action_names):
        removed = rank_weight.copy()
        removed[index] = 0.0
        score = np.einsum("tpf,f->tp", z, removed) + rank_bias
        position = np.argmax(score, axis=1)
        rate = float(action_y[np.arange(len(action_y)), position].mean())
        action_ablation.append({
            "feature": name,
            "rank1_rate_delta": rate - base_hit_rate,
            "selection_change_rate": float((position != base_position).mean()),
        })

    require(all(math.isfinite(row["delta_log_loss"]) for row in number_ablation),
            "NUMBER_ABLATION_NONFINITE")
    require(any(row["probability_changed"] for row in number_ablation),
            "NUMBER_ABLATION_NO_RECOMPUTATION")
    runtime = read_java("V50TrueAblationEngine.java")
    for token in ("fuseWithout(", "new V50PositionActionBuilder().build(",
                  "new V50ActionRankerEngine().rank(",
                  "new V50SelectedActionCalibrationEngine().calibrate("):
        require(token in runtime, "RUNTIME_ABLATION_STAGE_MISSING", token)
    return emit("v50_ablation_benchmark", {
        "sample_targets": len(x),
        "strict_oos": False,
        "promotion_evidence": False,
        "purpose": "numeric serving recomputation smoke test",
        "baseline_number_log_loss": baseline_log_loss,
        "baseline_all_position_top3": baseline_top3,
        "baseline_rank1_rate": base_hit_rate,
        "number_feature_ablations": number_ablation,
        "action_feature_ablations": action_ablation,
    })


if __name__ == "__main__":
    raise SystemExit(main())

