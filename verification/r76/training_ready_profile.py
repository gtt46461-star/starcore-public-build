"""Compare real exported predicates on report task rows and adversarial history growth.

SQLite VM work is deterministic work evidence, not a phone latency measurement.
"""
from pathlib import Path
import argparse, hashlib, json, shutil, sqlite3
p=argparse.ArgumentParser()
for key in ('database','baseline','current','out'):p.add_argument('--'+key,type=Path,required=True)
a=p.parse_args();a.out.mkdir(parents=True,exist_ok=True);target=a.out/'queue.db';shutil.copy2(a.database,target)
c=sqlite3.connect(target);c.execute('SELECT COUNT(*) FROM sqlite_master').fetchone()
old=a.baseline.read_text();new=a.current.read_text();now=1791878660000
old_orphan=(Path(__file__).parent/'evidence/training_r75_orphan.sql').read_text()
new_orphan=Path(str(a.current)+'-orphan').read_text()
def run(predicate,kind):
    sql='SELECT t.task_id,t.candidate_id FROM v57_research_task_r967 t WHERE '+predicate+' ORDER BY t.updated_at,t.target_issue,t.task_id'
    args=[kind,now,'test-process'];steps=[0]
    def tick():steps[0]+=1;return 0
    c.set_progress_handler(tick,1);rows=c.execute(sql,args).fetchall();c.set_progress_handler(None,0)
    return rows,steps[0],[list(r) for r in c.execute('EXPLAIN QUERY PLAN '+sql,args)]
def orphan(sql):
    steps=[0]
    def tick():steps[0]+=1;return 0
    c.set_progress_handler(tick,1);rows=c.execute(sql,['test-process']).fetchall();c.set_progress_handler(None,0)
    return rows,steps[0],[list(r) for r in c.execute('EXPLAIN QUERY PLAN '+sql,['test-process'])]
results={}
for phase in ('report','expanded'):
    if phase=='expanded':
        # Terminal history cannot increase active work or change runnable order.
        rows=[]
        for i in range(50000):
            rows.append(('r76-history-'+str(i),'MAINTENANCE','lane:'+str(i%22),'202601010001',str(i),'h','h','COMPLETED',i,i))
        c.executemany('INSERT INTO v57_research_task_r967(task_id,kind,candidate_id,target_issue,stream_seq,input_hash,authority_ref,status,created_at,updated_at) VALUES(?,?,?,?,?,?,?,?,?,?)',rows);c.commit()
    results[phase]={}
    for kind in ('MAINTENANCE','TRAIN_CANDIDATE','POST_TRAIN','FREEZE_CANDIDATE'):
        before,steps0,plan0=run(old,kind);after,steps1,plan1=run(new,kind)
        assert before==after,(phase,kind,'ORDER_OR_DOMAIN_CHANGED')
        if kind=='MAINTENANCE':assert steps1*20<steps0,(phase,steps0,steps1)
        results[phase][kind]={'ready':len(after),'baseline_steps':steps0,'current_steps':steps1,'baseline_plan':plan0,'current_plan':plan1}
    before,steps0,plan0=orphan(old_orphan);after,steps1,plan1=orphan(new_orphan)
    assert before==after and steps1*20<steps0
    results[phase]['LOCAL_ORPHAN']={'rows':len(after),'baseline_steps':steps0,'current_steps':steps1,'baseline_plan':plan0,'current_plan':plan1}
# Unknown/TRAIN kind retains a terminal predecessor block. POST and MAINTENANCE
# may review later independent tasks; unresolved earlier work blocks every kind.
def task(id,kind,candidate,issue,status,owner='',available=0):
    c.execute('INSERT INTO v57_research_task_r967(task_id,kind,candidate_id,target_issue,stream_seq,input_hash,authority_ref,status,lease_owner,available_at,created_at,updated_at) VALUES(?,?,?,?,?,?,?,?,?,?,1,1)',[id,kind,candidate,issue,id,'h','h',status,owner,available])
for kind in ('TRAIN_CANDIDATE','POST_TRAIN'):
    for predecessor in ('FAILED_TERMINAL','EXPIRED','YIELDED'):
        stem=kind+'-'+predecessor;candidate='blocked-'+stem
        task(stem+'-1',kind,candidate,'202609010001',predecessor,available=now+1000)
        task(stem+'-2',kind,candidate,'202609010002','PENDING')
task('foreign','TRAIN_CANDIDATE','ready-foreign','202609010003','RUNNING','foreign-process')
task('local','TRAIN_CANDIDATE','ready-local','202609010003','RUNNING','test-process')
c.commit();checks=0
for kind in ('TRAIN_CANDIDATE','POST_TRAIN'):
    before,_,_=run(old,kind);after,_,_=run(new,kind);assert before==after;checks+=1
    ids={r[0] for r in after}
    for predecessor in ('FAILED_TERMINAL','EXPIRED','YIELDED'):
        eligible=kind=='POST_TRAIN' and predecessor!='YIELDED'
        assert (kind+'-'+predecessor+'-2' in ids)==eligible;checks+=1
    if kind=='TRAIN_CANDIDATE':assert 'foreign' in ids and 'local' not in ids;checks+=1
summary={'status':'PASS','scope':'CONTROLLED_REPORT_ROWS_SQL_VM_WORK_NOT_PHONE_LATENCY','semantic_checks':checks,'profiles':results,'predicates_sha256':{'baseline':hashlib.sha256(old.encode()).hexdigest(),'current':hashlib.sha256(new.encode()).hexdigest()}}
(a.out/'summary.json').write_text(json.dumps(summary,indent=2)+'\n');print(json.dumps({k:v['MAINTENANCE'] for k,v in results.items()},indent=2));c.close()
