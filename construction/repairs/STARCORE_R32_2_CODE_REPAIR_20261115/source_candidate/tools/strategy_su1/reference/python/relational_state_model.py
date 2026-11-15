"""SU1 reference: jointly trainable relation matrix and temporal-state predictor.

This is an executable numerical reference, not an Android integration or a claim
of predictive advantage on real draws. NumPy is the only third-party dependency.
Inputs and labels use numbers 1..10; outputs are ten position-wise distributions.
The reference uses WINDOW_RESET state: every prediction replays its causal window
from zero. A production streaming-state model must use a different protocol ID.
"""
from __future__ import annotations

from dataclasses import dataclass
import hashlib
import json
from typing import Iterable
import numpy as np


@dataclass(frozen=True)
class Draw:
    issue_ordinal: int
    available_at_ms: int
    numbers: tuple[int, ...]

    def __post_init__(self) -> None:
        if (not isinstance(self.issue_ordinal, int) or self.issue_ordinal < 0
                or not isinstance(self.available_at_ms, int)
                or self.available_at_ms < 0):
            raise ValueError("DRAW_IDENTITY")
        if len(self.numbers) != 10 or sorted(self.numbers) != list(range(1, 11)):
            raise ValueError("DRAW_NOT_PERMUTATION")


def causal_window(records: Iterable[Draw], target_ordinal: int,
                  cutoff_ms: int, lookback: int) -> tuple[np.ndarray, str]:
    """Select only available past records; future contents cannot enter the hash.

    Ordinals are an adapter-bound consecutive sequence, not lexical issue IDs.
    Production must obtain them from the existing canonical issue resolver.
    """
    if target_ordinal < 1 or cutoff_ms < 0 or not 1 <= lookback <= 256:
        raise ValueError("WINDOW_REQUEST")
    available = [r for r in records
                 if r.issue_ordinal < target_ordinal and r.available_at_ms <= cutoff_ms]
    available.sort(key=lambda r: r.issue_ordinal)
    if any(a.issue_ordinal == b.issue_ordinal for a, b in zip(available, available[1:])):
        raise ValueError("DUPLICATE_SOURCE_ISSUE")
    used = available[-lookback:]
    expected = list(range(target_ordinal - lookback, target_ordinal))
    if len(used) != lookback or [r.issue_ordinal for r in used] != expected:
        raise ValueError("INCOMPLETE_CAUSAL_WINDOW")
    identity = {
        "protocol": "SU1-RELSTATE10-REFERENCE-V1", "targetOrdinal": target_ordinal,
        "cutoffMs": cutoff_ms,
        "used": [[r.issue_ordinal, r.available_at_ms, [int(x) for x in r.numbers]] for r in used],
    }
    wire = json.dumps(identity, sort_keys=True, separators=(",", ":")).encode()
    return np.asarray([r.numbers for r in used], dtype=np.int64), hashlib.sha256(wire).hexdigest()


def _softmax(x: np.ndarray) -> np.ndarray:
    z = x - np.max(x, axis=-1, keepdims=True)
    ex = np.exp(z)
    return ex / ex.sum(axis=-1, keepdims=True)


def _history(history: np.ndarray) -> np.ndarray:
    x = np.asarray(history)
    if x.ndim != 2 or x.shape[1] != 10 or not 1 <= x.shape[0] <= 256:
        raise ValueError("HISTORY_SHAPE")
    if not np.issubdtype(x.dtype, np.integer):
        raise ValueError("HISTORY_INTEGER_REQUIRED")
    if not np.all(np.sort(x, axis=1) == np.arange(1, 11)):
        raise ValueError("HISTORY_NOT_PERMUTATION")
    return x.astype(np.int64, copy=False) - 1


def _label(actual: np.ndarray) -> np.ndarray:
    y = np.asarray(actual)
    if y.shape != (10,) or not np.issubdtype(y.dtype, np.integer):
        raise ValueError("LABEL_SHAPE")
    if not np.array_equal(np.sort(y), np.arange(1, 11)):
        raise ValueError("LABEL_NOT_PERMUTATION")
    return y.astype(np.int64, copy=False) - 1


class RelationalStateModel:
    """A shared number embedding, learned source-position relation and tanh state.

    z[t,p] = E[number[t,p]] + U[p]
    A = softmax(R, source_position)
    message[t,p] = sum_q A[p,q] * z[t,q]
    h[t,p] = tanh(z[t,p] Wx + message[t,p] Wm + h[t-1,p] Wh + b)
    probability[p] = softmax(h[last,p] Wo + c)

    All nine parameter groups receive exact BPTT gradients. A is a predictive
    relation parameter, not a causal discovery certificate. Column probability
    sums are not constrained; this predicts marginals rather than a joint law.
    """
    PROTOCOL = "SU1-RELSTATE10-REFERENCE-V1"
    KEYS = ("E", "U", "R", "Wx", "Wm", "Wh", "b", "Wo", "c")

    def __init__(self, width: int = 8, seed: int = 1):
        if not isinstance(width, int) or not 2 <= width <= 64:
            raise ValueError("MODEL_WIDTH")
        self.width = width
        rng = np.random.default_rng(seed)
        rand = lambda shape, scale: rng.normal(0.0, scale, size=shape).astype(np.float64)
        self.params = {
            "E": rand((10, width), 0.25), "U": rand((10, width), 0.05),
            "R": rand((10, 10), 0.15),
            "Wx": rand((width, width), 1.0 / np.sqrt(width)),
            "Wm": rand((width, width), 1.0 / np.sqrt(width)),
            "Wh": rand((width, width), 0.4 / np.sqrt(width)),
            "b": np.zeros(width), "Wo": rand((width, 10), 0.3),
            "c": np.zeros(10),
        }

    def _forward(self, history: np.ndarray):
        indices = _history(history)
        p = self.params
        if any(not np.isfinite(value).all() for value in p.values()):
            raise ValueError("NONFINITE_PARAMETERS")
        relation = _softmax(p["R"])
        state = np.zeros((10, self.width), dtype=np.float64)
        tape = []
        for row in indices:
            z = p["E"][row] + p["U"]
            message = relation @ z
            previous = state
            state = np.tanh(z @ p["Wx"] + message @ p["Wm"] + previous @ p["Wh"] + p["b"])
            tape.append((row, z, message, previous, state))
        logits = state @ p["Wo"] + p["c"]
        probs = _softmax(logits)
        return probs, logits, state, relation, tape

    def predict(self, history: np.ndarray) -> np.ndarray:
        return self._forward(history)[0].copy()

    def loss_and_grads(self, history: np.ndarray, actual: np.ndarray):
        target = _label(actual)
        probs, logits, last, relation, tape = self._forward(history)
        shifted = logits - np.max(logits, axis=1, keepdims=True)
        loss = np.mean(np.log(np.exp(shifted).sum(axis=1)) - shifted[np.arange(10), target])
        g = {k: np.zeros_like(v) for k, v in self.params.items()}
        delta = probs.copy()
        delta[np.arange(10), target] -= 1.0
        delta /= 10.0  # One mean loss per draw; ten positions are not ten independent periods.
        g["Wo"] = last.T @ delta
        g["c"] = delta.sum(axis=0)
        dh = delta @ self.params["Wo"].T
        g_relation = np.zeros((10, 10))
        for row, z, message, previous, state in reversed(tape):
            da = dh * (1.0 - state * state)
            g["Wx"] += z.T @ da
            g["Wm"] += message.T @ da
            g["Wh"] += previous.T @ da
            g["b"] += da.sum(axis=0)
            gm = da @ self.params["Wm"].T
            g_relation += gm @ z.T
            gz = da @ self.params["Wx"].T + relation.T @ gm
            np.add.at(g["E"], row, gz)
            g["U"] += gz
            dh = da @ self.params["Wh"].T
        g["R"] = relation * (g_relation - (g_relation * relation).sum(axis=1, keepdims=True))
        if not np.isfinite(loss) or any(not np.isfinite(v).all() for v in g.values()):
            raise FloatingPointError("NONFINITE_TRAINING")
        return float(loss), g

    def train_batch(self, examples: list[tuple[np.ndarray, np.ndarray]],
                    learning_rate: float = 0.05, clip_norm: float = 1.0,
                    optimizer: "AdamOptimizer | None" = None):
        if not examples or not np.isfinite(learning_rate) or learning_rate <= 0:
            raise ValueError("TRAIN_BATCH")
        if not np.isfinite(clip_norm) or clip_norm <= 0:
            raise ValueError("CLIP_NORM")
        summed = {k: np.zeros_like(v) for k, v in self.params.items()}
        losses = []
        for history, actual in examples:
            loss, grads = self.loss_and_grads(history, actual)
            losses.append(loss)
            for k in summed:
                summed[k] += grads[k] / len(examples)
        norm = np.sqrt(sum(float(np.sum(v * v)) for v in summed.values()))
        scale = min(1.0, clip_norm / max(norm, 1e-300))
        if optimizer is None:
            for k in self.params:
                self.params[k] -= learning_rate * scale * summed[k]
        else:
            optimizer.apply(self.params, {k: scale * v for k, v in summed.items()}, learning_rate)
        return {"loss": float(np.mean(losses)), "unclippedGradientNorm": float(norm)}

    @staticmethod
    def actions(probs: np.ndarray) -> tuple[int, tuple[int, int, int], float]:
        """One position and three distinct numbers; ties use ascending identity."""
        p = np.asarray(probs, dtype=float)
        if p.shape != (10, 10) or not np.isfinite(p).all() or np.any(p < 0):
            raise ValueError("PROBABILITY_SHAPE")
        if not np.allclose(p.sum(axis=1), 1.0, atol=1e-12, rtol=0):
            raise ValueError("PROBABILITY_SUM")
        numbers = np.argsort(-p, axis=1, kind="stable")[:, :3]
        masses = np.take_along_axis(p, numbers, axis=1).sum(axis=1)
        position = int(np.argmax(masses))
        return position + 1, tuple(int(x) + 1 for x in numbers[position]), float(masses[position])

    def export_bytes(self) -> bytes:
        if any(not np.isfinite(v).all() for v in self.params.values()):
            raise ValueError("NONFINITE_PARAMETERS")
        data = {"protocol": self.PROTOCOL, "stateMode": "WINDOW_RESET",
                "dtype": "float64", "width": self.width,
                "weights": {k: self.params[k].tolist() for k in self.KEYS}}
        return json.dumps(data, sort_keys=True, separators=(",", ":"), allow_nan=False).encode()

    @classmethod
    def from_bytes(cls, wire: bytes, expected_sha256: str | None = None):
        if expected_sha256 is not None and hashlib.sha256(wire).hexdigest() != expected_sha256:
            raise ValueError("MODEL_HASH_MISMATCH")
        data = json.loads(wire)
        if (set(data) != {"protocol", "stateMode", "dtype", "width", "weights"}
                or data["protocol"] != cls.PROTOCOL or data["stateMode"] != "WINDOW_RESET"
                or data["dtype"] != "float64"):
            raise ValueError("MODEL_CONTRACT")
        obj = cls(data["width"])
        if set(data["weights"]) != set(cls.KEYS):
            raise ValueError("MODEL_WEIGHT_KEYS")
        for key in cls.KEYS:
            value = np.asarray(data["weights"][key], dtype=np.float64)
            if value.shape != obj.params[key].shape or not np.isfinite(value).all():
                raise ValueError("MODEL_WEIGHT_SHAPE")
            obj.params[key] = value.copy()
        if obj.export_bytes() != wire:
            raise ValueError("MODEL_NONCANONICAL")
        return obj


class AdamOptimizer:
    """Reference optimizer state; save together with model for exact continuation."""
    def __init__(self, params):
        self.steps = 0
        self.m = {k: np.zeros_like(v) for k, v in params.items()}
        self.v = {k: np.zeros_like(v) for k, v in params.items()}

    def apply(self, params, grads, learning_rate):
        if set(params) != set(self.m) or set(params) != set(grads):
            raise ValueError("OPTIMIZER_KEYS")
        if any(params[k].shape != self.m[k].shape or grads[k].shape != params[k].shape
               for k in params):
            raise ValueError("OPTIMIZER_SHAPE")
        self.steps += 1
        for key in params:
            grad = grads[key]
            self.m[key] = 0.9 * self.m[key] + 0.1 * grad
            self.v[key] = 0.999 * self.v[key] + 0.001 * grad * grad
            params[key] -= learning_rate * (self.m[key] / (1.0 - 0.9 ** self.steps)) / (
                np.sqrt(self.v[key] / (1.0 - 0.999 ** self.steps)) + 1e-8)

    def export_bytes(self):
        data = {"kind": "ADAM_REFERENCE_V1", "steps": self.steps,
                "m": {k: v.tolist() for k, v in self.m.items()},
                "v": {k: v.tolist() for k, v in self.v.items()}}
        return json.dumps(data, sort_keys=True, separators=(",", ":"), allow_nan=False).encode()

    @classmethod
    def from_bytes(cls, wire, params):
        data = json.loads(wire)
        if (set(data) != {"kind", "steps", "m", "v"} or data["kind"] != "ADAM_REFERENCE_V1"
                or type(data["steps"]) is not int or data["steps"] < 0):
            raise ValueError("OPTIMIZER_CONTRACT")
        obj = cls(params)
        obj.steps = data["steps"]
        for group in ("m", "v"):
            if set(data[group]) != set(params):
                raise ValueError("OPTIMIZER_KEYS")
            for key in params:
                arr = np.asarray(data[group][key], dtype=np.float64)
                if arr.shape != params[key].shape or not np.isfinite(arr).all():
                    raise ValueError("OPTIMIZER_SHAPE")
                if group == "v" and np.any(arr < 0):
                    raise ValueError("OPTIMIZER_VARIANCE")
                getattr(obj, group)[key] = arr.copy()
        if obj.export_bytes() != wire:
            raise ValueError("OPTIMIZER_NONCANONICAL")
        return obj


def synthetic_examples(count: int, seed: int = 40):
    """Artificial relational-memory task: label is a position rotation of t-2.

    This creates a known trainable task solely for numerical/capability testing.
    These samples must never enter real forecast evidence tables.
    """
    rng = np.random.default_rng(seed)
    examples = []
    for _ in range(count):
        history = np.stack([rng.permutation(np.arange(1, 11)) for _ in range(3)])
        actual = np.roll(history[-2], 2)
        examples.append((history, actual))
    return examples


if __name__ == "__main__":
    m = RelationalStateModel(width=16, seed=7)
    optimizer = AdamOptimizer(m.params)
    train = synthetic_examples(256, 40)
    validation = synthetic_examples(64, 41)
    before = np.mean([m.loss_and_grads(x, y)[0] for x, y in validation])
    for _ in range(10):
        for at in range(0, len(train), 16):
            m.train_batch(train[at:at + 16], learning_rate=0.01, optimizer=optimizer)
    after = np.mean([m.loss_and_grads(x, y)[0] for x, y in validation])
    print(json.dumps({"domain": "SYNTHETIC_CAPABILITY_ONLY", "validationCeBefore": float(before),
                      "validationCeAfter": float(after), "modelBytes": len(m.export_bytes())}, indent=2))
