#!/bin/sh
set -eu

ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
BASE_APK=${1:-"$ROOT/../deliverables/Starcore-Tianshu-V57.15.3-R3.6.5-Full-Blood-Repair/APK/Starcore-Tianshu-V57.15.3-R3.6.5-release.apk"}
OUTPUT_APK=${2:-"$ROOT/../build/r367-release/Starcore-Tianshu-V57.15.3-R3.6.7-release.apk"}
SIGNING_KIT=${3:-"$ROOT/../deliverables/Starcore-Tianshu-V57.15.3-R3.6.5-Full-Blood-Repair/PRIVATE_SIGNING/STARCORE-V57-release-signing-kit.zip"}
REFERENCE_APK=${4:-}

TOOLCHAIN_ROOT=${STARCORE_TOOLCHAIN_ROOT:-"$ROOT/../toolchain"}
APKTOOL_JAR="$TOOLCHAIN_ROOT/apktool/apktool.jar"
JADX_LIB="$TOOLCHAIN_ROOT/jadx-node/package/resources/jadx-1.1.0/lib"
ANDROID_JAR=${STARCORE_ANDROID_JAR:-"$TOOLCHAIN_ROOT/android-platform-36/android.jar"}
R8_JAR=${STARCORE_R8_JAR:-"$TOOLCHAIN_ROOT/r8-9.2.23/r8-9.2.23.jar"}
BAKSMALI_JAR="$JADX_LIB/baksmali-2.3.4.jar"
AAPT2="$TOOLCHAIN_ROOT/aapt/bin/x64/linux/aapt2"
SIGN_TOOLS=${STARCORE_SIGN_TOOLS:-"$TOOLCHAIN_ROOT/android-signing"}
APKSIGNER="$SIGN_TOOLS/apksigner"
ZIPALIGN="$SIGN_TOOLS/zipalign"
EVIDENCE_DIR=${R367_EVIDENCE_DIR:-"$ROOT/../build/r367-release/evidence"}
TMP=$(mktemp -d "${TMPDIR:-/tmp}/starcore-r367-build.XXXXXX")
trap 'rm -rf "$TMP"' EXIT HUP INT TERM

VERSION_CODE=577180819
VERSION_NAME='V57.15.3-tianshu-b01-b15-live-sync-settlement-ui-hf7-20260811'

fail() { echo "R367_RELEASE_BUILD=FAIL|$1" >&2; exit 1; }
require_file() { [ -f "$1" ] || fail "MISSING:$2"; }

require_file "$BASE_APK" BASE_APK
require_file "$SIGNING_KIT" SIGNING_KIT
require_file "$APKTOOL_JAR" APKTOOL
require_file "$ANDROID_JAR" ANDROID_PLATFORM_36_API
require_file "$R8_JAR" R8_D8_9_2_23
require_file "$BAKSMALI_JAR" BAKSMALI
require_file "$AAPT2" AAPT2
require_file "$APKSIGNER" APKSIGNER
require_file "$ZIPALIGN" ZIPALIGN

"$ROOT/scripts/r36_behavior_gate.sh" > "$TMP/java17-behavior-gate.log" 2>&1 \
    || { sed -n '1,240p' "$TMP/java17-behavior-gate.log" >&2; fail JAVA17_BEHAVIOR_GATE; }

base_abs=$(CDPATH= cd -- "$(dirname -- "$BASE_APK")" && pwd)/$(basename -- "$BASE_APK")
output_parent=$(dirname -- "$OUTPUT_APK")
mkdir -p "$output_parent"
output_abs=$(CDPATH= cd -- "$output_parent" && pwd)/$(basename -- "$OUTPUT_APK")
[ "$base_abs" != "$output_abs" ] || fail OUTPUT_MUST_NOT_OVERWRITE_BASE_APK

# Recover the already provisioned production key only inside the private temp
# directory.  Alternatively a clean build may provide DUOQU_API_KEY in env.
unzip -p "$BASE_APK" classes.dex > "$TMP/base.dex"
java -cp "$JADX_LIB/*" org.jf.baksmali.Main d \
    "$TMP/base.dex" -o "$TMP/base-smali" >/dev/null
BUILD_CONFIG_SMALI="$TMP/base-smali/com/ethk3/novaonepro/BuildConfig.smali"
require_file "$BUILD_CONFIG_SMALI" BASE_BUILD_CONFIG_DEX

if [ -n "${DUOQU_API_KEY:-}" ]; then
    api_key=${DUOQU_API_KEY}
    key_length=${#api_key}
    split=$((key_length / 2))
    [ "$split" -gt 0 ] || fail API_KEY_TOO_SHORT
    api_a=$(printf '%s' "$api_key" | cut -c "1-$split")
    api_b=$(printf '%s' "$api_key" | cut -c "$((split + 1))-" )
else
    api_a=$(sed -n 's/^\.field public static final DUOQU_API_KEY_A:Ljava\/lang\/String; = "\([A-Za-z0-9][A-Za-z0-9]*\)"$/\1/p' "$BUILD_CONFIG_SMALI")
    api_b=$(sed -n 's/^\.field public static final DUOQU_API_KEY_B:Ljava\/lang\/String; = "\([A-Za-z0-9][A-Za-z0-9]*\)"$/\1/p' "$BUILD_CONFIG_SMALI")
    api_key=$api_a$api_b
    key_length=${#api_key}
fi
case "$api_key" in *[!A-Za-z0-9]*) fail API_KEY_FORMAT ;; esac
[ "$key_length" -ge 8 ] || fail API_KEY_NOT_PROVISIONED

mkdir -p "$TMP/app-classes" "$TMP/d8-out" \
    "$TMP/generated/com/ethk3/novaonepro" "$TMP/logs"
find "$ROOT/app/src/main/java" -type f -name '*.java' -print \
    | sort > "$TMP/app-sources.list"

sed \
    -e "s/DUOQU_API_KEY_A = \"\"/DUOQU_API_KEY_A = \"$api_a\"/" \
    -e "s/DUOQU_API_KEY_B = \"\"/DUOQU_API_KEY_B = \"$api_b\"/" \
    "$ROOT/verification/r36-stubs/com/ethk3/novaonepro/BuildConfig.java" \
    > "$TMP/generated/com/ethk3/novaonepro/BuildConfig.java"

java -m jdk.compiler/com.sun.tools.javac.Main \
    -encoding UTF-8 -source 17 -target 17 \
    -cp "$ANDROID_JAR" -d "$TMP/app-classes" \
    @"$TMP/app-sources.list" \
    "$TMP/generated/com/ethk3/novaonepro/BuildConfig.java" \
    "$ROOT/verification/r36-stubs/com/ethk3/novaonepro/R.java" \
    >"$TMP/logs/app-javac-java17.log" 2>&1 \
    || { sed -n '1,260p' "$TMP/logs/app-javac-java17.log" >&2; fail FULL_APP_JAVA17_COMPILE; }

class_major_hex=$(od -An -t x1 -j 6 -N 2 \
    "$TMP/app-classes/com/ethk3/novaonepro/DbHelper.class" | tr -d ' ')
[ "$class_major_hex" = "003d" ] || fail "JAVA17_CLASSFILE_MAJOR:$class_major_hex"

(cd "$TMP/app-classes" && zip -q -r "$TMP/app-classes-java17.jar" .)
java -cp "$R8_JAR" com.android.tools.r8.D8 \
    --release --min-api 21 --lib "$ANDROID_JAR" \
    --output "$TMP/d8-out" "$TMP/app-classes-java17.jar" \
    >"$TMP/logs/d8.log" 2>&1 \
    || { sed -n '1,220p' "$TMP/logs/d8.log" >&2; fail D8_BUILD; }
dex_count=$(find "$TMP/d8-out" -maxdepth 1 -type f -name 'classes*.dex' \
    | wc -l | tr -d ' ')
[ "$dex_count" -ge 1 ] || fail D8_OUTPUT_EMPTY

mkdir -p "$TMP/framework"
java -jar "$APKTOOL_JAR" d -s -p "$TMP/framework" -f \
    -o "$TMP/decoded" "$BASE_APK" >"$TMP/logs/apktool-decode.log" 2>&1 \
    || { tail -n 100 "$TMP/logs/apktool-decode.log" >&2; fail APKTOOL_DECODE; }
mkdir -p "$TMP/original-dex"
for old_dex in "$TMP"/decoded/classes*.dex; do
    [ -f "$old_dex" ] || continue
    mv "$old_dex" "$TMP/original-dex/"
done
cp "$TMP"/d8-out/classes*.dex "$TMP/decoded/"
sed \
    -e "s/^  versionCode: .*/  versionCode: $VERSION_CODE/" \
    -e "s/^  versionName: .*/  versionName: $VERSION_NAME/" \
    "$TMP/decoded/apktool.yml" > "$TMP/apktool.yml.updated"
cp "$TMP/apktool.yml.updated" "$TMP/decoded/apktool.yml"

java -jar "$APKTOOL_JAR" b -p "$TMP/framework" \
    -o "$TMP/unsigned.apk" "$TMP/decoded" >"$TMP/logs/apktool-build.log" 2>&1 \
    || { tail -n 120 "$TMP/logs/apktool-build.log" >&2; fail APKTOOL_BUILD; }
"$ZIPALIGN" -f -p 4 "$TMP/unsigned.apk" "$TMP/aligned.apk" \
    >"$TMP/logs/zipalign.log" 2>&1 || fail ZIPALIGN

mkdir -p "$TMP/signing"
unzip -q "$SIGNING_KIT" -d "$TMP/signing"
P12=$(find "$TMP/signing" -type f -name '*.p12' -print -quit)
CREDENTIALS=$(find "$TMP/signing" -type f -name 'SIGNING-CREDENTIALS.txt' -print -quit)
require_file "$P12" PKCS12
require_file "$CREDENTIALS" SIGNING_CREDENTIALS
sign_alias=$(sed -n 's/^Alias:[[:space:]]*//p' "$CREDENTIALS" | tr -d '\r')
sign_password=$(sed -n 's/^Password:[[:space:]]*//p' "$CREDENTIALS" | tr -d '\r')
sign_type=$(sed -n 's/^Keystore type:[[:space:]]*//p' "$CREDENTIALS" | tr -d '\r')
[ -n "$sign_alias" ] && [ -n "$sign_password" ] && [ -n "$sign_type" ] \
    || fail SIGNING_CREDENTIALS_INVALID

"$APKSIGNER" sign --ks "$P12" --ks-type "$sign_type" \
    --ks-key-alias "$sign_alias" --ks-pass "pass:$sign_password" \
    --key-pass "pass:$sign_password" --out "$output_abs" "$TMP/aligned.apk" \
    >"$TMP/logs/apksigner-sign.log" 2>&1 || fail APK_SIGN

"$ZIPALIGN" -c -p 4 "$output_abs" >"$TMP/logs/zipalign-verify.log" 2>&1 \
    || fail ZIPALIGN_VERIFY
"$APKSIGNER" verify --verbose --print-certs "$output_abs" \
    >"$TMP/logs/apksigner-verify.log" 2>&1 || fail APK_SIGNATURE_VERIFY
unzip -t "$output_abs" >"$TMP/logs/unzip-test.log" || fail APK_ZIP_INTEGRITY

"$AAPT2" dump badging "$output_abs" > "$TMP/logs/aapt2-badging.log"
rg -q "package: name='com.ethk3.novaonepro' versionCode='$VERSION_CODE' versionName='$VERSION_NAME'" \
    "$TMP/logs/aapt2-badging.log" || fail APK_IDENTITY_BADGING

base_cert=$("$APKSIGNER" verify --print-certs "$BASE_APK" 2>/dev/null \
    | sed -n 's/^.*certificate SHA-256 digest: //p' | head -n 1)
new_cert=$(sed -n 's/^.*certificate SHA-256 digest: //p' \
    "$TMP/logs/apksigner-verify.log" | head -n 1)
[ -n "$base_cert" ] && [ "$base_cert" = "$new_cert" ] || fail SIGNING_CERT_CHANGED

BAKSMALI_JAR="$BAKSMALI_JAR" BAKSMALI_CP="$JADX_LIB/*" "$ROOT/scripts/r36_apk_abi_gate.sh" \
    "$output_abs" "$REFERENCE_APK" > "$TMP/logs/r367-apk-abi-gate.log"

source_files=$(wc -l < "$TMP/app-sources.list" | tr -d ' ')
source_lines=$(xargs wc -l < "$TMP/app-sources.list" | tail -n 1 | awk '{print $1}')
apk_bytes=$(wc -c < "$output_abs" | tr -d ' ')
dex_bytes=$(unzip -p "$output_abs" 'classes*.dex' | wc -c | tr -d ' ')
apk_sha=$(sha256sum "$output_abs" | awk '{print $1}')
d8_version=$(java -cp "$R8_JAR" com.android.tools.r8.D8 --version 2>&1 | head -n 1)
android_jar_sha=$(sha256sum "$ANDROID_JAR" | awk '{print $1}')
r8_jar_sha=$(sha256sum "$R8_JAR" | awk '{print $1}')

mkdir -p "$EVIDENCE_DIR"
cp "$TMP/java17-behavior-gate.log" "$EVIDENCE_DIR/R367_JAVA17_BEHAVIOR_GATE.log"
cp "$TMP/logs/r367-apk-abi-gate.log" "$EVIDENCE_DIR/R367_APK_ABI_GATE.log"
cp "$TMP/logs/aapt2-badging.log" "$EVIDENCE_DIR/R367_AAPT2_BADGING.log"
cp "$TMP/logs/apksigner-verify.log" "$EVIDENCE_DIR/R367_APKSIGNER_VERIFY.log"
cp "$TMP/logs/zipalign-verify.log" "$EVIDENCE_DIR/R367_ZIPALIGN_VERIFY.log"
cp "$TMP/logs/unzip-test.log" "$EVIDENCE_DIR/R367_APK_ZIP_INTEGRITY.log"
{
    echo "R367_RELEASE_BUILD=PASS"
    echo "JDK_RUNTIME=$(java -version 2>&1 | head -n 1)"
    echo "JAVA17_SOURCE_GATE=PASS|SOURCE=17|TARGET=17|CLASSFILE_MAJOR=61"
    echo "ANDROID_COMPILE_API=OFFICIAL_ANDROID_36_ANDROID_JAR|SHA256=$android_jar_sha"
    echo "DEX_BACKEND=$d8_version|R8_JAR_SHA256=$r8_jar_sha|MIN_API=21|LEGACY_DX=0"
    echo "R367_DB_STARTUP_CLOSURE=CRITICAL_SCHEMA_SINGLE_FLIGHT|DB35_TO_DB36_ADDITIVE_ONLY|LEGACY_SCHEMA_LAZY"
    echo "R367_SERVICE_TRUTH=FOREGROUND_ENTRY_DISTINCT_FROM_BUSINESS_READY|FAILED_ANALYSIS_NOT_HEALTHY"
    echo "R367_ANALYSIS_CLOSURE=UNIQUE_EXECUTION_OWNER|FENCED_CLAIM_HEARTBEAT_HISTORY_COMMIT|ACTIVE_OWNER_DEFERRED|STALE_OWNER_TAKEOVER|BOUNDED_RETRY"
    echo "R367_ACTION_SNAPSHOT_CLOSURE=N4_BOUNDARY_VALID|1200_ROUNDTRIP|PRODUCER_CONSUMER_HASH_PARITY"
    echo "R367_DIAGNOSIS_CLOSURE=READ_ONLY_B12_AND_STATS|INFLIGHT_NOT_FALSE_FAIL|FAILED_ATTEMPT_NOT_HEALTHY"
    echo "SOURCE_JAVA_FILES=$source_files|SOURCE_JAVA_LINES=$source_lines"
    echo "APK=$output_abs"
    echo "APK_BYTES=$apk_bytes|DEX_FILES=$dex_count|UNCOMPRESSED_CLASSES_DEX_BYTES=$dex_bytes"
    echo "APK_SHA256=$apk_sha"
    echo "PACKAGE=com.ethk3.novaonepro|VERSION_CODE=$VERSION_CODE|VERSION_NAME=$VERSION_NAME"
    echo "SIGNATURE_CERT_CONTINUITY=PASS"
    sed -n '1,20p' "$TMP/logs/r367-apk-abi-gate.log"
} | tee "$EVIDENCE_DIR/R367_RELEASE_BUILD_SUMMARY.txt"
sha256sum "$output_abs" > "$EVIDENCE_DIR/R367_APK_SHA256.txt"
