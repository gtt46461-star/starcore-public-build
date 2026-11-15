#!/usr/bin/env python3
"""Compare production JVM, ART and the actual offline training decoder, issue by issue.

Synthetic chronology exercises engineering parity only. It is not physical-phone or future OOS
evidence. Input provenance, masks, semantic hashes and complete arrays are checked separately;
matching dimensions or existence of old log files cannot close this acceptance.
"""
import argparse, copy, gzip, hashlib, itertools, json, sys
from pathlib import Path
import numpy as np
ROOT=Path(__file__).resolve().parents[1]
sys.path.insert(0,str(ROOT))
from training.r929 import pipeline as p

def digest(file):
    h=hashlib.sha256()
    with file.open('rb') as f:
        for block in iter(lambda:f.read(1048576),b''):h.update(block)
    return h.hexdigest()

def rows(file):
    return gzip.open(file,'rt',encoding='utf-8') if file.suffix=='.gz' else file.open(encoding='utf-8')

def reject(fn, expected):
    try:fn()
    except ValueError as e:
        if expected not in str(e):raise
    else:raise AssertionError('ACCEPTED_INVALID_INPUT:'+expected)

def main():
    a=argparse.ArgumentParser();a.add_argument('--host',type=Path,required=True)
    a.add_argument('--android',type=Path,required=True);a.add_argument('--out',type=Path,required=True)
    a.add_argument('--source-commit',required=True);args=a.parse_args()
    n=values=0;max_error=0.;last=['',''];manifests=[None,None];checks=[];first=None
    with rows(args.host) as host,rows(args.android) as device:
        for h,d in itertools.zip_longest(host,device):
            if h is None or d is None:raise AssertionError('ISSUE_COUNT_MISMATCH')
            pair=[p.read_integrity_row(h),p.read_integrity_row(d)];raw=[]
            for i,row in enumerate(pair):
                manifests[i]=p.validate_row(row,last[i],manifests[i]);last[i]=row['target_issue']
                raw.append(p.decode_raw_upstream(row))
                if raw[-1] is None:raise AssertionError('LEGACY_CANNOT_CLOSE_FULL_PARITY')
            for field in ('target_issue','max_source_issue','feature_manifest_sha256','input_stage','frozen_at','draw_at','actual'):
                if pair[0][field]!=pair[1][field]:raise AssertionError('ROW_FIELD:'+field)
            if raw[0].keys()!=raw[1].keys():raise AssertionError('RAW_FIELDS_DIFFER')
            for field in raw[0]:
                x,y=raw[0][field],raw[1][field]
                if isinstance(x,np.ndarray):
                    if x.shape!=y.shape or not np.isfinite(x).all() or not np.isfinite(y).all():raise AssertionError(field)
                    err=float(np.max(np.abs(x-y)));max_error=max(max_error,err);values+=x.size
                    if not np.allclose(x,y,rtol=1e-6,atol=1e-8):raise AssertionError('RAW_VALUE:'+field)
                elif x!=y:raise AssertionError('RAW_PROVENANCE:'+field)
            x=np.asarray([v[4] for v in pair[0]['actions']]);y=np.asarray([v[4] for v in pair[1]['actions']])
            if x.shape!=(1200,96) or y.shape!=x.shape:raise AssertionError('META_SHAPE')
            max_error=max(max_error,float(np.max(np.abs(x-y))));values+=x.size
            if not np.allclose(x,y,rtol=1e-6,atol=1e-8):raise AssertionError('META_VALUE')
            if first is None:first=pair[0]
            checks.append({'issue':last[0],'fields':int(x.size+1800+40+900),'status':'PASS'})
            n+=1
    if n!=100:raise AssertionError('EXACTLY_100_REQUIRED:'+str(n))
    # These negatives call the same decoder as dataset training.
    def mutated_raw(change):
        row=copy.deepcopy(first);raw=json.loads(row['proofs'][0][len('R932_UPSTREAM_RAW='):]);change(raw)
        payload=json.dumps(raw,separators=(',',':'));row['proofs'][0]='R932_UPSTREAM_RAW='+payload
        row['proofs'][1]='R932_UPSTREAM_SHA256='+p.digest(payload.encode());return row
    reject(lambda:p.decode_raw_upstream(mutated_raw(lambda r:r.update(created_at=first['frozen_at']+1))), 'RAW_UPSTREAM_AVAILABLE_AFTER_FREEZE')
    reject(lambda:p.decode_raw_upstream(mutated_raw(lambda r:r.update(meta_schema_sha256='0'*64))), 'RAW_UPSTREAM_META_SEMANTIC_MISMATCH')
    reject(lambda:p.decode_raw_upstream(mutated_raw(lambda r:r.update(base_issue=first['target_issue']))), 'RAW_UPSTREAM_PIT_IDENTITY')
    zero=mutated_raw(lambda r:r['number_features'][0][0].__setitem__(0,0.))
    assert p.decode_raw_upstream(zero)['number_features'][0,0,0]==0.
    legacy=copy.deepcopy(first);legacy['proofs']=[];legacy['upstream_raw_available']=False
    assert p.decode_raw_upstream(legacy) is None
    report={'status':'PASS','task':'E03A','source_commit':args.source_commit,'issues':n,'numeric_values_compared':values,
            'max_abs_error':max_error,'endpoints':['production_JVM','production_Android_ART','training.r929.pipeline'],
            'runtime_scope':'ANDROID_EMULATOR_NOT_PHYSICAL_PHONE','device_verified':True,'physical_device_verified':False,
            'effect_proven':False,'may_authorize_real':False,'inputs':{str(f):digest(f) for f in (args.host,args.android)},
            'per_issue':checks,'negative_cases':['late_availability','wrong_semantic_same_shape','future_source'],
            'legal_zero_present':True,'legacy_unknown_not_upgraded':True}
    args.out.parent.mkdir(parents=True,exist_ok=True);args.out.write_text(json.dumps(report,indent=2)+'\n')
    print('R939_E03A_THREE_END_PASS|issues=100|values='+str(values)+'|scope=ANDROID_EMULATOR')

if __name__=='__main__':main()
