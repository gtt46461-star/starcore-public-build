#!/usr/bin/env python3
"""Fresh actual hypothesis/Foundry projection sources, native SQLite and process
kill/reopen. Actual ModelLive outputs are synthetic controls, never Android or
natural-quality evidence. No prior pass files are consulted."""
from pathlib import Path
import argparse,hashlib,json,os,subprocess,tempfile,uuid
ROOT=Path(__file__).resolve().parents[2]
def digest(paths,base):
 h=hashlib.sha256()
 for path in sorted(paths):h.update(str(path.relative_to(base)).encode()+b'\0'+hashlib.sha256(path.read_bytes()).digest())
 return h.hexdigest()
def main():
 parser=argparse.ArgumentParser();parser.add_argument('--out',type=Path);args=parser.parse_args()
 out=args.out or Path(os.environ.get('R31_HYPOTHESIS_OUT',ROOT/'app/build/r31-hypothesis-store'));out.mkdir(parents=True,exist_ok=True)
 source=ROOT/'app/src/main/java/com/ethk3/novaonepro/strategyv2'
 sources=[source/name for name in ['ResearchHypothesisR31.java','HypothesisStoreR31.java','FoundryCampaignR26.java','FailureDirectedResearchR26.java']]
 host=ROOT/'tools/r31/unified_host/HypothesisStoreR31Host.java'
 shims=list((ROOT/'tools/r29/qual_host/shim').rglob('*.java'))
 classes=Path(os.environ.get('R31_HYPOTHESIS_SUPPORT_CLASSES',ROOT/'app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes'))
 sdk=Path(os.environ['ANDROID_HOME'])/'platforms/android-36/android.jar';jdbc=Path(os.environ.get('SQLITE_JDBC','/tmp/r31-baseline-deps/sqlite-jdbc.jar'))
 assert classes.is_dir() and sdk.is_file() and jdbc.is_file(),'COMPILED_SUPPORT_OR_NATIVE_DEPENDENCY_UNAVAILABLE'
 before=digest(sources+[host,*shims],ROOT);support=digest(classes.rglob('*.class'),classes)
 receipt={'invocationId':str(uuid.uuid4()),'sourceHead':subprocess.check_output(['git','rev-parse','HEAD'],cwd=ROOT,text=True).strip(),'sourceTree':subprocess.check_output(['git','rev-parse','HEAD^{tree}'],cwd=ROOT,text=True).strip(),'dirty':bool(subprocess.check_output(['git','status','--porcelain'],cwd=ROOT,text=True).strip()),'sourceHashes':{str(p.relative_to(ROOT)):hashlib.sha256(p.read_bytes()).hexdigest() for p in sources},'sourceAndHarnessSha256':before,'compiledSupportSha256':support,'scope':'ACTUAL_PRODUCTION_STORE_AND_FOUNDRY_FINISH_PROJECTION_NATIVE_SQLITE; ACTUAL_MODELLIVE_SYNTHETIC_OUTPUTS; NOT_ANDROID_OR_NATURAL','status':'FAILED_OR_INCOMPLETE','executed':[]}
 (out/'RECEIPT.json').write_text(json.dumps(receipt,indent=2)+'\n')
 with tempfile.TemporaryDirectory(prefix='r31-hypothesis-') as temp:
  work=Path(temp);platform=work/'platform';product=work/'product';platform.mkdir();product.mkdir()
  cp=os.pathsep.join(map(str,[product,platform,classes,jdbc,sdk]))
  commands=[('compile_platform',['javac','-encoding','UTF-8','-cp',str(sdk),'-d',str(platform),*map(str,shims)]),('compile_actual_sources_and_host',['javac','-encoding','UTF-8','-cp',cp,'-d',str(product),*map(str,sources),str(host)]),('native_sqlite_kill_restart_and_actual_model_select',['java','-Xmx768m','-cp',cp,'com.ethk3.novaonepro.strategyv2.HypothesisStoreR31Host',str(work/'execution')])]
  for index,(name,command) in enumerate(commands):
   run=subprocess.run(command,cwd=ROOT,capture_output=True,text=True,timeout=240);(out/(str(index)+'-'+name+'.log')).write_text(run.stdout+run.stderr)
   receipt['executed'].append({'name':name,'exitCode':run.returncode});print(run.stdout+run.stderr,end='');(out/'RECEIPT.json').write_text(json.dumps(receipt,indent=2)+'\n');run.check_returncode()
 assert digest(sources+[host,*shims],ROOT)==before,'SOURCE_OR_HARNESS_CHANGED_DURING_EXECUTION'
 assert digest(classes.rglob('*.class'),classes)==support,'COMPILED_SUPPORT_CHANGED_DURING_EXECUTION'
 receipt.update(status='PASS_NATIVE_PROTOCOL_ONLY',sourcesAndCompiledSupportUnchanged=True)
 (out/'RECEIPT.json').write_text(json.dumps(receipt,indent=2)+'\n')
if __name__=='__main__':main()
