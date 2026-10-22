#!/usr/bin/env python3
"""R811 deterministic TinyMLP Research trainer.

The final 20% issue block is sealed and never read by training, model selection or early stopping.
The emitted binary uses the exact ResearchArtifactCodecR811 byte format and remains SHADOW-only.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import math
import struct
from dataclasses import dataclass
from pathlib import Path
from typing import Dict, Iterable, Tuple

import numpy as np

MAGIC = 0x5238313141525431  # R811ART1
SCHEMA_VERSION = 1
INPUT_D, H1, H2, OUTPUT = 96, 64, 32, 100
LEARNING_RATES = (1e-4, 3e-4, 1e-3)
LAMBDA2_VALUES = (0.0, 1e-6, 1e-5, 1e-4)
GRADIENT_CLIP = 5.0
EPS = 1e-15


class XorShift64Star:
    """Bit-for-bit equivalent of ResearchDeterministicRngR811."""

    MASK = (1 << 64) - 1

    def __init__(self, seed: int):
        self.state = (seed & self.MASK) or 0x9E3779B97F4A7C15

    def xorshift(self) -> int:
        x = self.state
        x ^= x >> 12
        x ^= (x << 25) & self.MASK
        x ^= x >> 27
        self.state = x & self.MASK
        return (self.state * 2685821657736338717) & self.MASK

    def uniform(self, count: int) -> np.ndarray:
        return np.asarray(
            [((self.xorshift() >> 11) * (2.0**-53)) for _ in range(count)],
            dtype=np.float64,
        )


def xavier(rng: XorShift64Star, fan_in: int, fan_out: int) -> np.ndarray:
    limit = math.sqrt(6.0 / (fan_in + fan_out))
    return (rng.uniform(fan_in * fan_out) * 2.0 - 1.0) * limit


def initialize(seed: int) -> Dict[str, np.ndarray]:
    rng = XorShift64Star(seed)
    return {
        "W1": xavier(rng, INPUT_D, H1).reshape(H1, INPUT_D),
        "b1": np.zeros(H1, np.float64),
        "W2": xavier(rng, H1, H2).reshape(H2, H1),
        "b2": np.zeros(H2, np.float64),
        "W3": xavier(rng, H2, OUTPUT).reshape(OUTPUT, H2),
        "b3": np.zeros(OUTPUT, np.float64),
    }


def logsumexp(x: np.ndarray, axis: int) -> np.ndarray:
    maximum = np.max(x, axis=axis, keepdims=True)
    return np.squeeze(
        maximum + np.log(np.sum(np.exp(x - maximum), axis=axis, keepdims=True)),
        axis=axis,
    )


def softmax(x: np.ndarray, axis: int) -> np.ndarray:
    maximum = np.max(x, axis=axis, keepdims=True)
    exponent = np.exp(x - maximum)
    return exponent / np.sum(exponent, axis=axis, keepdims=True)


@dataclass
class SinkhornTape:
    row_softmax: Tuple[np.ndarray, ...]
    column_softmax: Tuple[np.ndarray, ...]
    unclipped: np.ndarray


def sinkhorn(
    logits: np.ndarray, iterations: int
) -> Tuple[np.ndarray, np.ndarray, SinkhornTape]:
    raw = logits.reshape(-1, 10, 10)
    log_kernel = np.clip(raw, -80.0, 80.0)
    batch = log_kernel.shape[0]
    v = np.zeros((batch, 10), np.float64)
    row_states, column_states = [], []
    for _ in range(iterations):
        row_input = log_kernel + v[:, None, :]
        row_states.append(softmax(row_input, axis=2))
        u = -logsumexp(row_input, axis=2)
        column_input = log_kernel + u[:, :, None]
        column_states.append(softmax(column_input, axis=1))
        v = -logsumexp(column_input, axis=1)
    log_q = log_kernel + u[:, :, None] + v[:, None, :]
    q = np.exp(np.clip(log_q, -745.0, 50.0))
    return q, log_q, SinkhornTape(tuple(row_states), tuple(column_states), raw)


def sinkhorn_backward(d_log_q: np.ndarray, tape: SinkhornTape) -> np.ndarray:
    d_log_kernel = d_log_q.copy()
    d_u = np.sum(d_log_q, axis=2)
    d_v = np.sum(d_log_q, axis=1)
    for row_probability, column_probability in zip(
        reversed(tape.row_softmax), reversed(tape.column_softmax)
    ):
        d_column_input = -column_probability * d_v[:, None, :]
        d_log_kernel += d_column_input
        d_u += np.sum(d_column_input, axis=2)
        d_row_input = -row_probability * d_u[:, :, None]
        d_log_kernel += d_row_input
        d_v = np.sum(d_row_input, axis=1)
        d_u = np.zeros_like(d_u)
    active = (tape.unclipped >= -80.0) & (tape.unclipped <= 80.0)
    return (d_log_kernel * active).reshape(-1, OUTPUT)


def forward(
    parameters: Dict[str, np.ndarray], x: np.ndarray
) -> Tuple[np.ndarray, Tuple[np.ndarray, ...]]:
    pre1 = x @ parameters["W1"].T + parameters["b1"]
    hidden1 = np.maximum(0.0, pre1)
    pre2 = hidden1 @ parameters["W2"].T + parameters["b2"]
    hidden2 = np.maximum(0.0, pre2)
    logits = hidden2 @ parameters["W3"].T + parameters["b3"]
    return logits, (x, pre1, hidden1, pre2, hidden2)


def loss_and_gradients(
    parameters: Dict[str, np.ndarray],
    x: np.ndarray,
    y: np.ndarray,
    lambda2: float,
    sinkhorn_iterations: int,
) -> Tuple[float, Dict[str, np.ndarray]]:
    logits, cache = forward(parameters, x)
    _, log_q, tape = sinkhorn(logits, sinkhorn_iterations)
    batch = len(x)
    positions = np.arange(10)[None, :]
    selected_log = log_q[np.arange(batch)[:, None], positions, y]
    ce = -float(np.sum(selected_log) / batch)
    regularization = lambda2 * sum(
        float(np.sum(parameters[name] ** 2)) for name in ("W1", "W2", "W3")
    )
    d_log_q = np.zeros_like(log_q)
    d_log_q[np.arange(batch)[:, None], positions, y] = -1.0 / batch
    d_logits = sinkhorn_backward(d_log_q, tape)
    input_x, pre1, hidden1, pre2, hidden2 = cache
    gradients: Dict[str, np.ndarray] = {}
    gradients["W3"] = d_logits.T @ hidden2 + 2.0 * lambda2 * parameters["W3"]
    gradients["b3"] = np.sum(d_logits, axis=0)
    d_hidden2 = d_logits @ parameters["W3"]
    d_pre2 = d_hidden2 * (pre2 > 0.0)
    gradients["W2"] = d_pre2.T @ hidden1 + 2.0 * lambda2 * parameters["W2"]
    gradients["b2"] = np.sum(d_pre2, axis=0)
    d_hidden1 = d_pre2 @ parameters["W2"]
    d_pre1 = d_hidden1 * (pre1 > 0.0)
    gradients["W1"] = d_pre1.T @ input_x + 2.0 * lambda2 * parameters["W1"]
    gradients["b1"] = np.sum(d_pre1, axis=0)
    if not math.isfinite(ce + regularization) or any(
        not np.all(np.isfinite(value)) for value in gradients.values()
    ):
        raise FloatingPointError("R811_NONFINITE_TRAINING_STATE")
    return ce + regularization, gradients


def validation_loss(
    parameters: Dict[str, np.ndarray], x: np.ndarray, y: np.ndarray, iterations: int
) -> float:
    total, count = 0.0, 0
    for start in range(0, len(x), 64):
        logits, _ = forward(parameters, x[start : start + 64])
        _, log_q, _ = sinkhorn(logits, iterations)
        labels = y[start : start + 64]
        positions = np.arange(10)[None, :]
        total -= float(
            np.sum(log_q[np.arange(len(labels))[:, None], positions, labels])
        )
        count += len(labels)
    return total / max(1, count)


def clip_gradients(gradients: Dict[str, np.ndarray]) -> Tuple[float, float]:
    norm = math.sqrt(sum(float(np.sum(value * value)) for value in gradients.values()))
    scale = min(1.0, GRADIENT_CLIP / max(norm, 1e-300))
    if scale < 1.0:
        for name in gradients:
            gradients[name] *= scale
    return norm, scale


class Adam:
    def __init__(self, parameters: Dict[str, np.ndarray]):
        self.m = {name: np.zeros_like(value) for name, value in parameters.items()}
        self.v = {name: np.zeros_like(value) for name, value in parameters.items()}
        self.step = 0

    def update(
        self,
        parameters: Dict[str, np.ndarray],
        gradients: Dict[str, np.ndarray],
        learning_rate: float,
    ) -> None:
        self.step += 1
        for name in parameters:
            gradient = gradients[name]
            self.m[name] = 0.9 * self.m[name] + 0.1 * gradient
            self.v[name] = 0.999 * self.v[name] + 0.001 * gradient * gradient
            m_hat = self.m[name] / (1.0 - 0.9**self.step)
            v_hat = self.v[name] / (1.0 - 0.999**self.step)
            parameters[name] -= learning_rate * m_hat / (np.sqrt(v_hat) + 1e-8)


@dataclass
class Trial:
    parameters: Dict[str, np.ndarray]
    learning_rate: float
    lambda2: float
    best_epoch: int
    validation_loss: float
    gradient_norm: float
    gradient_scale: float


def train_trial(
    x_train: np.ndarray,
    y_train: np.ndarray,
    x_validation: np.ndarray,
    y_validation: np.ndarray,
    seed: int,
    learning_rate: float,
    lambda2: float,
    epochs: int,
    batch_size: int,
    patience: int,
    sinkhorn_iterations: int,
) -> Trial:
    parameters = initialize(seed)
    optimizer = Adam(parameters)
    best = {name: value.copy() for name, value in parameters.items()}
    best_loss, best_epoch, stale = math.inf, 0, 0
    last_norm, last_scale = 0.0, 1.0
    for epoch in range(1, epochs + 1):
        # Chronological mini-batches; no random row or issue shuffle.
        for start in range(0, len(x_train), batch_size):
            _, gradients = loss_and_gradients(
                parameters,
                x_train[start : start + batch_size],
                y_train[start : start + batch_size],
                lambda2,
                sinkhorn_iterations,
            )
            last_norm, last_scale = clip_gradients(gradients)
            optimizer.update(parameters, gradients, learning_rate)
        current = validation_loss(
            parameters, x_validation, y_validation, sinkhorn_iterations * 2
        )
        if current + 1e-10 < best_loss:
            best_loss, best_epoch, stale = current, epoch, 0
            best = {name: value.copy() for name, value in parameters.items()}
        else:
            stale += 1
            if stale >= patience:
                break
    return Trial(
        best,
        learning_rate,
        lambda2,
        best_epoch,
        best_loss,
        last_norm,
        last_scale,
    )


def refit(
    x: np.ndarray,
    y: np.ndarray,
    seed: int,
    trial: Trial,
    batch_size: int,
    sinkhorn_iterations: int,
) -> Dict[str, np.ndarray]:
    parameters = initialize(seed)
    optimizer = Adam(parameters)
    for _ in range(trial.best_epoch):
        for start in range(0, len(x), batch_size):
            _, gradients = loss_and_gradients(
                parameters,
                x[start : start + batch_size],
                y[start : start + batch_size],
                trial.lambda2,
                sinkhorn_iterations,
            )
            clip_gradients(gradients)
            optimizer.update(parameters, gradients, trial.learning_rate)
    return parameters


def write_utf(output: bytearray, value: str) -> None:
    encoded = value.encode("utf-8")
    if len(encoded) > 65535:
        raise ValueError("R811_ARTIFACT_TEXT_SIZE")
    output.extend(struct.pack(">i", len(encoded)))
    output.extend(encoded)


def encode_artifact(
    artifact_type: str, trained_until_issue: str, fields: Dict[str, np.ndarray]
) -> bytes:
    output = bytearray(struct.pack(">Qi", MAGIC, SCHEMA_VERSION))
    write_utf(output, artifact_type)
    write_utf(output, trained_until_issue)
    output.extend(struct.pack(">i", len(fields)))
    for name in sorted(fields):
        write_utf(output, name)
        values = np.asarray(fields[name], np.float64).reshape(-1)
        output.extend(struct.pack(">i", len(values)))
        for value in values:
            if not math.isfinite(float(value)):
                raise FloatingPointError("R811_ARTIFACT_NONFINITE")
            output.extend(struct.pack(">d", float(value)))
    return bytes(output)


def load_dataset(path: Path) -> Tuple[np.ndarray, np.ndarray, np.ndarray, str]:
    raw = path.read_bytes()
    with np.load(path, allow_pickle=False) as bundle:
        x_key = "x" if "x" in bundle else "X"
        y_key = "y"
        issue_key = "issues" if "issues" in bundle else "period"
        if x_key not in bundle or y_key not in bundle or issue_key not in bundle:
            raise ValueError("R811_DATASET_KEYS_REQUIRE_X_Y_ISSUES")
        x = np.asarray(bundle[x_key], np.float64)
        y = np.asarray(bundle[y_key], np.int64)
        issues = np.asarray(bundle[issue_key]).astype(str)
    if x.ndim != 2 or x.shape[1] != INPUT_D:
        raise ValueError(f"R811_DATASET_X_SHAPE_EXPECT_Nx{INPUT_D}:{x.shape}")
    if y.shape != (len(x), 10) or len(issues) != len(x):
        raise ValueError("R811_DATASET_Y_ISSUE_SHAPE")
    if np.min(y) == 1 and np.max(y) == 10:
        y = y - 1
    if np.min(y) < 0 or np.max(y) > 9:
        raise ValueError("R811_DATASET_Y_RANGE")
    if any(len(set(row.tolist())) != 10 for row in y):
        raise ValueError("R811_DATASET_Y_NOT_PERMUTATION")
    order = np.argsort(issues, kind="stable")
    if not np.array_equal(order, np.arange(len(issues))):
        raise ValueError("R811_DATASET_MUST_ALREADY_BE_TIME_ORDERED")
    if len(np.unique(issues)) != len(issues):
        raise ValueError("R811_DATASET_ONE_ROW_PER_ISSUE_REQUIRED")
    if not np.all(np.isfinite(x)):
        raise ValueError("R811_DATASET_NONFINITE")
    return x, y, issues, hashlib.sha256(raw).hexdigest()


def train(args: argparse.Namespace) -> dict:
    x, y, issues, dataset_hash = load_dataset(Path(args.dataset))
    if len(x) < args.minimum_issues:
        raise ValueError(
            f"R811_STRICT_OOS_MINIMUM:{len(x)}<{args.minimum_issues}"
        )
    frozen_start = int(math.floor(len(x) * 0.80))
    validation_start = int(math.floor(frozen_start * 0.80))
    if validation_start < 1 or frozen_start <= validation_start:
        raise ValueError("R811_TIME_SPLIT_TOO_SMALL")
    mean = np.mean(x[:validation_start], axis=0)
    scale = np.std(x[:validation_start], axis=0)
    scale = np.where(scale < 1e-9, 1.0, scale)
    # Only the train+validation prefix is transformed. The sealed final feature/outcome block is
    # not evaluated, normalized, ranked or inspected by this trainer.
    normalized_train_validation = (x[:frozen_start] - mean) / scale
    x_train, y_train = (
        normalized_train_validation[:validation_start],
        y[:validation_start],
    )
    x_validation, y_validation = (
        normalized_train_validation[validation_start:frozen_start],
        y[validation_start:frozen_start],
    )
    # x[frozen_start:] and y[frozen_start:] are intentionally never read below.
    trials = []
    for lr_index, learning_rate in enumerate(LEARNING_RATES):
        for lambda_index, lambda2 in enumerate(LAMBDA2_VALUES):
            # Identical initialization isolates the learning-rate/L2 comparison.
            trial_seed = int(args.seed) & ((1 << 64) - 1)
            trials.append(
                train_trial(
                    x_train,
                    y_train,
                    x_validation,
                    y_validation,
                    trial_seed,
                    learning_rate,
                    lambda2,
                    args.epochs,
                    args.batch_size,
                    args.patience,
                    args.sinkhorn_iterations,
                )
            )
    winner = min(
        trials,
        key=lambda item: (
            item.validation_loss,
            item.learning_rate,
            item.lambda2,
            item.best_epoch,
        ),
    )
    refit_seed = int(args.seed) & ((1 << 64) - 1)
    parameters = refit(
        normalized_train_validation,
        y[:frozen_start],
        refit_seed,
        winner,
        args.batch_size,
        args.sinkhorn_iterations,
    )
    trained_until = str(issues[frozen_start - 1])
    fields = {name: value.reshape(-1) for name, value in parameters.items()}
    fields["feature_mean"] = mean
    fields["feature_scale"] = scale
    fields["training_meta"] = np.asarray(
        [
            float(args.seed),
            winner.learning_rate,
            winner.lambda2,
            float(winner.best_epoch),
            float(validation_start),
            float(frozen_start - validation_start),
            float(len(x) - frozen_start),
            winner.validation_loss,
            winner.gradient_norm,
            winner.gradient_scale,
            float(args.sinkhorn_iterations),
        ],
        np.float64,
    )
    artifact = encode_artifact("TINY_MLP_96_64_32_100", trained_until, fields)
    artifact_hash = hashlib.sha256(artifact).hexdigest()
    artifact_path = Path(args.output)
    artifact_path.parent.mkdir(parents=True, exist_ok=True)
    artifact_path.write_bytes(artifact)
    manifest = {
        "schema": "R811_TINY_MLP_TRAINING_MANIFEST_V1",
        "status": "RESEARCH_ONLY_UNPROVEN",
        "production_authority": False,
        "model_family": "TINY_MLP_96_64_32_100_SINKHORN",
        "dataset_sha256": dataset_hash,
        "artifact_sha256": artifact_hash,
        "trained_until_issue": trained_until,
        "candidate_freeze_issue": trained_until,
        "train_issues": validation_start,
        "validation_issues": frozen_start - validation_start,
        "sealed_final_issues": len(x) - frozen_start,
        "sealed_final_start_issue": str(issues[frozen_start]),
        "sealed_final_consumed": False,
        "seed": int(args.seed),
        "learning_rate": winner.learning_rate,
        "lambda2": winner.lambda2,
        "best_epoch": winner.best_epoch,
        "validation_loss": winner.validation_loss,
        "gradient_clip": GRADIENT_CLIP,
        "adam": {"beta1": 0.9, "beta2": 0.999, "epsilon": 1e-8},
        "sinkhorn_iterations": args.sinkhorn_iterations,
        "no_shuffle": True,
        "early_stopping_source": "VALIDATION_ONLY",
        "model_proof_45": "NOT_PROVEN_REQUIRES_FUTURE_LIVE_OOS",
    }
    manifest_path = artifact_path.with_suffix(artifact_path.suffix + ".manifest.json")
    manifest_path.write_text(
        json.dumps(manifest, ensure_ascii=False, indent=2, sort_keys=True) + "\n",
        encoding="utf-8",
    )
    return {**manifest, "artifact": str(artifact_path), "manifest": str(manifest_path)}


def self_test() -> dict:
    seed = 811
    first, second = initialize(seed), initialize(seed)
    deterministic = all(np.array_equal(first[name], second[name]) for name in first)
    x = np.linspace(-1.0, 1.0, INPUT_D, dtype=np.float64)[None, :]
    y = np.arange(10, dtype=np.int64)[None, :]
    loss, gradients = loss_and_gradients(first, x, y, 1e-5, 8)
    gradient_index = int(np.argmax(np.abs(gradients["b3"])))
    original = float(first["b3"][gradient_index])
    delta = 1e-6
    first["b3"][gradient_index] = original + delta
    plus, _ = loss_and_gradients(first, x, y, 1e-5, 8)
    first["b3"][gradient_index] = original - delta
    minus, _ = loss_and_gradients(first, x, y, 1e-5, 8)
    first["b3"][gradient_index] = original
    numeric_gradient = (plus - minus) / (2.0 * delta)
    analytic_gradient = float(gradients["b3"][gradient_index])
    gradient_relative_error = abs(numeric_gradient - analytic_gradient) / max(
        1.0, abs(numeric_gradient), abs(analytic_gradient)
    )
    logits, _ = forward(first, x)
    q, _, _ = sinkhorn(logits, 32)
    sinkhorn_row_error = float(np.max(np.abs(np.sum(q, axis=2) - 1.0)))
    sinkhorn_column_error = float(np.max(np.abs(np.sum(q, axis=1) - 1.0)))
    norm, scale = clip_gradients(gradients)
    fields = {name: value.reshape(-1) for name, value in first.items()}
    fields["feature_mean"] = np.zeros(INPUT_D)
    fields["feature_scale"] = np.ones(INPUT_D)
    artifact_a = encode_artifact("TINY_MLP_96_64_32_100", "202608170001", fields)
    artifact_b = encode_artifact("TINY_MLP_96_64_32_100", "202608170001", fields)
    result = {
        "deterministic_init": deterministic,
        "finite_loss": math.isfinite(loss),
        "finite_gradients": all(np.all(np.isfinite(value)) for value in gradients.values()),
        "gradient_relative_error": gradient_relative_error,
        "gradient_check_pass": gradient_relative_error < 5e-5,
        "sinkhorn_row_error": sinkhorn_row_error,
        "sinkhorn_column_error": sinkhorn_column_error,
        "sinkhorn_check_pass": max(sinkhorn_row_error, sinkhorn_column_error) < 1e-8,
        "gradient_norm": norm,
        "gradient_scale": scale,
        "artifact_deterministic": artifact_a == artifact_b,
        "artifact_sha256": hashlib.sha256(artifact_a).hexdigest(),
    }
    if not all(
        result[key]
        for key in (
            "deterministic_init",
            "finite_loss",
            "finite_gradients",
            "gradient_check_pass",
            "sinkhorn_check_pass",
            "artifact_deterministic",
        )
    ):
        raise AssertionError(f"R811_SELF_TEST_FAILED:{result}")
    return result


def parser() -> argparse.ArgumentParser:
    output = argparse.ArgumentParser(description=__doc__)
    output.add_argument("dataset", nargs="?", help="NPZ with x[N,96], y[N,10], issues[N]")
    output.add_argument("--output", default="dist/r811_tiny_mlp.artifact")
    output.add_argument("--seed", type=int, default=811)
    output.add_argument("--epochs", type=int, default=60)
    output.add_argument("--batch-size", type=int, default=16)
    output.add_argument("--patience", type=int, default=10)
    output.add_argument("--sinkhorn-iterations", type=int, default=16)
    output.add_argument("--minimum-issues", type=int, default=120)
    output.add_argument("--self-test", action="store_true")
    return output


def main() -> None:
    args = parser().parse_args()
    if args.self_test:
        result = self_test()
    else:
        if not args.dataset:
            raise SystemExit("dataset is required unless --self-test is used")
        if args.batch_size <= 0 or args.epochs <= 0 or args.patience <= 0:
            raise SystemExit("positive epochs/batch-size/patience required")
        result = train(args)
    print(json.dumps(result, ensure_ascii=False, sort_keys=True))


if __name__ == "__main__":
    main()

