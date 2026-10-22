#!/usr/bin/env python3
"""Exercise actual DbStore/coordinator batch updates against SQLite, with no final product overlay."""
import argparse
import hashlib
import json
from pathlib import Path
import shutil
import subprocess

p = argparse.ArgumentParser(description=__doc__)
for key in ('jdk', 'classes', 'adapter-classes', 'android-runtime', 'database', 'out'):
    p.add_argument('--' + key, type=Path, required=True)
p.add_argument('--development-dependencies', type=Path)
p.add_argument('--final-bytecode', action='store_true')
p.add_argument('--expect-failure', help='Required exact failure marker for an old-code negative control')
p.add_argument('--case', choices=('all', 'direct-minus-one', 'background-slice'), default='all')
a = p.parse_args()
assert not (a.final_bytecode and a.development_dependencies), 'FINAL_PRODUCT_OVERLAY_FORBIDDEN'
root = Path(__file__).resolve().parents[2]
out = a.out.resolve()
out.mkdir(parents=True, exist_ok=False)
tests = out / 'tests'
tests.mkdir()
classes = a.classes.resolve()
for source in (a.adapter_classes / 'android').rglob('*.class'):
    dest = tests / source.relative_to(a.adapter_classes)
    dest.parent.mkdir(parents=True, exist_ok=True)
    shutil.copy2(source, dest)
for source in (a.adapter_classes / 'com/ethk3/novaonepro').glob('HostProductR974*.class'):
    dest = tests / source.relative_to(a.adapter_classes)
    dest.parent.mkdir(parents=True, exist_ok=True)
    shutil.copy2(source, dest)
cp = ':'.join(map(str, [tests, classes]
    + ([a.development_dependencies] if a.development_dependencies else []) + [a.android_runtime]))

def hashes():
    return {str(path.relative_to(classes)): hashlib.sha256(path.read_bytes()).hexdigest()
            for name in ('DbStore', 'DatabaseCoordinator', 'Models')
            for path in sorted((classes / 'com/ethk3/novaonepro').glob(name + '*.class'))}

before = hashes()
assert any(name.endswith('/DbStore.class') for name in before), 'DBSTORE_BYTECODE_REQUIRED'
steps = []

def run(name, command):
    log = out / (name + '.log')
    with log.open('w') as stream:
        result = subprocess.run(list(map(str, command)), cwd=root,
                                stdout=stream, stderr=subprocess.STDOUT, timeout=180)
    steps.append({'name': name, 'exit_code': result.returncode,
                  'log_sha256': hashlib.sha256(log.read_bytes()).hexdigest()})
    (out / 'steps.json').write_text(json.dumps(steps, indent=2) + '\n')
    return result.returncode

rc = run('compile', [a.jdk / 'bin/javac', '-sourcepath', out / 'empty-sourcepath',
    '-cp', cp, '-d', tests, root / 'verification/r77/runtime_state_batch.java',
    root / 'verification/canonical/adapters/android/os/SystemClock.java'])
if rc:
    print((out / 'compile.log').read_text())
    raise SystemExit(rc)
collisions = [str(path.relative_to(tests)) for path in tests.rglob('*.class')
              if (classes / path.relative_to(tests)).is_file()]
assert not collisions, 'PRODUCT_OVERLAY_FORBIDDEN:' + str(collisions)
database = out / 'runtime-state.db'
shutil.copy2(a.database, database)
rc = run('test', [a.jdk / 'bin/java', '-cp', cp,
    'com.ethk3.novaonepro.runtime_state_batch',
    root / 'verification/canonical/schema/sqlite_bridge.py', database, out / 'result.json', a.case])
after = hashes()
assert before == after, 'PRODUCT_BYTECODE_CHANGED_DURING_TEST'
result = {'status': 'PASS' if rc == 0 else 'FAIL', 'test_exit': rc, 'case': a.case,
          'clock': 'REAL_JVM_MONOTONIC_NANOTIME_NOT_ANDROID_PHONE_LATENCY',
          'clock_source_sha256': hashlib.sha256((root / 'verification/canonical/adapters/android/os/SystemClock.java').read_bytes()).hexdigest(),
          'scope': ('DEVELOPMENT_SOURCE_OVERLAY_WITH_EXPLICIT_DEPENDENCIES'
                    if a.development_dependencies else 'FINAL_BYTECODE_NO_PRODUCT_OVERLAY'
                    if a.final_bytecode else 'COMPLETE_DEVELOPMENT_CLASSES_NO_PRODUCT_OVERLAY'),
          'classes': str(classes), 'unchanged': True, 'product_class_collisions': collisions,
          'before': before, 'after': after}
if a.expect_failure:
    assert rc != 0 and a.expect_failure in (out / 'test.log').read_text(), \
        'NEGATIVE_CONTROL_DID_NOT_FAIL_AS_EXPECTED'
    result['status'] = 'EXPECTED_FAILURE'
    result['expected_failure'] = a.expect_failure
(out / 'receipt.json').write_text(json.dumps(result, indent=2) + '\n')
print((out / 'test.log').read_text())
raise SystemExit(0 if a.expect_failure else rc)
