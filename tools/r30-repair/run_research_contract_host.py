"""R31 K07 research result contract and 16 logical views on original native SQLite shim. No device/quality claim."""
import hashlib,json,pathlib,os,subprocess,tempfile
root=pathlib.Path(__file__).resolve().parents[2];classes=pathlib.Path(os.environ.get('R31_HOST_CLASSES',root/'app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes'))
sdk=pathlib.Path(os.environ['ANDROID_HOME'])/'platforms/android-36/android.jar';jdbc=pathlib.Path(os.environ.get('SQLITE_JDBC','/workspace/work/r29/deps/sqlite-jdbc.jar'))
def run(args):subprocess.run(list(map(str,args)),cwd=root,check=True)
with tempfile.TemporaryDirectory(prefix='r31-research-contract-host-') as temporary:
 out=pathlib.Path(temporary);shim=out/'shim';shim.mkdir();test=out/'test';test.mkdir();production=out/'production';production.mkdir();log=out/'Log.java';log.write_text('package android.util;public final class Log{public static int w(String t,String v){return 0;}public static int w(String t,String v,Throwable e){return 0;}public static int e(String t,String v){return 0;}}')
 run(['javac','-nowarn','-encoding','UTF-8','-cp',sdk,'-d',shim,*sorted((root/'tools/r29/qual_host/shim').rglob('*.java')),log])
 cp=os.pathsep.join(map(str,[shim,classes,sdk]))
 reader=root/'app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchTableAdaptersR30R1.java'
 before=hashlib.sha256(reader.read_bytes()).hexdigest()
 production_sources=[reader]+[root/'app/src/main/java/com/ethk3/novaonepro/strategyv2'/(name+'.java') for name in ['ResearchContextR30R1','ResearchResultContractR31','ResearchConsumptionStoreR31','ResearchStoreR30R1','EvolutionLoopR27']]
 source_hashes={str(p.relative_to(root)):hashlib.sha256(p.read_bytes()).hexdigest() for p in production_sources}
 run(['javac','-sourcepath','','-implicit:none','-nowarn','-encoding','UTF-8','-cp',cp,'-d',production,*production_sources])
 cp=os.pathsep.join(map(str,[shim,production,classes,sdk]))
 run(['javac','-nowarn','-encoding','UTF-8','-cp',cp,'-d',test,root/'tools/r30-repair/qual_host/R31ResearchContractHost.java',root/'tools/r30-repair/qual_host/R31ResearchTableBudgetHost.java',root/'tools/r30-repair/qual_host/R31ResearchConsumptionHost.java'])
 cp=os.pathsep.join(map(str,[shim,test,production,classes,jdbc,sdk]))
 for name in ["R31ResearchTableBudgetHost","R31ResearchContractHost","R31ResearchConsumptionHost"]:run(['java','-Xmx768m','-cp',cp,'com.ethk3.novaonepro.strategyv2.'+name])
 assert {str(p.relative_to(root)):hashlib.sha256(p.read_bytes()).hexdigest() for p in production_sources}==source_hashes,'PRODUCTION_SOURCE_CHANGED_DURING_NATIVE_RUN'
 print('R31_TABLE_READER_SOURCE_BINDING '+json.dumps({'sourceHead':subprocess.check_output(['git','rev-parse','HEAD'],cwd=root,text=True).strip(),'readerSha256':before,'productionSourceHashes':source_hashes,'freshProductionCompilation':True,'nativeSQLite':True,'Android':'NOT_RUN','naturalQuality':'NOT_PROVEN'}))
