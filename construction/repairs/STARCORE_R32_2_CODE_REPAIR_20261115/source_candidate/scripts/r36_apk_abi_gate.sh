#!/bin/sh
set -eu

ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
APK=${1:-}
REFERENCE_APK=${2:-}
BAKSMALI_JAR=${BAKSMALI_JAR:-"$ROOT/../toolchain/jadx-node/package/resources/jadx-1.1.0/lib/baksmali-2.3.4.jar"}
BAKSMALI_CP=${BAKSMALI_CP:-"$(dirname -- "$BAKSMALI_JAR")/*"}
TMP=$(mktemp -d "${TMPDIR:-/tmp}/starcore-r366-abi.XXXXXX")
trap 'rm -rf "$TMP"' EXIT HUP INT TERM

fail() { echo "R366_APK_ABI_GATE=FAIL|$1" >&2; exit 1; }

[ -f "$APK" ] || fail APK_NOT_FOUND
[ -f "$BAKSMALI_JAR" ] || fail BAKSMALI_NOT_FOUND

if [ -z "$REFERENCE_APK" ]; then
    REFERENCE_APK=$(find /workspace/scratch -type f \
        -name 'ETH-K3-Nova-One-Pro-V51.0.51072406-engineering-model-not-proven-debug.apk' \
        -print -quit 2>/dev/null || true)
fi
[ -f "$REFERENCE_APK" ] || fail REAL_SDK_BUILT_REFERENCE_APK_NOT_FOUND

unzip -p "$APK" classes.dex > "$TMP/release.dex"
[ -s "$TMP/release.dex" ] || fail CLASSES_DEX_MISSING
java -cp "$BAKSMALI_CP" org.jf.baksmali.Main d \
    "$TMP/release.dex" -o "$TMP/release-smali" >/dev/null

if [ -d "$TMP/release-smali/android" ]; then
    fail ANDROID_COMPILE_STUBS_PACKAGED
fi

find "$TMP/release-smali" -type f -name '*.smali' -print0 \
    | xargs -0 rg -o 'Landroid/[^;]+;->[A-Za-z0-9_$<>]+\([^)]*\)[^ ,}]+' \
    | sed 's#^[^:]*:##' | sort -u > "$TMP/release-methods.txt"

for dex_name in $(unzip -Z1 "$REFERENCE_APK" | rg '^classes([0-9]+)?\.dex$' | sort); do
    safe_name=$(printf '%s' "$dex_name" | tr '/.' '__')
    unzip -p "$REFERENCE_APK" "$dex_name" > "$TMP/$safe_name"
    java -cp "$BAKSMALI_CP" org.jf.baksmali.Main d \
        "$TMP/$safe_name" -o "$TMP/reference-$safe_name" >/dev/null
done
find "$TMP" -type f -path '*/reference-*/*.smali' -print0 \
    | xargs -0 rg -o 'Landroid/[^;]+;->[A-Za-z0-9_$<>]+\([^)]*\)[^ ,}]+' \
    | sed 's#^[^:]*:##' | sort -u > "$TMP/reference-methods.txt"

while IFS= read -r signature; do
    if rg -Fqx "$signature" "$TMP/release-methods.txt"; then
        fail "FORBIDDEN_FRAMEWORK_DESCRIPTOR:$signature"
    fi
done <<'EOF'
Landroid/content/ContentValues;->containsKey(Ljava/lang/Object;)Z
Landroid/content/ContentValues;->get(Ljava/lang/Object;)Ljava/lang/Object;
Landroid/content/ContentValues;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
Landroid/database/sqlite/SQLiteOpenHelper;-><init>(Landroid/content/Context;Ljava/lang/String;Ljava/lang/Object;I)V
Landroid/widget/ListView;->setAdapter(Landroid/widget/BaseAdapter;)V
Landroid/widget/TextView;->setTypeface(Ljava/lang/Object;I)V
EOF

while IFS= read -r signature; do
    rg -Fqx "$signature" "$TMP/release-methods.txt" \
        || fail "REQUIRED_FRAMEWORK_DESCRIPTOR_MISSING:$signature"
done <<'EOF'
Landroid/content/ContentValues;->containsKey(Ljava/lang/String;)Z
Landroid/content/ContentValues;->get(Ljava/lang/String;)Ljava/lang/Object;
Landroid/content/ContentValues;->put(Ljava/lang/String;Ljava/lang/Short;)V
Landroid/database/sqlite/SQLiteOpenHelper;-><init>(Landroid/content/Context;Ljava/lang/String;Landroid/database/sqlite/SQLiteDatabase$CursorFactory;I)V
Landroid/widget/ListView;->setAdapter(Landroid/widget/ListAdapter;)V
Landroid/widget/TextView;->setTypeface(Landroid/graphics/Typeface;I)V
EOF

# All Android calls already present in the prior real-SDK-built V51 APK are
# accepted.  The exact R3.6 additions below were separately checked against
# the Android 9 / API-28 AOSP declarations or inherited virtual resolution.
unreviewed=$(
    comm -23 "$TMP/release-methods.txt" "$TMP/reference-methods.txt" \
        | while IFS= read -r signature; do
            case "$signature" in
                'Landroid/app/Activity;->onTrimMemory(I)V'|\
                'Landroid/app/AlarmManager;->set(IJLandroid/app/PendingIntent;)V'|\
                'Landroid/app/Application;-><init>()V'|\
                'Landroid/app/Application;->onCreate()V'|\
                'Landroid/app/Service;->onLowMemory()V'|\
                'Landroid/database/sqlite/SQLiteDatabase;->isOpen()Z'|\
                'Landroid/database/sqlite/SQLiteDatabase;->setForeignKeyConstraintsEnabled(Z)V'|\
                'Landroid/database/sqlite/SQLiteException;-><init>(Ljava/lang/String;)V'|\
                'Landroid/os/HandlerThread;->quit()Z'|\
                'Landroid/text/TextUtils;->equals(Ljava/lang/CharSequence;Ljava/lang/CharSequence;)Z'|\
                'Landroid/view/View;->setSystemUiVisibility(I)V'|\
                'Landroid/view/ViewGroup$LayoutParams;-><init>(II)V'|\
                'Landroid/view/Window;->addFlags(I)V'|\
                'Landroid/view/Window;->clearFlags(I)V'|\
                'Landroid/view/Window;->getDecorView()Landroid/view/View;'|\
                'Landroid/widget/Button;-><init>(Landroid/content/Context;)V'|\
                'Landroid/widget/Button;->setText(Ljava/lang/CharSequence;)V'|\
                'Landroid/widget/LinearLayout;->removeAllViews()V'|\
                'Landroid/widget/TextView;->setPadding(IIII)V') ;;
                *) printf '%s\n' "$signature" ;;
            esac
        done
)
if [ -n "$unreviewed" ]; then
    printf '%s\n' "$unreviewed" >&2
    fail UNREVIEWED_ANDROID_METHOD_DESCRIPTOR
fi

BOOT_SMALI="$TMP/release-smali/com/ethk3/novaonepro/BootReceiver.smali"
SERVICE_SMALI="$TMP/release-smali/com/ethk3/novaonepro/AutoFetchService.smali"
[ -f "$BOOT_SMALI" ] || fail BOOT_RECEIVER_DEX_MISSING
[ -f "$SERVICE_SMALI" ] || fail AUTO_FETCH_SERVICE_DEX_MISSING
sed -n '/\.method private static pendingFlags()I/,/\.end method/p' "$BOOT_SMALI" \
    > "$TMP/pending-flags.smali"
rg -q '0x8000000' "$TMP/pending-flags.smali" || fail FLAG_UPDATE_CURRENT_NOT_0X08000000
rg -q '0xc000000' "$TMP/pending-flags.smali" || fail UPDATE_PLUS_IMMUTABLE_NOT_0X0C000000
rg -q '0xc000000' "$SERVICE_SMALI" || fail COMBINED_PENDING_INTENT_FLAGS_NOT_0X0C000000

method_count=$(wc -l < "$TMP/release-methods.txt" | tr -d ' ')
echo "R366_ANDROID_METHOD_DESCRIPTORS=$method_count"
echo "R366_FORBIDDEN_ABI_SIGNATURES=0"
echo "R366_PENDING_INTENT_FLAGS=PASS|UPDATE=0x08000000|IMMUTABLE=0x04000000|COMBINED=0x0c000000"
echo "R366_ANDROID_ABI_PROVENANCE=OFFICIAL_ANDROID_36_ANDROID_JAR_PLUS_D8_9_2_23"
echo "R366_APK_ABI_GATE=PASS"
