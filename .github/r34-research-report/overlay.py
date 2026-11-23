"""Count only production-created objects following a normal start on a disposable Android 14 guest."""
from pathlib import Path
import importlib.util,json,os,subprocess,sys,time
kit=Path('.github/r335-independent-window').resolve()
sys.path.insert(0,str(kit))
spec=importlib.util.spec_from_file_location('research_live_app',kit/'observe.py')
observer=importlib.util.module_from_spec(spec);spec.loader.exec_module(observer)
f=observer.base
source='273641e838e0b8dd842b42a67a674e184942a30a'
digest=None
tables=('draws','r26_feedback','r26_consumption','r26_foundry','r26_candidate','r26_evolution','r26_live_model','r26_training_step','r26_statistic_training_step','r27_replay_dataset','r27_policy','r27_live_frozen','r27_certificate','r30r1_analysis_run','r30r1_research_measure','r30r1_proposal','r30r1_consume','r30r1_chart_cache','r33_research_applied','r33_research_input_head','r31_hypothesis_registry','r31_task_lease','su2_certificate','v57_shadow_observation_r35','v57_issue_closure_r906','v57_settlement_task_r36','trades')
groups={
 'candidateStates':('r26_candidate','status',('READY','DONE','RUNNING','BLOCKED_HISTORY_GAP','FAILED')),
 'lineage':('r26_evolution','category',('REGISTERED_SEED','STRUCTURAL_CANDIDATE','NO_OP','ABLATION','MUTATION','CROSSOVER','FUSION')),
 'researchInput':('r33_research_input_head','state',('PRODUCED','WAITING_DATASET','WAITING_STUDENT','WAITING_SOURCE','INSUFFICIENT','FAILED')),
 'candidateFamilies':('r27_policy','idea_family',tuple(f'I{i:02}' for i in range(1,25))),
 'visibleViews':('r30r1_chart_cache','view_id',tuple(f'R{i:02}' for i in range(1,19))),
 'appliedViews':('r33_research_applied','view_id',tuple(f'R{i:02}' for i in range(1,19))),
 'taskWait':('r26_task','wait_kind',('NONE','READY','WAITING_NEW_DRAW','WAITING_FEEDBACK','WAITING_DATASET','WAITING_SCHEMA','RESOURCE','TIME','DATA','SCHEMA','FAULT','BACKOFF','INPUT','DEPENDENCY')),
 'taskStates':('r26_task','status',('READY','RUNNING','CLAIMED','WAITING','WAITING_DATA','RETRY','PAUSED','BLOCKED','DONE','FAILED','YIELDED','QUARANTINED','NEEDS_FIX','COMMIT_UNCERTAIN')),
 'settlementStates':('v57_settlement_task_r36','status',('PENDING','RUNNING','NO_PREDRAW_CANONICAL_TERMINAL','COMMITTED','SETTLED','SETTLED_SHADOW','SETTLED_REAL','RETRY','TERMINAL'))}
record={'sourceCommit':source,'apkSha256':digest,'scope':'PUBLIC_NUMERIC_COUNTS_ONLY_FROM_DISPOSABLE_GUEST_NORMAL_NETWORK_SERVICE_NO_FIXTURE_ROWS','observations':[]}
out=f.TEMP/('R335-095-research-import-'+os.environ['GITHUB_RUN_ID']+'.json')
def save():out.write_text(json.dumps(record,indent=2)+'\n')
def query(stmt):
    path='/data/user/0/'+f.PKG+'/databases/'+observer.DB
    proc=subprocess.run(['adb','shell','sqlite3','-batch','-readonly',path],input='.timeout 4000\nPRAGMA query_only=ON;\n'+stmt+';\n',capture_output=True,text=True,timeout=25)
    if proc.returncode or proc.stderr.strip():raise ValueError('SQL_READ_INCOMPLETE')
    return proc.stdout.strip()
def sample(elapsed):
    known=set(query("SELECT name FROM sqlite_master WHERE type='table'").splitlines())
    row={'elapsedSeconds':elapsed,'tableCount':len(known),'counts':{},'groups':{}}
    for table in tables:
        try:row['counts'][table]=int(query('SELECT count(*) FROM '+table)) if table in known else None
        except (ValueError,subprocess.TimeoutExpired):row['counts'][table]='READ_UNAVAILABLE'
    for label,(table,column,allowed) in groups.items():
        if table not in known:row['groups'][label]=None;continue
        try:
            grouped={}
            for item in query('SELECT '+column+',count(*) FROM '+table+' GROUP BY '+column).splitlines():
                token,number=item.rsplit('|',1)
                enum=token if token in allowed else 'OTHER'
                grouped[enum]=grouped.get(enum,0)+int(number)
            row['groups'][label]=grouped
        except (ValueError,subprocess.TimeoutExpired):row['groups'][label]='READ_UNAVAILABLE'
    task_names=('schema','schema-base','lifecycle-schema','foundry-su2','warm-candidates','publish-models','live-model-training','feedback','foundry-su1','legacy-generation','research-observatory','r27-search','research-action','r27-replay-dataset','r27-evolution','r27-live','r27-atomnet','r27-chart-cache')
    cause_names=('WAITING_SELF_CONTAINED_STRATEGY_STORAGE','WAITING_NEW_LIFECYCLE_SCHEMA_OBJECTS','COOPERATIVE_LIFECYCLE_SCHEMA_UNIT_COMMITTED','SCHEMA_PENDING','SOURCE_NOT_PRESENT','WRITER_BUSY','CP3_DB_BUSY_DEADLINE','R33_WAIT_WRITER_HOLD_FAULT_BACKLOG','WAITING_NEW_DRAW','WAITING_SOURCE','WAITING_STUDENT','WAITING_DATASET','DEPENDENCY','WAITING_DRAWS','NO_NEW_EVENT','DATASET_READ_SOURCE_CHANGED','SOURCE_CHANGED','NO_ADMISSIBLE','WRITER_HOLD_FAULT_BACKLOG','DATASET','NO_SOURCE','READY','UNSLICED_KERNEL','HISTORY_GAP','NO_ELIGIBLE','STALE','PREEMPT','BUDGET','CAPACITY','QUARANTINE','FAULT','COOPERATIVE')
    if 'r26_task' in known:
        row['taskEvidence']={}
        for line in query("SELECT task_id,status,steps,cursor,coalesce(blocked_reason,'') FROM r26_task").splitlines():
            values=line.split('|',4)
            if len(values)!=5 or values[0] not in task_names:continue
            task,status,steps,cursor,reason=values
            row['taskEvidence'][task]={'status':status if status in groups['taskStates'][2] else 'OTHER','steps':int(steps),'cursor':int(cursor),'causes':[code for code in cause_names if code in reason]}
    # Only counts for known production failure categories and call sites leave the guest.
    # Keep the raw fault text and SQLite database on the disposable runner.
    categories = (
        'RESOURCE_WAIT:MEMORY_DECLARED_CAPACITY','RESOURCE_WAIT:MEMORY_HEAP_PRESSURE',
        'RESOURCE_WAIT:MEMORY_EMERGENCY_PRESSURE','RESOURCE_WAIT:MEMORY_UNKNOWN_CAPACITY',
        'RESOURCE_WAIT:MEMORY_NATIVE_DECLARED_CAPACITY','RESOURCE_WAIT:MEMORY_OWNER_LIMIT',
        'R31_COMPUTE_CAPACITY_WAIT','R26_UNSLICED_COMPUTATION_CAPACITY','BIND_CAPACITY',
        'BLOCKED_PROTECTED_MANIFEST_DEBT','WAITING_PRIVATE_SCRATCH_CLEANUP','BLOCKED_PROTECTED_BYTES',
        'OWNER_LOGICAL_ENVELOPE','STAGING_SLICE_TOO_LARGE','PUBLICATION_BATCH_ENVELOPE',
        'PREPARED_MEMORY_ENVELOPE','R31_INTEGRITY_CONFLICT_COMMIT_PAYLOAD','R31_EVOLUTION_STALE',
        'R31_STALE_PREPARED_LEASE','CP3_DB_BUSY_DEADLINE','ACTIVE_SCHEMA_SESSION_TASK_LEASE_OR_RECOVERY',
        'r27-evolution','stepGate','EvolutionPublicationStoreR31','BirthGateR27','FeatureCacheR27',
        'MemoryBudgetR31','CooperativeContinuationR26','EvolutionPublicationSqlR31','RuntimeR27',
        'WRITER_HOLD_FAULT_BACKLOG','CANCELLED','CORRECTED_REPLAY_DATASET','R31_DEPENDENCY')
    if 'r31_runtime_fault' in known:
        row['knownEvolutionFaultCounts']={}
        for task_id in ('r27-evolution','r27-replay-dataset'):
            columns=','.join("coalesce(sum(case when instr(coalesce(last_cause_chain_json,'')||coalesce(last_stack,''),'"+name+"')>0 then occurrences else 0 end),0)" for name in categories)
            try:
                nums=[int(v) for v in query("select "+columns+" from r31_runtime_fault where task_id='"+task_id+"'").split('|')]
                if len(nums)!=len(categories):raise ValueError('INCOMPLETE_COUNT')
                row['knownEvolutionFaultCounts'][task_id]={name:n for name,n in zip(categories,nums) if n}
            except (ValueError,subprocess.TimeoutExpired):row['knownEvolutionFaultCounts'][task_id]={'readUnavailable':1}
    if 'r27_evolution_cursor' in known:
        try:row['lastPersistedEvolutionStep']=int(query("select coalesce(max(step),0) from r27_evolution_cursor"))
        except (ValueError,subprocess.TimeoutExpired):pass
    if 'r27_replay_dataset' in known:row['maximumReplayRows']=int(query("SELECT coalesce(max(rows),0) FROM r27_replay_dataset"))
    if 'r26_candidate' in known:
        row['candidateCursorMax']=int(query('SELECT coalesce(max(cursor),0) FROM r26_candidate'))
        row['candidateCursorSum']=int(query('SELECT coalesce(sum(cursor),0) FROM r26_candidate'))
    record['observations'].append(row);save()
def main():
    assert f.adb('shell','getprop','ro.kernel.qemu').strip()==b'1'
    f.EVID.mkdir(exist_ok=True);observer.RAW.mkdir(exist_ok=True,parents=True)
    f.adb('root');f.adb('wait-for-device')
    assert not f.adb('shell','pm','list','packages',f.PKG).strip()
    old=observer.get_apk('B08',577311065,'a724adbfb31dd96f1459aa2e5cbd94582dd6e82d')
    assert f.fingerprint(old)=='09af709a6dc462f0f883156d20a723c0eb60a63aa6a43896e2c65ffa1905cced'
    assert b'Success' in f.adb('install',str(old))
    f.adb('shell','pm','grant',f.PKG,'android.permission.POST_NOTIFICATIONS')
    f.adb('shell','am','start','-W','-n',f.ACTIVITY);time.sleep(30)
    olduid,_=f.installed(f.EVID/'prior.txt',577311065)
    global digest
    target=observer.get_apk('B08',577311095,source)
    digest=next(line.split()[0] for line in (f.TEMP/'B08-SUMS').read_text().splitlines() if line.endswith(source[:12]+'-release.apk'))
    record['apkSha256']=digest
    assert f.fingerprint(target)==digest and b'Success' in f.adb('install','-r',str(target))
    assert f.installed(f.EVID/'current.txt',577311095)==(olduid,digest)
    record['scope']='PUBLIC_HISTORICAL_REPLAY_ONLY_WITH_NORMAL_SERVICE_NO_NATURAL_QUALIFICATION';record['uiStart']=observer.tap_start();save();assert record['uiStart']
    from import_ui import import_history
    chart_path=Path('.github/r335-095-panel-readback/observe.py').resolve()
    chart_spec=importlib.util.spec_from_file_location('history_chart_reader',chart_path)
    charts=importlib.util.module_from_spec(chart_spec)
    chart_spec.loader.exec_module(charts)
    assert charts.source==source and charts.digest==digest
    dex=chart_path.parent/'observer.dex.jar'
    remote='/data/local/tmp/read-only-import-chart-observer.jar'
    assert f.fingerprint(dex)==charts.observer_digest
    f.adb('push',str(dex),remote)
    f.adb('shell','chmod','444',remote)
    assert f.adb('shell','sha256sum',remote).decode().split()[0]==charts.observer_digest
    installed=f.adb('shell','pm','path',f.PKG).decode().strip().split('package:')[-1]
    assert f.adb('shell','sha256sum',installed).decode().split()[0]==digest
    capture_start=time.monotonic()
    def read_panels():
        charts.snapshot(installed,remote,int(time.monotonic()-capture_start))
        record.setdefault('installedChartReads',[]).append(charts.record['samples'][-1])
        save()
    try:
        import_history(f.PKG,query,record,save)
    finally:
        read_panels()
    record['scope']='PUBLIC_HISTORICAL_REPLAY_ONLY_WITH_NORMAL_SERVICE_NO_NATURAL_QUALIFICATION';save()
    start=time.monotonic()
    for at in (30,180,600):
        time.sleep(max(0,start+at-time.monotonic()))
        sample(int(time.monotonic()-start))
        if at in (600,2400,3600):read_panels()
        if at==180:
            f.adb('shell','input','keyevent','KEYCODE_HOME');record['backgroundAfterSeconds']=180;save()
    report_path=Path('.github/r34-research-report/warm_report.py').resolve()
    report_spec=importlib.util.spec_from_file_location('warm_report',report_path)
    warm=importlib.util.module_from_spec(report_spec);report_spec.loader.exec_module(warm)
    assert warm.installed(122)==olduid
    warm.adb('shell','input','keyevent','KEYCODE_WAKEUP')
    warm.adb('shell','wm','dismiss-keyguard')
    warm.adb('shell','wm','size','480x850')
    warm.adb('shell','wm','density','195')
    warm.adb('shell','settings','put','system','screen_off_timeout','7200000')
    warm.adb('shell','svc','power','stayon','true')
    warm.adb('shell','am','start','-W','--activity-new-task','--activity-clear-task','-n',warm.ACTIVITY)
    time.sleep(8)
    try:
        for i,profile in enumerate(['COMPACT_DIAGNOSTIC','FULL_RAW'],1):
            warm.report(122,i,profile)
            record['warmedReports']=warm.receipt['reports'];save()
    finally:
        record['warmedReports']=warm.receipt['reports'];save()
    assert f.installed(f.EVID/'end.txt',577311095)==(olduid,digest)
    record['completed']=True;save()
try:main()
except Exception as error:
    record.update(completed=False,failureType=type(error).__name__)
    import re
    if re.fullmatch('[A-Z0-9_]{1,80}',str(error)):record['driverReason']=str(error)
    try:
        from xml.etree import ElementTree
        latest=next((f.TEMP/'test-documents').glob('entry-*-last.xml'))
        nodes=list(ElementTree.fromstring(latest.read_bytes()).iter('node'))
        record['lastUi']={'app':sum(n.get('package')==f.PKG for n in nodes),'buttons':sum(n.get('class')=='android.widget.Button' for n in nodes),'scrollers':sum(n.get('scrollable')=='true' for n in nodes),'labels':[t for t in ['导入','完整报告','检测报告','快速诊断','开始','继续','确定','返回','等待','停止','训练','备份'] if any(t in n.get('text','') for n in nodes)]}
    except (StopIteration,OSError,ValueError):pass
    import traceback
    record['failureFrames']=[Path(frame.filename).name+':'+str(frame.lineno) for frame in traceback.extract_tb(error.__traceback__)][-8:]
    save();raise
