#!/usr/bin/env python3
"""V49 expanding-time conditional-softmax training and exact Java export.

The evaluation unit is one target issue.  Every fold trains only on earlier targets,
selects at most one position action per target, and reports the 18% coverage slice.
The final artifact is then retrained on all completed historical targets for serving.
"""
from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path

import numpy as np


def softmax(z: np.ndarray) -> np.ndarray:
    z = z - z.max(axis=2, keepdims=True)
    e = np.exp(np.clip(z, -40.0, 40.0))
    return e / e.sum(axis=2, keepdims=True)


def top3_metrics(prob: np.ndarray, y: np.ndarray, score: np.ndarray | None = None,
                 coverage: float = 0.18) -> dict:
    order = np.argsort(-prob, axis=2)
    mass = np.take_along_axis(prob, order[:, :, :3], axis=2).sum(2)
    hit = (order[:, :, :3] == y[:, :, None]).any(axis=2)
    position_score = mass if score is None else score
    pos = np.argmax(position_score, axis=1)
    chosen_hit = hit[np.arange(len(y)), pos]
    confidence = mass[np.arange(len(y)), pos]
    threshold = float(np.quantile(confidence, 1.0 - coverage))
    selected = confidence >= threshold
    selected_hit = chosen_hit[selected]
    n = int(selected.sum())
    wins = int(selected_hit.sum())
    return {
        "periods": int(len(y)),
        "selected": n,
        "coverage": n / max(1, len(y)),
        "hit": wins,
        "rate": wins / max(1, n),
        "all_position_top3": float(hit.mean()),
        "mean_action_p": float(confidence[selected].mean()) if n else 0.0,
        "threshold": threshold,
        "pnl": wins * 6.9 - (n - wins) * 3.0,
    }


def aggregate_folds(folds: list[dict]) -> dict:
    periods = sum(int(x["periods"]) for x in folds)
    selected = sum(int(x["selected"]) for x in folds)
    hit = sum(int(x["hit"]) for x in folds)
    weighted_top3 = sum(float(x["all_position_top3"]) * int(x["periods"]) for x in folds)
    weighted_action = sum(float(x["mean_action_p"]) * int(x["selected"]) for x in folds)
    return {
        "periods": periods,
        "selected": selected,
        "coverage": selected / max(1, periods),
        "hit": hit,
        "rate": hit / max(1, selected),
        "all_position_top3": weighted_top3 / max(1, periods),
        "mean_action_p": weighted_action / max(1, selected),
        "pnl": hit * 6.9 - (selected - hit) * 3.0,
    }


def train(x: np.ndarray, y: np.ndarray, epochs: int = 18, lr: float = 0.025,
          l2: float = 2e-4, seed: int = 7) -> tuple[np.ndarray, ...]:
    rng = np.random.default_rng(seed)
    targets, positions, numbers, features = x.shape
    mean = x.mean((0, 1, 2), dtype=np.float64)
    scale = x.std((0, 1, 2), dtype=np.float64)
    scale = np.where(scale < 1e-5, 1.0, scale)
    z_all = (x - mean) / scale

    weight = np.zeros(features, dtype=np.float64)
    position_number_bias = np.zeros((positions, numbers), dtype=np.float64)
    number_bias = np.zeros(numbers, dtype=np.float64)
    parameters = (weight, position_number_bias, number_bias)
    m = [np.zeros_like(v) for v in parameters]
    v = [np.zeros_like(v) for v in parameters]
    step = 0
    order = np.arange(targets)
    batch_size = 192

    for _ in range(epochs):
        rng.shuffle(order)
        for start in range(0, targets, batch_size):
            idx = order[start:start + batch_size]
            z = (np.einsum("tpnf,f->tpn", z_all[idx], weight)
                 + position_number_bias[None, :, :] + number_bias[None, None, :])
            probability = softmax(z)
            gradient = probability.copy()
            gradient[np.arange(len(idx))[:, None], np.arange(positions)[None, :], y[idx]] -= 1.0
            gradient /= len(idx) * positions
            gradients = (
                np.einsum("tpn,tpnf->f", gradient, z_all[idx]) + l2 * weight,
                gradient.sum(0) + l2 * position_number_bias,
                gradient.sum((0, 1)) + l2 * number_bias,
            )
            step += 1
            for j, (parameter, grad) in enumerate(zip(parameters, gradients)):
                m[j][...] = 0.9 * m[j] + 0.1 * grad
                v[j][...] = 0.999 * v[j] + 0.001 * grad * grad
                m_hat = m[j] / (1.0 - 0.9 ** step)
                v_hat = v[j] / (1.0 - 0.999 ** step)
                parameter -= lr * m_hat / (np.sqrt(v_hat) + 1e-8)
        lr *= 0.94
    return mean, scale, weight, position_number_bias, number_bias


def predict(x: np.ndarray, artifact: tuple[np.ndarray, ...]) -> np.ndarray:
    mean, scale, weight, position_number_bias, number_bias = artifact
    z = (x - mean) / scale
    return softmax(np.einsum("tpnf,f->tpn", z, weight)
                   + position_number_bias[None, :, :] + number_bias[None, None, :])


def java_numbers(x: np.ndarray) -> str:
    return ",".join("%.17g" % float(v) for v in np.asarray(x).ravel())


def java_artifact(path: Path, artifact: tuple[np.ndarray, ...], data_sha: str,
                  metrics: str, contract: str, feature_names: list[str],
                  target_count: int, period_start: str, period_end: str) -> None:
    mean, scale, weight, position_number_bias, number_bias = artifact
    rows = ",\n            ".join("{" + java_numbers(row) + "}" for row in position_number_bias)
    parameter_payload = np.concatenate([
        mean.ravel(), scale.ravel(), weight.ravel(),
        position_number_bias.ravel(), number_bias.ravel(),
    ]).astype(np.float64).tobytes()
    parameter_sha = hashlib.sha256(parameter_payload).hexdigest()
    feature_java = ",".join(json.dumps(str(x), ensure_ascii=False) for x in feature_names)
    escaped_metrics = metrics.replace("\\", "\\\\").replace('"', '\\"')
    text = f'''package com.ethk3.novaonepro;

/** Generated by training/train_compare_export.py; do not hand-edit. */
public final class V49ModelArtifact {{
    private V49ModelArtifact() {{ }}
    public static final String MODEL_ID = "V49_CONDITIONAL_SOFTMAX_24F_EXPORTED";
    public static final String DATA_SHA256 = "{data_sha}";
    public static final String FEATURE_CONTRACT = "{contract}";
    public static final String PARAMETER_SHA256 = "{parameter_sha}";
    public static final String TRAINING_PERIOD_START = "{period_start}";
    public static final String TRAINING_PERIOD_END = "{period_end}";
    public static final int TRAINING_TARGET_COUNT = {target_count};
    public static final String METRICS = "{escaped_metrics}";
    public static final int FEATURE_COUNT = 24;
    public static final String[] FEATURE_NAMES = new String[]{{{feature_java}}};
    public static final double[] MEAN = new double[]{{{java_numbers(mean)}}};
    public static final double[] SCALE = new double[]{{{java_numbers(scale)}}};
    public static final double[] WEIGHT = new double[]{{{java_numbers(weight)}}};
    public static final double[] NUMBER_BIAS = new double[]{{{java_numbers(number_bias)}}};
    public static final double[][] POSITION_NUMBER_BIAS = new double[][]{{
            {rows}}};
}}
'''
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(text, encoding="utf-8")


def scalar_string(value: np.ndarray) -> str:
    return str(value.item() if getattr(value, "shape", None) == () else value)


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("dataset")
    parser.add_argument("--output", default="reports/V49_MODEL_LAB_49072201.json")
    parser.add_argument("--java-output", default="app/src/main/java/com/ethk3/novaonepro/V49ModelArtifact.java")
    parser.add_argument("--epochs", type=int, default=16)
    args = parser.parse_args()

    dataset = np.load(args.dataset, allow_pickle=False)
    x = dataset["X"]
    y = dataset["y"]
    target_count = len(x)
    cuts = [int(target_count * ratio) for ratio in (0.50, 0.65, 0.78, 0.90, 1.0)]
    folds: list[dict] = []
    for fold in range(4):
        train_slice = slice(0, cuts[fold])
        test_slice = slice(cuts[fold], cuts[fold + 1])
        artifact = train(x[train_slice], y[train_slice],
                         epochs=max(8, args.epochs - 4), seed=17 + fold)
        folds.append(top3_metrics(predict(x[test_slice], artifact), y[test_slice], coverage=0.18))

    strict_oos = aggregate_folds(folds)
    final_artifact = train(x, y, epochs=args.epochs, seed=29)
    train_fit = top3_metrics(predict(x, final_artifact), y, coverage=0.18)
    data_sha = scalar_string(dataset["data_sha256"])
    contract = scalar_string(dataset["feature_contract"])
    feature_names = [str(v) for v in dataset["feature_names"].tolist()]
    periods = dataset["period"]
    targets = dataset["target"]
    period_start = str(periods[int(targets[0])]) if len(targets) else ""
    period_end = str(periods[int(targets[-1])]) if len(targets) else ""
    metric_payload = {
        "folds": folds,
        "strict_oos": strict_oos,
        "train_fit_diagnostic": train_fit,
    }
    metrics = json.dumps(metric_payload, ensure_ascii=False, separators=(",", ":"))
    java_output = Path(args.java_output)
    java_artifact(java_output, final_artifact, data_sha, metrics, contract,
                  feature_names, target_count, period_start, period_end)
    report = {
        "strict_point_in_time": True,
        "group_unit": "target_issue",
        "feature_count": 24,
        "data_sha256": data_sha,
        "feature_contract": contract,
        "folds": folds,
        "strict_oos": strict_oos,
        "train_fit_diagnostic": train_fit,
        "training_period_start": period_start,
        "training_period_end": period_end,
        "java_artifact": args.java_output,
        "artifact_sha256": hashlib.sha256(java_output.read_bytes()).hexdigest(),
    }
    output = Path(args.output)
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8")
    print(json.dumps(report, ensure_ascii=False))


if __name__ == "__main__":
    main()

