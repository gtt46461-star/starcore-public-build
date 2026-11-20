"""Use a released plan's own fixtures to reproduce accepted counterexamples."""
import hashlib
import json
from pathlib import Path
import sqlite3
import sys
import unittest

import check_research as gate

PLAN = Path(sys.argv.pop(1)).resolve()
sys.path.insert(0, str(PLAN/'B07/verify'))
sys.path.insert(0, str(PLAN/'verification/mainline'))
import check_improvement as oracle
import test_improvement as improvement
sys.path.insert(0, str(PLAN/'B07/replay_r335'))
import test_case_loop_r335 as real_cases
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
            gate.assess_followup(oracle, f.p, chain)

    def test_followup_accepts_observed_postwork_rows(self):
        f, chain, after = self.experiment()
        after['observedAt'] = 190
        chain['nextCase'] = f.put('next', after)
        f.put('chain', chain)
        self.assertEqual(gate.assess_followup(oracle, f.p, chain)['effect'],
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
            gate.assess_followup(oracle, f.p, chain)

    def test_no_report_or_git_originals(self):
        f, chain, after = self.experiment()
        after['observedAt'] = 190
        chain['nextCase'] = f.put('next', after)
        f.put('chain', chain)
        self.assertEqual(gate.assess_followup(oracle, f.p, chain)['effect'], 'MEASURED_IMPROVEMENT_IN_THIS_TEST')
        with self.assertRaisesRegex(ValueError, 'ORIGINAL_REPORTS_AND_REPAIR_WORK_REQUIRED'):
            gate.followup(PLAN, f.p/'chain.json', f.p)

    def real_link(self):
        c = real_cases.CaseLoopTest()
        c.setUp()
        self.addCleanup(c.tearDown)
        first, _ = real_cases.M.case_from_index(c.root/'case1', c.ck)
        f = improvement.ReportPrescription()
        f.setUp()
        self.addCleanup(f.doCleanups)
        f.p = c.root
        f.key.update(needKey=first['needKey'], commit=c.after)
        f.spec.update(f.key, registeredAt=2100)
        f.b['contract'] = f.put('spec', f.spec)
        f.catalog([True, True])
        f.run_receipt()
        f.experiment([1] * 8)
        for row in f.raw['rows']:
            for clock in ('baseAt', 'challengerAt', 'drawAt'):
                row[clock] += 2000
        f.b['experiment'] = f.put('exp', f.raw)
        chain, after = f.followup()
        actual = dict(firstCase=f.put('original-first', dict(
                caseKey=c.ck, needKey=first['needKey'], commit=c.before, observedAt=1000,
                ownerGate='B07.4', sourceFile='owner.java', method='run')))
        work = json.loads((f.p/'work.json').read_bytes())
        work.update(caseKey=c.ck, beforeCommit=c.before, afterCommit=c.after, appliedAt=2000,
                    parentCaseSha=actual['firstCase']['sha256'], ownerGate='B07.4', sourceFile='owner.java', method='run')
        for receipt in c.work['verificationReferences']:
            role = receipt['role']
            work['gateRefs'][role] = f.put('checked-'+role, dict(gate='B07.4', subKind=role,
                                                                 commit=c.after, rawRef=receipt))
        actual['work'] = f.put('post-work', work)
        after.update(caseKey=c.ck, commit=c.after, workSha=actual['work']['sha256'],
                     parentCaseSha=actual['firstCase']['sha256'], observedAt=3000)
        actual['nextCase'] = f.put('post-case', after)
        actual['productionEvidence'] = {'repairWork': f.put('repair-work', c.work)}
        for idx, role in ((1, 'firstReport'), (2, 'nextReport')):
            name = 'report'+str(idx)+'.json'
            actual['productionEvidence'][role] = dict(path=name, sha256=hashlib.sha256((f.p/name).read_bytes()).hexdigest())
        f.put('chain', actual)
        return c, f, actual

    def test_joined_originals_and_git(self):
        c, f, _ = self.real_link()
        result = gate.followup(PLAN, f.p/'chain.json', c.repo)
        self.assertEqual(result['effect'], 'MEASURED_IMPROVEMENT_IN_THIS_TEST')
        self.assertEqual(result['reportWork']['actualChangedPaths'], ['owner.java'])
        self.assertEqual(result['reportWork']['afterSourceCommit'], c.after)
        self.assertFalse(result['realAuthorized'])

    def test_wrong_actual_git_file_even_after_rehash(self):
        c, f, chain = self.real_link()
        c.work['changes'][0]['afterSha256'] = '0'*64
        chain['productionEvidence']['repairWork'] = f.put('repair-work', c.work)
        f.put('chain', chain)
        with self.assertRaisesRegex(ValueError, 'WORK_SOURCE_BYTES_MISMATCH'):
            gate.followup(PLAN, f.p/'chain.json', c.repo)

    def test_different_raw_work_receipts(self):
        c, f, chain = self.real_link()
        receipt = c.work['verificationReferences'][0]
        payload = json.loads((f.p/receipt['path']).read_bytes())
        payload['note'] = 'different valid observation'
        c.work['verificationReferences'][0] = dict(role=receipt['role'], **f.put('different-observation', payload))
        chain['productionEvidence']['repairWork'] = f.put('repair-work', c.work)
        f.put('chain', chain)
        with self.assertRaisesRegex(ValueError, 'REPAIR_PROOF_MISMATCH'):
            gate.followup(PLAN, f.p/'chain.json', c.repo)

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
