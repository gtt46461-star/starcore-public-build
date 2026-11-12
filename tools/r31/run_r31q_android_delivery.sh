#!/usr/bin/env bash
# Clean disposable emulator only. Actual services, SQLite and provider readback.
set -euo pipefail
candidate=${1:?candidate directory}
out=${2:?evidence directory}
mkdir -p "$out"
adb wait-for-device
adb install -r "$(find "$candidate" -name '*-release.apk' -print -quit)" > "$out/release-install.txt"
adb install -r "$(find "$candidate" -name '*-debug.apk' -print -quit)" > "$out/debug-cover-install.txt"
adb install -r -t "$(find "$candidate" -name '*-androidTest.apk' -print -quit)" > "$out/instrumentation-install.txt"
adb shell am force-stop com.starcore.research.r13
adb logcat -c
adb shell am instrument -w -r -e r30Sandbox "r31q-delivery-${GITHUB_RUN_ID:-local}" -e r31qRealService true -e class com.ethk3.novaonepro.R31QDeliveryInstrumentedTest com.starcore.research.r13.test/com.ethk3.novaonepro.R30FaultAndroidRunner > "$out/delivery-protocol.txt" 2>&1
adb logcat -d -v threadtime > "$out/logcat-private.txt"
python3 - "$out" <<'PY'
import hashlib,json,pathlib,re,sys
out=pathlib.Path(sys.argv[1]);p=out/'logcat-private.txt'
clean=re.sub(r'(?i)((?:api[_-]?key|password|token|key)\s*[=:]\s*)[^\s&,;]+',r'\1[REDACTED]',p.read_text(errors='replace'))
clean=re.sub(r'(?i)Bearer\s+[^\s]+','Bearer [REDACTED]',clean)
(out/'logcat-redacted.txt').write_text(clean);p.unlink()
raw=(out/'delivery-protocol.txt').read_bytes();text=raw.decode(errors='replace')
print(text[-16000:])
found=re.search(r'OK \((\d+) tests?\)',text)
ok=bool(found and int(found.group(1))==4 and 'FAILURES!!!' not in text and 'FULL_REPORT_PUBLISHED|PROVIDER_READBACK_HASH_PASS' in text and 'REAL_API_FETCH_AND_NEW_FREEZE_SETTLEMENT' in text and 'FAILED_PROBE_NOT_SWALLOWED' in text)
(out/'DELIVERY_REGRESSION.json').write_text(json.dumps({'status':'PASS_ANDROID_EMULATOR' if ok else 'FAIL','tests':int(found.group(1)) if found else None,'protocolSha256':hashlib.sha256(raw).hexdigest(),'scope':'real Android main-process request, diagnostics Service, published ZIP and ContentProvider hash readback; injected/empty data; not handset or natural quality','handset':'NOT_RUN','24h':'NOT_RUN','72h':'NOT_RUN','naturalQuality':'NOT_PROVEN'},indent=2)+'\n')
assert ok,'R31Q_REAL_SERVICE_EXPORT_REGRESSION_FAILED'
PY
