#!/usr/bin/env bash
set -euo pipefail

ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
BASE=${1:?Pass the frozen R979 APK explicitly; no old-source fallback is allowed}
OUT_DIR=${2:-"$ROOT/build/r980-release"}
TC=${STARCORE_TOOLCHAIN_ROOT:-"$ROOT/../TOOLCHAIN_USED"}
ANDROID="$TC/android-platform-36/android.jar"
R8="$TC/r8-9.2.23/r8-9.2.23.jar"
APKTOOL="$TC/apktool/apktool.jar"
LIB="$TC/jadx-node/package/resources/jadx-1.1.0/lib"
ZIPALIGN="$TC/android-signing/zipalign"
APKSIGNER="$TC/android-signing/apksigner"
AAPT2="$TC/aapt/bin/x64/linux/aapt2"
TMP=$(mktemp -d "${TMPDIR:-/tmp}/starcore-r980-build.XXXXXX")
trap 'rm -rf "$TMP"' EXIT HUP INT TERM

fail() {
    mkdir -p "$ROOT/build/r980-release-failure"
    cp -f "$TMP/logs/"* "$ROOT/build/r980-release-failure/" 2>/dev/null || true
    echo "R980_RELEASE_BUILD=FAIL|$1" >&2
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
[ "$VERSION_NAME" = "V57.21.2.0.23-R5.4.2.23-980" ] || fail RELEASE_PROPERTY_VERSION_NAME
[ "$VERSION_CODE" = "577250980" ] || fail RELEASE_PROPERTY_VERSION_CODE
for required in "$BASE" "$ANDROID" "$R8" "$APKTOOL" "$ZIPALIGN" "$APKSIGNER" "$AAPT2"; do
    [ -f "$required" ] || fail "MISSING:$(basename "$required")"
done

mkdir -p "$OUT_DIR/evidence" "$TMP/classes" "$TMP/gen/com/ethk3/novaonepro" \
    "$TMP/d8" "$TMP/framework-stage1" "$TMP/framework-stage2" "$TMP/logs" \
    "$TMP/product-smali" "$TMP/final-smali"
cd "$ROOT"
python3 scripts/r926_release_manifest.py --check > "$TMP/logs/release-manifest.log" 2>&1 || fail RELEASE_IDENTITY
python3 scripts/r967_semantic_identity.py --check > "$TMP/logs/semantic-identity.log" 2>&1 || fail SEMANTIC_IDENTITY

# Decode the signed predecessor once. It is used only for certificate/key continuity and as the
# apktool resource container; no predecessor Dex survives into the R922 product.
java -jar "$APKTOOL" d -s -p "$TMP/framework-stage1" -f -o "$TMP/stage1" "$BASE" \
    > "$TMP/logs/apktool-stage1-decode.log" 2>&1 || fail BASE_APK_DECODE
cp -R "$TMP/stage1" "$TMP/predecessor-decoded"
mkdir -p "$TMP/predecessor-source"
if [ -n "${STARCORE_BASE_JAVA_ROOT:-}" ]; then
    cp -R "$STARCORE_BASE_JAVA_ROOT" "$TMP/predecessor-source/java"
    BASE_JAVA="$TMP/predecessor-source/java"
else
    git archive "${STARCORE_BASE_COMMIT:?Provide the frozen base commit or STARCORE_BASE_JAVA_ROOT}" app/src/main/java \
        | tar -x -C "$TMP/predecessor-source" || fail BASELINE_SOURCE_ARCHIVE
    BASE_JAVA="$TMP/predecessor-source/app/src/main/java/com/ethk3/novaonepro"
fi
unzip -p "$BASE" classes.dex > "$TMP/base.dex" || fail BASE_DEX
java -cp "$LIB/*" org.jf.baksmali.Main d "$TMP/base.dex" -o "$TMP/base-smali" \
    > "$TMP/logs/base-baksmali.log" 2>&1 || fail BASE_DEX_DECODE
BASE_BUILD_CONFIG="$TMP/base-smali/com/ethk3/novaonepro/BuildConfig.smali"
[ -f "$BASE_BUILD_CONFIG" ] || fail BASE_BUILD_CONFIG
KEY_A=$(sed -n 's/^\.field public static final DUOQU_API_KEY_A:Ljava\/lang\/String; = "\([A-Za-z0-9][A-Za-z0-9]*\)"$/\1/p' "$BASE_BUILD_CONFIG")
KEY_B=$(sed -n 's/^\.field public static final DUOQU_API_KEY_B:Ljava\/lang\/String; = "\([A-Za-z0-9][A-Za-z0-9]*\)"$/\1/p' "$BASE_BUILD_CONFIG")
PROVIDER_KEY="$KEY_A$KEY_B"
[[ "$PROVIDER_KEY" =~ ^[A-Za-z0-9]{8,128}$ ]] || fail API_KEY_FORMAT

# Stage one compiles the canonical manifest/resources first. This is the decisive R922 ordering:
# product Java is forbidden from compiling until apktool has allocated every real resource ID.
cp app/src/main/AndroidManifest.xml "$TMP/stage1/AndroidManifest.xml"
cp -R app/src/main/res/. "$TMP/stage1/res/"
# Assets come from this exact source tree, not silently from the predecessor APK.
rm -rf "$TMP/stage1/assets"
mkdir -p "$TMP/stage1/assets"
cp -R app/src/main/assets/. "$TMP/stage1/assets/"
sed -i -e "s/^  versionCode: .*/  versionCode: $VERSION_CODE/" \
    -e "s/^  versionName: .*/  versionName: $VERSION_NAME/" "$TMP/stage1/apktool.yml"
java -jar "$APKTOOL" b -p "$TMP/framework-stage1" -o "$TMP/resource-skeleton.apk" \
    "$TMP/stage1" > "$TMP/logs/apktool-resource-skeleton-build.log" 2>&1 \
    || fail RESOURCE_SKELETON_BUILD

# Decode the compiled skeleton so public.xml is derived from resources.arsc, not a guessed stub.
java -jar "$APKTOOL" d -s -p "$TMP/framework-stage2" -f -o "$TMP/stage2" \
    "$TMP/resource-skeleton.apk" > "$TMP/logs/apktool-stage2-decode.log" 2>&1 \
    || fail RESOURCE_SKELETON_DECODE
python3 scripts/r922_generate_r_java.py \
    "$TMP/stage2/res/values/public.xml" \
    "$TMP/gen/com/ethk3/novaonepro/R.java" \
    --package "$APP_ID" \
    --inventory "$OUT_DIR/evidence/R980_COMPILED_RESOURCE_INVENTORY.tsv" \
    > "$TMP/logs/r922-r-generator.log" 2>&1 || fail RESOURCE_R_GENERATION

# Cover-install provider credentials remain private and are injected only into the temporary tree.
sed -e "s/DUOQU_API_KEY_A = \"\"/DUOQU_API_KEY_A = \"$KEY_A\"/" \
    -e "s/DUOQU_API_KEY_B = \"\"/DUOQU_API_KEY_B = \"$KEY_B\"/" \
    -e "s/VERSION_CODE = [0-9][0-9]*/VERSION_CODE = $VERSION_CODE/" \
    -e "s/VERSION_NAME = \"[^\"]*\"/VERSION_NAME = \"$VERSION_NAME\"/" \
    verification/r36-stubs/com/ethk3/novaonepro/BuildConfig.java \
    > "$TMP/gen/com/ethk3/novaonepro/BuildConfig.java"
unset KEY_A KEY_B PROVIDER_KEY

find app/src/main/java -name '*.java' -print | sort > "$TMP/sources.list"
find app/build/generated/source/r967 -name '*.java' -print | sort >> "$TMP/sources.list"
java -m jdk.compiler/com.sun.tools.javac.Main -encoding UTF-8 -source 17 -target 17 \
    -cp "$ANDROID" -d "$TMP/classes" @"$TMP/sources.list" \
    "$TMP/gen/com/ethk3/novaonepro/BuildConfig.java" \
    "$TMP/gen/com/ethk3/novaonepro/R.java" \
    > "$TMP/logs/javac.log" 2>&1 || fail JAVAC
(cd "$TMP/classes" && zip -q -r "$TMP/classes.jar" .)
java -Xmx2g -cp "$R8" com.android.tools.r8.D8 --release --min-api 21 --lib "$ANDROID" \
    --output "$TMP/d8" "$TMP/classes.jar" > "$TMP/logs/d8.log" 2>&1 || fail D8

# Prove the product Dex identity before assembly.
dex_index=0
for dex in "$TMP/d8"/classes*.dex; do
    dex_index=$((dex_index + 1))
    java -cp "$LIB/*" org.jf.baksmali.Main d "$dex" -o "$TMP/product-smali" \
        >> "$TMP/logs/product-dex-decode.log" 2>&1 || fail PRODUCT_DEX_DECODE
done
[ "$dex_index" -ge 1 ] || fail PRODUCT_DEX_MISSING
PRODUCT_BUILD_CONFIG="$TMP/product-smali/com/ethk3/novaonepro/BuildConfig.smali"
[ -f "$PRODUCT_BUILD_CONFIG" ] || fail PRODUCT_DEX_BUILD_CONFIG_MISSING
rg -q "APPLICATION_ID:Ljava/lang/String; = \"$APP_ID\"" "$PRODUCT_BUILD_CONFIG" \
    || fail PRODUCT_DEX_APP_ID
rg -q "VERSION_NAME:Ljava/lang/String; = \"$VERSION_NAME\"" "$PRODUCT_BUILD_CONFIG" \
    || fail PRODUCT_DEX_VERSION_NAME
rg -q "VERSION_CODE:I = ($VERSION_CODE|$VERSION_CODE_HEX)" "$PRODUCT_BUILD_CONFIG" \
    || fail PRODUCT_DEX_VERSION_CODE

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
    V57CompleteDiagnosticReportEngine DiagnosticStateRendererR906 \
    WhyTruthSectionRendererR906 ResearchScheduleR924 ResearchReadinessR924 ResearchDashboardR924 TrainingContinuationR973 ResearchStageProgressR973 CertifiedPredictionR974 RuntimeChainDiagnosisR975 r952/EvolutionEvidenceR975 TransactionSchemaCacheR941 ResearchTaskQueueR967; do
    [ -f "$TMP/product-smali/com/ethk3/novaonepro/$class_name.smali" ] \
        || fail "PRODUCT_DEX_CLASS_MISSING_$class_name"
    class_count=$((class_count + 1))
done
PRODUCT_RELEASE_IDENTITY="$TMP/product-smali/com/ethk3/novaonepro/ReleaseIdentityR817.smali"
[ -f "$PRODUCT_RELEASE_IDENTITY" ] || fail PRODUCT_RELEASE_IDENTITY_MISSING
rg -Fq "$VERSION_NAME" "$PRODUCT_RELEASE_IDENTITY" \
    || fail PRODUCT_RELEASE_IDENTITY_NAME

# Replace every predecessor Dex in the stage-two container and rebuild with its compiled public IDs.
rm -f "$TMP/stage2"/classes*.dex
cp "$TMP/d8"/classes*.dex "$TMP/stage2/"
java -jar "$APKTOOL" b -p "$TMP/framework-stage2" -o "$TMP/unsigned-raw.apk" \
    "$TMP/stage2" > "$TMP/logs/apktool-product-build.log" 2>&1 || fail APKTOOL_PRODUCT_BUILD

UNSIGNED="$OUT_DIR/Starcore-Tianshu-V57.21.2.0.23-R5.4.2.23-980-unsigned-aligned.apk"
"$ZIPALIGN" -f -p 4 "$TMP/unsigned-raw.apk" "$UNSIGNED" \
    > "$TMP/logs/zipalign.log" 2>&1 || fail ZIPALIGN
"$ZIPALIGN" -c -p 4 "$UNSIGNED" > "$TMP/logs/zipalign-verify.log" 2>&1 \
    || fail ZIPALIGN_VERIFY
"$AAPT2" dump badging "$UNSIGNED" > "$TMP/logs/aapt2-badging.log" \
    || fail BADGING
rg -q "package: name='$APP_ID' versionCode='$VERSION_CODE' versionName='$VERSION_NAME'" \
    "$TMP/logs/aapt2-badging.log" || fail MANIFEST_IDENTITY
unzip -t "$UNSIGNED" > "$TMP/logs/unzip-test.log" || fail ZIP_VERIFY

# Decode the exact assembled APK and compare every public ID in resources.arsc with every R field
# in Dex. The R919/R915 predecessor must fail this gate; the R922 product must pass with zero drift.
java -jar "$APKTOOL" d -s -p "$TMP/framework-final" -f -o "$TMP/final-decoded" \
    "$UNSIGNED" > "$TMP/logs/final-apktool-decode.log" 2>&1 || fail FINAL_APK_DECODE
while IFS= read -r dex_name; do
    unzip -p "$UNSIGNED" "$dex_name" > "$TMP/final-$dex_name"
    java -cp "$LIB/*" org.jf.baksmali.Main d "$TMP/final-$dex_name" \
        -o "$TMP/final-smali" >> "$TMP/logs/final-baksmali.log" 2>&1 \
        || fail FINAL_DEX_DECODE
done < <(unzip -Z1 "$UNSIGNED" | rg '^classes([0-9]+)?\.dex$' | sort -V)
python3 verification/r922/r922_apk_resource_contract.py \
    "$TMP/final-decoded" "$TMP/final-smali" \
    app/src/main/java/com/ethk3/novaonepro \
    > "$OUT_DIR/evidence/R980_APK_RESOURCE_CONTRACT.txt" 2>&1 \
    || fail APK_RESOURCE_CONTRACT

# R922 is a source/runtime/report patch on top of the resource-correct R921 package. Its predecessor
# must therefore remain resource-compatible; expecting an older resource mismatch here would reject a
# healthy cover-install base for the wrong reason.
python3 verification/r922/r922_apk_resource_contract.py \
    "$TMP/predecessor-decoded" "$TMP/base-smali" "$BASE_JAVA" \
    > "$OUT_DIR/evidence/R980_PREDECESSOR_RESOURCE_COMPATIBILITY.txt" 2>&1 \
    || fail PREDECESSOR_RESOURCE_COMPATIBILITY
printf 'R980_PREDECESSOR_RESOURCE_COMPATIBILITY=PASS\n' \
    >> "$OUT_DIR/evidence/R980_PREDECESSOR_RESOURCE_COMPATIBILITY.txt"

sha256sum "$UNSIGNED" > "$OUT_DIR/evidence/R980_UNSIGNED_APK_SHA256.txt"
CERT_BASE=${STARCORE_CERTIFICATE_BASE_APK:-"$BASE"}
BASE_CERT=$("$APKSIGNER" verify --print-certs "$CERT_BASE" 2>/dev/null \
    | sed -n 's/^.*certificate SHA-256 digest: //p' | head -n 1)
[ -n "$BASE_CERT" ] || fail BASE_CERTIFICATE_UNREADABLE
printf 'BASE_CERT_SHA256=%s\nCERTIFICATE_REFERENCE_APK=%s\n' "$BASE_CERT" "$CERT_BASE" > "$OUT_DIR/evidence/R980_BASE_CERTIFICATE.txt"

if [ -z "${STARCORE_SIGNING_P12:-}" ] || [ -z "${STARCORE_SIGNING_PASSWORD:-}" ]; then
    cp -f "$TMP/logs/"* "$OUT_DIR/evidence/"
    {
        echo 'R980_RELEASE_BUILD=VERIFY_ONLY'
        echo "UNSIGNED_ALIGNED=$UNSIGNED"
        echo 'DEX_INTERNAL_IDENTITY=PASS'
        echo "R980_PRODUCT_CLASSES=$class_count"
        echo 'RESOURCE_ID_PARITY=PASS_ZERO_MISMATCH'
        echo 'MAIN_ACTIVITY_STATIC_RESOURCE_BINDINGS=PASS_RUNTIME_NOT_RUN'
        echo 'MANIFEST_IDENTITY=PASS'
        echo 'ZIPALIGN=PASS'
        echo 'ZIP_INTEGRITY=PASS'
        echo "BASE_CERT_SHA256=$BASE_CERT"
        echo 'SIGNING=BLOCKED_ORIGINAL_PRIVATE_KEY_NOT_PROVIDED'
        echo 'OVERWRITE_INSTALL=NOT_CLAIMED'
        echo 'PHYSICAL_DEVICE_RUNTIME=RETEST_REQUIRED'
    } | tee "$OUT_DIR/evidence/R980_RELEASE_BUILD_SUMMARY.txt"
    exit 0
fi

P12=$STARCORE_SIGNING_P12
PASS=$STARCORE_SIGNING_PASSWORD
ALIAS=${STARCORE_SIGNING_ALIAS:-starcore-r902}
TYPE=${STARCORE_SIGNING_TYPE:-PKCS12}
KEY_PASS=${STARCORE_SIGNING_KEY_PASSWORD:-$PASS}
[ -f "$P12" ] || fail SIGNING_KEY_MISSING
SIGNED="$OUT_DIR/Starcore-Tianshu-V57.21.2.0.23-R5.4.2.23-980-release.apk"
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
sha256sum "$SIGNED" > "$OUT_DIR/evidence/R980_SIGNED_APK_SHA256.txt"
{
    echo 'R980_RELEASE_BUILD=PASS'
    echo "APK=$SIGNED"
    echo "CERT_SHA256=$SIGNED_CERT"
    echo 'DEX_INTERNAL_IDENTITY=PASS'
    echo "R980_PRODUCT_CLASSES=$class_count"
    echo 'RESOURCE_ID_PARITY=PASS_ZERO_MISMATCH'
    echo 'MAIN_ACTIVITY_STATIC_RESOURCE_BINDINGS=PASS_RUNTIME_NOT_RUN'
    echo 'PREDECESSOR_RESOURCE_COMPATIBILITY=PASS'
    echo 'MANIFEST_IDENTITY=PASS'
    echo 'SIGNATURE_CONTINUITY=PASS_CERTIFICATE_MATCH_ONLY'
    echo 'PHYSICAL_OVERWRITE_INSTALL=NOT_RUN'
    echo 'PHYSICAL_DEVICE_RUNTIME=RETEST_REQUIRED'
} | tee "$OUT_DIR/evidence/R980_RELEASE_BUILD_SUMMARY.txt"
