#!/usr/bin/env python3
"""Replay one frozen target three times through exported serving artifacts."""
from __future__ import annotations

import hashlib
import importlib.util

import numpy as np

from v50_harness_common import (
    ROOT,
    emit,
    java_bool,
    java_double,
    java_double_array,
    java_double_matrix,
    read_java,
    require,
)


def load_dataset_module():
    path = ROOT / "training" / "build_dataset.py"
    spec = importlib.util.spec_from_file_location("v50_replay_dataset", path)
    require(spec is not None and spec.loader is not None, "DATASET_IMPORT_FAILED")
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


def softmax(score: np.ndarray) -> np.ndarray:
    shifted = score - score.max(axis=1, keepdims=True)
    value = np.exp(np.clip(shifted, -40.0, 40.0))
    return value / value.sum(axis=1, keepdims=True)


def action_features(probability: np.ndarray, feature: np.ndarray) -> tuple[np.ndarray, np.ndarray]:
    order = np.argsort(-probability, axis=1, kind="stable")
    top4 = order[:, :4]
    top_p = np.take_along_axis(probability, top4, axis=1)
    raw = top_p[:, :3].sum(axis=1)
    mean = raw.mean()
    std = max(1.0e-6, raw.std())
    second = np.sort(raw)[-2]
    entropy = -(np.maximum(probability, 1.0e-12)
                * np.log(np.maximum(probability, 1.0e-12))).sum(axis=1) / np.log(10.0)
    out = []
    for position in range(10):
        gathered = feature[position, top4[position, :3]]
        signal = np.clip(
            (1.0 - entropy[position]) * 1.75
            + (raw[position] - 0.30) * 1.8,
            0.0,
            1.0,
        )
        out.append([
            raw[position], top_p[position, 0], top_p[position, 1],
            top_p[position, 2], top_p[position, 3],
            top_p[position, 2] - top_p[position, 3],
            top_p[position, 0] - top_p[position, 1],
            entropy[position], signal, position / 9.0,
            gathered[:, 13].mean(), gathered[:, 14].mean(),
            gathered[:, 12].mean(), gathered[:, 15].mean(),
            feature[position, 0, 22], feature[position, 0, 23],
            raw[position] - mean, raw[position] - second,
            (raw[position] - mean) / std, entropy[position],
        ])
    return np.asarray(out, dtype=np.float64), top4[:, :3] + 1


def replay(dataset, draws: np.ndarray) -> tuple[str, dict]:
    feature = dataset.build_dataset(draws[:320], 96)[-1].astype(np.float64)
    number = read_java("V50NumberModelArtifact.java")
    mean = np.asarray(java_double_array(number, "MEAN"))
    scale = np.asarray(java_double_array(number, "SCALE"))
    weight = np.asarray(java_double_array(number, "WEIGHT"))
    number_bias = np.asarray(java_double_array(number, "NUMBER_BIAS"))
    position_bias = np.asarray(java_double_matrix(number, "POSITION_NUMBER_BIAS"))
    score = np.einsum("pnf,f->pn", (feature - mean) / scale, weight)
    probability = softmax(score + position_bias + number_bias[None, :])

    meta, top3 = action_features(probability, feature)
    rank = read_java("V50ActionRankerArtifact.java")
    rank_score = (
        ((meta - np.asarray(java_double_array(rank, "MEAN")))
         / np.asarray(java_double_array(rank, "SCALE")))
        @ np.asarray(java_double_array(rank, "WEIGHT"))
        + java_double(rank, "BIAS")
    )
    selected_position = int(np.argsort(-rank_score, kind="stable")[0])
    calibration = read_java("V50SelectedCalibrationArtifact.java")
    calibrated = float(np.interp(
        rank_score[selected_position],
        np.asarray(java_double_array(calibration, "X")),
        np.asarray(java_double_array(calibration, "Y")),
    ))
    policy = read_java("V50SelectivePolicyArtifact.java")
    lane = "SHADOW" if not java_bool(policy, "MODEL_GATE_PASSED") else (
        "ELIGIBLE" if calibrated >= java_double(
            policy, "GLOBAL_SELECTED_P_THRESHOLD"
        ) else "SHADOW"
    )
    payload = b"".join([
        np.asarray(feature, dtype="<f8").tobytes(),
        np.asarray(probability, dtype="<f8").tobytes(),
        np.asarray(meta, dtype="<f8").tobytes(),
        np.asarray(rank_score, dtype="<f8").tobytes(),
        np.asarray(top3, dtype="<i4").tobytes(),
        ("%d|%.17g|%s" % (selected_position + 1, calibrated, lane)).encode("utf-8"),
    ])
    return hashlib.sha256(payload).hexdigest(), {
        "selected_position": selected_position + 1,
        "top3": top3[selected_position].tolist(),
        "selected_hit_p": calibrated,
        "lane": lane,
        "number_row_sums": probability.sum(axis=1).tolist(),
    }


def main() -> int:
    dataset = load_dataset_module()
    _, draws, _ = dataset.load(
        ROOT / "research_data" / "ethpk10_draws_20260316_201059.csv"
    )
    results = [replay(dataset, draws) for _ in range(3)]
    hashes = [value[0] for value in results]
    require(len(set(hashes)) == 1, "REPLAY_HASH_MISMATCH", str(hashes))
    require(all(abs(total - 1.0) <= 1.0e-12
                for total in results[0][1]["number_row_sums"]),
            "REPLAY_NUMBER_P_NOT_NORMALIZED")
    return emit("v50_deterministic_replay", {
        "replays": 3,
        "replay_sha256": hashes[0],
        "decision": results[0][1],
        "identical": True,
    })


if __name__ == "__main__":
    raise SystemExit(main())

