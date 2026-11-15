#!/usr/bin/env python3
"""Verify source delivery, exact patch application and evidence labels.

PASS means the package's declared source bytes and documents are consistent.
It does not mean Android behavior, all repair designs, or natural quality pass.
"""
import argparse
import collections
import hashlib
import importlib.util
import json
import pathlib
import re
import subprocess
import tempfile
import zipfile


def sha(data):
    return hashlib.sha256(data).hexdigest()


def read(path):
    return json.loads(path.read_text(encoding="utf-8"))


def require(condition, label):
    if not condition:
        raise ValueError(label)


def safe_name(name):
    rel = pathlib.PurePosixPath(name)
    require(not rel.is_absolute() and ".." not in rel.parts and bool(rel.parts), "unsafe path: " + name)
    return rel


def safe(root, name):
    p = root.joinpath(*safe_name(name).parts)
    require(p.resolve().is_relative_to(root.resolve()), "symlink escape: " + name)
    return p


def zip_entries(path):
    with zipfile.ZipFile(path) as z:
        require(z.testzip() is None, "ZIP CRC failure: " + path.name)
        names = [x.filename for x in z.infolist() if not x.is_dir()]
        require(len(names) == len(set(names)), "duplicate ZIP path: " + path.name)
        result = {}
        for name in names:
            safe_name(name)
            info = z.getinfo(name)
            require((info.external_attr >> 16) & 0o170000 != 0o120000, "symlink in source ZIP: " + name)
            result[name] = z.read(name)
        return result


def verify(package, out):
    manifest = read(package / "code_manifest.json")
    reference = package / "reference/R32.1"
    cards = read(reference / "contracts/repair_cards.json")
    owners = {r["path"]: r["owner"] for r in read(reference / "ledger/file_owners.json")}
    contracts = read(reference / "contracts/batches.json")
    original_batches = {r["id"]: r for r in contracts}
    batches = manifest["batches"]
    order = list(batches)
    require(order == [r["id"] for r in contracts], "batch order differs from repair contract")
    ancestors = {}
    entries = {}
    states = collections.Counter()
    for i, (bid, batch) in enumerate(batches.items()):
        require(batch["prerequisites"] == original_batches[bid]["prerequisites"], "batch prerequisites drift: " + bid)
        require(set(batch["prerequisites"]) <= set(order[:i]), "cycle/future prerequisite: " + bid)
        ancestors[bid] = set(batch["prerequisites"])
        for prev in batch["prerequisites"]:
            ancestors[bid].update(ancestors[prev])
        for row in batch["files"]:
            path = row["path"]
            safe_name(path)
            require(path not in entries, "multiple file writers: " + path)
            require(row["owner"] == bid, "row owner differs from batch: " + path)
            if path in owners:
                require(owners[path] == bid, "existing authoritative owner drift: " + path)
            source = safe(package, row["source"])
            require(source.is_file(), "missing complete post-state code: " + path)
            require(sha(source.read_bytes()) == row["after_sha256"], "post-state code SHA mismatch: " + path)
            require(row["action"] in {"ADD", "REPLACE", "PRESERVE_REFERENCE"}, "unknown action: " + path)
            require(bool(row.get("implementation_assessment")), "missing code assessment: " + path)
            if row["action"] == "PRESERVE_REFERENCE":
                require(row["before_sha256"] == row["after_sha256"], "preserved source secretly changed: " + path)
                require(row["implementation_assessment"] == "EXISTING_SOURCE_DELIVERED_NOT_NEW_IMPLEMENTATION", "preservation falsely labeled new implementation: " + path)
            else:
                require(row["before_sha256"] != row["after_sha256"], "no-op counted as code modification: " + path)
                require(row["implementation_assessment"] == "MODIFIED_SOURCE_TEST_SCOPE_IN_VERIFICATION", "changed source labeled complete without scoped proof: " + path)
            states[row["action"]] += 1
            entries[path] = row
    require(read(package / "contracts/file_actions.json") == list(entries.values()), "flattened action list differs from authoritative manifest")
    declared_writes = set()
    doc_count = 0
    source_links = set()
    for card in cards:
        cid, batch = card["id"], card["batch"]
        declared_writes.update(card["writes"])
        for path in card["writes"]:
            require(path in entries and entries[path]["owner"] == batch, "missing/wrong declared writer: " + cid + " " + path)
        for path in card["reads"]:
            require(path in entries and entries[path]["owner"] in ancestors[batch], "read needs future producer: " + cid + " " + path)
        for path in card["files"] + card["traceOnly"]:
            require(path in entries, "card file has no complete code: " + cid + " " + path)
            stem = sha(path.encode())[:12]
            doc = package / "issues" / cid / (stem + ".md")
            require(doc.is_file(), "missing per-card per-file document: " + cid + " " + path)
            text = doc.read_text(encoding="utf-8")
            require(path in text and entries[path]["after_sha256"] in text and entries[path]["action"] in text, "file document identity/hash/action mismatch: " + cid + " " + path)
            require(("../../" + entries[path]["source"]) in text, "file document lacks authoritative source link: " + cid + " " + path)
            doc_count += 1
            source_links.add(path)
        require((package / "issues" / cid / "README.md").is_file(), "missing issue README: " + cid)
        page = package / "issues" / cid / "index.html"
        require(page.is_file(), "missing complete-code issue HTML: " + cid)
        html = page.read_text(encoding="utf-8")
        for path in card["files"] + card["traceOnly"]:
            require(entries[path]["source"] in html, "issue HTML lacks complete source: " + cid + " " + path)
    baseline = zip_entries(package / "SOURCE_BASELINE.zip")
    candidate = zip_entries(package / "SOURCE_CANDIDATE.zip")
    changed_archived = {p for p in baseline.keys() | candidate.keys() if baseline.get(p) != candidate.get(p)}
    for path, row in entries.items():
        require(path in candidate, "candidate source ZIP omitted declared code: " + path)
        require(sha(candidate[path]) == row["after_sha256"], "candidate ZIP code mismatch: " + path)
        if row["before_sha256"] is None:
            require(path not in baseline and row["action"] == "ADD", "ADD preimage already exists: " + path)
        else:
            require(path in baseline and sha(baseline[path]) == row["before_sha256"], "baseline ZIP code mismatch: " + path)
        if row["action"] == "REPLACE":
            before = safe(package, "code/" + row["owner"] + "/before/" + path)
            require(before.is_file() and sha(before.read_bytes()) == row["before_sha256"], "replacement lacks complete preimage: " + path)
    declared_changes = {p for p, row in entries.items() if row["action"] != "PRESERVE_REFERENCE"}
    require(changed_archived == declared_changes, "baseline/candidate changes differ from declared actions: " + repr(sorted(changed_archived ^ declared_changes)))
    roundtrip = []
    with tempfile.TemporaryDirectory(prefix="starcore-code-package-verify-") as temp:
        root = pathlib.Path(temp)
        source_tree = root / "source"
        source_tree.mkdir()
        for path, data in baseline.items():
            target = safe(source_tree, path)
            target.parent.mkdir(parents=True, exist_ok=True)
            target.write_bytes(data)
        patches = []
        for bid in order:
            patch = package / "patches" / (bid + ".patch")
            require(patch.is_file(), "missing batch diff: " + bid)
            paths = re.findall(r"(?m)^diff --git a/(.+?) b/(.+?)$", patch.read_text(encoding="utf-8"))
            declared = {r["path"] for r in batches[bid]["files"] if r["action"] != "PRESERVE_REFERENCE"}
            require(len(paths) == len({b for a, b in paths}) and {b for a, b in paths} == declared and all(a == b for a, b in paths), "batch patch scope differs from code manifest: " + bid)
            if not declared:
                require(patch.stat().st_size == 0, "nonempty no-change patch: " + bid)
                continue
            for flags in (["--check"], []):
                run = subprocess.run(["git", "apply", *flags, str(patch)], cwd=source_tree, text=True, capture_output=True)
                require(run.returncode == 0, "patch apply failed " + bid + ": " + run.stderr)
            for row in batches[bid]["files"]:
                require(sha(safe(source_tree, row["path"]).read_bytes()) == row["after_sha256"], "post-apply code differs from manifest: " + row["path"])
            patches.append((bid, patch))
            roundtrip.append(bid)
        require({p: safe(source_tree, p).read_bytes() for p in candidate} == candidate, "fully patched source differs from complete candidate ZIP")
        for bid, patch in reversed(patches):
            for flags in (["--reverse", "--check"], ["--reverse"]):
                run = subprocess.run(["git", "apply", *flags, str(patch)], cwd=source_tree, text=True, capture_output=True)
                require(run.returncode == 0, "patch reverse failed " + bid + ": " + run.stderr)
        require(all(safe(source_tree, p).read_bytes() == b for p, b in baseline.items()), "reverse patches did not restore baseline")
        require(all(not safe(source_tree, p).exists() for p in candidate.keys() - baseline.keys()), "reverse patches left added source")
        # Exercise the actual byte-applying tool in a second independent baseline.
        apply_tree = root / "apply"
        apply_tree.mkdir()
        for path, data in baseline.items():
            target = safe(apply_tree, path)
            target.parent.mkdir(parents=True, exist_ok=True)
            target.write_bytes(data)
        spec = importlib.util.spec_from_file_location("verified_apply_batch", package / "tools/apply_batch.py")
        module = importlib.util.module_from_spec(spec)
        spec.loader.exec_module(module)
        for bid in order:
            check = module.execute(package, apply_tree, bid)
            require(check["scope"] == "SOURCE_BYTES_ONLY_NOT_BUSINESS_ACCEPTANCE", "byte check masquerades as business acceptance")
            applied = module.execute(package, apply_tree, bid, True)
            require(applied["scope"] == "SOURCE_BYTES_ONLY_NOT_BUSINESS_ACCEPTANCE", "source receipt masquerades as business acceptance")
            again = module.execute(package, apply_tree, bid, True)
            require(again["changed_files"] == 0, "batch application not idempotent: " + bid)
        require(all(safe(apply_tree, p).read_bytes() == b for p, b in candidate.items()), "byte-applying tool differs from candidate source")
    result = {
        "schema_version": 1,
        "status": "PASS",
        "scope": "SOURCE_PACKAGE_CONSISTENCY_ONLY",
        "runtime_or_design_completion_proven": False,
        "natural_quality_proven": False,
        "code_manifest_sha256": sha((package / "code_manifest.json").read_bytes()),
        "batches": len(order),
        "issues": len(cards),
        "declared_write_paths": len(declared_writes),
        "complete_code_paths": len(entries),
        "source_actions": dict(states),
        "per_card_file_documents": doc_count,
        "unique_per_card_source_paths": len(source_links),
        "baseline_archive_files": len(baseline),
        "candidate_archive_files": len(candidate),
        "patch_roundtrip_batches": roundtrip,
        "batch_application_idempotence": True,
        "source_copy_counted_as_new_implementation": False,
        "runtime_tests_marked_pass_by_this_audit": False,
    }
    if out:
        out.parent.mkdir(parents=True, exist_ok=True)
        out.write_text(json.dumps(result, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    return result


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--package", required=True, type=pathlib.Path)
    parser.add_argument("--out", type=pathlib.Path)
    args = parser.parse_args()
    try:
        print(json.dumps(verify(args.package.resolve(), args.out), ensure_ascii=False, indent=2))
    except Exception as error:
        print(json.dumps({"status": "FAIL", "scope": "SOURCE_PACKAGE_CONSISTENCY_ONLY", "error": str(error)}, ensure_ascii=False))
        raise SystemExit(2)
