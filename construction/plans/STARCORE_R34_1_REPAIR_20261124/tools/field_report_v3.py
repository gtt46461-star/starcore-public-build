#!/usr/bin/env python3
"""Verify the field report against its sealed identity and optionally extract it locally."""
import argparse
import hashlib
import io
import json
import os
from pathlib import Path
import stat
import tempfile
import zipfile

OUTER_SHA256 = "fb499fc609435be82591ea015239fbee2b7da5c0dba772dd028d1417f1b8784c"
JOB = "4bef989d7e71259a0793ffc8f51871d62b808d770b4aeb834ca065c788c984eb"


def check(condition, message):
    if not condition:
        raise ValueError(message)


def sha(data):
    return hashlib.sha256(data).hexdigest()


def read_unique_object(pairs):
    obj = {}
    for name, value in pairs:
        check(name not in obj, "Repeated JSON key: " + name)
        obj[name] = value
    return obj


def decode(data):
    return json.loads(data, object_pairs_hook=read_unique_object)


def file_infos(archive):
    result = {}
    for info in archive.infolist():
        name = info.filename
        check(name not in result, "Repeated ZIP member: " + name)
        check(info.flag_bits & 1 == 0, "Encrypted ZIP member")
        check(not stat.S_ISLNK(info.external_attr >> 16), "ZIP symbolic link")
        result[name] = info
    return result


def regular_name(name):
    return (isinstance(name, str) and name not in {"", ".", ".."} and
            not any(c in name for c in ("/", "\\", ":", "\x00")))


def verify_report(input_path, identity_path, output_dir=None):
    identity = decode(identity_path.read_bytes())
    check(identity["schema"] == "R341_FIELD_REPORT_IDENTITY_3" and
          identity["job_id"] == JOB and identity["outer"]["sha256"] == OUTER_SHA256,
          "Identity sidecar belongs to another report")
    check(input_path.stat().st_size == identity["outer"]["bytes"], "Outer ZIP size differs")
    data = input_path.read_bytes()
    check(sha(data) == OUTER_SHA256, "Outer ZIP SHA-256 differs")
    stem = "STARCORE-" + JOB + "/"
    with zipfile.ZipFile(io.BytesIO(data)) as outer:
        files = file_infos(outer)
        check(set(files) == {stem, stem + "index.json", stem + "STARCORE_FULL_REPORT.zip"} and
              files[stem].is_dir(), "Unexpected outer ZIP structure")
        inner_bytes = outer.read(stem + "STARCORE_FULL_REPORT.zip")
        index = decode(outer.read(stem + "index.json"))
        check(outer.testzip() is None, "Outer CRC mismatch")
    inner = identity["inner"]
    check(len(inner_bytes) == inner["bytes"] and sha(inner_bytes) == inner["sha256"], "Inner ZIP digest differs")
    check(index["jobId"] == JOB and len(index["files"]) == 1 and index["allChecksPassed"] is False and
          index["checksComplete"] is True and index["evidenceComplete"] is True and
          index["files"][0]["sha256"] == inner["sha256"] and index["files"][0]["bytes"] == inner["bytes"] and
          index["files"][0]["name"] == "STARCORE_FULL_REPORT.zip", "Original delivery index disagrees")
    expected = identity["members"]
    check(len(expected) == identity["member_count"] == 57, "Invalid member count")
    names = {row["name"] for row in expected}
    check(len(names) == 57 and all(regular_name(n) for n in names), "Duplicate or nested member")
    with zipfile.ZipFile(io.BytesIO(inner_bytes)) as archive:
        files = file_infos(archive)
        check(set(files) == names, "Unexpected or missing inner members")
        for row in expected:
            name = row["name"]
            info = files[name]
            check(info.file_size == row["bytes"] and f"{info.CRC:08x}" == row["crc32"], "ZIP metadata differs: " + name)
            # read() additionally enforces ZIP CRC over the decompressed bytes.
            check(sha(archive.read(name)) == row["sha256"], "Member SHA-256 differs: " + name)
        raw_manifest = archive.read("ARCHIVE_MANIFEST.jsonl")
        check(sha(raw_manifest) == index["manifestSha256"] == identity["archive_manifest_sha256"], "Archive manifest mismatch")
        lines = raw_manifest.splitlines(keepends=True)
        records = [decode(line) for line in lines]
        check(records[0]["kind"] == "header" and records[0]["report_id"] == JOB and records[-1]["kind"] == "footer" and
              records[-1]["manifest_body_sha256"] == sha(b"".join(lines[:-1])), "Archive manifest body mismatch")
        mem = [r for r in records if r["kind"] == "member"]
        expanded = {r["name"]: (r["raw_bytes"], r["raw_sha256"]) for r in mem}
        wanted = {r["name"]: (r["bytes"], r["sha256"]) for r in expected if r["name"] != "ARCHIVE_MANIFEST.jsonl"}
        check(records[-1]["members"] == len(mem) == len(expanded) == len(wanted) and expanded == wanted,
              "Archive manifest and exported members disagree")
        source = decode(archive.read("source-manifest.json"))
        receipt = decode(archive.read("R31_SEMANTIC_RECEIPT.json"))
        watermark = decode(archive.read("R30_REPORT.json"))["snapshotWatermark"]
        check(receipt["jobId"] == watermark["reportJobId"] == JOB and
              source["sourceCommit"] == receipt["sourceCommit"] == watermark["sourceCommit"] == identity["source_commit"] and
              source["snapshotSha256"] == receipt["snapshotSha256"] == identity["snapshot_sha256"] and
              source["apkSha256"] == identity["apk_sha256"] and
              source["snapshotBytes"] == identity["snapshot_bytes"] and
              watermark["captureStartedAt"] == identity["capture_started_at_ms"] and
              watermark["captureFinishedAt"] == identity["capture_finished_at_ms"], "Job, source or snapshot identities disagree")
        if output_dir is not None:
            target = Path(output_dir)
            check(not os.path.lexists(target), "Output directory already exists: " + str(target))
            # A sibling staging directory protects the caller from half-written output.
            with tempfile.TemporaryDirectory(dir=target.parent, prefix=".field3-") as private_dir:
                stage = Path(private_dir) / "checked"
                stage.mkdir(mode=0o700)
                for row in expected:
                    with (stage / row["name"]).open("xb") as w:
                        w.write(archive.read(row["name"]))
                check(not os.path.lexists(target), "Output directory appeared during extraction")
                stage.rename(target)
    return {"result": "PASS_FIELD_REPORT_IDENTITY", "job_id": JOB, "members": len(expected),
            "source_commit": identity["source_commit"], "outer_sha256": OUTER_SHA256,
            "snapshot_sha256": identity["snapshot_sha256"], "report_all_checks_passed": index["allChecksPassed"]}


def main():
    cli = argparse.ArgumentParser(description=__doc__)
    cli.add_argument("--input", required=True, type=Path, help="Original outer ZIP held by the operator")
    cli.add_argument("--identity", type=Path, default=Path(__file__).resolve().parents[1] / "evidence/FIELD3_IDENTITY.json")
    cli.add_argument("--output-dir", type=Path, help="Optionally create this new private directory after verification")
    args = cli.parse_args()
    try:
        print(json.dumps(verify_report(args.input, args.identity, args.output_dir), ensure_ascii=False))
    except (OSError, ValueError, TypeError, KeyError, IndexError, zipfile.BadZipFile) as exc:
        cli.exit(1, "FAIL_FIELD_REPORT: " + str(exc) + "\n")


if __name__ == "__main__":
    main()
