#!/usr/bin/env python3
"""Actual current factory Store/Batch on native SQLite; explicit HOST window model."""
from pathlib import Path
import hashlib,json,os,subprocess,tempfile
ROOT=Path(__file__).resolve().parents[2]
def main():
 out=Path(os.environ.get('R31_FACTORY_TRANSPORT_OUT',ROOT/'app/build/r31-factory-transport'));out.mkdir(parents=True,exist_ok=True)
 classes=ROOT/'app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes';sdk=Path(os.environ['ANDROID_HOME'])/'platforms/android-36/android.jar';jdbc=Path(os.environ.get('SQLITE_JDBC','/tmp/r31-baseline-deps/sqlite-jdbc.jar'))
 sources=[ROOT/'app/src/main/java/com/ethk3/novaonepro/strategyv2'/f for f in ['FactoryCapabilityStoreR31.java','FactoryStateCodecR31.java','FactoryCapabilityExperimentR31.java','ResearchWriteQueueR27.java']]
 receipt={'sourceHead':subprocess.check_output(['git','rev-parse','HEAD'],cwd=ROOT,text=True).strip(),'scope':'ACTUAL_COMPILED_STORE_BATCH_NATIVE_SQLITE; CURSOR_WINDOW_IS_HOST_MODEL; NO_EXPERIMENT_MEASUREMENT','sourceHashes':{str(p.relative_to(ROOT)):hashlib.sha256(p.read_bytes()).hexdigest() for p in sources},'status':'FAILED_OR_INCOMPLETE'}
 with tempfile.TemporaryDirectory(prefix='r31-factory-transport-') as temp:
  work=Path(temp);platform=work/'platform';platform.mkdir();paths=[]
  for source in (ROOT/'tools/r29/qual_host/shim').rglob('*.java'):
   target=work/'sources'/source.relative_to(ROOT/'tools/r29/qual_host/shim');target.parent.mkdir(parents=True,exist_ok=True);text=source.read_text()
   if source.name=='SQLiteDatabase.java':
    anchor='for(int i=1;i<=n;i++)names.add(r.getMetaData().getColumnLabel(i));';assert anchor in text
    old='for (int i = 0; i < n; i++) o[i] = r.getObject(i + 1);';assert old in text
    text=text.replace(old,'for (int i = 0; i < n; i++) {o[i] = r.getObject(i + 1);if(o[i] instanceof String && ((String)o[i]).getBytes(java.nio.charset.StandardCharsets.UTF_8).length>2097152)throw new SQLiteException("HOST_CURSOR_WINDOW_2_MIB");}')
   target.write_text(text);paths.append(target)
  cp=os.pathsep.join(map(str,[platform,classes,jdbc,sdk]));host=ROOT/'tools/r31/unified_host/FactoryTransportR31Host.java'
  commands=[['javac','-encoding','UTF-8','-cp',str(sdk),'-d',str(platform),*map(str,paths)],['javac','-encoding','UTF-8','-cp',cp,'-d',str(platform),str(host)],['java','-Xmx512m','-cp',cp,'com.ethk3.novaonepro.strategyv2.FactoryTransportR31Host']]
  for i,command in enumerate(commands):
   run=subprocess.run(command,cwd=ROOT,capture_output=True,text=True,timeout=180);(out/(str(i)+'.log')).write_text(run.stdout+run.stderr);print(run.stdout+run.stderr,end='');run.check_returncode()
  receipt['status']='PASS_HOST_TRANSPORT_ONLY'
 (out/'RECEIPT.json').write_text(json.dumps(receipt,indent=2)+'\n')
if __name__=='__main__':main()
