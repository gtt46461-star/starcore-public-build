#!/usr/bin/env python3
"""Check Python-training and Java-serving feature identities and order."""
from __future__ import annotations

import ast
import hashlib
import importlib.util

import numpy as np

from v50_harness_common import (
    ROOT,
    emit,
    java_int,
    java_string,
    java_string_array,
    load_report,
    read_java,
    require,
)


def assignment(path, name: str):
    tree = ast.parse(path.read_text("utf-8"))
    for node in tree.body:
        if isinstance(node, (ast.Assign, ast.AnnAssign)):
            targets = node.targets if isinstance(node, ast.Assign) else [node.target]
            if any(isinstance(target, ast.Name) and target.id == name
                   for target in targets):
                return ast.literal_eval(node.value)
    raise AssertionError("PYTHON_ASSIGNMENT_MISSING: " + name)


def load_dataset_module():
    path = ROOT / "training" / "build_dataset.py"
    spec = importlib.util.spec_from_file_location("v50_parity_dataset", path)
    require(spec is not None and spec.loader is not None, "DATASET_IMPORT_FAILED")
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


def main() -> int:
    pipeline = ROOT / "training" / "v50_train_pipeline.py"
    dataset_path = ROOT / "training" / "build_dataset.py"
    number_features = tuple(assignment(pipeline, "NUMBER_FEATURES"))
    action_features = tuple(assignment(pipeline, "ACTION_FEATURES"))
    dataset_features = tuple(assignment(dataset_path, "FEATURE_NAMES"))
    require(number_features == dataset_features,
            "PYTHON_DATASET_TRAINING_FEATURE_ORDER_MISMATCH")

    number_java = read_java("V50NumberModelArtifact.java")
    rank_java = read_java("V50ActionRankerArtifact.java")
    require(java_string_array(number_java, "FEATURE_NAMES")
            == list(number_features), "JAVA_NUMBER_FEATURE_ORDER_MISMATCH")
    require(java_string_array(rank_java, "FEATURE_NAMES")
            == list(action_features), "JAVA_ACTION_FEATURE_ORDER_MISMATCH")
    require(java_int(number_java, "FEATURE_COUNT") == len(number_features),
            "JAVA_NUMBER_FEATURE_COUNT_MISMATCH")
    require(java_int(rank_java, "FEATURE_COUNT") == len(action_features),
            "JAVA_ACTION_FEATURE_COUNT_MISMATCH")

    feature_contract = hashlib.sha256(
        ("V50|" + ",".join(number_features)).encode("utf-8")
    ).hexdigest()
    require(java_string(number_java, "FEATURE_CONTRACT") == feature_contract,
            "NUMBER_FEATURE_CONTRACT_HASH_MISMATCH")

    feature_source = read_java("V50NumberFeatureEngine.java")
    for snippet in (
        "(position - 1) / 9.0d", "(number - 1) / 9.0d",
        "frequency[0][n]", "frequency[8][n]",
        "Math.min(60, gap) / 60.0d", "gapEvidence.percentile",
        "slope, acceleration, volatility, entropy, concentration",
        "lastDistance, neighbor, parity, big",
        "estimateSwitchRisk(frequency, entropy)",
        "estimateFamilyStrength(frequency, entropy, drift)",
    ):
        require(snippet in feature_source, "JAVA_NUMBER_FORMULA_MISSING", snippet)
    for window in ("6,12,24,48,96,180,360,720,1440,2880",):
        require(window in feature_source, "JAVA_WINDOW_CONTRACT_MISMATCH")

    action_source = read_java("V50ActionMetaFeatureEngine.java")
    for snippet in (
        "raw[p], top1, top2, third, fourth",
        "third - fourth, top1 - top2, entropy, signal",
        "p / 9.0d, slope, acceleration, gapPercentile",
        "volatility, switchRisk, familyStrength",
        "raw[p] - mean, raw[p] - second",
        "(raw[p] - mean) / std, entropy",
    ):
        require(snippet in action_source, "JAVA_ACTION_FORMULA_MISSING", snippet)

    report = load_report()
    data_hashes = {
        java_string(read_java(name), "DATA_SHA256")
        for name in (
            "V50NumberModelArtifact.java", "V50ActionRankerArtifact.java",
            "V50SelectedCalibrationArtifact.java", "V50SelectivePolicyArtifact.java",
        )
    }
    require(data_hashes == {report["data_sha256"]},
            "ARTIFACT_DATA_HASH_MISMATCH", str(data_hashes))

    dataset = load_dataset_module()
    _, draws, _ = dataset.load(
        ROOT / "research_data" / "ethpk10_draws_20260316_201059.csv"
    )
    sample = dataset.build_dataset(draws[:132], 96)
    require(sample.shape == (36, 10, 10, 24), "SAMPLE_FEATURE_SHAPE", str(sample.shape))
    require(bool(np.isfinite(sample).all()), "SAMPLE_FEATURE_NONFINITE")
    return emit("v50_feature_parity", {
        "number_features": len(number_features),
        "action_features": len(action_features),
        "feature_contract_sha256": feature_contract,
        "data_sha256": report["data_sha256"],
        "sample_shape": list(sample.shape),
        "formula_order_checked": True,
    })


if __name__ == "__main__":
    raise SystemExit(main())

