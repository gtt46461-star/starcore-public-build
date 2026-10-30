#!/usr/bin/env python3
"""Offline control-flow checks. Stub commands never claim Android/device acceptance."""
import hashlib
import json
import os
from pathlib import Path
import sqlite3
import subprocess
import sys
import tempfile


ROOT = Path(__file__).resolve().parents[2]
CERT = '9e6d5dc79d29d4ba5b435a171e6535386f8a56ea6fad71a6cc77aeb10ade3eca'
BASE = b'OFFLINE_HARNESS_BASELINE_NOT_AN_APK'
OLD_SOURCE = '116888fe641aa0f67c4ef85659d9fa0f1303c4b7'
DRIVER = r'''import json,os,shutil,sys
from pathlib import Path
root=Path(os.environ['R29_HARNESS_ROOT']); name=Path(sys.argv[0]).name; args=sys.argv[1:]
with (root/'commands.jsonl').open('a') as f:f.write(json.dumps([name,*args])+'\n')
state_path=root/'state.json'; state=json.loads(state_path.read_text()) if state_path.exists() else {}
phase=state.get('phase','setup')
if name=='adb':
    if args==['get-serialno']:print(os.environ.get('R29_HARNESS_SERIAL','emulator-5554'))
    elif args and args[0]=='install':
        phase='baseline' if Path(args[-1]).name=='baseline.apk' else 'upgrade' if '-r' in args else 'clean'
        state['phase']=phase;state_path.write_text(json.dumps(state))
        if os.environ.get('R29_HARNESS_INSTALL_FAIL')==phase:sys.exit(29)
        state['installed']=True;state_path.write_text(json.dumps(state))
        print('Success')
    elif args and args[0]=='uninstall':
        if not state.get('installed',False):sys.exit(1)
        if os.environ.get('R29_HARNESS_UNINSTALL_FAIL')=='1':sys.exit(37)
        state['installed']=False;state_path.write_text(json.dumps(state))
    elif args and args[0]=='pull':
        if args[1].endswith(('-wal','-shm')):sys.exit(1)
        shutil.copyfile(root/'fixture.db',args[2])
    elif args[:4]==['shell','pm','list','packages']:
        if state.get('installed',False):print('package:com.starcore.research.r13')
    elif args and args[0]=='shell' and 'cat /data/' in args[1]:print('r29-official-upgrade-probe')
    elif args and args[0]=='logcat':print('OFFLINE_COMMAND_STUB_NOT_ANDROID_EVIDENCE')
elif name=='curl':Path(args[args.index('-o')+1]).write_bytes(b'OFFLINE_HARNESS_BASELINE_NOT_AN_APK')
elif name=='apksigner':print('Signer #1 certificate SHA-256 digest: '+os.environ['EXPECTED_CERT'])
elif name=='aapt':
    baseline=Path(args[-1]).name=='baseline.apk';code=577291006 if baseline else 577291013
    print("package: name='com.starcore.research.r13' versionCode='%d' versionName='offline-%d'"%(code,code))
elif name=='python3':
    if args and args[0]=='scripts/wait_r13_android_ready.py':pass
    elif args and args[0]=='scripts/probe_r13_startup.py':
        out=Path('startup-evidence');out.mkdir();(out/'startup.json').write_text(json.dumps({'scope':'OFFLINE_STUB_NOT_DEVICE','phase':phase}))
        if os.environ.get('R29_HARNESS_PROBE_FAIL')==phase:sys.exit(31)
    elif args and args[0]=='tools/r29/inspect_runtime.py':
        out=Path(args[args.index('--out')+1]);out.write_text(json.dumps({'scope':'OFFLINE_STUB_NOT_DEVICE','phase':phase}))
        if '--observe-only' not in args and os.environ.get('R29_HARNESS_PROTOCOL_FAIL') in (phase,'both'):sys.exit(23)
    else:os.execv(sys.executable,[sys.executable,*args])
elif name!='sleep':raise AssertionError('UNEXPECTED_STUB_COMMAND:'+name)
'''


def run_case(script, *, protocol_fail='', probe_fail='', install_fail='', uninstall_fail=False, serial='emulator-5554'):
    with tempfile.TemporaryDirectory(prefix='r29-smoke-phase-selftest-') as temporary:
        root = Path(temporary)
        delivered = root / 'delivered'
        delivered.mkdir()
        (delivered / 'candidate-debug.apk').write_bytes(b'OFFLINE_NOT_AN_APK')
        (delivered / 'delivery.json').write_text(json.dumps({
            'apk': 'candidate-debug.apk', 'versionCode': 577291013, 'versionName': 'offline-577291013'}))
        with sqlite3.connect(root / 'fixture.db') as db:
            db.execute('CREATE TABLE draws(issue TEXT,n1,n2,n3,n4,n5,n6,n7,n8,n9,n10)')
            db.execute("INSERT INTO draws VALUES('202610300001',1,2,3,4,5,6,7,8,9,10)")
            db.execute('CREATE TABLE r29_migration_publication(singleton INTEGER,phase TEXT,baseline_name TEXT)')
            db.execute("INSERT INTO r29_migration_publication VALUES(1,'PUBLISHED','starcore-r29-baseline-1.db')")
        binary = root / 'bin'
        binary.mkdir()
        driver = binary / 'driver'
        driver.write_text('#!' + sys.executable + '\n' + DRIVER)
        driver.chmod(0o755)
        for name in ['adb', 'curl', 'apksigner', 'aapt', 'python3', 'sleep']:
            (binary / name).symlink_to(driver)
        sdk = root / 'sdk/build-tools/36.0.0'
        sdk.mkdir(parents=True)
        for name in ['aapt', 'apksigner']:
            (sdk / name).symlink_to(binary / name)
        test_script = root / 'emulator_smoke.sh'
        test_script.write_text(script)
        env = dict(os.environ, PATH=str(binary) + os.pathsep + os.environ['PATH'],
                   ANDROID_HOME=str(root / 'sdk'), EXPECTED_CERT=CERT,
                   R29_BASELINE_SHA256=hashlib.sha256(BASE).hexdigest(),
                   R29_BASELINE_VERSION='577291006', R29_VARIANT='debug',
                   R29_HARNESS_ROOT=str(root), R29_HARNESS_PROTOCOL_FAIL=protocol_fail,
                   R29_HARNESS_PROBE_FAIL=probe_fail,
                   R29_HARNESS_INSTALL_FAIL=install_fail,
                   R29_HARNESS_UNINSTALL_FAIL='1' if uninstall_fail else '0',
                   R29_HARNESS_SERIAL=serial)
        completed = subprocess.run(['bash', str(test_script), str(delivered), str(root / 'out')],
                                   cwd=root, env=env, text=True, capture_output=True, timeout=30)
        records = [json.loads(line) for line in (root / 'commands.jsonl').read_text().splitlines()]
        out = root / 'out'
        return {
            'code': completed.returncode,
            'phaseResults': json.loads((out / 'phase-results.json').read_text()) if (out / 'phase-results.json').exists() else None,
            'cleanInstalled': any(row[0:2] == ['adb', 'install'] and '-r' not in row and row[-1].endswith('candidate-debug.apk') for row in records),
            'upgradeProbed': any(row[0:2] == ['python3', 'scripts/probe_r13_startup.py'] for row in records) and (out / 'startup-upgrade/startup.json').exists(),
            'startupEvidence': {phase: (out / ('startup-' + phase) / 'startup.json').exists() for phase in ['upgrade', 'clean']},
            'privateFilesRetained': sorted(p.name for p in out.glob('*') if p.suffix in ['.db', '.apk'] or p.name.endswith(('.db-wal', '.db-shm'))),
            'result': (out / 'result.txt').read_text() if (out / 'result.txt').exists() else '',
            'tail': (completed.stdout + completed.stderr)[-2500:],
        }


def require(condition, description, result):
    if not condition:
        raise AssertionError(description + ': ' + json.dumps(result))
    print('R29_SMOKE_PHASES|' + description + '|OK')


def main():
    script = (ROOT / 'tools/r29/emulator_smoke.sh').read_text()
    for fail in ['upgrade', 'clean', 'both', '']:
        result = run_case(script, protocol_fail=fail)
        expected = {'upgrade': 23 if fail in ['upgrade', 'both'] else 0,
                    'clean': 23 if fail in ['clean', 'both'] else 0}
        require(result['phaseResults'] is not None and result['cleanInstalled']
                and all(result['phaseResults'][phase]['exitCode'] == code for phase, code in expected.items())
                and result['code'] == (1 if fail else 0)
                and result['phaseResults']['status'] == ('FAIL' if fail else 'PASS')
                and result['startupEvidence'] == {'upgrade': True, 'clean': True}
                and result['privateFilesRetained'] == []
                and ('_PASS;' in result['result']) == (not fail),
                'PROTOCOL_' + (fail or 'ALL_PASS').upper(), result)
    result = run_case(script, probe_fail='upgrade')
    require(result['code'] == 1 and result['cleanInstalled']
            and result['phaseResults']['upgrade']['exitCode'] == 31
            and result['phaseResults']['clean']['exitCode'] == 0
            and result['startupEvidence'] == {'upgrade': True, 'clean': True},
            'FAILED_PROBE_EVIDENCE_RETAINED_AND_CLEAN_STILL_RUNS', result)
    result = run_case(script, install_fail='upgrade')
    require(result['code'] == 1 and result['cleanInstalled']
            and result['phaseResults']['upgrade']['exitCode'] == 29
            and not result['startupEvidence']['upgrade']
            and result['phaseResults']['clean']['exitCode'] == 0,
            'SUBSHELL_ERREXIT_STOPS_FAILED_INSTALL_ONLY', result)
    result = run_case(script, install_fail='baseline')
    require(result['code'] == 1 and result['cleanInstalled']
            and result['phaseResults']['upgrade']['exitCode'] == 29
            and not result['startupEvidence']['upgrade']
            and result['phaseResults']['clean']['exitCode'] == 0,
            'BASELINE_INSTALL_FAILURE_WITH_NO_PACKAGE_STILL_ATTEMPTS_CLEAN', result)
    result = run_case(script, uninstall_fail=True)
    require(result['code'] == 1 and not result['cleanInstalled']
            and result['phaseResults']['upgrade']['exitCode'] == 0
            and result['phaseResults']['clean']['exitCode'] == 37,
            'EXISTING_PACKAGE_UNINSTALL_FAILURE_CANNOT_MASQUERADE_AS_CLEAN', result)
    result = run_case(script, serial='physical-phone')
    require(result['code'] != 0 and not result['cleanInstalled'] and result['phaseResults'] is None,
            'PHYSICAL_PHONE_REJECTED_BEFORE_INSTALL', result)
    old = subprocess.check_output(['git', 'show', OLD_SOURCE + ':tools/r29/emulator_smoke.sh'], cwd=ROOT, text=True)
    result = run_case(old, protocol_fail='upgrade')
    require(result['code'] == 23 and not result['cleanInstalled'],
            'OLD_116888FE_REPRODUCES_SKIPPED_CLEAN_REGRESSION', result)
    print('R29_SMOKE_PHASES|ALL_PASS|checks=10|OFFLINE_COMMAND_CONTROL_FLOW_ONLY_NOT_ANDROID_ACCEPTANCE')


if __name__ == '__main__':
    main()
