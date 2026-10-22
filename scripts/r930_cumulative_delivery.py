#!/usr/bin/env python3
"""Assemble a current-commit Batch3/4 cumulative engineering delivery artifact for CI export.

The CI bundle contains the exact Git source snapshot, current engineering APK and evidence. Owner signing
material is deliberately excluded. A locally owner-signed APK can be added to the final handoff after
running scripts/r926_build_from_signing_kit.py against this same source commit.
"""
from __future__ import annotations

import hashlib
import json
import shutil
import subprocess
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "build/r930-delivery"
MANIFEST = ROOT / "release/release_manifest.json"


def sha256(path: Path) -> str:
    h = hashlib.sha256()
    with path.open("rb") as stream:
        for block in iter(lambda: stream.read(1024 * 1024), b""):
            h.update(block)
    return h.hexdigest()


def run(*args: str) -> str:
    p = subprocess.run(list(args), cwd=ROOT, text=True, stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
    if p.returncode:
        raise SystemExit("R930_DELIVERY_FAIL|COMMAND|" + " ".join(args) + "|" + p.stdout[-1200:])
    return p.stdout.strip()


def main() -> None:
    m = json.loads(MANIFEST.read_text(encoding="utf-8"))
    version = str(m["versionName"])
    if int(m["versionCode"]) < 577250930 or not version.endswith("-930"):
        raise SystemExit("R930_DELIVERY_FAIL|IDENTITY_NOT_R930")
    completion_path = ROOT / "reports/r930/R930_BATCH34_COMPLETION.json"
    if not completion_path.is_file():
        raise SystemExit("R930_DELIVERY_FAIL|COMPLETION_MISSING")
    completion = json.loads(completion_path.read_text(encoding="utf-8"))
    if completion.get("engineeringConstruction", {}).get("percent") != 100.0:
        raise SystemExit("R930_DELIVERY_FAIL|ENGINEERING_NOT_100")

    debug_apks = sorted((ROOT / "app/build/outputs/apk/debug").glob("*.apk"))
    if len(debug_apks) != 1:
        raise SystemExit("R930_DELIVERY_FAIL|DEBUG_APK_COUNT|" + str(len(debug_apks)))
    debug_apk = debug_apks[0]

    OUT.mkdir(parents=True, exist_ok=True)
    stem = "STARCORE_5.4.1.0.4_R930_BATCH34"
    source_zip = OUT / (stem + "_COMPLETE_SOURCE.zip")
    run("git", "archive", "--format=zip", "-o", str(source_zip), "HEAD")
    source_commit = run("git", "rev-parse", "HEAD")

    engineering_apk = OUT / ("Starcore-Tianshu-" + version + "-engineering-debug.apk")
    shutil.copy2(debug_apk, engineering_apk)

    evidence_files = [
        ROOT / "reports/r930/BATCH34_ENGINEERING_ACCEPTANCE.json",
        ROOT / "reports/r930/R930_RELEASE_BOUNDARY.json",
        ROOT / "reports/r930/R930_APK_BOUNDARY.json",
        ROOT / "reports/r930/R930_BATCH34_COMPLETION.json",
        ROOT / "reports/r927/AUDIT_SQL_RESULT.json",
        ROOT / "reports/r927/ENGINEERING_CLOSURE_RESULT.json",
        ROOT / "build/r925-host/R930_BATCH34_SQL_SUMMARY.txt",
        ROOT / "build/r925-host/Batch34ManifestHarnessR930.txt",
        ROOT / "build/r925-host/Batch34DiagnosticCardsHarnessR930.txt",
    ]
    for path in evidence_files:
        if not path.is_file():
            raise SystemExit("R930_DELIVERY_FAIL|EVIDENCE_MISSING|" + str(path.relative_to(ROOT)))

    manifest = {
        "schema": "STARCORE_R930_BATCH34_CI_DELIVERY_V1",
        "sourceCommit": source_commit,
        "versionName": version,
        "versionCode": m["versionCode"],
        "applicationId": m["applicationId"],
        "databaseName": m["databaseName"],
        "dbVersion": m["schema"]["userVersion"],
        "engineeringConstructionPercent": 100.0,
        "batch3": "12/12",
        "batch4": "10/10",
        "enhancements": "17/17",
        "sourceArchive": {"name": source_zip.name, "sha256": sha256(source_zip), "bytes": source_zip.stat().st_size},
        "engineeringApk": {"name": engineering_apk.name, "sha256": sha256(engineering_apk), "bytes": engineering_apk.stat().st_size},
        "ownerSignedRelease": completion.get("ownerSignedRelease", {}),
        "truthBoundary": {
            "ciBundleIsEngineeringDelivery": True,
            "ownerPrivateSigningMaterialIncluded": False,
            "formalSameCertificateReleaseAddedOnlyAfterExternalOwnerSigning": True,
            "physicalDeviceRuntime": "RETEST_REQUIRED",
            "futureOosModelEffect": "NOT_PROVEN",
        },
    }
    manifest_path = OUT / "R930_BATCH34_DELIVERY_MANIFEST.json"
    manifest_path.write_text(json.dumps(manifest, ensure_ascii=False, indent=2, sort_keys=True) + "\n", encoding="utf-8")

    readme = OUT / "00_READ_FIRST_ZH.txt"
    readme.write_text(
        "STARCORE_TIANSHU 第三+第四批 R930 CI 累计交付\n"
        f"Version={version}\nVersionCode={m['versionCode']}\nCommit={source_commit}\n"
        "EngineeringConstruction=100% (Batch3 12/12, Batch4 10/10, Enhancement 17/17)\n"
        "本 CI 包包含当前提交完整源码、资产、工程 APK 与验收报告。\n"
        "正式同证书覆盖安装 APK 必须使用外部 owner signing-kit 对同一源码提交执行 release 构建；私钥绝不进入 GitHub 源码或 CI 工件。\n"
        "真机息屏/杀进程长期运行与未来 OOS 胜率仍需独立实机/未来样本证据，不用 host/synthetic 结果冒充。\n",
        encoding="utf-8",
    )

    checksum_path = OUT / "R930_BATCH34_SHA256SUMS.txt"
    checksum_targets = [source_zip, engineering_apk, manifest_path, readme] + evidence_files
    lines = [sha256(p) + "  " + str(p.relative_to(ROOT)) for p in checksum_targets]
    checksum_path.write_text("\n".join(lines) + "\n", encoding="utf-8")

    final_zip = OUT / (stem + "_CI_CUMULATIVE_TOTAL.zip")
    with zipfile.ZipFile(final_zip, "w", compression=zipfile.ZIP_DEFLATED, compresslevel=6) as z:
        for p in [source_zip, engineering_apk, manifest_path, readme, checksum_path]:
            z.write(p, "R930_DELIVERY/" + p.name)
        for p in evidence_files:
            z.write(p, "R930_DELIVERY/EVIDENCE/" + str(p.relative_to(ROOT)))
    print("R930_CUMULATIVE_DELIVERY=PASS|commit=%s|zip=%s|sha256=%s" % (source_commit, final_zip, sha256(final_zip)))


if __name__ == "__main__":
    main()
