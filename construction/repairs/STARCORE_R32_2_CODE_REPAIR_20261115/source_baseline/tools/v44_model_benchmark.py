#!/usr/bin/env python3
"""Strict chronological multi-class benchmark for single-position fixed-K3.
No random split, no future feature, no target leakage. Models are offline challengers only.
"""
from pathlib import Path
import json, time, warnings
import numpy as np
import pandas as pd
warnings.filterwarnings('ignore')
ROOT=Path(__file__).resolve().parents[1]
CSV=ROOT/'research_data/ethpk10_draws_20260316_201059.csv'
OUT=ROOT/'reports/V44_MODEL_CHALLENGER_BENCHMARK_44157296.json'

def load_numbers():
    df=pd.read_csv(CSV)
    a=np.array([[int(x)-1 for x in str(s).split(',')] for s in df['open_number']],dtype=np.int16)
    assert a.shape[1]==10
    return a

def build_features(a, start=96):
    n=a.shape[0]; lags=[1,2,3,4,6,8,12,24]
    X=[]; y=[]; issue=[]; pos=[]
    eye=np.eye(10,dtype=np.float32)
    for t in range(start,n):
        prev=a[t-1]
        inv=np.empty(10,dtype=np.float32)
        for j,num in enumerate(prev): inv[num]=j/9.0
        for p in range(10):
            f=[p/9.0]
            f.extend((prev/9.0).tolist())
            f.extend([a[t-k,p]/9.0 for k in lags])
            for w in (12,24,48,96):
                counts=np.bincount(a[t-w:t,p],minlength=10).astype(np.float32)/float(w)
                f.extend(counts.tolist())
            hist=a[max(0,t-96):t,p]
            omissions=[]
            for num in range(10):
                idx=np.flatnonzero(hist==num)
                om=(len(hist)-1-idx[-1]) if len(idx) else len(hist)
                omissions.append(min(96,om)/96.0)
            f.extend(omissions)
            f.extend(inv.tolist())
            X.append(f); y.append(int(a[t,p])); issue.append(t); pos.append(p)
    return np.asarray(X,np.float32),np.asarray(y,np.int16),np.asarray(issue,np.int32),np.asarray(pos,np.int8)

def issue_predictions(model,X,issue,pos,classes=10):
    proba=model.predict_proba(X)
    # Feature construction is issue-major with exactly ten position rows per issue.
    uniq=np.unique(issue)
    if len(X)!=len(uniq)*10:
        raise RuntimeError("unexpected issue layout")
    pp=proba.reshape(len(uniq),10,10)
    yy=Y_GLOBAL[np.arange(len(Y_GLOBAL))] if len(Y_GLOBAL)==len(X) else None
    # Y_GLOBAL is the matching subset assigned by caller below through ACTIVE_Y.
    yy=ACTIVE_Y.reshape(len(uniq),10)
    top3=np.argpartition(pp, -3, axis=2)[:,:, -3:]
    conf=np.take_along_axis(pp,top3,axis=2).sum(axis=2)
    chosen=np.argmax(conf,axis=1)
    chosen_top=top3[np.arange(len(uniq)),chosen]
    actual=yy[np.arange(len(uniq)),chosen]
    hit=(chosen_top==actual[:,None]).any(axis=1).astype(np.int8)
    return conf[np.arange(len(uniq)),chosen],hit,chosen.astype(np.int8),uniq

def threshold_from_validation(conf,target):
    # threshold giving approximately target coverage, derived only from validation.
    return float(np.quantile(conf,max(0.0,min(1.0,1.0-target))))

def eval_at(conf,hit,thr):
    m=conf>=thr;n=int(m.sum());N=len(hit)
    return {'threshold':thr,'issues':N,'bets':n,'coverage':n/max(1,N),'hits':int(hit[m].sum()),'hit_rate':float(hit[m].mean()) if n else 0.0,'max_lose_streak':max_lose(hit[m])}

def max_lose(h):
    cur=best=0
    for x in h:
        if int(x)==1: cur=0
        else: cur+=1;best=max(best,cur)
    return int(best)

def main():
    global Y_GLOBAL, ACTIVE_Y
    t0=time.time();a=load_numbers();X,y,issues,positions=build_features(a);Y_GLOBAL=y
    # issue split: 6000 train, 2000 validation, remainder test.
    all_issues=np.unique(issues)
    train_end=all_issues[5999]; val_end=all_issues[7999]
    tr=issues<=train_end;va=(issues>train_end)&(issues<=val_end);te=issues>val_end
    models={}
    from sklearn.linear_model import LogisticRegression
    models['logistic_l2']=LogisticRegression(max_iter=160,C=.35,solver='lbfgs')
    try:
        from lightgbm import LGBMClassifier
        models['lightgbm']=LGBMClassifier(n_estimators=70,num_leaves=15,max_depth=5,learning_rate=.05,min_child_samples=60,subsample=.8,colsample_bytree=.65,reg_lambda=5.0,n_jobs=2,random_state=44,verbosity=-1)
    except Exception: pass
    result={'version':'V44_MODEL_CHALLENGER_44157296','rows':int(len(X)),'issues':int(len(all_issues)),'feature_count':int(X.shape[1]),'split':{'train_issues':6000,'validation_issues':2000,'test_issues':int(len(all_issues)-8000)},'models':{},'elapsed_sec':0}
    val_probs={};test_probs={}
    for name,m in models.items():
        s=time.time();m.fit(X[tr],y[tr]);
        ACTIVE_Y=y[va]; cv,hv,pv,iv=issue_predictions(m,X[va],issues[va],positions[va]);
        ACTIVE_Y=y[te]; ct,ht,pt,it=issue_predictions(m,X[te],issues[te],positions[te]);
        val_probs[name]=m.predict_proba(X[va]);test_probs[name]=m.predict_proba(X[te]);
        r={'fit_sec':round(time.time()-s,3),'all_test_hit_rate':float(ht.mean()),'all_test_max_lose':max_lose(ht),'targets':{}}
        for target in (.10,.15,.20):
            thr=threshold_from_validation(cv,target)
            r['targets'][str(int(target*100))]=eval_at(ct,ht,thr)
        result['models'][name]=r
        print(name,r,flush=True)
    # Equal-weight soft-vote across non-neural challengers; threshold remains validation-only.
    if val_probs:
        names=list(val_probs)
        class Proxy:
            def __init__(self,p):self.p=p
            def predict_proba(self,X):return self.p
        pv=sum(val_probs[n] for n in names)/len(names)
        pt=sum(test_probs[n] for n in names)/len(names)
        ACTIVE_Y=y[va]; cv,hv,_,_=issue_predictions(Proxy(pv),X[va],issues[va],positions[va])
        ACTIVE_Y=y[te]; ct,ht,_,_=issue_predictions(Proxy(pt),X[te],issues[te],positions[te])
        rr={'members':names,'all_test_hit_rate':float(ht.mean()),'all_test_max_lose':max_lose(ht),'targets':{}}
        for target in (.10,.15,.20):
            thr=threshold_from_validation(cv,target);rr['targets'][str(int(target*100))]=eval_at(ct,ht,thr)
        result['soft_vote']=rr
    result['elapsed_sec']=round(time.time()-t0,3)
    OUT.parent.mkdir(parents=True,exist_ok=True);OUT.write_text(json.dumps(result,ensure_ascii=False,indent=2),encoding='utf-8')
    print('saved',OUT)
if __name__=='__main__':main()
