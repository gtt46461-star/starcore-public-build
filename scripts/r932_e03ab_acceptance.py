#!/usr/bin/env python3
from __future__ import annotations
import argparse
import json
import os
import shutil
import sqlite3
import subprocess
import sys
import tempfile
from pathlib import Path

R=Path(__file__).resolve().parents[1]
if str(R) not in sys.path: sys.path.insert(0,str(R))


def manifest(source='a'*64,feature='b'*64,split='c'*64):
    return {'game':'Binance1-PK10','schema':'R929_CANONICAL_1200_META96','source_sha256':source,
            'feature_manifest_sha256':feature,'split_sha256':split,'splits':{'sealed':[6,10]}}

def issues(): return ['20260910%04d'%i for i in range(1,13)]

def sealed_worker(root,config):
    from training.r932.registry import consume
    try:
        value=consume(manifest(),issues(),{'family':config,'selector':'fixed'},Path(root))
        print('OK '+value['token'])
    except Exception as error:
        print('ERR '+type(error).__name__+':'+str(error))

def trial_worker(root,status):
    os.environ['STARCORE_EXPERIMENT_REGISTRY']=root
    from training.r929.pipeline import register_trial
    value=register_trial(manifest(),'ranker',{'family':'concurrent'},status)
    print('OK '+value)

def expect(ok,name):
    if not ok: raise AssertionError(name)
    print(name+'=PASS')

def main():
    parser=argparse.ArgumentParser();parser.add_argument('--sealed-worker',nargs=2);parser.add_argument('--trial-worker',nargs=2)
    args=parser.parse_args()
    if args.sealed_worker: sealed_worker(*args.sealed_worker); return
    if args.trial_worker: trial_worker(*args.trial_worker); return

    from training.r929 import pipeline
    from training.r932.registry import consume
    from training.r932.trial_registry import attempts

    work=R/'build/r932-e03ab-acceptance'
    if work.exists(): shutil.rmtree(work)
    work.mkdir(parents=True)
    sealed=work/'sealed';trial=work/'trials';os.environ['STARCORE_EXPERIMENT_REGISTRY']=str(trial)

    # E03B: two distinct experiments racing for the exact same SEALED issues: exactly one wins.
    commands=[[sys.executable,__file__,'--sealed-worker',str(sealed),family] for family in ('A','B')]
    procs=[subprocess.Popen(c,stdout=subprocess.PIPE,stderr=subprocess.STDOUT,text=True) for c in commands]
    outputs=[p.communicate(timeout=30)[0].strip() for p in procs]
    expect(sum(v.startswith('OK ') for v in outputs)==1 and sum(v.startswith('ERR ') for v in outputs)==1,
           'CONCURRENT_DISTINCT_SEALED_WRITERS_EXACTLY_ONE_WINNER')
    winner='A' if outputs[0].startswith('OK ') else 'B';token=outputs[0].split()[-1] if outputs[0].startswith('OK ') else outputs[1].split()[-1]

    replay=consume(manifest(),issues(),{'family':winner,'selector':'fixed'},sealed)
    expect(replay['token']==token,'SAME_CONTENT_REPLAY_IS_IDEMPOTENT_NOT_SECOND_SEALED_READ')
    renamed=dict(manifest());renamed['source_path']='/different/path/copy.jsonl';renamed['archive_name']='renamed-family.zip'
    expect(consume(renamed,issues(),{'family':winner,'selector':'fixed'},sealed)['token']==token,
           'COPY_RENAME_ARCHIVE_METADATA_CANNOT_RESET_SEALED_IDENTITY')
    try:
        consume(manifest(),issues(),{'family':'RENAMED_SPLIT_ATTEMPT','selector':'fixed'},sealed);raised=False
    except RuntimeError: raised=True
    expect(raised,'CHANGED_FAMILY_OR_SELECTION_CANNOT_REUSE_CONSUMED_SEALED_ISSUES')

    changed=manifest(source='d'*64)
    try: consume(changed,issues(),{'family':'upstream-change','selector':'fixed'},sealed);raised=False
    except RuntimeError: raised=True
    expect(raised,'UPSTREAM_CONTENT_CHANGE_INVALIDATES_OLD_SEALED_AUTHORITY_NOT_REFUND_WINDOW')
    base_id,_=pipeline.experiment_identity(manifest(),'ranker',{'family':'x'})
    for changed_manifest,name in ((manifest(source='d'*64),'SOURCE'),(manifest(feature='e'*64),'FEATURE'),(manifest(split='f'*64),'SPLIT')):
        changed_id,_=pipeline.experiment_identity(changed_manifest,'ranker',{'family':'x'})
        expect(changed_id!=base_id,'UPSTREAM_'+name+'_HASH_INVALIDATES_DOWNSTREAM_TRIAL_IDENTITY')

    # Leakage traps use the actual production dataset validator and whole-issue embargo splitter.
    action_rows=[]
    zeros=[0.0]*pipeline.FEATURE_COUNT
    for action in pipeline.ACTIONS:
        action_rows.append([int(action[0]),int(action[1]),int(action[2]),int(action[3]),zeros])
    row={'target_issue':'202609100002','max_source_issue':'202609100001','frozen_at':100,'draw_at':200,
         'actual':list(range(1,11)),'actions':action_rows,'feature_manifest_sha256':'b'*64,
         'snapshot_hash':'1'*64,'parent_bundle_hash':'2'*64,'selection_contract_hash':'3'*64,'record_hash':'4'*64}
    expect(pipeline.validate_row(row,'',None)=='b'*64,'VALID_PREDRAW_ROW_ACCEPTED')
    bad=dict(row);bad['max_source_issue']=bad['target_issue']
    try:pipeline.validate_row(bad,'',None);raised=False
    except ValueError as error:raised='PIT_SOURCE_NOT_BEFORE_TARGET' in str(error)
    expect(raised,'FUTURE_OR_TARGET_LABEL_SOURCE_LEAKAGE_TRAP_BLOCKED')
    bad=dict(row);bad['frozen_at']=bad['draw_at']
    try:pipeline.validate_row(bad,'',None);raised=False
    except ValueError as error:raised='POSTDRAW_FEATURE_FREEZE' in str(error)
    expect(raised,'POSTDRAW_FEATURE_LEAKAGE_TRAP_BLOCKED')
    splits=pipeline.chronological_splits(120,embargo=1);ordered=['train','tune','calibration','policy','sealed']
    expect(all(splits[ordered[i]][0]>splits[ordered[i-1]][1] for i in range(1,len(ordered))),
           'ALL_NONTRAIN_WINDOWS_HAVE_WHOLE_ISSUE_EMBARGO')

    # E03B: every state change is a new immutable attempt row, including failures/interruption.
    trial_id=pipeline.register_trial(manifest(),'ranker',{'family':'retained'},'PRE_REGISTERED')
    pipeline.register_trial(manifest(),'ranker',{'family':'retained'},'FAILED')
    pipeline.register_trial(manifest(),'ranker',{'family':'retained'},'INTERRUPTED')
    rows=attempts(trial,trial_id)
    expect([r[1] for r in rows]==['PRE_REGISTERED','FAILED','INTERRUPTED'],'FAILED_AND_INTERRUPTED_TRIAL_ATTEMPTS_ARE_ALL_RETAINED')
    db=sqlite3.connect(trial/'trial_registry_r932.sqlite3')
    try:
        try:db.execute("UPDATE attempt SET status='ERASED' WHERE trial_id=?",(trial_id,));db.commit();raised=False
        except sqlite3.DatabaseError:raised=True;db.rollback()
        expect(raised,'TRIAL_ATTEMPT_HISTORY_CANNOT_BE_REWRITTEN')
        try:db.execute("DELETE FROM attempt WHERE trial_id=?",(trial_id,));db.commit();raised=False
        except sqlite3.DatabaseError:raised=True;db.rollback()
        expect(raised,'TRIAL_ATTEMPT_HISTORY_CANNOT_BE_DELETED')
    finally:db.close()

    # Concurrent attempt writers all survive; no last-write-wins status loss.
    workers=[subprocess.Popen([sys.executable,__file__,'--trial-worker',str(trial),'WORKER_%d'%i],stdout=subprocess.PIPE,stderr=subprocess.STDOUT,text=True) for i in range(4)]
    out=[p.communicate(timeout=30)[0].strip() for p in workers]
    expect(all(v.startswith('OK ') for v in out),'CONCURRENT_TRIAL_WRITERS_ALL_COMMIT_DISTINCT_ATTEMPTS')
    concurrent_id=out[0].split()[-1];crow=attempts(trial,concurrent_id)
    expect(len(crow)==4 and {r[1] for r in crow}=={'WORKER_0','WORKER_1','WORKER_2','WORKER_3'},
           'CONCURRENT_TRIAL_ATTEMPTS_RETAINED_WITHOUT_LAST_WRITE_WINS_LOSS')

    result={'status':'PASS','scope':'E03A_E03B_HOST_ENGINEERING_NOT_DEVICE_NOT_FUTURE_EFFECT',
            'sealed_race_outputs':outputs,'trial_attempts':len(rows),'concurrent_attempts':len(crow),
            'raw_upstream_reference':'reports/r932/RAW_UPSTREAM.txt','raw_upstream_required_issues':100,
            'device_verified':False,'future_effect_proven':False}
    (R/'reports/r932/E03AB_ACCEPTANCE.json').write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    print('R932_E03AB_ACCEPTANCE_PASS|sealed_one_winner=1|failed_attempts_retained=1|device=false|future_effect=false')

if __name__=='__main__': main()
