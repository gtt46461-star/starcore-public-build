#!/usr/bin/env python3
"""Independent sequential P1..P5 package rehearsal. No candidate production edits."""
from pathlib import Path
import datetime,hashlib,json,os,stat,subprocess,sys,traceback,zipfile

import argparse
cli=argparse.ArgumentParser(description=__doc__)
for name in ('package','out','android-jar','sqlite-jdbc','slf4j-api','json-jar'):
 cli.add_argument('--'+name,type=Path,required=True)
args=cli.parse_args()
PACKAGE=args.package.resolve()
OUT=args.out.resolve()
if OUT.exists():raise ValueError('OUT_MUST_BE_NEW')
OUT.mkdir(parents=True)
REPO=OUT/'stage-worktree'
EXACT=OUT/'exact-baseline'
SDK=args.android_jar.resolve()
JDBC=args.sqlite_jdbc.resolve()
SLF=args.slf4j_api.resolve()
JSON_JAR=args.json_jar.resolve()
PY=sys.executable

def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def now():return datetime.datetime.now(datetime.timezone.utc).isoformat()

result={'format':'R32_5_SEQUENTIAL_STAGED_REHEARSAL_V1','startedAt':now(),'status':'IN_PROGRESS',
 'package':str(PACKAGE),'packageManifestSha256':sha(PACKAGE/'code_manifest.json'),
 'baselineZipSha256':sha(PACKAGE/'SOURCE_BASELINE.zip'),
 'sourceScope':'EXACT_ZIP_BASELINE_THEN_PACKAGE_P1_THROUGH_P5_NO_LATER_CLASS_SUPPORT',
 'AndroidBuild':'NOT_RUN','device':'NOT_RUN','naturalQuality':'NOT_PROVEN',
 'coldArchiveReclamation':'RUN_CURRENT_P2_CONTRACTS; NOT_DEVICE_LONGEVITY','commands':[],'stages':{}}

def save():
 p=OUT/'RESULTS.json';tmp=p.with_suffix('.tmp');tmp.write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n');tmp.replace(p)

def run(label,argv,cwd=REPO,env=None):
 log=OUT/(label+'.log');entry={'id':label,'argv':list(map(str,argv)),'cwd':str(cwd),'startedAt':now(),'log':str(log.relative_to(OUT))};result['commands'].append(entry);save()
 print('START '+label,flush=True)
 with log.open('w') as fd:
  process=subprocess.run(entry['argv'],cwd=cwd,env=env,stdout=fd,stderr=subprocess.STDOUT,timeout=240)
 entry.update(exitCode=process.returncode,finishedAt=now(),logSha256=sha(log));save()
 print('END '+label+' exit='+str(process.returncode),flush=True)
 if process.returncode:raise RuntimeError(label+' failed: '+log.read_text()[-8000:])
 return log.read_text()

def extract(target):
 target.mkdir(parents=True,exist_ok=False);count=0;modes={}
 with zipfile.ZipFile(PACKAGE/'SOURCE_BASELINE.zip') as z:
  for member in z.infolist():
   rel=Path(member.filename)
   if rel.is_absolute() or '..' in rel.parts:raise ValueError('unsafe ZIP member')
   mode=member.external_attr>>16
   if stat.S_ISLNK(mode):raise ValueError('symlink baseline member unsupported')
   p=target/rel
   if member.is_dir():p.mkdir(parents=True,exist_ok=True);continue
   p.parent.mkdir(parents=True,exist_ok=True);p.write_bytes(z.read(member));os.chmod(p,stat.S_IMODE(mode) or 0o644);count+=1
   if mode & 0o111:modes[member.filename]=oct(stat.S_IMODE(mode))
 return {'files':count,'executableModes':modes}

def class_digest(directory):
 rows={p.relative_to(directory).as_posix():sha(p) for p in sorted(directory.rglob('*.class'))}
 return len(rows),hashlib.sha256(json.dumps(rows,sort_keys=True,separators=(',',':')).encode()).hexdigest()

try:
 save();result['baselineExtraction']=extract(EXACT);extract(REPO);save()
 run('baseline-git-init',['git','init','--quiet'],REPO)
 run('baseline-git-add',['git','add','--force','.'],REPO)
 run('baseline-git-commit',['git','-c','user.name=Staged Rehearsal','-c','user.email=rehearsal@example.invalid','commit','--quiet','-m','Local rehearsal snapshot of supplied baseline archive'],REPO)
 result['localBaselineCommit']=subprocess.check_output(['git','rev-parse','HEAD'],cwd=REPO,text=True).strip();result['localCommitIsRemoteBaselineCommit']=False;save()
 for number in range(1,6):
  batch='P'+str(number);classes=OUT/(batch+'-classes');stage={'state':'IN_PROGRESS','classesDirectory':str(classes),'startedAt':now()};result['stages'][batch]=stage;save()
  run(batch+'-apply',['python3',PACKAGE/'tools/apply_batch.py','--package',PACKAGE,'--repo',REPO,'--batch',batch,'--apply'])
  run(batch+'-identity',['python3',PACKAGE/'tools/verify_stage_identity.py','--package',PACKAGE,'--repo',REPO,'--batch',batch,'--generate'])
  log=run(batch+'-compile',['python3',PACKAGE/'tools/compile_rehearsal.py','--repo',REPO,'--android-jar',SDK,'--out',classes])
  count,digest=class_digest(classes);stage.update(productionJavaSources=len(list((REPO/'app/src/main/java').rglob('*.java'))),compiledClassCount=count,compiledClassManifestSha256=digest,typecheck='PASS_HOST_RESOURCE_DECLARATIONS_CURRENT_STAGE_SEMANTIC_GENERATION')
  host=OUT/(batch+'-host')
  if number==1:
   run(batch+'-host',['python3',REPO/'tools/r32/p1_current/run_p1_current_host.py','--repo',REPO,'--classes',classes,'--sdk',SDK,'--jdbc',JDBC,'--slf4j',SLF,'--out',host])
   receipt=json.loads((host/'RECEIPT.json').read_text());stage.update(hostState=receipt['state'],hostCases=receipt['cases'],assertions=sum(x['assertions'] or 0 for x in receipt['cases']),receipt='P1-host/RECEIPT.json')
  elif number==2:
   run(batch+'-host',['python3',REPO/'tools/r32/run_atomnet_recovery_host.py','--classes',classes,'--android-jar',SDK,'--sqlite-jdbc',JDBC,'--slf4j-api',SLF,'--out',host])
   receipt=json.loads((host/'RECEIPT.json').read_text());stage.update(hostState=receipt['status'],assertions=receipt['result']['assertions'],scenarios=len(receipt['result']['scenarios']),receipt='P2-host/RECEIPT.json')
   run(batch+'-archive-liveness',[PY,REPO/'tools/r325/archive_liveness/run_archive_liveness.py','--repo',REPO,'--classes',classes,'--android-jar',SDK,'--jdbc-jar',JDBC,'--slf4j-jar',SLF,'--out',OUT/'P2-archive-liveness'])
   run(batch+'-retention',[PY,REPO/'tools/r324/retention/run_retention_host.py','--repo',REPO,'--classes',classes,'--android-jar',SDK,'--jdbc-jar',JDBC,'--slf4j-jar',SLF,'--out',OUT/'P2-retention'])
   # Each specialized entry point uses the same current P2 classes and records its own scope.
   gc=REPO/'tools/r324/gc/run_gc_closure.py'
   if not gc.is_file():raise FileNotFoundError(gc)
   if gc.is_file():
    run(batch+'-gc',[PY,gc,'--repo',REPO,'--classes',classes,'--android-jar',SDK,'--jdbc-jar',JDBC,'--slf4j-jar',SLF,'--out',OUT/'P2-gc'])
   closure=REPO/'tools/r324/retention/run_closure_host.py'
   if not closure.is_file():raise FileNotFoundError(closure)
   if closure.is_file():
    run(batch+'-closure',[PY,closure,'--repo',REPO,'--classes',classes,'--android-jar',SDK,'--jdbc-jar',JDBC,'--slf4j-jar',SLF,'--json-jar',JSON_JAR,'--out',OUT/'P2-closure'])

  elif number==3:
   run(batch+'-host',['python3',REPO/'tools/r32/run_mainline_host.py','--repo',REPO,'--classes',classes,'--android-jar',SDK,'--sqlite-jdbc',JDBC,'--slf4j',SLF,'--out',host])
   receipt=json.loads((host/'RECEIPT.json').read_text());stage.update(hostState=receipt['status'],receipt='P3-host/RECEIPT.json')
  elif number==4:
   env=os.environ.copy();env.update(R33_DEPENDENCY_CLASSES=str(classes),R33_ANDROID_JAR=str(SDK),R33_SQLITE_JDBC=str(JDBC),R33_SLF4J_JAR=str(SLF),R33_INPUT_DIR=str(PACKAGE/'evidence/report_inputs'),R33_REPORT_OUT=str(host))
   run(batch+'-host',['bash',REPO/'tools/r33/report_audit/run_report_reaudit.sh'],env=env)
   receipt=json.loads((host/'receipt.json').read_text());stage.update(hostState=receipt['status'],assertions=receipt['assertions'],originalReportRerenders=len(receipt['rerenders']),receipt='P4-host/receipt.json',reportInputs={p.name:sha(p) for p in [PACKAGE/'evidence/report_inputs/report326_input.json',PACKAGE/'evidence/report_inputs/report425_input.json']})
   run(batch+'-diagnostic',[PY,REPO/'tools/r324/diagnostic/run_diagnostic.py','--repo',REPO,'--out',OUT/'P4-diagnostic'])
   run(batch+'-q08-originals',[PY,REPO/'tools/r325/diagnostic/run_q08_diagnostic.py','--repo',REPO,'--dependency-classes',classes,'--android-jar',SDK,'--sqlite-jdbc',JDBC,'--slf4j-jar',SLF,'--out',OUT/'P4-q08-originals'])

  else:
   run(batch+'-device-tool-regression',[PY,REPO/'tools/r31/test_device_receipts.py'])
   run(batch+'-apply-contract',['python3',PACKAGE/'tools/test_apply_batch.py'],cwd=PACKAGE)
   run(batch+'-resume-contract',[PY,PACKAGE/'tools/test_inspect_resume.py'],cwd=PACKAGE)
   run(batch+'-file-card-contract',[PY,PACKAGE/'tools/test_verify_package.py'],cwd=PACKAGE)
   run(batch+'-release-contract',['python3',PACKAGE/'tools/test_release_contract.py','--baseline',EXACT],cwd=PACKAGE)
   run(batch+'-r926-check',['python3',REPO/'scripts/r926_release_manifest.py','--check'])
   run(batch+'-r926-drift',['python3',REPO/'scripts/r926_release_manifest.py','--drift-test'])
   run(batch+'-r982-check',['python3',REPO/'scripts/r982_source_call_asset.py'])
   stage.update(hostState='PASS_PACKAGE_APPLY_RELEASE_AND_SOURCE_INDEX_CONTRACTS')
  after_count,after_digest=class_digest(classes)
  if (count,digest)!=(after_count,after_digest):raise RuntimeError('stage support classes mutated '+batch)
  stage.update(state='PASS_DECLARED_STAGE_HOST_SCOPE',finishedAt=now());save()
 if result['packageManifestSha256']!=sha(PACKAGE/'code_manifest.json'):raise RuntimeError('package manifest changed during rehearsal')
 result.update(status='PASS_SEQUENTIAL_STAGE_HOST_NOT_ANDROID_OR_FULL_PROJECT_CLOSURE',finishedAt=now());save();print('ALL_STAGES_HOST_PASS',flush=True)
except Exception as error:
 result.update(status='FAILED_OR_BLOCKED',error=str(error),errorType=type(error).__name__,finishedAt=now());save();traceback.print_exc();sys.exit(1)
