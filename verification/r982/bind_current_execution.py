"""Link formal requirements/cards/tests to current receipts without inferring full PASS."""
from pathlib import Path
import json,csv,re,argparse,hashlib,subprocess
p=argparse.ArgumentParser();p.add_argument('--plan',type=Path,required=True);a=p.parse_args()
src=Path(__file__).resolve().parents[2];root=src.parent;out=src/'construction/r982'
names=['certificate-final-host-complete','certificate-final-host-complete-integrated','certificate-final-host-complete-extended','certificate-native-migration-complete']
records=[]
for name in names:
 path=root/'evidence'/name/'execution_receipts.json'
 for row in json.loads(path.read_text()):
  row=dict(row,receipt_file=str(path.relative_to(root)),receipt_sha256=hashlib.sha256(path.read_bytes()).hexdigest())
  records.append(row)
byname={}
for row in records:byname.setdefault(row['test'],[]).append(row)
(out/'CURRENT_HOST_EXECUTIONS.json').write_text(json.dumps(records,indent=2))
with(a.plan/'34_TEST_MATRIX/TEST_MATRIX.csv').open(encoding='utf-8-sig',newline='')as f:cases=list(csv.DictReader(f))
extra=['current_named_component_receipts','current_execution_status','current_acceptance_status','current_remaining_scope']
for row in cases:
 defined=row.get('test_definition','');found=[]
 for name,receipts in byname.items():
  if name in defined:found+=receipts
 # Alias is a separately stated augmented test, not the original old positive fixture.
 if 'CertificatePrepareCP12Test' in defined:found+=byname.get('CertificateCommonFutureR982',[])
 row.update(current_named_component_receipts=json.dumps(found,separators=(',',':')),current_execution_status='NAMED_COMPONENT_EXECUTED' if found else 'NOT_RUN_OR_NO_EXACT_BINDING',current_acceptance_status='IN_PROGRESS',current_remaining_scope='Component assertions retain their stated scope. Review this exact formal Arrange/Act/Assertion; unmatched, Android device, customer DB, scheduling and natural Future requirements remain unverified.')
with(out/'TEST_CASES_328.csv').open('w',encoding='utf-8-sig',newline='')as f:
 w=csv.DictWriter(f,fieldnames=list(cases[0]));w.writeheader();w.writerows(cases)
casebyid={row['test_id']:row for row in cases}
for name in ['REQUIREMENTS_1832.csv','DETECTIONS_166.csv']:
 path=out/name
 with path.open(encoding='utf-8-sig',newline='')as f:rows=list(csv.DictReader(f))
 for row in rows:
  ids=re.split(r'[;|,]',row.get('canonical_test','')+';'+row.get('test_case',''))
  linked=[casebyid[id.strip()] for id in ids if id.strip()in casebyid]
  receipts=[r for case in linked for r in json.loads(case['current_named_component_receipts'])]
  row['current_named_component_receipts']=json.dumps(receipts,separators=(',',':'))
  if receipts:row['current_execution_status']='NAMED_COMPONENT_EXECUTED_EXACT_CLAUSE_SCOPE_PENDING'
 with path.open('w',encoding='utf-8-sig',newline='')as f:
  w=csv.DictWriter(f,fieldnames=list(rows[0]));w.writeheader();w.writerows(rows)
cards=json.loads((out/'CARDS_299.json').read_text())
for card in cards:
 related=[r for r in cases if card['path']in re.split(r'[;|]',r.get('file',''))]
 card['formal_test_case_ids']=[r['test_id'] for r in related]
 card['current_component_receipts']=sorted({x['receipt_file']+'#'+x['test'] for r in related for x in json.loads(r['current_named_component_receipts'])})
(out/'CARDS_299.json').write_text(json.dumps(cards,ensure_ascii=False,indent=2))
summary={'host_executions':len(records),'pass_executions':sum(r['status']=='PASS'for r in records),'retained_failed_executions':sum(r['status']=='FAIL'for r in records),'formal_cases':len(cases),'cases_with_named_component_receipts':sum(bool(json.loads(r['current_named_component_receipts']))for r in cases),'full_acceptance':'IN_PROGRESS_NOT_INFERRED_FROM_COMPONENT_COUNTS'}
(out/'CURRENT_EXECUTION_BINDING_SUMMARY.json').write_text(json.dumps(summary,indent=2));print(json.dumps(summary))
