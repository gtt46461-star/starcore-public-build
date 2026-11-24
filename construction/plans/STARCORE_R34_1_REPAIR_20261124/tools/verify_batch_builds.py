#!/usr/bin/env python3
"""Read back a B01..Bn APK delivery. Never invoke build commands taken from receipts."""
import argparse
import contextlib
import datetime
import hashlib
import json
import re
import shutil
import subprocess
import sys
import tempfile
import urllib.request
import zipfile
from pathlib import Path
from urllib.parse import urlsplit

from verify_plan import verify_seal
from verify_receipt import decode, git, need, read, safe_name, sha, validate

PACKAGE = "com.starcore.research.r13"
CERT = "d8d8cee7f406267b2bf18fc9aef0c6ab760423cf205ccd6613dd44085bcc74e7"
TASKS = {":app:assembleRelease", ":app:assembleDebug", ":app:assembleDebugAndroidTest"}


def artifact(root, ref):
    path = root / safe_name(ref["path"])
    need(path.resolve() == path and path.is_file() and sha(path) == ref["sha256"], "Missing or altered original: " + str(ref["path"]))
    return path


def sdk_identity(apk, aapt, apksigner):
    if not aapt or not apksigner:
        return None
    def invoke(argv):
        return subprocess.run([str(a) for a in argv], capture_output=True, text=True,
                              check=True, timeout=180).stdout
    manifest = invoke([aapt, "dump", "badging", apk])
    package = re.findall(r"^package: name='([^']+)' versionCode='([0-9]+)' versionName='([^']*)'", manifest, re.M)
    need(len(package) == 1, "aapt did not identify a unique application")
    result = subprocess.run([str(apksigner), "verify", "--verbose", "--print-certs", str(apk)],
                            capture_output=True, text=True, timeout=180)
    sig = result.stdout + "\n" + result.stderr
    certs = {s.replace(":", "").lower() for s in re.findall(
        r"^(?:V[0-9]+(?:\.[0-9]+)? )?Signer(?: #[0-9]+)?(?: \([^\r\n]*\))?:? certificate SHA-256 digest:\s*([a-fA-F0-9:]+)\s*$",
        sig, re.M)}
    signed = result.returncode == 0 and bool(re.search(r"^Verifies\s*$", sig, re.M)) and certs == {CERT}
    name, code, tag = package[0]
    return name, int(code), tag, signed


def check_download(url, apk, container=None):
    parts = urlsplit(url)
    need(parts.scheme == "https" and parts.hostname and not parts.username and not parts.password and
         not parts.fragment, "Download needs an HTTPS artifact URL")
    if container is None:
        size, expected = apk.stat().st_size, sha(apk)
        storage = contextlib.nullcontext(None)
    else:
        size, expected = container["bytes"], container["sha256"]
        need(type(size) is int and size > 0 and re.fullmatch(r"[a-f0-9]{64}",expected), "Missing published ZIP identity")
        member = safe_name(container["apk_member"])
        storage = tempfile.TemporaryFile()
    with storage as copy, urllib.request.urlopen(url, timeout=60) as response:
        need(response.status == 200 and urlsplit(response.geturl()).scheme == "https", "Unsuccessful HTTPS readback")
        total, digest = 0, hashlib.sha256()
        for chunk in iter(lambda: response.read(1024*1024), b""):
            total += len(chunk)
            need(total <= size, "Downloaded artifact exceeds its published size")
            digest.update(chunk)
            if copy is not None:
                copy.write(chunk)
        need(total == size and digest.hexdigest() == expected, "Downloaded artifact digest or size differs")
        if copy is not None:
            with zipfile.ZipFile(copy) as archive:
                need(archive.namelist().count(member) == 1 and archive.getinfo(member).file_size == apk.stat().st_size,
                     "Missing, duplicate or wrong downloaded APK member")
                actual = hashlib.sha256()
                with archive.open(member) as stream:
                    for chunk in iter(lambda: stream.read(1024*1024), b""):
                        actual.update(chunk)
                need(actual.hexdigest() == sha(apk), "Downloaded ZIP contains a different APK")


def check_build(row, root, repo, context, previous_code, aapt, apksigner, fetch):
    apk = artifact(root, row["apk"])
    build = read(artifact(root, row["build_receipt"]))
    log = artifact(root, row["gradle_log"])
    batch, source = context["batch"], context["sourceCommit"]
    needed = {"sourceCommit": source, "sourceTree": context["sourceTree"], "sourceClean": True,
              "sourceCleanBeforeBuild": True, "sourceUnchangedDuringBuild": True, "gradleRelease": "PASS",
              "exitCode": 0, "wrapperExitCode": 0, "identityError": None, "buildLaunchError": None,
              "gradleLogSha256": sha(log), "apkSha256": row["apk"]["sha256"],
              "r341PlanManifestSha256": context["planSha256"], "r341PlanCommit":context["planCommit"],
              "r341Batch": batch}
    need(all(k in build and type(build[k]) is type(v) and build[k] == v for k,v in needed.items()),
         "Release wrapper, Gradle log, source, APK, batch or new plan identity differs")
    historical_path = ":release/r33_plan_binding.json"
    need(git(repo, "rev-parse", source + historical_path) ==
         git(repo, "rev-parse", context["sourceBaseline"] + historical_path), "R33.5 source pin changed")
    historical = decode(git(repo, "show", source + historical_path))
    need(historical["planRevision"] == "R33.5" and
         re.fullmatch("[0-9a-f]{64}", historical["planManifestSha256"]) and
         build.get("planManifestSha256") == historical["planManifestSha256"],
         "Wrapper's historical plan digest differs from its R33.5 source pin")
    command = build["command"]
    need(isinstance(command, list) and command and Path(command[0]).name == "gradlew" and TASKS <= set(command) and
         type(build["durationNs"]) is int and build["durationNs"] > 0 and log.stat().st_size > 0,
         "Missing real build tasks or log")
    with log.open("rb") as f:
        need(any(re.search(rb"\bBUILD SUCCESSFUL\b", line) for line in f), "Gradle completion missing from original log")
    member = safe_name(row["apk_identity_member"])
    with zipfile.ZipFile(apk) as z:
        names = z.namelist()
        need(member.startswith("assets/") and member in names and "classes.dex" in names and
             "AndroidManifest.xml" in names and len(names) == len(set(names)) and z.testzip() is None,
             "APK program, identity, integrity or duplicate member failure")
        identity = decode(z.read(member))
    need(all(identity.get(k) == val for k,val in context.items()), "APK embedded identity differs")
    binding = decode(git(repo, "show", source + ":release/r341_plan_binding.json"))
    need(all(binding.get(k) == context[k] for k in ("sourceBaseline","planSha256","planCommit")),
         "Application source plan binding differs")
    release = decode(git(repo, "show", source + ":release/release_manifest.json"))
    code, name = identity["versionCode"], identity["versionName"]
    need(type(code) is int and previous_code < code <= 2100000000 and
         release["versionCode"] == code and release["versionName"] == name and release["applicationId"] == PACKAGE,
         "APK version, package or monotonicity differs from source")
    match = re.fullmatch(r"R34\.1-" + batch + r"-(\d{4}-\d{2}-\d{2})-" + str(code), name)
    need(match, "Version name is not for this R34.1 batch")
    datetime.date.fromisoformat(match[1])
    actual = sdk_identity(apk, aapt, apksigner)
    status = "LOCAL_CANDIDATE_SDK_NOT_CHECKED"
    if actual is not None:
        need(actual[:3] == (PACKAGE, code, name), "Android manifest differs from staged identity")
        status = "ORIGINAL_SIGNED" if actual[3] else "SIGNATURE_NOT_ORIGINAL_OR_INVALID"
    downloaded = False
    if fetch and row.get("download_url"):
        check_download(row["download_url"], apk, row.get("download_container"))
        downloaded = True
    return {"batch": batch, "engineering": "VERIFIED", "build": status, "versionCode":code,
            "apk_sha256": row["apk"]["sha256"], "download": "READBACK_IDENTICAL" if downloaded else "NOT_CHECKED"}


def verify_prefix(plan, repo, plan_commit, through, index, root, aapt, apksigner, fetch=False):
    need(isinstance(through,str) and re.fullmatch(r"B(0[1-9]|1[0-2])",through), "Invalid ending batch")
    desc = read(plan/"control/PLAN_IDENTITY.json")
    seal = verify_seal(plan)["manifest_sha256"]
    need(re.fullmatch("[a-f0-9]{40}",plan_commit), "Exact plan commit required")
    target = plan_commit + ":" + desc["plan_path"] + "/MANIFEST_SHA256.json"
    need(git(repo,"rev-parse",target) == git(repo,"hash-object",str(plan/"MANIFEST_SHA256.json")), "Different plan commit")
    rows = index["batches"]
    need(index["schema"] == "STARCORE_R341_BUILD_PREFIX_1" and index["planCommit"] == plan_commit and
         [r["batch"] for r in rows] == [f"B{i:02}" for i in range(1,int(through[1:])+1)],
         "Missing, surplus, duplicated or reordered batch")
    baseline = desc["application_baseline_commit"]
    last_source, last_code = baseline, desc["application_baseline_version_code"]
    output = []
    for row in rows:
        batch = row["batch"]
        receipt_path = artifact(root, row["engineering_receipt"])
        engineering = read(receipt_path)
        need(engineering["batch"] == batch and engineering["status"] == "PASS" and
             engineering["plan_manifest_sha256"] == seal and engineering["level"] == "ENGINEERING",
             "Engineering evidence belongs to another batch, seal or status")
        outcome = validate(plan, engineering, receipt_path.parent, repo, "ENGINEERING")
        need(outcome["status"] == "PASS_RECEIPT_STRUCTURE_AND_DIGESTS", "Engineering receipt not accepted")
        git(repo, "merge-base", "--is-ancestor", last_source, engineering["source_before"])
        source = engineering["source_after"]
        need(source != last_source, "Batch reused the prior source commit")
        tree = git(repo, "rev-parse", source+"^{tree}")
        context = dict(batch=batch,sourceCommit=source,sourceTree=tree,planCommit=plan_commit,
                       planSha256=seal,sourceBaseline=baseline)
        if row.get("apk") is None:
            need(isinstance(row.get("reason"),str) and row["reason"].strip() and
                 row.get("build_receipt") is None and row.get("download_url") is None,
                 "Pending build needs its actual reason and no successful artifacts")
            item = {"batch":batch,"engineering":"VERIFIED","build":"BUILD_PENDING","download":"NOT_CHECKED",
                    "reason":row["reason"]}
        else:
            item = check_build(row, root, repo, context, last_code, aapt, apksigner, fetch)
            last_code = item["versionCode"]
        last_source = source
        output.append(item)
    return output


def main():
    p = argparse.ArgumentParser(description=__doc__)
    for key in ("plan","repo","index"):
        p.add_argument("--"+key,type=Path,required=True)
    p.add_argument("--plan-commit",required=True)
    p.add_argument("--through",required=True)
    p.add_argument("--aapt",default=shutil.which("aapt"))
    p.add_argument("--apksigner",default=shutil.which("apksigner"))
    p.add_argument("--require",choices=("local","signed","published"),default="signed")
    p.add_argument("--verify-downloads",action="store_true",help="Read the authorized publication URLs in the local index")
    a = p.parse_args()
    try:
        need(a.require != "published" or a.verify_downloads, "Published acceptance requires an actual readback")
        rows = verify_prefix(a.plan.resolve(), a.repo.resolve(), a.plan_commit, a.through, read(a.index),
                             a.index.resolve().parent,a.aapt,a.apksigner,a.verify_downloads)
        states = {"LOCAL_CANDIDATE_SDK_NOT_CHECKED","ORIGINAL_SIGNED"} if a.require=="local" else {"ORIGINAL_SIGNED"}
        passed = all(r["build"] in states and (a.require!="published" or r["download"]=="READBACK_IDENTICAL") for r in rows)
        print(json.dumps({"status":"PASS_PREFIX" if passed else "PENDING_PREFIX","criterion":a.require,"batches":rows},
                         ensure_ascii=False))
        return 0 if passed else 2
    except (OSError,KeyError,ValueError,TypeError,zipfile.BadZipFile,subprocess.SubprocessError) as e:
        p.exit(1, f"FAIL_BUILD_PREFIX: {e}\n")


if __name__ == "__main__":
    sys.exit(main())
