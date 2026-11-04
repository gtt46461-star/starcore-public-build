"""Executable R31 reference contracts. NOT Android implementation or acceptance.

Only standard-library dependencies. SQL schema names are deliberately isolated
from production tables: integration must preserve the production owner contracts.
"""
from __future__ import annotations

from collections import OrderedDict
from contextlib import contextmanager
from dataclasses import dataclass
import hashlib
import json
from pathlib import Path
import sqlite3

SCOPE = 'REFERENCE_CONTRACT_ONLY'


class BusyWait(Exception):
    """SQLite refused entry into the write transaction; no task defect."""


class DependencyWait(Exception):
    pass


class StaleFence(Exception):
    pass


class ConflictingEvent(Exception):
    pass


class BudgetRefused(Exception):
    pass


def connect(path: str | Path) -> sqlite3.Connection:
    db = sqlite3.connect(str(path), timeout=0.02, isolation_level=None)
    db.row_factory = sqlite3.Row
    db.execute('PRAGMA foreign_keys=ON')
    db.execute('PRAGMA journal_mode=WAL')
    return db


def install(db: sqlite3.Connection) -> None:
    db.executescript(Path(__file__).with_name('schema.sql').read_text())


def is_busy(exc: BaseException) -> bool:
    code = getattr(exc, 'sqlite_errorcode', None)
    return isinstance(exc, sqlite3.OperationalError) and code is not None and (
        code & 255) in (sqlite3.SQLITE_BUSY, sqlite3.SQLITE_LOCKED)


@contextmanager
def write_tx(db: sqlite3.Connection):
    try:
        db.execute('BEGIN IMMEDIATE')
    except sqlite3.OperationalError as exc:
        if is_busy(exc):
            raise BusyWait('WRITER_BUSY') from exc
        raise
    try:
        yield
        db.commit()
    except BaseException:
        db.rollback()
        raise


@dataclass(frozen=True)
class Clock:
    boot_id: str
    elapsed_ms: int

    def __post_init__(self):
        if not self.boot_id or self.elapsed_ms < 0:
            raise ValueError('INVALID_CLOCK')


@dataclass(frozen=True)
class Claim:
    task_id: str
    owner: str
    fence: int
    boot_id: str


class TaskStore:
    ERROR_WINDOW_MS = 600_000
    ERROR_THRESHOLD = 3
    MAX_QUARANTINE_ROUNDS = 3
    BASE_QUARANTINE_MS = 60_000

    def __init__(self, db):
        self.db = db

    def register(self, task_id):
        with write_tx(self.db):
            self.db.execute('INSERT OR IGNORE INTO task(task_id) VALUES(?)', (task_id,))

    def stop(self, task_id):
        with write_tx(self.db):
            self.db.execute("UPDATE task SET state='STOPPED',fence=fence+1,owner=NULL,lease_until_ms=0 WHERE task_id=?",
                            (task_id,))

    def claim(self, task_id, owner, now: Clock, lease_ms=5_000):
        if lease_ms <= 0 or lease_ms > 60_000:
            raise ValueError('LEASE_OUT_OF_RANGE')
        with write_tx(self.db):
            row = self.db.execute('SELECT * FROM task WHERE task_id=?', (task_id,)).fetchone()
            if row is None:
                raise DependencyWait('TASK_NOT_REGISTERED')
            if row['state'] in ('NEEDS_FIX', 'STOPPED'):
                raise DependencyWait(row['state'])
            same_boot = row['boot_id'] == now.boot_id
            if same_boot and row['lease_until_ms'] > now.elapsed_ms:
                raise DependencyWait('ACTIVE_LEASE')
            if row['state'] == 'QUARANTINED' and same_boot and row['quarantine_until_ms'] > now.elapsed_ms:
                raise DependencyWait('CIRCUIT_OPEN')
            fence = row['fence'] + 1
            changed = self.db.execute(
                "UPDATE task SET owner=?,fence=?,boot_id=?,lease_until_ms=?,state='RUNNING',"
                "half_open=?,wait_kind=NULL,error_count=?,error_start_ms=? WHERE task_id=? AND fence=?",
                (owner, fence, now.boot_id, now.elapsed_ms + lease_ms,
                 int(row['state'] == 'QUARANTINED'),
                 row['error_count'] if same_boot else 0,
                 row['error_start_ms'] if same_boot else 0,
                 task_id, row['fence']))
            if changed.rowcount != 1:
                raise StaleFence('CLAIM_CAS_FAILED')
            return Claim(task_id, owner, fence, now.boot_id)

    def _current(self, claim, now):
        row = self.db.execute('SELECT * FROM task WHERE task_id=?', (claim.task_id,)).fetchone()
        if row is None or row['owner'] != claim.owner or row['fence'] != claim.fence \
                or row['boot_id'] != claim.boot_id or now.boot_id != claim.boot_id \
                or row['state'] != 'RUNNING' or row['lease_until_ms'] <= now.elapsed_ms:
            raise StaleFence('OWNER_FENCE_OR_LEASE_CHANGED')
        return row

    def renew(self, claim, now, lease_ms=5_000):
        if not 0 < lease_ms <= 60_000:
            raise ValueError('LEASE_OUT_OF_RANGE')
        with write_tx(self.db):
            self._current(claim, now)
            self.db.execute('UPDATE task SET lease_until_ms=? WHERE task_id=?',
                            (now.elapsed_ms + lease_ms, claim.task_id))

    def commit_progress(self, claim, now, event_key, payload: bytes, cursor):
        digest = hashlib.sha256(payload).hexdigest()
        with write_tx(self.db):
            old = self.db.execute('SELECT payload_hash,cursor FROM task_commit WHERE task_id=? AND event_key=?',
                                  (claim.task_id, event_key)).fetchone()
            if old:
                if old['payload_hash'] != digest or old['cursor'] != cursor:
                    raise ConflictingEvent('REUSED_EVENT_WITH_DIFFERENT_PAYLOAD')
                return 'ALREADY_COMMITTED'
            row = self._current(claim, now)
            if cursor < row['cursor']:
                raise ValueError('CURSOR_REGRESSION')
            self.db.execute('INSERT INTO task_commit VALUES(?,?,?,?,?,?)',
                            (claim.task_id, event_key, digest, claim.owner, claim.fence, cursor))
            self.db.execute("UPDATE task SET cursor=?,state='READY',owner=NULL,lease_until_ms=0,"
                            'error_count=0,error_start_ms=0,half_open=0,wait_kind=NULL WHERE task_id=?',
                            (cursor, claim.task_id))
            return 'COMMITTED'

    def wait(self, claim, now, kind):
        if kind not in ('DEPENDENCY', 'CANCELLED', 'BUSY'):
            raise ValueError('INVALID_WAIT_KIND')
        with write_tx(self.db):
            self._current(claim, now)
            self.db.execute("UPDATE task SET state='WAITING',owner=NULL,lease_until_ms=0,wait_kind=? WHERE task_id=?",
                            (kind, claim.task_id))

    def failure(self, claim, now, exc):
        # Caller must leave its worker loop after cancellation. Never clear the
        # thread/structured cancellation signal and continue a stale computation.
        if isinstance(exc, InterruptedError):
            try:
                self.wait(claim, now, 'CANCELLED')
            except (BusyWait, StaleFence):
                # Cancellation is stronger than a best-effort receipt. The
                # durable lease/fence remains the recovery authority.
                pass
            raise exc
        if isinstance(exc, DependencyWait):
            self.wait(claim, now, 'DEPENDENCY')
            return 'WAITING'
        if isinstance(exc, BusyWait) or is_busy(exc):
            # Releasing the lease also needs a writer. If that is busy, let the
            # durable lease expire; caller keeps a bounded coalesced diagnostic.
            self.wait(claim, now, 'BUSY')
            return 'WAITING'
        if isinstance(exc, (KeyboardInterrupt, SystemExit)):
            raise exc
        with write_tx(self.db):
            row = self._current(claim, now)
            start = row['error_start_ms']
            count = row['error_count']
            if count == 0 or now.elapsed_ms - start >= self.ERROR_WINDOW_MS:
                start, count = now.elapsed_ms, 0
            count += 1
            rounds = row['quarantine_rounds']
            state, until = 'WAITING', 0
            if row['half_open'] or count >= self.ERROR_THRESHOLD:
                rounds += 1
                state = 'NEEDS_FIX' if rounds >= self.MAX_QUARANTINE_ROUNDS else 'QUARANTINED'
                until = now.elapsed_ms + min(900_000, self.BASE_QUARANTINE_MS * 2 ** (rounds - 1))
            self.db.execute('UPDATE task SET error_start_ms=?,error_count=?,quarantine_rounds=?,state=?, '
                            'quarantine_until_ms=?,lease_until_ms=0,owner=NULL,half_open=0,wait_kind=? WHERE task_id=?',
                            (start, count, rounds, state, until, type(exc).__name__, claim.task_id))
            return state


class ForecastStore:
    def __init__(self, db):
        self.db = db

    def freeze(self, *, stream, issue, ordinal, model_hash, feature_max_ordinal,
               position, numbers, input_hash, clock_before: Clock,
               deadline_elapsed_ms, clock_after_commit, after_commit_hook=None):
        """Use a commit-ack upper bound. A late/unknown ack is never predraw.

        clock_after_commit() must read the same trusted monotonic clock after
        SQLite commit returns. The optional hook exists solely for fault tests.
        """
        if feature_max_ordinal >= ordinal:
            raise ValueError('PIT_FUTURE_INPUT')
        if len(numbers) != 3 or len(set(numbers)) != 3 or any(type(n) is not int or not 1 <= n <= 10 for n in numbers):
            raise ValueError('FIXED_K3_REQUIRED')
        if not 0 <= position < 10:
            raise ValueError('INVALID_POSITION')
        if clock_before.elapsed_ms >= deadline_elapsed_ms:
            raise ValueError('FREEZE_DEADLINE_PASSED')
        body = {'stream': stream, 'issue': issue, 'ordinal': ordinal, 'model_hash': model_hash,
                'feature_max_ordinal': feature_max_ordinal, 'position': position,
                'numbers': sorted(numbers), 'input_hash': input_hash}
        digest = hashlib.sha256(json.dumps(body, sort_keys=True, separators=(',', ':')).encode()).hexdigest()
        identity = hashlib.sha256(json.dumps([stream, issue, model_hash], separators=(',', ':')).encode()).hexdigest()
        with write_tx(self.db):
            existing = self.db.execute('SELECT prediction_hash FROM forecast WHERE forecast_id=?', (identity,)).fetchone()
            if existing:
                if existing['prediction_hash'] != digest:
                    raise ConflictingEvent('FROZEN_PREDICTION_ALREADY_EXISTS')
                # A retry cannot fabricate a lost receipt using a later clock.
                return identity, 'EXISTING_IMMUTABLE_FORECAST'
            if self.db.execute('SELECT 1 FROM reveal WHERE stream=? AND issue=?', (stream, issue)).fetchone():
                raise ValueError('OUTCOME_ALREADY_OBSERVED')
            self.db.execute('INSERT INTO forecast VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?)',
                            (identity, stream, issue, ordinal, model_hash, feature_max_ordinal,
                             position, json.dumps(sorted(numbers)), input_hash, digest,
                             clock_before.boot_id, deadline_elapsed_ms, clock_before.elapsed_ms))
        if after_commit_hook:
            after_commit_hook()
        ack = clock_after_commit()
        valid = ack.boot_id == clock_before.boot_id and clock_before.elapsed_ms <= ack.elapsed_ms < deadline_elapsed_ms
        qualification = 'PREDRAW' if valid else 'LATE_OR_UNKNOWN'
        with write_tx(self.db):
            self.db.execute('INSERT INTO forecast_commit_receipt VALUES(?,?,?,?)',
                            (identity, ack.elapsed_ms, ack.boot_id, qualification))
        return identity, qualification

    def reveal(self, stream, issue, ordinal, numbers):
        if len(numbers) != 10 or set(numbers) != set(range(1, 11)):
            raise ValueError('DRAW_MUST_BE_PERMUTATION')
        content = json.dumps(numbers, separators=(',', ':'))
        digest = hashlib.sha256(content.encode()).hexdigest()
        with write_tx(self.db):
            old = self.db.execute('SELECT * FROM reveal WHERE stream=? AND issue=?', (stream, issue)).fetchone()
            if old:
                if old['draw_hash'] != digest or old['issue_ordinal'] != ordinal:
                    raise ConflictingEvent('DRAW_REVISION_CONFLICT')
                return digest
            self.db.execute('INSERT INTO reveal VALUES(?,?,?,?,?)', (stream, issue, ordinal, digest, content))
        return digest

    def settle(self, forecast_id, fail_after_settlement=None):
        with write_tx(self.db):
            frozen = self.db.execute('SELECT * FROM forecast WHERE forecast_id=?', (forecast_id,)).fetchone()
            receipt = self.db.execute('SELECT * FROM forecast_commit_receipt WHERE forecast_id=?', (forecast_id,)).fetchone()
            if frozen is None or receipt is None or receipt['qualification'] != 'PREDRAW':
                raise DependencyWait('NO_VERIFIED_PREDRAW_COMMIT_RECEIPT')
            draw = self.db.execute('SELECT * FROM reveal WHERE stream=? AND issue=?',
                                   (frozen['stream'], frozen['issue'])).fetchone()
            if draw is None:
                raise DependencyWait('DRAW_NOT_COMMITTED')
            if draw['issue_ordinal'] != frozen['issue_ordinal']:
                raise ConflictingEvent('CALENDAR_IDENTITY_MISMATCH')
            hit = int(json.loads(draw['numbers_json'])[frozen['position']] in json.loads(frozen['numbers_json']))
            old = self.db.execute('SELECT * FROM settlement WHERE forecast_id=?', (forecast_id,)).fetchone()
            if old:
                if old['draw_hash'] != draw['draw_hash'] or old['hit'] != hit:
                    raise ConflictingEvent('SETTLEMENT_CONFLICT')
                return 'ALREADY_SETTLED'
            self.db.execute('INSERT INTO settlement VALUES(?,?,?)', (forecast_id, draw['draw_hash'], hit))
            if fail_after_settlement:
                fail_after_settlement()
            self.db.execute('INSERT INTO feedback_outbox VALUES(?,?,?,?)',
                            ('settle:' + forecast_id, forecast_id, draw['draw_hash'], hit))
            return 'SETTLED'

    def apply_feedback(self, consumer_id, event_id, apply_model_in_tx):
        with write_tx(self.db):
            event = self.db.execute('SELECT * FROM feedback_outbox WHERE event_id=?', (event_id,)).fetchone()
            if not event:
                raise DependencyWait('NO_FEEDBACK_EVENT')
            digest = hashlib.sha256(json.dumps(dict(event), sort_keys=True).encode()).hexdigest()
            old = self.db.execute('SELECT payload_hash FROM learning_applied WHERE consumer_id=? AND event_id=?',
                                  (consumer_id, event_id)).fetchone()
            if old:
                if old['payload_hash'] != digest:
                    raise ConflictingEvent('FEEDBACK_PAYLOAD_CHANGED')
                return 'ALREADY_APPLIED'
            # Callback must perform only bounded writes on this transaction.
            # Expensive training runs outside it, then fences its checkpoint.
            apply_model_in_tx(self.db, dict(event))
            self.db.execute('INSERT INTO learning_applied VALUES(?,?,?)', (consumer_id, event_id, digest))
            return 'APPLIED'


class ObjectBudget:
    def __init__(self, db, global_cap_bytes, emergency_disk_bytes=64):
        self.db, self.global_cap, self.emergency = db, global_cap_bytes, emergency_disk_bytes

    def configure_pool(self, pool, cap):
        with write_tx(self.db):
            self.db.execute('INSERT INTO budget_pool VALUES(?,?)', (pool, cap))

    def used(self, pool=None):
        suffix, args = (' WHERE pool=?', (pool,)) if pool else ('', ())
        return self.db.execute('SELECT coalesce(sum(bytes),0) FROM object_catalog' + suffix, args).fetchone()[0] + \
            self.db.execute('SELECT coalesce(sum(bytes),0) FROM budget_reservation' + suffix, args).fetchone()[0]

    def reserve(self, token, pool, worst_case_bytes, disk_free_bytes, purpose):
        if worst_case_bytes < 0:
            raise ValueError('NEGATIVE_RESERVATION')
        with write_tx(self.db):
            old = self.db.execute('SELECT * FROM budget_reservation WHERE token=?', (token,)).fetchone()
            if old:
                if (old['pool'], old['bytes'], old['purpose']) != (pool, worst_case_bytes, purpose):
                    raise ConflictingEvent('RESERVATION_TOKEN_REUSED')
                return 'ALREADY_RESERVED'
            cap = self.db.execute('SELECT cap_bytes FROM budget_pool WHERE pool=?', (pool,)).fetchone()
            if cap is None:
                raise BudgetRefused('UNREGISTERED_POOL')
            if disk_free_bytes - worst_case_bytes < self.emergency:
                raise BudgetRefused('LOW_PHYSICAL_DISK')
            if self.used() + worst_case_bytes > self.global_cap or self.used(pool) + worst_case_bytes > cap[0]:
                raise BudgetRefused('BUDGET_CAP')
            self.db.execute('INSERT INTO budget_reservation VALUES(?,?,?,?)',
                            (token, pool, worst_case_bytes, purpose))
            return 'RESERVED'

    def publish(self, token, object_id, exact_bytes, *, root_id=None, kind='CHECKPOINT', children=(), before_root_swap=None):
        """Caller verifies immutable file + fsync before SQL publication.

        The reservation must cover old + new coexistence, temp/rename behavior
        and measured per-filesystem allocation. This catalog models byte budgets;
        it does not replace Android's file publisher/recovery journal.
        """
        with write_tx(self.db):
            reservation = self.db.execute('SELECT * FROM budget_reservation WHERE token=?', (token,)).fetchone()
            if reservation is None or exact_bytes < 0 or exact_bytes > reservation['bytes']:
                raise BudgetRefused('NO_SUFFICIENT_RESERVATION')
            old = self.db.execute('SELECT * FROM object_catalog WHERE object_id=?', (object_id,)).fetchone()
            if old:
                if old['bytes'] != exact_bytes or old['pool'] != reservation['pool'] or old['state'] != 'LIVE':
                    raise ConflictingEvent('OBJECT_IDENTITY_OR_STATE_CONFLICT')
            else:
                self.db.execute('INSERT INTO object_catalog(object_id,pool,bytes) VALUES(?,?,?)',
                                (object_id, reservation['pool'], exact_bytes))
            for child in children:
                self.db.execute('INSERT OR IGNORE INTO object_edge VALUES(?,?)', (object_id, child))
            if before_root_swap:
                before_root_swap()
            if root_id is not None:
                self.db.execute('INSERT INTO object_root VALUES(?,?,?) ON CONFLICT(root_id) DO UPDATE SET object_id=excluded.object_id,kind=excluded.kind',
                                (root_id, object_id, kind))
            self.db.execute('DELETE FROM budget_reservation WHERE token=?', (token,))

    def root(self, root_id, object_id, kind):
        with write_tx(self.db):
            self.db.execute('INSERT INTO object_root VALUES(?,?,?)', (root_id, object_id, kind))

    def release_root(self, root_id):
        # A production caller must first prove consumer/settlement completion.
        # No automatic expiry is permitted for certificate or unresolved roots.
        with write_tx(self.db):
            self.db.execute('DELETE FROM object_root WHERE root_id=?', (root_id,))

    def mark_unreachable(self):
        with write_tx(self.db):
            # UNION (not UNION ALL) terminates on shared edges and cycles.
            self.db.execute('DELETE FROM temp.gc_mark')
            self.db.execute('''WITH RECURSIVE reachable(id) AS (
                SELECT object_id FROM object_root
                UNION SELECT e.child FROM object_edge e JOIN reachable r ON e.parent=r.id
            ) INSERT INTO temp.gc_mark SELECT object_id,bytes FROM object_catalog
              WHERE state='LIVE' AND object_id NOT IN (SELECT id FROM reachable)''')
            count = self.db.execute('SELECT count(*) FROM temp.gc_mark').fetchone()[0]
            self.db.execute("UPDATE object_catalog SET state='DELETE_PENDING' WHERE object_id IN (SELECT object_id FROM temp.gc_mark)")
            self.db.execute("INSERT OR IGNORE INTO gc_receipt SELECT object_id,bytes,'PLANNED' FROM temp.gc_mark")
            self.db.execute('DELETE FROM temp.gc_mark')
            return count

    def sweep(self, unlink, limit=128):
        if not 1 <= limit <= 1024:
            raise ValueError('INVALID_SWEEP_LIMIT')
        deleted = []
        pending = self.db.execute("SELECT object_id FROM object_catalog WHERE state='DELETE_PENDING' ORDER BY object_id LIMIT ?", (limit,)).fetchall()
        for row in pending:
            object_id = row['object_id']
            # File I/O is deliberately outside SQLite's write transaction.
            # Unlink must be idempotent: missing file after a crash means success.
            try:
                unlink(object_id)
            except OSError:
                with write_tx(self.db):
                    self.db.execute("UPDATE gc_receipt SET status='UNLINK_FAILED' WHERE object_id=?", (object_id,))
                continue
            with write_tx(self.db):
                self.db.execute('DELETE FROM object_edge WHERE parent=? OR child=?', (object_id, object_id))
                self.db.execute("DELETE FROM object_catalog WHERE object_id=? AND state='DELETE_PENDING'", (object_id,))
                self.db.execute("UPDATE gc_receipt SET status='UNLINKED' WHERE object_id=?", (object_id,))
            deleted.append(object_id)
        return deleted


class BoundedCoalescingQueue:
    def __init__(self, max_items, max_bytes):
        if max_items < 1 or max_bytes < 1:
            raise ValueError('INVALID_BOUND')
        self.max_items, self.max_bytes = max_items, max_bytes
        self.items, self.bytes = OrderedDict(), 0

    def offer(self, key: str, value: bytes):
        if not isinstance(value, bytes):
            raise TypeError('IMMUTABLE_BYTES_REQUIRED')
        size = len(key.encode()) + len(value)
        old_size = len(key.encode()) + len(self.items[key]) if key in self.items else 0
        if self.bytes - old_size + size > self.max_bytes or (key not in self.items and len(self.items) >= self.max_items):
            return False
        self.bytes += size - old_size
        self.items[key] = value
        return True

    def pop(self):
        key, value = self.items.popitem(last=False)
        self.bytes -= len(key.encode()) + len(value)
        return key, value


class BoundedCache:
    def __init__(self, max_items, max_bytes):
        self.queue = BoundedCoalescingQueue(max_items, max_bytes)
        self.pins = set()

    def put(self, key, value):
        if len(key.encode()) + len(value) > self.queue.max_bytes:
            return False
        while not self.queue.offer(key, value):
            victim = next((k for k in self.queue.items if k not in self.pins and k != key), None)
            if victim is None:
                return False
            v = self.queue.items.pop(victim)
            self.queue.bytes -= len(victim.encode()) + len(v)
        return True

    def get(self, key):
        value = self.queue.items[key]
        self.queue.items.move_to_end(key)
        return value

    def pin(self, key):
        if key not in self.queue.items:
            raise KeyError(key)
        self.pins.add(key)

    def unpin(self, key):
        self.pins.discard(key)
