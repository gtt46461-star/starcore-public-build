"""Behavior tests, deliberately scoped to reference contracts, never APK proof."""
from __future__ import annotations

import json
import os
from pathlib import Path
import sqlite3
import subprocess
import sys
import tempfile
import threading
import time
import unittest

from contracts import (SCOPE, BoundedCache, BoundedCoalescingQueue, BudgetRefused,
                       BusyWait, Clock, ConflictingEvent, DependencyWait,
                       ForecastStore, ObjectBudget, StaleFence, TaskStore,
                       connect, install, is_busy, write_tx)


class StoreTest(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.path = Path(self.tmp.name) / 'reference.db'
        self.db = connect(self.path)
        install(self.db)

    def tearDown(self):
        self.db.close()
        self.tmp.cleanup()

    def task(self):
        store = TaskStore(self.db)
        store.register('learn')
        return store

    def forecast_args(self):
        return dict(stream='LIVE', issue='202611040010', ordinal=100,
                    model_hash='fixed-model-hash', feature_max_ordinal=99,
                    position=0, numbers=[1, 2, 3], input_hash='verified-input-hash',
                    clock_before=Clock('boot-a', 1000), deadline_elapsed_ms=5000,
                    clock_after_commit=lambda: Clock('boot-a', 1100))

    def valid_forecast(self):
        store = ForecastStore(self.db)
        identity, state = store.freeze(**self.forecast_args())
        self.assertEqual('PREDRAW', state)
        store.reveal('LIVE', '202611040010', 100, list(range(1, 11)))
        return store, identity

    def budget(self, cap=1000):
        store = ObjectBudget(self.db, cap, 100)
        store.configure_pool('models', cap)
        return store

    def add_object(self, store, oid, size=40, **kwargs):
        token = 'reserve:' + oid
        store.reserve(token, 'models', size, 100000, 'TEST_VERIFIED_BODY')
        store.publish(token, oid, size, **kwargs)

    def test_two_connections_busy_does_not_count_defect(self):
        store = self.task()
        other = connect(self.path)
        other.execute('BEGIN IMMEDIATE')
        try:
            with self.assertRaises(BusyWait):
                store.claim('learn', 'owner-a', Clock('boot-a', 1000))
            self.assertEqual(0, self.db.execute('SELECT error_count FROM task').fetchone()[0])
        finally:
            other.rollback()
            other.close()
        claim = store.claim('learn', 'owner-a', Clock('boot-a', 1001))
        self.assertEqual(1, claim.fence)

    def test_concurrent_claim_only_one_owner(self):
        self.task()
        barrier = threading.Barrier(2)
        result, lock = [], threading.Lock()
        def compete(owner):
            db = connect(self.path)
            try:
                barrier.wait(timeout=5)
                try:
                    claim = TaskStore(db).claim('learn', owner, Clock('boot-a', 1000))
                    outcome = ('CLAIM', claim.owner, claim.fence)
                except (BusyWait, DependencyWait) as exc:
                    outcome = ('WAIT', type(exc).__name__)
                with lock:
                    result.append(outcome)
            finally:
                db.close()
        workers = [threading.Thread(target=compete, args=('a',)), threading.Thread(target=compete, args=('b',))]
        for t in workers: t.start()
        for t in workers: t.join(10)
        self.assertTrue(all(not t.is_alive() for t in workers))
        self.assertEqual(2, len(result))
        self.assertEqual(1, sum(x[0] == 'CLAIM' for x in result), result)
        self.assertEqual(0, self.db.execute('SELECT error_count FROM task').fetchone()[0])

    def test_expired_fence_cannot_publish_or_reset_new_owner(self):
        store = self.task()
        old = store.claim('learn', 'old', Clock('boot-a', 0), 50)
        new = store.claim('learn', 'new', Clock('boot-a', 50), 500)
        self.assertGreater(new.fence, old.fence)
        with self.assertRaises(StaleFence):
            store.commit_progress(old, Clock('boot-a', 60), 'e1', b'bad', 1)
        with self.assertRaises(StaleFence):
            store.failure(old, Clock('boot-a', 60), RuntimeError('old worker failure'))
        self.assertEqual('new', self.db.execute('SELECT owner FROM task').fetchone()[0])
        self.assertEqual('COMMITTED', store.commit_progress(new, Clock('boot-a', 70), 'e1', b'good', 1))

    def test_commit_replay_idempotent_but_conflict_rejected(self):
        store = self.task()
        claim = store.claim('learn', 'worker', Clock('boot-a', 0))
        self.assertEqual('COMMITTED', store.commit_progress(claim, Clock('boot-a', 10), 'event', b'x', 3))
        self.assertEqual('ALREADY_COMMITTED', store.commit_progress(claim, Clock('boot-a', 10000), 'event', b'x', 3))
        with self.assertRaises(ConflictingEvent):
            store.commit_progress(claim, Clock('boot-a', 10000), 'event', b'y', 3)
        self.assertEqual(1, self.db.execute('SELECT count(*) FROM task_commit').fetchone()[0])

    def test_cancel_dependency_busy_do_not_increment_and_cancel_propagates(self):
        store = self.task()
        for i, exc in enumerate((BusyWait('load'), DependencyWait('student missing'), InterruptedError('cancel'))):
            claim = store.claim('learn', f'w{i}', Clock('boot-a', i * 100))
            if isinstance(exc, InterruptedError):
                with self.assertRaises(InterruptedError):
                    store.failure(claim, Clock('boot-a', i * 100 + 1), exc)
            else:
                store.failure(claim, Clock('boot-a', i * 100 + 1), exc)
        self.assertEqual(0, self.db.execute('SELECT error_count FROM task').fetchone()[0])
        self.assertEqual(0, self.db.execute('SELECT quarantine_rounds FROM task').fetchone()[0])

    def test_sql_syntax_error_not_misclassified_as_busy(self):
        try:
            self.db.execute('this is not valid SQL')
        except sqlite3.OperationalError as exc:
            self.assertFalse(is_busy(exc))
        else:
            self.fail('Invalid SQL did not fail')

    def test_cancellation_still_propagates_when_receipt_writer_is_busy(self):
        store = self.task()
        claim = store.claim('learn', 'worker', Clock('boot-a', 0))
        other = connect(self.path)
        other.execute('BEGIN IMMEDIATE')
        try:
            with self.assertRaises(InterruptedError):
                store.failure(claim, Clock('boot-a', 10), InterruptedError('user stop'))
        finally:
            other.rollback(); other.close()
        self.assertEqual(0, self.db.execute('SELECT error_count FROM task').fetchone()[0])

    def test_user_stop_revokes_inflight_worker_fence(self):
        store = self.task()
        claim = store.claim('learn', 'worker', Clock('boot-a', 0))
        store.stop('learn')
        with self.assertRaises(StaleFence):
            store.commit_progress(claim, Clock('boot-a', 1), 'late', b'x', 1)
        with self.assertRaisesRegex(DependencyWait, 'STOPPED'):
            store.claim('learn', 'new-worker', Clock('boot-a', 10000))

    def test_circuit_half_open_single_probe_then_bounded_needs_fix(self):
        store = self.task()
        for i in range(3):
            claim = store.claim('learn', 'w', Clock('boot-a', i * 10))
            state = store.failure(claim, Clock('boot-a', i * 10 + 1), ValueError('repeat defect'))
        self.assertEqual('QUARANTINED', state)
        with self.assertRaises(DependencyWait):
            store.claim('learn', 'w', Clock('boot-a', 300))
        next_time = self.db.execute('SELECT quarantine_until_ms FROM task').fetchone()[0]
        claim = store.claim('learn', 'probe-1', Clock('boot-a', next_time))
        with self.assertRaises(DependencyWait):
            store.claim('learn', 'probe-2', Clock('boot-a', next_time))
        self.assertEqual('QUARANTINED', store.failure(claim, Clock('boot-a', next_time + 1), ValueError('still broken')))
        next_time = self.db.execute('SELECT quarantine_until_ms FROM task').fetchone()[0]
        claim = store.claim('learn', 'probe-3', Clock('boot-a', next_time))
        self.assertEqual('NEEDS_FIX', store.failure(claim, Clock('boot-a', next_time + 1), ValueError('still broken')))
        with self.assertRaisesRegex(DependencyWait, 'NEEDS_FIX'):
            store.claim('learn', 'probe-4', Clock('boot-a', next_time + 1_000_000))

    def test_monotonic_lease_independent_of_wall_clock_and_boot_reset(self):
        store = self.task()
        old = store.claim('learn', 'old', Clock('boot-a', 99_000), 1000)
        # Wall clock/DST are not accepted by the API. Same elapsed time cannot
        # expire a lease even if wall time would have jumped an hour.
        with self.assertRaises(DependencyWait):
            store.claim('learn', 'same-boot', Clock('boot-a', 99_100))
        new = store.claim('learn', 'new-boot', Clock('boot-b', 100), 1000)
        with self.assertRaises(StaleFence):
            store.commit_progress(old, Clock('boot-a', 99_200), 'old', b'x', 1)
        self.assertEqual('COMMITTED', store.commit_progress(new, Clock('boot-b', 150), 'new', b'y', 1))

    def test_abrupt_process_exit_rolls_back_owner_and_artifact_transaction(self):
        self.task()
        code = '''import os,sys
from contracts import connect
d=connect(sys.argv[1]);d.execute('BEGIN IMMEDIATE')
d.execute("UPDATE task SET owner='dead-process',cursor=77 WHERE task_id='learn'")
d.execute("INSERT INTO task_commit VALUES('learn','k','h','dead-process',0,77)")
os._exit(73)
'''
        result = subprocess.run([sys.executable, '-c', code, str(self.path)], cwd=Path(__file__).parent, timeout=10)
        self.assertEqual(73, result.returncode)
        self.assertEqual(0, self.db.execute('SELECT count(*) FROM task_commit').fetchone()[0])
        row = self.db.execute('SELECT owner,cursor FROM task').fetchone()
        self.assertEqual((None, 0), tuple(row))
        self.assertEqual('ok', self.db.execute('PRAGMA integrity_check').fetchone()[0])

    def test_pit_and_fixed_k3_validation(self):
        store = ForecastStore(self.db)
        for change in ({'feature_max_ordinal': 100}, {'feature_max_ordinal': 101},
                       {'numbers': [1, 2, 2]}, {'numbers': [1, 2, 3, 4]},
                       {'numbers': [1, 2, 11]}):
            args = self.forecast_args(); args.update(change)
            with self.assertRaises(ValueError): store.freeze(**args)
        self.assertEqual(0, self.db.execute('SELECT count(*) FROM forecast').fetchone()[0])

    def test_frozen_forecast_cannot_be_replaced_even_before_deadline(self):
        store = ForecastStore(self.db)
        args = self.forecast_args()
        identity, _ = store.freeze(**args)
        args['numbers'] = [4, 5, 6]
        with self.assertRaises(ConflictingEvent): store.freeze(**args)
        with self.assertRaises(sqlite3.IntegrityError):
            self.db.execute('UPDATE forecast SET position=2 WHERE forecast_id=?', (identity,))
        self.assertEqual('[1, 2, 3]', self.db.execute('SELECT numbers_json FROM forecast').fetchone()[0])

    def test_already_observed_outcome_cannot_create_predraw_even_with_early_clock(self):
        store = ForecastStore(self.db)
        store.reveal('LIVE', '202611040010', 100, list(range(1, 11)))
        with self.assertRaisesRegex(ValueError, 'OUTCOME_ALREADY_OBSERVED'):
            store.freeze(**self.forecast_args())
        self.assertEqual(0, self.db.execute('SELECT count(*) FROM forecast').fetchone()[0])

    def test_commit_crossing_deadline_is_excluded_not_backdated(self):
        store = ForecastStore(self.db)
        args = self.forecast_args(); args['clock_after_commit'] = lambda: Clock('boot-a', 5000)
        identity, state = store.freeze(**args)
        self.assertEqual('LATE_OR_UNKNOWN', state)
        store.reveal('LIVE', '202611040010', 100, list(range(1, 11)))
        with self.assertRaises(DependencyWait): store.settle(identity)
        self.assertEqual(0, self.db.execute('SELECT count(*) FROM settlement').fetchone()[0])

    def test_commit_receipt_cannot_mix_boot_clocks(self):
        store = ForecastStore(self.db)
        args = self.forecast_args(); args['clock_after_commit'] = lambda: Clock('boot-b', 1)
        _, state = store.freeze(**args)
        self.assertEqual('LATE_OR_UNKNOWN', state)

    def test_crash_after_prediction_commit_leaves_unknown_timing(self):
        code = '''import os,sys
from contracts import connect,ForecastStore,Clock
d=connect(sys.argv[1]);s=ForecastStore(d)
s.freeze(stream='LIVE',issue='202611040010',ordinal=100,model_hash='fixed-model-hash',feature_max_ordinal=99,
 position=0,numbers=[1,2,3],input_hash='verified-input-hash',clock_before=Clock('boot-a',1000),
 deadline_elapsed_ms=5000,clock_after_commit=lambda:Clock('boot-a',1100),after_commit_hook=lambda:os._exit(74))
'''
        result = subprocess.run([sys.executable, '-c', code, str(self.path)], cwd=Path(__file__).parent, timeout=10)
        self.assertEqual(74, result.returncode)
        self.assertEqual(1, self.db.execute('SELECT count(*) FROM forecast').fetchone()[0])
        self.assertEqual(0, self.db.execute('SELECT count(*) FROM forecast_commit_receipt').fetchone()[0])
        store = ForecastStore(self.db)
        identity, state = store.freeze(**self.forecast_args())
        self.assertEqual('EXISTING_IMMUTABLE_FORECAST', state)
        with self.assertRaises(DependencyWait): store.settle(identity)

    def test_settlement_outbox_rollback_atomic_and_duplicate_safe(self):
        store, identity = self.valid_forecast()
        def fail(): raise RuntimeError('power loss equivalent before commit')
        with self.assertRaises(RuntimeError): store.settle(identity, fail)
        self.assertEqual(0, self.db.execute('SELECT count(*) FROM settlement').fetchone()[0])
        self.assertEqual(0, self.db.execute('SELECT count(*) FROM feedback_outbox').fetchone()[0])
        self.assertEqual('SETTLED', store.settle(identity))
        for _ in range(20): self.assertEqual('ALREADY_SETTLED', store.settle(identity))
        self.assertEqual((1, 1), (self.db.execute('SELECT count(*) FROM settlement').fetchone()[0],
                                self.db.execute('SELECT count(*) FROM feedback_outbox').fetchone()[0]))

    def test_feedback_applied_once_with_model_checkpoint_transaction(self):
        store, identity = self.valid_forecast(); store.settle(identity)
        self.db.execute('CREATE TABLE test_model(id INTEGER PRIMARY KEY,updates INTEGER)')
        self.db.execute('INSERT INTO test_model VALUES(1,0)')
        def apply(db, event): db.execute('UPDATE test_model SET updates=updates+1 WHERE id=1')
        for _ in range(10): store.apply_feedback('model-1', 'settle:' + identity, apply)
        self.assertEqual(1, self.db.execute('SELECT updates FROM test_model').fetchone()[0])

    def test_reveal_revision_and_calendar_mismatch_not_silently_overwritten(self):
        store, identity = self.valid_forecast()
        with self.assertRaises(ConflictingEvent):
            store.reveal('LIVE', '202611040010', 100, list(range(10, 0, -1)))
        with self.assertRaises(ConflictingEvent):
            store.reveal('LIVE', '202611040010', 101, list(range(1, 11)))
        self.assertEqual('SETTLED', store.settle(identity))

    def test_budget_reserves_peak_not_only_final_size(self):
        store = self.budget(200)
        self.add_object(store, 'old', 100, root_id='model:current')
        store.reserve('new', 'models', 100, 10000, 'checkpoint double residency')
        with self.assertRaises(BudgetRefused): store.reserve('report', 'models', 1, 10000, 'extra')
        self.assertEqual(200, store.used())
        store.publish('new', 'new', 80, root_id='model:current')
        self.assertEqual(180, store.used())
        self.assertEqual(1, store.mark_unreachable())
        store.sweep(lambda _: None)
        self.assertEqual(80, store.used())

    def test_low_disk_refuses_before_allocating_or_evicting_protected(self):
        store = self.budget()
        self.add_object(store, 'certificate', 50, root_id='cert:active', kind='CERTIFICATE')
        with self.assertRaisesRegex(BudgetRefused, 'LOW_PHYSICAL_DISK'):
            store.reserve('too-big', 'models', 80, 150, 'export snapshot')
        self.assertEqual(0, self.db.execute('SELECT count(*) FROM budget_reservation').fetchone()[0])
        self.assertEqual(0, store.mark_unreachable())
        self.assertEqual(50, store.used())

    def test_reference_closure_protects_pins_certificates_unsettled_and_cycles(self):
        store = self.budget()
        for oid in ['data', 'model', 'certificate', 'forecast', 'pin', 'orphan-a', 'orphan-b']:
            self.add_object(store, oid)
        with write_tx(self.db):
            self.db.executemany('INSERT INTO object_edge VALUES(?,?)',
                                [('certificate','model'),('model','data'),('forecast','data'),
                                 ('orphan-a','orphan-b'),('orphan-b','orphan-a')])
        store.root('cert', 'certificate', 'CERTIFICATE')
        store.root('predraw', 'forecast', 'UNSETTLED')
        store.root('inflight', 'pin', 'PIN')
        self.assertEqual(2, store.mark_unreachable())
        deleted = store.sweep(lambda _: None)
        self.assertEqual(['orphan-a', 'orphan-b'], deleted)
        self.assertEqual(5, self.db.execute('SELECT count(*) FROM object_catalog').fetchone()[0])
        self.assertEqual([], self.db.execute('PRAGMA foreign_key_check').fetchall())

    def test_unlink_failure_keeps_budget_and_retry_is_idempotent(self):
        store = self.budget()
        self.add_object(store, 'orphan', 100)
        store.mark_unreachable()
        def fail(_): raise OSError('filesystem refused')
        self.assertEqual([], store.sweep(fail))
        self.assertEqual(100, store.used())
        self.assertEqual(['orphan'], store.sweep(lambda _: None))
        self.assertEqual([], store.sweep(lambda _: None))
        self.assertEqual(0, store.used())

    def test_gc_mark_prevents_new_references_to_pending_delete(self):
        store = self.budget()
        self.add_object(store, 'old', 20)
        store.mark_unreachable()
        with self.assertRaises(sqlite3.IntegrityError): store.root('late-pin', 'old', 'PIN')
        with self.assertRaises(ConflictingEvent):
            store.reserve('reuse', 'models', 20, 10000, 'reuse')
            store.publish('reuse', 'old', 20)

    def test_checkpoint_failure_preserves_old_root_and_reservation(self):
        store = self.budget()
        self.add_object(store, 'old', 40, root_id='model:current')
        store.reserve('new-reserve', 'models', 40, 10000, 'checkpoint')
        def fail(): raise RuntimeError('crash before root swap')
        with self.assertRaises(RuntimeError):
            store.publish('new-reserve', 'new', 40, root_id='model:current', before_root_swap=fail)
        self.assertEqual('old', self.db.execute('SELECT object_id FROM object_root').fetchone()[0])
        self.assertEqual(80, store.used())
        self.assertEqual(1, self.db.execute('SELECT count(*) FROM budget_reservation').fetchone()[0])
        store.publish('new-reserve', 'new', 40, root_id='model:current')
        self.assertEqual('new', self.db.execute('SELECT object_id FROM object_root').fetchone()[0])

    def test_repeated_checkpoint_evolution_reclaims_old_objects(self):
        store = self.budget(128)
        peaks = []
        for i in range(200):
            self.add_object(store, f'checkpoint-{i}', 40, root_id='model:current')
            peaks.append(store.used())
            store.mark_unreachable(); store.sweep(lambda _: None)
            self.assertEqual(40, store.used())
        self.assertLessEqual(max(peaks), 80)
        self.assertEqual(1, self.db.execute('SELECT count(*) FROM object_catalog').fetchone()[0])

    def test_move_to_undeletable_keep_reproduces_unbounded_logical_retention(self):
        self.db.executescript('''CREATE TABLE legacy_hot(id INTEGER PRIMARY KEY,payload BLOB);
            CREATE TABLE legacy_keep(id INTEGER PRIMARY KEY,payload BLOB);
            CREATE TRIGGER legacy_move BEFORE DELETE ON legacy_hot BEGIN
                INSERT OR IGNORE INTO legacy_keep VALUES(OLD.id,OLD.payload); END;
            CREATE TRIGGER legacy_keep_no_delete BEFORE DELETE ON legacy_keep BEGIN
                SELECT RAISE(ABORT,'protected'); END;''')
        totals = []
        for i in range(1, 201):
            self.db.execute('INSERT INTO legacy_hot VALUES(?,?)', (i, b'x' * 40))
            self.db.execute('DELETE FROM legacy_hot WHERE id<=?', (i - 10,))
            total = self.db.execute('SELECT (SELECT count(*) FROM legacy_hot)+(SELECT count(*) FROM legacy_keep)').fetchone()[0]
            totals.append(total)
        self.assertEqual(200, totals[-1])
        self.assertEqual(10, self.db.execute('SELECT count(*) FROM legacy_hot').fetchone()[0])
        self.assertEqual(190, self.db.execute('SELECT count(*) FROM legacy_keep').fetchone()[0])

    def test_bounded_queue_rejects_overflow_without_losing_existing_item(self):
        q = BoundedCoalescingQueue(2, 12)
        self.assertTrue(q.offer('a', b'111'))
        self.assertTrue(q.offer('b', b'222'))
        self.assertFalse(q.offer('c', b'3'))
        self.assertFalse(q.offer('a', b'0123456789012'))
        self.assertEqual(('a', b'111'), q.pop())
        self.assertEqual(4, q.bytes)

    def test_cache_lru_and_pins_are_bounded(self):
        cache = BoundedCache(2, 12)
        self.assertTrue(cache.put('a', b'111'))
        cache.pin('a')
        self.assertTrue(cache.put('b', b'222'))
        self.assertTrue(cache.put('c', b'333'))
        self.assertNotIn('b', cache.queue.items)
        self.assertEqual(b'111', cache.get('a'))
        cache.pin('c')
        self.assertFalse(cache.put('d', b'444'))
        self.assertLessEqual(cache.queue.bytes, 12)
        self.assertLessEqual(len(cache.queue.items), 2)


class JsonResult(unittest.TextTestResult):
    def __init__(self, *args, **kwargs):
        super().__init__(*args, **kwargs)
        self.records = []

    def addSuccess(self, test):
        super().addSuccess(test)
        self.records.append({'id': test.id(), 'status': 'PASS'})

    def addFailure(self, test, err):
        super().addFailure(test, err)
        self.records.append({'id': test.id(), 'status': 'FAIL', 'detail': self._exc_info_to_string(err, test)})

    def addError(self, test, err):
        super().addError(test, err)
        self.records.append({'id': test.id(), 'status': 'ERROR', 'detail': self._exc_info_to_string(err, test)})


if __name__ == '__main__':
    start = time.perf_counter()
    result = unittest.TextTestRunner(verbosity=2, resultclass=JsonResult).run(unittest.defaultTestLoader.loadTestsFromTestCase(StoreTest))
    report = {'scope': SCOPE, 'status': 'PASS' if result.wasSuccessful() else 'FAIL',
              'tests': result.testsRun, 'failures': len(result.failures), 'errors': len(result.errors),
              'python': sys.version.split()[0], 'sqlite': sqlite3.sqlite_version,
              'duration_seconds': round(time.perf_counter() - start, 3),
              'command': 'python3 reference_code/contracts/test_contracts.py',
              'android_integrated': False, 'apk_built': False, 'physical_device': 'NOT_RUN',
              'natural_quality': 'NOT_EVALUATED', 'results': result.records}
    Path(__file__).with_name('test_results.json').write_text(json.dumps(report, ensure_ascii=False, indent=2) + '\n')
    sys.exit(0 if result.wasSuccessful() else 1)
