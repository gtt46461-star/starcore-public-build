#!/usr/bin/env python3
"""R930 release boundary: preserve the R928 owner-signed baseline while validating current Batch3/4 identity.

This gate intentionally does not compare current production source bytes to the historical R928 source
snapshot. Batch3/4 changed those sources by design. The R928 release artifact remains immutable evidence
for predecessor APK integrity, certificate continuity and monotonic versioning.
"""
from __future__ import annotations

import hashlib
import json
import os
import re
import shutil
import subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
CURRENT = ROOT / "release/release_manifest.json"
PREVIOUS_RECORD = ROOT / "delivery/r928-runtime/release-artifact.json"
OUT = ROOT / "reports/r930/R930_RELEASE_BOUNDARY.json"


def sha256(path: Path) -> str:
    h = hashlib.sha256()
    with path.open("rb") as stream:
        for block in iter(lambda: stream.read(1024 * 1024), b""):
            h.update(block)
    return h.hexdigest()


def find_apksigner() -> str:
    sdk = Path(os.environ.get("ANDROID_HOME") or os.environ.get("ANDROID_SDK_ROOT") or "")
    direct = sdk / "build-tools/36.0.0/apksigner"
    if direct.is_file():
        return str(direct)
    found = shutil.which("apksigner")
    if found:
        return found
    raise SystemExit("R930_RELEASE_BOUNDARY_FAIL|APKSIGNER_MISSING")


def cert_digest(apksigner: str, apk: Path) -> str:
    p = subprocess.run([apksigner, "verify", "--print-certs", str(apk)], text=True,
                       stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
    if p.returncode:
        raise SystemExit("R930_RELEASE_BOUNDARY_FAIL|BASELINE_SIGNATURE_INVALID")
    m = re.search(r"certificate SHA-256 digest:\s*([0-9a-fA-F:]+)", p.stdout)
    if not m:
        raise SystemExit("R930_RELEASE_BOUNDARY_FAIL|BASELINE_CERT_NOT_FOUND")
    return m.group(1).replace(":", "").lower()


def check(condition: bool, code: str) -> None:
    if not condition:
        raise SystemExit("R930_RELEASE_BOUNDARY_FAIL|" + code)


def main() -> None:
    current = json.loads(CURRENT.read_text(encoding="utf-8"))
    previous = json.loads(PREVIOUS_RECORD.read_text(encoding="utf-8"))
    previous_apk = ROOT / previous["apk_path"]
    check(previous_apk.is_file(), "R928_BASELINE_APK_MISSING")
    check(sha256(previous_apk) == previous["apk_sha256"], "R928_BASELINE_APK_SHA_DRIFT")

    signer = find_apksigner()
    actual_previous_cert = cert_digest(signer, previous_apk)
    check(actual_previous_cert == str(previous["certificate_sha256"]).lower(), "R928_BASELINE_CERT_DRIFT")
    check(previous["certificate_sha256"] == previous["previous_r926_certificate_sha256"], "R928_HISTORY_CERT_DRIFT")

    check(current["applicationId"] == previous["applicationId"] == "com.ethk3.novaonepro", "APPLICATION_ID_DRIFT")
    check(int(current["versionCode"]) > int(previous["versionCode"]), "VERSION_NOT_MONOTONIC")
    check(int(current["versionCode"]) >= 577250930, "R930_VERSION_CODE_REQUIRED")
    check(str(current["versionName"]).endswith("-930"), "R930_VERSION_NAME_REQUIRED")
    check(current["schema"]["userVersion"] == 36, "DB_VERSION_DRIFT")
    check(current["runtimeContract"]["decisionOwner"] == "DecisionGateEngine_ONLY", "DECISION_OWNER_DRIFT")
    check(current["runtimeContract"]["settlementOwner"] == "SettlementEngine_ONLY", "SETTLEMENT_OWNER_DRIFT")
    forbidden = set(current["runtimeContract"].get("forbidden", []))
    check({"K4", "dynamicK", "multi-position-REAL", "chase", "martingale", "doubling", "frequency-debt"}.issubset(forbidden), "REDLINE_DRIFT")

    payload = {
        "schema": "STARCORE_R930_RELEASE_BOUNDARY_V1",
        "status": "PASS",
        "current": {
            "applicationId": current["applicationId"],
            "versionName": current["versionName"],
            "versionCode": current["versionCode"],
            "dbVersion": current["schema"]["userVersion"],
            "releaseMode": current["releaseMode"],
        },
        "predecessor": {
            "release": "R928",
            "versionName": previous["versionName"],
            "versionCode": previous["versionCode"],
            "apk": previous["apk_path"],
            "apkSha256": previous["apk_sha256"],
            "certificateSha256": actual_previous_cert,
        },
        "certificate_reference": "IMMUTABLE_R928_OWNER_SIGNED_APK",
        "historical_source_snapshot_mutated": False,
        "current_source_may_differ_from_r928": True,
        "formal_current_signing": "CHECK_AFTER_CURRENT_APK_BUILD",
        "physical_device_runtime": "RETEST_REQUIRED",
        "model_future_oos_effect": "NOT_PROVEN",
    }
    OUT.parent.mkdir(parents=True, exist_ok=True)
    OUT.write_text(json.dumps(payload, ensure_ascii=False, indent=2, sort_keys=True) + "\n", encoding="utf-8")
    print("R930_RELEASE_BOUNDARY=PASS|current=%s|code=%s|previous=R928|cert=%s" % (
        current["versionName"], current["versionCode"], actual_previous_cert))


if __name__ == "__main__":
    main()
