import sys,json,math,tempfile,unittest
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1];sys.path.insert(0,str(ROOT/'code'))
from runtime_reference import *

def state(n=0):return {k:({'n':n} if k=='weights' else {'v':0}) for k in STATE}
def spec():
    s={k:{'v':1.23441} for k in RULES};s['initial_state']=state();return s

class Identity(unittest.TestCase):
    def test_near_floats(self):self.assertNotEqual(digest(1.23441),digest(1.23449))
    def test_type_separation(self):self.assertEqual(len({digest(1),digest(1.),digest(True),digest('1')}),4)
    def test_signed_zero(self):self.assertNotEqual(digest(-0.),digest(0.))
    def test_nonfinite(self):
        for v in [float('nan'),float('inf'),-float('inf')]:
            with self.assertRaises(ValueError):digest(v)
    def test_map_order(self):self.assertEqual(digest({'中':1,'a':2}),digest({'a':2,'中':1}))
    def test_utf8_order(self):self.assertLess(canonical({'\U00010000':1,'\ue000':2}).index('\ue000'),canonical({'\U00010000':1,'\ue000':2}).index('\U00010000'))
    def test_surrogate_rejected(self):
        with self.assertRaises(UnicodeEncodeError):digest('\ud800')
    def test_input_mutation(self):
        s=spec();p=Policy.create(s);h=p.hash;s['graph']['v']=999.;self.assertEqual(p.hash,h);self.assertNotEqual(Policy.create(s).hash,h)
    def test_complete_rule_contract(self):
        s=spec();s.pop('resource_schedule')
        with self.assertRaises(ValueError):Policy.create(s)
    def test_parameter_order_irrelevant(self):self.assertEqual(Policy.create(spec()).hash,Policy.create(dict(reversed(list(spec().items())))).hash)

class StoreTests(unittest.TestCase):
    def setUp(self):
        self.temp=tempfile.TemporaryDirectory();self.path=Path(self.temp.name)/'state.db';self.s=RuntimeStore(self.path);self.p=Policy.create(spec());self.parent=self.s.register(self.p,1,state());self.s.task('train','P2','F01','snap');self.f=self.s.claim('train','owner',1,100);self.s.feedback('e1',1,'source1',{'label':1})
    def tearDown(self):self.s.close();self.temp.cleanup()
    def commit(self,**kw):
        args=dict(policy=self.p.hash,event='e1',expected_parent=self.parent,state=state(1),target=2,known_through=1,task='train',owner='owner',fence=self.f,now=2,new_cursor=1);args.update(kw);return self.s.commit_update(**args)
    def test_exactly_once_effect(self):
        h,new=self.commit();self.assertTrue(new);self.assertEqual(self.commit(),(h,False));self.assertEqual(self.s.active(self.p.hash),(h,1))
    def test_event_conflict(self):
        with self.assertRaises(ValueError):self.s.feedback('e1',1,'different',{'label':1})
    def test_event_duplicate(self):self.assertFalse(self.s.feedback('e1',1,'source1',{'label':1}))
    def test_future_rejected(self):
        with self.assertRaises(ValueError):self.commit(target=1)
    def test_unavailable_rejected(self):
        with self.assertRaises(ValueError):self.commit(known_through=0)
    def test_parent_cas(self):
        with self.assertRaises(ValueError):self.commit(expected_parent='wrong')
    def test_stale_fence(self):
        f2=self.s.claim('train','newowner',102,100)
        with self.assertRaises(ValueError):self.commit(now=103)
        self.assertTrue(self.commit(owner='newowner',fence=f2,now=103)[1])
    def test_expired_lease(self):
        with self.assertRaises(ValueError):self.commit(now=101)
    def test_incomplete_state(self):
        v=state();v.pop('rng')
        with self.assertRaises(ValueError):self.commit(state=v)
    def test_initial_state_is_bound(self):
        with self.assertRaises(ValueError):self.s.register(self.p,1,state(99))
    def test_crash_before_commit_rolls_back(self):
        with self.assertRaises(RuntimeError):self.commit(fail_before_commit=True)
        self.assertEqual(self.s.active(self.p.hash),(self.parent,0));self.assertEqual(self.s.db.execute('SELECT COUNT(*) FROM r26_consumption').fetchone()[0],0);self.assertTrue(self.commit()[1])
    def test_after_commit_restart(self):
        h,_=self.commit();self.s.close();self.s=RuntimeStore(self.path);self.assertEqual(self.s.active(self.p.hash),(h,1));self.assertEqual(self.commit(),(h,False))
    def test_snapshot_drift(self):
        with self.assertRaises(ValueError):self.s.task('train','P2','F01','new-snap')
    def test_no_progress(self):
        with self.assertRaises(ValueError):self.commit(new_cursor=0)
    def test_no_silent_event_skip(self):
        self.s.feedback('e2',2,'source2',{'label':2})
        with self.assertRaises(ValueError):self.commit(event='e2',target=3,known_through=2)
    def test_recovery_next_update_equivalence(self):
        h,_=self.commit();self.s.close();self.s=RuntimeStore(self.path);self.s.feedback('e2',2,'source2',{'label':2});f=self.s.claim('train','owner',3,100)
        h2,new=self.commit(event='e2',target=3,known_through=2,expected_parent=h,state=state(2),fence=f,now=4,new_cursor=2)
        expected=digest({'domain':'STARCORE_CHECKPOINT_R26_V1','policy':self.p.hash,'parent':h,'through':2,'state':state(2),'event':'e2','source':'source2'})
        self.assertEqual(h2,expected);self.assertTrue(new)

class BundleContracts(unittest.TestCase):
    def test_all_capabilities_enabled_without_power_gate(self):
        p=json.loads((ROOT/'registries/runtime_policy.json').read_text())
        for k in ['requires_charging','requires_device_idle','requires_screen_off','requires_unmetered_for_local_training','periodic_job_is_primary_training_driver']:self.assertFalse(p[k])
        self.assertEqual(len(p['family_ids']),24);self.assertEqual(len(p['expert_ids']),16)
    def test_card_identity_and_source_coverage(self):
        cards=json.loads((ROOT/'registries/unified_128_cards.json').read_text());ids={x['id'] for x in cards};self.assertEqual(len(ids),128)
        self.assertEqual(sum(x.startswith('A:') for x in ids),32);self.assertEqual(sum(x.startswith('B:') for x in ids),32)
        for c in cards:
            if c.get('source'):self.assertTrue((ROOT/c['source']).is_file())
    def test_report_preserves_36_and_adds_4(self):
        old=json.loads((ROOT/'sources/plan_a/upgrade_r25/registries/report_contract_r25.json').read_text());new=json.loads((ROOT/'registries/report_contract_40.json').read_text());self.assertEqual(len(new['sections']),40);self.assertEqual(new['sections'][:36],old['sections'])

if __name__=='__main__':unittest.main()
