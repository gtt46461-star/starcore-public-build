#!/usr/bin/env python3
"""Run the unchanged 20-check fixture against final release bytecode, never a product overlay."""
from pathlib import Path
import argparse, subprocess, sqlite3, shutil, json, hashlib

p=argparse.ArgumentParser()
for name in ('repo','schema','classes','jdk','out'):
    p.add_argument('--'+name,type=Path,required=True)
a=p.parse_args()
repo=a.repo.resolve(); host=a.schema.resolve(); product=a.classes.resolve(); out=a.out.resolve()
out.mkdir(); fixture=out/'classes'; fixture.mkdir()
android=(host/'host-classpath').read_text().strip().split(':')[-1]
cp=':'.join((str(fixture),str(host/'classes'),str(product),android))
java=a.jdk.resolve()/'bin'
compile_command=[str(java/'javac'),'-sourcepath',str(out/'empty-sourcepath'),'-cp',cp,'-d',str(fixture),
    str(repo/'verification/r75/RetentionRevisionR75.java'),
    str(repo/'verification/r73/adapters/android/system/Os.java')]
with (out/'compile.log').open('w') as f:
    subprocess.run(compile_command,stdout=f,stderr=subprocess.STDOUT,check=True)
collisions=[str(f.relative_to(base)) for base in (fixture,host/'classes')
    for f in base.rglob('*.class') if (product/f.relative_to(base)).is_file()]
assert not collisions,'PRODUCT_CLASS_OVERLAY_FORBIDDEN:'+str(collisions)
namespace=product/'com/ethk3/novaonepro'
def product_hashes():
    return {str(f.relative_to(product)):hashlib.sha256(f.read_bytes()).hexdigest()
        for f in sorted(namespace.glob('*.class'))
        if f.name.startswith(('DataRetentionGovernorR900','Retention','DatabaseCoordinator'))}
before_classes=product_hashes()
assert before_classes,'EMPTY_PRODUCT_CLASS_MANIFEST'
db=out/'live.db'; shutil.copy2(host/'schema-complete.db',db)
with sqlite3.connect(db) as connection:
    for i in range(1,301):
        connection.execute("INSERT INTO diagnostic_log(rowid,log_key,created_at,level,module,code,message,detail) VALUES(?,?,1,'INFO','R75','HOST','test','')",(i,'live-'+str(i)))
    for i in range(1001,1261):
        connection.execute("INSERT INTO research_row_revision_v2(source_table,source_rowid) VALUES('diagnostic_log',?)",(i,))
command=[str(java/'java'),'-Xmx128m','-cp',cp,'com.ethk3.novaonepro.RetentionRevisionR75',
    str(repo/'verification/canonical/schema/sqlite_bridge.py'),str(db),str(out/'result.json')]
with (out/'test.log').open('w') as f:
    result=subprocess.run(command,stdout=f,stderr=subprocess.STDOUT)
classes=product_hashes()
assert before_classes==classes,'PRODUCT_CLASSES_CHANGED_DURING_RUN'
receipt={'exit_code':result.returncode,'command':command,'compile_command':compile_command,
    'product_class_root':str(product),'product_overlay_compiled':False,'class_collisions':collisions,
    'product_classes_unchanged':before_classes==classes,'product_classes_sha256':classes,
    'fixture_sha256':hashlib.sha256((repo/'verification/r75/RetentionRevisionR75.java').read_bytes()).hexdigest(),
    'schema_sha256':hashlib.sha256((host/'schema-complete.db').read_bytes()).hexdigest(),
    'log_sha256':hashlib.sha256((out/'test.log').read_bytes()).hexdigest()}
(out/'run.json').write_text(json.dumps(receipt,indent=2)+'\n')
print((out/'test.log').read_text())
raise SystemExit(result.returncode)
