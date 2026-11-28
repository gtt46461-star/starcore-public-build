#!/usr/bin/env python3
"""Restore the byte-exact original delivery from verified repository parts.

Standard-library only. No network, Git credentials, source edits, or ZIP extraction.
Refuses to overwrite an existing file with different bytes.
"""
from __future__ import annotations
import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import tempfile

PACKAGE_BYTES = 20814251
PACKAGE_SHA256 = "4770e865a14cb3c4411e24d180368077c4585cdaa40255945a72742d615f9915"
PACKAGE_NAME = "STARCORE_R34_4_211_20261128_Repair_Plan.zip"
PART_BYTES = 786432
PART_COUNT = 27


def require(condition, message):
    if not condition:
        raise ValueError(message)


def digest_file(path):
    h = hashlib.sha256()
    size = 0
    with path.open("rb") as stream:
        while data := stream.read(1024 * 1024):
            size += len(data)
            h.update(data)
    return size, h.hexdigest()


def no_duplicates(pairs):
    result = {}
    for key, value in pairs:
        require(key not in result, "duplicate manifest key: " + key)
        result[key] = value
    return result


def restore(package_dir, output):
    package_dir = package_dir.resolve()
    manifest = json.loads((package_dir / "PACKAGE_PARTS.json").read_text(encoding="utf-8"), object_pairs_hook=no_duplicates)
    require(manifest["schema"] == "STARCORE_COMPLETE_PACKAGE_PARTS_1", "wrong package schema")
    require((manifest["filename"], manifest["bytes"], manifest["sha256"], manifest["part_bytes"]) ==
            (PACKAGE_NAME, PACKAGE_BYTES, PACKAGE_SHA256, PART_BYTES), "unexpected package identity")
    parts = manifest["parts"]
    require(len(parts) == PART_COUNT, "wrong part count")
    for number, part in enumerate(parts, 1):
        expected_size = min(PART_BYTES, PACKAGE_BYTES - (number - 1) * PART_BYTES)
        require(part["part"] == number and part["path"] == f"parts/part-{number:04d}.bin", "wrong part order/path")
        require(part["bytes"] == expected_size, "wrong part length")
        require(re.fullmatch("[a-f0-9]{64}", part["sha256"]) is not None, "invalid part SHA256")
        require(re.fullmatch("[a-f0-9]{40}", part["git_blob_sha"]) is not None, "invalid Git blob SHA")
    output = Path(output).absolute()
    require(not output.is_symlink(), "refusing symlink output")
    output.parent.mkdir(parents=True, exist_ok=True)
    if output.exists():
        require(output.is_file() and digest_file(output) == (PACKAGE_BYTES, PACKAGE_SHA256),
                "existing output differs; choose a new path instead of overwriting it")
        return {"status": "VERIFIED_EXISTING", "path": str(output), "bytes": PACKAGE_BYTES,
                "sha256": PACKAGE_SHA256, "parts_rechecked": False}
    temporary = None
    try:
        with tempfile.NamedTemporaryFile(prefix=".starcore-restore-", dir=output.parent, delete=False) as stream:
            temporary = Path(stream.name)
            total = hashlib.sha256()
            total_bytes = 0
            for part in parts:
                path = package_dir / part["path"]
                require(path.is_file() and not path.is_symlink() and path.resolve().is_relative_to(package_dir),
                        "missing/unsafe part: " + part["path"])
                part_sha = hashlib.sha256()
                git_sha = hashlib.sha1(f"blob {part['bytes']}\0".encode("ascii"))
                size = 0
                with path.open("rb") as source:
                    while data := source.read(256 * 1024):
                        size += len(data)
                        require(size <= part["bytes"], "oversized part: " + part["path"])
                        part_sha.update(data)
                        git_sha.update(data)
                        total.update(data)
                        total_bytes += len(data)
                        stream.write(data)
                require((size, part_sha.hexdigest(), git_sha.hexdigest()) ==
                        (part["bytes"], part["sha256"], part["git_blob_sha"]), "corrupt part: " + part["path"])
            require((total_bytes, total.hexdigest()) == (PACKAGE_BYTES, PACKAGE_SHA256), "complete package mismatch")
            stream.flush()
            os.fsync(stream.fileno())
        # Hard-link creation is atomic and cannot replace a file created concurrently.
        os.link(temporary, output)
        return {"status": "RESTORED_AND_VERIFIED", "path": str(output), "bytes": total_bytes,
                "sha256": total.hexdigest(), "parts_verified": PART_COUNT}
    finally:
        if temporary is not None:
            temporary.unlink(missing_ok=True)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--output", type=Path, default=Path.cwd() / PACKAGE_NAME)
    args = parser.parse_args()
    try:
        result = restore(Path(__file__).resolve().parent, args.output)
    except (OSError, ValueError, KeyError, TypeError) as error:
        parser.exit(1, f"RESTORE_FAILED: {type(error).__name__}: {error}\n")
    print(json.dumps(result, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
