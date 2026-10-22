#!/usr/bin/env python3
"""Actual JNI SQLite page snapshot + existing SCROW/generation-CAS pipeline.
Host transports only; not a device performance assertion. Use --compile-overlay for development.
"""
from pathlib import Path
import argparse,subprocess,sqlite3,shutil,datetime,json,hashlib,time
p=argparse.ArgumentParser()
for name in ('schema','classes','jdk','native','out'):p.add_argument('--'+name,type=Path,required=True)
p.add_argument('--compile-overlay',action='store_true')
p.add_argument('--filler-mib',type=int,default=256)
a=p.parse_args();r=Path(__file__).resolve().parents[2];o=a.out.resolve();o.mkdir()
j=a.jdk.resolve()/'bin';product=a.classes.resolve();host=a.schema.resolve();tests=o/'classes';tests.mkdir()
cp=':'.join([str(tests),str(host/'classes'),str(product),(host/'host-classpath').read_text().strip().split(':')[-1]])
def product_hashes():
 return {str(x.relative_to(product)):hashlib.sha256(x.read_bytes()).hexdigest() for name in ('ReportNativeCP4','RetentionSnapshotCacheR73','DataRetentionGovernorR900','StorageReclaimR943') for x in sorted((product/'com/ethk3/novaonepro').glob(name+'*.class'))}
product_before=product_hashes()
assert product_before
host_collisions=[str(x.relative_to(host/'classes')) for x in (host/'classes').rglob('*.class') if (product/x.relative_to(host/'classes')).is_file()]
assert not host_collisions,'HOST_PRODUCT_OVERLAY_FORBIDDEN:'+str(host_collisions)
sources=[r/'verification/r76'/x for x in ['RetentionPageClosureR76.java','NativeRetentionPageR76.java','RetentionPageEdgesR76.java']]+[r/'verification/r73/adapters/android/system/Os.java']
if a.compile_overlay:sources += [r/'app/src/main/java/com/ethk3/novaonepro'/x for x in ['ReportNativeCP4.java','RetentionSnapshotCacheR73.java']]
with (o/'compile.log').open('w') as f:subprocess.run([str(j/'javac'),'-cp',cp,'-d',str(tests)]+list(map(str,sources)),stdout=f,stderr=subprocess.STDOUT,check=True)
collisions=[str(x.relative_to(tests)) for x in tests.rglob('*.class') if (product/x.relative_to(tests)).is_file()]
if not a.compile_overlay:assert not collisions,'TEST_PRODUCT_OVERLAY_FORBIDDEN:'+str(collisions)
base=[str(j/'java'),'-Xmx96m','-Djava.library.path='+str(a.native.resolve()),'-cp',cp]
checks=[]
def ok(value,why):
 if not value:raise AssertionError(why)
 checks.append(why)
def native(src,dst,table,rows,maxbytes=64<<20,cancel=0,expected=0):
 start=time.monotonic();q=subprocess.run(base+['com.ethk3.novaonepro.NativeRetentionPageR76',str(src),str(dst),table,str(maxbytes),str(cancel)]+list(map(str,rows)),capture_output=True,text=True)
 ok(q.returncode==expected,f'native return {expected}: {dst.name}: {q.stdout.strip()} {q.stderr.strip()}')
 return {'elapsed_ms':round((time.monotonic()-start)*1000,3),'output':q.stdout.strip(),'bytes':dst.stat().st_size if dst.exists() else 0}
for enc in ['UTF-8','UTF-16le','UTF-16be']:
 src=o/('source#?'+enc+'.db');c=sqlite3.connect(src);c.execute("PRAGMA encoding='%s'"%enc)
 c.execute('CREATE TABLE specimen(k TEXT PRIMARY KEY,n INTEGER,r REAL,t TEXT,b BLOB,z,UNIQUE(n),CHECK(n>0))')
 c.execute('CREATE TABLE research_row_revision_v2(source_table TEXT,source_rowid INTEGER,generation INTEGER,PRIMARY KEY(source_table,source_rowid))')
 for row in [(-25,'键nul\0x',9223372036854775807,0.125,'汉文😀\0tail',bytes(range(256)),None),(99,'other',13,2.0,'第二',b'',b'123'),(101,'unselected',12,-1.25,'not copied',b'no',3)]:
  c.execute('INSERT INTO specimen(rowid,k,n,r,t,b,z) VALUES(?,?,?,?,?,?,?)',row)
 c.execute("INSERT INTO research_row_revision_v2 VALUES('specimen',-25,888)")
 c.execute("INSERT INTO research_row_revision_v2 VALUES('specimen',101,999)");c.commit();c.close()
 before=hashlib.sha256(src.read_bytes()).hexdigest();dst=o/('selected-'+enc+'.db');native(src,dst,'specimen',[-25,99])
 x=sqlite3.connect(src);y=sqlite3.connect(dst)
 ok(x.execute('SELECT rowid,* FROM specimen WHERE rowid IN(-25,99)').fetchall()==y.execute('SELECT rowid,* FROM specimen').fetchall(),enc+' exact cells and negative rowid')
 ok(x.execute("SELECT sql FROM sqlite_master WHERE name='specimen'").fetchone()==y.execute("SELECT sql FROM sqlite_master WHERE name='specimen'").fetchone(),enc+' exact original DDL')
 ok(x.execute('pragma encoding').fetchone()==y.execute('pragma encoding').fetchone(),enc+' encoding unchanged')
 ok(y.execute('SELECT * FROM research_row_revision_v2').fetchall()==[('specimen',-25,888)],enc+' only selected actual generations')
 ok(y.execute('pragma quick_check').fetchone()==('ok',),enc+' private page quick_check')
 x.close();y.close();ok(hashlib.sha256(src.read_bytes()).hexdigest()==before,enc+' source bytes unchanged')
 for suffix,rows,cancel,code in [('cancel',[-25,99],3,-1001),('missing',[999],0,12)]:
  failed=o/(enc+'-'+suffix+'.db');result=native(src,failed,'specimen',rows,cancel=cancel,expected=3)
  ok('code='+str(code) in result['output'] and not failed.exists(),enc+' failure removes only private snapshot '+suffix)
 # An existing destination must never be deleted/replaced on refusal.
 existing=o/(enc+'-existing.db');existing.write_bytes(b'keep prior bytes')
 native(src,existing,'specimen',[-25],expected=3);ok(existing.read_bytes()==b'keep prior bytes',enc+' existing destination preserved')
# Full installed schema + every actual publication/prune guard. Unrelated filler deliberately
# exceeds the admitted snapshot/output budget: selected rows must still work.
db=o/'live.db';shutil.copy2(host/'schema-complete.db',db);c=sqlite3.connect(db)
edges=o/'edges.db';shutil.copy2(host/'schema-complete.db',edges);ec=sqlite3.connect(edges);ec.execute('PRAGMA journal_mode=WAL')
start=datetime.datetime(2026,10,7,tzinfo=datetime.timezone.utc)
for i in range(5000):
 date=start+datetime.timedelta(days=i//1440);issue=date.strftime('%Y%m%d')+f'{i%1440+1:04}';at=int((start+datetime.timedelta(minutes=i+1)).timestamp()*1000)
 c.execute('INSERT INTO draws(issue,open_time,fetch_time,first_seen_at,dirty,n1,n2,n3,n4,n5,n6,n7,n8,n9,n10) VALUES(?,?,?,?,0,?,?,?,?,?,?,?,?,?,?)',[issue,at,at+1,at+1]+[(i+k)%10+1 for k in range(10)])
 ec.execute('INSERT INTO draws(issue,open_time,fetch_time,first_seen_at,dirty,n1,n2,n3,n4,n5,n6,n7,n8,n9,n10) VALUES(?,?,?,?,0,?,?,?,?,?,?,?,?,?,?)',[issue,at,at+1,at+1]+[(i+k)%10+1 for k in range(10)])
for i in range(400):c.execute("INSERT INTO diagnostic_log(log_key,created_at,level,module,code,message,detail) VALUES(?,1,'INFO','R76_TEST','HOST','eligible old log',?)",[f'R76-{i}',bytes(8<<20) if i==0 else 'test-detail-'+str(i)])
c.commit();c.close()
# Publish a closed, durable fixture before another native process is launched.
# A fixture-setup write transaction must not span the independent WAL edge suite.
ec.execute("INSERT INTO diagnostic_log(log_key,created_at,level,module,code,message,detail) VALUES('edge',1,'INFO','R76_TEST','HOST','eligible','original')");ec.commit();ec.close()
edge_command=base+['com.ethk3.novaonepro.RetentionPageEdgesR76',str(r/'verification/canonical/schema/sqlite_bridge.py'),str(edges),str(o/'edges-copy.db'),str(o/'edges-files'),str(o/'edges-result.json')]
with (o/'edges.log').open('w') as f:edge_code=subprocess.run(edge_command,stdout=f,stderr=subprocess.STDOUT).returncode
if edge_code:print((o/'edges.log').read_text()[-6000:]);raise SystemExit(edge_code)
c=sqlite3.connect(db)
c.execute('CREATE TABLE r76_unrelated_filler(id INTEGER PRIMARY KEY,payload BLOB)')
for i in range(a.filler_mib//16):c.execute('INSERT INTO r76_unrelated_filler(payload) VALUES(zeroblob(?))',(16<<20,))
c.commit();c.close();source_bytes=db.stat().st_size
def file_hash(path):
 h=hashlib.sha256()
 with path.open('rb') as f:
  while block:=f.read(1048576):h.update(block)
 return h.hexdigest()
initial_source_hash=file_hash(db)
def live_check(stage):
 z=sqlite3.connect('file:'+str(db)+'?mode=ro',uri=True);status=z.execute('pragma quick_check(1)').fetchone();z.close();print('LIVE_CHECK',stage,db.stat().st_size,status,flush=True);ok(status==('ok',),'source quick_check '+stage)
live_check('setup')
small=o/'large-source-small-page.db';large_result=native(db,small,'diagnostic_log',list(range(2,130)),maxbytes=1<<20)
live_check('small-native');y=sqlite3.connect(small);ok(y.execute('SELECT COUNT(*) FROM diagnostic_log').fetchone()[0]==128,'128 exact rows on large source');ok(y.execute("SELECT COUNT(*) FROM sqlite_master WHERE name='r76_unrelated_filler'").fetchone()[0]==0,'unrelated history never copied');y.close()
ok(small.stat().st_size<1<<20,'small page fits 1MiB even with large source')
full=o/'page-disk-full.db';q=native(db,full,'diagnostic_log',[1],maxbytes=65536,expected=3);ok('code=13' in q['output'] and not full.exists(),'native disk bound fails without accepted snapshot')
live_check('full-native-refusal')
ok(file_hash(db)==initial_source_hash,'selected page and rejected oversized page leave live source byte-identical')
command=base+['com.ethk3.novaonepro.RetentionPageClosureR76',str(r/'verification/canonical/schema/sqlite_bridge.py'),str(db),str(o/'files'),str(o/'pipeline-result.json')]
with (o/'test.log').open('w') as f:code=subprocess.run(command,stdout=f,stderr=subprocess.STDOUT).returncode
if code:print((o/'test.log').read_text()[-6000:]);raise SystemExit(code)
result={'status':'PASS','scope':'ACTUAL_PRODUCT_JNI_AND_SQLITE_HOST_NOT_PHONE','compile_overlay':a.compile_overlay,'checks':len(checks),'pipeline':json.loads((o/'pipeline-result.json').read_text()),'edges':json.loads((o/'edges-result.json').read_text()),'source_bytes':source_bytes,'selected_page':large_result,'checks_detail':checks,'native_sha256':hashlib.sha256((a.native/'libstarcore_report_cp4.so').read_bytes()).hexdigest(),'source_sha256':{str(x.relative_to(r)):hashlib.sha256(x.read_bytes()).hexdigest() for x in sources if '/app/' in str(x)}}
product_after=product_hashes();assert product_before==product_after,'PRODUCT_BYTECODE_CHANGED'
result['product_bytecode']={'unchanged':True,'before':product_before,'after':product_after,'host_collisions':host_collisions,'test_collisions':collisions}
(o/'run.json').write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n');print(json.dumps({k:v for k,v in result.items() if k not in ['checks_detail','source_sha256','product_bytecode']},ensure_ascii=False,indent=2))
