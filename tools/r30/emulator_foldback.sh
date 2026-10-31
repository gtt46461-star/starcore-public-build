#!/usr/bin/env bash
# R30-B00-03 step 5: run R30FoldBackInstrumentedTest on the disposable emulator only (it rewrites the app database).
# Usage: emulator_foldback.sh <dir with *debug.apk> <dir with androidTest apk> <evidence dir>
set -euo pipefail
DELIVERED=$1; TESTS=$2; OUT=$3; mkdir -p "$OUT"
serial=$(adb get-serialno)
[[ "$serial" == emulator-* ]] || { echo 'EMULATOR_REQUIRED'; exit 1; }
APK=$(find "$DELIVERED" -maxdepth 1 -name '*debug.apk' -print -quit); TAPK=$(find "$TESTS" -maxdepth 1 -name '*.apk' -print -quit)
[ -n "$APK" ] && [ -n "$TAPK" ]
adb install -r -t "$APK" > "$OUT/foldback-install-app.txt"
adb install -r -t "$TAPK" > "$OUT/foldback-install-test.txt"
adb shell am instrument -w -r -e class com.ethk3.novaonepro.R30FoldBackInstrumentedTest \
  com.starcore.research.r13.test/com.ethk3.novaonepro.strategyv2.Su1AndroidTestRunner | tee "$OUT/foldback-instrument.txt"
grep -q 'OK (4 tests)' "$OUT/foldback-instrument.txt" || { echo 'R30_FOLDBACK_INSTRUMENTED_FAIL'; exit 1; }
echo 'R30_FOLDBACK_INSTRUMENTED_PASS (API34 emulator, not a physical device)' | tee "$OUT/foldback-result.txt"
