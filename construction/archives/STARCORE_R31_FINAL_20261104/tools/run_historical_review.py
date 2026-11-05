#!/usr/bin/env python3
"""R31 plan diagnostic, never an installed strategy or REAL/FUTURE receipt."""
import csv, hashlib, json, math
from pathlib import Path
import numpy as np

ROOT = Path(__file__).resolve().parents[1]
SPEC = json.loads((ROOT/'evidence/review_experiment/replay_spec.json').read_text())
OUT = ROOT/'evidence/review_experiment'
OUT.mkdir(parents=True, exist_ok=True)

def wilson(h, n):
    if n == 0: return [None, None]
    z=1.959963984540054; p=h/n; z2=z*z
    a=(p+z2/(2*n))/(1+z2/n)
    b=z*math.sqrt(p*(1-p)/n+z2/(4*n*n))/(1+z2/n)
    return [a-b,a+b]

def longest_loss(hits):
    longest=run=0
    for h in hits:
        run=0 if h else run+1; longest=max(longest,run)
    return longest

def streak_probability(p, n, limit=6):
    mass=[1.0]+[0.0]*limit
    for _ in range(n): mass=[sum(mass)*p]+[mass[k-1]*(1-p) for k in range(1,limit+1)]
    return sum(mass)

def main():
    data_path=ROOT/'evidence/derived/draws_5000.csv'
    raw=data_path.read_bytes()
    with data_path.open(encoding='utf-8-sig',newline='') as f: rows=list(csv.DictReader(f))
    a=np.array([[int(r['n'+str(i)])-1 for i in range(1,11)] for r in rows],dtype=np.int64)
    assert a.shape==(5000,10)
    assert all(sorted(r.tolist())==list(range(10)) for r in a)
    assert len({r['issue'] for r in rows})==5000
    times=np.array([int(r['open_time']) for r in rows]);assert np.all(np.diff(times)==60000)
    N=len(rows); warm=SPEC['warmup']; end=SPEC['select_end_exclusive']
    ids=[]; results=[]; all_masks={}; all_trades={}; thresholds={}
    for pos in range(10):
        onehot=np.eye(10,dtype=np.int64)[a[:,pos]]
        prefix=np.concatenate([np.zeros((1,10),dtype=np.int64),np.cumsum(onehot,axis=0)])
        defs=[(f,w) for f in ('hot_frequency','cold_frequency') for w in SPEC['windows']]+[('lag_neighbors',1),('fixed_1_2_3',0)]
        for family,w in defs:
            cid=f'{family}:p{pos+1}:w{w}';ids.append(cid)
            if family in ('hot_frequency','cold_frequency'):
                idx=np.arange(warm,N)
                # Prefix at t includes exactly outcomes [0,t), never target t.
                counts=prefix[idx]-prefix[idx-w]
                probs=(counts+1)/(w+10) if family=='hot_frequency' else (counts.max(axis=1,keepdims=True)+1-counts)
                probs=probs/probs.sum(axis=1,keepdims=True)
                picks=np.argsort(-probs,axis=1,kind='stable')[:,:3]
                confidence=np.take_along_axis(probs,picks,axis=1).sum(axis=1)
            elif family=='lag_neighbors':
                y=a[warm-1:N-1,pos];picks=np.stack([(y-1)%10,y,(y+1)%10],axis=1);confidence=np.zeros(N-warm)
            else:
                picks=np.tile([0,1,2],(N-warm,1));confidence=np.zeros(N-warm)
            tie=np.array([int.from_bytes(hashlib.sha256(f'{cid}:{t}'.encode()).digest()[:8],'big')/2**64 for t in range(warm,N)])
            score=confidence+tie*1e-9
            threshold=float(np.quantile(score[:end-warm],0.85))
            trade=score>=threshold
            hit=(picks==a[warm:,pos,None]).any(axis=1)
            all_masks[cid]=picks;all_trades[cid]=trade;thresholds[cid]=threshold
            row={'id':cid,'family':family,'position':pos+1,'window':w,'frozen_threshold':threshold}
            for label,sl in [('select',slice(0,end-warm)),('holdout',slice(end-warm,N-warm))]:
                acted=trade[sl]; h=hit[sl][acted];n=int(acted.sum());k=int(h.sum());lo,hi=wilson(k,n)
                row.update({f'{label}_calendar':len(acted),f'{label}_acts':n,f'{label}_hits':k,f'{label}_frequency':n/len(acted),f'{label}_hr':k/n if n else None,f'{label}_wilson_low':lo,f'{label}_wilson_high':hi,f'{label}_max_loss':longest_loss(h)})
            results.append(row)
    assert len(results)==SPEC['candidate_count']
    # Winner is selected exclusively on SELECT, before consulting HOLDOUT values.
    winner=sorted(results,key=lambda r:(-r['select_wilson_low'],r['id']))[0]
    cid=winner['id'];picks=all_masks[cid];trade=all_trades[cid]
    with (OUT/'all_candidates.csv').open('w',encoding='utf-8-sig',newline='') as f:
        w=csv.DictWriter(f,fieldnames=list(results[0]));w.writeheader();w.writerows(results)
    with (OUT/'frozen_champion_holdout_predictions.csv').open('w',encoding='utf-8-sig',newline='') as f:
        w=csv.writer(f);w.writerow(['role','issue','position','n1','n2','n3','act','outcome','hit','max_input_index','target_index'])
        for t in range(end,N):
            ix=t-warm;vals=(picks[ix]+1).tolist();outcome=int(a[t,winner['position']-1]+1)
            w.writerow(['HISTORICAL_REPLAY',rows[t]['issue'],winner['position'],*vals,int(trade[ix]),outcome,int(outcome in vals),t-1,t])
    sensitivity=[{'hit_probability_assumption':p,'acts':n,'longest_loss_at_most':6,'single_rule_pass_probability':streak_probability(p,n)} for p in SPEC['maximum_loss_streak_sensitivity']['p'] for n in SPEC['maximum_loss_streak_sensitivity']['n']]
    # Independent small checks against direct enumeration and exact edge cases.
    import itertools
    for n in range(1,10):
        exact=sum(.34**sum(x)*.66**(n-sum(x)) for x in itertools.product([0,1],repeat=n) if longest_loss(x)<=6)
        assert abs(exact-streak_probability(.34,n))<1e-12
    assert streak_probability(1,250)==1 and streak_probability(0,7)==0
    assert wilson(0,10)[0] >= -1e-15 and wilson(10,10)[1] <= 1+1e-15
    report={'scope':SPEC['scope'],'spec_sha256':hashlib.sha256((ROOT/'evidence/review_experiment/replay_spec.json').read_bytes()).hexdigest(),'input_csv_sha256':hashlib.sha256(raw).hexdigest(),'rows':N,'select_range':[rows[warm]['issue'],rows[end-1]['issue']],'holdout_range':[rows[end]['issue'],rows[-1]['issue']],'candidate_count':len(results),'selection_criterion':SPEC['champion'],'selected_champion':winner,'new_future_evidence':0,'real_bets':0,'production_code_tested':False,'no_claim_of_positive_edge':True,'limits':['Only 100 simple baseline specifications; not the existing app model implementations.','Historical prior-outcome replay is causal by array index but not a proof of historical device availability. All first_seen_at values are after draw time.','SELECT is optimized and biased. HOLDOUT was inspected once in this experiment, but these historical data may have been exposed in prior app research.','Wilson intervals are descriptive under Bernoulli assumptions; temporal dependence, drift, adaptive policy search and repeated tests require registered production evidence tests.','Other candidates HOLDOUT rows are for reproducibility; no reselection after reading them.'],'gate_streak_sensitivity':sensitivity,'reference_test_result':'PASS','reference_test_scope':'DP enumeration/edge cases and input integrity, not Android integration'}
    (OUT/'review_results.json').write_text(json.dumps(report,ensure_ascii=False,indent=2))
    # Frozen protocol bytes are immutable input; do not reformat or overwrite them.
    print(json.dumps({'selected_champion':winner,'input_rows':N,'gate_probability_p034_n250':streak_probability(.34,250),'scope':report['scope']},ensure_ascii=False,indent=2))

if __name__=='__main__':main()
