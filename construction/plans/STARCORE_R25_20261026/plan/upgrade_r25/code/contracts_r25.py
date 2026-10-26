"""Executable HOST contracts, not an Android engine and never a REAL authorizer.

The SQL connection represents the existing single Owner in isolation tests.
Production must use DatabaseCoordinator and durable files before publication.
No model-quality claim is made by these contracts.
"""
from __future__ import annotations
import hashlib,json,math,sqlite3
from contextlib import contextmanager
from pathlib import Path

REQUIRED_RULES={'feature','architecture','optimizer','update_schedule','initial_state',
                'rng','gate','router','calibration','missing_policy','resource_schedule','evidence_protocol'}
def canonical(value):
    return json.dumps(value,ensure_ascii=False,sort_keys=True,separators=(',',':'),allow_nan=False)
def digest(value):return hashlib.sha256(canonical(value).encode()).hexdigest()

def policy_hash(rules):
    if set(rules)!=REQUIRED_RULES or any(not isinstance(v,str) or not v for v in rules.values()):
        raise ValueError('POLICY_RULES_INCOMPLETE')
    return digest({'version':'R25_POLICY_V1','rules':rules})

def k3_mass(row,numbers):
    if len(row)!=10 or any(not isinstance(x,(float,int)) or not math.isfinite(x) or x<0 for x in row):
        raise ValueError('PROBABILITY_INVALID')
    if abs(math.fsum(row)-1)>1e-6:raise ValueError('PROBABILITY_SUM')
    if len(numbers)!=3 or len(set(numbers))!=3 or any(type(n) is not int or not 1<=n<=10 for n in numbers):
        raise ValueError('FIXED_K3')
    return math.fsum(row[n-1] for n in numbers)

def validate_availability(target,input_through,freeze_wall,deadline_wall,freeze_mono,deadline_mono,first_seen):
    if input_through>=target:raise ValueError('FUTURE_LABEL')
    if freeze_wall>=deadline_wall or freeze_mono>=deadline_mono:raise ValueError('LATE_FREEZE')
    if any(t>freeze_wall for t in first_seen):raise ValueError('NOT_AVAILABLE_AT_FREEZE')

def evidence_row(origin,has_predraw_receipt):
    return origin=='REAL' and has_predraw_receipt is True

def check_envelope(envelope,required_ids):
    if len({x['sectionId'] for x in envelope})!=len(envelope):raise ValueError('DUPLICATE_SECTION')
    if set(required_ids)-{x['sectionId'] for x in envelope}:raise ValueError('MISSING_SECTION')
    statuses={'PASS','FAIL','PARTIAL','NOT_RUN','INSUFFICIENT','NOT_APPLICABLE','ERROR'}
    for x in envelope:
        if x['status'] not in statuses:raise ValueError('STATUS')
        ns=[x[k] for k in ('expectedRows','exportedRows','omittedRows')]
        if any(type(n) is not int or n<0 for n in ns) or ns[0]!=ns[1]+ns[2]:raise ValueError('ROW_ACCOUNTING')
        if x['status']=='ERROR' and not x.get('errorChain'):raise ValueError('ERROR_WITHOUT_REASON')
        if x['status']=='PASS' and (ns[2] or x.get('truncated')):raise ValueError('TRUNCATED_PASS')
    return {'structurally_complete':True,'all_passed':all(x['status']=='PASS' for x in envelope)}

class ContractStore:
    def __init__(self,path=':memory:'):
        self.db=sqlite3.connect(path,isolation_level=None)
        self.db.executescript((Path(__file__).parents[1]/'schema/r25_reference.sql').read_text())
    @contextmanager
    def tx(self):
        self.db.execute('BEGIN IMMEDIATE')
        try:yield;self.db.execute('COMMIT')
        except BaseException:self.db.execute('ROLLBACK');raise
    def register(self,rules,birth,mode='ADAPTIVE'):
        h=policy_hash(rules);body=canonical(rules)
        with self.tx():
            old=self.db.execute('SELECT spec_json,birth_ordinal,mode FROM r25_policy WHERE policy_hash=?',(h,)).fetchone()
            if old:
                if old!=(body,birth,mode):raise ValueError('POLICY_IDENTITY_CONFLICT')
                return h
            self.db.execute('INSERT INTO r25_policy VALUES(?,?,?,?)',(h,body,birth,mode))
            initial=digest({'policy':h,'initial':rules['initial_state']})
            self.db.execute('INSERT INTO r25_checkpoint VALUES(?,?,NULL,0,?,?,?,?)',(initial,h,birth-1,rules['initial_state'],rules['optimizer'],rules['rng']))
            self.db.execute('INSERT INTO r25_active VALUES(?,?,0)',(h,initial))
        return h
    def active(self,p):return self.db.execute('SELECT checkpoint_hash,version FROM r25_active WHERE policy_hash=?',(p,)).fetchone()
    def update(self,p,event,source_hash,expected_parent,train_through,next_target,known_through,artifact,optimizer,rng,fail_after_insert=False):
        if not all(isinstance(v,str) and v for v in [p,event,source_hash,expected_parent,artifact,optimizer,rng]):raise ValueError('IDENTITY')
        if train_through>=next_target or train_through>known_through:raise ValueError('FUTURE_LABEL')
        with self.tx():
            old=self.db.execute('SELECT source_hash,checkpoint_hash FROM r25_consumption WHERE event_id=? AND policy_hash=?',(event,p)).fetchone()
            if old:
                if old[0]!=source_hash:raise ValueError('CONFLICTING_EVENT')
                return old[1],False
            pol=self.db.execute('SELECT mode FROM r25_policy WHERE policy_hash=?',(p,)).fetchone()
            if pol is None or pol[0]!='ADAPTIVE':raise ValueError('UPDATE_NOT_ALLOWED')
            a=self.active(p)
            if a is None or a[0]!=expected_parent:raise ValueError('STALE_CAS')
            previous=self.db.execute('SELECT trained_through FROM r25_checkpoint WHERE checkpoint_hash=?',(expected_parent,)).fetchone()[0]
            if train_through<=previous:raise ValueError('CURSOR_NOT_ADVANCED')
            h=digest([p,expected_parent,a[1]+1,train_through,artifact,optimizer,rng,event,source_hash])
            self.db.execute('INSERT INTO r25_checkpoint VALUES(?,?,?,?,?,?,?,?)',(h,p,expected_parent,a[1]+1,train_through,artifact,optimizer,rng))
            if fail_after_insert:raise RuntimeError('INJECTED_CRASH_BEFORE_COMMIT')
            self.db.execute('INSERT INTO r25_consumption VALUES(?,?,?,?)',(event,p,source_hash,h))
            cur=self.db.execute('UPDATE r25_active SET checkpoint_hash=?,version=version+1 WHERE policy_hash=? AND checkpoint_hash=? AND version=?',(h,p,expected_parent,a[1]))
            if cur.rowcount!=1:raise ValueError('STALE_CAS')
        return h,True
    def task(self,key,snapshot):
        with self.tx():
            old=self.db.execute('SELECT snapshot_hash FROM r25_task WHERE task_id=?',(key,)).fetchone()
            if old:
                if old[0]!=snapshot:raise ValueError('CAMPAIGN_INPUT_DRIFT')
                return
            self.db.execute('INSERT INTO r25_task(task_id,snapshot_hash) VALUES(?,?)',(key,snapshot))
    def claim(self,key,owner,now,ttl):
        if ttl<=0 or not owner:raise ValueError('LEASE_ARGUMENT')
        with self.tx():
            r=self.db.execute('SELECT fence,lease_until,status FROM r25_task WHERE task_id=?',(key,)).fetchone()
            if r is None or r[1]>now or r[2] in ('DONE','CANCELLED','FAILED'):raise ValueError('TASK_NOT_CLAIMABLE')
            self.db.execute("UPDATE r25_task SET fence=?,owner=?,lease_until=?,status='RUNNING' WHERE task_id=?",(r[0]+1,owner,now+ttl,key))
            return r[0]+1
    def progress(self,key,owner,fence,now,cursor):
        with self.tx():
            c=self.db.execute("UPDATE r25_task SET cursor=?,progress_at=?,status='YIELDED',lease_until=? WHERE task_id=? AND owner=? AND fence=? AND lease_until>? AND cursor<? AND status='RUNNING'",(cursor,now,now,key,owner,fence,now,cursor))
            if c.rowcount!=1:raise ValueError('STALE_FENCE_EXPIRED_OR_NO_PROGRESS')
    def cohort(self,key,p,start,end,alpha=.05,mode='FIXED_ENDPOINT'):
        with self.tx():
            birth=self.db.execute('SELECT birth_ordinal FROM r25_policy WHERE policy_hash=?',(p,)).fetchone()
            if birth is None or start<birth[0]:raise ValueError('PRE_BIRTH_EVIDENCE')
            protocol=digest([key,p,start,end,alpha,mode])
            self.db.execute('INSERT INTO r25_cohort(cohort_id,policy_hash,start_ordinal,end_ordinal,mode,alpha,protocol_hash) VALUES(?,?,?,?,?,?,?)',(key,p,start,end,mode,alpha,protocol))
    def reserve_look(self,cohort,evidence_digest,as_of,prechecks_ok=True):
        if not prechecks_ok:return None
        if not evidence_digest:raise ValueError('DIGEST_REQUIRED')
        with self.tx():
            c=self.db.execute('SELECT end_ordinal,mode,alpha FROM r25_cohort WHERE cohort_id=?',(cohort,)).fetchone()
            if c is None:raise ValueError('COHORT_MISSING')
            old=self.db.execute('SELECT look_index,alpha FROM r25_look WHERE cohort_id=? AND evidence_digest=?',(cohort,evidence_digest)).fetchone()
            if old:return old
            n=self.db.execute('SELECT count(*) FROM r25_look WHERE cohort_id=?',(cohort,)).fetchone()[0]
            if c[1]=='FIXED_ENDPOINT' and (as_of<c[0] or n):raise ValueError('FIXED_ENDPOINT_OR_ALREADY_OPENED')
            index=n+1
            # Summable spending is an allocation only, not a statistical test implementation.
            alpha=c[2] if c[1]=='FIXED_ENDPOINT' else c[2]/(index*(index+1))
            self.db.execute('INSERT INTO r25_look(cohort_id,evidence_digest,look_index,alpha) VALUES(?,?,?,?)',(cohort,evidence_digest,index,alpha))
            return index,alpha
    def close(self):self.db.close()
