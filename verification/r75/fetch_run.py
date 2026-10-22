#!/usr/bin/env python3
"""Compile the actual edited fetch owners and run synthetic provider publication fixtures."""
import argparse, hashlib, json, subprocess
from pathlib import Path
p=argparse.ArgumentParser()
for key in ('jdk','classes','android-runtime','out'):
    p.add_argument('--'+key,type=Path,required=True)
p.add_argument('--source-revision',help='Optional Git revision for the same regression against baseline owners')
p.add_argument('--final-bytecode',action='store_true',help='Compile fixtures/adapters only; load all fetch owners from --classes')
a=p.parse_args(); root=Path(__file__).resolve().parents[2]; out=a.out.resolve()
if a.final_bytecode and a.source_revision:p.error('--final-bytecode cannot use --source-revision')
out.mkdir(parents=True,exist_ok=False); classes=out/'classes'; classes.mkdir()
cp=':'.join(map(str,[classes,a.classes.resolve(),a.android_runtime.resolve()]))
src=root/'app/src/main/java/com/ethk3/novaonepro'
sources=[src/(name+'.java') for name in ('ProviderFreshnessGateR3611','ApiTransportGovernanceEngine','ApiClient')]
logical_names=[str(v.relative_to(root)) for v in sources]
def product_hashes():
    base=a.classes.resolve()/'com/ethk3/novaonepro'
    return {str(v.relative_to(a.classes.resolve())):hashlib.sha256(v.read_bytes()).hexdigest()
            for name in ('ProviderFreshnessGateR3611','ApiTransportGovernanceEngine','ApiClient')
            for v in sorted(base.glob(name+'*.class'))}
before=product_hashes()
if a.final_bytecode:sources=[];logical_names=[]
if a.source_revision:
    baseline=out/'product-sources'; baseline.mkdir()
    for i,source in enumerate(sources):
        target=baseline/source.name
        target.write_bytes(subprocess.check_output(['git','show',a.source_revision+':'+logical_names[i]],cwd=root))
        sources[i]=target
sources += [root/'verification/r75/fetch_poll.java',root/'verification/r72/realtime/android/os/SystemClock.java']
logical_names += ['verification/r75/fetch_poll.java','verification/r72/realtime/android/os/SystemClock.java']
command=[str(a.jdk/'bin/javac'),'-encoding','UTF-8','-cp',cp,'-d',str(classes)]+list(map(str,sources))
with (out/'compile.log').open('w') as f:
    subprocess.run(command,stdout=f,stderr=subprocess.STDOUT,check=True)
with (out/'test.log').open('w') as f:
    res=subprocess.run([str(a.jdk/'bin/java'),'-cp',cp,'com.ethk3.novaonepro.fetch_poll',str(out/'result.json')],stdout=f,stderr=subprocess.STDOUT)
(out/'sources.json').write_text(json.dumps({name:hashlib.sha256(v.read_bytes()).hexdigest() for name,v in zip(logical_names,sources)},indent=2)+'\n')
after=product_hashes()
assert before and before==after,'PRODUCT_BYTECODE_CHANGED_DURING_TEST'
if a.final_bytecode:
    assert not any((classes/'com/ethk3/novaonepro').glob('ApiClient*.class')),'PRODUCT_OVERLAY_FORBIDDEN'
    assert not any((classes/'com/ethk3/novaonepro').glob('ProviderFreshnessGateR3611*.class')),'PRODUCT_OVERLAY_FORBIDDEN'
    assert not any((classes/'com/ethk3/novaonepro').glob('ApiTransportGovernanceEngine*.class')),'PRODUCT_OVERLAY_FORBIDDEN'
(out/'product-bytecode.json').write_text(json.dumps({'mode':'FINAL_BYTECODE_NO_PRODUCT_OVERLAY' if a.final_bytecode else 'ISOLATED_SOURCE_REGRESSION','unchanged':before==after,'before':before,'after':after},indent=2)+'\n')
print((out/'test.log').read_text()); raise SystemExit(res.returncode)
