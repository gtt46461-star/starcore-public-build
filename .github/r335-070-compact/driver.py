import importlib.util,os,json,time,re,zipfile,hashlib
from pathlib import Path
temp=Path(os.environ['RUNNER_TEMP'])
npath=Path(os.environ['GITHUB_WORKSPACE'])/'.github/r335-vivo-final-export/driver.py'
spec=importlib.util.spec_from_file_location('reader',npath);n=importlib.util.module_from_spec(spec);spec.loader.exec_module(n)
f=n.field;pkg=f.PKG
n.apk='9bc5131d5e631b0983a3b0106eb5e740d7ff9eaed2875aae7af44170a098d4d3'
n.source='ff555a802da6c135d395cce51451b0b1ade93b69'
record={'runId':int(os.environ['GITHUB_RUN_ID']),'apkSha256':n.apk,'sourceCommit':n.source,'scope':'DISPOSABLE_ANDROID14_DEFAULT_DETAILED_UI_AND_CONTENT_PROVIDER','stage':'START','transitions':[]}
out=temp/('R335-070-compact-'+str(record['runId'])+'.json')
def save(stage):
 record['stage']=stage;out.write_text(json.dumps(record,ensure_ascii=False,indent=2)+'\n')
def check(v,c):
 if not v:raise ValueError(c)
def readHierarchy():
 import xml.etree.ElementTree as ET, subprocess
 remote='/sdcard/r335-current-compact-ui.xml'
 pending=None
 for i in range(3):
  try:
   result=n.ui.adb('shell','uiautomator','dump',remote,timeout=85)
   if 'dumped to:' not in result:raise ValueError('HIERARCHY_NOT_WRITTEN')
   body=n.ui.adb('shell','cat',remote,timeout=30)
   node=ET.fromstring(body);(n.private/'last-ui.xml').write_text(body);return node
  except (ValueError,ET.ParseError,RuntimeError,subprocess.TimeoutExpired) as problem:
   pending=problem;time.sleep(2)
 raise pending
n.ui.hierarchy=readHierarchy
def main():
 f.EVID.mkdir(exist_ok=True);check(f.adb('shell','getprop','ro.kernel.qemu').strip()==b'1','ISOLATED_GUEST')
 f.adb('root');f.adb('wait-for-device')
 check(not f.adb('shell','pm','list','packages',pkg).strip(),'EXPECTED_FRESH_GUEST')
 old=n.observe.get_apk('B08',577311065,'a724adbfb31dd96f1459aa2e5cbd94582dd6e82d')
 check(f.fingerprint(old)=='09af709a6dc462f0f883156d20a723c0eb60a63aa6a43896e2c65ffa1905cced','PREVIOUS_IDENTITY')
 f.adb('install',str(old));f.adb('shell','am','start','-W','-n',f.ACTIVITY);time.sleep(35)
 olduid,_=f.installed(f.EVID/'before.txt',577311065)
 current=n.observe.get_apk('B08',577311070,n.source);check(f.fingerprint(current)==n.apk,'SIGNED_IDENTITY')
 f.adb('install','-r',str(current))
 check(f.installed(f.EVID/'current.txt',577311070)==(olduid,n.apk),'GUEST_VERSION')
 f.adb('shell','pm','grant',pkg,'android.permission.POST_NOTIFICATIONS')
 f.adb('shell','am','start','-W','-n',f.ACTIVITY)
 expected='清理旧报告并生成当前完整报告';n.report_dialog(expected)
 prior=n.ui.acknowledgements()
 check(n.tap(lambda x:x.get('text')==expected and x.get('enabled')=='true',15),'OPEN_DEFAULT')
 save('REQUESTED');deadline=time.monotonic()+3600;job=None
 while time.monotonic()<deadline:
  for entry in sorted(n.ui.acknowledgements()-prior):
   try:d=n.ui.read_json(entry)
   except (OSError,ValueError,RuntimeError):continue
   token=d.get('requestId','');stage=d.get('stage','')
   if d.get('mode')!='FULL_REPORT' or not re.fullmatch('[0-9a-f]{64}',token):continue
   if job is None:job=token;record['jobId']=job
   if token!=job:continue
   sign=[stage,d.get('resumeStage'),d.get('revision'),[x for x in d.get('reason','').split('|') if re.fullmatch('[A-Z][A-Z0-9_]{2,65}',x)]]
   if not record['transitions'] or sign!=record['transitions'][-1]:record['transitions'].append(sign);save('WAITING')
   if stage in ('FAILED','CANCELLED'):raise ValueError('NATIVE_REPORT_FAILED')
   if stage=='READY_TO_SHARE':
    idxdir=n.ui.ROOT+'/jobs/'+job+'/delivery'
    dest=n.private;archive=dest/'joined.zip';parts=dest/'parts';parts.mkdir(exist_ok=True)
    n.pull(idxdir+'/index.json',dest/'index.json');idx=n.read(dest/'index.json')
    hashes=n.assemble(idx,idxdir,parts,archive);save('VERIFY_SOURCE')
    with zipfile.ZipFile(archive) as z:
     manifest=json.loads(z.read('source-manifest.json'));doc=json.loads(z.read('R30_REPORT.json'))
     check(manifest['sourceCommit']==n.source and manifest['apkSha256']==n.apk and z.testzip() is None,'MIXED_RELEASE')
     record.update(archiveSha256=f.fingerprint(archive),archiveBytes=archive.stat().st_size,zipMembers=len(z.namelist()),reportBytes=z.getinfo('R30_REPORT.json').file_size,reportMarkdownBytes=z.getinfo('R30_REPORT.md').file_size,rawEvidenceIncluded=manifest['rawEvidenceIncluded'],chapters=len(doc['chapters']),metrics=len(doc['metricResults']),checksComplete=d.get('checksComplete'),allChecksPassed=d.get('allChecksPassed'))
    strict=n.oracle.validate_export(archive,{'profile':'DIAGNOSTIC','jobId':job,'sourceCommit':n.source,'apkSha256':n.apk})
    record['originalVerifier']=strict['status'];save('SYSTEM_PICKER')
    record.update(n.system_save(idx,job,archive,dest,hashes))
    check(record['actualSystemPicker'] and record['safExternalZipSha256']==record['archiveSha256'],'EXTERNAL_BYTES')
    save('VERIFIED');return
  time.sleep(3)
 raise ValueError('REAL_PUBLICATION_TIMEOUT')
try:main()
except Exception as e:
 import traceback
 record['failurePhase']=record['stage'];record['failure']=str(e) if re.fullmatch('[A-Z_|]{1,100}',str(e)) else type(e).__name__
 record['frames']=[Path(k.filename).name+':'+str(k.lineno) for k in traceback.extract_tb(e.__traceback__)][-8:];save('INCOMPLETE');raise
