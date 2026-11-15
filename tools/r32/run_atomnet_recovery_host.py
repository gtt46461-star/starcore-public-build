#!/usr/bin/env python3
"""Run R32.4 inherited AtomNet recovery contracts with actual Java production sources/native SQLite.

The --classes dependency must be the compiled whole candidate application (directory
or JAR). This runner recompiles the five reviewed production files and adapts only
Android platform APIs for host execution. It is not an Android/device/quality test.
No network dependency download or credential access occurs in this runner.
"""
from __future__ import annotations
import argparse
from datetime import datetime, timezone
import hashlib
import importlib.util
import json
import os
from pathlib import Path
import shutil
import subprocess
import sys

PRODUCTION = ["AtomNetPublicationR31", "AtomNetStoreR30", "AtomNetRuntimeR30", "AtomNetSessionR30", "RuntimeR27"]
PACKAGE = "com.ethk3.novaonepro.strategyv2."

def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()

def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--classes', type=Path, required=True)
    parser.add_argument('--android-jar', type=Path, required=True)
    parser.add_argument('--sqlite-jdbc', type=Path, required=True)
    parser.add_argument('--slf4j-api', type=Path)
    parser.add_argument('--out', type=Path, required=True)
    parser.add_argument('--java', default='java')
    args=parser.parse_args()
    root=Path(__file__).resolve().parents[2]
    out=args.out.resolve()
    out.mkdir(parents=True, exist_ok=False)
    receipt={'status':'IN_PROGRESS','scope':'ACTUAL_REVIEWED_PRODUCTION_SOURCES_NATIVE_SQLITE_HOST','device':'NOT_RUN','naturalQuality':'NOT_EVALUATED','coldArchiveReclamation':'NOT_EVALUATED_BY_THIS_RECOVERY_RUNNER','startedAtUtc':datetime.now(timezone.utc).isoformat(),'commands':[]}
    def save():
        (out/'RECEIPT.json').write_text(json.dumps(receipt,ensure_ascii=False,indent=2)+'\n')
    def run(command,log):
        completed=subprocess.run(list(map(str,command)),cwd=root,capture_output=True,text=True,timeout=90)
        (out/log).write_text(completed.stdout+completed.stderr)
        receipt['commands'].append({'argv':list(map(str,command)),'log':log,'exitCode':completed.returncode})
        save()
        if completed.returncode:
            raise RuntimeError(log+': '+(completed.stdout+completed.stderr)[-4000:])
        return completed.stdout
    try:
        for path in [args.classes,args.android_jar,args.sqlite_jdbc]+([args.slf4j_api] if args.slf4j_api else []):
            if not path.exists():
                raise FileNotFoundError(path)
        support=root/'tools/r31/run_atomnet_publication_host.py'
        spec=importlib.util.spec_from_file_location('r31_atomnet_host_support',support)
        module=importlib.util.module_from_spec(spec);spec.loader.exec_module(module)
        platform_sources=[]
        for name,content in module.platform_sources().items():
            if not name.startswith('android/'):
                raise ValueError('only Android platform boundaries may be adapted')
            path=out/'platform_sources'/name;path.parent.mkdir(parents=True,exist_ok=True);path.write_text(content);platform_sources.append(path)
        production=[root/'app/src/main/java/com/ethk3/novaonepro/strategyv2'/(name+'.java') for name in PRODUCTION]
        fixture=Path(__file__).with_name('AtomNetRecoveryR32Host.java')
        receipt['productionSourceSha256']={str(path.relative_to(root)):sha(path) for path in production}
        receipt['fixtureSha256']=sha(fixture)
        receipt['platformAdapterGeneratorSha256']=sha(support)
        receipt['platformSourceSha256']={str(path.relative_to(out)):sha(path) for path in platform_sources}
        receipt['dependencyInputs']={name:{'path':str(path.resolve()),'sha256':sha(path) if path.is_file() else None,'kind':'DIRECTORY' if path.is_dir() else 'FILE'} for name,path in [('wholeApplicationClasses',args.classes),('androidJar',args.android_jar),('sqliteJdbc',args.sqlite_jdbc)]+([('slf4jApi',args.slf4j_api)] if args.slf4j_api else [])}
        java=shutil.which(args.java) or args.java
        run([java,'-version'],'java_version.log')
        compiler=[java,'-m','jdk.compiler/com.sun.tools.javac.Main','-encoding','UTF-8','-sourcepath','','-implicit:none']
        classpath=os.pathsep.join(map(str,[args.classes.resolve(),args.android_jar.resolve()]))
        run(compiler+['-cp',classpath,'-d',out/'platform']+platform_sources,'platform_compile.log')
        run(compiler+['-cp',classpath,'-d',out/'production']+production,'production_compile.log')
        compile_cp=os.pathsep.join(map(str,[out/'platform',out/'production',args.classes.resolve(),args.android_jar.resolve(),args.sqlite_jdbc.resolve()]))
        run(compiler+['-cp',compile_cp,'-d',out/'tests',fixture],'host_compile.log')
        cp=os.pathsep.join(map(str,[out/'platform',out/'production',out/'tests',args.classes.resolve(),args.android_jar.resolve(),args.sqlite_jdbc.resolve()]+([args.slf4j_api.resolve()] if args.slf4j_api else [])))
        raw=run([java,'-Xmx768m','-cp',cp,PACKAGE+'AtomNetRecoveryR32Host'],'host_run.log')
        result=json.loads(raw.strip().splitlines()[-1])
        if result.get('status')!='PASS' or result.get('assertions',0)<71:
            raise AssertionError('R32 recovery coverage/result missing')
        if receipt['productionSourceSha256']!={str(path.relative_to(root)):sha(path) for path in production}:
            raise AssertionError('production sources changed while testing')
        receipt.update({'status':'PASS','result':result,'finishedAtUtc':datetime.now(timezone.utc).isoformat()})
        save();print(json.dumps({'status':'PASS','assertions':result['assertions'],'receipt':str(out/'RECEIPT.json')}));return 0
    except Exception as error:
        receipt.update({'status':'FAILED_OR_BLOCKED','failureType':type(error).__name__,'failure':str(error),'finishedAtUtc':datetime.now(timezone.utc).isoformat()});save();print(str(error),file=sys.stderr);return 1

if __name__=='__main__':
    sys.exit(main())
