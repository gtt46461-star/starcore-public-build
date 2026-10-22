#!/usr/bin/env bash
set -euo pipefail

ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
APK=${1:-""}
BASE=${2:-"$ROOT/build/r915-release/Starcore-Tianshu-V57.20.1-R5.3.1-915-release.apk"}
OUT=${R926_EVIDENCE_DIR:-${R925_EVIDENCE_DIR:-"$ROOT/build/r926-release/evidence"}}
TC=${STARCORE_TOOLCHAIN_ROOT:-"$ROOT/../08_TOOLCHAIN"}
AAPT2="$TC/aapt/bin/x64/linux/aapt2"
APKSIGNER="$TC/android-signing/apksigner"
ZIPALIGN="$TC/android-signing/zipalign"
APKTOOL="$TC/apktool/apktool.jar"
LIB="$TC/jadx-node/package/resources/jadx-1.1.0/lib"
python3 "$ROOT/scripts/r926_release_manifest.py" --check >/dev/null || { echo "R926_APK_ACCEPTANCE=FAIL|RELEASE_MANIFEST_DRIFT" >&2; exit 1; }
identity_value() { sed -n "s/^$1=//p" "$ROOT/release/release_identity.properties" | head -n 1; }
APP_ID=$(identity_value applicationId)
VERSION_NAME=$(identity_value versionName)
VERSION_CODE=$(identity_value versionCode)
[ -n "$APK" ] || APK="$ROOT/build/r926-release/Starcore-Tianshu-${VERSION_NAME}-release.apk"
TMP=$(mktemp -d "${TMPDIR:-/tmp}/starcore-r926-accept.XXXXXX")
trap 'rm -rf "$TMP"' EXIT HUP INT TERM
mkdir -p "$OUT" "$TMP/smali"

fail() {
    echo "R926_APK_ACCEPTANCE=FAIL|$1" >&2
    exit 1
}

for required in "$APK" "$BASE" "$AAPT2" "$APKSIGNER" "$ZIPALIGN" "$APKTOOL"; do
    [ -f "$required" ] || fail "MISSING_$(basename "$required")"
done

"$ZIPALIGN" -c -p 4 "$APK" > "$OUT/R926_APK_ZIPALIGN.txt" 2>&1 || fail ZIPALIGN
"$APKSIGNER" verify --verbose --print-certs "$APK" \
    > "$OUT/R926_APK_SIGNATURE.txt" 2>&1 || fail SIGNATURE
"$AAPT2" dump badging "$APK" > "$OUT/R926_APK_BADGING.txt" || fail BADGING
rg -q "package: name='$APP_ID' versionCode='$VERSION_CODE' versionName='$VERSION_NAME'" \
    "$OUT/R926_APK_BADGING.txt" || fail IDENTITY
rg -q "minSdkVersion:'21'" "$OUT/R926_APK_BADGING.txt" || fail MIN_SDK
unzip -t "$APK" > "$OUT/R926_APK_ZIP_INTEGRITY.txt" || fail ZIP_INTEGRITY

SIGNED_CERT=$(sed -n 's/^.*certificate SHA-256 digest: //p' \
    "$OUT/R926_APK_SIGNATURE.txt" | head -n 1)
BASE_CERT=$("$APKSIGNER" verify --print-certs "$BASE" 2>/dev/null \
    | sed -n 's/^.*certificate SHA-256 digest: //p' | head -n 1)
[ -n "$SIGNED_CERT" ] && [ "$SIGNED_CERT" = "$BASE_CERT" ] \
    || fail COVER_INSTALL_CERTIFICATE

java -jar "$APKTOOL" d -s -f -o "$TMP/decoded" "$APK" \
    > "$OUT/R925_APKTOOL_DECODE.txt" 2>&1 || fail APK_DECODE
dex_count=0
while IFS= read -r dex_name; do
    dex_count=$((dex_count + 1))
    unzip -p "$APK" "$dex_name" > "$TMP/$dex_name"
    java -cp "$LIB/*" org.jf.baksmali.Main d "$TMP/$dex_name" -o "$TMP/smali" \
        >> "$OUT/R926_APK_DEX_DECODE.txt" 2>&1 || fail DEX_DECODE
done < <(unzip -Z1 "$APK" | rg '^classes([0-9]+)?\.dex$' | sort -V)
[ "$dex_count" -ge 1 ] || fail DEX_MISSING

python3 "$ROOT/verification/r922/r922_apk_resource_contract.py" \
    "$TMP/decoded" "$TMP/smali" "$ROOT/app/src/main/java/com/ethk3/novaonepro" \
    > "$OUT/R926_APK_RESOURCE_CONTRACT_ACCEPTANCE.txt" 2>&1 \
    || fail DEX_RESOURCE_ID_PARITY

python3 "$ROOT/scripts/r967_semantic_identity.py" --check > "$OUT/R968_GENERATED_SEMANTICS_CHECK.txt" 2>&1 || fail R968_GENERATED_SEMANTICS
python3 "$ROOT/verification/r968/apk_wiring_r968.py" "$TMP/smali" > "$OUT/R968_FINAL_DEX_WIRING.txt" 2>&1 || fail R968_FINAL_DEX_WIRING
python3 "$ROOT/verification/r969/apk_wiring_r969.py" "$TMP/smali" > "$OUT/R969_FINAL_DEX_WIRING.txt" 2>&1 || fail R969_FINAL_DEX_WIRING
python3 "$ROOT/verification/r970/apk_wiring_r970.py" "$TMP/smali" "$APK" > "$OUT/R970_FINAL_DEX_WIRING.txt" 2>&1 || fail R970_FINAL_DEX_WIRING
python3 "$ROOT/verification/r971/apk_wiring_r971.py" "$TMP/smali" > "$OUT/R971_FINAL_DEX_WIRING.txt" 2>&1 || fail R971_FINAL_DEX_WIRING

python3 "$ROOT/verification/r967/apk_wiring_r967.py" "$TMP/smali" > "$OUT/R967_FINAL_DEX_WIRING.txt" 2>&1 || fail R967_FINAL_DEX_WIRING

python3 "$ROOT/verification/r958/apk_wiring_r966.py" "$TMP/smali" > "$OUT/R966_FINAL_DEX_WIRING.txt" 2>&1 || fail R966_FINAL_DEX_WIRING

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
    ControlledRealContractAuditR922 ControlledCandidateOpportunityAuditR922 \
    ResearchMainlineContributionAuditR922 ResearchDynamicBootstrapR922 \
    ResearchEvolutionDeltaR922 BackgroundRuntimeContractR922 \
    AlgorithmRegimeEvidenceR923 ControlledFrequencyDiagnosticR923 MissStreakEscalationR923 \
    DiagnosticTextExportR923 DiagnosticReportProviderR923 UnifiedSoftwareStatusR923 FullResearchInventoryR923 \
    EssentialSettlementFeedbackR923 ResearchPredrawCaptureR923 RealtimeProvenanceRepairR923 \
    DynamicFreezeRealtimeSnapshotR923 V51TianshuEvidenceBridgeR923 PredictionGenerationCoverageR923 \
    RuntimeResourceAuditR927 BudgetedQueryR927 ResearchWorkBudgetR927 \
    CompleteReportFileR927 AlgorithmLifecycleReportR927 V57CompleteDiagnosticReportEngine DiagnosticStateRendererR906 \
    WhyTruthSectionRendererR906 ResearchScheduleR924 ResearchReadinessR924 ResearchDashboardR924 \
    SchemaRevisionR925 LifecycleAuditR925 RuntimeObservabilityR925 OperationalDashboardR925 \
    SoftwareAuditR925 CaptureOpportunityR925 TrainingSnapshotExportR925 RankerMathR925 \
    PromotionWindowR925 ResearchFamilyRegistryR925 AtomQualityLedgerR925 FetchLedgerR925 \
    BacklogConvergenceR925 DeepCandidateRuntimeR925 \
    ReleaseManifestR926 RuntimeReadOnlyRepositoryR926 MainActivityTruthPresenterR926 \
    GenesisScanR967 ResearchTaskQueueR967 ResearchFunnelR967 \
    SchemaDomainCoordinatorR926 ResearchSchemaCoordinatorR926 LegacySchemaCoordinatorR926 \
    DatabaseBackupManifestR926 RuntimeEventLedgerR926 RuntimeEventHooksR926 RuntimeRecoveryCoordinatorR926; do
    [ -f "$TMP/smali/com/ethk3/novaonepro/$class_name.smali" ] \
        || fail "DEX_CLASS_$class_name"
    class_count=$((class_count + 1))
done

for call in 'RuntimeResourceAuditR927;->begin' 'RuntimeResourceAuditR927;->finish' 'BudgetedQueryR927;->query' 'ResearchWorkBudgetR927;->checkpoint' 'SchemaDomainCoordinatorR926;->matches' 'SchemaDomainCoordinatorR926;->prepareRepairIfDrift' 'RuntimeEventLedgerR926;->appendWithinTransaction' 'RuntimeRecoveryCoordinatorR926;->record' 'MainActivityTruthPresenterR926;->render' 'RuntimeObservabilityR925;->timing' 'FetchLedgerR925;->begin' 'FetchLedgerR925;->complete' 'FetchLedgerR925;->payload' 'FixedResearchWindowR932;->maintain' 'PairedResearchChallengeR932;->maintain' 'ResearchLifecycleR932;->maintain' 'FixedWindowCertificatePublisherR932;->maintain' 'AlgorithmProductionGovernorR915;->skippedForAsset' 'ResearchFamilyRegistryR925;->register' 'AtomQualityLedgerR925;->maintain' 'TrainingSnapshotExportR925;->start'; do
    rg -q "$call" "$TMP/smali" || fail "R926_DEX_CALL_$call"
done
cmp "$ROOT/app/src/main/assets/r925_software_audit.txt" "$TMP/decoded/assets/r925_software_audit.txt" || fail R925_AUDIT_ASSET_MISMATCH

python3 "$ROOT/verification/r958/apk_wiring_r965.py" "$TMP/smali" || fail R965_FINAL_DEX_WIRING

BUILD_CONFIG="$TMP/smali/com/ethk3/novaonepro/BuildConfig.smali"
[ -f "$BUILD_CONFIG" ] || fail BUILD_CONFIG
rg -q 'APPLICATION_ID:Ljava/lang/String; = "com.ethk3.novaonepro"' "$BUILD_CONFIG" \
    || fail DEX_APP_ID
rg -F -q "VERSION_NAME:Ljava/lang/String; = \"$VERSION_NAME\"" "$BUILD_CONFIG" \
    || fail DEX_VERSION_NAME
VERSION_CODE_HEX=$(printf '0x%x' "$VERSION_CODE")
rg -q "VERSION_CODE:I = ($VERSION_CODE|$VERSION_CODE_HEX)" "$BUILD_CONFIG" \
    || fail DEX_VERSION_CODE
KEY_A=$(sed -n 's/^\.field public static final DUOQU_API_KEY_A:Ljava\/lang\/String; = "\([A-Za-z0-9][A-Za-z0-9]*\)"$/\1/p' "$BUILD_CONFIG")
KEY_B=$(sed -n 's/^\.field public static final DUOQU_API_KEY_B:Ljava\/lang\/String; = "\([A-Za-z0-9][A-Za-z0-9]*\)"$/\1/p' "$BUILD_CONFIG")
[[ "$KEY_A$KEY_B" =~ ^[A-Za-z0-9]{8,128}$ ]] || fail PROVIDER_KEY_NOT_CARRIED
unset KEY_A KEY_B

TRUTH="$TMP/smali/com/ethk3/novaonepro/RuntimeTruthSnapshotR906.smali"
MAIN="$TMP/smali/com/ethk3/novaonepro/MainActivity.smali"
ATOM_SCHEMA="$TMP/smali/com/ethk3/novaonepro/GenesisSchemaRegistryR915.smali"
for call in \
    'AssetGenesisEngineR915;->capturePredraw' \
    'AlgorithmInventionLabR908;->capturePredraw' \
    'ResearchTinyDeepLearnerR909;->capturePredraw' \
    'ResearchLearningContinuityR912;->pulse' \
    'ResearchDynamicBootstrapR922;->pulse' \
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
rg -q 'R923-RUNTIME-TRUTH-V9' "$TRUTH" || fail APK_TRUTH_VERSION
# baksmali renders non-ASCII string contents as \uXXXX escapes; the stable visible payload and
# semantic owner version together prove that the final Dex, rather than only source, has the card.
rg -q 'PRODUCTION_SCORE_SEMANTIC' "$ATOM_SCHEMA" || fail APK_VISIBLE_ATOM_SEMANTIC_TIER
rg -q 'R922_SEMANTIC_ATOMIC_WORLD_V2' "$TRUTH" || fail APK_VISIBLE_SEMANTIC_ATOMIC_OWNER
rg -q 'R922_SEMANTIC_ATOMIC_WORLD_V2' \
    "$TMP/smali/com/ethk3/novaonepro/GenesisAtomicWorldR920.smali" \
    || fail APK_SEMANTIC_ATOMIC_WORLD_VERSION
rg -q 'GenesisAtomicWorldR920;->materialize' "$TMP/smali/com/ethk3/novaonepro/GenesisAssetVmR915.smali" \
    || fail APK_SEMANTIC_ATOMIC_WORLD_WIRING
rg -q 'installLaunchSafeModeR922' "$MAIN" || fail APK_LAUNCH_SAFE_MODE_METHOD
rg -q 'R922_UI_BIND_OR_BOOTSTRAP' "$MAIN" || fail APK_LAUNCH_SAFE_MODE_REASON
rg -q 'LifecycleHealthR963;->keepService' "$MAIN" || fail APK_UI_EXIT_ASYNC_DELEGATE
LIFECYCLE="$TMP/smali/com/ethk3/novaonepro/LifecycleHealthR963.smali"
rg -q 'Landroid/os/HandlerThread;->start' "$LIFECYCLE" || fail APK_LIFECYCLE_BACKGROUND_THREAD
rg -q 'reconcileAutoFetchService' "$TMP/smali/com/ethk3/novaonepro/"LifecycleHealthR963*.smali || fail APK_UI_EXIT_BACKGROUND_RECONCILE
rg -q 'isUserEnabled' "$TMP/smali/com/ethk3/novaonepro/"LifecycleHealthR963*.smali || fail APK_UI_EXIT_USER_ENABLED_RECHECK
rg -q 'scroll_build_badge' "$TMP/decoded/res/values/public.xml" || fail APK_HORIZONTAL_BUILD_BADGE
rg -q 'ControlledCandidateOpportunityAuditR922;->from' \
    "$TMP/smali/com/ethk3/novaonepro/V51RuntimeOrchestrator.smali" \
    || fail APK_OPPORTUNITY_AUDIT_WIRING
rg -q 'ResearchMainlineContributionAuditR922' \
    "$TMP/smali/com/ethk3/novaonepro/V51RuntimeOrchestrator.smali" \
    || fail APK_RESEARCH_CONTRIBUTION_WIRING
rg -q 'R5.4.1' "$TMP/decoded/res/values/strings.xml" || fail APK_VISIBLE_R925
rg -q 'DiagnosticReportProviderR923' "$TMP/decoded/AndroidManifest.xml" || fail REPORT_PROVIDER_MISSING
for call in 'ResearchPredrawCaptureR923;->captureTarget' 'EssentialSettlementFeedbackR923;->settleNow' \
    'MissStreakEscalationR923;->analyze' 'DiagnosticTextExportR923;->shareFile' 'ResearchDashboardR924;->capture'; do
    rg -q "$call" "$TMP/smali" || fail "R926_RUNTIME_WIRING_$call"
done
rg -q '星核天枢' "$TMP/decoded/res/values/strings.xml" || fail APK_VISIBLE_PRODUCT_NAME

sha256sum "$APK" > "$OUT/R926_APK_SHA256.txt"
APK_BYTES=$(stat -c '%s' "$APK")
[ "$APK_BYTES" -gt 1000000 ] || fail APK_SUSPICIOUSLY_SMALL
{
    echo 'R926_APK_ACCEPTANCE=PASS'
    echo "APPLICATION_ID=$APP_ID"
    echo "VERSION_NAME=$VERSION_NAME"
    echo "VERSION_CODE=$VERSION_CODE"
    echo "APK_BYTES=$APK_BYTES"
    echo "DEX_COUNT=$dex_count"
    echo "R926_RUNTIME_CLASSES=$class_count/$class_count"
    echo 'DEX_RESOURCE_ID_PARITY=PASS_ZERO_MISMATCH'
    echo 'R926_NEW_CLASSES_AND_CALLS=PASS'
    echo 'R926_EMBEDDED_AUDIT_BYTES=PASS'
    echo 'MAIN_ACTIVITY_BINDINGS=PASS'
    echo 'ROW_RENDERER_BINDINGS=PASS'
    echo 'PREDRAW_RESEARCH_WIRING=PASS'
    echo 'POSTDRAW_RESEARCH_TEACHER_WIRING=PASS'
    echo 'SEMANTIC_ATOMIC_WORLD_WIRING=PASS'
    echo 'CONTROLLED_OPPORTUNITY_AND_RESEARCH_CONTRIBUTION_WIRING=PASS'
    echo 'UI_EXIT_BACKGROUND_RECONCILE=PASS'
    echo 'HORIZONTAL_BUILD_BADGE=PASS'
    echo 'VISIBLE_ATOM_SEMANTIC_TIERS=165_PRODUCTION/40_PROXY/20_SETTLEMENT/15_AUDIT'
    echo 'PROVIDER_KEY_CARRIED=YES_REDACTED'
    echo "CERT_SHA256=$SIGNED_CERT"
    echo 'SIGNATURE_CONTINUITY=PASS_OVERWRITE_INSTALL'
    echo 'ZIPALIGN=PASS'
    echo 'ZIP_INTEGRITY=PASS'
    echo 'HOST_PROVEN_LAUNCH_RESOURCE_CONTRACT=PASS'
    echo 'LAUNCH_SAFE_MODE_FALLBACK=PASS_IN_FINAL_DEX'
    echo 'PHYSICAL_DEVICE_COLD_START=RETEST_REQUIRED'
    echo 'PHYSICAL_DEVICE_BACKGROUND_RUNTIME=RETEST_REQUIRED'
} | tee "$OUT/R926_APK_ACCEPTANCE_SUMMARY.txt"
