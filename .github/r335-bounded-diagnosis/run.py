import os,re,subprocess,json,hashlib
from pathlib import Path
root=Path(os.environ['RUNNER_TEMP']);cand=root/'protocol-candidate'
def cmd(*argv):return subprocess.run(['adb',*map(str,argv)],capture_output=True,timeout=500)
def check(*argv):s=cmd(*argv);s.check_returncode();return s.stdout
assert check('shell','getprop','ro.kernel.qemu').strip()==b'1'
target=next(cand.glob('*-release.apk'));apk=hashlib.sha256(target.read_bytes()).hexdigest();assert apk=="a8ce8f811af22cd29b14fe46774c32a12565b9a1e5b44b2c801c5412e71ae51f"
check('install',target);check('install','-t',cand/'instrument-androidTest.apk')
installed=check('shell','pm','list','instrumentation').decode()
target=re.search(r'instrumentation:(com[.]starcore[.]research[.]r13[.]test/com[.]ethk3[.]novaonepro[.]R30FaultAndroidRunner) [(]target=com[.]starcore[.]research[.]r13[)]',installed)
assert target
res=cmd('shell','am','instrument','-w','-r','-e','class','com.ethk3.novaonepro.R335AppBoundaryTest',target[1])
text=res.stdout.decode(errors='replace');log=check('logcat','-d','-s','System.out:I').decode(errors='replace')
keys={'recorded','enabled','memoryJournal','frameworkFormat','projection','chart1','chart2','chart3'}|{f'R0{x}{k}Bytes' for x in (1,2,3) for k in ('Original','Page')}
enums={'YES','NO','ABSENT','OTHER','VALID','EMPTY','INVALID','ANDROID_BINARY_XML','TEXT_OR_OTHER','PASS','XML_PARSE','PREF_ROOT','JOURNAL_TYPE_OR_DUPLICATE','STATUS_TYPE_OR_DUPLICATE','ASSIGNMENT_TYPE_OR_DUPLICATE','ASSIGNMENT_VALUE','ASSIGNMENT_INCOMPLETE','ASSIGNMENT_HISTORY_MISMATCH','JOURNAL_TRUNCATED','JOURNAL_HASH','JOURNAL_HEADER','JOURNAL_IDENTITY','JOURNAL_ELAPSED_REVERSED','JOURNAL_CLOCK_DISCONTINUITY','UNCLASSIFIED_STATUS','CHART','FRAME_BYTE_BUDGET','CURRENT_ORIGINAL_RESEARCH_SOURCE_REQUIRED','FRAME_HASH_MISMATCH','RESEARCH_CACHE_NOT_BOUND_TO_ORIGINAL','RESEARCH_FRAME_REQUIRES_PAGINATION','WAITING_ANALYSIS','R27_JSON','CursorWindow','RESEARCH_SERIES_LIMIT','NullPointerException','IllegalArgumentException'}
obs={}
for key,val in re.findall(r'R335_BOUNDED (\w+) (\w+)',text+'\n'+log):
 if key in keys and (val in enums or (key.endswith('Bytes') and val.isdigit() and len(val)<=8)):obs[key]=val
success=re.search(r'OK \((\d+) tests?\)',text)
out={'run':int(os.environ['GITHUB_RUN_ID']),'sourceCommit':'69eab914678bc988b71f2d7ce2287ccf89bd49eb','apkSha256':apk,'device':'fresh disposable emulator','namedTestsExecuted':int(success[1]) if success else None,'testOutputSha256':hashlib.sha256(res.stdout).hexdigest(),'observations':obs}
(root/f"R335-app-boundary-{out['run']}.json").write_text(json.dumps(out,indent=2)+'\n')
