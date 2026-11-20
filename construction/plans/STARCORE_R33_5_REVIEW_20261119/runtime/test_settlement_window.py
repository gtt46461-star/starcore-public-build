"""Full 100-period regression for early OOS labels hidden by later completed tasks.

Usage: python -B test_settlement_window.py PREPARED_TOOLS [OLD_CHECK_WINDOW]
"""
import importlib.util
import json
import sqlite3
import sys
import unittest
from pathlib import Path

import check_window

TOOLS = Path(sys.argv.pop(1)).resolve()
OLD = Path(sys.argv.pop(1)).resolve() if len(sys.argv)>1 and sys.argv[1].endswith('.py') else None
sys.path[:0] = [str(TOOLS/'B02/enabled_window_r335'),str(TOOLS/'verification/mainline')]
import test_enabled_window as windows


class SettlementWindow(unittest.TestCase):
    def setUp(self):
        f = self.f = windows.EnabledCoverageTests()
        f.setUp()
        self.addCleanup(f.doCleanups)
        trace, fixture = windows.m.trace, windows.fixture
        # The original fixture supplies 80 of 100 periods. Add the remaining
        # exact canonical chains; no enabled period disappears from the calendar.
        with sqlite3.connect(f.database) as db:
            for i, entry in enumerate(f.cal['entries'],1):
                if i<=80:
                    continue
                issue, tm = entry['targetIssue'], f.t0+60000*i
                base, frozen = trace.calendar.issue_offset(issue,-1), tm-56000
                hist, action, snap = [trace.digest(x,issue) for x in ('hist','act','snap')]
                draw = trace.digest('R815_DRAW_V2',issue,*range(1,11))
                signature = fixture.envelope(issue,'SHADOW',frozen-1)
                contract, decision = trace.digest('contract',issue),trace.digest('decision',issue)
                canon = trace.digest('R36_CANONICAL',issue,base,hist,action,snap,trace.digest('a'),signature)
                fixture.insert(db,'v57_k3_champion_freeze',[issue,trace.GAME,'bundle',1,1,2,3,1,trace.digest('a')])
                provenance = trace.digest('R36_OOS_PROVENANCE',canon,snap,action,hist,draw,tm+300)
                label = trace.digest(provenance,1,1)
                terminal = trace.digest('R36_SETTLEMENT',draw,label,'','COMPLETED')
                fixture.insert(db,trace.REQUEST,[issue,base,'CANONICAL_COMMITTED',frozen-100,tm,hist,canon])
                fixture.insert(db,trace.FREEZE,[issue,base,trace.GAME,base,1,1,2,3,'SHADOW','SHADOW',0.4,'bundle',contract,signature,hist,action,snap,canon,tm,frozen,1,1,1,1,label])
                fixture.insert(db,trace.LABEL,[issue,base,trace.GAME,1,1,2,3,0.4,1,1,1,1,frozen,tm,tm+400,0.36,provenance,label])
                fixture.insert(db,trace.TASK,[issue,issue,'COMPLETED',draw,terminal,tm+450])
                fixture.insert(db,'v51_selected_action',[issue,trace.GAME,1,1,2,3,'SHADOW','SHADOW',0.4,'bundle',contract,signature,decision])
                fixture.insert(db,'v51_decision_authorization',[issue,trace.GAME,contract,'bundle',signature,decision])
        f.refresh_bundle()

    def evaluate(self, module=check_window):
        f=self.f
        return module.evaluate(TOOLS,f.bundle,f.root,f.start,f.end,windows.C,windows.A,'FIXTURE')

    def test_all_100_valid_settlements_pass(self):
        result=self.evaluate()
        self.assertEqual(result['outcome'],'PASS')
        self.assertEqual(result['settlementTimesValidated'],100)

    def extend_disabled_window(self, partial_stop=False):
        f,trace,fixture=self.f,windows.m.trace,windows.fixture
        f.cal=trace.calendar.make_calendar(f.anchor,f.t0,120,f.t0+1000,windows.C,windows.A,
                   trace.sha_file(f.schedule),window_start_ms=f.t0+2000)
        fixture.save(f.calendar,f.cal)
        with sqlite3.connect(f.database) as db:
            for i,entry in enumerate(f.cal['entries'],1):
                if i<=100:continue
                issue,tm=entry['targetIssue'],f.t0+60000*i
                draw=trace.digest('R815_DRAW_V2',issue,*range(1,11))
                fixture.insert(db,trace.DRAW,[issue,tm,tm+300,0,2,draw,*range(1,11)])
        f.start=f.capture('START',f.lines,f.t0+1500)
        f.lines.append(windows.event(2,f.t0+6000000+(1000 if partial_stop else 0),
                                     False,f.lines[-1].split('|')[-1]))
        f.end=f.capture('END',f.lines,f.t0+7201000)
        f.refresh_bundle()

    def test_100_enabled_plus_20_disabled_periods_pass(self):
        self.extend_disabled_window()
        result=self.evaluate()
        self.assertEqual(result['outcome'],'PASS')
        self.assertEqual((result['counts']['calendarPeriods'],result['counts']['expectedEnabledDue'],
                          result['counts']['explicitlyDisabled']),(120,100,20))
        self.assertEqual(result['settlementTimesValidated'],100)
        self.assertEqual(len(result['explicitlyDisabledTraceFailures']),20)
        self.assertEqual(result['enabledTraceFailures'],[])
        self.assertEqual(len(result['originalTraceFailures']),20)

    def test_partial_stop_keeps_unfrozen_enabled_period_failure(self):
        self.extend_disabled_window(partial_stop=True)
        result=self.evaluate()
        self.assertEqual(result['outcome'],'INCOMPLETE')
        self.assertEqual(result['counts']['expectedEnabledDue'],101)
        self.assertEqual(len(result['enabledTraceFailures']),1)

    def corrupt_label_clock(self, first_seen_offset=-2, settled_offset=-1):
        f, trace=self.f,windows.m.trace
        target=f.cal['entries'][0]['targetIssue']
        with sqlite3.connect(f.database) as db:
            db.row_factory=sqlite3.Row
            frozen=trace.single(db,trace.FREEZE,'target_issue',target)
            draw=trace.single(db,trace.DRAW,'issue',target)
            seen=draw['open_time']+first_seen_offset
            provenance=trace.digest('R36_OOS_PROVENANCE',frozen['record_hash'],frozen['snapshot_hash'],
                       frozen['action_space_hash'],frozen['history_hash'],draw['draw_hash_v2'],seen)
            label=trace.digest(provenance,1,1)
            terminal=trace.digest('R36_SETTLEMENT',draw['draw_hash_v2'],label,'','COMPLETED')
            db.execute('UPDATE '+trace.DRAW+' SET first_seen_at=? WHERE issue=?',(seen,target))
            db.execute('UPDATE '+trace.FREEZE+' SET settlement_hash=? WHERE target_issue=?',(label,target))
            db.execute('UPDATE '+trace.LABEL+' SET settled_at=?,provenance_hash=?,record_hash=? WHERE target_issue=?',
                       (draw['open_time']+settled_offset,provenance,label,target))
            db.execute('UPDATE '+trace.TASK+' SET settlement_hash=? WHERE target_issue=?',(terminal,target))
        f.refresh_bundle()

    def test_early_label_does_not_pass_via_late_task_completion(self):
        self.corrupt_label_clock()
        f=self.f
        # All original source/manifest/row hashes are recomputed; this isolates
        # the time omission, not a basic checksum corruption.
        baseline=windows.m.trace.verify(f.bundle,f.root,'FIXTURE',windows.C,windows.A)
        self.assertEqual(baseline['outcome'],'PASS')
        self.assertEqual(baseline['joined'],100)
        if OLD:
            spec=importlib.util.spec_from_file_location('old_r335_window',OLD)
            old=importlib.util.module_from_spec(spec);spec.loader.exec_module(old)
            self.assertEqual(self.evaluate(old)['outcome'],'PASS')
        with self.assertRaisesRegex(ValueError,'LABEL_BEFORE_OPEN_OR_OBSERVATION'):
            self.evaluate()

    def test_snapshot_sidecar_refused(self):
        Path(str(self.f.database)+'-wal').write_bytes(b'pending-data')
        with self.assertRaises((ValueError,sqlite3.Error)):
            self.evaluate()


if __name__=='__main__':
    unittest.main(verbosity=2)
