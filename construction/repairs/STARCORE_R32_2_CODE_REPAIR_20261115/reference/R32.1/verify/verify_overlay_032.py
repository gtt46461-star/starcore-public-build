"""Observe one 032 → candidate upgrade on an adb target with test access to the app's files directory."""
import argparse
import hashlib
import json
import re
import subprocess
import uuid
from pathlib import Path

PACKAGE = "com.starcore.research.r13"
CODE = 577311032
APK_SHA = "876f26e81114acfa3d2fa2d023b2e36309bec6dde3ef32f0e87c562b48d165ac"
SIGNER = "d8d8cee7f406267b2bf18fc9aef0c6ab760423cf205ccd6613dd44085bcc74e7"


def require(ok, message):
    if not ok:
        raise ValueError(message)


def file_sha(path):
    with path.open("rb") as data:
        digest = hashlib.sha256()
        for chunk in iter(lambda: data.read(1024 * 1024), b""):
            digest.update(chunk)
        return digest.hexdigest()


def file_identity(apk, aapt, apksigner):
    badging = subprocess.check_output([aapt, "dump", "badging", str(apk)], text=True)
    signature = subprocess.check_output([apksigner, "verify", "--verbose", "--print-certs", str(apk)], text=True)
    identity = re.search(r"^package: name='([^']+)' versionCode='(\d+)'", badging, re.M)
    certs = {v.lower() for v in re.findall(r"^(?:V[\d.]+ )?Signer(?: #\d+)?(?: \([^\r\n]*\))?:? certificate SHA-256 digest: ([a-fA-F0-9]{64})\s*$", signature, re.M)}
    require(identity and len(certs) == 1 and re.search(r"^Verifies\s*$", signature, re.M), "APK identity or signature unverifiable")
    return {"package": identity[1], "versionCode": int(identity[2]), "certificate": certs.pop(), "sha256": file_sha(apk)}


def check_transition(before, after):
    require((before["package"], before["versionCode"], before["sha256"], before["certificate"]) ==
            (PACKAGE, CODE, APK_SHA, SIGNER), "The source APK is not the exact installed 032 release")
    require(after["package"] == PACKAGE and after["certificate"] == SIGNER, "Candidate package or signer differs")
    require(after["versionCode"] > CODE and after["sha256"] != APK_SHA, "Candidate must be newer than 032")


def device_sha(adb):
    listing = subprocess.check_output(adb + ["shell", "pm", "path", PACKAGE], text=True)
    bases = [s.split(":", 1)[1].strip() for s in listing.splitlines() if s.startswith("package:") and s.strip().endswith("/base.apk")]
    require(len(bases) == 1, "Base APK ambiguous or missing from the selected device")
    with subprocess.Popen(adb + ["exec-out", "cat", bases[0]], stdout=subprocess.PIPE) as proc:
        digest = hashlib.sha256()
        for chunk in iter(lambda: proc.stdout.read(1024 * 1024), b""):
            digest.update(chunk)
        require(proc.wait() == 0, "Unable to read installed base APK bytes")
    return digest.hexdigest()


def observe(args, receipt):
    before = file_identity(args.old, args.aapt, args.apksigner)
    after = file_identity(args.candidate, args.aapt, args.apksigner)
    check_transition(before, after)
    receipt.update(before=before, after=after)
    adb = [args.adb, "-s", args.serial]
    nonce = uuid.uuid4().hex
    directory = "/data/user/0/" + PACKAGE + "/files"
    marker = directory + "/r32-upgrade-" + nonce
    receipt["step"] = "install_032"
    subprocess.run(adb + ["install", "-r", str(args.old)], check=True)
    receipt["installedBeforeSha256"] = device_sha(adb)
    require(receipt["installedBeforeSha256"] == APK_SHA, "Installed predecessor bytes differ")
    receipt["step"] = "create_private_marker"
    subprocess.run(adb + ["shell", "mkdir", "-p", directory], check=True)
    # Both the path and content are generated locally using only hex and a fixed prefix.
    subprocess.run(adb + ["shell", "echo " + nonce + " > " + marker], check=True)
    try:
        require(subprocess.check_output(adb + ["exec-out", "cat", marker]).strip() == nonce.encode(), "Marker write could not be read back")
        receipt["step"] = "install_candidate_over_032"
        subprocess.run(adb + ["install", "-r", str(args.candidate)], check=True)
        receipt["installedAfterSha256"] = device_sha(adb)
        require(receipt["installedAfterSha256"] == after["sha256"], "Installed successor bytes differ")
        marker_kept = subprocess.check_output(adb + ["exec-out", "cat", marker]).strip() == nonce.encode()
        require(marker_kept, "Private marker disappeared during upgrade")
        receipt.update(markerPreserved=True, step="complete", status="PASS")
    finally:
        subprocess.run(adb + ["shell", "rm", "-f", marker], check=False)


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--old", type=Path, required=True)
    parser.add_argument("--candidate", type=Path, required=True)
    parser.add_argument("--serial", required=True)
    parser.add_argument("--adb", default="adb")
    parser.add_argument("--aapt", default="aapt")
    parser.add_argument("--apksigner", default="apksigner")
    parser.add_argument("--out", type=Path, required=True)
    args = parser.parse_args()
    receipt = {"scope":"EXACT_ANDROID_PACKAGE_TRANSITION_AND_PRIVATE_MARKER", "status":"FAIL", "step":"preflight"}
    args.out.parent.mkdir(parents=True, exist_ok=True)
    with args.out.open("x", encoding="utf-8") as output:
        try:
            observe(args, receipt)
        except Exception as exc:
            receipt["error"] = str(exc)
            raise
        finally:
            json.dump(receipt, output, ensure_ascii=False, indent=2)
            output.write("\n")
