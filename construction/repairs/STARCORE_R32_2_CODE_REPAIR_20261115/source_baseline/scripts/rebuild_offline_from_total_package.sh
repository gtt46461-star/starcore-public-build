#!/bin/sh
set -eu

PACKAGE_ROOT=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
SOURCE_ZIP="$PACKAGE_ROOT/SOURCE/Starcore-Tianshu-V57.15.3-R3.6.6-source.zip"
BASE_APK="$PACKAGE_ROOT/BASELINES/Starcore-Tianshu-V57.15.3-R3.6.5-release.apk"
REFERENCE_APK="$PACKAGE_ROOT/BASELINES/ETH-K3-Nova-One-Pro-V51-real-sdk-reference.apk"
SIGNING_KIT="$PACKAGE_ROOT/PRIVATE_SIGNING/STARCORE-V57-release-signing-kit.zip"
TOOLCHAIN="$PACKAGE_ROOT/TOOLCHAIN_USED"
OUTPUT_APK="$PACKAGE_ROOT/APK/Starcore-Tianshu-V57.15.3-R3.6.6-offline-rebuilt.apk"
WORK="$PACKAGE_ROOT/OFFLINE_REBUILD_WORK"

fail() { echo "R366_OFFLINE_REBUILD=FAIL|$1" >&2; exit 1; }
for file in "$SOURCE_ZIP" "$BASE_APK" "$REFERENCE_APK" "$SIGNING_KIT"; do
    [ -f "$file" ] || fail "MISSING:$(basename -- "$file")"
done
[ -d "$TOOLCHAIN" ] || fail TOOLCHAIN_MISSING
[ ! -e "$WORK" ] || fail OFFLINE_REBUILD_WORK_ALREADY_EXISTS

mkdir -p "$WORK/source" "$WORK/evidence"
unzip -q "$SOURCE_ZIP" -d "$WORK/source"
PROJECT=$(find "$WORK/source" -type f -name settings.gradle -print -quit)
[ -n "$PROJECT" ] || fail SETTINGS_GRADLE_NOT_FOUND
PROJECT=$(dirname -- "$PROJECT")

STARCORE_TOOLCHAIN_ROOT="$TOOLCHAIN" \
STARCORE_SIGN_TOOLS="$TOOLCHAIN/android-signing" \
R366_EVIDENCE_DIR="$WORK/evidence" \
    "$PROJECT/scripts/r36_build_release.sh" \
    "$BASE_APK" "$OUTPUT_APK" "$SIGNING_KIT" "$REFERENCE_APK"

echo "R366_OFFLINE_REBUILD=PASS"
echo "APK=$OUTPUT_APK"
sha256sum "$OUTPUT_APK"
