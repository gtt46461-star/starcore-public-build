#!/usr/bin/env python3
"""Test the deployed Java kernels, independent Python parity, and production SQLite DDL."""
from pathlib import Path
import json, os, subprocess, sys, time, shutil
from pure_java_sources import select_pure_strategy_sources
ROOT=Path(__file__).resolve().parents[1]
HOST=ROOT/'tools/strategy_su1'
REPORT=ROOT/'build/strategy_su1-verification.json'

def main():
    results=[]
    parity_dir=ROOT/'build/su1-generated-parity'
    parity_dir.mkdir(parents=True,exist_ok=True)
    verification_env=dict(os.environ, SU1_PARITY_OUTPUT_DIR=str(parity_dir))
    def run(name,args,cwd=ROOT):
        start=time.monotonic();p=subprocess.run(list(map(str,args)),cwd=cwd,text=True,stdout=subprocess.PIPE,stderr=subprocess.STDOUT,env=verification_env)
        results.append({'name':name,'returncode':p.returncode,'seconds':round(time.monotonic()-start,3),'output':p.stdout})
        print(name, 'PASS' if p.returncode==0 else 'FAIL',flush=True)
        if p.returncode:print(p.stdout,flush=True)
        return p.returncode==0
    okay=run('schema_and_catalog_reproducibility',[sys.executable,'scripts/generate_strategy_su1_schema.py','--check'])
    okay=run('independent_atom_golden',[sys.executable,HOST/'scripts/build_atom_parity.py']) and okay
    okay=run('independent_training_golden',[sys.executable,HOST/'scripts/build_training_parity.py']) and okay
    okay=run('independent_relgraph_golden',[sys.executable,HOST/'scripts/build_relgraph_parity.py']) and okay
    classes=ROOT/'build/su1-host-classes'
    if classes.exists():shutil.rmtree(classes)
    classes.mkdir(parents=True,exist_ok=True)
    prod=ROOT/'app/src/main/java/com/ethk3/novaonepro/strategyv2'
    sources,source_selection=select_pure_strategy_sources(prod)
    sources += [ROOT / 'app/build/generated/source/r967/com/ethk3/novaonepro/ResearchSemanticsGeneratedR967.java']
    r952=ROOT/'app/src/main/java/com/ethk3/novaonepro/r952'
    sources += [r952/n for n in ['AtomEvaluatorCP4.java','AtomWindowCP4.java','MemoryBudgetCP4.java','MatrixMetricsCP11.java','StrictCanonicalJson.java']]
    sources += list((HOST/'reference/java/com/ethk3/novaonepro/strategyv2').glob('*Test.java'))
    compiled=run('deployed_java_compile',['java','com.sun.tools.javac.Main','-source','17','-target','17','-d',classes]+sources)
    okay=compiled and okay
    if compiled:
        for name in ['ProgramTreeSU1Test','TypedProgramTest','PairedEvidenceTest','NullBaselineTest','ProductionKernelSU1Test','AdvancedContractsSU1Test','TrainingParitySU1Test','AuditControlsSU1Test','RelGraphContractsSU1Test','RelGraphParitySU1Test','ModelAblationsSU1Test','ResearchFailureSU1Test','TrainingFidelitySU1Test','ProductionR13Test']:
            okay=run(name,['java','-Dsu1.parity.dir='+str(parity_dir),'-cp',classes,'com.ethk3.novaonepro.strategyv2.'+name],HOST) and okay
    okay=run('python_schema_learning_and_null_suites',[sys.executable,'-m','unittest','discover','-s','reference/python','-p','test_*.py','-v'],HOST) and okay
    okay=run('python_replay_and_creation_suites',[sys.executable,'-m','unittest','discover','-s','reference/replay','-p','test_*.py','-v'],HOST) and okay
    REPORT.parent.mkdir(exist_ok=True);REPORT.write_text(json.dumps({'status':'PASS' if okay else 'FAIL','scope':'deployed pure Java source + independent Python and SQLite; Android/physical device and natural prediction qualification are separate','source_selection':source_selection,'checks':results},ensure_ascii=False,indent=2)+'\n')
    return 0 if okay else 1
if __name__=='__main__':raise SystemExit(main())
