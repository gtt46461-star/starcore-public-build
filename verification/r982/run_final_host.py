"""Compile final product bytecode adapters and unchanged acceptance assertions, fresh fixtures only."""
from pathlib import Path
import os,json,subprocess,re,shutil
src=Path(__file__).resolve().parents[2];root=src.parent
tool=Path('/tmp/starcore-cdaf1f33/toolchain');jdk=tool/'jdk-17.0.16+8/bin';cl=root/'evidence/final-schema/classes';jar=tool/'android-sdk/platforms/android-36/android.jar'
out=root/'evidence/final-host';out.mkdir(exist_ok=True)
adapters=list((src/'verification/canonical/adapters').rglob('*.java'))
tests=[src/p for p in json.loads((src/'verification/canonical/TEST_SOURCES.json').read_text())]
missing=[];valid=[]
for f in tests:
 if 'TrainingFixtureCP11' in f.read_text():missing.append(f)
 else:valid.append(f)
valid+=list((src/'verification/r982').glob('*.java'))
cmd=[str(jdk/'javac'),'-encoding','UTF-8','-cp',str(cl)+':'+str(jar),'-d',str(cl)]+list(map(str,adapters+valid))
with (out/'compile.log').open('w') as log:p=subprocess.run(cmd,stdout=log,stderr=subprocess.STDOUT)
(out/'compile-command.json').write_text(json.dumps(cmd,indent=2))
if p.returncode:raise SystemExit(p.returncode)
driver=(src/'verification/canonical/run_current_host.py').read_text().replace("evidence/current-schema","evidence/final-schema").replace("evidence/current-host","evidence/final-host").replace("Path(__file__).resolve().parents[2]","Path("+repr(str(src))+")").replace("Path(__file__).resolve().parent", "Path("+repr(str(src/'verification/canonical'))+")")
# Keep original per-case assertions and explicit missing-fixture NOT_RUN disposition.
exec(compile(driver,str(src/'verification/canonical/run_current_host.py'),'exec'),{'__file__':str(src/'verification/canonical/run_current_host.py')})
