"""Open a new report in the installed app and export through the system picker on a fresh QA upgrade."""
import importlib.util,json,os,subprocess,time,re,traceback
from pathlib import Path
w=Path(os.environ['GITHUB_WORKSPACE'])
tmp=Path(os.environ['RUNNER_TEMP'])
c='273641e838e0b8dd842b42a67a674e184942a30a'
spec=importlib.util.spec_from_file_location('readback',w/'.github/r335-vivo-final-export/driver.py')
n=importlib.util.module_from_spec(spec);spec.loader.exec_module(n)
f=n.field;n.source=c
out=tmp/('R335-095-report-'+os.environ['GITHUB_RUN_ID']+'.json')
record={'sourceCommit':c,'runId':int(os.environ['GITHUB_RUN_ID']),'scope':'NORMAL_APP_UI_ON_UPGRADED_ANDROID_14','attempts':[]}
def save():out.write_text(json.dumps(record,ensure_ascii=False,indent=2)+'\n')
def fresh_hierarchy():
    import xml.etree.ElementTree as ET
    for _ in range(8):
        path='/data/local/tmp/report-view-'+str(time.monotonic_ns())+'.xml'
        try:
            subprocess.run(['adb','shell','uiautomator','dump',path],capture_output=True,timeout=22)
            read=subprocess.run(['adb','exec-out','cat',path],capture_output=True,timeout=12)
            if read.returncode==0 and read.stdout.lstrip().startswith(b'<?xml'):
                return ET.fromstring(read.stdout)
        except (ET.ParseError,subprocess.TimeoutExpired):pass
        finally:subprocess.run(['adb','shell','rm','-f',path],capture_output=True,timeout=12)
        time.sleep(.5)
    raise RuntimeError('NO_FRESH_WINDOW_HIERARCHY')
n.ui.hierarchy=fresh_hierarchy
def request(seq,known):
    base=f.NATIVE/'B08'/str(seq);base.mkdir(parents=True,exist_ok=True)
    row={'attempt':seq,'stages':[],'id':None,'phase':'OPEN_UI'}
    record['attempts'].append(row);save()
    f.adb('shell','am','start','-W','-n',f.ACTIVITY)
    n.report_dialog('另存完整原始备份（体积大）')
    before=n.ui.acknowledgements()
    menu='另存完整原始备份（体积大）'
    for index in range(3):
        if not n.tap(lambda node: node.get('text')==menu and node.get('enabled')=='true',20):
            raise RuntimeError('RAW_MENU_NOT_CLICKED')
        until=time.monotonic()+12
        while time.monotonic()<until:
            if menu not in n.labels() or n.ui.acknowledgements()-before:break
            time.sleep(.6)
        else:continue
        break
    else:raise RuntimeError('RAW_MENU_DID_NOT_DISMISS')
    deadline=time.monotonic()+1200
    row['phase']='WAIT_PUBLICATION';save()
    while time.monotonic()<deadline:
        for path in sorted(n.ui.acknowledgements()-before):
            try:j=n.ui.read_json(path)
            except (ValueError,RuntimeError):continue
            job=j.get('jobId');stage=j.get('stage')
            if j.get('mode')!='FULL_REPORT' or not isinstance(job,str) or not re.fullmatch('[0-9a-f]{64}',job):continue
            if row['id'] is None:row['id']=job
            if job!=row['id']:continue
            reason=j.get('reason','')
            code='|'.join(s for s in reason.split('|') if re.fullmatch('[A-Z][A-Z0-9_]{1,80}',s))
            sig=[stage,code,j.get('resumeStage'),j.get('revision')]
            if not row['stages'] or row['stages'][-1]!=sig:
                row['stages'].append(sig);save()
                if stage=='PAUSED':
                    trace=j.get('failureDetailR32','')
                    row['lastFailureFrames']=list(dict.fromkeys(re.findall(r'[(]([A-Za-z][A-Za-z_0-9]+[.]java:[0-9]+)[)]',trace)))[:16]
                    row['lastFailurePhase']=j.get('failurePhaseR32','')
                    save()
            if stage in ('FAILED','CANCELLED'):raise ValueError('NATIVE_REPORT_TERMINAL_FAILURE')
            if stage=='READY_TO_SHARE':
                p=base/'native'/job;p.mkdir(parents=True,exist_ok=True)
                n.pull(n.ui.ROOT+'/jobs/'+job+'/delivery/index.json',p/'index.json')
                row['phase']='VERIFY_AND_SAVE';save()
                verified=n.observe_native(base,seq,known);row.update(verified);save()
                return verified.get('fullyVerifiedRawAndSystemPicker',False)
        time.sleep(3)
    row['phase']='PUBLICATION_TIMEOUT';save();return False
