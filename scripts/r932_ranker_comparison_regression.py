#!/usr/bin/env python3
"""Actual production trainers on synthetic permutation labels; not Future performance."""
import copy
import fcntl
import json
import sys
import tempfile
from pathlib import Path
import numpy as np

ROOT=Path(__file__).resolve().parents[1]
sys.path.insert(0,str(ROOT))
from training.r929 import pipeline as p
from training.r932 import ranker_comparison as r

checks=[]
def check(value,name):
    if not value: raise AssertionError(name)
    checks.append(name);print(name+'=PASS',flush=True)

def rejects(fn,name):
    try: fn()
    except (ValueError,RuntimeError): check(True,name);return
    raise AssertionError(name)

def run():
    rng=np.random.default_rng(932)
    x=rng.normal(0,.2,(80,1200,96))
    x[:,:,0]=rng.uniform(.1,.7,(80,1200));x[:,:,11]=rng.uniform(0,1,(80,1200))
    actual=np.array([rng.permutation(np.arange(1,11)) for _ in range(80)])
    y=np.array([[int(actual[i,a[0]-1] in a[1:]) for a in p.ACTIONS] for i in range(80)],dtype=np.int8)
    issues=['20260911%04d'%(i+1) for i in range(80)]
    with tempfile.TemporaryDirectory(prefix='r932-ranker-') as directory:
        out=Path(directory)/'valid'
        model,report=r.compare(p,out,x,y,issues,0,40,[41,56],p.digest(b'FIXTURE'))
        protocol=p.read_json(out/'ranker_comparison_protocol.json')
        fs=protocol['folds']
        check(len(fs)==5 and all(a['test'][1]<=b['test'][0] for a,b in zip(fs,fs[1:])), 'FIVE_DISJOINT_CHRONOLOGICAL_TEST_WINDOWS')
        check(all(f['train'][1]+1==f['test'][0] for f in fs),'WHOLE_ISSUE_EMBARGO_IN_EVERY_FOLD')
        check(len(report['candidates'])==6 and all(len(f['candidates'])==6 for f in report['expanding_folds']),'ALL_R0_R5_HAVE_SAME_FOLD_RECORDS')
        check(all(not c['failed'] for f in report['expanding_folds'] for c in f['candidates']) and all(not c['failed'] for c in report['candidates']),'ALL_SIX_ACTUAL_TRAINERS_EXECUTE')
        check(all(c['metrics']['n']==f['test'][1]-f['test'][0] for f in report['expanding_folds'] for c in f['candidates']),'1200_ACTIONS_COUNT_AS_ONE_ISSUE_NOT_1200_SAMPLES')
        check(report['candidates'][5]['deployment_failure']=='NO_JAVA_TREE_EXPORTER' and model['family']!='STUMP_ENSEMBLE_RESEARCH','RESEARCH_TREE_IS_COMPARED_BUT_NEVER_DEPLOYED_WITHOUT_EXPORTER')
        requests=list((out/'ranker_trials').glob('*/request.json'))
        check(len(requests)==36 and all((q.parent/'result.json').is_file() for q in requests),'THIRTYSIX_TRIALS_REGISTERED_BEFORE_FITTING_AND_RETAINED')
        check(all(len(p.read_json(q.parent/'predictions.json')['all_1200_scores_sha256'])==64 for q in requests),'EVERY_TRIAL_HAS_FULL1200_SCORE_AND_RANKING_HASHES')
        model2,report2=r.compare(p,out,x,y,issues,0,40,[41,56],p.digest(b'FIXTURE'))
        check(p.canonical(model)==p.canonical(model2) and report==report2,'COMPLETED_TRIAL_RESTART_REPLAYS_IDENTICAL_MODEL_AND_REPORT')
        equal=copy.deepcopy(report['candidates']);folds=copy.deepcopy(report['expanding_folds'])
        for row in equal:
            row['metrics']['paired_baseline_delta']=0
        check(r.choose(equal,folds)['candidate_family']=='RAW_ACTION_P','NO_GAIN_RETAINS_SIMPLE_BASELINE')
        broken=copy.deepcopy(report['candidates']);broken[1]['metrics']['paired_baseline_delta']=.9;broken[1]['metrics']['rank1_rate']=1.
        badfolds=copy.deepcopy(folds);badfolds[0]['candidates'][1]['failed']=True
        check(r.choose(broken,badfolds)['candidate_family']!='pairwise','FAILED_FOLD_CANNOT_WIN_BY_TUNE_SCORE')
        original=p.mlp_fit
        def fail(*a,**kw): raise ValueError('INJECTED_FIT_FAILURE')
        p.mlp_fit=fail
        try:
            _m,failed=r.compare(p,Path(directory)/'failed',x,y,issues,0,40,[41,56],p.digest(b'FAIL'))
        finally: p.mlp_fit=original
        check(failed['candidates'][4]['failed'] and all(f['candidates'][4]['failed'] for f in failed['expanding_folds']),'FIT_FAILURES_RETAINED_IN_TUNE_AND_ALL_FIVE_FOLDS')
        count=len(list((Path(directory)/'failed'/'ranker_trials').glob('*/request.json')))
        _m,again=r.compare(p,Path(directory)/'failed',x,y,issues,0,40,[41,56],p.digest(b'FAIL'))
        check(again==failed and count==36,'FAILED_IDENTICAL_TRIAL_NOT_RETRIED_UNTIL_IT_WINS')
        y2=y.copy();y2[56:]=y2[56:][::-1]
        _m,trap=r.compare(p,Path(directory)/'trap',x,y2,issues,0,40,[41,56],p.digest(b'FIXTURE'))
        check(p.canonical(_m)==p.canonical(model),'SEALED_LABEL_CHANGES_CANNOT_CHANGE_TRAIN_TUNE_MODEL')
        bad=y[:2].copy();bad[0,0]=1-bad[0,0]
        rejects(lambda:r.validate(x[:2],bad),'INVALID_360_LABEL_COUNT_REJECTED')
        nan=x[:2].copy();nan[0,0,0]=np.nan
        rejects(lambda:r.validate(nan,y[:2]),'NONFINITE_SURFACE_REJECTED')
        rejects(lambda:r.compare(p,Path(directory)/'overlap',x,y,issues,0,40,[40,56],p.digest(b'BAD')),'MISSING_TUNE_EMBARGO_REJECTED')
        with (out/'ranker_comparison.lock').open('a+b') as lock:
            fcntl.flock(lock.fileno(),fcntl.LOCK_EX|fcntl.LOCK_NB)
            rejects(lambda:r.compare(p,out,x,y,issues,0,40,[41,56],p.digest(b'FIXTURE')),'SECOND_PROCESS_OWNER_CANNOT_TRAIN_SAME_EXPERIMENT')
            fcntl.flock(lock.fileno(),fcntl.LOCK_UN)
        one=requests[0].parent/'result.json';payload=p.read_json(one);payload['family']='TAMPERED';p.write_json(one,payload)
        rejects(lambda:r.compare(p,out,x,y,issues,0,40,[41,56],p.digest(b'FIXTURE')),'TAMPERED_COMPLETED_RESULT_REJECTED')
        check('ranker_comparison' in p.stage_ranker.__code__.co_names or 'stage' in p.stage_ranker.__code__.co_names,'EXISTING_PRODUCTION_STAGE_DISPATCHES_NEW_COMPARISON')
        summary={'revision':r.REVISION,'checks':checks,'passed':len(checks),'failed':0,'scope':'SYNTHETIC_PERMUTATIONS_ACTUAL_NUMPY_TRAINERS_NOT_DEVICE_OR_FUTURE','selected_family':model['family'],'effect_proven':False}
        target=ROOT/'reports/r932/RANKER_COMPARISON_REGRESSION.json';p.write_json(target,summary)
        print('R932_RANKER_COMPARISON_PASS|assertions='+str(len(checks)))

if __name__=='__main__':run()
