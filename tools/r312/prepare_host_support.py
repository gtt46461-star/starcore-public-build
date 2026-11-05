#!/usr/bin/env python3
"""Capture the current gate's actual Java/R artifacts for HOST tools, never a release freeze.

The development pre-build mode only records source identity in ignored app/build.
It does not refresh tracked R967 literals, reviewed inputs, signing or r982 assets.
"""
from __future__ import annotations

import argparse
from datetime import datetime, timezone
import hashlib
import json
import os
from pathlib import Path
import re
import struct
import subprocess
import sys
import time
import uuid
import zipfile

ROOT = Path(__file__).resolve().parents[2]
BUILD = ROOT / "app/build/r312"
SDK_SHA = "d9eb9da824d9e247a352f570f01e1169e725b2954bca9e283a71786c59b59f9a"
JDBC_SHA = "4a4832720a65eaf7f4d6fd7ede52087b994dc5633c076f9e994dc0c8b4b0b4fa"
ORIGINAL014_SHA = "15a524242feefe47f158c3cb4513b0ac4a5c7dc9038b4bf924525d11ec710b96"


def sha(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def git(*args: str) -> str:
    return subprocess.check_output(["git", *args], cwd=ROOT, text=True).strip()


def source_identity() -> dict:
    raw = subprocess.check_output([sys.executable, str(ROOT / "tools/r31/verify_baseline.py"), "--identity-only"], cwd=ROOT, text=True)
    observed = json.loads(raw)
    return {"source_head": git("rev-parse", "HEAD"), "source_tree": git("rev-parse", "HEAD^{tree}"),
            "product_worktree_sha256": observed["product_worktree_sha256"], "source_dirty": observed["source_dirty"]}


def developer_dirty_status(root: Path, observed_flag: bool, compiled_flag: int) -> dict:
    """Do not change release dirty policy. Permit only explicitly declared gate evidence growth."""
    raw = subprocess.check_output(["git", "status", "--porcelain=v1", "-z", "--untracked-files=all"], cwd=root)
    records = raw.split(b"\0")
    evidence, rejected = [], 0
    index = 0
    while index < len(records):
        row = records[index]
        index += 1
        if not row:
            continue
        if len(row) < 4:
            rejected += 1
            continue
        code, path = row[:2], os.fsdecode(row[3:])
        paths = [path]
        if b"R" in code or b"C" in code:
            if index >= len(records) or not records[index]:
                rejected += 1
            else:
                paths.append(os.fsdecode(records[index]))
                index += 1
        for value in paths:
            if value.startswith(("construction/r31/r312/", "verification/r31/r312/")):
                evidence.append(value)
            else:
                rejected += 1
    actual_flag = bool(raw.strip(b"\0"))
    if rejected:
        raise ValueError("NON_EVIDENCE_OR_UNKNOWN_DIRTY_FILES:" + str(rejected))
    if actual_flag != bool(observed_flag) or compiled_flag not in (0, 1) or bool(compiled_flag) != actual_flag:
        raise ValueError("COMPILED_DIRTY_FLAG_DIFFERS_FROM_ACTUAL_GATE_FLAG")
    return {"compiled_source_dirty": bool(compiled_flag), "observed_source_dirty": actual_flag,
            "evidence_only_dirty": actual_flag, "allowed_evidence_status_records": len(evidence),
            "release_dirty_policy_modified": False}


def class_constant(data: bytes, wanted: str):
    """Read one ConstantValue without invoking Java or printing other BuildConfig fields."""
    offset = 0

    def take(size):
        nonlocal offset
        part = data[offset:offset + size]
        if len(part) != size:
            raise ValueError("TRUNCATED_CLASS")
        offset += size
        return part

    def u2():
        return struct.unpack(">H", take(2))[0]

    def u4():
        return struct.unpack(">I", take(4))[0]

    if u4() != 0xCAFEBABE:
        raise ValueError("NOT_A_CLASS")
    take(4)
    pool = [None] * u2()
    index = 1
    while index < len(pool):
        tag = take(1)[0]
        if tag == 1:
            pool[index] = take(u2()).decode("utf-8", errors="replace")
        elif tag == 3:
            pool[index] = struct.unpack(">i", take(4))[0]
        elif tag in (7, 8, 16, 19, 20):
            pool[index] = (tag, u2())
        elif tag in (9, 10, 11, 12, 17, 18):
            take(4)
        elif tag == 4:
            take(4)
        elif tag in (5, 6):
            take(8)
            index += 1
        elif tag == 15:
            take(3)
        else:
            raise ValueError("UNSUPPORTED_CLASS_CONSTANT_TAG:" + str(tag))
        index += 1
    take(6)
    take(2 * u2())
    for _ in range(u2()):
        take(2)
        name = pool[u2()]
        take(2)
        for _ in range(u2()):
            attribute, length = pool[u2()], u4()
            if name == wanted and attribute == "ConstantValue" and length == 2:
                value = pool[u2()]
                return pool[value[1]] if isinstance(value, tuple) and value[0] == 8 else value
            take(length)
    raise ValueError("COMPILED_IDENTITY_CONSTANT_MISSING:" + wanted)


def current_batch() -> str:
    configured = os.environ.get("R312_CURRENT_BATCH")
    if configured:
        if not re.fullmatch(r"S\d{2}", configured):
            raise ValueError("INVALID_R312_CURRENT_BATCH")
        return configured
    command = ["python3", str(ROOT / "construction/STARCORE_R31_FINAL_20261104/tools/r312_gate.py"), "status"]
    return json.loads(subprocess.check_output(command, cwd=ROOT, text=True))["open_batch"]


def pre_build() -> dict:
    identity = source_identity()
    receipt = dict(identity, format="R312_DEVELOPMENT_INPUTS_V1",
                   scope="SOURCE_IDENTITY_INPUT_ONLY_NOT_RELEASE_OR_SEMANTIC_FREEZE",
                   captured_at=datetime.now(timezone.utc).isoformat(),
                   developer_mode=os.environ.get("ORG_GRADLE_PROJECT_r312Development") == "true",
                   tracked_R967_refreshed=False, release_manifest_refreshed=False,
                   r982_release_inventory_generated=False, signing_performed=False)
    batch = current_batch()
    context = dict(identity, format="R312_GATE_PREBUILD_CONTEXT_V1", batch=batch,
                   run_id=uuid.uuid4().hex, started_wall_ns=time.time_ns(),
                   scope="PRE_BUILD_METADATA_ONLY_NOT_GRADLE_HOST_OR_SUMMARY_PROOF")
    context_path = ROOT / f"verification/r31/r312/{batch}/GATE_CONTEXT.json"
    context_path.parent.mkdir(parents=True, exist_ok=True)
    context_path.write_text(json.dumps(context, indent=2) + "\n")
    receipt["gate_context"] = str(context_path)
    BUILD.mkdir(parents=True, exist_ok=True)
    (BUILD / "development-inputs.json").write_text(json.dumps(receipt, indent=2) + "\n")
    return receipt


def prepare() -> dict:
    identity = source_identity()
    batch = current_batch()
    classes = ROOT / "app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes"
    resources = ROOT / "app/build/intermediates/compile_and_runtime_not_namespaced_r_class_jar/debug/processDebugResources/R.jar"
    gradle_log = ROOT / f"verification/r31/r312/{batch}/logs/gradle.log"
    if not gradle_log.is_file():
        raise FileNotFoundError("CURRENT_GATE_GRADLE_LOG_REQUIRED:" + str(gradle_log))
    text = gradle_log.read_text(errors="replace")
    if not re.search(r"> Task :app:compileDebugJavaWithJavac(?:\s|$)", text):
        raise ValueError("CURRENT_GATE_MAIN_COMPILE_NOT_RECORDED")
    if re.search(r"> Task :app:compileDebugJavaWithJavac FAILED|Execution failed for task ':app:compileDebugJavaWithJavac'", text):
        raise ValueError("CURRENT_GATE_MAIN_COMPILE_FAILED_NO_OLD_CLASS_FALLBACK")
    config = classes / "com/ethk3/novaonepro/BuildConfig.class"
    if not config.is_file() or not resources.is_file():
        raise FileNotFoundError("ACTUAL_GATE_MAIN_CLASSES_AND_R_JAR_REQUIRED")
    raw = config.read_bytes()
    if class_constant(raw, "SOURCE_COMMIT_R26") != identity["source_head"]:
        raise ValueError("COMPILED_SOURCE_HEAD_MISMATCH_NO_HISTORICAL_JAR_FALLBACK")
    if class_constant(raw, "SOURCE_TREE_R31") != identity["source_tree"]:
        raise ValueError("COMPILED_SOURCE_TREE_MISMATCH")
    dirty = developer_dirty_status(ROOT, identity["source_dirty"], class_constant(raw, "SOURCE_DIRTY_R31"))
    if class_constant(raw, "SOURCE_WORKTREE_SHA256_R31") != identity["product_worktree_sha256"]:
        raise ValueError("CURRENT_WORKTREE_DIFFERS_FROM_COMPILED_GATE_SOURCE")
    inventory = {str(p.relative_to(classes)): sha(p) for p in sorted(classes.rglob("*.class"))}
    if not inventory:
        raise ValueError("EMPTY_GATE_CLASSES")
    tree_sha = hashlib.sha256(json.dumps(inventory, sort_keys=True, separators=(",", ":")).encode()).hexdigest()
    location = BUILD / "host-support" / identity["source_head"] / tree_sha
    location.mkdir(parents=True, exist_ok=True)
    jar = location / "classes.jar"
    if not jar.exists():
        temporary = location / "classes.jar.tmp"
        with zipfile.ZipFile(temporary, "w", zipfile.ZIP_DEFLATED, compresslevel=6) as archive:
            for relative in inventory:
                archive.write(classes / relative, relative)
        temporary.replace(jar)
    with zipfile.ZipFile(jar) as archive:
        observed = {name: hashlib.sha256(archive.read(name)).hexdigest() for name in archive.namelist() if name.endswith(".class")}
    if observed != inventory:
        raise ValueError("CURRENT_GATE_CLASS_DIRECTORY_JAR_MISMATCH")
    resource_copy = location / "R.jar"
    if resource_copy.exists() and sha(resource_copy) != sha(resources):
        raise ValueError("CURRENT_GATE_RESOURCE_SNAPSHOT_CHANGED")
    if not resource_copy.exists():
        resource_copy.write_bytes(resources.read_bytes())
    java_home = Path(os.environ["JAVA_HOME"]).resolve()
    sdk_home = Path(os.environ.get("ANDROID_HOME") or os.environ["ANDROID_SDK_ROOT"]).resolve()
    sdk = Path(os.environ.get("R312_ANDROID_JAR", str(sdk_home / "platforms/android-36/android.jar"))).resolve()
    jdbc = Path(os.environ.get("R312_SQLITE_JDBC") or os.environ.get("SQLITE_JDBC") or str(BUILD / "deps/sqlite-jdbc.jar")).resolve()
    if sha(sdk) != SDK_SHA or sha(jdbc) != JDBC_SHA:
        raise ValueError("FIXED_SDK36_OR_SQLITE_JDBC_SHA256_MISMATCH")
    for name in ("java", "javac"):
        if not (java_home / "bin" / name).is_file():
            raise FileNotFoundError("JAVA_HOME_TOOL_REQUIRED:" + name)
    outcome_sources = ["DatabaseCoordinator", "strategyv2/FailureDispositionR31", "strategyv2/WriterHoldLedgerR30", "strategyv2/ReportSnapshotR30", "strategyv2/ReportR27"]
    source_sha = {"app/src/main/java/com/ethk3/novaonepro/" + name + ".java": sha(ROOT / ("app/src/main/java/com/ethk3/novaonepro/" + name + ".java")) for name in outcome_sources}
    outcome_ahead = location / "writer-outcomes-ahead"
    outcome_classes = {}
    for name in outcome_sources:
        relative = "com/ethk3/novaonepro/" + name
        selected = [p for p in inventory if p == relative + ".class" or p.startswith(relative + "$")]
        if relative + ".class" not in selected:
            raise ValueError("WRITER_OUTCOME_ROOT_CLASS_MISSING:" + name)
        for p in selected:
            target = outcome_ahead / p
            target.parent.mkdir(parents=True, exist_ok=True)
            target.write_bytes((classes / p).read_bytes())
            if sha(target) != inventory[p]:
                raise ValueError("WRITER_OUTCOME_CLASS_COPY_MISMATCH:" + p)
            outcome_classes[p] = inventory[p]
    if {str(p.relative_to(outcome_ahead)) for p in outcome_ahead.rglob("*.class")} != set(outcome_classes):
        raise ValueError("WRITER_OUTCOME_AHEAD_UNEXPECTED_CLASSES")
    receipt = dict(identity, format="R312_ACTUAL_GATE_HOST_SUPPORT_V1", scope="ACTUAL_CURRENT_GATE_BUILD_INPUT_SNAPSHOT_NOT_RELEASE_FREEZE",
                   batch=batch, class_count=len(inventory), classes_dir=str(classes), classes_jar=str(jar),
                   classes_jar_sha256=sha(jar), class_tree_sha256=tree_sha, r_jar=str(resource_copy), r_jar_sha256=sha(resource_copy),
                   java_home=str(java_home), sdk=str(sdk), sdk_sha256=SDK_SHA, jdbc=str(jdbc), jdbc_sha256=JDBC_SHA,
                   gradle_log=str(gradle_log), gradle_log_sha256=sha(gradle_log), baseJarSha256=sha(jar), sourceSha256=source_sha,
                   aheadMeaning="The same actual whole gate class directory; not independently compiled business overrides.")
    receipt["development_dirty_observation"] = dirty
    receipt["writer_outcomes_ahead"] = str(outcome_ahead)
    receipt["writer_outcomes_classes_sha256"] = outcome_classes
    receipt_path = location / "SUPPORT_RECEIPT.json"
    receipt_path.write_text(json.dumps(receipt, indent=2) + "\n")
    # The original014 artifact is an explicitly historical negative-control dependency only.
    old = Path(os.environ.get("R312_ORIGINAL014_CLASSES", str(BUILD / "deps/original014-classes.jar")))
    manifest = {"production_classes": str(classes), "support": {
        "actual014Classes": {"path": str(old), "sha256": ORIGINAL014_SHA},
        "androidSdk": {"path": str(sdk), "sha256": SDK_SHA},
        "sqliteJdbc": {"path": str(jdbc), "sha256": JDBC_SHA},
        "actualGeneratedR": {"path": str(resource_copy), "sha256": sha(resource_copy)}},
        "scope": "Current producer/API closure is this gate build. actual014Classes is the labelled historical negative control; missing original014 remains a real dependency failure."}
    manifest_path = location / "platform-joint-manifest.json"
    manifest_path.write_text(json.dumps(manifest, indent=2) + "\n")
    return dict(receipt, receipt=str(receipt_path), platform_manifest=str(manifest_path),
                run_key=str(gradle_log.stat().st_mtime_ns))


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--pre-build", action="store_true", help="Record ignored development source identity only; no compilation, tests or release freeze")
    args = parser.parse_args()
    print(json.dumps(pre_build() if args.pre_build else prepare(), indent=2))


if __name__ == "__main__":
    main()
