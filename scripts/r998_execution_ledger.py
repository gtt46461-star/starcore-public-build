#!/usr/bin/env python3
"""Preserve exact canonical denominators; current execution starts unverified."""
import argparse, ast, collections, csv, gzip, hashlib, json, sqlite3, subprocess
from pathlib import Path
csv.field_size_limit(256 * 1024 * 1024)

def sha(p):
    h=hashlib.sha256()
    with p.open('rb') as f:
        for b in iter(lambda:f.read(1024*1024),b''):h.update(b)
    return h.hexdigest()

def rows(p):
    with p.open(encoding='utf-8-sig',newline='') as f:return list(csv.DictReader(f))

def save_json(p,v):p.write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')

def csv_write(p,rs):
    fields=list(dict.fromkeys(k for r in rs for k in r))
    with p.open('w',encoding='utf-8',newline='') as f:
        w=csv.DictWriter(f,fieldnames=fields);w.writeheader();w.writerows(rs)

def main():
    a=argparse.ArgumentParser();a.add_argument('--plan',type=Path,required=True);a.add_argument('--report',type=Path,required=True);a.add_argument('--remote-tree',type=Path,required=True);args=a.parse_args()
    root=Path(__file__).resolve().parents[1];plan=args.plan.resolve();report=args.report.resolve();out=root/'construction/r998';out.mkdir(parents=True,exist_ok=True)
    verified=[];bad=[]
    for line in (plan/'PACKAGE_MANIFEST.sha256').read_text().splitlines():
        expected,name=line.split('  ',1);p=plan/name
        actual=sha(p) if p.is_file() else 'MISSING'
        (verified if actual==expected else bad).append({'path':name,'expected':expected,'actual':actual})
    save_json(out/'PLAN_MANIFEST_VERIFICATION.json',{'verified':len(verified),'failures':bad,'manifest_sha256':sha(plan/'PACKAGE_MANIFEST.sha256')})
    if bad:raise SystemExit('PLAN_MANIFEST_INVALID')
    inventory=[];parse_errors=[]
    # Parse whole lossless objects/streams, including member bodies and all AFTER bytes.
    for p in sorted(plan.rglob('*')):
        if not p.is_file():continue
        n=str(p.relative_to(plan));item={'path':n,'bytes':p.stat().st_size,'sha256':sha(p)}
        try:
            if p.suffix=='.gz':
                with gzip.open(p,'rb') as f:raw=f.read()
                item['decoded_bytes']=len(raw);item['decoded_sha256']=hashlib.sha256(raw).hexdigest()
                inner=p.with_suffix('').suffix
                if inner=='.jsonl':item['parsed_records']=sum(1 for line in raw.decode().splitlines() if line.strip() and json.loads(line) is not None)
                elif inner=='.json':item['parsed_type']=type(json.loads(raw)).__name__
                elif inner=='.csv':item['parsed_records']=len(list(csv.DictReader(raw.decode('utf-8-sig').splitlines())))
            elif p.suffix=='.json':item['parsed_type']=type(json.loads(p.read_text(encoding='utf-8-sig'))).__name__
            elif p.suffix=='.jsonl':
                with p.open(encoding='utf-8-sig') as f:item['parsed_records']=sum(1 for line in f if line.strip() and json.loads(line) is not None)
            elif p.suffix=='.csv':item['parsed_records']=len(rows(p))
            elif p.suffix in {'.md','.txt','.html','.java','.c','.h','.sql','.diff','.py','.gradle','.xml','.tsv'}:
                raw=p.read_bytes();raw.decode('utf-8-sig');item['read_lines']=raw.count(b'\n')+bool(raw)
        except Exception as e:parse_errors.append({'path':n,'error':type(e).__name__+':'+str(e)})
        inventory.append(item)
    save_json(out/'PLAN_READ_INVENTORY.json',{'files':inventory,'parse_errors':parse_errors,'scope':'Byte-complete read and machine parse; per-card semantic review separately required'})
    req=rows(plan/'38_REQUIREMENTS_TRACEABILITY/REQUIREMENTS_1832_SEMANTIC_REVIEW_CP12.csv');cards=rows(plan/'31_FILE_CONSTRUCTION_CARDS/FILE_CARD_MANIFEST.csv');tests=rows(plan/'34_TEST_MATRIX/TEST_MATRIX.csv');det=rows(plan/'01_DETECTION_ISSUE_MASTER_LEDGER/DETECTION_ISSUE_MASTER_LEDGER_CP12.csv');seq=rows(plan/'30_FILE_BY_FILE_SEQUENCE/FINAL_FILE_BY_FILE_SEQUENCE.csv')
    kinds=collections.Counter(r['semantic_disposition'] for r in req);dispositions=collections.Counter(r['disposition'] for r in cards)
    assert len(req)==1832 and len({r['requirement_id'] for r in req})==1832
    assert kinds=={'PLAN_ARTIFACT':530,'EXACT_SUPPORTED':860,'SUPERSEDED_EQUIVALENT':426,'HISTORICAL_REFERENCE':16}
    assert len(cards)==299 and dispositions=={'MODIFY':201,'ADD':79,'KEEP':11,'DELETE':8}
    assert len(tests)==328 and len(det)==166
    bindings={}
    with gzip.open(plan/'38_REQUIREMENTS_TRACEABILITY/normalized_bindings/REQUIREMENT_BINDINGS_1832.jsonl.gz','rt') as f:
        for line in f:
            v=json.loads(line);bindings[v['requirement_id']]=v
    assert set(bindings)=={r['requirement_id'] for r in req}
    common={'current_status':'NOT_VERIFIED','code_status':'NOT_ASSESSED','current_contract_review':'PENDING','caller':'','callee':'','owner':'','input_output':'','database_objects':'','thread_transaction':'','idempotency_recovery':'','current_before_sha256':'','current_after_sha256':'','diff_commit':'','current_test_id':'','command':'','exit_code':'','log':'','current_evidence_scope':'NO_CURRENT_EXECUTION_EVIDENCE','subconditions':'','blocker':'','acceptance_evidence':''}
    req_out=[]
    for r in req:
        actual=hashlib.sha256(r['exact_requirement'].encode()).hexdigest()
        assert actual==r['exact_requirement_sha256'],r['requirement_id']
        req_out.append({**r,**common,'original_source_file_sha256':sha(plan/'38_REQUIREMENTS_TRACEABILITY/REQUIREMENTS_1832_SEMANTIC_REVIEW_CP12.csv'),'normalized_binding':json.dumps(bindings[r['requirement_id']],ensure_ascii=False,separators=(',',':')),'current_test_id':r['canonical_test'],'subconditions':r['canonical_acceptance']})
    card_out=[]
    for r in cards:
        card=json.loads((plan/'31_FILE_CONSTRUCTION_CARDS/card_data'/(r['card_id']+'.json')).read_text());f=root/r['path'];cur=sha(f) if f.is_file() else 'MISSING';fix=card.get('fixed_contract_fields',{})
        card_out.append({**r,**common,'sequence_group':next((s['sequence_group'] for s in seq if s['construction_card']==r['card_id']),''),'current_before_sha256':cur,'static_comparison':'EXACT_AFTER_UNVERIFIED' if cur==r['after_sha256'] else ('EXACT_BEFORE_UNVERIFIED' if cur==r['before_sha256'] else 'CURRENT_DIFF_REQUIRES_MEMBER_REVIEW'),'card_body_sha256':sha(plan/'31_FILE_CONSTRUCTION_CARDS/card_data'/(r['card_id']+'.json')),'owner':json.dumps(fix.get('owner',[]),ensure_ascii=False),'thread_transaction':json.dumps({k:fix.get(k,[]) for k in ('thread','transaction')},ensure_ascii=False),'idempotency_recovery':json.dumps({k:fix.get(k,[]) for k in ('idempotency','recovery','failure')},ensure_ascii=False),'caller':json.dumps(card.get('typed_callers',[]),ensure_ascii=False),'callee':json.dumps(card.get('typed_callees',[]),ensure_ascii=False),'subconditions':json.dumps(fix.get('acceptance',[]),ensure_ascii=False),'domain_contracts':json.dumps(card.get('domain_contracts',[]),ensure_ascii=False),'current_test_id':json.dumps(fix.get('test',[]),ensure_ascii=False)})
    det_out=[{**r,**common,'source_cohort':'ORIGINAL_R980','original_source_file_sha256':sha(plan/'01_DETECTION_ISSUE_MASTER_LEDGER/DETECTION_ISSUE_MASTER_LEDGER_CP12.csv')}for r in det]
    checks=[]
    for line in (report/'REPORT_DETAILS.txt').read_text().splitlines():
        if line.startswith('{'):
            try:v=json.loads(line)
            except json.JSONDecodeError:continue
            if v.get('kind')=='check_result':checks.append(v)
    lookup={v['id']:v for v in checks}
    additions=[('RSEC-078','SHADOW prediction/settlement gap; determine task provenance and frozen observations'),('RSEC-003','Research funnel registration versus numerical update/freeze/qualification gap'),('RSEC-009','PLAN_ACCEPTANCE not proven by current report'),('RSEC-001','Current exact source snapshot evaluator warning; resolve evidence boundaries')]
    for i,(rid,title)in enumerate(additions,1):
        v=lookup.get(rid,{})
        det_out.append({**common,'issue_id':'NEW-DET-%03d'%i,'source_cohort':'PHONE_R996','reported_version':'5.4.2.39/R996','record_kind':'OBSERVATION_REQUIRES_CAUSAL_INVESTIGATION','report_position':'REPORT_DETAILS.txt:'+rid,'report_evidence':json.dumps(v,ensure_ascii=False),'root_cause':'NOT_YET_DETERMINED','acceptance':title,'original_source_file_sha256':sha(report/'REPORT_DETAILS.txt')})
    test_out=[{**r,**common,'current_status':'NOT_RUN','original_source_file_sha256':sha(plan/'34_TEST_MATRIX/TEST_MATRIX.csv'),'historical_execution_status':r['execution_status'],'execution_status':'NOT_RUN'}for r in tests]
    for name,data in [('REQUIREMENT_EXECUTION_LEDGER.csv',req_out),('CARD_EXECUTION_LEDGER.csv',card_out),('DETECTION_EXECUTION_LEDGER.csv',det_out),('TEST_EXECUTION_LEDGER.csv',test_out)]:csv_write(out/name,data)
    failures=[];members=0
    for line in (report/'ARCHIVE_MANIFEST.jsonl').read_text().splitlines():
        v=json.loads(line)
        if v.get('kind')!='member':continue
        members+=1;p=report/v['name'];actual=sha(p) if p.is_file() else 'MISSING'
        if actual!=v['raw_sha256'] or p.stat().st_size!=v['raw_bytes']:failures.append({'member':v['name'],'actual':actual})
    db=sqlite3.connect('file:'+str(report/'snapshot.db')+'?mode=ro',uri=True);integrity=db.execute('PRAGMA integrity_check').fetchone()[0];fk=db.execute('PRAGMA foreign_key_check').fetchall();db.close()
    save_json(out/'REPORT_INPUT_VERIFICATION.json',{'member_count':members,'hash_failures':failures,'check_count':len(checks),'check_outcomes':dict(collections.Counter(v['domain_outcome']for v in checks)),'sqlite_integrity':integrity,'fk_failure_count':len(fk),'apk_sha256':sha(report/'installed-app.apk'),'source_manifest':json.loads((report/'source-manifest.json').read_text()),'privacy':'No raw database values copied to evidence'})
    local=subprocess.check_output(['git','rev-parse','HEAD'],cwd=root,text=True).strip()
    save_json(out/'EXECUTION_STATE.json',{'schema_version':1,'remote_baseline_commit':'9ca5bc1ad0ce47cf85b6b435606c85629d4f88c2','local_baseline_commit':local,'local_baseline_origin':'R996 byte-verified source plus reviewed unpushed R997 fixes','branch':'construction/r998-canonical-20261005','requirements':1832,'semantic_counts':dict(kinds),'cards':299,'card_actions':dict(dispositions),'original_detections':166,'new_detections':len(additions),'tests':328,'sequence_rows':len(seq),'java_scc_groups':len({r['sequence_group']for r in seq if r['sequence_group'].startswith('SCC-')}),'current_group':'SCC-001','current_card':'FILE-CAN-003','next_action':'Review semantic generation card and three-way method/source identity; execute generator and Java compile on current source','blockers':['REMOTE_SYNC_PENDING','DEVICE_NOT_VERIFIED','NATURAL_FUTURE_NOT_VERIFIED'],'final_closed_requirements':0,'final_closed_cards':0,'test_matrix_executed':0,'status':'IN_PROGRESS'})
    print(json.dumps({'requirements':len(req_out),'cards':len(card_out),'detections':len(det_out),'tests':len(test_out),'plan_hash_failures':len(bad),'report_hash_failures':len(failures),'parse_errors':len(parse_errors),'sqlite_integrity':integrity},ensure_ascii=False))

if __name__=='__main__':main()
