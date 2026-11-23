#!/usr/bin/env python3
"""Verify a received R34 source bundle and archive against the reviewed commit."""
import argparse
import json
from pathlib import Path
import shutil
import sys
import tempfile
import zipfile

sys.path.insert(0, str(Path(__file__).resolve().parents[1] / "r33"))
import package_ci_candidate as package
import verify_plan_binding as binding


def verify(archive_path, repo, commit, scratch):
    tree = binding.git(repo, "rev-parse", commit + "^{tree}").decode().strip()
    scope = package.source_tree_entries(repo, commit)
    with tempfile.TemporaryDirectory(prefix="r34-source-check-", dir=scratch) as temporary:
        work = Path(temporary)
        with zipfile.ZipFile(archive_path) as received:
            delivery = json.loads(received.read("DELIVERY.json"))
            package.require(delivery["sourceCommit"] == commit and delivery["sourceTree"] == tree,
                            "R34_RECEIVED_SOURCE_IDENTITY")
            for suffix in ("-source.zip", "-source.bundle"):
                names = [name for name in received.namelist() if name.endswith(suffix)]
                package.require(len(names) == 1 and "/" not in names[0], "R34_SOURCE_MEMBER_CARDINALITY")
                target = work / ("archive.zip" if suffix.endswith(".zip") else "original.bundle")
                with target.open("xb") as output, received.open(names[0]) as content:
                    shutil.copyfileobj(content, output, length=1 << 20)
                if suffix.endswith(".zip"):
                    package.verify_source_archive(target, scope)
                    target.unlink()
        empty = work / "empty.git"
        empty.mkdir()
        binding.git(empty, "init", "--bare", "--quiet")
        bundle = str(work / "original.bundle")
        binding.git(empty, "bundle", "verify", bundle)
        binding.git(empty, "fetch", "--quiet", "--no-tags", bundle, "HEAD")
        package.require(binding.git(empty, "rev-parse", "FETCH_HEAD").decode().strip() == commit,
                        "R34_RECOVERED_COMMIT")
        package.require(binding.git(empty, "rev-parse", "FETCH_HEAD^{tree}").decode().strip() == tree,
                        "R34_RECOVERED_TREE")
        binding.git(empty, "merge-base", "--is-ancestor",
                    "273641e838e0b8dd842b42a67a674e184942a30a", "FETCH_HEAD")
    return {"status": "VERIFIED", "sourceCommit": commit, "sourceTree": tree,
            "sourceZipMembers": len(scope), "restoredFromEmptyRepository": True,
            "scope": "EXACT_SOURCE_BYTES_AND_HISTORY"}


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--archive", type=Path, required=True)
    parser.add_argument("--repo", type=Path, required=True)
    parser.add_argument("--commit", required=True)
    parser.add_argument("--scratch", type=Path)
    args = parser.parse_args()
    print(json.dumps(verify(args.archive, args.repo.resolve(), args.commit, args.scratch), indent=2))


if __name__ == "__main__":
    main()
