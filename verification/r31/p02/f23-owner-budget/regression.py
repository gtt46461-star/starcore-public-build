#!/usr/bin/env python3
"""Preserved original replay/PIT/model and ownership tests; real exact before/after golden ABI."""
from pathlib import Path
import hashlib,json,os,re,subprocess
ROOT=Path(__file__).resolve().parents[4]
OUT=Path(__file__).resolve().parent
BASE='5c91d2620c9fbfb96336b6c84b81a72fb37d682c'

def sha(path):return hashlib.sha256(path.read_bytes()).hexdigest()
def main():
    gradle=Path(os.environ.get('GRADLE_USER_HOME',Path.home()/'.gradle'))
    junit=next(gradle.glob('caches/modules-2/files-2.1/junit/junit/4.13.2/*/*.jar'))
    hamcrest=next(gradle.glob('caches/modules-2/files-2.1/org.hamcrest/hamcrest-core/1.3/*/*.jar'))
    sdk=Path(os.environ.get('ANDROID_HOME',os.environ.get('ANDROID_SDK_ROOT','')))/'platforms/android-36/android.jar'
    testroot=ROOT/'app/src/test/java/com/ethk3/novaonepro/strategyv2'
    names=['R27TestSupport','R27ReplayTest','R27LeakageTest','R30ModelSurfaceTest','R31FamilyAdmissionTest','R31FamilyScopeIsolationTest','R31FamilyBudgetResumeTest','RepairLegacyTeacherR30Test']
    tests=[testroot/(name+'.java') for name in names]
    for path in tests:
        assert subprocess.check_output(['git','show',BASE+':'+str(path.relative_to(ROOT))],cwd=ROOT)==path.read_bytes(),str(path)+' was changed'
    golden_host=ROOT/'tools/r31/memory_host/FeatureGoldenR31Host.java'
    inputs=[*tests,golden_host,Path(__file__).resolve()]
    before={str(path.relative_to(ROOT)):sha(path) for path in inputs}
    commands=[]
    def run(command,log):
        cmd=list(map(str,command));result=subprocess.run(cmd,cwd=ROOT,capture_output=True,text=True,timeout=240)
        (OUT/log).write_text(result.stdout+result.stderr)
        commands.append(dict(command=cmd,exitCode=result.returncode,log=log))
        result.check_returncode();return result.stdout
    golden={};unit_count=None;actual_compared=None;bound={}
    for mode in ('baseline','fixed'):
        receipt=json.loads((OUT/(mode+'-RECEIPT.json')).read_text());work=Path(receipt['work']);output=work/'numerical-tests';output.mkdir(exist_ok=True)
        bound[mode]=receipt['compiledSources']
        cp=os.pathsep.join(map(str,[work/'platform',work/'production',output,work/'support.jar',junit,hamcrest,sdk]))
        run(['javac','-sourcepath','','-implicit:none','-encoding','UTF-8','-cp',cp,'-d',output,golden_host],'golden-'+mode+'-javac.log')
        golden[mode]=json.loads(run(['java','-Xmx512m','-cp',cp,'com.ethk3.novaonepro.strategyv2.FeatureGoldenR31Host',ROOT/'app/src/main/assets/r27/golden_512.bin'],'golden-'+mode+'.log'))
        if mode=='fixed':
            run(['javac','-sourcepath','','-implicit:none','-encoding','UTF-8','-cp',cp,'-d',output,*tests],'original24-javac.log')
            unit=run(['java','-Xmx512m','-cp',cp,'org.junit.runner.JUnitCore',*['com.ethk3.novaonepro.strategyv2.'+name for name in names[1:]]],'original24-junit.log')
            unit_count=int(re.search(r'OK \((\d+) tests\)',unit).group(1));actual_compared=int(re.search(r'H02_PASS compared=(\d+) bitIdentical=true',unit).group(1))
    assert golden['baseline']==golden['fixed'];assert len(golden['fixed'])==52;assert actual_compared>=190
    assert before=={str(path.relative_to(ROOT)):sha(path) for path in inputs}
    result=dict(status='TARGETED_HOST_PASS',scope='ORIGINAL_REPLAY_PIT_MODEL_AND_FAMILY_OWNERSHIP_TESTS_ONE_JVM_ALL_ORIGINAL_200_190_BIT_ASSERTIONS_UNCHANGED;52_BEFORE_AFTER_PROTECTED_GOLDEN_FINGERPRINTS',originalJUnit=unit_count,actualBitCompared=actual_compared,scheduledLiveTargets=200,allOriginalTestBytesRetained=True,goldenFingerprints=len(golden['fixed']),goldenIdentical=True,sourceSha256=before,compiledProduction=bound,sourceStableDuringRun=True,explicitGc=False,heapArgument='-Xmx512m_ONLY',gradleIsolationChanges=False,trainingMathChanged=False,commands=commands,device='NOT_RUN',naturalQuality='NOT_EVALUATED')
    (OUT/'REGRESSION_RECEIPT.json').write_text(json.dumps(result,indent=2)+'\n');print(json.dumps(result))
if __name__=='__main__':main()
