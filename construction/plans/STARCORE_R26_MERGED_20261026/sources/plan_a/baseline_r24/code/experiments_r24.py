"""Fixed, disclosed research rechecks, not a live qualification. Never searches seeds.
1) Frozen R23 checkpoints on the 63 later, contiguous, unseen-by-checkpoint issues.
2) Fully disclosed retrospective online ablations of already exposed R23 predictions.
"""
from __future__ import annotations
import argparse,collections,csv,hashlib,json,sys
from pathlib import Path
import numpy as np
from audit_inputs_r24 import dump,ordinal,metrics

def sinkhorn(p,steps=30):
    q=np.maximum(np.asarray(p,dtype=np.float64),1e-12).copy()
    for _ in range(steps):
        q/=q.sum(1,keepdims=True);q/=q.sum(0,keepdims=True)
    return q/q.sum(1,keepdims=True)

def run(plan:Path,evidence:Path,out:Path):
    out.mkdir(parents=True,exist_ok=True);sys.path.insert(0,str(plan/'code'))
    import torch
    from mechanisms_r23 import FAMILIES,coherent_k3,load_checkpoint,parameter_hash
    torch.set_num_threads(1);torch.use_deterministic_algorithms(True)
    d=list(csv.DictReader((evidence/'draws_4836_sorted.csv').open()));ids=[x['issue'] for x in d]
    nums=np.array([[int(x[f'n{i}'])-1 for i in range(1,11)] for x in d],dtype=np.int64)
    orig=list(csv.DictReader((plan/'data/report_4743_draws.csv').open()))
    rows=[json.loads(s) for s in (plan/'results/replay/predraw_settlement.jsonl').read_text().splitlines()]
    ph=(plan/'results/replay/protocol.sha256').read_text().strip()
    models={};checks={}
    for name in FAMILIES:
        m,opt,through=load_checkpoint(plan/'results/replay'/(name+'.pt'),ph,len(orig));m.eval();models[name]=m
        checks[name]={'trained_through':through,'weights_hash':parameter_hash(m),'checkpoint_sha256':hashlib.sha256((plan/'results/replay'/(name+'.pt')).read_bytes()).hexdigest()}
    eligibility=json.loads((evidence/'data_integrity.json').read_text());targets=eligibility['extension_eligible_32_contiguous']
    assert len(targets)==63 and all(ids.index(x)>=len(orig) for x in targets)
    x=torch.tensor(np.stack([nums[ids.index(i)-32:ids.index(i)] for i in targets]))
    with torch.no_grad():probs={k:torch.softmax(m(x),-1).numpy() for k,m in models.items()}
    probs['equal_marginal_mixture']=np.mean(np.stack(list(probs.values())),axis=0)
    extension=[]
    for name in probs:
        past=[r['raw_probability'] for r in rows if r['policy']==name][-500:]
        for i,issue in enumerate(targets):
            t=ids.index(issue);p,ns,conf=coherent_k3(probs[name][i]);thr=float(np.quantile(past[-500:],.85))
            extension.append({'issue':issue,'policy':name,'position':p+1,'numbers':[a+1 for a in ns],'raw_probability':conf,'threshold':thr,'research_act':bool(conf>=thr),'settled_hit':int(nums[t,p] in ns),'checkpoint_train_through_index':len(orig)-1,'input_through_issue':ids[t-1],'scope':'RETROSPECTIVE_FROZEN_CHECKPOINT_EXTENSION_NOT_LIVE','real_authorized':False})
            past.append(conf)
    (out/'extension_actions.jsonl').write_text('\n'.join(json.dumps(r,ensure_ascii=False) for r in extension)+'\n')
    res={k:metrics([r for r in extension if r['policy']==k]) for k in probs}
    dump(out/'frozen_extension.json',{'protocol':'No retraining, no seed search, same K3/15%-quantile gate, all five models disclosed; gap rows excluded. This protocol was authored after prior report inspection, not prospectively registered.','input_checkpoint_checks':checks,'targets':targets,'excluded':eligibility['extension_excluded_32_gap'],'results':res,'fresh_oos_release':False,'external_real_execution':False})
    # Retrospective ablations on identical 2243 previously exposed issues.
    z=np.load(plan/'results/replay/marginals.npz');ps={k:z[k] for k in z.files};orig_records=[r for r in rows if r['policy']=='equal_marginal_mixture'];Y=np.array([[int(orig[r['sequence']][f'n{i}'])-1 for i in range(1,11)] for r in orig_records])
    names=['A0_equal_original','A1_equal_permutation_projection','A2_past_NLL_adaptive_mixture','A3_adaptive_projection_calibrated_gate']
    logits=np.zeros(4);past={k:[] for k in names};cal=collections.deque(maxlen=800);stream={k:[] for k in names}
    projection_change=0
    for t,orig_rec in enumerate(orig_records):
        base=np.array([ps[k][t] for k in FAMILIES]);w=np.exp(logits-logits.max());w/=w.sum()
        eq=ps['equal_marginal_mixture'][t];mix=np.einsum('k,kij->ij',w,base);proj=sinkhorn(mix)
        prob={'A0_equal_original':eq,'A1_equal_permutation_projection':sinkhorn(eq),'A2_past_NLL_adaptive_mixture':mix,'A3_adaptive_projection_calibrated_gate':proj}
        proposed=[]
        for name,pred in prob.items():
            p,ns,raw=coherent_k3(pred)
            if name=='A0_equal_original':conf=raw;act=bool(orig_rec['research_act']);thr=orig_rec['threshold']
            elif name=='A3_adaptive_projection_calibrated_gate':
                # Local score-bin calibration uses only earlier labels and fixed shrinkage.
                b=int(raw/.025);prior=[h for cb,h in cal if cb==b];conf=(sum(prior)+30*.3)/(len(prior)+30)
                thr=.35;act=len(prior)>=120 and conf>=thr
                proposed.append((b,int(Y[t,p] in ns)))
            else:
                conf=raw;thr=float(np.quantile(past[name][-500:],.85)) if len(past[name])>=120 else 2.;act=conf>=thr
            stream[name].append({'issue':orig_rec['issue'],'policy':name,'position':p+1,'numbers':[a+1 for a in ns],'raw_probability':float(conf),'marginal_probability':float(raw),'threshold':float(thr),'research_act':bool(act),'settled_hit':int(Y[t,p] in ns),'scope':'EXPOSED_HISTORY_RESEARCH_NOT_NEW_OOS','real_authorized':False})
            past[name].append(raw)
        if stream[names[0]][-1]['numbers']!=stream[names[1]][-1]['numbers'] or stream[names[0]][-1]['position']!=stream[names[1]][-1]['position']:projection_change+=1
        # Loss and calibration updates ONLY after all current actions have been frozen.
        loss=-np.log(np.maximum(base[:,np.arange(10),Y[t]],1e-12)).mean(1)
        logits=.995*logits-.5*(loss-loss.mean());cal.extend(proposed)
    for name in names:(out/(name+'.jsonl')).write_text('\n'.join(json.dumps(r,ensure_ascii=False) for r in stream[name])+'\n')
    # A0 preserves original gate; A1/A2 have 120-period cold-start. Also report common tail.
    abl={'protocol':{'all_variants':names,'fixed_hyperparameters':{'sinkhorn_steps':30,'expert_eta':.5,'forget':.995,'calibration_bin_width':.025,'calibration_prior_count':30,'calibration_prior_mean':.3,'minimum_bin_labels':120,'calibrated_gate':.35},'scope':'RETROSPECTIVE_RESEARCH_ALREADY_EXPOSED','no_hyperparameter_search':True,'not_proof_of_deployment_improvement':True,'common_comparison_drop':120},'all_periods':{k:metrics(v) for k,v in stream.items()},'common_tail':{k:metrics(v[120:]) for k,v in stream.items()},'projection_changes_action_count':projection_change}
    dump(out/'retrospective_ablations.json',abl)
    print(json.dumps({'extension':res,'ablations_common_tail':abl['common_tail']},ensure_ascii=False,indent=2))
if __name__=='__main__':
    a=argparse.ArgumentParser();a.add_argument('--plan',type=Path,required=True);a.add_argument('--evidence',type=Path,required=True);a.add_argument('--out',type=Path,required=True);x=a.parse_args();run(x.plan,x.evidence,x.out)
