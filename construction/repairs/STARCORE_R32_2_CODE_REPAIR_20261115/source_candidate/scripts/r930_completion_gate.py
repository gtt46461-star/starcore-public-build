#!/usr/bin/env python3
"""R930 Batch3/4 completion gate.

Engineering construction is counted from executable Batch3/4 task and enhancement contracts plus the
retained R927 regression suite. Physical-device execution and future OOS model effect remain separate
truth boundaries and never inflate construction completion.
"""
from __future__ import annotations

import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "reports/r930/R930_BATCH34_COMPLETION.json"
PATHS = {
    "acceptance": ROOT / "reports/r930/BATCH34_ENGINEERING_ACCEPTANCE.json",
    "release": ROOT / "reports/r930/R930_RELEASE_BOUNDARY.json",
    "apk": ROOT / "reports/r930/R930_APK_BOUNDARY.json",
    "sql_regression": ROOT / "reports/r927/AUDIT_SQL_RESULT.json",
    "engineering_regression": ROOT / "reports/r927/ENGINEERING_CLOSURE_RESULT.json",
    "manifest": ROOT / "release/release_manifest.json",
}


def fail(code: str, detail: str = "") -> None:
    raise SystemExit("R930_COMPLETION_FAIL|" + code + ("|" + detail if detail else ""))


def load(name: str) -> dict:
    path = PATHS[name]
    if not path.is_file():
        fail("MISSING_" + name.upper(), str(path.relative_to(ROOT)))
    try:
        return json.loads(path.read_text(encoding="utf-8"))
    except Exception as exc:
        fail("INVALID_JSON_" + name.upper(), str(exc))
    return {}


def expect(value: bool, code: str) -> None:
    if not value:
        fail(code)


def main() -> None:
    acceptance = load("acceptance")
    release = load("release")
    apk = load("apk")
    sql = load("sql_regression")
    engineering = load("engineering_regression")
    manifest = load("manifest")

    tasks = acceptance.get("task_contracts", {})
    b3, b4, combined = tasks.get("batch3", {}), tasks.get("batch4", {}), tasks.get("combined", {})
    enhancements = acceptance.get("enhancement_contracts", {})
    expect(b3.get("accepted") == 12 and b3.get("total") == 12 and b3.get("percent") == 100, "BATCH3_NOT_12_OF_12")
    expect(b4.get("accepted") == 10 and b4.get("total") == 10 and b4.get("percent") == 100, "BATCH4_NOT_10_OF_10")
    expect(combined.get("accepted") == 22 and combined.get("total") == 22 and combined.get("percent") == 100, "COMBINED_NOT_22_OF_22")
    expect(enhancements.get("accepted") == 17 and enhancements.get("total") == 17 and enhancements.get("percent") == 100, "ENHANCEMENTS_NOT_17_OF_17")
    expect(acceptance.get("proof_boundary", {}).get("engineering_construction") == "PASS", "ENGINEERING_CONSTRUCTION_NOT_PASS")
    expect(acceptance.get("proof_boundary", {}).get("future_oos_effect_proven") is False, "FUTURE_OOS_BOUNDARY_DRIFT")
    expect(acceptance.get("proof_boundary", {}).get("real_unlock") is False, "REAL_UNLOCK_BOUNDARY_DRIFT")

    expect(release.get("status") == "PASS", "RELEASE_BOUNDARY_NOT_PASS")
    expect(apk.get("status") == "PASS", "APK_BOUNDARY_NOT_PASS")
    expect(sql.get("passed") is True, "R927_SQL_REGRESSION_NOT_PASS")
    expect(engineering.get("passed") is True, "R927_ENGINEERING_REGRESSION_NOT_PASS")
    expect(int(manifest.get("versionCode", 0)) >= 577250930, "VERSION_CODE_BELOW_R930")
    expect(str(manifest.get("versionName", "")).endswith("-930"), "VERSION_NAME_NOT_R930")

    signed = apk.get("ownerSignedReleaseApk", {})
    signed_status = str(signed.get("status", "EXTERNAL_SIGNING_REQUIRED"))
    formal_cover = bool(apk.get("formalCoverInstallClaim", False))
    rows = [
        {"id": "BATCH3", "status": "PASS", "accepted": 12, "total": 12},
        {"id": "BATCH4", "status": "PASS", "accepted": 10, "total": 10},
        {"id": "ENHANCEMENT", "status": "PASS", "accepted": 17, "total": 17},
        {"id": "R927_SQL_REGRESSION", "status": "PASS"},
        {"id": "R927_ENGINEERING_REGRESSION", "status": "PASS"},
        {"id": "R930_RELEASE_IDENTITY", "status": "PASS"},
        {"id": "R930_ENGINEERING_APK", "status": "PASS"},
    ]
    payload = {
        "schema": "STARCORE_R930_BATCH34_COMPLETION_V1",
        "versionName": manifest["versionName"],
        "versionCode": manifest["versionCode"],
        "engineeringConstruction": {
            "status": "PASS",
            "percent": 100.0,
            "batch3": "12/12",
            "batch4": "10/10",
            "combinedTasks": "22/22",
            "enhancements": "17/17",
            "retainedRegression": "PASS",
        },
        "rows": rows,
        "ownerSignedRelease": {
            "status": signed_status,
            "formalCoverInstallClaim": formal_cover,
            "certificateSha256": signed.get("certificateSha256", ""),
            "apkSha256": signed.get("sha256", ""),
        },
        "truthBoundaries": {
            "physicalDeviceRuntime": "RETEST_REQUIRED",
            "futureOosModelEffect": "NOT_PROVEN",
            "syntheticOrHostEvidenceCannotClaimFutureHitRate": True,
            "engineeringTargets": {
                "selectedCoverageMinimum": 0.15,
                "selectedHitRateTarget": 0.40,
                "eceMaximum": 0.04,
                "longestMissTargetMaximum": 8,
            },
        },
        "constructionPercentExcludes": ["PHYSICAL_DEVICE_RETEST", "FUTURE_OOS_EFFECT_PROOF"],
    }
    OUT.parent.mkdir(parents=True, exist_ok=True)
    OUT.write_text(json.dumps(payload, ensure_ascii=False, indent=2, sort_keys=True) + "\n", encoding="utf-8")
    print("R930_BATCH34_COMPLETION=PASS|engineering=100.0|batch3=12/12|batch4=10/10|enhancements=17/17|signed=%s" % signed_status)


if __name__ == "__main__":
    main()
