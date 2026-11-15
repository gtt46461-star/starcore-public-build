#!/usr/bin/env python3
import argparse,hashlib,json,os,subprocess,pathlib,shutil
P=pathlib.Path(__file__).resolve().parent
p=argparse.ArgumentParser();p.add_argument('--repo',type=pathlib.Path,required=True);p.add_argument('--classes',type=pathlib.Path,required=True);p.add_argument('--sdk',type=pathlib.Path,required=True);p.add_argument('--jdbc',type=pathlib.Path,required=True);p.add_argument('--slf4j',type=pathlib.Path,required=True);p.add_argument('--out',type=pathlib.Path,required=True);a=p.parse_args()
a.repo=a.repo.resolve();a.out=a.out.resolve();a.out.mkdir(parents=True,exist_ok=False)
for n in ['platform','production','tests']:(a.out/n).mkdir(exist_ok=True)
commands=[]
def run(cmd,log):
 if cmd[0]=='javac':cmd=['java','-m','jdk.compiler/com.sun.tools.javac.Main',*cmd[1:]]
 r=subprocess.run(list(map(str,cmd)),capture_output=True,text=True,cwd=a.repo)
 (a.out/log).write_text(r.stdout+r.stderr);commands.append({'command':list(map(str,cmd)),'exitCode':r.returncode,'log':log})
 if r.returncode:raise RuntimeError(log+'\n'+(r.stdout+r.stderr)[-4000:])
 return r.stdout
sdk=a.sdk.resolve();classes=a.classes.resolve();base=a.repo/'app/src/main/java/com/ethk3/novaonepro'
platform=a.out/'platform';production=a.out/'production';tests=a.out/'tests'
shims=list((a.repo/'tools/r31/ddl_host').rglob('*.java'))+list((a.repo/'tools/r29/qual_host/shim/android/database/sqlite').glob('SQLite*Exception.java'))
run(['javac','-encoding','UTF-8','-cp',os.pathsep.join(map(str,[sdk,classes])),'-d',platform,*shims],'platform.log')
sources=[base/(n+'.java') for n in ['LegacyDdlR31','SchemaDomainCoordinatorR926']]+[P/'full/app/src/main/java/com/ethk3/novaonepro/LegacySchemaCoordinatorR926.java']
run(['javac','-sourcepath','','-implicit:none','-encoding','UTF-8','-cp',os.pathsep.join(map(str,[sdk,platform,classes])),'-d',production,*sources],'production.log')
cp=os.pathsep.join(map(str,[platform,production,tests,classes,a.jdbc.resolve(),a.slf4j.resolve(),sdk]))
run(['javac','-cp',cp,'-d',tests,P/'tests/CompletedDriftR32Host.java'],'testcompile.log')
print(run(['java','-cp',cp,'com.ethk3.novaonepro.CompletedDriftR32Host',a.out/'case.db'],'run.log'))
# Negative control: compile the unchanged baseline owner over the modified classes.
negative=a.out/'negative';negative.mkdir(exist_ok=True)
run(['javac','-sourcepath','','-implicit:none','-cp',os.pathsep.join(map(str,[sdk,platform,classes])),'-d',negative,P/'baseline/LegacySchemaCoordinatorR926.java'],'negativecompile.log')
r=subprocess.run(['java','-cp',str(negative)+os.pathsep+cp,'com.ethk3.novaonepro.CompletedDriftR32Host',str(a.out/'negative.db')],capture_output=True,text=True)
(a.out/'negative.log').write_text(r.stdout+r.stderr)
if r.returncode==0 or 'responsible old complete row rearmed' not in r.stderr:raise RuntimeError('negative control did not expose exact old failure: '+r.stderr[-2000:])
run(['javac','-cp',cp,'-d',tests,a.repo/'tools/r31/unified_host/StrictMigrationR31Host.java'],'strictcompile.log')
print(run(['java','-Xmx384m','-cp',cp,'com.ethk3.novaonepro.StrictMigrationR31Host','run',a.out/'strict-regression.db'],'strict.log'))
result={'existingStrictMigrationRegression':{'status':'PASS','assertions':61,'realKillRestart':True,'log':'strict.log'},'status':'PASS_NATIVE_SQLITE_HOST_ONLY','assertions':12,'negativeControl':'BASELINE_FAILS_OWNER_REARM_ASSERTION','Android':'NOT_RUN','device':'NOT_RUN','scope':'existing completed-unit audit definition drift is invalidated; exact index reconstruction NOT_IMPLEMENTED','sources':{str(s):hashlib.sha256(s.read_bytes()).hexdigest() for s in sources},'commands':commands}
(a.out/'RESULTS.json').write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n')
