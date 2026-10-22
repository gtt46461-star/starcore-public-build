# Explicit synthetic stored Owner facts from supplied UI fixture, not issuing evidence.
from pathlib import Path
import sqlite3
import sys
r=Path(sys.argv[1])
c=sqlite3.connect(r/'fixture.db');c.execute('PRAGMA foreign_keys=ON');h='a'*64;b='b'*64;p='c'*64;issue='202610030001'
def insert(t,values):
 names=list(values);c.execute('INSERT INTO "'+t+'"('+','.join('"'+x+'"'for x in names)+') VALUES('+','.join('?'for x in names)+')',[values[x]for x in names])
insert('r952_candidate',dict(candidate_id=h,intent_id=b,ast='{}',structural_hash=p,parent_id='',state='NURSERY',future_n=0,created_issue=issue,may_create_real=0,created_at=1))
insert('research_lifecycle_r932',dict(candidate_key='r952:'+h,structural_hash=p,state='OBSERVING',reason='WAIT_FIXED_WINDOW_AND_SHARED_CERTIFICATE',first_prune_reason='',future_n=1000,paired_n=1000,evidence_hash=b,last_issue=issue,reviewed_at=1))
insert('v57_research_task_r967',dict(task_id=h,kind='TRAIN_CANDIDATE',candidate_id=h,target_issue=issue,stream_seq=b,input_hash=p,authority_ref=b,status='WAIT_INPUT',cursor=17,generation=2,lease_owner='',lease_until=0,available_at=10,attempt_count=1,last_error='TRAINING_WORKING_STORAGE_CAPACITY',result_hash='',created_at=1,updated_at=1))
insert('research_candidate_cal_study_r932',dict(study_hash=h,candidate_key='r952:'+h,captured_n=241,settled_n=241,state='READY',first_issue=issue))
# A raw legacy ACTIVE certificate and positive contribution cannot fabricate current Certificate/REAL success.
fields={x[1]:''for x in c.execute('pragma table_info(research_certificate_r929)')}
fields.update(candidate_key=h,consumer='INVENTED_ALGORITHM',namespace='TEST_FIXTURE',family_id='FIXTURE',candidate_id=h,structural_hash=p,evidence_hash=b,level='L4_CHALLENGER',state='ACTIVE',bundle_hash=b,schema_hash=b,atom_graph_hash=b,model_hash=b,policy_hash=b,calibration_hash=b,domain_hash=b,trial_hash=b,selection_hash=b,code_hash=b,valid_from_issue=issue,valid_until_issue=issue,maximum_weight=0,all_gates_pass=0,gate_hash=b,reason='LEGACY_FIXTURE',certificate_hash=h,issued_at=1,updated_at=1,contract_revision='R932_REQUIRED_GATES_V1');insert('research_certificate_r929',fields)
insert('research_mainline_contribution_r929',dict(target_issue=issue,consumer='INVENTED_ALGORITHM',candidate_id=h,certificate_hash=h,bundle_hash=b,requested_weight=.25,authorized_weight=.25,before_rank1_hash=b,after_rank1_hash=p,input_hash=h,status='FIXTURE_RECORDED',reason='',record_hash=p,created_at=1))
c.commit();assert not c.execute('PRAGMA foreign_key_check').fetchall();c.close();print('actual405schema synthetic Owner-fact projection fixture, not genuine issuing/training claim')
