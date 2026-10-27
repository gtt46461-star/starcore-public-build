#!/usr/bin/env python3
"""Print bounded engineering receipt metadata without history or label payloads."""
from pathlib import Path
import argparse,json,re,sys,unittest

def integer(value):
    return value if type(value) is int and value>=0 else None

def summary(receipt,owner_text=""):
    if not isinstance(receipt,dict):
        return {"availability":"NO_VALID_RUNTIME_RECEIPT","scope":"NO_ACCEPTANCE_EVIDENCE"}
    keys=("result","scope","api","familyMechanismsWithCommittedProgress","expertsWithActualReceipts",
          "allMechanismsServiceMillis","strategyReadyToAllMechanismsWallMs","firstServiceTargetMs",
          "firstServiceTargetObservedWithin","canonicalDrawWriteP50Ms","canonicalDrawWriteP95Ms",
          "canonicalDrawWriteP99Ms","canonicalDrawWriteMaxMs","retentionBoundaryRenewed",
          "restartResumed","staleOwnerRejected","checkpointFileBytes")
    out={key:receipt[key] for key in keys if key in receipt}
    out["availability"]="RUNTIME_RECEIPT"
    out["fullPredrawOpportunitySloClaimed"]=receipt.get("fullPredrawOpportunitySloClaimed",False)
    out["writeProbeScope"]="Canonical draw writes only; not full prediction opportunities"
    totals=receipt.get("committedResearchService") or {}
    failures=integer(totals.get("committedCapacityFailuresSinceInstallation"))
    out["durableServiceCounterAvailability"]="INSTALLED" if failures is not None else "UNKNOWN_OR_NOT_INSTALLED"
    out["committedCapacityFailuresSinceInstallation"]=failures
    out["committedServiceScope"]=totals.get("scope")
    sessions=totals.get("sessions") or []
    out["sourceCommits"]=sorted({s["sourceCommit"] for s in sessions if isinstance(s,dict) and isinstance(s.get("sourceCommit"),str) and re.fullmatch(r"[0-9a-f]{40}",s["sourceCommit"])})
    out["numericalAbis"]=sorted({s["numericalAbi"] for s in sessions if isinstance(s,dict) and isinstance(s.get("numericalAbi"),str) and re.fullmatch(r"[0-9a-f]{64}",s["numericalAbi"])})
    endurance=receipt.get("endurance") or {}
    samples=endurance.get("samples") or []
    retained=[integer(s.get("capacityFailReceipts")) for s in samples]
    durable=[integer(s.get("committedServiceCapacityFailures")) for s in samples]
    retained=[x for x in retained if x is not None]
    durable=[x for x in durable if x is not None]
    out["endurance"]={"screen":endurance.get("screen"),"elapsedMs":endurance.get("elapsedMs"),
        "minuteSamples":len(samples),"maximumRetainedSliceFailures":max(retained) if retained else None,
        "maximumCommittedSliceFailures":max(durable) if durable else None,
        "retainedCounterScope":"Bounded receipt ring; may decrease; never substitutes for monotonic totals"}
    failures=[]
    pattern=r"task=([A-Za-z0-9_-]{1,64})\|wallMs=(\d+)\|cpuMs=(\d+)\|CAPACITY_FAIL:UNSLICED_KERNEL:(\d+)ms"
    for match in re.finditer(pattern,owner_text):
        task,wall,cpu,kernel=match.groups()
        failures.append({"task":task,"wallMs":int(wall),"cpuMs":int(cpu),"kernelWallMs":int(kernel)})
    out["retainedOwnerCapacityEvents"]=failures[-32:]
    out["ownerCapacityEventsOmitted"]=max(0,len(failures)-32)
    return out

class ScopeTests(unittest.TestCase):
    def test_absent_counter_is_unknown(self):
        self.assertIsNone(summary({})["committedCapacityFailuresSinceInstallation"])
        self.assertIsNone(summary({"committedResearchService":{"availability":"NOT_INSTALLED","committedCapacityFailuresSinceInstallation":None}})["committedCapacityFailuresSinceInstallation"])
    def test_ring_cleanup_does_not_erase_durable_failure(self):
        result=summary({"committedResearchService":{"committedCapacityFailuresSinceInstallation":8},"endurance":{"samples":[{"capacityFailReceipts":6,"committedServiceCapacityFailures":7},{"capacityFailReceipts":0,"committedServiceCapacityFailures":8}]}})
        self.assertEqual(8,result["committedCapacityFailuresSinceInstallation"])
        self.assertEqual(6,result["endurance"]["maximumRetainedSliceFailures"])
        self.assertEqual(8,result["endurance"]["maximumCommittedSliceFailures"])
    def test_writes_do_not_create_full_opportunity_claim(self):
        result=summary({"engineeringWriteProbes":1000,"canonicalDrawWriteP99Ms":14,"payload":"PRIVATE_HISTORY"})
        self.assertFalse(result["fullPredrawOpportunitySloClaimed"])
        self.assertNotIn("PRIVATE_HISTORY",json.dumps(result))
    def test_owner_filter_drops_unrelated_and_message_data(self):
        result=summary({}, "credential=DO_NOT_EXPORT\ntask=foundry-su2|wallMs=1200|cpuMs=8|CAPACITY_FAIL:UNSLICED_KERNEL:1200ms|PRIVATE_MESSAGE")
        self.assertEqual([{"task":"foundry-su2","wallMs":1200,"cpuMs":8,"kernelWallMs":1200}],result["retainedOwnerCapacityEvents"])
        self.assertNotIn("PRIVATE",json.dumps(result))
        self.assertNotIn("credential",json.dumps(result))

def main():
    p=argparse.ArgumentParser(description=__doc__)
    p.add_argument("--receipt",default="r26-runtime-synthetic.json")
    p.add_argument("--owner",default="r26-owner-logcat.txt")
    p.add_argument("--self-test",action="store_true")
    args=p.parse_args()
    if args.self_test:
        result=unittest.TextTestRunner().run(unittest.defaultTestLoader.loadTestsFromTestCase(ScopeTests))
        return 0 if result.wasSuccessful() else 1
    try:receipt=json.loads(Path(args.receipt).read_text())
    except (OSError,ValueError):receipt=None
    try:owner=Path(args.owner).read_text(errors="replace")
    except OSError:owner=""
    print("R26_RUNTIME_ENGINEERING_SUMMARY="+json.dumps(summary(receipt,owner),sort_keys=True,separators=(",",":"),ensure_ascii=False))
    return 0
if __name__=="__main__":sys.exit(main())
