#!/usr/bin/env python3
"""One-time source integration for E03B durable full-attempt trial history."""
from pathlib import Path

R=Path(__file__).resolve().parents[1]
p=R/'training/r929/pipeline.py'
s=p.read_text(encoding='utf-8')
old='''def register_trial(manifest, stage: str, config: dict, status: str, output_hash: str = ""):\n    root = registry_root(); root.mkdir(parents=True, exist_ok=True)\n    trial_id, key = experiment_identity(manifest, stage, config)\n    record = root / (trial_id + ".json")\n    body = {"trial_id": trial_id, "identity": key, "status": status,\n            "output_hash": output_hash, "failed_trials_are_retained": True}\n    if record.exists():\n        prior = read_json(record)\n        if prior.get("identity") != key:\n            raise ValueError("TRIAL_ID_COLLISION")\n        return trial_id\n    write_json(record, body)\n    return trial_id\n'''
new='''def register_trial(manifest, stage: str, config: dict, status: str, output_hash: str = ""):\n    # Canonical identity remains content-addressed; each invocation is an append-only attempt.\n    # A renamed/copied pipeline or archive operation cannot alter statistical identity.\n    from training.r932.trial_registry import record as record_attempt\n    root = registry_root(); root.mkdir(parents=True, exist_ok=True)\n    trial_id, key = experiment_identity(manifest, stage, config)\n    record_attempt(root, trial_id, key, status, output_hash, detail="stage="+stage)\n    # Retain the historical JSON identity mirror for old readers, but the SQLite attempt ledger\n    # is canonical for all statuses/failures/interruption history and is immutable after commit.\n    legacy = root / (trial_id + ".json")\n    body = {"trial_id": trial_id, "identity": key, "registry": "trial_registry_r932.sqlite3",\n            "failed_trials_are_retained": True}\n    if legacy.exists():\n        prior = read_json(legacy)\n        if prior.get("identity") != key:\n            raise ValueError("TRIAL_ID_COLLISION")\n    else:\n        write_json(legacy, body)\n    return trial_id\n'''
if s.count(old)!=1: raise SystemExit('R932_E03B_REGISTER_TRIAL_ANCHOR='+str(s.count(old)))
p.write_text(s.replace(old,new,1),encoding='utf-8')
print('R932_E03B_TRIAL_REGISTRY_INTEGRATED')
