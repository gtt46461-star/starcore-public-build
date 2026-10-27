"""Auditable evidence utilities; no betting authorization, no fabricated future performance.
E-process condition: before each selected draw, E[Y_t|past] <= p0 under H0.
Family registration alpha_j = alpha_total/[j(j+1)]; sum <= alpha_total.
"""
from __future__ import annotations
from dataclasses import dataclass
import math
import numpy as np

class BernoulliEProcess:
    def __init__(self,p0:float=.30):
        if not 0<p0<1: raise ValueError('p0')
        self.p0=p0
        # Fixed mixture, chosen before seeing validation outcomes.
        self.lambdas=np.array([.05,.10,.20,.40,.80,1.20,1.60])/max(1.,1.60*p0)
        self.logs=np.zeros(len(self.lambdas));self.n=0;self.hits=0
    def update(self,y:int):
        if y not in (0,1): raise ValueError('binary outcome required')
        self.logs+=np.log1p(self.lambdas*(y-self.p0));self.n+=1;self.hits+=y
    @property
    def log_e(self):
        a=float(np.max(self.logs));return a+math.log(float(np.exp(self.logs-a).mean()))
    def crossed(self,alpha:float):
        if not 0<alpha<1: raise ValueError('alpha')
        return self.log_e>=math.log(1/alpha)

def registration_alpha(index:int,total:float=.05)->float:
    if not isinstance(index,int) or index<1 or not 0<total<1: raise ValueError('alpha registration')
    return total/(index*(index+1))

def cofailure_stats(a:np.ndarray,b:np.ndarray):
    """Aligned issue-level frozen 0/1 hit sequences; no independent-sample fiction."""
    a=np.asarray(a);b=np.asarray(b)
    if a.ndim!=1 or a.shape!=b.shape or len(a)==0 or not np.isin(a,[0,1]).all() or not np.isin(b,[0,1]).all():
        raise ValueError('aligned binary streams required')
    return {'n':len(a),'both_fail':int(((a==0)&(b==0)).sum()),'a_only_hit':int(((a==1)&(b==0)).sum()),
            'b_only_hit':int(((a==0)&(b==1)).sum()),'both_hit':int(((a==1)&(b==1)).sum())}

def streak_metrics(acted,hit):
    a=np.asarray(acted,dtype=bool);h=np.asarray(hit,dtype=bool)
    if a.ndim!=1 or h.shape!=a.shape: raise ValueError('shape')
    cur=best=0;n=int(a.sum());k=int((a&h).sum())
    for act,ok in zip(a,h):
        if not act: continue # pauses and skips NEVER reset a losing streak
        cur=0 if ok else cur+1;best=max(best,cur)
    return {'issues':len(a),'actions':n,'hits':k,'hit_rate':k/n if n else None,
            'coverage':n/len(a) if len(a) else None,'max_losing_streak':best}

@dataclass(frozen=True)
class ImprovementReceipt:
    parent_id:str
    child_id:str
    evidence_start:str
    evidence_end:str
    paired_issues:int
    parent_actions:int
    child_actions:int
    engineering_ok:bool
    quality_ok:bool
    new_evidence:bool
    paired_test_ok:bool
    behavior_changed:bool
    @property
    def verified_improvement(self):
        return (self.parent_id!=self.child_id and self.paired_issues>0 and self.parent_actions>0 and self.child_actions>0
                and all([self.engineering_ok,self.quality_ok,self.new_evidence,self.paired_test_ok,self.behavior_changed]))
