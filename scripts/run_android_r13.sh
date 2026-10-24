#!/usr/bin/env bash
set -euo pipefail
# Emulator-runner executes each YAML line in a fresh shell; keep the failure trap
# and all real checks together in this process so a failing test retains logcat.
trap 'adb logcat -d > android-logcat.txt || true' EXIT
adb install -r delivered/app/build/outputs/apk/debug/*.apk
if [[ "${1:-29}" == "29" ]]; then
adb install -r delivered/app/build/outputs/apk/androidTest/debug/*.apk
adb shell am instrument -w -e class com.ethk3.novaonepro.strategyv2.ResearchUiDeviceSU1Test,com.ethk3.novaonepro.strategyv2.SchemaAndLiveSU1Test,com.ethk3.novaonepro.strategyv2.ProgramTreeDeviceSU1Test,com.ethk3.novaonepro.strategyv2.AtomKernelDeviceSU1Test,com.ethk3.novaonepro.strategyv2.TrainingParityDeviceSU1Test,com.ethk3.novaonepro.strategyv2.BundledApiDeviceSU1Test,com.ethk3.novaonepro.strategyv2.RelGraphDeviceSU1Test,com.ethk3.novaonepro.strategyv2.ResearchSchedulingDeviceSU1Test,com.ethk3.novaonepro.strategyv2.SchemaAndChartsR13Test,com.ethk3.novaonepro.strategyv2.StartupLeaseR13Test com.starcore.research.r13.test/com.ethk3.novaonepro.strategyv2.Su1AndroidTestRunner > android-r13.txt
cat android-r13.txt
python3 -c 'from pathlib import Path; x=Path("android-r13.txt").read_text(); assert "OK (26 tests)" in x and "FAILURES" not in x, x'
adb exec-out run-as com.starcore.research.r13 cat cache/su1-research.png > r13-research.png
adb exec-out run-as com.starcore.research.r13 cat cache/r13-charts.png > r13-charts.png
fi
python3 scripts/probe_r13_startup.py
