#!/usr/bin/env bash
set -euo pipefail
mkdir -p evidence/r29/integration
INTEGRATION_OUT=evidence/r29/integration
adb install -r "$(find app/build/outputs/apk/debug -name '*.apk' -print -quit)"
adb install -r "$(find app/build/outputs/apk/androidTest/debug -name '*.apk' -print -quit)"
adb logcat -c
set +e
adb shell am instrument -w -r -e class com.ethk3.novaonepro.strategyv2.R29DatabaseCutoverDeviceTest com.starcore.research.r13.test/com.ethk3.novaonepro.strategyv2.Su1AndroidTestRunner | tee "$INTEGRATION_OUT/instrumentation.txt"
INTEGRATION_STATUS=${PIPESTATUS[0]}
set -e
adb logcat -d > "$INTEGRATION_OUT/logcat.txt"
adb exec-out run-as com.starcore.research.r13 cat files/r29-cutover-device.json > "$INTEGRATION_OUT/r29-cutover-device.json" || true
python3 - <<'PY'
import json,pathlib
p=pathlib.Path('evidence/r29/integration')
text=(p/'instrumentation.txt').read_text()
assert 'OK (1 test)' in text and 'FAILURES!!!' not in text and 'INSTRUMENTATION_FAILED' not in text,text[-4000:]
r=json.loads((p/'r29-cutover-device.json').read_text())
assert r['status']=='PASS_ANDROID_ENGINEERING_SUBSET'
print(json.dumps(r,ensure_ascii=False))
PY
test "$INTEGRATION_STATUS" -eq 0
adb shell am force-stop com.starcore.research.r13
adb logcat -c
set +e
adb shell am instrument -w -r -e class com.ethk3.novaonepro.strategyv2.R26RuntimeDeviceTest com.starcore.research.r13.test/com.ethk3.novaonepro.strategyv2.Su1AndroidTestRunner | tee "$INTEGRATION_OUT/full-runtime-instrumentation.txt"
INTEGRATION_STATUS=${PIPESTATUS[0]}
set -e
adb logcat -d > "$INTEGRATION_OUT/full-runtime-logcat.txt"
adb exec-out run-as com.starcore.research.r13 cat cache/r26-runtime-synthetic.json > "$INTEGRATION_OUT/r26-runtime-synthetic.json" || true
python3 - <<'PY'
import json,pathlib
p=pathlib.Path('evidence/r29/integration');text=(p/'full-runtime-instrumentation.txt').read_text()
assert 'OK (1 test)' in text and 'FAILURES!!!' not in text and 'INSTRUMENTATION_FAILED' not in text,text[-5000:]
r=json.loads((p/'r26-runtime-synthetic.json').read_text())
assert r['result']=='PASS_WITH_STATED_ENGINEERING_SCOPE' and r['r29IndependentResearchActive'] and r['legacyDispatchOwner']=='RESEARCH_POOL'
print(json.dumps({k:r.get(k) for k in ['result','familyMechanismsWithCommittedProgress','expertsWithActualReceipts','r29TaskErrors','retainedLegacyStateExports','restartResumed','staleOwnerRejected','committedServiceCapacityFailures']},ensure_ascii=False))
PY
exit "$INTEGRATION_STATUS"
