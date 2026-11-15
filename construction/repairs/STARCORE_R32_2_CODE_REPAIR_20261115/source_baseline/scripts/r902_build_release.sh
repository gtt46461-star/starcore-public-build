#!/usr/bin/env bash
set -euo pipefail

ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
BASE=${1:-"$ROOT/../APK/Starcore-Tianshu-V57.16.1-R4.0.1-901-release.apk"}
OUT=${2:-"$ROOT/build/r902-release/Starcore-Tianshu-V57.16.2-R4.0.2-902-release.apk"}
TC=${STARCORE_TOOLCHAIN_ROOT:-"$ROOT/../TOOLCHAIN_USED"}
EVIDENCE=${R902_EVIDENCE_DIR:-"$ROOT/build/r902-release/evidence"}
P12=${STARCORE_SIGNING_P12:?STARCORE_SIGNING_P12 is required}
PASS=${STARCORE_SIGNING_PASSWORD:?STARCORE_SIGNING_PASSWORD is required}
ALIAS=${STARCORE_SIGNING_ALIAS:-starcore-r902}
TYPE=${STARCORE_SIGNING_TYPE:-PKCS12}
ANDROID="$TC/android-platform-36/android.jar"
R8="$TC/r8-9.2.23/r8-9.2.23.jar"
APKTOOL="$TC/apktool/apktool.jar"
LIB="$TC/jadx-node/package/resources/jadx-1.1.0/lib"
ZIPALIGN="$TC/android-signing/zipalign"
APKSIGNER="$TC/android-signing/apksigner"
AAPT2="$TC/aapt/bin/x64/linux/aapt2"
TMP=$(mktemp -d "${TMPDIR:-/tmp}/starcore-r902-build.XXXXXX")
trap 'rm -rf "$TMP"' EXIT HUP INT TERM

fail() { echo "R902_RELEASE_BUILD=FAIL|$1" >&2; exit 1; }
for required in "$BASE" "$P12" "$ANDROID" "$R8" "$APKTOOL" "$ZIPALIGN" "$APKSIGNER" "$AAPT2"; do
    [ -f "$required" ] || fail "MISSING:$(basename "$required")"
done
mkdir -p "$(dirname "$OUT")" "$EVIDENCE" "$TMP/classes" "$TMP/gen/com/ethk3/novaonepro" "$TMP/d8" "$TMP/framework" "$TMP/logs"
cd "$ROOT"

# Provision the already-configured provider key only inside the private temporary build tree.
unzip -p "$BASE" classes.dex > "$TMP/base.dex" || fail BASE_DEX
java -cp "$LIB/*" org.jf.baksmali.Main d "$TMP/base.dex" -o "$TMP/smali" > "$TMP/logs/baksmali.log" 2>&1 || fail BASE_DECODE
BC="$TMP/smali/com/ethk3/novaonepro/BuildConfig.smali"
[ -f "$BC" ] || fail BASE_BUILD_CONFIG
A=$(sed -n 's/^\.field public static final DUOQU_API_KEY_A:Ljava\/lang\/String; = "\([A-Za-z0-9][A-Za-z0-9]*\)"$/\1/p' "$BC")
B=$(sed -n 's/^\.field public static final DUOQU_API_KEY_B:Ljava\/lang\/String; = "\([A-Za-z0-9][A-Za-z0-9]*\)"$/\1/p' "$BC")
KEY="$A$B"
[[ "$KEY" =~ ^[A-Za-z0-9]{8,128}$ ]] || fail API_KEY_FORMAT
sed -e "s/DUOQU_API_KEY_A = \"\"/DUOQU_API_KEY_A = \"$A\"/" \
    -e "s/DUOQU_API_KEY_B = \"\"/DUOQU_API_KEY_B = \"$B\"/" \
    verification/r36-stubs/com/ethk3/novaonepro/BuildConfig.java \
    > "$TMP/gen/com/ethk3/novaonepro/BuildConfig.java"
unset A B KEY

find app/src/main/java -name '*.java' -print | sort > "$TMP/sources.list"
java -m jdk.compiler/com.sun.tools.javac.Main -encoding UTF-8 -source 17 -target 17 \
    -cp "$ANDROID" -d "$TMP/classes" @"$TMP/sources.list" \
    "$TMP/gen/com/ethk3/novaonepro/BuildConfig.java" \
    verification/r36-stubs/com/ethk3/novaonepro/R.java > "$TMP/logs/javac.log" 2>&1 || fail JAVAC
(cd "$TMP/classes" && zip -q -r "$TMP/classes.jar" .)
java -cp "$R8" com.android.tools.r8.D8 --release --min-api 21 --lib "$ANDROID" \
    --output "$TMP/d8" "$TMP/classes.jar" > "$TMP/logs/d8.log" 2>&1 || fail D8

java -jar "$APKTOOL" d -s -p "$TMP/framework" -f -o "$TMP/decoded" "$BASE" \
    > "$TMP/logs/apktool-decode.log" 2>&1 || fail APKTOOL_DECODE
rm -f "$TMP/decoded"/classes*.dex
cp "$TMP/d8"/classes*.dex "$TMP/decoded/"
cp app/src/main/AndroidManifest.xml "$TMP/decoded/AndroidManifest.xml"
cp -R app/src/main/res/. "$TMP/decoded/res/"
sed -i -e 's/^  versionCode: .*/  versionCode: 577200902/' \
    -e 's/^  versionName: .*/  versionName: V57.16.2-R4.0.2-902/' "$TMP/decoded/apktool.yml"
java -jar "$APKTOOL" b -p "$TMP/framework" -o "$TMP/unsigned.apk" "$TMP/decoded" \
    > "$TMP/logs/apktool-build.log" 2>&1 || fail APKTOOL_BUILD
"$ZIPALIGN" -f -p 4 "$TMP/unsigned.apk" "$TMP/aligned.apk" > "$TMP/logs/zipalign.log" 2>&1 || fail ZIPALIGN
"$APKSIGNER" sign --ks "$P12" --ks-type "$TYPE" --ks-key-alias "$ALIAS" \
    --ks-pass "pass:$PASS" --key-pass "pass:$PASS" --min-sdk-version 21 \
    --out "$OUT" "$TMP/aligned.apk" > "$TMP/logs/apksigner-sign.log" 2>&1 || fail SIGN
unset PASS

"$ZIPALIGN" -c -p 4 "$OUT" > "$TMP/logs/zipalign-verify.log" 2>&1 || fail ZIPALIGN_VERIFY
"$APKSIGNER" verify --verbose --print-certs "$OUT" > "$TMP/logs/apksigner-verify.log" 2>&1 || fail SIGN_VERIFY
unzip -t "$OUT" > "$TMP/logs/unzip-test.log" || fail ZIP_VERIFY
"$AAPT2" dump badging "$OUT" > "$TMP/logs/aapt2-badging.log" || fail BADGING
rg -q "package: name='com.ethk3.novaonepro' versionCode='577200902' versionName='V57.16.2-R4.0.2-902'" \
    "$TMP/logs/aapt2-badging.log" || fail IDENTITY
cp "$TMP/logs/"* "$EVIDENCE/"
sha256sum "$OUT" > "$EVIDENCE/R902_APK_SHA256.txt"
cert_sha=$(sed -n 's/^.*certificate SHA-256 digest: //p' "$TMP/logs/apksigner-verify.log" | head -n 1)
base_cert=$("$APKSIGNER" verify --print-certs "$BASE" 2>/dev/null | sed -n 's/^.*certificate SHA-256 digest: //p' | head -n 1)
[ -n "$base_cert" ] && [ "$base_cert" = "$cert_sha" ] || fail SIGNING_CERT_CHANGED
printf 'R902_RELEASE_BUILD=PASS\nAPK=%s\nCERT_SHA256=%s\nSIGNATURE_CONTINUITY=PASS_OVERWRITE_INSTALL\n' \
    "$OUT" "$cert_sha" | tee "$EVIDENCE/R902_RELEASE_BUILD_SUMMARY.txt"
