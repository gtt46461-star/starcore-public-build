#!/usr/bin/env python3
"""Actual original125 asset/VM, source-bound Java instrumentation and owner lifetime proof."""
from pathlib import Path
import hashlib, json, os, shutil, subprocess, sys, tempfile, zipfile

ROOT = Path(__file__).resolve().parents[4]
OUT = Path(__file__).resolve().parent
MAIN = ROOT / 'app/src/main/java/com/ethk3/novaonepro'
BASE = '5c91d2620c9fbfb96336b6c84b81a72fb37d682c'
EXPECTED_SUPPORT = '8dd37c4c7f21e09fc1413e7c2b992538b4d66f833d07c754d41ca65df2dc5da0'

def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()

def main():
    mode = sys.argv[1] if len(sys.argv) > 1 else 'fixed'
    assert mode in ('baseline', 'fixed')
    tmp = Path(tempfile.mkdtemp(prefix='r31-f23-' + mode + '-'))
    prod, platform, host, sources = [tmp / name for name in ('production', 'platform', 'host', 'sources')]
    for path in (prod, platform, host, sources):
        path.mkdir()
    support = tmp / 'support.jar'
    shutil.copyfile(Path(os.environ.get('R31_F23_SUPPORT', '/tmp/r31-ba35-host-integration-mifthdj3/frozen-debug-classes.jar')), support)
    assert sha(support) == EXPECTED_SUPPORT, 'support must be the real postfull ba35 artifact'
    sdk = Path(os.environ.get('ANDROID_HOME', os.environ.get('ANDROID_SDK_ROOT', ''))) / 'platforms/android-36/android.jar'
    assert sdk.is_file()
    commands = []

    def run(command, log):
        cmd = list(map(str, command))
        result = subprocess.run(cmd, cwd=ROOT, capture_output=True, text=True, timeout=180)
        (OUT / (mode + '-' + log)).write_text(result.stdout + result.stderr)
        commands.append(dict(command=cmd, exitCode=result.returncode, log=mode + '-' + log))
        result.check_returncode()
        return result.stdout

    adapters = {
        'Context.java': '''package android.content;public class Context{private final android.content.res.AssetManager assets;public Context(java.io.File root){assets=new android.content.res.AssetManager(root);}public android.content.res.AssetManager getAssets(){return assets;}public Context getApplicationContext(){return this;}}''',
        'AssetManager.java': '''package android.content.res;public class AssetManager{private final java.io.File root;private int opened,closed,active;public AssetManager(java.io.File root){this.root=root;}public java.io.InputStream open(String name)throws java.io.IOException{java.io.FileInputStream real=new java.io.FileInputStream(new java.io.File(root,name));synchronized(this){opened++;active++;}return new java.io.FilterInputStream(real){private boolean done;public void close()throws java.io.IOException{if(done)return;try{super.close();}finally{synchronized(AssetManager.this){done=true;closed++;active--;}}}};}public synchronized java.util.Map<String,Object> snapshotR31(){java.util.Map<String,Object> m=new java.util.LinkedHashMap<>();m.put("opened",opened);m.put("closed",closed);m.put("active",active);m.put("source","REAL_FILEINPUTSTREAM_REPOSITORY_ASSET");return m;}}''',
        'SystemClock.java': '''package android.os;public final class SystemClock{public static long elapsedRealtime(){return System.nanoTime()/1000000L;}public static long currentThreadTimeMillis(){return java.lang.management.ManagementFactory.getThreadMXBean().getCurrentThreadCpuTime()/1000000L;}}'''
    }
    shims = []
    for name, source in adapters.items():
        path = sources / name
        path.write_text(source)
        shims.append(path)
    run(['javac', '-encoding', 'UTF-8', '-d', platform, *shims], 'platform.log')
    paths = [MAIN / (name + '.java') for name in ('LegacyTeacherOwnerR26', 'strategyv2/FamilyRegistryR26', 'strategyv2/DistillationFamilyR26', 'strategyv2/FeatureCacheR27')]
    inputs = [*paths, OUT / 'SizeAgentR31.java', OUT / 'F23OwnerBudgetHost.java', OUT / 'F23OwnerBaselineHost.java', Path(__file__).resolve()]
    before = {str(path.relative_to(ROOT)): sha(path) for path in inputs}
    compile_paths = []
    for path in paths:
        if mode == 'baseline':
            target = sources / path.name
            target.write_bytes(subprocess.check_output(['git', 'show', BASE + ':' + str(path.relative_to(ROOT))], cwd=ROOT))
            compile_paths.append(target)
        else:
            compile_paths.append(path)
    cp = os.pathsep.join(map(str, (platform, support, sdk)))
    run(['javac', '-sourcepath', '', '-implicit:none', '-encoding', 'UTF-8', '-cp', cp, '-d', prod, *compile_paths], 'production.log')
    runtime = os.pathsep.join(map(str, (platform, prod, host, support, sdk)))
    main_class = 'F23OwnerBaselineHost' if mode == 'baseline' else 'F23OwnerBudgetHost'
    run(['javac', '-sourcepath', '', '-encoding', 'UTF-8', '-cp', runtime, '-d', host, OUT / 'SizeAgentR31.java', OUT / (main_class + '.java')], 'host.log')
    agent = tmp / 'size-agent.jar'
    with zipfile.ZipFile(agent, 'w') as archive:
        archive.writestr('META-INF/MANIFEST.MF', 'Manifest-Version: 1.0\nPremain-Class: com.ethk3.novaonepro.strategyv2.SizeAgentR31\n\n')
        archive.write(host / 'com/ethk3/novaonepro/strategyv2/SizeAgentR31.class', 'com/ethk3/novaonepro/strategyv2/SizeAgentR31.class')
    cmd = ['java', '-Xmx512m', '-javaagent:' + str(agent)]
    for package in ('java.lang', 'java.util', 'java.util.concurrent.atomic', 'java.lang.ref'):
        cmd.extend(('--add-opens', 'java.base/' + package + '=ALL-UNNAMED'))
    data = json.loads(run([*cmd, '-cp', runtime, 'com.ethk3.novaonepro.strategyv2.' + main_class, mode, ROOT / 'app/src/main/assets'], 'native.log'))
    comparison = {}
    if mode == 'fixed':
        baseline = json.loads((OUT / 'baseline-result.json').read_text())
        keys = ('binding', 'audit', 'trainedCodecSha256', 'lateInput')
        assert all(data[key] == baseline[key] for key in keys)
        comparison = dict(baselineSources=BASE, exactKeys=keys, originalVmAndStudentCodecIdentical=True, baselineActualFeatureFailure=baseline['featureFailure'])
    (OUT / (mode + '-result.json')).write_text(json.dumps(data, indent=2) + '\n')
    assert before == {str(path.relative_to(ROOT)): sha(path) for path in inputs}
    receipt = dict(status='TARGETED_HOST_PASS', mode=mode, baseline=BASE, scope=data['scope'], sourceSha256=before, compiledSources={path.name: sha(path) for path in compile_paths}, sourceStableDuringRun=True, assetSha256=sha(ROOT / 'app/src/main/assets/research/r967_history/R908_125_algorithms.tsv'), supportSha256=sha(support), actualObjectInstrumentation=True, originalComparison=comparison, work=str(tmp), assertions=data['assertions'], commands=commands, device='NOT_RUN', quality='NOT_EVALUATED')
    (OUT / (mode + '-RECEIPT.json')).write_text(json.dumps(receipt, indent=2) + '\n')
    print(json.dumps(receipt))

if __name__ == '__main__':
    main()
