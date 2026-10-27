"""Fixed-recipe, predict-before-update research replay. No best-run selection.
All inputs are past draw permutations. This is historical research, NOT fresh OOS.
"""
from __future__ import annotations
import argparse,csv,hashlib,json,platform,time
from pathlib import Path
import numpy as np
import torch
from torch.nn import functional as F
from mechanisms_r23 import FAMILIES,TemporalNet,coherent_k3,parameter_hash,save_checkpoint
from evidence_r23 import streak_metrics

def run(data:Path,out:Path):
    out.mkdir(parents=True,exist_ok=True)
    torch.set_num_threads(1);torch.manual_seed(2301);np.random.seed(2301)
    torch.use_deterministic_algorithms(True)
    rows=list(csv.DictReader(data.open(encoding='utf-8')))
    d=np.array([[int(r[f'n{i}'])-1 for i in range(1,11)] for r in rows],dtype=np.int64)
    if len(d)<3000 or not np.all(np.sort(d,axis=1)==np.arange(10)): raise ValueError('invalid history')
    if len(set(r['issue'] for r in rows))!=len(rows): raise ValueError('duplicate issue')
    L=32;train_end=2000;score_start=2500;block=64
    protocol={'schema':'R23_FIXED_RECIPE_1','dataset_sha256':hashlib.sha256(data.read_bytes()).hexdigest(),
        'rows':len(d),'history_scope':'PREVIOUSLY_EXPOSED_HISTORICAL_RESEARCH','seed':2301,'window':L,
        'train_end_exclusive':train_end,'calibration_end_exclusive':score_start,'evaluation_end_exclusive':len(d),
        'families':list(FAMILIES),'primary':'equal_marginal_mixture','width':32,'initial_epochs':4,
        'batch_size':64,'lr':.0005,'weight_decay':.001,'update_every_labels':64,'steps_per_update':8,
        'replay_history_max':1024,'gate':'past_score_85_percentile_500_ONLY_RESEARCH_NOT_QUALITY_CERT',
        'variants_after_inspection':0,'production_authorization':False,'software':{'python':platform.python_version(),'torch':torch.__version__}}
    raw=json.dumps(protocol,sort_keys=True,ensure_ascii=False).encode();ph=hashlib.sha256(raw).hexdigest()
    (out/'protocol.json').write_bytes(raw);(out/'protocol.sha256').write_text(ph+'\n')
    # Materialization is causal by construction: row t contains draws [t-L,t), never d[t].
    X=torch.tensor(np.stack([d[t-L:t] for t in range(L,len(d))]));Y=torch.tensor(d[L:])
    models={k:TemporalNet(k) for k in FAMILIES}; initial={k:parameter_hash(m) for k,m in models.items()}
    opts={k:torch.optim.AdamW(m.parameters(),lr=.0005,weight_decay=.001) for k,m in models.items()}
    train_steps={k:0 for k in FAMILIES};rng=np.random.default_rng(2301)
    def train(ids):
        ids=torch.as_tensor(ids,dtype=torch.long)
        for k,m in models.items():
            m.train();opts[k].zero_grad();z=m(X[ids]);loss=F.cross_entropy(z.reshape(-1,10),Y[ids].reshape(-1))
            if not torch.isfinite(loss): raise ValueError('nonfinite loss')
            loss.backward();torch.nn.utils.clip_grad_norm_(m.parameters(),1);opts[k].step();train_steps[k]+=1
    for epoch in range(4):
        order=rng.permutation(train_end-L)
        for i in range(0,len(order),64): train(order[i:i+64])
    print('initial training complete',train_steps,flush=True)
    names=list(FAMILIES)+['equal_marginal_mixture']
    past={k:[] for k in names};streams={k:[] for k in names};probs={k:[] for k in names}
    through=train_end-1;log=out/'predraw_settlement.jsonl'
    with log.open('w',encoding='utf-8') as f:
        for st in range(train_end,len(d),block):
            end=min(st+block,len(d));xb=X[st-L:end-L]
            for m in models.values():m.eval()
            with torch.no_grad(): ps={k:F.softmax(m(xb),-1).numpy() for k,m in models.items()}
            ps['equal_marginal_mixture']=np.mean(np.stack(list(ps.values())),axis=0)
            state_hashes={k:parameter_hash(m) for k,m in models.items()}
            for j,t in enumerate(range(st,end)):
                assert through<t
                for k in names:
                    p,nums,conf=coherent_k3(ps[k][j]); threshold=float(np.quantile(past[k][-500:],.85)) if len(past[k])>=120 else float('inf')
                    act=conf>=threshold;hit=int(int(d[t,p]) in nums)
                    if t>=score_start:
                        rec={'issue':rows[t]['issue'],'sequence':t,'policy':k,'train_through':through,'input_through':t-1,
                            'position':p+1,'numbers':[n+1 for n in nums],'raw_probability':conf,'threshold':threshold,
                            'research_act':bool(act),'settled_hit':hit,'draw_number':int(d[t,p])+1,
                            'protocol_hash':ph,'evidence_scope':'HISTORY_REPLAY','external_execution':False,
                            'artifact_hash':state_hashes.get(k,hashlib.sha256(''.join(state_hashes.values()).encode()).hexdigest())}
                        f.write(json.dumps(rec,ensure_ascii=False,sort_keys=True)+'\n');streams[k].append(rec);probs[k].append(ps[k][j])
                    past[k].append(conf)
            # Now the block's labels are revealed. Update privately, then expose the next snapshot.
            lo=max(L,end-1024);available=np.arange(lo-L,end-L)
            for _ in range(8):train(rng.choice(available,size=min(64,len(available)),replace=False))
            through=end-1
    results={}
    for k,recs in streams.items():
        a=[r['research_act'] for r in recs];h=[r['settled_hit'] for r in recs]
        s=streak_metrics(a,h)
        s['raw_brier_selected']=float(np.mean([(r['raw_probability']-r['settled_hit'])**2 for r in recs if r['research_act']])) if any(a) else None
        s['raw_calibration_gap_selected']=float(np.mean([r['raw_probability'] for r in recs if r['research_act']])-s['hit_rate']) if any(a) else None
        s['quality_target_met']=bool(s['hit_rate'] is not None and s['hit_rate']>=.35 and s['coverage']>=.15 and s['max_losing_streak']<=6)
        s['fresh_evidence_qualified']=False
        s['blocks']=[streak_metrics(a[lo:hi],h[lo:hi]) for lo,hi in zip(np.linspace(0,len(a),5,dtype=int)[:-1],np.linspace(0,len(a),5,dtype=int)[1:])]
        if k in models:
            m=models[k];s['parameter_count']=sum(p.numel() for p in m.parameters());s['optimizer_steps']=train_steps[k]
            s['initial_hash']=initial[k];s['final_hash']=parameter_hash(m);s['parameters_changed']=initial[k]!=s['final_hash']
            save_checkpoint(out/(k+'.pt'),m,opts[k],through,ph)
        results[k]=s
    np.savez_compressed(out/'marginals.npz',**{k:np.array(v,dtype=np.float32) for k,v in probs.items()})
    result={'protocol_hash':ph,'primary':protocol['primary'],'results':results,'fresh_oos':False,
            'real_execution':False,'experimental_only':True,'log_sha256':hashlib.sha256(log.read_bytes()).hexdigest()}
    (out/'summary.json').write_text(json.dumps(result,ensure_ascii=False,indent=2))
    print(json.dumps(result,ensure_ascii=False,indent=2),flush=True)
if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('--data',type=Path,required=True);p.add_argument('--out',type=Path,required=True)
    a=p.parse_args();run(a.data,a.out)
