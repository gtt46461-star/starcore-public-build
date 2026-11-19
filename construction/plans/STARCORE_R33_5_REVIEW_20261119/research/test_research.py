"""Use a released plan's own fixtures to reproduce accepted counterexamples."""
import json
from pathlib import Path
import sqlite3
import sys
import unittest

import check_research as gate

PLAN = Path(sys.argv.pop(1)).resolve()
sys.path.insert(0, str(PLAN/'B07/verify'))
sys.path.insert(0, str(PLAN/'verification/mainline'))
import test_improvement as improvement
import test_trace as traces


class AdditionalChecks(unittest.TestCase):
    def setUp(self):
        self.db = sqlite3.connect(':memory:')
        self.addCleanup(self.db.close)
        self.db.executescript("""
            CREATE TABLE su2_lineage(child_key,parent_key);
            CREATE TABLE su2_model_genome(genome_key,born_sequence);
            CREATE TABLE su2_live_decision(genome_key,sequence_ordinal,act,hit,
                                          frozen_at,target_open_time,settled_at);
            INSERT INTO su2_lineage VALUES('C','P');
            INSERT INTO su2_model_genome VALUES('C',100);
            INSERT INTO su2_live_decision VALUES
            ('C',100,1,1,5,10,11), ('P',100,1,0,5,10,11),
            ('C',101,0,NULL,15,20,NULL), ('P',101,1,1,15,20,22);
        """)

    def paired(self):
        return gate.paired(self.db,'C','P',103)

    def test_pair_denominator(self):
        r = self.paired()
        self.assertEqual((r['ordinals'], r['joined'], r['pending'], r['missingOneOrBothFreezes']),
                         (3, 1, 1, 1))
        self.assertEqual(r['meanOpportunityHitDifference'], 1)

    def test_pair_backward_settle(self):
        self.db.execute("UPDATE su2_live_decision SET settled_at=9 WHERE sequence_ordinal=100")
        with self.assertRaisesRegex(ValueError, 'INVALID_SETTLEMENT'):
            self.paired()

    def test_pair_zero_freeze(self):
        self.db.execute("UPDATE su2_live_decision SET frozen_at=0 WHERE genome_key='C'")
        with self.assertRaisesRegex(ValueError, 'INVALID_PREDRAW'):
            self.paired()

    def test_pair_duplicate(self):
        self.db.execute('INSERT INTO su2_live_decision SELECT * FROM su2_live_decision LIMIT 1')
        with self.assertRaisesRegex(ValueError, 'DUPLICATE'):
            self.paired()

    def experiment(self):
        f = improvement.ReportPrescription()
        f.setUp()
        self.addCleanup(f.doCleanups)
        f.experiment([1]*8)
        chain, after = f.followup()
        return f, chain, after

    def test_followup_rejects_future_rows(self):
        f, chain, _ = self.experiment()
        self.assertEqual(improvement.compare_followup(f.p, chain)['effect'],
                         'MEASURED_IMPROVEMENT_IN_THIS_TEST')
        f.put('chain', chain)
        with self.assertRaisesRegex(ValueError, 'FOLLOWUP_WINDOW'):
            gate.followup(PLAN, f.p/'chain.json')

    def test_followup_accepts_observed_postwork_rows(self):
        f, chain, after = self.experiment()
        after['observedAt'] = 190
        chain['nextCase'] = f.put('next', after)
        f.put('chain', chain)
        self.assertEqual(gate.followup(PLAN, f.p/'chain.json')['effect'],
                         'MEASURED_IMPROVEMENT_IN_THIS_TEST')

    def test_followup_rejects_recycled_prework_rows(self):
        f, chain, after = self.experiment()
        work = json.loads((f.p/'work.json').read_bytes())
        work['appliedAt'] = 181
        chain['work'] = f.put('work', work)
        after['workSha'] = chain['work']['sha256']
        after['observedAt'] = 190
        chain['nextCase'] = f.put('next', after)
        f.put('chain', chain)
        with self.assertRaisesRegex(ValueError, 'FOLLOWUP_WINDOW'):
            gate.followup(PLAN, f.p/'chain.json')

    def test_oos_open_time(self):
        f = traces.TraceTests()
        f.setUp()
        self.addCleanup(f.doCleanups)
        c = traces.c
        def bundle():
            path = f.bundle()
            data = json.loads(path.read_bytes())
            data['scope'] = 'INTEGRATED'
            traces.save(path, data)
            return path
        self.assertEqual(gate.trace(PLAN,bundle(),f.root,traces.C,traces.A)['checked'],2)
        target = f.ids[0]
        with sqlite3.connect(f.db) as db:
            db.row_factory = sqlite3.Row
            frozen = c.single(db,c.FREEZE,'target_issue',target)
            draw = c.single(db,c.DRAW,'issue',target)
            first = draw['open_time']-2
            prov = c.digest('R36_OOS_PROVENANCE',frozen['record_hash'],frozen['snapshot_hash'],
                            frozen['action_space_hash'],frozen['history_hash'],draw['draw_hash_v2'],first)
            label = c.digest(prov,1,1)
            terminal = c.digest('R36_SETTLEMENT',draw['draw_hash_v2'],label,'','COMPLETED')
            db.execute('UPDATE '+c.DRAW+' SET first_seen_at=? WHERE issue=?',(first,target))
            db.execute('UPDATE '+c.FREEZE+' SET settlement_hash=? WHERE target_issue=?',(label,target))
            db.execute('UPDATE '+c.LABEL+' SET settled_at=?,provenance_hash=?,record_hash=? WHERE target_issue=?',
                       (first+1,prov,label,target))
            db.execute('UPDATE '+c.TASK+' SET settlement_hash=? WHERE target_issue=?',(terminal,target))
        path = bundle()
        self.assertEqual(c.verify(path,f.root,'INTEGRATED',traces.C,traces.A)['outcome'],'PASS')
        with self.assertRaisesRegex(ValueError, 'LABEL_BEFORE_OPEN'):
            gate.trace(PLAN,path,f.root,traces.C,traces.A)


if __name__ == '__main__':
    unittest.main()
