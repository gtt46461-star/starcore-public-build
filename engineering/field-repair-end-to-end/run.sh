#!/usr/bin/env bash
set -euo pipefail
cd app-source
export SOURCE="$(git rev-parse HEAD)"
CONTEXT=$(python3 -c 'import base64,json,os; print(base64.urlsafe_b64encode(json.dumps({"sourceCommit":os.environ["SOURCE"]}).encode()).decode())')
for attempt in 1 2 3; do
  if ./gradlew --no-daemon --max-workers=4 :app:connectedDebugAndroidTest -Pr30Instrumentation=true -Pandroid.testInstrumentationRunnerArguments.class=com.ethk3.novaonepro.Field173B12ReportTest#reportPositive -Pandroid.testInstrumentationRunnerArguments.r30Sandbox=R343_57731121157731121157731121110101 -Pandroid.testInstrumentationRunnerArguments.field173Context="$CONTEXT"; then break; fi
  if [ "$attempt" = 3 ]; then exit 1; fi
  sleep 25
done
export SOURCE="$(git rev-parse HEAD)"
export VERSION="$(jq -er '.versionCode' release/release_manifest.json)"
shopt -s nullglob
apks=(app/build/outputs/apk/debug/*.apk)
[[ ${#apks[@]} == 1 ]]
cp "${apks[0]}" "$RUNNER_TEMP/candidate-tested.apk"
ln -s app-source ../source
cd ..
python3 -B "$RUNNER_TEMP/phone.py"
