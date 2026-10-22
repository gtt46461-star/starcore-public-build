#!/usr/bin/env python3
"""Validate the current R930 APK identity and, when supplied, owner-certificate continuity.

CI always validates the engineering debug APK. A formal same-certificate release is PASS only when
R930_SIGNED_APK points at an externally owner-signed current APK. Missing private signing material is
reported as an external boundary and is never converted into a false PASS.
"""
from __future__ import annotations

import glob
import hashlib
import json
import os
import re
import shutil
import subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
MANIFEST = ROOT / "release/release_manifest.json"
PREVIOUS_RECORD = ROOT / "delivery/r928-runtime/release-artifact.json"
OUT = ROOT / "reports/r930/R930_APK_BOUNDARY.json"


def find_tool(name: str) -> str:
    sdk = Path(os.environ.get("ANDROID_HOME") or os.environ.get("ANDROID_SDK_ROOT") or "")
    direct = sdk / "build-tools/36.0.0" / name
    if direct.is_file():
        return str(direct)
    found = shutil.which(name)
    if found:
        return found
    raise SystemExit("R930_APK_BOUNDARY_FAIL|TOOL_MISSING|" + name)


def run(*args: str) -> str:
    p = subprocess.run(list(args), text=True, stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
    if p.returncode:
        raise SystemExit("R930_APK_BOUNDARY_FAIL|COMMAND|" + args[0] + "|" + p.stdout[-800:])
    return p.stdout


def sha256(path: Path) -> str:
    h = hashlib.sha256()
    with path.open("rb") as stream:
        for block in iter(lambda: stream.read(1024 * 1024), b""):
            h.update(block)
    return h.hexdigest()


def cert_digest(apksigner: str, apk: Path) -> str:
    text = run(apksigner, "verify", "--print-certs", str(apk))
    m = re.search(r"certificate SHA-256 digest:\s*([0-9a-fA-F:]+)", text)
    if not m:
        raise SystemExit("R930_APK_BOUNDARY_FAIL|CERT_NOT_FOUND|" + str(apk))
    return m.group(1).replace(":", "").lower()


def identity(aapt: str, apk: Path) -> dict[str, str]:
    text = run(aapt, "dump", "badging", str(apk))
    first = text.splitlines()[0] if text else ""
    values: dict[str, str] = {}
    for key in ("name", "versionCode", "versionName"):
        match = re.search(rf"\b{key}='([^']*)'", first)
        values[key] = match.group(1) if match else ""
    return values


def require_current(actual: dict[str, str], manifest: dict, label: str) -> None:
    expected = {
        "name": str(manifest["applicationId"]),
        "versionCode": str(manifest["versionCode"]),
        "versionName": str(manifest["versionName"]),
    }
    if actual != expected:
        raise SystemExit("R930_APK_BOUNDARY_FAIL|IDENTITY|%s|actual=%s|expected=%s" % (label, actual, expected))


def main() -> None:
    manifest = json.loads(MANIFEST.read_text(encoding="utf-8"))
    previous = json.loads(PREVIOUS_RECORD.read_text(encoding="utf-8"))
    aapt, apksigner = find_tool("aapt"), find_tool("apksigner")
    baseline_apk = ROOT / previous["apk_path"]
    baseline_cert = cert_digest(apksigner, baseline_apk)
    if baseline_cert != str(previous["certificate_sha256"]).lower():
        raise SystemExit("R930_APK_BOUNDARY_FAIL|BASELINE_CERT_DRIFT")

    debug_matches = sorted(glob.glob(str(ROOT / "app/build/outputs/apk/debug/*.apk")))
    if len(debug_matches) != 1:
        raise SystemExit("R930_APK_BOUNDARY_FAIL|DEBUG_APK_COUNT|" + str(len(debug_matches)))
    debug_apk = Path(debug_matches[0])
    debug_identity = identity(aapt, debug_apk)
    require_current(debug_identity, manifest, "DEBUG")
    debug_cert = cert_digest(apksigner, debug_apk)

    signed_value = os.environ.get("R930_SIGNED_APK", "").strip()
    signed: dict[str, object] = {
        "status": "EXTERNAL_SIGNING_REQUIRED",
        "sameCertificate": False,
        "path": "",
        "sha256": "",
        "certificateSha256": "",
    }
    if signed_value:
        signed_apk = Path(signed_value).expanduser().resolve()
        if not signed_apk.is_file():
            raise SystemExit("R930_APK_BOUNDARY_FAIL|SIGNED_APK_MISSING|" + str(signed_apk))
        signed_identity = identity(aapt, signed_apk)
        require_current(signed_identity, manifest, "SIGNED")
        signed_cert = cert_digest(apksigner, signed_apk)
        if signed_cert != baseline_cert:
            raise SystemExit("R930_APK_BOUNDARY_FAIL|SIGNED_CERTIFICATE_DRIFT")
        signed = {
            "status": "PASS_SAME_CERTIFICATE",
            "sameCertificate": True,
            "path": str(signed_apk),
            "sha256": sha256(signed_apk),
            "certificateSha256": signed_cert,
            "identity": signed_identity,
        }

    payload = {
        "schema": "STARCORE_R930_APK_BOUNDARY_V1",
        "status": "PASS",
        "manifest": {
            "applicationId": manifest["applicationId"],
            "versionName": manifest["versionName"],
            "versionCode": manifest["versionCode"],
        },
        "engineeringDebugApk": {
            "status": "PASS_IDENTITY",
            "path": str(debug_apk.relative_to(ROOT)),
            "sha256": sha256(debug_apk),
            "certificateSha256": debug_cert,
            "sameCertificateAsR928": debug_cert == baseline_cert,
            "identity": debug_identity,
        },
        "ownerSignedReleaseApk": signed,
        "baselineR928CertificateSha256": baseline_cert,
        "formalCoverInstallClaim": bool(signed.get("sameCertificate")),
        "privateSigningMaterialStoredInSource": False,
        "externalSigningPath": "scripts/r926_build_from_signing_kit.py -> scripts/r926_build_release.sh",
        "physicalDeviceInstallExecuted": False,
    }
    OUT.parent.mkdir(parents=True, exist_ok=True)
    OUT.write_text(json.dumps(payload, ensure_ascii=False, indent=2, sort_keys=True) + "\n", encoding="utf-8")
    print("R930_APK_BOUNDARY=PASS|debug=IDENTITY_PASS|signed=%s|cover_install_claim=%s" % (
        signed["status"], str(payload["formalCoverInstallClaim"]).lower()))


if __name__ == "__main__":
    main()
