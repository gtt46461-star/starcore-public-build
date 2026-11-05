#!/usr/bin/env python3
"""Validate this PLAN package, not Android software acceptance."""
from pathlib import Path
import csv, hashlib, json, sys

P=Path(__file__).resolve().parents[1]
errors=[];warnings=[];checks=[]
def check(name,ok,detail=''):
    checks.append({'check':name,'status':'PASS' if ok else 'FAIL','detail':detail})
    if not ok:errors.append(name+': '+detail)
def load(rel):return json.loads((P/rel).read_text())
cards=load('ledgers/cards.json');summary=load('ledgers/package_summary.json');receipts=load('ledgers/source_receipts.json')
ids=[c['id'] for c in cards]
check('card ids unique',len(ids)==len(set(ids)))
check('13 batches mapped',set(c['batch'] for c in cards)=={f'B{i:02}' for i in range(13)})
check('card count agrees',len(cards)==summary['card_count'])
check('all statuses planning only',all(c['status']=='DESIGNED_NOT_IMPLEMENTED' for c in cards))
for c in cards:
    check(c['id']+' complete card',bool(c.get('files') and c.get('steps') and c.get('acceptance') and c.get('rollback') and c.get('issue')))
    check(c['id']+' individual document',(P/'cards'/c['batch']/(c['id']+'.md')).is_file())
    for f in c['files']:
        check(c['id']+' file contract '+f['path'],all(f.get(k) for k in ['path','operation','before','after','methods']))
        if f['baseline_state']=='EXISTING':check(c['id']+' exact baseline included '+f['path'],(P/'baseline_source'/f['path']).is_file())
for r in receipts:
    p=P/'baseline_source'/r['path'];b=p.read_bytes()
    check('source '+r['path'],hashlib.sha256(b).hexdigest()==r['sha256'] and hashlib.sha1(b'blob '+str(len(b)).encode()+b'\0'+b).hexdigest()==r['git_blob_sha1'])
req=load('ledgers/user_requirements_40.json')
check('40 user requirement mappings',len(req)==40 and all(r['cards'] for r in req))
check('requirement references resolve',all(cid in ids for r in req for cid in r['cards']))
phases=load('ledgers/execution_phases.json');seen=set()
eng=load('ledgers/repair_cards.json');eng_ids=[e['id'] for e in eng]
for e in eng:
    check(e['id']+' repair card document',(P/'cards'/'ENG'/(e['id']+'.md')).is_file() and bool(e['steps'] and e['acceptance'] and e['files']))
for p in phases:
    check('phase DAG '+p['phase'],set(p['requires'])<=seen);seen.add(p['phase'])
phase_index={p['phase']:i for i,p in enumerate(phases)}
phase_cards=[cid for p in phases for cid in p['cards']]
check('every card occurs once in sequential batches',sorted(phase_cards)==sorted(ids+eng_ids))
orig_phase_order={'P0%d'%k:k for k in range(9)}
for c in cards:
    check(c['id']+' construction batch placement',c.get('construction_batch') in phase_index and c['id'] in phases[phase_index[c['construction_batch']]]['cards'])
    check(c['id']+' original phase predecessors precede it',all(max(phase_index[d['construction_batch']] for d in cards if d['execution_phase']==x)<phase_index[c['construction_batch']] for x in c['implementation_predecessor_phases']))
    check(c['id']+' assigned group',c['joint_integration_group']=='G'+c['execution_phase'][1:])
check('all referenced existing source present',not summary['source_missing_for_card_files'])
check('new files explicitly additions',not summary['unknown_existing_paths'])
b10=max(phase_index[c['construction_batch']] for c in cards if c['batch']=='B10')
check('GC after mainline migration',all(phase_index[c['construction_batch']]>b10 for c in cards if c['execution_phase']=='P06'))
e=load('evidence/derived/summary.json')
check('report counts',all(e[k]==v for k,v in {'table_count':713,'task_count':60,'metric_count':44,'check_count':96,'r27_task_count':13,'r27_quarantined':13,'r27_claim_failure_count':11,'raw_manifest_members_pass':48}.items()))
check('44 metrics actual states',e['metric_statuses']['INSUFFICIENT']==33)
check('96 execution distinct from quality',e['check_execution_states']=={'EXECUTED':96} and e['check_domain_outcomes']['PASS']==4)
check('original archive members valid',e['raw_manifest_members_fail']==0 and e['raw_manifest_body_matches'])
chart=load('ledgers/charts.json')
if isinstance(chart,dict):chart=chart.get('charts',chart.get('panels',chart))
check('47 chart contracts',isinstance(chart,list) and len(chart)==47)
refs=load('reference_code/contracts/test_results.json')
check('reference contract scope explicit','REFERENCE_CONTRACT_ONLY' in json.dumps(refs))
check('30 reference behavior tests pass',refs['tests']==30 and refs['failures']==0 and refs['errors']==0 and refs['status']=='PASS')
st=load('reference_code/storage/storage_contract_receipt.json')
check('12 storage protocol tests pass',st['tests']==12 and st['failures']==0 and st['errors']==0 and st['passed'])
review=load('evidence/review_experiment/review_results.json')
check('historical protocol identity',review['spec_sha256']==hashlib.sha256((P/'evidence/review_experiment/replay_spec.json').read_bytes()).hexdigest())
check('historical input identity',review['input_csv_sha256']==hashlib.sha256((P/'evidence/derived/draws_5000.csv').read_bytes()).hexdigest())
check('historical scope and finite experiment',review['candidate_count']==100 and review['rows']==5000 and review['new_future_evidence']==0 and not review['production_code_tested'])
config=load('ledgers/acceptance_config.json')
check('budget ordering',config['disk_normal_soft_mib']<config['disk_normal_hard_admission_mib']<config['disk_transient_target_mib']<config['disk_transient_hard_admission_mib'])
check('memory budgets and locked acceptance',0<config['memory_heap_resume_fraction']<config['memory_heap_high_water_fraction']<config['memory_heap_emergency_fraction']<1 and config['memory_oom_allowed']==0 and config['memory_constant_workload_last_vs_first_p95_live_heap_ratio_max']==1.10)
check('no production success claimed',summary['android_implementation']=='NOT_PERFORMED_THIS_PLAN_DELIVERY' and summary['device_test']=='NOT_RUN' and summary['natural_quality']=='NOT_PROVEN')
policy=load('ledgers/execution_policy.json')
check('R31.3 explicit revision',summary['plan_version']==policy['version']=='R31.3-CONSTRUCTION-20261105')
check('resume existing work',policy['resume']['never_reset_existing_r31_to_report_baseline'] and policy['resume']['do_not_override_newer_progress'])
check('one open batch',policy['sequence']['open_batches_max']==1 and policy['sequence']['next_batch_requires_previous_receipt'])
r=policy['closure']['ratchet']
check('ratchet: no new failures, owned failures fixed, zero from K20',not r['new_failures_allowed'] and r['known_failures_owned_by_batch_must_pass'] and r['zero_known_failures_from']=='K20' and r['compilation_failure_always_blocks'])
check('carry is bounded and never PASS',policy['closure']['carry']['max_per_batch']==2 and policy['closure']['carry']['carried_is_not_pass'] and policy['closure']['carry']['latest_target']=='K20')
check('batch ids are construction order',[p['phase'] for p in phases]==[f'K{i:02}' for i in range(1,len(phases)+1)])
sys.path.insert(0,str(P/'tools'))
sys.dont_write_bytecode=True
import r31_gate
gate_errors=r31_gate.check_plan()
check('r31 gate check-plan',not gate_errors,'; '.join(gate_errors))
check('physical absence not source blocker',all(not p['physical_device_is_source_prerequisite'] for p in phases))
check('all phases use one execution policy',all(p['execution_policy']=='ledgers/execution_policy.json' for p in phases))
check('acceptance thresholds byte-preserved',hashlib.sha256((P/'ledgers/acceptance_config.json').read_bytes()).hexdigest()==policy['preservation']['acceptance_config_sha256'])
check('no weakened assertion policy',not policy['verification']['weaken_assertions_limits_or_acceptance'])
check('layers not conflated',policy['closure']['blocking_levels']==['LOCAL'] and sorted(policy['closure']['non_blocking_levels'])==['DEVICE','EMU','NATURAL'] and not policy['verification']['physical_absence_blocks_source_repair'])
check('all current card references retained',policy['preservation']['cards']==len(cards) and policy['preservation']['batches']==13)
manifest=P/'MANIFEST_SHA256.json'
if manifest.exists():
    m=json.loads(manifest.read_text())
    for x in m['files']:
        f=P/x['path'];check('package member '+x['path'],f.is_file() and f.stat().st_size==x['bytes'] and hashlib.sha256(f.read_bytes()).hexdigest()==x['sha256'])
else:warnings.append('Manifest has not yet been sealed; run this validator again after final sealing.')
result={'scope':'PLAN_PACKAGE_AND_REFERENCE_CONTRACT_INTEGRITY_ONLY','status':'PASS' if not errors else 'FAIL','checks':len(checks),'pass':sum(x['status']=='PASS' for x in checks),'fail':len(errors),'errors':errors,'warnings':warnings,'android_implementation':'NOT_PERFORMED','device_test':'NOT_RUN','natural_quality':'NOT_PROVEN'}
print(json.dumps(result,ensure_ascii=False,indent=2))
sys.exit(bool(errors))
