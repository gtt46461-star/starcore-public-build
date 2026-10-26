import sys,unittest,sqlite3,tempfile
from pathlib import Path
sys.path.insert(0,str(Path(__file__).parents[1]/'code'))
from contracts_r25 import *

class Contracts(unittest.TestCase):
    def setUp(self):
        self.s=ContractStore();self.rules={k:k+'_v1' for k in REQUIRED_RULES};self.p=self.s.register(self.rules,100);self.parent=self.s.active(self.p)[0]
    def tearDown(self):self.s.close()
    def change(self,**kw):
        a=dict(p=self.p,event='e100',source_hash='draw100',expected_parent=self.parent,train_through=100,next_target=101,known_through=100,artifact='w100',optimizer='o100',rng='r100');a.update(kw);return self.s.update(**a)
    def test_finite_k3(self):self.assertAlmostEqual(k3_mass([.1]*10,[1,4,10]),.3)
    def test_k4_rejected(self):
        with self.assertRaises(ValueError):k3_mass([.1]*10,[1,2,3,4])
    def test_duplicate_number_rejected(self):
        with self.assertRaises(ValueError):k3_mass([.1]*10,[1,1,3])
    def test_nan_rejected(self):
        with self.assertRaises(ValueError):k3_mass([float('nan')]+[.1]*9,[1,2,3])
    def test_input_timing(self):validate_availability(101,100,1000,1100,50,60,[999])
    def test_late_availability(self):
        with self.assertRaises(ValueError):validate_availability(101,100,1000,1100,50,60,[1001])
    def test_clock_rollback_no_extra_deadline(self):
        with self.assertRaises(ValueError):validate_availability(101,100,900,1100,61,60,[899])
    def test_train_after_target_rejected(self):
        with self.assertRaises(ValueError):self.change(train_through=101)
    def test_unknown_label_rejected(self):
        with self.assertRaises(ValueError):self.change(known_through=99)
    def test_valid_update_retains_policy(self):
        h,changed=self.change();self.assertTrue(changed);self.assertNotEqual(h,self.parent);self.assertEqual(self.s.active(self.p),(h,1))
    def test_duplicate_feedback_once(self):
        first=self.change();self.assertEqual(self.change(),(first[0],False));self.assertEqual(self.s.active(self.p)[1],1)
    def test_conflicting_feedback(self):
        self.change()
        with self.assertRaises(ValueError):self.change(source_hash='corrected-result')
    def test_crash_rolls_back_all_effects(self):
        with self.assertRaises(RuntimeError):self.change(fail_after_insert=True)
        self.assertEqual(self.s.active(self.p),(self.parent,0));self.assertEqual(self.s.db.execute('SELECT count(*) FROM r25_consumption').fetchone()[0],0);self.assertTrue(self.change()[1])
    def test_stale_parent_rejected(self):
        self.change()
        with self.assertRaises(ValueError):self.change(event='new',train_through=101,next_target=102,known_through=101)
    def test_rule_change_new_policy_no_inherited_state(self):
        self.change();other=dict(self.rules,gate='gate_v2');p2=self.s.register(other,101);self.assertNotEqual(p2,self.p);self.assertEqual(self.s.active(p2)[1],0)
    def test_rename_cannot_reset_birth(self):
        with self.assertRaises(ValueError):self.s.register(self.rules,101)
    def test_sql_policy_immutable(self):
        with self.assertRaises(sqlite3.IntegrityError):self.s.db.execute('UPDATE r25_policy SET birth_ordinal=1')
    def test_static_policy_cannot_increment(self):
        rules=dict(self.rules,gate='static');p=self.s.register(rules,100,'STATIC')
        with self.assertRaises(ValueError):self.change(p=p,expected_parent=self.s.active(p)[0])
    def test_campaign_drift(self):
        self.s.task('t','snapshot1')
        with self.assertRaises(ValueError):self.s.task('t','snapshot2')
    def test_live_lease_cannot_be_stolen(self):
        self.s.task('t','s');self.s.claim('t','a',10,10)
        with self.assertRaises(ValueError):self.s.claim('t','b',15,10)
    def test_old_owner_revived_cannot_write(self):
        self.s.task('t','s');f=self.s.claim('t','a',10,10);g=self.s.claim('t','b',21,10)
        with self.assertRaises(ValueError):self.s.progress('t','a',f,22,1)
        self.s.progress('t','b',g,22,1)
    def test_expired_owner_cannot_write(self):
        self.s.task('t','s');f=self.s.claim('t','a',10,10)
        with self.assertRaises(ValueError):self.s.progress('t','a',f,20,1)
    def test_no_progress_rejected(self):
        self.s.task('t','s');f=self.s.claim('t','a',10,10)
        with self.assertRaises(ValueError):self.s.progress('t','a',f,11,0)
    def test_failed_precheck_no_budget(self):
        self.s.cohort('c',self.p,100,200);self.assertIsNone(self.s.reserve_look('c','d',200,False));self.assertEqual(self.s.db.execute('SELECT count(*) FROM r25_look').fetchone()[0],0)
    def test_repeated_look_no_extra_budget(self):
        self.s.cohort('c',self.p,100,200);r=self.s.reserve_look('c','d',200)
        for _ in range(100):self.assertEqual(self.s.reserve_look('c','d',200),r)
        self.assertEqual(self.s.db.execute('SELECT count(*) FROM r25_look').fetchone()[0],1)
    def test_fixed_endpoint_cannot_peek(self):
        self.s.cohort('c',self.p,100,200)
        with self.assertRaises(ValueError):self.s.reserve_look('c','d',199)
    def test_fixed_endpoint_cannot_reopen(self):
        self.s.cohort('c',self.p,100,200);self.s.reserve_look('c','d',200)
        with self.assertRaises(ValueError):self.s.reserve_look('c','new-digest',201)
    def test_prebirth_cohort_rejected(self):
        with self.assertRaises(ValueError):self.s.cohort('c',self.p,99,200)
    def test_augmented_never_natural(self):
        self.assertFalse(evidence_row('TRAIN_AUGMENTED',True));self.assertFalse(evidence_row('SYNTHETIC_MECHANISM',True));self.assertFalse(evidence_row('REAL',False));self.assertTrue(evidence_row('REAL',True))
    def test_report_error_is_complete_but_not_pass(self):
        x={'sectionId':'A','status':'ERROR','expectedRows':10,'exportedRows':0,'omittedRows':10,'errorChain':['missing schema']}
        self.assertEqual(check_envelope([x],['A']),{'structurally_complete':True,'all_passed':False})
    def test_report_omissions_cannot_pass(self):
        with self.assertRaises(ValueError):check_envelope([dict(sectionId='A',status='PASS',expectedRows=79402,exportedRows=5000,omittedRows=74402)],['A'])
    def test_report_missing_section(self):
        with self.assertRaises(ValueError):check_envelope([],['A'])
    def test_row_accounting(self):
        with self.assertRaises(ValueError):check_envelope([dict(sectionId='A',status='PARTIAL',expectedRows=10,exportedRows=5,omittedRows=0)],['A'])
    def test_restart_preserves_effect(self):
        with tempfile.TemporaryDirectory() as d:
            path=Path(d)/'state.db';s=ContractStore(path);p=s.register(self.rules,100);a=s.active(p)[0]
            h=s.update(p,'e','src',a,100,101,100,'w','o','r')[0];s.close()
            s=ContractStore(path);self.assertEqual(s.active(p),(h,1));self.assertFalse(s.update(p,'e','src',a,100,101,100,'w','o','r')[1]);s.close()
    def test_sequential_allocations_sum_bounded(self):
        self.s.cohort('s',self.p,100,1000,.05,'SEQUENTIAL');total=sum(self.s.reserve_look('s',str(i),100+i)[1] for i in range(100));self.assertLess(total,.05)

if __name__=='__main__':unittest.main()
