"""Execute unchanged supplied assertions with explicitly new test-only missing helpers.

ModernModelContractTest and DevelopmentTrainerContractCP9 are new fixture adapters;
they are not recovered originals. All databases are isolated synthetic test data.
"""
from pathlib import Path
import subprocess, shutil, json, hashlib
src=Path(__file__).resolve().parents[2]; root=src.parent
import os
out=root/'evidence'/os.environ.get('STARCORE_EXTENDED_EVIDENCE','extended-current-mutable'); out.mkdir(exist_ok=True)
tool=Path('/tmp/starcore-cdaf1f33/toolchain')
java=tool/'jdk-17.0.16+8/bin/java'; javac=tool/'jdk-17.0.16+8/bin/javac'
jar=tool/'android-sdk/platforms/android-36/android.jar'
schema_root=root/'evidence'/os.environ.get('STARCORE_HOST_SCHEMA','final-schema')
classes=schema_root/'classes'
bridge=src/'verification/canonical/schema/sqlite_bridge.py'
asset=src/'app/src/main/assets/cp4_atom_registry.jsonl'
sources=sorted((src/'verification/canonical/extended').glob('*.java'))
cmd=[str(javac),'-cp',str(classes)+':'+str(jar),'-d',str(classes)]+list(map(str,sources))
with (out/'compile.log').open('w') as f:
    compiled=subprocess.run(cmd,stdout=f,stderr=subprocess.STDOUT)
if compiled.returncode: raise SystemExit(compiled.returncode)
tests=['LowHeapTrainingContractCP12','DrawMatrixTrainingContractCP12',
       'HistoricalTeacherCasContractCP12','ProductionRegistrationRootCP12',
       'SemanticBirthContractCP12','EvolutionTeachersCP12Test',
       'MatrixSourcesCP12Test','AtomStudyGeneralCP12Test',
       'MatrixBindingsCP12Test','MatrixHistoricalDrawCP12Test']
receipts=[]
for name in tests:
    d=out/name
    if d.exists(): shutil.rmtree(d)
    d.mkdir(); db=d/'fixture.db'; receipt=d/'receipt.json'; files=d/'files'
    schema=(root/'evidence'/os.environ.get('STARCORE_INTEGRATED_EVIDENCE','final-integrated')/'AtomCatalogImportCP12Test/fixture.db'
            if name=='AtomStudyGeneralCP12Test' else schema_root/'schema.db')
    shutil.copy2(schema,db)
    if name=='LowHeapTrainingContractCP12': args=[receipt]
    elif name=='DrawMatrixTrainingContractCP12': args=[asset,receipt,bridge,db]
    elif name=='HistoricalTeacherCasContractCP12': args=[files,receipt,bridge,db]
    elif name in ('ProductionRegistrationRootCP12','SemanticBirthContractCP12'): args=[bridge,db,files,receipt]
    elif name=='EvolutionTeachersCP12Test': args=[bridge,db,receipt]
    else: args=[bridge,db,asset]
    cmd=[str(java),'-Xmx128m','-Djava.library.path='+str(root/'evidence'/os.environ.get('STARCORE_NATIVE_EVIDENCE','current-native')),'-Dstarcore.test.ast='+str(root/'evidence/training-fixture-ast.txt'),
         '-cp',str(classes)+':'+str(jar),'com.ethk3.novaonepro.r952.'+name]+list(map(str,args))
    with (d/'execution.log').open('w') as f:
        try: result=subprocess.run(cmd,stdout=f,stderr=subprocess.STDOUT,timeout=300); code=result.returncode
        except subprocess.TimeoutExpired: code=124
    receipts.append({'test':name,'status':'PASS' if code==0 else 'FAIL','exit_code':code,
                     'command':cmd,'supplied_test_sha256':hashlib.sha256((src/'verification/canonical/extended'/ (name+'.java')).read_bytes()).hexdigest(),
                     'scope':'Current product bytecode; unchanged supplied assertions; explicitly new missing helper adapters; synthetic host fixtures only'})
    (out/'execution_receipts.json').write_text(json.dumps(receipts,indent=2))
    print(name,receipts[-1]['status'],flush=True)
