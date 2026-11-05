#!/usr/bin/env python3
"""Reproduce R31 report-derived evidence ledgers. No network and no model/quality claims.
Usage: python3 summarize_evidence.py --input analysis/report/expanded --output audits/evidence
"""
from pathlib import Path
import argparse, csv, collections, hashlib, json, re, datetime, math

def jdump(path, obj):
    path.write_text(json.dumps(obj, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')

def csvdump(path, rows, fields=None):
    if fields is None:
        fields=list(dict.fromkeys(k for row in rows for k in row))
    with path.open('w', newline='', encoding='utf-8-sig') as f:
        w=csv.DictWriter(f, fieldnames=fields, extrasaction='ignore');w.writeheader()
        for row in rows:
            w.writerow({k: json.dumps(v,ensure_ascii=False,sort_keys=True) if isinstance(v,(dict,list)) else v for k,v in row.items()})

def digest(p): return hashlib.sha256(p.read_bytes()).hexdigest()
def source(file, ptr): return file+'#'+ptr

def main():
    ap=argparse.ArgumentParser();ap.add_argument('--input',type=Path,required=True);ap.add_argument('--output',type=Path,required=True)
    args=ap.parse_args();root=args.input.resolve();out=args.output.resolve();out.mkdir(parents=True,exist_ok=True)
    if not root.is_dir():raise SystemExit('missing report directory')
    docs={}
    inv=[]
    for p in sorted(root.iterdir()):
        if not p.is_file():continue
        row={'file':p.name,'bytes':p.stat().st_size,'sha256':digest(p)}
        if p.suffix=='.json':
            d=json.loads(p.read_text());docs[p.name]=d
            row['json_type']=type(d).__name__
            if isinstance(d,dict):row['structure']={k:{'type':type(v).__name__,'size':len(v) if isinstance(v,(dict,list,str)) else None} for k,v in d.items()}
        inv.append(row)
    jdump(out/'input_inventory.json',inv)
    legacy=[];legacy_cards=[]
    for p in sorted(root.glob('construction-current-*.txt')):
        with p.open(encoding='utf-8-sig',newline='') as f:
            reader=csv.DictReader(f)
            fields=reader.fieldnames or []
            if 'requirement_id' in fields:
                for i,row in enumerate(reader,1):
                    legacy.append({**row,'source':p.name+'#csv-record='+str(i)})
            elif 'card_id' in fields:
                for i,row in enumerate(reader,1):
                    legacy_cards.append({k:v for k,v in {**row,'source':p.name+'#csv-record='+str(i)}.items() if k not in ['original_card','domain_contract_json']})
    if legacy:csvdump(out/'legacy_requirements_1832.csv',legacy)
    if legacy_cards:csvdump(out/'legacy_file_cards_299.csv',legacy_cards)
    d=docs['R30_REPORT.json'];s=docs['DIAGNOSTIC_SCOPE.json'];sm=docs['source-manifest.json'];runtime=docs['runtime-observation.json']
    tables=[];columns=[];states=[]
    for i,t in enumerate(s['tables']):
        tables.append({'table':t['table'],'rows':t.get('rows'),'column_count':len(t.get('columns',[])),'states':t.get('states',[]),'source':source('DIAGNOSTIC_SCOPE.json',f'/tables/{i}')})
        for j,c in enumerate(t.get('columns',[])):
            columns.append({'table':t['table'],**c,'source':source('DIAGNOSTIC_SCOPE.json',f'/tables/{i}/columns/{j}')})
        for j,v in enumerate(t.get('states',[])):
            states.append({'table':t['table'],**v,'source':source('DIAGNOSTIC_SCOPE.json',f'/tables/{i}/states/{j}')})
    csvdump(out/'tables_713.csv',tables);csvdump(out/'table_columns.csv',columns);csvdump(out/'table_states.csv',states)
    jdump(out/'tables_schema.json',{'source':'DIAGNOSTIC_SCOPE.json#/tables','tables':s['tables']})
    schema=[]
    for p in sorted(root.glob('catalog-*.jsonl')):
        for lineno,line in enumerate(p.read_text().splitlines(),1):
            o=json.loads(line)
            if o.get('kind') in ['schema_object','schema_sql_chunk']:
                schema.append({**o,'source':p.name+':'+str(lineno)})
    jdump(out/'schema_objects_and_sql.json',schema)
    ch=d['chapters'][3];live=ch['data']['taskLiveness'];liveby={x['taskId']:x for x in live['tasks']}
    tasks=[]
    for i,t in enumerate(ch['data']['tasks']):
        row={**t,'source':source('R30_REPORT.json',f'/chapters/3/data/tasks/{i}'),'typedLiveness':liveby.get(t['task_id'])}
        tasks.append(row)
    jdump(out/'tasks_60.json',tasks)
    flat=[]
    for t in tasks:
        row={k:v for k,v in t.items() if k!='typedLiveness'}
        row.update({'typed_'+k:v for k,v in (t['typedLiveness'] or {}).items() if k!='taskId'})
        flat.append(row)
    csvdump(out/'tasks_60.csv',flat)
    metrics=[]
    for i,m in enumerate(d['metricResults']):
        row={k:v for k,v in m.items() if k not in ['sourceSnapshot','observations']}
        row['observations_source']=source('R30_REPORT.json',f'/metricResults/{i}/observations')
        row['source']=source('R30_REPORT.json',f'/metricResults/{i}')
        metrics.append(row)
    jdump(out/'metrics_44.json',metrics);csvdump(out/'metrics_44.csv',metrics)
    chapters=[]
    for i,c in enumerate(d['chapters']):
        chapters.append({'sectionId':c['sectionId'],'title':c['title'],'status':c['status'],'reason':c.get('reason'),'evidenceLevel':c.get('evidenceLevel'),'dataKeys':list(c.get('data',{})),'sourceErrors':c.get('sourceErrors'),'collectionAction':c.get('collectionAction'),'source':source('R30_REPORT.json',f'/chapters/{i}')})
    jdump(out/'chapters_12.json',chapters);csvdump(out/'chapters_12.csv',chapters)
    checks=[]
    for i,line in enumerate((root/'REPORT_DETAILS.txt').read_text().splitlines(),1):
        if not line.startswith('{'):continue
        try:x=json.loads(line)
        except json.JSONDecodeError:continue
        if x.get('kind')=='check_result':checks.append({**x,'source':'REPORT_DETAILS.txt:'+str(i)})
    jdump(out/'checks_96.json',checks);csvdump(out/'checks_96.csv',checks)
    selected=[]
    for i,x in enumerate(s['recentEvidence']):
        selected.append({'table':x['table'],'scope':x.get('scope'),'rowOrder':x.get('rowOrder'),'row_count':len(x.get('rows',[])),'columns':x.get('columns',[]),'source':source('DIAGNOSTIC_SCOPE.json',f'/recentEvidence/{i}')})
    csvdump(out/'selected_rowsets_83.csv',selected)
    draws=[]
    for i,x in enumerate(s['recentEvidence']):
        if x['table']!='draws':continue
        for ri,r in enumerate(x['rows']):
            draws.append({**r,'source_row_index':ri,'source':source('DIAGNOSTIC_SCOPE.json',f'/recentEvidence/{i}/rows/{ri}')})
    draws.sort(key=lambda x:(int(x['open_time']),x['issue']))
    errors=[];issues=collections.Counter(x['issue'] for x in draws);times=collections.Counter(x['open_time'] for x in draws)
    for i,row in enumerate(draws):
        row['chronological_index_0']=i
        nums=[int(row['n'+str(j)]) for j in range(1,11)]
        if sorted(nums)!=list(range(1,11)):errors.append({'kind':'INVALID_PERMUTATION','issue':row['issue']})
        raw=json.loads(row['raw_json'])
        rnums=[int(n) for n in raw['openNumber'].split(',')]
        if rnums!=nums or raw.get('betNo')!=row['issue']:errors.append({'kind':'RAW_JSON_MISMATCH','issue':row['issue']})
        if row.get('dirty')!='0':errors.append({'kind':'DIRTY_ROW','issue':row['issue'],'value':row.get('dirty')})
    deltas=collections.Counter(int(draws[i]['open_time'])-int(draws[i-1]['open_time']) for i in range(1,len(draws)))
    gaps=[{'before_issue':draws[i-1]['issue'],'after_issue':draws[i]['issue'],'delta_ms':int(draws[i]['open_time'])-int(draws[i-1]['open_time'])} for i in range(1,len(draws)) if int(draws[i]['open_time'])-int(draws[i-1]['open_time'])!=60000]
    jdump(out/'draws_5000.json',draws);csvdump(out/'draws_5000.csv',draws)
    drawcheck={'rows':len(draws),'first_issue':draws[0]['issue'],'last_issue':draws[-1]['issue'],'first_open_time_ms':int(draws[0]['open_time']),'last_open_time_ms':int(draws[-1]['open_time']),'duplicate_issues':{k:v for k,v in issues.items() if v>1},'duplicate_open_times':{k:v for k,v in times.items() if v>1},'consecutive_open_time_deltas_ms':dict(deltas),'gaps':gaps,'errors':errors,'all_permutations_of_1_to_10':not any(x['kind']=='INVALID_PERMUTATION' for x in errors),'fetch_time_greater_than_open_time':sum(int(x['fetch_time'])>int(x['open_time']) for x in draws),'first_seen_time_greater_than_open_time':sum(int(x['first_seen_at'])>int(x['open_time']) for x in draws),'warning':'These are ordered historical metadata rows, not proof that the strategy or device knew each row before its historical deadline. Replays must use prior rows only. first_seen_at/fetch_time retain original values; importing backfill does not create historical predraw evidence.'}
    jdump(out/'draws_validation.json',drawcheck)
    rowsets={x['table']:(i,x) for i,x in enumerate(s['recentEvidence'])}
    shadow_i,shadow_set=rowsets['v57_shadow_observation_r35'];settle_i,settle_set=rowsets['v57_settlement_task_r36']
    shadow=[{**row,'source':source('DIAGNOSTIC_SCOPE.json',f'/recentEvidence/{shadow_i}/rows/{i}')} for i,row in enumerate(shadow_set['rows'])]
    settlement=[{**row,'source':source('DIAGNOSTIC_SCOPE.json',f'/recentEvidence/{settle_i}/rows/{i}')} for i,row in enumerate(settle_set['rows'])]
    csvdump(out/'shadow_observations_73.csv',shadow);csvdump(out/'settlement_tasks_5519.csv',settlement)
    drawby={x['issue']:x for x in draws}; settled=[x for x in shadow if x['settled']=='1']; checks_shadow=[]
    for x in settled:
        dr=drawby.get(x['target_issue']);pos=int(x['position']);actual=int(dr['n'+str(pos)]) if dr and 1<=pos<=10 else None
        expected_hit=int(actual in [int(x['n1']),int(x['n2']),int(x['n3'])]) if actual is not None else None
        checks_shadow.append({'target_issue':x['target_issue'],'lane':x['lane'],'position':pos,'reported_hit':int(x['hit_label']),'recomputed_hit':expected_hit,'actual_number_matches':actual==int(x['actual_number']),'hit_matches':expected_hit==int(x['hit_label']),'pre_draw_frozen':x['pre_draw_frozen']=='1','commit_before_deadline':int(x['commit_at'])<int(x['deadline_at']),'history_max_before_target':x['history_max_issue']<x['target_issue'],'source':x['source']})
    n=len(settled);hits=sum(int(x['hit_label']) for x in settled);z=1.959963984540054;ph=hits/n;den=1+z*z/n;centre=(ph+z*z/(2*n))/den;half=z*math.sqrt(ph*(1-ph)/n+z*z/(4*n*n))/den
    shadowcheck={'rows':len(shadow),'settled':n,'unsettled':len(shadow)-n,'settled_hits':hits,'settled_hit_rate':ph,'wilson_two_sided_95_lower':centre-half,'wilson_two_sided_95_upper':centre+half,'settled_lanes':dict(collections.Counter(x['lane'] for x in settled)),'all_hit_values_match_current_retained_draw_rows':all(x['hit_matches'] and x['actual_number_matches'] for x in checks_shadow),'all_settled_commit_timestamps_precede_deadline':all(x['commit_before_deadline'] for x in checks_shadow),'all_settled_history_max_precedes_target':all(x['history_max_before_target'] for x in checks_shadow),'checks':checks_shadow,'boundary':'Only stored SHADOW observations were recomputed against stored draw rows. This is not a REAL result, an independent source verification, or a replay of missing full model/history artifacts. Historical record hashes and PIT code paths are not proved by timestamp comparisons alone.'}
    jdump(out/'shadow_validation.json',shadowcheck)
    jdump(out/'settlement_summary.json',{'rows':len(settlement),'states':dict(collections.Counter(x['status'] for x in settlement)),'status_and_task_class':dict(collections.Counter(x['status']+'/'+x['task_class'] for x in settlement)),'latest_completed_target':max(x['target_issue'] for x in settlement if x['status']=='COMPLETED'),'source':'DIAGNOSTIC_SCOPE.json#/recentEvidence/'+str(settle_i),'heartbeat_reported_last_settlement':'202611020847','heartbeat_source':'runtime-observation-heartbeatSummary.txt:1','interpretation':'Heartbeat last-settlement is stale/different scope from the immutable task rows. Do not treat it as proof that settlement has stopped at that old issue.'})
    manifest=(root/'ARCHIVE_MANIFEST.jsonl').read_bytes();lines=manifest.splitlines(keepends=True);objects=[json.loads(l) for l in lines]
    verifications=[]
    for i,m in enumerate(objects,1):
        if m['kind'] not in ['member','part']:continue
        p=root/m['name'];present=p.is_file();raw=m['kind']=='member';expectbytes=m['raw_bytes'] if raw else m['bytes'];expectsha=m['raw_sha256'] if raw else m['sha256']
        existsbytes=p.stat().st_size if present else None;existssha=digest(p) if present else None
        verifications.append({'kind':m['kind'],'name':m['name'],'expected_bytes':expectbytes,'observed_bytes':existsbytes,'expected_sha256':expectsha,'observed_sha256':existssha,'status':'PASS' if present and expectbytes==existsbytes and expectsha==existssha else ('NOT_PRESENT_PACKAGING_DIFFERENCE' if not present and not raw else 'FAIL'),'source':'ARCHIVE_MANIFEST.jsonl:'+str(i)})
    footer=objects[-1];bodysha=hashlib.sha256(b''.join(lines[:-1])).hexdigest()
    integrity={'member_checks':verifications,'footer':footer,'manifest_body_observed_sha256':bodysha,'manifest_body_sha256_matches':bodysha==footer['manifest_body_sha256'],'raw_members_pass':sum(x['kind']=='member' and x['status']=='PASS' for x in verifications),'raw_members_fail':sum(x['kind']=='member' and x['status']!='PASS' for x in verifications),'encoded_parts_absent':sum(x['kind']=='part' and x['observed_bytes'] is None for x in verifications),'boundary':'Decoded raw members are byte-verifiable; original P*.bin zstd chunks are absent from this ZIP, so encoded chunk byte hashes are not reverified. Raw database snapshot and model binaries are not included, and their recorded hashes cannot be recomputed here.'}
    jdump(out/'archive_integrity.json',integrity);csvdump(out/'archive_members_and_parts.csv',verifications)
    storage_lines=(root/'STORAGE_CENSUS.txt').read_text().splitlines();storage={}
    for token in storage_lines[0].split('|')[1:]:
        if '=' not in token:continue
        k,v=token.split('=',1)
        storage[k]=int(v) if re.fullmatch(r'-?\d+',v) else v
    storage['snapshot_pragma']=json.loads(storage_lines[1]);storage['source']='STORAGE_CENSUS.txt:1-2'
    jdump(out/'storage_facts.json',storage)
    runtime_out={'source':'runtime-observation.json','observation':runtime,'heartbeat_source':'runtime-observation-heartbeatSummary.txt:1','heartbeat_raw':(root/'runtime-observation-heartbeatSummary.txt').read_text(),'crash_source':'runtime-observation-crashSummary.txt','crash_raw':(root/'runtime-observation-crashSummary.txt').read_text()}
    jdump(out/'runtime_facts.json',runtime_out)
    r27=[x for x in tasks if x['lane']=='R27'];keep=[x for x in tables if x['table'].startswith('r30_keep_')]
    summary={'generated_by':'summarize_evidence.py','source_commit':sm['sourceCommit'],'source_apk_sha256':sm['apkSha256'],'report_job_id':d['snapshotWatermark']['reportJobId'],'report_snapshot_sha256':sm['snapshotSha256'],'raw_database_included':s['rawDatabaseIncluded'],'table_count':len(tables),'empty_tables':sum(x['rows']==0 for x in tables),'nonempty_tables':sum(x['rows']>0 for x in tables),'keep_table_count':len(keep),'keep_total_rows':sum(x['rows'] for x in keep),'task_count':len(tasks),'task_statuses':dict(collections.Counter(x['status'] for x in tasks)),'r27_task_count':len(r27),'r27_quarantined':sum(x['status']=='QUARANTINED' for x in r27),'r27_claim_failure_count':sum('R30_PREPARED_TASK_CLAIM' in (x['blocked_reason'] or '') for x in r27),'typed_liveness_summary':live['summary'],'metric_count':len(metrics),'metric_statuses':dict(collections.Counter(x['status'] for x in metrics)),'check_count':len(checks),'check_execution_states':dict(collections.Counter(x['execution_state'] for x in checks)),'check_domain_outcomes':dict(collections.Counter(x['domain_outcome'] for x in checks)),'chapter_count':len(chapters),'chapter_statuses':dict(collections.Counter(x['status'] for x in chapters)),'overall_acceptance':d['overallAcceptance'],'natural_quality':d['naturalQuality'],'physical_device_test':d['physicalDevice'],'consistency_conflicts_reported':d['reportConsistencyConflicts'],'storage_total_bytes':storage['total'],'storage_total_decimal_GB':storage['total']/1e9,'storage_total_GiB':storage['total']/(2**30),'raw_manifest_members_pass':integrity['raw_members_pass'],'raw_manifest_members_fail':integrity['raw_members_fail'],'raw_manifest_body_matches':integrity['manifest_body_sha256_matches'],'draw_validation':drawcheck}
    jdump(out/'summary.json',summary)
    assert len(tables)==713 and len(tasks)==60 and len(metrics)==44 and len(checks)==96 and len(chapters)==12
    assert len(r27)==13 and all(t['status']=='QUARANTINED' for t in r27)
    assert len(keep)==55 and all(t['rows']==0 for t in keep)
    assert integrity['raw_members_pass']==48 and integrity['raw_members_fail']==0 and integrity['manifest_body_sha256_matches']
    assert len(draws)==5000 and not errors and not gaps and not drawcheck['duplicate_issues'] and not drawcheck['duplicate_open_times']
    print(json.dumps({k:summary[k] for k in ['table_count','empty_tables','task_count','r27_task_count','r27_quarantined','metric_statuses','check_execution_states','raw_manifest_members_pass','storage_total_bytes']},ensure_ascii=False,indent=2))

if __name__=='__main__': main()
