#!/usr/bin/env bash
# Keep exit status and capture in one shell; emulator-runner splits YAML lines.
set -euo pipefail
app_apk=$(find app/build/outputs/apk/debug -name '*.apk' -print -quit)
test_apk=$(find app/build/outputs/apk/androidTest/debug -name '*.apk' -print -quit)
test -n "$app_apk"
test -n "$test_apk"
timeout 180s adb install -r "$app_apk"
timeout 180s adb install -r "$test_apk"
timeout 30s adb logcat -c
test_status=0
timeout 180s adb shell am instrument -w -r \
  -e class com.ethk3.novaonepro.R940UiStabilityTest \
  com.ethk3.novaonepro.test/androidx.test.runner.AndroidJUnitRunner \
  > R940_ANDROID_UI.txt 2>&1 || test_status=$?
timeout 30s adb logcat -d -v threadtime > R940_LOGCAT.txt || true
timeout 30s adb shell dumpsys dropbox --print data_app_crash > R940_DROPBOX_CRASH.txt || true
cat R940_ANDROID_UI.txt
grep -E -n 'FATAL EXCEPTION|AndroidRuntime|Process: com.ethk3.novaonepro|Caused by:|R940_|OutOfMemoryError|IllegalStateException' R940_LOGCAT.txt | tail -240 || true
tail -240 R940_DROPBOX_CRASH.txt || true
test "$test_status" -eq 0
! grep -E 'FAILURES!!!|INSTRUMENTATION_FAILED|Process crashed' R940_ANDROID_UI.txt
grep -F 'OK (3 tests)' R940_ANDROID_UI.txt
