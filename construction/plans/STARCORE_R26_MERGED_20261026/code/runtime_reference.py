"""R26 executable HOST contracts, not Android integration or a quality certificate.
SQLite stores state inline to exercise atomic commits. Production artifact fsync,
Android scheduling, real model training and existing Owner adapters remain required.
"""
from __future__ import annotations
from dataclasses import dataclass
from pathlib import Path
from contextlib import contextmanager
from collections.abc import Mapping
import hashlib, json, math, sqlite3, struct

RULES={'graph','features','updater','update_schedule','initial_state','rng','gate',
       'router','calibration','missing_policy','resource_schedule','evidence_protocol','runtime_abi'}
STATE={'weights','optimizer','rng','normalizer','calibrator','teacher','recurrent','feature_dictionary'}

def typed(value):
    if value is None:return ['null']
    if type(value) is bool:return ['bool',value]
    if type(value) is int:return ['int',str(value)]
    if type(value) is float:
        if not math.isfinite(value):raise ValueError('NON_FINITE')
        return ['f64',struct.pack('>d',value).hex()]
    if type(value) is str:
        value.encode('utf-8',errors='strict')
        return ['str',value]
    if isinstance(value,Mapping):
        if any(type(k) is not str for k in value):raise ValueError('NON_STRING_KEY')
        keys=sorted(value,key=lambda k:k.encode('utf-8',errors='strict'))
        return ['map',[[k,typed(value[k])] for k in keys]]
    if isinstance(value,(list,tuple)):return ['seq',[typed(v) for v in value]]
    raise TypeError(type(value).__name__)

def canonical(value):
    return json.dumps(typed(value),ensure_ascii=False,separators=(',',':'),allow_nan=False)

def digest(value):return hashlib.sha256(canonical(value).encode('utf-8')).hexdigest()

@dataclass(frozen=True)
class Policy:
    body:str
    hash:str
    initial_state_hash:str
    @classmethod
    def create(cls,spec):
        if set(spec)!=RULES:raise ValueError('INCOMPLETE_POLICY')
        body=canonical({'domain':'STARCORE_POLICY_R26_V1','spec':spec})
        return cls(body,hashlib.sha256(body.encode('utf-8')).hexdigest(),digest(spec['initial_state']))

class RuntimeStore:
    def __init__(self,path=':memory:'):
        self.db=sqlite3.connect(path,isolation_level=None)
        self.db.executescript((Path(__file__).parents[1]/'schema/r26_runtime.sql').read_text())
    @contextmanager
    def tx(self):
        self.db.execute('BEGIN IMMEDIATE')
        try:yield;self.db.execute('COMMIT')
        except BaseException:self.db.execute('ROLLBACK');raise
    def close(self):self.db.close()
    def feedback(self,event,ordinal,source_hash,payload):
        if not event or not source_hash or type(ordinal) is not int or ordinal<0:raise ValueError('EVENT')
        body=canonical(payload)
        with self.tx():
            prior=self.db.execute('SELECT issue_ordinal,source_hash,payload FROM r26_feedback WHERE event_id=?',(event,)).fetchone()
            if prior:
                if prior!=(ordinal,source_hash,body):raise ValueError('EVENT_CONFLICT')
                return False
            self.db.execute('INSERT INTO r26_feedback VALUES(?,?,?,?,?)',(event,ordinal,source_hash,body,ordinal))
            return True
    def register(self,policy,birth,state,mode='ADAPTIVE'):
        if set(state)!=STATE or type(birth) is not int or birth<1:raise ValueError('INITIAL_STATE')
        if digest(state)!=policy.initial_state_hash:raise ValueError('INITIAL_STATE_NOT_BOUND')
        body=json.dumps(state,sort_keys=True,allow_nan=False,separators=(',',':'))
        h=digest({'domain':'STARCORE_CHECKPOINT_R26_V1','policy':policy.hash,'parent':None,'through':birth-1,'state':state})
        with self.tx():
            row=self.db.execute('SELECT canonical_spec,birth_ordinal,mode FROM r26_policy WHERE policy_hash=?',(policy.hash,)).fetchone()
            if row:
                if row!=(policy.body,birth,mode):raise ValueError('POLICY_CONFLICT')
                return self.active(policy.hash)[0]
            self.db.execute('INSERT INTO r26_policy VALUES(?,?,?,?)',(policy.hash,policy.body,birth,mode))
            self.db.execute('INSERT INTO r26_checkpoint VALUES(?,?,?,?,?)',(h,policy.hash,None,birth-1,body))
            self.db.execute('INSERT INTO r26_active VALUES(?,?,0)',(policy.hash,h))
        return h
    def active(self,policy):return self.db.execute('SELECT checkpoint_hash,version FROM r26_active WHERE policy_hash=?',(policy,)).fetchone()
    def task(self,key,lane,mechanism,snapshot):
        if not key or not snapshot:raise ValueError('TASK_IDENTITY')
        with self.tx():
            r=self.db.execute('SELECT lane,mechanism,snapshot_hash FROM r26_task WHERE task_id=?',(key,)).fetchone()
            if r:
                if r!=(lane,mechanism,snapshot):raise ValueError('SNAPSHOT_OR_TASK_DRIFT')
                return
            self.db.execute('INSERT INTO r26_task(task_id,lane,mechanism,snapshot_hash) VALUES(?,?,?,?)',(key,lane,mechanism,snapshot))
    def claim(self,key,owner,now,ttl):
        if ttl<=0 or not owner:raise ValueError('LEASE')
        with self.tx():
            r=self.db.execute('SELECT fence,lease_until,status FROM r26_task WHERE task_id=?',(key,)).fetchone()
            if not r or r[1]>now or r[2] in ('DONE','FAILED','CANCELLED'):raise ValueError('NOT_CLAIMABLE')
            self.db.execute("UPDATE r26_task SET fence=?,owner=?,lease_until=?,status='RUNNING',blocked_reason=NULL WHERE task_id=?",(r[0]+1,owner,now+ttl,key))
            return r[0]+1
    def commit_update(self,policy,event,expected_parent,state,target,known_through,
                      task,owner,fence,now,new_cursor,fail_before_commit=False):
        if set(state)!=STATE:raise ValueError('INCOMPLETE_CHECKPOINT')
        state_body=json.dumps(state,sort_keys=True,allow_nan=False,separators=(',',':'))
        with self.tx():
            # Replayed completion returns the already committed result, without writes.
            done=self.db.execute('SELECT checkpoint_hash FROM r26_consumption WHERE event_id=? AND policy_hash=?',(event,policy)).fetchone()
            if done:return done[0],False
            t=self.db.execute('SELECT owner,fence,lease_until,status,cursor FROM r26_task WHERE task_id=?',(task,)).fetchone()
            if not t or t[:2]!=(owner,fence) or t[2]<=now or t[3]!='RUNNING':raise ValueError('STALE_FENCE_OR_LEASE')
            if type(new_cursor) is not int or new_cursor<=t[4]:raise ValueError('NO_PROGRESS')
            ev=self.db.execute('SELECT issue_ordinal,source_hash FROM r26_feedback WHERE event_id=?',(event,)).fetchone()
            if not ev or ev[0]>=target or ev[0]>known_through:raise ValueError('FUTURE_OR_MISSING_LABEL')
            pol=self.db.execute('SELECT mode FROM r26_policy WHERE policy_hash=?',(policy,)).fetchone()
            if not pol or pol[0]!='ADAPTIVE':raise ValueError('UPDATE_NOT_ALLOWED')
            active=self.active(policy)
            if not active or active[0]!=expected_parent:raise ValueError('STALE_CAS')
            through=self.db.execute('SELECT trained_through FROM r26_checkpoint WHERE checkpoint_hash=?',(expected_parent,)).fetchone()[0]
            if ev[0]<=through:raise ValueError('CURSOR_REGRESSION')
            # Reject silent skipping of an earlier READY event for this policy.
            pending=self.db.execute('SELECT f.issue_ordinal FROM r26_feedback f LEFT JOIN r26_consumption c ON c.event_id=f.event_id AND c.policy_hash=? WHERE f.issue_ordinal>? AND c.event_id IS NULL ORDER BY f.issue_ordinal LIMIT 1',(policy,through)).fetchone()
            if pending is None or pending[0]!=ev[0]:raise ValueError('OUT_OF_ORDER_EVENT')
            h=digest({'domain':'STARCORE_CHECKPOINT_R26_V1','policy':policy,'parent':expected_parent,'through':ev[0],'state':state,'event':event,'source':ev[1]})
            self.db.execute('INSERT INTO r26_checkpoint VALUES(?,?,?,?,?)',(h,policy,expected_parent,ev[0],state_body))
            self.db.execute('INSERT INTO r26_consumption VALUES(?,?,?)',(event,policy,h))
            changed=self.db.execute('UPDATE r26_active SET checkpoint_hash=?,version=version+1 WHERE policy_hash=? AND checkpoint_hash=? AND version=?',(h,policy,expected_parent,active[1])).rowcount
            if changed!=1:raise ValueError('STALE_CAS')
            self.db.execute("UPDATE r26_task SET cursor=?,status='YIELDED',lease_until=? WHERE task_id=?",(new_cursor,now,task))
            self.db.execute('INSERT INTO r26_progress(task_id,fence,cursor_before,cursor_after,cpu_ms) VALUES(?,?,?,?,0)',(task,fence,t[4],new_cursor))
            if fail_before_commit:raise RuntimeError('INJECTED_BEFORE_COMMIT')
        return h,True
