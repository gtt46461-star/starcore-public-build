"""Original generator vectors plus acceptance/rejection probes for the R34.1 Q08 dependence receipt."""
import copy
import hashlib
import unittest
from types import SimpleNamespace
from q08_receipt import (GROUP_ESTIMAND, NULL_ARMS, POWER_SCOPE, PROTOCOL, SEEDS,
                         check_q08_dependencies, registered_stream, upper95)


class Dependence(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.commit = 'e'*40
        files, hashes, trials = {}, {}, []
        for arm in (*NULL_ARMS, "SIGNAL","DRIFT","GAPS"):
            for seed in SEEDS:
                row = dict(sourceCommit=cls.commit, protocolHash=PROTOCOL, namespace="SYNTHETIC_FULL_FACTORY_R31",
                           arm=arm, seed=seed, status="DETECTED" if seed == SEEDS.start else "NOT_DETECTED",
                           realPermission=False, naturalQualityEligible=False)
                if arm in NULL_ARMS:
                    name = arm+str(seed)
                    sha = hashlib.sha256(registered_stream(seed, arm=="NULL_TIME_SHUFFLED")).hexdigest()
                    files[name], hashes[name] = name, sha
                    row.update(draw_stream_artifact=name,draw_stream_sha256=sha)
                trials.append(row)
        cls.src = {"factory":{"protocolHash":PROTOCOL,"sourceCommit":cls.commit,"trialResults":trials},
                   "power_source_commit":cls.commit,
                   "power_trials":[dict(trueHitProbability=p,rep=i,periods=1,detected=False)
                                   for p in (.34,.37,.40) for i in range(20)],
                   "dependence_artifact":"checked-original"}
        cls.files, cls.hashes = files, hashes
        cls.proof = dict(schema="R341_Q08_DEPENDENCE_1", source_commit=cls.commit, protocol_hash=PROTOCOL,
                         power_scope=POWER_SCOPE, estimand=GROUP_ESTIMAND, natural_authority=False,
                         nominal_trials=60, seed_groups=20, identical_uniform_disabled_pairs=20,
                         any_null_detected_seeds=1, per_arm_detections={a:1 for a in NULL_ARMS},
                         nominal_estimand="REGISTERED_POOLED_VALUE_DEPENDENT_ARMS", any_null_per_seed_upper95=.2161061642068473)

    def check(self, src=None, proof=None):
        originals = SimpleNamespace(files=self.files,hashes=self.hashes,
                                    json=lambda name:{"checked-original":self.proof if proof is None else proof}[name])
        return check_q08_dependencies(src or self.src,{"source_commit":self.commit},originals)

    def test_frozen_java_vectors_and_cluster_value(self):
        expected = ("e1425bc501abb654a33c8a10307f06ef415425c53f2c3eb5426042c593df6221",
                    "7329e4939a7a41b9ec97595c563918bc585fa3f1440938541d8717474e526e4a")
        for shuffle, sha in enumerate(expected):
            self.assertEqual(sha,hashlib.sha256(registered_stream(710000,bool(shuffle))).hexdigest())
        self.assertAlmostEqual(upper95(0,20),1-.05**(1/20))
        self.assertEqual(self.check(),1)

    def test_wrong_pooled_interpretation_or_changed_metric(self):
        for key,value in (("nominal_estimand","INDEPENDENT_BINOMIAL"),("seed_groups",60),("any_null_detected_seeds",3),
                          ("any_null_per_seed_upper95",upper95(3,60)),("power_scope","FULL_FACTORY"),
                          ("natural_authority",True),("source_commit","d"*40)):
            with self.subTest(key=key),self.assertRaises(ValueError):
                self.check(proof={**self.proof,key:value})

    def test_wrong_stream_relabelled_seed_missing_or_reused(self):
        for alter in ("missing","relabelled","reused","moved_power_role","missing_arm"):
            src = copy.deepcopy(self.src)
            trials = src["factory"]["trialResults"]
            if alter=="missing":
                trials[0]["draw_stream_artifact"]="absent"
            elif alter=="relabelled":
                trials[0]["seed"]=710001
            elif alter=="reused":
                for key in ("draw_stream_artifact","draw_stream_sha256"):
                    trials[1][key]=trials[0][key]
            elif alter=="moved_power_role":
                src["power_trials"].pop()
            else:
                trials.pop()
            with self.subTest(alter=alter),self.assertRaises(ValueError):
                self.check(src=src)


if __name__ == "__main__":
    unittest.main()
