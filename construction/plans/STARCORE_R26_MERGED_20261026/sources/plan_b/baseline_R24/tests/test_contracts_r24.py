import sys,unittest,tempfile,sqlite3
from pathlib import Path
sys.path.insert(0,str(Path(__file__).resolve().parents[1]/'code'))
from contracts_r24 import *
class ContractsTest(unittest.TestCase):
    def receipt(self,**kw):
        d=dict(spec_hash='s',train_through=10,frozen_target=11,model_hash='m',evidence_scope='PROSPECTIVE_FROZEN',causal=True,n=500,hits=190,opportunities=3000,max_loss=5,fresh_corrected_lower=.31);d.update(kw);return Receipt(**d)
    def test_exact_k3(self):self.assertEqual(best_k3([[.1]*10 for _ in range(10)])[:2],(1,(1,2,3)))
    def test_nan_rejected(self):
        p=[[.1]*10 for _ in range(10)];p[0][0]=float('nan')
        with self.assertRaises(ValueError):best_k3(p)
    def test_probability_not_normalized(self):
        with self.assertRaises(ValueError):best_k3([[.11]*10 for _ in range(10)])
    def test_history_gap_rejected(self):
        rows=[{'issue':i,'numbers':list(range(1,11))} for i in ['202610250485','202610250598']]
        with self.assertRaises(ValueError):history_window(rows,'202610250599',2)
    def test_next_day_contiguous(self):
        r=[{'issue':'202610241440','numbers':list(range(1,11))}];self.assertEqual(len(history_window(r,'202610250001',1)),1)
    def test_future_ignored(self):
        r=[{'issue':'202610250001','numbers':list(range(1,11))},{'issue':'202610250003','numbers':list(range(10,0,-1))}];self.assertEqual(history_window(r,'202610250002',1)[0]['issue'],'202610250001')
    def test_atom_future(self):
        with self.assertRaises(ValueError):AtomSpec('a','HISTORY','VECTOR',32,'v',0).validate()
    def test_atom_typed(self):AtomSpec('a','HISTORY','VECTOR',5000,'v').validate()
    def test_no_history_as_live(self):self.assertEqual(self.receipt(evidence_scope='HISTORY').qualification(),'RESEARCH_ONLY')
    def test_information_not_failure(self):self.assertEqual(self.receipt(n=20,hits=10).qualification(),'INSUFFICIENT_EVIDENCE')
    def test_coverage_not_forced(self):self.assertEqual(self.receipt(opportunities=5000).qualification(),'COVERAGE_NOT_MET')
    def test_bad_streak(self):self.assertEqual(self.receipt(max_loss=7).qualification(),'STABILITY_NOT_MET')
    def test_no_auto_real(self):self.assertEqual(self.receipt().qualification(),'ELIGIBLE_FOR_AUTHORITY_REVIEW_NOT_AUTO_REAL')
    def test_no_op_not_generation(self):self.assertEqual(generation_credit('a','a',True,self.receipt(),.01),'NO_OP')
    def test_parameter_variant(self):self.assertEqual(generation_credit('a','b',False,self.receipt(),.01),'PARAMETER_VARIANT')
    def test_unproven_generation(self):self.assertEqual(generation_credit('a','b',True,self.receipt(),-.01),'UNPROVEN_IMPROVEMENT')
    def test_verified_generation(self):self.assertEqual(generation_credit('a','b',True,self.receipt(),.01),'VERIFIED_GENERATION')
    def test_future_checkpoint(self):
        with self.assertRaises(ValueError):self.receipt(train_through=11).validate()
    def test_duplicate_settlement(self):
        l=EvidenceLedger();l.freeze('202610250001',1,[1,2,3],1,2,'m');self.assertTrue(l.settle('202610250001',list(range(1,11)),2));self.assertFalse(l.settle('202610250001',list(range(1,11)),3));self.assertEqual(l.db.execute('SELECT count(*) FROM feedback').fetchone()[0],1);l.close()
    def test_missing_freeze(self):
        l=EvidenceLedger()
        with self.assertRaises(ValueError):l.settle('202610250001',list(range(1,11)),3)
        l.close()
    def test_late_freeze(self):
        l=EvidenceLedger()
        with self.assertRaises(ValueError):l.freeze('202610250001',1,[1,2,3],2,2,'m')
        l.close()
    def test_one_real_per_issue(self):
        l=EvidenceLedger();l.freeze('202610250001',1,[1,2,3],1,2,'m',True)
        with self.assertRaises(sqlite3.IntegrityError):l.freeze('202610250001',2,[1,2,3],1,2,'m',True)
        l.close()
    def test_skip_no_reset(self):
        l=EvidenceLedger()
        for i in [1,3]:l.freeze(f'20261025{i:04}',1,[8,9,10],1,2,'m',True);l.settle(f'20261025{i:04}',list(range(1,11)),3)
        self.assertEqual(l.losing_streak(),(2,2));l.close()
    def test_cas_and_recovery(self):
        with tempfile.TemporaryDirectory() as d:
            p=str(Path(d)/'e.db');l=EvidenceLedger(p);l.checkpoint('o',0,'202610250001','m');l.close();l=EvidenceLedger(p)
            with self.assertRaises(ValueError):l.checkpoint('o',0,'202610250002','n')
            self.assertEqual(l.checkpoint('o',1,'202610250002','n'),2);l.close()
    def test_conflicting_full_draw_same_hit(self):
        l=EvidenceLedger();l.freeze('202610250001',1,[1,2,3],1,2,'m');l.settle('202610250001',list(range(1,11)),2)
        altered=[1,3,2,4,5,6,7,8,9,10]
        with self.assertRaises(ValueError):l.settle('202610250001',altered,3)
        self.assertEqual(l.db.execute('SELECT count(*) FROM feedback').fetchone()[0],1);l.close()
    def test_invalid_lower_bound_and_loss(self):
        for value in [float('nan'),float('inf'),-.01,1.01]:
            with self.subTest(value=value):
                with self.assertRaises(ValueError):self.receipt(fresh_corrected_lower=value).qualification()
        with self.assertRaises(ValueError):self.receipt(max_loss=-1).validate()
    def test_nan_improvement_not_verified(self):
        self.assertEqual(generation_credit('a','b',True,self.receipt(),float('nan')),'UNPROVEN_IMPROVEMENT')
if __name__=='__main__':unittest.main()
