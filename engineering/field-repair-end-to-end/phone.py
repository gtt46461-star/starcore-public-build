"""Measure an Android debug build report against a field-sized synthetic event and fetch ledgers."""
from contextlib import closing
import re
import json
import os
from pathlib import Path
import sqlite3
import subprocess
import sys
import time
import threading
import zipfile

source = Path('source').resolve()
sys.path.insert(0, str(source/'tools/r342'))
import ci_candidate_tests as ci
import report_ui_driver as ui

out = Path(os.environ['RUNNER_TEMP'])/'large-report-evidence'
out.mkdir()
serial = ui.DEVICE
pkg = ci.PACKAGE
tmp = Path(os.environ['RUNNER_TEMP'])
candidate = tmp/'candidate-tested.apk'
assert serial.startswith('emulator-') and ci.adb(serial,'shell','getprop','ro.kernel.qemu') == '1'
new_source = os.environ['SOURCE']
assert 'Success' in ci.adb(serial,'install','-r',candidate)
after=ci.installed(serial,out/'after.txt')
assert after['versionCode']==int(os.environ['VERSION']) and after['apkSha256']==ci.sha(candidate)
before=after
warmup=ci.signed_report_campaign(serial,out,ci.sha(candidate))
try:
    warmup('seed','COMPACT_DIAGNOSTIC',new_source)
except (RuntimeError, subprocess.SubprocessError) as error:
    progress=ui.OUT/'seed-progress.json'
    if progress.exists():
        history=json.loads(progress.read_text())
        print("EMPTY_DEVICE_WARMUP_STATE",str(history[-2:])[:1500],flush=True)
    root='/data/user/0/'+pkg+'/files/r981_reports'
    try:
        job=json.loads(ci.adb(serial,'shell','cat',root+'/current-job.json'))
        ack=json.loads(ci.adb(serial,'shell','cat',root+'/requests/'+job['requestId']+'.ack.json'))
        print("EMPTY_DEVICE_WARMUP_RECEIPT",json.dumps({k:ack.get(k) for k in ('stage','reason','failurePhaseR32','failureDetailR32','reportSourcePreparation')}),flush=True)
    except (RuntimeError,ValueError,KeyError,subprocess.SubprocessError) as missing:
        print("EMPTY_DEVICE_WARMUP_NO_RECEIPT",type(missing).__name__,flush=True)
    raise
db=json.loads((source/'release/release_manifest.json').read_text())['databaseName']
assert '/' not in db
live=f'/data/user/0/{pkg}/databases/{db}'
local=tmp/db
ci.adb(serial,'shell','am','force-stop',pkg)
identity = ci.adb(serial,'shell','stat','-c','%u:%g',live)
assert identity.replace(':','').isdigit()
for suffix in ['', '-wal', '-shm']:
    if subprocess.run(['adb','-s',serial,'shell','test','-f',live+suffix]).returncode==0:
        ci.adb(serial,'pull',live+suffix,str(local)+suffix)
pairs=[(f'test-{a:03}',f'test-{b:03}',(a+b)/480.0,100,'202611270384') for a in range(240) for b in range(a+1,240)]
with closing(sqlite3.connect(local)) as c:
    import io, hashlib
    original=source/'construction/plans/STARCORE_R34_3_REPAIR_20261126/report/ORIGINAL_DIAGNOSTIC_173.zip'
    assert ci.sha(original)=='3fb241b3181dccd4d149cf91471d86532919f77d943847ff8ce348cce5ec4d86'
    with zipfile.ZipFile(original) as outer, zipfile.ZipFile(io.BytesIO(outer.read(outer.namelist()[0]))) as archive:
        archive_scope=json.loads(archive.read('DIAGNOSTIC_SCOPE.json'))
        selected={section['table'] for section in archive_scope['recentEvidence'] if section['table'].startswith(('r80_probe_','research_long_'))}
        # MultiHorizonProbe's read-only retained-original accounting joins the archive registry.
        selected.update({'research_evidence_archive_v2','research_evidence_archive_member_v2','research_evidence_locator_v2','v57_atom_edge_r925','v57_runtime_event_r926','v57_fetch_ledger_r925'})
        ddl={}
        for member in sorted(n for n in archive.namelist() if re.fullmatch(r'catalog-\d+\.jsonl',n)):
            for line in archive.open(member):
                row=json.loads(line)
                if row['kind']=='schema_object' and row['type']=='table' and row['table'] in selected and not row['sql_null']:
                    ddl[row['type'],row['name']]=[]
                elif row['kind']=='schema_sql_chunk' and (row['type'],row['name']) in ddl:
                    ddl[row['type'],row['name']].append((row['start_char'],row['text']))
        for (kind,name),chunks in ddl.items():
            if not c.execute('SELECT 1 FROM sqlite_master WHERE type=? AND name=?',(kind,name)).fetchone():
                sql=''.join(text for pos,text in sorted(chunks))
                assert sql.upper().startswith('CREATE ') and kind.upper() in sql.upper()[:30]
                c.execute(sql)
    for table in selected:
        assert c.execute('SELECT 1 FROM sqlite_master WHERE type=\'table\' AND name=?',(table,)).fetchone(),table
    c.executemany("INSERT INTO v57_atom_edge_r925(left_atom,right_atom,correlation,support_issues,last_issue) VALUES(?,?,?,?,?)",pairs)
    # Two actual report ledgers at the cardinalities observed in the sealed 173 original.
    # The fixture values are synthetic and use a separate game key.
    event='v57_runtime_event_r926'
    fetch='v57_fetch_ledger_r925'
    c.execute("""WITH RECURSIVE n(i) AS (VALUES(1) UNION ALL SELECT i+1 FROM n WHERE i<718660)
      INSERT INTO v57_runtime_event_r926(event_id,dedup_key,game_code,target_issue,epoch_id,attempt_id,event_type,stage,state_rank,payload_text,payload_hash,source,created_at)
      SELECT printf('F207E%d',i),printf('F207D%d',i),'report-fixture','000','fixture','fixture','fixture',
             printf('F207_STAGE_%02d',i%23),0,printf('%0300d',i)||lower(hex(randomblob(320))),'fixture','offline-regression',i FROM n""")
    c.execute("""WITH RECURSIVE n(i) AS (VALUES(1) UNION ALL SELECT i+1 FROM n WHERE i<164173)
      INSERT INTO v57_fetch_ledger_r925(request_id,method,endpoint,request_at,status,source_line)
      SELECT printf('F207F%d',i),'FIXTURE','offline',i,printf('F207_STATUS_%02d',i%8),printf('%0188d',i)||lower(hex(randomblob(256))) FROM n""")
    archived={}
    projected_blobs=0
    projected_blob_bytes=0
    for section in archive_scope['recentEvidence']:
        name=section['table']
        if not name.startswith(('research_long_','r80_probe_')) or not section.get('rows'):continue
        assert re.fullmatch('[a-z0-9_]+',name) and section['complete']
        columns=section['columns']
        assert all(re.fullmatch('[a-zA-Z0-9_]+',key) for key in columns)
        definition={r[1] for r in c.execute('pragma table_info('+name+')')}
        assert set(columns)<=definition,(name,columns,definition)
        rows=[tuple(row[key] for key in columns) for row in section['rows']]
        # The historical compact ZIP preserves binary lengths only. In this disposable DB,
        # create equal-length synthetic BLOBs so the exported descriptors can be compared exactly.
        c.execute('DELETE FROM '+name)
        for row in rows:
            params=[]
            placeholders=[]
            for value in row:
                match=re.fullmatch(r'\[BINARY_PAYLOAD_NOT_INCLUDED bytes=(\d+)\]',value) if isinstance(value,str) else None
                if match:
                    length=int(match[1])
                    assert 0<=length<=128*1024*1024
                    projected_blobs+=1
                    projected_blob_bytes+=length
                    placeholders.append('zeroblob(?)')
                    params.append(length)
                else:
                    placeholders.append('?')
                    params.append(value)
            c.execute('INSERT INTO '+name+'('+','.join(columns)+') VALUES('+','.join(placeholders)+')',params)
        archived[name]=section
    assert len(archived['research_long_source_token_r80']['rows'])==1 and len(archived['research_long_snapshot_r80']['rows'])==1730
    contribution = 'v57_research_mainline_contribution_r922'
    version_name = json.loads((source/'release/release_manifest.json').read_text())['versionName']
    c.executemany(f"""INSERT INTO {contribution}(
        target_issue,runtime_version,lane,candidate_id,candidate_name,status,applied,influence_weight,
        future_n,hit_rate,wilson_lower,brier,ece,coverage,paired_n,paired_delta,fdr_q,evidence_hash,rank1_hash,
        controlled_influence_eligible,direct_real_authority,record_hash,created_at)
        VALUES(?,?,'SHADOW',?,?,'FIXTURE_OBSERVED',0,0,0,0,0,0,0,0,0,0,1,?,?,0,0,?,?)""",
        [(f'20261001{i:04}',version_name,'fix210-contribution',
          f'FIX210_TEXT_{i:02d}_' + '观测备查abcdefghijklmnop' * 280,
          hashlib.sha256(f'fix210-evidence-{i}'.encode()).hexdigest(),
          hashlib.sha256(f'fix210-rank-{i}'.encode()).hexdigest(),
          hashlib.sha256(f'fix210-row-{i}'.encode()).hexdigest(),2000000000000+i)
         for i in range(1,61)])
    # Mirror the field chart-cache cardinality and stress the R26 journal preview.
    c.execute("""WITH RECURSIVE n(i) AS (VALUES(1) UNION ALL SELECT i+1 FROM n WHERE i<281099)
        INSERT INTO r27_chart_cache(panel_id,key,payload_json,updated_at)
        SELECT 'fixture-report-only',printf('R211-%d',i),'{"scope":"SYNTHETIC_OFFLINE_ONLY"}',i FROM n""")
    c.execute("""WITH RECURSIVE n(i) AS (VALUES(1) UNION ALL SELECT i+1 FROM n WHERE i<180000)
        INSERT INTO r27_task_error(task_id,error_class,message,stack,recorded_at)
        SELECT 'fixture-report-only','R211_FIXTURE',printf('R211_ERROR_%06d',i)||printf('%0400d',i),'',i FROM n""")
    # Large overview state is stored as text, just as it is on a retained install.
    keys=('r923_freeze_snapshot_status','r923_hot_path_stage_trace','r923_realtime_provenance_repair_summary',
          'r923_miss_streak_analysis','r923_miss_streak_research_step','r923_essential_feedback_status')
    for index,key in enumerate(keys):
        value=f'R211_OVERVIEW_{index}_'+ '记录abcdefghijklmnop' * 250
        c.execute("""INSERT INTO app_state(state_key,state_value,updated_at) VALUES(?,?,1)
                     ON CONFLICT(state_key) DO UPDATE SET state_value=excluded.state_value""",(key,value))
    c.commit()
    assert c.execute("SELECT COUNT(*) FROM v57_atom_edge_r925 WHERE left_atom LIKE 'test-%'").fetchone()[0]==len(pairs)
    c.execute('PRAGMA wal_checkpoint(TRUNCATE)').fetchall()
    c.execute('PRAGMA journal_mode=DELETE').fetchall()
    c.execute('PRAGMA auto_vacuum=NONE')
    c.execute('VACUUM')
    assert c.execute('PRAGMA auto_vacuum').fetchone()==(0,)
    assert c.execute('PRAGMA freelist_count').fetchone()==(0,)
    used=c.execute('PRAGMA page_count').fetchone()[0]
    page=c.execute('PRAGMA page_size').fetchone()[0]
print('ARCHIVED_METADATA',len(archived),sum(len(row['rows']) for row in archived.values()),projected_blobs,projected_blob_bytes,flush=True)
assert page==4096 and 100000 < used < 1150000
with closing(sqlite3.connect(local)) as c:
    core={'app_state','research_evidence_archive_v2','research_evidence_locator_v2'}
    exists={name for name, in c.execute("SELECT name FROM sqlite_master WHERE type='table'")}
    assert core<=exists, sorted(core-exists)
    seed_check=c.execute('PRAGMA quick_check').fetchall()
    assert seed_check==[('ok',)],seed_check[:10]
    c.execute('PRAGMA journal_mode=OFF')
    c.execute('PRAGMA synchronous=OFF')
    # Let SQLite build its own free list, including its reserved lock-byte page.
    c.execute('CREATE TABLE fixture_storage_padding(value BLOB)')
    target=5018628096
    while True:
        remaining=target-c.execute('PRAGMA page_count').fetchone()[0]*page
        if remaining<=0:
            break
        c.execute('INSERT INTO fixture_storage_padding VALUES (zeroblob(?))',(min(32<<20,remaining),))
    c.execute('DROP TABLE fixture_storage_padding')
    c.commit()
    pages=c.execute('PRAGMA page_count').fetchone()[0]
    check=c.execute('PRAGMA quick_check').fetchall()
    assert check==[('ok',)],check[:10]
size=local.stat().st_size
assert target<=size<=target+(1<<20) and size==pages*page,(target,size,pages)
# This is a stopped, disposable emulator after a verified setup export.
assert ci.adb(serial,'shell','getprop','ro.kernel.qemu')=='1'
ci.adb(serial,'shell','rm','-f',live+'-wal',live+'-shm')
subprocess.run(['adb','-s',serial,'push','-z','lz4',str(local),live],check=True,timeout=600)
ci.adb(serial,'shell','chown',identity,live)
ci.adb(serial,'shell','restorecon',live)
assert int(ci.adb(serial,'shell','stat','-c','%s',live))==size
# Recreate the observed private-file cardinality without copying any user file content.
import io, tarfile
tree=tmp/'field-files.tar'
with tarfile.open(tree,'w') as tar:
    for number in range(17233):
        entry=tarfile.TarInfo('field-inventory-208/%03d/%05d.receipt'%(number//128,number))
        payload=b'field-size regression\n'
        entry.size=len(payload)
        tar.addfile(entry,io.BytesIO(payload))
ci.adb(serial,'push',str(tree),'/data/local/tmp/field-files.tar')
ci.adb(serial,'shell','tar','-xf','/data/local/tmp/field-files.tar','-C',f'/data/user/0/{pkg}/files')
field=f'/data/user/0/{pkg}/files/field-inventory-208'
ci.adb(serial,'shell','chown','-R',identity,field)
ci.adb(serial,'shell','restorecon','-R',field)
# Inert historical acknowledgements model the handset's 320-entry control directory.
with tarfile.open(tree,'w') as tar:
    for number in range(315):
        request=hashlib.sha256(('closed-fixture-%d'%number).encode()).hexdigest()
        entry=tarfile.TarInfo(request+'.ack.json')
        payload=json.dumps(dict(requestId=request,jobId=request,stage='FAILED',reason='DISPOSABLE_CLOSED_FIXTURE',failureDetailR32='test terminal receipt '*264)).encode()
        entry.size=len(payload)
        tar.addfile(entry,io.BytesIO(payload))
ci.adb(serial,'push',str(tree),'/data/local/tmp/field-history.tar')
requests=f'/data/user/0/{pkg}/files/r981_reports/requests'
ci.adb(serial,'shell','mkdir','-p',requests)
ci.adb(serial,'shell','chown','-R',identity,f'/data/user/0/{pkg}/files/r981_reports')
ci.adb(serial,'shell','tar','-xf','/data/local/tmp/field-history.tar','-C',requests)
ci.adb(serial,'shell','chown','-R',identity,requests)
ci.adb(serial,'shell','restorecon','-R',requests)
control_stats=ci.adb(serial,'shell','du','-sk',requests)
print('CONTROL_FIXTURE',control_stats,flush=True)
profile='COMPACT_DIAGNOSTIC'
stop=threading.Event()
events={}
sections={}
root='/data/user/0/'+pkg+'/files/r981_reports'
def observe():
    while not stop.is_set():
        try:
            current=json.loads(ci.adb(serial,'shell','cat',root+'/current-job.json'))['jobId']
            if re.fullmatch('[a-f0-9]{64}',current):
                job=root+'/jobs/'+current
                sample=json.loads(ci.adb(serial,'shell','cat',job+'/stage-progress.json'))
                stage=sample['stage']
                if stage=='INDEXING':
                    label=sample.get('checkStep') or sample.get('indexSection','')
                    if sample.get('activeCheck') and sample.get('checkStep'):
                        label=sample['activeCheck']+' / '+label
                    sections.setdefault(current,{}).setdefault(label,time.monotonic())
                started=sample.get('startedWallMs',0)
                if started:
                    events.setdefault(current,{})[stage]=started
                journal=json.loads(ci.adb(serial,'shell','cat',job+'/job.json'))
                if journal.get('stage')=='READY_TO_SHARE':
                    events.setdefault(current,{})['READY_TO_SHARE']=journal['updatedWallMs']
        except (KeyError,ValueError,RuntimeError,subprocess.SubprocessError):
            pass
        stop.wait(0.75)
monitor=threading.Thread(target=observe,daemon=True)
monitor.start()
start=time.monotonic()
try:
    ui.commit = new_source
    ui.out = {'reportReadbacks': [], 'activeProfile': profile}
    ui.report_entry(ui.CREATE)
    dialog=ui.wait_for_report('field207-compact')
    expected=['一次保存完整ZIP文件','分享完整ZIP','保存到文件夹']
    actions=[node.get('text') for node in dialog.iter('node') if node.get('text') in expected]
    assert actions==expected,actions
    candidate=ui.save_report(dialog,'field207-compact')
    print('FIELD_ZIP_READBACK', candidate['bytes'], candidate['sha256'], events.get(candidate['jobId'],{}), flush=True)
    assert candidate['status']=='VERIFIED_ANDROID_SAF_ZIP' and not candidate['rawEvidenceIncluded']
    with zipfile.ZipFile(ui.OUT/candidate['file']) as zip_saved:
        source_manifest=json.loads(zip_saved.read('source-manifest.json'))
        assert source_manifest['apkSha256']==ci.sha(tmp/'candidate-tested.apk')
        assert source_manifest['rawEvidenceIncluded'] is False
        assert 'snapshot.db' not in zip_saved.namelist() and 'installed-app.apk' not in zip_saved.namelist()
        assert 'DIAGNOSTIC_SCOPE.json' in zip_saved.namelist()
    result=dict(candidate,sourceCommit=new_source)
except Exception:
    print('STAGE_TIMES',json.dumps(events),flush=True)
    link=root+'/current-job.json'
    try:
        job_id=json.loads(ci.adb(serial,'shell','cat',link))['jobId']
        ack=json.loads(ci.adb(serial,'shell','cat',root+'/requests/'+job_id+'.ack.json'))
        print('EXPORT_FAILURE',json.dumps({key:ack.get(key) for key in ('stage','reason','failurePhaseR32','failureDetailR32','reportSourcePreparation')}),flush=True)
    except Exception as probe:
        print('EVIDENCE_READ',str(probe)[:300],flush=True)
    recent=ui.OUT/'field207-compact-progress.json'
    if recent.is_file():
        status=json.loads(recent.read_text())
        print('DELIVERY_STATUS',str(status[-1])[:2000],flush=True)
    raise
finally:
    stop.set()
    monitor.join(timeout=6)
    (out/'sampled-stage-transitions.json').write_text(json.dumps(events,indent=2))
elapsed=time.monotonic()-start
assert result['status']=='VERIFIED_ANDROID_SAF_ZIP' and result['sourceCommit']==new_source
with zipfile.ZipFile(ui.OUT/result['file']) as reopened:
    original_archive_path=source/'construction/plans/STARCORE_R34_3_REPAIR_20261126/report/ORIGINAL_DIAGNOSTIC_173.zip'
    with zipfile.ZipFile(original_archive_path) as outer:
        with zipfile.ZipFile(io.BytesIO(outer.read(outer.namelist()[0]))) as historical:
            old_names=set(historical.namelist())
            current_names=set(reopened.namelist())
            legacy_fixed={
                'ARCHIVE_MANIFEST.jsonl', 'DIAGNOSTIC_SCOPE.json', 'FILE_STATE.jsonl',
                'R26_FROZEN_SURFACE.json', 'R26_POLICY_IDENTITIES.jsonl', 'R26_REPORT.json', 'R27_REPORT.md',
                'R30_REPORT.json', 'R30_REPORT.md', 'R31_SEMANTIC_RECEIPT.json', 'REPORT_DETAILS.txt',
                'STORAGE_CENSUS.txt', 'STRATEGY_MATRICES.json', 'UPGRADE_DIAGNOSIS.json', 'UPGRADE_DIAGNOSIS.md',
                'attachments.jsonl', 'source-manifest.json',
            }
            assert legacy_fixed <= old_names
            print('CONDITIONAL_MEMBERS_NOT_IN_FIXTURE', sorted(old_names-current_names-legacy_fixed), flush=True)
            missing=sorted(legacy_fixed-current_names)
            old_scope=json.loads(historical.read('DIAGNOSTIC_SCOPE.json'))
            new_scope=json.loads(reopened.read('DIAGNOSTIC_SCOPE.json'))
            print('LEGACY_FIXED_MEMBERS',len(legacy_fixed),'MISSING',missing,flush=True)
            assert not missing,missing
            assert set(old_scope)<=set(new_scope),sorted(set(old_scope)-set(new_scope))
            for entry,key,id_column in [('R26_REPORT.json','sections','sectionId'),('R30_REPORT.json','chapters','sectionId'),('R30_REPORT.json','metricResults','id'),('R30_REPORT.json','researchTables','id')]:
                old_doc=json.loads(historical.read(entry))
                new_doc=json.loads(reopened.read(entry))
                old_ids={item[id_column] for item in old_doc[key]}
                new_ids={item[id_column] for item in new_doc[key]}
                print('LEGACY_COLLECTION',entry,key,len(old_ids),len(new_ids),sorted(old_ids-new_ids),flush=True)
                assert not old_ids-new_ids,(entry,key,old_ids-new_ids)
            for name in ['R27_REPORT.md','UPGRADE_DIAGNOSIS.md','STRATEGY_MATRICES.json','REPORT_DETAILS.txt','FILE_STATE.jsonl']:
                assert reopened.getinfo(name).file_size>0,name
            def catalog_ids(z):
                result=set()
                for name in z.namelist():
                    if re.fullmatch(r'catalog-\d+\.jsonl',name):
                        for line in z.open(name):
                            row=json.loads(line)
                            if row.get('kind')=='check_result':result.add(row['id'])
                return result
            old_ids=catalog_ids(historical)
            new_ids=catalog_ids(reopened)
            print('LEGACY_CHECKS',len(old_ids),len(new_ids),sorted(old_ids-new_ids),flush=True)
            assert len(old_ids)==len(new_ids)==96 and not old_ids-new_ids,old_ids-new_ids
            print('ZIP_COMPARISON',json.dumps({'historicalBytes':19670861,'historicalMembers':len(old_names),'actualSavedBytes':result['bytes'],'actualSavedMembers':len(current_names),'fixedMembersCovered':len(legacy_fixed),'requiredChecksCovered':len(old_ids),'physicalDevice':False}),flush=True)
    manifest=json.loads(reopened.read('source-manifest.json'))
    if profile=='FULL_RAW':
        assert result['rawEvidenceIncluded'] and result['snapshotQuickCheck']=='ok' and 'snapshot.db' in reopened.namelist()
    else:
        assert not result['rawEvidenceIncluded'] and 'DIAGNOSTIC_SCOPE.json' in reopened.namelist()
    if profile=='COMPACT_DIAGNOSTIC':
        inventory=[json.loads(line) for line in reopened.open('FILE_STATE.jsonl')]
        assert sum(r.get('state')=='FILE_PRESENT' and r.get('path','').startswith('files/field-inventory-208/') for r in inventory)==17233
        scope=json.loads(reopened.read('DIAGNOSTIC_SCOPE.json'))
        tables={r['table']:r for r in scope['tables']}
        recent={r['table']:r for r in scope['recentEvidence']}
        for name,original in archived.items():
            expected={json.dumps(row,sort_keys=True) for row in original['rows']}
            emitted={json.dumps(row,sort_keys=True) for row in recent[name]['rows']}
            assert expected<=emitted,(name,len(expected),len(emitted))
        for table,column,prefix,n,k in [('v57_runtime_event_r926','stage','F207_STAGE_',718660,23),('v57_fetch_ledger_r925','status','F207_STATUS_',164173,8)]:
            groups={r['value']:r['rows'] for r in tables[table]['states'] if r['column']==column and str(r['value']).startswith(prefix)}
            assert groups=={prefix+('%02d'%i): n//k+(1 if 0<i<=n%k else 0) for i in range(k)},(table,groups)
            assert recent[table]['totalRows']==tables[table]['rows']>=n and recent[table]['exportedRows']==5000
            assert any(str(row.get(column,'')).startswith(prefix) for row in recent[table]['rows'])
    checks={}
    for member in sorted(reopened.namelist()):
        if re.fullmatch(r'catalog-\d+\.jsonl',member):
            for line in reopened.open(member):
                row=json.loads(line)
                if row.get('kind')=='check_result':checks[row['id']]=row['execution_state']
    failures={ident:state for ident,state in checks.items() if state!='EXECUTED'}
    for name in reopened.namelist():
        if not re.fullmatch(r"catalog-\d+\.jsonl",name): continue
        for line in reopened.open(name):
            entry=json.loads(line)
            if entry.get("kind")!="check_result" or entry.get("id") not in failures: continue
            detail=str(entry.get("error_detail",""))
            trace=detail.replace("\\n","\n").splitlines()
            print("READBACK_FAILED_CHECK",entry["id"],entry.get("reason"),"\n".join(trace[:32])[:7000],flush=True)
    print("READBACK_CHECK_SUMMARY",len(checks),len(failures),json.dumps(failures),round(elapsed,2),flush=True)
    check_rows={}
    for member in reopened.namelist():
        if re.fullmatch(r'catalog-\d+\.jsonl',member):
            for line in reopened.open(member):
                row=json.loads(line)
                if row.get('kind')=='check_result' and row['id'] in ('RSEC-010','RSEC-011'):
                    check_rows[row['id']]=row
    def full_check_text(row):
        ref=re.fullmatch(r'\x1eSTARCORE_REPORT_REF_CP12\|(report-text-[a-f0-9]{64}\.txt)\|([a-f0-9]{64})\|(\d+)\x1e',row['evidence'])
        if not ref:
            return row['evidence'].encode('utf-8')
        raw=reopened.read(ref.group(1))
        assert len(raw)==int(ref.group(3)) and hashlib.sha256(raw).hexdigest()==ref.group(2)
        return raw
    overview=full_check_text(check_rows['RSEC-010'])
    dashboard=full_check_text(check_rows['RSEC-011'])
    assert dashboard in overview and '[F06概率与修复证据]'.encode() in overview
    assert len(overview)>22000,len(overview)
    # Other seeded UI states have active runtime writers between launch and snapshot.
    # The two stable bound fields and the complete appended dashboard are the readback witnesses.
    markers=[index for index in range(6) if f'R211_OVERVIEW_{index}_'.encode() in overview]
    print('OVERVIEW_WITNESSES', markers, 'overview_bytes',len(overview), 'dashboard_bytes',len(dashboard),flush=True)
    assert {0,1} <= set(markers),markers
    if b'FIX210_TEXT_60_' in dashboard:
        for index in range(11,61):
            assert f'FIX210_TEXT_{index:02d}_'.encode() in dashboard,index
    r26=json.loads(reopened.read('R26_REPORT.json'))
    preview=next(section for section in r26['sections'] if section['sectionId']=='FAILURES')
    # The live failure ledger keeps its most recent 2001 entries. Check the exact
    # captured denominator against the independent inventory of the same snapshot.
    history_rows=tables['r27_task_error']['rows']
    assert preview['status']=='PARTIAL' and 64 < history_rows==preview['availableRows'],(history_rows,preview['status'],preview['availableRows'])
    assert preview['exportedRows']==64 and preview['omittedRows']==preview['availableRows']-64,preview
    fixture_preview=[row['message'] for row in preview['metrics'] if row.get('error_class')=='R211_FIXTURE']
    assert fixture_preview and fixture_preview[0].startswith('R211_ERROR_180000'),len(fixture_preview)
    print('R26_EXACT_DENOMINATOR',preview['availableRows'],preview['exportedRows'],preview['omittedRows'],flush=True)
    print('STALLED_CHECKS_EXECUTED',len(overview),len(dashboard), 'SEEDED_CONTRIBUTION_WITNESSES='+(str(b'FIX210_TEXT_60_' in dashboard)),json.dumps({k:checks[k] for k in check_rows}),flush=True)
    if profile=='FULL_RAW':assert result['snapshotBytes']>=size

saved={}
for attempt in range(90):
    names=ci.adb(serial,'shell','ls',root+'/saf').split()
    for name in names:
        if not re.fullmatch(r'[a-f0-9]{64}\.json',name):
            continue
        try:
            receipt=json.loads(ci.adb(serial,'shell','cat',root+'/saf/'+name))
        except (ValueError,RuntimeError,subprocess.SubprocessError):
            continue
        if receipt.get('jobId')==result['jobId'] and receipt.get('state')=='VERIFIED_READBACK' and receipt.get('actualSha256')==result['sha256']:
            saved=receipt
            break
    if saved:break
    time.sleep(1)
assert saved and saved['actualBytes']==result['bytes'] and saved['allVolumesSaved'] and saved['volumeName']=='STARCORE_FULL_REPORT.zip',saved
assert 0 < result['bytes'] < manifest['snapshotBytes']
assert manifest['snapshotBytes']>=size and manifest['snapshotSha256']==result['snapshotSha256']
receipt={'profile':profile,'publicSaveReadback':saved['state'],'publicSha256':saved['actualSha256'],'dialogActions':actions,'rawDatabaseIncluded':False,'includedCompactScope':True,'stageStartsWallMs':events.get(result['jobId'],{}),'dashboardCheck':checks['RSEC-011'],'allCheckStates':checks,'fixtureEventRows':718660,'fixtureFetchRows':164173,'fixtureEdgeRows':len(pairs),'verifiedSnapshotBytes':manifest['snapshotBytes'],'exportedZipBytes':result['bytes'],'scope':'API34_SYNTHETIC_LOAD_WITH_SEALED_173_LONG_AND_PROBE_METADATA','archivedRows':{n:len(v['rows']) for n,v in archived.items()},'syntheticBinaryCells':projected_blobs,'syntheticBinaryBytes':projected_blob_bytes,'fileFixtureEntries':17233,'historicalAcks':315,'sampledIndexSections':list(sections.get(result['jobId'],{})),'source':new_source,'before':before,'after':after,'sourceBytesBeforeLaunch':size,'populatedPagesBeforePadding':used,'secondsFromUiEntryThroughReadback':round(elapsed,2),'underTwentyMinutes':elapsed<1200,'checks':result['checks'],'metrics':result['metrics'],'jobId':result['jobId'],'physicalDevice':False}
(out/'field-size-android-readback.json').write_text(json.dumps(receipt,indent=2)+'\n')
print(json.dumps(receipt),flush=True)
assert len(checks)==96 and not failures,failures

with Path(os.environ['GITHUB_STEP_SUMMARY']).open('a') as summary:
    summary.write('\n```json\n'+json.dumps(receipt)+'\n```\n')
