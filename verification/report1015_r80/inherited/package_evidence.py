#!/usr/bin/env python3
"""Package text/hash-only inherited evidence; never copy runtime DB/model/classes."""
from pathlib import Path
import argparse, hashlib, json, shutil
p=argparse.ArgumentParser();p.add_argument('--private-root',type=Path,required=True);p.add_argument('--out',type=Path,required=True);a=p.parse_args()
r=a.private_root.resolve();o=a.out.resolve();o.mkdir(parents=True,exist_ok=False)
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
def write(p,v):p.write_text(json.dumps(v,indent=2)+'\n')
final=r/'inherited_final_diagnostic';assert (final/'summary.json').exists(),'FINAL_RUN_NOT_TERMINAL'
# Flat explicitly allowlisted evidence only. Compiled/source model fixtures stay private.
for name,folder in [('owners',final),('prescope',r/'prescope_final')]:
 dest=o/name;dest.mkdir()
 for f in sorted(folder.iterdir()):
  if f.is_file() and f.suffix in ('.json','.log','.exit'):
   shutil.copy2(f,dest/f.name)
# Preserve the intervening schema-only failure, before required immutable fixture identities were supplied.
dest=o/'schema_only_attempt';dest.mkdir()
for suffix in ['-command.json','.log','.exit']:
 f=r/'inherited_current_schema'/('outbox-transaction-current-schema'+suffix);shutil.copy2(f,dest/f.name)
shutil.copy2(r/'inherited_current_schema'/'schema_adaptations.json',dest/'schema_adaptations.json')
commands=o/'commands';commands.mkdir()
for f in (r/'inherited_commands').iterdir():
 if f.is_file() and f.suffix in ('.log','.exit'):shutil.copy2(f,commands/f.name)
write(commands/'invocations.json',{
 'prescope':['python3','verification/report1015_r79/prescope/run.py','--config',str(r/'config.json'),'--schema',str(r/'host-schema.db'),'--out',str(r/'prescope_final')],
 'final_diagnostic':['python3','verification/report1015_r80/inherited/run.py','--config',str(r/'config.json'),'--schema',str(r/'host-schema.db'),'--ast',str(r.parent/'r80_checkpoint_host/owners/fixture.ast'),'--out',str(final)],
 'cwd':str(Path(__file__).resolve().parents[3])})
b=r/'prescope_final/service-bytecode.txt';txt=b.read_text()
write(o/'prescope/service-bytecode-review.json',{'raw_output_sha256':sha(b),'completedBeforeScopeR79_call_count':txt.count('RuntimeResourceAuditR927.completedBeforeScopeR79:'),'private_raw_output_path':str(b),'output_omitted_from_package':'Only relevant count and hash retained; no product bytecode disassembly packaged.'})
records=json.loads((final/'executions.json').read_text());by={x['name']:x for x in records}
required=['post-batch','reserved-batch','post-repair','draw-ingest','fairness-low','capture-timely','capture-expired','capture-crossing','residual-owners','outbox-recovery','post-retry-current-schema','outbox-transaction-monotonic-expiry']
current_pass=all(by[x]['exit_code']==0 for x in required)
identity=json.loads((final/'input_identity.json').read_text());manifest=json.loads((r/'product_hashes.json').read_text())
assert manifest==json.loads((final/'product_hashes.json').read_text())['sha256']
findings={
 'historical_outbox_mini_schema':{'result':'FAIL_BEFORE_ASSERTIONS','cause':'R967 handwritten table omits current lease_owner column','evidence':'owners/outbox-transaction.log'},
 'historical_post_mini_schema':{'result':'FAIL_BEFORE_ASSERTIONS','cause':'R976 handwritten schema omits current research_candidate_cal_model_r932 table','evidence':'owners/post-retry.log'},
 'schema_only_outbox_attempt':{'result':'FAIL_BEFORE_ASSERTIONS','cause':'Original sparse label fixture omits mandatory immutable identity columns in current product schema','evidence':'schema_only_attempt/outbox-transaction-current-schema.log'},
 'historical_wall_clock_expiry_driver':{'result':'FAIL_ORIGINAL_ASSERTION_AFTER_13_PASS','cause':'claim(e1.lease+1) advances only its supplied wall clock; current live same-process owner is fenced by monotonicUntil, so successor is null and prior owner remains valid','evidence':'owners/outbox-transaction-current-schema.log'},
 'actual_monotonic_expiry':{'result':by['outbox-transaction-monotonic-expiry']['status'],'cause':'Separate diagnostic preserves original assertions, additionally proves wall-clock jump cannot steal live owner, then waits actual unmodified 45-second monotonic lease before reclaim and stale-owner checks','evidence':'owners/outbox-transaction-monotonic-expiry.log'}
}
write(o/'historical_fixture_findings.json',findings)
write(o/'receipt.json',{'status':'CURRENT_CONTRACTS_PASS_WITH_RECORDED_HISTORICAL_FIXTURE_FAILURES' if current_pass else 'CURRENT_CONTRACT_FAILURE',
 'original_unadapted_suite_status':'FAIL','original_final_runner_exit':int((r/'inherited_commands/final-diagnostic.exit').read_text()),
 'current_contracts_pass':current_pass,'prescope_checks':31,'prescope_pass':json.loads((o/'prescope/summary.json').read_text())['results'][0]['exit_code']==0,
 'current_contract_scenarios':len(required),'current_contract_results':[by[x] for x in required],
 'source_commit':identity['source_head'],'product_classes':identity['product_classes'],
 'product_manifest_file_sha256':sha(r/'product_hashes.json'),'product_manifest_canonical_json_sha256':identity['product_manifest_sha256'],
 'product_manifest_serialization_note':'File digest uses original pretty JSON bytes; canonical digest uses json.dumps(sorted keys). Both refer to the same 3441 path/hash mapping.',
 'source_and_product_unchanged':True,'product_overlay':False,'original_fixture_sources_unchanged':True,
 'changed_product_budgets_formulas_or_lease_duration':False,
 'scope':'Actual sealed Gradle product, real host SQLite and explicit synthetic inputs. Not Android/ART, phone latency, natural Future qualification, original device backlog performance, or full dynamic-owner/service-boundary audit.',
 'unavailable_fixtures':['Original phone database/backlog and natural runtime history are not supplied to this host suite.'],
 'db_model_generated_buildconfig_keyprovider_or_class_bytes_packaged':False})
for f in o.rglob('*'):
 if f.is_file():assert f.suffix in ('.json','.log','.exit'),f
checks=[f'{sha(f)}  {f.relative_to(o)}' for f in sorted(o.rglob('*')) if f.is_file()]
(o/'SHA256SUMS').write_text('\n'.join(checks)+'\n')
print(json.dumps({'evidence':str(o),'files':len(checks),'receipt_sha256':sha(o/'receipt.json'),'current_contracts_pass':current_pass},indent=2))
