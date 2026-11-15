#!/usr/bin/env python3
"""Record batch2 APK signing/cover-install boundary without promoting debug to formal release."""
from __future__ import annotations

import glob
import json
import os
import re
import shutil
import subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "reports" / "r926" / "batch2" / "BATCH2_SIGNING_BOUNDARY.json"
BASELINE = ROOT / "delivery" / "r926-batch1" / "Starcore-Tianshu-V57.21.1.0.1-R5.4.1.0.1-926-release.apk"
MANIFEST = ROOT / "release" / "release_manifest.json"


def find_tool(name: str) -> str:
    sdk = Path(os.environ.get("ANDROID_HOME") or os.environ.get("ANDROID_SDK_ROOT") or "")
    direct = sdk / "build-tools" / "36.0.0" / name
    if direct.is_file():
        return str(direct)
    found = shutil.which(name)
    if found:
        return found
    raise SystemExit("ANDROID_TOOL_MISSING|" + name)


def run(*args: str) -> str:
    p = subprocess.run(list(args), stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True)
    if p.returncode != 0:
        raise SystemExit("COMMAND_FAILED|" + " ".join(args) + "|" + p.stdout[-1200:])
    return p.stdout


def cert_digest(apksigner: str, apk: Path) -> str:
    text = run(apksigner, "verify", "--print-certs", str(apk))
    m = re.search(r"certificate SHA-256 digest:\s*([0-9a-fA-F:]+)", text)
    if not m:
        raise SystemExit("CERT_SHA256_NOT_FOUND|" + str(apk))
    return m.group(1).replace(":", "").lower()


def badging(aapt: str, apk: Path) -> dict[str, str]:
    text = run(aapt, "dump", "badging", str(apk))
    first = text.splitlines()[0] if text else ""
    out = {}
    for key in ("name", "versionCode", "versionName"):
        m = re.search(rf"\b{key}='([^']*)'", first)
        out[key] = m.group(1) if m else ""
    return out


def main() -> None:
    if not BASELINE.is_file():
        raise SystemExit("BATCH1_SIGNED_APK_MISSING")
    debug_matches = sorted(glob.glob(str(ROOT / "app/build/outputs/apk/debug/*.apk")))
    if len(debug_matches) != 1:
        raise SystemExit("BATCH2_DEBUG_APK_COUNT|" + str(len(debug_matches)))
    debug = Path(debug_matches[0])
    manifest = json.loads(MANIFEST.read_text(encoding="utf-8"))
    apksigner, aapt = find_tool("apksigner"), find_tool("aapt")
    baseline_cert, debug_cert = cert_digest(apksigner, BASELINE), cert_digest(apksigner, debug)
    base_id, debug_id = badging(aapt, BASELINE), badging(aapt, debug)
    package_ok = debug_id["name"] == str(manifest["applicationId"])
    version_ok = debug_id["versionCode"] == str(manifest["versionCode"]) and debug_id["versionName"] == str(manifest["versionName"])
    payload = {
        "status": "PASS_BOUNDARY_RECORDED",
        "batch2_artifact_kind": "DEBUG_ENGINEERING_APK",
        "formal_same_cert_cover_install_apk": "NOT_PROVEN",
        "reason": "CI executes assembleDebug; formal release requires external original signing material and the release signing path",
        "baseline_signed_apk": str(BASELINE.relative_to(ROOT)),
        "batch2_debug_apk": str(debug.relative_to(ROOT)),
        "baseline_identity": base_id,
        "batch2_identity": debug_id,
        "manifest_identity": {
            "applicationId": manifest["applicationId"],
            "versionCode": manifest["versionCode"],
            "versionName": manifest["versionName"],
        },
        "package_identity_matches_manifest": package_ok,
        "version_identity_matches_manifest": version_ok,
        "baseline_cert_sha256": baseline_cert,
        "batch2_debug_cert_sha256": debug_cert,
        "certificate_continuity_with_baseline": baseline_cert == debug_cert,
    }
    if not package_ok or not version_ok:
        raise SystemExit("BATCH2_APK_IDENTITY_MISMATCH|" + json.dumps(payload, ensure_ascii=False, sort_keys=True))
    OUT.parent.mkdir(parents=True, exist_ok=True)
    OUT.write_text(json.dumps(payload, ensure_ascii=False, indent=2, sort_keys=True) + "\n", encoding="utf-8")
    print(json.dumps(payload, ensure_ascii=False, sort_keys=True))


if __name__ == "__main__":
    main()
