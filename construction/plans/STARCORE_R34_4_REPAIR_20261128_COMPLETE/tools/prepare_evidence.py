#!/usr/bin/env python3
"""Materialize the exact original diagnostic from the verified repository package.

Only the pinned ZIP member is read. No generic extractall, network or application edits.
The immutable original package and existing differing evidence are never overwritten.
"""
from __future__ import annotations
import argparse
import hashlib
import json
import os
from pathlib import Path
import subprocess
import sys
import tempfile
import zipfile

EXPECTED_BYTES = 19471817
EXPECTED_SHA256 = "e0e3ffdb5a58a33175d6ee305d788bfaea3bd6186495c1828a0e1edddb6112b0"
MEMBER = "STARCORE_R34_4_211_Delivery/complete_plan/evidence/ORIGINAL_DIAGNOSTIC_211.zip"
PACKAGE_RELATIVE = "construction/packages/STARCORE_R34_4_211_20261128"


def digest_file(path):
    h = hashlib.sha256()
    size = 0
    with path.open("rb") as stream:
        while data := stream.read(1024 * 1024):
            size += len(data)
            h.update(data)
    return size, h.hexdigest()


def prepare(repo_root):
    plan = Path(__file__).resolve().parents[1]
    evidence = plan / "evidence/ORIGINAL_DIAGNOSTIC_211.zip"
    if evidence.is_symlink():
        raise ValueError("refusing symlink evidence")
    if evidence.exists():
        if not evidence.is_file() or digest_file(evidence) != (EXPECTED_BYTES, EXPECTED_SHA256):
            raise ValueError("existing diagnostic differs; retain it and investigate, do not overwrite")
        return {"status": "VERIFIED_EXISTING", "path": str(evidence), "bytes": EXPECTED_BYTES,
                "sha256": EXPECTED_SHA256}
    package = repo_root.resolve() / PACKAGE_RELATIVE
    script = package / "restore_package.py"
    if not script.is_file():
        raise ValueError("package restore script missing; use a complete checkout or specify --repo-root")
    evidence.parent.mkdir(parents=True, exist_ok=True)
    with tempfile.TemporaryDirectory(prefix="starcore-evidence-") as temporary_dir:
        archive = Path(temporary_dir) / "delivery.zip"
        subprocess.run([sys.executable, str(script), "--output", str(archive)], check=True, stdout=subprocess.PIPE, text=True)
        with zipfile.ZipFile(archive) as zipped:
            members = [item for item in zipped.infolist() if item.filename == MEMBER]
            if len(members) != 1 or members[0].file_size != EXPECTED_BYTES:
                raise ValueError("missing/duplicate/wrong-sized original diagnostic member")
            temporary = None
            try:
                with tempfile.NamedTemporaryFile(prefix=".diagnostic-", dir=evidence.parent, delete=False) as output:
                    temporary = Path(output.name)
                    size = 0
                    digest = hashlib.sha256()
                    with zipped.open(members[0]) as source:
                        while data := source.read(1024 * 1024):
                            size += len(data)
                            if size > EXPECTED_BYTES:
                                raise ValueError("original diagnostic too large")
                            digest.update(data)
                            output.write(data)
                    if (size, digest.hexdigest()) != (EXPECTED_BYTES, EXPECTED_SHA256):
                        raise ValueError("original diagnostic length/SHA mismatch")
                    output.flush()
                    os.fsync(output.fileno())
                os.link(temporary, evidence)
            finally:
                if temporary is not None:
                    temporary.unlink(missing_ok=True)
    return {"status": "MATERIALIZED_AND_VERIFIED", "path": str(evidence), "bytes": EXPECTED_BYTES,
            "sha256": EXPECTED_SHA256, "source": PACKAGE_RELATIVE}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--repo-root", type=Path, default=Path(__file__).resolve().parents[4])
    args = parser.parse_args()
    try:
        result = prepare(args.repo_root)
    except (OSError, ValueError, zipfile.BadZipFile, subprocess.CalledProcessError) as error:
        parser.exit(1, f"PREPARE_EVIDENCE_FAILED: {type(error).__name__}: {error}\n")
    print(json.dumps(result, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
