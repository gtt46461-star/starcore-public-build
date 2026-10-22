#!/usr/bin/env python3
from pathlib import Path
import json,sys,time
root=Path(__file__).resolve().parents[2]
src=root/'app/src/main/java/com/ethk3/novaonepro'
checks=[]
def ck(name, ok, detail=''):
    checks.append({'name':name,'pass':bool(ok),'detail':detail})
    if not ok: print('FAIL',name,detail)
def txt(p): return (root/p).read_text(encoding='utf-8')
start=time.time()
app=txt('app/src/main/java/com/ethk3/novaonepro/AppConfig.java')
svc=txt('app/src/main/java/com/ethk3/novaonepro/AutoFetchService.java')
boot=txt('app/src/main/java/com/ethk3/novaonepro/BootReceiver.java')
main=txt('app/src/main/java/com/ethk3/novaonepro/MainActivity.java')
diag=txt('app/src/main/java/com/ethk3/novaonepro/B07ReinforcedDiagnosticEngine.java')
manifest=txt('app/src/main/AndroidManifest.xml')
workflow=txt('.github/workflows/starcore-tianshu-b07-reinforced.yml')
ck('scope_b01_b07_only','B08=NOT_STARTED_BY_CONTRACT' in diag,'B08 remains NOT_STARTED')
ck('v51_tianshu_active','ENABLE_V51_TIANSHU = true' in app)
ck('fixed_k3','FIXED_K = 3' in app and 'ALLOW_K4 = false' in app and 'ALLOW_DYNAMIC_K = false' in app)
ck('single_bet_owner','STRICT_DECISION_GATE_ONLY_CAN_BET = true' in app and 'B07R_REAL_ACTION_OWNER = "DecisionGateEngine"' in app)
ck('no_unattended_money','B07R_UNATTENDED_MONEY_EXECUTION = false' in app)
ck('background_contract','B07R_BACKGROUND_CONTINUE_AFTER_UI_EXIT = true' in app and 'stopWithTask="false"' in manifest)
ck('task_removed_rearm','B07R_TASK_REMOVED_REARM' in svc and 'startWork();' in svc)
ck('business_stale_recovery','isServiceHealthyForRecovery() && !state.isBusinessStale()' in boot)
ck('bounded_guard','B07R_RUNTIME_GUARD_INTERVAL_MS' in svc and '5000L' in app)
ck('one_tap_copy','完整检测报告已一次复制' in main and '完整诊断分段' not in main)
ck('system_report_integrated','B07ReinforcedDiagnosticEngine.build(getApplicationContext(), db)' in main)
crash=txt('app/src/main/java/com/ethk3/novaonepro/CrashRecorder.java')
ck('crash_counters','crash_count' in crash and 'oom_count' in crash and 'soe_count' in crash)
ck('diagnosis_targets',all(x in diag for x in ['39/40/50/60','Wilson95%','ECE<=4%','频率','唯一主链','闪退','后台','策略/算法/公式','50/60%']))
ck('diagnosis_modules',all(x in diag for x in ['OrthogonalEvidenceWeightEngine','RegimeDriftCertificateEngine','ThirdNumberConditionalRankEngine','CertifiedCoverageExpansionEngine','HierarchicalCalibrationEngine','ExpertCorrelationAuditEngine']))
ck('bounded_ci','timeout 90s' in workflow and 'timeout 18m' in workflow and 'timeout-minutes: 45' in workflow)
bad=[]
for p in src.glob('*.java'):
    z=p.read_text(encoding='utf-8',errors='replace')
    if 'package com.ethk3.novaonepro;' not in z: bad.append(p.name+':package')
ck('java_package_integrity',not bad,','.join(bad[:20]))
report={'version':'B07R-20260807','started_at':start,'ended_at':time.time(),'duration_sec':round(time.time()-start,3),'checks':checks,'pass':all(x['pass'] for x in checks)}
out=root/'reports/B07R_BOUNDED_SELFCHECK.json';out.parent.mkdir(exist_ok=True);out.write_text(json.dumps(report,ensure_ascii=False,indent=2),encoding='utf-8')
print(json.dumps(report,ensure_ascii=False,indent=2))
sys.exit(0 if report['pass'] else 2)
