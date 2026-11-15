#!/usr/bin/env python3
"""Compile current production sources and execute GC/CAS/root contracts on native SQLite.

This is a host contract rehearsal, not an Android APK, device, training-quality,
or natural-data acceptance. No production class is replaced with a business stub.
"""
import argparse
import hashlib
import importlib.util
import json
import os
from pathlib import Path
import subprocess
import sys
import time

DEPENDENCIES = {
    'android.jar': 'd9eb9da824d9e247a352f570f01e1169e725b2954bca9e283a71786c59b59f9a',
    'sqlite-jdbc-3.46.1.3.jar': '4a4832720a65eaf7f4d6fd7ede52087b994dc5633c076f9e994dc0c8b4b0b4fa',
    'slf4j-api-2.0.13.jar': 'e7c2a48e8515ba1f49fa637d57b4e2f590b3f5bd97407ac699c3aa5efb1204a9',
}


def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def run(command, destination, timeout=300):
    started = time.monotonic()
    result = subprocess.run([str(x) for x in command], stdout=subprocess.PIPE,
                            stderr=subprocess.STDOUT, text=True, timeout=timeout)
    destination.write_text(result.stdout, encoding='utf-8')
    if result.returncode:
        raise RuntimeError(f'Command exited {result.returncode}; see {destination}')
    return {'argv': [str(x) for x in command], 'log': destination.name,
            'exit_code': result.returncode, 'seconds': round(time.monotonic()-started, 3)}


def main():
    cli = argparse.ArgumentParser(description=__doc__)
    cli.add_argument('--repo', required=True, type=Path)
    cli.add_argument('--classes', type=Path, help='This exact stage full production typecheck output; not a later batch')
    cli.add_argument('--android-jar', required=True, type=Path)
    cli.add_argument('--jdbc-jar', required=True, type=Path)
    cli.add_argument('--slf4j-jar', required=True, type=Path)
    cli.add_argument('--package', type=Path, help='Only for optional full compilation when --classes is omitted')
    cli.add_argument('--out', required=True, type=Path, help='New or empty output directory')
    args = cli.parse_args()
    repo,out=args.repo.resolve(),args.out.resolve()
    jars={'android.jar':args.android_jar.resolve(),'sqlite-jdbc-3.46.1.3.jar':args.jdbc_jar.resolve(),'slf4j-api-2.0.13.jar':args.slf4j_jar.resolve()}
    if out.exists() and any(out.iterdir()):
        raise ValueError('Use an empty output directory; old PASS logs are not reused')
    out.mkdir(parents=True, exist_ok=True)
    dependencies=[]
    for name, expected in DEPENDENCIES.items():
        path=jars[name]
        if not path.is_file() or digest(path)!=expected:
            raise ValueError(f'Dependency identity mismatch or absent: {path}')
        dependencies.append({'name': name, 'bytes': path.stat().st_size, 'sha256': expected})
    fixture=Path(__file__).resolve().with_name('GcClosureR324Host.java')
    provider_path=repo/'tools/r31/run_atomnet_publication_host.py'
    spec=importlib.util.spec_from_file_location('gc_platform_source_provider',provider_path)
    provider=importlib.util.module_from_spec(spec);spec.loader.exec_module(provider);provider.ROOT=repo
    adapter_sources=provider.platform_sources();adapters=[]
    for name,content in sorted(adapter_sources.items()):
        if not name.startswith('android/') or '..' in Path(name).parts:
            raise ValueError('Platform adapter must never replace an application business class')
        path=out/'platform_sources'/name;path.parent.mkdir(parents=True,exist_ok=True);path.write_text(content,encoding='utf-8');adapters.append(path)
    production=sorted((repo/'app/src/main/java').rglob('*.java'))
    if not production or not adapters or not fixture.is_file():
        raise ValueError('Production source, Android transport adapters, or host fixture missing')
    pins={str(p.relative_to(repo)):digest(p) for p in production}
    adapter_pins={str(p.relative_to(out)):digest(p) for p in adapters}
    commands=[]
    started=time.monotonic()
    commands.append(run(['java','-version'],out/'java_version.log'))
    production_classes=args.classes.resolve() if args.classes else out/'production'
    if args.classes:
        if not production_classes.is_dir() or not (production_classes/'com/ethk3/novaonepro/strategyv2/CheckpointArtifactsR26.class').is_file():
            raise ValueError('The current stage full production classes are missing')
        if (production_classes/'android').exists():
            raise ValueError('Full production output must not contain platform transport overrides')
    else:
        if not args.package:raise ValueError('Supply --classes, or --package for a fresh full production typecheck')
        commands.append(run([sys.executable,args.package.resolve()/'tools/compile_rehearsal.py','--repo',repo,
            '--android-jar',jars['android.jar'],'--expected-sdk-sha256',DEPENDENCIES['android.jar'],
            '--out',production_classes],out/'production_compile.log'))
    class_pins={str(p.relative_to(production_classes)):digest(p) for p in sorted(production_classes.rglob('*.class'))}
    compiler=['java','-m','jdk.compiler/com.sun.tools.javac.Main','-encoding','UTF-8','-proc:none']
    commands.append(run(compiler+['-cp',os.pathsep.join(str(jars[n]) for n in ['android.jar','sqlite-jdbc-3.46.1.3.jar']),'-d',out/'platform']+adapters,
                        out/'platform_compile.log'))
    classpath=os.pathsep.join(str(p) for p in [out/'platform',production_classes,jars['android.jar'],
        jars['sqlite-jdbc-3.46.1.3.jar'],jars['slf4j-api-2.0.13.jar']])
    recompile_names=['CheckpointArtifactsR26.java','CheckpointGcReachabilityR324.java','CheckpointGcStoreR31.java',
                     'CheckpointReferencesR26.java','CheckpointScratchGcR31.java','ColdCheckpointR324.java']
    recompiled=[repo/'app/src/main/java/com/ethk3/novaonepro/strategyv2'/name for name in recompile_names]
    commands.append(run(compiler+['-cp',classpath,'-d',out/'host']+recompiled+[fixture],out/'host_compile.log'))
    runtime_cp=os.pathsep.join(str(p) for p in [out/'platform',out/'host',production_classes,jars['android.jar'],
        jars['sqlite-jdbc-3.46.1.3.jar'],jars['slf4j-api-2.0.13.jar']])
    commands.append(run(['java','-cp',runtime_cp,'com.ethk3.novaonepro.strategyv2.GcClosureR324Host'],out/'host_run.log'))
    results=[json.loads(line) for line in (out/'host_run.log').read_text().splitlines() if line.startswith('{')]
    if len(results)!=1 or results[0].get('status')!='PASS' or results[0].get('assertions',0)<338 or len(results[0].get('scenarios',[]))<20:
        raise RuntimeError('Missing or incomplete current execution result')
    if pins!={str(p.relative_to(repo)):digest(p) for p in production}:
        raise RuntimeError('Production source changed while the rehearsal ran')
    if class_pins!={str(p.relative_to(production_classes)):digest(p) for p in sorted(production_classes.rglob('*.class'))}:
        raise RuntimeError('Provided production class inputs changed while rehearsal ran')
    receipt={'status':'PASS_HOST_GC_CONTRACTS_ONLY', 'host_result':results[0],
             'elapsed_seconds':round(time.monotonic()-started,3), 'dependencies':dependencies,
             'production_sources':pins,'adapter_sources':adapter_pins,'fixture_sha256':digest(fixture),
             'platform_provider':{'path':str(provider_path.relative_to(repo)),'sha256':digest(provider_path)},
             'production_class_inputs':class_pins,
             'production_class_mode':'CALLER_SUPPLIED_CURRENT_STAGE_FULL_TYPECHECK' if args.classes else 'CURRENT_SOURCE_FULL_TYPECHECK',
             'recompiled_production_paths':[str(p.relative_to(repo)) for p in recompiled],
             'commands':commands,
             'scope':'Actual production Java GC/CAS/reference entry points and SQL on native sqlite-jdbc; real filesystem IO/fsync and test-only platform transport',
             'excluded':['Android resource/aapt/dex/APK/signing/device acceptance','Android CursorWindow/ANR/low-memory timing',
                         'training or win-rate quality','natural data or real order admission','full 17000 physical artifacts (the 17000 case is committed-metadata scaling only)',
                         'arbitrary-size cyclic-component collection (more than 64 nodes or 32MiB raw stays intact and is reported)']}
    (out/'RECEIPT.json').write_text(json.dumps(receipt,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    print(json.dumps({'status':receipt['status'],'assertions':results[0]['assertions'],
                      'scenarios':len(results[0]['scenarios']),'receipt':str(out/'RECEIPT.json')},ensure_ascii=False))


if __name__=='__main__':
    main()
