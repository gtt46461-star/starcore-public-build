package com.ethk3.novaonepro;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Immutable build identity for Starcore Tianshu B01 on the frozen V51 full source. */
public final class V51BuildManifest {
    private V51BuildManifest() {}

    public static final String MATERIALIZATION_MARKER =
            "STARCORE_TIANSHU_B01_B15_R368_811_FINAL_LOCKED_RUNTIME_RESEARCH_BOUNDARY_20260816";
    public static final String CODE_NAME = "STARCORE_TIANSHU";
    public static final String BATCH_ID = "B15";
    public static final String VERSION_NAME = ReleaseManifestR926.VERSION_NAME;
    public static final int VERSION_CODE = ReleaseManifestR926.VERSION_CODE;
    public static final String PRODUCT_NAME = "星核·币安K3天枢";
    public static final String FORMAL_NAME =
            "Starcore Tianshu Binance K3 Point-In-Time Selective Research System";
    public static final String STRATEGY_VERSION =
            "STARCORE_TIANSHU_B01_B15_R368_811_FINAL_LOCKED_RUNTIME_RESEARCH_BOUNDARY_20260816";
    public static final String BUILD_ID =
            "STARCORE_TIANSHU_BUILD_B01_B15_R368_811_FINAL_LOCKED_RUNTIME_RESEARCH_BOUNDARY_20260816";
    /**
     * The APK/build identity advances at R914, but the Production strategy cohort stays frozen.
     * Changing this value would alter B04's feature-contract hash and silently start a new model
     * cohort, which is forbidden by the R811 zero-drift contract.
     */
    public static final String COHORT_ID =
            "STARCORE_V57_B01_B15_R366_ACTION_SNAPSHOT_CANONICAL_RUNTIME_CLOSURE_20260811";
    public static final String GAME_CODE = ReleaseManifestR926.GAME_CODE;
    public static final String DATABASE_NAME = ReleaseManifestR926.DATABASE_NAME;
    public static final String DATABASE_SCHEMA = "TIANSHU_BINANCE_SCHEMA_B03_IMMUTABLE";
    public static final String FEATURE_SCHEMA = "TIANSHU_PIT_FEATURE_SCHEMA_B04_V1";
    public static final String STATE_SCHEMA = "TIANSHU_RUNTIME_OOS_DIAGNOSIS_R36_V1";
    public static final String EXPERT_SCHEMA = "TIANSHU_EXPERT_SCHEMA_B09_V1";
    public static final String TRANSPORT_SCHEMA = "TIANSHU_DUOQU_TRANSPORT_B01";
    public static final String PARSER_SCHEMA = "TIANSHU_DUOQU_PARSER_B01";
    public static final String SECRET_SCHEMA = "TIANSHU_BUILD_INJECTED_SECRET_B01";
    public static final String RELEASE_STATE =
            "ENGINEERING_COMPLETE_MODEL_NOT_PROVEN_CONTROLLED_CANDIDATE";
    public static final String CONTRACT_HASH =
            V51ChecksumEngine.sha256(
                    GAME_CODE
                            + "|single-position|K=3|max-real-bet=1|no-martingale|"
                            + "no-frequency-debt|decision-gate-only|settlement-engine-only|strict-oos-required|"
                            + "model-not-proven|controlled-candidate-exact-gate|starcore-tianshu-b01-b15-final");

    public static final boolean ACTIVE_RUNTIME = true;
    public static final boolean LEGACY_OBSERVER_ENABLED = true;
    public static final boolean LEGACY_RUNTIME_ENABLED = false;
    public static final boolean ENGINEERING_COMPLETE = true;
    public static final boolean REALTIME_PROVEN = false;
    public static final boolean MODEL_PROVEN = false;
    public static final boolean SHADOW_ONLY = false;
    public static final boolean REAL_BET_HARD_DISABLED = false;
    public static final boolean CANDIDATE_REAL_AUTH_ENABLED = true;

    public static final class ManifestEntry {
        public final String id;
        public final String field;
        public final String group;
        public final boolean mandatory;

        ManifestEntry(String id, String field, String group, boolean mandatory) {
            this.id = id;
            this.field = field;
            this.group = group;
            this.mandatory = mandatory;
        }

        public String canonical() {
            return id + ':' + field + ':' + group + ':' + (mandatory ? '1' : '0');
        }
    }

    public static List<ManifestEntry> entries() {
        ArrayList<ManifestEntry> rules = new ArrayList<ManifestEntry>();
        rules.add(new ManifestEntry("TIANSHU-BUILD-001", "PRODUCT", "GROUP_01", true));
        rules.add(new ManifestEntry("TIANSHU-BUILD-002", "VERSION", "GROUP_01", true));
        rules.add(new ManifestEntry("TIANSHU-BUILD-003", "BUILD", "GROUP_01", true));
        rules.add(new ManifestEntry("TIANSHU-BUILD-004", "COHORT", "GROUP_01", true));
        rules.add(new ManifestEntry("TIANSHU-BUILD-005", "GAME", "GROUP_01", true));
        rules.add(new ManifestEntry("TIANSHU-BUILD-006", "DATABASE", "GROUP_01", true));
        rules.add(new ManifestEntry("TIANSHU-BUILD-007", "SCHEMA", "GROUP_01", true));
        rules.add(new ManifestEntry("TIANSHU-BUILD-008", "FEATURE", "GROUP_01", true));
        rules.add(new ManifestEntry("TIANSHU-BUILD-009", "STATE", "GROUP_01", true));
        rules.add(new ManifestEntry("TIANSHU-BUILD-010", "EXPERT", "GROUP_01", true));
        rules.add(new ManifestEntry("TIANSHU-BUILD-011", "TRANSPORT", "GROUP_01", true));
        rules.add(new ManifestEntry("TIANSHU-BUILD-012", "PARSER", "GROUP_01", true));
        rules.add(new ManifestEntry("TIANSHU-BUILD-013", "KEY", "GROUP_01", true));
        rules.add(new ManifestEntry("TIANSHU-BUILD-014", "SAFE_LOG", "GROUP_01", true));
        rules.add(new ManifestEntry("TIANSHU-BUILD-015", "PIT", "GROUP_01", true));
        rules.add(new ManifestEntry("TIANSHU-BUILD-016", "OWNER", "GROUP_01", true));
        rules.add(new ManifestEntry("TIANSHU-BUILD-017", "NUMBER_P", "GROUP_01", true));
        rules.add(new ManifestEntry("TIANSHU-BUILD-018", "ACTION_P", "GROUP_01", true));
        rules.add(new ManifestEntry("TIANSHU-BUILD-019", "SELECTED_HIT_P", "GROUP_01", true));
        rules.add(new ManifestEntry("TIANSHU-BUILD-020", "LANE", "GROUP_01", true));
        rules.add(new ManifestEntry("TIANSHU-BUILD-021", "BET", "GROUP_01", true));
        rules.add(new ManifestEntry("TIANSHU-BUILD-022", "SETTLEMENT", "GROUP_01", true));
        rules.add(new ManifestEntry("TIANSHU-BUILD-023", "LEDGER", "GROUP_01", true));
        rules.add(new ManifestEntry("TIANSHU-BUILD-024", "SHADOW", "GROUP_01", true));
        rules.add(new ManifestEntry("TIANSHU-BUILD-025", "MODEL_PROOF", "GROUP_01", true));
        rules.add(new ManifestEntry("TIANSHU-BUILD-026", "REALTIME_PROOF", "GROUP_01", true));
        rules.add(new ManifestEntry("TIANSHU-BUILD-027", "PARITY", "GROUP_01", true));
        rules.add(new ManifestEntry("TIANSHU-BUILD-028", "HASH", "GROUP_01", true));
        rules.add(new ManifestEntry("TIANSHU-BUILD-029", "REPLAY", "GROUP_01", true));
        rules.add(new ManifestEntry("TIANSHU-BUILD-030", "OOS", "GROUP_01", true));
        rules.add(new ManifestEntry("TIANSHU-BUILD-031", "CALIBRATION", "GROUP_01", true));
        rules.add(new ManifestEntry("TIANSHU-BUILD-032", "CERTIFICATE", "GROUP_01", true));
        rules.add(new ManifestEntry("TIANSHU-BUILD-033", "PRODUCT", "GROUP_02", true));
        rules.add(new ManifestEntry("TIANSHU-BUILD-034", "VERSION", "GROUP_02", true));
        rules.add(new ManifestEntry("TIANSHU-BUILD-035", "BUILD", "GROUP_02", true));
        rules.add(new ManifestEntry("TIANSHU-BUILD-036", "COHORT", "GROUP_02", true));
        rules.add(new ManifestEntry("TIANSHU-BUILD-037", "GAME", "GROUP_02", true));
        rules.add(new ManifestEntry("TIANSHU-BUILD-038", "DATABASE", "GROUP_02", true));
        rules.add(new ManifestEntry("TIANSHU-BUILD-039", "SCHEMA", "GROUP_02", true));
        rules.add(new ManifestEntry("TIANSHU-BUILD-040", "FEATURE", "GROUP_02", true));
        rules.add(new ManifestEntry("TIANSHU-BUILD-041", "STATE", "GROUP_02", true));
        rules.add(new ManifestEntry("TIANSHU-BUILD-042", "EXPERT", "GROUP_02", true));
        rules.add(new ManifestEntry("TIANSHU-BUILD-043", "TRANSPORT", "GROUP_02", true));
        rules.add(new ManifestEntry("TIANSHU-BUILD-044", "PARSER", "GROUP_02", true));
        rules.add(new ManifestEntry("TIANSHU-BUILD-045", "KEY", "GROUP_02", true));
        rules.add(new ManifestEntry("TIANSHU-BUILD-046", "SAFE_LOG", "GROUP_02", true));
        rules.add(new ManifestEntry("TIANSHU-BUILD-047", "PIT", "GROUP_02", true));
        rules.add(new ManifestEntry("TIANSHU-BUILD-048", "OWNER", "GROUP_02", true));
        rules.add(new ManifestEntry("TIANSHU-BUILD-049", "NUMBER_P", "GROUP_02", true));
        rules.add(new ManifestEntry("TIANSHU-BUILD-050", "ACTION_P", "GROUP_02", true));
        rules.add(new ManifestEntry("TIANSHU-BUILD-051", "SELECTED_HIT_P", "GROUP_02", true));
        rules.add(new ManifestEntry("TIANSHU-BUILD-052", "LANE", "GROUP_02", true));
        rules.add(new ManifestEntry("TIANSHU-BUILD-053", "BET", "GROUP_02", true));
        rules.add(new ManifestEntry("TIANSHU-BUILD-054", "SETTLEMENT", "GROUP_02", true));
        rules.add(new ManifestEntry("TIANSHU-BUILD-055", "LEDGER", "GROUP_02", true));
        rules.add(new ManifestEntry("TIANSHU-BUILD-056", "SHADOW", "GROUP_02", true));
        rules.add(new ManifestEntry("TIANSHU-BUILD-057", "MODEL_PROOF", "GROUP_02", true));
        rules.add(new ManifestEntry("TIANSHU-BUILD-058", "REALTIME_PROOF", "GROUP_02", true));
        rules.add(new ManifestEntry("TIANSHU-BUILD-059", "PARITY", "GROUP_02", true));
        rules.add(new ManifestEntry("TIANSHU-BUILD-060", "HASH", "GROUP_02", true));
        rules.add(new ManifestEntry("TIANSHU-BUILD-061", "REPLAY", "GROUP_02", true));
        rules.add(new ManifestEntry("TIANSHU-BUILD-062", "OOS", "GROUP_02", true));
        rules.add(new ManifestEntry("TIANSHU-BUILD-063", "CALIBRATION", "GROUP_02", true));
        rules.add(new ManifestEntry("TIANSHU-BUILD-064", "CERTIFICATE", "GROUP_02", true));
        rules.add(new ManifestEntry("TIANSHU-BUILD-065", "PRODUCT", "GROUP_03", true));
        rules.add(new ManifestEntry("TIANSHU-BUILD-066", "VERSION", "GROUP_03", true));
        rules.add(new ManifestEntry("TIANSHU-BUILD-067", "BUILD", "GROUP_03", true));
        rules.add(new ManifestEntry("TIANSHU-BUILD-068", "COHORT", "GROUP_03", true));
        rules.add(new ManifestEntry("TIANSHU-BUILD-069", "GAME", "GROUP_03", true));
        rules.add(new ManifestEntry("TIANSHU-BUILD-070", "DATABASE", "GROUP_03", true));
        rules.add(new ManifestEntry("TIANSHU-BUILD-071", "SCHEMA", "GROUP_03", true));
        rules.add(new ManifestEntry("TIANSHU-BUILD-072", "FEATURE", "GROUP_03", true));
        rules.add(new ManifestEntry("TIANSHU-BUILD-073", "STATE", "GROUP_03", true));
        rules.add(new ManifestEntry("TIANSHU-BUILD-074", "EXPERT", "GROUP_03", true));
        rules.add(new ManifestEntry("TIANSHU-BUILD-075", "TRANSPORT", "GROUP_03", true));
        rules.add(new ManifestEntry("TIANSHU-BUILD-076", "PARSER", "GROUP_03", true));
        rules.add(new ManifestEntry("TIANSHU-BUILD-077", "KEY", "GROUP_03", true));
        rules.add(new ManifestEntry("TIANSHU-BUILD-078", "SAFE_LOG", "GROUP_03", true));
        rules.add(new ManifestEntry("TIANSHU-BUILD-079", "PIT", "GROUP_03", true));
        rules.add(new ManifestEntry("TIANSHU-BUILD-080", "OWNER", "GROUP_03", true));
        rules.add(new ManifestEntry("TIANSHU-BUILD-081", "NUMBER_P", "GROUP_03", true));
        rules.add(new ManifestEntry("TIANSHU-BUILD-082", "ACTION_P", "GROUP_03", true));
        rules.add(new ManifestEntry("TIANSHU-BUILD-083", "SELECTED_HIT_P", "GROUP_03", true));
        rules.add(new ManifestEntry("TIANSHU-BUILD-084", "LANE", "GROUP_03", true));
        rules.add(new ManifestEntry("TIANSHU-BUILD-085", "BET", "GROUP_03", true));
        rules.add(new ManifestEntry("TIANSHU-BUILD-086", "SETTLEMENT", "GROUP_03", true));
        rules.add(new ManifestEntry("TIANSHU-BUILD-087", "LEDGER", "GROUP_03", true));
        rules.add(new ManifestEntry("TIANSHU-BUILD-088", "SHADOW", "GROUP_03", true));
        rules.add(new ManifestEntry("TIANSHU-BUILD-089", "MODEL_PROOF", "GROUP_03", true));
        rules.add(new ManifestEntry("TIANSHU-BUILD-090", "REALTIME_PROOF", "GROUP_03", true));
        rules.add(new ManifestEntry("TIANSHU-BUILD-091", "PARITY", "GROUP_03", true));
        rules.add(new ManifestEntry("TIANSHU-BUILD-092", "HASH", "GROUP_03", true));
        rules.add(new ManifestEntry("TIANSHU-BUILD-093", "REPLAY", "GROUP_03", true));
        rules.add(new ManifestEntry("TIANSHU-BUILD-094", "OOS", "GROUP_03", true));
        rules.add(new ManifestEntry("TIANSHU-BUILD-095", "CALIBRATION", "GROUP_03", true));
        rules.add(new ManifestEntry("TIANSHU-BUILD-096", "CERTIFICATE", "GROUP_03", true));
        rules.add(new ManifestEntry("TIANSHU-BUILD-097", "PRODUCT", "GROUP_04", true));
        rules.add(new ManifestEntry("TIANSHU-BUILD-098", "VERSION", "GROUP_04", true));
        rules.add(new ManifestEntry("TIANSHU-BUILD-099", "BUILD", "GROUP_04", true));
        rules.add(new ManifestEntry("TIANSHU-BUILD-100", "COHORT", "GROUP_04", true));
        rules.add(new ManifestEntry("TIANSHU-BUILD-101", "GAME", "GROUP_04", true));
        rules.add(new ManifestEntry("TIANSHU-BUILD-102", "DATABASE", "GROUP_04", true));
        rules.add(new ManifestEntry("TIANSHU-BUILD-103", "SCHEMA", "GROUP_04", true));
        rules.add(new ManifestEntry("TIANSHU-BUILD-104", "FEATURE", "GROUP_04", true));
        rules.add(new ManifestEntry("TIANSHU-BUILD-105", "STATE", "GROUP_04", true));
        rules.add(new ManifestEntry("TIANSHU-BUILD-106", "EXPERT", "GROUP_04", true));
        rules.add(new ManifestEntry("TIANSHU-BUILD-107", "TRANSPORT", "GROUP_04", true));
        rules.add(new ManifestEntry("TIANSHU-BUILD-108", "PARSER", "GROUP_04", true));
        rules.add(new ManifestEntry("TIANSHU-BUILD-109", "KEY", "GROUP_04", true));
        rules.add(new ManifestEntry("TIANSHU-BUILD-110", "SAFE_LOG", "GROUP_04", true));
        rules.add(new ManifestEntry("TIANSHU-BUILD-111", "PIT", "GROUP_04", true));
        rules.add(new ManifestEntry("TIANSHU-BUILD-112", "OWNER", "GROUP_04", true));
        rules.add(new ManifestEntry("TIANSHU-BUILD-113", "NUMBER_P", "GROUP_04", true));
        rules.add(new ManifestEntry("TIANSHU-BUILD-114", "ACTION_P", "GROUP_04", true));
        rules.add(new ManifestEntry("TIANSHU-BUILD-115", "SELECTED_HIT_P", "GROUP_04", true));
        rules.add(new ManifestEntry("TIANSHU-BUILD-116", "LANE", "GROUP_04", true));
        rules.add(new ManifestEntry("TIANSHU-BUILD-117", "BET", "GROUP_04", true));
        rules.add(new ManifestEntry("TIANSHU-BUILD-118", "SETTLEMENT", "GROUP_04", true));
        rules.add(new ManifestEntry("TIANSHU-BUILD-119", "LEDGER", "GROUP_04", true));
        rules.add(new ManifestEntry("TIANSHU-BUILD-120", "SHADOW", "GROUP_04", true));
        rules.add(new ManifestEntry("TIANSHU-BUILD-121", "MODEL_PROOF", "GROUP_04", true));
        rules.add(new ManifestEntry("TIANSHU-BUILD-122", "REALTIME_PROOF", "GROUP_04", true));
        rules.add(new ManifestEntry("TIANSHU-BUILD-123", "PARITY", "GROUP_04", true));
        rules.add(new ManifestEntry("TIANSHU-BUILD-124", "HASH", "GROUP_04", true));
        rules.add(new ManifestEntry("TIANSHU-BUILD-125", "REPLAY", "GROUP_04", true));
        rules.add(new ManifestEntry("TIANSHU-BUILD-126", "OOS", "GROUP_04", true));
        rules.add(new ManifestEntry("TIANSHU-BUILD-127", "CALIBRATION", "GROUP_04", true));
        rules.add(new ManifestEntry("TIANSHU-BUILD-128", "CERTIFICATE", "GROUP_04", true));
        rules.add(new ManifestEntry("TIANSHU-BUILD-129", "PRODUCT", "GROUP_05", true));
        rules.add(new ManifestEntry("TIANSHU-BUILD-130", "VERSION", "GROUP_05", true));
        rules.add(new ManifestEntry("TIANSHU-BUILD-131", "BUILD", "GROUP_05", true));
        rules.add(new ManifestEntry("TIANSHU-BUILD-132", "COHORT", "GROUP_05", true));
        rules.add(new ManifestEntry("TIANSHU-BUILD-133", "GAME", "GROUP_05", true));
        rules.add(new ManifestEntry("TIANSHU-BUILD-134", "DATABASE", "GROUP_05", true));
        rules.add(new ManifestEntry("TIANSHU-BUILD-135", "SCHEMA", "GROUP_05", true));
        rules.add(new ManifestEntry("TIANSHU-BUILD-136", "FEATURE", "GROUP_05", true));
        rules.add(new ManifestEntry("TIANSHU-BUILD-137", "STATE", "GROUP_05", true));
        rules.add(new ManifestEntry("TIANSHU-BUILD-138", "EXPERT", "GROUP_05", true));
        rules.add(new ManifestEntry("TIANSHU-BUILD-139", "TRANSPORT", "GROUP_05", true));
        rules.add(new ManifestEntry("TIANSHU-BUILD-140", "PARSER", "GROUP_05", true));
        rules.add(new ManifestEntry("TIANSHU-BUILD-141", "KEY", "GROUP_05", true));
        rules.add(new ManifestEntry("TIANSHU-BUILD-142", "SAFE_LOG", "GROUP_05", true));
        rules.add(new ManifestEntry("TIANSHU-BUILD-143", "PIT", "GROUP_05", true));
        rules.add(new ManifestEntry("TIANSHU-BUILD-144", "OWNER", "GROUP_05", true));
        rules.add(new ManifestEntry("TIANSHU-BUILD-145", "NUMBER_P", "GROUP_05", true));
        rules.add(new ManifestEntry("TIANSHU-BUILD-146", "ACTION_P", "GROUP_05", true));
        rules.add(new ManifestEntry("TIANSHU-BUILD-147", "SELECTED_HIT_P", "GROUP_05", true));
        rules.add(new ManifestEntry("TIANSHU-BUILD-148", "LANE", "GROUP_05", true));
        rules.add(new ManifestEntry("TIANSHU-BUILD-149", "BET", "GROUP_05", true));
        rules.add(new ManifestEntry("TIANSHU-BUILD-150", "SETTLEMENT", "GROUP_05", true));
        rules.add(new ManifestEntry("TIANSHU-BUILD-151", "LEDGER", "GROUP_05", true));
        rules.add(new ManifestEntry("TIANSHU-BUILD-152", "SHADOW", "GROUP_05", true));
        rules.add(new ManifestEntry("TIANSHU-BUILD-153", "MODEL_PROOF", "GROUP_05", true));
        rules.add(new ManifestEntry("TIANSHU-BUILD-154", "REALTIME_PROOF", "GROUP_05", true));
        rules.add(new ManifestEntry("TIANSHU-BUILD-155", "PARITY", "GROUP_05", true));
        rules.add(new ManifestEntry("TIANSHU-BUILD-156", "HASH", "GROUP_05", true));
        rules.add(new ManifestEntry("TIANSHU-BUILD-157", "REPLAY", "GROUP_05", true));
        rules.add(new ManifestEntry("TIANSHU-BUILD-158", "OOS", "GROUP_05", true));
        rules.add(new ManifestEntry("TIANSHU-BUILD-159", "CALIBRATION", "GROUP_05", true));
        rules.add(new ManifestEntry("TIANSHU-BUILD-160", "CERTIFICATE", "GROUP_05", true));
        rules.add(new ManifestEntry("TIANSHU-BUILD-161", "PRODUCT", "GROUP_06", true));
        rules.add(new ManifestEntry("TIANSHU-BUILD-162", "VERSION", "GROUP_06", true));
        rules.add(new ManifestEntry("TIANSHU-BUILD-163", "BUILD", "GROUP_06", true));
        rules.add(new ManifestEntry("TIANSHU-BUILD-164", "COHORT", "GROUP_06", true));
        rules.add(new ManifestEntry("TIANSHU-BUILD-165", "GAME", "GROUP_06", true));
        rules.add(new ManifestEntry("TIANSHU-BUILD-166", "DATABASE", "GROUP_06", true));
        rules.add(new ManifestEntry("TIANSHU-BUILD-167", "SCHEMA", "GROUP_06", true));
        rules.add(new ManifestEntry("TIANSHU-BUILD-168", "FEATURE", "GROUP_06", true));
        rules.add(new ManifestEntry("TIANSHU-BUILD-169", "STATE", "GROUP_06", true));
        rules.add(new ManifestEntry("TIANSHU-BUILD-170", "EXPERT", "GROUP_06", true));
        rules.add(new ManifestEntry("TIANSHU-BUILD-171", "TRANSPORT", "GROUP_06", true));
        rules.add(new ManifestEntry("TIANSHU-BUILD-172", "PARSER", "GROUP_06", true));
        rules.add(new ManifestEntry("TIANSHU-BUILD-173", "KEY", "GROUP_06", true));
        rules.add(new ManifestEntry("TIANSHU-BUILD-174", "SAFE_LOG", "GROUP_06", true));
        rules.add(new ManifestEntry("TIANSHU-BUILD-175", "PIT", "GROUP_06", true));
        rules.add(new ManifestEntry("TIANSHU-BUILD-176", "OWNER", "GROUP_06", true));
        rules.add(new ManifestEntry("TIANSHU-BUILD-177", "NUMBER_P", "GROUP_06", true));
        rules.add(new ManifestEntry("TIANSHU-BUILD-178", "ACTION_P", "GROUP_06", true));
        rules.add(new ManifestEntry("TIANSHU-BUILD-179", "SELECTED_HIT_P", "GROUP_06", true));
        rules.add(new ManifestEntry("TIANSHU-BUILD-180", "LANE", "GROUP_06", true));
        return Collections.unmodifiableList(rules);
    }

    public static String fingerprint() {
        StringBuilder sb = new StringBuilder();
        sb.append(VERSION_NAME)
                .append('|')
                .append(BUILD_ID)
                .append('|')
                .append(COHORT_ID)
                .append('|')
                .append(GAME_CODE)
                .append('|')
                .append(DATABASE_NAME)
                .append('|')
                .append(DATABASE_SCHEMA)
                .append('|')
                .append(FEATURE_SCHEMA)
                .append('|')
                .append(STATE_SCHEMA)
                .append('|')
                .append(EXPERT_SCHEMA)
                .append('|')
                .append(TRANSPORT_SCHEMA)
                .append('|')
                .append(PARSER_SCHEMA)
                .append('|')
                .append(SECRET_SCHEMA)
                .append('|')
                .append(CONTRACT_HASH)
                .append('|')
                .append(V51ArtifactBundleManifest.BUNDLE_HASH);
        List<ManifestEntry> list = entries();
        for (int i = 0; i < list.size(); i++) sb.append('|').append(list.get(i).canonical());
        return V51ChecksumEngine.sha256(sb.toString());
    }

    public static String releaseState() {
        if (!ENGINEERING_COMPLETE) return RELEASE_STATE;
        if (!MODEL_PROVEN || !REALTIME_PROVEN) {
            return CANDIDATE_REAL_AUTH_ENABLED && !REAL_BET_HARD_DISABLED
                    ? "RUNTIME_REPAIR_BUILD_MODEL_NOT_PROVEN_CONTROLLED_PROBE"
                    : "RUNTIME_REPAIR_BUILD_MODEL_NOT_PROVEN_SHADOW_ONLY";
        }
        return "FULLY_PROVEN_RELEASE";
    }

    public static boolean isIdentityConsistent() {
        return VERSION_NAME.length() > 0
                && VERSION_CODE > 0
                && GAME_CODE.equals(AppConfig.LOTTERY_CODE)
                && DATABASE_NAME.equals(AppConfig.DB_NAME)
                && AppConfig.DB_VERSION == 36
                && AppConfig.R36_RUNTIME_REPAIR_VERSION.equals(ReleaseManifestR926.VERSION_NAME)
                && AppConfig.R811_SCHEMA_REVISION == 1
                && AppConfig.R815_SCHEMA_REVISION == 3
                && AppConfig.R906_SCHEMA_REVISION == 4
                && AppConfig.TianshuB01.isProductionTransportConfigValid()
                && !MODEL_PROVEN
                && !REALTIME_PROVEN
                && !SHADOW_ONLY
                && !REAL_BET_HARD_DISABLED
                && CANDIDATE_REAL_AUTH_ENABLED
                && !LEGACY_RUNTIME_ENABLED;
    }

    /**
     * Engineering authorization is separate from a statistical MODEL_PROVEN claim. Actual BET still
     * requires the typed V51 policy lane and DecisionGateEngine.
     */
    public static boolean mayAuthorizeRealBet() {
        return ACTIVE_RUNTIME
                && ENGINEERING_COMPLETE
                && CANDIDATE_REAL_AUTH_ENABLED
                && !SHADOW_ONLY
                && !REAL_BET_HARD_DISABLED
                && !LEGACY_RUNTIME_ENABLED
                && GAME_CODE.equals(AppConfig.LOTTERY_CODE);
    }

    public static final String B13_ARTIFACT_GOVERNANCE =
            "B13_UNIQUE_RUNTIME_ARTIFACT_GOVERNANCE_V1";

    public static boolean b13UniqueRuntime() {
        return "V51RuntimeOrchestrator"
                .equals(
                        AutoFetchService.B13_UNIQUE_PRODUCTION_RUNTIME.substring(
                                AutoFetchService.B13_UNIQUE_PRODUCTION_RUNTIME.indexOf("->") + 2));
    }

    public static String b13Governance() {
        return B13_ARTIFACT_GOVERNANCE
                + "|runtime="
                + AutoFetchService.B13_UNIQUE_PRODUCTION_RUNTIME
                + "|bundle="
                + V51ArtifactBundleManifest.BUNDLE_HASH
                + "|release="
                + releaseState();
    }
}
