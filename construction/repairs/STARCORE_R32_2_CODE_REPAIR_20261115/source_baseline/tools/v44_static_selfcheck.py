#!/usr/bin/env python3
from pathlib import Path
import hashlib, json, re, sys
try:
    import javalang
except Exception as exc:
    print(json.dumps({'ok':False,'fatal':'javalang_missing','error':repr(exc)},ensure_ascii=False,indent=2))
    raise SystemExit(2)
ROOT=Path(__file__).resolve().parents[1]
JAVA_ROOT=ROOT/'app/src/main/java'
PKG=JAVA_ROOT/'com/ethk3/novaonepro'
VERSION='V44_NOVA_DIAGNOSIS_FREQUENCY_STABILITY_20260720_44157296'
checks=[]
def add(name,ok,detail=''):
    checks.append({'name':name,'ok':bool(ok),'detail':detail})
def text(name):
    return (PKG/name).read_text(encoding='utf-8')
java_files=list(JAVA_ROOT.rglob('*.java'))
xml_files=list((ROOT/'app/src/main').rglob('*.xml'))
parse=[]; lambdas=[]; bad_imports=[]
for p in java_files:
    rel=str(p.relative_to(ROOT));src=p.read_text(encoding='utf-8')
    try:
        tree=javalang.parse.parse(src)
        for _,node in tree:
            if isinstance(node,javalang.tree.LambdaExpression): lambdas.append(rel)
        for imp in tree.imports:
            q=imp.path
            if not (q.startswith('java.') or q.startswith('android.') or q.startswith('org.json') or q.startswith('com.ethk3.novaonepro')):
                bad_imports.append(rel+':'+q)
    except Exception as e: parse.append({'file':rel,'error':repr(e)})
add('java_file_count_119',len(java_files)==119,str(len(java_files)))
add('xml_file_count_64',len(xml_files)==64,str(len(xml_files)))
add('all_java_parse',not parse,json.dumps(parse,ensure_ascii=False))
add('java7_no_lambda',not lambdas,json.dumps(lambdas,ensure_ascii=False))
add('no_third_party_runtime_import',not bad_imports,json.dumps(bad_imports,ensure_ascii=False))
app=text('AppConfig.java');lane=text('LanePolicyCompiler.java');auth=text('DecisionAuthorizationEngine.java')
diag=text('DecisionFunnelDiagnosisEngine.java');real=text('RealBetDiagnosisEngine.java');main=text('MainActivity.java');fix=text('DiagnosisFixPlanEngine.java')
add('v44_identity',VERSION in app and 'V44.0.44157296' in app)
add('fixed_single_position_k3','FIXED_K = 3' in app and 'ALLOW_MULTI_POSITION_BET = false' in app)
add('controlled_floor_minima',all(x in app for x in ['V44_CONTROLLED_MIN_FINAL_P = 0.350d','V44_CONTROLLED_MIN_MODEL_P = 0.345d','V44_CONTROLLED_GATE = 0.360d','V44_CONTROLLED_MAX_NEGATIVE_EDGE = 0.010d']))
add('lane_quality_quorum','FLOOR_QUALITY_QUORUM_LOW' in lane and 'PROBE_QUALITY_QUORUM_LOW' in lane and 'qualityVotes(' in lane)
add('lane_hard_quality_floor',all(x in lane for x in ['FLOOR_COVERAGE_LOW','FLOOR_THIRD_LOW','FLOOR_COUNTER_HIGH','FLOOR_DISTILL_RISK']))
add('normal_edge_non_negative','BET_DOMAIN_NORMAL' in auth and 'CONTRACT_PROBABILITY_EPSILON' in auth)
add('controlled_edge_minus_one_percent','V44_CONTROLLED_MAX_NEGATIVE_EDGE' in auth)
add('diagnosis_read_only_contract',all(x in diag for x in ['never creates Trade','changes probability','changes a gate','counterfactual evidence']))
for forbidden in ['insertTrade','createTrade','saveTrade','DECISION_BET +','= AppConfig.DECISION_BET']:
    add('diagnosis_forbidden_'+re.sub(r'\W+','_',forbidden),forbidden not in diag,forbidden)
add('diagnosis_has_reject_funnel',all(x in diag for x in ['primaryRejects','conditionFails','topReject','frequencyCause']))
add('diagnosis_has_stability_metrics',all(x in diag for x in ['brier','calibrationGap','wilsonLower','maxLoseStreak','rollingSwing']))
add('diagnosis_has_counterfactual_lane',all(x in diag for x in ['controlledReady','controlledFrequency','controlledWilsonLower']))
add('real_bet_integrates_funnel','DecisionFunnelDiagnosisEngine.buildAndPersist' in real and 'v44DecisionFunnel' in real)
add('copy_limit_expanded','V41_DIAGNOSIS_CACHE_MAX_CHARS' in main and 'v44_decision_funnel_detail' in main and 'UI_DIAGNOSIS_CLIPBOARD_CHUNK' in main)
add('fix_plan_targets_actual_owners',all(x in fix for x in ['DecisionFunnelDiagnosisEngine.java','LanePolicyCompiler.java','DecisionAuthorizationEngine.java']))
# Every OOS column read by V44 diagnostic must exist in DbHelper migration/schema.
db=text('DbHelper.java')
m=re.search(r'String\[\] cols = new String\[\]\{(.*?)\};',diag,re.S)
cols=re.findall(r'"([a-zA-Z0-9_]+)"',m.group(1)) if m else []
missing=[c for c in cols if ('"'+c+' ') not in db and ('"'+c+'"') not in db]
add('diagnostic_columns_exist',not missing,','.join(missing))
# Unique real BET owner: diagnose broad textual owners without claiming comments are execution.
java_with_auth=[]
for p in PKG.glob('*.java'):
    s=p.read_text(encoding='utf-8')
    if 'DecisionAuthorizationEngine.authorize' in s: java_with_auth.append(p.name)
add('authorization_call_sites_audited',set(java_with_auth).issubset({'DecisionGateEngine.java','DecisionAuthorizationEngine.java'}),','.join(java_with_auth))
# Build files identity.
build=(ROOT/'app/build.gradle').read_text(encoding='utf-8')
add('gradle_identity','versionCode 440441572' in build and "versionName 'V44.0.44157296'" in build)
# Model challenger result exists and is chronological.
bench=ROOT/'reports/V44_MODEL_CHALLENGER_BENCHMARK_44157296.json'
if bench.exists():
    data=json.loads(bench.read_text(encoding='utf-8'))
    add('challenger_time_split',data.get('split',{}).get('train_issues')==6000 and data.get('split',{}).get('validation_issues')==2000,str(data.get('split')))
else:add('challenger_time_split',False,'missing')
# SHA manifest for the package itself.
manifest=[]
for p in sorted(x for x in ROOT.rglob('*') if x.is_file() and '.git/' not in str(x)):
    if p.name=='V44_PROJECT_MANIFEST_44157296.sha256': continue
    manifest.append(hashlib.sha256(p.read_bytes()).hexdigest()+'  '+str(p.relative_to(ROOT)))
(ROOT/'reports/V44_PROJECT_MANIFEST_44157296.sha256').write_text('\n'.join(manifest)+'\n',encoding='utf-8')
ok=all(x['ok'] for x in checks)
out={'version':VERSION,'ok':ok,'java_files':len(java_files),'xml_files':len(xml_files),'checks':checks}
print(json.dumps(out,ensure_ascii=False,indent=2))
raise SystemExit(0 if ok else 1)
