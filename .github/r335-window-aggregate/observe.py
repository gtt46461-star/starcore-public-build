"""Observe signed B08 on an Android guest using the release UI and a prospective calendar."""
from pathlib import Path
import hashlib,importlib.util,json,os,re,shutil,sqlite3,subprocess,sys,time
import xml.etree.ElementTree as ET
import zipfile

def module(path,name):
    spec=importlib.util.spec_from_file_location(name,path)
    mod=importlib.util.module_from_spec(spec);spec.loader.exec_module(mod);return mod

base=module(Path('.github/r335-window-aggregate/field.py'),'upgrades')
sys.path.insert(0,str(base.SRC/'tools/r3251'))
dbprobe=module(base.SRC/'tools/r3251/verify_release_overlay.py','actual032033device')
EVID=base.EVID
RAW=base.NATIVE/'window'
PLAN=base.TEMP/'material/STARCORE_R33_5_20261118'
REVIEW=base.TEMP/'material/current-review'
TOOLS=base.TEMP/'tools'
B07,B08=base.STAGES[-2:]
PKG=base.PKG
DB=dbprobe.DB
def measure(kind,**fields):
    with (EVID/'observations.jsonl').open('a') as f:
        print(json.dumps(dict(kind=kind,wallUtc=int(time.time()),**fields)),file=f)
    print(kind,flush=True)

def execfile(argv,output):
    with output.open('wb') as raw:
        p=subprocess.run(list(map(str,argv)),stdout=raw,stderr=subprocess.STDOUT,timeout=600)
    return p.returncode

def get_apk(stage,version,commit):
    url='https://github.com/'+base.REPO+'/releases/download/r335-'+stage+'-'+commit[:12]+'/'
    name='STARCORE-R33-'+str(version)+'-'+commit[:12]+'-release.apk'
    sums=base.TEMP/(stage+'-SUMS')
    base.download(url+'SHA256SUMS.txt',sums,wait=True)
    received=base.TEMP/(stage+'.apk')
    base.download(url+name,received)
    expected=next(d for d,n in [l.split('  ',1) for l in sums.read_text().splitlines()] if n==name)
    assert base.fingerprint(received)==expected
    signer=base.cmd(os.environ['ANDROID_HOME']+'/build-tools/35.0.0/apksigner','verify','--print-certs',received).decode()
    assert 'Signer #1 certificate SHA-256 digest: '+base.CERT in signer
    return received

def tap_start():
    base.adb('shell','am','start','-W','-n',base.ACTIVITY)
    for attempt in range(10):
        time.sleep(3)
        result=subprocess.run(['adb','shell','uiautomator','dump','/sdcard/r335-window-ui.xml'],capture_output=True,timeout=40)
        if result.returncode:continue
        ui=base.adb('shell','cat','/sdcard/r335-window-ui.xml')
        (RAW/'last-start-ui.xml').write_bytes(ui)
        try:tree=ET.fromstring(ui)
        except ET.ParseError:continue
        for node in tree.iter('node'):
            if node.get('resource-id','')!=PKG+':id/btn_start' or node.get('enabled')=='false':continue
            bounds=list(map(int,re.findall(r'\d+',node.get('bounds',''))))
            if len(bounds)==4 and bounds[2]>bounds[0] and bounds[3]>bounds[1]:
                base.adb('shell','input','tap',str((bounds[0]+bounds[2])//2),str((bounds[1]+bounds[3])//2))
                measure('production-service-start-button',resourceId=node.get('resource-id'),attempt=attempt+1)
                return True
        if attempt in (3,6):
            base.adb('shell','input','swipe','525','1600','525','420','310')
    return False

def get_anchor():
    sql='SELECT issue,open_time,first_seen_at FROM draws WHERE dirty=0 AND open_time>0 ORDER BY open_time DESC LIMIT 1;'
    for attempt in range(6):
        time.sleep(25)
        proc=subprocess.run(['adb','shell','sqlite3','-readonly','/data/user/0/'+PKG+'/databases/'+DB,'"'+sql+'"'],capture_output=True)
        raw=proc.stdout
        if proc.returncode or b"not found" in proc.stdout+proc.stderr:
            # Some system images omit sqlite3. Take a read-only stable copy; keep the service running.
            import tempfile
            location='/data/user/0/'+PKG+'/databases/'+DB
            def hashes():
                observed={}
                for suffix in ('','-wal'):
                    q=subprocess.run(['adb','shell','sha256sum',location+suffix],capture_output=True)
                    if q.returncode==0:observed[suffix]=q.stdout.split()[0].decode()
                return observed
            with tempfile.TemporaryDirectory() as storage:
                f=Path(storage)/DB;before=hashes()
                if '' in before:
                    for suffix,digest in before.items():
                        target=Path(str(f)+suffix)
                        with target.open('wb') as stream:
                            q=subprocess.run(['adb','exec-out','cat',location+suffix],stdout=stream,stderr=subprocess.PIPE)
                        if q.returncode or base.fingerprint(target)!=digest:break
                    else:
                        if before==hashes():
                            try:
                                with sqlite3.connect(f.as_uri()+'?mode=ro',uri=True) as db:
                                    row=db.execute(sql).fetchone()
                                    if row and db.execute('PRAGMA quick_check').fetchone()[0]=='ok':
                                        raw=('|'.join(map(str,row))+'\n').encode()
                                        measure('schedule-anchor-read',mode='read-only-stable-DB-and-WAL',componentSha256=before)
                            except sqlite3.Error as err:
                                (RAW/f'schedule-copy-{attempt}.txt').write_text(str(err)+'\n')
        (RAW/f'schedule-query-{attempt}.txt').write_bytes(raw+proc.stderr)
        (RAW/'service-before-window.txt').write_bytes(base.adb('shell','dumpsys','activity','services',PKG))
        match=re.fullmatch(rb'([0-9]{6,24})\|([0-9]{13})\|([0-9]{13})\s*',raw)
        if match:
            wall=int(base.adb('shell','date','+%s%3N'))
            if 0<=wall-int(match[2])<=180000:
                (RAW/'schedule.bin').write_bytes(raw)
                return match[1].decode(),int(match[2])
    return None

def assignment_readiness():
    base.adb('unroot');base.adb('wait-for-device')
    try:
        assert base.adb('shell','id','-u').strip()==b'2000'
        reply=subprocess.run(['adb','exec-out','content','read','--uri','content://'+PKG+'.r335.enabled/assignment'],capture_output=True,timeout=45)
        try:
            doc=ET.fromstring(reply.stdout)
            return 'READY' if doc.tag=='map' and doc.find("boolean[@name='user_enabled']") is not None else 'PROJECTION_INCOMPLETE'
        except ET.ParseError:
            for code in ('R335_ASSIGNMENT_UNREADABLE','R335_ASSIGNMENT_NOT_STABLE','R335_ASSIGNMENT_WRITE_IN_PROGRESS','R335_ASSIGNMENT_READ_ONLY_SHELL','R335_ASSIGNMENT_ACCESS','SecurityException','Permission Denial','Unknown URI','FileNotFoundException','No content provider','DeadObjectException'):
                if code.encode() in reply.stdout+reply.stderr:return code
            return 'UNRECOGNIZED_PROVIDER_RESPONSE'
    finally:
        base.adb('root');base.adb('wait-for-device')

def live_window(apk):
    commit=B08[2];digest=base.fingerprint(apk)
    for i in range(3):
        status=assignment_readiness()
        measure('assignment-readiness',attempt=i+1,category=status)
        if status=='READY':break
        time.sleep(12)
    anchor=get_anchor()
    if not anchor:
        measure('calendar-unavailable',reason='No observed clean current draw within three minutes of device time; retained original reads.')
        return False
    cal=RAW/'calendar.json';reg=int(base.adb('shell','date','+%s%3N'))
    command=[sys.executable,'-B',PLAN/'reference/tools/r31/collect_device_receipts.py','calendar','--anchor-issue',anchor[0],
        '--anchor-open-ms',anchor[1],'--count',100,'--registered-at-ms',reg,'--source-commit',commit,'--apk',apk,
        '--schedule-evidence',RAW/'schedule.bin','--out',cal]
    if execfile(command,RAW/'create-calendar.log'):
        measure('calendar-refused');return False
    tool=TOOLS/'B02/enabled_window_r335/collect_enabled_window.py'
    def capture(phase):
        # The official read-only provider authenticates the Android shell UID. A root caller must be rejected.
        base.adb('unroot');base.adb('wait-for-device')
        assert base.adb('shell','id','-u').strip()==b'2000'
        try:
            return execfile([sys.executable,'-B',tool,'capture','--capture-source','APP_OWNED','--phase',phase,'--calendar',cal,
                '--source-commit',commit,'--apk',apk,'--out',RAW/('enable-'+phase.lower()+'.json')],RAW/(phase+'.log'))
        finally:
            base.adb('root');base.adb('wait-for-device')
    start_status=capture('START')
    measure('official-start-result',exit=start_status)
    if start_status:measure('strict-start-unavailable',category=assignment_readiness())
    # Observe the complete prospective calendar, with its strict START outcome preserved.

    data=json.loads(cal.read_bytes())
    measure('prospective-emulator-start',periods=len(data['entries']),calendarSha256=base.fingerprint(cal))
    base.adb('shell','input','keyevent','KEYCODE_HOME')
    while int(base.adb('shell','date','+%s%3N')) < data['windowEndMs']+2000:
        time.sleep(45)
        with (RAW/'foreground-service-observations.txt').open('ab') as log:
            log.write(base.adb('shell','date','+%s%3N'))
            log.write(base.adb('shell','dumpsys','activity','services',PKG))
    code=capture('END')
    measure('official-end',exit=code)
    return code==0 and start_status==0

def actual_export_and_snapshot(apk):
    (EVID/'B08').mkdir(exist_ok=True)
    base.safe_reports('B08',1,B08[2])
    reports=base.NATIVE/'B08/1'
    original=reports/'UI_EXPORTED_FULL_REPORT.zip'
    if not original.exists():
        measure('export-missing');return
    with zipfile.ZipFile(original) as z:
        if 'source-manifest.json' not in z.namelist():return
        manifest=z.read('source-manifest.json')
        (RAW/'source-manifest.json').write_bytes(manifest)
        doc=json.loads(manifest)
        if doc.get('sourceCommit')!=B08[2] or doc.get('apkSha256')!=base.fingerprint(apk):
            measure('export-source-binding-incomplete');return
        name=next((n for n in ['snapshot.db','snapshot.sqlite'] if n in z.namelist()),None)
        if name:
            with z.open(name) as source,(RAW/'snapshot.sqlite').open('wb') as target:shutil.copyfileobj(source,target,1<<20)
    if not (RAW/'snapshot.sqlite').exists():
        jobs=base.adb('shell',f'find {base.APP_ROOT}/jobs -name snapshot.db -type f 2>/dev/null || true').decode().splitlines()
        for remote in jobs:
            if not re.fullmatch(re.escape(base.APP_ROOT)+'/jobs/[a-f0-9]{64}/snapshot.db',remote):continue
            if base.adb('shell','sha256sum',remote).decode().split()[0]==doc.get('snapshotSha256'):
                base.adb('pull',remote, str(RAW/'snapshot.sqlite'),timeout=420);break
    if not (RAW/'snapshot.sqlite').exists() or base.fingerprint(RAW/'snapshot.sqlite')!=doc.get('snapshotSha256'):
        measure('report-snapshot-unavailable');return
    if not all((RAW/f).is_file() for f in ['calendar.json','schedule.bin']):return
    binding=[sys.executable,'-B',PLAN/'verification/mainline/bind_snapshot.py','--root',RAW,'--scope','INTEGRATED','--database','snapshot.sqlite',
        '--source-manifest','source-manifest.json','--calendar','calendar.json','--schedule-evidence','schedule.bin','--out','trace.json']
    if execfile(binding,RAW/'bind.log'):return
    command=[sys.executable,'-B',REVIEW/'runtime/check_window.py','--tools',TOOLS,'--bundle',RAW/'trace.json','--evidence',RAW,'--start',RAW/'enable-start.json',
        '--end',RAW/'enable-end.json','--commit',B08[2],'--apk',base.fingerprint(apk)]
    measure('hundred-window-verifier',exit=execfile(command,RAW/'window-result.json'))

def main():
    EVID.mkdir();RAW.mkdir(parents=True)
    a,b=[get_apk(*s) for s in (B07,B08)]
    base.adb('root');base.adb('wait-for-device')
    assert base.adb('shell','getprop','ro.kernel.qemu').strip()==b'1'
    assert b'Success' in base.adb('install',str(a))
    base.adb('shell','pm','grant',PKG,'android.permission.POST_NOTIFICATIONS')
    base.adb('shell','am','start','-W','-n',base.ACTIVITY)
    time.sleep(75)
    before=dbprobe.snapshot(base.TEMP/'old-database')
    before_facts,before_rows=dbprobe.inspect(base.TEMP/'old-database'/DB)
    uid,oldsha=base.installed(EVID/'B08-before-upgrade.txt',B07[1])
    assert oldsha==base.fingerprint(a)
    assert b'Success' in base.adb('install','-r',str(b))
    after=dbprobe.snapshot(base.TEMP/'updated-before-first-open')
    uid2,newsha=base.installed(EVID/'B08-after-upgrade.txt',B08[1])
    measure('overlay-before-first-open',oldPackSha=oldsha,newPackSha=newsha,sameUid=uid==uid2,databaseBefore=before,
        databaseAfterReplace=after,bytesIdentical=before==after)
    assert uid==uid2 and newsha==base.fingerprint(b)
    start=base.adb('shell','am','start','-W','-n',base.ACTIVITY);(EVID/'B08-cold-start.txt').write_bytes(start)
    time.sleep(40)
    observed=dbprobe.snapshot(base.TEMP/'updated-after-first-open')
    facts,rows=dbprobe.inspect(base.TEMP/'updated-after-first-open'/DB)
    matches={k:dict(older=v['count'],newer=rows[k]['count'],retained=len(v['rows']&rows[k]['rows'])) for k,v in before_rows.items() if k in rows}
    measure('overlay-readback',database=observed,integrity=facts.get('integrity','ok'),protectedRows=matches)
    down=subprocess.run(['adb','install','-r',str(a)],capture_output=True)
    (EVID/'B08-downgrade-refusal.txt').write_bytes(down.stdout+down.stderr)
    assert down.returncode and b'VERSION_DOWNGRADE' in down.stdout+down.stderr
    base.installed(EVID/'B08-after-rejected-downgrade.txt',B08[1])
    started=tap_start()
    if started:
        try:live_window(b)
        except Exception as err:measure('window-error',error=type(err).__name__)
    else:measure('start-button-unavailable')
    actual_export_and_snapshot(b)

if __name__=='__main__':main()
