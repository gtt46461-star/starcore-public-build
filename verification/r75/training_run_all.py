"""Real product owner regression on SQLite host fixtures; does not access a phone."""
from pathlib import Path
import argparse
import hashlib
import json
import shutil
import subprocess
import sys

p = argparse.ArgumentParser()
for name in ('schema', 'classes', 'jdk', 'out', 'report'):
    p.add_argument('--' + name, type=Path, required=True)
p.add_argument('--native', type=Path)
p.add_argument('--overlay', type=Path, action='append', default=[],
               help='Optional explicitly recorded product bytecode overlays; omit after final build.')
a = p.parse_args()
r = Path(__file__).resolve().parents[2]
o = a.out.resolve()
o.mkdir(parents=True, exist_ok=True)
cl = o / 'classes'
cl.mkdir(exist_ok=True)
original = (a.schema / 'host-classpath').read_text().strip().split(':')
cp = ':'.join(map(str, [cl, a.schema.resolve() / 'classes',
                      *[x.resolve() for x in a.overlay], a.classes.resolve(), original[-1]]))
bridge = r / 'verification/canonical/schema/sqlite_bridge.py'
native = (a.native or r.parent / 'host-native').resolve()
assets = r / 'app/src/main/assets'
sources = [r / 'verification/r75' / (name + '.java') for name in (
    'training_DirectorBudgetR75', 'training_OutboxRecoveryR75', 'training_ReportModernR75')]
sources.append(r / 'verification/r72/realtime/android/os/SystemClock.java')
commands = []


def run(command, log):
    command = list(map(str, command))
    commands.append(command)
    with (o / log).open('w') as f:
        subprocess.run(command, stdout=f, stderr=subprocess.STDOUT, check=True)


run([a.jdk / 'bin/javac', '-cp', cp, '-d', cl, *sources], 'compile.log')
base = [a.jdk / 'bin/java', '-Djava.library.path=' + str(native), '-cp', cp]
shutil.copy2(a.schema / 'schema-complete.db', o / 'director.db')
run([*base, 'com.ethk3.novaonepro.training_DirectorBudgetR75', bridge,
     o / 'director.db', o / 'director.json'], 'director.log')
shutil.copy2(a.schema / 'schema-complete.db', o / 'outbox.db')
run([*base, 'com.ethk3.novaonepro.training_OutboxRecoveryR75', bridge,
     o / 'outbox.db', o / 'outbox.json', assets], 'outbox.log')
run([sys.executable, r / 'verification/r75/training_prepare_report.py',
     '--schema', a.schema, '--report', a.report, '--out', o / 'modern'], 'fixture.log')
run([*base, 'com.ethk3.novaonepro.training_ReportModernR75', bridge,
     o / 'modern/family-report.db', o / 'modern/inputs', assets, o / 'modern.json'], 'modern.log')
results = {key: json.loads((o / (key + '.json')).read_text())
           for key in ('director', 'outbox', 'modern')}
manifest = {
    'status': 'PASS', 'results': results, 'commands': commands,
    'report_sha256': hashlib.sha256(a.report.read_bytes()).hexdigest(),
    'sources': {str(x.relative_to(r)): hashlib.sha256(x.read_bytes()).hexdigest() for x in sources},
    'product_classes': str(a.classes.resolve()),
    'explicit_product_overlays': [str(x.resolve()) for x in a.overlay],
    'scope': 'HOST_OWNER_RECOVERY_AND_OBSERVED_REPORT_DEVELOPMENT_NOT_PHONE_OR_PREDICTIVE_QUALITY',
}
(o / 'result.json').write_text(json.dumps(manifest, indent=2) + '\n')
print(json.dumps(results, indent=2))
