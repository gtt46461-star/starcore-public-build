"""Choose a reviewed repository CSV using the installed ChartsActivity and Android document picker."""
from pathlib import Path
import csv,hashlib,io,os,re,subprocess,time,xml.etree.ElementTree as ET

def import_history(package, query, record, save):
    def adb(*args):
        return subprocess.check_output(['adb',*args],timeout=60)
    assert adb('shell','getprop','ro.kernel.qemu').strip()==b'1'
    source=Path('app-source/research_data/ethpk10_draws_20260316_201059.csv')
    original=source.read_bytes();lines=original.decode('utf-8-sig').splitlines(keepends=True)
    rows=[(row[0],line) for line in lines[1:] for row in csv.reader([line]) if len(row)==8 and row[6]=='1']
    chosen=sorted(rows,key=lambda x:x[0])[-5000:]
    assert len({p for p,_ in chosen})==len(chosen)==5000
    fixture=Path(os.environ['RUNNER_TEMP'])/'starcore-review-5000.csv'
    fixture.write_text(lines[0]+''.join(line for _,line in chosen))
    record['input']={'role':'REPOSITORY_HISTORICAL_REPLAY_ONLY','sourceSha256':hashlib.sha256(original).hexdigest(),'selectedSha256':hashlib.sha256(fixture.read_bytes()).hexdigest(),'selectedRows':5000}
    save()
    location='/sdcard/Download/'+fixture.name
    adb('push',str(fixture),location)
    adb('shell','am','broadcast','-a','android.intent.action.MEDIA_SCANNER_SCAN_FILE','-d','file://'+location)
    def hierarchy():
        subprocess.run(['adb','shell','uiautomator','dump','/sdcard/r335-research-import.xml'],capture_output=True,timeout=45,check=True)
        return ET.fromstring(adb('shell','cat','/sdcard/r335-research-import.xml'))
    def tap_text(choices,attempts=5):
        for _ in range(attempts):
            try: current=hierarchy()
            except (ET.ParseError,subprocess.CalledProcessError):
                time.sleep(3);continue
            for node in current.iter('node'):
                label=' '.join(node.attrib.get(k,'') for k in ('text','content-desc','resource-id'))
                if not any(choice in label for choice in choices):continue
                coord=list(map(int,re.findall(r'\d+',node.attrib['bounds'])))
                if len(coord)==4 and coord[2]>coord[0] and coord[3]>coord[1]:
                    adb('shell','input','tap',str((coord[0]+coord[2])//2),str((coord[1]+coord[3])//2));time.sleep(1.2);return True
            time.sleep(2)
        return False
    # An empty Research schema records a genuine dependency until startup makes the official import owner ready.
    for _ in range(50):
        if query("SELECT count(*) FROM sqlite_master WHERE type='table' AND name IN ('r31_history_import_run','r31_history_import_row')")=='2':break
        time.sleep(6)
    activity=package+'/com.ethk3.novaonepro.ChartsActivityR83'
    adb('shell','am','start','-W','-n',activity)
    def stage(code):
        record['lastImportUiStep']=code
        save()
    def start_or_resume(staged):
        stage('OPEN_CHART_TOOLS')
        assert tap_text(['R27 工具']),'CHART_TOOLS_UNAVAILABLE'
        stage('WAIT_TOOL_ACTION')
        if staged:
            assert tap_text(['恢复未完成历史CSV（'],20),'RECOVERY_ACTION_UNAVAILABLE'
            return
        assert tap_text(['导入历史CSV（'],20),'IMPORT_ACTION_UNAVAILABLE'
        stage('FIND_CSV_IN_DOCUMENT_PICKER')
        if not tap_text([fixture.name],3):
            stage('OPEN_DOCUMENT_ROOTS')
            assert tap_text(['Show roots','Open from','打开抽屉','显示根目录','Navigate up','drawer_toggle','Show navigation drawer']),'DOCUMENT_ROOTS_UNAVAILABLE'
            stage('OPEN_DOWNLOADS')
            assert tap_text(['Downloads','下载内容','下载']),'DOWNLOADS_UNAVAILABLE'
            stage('PICK_ARCHIVED_CSV')
            assert tap_text([fixture.name],15),'FIXTURE_NOT_FOUND_IN_PICKER'
        record['documentPickerSelected']=True
    initial=time.monotonic()
    for attempt in range(3):
        try:
            start_or_resume(False)
            record['secondsToSelectCsv']=int(time.monotonic()-initial)
            save()
            break
        except AssertionError as error:
            reason=str(error)
            allowed={'CHART_TOOLS_UNAVAILABLE','IMPORT_ACTION_UNAVAILABLE','DOCUMENT_ROOTS_UNAVAILABLE','DOWNLOADS_UNAVAILABLE','FIXTURE_NOT_FOUND_IN_PICKER','RECOVERY_ACTION_UNAVAILABLE'}
            if reason not in allowed:raise
            record.setdefault('initialPickerFailures',[]).append({'reason':reason,'elapsed':int(time.monotonic()-initial)})
            save()
            if attempt==2:raise
            adb('shell','input','keyevent','KEYCODE_BACK')
            adb('shell','am','start','-W','-n',activity)
            time.sleep(5)
    record['importProgress']=[]
    record['recoveryAttempts']=[]
    save()
    reason_codes=('R31_HISTORY_IMPORT_CLAIM','R31_HISTORY_IMPORT_RENEW','R27_HISTORY_IMPORT_R31','R31_WRITER_HOLD_FAULT_BACKLOG_PENDING_DURABLE_DRAIN','ROLLED_BACK','UNKNOWN_OUTCOME','STALE_OWNER_OR_CURSOR','LOCK_BUSY','SCHEMA_PENDING','ACTIVE_INPUT_CAPACITY','CLAIM_CAS','OWNER_DRAINING','SOURCE_CHANGED_REPREPARE','REPLAY_SOURCE_CHANGED','STORAGE_BUDGET','MEMORY_BUDGET','CP3_DB_BUSY_DEADLINE','BUSY','WRITER','CANCELLED','INTEGRITY','HISTORY_SOURCE','PUBLICATION_SCHEMA','R27_CSV_HEADER','MEMORY','LEASE','CAPACITY','RESOURCE','SQLiteException','IllegalStateException','FileNotFoundException','WriteRejectedR31','CapacityWait','DependencyR31','IOException')
    last_recovery=-120
    started=time.monotonic()
    while time.monotonic()-started<1800:
        elapsed=int(time.monotonic()-started)
        try:
            cols=query("SELECT count(*),coalesce(max(cursor_line),0),coalesce(max(chunk_seq),0),coalesce(max(invalid_rows),0),coalesce(max(imported_rows),0),coalesce(max(match_rows),0),coalesce(max(conflict_rows),0),sum(CASE WHEN status='STAGING' THEN 1 ELSE 0 END),sum(CASE WHEN status='COMPLETE' THEN 1 ELSE 0 END) FROM r31_history_import_run").split('|')
            progress=dict(zip(('jobs','cursorLine','chunks','invalid','imported','match','conflict','staging','complete'),[int(x or 0) for x in cols]))
            progress['rows']=int(query('SELECT count(*) FROM r31_history_import_row'))
            accepted=int(query("SELECT count(*) FROM r31_history_import_row d JOIN r31_history_import_run r ON r.job_id=d.job_id WHERE r.status='COMPLETE' AND d.status='IMPORTED'"))
            record['publishedReplayRows']=accepted
            if accepted==5000:
                record['importProgress'].append(dict(elapsed=elapsed,**progress))
                save();return
            view=hierarchy()
            labels=' '.join(n.attrib.get('text','') for n in view.iter('node') if n.attrib.get('package')==package)
            progress['uiState']=('RECOVERABLE' if '导入需核查或恢复' in labels else 'RUNNING' if '正在比对并导入' in labels else 'UNKNOWN')
            progress['causes']=[token for token in reason_codes if token in labels]
            record['importProgress'].append(dict(elapsed=elapsed,**progress))
            save()
            if progress['uiState']=='RECOVERABLE' and elapsed-last_recovery>=90 and len(record['recoveryAttempts'])<10:
                last_recovery=elapsed
                record['recoveryAttempts'].append({'elapsed':elapsed,'reopenPersisted':progress['staging']>0,'cause':progress['causes']})
                save()
                start_or_resume(progress['staging']>0)
        except (ET.ParseError,ValueError,subprocess.SubprocessError):
            record['incompleteObserverReads']=record.get('incompleteObserverReads',0)+1
            save()
        time.sleep(12)
    raise RuntimeError('FULL_INPUT_PUBLICATION_NOT_OBSERVED')
