#!/usr/bin/env python3
"""Prospective simulation from exported draws; not natural Future-OOS or REAL authority.

Fixed position 1, fixed K=3. All windows, horizons, weights, holdout size and the
0.40 research threshold are fixed here, before reading outcomes. Every horizon
is scored separately. No winning retrospective horizon selection is permitted.
"""
import argparse, hashlib, json, math
from datetime import datetime
from pathlib import Path
import numpy as np

def interval(hit,n):
    p=hit/n;z=1.95996398454;d=1+z*z/n
    mid=(p+z*z/(2*n))/d;half=z*math.sqrt((p*(1-p)+z*z/(4*n))/n)/d
    return [mid-half,mid+half]

def metrics(rows):
    n=len(rows)
    if not n:return {'n':0,'hit_rate':None}
    hit=sum(r['hit'] for r in rows);streak=longest=0;bs=0
    for r in rows:
        streak=0 if r['hit'] else streak+1;longest=max(longest,streak)
        bs+=(r['raw_mass']-r['hit'])**2
    return {'n':n,'hits':hit,'hit_rate':hit/n,'wilson95':interval(hit,n),
            'maximum_miss_streak':longest,'brier_raw_not_calibrated':bs/n}

def main():
    p=argparse.ArgumentParser();p.add_argument('draws',type=Path);p.add_argument('output',type=Path);a=p.parse_args()
    raw=a.draws.read_bytes();data=sorted(json.loads(raw),key=lambda x:x['issue'])
    assert len({r['issue'] for r in data})==len(data)
    # The supplied feed declares 1440 one-minute issue ordinals per day.
    ordinals=[]
    for r in data:
        issue=r['issue'];slot=int(issue[8:]);assert len(issue)==12 and 1<=slot<=1440
        ordinals.append(datetime.strptime(issue[:8],'%Y%m%d').toordinal()*1440+slot-1)
    assert all(b==a+1 for a,b in zip(ordinals,ordinals[1:])), 'REPORT_DRAW_GAP'
    values=np.array([int(r['n1'])-1 for r in data]);n=len(values);first=n-1200
    assert first>=960
    forecasts={};records=[];baseline=[];holdout={str(h):[] for h in [1,2,4,8]}
    windows=[48,240,960];horizons=[1,2,4,8]
    for anchor in range(959,n-1):
        transitions=[]
        for window in windows:
            past=values[anchor-window+1:anchor+1]
            counts=np.bincount(past[:-1]*10+past[1:],minlength=100).reshape(10,10).astype(float)+1
            transitions.append(counts/counts.sum(axis=1,keepdims=True))
        for h in horizons:
            target=anchor+h
            if target>=n:continue
            q=sum(np.linalg.matrix_power(t,h)[values[anchor]] for t in transitions)/len(windows)
            assert np.isfinite(q).all() and abs(float(q.sum())-1)<1e-12
            # Store a deterministic forecast before accessing the target outcome.
            picks=sorted(range(10),key=lambda digit:(-q[digit],digit))[:3]
            record={'anchor_issue':data[anchor]['issue'],'target_issue':data[target]['issue'],'horizon':h,
                    'position':1,'k3':[int(x+1) for x in sorted(picks)],'raw_mass':float(q[picks].sum()),
                    'role':'PROBE_REPLAY_NOT_FORMAL','frozen_distribution':[float(x) for x in q]}
            forecasts[target,h]=q.copy()
            if target>=first:
                record['hit']=int(values[target] in picks);records.append(record);holdout[str(h)].append(record)
    consensus=[];gated=[]
    for target in range(first,n):
        # All four forecasts existed by target-1. The newest forecast's base is
        # fixed; no outcomes from target or later can select a horizon or threshold.
        q=sum(forecasts[target,h] for h in horizons)/len(horizons)
        picks=sorted(range(10),key=lambda d:(-q[d],d))[:3]
        r={'target_issue':data[target]['issue'],'raw_mass':float(q[picks].sum()),'hit':int(values[target] in picks)}
        consensus.append(r)
        if r['raw_mass']>=.40:gated.append(r)
        baseline.append({'raw_mass':.3,'hit':int(values[target] in [0,1,2])})
    report={'scope':'RETROSPECTIVE_PROSPECTIVE_REPLAY_NOT_NATURAL_FUTURE_NOT_CERTIFICATE',
            'draws_sha256':hashlib.sha256(raw).hexdigest(),'position':1,'k':3,'history_windows':windows,
            'horizons':horizons,'dirichlet_alpha':1,'weights':'EQUAL_FIXED','holdout_n':1200,
            'holdout_first':data[first]['issue'],'holdout_last':data[-1]['issue'],
            'fixed_123_baseline':metrics(baseline),'horizon_results':{h:metrics(r) for h,r in holdout.items()},
            'probe_consensus':metrics(consensus),'probe_consensus_threshold_040':metrics(gated),
            'threshold_040_coverage':len(gated)/1200,'promotion':'NOT_GRANTED',
            'limitations':['Single reported feed; no independent feed verification.',
                'Replay rather than device-frozen natural predictions.',
                'Raw probability mass is not selection-conditioned calibration.',
                'No hyperparameter or horizon chosen by holdout outcomes; multiple comparisons remain exploratory.',
                'Correlated horizon outcomes must not be summed into independent sample N.']}
    a.output.mkdir(parents=True,exist_ok=True)
    (a.output/'probe-replay-result.json').write_text(json.dumps(report,indent=2)+'\n')
    with (a.output/'probe-freeze-replay.jsonl').open('w') as f:
        for r in records:f.write(json.dumps(r,separators=(',',':'))+'\n')
    print(json.dumps(report,indent=2))

if __name__=='__main__':main()
