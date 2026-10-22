#!/usr/bin/env python3
"""Exercise actual release bytecode with isolated synthetic SQL certificate evidence."""
from pathlib import Path
import argparse
import hashlib
import json
import shutil
import subprocess
import sqlite3

p = argparse.ArgumentParser()
p.add_argument('--jdk',type=Path,required=True)
p.add_argument('--schema',type=Path,required=True)
p.add_argument('--workspace', type=Path)
p.add_argument('--product-classes',type=Path)
p.add_argument('--fixture', type=Path,
               help='Optional current-bytecode qualification fixture; default builds one with actual publishers')
p.add_argument('--out', type=Path, required=True)
a = p.parse_args()
w = a.workspace.resolve() if a.workspace else Path.cwd()
r = Path(__file__).resolve().parents[2]
o = a.out.resolve()
o.mkdir(parents=True, exist_ok=False)
classes = o / 'classes'
classes.mkdir()
java = a.jdk.resolve() / 'bin/java'
parts=(a.schema / 'host-classpath').read_text().strip().split(':')
if a.product_classes:parts[1]=str(a.product_classes.resolve())
cp = ':'.join([str(classes),*parts])
sources = [r / 'verification' / path for path in (
    'r983/android/database/sqlite/SQLiteDatabase.java',
    'r983/android/os/Process.java',
    'r983/CertificateMainlineProbeR982.java',
    'r983/CertificateMainlineGuardsR982.java',
    'r70/CertifiedBetClosureR70.java',
    'r77/CertifiedEveryPeriodR77.java',
    'r77/CertificateScopeR77.java',
    'r982/FrozenAtomFixtureR982.java',
    'r77/PairedCertificateFixtureR77.java',
    'r997/TestAstR997.java',
    'canonical/extended/ModernModelContractTest.java',
    'canonical/adapters/android/os/SystemClock.java',
    'canonical/adapters/android/os/Debug.java')]
compile_command = [str(java.with_name('javac')), '-cp', cp, '-d', str(classes),
                   *map(str, sources)]
with (o / 'compile.log').open('w') as log:
    subprocess.run(compile_command, stdout=log, stderr=subprocess.STDOUT,
                   check=True, timeout=90)
fixture=a.fixture
if fixture is None:
    ast=o/'ast.txt';asset=r/'app/src/main/assets/cp4_atom_registry.jsonl'
    subprocess.run([str(java),'-cp',cp,'com.ethk3.novaonepro.r952.TestAstR997',str(asset),str(ast)],check=True,stdout=subprocess.DEVNULL)
    fixture=o/'qualification.db';shutil.copy2(a.schema/'schema-complete.db',fixture)
    required={x['atomId'] for x in json.loads(ast.read_text()[5:])['nodes'] if x.get('atomId')}
    with sqlite3.connect(fixture) as connection:
        for line in asset.open():
            x=json.loads(line)
            if x['atomId'] not in required:continue
            s=x['spec'];wire=json.dumps(s,sort_keys=True,separators=(',',':'),ensure_ascii=False)
            connection.execute('INSERT INTO research_atom_spec_v2(atom_id,family,spec_version,params_json,position,output_type,min_history,spec_hash,missing_policy,state) VALUES(?,?,?,?,?,?,?,?,?,?)',[x['atomId'],s['family'],s['version'],wire,s['position'],x['outputType'],x['minimumHistory'],x['specHash'],x['missingPolicy'],x['state']])
    with (o/'qualification.log').open('w') as f:
        result=subprocess.run([str(java),'-Xmx512m','-Dstarcore.test.ast='+str(ast),'-cp',cp,'com.ethk3.novaonepro.CertificateScopeR77',str(r/'verification/canonical/schema/sqlite_bridge.py'),str(fixture)],stdout=f,stderr=subprocess.STDOUT,timeout=240)
    assert result.returncode==0,(o/'qualification.log').read_text()
receipt = dict(scope='SYNTHETIC_HOST_ACTUAL_PRODUCT_NO_DEVICE_OR_USER_BET',
               fixture_sha256=hashlib.sha256(fixture.read_bytes()).hexdigest(),
               product_classes=parts[1],
               sources={str(s.relative_to(r)): hashlib.sha256(s.read_bytes()).hexdigest()
                        for s in sources}, compile_command=compile_command, tests=[])
for name, mode in [('CertificateMainlineProbeR982', ''),
                   ('CertificateMainlineGuardsR982', 'repeat'),
                   ('CertificateMainlineGuardsR982', 'revoke'),
                   ('CertificateMainlineGuardsR982', 'draw'),
                   ('CertifiedBetClosureR70', ''),
                   ('CertifiedEveryPeriodR77', '')]:
    key = name + ('-' + mode if mode else '')
    db = o / (key + '.db')
    shutil.copyfile(fixture, db)
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
