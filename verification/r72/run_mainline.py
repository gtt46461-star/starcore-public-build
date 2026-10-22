#!/usr/bin/env python3
"""Exercise actual release bytecode with isolated synthetic SQL certificate evidence."""
from pathlib import Path
import argparse
import hashlib
import json
import shutil
import subprocess

p = argparse.ArgumentParser()
p.add_argument('--jdk',type=Path,required=True)
p.add_argument('--schema',type=Path,required=True)
p.add_argument('--workspace', type=Path, required=True)
p.add_argument('--fixture', type=Path, required=True,
               help='Unmodified successful CertificateScopeR70.db from run_domains.py')
p.add_argument('--out', type=Path, required=True)
a = p.parse_args()
w = a.workspace.resolve()
r = Path(__file__).resolve().parents[2]
o = a.out.resolve()
o.mkdir(parents=True, exist_ok=False)
classes = o / 'classes'
classes.mkdir()
java = a.jdk.resolve() / 'bin/java'
cp = str(classes) + ':' + (a.schema / 'host-classpath').read_text().strip()
sources = [r / 'verification' / path for path in (
    'r983/android/database/sqlite/SQLiteDatabase.java',
    'r983/android/os/Process.java',
    'r983/CertificateMainlineProbeR982.java',
    'r983/CertificateMainlineGuardsR982.java',
    'r70/CertifiedBetClosureR70.java')]
compile_command = [str(java.with_name('javac')), '-cp', cp, '-d', str(classes),
                   *map(str, sources)]
with (o / 'compile.log').open('w') as log:
    subprocess.run(compile_command, stdout=log, stderr=subprocess.STDOUT,
                   check=True, timeout=90)
receipt = dict(scope='SYNTHETIC_HOST_ACTUAL_PRODUCT_NO_DEVICE_OR_USER_BET',
               fixture_sha256=hashlib.sha256(a.fixture.read_bytes()).hexdigest(),
               sources={str(s.relative_to(r)): hashlib.sha256(s.read_bytes()).hexdigest()
                        for s in sources}, compile_command=compile_command, tests=[])
for name, mode in [('CertificateMainlineProbeR982', ''),
                   ('CertificateMainlineGuardsR982', 'repeat'),
                   ('CertificateMainlineGuardsR982', 'revoke'),
                   ('CertificateMainlineGuardsR982', 'draw'),
                   ('CertifiedBetClosureR70', '')]:
    key = name + ('-' + mode if mode else '')
    db = o / (key + '.db')
    shutil.copyfile(a.fixture, db)
    command = [str(java), '-Xmx512m', '-cp', cp, 'com.ethk3.novaonepro.' + name,
               str(r / 'verification/canonical/schema/sqlite_bridge.py'), str(db)]
    if mode:
        command.append(mode)
    log = o / (key + '.log')
    with log.open('w') as f:
        result = subprocess.run(command, stdout=f, stderr=subprocess.STDOUT, timeout=120)
    receipt['tests'].append(dict(test=key, command=command, exit_code=result.returncode,
                                log_sha256=hashlib.sha256(log.read_bytes()).hexdigest()))
    (o / 'results.json').write_text(json.dumps(receipt, indent=2) + '\n')
    print(key, result.returncode, flush=True)
raise SystemExit(1 if any(t['exit_code'] for t in receipt['tests']) else 0)
