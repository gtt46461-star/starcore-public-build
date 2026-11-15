#!/usr/bin/env bash
set -euo pipefail

ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
APK=${1:-"$ROOT/build/r915-release/Starcore-Tianshu-V57.20.1-R5.3.1-915-release.apk"}
BASE=${2:-"$ROOT/build/r914-release/Starcore-Tianshu-V57.20.0-R5.3.0-914-release.apk"}
OUT=${R915_EVIDENCE_DIR:-"$ROOT/build/r915-release/evidence"}
TC=${STARCORE_TOOLCHAIN_ROOT:-"$ROOT/../TOOLCHAIN_USED"}
AAPT2="$TC/aapt/bin/x64/linux/aapt2"
APKSIGNER="$TC/android-signing/apksigner"
ZIPALIGN="$TC/android-signing/zipalign"
LIB="$TC/jadx-node/package/resources/jadx-1.1.0/lib"
TMP=$(mktemp -d "${TMPDIR:-/tmp}/starcore-r915-accept.XXXXXX")
trap 'rm -rf "$TMP"' EXIT HUP INT TERM
mkdir -p "$OUT"

fail() {
    echo "R915_APK_ACCEPTANCE=FAIL|$1" >&2
    exit 1
}

for required in "$APK" "$BASE" "$AAPT2" "$APKSIGNER" "$ZIPALIGN"; do
    [ -f "$required" ] || fail "MISSING_$(basename "$required")"
done

"$ZIPALIGN" -c -p 4 "$APK" > "$OUT/R915_APK_ZIPALIGN.txt" 2>&1 || fail ZIPALIGN
"$APKSIGNER" verify --verbose --print-certs "$APK" > "$OUT/R915_APK_SIGNATURE.txt" 2>&1 \
    || fail SIGNATURE
"$AAPT2" dump badging "$APK" > "$OUT/R915_APK_BADGING.txt" || fail BADGING
rg -q "package: name='com.ethk3.novaonepro' versionCode='577240915' versionName='V57.20.1-R5.3.1-915'" \
    "$OUT/R915_APK_BADGING.txt" || fail IDENTITY
rg -q "minSdkVersion:'21'" "$OUT/R915_APK_BADGING.txt" || fail MIN_SDK
unzip -t "$APK" > "$OUT/R915_APK_ZIP_INTEGRITY.txt" || fail ZIP_INTEGRITY

SIGNED_CERT=$(sed -n 's/^.*certificate SHA-256 digest: //p' "$OUT/R915_APK_SIGNATURE.txt" | head -n 1)
BASE_CERT=$("$APKSIGNER" verify --print-certs "$BASE" 2>/dev/null \
    | sed -n 's/^.*certificate SHA-256 digest: //p' | head -n 1)
[ -n "$SIGNED_CERT" ] && [ "$SIGNED_CERT" = "$BASE_CERT" ] || fail COVER_INSTALL_CERTIFICATE

mkdir -p "$TMP/smali"
dex_count=0
while IFS= read -r dex_name; do
    dex_count=$((dex_count + 1))
    unzip -p "$APK" "$dex_name" > "$TMP/$dex_name"
    mkdir -p "$TMP/smali/$dex_count"
    java -cp "$LIB/*" org.jf.baksmali.Main d "$TMP/$dex_name" -o "$TMP/smali/$dex_count" \
        >> "$OUT/R915_APK_DEX_DECODE.txt" 2>&1 || fail DEX_DECODE
done < <(unzip -Z1 "$APK" | rg '^classes([0-9]+)?\.dex$' | sort -V)
[ "$dex_count" -ge 1 ] || fail DEX_MISSING

for class_name in \
    AutoFetchService \
    SettlementEngine \
    AssetGenesisEngineR915 \
    GenesisAssetGenomeR915 \
    GenesisAssetVmR915 \
    CertifiedResearchMixerR915 \
    ResearchGenomeContractR915 \
    AlgorithmProductionGovernorR915 \
    V51CandidateRealGateR915; do
    found=$(find "$TMP/smali" -path "*/com/ethk3/novaonepro/$class_name.smali" -print -quit)
    [ -n "$found" ] || fail "DEX_CLASS_$class_name"
done

BUILD_CONFIG=$(find "$TMP/smali" -path '*/com/ethk3/novaonepro/BuildConfig.smali' -print -quit)
[ -n "$BUILD_CONFIG" ] || fail BUILD_CONFIG
rg -q 'APPLICATION_ID:Ljava/lang/String; = "com.ethk3.novaonepro"' "$BUILD_CONFIG" \
    || fail DEX_APP_ID
rg -q 'VERSION_NAME:Ljava/lang/String; = "V57.20.1-R5.3.1-915"' "$BUILD_CONFIG" \
    || fail DEX_VERSION_NAME
rg -q 'VERSION_CODE:I = (577240915|0x2267ff53)' "$BUILD_CONFIG" || fail DEX_VERSION_CODE
KEY_A=$(sed -n 's/^\.field public static final DUOQU_API_KEY_A:Ljava\/lang\/String; = "\([A-Za-z0-9][A-Za-z0-9]*\)"$/\1/p' "$BUILD_CONFIG")
KEY_B=$(sed -n 's/^\.field public static final DUOQU_API_KEY_B:Ljava\/lang\/String; = "\([A-Za-z0-9][A-Za-z0-9]*\)"$/\1/p' "$BUILD_CONFIG")
[[ "$KEY_A$KEY_B" =~ ^[A-Za-z0-9]{8,128}$ ]] || fail PROVIDER_KEY_NOT_CARRIED
unset KEY_A KEY_B

sha256sum "$APK" > "$OUT/R915_APK_SHA256.txt"
APK_BYTES=$(stat -c '%s' "$APK")
[ "$APK_BYTES" -gt 1000000 ] || fail APK_SUSPICIOUSLY_SMALL
{
    echo 'R915_APK_ACCEPTANCE=PASS'
    echo 'APPLICATION_ID=com.ethk3.novaonepro'
    echo 'VERSION_NAME=V57.20.1-R5.3.1-915'
    echo 'VERSION_CODE=577240915'
    echo "APK_BYTES=$APK_BYTES"
    echo "DEX_COUNT=$dex_count"
    echo 'R915_RUNTIME_CLASSES=9/9'
    echo 'PROVIDER_KEY_CARRIED=YES_REDACTED'
    echo "CERT_SHA256=$SIGNED_CERT"
    echo 'SIGNATURE_CONTINUITY=PASS_OVERWRITE_INSTALL'
    echo 'ZIPALIGN=PASS'
    echo 'ZIP_INTEGRITY=PASS'
    echo 'PHYSICAL_DEVICE_RUNTIME=RETEST_REQUIRED'
} | tee "$OUT/R915_APK_ACCEPTANCE_SUMMARY.txt"
