#!/usr/bin/env python3
"""Final-source actual-product regression, real SQLite and explicit Android adapters.
No process startup, screen rendering, phone installation or live accuracy is tested.
Use --step all for clean compilation and all tests. Each command retains its output,
exit code, source digest and test/fixture digest. Never imports legacy product mocks.
"""
from pathlib import Path
import argparse, hashlib, importlib.util, json, os, subprocess, sys, time
ROOT=Path(__file__).resolve().parents[2]
spec=importlib.util.spec_from_file_location('previous_runner',ROOT/'verification/r979/run.py')
old=importlib.util.module_from_spec(spec);spec.loader.exec_module(old)
LEGACY=old.STEPS
CUSTOM=('resource-closure','backlog','full-population','business-closure')
STEPS=LEGACY+CUSTOM

def file_digest(paths):
    h=hashlib.sha256()
    for p in sorted(set(paths)):
        if p.is_file() and '__pycache__' not in p.parts and p.suffix not in ('.pyc','.pyo'):
            h.update(str(p.relative_to(ROOT)).encode()+b'\0'+p.read_bytes())
    return h.hexdigest()

def main():
    p=argparse.ArgumentParser(description=__doc__)
    p.add_argument('--step',choices=STEPS+('all',),required=True)
    p.add_argument('--out',type=Path,required=True)
    p.add_argument('--android-jar',type=Path,required=True)
    a=p.parse_args();out=a.out.resolve();android=a.android_jar.resolve()
    if not android.is_file():p.error('Android SDK jar is missing')
    if out==ROOT or ROOT.is_relative_to(out):p.error('Output may not contain repository')
    out.mkdir(parents=True,exist_ok=True)
    env=dict(os.environ,JAVA_TOOL_OPTIONS='-Xmx768m -XX:ActiveProcessorCount=2')
    meta=out/'r980_test_manifest.json'
    records=json.loads(meta.read_text())['executions'] if meta.exists() else []
    def run(name,command):
        command=list(map(str,command));before=old.source_digest();start=time.time()
        test_hash=file_digest((ROOT/'verification').rglob('*'))
        log=out/('r980_'+name+'.log')
        # Preserve a failed attempt rather than overwrite it with a successful retry.
        attempt=1
        while log.exists():
            attempt+=1;log=out/('r980_'+name+'_attempt'+str(attempt)+'.log')
        with log.open('w') as f:
            f.write('COMMAND='+json.dumps(command)+'\n');f.flush()
            code=subprocess.run(command,cwd=ROOT,env=env,stdout=f,stderr=subprocess.STDOUT).returncode
        after=old.source_digest()
        records.append({'name':name,'attempt':attempt,'command':command,'exit_code':code,
                        'status':'PASS' if code==0 and before==after else 'FAIL',
                        'source_before_sha256':before,'source_after_sha256':after,
                        'tests_and_fixtures_sha256':test_hash,'log':log.name,
                        'started_unix':start,'finished_unix':time.time()})
        meta.write_text(json.dumps({'scope':'ACTUAL_PRODUCT_JAVA_REAL_SQLITE_WITH_ANDROID_API_ADAPTERS',
                                   'phone_runtime':'NOT RUN','physical_overwrite_install':'NOT RUN',
                                   'live_accuracy':'NOT RUN','executions':records},indent=2)+'\n')
        print(name,records[-1]['status'],'exit='+str(code),flush=True)
        if code or before!=after:raise SystemExit(code or 1)
    for step in (STEPS if a.step=='all' else (a.step,)):
        if step in LEGACY:
            run(step,[sys.executable,ROOT/'verification/r979/run.py','--step',step,'--out',out,'--android-jar',android]);continue
        marker=out/'compiled.sha256'
        if not marker.is_file() or marker.read_text().strip()!=old.source_digest():
            raise SystemExit('FINAL_SOURCE_CHANGED_OR_NOT_COMPILED')
        cp=os.pathsep.join(map(str,(out/'classes',out/'android-runtime')))
        target=out/'r980-tests';target.mkdir(exist_ok=True)
        adapters=sorted((ROOT/'verification/r974/android').rglob('*.java'))
        adapters+=sorted((ROOT/'verification/r980/android').rglob('*.java'))
        adapters+=sorted((ROOT/'verification/r932/android-json').rglob('*.java'))
        adapters+=[ROOT/('verification/r967/host/android/'+s) for s in ('content/ContentValues.java','os/SystemClock.java','os/CancellationSignal.java')]
        inputs=adapters+[ROOT/'verification/r974/HostProductR974.java',ROOT/'verification/r978/DevelopmentHarnessR978.java']
        inputs+=sorted((ROOT/'verification/r980').glob('*.java'))
        run(step+'_harness_compile',['javac','-encoding','UTF-8','-cp',cp,'-d',target,*inputs])
        if step=='business-closure':
            run(step,['java','-cp',str(target)+os.pathsep+cp,'com.ethk3.novaonepro.BusinessClosureHarnessR980',
                      ROOT/'verification/r967/host/sqlite_bridge.py',out/'v3_e2e.sqlite',out/'r980_business_closure.sqlite'])
            continue
        name='ResourceClosureHarnessR980' if step=='resource-closure' else 'BacklogHarnessR980'
        args=[] if step=='resource-closure' else ['population' if step=='full-population' else 'backlog']
        run(step,['java','-cp',str(target)+os.pathsep+cp,'com.ethk3.novaonepro.'+name,
                  ROOT/'verification/r967/host/sqlite_bridge.py',ROOT/'verification/r980/fixtures',*args])
if __name__=='__main__':main()
