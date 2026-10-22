#!/usr/bin/env python3
"""STARCORE R929 batch-3 staged training/evaluation pipeline.

The pipeline consumes only immutable Android predraw exports.  Labels are joined for training and
metrics after the export row has passed PIT/integrity checks.  Production authority is never granted
by this script; it emits research artifacts/certificates for Android verification and Future OOS.
"""
from __future__ import annotations
import argparse
import hashlib
import itertools
import json
import math
import os
import tempfile
from pathlib import Path
from typing import Dict, Iterable, List, Tuple
import numpy as np

SCHEMA = "R929_CANONICAL_1200_META96"
GAME = "Binance1-PK10"
FEATURE_STAGE = "ACTION_META_1200"
FEATURE_COUNT = 96
GROUP_SIZE = 1200
POSITIONS = 10
K = 3
ACTIONS = np.asarray([(p, *k3) for p in range(1, 11)
                      for k3 in itertools.combinations(range(1, 11), 3)], dtype=np.int16)
STAGES = ("dataset", "number", "gate", "ranker", "calibration", "policy", "evaluate")


def digest(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def file_digest(path: Path) -> str:
    h = hashlib.sha256()
    with path.open("rb") as f:
        for block in iter(lambda: f.read(1024 * 1024), b""):
            h.update(block)
    return h.hexdigest()


def canonical(obj) -> bytes:
    return json.dumps(obj, ensure_ascii=False, sort_keys=True, separators=(",", ":"),
                      allow_nan=False).encode("utf-8")


def write_json(path: Path, obj) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    payload = json.dumps(obj, ensure_ascii=False, sort_keys=True, indent=2, allow_nan=False) + "\n"
    temp = path.with_suffix(path.suffix + ".tmp")
    temp.write_text(payload, encoding="utf-8")
    os.replace(temp, path)


def read_json(path: Path):
    return json.loads(path.read_text(encoding="utf-8"))


def read_artifact(path):
    artifact=read_json(path);expected=artifact.get("artifact_sha256")
    payload={k:v for k,v in artifact.items() if k!="artifact_sha256"}
    if not expected or digest(canonical(payload))!=expected:raise ValueError("ARTIFACT_HASH_DRIFT:"+path.name)
    return artifact


def sigmoid(x):
    return 1.0 / (1.0 + np.exp(-np.clip(x, -35, 35)))


def unique_object(pairs):
    out = {}
    for key, value in pairs:
        if key in out:
            raise ValueError("DUPLICATE_JSON_FIELD:" + key)
        out[key] = value
    return out


def read_integrity_row(line: str):
    row = json.loads(line, object_pairs_hook=unique_object)
    if row.get("integrity_version") != 2:
        raise ValueError("R929_EXPORT_INTEGRITY_V2_REQUIRED")
    text = line.rstrip("\r\n")
    prefix, sep, _tail = text.rpartition(',"payload_sha256":')
    if not sep or digest((prefix + "}").encode("utf-8")) != row.get("payload_sha256"):
        raise ValueError("EXPORTED_PAYLOAD_HASH_MISMATCH")
    if row.get("schema") != SCHEMA or row.get("game") != GAME:
        raise ValueError("SCHEMA_OR_GAME_MISMATCH")
    if row.get("input_stage") != FEATURE_STAGE:
        raise ValueError("INPUT_STAGE_MISMATCH")
    manifest_hash = str(row.get("feature_manifest_sha256", ""))
    if len(manifest_hash) != 64 or any(c not in "0123456789abcdef" for c in manifest_hash):
        raise ValueError("FEATURE_MANIFEST_HASH_INVALID")
    return row


def validate_row(row, previous_issue: str, expected_manifest_hash: str | None):
    issue = str(row.get("target_issue", ""))
    base = str(row.get("max_source_issue", ""))
    if len(issue) != 12 or not issue.isdigit() or len(base) != 12 or not base.isdigit():
        raise ValueError("ISSUE_FORMAT")
    if previous_issue and not previous_issue < issue:
        raise ValueError("DUPLICATE_OR_OUT_OF_ORDER")
    if not base < issue:
        raise ValueError("PIT_SOURCE_NOT_BEFORE_TARGET")
    frozen = int(row.get("frozen_at", 0)); draw = int(row.get("draw_at", 0))
    if not 0 < frozen < draw:
        raise ValueError("POSTDRAW_FEATURE_FREEZE")
    if sorted(row.get("actual", [])) != list(range(1, 11)):
        raise ValueError("INVALID_PERMUTATION")
    actions = row.get("actions")
    if not isinstance(actions, list) or len(actions) != GROUP_SIZE:
        raise ValueError("GROUP_NOT_1200")
    manifest_hash = row["feature_manifest_sha256"]
    if expected_manifest_hash is not None and manifest_hash != expected_manifest_hash:
        raise ValueError("FEATURE_MANIFEST_DRIFT")
    for i, action in enumerate(actions):
        if len(action) != 5 or list(action[:4]) != ACTIONS[i].tolist():
            raise ValueError("ORDINAL_K3_MISMATCH:%d" % i)
        f = np.asarray(action[4], dtype=float)
        if f.shape != (FEATURE_COUNT,) or not np.isfinite(f).all():
            raise ValueError("INVALID_FEATURE_VECTOR:%d" % i)
    for field in ("snapshot_hash", "parent_bundle_hash", "selection_contract_hash", "record_hash"):
        value = str(row.get(field, ""))
        if len(value) != 64 or any(c not in "0123456789abcdef" for c in value):
            raise ValueError("HASH_INVALID:" + field)
    return manifest_hash


def decode_raw_upstream(row):
    values={}
    for proof in row.get("proofs",[]):
        for prefix in ("R932_UPSTREAM_RAW=","R932_UPSTREAM_SHA256="):
            if proof.startswith(prefix):
                if prefix in values: raise ValueError("DUPLICATE_RAW_UPSTREAM_PROOF")
                values[prefix]=proof[len(prefix):]
    payload=values.get("R932_UPSTREAM_RAW=")
    if payload is None:
        if row.get("upstream_raw_available"): raise ValueError("RAW_READY_WITHOUT_PAYLOAD")
        return None
    if digest(payload.encode("utf-8"))!=values.get("R932_UPSTREAM_SHA256="):
        raise ValueError("RAW_UPSTREAM_HASH_MISMATCH")
    raw=json.loads(payload,object_pairs_hook=unique_object)
    if raw.get("revision")!="R932_FROZEN_RAW_UPSTREAM_V1" or raw.get("target_issue")!=row["target_issue"] or raw.get("base_issue")!=row["max_source_issue"]:
        raise ValueError("RAW_UPSTREAM_PIT_IDENTITY")
    if not raw.get("ready"):
        if row.get("upstream_raw_available"): raise ValueError("RAW_READY_FLAG_CONFLICT")
        return None
    # Bind raw provenance to the exported predraw surface, not just tensor dimensions.
    # Older audit-only rows without a full export keep their explicit legacy status.
    if "frozen_at" in row:
        created = raw.get("created_at")
        if type(created) is not int or created <= 0 or created > int(row["frozen_at"]):
            raise ValueError("RAW_UPSTREAM_AVAILABLE_AFTER_FREEZE")
    if "feature_manifest_sha256" in row and raw.get("meta_schema_sha256") != row["feature_manifest_sha256"]:
        raise ValueError("RAW_UPSTREAM_META_SEMANTIC_MISMATCH")
    expected=["HIERARCHICAL_PERMUTATION_PRIOR","MULTISCALE_GAP_HAZARD","VARIABLE_ORDER_CONTEXT_TREE","STATE_DURATION_POSTERIOR","MULTILENGTH_MATRIX_PROFILE","LAGGED_RELATION_TENSOR","TEACHER_POSTERIOR","OFFLINE_RESIDUAL","BOUNDED_ONLINE_FTRL"]
    if raw.get("expert_ids")!=expected or raw.get("state_fields")!=["confidence","change_point_risk","remaining_life","posterior"]:
        raise ValueError("RAW_UPSTREAM_SEMANTIC_ORDER")
    for key,shape in (("number_features",(10,10,18)),("states",(10,4)),("expert_logits",(9,10,10))):
        value=np.asarray(raw.get(key),dtype=np.float64)
        if value.shape!=shape or not np.isfinite(value).all(): raise ValueError("RAW_UPSTREAM_TENSOR:"+key)
        raw[key]=value
    for key in ("number_schema_sha256","meta_schema_sha256","domain_schema_sha256","history_hash","model_bundle_hash"):
        if len(str(raw.get(key,"")))!=64 or any(c not in "0123456789abcdef" for c in raw[key]): raise ValueError("RAW_UPSTREAM_HASH:"+key)
    return raw


def chronological_splits(n: int, embargo: int = 1):
    if n < 120:
        raise ValueError("DATASET_LT_120_ISSUES")
    raw = [0, int(n * .45), int(n * .60), int(n * .72), int(n * .82), n]
    names = ["train", "tune", "calibration", "policy", "sealed"]
    splits = {}
    for i, name in enumerate(names):
        lo, hi = raw[i], raw[i + 1]
        if i > 0:
            lo = min(hi, lo + embargo)
        if hi - lo < 5:
            raise ValueError("SPLIT_TOO_SMALL:" + name)
        splits[name] = [lo, hi]
    return splits


def stage_dataset(source: Path, out: Path):
    out.mkdir(parents=True, exist_ok=True)
    source_hash = file_digest(source)
    previous = ""; count = 0; feature_manifest_hash = None; raw_ready = True
    issues: List[str] = []; snapshots: List[str] = []
    for line in source.open(encoding="utf-8-sig"):
        row = read_integrity_row(line)
        feature_manifest_hash = validate_row(row, previous, feature_manifest_hash)
        previous = row["target_issue"]; count += 1
        raw_ready = raw_ready and decode_raw_upstream(row) is not None
    splits = chronological_splits(count)
    x = np.lib.format.open_memmap(out / "features.npy", mode="w+", dtype=np.float64,
                                  shape=(count, GROUP_SIZE, FEATURE_COUNT))
    y = np.lib.format.open_memmap(out / "labels.npy", mode="w+", dtype=np.int8,
                                  shape=(count, GROUP_SIZE))
    raw_flags = np.zeros(count, dtype=np.int8)
    raw_arrays={name:np.lib.format.open_memmap(out/(name+".npy"),mode="w+",dtype=np.float64,shape=(count,*shape))
                for name,shape in (("raw_number",(10,10,18)),("raw_state",(10,4)),("raw_experts",(9,10,10)))}
    actual=np.lib.format.open_memmap(out/"actual.npy",mode="w+",dtype=np.int8,shape=(count,10))
    raw_schemas=None; raw_rejections=[]; predictability_captures=[]
    for t, line in enumerate(source.open(encoding="utf-8-sig")):
        row = read_integrity_row(line)
        issues.append(row["target_issue"]); snapshots.append(row["snapshot_hash"])
        raw=decode_raw_upstream(row);raw_flags[t]=int(raw is not None);actual[t]=np.asarray(row["actual"],dtype=np.int8)-1
        if raw is not None:
            schemas={key:raw[key] for key in ("number_schema_sha256","meta_schema_sha256","domain_schema_sha256")}
            if raw_schemas is not None and schemas!=raw_schemas:raise ValueError("RAW_UPSTREAM_SCHEMA_DRIFT")
            raw_schemas=schemas
            for name,key in (("raw_number","number_features"),("raw_state","states"),("raw_experts","expert_logits")):raw_arrays[name][t]=raw[key]
        else:
            for array in raw_arrays.values():array[t]=np.nan
            raw_rejections.append({"target_issue":row["target_issue"],"reason":"FROZEN_RAW_UPSTREAM_UNAVAILABLE_NO_META_INVERSION"})
        for ordinal, action in enumerate(row["actions"]):
            x[t, ordinal] = action[4]
            y[t, ordinal] = int(row["actual"][action[0] - 1] in action[1:4])
        from training.r932.predictability import capture as capture_predictability
        predictability_captures.append(capture_predictability(raw, y[t]))
        if int(y[t].sum()) != 360:
            raise ValueError("HIT_LABEL_GROUP_NOT_360:" + row["target_issue"])
    x.flush(); y.flush();actual.flush()
    for array in raw_arrays.values():array.flush()
    np.save(out/"raw_ready.npy",raw_flags)
    write_json(out/"raw_rejections.json",raw_rejections)
    write_json(out/"predictability_captures.json", predictability_captures)
    split_hash = digest(canonical(splits))
    manifest = {
        "schema": SCHEMA, "game": GAME, "input_stage": FEATURE_STAGE,
        "source_sha256": source_hash, "feature_manifest_sha256": feature_manifest_hash,
        "feature_count": FEATURE_COUNT, "group_size": GROUP_SIZE,
        "statistical_unit": "target_issue", "issues": count,
        "start_issue": issues[0], "end_issue": issues[-1], "splits": splits,
        "split_sha256": split_hash, "snapshot_hashes_sha256": digest(canonical(snapshots)),
        "features_sha256": file_digest(out / "features.npy"),
        "labels_sha256": file_digest(out / "labels.npy"),
        "pit_violations": 0, "postdraw_feature_count": 0,
        "predictability_captures_sha256":file_digest(out/"predictability_captures.json"),
        "raw_tensor_hashes":{name:file_digest(out/(name+".npy")) for name in (*raw_arrays,"actual","raw_ready")},
        "raw_input_schemas":raw_schemas,
        "legacy_missing_is_unknown": True,
        "raw_upstream_complete_for_all_rows": bool(raw_ready),
        "raw_upstream_ready_n": int(raw_flags.sum()),
        "raw_upstream_missing_n": int(count - raw_flags.sum()),
        "future_rule": "MISSING_RAW_IS_NOT_SYNTHESIZED; ACCUMULATE_NEW_PREDRAW_EVIDENCE",
        "embargo_issues": 1,
    }
    if file_digest(source)!=source_hash:raise ValueError("SOURCE_CHANGED_DURING_EXPORT")
    manifest["issues_sha256"]=digest(canonical(issues))
    manifest["dataset_contract_sha256"] = digest(canonical(manifest))
    write_json(out / "dataset_manifest.json", manifest)
    write_json(out / "issues.json", issues)
    return manifest


def load_dataset(out: Path):
    manifest = read_json(out / "dataset_manifest.json")
    payload={k:v for k,v in manifest.items() if k!="dataset_contract_sha256"}
    if manifest.get("dataset_contract_sha256")!=digest(canonical(payload)):raise ValueError("DATASET_MANIFEST_HASH_DRIFT")
    if manifest.get("schema") != SCHEMA:
        raise ValueError("DATASET_SCHEMA")
    if file_digest(out / "features.npy") != manifest["features_sha256"]:
        raise ValueError("FEATURE_FILE_HASH_DRIFT")
    if file_digest(out / "labels.npy") != manifest["labels_sha256"]:
        raise ValueError("LABEL_FILE_HASH_DRIFT")
    x = np.load(out / "features.npy", mmap_mode="r")
    y = np.load(out / "labels.npy", mmap_mode="r")
    issues = read_json(out / "issues.json")
    if manifest.get("issues_sha256")!=digest(canonical(issues)):raise ValueError("DATASET_ISSUE_IDENTITY_DRIFT")
    if len(issues)!=len(x) or len(y)!=len(x):raise ValueError("DATASET_ROW_COUNT_DRIFT")
    return x, y, issues, manifest


def registry_root() -> Path:
    return Path(os.environ.get("STARCORE_EXPERIMENT_REGISTRY",
                str(Path.home() / ".local/state/starcore/experiments-r929")))


def experiment_identity(manifest, stage: str, config: dict, sealed_issues: Iterable[str] = ()): 
    key = {
        "game": manifest["game"], "schema": manifest["schema"],
        "dataset_sha256": manifest["source_sha256"],
        "feature_manifest_sha256": manifest["feature_manifest_sha256"],
        "split_sha256": manifest["split_sha256"], "stage": stage,
        "config": config, "sealed_issues": list(sealed_issues),
    }
    return digest(canonical(key)), key


def register_trial(manifest, stage: str, config: dict, status: str, output_hash: str = ""):
    root = registry_root(); root.mkdir(parents=True, exist_ok=True)
    trial_id, key = experiment_identity(manifest, stage, config)

    # R932 E03A/E03B: every invocation is an immutable attempt.  The historical JSON file is
    # identity compatibility only; it must never collapse FAILED/INTERRUPTED/retry attempts.
    from training.r932.trial_registry import record as record_attempt
    attempt_id = record_attempt(
        root, trial_id, key, status, output_hash=output_hash,
        detail="R929_PIPELINE_REGISTER_TRIAL")

    record = root / (trial_id + ".json")
    body = {"trial_id": trial_id, "identity": key, "status": status,
            "output_hash": output_hash, "failed_trials_are_retained": True,
            "append_only_attempt_ledger": "trial_registry_r932.sqlite3",
            "first_or_current_attempt_id": attempt_id}
    if record.exists():
        prior = read_json(record)
        if prior.get("identity") != key:
            raise ValueError("TRIAL_ID_COLLISION")
        return trial_id

    # Concurrent registrations may share one trial identity.  Publish the legacy identity file
    # with a per-process temporary and atomic hard-link; losing writers only verify the winner.
    temp = root / (trial_id + "." + str(os.getpid()) + ".tmp")
    temp.write_text(json.dumps(body, ensure_ascii=False, sort_keys=True, indent=2, allow_nan=False) + "\n", encoding="utf-8")
    try:
        try:
            os.link(str(temp), str(record))
        except FileExistsError:
            pass
    finally:
        try:
            temp.unlink()
        except FileNotFoundError:
            pass
    prior = read_json(record)
    if prior.get("identity") != key:
        raise ValueError("TRIAL_ID_COLLISION")
    return trial_id


def consume_sealed_once(manifest, issues, config):
    from training.r932.registry import consume
    return consume(manifest,issues,config,registry_root())


def normalize_fit(train_x):
    mean = np.zeros(FEATURE_COUNT); second = np.zeros(FEATURE_COUNT)
    for group in train_x:
        mean += group.mean(0); second += (group * group).mean(0)
    mean /= len(train_x); scale = np.sqrt(np.maximum(second / len(train_x) - mean * mean, 1e-12))
    return mean, scale


def linear_fit(x, y, mode="listwise", epochs=6, lr=.02, penalty=.01):
    mean, scale = normalize_fit(x); weight = np.zeros(FEATURE_COUNT)
    for epoch in range(epochs):
        for group, label in zip(x, y):
            z = (group - mean) / scale; s = z @ weight; s -= s.max()
            p = np.exp(np.clip(s, -50, 50)); p /= p.sum()
            relevance = label / float(label.sum())
            gradient = z.T @ (p - relevance) + penalty * weight
            if mode in ("pairwise", "top_weighted"):
                pos = np.flatnonzero(label); neg = np.flatnonzero(label == 0)
                hard = neg[np.argsort(-s[neg], kind="stable")[:48]]
                good = pos[np.argsort(-s[pos], kind="stable")[:48]]
                negative_mask=label==0
                paired=[]
                for positive in good:
                    action=ACTIONS[positive]
                    neighbors=[j for j in range((positive//120)*120,(positive//120+1)*120)
                               if negative_mask[j] and len(set(action[1:]) & set(ACTIONS[j][1:]))==2]
                    paired.append(max(neighbors,key=lambda j:(s[j],-j)) if neighbors else int(hard[0]))
                hard=np.asarray(paired)
                losses=(z[hard]-z[good])*sigmoid(s[hard]-s[good])[:,None]
                if mode=="top_weighted":losses*= (1/np.log2(np.arange(len(good))+2))[:,None]
                gradient=losses.mean(0)+penalty*weight
            weight -= lr / (1 + epoch * .2) * np.clip(gradient, -3, 3)
    return {"family": mode.upper() + "_LINEAR_1200", "mean": mean.tolist(),
            "scale": scale.tolist(), "weight": weight.tolist(), "bias": 0.0}


def mlp_fit(x, y, epochs=3, hidden=12, lr=.01, seed=929):
    mean, scale = normalize_fit(x); rng = np.random.default_rng(seed)
    w1 = rng.normal(0, .04, size=(FEATURE_COUNT, hidden)); b1 = np.zeros(hidden)
    w2 = rng.normal(0, .04, size=hidden); b2 = 0.0
    for _ in range(epochs):
        for group, label in zip(x, y):
            z = (group - mean) / scale; h = np.tanh(z @ w1 + b1); s = h @ w2 + b2; s -= s.max()
            p = np.exp(np.clip(s, -50, 50)); p /= p.sum(); target = label / float(label.sum())
            ds = np.clip(p - target, -.05, .05)
            dw2 = h.T @ ds; db2 = ds.sum(); dh = ds[:, None] * w2[None, :] * (1 - h * h)
            dw1 = z.T @ dh; db1 = dh.sum(0)
            w2 -= lr * np.clip(dw2, -3, 3); b2 -= lr * db2
            w1 -= lr * np.clip(dw1, -3, 3); b1 -= lr * np.clip(db1, -3, 3)
    return {"family": "SMALL_MLP_1200", "mean": mean.tolist(), "scale": scale.tolist(),
            "w1": w1.tolist(), "b1": b1.tolist(), "w2": w2.tolist(), "b2": float(b2)}


def model_scores(model, x):
    family = model["family"]
    if family == "RAW_ACTION_P": return np.asarray(x[:, :, 0], dtype=float)
    mean = np.asarray(model["mean"]); scale = np.asarray(model["scale"])
    z = (x - mean) / scale
    if family == "SMALL_MLP_1200":
        w1=np.asarray(model["w1"]);w2=np.asarray(model["w2"]);b1=np.asarray(model["b1"])
        result=np.full(x.shape[:2],model["b2"],dtype=np.float64)
        for j in range(len(b1)):
            hidden=np.full(x.shape[:2],b1[j],dtype=np.float64)
            for i in range(FEATURE_COUNT):hidden+=z[:,:,i]*w1[i,j]
            result+=np.tanh(hidden)*w2[j]
        return result
    weight=np.asarray(model["weight"]);result=np.full(x.shape[:2],model.get("bias",0.0),dtype=np.float64)
    for i in range(FEATURE_COUNT):result+=((x[:,:,i]-mean[i])/max(1e-9,scale[i]))*weight[i]
    return result


def rank_order(score,raw_action_p=None):
    score=np.asarray(score,dtype=float)
    if score.ndim!=2 or score.shape[1]!=GROUP_SIZE or not np.isfinite(score).all():raise ValueError("RANK_SCORE_SURFACE_INVALID")
    raw=np.zeros_like(score) if raw_action_p is None else np.asarray(raw_action_p,dtype=float)
    if raw.shape!=score.shape or not np.isfinite(raw).all():raise ValueError("RAW_ACTION_P_SURFACE_INVALID")
    ordinal=np.broadcast_to(np.arange(GROUP_SIZE),score.shape)
    return np.lexsort((ordinal,-raw,-score),axis=1)


def selected_hits(score, y, raw_action_p=None):
    return y[np.arange(len(y)),rank_order(score,raw_action_p)[:,0]]


def metrics(score, y, raw_action_p=None):
    if len(y) == 0: return {"n": 0, "status": "UNAVAILABLE"}
    order=rank_order(score,raw_action_p);ranked=np.take_along_axis(y,order,1)
    hit = ranked[:, 0]; n = len(hit); rate = float(hit.mean()); z = 1.959963984540054
    den = 1 + z * z / n; center = rate + z * z / (2 * n)
    radius = z * math.sqrt((rate * (1 - rate) + z * z / (4 * n)) / n)
    miss = longest = 0
    for v in hit: miss = 0 if v else miss + 1; longest = max(longest, miss)
    return {"n": n, "rank1_rate": rate, "rank2_rate": float(ranked[:, 1].mean()),
            "rank3_rate": float(ranked[:, 2].mean()), "wilson_lower": (center - radius) / den,
            "wilson_upper": (center + radius) / den, "max_miss": longest,
            "mrr": float(np.mean(1 / (np.argmax(ranked, axis=1) + 1))),
            "ndcg_at_3": float((ranked[:, :3] / np.log2(np.arange(3) + 2)).sum(1).mean()
                               / np.sum(1 / np.log2(np.arange(3) + 2))),
            "blocks": [float(v.mean()) for v in np.array_split(hit, 4) if len(v)]}


def expanding_folds(x, y, end: int):
    folds=[]
    endpoints=np.linspace(max(30,end//3),end,6,dtype=int)[1:]
    for i,stop in enumerate(map(int,endpoints)):
        cut=max(20,int(stop*.8));start=cut+1
        if stop-start<5:raise ValueError("EXPANDING_FOLD_SUPPORT_TOO_SMALL")
        results=[]
        for family in ("RAW_ACTION_P","pairwise","listwise","top_weighted","SMALL_MLP_1200"):
            try:
                model={'family':'RAW_ACTION_P'} if family=='RAW_ACTION_P' else mlp_fit(x[:cut],y[:cut],epochs=2) if family=='SMALL_MLP_1200' else linear_fit(x[:cut],y[:cut],mode=family,epochs=2)
                results.append({'family':model['family'],'failed':False,'metrics':metrics(model_scores(model,x[start:stop]),y[start:stop],x[start:stop,:,0]),'artifact_sha256':digest(canonical(model))})
            except (ValueError,FloatingPointError) as failure:
                results.append({'family':family,'failed':True,'reason':type(failure).__name__+':'+str(failure)})
        folds.append({'fold':i,'train':[0,cut],'embargo':[cut,start],'test':[start,stop],'candidates':results})
    if len(folds)<5:raise ValueError("EXPANDING_FOLDS_LT_5")
    return folds


def stage_number(out: Path):
    x, y, issues, manifest = load_dataset(out)
    if not manifest.get("raw_upstream_complete_for_all_rows"):
        result = {"status": "WAIT_FUTURE_RAW_UPSTREAM", "production_authority": False,
                  "reason": "NUMBER_Q_RETRAIN_REQUIRES_RAW_HISTORY_BASE_EXPERT_INPUT; META96_CANNOT_FEED_NUMBER_Q",
                  "ready_n": manifest.get("raw_upstream_ready_n", 0),
                  "missing_n": manifest.get("raw_upstream_missing_n", len(x))}
        write_json(out / "number_stage.json", result); register_trial(manifest, "number", {}, "WAIT_FUTURE_RAW_UPSTREAM")
        return result
    from . import deep_candidates
    state,experts,actual=load_raw_dataset(out,manifest)
    result=deep_candidates.compare(x,y,ACTIONS,issues,manifest["splits"],out,write_json,digest,canonical,
                                   raw_state=state,raw_experts=experts)
    result={"status":"NUMBER_FAMILY_COMPARISON_COMPLETE","production_authority":False,
            "result_sha256":result["result_sha256"],"raw_schemas":manifest["raw_input_schemas"]}
    write_json(out/"number_stage.json",result)
    register_trial(manifest,"number",{"contract":"R932_DIRECT_FROZEN_UPSTREAM"},"TUNE_FROZEN",result["result_sha256"])
    return result


def load_raw_dataset(out,manifest):
    if not manifest.get("raw_upstream_complete_for_all_rows"):raise ValueError("RAW_UPSTREAM_INCOMPLETE")
    for name,sha in manifest["raw_tensor_hashes"].items():
        if file_digest(out/(name+".npy"))!=sha:raise ValueError("RAW_TENSOR_HASH_DRIFT:"+name)
    return tuple(np.load(out/(name+".npy"),mmap_mode="r") for name in ("raw_state","raw_experts","actual"))


def stage_gate(out: Path):
    from . import deep_candidates
    _x,_y,issues,manifest=load_dataset(out)
    if not manifest.get("raw_upstream_complete_for_all_rows"):
        result={"status":"WAIT_FROZEN_RAW_UPSTREAM","production_authority":False}
        write_json(out/"gate_stage.json",result);return result
    state,experts,actual=load_raw_dataset(out,manifest)
    a,b=manifest["splits"]["train"];ta,tb=manifest["splits"]["tune"]
    gate=deep_candidates.train_gate9(state[a:b],experts[a:b],actual[a:b])
    q,weights=deep_candidates.predict_gate9(gate,state[ta:tb],experts[ta:tb])
    result={"status":"GATE9_TUNE_FROZEN","input_stage":"FROZEN_EXPERT_LOGITS","trained_until_issue":issues[b-1],
            "model":deep_candidates.serial(gate),"metrics":deep_candidates.metrics(q,actual[ta:tb]),
            "raw_schemas":manifest["raw_input_schemas"],"production_authority":False}
    result["artifact_sha256"]=digest(canonical(result));write_json(out/"gate_stage.json",result)
    register_trial(manifest,"gate",{"expert_count":9},"TUNE_FROZEN",result["artifact_sha256"]);return result


def stage_ranker(out: Path):
    from training.r932.ranker_comparison import stage
    return stage(out)


def logistic_fit(raw, y, iterations=500):
    f = np.column_stack([np.ones(len(raw)), raw]); w = np.zeros(2)
    for _ in range(iterations):
        w -= .05 * (f.T @ (sigmoid(f @ w) - y) / len(y) + np.asarray([0, .01 * w[1]]))
    return {"family": "LOGISTIC", "bias": float(w[0]), "weight": float(w[1])}


def beta_fit(raw, y, iterations=500):
    p = np.clip(sigmoid(raw), 1e-6, 1 - 1e-6)
    f = np.column_stack([np.ones(len(p)), np.log(p), np.log1p(-p)]); w = np.zeros(3)
    for _ in range(iterations):
        w -= .03 * (f.T @ (sigmoid(f @ w) - y) / len(y) + .005 * w)
    return {"family": "BETA", "weight": w.tolist()}


def isotonic_fit(raw, y, bins=20):
    order = np.argsort(raw, kind="stable"); chunks = np.array_split(order, min(bins, len(order)))
    blocks = []
    for chunk in chunks:
        if len(chunk): blocks.append([float(raw[chunk].mean()), float(y[chunk].mean()), int(len(chunk))])
    i = 0
    while i < len(blocks) - 1:
        if blocks[i][1] <= blocks[i + 1][1]: i += 1; continue
        n = blocks[i][2] + blocks[i + 1][2]
        merged = [(blocks[i][0] * blocks[i][2] + blocks[i + 1][0] * blocks[i + 1][2]) / n,
                  (blocks[i][1] * blocks[i][2] + blocks[i + 1][1] * blocks[i + 1][2]) / n, n]
        blocks[i:i + 2] = [merged]; i = max(0, i - 1)
    return {"family": "ISOTONIC", "blocks": blocks}


def calibration_predict(model, raw):
    if model["family"] == "LOGISTIC": return sigmoid(model["bias"] + model["weight"] * raw)
    if model["family"] == "BETA":
        p = np.clip(sigmoid(raw), 1e-6, 1 - 1e-6); w = np.asarray(model["weight"])
        return sigmoid(w[0] + w[1] * np.log(p) + w[2] * np.log1p(-p))
    blocks = model["blocks"]; centers = np.asarray([b[0] for b in blocks]); probs = np.asarray([b[1] for b in blocks])
    return probs[np.argmin(np.abs(raw[:, None] - centers[None, :]), axis=1)]


def calibration_metrics(p, y):
    bins = []; ece = 0.; min_n = 10**9
    for i in range(10):
        mask = (p >= i / 10) & (p < ((i + 1) / 10) if i < 9 else p <= 1)
        if mask.any():
            n = int(mask.sum()); mp = float(p[mask].mean()); hr = float(y[mask].mean())
            ece += n / len(y) * abs(mp - hr); min_n = min(min_n, n)
            bins.append({"bin": i, "n": n, "p": mp, "hit_rate": hr})
    return {"n": len(y), "hits": int(y.sum()), "brier": float(np.mean((p - y) ** 2)), "ece": ece,
            "log_loss": float(-np.mean(y * np.log(np.clip(p, 1e-9, 1)) + (1 - y) * np.log(np.clip(1 - p, 1e-9, 1)))),
            "reliability_bin_min_n": 0 if min_n == 10**9 else int(min_n), "bins": bins}


def selection_policy_hash(ranker_artifact):
    values=["R929_SELECTION_EVIDENCE_V1",ranker_artifact["artifact_sha256"],
            "V51_RUNTIME_RANK1_THEN_CERTIFIED_RESEARCH_MIXERS","CERTIFIED_RESEARCH_WEIGHT_CONTRACT","PREDRAW_STATE_DOMAIN_ONLY",
            "RANK_SCORE_DESC_RAW_P_DESC_POSITION_ASC_K3_LEXICOGRAPHIC",ranker_artifact["schema_hash"]]
    return digest("|".join(values).encode("utf-8"))


def stage_calibration(out: Path):
    x, y, _issues, manifest = load_dataset(out)
    ranker = read_artifact(out / "ranker_artifact.json") if (out / "ranker_artifact.json").exists() else stage_ranker(out)
    from training.r932.stacking import load as load_stack
    x, _stack = load_stack(out,manifest,ranker)
    ca, cb = manifest["splits"]["calibration"]; score = model_scores(ranker["model"], x[ca:cb])
    raw = score.max(1); hit = selected_hits(score, y[ca:cb],x[ca:cb,:,0])
    cut=max(2,int(len(raw)*.6))
    if len(raw)-cut<2:raise ValueError("CAL_INNER_VALIDATION_TOO_SMALL")
    candidates = [logistic_fit(raw[:cut], hit[:cut]), beta_fit(raw[:cut], hit[:cut]), isotonic_fit(raw[:cut], hit[:cut])]
    reports = []
    for model in candidates:
        pred = calibration_predict(model, raw[cut:]); m = calibration_metrics(pred, hit[cut:])
        reports.append({"model": model, "metrics": m})
    chosen = min(reports, key=lambda r: (r["metrics"]["brier"], r["metrics"]["ece"]))
    policy_hash = selection_policy_hash(ranker)
    artifact = {"revision": "R929_SELECTED_CALIBRATION_V1", "model": chosen["model"],
                "metrics": chosen["metrics"], "selection_policy_sha256": policy_hash,
                "input_schema_sha256": manifest["feature_manifest_sha256"],
                "ranker_sha256": ranker["artifact_sha256"], "fit_split": "calibration_inner_train", "fit_range":[ca,ca+cut], "selection_range":[ca+cut,cb],
                "probability_semantics": "SELECTED_ACTION_HIT_PROBABILITY",
                "raw_rank_score_is_probability": False, "production_authority": False}
    artifact["artifact_sha256"] = digest(canonical(artifact))
    write_json(out / "calibration_candidates.json", reports); write_json(out / "calibration_artifact.json", artifact)
    register_trial(manifest, "calibration", {"families": [r["model"]["family"] for r in reports]},
                   "FROZEN_CAL_SELECTED", artifact["artifact_sha256"])
    return artifact


def pareto_front(rows):
    out = []
    for row in rows:
        dominated = False
        for other in rows:
            if other is row: continue
            if other["coverage"] >= row["coverage"] and other["hit_rate"] >= row["hit_rate"] \
                    and other["brier"] <= row["brier"] and (other["coverage"] > row["coverage"]
                    or other["hit_rate"] > row["hit_rate"] or other["brier"] < row["brier"]):
                dominated = True; break
        if not dominated: out.append(row)
    return out


def stage_predictability(out: Path):
    from training.r932.predictability import stage
    return stage(out)


def stage_policy(out: Path):
    x, y, _issues, manifest = load_dataset(out)
    ranker = read_artifact(out / "ranker_artifact.json") if (out / "ranker_artifact.json").exists() else stage_ranker(out)
    from training.r932.stacking import load as load_stack
    x, _stack = load_stack(out,manifest,ranker)
    cal = read_artifact(out / "calibration_artifact.json") if (out / "calibration_artifact.json").exists() else stage_calibration(out)
    if cal["ranker_sha256"]!=ranker["artifact_sha256"]:raise ValueError("CAL_RANKER_DEPENDENCY_DRIFT")
    pa, pb = manifest["splits"]["policy"]; score = model_scores(ranker["model"], x[pa:pb]); hit = selected_hits(score, y[pa:pb],x[pa:pb,:,0])
    prob = calibration_predict(cal["model"], score.max(1)); rows = []
    from training.r932.predictability import predict as predict_trust
    trust = read_artifact(out / "predictability_artifact.json") if (out / "predictability_artifact.json").exists() else stage_predictability(out)
    captured = read_json(out / "predictability_captures.json")[pa:pb]
    trust_state = "READY"
    if trust.get("model") is None: trust_state = "PREDICTABILITY_MODEL_UNAVAILABLE"
    elif trust.get("selection_policy_sha256") != cal["selection_policy_sha256"]: trust_state = "PREDICTABILITY_SELECTOR_RECAPTURE_REQUIRED"
    elif any(row is None for row in captured): trust_state = "PREDICTABILITY_PREDRAW_INPUTS_UNAVAILABLE"
    elif any(row["selection_policy_sha256"] != trust["selection_policy_sha256"] for row in captured): trust_state = "PREDICTABILITY_POLICY_CAPTURE_DRIFT"
    trust_probability = predict_trust(trust["model"], np.asarray([row["inputs"] for row in captured])) if trust_state == "READY" else np.full(len(prob), np.nan)
    for threshold in np.arange(.20, .701, .02):
        for trust_threshold in (.35, .40, .45, .50, .60):
            mask = (prob >= threshold) & (trust_probability >= trust_threshold); n = int(mask.sum())
            rows.append({"threshold": float(round(threshold, 4)), "predictability_threshold": trust_threshold,
                         "eligible_n": len(mask), "selected_n": n, "coverage": float(mask.mean()),
                         "hit_rate": float(hit[mask].mean()) if n else 0.0,
                         "brier": float(np.mean((prob[mask] - hit[mask]) ** 2)) if n else 1.0,
                         "risk": float(1 - hit[mask].mean()) if n else 1.0})
    front = pareto_front(rows)
    feasible = [r for r in front if r["coverage"] >= .15 and r["hit_rate"] >= .40]
    chosen = max(feasible, key=lambda r: (r["hit_rate"], r["coverage"], -r["brier"])) if feasible else None
    artifact = {"revision": "R929_POLICY_V1", "selection_policy_sha256": cal["selection_policy_sha256"],
                "calibration_sha256": cal["artifact_sha256"], "predictability_sha256":trust["artifact_sha256"],
                "predictability_state":trust_state,"pareto": rows, "pareto_front": front,
                "target_coverage": .15, "target_hit_rate": .40,
                "chosen": chosen, "effect_status": "POLICY_TARGET_MET_ON_POLICY_SPLIT" if chosen else "FAIL_EFFECT",
                "frequency_debt": False, "forced_bet": False, "chase": False,
                "decision_owner": "DecisionGateEngine_ONLY", "production_authority": False}
    artifact["artifact_sha256"] = digest(canonical(artifact)); write_json(out / "policy_artifact.json", artifact)
    register_trial(manifest, "policy", {"target_coverage": .15, "target_hit_rate": .40}, artifact["effect_status"],
                   artifact["artifact_sha256"])
    return artifact


def stage_evaluate(out: Path):
    x, y, issues, manifest = load_dataset(out)
    ranker = read_artifact(out / "ranker_artifact.json") if (out / "ranker_artifact.json").exists() else stage_ranker(out)
    from training.r932.stacking import load as load_stack
    x, _stack = load_stack(out,manifest,ranker)
    cal = read_artifact(out / "calibration_artifact.json") if (out / "calibration_artifact.json").exists() else stage_calibration(out)
    policy = read_artifact(out / "policy_artifact.json") if (out / "policy_artifact.json").exists() else stage_policy(out)
    if cal["ranker_sha256"]!=ranker["artifact_sha256"] or policy["calibration_sha256"]!=cal["artifact_sha256"]:
        raise ValueError("EVALUATE_DEPENDENCY_DRIFT")
    token = consume_sealed_once(manifest, issues, {"ranker": ranker["artifact_sha256"],
                                "cal": cal["artifact_sha256"], "policy": policy["artifact_sha256"]})
    sa, sb = manifest["splits"]["sealed"]; score = model_scores(ranker["model"], x[sa:sb]); raw = x[sa:sb, :, 0]
    hit = selected_hits(score, y[sa:sb],x[sa:sb,:,0]); baseline = selected_hits(raw, y[sa:sb]); prob = calibration_predict(cal["model"], score.max(1))
    selected = np.zeros(len(hit), dtype=bool)
    if policy.get("chosen") is not None:
        from training.r932.predictability import predict as predict_trust
        trust=read_artifact(out / "predictability_artifact.json")
        captures=read_json(out / "predictability_captures.json")[sa:sb]
        if trust["artifact_sha256"]!=policy["predictability_sha256"] or trust.get("selection_policy_sha256")!=cal["selection_policy_sha256"] or any(row is None or row["selection_policy_sha256"]!=trust["selection_policy_sha256"] for row in captures):
            raise ValueError("SEALED_PREDICTABILITY_CAPTURE_DEPENDENCY_DRIFT")
        trust_probability=predict_trust(trust["model"],np.asarray([row["inputs"] for row in captures]))
        selected=(prob >= float(policy["chosen"]["threshold"])) & (trust_probability >= float(policy["chosen"]["predictability_threshold"]))
    report = {"sealed_token": token, "ranker": metrics(score, y[sa:sb],raw), "baseline": metrics(raw, y[sa:sb]),
              "calibration": calibration_metrics(prob, hit), "eligible_n": len(hit),
              "selected_n": int(selected.sum()), "selected_coverage": float(selected.mean()),
              "selected_hit_rate": float(hit[selected].mean()) if selected.any() else 0.0,
              "target_coverage": .15, "target_hit_rate": .40,
              "effect_status": "PASS_EFFECT" if selected.mean() >= .15 and selected.any() and hit[selected].mean() >= .40 else "FAIL_EFFECT",
              "future_live_n": 0, "model_proven": False,
              "proof_boundary": "SEALED_IS_NOT_FUTURE_LIVE"}
    report["report_sha256"] = digest(canonical(report)); write_json(out / "sealed_evaluation.json", report)
    register_trial(manifest, "evaluate", {"sealed_token": token["token"]}, report["effect_status"], report["report_sha256"])
    return report


def bundle(out: Path):
    files = {}
    for path in sorted(out.iterdir()):
        if path.is_file() and path.name != "bundle_manifest.json": files[path.name] = file_digest(path)
    body = {"schema": SCHEMA, "revision": "R929_BATCH3_BUNDLE_V1", "files": files,
            "state": "RESEARCH_STAGED_NOT_PRODUCTION", "model_proven": False,
            "activation": "ANDROID_ATOMIC_BUNDLE_VERIFY_REQUIRED"}
    body["bundle_sha256"] = digest(canonical(body)); write_json(out / "bundle_manifest.json", body)
    return body


def run_all(source: Path, out: Path):
    if out.exists() and any(out.iterdir()): raise FileExistsError("OUTPUT_MUST_BE_NEW_OR_EMPTY")
    stage_dataset(source, out); stage_number(out); stage_gate(out); stage_ranker(out)
    stage_calibration(out); stage_predictability(out); stage_policy(out); stage_evaluate(out); return bundle(out)


def main():
    p = argparse.ArgumentParser(); sub = p.add_subparsers(dest="stage", required=True)
    allp = sub.add_parser("all"); allp.add_argument("--input", required=True, type=Path); allp.add_argument("--out", required=True, type=Path)
    dp = sub.add_parser("dataset"); dp.add_argument("--input", required=True, type=Path); dp.add_argument("--out", required=True, type=Path)
    for name in ("number", "gate", "ranker", "calibration", "predictability", "policy", "evaluate", "bundle"):
        sp = sub.add_parser(name); sp.add_argument("--out", required=True, type=Path)
    args = p.parse_args()
    if args.stage == "all": result = run_all(args.input, args.out)
    elif args.stage == "dataset": result = stage_dataset(args.input, args.out)
    elif args.stage == "number": result = stage_number(args.out)
    elif args.stage == "gate": result = stage_gate(args.out)
    elif args.stage == "ranker": result = stage_ranker(args.out)
    elif args.stage == "calibration": result = stage_calibration(args.out)
    elif args.stage == "predictability": result = stage_predictability(args.out)
    elif args.stage == "policy": result = stage_policy(args.out)
    elif args.stage == "evaluate": result = stage_evaluate(args.out)
    else: result = bundle(args.out)
    print(json.dumps({"stage": args.stage, "status": "OK", "result": result}, ensure_ascii=False, allow_nan=False))


if __name__ == "__main__":
    main()

