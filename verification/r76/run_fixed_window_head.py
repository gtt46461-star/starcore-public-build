#!/usr/bin/env python3
"""Measure the actual product head query, then exercise its public maintenance owner.
Only synthetic input fixtures; baseline SQL comes from pre-patch compiled bytecode.
"""
from pathlib import Path
import argparse,datetime,hashlib,json,shutil,sqlite3,subprocess
p=argparse.ArgumentParser()
for name in ('jdk','schema','out'):p.add_argument('--'+name,type=Path,required=True)
p.add_argument('--product-classes',type=Path,help='Final bytecode mode: test classes only, no product overlay')
a=p.parse_args();r=Path(__file__).resolve().parents[2];o=a.out.resolve();o.mkdir(parents=True);classes=o/'classes';classes.mkdir()
java=a.jdk.resolve()/'bin/java';parts=(a.schema/'host-classpath').read_text().strip().split(':')
if a.product_classes:parts[1]=str(a.product_classes.resolve())
cp=':'.join([str(classes),*parts]);test=r/'verification/r76/FixedWindowHeadR76.java';product=r/'app/src/main/java/com/ethk3/novaonepro/FixedResearchWindowR932.java'
def compile_files(label,files):
    with (o/(label+'.log')).open('w') as f:subprocess.run([str(java.with_name('javac')),'-cp',cp,'-d',str(classes),*map(str,files)],check=True,stdout=f,stderr=subprocess.STDOUT)
def export(label):subprocess.run([str(java),'-cp',cp,'com.ethk3.novaonepro.FixedWindowHeadR76','sql',str(o/(label+'.sql'))],check=True)
compile_files('compile-test',[test])
if not a.product_classes:export('baseline');compile_files('compile-product',[product])
export('current')
db=o/'fixture.db';shutil.copy2(a.schema/'schema-complete.db',db)
h=lambda x:hashlib.sha256(x.encode()).hexdigest()
with sqlite3.connect(db) as c:
    for issue in ['202609190001','202609190002','202609190003','202609190004','202609200001','202609250001']:
        study=h('r76-study:'+issue);key='algorithm:R76-SYNTHETIC-'+issue
        c.execute("INSERT INTO research_candidate_cal_study_r932 VALUES(?,?,1,0,'COLLECTING',?)",[study,key,issue])
        c.execute('INSERT INTO research_candidate_cal_observation_r932(study_hash,target_issue,candidate_key,structure_hash,bundle_hash,selection_hash,raw_score,position,n1,n2,n3,prediction_hash,frozen_at) VALUES(?,?,?,?,?,?,.3,1,1,2,3,?,1)',[study,issue,key,h('structure'),h('bundle'),h('selector'),h('prediction:'+issue)])
    def draw(issue,dirty=0):
        c.execute('INSERT INTO draws(issue,open_time,fetch_time,dirty,n1,n2,n3,n4,n5,n6,n7,n8,n9,n10,raw_json) VALUES(?,10,11,?,1,2,3,4,5,6,7,8,9,10,?)',[issue,dirty,'x'*8192])
    draw('202609190002',1);draw('202609190004')
    for i in range(4998):
        day=datetime.date(2026,9,20)+datetime.timedelta(days=i//1440);draw(day.strftime('%Y%m%d')+f'{i%1440+1:04d}')
    # Retained-only, dirty-hot shadow, and valid-hot shadow inputs. The hot valid
    # row has digit1 while its retained copy has digit10: precedence affects labels.
    for issue in ['202609190002','202609190003','202609190004']:
        digits=list(range(1,11)) if issue!='202609190004' else list(range(10,0,-1))
        c.execute('INSERT INTO draw_dependency_input_cp12(_id,issue,open_time,fetch_time,n1,n2,n3,n4,n5,n6,n7,n8,n9,n10,dirty,first_seen_at,draw_hash_version,draw_hash_v2,input_hash,retained_at) VALUES(1,?,10,11,?,?,?,?,?,?,?,?,?,?,0,1,2,?,?,1)',[issue,*digits,h('draw:'+issue),h('synthetic-retained:'+issue)])
    c.commit()
    profiles={}
    for name in ['baseline','current']:
        path=o/(name+'.sql')
        if not path.exists():continue
        sql=path.read_text();steps=[0]
        def tick():steps[0]+=1;return 0
        c.set_progress_handler(tick,1);row=c.execute(sql).fetchone();c.set_progress_handler(None,0)
        plan=[list(x) for x in c.execute('EXPLAIN QUERY PLAN '+sql)]
        profiles[name]={'row':row,'sqlite_vm_steps':steps[0],'plan':plan}
    (o/'query-profile.json').write_text(json.dumps(profiles,indent=2)+'\n')
    assert profiles['current']['row']==('202609190003',)
    assert not any('SCAN draws' in x[3] or 'SCAN e'==x[3] or 'AUTOMATIC' in x[3] for x in profiles['current']['plan']),profiles['current']['plan']
    if 'baseline' in profiles:
        assert profiles['baseline']['row']==profiles['current']['row']
        assert profiles['current']['sqlite_vm_steps']*20<profiles['baseline']['sqlite_vm_steps'],profiles
log=o/'owner.log'
with log.open('w') as f:result=subprocess.run([str(java),'-Xmx384m','-cp',cp,'com.ethk3.novaonepro.FixedWindowHeadR76',str(r/'verification/canonical/schema/sqlite_bridge.py'),str(db),str(o/'owner.json')],stdout=f,stderr=subprocess.STDOUT,timeout=180)
print(log.read_text(),end='')
if result.returncode:raise SystemExit(result.returncode)
summary={'status':'PASS','owner':json.loads((o/'owner.json').read_text()),'sqlite_vm_steps':{k:v['sqlite_vm_steps'] for k,v in profiles.items()},'sources':{str(x.relative_to(r)):h(x.read_text()) for x in [test,product,Path(__file__).resolve()]},'scope':'ACTUAL_SQL_PLAN_AND_VM_WORK_NOT_PHONE_WALL_TIME','final_product_classes':str(a.product_classes.resolve()) if a.product_classes else None}
rel=Path('com/ethk3/novaonepro/FixedResearchWindowR932.class');actual=classes/rel
if not actual.exists():actual=Path(parts[1])/rel
summary['product_class_sha256']={'FixedResearchWindowR932':hashlib.sha256(actual.read_bytes()).hexdigest()}
(o/'summary.json').write_text(json.dumps(summary,indent=2)+'\n');print(summary['sqlite_vm_steps'])
