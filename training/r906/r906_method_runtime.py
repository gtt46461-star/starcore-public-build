#!/usr/bin/env python3
"""Pure-Python reference runtime for R906 light research families.

This module deliberately contains no post-draw access.  It consumes only the seven frozen
pre-draw features used by the Android shared matrix so golden parity is reproducible.
"""
from __future__ import annotations
import hashlib, math, re
from typing import Dict, List

SCHEMA = ["raw_action_p","rank_score","third_margin","entropy_risk","stability","window_consensus","assignment_support"]
MASK=(1<<64)-1

def sha256(s:str)->str: return hashlib.sha256(s.encode("utf-8")).hexdigest()
def norm(s:str)->str: return re.sub(r"\s+","_",(s or "").strip().upper())
def method_signature(plan:Dict[str,str])->str:
    parts=[plan[k] for k in ("representation","feature","learner","update_rule","objective","calibration","risk","falsifier")]
    return sha256("R906_METHOD_SIGNATURE|"+"|".join(norm(x) for x in parts))
def family(plan:Dict[str,str])->str:
    rep=norm(plan.get("representation","")); learner=norm(plan.get("learner",""))
    if "BAYES" in rep or "BAYES" in learner or "MARKOV" in learner:return "BAYESIAN_TRANSITION"
    if "RETRIEVAL" in rep or "RETRIEVAL" in learner or "KNN" in learner:return "METRIC_RETRIEVAL"
    if "FOREST" in rep or "FOREST" in learner:return "COMPACT_FOREST"
    return "LINEAR_GRAPH"
def _signed(x:int)->int: return x-(1<<64) if x&(1<<63) else x
def _urshift(x:int,n:int)->int: return (x&MASK)>>n
def java_seed(s:str)->int:
    h=1125899906842597
    for ch in s:
        h=_signed((31*h+ord(ch))&MASK)
    return h
def parameters(method_hash:str,fam:str)->List[float]:
    n=14 if fam=="METRIC_RETRIEVAL" else 27 if fam=="COMPACT_FOREST" else 8
    s=java_seed(method_hash+"|"+fam); out=[]
    for _ in range(n):
        s=_signed(((s*6364136223846793005)+1442695040888963407)&MASK)
        out.append((((_urshift(s,16)&0xffff)/65535.0)*2.0)-1.0)
    if fam=="METRIC_RETRIEVAL":
        for i in range(7): out[i]=clamp(0.5+out[i]*0.30,0,1)
    return out
def clamp(x,lo,hi):return max(lo,min(hi,float(x)))
def finite(x):
    try:x=float(x)
    except Exception:return 0.0
    return x if math.isfinite(x) else 0.0
def clamp01(x):return clamp(finite(x),0,1)
def centered(x):return (clamp01(x)-.5)*2
def sig(z):return 1/(1+math.exp(-clamp(z,-20,20)))
def vec(x):
    y=[finite(v) for v in list(x)[:7]]+[0.0]*7; y=y[:7]
    for i in (0,3,4,5,6):y[i]=clamp01(y[i])
    return y
def std(i,v):
    if i==1:return math.tanh(v/4)
    if i==2:return math.tanh(v*12)
    return centered(v)
def normalized(i,v):
    if i==1:return clamp01(.5+.5*math.tanh(v/4))
    if i==2:return clamp01(.5+.5*math.tanh(v*12))
    return clamp01(v)
def evaluate(plan:Dict[str,str],x)->float:
    fam=family(plan); mh=method_signature(plan); p=parameters(mh,fam); x=vec(x)
    if fam=="BAYESIAN_TRANSITION":
        prior=clamp(x[0],.02,.98); logodds=math.log(prior/(1-prior))
        ev=p[0]*centered(x[6])+p[1]*centered(x[5])+p[2]*clamp(x[2]*12,-1,1)+p[3]*centered(x[4])-abs(p[4])*centered(x[3])
        pseudo=4+abs(p[5])*12; return sig(logodds+(pseudo/(pseudo+18))*ev)
    if fam=="METRIC_RETRIEVAL":
        d2=0
        for i in range(7):
            scale=.30+.35*abs(p[7+i]); d=(normalized(i,x[i])-p[i])/scale; d2+=d*d
        sim=1/(1+math.sqrt(d2/7)); return clamp01(.60*sim+.30*clamp01(x[0])+.10*clamp01(x[4]))
    if fam=="COMPACT_FOREST":
        votes=0
        for t in range(9):
            a=(abs(round(p[t]*1000))+t)%7; b=(a+1+t*2)%7
            ta=clamp01(.5+.22*p[9+t]); tb=clamp01(.5-.18*p[18+t])
            left=normalized(a,x[a])>=ta; right=normalized(b,x[b])>=tb
            if (left and right) or (left and t%3==0) or (right and t%4==0):votes+=1
        return clamp01(.70*((votes+1)/11)+.30*clamp01(x[0]))
    z=p[0]+sum(p[i+1]*std(i,x[i]) for i in range(7)); return sig(z)

