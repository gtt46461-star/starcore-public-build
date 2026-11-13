#!/usr/bin/env bash
set -euo pipefail
candidate=$(realpath "${1:?built APKs}")
out=$(realpath -m "${2:?public result directory}")
mkdir -p "$out"
bash tools/r30-repair/prepare_emulator.sh
test "$(cat "$candidate/r31v/source-commit.txt")" = "$(git rev-parse HEAD)"
apk=$(find "$candidate/app/build/outputs/apk/release" -name '*-release.apk' -type f)
test_apk=$(find "$candidate/app/build/outputs/apk/androidTest/debug" -name '*-androidTest.apk' -type f)
test -n "$apk" && test -n "$test_apk"
adb install -r "$apk" > /dev/null
adb install -r -t "$test_apk" > /dev/null
installed=$(adb shell pm path com.starcore.research.r13 | tr -d '\r' | sed -n 's/^package://p')
apk_sha=$(sha256sum "$apk" | cut -d ' ' -f1)
installed_sha=$(adb shell sha256sum "$installed" | tr -d '\r' | cut -d ' ' -f1)
test "$apk_sha" = "$installed_sha"
export R31_TARGETED_APK_SHA="$apk_sha" R31_TARGETED_OUT="$out"
private=$(mktemp -d "${RUNNER_TEMP:-/tmp}/r31-critical-private.XXXXXX")
trap 'rm -rf "$private"' EXIT
adb logcat -c
set +e
timeout 2400s adb shell am instrument -w -r -e r30Sandbox "critical-targeted-${GITHUB_RUN_ID:-local}" -e r31qRealService true -e r31RestoreDisposable true -e class com.ethk3.novaonepro.SchemaStampReasonAndroidTest,com.ethk3.novaonepro.R31RestoreProofInstrumentedTest com.starcore.research.r13.test/com.ethk3.novaonepro.R30FaultAndroidRunner > "$private/protocol.txt" 2>&1
instrumentation_exit=$?
set -e
python3 - "$private/protocol.txt" "$instrumentation_exit" <<'PY'
import hashlib,json,os,pathlib,re,subprocess,sys
raw=pathlib.Path(sys.argv[1]).read_bytes();text=raw.decode(errors='replace')
expected={'reportsMismatchWithoutAcceptingDrift','completedUpgradeAcceptsOnlyVerifiedResearchPinAdditions','report003SchemaReproducesLegacyStampWithoutMigration','independentRestoreBindsActualPidCloneAndCurrentBytesBeforeSeparateCleanup'}
done=set(re.findall(r'INSTRUMENTATION_STATUS: test=([^\r\n]+)\r?\nINSTRUMENTATION_STATUS_CODE: 0',text))
ok=sys.argv[2]=='0' and bool(re.search(r'OK \(4 tests\)',text)) and expected==done and not any(x in text for x in ['FAILURES!!!','INSTRUMENTATION_FAILED','Process crashed'])
public='\n'.join(p.read_text(errors='replace') for p in pathlib.Path('app/src').rglob('*.java'))
known=set(re.findall(r'\b[A-Z][A-Z0-9_]{2,80}\b',public));files={p.name for p in pathlib.Path('app/src').rglob('*.java')}
frames=[list(row) for row in re.findall(r'at (com\.ethk3\.novaonepro\.[A-Za-z0-9_.$]+)\(([A-Za-z0-9_]+\.java):([0-9]+)\)',text) if row[1] in files]
result={'sourceCommit':subprocess.check_output(['git','rev-parse','HEAD'],text=True).strip(),'apkSha256':os.environ['R31_TARGETED_APK_SHA'],'status':'PASS_TARGETED_ANDROID_EMULATOR' if ok else 'FAILED_OR_INCOMPLETE','expectedTests':4,'completedTests':sorted(done),'exitCode':int(sys.argv[2]),'protocolSha256':hashlib.sha256(raw).hexdigest(),'publicFailureSymbols':sorted(known.intersection(re.findall(r'\b[A-Z][A-Z0-9_]{2,80}\b',text))),'sourceFrames':frames[:60],'api':subprocess.check_output(['adb','shell','getprop','ro.build.version.sdk'],text=True).strip(),'scope':'EXACT_SIGNED_RELEASE_REAL_ANDROID_RESTORE_AND_WHOLE_CRITICAL_SCHEMA_FIXTURES; SYNTHETIC_RESTORE_INPUT_ONLY; RAW_PROTOCOL_AND_DATABASE_NOT_PUBLISHED','physicalDevice':'NOT_RUN','24h':'NOT_RUN','72h':'NOT_RUN','naturalQuality':'NOT_PROVEN'}
(pathlib.Path(os.environ['R31_TARGETED_OUT'])/'SELECTED_ANDROID_RECEIPT.json').write_text(json.dumps(result,indent=2)+'\n');print(json.dumps(result))
assert ok,'TARGETED_CRITICAL_RESTORE_REGRESSION_FAILED'
PY
