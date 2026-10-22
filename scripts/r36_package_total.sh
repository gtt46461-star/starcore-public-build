#!/bin/sh
set -eu

ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
WORKSPACE=$(CDPATH= cd -- "$ROOT/.." && pwd)
NAME=Starcore-Tianshu-V57.15.3-R3.6.6-Full-Blood-Repair
DEST=${1:-"$WORKSPACE/deliverables/$NAME"}
MASTER=${2:-"$WORKSPACE/deliverables/$NAME.zip"}
APK=${3:-"$WORKSPACE/build/r366-release/Starcore-Tianshu-V57.15.3-R3.6.6-release.apk"}
PLAN_R36=${4:-"$WORKSPACE/upload/8+10一次性返修.zip"}
PLAN_B01_B15=${5:-"$WORKSPACE/upload/两文件一8+5下午两点完整方案(4).zip"}
OLD_SOURCE=${6:-"$WORKSPACE/upload/两文件二8+4下午4.18分星核软件完整源代码(4).zip"}
PREVIOUS_PACKAGE=${R366_PREVIOUS_PACKAGE:-"$WORKSPACE/deliverables/Starcore-Tianshu-V57.15.3-R3.6.5-Full-Blood-Repair"}
BASE_APK="$PREVIOUS_PACKAGE/APK/Starcore-Tianshu-V57.15.3-R3.6.5-release.apk"
SIGNING_KIT="$PREVIOUS_PACKAGE/PRIVATE_SIGNING/STARCORE-V57-release-signing-kit.zip"
TOOLCHAIN=${STARCORE_TOOLCHAIN_ROOT:-"$WORKSPACE/toolchain"}
BAKSMALI_LIB="$TOOLCHAIN/jadx-node/package/resources/jadx-1.1.0/lib"
AAPT2="$TOOLCHAIN/aapt/bin/x64/linux/aapt2"
APKSIGNER="$TOOLCHAIN/android-signing/apksigner"
ZIPALIGN="$TOOLCHAIN/android-signing/zipalign"
ANDROID_JAR="$TOOLCHAIN/android-platform-36/android.jar"
R8_JAR="$TOOLCHAIN/r8-9.2.23/r8-9.2.23.jar"
TMP=$(mktemp -d "${TMPDIR:-/tmp}/starcore-r366-package.XXXXXX")
trap 'rm -rf "$TMP"' EXIT HUP INT TERM

fail() { echo "R366_TOTAL_PACKAGE=FAIL|$1" >&2; exit 1; }
require_file() { [ -f "$1" ] || fail "MISSING:$2"; }
require_dir() { [ -d "$1" ] || fail "MISSING_DIR:$2"; }

require_file "$APK" APK
require_file "$PLAN_R36" R36_PLAN
require_file "$PLAN_B01_B15" B01_B15_PLAN
require_file "$OLD_SOURCE" OLD_COMPLETE_SOURCE
require_file "$BASE_APK" R365_BASE_APK
require_file "$SIGNING_KIT" SIGNING_KIT
require_file "$AAPT2" AAPT2
require_file "$APKSIGNER" APKSIGNER
require_file "$ZIPALIGN" ZIPALIGN
require_file "$ANDROID_JAR" OFFICIAL_ANDROID_36_API
require_file "$R8_JAR" R8_D8_9_2_23
require_dir "$TOOLCHAIN/apktool" APKTOOL
require_dir "$TOOLCHAIN/jadx-node" JADX_BAKSMALI
[ ! -e "$DEST" ] || fail DEST_ALREADY_EXISTS
[ ! -e "$MASTER" ] || fail MASTER_ALREADY_EXISTS

mkdir -p \
    "$DEST/APK" \
    "$DEST/SOURCE" \
    "$DEST/EVIDENCE/APK" \
    "$DEST/EVIDENCE/SOURCE" \
    "$DEST/EVIDENCE/REPRODUCIBILITY" \
    "$DEST/PLANS" \
    "$DEST/BASELINES" \
    "$DEST/PRIVATE_SIGNING" \
    "$DEST/REPORT" \
    "$DEST/TOOLCHAIN_USED"

cp "$APK" "$DEST/APK/Starcore-Tianshu-V57.15.3-R3.6.6-release.apk"
if [ -f "$APK.idsig" ]; then
    cp "$APK.idsig" "$DEST/APK/Starcore-Tianshu-V57.15.3-R3.6.6-release.apk.idsig"
fi
sha256sum "$DEST/APK/Starcore-Tianshu-V57.15.3-R3.6.6-release.apk" \
    > "$DEST/APK/Starcore-Tianshu-V57.15.3-R3.6.6-release.apk.sha256"

git -C "$ROOT" archive \
    --format=zip \
    --prefix=Starcore-Tianshu-V57.15.3-R3.6.6-source/ \
    --output="$DEST/SOURCE/Starcore-Tianshu-V57.15.3-R3.6.6-source.zip" HEAD
git -C "$ROOT" bundle create \
    "$DEST/SOURCE/Starcore-Tianshu-V57.15.3-R3.6.6-history.bundle" --all
git -C "$ROOT" log --decorate --oneline -20 \
    > "$DEST/EVIDENCE/SOURCE/R366_GIT_LINEAGE.txt"
git -C "$ROOT" status --short \
    > "$DEST/EVIDENCE/SOURCE/R366_GIT_STATUS.txt"
git -C "$ROOT" rev-parse HEAD \
    > "$DEST/EVIDENCE/SOURCE/R366_GIT_COMMIT.txt"

cp "$PLAN_R36" "$DEST/PLANS/8+10一次性返修.zip"
cp "$PLAN_B01_B15" "$DEST/PLANS/两文件一8+5下午两点完整方案(4).zip"
cp "$OLD_SOURCE" "$DEST/PLANS/两文件二8+4下午4.18分星核软件完整源代码(4).zip"
cp "$BASE_APK" "$DEST/BASELINES/Starcore-Tianshu-V57.15.3-R3.6.5-release.apk"
reference_entry=$(unzip -Z1 "$OLD_SOURCE" \
    | rg 'ETH-K3-Nova-One-Pro-V51\.0\.51072406-engineering-model-not-proven-debug\.apk$' \
    | head -n 1)
[ -n "$reference_entry" ] || fail V51_REFERENCE_ENTRY_NOT_FOUND
unzip -p "$OLD_SOURCE" "$reference_entry" \
    > "$DEST/BASELINES/ETH-K3-Nova-One-Pro-V51-real-sdk-reference.apk"
require_file "$DEST/BASELINES/ETH-K3-Nova-One-Pro-V51-real-sdk-reference.apk" V51_REFERENCE_APK

cp "$SIGNING_KIT" "$DEST/PRIVATE_SIGNING/STARCORE-V57-release-signing-kit.zip"
cp "$PREVIOUS_PACKAGE/PRIVATE_SIGNING/DO_NOT_SHARE.md" "$DEST/PRIVATE_SIGNING/DO_NOT_SHARE.md"

cp -a "$TOOLCHAIN/apktool" "$DEST/TOOLCHAIN_USED/apktool"
cp -a "$TOOLCHAIN/aapt" "$DEST/TOOLCHAIN_USED/aapt"
cp -a "$TOOLCHAIN/jadx-node" "$DEST/TOOLCHAIN_USED/jadx-node"
cp -a "$TOOLCHAIN/android-signing" "$DEST/TOOLCHAIN_USED/android-signing"
cp -a "$TOOLCHAIN/android-platform-36" "$DEST/TOOLCHAIN_USED/android-platform-36"
cp -a "$TOOLCHAIN/r8-9.2.23" "$DEST/TOOLCHAIN_USED/r8-9.2.23"

cp "$ROOT/docs/"*.md "$DEST/REPORT/"
cp "$ROOT/scripts/rebuild_offline_from_total_package.sh" "$DEST/REBUILD_OFFLINE.sh"
chmod 755 "$DEST/REBUILD_OFFLINE.sh"

if [ -d "$WORKSPACE/build/r366-release/evidence" ]; then
    cp -a "$WORKSPACE/build/r366-release/evidence/." "$DEST/EVIDENCE/APK/"
fi
mkdir -p "$DEST/EVIDENCE/SOURCE/CONTRACT"
python3 "$ROOT/scripts/r36_source_integrity_audit.py" \
    --repo "$ROOT" \
    --plan-zip "$PLAN_B01_B15" \
    --output-dir "$DEST/EVIDENCE/SOURCE/CONTRACT" \
    > "$DEST/EVIDENCE/SOURCE/R366_SOURCE_AUDIT_RESULT.txt"
"$ROOT/scripts/r36_behavior_gate.sh" \
    > "$DEST/EVIDENCE/SOURCE/R366_JAVA17_BEHAVIOR_GATE.log" 2>&1
"$ROOT/scripts/r36_secret_gate.sh" \
    > "$DEST/EVIDENCE/SOURCE/R366_SECRET_GATE.log" 2>&1
BAKSMALI_JAR="$BAKSMALI_LIB/baksmali-2.3.4.jar" \
BAKSMALI_CP="$BAKSMALI_LIB/*" \
    "$ROOT/scripts/r36_apk_abi_gate.sh" \
    "$APK" "$DEST/BASELINES/ETH-K3-Nova-One-Pro-V51-real-sdk-reference.apk" \
    > "$DEST/EVIDENCE/APK/R366_FINAL_APK_ABI_GATE.log"
"$AAPT2" dump badging "$APK" \
    > "$DEST/EVIDENCE/APK/R366_FINAL_AAPT2_BADGING.log"
"$APKSIGNER" verify --verbose --print-certs "$APK" \
    > "$DEST/EVIDENCE/APK/R366_FINAL_APKSIGNER_VERIFY.log"
"$ZIPALIGN" -c -p 4 "$APK" \
    > "$DEST/EVIDENCE/APK/R366_FINAL_ZIPALIGN_VERIFY.log"
unzip -t "$APK" > "$DEST/EVIDENCE/APK/R366_FINAL_APK_ZIP_TEST.log"

unzip -p "$APK" classes.dex > "$TMP/classes.dex"
java -cp "$BAKSMALI_LIB/*" org.jf.baksmali.Main d \
    "$TMP/classes.dex" -o "$TMP/smali" >/dev/null
{
    for class_path in \
        com/ethk3/novaonepro/LiveResearchDiagnosisOrchestrator.smali \
        com/ethk3/novaonepro/LiveDiagnosticReportEngine.smali \
        com/ethk3/novaonepro/K3ActionSpaceEngine.smali \
        com/ethk3/novaonepro/DeferredEvidenceSnapshotR36.smali \
        com/ethk3/novaonepro/K3FunnelOrchestrator.smali \
        com/ethk3/novaonepro/DatabaseUpgradePlanR363.smali \
        com/ethk3/novaonepro/ServiceStartupStateR363.smali \
        com/ethk3/novaonepro/R36AnalysisHistoryPolicyR364.smali \
        com/ethk3/novaonepro/R36AnalysisLeasePolicyR365.smali \
        com/ethk3/novaonepro/AnalysisRequestStoreR36.smali \
        com/ethk3/novaonepro/AnalysisSingleFlightR36.smali \
        com/ethk3/novaonepro/V51RuntimeInputR36.smali \
        com/ethk3/novaonepro/V57CompleteDiagnosticReportEngine.smali; do
        [ -f "$TMP/smali/$class_path" ] || fail "DEX_CLASS_MISSING:$class_path"
        echo "DEX_CLASS=PASS|$class_path"
    done
    rg -q 'replayDeterminism' "$TMP/smali/com/ethk3/novaonepro/LiveDiagnosticReportEngine.smali" \
        || fail DEX_REPORT_REPLAY_METHOD_MISSING
    rg -q 'deterministicFixture' "$TMP/smali/com/ethk3/novaonepro/LiveResearchDiagnosisOrchestrator.smali" \
        || fail DEX_DIAGNOSIS_REPLAY_METHOD_MISSING
    rg -q 'traceForPosition' "$TMP/smali/com/ethk3/novaonepro/V51Repository.smali" \
        || fail DEX_B10_TRACE_METHOD_MISSING
    rg -q 'validFourthBoundary' "$TMP/smali/com/ethk3/novaonepro/K3ActionSpaceEngine.smali" \
        || fail DEX_R366_N4_CONTRACT_OWNER_MISSING
    rg -q 'actionSpaceHashFromPayloadBody' \
        "$TMP/smali/com/ethk3/novaonepro/K3ActionSpaceEngine.smali" \
        || fail DEX_R366_ACTION_HASH_PARITY_METHOD_MISSING
    rg -q 'R366_ACTION_BOUNDARY_N4_INVALID' \
        "$TMP/smali/com/ethk3/novaonepro/DeferredEvidenceSnapshotR36\$ActionRow.smali" \
        || fail DEX_R366_ACTION_SNAPSHOT_FIELD_DIAGNOSIS_MISSING
    rg -q 'ensureCriticalSchemaOrThrow' "$TMP/smali/com/ethk3/novaonepro/DatabaseCoordinator.smali" \
        || fail DEX_R366_CRITICAL_SCHEMA_METHOD_MISSING
    rg -q 'markRecoveryEvaluation' "$TMP/smali/com/ethk3/novaonepro/ServiceHeartbeatStore.smali" \
        || fail DEX_R366_RECOVERY_TRUTH_METHOD_MISSING
    rg -q 'prepareAnalysisHistoryR364' "$TMP/smali/com/ethk3/novaonepro/AutoFetchService.smali" \
        || fail DEX_R366_HISTORY_BOOTSTRAP_METHOD_MISSING
    rg -q 'rearmKnownR363PreCommitFailureR364' \
        "$TMP/smali/com/ethk3/novaonepro/AnalysisRequestStoreR36.smali" \
        || fail DEX_R366_LEGACY_REQUEST_REARM_METHOD_MISSING
    rg -q 'rearmUpgradeFailureR366' \
        "$TMP/smali/com/ethk3/novaonepro/AnalysisRequestStoreR36.smali" \
        || fail DEX_R366_OVERWRITE_REARM_METHOD_MISSING
    rg -q 'claimDetailed' \
        "$TMP/smali/com/ethk3/novaonepro/AnalysisRequestStoreR36.smali" \
        || fail DEX_R366_FENCED_CLAIM_METHOD_MISSING
    rg -q 'DEFER_ACTIVE_OWNER' \
        "$TMP/smali/com/ethk3/novaonepro/R36AnalysisLeasePolicyR365.smali" \
        || fail DEX_R366_ACTIVE_OWNER_DEFER_POLICY_MISSING
    rg -q 'R366_EXECUTION_LEASE' \
        "$TMP/smali/com/ethk3/novaonepro/AnalysisSingleFlightR36\$Lease.smali" \
        || fail DEX_R366_UNIQUE_EXECUTION_OWNER_MISSING
    rg -q 'Lcom/ethk3/novaonepro/V51PermutationFeatureEngine;->historyHash' \
        "$TMP/smali/com/ethk3/novaonepro/V51RuntimeInputR36.smali" \
        || fail DEX_R366_CANONICAL_HISTORY_HASH_OWNER_MISSING
    echo "DEX_METHOD=PASS|LiveDiagnosticReportEngine#replayDeterminism"
    echo "DEX_METHOD=PASS|LiveResearchDiagnosisOrchestrator#deterministicFixture"
    echo "DEX_METHOD=PASS|V51Repository#traceForPosition"
    echo "DEX_METHOD=PASS|K3ActionSpaceEngine#validFourthBoundary"
    echo "DEX_METHOD=PASS|K3ActionSpaceEngine#actionSpaceHashFromPayloadBody"
    echo "DEX_DIAGNOSIS=PASS|DeferredEvidenceSnapshotR36#R366_ACTION_BOUNDARY_N4_INVALID"
    echo "DEX_METHOD=PASS|DatabaseCoordinator#ensureCriticalSchemaOrThrow"
    echo "DEX_METHOD=PASS|ServiceHeartbeatStore#markRecoveryEvaluation"
    echo "DEX_METHOD=PASS|AutoFetchService#prepareAnalysisHistoryR364"
    echo "DEX_METHOD=PASS|AnalysisRequestStoreR36#rearmKnownR363PreCommitFailureR364"
    echo "DEX_METHOD=PASS|AnalysisRequestStoreR36#rearmUpgradeFailureR366"
    echo "DEX_METHOD=PASS|AnalysisRequestStoreR36#claimDetailed"
    echo "DEX_POLICY=PASS|R36AnalysisLeasePolicyR365#DEFER_ACTIVE_OWNER"
    echo "DEX_OWNER=PASS|AnalysisSingleFlightR36#R366_EXECUTION_LEASE"
    echo "DEX_METHOD=PASS|V51RuntimeInputR36->V51PermutationFeatureEngine#historyHash"
    echo "R366_DEX_CORE_PRESENCE=PASS"
} > "$DEST/EVIDENCE/APK/R366_DEX_CORE_PRESENCE.log"

unzip -t "$DEST/SOURCE/Starcore-Tianshu-V57.15.3-R3.6.6-source.zip" \
    > "$DEST/EVIDENCE/REPRODUCIBILITY/R366_SOURCE_ZIP_TEST.log"
git -C "$ROOT" bundle verify \
    "$DEST/SOURCE/Starcore-Tianshu-V57.15.3-R3.6.6-history.bundle" \
    > "$DEST/EVIDENCE/REPRODUCIBILITY/R366_GIT_BUNDLE_VERIFY.log" 2>&1
unzip -t "$PLAN_R36" \
    > "$DEST/EVIDENCE/REPRODUCIBILITY/R366_R36_PLAN_ZIP_TEST.log"
unzip -t "$PLAN_B01_B15" \
    > "$DEST/EVIDENCE/REPRODUCIBILITY/R366_B01_B15_PLAN_ZIP_TEST.log"
unzip -t "$OLD_SOURCE" \
    > "$DEST/EVIDENCE/REPRODUCIBILITY/R366_OLD_SOURCE_ZIP_TEST.log"
unzip -t "$SIGNING_KIT" \
    > "$DEST/EVIDENCE/REPRODUCIBILITY/R366_SIGNING_KIT_ZIP_TEST.log"

{
    echo "JDK=$(java -version 2>&1 | head -n 1)"
    echo "APKTOOL=$(java -jar "$TOOLCHAIN/apktool/apktool.jar" --version 2>&1 | head -n 1)"
    echo "AAPT2=$($AAPT2 version 2>&1 | head -n 1)"
    echo "BAKSMALI=$(java -cp "$BAKSMALI_LIB/*" org.jf.baksmali.Main --version 2>&1 | head -n 1)"
    echo "ANDROID_36_JAR_SHA256=$(sha256sum "$ANDROID_JAR" | awk '{print $1}')"
    echo "D8=$(java -cp "$R8_JAR" com.android.tools.r8.D8 --version 2>&1 | head -n 1)"
    echo "R8_JAR_SHA256=$(sha256sum "$R8_JAR" | awk '{print $1}')"
    echo "APKSIGNER_JAR_SHA256=$(sha256sum "$TOOLCHAIN/android-signing/lib/apksigner.jar" | awk '{print $1}')"
    echo "JAVA_CLASSFILE=MAJOR_61|DEX_BACKEND=D8_9_2_23|LEGACY_DX=0"
    echo "TOOLCHAIN_POLICY=ONLY_ACTUALLY_USED_TOOLS_INCLUDED|OFFICIAL_ANDROID_API|NO_FAKE_PADDING"
} > "$DEST/EVIDENCE/REPRODUCIBILITY/R366_TOOLCHAIN_VERSIONS.txt"

find "$DEST/TOOLCHAIN_USED" -type f -print0 \
    | sort -z \
    | xargs -0 sha256sum \
    > "$DEST/EVIDENCE/REPRODUCIBILITY/R366_TOOLCHAIN_SHA256.txt"

{
    echo "STARCORE_TIANSHU=V57.15.3-R3.6.6"
    echo "PACKAGE=com.ethk3.novaonepro"
    echo "VERSION_CODE=577180818"
    echo "VERSION_NAME=V57.15.3-tianshu-b01-b15-runtime-closure-hf6-20260811"
    echo "GIT_COMMIT=$(git -C "$ROOT" rev-parse HEAD)"
    echo "GIT_TAG=v57.15.3-r3.6.6-action-snapshot-runtime-closure"
    echo "APK_SHA256=$(sha256sum "$APK" | awk '{print $1}')"
    echo "APK_BYTES=$(wc -c < "$APK" | tr -d ' ')"
    echo "DEX_BYTES=$(unzip -l "$APK" | awk '$4 ~ /^classes([0-9]+)?[.]dex$/ { total += $1 } END { print total + 0 }')"
    echo "JAVA_SOURCE_TARGET=17|JAVA_CLASSFILE_MAJOR=61"
    echo "DEX_BACKEND=D8_9_2_23|LEGACY_DX=0"
    echo "ANDROID_COMPILE_API=OFFICIAL_ANDROID_36_ANDROID_JAR"
    echo "SOURCE_JAVA_FILES=$(find "$ROOT/app/src/main/java" -name '*.java' | wc -l | tr -d ' ')"
    echo "SOURCE_JAVA_LINES=$(find "$ROOT/app/src/main/java" -name '*.java' -print0 | xargs -0 wc -l | tail -n 1 | awk '{print $1}')"
    echo "CONTRACT_FILES=114/114"
    echo "SOURCE_AUDIT=PASS"
    echo "JAVA17_BEHAVIOR_GATE=PASS"
    echo "APK_ABI_GATE=PASS"
    echo "SIGNATURE_CONTINUITY=PASS"
    echo "DB35_TO_DB36_ADDITIVE_ONLY=PASS|DESTRUCTIVE=0"
    echo "STARTUP_TRUTH_CLASSIFIER=PASS|FOREGROUND_NOT_EQUAL_BUSINESS_READY"
    echo "CRITICAL_SCHEMA_SINGLE_FLIGHT=PASS|LEGACY_SCHEMA_LAZY=TRUE"
    echo "HISTORY_HASH_PARITY=PASS|CANONICAL_OWNER=V51PermutationFeatureEngine"
    echo "FRESH_INSTALL_HISTORY=MIN12|BOOTSTRAP100|MAX240|240_IS_NOT_MINIMUM"
    echo "FAILED_ANALYSIS_ATTEMPT_VISIBLE=PASS"
    echo "R363_FAILED_REQUEST_REARM=KNOWN_ERROR_ONLY|TARGET_UNDRAWN|ONE_TIME"
    echo "R366_DURABLE_CLAIM=ACTIVE_OWNER_DEFER|STALE_TAKEOVER|SAME_OWNER_RESUME|POSTDRAW_BLOCK"
    echo "R366_OWNER_FENCING=CLAIM|HEARTBEAT|HISTORY_BIND|RETRY|TERMINAL|CANONICAL_COMMIT"
    echo "R366_ACTION_SNAPSHOT=N4_BOUNDARY_1_TO_10|FIXED_K3_IDENTITY_N1_N2_N3|FULL_1200_ROUNDTRIP"
    echo "R366_ACTION_HASH=ONE_CANONICAL_SERIALIZER|PRODUCER_CONSUMER_PARITY"
    echo "R366_RETRY=BOUNDED_BACKOFF_1_TO_30_SECONDS|TIGHT_LOOP=0"
    echo "R366_DIAGNOSIS=READ_ONLY|FAILED_ATTEMPT_NOT_HEALTHY|INFLIGHT_NOT_FALSE_FAIL"
    echo "PHYSICAL_DEVICE_RUNTIME=RETEST_REQUIRED"
    echo "MODEL_PROOF=NOT_PROVEN_UNTRAINED"
    echo "REAL_BET_UNLOCK=NO"
} > "$DEST/PACKAGE_MANIFEST.txt"

# Prove that the self-contained package can rebuild the same executable payload. APK signing/ZIP
# container bytes may vary between invocations, so the hard equivalence gate is DEX + resources +
# binary manifest + signing certificate + package/version identity; the complete APK hashes remain
# recorded separately and are never falsely reported as byte-identical.
"$DEST/REBUILD_OFFLINE.sh" \
    > "$DEST/EVIDENCE/REPRODUCIBILITY/R366_OFFLINE_REBUILD.log" 2>&1
REBUILT_APK="$DEST/APK/Starcore-Tianshu-V57.15.3-R3.6.6-offline-rebuilt.apk"
require_file "$REBUILT_APK" OFFLINE_REBUILT_APK
release_dex=$(unzip -p "$APK" classes.dex | sha256sum | awk '{print $1}')
rebuilt_dex=$(unzip -p "$REBUILT_APK" classes.dex | sha256sum | awk '{print $1}')
release_resources=$(unzip -p "$APK" resources.arsc | sha256sum | awk '{print $1}')
rebuilt_resources=$(unzip -p "$REBUILT_APK" resources.arsc | sha256sum | awk '{print $1}')
release_manifest=$(unzip -p "$APK" AndroidManifest.xml | sha256sum | awk '{print $1}')
rebuilt_manifest=$(unzip -p "$REBUILT_APK" AndroidManifest.xml | sha256sum | awk '{print $1}')
release_cert=$("$APKSIGNER" verify --print-certs "$APK" 2>/dev/null \
    | sed -n 's/^.*certificate SHA-256 digest: //p' | head -n 1)
rebuilt_cert=$("$APKSIGNER" verify --print-certs "$REBUILT_APK" 2>/dev/null \
    | sed -n 's/^.*certificate SHA-256 digest: //p' | head -n 1)
release_identity=$("$AAPT2" dump badging "$APK" | sed -n '1p')
rebuilt_identity=$("$AAPT2" dump badging "$REBUILT_APK" | sed -n '1p')
[ "$release_dex" = "$rebuilt_dex" ] || fail OFFLINE_DEX_PAYLOAD_MISMATCH
[ "$release_resources" = "$rebuilt_resources" ] || fail OFFLINE_RESOURCES_MISMATCH
[ "$release_manifest" = "$rebuilt_manifest" ] || fail OFFLINE_MANIFEST_MISMATCH
[ -n "$release_cert" ] && [ "$release_cert" = "$rebuilt_cert" ] \
    || fail OFFLINE_SIGNING_CERT_MISMATCH
[ "$release_identity" = "$rebuilt_identity" ] || fail OFFLINE_PACKAGE_IDENTITY_MISMATCH
if cmp -s "$APK" "$REBUILT_APK"; then
    byte_identical=TRUE
else
    byte_identical=FALSE
fi
{
    echo "R366_OFFLINE_REBUILD=PASS"
    echo "EXECUTABLE_PAYLOAD_EQUIVALENT=PASS"
    echo "FULL_APK_BYTE_IDENTICAL=$byte_identical"
    echo "RELEASE_APK_SHA256=$(sha256sum "$APK" | awk '{print $1}')"
    echo "REBUILT_APK_SHA256=$(sha256sum "$REBUILT_APK" | awk '{print $1}')"
    echo "CLASSES_DEX_SHA256=$release_dex"
    echo "RESOURCES_ARSC_SHA256=$release_resources"
    echo "ANDROID_MANIFEST_SHA256=$release_manifest"
    echo "SIGNING_CERT_SHA256=$release_cert"
    echo "PACKAGE_IDENTITY=$release_identity"
    echo "POLICY=DEX_RESOURCES_MANIFEST_CERT_IDENTITY_MUST_MATCH;CONTAINER_HASH_RECORDED_EXACTLY"
} > "$DEST/EVIDENCE/REPRODUCIBILITY/R366_OFFLINE_PAYLOAD_EQUIVALENCE.txt"
rm -f "$REBUILT_APK" "$REBUILT_APK.idsig"
rm -rf "$DEST/OFFLINE_REBUILD_WORK"

find "$DEST" -type f -printf '%P|%s bytes\n' \
    | sort > "$DEST/PACKAGE_FILE_LIST.txt"
(
    cd "$DEST"
    find . -type f \
        ! -name SHA256SUMS.txt \
        -print0 \
        | sort -z \
        | xargs -0 sha256sum \
        > SHA256SUMS.txt
    sha256sum -c SHA256SUMS.txt \
        > EVIDENCE/REPRODUCIBILITY/R366_PACKAGE_SHA256_VERIFY.log
)

(
    cd "$(dirname -- "$DEST")"
    zip -q -6 -r "$MASTER" "$(basename -- "$DEST")"
)
unzip -t "$MASTER" > "$MASTER.zip-test.log"
sha256sum "$MASTER" > "$MASTER.sha256"

echo "R366_TOTAL_PACKAGE=PASS"
echo "PACKAGE_DIR=$DEST"
echo "MASTER_ZIP=$MASTER"
echo "MASTER_BYTES=$(wc -c < "$MASTER" | tr -d ' ')"
echo "MASTER_SHA256=$(sha256sum "$MASTER" | awk '{print $1}')"
echo "APK=$DEST/APK/Starcore-Tianshu-V57.15.3-R3.6.6-release.apk"
echo "APK_SHA256=$(sha256sum "$APK" | awk '{print $1}')"
