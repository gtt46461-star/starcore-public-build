#!/usr/bin/env python3
"""Stage the immutable source117 delivery; full-plan and future quality remain unaccepted."""
import argparse
import hashlib
import json
from pathlib import Path
import shutil

SOURCE = 'fc1402f8ea426b0d19ea0796f7dd19aaeebb11f3'
COMPILED = 'a7f0052459331988ab58ccd667fa1fe43ccacd9d'
APK_SHA = '739e6b13c692d11ed1cbc7c09f5f95d904328cefc1bf9d6616a9e5266203d433'
CERT = '9e6d5dc79d29d4ba5b435a171e6535386f8a56ea6fad71a6cc77aeb10ade3eca'
RUN = 40645360647
TAG = 'r26-20261028-runtime-117'
NAME = 'STARCORE-R26-20261028-117-release.apk'


def read(path):
    return json.loads(path.read_text())


def write(path, value):
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2) + '\n')


def main():
    p = argparse.ArgumentParser()
    for name in ['build', 'runtime', 'workflow', 'jobs', 'out']:
        p.add_argument('--' + name, required=True, type=Path)
    a = p.parse_args()
    workflow = read(a.workflow)
    assert workflow['id'] == RUN and workflow['head_sha'] == SOURCE
    assert workflow['path'] == '.github/workflows/r13.yml'
    assert workflow['head_branch'] == 'construction/su1-20261022'
    jobs = read(a.jobs)['jobs']
    names = {x['name']: x for x in jobs}
    required = ['build', 'android-runtime (29)', 'android-runtime (35)', 'android-runtime (36)']
    assert all(names[n]['status'] == 'completed' and names[n]['conclusion'] == 'success' for n in required)
    # Long endurance jobs are deliberately recorded separately, never promoted to PASS.
    receipt = a.build / 'package-receipt'
    assert (receipt / 'source-commit.txt').read_text().strip() == COMPILED
    apks = list(a.build.glob('app/build/outputs/apk/release/*.apk'))
    assert len(apks) == 1
    apk = apks[0]
    assert apk.stat().st_size == 27047093 and hashlib.sha256(apk.read_bytes()).hexdigest() == APK_SHA
    assert APK_SHA in (receipt / 'SHA256SUMS.txt').read_text()
    badging = (receipt / 'package.txt').read_text()
    assert all(x in badging for x in ["name='com.starcore.research.r13'", "versionCode='577271027'", "versionName='R26-2026.10.27-validation2'", 'arm64-v8a', 'armeabi-v7a', 'x86_64'])
    signature = (receipt / 'signature.txt').read_text()
    assert CERT in signature and 'Verified using v1 scheme (JAR signing): true' in signature and 'Verified using v2 scheme (APK Signature Scheme v2): true' in signature
    native = read(a.runtime / 'r26-instrumentation-results.json')
    assert (native['declaredTests'], native['executedTests'], native['notExecutedTests'], native['failedSelectors'], native['status']) == (117, 117, 0, 0, 'PASS')
    assert native['manifestSha256'] == hashlib.sha256((receipt / 'instrumentation-tests.json').read_bytes()).hexdigest()
    assert sum(x['executed'] for x in native['selectors']) == 117
    assert all(x['status'] == 'PASS' and x['executed'] == x['expected'] and x['returncode'] == 0 for x in native['selectors'])
    runtime = read(a.runtime / 'r26-runtime-synthetic.json')
    assert runtime['api'] == 29 and runtime['result'] == 'PASS_WITH_STATED_ENGINEERING_SCOPE'
    assert runtime['familyMechanismsWithCommittedProgress'] == 24 and runtime['expertsWithActualReceipts'] == 16
    service = runtime['committedResearchService']
    assert service['committedCapacityFailuresSinceInstallation'] == 0 and service['sessions']
    assert all(x['sourceCommit'] == COMPILED for x in service['sessions'])
    protocols = read(a.build / 'build/r26/verification.json')
    assert protocols['status'] == 'PASS'
    a.out.mkdir(parents=True, exist_ok=True)
    shutil.copy2(apk, a.out / NAME)
    (a.out / 'SHA256SUMS.txt').write_text(APK_SHA + '  ' + NAME + '\n')
    metadata = dict(schema='STARCORE_R26_DELIVERY_V1', deliveryStatus='INSTALLABLE_VALIDATION_NOT_FULL_PLAN_ACCEPTANCE', sourceCommit=SOURCE, compiledCommit=COMPILED, workflowRun=RUN, buildArtifactId=12797901118, runtimeArtifactId=12798501098, apk=NAME, apkSha256=APK_SHA, bytes=apk.stat().st_size, applicationId='com.starcore.research.r13', versionCode=577271027, versionName='R26-2026.10.27-validation2', certificateSha256=CERT, minSdk=21, targetSdk=28, nativeAbis=['arm64-v8a', 'armeabi-v7a', 'x86_64'], nativeTests={k:native[k] for k in ['declaredTests','executedTests','failedSelectors','notExecutedTests','status']}, api35and36Scope='STARTUP_RESTART_RELEASE_SURVIVAL_ONLY', durableCapacityFailures=0, constructionAcceptance='INCOMPLETE', wholeCardAcceptance={'PASS':1,'PARTIAL':112,'NOT_RUN':15,'total':128}, trainingDiagnostics={'implemented':19,'total':24,'scope':'SOURCE117_DEFAULT_DIAGNOSTIC_IMPLEMENTATION_NOT_FULL_FAMILY_CONTRACTS'}, naturalStrategyQuality='INSUFFICIENT', physicalDevice='USER_WAIVED_NOT_PERFORMED', workflowJobs=[{k:x.get(k) for k in ['id','name','status','conclusion']} for x in jobs], enduranceScope='SEPARATE_JOBS_NOT_REQUIRED_OR_CLAIMED_PASSED_FOR_THIS_RUNTIME_DELIVERY')
    write(a.out / 'R26-delivery.json', metadata)
    write(a.out / 'R26-native-117-results.json', native)
    write(a.out / 'R26-runtime-117-results.json', runtime)
    write(a.out / 'R26-protocol-verification.json', protocols)
    notes = ('已签名、可安装的 R26 运行验证包。\n\n'
             'Android 29：117/117 项原生测试通过；24 家族、16 专家有实际运行进展，容量失败计数为 0。Android 35、36：启动、重启及 release 包运行检查通过。原生全套测试运行于 debug 测试目标；release 包另做存活检查。\n\n'
             '本包尚未完成全方案验收：128 张施工卡为 1 PASS / 112 PARTIAL / 15 NOT_RUN；训练诊断实现 19/24。自然前瞻证据仍不足，不能保证高胜率、长期稳定或每代必然提高。完整预测机会 SLO、动态专家和旧资产等仍有待办。60 分钟场景另行记录，不以历史结果代替本包结果。\n\n'
             '安装后打开应用并启动持续研究，运行后导出检测报告。APK、源码、签名及检测分母见 R26-delivery.json；原始检测结果随附件提供。\n')
    (a.out / 'release-notes.md').write_text(notes)
    print('SOURCE117_DELIVERY_STAGED_WITH_MATCHING_117_NATIVE_TESTS')


if __name__ == '__main__':
    main()
