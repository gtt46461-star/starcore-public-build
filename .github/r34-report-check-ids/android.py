import hashlib
import json
from pathlib import Path
import re
import os
import tempfile
import shutil
import sqlite3
import subprocess
import sys
import time
import xml.etree.ElementTree as ET
import zipfile

OUT=Path(os.environ['RUNNER_TEMP'])/'test-documents'
OUT.mkdir(exist_ok=True)
PACKAGE='com.starcore.research.r13'
DEVICE=subprocess.check_output(['adb','get-serialno'],text=True).strip()
ACTIVITY=PACKAGE+'/com.ethk3.novaonepro.MainActivity'
REPORT='导出完整报告'
CREATE='生成当前完整报告'
SHARE='完整检测报告交付'


def adb(*options,timeout=45,check=True):
    p=subprocess.run(['adb','-s',DEVICE,*options],capture_output=True,text=True,timeout=timeout)
    if check and p.returncode:
        raise RuntimeError(p.stderr.strip() or p.stdout.strip())
    return p.stdout


def ui():
    for _ in range(5):
        try:
            dumped=adb('shell','uiautomator','dump','/sdcard/starcore-final-ui.xml',timeout=25)
            if 'dumped to' in dumped:
                xml=adb('shell','cat','/sdcard/starcore-final-ui.xml')
                return xml,ET.fromstring(xml)
        except (RuntimeError,subprocess.TimeoutExpired,ET.ParseError):
            pass
        time.sleep(2)
    raise RuntimeError('ANDROID_UI_TREE_NOT_READY')


def button(tree,text):
    for node in tree.iter('node'):
        label=node.get('text','') or node.get('content-desc','')
        p=re.fullmatch(r'\[(\d+),(\d+)\]\[(\d+),(\d+)\]',node.get('bounds',''))
        if p and text.casefold() in label.casefold():
            x,y,right,bottom=map(int,p.groups())
            if right>x and bottom>y:
                return (x+right)//2,(y+bottom)//2
    return None


def tap(tree,text):
    exact=next((n for n in tree.iter('node') if n.get('clickable')=='true' and n.get('enabled')=='true' and n.get('text','').casefold()==text.casefold()),None)
    if exact is not None:
        left,top,right,bottom=map(int,re.findall('[0-9]+',exact.get('bounds')))
        xy=(left+right)//2,(top+bottom)//2
    else:xy=button(tree,text)
    if not xy:
        return False
    adb('shell','input','tap',*(str(v) for v in xy))
    return True


def report_entry(action=CREATE):
    valid_actions={CREATE,'另存完整原始备份（体积大）','取回上次报告或继续生成'}
    assert action in valid_actions
    adb('shell','am','start','-W','--activity-clear-top','-n',ACTIVITY,timeout=100)
    for attempt in range(45):
        try:
            xml,tree=ui()
        except (RuntimeError,subprocess.TimeoutExpired,ET.ParseError):
            time.sleep(3)
            continue
        (OUT/('entry-'+str(len(out['reportReadbacks'])+1)+'-last.xml')).write_text(xml)
        nodes=list(tree.iter('node'))
        menu=any(n.get('text')=='完整检测报告' for n in nodes)
        if menu and any(n.get('text')==action for n in nodes):
            (OUT/('entry-'+str(len(out['reportReadbacks'])+1)+'-menu.xml')).write_text(xml)
            if tap(tree,action):
                return
        # Scrolls recycle screen coordinates. Exercise the visible enabled production button.
        entry=next((n for n in nodes if n.get('class')=='android.widget.Button' and n.get('clickable')=='true' and n.get('enabled')=='true' and n.get('text','').startswith('导出完整报告')),None)
        if entry is not None:
            l,t,r,b=map(int,re.findall('[0-9]+',entry.get('bounds')))
            if b-t>=24:
                adb('shell','input','tap',str((l+r)//2),str((t+b)//2))
                time.sleep(2)
                continue
        if any(n.get('package')=='com.android.documentsui' for n in nodes):
            raise RuntimeError('REPORT_ENTRY_PREVIOUS_PICKER')
        if not menu:
            adb('shell','for n in 1 2 3; do input swipe 230 730 230 110 180; done')
        time.sleep(1)
    raise RuntimeError('REPORT_ENTRY_NOT_FOUND')


def wait_for_report(tag):
    until=time.monotonic()+1200
    previous=OUT/(tag+'-progress.json')
    states=json.loads(previous.read_text()) if previous.is_file() else []
    while time.monotonic()<until:
        time.sleep(6)
        try:
            xml,tree=ui()
        except (RuntimeError,subprocess.TimeoutExpired,ET.ParseError):
            continue
        hits=[e.get('text','') for e in tree.iter('node') if any(t in e.get('text','') for t in ('报告','PAUSED','FAILED'))]
        if hits and (not states or hits!=states[-1]):
            states.append(hits)
        (OUT/(tag+'-progress.json')).write_text(json.dumps(states,ensure_ascii=False,indent=2)+'\n')
        if button(tree,SHARE):
            (OUT/(tag+'-ready.xml')).write_text(xml)
            return tree
        if any(('导出失败' in h and '查看上次导出失败详情' not in h) or '检测报告状态=FAILED' in h or '完整报告未交付' in h or h.startswith('完整报告失败') for h in hits):
            break
    raise RuntimeError('REPORT_NOT_READY_ON_EMULATOR')


def check(value,code):
    if not value: raise ValueError(code)


def digest(data):
    return hashlib.sha256(data).hexdigest()


def training_objects(archive,snapshot):
    expected={}
    with sqlite3.connect(snapshot.as_uri()+'?mode=ro&immutable=1',uri=True) as db:
        tables={name for name, in db.execute("SELECT name FROM sqlite_master WHERE type='table'")}
        for table,column,prefix,suffix in [('research_training_input_v2','input_hash','training-input-','.scdev'),('research_training_attachment_cp12','attachment_hash','training-bound-','.scbind')]:
            if table not in tables:continue
            for sha,rel,size in db.execute(f'SELECT {column},relative_path,byte_count FROM {table}'):
                check(re.fullmatch('[0-9a-f]{64}',sha) and rel==sha+suffix and 0<size<=8*1024**2,'TRAINING_ROW_IDENTITY')
                name=prefix+rel
                check(name not in expected,'TRAINING_ROW_IDENTITY')
                expected[name]=(sha,size)
    rows=[json.loads(line) for line in archive.read('attachments.jsonl').splitlines() if line.strip()]
    observed={}
    for row in rows:
        if row.get('sourceKind')!='TRAINING':continue
        name=row['logicalName']
        check(name not in observed and name in expected and row.get('state')=='FIXED_VERIFIED','TRAINING_DESCRIPTOR')
        with archive.open(name) as body:sha=hashlib.file_digest(body,'sha256').hexdigest()
        observed[name]=(sha,archive.getinfo(name).file_size)
        check(observed[name]==(row['sha256'],row['bytes']),'TRAINING_BODY_IDENTITY')
    check(expected==observed,'TRAINING_DB_CLOSURE')
    bound=digest(''.join(name+' '+sha+' '+str(size)+chr(10) for name,(sha,size) in sorted(observed.items())).encode())
    return dict(trainingRowsReopened=True,trainingMembers=len(observed),trainingBytes=sum(v[1] for v in observed.values()),trainingSetSha256=bound)


def raw_readback(archive,semantics):
    m=json.loads(archive.read('source-manifest.json'))
    if not m.get('rawEvidenceIncluded'):
        assert 'snapshot.db' not in archive.namelist() and semantics['rawEvidence'] is False
        return {'profile':'COMPACT_DIAGNOSTIC','rawEvidenceIncluded':False}
    assert semantics['rawEvidence'] is True and m['r26CheckpointBodiesIncluded']
    assert m['r26CheckpointByteValidation']=='EACH_INCLUDED_BODY_SHA256_VERIFIED'
    bodies={}
    for name in archive.namelist():
        match=re.fullmatch(r'r26-checkpoint-([0-9a-f]{64})[.]bin',Path(name).name)
        if match:
            with archive.open(name) as member: rawhash=hashlib.file_digest(member,'sha256').hexdigest()
            assert rawhash==match[1] and rawhash not in bodies
            bodies[rawhash]=archive.getinfo(name).file_size
    assert (len(bodies),sum(bodies.values()))==(m['r26CheckpointObjects'],m['r26CheckpointBytes'])
    with tempfile.TemporaryDirectory(dir='/tmp') as scratch:
        db=Path(scratch)/'snapshot.db'
        with db.open('wb') as out,archive.open('snapshot.db') as inp:shutil.copyfileobj(inp,out)
        with db.open('rb') as f:h=hashlib.file_digest(f,'sha256').hexdigest()
        assert h==semantics['snapshotSha256']==m['snapshotSha256'] and db.stat().st_size==m['snapshotBytes']
        with sqlite3.connect(db.as_uri()+'?mode=ro&immutable=1',uri=True) as conn:
            assert conn.execute('pragma quick_check').fetchall()==[('ok',)]
            tables=conn.execute("select count(*) from sqlite_master where type='table'").fetchone()[0]
        training=training_objects(archive,db)
    return dict(profile='FULL_RAW',rawEvidenceIncluded=True,snapshotQuickCheck='ok',snapshotTables=tables,
                snapshotBytes=m['snapshotBytes'],checkpointObjects=len(bodies),checkpointBytes=sum(bodies.values()),**training)


def save_report(tree,tag,picker_open=False):
    if not picker_open and not tap(tree,'一次保存完整ZIP文件'):
        raise RuntimeError('SINGLE_ZIP_SAVE_OPTION_MISSING')
    file_base='starcore121r'+tag.rsplit('-',1)[-1]+'t'+str(int(time.time()))
    roots=['/sdcard/Download','/sdcard/Documents']
    def files():
        found=adb('shell','find',*roots,'-type','f',check=False)
        return {f for f in found.splitlines() if f.startswith('/sdcard/')}
    existing=files()
    picker=None
    typed=None
    for attempt in range(8):
        xml,picker=ui()
        (OUT/(tag+'-picker.xml')).write_text(xml)
        edit=next((e for e in picker.iter('node') if e.get('class')=='android.widget.EditText'),None)
        if edit is None:
            time.sleep(2)
            continue
        xy=list(map(int,re.findall('[0-9]+',edit.get('bounds'))))
        adb('shell','input','tap',str((xy[0]+xy[2])//2),str((xy[1]+xy[3])//2))
        adb('shell','input','keycombination','KEYCODE_CTRL_LEFT','KEYCODE_A')
        adb('shell','input','text',file_base+'.zip')
        for _ in range(3):
            xml,picker=ui()
            (OUT/(tag+'-picker-after-edit.xml')).write_text(xml)
            field=next((e for e in picker.iter('node') if e.get('class')=='android.widget.EditText'),None)
            typed=field.get('text','') if field is not None else ''
            if file_base in typed:break
            time.sleep(1)
        if file_base in (typed or ''):break
        # DocumentsUI can keep the automatically suggested filename. Let it create the
        # new document when that name is free. Never authorize replacement.
        if typed and not any(Path(n).name==typed for n in existing):break
    if not typed:raise RuntimeError('SAF_FILENAME_FIELD_MISSING')
    if file_base not in typed and any(Path(n).name==typed for n in existing):
        raise RuntimeError('SAF_NONDESTRUCTIVE_NEW_NAME_REQUIRED')
    (OUT/(tag+'-saf-new-name.json')).write_text(json.dumps({'before':sorted(n for n in existing if 'report' in n.lower()),'suggestedOrTyped':typed,'uniqueMarker':file_base}))
    if not (tap(picker,'保存') or tap(picker,'Save')):raise RuntimeError('SAF_SAVE_BUTTON_MISSING')
    for _ in range(360):
        candidates=sorted(f for f in files()-existing if file_base in Path(f).name or Path(f).name==typed or Path(f).name.startswith(typed.rsplit('.',1)[0]))
        if candidates:
            result=OUT/(tag+'.zip')
            adb('pull',candidates[0],str(result),timeout=90)
            try:
                with zipfile.ZipFile(result) as z:
                    bad=z.testzip()
                    if bad is None:
                        summary=json.loads(z.read('R30_REPORT.json'))
                        matrices=json.loads(z.read('STRATEGY_MATRICES.json'))
                        semantics=json.loads(z.read('R31_SEMANTIC_RECEIPT.json'))
                        assert z.testzip() is None, 'ARCHIVE_CRC_ERROR'
                        assert len(semantics['checks'])==96, 'CHECK_CATALOG_COUNT'
                        assert len(matrices.get('r930TypedSnapshotsR34',[]))==19, 'R930_KIND_COUNT'
                        assert summary['chapters'][11]['fieldFollowupR34']['sourceCommit']==commit
                        assert semantics['sourceCommit']==commit and semantics['chapters']==12 and semantics['metrics']==44
                        assert hashlib.sha256(z.read('R30_REPORT.json')).hexdigest()==semantics['reportSha256']
                        assert all(r.get('jobId')!=semantics['jobId'] for r in out['reportReadbacks']), 'REPEATED_REPORT_JOB'
                        with result.open('rb') as f: sha=hashlib.file_digest(f,'sha256').hexdigest()
                        details=raw_readback(z,semantics)
                        if out.get('activeProfile')=='FULL_RAW':assert details['rawEvidenceIncluded'], 'FULL_RAW_REQUEST_HAS_NO_SNAPSHOT'
                        return dict(status='VERIFIED_ANDROID_SAF_ZIP',**details,file=result.name,bytes=result.stat().st_size,
                                    sha256=sha,matrixKinds=19,members=len(z.namelist()),jobId=semantics['jobId'],
                                    snapshotSha256=semantics['snapshotSha256'],chapters=12,metrics=44,checks=len(semantics['checks']))
            except (zipfile.BadZipFile,KeyError,json.JSONDecodeError):
                pass
        time.sleep(5)
    raise RuntimeError('SAF_REPORT_READBACK_NOT_COMPLETED')


import importlib.util
from collections import Counter
T=Path(os.environ['RUNNER_TEMP']);src=Path(os.environ['GITHUB_WORKSPACE'])
pin={120:('284c4a379c2348ebdba0eaefa2c3a661df686238','2af9014b9fe883081a7464a89f2b883761c452f57fba82566b8e44f18d3ecfd7'),121:('db60f716c3bca8ced4a873aa94b16123307b683f','52fac634b22f42da9bea4dd1d21dbe00eaa24d81698fad5008879cba6b605c97')}
spec=importlib.util.spec_from_file_location('original_oracle',T/'restored/STARCORE_R33_5_20261118/B01/scripts/verify_export_artifacts_r334.py');oracle=importlib.util.module_from_spec(spec);spec.loader.exec_module(oracle)
assert hashlib.sha256(Path(oracle.__file__).read_bytes()).hexdigest()=='25f75664f00b69693416bcc3766d7a1434ee01ba7816bb40eb3fb52932c2e034'
receipt={'sourceCommit':pin[121][0],'apkSha256':pin[121][1],'runId':int(os.environ['GITHUB_RUN_ID']),'api':34,'physical':False,'scope':'DISPOSABLE_ANDROID_API34_SIGNED_UPGRADE_AND_DOCUMENTS_PROVIDER_READBACK','reports':[]}
def persist():
    name=T/('report-'+os.environ['GITHUB_RUN_ID']+'.json')
    data=json.dumps(receipt,indent=2)
    assert len(data)<120000
    name.write_text(data)
def installed(v):
    package=adb('shell','pm','path',PACKAGE).strip().split(':',1)[1]
    assert adb('shell','sha256sum',package).split()[0]==pin[v][1]
    di=adb('shell','dumpsys','package',PACKAGE)
    assert f'versionCode={577311000+v} ' in di
    uid=re.search(r'\b(?:userId|appId)=(\d+)',di)
    assert uid
    return int(uid[1])
def quiescent():
    prefix='/data/user/0/'+PACKAGE+'/databases/binance_k3_nova_one_pro_v57.db'
    facts={}
    for suffix in ('','-wal','-shm'):
        sha=adb('shell','sha256sum',prefix+suffix,timeout=180,check=False).split()
        size=adb('shell','stat','-c','%s',prefix+suffix,check=False).strip()
        facts[suffix or 'db']=dict(present=True,sha256=sha[0],bytes=int(size)) if sha and re.fullmatch('[0-9a-f]{64}',sha[0]) and size.isdigit() else {'present':False}
    return facts
def report(v,i,profile):
    global commit,out
    commit=pin[v][0]
    out={'activeProfile':profile,'reportReadbacks':[]}
    report_entry(CREATE if profile=='COMPACT_DIAGNOSTIC' else '另存完整原始备份（体积大）')
    row=save_report(wait_for_report('v'+str(v)+'-'+str(i)),'v'+str(v)+'-'+str(i))
    fn=OUT/row['file']
    actual='DIAGNOSTIC' if profile=='COMPACT_DIAGNOSTIC' else 'FULL_RAW'
    with zipfile.ZipFile(fn) as z:
        sem=json.loads(z.read('R31_SEMANTIC_RECEIPT.json'))
        fixed={'OBSERVED','N/A','RETEST_REQUIRED','FAIL','NOT_PROVEN','PASS','WARN','NO_SAMPLE','AVAILABLE','READ_ERROR','READ_OK','NO_MEASURED_DATA','WAIT_CALIBRATION_INPUT','BLOCKED_UPSTREAM','NO_MEASURED_ABLATION','WATCH','HARD_BLOCK','REAL_LOCKED_SHADOW_ONLY','RESEARCH_SCHEMA_NOT_READY'}
        details={hashlib.sha256(k.encode()).hexdigest():e.get('domain_outcome') if e.get('domain_outcome') in fixed else 'OTHER_TYPED_OUTCOME' for k,e in sem['checks'].items()}
        states=dict(Counter(v if v in fixed else 'OTHER_TYPED_OUTCOME' for v in (e.get('domain_outcome') for e in sem['checks'].values())))
        assert sum(states.values())==96, 'INCOMPLETE_CHECK_DENOMINATOR'
        assert sem['sourceCommit']==pin[v][0]
    evaluate=dict(profile=actual,jobId=row['jobId'],sourceCommit=pin[v][0],apkSha256=pin[v][1])
    try:strict=oracle.validate_export(fn,evaluate)['status']
    except ValueError as e:strict=str(e) if re.fullmatch('[A-Z_]{1,90}',str(e)) else 'ORIGINAL_VALIDATOR_REJECTED'
    keys=('jobId','bytes','sha256','snapshotSha256','chapters','metrics','checks','members','matrixKinds','trainingMembers','trainingBytes','rawEvidenceIncluded')
    data={k:row[k] for k in keys if k in row}
    for k in ('jobId','sha256','snapshotSha256'):assert re.fullmatch('[0-9a-f]{64}',data[k])
    assert not any(x['jobId']==data['jobId'] for x in receipt['reports'])
    receipt['reports'].append(dict(**data,version=577311000+v,profile=actual,originalValidator=strict,systemPickerReopened=True,checksByDomain=states,registeredCheckOutcomes=details))
    persist()
phase='BOOT'
try:
    assert DEVICE.startswith('emulator-') and adb('shell','getprop','ro.kernel.qemu').strip()=='1'
    adb('root');adb('wait-for-device');assert adb('shell','getprop','ro.build.version.sdk').strip()=='34'
    assert not adb('shell','pm','list','packages',PACKAGE).strip()
    adb('shell','wm','size','480x850');adb('shell','wm','density','195')
    adb('shell','settings','put','system','screen_off_timeout','7200000');adb('shell','svc','power','stayon','true')
    for v in (120,121):
        phase='INSTALL_'+str(v);file=T/('v'+str(v)+'.apk')
        assert hashlib.sha256(file.read_bytes()).hexdigest()==pin[v][1]
        if v==121:
            adb('shell','am','force-stop',PACKAGE)
            receipt['before']=quiescent();persist()
        assert 'Success' in adb('install','-r',str(file),timeout=300)
        uid=installed(v);receipt['uid'+str(v)]=uid
        if v==121:
            receipt['after']=quiescent();receipt['sameUid']=receipt['uid120']==uid;persist()
        adb('shell','pm','grant',PACKAGE,'android.permission.POST_NOTIFICATIONS')
        adb('shell','am','start','-W','-n',ACTIVITY);time.sleep(20)
        if v==120:phase='BEFORE_REPORT';report(v,1,'COMPACT_DIAGNOSTIC')
        else:
            for i,kind in enumerate(['COMPACT_DIAGNOSTIC','FULL_RAW','COMPACT_DIAGNOSTIC'],1):
                phase='FINAL_REPORT_'+str(i);report(v,i,kind)
    receipt['completed']=True
except Exception as exc:
    import traceback
    receipt['completed']=False;receipt['phase']=phase;receipt['errorClass']=type(exc).__name__
    receipt['driverLines']=[f.lineno for f in traceback.extract_tb(exc.__traceback__) if Path(f.filename).name=='android.py'][-5:]
    if re.fullmatch('[A-Z0-9_]{1,100}',str(exc)):receipt['reason']=str(exc)
    raise
finally:persist()
