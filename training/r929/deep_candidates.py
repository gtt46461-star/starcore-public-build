"""NumberQ / D0-D3 / Gate9 comparison on directly frozen upstream tensors.

Production comparison requires raw_state and raw_experts from R932 capture. extract_upstream is
retained solely for legacy audit and is never a fallback for missing production inputs.
"""
from __future__ import annotations
import sys
from pathlib import Path
import numpy as np

TRAINING_ROOT = Path(__file__).resolve().parents[1]
if str(TRAINING_ROOT) not in sys.path:
    sys.path.insert(0, str(TRAINING_ROOT))
from r811_autonomous_research import sinkhorn, sinkhorn_backward, softmax  # noqa: E402

CONTRACT = "R929_UPSTREAM_EXPERT_STATE_V1"
EXPERTS = 9
POSITIONS = 10
NUMBERS = 10
STATE_DIM = 4


def extract_upstream(action_meta, actions):
    x = np.asarray(action_meta, dtype=float)
    if x.ndim != 3 or x.shape[1:] != (1200, 96):
        raise ValueError("R929_META1200_SHAPE")
    # State indices 11..14 are invariant across the 120 actions at one position.
    state = np.empty((len(x), POSITIONS, STATE_DIM), dtype=float)
    logits = np.empty((len(x), EXPERTS, POSITIONS, NUMBERS), dtype=float)
    for p in range(POSITIONS):
        ids = np.arange(p * 120, (p + 1) * 120)
        base_state = x[:, ids[0], 11:15]
        if not np.allclose(x[:, ids, 11:15], base_state[:, None, :], atol=1e-10, rtol=1e-10):
            raise ValueError("R929_STATE_NOT_POSITION_INVARIANT:%d" % p)
        state[:, p] = base_state
        number_sets = actions[ids, 1:4]
        for e in range(EXPERTS):
            means = x[:, ids, 25 + 5 * e]
            overall = means.mean(1)
            # For all C(10,3) K3 means, each number appears 36 times. Exact linear inversion.
            for number in range(1, 11):
                containing = np.any(number_sets == number, axis=1)
                logits[:, e, p, number - 1] = (27.0 * means[:, containing].mean(1) - 20.0 * overall) / 7.0
            reconstructed = np.asarray([
                logits[:, e, p, triple - 1].mean(1) for triple in number_sets
            ]).T
            if not np.allclose(reconstructed, means, atol=1e-8, rtol=1e-8):
                raise ValueError("R929_EXPERT_INVERSION_FAILED:%d:%d" % (e, p))
    if not np.isfinite(state).all() or not np.isfinite(logits).all():
        raise ValueError("R929_UPSTREAM_NONFINITE")
    return state, logits


def labels_from_k3(y, actions):
    labels = np.empty((len(y), POSITIONS), dtype=np.int16)
    for t in range(len(y)):
        for p in range(POSITIONS):
            ids = slice(p * 120, (p + 1) * 120)
            triples = actions[ids, 1:4][y[t, ids] == 1]
            counts = np.bincount(triples.reshape(-1), minlength=11)[1:]
            if counts.max() != 36 or np.sum(counts == 36) != 1:
                raise ValueError("R929_LABEL_RECOVERY_NOT_UNIQUE")
            labels[t, p] = int(np.argmax(counts))
        if sorted(labels[t].tolist()) != list(range(10)):
            raise ValueError("R929_LABEL_NOT_PERMUTATION")
    return labels


def baseline_logits(experts):
    return experts.mean(axis=1)


def base_probability(experts):
    return sinkhorn(baseline_logits(experts).reshape(-1, 100), 60)[0]


def feature_vectors(state, experts):
    # Strictly upstream/current-predraw: state + expert logits + disagreement summaries.
    flat_state = state.reshape(len(state), -1)
    flat_expert = experts.reshape(len(experts), -1)
    disagreement = experts.std(axis=1).reshape(len(experts), -1)
    mean = experts.mean(axis=(2, 3))
    return np.concatenate([flat_state, flat_expert, disagreement, mean], axis=1)


def require_contiguous_issues(issues):
    from datetime import datetime, timedelta
    previous=None
    for issue in issues:
        if len(issue)!=12 or not issue.isdigit() or not 1<=int(issue[8:])<=1440:
            raise ValueError('D2_INVALID_ISSUE')
        day=datetime.strptime(issue[:8],'%Y%m%d')
        ordinal=day.toordinal()*1440+int(issue[8:])
        if previous is not None and ordinal!=previous+1:
            raise ValueError('D2_GAP_REPLAY_REQUIRED:'+issue)
        previous=ordinal


def slow_memory(features, alpha=.05):
    slow = np.zeros_like(features); memory = np.zeros(features.shape[1], dtype=float)
    for i, row in enumerate(features):
        slow[i] = memory
        memory = (1 - alpha) * memory + alpha * row
    return slow


def initialize(kind, in_dim, hidden=64, seed=929):
    rng = np.random.default_rng(seed + int(kind[1]))
    first = in_dim * 2 if kind == "D2" else in_dim
    dims = [first, hidden, 48, 100]
    return {"kind": kind,
            "weights": [rng.normal(0, np.sqrt(2 / (a + b)), (b, a)) for a, b in zip(dims, dims[1:])],
            "biases": [np.zeros(b) for b in dims[1:]]}


def forward(model, features, slow, base_logits_flat=None):
    raw = np.concatenate([features, slow], axis=1) if model["kind"] == "D2" else features
    a = np.clip((raw - model["mean"]) / model["scale"], -8, 8)
    inputs, pre = [], []
    for k, (w, b) in enumerate(zip(model["weights"], model["biases"])):
        inputs.append(a); z = a @ w.T + b
        pre.append(z); a = np.maximum(z, 0) if k < 2 else z
    if model["kind"] == "D1" and base_logits_flat is not None:
        a = a + base_logits_flat
    return a, (inputs, pre)


def train(kind, features, slow, experts, labels, teacher, epochs=5, lr=.008):
    model = initialize(kind, features.shape[1])
    raw = np.concatenate([features, slow], axis=1) if kind == "D2" else features
    model["mean"] = raw.mean(0); model["scale"] = np.maximum(raw.std(0), 1e-6)
    target = np.eye(10)[labels].reshape(-1, 100)
    if kind == "D3":
        # Distillation teacher is the frozen upstream expert ensemble for the same predraw input;
        # it contains no target-window labels. Labels are used only in TRAIN fitting.
        target = .5 * target + .5 * teacher.reshape(-1, 100)
    base_flat = baseline_logits(experts).reshape(-1, 100)
    for epoch in range(epochs):
        for start in range(0, len(features), 24):
            sl = slice(start, start + 24)
            logits, tape = forward(model, features[sl], slow[sl], base_flat[sl] if kind == "D1" else None)
            q, _logq, sink_tape = sinkhorn(logits, 30)
            grad = sinkhorn_backward(-target[sl].reshape(-1, 10, 10) / (max(1, len(logits)) * 10), sink_tape)
            inputs, pre = tape; gw, gb = [], []
            for k in range(2, -1, -1):
                if k < 2: grad = grad * (pre[k] > 0)
                gw.insert(0, grad.T @ inputs[k] + 1e-5 * model["weights"][k]); gb.insert(0, grad.sum(0))
                grad = grad @ model["weights"][k]
            norm = np.sqrt(sum((g * g).sum() for g in gw + gb)); factor = min(1, 5 / max(norm, 1e-12))
            for k in range(3):
                model["weights"][k] -= lr * factor * gw[k]; model["biases"][k] -= lr * factor * gb[k]
    return model


def predict(model, features, slow, experts):
    base = baseline_logits(experts).reshape(-1, 100)
    logits, _ = forward(model, features, slow, base if model["kind"] == "D1" else None)
    q = sinkhorn(logits, 60)[0]
    if not np.isfinite(q).all() or (q < 0).any(): raise ValueError("R929_NUMBER_Q_INVALID")
    if np.max(np.abs(q.sum(2) - 1)) > 1e-6 or np.max(np.abs(q.sum(1) - 1)) > 1e-6:
        raise ValueError("R929_SINKHORN_MARGINAL")
    return q


def gate_features(state, experts):
    # Upstream-only compact gate surface, no NumberQ/meta downstream feedback.
    return np.concatenate([state.reshape(len(state), -1),
                           experts.mean((2, 3)), experts.std((2, 3)),
                           experts.std(1).mean(2)], axis=1)


def train_gate9(state, experts, labels, epochs=35):
    x = gate_features(state, experts); mean=x.mean(0); scale=np.maximum(x.std(0),1e-6); z=np.clip((x-mean)/scale,-8,8)
    w=np.zeros((EXPERTS,x.shape[1])); b=np.zeros(EXPERTS); y=np.eye(10)[labels]
    for _ in range(epochs):
        gate=softmax(z@w.T+b,1); combined=np.einsum('ne,neij->nij',gate,experts)
        _q,_logq,tape=sinkhorn(combined.reshape(-1,100),30); gl=sinkhorn_backward(-y/(len(x)*10),tape).reshape(-1,10,10)
        dg=np.einsum('nij,neij->ne',gl,experts); dz=gate*(dg-(dg*gate).sum(1,keepdims=True))
        w-=.04*(dz.T@z+1e-5*w); b-=.04*dz.sum(0)
    return {'mean':mean,'scale':scale,'weight':w,'bias':b}


def predict_gate9(model,state,experts):
    x=gate_features(state,experts); z=np.clip((x-model['mean'])/model['scale'],-8,8)
    gate=softmax(z@model['weight'].T+model['bias'],1); logits=np.einsum('ne,neij->nij',gate,experts)
    return sinkhorn(logits.reshape(-1,100),60)[0], gate


def metrics(q, labels):
    order=np.argsort(-q,axis=2,kind='stable'); hit=order[:,:,0]==labels; top3=np.any(order[:,:,:3]==labels[:,:,None],axis=2)
    p=q[np.arange(len(q))[:,None],np.arange(10),labels]
    return {'n_issues':len(q),'cross_entropy':float(-np.log(np.maximum(p,1e-15)).mean()),
            'top1':float(hit.mean()),'top3':float(top3.mean()),'per_position_top1':hit.mean(0).tolist(),
            'brier':float(np.mean(np.sum((q-np.eye(10)[labels])**2,axis=2))),
            'row_error':float(np.max(np.abs(q.sum(2)-1))), 'column_error':float(np.max(np.abs(q.sum(1)-1))),
            'finite_nonnegative':bool(np.isfinite(q).all() and (q>=0).all())}


def serial(model):
    return {k:[a.tolist() for a in v] if isinstance(v,list) else v.tolist() if isinstance(v,np.ndarray) else v for k,v in model.items()}


def compare(action_meta, action_labels, actions, issues, splits, out, write_json, digest, canonical, *, raw_state=None, raw_experts=None):
    if raw_state is None or raw_experts is None:
        raise ValueError("PRODUCTION_TRAINING_REQUIRES_DIRECT_FROZEN_UPSTREAM; META_INVERSION_IS_LEGACY_AUDIT_ONLY")
    state=np.asarray(raw_state,dtype=float);experts=np.asarray(raw_experts,dtype=float)
    if state.shape!=(len(action_meta),10,4) or experts.shape!=(len(action_meta),9,10,10) or not np.isfinite(state).all() or not np.isfinite(experts).all():
        raise ValueError("DIRECT_RAW_TENSOR_CONTRACT")
    labels=labels_from_k3(action_labels, actions)
    features=feature_vectors(state,experts); slow=slow_memory(features); teacher=base_probability(experts)
    a,b=splits['train']; ta,tb=splits['tune']; models={}; reports={}
    continuity=''
    try:require_contiguous_issues(issues)
    except ValueError as error:continuity=str(error)
    baseline=base_probability(experts[ta:tb]); reports['BASE_NUMBER_Q_9_EXPERT_MEAN']=metrics(baseline,labels[ta:tb])
    for kind in ('D0','D1','D2','D3'):
        if kind=='D2' and continuity:
            reports[kind]={'status':'FAILED','error':continuity};continue
        m=train(kind,features[a:b],slow[a:b],experts[a:b],labels[a:b],teacher[a:b]);models[kind]=m
        q=predict(m,features[ta:tb],slow[ta:tb],experts[ta:tb]);reports[kind]=metrics(q,labels[ta:tb])
        artifact={'revision':'R929_'+kind+'_UPSTREAM','feature_contract':CONTRACT,'trained_until_issue':issues[b-1],
                  'input_stage':'BASE_EXPERTS_PLUS_PREDRAW_STATE','model':serial(m),'proven':False}
        artifact['artifact_sha256']=digest(canonical(artifact));write_json(out/('number_'+kind+'.json'),artifact)
    gate=train_gate9(state[a:b],experts[a:b],labels[a:b]);qg,gate_w=predict_gate9(gate,state[ta:tb],experts[ta:tb]);reports['GATE9']=metrics(qg,labels[ta:tb])
    reports['GATE9']['mean_expert_weight']=gate_w.mean(0).tolist()
    gate_art={'revision':'R929_GATE9_UPSTREAM','feature_contract':CONTRACT,'trained_until_issue':issues[b-1],
              'input_stage':'BASE_EXPERTS_PLUS_PREDRAW_STATE','model':serial(gate),'proven':False}
    gate_art['artifact_sha256']=digest(canonical(gate_art));write_json(out/'number_GATE9.json',gate_art)
    # Same five chronological folds for all candidates; failures remain explicit records.
    folds=[]
    edges=np.linspace(max(a+21,a+(b-a)//3),b,6,dtype=int)
    if min(np.diff(edges))<1:raise ValueError('NUMBER_COMPARISON_FOLD_SUPPORT')
    for start,stop in zip(edges[:-1],edges[1:]):
        cut=int(start)-1 # one complete issue embargo; non-overlapping test windows
        fold={'train':[a,cut],'embargo':[cut,int(start)],'test':[int(start),int(stop)],
              'train_end':issues[cut-1],'test_start':issues[start],'test_end':issues[stop-1],'models':{}}
        base=base_probability(experts[start:stop]);fold['models']['BASE_NUMBER_Q_9_EXPERT_MEAN']=metrics(base,labels[start:stop])
        for kind in ('D0','D1','D2','D3'):
            try:
                if kind=='D2' and continuity:raise ValueError(continuity)
                m=train(kind,features[a:cut],slow[a:cut],experts[a:cut],labels[a:cut],teacher[a:cut],epochs=2)
                fold['models'][kind]=metrics(predict(m,features[start:stop],slow[start:stop],experts[start:stop]),labels[start:stop])
            except Exception as exc: fold['models'][kind]={'status':'FAILED','error':type(exc).__name__+':'+str(exc)}
        try:
            gm=train_gate9(state[a:cut],experts[a:cut],labels[a:cut],epochs=12);fold['models']['GATE9']=metrics(predict_gate9(gm,state[start:stop],experts[start:stop])[0],labels[start:stop])
        except Exception as exc: fold['models']['GATE9']={'status':'FAILED','error':type(exc).__name__+':'+str(exc)}
        folds.append(fold)
    valid={k:v for k,v in reports.items() if v.get('finite_nonnegative',True) and 'cross_entropy' in v}
    champion=min(valid,key=lambda k:(valid[k]['cross_entropy'], -valid[k]['top1'], k))
    result={'contract':CONTRACT,'same_folds':True,'candidate_metrics':reports,'folds':folds,'tune_champion':champion,
            'online_FTRL':{'status':'NOT_EXPORTED_IN_R929_DATASET','promotion':False},
            'online_TINY_DEEP':{'status':'SEPARATE_PAST_LABEL_ONLY_RUNTIME_CERTIFICATE_REQUIRED','promotion':False},
            'production_switch':False,'reason':'TUNE_SELECTION_ONLY; SEALED/FUTURE/PARITY/RESOURCE_CERTIFICATE_REQUIRED'}
    result['result_sha256']=digest(canonical(result));write_json(out/'number_comparison.json',result)
    return result

