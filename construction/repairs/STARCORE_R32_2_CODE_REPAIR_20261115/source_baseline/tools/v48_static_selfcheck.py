#!/usr/bin/env python3
from pathlib import Path
import json,re
root=Path('.')
checks={}
def ck(name,ok):checks[name]=bool(ok);assert ok,name
app=(root/'app/src/main/java/com/ethk3/novaonepro/AppConfig.java').read_text()
ck('identity_single_manifest','ACTIVE_QUALITY_BUILD_COHORT = V48BuildManifest.COHORT_ID' in app and 'ACTIVE_EVIDENCE_DOMAIN_SCHEMA = V48BuildManifest.SCHEMA_VERSION' in app)
ck('candidate_contract',all(x in app for x in ['V48_FULL_SCAN_CANDIDATE_COUNT = 1200','V48_PROBABILITY_CANDIDATE_COUNT = 60','V48_FORMAL_CANDIDATE_COUNT = 40','V48_UI_DISPLAY_CANDIDATE_COUNT = 12']))
ck('challenger_zero','V47_CHALLENGER_BLEND = 0.00d' in app and 'LiveWeight=0' in (root/'app/src/main/java/com/ethk3/novaonepro/RealtimeCausalProbabilityEngine.java').read_text())
ck('diagnostic_always_on','DiagnosticObservationEngine.persist' in (root/'app/src/main/java/com/ethk3/novaonepro/AutoFetchService.java').read_text())
ck('oos_exact','recordCandidatesDetailed' in (root/'app/src/main/java/com/ethk3/novaonepro/OosActionLogEngine.java').read_text())
ck('wall_clock_freshness','analysisAgeMs' in (root/'app/src/main/java/com/ethk3/novaonepro/RuntimeFreshnessGuard.java').read_text())
ck('v48_diagnosis','V48DecisionFunnelDiagnosisEngine.buildAndPersist' in (root/'app/src/main/java/com/ethk3/novaonepro/RealBetDiagnosisEngine.java').read_text())
ck('db31','DB_VERSION = 31' in app)
ck('java17',"sourceCompatibility JavaVersion.VERSION_17" in (root/'app/build.gradle').read_text())
ck('harness_count',len(list((root/'tools/v48_harnesses').glob('*Harness.py')))==15)
# no current activity constants may retain V46 cohort
ck('no_active_v46_cohort','ACTIVE_QUALITY_BUILD_COHORT = "NOVA_V46' not in app and 'HIERARCHICAL_EVIDENCE_COHORT =\n            "NOVA_V46' not in app)
ck('rank_calibration_java_string_literal',"'|rate='" not in (root/'app/src/main/java/com/ethk3/novaonepro/RankConditionalCalibrationEngine.java').read_text() and '"|rate="' in (root/'app/src/main/java/com/ethk3/novaonepro/RankConditionalCalibrationEngine.java').read_text())
out=root/'reports/V48_STATIC_SELFCHECK_48072148.json';out.write_text(json.dumps({'passed':all(checks.values()),'checks':checks},indent=2),encoding='utf-8')
print(json.dumps({'passed':True,'count':len(checks),'report':str(out)}))
