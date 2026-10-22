#!/usr/bin/env python3
"""Executable B06 acceptance harness for the R926 immutable runtime ledger contract.

Uses real SQLite transactions/triggers to exercise duplicate callbacks, out-of-order delivery,
transaction rollback, projection rebuild, durable external outbox replay and read/export parity.
It also statically checks the production Java owner boundaries so the executable model cannot
silently drift from production.
"""
from __future__ import annotations
from pathlib import Path
import csv
import hashlib
import io
import json
import sqlite3
import tempfile
import time

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / 'app/src/main/java/com/ethk3/novaonepro/RuntimeEventLedgerR926.java'
HOOKS = ROOT / 'app/src/main/java/com/ethk3/novaonepro/RuntimeEventHooksR926.java'
RECOVERY = ROOT / 'app/src/main/java/com/ethk3/novaonepro/RuntimeRecoveryCoordinatorR926.java'
WRITER = ROOT / 'app/src/main/java/com/ethk3/novaonepro/RuntimeExternalEventWriterR926.java'
OUT = ROOT / 'reports/r926/batch1'
OUT.mkdir(parents=True, exist_ok=True)
GAME = 'Binance1-PK10'

SCHEMA = '''
CREATE TABLE event(
 seq INTEGER PRIMARY KEY AUTOINCREMENT,event_id TEXT NOT NULL UNIQUE,dedup_key TEXT NOT NULL UNIQUE,
 game_code TEXT NOT NULL,target_issue TEXT NOT NULL,epoch_id TEXT NOT NULL,attempt_id TEXT NOT NULL,
 event_type TEXT NOT NULL,stage TEXT NOT NULL,state_rank INTEGER NOT NULL,payload_text TEXT NOT NULL DEFAULT '',
 payload_hash TEXT NOT NULL,source TEXT NOT NULL,created_at INTEGER NOT NULL);
CREATE TABLE projection(
 game_code TEXT NOT NULL,target_issue TEXT NOT NULL,current_state TEXT NOT NULL,state_rank INTEGER NOT NULL,
 epoch_id TEXT NOT NULL,attempt_id TEXT NOT NULL,last_event_id TEXT NOT NULL,event_watermark INTEGER NOT NULL,
 request_seen INTEGER NOT NULL DEFAULT 0,accepted_seen INTEGER NOT NULL DEFAULT 0,rejected_seen INTEGER NOT NULL DEFAULT 0,
 queued_seen INTEGER NOT NULL DEFAULT 0,started_seen INTEGER NOT NULL DEFAULT 0,frozen_seen INTEGER NOT NULL DEFAULT 0,
 canonical_seen INTEGER NOT NULL DEFAULT 0,settled_seen INTEGER NOT NULL DEFAULT 0,full_seen INTEGER NOT NULL DEFAULT 0,
 learned_seen INTEGER NOT NULL DEFAULT 0,recovery_seen INTEGER NOT NULL DEFAULT 0,last_error_code TEXT NOT NULL DEFAULT '',
 as_of INTEGER NOT NULL,PRIMARY KEY(game_code,target_issue));
CREATE TABLE outbox(
 outbox_id TEXT PRIMARY KEY,dedup_key TEXT NOT NULL UNIQUE,target_issue TEXT NOT NULL,epoch_id TEXT NOT NULL,
 attempt_id TEXT NOT NULL,event_type TEXT NOT NULL,stage TEXT NOT NULL,state_rank INTEGER NOT NULL,
 payload_text TEXT NOT NULL DEFAULT '',payload_hash TEXT NOT NULL,source TEXT NOT NULL,status TEXT NOT NULL,
 attempts INTEGER NOT NULL DEFAULT 0,created_at INTEGER NOT NULL,updated_at INTEGER NOT NULL,last_error TEXT NOT NULL DEFAULT '');
CREATE TRIGGER no_update BEFORE UPDATE ON event BEGIN SELECT RAISE(ABORT,'R926_RUNTIME_EVENT_APPEND_ONLY'); END;
CREATE TRIGGER no_delete BEFORE DELETE ON event BEGIN SELECT RAISE(ABORT,'R926_RUNTIME_EVENT_APPEND_ONLY'); END;
CREATE TABLE business(target_issue TEXT PRIMARY KEY,value TEXT NOT NULL);
'''

FLAGS = {
    'REQUESTED': 'request_seen', 'ACCEPTED': 'accepted_seen', 'REJECTED': 'rejected_seen',
    'QUEUED': 'queued_seen', 'STARTED': 'started_seen', 'FROZEN': 'frozen_seen',
    'CANONICAL': 'canonical_seen', 'SETTLED': 'settled_seen', 'FULL': 'full_seen',
    'LEARNED': 'learned_seen', 'RECOVERY': 'recovery_seen'
}


def h(s: str) -> str:
    return hashlib.sha256(s.encode()).hexdigest()


def ev(issue, typ, stage, rank, dedup, created, epoch='e1', attempt='a1', payload=''):
    ph = h(payload)
    eid = h('|'.join([GAME, issue, epoch, attempt, typ, stage, str(rank), ph, dedup]))
    return dict(event_id=eid, dedup_key=dedup, game_code=GAME, target_issue=issue,
                epoch_id=epoch, attempt_id=attempt, event_type=typ, stage=stage,
                state_rank=rank, payload_text=payload, payload_hash=ph,
                source='HARNESS', created_at=created)


def append(c: sqlite3.Connection, e: dict) -> bool:
    cur = c.execute('''INSERT OR IGNORE INTO event(event_id,dedup_key,game_code,target_issue,epoch_id,attempt_id,event_type,stage,
           state_rank,payload_text,payload_hash,source,created_at)
           VALUES(:event_id,:dedup_key,:game_code,:target_issue,:epoch_id,:attempt_id,:event_type,:stage,
                  :state_rank,:payload_text,:payload_hash,:source,:created_at)''', e)
    if cur.rowcount != 1:
        return False
    seq = c.execute('SELECT seq FROM event WHERE event_id=?', (e['event_id'],)).fetchone()[0]
    apply(c, e, seq, e['event_id'])
    return True


def apply(c, e, seq, event_id):
    row = c.execute('SELECT * FROM projection WHERE game_code=? AND target_issue=?',
                    (GAME, e['target_issue'])).fetchone()
    cols = [d[0] for d in c.execute('SELECT * FROM projection LIMIT 0').description]
    old = dict(zip(cols, row)) if row else {}
    rank = int(old.get('state_rank', -1))
    watermark = int(old.get('event_watermark', 0))
    asof = int(old.get('as_of', 0))
    advances = e['state_rank'] >= rank
    vals = {
        'game_code': GAME,
        'target_issue': e['target_issue'],
        'current_state': e['stage'] if advances else old.get('current_state', 'LEGACY_UNKNOWN'),
        'state_rank': max(rank, e['state_rank']),
        'epoch_id': e['epoch_id'] if advances and e['epoch_id'] else old.get('epoch_id', ''),
        'attempt_id': e['attempt_id'] if advances and e['attempt_id'] else old.get('attempt_id', ''),
        'last_event_id': event_id,
        'event_watermark': max(watermark, seq),
        'last_error_code': e['stage'] if e['event_type'] == 'REJECTED' else old.get('last_error_code', ''),
        'as_of': max(asof, e['created_at'])
    }
    for typ, col in FLAGS.items():
        vals[col] = 1 if old.get(col, 0) == 1 or e['event_type'] == typ else 0
    names = list(vals)
    qs = ','.join('?' for _ in names)
    c.execute(f"INSERT OR REPLACE INTO projection({','.join(names)}) VALUES({qs})",
              [vals[n] for n in names])


def rebuild(c, issue):
    c.execute('DELETE FROM projection WHERE game_code=? AND target_issue=?', (GAME, issue))
    rows = c.execute('SELECT * FROM event WHERE game_code=? AND target_issue=? ORDER BY seq',
                     (GAME, issue)).fetchall()
    cols = [d[0] for d in c.execute('SELECT * FROM event LIMIT 0').description]
    for row in rows:
        e = dict(zip(cols, row))
        apply(c, e, e['seq'], e['event_id'])


def durable(c, dedup):
    return c.execute('SELECT 1 FROM event WHERE dedup_key=? LIMIT 1', (dedup,)).fetchone() is not None


def enqueue_external(c, e):
    if durable(c, e['dedup_key']):
        return True
    outbox_id = h('OUTBOX|' + e['event_id'])
    cur = c.execute('''INSERT OR IGNORE INTO outbox(outbox_id,dedup_key,target_issue,epoch_id,attempt_id,event_type,stage,state_rank,
           payload_text,payload_hash,source,status,created_at,updated_at)
           VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?)''',
        (outbox_id, e['dedup_key'], e['target_issue'], e['epoch_id'], e['attempt_id'],
         e['event_type'], e['stage'], e['state_rank'], e['payload_text'], e['payload_hash'],
         e['source'], 'PENDING', e['created_at'], e['created_at']))
    if cur.rowcount == 1:
        return True
    return c.execute('SELECT 1 FROM outbox WHERE dedup_key=? LIMIT 1',
                     (e['dedup_key'],)).fetchone() is not None


def drain_outbox(c, limit=100):
    rows = c.execute('''SELECT outbox_id,target_issue,epoch_id,attempt_id,event_type,stage,state_rank,
                  payload_text,payload_hash,source,dedup_key,created_at
           FROM outbox WHERE status='PENDING' ORDER BY created_at LIMIT ?''', (limit,)).fetchall()
    done = 0
    for r in rows:
        outbox_id, issue, epoch, attempt, typ, stage, rank, payload, _payload_hash, source, dedup, created = r
        e = ev(issue, typ, stage, rank, dedup, created, epoch, attempt, payload)
        e['source'] = source
        c.execute('SAVEPOINT r926_drain')
        try:
            inserted = append(c, e)
            if not inserted:
                if not durable(c, dedup):
                    raise RuntimeError('R926_EVENT_DRAIN_NOT_DURABLE')
                rebuild(c, issue)
            c.execute("UPDATE outbox SET status='COMPLETED',attempts=attempts+1,updated_at=?,last_error='' WHERE outbox_id=?",
                (int(time.time() * 1000), outbox_id))
            c.execute('RELEASE r926_drain')
            done += 1
        except Exception as exc:
            c.execute('ROLLBACK TO r926_drain')
            c.execute('RELEASE r926_drain')
            c.execute("UPDATE outbox SET status='PENDING',attempts=attempts+1,updated_at=?,last_error=? WHERE outbox_id=?",
                (int(time.time() * 1000), str(exc)[:500], outbox_id))
    return done


def external_append(c, e, do_drain=True):
    c.execute('SAVEPOINT r926_enqueue')
    try:
        if not enqueue_external(c, e):
            raise RuntimeError('R926_EVENT_OUTBOX_ENQUEUE_FAILED')
        c.execute('RELEASE r926_enqueue')
    except Exception:
        c.execute('ROLLBACK TO r926_enqueue')
        c.execute('RELEASE r926_enqueue')
        return False
    if do_drain:
        drain_outbox(c, 64)
    return durable(c, e['dedup_key'])


def canon(c, issue):
    r = c.execute('SELECT * FROM projection WHERE game_code=? AND target_issue=?',
                  (GAME, issue)).fetchone()
    cols = [d[0] for d in c.execute('SELECT * FROM projection LIMIT 0').description]
    return dict(zip(cols, r)) if r else None


def export_rows(c, limit):
    cols = ['target_issue', 'current_state', 'state_rank', 'epoch_id', 'attempt_id',
            'event_watermark', 'as_of', 'last_error_code', 'request_seen', 'accepted_seen',
            'rejected_seen', 'queued_seen', 'started_seen', 'frozen_seen', 'canonical_seen',
            'settled_seen', 'full_seen', 'learned_seen', 'recovery_seen']
    rows = c.execute('SELECT ' + ','.join(cols) +
        ' FROM projection WHERE game_code=? ORDER BY target_issue DESC,event_watermark DESC LIMIT ?',
        (GAME, limit)).fetchall()
    out = io.StringIO()
    w = csv.writer(out, lineterminator='\n')
    w.writerow(cols)
    w.writerows(rows)
    return out.getvalue(), [dict(zip(cols, r)) for r in rows]


def main():
    src = JAVA.read_text(encoding='utf-8')
    hooks = HOOKS.read_text(encoding='utf-8')
    recovery = RECOVERY.read_text(encoding='utf-8')
    writer = WRITER.read_text(encoding='utf-8')
    a = src.index('public static boolean appendWithinTransaction')
    b = src.index('public static boolean append(DbStore', a)
    dml_body = src[a:b]

    assertions = {
        'business_append_has_no_schema_ddl': all(x not in dml_body for x in ['CREATE TABLE', 'CREATE TRIGGER', 'ALTER TABLE']),
        'append_only_trigger_in_source': 'R926_RUNTIME_EVENT_APPEND_ONLY' in src,
        'deterministic_projection_asof': 'Math.max(existingAsOf, e.createdAt)' in src,
        'replay_uses_stored_event_id': 'Event.replay(' in src and 'c.getString(0)' in src,
        'outbox_error_persisted_after_end_transaction': src.find('if (failure != null)') > src.find('db.endTransaction();'),
        'external_append_commits_outbox_before_drain': 'R926_EXTERNAL_EVENT_OUTBOX_ENQUEUE_' in writer and 'RuntimeEventLedgerR926.enqueueExternal(db, event)' in writer,
        'external_append_verifies_ledger_durability': 'R926_EXTERNAL_EVENT_VERIFY_' in writer and 'eventDurable(db, event.dedupKey)' in writer,
        'hooks_return_real_persistence_result': all(x in hooks for x in [
            'public static boolean queued(', 'public static boolean learned(',
            'public static boolean recovery(', 'private static boolean append(']) and 'RuntimeExternalEventWriterR926.append(' in hooks and 'RuntimeEventLedgerR926.append(' not in hooks,
        'recovery_evidence_uses_hook_result': 'eventWritten = RuntimeEventHooksR926.recovery(' in recovery,
    }

    with tempfile.TemporaryDirectory(prefix='r926-ledger-') as td:
        db_path = Path(td) / 'ledger.db'
        db = sqlite3.connect(str(db_path))
        db.executescript(SCHEMA)
        base = 1700000000000
        issue = '202609080001'
        append(db, ev(issue, 'REQUESTED', 'REQUESTED', 10, 'req', base + 1))
        append(db, ev(issue, 'ACCEPTED', 'ACCEPTED', 20, 'acc', base + 2))
        append(db, ev(issue, 'QUEUED', 'QUEUED', 30, 'q', base + 3))
        append(db, ev(issue, 'STARTED', 'RUNNING', 40, 's', base + 4))
        append(db, ev(issue, 'FROZEN', 'PIT_FROZEN', 50, 'f', base + 5))
        for _ in range(10):
            append(db, ev(issue, 'CANONICAL', 'CANONICAL_COMMITTED', 60, 'canonical', base + 6, payload='action_rows=1200'))
        for _ in range(10):
            append(db, ev(issue, 'SETTLED', 'COMPLETED', 70, 'settled', base + 7))
        append(db, ev(issue, 'FULL', 'FULL_EVIDENCE_COMPLETED', 80, 'full', base + 8))
        append(db, ev(issue, 'LEARNED', 'LEARNED', 90, 'learn', base + 9))
        append(db, ev(issue, 'REQUESTED', 'LATE_REQUEST', 10, 'late', base + 10))
        db.commit()
        p1 = canon(db, issue)
        assertions['duplicate_10_canonical_is_one'] = db.execute("SELECT COUNT(*) FROM event WHERE target_issue=? AND event_type='CANONICAL'", (issue,)).fetchone()[0] == 1
        assertions['duplicate_10_settlement_is_one'] = db.execute("SELECT COUNT(*) FROM event WHERE target_issue=? AND event_type='SETTLED'", (issue,)).fetchone()[0] == 1
        assertions['late_low_rank_does_not_regress'] = p1['state_rank'] == 90 and p1['current_state'] == 'LEARNED' and p1['request_seen'] == 1
        before = {k: p1[k] for k in p1}
        rebuild(db, issue)
        p2 = canon(db, issue)
        assertions['projection_rebuild_exact'] = before == p2
        for op, key in [("UPDATE event SET stage='BAD' WHERE target_issue=?", 'update_rejected'), ("DELETE FROM event WHERE target_issue=?", 'delete_rejected')]:
            try:
                db.execute(op, (issue,))
                assertions[key] = False
            except sqlite3.DatabaseError as exc:
                assertions[key] = 'R926_RUNTIME_EVENT_APPEND_ONLY' in str(exc)
        db.rollback()
        issue2 = '202609080002'
        try:
            db.execute('BEGIN')
            db.execute('INSERT INTO business VALUES(?,?)', (issue2, 'canonical'))
            append(db, ev(issue2, 'CANONICAL', 'CANONICAL_COMMITTED', 60, 'c2', base + 20))
            raise RuntimeError('KILLPOINT')
        except RuntimeError:
            db.rollback()
        assertions['killpoint_rolls_back_business_and_event'] = db.execute('SELECT COUNT(*) FROM business WHERE target_issue=?', (issue2,)).fetchone()[0] == 0 and db.execute('SELECT COUNT(*) FROM event WHERE target_issue=?', (issue2,)).fetchone()[0] == 0
        db.execute('BEGIN')
        db.execute('INSERT INTO business VALUES(?,?)', (issue2, 'canonical'))
        append(db, ev(issue2, 'CANONICAL', 'CANONICAL_COMMITTED', 60, 'c2', base + 21))
        db.commit()
        assertions['retry_commits_exactly_once'] = db.execute('SELECT COUNT(*) FROM business WHERE target_issue=?', (issue2,)).fetchone()[0] == 1 and db.execute("SELECT COUNT(*) FROM event WHERE target_issue=? AND event_type='CANONICAL'", (issue2,)).fetchone()[0] == 1
        issue3 = '202609080003'
        e3 = ev(issue3, 'RECOVERY', 'R926_TEST', 10, 'external-recovery', base + 30)
        assert enqueue_external(db, e3)
        db.commit()
        assertions['external_handoff_survives_before_drain'] = db.execute("SELECT COUNT(*) FROM outbox WHERE dedup_key=? AND status='PENDING'", (e3['dedup_key'],)).fetchone()[0] == 1 and not durable(db, e3['dedup_key'])
        db.close()
        db = sqlite3.connect(str(db_path))
        assertions['startup_outbox_replay_commits_event_projection'] = drain_outbox(db, 100) == 1 and durable(db, e3['dedup_key']) and canon(db, issue3) is not None and db.execute("SELECT status FROM outbox WHERE dedup_key=?", (e3['dedup_key'],)).fetchone()[0] == 'COMPLETED'
        db.commit()
        issue4 = '202609080004'
        e4 = ev(issue4, 'LEARNED', 'LEARNED', 90, 'external-dup', base + 40)
        for _ in range(10):
            external_append(db, e4, True)
        assertions['external_duplicate_10_exactly_once'] = db.execute('SELECT COUNT(*) FROM event WHERE dedup_key=?', (e4['dedup_key'],)).fetchone()[0] == 1 and db.execute('SELECT COUNT(*) FROM outbox WHERE dedup_key=?', (e4['dedup_key'],)).fetchone()[0] == 1
        csv20, r20 = export_rows(db, 20)
        _csv500, r500 = export_rows(db, 500)
        assertions['recent20_is_prefix_of_500'] = r20 == r500[:len(r20)]
        assertions['csv_roundtrip_matches_projection'] = [dict(r) for r in csv.DictReader(io.StringIO(csv20))] == [{k: str(v) for k, v in x.items()} for x in r20]
        db.close()

    passed = all(assertions.values()) and len(assertions) == 22
    result = {'task': 'B06', 'passed': passed, 'assertion_count': len(assertions), 'assertions': assertions,
              'proof_scope': 'SQLITE_EXECUTABLE_LEDGER_SEMANTICS_PLUS_JAVA_BOUNDARY_STATIC_CHECK'}
    (OUT / 'B06_EVENT_LEDGER_HARNESS.json').write_text(json.dumps(result, indent=2, ensure_ascii=False), encoding='utf-8')
    print(json.dumps(result, ensure_ascii=False))
    return 0 if passed else 1


if __name__ == '__main__':
    raise SystemExit(main())
