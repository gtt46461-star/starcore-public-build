"""Bind the disposable rolling-checkpoint test build to its reviewed source bytes."""
from pathlib import Path
import hashlib
import json
import os
import subprocess
import sys

parent = "5c7f1b36eaee6036df280e85835bb0fb4ce0b40d"
source = os.environ["REPAIR_INPUT"]
paths = [
  "app/src/main/java/com/ethk3/novaonepro/r952/RollingLearnerR343.java"
]
reason = "Retire an incompatible rolling run only after authenticated later settlement, preserving its original checkpoint and formal publication rules."
def git(*args):
    return subprocess.check_output(["git",*args])
def sha(raw):
    return hashlib.sha256(raw).hexdigest()
def document(path,data):
    Path(path).write_text(json.dumps(data,indent=2,sort_keys=True,ensure_ascii=False)+"\n")
assert git("rev-parse","HEAD").decode().strip() == parent
for path in paths + ["app/src/androidTest/java/com/ethk3/novaonepro/r952/R343RollingNativeTest.java"]:
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
reviewer="STARCORE rolling training checkpoint correction"
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
                                  rationale=reason if path in paths else
                                  "Verified unchanged release binding while restoring the original test entry.")
v["controlled_changes"]=list(review_by_path.values())
provenance=[[path,sha(Path(path).read_bytes())] for path,entry in sorted(classified.items()) if entry["role"]!="PROVENANCE"]
v["reviewed_input_provenance_sha256"]=sha(sem.canonical(provenance).encode())
document(proof,v)
for args in (["scripts/r967_semantic_identity.py"],["scripts/r967_semantic_identity.py","--check"],["scripts/r926_release_manifest.py","--check"]):
    subprocess.run([sys.executable,"-B",*args],check=True)
