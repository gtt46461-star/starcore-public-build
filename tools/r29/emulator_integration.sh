#!/usr/bin/env bash
set -euo pipefail
mkdir -p evidence/r29/integration
INTEGRATION_OUT=evidence/r29/integration
adb install -r "$(find app/build/outputs/apk/debug -name '*.apk' -print -quit)"
adb install -r "$(find app/build/outputs/apk/androidTest/debug -name '*.apk' -print -quit)"
INTEGRATION_FAILED=0
run_case() {
    local device_class=$1 device_prefix=$2 device_receipt=$3
    local instrument_file=${device_prefix:+$device_prefix-}instrumentation.txt
    local logcat_file=${device_prefix:+$device_prefix-}logcat.txt
    adb shell am force-stop com.starcore.research.r13
    adb logcat -c
    set +e
    adb shell am instrument -w -r -e class "com.ethk3.novaonepro.strategyv2.$device_class" com.starcore.research.r13.test/com.ethk3.novaonepro.strategyv2.Su1AndroidTestRunner | tee "$INTEGRATION_OUT/$instrument_file"
    local device_status=${PIPESTATUS[0]}
    set -e
    adb logcat -d > "$INTEGRATION_OUT/$logcat_file"
    adb exec-out run-as com.starcore.research.r13 cat "$device_receipt" > "$INTEGRATION_OUT/${device_receipt##*/}" || true
    if ! python3 - "$device_class" "$instrument_file" "${device_receipt##*/}" <<'PY'
import json,pathlib,sys
p=pathlib.Path('evidence/r29/integration');name=sys.argv[1];text=(p/sys.argv[2]).read_text()
assert 'OK (1 test)' in text and 'FAILURES!!!' not in text and 'INSTRUMENTATION_FAILED' not in text,text[-5000:]
r=json.loads((p/sys.argv[3]).read_text())
if name=='R29LargeModelCheckpointDeviceTest':
    assert r['result']=='PASS_WITH_STATED_ANDROID_SCOPE' and r['legacyWholeRowReadRejectedByActualAndroid'] and r['completeWeightsAdamRngFtrlReplayStateAndNextUpdateEqual']
elif name=='R29LargeRowMigrationDeviceTest':
    assert r['result']=='PASS_WITH_STATED_ANDROID_SCOPE' and r['wholeRowNegativeControlRejectedByActualAndroid']
    assert all(r[k] for k in ['typedPayloadAndHashesExact','physicalAndCompositePrimaryKeyPaginationExact','stagedIndependentRehashPassed','legacyDomainUpgradeCapturePassed','damagedTransferRejected'])
elif name=='R29DatabaseCutoverDeviceTest':
    assert r['status']=='PASS_ANDROID_ENGINEERING_SUBSET' and r['legacyScopedWriteIndependentAndOriginalRowsPreserved'] and r['preparedFrameWithActualAndroidPostCommitReceipt']
else:
    assert r['result']=='PASS_WITH_STATED_ENGINEERING_SCOPE' and r['r29IndependentResearchActive'] and r['legacyDispatchOwner']=='RESEARCH_POOL'
    assert r['firstServiceTargetObservedWithin'] and r['allResearchTaskErrors']==0 and r['retentionBoundaryRenewed'] and r['restartResumed']
print(json.dumps({k:v for k,v in r.items() if not isinstance(v,(dict,list))},ensure_ascii=False))
PY
    then INTEGRATION_FAILED=1; fi
    if [ "$device_status" -ne 0 ]; then INTEGRATION_FAILED=1; fi
}
# Each case gets a fresh process and an isolated fixture. Retain every failure in this single pinned-source run.
run_case R29LargeModelCheckpointDeviceTest large-model files/r29-model-cursorwindow-device.json
run_case R29LargeRowMigrationDeviceTest large-rows files/r29-large-row-migration-device.json
run_case R29DatabaseCutoverDeviceTest '' files/r29-cutover-device.json
run_case R26RuntimeDeviceTest full-runtime cache/r26-runtime-synthetic.json
exit "$INTEGRATION_FAILED"
