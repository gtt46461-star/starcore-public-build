#!/usr/bin/env python3
"""Verification-only fixture: no production/tools/original-test modifications and no explicit collection."""
from pathlib import Path
from datetime import datetime,timezone
import hashlib,json,os,re,shutil,subprocess,tempfile
ROOT=Path(__file__).resolve().parents[4];OUT=Path(__file__).resolve().parent
OLD='3246ccf8eb6f7b2f7b9d0fe77fc18a70ab958056';SUBJECT='app/src/test/java/com/ethk3/novaonepro/strategyv2/R31OptionalHeapRecoveryTest.java';PROD='app/src/main/java/com/ethk3/novaonepro/strategyv2/'
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def main():
    work=Path(tempfile.mkdtemp(prefix='r31-guard-pressure-'));env=Path(os.environ.get('GRADLE_USER_HOME',str(Path.home()/'.gradle')));local=Path('/workspace/starcore-environment/gradle-home');env=env if env.exists() else local
    junit=next(env.glob('caches/modules-2/files-2.1/junit/junit/4.13.2/*/*.jar'));hamcrest=next(env.glob('caches/modules-2/files-2.1/org.hamcrest/hamcrest-core/1.3/*/*.jar'));java,javac=shutil.which('java'),shutil.which('javac')
    inputs=[ROOT/(PROD+'MemoryBudgetR31.java'),ROOT/(PROD+'JsonR27.java'),ROOT/SUBJECT,OUT/'RecoveryGuardPressureHost.java',Path(__file__).resolve()];before={str(p.relative_to(ROOT)):sha(p) for p in inputs};original={p.relative_to(ROOT):p.read_bytes() for p in inputs[:3]};head=subprocess.check_output(['git','rev-parse','HEAD'],cwd=ROOT,text=True).strip();old_bytes=subprocess.check_output(['git','show',OLD+':'+SUBJECT],cwd=ROOT);new_bytes=(ROOT/SUBJECT).read_bytes()
    # The intended fixture repair preserves every other byte, including all assertion expressions.
    expected=old_bytes.replace(b'MemoryBudgetR31.Kind.RESEARCH,available-(4L<<20)',b'MemoryBudgetR31.Kind.CHECKPOINT,Math.max(4096L,available-(4L<<20))')
    assert expected==new_bytes,'current test differs from the exact authorized owner-kind/nonnegative-byte fixture repair'
    oldsrc=work/'old-source'/Path(SUBJECT).name;oldsrc.parent.mkdir();oldsrc.write_bytes(old_bytes);newsrc=work/'current-source'/Path(SUBJECT).name;newsrc.parent.mkdir();newsrc.write_bytes(new_bytes)
    commands=[]
    def run(args,log,timeout=90):
        args=list(map(str,args));p=subprocess.run(args,cwd=ROOT,capture_output=True,text=True,timeout=timeout);(OUT/log).write_text(p.stdout+p.stderr);commands.append(dict(command=args,log=log,exitCode=p.returncode));p.check_returncode();return p.stdout
    dirs={}
    for name,source in [('old',oldsrc),('current',newsrc)]:
        classes=work/(name+'-classes');classes.mkdir();cp=os.pathsep.join(map(str,[classes,junit,hamcrest]));run([javac,'-encoding','UTF-8','-sourcepath','','-cp',cp,'-d',classes,ROOT/(PROD+'MemoryBudgetR31.java'),ROOT/(PROD+'JsonR27.java'),source,OUT/'RecoveryGuardPressureHost.java'],name+'-javac.log');dirs[name]=cp
    results=[]
    for case in ['old-pressure','current-pressure','current-healthy']:
        data=json.loads(run([java,'-Xmx512m','-cp',dirs['old' if case=='old-pressure' else 'current'],'com.ethk3.novaonepro.strategyv2.RecoveryGuardPressureHost',case],case+'.log'));(OUT/(case+'.json')).write_text(json.dumps(data,indent=2)+'\n');results.append(data)
    assert len({r['pid'] for r in results})==3,'each case must be a distinct JVM'
    after={str(p.relative_to(ROOT)):sha(p) for p in inputs};assert before==after,'source changed during verification'
    for p,data in original.items():assert (ROOT/p).read_bytes()==data,'original production or test input modified'
    receipt=dict(status='TARGETED_HOST_PASS',capturedAt=datetime.now(timezone.utc).isoformat(),scope='UNCHANGED_4_GUARD_JUNIT_OLD_FIXTURE_REPRODUCED_AND_ROOT_REPAIR_VALIDATED_UNDER_REAL_DEFAULT_512M_HEAP_PRESSURE_NOT_FULL_SUITE_OR_DEVICE',headAtCompile=head,oldTestCommit=OLD,oldTestPath=SUBJECT,oldTestSha256=hashlib.sha256(old_bytes).hexdigest(),currentTestSha256=hashlib.sha256(new_bytes).hexdigest(),onlyAuthorizedFixtureDifferences=True,allOriginalAssertionBytesPreserved=True,sourceSha256=before,sourceStableDuringRun=True,productionAndOriginalTestWrites=False,work=str(work),junitJarSha256=sha(junit),hamcrestJarSha256=sha(hamcrest),cases=[dict(mode=r['mode'],pid=r['pid'],tests=r['junitRunCount'],failures=r['junitFailureCount'],ignored=r['junitIgnoreCount'],measuredHeapBefore=r['beforeJUnit']['heapUsedBytes'],maxHeap=r['maxHeapBytes'],payloadBytes=r['protectedPayloadBytes'],protectedPayloadAndLeaseRetained=r['protectedPayloadRetainedThroughJUnit'] and r['protectedLeaseRetainedThroughJUnit'],explicitCollectorRequests=r['explicitCollectorRequestCount'],observedGcDuringJUnit=r['observedGcDuringJUnit']) for r in results],scopeLimitations=['pressure orchestration is an explicitly labeled engineering payload, not a natural model workload','existing custom-Probe guard remains unchanged; no substituted pressure Probe or time','full 472-test capacity failures, Android/device, 24h/72h and natural quality remain separate'],commands=commands)
    (OUT/'RECEIPT.json').write_text(json.dumps(receipt,indent=2)+'\n');(OUT/'FROZEN_SOURCES.json').write_text(json.dumps(dict(scope='VERIFICATION_ONLY_FIXTURE_NO_BUILD_INPUT_CHANGE',sourceSha256=before,oldTestSha256=receipt['oldTestSha256'],currentTestSha256=receipt['currentTestSha256']),indent=2)+'\n');print(json.dumps(receipt))
if __name__=='__main__':main()
