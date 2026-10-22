#!/usr/bin/env python3
"""Verify pre-scope observers with actual Gradle classes and no product overlays."""
from pathlib import Path
import argparse
import hashlib
import json
import shutil
import subprocess

ROOT = Path(__file__).resolve().parents[3]
HERE = Path(__file__).resolve().parent
parser = argparse.ArgumentParser()
parser.add_argument('--config', type=Path, required=True)
parser.add_argument('--schema', type=Path, required=True)
parser.add_argument('--out', type=Path, required=True)
parser.add_argument('--baseline-product', type=Path)
parser.add_argument('--baseline-only', action='store_true')
args = parser.parse_args()
config = json.loads(args.config.read_text())
product = Path(config['product']).resolve()
out = args.out.resolve()
out.mkdir(parents=True, exist_ok=False)
tests = out / 'tests'
tests.mkdir()
empty = out / 'empty'
empty.mkdir()

def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()

def manifest(root):
    return {str(p.relative_to(root)): sha(p) for p in sorted(root.rglob('*.class'))}

original = manifest(product)
assert 'com/ethk3/novaonepro/RuntimeResourceAuditR927.class' in original, 'GRADLE_PRODUCT_REQUIRED'
assert 'com/ethk3/novaonepro/AutoFetchService.class' in original, 'ACTUAL_SERVICE_REQUIRED'
classpath = ':'.join([str(tests), config['classpath']])
assert str(product) in [str(Path(p).resolve()) for p in config['classpath'].split(':')], 'PRODUCT_NOT_IN_CONFIG'
sources = [HERE / 'CompletedStageObservationR79.java', ROOT / 'verification/r983/android/os/Debug.java']
compile_command = [config['javac'], '-encoding', 'UTF-8', '-sourcepath', str(empty),
                   '-cp', classpath, '-d', str(tests), *map(str, sources)]
(out / 'compile-command.json').write_text(json.dumps(compile_command, indent=2) + '\n')
with (out / 'compile.log').open('w') as log:
    subprocess.run(compile_command, stdout=log, stderr=subprocess.STDOUT, check=True)
for entry in classpath.split(':'):
    candidate = Path(entry).resolve()
    if candidate.is_dir() and candidate != product:
        assert not set(original).intersection(manifest(candidate)), 'PRODUCT_OVERLAY: ' + str(candidate)

results = []
products = {'current': {'path': str(product), 'sha256': original}}

def run(label, selected_product, before):
    selected_product = selected_product.resolve()
    selected_manifest = manifest(selected_product)
    products[label] = {'path': str(selected_product), 'sha256': selected_manifest}
    cp = ':'.join(str(selected_product) if Path(p).resolve() == product else p
                  for p in classpath.split(':'))
    for entry in cp.split(':'):
        candidate = Path(entry).resolve()
        if candidate.is_dir() and candidate != selected_product:
            assert not set(selected_manifest).intersection(manifest(candidate)), 'BASELINE_PRODUCT_OVERLAY'
    db = out / (label + '.db')
    shutil.copy2(args.schema, db)
    command = [config['java'], '-Xmx256m', '-cp', cp,
               'com.ethk3.novaonepro.CompletedStageObservationR79', config['bridge'],
               str(db), 'before' if before else 'repair']
    (out / (label + '-command.json')).write_text(json.dumps(command, indent=2) + '\n')
    with (out / (label + '.log')).open('w') as log:
        result = subprocess.run(command, stdout=log, stderr=subprocess.STDOUT, timeout=180)
    text = (out / (label + '.log')).read_text()
    assert result.returncode == 0, label + ': ' + text[-5000:]
    report = json.loads(text.splitlines()[-1])
    report.update({'label': label, 'exit_code': result.returncode,
                   'log_sha256': sha(out / (label + '.log'))})
    assert manifest(selected_product) == selected_manifest, 'PRODUCT_CHANGED_DURING_RUN'
    results.append(report)

if args.baseline_product:
    run('remote-r79-before', args.baseline_product, True)
if args.baseline_only:
    assert args.baseline_product, '--baseline-only requires --baseline-product'
else:
    # Check the actual service bytecode calls the helper exactly at the three repaired sites.
    javap = str(Path(config['java']).with_name('javap'))
    command = [javap, '-c', '-p', '-classpath', str(product), 'com.ethk3.novaonepro.AutoFetchService']
    (out / 'service-bytecode-command.json').write_text(json.dumps(command, indent=2) + '\n')
    bytecode = subprocess.check_output(command, text=True)
    (out / 'service-bytecode.txt').write_text(bytecode)
    assert bytecode.count('RuntimeResourceAuditR927.completedBeforeScopeR79:') == 3, 'SERVICE_CALLSITE_COUNT'
    run('current-after', product, False)
assert manifest(product) == original, 'CURRENT_PRODUCT_CHANGED_DURING_TEST'
(out / 'product_hashes.json').write_text(json.dumps(products, indent=2) + '\n')
summary = {'scope': 'ACTUAL_GRADLE_PRODUCT_HOST_SYNTHETIC_SQLITE_NOT_PHONE_LATENCY',
           'product_overlay': False, 'cpu_adapter': 'Real host ThreadMXBean; no claim about Android/ART CPU',
           'schema_sha256': sha(args.schema), 'product_classes': len(original),
           'product_manifest_sha256': hashlib.sha256(json.dumps(original, sort_keys=True).encode()).hexdigest(),
           'source_hashes': {str(p.relative_to(ROOT)): sha(p) for p in sources + [Path(__file__),
               ROOT / 'app/src/main/java/com/ethk3/novaonepro/RuntimeResourceAuditR927.java',
               ROOT / 'app/src/main/java/com/ethk3/novaonepro/AutoFetchService.java']},
           'results': results}
(out / 'summary.json').write_text(json.dumps(summary, indent=2) + '\n')
print(json.dumps(summary, indent=2))
