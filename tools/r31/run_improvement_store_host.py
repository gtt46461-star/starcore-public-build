#!/usr/bin/env python3
"""Actual confirmation packet/registration/subscription/cost/claim production
Store on native SQLite. Controlled720-row clock is synthetic mechanics only."""
from pathlib import Path
import argparse,datetime,hashlib,json,os,subprocess,tempfile,uuid
ROOT=Path(__file__).resolve().parents[2]
def digest(p):
 if p.is_file():return hashlib.sha256(p.read_bytes()).hexdigest()
 h=hashlib.sha256()
 for f in sorted(p.rglob('*.class')):h.update(str(f.relative_to(p)).encode()+b'\0'+hashlib.sha256(f.read_bytes()).digest())
 return h.hexdigest()
def main():
 p=argparse.ArgumentParser();p.add_argument('--out',type=Path,required=True);p.add_argument('--classes',type=Path,default=ROOT/'app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes');a=p.parse_args();out=a.out.resolve();out.mkdir(parents=True,exist_ok=False)
 sdk=Path(os.environ['ANDROID_HOME'])/'platforms/android-36/android.jar';jdbc=Path(os.environ.get('SQLITE_JDBC','/tmp/r31-baseline-deps/sqlite-jdbc.jar'));slf=next(Path(os.environ['GRADLE_USER_HOME']).glob('caches/modules-2/files-2.1/org.slf4j/slf4j-api/*/*/*.jar'))
 source=ROOT/'app/src/main/java/com/ethk3/novaonepro/strategyv2';sources=[source/(n+'.java') for n in ['ImprovementStoreR31','FrozenConfirmationR30','FrozenConfirmationStoreR30','VerifiedImprovementR31','ReplayDatasetR27','ObserverPublicationStoreR31']]
 bound=sources+[source/'RuntimeR27.java',ROOT/'app/src/main/java/com/ethk3/novaonepro/LifecycleSchemaR31.java',Path(__file__).resolve(),ROOT/'tools/r31/unified_host/ImprovementStoreR31Host.java'];shims=list((ROOT/'tools/r31/ddl_host').rglob('*.java'))+list((ROOT/'tools/r29/qual_host/shim/android/database/sqlite').glob('SQLite*Exception.java'));bound+=shims
 before={str(f.relative_to(ROOT)):digest(f) for f in bound};support=digest(a.classes);receipt={'format':'R31_IMPROVEMENT_NATIVE_STORE_1','invocationId':str(uuid.uuid4()),'head':subprocess.check_output(['git','rev-parse','HEAD'],cwd=ROOT,text=True).strip(),'sourceSha256':before,'supportSha256':support,'scope':'ACTUAL_CONFIRM_PACKET_STORE_AND_FORWARD_REGISTRATION_SUBSCRIPTION_COST_CALLBACK_CLAIM_SQLITE; SYNTHETIC_TIME_COMPRESSED720OPPORTUNITIES; NOT_ANDROID_NOT_NATURAL_NOT_WHOLE_CARD','status':'STARTED_NOT_PASS','records':[]}
 def save():(out/'RECEIPT.json').write_text(json.dumps(receipt,indent=2)+'\n')
 def run(cmd,name):
  r=subprocess.run(list(map(str,cmd)),cwd=ROOT,capture_output=True,text=True,timeout=180);(out/(name+'.log')).write_text(r.stdout+r.stderr);receipt['records'].append({'stage':name,'exitCode':r.returncode});save()
  if r.returncode:raise RuntimeError(name+'\n'+(r.stdout+r.stderr)[-4500:])
  print(name+' PASS',flush=True)
 save()
 try:
  with tempfile.TemporaryDirectory(prefix='r31-improvement-native-') as directory:
   work=Path(directory);platform=work/'platform';product=work/'product';tests=work/'test'
   for f in [platform,product,tests]:f.mkdir()
   run(['javac','-cp',sdk,'-d',platform,*shims],'platform')
   run(['javac','-encoding','UTF-8','-sourcepath','','-implicit:none','-cp',os.pathsep.join(map(str,[sdk,a.classes])),'-d',product,*sources],'actual-production')
   cp=os.pathsep.join(map(str,[platform,product,tests,a.classes,jdbc,slf,sdk]));run(['javac','-cp',cp,'-d',tests,ROOT/'tools/r31/unified_host/ImprovementStoreR31Host.java'],'host')
   run(['java','-Xmx512m','-cp',cp,'com.ethk3.novaonepro.strategyv2.ImprovementStoreR31Host',work/'main.db'],'actual-Store-native-protocol')
  if before!={str(f.relative_to(ROOT)):digest(f) for f in bound} or support!=digest(a.classes):raise RuntimeError('SOURCE_OR_SUPPORT_CHANGED_DURING_EXECUTION')
  receipt.update(status='PASS_NATIVE_PROTOCOL_NOT_NATURAL_OR_ANDROID',sourceAndSupportUnchanged=True)
 except BaseException:receipt['status']='FAILED_CURRENT_EXECUTION';raise
 finally:receipt['finishedAt']=datetime.datetime.now(datetime.timezone.utc).isoformat();save()
if __name__=='__main__':main()
