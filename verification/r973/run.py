#!/usr/bin/env python3
"""Reproducible R973 product-compilation and host-SQLite regression runner.

Use --stage compile then --stage exercise when a command executor has a short
foreground time limit. No Android device, real draw stream or REAL routing proof
is inferred from these host tests.
"""
import argparse
import hashlib
import json
import os
from pathlib import Path
import subprocess
import sys


def main() -> None:
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('--android-jar', required=True, type=Path)
    p.add_argument('--out', required=True, type=Path)
    p.add_argument('--stage', choices=('all', 'compile', 'exercise'), default='all')
    a = p.parse_args()
    root = Path(__file__).resolve().parents[2]
    out = a.out.resolve(); out.mkdir(parents=True, exist_ok=True)
    host = out/'host'; results = []
    summary = {'scope': 'ACTUAL_PRODUCT_HOST_REGRESSION', 'steps': results,
               'physical_device': 'NOT_RUN', 'REAL_end_to_end_routing': 'NOT_RUN',
               'live_future_evidence': 'NOT_RUN', 'live_slow_training_latency': 'NOT_RUN'}
    def save(): (out/'summary.json').write_text(json.dumps(summary, indent=2)+'\n')
    def run(name, cmd):
        log=out/(name+'.log')
        with log.open('w') as f:
            f.write('COMMAND='+json.dumps([str(x) for x in cmd])+'\n'); f.flush()
            r=subprocess.run([str(x) for x in cmd], cwd=root, stdout=f, stderr=subprocess.STDOUT)
        results.append({'name':name,'exit_code':r.returncode,'log':log.name}); save()
        print(name, 'PASS' if r.returncode==0 else 'FAIL', flush=True)
        if r.returncode: raise SystemExit(r.returncode)
    def source_digest():
        h=hashlib.sha256()
        for folder in ('app/src/main/java','app/build/generated/source/r967','release'):
            for f in sorted((root/folder).rglob('*')):
                if f.is_file(): h.update(str(f.relative_to(root)).encode()+b'\0'+f.read_bytes())
        return h.hexdigest()
    if a.stage in ('all','compile'):
        run('existing_six_product_harnesses',[sys.executable,root/'verification/r967/run.py',
            '--android-jar',a.android_jar.resolve(),'--out',host])
        (out/'compiled-input.sha256').write_text(source_digest()+'\n')
    if a.stage=='compile': return
    if not (out/'compiled-input.sha256').is_file() or (out/'compiled-input.sha256').read_text().strip()!=source_digest():
        raise SystemExit('Run --stage compile on the same exact product first')
    old=json.loads((host/'summary.json').read_text())
    if any(x['exit_code'] for x in old['tests']): raise SystemExit('Prior product compilation/regression failed')
    cl=host/'classes'; runtime=host/'android-runtime'
    cp=os.pathsep.join(map(str,(cl,runtime)))
    v=root/'verification/r973'
    run('compile_new_sqlite_harnesses',['javac','-encoding','UTF-8','-cp',cp,'-d',cl,
        v/'QueueFreezeRegressionR973.java',v/'AdmissionProgressRegressionR973.java'])
    bridge=root/'verification/r967/host/sqlite_bridge.py'
    run('queue_freeze_23',['java','-cp',cp,'com.ethk3.novaonepro.QueueFreezeRegressionR973',bridge])
    run('admission_progress_16',['java','-cp',cp,'com.ethk3.novaonepro.AdmissionProgressRegressionR973',bridge])
    clock=out/'clock-test-double-classes'; clock.mkdir(exist_ok=True)
    run('compile_clock_harness',['javac','-encoding','UTF-8','-cp',cp,'-d',clock,
        *sorted((v/'clock-stubs').rglob('*.java')),
        root/'app/src/main/java/com/ethk3/novaonepro/CountdownClock.java',v/'CountdownLoopRegressionR973.java'])
    run('clock_7',['java','-cp',str(clock)+os.pathsep+cp,'com.ethk3.novaonepro.CountdownLoopRegressionR973'])
    run('semantic_mutation_8',[sys.executable,root/'scripts/r967_semantic_identity.py','--self-test'])
    run('semantic_identity_check',[sys.executable,root/'scripts/r967_semantic_identity.py','--check'])
    run('release_identity_check',[sys.executable,root/'scripts/r926_release_manifest.py','--check'])
    summary['existing_six_product_harnesses']=old['tests']
    summary['new_assertions']=46; summary['semantic_mutation_checks']=8
    summary['compiled_source_sha256']=source_digest(); save()
    print('R973_HOST_REGRESSION=PASS; NEW_ASSERTIONS=46; MUTATION_CHECKS=8; DEVICE_AND_REAL_ROUTING=NOT_RUN')


if __name__=='__main__': main()
