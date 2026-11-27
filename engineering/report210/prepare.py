"""Prepare release identity and evidence bindings for the reviewed report changes."""
from pathlib import Path
import hashlib
import json
import os
import subprocess
import sys

parent = "ab2695b1b7e764fe4fd057183d2007f35a720a63"
source = os.environ["REPAIR_INPUT"]
paths = [
  "app/src/main/java/com/ethk3/novaonepro/UnifiedSoftwareStatusR923.java",
  "app/src/main/java/com/ethk3/novaonepro/ResearchDashboardR924.java",
  "app/src/main/java/com/ethk3/novaonepro/ReportCheckRegistryCP12.java",
  "app/src/main/java/com/ethk3/novaonepro/ReportReadContextCP4.java",
  "app/src/main/java/com/ethk3/novaonepro/ReportExportServiceCP4.java",
  "app/src/main/java/com/ethk3/novaonepro/CompleteReportFileR927.java"
]
reason = ("Render the R923 overview to its final report output, avoiding intermediate "
          "copies, and reuse immutable R924 dashboard sections in both checks. "
          "Preserve check contents, source queries and saved ZIP verification.")
def git(*args):
    return subprocess.check_output(["git",*args])
def sha(raw):
    return hashlib.sha256(raw).hexdigest()
def document(path,data):
    Path(path).write_text(json.dumps(data,indent=2,sort_keys=True,ensure_ascii=False)+"\n")
assert git("rev-parse","HEAD").decode().strip() == parent
for path in paths:
    raw=git("show",source+":"+path)
    assert raw != Path(path).read_bytes(), path
    Path(path).write_bytes(raw)
manifest = Path("release/release_manifest.json")
r=json.loads(manifest.read_text())
assert r["versionCode"]==577311209
r["versionCode"]+=1
r["versionName"]=r["versionName"].replace("577311209","577311210")
document(manifest,r)
subprocess.run([sys.executable,"scripts/r926_release_manifest.py","--write"],check=True)
sys.path.insert(0,str(Path("scripts").resolve()))
import r967_semantic_identity as sem
classified={e["path"]:e for e in json.loads(Path(sem.MANIFEST).read_text())["files"]}
proof=Path("release/r82_training_invalidation.json")
v=json.loads(proof.read_text())
allowed=set(paths) | {"app/src/main/java/com/ethk3/novaonepro/ReleaseManifestR926.java","release/release_manifest.json","release/release_identity.properties"}
baseline={row["path"]:row for row in json.loads(Path("release/r81_controlled_source_baseline.json").read_text())["files"]}
review_by_path={entry["path"]:entry for entry in v["controlled_changes"]}
reviewer="STARCORE B12 RSEC-010 report correction"
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
                                  "Regenerated the version identity for the reviewed report export correction.")
v["controlled_changes"]=list(review_by_path.values())
provenance=[[path,sha(Path(path).read_bytes())] for path,entry in sorted(classified.items()) if entry["role"]!="PROVENANCE"]
v["reviewed_input_provenance_sha256"]=sha(sem.canonical(provenance).encode())
document(proof,v)
for args in (["scripts/r967_semantic_identity.py"],["scripts/r967_semantic_identity.py","--check"],["scripts/r926_release_manifest.py","--check"]):
    subprocess.run([sys.executable,"-B",*args],check=True)
index=Path("verification/r343/corrections/INDEX.json")
records=json.loads(index.read_text())
base=index.parent
sub=base/"B12-rsec010-report-210"
sub.mkdir()
(sub/"production.patch").write_bytes(git("diff","--binary",parent,"--",*paths))
for path in paths:
    before=git("show",parent+":"+path)
    after=Path(path).read_bytes()
    previous=sub/(Path(path).name+".before.snapshot")
    current=sub/(Path(path).name+".snapshot")
    previous.write_bytes(before)
    current.write_bytes(after)
    originals={str(f.relative_to(base)):sha(f.read_bytes()) for f in [previous,current,sub/"production.patch"]}
    records["reviews"].append(dict(path=path,batch="B12",issue="R343-044",role=classified[path]["role"],
        beforeSha256=sha(before),afterSha256=sha(after),afterFile=str(current.relative_to(base)),originals=originals,reason=reason))
document(index,records)
print("RSEC-010 report source and version bindings prepared",flush=True)
