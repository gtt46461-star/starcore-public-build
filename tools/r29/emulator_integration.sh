#!/usr/bin/env bash
set -euo pipefail
mkdir -p evidence/r29/integration
INTEGRATION_OUT=evidence/r29/integration
adb install -r "$(find app/build/outputs/apk/debug -name '*.apk' -print -quit)"
adb install -r "$(find app/build/outputs/apk/androidTest/debug -name '*.apk' -print -quit)"
for fixture in model rows; do
    if [ "$fixture" = model ]; then
        DEVICE_CLASS=R29LargeModelCheckpointDeviceTest
        DEVICE_RECEIPT=r29-model-cursorwindow-device.json
    else
        DEVICE_CLASS=R29LargeRowMigrationDeviceTest
        DEVICE_RECEIPT=r29-large-row-migration-device.json
    fi
    adb shell am force-stop com.starcore.research.r13
    adb logcat -c
    set +e
    adb shell am instrument -w -r -e class "com.ethk3.novaonepro.strategyv2.$DEVICE_CLASS" com.starcore.research.r13.test/com.ethk3.novaonepro.strategyv2.Su1AndroidTestRunner | tee "$INTEGRATION_OUT/large-$fixture-instrumentation.txt"
    FIXTURE_STATUS=${PIPESTATUS[0]}
    set -e
    adb logcat -d > "$INTEGRATION_OUT/large-$fixture-logcat.txt"
    adb exec-out run-as com.starcore.research.r13 cat "files/$DEVICE_RECEIPT" > "$INTEGRATION_OUT/$DEVICE_RECEIPT" || true
    python3 - "$fixture" "$DEVICE_RECEIPT" <<'PYFIXTURE'
import json,pathlib,sys
p=pathlib.Path('evidence/r29/integration');text=(p/('large-'+sys.argv[1]+'-instrumentation.txt')).read_text()
assert 'OK (1 test)' in text and 'FAILURES!!!' not in text and 'INSTRUMENTATION_FAILED' not in text,text[-4000:]
r=json.loads((p/sys.argv[2]).read_text());assert r['result']=='PASS_WITH_STATED_ANDROID_SCOPE'
if sys.argv[1]=='model':
    assert r['legacyWholeRowReadRejectedByActualAndroid'] and r['completeWeightsAdamRngFtrlReplayStateAndNextUpdateEqual']
else:
    assert r['typedPayloadAndHashesExact'] and r['physicalAndCompositePrimaryKeyPaginationExact'] and r['stagedIndependentRehashPassed'] and r['legacyDomainUpgradeCapturePassed'] and r['damagedTransferRejected']
print(json.dumps(r,ensure_ascii=False))
PYFIXTURE
    test "$FIXTURE_STATUS" -eq 0
done
adb shell am force-stop com.starcore.research.r13
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
