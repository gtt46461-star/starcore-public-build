"""Prepare release identity and evidence bindings for the reviewed report changes."""
from pathlib import Path
import hashlib
import json
import os
import subprocess
import sys
from shutil import copyfile

parent = "a5b4c26275e7ce74b6a178fe3bbc8ea861a80b5e"
source = os.environ["REPAIR_INPUT"]
paths = [
  "app/src/main/java/com/ethk3/novaonepro/MainActivity.java",
  "app/src/main/java/com/ethk3/novaonepro/CompleteReportFileR927.java",
  "app/src/main/java/com/ethk3/novaonepro/ReportCatalogCP4.java",
  "app/src/main/java/com/ethk3/novaonepro/ReportDiagnosticEvidenceR64.java",
  "app/src/main/java/com/ethk3/novaonepro/ReportMetadataStreamR81.java"
]
test = "app/src/androidTest/java/com/ethk3/novaonepro/Field173B12ReportTest.java"
reason = "The requested export is the compact compressed diagnostic ZIP. Use the user-selected document as a durable destination for the compact profile. Reuse complete table definitions and counts from the same immutable catalog, skip empty groups, and preserve the original 96 checks, grouped states and sampled metadata. The 173 field ZIP is the independent comparison input."
def git(*args):
    return subprocess.check_output(["git",*args])
def sha(raw):
    return hashlib.sha256(raw).hexdigest()
def document(path,data):
    Path(path).write_text(json.dumps(data,indent=2,sort_keys=True,ensure_ascii=False)+"\n")
assert git("rev-parse","HEAD").decode().strip() == parent
for path in paths + [test]:
    raw=git("show",source+":"+path)
    assert raw != Path(path).read_bytes(), path
    Path(path).write_bytes(raw)
manifest = Path("release/release_manifest.json")
r=json.loads(manifest.read_text())
assert r["versionCode"]==577311205
r["versionCode"]+=1
r["versionName"]=r["versionName"].replace("577311205","577311206")
document(manifest,r)
subprocess.run([sys.executable,"scripts/r926_release_manifest.py","--write"],check=True)
sys.path.insert(0,str(Path("scripts").resolve()))
import r967_semantic_identity as sem
classified={e["path"]:e for e in json.loads(Path(sem.MANIFEST).read_text())["files"]}
proof=Path("release/r82_training_invalidation.json")
v=json.loads(proof.read_text())
allowed=set(paths) | {"app/src/main/java/com/ethk3/novaonepro/ReleaseManifestR926.java","release/release_manifest.json","release/release_identity.properties"}
updated=[]
for record in v["controlled_changes"]:
    path=record["path"]
    actual=sha(Path(path).read_bytes()) if record["after_role"] else ""
    if record["after_sha256"]!=actual:
        assert path in allowed,path
        record["after_sha256"]=actual
        record["reviewer"]="STARCORE B12 report delivery correction"
        record["rationale"]=reason if path in paths else "Regenerated the version identity for the reviewed report-delivery correction."
        updated.append(path)
assert set(paths)<=set(updated)
provenance=[[path,sha(Path(path).read_bytes())] for path,entry in sorted(classified.items()) if entry["role"]!="PROVENANCE"]
v["reviewed_input_provenance_sha256"]=sha(sem.canonical(provenance).encode())
document(proof,v)
for args in (["scripts/r967_semantic_identity.py"],["scripts/r967_semantic_identity.py","--check"],["scripts/r926_release_manifest.py","--check"]):
    subprocess.run([sys.executable,"-B",*args],check=True)
index=Path("verification/r343/corrections/INDEX.json")
records=json.loads(index.read_text())
base=index.parent
sub=base/"B12-compact-save-206"
sub.mkdir()
probe=sub/"scope_probe.py"
probe.write_bytes(git("show",source+":engineering/report206/scope_probe.py"))
measured=subprocess.check_output([sys.executable,str(probe)])
(sub/"scope-measurement.json").write_bytes(measured)
(sub/"production.patch").write_bytes(git("diff","--binary",parent,"--",*paths))
for path in paths:
    before=git("show",parent+":"+path)
    after=Path(path).read_bytes()
    previous=sub/(Path(path).name+".before.snapshot")
    current=sub/(Path(path).name+".snapshot")
    previous.write_bytes(before)
    current.write_bytes(after)
    originals={str(f.relative_to(base)):sha(f.read_bytes()) for f in [previous,current,probe,sub/"scope-measurement.json",sub/"production.patch"]}
    records["reviews"].append(dict(path=path,batch="B12",issue="R343-044",role=classified[path]["role"],
        beforeSha256=sha(before),afterSha256=sha(after),afterFile=str(current.relative_to(base)),originals=originals,reason=reason))
document(index,records)
print(measured.decode(),flush=True)
