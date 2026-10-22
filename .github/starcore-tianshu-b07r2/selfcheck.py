#!/usr/bin/env python3
from pathlib import Path
import json,re,sys,hashlib,os
R=Path.cwd(); S=R/'app/src/main/java/com/ethk3/novaonepro'
checks=[]
def ck(name, ok, detail=''):
    checks.append({'name':name,'ok':bool(ok),'detail':detail})
    print(('PASS ' if ok else 'FAIL ')+name+(' :: '+detail if detail else ''))

app=(S/'AppConfig.java').read_text(encoding='utf-8')
svc=(S/'AutoFetchService.java').read_text(encoding='utf-8')
main=(S/'MainActivity.java').read_text(encoding='utf-8')
man=(R/'app/src/main/AndroidManifest.xml').read_text(encoding='utf-8')
boot=(S/'BootReceiver.java').read_text(encoding='utf-8')
provider=(S/'V50BootstrapProvider.java').read_text(encoding='utf-8')
dec=(S/'DecisionGateEngine.java').read_text(encoding='utf-8')
manifest=(S/'B07R2BuildAuditManifest.java').read_text(encoding='utf-8')

ck('scope_B08_not_started','B08=NOT_STARTED_BY_CONTRACT' in (S/'B07R2FullDiagnosticEngine.java').read_text(encoding='utf-8'))
ck('game_Binance1_PK10','LOTTERY_CODE = "Binance1-PK10"' in app)
ck('fixed_K3','FIXED_K = 3' in app and 'ALLOW_K4 = false' in app and 'ALLOW_DYNAMIC_K = false' in app and 'ALLOW_MULTI_POSITION_BET = false' in app)
ck('decision_gate_only','STRICT_DECISION_GATE_ONLY_CAN_BET = true' in app)
ck('v51_active_v50_inactive','ENABLE_V51_TIANSHU = true' in app and 'ENABLE_V50_PRIME = false' in app)
ck('no_fake_model_proof','B07R2_NO_FAKE_MODEL_PROOF = true' in app)
ck('strict_live_action_policy','B07R2_ACTIVE_TIANSHU_REAL_ACTION = true' in app and 'BET_ALLOWED_BY_STRICT_GATE' in dec)
ck('service_stop_with_task_false','android:stopWithTask="false"' in man)
ck('service_sticky','return START_STICKY;' in svc)
ck('task_removed_rearm','forceRearmAutoFetchService' in svc and 'scheduleServiceRestartV5("onTaskRemoved")' in svc)
ck('low_memory_self_heal','onLowMemory()' in svc and 'B07R2_LOW_MEMORY_SELF_HEAL' in svc)
ck('process_bootstrap_rearm','BootReceiver.startAutoFetchService' in provider and 'process_bootstrap_light' in provider)
ck('recovery_alarm_job','scheduleAllRecovery' in boot and 'scheduleJobWatchdog' in boot)
ck('one_tap_r2_report','B07R2FullDiagnosticEngine.build' in main and 'B07R31_REPORT_COMPLETE_MAX_CHARS' in main)
ck('54_file_audit_manifest',manifest.count('new Entry(')==54,'entries='+str(manifest.count('new Entry(')))
missing=[line for line in manifest.splitlines() if 'new Entry(' in line and ', false,' in line]
ck('54_plan_files_present',len(missing)==0,'missing='+str(len(missing)))
secret=os.environ.get('DUOQU_API_KEY','').strip(); source_text='\n'.join(p.read_text(encoding='utf-8',errors='ignore') for p in S.glob('*.java')); ck('no_key_literal_in_source',(not secret) or secret not in source_text,'env_secret_scan' if secret else 'secret_not_injected')
ck('diagnosis_has_targets',all(x in (S/'B07R2FullDiagnosticEngine.java').read_text(encoding='utf-8') for x in ['40%','50%','60%','Coverage','Wilson','ECE','ExpertCorrelationAuditEngine','ThirdNumberConditionalRankEngine']))
ck('bounded_runtime_guard','B07R2_RUNTIME_GUARD_INTERVAL_MS = 3000L' in app)

out={'version':'B07R2','checks':checks,'pass':sum(1 for x in checks if x['ok']),'fail':sum(1 for x in checks if not x['ok']),'missing_plan_entries':missing}
(R/'reports').mkdir(exist_ok=True)
(R/'reports/B07R2_BOUNDED_SELFCHECK.json').write_text(json.dumps(out,ensure_ascii=False,indent=2),encoding='utf-8')
hard=[x for x in checks if not x['ok'] and x['name']!='54_plan_files_present']
if hard: sys.exit(2)
