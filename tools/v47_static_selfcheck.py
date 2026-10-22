from pathlib import Path
import re, json
ROOT=Path(__file__).resolve().parents[1]
J=ROOT/'app/src/main/java/com/ethk3/novaonepro'
checks={}
def ck(k,v): checks[k]=bool(v)
app=(J/'AppConfig.java').read_text('utf-8'); auto=(J/'AutoFetchService.java').read_text('utf-8')
ck('version', 'V47.0.47072147' in app)
ck('java17', 'VERSION_17' in (ROOT/'app/build.gradle').read_text('utf-8'))
ck('agp8122', "8.12.2" in (ROOT/'build.gradle').read_text('utf-8'))
ck('surface1200_60_40_12', all(x in app for x in ['V47_FULL_SCAN_CANDIDATE_COUNT = 1200','V47_PROBABILITY_CANDIDATE_COUNT = 60','V47_FORMAL_CANDIDATE_COUNT = 40','V47_UI_DISPLAY_CANDIDATE_COUNT = 12']))
ck('no_18_trim', 'DYNAMIC_CAUSAL_REALTIME_CANDIDATE_CAP = 18' not in auto)
ck('startup_context', 'StartupContextEngine.resolve(store, c)' in (J/'LanePolicyCompiler.java').read_text('utf-8'))
ck('final40_audit', 'Final40ContractAuditor.rank' in auto)
ck('oos_audit', 'OosWriteAuditEngine.persist' in (J/'CandidateDecisionFunnelLogEngine.java').read_text('utf-8'))
ck('freshness_guard', 'RuntimeFreshnessGuard.evaluate' in (J/'LanePolicyCompiler.java').read_text('utf-8'))
ck('crossfit', (J/'CrossFittedNumberModelEngine.java').exists())
ck('scoped_calibration', (J/'VersionScopedCalibrationEngine.java').exists())
ck('harnesses10', len(list((ROOT/'tools/v47_harnesses').glob('*Harness.py')))==10)
failed=[k for k,v in checks.items() if not v]
out={'status':'PASS' if not failed else 'FAIL','checks':checks,'failed':failed}
(ROOT/'reports/V47_STATIC_SELFCHECK_47072147.json').write_text(json.dumps(out,ensure_ascii=False,indent=2),'utf-8')
print(json.dumps(out,ensure_ascii=False))
raise SystemExit(1 if failed else 0)
