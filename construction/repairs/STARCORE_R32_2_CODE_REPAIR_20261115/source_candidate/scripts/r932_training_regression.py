#!/usr/bin/env python3
"""Cross-language frozen input and global SEALED regression; no model efficacy claim."""
import copy,json,os,sys,tempfile,concurrent.futures
from pathlib import Path
import numpy as np
ROOT=Path(__file__).resolve().parents[1];sys.path.insert(0,str(ROOT))
from training.r929 import pipeline,deep_candidates
from training.r932.registry import consume
lines=[]
def check(value,name):
    if not value:raise AssertionError(name)
    lines.append(name+'=PASS')
def reject(fn,name):
    try:fn()
    except (ValueError,RuntimeError):check(True,name);return
    raise AssertionError(name+' accepted')
def rehash(row):
    payload=json.dumps(row,ensure_ascii=False,separators=(',',':'))
    return ['R932_UPSTREAM_RAW='+payload,'R932_UPSTREAM_SHA256='+pipeline.digest(payload.encode())]
def main():
    source=ROOT/'build/r932-model-bundle/raw_upstream.jsonl'
    exported=[json.loads(line)for line in source.read_text().splitlines()]
    arrays=[pipeline.decode_raw_upstream(row)for row in exported]
    check(len(arrays)>=100 and all(a['number_features'].shape==(10,10,18) for a in arrays),'JAVA_TO_PYTHON_100_ISSUE_DIRECT_TENSORS')
    row=copy.deepcopy(exported[0]);row['proofs'][0]+=' ';reject(lambda:pipeline.decode_raw_upstream(row),'RAW_BYTE_HASH_TAMPER_REJECTED')
    row=copy.deepcopy(exported[0]);raw=json.loads(row['proofs'][0].split('=',1)[1]);raw['expert_ids'][0],raw['expert_ids'][1]=raw['expert_ids'][1],raw['expert_ids'][0];row['proofs']=rehash(raw)
    reject(lambda:pipeline.decode_raw_upstream(row),'SAME_WIDTH_EXPERT_ORDER_DRIFT_REJECTED')
    row=copy.deepcopy(exported[0]);row['target_issue']='202609101400';reject(lambda:pipeline.decode_raw_upstream(row),'RAW_TARGET_IDENTITY_DRIFT_REJECTED')
    row=copy.deepcopy(exported[0]);row['proofs']=[];row['upstream_raw_available']=False
    check(pipeline.decode_raw_upstream(row) is None,'MISSING_RAW_REMAINS_UNKNOWN')
    reject(lambda:deep_candidates.compare(None,None,None,None,None,None,None,None,None),'META_INVERSION_CANNOT_ENTER_TRAINING')
    scores=np.zeros((2,1200));raw=np.zeros_like(scores);raw[0,1199]=.5;raw[1,50]=.7
    order=pipeline.rank_order(scores,raw);check(order[0,0]==1199 and order[1,0]==50,'TIE_BREAK_RAW_PROBABILITY_BEFORE_ORDINAL')
    scores[0,10]=.00000001;check(pipeline.rank_order(scores,raw)[0,0]==10,'RANK_SCORE_PRIORITY_OVER_RAW_PROBABILITY')
    deep_candidates.require_contiguous_issues(['202609101439','202609101440','202609110001'])
    check(True,'D2_DAY_ROLLOVER_CONTINUITY')
    reject(lambda:deep_candidates.require_contiguous_issues(['202609100001','202609100003']),'D2_MISSING_ISSUE_CANNOT_BE_TREATED_AS_CONTIGUOUS')
    with tempfile.TemporaryDirectory(prefix='r932-sealed-fixture-') as tmp:
        root=Path(tmp);issues=[str(100+i)for i in range(20)]
        manifest={'game':'SYNTHETIC_TEST','schema':'FIXTURE','feature_manifest_sha256':'a'*64,'source_sha256':'b'*64,'splits':{'sealed':[10,20]}}
        first=consume(manifest,issues,{'ranker':'frozen-a'},root)
        check(consume(manifest,issues,{'ranker':'frozen-a'},root)==first,'SAME_TRIAL_CRASH_REPLAY_IS_IDEMPOTENT')
        reject(lambda:consume(manifest,issues,{'ranker':'renamed-or-tuned'},root),'NEW_CONFIG_CANNOT_RESET_SEALED_WINDOW')
        changed=copy.deepcopy(manifest);changed['source_sha256']='c'*64;changed['schema']='NEW_NAME'
        reject(lambda:consume(changed,issues,{'ranker':'frozen-a'},root),'COPIED_REENCODED_SOURCE_CANNOT_RESET_WINDOW')
        overlap=copy.deepcopy(manifest);overlap['splits']['sealed']=[9,19]
        reject(lambda:consume(overlap,issues,{'ranker':'b'},root),'OVERLAPPING_WINDOW_CANNOT_REUSE_LABELS')
        parallel=copy.deepcopy(manifest);parallel['splits']['sealed']=[0,8]
        def attempt(name):
            try:consume(parallel,issues,{'ranker':name},root);return 'won'
            except RuntimeError:return 'blocked'
        with concurrent.futures.ThreadPoolExecutor(max_workers=2)as pool:results=list(pool.map(attempt,['a','b']))
        check(sorted(results)==['blocked','won'],'CONCURRENT_SELECTIONS_HAVE_ONE_DURABLE_OWNER')
    lines.append('R932_TRAINING_REGRESSION_PASS|scope=JAVA_RAW_FIXTURES_AND_REAL_SQLITE_REGISTRY')
    report=ROOT/'reports/r932/TRAINING_REGRESSION.txt';report.parent.mkdir(parents=True,exist_ok=True);report.write_text('\n'.join(lines)+'\n')
    print('\n'.join(lines))
if __name__=='__main__':main()
