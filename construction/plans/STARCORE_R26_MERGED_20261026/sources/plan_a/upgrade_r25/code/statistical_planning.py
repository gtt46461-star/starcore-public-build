"""Exact IID fixed-endpoint illustration only, not production qualification.
Includes no search, routing, stopping, dependency or calibration correction.
"""
import math,json
from pathlib import Path
def tails(n,p):
    probs=[math.exp(math.lgamma(n+1)-math.lgamma(k+1)-math.lgamma(n-k+1)+k*math.log(p)+(n-k)*math.log1p(-p)) for k in range(n+1)]
    out=[0.]*(n+2)
    for k in range(n,-1,-1):out[k]=out[k+1]+probs[k]
    return out
def no_run_probability(n,hit,limit=7):
    state=[1.]+[0.]*(limit-1)
    for _ in range(n):state=[sum(state)*hit]+[v*(1-hit) for v in state[:-1]]
    return sum(state)
rows=[]
for n in [250,500,1000]:
    null=tails(n,.30)
    for candidates in [1,24]:
        alpha=.05/candidates;k=next(k for k in range(n+1) if null[k]<=alpha)
        rows.append(dict(n=n,candidates=candidates,alpha=alpha,minimum_hits=k,minimum_rate=k/n,power_true_35=tails(n,.35)[k],power_true_40=tails(n,.40)[k]))
out={'scope':'IID_FIXED_ENDPOINT_ILLUSTRATION_NOT_STARCORE_EVIDENCE','rows':rows,'no_seven_loss_run_in_250_actions':{str(p):no_run_probability(250,p) for p in [.35,.4]},'minimum_actions':250,'expected_opportunities_at_15':250/.15,'cautions':['250 actions is not enough for all effect sizes and multiplicities','maxLoss<=6 is a finite-window target, not a lifetime guarantee','neither calculation certifies actual project strategies']}
dest=Path(__file__).parents[1]/'results/statistical_planning.json';dest.write_text(json.dumps(out,indent=2));print(json.dumps(out,indent=2))
