"""Open a new report in the installed app and export through the system picker on a fresh QA upgrade."""
import importlib.util,json,os,subprocess,time,re,traceback
from pathlib import Path
w=Path(os.environ['GITHUB_WORKSPACE'])
tmp=Path(os.environ['RUNNER_TEMP'])
c='d417e459c1bd0e89d70b4dd549f5555b93e49d4e'
spec=importlib.util.spec_from_file_location('readback',w/'.github/r335-vivo-final-export/driver.py')
n=importlib.util.module_from_spec(spec);spec.loader.exec_module(n)
f=n.field;n.source=c
out=tmp/('R335-094-report-'+os.environ['GITHUB_RUN_ID']+'.json')
record={'sourceCommit':c,'runId':int(os.environ['GITHUB_RUN_ID']),'scope':'NORMAL_APP_UI_ON_UPGRADED_ANDROID_14','attempts':[]}
def save():out.write_text(json.dumps(record,ensure_ascii=False,indent=2)+'\n')
def request(seq,known):
    base=f.NATIVE/'B08'/str(seq);base.mkdir(parents=True,exist_ok=True)
    row={'attempt':seq,'stages':[],'id':None,'phase':'OPEN_UI'}
    record['attempts'].append(row);save()
    f.adb('shell','am','start','-W','-n',f.ACTIVITY)
    # A cold upgrade can initially expose the startup overlay. Reopen the actual
    # main Activity before retrying; keep the same ordinary UI and validation.
    for attempt in range(3):
        try:
            n.report_dialog('另存完整原始备份（体积大）')
            break
        except (AssertionError, ValueError) as failure:
            row.setdefault('openAttempts',[]).append(type(failure).__name__)
            save()
            if attempt==2:raise
            f.adb('shell','input','keyevent','KEYCODE_BACK')
            f.adb('shell','am','start','-W','-n',f.ACTIVITY)
            time.sleep(5)
    before=n.ui.acknowledgements()
    if not n.tap(lambda node: node.get('text')=='另存完整原始备份（体积大）' and node.get('enabled')=='true',15):
        raise RuntimeError('RAW_MENU_NOT_CLICKED')
    dismissal_deadline = time.monotonic() + 20
    while '另存完整原始备份（体积大）' in n.labels():
        if time.monotonic() >= dismissal_deadline:
            raise RuntimeError('RAW_MENU_DID_NOT_DISMISS')
        time.sleep(0.4)
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
