"""Freeze a label-blind drift monitor on the declared earlier TUNE interval only.

The budget is empirical TUNE exceedance, not a distribution-free false-alarm guarantee.
No outcomes after tune_last, prediction qualification, or REAL authority are produced.
"""
from __future__ import annotations
import argparse, hashlib, json, math, sqlite3
from pathlib import Path

REVISION = "R82_FROZEN_DRIFT_TUNE_V1"
PROTOCOL = "R82_JS_RECENT20_PRECEDING_REFERENCE_LAPLACE.5_ISSUE_MAX10_STRICT_GREATER_V1"
PROTOCOL_HASH = hashlib.sha256(PROTOCOL.encode()).hexdigest()

def canonical(value): return json.dumps(value, sort_keys=True, separators=(",", ":"), ensure_ascii=False).encode()
def digest(value): return hashlib.sha256(canonical(value)).hexdigest()
def valid_issue(issue):
    import datetime
    if not isinstance(issue, str) or len(issue) != 12 or not issue.isdigit() or not 1 <= int(issue[8:]) <= 1440: return False
    try: return datetime.datetime.strptime(issue[:8], "%Y%m%d").strftime("%Y%m%d") == issue[:8]
    except ValueError: return False
def next_issue(issue):
    import datetime
    seq=int(issue[8:]);date=issue[:8]
    if seq==1440:date=(datetime.datetime.strptime(date,"%Y%m%d")+datetime.timedelta(days=1)).strftime("%Y%m%d");seq=0
    return date+f"{seq+1:04d}"
def js(short, reference):
    a=[(x+.5)/(sum(short)+5) for x in short];b=[(x+.5)/(sum(reference)+5) for x in reference]
    return sum(.5*(x*math.log(x/((x+y)/2))+y*math.log(y/((x+y)/2))) for x,y in zip(a,b))/math.log(2)

def fit_rows(rows, tune_first, tune_last, budget=.05):
    if not valid_issue(tune_first) or not valid_issue(tune_last) or tune_first>tune_last or not .001<=budget<=.25:raise ValueError("R82_DRIFT_TUNE_CONTRACT")
    if not rows or len(rows)>100000:raise ValueError("R82_DRIFT_TUNE_ROW_BOUND")
    previous=None
    for row in rows:
        if not valid_issue(row["issue"]) or row["issue"]>tune_last or previous is not None and next_issue(previous)!=row["issue"]:raise ValueError("R82_DRIFT_TUNE_GAP_OR_FUTURE")
        if sorted(row["digits"])!=list(range(1,11)):raise ValueError("R82_DRIFT_TUNE_REAL_DRAW_PERMUTATION_REQUIRED")
        if not isinstance(row.get("draw_hash"),str) or len(row["draw_hash"])!=64 or any(c not in '0123456789abcdef' for c in row['draw_hash']):raise ValueError("R82_DRIFT_TUNE_SOURCE_HASH_REQUIRED")
        previous=row["issue"]
    tune=[i for i,row in enumerate(rows) if tune_first<=row["issue"]<=tune_last]
    if len(tune)<120 or rows[tune[0]]["issue"]!=tune_first or rows[tune[-1]]["issue"]!=tune_last:raise ValueError("WAIT_R82_DRIFT_TUNE_SUPPORT120")
    prefix=[[[0]*10 for _ in range(10)]]
    for row in rows:
        current=[counts.copy() for counts in prefix[-1]]
        for pos,digit in enumerate(row['digits']):current[pos][digit-1]+=1
        prefix.append(current)
    comparisons=[]
    for reference in (120,300,1000):
        if tune[0]<reference+20:continue
        risks=[]
        for index in tune:
            risks.append(max(js([prefix[index][p][d]-prefix[index-20][p][d] for d in range(10)], [prefix[index-20][p][d]-prefix[index-20-reference][p][d] for d in range(10)]) for p in range(10)))
        ordered=sorted(risks);threshold=ordered[min(len(ordered)-1, math.ceil((1-budget)*len(ordered))-1)]
        for cooldown in (3,5,10):
            triggers=0;remaining=0
            for risk in risks:
                if remaining:remaining-=1;continue
                if risk>threshold:triggers+=1;remaining=cooldown
            comparisons.append({"reference_window":reference,"cooldown_issues":cooldown,"threshold_hex":threshold.hex(),"exceedance_n":sum(x>threshold for x in risks),"trigger_n":triggers,"tune_n":len(tune)})
    if not comparisons:raise ValueError("WAIT_R82_DRIFT_REFERENCE_SUPPORT")
    # The grid and tie rule are fixed; selection uses no current/future label.
    chosen=min(comparisons,key=lambda x:(abs(x['trigger_n']/x['tune_n']-budget),-x['reference_window'],-x['cooldown_issues']))
    body={"revision":REVISION,"protocol_hash":PROTOCOL_HASH,"tune_first":tune_first,"tune_last":tune_last,"tune_n":len(tune),"short_window":20,"reference_window":chosen['reference_window'],"cooldown_issues":chosen['cooldown_issues'],"threshold_hex":chosen['threshold_hex'],"false_alarm_budget_hex":float(budget).hex(),"tune_exceedance_n":chosen['exceedance_n'],"source_rows_hash":digest(rows),"source_max_issue":tune_last,"trial_grid_hash":digest(comparisons),"budget_interpretation":"EMPIRICAL_TUNE_ISSUE_MAX10_EXCEEDANCE_NOT_FALSE_ALARM_GUARANTEE"}
    body['policy_hash']=digest(body)
    return body,comparisons

def validate(body, latest_tune=None):
    copy=dict(body);expected=copy.pop('policy_hash',None)
    if expected!=digest(copy) or body.get('revision')!=REVISION or body.get('protocol_hash')!=PROTOCOL_HASH:raise ValueError('R82_DRIFT_POLICY_HASH_OR_PROTOCOL')
    if not valid_issue(body.get('tune_first')) or not valid_issue(body.get('tune_last')) or body['tune_first']>body['tune_last'] or body['source_max_issue']!=body['tune_last'] or latest_tune is not None and body['tune_last']>latest_tune:raise ValueError('R82_DRIFT_POLICY_TUNE_BOUNDARY')
    if body['tune_n']<120 or body['short_window']!=20 or body['reference_window'] not in (120,300,1000) or body['cooldown_issues'] not in (3,5,10):raise ValueError('R82_DRIFT_POLICY_SHAPE')
    threshold=float.fromhex(body['threshold_hex']);budget=float.fromhex(body['false_alarm_budget_hex'])
    if not math.isfinite(threshold) or not 0<=threshold<=1 or not .001<=budget<=.25 or not 0<=body['tune_exceedance_n']<=math.floor(budget*body['tune_n']+1e-9):raise ValueError('R82_DRIFT_POLICY_BUDGET')
    return body

def main():
    parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('--database',type=Path,required=True);parser.add_argument('--models',type=Path,required=True);parser.add_argument('--tune-first',required=True);parser.add_argument('--tune-last',required=True);parser.add_argument('--false-alarm-budget',type=float,default=.05);parser.add_argument('--output',type=Path,required=True);a=parser.parse_args()
    from training.r929 import pipeline as owner
    _features,_labels,issues,dataset=owner.load_dataset(a.models)
    if owner.file_digest(a.database)!=dataset['source_sha256']:raise ValueError('R82_DRIFT_SOURCE_NOT_FROZEN_DATASET_SNAPSHOT')
    ta,tb=dataset['splits']['tune']
    if a.tune_first<issues[ta] or a.tune_last>issues[tb-1]:raise ValueError('R82_DRIFT_OUTSIDE_FROZEN_DATASET_TUNE_SPLIT')
    configurations=[{'protocol_hash':PROTOCOL_HASH,'reference_window':ref,'cooldown_issues':cooldown,'false_alarm_budget_hex':float(a.false_alarm_budget).hex(),'tune_first':a.tune_first,'tune_last':a.tune_last} for ref in (120,300,1000) for cooldown in (3,5,10)]
    for config in configurations:owner.register_trial(dataset,'drift_monitor_r82',config,'REGISTERED_TUNE_ONLY')
    uri=a.database.resolve().as_uri()+'?mode=ro'
    with sqlite3.connect(uri,uri=True) as db:
        db.execute('PRAGMA query_only=ON');columns=','.join('n'+str(i) for i in range(1,11));rows=[{'issue':r[0],'digits':list(r[1:11]),'draw_hash':r[11]} for r in db.execute('SELECT issue,'+columns+',draw_hash_v2 FROM draws WHERE dirty=0 AND issue<=? ORDER BY issue',(a.tune_last,))]
    try:policy,comparison=fit_rows(rows,a.tune_first,a.tune_last,a.false_alarm_budget);validate(policy)
    except Exception:
        for config in configurations:owner.register_trial(dataset,'drift_monitor_r82',config,'FAILED_OR_WAIT_INPUT_RETAINED')
        raise
    actual={(row['reference_window'],row['cooldown_issues']) for row in comparison}
    for config in configurations:owner.register_trial(dataset,'drift_monitor_r82',config,'FROZEN_TUNE_GRID' if (config['reference_window'],config['cooldown_issues']) in actual else 'WAIT_REFERENCE_SUPPORT',policy['policy_hash'])
    a.output.parent.mkdir(parents=True,exist_ok=True);a.output.write_bytes(canonical(policy)+b'\n');a.output.with_suffix('.selection.json').write_bytes(canonical({'scope':'TUNE_ONLY_NO_DEVICE_OR_EFFECT_ACCEPTANCE','comparisons':comparison,'policy_hash':policy['policy_hash']})+b'\n');print(json.dumps({'status':'FROZEN_TUNE_POLICY','policy_hash':policy['policy_hash'],'tune_n':policy['tune_n'],'output':str(a.output)}))
if __name__=='__main__':main()
