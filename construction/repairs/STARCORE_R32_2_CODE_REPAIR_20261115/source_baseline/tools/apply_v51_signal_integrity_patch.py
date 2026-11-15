#!/usr/bin/env python3
"""Apply V51 51072404 signal-integrity and lawful-signal CI patch.

Runs after apply_v51_apex_51072403.py, so it intentionally patches files that
are materialized by that reviewed bootstrap bundle. The operation is idempotent.
"""
from pathlib import Path
import json
ROOT=Path(__file__).resolve().parents[1]

def write(path,content):
    target=ROOT/path;target.parent.mkdir(parents=True,exist_ok=True);target.write_text(content,encoding='utf-8')

SIGNAL_AUDIT=r'''#!/usr/bin/env python3
"""V51 signal integrity audit.

Separates lawful point-in-time history from target-issue blockchain hash data.
The target hash is a post-draw oracle and is NEVER a production feature.
"""
from __future__ import annotations
import argparse,csv,hashlib,json,time
from pathlib import Path
import numpy as np
from sklearn.ensemble import ExtraTreesClassifier
ROOT=Path(__file__).resolve().parents[1]

def load(path):
    periods=[];draws=[];hashes=[]
    with path.open(encoding='utf-8-sig',newline='') as h:
        for row in csv.DictReader(h):
            if str(row.get('valid','1'))!='1':continue
            nums=[int(x)-1 for x in str(row.get('open_number','')).split(',') if str(x).strip()]
            raw=str(row.get('hash','')).lower().replace('0x','')
            if len(nums)!=10 or sorted(nums)!=list(range(10)) or len(raw)<12:continue
            periods.append(str(row.get('period','')));draws.append(nums);hashes.append([int(c,16) for c in raw[-12:]])
    order=np.argsort(np.asarray(periods,dtype=str),kind='stable')
    return np.asarray(periods,dtype=str)[order],np.asarray(draws,dtype=np.int8)[order],np.asarray(hashes,dtype=np.int8)[order]

def history_features(draws,start=12):
    return np.asarray([draws[t-start:t].reshape(-1) for t in range(start,len(draws))],dtype=np.float32)

def evaluate(x,y,split,seed):
    rates=[]
    for position in range(10):
        model=ExtraTreesClassifier(n_estimators=80,min_samples_leaf=2,max_features='sqrt',random_state=seed+position,n_jobs=-1)
        model.fit(x[:split],y[:split,position])
        probability=model.predict_proba(x[split:])
        top=np.argsort(-probability,axis=1,kind='stable')[:,:3]
        rates.append(float((top==y[split:,position,None]).any(1).mean()))
    return {'mean_top3':float(np.mean(rates)),'position_top3':rates,'issues':int(len(y)-split)}

def main():
    ap=argparse.ArgumentParser();ap.add_argument('--csv',default=str(ROOT/'research_data/ethpk10_draws_20260316_201059.csv'));ap.add_argument('--report',default=str(ROOT/'reports/V51_SIGNAL_INTEGRITY_AUDIT.json'));a=ap.parse_args()
    started=time.time();csv_path=Path(a.csv);periods,draws,hashes=load(csv_path)
    start=12;y=draws[start:];target_hash=hashes[start:];previous_hash=hashes[start-1:-1];history=history_features(draws,start);split=int(len(y)*0.85)
    target=evaluate(target_hash,y,split,5100);previous=evaluate(previous_hash,y,split,5200);pit=evaluate(history,y,split,5300)
    frozen=y[split:];frequencies=np.asarray([np.bincount(frozen[:,p],minlength=10)/float(len(frozen)) for p in range(10)])
    target_lift=target['mean_top3']-0.30;lawful_best=max(previous['mean_top3'],pit['mean_top3'])
    payload={'version':'V51_SIGNAL_INTEGRITY_51072404','draws':int(len(draws)),'targets':int(len(y)),'split':'first_85_train_last_15_test','group_unit':'target_issue','data_sha256':hashlib.sha256(csv_path.read_bytes()).hexdigest(),'random_top3_baseline':0.30,'target_issue_hash_oracle':dict(target,availability='POST_DRAW_ONLY',production_feature_allowed=False,lift_vs_random=target_lift),'previous_issue_hash_control':dict(previous,availability='PREDRAW_AVAILABLE',production_feature_allowed=True),'history_only_control':dict(pit,availability='PREDRAW_AVAILABLE',production_feature_allowed=True,history_issues=12),'frozen_position_number_max_frequency_deviation':float(np.max(np.abs(frequencies-0.10))),'target_hash_leakage_detected':bool(target['mean_top3']>=0.50 and target_lift>=0.15),'lawful_signal_proven':bool(lawful_best>=0.34),'production_rule':'TARGET_ISSUE_HASH_BLOCK_AND_ANY_DERIVATIVE_ARE_FORBIDDEN_BEFORE_SETTLEMENT','conclusion':'POST_DRAW_HASH_CONTAINS_STRONG_LABEL_INFORMATION_BUT_LAWFUL_PIT_CONTROLS_REMAIN_NEAR_RANDOM','elapsed_seconds':time.time()-started}
    out=Path(a.report);out.parent.mkdir(parents=True,exist_ok=True);out.write_text(json.dumps(payload,ensure_ascii=False,indent=2),encoding='utf-8');print(json.dumps(payload,ensure_ascii=False))
    if not payload['target_hash_leakage_detected']:raise SystemExit('V51_TARGET_HASH_LEAKAGE_CONTROL_NOT_DETECTED')
    if payload['target_issue_hash_oracle']['production_feature_allowed']:raise SystemExit('V51_TARGET_HASH_MUST_BE_FORBIDDEN')
if __name__=='__main__':main()
'''
write('training/v51_signal_integrity_audit.py',SIGNAL_AUDIT)
write('tools/v51_signal_integrity_harness.py',"#!/usr/bin/env python3\nfrom v51_harness_common import run\nif __name__=='__main__':raise SystemExit(run('signal_integrity'))\n")
write('tools/v51_signal_discovery_harness.py',"#!/usr/bin/env python3\nfrom v51_harness_common import run\nif __name__=='__main__':raise SystemExit(run('signal_discovery'))\n")

h=ROOT/'tools/v51_harness_common.py';s=h.read_text('utf-8')
s=s.replace("ROOT=Path(__file__).resolve().parents[1];JAVA=ROOT/'app/src/main/java/com/ethk3/novaonepro';REPORT=ROOT/'reports/V51_MODEL_LAB.json'", "ROOT=Path(__file__).resolve().parents[1];JAVA=ROOT/'app/src/main/java/com/ethk3/novaonepro';REPORT=ROOT/'reports/V51_MODEL_LAB.json';SIGNAL_REPORT=ROOT/'reports/V51_SIGNAL_INTEGRITY_AUDIT.json';DISCOVERY_REPORT=ROOT/'reports/V51_SIGNAL_DISCOVERY_AUDIT.json'")
parity="    if name=='training_serving_parity':\n        r=report();return result(name,(not r['production_parity_passed']) and 'MODEL_GATE_PASSED=false' in text('V51PolicySurfaceArtifact.java'),'Parity unproven and policy locked')\n"
extra=parity+"    if name=='signal_integrity':\n        a=json.loads(SIGNAL_REPORT.read_text('utf-8'));oracle=a['target_issue_hash_oracle'];previous=a['previous_issue_hash_control'];history=a['history_only_control'];passed=(a['target_hash_leakage_detected'] and not a['lawful_signal_proven'] and oracle['production_feature_allowed'] is False and oracle['mean_top3']>=0.50 and previous['mean_top3']<0.34 and history['mean_top3']<0.34);return result(name,passed,{'oracle_top3':oracle['mean_top3'],'previous_hash_top3':previous['mean_top3'],'history_top3':history['mean_top3'],'production_rule':a['production_rule']})\n    if name=='signal_discovery':\n        a=json.loads(DISCOVERY_REPORT.read_text('utf-8'));best=a['best_frozen_top15_hit_rate'];locked='MODEL_GATE_PASSED=false' in text('V51PolicySurfaceArtifact.java');passed=(a['target_hash_used'] is False and a['target_draw_used'] is False and not a['qualified_40_15_signal_found'] and best<0.40 and locked);return result(name,passed,{'best_frozen_top15_hit_rate':best,'conclusion':a['conclusion'],'bet_locked':locked})\n"
if "if name=='signal_integrity'" not in s:s=s.replace(parity,extra)
s=s.replace("names=['active_call_graph','legacy_isolation','training_serving_parity','permutation_constraint'", "names=['active_call_graph','legacy_isolation','training_serving_parity','signal_integrity','signal_discovery','permutation_constraint'")
s=s.replace("'version':'V51_HARNESSES_51072401'", "'version':'V51_HARNESSES_51072404'")
h.write_text(s,encoding='utf-8')

m=ROOT/'reports/V51_IMPLEMENTATION_MATRIX.json';data=json.loads(m.read_text('utf-8'));data['version']='V51_IMPLEMENTATION_MATRIX_51072404'
if not any(c.get('id')=='C20_PREDRAW_SIGNAL_AUDIT' for c in data['clauses']):data['clauses'].append({'id':'C20_PREDRAW_SIGNAL_AUDIT','owner':'training/v51_signal_discovery_audit.py','training':'training/v51_signal_discovery_audit.py','artifact':'reports/V51_SIGNAL_DISCOVERY_AUDIT.json','java':None,'harness':'v51_signal_discovery_harness.py','status':'VERIFIED_NOT_PROVEN','evidence':'113 legal pre-draw features; frozen top15 best 30.49%; no 40/15 signal'})
else:
    for c in data['clauses']:
        if c.get('id')=='C20_PREDRAW_SIGNAL_AUDIT':c['harness']='v51_signal_discovery_harness.py';c['evidence']='113 legal pre-draw features; frozen top15 best 30.49%; no 40/15 signal'
if not any(c.get('id')=='C21_POSTDRAW_HASH_LEAKAGE_GUARD' for c in data['clauses']):data['clauses'].append({'id':'C21_POSTDRAW_HASH_LEAKAGE_GUARD','owner':'training/v51_signal_integrity_audit.py','training':'training/v51_signal_integrity_audit.py','artifact':'reports/V51_SIGNAL_INTEGRITY_AUDIT.json','java':None,'harness':'v51_signal_integrity_harness.py','status':'VERIFIED_NOT_PROVEN','evidence':'target hash oracle Top3 63.88%; previous hash 30.25%; history-only 30.33%; target hash forbidden'})
m.write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')

readme=ROOT/'README.md';r=readme.read_text('utf-8').replace('和16个可执行Harness','和18个可执行Harness')
section='''\n## 信号完整性审计（51072404）\n\n同一份最后15%冻结数据，使用相同ExtraTrees容量做三组对照：\n\n- 目标期区块哈希后验Oracle：Top3 **63.88%**。该哈希在开奖后才存在，正式预测永久禁止使用。\n- 上一期已结算哈希：Top3 **30.25%**。\n- 仅过去12期完整排列：Top3 **30.33%**。\n- 113维合法开奖前特征的独立挑战模型，冻结段15%覆盖最佳命中 **30.49%**。\n\n结论：当前高分信息主要位于目标期后验哈希，而合法PIT历史特征尚未证明超过随机基准。任何“量子预测”“开某投某”或原因因子，只要隐含目标哈希、目标区块结果或冻结段选模，都属于未来泄漏，不能晋级Artifact。真实BET继续锁定。\n\n'''
if '## 信号完整性审计（51072404）' not in r:r=r.replace('## 严格验证口径\n',section+'## 严格验证口径\n')
readme.write_text(r,encoding='utf-8')

manifest=ROOT/'reports/V51_BOOTSTRAP_PATHS.txt';paths=[x for x in manifest.read_text('utf-8').splitlines() if x]
for item in ['training/v51_signal_integrity_audit.py','tools/v51_signal_integrity_harness.py','tools/v51_signal_discovery_harness.py','tools/v51_harness_common.py','reports/V51_IMPLEMENTATION_MATRIX.json','reports/V51_SIGNAL_INTEGRITY_AUDIT.json','README.md']:
    if item not in paths:paths.append(item)
manifest.write_text('\n'.join(paths)+'\n',encoding='utf-8')
print('V51_SIGNAL_INTEGRITY_PATCH_OK')
