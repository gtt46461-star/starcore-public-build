import copy
import unittest
from stability5 import RULE_HASH, reconstruct, verify_stability


def row(seq, hit):
    return {"target_key": "20261124%04d" % seq, "hit": hit}


class StabilityTests(unittest.TestCase):
    def test_fixed_blocks_and_eligibility(self):
        # 720 belongs to the next block. The first has 19 losses and contributes no block rate.
        rows = [row(i, False) for i in range(701, 720)] + [row(i, i % 2 == 0) for i in range(720, 740)]
        self.assertEqual(reconstruct(list(reversed(rows))), (.5, 19))
        self.assertEqual(reconstruct(rows + [row(700, False)]), (0, 20))
        self.assertEqual(reconstruct(rows[:19])[0], None)

    def test_last_act_is_anchor_and_abstentions_do_not_reset(self):
        self.assertEqual(reconstruct([row(11,False),row(12,False),row(729,False),row(731,False)]), (None,3))
        with self.assertRaises(ValueError):
            reconstruct([row(13,True),row(13,False)])
        for bad in ['202602300001', '202611240000', '202611241441']:
            with self.assertRaises(ValueError):
                reconstruct([dict(target_key=bad,hit=True)])

    def test_independent_projection_and_false_claims(self):
        base=dict(candidate_hash="c",source_commit="s",protocol_hash="p")
        result=dict(**base,future_act_count=250,future_act_ledger_artifact="acts")
        doc=dict(**base,role="PREREGISTERED_FUTURE",fixture=False,settled_acts=[row(i,i%2==0) for i in range(1,251)])
        original=type("Originals",(),{"json":lambda self,key:doc})()
        pre={"stability_window_rule_hash":RULE_HASH}
        q=dict(window_rule_hash=RULE_HASH,values=dict(worst720Block=.5,maxLosingStreak=1))
        verify_stability(pre,result,q,original)
        for key,val in (("worst720Block",.6),("maxLosingStreak",0),("maxLosingStreak",True)):
            with self.subTest(key=key,value=val), self.assertRaises(ValueError):
                stale=copy.deepcopy(q); stale["values"][key]=val
                verify_stability(pre,result,stale,original)
        # Same 125 hits and same 50% total, but all losses concentrate in one run.
        doc["settled_acts"]=[row(i,i>125) for i in range(1,251)]
        with self.assertRaisesRegex(ValueError,"streak differs"):
            verify_stability(pre,result,q,original)
        q["values"]["maxLosingStreak"]=125
        with self.assertRaisesRegex(ValueError,"below the existing thresholds"):
            verify_stability(pre,result,q,original)
        with self.assertRaises(ValueError):
            verify_stability(dict(stability_window_rule_hash="late"),result,q,original)


if __name__ == "__main__":
    unittest.main()
