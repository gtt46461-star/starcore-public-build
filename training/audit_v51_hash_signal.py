#!/usr/bin/env python3
"""V51 APEX hash-signal audit.

Purpose:
1. prove that target-issue hash is forbidden at prediction time;
2. quantify whether *historical* hashes add stable chronological OOS lift;
3. keep hash-derived number weight at zero unless FDR and multi-fold gates pass.

The statistical unit is target_issue. All ten positions of an issue stay in the
same chronological fold. No target-issue hash enters any production-eligible
feature row.
"""
from __future__ import annotations

import argparse
import csv
import hashlib
import json
import math
import time
from pathlib import Path

import numpy as np
from scipy.stats import chisquare, chi2_contingency
from sklearn.ensemble import ExtraTreesClassifier

ROOT = Path(__file__).resolve().parents[1]
WINDOWS = (4, 8, 12, 24, 48, 96)
HASH_LAGS = (1, 2, 3, 4)
START = 96


def load(path: Path):
    periods, times, blocks, hashes, draws = [], [], [], [], []
    with path.open(encoding="utf-8-sig", newline="") as handle:
        for row in csv.DictReader(handle):
            if str(row.get("valid", "1")) != "1":
                continue
            numbers = [int(x) for x in str(row.get("open_number", "")).split(",") if str(x).strip()]
            raw_hash = str(row.get("hash", "")).lower()
            if len(numbers) != 10 or sorted(numbers) != list(range(1, 11)):
                continue
            if raw_hash.startswith("0x"):
                raw_hash = raw_hash[2:]
            if len(raw_hash) != 64 or any(c not in "0123456789abcdef" for c in raw_hash):
                continue
            periods.append(str(row.get("period", "")))
            times.append(int(row.get("open_time_utc_ms") or 0))
            blocks.append(int(row.get("block") or 0))
            hashes.append([int(c, 16) for c in raw_hash])
            draws.append(numbers)
    order = np.argsort(np.asarray(periods, dtype=str), kind="stable")
    return (
        np.asarray(periods, dtype=str)[order],
        np.asarray(times, dtype=np.int64)[order],
        np.asarray(blocks, dtype=np.int64)[order],
        np.asarray(hashes, dtype=np.int8)[order],
        np.asarray(draws, dtype=np.int8)[order] - 1,
    )


def bh_qvalues(pvalues):
    p = np.asarray(pvalues, dtype=float)
    order = np.argsort(p)
    ranked = p[order]
    q = np.empty(len(p), dtype=float)
    running = 1.0
    for index in range(len(p) - 1, -1, -1):
        running = min(running, ranked[index] * len(p) / float(index + 1))
        q[order[index]] = min(1.0, running)
    return q


def permutation_global(previous, current):
    previous_position = np.empty(10, dtype=np.int8)
    current_position = np.empty(10, dtype=np.int8)
    for position, number in enumerate(previous):
        previous_position[number] = position
    for position, number in enumerate(current):
        current_position[number] = position
    displacement = np.abs(current_position.astype(int) - previous_position.astype(int))
    inversions = 0
    for left in range(10):
        for right in range(left + 1, 10):
            a, b = int(previous[left]), int(previous[right])
            if (int(current_position[a]) - int(current_position[b])) * (left - right) < 0:
                inversions += 1
    fixed = int((displacement == 0).sum())
    preserved = 0
    for index in range(9):
        a, b = int(previous[index]), int(previous[index + 1])
        if abs(int(current_position[a]) - int(current_position[b])) == 1 and current_position[a] < current_position[b]:
            preserved += 1
    crossed = 0
    for number in range(10):
        before = 0 if previous_position[number] < 3 else (1 if previous_position[number] < 7 else 2)
        after = 0 if current_position[number] < 3 else (1 if current_position[number] < 7 else 2)
        crossed += int(before != after)
    return np.asarray([
        1.0 - 4.0 * inversions / 90.0,
        displacement.mean() / 9.0,
        displacement.max() / 9.0,
        fixed / 10.0,
        preserved / 9.0,
        crossed / 10.0,
    ], dtype=np.float32)


def schedule_features(target, times, blocks):
    minute = int((times[target] // 60000) % 1440)
    day = int((times[target] // 86400000 + 4) % 7)
    previous_block = int(blocks[target - 1])
    cadence = max(0, min(16, int(blocks[target - 1] - blocks[target - 2])))
    return np.asarray([
        math.sin(2.0 * math.pi * minute / 1440.0),
        math.cos(2.0 * math.pi * minute / 1440.0),
        math.sin(2.0 * math.pi * day / 7.0),
        math.cos(2.0 * math.pi * day / 7.0),
        (previous_block % 3) / 2.0,
        (previous_block % 5) / 4.0,
        (previous_block % 7) / 6.0,
        (previous_block % 16) / 15.0,
        (previous_block % 32) / 31.0,
        cadence / 16.0,
    ], dtype=np.float32)


def hash_features(target, hashes):
    values = []
    for lag in HASH_LAGS:
        h = hashes[target - lag].astype(np.float32)
        values.extend((np.bincount(h.astype(np.int16), minlength=16) / 64.0).tolist())
        values.extend((h[:4] / 15.0).tolist())
        values.extend((h[-4:] / 15.0).tolist())
        values.extend([
            float(h.mean() / 15.0),
            float(h.std() / 8.0),
            float(h.min() / 15.0),
            float(h.max() / 15.0),
        ])
    xor = np.bitwise_xor(hashes[target - 1], hashes[target - 2])
    values.extend((np.bincount(xor.astype(np.int16), minlength=16) / 64.0).tolist())
    return np.asarray(values, dtype=np.float32)


def build_rows(draws, hashes, times, blocks):
    total = len(draws) - START
    issue_history, issue_hash, labels = [], [], []
    last_seen = np.full((10, 10), -1, dtype=np.int32)
    for target in range(START):
        for position, number in enumerate(draws[target]):
            last_seen[position, number] = target
    for target in range(START, len(draws)):
        schedule = schedule_features(target, times, blocks)
        global_state = permutation_global(draws[target - 2], draws[target - 1])
        hfeature = hash_features(target, hashes)
        rows_history, rows_hash, row_label = [], [], []
        for position in range(10):
            position_one_hot = np.zeros(10, dtype=np.float32)
            position_one_hot[position] = 1.0
            history = []
            for window in WINDOWS:
                lower = max(0, target - window)
                counts = np.bincount(draws[lower:target, position], minlength=10).astype(np.float32)
                history.extend(((counts + 0.5) / (counts.sum() + 5.0)).tolist())
            for number in range(10):
                gap = target if last_seen[position, number] < 0 else target - 1 - int(last_seen[position, number])
                history.append(min(96, gap) / 96.0)
            history.extend((draws[target - 4:target, position] / 9.0).tolist())
            history_vector = np.concatenate([
                position_one_hot,
                schedule,
                global_state,
                np.asarray(history, dtype=np.float32),
            ])
            hash_vector = np.concatenate([position_one_hot, schedule, hfeature])
            rows_history.append(history_vector)
            rows_hash.append(hash_vector)
            row_label.append(int(draws[target, position]))
        issue_history.append(rows_history)
        issue_hash.append(rows_hash)
        labels.append(row_label)
        for position, number in enumerate(draws[target]):
            last_seen[position, number] = target
    return (
        np.asarray(issue_history, dtype=np.float32),
        np.asarray(issue_hash, dtype=np.float32),
        np.asarray(labels, dtype=np.int8),
    )


def fit_predict(train_x, train_y, test_x, seed):
    model = ExtraTreesClassifier(
        n_estimators=56,
        max_depth=15,
        min_samples_leaf=18,
        max_features="sqrt",
        n_jobs=2,
        random_state=seed,
    )
    model.fit(train_x.reshape(-1, train_x.shape[-1]), train_y.reshape(-1))
    probability = model.predict_proba(test_x.reshape(-1, test_x.shape[-1]))
    full = np.zeros((len(probability), 10), dtype=float)
    for index, value in enumerate(model.classes_):
        full[:, int(value)] = probability[:, index]
    return full.reshape(len(test_x), 10, 10)


def metrics(probability, label):
    order = np.argsort(-probability, axis=2, kind="stable")
    hit = (order[:, :, :3] == label[:, :, None]).any(2)
    mass = np.take_along_axis(probability, order[:, :, :3], axis=2).sum(2)
    position_order = np.argsort(-mass, axis=1, kind="stable")
    rows = np.arange(len(label))
    return {
        "issues": int(len(label)),
        "all_position_top3": float(hit.mean()),
        "rank1": float(hit[rows, position_order[:, 0]].mean()),
        "rank2": float(hit[rows, position_order[:, 1]].mean()),
        "rank3": float(hit[rows, position_order[:, 2]].mean()),
    }


def fdr_audit(hashes, draws):
    tests = []
    for lag in HASH_LAGS:
        for hash_position in range(64):
            x = hashes[:-lag, hash_position]
            for output_position in range(10):
                y = draws[lag:, output_position]
                table = np.zeros((16, 10), dtype=np.int32)
                np.add.at(table, (x, y), 1)
                chi, pvalue, _, _ = chi2_contingency(table, correction=False)
                cramer_v = math.sqrt(float(chi) / (table.sum() * 9.0))
                tests.append({
                    "lag": lag,
                    "hash_position": hash_position,
                    "output_position": output_position + 1,
                    "p": float(pvalue),
                    "cramer_v": cramer_v,
                })
    q = bh_qvalues([item["p"] for item in tests])
    for index, value in enumerate(q):
        tests[index]["q"] = float(value)
    ordered = sorted(tests, key=lambda item: item["q"])
    return {
        "tests": len(tests),
        "discoveries_q_le_0_05": int(sum(item["q"] <= 0.05 for item in tests)),
        "minimum_raw_p": float(min(item["p"] for item in tests)),
        "minimum_q": float(min(item["q"] for item in tests)),
        "strongest": ordered[:10],
    }


def integrity(hashes, draws, blocks, times):
    nibble_counts = np.bincount(hashes.reshape(-1), minlength=16)
    hash_chi = chisquare(nibble_counts)
    position_counts = [np.bincount(draws[:, position], minlength=10) for position in range(10)]
    position_p = [float(chisquare(count).pvalue) for count in position_counts]
    return {
        "draws": int(len(draws)),
        "all_complete_permutations": bool(np.all(np.sort(draws, axis=1) == np.arange(10))),
        "hash_nibble_chi_square_p": float(hash_chi.pvalue),
        "hash_nibble_min_count": int(nibble_counts.min()),
        "hash_nibble_max_count": int(nibble_counts.max()),
        "position_uniformity_min_raw_p": float(min(position_p)),
        "position_uniformity_bh_discoveries": int(sum(q <= 0.05 for q in bh_qvalues(position_p))),
        "block_increment_mode": int(np.bincount(np.clip(np.diff(blocks), 0, 64)).argmax()),
        "one_minute_intervals": int((np.diff(times) == 60000).sum()),
    }


def run(csv_path: Path, report_path: Path):
    started = time.time()
    periods, times, blocks, hashes, draws = load(csv_path)
    if len(draws) < 1000:
        raise RuntimeError("V51_HASH_AUDIT_DATASET_TOO_SMALL")
    history_x, hash_x, labels = build_rows(draws, hashes, times, blocks)
    combined_x = np.concatenate([history_x, hash_x[:, :, 20:]], axis=2)
    development_end = int(len(labels) * 0.85)
    cuts = [int(development_end * value) for value in (0.35, 0.50, 0.65, 0.78, 1.0)]
    folds = []
    for fold in range(4):
        train_end, test_end = cuts[fold], cuts[fold + 1]
        if test_end <= train_end:
            continue
        history_probability = fit_predict(history_x[:train_end], labels[:train_end], history_x[train_end:test_end], 5100 + fold)
        hash_probability = fit_predict(hash_x[:train_end], labels[:train_end], hash_x[train_end:test_end], 5200 + fold)
        combined_probability = fit_predict(combined_x[:train_end], labels[:train_end], combined_x[train_end:test_end], 5300 + fold)
        history_metric = metrics(history_probability, labels[train_end:test_end])
        hash_metric = metrics(hash_probability, labels[train_end:test_end])
        combined_metric = metrics(combined_probability, labels[train_end:test_end])
        folds.append({
            "fold": fold + 1,
            "train_issues": train_end,
            "test_issues": test_end - train_end,
            "history_schedule": history_metric,
            "historical_hash_schedule": hash_metric,
            "combined": combined_metric,
            "hash_increment_all_position_top3": combined_metric["all_position_top3"] - history_metric["all_position_top3"],
            "hash_increment_rank1": combined_metric["rank1"] - history_metric["rank1"],
        })
    fdr = fdr_audit(hashes[: START + development_end], draws[: START + development_end])
    all_positive = sum(fold["hash_increment_all_position_top3"] > 0.005 for fold in folds)
    rank_positive = sum(fold["hash_increment_rank1"] > 0.01 for fold in folds)
    hash_gate = bool(fdr["discoveries_q_le_0_05"] > 0 and all_positive >= 3 and rank_positive >= 3)
    data_hash = hashlib.sha256(csv_path.read_bytes()).hexdigest()
    report = {
        "version": "V51_HASH_SIGNAL_AUDIT_51072404",
        "data_sha256": data_hash,
        "prediction_time_contract": {
            "target_issue_hash_available_pre_draw": False,
            "target_issue_hash_allowed_in_prediction": False,
            "historical_hash_allowed_roles": [
                "draw_mapping_audit",
                "wrong_block_detection",
                "provider_rule_change_detection",
                "data_integrity_hash_chain",
                "leakage_guard",
                "anomaly_and_falsifier_evidence",
            ],
            "historical_hash_number_expert_default_weight": 0.0,
        },
        "integrity": integrity(hashes, draws, blocks, times),
        "historical_hash_fdr": fdr,
        "chronological_development_folds": folds,
        "hash_number_signal_gate": {
            "passed": hash_gate,
            "required": "q<=0.05 plus >0.5pp all-position lift and >1pp Rank1 lift in at least 3/4 folds",
            "all_position_positive_folds": all_positive,
            "rank1_positive_folds": rank_positive,
            "production_number_weight": 1.0 if hash_gate else 0.0,
        },
        "formal_conclusion": (
            "HISTORICAL_HASH_SIGNAL_PROVEN" if hash_gate
            else "HISTORICAL_HASH_USE_FOR_AUDIT_ONLY_NUMBER_WEIGHT_ZERO"
        ),
        "release_effect": "NO_MODEL_PROMOTION_AND_REAL_BET_REMAINS_LOCKED",
        "elapsed_seconds": time.time() - started,
    }
    report_path.parent.mkdir(parents=True, exist_ok=True)
    report_path.write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8")
    print(json.dumps(report, ensure_ascii=False))
    return report


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--csv", default=str(ROOT / "research_data/ethpk10_draws_20260316_201059.csv"))
    parser.add_argument("--report", default=str(ROOT / "reports/V51_HASH_SIGNAL_AUDIT.json"))
    args = parser.parse_args()
    run(Path(args.csv), Path(args.report))

