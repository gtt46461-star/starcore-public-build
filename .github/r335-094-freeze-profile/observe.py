"""Count only production-created objects following a normal start on a disposable Android 14 guest."""
from pathlib import Path
import importlib.util,json,os,subprocess,sys,time
kit=Path('.github/r335-independent-window').resolve()
sys.path.insert(0,str(kit))
spec=importlib.util.spec_from_file_location('research_live_app',kit/'observe.py')
observer=importlib.util.module_from_spec(spec);spec.loader.exec_module(observer)
f=observer.base
source='d417e459c1bd0e89d70b4dd549f5555b93e49d4e'
digest=None
record={'sourceCommit':source,'apkSha256':None,'scope':'SIGNED_DISPOSABLE_ANDROID_NUMERIC_LATENCY_DISTRIBUTIONS_ONLY','observations':[]}
out=f.TEMP/('R335-094-freeze-profile-'+os.environ['GITHUB_RUN_ID']+'.json')
def save():out.write_text(json.dumps(record,indent=2)+'\n')
def query(stmt):
    path='/data/user/0/'+f.PKG+'/databases/'+observer.DB
    proc=subprocess.run(['adb','shell','sqlite3','-batch','-readonly',path],input='.timeout 4000\nPRAGMA query_only=ON;\n'+stmt+';\n',capture_output=True,text=True,timeout=25)
    if proc.returncode or proc.stderr.strip():raise ValueError('SQL_READ_INCOMPLETE')
    return proc.stdout.strip()

def distribution(values):
    import math
    values=sorted(values)
    return {'count':len(values),'p50Ms':values[math.ceil(.50*len(values))-1] if values else None,
            'p95Ms':values[math.ceil(.95*len(values))-1] if values else None,
            'maxMs':max(values,default=None),'over1s':sum(v>1000 for v in values),'over5s':sum(v>5000 for v in values)}
def integers(sql, count):
    result=[]
    for row in query(sql).splitlines():
        token=row.split('|')
        if len(token)!=count:raise ValueError('UNEXPECTED_NUMERIC_ROW')
        numbers=[int(v) if v else None for v in token]
        result.append(numbers)
    return result
def phase_summary(begin):
    import re
    summary={}
    keys=['queue','compute','commit','requestToDurable']
    times=integers('SELECT queue_ms,compute_ms,commit_ms,total_ms FROM v57_prediction_timing_r925 WHERE release_code=577311094 AND request_at>='+str(begin),len(keys))
    summary['nativeRequest']= {k:distribution([row[i] for row in times if row[i] is not None and row[i]>=0]) for i,k in enumerate(keys)}
    keys=['fromOpenToFirstIngest','fromOpenToSettlementRecord','fromFirstIngestToSettlementRecord']
    sql="""SELECT d.first_seen_at-d.open_time,t.completed_at-d.open_time,t.completed_at-d.first_seen_at
        FROM draws d LEFT JOIN v57_settlement_task_r36 t ON d.issue=t.target_issue AND t.status='COMPLETED' AND t.draw_issue=d.issue AND t.draw_hash=d.draw_hash_v2
        WHERE d.dirty=0 AND d.open_time>="""+str(begin)
    times=integers(sql,len(keys))
    summary['draw']= {k:distribution([row[i] for row in times if row[i] is not None and row[i]>=0]) for i,k in enumerate(keys)}
    stages=('FEATURES_SUB','STATE_SUB','NUMBER_MODEL','ACTION_BUILD_FUNNEL_META_RANK','SELECTION_POLICY','DECISION_GATE','SNAPSHOT_STREAM','MODEL_BUNDLE_OPEN_CP3')
    field={k:[] for k in stages}
    traces=query("SELECT e.payload_text FROM v57_runtime_event_r926 e JOIN v57_shadow_observation_r35 o ON e.target_issue=o.target_issue WHERE e.stage='CANONICAL_COMMITTED' AND o.commit_at>="+str(begin))
    for trace in traces.splitlines():
        for name in stages:
            for ms in re.findall(r'(?:^|\|)'+name+r'=([0-9]{1,10})ms:(?:OK|FAIL)(?=\||$)',trace):field[name].append(int(ms))
    summary['completedNativeSpans']={k:distribution(v) for k,v in field.items()}
    freeze=integers("""SELECT MIN(h.commit_at)-o.commit_at
        FROM v57_shadow_observation_r35 o JOIN v57_runtime_health_r35 h ON h.target_issue=o.target_issue
        WHERE h.event_type='R335_CANONICAL_RETURN_BOUND' AND h.commit_at>=o.commit_at AND o.commit_at>="""
        +str(begin)+' GROUP BY o.target_issue',1)
    summary['observedReturnAfterFreezeRecord']=distribution([row[0] for row in freeze if row[0] is not None])
    return summary
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
    target=observer.get_apk('B08',577311094,source)
    digest=next(line.split()[0] for line in (f.TEMP/'B08-SUMS').read_text().splitlines() if line.endswith(source[:12]+'-release.apk'))
    record['apkSha256']=digest
    assert f.fingerprint(target)==digest and b'Success' in f.adb('install','-r',str(target))
    assert f.installed(f.EVID/'current.txt',577311094)==(olduid,digest)
    record['uiStart']=observer.tap_start();save();assert record['uiStart']
    start=time.monotonic()
    begin=int(f.adb('shell','date','+%s').strip())*1000+1000
    for deadline in (60,240,600,960):
        time.sleep(max(0,start+deadline-time.monotonic()))
        try:record['observations'].append({'elapsedSeconds':int(time.monotonic()-start),'stats':phase_summary(begin)})
        except (RuntimeError,subprocess.SubprocessError,ValueError) as err:record.setdefault('readFailures',[]).append({'type':type(err).__name__,'elapsedSeconds':int(time.monotonic()-start)})
        save()
    assert f.installed(f.EVID/'end.txt',577311094)==(olduid,digest)
    record['completed']=True;save()
try:main()
except Exception as error:
    record.update(completed=False,failureType=type(error).__name__);save();raise
