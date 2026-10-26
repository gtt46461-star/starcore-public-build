"""Run actual HOST checks; never edits source archives or claims Android success."""
from pathlib import Path
import sys,subprocess,json,time,tempfile,shutil,re,os,importlib.util
ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'evidence/host_run';OUT.mkdir(parents=True,exist_ok=True)
env=dict(os.environ,PYTHONDONTWRITEBYTECODE='1')
selected=set(sys.argv[1:])
prior=ROOT/'evidence/validation_summary.json'
results=json.loads(prior.read_text())['runs'] if selected and prior.exists() else []
if selected and prior.exists():
    shutil.copy2(prior,ROOT/'evidence/validation_initial_attempt.json')
def wanted(group):return not selected or group in selected
def run(label,cmd,cwd=ROOT):
    start=time.monotonic();r=subprocess.run(cmd,cwd=cwd,env=env,text=True,stdout=subprocess.PIPE,stderr=subprocess.STDOUT)
    (OUT/(label+'.log')).write_text(r.stdout)
    m=re.search(r'Ran (\d+) tests?',r.stdout)
    row={'name':label,'status':'PASS' if r.returncode==0 else 'FAIL','returncode':r.returncode,'seconds':round(time.monotonic()-start,3),'python_tests':int(m[1]) if m else None,'command':[str(x) for x in cmd]}
    results[:]=[x for x in results if x['name']!=label];results.append(row);print(json.dumps(row,ensure_ascii=False),flush=True)
    if r.returncode:print(r.stdout[-5000:],flush=True)
    return r.returncode==0
if wanted('python'):
    run('r26_python',[sys.executable,'-m','unittest','discover','-s','tests','-v'])
    run('source_a_r25_python',[sys.executable,'-m','unittest','discover','-s',str(ROOT/'sources/plan_a/upgrade_r25/tests'),'-v'])
    run('source_b_r25_python',[sys.executable,'-m','unittest','discover','-s',str(ROOT/'sources/plan_b/tests'),'-v'])
    run('shared_r24_python',[sys.executable,'-m','unittest','discover','-s',str(ROOT/'sources/plan_b/baseline_R24/tests'),'-v'])
if wanted('r23'):
    if importlib.util.find_spec('torch'):
        run('shared_r23_python',[sys.executable,'-m','unittest','discover','-s',str(ROOT/'sources/plan_b/baseline_R23/tests'),'-v'])
    else:
        results[:]=[x for x in results if x['name']!='shared_r23_python']
        results.append({'name':'shared_r23_neural','status':'NOT_RUN','required':False,'reason':'UNCHANGED_LEGACY_TORCH_DEPENDENCY_NOT_INSTALLED; original evidence preserved, not counted as rerun'})
        run('shared_r23_durable',[sys.executable,'-m','unittest','discover','-s',str(ROOT/'sources/plan_b/baseline_R23/tests'),'-p','test_durable_learning.py','-v'])
compiler_tmp=None
if wanted('java'):
    results[:]=[x for x in results if x['name']!='java']
    if not shutil.which('javac') and shutil.which('java'):
        probe=subprocess.run(['java','--module','jdk.compiler/com.sun.tools.javac.Main','-version'],stdout=subprocess.PIPE,stderr=subprocess.STDOUT)
        if probe.returncode==0:
            compiler_tmp=tempfile.TemporaryDirectory(prefix='r26-javac-');wrapper=Path(compiler_tmp.name)/'javac'
            wrapper.write_text('#!/bin/sh\nexec java --module jdk.compiler/com.sun.tools.javac.Main "$@"\n');wrapper.chmod(0o755)
            env['PATH']=compiler_tmp.name+os.pathsep+env['PATH']
if wanted('java') and shutil.which('javac',path=env['PATH']) and shutil.which('java'):
    with tempfile.TemporaryDirectory(prefix='r26-host-') as tmp:
        if run('r26_javac',['javac','-encoding','UTF-8','-d',tmp,str(ROOT/'code/ContinuousSchedulerR26.java'),str(ROOT/'tests/ContinuousSchedulerR26Test.java')]):
            run('r26_java',['java','-cp',tmp,'ContinuousSchedulerR26Test'])
    run('source_a_java',[sys.executable,str(ROOT/'sources/plan_a/upgrade_r25/code/run_java_contracts.py')])
    run('shared_r24_java',[sys.executable,str(ROOT/'sources/plan_b/baseline_R24/code/run_java_regression.py')])
    with tempfile.TemporaryDirectory(prefix='r26-original-') as tmp:
        src=Path(tmp)/'src';src.mkdir();cls=Path(tmp)/'classes';cls.mkdir()
        origin=ROOT/'sources/plan_b/baseline_R24/evidence/source_verified/app/src/main/java/com/ethk3/novaonepro/strategyv2'
        for p in origin.glob('*.java'):
            if p.name=='ProgramModelSU2.java' or re.search(r'^import (android|com\.ethk3\.novaonepro\.[A-Z])',p.read_text(),re.M):continue
            shutil.copy2(p,src/p.name)
        shutil.copy2(ROOT/'sources/plan_b/tests/R25LegacyDefectReproducer.java',src/'R25LegacyDefectReproducer.java')
        if run('source_b_legacy_compile',['javac','-encoding','UTF-8','-d',str(cls)]+[str(p) for p in src.glob('*.java')]):
            run('source_b_legacy_defects',['java','-cp',str(cls),'com.ethk3.novaonepro.strategyv2.R25LegacyDefectReproducer'])
elif wanted('java'):results.append({'name':'java','status':'NOT_RUN','reason':'JDK_COMPILER_MISSING'})
if compiler_tmp:compiler_tmp.cleanup()
summary={'scope':'HOST_CONTRACTS_AND_LEGACY_REGRESSIONS_ONLY','runs':results,'all_required_pass':all(x['status']=='PASS' for x in results if x.get('required',True)),'all_including_optional_pass':all(x['status']=='PASS' for x in results),'python_tests_passed':sum(x.get('python_tests') or 0 for x in results if x['status']=='PASS'),'android':'NOT_RUN','device':'NOT_RUN','new_prospective_quality':'NOT_RUN'}
(ROOT/'evidence/validation_summary.json').write_text(json.dumps(summary,ensure_ascii=False,indent=2)+'\n')
print(json.dumps(summary,ensure_ascii=False,indent=2))
sys.exit(0 if summary['all_required_pass'] else 1)
