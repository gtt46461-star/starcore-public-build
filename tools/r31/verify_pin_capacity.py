#!/usr/bin/env python3
"""Actual pin-size comparison, native SQLite regressions, and explicit capacity/owner fault fixtures."""
from pathlib import Path
import argparse,datetime,hashlib,json,os,shutil,subprocess,tempfile
ROOT=Path(__file__).resolve().parents[2];SHA=lambda data:hashlib.sha256(data).hexdigest()
def main():
 ap=argparse.ArgumentParser();ap.add_argument('--support-jar',type=Path,required=True);args=ap.parse_args()
 out=ROOT/'verification/r31/p02/pin-capacity/runs'/datetime.datetime.now(datetime.timezone.utc).strftime('%Y%m%dT%H%M%SZ');out.mkdir(parents=True);work=Path(tempfile.mkdtemp(prefix='r31-pin-capacity-final-'));src,platform,prod,host=[work/n for n in ['sources','platform','production','host']]
 for p in [src,platform,prod,host]:p.mkdir()
 sdk=Path('/workspace/starcore-environment/android-sdk/platforms/android-36/android.jar');jdbc=Path('/tmp/r31-baseline-deps/sqlite-jdbc.jar');jdk=Path('/workspace/starcore-environment/tools/jdk-17.0.17+10/bin')
 receipt={'schema':'R31_PIN_CAPACITY_NATIVE_1','scope':'ACTUAL_PIN_MEASUREMENT_AND_NATIVE_PROTOCOL_WITH_EXPLICIT_DISK_FIXTURES_NOT_ART_DEVICE_OR_NATURAL','status':'IMPLEMENTING','work':str(work),'head':subprocess.check_output(['git','rev-parse','HEAD'],cwd=ROOT,text=True).strip(),'supportJar':str(args.support_jar),'supportJarSha256':SHA(args.support_jar.read_bytes()),'runnerSha256':SHA(Path(__file__).read_bytes()),'sources':[],'commands':[],'art':'NOT_RUN','device':'NOT_RUN','24h':'NOT_RUN','72h':'NOT_RUN','naturalQuality':'NOT_EVALUATED'}
 def save():(out/'RECEIPT.json').write_text(json.dumps(receipt,ensure_ascii=False,indent=2)+'\n')
 def snap(p,q):
  b=p.read_bytes();q.parent.mkdir(parents=True,exist_ok=True);q.write_bytes(b);receipt['sources'].append({'origin':str(p),'snapshot':str(q),'sha256':SHA(b)});return q
 def run(name,argv):
  p=subprocess.run(list(map(str,argv)),cwd=work,capture_output=True,text=True,timeout=180);(out/(name+'.stdout.log')).write_text(p.stdout);(out/(name+'.stderr.log')).write_text(p.stderr);receipt['commands'].append({'name':name,'argv':list(map(str,argv)),'exit':p.returncode,'stdoutSha256':SHA(p.stdout.encode()),'stderrSha256':SHA(p.stderr.encode())});save();print(json.dumps({'name':name,'exit':p.returncode,'out':str(out)}),flush=True)
  if p.returncode:raise RuntimeError(name+' failed; raw stdout/stderr retained')
  return p.stdout
 try:
  shims=[]
  for p in (ROOT/'tools/r31/ddl_host').rglob('*.java'):
   q=snap(p,src/'platform'/p.relative_to(ROOT/'tools/r31/ddl_host'))
   if p.name=='SQLiteDatabase.java':
    text=q.read_text();old='public Cursor rawQuery(String sql,String[] args){log(';assert text.count(old)==1;text=text.replace(old,'public Cursor rawQuery(String sql,String[] args){if(args!=null)for(int i=0;i<args.length;i++)if(args[i]==null)throw new IllegalArgumentException("the bind value at index "+(i+1)+" is null");log(');q.write_text(text);receipt['strictNullAdapterSha256']=SHA(text.encode())
   shims.append(q)
  shims.append(snap(ROOT/'tools/r29/qual_host/shim/android/database/sqlite/SQLiteException.java',src/'platform/android/database/sqlite/SQLiteException.java'))
  for p in (ROOT/'verification/r932/android-json').rglob('*.java'):shims.append(snap(p,src/'json'/p.relative_to(ROOT/'verification/r932/android-json')))
  adapters={'android/os/SystemClock.java':'package android.os;public final class SystemClock{public static long elapsedRealtime(){return System.nanoTime()/1000000;}public static long uptimeMillis(){return elapsedRealtime();}public static long currentThreadTimeMillis(){return java.lang.management.ManagementFactory.getThreadMXBean().getCurrentThreadCpuTime()/1000000;}}','android/os/Looper.java':'package android.os;public final class Looper{public static Looper myLooper(){return null;}public static Looper getMainLooper(){return null;}}','android/util/Log.java':'package android.util;public final class Log{public static int w(String t,String m){return 0;}public static int w(String t,String m,Throwable e){return 0;}public static int e(String t,String m){return 0;}public static int e(String t,String m,Throwable e){return 0;}public static int d(String t,String m){return 0;}public static int i(String t,String m){return 0;}}'}
  for name,text in adapters.items():q=src/'platform'/name;q.parent.mkdir(parents=True,exist_ok=True);q.write_text(text);shims.append(q)
  receipt['hostOnlyAdapters']=adapters
  run('platform-javac',[jdk/'javac','-encoding','UTF-8','-cp',sdk,'-d',platform,*shims])
  files=[snap(ROOT/'app/src/main/java/com/ethk3/novaonepro/strategyv2'/f'{n}.java',src/'production'/f'{n}.java') for n in ['ResearchInputPinR30R1','PinPublicationR31','PinSnapshotProofR31','ResearchSourceJournalR31']]
  run('production-javac',[jdk/'javac','-encoding','UTF-8','-sourcepath','','-implicit:none','-cp',str(args.support_jar)+os.pathsep+str(sdk),'-d',prod,*files])
  files=[snap(ROOT/'tools/r31/pin_capacity_host'/f'{n}.java',src/f'{n}.java') for n in ['PinCapacityMeasureR31Host','PinAdmissionR31Host']]
  files += [snap(ROOT/'tools/r30-repair/qual_host/RepairInputPinR30HostTest.java',src/'RepairInputPinR30HostTest.java'),snap(ROOT/'tools/r31/input_pin_host/InputPinJournalR31Host.java',src/'InputPinJournalR31Host.java')]
  prior=src/'InputPinJournalR31Host.java';text=prior.read_text();old='@Override public void close()throws SQLException{jdbc.close();}'
  assert text.count(old)==1;text=text.replace(old,'@Override public void close(){try{jdbc.close();}catch(SQLException failed){throw new IllegalStateException(failed);}}');prior.write_text(text)
  receipt['legacyHostCloseAdapter']={'purpose':'Only HOST close checked-exception signature adjusted for rich SQLite boundary; all production code and assertions unchanged','compiledSha256':SHA(text.encode())}
  cp=os.pathsep.join(map(str,[platform,prod,args.support_jar,jdbc,sdk]));run('host-javac',[jdk/'javac','-encoding','UTF-8','-sourcepath','','-implicit:none','-cp',cp,'-d',host,*files])
  baseline=os.pathsep.join(map(str,[platform,host,args.support_jar,jdbc,sdk]));current=os.pathsep.join(map(str,[platform,prod,host,args.support_jar,jdbc,sdk]))
  old=run('baseline-pin-measurement',[jdk/'java','-Xmx512m','-cp',baseline,'com.ethk3.novaonepro.strategyv2.PinCapacityMeasureR31Host'])
  new=run('current-pin-measurement',[jdk/'java','-Xmx512m','-cp',current,'com.ethk3.novaonepro.strategyv2.PinCapacityMeasureR31Host'])
  old=json.loads(old.splitlines()[-1]);new=json.loads(new.splitlines()[-1]);fields=['rows','distribution','rawBytes','gzipBytes','base64Bytes','summaryUtf8Bytes','actualBindBytes','originalHash','roles','batchOps']
  assert len(old['cases'])==len(new['cases'])==4
  for a,b in zip(old['cases'],new['cases']):
   assert all(a[k]==b[k] for k in fields),(a,b)
  receipt['actualBaselineCurrentParity']={'status':'PASS','fields':fields,'caseCount':4,'rawAndRolesAlsoDecodedAndChecked':True}
  for name in ['PinAdmissionR31Host','RepairInputPinR30HostTest','InputPinJournalR31Host']:run(name,[jdk/'java','-Xmx512m','-cp',current,'com.ethk3.novaonepro.strategyv2.'+name])
  changed=[]
  for f in receipt['sources']:
   f['originUnchanged']=SHA(Path(f['origin']).read_bytes())==f['sha256']
   if not f['originUnchanged']:changed.append(f['origin'])
  if changed:raise RuntimeError('Source drift; snapshot only:'+repr(changed))
  if SHA(args.support_jar.read_bytes())!=receipt['supportJarSha256']:raise RuntimeError('Support jar drift')
  receipt['status']='NATIVE_NAMED_PIN_ADMISSION_PASS_NOT_ART_OR_WHOLE_CARD'
  receipt['open']=['Root Runtime caller must hold appendAdmitted scope across actual submit/retry/finally','Journal retention and optional import/online-window fence capacity admission remain OPEN','Native zlib capacity and restored-input lifetime admission remain OPEN','Actual production allocated-block/free-space tests require ART; HOST positive disk config explicitly uses logical-byte fixtures','No device duration or natural quality proof']
 except Exception as error:receipt['status']='FAILED';receipt['failure']=repr(error);raise
 finally:receipt['finished']=datetime.datetime.now(datetime.timezone.utc).isoformat();save();print('RECEIPT '+str(out/'RECEIPT.json'),flush=True)
if __name__=='__main__':main()
