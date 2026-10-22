#!/usr/bin/env python3
"""Read-only recovery of an existing job's oversized lossless evidence log.

Retains original receipts, instrumentation logs and unit XML bytes. Large fixture
and vector bytes remain in the original checksum-verified job evidence archive.
No APK, credentials, private Android properties or repository files are emitted.
"""
import argparse
import base64
import hashlib
import io
import json
import os
import re
import time
import urllib.error
import urllib.request
import zipfile

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument("--job", type=int, required=True)
parser.add_argument("--commit", required=True)
args = parser.parse_args()
repo = os.environ["GITHUB_REPOSITORY"]
if repo != "gtt46461-star/eth-k3-v43-nova" or not re.fullmatch(r"[0-9a-f]{40}", args.commit):
    raise SystemExit("RECOVERY_INPUT_IDENTITY")
base = f"https://api.github.com/repos/{repo}/actions/jobs/{args.job}"


class NoRedirect(urllib.request.HTTPRedirectHandler):
    def redirect_request(self, request, fp, code, message, headers, url):
        return None


opener = urllib.request.build_opener(NoRedirect)


def request_api(url):
    return opener.open(urllib.request.Request(url, headers={
        "Authorization": "Bearer " + os.environ["GH_TOKEN"],
        "Accept": "application/vnd.github+json", "X-GitHub-Api-Version": "2022-11-28",
    }), timeout=60)


for attempt in range(241):
    with request_api(base) as response:
        job = json.load(response)
    if job["head_sha"] != args.commit:
        raise SystemExit("RECOVERY_JOB_COMMIT_MISMATCH")
    if job["status"] == "completed":
        break
    if attempt == 240:
        raise SystemExit("RECOVERY_SOURCE_JOB_NOT_COMPLETE")
    time.sleep(5)
try:
    with request_api(base + "/logs") as response:
        raw_log = response.read()
except urllib.error.HTTPError as redirect:
    if redirect.code != 302:
        raise
    location = redirect.headers["Location"]
    if not location.startswith("https://"):
        raise SystemExit("RECOVERY_LOG_REDIRECT_NOT_HTTPS")
    # Never forward the repository token to the signed storage URL.
    with urllib.request.urlopen(location, timeout=60) as response:
        raw_log = response.read()
log = raw_log.decode("utf-8")
manifest = json.loads(re.search(r"O09_EVIDENCE_MANIFEST\|(\{[^\n]+\})", log).group(1))
chunks = re.findall(r"O09_EVIDENCE_CHUNK\|(\d+)\|([A-Za-z0-9+/=]+)", log)
ordered = sorted((int(i), value) for i, value in chunks)
if [i for i, _ in ordered] != list(range(len(ordered))):
    raise SystemExit("RECOVERY_CHUNK_SEQUENCE")
original = base64.b64decode("".join(value for _, value in ordered), validate=True)
if hashlib.sha256(original).hexdigest() != manifest["sha256"] or len(original) != manifest["bytes"]:
    raise SystemExit("RECOVERY_ORIGINAL_ZIP_IDENTITY")
selected = []
small = io.BytesIO()
with zipfile.ZipFile(io.BytesIO(original)) as archive, zipfile.ZipFile(small, "w", zipfile.ZIP_DEFLATED, 9) as target:
    for name in archive.namelist():
        keep = (name.startswith("validation-o09/") and name.endswith("receipt.json"))
        keep |= name in ("validation-o09/android-rolling-result.json", "validation-o09/d04/d04_android_metrics.json",
                         "validation-o09/d04/online-fairness.json", "validation-o09/input-commit.txt",
                         "validation-o09/runtime/D04-host-android-compare.log")
        keep |= name in ("validation-o09/runtime/FoundationImportAndroidCP12Test-logcat.log",
                         "validation-o09/runtime/ShadowCycleAndroidTest-logcat.log",
                         "validation-o09/runtime/install-app.log",
                         "validation-o09/runtime/install-test.log",
                         "validation-o09/runtime/emulator.log",
                         "validation-o09/runtime/ResearchContinuationAndroidTest-logcat.log")
        keep |= bool(re.fullmatch(r"validation-o09/runtime/[A-Za-z0-9]+Test\.log", name))
        keep |= bool(re.fullmatch(r"validation-o09/cp9-kernel/[^/]+\.(?:json|log)", name))
        keep |= name in ("validation-o09/android-validation.log", "validation-o09/android-test-build.log",
                         "validation-o09/d04-prepare.log")
        keep |= bool(re.fullmatch(r"app/build/test-results/testDebugUnitTest/TEST-[^/]+\.xml", name))
        keep |= bool(re.fullmatch(r"validation-o09/runtime/install-[A-Za-z0-9-]+\.log", name))
        if keep:
            data = archive.read(name)
            target.writestr(name, data)
            selected.append({"path": name, "bytes": len(data), "sha256": hashlib.sha256(data).hexdigest()})
if not selected:
    raise SystemExit("RECOVERY_SELECTED_EVIDENCE_MISSING")
data = small.getvalue()
if len(data) > 1024 * 1024:
    raise SystemExit("RECOVERY_SELECTED_ARCHIVE_TOO_LARGE")
print("O09_RECOVERY_MANIFEST|" + json.dumps({"job": args.job, "commit": args.commit,
    "source_log_sha256": hashlib.sha256(raw_log).hexdigest(), "original_evidence": manifest,
    "selected_files": selected, "bytes": len(data), "sha256": hashlib.sha256(data).hexdigest(),
    "scope": "EXACT_SUBSET_OF_VERIFIED_ORIGINAL_ARCHIVE_NOT_A_NEW_TEST_RUN"}), flush=True)
encoded = base64.b64encode(data).decode("ascii")
for index, offset in enumerate(range(0, len(encoded), 4096)):
    print(f"O09_RECOVERY_CHUNK|{index}|{encoded[offset:offset + 4096]}", flush=True)
for line in log.splitlines():
    if "O09_EVIDENCE_CHUNK" not in line and any(marker in line for marker in (
            "O09_ANDROID_CLASS_RESULT|", "O09_D04_HOST_PREPARED|", "O09_ANDROID_RUNTIME_RECEIPT|")):
        print(line, flush=True)
