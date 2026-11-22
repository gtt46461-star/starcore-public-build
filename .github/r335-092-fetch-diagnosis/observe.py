"""Count only production-created objects following a normal start on a disposable Android 14 guest."""
from pathlib import Path
import importlib.util,json,os,subprocess,sys,time
kit=Path('.github/r335-independent-window').resolve()
sys.path.insert(0,str(kit))
spec=importlib.util.spec_from_file_location('research_live_app',kit/'observe.py')
observer=importlib.util.module_from_spec(spec);spec.loader.exec_module(observer)
f=observer.base
source='d9c14fac75c2fb18a07ecc0710dd7ab6478ad1d0'
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
out=f.TEMP/('R335-092-fetch-diagnosis-'+os.environ['GITHUB_RUN_ID']+'.json')
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
    if 'r27_replay_dataset' in known:row['maximumReplayRows']=int(query("SELECT coalesce(max(rows),0) FROM r27_replay_dataset"))
    if 'r26_candidate' in known:
        row['candidateCursorMax']=int(query('SELECT coalesce(max(cursor),0) FROM r26_candidate'))
        row['candidateCursorSum']=int(query('SELECT coalesce(sum(cursor),0) FROM r26_candidate'))
    record['observations'].append(row);save()
def telemetry(begin):
    # All times below come from the normal app's persisted telemetry in this disposable guest.
    # The window includes missing request timings; no monitoring requests are sent to the provider.
    sql=f'''WITH live AS (
       SELECT t.* FROM r31_fetch_timing t JOIN v57_fetch_ledger_r925 l USING(request_id)
       WHERE l.request_role='LIVE' AND t.body_complete_wall_ms>={begin}-10000
         AND t.body_complete_wall_ms>0 AND t.accepted=1 AND t.body_truncated=0
      ), targets AS (
       SELECT issue,open_time,first_seen_at FROM draws WHERE dirty=0 AND open_time>={begin}
       ORDER BY open_time LIMIT 20
      )
      SELECT d.issue,d.open_time,d.first_seen_at,early.request_wall_ms,early.headers_wall_ms,
        early.first_byte_wall_ms,early.body_complete_wall_ms,early.parse_wall_ms,early.validated_wall_ms,
        early.finished_wall_ms,early.request_mono_ms,early.headers_mono_ms,early.body_complete_mono_ms,
        early.parse_mono_ms,early.validated_mono_ms,early.finished_mono_ms,
        (SELECT max(q.body_complete_wall_ms) FROM live q WHERE q.issue<d.issue AND
          q.request_wall_ms>=d.open_time-5000 AND q.body_complete_wall_ms>=d.open_time
          AND q.body_complete_wall_ms<early.body_complete_wall_ms),
        (SELECT count(*) FROM live q WHERE q.issue<d.issue AND q.request_wall_ms>=d.open_time
          AND q.body_complete_wall_ms<early.body_complete_wall_ms),
        tm.wall_draw_committed FROM targets d
        LEFT JOIN live early ON early.request_id=(SELECT q.request_id FROM live q WHERE q.issue=d.issue
            ORDER BY q.body_complete_wall_ms,q.request_id LIMIT 1)
        LEFT JOIN r30_issue_timeline tm ON tm.issue=d.issue ORDER BY d.open_time'''
    cols=('issue','open','firstSeen','request','headers','firstByte','body','parse','validated','finished',
          'requestMono','headersMono','bodyMono','parseMono','validatedMono','finishedMono',
          'lastEarlierIssueResponseAfterOpen','completedEarlierIssueRequestsAfterOpen','drawCommit')
    raw=query(sql)
    rows=[]
    for line in raw.splitlines():
        values=line.split('|')
        if len(values)!=len(cols) or not values[0].isdigit():raise ValueError('Unexpected telemetry shape')
        rows.append({key:(int(v) if v else None) for key,v in zip(cols,values)})
    return rows

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
    target=observer.get_apk('B08',577311092,source)
    digest=next(line.split()[0] for line in (f.TEMP/'B08-SUMS').read_text().splitlines() if line.endswith(source[:12]+'-release.apk'))
    record['apkSha256']=digest
    assert f.fingerprint(target)==digest and b'Success' in f.adb('install','-r',str(target))
    assert f.installed(f.EVID/'current.txt',577311092)==(olduid,digest)
    record['uiStart']=observer.tap_start();save();assert record['uiStart']
    start=time.monotonic()
    begin=int(f.adb('shell','date','+%s').strip())*1000+1000
    record.update(scope='DISPOSABLE_ANDROID_NORMAL_SERVICE_OBSERVED_LIVE_REQUEST_PHASES_ONLY',
                  deviceStartWallMs=begin)
    for at in (60,180,360,660):
        time.sleep(max(0,start+at-time.monotonic()))
        try:
            record['periods']=telemetry(begin)
            record['samples']=record.get('samples',[])+[{'elapsedSec':int(time.monotonic()-start),'rows':len(record['periods'])}]
        except (RuntimeError,subprocess.SubprocessError,ValueError) as error:
            record.setdefault('readFailures',[]).append(type(error).__name__)
        save()
    assert f.installed(f.EVID/'end.txt',577311092)==(olduid,digest)
    record['completed']=True;save()
try:main()
except Exception as error:
    record.update(completed=False,failureType=type(error).__name__);save();raise
