#!/usr/bin/env bash
set -euo pipefail
ROOT=$(cd "$(dirname "$0")/.." && pwd)
APK=${1:?signed APK required}
BASE=${2:?signed predecessor APK required}
TC=${STARCORE_TOOLCHAIN_ROOT:?STARCORE_TOOLCHAIN_ROOT required}
OUT=${R952_APK_EVIDENCE_DIR:-"$ROOT/build/r957-release/evidence/r952-apk"}
mkdir -p "$OUT"
TMP=$(mktemp -d "${TMPDIR:-/tmp}/r952-apk.XXXXXX"); trap 'rm -rf "$TMP"' EXIT
"$TC/android-signing/zipalign" -c -p 4 "$APK"
"$TC/android-signing/apksigner" verify --verbose --print-certs "$APK" > "$OUT/signature.txt"
"$TC/aapt/bin/x64/linux/aapt2" dump badging "$APK" > "$OUT/badging.txt"
rg -q "package: name='com.ethk3.novaonepro' versionCode='577250957' versionName='V57.21.2.0.0-R5.4.2-957'" "$OUT/badging.txt"
rg -q "minSdkVersion:'21'" "$OUT/badging.txt"
unzip -t "$APK" > "$OUT/zip_integrity.txt"
new_cert=$(sed -n 's/^.*certificate SHA-256 digest: //p' "$OUT/signature.txt" | head -n1)
base_cert=$("$TC/android-signing/apksigner" verify --print-certs "$BASE" 2>/dev/null | sed -n 's/^.*certificate SHA-256 digest: //p' | head -n1)
[[ -n "$new_cert" && "$new_cert" == "$base_cert" ]]
java -jar "$TC/apktool/apktool.jar" d -s -f -o "$TMP/decoded" "$APK" > "$OUT/apktool_decode.txt" 2>&1
mkdir -p "$TMP/smali"
while IFS= read -r dex; do unzip -p "$APK" "$dex" > "$TMP/$dex"; java -cp "$TC/jadx-node/package/resources/jadx-1.1.0/lib/*" org.jf.baksmali.Main d "$TMP/$dex" -o "$TMP/smali"; done < <(unzip -Z1 "$APK" | rg '^classes([0-9]+)?\.dex$' | sort -V)
for class in ResearchDirectorR952 ResearchOutboxR952 ResearchMigrationRegistryR952 CandidateSandboxR952 FullbloodDiagnosticsProviderR952; do test -f "$TMP/smali/com/ethk3/novaonepro/r952/$class.smali"; done
rg -q 'ResearchSettlementHookRegistryR952;->dispatchCompleted' "$TMP/smali/com/ethk3/novaonepro/SettlementCoordinatorR815.smali"
rg -q 'ResearchDirectorR952;->capturePredraw' "$TMP/smali/com/ethk3/novaonepro/ResearchPredrawCaptureR923.smali"
python3 "$ROOT/verification/r922/r922_apk_resource_contract.py" "$TMP/decoded" "$TMP/smali" "$ROOT/app/src/main/java/com/ethk3/novaonepro" > "$OUT/resource_contract.txt"
sha256sum "$APK" > "$OUT/apk.sha256"
printf 'R952_APK_ACCEPTANCE=PASS|VERSION_CODE=577250957|CERT_SHA256=%s|R952_DEX=PASS|RESOURCE_PARITY=PASS\n' "$new_cert" | tee "$OUT/summary.txt"
