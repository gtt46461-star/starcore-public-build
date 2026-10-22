#!/usr/bin/env bash
# Clean, signed R817 APK build. Private key/API material is read only inside a mktemp directory,
# never copied into build evidence, source archives, or the output directory.
set -euo pipefail

ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
BASE_ROOT=${STARCORE_BASE_ROOT:-"$ROOT/../full813/STARCORE_TIANSHU_V57.15.3-R3.6.10-813_FULL_ALL_IN_ONE"}
BASE_APK=${1:-"$BASE_ROOT/APK/STARCORE_TIANSHU_V57.15.3-R3.6.10-813.apk"}
OUTPUT_APK=${2:-"$ROOT/build/r820-release/Starcore-Tianshu-V57.16.1-R4.0.1-901-release.apk"}
SIGNING_KIT=${3:-"$BASE_ROOT/PRIVATE_SIGNING/STARCORE-V57-release-signing-kit.zip"}
TOOLCHAIN_ROOT=${STARCORE_TOOLCHAIN_ROOT:-"$BASE_ROOT/TOOLCHAIN_USED"}
ANDROID_JAR=${STARCORE_ANDROID_JAR:-"$TOOLCHAIN_ROOT/android-platform-36/android.jar"}
R8_JAR=${STARCORE_R8_JAR:-"$TOOLCHAIN_ROOT/r8-9.2.23/r8-9.2.23.jar"}
APKTOOL_JAR=${STARCORE_APKTOOL_JAR:-"$TOOLCHAIN_ROOT/apktool/apktool.jar"}
AAPT2=${STARCORE_AAPT2:-"$TOOLCHAIN_ROOT/aapt/bin/x64/linux/aapt2"}
SIGN_TOOLS=${STARCORE_SIGN_TOOLS:-"$TOOLCHAIN_ROOT/android-signing"}
APKSIGNER=${STARCORE_APKSIGNER:-"$SIGN_TOOLS/apksigner"}
ZIPALIGN=${STARCORE_ZIPALIGN:-"$SIGN_TOOLS/zipalign"}
JADX_LIB=${STARCORE_JADX_LIB:-"$TOOLCHAIN_ROOT/jadx-node/package/resources/jadx-1.1.0/lib"}
BAKSMALI_JAR=${STARCORE_BAKSMALI_JAR:-"$JADX_LIB/baksmali-2.3.4.jar"}
EVIDENCE_DIR=${R817_EVIDENCE_DIR:-${R816_EVIDENCE_DIR:-${R815_EVIDENCE_DIR:-"$ROOT/build/r820-release/evidence"}}}
VERSION_CODE=577200901
VERSION_NAME='V57.16.1-R4.0.1-901'
PACKAGE_NAME='com.ethk3.novaonepro'
TMP=$(mktemp -d "${TMPDIR:-/tmp}/starcore-r817-build.XXXXXX")
trap 'rm -rf "$TMP"' EXIT HUP INT TERM
cd "$ROOT"

fail() { echo "R817_RELEASE_BUILD=FAIL|$1" >&2; exit 1; }
require_file() { [ -f "$1" ] || fail "MISSING:$2"; }
safe_copy() { cp "$1" "$2" || fail "COPY_FAILED:$(basename "$1")"; }

require_file "$BASE_APK" BASE_APK
require_file "$SIGNING_KIT" SIGNING_KIT
require_file "$ANDROID_JAR" ANDROID_PLATFORM_36
require_file "$R8_JAR" R8_D8
require_file "$APKTOOL_JAR" APKTOOL
require_file "$AAPT2" AAPT2
require_file "$APKSIGNER" APKSIGNER
require_file "$ZIPALIGN" ZIPALIGN
require_file "$BAKSMALI_JAR" BAKSMALI
require_file release/release_identity.properties RELEASE_IDENTITY

if ! rg -q '^versionName=V57\.15\.3-R3\.6\.17-820$' release/release_identity.properties \
    || ! rg -q '^versionCode=577200901$' release/release_identity.properties; then
    fail RELEASE_IDENTITY_DRIFT
fi

# The gate regenerates source/resource/toolchain provenance before compiling and runs all host
# contracts. It does not consume nor print any API key.
if [ "${R820_PREVERIFIED_GATE:-0}" = "1" ]; then
    PREVERIFIED_GATE="$ROOT/build/r820-verification/evidence/R817_BEHAVIOR_GATE_SUMMARY.txt"
    require_file "$PREVERIFIED_GATE" PREVERIFIED_GATE
    rg -q '^R817_GATE=PASS$' "$PREVERIFIED_GATE" || fail PREVERIFIED_GATE_NOT_PASS
    cp "$PREVERIFIED_GATE" "$TMP/behavior-gate.log"
else
    R817_EVIDENCE_DIR="$EVIDENCE_DIR/behavior" \
    STARCORE_BASE_ROOT="$BASE_ROOT" \
    STARCORE_TOOLCHAIN_ROOT="$TOOLCHAIN_ROOT" \
    STARCORE_ANDROID_JAR="$ANDROID_JAR" \
    STARCORE_R8_JAR="$R8_JAR" \
    STARCORE_APKTOOL_JAR="$APKTOOL_JAR" \
    STARCORE_AAPT2="$AAPT2" \
    scripts/r815_behavior_gate.sh > "$TMP/behavior-gate.log" 2>&1 \
        || { sed -n '1,300p' "$TMP/behavior-gate.log" >&2; fail BEHAVIOR_GATE; }
fi

# A release build requires a real provisioned key. An explicitly supplied environment value takes
# precedence. For an in-place signed upgrade, the already-provisioned trusted baseline split is
# allowed only inside this temporary directory; the value is never logged or retained.
key_source='ENV'
if [ -n "${DUOQU_API_KEY:-}" ]; then
    api_key=$DUOQU_API_KEY
else
    key_source='TRUSTED_BASELINE_SPLIT'
    unzip -p "$BASE_APK" classes.dex > "$TMP/base.dex" || fail BASE_DEX_EXTRACT
    java -cp "$JADX_LIB/*" org.jf.baksmali.Main d "$TMP/base.dex" -o "$TMP/base-smali" \
        > "$TMP/baksmali.log" 2>&1 || fail BASE_DEX_DECODE
    build_smali="$TMP/base-smali/com/ethk3/novaonepro/BuildConfig.smali"
    require_file "$build_smali" BASE_BUILD_CONFIG
    api_a=$(sed -n 's/^\.field public static final DUOQU_API_KEY_A:Ljava\/lang\/String; = "\([A-Za-z0-9][A-Za-z0-9]*\)"$/\1/p' "$build_smali")
    api_b=$(sed -n 's/^\.field public static final DUOQU_API_KEY_B:Ljava\/lang\/String; = "\([A-Za-z0-9][A-Za-z0-9]*\)"$/\1/p' "$build_smali")
    api_key=$api_a$api_b
fi
case "$api_key" in
    *[!A-Za-z0-9]*|'') fail API_KEY_FORMAT ;;
esac
key_length=${#api_key}
[ "$key_length" -ge 8 ] && [ "$key_length" -le 128 ] || fail API_KEY_LENGTH
split=$((key_length / 2))
[ "$split" -ge 1 ] || fail API_KEY_SPLIT
api_a=$(printf '%s' "$api_key" | cut -c "1-$split")
api_b=$(printf '%s' "$api_key" | cut -c "$((split + 1))-" )
[ -n "$api_a" ] && [ -n "$api_b" ] || fail API_KEY_EMPTY_SPLIT
unset api_key

mkdir -p "$TMP/generated/com/ethk3/novaonepro" "$TMP/app-classes" "$TMP/d8-out" "$TMP/logs"
find app/src/main/java -type f -name '*.java' -print | sort > "$TMP/app-sources.list"
sed \
    -e "s/DUOQU_API_KEY_A = \"\"/DUOQU_API_KEY_A = \"$api_a\"/" \
    -e "s/DUOQU_API_KEY_B = \"\"/DUOQU_API_KEY_B = \"$api_b\"/" \
    verification/r36-stubs/com/ethk3/novaonepro/BuildConfig.java \
    > "$TMP/generated/com/ethk3/novaonepro/BuildConfig.java"
unset api_a api_b

java -m jdk.compiler/com.sun.tools.javac.Main \
    -encoding UTF-8 -source 17 -target 17 -cp "$ANDROID_JAR" -d "$TMP/app-classes" \
    @"$TMP/app-sources.list" \
    "$TMP/generated/com/ethk3/novaonepro/BuildConfig.java" \
    verification/r36-stubs/com/ethk3/novaonepro/R.java \
    > "$TMP/logs/app-javac-java17.log" 2>&1 \
    || { sed -n '1,300p' "$TMP/logs/app-javac-java17.log" >&2; fail FULL_APP_JAVA17_COMPILE; }
class_major_hex=$(od -An -t x1 -j 6 -N 2 "$TMP/app-classes/com/ethk3/novaonepro/ReleaseIdentityR817.class" | tr -d ' ')
[ "$class_major_hex" = '003d' ] || fail "JAVA17_CLASSFILE_MAJOR:$class_major_hex"
(cd "$TMP/app-classes" && zip -q -r "$TMP/app-classes-java17.jar" .)
java -cp "$R8_JAR" com.android.tools.r8.D8 --release --min-api 21 --lib "$ANDROID_JAR" \
    --output "$TMP/d8-out" "$TMP/app-classes-java17.jar" \
    > "$TMP/logs/d8.log" 2>&1 \
    || { sed -n '1,260p' "$TMP/logs/d8.log" >&2; fail D8_BUILD; }
dex_count=$(find "$TMP/d8-out" -maxdepth 1 -type f -name 'classes*.dex' | wc -l | tr -d ' ')
[ "$dex_count" -ge 1 ] || fail D8_OUTPUT_EMPTY

mkdir -p "$TMP/framework"
java -jar "$APKTOOL_JAR" d -s -p "$TMP/framework" -f -o "$TMP/decoded" "$BASE_APK" \
    > "$TMP/logs/apktool-decode.log" 2>&1 \
    || { tail -n 120 "$TMP/logs/apktool-decode.log" >&2; fail APKTOOL_DECODE; }
mkdir -p "$TMP/original-dex"
shopt -s nullglob
for old_dex in "$TMP"/decoded/classes*.dex; do mv "$old_dex" "$TMP/original-dex/"; done
shopt -u nullglob
safe_copy app/src/main/AndroidManifest.xml "$TMP/decoded/AndroidManifest.xml"
mkdir -p "$TMP/decoded/res"
cp -R app/src/main/res/. "$TMP/decoded/res/" || fail RESOURCE_COPY
cp "$TMP"/d8-out/classes*.dex "$TMP/decoded/" || fail DEX_COPY
sed \
    -e "s/^  versionCode: .*/  versionCode: $VERSION_CODE/" \
    -e "s/^  versionName: .*/  versionName: $VERSION_NAME/" \
    "$TMP/decoded/apktool.yml" > "$TMP/apktool.yml.updated"
safe_copy "$TMP/apktool.yml.updated" "$TMP/decoded/apktool.yml"
java -jar "$APKTOOL_JAR" b -p "$TMP/framework" -o "$TMP/unsigned.apk" "$TMP/decoded" \
    > "$TMP/logs/apktool-build.log" 2>&1 \
    || { tail -n 160 "$TMP/logs/apktool-build.log" >&2; fail APKTOOL_BUILD; }
"$ZIPALIGN" -f -p 4 "$TMP/unsigned.apk" "$TMP/aligned.apk" \
    > "$TMP/logs/zipalign.log" 2>&1 || fail ZIPALIGN

mkdir -p "$TMP/signing"
unzip -q "$SIGNING_KIT" -d "$TMP/signing" || fail SIGNING_KIT_EXTRACT
pkcs12=$(find "$TMP/signing" -type f -name '*.p12' -print -quit)
credentials=$(find "$TMP/signing" -type f -name 'SIGNING-CREDENTIALS.txt' -print -quit)
require_file "$pkcs12" PKCS12
require_file "$credentials" SIGNING_CREDENTIALS
sign_alias=$(sed -n 's/^Alias:[[:space:]]*//p' "$credentials" | tr -d '\r')
sign_password=$(sed -n 's/^Password:[[:space:]]*//p' "$credentials" | tr -d '\r')
sign_type=$(sed -n 's/^Keystore type:[[:space:]]*//p' "$credentials" | tr -d '\r')
[ -n "$sign_alias" ] && [ -n "$sign_password" ] && [ -n "$sign_type" ] || fail SIGNING_CREDENTIALS_INVALID
output_parent=$(dirname "$OUTPUT_APK")
mkdir -p "$output_parent"
output_abs=$(CDPATH= cd -- "$output_parent" && pwd)/$(basename "$OUTPUT_APK")
base_abs=$(CDPATH= cd -- "$(dirname "$BASE_APK")" && pwd)/$(basename "$BASE_APK")
[ "$base_abs" != "$output_abs" ] || fail OUTPUT_MUST_NOT_OVERWRITE_BASE
"$APKSIGNER" sign --ks "$pkcs12" --ks-type "$sign_type" --ks-key-alias "$sign_alias" \
    --ks-pass "pass:$sign_password" --key-pass "pass:$sign_password" --min-sdk-version 21 \
    --out "$output_abs" "$TMP/aligned.apk" > "$TMP/logs/apksigner-sign.log" 2>&1 \
    || fail APK_SIGN
unset sign_password

"$ZIPALIGN" -c -p 4 "$output_abs" > "$TMP/logs/zipalign-verify.log" 2>&1 || fail ZIPALIGN_VERIFY
"$APKSIGNER" verify --verbose --print-certs "$output_abs" > "$TMP/logs/apksigner-verify.log" 2>&1 \
    || fail APK_SIGNATURE_VERIFY
unzip -t "$output_abs" > "$TMP/logs/unzip-test.log" || fail APK_ZIP_INTEGRITY
"$AAPT2" dump badging "$output_abs" > "$TMP/logs/aapt2-badging.log" || fail AAPT2_BADGING
rg -q "package: name='$PACKAGE_NAME' versionCode='$VERSION_CODE' versionName='$VERSION_NAME'" \
    "$TMP/logs/aapt2-badging.log" || fail APK_IDENTITY_BADGING
"$AAPT2" dump xmltree "$output_abs" --file AndroidManifest.xml > "$TMP/logs/aapt2-manifest-xmltree.log" \
    || fail AAPT2_MANIFEST
rg -q 'InternalWatchdogReceiverR815' "$TMP/logs/aapt2-manifest-xmltree.log" || fail APK_INTERNAL_WATCHDOG_MISSING
rg -q 'InternalWatchdogReceiverR815\.ACTION' app/src/main/java/com/ethk3/novaonepro/BootReceiver.java \
    || fail WATCHDOG_ACTION_NOT_SCHEDULED

base_cert=$("$APKSIGNER" verify --print-certs "$BASE_APK" 2>/dev/null | sed -n 's/^.*certificate SHA-256 digest: //p' | head -n 1)
cert_sha=$(sed -n 's/^.*certificate SHA-256 digest: //p' "$TMP/logs/apksigner-verify.log" | head -n 1)
[ -n "$base_cert" ] && [ "$base_cert" = "$cert_sha" ] || fail SIGNING_CERT_CHANGED
apk_sha=$(sha256sum "$output_abs" | awk '{print $1}')
dex_sha=$(
    unzip -Z1 "$output_abs" | rg '^classes([0-9]+)?\.dex$' | sort | while IFS= read -r dex_entry; do
        printf '%s\0' "$dex_entry"
        unzip -p "$output_abs" "$dex_entry"
        printf '\0'
    done | sha256sum | awk '{print $1}')
[ "${#dex_sha}" = '64' ] || fail DEX_HASH
source_sha=$(sed -n 's/.*SOURCE_TREE_SHA256 = "\([0-9a-f][0-9a-f]*\)".*/\1/p' app/src/main/java/com/ethk3/novaonepro/SourceManifestR815.java)
resource_sha=$(sed -n 's/.*RESOURCE_MANIFEST_SHA256 = "\([0-9a-f][0-9a-f]*\)".*/\1/p' app/src/main/java/com/ethk3/novaonepro/SourceManifestR815.java)
toolchain_sha=$(sed -n 's/.*TOOLCHAIN_SHA256 = "\([0-9a-f][0-9a-f]*\)".*/\1/p' app/src/main/java/com/ethk3/novaonepro/SourceManifestR815.java)
generated_at=$(sed -n 's/.*GENERATED_AT_UTC = "\([^"]*\)".*/\1/p' app/src/main/java/com/ethk3/novaonepro/SourceManifestR815.java)
[ "${#source_sha}" = '64' ] && [ "${#resource_sha}" = '64' ] && [ "${#toolchain_sha}" = '64' ] || fail SOURCE_PROVENANCE_HASH

mkdir -p "$EVIDENCE_DIR"
cp "$TMP/behavior-gate.log" "$EVIDENCE_DIR/R817_BEHAVIOR_GATE_AT_BUILD.log"
cp "$TMP/logs/app-javac-java17.log" "$EVIDENCE_DIR/R817_RELEASE_JAVAC.log"
cp "$TMP/logs/d8.log" "$EVIDENCE_DIR/R817_RELEASE_D8.log"
cp "$TMP/logs/aapt2-badging.log" "$EVIDENCE_DIR/R817_AAPT2_BADGING.log"
cp "$TMP/logs/aapt2-manifest-xmltree.log" "$EVIDENCE_DIR/R817_AAPT2_MANIFEST_XMLTREE.log"
cp "$TMP/logs/apksigner-verify.log" "$EVIDENCE_DIR/R817_APKSIGNER_VERIFY.log"
cp "$TMP/logs/zipalign-verify.log" "$EVIDENCE_DIR/R817_ZIPALIGN_VERIFY.log"
cp "$TMP/logs/unzip-test.log" "$EVIDENCE_DIR/R817_APK_ZIP_INTEGRITY.log"
{
    printf '{\n'
    printf '  "release": "V57.16.1-R4.0.1-901",\n'
    printf '  "versionCode": 577200901,\n'
    printf '  "applicationId": "com.ethk3.novaonepro",\n'
    printf '  "gameCode": "Binance1-PK10",\n'
    printf '  "releaseMode": "SHADOW_ONLY",\n'
    printf '  "modelProven": false,\n'
    printf '  "realBetHardDisabled": true,\n'
    printf '  "sourceTreeSha256": "%s",\n' "$source_sha"
    printf '  "resourceManifestSha256": "%s",\n' "$resource_sha"
    printf '  "toolchainSha256": "%s",\n' "$toolchain_sha"
    printf '  "generatedAtUtc": "%s",\n' "$generated_at"
    printf '  "classesDexEntriesSha256": "%s",\n' "$dex_sha"
    printf '  "apkSha256": "%s",\n' "$apk_sha"
    printf '  "signingCertificateSha256": "%s",\n' "$cert_sha"
    printf '  "apiKeyProvisioning": "%s",\n' "$key_source"
    printf '  "researchRuntime": "EXECUTABLE_FORMULA_PREDRAW_SHADOW_AND_FUTURE_LIVE_LABELS",\n'
    printf '  "dynamicLearning": "POST_SETTLEMENT_LIGHT_FTRL_CONSUMED_BY_NEXT_PREDRAW_ONLY_NO_FUTURE_BACKFILL",\n'
    printf '  "physicalDeviceValidation": "NOT_CLAIMED_REQUIRES_SEPARATE_EXECUTION"\n'
    printf '}\n'
} > "$EVIDENCE_DIR/RELEASE_MANIFEST.json"
sha256sum "$output_abs" > "$EVIDENCE_DIR/R817_APK_SHA256.txt"
sha256sum "$EVIDENCE_DIR/RELEASE_MANIFEST.json" > "$EVIDENCE_DIR/R817_RELEASE_MANIFEST_SHA256.txt"
source_files=$(wc -l < "$TMP/app-sources.list" | tr -d ' ')
source_lines=$(xargs wc -l < "$TMP/app-sources.list" | tail -n 1 | awk '{print $1}')
apk_bytes=$(wc -c < "$output_abs" | tr -d ' ')
{
    echo 'R817_RELEASE_BUILD=PASS'
    echo "PACKAGE=$PACKAGE_NAME|VERSION_CODE=$VERSION_CODE|VERSION_NAME=$VERSION_NAME"
    echo "SOURCE_JAVA_FILES=$source_files|SOURCE_LINES=$source_lines|DEX_FILES=$dex_count|APK_BYTES=$apk_bytes"
    echo "APK_SHA256=$apk_sha"
    echo "DEX_ENTRIES_SHA256=$dex_sha"
    echo "SOURCE_TREE_SHA256=$source_sha|RESOURCE_MANIFEST_SHA256=$resource_sha|TOOLCHAIN_SHA256=$toolchain_sha"
    echo 'SIGNATURE_CERT_CONTINUITY=PASS|ZIPALIGN=PASS|APK_ZIP_INTEGRITY=PASS'
    echo "API_KEY_PROVISIONING=$key_source|VALUE_REDACTED=TRUE"
    echo 'RESEARCH_RUNTIME=EXECUTABLE_PREDRAW_SHADOW|DYNAMIC_LEARNING=POST_SETTLEMENT_FTRL_CONSUMED_BY_NEXT_PREDRAW|REAL_AUTHORITY=0'
    echo 'PHYSICAL_DEVICE_VALIDATION=NOT_CLAIMED_REQUIRES_SEPARATE_EXECUTION'
} | tee "$EVIDENCE_DIR/R817_RELEASE_BUILD_SUMMARY.txt"
