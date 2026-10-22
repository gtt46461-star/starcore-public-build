#!/usr/bin/env python3
"""Exercise current production Android bytecode on an isolated synthetic emulator.

Creates no delivery APK or natural settlement evidence. Build tasks run separately
through the unchanged Gradle gates; artifacts from this script contain logs only.
"""
import argparse
import hashlib
import json
import os
from pathlib import Path
import subprocess
import time

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / 'validation-o09'
SDK = Path(os.environ['ANDROID_HOME'])
OUT.mkdir(exist_ok=True)
# sdkmanager/avdmanager and emulator must resolve the same explicit AVD home.
# Hosted runners may otherwise use different legacy/XDG default locations.
avd_home = Path(os.environ.get('RUNNER_TEMP', str(ROOT / 'build'))) / 'o09-avd'
avd_home.mkdir(parents=True, exist_ok=True)
os.environ['ANDROID_AVD_HOME'] = str(avd_home)
manager = next(p for p in (SDK/'cmdline-tools/latest/bin/sdkmanager',
                          SDK/'cmdline-tools/12.0/bin/sdkmanager') if p.is_file())
adb = [str(SDK/'platform-tools/adb'), '-s', 'emulator-5554']
parser = argparse.ArgumentParser(description=__doc__)
mode = parser.add_mutually_exclusive_group()
mode.add_argument('--boot-only', action='store_true')
mode.add_argument('--tests-only', action='store_true')
args = parser.parse_args()


def run(args, name, timeout=300, input_text=None):
    try:
        result = subprocess.run([str(x) for x in args], cwd=ROOT, input=input_text,
                                text=True, stdout=subprocess.PIPE, stderr=subprocess.STDOUT,
                                timeout=timeout)
    except subprocess.TimeoutExpired as error:
        partial = error.stdout or b''
        if isinstance(partial, bytes):
            partial = partial.decode('utf-8', errors='replace')
        (OUT/(name+'.log')).write_text(partial + '\nTIMEOUT\n')
        raise
    (OUT/(name+'.log')).write_text(result.stdout)
    if result.returncode:
        print(result.stdout[-6000:])
        raise RuntimeError(f'{name} exited {result.returncode}')
    return result.stdout


def one_apk(directory):
    files = list(directory.glob('*.apk'))
    if len(files) != 1:
        raise RuntimeError(f'Expected one current APK in {directory}, found {len(files)}')
    return files[0]


if not args.tests_only:
    run([manager, 'platform-tools', 'emulator', 'system-images;android-35;google_apis;x86_64'],
        'emulator-packages', timeout=900)
    run([manager.parent/'avdmanager', 'create', 'avd', '--force', '--name', 'o09-validation',
         '--path', avd_home/'o09-validation.avd',
         '--package', 'system-images;android-35;google_apis;x86_64', '--device', 'pixel_2'],
        'avd-create', input_text='no\n')
    # This API35 image has a 6 GiB userdata floor. Hosted-runner preparation frees
    # unused toolchains; lowering the requested partition cannot shrink that image.
    config = avd_home/'o09-validation.avd/config.ini'
    lines = [line for line in config.read_text().splitlines()
             if not line.startswith('disk.dataPartition.size=')]
    config.write_text('\n'.join(lines + ['disk.dataPartition.size=6442450944']) + '\n')
available = run([SDK/'emulator/emulator', '-list-avds'], 'avd-list')
if 'o09-validation' not in available.splitlines():
    raise RuntimeError('Created AVD is not visible to emulator in explicit ANDROID_AVD_HOME')
emulator_log_path = OUT / ('emulator-preflight.log' if args.boot_only else 'emulator.log')
run(['df', '-h', str(avd_home)], 'emulator-disk')
with emulator_log_path.open('w') as emulator_log:
    emulator = subprocess.Popen([str(SDK/'emulator/emulator'), '-avd', 'o09-validation',
            '-no-window', '-no-audio', '-no-boot-anim', '-no-snapshot', '-no-metrics',
            '-gpu', 'swiftshader_indirect', '-memory', '4096', '-cores', '2', '-port', '5554',
            '-partition-size', '6144'],
            stdout=emulator_log, stderr=subprocess.STDOUT)
    try:
        deadline = time.monotonic()+240
        while time.monotonic() < deadline:
            if emulator.poll() is not None:
                raise RuntimeError('Emulator exited before boot completion; see emulator.log')
            boot = subprocess.run(adb+['shell', 'getprop', 'sys.boot_completed'],
                                  stdout=subprocess.PIPE, stderr=subprocess.STDOUT,
                                  text=True, timeout=20)
            (OUT/'boot-property.log').write_text(boot.stdout)
            if boot.returncode == 0 and boot.stdout.strip() == '1':
                break
            time.sleep(2)
        else:
            raise RuntimeError('Android boot deadline exceeded')
        if args.boot_only:
            print('O09_EMULATOR_BOOT_PREFLIGHT=PASS', flush=True)
            raise SystemExit(0)
        app = one_apk(ROOT/'app/build/outputs/apk/debug')
        test = one_apk(ROOT/'app/build/outputs/apk/androidTest/debug')
        run([SDK/'build-tools/36.0.0/apksigner', 'verify', '--verbose', '--print-certs', app],
            'debug-signature-verification')
        run(adb+['shell', 'settings', 'put', 'global', 'window_animation_scale', '0'], 'animation')
        run(adb+['install', '-t', str(app)], 'install-current-debug', timeout=180)
        run(adb+['install', '-t', str(test)], 'install-current-tests', timeout=180)
        result = run(adb+['shell', 'am', 'instrument', '-w', '-r', '-e', 'class',
             'com.ethk3.novaonepro.DrawDependencyAndroidCP12Test',
             'com.ethk3.novaonepro.test/androidx.test.runner.AndroidJUnitRunner'],
             'android-rolling-instrumentation', timeout=900)
        if 'OK (1 test)' not in result or 'FAILURES!!!' in result or 'INSTRUMENTATION_FAILED' in result:
            print(result[-8000:])
            raise RuntimeError('Current Android rolling contract did not pass')
        summary = {'status': 'PASS', 'scope': 'ANDROID_EMULATOR_SYNTHETIC_ROLLING_ONLY',
                   'commit': subprocess.check_output(['git','rev-parse','HEAD'], cwd=ROOT, text=True).strip(),
                   'api': 35, 'abi': 'x86_64', 'apk_sha256': hashlib.sha256(app.read_bytes()).hexdigest(),
                   'natural': 'NOT_PROVEN', 'physical_device': 'NOT_RUN', 'delivery_apk': 'WITHHELD'}
        (OUT/'android-rolling-result.json').write_text(json.dumps(summary, indent=2)+'\n')
        print(json.dumps(summary))
    finally:
        try:
            run(adb+['logcat', '-d', '-v', 'threadtime'], 'android-logcat', timeout=30)
        except (subprocess.TimeoutExpired, RuntimeError) as error:
            print('LOGCAT_UNAVAILABLE|' + str(error))
        finally:
            # Stop the Android/qemu child through its own emulator transport.
            # Terminating only the launcher can leave emulator-5554 registered.
            if emulator.poll() is None:
                subprocess.run(adb + ['emu', 'kill'], stdout=subprocess.PIPE,
                               stderr=subprocess.STDOUT, timeout=20)
            try:
                emulator.wait(timeout=30)
            except subprocess.TimeoutExpired:
                emulator.kill()
                emulator.wait(timeout=10)
            deadline = time.monotonic() + 30
            while time.monotonic() < deadline:
                devices = subprocess.check_output([str(SDK/'platform-tools/adb'), 'devices'], text=True)
                if 'emulator-5554' not in devices:
                    break
                time.sleep(1)
            else:
                raise RuntimeError('Owned emulator transport did not disappear after shutdown')
            emulator_log.flush()
            print('O09_EMULATOR_LOG_BEGIN\n' + emulator_log_path.read_text(errors='replace')[-16000:]
                  + '\nO09_EMULATOR_LOG_END', flush=True)

