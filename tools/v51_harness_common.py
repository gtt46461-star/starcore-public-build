#!/usr/bin/env python3
from __future__ import annotations
import hashlib,json,subprocess,sys
from pathlib import Path
import numpy as np
ROOT=Path(__file__).resolve().parents[1];JAVA=ROOT/'app/src/main/java/com/ethk3/novaonepro';REPORT=ROOT/'reports/V51_MODEL_LAB.json'
def report():return json.loads(REPORT.read_text('utf-8'))
def text(name):return (JAVA/name).read_text('utf-8')
def result(name,passed,detail,status='VERIFIED_NOT_PROVEN'):
    payload={'harness':name,'passed':bool(passed),'status':status if passed else 'FAILED','detail':detail};print(json.dumps(payload,ensure_ascii=False));return 0 if passed else 1
def run(name):
    if name=='active_call_graph':
        runtime=text('V51RuntimeOrchestrator.java');auto=text('AutoFetchService.java');manifest=text('V51BuildManifest.java')
        active_start=auto.find('private Models.Decision buildV51ApexDecision')
        active_end=auto.find('private Models.Decision buildV50PrimeDecision',active_start)
        active=auto[active_start:active_end] if active_start>=0 and active_end>active_start else ''
        bad=[token for token in ('V50RuntimeOrchestrator','FREQUENCY_FLOOR','needSupply','buildV50PrimeDecision(') if token in active]
        passed=('public static final boolean ACTIVE_RUNTIME = true' in manifest and 'return buildV51ApexDecision' in auto
                and 'V51AutoFetchBridge.run(' in active and 'toLegacyDecisionV51' in active
                and 'new DecisionGateEngine().decideV51' in runtime and 'V50RuntimeOrchestrator' not in runtime and not bad)
        return result(name,passed,{'active_route_found':active_start>=0,'forbidden':bad})
    if name=='legacy_isolation':
        bad=[]
        for p in JAVA.glob('V51*.java'):
            if 'V50RuntimeOrchestrator' in p.read_text('utf-8'):bad.append(p.name)
        auto=text('AutoFetchService.java')
        passed=not bad and 'V51_APEX_SIDECAR_CALL' not in auto and auto.count('V51AutoFetchBridge.run(')==1
        return result(name,passed,{'legacy_references':bad,'active_bridge_count':auto.count('V51AutoFetchBridge.run(')})
    if name=='training_serving_parity':
        r=report();return result(name,(not r['production_parity_passed']) and 'MODEL_GATE_PASSED=false' in text('V51PolicySurfaceArtifact.java'),'Parity unproven and policy locked')
    if name=='permutation_constraint':
        sys.path.insert(0,str(ROOT/'training'));from v51_training_core import sinkhorn
        q=sinkhorn(np.random.default_rng(51).normal(size=(32,10,10)));row=float(np.max(np.abs(q.sum(2)-1)));col=float(np.max(np.abs(q.sum(1)-1)));return result(name,row<1e-8 and col<1e-8,{'row_error':row,'column_error':col})
    if name=='window_discovery':return result(name,'bestWindow' in text('V51WindowPosteriorEngine.java'),'Per-cell dynamic window posterior')
    if name=='false_discovery':
        s=text('V51FalseDiscoveryGuard.java')+text('V51RelationGraphVerifier.java');return result(name,'wilsonLower' in s and 'benjaminiHochberg' in s and 'winnerPenalty' in s,'Wilson, BH-FDR, winner penalty')
    if name=='state_replay':
        s=text('V51StateTrajectoryEngine.java');a=hashlib.sha256(s.encode()).hexdigest();return result(name,a==hashlib.sha256(text('V51StateTrajectoryEngine.java').encode()).hexdigest() and 'String.contains' not in s,{'hash':a})
    if name=='expert_ablation':
        required=['V51HierarchicalPriorNumberExpert.java','V51MultiScaleTrendNumberExpert.java','V51ContextTreeNumberExpert.java','V51StateDurationNumberExpert.java','V51MotifNumberExpert.java','V51RelationGraphNumberExpert.java','V51TeacherPosteriorNumberExpert.java','V51OfflineResidualNumberExpert.java','V51OnlineResidualNumberExpert.java'];missing=[x for x in required if not (JAVA/x).exists()];return result(name,not missing,{'missing':missing,'oos_ablation':'NOT_PROVEN'})
    if name=='action_rank':
        r=report();m=r['development_rank'];proven='PROVEN=true' in text('V51ActionRankerArtifact.java');gate=m['stable_folds']>=3 and m['rank1_rate']>m['rank2_rate'] and m['rank1_rate']>m['rank3_rate'];return result(name,(gate and proven) or ((not gate) and not proven),{'metrics':m,'artifact_proven':proven})
    if name=='selected_calibration':
        e=report()['development_calibration']['ece'];p='PROVEN=true' in text('V51SelectedCalibrationArtifact.java');return result(name,(e<=0.04 and p) or not p,{'ece':e,'artifact_proven':p})
    if name=='predictability':
        p='PROVEN=true' in text('V51PredictabilityArtifact.java');surface=report()['development_policy_surface'];return result(name,(surface is not None and p) or not p,{'surface_found':surface is not None,'artifact_proven':p})
    if name=='risk_coverage':
        r=report();f=r['frozen_once'];g=r['model_gate_passed'];j='MODEL_GATE_PASSED=true' in text('V51PolicySurfaceArtifact.java');q=f['coverage']>=.15 and f['hit_rate']>=.40 and f['ece']<=.04 and len(f['blocks'])==4 and min(f['blocks'])>=.37;return result(name,g==j and (not g or q),{'frozen':f,'model_gate':g,'java_gate':j})
    if name=='ledger':
        s=text('V51Repository.java');req=['v51_issue_prediction','v51_number_event','v51_position_action','v51_permutation','v51_selected_decision'];missing=[x for x in req if x not in s];return result(name,not missing and 'V51_PREDICTION_HASH_CONFLICT' in s,{'missing':missing})
    if name=='deterministic_replay':
        r=report();return result(name,r['data_sha256']==json.loads(REPORT.read_text('utf-8'))['data_sha256'],{'data_sha256':r['data_sha256']})
    if name=='fault_injection':
        s=text('V51SinkhornEngine.java')+text('V51Repository.java')+text('V51ArtifactPromotionEngine.java')+text('V51ArtifactRollbackEngine.java')
        passed='usedFallback' in s and 'PREDICTION_HASH_CONFLICT' in s and 'reconcileOrThrow' in s and 'V51_PROMOTION_GATE_NOT_PASSED' in s and 'v51_runtime_forced_shadow' in s
        return result(name,passed,'Fallback, ledger conflict, promotion refusal and rollback-to-shadow guards')
    if name=='hash_signal':
        path=ROOT/'reports/V51_HASH_SIGNAL_AUDIT.json'
        if not path.exists():return result(name,False,'V51_HASH_SIGNAL_AUDIT.json missing')
        audit=json.loads(path.read_text('utf-8'));gate=audit['hash_number_signal_gate'];contract=audit['prediction_time_contract']
        script=(ROOT/'training/audit_v51_hash_signal.py').read_text('utf-8')
        passed=(not gate['passed'] and gate['production_number_weight']==0.0
                and contract['target_issue_hash_available_pre_draw'] is False
                and contract['target_issue_hash_allowed_in_prediction'] is False
                and audit['historical_hash_fdr']['discoveries_q_le_0_05']==0
                and 'hashes[target - lag]' in script and 'hashes[target]' not in script)
        return result(name,passed,{'conclusion':audit['formal_conclusion'],'gate':gate,'fdr':audit['historical_hash_fdr']})
    if name=='performance':
        runtime=text('V51RuntimeOrchestrator.java');ledger=text('V51PerformanceSpanLedger.java')
        passed=float(report()['elapsed_seconds'])<3600 and 'performance.begin' in runtime and 'exclusive_ms' in ledger
        return result(name,passed,{'device_p95':'NOT_MEASURED','span_ledger':passed})
    raise SystemExit('unknown harness '+name)
def run_all():
    names=['active_call_graph','legacy_isolation','training_serving_parity','permutation_constraint','window_discovery','false_discovery','state_replay','expert_ablation','action_rank','selected_calibration','predictability','risk_coverage','ledger','deterministic_replay','fault_injection','hash_signal','performance'];results=[];failed=[]
    for name in names:
        p=subprocess.run([sys.executable,str(ROOT/'tools'/('v51_'+name+'_harness.py'))],capture_output=True,text=True);line=(p.stdout.strip().splitlines() or [p.stderr.strip()])[-1]
        try:data=json.loads(line)
        except Exception:data={'harness':name,'passed':False,'detail':line}
        results.append(data)
        if p.returncode!=0 or not data.get('passed'):failed.append(name)
    payload={'version':'V51_HARNESSES_51072404','passed':len(results)-len(failed),'total':len(results),'failed':failed,'release_state':report()['release_state'],'model_gate_passed':report()['model_gate_passed'],'results':results};(ROOT/'reports/V51_HARNESSES_51072401.json').write_text(json.dumps(payload,ensure_ascii=False,indent=2),encoding='utf-8');print(json.dumps(payload,ensure_ascii=False));return 1 if failed else 0
