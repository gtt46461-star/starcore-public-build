#!/usr/bin/env python3
"""Actual normal Gradle product, installed schema and real SQLite; no product overlay."""
from pathlib import Path
import argparse,hashlib,json,shutil,sqlite3,subprocess,time
p=argparse.ArgumentParser()
for name in ['config','schema','out']:p.add_argument('--'+name,type=Path,required=True)
p.add_argument('--native',type=Path)
a=p.parse_args();src=Path(__file__).resolve().parent;r=src.parents[2]
c=json.loads(a.config.read_text());o=a.out.resolve();o.mkdir(parents=True,exist_ok=False);tests=o/'tests';tests.mkdir()
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();product=Path(c['product'])
before={str(x.relative_to(product)):sha(x) for x in product.rglob('*.class')};assert 'com/ethk3/novaonepro/MultiHorizonProbeR80.class' in before
cp=':'.join([str(tests),c['tests'],c['adapters'],c['product'],c['android']])
executions=[]
def execute(name,command,timeout=240):
    started=time.monotonic()
    (o/(name+'-command.json')).write_text(json.dumps(command,indent=2)+'\n')
    with (o/(name+'.log')).open('w') as f:result=subprocess.run(command,stdout=f,stderr=subprocess.STDOUT,timeout=timeout)
    executions.append({'name':name,'command':command,'exit_code':result.returncode,'elapsed_seconds':time.monotonic()-started,'log_sha256':sha(o/(name+'.log'))})
    (o/'executions.json').write_text(json.dumps(executions,indent=2)+'\n')
    if result.returncode:print((o/(name+'.log')).read_text());raise SystemExit(result.returncode)

sources=[src/'ProbeLifecycleR80.java',src/'ProbeArchiveFifoR80.java',r/'verification/r73/adapters/android/system/Os.java',r/'verification/canonical/adapters/android/util/AtomicFile.java']
execute('compile',[c['javac'],'-encoding','UTF-8','-sourcepath',str(o/'empty'),'-cp',cp,'-d',str(tests),*map(str,sources)])
for root in [tests,Path(c['tests']),Path(c['adapters'])]:assert not set(before)&{str(x.relative_to(root)) for x in root.rglob('*.class')},'PRODUCT_OVERLAY'
def fixture(name,late=False):
    db=o/(name+'.db');shutil.copy2(a.schema,db);now=int(time.time()*1000);last=now-(61000 if late else 1000);n=967 if name=='consensus' else 960
    with sqlite3.connect(db) as d:
        for i in range(n):
            issue='20261014'+f'{i+1:04}';at=last-(n-1-i)*60000
            d.execute('INSERT INTO draws(issue,open_time,fetch_time,first_seen_at,dirty,n1,n2,n3,n4,n5,n6,n7,n8,n9,n10) VALUES(?,?,?,?,0,?,?,?,?,?,?,?,?,?,?)',[issue,at,at+1,at+1]+[(i+j)%10+1 for j in range(10)])
        d.execute("INSERT INTO research_schedule_plan_r932(target_issue,input_hash,roster_hash,created_at) VALUES(?,?,?,?)",['20261014'+f'{n+1:04}','a'*64,'b'*64,now])
    return db
results=[]
def run(mode,db):
    command=[c['java'],'-Xmx192m','-cp',cp,'com.ethk3.novaonepro.ProbeLifecycleR80',mode,c['bridge'],str(db),str(o/(mode+'.json'))]
    execute(mode,command,120)
    report=json.loads((o/(mode+'.json')).read_text());results.append(report);print(mode,report,flush=True)
db=fixture('capture');run('capture',db);fifo=o/'fifo.db';shutil.copy2(db,fifo);tamper=o/'tamper.db';shutil.copy2(db,tamper);multi=o/'multihorizon.db';shutil.copy2(db,multi);run('resume',db);run('tamper',tamper);run('multihorizon',multi)
for mode in ['rollback','prefix','observation','authority','retention','archive','consensus','budget','pressure','late']:run(mode,fixture(mode,mode=='late'))
if a.native:
    native_adapters=o/'native-adapters';native_adapters.mkdir()
    native_sources=[r/'verification/r983/android/database/sqlite/SQLiteDatabase.java',r/'verification/r996/android/system/Os.java']
    execute('native-adapters-compile',[c['javac'],'-encoding','UTF-8','-sourcepath',str(o/'empty'),'-cp',cp,'-d',str(native_adapters),*map(str,native_sources)])
    assert not set(before)&{str(x.relative_to(native_adapters)) for x in native_adapters.rglob('*.class')},'NATIVE_PRODUCT_OVERLAY'
    native_cp=str(native_adapters)+':'+cp
    for resume in [False,True]:
        mode='archive_fifo_resume' if resume else 'archive_fifo'
        command=[c['java'],'-Xmx192m','-Djava.library.path='+str(a.native.resolve()),'-Dr982.readonly.bridge='+str(r/'verification/r996/read_only_bridge.py'),'-cp',native_cp,'com.ethk3.novaonepro.ProbeArchiveFifoR80',str(r/'verification/r983/typed_live_bridge.py'),str(fifo),str(o/'fifo-files'),str(o/(mode+'.json'))]+(['resume'] if resume else [])
        execute(mode,command)
        report=json.loads((o/(mode+'.json')).read_text());results.append(report);print(mode,report,flush=True)
after={str(x.relative_to(product)):sha(x) for x in product.rglob('*.class')};assert before==after,'PRODUCT_CHANGED'
summary={'status':'PASS','scope':'ACTUAL_GRADLE_PRODUCT_REAL_SQLITE_SYNTHETIC_NO_PHONE_NO_NATURAL_EFFICACY','product_overlay':False,'product_unchanged':True,'schema_sha256':sha(a.schema),'product_manifest_sha256':hashlib.sha256(json.dumps(before,sort_keys=True).encode()).hexdigest(),'source_sha256':{str(x.relative_to(r)):sha(x) for x in sources+(native_sources if a.native else [])+[src/'run.py',r/'verification/r996/read_only_bridge.py',r/'verification/r983/typed_live_bridge.py']},'native_library_sha256':sha(a.native/'libstarcore_report_cp4.so') if a.native else None,'checks':sum(x['checks'] for x in results),'executions':executions,'results':results,'all_project_requirements_closed':False}
(o/'product_hashes.json').write_text(json.dumps(before,sort_keys=True,indent=2)+'\n')
(o/'summary.json').write_text(json.dumps(summary,indent=2)+'\n')
