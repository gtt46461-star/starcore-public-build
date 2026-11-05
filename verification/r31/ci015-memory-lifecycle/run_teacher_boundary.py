from pathlib import Path
import ast,hashlib,json,os,subprocess,tempfile
import argparse
parser=argparse.ArgumentParser(description='Actual125 source lifetime and protected52 golden before/after; separate from full Gradle suite')
parser.add_argument('--out',type=Path,required=True)
args=parser.parse_args()
root=Path(__file__).resolve().parents[3];out=args.out.resolve();out.mkdir(parents=True,exist_ok=False)
support=Path(os.environ.get('R31_CI015_SUPPORT','/workspace/r31-4e-baseline-frozen/classes.jar'));sdk=Path(os.environ['ANDROID_HOME'])/'platforms/android-36/android.jar'
assert hashlib.sha256(support.read_bytes()).hexdigest()=='4e0d9198539a32a7b635c20f2186323f2db81581374628df02ccccd506a690e5'
work=Path(tempfile.mkdtemp(prefix='r31-ci015-teacher-'));platform=work/'platform';sources=work/'sources';host=work/'host'
for p in [platform,sources,host]:p.mkdir()
old=ast.parse((root/'verification/r31/p02/f23-owner-budget/run.py').read_text());adapters=next(ast.literal_eval(n.value) for n in ast.walk(old) if isinstance(n,ast.Assign) and any(isinstance(t,ast.Name) and t.id=='adapters' for t in n.targets))
adapters['Context.java']='package android.content;public class Context{private final android.content.res.AssetManager assets;private boolean retired;public Context(java.io.File root){assets=new android.content.res.AssetManager(root);}public android.content.res.AssetManager getAssets(){if(retired)throw new IllegalStateException("HOST_CONTEXT_RETIRED_MANAGER_IS_STILL_VALID");return assets;}public Context getApplicationContext(){return null;}public void retireR31(){retired=true;}}'
commands=[]
def run(cmd,name):
 cmd=list(map(str,cmd));p=subprocess.run(cmd,cwd=root,capture_output=True,text=True,timeout=180);(out/name).write_text(p.stdout+p.stderr);commands.append({'command':cmd,'exitCode':p.returncode,'log':name});return p
for name,text in adapters.items():(sources/name).write_text(text)
assert run(['javac','-d',platform,*[sources/n for n in adapters]],'platform.log').returncode==0
fixture=root/'verification/r31/ci015-memory-lifecycle/TeacherAssetLifetimeR31Host.java';golden=root/'tools/r31/memory_host/FeatureGoldenR31Host.java';cpp=os.pathsep.join(map(str,[platform,support,sdk]))
assert run(['javac','-sourcepath','','-implicit:none','-cp',cpp,'-d',host,fixture,golden],'fixture-compile.log').returncode==0
results={}
for mode in ['before','after']:
 prod=work/mode;prod.mkdir();source=sources/mode/'LegacyTeacherOwnerR26.java';source.parent.mkdir()
 original=root/'app/src/main/java/com/ethk3/novaonepro/LegacyTeacherOwnerR26.java';source.write_bytes(subprocess.check_output(['git','show','4e395662838a69d3b784eee8f05af11fa94460ac:'+str(original.relative_to(root))],cwd=root) if mode=='before' else original.read_bytes())
 assert run(['javac','-sourcepath','','-implicit:none','-cp',cpp,'-d',prod,source],mode+'-production-compile.log').returncode==0
 cp=os.pathsep.join(map(str,[platform,prod,host,support,sdk]))
 actual=run(['java','-Xmx512m','-cp',cp,'com.ethk3.novaonepro.strategyv2.TeacherAssetLifetimeR31Host',root/'app/src/main/assets'],mode+'-teacher.log')
 g=run(['java','-Xmx512m','-cp',cp,'com.ethk3.novaonepro.strategyv2.FeatureGoldenR31Host',root/'app/src/main/assets/r27/golden_512.bin'],mode+'-golden.log');assert g.returncode==0
 results[mode]={'sourceSha256':hashlib.sha256(source.read_bytes()).hexdigest(),'teacherExitCode':actual.returncode,'golden':json.loads(g.stdout)}
assert results['before']['teacherExitCode']!=0 and results['after']['teacherExitCode']==0
assert 'HOST_CONTEXT_RETIRED_MANAGER_IS_STILL_VALID' in (out/'before-teacher.log').read_text()
assert results['before']['golden']==results['after']['golden'] and len(results['after']['golden'])==52
before=next(json.loads(x[len('BEFORE_RELOAD '):]) for x in (out/'before-teacher.log').read_text().splitlines() if x.startswith('BEFORE_RELOAD '));after=next(json.loads(x[len('PASS '):]) for x in (out/'after-teacher.log').read_text().splitlines() if x.startswith('PASS '))
assert before['originalAudit']==after['originalAudit'] and before['binding']==after['binding'] and before['trainedCodecSha256']==after['trainedCodecSha256']
asset=root/'app/src/main/assets/research/r967_history/R908_125_algorithms.tsv'
r={'scope':'ACTUAL_ASSET_LIFETIME_AND_NUMERIC_ABI_HOST_BOUNDARY;NOT_FULL_SUITE_ANDROID_OR_NATURAL_QUALITY','state':'TARGETED_PASS_WITH_PRESERVED_BASELINE_FAILURE','work':str(work),'supportSha256':hashlib.sha256(support.read_bytes()).hexdigest(),'assetSha256':hashlib.sha256(asset.read_bytes()).hexdigest(),'assetRows':len(asset.read_text().splitlines())-1,'results':results,'after':after,'commands':commands,'explicitGc':False,'fakeProbe':False,'workerHeap':'512MiB','full485':'SEPARATE_GRADLE_EVIDENCE','device':'NOT_RUN','naturalQuality':'NOT_EVALUATED'}
(out/'RESULTS.json').write_text(json.dumps(r,indent=2)+'\n');print(json.dumps({'state':r['state'],'baselineExit':results['before']['teacherExitCode'],'afterExit':results['after']['teacherExitCode'],'assetRows':r['assetRows'],'goldenCount':52,'afterAssertions':after['assertions'],'work':str(work)}))
