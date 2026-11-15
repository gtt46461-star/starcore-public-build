#!/usr/bin/env bash
set -euo pipefail
ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
BASE=${1:-/mnt/data/STARCORE_TIANSHU_V57.15.3-R3.6.17-820_FULL_ALL_IN_ONE/APK/Starcore-Tianshu-V57.15.3-R3.6.17-820-release.apk}
OUT=${2:-/mnt/data/r900_release/Starcore-Tianshu-V57.16.1-R4.0.1-901-release.apk}
TC=${STARCORE_TOOLCHAIN_ROOT:-/mnt/data/STARCORE_TIANSHU_V57.15.3-R3.6.17-820_FULL_ALL_IN_ONE/TOOLCHAIN_USED}
ANDROID="$TC/android-platform-36/android.jar"; R8="$TC/r8-9.2.23/r8-9.2.23.jar"; APKTOOL="$TC/apktool/apktool.jar"
LIB="$TC/jadx-node/package/resources/jadx-1.1.0/lib"; ZIPALIGN="$TC/android-signing/zipalign"; APKSIGNER="$TC/android-signing/apksigner"; AAPT2="$TC/aapt/bin/x64/linux/aapt2"
P12=/mnt/data/signing_private/STARCORE-V57-release-signing.p12; CRED=/mnt/data/signing_private/SIGNING-CREDENTIALS.txt
TMP=$(mktemp -d /mnt/data/r900-build.XXXXXX); trap 'rm -rf "$TMP"' EXIT
mkdir -p "$(dirname "$OUT")" "$TMP/classes" "$TMP/gen/com/ethk3/novaonepro" "$TMP/d8" "$TMP/framework" "$TMP/logs"
# Extract provisioned key only inside temp memory/disk; never log it.
unzip -p "$BASE" classes.dex > "$TMP/base.dex"
java -cp "$LIB/*" org.jf.baksmali.Main d "$TMP/base.dex" -o "$TMP/smali" >/dev/null
BC="$TMP/smali/com/ethk3/novaonepro/BuildConfig.smali"
A=$(sed -n 's/^\.field public static final DUOQU_API_KEY_A:Ljava\/lang\/String; = "\([A-Za-z0-9][A-Za-z0-9]*\)"$/\1/p' "$BC")
B=$(sed -n 's/^\.field public static final DUOQU_API_KEY_B:Ljava\/lang\/String; = "\([A-Za-z0-9][A-Za-z0-9]*\)"$/\1/p' "$BC")
KEY="$A$B"; [[ "$KEY" =~ ^[A-Za-z0-9]{8,128}$ ]] || { echo R900_BUILD_FAIL_API_KEY; exit 2; }
sed -e "s/DUOQU_API_KEY_A = \"\"/DUOQU_API_KEY_A = \"$A\"/" -e "s/DUOQU_API_KEY_B = \"\"/DUOQU_API_KEY_B = \"$B\"/" verification/r36-stubs/com/ethk3/novaonepro/BuildConfig.java > "$TMP/gen/com/ethk3/novaonepro/BuildConfig.java"
find app/src/main/java -name '*.java' | sort > "$TMP/sources.list"
java -m jdk.compiler/com.sun.tools.javac.Main -encoding UTF-8 -source 17 -target 17 -cp "$ANDROID" -d "$TMP/classes" @"$TMP/sources.list" "$TMP/gen/com/ethk3/novaonepro/BuildConfig.java" verification/r36-stubs/com/ethk3/novaonepro/R.java >"$TMP/logs/javac.log" 2>&1
(cd "$TMP/classes" && zip -q -r "$TMP/classes.jar" .)
java -cp "$R8" com.android.tools.r8.D8 --release --min-api 21 --lib "$ANDROID" --output "$TMP/d8" "$TMP/classes.jar" >"$TMP/logs/d8.log" 2>&1
java -jar "$APKTOOL" d -s -p "$TMP/framework" -f -o "$TMP/decoded" "$BASE" >"$TMP/logs/decode.log" 2>&1
rm -f "$TMP/decoded"/classes*.dex; cp "$TMP/d8"/classes*.dex "$TMP/decoded/"
python3 - "$TMP/decoded/apktool.yml" <<'PY'
import sys,re
p=sys.argv[1]; s=open(p,encoding='utf-8').read(); s=re.sub(r'(?m)^  versionCode: .*$', '  versionCode: 577200901', s); s=re.sub(r'(?m)^  versionName: .*$', '  versionName: V57.16.1-R4.0.1-901', s); open(p,'w',encoding='utf-8').write(s)
PY
python3 - "$TMP/decoded" <<'PY'
import sys,glob,re,html
root=sys.argv[1]
repl={
'label_build_identity':'R900 工程完整性已校验；MODEL_PROVEN=false 时保持 SHADOW ONLY，不会创建 REAL BET',
'diagnosis_title':'R900 完整诊断与完整性校验',
'footer_single_gate':'Nova One Pro R900 · SettlementCoordinator 唯一结算 · SHADOW 不计真实盈亏 · MODEL_NOT_PROVEN'}
for p in glob.glob(root+'/res/values*/strings.xml'):
    s=open(p,encoding='utf-8').read()
    for k,v in repl.items():
        esc=html.escape(v,quote=False)
        s=re.sub(r'(<string\s+name="'+re.escape(k)+r'"[^>]*>).*?(</string>)',lambda m:m.group(1)+esc+m.group(2),s,flags=re.S)
    open(p,'w',encoding='utf-8').write(s)
PY
java -jar "$APKTOOL" b -p "$TMP/framework" -o "$TMP/unsigned.apk" "$TMP/decoded" >"$TMP/logs/build.log" 2>&1
"$ZIPALIGN" -f -p 4 "$TMP/unsigned.apk" "$TMP/aligned.apk"
ALIAS=$(sed -n 's/^Alias:[[:space:]]*//p' "$CRED" | tr -d '\r'); PASS=$(sed -n 's/^Password:[[:space:]]*//p' "$CRED" | tr -d '\r'); TYPE=$(sed -n 's/^Keystore type:[[:space:]]*//p' "$CRED" | tr -d '\r')
"$APKSIGNER" sign --ks "$P12" --ks-type "$TYPE" --ks-key-alias "$ALIAS" --ks-pass "pass:$PASS" --key-pass "pass:$PASS" --out "$OUT" "$TMP/aligned.apk" >/dev/null
"$ZIPALIGN" -c -p 4 "$OUT"
"$APKSIGNER" verify --verbose --print-certs "$OUT" >"$TMP/logs/sign-verify.log"
unzip -t "$OUT" >"$TMP/logs/unzip.log"
"$AAPT2" dump badging "$OUT" >"$TMP/logs/badging.log"
grep -q "package: name='com.ethk3.novaonepro' versionCode='577200901' versionName='V57.16.1-R4.0.1-901'" "$TMP/logs/badging.log"
BASECERT=$("$APKSIGNER" verify --print-certs "$BASE" 2>/dev/null | sed -n 's/^.*certificate SHA-256 digest: //p' | head -1)
NEWCERT=$(sed -n 's/^.*certificate SHA-256 digest: //p' "$TMP/logs/sign-verify.log" | head -1)
[ -n "$BASECERT" ] && [ "$BASECERT" = "$NEWCERT" ]
mkdir -p /mnt/data/r900_release/evidence
cp "$TMP/logs/"* /mnt/data/r900_release/evidence/
sha256sum "$OUT" > /mnt/data/r900_release/APK_SHA256.txt
printf 'R900_BUILD=PASS\nAPK=%s\nDEX_COUNT=%s\nSOURCE_FILES=%s\n' "$OUT" "$(find "$TMP/d8" -name 'classes*.dex' | wc -l)" "$(wc -l < "$TMP/sources.list")"
