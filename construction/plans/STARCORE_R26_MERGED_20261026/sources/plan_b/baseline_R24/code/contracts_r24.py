"""Executable HOST specification of invariants; NOT an Android replacement engine.
Transaction owner in Android must remain DatabaseCoordinator/SettlementEngine.
"""
from __future__ import annotations
from dataclasses import dataclass
import hashlib,json,math,sqlite3
from typing import Sequence
from audit_inputs_r24 import ordinal

@dataclass(frozen=True)
class AtomSpec:
    key:str
    input_type:str
    output_type:str
    max_lookback:int
    feature_version:str
    availability_lag:int=1
    def validate(self):
        if not self.key or not self.feature_version:raise ValueError('ATOM_IDENTITY')
        if not 1<=self.max_lookback<=5000 or self.availability_lag<1:raise ValueError('ATOM_FUTURE_OR_WINDOW')
        if self.input_type not in {'HISTORY','STATE','RELATION'}:raise ValueError('ATOM_TYPE')
        if self.output_type not in {'SCALAR','VECTOR','MARGINAL','STATE','RELATION'}:raise ValueError('ATOM_TYPE')

@dataclass(frozen=True)
class Receipt:
    spec_hash:str
    train_through:int
    frozen_target:int
    model_hash:str
    evidence_scope:str
    causal:bool
    n:int
    hits:int
    opportunities:int
    max_loss:int
    fresh_corrected_lower:float|None
    def validate(self):
        if not self.spec_hash or not self.model_hash or not self.causal or self.train_through>=self.frozen_target:raise ValueError('CAUSAL_BINDING')
        if not 0<=self.hits<=self.n<=self.opportunities or self.max_loss<0:raise ValueError('COUNTS')
        if self.fresh_corrected_lower is not None and (not math.isfinite(self.fresh_corrected_lower) or not 0<=self.fresh_corrected_lower<=1):raise ValueError('INVALID_LOWER_BOUND')
    def qualification(self)->str:
        self.validate()
        if self.evidence_scope!='PROSPECTIVE_FROZEN':return 'RESEARCH_ONLY'
        if self.n<250 or self.fresh_corrected_lower is None:return 'INSUFFICIENT_EVIDENCE'
        if self.opportunities==0 or self.n/self.opportunities<.15:return 'COVERAGE_NOT_MET'
        if self.hits/self.n<.35 or self.fresh_corrected_lower<=.30:return 'QUALITY_NOT_MET'
        if self.max_loss>6:return 'STABILITY_NOT_MET'
        return 'ELIGIBLE_FOR_AUTHORITY_REVIEW_NOT_AUTO_REAL'

def history_window(rows:Sequence[dict],target_issue:str,width:int)->list[dict]:
    if width<1 or width>5000:raise ValueError('WINDOW')
    target=ordinal(target_issue);prior=sorted((r for r in rows if ordinal(r['issue'])<target),key=lambda r:ordinal(r['issue']))
    if len(prior)<width:raise ValueError('HISTORY_SHORT')
    x=prior[-width:]
    if ordinal(x[-1]['issue'])!=target-1:raise ValueError('HISTORY_GAP')
    if any(ordinal(b['issue'])!=ordinal(a['issue'])+1 for a,b in zip(x,x[1:])):raise ValueError('HISTORY_GAP')
    if any(sorted(r['numbers'])!=list(range(1,11)) for r in x):raise ValueError('INVALID_PERMUTATION')
    return x

def best_k3(p:Sequence[Sequence[float]])->tuple[int,tuple[int,...],float]:
    if len(p)!=10 or any(len(r)!=10 for r in p):raise ValueError('PROB_SHAPE')
    if any(any(not math.isfinite(v) or v<0 for v in r) or abs(sum(r)-1)>1e-6 for r in p):raise ValueError('PROB_VALUE')
    choices=[]
    for i,r in enumerate(p):
        ns=tuple(sorted(sorted(range(10),key=lambda j:(-r[j],j))[:3]));choices.append((sum(r[j] for j in ns),i,ns))
    mass,pos,ns=min(choices,key=lambda x:(-x[0],x[1],x[2]));return pos+1,tuple(n+1 for n in ns),mass

class EvidenceLedger:
    def __init__(self,path:str=':memory:'):
        self.db=sqlite3.connect(path);self.db.execute('PRAGMA foreign_keys=ON')
        self.db.executescript('''
        CREATE TABLE IF NOT EXISTS decisions(issue TEXT PRIMARY KEY,position INTEGER,mask INTEGER,freeze_ms INTEGER,open_ms INTEGER,model_hash TEXT,real INTEGER,hit INTEGER);
        CREATE TABLE IF NOT EXISTS feedback(issue TEXT PRIMARY KEY REFERENCES decisions(issue),payload_hash TEXT NOT NULL,consumed INTEGER NOT NULL DEFAULT 0);
        CREATE TABLE IF NOT EXISTS checkpoints(owner TEXT PRIMARY KEY,version INTEGER NOT NULL,through_issue TEXT,model_hash TEXT);
        ''')
    def freeze(self,issue:str,position:int,numbers:Sequence[int],freeze_ms:int,open_ms:int,model_hash:str,real:bool=False):
        ordinal(issue)
        if not 1<=position<=10 or len(numbers)!=3 or len(set(numbers))!=3 or any(n<1 or n>10 for n in numbers):raise ValueError('EXACT_ONE_POSITION_K3')
        if freeze_ms>=open_ms or not model_hash:raise ValueError('INVALID_FREEZE')
        mask=sum(1<<(n-1) for n in numbers)
        with self.db:self.db.execute('INSERT INTO decisions VALUES(?,?,?,?,?,?,?,NULL)',(issue,position,mask,freeze_ms,open_ms,model_hash,int(real)))
    def settle(self,issue:str,numbers:Sequence[int],now_ms:int)->bool:
        if sorted(numbers)!=list(range(1,11)):raise ValueError('INVALID_DRAW')
        with self.db:
            row=self.db.execute('SELECT position,mask,open_ms,hit FROM decisions WHERE issue=?',(issue,)).fetchone()
            if row is None:raise ValueError('NO_FROZEN_OBSERVATION')
            pos,mask,ot,hit=row
            if now_ms<ot:raise ValueError('RESULT_BEFORE_OPEN')
            expected=int(bool(mask&(1<<(numbers[pos-1]-1))))
            digest=hashlib.sha256(json.dumps([issue,list(numbers)],separators=(',',':')).encode()).hexdigest()
            if hit is not None:
                prior=self.db.execute('SELECT payload_hash FROM feedback WHERE issue=?',(issue,)).fetchone()
                if hit!=expected or prior is None or prior[0]!=digest:raise ValueError('CONFLICTING_DRAW')
                return False
            self.db.execute('UPDATE decisions SET hit=? WHERE issue=? AND hit IS NULL',(expected,issue))
            self.db.execute('INSERT INTO feedback(issue,payload_hash) VALUES(?,?)',(issue,digest))
            return True
    def checkpoint(self,owner:str,expected_version:int,through_issue:str,model_hash:str):
        if not model_hash:raise ValueError('ARTIFACT_REQUIRED')
        ordinal(through_issue)
        with self.db:
            old=self.db.execute('SELECT version,through_issue FROM checkpoints WHERE owner=?',(owner,)).fetchone()
            if old is None:
                if expected_version!=0:raise ValueError('STALE_CAS')
                self.db.execute('INSERT INTO checkpoints VALUES(?,?,?,?)',(owner,1,through_issue,model_hash))
            else:
                if old[0]!=expected_version or ordinal(through_issue)<=ordinal(old[1]):raise ValueError('STALE_CAS_OR_REWIND')
                self.db.execute('UPDATE checkpoints SET version=?,through_issue=?,model_hash=? WHERE owner=?',(old[0]+1,through_issue,model_hash,owner))
        return expected_version+1
    def losing_streak(self):
        # ABSTAIN is not a win and must not reset the REAL action sequence.
        cur=maximum=0
        for hit, in self.db.execute('SELECT hit FROM decisions WHERE real=1 AND hit IS NOT NULL ORDER BY issue'):
            cur=0 if hit else cur+1;maximum=max(maximum,cur)
        return cur,maximum
    def close(self):self.db.close()

def generation_credit(parent_behavior:str,child_behavior:str,changed_structure:bool,receipt:Receipt,paired_improvement_lower:float|None)->str:
    receipt.validate()
    if parent_behavior==child_behavior:return 'NO_OP'
    if not changed_structure:return 'PARAMETER_VARIANT'
    if receipt.qualification()!='ELIGIBLE_FOR_AUTHORITY_REVIEW_NOT_AUTO_REAL':return 'RESEARCH_GENERATION'
    if paired_improvement_lower is None or not math.isfinite(paired_improvement_lower) or paired_improvement_lower<=0:return 'UNPROVEN_IMPROVEMENT'
    return 'VERIFIED_GENERATION'
