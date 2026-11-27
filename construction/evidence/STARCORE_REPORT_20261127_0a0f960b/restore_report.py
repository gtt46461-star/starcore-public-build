#!/usr/bin/env python3
"""Restore the original diagnostic ZIP and verify every part and the final SHA-256."""
import hashlib
import json
import os
from pathlib import Path
import tempfile

base = Path(__file__).resolve().parent
manifest = json.loads((base / "REPORT_MANIFEST.json").read_text(encoding="utf-8"))
target = base / "raw" / manifest["original_filename"]

def checksum(path):
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for block in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(block)
    return digest.hexdigest()

if target.exists():
    if target.stat().st_size != manifest["original_size_bytes"] or checksum(target) != manifest["original_sha256"]:
        raise SystemExit("An existing output file differs from the original; no file was replaced.")
    print("Verified original ZIP:", target)
    raise SystemExit(0)

fd, temporary_name = tempfile.mkstemp(prefix=".restore-", suffix=".tmp", dir=target.parent)
temporary = Path(temporary_name)
try:
    with os.fdopen(fd, "wb") as output:
        for part in manifest["parts"]:
            path = (base / part["path"]).resolve()
            if not path.is_relative_to(base):
                raise SystemExit("Invalid part path")
            if path.stat().st_size != part["size_bytes"] or checksum(path) != part["sha256"]:
                raise SystemExit("Part verification failed: " + part["path"])
            with path.open("rb") as stream:
                for block in iter(lambda: stream.read(1024 * 1024), b""):
                    output.write(block)
    if temporary.stat().st_size != manifest["original_size_bytes"] or checksum(temporary) != manifest["original_sha256"]:
        raise SystemExit("Restored ZIP verification failed")
    os.replace(temporary, target)
    print("Restored and verified original ZIP:", target)
finally:
    if temporary.exists():
        temporary.unlink()
