"""Bind the disposable rolling-checkpoint test build to its reviewed source bytes."""
from pathlib import Path
import hashlib
import json
import os
import subprocess
import sys

parent = "5c7f1b36eaee6036df280e85835bb0fb4ce0b40d"
source = os.environ["REPAIR_INPUT"]
reasons = {
  "app/src/main/java/com/ethk3/novaonepro/K3ActionSpaceEngine.java": "Reuse the exact immutable action identifiers and skip unused digest serialization for cardinality-only checks.",
  "app/src/main/java/com/ethk3/novaonepro/StarcoreEvidenceKernel.java": "Use the same UTF-8 evidence SHA-256 without formatting each byte and retain the existing failure disposition.",
  "app/src/main/java/com/ethk3/novaonepro/r952/ResearchDirectorR952.java": "Reserve an existing Director turn for already-ready rolling learners and pending frozen publications.",
  "app/src/main/java/com/ethk3/novaonepro/r952/RollingLearnerR343.java": "Check the persisted numerical rule before training and retire it only after an authenticated later settlement.",
  "app/src/main/java/com/ethk3/novaonepro/ResearchLearningContinuityR912.java": "Reopen the existing Director lane for pending rolling learner and freeze work on the same draw.",
  "app/src/main/java/com/ethk3/novaonepro/r952/TrainingRecoveryR343.java": "Use the verified sealed field task identity with the original file, protocol, and quarantined-source checks.",
  "app/src/main/java/com/ethk3/novaonepro/r952/ResearchTrainingHookRegistryR952.java": "Rotate historical input verification within the existing TRAIN owner instead of doing it ahead of every ready task.",
  "app/src/main/java/com/ethk3/novaonepro/RuntimeHealthLedgerR36.java": "Select the same release-scoped event and predraw receipt through a bounded join order.",
  "app/src/main/java/com/ethk3/novaonepro/RuntimeTruthSnapshotR906.java": "Count the same retained draws and strict label evidence without materializing all history.",
  "app/src/main/java/com/ethk3/novaonepro/RuntimeAttemptObservabilityR926.java": "Read matching recent distinct attempts and authoritative resource observations with indexed probes.",
  "app/src/main/java/com/ethk3/novaonepro/ModelEffectivenessAnalyzerR35.java": "Reuse the exact immutable snapshot business truth and runtime window across report sections.",
  "app/src/main/java/com/ethk3/novaonepro/LiveResearchDiagnosisOrchestrator.java": "Scope report computation to the chapters that need model and business inputs.",
  "app/src/main/java/com/ethk3/novaonepro/ReportExportServiceCP4.java": "Use the existing native durability flush before finalizing the same verified snapshot.",
  "app/src/main/cpp/report_native_cp4.c": "Pace native backup scheduling while preserving the cancellation, storage, and realtime callbacks.",
  "app/src/main/java/com/ethk3/novaonepro/ChartsRepositoryR27.java": "Keep the most recent valid chart frame while preserving its original source and freshness status.",
  "app/src/main/java/com/ethk3/novaonepro/strategyv2/ChartCacheR27.java": "Select a valid retained chart frame without treating a stale frame as current evidence.",
  "app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchPoolR27.java": "Keep a lane reserve within the original four continuation permits while observing worker termination before release.",
  "app/src/main/java/com/ethk3/novaonepro/ReportSchemaSourceProviderR335.java": "Keep the existing source preparation window available for the original migration owner after one cold Binder timeout.",
  "app/src/main/java/com/ethk3/novaonepro/V51TianshuEvidenceBridge.java": "Reuse identical point-in-time pattern and number evidence within one request.",
  "app/src/main/java/com/ethk3/novaonepro/TransitionRelationEngine.java": "Consume exact matching evidence computed from the same ordered draw history by the active bridge.",
  "app/src/main/java/com/ethk3/novaonepro/DeferredEvidenceSnapshotR36.java": "Reuse the identical decimal text for repeating action metadata in the same verified snapshot.",
  "app/src/main/java/com/ethk3/novaonepro/V51ChecksumEngine.java": "Reduce allocation for identical UTF-8 SHA-256 hex bytes using the same provider."
}
paths = list(reasons)
def git(*args):
    return subprocess.check_output(["git",*args])
def sha(raw):
    return hashlib.sha256(raw).hexdigest()
def document(path,data):
    Path(path).write_text(json.dumps(data,indent=2,sort_keys=True,ensure_ascii=False)+"\n")
assert git("rev-parse","HEAD").decode().strip() == parent
for path in paths + ["app/src/androidTest/java/com/ethk3/novaonepro/r952/R343TrainingNativeTest.java","app/src/androidTest/java/com/ethk3/novaonepro/R30FaultMatrixTest.java","app/src/androidTest/java/com/ethk3/novaonepro/Field173CoverageAndroidTest.java","app/src/androidTest/java/com/ethk3/novaonepro/r952/R343RollingNativeTest.java","app/src/androidTest/java/com/ethk3/novaonepro/ResearchContinuationAndroidTest.java","app/src/androidTest/java/com/ethk3/novaonepro/Field173ChartGalleryCapture.java","app/src/androidTest/java/com/ethk3/novaonepro/strategyv2/R31FamilyContinuationInstrumentedTest.java"]:
    raw=git("show",source+":"+path)
    assert raw != Path(path).read_bytes(), path
    Path(path).write_bytes(raw)
sys.path.insert(0,str(Path("scripts").resolve()))
import r967_semantic_identity as sem
classified={e["path"]:e for e in json.loads(Path(sem.MANIFEST).read_text())["files"]}
proof=Path("release/r82_training_invalidation.json")
v=json.loads(proof.read_text())
allowed=set(paths) | {"app/src/main/java/com/ethk3/novaonepro/ReleaseManifestR926.java","release/release_manifest.json","release/release_identity.properties"}
baseline={row["path"]:row for row in json.loads(Path("release/r81_controlled_source_baseline.json").read_text())["files"]}
review_by_path={entry["path"]:entry for entry in v["controlled_changes"]}
reviewer="STARCORE 0a0f960b field repair"
for path in sorted(allowed & classified.keys()):
    actual=sha(Path(path).read_bytes())
    before=baseline.get(path,{})
    role=classified[path]["role"]
    if role == "PROVENANCE" and before.get("role","") in ("PROVENANCE", ""):
        continue
    if (actual,role)==(before.get("sha256",""),before.get("role","")):
        review_by_path.pop(path,None)
    else:
        review_by_path[path]=dict(path=path,before_sha256=before.get("sha256",""),after_sha256=actual,
                                  before_role=before.get("role",""),after_role=role,reviewer=reviewer,
                                  rationale=reasons[path] if path in paths else
                                  "Verified unchanged release binding while restoring the original test entry.")
v["controlled_changes"]=list(review_by_path.values())
provenance=[[path,sha(Path(path).read_bytes())] for path,entry in sorted(classified.items()) if entry["role"]!="PROVENANCE"]
v["reviewed_input_provenance_sha256"]=sha(sem.canonical(provenance).encode())
document(proof,v)
for args in (["scripts/r967_semantic_identity.py"],["scripts/r967_semantic_identity.py","--check"],["scripts/r926_release_manifest.py","--check"]):
    subprocess.run([sys.executable,"-B",*args],check=True)
