"""Bind every formal card, requirement and detection to actual source without inventing acceptance."""
from pathlib import Path
import argparse,csv,json,hashlib,subprocess,re
p=argparse.ArgumentParser();p.add_argument('--plan',type=Path,required=True);a=p.parse_args()
src=Path(__file__).resolve().parents[2];out=src/'construction/r982';out.mkdir(parents=True,exist_ok=True)
baseline='8e188bd4726ad205f971428876967de5f89fd633'
sha=lambda b:hashlib.sha256(b).hexdigest()
cards=[]
for f in sorted((a.plan/'31_FILE_CONSTRUCTION_CARDS/card_data').glob('*.json')):
 d=json.loads(f.read_text());path=d['path'];actual=src/path
 try:before=subprocess.check_output(['git','show',baseline+':'+path],cwd=src,stderr=subprocess.DEVNULL)
 except subprocess.CalledProcessError:before=None
 after=actual.read_bytes() if actual.is_file() else None
 state='DELETE_VERIFIED' if d['disposition']=='DELETE' and after is None else 'CANONICAL_BYTES_MATCH' if after is not None and sha(after)==d.get('after_sha256') else 'ADAPTED_REVIEW_REQUIRED'
 diff=subprocess.run(['git','diff',baseline,'--',path],cwd=src,capture_output=True).stdout
 (out/'diffs').mkdir(exist_ok=True);(out/'diffs'/(d['card_id']+'.diff')).write_bytes(diff)
 cards.append({'card_id':d['card_id'],'path':path,'disposition':d['disposition'],'original_before_sha256':d.get('before_sha256'),'verified_baseline_before_sha256':sha(before) if before else None,'canonical_after_sha256':d.get('after_sha256'),'actual_after_sha256':sha(after) if after else None,'source_alignment':state,'build_input':path.startswith(('app/src/','app/build/generated/')),'methods':[x['member_key'] for x in d.get('changed_members',[])],'diff':'diffs/'+d['card_id']+'.diff','database_changes': 'Review method/card SQL and current actual schema diff; not inferred from class name','caller_and_consumer_evidence':'release/r982_source_calls.tsv.gz; consumer runtime acceptance still required','test_evidence_root':'delivery evidence/current-*; evidence/final-*','acceptance_status':'IN_PROGRESS','remaining_blocker':'Per-method runtime, failure/recovery and applicable device evidence not closed by byte equality'})
(out/'CARDS_299.json').write_text(json.dumps(cards,ensure_ascii=False,indent=2))
for folder,name,dest in [('38_REQUIREMENTS_TRACEABILITY','REQUIREMENT_TO_FILE_TRACEABILITY.csv','REQUIREMENTS_1832.csv'),('37_DETECTION_TRACEABILITY','DETECTION_TO_CONSTRUCTION_TRACEABILITY.csv','DETECTIONS_166.csv')]:
 with (a.plan/folder/name).open(encoding='utf-8-sig',newline='') as f:rows=list(csv.DictReader(f))
 extra=['current_source_bindings','current_execution_status','current_acceptance_status','current_blocker']
 for row in rows:
  paths=re.split(r'[;|]',row.get('canonical_files','') or row.get('canonical_remediation_files','') or row.get('target_file',''))
  bindings=[]
  for path in paths:
   path=path.strip();file=src/path
   if path.startswith(('app/','release/','scripts/','verification/')):bindings.append({'path':path,'sha256':sha(file.read_bytes()) if file.is_file() else None})
  row.update(current_source_bindings=json.dumps(bindings,separators=(',',':')),current_execution_status='REVIEW_CURRENT_LOG_SCOPE',current_acceptance_status='IN_PROGRESS',current_blocker='No inherited plan PASS; named current assertion coverage and device applicability require individual adjudication')
 with (out/dest).open('w',encoding='utf-8-sig',newline='') as f:
  w=csv.DictWriter(f,fieldnames=list(rows[0]));w.writeheader();w.writerows(rows)
 print(dest,len(rows))
print('cards',len(cards),'alignment',{s:sum(c['source_alignment']==s for c in cards) for s in {c['source_alignment'] for c in cards}})
