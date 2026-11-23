"""Measure only the original named Android assertions on an empty API 34 guest."""
import hashlib
import json
import os
from pathlib import Path
import re
import subprocess


def digest(path):
    with path.open('rb') as stream:
        return hashlib.file_digest(stream, 'sha256').hexdigest()


def adb(*args, timeout=100):
    return subprocess.check_output(['adb', *map(str, args)], stderr=subprocess.STDOUT, timeout=timeout)


root = Path(os.environ['GITHUB_WORKSPACE']) / 'app-source'
plan = Path(os.environ['STARCORE_R34_PLAN_ROOT'])
temp = Path(os.environ['RUNNER_TEMP'])
package = 'com.starcore.research.r13'
source = os.environ['SOURCE']
assert subprocess.check_output(['git','-C',root,'rev-parse','HEAD'],text=True).strip() == source
assert adb('shell','getprop','ro.kernel.qemu').strip() == b'1'
assert adb('shell','getprop','ro.build.version.sdk').strip() == b'34'
assert package.encode() not in adb('shell','pm','list','packages',package)
assert (temp/'verified-target.apk').is_file() and (temp/'verified-tests.apk').is_file()
adb('shell','cmd','connectivity','airplane-mode','enable')
adb('install',temp/'verified-target.apk'); adb('install',temp/'verified-tests.apk')
apk_path = adb('shell','pm','path',package).decode().strip().removeprefix('package:')
apk_hash = adb('shell','sha256sum',apk_path).decode().split()[0]
assert apk_hash == digest(temp/'verified-target.apk') == os.environ['REPAIRED_APK_SHA']
test_path = adb('shell','pm','path',package+'.test').decode().strip().removeprefix('package:')
assert adb('shell','sha256sum',test_path).decode().split()[0] == digest(temp/'verified-tests.apk')
rows = json.loads((plan/'R335_CROSSWALK.json').read_bytes())['requirements']
methods = {}
for row in rows:
    for contract in row['test_contracts']:
        cmd = contract['command']
        if contract['state'] == 'EXISTING_ENTRY_REQUIRES_REVALIDATION' and ':app:connectedDebugAndroidTest' in cmd:
            name = cmd.rsplit('=', 1)[1]
            assert re.fullmatch(r'com\.ethk3\.novaonepro(?:\.strategyv2)?\.\w+#\w+', name)
            methods[name] = contract['path']
assert len(methods) == 13
out = {'sourceCommit':source,'sourceTree':subprocess.check_output(['git','-C',root,'rev-parse','HEAD^{tree}'],text=True).strip(),
       'apkSha256':apk_hash,'testApkSha256':digest(temp/'verified-tests.apk'),'api':34,'physical':False,
       'runId':int(os.environ['GITHUB_RUN_ID']),'scope':'ORIGINAL_ANDROID_JUNIT_ON_ISOLATED_API_34','results':{}}
receipt = temp / 'legacy-instrumentation.json'
for name, path in sorted(methods.items()):
    runner = package+'.test/com.ethk3.novaonepro.strategyv2.Su1AndroidTestRunner'
    try:
        p = subprocess.run(['adb','shell','am','instrument','-w','-r','-e','class',name,runner], stdout=subprocess.PIPE,
                           stderr=subprocess.STDOUT, timeout=360)
        raw=p.stdout; code=p.returncode
    except subprocess.TimeoutExpired as exc:
        raw=exc.stdout or b''; code=124
        adb('shell','am','force-stop',package+'.test'); adb('shell','am','force-stop',package)
    counts=[int(x) for x in re.findall(rb'INSTRUMENTATION_STATUS_CODE: (-?\d+)',raw)]
    ok=bool(code == 0 and re.search(rb'OK\s*\(1 test\)',raw) and 0 in counts and not any(c<0 for c in counts))
    out['results'][name]={'method':name,'fixtureSha256':digest(root/path),'exit':code,'statusCodes':counts,
                          'passed':ok,'outputSha256':hashlib.sha256(raw).hexdigest()}
    # Only fixed method names, counts and hashes leave this disposable guest.
    receipt.write_text(json.dumps(out,sort_keys=True)+'\n')
