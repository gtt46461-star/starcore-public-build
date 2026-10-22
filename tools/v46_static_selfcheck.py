#!/usr/bin/env python3
from pathlib import Path
import json,re,sys
try:
    import javalang
except Exception as e:
    print(json.dumps({'ok':False,'fatal':'javalang_missing','error':repr(e)},ensure_ascii=False,indent=2));sys.exit(2)
root=Path(__file__).resolve().parents[1]
java=root/'app/src/main/java/com/ethk3/novaonepro'
checks=[]
def add(name,ok,detail=''): checks.append({'name':name,'ok':bool(ok),'detail':detail})
def text(name): return (java/name).read_text('utf-8')
errs=[]
for p in java.glob('*.java'):
    try:javalang.parse.parse(p.read_text('utf-8'))
    except Exception as e:errs.append({'file':p.name,'error':repr(e)})
add('all_java_parse',not errs,json.dumps(errs[:10],ensure_ascii=False))
alltxt='\n'.join(p.read_text('utf-8') for p in java.glob('*.java'))
add('java7_no_lambda','->' not in re.sub(r'"(?:\\.|[^"\\])*"','',alltxt),'lambda token check')
add('java7_no_stream_api','.stream()' not in alltxt and 'java.util.stream' not in alltxt)
app=text('AppConfig.java')
add('v46_identity','V46_NUMBER_DISTRIBUTION_FINAL_RANK_FULL_DIAGNOSIS_20260721_46072130' in app)
add('startup_30','MIN_DRAWS_ANALYZE = 30' in app and 'LIVE_SHADOW_WARMUP_ISSUES = 30' in app and 'V46_CONTROLLED_START_MIN_ISSUES = 30' in app)
add('candidate_surface_1200_60_40_12','V46_PROBABILITY_CANDIDATE_COUNT = 60' in app and 'V46_FORMAL_CANDIDATE_COUNT = 40' in app and 'V46_UI_DISPLAY_CANDIDATE_COUNT = 12' in app and 'OOS_LIVE_CANDIDATES_PER_ISSUE = 40' in app)
add('fixed_single_position_k3','FIXED_K = 3' in app and 'ALLOW_K4 = false' in app and 'ALLOW_MULTI_POSITION_BET = false' in app and 'ALLOW_MARTINGALE = false' in app)
rt=text('RealtimeCausalProbabilityEngine.java')
add('number_first_owner','NumberDistributionEngineV46' in rt and 'ConditionalCalibrationEngine' in rt)
add('no_oos_before_final_rank','OosActionLogEngine.recordCandidates' not in rt and 'OosActionLogEngine.recordCandidate' not in rt)
auto=text('AutoFetchService.java')
rank_at=auto.find('FinalCandidateRankingEngine.rank')
log_at=auto.find('CandidateDecisionFunnelLogEngine.persist')
gate_at=auto.find('decisionGateEngine.decide',rank_at)
add('final_rank_before_log_and_gate',rank_at>=0 and log_at>rank_at and gate_at>log_at,f'{rank_at},{log_at},{gate_at}')
segment=auto[auto.find('private Models.Decision buildDecision'):auto.find('private Models.Decision buildDecision')+50000]
add('single_teacher_snapshot_live',segment.count('runTeacherNumberMatrixFinalBridge(')<=1,str(segment.count('runTeacherNumberMatrixFinalBridge(')))
add('root_cause_live','RootCauseAttributionEngine.diagnose' in auto)
dg=text('DecisionGateEngine.java')
public_start=dg.find('public Models.Decision decide(')
legacy_start=dg.find('private Models.Decision decideLegacyDiagnosticOnly')
public_body=dg[public_start:legacy_start] if public_start>=0 and legacy_start>public_start else ''
add('legacy_real_gate_not_called','pickDecisionCandidate(' not in public_body and 'finalCheckInternal(' not in public_body)
add('legacy_gate_private_only',legacy_start>=0 and 'v46_legacy_gate_invoked", "0"' in public_body)
add('authorized_rank_eventp_first','betterV46Authorized' in dg and 'e.probability' in dg)
pe=text('PredictionEnvelope.java')
add('eventp_actionlcb_separate','canonicalProbability' in pe and 'actionLcb' in pe and 'c.v43SafeProbability = out.actionLcb' in pe)
ua=text('DecisionAuthorizationEngine.java')
add('authorization_sign_only','compileForLane(' not in ua and 'CompiledActionContract.getEligible' in ua and 'contract.actionLcb' in ua)
lane=text('LanePolicyCompiler.java')
add('single_lane_compile','nextCompileCount' in lane and 'V46_PROBE_MIN_EVENT_P' in lane and 'V46_PROBE_MIN_ACTION_LCB' in lane)
oos=text('OosActionLogEngine.java')
add('exact_final40_oos','V46_FINAL40_EXACT_RANK_IMMUTABLE' in oos and 'v46_event_probability' in oos and 'OOS_LIVE_CANDIDATES_PER_ISSUE = 40' in app)
db=text('DbHelper.java')
add('v46_oos_schema',all(x in db for x in ['v46_event_probability','v46_action_lcb','v46_probability_uncertainty','v46_root_cause_code','v46_final_contract_hash']))
diag=text('DecisionFunnelDiagnosisEngine.java')
add('v46_real_contract_diagnosis','V46_FINAL40_DECISION_FUNNEL_DIAGNOSIS' in diag and 'v46_event_probability' in diag and 'MODEL_NO_LIFT' in diag and 'RANK_INVERSION' in diag)
add('no_old_v44_diagnosis_replay','V44统一决策漏斗与稳定性诊断' not in diag and 'currentApproxReady' not in diag)
models=text('Models.java')
add('v46_fields_copied',models.count('v46EventProbability')>=6 and 'c.v46ActionLcb = v46ActionLcb' in models)
workflow=root/'.github/workflows/v46-build-and-selfcheck.yml'
add('v46_workflow_exists',workflow.exists())
failed=[x for x in checks if not x['ok']]
out={'ok':not failed,'version':'V46.0.46072130','java_files':len(list(java.glob('*.java'))),'checks':checks,'failed':failed}
(root/'reports').mkdir(exist_ok=True)
(root/'reports/V46_STATIC_SELFCHECK_46072130.json').write_text(json.dumps(out,ensure_ascii=False,indent=2),'utf-8')
print(json.dumps(out,ensure_ascii=False,indent=2));sys.exit(1 if failed else 0)
