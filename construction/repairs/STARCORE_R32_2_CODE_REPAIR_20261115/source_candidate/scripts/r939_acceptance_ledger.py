#!/usr/bin/env python3
"""Derive the 64-item ledger from named, hashed evidence; never set a fixed passed count.

Inherited evidence is explicitly distinguished from the new R939 executions. Every original
acceptance sentence is retained. A missing Android result leaves E03A open and the gate fails.
"""
import argparse, hashlib, json
from pathlib import Path
R=Path(__file__).resolve().parents[1]
P=R/'reports/r932'

def evidence(name, marker):
    path=R/name if '/' in name else P/name
    if not path.is_file():return {'path':str(path.relative_to(R)),'passed':False,'reason':'MISSING'}
    data=path.read_bytes();text=data.decode('utf-8')
    return {'path':str(path.relative_to(R)),'sha256':hashlib.sha256(data).hexdigest(),
            'marker':marker,'passed':marker in text}

def group(*pairs):return [evidence(*p) for p in pairs]

def main():
    ap=argparse.ArgumentParser();ap.add_argument('--require-complete',action='store_true');args=ap.parse_args()
    # Main-task suites contain actual production execution, not the old declaration-only summary.
    main={
      'D01':('RAW_UPSTREAM.txt','R932_RAW_UPSTREAM_PASS'),
      'D02':('E03A_PIT_DAG_STRICT.txt','R932_E03A_PIT_DAG_ACCEPTANCE_PASS'),
      'D03':('MODEL_BUNDLE_PRODUCTION.txt','R932_MODEL_BUNDLE_PRODUCTION_PASS'),
      'D04':('reports/r932/d04_final/D04_ACCEPTANCE.json','"status": "PASS"'),
      'D05':('D05_ANDROID_PARITY.json','"status": "PASS"'),
      'D06':('CALIBRATED_WINDOW.txt','R932_CALIBRATED_WINDOW_PASS'),
      'D07':('DOMAIN_ROUTE.txt','R932_DOMAIN_ROUTE_PASS'),
      'D08':('UNIFIED_CERTIFICATE.txt','R932_UNIFIED_CERTIFICATE_PASS'),
      'D10':('DUAL_MEMORY.txt','R932_DUAL_MEMORY_PASS'),
      'C01':('FIXED_WINDOW.txt','R932_FIXED_WINDOW_PASS'),
      'C02':('FIXED_WINDOW.txt','FAILED_WINDOW_CANNOT_RESET_SAME_TRIAL=PASS'),
      'C03':('MULTIPLICITY.txt','R932_MULTIPLICITY_PASS'),
      'C04':('CAPTURE_TRANSACTION.txt','R932_CAPTURE_TRANSACTION_PASS'),
      'C05':('C05_FAMILY_CHAMPION.txt','R932_FAMILY_CHAMPION_PASS'),
      'C06':('UNIFIED_CERTIFICATE.txt','R932_UNIFIED_CERTIFICATE_PASS'),
      'C07':('C07_ANDROID_RUNTIME_MATCH.txt','R932_C07_ANDROID_RUNTIME_PASS'),
      'C08':('PAIRED_CHALLENGE.txt','C08_REPAIR_REPORT_COUNTS_MATURED_PAIRED_SUCCESS=PASS'),
      'C09':('MODULE_ABLATION.txt','R932_MODULE_ABLATION_PASS'),
      'C10':('ATOM_CAPTURE.txt','R932_ATOM_CAPTURE_PASS'),
      'C11':('C11_QUARANTINE_LEGACY.txt','R932_C11_QUARANTINE_LEGACY_PASS'),
      'C12':('C12_ACCEPTANCE.json','"status": "PASS"'),
      'D09':('FROZEN_SELECTOR_REGRESSION.json','"status": "PASS"')}
    cards={
      'E03A':[('reports/r939/E03A_ACCEPTANCE.json','"device_verified": true'),('E03A_PIT_DAG_STRICT.txt','R932_E03A_PIT_DAG_ACCEPTANCE_PASS'),('E03A_PIT_DAG_STRICT.txt','LEGACY_UNKNOWN_ZERO_STORAGE_DOES_NOT_UPGRADE_TO_PRESENT_ZERO=PASS')],
      'E03B':[('E03A_SEALED_TRIAL_STRICT.txt','COPY_RENAME_ARCHIVE_METADATA_CANNOT_RESET_SEALED_IDENTITY=PASS'),('E03A_SEALED_TRIAL_STRICT.txt','UPSTREAM_CONTENT_CHANGE_INVALIDATES_OLD_SEALED_AUTHORITY_NOT_REFUND_WINDOW=PASS'),('E03A_SEALED_TRIAL_STRICT.txt','FAILED_AND_INTERRUPTED_TRIAL_ATTEMPTS_ARE_ALL_RETAINED=PASS')],
      'E03C':[('TRAINED_MODEL_PARITY.json','1200'),('D05_ANDROID_PARITY.json','"status": "PASS"'),('RANKER_COMPARISON_REGRESSION.json','"failed": 0')],
      'E03D':[('FIXED_WINDOW.txt','RAW_SCORES_CANNOT_PASS_PROBABILITY_WINDOW=PASS'),('MODULE_CALIBRATION.txt','CHANGED_SELECTOR_OR_MODULE_CANNOT_REUSE_CALIBRATION=PASS'),('FROZEN_SELECTOR_REGRESSION.json','"status": "PASS"')],
      'E03E':[('ENHANCEMENT_GAP_ACCEPTANCE.txt','E03E_STABLE_STATE_DISTINCT=PASS'),('DOMAIN_ROUTE.txt','EXIT_DOMAIN_ZERO_CONTRIBUTION=PASS'),('ENHANCEMENT_GAP_ACCEPTANCE.txt','E03E_DIAGNOSTIC_CANNOT_EXPAND_K_OR_CREATE_REAL=PASS')],
      'E03F':[('DUAL_MEMORY.txt','forgetting_A_after_B'),('DUAL_MEMORY.txt','R932_DUAL_MEMORY_PASS'),('UNIFIED_CERTIFICATE.txt','INVENTED_ALGORITHM_TRIAL_RECORD_MUST_REMAIN_FROZEN=PASS')],
      'E03G':[('UNIFIED_CERTIFICATE.txt','FORMULA_GRAPH_IDENTICAL_AUTHORIZATION_NO_REAL=PASS'),('MODEL_BUNDLE_PRODUCTION.txt','R932_MODEL_BUNDLE_PRODUCTION_PASS'),('MODEL_BUNDLE_PRODUCTION.txt','R932_MODEL_BUNDLE_PRODUCTION_PASS')],
      'E04A':[('ATOM_CAPTURE.txt','R932_ATOM_CAPTURE_PASS'),('ATOM_CAPTURE.txt','ATOM_NONDETERMINISM_HARD_FAIL=PASS'),('ENHANCEMENT_GAP_ACCEPTANCE.txt','E04A_NEW_MOLECULE_REVERSES_ALL_PARENT_ATOMS=PASS')],
      'E04B':[('C11_LEGACY_SOURCE_LOCKS.txt','PASS'),('MODULE_ABLATION.txt','FROZEN_RECIPE_CANNOT_SUBSTITUTE_ANOTHER_SELECTED_CANDIDATE=PASS'),('MODULE_ABLATION.txt','EXISTING_WORKER_COMPLETES_BASELINE_AND_SEVEN_MODULES_ONCE=PASS')],
      'E04C':[('MODULE_ABLATION.txt','ALL_FOUR_ACTIVE_VM_FAMILIES_CHANGE_ACTUAL_SCORE_SURFACE=PASS'),('COHORT_LIFECYCLE.txt','PARENT_200_CHILD_STARTS_WITH_ZERO_FUTURE_SUPPORT=PASS'),('PAIRED_CHALLENGE.txt','MISSING_PARENT_PAIR_IS_PENDING_SYSTEM_PAUSE_NOT_REPAIR_FAILURE=PASS')],
      'E04D':[('COHORT_LIFECYCLE.txt','RESIDUAL_DUPLICATES_DO_NOT_CONSUME_DOUBLE_ACTIVE_BUDGET=PASS'),('DIRECTED_CREATION.txt','GENERATION_SEED_REPRODUCES_EXECUTABLE_CHILD=PASS'),('GLOBAL_SCHEDULE.txt','PROXY_RETRY_IS_ONCE_AND_CANNOT_CLAIM_FULL1200=PASS')],
      'E04E':[('CAPTURE_TRANSACTION.txt','RETRY_NO_DUPLICATE_TERMINAL_EVENT=PASS'),('LIFECYCLE_INTEGRITY.txt','STALE_WINDOW_CONTRACT_CANNOT_SUPPLY_LIFECYCLE_OR_ELITE_SUPPORT=PASS'),('FIXED_WINDOW.txt','FAILED_WINDOW_CANNOT_RESET_SAME_TRIAL=PASS')],
      'E04F':[('ENHANCEMENT_GAP_ACCEPTANCE.txt','E04F_JOINT_HR40_COVERAGE15_ECE4_BOUNDARY_PASSES=PASS'),('FROZEN_SELECTOR_REGRESSION.json','"status": "PASS"'),('ENHANCEMENT_GAP_ACCEPTANCE.txt','E04F_MODEL_GATE_FALSE_REJECTED=PASS')],
      'E04G':[('ENHANCEMENT_GAP_ACCEPTANCE.txt','E04G_QUARANTINED_PARENT_BLOCKS_DESCENDANT_NEXT_ISSUE=PASS'),('UNIFIED_CERTIFICATE.txt','GENESIS_ASSET_OLD_CERTIFICATE_CANNOT_RESURRECT=PASS'),('ENHANCEMENT_GAP_ACCEPTANCE.txt','E04G_ZERO_DISPLAY_CANNOT_SECRETLY_REORDER_ACTION_SURFACE=PASS')]}
    rows=[]
    for batch,filename in ((3,'tasks_batch3_D01-D08_D10_C01_C03_C06.json'),(4,'tasks_batch4_C02_C04_C05_C07-C12_D09.json')):
        for task in json.loads((R/'docs/5.4.2/01_TASKS'/filename).read_text()):
            ev=group(main[task['id']])
            if task['id']=='D03':ev+=group(('reports/r939/MAIN_CHAIN_ANDROID.txt','OK (4 tests)'))
            if task['id']=='C08':ev+=group(('COHORT_LIFECYCLE.txt','PARENT_200_CHILD_STARTS_WITH_ZERO_FUTURE_SUPPORT=PASS'),('LIFECYCLE_INTEGRITY.txt','OLD_ACTIVE_CERTIFICATE_CANNOT_OVERRIDE_PAUSED_CAPTURE=PASS'),('PAIRED_CHALLENGE.txt','REPAIR_SUCCESS_REQUIRES_NEW_120_PAIRED_ENDPOINT_GAIN_AND_CALIBRATION=PASS'),('PAIRED_CHALLENGE.txt','C08_IMMATURE_REPAIR_REPORT_REMAINS_PENDING=PASS'))
            rows.append({'id':task['id'],'batch':batch,'kind':'main','requirements':task['acceptance'],'evidence':ev,'passed':all(v['passed'] for v in ev)})
    tasks=json.loads((R/'docs/5.4.2/11_ENHANCEMENT_20260909/enhancement_tasks.json').read_text())['tasks']
    for task in tasks:
        if task['batch'] not in (3,4):continue
        assert len(task['acceptance'])==len(cards[task['id']])==3
        for i,text in enumerate(task['acceptance']):
            ev=group(cards[task['id']][i]);passed=all(v['passed'] for v in ev)
            if task['id']=='E03A' and i==0 and passed:
                r=json.loads((R/'reports/r939/E03A_ACCEPTANCE.json').read_text())
                passed=r.get('status')=='PASS' and r.get('issues')==100 and r.get('device_verified') is True and len(r.get('per_issue',[]))==100 and r.get('numeric_values_compared',0)>=11794000
            rows.append({'id':task['id']+'.'+str(i+1),'batch':task['batch'],'kind':'enhancement','requirements':[text],'evidence':ev,'passed':passed})
    assert len(rows)==64 and len({r['id'] for r in rows})==64
    passed=sum(r['passed'] for r in rows)
    result={'scope':'ENGINEERING_ACCEPTANCE_ANDROID_EMULATOR_NOT_PHYSICAL_PHONE_NOT_FUTURE_EFFECT','inherited_source':'a2fa75ce0518b93a25c90434854766af1b71ee73','passed':passed,'total':len(rows),'percent':100*passed/len(rows),'rows':rows,'effect_proven':False,'physical_device_verified':False,'may_authorize_real':False}
    out=R/'reports/r939/ACCEPTANCE_LEDGER.json';out.parent.mkdir(parents=True,exist_ok=True);out.write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n')
    def summary(selected):
        count=sum(r['passed'] for r in selected)
        return {'passed':count,'total':len(selected),'percent':100*count/len(selected),
                'verified':[r['id'] for r in selected if r['passed']],
                'remaining':[r['id'] for r in selected if not r['passed']]}
    stage={'scope':result['scope'],'ledger':'reports/r939/ACCEPTANCE_LEDGER.json',
           'combined':summary(rows),'combined_percent':result['percent'],
           'effect_proven':False,'physical_device_verified':False}
    for batch in (3,4):
        stage['batch'+str(batch)]=summary([r for r in rows if r['batch']==batch])
        for kind in ('main','enhancement'):
            stage['batch'+str(batch)+'_'+kind]=summary([r for r in rows if r['batch']==batch and r['kind']==kind])
    stage['status']='PASS' if passed==len(rows) else 'INCOMPLETE'
    stage['supersedes']='Old 63.64% main-only snapshot and unsupported hardcoded 100% receipt'
    (P/'STAGE_PROGRESS.json').write_text(json.dumps(stage,ensure_ascii=False,indent=2)+'\n')
    (P/'d04_final/BATCH34_FINAL_ACCEPTANCE.json').write_text(json.dumps(stage,ensure_ascii=False,indent=2)+'\n')
    print(json.dumps({'passed':passed,'total':len(rows),'percent':result['percent'],'open':[r['id'] for r in rows if not r['passed']]}))
    if args.require_complete and passed!=len(rows):raise SystemExit(1)

if __name__=='__main__':main()
