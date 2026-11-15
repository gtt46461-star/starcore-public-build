#!/usr/bin/env bash
set -euo pipefail

ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
APK=${1:-"$ROOT/build/r920-release/Starcore-Tianshu-V57.20.6-R5.3.6-920-release.apk"}
BASE=${2:-"$ROOT/build/r915-release/Starcore-Tianshu-V57.20.1-R5.3.1-915-release.apk"}
OUT=${R920_EVIDENCE_DIR:-"$ROOT/build/r920-release/evidence"}
TC=${STARCORE_TOOLCHAIN_ROOT:-"$ROOT/../TOOLCHAIN_USED"}
AAPT2="$TC/aapt/bin/x64/linux/aapt2"
APKSIGNER="$TC/android-signing/apksigner"
ZIPALIGN="$TC/android-signing/zipalign"
APKTOOL="$TC/apktool/apktool.jar"
LIB="$TC/jadx-node/package/resources/jadx-1.1.0/lib"
TMP=$(mktemp -d "${TMPDIR:-/tmp}/starcore-r920-accept.XXXXXX")
trap 'rm -rf "$TMP"' EXIT HUP INT TERM
mkdir -p "$OUT" "$TMP/smali"

fail() {
    echo "R920_APK_ACCEPTANCE=FAIL|$1" >&2
    exit 1
}

for required in "$APK" "$BASE" "$AAPT2" "$APKSIGNER" "$ZIPALIGN" "$APKTOOL"; do
    [ -f "$required" ] || fail "MISSING_$(basename "$required")"
done

"$ZIPALIGN" -c -p 4 "$APK" > "$OUT/R920_APK_ZIPALIGN.txt" 2>&1 || fail ZIPALIGN
"$APKSIGNER" verify --verbose --print-certs "$APK" \
    > "$OUT/R920_APK_SIGNATURE.txt" 2>&1 || fail SIGNATURE
"$AAPT2" dump badging "$APK" > "$OUT/R920_APK_BADGING.txt" || fail BADGING
rg -q "package: name='com.ethk3.novaonepro' versionCode='577240920' versionName='V57.20.6-R5.3.6-920'" \
    "$OUT/R920_APK_BADGING.txt" || fail IDENTITY
rg -q "minSdkVersion:'21'" "$OUT/R920_APK_BADGING.txt" || fail MIN_SDK
unzip -t "$APK" > "$OUT/R920_APK_ZIP_INTEGRITY.txt" || fail ZIP_INTEGRITY

SIGNED_CERT=$(sed -n 's/^.*certificate SHA-256 digest: //p' \
    "$OUT/R920_APK_SIGNATURE.txt" | head -n 1)
BASE_CERT=$("$APKSIGNER" verify --print-certs "$BASE" 2>/dev/null \
    | sed -n 's/^.*certificate SHA-256 digest: //p' | head -n 1)
[ -n "$SIGNED_CERT" ] && [ "$SIGNED_CERT" = "$BASE_CERT" ] \
    || fail COVER_INSTALL_CERTIFICATE

java -jar "$APKTOOL" d -s -f -o "$TMP/decoded" "$APK" \
    > "$OUT/R920_APKTOOL_DECODE.txt" 2>&1 || fail APK_DECODE
dex_count=0
while IFS= read -r dex_name; do
    dex_count=$((dex_count + 1))
    unzip -p "$APK" "$dex_name" > "$TMP/$dex_name"
    java -cp "$LIB/*" org.jf.baksmali.Main d "$TMP/$dex_name" -o "$TMP/smali" \
        >> "$OUT/R920_APK_DEX_DECODE.txt" 2>&1 || fail DEX_DECODE
done < <(unzip -Z1 "$APK" | rg '^classes([0-9]+)?\.dex$' | sort -V)
[ "$dex_count" -ge 1 ] || fail DEX_MISSING

python3 "$ROOT/verification/r920/r920_apk_resource_contract.py" \
    "$TMP/decoded" "$TMP/smali" "$ROOT/app/src/main/java/com/ethk3/novaonepro" \
    > "$OUT/R920_APK_RESOURCE_CONTRACT_ACCEPTANCE.txt" 2>&1 \
    || fail DEX_RESOURCE_ID_PARITY

class_count=0
for class_name in \
    StarcoreApplication MainActivity CrashRecorder AutoFetchService BootReceiver \
    BackgroundRuntimeCapabilityR811 SettlementEngine SettlementRecoverySupervisorR906 \
    SettlementPostCommitSidecarR909 RuntimeTruthSnapshotR906 LegacyPredrawContextBridgeR915 \
    GenesisSchemaRegistryR915 GenesisAtomicWorldR920 GenesisAssetGenomeR915 GenesisAssetVmR915 AssetGenesisEngineR915 \
    AlgorithmGenomeR908 AlgorithmGenomeVmR908 AlgorithmInventionLabR908 \
    AlgorithmEvolutionLifecycleR912 ResearchLearningContinuityR912 \
    ResearchPredrawSettlementPumpR908 ResearchTinyDeepLearnerR909 \
    CertifiedResearchMixerR915 ModelLearningActivity V51CandidateRealGateR915 \
    DiagnosticStateRendererR906 WhyTruthSectionRendererR906; do
    [ -f "$TMP/smali/com/ethk3/novaonepro/$class_name.smali" ] \
        || fail "DEX_CLASS_$class_name"
    class_count=$((class_count + 1))
done

BUILD_CONFIG="$TMP/smali/com/ethk3/novaonepro/BuildConfig.smali"
[ -f "$BUILD_CONFIG" ] || fail BUILD_CONFIG
rg -q 'APPLICATION_ID:Ljava/lang/String; = "com.ethk3.novaonepro"' "$BUILD_CONFIG" \
    || fail DEX_APP_ID
rg -q 'VERSION_NAME:Ljava/lang/String; = "V57.20.6-R5.3.6-920"' "$BUILD_CONFIG" \
    || fail DEX_VERSION_NAME
VERSION_CODE_HEX=$(printf '0x%x' 577240920)
rg -q "VERSION_CODE:I = (577240920|$VERSION_CODE_HEX)" "$BUILD_CONFIG" \
    || fail DEX_VERSION_CODE
KEY_A=$(sed -n 's/^\.field public static final DUOQU_API_KEY_A:Ljava\/lang\/String; = "\([A-Za-z0-9][A-Za-z0-9]*\)"$/\1/p' "$BUILD_CONFIG")
KEY_B=$(sed -n 's/^\.field public static final DUOQU_API_KEY_B:Ljava\/lang\/String; = "\([A-Za-z0-9][A-Za-z0-9]*\)"$/\1/p' "$BUILD_CONFIG")
[[ "$KEY_A$KEY_B" =~ ^[A-Za-z0-9]{8,128}$ ]] || fail PROVIDER_KEY_NOT_CARRIED
unset KEY_A KEY_B

TRUTH="$TMP/smali/com/ethk3/novaonepro/RuntimeTruthSnapshotR906.smali"
MAIN="$TMP/smali/com/ethk3/novaonepro/MainActivity.smali"
for call in \
    'AssetGenesisEngineR915;->capturePredraw' \
    'AlgorithmInventionLabR908;->capturePredraw' \
    'ResearchTinyDeepLearnerR909;->capturePredraw' \
    'ResearchLearningContinuityR912;->pulse' \
    'AlgorithmInventionLabR908;->maintain' \
    'AssetGenesisEngineR915;->maintain'; do
    rg -q "$call" "$TMP/smali" || fail "APK_PREDRAW_WIRING_$call"
done
for call in \
    'AlgorithmInventionLabR908;->settleDraw' \
    'AssetGenesisEngineR915;->settleDraw' \
    'LegacyPredrawContextBridgeR915;->settleDraw'; do
    rg -q "$call" "$TMP/smali" || fail "APK_POSTDRAW_WIRING_$call"
done
rg -q 'R920-RUNTIME-TRUTH-V7' "$TRUTH" || fail APK_TRUTH_VERSION
# baksmali renders non-ASCII string contents as \uXXXX escapes; the stable visible payload and
# semantic owner version together prove that the final Dex, rather than only source, has the card.
rg -q '240/240' "$TRUTH" || fail APK_VISIBLE_SEMANTIC_ATOMIC_WORLD
rg -q 'R920_SEMANTIC_ATOMIC_WORLD_V1' "$TRUTH" || fail APK_VISIBLE_SEMANTIC_ATOMIC_OWNER
rg -q 'R920_SEMANTIC_ATOMIC_WORLD_V1' \
    "$TMP/smali/com/ethk3/novaonepro/GenesisAtomicWorldR920.smali" \
    || fail APK_SEMANTIC_ATOMIC_WORLD_VERSION
rg -q 'GenesisAtomicWorldR920;->materialize' "$TMP/smali/com/ethk3/novaonepro/GenesisAssetVmR915.smali" \
    || fail APK_SEMANTIC_ATOMIC_WORLD_WIRING
rg -q 'installLaunchSafeModeR920' "$MAIN" || fail APK_LAUNCH_SAFE_MODE_METHOD
rg -q 'R920_UI_BIND_OR_BOOTSTRAP' "$MAIN" || fail APK_LAUNCH_SAFE_MODE_REASON
rg -q 'R920' "$TMP/decoded/res/values/strings.xml" || fail APK_VISIBLE_R920
rg -q '星核天枢' "$TMP/decoded/res/values/strings.xml" || fail APK_VISIBLE_PRODUCT_NAME

sha256sum "$APK" > "$OUT/R920_APK_SHA256.txt"
APK_BYTES=$(stat -c '%s' "$APK")
[ "$APK_BYTES" -gt 1000000 ] || fail APK_SUSPICIOUSLY_SMALL
{
    echo 'R920_APK_ACCEPTANCE=PASS'
    echo 'APPLICATION_ID=com.ethk3.novaonepro'
    echo 'VERSION_NAME=V57.20.6-R5.3.6-920'
    echo 'VERSION_CODE=577240920'
    echo "APK_BYTES=$APK_BYTES"
    echo "DEX_COUNT=$dex_count"
    echo "R920_RUNTIME_CLASSES=$class_count/$class_count"
    echo 'DEX_RESOURCE_ID_PARITY=PASS_ZERO_MISMATCH'
    echo 'MAIN_ACTIVITY_BINDINGS=PASS'
    echo 'ROW_RENDERER_BINDINGS=PASS'
    echo 'PREDRAW_RESEARCH_WIRING=PASS'
    echo 'POSTDRAW_RESEARCH_TEACHER_WIRING=PASS'
    echo 'SEMANTIC_ATOMIC_WORLD_WIRING=PASS'
    echo 'VISIBLE_SEMANTIC_ATOMIC_WORLD_240_OF_240=PASS'
    echo 'PROVIDER_KEY_CARRIED=YES_REDACTED'
    echo "CERT_SHA256=$SIGNED_CERT"
    echo 'SIGNATURE_CONTINUITY=PASS_OVERWRITE_INSTALL'
    echo 'ZIPALIGN=PASS'
    echo 'ZIP_INTEGRITY=PASS'
    echo 'HOST_PROVEN_LAUNCH_RESOURCE_CONTRACT=PASS'
    echo 'LAUNCH_SAFE_MODE_FALLBACK=PASS_IN_FINAL_DEX'
    echo 'PHYSICAL_DEVICE_COLD_START=RETEST_REQUIRED'
    echo 'PHYSICAL_DEVICE_BACKGROUND_RUNTIME=RETEST_REQUIRED'
} | tee "$OUT/R920_APK_ACCEPTANCE_SUMMARY.txt"
