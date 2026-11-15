package com.ethk3.novaonepro;

import android.content.Context;
import android.content.pm.PackageInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Exact Starcore Tianshu build/schema/game/bundle/authorization identity verifier. */
public final class BuildIdentity {
    public static final String MATERIALIZATION_MARKER =
            "STARCORE_TIANSHU_B01_B15_R368_811_FINAL_LOCKED_RUNTIME_RESEARCH_BOUNDARY_20260816";
    public static final String STATUS_OK = "BUILD_IDENTITY_OK_R915_CONTROLLED_CANDIDATE";
    public static final String STATE_STATUS = "build_identity_status";
    public static final String STATE_DETAIL = "build_identity_detail";
    public static final String STATE_PACKAGE = "build_identity_package";
    public static final String STATE_VERSION_NAME = "build_identity_version_name";
    public static final String STATE_EXPECTED_VERSION_NAME = "build_identity_expected_version_name";
    public static final String STATE_STRATEGY = "build_identity_strategy";
    public static final String STATE_VERIFIED_AT = "build_identity_verified_at";
    public static final String STATE_HARD_BLOCK = "build_identity_hard_block";
    public static final String STATE_CODE_FINGERPRINT = "build_identity_code_fingerprint";

    private BuildIdentity() {}

    public static final class CheckResult {
        public final String id;
        public final boolean passed;
        public final String detail;
        public final boolean hard;

        CheckResult(String id, boolean passed, String detail, boolean hard) {
            this.id = id;
            this.passed = passed;
            this.detail = safe(detail);
            this.hard = hard;
        }

        public String canonical() {
            return id + ':' + (passed ? '1' : '0') + ':' + (hard ? '1' : '0') + ':' + detail;
        }
    }

    public static final class VerificationReport {
        public final String status;
        public final List<CheckResult> checks;
        public final String fingerprint;

        VerificationReport(String status, List<CheckResult> checks, String fingerprint) {
            this.status = safe(status);
            this.checks = checks;
            this.fingerprint = safe(fingerprint);
        }

        public boolean hardBlocked() {
            for (int i = 0; i < checks.size(); i++)
                if (!checks.get(i).passed && checks.get(i).hard) return true;
            return !STATUS_OK.equals(status);
        }

        public String detail() {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < checks.size(); i++) {
                if (i > 0) sb.append('|');
                sb.append(checks.get(i).canonical());
            }
            return sb.toString();
        }
    }

    public static final class RequirementSpec {
        public final String id;
        public final String field;
        public final String group;
        public final boolean mandatory;

        RequirementSpec(String id, String field, String group, boolean mandatory) {
            this.id = id;
            this.field = field;
            this.group = group;
            this.mandatory = mandatory;
        }
    }

    public static List<RequirementSpec> requirements() {
        ArrayList<RequirementSpec> rules = new ArrayList<RequirementSpec>();
        rules.add(new RequirementSpec("TIANSHU-IDENTITY-001", "PACKAGE", "GROUP_01", true));
        rules.add(new RequirementSpec("TIANSHU-IDENTITY-002", "VERSION", "GROUP_01", true));
        rules.add(new RequirementSpec("TIANSHU-IDENTITY-003", "GAME_CODE", "GROUP_01", true));
        rules.add(new RequirementSpec("TIANSHU-IDENTITY-004", "DATABASE", "GROUP_01", true));
        rules.add(new RequirementSpec("TIANSHU-IDENTITY-005", "BUILD_ID", "GROUP_01", true));
        rules.add(new RequirementSpec("TIANSHU-IDENTITY-006", "COHORT", "GROUP_01", true));
        rules.add(new RequirementSpec("TIANSHU-IDENTITY-007", "CONTRACT_HASH", "GROUP_01", true));
        rules.add(new RequirementSpec("TIANSHU-IDENTITY-008", "BUNDLE_HASH", "GROUP_01", true));
        rules.add(new RequirementSpec("TIANSHU-IDENTITY-009", "TRANSPORT", "GROUP_01", true));
        rules.add(new RequirementSpec("TIANSHU-IDENTITY-010", "PARSER", "GROUP_01", true));
        rules.add(new RequirementSpec("TIANSHU-IDENTITY-011", "SECRET", "GROUP_01", true));
        rules.add(new RequirementSpec("TIANSHU-IDENTITY-012", "SAFE_LOG", "GROUP_01", true));
        rules.add(new RequirementSpec("TIANSHU-IDENTITY-013", "MODEL_PROOF", "GROUP_01", true));
        rules.add(new RequirementSpec("TIANSHU-IDENTITY-014", "REALTIME_PROOF", "GROUP_01", true));
        rules.add(new RequirementSpec("TIANSHU-IDENTITY-015", "SHADOW_ONLY", "GROUP_01", true));
        rules.add(new RequirementSpec("TIANSHU-IDENTITY-016", "LEGACY_RUNTIME", "GROUP_01", true));
        rules.add(new RequirementSpec("TIANSHU-IDENTITY-017", "OWNER", "GROUP_01", true));
        rules.add(new RequirementSpec("TIANSHU-IDENTITY-018", "PIT", "GROUP_01", true));
        rules.add(new RequirementSpec("TIANSHU-IDENTITY-019", "SETTLEMENT", "GROUP_01", true));
        rules.add(new RequirementSpec("TIANSHU-IDENTITY-020", "LEDGER", "GROUP_01", true));
        rules.add(new RequirementSpec("TIANSHU-IDENTITY-021", "OOS", "GROUP_01", true));
        rules.add(new RequirementSpec("TIANSHU-IDENTITY-022", "PARITY", "GROUP_01", true));
        rules.add(new RequirementSpec("TIANSHU-IDENTITY-023", "CALIBRATION", "GROUP_01", true));
        rules.add(new RequirementSpec("TIANSHU-IDENTITY-024", "CERTIFICATE", "GROUP_01", true));
        rules.add(new RequirementSpec("TIANSHU-IDENTITY-025", "PERFORMANCE", "GROUP_01", true));
        rules.add(new RequirementSpec("TIANSHU-IDENTITY-026", "APK", "GROUP_01", true));
        rules.add(new RequirementSpec("TIANSHU-IDENTITY-027", "SOURCE_ZIP", "GROUP_01", true));
        rules.add(new RequirementSpec("TIANSHU-IDENTITY-028", "SHA256", "GROUP_01", true));
        rules.add(new RequirementSpec("TIANSHU-IDENTITY-029", "PACKAGE", "GROUP_02", true));
        rules.add(new RequirementSpec("TIANSHU-IDENTITY-030", "VERSION", "GROUP_02", true));
        rules.add(new RequirementSpec("TIANSHU-IDENTITY-031", "GAME_CODE", "GROUP_02", true));
        rules.add(new RequirementSpec("TIANSHU-IDENTITY-032", "DATABASE", "GROUP_02", true));
        rules.add(new RequirementSpec("TIANSHU-IDENTITY-033", "BUILD_ID", "GROUP_02", true));
        rules.add(new RequirementSpec("TIANSHU-IDENTITY-034", "COHORT", "GROUP_02", true));
        rules.add(new RequirementSpec("TIANSHU-IDENTITY-035", "CONTRACT_HASH", "GROUP_02", true));
        rules.add(new RequirementSpec("TIANSHU-IDENTITY-036", "BUNDLE_HASH", "GROUP_02", true));
        rules.add(new RequirementSpec("TIANSHU-IDENTITY-037", "TRANSPORT", "GROUP_02", true));
        rules.add(new RequirementSpec("TIANSHU-IDENTITY-038", "PARSER", "GROUP_02", true));
        rules.add(new RequirementSpec("TIANSHU-IDENTITY-039", "SECRET", "GROUP_02", true));
        rules.add(new RequirementSpec("TIANSHU-IDENTITY-040", "SAFE_LOG", "GROUP_02", true));
        rules.add(new RequirementSpec("TIANSHU-IDENTITY-041", "MODEL_PROOF", "GROUP_02", true));
        rules.add(new RequirementSpec("TIANSHU-IDENTITY-042", "REALTIME_PROOF", "GROUP_02", true));
        rules.add(new RequirementSpec("TIANSHU-IDENTITY-043", "SHADOW_ONLY", "GROUP_02", true));
        rules.add(new RequirementSpec("TIANSHU-IDENTITY-044", "LEGACY_RUNTIME", "GROUP_02", true));
        rules.add(new RequirementSpec("TIANSHU-IDENTITY-045", "OWNER", "GROUP_02", true));
        rules.add(new RequirementSpec("TIANSHU-IDENTITY-046", "PIT", "GROUP_02", true));
        rules.add(new RequirementSpec("TIANSHU-IDENTITY-047", "SETTLEMENT", "GROUP_02", true));
        rules.add(new RequirementSpec("TIANSHU-IDENTITY-048", "LEDGER", "GROUP_02", true));
        rules.add(new RequirementSpec("TIANSHU-IDENTITY-049", "OOS", "GROUP_02", true));
        rules.add(new RequirementSpec("TIANSHU-IDENTITY-050", "PARITY", "GROUP_02", true));
        rules.add(new RequirementSpec("TIANSHU-IDENTITY-051", "CALIBRATION", "GROUP_02", true));
        rules.add(new RequirementSpec("TIANSHU-IDENTITY-052", "CERTIFICATE", "GROUP_02", true));
        rules.add(new RequirementSpec("TIANSHU-IDENTITY-053", "PERFORMANCE", "GROUP_02", true));
        rules.add(new RequirementSpec("TIANSHU-IDENTITY-054", "APK", "GROUP_02", true));
        rules.add(new RequirementSpec("TIANSHU-IDENTITY-055", "SOURCE_ZIP", "GROUP_02", true));
        rules.add(new RequirementSpec("TIANSHU-IDENTITY-056", "SHA256", "GROUP_02", true));
        rules.add(new RequirementSpec("TIANSHU-IDENTITY-057", "PACKAGE", "GROUP_03", true));
        rules.add(new RequirementSpec("TIANSHU-IDENTITY-058", "VERSION", "GROUP_03", true));
        rules.add(new RequirementSpec("TIANSHU-IDENTITY-059", "GAME_CODE", "GROUP_03", true));
        rules.add(new RequirementSpec("TIANSHU-IDENTITY-060", "DATABASE", "GROUP_03", true));
        rules.add(new RequirementSpec("TIANSHU-IDENTITY-061", "BUILD_ID", "GROUP_03", true));
        rules.add(new RequirementSpec("TIANSHU-IDENTITY-062", "COHORT", "GROUP_03", true));
        rules.add(new RequirementSpec("TIANSHU-IDENTITY-063", "CONTRACT_HASH", "GROUP_03", true));
        rules.add(new RequirementSpec("TIANSHU-IDENTITY-064", "BUNDLE_HASH", "GROUP_03", true));
        rules.add(new RequirementSpec("TIANSHU-IDENTITY-065", "TRANSPORT", "GROUP_03", true));
        rules.add(new RequirementSpec("TIANSHU-IDENTITY-066", "PARSER", "GROUP_03", true));
        rules.add(new RequirementSpec("TIANSHU-IDENTITY-067", "SECRET", "GROUP_03", true));
        rules.add(new RequirementSpec("TIANSHU-IDENTITY-068", "SAFE_LOG", "GROUP_03", true));
        rules.add(new RequirementSpec("TIANSHU-IDENTITY-069", "MODEL_PROOF", "GROUP_03", true));
        rules.add(new RequirementSpec("TIANSHU-IDENTITY-070", "REALTIME_PROOF", "GROUP_03", true));
        rules.add(new RequirementSpec("TIANSHU-IDENTITY-071", "SHADOW_ONLY", "GROUP_03", true));
        rules.add(new RequirementSpec("TIANSHU-IDENTITY-072", "LEGACY_RUNTIME", "GROUP_03", true));
        rules.add(new RequirementSpec("TIANSHU-IDENTITY-073", "OWNER", "GROUP_03", true));
        rules.add(new RequirementSpec("TIANSHU-IDENTITY-074", "PIT", "GROUP_03", true));
        rules.add(new RequirementSpec("TIANSHU-IDENTITY-075", "SETTLEMENT", "GROUP_03", true));
        rules.add(new RequirementSpec("TIANSHU-IDENTITY-076", "LEDGER", "GROUP_03", true));
        rules.add(new RequirementSpec("TIANSHU-IDENTITY-077", "OOS", "GROUP_03", true));
        rules.add(new RequirementSpec("TIANSHU-IDENTITY-078", "PARITY", "GROUP_03", true));
        rules.add(new RequirementSpec("TIANSHU-IDENTITY-079", "CALIBRATION", "GROUP_03", true));
        rules.add(new RequirementSpec("TIANSHU-IDENTITY-080", "CERTIFICATE", "GROUP_03", true));
        rules.add(new RequirementSpec("TIANSHU-IDENTITY-081", "PERFORMANCE", "GROUP_03", true));
        rules.add(new RequirementSpec("TIANSHU-IDENTITY-082", "APK", "GROUP_03", true));
        rules.add(new RequirementSpec("TIANSHU-IDENTITY-083", "SOURCE_ZIP", "GROUP_03", true));
        rules.add(new RequirementSpec("TIANSHU-IDENTITY-084", "SHA256", "GROUP_03", true));
        rules.add(new RequirementSpec("TIANSHU-IDENTITY-085", "PACKAGE", "GROUP_04", true));
        rules.add(new RequirementSpec("TIANSHU-IDENTITY-086", "VERSION", "GROUP_04", true));
        rules.add(new RequirementSpec("TIANSHU-IDENTITY-087", "GAME_CODE", "GROUP_04", true));
        rules.add(new RequirementSpec("TIANSHU-IDENTITY-088", "DATABASE", "GROUP_04", true));
        rules.add(new RequirementSpec("TIANSHU-IDENTITY-089", "BUILD_ID", "GROUP_04", true));
        rules.add(new RequirementSpec("TIANSHU-IDENTITY-090", "COHORT", "GROUP_04", true));
        rules.add(new RequirementSpec("TIANSHU-IDENTITY-091", "CONTRACT_HASH", "GROUP_04", true));
        rules.add(new RequirementSpec("TIANSHU-IDENTITY-092", "BUNDLE_HASH", "GROUP_04", true));
        rules.add(new RequirementSpec("TIANSHU-IDENTITY-093", "TRANSPORT", "GROUP_04", true));
        rules.add(new RequirementSpec("TIANSHU-IDENTITY-094", "PARSER", "GROUP_04", true));
        rules.add(new RequirementSpec("TIANSHU-IDENTITY-095", "SECRET", "GROUP_04", true));
        rules.add(new RequirementSpec("TIANSHU-IDENTITY-096", "SAFE_LOG", "GROUP_04", true));
        rules.add(new RequirementSpec("TIANSHU-IDENTITY-097", "MODEL_PROOF", "GROUP_04", true));
        rules.add(new RequirementSpec("TIANSHU-IDENTITY-098", "REALTIME_PROOF", "GROUP_04", true));
        rules.add(new RequirementSpec("TIANSHU-IDENTITY-099", "SHADOW_ONLY", "GROUP_04", true));
        rules.add(new RequirementSpec("TIANSHU-IDENTITY-100", "LEGACY_RUNTIME", "GROUP_04", true));
        rules.add(new RequirementSpec("TIANSHU-IDENTITY-101", "OWNER", "GROUP_04", true));
        rules.add(new RequirementSpec("TIANSHU-IDENTITY-102", "PIT", "GROUP_04", true));
        rules.add(new RequirementSpec("TIANSHU-IDENTITY-103", "SETTLEMENT", "GROUP_04", true));
        rules.add(new RequirementSpec("TIANSHU-IDENTITY-104", "LEDGER", "GROUP_04", true));
        rules.add(new RequirementSpec("TIANSHU-IDENTITY-105", "OOS", "GROUP_04", true));
        rules.add(new RequirementSpec("TIANSHU-IDENTITY-106", "PARITY", "GROUP_04", true));
        rules.add(new RequirementSpec("TIANSHU-IDENTITY-107", "CALIBRATION", "GROUP_04", true));
        rules.add(new RequirementSpec("TIANSHU-IDENTITY-108", "CERTIFICATE", "GROUP_04", true));
        rules.add(new RequirementSpec("TIANSHU-IDENTITY-109", "PERFORMANCE", "GROUP_04", true));
        rules.add(new RequirementSpec("TIANSHU-IDENTITY-110", "APK", "GROUP_04", true));
        rules.add(new RequirementSpec("TIANSHU-IDENTITY-111", "SOURCE_ZIP", "GROUP_04", true));
        rules.add(new RequirementSpec("TIANSHU-IDENTITY-112", "SHA256", "GROUP_04", true));
        rules.add(new RequirementSpec("TIANSHU-IDENTITY-113", "PACKAGE", "GROUP_05", true));
        rules.add(new RequirementSpec("TIANSHU-IDENTITY-114", "VERSION", "GROUP_05", true));
        rules.add(new RequirementSpec("TIANSHU-IDENTITY-115", "GAME_CODE", "GROUP_05", true));
        rules.add(new RequirementSpec("TIANSHU-IDENTITY-116", "DATABASE", "GROUP_05", true));
        rules.add(new RequirementSpec("TIANSHU-IDENTITY-117", "BUILD_ID", "GROUP_05", true));
        rules.add(new RequirementSpec("TIANSHU-IDENTITY-118", "COHORT", "GROUP_05", true));
        rules.add(new RequirementSpec("TIANSHU-IDENTITY-119", "CONTRACT_HASH", "GROUP_05", true));
        rules.add(new RequirementSpec("TIANSHU-IDENTITY-120", "BUNDLE_HASH", "GROUP_05", true));
        rules.add(new RequirementSpec("TIANSHU-IDENTITY-121", "TRANSPORT", "GROUP_05", true));
        rules.add(new RequirementSpec("TIANSHU-IDENTITY-122", "PARSER", "GROUP_05", true));
        rules.add(new RequirementSpec("TIANSHU-IDENTITY-123", "SECRET", "GROUP_05", true));
        rules.add(new RequirementSpec("TIANSHU-IDENTITY-124", "SAFE_LOG", "GROUP_05", true));
        rules.add(new RequirementSpec("TIANSHU-IDENTITY-125", "MODEL_PROOF", "GROUP_05", true));
        rules.add(new RequirementSpec("TIANSHU-IDENTITY-126", "REALTIME_PROOF", "GROUP_05", true));
        rules.add(new RequirementSpec("TIANSHU-IDENTITY-127", "SHADOW_ONLY", "GROUP_05", true));
        rules.add(new RequirementSpec("TIANSHU-IDENTITY-128", "LEGACY_RUNTIME", "GROUP_05", true));
        rules.add(new RequirementSpec("TIANSHU-IDENTITY-129", "OWNER", "GROUP_05", true));
        rules.add(new RequirementSpec("TIANSHU-IDENTITY-130", "PIT", "GROUP_05", true));
        rules.add(new RequirementSpec("TIANSHU-IDENTITY-131", "SETTLEMENT", "GROUP_05", true));
        rules.add(new RequirementSpec("TIANSHU-IDENTITY-132", "LEDGER", "GROUP_05", true));
        rules.add(new RequirementSpec("TIANSHU-IDENTITY-133", "OOS", "GROUP_05", true));
        rules.add(new RequirementSpec("TIANSHU-IDENTITY-134", "PARITY", "GROUP_05", true));
        rules.add(new RequirementSpec("TIANSHU-IDENTITY-135", "CALIBRATION", "GROUP_05", true));
        rules.add(new RequirementSpec("TIANSHU-IDENTITY-136", "CERTIFICATE", "GROUP_05", true));
        rules.add(new RequirementSpec("TIANSHU-IDENTITY-137", "PERFORMANCE", "GROUP_05", true));
        rules.add(new RequirementSpec("TIANSHU-IDENTITY-138", "APK", "GROUP_05", true));
        rules.add(new RequirementSpec("TIANSHU-IDENTITY-139", "SOURCE_ZIP", "GROUP_05", true));
        rules.add(new RequirementSpec("TIANSHU-IDENTITY-140", "SHA256", "GROUP_05", true));
        rules.add(new RequirementSpec("TIANSHU-IDENTITY-141", "PACKAGE", "GROUP_06", true));
        rules.add(new RequirementSpec("TIANSHU-IDENTITY-142", "VERSION", "GROUP_06", true));
        rules.add(new RequirementSpec("TIANSHU-IDENTITY-143", "GAME_CODE", "GROUP_06", true));
        rules.add(new RequirementSpec("TIANSHU-IDENTITY-144", "DATABASE", "GROUP_06", true));
        rules.add(new RequirementSpec("TIANSHU-IDENTITY-145", "BUILD_ID", "GROUP_06", true));
        rules.add(new RequirementSpec("TIANSHU-IDENTITY-146", "COHORT", "GROUP_06", true));
        rules.add(new RequirementSpec("TIANSHU-IDENTITY-147", "CONTRACT_HASH", "GROUP_06", true));
        rules.add(new RequirementSpec("TIANSHU-IDENTITY-148", "BUNDLE_HASH", "GROUP_06", true));
        rules.add(new RequirementSpec("TIANSHU-IDENTITY-149", "TRANSPORT", "GROUP_06", true));
        rules.add(new RequirementSpec("TIANSHU-IDENTITY-150", "PARSER", "GROUP_06", true));
        rules.add(new RequirementSpec("TIANSHU-IDENTITY-151", "SECRET", "GROUP_06", true));
        rules.add(new RequirementSpec("TIANSHU-IDENTITY-152", "SAFE_LOG", "GROUP_06", true));
        rules.add(new RequirementSpec("TIANSHU-IDENTITY-153", "MODEL_PROOF", "GROUP_06", true));
        rules.add(new RequirementSpec("TIANSHU-IDENTITY-154", "REALTIME_PROOF", "GROUP_06", true));
        rules.add(new RequirementSpec("TIANSHU-IDENTITY-155", "SHADOW_ONLY", "GROUP_06", true));
        rules.add(new RequirementSpec("TIANSHU-IDENTITY-156", "LEGACY_RUNTIME", "GROUP_06", true));
        rules.add(new RequirementSpec("TIANSHU-IDENTITY-157", "OWNER", "GROUP_06", true));
        rules.add(new RequirementSpec("TIANSHU-IDENTITY-158", "PIT", "GROUP_06", true));
        rules.add(new RequirementSpec("TIANSHU-IDENTITY-159", "SETTLEMENT", "GROUP_06", true));
        rules.add(new RequirementSpec("TIANSHU-IDENTITY-160", "LEDGER", "GROUP_06", true));
        rules.add(new RequirementSpec("TIANSHU-IDENTITY-161", "OOS", "GROUP_06", true));
        rules.add(new RequirementSpec("TIANSHU-IDENTITY-162", "PARITY", "GROUP_06", true));
        rules.add(new RequirementSpec("TIANSHU-IDENTITY-163", "CALIBRATION", "GROUP_06", true));
        rules.add(new RequirementSpec("TIANSHU-IDENTITY-164", "CERTIFICATE", "GROUP_06", true));
        rules.add(new RequirementSpec("TIANSHU-IDENTITY-165", "PERFORMANCE", "GROUP_06", true));
        rules.add(new RequirementSpec("TIANSHU-IDENTITY-166", "APK", "GROUP_06", true));
        rules.add(new RequirementSpec("TIANSHU-IDENTITY-167", "SOURCE_ZIP", "GROUP_06", true));
        rules.add(new RequirementSpec("TIANSHU-IDENTITY-168", "SHA256", "GROUP_06", true));
        rules.add(new RequirementSpec("TIANSHU-IDENTITY-169", "PACKAGE", "GROUP_07", true));
        rules.add(new RequirementSpec("TIANSHU-IDENTITY-170", "VERSION", "GROUP_07", true));
        rules.add(new RequirementSpec("TIANSHU-IDENTITY-171", "GAME_CODE", "GROUP_07", true));
        rules.add(new RequirementSpec("TIANSHU-IDENTITY-172", "DATABASE", "GROUP_07", true));
        rules.add(new RequirementSpec("TIANSHU-IDENTITY-173", "BUILD_ID", "GROUP_07", true));
        rules.add(new RequirementSpec("TIANSHU-IDENTITY-174", "COHORT", "GROUP_07", true));
        rules.add(new RequirementSpec("TIANSHU-IDENTITY-175", "CONTRACT_HASH", "GROUP_07", true));
        return Collections.unmodifiableList(rules);
    }

    public static String verify(Context context) {
        return verifyReport(context).status;
    }

    public static VerificationReport verifyReport(Context context) {
        ArrayList<CheckResult> checks = new ArrayList<CheckResult>();
        if (context == null) {
            checks.add(new CheckResult("CONTEXT", false, "CONTEXT_NULL", true));
            return finish(checks);
        }
        String packageName = safe(context.getPackageName());
        String version = actualVersion(context, packageName);
        checks.add(
                new CheckResult(
                        "PACKAGE", AppConfig.PACKAGE_NAME.equals(packageName), packageName, true));
        checks.add(
                new CheckResult(
                        "VERSION", V51BuildManifest.VERSION_NAME.equals(version), version, true));
        checks.add(
                new CheckResult(
                        "GAME_CODE",
                        V51BuildManifest.GAME_CODE.equals(AppConfig.LOTTERY_CODE),
                        AppConfig.LOTTERY_CODE,
                        true));
        checks.add(
                new CheckResult(
                        "DATABASE",
                        V51BuildManifest.DATABASE_NAME.equals(AppConfig.DB_NAME),
                        AppConfig.DB_NAME,
                        true));
        checks.add(
                new CheckResult(
                        "DATABASE_SCHEMA_VERSION",
                        AppConfig.DB_VERSION == 36,
                        String.valueOf(AppConfig.DB_VERSION),
                        true));
        checks.add(
                new CheckResult(
                        "R36_RUNTIME",
                        AppConfig.R36_RUNTIME_REPAIR_VERSION.equals(ReleaseManifestR926.VERSION_NAME),
                        AppConfig.R36_RUNTIME_REPAIR_VERSION,
                        true));
        checks.add(
                new CheckResult(
                        "R811_SCHEMA_REVISION_DECLARED",
                        AppConfig.R811_SCHEMA_REVISION == 1,
                        String.valueOf(AppConfig.R811_SCHEMA_REVISION),
                        true));
        checks.add(
                new CheckResult(
                        "R811_RESEARCH_BOUNDARY",
                        !V51BuildManifest.MODEL_PROVEN
                                && !V51BuildManifest.REALTIME_PROVEN
                                && !V51BuildManifest.SHADOW_ONLY
                                && !V51BuildManifest.REAL_BET_HARD_DISABLED
                                && V51BuildManifest.CANDIDATE_REAL_AUTH_ENABLED,
                        "RESEARCH_NO_REAL_AUTHORITY|PRODUCTION_EXACT_CONTROLLED_GATE",
                        true));
        checks.add(
                new CheckResult(
                        "MANIFEST",
                        V51BuildManifest.isIdentityConsistent(),
                        V51BuildManifest.releaseState(),
                        true));
        checks.add(
                new CheckResult(
                        "BUNDLE",
                        V51ArtifactBundleManifest.isInternallyConsistent(),
                        V51ArtifactBundleManifest.promotionBlockReason(),
                        true));
        checks.add(
                new CheckResult(
                        "TRANSPORT",
                        AppConfig.TianshuB01.isProductionTransportConfigValid(),
                        AppConfig.TianshuB01.status(),
                        true));
        checks.add(
                new CheckResult(
                        "SECRET", KeyProvider.policyReady(), KeyProvider.maskedStatus(), true));
        checks.add(new CheckResult("SAFE_LOG", SafeLog.selfTest(), "redaction-self-test", true));
        checks.add(
                new CheckResult(
                        "MODEL_PROVEN_FALSE",
                        !V51BuildManifest.MODEL_PROVEN,
                        "MODEL_NOT_PROVEN",
                        true));
        checks.add(
                new CheckResult(
                        "CANDIDATE_REAL_AUTH",
                        (V51BuildManifest.MODEL_PROVEN
                                        && V51BuildManifest.CANDIDATE_REAL_AUTH_ENABLED
                                        && !V51BuildManifest.SHADOW_ONLY
                                        && !V51BuildManifest.REAL_BET_HARD_DISABLED)
                                || (!V51BuildManifest.MODEL_PROVEN
                                        && !V51BuildManifest.REALTIME_PROVEN
                                        && V51BuildManifest.CANDIDATE_REAL_AUTH_ENABLED
                                        && !V51BuildManifest.SHADOW_ONLY
                                        && !V51BuildManifest.REAL_BET_HARD_DISABLED),
                        V51BuildManifest.releaseState(),
                        true));
        checks.add(
                new CheckResult(
                        "LEGACY_RUNTIME_DISABLED",
                        !V51BuildManifest.LEGACY_RUNTIME_ENABLED,
                        "legacy-observer-only",
                        true));
        return finish(checks);
    }

    private static VerificationReport finish(ArrayList<CheckResult> checks) {
        boolean passed = true;
        StringBuilder canonical = new StringBuilder();
        for (int i = 0; i < checks.size(); i++) {
            CheckResult check = checks.get(i);
            if (!check.passed && check.hard) passed = false;
            if (i > 0) canonical.append('|');
            canonical.append(check.canonical());
        }
        String status = passed ? STATUS_OK : "BUILD_IDENTITY_HARD_BLOCK";
        String fp =
                V51ChecksumEngine.sha256(
                        V51BuildManifest.fingerprint() + '|' + canonical.toString());
        return new VerificationReport(status, Collections.unmodifiableList(checks), fp);
    }

    public static boolean isRuntimeCompatible(String status) {
        return STATUS_OK.equals(status);
    }

    public static boolean isShadowCompatible(String status) {
        return isRuntimeCompatible(status);
    }

    public static boolean isBetCompatible(String status) {
        return isRuntimeCompatible(status) && V51BuildManifest.mayAuthorizeRealBet();
    }

    public static boolean isHardBlock(String status) {
        return !isRuntimeCompatible(status);
    }

    public static String verifyAndPersist(Context context, DbStore store) {
        VerificationReport report = verifyReport(context);
        String packageName = context == null ? "" : safe(context.getPackageName());
        String version = actualVersion(context, packageName);
        String status = report.status;
        if (store != null) {
            try {
                String schemaRevision = store.getState("r811_schema_revision", "");
                if (!String.valueOf(AppConfig.R811_SCHEMA_REVISION).equals(schemaRevision)) {
                    status = "BUILD_IDENTITY_HARD_BLOCK:R811_SCHEMA_REVISION=" + schemaRevision;
                }
                store.setState(STATE_STATUS, status);
                store.setState(STATE_DETAIL, report.detail());
                store.setState(STATE_PACKAGE, packageName);
                store.setState(STATE_VERSION_NAME, version);
                store.setState(STATE_EXPECTED_VERSION_NAME, V51BuildManifest.VERSION_NAME);
                store.setState(STATE_STRATEGY, V51BuildManifest.STRATEGY_VERSION);
                store.setState(STATE_VERIFIED_AT, String.valueOf(System.currentTimeMillis()));
                store.setState(STATE_HARD_BLOCK, report.hardBlocked() ? "1" : "0");
                store.setState(STATE_CODE_FINGERPRINT, report.fingerprint);
                store.registerModuleBuildV20(
                        "BuildIdentity", V51BuildManifest.STRATEGY_VERSION, status);
            } catch (Throwable error) {
                status = "BUILD_IDENTITY_ERROR:" + error.getClass().getSimpleName();
            }
        }
        return status;
    }

    private static String actualVersion(Context context, String packageName) {
        if (context == null) return "";
        try {
            PackageInfo info = context.getPackageManager().getPackageInfo(packageName, 0);
            return info == null ? "" : safe(info.versionName);
        } catch (Throwable error) {
            return "PACKAGE_INFO_ERROR:" + error.getClass().getSimpleName();
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
