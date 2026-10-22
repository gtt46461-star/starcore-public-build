#!/usr/bin/env python3
"""Rebuild independent D04 host fixtures and verify Android output.

Synthetic engineering parity only: no natural draws, certificate or REAL authority.
Uses current Gradle production classes, existing production-capture/online harnesses,
and the repository's independent NumPy trainer. Never accepts Android as its oracle.
"""
import argparse
import hashlib
import json
import os
from pathlib import Path
import shutil
import subprocess
import sys

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT))
SCOPE = "SYNTHETIC_HOST_NUMPY_JAVA_ANDROID_PARITY_NOT_NATURAL_NOT_PHYSICAL_PHONE"


def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def write_json(path, value):
    path.write_text(json.dumps(value, indent=2, allow_nan=False) + "\n")


def vector(path, count):
    import numpy as np
    if path.stat().st_size != count * 8:
        raise AssertionError(f"D04_VECTOR_SIZE:{path.name}:{path.stat().st_size}!={count * 8}")
    values = np.fromfile(path, dtype=">f8")
    if not np.isfinite(values).all():
        raise AssertionError("D04_VECTOR_NONFINITE:" + path.name)
    return values


def compare_vectors(expected, actual, count, tolerance):
    import numpy as np
    delta = float(np.max(np.abs(vector(expected, count) - vector(actual, count))))
    if delta > tolerance:
        raise AssertionError(f"D04_PARITY:{actual.name}:{delta}>{tolerance}")
    return {"values": count, "max_absolute_error": delta, "tolerance": tolerance,
            "host_sha256": digest(expected), "android_sha256": digest(actual)}


def prepare(output):
    import numpy as np
    from training.r929 import deep_candidates as d
    classes = ROOT / "app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes"
    platform = Path(os.environ["ANDROID_HOME"]) / "platforms/android-36/android.jar"
    resource = ROOT / "app/build/intermediates/compile_and_runtime_not_namespaced_r_class_jar/debug/processDebugResources/R.jar"
    if not classes.is_dir() or not platform.is_file() or not resource.is_file():
        raise AssertionError("D04_REQUIRES_CURRENT_NORMAL_GRADLE_COMPILE")
    host = output / "host-classes"
    host.mkdir(parents=True, exist_ok=True)
    names = ("RawUpstreamHarnessR932", "UpstreamParityHarnessR932", "D04OnlineFamilyFairnessHarnessR932")
    sources = sorted((ROOT / "verification/r932/android-json").rglob("*.java"))
    sources += [ROOT / "verification/r960/host/android/os/SystemClock.java"]
    sources += [ROOT / "verification/r932" / (name + ".java") for name in names]
    cp = os.pathsep.join(map(str, (host, classes, resource, platform)))

    def run(argv, name):
        with (output / (name + ".log")).open("w") as log:
            subprocess.run(argv, cwd=ROOT, stdout=log, stderr=subprocess.STDOUT, check=True, timeout=300)

    run(["javac", "-encoding", "UTF-8", "-source", "17", "-target", "17", "-cp", cp,
         "-d", str(host), *map(str, sources)], "host-compile")
    raw_path = output / "raw_upstream.jsonl"
    run(["java", "-cp", cp, "com.ethk3.novaonepro." + names[0], str(raw_path)], "host-capture")
    rows = [json.loads(line) for line in raw_path.read_text().splitlines()]
    if len(rows) != 120 or any(r["max_source_issue"] >= r["target_issue"] for r in rows):
        raise AssertionError("D04_RAW_CAPTURE_COUNT_OR_PIT")
    raw = [json.loads(r["proofs"][0].split("=", 1)[1]) for r in rows[:100]]
    state = np.asarray([r["states"] for r in raw])
    experts = np.asarray([r["expert_logits"] for r in raw])
    features = d.feature_vectors(state, experts)
    slow = d.slow_memory(features)
    labels = (np.arange(100)[:, None] + np.arange(10)[None, :]) % 10
    teacher = d.base_probability(experts)
    # Same scoped fixture contract as scripts/r932_upstream_model_parity.py.
    models = [d.train(kind, features[:24], slow[:24], experts[:24], labels[:24], teacher[:24], epochs=2)
              for kind in ("D0", "D1", "D2", "D3")]
    gate = d.train_gate9(state[:24], experts[:24], labels[:24], epochs=5)
    model_path = output / "upstream_models.json"
    write_json(model_path, {"numbers": [d.serial(m) for m in models], "gate": d.serial(gate),
                            "scope": "SYNTHETIC_PARITY_ONLY"})
    upstream = output / "upstream_host.bin"
    online = output / "online_host.bin"
    run(["java", "-cp", cp, "com.ethk3.novaonepro." + names[1], str(model_path), str(raw_path),
         str(upstream)], "host-upstream")
    run(["java", "-cp", cp, "com.ethk3.novaonepro." + names[2], str(output / "online-fairness.json"),
         str(online)], "host-online")
    actual = vector(upstream, 145800).reshape(100, 1458)
    vector(online, 60)
    errors = {"features": float(np.max(np.abs(actual[:, :1049] - features)))}
    if errors["features"] > 1e-12:
        raise AssertionError("D04_NUMPY_FEATURE_PARITY:" + str(errors))
    for i, model in enumerate(models):
        probability = actual[:, 1049 + 100 * i:1049 + 100 * (i + 1)].reshape(100, 10, 10)
        errors[model["kind"]] = float(np.max(np.abs(probability - d.predict(model, features, slow, experts))))
        if (errors[model["kind"]] > 1e-8 or (probability < 0).any()
                or np.max(np.abs(probability.sum(1) - 1)) > 1e-6
                or np.max(np.abs(probability.sum(2) - 1)) > 1e-6):
            raise AssertionError("D04_NUMPY_NUMBER_PARITY:" + str(errors))
    errors["GATE9"] = float(np.max(np.abs(actual[:, -9:] - d.predict_gate9(gate, state, experts)[1])))
    if errors["GATE9"] > 1e-10:
        raise AssertionError("D04_NUMPY_GATE_PARITY:" + str(errors))
    assets = ROOT / "app/src/androidTest/assets"
    assets.mkdir(parents=True, exist_ok=True)
    for source, name in ((model_path, "d04_upstream_models.json"), (raw_path, "d04_raw_upstream.jsonl")):
        shutil.copyfile(source, assets / name)
    inputs = {p.name: digest(p) for p in (model_path, raw_path, upstream, online)}
    receipt = {"status": "PASS", "scope": SCOPE, "commit": subprocess.check_output(
        ["git", "rev-parse", "HEAD"], cwd=ROOT, text=True).strip(), "numpy_errors": errors,
        "inputs": inputs, "effect_proven": False, "may_authorize_real": False}
    write_json(output / "prepare-receipt.json", receipt)
    print("O09_D04_HOST_PREPARED|" + json.dumps(receipt), flush=True)


def compare(output):
    prepared = json.loads((output / "prepare-receipt.json").read_text())
    if prepared["status"] != "PASS":
        raise AssertionError("D04_HOST_NOT_READY")
    for name, expected in prepared["inputs"].items():
        if digest(output / name) != expected:
            raise AssertionError("D04_HOST_INPUT_CHANGED:" + name)
    metrics = json.loads((output / "d04_android_metrics.json").read_text())
    if (metrics["status"] != "PASS" or metrics["issues"] != 100
            or metrics["upstream_host_device_values"] != 145800 or metrics["online_host_device_values"] != 60
            or metrics["effect_proven"] or metrics["may_authorize_real"]):
        raise AssertionError("D04_ANDROID_CONTRACT:" + str(metrics))
    result = {"status": "PASS", "scope": SCOPE, "commit": prepared["commit"],
              "upstream": compare_vectors(output / "upstream_host.bin", output / "d04_upstream_android.bin", 145800, 1e-8),
              "online": compare_vectors(output / "online_host.bin", output / "d04_online_android.bin", 60, 1e-10),
              "android_metrics": metrics, "numpy_errors": prepared["numpy_errors"],
              "effect_proven": False, "may_authorize_real": False, "overall_1_9": "NOT_CLOSED"}
    write_json(output / "parity-receipt.json", result)
    print("O09_D04_ANDROID_HOST_PARITY|" + json.dumps(result), flush=True)


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("phase", choices=("prepare", "compare"))
    parser.add_argument("--output", required=True, type=Path)
    args = parser.parse_args()
    args.output = args.output.resolve()
    args.output.mkdir(parents=True, exist_ok=True)
    {"prepare": prepare, "compare": compare}[args.phase](args.output)
