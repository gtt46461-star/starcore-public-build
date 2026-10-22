#!/usr/bin/env python3
"""Host regression of actual product StorageReclaimR943 quiet owner; no product overlay.
Only Android SQLite transport is replaced, with real SQLite in another process.
"""
import argparse,hashlib,json,os,shutil,sqlite3,subprocess,time
from pathlib import Path
p=argparse.ArgumentParser();p.add_argument('--config',type=Path,required=True);p.add_argument('--schema',type=Path,required=True);p.add_argument('--out',type=Path,required=True);p.add_argument('--without-reader',action='store_true');p.add_argument('--source',type=Path);a=p.parse_args();r=Path(__file__).resolve().parents[2];c=json.loads(a.config.read_text());a.out.mkdir(parents=True,exist_ok=False)
product=a.out/'product';shutil.copytree(c['product'],product);classes=a.out/'tests';classes.mkdir();transport=a.out/'transport';transport.mkdir()
sha=lambda f:hashlib.sha256(f.read_bytes()).hexdigest()
product_hashes={str(f.relative_to(product)):sha(f) for f in product.rglob('*.class')}
original=c['classpath'].split(':');cp=':'.join(map(str,[classes,transport,*[product if Path(x)==Path(c['product']) else x for x in original]]))
java=Path(c['java']);javac=Path(c['javac']);adapter=r/'verification/r64/adapters/android/database/sqlite/SQLiteDatabase.java';harness=r/'verification/r79/ReclaimResumeR79.java'
for label,sources,target in [('adapter',[adapter],transport),('harness',[harness],classes)]:
    with (a.out/(label+'-compile.log')).open('w') as f:subprocess.run([str(javac),'-encoding','UTF-8','-sourcepath',str(a.out/'empty'),'-cp',cp,'-d',str(target),*map(str,sources)],stdout=f,stderr=subprocess.STDOUT,check=True)
for path in [classes,transport]:
    assert not set(product_hashes).intersection(str(f.relative_to(path)) for f in path.rglob('*.class')),'PRODUCT_OVERLAY'
fixture=a.out/'reclaim.db';shutil.copyfile(a.schema,fixture)
with sqlite3.connect(fixture) as d:
    d.execute('PRAGMA journal_mode=DELETE');d.execute('PRAGMA auto_vacuum=INCREMENTAL');d.execute('VACUUM')
    d.execute('CREATE TABLE r79_preserved_sentinel(id INTEGER PRIMARY KEY,payload BLOB NOT NULL)');d.execute('INSERT INTO r79_preserved_sentinel VALUES(1,?)',(bytes(range(256))*32,))
    d.execute('CREATE TABLE r79_disposable_fixture(id INTEGER PRIMARY KEY,payload BLOB NOT NULL)')
    for i in range(32):d.execute('INSERT INTO r79_disposable_fixture VALUES(?,?)',(i,bytes([i])*1024*1024))
    d.commit();filled=fixture.stat().st_size
    # Only newly created test padding is deleted to form a measurable freelist.
    # The tested product owner itself never deletes records.
    d.execute('DELETE FROM r79_disposable_fixture');d.commit();free=d.execute('PRAGMA freelist_count').fetchone()[0];assert free>256
    d.execute("DELETE FROM app_state WHERE state_key IN('r943_storage_reclaim_at','r943_storage_reclaim')");d.commit()
results=[]
for mode in ['initial','resume']+([] if a.without_reader else ['reader']):
    cmd=[str(java),'-Xmx256m','-cp',cp,'com.ethk3.novaonepro.ReclaimResumeR79',mode,str(r/'verification/r79/retention_sqlite_bridge.py'),str(fixture),str(a.out/(mode+'.json'))]
    start=time.monotonic()
    with (a.out/(mode+'.log')).open('w') as f:q=subprocess.run(cmd,stdout=f,stderr=subprocess.STDOUT)
    results.append({'mode':mode,'exit':q.returncode,'seconds':time.monotonic()-start})
    if q.returncode:print((a.out/(mode+'.log')).read_text()[-5000:]);break
    results[-1]['result']=json.loads((a.out/(mode+'.json')).read_text())
current={str(f.relative_to(product)):sha(f) for f in product.rglob('*.class')};assert current==product_hashes,'PRODUCT_CHANGED'
source=a.source or r/'app/src/main/java/com/ethk3/novaonepro/StorageReclaimR943.java'
result={'status':'PASS' if all(x['exit']==0 for x in results) else 'FAIL','scope':'ACTUAL_GRADLE_PRODUCT_OWNER_SQLITE_HOST_SYNTHETIC_FIXTURE_NOT_DEVICE','product_overlay':False,'source_sha256':sha(source),'class_sha256':sha(product/'com/ethk3/novaonepro/StorageReclaimR943.class'),'product_hashes':product_hashes,'fixture_bytes_before':filled,'fixture_free_pages_before':free,'transport_sha256':sha(adapter),'test_sha256':sha(harness),'runs':results}
(a.out/'result.json').write_text(json.dumps(result,indent=2)+'\n');print(json.dumps({k:v for k,v in result.items() if k!='product_hashes'},indent=2));raise SystemExit(0 if result['status']=='PASS' else 1)
