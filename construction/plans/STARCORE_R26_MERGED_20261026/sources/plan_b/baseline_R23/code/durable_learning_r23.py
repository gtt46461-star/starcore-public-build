"""SQLite host reference for training independent of REAL actions.

Not a second production DB owner. Android must port these transactions to the
existing DatabaseCoordinator, with the existing SettlementEngine as sole owner.
No API here authorizes external execution. Artifact computation stays outside SQL.
"""
from __future__ import annotations
from pathlib import Path
import hashlib
import json
import sqlite3

DDL = """
CREATE TABLE IF NOT EXISTS labels(issue INTEGER PRIMARY KEY, payload TEXT NOT NULL, digest TEXT NOT NULL);
CREATE TABLE IF NOT EXISTS models(id TEXT PRIMARY KEY, through_issue INTEGER NOT NULL, artifact_hash TEXT NOT NULL);
CREATE TABLE IF NOT EXISTS learn_queue(model TEXT NOT NULL, issue INTEGER NOT NULL,
 state TEXT NOT NULL DEFAULT 'PENDING', PRIMARY KEY(model,issue));
CREATE TABLE IF NOT EXISTS predictions(issue INTEGER NOT NULL, policy TEXT NOT NULL,
 mode TEXT NOT NULL, position INTEGER NOT NULL, numbers TEXT NOT NULL,
 through_issue INTEGER NOT NULL, PRIMARY KEY(issue,policy,mode));
CREATE TABLE IF NOT EXISTS settlements(issue INTEGER NOT NULL,policy TEXT NOT NULL,
 mode TEXT NOT NULL,hit INTEGER NOT NULL,PRIMARY KEY(issue,policy,mode));
CREATE TABLE IF NOT EXISTS publications(model TEXT NOT NULL, through_issue INTEGER NOT NULL,
 artifact_hash TEXT NOT NULL, PRIMARY KEY(model,through_issue));
"""

class DurableLearning:
    def __init__(self, path: str | Path = ':memory:'):
        self.db = sqlite3.connect(str(path))
        self.db.executescript(DDL)
    def close(self):
        self.db.close()
    def register(self, model: str, through: int = 0, artifact: str = 'seed'):
        if not model or through < 0: raise ValueError('bad model')
        with self.db:
            self.db.execute('INSERT OR IGNORE INTO models VALUES (?,?,?)',(model,through,artifact))
            # Existing labels are usable for training even if no prediction or REAL exists.
            self.db.execute('INSERT OR IGNORE INTO learn_queue(model,issue) SELECT ?,issue FROM labels WHERE issue>?',(model,through))
    def freeze(self, issue:int, policy:str, mode:str, position:int, numbers:list[int], through:int):
        if mode not in ('LIVE_SHADOW','HISTORY_REPLAY'): raise ValueError('no REAL authority here')
        if not policy or not 0 <= position < 10 or len(numbers)!=3 or len(set(numbers))!=3 or not all(1<=x<=10 for x in numbers):
            raise ValueError('invalid K3')
        if through>=issue: raise ValueError('future or current label in input')
        payload=json.dumps(sorted(numbers))
        with self.db:
            if mode=='LIVE_SHADOW' and self.db.execute('SELECT 1 FROM labels WHERE issue=?',(issue,)).fetchone():
                raise ValueError('cannot backfill live prediction')
            old=self.db.execute('SELECT position,numbers,through_issue FROM predictions WHERE issue=? AND policy=? AND mode=?',(issue,policy,mode)).fetchone()
            if old and old!=(position,payload,through): raise ValueError('immutable freeze conflict')
            self.db.execute('INSERT OR IGNORE INTO predictions VALUES (?,?,?,?,?,?)',(issue,policy,mode,position,payload,through))
    def record_label(self, issue:int, draw:list[int], inject_failure:bool=False):
        if issue<1 or sorted(draw)!=list(range(1,11)): raise ValueError('invalid permutation')
        payload=json.dumps(draw,separators=(',',':'));digest=hashlib.sha256(payload.encode()).hexdigest()
        with self.db:
            old=self.db.execute('SELECT digest FROM labels WHERE issue=?',(issue,)).fetchone()
            if old and old[0]!=digest: raise ValueError('label revision must be explicitly reconciled')
            self.db.execute('INSERT OR IGNORE INTO labels VALUES (?,?,?)',(issue,payload,digest))
            for policy,mode,p,ns in self.db.execute('SELECT policy,mode,position,numbers FROM predictions WHERE issue=?',(issue,)).fetchall():
                self.db.execute('INSERT OR IGNORE INTO settlements VALUES (?,?,?,?)',(issue,policy,mode,int(draw[p] in json.loads(ns))))
            self.db.execute('INSERT OR IGNORE INTO learn_queue(model,issue) SELECT id,? FROM models WHERE through_issue<?',(issue,issue))
            if inject_failure: raise RuntimeError('injected transaction failure')
    def publish(self, model:str, expected:int, through:int, artifact:Path, digest:str, inject_failure:bool=False):
        # Blob fully exists and hashes before SQL; immutable hashes prevent partially written publication.
        if not artifact.is_file() or hashlib.sha256(artifact.read_bytes()).hexdigest()!=digest:
            raise ValueError('artifact not durable or corrupt')
        if through<=expected: raise ValueError('non-monotonic cursor')
        with self.db:
            pending=self.db.execute('SELECT COUNT(*) FROM learn_queue WHERE model=? AND issue>? AND issue<=? AND state=\'PENDING\'',(model,expected,through)).fetchone()[0]
            labels=self.db.execute('SELECT COUNT(*) FROM labels WHERE issue>? AND issue<=?',(expected,through)).fetchone()[0]
            # Host fixture uses consecutive integer issue sequences; production must use validated sequence IDs,
            # NOT subtract YYYYMMDD issue strings or assume chronological issues are contiguous integers.
            if labels!=through-expected or pending!=through-expected: raise ValueError('label/queue gap')
            n=self.db.execute('UPDATE models SET through_issue=?,artifact_hash=? WHERE id=? AND through_issue=?',(through,digest,model,expected)).rowcount
            if n!=1: raise ValueError('stale publication')
            self.db.execute('INSERT INTO publications VALUES (?,?,?)',(model,through,digest))
            self.db.execute('UPDATE learn_queue SET state=\'DONE\' WHERE model=? AND issue<=?',(model,through))
            if inject_failure: raise RuntimeError('injected publish failure')
        # Production may now publish the immutable RAM snapshot, but never before commit.
        return {'model':model,'through':through,'artifact':digest}
