#!/usr/bin/env python3
"""Run current P1 production quota, strict migration and tail-recovery host gates.

Uses native SQLite through the repository's transport adapter, not store stubs.
Dependency paths are explicit; receipts hash all directly compiled source and
platform adapters. This is a host contract test, not an Android device test.
"""
import argparse
import datetime
import hashlib
import json
import os
from pathlib import Path
import re
import subprocess


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def support_inventory(root):
    if not root.is_dir():
        raise ValueError('COMPILED_SUPPORT_DIRECTORY_MISSING')
    files=sorted(root.rglob('*.class'))
    if not files:
        raise ValueError('COMPILED_SUPPORT_CLASSES_EMPTY')
    return {p.relative_to(root).as_posix():sha(p)for p in files}


def main():
    cli=argparse.ArgumentParser(description=__doc__)
    for name in ('repo','classes','sdk','jdbc','slf4j','out'):
        cli.add_argument('--'+name,type=Path,required=True)
    a=cli.parse_args()
    for name in ('repo','classes','sdk','jdbc','slf4j','out'):
        setattr(a,name,getattr(a,name).resolve())
    a.out.mkdir(parents=True,exist_ok=False)
    for name in ('platform','production','tests'):(a.out/name).mkdir()
    here=Path(__file__).resolve().parent
    sources=a.repo/'app/src/main/java/com/ethk3/novaonepro'
    production_sources=[sources/(name+'.java') for name in ('LegacyDdlR31','SchemaDomainCoordinatorR926','LegacySchemaCoordinatorR926','PredrawTailRecoveryR32','R36AnalysisHistoryPolicyR364','TargetIssueResolver','Models')]
    production_sources += [sources/'strategyv2'/(name+'.java') for name in ('HypothesisStoreR31','JsonR27','StatsR27')]
    adapters=list((a.repo/'tools/r31/ddl_host').rglob('*.java'))+list((a.repo/'tools/r29/qual_host/shim/android/database/sqlite').glob('SQLite*Exception.java'))
    tests=[here/'tests'/name for name in ('HypothesisQuotaR32Host.java','TailRecoveryProbe.java','CompletedDriftR32Host.java')]
    tests.append(a.repo/'tools/r31/unified_host/StrictMigrationR31Host.java')
    for path in production_sources+adapters+tests+[a.sdk,a.jdbc,a.slf4j]:
        if not path.is_file():raise ValueError('INPUT_MISSING '+str(path))
    watched=production_sources+adapters+tests+[Path(__file__).resolve(),a.sdk,a.jdbc,a.slf4j]
    support=support_inventory(a.classes)
    before={str(p):sha(p)for p in watched};commands=[]
    def run(command,log):
        command=list(map(str,command))
        done=subprocess.run(command,cwd=a.repo,text=True,capture_output=True,timeout=120)
        (a.out/log).write_text(done.stdout+done.stderr)
        commands.append({'command':command,'exitCode':done.returncode,'log':log})
        if done.returncode:raise RuntimeError(log+'\n'+(done.stdout+done.stderr)[-5000:])
        return done.stdout
    # Avoid shell-specific javac wrappers. jdk.compiler executes the actual JDK.
    javac=['java','-m','jdk.compiler/com.sun.tools.javac.Main','-encoding','UTF-8','-source','17','-target','17','-sourcepath','','-implicit:none']
    platform=a.out/'platform';production=a.out/'production';test=a.out/'tests'
    run(javac+['-cp',os.pathsep.join(map(str,[a.sdk,a.classes])),'-d',platform,*adapters],'platform-compile.log')
    cp=os.pathsep.join(map(str,[platform,production,test,a.classes,a.sdk,a.jdbc,a.slf4j]))
    # Compile production against the actual Android public API. The runtime
    # adapter implements the exercised subset; SDK integer constants are inlined.
    production_cp=os.pathsep.join(map(str,[a.sdk,a.classes,platform,production]))
    run(javac+['-cp',production_cp,'-d',production,*production_sources],'production-compile.log')
    run(javac+['-cp',cp,'-d',test,*tests],'tests-compile.log')
    cases=[('quota','com.ethk3.novaonepro.strategyv2.HypothesisQuotaR32Host',['run',a.out/'quota.db'],'HYPOTHESIS_QUOTA_NATIVE_PASS'),
           ('strict','com.ethk3.novaonepro.StrictMigrationR31Host',['run',a.out/'strict.db'],'STRICT_MIGRATION_NATIVE_PASS'),
           ('completed-drift','com.ethk3.novaonepro.CompletedDriftR32Host',[a.out/'drift.db'],'R32_COMPLETED_DRIFT_HOST_PASS'),
           ('tail','com.ethk3.novaonepro.TailRecoveryProbe',[],'PASS_COUNT=9')]
    results=[]
    for case,klass,args,marker in cases:
        output=run(['java','-Xmx384m','-cp',cp,klass,*args],case+'.log')
        if marker not in output:raise ValueError('SUCCESS_MARKER_MISSING '+case)
        match=re.search(r'(?:assertions|PASS_COUNT)=(\d+)',output)
        results.append({'id':'P1-'+case,'state':'PASS_HOST_CURRENT_PRODUCTION','assertions':int(match.group(1)) if match else None,'log':case+'.log'})
        print(output.strip(),flush=True)
    if before!={str(p):sha(p)for p in watched}:raise ValueError('SOURCE_CHANGED_DURING_RUN')
    if support!=support_inventory(a.classes):raise ValueError('COMPILED_SUPPORT_CHANGED_DURING_RUN')
    (a.out/'SUPPORT_CLASSES.json').write_text(json.dumps(support,sort_keys=True,indent=2)+'\n')
    receipt={'state':'PASS_HOST_CURRENT_P1','scope':'ACTUAL_TARGET_PRODUCTION_METHODS_NATIVE_SQLITE_ANDROID_TRANSPORT_ADAPTER',
             'capturedAt':datetime.datetime.now(datetime.timezone.utc).isoformat(),'cases':results,
             'targetSources':{p.relative_to(a.repo).as_posix():sha(p)for p in production_sources},
             'adapters':{p.relative_to(a.repo).as_posix():sha(p)for p in adapters},
             'testSources':{str(p):sha(p)for p in tests},'runnerSha256':sha(Path(__file__).resolve()),
             'dependencies':{'androidJar':sha(a.sdk),'sqliteJdbc':sha(a.jdbc),'slf4jApi':sha(a.slf4j)},
             'compiledSupportDirectory':str(a.classes),'compiledSupportCount':len(support),
             'compiledSupportManifest':'SUPPORT_CLASSES.json','compiledSupportManifestSha256':sha(a.out/'SUPPORT_CLASSES.json'),'commands':commands,
             'Android':'NOT_RUN','device':'NOT_RUN','naturalQuality':'NOT_PROVEN',
             'quotaScope':'real recoverQuotaSliceR32 on four-table synthetic old DB; full service/foundry birth is separate',
             'strictScope':'61 existing strict-migration assertions plus 12 completed-definition-drift assertions',
             'tailScope':'9 production reducer/resolver assertions; network and service scheduling are not measured'}
    (a.out/'RECEIPT.json').write_text(json.dumps(receipt,ensure_ascii=False,indent=2)+'\n')


if __name__=='__main__':main()
