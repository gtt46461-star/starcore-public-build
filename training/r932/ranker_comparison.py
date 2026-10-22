"""Issue-level R0--R5 comparison for the production staged Ranker trainer.

Only the five existing Java-deployable model families can be selected. R5 is a real,
bounded stump ensemble research comparison; absence of a Java exporter is an explicit
deployment failure, never silently relabelled a linear model. No REAL authority here.
"""
from __future__ import annotations
import fcntl
import time
from pathlib import Path
import numpy as np

REVISION = "R932_RANKER_SAME_FOLD_V2"
FAMILIES = ("RAW_ACTION_P", "pairwise", "listwise", "top_weighted", "SMALL_MLP_1200", "STUMP_ENSEMBLE_RESEARCH")
EPOCHS = 3


def validate(x, y):
    if x.ndim != 3 or x.shape[1:] != (1200, 96) or y.shape != x.shape[:2]:
        raise ValueError("R932_COMPARISON_FULL1200_SHAPE")
    if len(x) == 0 or not np.isfinite(x).all():
        raise ValueError("R932_COMPARISON_NONFINITE_OR_EMPTY")
    if not np.isin(y, (0, 1)).all() or not np.all(y.sum(axis=1) == 360):
        raise ValueError("R932_COMPARISON_ISSUE_REQUIRES_360_HITS")


def folds(start, stop, count=5, embargo=1):
    """Expanding TRAIN, whole-issue embargo, disjoint TEST; absolute dataset offsets."""
    if not (0 <= start < stop and count == 5 and embargo >= 1):
        raise ValueError("R932_COMPARISON_FOLD_ARGUMENTS")
    first = start + max(12, (stop - start) // 3)
    edges = np.linspace(first, stop, count + 1, dtype=int)
    if np.min(np.diff(edges)) < 1 or first - embargo <= start:
        raise ValueError("R932_COMPARISON_FOLD_SUPPORT")
    return [{"fold": i, "train": [start, int(a) - embargo],
             "embargo": [int(a) - embargo, int(a)], "test": [int(a), int(b)]}
            for i, (a, b) in enumerate(zip(edges[:-1], edges[1:]))]


def fit_stumps(x, y):
    """Deterministic, fixed-pass boosted least-squares stumps; research only.

    Feature choices/quantiles use TRAIN only. Keeping eight predeclared META features
    bounds the search; all 1200 actions in every TRAIN issue still contribute.
    """
    z = np.asarray(x).reshape(-1, 96)
    label = np.asarray(y, dtype=float).reshape(-1)
    prediction = np.full(len(label), .3)
    nodes = []
    selected = (0, 1, 2, 3, 11, 12, 13, 14)
    thresholds = {j: np.unique(np.quantile(z[:, j], (.25, .5, .75))) for j in selected}
    for _ in range(EPOCHS):
        residual = label - prediction
        best = None
        for feature in selected:
            for threshold in thresholds[feature]:
                left = z[:, feature] <= threshold
                n = int(left.sum())
                if n == 0 or n == len(z):
                    continue
                lv = float(residual[left].mean()); rv = float(residual[~left].mean())
                loss = float(np.sum((residual[left] - lv)**2) + np.sum((residual[~left] - rv)**2))
                key = (loss, feature, float(threshold))
                if best is None or key < best[0]:
                    best = (key, left, lv, rv)
        if best is None:
            break
        (loss, feature, threshold), mask, lv, rv = best
        lv *= .25; rv *= .25
        prediction += np.where(mask, lv, rv)
        nodes.append({"feature": feature, "threshold": threshold, "left": lv, "right": rv})
    return {"family": "STUMP_ENSEMBLE_RESEARCH", "bias": .3, "nodes": nodes,
            "deployment_supported": False}


def stump_scores(model, x):
    score = np.full(x.shape[:2], model["bias"], dtype=float)
    for node in model["nodes"]:
        score += np.where(x[:, :, node["feature"]] <= node["threshold"], node["left"], node["right"])
    return score


def fit(p, family, x, y):
    if family == "RAW_ACTION_P": return {"family": family}
    if family == "SMALL_MLP_1200": return p.mlp_fit(x, y, epochs=EPOCHS, seed=929)
    if family == "STUMP_ENSEMBLE_RESEARCH": return fit_stumps(x, y)
    return p.linear_fit(x, y, mode=family, epochs=EPOCHS)


def score(p, model, x):
    return stump_scores(model, x) if model["family"] == "STUMP_ENSEMBLE_RESEARCH" else p.model_scores(model, x)


def evaluate(p, model, x, y, issues, baseline_hits):
    times = []; scores = []
    for group in x:
        started = time.perf_counter_ns()
        row = score(p, model, group[None, :, :])
        times.append(time.perf_counter_ns() - started)
        scores.append(row[0])
    surface = np.asarray(scores, dtype=np.float64)
    order = p.rank_order(surface, x[:, :, 0])
    hit = y[np.arange(len(y)), order[:, 0]].astype(int)
    delta = hit - baseline_hits
    metrics = p.metrics(surface, y, x[:, :, 0])
    metrics["paired_baseline_delta"] = float(delta.mean())
    metrics["paired_baseline_wins"] = int(np.sum(delta == 1))
    metrics["paired_baseline_losses"] = int(np.sum(delta == -1))
    metrics["paired_baseline_ties"] = int(np.sum(delta == 0))
    metrics["scoring_host_p95_ms"] = float(np.quantile(times, .95) / 1e6)
    # Raw per-position state is in META 11..14. These fixed bins are diagnostics;
    # unlike a domain certificate they neither change the selector nor grant weight.
    confidence = x[np.arange(len(x)), order[:, 0], 11]
    regime = np.clip((confidence * 3).astype(int), 0, 2)
    metrics["fixed_state_bins"] = [
        {"bin": j, "n_issues": int(np.sum(regime == j)),
         "rank1_rate": float(hit[regime == j].mean()) if np.any(regime == j) else None,
         "paired_baseline_delta": float(delta[regime == j].mean()) if np.any(regime == j) else None}
        for j in range(3)]
    return metrics, {"issues": list(issues), "selected_ordinals": order[:, 0].tolist(),
        "hits": hit.tolist(), "baseline_hits": baseline_hits.astype(int).tolist(),
        "all_1200_scores_sha256": p.digest(surface.astype('<f8').tobytes()),
        "all_1200_order_sha256": p.digest(order.astype('<i4').tobytes()),
        "tie_break": "SCORE_DESC_RAW_P_DESC_ORDINAL_ASC", "statistical_unit": "issue"}


def attempt(p, out, family, split, x, y, issues, dataset_hash, baseline_hits):
    """Freeze identity before fitting. Keep failed and interrupted attempts in the ledger."""
    source = p.digest(Path(__file__).read_bytes() + Path(p.__file__).read_bytes())
    identity = {"revision": REVISION, "family": family, "split": split,
                "dataset_hash": dataset_hash, "source_sha256": source, "seed": 929,
                "fit_epoch_budget": EPOCHS, "group_size": 1200}
    trial = p.digest(p.canonical(identity)); directory = out / 'ranker_trials' / trial
    request = directory / 'request.json'; result_path = directory / 'result.json'
    if request.exists() and p.read_json(request) != identity:
        raise ValueError("R932_RANKER_TRIAL_IDENTITY_DRIFT")
    if not request.exists(): p.write_json(request, identity)
    if result_path.exists():
        old = p.read_json(result_path)
        if old['trial_id'] != trial: raise ValueError("R932_RANKER_TRIAL_RESULT_IDENTITY")
        expected=old.get('result_sha256')
        if expected != p.digest(p.canonical({k:v for k,v in old.items() if k!='result_sha256'})):
            raise ValueError("R932_RANKER_RESULT_HASH_DRIFT")
        if old.get('failed'): return old, None
        model = p.read_artifact(directory / 'model.json')['model']
        # Replay scores rather than trusting a cached pass flag.
    else:
        a, b = split['train']
        started = time.perf_counter_ns()
        try:
            model = fit(p, family, x[a:b], y[a:b])
            trained = {"trial_id": trial, "trained_until_issue": issues[b-1], "model": model}
            trained['artifact_sha256'] = p.digest(p.canonical(trained))
            p.write_json(directory / 'model.json', trained)
        except (ValueError, FloatingPointError, ArithmeticError, RuntimeError) as error:
            failure = {"trial_id": trial, "family": family, "failed": True,
                       "reason": type(error).__name__ + ':' + str(error), "split": split}
            failure['result_sha256']=p.digest(p.canonical(failure))
            p.write_json(result_path, failure)
            return failure, None
        fit_ms = (time.perf_counter_ns() - started) / 1e6
    lo, hi = split['test']
    try:
        metric, predictions = evaluate(p, model, x[lo:hi], y[lo:hi], issues[lo:hi], baseline_hits)
        p.write_json(directory / 'predictions.json', predictions)
        result = {"trial_id": trial, "family": model['family'], "candidate_family": family,
                  "failed": False, "split": split, "metrics": metric,
                  "artifact_sha256": p.digest(p.canonical(model)),
                  "prediction_sha256": p.digest(p.canonical(predictions)),
                  "fit_host_ms": old.get('fit_host_ms') if result_path.exists() else fit_ms,
                  "deployable": family != 'STUMP_ENSEMBLE_RESEARCH',
                  "deployment_failure": 'NO_JAVA_TREE_EXPORTER' if family == 'STUMP_ENSEMBLE_RESEARCH' else ''}
    except (ValueError, FloatingPointError, ArithmeticError, RuntimeError) as error:
        result = {"trial_id": trial, "family": family, "failed": True, "split": split,
                  "reason": type(error).__name__ + ':' + str(error)}
        result['result_sha256']=p.digest(p.canonical(result))
        p.write_json(result_path, result); return result, None
    if result_path.exists():
        if old['artifact_sha256'] != result['artifact_sha256'] or old['prediction_sha256'] != result['prediction_sha256']:
            raise ValueError("R932_RANKER_COMPLETED_TRIAL_REPLAY_DRIFT")
        return old, model
    result['result_sha256']=p.digest(p.canonical(result))
    p.write_json(result_path, result)
    return result, model


def choose(reports, fold_reports):
    """TUNE selects once; no actual gain, failed fold or unsupported exporter keeps baseline."""
    baseline = reports[0]
    if baseline.get('failed'):
        raise ValueError("R932_BASELINE_COMPARISON_FAILED")
    best = baseline
    for row in reports[1:]:
        if row.get('failed') or not row.get('deployable'): continue
        family = row['candidate_family']
        evidence = [f['candidates'][FAMILIES.index(family)] for f in fold_reports]
        if len(evidence) != 5 or any(r.get('failed') for r in evidence): continue
        n = sum(r['metrics']['n'] for r in evidence)
        delta = sum(r['metrics']['paired_baseline_delta'] * r['metrics']['n'] for r in evidence) / n
        if delta <= 0 or row['metrics']['paired_baseline_delta'] <= 0: continue
        if row['metrics']['rank1_rate'] > best['metrics']['rank1_rate']:
            best = row
    return best


def _compare(p, out, x, y, issues, start, stop, tune, dataset_hash):
    validate(x[start:stop], y[start:stop]); validate(x[tune[0]:tune[1]], y[tune[0]:tune[1]])
    if len(issues) != len(x) or any(a >= b for a,b in zip(issues, issues[1:])):
        raise ValueError("R932_COMPARISON_ISSUE_ORDER")
    if tune[0] <= stop or tune[1] > len(x): raise ValueError("R932_COMPARISON_TUNE_EMBARGO")
    protocol = {"revision": REVISION, "families": list(FAMILIES), "folds": folds(start,stop),
                "tune": list(tune), "train": [start,stop], "epoch_budget": EPOCHS,
                "statistical_unit": "issue", "same_data_and_epoch_budget": True,
                "equal_hardware_cost_claim": False, "source": "EARLIER_OOF_NUMBER_GATE_META",
                "selection": "POSITIVE_TUNE_AND_DISJOINT_OOF_PAIRED_GAIN_ELSE_BASELINE",
                "effect_proven": False, "may_authorize_real": False}
    protocol_path = out / 'ranker_comparison_protocol.json'
    if protocol_path.exists() and p.read_json(protocol_path) != protocol:
        raise ValueError("R932_COMPARISON_PROTOCOL_CHANGED_USE_NEW_EXPERIMENT_DIRECTORY")
    if not protocol_path.exists(): p.write_json(protocol_path, protocol)
    fold_reports = []
    for split in protocol['folds']:
        a,b = split['test']; baseline = p.selected_hits(x[a:b,:,0],y[a:b],x[a:b,:,0])
        rows = [attempt(p,out,family,split,x,y,issues,dataset_hash,baseline)[0] for family in FAMILIES]
        fold_reports.append(dict(split, candidates=rows))
    split = {"fold": "TUNE", "train": [start,stop], "embargo": [stop,tune[0]], "test": list(tune)}
    a,b = tune; baseline = p.selected_hits(x[a:b,:,0],y[a:b],x[a:b,:,0])
    pairs = [attempt(p,out,family,split,x,y,issues,dataset_hash,baseline) for family in FAMILIES]
    reports = [row for row,model in pairs]; selected = choose(reports,fold_reports)
    model = pairs[next(i for i,r in enumerate(reports) if r['trial_id']==selected['trial_id'])][1]
    report = {"revision": REVISION, "candidates": reports, "selected": selected,
              "expanding_folds": fold_reports, "protocol_sha256": p.digest(p.canonical(protocol)),
              "scope": "TRAIN_TUNE_ONLY_NOT_SEALED_FUTURE_OR_ANDROID_DEVICE",
              "effect_proven": False, "may_authorize_real": False}
    p.write_json(out / 'ranker_candidates.json', report)
    return model, report


def compare(p, out, x, y, issues, start, stop, tune, dataset_hash):
    out=Path(out);out.mkdir(parents=True,exist_ok=True)
    # OS lock releases after crashes; an interrupted request resumes the same trial identity.
    with (out/'ranker_comparison.lock').open('a+b') as lock:
        try: fcntl.flock(lock.fileno(),fcntl.LOCK_EX|fcntl.LOCK_NB)
        except BlockingIOError as error:
            raise RuntimeError("R932_RANKER_COMPARISON_ALREADY_RUNNING") from error
        try: return _compare(p,out,x,y,issues,start,stop,tune,dataset_hash)
        finally: fcntl.flock(lock.fileno(),fcntl.LOCK_UN)


def stage(out):
    from training.r929 import pipeline as p
    from training.r932.stacking import load, create
    out=Path(out); _x,y,issues,manifest=p.load_dataset(out)
    create(out); x,stack=load(out,manifest)
    start=stack['ranker_train_start']; stop=manifest['splits']['train'][1]
    chosen,report=compare(p,out,x,y,issues,start,stop,manifest['splits']['tune'],manifest['dataset_contract_sha256'])
    artifact={"revision":"R929_RANKER_V1","model":chosen,"input_stage":p.FEATURE_STAGE,
              "schema_hash":manifest['feature_manifest_sha256'],"parent_artifact_hash":manifest['dataset_contract_sha256'],
              "upstream_stack_sha256":stack['artifact_sha256'],"train_range":[start,stop],
              "trained_until_issue":issues[stop-1],"tie_break":"RANK_SCORE_DESC_RAW_P_DESC_POSITION_ASC_K3_LEXICOGRAPHIC",
              "complete_action_count":1200,"comparison_protocol_sha256":report['protocol_sha256']}
    artifact['artifact_sha256']=p.digest(p.canonical(artifact));p.write_json(out/'ranker_artifact.json',artifact)
    p.register_trial(manifest,'ranker',{'revision':REVISION,'families':list(FAMILIES)},'FROZEN_TUNE_SELECTED',artifact['artifact_sha256'])
    return artifact

