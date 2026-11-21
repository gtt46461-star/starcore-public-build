#!/usr/bin/env bash
set -euo pipefail
p="$RUNNER_TEMP/protocol-candidate"
raw="$RUNNER_TEMP/report-originals"
e="$RUNNER_TEMP/upgrade-evidence"
cd app-source
release=$(find "$p" -name '*-release.apk' -print -quit)
adb install -r "$release" > "$e/target-install.txt"
adb install -r -t "$p/instrument-androidTest.apk" > "$e/test-install.txt"
read -r installed <<< "$(adb shell pm path com.starcore.research.r13 | tr -d '\r' | sed 's/^package://' | head -1)"
actual=$(adb shell sha256sum "$installed" | awk '{print $1}' | tr -d '\r')
original=$(sha256sum "$release" | awk '{print $1}')
test "$actual" = "$original"
adb shell getprop ro.build.fingerprint > "$e/emulator-fingerprint.txt"
adb shell dumpsys package com.starcore.research.r13 > "$e/installed-package.txt"
printf '%s\n' "$actual" > "$e/target-source-apk-sha256.txt"
git rev-parse HEAD > "$e/source-commit.txt"
adb shell pm grant com.starcore.research.r13 android.permission.POST_NOTIFICATIONS
rc=0
python3 -B tools/r31/run_android_joint_protocols.py --out "$raw/joint" --sandbox-prefix r335-058 > "$raw/joint-console.txt" 2>&1 || rc=$?
python3 - "$raw/joint/JOINT-ANDROID-RECEIPT.json" "$e/protocol-counts.json" "$actual" "$rc" <<'PUBLIC'
import json,sys
from pathlib import Path
src,out,apk,code=sys.argv[1:]
v=json.load(open(src))
keys=['scope','sourceCommit','api','startedAt','finishedAt','passedGroups','failedGroups','passedTests','physicalDevice','naturalQuality']
public={k:v[k] for k in keys if k in v}
public['apkSha256']=apk
public['exitCode']=int(code)
public['groups']=[{k:g[k] for k in ['name','classes','status','expectedTests','tests','matched','sha256','rawSha256','startedAt','finishedAt'] if k in g} for g in v.get('groups',[])]
Path(out).write_text(json.dumps(public,indent=2)+'
')
PUBLIC
exit "$rc"
