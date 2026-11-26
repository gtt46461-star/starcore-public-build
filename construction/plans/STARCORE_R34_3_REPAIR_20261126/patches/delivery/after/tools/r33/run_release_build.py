#!/usr/bin/env python3
"""Run the real release tasks and bind exit status, log and actual APK bytes to a clean source."""
from pathlib import Path
import argparse
import json
import os
import re
import subprocess
import time
import verify_plan_binding as binding


EXPECTED_CERT = 'd8d8cee7f406267b2bf18fc9aef0c6ab760423cf205ccd6613dd44085bcc74e7'


def build_environment(plan, inherited=None, *, r34_plan=None, r341_plan=None, r342_plan=None):
    """Inject the public identity required by CP12 without reading or printing private inputs."""
    env = dict(os.environ if inherited is None else inherited)
    supplied = env.get('R13_EXPECTED_CERT_SHA256', EXPECTED_CERT).lower()
    binding.require(supplied == EXPECTED_CERT, 'R13_PUBLIC_CERTIFICATE_PIN_CONFLICT')
    env['R13_EXPECTED_CERT_SHA256'] = EXPECTED_CERT
    env['STARCORE_R33_PLAN_ROOT'] = str(Path(plan).resolve())
    for key, configured in (('STARCORE_R34_PLAN_ROOT', r34_plan),
                            ('STARCORE_R341_PLAN_ROOT', r341_plan),
                            ('STARCORE_R342_PLAN_ROOT', r342_plan)):
        location = configured or env.get(key)
        if location:
            env[key] = str(Path(location).resolve())
    return env


def public_error(error, phase):
    """Publish a bounded machine code, never exception text or environment values."""
    code = str(error).split(':', 1)[0]
    if not re.fullmatch('[A-Z][A-Z0-9_]{0,95}', code):
        code = 'IDENTITY_VERIFICATION_ERROR' if phase == 'POST_BUILD_IDENTITY' else 'BUILD_LAUNCH_ERROR'
    return {'phase': phase, 'code': code, 'exceptionType': type(error).__name__}


def main():
    p=argparse.ArgumentParser(description=__doc__)
    p.add_argument('--repo',type=Path,default=Path(__file__).resolve().parents[2])
    p.add_argument('--plan',type=Path,default=os.environ.get('STARCORE_R33_PLAN_ROOT'))
    p.add_argument('--r34-plan',type=Path,help='Sealed R34 root, defaults to STARCORE_R34_PLAN_ROOT')
    p.add_argument('--r341-plan',type=Path,help='Sealed R34.1 root, defaults to STARCORE_R341_PLAN_ROOT')
    p.add_argument('--r342-plan',type=Path,help='Sealed R34.2 root, defaults to STARCORE_R342_PLAN_ROOT')
    p.add_argument('--out',type=Path,required=True)
    a=p.parse_args();repo=a.repo.resolve();out=a.out.resolve()
    binding.require(a.plan is not None,'STARCORE_R33_PLAN_ROOT_REQUIRED')
    binding.require(not out.is_relative_to(repo) and not out.exists(),'FRESH_EXTERNAL_BUILD_OUTPUT_REQUIRED')
    roots = dict(r34_plan=a.r34_plan, r341_plan=a.r341_plan, r342_plan=a.r342_plan)
    before=binding.identity(repo,a.plan,repo/'release/r33_plan_binding.json',True,**roots)
    out.mkdir(parents=True,exist_ok=False);log=out/'gradle-build.log'
    command=[str(repo/'gradlew'),'--no-daemon','--max-workers=4','-Pr30Instrumentation=true',
             ':app:assembleRelease',':app:assembleDebug',':app:assembleDebugAndroidTest']
    start=time.monotonic_ns()
    env=build_environment(a.plan, **roots)
    launch_error=None
    with log.open('xb') as f:
        try:
            result=subprocess.run(command,cwd=repo,env=env,stdout=f,stderr=subprocess.STDOUT)
            exit_code=result.returncode
        except (OSError, subprocess.SubprocessError) as error:
            exit_code=127
            launch_error=public_error(error,'BUILD_LAUNCH')
            f.write(b'R334_BUILD_LAUNCH_FAILED; consult structured receipt.\n')
    after=None
    identity_error=None
    try:
        after=binding.identity(repo,a.plan,repo/'release/r33_plan_binding.json',True,**roots)
    except Exception as error:
        # This boundary must preserve the original build result and a FAIL
        # receipt for every ordinary identity failure, including path errors.
        # KeyboardInterrupt/SystemExit remain interrupts, not forged outcomes.
        identity_error=public_error(error,'POST_BUILD_IDENTITY')
    unchanged=after is not None and before==after
    passed=exit_code==0 and unchanged and identity_error is None and launch_error is None
    apks=sorted((repo/'app/build/outputs/apk/release').glob('*.apk'))
    passed=passed and len(apks)==1
    receipt={'schema':'STARCORE_R33_4_RELEASE_BUILD_1','sourceCommit':before['source_commit'],
             'sourceTree':before['source_tree'],'sourceClean':after is not None and after.get('source_dirty') is False,
             'sourceCleanBeforeBuild':True,'command':command,
             'exitCode':exit_code,'wrapperExitCode':0 if passed else 1,'gradleRelease':'PASS' if passed else 'FAIL',
             'gradleLogSha256':binding.sha(log),'durationNs':time.monotonic_ns()-start,
             'planManifestSha256':before['plan_manifest_sha256'],
             'apkSha256':binding.sha(apks[0]) if passed else None,
             'sourceUnchangedDuringBuild':unchanged,'identityError':identity_error,'buildLaunchError':launch_error,
             'ciRun':os.environ.get('GITHUB_RUN_ID'),
             'deviceAcceptance':'NOT_INFERRED','naturalQuality':'NOT_INFERRED'}
    for series in ('r341', 'r342'):
        if current := before.get(series):
            receipt.update({series+'PlanManifestSha256':current['planSha256'],
                            series+'PlanCommit':current['planCommit'], series+'Batch':current['batch']})
    if field := before.get('fieldRepair173'):
        receipt['fieldRepair173'] = field
    binding.write_json(out/'CI_BUILD_RECEIPT.json',receipt)
    print(json.dumps({'gradleRelease':receipt['gradleRelease'],'receipt':str(out/'CI_BUILD_RECEIPT.json'),
                      'log':str(log)},ensure_ascii=False))
    raise SystemExit(0 if passed else 1)


if __name__=='__main__':main()
