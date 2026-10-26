import unittest,sys,math,itertools,random,dataclasses
from pathlib import Path
sys.path.insert(0,str(Path(__file__).resolve().parents[1]/'code'))
from reference_r25 import *
class R25Tests(unittest.TestCase):
 def spec(self):return dict(graph={'op':'matching','weight':1.23441},updater={'op':'sgd','lr':.01},feature_contract={'lag':1},calibration_rule='past_only',gate_rule='quality_first',protocol='R25-v1')
 def receipt(self,**kwargs):
  d=dict(policy_hash='p',checkpoint_hash='c',target=11,trained_through=10,input_through=10,freeze_elapsed_ns=5,deadline_elapsed_ns=10,status='ACT',position=1,numbers=(1,2,3));d.update(kwargs);return FrozenReceipt(**d)
 def test_identity_distinguishes_close_float(self):self.assertNotEqual(digest(1.23441),digest(1.23449))
 def test_identity_types_distinct(self):self.assertNotEqual(digest(1),digest(1.))
 def test_identity_order(self):self.assertEqual(digest({'a':1,'b':2}),digest({'b':2,'a':1}))
 def test_identity_no_mutable_alias(self):
  s=self.spec();p=Policy.create(s);text=p.canonical_spec;s['graph']['weight']=3;self.assertEqual(p.canonical_spec,text);self.assertNotEqual(p.policy_hash,Policy.create(s).policy_hash)
 def test_policy_requires_complete(self):
  with self.assertRaises(ValueError):Policy.create({'graph':1})
 def test_identity_nan_rejected(self):
  with self.assertRaises(ValueError):digest(float('nan'))
 def test_checkpoint_state_changes_not_policy(self):
  p=Policy.create(self.spec());a=checkpoint_hash(p,[1.],10,{},1,'a');b=checkpoint_hash(p,[1.1],11,{},1,a);self.assertNotEqual(a,b);self.assertEqual(p.policy_hash,Policy.create(self.spec()).policy_hash)
 def test_updater_change_new_policy(self):
  a=self.spec();b=self.spec();b['updater']['lr']=.02;self.assertNotEqual(Policy.create(a).policy_hash,Policy.create(b).policy_hash)
 def test_future_train_rejected(self):
  with self.assertRaises(ValueError):self.receipt(trained_through=11).validate()
 def test_future_input_rejected(self):
  with self.assertRaises(ValueError):self.receipt(input_through=12).validate()
 def test_late_freeze_rejected(self):
  with self.assertRaises(ValueError):self.receipt(freeze_elapsed_ns=10).validate()
 def test_duplicate_numbers_rejected(self):
  with self.assertRaises(ValueError):self.receipt(numbers=(1,1,2)).validate()
 def test_k4_rejected(self):
  with self.assertRaises(ValueError):self.receipt(numbers=(1,2,3,4)).validate()
 def test_mislabelled_history_rejected(self):
  with self.assertRaises(ValueError):self.receipt(evidence_scope='HISTORY').validate()
 def test_valid_receipt(self):self.receipt().validate()
 def test_abstain_no_hidden_action(self):
  with self.assertRaises(ValueError):self.receipt(status='ABSTAIN').validate()
 def test_correct_abstain(self):self.receipt(status='ABSTAIN',position=None,numbers=()).validate()
 def test_global_budget_summable(self):
  s=sum(alpha_allocation(.05,j,k,1.) for j in range(1,101) for k in range(1,101));self.assertLess(s,.05);self.assertGreater(s,.049)
 def test_noninferiority_not_failure_to_reject(self):self.assertFalse(noninferior([-.05]*1000,.025,.01))
 def test_noninferiority_supported_case(self):self.assertTrue(noninferior([.2]*1000,.025,.01))
 def test_paired_insufficient_rejected(self):
  with self.assertRaises(ValueError):paired_lower([],.05)
 def test_skip_not_hit(self):
  with self.assertRaises(ValueError):excess_hit(False,True)
 def test_evalue_mean_under_iid_null(self):
  n=8;s=0.
  for y in itertools.product([0,1],repeat=n):
   k=sum(y);s+=.3**k*.7**(n-k)*math.exp(bernoulli_log_e(y,[.4]*n))
  self.assertAlmostEqual(s,1.,places=12)
 def test_evalue_bad_alternative_rejected(self):
  with self.assertRaises(ValueError):bernoulli_log_e([1],[.2])
 def test_streak_short(self):self.assertEqual(probability_of_losing_run(6,.4,7),0.)
 def test_streak_single_window(self):self.assertAlmostEqual(probability_of_losing_run(7,.4,7),.6**7,places=12)
 def test_streak_longer_is_riskier(self):self.assertGreater(probability_of_losing_run(750,.4),probability_of_losing_run(250,.4))
 def test_streak_extreme(self):self.assertEqual(probability_of_losing_run(8,0),1.);self.assertEqual(probability_of_losing_run(8,1),0.)
 def test_uniform_matching(self):
  z,p=exact_unary_matching([[0.]*10 for _ in range(10)]);self.assertAlmostEqual(z,math.lgamma(11),places=11)
  for row in p:
   for x in row:self.assertAlmostEqual(x,.1,places=11)
 def test_matching_matches_enumeration(self):
  rng=random.Random(3);n=4;s=[[rng.uniform(-2,2) for _ in range(n)] for _ in range(n)];z,p=exact_unary_matching(s);zs=0.;m=[[0.]*n for _ in range(n)]
  for pi in itertools.permutations(range(n)):
   w=math.exp(sum(s[i][pi[i]] for i in range(n)));zs+=w
   for i,j in enumerate(pi):m[i][j]+=w
  self.assertAlmostEqual(z,math.log(zs),places=12)
  for i in range(n):
   for j in range(n):self.assertAlmostEqual(p[i][j],m[i][j]/zs,places=12)
 def test_gradient_finite_difference(self):
  s=[[.1,.4,-.2],[-.1,.3,.7],[.2,-.4,.8]];d=[2,0,1];v,g=matching_nll_gradient(s,d);eps=1e-5
  for i in range(3):
   for j in range(3):
    a=[r[:] for r in s];b=[r[:] for r in s];a[i][j]+=eps;b[i][j]-=eps
    fd=(matching_nll_gradient(a,d)[0]-matching_nll_gradient(b,d)[0])/(2*eps);self.assertAlmostEqual(fd,g[i][j],places=8)
 def test_gradient_step_changes_and_reduces_toy_loss(self):
  s=[[0.]*3 for _ in range(3)];v,g=matching_nll_gradient(s,[2,0,1]);t=[[s[i][j]-.1*g[i][j] for j in range(3)] for i in range(3)];self.assertLess(matching_nll_gradient(t,[2,0,1])[0],v)
 def test_large_logits_stable(self):
  z,p=exact_unary_matching([[1000.,-1000.],[-1000.,1000.]]);self.assertTrue(math.isfinite(z));self.assertAlmostEqual(p[0][0],1.)
 def test_k3_universe(self):
  _,p=exact_unary_matching([[0.]*10 for _ in range(10)]);a=k3_actions(p);self.assertEqual(len(a),1200);self.assertEqual(sum(1 for pos,nums,_ in a if pos in nums),360)
  for _,_,q in a:self.assertAlmostEqual(q,.3,places=10)
 def test_bad_permutation(self):
  with self.assertRaises(ValueError):matching_nll_gradient([[0.,0.],[0.,0.]],[1,1])
 def test_noncoherent_probability_rejected(self):
  with self.assertRaises(ValueError):k3_actions([[1.]+[0.]*9 for _ in range(10)])
if __name__=='__main__':unittest.main()
