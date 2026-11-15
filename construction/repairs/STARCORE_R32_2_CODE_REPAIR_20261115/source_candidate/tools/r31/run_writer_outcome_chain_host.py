#!/usr/bin/env python3
"""Small actual coordinator -> ledger -> flush -> report HOST proof, no business stubs."""
import argparse,datetime,hashlib,importlib.util,json,os,shutil,subprocess,sys
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
SOURCE_NAMES=['DatabaseCoordinator','strategyv2/FailureDispositionR31','strategyv2/WriterHoldLedgerR30','strategyv2/ReportSnapshotR30','strategyv2/ReportR27']
def sha(path):return hashlib.sha256(path.read_bytes()).hexdigest()
def main():
 p=argparse.ArgumentParser(description=__doc__);p.add_argument('--classes',type=Path,required=True);p.add_argument('--ahead',type=Path,required=True);p.add_argument('--ahead-receipt',type=Path,required=True);p.add_argument('--out',type=Path,required=True);a=p.parse_args();out=a.out.resolve();out.mkdir(parents=True,exist_ok=False)
 receipt={'scope':'FIVE_ROOT_FROZEN_ACTUAL_PRODUCTION_CLASS_OVERLAY_NATIVE_SQLITE_HOST_NOT_WHOLE_NEW_BUILD_OR_RELEASE','status':'IN_PROGRESS','commands':[],'startedAtUtc':datetime.datetime.now(datetime.timezone.utc).isoformat()}
 def save():(out/'RECEIPT.json').write_text(json.dumps(receipt,indent=2)+'\n')
 def run(argv,label):
  cmd=list(map(str,argv));environment=dict(os.environ);environment['TMPDIR']=str(out/'tmp');print(label,flush=True);z=subprocess.run(cmd,cwd=ROOT,env=environment,text=True,capture_output=True,timeout=90);(out/(label+'.log')).write_text(z.stdout+z.stderr);receipt['commands'].append({'label':label,'argv':cmd,'exitCode':z.returncode});save()
  if z.returncode:raise RuntimeError(label+': '+(z.stdout+z.stderr)[-2500:])
  return z.stdout
 try:
  spec=importlib.util.spec_from_file_location('portable',ROOT/'tools/r31/run_atomnet_publication_host.py');portable=importlib.util.module_from_spec(spec);spec.loader.exec_module(portable)
  java=portable.java_tool('java');javac=portable.java_tool('javac');sdk=Path(os.environ.get('ANDROID_HOME',os.environ.get('ANDROID_SDK_ROOT','')))/'platforms/android-36/android.jar';jdbc=Path(os.environ['SQLITE_JDBC']);original=json.loads(a.ahead_receipt.read_text())
  source_paths=[ROOT/'app/src/main/java/com/ethk3/novaonepro'/(name+'.java') for name in SOURCE_NAMES];source_before={str(f.relative_to(ROOT)):sha(f) for f in source_paths}
  for name,digest in source_before.items():
   if original['sourceSha256'].get(name)!=digest:raise AssertionError('ROOT_FROZEN_SOURCE_CHANGED:'+name)
  if original['baseJarSha256']!=sha(a.classes):raise AssertionError('ROOT_WHOLE_SUPPORT_CHANGED')
  for name in ['tmp','sources','platform','tests','support','ahead']:(out/name).mkdir()
  whole=out/'support/classes.jar';shutil.copy2(a.classes,whole);shutil.copytree(a.ahead,out/'ahead',dirs_exist_ok=True);shutil.copy2(a.ahead_receipt,out/'ROOT_AHEAD_RECEIPT.json')
  for f in source_paths:shutil.copy2(f,out/'sources'/f.name)
  receipt['sourceSha256']=source_before;receipt['wholeSupportSha256']=sha(whole);receipt['rootCompileReceiptSha256']=sha(a.ahead_receipt);receipt['aheadClassSha256']={str(f.relative_to(out/'ahead')):sha(f) for f in sorted((out/'ahead').rglob('*.class'))};receipt['jdbcSha256']=sha(jdbc)
  definitions=portable.platform_sources();definitions['android/os/Build.java']='package android.os;public final class Build{public static final String MODEL="HOST_NATIVE_BOUNDARY";public static final class VERSION{public static final int SDK_INT=0;}}'
  name='android/database/sqlite/SQLiteDatabase.java';old=definitions[name];needle='public static SQLiteDatabase openDatabase(String path,CursorFactory factory,int flags){try{';replacement='public static SQLiteDatabase openDatabase(String path,CursorFactory factory,int flags){if(Boolean.getBoolean("r31.host.receipt.readback.unavailable"))throw new SQLiteException("HOST_INDEPENDENT_READBACK_UNAVAILABLE");try{';assert needle in old;definitions[name]=old.replace(needle,replacement,1)
  platform=[]
  for name,body in definitions.items():
   f=out/'sources/platform'/name;f.parent.mkdir(parents=True,exist_ok=True);f.write_text(body);platform.append(f)
  host=ROOT/'tools/r31/host/R31WriterOutcomeChainHost.java';shutil.copy2(host,out/'sources'/host.name);receipt['fixtureSha256']=sha(host);receipt['runnerSha256']=sha(Path(__file__));receipt['platformSourceSha256']={str(f.relative_to(out)):sha(f) for f in platform}
  cc=[javac,'-J-Djava.io.tmpdir='+str(out/'tmp'),'-sourcepath','','-implicit:none','-encoding','UTF-8'];run([*cc,'-cp',str(whole)+os.pathsep+str(sdk),'-d',out/'platform',*platform],'platform-compile')
  cp=os.pathsep.join(map(str,[out/'platform',out/'tests',out/'ahead',whole,jdbc,sdk]));run([*cc,'-cp',cp,'-d',out/'tests',out/'sources'/host.name],'native-fixture-compile');raw=run([java,'-Djava.io.tmpdir='+str(out/'tmp'),'-Xmx512m','-cp',cp,'com.ethk3.novaonepro.strategyv2.R31WriterOutcomeChainHost',out/'data/databases/main.db',out/'ahead',whole],'native-chain')
  result=json.loads(raw);assert result['status']=='ACTUAL_WRITER_LEDGER_FLUSH_REPORT_CHAIN_PASS';receipt['result']={k:v for k,v in result.items() if k!='sql'};receipt['sourceStable']=all(sha(f)==source_before[str(f.relative_to(ROOT))] for f in source_paths);assert receipt['sourceStable'];receipt['workingDirectoryBytes']=sum(f.stat().st_size for f in out.rglob('*') if f.is_file());assert receipt['workingDirectoryBytes']<100*1024*1024;receipt.update(status='TARGETED_NATIVE_CHAIN_PASS',device='NOT_RUN',naturalQuality='NOT_PROVEN',finishedAtUtc=datetime.datetime.now(datetime.timezone.utc).isoformat());save();return 0
 except Exception as problem:receipt.update(status='FAILED',error=str(problem));save();print(str(problem),file=sys.stderr);return 1
if __name__=='__main__':sys.exit(main())
