#!/usr/bin/env python3
"""Exercise actual Gradle release guards with explicitly synthetic identities.

These task-graph tests do not produce or authenticate a customer release APK.
The complete Android/unit/lint build is a separate required gate.
"""
import argparse
import hashlib
import json
import os
from pathlib import Path
import subprocess
import tempfile


ROOT = Path(__file__).resolve().parents[2]


def main():
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('--out', type=Path, required=True)
    a = p.parse_args()
    a.out.mkdir(parents=True, exist_ok=True)
    environment = {k: v for k, v in os.environ.items()
                   if k != 'DUOQU_API_KEY' and not k.startswith('STARCORE_SIGNING_')
                   and k != 'STARCORE_EXPECTED_CERT_SHA256'}
    receipts = []
    with tempfile.TemporaryDirectory(prefix='starcore-synthetic-signing-test-') as temporary:
        temp = Path(temporary)
        keystore = temp / 'synthetic-only.p12'
        password = 'synthetic-fixture-only'
        subprocess.run(['keytool', '-genkeypair', '-alias', 'fixture-only', '-keyalg', 'RSA',
                        '-keystore', str(keystore), '-storetype', 'PKCS12', '-storepass', password,
                        '-keypass', password, '-dname', 'CN=Synthetic Guard Fixture', '-validity', '1'],
                       check=True, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
        certificate = subprocess.check_output(['keytool', '-exportcert', '-alias', 'fixture-only',
                                              '-keystore', str(keystore), '-storepass', password])
        fixture = {'DUOQU_API_KEY': 'SYNTHETICCONFIGONLY', 'STARCORE_SIGNING_P12': str(keystore),
                   'STARCORE_SIGNING_PASSWORD': password, 'STARCORE_SIGNING_KEY_PASSWORD': password,
                   'STARCORE_SIGNING_ALIAS': 'fixture-only', 'STARCORE_SIGNING_TYPE': 'PKCS12',
                   'STARCORE_EXPECTED_CERT_SHA256': hashlib.sha256(certificate).hexdigest()}
        broken = temp / 'unreadable.p12'
        broken.write_bytes(b'INTENTIONALLY_INVALID_SYNTHETIC_KEYSTORE')
        cases = [(name, task, {}, 'R995_RELEASE_DEFAULT_API_KEY_REQUIRED') for name, task in
                 [('assemble-missing-api', ':app:assembleRelease'),
                  ('bundle-missing-api', ':app:bundleRelease'),
                  ('package-missing-api', ':app:packageRelease'),
                  ('graph-build-missing-api', ':app:build')]]
        cases.extend([
            ('missing-signing', ':app:assembleRelease', {'DUOQU_API_KEY': fixture['DUOQU_API_KEY']},
             'CP12_ORIGINAL_RELEASE_SIGNING_INPUTS_REQUIRED'),
            ('unreadable-signing', ':app:assembleRelease', dict(fixture, STARCORE_SIGNING_P12=str(broken)),
             'CP12_ORIGINAL_RELEASE_KEY_UNREADABLE'),
            ('wrong-alias', ':app:assembleRelease', dict(fixture, STARCORE_SIGNING_ALIAS='missing-fixture-alias'),
             'CP12_ORIGINAL_RELEASE_PRIVATE_KEY_REQUIRED'),
            ('wrong-certificate', ':app:assembleRelease', dict(fixture, STARCORE_EXPECTED_CERT_SHA256='0' * 64),
             'CP12_ORIGINAL_RELEASE_CERTIFICATE_MISMATCH'),
            ('synthetic-trusted-config-only', ':app:assembleRelease', fixture, None)])
        for name, task, values, expected_error in cases:
            executable = os.environ.get('STARCORE_GRADLE_BIN')
            prefix = [executable] if executable else ['bash', './gradlew']
            command = prefix + ['--no-daemon', '--console=plain', task, '--dry-run']
            completed = subprocess.run(command, cwd=ROOT, env=dict(environment, **values),
                                       stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True, timeout=180)
            # No environment or private key bytes are retained. Gradle emits only guard codes.
            log = completed.stdout.replace(str(temp), '<SYNTHETIC_TEMP_DIRECTORY>')
            (a.out / (name + '.log')).write_text(log)
            passed = (completed.returncode != 0 and expected_error in log) if expected_error else (
                completed.returncode == 0 and 'BUILD SUCCESSFUL' in log)
            receipts.append({'id': name, 'command': command, 'exit_code': completed.returncode,
                             'expected_guard': expected_error, 'assertion_passed': passed,
                             'log_sha256': hashlib.sha256(log.encode()).hexdigest(),
                             'scope': 'ACTUAL_GRADLE_TASK_GRAPH; SYNTHETIC_SIGNING_FIXTURE; NO_APK_PACKAGING'})
    receipt = {'source_commit': subprocess.check_output(['git', 'rev-parse', 'HEAD'], cwd=ROOT).decode().strip(),
               'build_gradle_sha256': hashlib.sha256((ROOT / 'app/build.gradle').read_bytes()).hexdigest(),
               'cases': receipts, 'release_signature_status': 'ORIGINAL_SIGNING_IDENTITY_NOT_VERIFIED'}
    (a.out / 'CURRENT_RELEASE_GUARDS.json').write_text(json.dumps(receipt, indent=2) + '\n')
    if not all(d['assertion_passed'] for d in receipts):
        raise SystemExit('CURRENT_RELEASE_GUARD_ASSERTION_FAILED')
    print('CURRENT_RELEASE_GUARDS_PASS|cases=' + str(len(receipts)) + '|scope=SYNTHETIC_CONFIG_NOT_CUSTOMER_RELEASE')


if __name__ == '__main__':
    main()
