"""Check the paired input streams and the interpretation of the registered R31 Q08 experiment."""
import functools
import hashlib
import math


NULL_ARMS = ("NULL_UNIFORM", "SIGNAL_DISABLED", "NULL_TIME_SHUFFLED")
SEEDS = range(710000, 710020)
PROTOCOL = "91b63a645fc0fcbfd0dbb6488dd252d2d6b5ca28ce941211dac8b71c6fa0a930"
GROUP_ESTIMAND = "ANY_NULL_DETECTION_PER_REGISTERED_SEED"
POWER_SCOPE = "SEQUENTIAL_ONLY_INDEX1_ALL_ACT"


def need(test, message):
    if not test:
        raise ValueError(message)


class SplitMix:
    """Bit-exact counterpart of StatsR27.SplitMix for the pinned R31 input generator."""
    mask = (1 << 64) - 1

    def __init__(self, seed):
        self.state = (seed * 0x9E3779B97F4A7C15 + 31) & self.mask

    def bits(self):
        self.state = (self.state + 0x9E3779B97F4A7C15) & self.mask
        z = self.state
        z = ((z ^ (z >> 30)) * 0xBF58476D1CE4E5B9) & self.mask
        z = ((z ^ (z >> 27)) * 0x94D049BB133111EB) & self.mask
        return z ^ (z >> 31)

    def choice(self, bound):
        bits = self.bits()
        signed = bits if bits < (1 << 63) else bits - (1 << 64)
        return signed % bound


@functools.lru_cache(maxsize=40)
def registered_stream(seed, shuffled):
    """Rows from generateStream(seed, 3000+6000, position 3, strength .30); headerless unsigned bytes."""
    rng = SplitMix(seed)
    rows = []
    for index in range(9000):
        row = list(range(1, 11))
        for i in range(9, 0, -1):
            j = rng.choice(i + 1)
            row[i], row[j] = row[j], row[i]
        if shuffled and index and (rng.bits() >> 11) * 2.0**-53 < .30:
            j = row.index(rows[-1][2])
            row[j], row[2] = row[2], row[j]
        rows.append(bytes(row))
    if shuffled:
        for i in range(len(rows)-1, 0, -1):
            j = rng.choice(i + 1)
            rows[i], rows[j] = rows[j], rows[i]
    return b"".join(rows)


def upper95(events, n):
    if events == n:
        return 1.0
    lo, hi = 0.0, 1.0
    for _ in range(90):
        p = (lo + hi)/2
        cdf = sum(math.comb(n,k) * p**k * (1-p)**(n-k) for k in range(events+1))
        if cdf > .05:
            lo = p
        else:
            hi = p
    return (lo + hi)/2


def check_q08_dependencies(src, result, originals):
    """src is the ORIGINAL Q08 component, result contains source_commit, originals is verify_receipt.Originals."""
    data = src["factory"]
    need(data["protocolHash"] == PROTOCOL and data["sourceCommit"] == result["source_commit"],
         "Q08 registered input scope or source mismatch")
    proof = originals.json(src["dependence_artifact"])
    need(proof["schema"] == "R341_Q08_DEPENDENCE_1" and proof["source_commit"] == result["source_commit"]
         and proof["protocol_hash"] == PROTOCOL and proof["power_scope"] == POWER_SCOPE
         and proof["estimand"] == GROUP_ESTIMAND and proof["natural_authority"] is False,
         "Wrong dependence evidence or power interpretation")
    arms = (*NULL_ARMS, "SIGNAL", "DRIFT", "GAPS")
    trials = data["trialResults"]
    need(len(trials) == 120 and {(r["arm"],r["seed"]) for r in trials} == {(a,s) for a in arms for s in SEEDS},
         "The registered 120 arm/seed records must remain present")
    powers = src["power_trials"]
    need(src["power_source_commit"] == result["source_commit"] and len(powers) == 60 and
         {(r["trueHitProbability"],r["rep"]) for r in powers} == {(p,i) for p in (.34,.37,.40) for i in range(20)}
         and all(type(r["detected"]) is bool and type(r["periods"]) is int and 0 < r["periods"] <= 5000 for r in powers),
         "Separate R30 sequential scan absent or incomplete")
    rows = [r for r in trials if r["arm"] in NULL_ARMS]
    need(len(rows) == 60 and {(r["arm"],r["seed"]) for r in rows} ==
         {(a,s) for a in NULL_ARMS for s in SEEDS}, "Missing or repeated null trial")
    groups = {}
    arm_events = {arm:0 for arm in NULL_ARMS}
    for row in rows:
        arm, seed = row["arm"], row["seed"]
        need(type(seed) is int and row["protocolHash"] == PROTOCOL and row["sourceCommit"] == result["source_commit"]
             and row["status"] in ("DETECTED","NOT_DETECTED") and row["naturalQualityEligible"] is False
             and row["realPermission"] is False and row["namespace"] == "SYNTHETIC_FULL_FACTORY_R31",
             "Invalid synthetic null trial original")
        artifact = row["draw_stream_artifact"]
        expected = registered_stream(seed, arm == "NULL_TIME_SHUFFLED")
        need(artifact in originals.files and originals.hashes[artifact] == row["draw_stream_sha256"]
             and originals.hashes[artifact] == hashlib.sha256(expected).hexdigest(), "Wrong registered input stream")
        found = row["status"] == "DETECTED"
        groups[seed] = groups.get(seed,False) or found
        arm_events[arm] += int(found)
    grouped = sum(groups.values())
    need(type(proof["nominal_trials"]) is int and proof["nominal_trials"] == 60
         and type(proof["seed_groups"]) is int and proof["seed_groups"] == 20 and
         type(proof["identical_uniform_disabled_pairs"]) is int and proof["identical_uniform_disabled_pairs"] == 20
         and type(proof["any_null_detected_seeds"]) is int and proof["any_null_detected_seeds"] == grouped
         and proof["nominal_estimand"] == "REGISTERED_POOLED_VALUE_DEPENDENT_ARMS",
         "Incorrect seed grouping or independence claim")
    need(isinstance(proof["per_arm_detections"],dict) and all(type(v) is int for v in proof["per_arm_detections"].values())
         and proof["per_arm_detections"] == arm_events, "Null-arm counts differ from originals")
    observed = proof["any_null_per_seed_upper95"]
    need(type(observed) in (float,int) and math.isfinite(observed)
         and abs(observed - upper95(grouped,20)) <= 1e-9, "Wrong seed-cluster bound")
    return grouped
