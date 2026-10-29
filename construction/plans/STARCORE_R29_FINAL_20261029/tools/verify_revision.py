#!/usr/bin/env python3
"""R29.1 deterministic contract checks. NOT Android execution or real prediction performance."""
import argparse, hashlib, json, math, sqlite3
from pathlib import Path

def calibration(p, y):
    if len(p) != len(y): raise ValueError('size mismatch')
    if not p: return {'status':'INSUFFICIENT','ece':None,'supported_mass':None,'occupied_bins':0}
    rows=[{'count':0,'sum_p':0.,'sum_y':0} for _ in range(10)]
    for a,b in zip(p,y):
        if not math.isfinite(a) or not 0<=a<=1 or b not in (0,1): raise ValueError('invalid sample')
        r=rows[min(9,int(a*10))];r['count']+=1;r['sum_p']+=a;r['sum_y']+=b
    n=len(p)
    ece=sum(abs(r['sum_p']-r['sum_y']) for r in rows)/n
    mass=sum(r['count'] for r in rows if r['count']>=20)/n
    return {'status':'MEASURED','n':n,'ece':ece,'supported_mass':mass,
            'occupied_bins':sum(r['count']>0 for r in rows),
            'old_v1_support_gate':sum(r['count']>=20 for r in rows)>=5,
            'v2_calibration_component_pass':n>=200 and mass>=.90 and ece<=.04+1e-12,
            'rows':rows,'qualifies_for_real':False}

def gap_sql_counterexample():
    # Exact hit-state predicates from LiveShadowsR27.settle, with a minimal table.
    db=sqlite3.connect(':memory:')
    db.execute('CREATE TABLE f(policy_hash TEXT,target_ordinal INTEGER,hit INTEGER, PRIMARY KEY(policy_hash,target_ordinal))')
    db.execute("INSERT INTO f VALUES('x',2,-1)")
    latest=5
    for policy,ordinal in db.execute('SELECT policy_hash,target_ordinal FROM f WHERE hit=-1 AND target_ordinal<=? ORDER BY target_ordinal,policy_hash LIMIT 640',(latest,)).fetchall():
        if ordinal<latest-2: db.execute('UPDATE f SET hit=-2 WHERE policy_hash=? AND target_ordinal=? AND hit=-1',(policy,ordinal))
    returned=db.execute('SELECT target_ordinal FROM f WHERE hit=-1 AND target_ordinal<=? ORDER BY target_ordinal,policy_hash LIMIT 640',(6,)).fetchall()
    assert returned==[]
    return {'scope':'SQL_PREDICATE_REPRODUCTION_NOT_ANDROID','missing_issue':2,'latest_at_expiry':5,'stored_hit':-2,'retry_query_rows_after_result_arrives':len(returned)}

def ordering_counterexample():
    # Faithful lastOrdinal guard demonstration; a reference inbox contract follows.
    legacy_last=-1;seen=[]
    for ordinal in [1,3,2]:
        if ordinal<=legacy_last:continue
        seen.append(ordinal);legacy_last=ordinal
    assert seen==[1,3]
    def consume(events):
        pending={};receipts={};cursor=0;ordered=[]
        for ordinal,hit in events:
            if ordinal in receipts:
                if receipts[ordinal]!=hit:raise ValueError('conflict')
                continue
            if ordinal in pending and pending[ordinal]!=hit:raise ValueError('conflict')
            pending[ordinal]=hit
            while cursor+1 in pending:
                cursor+=1;receipts[cursor]=pending.pop(cursor);ordered.append((cursor,receipts[cursor]))
        return ordered,cursor,pending
    sequential=consume([(1,1),(2,0),(3,1)])
    unordered=consume([(1,1),(3,1),(3,1),(2,0),(2,0)])
    assert unordered==sequential
    assert consume([(1,1),(3,1)])[1]==1
    try:consume([(1,1),(1,0)])
    except ValueError:conflict=True
    else:conflict=False
    assert conflict
    return {'scope':'CONTROL_FLOW_AND_PROPOSED_INBOX_MODEL_NOT_ANDROID','arrival_order':[1,3,2],
            'legacy_consumed':seen,'proposed_consumed':[x[0] for x in unordered[0]],
            'same_as_ordered':True,'duplicates_counted_once':True,'conflict_rejected':True,
            'restart_atomicity_device_test':'NOT_RUN'}

def run(root):
    d=json.loads((root/'registries/r29_policy_defaults.json').read_text())
    assert d['policy_version']=='R29_GATE_V2'
    assert 'minimum_populated_bins' not in d['qualification']['calibration']
    c=calibration([.34]*200,[1]*68+[0]*132)
    assert abs(c['ece'])<1e-12 and not c['old_v1_support_gate'] and c['v2_calibration_component_pass']
    bad=calibration([.80]*200,[1]*68+[0]*132)
    assert not bad['v2_calibration_component_pass']
    sparse=calibration([.34]*181+[.85]*19,[1]*61+[0]*120+[0]*19)
    assert sparse['supported_mass']>=.90 and sparse['ece']>.04 and not sparse['v2_calibration_component_pass']
    empty=calibration([],[]);assert empty['ece'] is None
    low=calibration([.34]*10,[1]*3+[0]*7);assert not low['v2_calibration_component_pass']
    metrics=json.loads((root/'registries/acceptance_metrics.json').read_text())
    q02=next(x for x in metrics if x['id']=='Q02');assert not q02['admission_prerequisite']
    return {'scope':'DETERMINISTIC_PLAN_CONTRACT_COUNTEREXAMPLES_ONLY','result':'PASS',
            'calibration_constant_probability':c,'miscalibration':bad,'sparse_errors_retained':sparse,
            'empty':empty,'small_sample_blocked':True,'gap_sql':gap_sql_counterexample(),
            'ordering':ordering_counterexample(),'Q02_pre_activation':q02['pre_activation_status'],
            'device_validation':'NOT_RUN','natural_quality':'NOT_RUN','app_source_modified':False}

if __name__=='__main__':
    parser=argparse.ArgumentParser();parser.add_argument('--out',type=Path,required=True);args=parser.parse_args()
    result=run(Path(__file__).resolve().parents[1]);args.out.parent.mkdir(parents=True,exist_ok=True)
    args.out.write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n')
    print(json.dumps({'result':result['result'],'scope':result['scope'],'output':str(args.out)},ensure_ascii=False))
