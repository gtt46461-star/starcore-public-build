"""Independent predraw trust model. Labels belong to the exact captured selector.

No META inversion, neutral filling, calibration score substitution or SEALED fitting.
"""
from pathlib import Path
import numpy as np


def capture(raw, labels):
    if raw is None or raw.get('predictability_missing_reason', '') not in ('', 'PREDICTABILITY_SELECTION_POLICY_CHANGED') or 'predictability_inputs' not in raw:
        return None
    x = np.asarray(raw['predictability_inputs'], dtype=float)
    ordinal = raw.get('selected_ordinal')
    if x.shape != (12,) or not np.isfinite(x).all() or (x < 0).any() or (x > 1).any():
        raise ValueError('PREDICTABILITY_INPUT_CONTRACT')
    if not isinstance(ordinal, int) or not 0 <= ordinal < 1200:
        raise ValueError('PREDICTABILITY_SELECTED_ORDINAL')
    for key in ('selection_policy_hash', 'predictability_schema_hash', 'selected_prediction_hash'):
        value = str(raw.get(key, ''))
        if len(value) != 64 or any(c not in '0123456789abcdef' for c in value):
            raise ValueError('PREDICTABILITY_IDENTITY:' + key)
    result={'inputs': x.tolist(), 'hit': int(labels[ordinal]),
            'selected_ordinal': ordinal, 'selection_policy_sha256': raw['selection_policy_hash'],
            'schema_sha256': raw['predictability_schema_hash'],
            'prediction_sha256': raw['selected_prediction_hash']}
    if 'selected_rank_score' in raw:
        score=float(raw['selected_rank_score'])
        if not np.isfinite(score):raise ValueError('PREDICTABILITY_SELECTED_SCORE_NONFINITE')
        result.update(rank_score=score,model_bundle_sha256=raw['model_bundle_hash'],created_at=int(raw.get('created_at',0)))
    return result


def predict(model, x):
    x = np.asarray(x, dtype=float)
    z = np.full(len(x), model['bias'], dtype=float)
    for i in range(12):
        z += (x[:, i] - model['mean'][i]) / model['scale'][i] * model['weight'][i]
    return 1 / (1 + np.exp(-np.clip(z, -35, 35)))


def fit(x, y, penalty):
    mean = x.mean(0); scale = np.maximum(x.std(0), 1e-6); z = (x - mean) / scale
    weight = np.zeros(12); bias = float(np.log((y.sum() + 1) / (len(y) - y.sum() + 1)))
    for _ in range(400):
        probability = 1 / (1 + np.exp(-np.clip(z @ weight + bias, -35, 35)))
        residual = probability - y
        weight -= .05 * (z.T @ residual / len(y) + penalty * weight)
        bias -= .05 * float(residual.mean())
    return {'family': 'PREDRAW_TRUST_LOGISTIC_12', 'mean': mean.tolist(),
            'scale': scale.tolist(), 'weight': weight.tolist(), 'bias': bias}


def stage(out: Path):
    from training.r929 import pipeline as p
    _x, _y, issues, manifest = p.load_dataset(out)
    path = out / 'predictability_captures.json'
    if not path.exists() or p.file_digest(path) != manifest.get('predictability_captures_sha256'):
        raise ValueError('PREDICTABILITY_CAPTURE_HASH_DRIFT')
    captures = p.read_json(path)
    if len(captures) != len(issues): raise ValueError('PREDICTABILITY_CAPTURE_COUNT')
    ta, tb = manifest['splits']['train']; ca, cb = manifest['splits']['calibration']
    required = captures[ta:tb] + captures[ca:cb]
    reason = ''
    if any(row is None for row in required): reason = 'FROZEN_PREDICTABILITY_INPUTS_UNAVAILABLE'
    identities = {(row['selection_policy_sha256'], row['schema_sha256']) for row in required if row}
    if len(identities) != 1: reason = 'CAPTURED_SELECTOR_OR_SCHEMA_CHANGED'
    artifact = {'revision': 'R932_INDEPENDENT_PREDICTABILITY_V1', 'production_authority': False,
                'dataset_contract_sha256': manifest['dataset_contract_sha256'],
                'fit_range': [ta, tb], 'calibration_range': [ca, cb], 'sealed_used': False,
                'missing_rows_n': sum(row is None for row in captures)}
    if reason:
        artifact.update(state='BLOCKED_INPUT_EVIDENCE', reason=reason, model=None)
    else:
        selector, schema = next(iter(identities))
        x = np.asarray([row['inputs'] for row in captures[ta:tb]])
        y = np.asarray([row['hit'] for row in captures[ta:tb]], dtype=float)
        folds = []; endpoints = list(map(int, np.linspace(max(12, len(x)//3), len(x), 6)))
        for penalty in (.01, .1, 1.):
            losses = []
            for stop, end in zip(endpoints[:-1], endpoints[1:]):
                if end <= stop + 1: raise ValueError('PREDICTABILITY_FOLD_TOO_SMALL')
                model = fit(x[:stop], y[:stop], penalty)
                losses.append(float(np.mean((predict(model, x[stop+1:end]) - y[stop+1:end])**2)))
            folds.append({'penalty': penalty, 'brier_folds': losses, 'mean_brier': float(np.mean(losses))})
        chosen = min(folds, key=lambda row: (row['mean_brier'], -row['penalty']))
        model = fit(x, y, chosen['penalty'])
        cx = np.asarray([row['inputs'] for row in captures[ca:cb]])
        cy = np.asarray([row['hit'] for row in captures[ca:cb]])
        artifact.update(state='FROZEN_RESEARCH_MODEL', model=model, folds=folds,
                        selection_policy_sha256=selector, schema_sha256=schema,
                        trained_until_issue=issues[tb-1], metrics=p.calibration_metrics(predict(model,cx),cy),
                        qualified_threshold=.40, target='CAPTURED_SELECTED_ACTION_HIT')
    artifact['artifact_sha256'] = p.digest(p.canonical(artifact))
    p.write_json(out/'predictability_artifact.json', artifact)
    p.register_trial(manifest, 'predictability', {'input': manifest.get('predictability_captures_sha256')},
                     artifact['state'], artifact['artifact_sha256'])
    return artifact

