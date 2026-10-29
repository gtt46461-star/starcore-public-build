#!/usr/bin/env python3
"""Exact IID Bernoulli gate calculation, NOT a performance estimate for real strategies."""
import argparse,json
from pathlib import Path

def probability_max_losses_at_most(p,n,limit=6):
    states=[1.0]+[0.0]*limit
    for _ in range(n):states=[sum(states)*p]+[q*(1-p) for q in states[:-1]]
    return sum(states)

if __name__=='__main__':
    a=argparse.ArgumentParser();a.add_argument('--out',type=Path,required=True);args=a.parse_args()
    assert probability_max_losses_at_most(1,100)==1
    assert probability_max_losses_at_most(0,7)==0
    assert abs(probability_max_losses_at_most(.5,7)-(1-.5**7))<1e-12
    o={'assumption':'Independent Bernoulli ACT outcomes with constant hit probability p; illustrative gate power only, not real data.',
       'method':'Exact Markov recursion over current losing-run lengths 0..6. Probability mass is discarded at seventh consecutive loss.',
       'rows':[{'p':p,'acts':n,'max_loss_limit':6,'pass_probability':probability_max_losses_at_most(p,n)} for p in [.30,.34,.37,.40,.45,.50] for n in [250,1000]],
       'does_not_authorize_gate_change':True}
    args.out.parent.mkdir(parents=True,exist_ok=True);args.out.write_text(json.dumps(o,ensure_ascii=False,indent=2)+'\n');print(json.dumps(o['rows'][:4]))
