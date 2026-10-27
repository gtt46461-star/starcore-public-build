import sys,copy,itertools,tempfile
from pathlib import Path
sys.path.insert(0,str(Path(__file__).resolve().parents[1]/'code'))
import unittest,numpy as np,torch
from torch.nn import functional as F
from mechanisms_r23 import *
from evidence_r23 import *
from capability_contract_r23 import *
torch.set_num_threads(1)
class TestMechanisms(unittest.TestCase):
    def setUp(self):
        torch.manual_seed(23); self.x=torch.stack([torch.stack([torch.randperm(10) for _ in range(16)]) for _ in range(3)])
        self.y=torch.stack([torch.randperm(10) for _ in range(3)])
    def test_all_families_shape_probability_and_gradient(self):
        for k in FAMILIES:
            with self.subTest(family=k):
                m=TemporalNet(k);s=m(self.x);self.assertEqual(tuple(s.shape),(3,10,10))
                F.cross_entropy(s.reshape(-1,10),self.y.reshape(-1)).backward()
                grads=[p.grad for p in m.parameters() if p.grad is not None]
                self.assertTrue(grads);self.assertTrue(all(torch.isfinite(g).all() for g in grads))
                self.assertGreater(sum(float(g.abs().sum()) for g in grads),0)
                self.assertTrue(torch.allclose(s.softmax(-1).sum(-1),torch.ones(3,10),atol=1e-6))
    def test_tcn_morphism_exact(self):
        p=TemporalNet('causal_tcn');p.eval();before=parameter_hash(p);c=grow_tcn(p);c.eval()
        self.assertTrue(torch.equal(p(self.x),c(self.x)));self.assertEqual(parameter_hash(p),before)
        self.assertGreater(sum(x.numel() for x in c.parameters()),sum(x.numel() for x in p.parameters()))
    def test_mutation_parent_is_immutable(self):
        p=TemporalNet('lstm');before=parameter_hash(p);c=mutate_weights(p,92)
        self.assertEqual(parameter_hash(p),before);self.assertNotEqual(parameter_hash(c),before)
    def test_checkpoint_restores_weights_optimizer_and_cutoff(self):
        m=TemporalNet('lstm');o=torch.optim.AdamW(m.parameters(),lr=.001)
        F.cross_entropy(m(self.x).reshape(-1,10),self.y.reshape(-1)).backward();o.step()
        with tempfile.TemporaryDirectory() as d:
            p=Path(d)/'state.pt';save_checkpoint(p,m,o,7,'r');c,co,t=load_checkpoint(p,'r',8)
            self.assertEqual(t,7);self.assertEqual(parameter_hash(m),parameter_hash(c));self.assertEqual(len(o.state),len(co.state))
            with self.assertRaises(ValueError):load_checkpoint(p,'r',7)
            with self.assertRaises(ValueError):load_checkpoint(p,'wrong',8)
    def test_distillation_changes_only_student(self):
        t=TemporalNet('lstm');s=TemporalNet('relation_graph');a=parameter_hash(t);b=parameter_hash(s)
        o=torch.optim.AdamW(s.parameters(),lr=.001);loss=distill_step(s,t,o,self.x,self.y)
        self.assertTrue(np.isfinite(loss));self.assertEqual(a,parameter_hash(t));self.assertNotEqual(b,parameter_hash(s))
    def test_k3_equal_to_exhaustive_1200(self):
        rng=np.random.default_rng(3)
        for _ in range(20):
            p=rng.dirichlet(np.ones(10),10);pos,nums,mass=coherent_k3(p)
            best=max((float(p[j,list(s)].sum()),-j,tuple(-x for x in s)) for j in range(10) for s in itertools.combinations(range(10),3))
            self.assertAlmostEqual(mass,best[0],places=14)
    def test_k3_rejects_invalid(self):
        for p in (np.ones((10,10)),np.ones((9,10))/10,np.full((10,10),np.nan)):
            with self.assertRaises(ValueError):coherent_k3(p)
    def test_causal_block_prefix(self):
        m=CausalBlock(4,2).eval();x=torch.randn(2,4,20);z=x.clone();z[:,:,10:]=torch.randn(2,4,10)*100
        self.assertTrue(torch.equal(m(x)[:,:,:10],m(z)[:,:,:10]))
class TestEvidence(unittest.TestCase):
    def test_skips_do_not_reset_streak(self):
        r=streak_metrics([1,0,1,0,1,1],[0,1,0,1,0,1]);self.assertEqual(r['max_losing_streak'],3)
    def test_alpha_lifetime_budget(self):
        self.assertLessEqual(sum(registration_alpha(j) for j in range(1,10000)),.05)
    def test_evalue_is_nonnegative_under_all_sequences(self):
        for seq in itertools.product((0,1),repeat=8):
            e=BernoulliEProcess()
            for y in seq:e.update(y)
            self.assertTrue(np.isfinite(e.log_e))
    def test_null_expected_e_at_fixed_time_is_one(self):
        expectation=0.0
        for seq in itertools.product((0,1),repeat=8):
            e=BernoulliEProcess()
            for y in seq:e.update(y)
            expectation+=.3**sum(seq)*.7**(8-sum(seq))*np.exp(e.log_e)
        self.assertAlmostEqual(expectation,1.0,places=12)
    def test_empty_evidence_not_quality(self):
        e=BernoulliEProcess();self.assertFalse(e.crossed(.05));self.assertEqual(e.n,0)
    def test_both_failures_are_counted(self):
        s=cofailure_stats([1,0,0,1],[0,0,1,1]);self.assertEqual(s['both_fail'],1)
    def test_no_op_cannot_be_verified_generation(self):
        r=ImprovementReceipt('a','a','1','2',100,15,15,True,True,True,True,True);self.assertFalse(r.verified_improvement)
    def test_history_only_cannot_be_verified_generation(self):
        r=ImprovementReceipt('a','b','1','2',100,15,15,True,True,False,True,True);self.assertFalse(r.verified_improvement)
    def test_verified_generation_requires_all_receipts(self):
        r=ImprovementReceipt('a','b','1','2',100,15,15,True,True,True,True,True);self.assertTrue(r.verified_improvement)
    def test_capability_registry_alone_fails(self):
        e=CapabilityEvidence('TCN');self.assertFalse(e.audit()['complete']);self.assertIn('train_receipt',e.audit()['missing'])
    def test_commit_failure_cannot_publish(self):
        c=LearningCursor(5,'a');c.prepare(5,6,'b');self.assertFalse(c.commit('b',False));self.assertEqual(c.through,5)
    def test_restore_old_origin_is_not_a_rebuild_trigger(self):
        c=LearningCursor(5000,'a');self.assertTrue(c.restore_available(2,5001))
    def test_stale_cas_rejected(self):
        c=LearningCursor(5,'a');c.prepare(5,6,'b');c.prepare(5,7,'c');self.assertTrue(c.commit('b',True));self.assertFalse(c.commit('c',True))
    def test_missing_gap_rejected(self):
        c=LearningCursor(3,'a');self.assertFalse(c.restore_available(6,9))
if __name__=='__main__':unittest.main(verbosity=2)
