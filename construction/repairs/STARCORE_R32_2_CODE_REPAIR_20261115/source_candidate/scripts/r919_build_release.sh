#!/usr/bin/env bash
set -euo pipefail

ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
BASE=${1:-"$ROOT/build/r915-release/Starcore-Tianshu-V57.20.1-R5.3.1-915-release.apk"}
OUT_DIR=${2:-"$ROOT/build/r919-release"}
TC=${STARCORE_TOOLCHAIN_ROOT:-"$ROOT/../TOOLCHAIN_USED"}
ANDROID="$TC/android-platform-36/android.jar"
R8="$TC/r8-9.2.23/r8-9.2.23.jar"
APKTOOL="$TC/apktool/apktool.jar"
LIB="$TC/jadx-node/package/resources/jadx-1.1.0/lib"
ZIPALIGN="$TC/android-signing/zipalign"
APKSIGNER="$TC/android-signing/apksigner"
AAPT2="$TC/aapt/bin/x64/linux/aapt2"
TMP=$(mktemp -d "${TMPDIR:-/tmp}/starcore-r919-build.XXXXXX")
trap 'rm -rf "$TMP"' EXIT HUP INT TERM

fail() {
    mkdir -p "$ROOT/build/r919-release-failure"
    cp -f "$TMP/logs/"* "$ROOT/build/r919-release-failure/" 2>/dev/null || true
    echo "R919_RELEASE_BUILD=FAIL|$1" >&2
    exit 1
}

identity_value() {
    sed -n "s/^$1=//p" "$ROOT/release/release_identity.properties" | head -n 1
}

APP_ID=$(identity_value applicationId)
VERSION_NAME=$(identity_value versionName)
VERSION_CODE=$(identity_value versionCode)
VERSION_CODE_HEX=$(printf '0x%x' "$VERSION_CODE")
[ "$APP_ID" = "com.ethk3.novaonepro" ] || fail RELEASE_PROPERTY_APP_ID
[ "$VERSION_NAME" = "V57.20.5-R5.3.5-919" ] || fail RELEASE_PROPERTY_VERSION_NAME
[ "$VERSION_CODE" = "577240919" ] || fail RELEASE_PROPERTY_VERSION_CODE
for required in "$BASE" "$ANDROID" "$R8" "$APKTOOL" "$ZIPALIGN" "$APKSIGNER" "$AAPT2"; do
    [ -f "$required" ] || fail "MISSING:$(basename "$required")"
done

mkdir -p "$OUT_DIR/evidence" "$TMP/classes" "$TMP/gen/com/ethk3/novaonepro" \
    "$TMP/d8" "$TMP/framework" "$TMP/logs"
cd "$ROOT"

# Cover-install provider credentials are inherited from the previous signed APK into this private
# temporary tree. Values are never printed or persisted in source/evidence.
unzip -p "$BASE" classes.dex > "$TMP/base.dex" || fail BASE_DEX
java -cp "$LIB/*" org.jf.baksmali.Main d "$TMP/base.dex" -o "$TMP/smali" \
    > "$TMP/logs/baksmali.log" 2>&1 || fail BASE_DECODE
BASE_BUILD_CONFIG="$TMP/smali/com/ethk3/novaonepro/BuildConfig.smali"
[ -f "$BASE_BUILD_CONFIG" ] || fail BASE_BUILD_CONFIG
KEY_A=$(sed -n 's/^\.field public static final DUOQU_API_KEY_A:Ljava\/lang\/String; = "\([A-Za-z0-9][A-Za-z0-9]*\)"$/\1/p' "$BASE_BUILD_CONFIG")
KEY_B=$(sed -n 's/^\.field public static final DUOQU_API_KEY_B:Ljava\/lang\/String; = "\([A-Za-z0-9][A-Za-z0-9]*\)"$/\1/p' "$BASE_BUILD_CONFIG")
PROVIDER_KEY="$KEY_A$KEY_B"
[[ "$PROVIDER_KEY" =~ ^[A-Za-z0-9]{8,128}$ ]] || fail API_KEY_FORMAT
sed -e "s/DUOQU_API_KEY_A = \"\"/DUOQU_API_KEY_A = \"$KEY_A\"/" \
    -e "s/DUOQU_API_KEY_B = \"\"/DUOQU_API_KEY_B = \"$KEY_B\"/" \
    -e "s/VERSION_CODE = [0-9][0-9]*/VERSION_CODE = $VERSION_CODE/" \
    -e "s/VERSION_NAME = \"[^\"]*\"/VERSION_NAME = \"$VERSION_NAME\"/" \
    verification/r36-stubs/com/ethk3/novaonepro/BuildConfig.java \
    > "$TMP/gen/com/ethk3/novaonepro/BuildConfig.java"
unset KEY_A KEY_B PROVIDER_KEY

find app/src/main/java -name '*.java' -print | sort > "$TMP/sources.list"
java -m jdk.compiler/com.sun.tools.javac.Main -encoding UTF-8 -source 17 -target 17 \
    -cp "$ANDROID" -d "$TMP/classes" @"$TMP/sources.list" \
    "$TMP/gen/com/ethk3/novaonepro/BuildConfig.java" \
    verification/r36-stubs/com/ethk3/novaonepro/R.java \
    > "$TMP/logs/javac.log" 2>&1 || fail JAVAC
(cd "$TMP/classes" && zip -q -r "$TMP/classes.jar" .)
java -Xmx2g -cp "$R8" com.android.tools.r8.D8 --release --min-api 21 --lib "$ANDROID" \
    --output "$TMP/d8" "$TMP/classes.jar" > "$TMP/logs/d8.log" 2>&1 || fail D8

mkdir -p "$TMP/product-smali"
dex_index=0
for dex in "$TMP/d8"/classes*.dex; do
    dex_index=$((dex_index + 1))
    java -cp "$LIB/*" org.jf.baksmali.Main d "$dex" -o "$TMP/product-smali/$dex_index" \
        >> "$TMP/logs/product-dex-identity.log" 2>&1 || fail PRODUCT_DEX_DECODE
done
PRODUCT_BUILD_CONFIG=$(find "$TMP/product-smali" -path '*/com/ethk3/novaonepro/BuildConfig.smali' -print -quit)
[ -n "$PRODUCT_BUILD_CONFIG" ] || fail PRODUCT_DEX_BUILD_CONFIG_MISSING
rg -q "APPLICATION_ID:Ljava/lang/String; = \"$APP_ID\"" "$PRODUCT_BUILD_CONFIG" || fail PRODUCT_DEX_APP_ID
rg -q "VERSION_NAME:Ljava/lang/String; = \"$VERSION_NAME\"" "$PRODUCT_BUILD_CONFIG" || fail PRODUCT_DEX_VERSION_NAME
rg -q "VERSION_CODE:I = ($VERSION_CODE|$VERSION_CODE_HEX)" "$PRODUCT_BUILD_CONFIG" || fail PRODUCT_DEX_VERSION_CODE

class_count=0
for class_name in \
    AutoFetchService \
    BackgroundRuntimeCapabilityR811 \
    SettlementEngine \
    SettlementRecoverySupervisorR906 \
    SettlementPostCommitSidecarR909 \
    RuntimeTruthSnapshotR906 \
    LegacyPredrawContextBridgeR915 \
    GenesisSchemaRegistryR915 \
    GenesisAssetGenomeR915 \
    GenesisAssetVmR915 \
    AssetGenesisEngineR915 \
    AlgorithmGenomeR908 \
    AlgorithmGenomeVmR908 \
    AlgorithmInventionLabR908 \
    AlgorithmEvolutionLifecycleR912 \
    ResearchLearningContinuityR912 \
    ModelLearningActivity \
    MainActivity \
    V51CandidateRealGateR915; do
    class_file=$(find "$TMP/product-smali" -path "*/com/ethk3/novaonepro/$class_name.smali" -print -quit)
    [ -n "$class_file" ] || fail "PRODUCT_DEX_CLASS_MISSING_$class_name"
    class_count=$((class_count + 1))
done
PRODUCT_RELEASE_IDENTITY=$(find "$TMP/product-smali" -path '*/com/ethk3/novaonepro/ReleaseIdentityR817.smali' -print -quit)
[ -n "$PRODUCT_RELEASE_IDENTITY" ] || fail PRODUCT_RELEASE_IDENTITY_MISSING
rg -q 'V57\.20\.5-R5\.3\.5-919' "$PRODUCT_RELEASE_IDENTITY" || fail PRODUCT_RELEASE_IDENTITY_NAME
printf 'DEX_INTERNAL_IDENTITY=PASS|%s|%s|%s|R919_CLASSES=%s\n' \
    "$APP_ID" "$VERSION_NAME" "$VERSION_CODE" "$class_count" \
    > "$TMP/logs/product-dex-identity-summary.log"

java -jar "$APKTOOL" d -s -p "$TMP/framework" -f -o "$TMP/decoded" "$BASE" \
    > "$TMP/logs/apktool-decode.log" 2>&1 || fail APKTOOL_DECODE
rm -f "$TMP/decoded"/classes*.dex
cp "$TMP/d8"/classes*.dex "$TMP/decoded/"
cp app/src/main/AndroidManifest.xml "$TMP/decoded/AndroidManifest.xml"
cp -R app/src/main/res/. "$TMP/decoded/res/"
sed -i -e "s/^  versionCode: .*/  versionCode: $VERSION_CODE/" \
    -e "s/^  versionName: .*/  versionName: $VERSION_NAME/" "$TMP/decoded/apktool.yml"
java -jar "$APKTOOL" b -p "$TMP/framework" -o "$TMP/unsigned.apk" "$TMP/decoded" \
    > "$TMP/logs/apktool-build.log" 2>&1 || fail APKTOOL_BUILD

UNSIGNED="$OUT_DIR/Starcore-Tianshu-V57.20.5-R5.3.5-919-unsigned-aligned.apk"
"$ZIPALIGN" -f -p 4 "$TMP/unsigned.apk" "$UNSIGNED" \
    > "$TMP/logs/zipalign.log" 2>&1 || fail ZIPALIGN
"$ZIPALIGN" -c -p 4 "$UNSIGNED" > "$TMP/logs/zipalign-verify.log" 2>&1 || fail ZIPALIGN_VERIFY
"$AAPT2" dump badging "$UNSIGNED" > "$TMP/logs/aapt2-badging.log" || fail BADGING
rg -q "package: name='$APP_ID' versionCode='$VERSION_CODE' versionName='$VERSION_NAME'" \
    "$TMP/logs/aapt2-badging.log" || fail MANIFEST_IDENTITY
unzip -t "$UNSIGNED" > "$TMP/logs/unzip-test.log" || fail ZIP_VERIFY
sha256sum "$UNSIGNED" > "$OUT_DIR/evidence/R919_UNSIGNED_APK_SHA256.txt"

BASE_CERT=$("$APKSIGNER" verify --print-certs "$BASE" 2>/dev/null \
    | sed -n 's/^.*certificate SHA-256 digest: //p' | head -n 1)
[ -n "$BASE_CERT" ] || fail BASE_CERTIFICATE_UNREADABLE
printf 'BASE_CERT_SHA256=%s\n' "$BASE_CERT" > "$OUT_DIR/evidence/R919_BASE_CERTIFICATE.txt"

if [ -z "${STARCORE_SIGNING_P12:-}" ] || [ -z "${STARCORE_SIGNING_PASSWORD:-}" ]; then
    cp -f "$TMP/logs/"* "$OUT_DIR/evidence/"
    {
        echo 'R919_RELEASE_BUILD=VERIFY_ONLY'
        echo "UNSIGNED_ALIGNED=$UNSIGNED"
        echo 'DEX_INTERNAL_IDENTITY=PASS'
        echo "R919_PRODUCT_CLASSES=$class_count"
        echo 'MANIFEST_IDENTITY=PASS'
        echo 'ZIPALIGN=PASS'
        echo 'ZIP_INTEGRITY=PASS'
        echo "BASE_CERT_SHA256=$BASE_CERT"
        echo 'SIGNING=BLOCKED_ORIGINAL_PRIVATE_KEY_NOT_PROVIDED'
        echo 'OVERWRITE_INSTALL=NOT_CLAIMED'
        echo 'PHYSICAL_DEVICE_RUNTIME=RETEST_REQUIRED'
    } | tee "$OUT_DIR/evidence/R919_RELEASE_BUILD_SUMMARY.txt"
    exit 0
fi

P12=$STARCORE_SIGNING_P12
PASS=$STARCORE_SIGNING_PASSWORD
ALIAS=${STARCORE_SIGNING_ALIAS:-starcore-r902}
TYPE=${STARCORE_SIGNING_TYPE:-PKCS12}
KEY_PASS=${STARCORE_SIGNING_KEY_PASSWORD:-$PASS}
[ -f "$P12" ] || fail SIGNING_KEY_MISSING
SIGNED="$OUT_DIR/Starcore-Tianshu-V57.20.5-R5.3.5-919-release.apk"
"$APKSIGNER" sign --ks "$P12" --ks-type "$TYPE" --ks-key-alias "$ALIAS" \
    --ks-pass "pass:$PASS" --key-pass "pass:$KEY_PASS" --min-sdk-version 21 \
    --out "$SIGNED" "$UNSIGNED" > "$TMP/logs/apksigner-sign.log" 2>&1 || fail SIGN
unset PASS KEY_PASS
"$APKSIGNER" verify --verbose --print-certs "$SIGNED" \
    > "$TMP/logs/apksigner-verify.log" 2>&1 || fail SIGN_VERIFY
SIGNED_CERT=$(sed -n 's/^.*certificate SHA-256 digest: //p' \
    "$TMP/logs/apksigner-verify.log" | head -n 1)
[ "$SIGNED_CERT" = "$BASE_CERT" ] || fail SIGNING_CERT_CHANGED
cp -f "$TMP/logs/"* "$OUT_DIR/evidence/"
sha256sum "$SIGNED" > "$OUT_DIR/evidence/R919_SIGNED_APK_SHA256.txt"
{
    echo 'R919_RELEASE_BUILD=PASS'
    echo "APK=$SIGNED"
    echo "CERT_SHA256=$SIGNED_CERT"
    echo 'DEX_INTERNAL_IDENTITY=PASS'
    echo "R919_PRODUCT_CLASSES=$class_count"
    echo 'MANIFEST_IDENTITY=PASS'
    echo 'SIGNATURE_CONTINUITY=PASS_OVERWRITE_INSTALL'
    echo 'PHYSICAL_DEVICE_RUNTIME=RETEST_REQUIRED'
} | tee "$OUT_DIR/evidence/R919_RELEASE_BUILD_SUMMARY.txt"
