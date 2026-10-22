#!/usr/bin/env python3
"""Run exact service lease/history methods against real SQLite; no product test doubles.
--development-dependencies explicitly marks an incomplete development overlay, never final.
Final use requires a complete sealed --classes directory, --final-bytecode, and omits that option.
"""
import argparse,hashlib,json,shutil,subprocess
from pathlib import Path
p=argparse.ArgumentParser()
for k in ('jdk','classes','adapter-classes','android-runtime','database','out'):
    p.add_argument('--'+k,type=Path,required=True)
p.add_argument('--development-dependencies',type=Path)
p.add_argument('--final-bytecode',action='store_true',help='Only for the sealed final build class directory')
p.add_argument('--fixture',choices=('lease_history','fetch_admission'),default='lease_history')
p.add_argument('--expect-failure',help='Exact failed assertion/cause required for a negative control')
a=p.parse_args();assert not (a.final_bytecode and a.development_dependencies),'FINAL_PRODUCT_OVERLAY_FORBIDDEN'
root=Path(__file__).resolve().parents[2];out=a.out.resolve();out.mkdir(parents=True,exist_ok=False)
tests=out/'tests';tests.mkdir();classes=a.classes.resolve()
for source in (a.adapter_classes/'android').rglob('*.class'):
    dest=tests/source.relative_to(a.adapter_classes);dest.parent.mkdir(parents=True,exist_ok=True);shutil.copy2(source,dest)
for source in (a.adapter_classes/'com/ethk3/novaonepro').glob('HostProductR974*.class'):
    dest=tests/source.relative_to(a.adapter_classes);dest.parent.mkdir(parents=True,exist_ok=True);shutil.copy2(source,dest)
cp=':'.join(map(str,[tests,classes]+([a.development_dependencies] if a.development_dependencies else [])+[a.android_runtime]))
def hashes():
    return {str(v.relative_to(classes)):hashlib.sha256(v.read_bytes()).hexdigest()
      for name in ('AutoFetchService','AnalysisRequestStoreR36','AnalysisSingleFlightR36','DatabaseCoordinator')
      for v in sorted((classes/'com/ethk3/novaonepro').glob(name+'*.class'))}
before=hashes(); records=[]
def run(name,cmd):
    log=out/(name+'.log')
    with log.open('w') as stream:r=subprocess.run(list(map(str,cmd)),cwd=root,stdout=stream,stderr=subprocess.STDOUT)
    records.append({'name':name,'exit_code':r.returncode,'log_sha256':hashlib.sha256(log.read_bytes()).hexdigest()})
    (out/'steps.json').write_text(json.dumps(records,indent=2)+'\n');return r.returncode
rc=run('compile',[a.jdk/'bin/javac','-sourcepath',out/'empty-sourcepath','-cp',cp,'-d',tests,root/('verification/r77/'+a.fixture+'.java')])
if rc:print((out/'compile.log').read_text());raise SystemExit(rc)
collisions=[str(v.relative_to(tests)) for v in tests.rglob('*.class') if (classes/v.relative_to(tests)).is_file()]
assert not collisions,'PRODUCT_OVERLAY_FORBIDDEN:'+str(collisions)
db=out/'lease.db';shutil.copy2(a.database,db)
rc=run('test',[a.jdk/'bin/java','-cp',cp,'com.ethk3.novaonepro.'+a.fixture,root/'verification/canonical/schema/sqlite_bridge.py',db,out/'result.json'])
after=hashes();assert before and before==after
result={'status':'PASS' if rc==0 else 'FAIL','test_exit':rc,'scope':'DEVELOPMENT_SOURCE_OVERLAY_WITH_EXPLICIT_DEPENDENCIES' if a.development_dependencies else ('FINAL_BYTECODE_NO_PRODUCT_OVERLAY' if a.final_bytecode else 'COMPLETE_DEVELOPMENT_CLASSES_NO_PRODUCT_OVERLAY'),'classes':str(classes),'fixture':a.fixture,'fixture_sha256':hashlib.sha256((root/('verification/r77/'+a.fixture+'.java')).read_bytes()).hexdigest(),'unchanged':before==after,'product_class_collisions':collisions,'before':before,'after':after}
if a.expect_failure:
    assert rc!=0 and a.expect_failure in (out/'test.log').read_text(),'NEGATIVE_CONTROL_DID_NOT_FAIL_AS_EXPECTED'
    result['status']='EXPECTED_FAILURE';result['expected_failure']=a.expect_failure
(out/'receipt.json').write_text(json.dumps(result,indent=2)+'\n')
print((out/'test.log').read_text());raise SystemExit(0 if a.expect_failure else rc)
