#!/usr/bin/env bash
set -euo pipefail
# Emulator-runner executes each YAML line in a fresh shell; keep the failure trap
# and all real checks together in this process so a failing test retains logcat.
collect_runtime_evidence() {
  adb logcat -d > android-logcat.txt || true
  local schema_tmp
  schema_tmp=$(mktemp r26-schema-receipt-XXXXXX.json)
  if adb exec-out run-as com.starcore.research.r13 cat cache/r26-schema-migration-synthetic.json > "$schema_tmp" 2>/dev/null && python3 -c 'import json,sys; assert isinstance(json.load(open(sys.argv[1])),dict)' "$schema_tmp"; then
    mv "$schema_tmp" r26-schema-migration-synthetic.json
  else
    rm -f "$schema_tmp"
  fi
  local api_tmp
  api_tmp=$(mktemp r26-api-receipt-XXXXXX.json)
  if adb exec-out run-as com.starcore.research.r13 cat cache/r26-bundled-api-environment.json > "$api_tmp" 2>/dev/null && python3 -c 'import json,sys; assert isinstance(json.load(open(sys.argv[1])),dict)' "$api_tmp"; then
    mv "$api_tmp" r26-bundled-api-environment.json
  else
    rm -f "$api_tmp"
  fi
  # Release is not debuggable. Keep the already collected debug receipt if run-as is denied
  # after the coverage upgrade, instead of truncating the successful receipt in the exit trap.
  local receipt_tmp
  receipt_tmp=$(mktemp r26-runtime-receipt-XXXXXX.json)
  if adb exec-out run-as com.starcore.research.r13 cat cache/r26-runtime-synthetic.json > "$receipt_tmp" 2>/dev/null && python3 -c 'import json,sys; assert isinstance(json.load(open(sys.argv[1])),dict)' "$receipt_tmp"; then
    mv "$receipt_tmp" r26-runtime-synthetic.json
  else
    rm -f "$receipt_tmp"
  fi
}
trap collect_runtime_evidence EXIT
adb install -r delivered/app/build/outputs/apk/debug/*.apk
if [[ "${1:-29}" == "29" ]]; then
adb install -r delivered/app/build/outputs/apk/androidTest/debug/*.apk
python3 scripts/verify_r26_emulator_heap.py
python3 scripts/run_r26_instrumentation.py
adb exec-out run-as com.starcore.research.r13 cat cache/su1-research.png > r13-research.png
adb exec-out run-as com.starcore.research.r13 cat cache/r13-charts.png > r13-charts.png
adb exec-out run-as com.starcore.research.r13 cat cache/r26-runtime-synthetic.json > r26-runtime-synthetic.json
adb exec-out run-as com.starcore.research.r13 cat cache/r26-schema-migration-synthetic.json > r26-schema-migration-synthetic.json
fi
# The startup probe clears app data; preserve every validated receipt first.
collect_runtime_evidence
python3 scripts/probe_r13_startup.py
# The deliverable is the release variant. Confirm the same fixed certificate
# permits an in-place update, then exercise its actual launcher on every API.
adb install -r delivered/app/build/outputs/apk/release/*.apk
adb shell am force-stop com.starcore.research.r13
adb logcat -c
adb shell am start -W -n com.starcore.research.r13/com.ethk3.novaonepro.MainActivity > release-launcher.txt
python3 - <<'PYRELEASE'
from pathlib import Path
import subprocess,time
text=Path('release-launcher.txt').read_text()
assert 'Status: ok' in text,text
time.sleep(15)
pid=subprocess.check_output(['adb','shell','pidof','com.starcore.research.r13'],text=True).strip()
assert pid,'release process exited'
logs=subprocess.check_output(['adb','logcat','-d'],text=True)
Path('release-logcat.txt').write_text(logs)
assert 'FATAL EXCEPTION' not in logs and 'ANR in com.starcore.research.r13' not in logs
Path('release-runtime.txt').write_text('Release launcher and process survival PASS; package=com.starcore.research.r13; pid='+pid+'\n')
PYRELEASE
