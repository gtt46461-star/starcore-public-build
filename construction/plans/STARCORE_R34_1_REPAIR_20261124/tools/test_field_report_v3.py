"""Exercise exact report validation, private local extraction, and failure isolation."""
import argparse
from copy import deepcopy
import hashlib
import json
from pathlib import Path
import tempfile
from field_report_v3 import verify_report


def rejected(action, fragment):
    try:
        action()
    except ValueError as exc:
        if fragment not in str(exc):
            raise
    else:
        raise ValueError("Unexpectedly accepted case requiring " + fragment)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--input", required=True, type=Path)
    parser.add_argument("--identity", type=Path, default=Path(__file__).resolve().parents[1] / "evidence/FIELD3_IDENTITY.json")
    args = parser.parse_args()
    manifest = json.loads(args.identity.read_bytes())
    with tempfile.TemporaryDirectory() as work:
        root = Path(work)
        target = root / "verified"
        verify_report(args.input, args.identity, target)
        for row in manifest["members"]:
            if hashlib.sha256((target / row["name"]).read_bytes()).hexdigest() != row["sha256"]:
                raise ValueError("Extracted bytes disagree for " + row["name"])
        sentinel = target / "private"
        sentinel.write_bytes(b"preserve")
        rejected(lambda: verify_report(args.input, args.identity, target), "already exists")
        if sentinel.read_bytes() != b"preserve":
            raise ValueError("Pre-existing output was changed")

        bad = root / "bad.zip"
        raw = bytearray(args.input.read_bytes())
        raw[0] ^= 1
        bad.write_bytes(raw)
        other = root / "should-not-exist"
        rejected(lambda: verify_report(bad, args.identity, other), "Outer ZIP SHA-256 differs")
        bad.write_bytes(raw[:-1])
        rejected(lambda: verify_report(bad, args.identity, other), "Outer ZIP size differs")

        sidecar = root / "bad.json"
        for name, change, message in (
            ("tampered member hash", lambda d: d["members"][0].update(sha256="0"*64), "Member SHA-256 differs"),
            ("unsafe path", lambda d: d["members"][0].update(name="../parent.txt"), "nested member"),
            ("different source", lambda d: d.update(source_commit="0"*40), "identities disagree"),
        ):
            clone = deepcopy(manifest)
            change(clone)
            sidecar.write_text(json.dumps(clone))
            rejected(lambda: verify_report(args.input, sidecar, other), message)
            if other.exists():
                raise ValueError(name + " created an output directory")
        if (root / "parent.txt").exists():
            raise ValueError("Unexpected traversal output")
    print(json.dumps({"result":"PASS_FIELD_REPORT_GUARDS","rejections":6,"extracted_member_checks":57}))


if __name__ == "__main__":
    main()
