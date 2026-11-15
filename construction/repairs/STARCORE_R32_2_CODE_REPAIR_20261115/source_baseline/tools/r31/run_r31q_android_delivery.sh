#!/usr/bin/env bash
# Clean disposable emulator only. Actual services, SQLite and provider readback.
set -euo pipefail
candidate=${1:?candidate directory}
out=${2:?evidence directory}
mkdir -p "$out"
adb wait-for-device
release_apk=$(find "$candidate" -name '*-release.apk' -print -quit)
test -n "$release_apk"
test "$(adb shell getprop ro.kernel.qemu | tr -d '\r')" = 1
adb install -r "$release_apk" > "$out/release-install.txt"
adb install -r -t "$(find "$candidate" -name '*-androidTest.apk' -print -quit)" > "$out/instrumentation-install.txt"
# The test APK has the same signer as the release target. Exercise the exact
# release bytes, not a debug overlay with the same package/version label.
installed_path=$(adb shell pm path com.starcore.research.r13 | tr -d '\r' | sed -n 's/^package://p' | head -1)
test -n "$installed_path"
expected_apk_sha=$(sha256sum "$release_apk" | cut -d' ' -f1)
actual_apk_sha=$(adb shell sha256sum "$installed_path" | tr -d '\r' | cut -d' ' -f1)
test "$expected_apk_sha" = "$actual_apk_sha"
printf '%s\n' "$actual_apk_sha" > "$out/installed-release-sha256.txt"
adb shell am force-stop com.starcore.research.r13
adb logcat -c
adb shell am instrument -w -r -e r30Sandbox "r31q-delivery-${GITHUB_RUN_ID:-local}" -e r31qRealService true -e class com.ethk3.novaonepro.R31QDeliveryInstrumentedTest com.starcore.research.r13.test/com.ethk3.novaonepro.R30FaultAndroidRunner > "$out/delivery-protocol.txt" 2>&1
adb logcat -d -v threadtime > "$out/logcat-private.txt"
python3 - "$out" <<'PY'
import hashlib,json,pathlib,re,sys
sys.path.insert(0,'tools/r31')
from android_delivery_protocol import verify
out=pathlib.Path(sys.argv[1]);p=out/'logcat-private.txt'
clean=re.sub(r'(?i)((?:api[_-]?key|password|token|key)\s*[=:]\s*)[^\s&,;]+',r'\1[REDACTED]',p.read_text(errors='replace'))
clean=re.sub(r'(?i)Bearer\s+[^\s]+','Bearer [REDACTED]',clean)
(out/'logcat-redacted.txt').write_text(clean);p.unlink()
raw=(out/'delivery-protocol.txt').read_bytes();text=raw.decode(errors='replace')
print(text[-16000:])
found=re.search(r'OK \((\d+) tests?\)',text)
transport=verify(text,clean)
(out/'ANDROID_PROTOCOL_VERIFICATION.json').write_text(json.dumps(transport,indent=2)+'\n')
ok=transport['passed']
(out/'DELIVERY_REGRESSION.json').write_text(json.dumps({'status':'PASS_ANDROID_EMULATOR' if ok else 'FAIL','tests':int(found.group(1)) if found else None,'protocolSha256':hashlib.sha256(raw).hexdigest(),'installedReleaseSha256':(out/'installed-release-sha256.txt').read_text().strip(),'scope':'exact release APK; real Android main-process request, diagnostics Service, published ZIP semantics and Provider readback; real API freeze/settlement smoke; not handset, SAF picker or natural quality','handset':'NOT_RUN','24h':'NOT_RUN','72h':'NOT_RUN','naturalQuality':'NOT_PROVEN'},indent=2)+'\n')
assert ok,'R31Q_REAL_SERVICE_EXPORT_REGRESSION_FAILED'
PY
