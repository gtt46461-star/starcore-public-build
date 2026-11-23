#!/usr/bin/env python3
"""Read-only, reproducible evidence extraction from the three supplied reports."""
import collections, datetime, hashlib, json, pathlib, re, zipfile
BASE = pathlib.Path(__file__).resolve().parents[1]
OUT = BASE / 'analyses'
TZ = datetime.timezone(datetime.timedelta(hours=8))
def stamp(ms):
    return datetime.datetime.fromtimestamp(int(ms)/1000, TZ).isoformat()
def checks(d):
    result = {}
    for line_number, line in enumerate((d/'REPORT_DETAILS.txt').read_text().splitlines(),1):
        if line.startswith('{"kind":"check_result"'):
            item=json.loads(line);item['source_line']=line_number; result[item['id']]=item
    return result
def sha(p):
    with p.open('rb') as f:return hashlib.file_digest(f,'sha256').hexdigest()
reports=[]
for key in ['870cd99','89d1cac','fabedd0']:
    d=BASE/'reports'/key
    def read(n):return json.loads((d/n).read_text())
    scope=read('DIAGNOSTIC_SCOPE.json');r30=read('R30_REPORT.json');runtime=read('runtime-observation.json');manifest=read('source-manifest.json');upgrade=read('UPGRADE_DIAGNOSIS.json');check=checks(d)
    outer=next((BASE/'upload').glob('STARCORE-'+key+'*.zip'))
    tablecounts={x['table']:x['rows'] for x in scope['tables']}
    recent={x['table']:x for x in scope['recentEvidence']}
    summaries={}
    for group in ['legacyCharts','researchCharts','matrices','capabilities','logicalTables']:
        items=r30['objectIndex'][group]['objects']
        summaries[group]={'count':len(items),'field_counts':{field:dict(collections.Counter(str(x.get(field,'__missing__')) for x in items)) for field in ['status','produced','consumed','effective','inputAvailable','callable']},'objects':[{a:x.get(a) for a in ['id','title','label','kind','status','sourceTable','sourceFile','sourceMethod','implemented','callable','inputAvailable','ran','produced','consumed','effective','currentSourceProducedFrames','actualSourceConsumptionReceipts','notProducedReason','executionBlockedReason'] if a in x} for x in items]}
    details=check['RSEC-078']['evidence']; health=check['RSEC-052']['evidence']
    first_health=health.splitlines()[0]
    parsed_health={}
    for part in first_health.split('|'):
        if '=' in part:
            k,v=part.split('=',1);parsed_health[k]=v
    selected_tables=['r952_candidate','r952_runtime_event','r952_candidate_funnel','v57_research_task_r967','research_lifecycle_r932']
    summary={
        'key':key,'original_archive':{'file':outer.name,'bytes':outer.stat().st_size,'sha256':sha(outer)},
        'evidence_root':'reports/'+key,
        'build':(d/'REPORT_DETAILS.txt').read_text().splitlines()[1],
        'source_commit':manifest['sourceCommit'],'apk_sha256':manifest['apkSha256'],
        'runtime_sample_local':stamp(runtime['sampleWallMs']),'facts_read_local':stamp(r30['snapshotWatermark']['factsReadAt']),
        'runtime_sample_ms':runtime['sampleWallMs'],'facts_read_ms':r30['snapshotWatermark']['factsReadAt'],
        'latest_issue':r30['snapshotWatermark']['latestIssue'],
        'common_metrics':r30['chapters'][0]['commonMetrics'],
        'overall_acceptance':r30['overallAcceptance'],'natural_quality':r30['naturalQuality'],
        'physical_device':r30['physicalDevice'],'total_tables':len(tablecounts),'total_rows':sum(tablecounts.values()),
        'table_counts':tablecounts,
        'table_count_source':'DIAGNOSTIC_SCOPE.json#/tables (select table by name; rows)',
        'selected_original_rows':{t:recent[t] for t in selected_tables if t in recent},
        'runtime':runtime,'scheduler':scope['researchSchedulerState'],
        'checks_health_counts':dict(collections.Counter(x['health'] for x in check.values())),
        'checks':[{k:v for k,v in x.items() if k!='evidence'} for x in check.values()],
        'business_health':parsed_health,
        'business_health_full':health,
        'common_metrics_source':'R30_REPORT.json#/chapters/0/commonMetrics',
        'diagnosis_counts':{k:r30['diagnosis'].get(k) for k in ['version','expectedRules','actionableRules','detectedOrReview','insufficient','unknown','actionCoverage']},
        'diagnosis_findings':[{k:x.get(k) for k in ['id','status','conclusion','condition','evidenceStrength','action','collectionAction']} for x in r30['diagnosis']['findings']],
        'object_groups':summaries,
        'operating_summary':upgrade['capabilityRepairR32']['operatingSummary'],
        'capability_repair_rows':[{k:x.get(k) for k in ['id','group','sourceFile','sourceMethod','entryClass','entryMethod','implementationClass','firstKnownGap','nextAction','verificationEntry','implemented','callable','produced','consumed','effective']} for x in upgrade['capabilityRepairR32']['rows']],
        'capability_needs':upgrade['capabilityRepairR32']['capabilityNeedDiagnosticsR335'],
        'su2_source_evidence':upgrade['sourceEvidenceR335'],
        'source_manifest':manifest,
        'evidence_assessment':r30['evidenceAssessmentR33'],
        'bounded_period_intersection':{'range':'202611230080 < issue <= 202611230288','stored_draws':sum('202611230080'<x['issue']<='202611230288' for x in recent['draws']['rows']),'stored_shadow_freezes':sum('202611230080'<x['target_issue']<='202611230288' for x in recent['v57_shadow_observation_r35']['rows']),'scope':'RETAINED_METADATA_ROWS; NOT_CALENDAR_COVERAGE_OR_CAUSAL_EFFECT'},
        'settlement_states':dict(collections.Counter(x.get('status') for x in recent['v57_settlement_task_r36']['rows'])),
        'members':[{'name':p.name,'bytes':p.stat().st_size,'sha256':sha(p)} for p in sorted(d.iterdir()) if p.is_file()]
    }
    reports.append(summary)
    extract_dir=OUT/'check_evidence'/key;extract_dir.mkdir(parents=True,exist_ok=True)
    for id in ['RSEC-001','RSEC-003','RSEC-004','RSEC-052','RSEC-066','RSEC-074','RSEC-078','RSEC-079','RSEC-305']:
        (extract_dir/(id+'.txt')).write_text('Source: reports/'+key+'/REPORT_DETAILS.txt line '+str(check[id]['source_line'])+'\n'+check[id]['evidence']+'\n')
result={'schema':'R34_REPORT_FORENSIC_EXTRACTION_V1','created_on':'2026-11-23','boundary':'Read-only supplied ZIP evidence. No raw database or APK was included. Source-code causality is a separate audit. Counter equality alone does not prove zero work if retention may operate. Runtime sample and report facts have separate clocks.','reports':reports}
(OUT/'report_metrics.json').write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n')
print(json.dumps({'out':str(OUT/'report_metrics.json'),'bytes':(OUT/'report_metrics.json').stat().st_size,'reports':len(reports)},ensure_ascii=False))
