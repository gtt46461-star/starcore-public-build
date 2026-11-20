"""Reproduce the distinct short trace with a delayed last draw and authentic joined hashes."""
import json
import sqlite3
import sys
import unittest
from pathlib import Path

PLAN, TOOLS = [Path(sys.argv.pop(1)).resolve() for _ in range(2)]
sys.path[:0] = [str(PLAN/'verification/mainline'),str(Path(__file__).resolve().parents[1]/'research')]
import test_trace as fixture
import check_research


class ShortTrace(unittest.TestCase):
    def test_delayed_result_never_reopens_short_trace_deadline(self):
        f=fixture.TraceTests('test_real_with_receipt_and_tampering')
        f.setUp()
        self.addCleanup(f.doCleanups)
        f.ids.reverse()  # Turn the final opportunity into REAL inside this test DB.
        f.test_real_with_receipt_and_tampering()
        c,issue=fixture.c,f.ids[0]
        with sqlite3.connect(f.db) as db:
            db.row_factory=sqlite3.Row
            db.execute('UPDATE r974_exact_real_receipt SET trade_id=7')
            frozen=c.single(db,c.FREEZE,'target_issue',issue)
            task=c.single(db,c.TASK,'target_issue',issue)
            cutoff=frozen['deadline_at']
            opened, seen=cutoff+1000,cutoff+1100
            provenance=c.digest('R36_OOS_PROVENANCE',frozen['record_hash'],frozen['snapshot_hash'],
                frozen['action_space_hash'],frozen['history_hash'],task['draw_hash'],seen)
            label=c.digest(provenance,1,1)
            real=c.digest('R36_REAL',7,task['draw_hash'],1,1)
            terminal=c.digest('R36_SETTLEMENT',task['draw_hash'],label,real,'COMPLETED')
            db.execute('UPDATE '+c.DRAW+' SET open_time=?,first_seen_at=? WHERE issue=?',(opened,seen,issue))
            db.execute('UPDATE '+c.FREEZE+' SET settlement_hash=? WHERE target_issue=?',(label,issue))
            db.execute('UPDATE '+c.LABEL+' SET draw_open_time=?,settled_at=?,provenance_hash=?,record_hash=? WHERE target_issue=?',
                       (opened,seen+100,provenance,label,issue))
            db.execute('UPDATE '+c.TASK+' SET completed_at=?,settlement_hash=? WHERE target_issue=?',(seen+200,terminal,issue))
        def bundle():
            path=f.bundle()
            data=json.loads(path.read_bytes());data['scope']='INTEGRATED'
            fixture.save(path,data)
            return path
        for table in ('research_exact_probability_f05','r974_exact_real_receipt'):
            with self.subTest(table=table):
                for committed in (cutoff-1,cutoff):
                    with sqlite3.connect(f.db) as db:
                        db.execute('UPDATE '+table+' SET committed_at=?',(committed,))
                    b=bundle()
                    self.assertEqual(c.verify(b,f.root,'INTEGRATED',fixture.C,fixture.A)['outcome'],'PASS')
                    if committed<cutoff:
                        self.assertEqual(check_research.trace(PLAN,b,f.root,fixture.C,fixture.A,TOOLS)['checked'],2)
                    else:
                        with self.assertRaisesRegex(ValueError,'BASE_TRACE_INCOMPLETE'):
                            check_research.trace(PLAN,b,f.root,fixture.C,fixture.A,TOOLS)
                with sqlite3.connect(f.db) as db:
                    db.execute('UPDATE '+table+' SET committed_at=?',(cutoff-1,))
        with self.assertRaisesRegex(ValueError,'REVIEWED_FIXED_DEADLINE_CHECKER_REQUIRED'):
            check_research.trace(PLAN,bundle(),f.root,fixture.C,fixture.A,PLAN)


if __name__ == '__main__':
    unittest.main(verbosity=2)
