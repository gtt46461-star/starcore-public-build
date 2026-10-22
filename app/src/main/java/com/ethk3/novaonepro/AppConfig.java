package com.ethk3.novaonepro;

import java.util.Locale;

public final class AppConfig {

    private AppConfig() {}

    public static final String PROJECT_NAME = "Starcore Binance K3 Tianshu";
    public static final String APP_DISPLAY_NAME = "星核·币安K3天枢";
    public static final String VERSION_NAME = V51BuildManifest.VERSION_NAME;
    public static final String STRATEGY_NAME = "STARCORE-TIANSHU-K3-ONE";
    public static final String STRATEGY_VERSION = V51BuildManifest.STRATEGY_VERSION;
    public static final String PACKAGE_NAME = ReleaseManifestR926.APPLICATION_ID;

    /* V50 is the only active realtime chain; earlier strategies are observers. */
    public static final boolean ENABLE_V50_PRIME = false;
    public static final boolean ENABLE_V51_TIANSHU = true;
    public static final boolean LEGACY_READ_ONLY_OBSERVER = true;
    public static final boolean ENABLE_V49_FULL_MOE = false;
    public static final boolean V49_BYPASS_LEGACY_1200_REALTIME = true;
    public static final boolean V49_FULL_INFORMATION_LEARNING = true;
    public static final boolean V49_PREDRAW_FREEZE_REQUIRED = true;
    public static final boolean V49_SINGLE_PROBABILITY_OWNER = true;
    public static final boolean V49_NO_FREQUENCY_DEBT_RESCUE = true;
    public static final int[] V49_REALTIME_WINDOWS = new int[] {6, 12, 24, 48, 96};
    public static final int[] V49_BACKGROUND_WINDOWS = new int[] {180, 360, 720, 1440};
    public static final int V49_HISTORY_LIMIT = 1440;
    public static final int V49_POSITION_ACTION_COUNT = 10;
    public static final int V49_NUMBER_EVENT_COUNT = 100;
    public static final int V49_EXPERT_COUNT = 9;
    public static final int V49_MOTIF_NEIGHBORS = 32;
    public static final int V49_CONTEXT_MAX_ORDER = 4;
    public static final double V49_CORE_MIN_ACTION_P = 0.335d;
    public static final double V49_DYNAMIC_MIN_ACTION_P = 0.315d;
    public static final double V49_PROBE_MIN_ACTION_P = 0.3035d;
    public static final double V49_CORE_MIN_AGREEMENT = 0.58d;
    public static final double V49_DYNAMIC_MIN_AGREEMENT = 0.48d;
    public static final double V49_MAX_SWITCH_RISK = 0.72d;
    public static final double V49_TARGET_COVERAGE = 0.18d;
    public static final double V49_MIN_COVERAGE = 0.15d;
    public static final int V49_COVERAGE_WINDOW = 300;
    public static final int V49_PROBE_MIN_GAP = 8;
    public static final int V49_UI_REASON_PREVIEW_LINES = 10;
    public static final int V49_UI_PAGE_SIZE = 30;
    public static final long V49_REALTIME_P95_TARGET_MS = 1500L;
    public static final long V49_REALTIME_HARD_LIMIT_MS = 3000L;
    public static final long V49_END_TO_END_REDLINE_MS = 5000L;

    public static final class V50 {
        private V50() {}

        public static final int ISSUE_LEDGER_ROWS = 1;
        public static final int POSITION_ACTION_ROWS = 10;
        public static final int NUMBER_EVENT_ROWS = 100;
        public static final int REAL_TRADE_MAX_PER_ISSUE = 1;
        public static final int UI_PAGE_SIZE = 30;
        public static final int UI_PREVIEW_LINES = 10;
        public static final long NUMBER_FEATURE_P95_MS = 300L;
        public static final long STATE_EXPERT_P95_MS = 600L;
        public static final long ACTION_CALIBRATION_P95_MS = 200L;
        public static final long DECISION_GATE_P95_MS = 300L;
        public static final long DATABASE_ATOMIC_P95_MS = 250L;
        public static final long REALTIME_CORE_P95_MS = 2000L;
        public static final long END_TO_END_REDLINE_MS = 5000L;
    }

    /* V46 single-owner realtime contract. V44/V45 identifiers below are read-only compatibility aliases only. */
    public static final String V43_UNIFIED_STRATEGY_VERSION = STRATEGY_VERSION;
    public static final String V43_STRATEGY_EVIDENCE_VERSION = "V44_EVIDENCE_CONTRACT_44157296";
    public static final String V43_ABLATION_VERSION = "V44_ABLATION_AUDIT_44157296";
    public static final String V43_INVARIANT_VERSION = "V44_PIPELINE_INVARIANT_44157296";
    public static final String V43_MODULE_CATALOG_VERSION = "V46_MODULE_CATALOG_46072130";
    public static final String V43_TRACE_VERSION = "V44_EXECUTION_TRACE_44157296";
    public static final int V43_MIN_EVIDENCE_GROUPS = 4;
    public static final double V43_HARD_RISK_LINE = 0.82d;
    public static final double V43_MAX_POSITIVE_PROBABILITY_RESIDUAL = 0.12d;
    public static final double V43_MAX_NEGATIVE_PROBABILITY_RESIDUAL = 0.20d;
    public static final double V43_MAX_EVENT_PROBABILITY_DELTA = 0.030d;
    public static final double V43_MISSING_EVIDENCE_PENALTY = 0.035d;
    public static final double V43_HARD_RISK_PROBABILITY_PENALTY = 0.18d;
    public static final double V43_SAFE_P_MAX_PENALTY = 0.055d;
    public static final double V43_MISSING_EVIDENCE_SAFE_P_PENALTY = 0.015d;
    public static final double V43_HARD_RISK_SAFE_P_PENALTY = 0.070d;
    public static final boolean V43_KEEP_ALL_LEGACY_STRATEGIES = false;
    public static final boolean V43_SINGLE_PROBABILITY_OWNER = true;
    public static final boolean V43_SINGLE_LANE_OWNER = true;
    public static final boolean V43_SINGLE_BET_OWNER = true;

    /* V21.2：AIDE可能改写PackageInfo.versionName。唯一包名与代码批次一致时只记录警告，不误杀正式链。 */
    public static final boolean BUILD_IDENTITY_ALLOW_AIDE_VERSION_WARNING = false;
    public static final boolean BUILD_IDENTITY_REQUIRE_PACKAGE_MATCH = true;
    public static final boolean BUILD_IDENTITY_REQUIRE_CODE_COHORT_MATCH = true;
    public static final boolean SOFT_DATA_ERROR_RECOVER_TO_SHADOW = true;

    public static final String API_PRIMARY_BASE = "https://api.duoqu.online";
    public static final String API_BACKUP_BASE = "https://api1.duoqu.online";
    public static final String API_PRIMARY = API_PRIMARY_BASE;
    public static final String API_BACKUP = API_BACKUP_BASE;
    public static final String LOTTERY_CODE = ReleaseManifestR926.GAME_CODE;

    public static final String API_PATH_LATEST = "/data";
    public static final String API_PATH_HISTORY = "/data";
    public static final String API_PATH_DATA = "/data";

    public static final int HTTP_CONNECT_TIMEOUT_MS = 2500;
    public static final int HTTP_READ_TIMEOUT_MS = 3500;
    public static final int HTTP_MAX_RETRY_PER_LINE = 1;

    public static final boolean SECURITY_HIDE_KEY = true;
    public static final boolean SECURITY_DB_SAVE_KEY = false;
    public static final boolean SECURITY_LOG_KEY = false;
    public static final boolean SECURITY_SHOW_KEY_IN_ERROR = false;
    public static final boolean SECURITY_SAVE_KEY_IN_HISTORY = false;

    public static final String KEY_MASK_TEXT = "***";
    public static final String SAFE_EMPTY_TEXT = "";

    public static final String DB_NAME = ReleaseManifestR926.DATABASE_NAME;
    public static final int DB_VERSION = ReleaseManifestR926.DB_USER_VERSION;
    public static final int DB_VERSION_DEEP_AI = DB_VERSION;

    /**
     * R3.5 integrated repair contract. These switches are intentionally immutable at runtime: model
     * proof cannot auto-unlock REAL money actions.
     */
    public static final String R35_RUNTIME_REPAIR_VERSION = "V57.15.2-R3.5";

    /**
     * R3.6 runtime-closure contract. R3.5 constants remain only for reading legacy rows; every
     * active runtime writer uses the R3.6 contract.
     */
    public static final String R36_RUNTIME_REPAIR_VERSION = ReleaseManifestR926.VERSION_NAME;
    /** Same-user-version additive extension; deliberately not represented by PRAGMA user_version. */
    public static final int R811_SCHEMA_REVISION = ReleaseManifestR926.LEGACY_R811_REVISION;
    /** R815 is a same-DB-version additive migration. It is deliberately separate from user_version. */
    public static final int R815_SCHEMA_REVISION = ReleaseManifestR926.LEGACY_R815_REVISION;
    /** R906 additive same-user_version schema. DB_VERSION deliberately remains 36. */
    public static final int R906_SCHEMA_REVISION = ReleaseManifestR926.LEGACY_R906_REVISION;
    public static final long R906_P0_CORE_SETTLEMENT_BUDGET_MS = 1500L;
    public static final long R906_P0_CANONICAL_P95_TARGET_MS = 5000L;
    public static final long R906_P1_COMPACT_RESEARCH_BUDGET_MS = 350L;
    public static final long R906_P2_DEFERRED_EVIDENCE_BUDGET_MS = 1200L;
    public static final int R906_RESEARCH_NURSERY_MAX = 48;
    public static final int R906_RESEARCH_ACTIVE_MAX = 96;
    public static final int R906_RESEARCH_CHALLENGER_MAX = 16;
    public static final String R906_COMPACT_FEATURE_SCHEMA = "R906_COMPACT_FEATURE_SCHEMA_V1";
    public static final int R815_MIN_CANONICAL_HISTORY = 12;
    public static final int R815_MIN_TRANSITION_HISTORY = 24;
    public static final int R815_LONG_CONTEXT_HISTORY = 360;
    public static final int R815_FULL_CONTEXT_HISTORY = 720;
    public static final int R815_HISTORY_RETENTION_MIN = 1440;
    /** R915 production remains statistically unproven, but an exact engineering-candidate lane
     * may create one internally tracked, fixed-K3 REAL ledger row after every typed safety check. */
    public static final boolean R815_SHADOW_ONLY = false;
    public static final boolean R815_MODEL_PROVEN = false;
    public static final boolean R815_REAL_BET_HARD_DISABLED = false;

    public static final long R36_ANALYSIS_TARGET_MS = 5000L;
    public static final long R36_NO_PROGRESS_CANCEL_MS = 30000L;
    public static final long R36_ANALYSIS_LEASE_MS = 45000L;
    public static final long R36_DEFERRED_LEASE_MS = 20000L;
    public static final long R36_SETTLEMENT_LEASE_MS = 15000L;
    public static final int R36_DEFERRED_MAX_ATTEMPTS = 5;
    public static final int R36_SETTLEMENT_MAX_ATTEMPTS = 5;
    public static final int R36_ANALYSIS_MAX_ATTEMPTS = 3;
    /** Four bounded action transactions instead of ten repeated snapshot decodes per issue. */
    /* R817 keeps the immutable 1200-action universe, but materializes it in ten small
     * transactions.  The former 300-row slice caused large Cursor/ContentValues bursts on
     * 256/512 MiB devices and repeatedly competed with the live analysis writer. */
    public static final int R36_DEFERRED_ACTION_CHUNK = 120;
    public static final int R36_SNAPSHOT_SCHEMA = 1;
    public static final int R36_SNAPSHOT_MAX_BYTES = 1572864;
    public static final long R811_LIVE_QUIET_MS = 5000L;
    public static final long R811_RESEARCH_SLICE_BUDGET_MS = 250L;
    public static final int R811_RESEARCH_MAX_CANDIDATES_PER_SLICE = 8;
    public static final int R811_RESEARCH_MAX_SETTLEMENT_LAG = 1;
    public static final int R811_RESEARCH_MAX_DEFERRED_PENDING = 2;
    public static final long R811_RESEARCH_PAUSE_P95_MS = 4000L;
    public static final long R811_CLOCK_SKEW_THRESHOLD_MS = 300000L;
    public static final boolean R36_REAL_BET_UNLOCK = false;
    public static final boolean R35_ANALYSIS_SINGLE_FLIGHT_ENABLED = true;
    public static final boolean R35_BUSINESS_RECONCILE_ENABLED = true;
    public static final boolean R35_DEFERRED_EVIDENCE_ENABLED = true;
    public static final boolean R35_FAST_EXACT_SETTLEMENT_ENABLED = true;
    public static final boolean R35_STRICT_OOS_AFTER_DRAW_ENABLED = true;
    public static final boolean R35_REAL_AUTO_UNLOCK_ENABLED = false;
    public static final long R35_REALTIME_TARGET_MS = 5000L;
    public static final long R35_HISTORY_BUDGET_MS = 350L;
    public static final long R35_FEATURE_BUDGET_MS = 900L;
    public static final long R35_NUMBER_MODEL_BUDGET_MS = 800L;
    public static final long R35_ACTION_1200_RANK_BUDGET_MS = 1200L;
    public static final long R35_SELECTION_GATE_BUDGET_MS = 700L;
    public static final long R35_CORE_PERSIST_BUDGET_MS = 350L;
    public static final int R35_PROMOTION_MIN_OOS_SAMPLE = 120;
    public static final int R35_RECENT_CANONICAL_WINDOW = 30;
    public static final int R35_RECENT_CANONICAL_MIN = 29;
    public static final int R35_DEFERRED_QUEUE_CAPACITY = 8;

    public static final int FEATURE_VERSION = 20;
    public static final int REASON_VERSION = 20;
    public static final int CONTRAST_VERSION = 20;
    public static final int CAUSAL_VERSION = 20;
    public static final int K3_ROLE_VERSION = 20;
    public static final int ELITE45_VERSION = 20;
    public static final int V20_CAUSE_CHAIN_VERSION = 20;

    public static final boolean ENABLE_PREDRAW_REASON = true;
    public static final boolean ENABLE_PREDRAW_REASON_SAMPLE = true;
    public static final boolean ENABLE_CONTRASTIVE_REASON = true;
    public static final boolean ENABLE_CAUSAL_REASON_MATRIX = true;
    public static final boolean ENABLE_K3_ROLE_COMBO = true;
    public static final boolean ENABLE_ELITE45_MODE = true;
    public static final boolean ENABLE_ELITE45_AUDIT = true;
    public static final boolean ENABLE_FALSE_STRONG_COOLDOWN = true;
    public static final boolean ENABLE_SAME_FINGERPRINT_COOLING = true;
    public static final boolean ENABLE_REASON_FORWARD_VALIDATE = true;
    public static final boolean ENABLE_REASON_WILSON_RATING = true;
    public static final boolean ENABLE_REASON_COMBO_CAUSAL = true;
    public static final boolean ENABLE_PREDRAW_REASON_TEXT = true;

    public static final boolean ENABLE_V20_DYNAMIC_CAUSE_CHAIN = false;
    public static final boolean ENABLE_V20_FULL_CANDIDATE_SCAN = false;
    public static final boolean ENABLE_V20_PREDRAW_FEATURE = false;
    public static final boolean ENABLE_V20_CAUSAL_FACTOR_MATRIX = false;
    public static final boolean ENABLE_V20_POSITION_CAUSE_MATRIX = false;
    public static final boolean ENABLE_V20_TEACHER_NUMBER_MATRIX = false;
    public static final boolean ENABLE_V20_RANK_COUNTERFACTUAL = false;
    public static final boolean ENABLE_V20_THIRD_GUARD_MATRIX = false;
    public static final boolean ENABLE_V20_RULE_HEALTH_MATRIX = false;
    public static final boolean ENABLE_V20_SHADOW_HEALTH_GATE = false;
    public static final boolean ENABLE_V20_FALSE_STRONG_FREEZE = false;
    public static final boolean ENABLE_V20_SEED_CAUSE_EXPANSION = false;
    public static final boolean ENABLE_V20_SAME_CLASS_FREEZE = false;
    public static final boolean ENABLE_V20_GLOBAL_FUSE = false;
    public static final boolean ENABLE_V20_HEALTHY_VOLUME_MODE = false;
    public static final boolean ENABLE_V20_HIGH_ACCURACY_MODE = false;

    public static final boolean STRICT_NO_AUTO_PROMOTE_BET_TO_REAL_TRADE = true;
    public static final boolean STRICT_REAL_STATS_REQUIRE_REALTIME_SOURCE = true;
    public static final boolean STRICT_REAL_STATS_REQUIRE_REAL_TRADE_SCOPE = true;
    public static final boolean STRICT_REAL_STATS_REQUIRE_SETTLED = true;
    public static final boolean STRICT_REAL_STATS_REQUIRE_IS_REALTIME_REAL = true;
    public static final boolean STRICT_PREDRAW_REASON_NO_REWRITE_AFTER_SETTLE = true;
    public static final boolean STRICT_ELITE45_ONLY_CAN_REAL_BET = false;
    public static final boolean STRICT_ALL_RUNTIME_SIGNAL_MUST_PASS_ELITE45 = false;

    public static final int MIN_POSITION = 1;
    public static final int MAX_POSITION = 10;
    public static final int MIN_NUMBER = 1;
    public static final int MAX_NUMBER = 10;
    public static final int NUMBER_COUNT = 10;
    public static final int POSITION_COUNT = 10;

    public static final int FIXED_K = 3;
    public static final int TOTAL_K3_COMBO_PER_POSITION = 120;
    public static final int TOTAL_CANDIDATES_PER_ISSUE = 1200;
    public static final int MAX_CANDIDATES_PER_ISSUE = TOTAL_CANDIDATES_PER_ISSUE;
    public static final int TOP_RANK_AUDIT = 18;
    public static final int RANK_COUNTERFACTUAL_TOP_N = TOP_RANK_AUDIT;

    public static final boolean ALLOW_K4 = false;
    public static final boolean ALLOW_DYNAMIC_K = false;
    public static final boolean ALLOW_MULTI_POSITION_BET = false;
    public static final boolean ALLOW_MARTINGALE = false;
    public static final boolean ALLOW_DOUBLE_BET = false;
    public static final boolean ALLOW_CHASE_BET = false;
    public static final boolean ALLOW_MULTI_ISSUE_CHASE = false;
    public static final boolean ALLOW_POSITION_SPLIT_BET = false;
    public static final boolean ALLOW_AUTO_INCREASE_STAKE = false;

    public static final boolean STRICT_SINGLE_POSITION_SETTLEMENT = true;
    public static final boolean STRICT_SINGLE_POSITION_SETTLE = STRICT_SINGLE_POSITION_SETTLEMENT;
    public static final boolean STRICT_K3_ONLY = true;
    public static final boolean STRICT_REAL_TRADE_ONLY_FOR_REAL_STATS = true;
    public static final boolean STRICT_NO_FULL_DRAW_SCAN_AS_HIT = true;
    public static final boolean STRICT_DECISION_GATE_ONLY_CAN_BET = true;
    public static final boolean STRICT_TEACHER_NEVER_BET_DIRECTLY = true;
    public static final boolean REQUIRE_DECISION_GATE = true;
    public static final boolean STRICT_SHADOW_NOT_COUNT_REAL_STATS = true;
    public static final boolean STRICT_WALK_FORWARD_NO_FUTURE = true;
    public static final boolean STRICT_B_GRADE_REQUIRE_SHADOW_PROMOTION = false;
    public static final boolean STRICT_C_GRADE_NEVER_BET = true;

    public static final double RANDOM_K3_BASELINE = 0.300d;
    public static final double K3_BASE_PROBABILITY = RANDOM_K3_BASELINE;
    public static final double ODDS = 9.9d;
    public static final double ODDS_K3 = ODDS;
    public static final double K3_ODDS = ODDS;
    public static final double COST_PER_NUMBER = 1.0d;
    public static final double UNIT_STAKE = 1.0d;
    public static final double BET_COST = 3.0d;
    public static final double BET_COST_K3 = BET_COST;
    public static final double COST_PER_BET = BET_COST;
    public static final double WIN_NET_PROFIT = 6.9d;
    public static final double WIN_PNL_K3 = WIN_NET_PROFIT;
    public static final double LOSE_NET_PROFIT = -3.0d;
    public static final double MISS_PNL_K3 = LOSE_NET_PROFIT;
    public static final double WIN_PROFIT = WIN_NET_PROFIT;
    public static final double MISS_LOSS = 3.0d;
    public static final double BREAK_EVEN_PROBABILITY = BET_COST / ODDS;

    /*
     * V20 重型计算升级主链闭环修复 · 第1/22 AppConfig 总配置
     *
     * 核心原则：
     * 1. 固定的不是窗口分类，而是特征原子和风控红线。
     * 2. 窗口类别由后台动态聚类自动生成、合并、拆分、晋级、冻结、淘汰。
     * 3. 每期必须学习 10位置 x 10号码 的正负样本。
     * 4. 实时只做快匹配，后台慢学习，不阻塞出号。
     * 5. 任何低于随机基准、样本不足、近期失效、反证偏高、字段不完整的窗口不得真实BET。
     * 6. DecisionGateEngine 仍是唯一真实BET出口。
     */
    public static final String V20_DYNAMIC_CAUSAL_WINDOW_VERSION =
            "V47_CROSSFIT_NUMBER_CHAMPION_FINAL40_FULL_DIAGNOSIS_20260721_47072147";
    public static final int V20_DYNAMIC_CAUSAL_WINDOW_CONFIG_VERSION = 22;
    public static final int V20_DYNAMIC_CAUSAL_WINDOW_DB_VERSION = 10;

    public static final boolean ENABLE_V20_DYNAMIC_CAUSAL_WINDOW_SYSTEM = false;
    public static final boolean ENABLE_DYNAMIC_CAUSAL_WINDOW_SYSTEM =
            ENABLE_V20_DYNAMIC_CAUSAL_WINDOW_SYSTEM;
    public static final boolean ENABLE_DYNAMIC_WINDOW_CLUSTER = true;
    public static final boolean ENABLE_DYNAMIC_WINDOW_AUTO_CLUSTER = true;
    public static final boolean ENABLE_DYNAMIC_WINDOW_AUTO_MERGE = true;
    public static final boolean ENABLE_DYNAMIC_WINDOW_AUTO_SPLIT = true;
    public static final boolean ENABLE_DYNAMIC_WINDOW_AUTO_PROMOTE = true;
    public static final boolean ENABLE_DYNAMIC_WINDOW_AUTO_FREEZE = true;
    public static final boolean ENABLE_DYNAMIC_WINDOW_AUTO_DEAD = true;
    public static final boolean ENABLE_DYNAMIC_WINDOW_SNAPSHOT = true;
    public static final boolean ENABLE_DYNAMIC_WINDOW_WALK_FORWARD_VERIFY = true;
    public static final boolean ENABLE_DYNAMIC_WINDOW_RECENT_VALIDITY = true;
    public static final boolean ENABLE_DYNAMIC_WINDOW_BASELINE_GUARD = true;
    public static final boolean ENABLE_DYNAMIC_WINDOW_MAX_LOSE_GUARD = true;
    public static final boolean ENABLE_DYNAMIC_WINDOW_VOLATILITY_GUARD = true;
    public static final boolean ENABLE_DYNAMIC_WINDOW_COUNTER_GUARD = true;
    public static final boolean ENABLE_DYNAMIC_WINDOW_NO_FUTURE = true;

    public static final boolean ENABLE_NEED_SUPPLY_MATRIX = true;
    public static final boolean ENABLE_POSITION_NEED_MATRIX = true;
    public static final boolean ENABLE_NUMBER_SUPPLY_MATRIX = true;
    public static final boolean ENABLE_NEED_SUPPLY_MATCH_MATRIX = true;
    public static final boolean ENABLE_K3_ROLE_STRUCTURE_MATRIX = true;
    public static final boolean ENABLE_THIRD_NUMBER_ROLE_GUARD = true;

    public static final boolean ENABLE_COUNTERFACTUAL_REASON_ENGINE = true;
    public static final boolean ENABLE_COUNTERFACTUAL_POSITION_COMPARE = true;
    public static final boolean ENABLE_COUNTERFACTUAL_NUMBER_COMPARE = true;
    public static final boolean ENABLE_COUNTERFACTUAL_K3_COMPARE = true;
    public static final boolean ENABLE_CAUSE_ABLATION_MATRIX = true;
    public static final boolean ENABLE_MISS_REASON_MATRIX = true;
    public static final boolean ENABLE_NEGATIVE_MIRROR_WINDOW = true;
    public static final boolean ENABLE_FALSE_CAUSE_FILTER = true;

    public static final boolean ENABLE_ADAPTIVE_JUDGE_REALTIME_CANDIDATES = true;
    public static final boolean ENABLE_ADAPTIVE_JUDGE_CLUSTER_INPUT = true;
    public static final boolean ENABLE_ADAPTIVE_JUDGE_NEED_SUPPLY_INPUT = true;
    public static final boolean ENABLE_ADAPTIVE_JUDGE_COUNTERFACTUAL_INPUT = true;
    public static final boolean ENABLE_ADAPTIVE_JUDGE_FREEZE_INPUT = true;
    public static final boolean ENABLE_ADAPTIVE_JUDGE_PROMOTION_INPUT = true;
    public static final boolean ENABLE_ADAPTIVE_JUDGE_OUTPUT_TO_DECISION = false;

    public static final boolean ENABLE_SHADOW_DYNAMIC_CLUSTER_PROMOTION = true;
    public static final boolean ENABLE_SHADOW_FULL_FIELD_SAVE = true;
    public static final boolean ENABLE_SHADOW_WINDOW_DOMAIN_SAVE = true;
    public static final boolean ENABLE_SHADOW_CLUSTER_FIELD_SAVE = true;
    public static final boolean ENABLE_SHADOW_NEED_SUPPLY_SAVE = true;
    public static final boolean ENABLE_SHADOW_COUNTERFACTUAL_SAVE = true;
    public static final boolean ENABLE_SHADOW_ADAPTIVE_JUDGE_SAVE = true;

    public static final boolean ENABLE_POS_NEG_NUMBER_SAMPLE_PER_DRAW = true;
    public static final boolean ENABLE_POSITION_NEED_SAMPLE_PER_DRAW = true;
    public static final boolean ENABLE_NUMBER_SUPPLY_SAMPLE_PER_DRAW = true;
    public static final boolean ENABLE_K3_COUNTERFACTUAL_SAMPLE_PER_DRAW = true;
    public static final boolean ENABLE_CLUSTER_SAMPLE_PER_CANDIDATE = true;

    public static final boolean STRICT_DYNAMIC_CLUSTER_REQUIRED_FOR_BET = false;
    public static final boolean STRICT_DYNAMIC_CLUSTER_NO_BET_WHEN_NEW = false;
    public static final boolean STRICT_DYNAMIC_CLUSTER_NO_BET_WHEN_WATCH_ONLY = false;
    public static final boolean STRICT_DYNAMIC_CLUSTER_NO_BET_WHEN_BELOW_BASELINE = true;
    public static final boolean STRICT_DYNAMIC_CLUSTER_NO_BET_WHEN_SAMPLE_WEAK = false;
    public static final boolean STRICT_DYNAMIC_CLUSTER_NO_BET_WHEN_RECENT_WEAK = true;
    public static final boolean STRICT_DYNAMIC_CLUSTER_NO_BET_WHEN_MAX_LOSE_HIGH = true;
    public static final boolean STRICT_NEED_SUPPLY_REQUIRED_FOR_BET = false;
    public static final boolean STRICT_COUNTERFACTUAL_REQUIRED_FOR_BET = false;
    public static final boolean STRICT_ADAPTIVE_JUDGE_REQUIRED_FOR_BET = false;
    public static final boolean STRICT_ADAPTIVE_JUDGE_MUST_SEE_CANDIDATES = false;
    public static final boolean STRICT_FIELD_SCORE_NOT_WIN_RATE = true;
    public static final boolean STRICT_COVER_SCORE_NOT_WIN_RATE = true;
    public static final boolean STRICT_NO_FAKE_STRONG_DOMAIN_BET = true;

    public static final String CLUSTER_STATUS_NEW = "NEW";
    public static final String CLUSTER_STATUS_WATCH = "WATCH";
    public static final String CLUSTER_STATUS_OBSERVE_READY = "OBSERVE_READY";
    public static final String CLUSTER_STATUS_READY = "READY";
    public static final String CLUSTER_STATUS_STRONG = "STRONG";
    public static final String CLUSTER_STATUS_RISK = "RISK";
    public static final String CLUSTER_STATUS_FREEZE = "FREEZE";
    public static final String CLUSTER_STATUS_DEAD = "DEAD";
    public static final String CLUSTER_STATUS_SAMPLE_WEAK = "SAMPLE_WEAK";
    public static final String CLUSTER_STATUS_RECENT_WEAK = "RECENT_WEAK";
    public static final String CLUSTER_STATUS_UNKNOWN = "UNKNOWN";
    public static final String CLUSTER_STATUS_WEAK = "WEAK";

    public static final String ADAPTIVE_ACTION_PASS = "PASS";
    public static final String ADAPTIVE_ACTION_WATCH = "WATCH";
    public static final String ADAPTIVE_ACTION_READY = "READY";
    public static final String ADAPTIVE_ACTION_STRONG = "STRONG";
    public static final String ADAPTIVE_ACTION_DOWNGRADE = "DOWNGRADE";
    public static final String ADAPTIVE_ACTION_RISK = "RISK";
    public static final String ADAPTIVE_ACTION_FREEZE = "FREEZE";
    public static final String ADAPTIVE_ACTION_FAKE = "FAKE";
    public static final String ADAPTIVE_ACTION_HOLD = "HOLD";
    public static final String ADAPTIVE_ACTION_PROMOTE = "PROMOTE";

    public static final String K3_ROLE_MAIN_CAUSE = "MAIN_CAUSE";
    public static final String K3_ROLE_ASSIST_CAUSE = "ASSIST_CAUSE";
    public static final String K3_ROLE_DEFENSE_CAUSE = "DEFENSE_CAUSE";
    public static final String K3_ROLE_FILLER = "FILLER";
    public static final String K3_ROLE_DRAG = "DRAG";
    public static final String DYNAMIC_K3_ROLE_UNKNOWN = "ROLE_UNKNOWN";

    public static final String NEED_TYPE_HOT_CONTINUE = "HOT_CONTINUE";
    public static final String NEED_TYPE_COLD_REPAIR = "COLD_REPAIR";
    public static final String NEED_TYPE_OMISSION_RETURN = "OMISSION_RETURN";
    public static final String NEED_TYPE_HIGH_LOW_BALANCE = "HIGH_LOW_BALANCE";
    public static final String NEED_TYPE_ODD_EVEN_BALANCE = "ODD_EVEN_BALANCE";
    public static final String NEED_TYPE_TAIL_REPAIR = "TAIL_REPAIR";
    public static final String NEED_TYPE_NEIGHBOR_RELAY = "NEIGHBOR_RELAY";
    public static final String NEED_TYPE_CROSS_POSITION_FLOW = "CROSS_POSITION_FLOW";
    public static final String NEED_TYPE_CROWD_RELEASE = "CROWD_RELEASE";
    public static final String NEED_TYPE_REVERSE_BREAK = "REVERSE_BREAK";
    public static final String NEED_TYPE_STABLE_REPEAT = "STABLE_REPEAT";
    public static final String NEED_TYPE_CHAOS_AVOID = "CHAOS_AVOID";

    public static final String MISS_REASON_WINDOW_WRONG = "WINDOW_WRONG";
    public static final String MISS_REASON_POSITION_WRONG = "POSITION_WRONG";
    public static final String MISS_REASON_MAIN_NUMBER_WRONG = "MAIN_NUMBER_WRONG";
    public static final String MISS_REASON_ASSIST_NUMBER_WRONG = "ASSIST_NUMBER_WRONG";
    public static final String MISS_REASON_THIRD_DRAG = "THIRD_DRAG";
    public static final String MISS_REASON_TREND_EARLY = "TREND_EARLY";
    public static final String MISS_REASON_TREND_LATE = "TREND_LATE";
    public static final String MISS_REASON_OMISSION_NOT_RELEASE = "OMISSION_NOT_RELEASE";
    public static final String MISS_REASON_HOT_REVERSE = "HOT_REVERSE";
    public static final String MISS_REASON_COLD_KEEP_COLD = "COLD_KEEP_COLD";
    public static final String MISS_REASON_TAIL_RELAY_FAIL = "TAIL_RELAY_FAIL";
    public static final String MISS_REASON_NEIGHBOR_RELAY_FAIL = "NEIGHBOR_RELAY_FAIL";
    public static final String MISS_REASON_CROSS_POSITION_FAIL = "CROSS_POSITION_FAIL";
    public static final String MISS_REASON_COUNTER_UNDER_ESTIMATED = "COUNTER_UNDER_ESTIMATED";
    public static final String MISS_REASON_CLUSTER_TOO_COARSE = "CLUSTER_TOO_COARSE";
    public static final String MISS_REASON_RECENT_INVALID = "RECENT_INVALID";
    public static final String MISS_REASON_FAKE_STRONG = "FAKE_STRONG";
    public static final String MISS_REASON_UNKNOWN = "MISS_UNKNOWN";
    public static final String MISS_REASON_COUNTER_IGNORED = "COUNTER_IGNORED";

    public static final double DYNAMIC_CLUSTER_RANDOM_BASELINE = RANDOM_K3_BASELINE;
    public static final double RANDOM_NUMBER_BASELINE = 0.100d;
    public static final double DYNAMIC_CLUSTER_FREEZE_HIT_RATE = 0.300d;
    public static final double DYNAMIC_CLUSTER_WATCH_HIT_RATE = 0.340d;
    public static final double DYNAMIC_CLUSTER_OBSERVE_READY_HIT_RATE = 0.360d;
    public static final double DYNAMIC_CLUSTER_READY_HIT_RATE = 0.370d;
    public static final double DYNAMIC_CLUSTER_STRONG_HIT_RATE = 0.400d;
    public static final double DYNAMIC_CLUSTER_ELITE_HIT_RATE = 0.420d;
    public static final double DYNAMIC_CLUSTER_MIN_LIFT_READY = 0.070d;
    public static final double DYNAMIC_CLUSTER_MIN_LIFT_STRONG = 0.100d;
    public static final double DYNAMIC_CLUSTER_RECENT_READY_HIT_RATE = 0.365d;
    public static final double DYNAMIC_CLUSTER_RECENT_STRONG_HIT_RATE = 0.390d;
    public static final double DYNAMIC_CLUSTER_RECENT_FREEZE_HIT_RATE = 0.300d;
    public static final double DYNAMIC_CLUSTER_MIN_CONFIDENCE_READY = 0.420d;
    public static final double DYNAMIC_CLUSTER_MIN_CONFIDENCE_STRONG = 0.560d;
    public static final double DYNAMIC_CLUSTER_MIN_CONFIDENCE_ELITE = 0.660d;
    public static final double DYNAMIC_CLUSTER_MAX_VOLATILITY_READY = 0.220d;
    public static final double DYNAMIC_CLUSTER_MAX_VOLATILITY_STRONG = 0.180d;
    public static final double DYNAMIC_CLUSTER_MAX_COUNTER_READY = 0.560d;
    public static final double DYNAMIC_CLUSTER_MAX_COUNTER_STRONG = 0.480d;
    public static final double DYNAMIC_CLUSTER_MAX_DISTANCE_MATCH = 0.360d;
    public static final double DYNAMIC_CLUSTER_MAX_DISTANCE_READY = 0.300d;
    public static final double DYNAMIC_CLUSTER_MAX_DISTANCE_STRONG = 0.240d;
    public static final double DYNAMIC_CLUSTER_MERGE_DISTANCE = 0.180d;
    public static final double DYNAMIC_CLUSTER_SPLIT_HIT_DIFF = 0.120d;
    public static final double DYNAMIC_CLUSTER_SPLIT_POSITION_DIFF = 0.100d;
    public static final double DYNAMIC_CLUSTER_DEAD_HIT_RATE = 0.260d;

    public static final int DYNAMIC_CLUSTER_MIN_SAMPLE_NEW = 8;
    public static final int DYNAMIC_CLUSTER_MIN_SAMPLE_WATCH = 12;
    public static final int DYNAMIC_CLUSTER_MIN_SAMPLE_READY = 30;
    public static final int DYNAMIC_CLUSTER_MIN_SAMPLE_STRONG = 80;
    public static final int DYNAMIC_CLUSTER_MIN_SAMPLE_ELITE = 120;
    public static final int DYNAMIC_CLUSTER_RECENT_MIN_SAMPLE = 12;
    public static final int DYNAMIC_CLUSTER_RECENT_STRONG_SAMPLE = 30;
    public static final int DYNAMIC_CLUSTER_MAX_LOSE_READY = 6;
    public static final int DYNAMIC_CLUSTER_MAX_LOSE_STRONG = 5;
    public static final int DYNAMIC_CLUSTER_FREEZE_LOSE = 7;
    public static final int DYNAMIC_CLUSTER_HARD_FREEZE_LOSE = 9;
    public static final int DYNAMIC_CLUSTER_DEAD_MIN_SAMPLE = 60;
    public static final int DYNAMIC_CLUSTER_MAX_ACTIVE = 360;
    public static final int DYNAMIC_CLUSTER_MAX_TOTAL = 1200;
    public static final int DYNAMIC_CLUSTER_MATCH_TOP_N = 5;
    public static final int DYNAMIC_CLUSTER_REALTIME_MATCH_TOP_N = 3;
    public static final int DYNAMIC_CLUSTER_BACKTEST_MATCH_TOP_N = 8;

    public static final int DYNAMIC_CAUSAL_WINDOW_12 = 12;
    public static final int DYNAMIC_CAUSAL_WINDOW_24 = 24;
    public static final int DYNAMIC_CAUSAL_WINDOW_48 = 48;
    public static final int DYNAMIC_CAUSAL_WINDOW_96 = 96;
    public static final int DYNAMIC_CAUSAL_WINDOW_360 = 360;
    public static final int[] DYNAMIC_CAUSAL_WINDOWS =
            new int[] {
                DYNAMIC_CAUSAL_WINDOW_12,
                DYNAMIC_CAUSAL_WINDOW_24,
                DYNAMIC_CAUSAL_WINDOW_48,
                DYNAMIC_CAUSAL_WINDOW_96,
                DYNAMIC_CAUSAL_WINDOW_360
            };

    public static final double NEED_SUPPLY_MIN_MATCH_SCORE = 0.540d;
    public static final double NEED_SUPPLY_READY_MATCH_SCORE = 0.620d;
    public static final double NEED_SUPPLY_STRONG_MATCH_SCORE = 0.700d;
    public static final double NEED_SUPPLY_MIN_MAIN_ROLE_SCORE = 0.580d;
    public static final double NEED_SUPPLY_MIN_ASSIST_ROLE_SCORE = 0.500d;
    public static final double NEED_SUPPLY_MIN_DEFENSE_ROLE_SCORE = 0.420d;
    public static final double NEED_SUPPLY_MIN_THIRD_QUALITY = 0.100d;
    public static final double NEED_SUPPLY_READY_THIRD_QUALITY = 0.125d;
    public static final double NEED_SUPPLY_STRONG_THIRD_QUALITY = 0.150d;
    public static final double NEED_SUPPLY_MAX_SUPPLY_RISK = 0.560d;
    public static final double NEED_SUPPLY_HARD_RISK = 0.700d;
    public static final double NEED_SUPPLY_ROLE_DIVERSITY_MIN = 0.420d;
    public static final double NEED_SUPPLY_ROLE_DIVERSITY_STRONG = 0.600d;
    public static final double NEED_SUPPLY_FILLER_PENALTY = 0.080d;
    public static final double NEED_SUPPLY_THIRD_DRAG_PENALTY = 0.120d;

    public static final double COUNTERFACTUAL_MIN_RANK_SCORE = 0.540d;
    public static final double COUNTERFACTUAL_READY_RANK_SCORE = 0.620d;
    public static final double COUNTERFACTUAL_STRONG_RANK_SCORE = 0.700d;
    public static final double COUNTERFACTUAL_MAX_WHY_NOT_RISK = 0.560d;
    public static final double COUNTERFACTUAL_HARD_WHY_NOT_RISK = 0.700d;
    public static final int COUNTERFACTUAL_TOP_POSITION_COMPARE = 10;
    public static final int COUNTERFACTUAL_TOP_NUMBER_COMPARE = 10;
    public static final int COUNTERFACTUAL_TOP_K3_COMPARE = 18;
    public static final int COUNTERFACTUAL_TOP_REASON_KEEP = 8;
    public static final int CAUSE_ABLATION_MIN_SAMPLE = 24;
    public static final int CAUSE_ABLATION_READY_SAMPLE = 60;
    public static final double CAUSE_ABLATION_MIN_DROP = 0.020d;
    public static final double CAUSE_ABLATION_STRONG_DROP = 0.050d;
    public static final double CAUSE_ABLATION_FALSE_CAUSE_DROP = 0.005d;
    public static final double CAUSE_ABLATION_MIN_SCORE = CAUSE_ABLATION_STRONG_DROP;
    public static final double NEGATIVE_MIRROR_MAX_SIMILARITY = 0.820d;
    public static final double NEGATIVE_MIRROR_HARD_RISK = 0.680d;

    public static final int DYNAMIC_CAUSAL_BACKGROUND_BATCH_DRAWS = 80;
    public static final int DYNAMIC_CAUSAL_BACKGROUND_BATCH_DRAWS_MAX = 120;
    public static final int DYNAMIC_CAUSAL_BACKGROUND_MIN_HISTORY = 240;
    public static final int DYNAMIC_CAUSAL_BACKGROUND_MAX_HISTORY = 5000;
    public static final int DYNAMIC_CAUSAL_BACKGROUND_TARGETS_PER_BATCH = 8;
    public static final int DYNAMIC_CAUSAL_BACKGROUND_MAX_CANDIDATE_SAMPLE = 180;
    public static final int DYNAMIC_CAUSAL_BACKGROUND_MAX_NUMBER_SAMPLE =
            POSITION_COUNT * NUMBER_COUNT;
    public static final long DYNAMIC_CAUSAL_BACKGROUND_INTERVAL_MS = 45L * 1000L;
    public static final long DYNAMIC_CAUSAL_BACKGROUND_IDLE_INTERVAL_MS = 75L * 1000L;
    public static final long DYNAMIC_CAUSAL_BACKGROUND_MAX_SINGLE_BATCH_MS = 9000L;
    public static final long DYNAMIC_CAUSAL_BACKGROUND_COOPERATIVE_SLEEP_MS = 3L;
    public static final boolean DYNAMIC_CAUSAL_BACKGROUND_NEVER_BLOCK_REALTIME = true;

    public static final int DYNAMIC_CAUSAL_REALTIME_FEATURE_MS = 320;
    public static final int DYNAMIC_CAUSAL_REALTIME_NEED_SUPPLY_MS = 360;
    public static final int DYNAMIC_CAUSAL_REALTIME_CLUSTER_MATCH_MS = 360;
    public static final int DYNAMIC_CAUSAL_REALTIME_COUNTERFACTUAL_MS = 420;
    public static final int DYNAMIC_CAUSAL_REALTIME_ADAPTIVE_JUDGE_MS = 320;
    public static final int DYNAMIC_CAUSAL_REALTIME_TOTAL_SOFT_MS = 1400;
    public static final int DYNAMIC_CAUSAL_REALTIME_TOTAL_HARD_MS = 2200;
    public static final int DYNAMIC_CAUSAL_REALTIME_CANDIDATE_LIMIT = 40;
    public static final int DYNAMIC_CAUSAL_REALTIME_DEEP_CANDIDATE_LIMIT = 18;
    public static final int DYNAMIC_CAUSAL_REALTIME_CLUSTER_READ_LIMIT = 60;
    public static final int DYNAMIC_CAUSAL_REALTIME_SNAPSHOT_TTL_MS = 60 * 1000;

    public static final String TABLE_DYNAMIC_WINDOW_CLUSTER = "dynamic_window_cluster";
    public static final String TABLE_WINDOW_FEATURE_SAMPLE = "window_feature_sample";
    public static final String TABLE_POSITION_NEED_SAMPLE = "position_need_sample";
    public static final String TABLE_NUMBER_SUPPLY_SAMPLE = "number_supply_sample";
    public static final String TABLE_NEED_SUPPLY_MATCH_SAMPLE = "need_supply_match_sample";
    public static final String TABLE_COUNTERFACTUAL_REASON_SAMPLE = "counterfactual_reason_sample";
    public static final String TABLE_CAUSE_ABLATION_SAMPLE = "cause_ablation_sample";
    public static final String TABLE_MISS_REASON_SAMPLE = "miss_reason_sample";
    public static final String TABLE_ADAPTIVE_JUDGE_SAMPLE = "adaptive_judge_sample";
    public static final String TABLE_PROMOTION_DOMAIN_SNAPSHOT = "promotion_domain_snapshot";
    public static final String TABLE_DYNAMIC_CAUSAL_STATE = "dynamic_causal_state";

    public static final String STATE_DYNAMIC_CLUSTER_SNAPSHOT_VERSION =
            "dynamic_cluster_snapshot_version";
    public static final String STATE_DYNAMIC_CLUSTER_LAST_RUN = "dynamic_cluster_last_run";
    public static final String STATE_DYNAMIC_CLUSTER_SUMMARY = "dynamic_cluster_summary";
    public static final String STATE_NEED_SUPPLY_LAST_RUN = "need_supply_last_run";
    public static final String STATE_COUNTERFACTUAL_LAST_RUN = "counterfactual_last_run";
    public static final String STATE_ADAPTIVE_JUDGE_CANDIDATE_COUNT =
            "adaptive_judge_candidate_count";
    public static final String STATE_ADAPTIVE_JUDGE_REALTIME_SUMMARY =
            "adaptive_judge_realtime_summary";
    public static final String STATE_DYNAMIC_CAUSAL_DIAGNOSIS = "dynamic_causal_diagnosis";

    public static final int[] WINDOWS =
            new int[] {
                8, 16, 24, 32, 48, 50, 60, 64, 80, 96, 100, 120, 128, 160, 180, 240, 300, 360, 480,
                500, 720, 960, 1000, 1200, 1440, 3000, 5000, 10000, 20000
            };

    public static final int WINDOW_8 = 8;
    public static final int WINDOW_16 = 16;
    public static final int WINDOW_24 = 24;
    public static final int WINDOW_32 = 32;
    public static final int WINDOW_48 = 48;
    public static final int WINDOW_50 = 50;
    public static final int WINDOW_60 = 60;
    public static final int WINDOW_64 = 64;
    public static final int WINDOW_80 = 80;
    public static final int WINDOW_96 = 96;
    public static final int WINDOW_100 = 100;
    public static final int WINDOW_120 = 120;
    public static final int WINDOW_128 = 128;
    public static final int WINDOW_160 = 160;
    public static final int WINDOW_180 = 180;
    public static final int WINDOW_240 = 240;
    public static final int WINDOW_300 = 300;
    public static final int WINDOW_360 = 360;
    public static final int WINDOW_480 = 480;
    public static final int WINDOW_500 = 500;
    public static final int WINDOW_720 = 720;
    public static final int WINDOW_960 = 960;
    public static final int WINDOW_1000 = 1000;
    public static final int WINDOW_1200 = 1200;
    public static final int WINDOW_1440 = 1440;
    public static final int WINDOW_3000 = 3000;
    public static final int WINDOW_5000 = 5000;
    public static final int WINDOW_10000 = 10000;
    public static final int WINDOW_20000 = 20000;

    public static final int REALTIME_WINDOW_FAST = WINDOW_60;
    public static final int REALTIME_WINDOW_MID = WINDOW_120;
    public static final int REALTIME_WINDOW_LONG = WINDOW_240;
    public static final int REALTIME_RECENT_LOAD_LIMIT = WINDOW_360;
    public static final int REALTIME_PREDRAW_FEATURE_LOAD_LIMIT = WINDOW_360;
    public static final int REALTIME_PREDRAW_REASON_LOAD_LIMIT = WINDOW_360;

    public static final int PREDRAW_FEATURE_WINDOW_16 = WINDOW_16;
    public static final int PREDRAW_FEATURE_WINDOW_24 = WINDOW_24;
    public static final int PREDRAW_FEATURE_WINDOW_32 = WINDOW_32;
    public static final int PREDRAW_FEATURE_WINDOW_48 = WINDOW_48;
    public static final int PREDRAW_FEATURE_WINDOW_64 = WINDOW_64;
    public static final int PREDRAW_FEATURE_WINDOW_96 = WINDOW_96;
    public static final int PREDRAW_FEATURE_WINDOW_120 = WINDOW_120;
    public static final int PREDRAW_FEATURE_WINDOW_180 = WINDOW_180;
    public static final int PREDRAW_FEATURE_WINDOW_240 = WINDOW_240;
    public static final int PREDRAW_FEATURE_WINDOW_360 = WINDOW_360;
    public static final int PREDRAW_FEATURE_WINDOW_500 = WINDOW_500;
    public static final int PREDRAW_FEATURE_WINDOW_720 = WINDOW_720;

    public static final double SMOOTH_ALPHA = 2.0d;
    public static final double WILSON_Z_80 = 1.28d;

    public static final double MIN_REAL_BET_PROBABILITY = 0.350d;
    public static final double MIN_REAL_BET_PROB = MIN_REAL_BET_PROBABILITY;
    public static final double MIN_EXEC_GATE = 0.345d;

    public static final double DEFAULT_DYNAMIC_GATE = 0.385d;
    public static final double DEFAULT_GATE = DEFAULT_DYNAMIC_GATE;
    public static final double MIN_DYNAMIC_GATE = 0.335d;
    public static final double MAX_DYNAMIC_GATE = 0.560d;
    public static final double HIGH_ACCURACY_GATE = 0.420d;

    public static final double NORMAL_GATE_LOW = 0.370d;
    public static final double NORMAL_GATE_HIGH = 0.420d;
    public static final double STRONG_GATE_LOW = 0.345d;
    public static final double STRONG_GATE_HIGH = 0.380d;
    public static final double LOW_FREQ_STRONG_GATE_HIGH = 0.365d;
    public static final double RISK_GATE_HIGH = 0.450d;
    public static final double HARD_RISK_GATE_HIGH = 0.500d;

    public static final double SHADOW_GATE_LOW = 0.320d;
    public static final double SHADOW_GATE_HIGH = 0.370d;
    public static final double OBSERVE_GATE_LOW = 0.305d;

    public static final int LIVE_SHADOW_WARMUP_ISSUES = 30;
    public static final int LIVE_SHADOW_WARMUP_MIN_ISSUES = LIVE_SHADOW_WARMUP_ISSUES;
    public static final int TEACHER_STRONG_SAMPLE_WARMUP_ISSUES = 1000;
    public static final int REAL_BET_PROBE_MIN_ISSUES = 30;
    public static final int REAL_BET_NORMAL_MIN_ISSUES = 30;
    public static final boolean ENABLE_REAL_BET_PROBE_DOMAIN = true;
    public static final boolean ENABLE_REAL_BET_PROBE_AFTER_240 = false;
    public static final boolean REAL_BET_PROBE_KEEP_NEG_EDGE_SHADOW = true;
    public static final boolean REAL_BET_PROBE_KEEP_HARD_RISK_HOLD = true;
    public static final double REAL_BET_PROBE_MIN_FINAL_P = 0.350d;
    public static final double REAL_BET_PROBE_MIN_EDGE = -0.010d;
    public static final double REAL_BET_PROBE_MIN_COVER_QUALITY = 0.300d;
    public static final double REAL_BET_PROBE_MIN_THIRD_QUALITY = 0.100d;
    public static final double REAL_BET_PROBE_MAX_RISK_SCORE = 0.620d;
    public static final boolean REAL_BET_PROBE_ALLOW_TEACHER_NO_SAMPLE = true;
    public static final boolean REAL_BET_PROBE_ALLOW_DYNAMIC_NEW_WATCH = true;
    public static final boolean REAL_BET_PROBE_ALLOW_ADAPTIVE_MISSING = true;
    public static final boolean REAL_BET_PROBE_REQUIRE_DECISION_GATE = true;
    public static final String REJECT_EDGE_NEGATIVE = "EDGE_NEGATIVE";
    public static final String REJECT_WARMUP_LOCK = "WARMUP_LOCK";
    public static final String REJECT_ADAPTIVE_NO_CANDIDATES = "ADAPTIVE_NO_CANDIDATES";
    public static final String REJECT_TEACHER_RISK = "TEACHER_RISK";
    public static final String REJECT_V20_FREEZE = "V20_FREEZE";
    public static final String REJECT_THIRD_WEAK = "THIRD_WEAK";
    public static final String REJECT_COVER_LOW = "COVER_LOW";
    public static final String REJECT_P_BELOW_GATE = "P_BELOW_GATE";
    public static final boolean B_GRADE_DEFAULT_SHADOW = true;
    public static final boolean B_GRADE_WARMUP_ONLY_SHADOW = true;
    public static final boolean NEVER_BET_GRADE_C = true;
    public static final boolean GRADE_C_ONLY_SHADOW_OR_HOLD = true;

    public static final String GRADE_S = "S";
    public static final String GRADE_A = "A";
    public static final String GRADE_B = "B";
    public static final String GRADE_C = "C";

    public static final double GRADE_S_MIN_SCORE = 0.780d;
    public static final double GRADE_A_MIN_SCORE = 0.660d;
    public static final double GRADE_B_MIN_SCORE = 0.540d;
    public static final double GRADE_C_MAX_SCORE = GRADE_B_MIN_SCORE;

    public static final double GRADE_S_MIN_FINAL_P = 0.400d;
    public static final double GRADE_A_MIN_FINAL_P = 0.370d;
    public static final double GRADE_B_MIN_FINAL_P = 0.345d;
    public static final double GRADE_C_MAX_FINAL_P = GRADE_B_MIN_FINAL_P;

    public static final double SHADOW_PROMOTE_HIT_30 = 0.370d;
    public static final double SHADOW_PROMOTE_HIT_60 = 0.350d;
    public static final double SHADOW_PROMOTE_HIT_120 = 0.330d;
    public static final double SHADOW_PROMOTION_MIN_SCORE = 0.050d;
    public static final int SHADOW_PROMOTE_MIN_SAMPLE_5 = 5;
    public static final int SHADOW_PROMOTE_MIN_SAMPLE_10 = 10;
    public static final int SHADOW_PROMOTE_MIN_SAMPLE_15 = 15;
    public static final double SHADOW_PROMOTE_HIT_5 = 0.600d;
    public static final double SHADOW_PROMOTE_HIT_10 = 0.450d;
    public static final double SHADOW_PROMOTE_HIT_15 = 0.400d;
    public static final double SHADOW_PROMOTE_LIGHT_HIT_15 = 0.370d;
    public static final int SHADOW_PROMOTE_MIN_SAMPLE_30 = 30;
    public static final int SHADOW_PROMOTE_MIN_SAMPLE_60 = 60;
    public static final int SHADOW_PROMOTE_MIN_SAMPLE_120 = 120;
    public static final int SHADOW_MAX_LOSE = 7;
    public static final int SHADOW_HARD_MAX_LOSE = 9;

    public static final double TARGET_FREQ_LOW = 0.080d;
    public static final double TARGET_FREQ_HIGH = 0.150d;
    public static final double SAFE_FREQ_LOW = 0.060d;
    public static final double HIGH_ACCURACY_FREQ_LOW = 0.080d;
    public static final double HIGH_ACCURACY_FREQ_HIGH = 0.150d;
    public static final double HEALTHY_VOLUME_FREQ_LOW = 0.080d;
    public static final double HEALTHY_VOLUME_FREQ_HIGH = 0.150d;

    public static final int SAME_CLASS_FREEZE_LOSE = 4;
    public static final int SAME_CLASS_HARD_FREEZE_LOSE = 6;
    public static final int SAME_CLASS_RECOVERY_SHADOW_MIN = 30;
    public static final double SAME_CLASS_RECOVERY_HIT_RATE = 0.370d;
    public static final int GLOBAL_KEEP_ONLY_S_LOSE = 8;
    public static final int GLOBAL_SHADOW_RECOVERY_LOSE = 10;

    public static final int FALSE_STRONG_FREEZE_LOSE = 3;
    public static final int FALSE_STRONG_HARD_FREEZE_LOSE = 6;
    public static final double FALSE_STRONG_RISK_LOW = 0.250d;
    public static final double FALSE_STRONG_RISK_MID = 0.420d;
    public static final double FALSE_STRONG_RISK_HIGH_V20 = 0.580d;

    public static final double THIRD_GUARD_MIN_NET = 0.000d;
    public static final double THIRD_GUARD_STRONG_NET = 0.030d;
    public static final double THIRD_GUARD_DRAG_RISK_HIGH = 0.580d;
    public static final double RANK_COUNTER_MIN_EDGE = 0.000d;
    public static final double RANK_COUNTER_STRONG_EDGE = 0.030d;
    public static final double RANK_4_6_OVERTAKE_RISK_HIGH = 0.420d;
    public static final double POSITION_MIN_HEALTH = 0.330d;
    public static final double POSITION_GOOD_HEALTH = 0.370d;
    public static final double POSITION_STRONG_HEALTH = 0.400d;

    public static final int CAUSAL_FACTOR_MIN_SAMPLE = 30;
    public static final int CAUSAL_FACTOR_GOOD_SAMPLE = 60;
    public static final int CAUSAL_FACTOR_STRONG_SAMPLE = 120;
    public static final double CAUSAL_FACTOR_MIN_EDGE = 0.000d;
    public static final double CAUSAL_FACTOR_GOOD_EDGE = 0.025d;
    public static final double CAUSAL_FACTOR_STRONG_EDGE = 0.055d;
    public static final double CAUSAL_FACTOR_LCB_MIN = 0.300d;

    public static final int POSITION_HEALTH_WINDOW_30 = 30;
    public static final int POSITION_HEALTH_WINDOW_60 = 60;
    public static final int POSITION_HEALTH_WINDOW_120 = 120;
    public static final int RULE_HEALTH_WINDOW_30 = 30;
    public static final int RULE_HEALTH_WINDOW_60 = 60;
    public static final int RULE_HEALTH_WINDOW_120 = 120;

    public static final String V20_MODE_WARMUP = "V20_WARMUP";
    public static final String V20_MODE_HIGH_ACCURACY = "V20_HIGH_ACCURACY";
    public static final String V20_MODE_HEALTHY_VOLUME = "V20_HEALTHY_VOLUME";
    public static final String V20_MODE_SHADOW_RECOVERY = "V20_SHADOW_RECOVERY";
    public static final String V20_MODE_HOLD_RISK = "V20_HOLD_RISK";

    public static final String V20_TREND_UP = "TU";
    public static final String V20_TREND_FLAT = "TF";
    public static final String V20_TREND_DOWN = "TD";
    public static final String V20_TREND_BREAK = "BREAK";
    public static final String V20_TREND_RECOVERY = "RECOVERY";

    public static final String V20_SHAPE_CONVERGE = "CO";
    public static final String V20_SHAPE_NORMAL = "NO";
    public static final String V20_SHAPE_DIVERGE = "DI";
    public static final String V20_SHAPE_WEAK_BOARD = "BL";
    public static final String V20_SHAPE_SHOCK = "SHOCK";

    public static final String V20_FLOW_HIT_STREAK = "HIT_STREAK";
    public static final String V20_FLOW_LOSE_STREAK = "LOSE_STREAK";
    public static final String V20_FLOW_BREAK = "FLOW_BREAK";
    public static final String V20_FLOW_BACKFILL = "BACKFILL";
    public static final String V20_FLOW_JUMP = "JUMP";
    public static final String V20_FLOW_RECOVERY = "FLOW_RECOVERY";

    public static final String V20_NUMBER_NEW_HOT = "NEW_HOT";
    public static final String V20_NUMBER_OLD_HOT = "OLD_HOT";
    public static final String V20_NUMBER_OVER_HOT = "OVER_HOT";
    public static final String V20_NUMBER_COLD_PRESS = "COLD_PRESS";
    public static final String V20_NUMBER_RECOVERY = "NUM_RECOVERY";
    public static final String V20_NUMBER_RECOVERY_FAIL = "RECOVERY_FAIL";
    public static final String V20_NUMBER_SECOND_RECOVERY = "SECOND_RECOVERY";
    public static final String V20_NUMBER_DEAD_COLD = "DEAD_COLD";

    public static final String V20_ROLE_MAIN_ATTACK = "MAIN_ATTACK";
    public static final String V20_ROLE_TREND = "TREND_NUMBER";
    public static final String V20_ROLE_GUARD = "GUARD_NUMBER";
    public static final String V20_ROLE_BACKFILL = "BACKFILL_NUMBER";

    public static final String V20_RANK_MODE_TOP3_TRUE = "TOP3_TRUE";
    public static final String V20_RANK_MODE_RANK4_6_OVERTAKE = "RANK4_6_OVERTAKE";
    public static final String V20_RANK_MODE_TOP18_WEAK = "TOP18_WEAK";
    public static final String V20_RANK_MODE_UNKNOWN = "RANK_UNKNOWN";

    public static final String TABLE_PRE_DRAW_FEATURE = "pre_draw_feature";
    public static final String TABLE_PRE_DRAW_NUMBER_SAMPLE = "pre_draw_number_sample";
    public static final String TABLE_CANDIDATE_AUDIT = "candidate_audit";
    public static final String TABLE_RANK_COUNTERFACTUAL = "rank_counterfactual";
    public static final String TABLE_POSITION_HEALTH = "position_health";
    public static final String TABLE_SOURCE_HEALTH = "source_health";
    public static final String TABLE_STATE_HEALTH = "state_health";
    public static final String TABLE_RULE_HEALTH = "rule_health";
    public static final String TABLE_SHADOW_SIGNAL = "shadow_signal";
    public static final String TABLE_FALSE_STRONG_FREEZE = "false_strong_freeze";
    public static final String TABLE_THIRD_GUARD_MATRIX = "third_guard_matrix";
    public static final String TABLE_SEED_CAUSE_MATRIX = "seed_cause_matrix";
    public static final String TABLE_DECISION_AUDIT = "decision_audit";
    public static final String TABLE_REAL_TRADE_LEDGER = "real_trade_ledger";

    public static final double ELITE45_MIN_FINAL_P = 0.425d;
    public static final double ELITE45_MIN_EDGE = 0.008d;
    public static final double ELITE45_MIN_REASON_EDGE = 0.030d;
    public static final double ELITE45_MIN_HIT_SIMILARITY = 0.650d;
    public static final double ELITE45_MAX_MISS_SIMILARITY = 0.350d;
    public static final int ELITE45_MIN_VALID_REASON_COUNT = 3;
    public static final int ELITE45_MAX_FALSE_REASON_COUNT = 0;
    public static final double ELITE45_MIN_COMBO_ROLE_DIVERSITY = 0.600d;
    public static final double ELITE45_MAX_COMBO_CONFLICT = 0.300d;
    public static final double ELITE45_MAX_DRIFT_RISK = 0.420d;
    public static final double ELITE45_MAX_BREAK_RISK = 0.380d;
    public static final double ELITE45_MAX_NEGATIVE_REASON_SCORE = 0.250d;
    public static final double ELITE45_MAX_CONTRADICTION_SCORE = 0.250d;
    public static final double ELITE45_MAX_RECENT_DECAY = 0.000d;
    public static final int ELITE45_MIN_HISTORY = 30;
    public static final int ELITE45_MIN_REASON_SAMPLE = 30;
    public static final int ELITE45_SHADOW_REASON_SAMPLE = 60;
    public static final int ELITE45_STRONG_REASON_SAMPLE = 120;
    public static final int ELITE45_AUDIT_KEEP = 20000;

    public static final double ELITE45_GATE_FLOOR = 0.405d;
    public static final double ELITE45_GATE_DEFAULT = ELITE45_MIN_FINAL_P;
    public static final double ELITE45_GATE_HARD = 0.450d;
    public static final double ELITE45_REASON_GATE_BOOST_MISS_SIM = 0.015d;
    public static final double ELITE45_REASON_GATE_BOOST_MISS_SIM_HARD = 0.030d;
    public static final double ELITE45_REASON_GATE_BOOST_FALSE = 0.025d;
    public static final double ELITE45_REASON_GATE_BOOST_COMBO_CONFLICT = 0.015d;

    public static final int SAME_FINGERPRINT_MISS_LIGHT = 1;
    public static final int SAME_FINGERPRINT_MISS_SHADOW = 2;
    public static final int SAME_FINGERPRINT_MISS_COOLDOWN = 3;
    public static final int SAME_FINGERPRINT_MISS_FALSE_STRONG = 4;
    public static final int FINGERPRINT_COOLDOWN_ISSUES_MIN = 30;
    public static final int FINGERPRINT_COOLDOWN_ISSUES_MAX = 60;
    public static final double FINGERPRINT_COOLDOWN_RISK_PENALTY = 0.035d;
    public static final double FINGERPRINT_FALSE_STRONG_RISK_PENALTY = 0.060d;

    public static final int REASON_MIN_SAMPLE = 30;
    public static final int REASON_USEFUL_SAMPLE = 60;
    public static final int REASON_STRONG_SAMPLE = 120;
    public static final int REASON_STABLE_SAMPLE = 240;
    public static final int REASON_RECENT_WINDOW = 120;
    public static final int REASON_FORWARD_VALIDATE_MIN = 60;
    public static final double REASON_A_WILSON_MIN = 0.395d;
    public static final double REASON_B_WILSON_MIN = 0.365d;
    public static final double REASON_FALSE_WILSON_MAX = 0.315d;
    public static final double REASON_DECAY_WARN = 0.080d;
    public static final double REASON_DECAY_HARD = 0.150d;
    public static final double REASON_HIT_RATE_BASELINE = RANDOM_K3_BASELINE;

    public static final String REASON_LEVEL_A_VALID = "A_VALID";
    public static final String REASON_LEVEL_B_USEFUL = "B_USEFUL";
    public static final String REASON_LEVEL_C_EXPLAIN_ONLY = "C_EXPLAIN_ONLY";
    public static final String REASON_LEVEL_D_DECAY = "D_DECAY";
    public static final String REASON_LEVEL_E_FALSE = "E_FALSE";

    public static final String SOURCE_TYPE_REALTIME = "REALTIME";
    public static final String SOURCE_TYPE_BACKTEST = "BACKTEST";
    public static final String SOURCE_TYPE_MANUAL_BACKTEST = "MANUAL_BACKTEST";
    public static final String SOURCE_TYPE_AUTO_LEARN = "AUTO_LEARN";
    public static final String SOURCE_TYPE_TEACHER_REPLAY = "TEACHER_REPLAY";
    public static final String SOURCE_TYPE_TEACHER_MATRIX = "TEACHER_MATRIX";
    public static final String SOURCE_TYPE_SHADOW = "SHADOW";
    public static final String SOURCE_TYPE_UNKNOWN = "UNKNOWN";

    public static final double LEARN_WEIGHT_PREDRAW_REAL_TRADE = 1.00d;
    public static final double LEARN_WEIGHT_PREDRAW_SHADOW = 0.35d;
    public static final double LEARN_WEIGHT_PREDRAW_AUTO_LEARN = 0.25d;
    public static final double LEARN_WEIGHT_PREDRAW_BACKTEST = 0.15d;
    public static final double LEARN_WEIGHT_PREDRAW_HOLD = 0.08d;

    public static final double SIMILAR_HIT_RATE_MIN = 0.340d;
    public static final double COMBO_HIT_RATE_MIN = 0.340d;
    public static final double CALIBRATION_ERROR_MAX = 0.080d;
    public static final double CALIBRATION_ERROR_HARD = 0.140d;

    public static final int EFFECTIVE_SAMPLE_MIN = 80;
    public static final int EFFECTIVE_SAMPLE_HIGH = 300;
    public static final int COMBO_SAMPLE_MIN = 60;
    public static final int COMBO_SAMPLE_HIGH = 180;

    public static final double TREND_LIFE_MIN = 58.0d;
    public static final double TREND_LIFE_HIGH = 78.0d;
    public static final double DRIFT_RISK_MAX = 42.0d;
    public static final double DRIFT_RISK_HARD_BLOCK = 58.0d;
    public static final double BREAK_DEPTH_MAX = 48.0d;
    public static final double BREAK_DEPTH_HARD_BLOCK = 60.0d;
    public static final double RECOVERY_SCORE_MIN = 50.0d;
    public static final double RECOVERY_SCORE_STRONG = 68.0d;

    public static final double CHAMPION_GAP_MIN = 0.008d;
    public static final double CHAMPION_GAP_HIGH = 0.030d;
    public static final double COMBO_GAP_MIN = 0.004d;
    public static final double COMBO_GAP_HIGH = 0.016d;

    public static final double HOT_REVERSE_RISK_MAX = 45.0d;
    public static final double FALSE_STRONG_RISK_MAX = 44.0d;

    public static final double DEEP_SCORE_MIN = 0.480d;
    public static final double DEEP_SCORE_STRONG = 0.600d;
    public static final double DEEP_SCORE_A_PLUS = 0.720d;

    public static final double AI_JUDGE_SCORE_MIN = 0.500d;
    public static final double AI_JUDGE_SCORE_STRONG = 0.620d;
    public static final double AI_JUDGE_SCORE_A_PLUS = 0.740d;

    public static final double AI_JUDGE_CONFIDENCE_MIN = 0.420d;
    public static final double AI_JUDGE_CONFIDENCE_STRONG = 0.560d;
    public static final double AI_JUDGE_CONFIDENCE_A_PLUS = 0.680d;

    public static final double AI_JUDGE_CHAMPION_GAP_MIN = 0.015d;
    public static final double AI_JUDGE_CHAMPION_GAP_STRONG = 0.040d;
    public static final double AI_JUDGE_CONFLICT_MAX = 0.660d;
    public static final double AI_JUDGE_CONFLICT_HARD = 0.800d;

    public static final double EVIDENCE_SCORE_MIN = 0.480d;
    public static final double EVIDENCE_SCORE_STRONG = 0.600d;
    public static final double EVIDENCE_SCORE_A_PLUS = 0.720d;
    public static final double EVIDENCE_RISK_MAX = 0.560d;
    public static final double EVIDENCE_RISK_HARD = 0.720d;

    public static final boolean STRONG_EVIDENCE_LOW_GATE_ENABLE = true;
    public static final double STRONG_EVIDENCE_MIN_FINAL_P = 0.345d;
    public static final double STRONG_EVIDENCE_MIN_SCORE = 0.600d;
    public static final double STRONG_EVIDENCE_MIN_AI_SCORE = 0.560d;
    public static final double STRONG_EVIDENCE_MIN_CONFIDENCE = 0.460d;
    public static final double STRONG_EVIDENCE_MAX_RISK = 0.560d;
    public static final double STRONG_EVIDENCE_MAX_CONFLICT = 0.660d;
    public static final double STRONG_EVIDENCE_GATE_REDUCE_MAX = 0.045d;

    public static final boolean LOW_FREQ_RELAX_ENABLE = true;
    public static final double LOW_FREQ_RELAX_MAX = 0.020d;
    public static final double LOW_FREQ_RELAX_STEP = 0.006d;

    public static final boolean WEAK_RISK_TIGHTEN_ENABLE = true;
    public static final double WEAK_RISK_GATE_BOOST_MAX = 0.045d;

    public static final double TARGET_HIT_RATE_MIN = 0.370d;
    public static final double TARGET_HIT_RATE_GOOD = 0.380d;
    public static final double TARGET_HIT_RATE_IDEAL = 0.400d;
    public static final double TARGET_HIT_RATE_A_PLUS = 0.420d;

    public static final boolean SMART_DYNAMIC_FREQUENCY_ENABLE = true;
    public static final boolean FREQUENCY_HARD_BLOCK_ENABLE = false;

    public static final double TARGET_FREQ_MIN = 0.150d;
    public static final double TARGET_FREQ_IDEAL_LOW = 0.150d;
    public static final double TARGET_FREQ_MID = 0.180d;
    public static final double TARGET_FREQ_IDEAL_HIGH = 0.200d;
    public static final double TARGET_FREQ_MAX = 1.000d;
    public static final double TARGET_FREQ_HARD_MAX = 1.000d;

    public static final double SMART_FREQ_ZERO_ALLOWED = 0.000d;
    public static final double SMART_FREQ_LOW = 0.150d;
    public static final double SMART_FREQ_NORMAL_LOW = 0.150d;
    public static final double SMART_FREQ_NORMAL_HIGH = 0.200d;
    public static final double SMART_FREQ_STRONG_HIGH = 0.250d;
    public static final double SMART_FREQ_OVERHEAT = 0.500d;

    public static final int TARGET_FREQ_MIN_100 = 15;
    public static final int TARGET_FREQ_MAX_100 = 100;
    public static final int TARGET_FREQ_HARD_MAX_100 = 100;

    public static final int FREQ_WINDOW_SHORT = 100;
    public static final int FREQ_WINDOW_MID = 500;
    public static final int FREQ_WINDOW_LONG = 1000;

    public static final double SMART_FREQ_LOW_RELAX_MAX = 0.018d;
    public static final double SMART_FREQ_HIGH_GATE_BOOST = 0.012d;
    public static final double SMART_FREQ_STRONG_HIGH_GATE_BOOST = 0.024d;
    public static final double SMART_FREQ_OVERHEAT_GATE_BOOST = 0.040d;
    public static final double SMART_FREQ_NO_FORCE_BET_GATE_BOOST = 0.000d;

    public static final int MIN_DRAWS_ANALYZE = 30;
    public static final int MIN_DRAWS_SHADOW_ONLY = 30;
    public static final int MIN_DRAWS_LOW_FREQ = 300;
    public static final int MIN_DRAWS_FULL_RUN = 1000;

    public static final int MIN_DRAWS_DEEP_FEATURE = 160;
    public static final int MIN_DRAWS_BIAS_EVIDENCE = 30;
    public static final int MIN_DRAWS_AI_JUDGE = 30;
    public static final int MIN_DRAWS_MANUAL_BACKTEST = 300;
    public static final int MAX_LOCAL_HISTORY_SAVE = 5000;

    public static final boolean REALTIME_DECISION_PRIORITY_ENABLE = true;
    public static final int REALTIME_DECISION_TARGET_MS = 3500;
    public static final int REALTIME_DECISION_HARD_LIMIT_MS = 5000;
    public static final int REALTIME_DECISION_EMERGENCY_LIMIT_MS = 6500;

    public static final int REALTIME_TIME_BUDGET_MS = REALTIME_DECISION_TARGET_MS;
    public static final int REALTIME_HARD_TIMEOUT_MS = REALTIME_DECISION_HARD_LIMIT_MS;
    public static final int REALTIME_EMERGENCY_TIMEOUT_MS = REALTIME_DECISION_EMERGENCY_LIMIT_MS;

    public static final int REALTIME_MAX_K3_CANDIDATES = 40;
    public static final int REALTIME_ADAPTIVE_CANDIDATE_INPUT_LIMIT = 40;
    public static final int REALTIME_DECISION_CANDIDATE_INPUT_LIMIT = 40;
    public static final int REALTIME_PROBABILITY_CANDIDATE_LIMIT = 120;
    public static final boolean REALTIME_FIRST_DECISION_THEN_DEEP_DIAGNOSIS = true;
    public static final boolean REALTIME_DEEP_DIAGNOSIS_BACKGROUND_ONLY = true;
    public static final boolean REALTIME_DO_NOT_WAIT_FULL_CAUSAL_REPORT = true;
    public static final int REALTIME_CORE_CANDIDATE_CAP = 60;
    public static final int REALTIME_CORE_CANDIDATE_CAP_URGENT = 12;
    public static final int REALTIME_MAX_POSITION_SCAN = 10;
    public static final int REALTIME_MAX_NUMBER_PER_POSITION = 10;
    public static final int REALTIME_MAX_PROFILE_READ_PER_POSITION = 12;
    public static final int REALTIME_MAX_REASON_LINES = 18;
    public static final int REALTIME_MAX_PREDRAW_REASON_SAVE_PER_ISSUE =
            POSITION_COUNT * NUMBER_COUNT;

    public static final boolean REALTIME_ALLOW_BACKTEST_BLOCK = false;
    public static final boolean REALTIME_ALLOW_MANUAL_BACKTEST_BLOCK = false;
    public static final boolean REALTIME_ALLOW_AUTO_LEARN_BLOCK = false;
    public static final boolean REALTIME_SAVE_FAST_RESULT_WHEN_TIMEOUT = true;
    public static final boolean REALTIME_TIMEOUT_FALLBACK_TO_SHADOW = true;
    public static final boolean REALTIME_TIMEOUT_HARD_HOLD_ENABLE = true;

    public static final long REALTIME_PRIORITY_HOLD_MS = 18L * 1000L;
    public static final long BACKGROUND_NEAR_OPEN_HARD_STOP_MS = 5L * 1000L;

    public static final boolean AUTO_LEARN_ENABLE = true;
    public static final boolean AUTO_LEARN_SMALL_BATCH_ENABLE = true;
    public static final boolean AUTO_LEARN_RANDOM_SEGMENT_ENABLE = true;
    public static final boolean AUTO_LEARN_SEGMENT_CONTINUOUS = true;
    public static final boolean AUTO_LEARN_DEDUP_CASE_ENABLE = true;
    public static final boolean AUTO_LEARN_FEED_REALTIME = false;
    public static final boolean AUTO_LEARN_FEED_PROFILE = true;
    public static final boolean AUTO_LEARN_FEED_NUMBER_PROFILE = true;
    public static final boolean AUTO_LEARN_FEED_COMBO_PROFILE = true;
    public static final boolean AUTO_LEARN_FEED_TEACHER = true;
    public static final boolean AUTO_LEARN_FEED_PREDRAW_REASON = true;
    public static final boolean AUTO_LEARN_NEVER_BLOCK_REALTIME = true;

    public static final int AUTO_LEARN_WINDOW_300 = 300;
    public static final int AUTO_LEARN_WINDOW_500 = 500;
    public static final int AUTO_LEARN_DEFAULT_WINDOW = AUTO_LEARN_WINDOW_300;
    public static final int AUTO_LEARN_MAX_WINDOW = AUTO_LEARN_WINDOW_500;
    public static final int AUTO_LEARN_MIN_WINDOW = AUTO_LEARN_WINDOW_300;

    public static final int AUTO_LEARN_MAX_TARGETS_PER_BATCH = 12;
    public static final int AUTO_LEARN_MAX_POSITION_SAMPLES_PER_TARGET = 10;
    public static final int AUTO_LEARN_MAX_NUMBER_SAMPLES_PER_TARGET = 100;
    public static final int AUTO_LEARN_MAX_POSITION_SAMPLES_PER_BATCH = 120;
    public static final int AUTO_LEARN_MAX_NUMBER_SAMPLES_PER_BATCH = 1200;
    public static final int AUTO_LEARN_SAVE_CURSOR_EVERY_TARGET = 1;
    public static final int AUTO_LEARN_BROADCAST_EVERY_TARGET = 1;

    public static final long AUTO_LEARN_INTERVAL_MS = 30L * 1000L;
    public static final long AUTO_LEARN_IDLE_INTERVAL_MS = 60L * 1000L;
    public static final long AUTO_LEARN_MIN_GAP_AFTER_REALTIME_MS = 8L * 1000L;
    public static final long AUTO_LEARN_MAX_SINGLE_BATCH_MS = 18000L;
    public static final long AUTO_LEARN_COOPERATIVE_SLEEP_MS = 2L;

    public static final int DRAW_PROTECT_SECONDS = 12;
    public static final int AUTO_LEARN_BLOCK_BEFORE_OPEN_SECONDS = DRAW_PROTECT_SECONDS;
    public static final int AUTO_LEARN_RESUME_AFTER_REALTIME_SECONDS = 5;

    public static final long AUTO_LEARN_RANDOM_SEED_DEFAULT = 2026061701L;
    public static final int AUTO_LEARN_USED_SEGMENT_KEEP = 120;
    public static final int AUTO_LEARN_SEGMENT_OVERLAP_TOLERANCE = 0;

    public static final String AUTO_LEARN_STATUS_IDLE = "LEARN_IDLE";
    public static final String AUTO_LEARN_STATUS_WAITING = "LEARN_WAITING";
    public static final String AUTO_LEARN_STATUS_RUNNING = "LEARN_RUNNING";
    public static final String AUTO_LEARN_STATUS_PAUSED = "LEARN_PAUSED";
    public static final String AUTO_LEARN_STATUS_SAVING = "LEARN_SAVING";
    public static final String AUTO_LEARN_STATUS_OK = "LEARN_OK";
    public static final String AUTO_LEARN_STATUS_SKIP = "LEARN_SKIP";
    public static final String AUTO_LEARN_STATUS_ERROR = "LEARN_ERROR";

    public static final String AUTO_LEARN_TASK_RECENT_300 = "RECENT_300";
    public static final String AUTO_LEARN_TASK_RECENT_500 = "RECENT_500";
    public static final String AUTO_LEARN_TASK_RANDOM_300 = "RANDOM_300";
    public static final String AUTO_LEARN_TASK_RANDOM_500 = "RANDOM_500";
    public static final String AUTO_LEARN_TASK_BAD_CASE_REPLAY = "BAD_CASE_REPLAY";
    public static final String AUTO_LEARN_TASK_TEACHER_MATRIX = "TEACHER_MATRIX";
    public static final String AUTO_LEARN_TASK_TEACHER_REPLAY = "TEACHER_REPLAY";
    public static final String AUTO_LEARN_TASK_PREDRAW_REASON = "PREDRAW_REASON";
    public static final String AUTO_LEARN_TASK_DEFAULT = AUTO_LEARN_TASK_RECENT_300;

    public static final boolean LEARN_ANALYZE_TREND = true;
    public static final boolean LEARN_ANALYZE_STATE = true;
    public static final boolean LEARN_ANALYZE_SHAPE = true;
    public static final boolean LEARN_ANALYZE_DRIFT = true;
    public static final boolean LEARN_ANALYZE_BREAK = true;
    public static final boolean LEARN_ANALYZE_RECOVERY = true;
    public static final boolean LEARN_ANALYZE_TRANSITION = true;
    public static final boolean LEARN_ANALYZE_POSITION_STRENGTH = true;
    public static final boolean LEARN_ANALYZE_NUMBER_PROFILE = true;
    public static final boolean LEARN_ANALYZE_COMBO_PROFILE = true;
    public static final boolean LEARN_ANALYZE_PREDRAW_REASON = true;
    public static final boolean LEARN_ANALYZE_CONTRASTIVE_REASON = true;
    public static final boolean LEARN_ANALYZE_CAUSAL_REASON = true;
    public static final boolean LEARN_ANALYZE_MISS_REASON = true;
    public static final boolean LEARN_ANALYZE_NEXT_ADVICE = true;
    public static final boolean LEARN_ANALYZE_COUNTERFACTUAL = true;

    public static final int LEARN_TOP_NUMBER_LIMIT = 6;
    public static final int LEARN_CACHE_STRONG_NUMBER_LIMIT = 4;
    public static final int LEARN_FAILED_K3_LIMIT = 4;
    public static final int LEARN_CANDIDATE_K3_LIMIT_PER_POSITION = 8;

    public static final int MIN_CACHE_SAMPLE_FOR_BET = 24;
    public static final int MIN_STATE_SAMPLE_FOR_BET = 36;
    public static final int MIN_RECENT_SAMPLE_FOR_BET = 12;
    public static final double MIN_EDGE_FOR_BET = 0.000d;
    public static final double MAX_RISK_FOR_BET = 0.620d;
    public static final double MAX_DRIFT_FOR_BET = 0.580d;
    public static final double MAX_BREAK_FOR_BET = 0.600d;

    public static final long RUNTIME_PROFILE_CACHE_TTL_MS = 24L * 60L * 60L * 1000L;
    public static final long STATE_NUMBER_PROFILE_CACHE_TTL_MS = 24L * 60L * 60L * 1000L;
    public static final long LEARNING_CASE_KEEP_MS = 7L * 24L * 60L * 60L * 1000L;

    public static final boolean TEACHER_REPLAY_ENABLE = true;
    public static final boolean TEACHER_REPLAY_NEVER_BLOCK_REALTIME = true;
    public static final boolean TEACHER_REPLAY_POINT_IN_TIME_ONLY = true;
    public static final boolean TEACHER_REPLAY_RANDOM_BATCH_ENABLE = true;
    public static final boolean TEACHER_REPLAY_CONTINUOUS_CURSOR_ENABLE = true;
    public static final boolean TEACHER_REPLAY_DEDUP_TARGET_ENABLE = true;
    public static final boolean TEACHER_REPLAY_FEED_TEACHER_MATRIX = true;
    public static final boolean TEACHER_REPLAY_FEED_TEACHER_ASSIST = true;
    public static final boolean TEACHER_REPLAY_FEED_REASON = true;
    public static final boolean TEACHER_REPLAY_FEED_PREDRAW_REASON = true;
    public static final boolean TEACHER_REPLAY_FEED_STABILITY = true;
    public static final boolean TEACHER_REPLAY_ALLOW_COUNTERFACTUAL = true;

    public static final int TEACHER_REPLAY_BATCH_SIZE = 5;
    public static final int TEACHER_REPLAY_BATCH_SIZE_MAX = 8;
    public static final int TEACHER_REPLAY_MIN_HISTORY_BEFORE_TARGET = 30;
    public static final int TEACHER_REPLAY_DEFAULT_LOOKBACK = 300;
    public static final int TEACHER_REPLAY_MAX_LOOKBACK = 500;
    public static final int TEACHER_REPLAY_TARGET_SCAN_LIMIT = 50000;
    public static final int TEACHER_REPLAY_MAX_TARGETS_PER_BATCH = 6;
    public static final int TEACHER_REPLAY_SAVE_CURSOR_EVERY_TARGET = 1;
    public static final int TEACHER_REPLAY_BROADCAST_EVERY_TARGET = 1;

    public static final int TEACHER_REPLAY_NUMBER_MATRIX_PER_TARGET = POSITION_COUNT * NUMBER_COUNT;
    public static final int TEACHER_REPLAY_COMBO_MATRIX_PER_TARGET =
            POSITION_COUNT * TOTAL_K3_COMBO_PER_POSITION;
    public static final int TEACHER_REPLAY_COUNTERFACTUAL_LIMIT = 6;
    public static final int TEACHER_REPLAY_TOP_CANDIDATE_EXPLAIN = 8;
    public static final int TEACHER_REPLAY_TOP_MISS_REASON = 6;

    public static final long TEACHER_REPLAY_MIN_INTERVAL_MS = 45L * 1000L;
    public static final long TEACHER_REPLAY_IDLE_DELAY_MS = 35L * 1000L;
    public static final long TEACHER_REPLAY_AFTER_ANALYSIS_DELAY_MS = 8L * 1000L;
    public static final long TEACHER_REPLAY_MAX_SINGLE_BATCH_MS = 6000L;
    public static final long TEACHER_REPLAY_COOPERATIVE_SLEEP_MS = 3L;
    public static final long TEACHER_REPLAY_CASE_KEEP_MS = 21L * 24L * 60L * 60L * 1000L;
    public static final long TEACHER_REPLAY_CURSOR_KEEP_MS = 30L * 24L * 60L * 60L * 1000L;

    public static final double TEACHER_REPLAY_REAL_TRADE_WEIGHT = 1.00d;
    public static final double TEACHER_REPLAY_SHADOW_WEIGHT = 0.50d;
    public static final double TEACHER_REPLAY_BACKTEST_WEIGHT = 0.35d;
    public static final double TEACHER_REPLAY_RECENT_WEIGHT = 1.15d;
    public static final double TEACHER_REPLAY_OLD_DECAY = 0.88d;

    public static final double TEACHER_REPLAY_MIN_LIFT = 0.000d;
    public static final double TEACHER_REPLAY_GOOD_LIFT = 0.025d;
    public static final double TEACHER_REPLAY_STRONG_LIFT = 0.055d;
    public static final double TEACHER_REPLAY_MAX_BAD_RATE = 0.680d;
    public static final double TEACHER_REPLAY_DRIFT_RISK_HARD = 0.700d;

    public static final String TEACHER_REPLAY_STATUS_IDLE = "TR_IDLE";
    public static final String TEACHER_REPLAY_STATUS_WAITING = "TR_WAITING";
    public static final String TEACHER_REPLAY_STATUS_RUNNING = "TR_RUNNING";
    public static final String TEACHER_REPLAY_STATUS_OK = "TR_OK";
    public static final String TEACHER_REPLAY_STATUS_SKIP = "TR_SKIP";
    public static final String TEACHER_REPLAY_STATUS_ERROR = "TR_ERROR";

    public static final String TEACHER_REPLAY_REASON_HIT = "WHY_HIT";
    public static final String TEACHER_REPLAY_REASON_MISS = "WHY_MISS";
    public static final String TEACHER_REPLAY_REASON_COUNTERFACTUAL = "COUNTERFACTUAL";
    public static final String TEACHER_REPLAY_REASON_STABILITY = "STABILITY";
    public static final String TEACHER_REPLAY_REASON_PIT = "POINT_IN_TIME";

    public static final boolean TEACHER_MATRIX_ENABLE = true;
    public static final boolean TEACHER_MATRIX_LEARN_AFTER_EACH_DRAW = true;
    public static final boolean TEACHER_MATRIX_LEARN_POSITIVE_AND_NEGATIVE = true;
    public static final boolean TEACHER_MATRIX_FEED_REALTIME = false;
    public static final boolean TEACHER_MATRIX_FEED_PROBABILITY = false;
    public static final boolean TEACHER_MATRIX_FEED_GATE = false;
    public static final boolean TEACHER_MATRIX_FEED_DECISION = false;
    public static final boolean TEACHER_MATRIX_NEVER_BLOCK_REALTIME = true;
    public static final boolean TEACHER_MATRIX_ACCEPT_REPLAY_FEED = true;
    public static final boolean TEACHER_MATRIX_ACCEPT_LATEST_DRAW_FEED = true;
    public static final boolean TEACHER_MATRIX_BUILD_COMBO_PROFILE = true;
    public static final boolean TEACHER_MATRIX_BUILD_STATE_PROFILE = true;
    public static final boolean TEACHER_MATRIX_BUILD_NUMBER_PROFILE = true;
    public static final boolean TEACHER_MATRIX_BUILD_POSITION_PROFILE = true;

    public static final int TEACHER_MATRIX_POSITION_PER_DRAW = POSITION_COUNT;
    public static final int TEACHER_MATRIX_NUMBER_PER_POSITION = NUMBER_COUNT;
    public static final int TEACHER_MATRIX_SAMPLE_PER_DRAW = POSITION_COUNT * NUMBER_COUNT;
    public static final int TEACHER_MATRIX_POSITIVE_PER_DRAW = POSITION_COUNT;
    public static final int TEACHER_MATRIX_NEGATIVE_PER_DRAW = POSITION_COUNT * (NUMBER_COUNT - 1);

    public static final int TEACHER_MATRIX_SAMPLE_PER_ISSUE = TEACHER_MATRIX_SAMPLE_PER_DRAW;
    public static final int TEACHER_MATRIX_POSITIVE_PER_ISSUE = TEACHER_MATRIX_POSITIVE_PER_DRAW;
    public static final int TEACHER_MATRIX_NEGATIVE_PER_ISSUE = TEACHER_MATRIX_NEGATIVE_PER_DRAW;
    public static final int TEACHER_MATRIX_COMBO_SAMPLE_PER_ISSUE =
            POSITION_COUNT * TOTAL_K3_COMBO_PER_POSITION;

    public static final int TEACHER_MATRIX_RECENT_WINDOW_FAST = 60;
    public static final int TEACHER_MATRIX_RECENT_WINDOW_MID = 120;
    public static final int TEACHER_MATRIX_RECENT_WINDOW_LONG = 240;
    public static final int TEACHER_MATRIX_RECENT_WINDOW = 100;
    public static final int TEACHER_MATRIX_MID_WINDOW = 300;
    public static final int TEACHER_MATRIX_LONG_WINDOW = 1000;
    public static final int TEACHER_MATRIX_PROFILE_WINDOW = 500;
    public static final int TEACHER_MATRIX_MAX_BATCH_DRAWS = 12;
    public static final int TEACHER_MATRIX_MAX_BATCH_SAMPLES =
            TEACHER_MATRIX_MAX_BATCH_DRAWS * TEACHER_MATRIX_SAMPLE_PER_DRAW;
    public static final int TEACHER_MATRIX_AUTO_BATCH_DRAWS = TEACHER_MATRIX_MAX_BATCH_DRAWS;
    public static final int TEACHER_MATRIX_AUTO_BATCH_SAMPLES = TEACHER_MATRIX_MAX_BATCH_SAMPLES;

    public static final int TEACHER_MATRIX_MIN_SAMPLE_NUMBER = 18;
    public static final int TEACHER_MATRIX_MIN_SAMPLE_POSITION = 80;
    public static final int TEACHER_MATRIX_MIN_SAMPLE_STATE = 36;
    public static final int TEACHER_MATRIX_MIN_SAMPLE_COMBO = 24;
    public static final int TEACHER_MATRIX_STRONG_SAMPLE_NUMBER = 60;
    public static final int TEACHER_MATRIX_STRONG_SAMPLE_POSITION = 240;
    public static final int TEACHER_MATRIX_STRONG_SAMPLE_STATE = 120;
    public static final int TEACHER_MATRIX_STRONG_SAMPLE_COMBO = 80;

    public static final int TEACHER_MATRIX_MIN_SAMPLE = 30;
    public static final int TEACHER_MATRIX_STRONG_SAMPLE = 120;
    public static final int TEACHER_MATRIX_STABLE_SAMPLE = 240;

    public static final int TEACHER_MATRIX_NUMBER_MIN_SAMPLE = TEACHER_MATRIX_MIN_SAMPLE_NUMBER;
    public static final int TEACHER_MATRIX_NUMBER_STRONG_SAMPLE =
            TEACHER_MATRIX_STRONG_SAMPLE_NUMBER;
    public static final int TEACHER_MATRIX_POSITION_MIN_SAMPLE = TEACHER_MATRIX_MIN_SAMPLE_POSITION;
    public static final int TEACHER_MATRIX_POSITION_STRONG_SAMPLE =
            TEACHER_MATRIX_STRONG_SAMPLE_POSITION;
    public static final int TEACHER_MATRIX_STATE_MIN_SAMPLE = TEACHER_MATRIX_MIN_SAMPLE_STATE;
    public static final int TEACHER_MATRIX_STATE_STRONG_SAMPLE = TEACHER_MATRIX_STRONG_SAMPLE_STATE;
    public static final int TEACHER_MATRIX_COMBO_MIN_SAMPLE = TEACHER_MATRIX_MIN_SAMPLE_COMBO;
    public static final int TEACHER_MATRIX_COMBO_STRONG_SAMPLE = TEACHER_MATRIX_STRONG_SAMPLE_COMBO;

    public static final int TEACHER_MATRIX_NUMBER_SAMPLE = TEACHER_MATRIX_NUMBER_MIN_SAMPLE;
    public static final int TEACHER_MATRIX_POSITION_SAMPLE = TEACHER_MATRIX_POSITION_MIN_SAMPLE;
    public static final int TEACHER_MATRIX_STATE_SAMPLE = TEACHER_MATRIX_STATE_MIN_SAMPLE;
    public static final int TEACHER_MATRIX_COMBO_SAMPLE = TEACHER_MATRIX_COMBO_MIN_SAMPLE;

    public static final int TEACHER_MATRIX_RECENT_MIN_SAMPLE = 12;
    public static final int TEACHER_MATRIX_RECENT_STRONG_SAMPLE = 32;
    public static final int TEACHER_MATRIX_RECENT_ELITE_SAMPLE = 60;

    public static final double TEACHER_MATRIX_POSITIVE_WEIGHT = 1.00d;
    public static final double TEACHER_MATRIX_NEGATIVE_WEIGHT = 0.28d;
    public static final double TEACHER_MATRIX_REAL_TRADE_WEIGHT = 1.00d;
    public static final double TEACHER_MATRIX_SHADOW_WEIGHT = 0.55d;
    public static final double TEACHER_MATRIX_BACKTEST_WEIGHT = 0.32d;
    public static final double TEACHER_MATRIX_RECENT_WEIGHT = 1.20d;
    public static final double TEACHER_MATRIX_OLD_DECAY = 0.86d;

    public static final double TEACHER_MATRIX_MIN_CONFIDENCE = 0.360d;
    public static final double TEACHER_MATRIX_GOOD_CONFIDENCE = 0.520d;
    public static final double TEACHER_MATRIX_STRONG_CONFIDENCE = 0.680d;
    public static final double TEACHER_MATRIX_ELITE_CONFIDENCE = 0.760d;

    public static final double TEACHER_MATRIX_BASE_HIT_RATE = RANDOM_K3_BASELINE;
    public static final double TEACHER_MATRIX_HIT_RATE = RANDOM_K3_BASELINE;
    public static final double TEACHER_MATRIX_WEAK_HIT_RATE = 0.315d;
    public static final double TEACHER_MATRIX_OK_HIT_RATE = 0.365d;
    public static final double TEACHER_MATRIX_STRONG_HIT_RATE = 0.395d;
    public static final double TEACHER_MATRIX_ELITE_HIT_RATE = 0.430d;

    public static final double TEACHER_MATRIX_WEAK_RECENT_HIT_RATE = 0.305d;
    public static final double TEACHER_MATRIX_OK_RECENT_HIT_RATE = 0.355d;
    public static final double TEACHER_MATRIX_STRONG_RECENT_HIT_RATE = 0.385d;
    public static final double TEACHER_MATRIX_ELITE_RECENT_HIT_RATE = 0.420d;
    public static final double TEACHER_MATRIX_BLOCK_HIT_RATE = 0.285d;

    public static final double TEACHER_MATRIX_MAX_PROB_BOOST = 0.035d;
    public static final double TEACHER_MATRIX_MAX_PROB_PENALTY = 0.045d;
    public static final double TEACHER_MATRIX_MAX_GATE_REDUCE = 0.024d;
    public static final double TEACHER_MATRIX_MAX_GATE_BOOST = 0.040d;

    public static final double TEACHER_MATRIX_MAX_PROB_ADJUST = TEACHER_MATRIX_MAX_PROB_BOOST;
    public static final double TEACHER_MATRIX_MIN_PROB_ADJUST = -TEACHER_MATRIX_MAX_PROB_PENALTY;
    public static final double TEACHER_MATRIX_PROB_ADJUST = 0.014d;
    public static final double TEACHER_MATRIX_STRONG_PROB_ADJUST = 0.018d;
    public static final double TEACHER_MATRIX_WEAK_PROB_ADJUST = -0.012d;

    public static final double TEACHER_MATRIX_PROB_SCALE = 0.650d;
    public static final double TEACHER_MATRIX_POSITION_PROB_SCALE = 0.620d;
    public static final double TEACHER_MATRIX_NUMBER_PROB_SCALE = 0.720d;
    public static final double TEACHER_MATRIX_STATE_PROB_SCALE = 0.520d;
    public static final double TEACHER_MATRIX_COMBO_PROB_SCALE = 0.580d;

    public static final double TEACHER_MATRIX_GATE_SCALE = 0.620d;
    public static final double TEACHER_MATRIX_POSITION_GATE_SCALE = 0.560d;
    public static final double TEACHER_MATRIX_NUMBER_GATE_SCALE = 0.660d;
    public static final double TEACHER_MATRIX_STATE_GATE_SCALE = 0.500d;
    public static final double TEACHER_MATRIX_COMBO_GATE_SCALE = 0.560d;

    public static final double TEACHER_MATRIX_MAX_GATE_ADJUST = TEACHER_MATRIX_MAX_GATE_BOOST;
    public static final double TEACHER_MATRIX_MIN_GATE_ADJUST = -TEACHER_MATRIX_MAX_GATE_REDUCE;
    public static final double TEACHER_MATRIX_GATE_ADJUST = -0.010d;
    public static final double TEACHER_MATRIX_STRONG_GATE_ADJUST = -0.016d;
    public static final double TEACHER_MATRIX_WEAK_GATE_ADJUST = 0.012d;

    public static final double TEACHER_MATRIX_RISK_WARN = 0.560d;
    public static final double TEACHER_MATRIX_RISK_HIGH = 0.680d;
    public static final double TEACHER_MATRIX_RISK_HARD = 0.780d;
    public static final double TEACHER_MATRIX_RISK_PENALTY = 0.012d;
    public static final double TEACHER_MATRIX_MAX_RISK_PENALTY = 0.026d;
    public static final double TEACHER_MATRIX_RISK_PENALTY_SCALE = 0.650d;

    public static final double TEACHER_MATRIX_WEIGHT_POSITION = 0.260d;
    public static final double TEACHER_MATRIX_WEIGHT_NUMBER = 0.320d;
    public static final double TEACHER_MATRIX_WEIGHT_COMBO = 0.260d;
    public static final double TEACHER_MATRIX_WEIGHT_STATE = 0.160d;

    public static final double TEACHER_MATRIX_STRIKE_HIT_RATE = 0.395d;
    public static final double TEACHER_MATRIX_STREAK_HIT_RATE = 0.405d;
    public static final double TEACHER_MATRIX_STREAK_HIT_BUFF = 0.006d;
    public static final int TEACHER_MATRIX_STREAK_MIN_SAMPLE = 18;
    public static final int TEACHER_MATRIX_STREAK_STRONG_SAMPLE = 40;

    public static final double TEACHER_MATRIX_NUMBER_HIT_RATE_GOOD = 0.120d;
    public static final double TEACHER_MATRIX_NUMBER_HIT_RATE_STRONG = 0.150d;
    public static final double TEACHER_MATRIX_NUMBER_HIT_RATE_WEAK = 0.080d;
    public static final double TEACHER_MATRIX_POSITION_HIT_RATE_GOOD = 0.100d;
    public static final double TEACHER_MATRIX_POSITION_HIT_RATE_STRONG = 0.130d;
    public static final double TEACHER_MATRIX_POSITION_HIT_RATE_WEAK = 0.070d;

    public static final double TEACHER_MATRIX_SCORE_POSITION_STRONG = 0.620d;
    public static final double TEACHER_MATRIX_SCORE_NUMBER_STRONG = 0.620d;
    public static final double TEACHER_MATRIX_SCORE_COMBO_STRONG = 0.620d;
    public static final double TEACHER_MATRIX_SCORE_STATE_STRONG = 0.620d;
    public static final double TEACHER_MATRIX_SCORE_POSITION_WEAK = 0.300d;
    public static final double TEACHER_MATRIX_SCORE_NUMBER_WEAK = 0.300d;
    public static final double TEACHER_MATRIX_SCORE_COMBO_WEAK = 0.300d;
    public static final double TEACHER_MATRIX_SCORE_STATE_WEAK = 0.300d;

    public static final String TEACHER_MATRIX_STATUS_NONE = "TM_NONE";
    public static final String TEACHER_MATRIX_STATUS_READY = "TM_READY";
    public static final String TEACHER_MATRIX_STATUS_WEAK = "TM_WEAK";
    public static final String TEACHER_MATRIX_STATUS_STALE = "TM_STALE";
    public static final String TEACHER_MATRIX_STATUS_APPLIED = "TM_APPLIED";
    public static final String TEACHER_MATRIX_STATUS_LEARNING = "TM_LEARNING";
    public static final String TEACHER_MATRIX_STATUS_ERROR = "TM_ERROR";
    public static final String TEACHER_MATRIX_STATUS_WAITING = "TM_WAITING";
    public static final String TEACHER_MATRIX_STATUS_RUNNING = "TM_RUNNING";
    public static final String TEACHER_MATRIX_STATUS_OK = "TM_OK";
    public static final String TEACHER_MATRIX_STATUS_RISK = "TM_RISK";
    public static final String TEACHER_MATRIX_STATUS_STRONG = "TM_STRONG";
    public static final String TEACHER_MATRIX_STATUS_ELITE = "TM_ELITE";

    public static final String TEACHER_MATRIX_HIT_SUPPORT = "TEACHER_MATRIX_HIT_SUPPORT";
    public static final String TEACHER_MATRIX_MISS_WARNING = "TEACHER_MATRIX_MISS_WARNING";
    public static final String TEACHER_MATRIX_BLOCK_RISK = "TEACHER_MATRIX_BLOCK_RISK";

    public static final int TEACHER_MATRIX_TRUE_SAMPLE = 1;
    public static final int TEACHER_MATRIX_FALSE_SAMPLE = 0;

    public static final long TEACHER_MATRIX_PROFILE_TTL_MS = 24L * 60L * 60L * 1000L;
    public static final long TEACHER_MATRIX_SAMPLE_KEEP_MS = 14L * 24L * 60L * 60L * 1000L;
    public static final int TEACHER_MATRIX_PROFILE_TTL_HOUR = 24;
    public static final int TEACHER_MATRIX_RAW_SAMPLE_KEEP = 50000;
    public static final int TEACHER_MATRIX_AUDIT_KEEP = 10000;
    public static final int TEACHER_MATRIX_BACKGROUND_MAX_MS = 6000;
    public static final int TEACHER_MATRIX_REALTIME_MAX_MS = 850;

    public static final String TEACHER_SAMPLE_POSITIVE = "POS";
    public static final String TEACHER_SAMPLE_NEGATIVE = "NEG";

    public static final boolean TEACHER_ASSIST_ENABLE = true;
    public static final boolean TEACHER_ASSIST_APPLY_TO_REALTIME = false;
    public static final boolean TEACHER_ASSIST_NEVER_BET_DIRECTLY = true;
    public static final boolean TEACHER_ASSIST_NEVER_BLOCK_REALTIME = true;
    public static final boolean TEACHER_ASSIST_USE_REPLAY_PROFILE = true;
    public static final boolean TEACHER_ASSIST_USE_MATRIX_PROFILE = true;

    public static final int TEACHER_ASSIST_MIN_SAMPLE = 30;
    public static final int TEACHER_ASSIST_STRONG_SAMPLE = 80;
    public static final int TEACHER_ASSIST_PROFILE_READ_LIMIT = 80;
    public static final int TEACHER_ASSIST_CANDIDATE_LIMIT = 18;

    public static final double TEACHER_ASSIST_MIN_CONFIDENCE = 0.360d;
    public static final double TEACHER_ASSIST_STRONG_CONFIDENCE = 0.520d;
    public static final double TEACHER_ASSIST_MIN_LIFT = 0.000d;
    public static final double TEACHER_ASSIST_STRONG_LIFT = 0.030d;
    public static final double TEACHER_ASSIST_RECENT_LIFT_MIN = -0.005d;
    public static final double TEACHER_ASSIST_MAX_CONFLICT_RATE = 0.660d;

    public static final double TEACHER_ASSIST_MAX_PROB_BOOST = 0.012d;
    public static final double TEACHER_ASSIST_MAX_PROB_PENALTY = 0.018d;
    public static final double TEACHER_ASSIST_MAX_GATE_REDUCE = 0.010d;
    public static final double TEACHER_ASSIST_MAX_GATE_BOOST = 0.012d;
    public static final double TEACHER_ASSIST_MAX_SORT_BOOST = 0.020d;
    public static final double TEACHER_ASSIST_MAX_SORT_PENALTY = 0.030d;

    public static final String TEACHER_ASSIST_STATUS_NONE = "TA_NONE";
    public static final String TEACHER_ASSIST_STATUS_READY = "TA_READY";
    public static final String TEACHER_ASSIST_STATUS_APPLIED = "TA_APPLIED";
    public static final String TEACHER_ASSIST_STATUS_RISK = "TA_RISK";
    public static final String TEACHER_ASSIST_STATUS_STALE = "TA_STALE";
    public static final String TEACHER_ASSIST_STATUS_ERROR = "TA_ERROR";

    public static final boolean AUTO_VALIDATE_ENABLE = true;
    public static final int AUTO_VALIDATE_DEFAULT_WINDOW = 120;
    public static final int AUTO_VALIDATE_WINDOW = AUTO_VALIDATE_DEFAULT_WINDOW;
    public static final int AUTO_VALIDATE_MIN_WINDOW = 60;
    public static final int AUTO_VALIDATE_MAX_WINDOW = 240;
    public static final int AUTO_VALIDATE_FALLBACK_WINDOW = 80;
    public static final int AUTO_VALIDATE_SAMPLE_MIN = 30;
    public static final int AUTO_VALIDATE_TIMEOUT_MS = 1200;
    public static final int AUTO_VALIDATE_HARD_TIMEOUT_MS = 1800;
    public static final boolean AUTO_VALIDATE_FAIL_OPEN = true;

    public static final double AUTO_VALIDATE_GATE_BOOST_FREQ_HIGH = 0.006d;
    public static final double AUTO_VALIDATE_GATE_BOOST_RECENT_MISS = 0.006d;
    public static final double AUTO_VALIDATE_GATE_BOOST_DRAWDOWN = 0.004d;
    public static final double AUTO_VALIDATE_GATE_REDUCE_STABLE = 0.004d;
    public static final double AUTO_VALIDATE_GATE_MAX_ADJUST = 0.018d;

    public static final double AUTO_VALIDATE_FREQ_WARN = 0.350d;
    public static final double AUTO_VALIDATE_FREQ_HARD = 0.500d;
    public static final double AUTO_VALIDATE_HIT_WARN = 0.330d;
    public static final double AUTO_VALIDATE_HIT_GOOD = 0.390d;

    public static final String AUTO_VALIDATE_STATUS_NONE = "NONE";
    public static final String AUTO_VALIDATE_STATUS_OK = "OK";
    public static final String AUTO_VALIDATE_STATUS_WARN = "WARN";
    public static final String AUTO_VALIDATE_STATUS_HARD = "HARD";
    public static final String AUTO_VALIDATE_STATUS_TIMEOUT = "TIMEOUT";
    public static final String AUTO_VALIDATE_STATUS_SAMPLE_WEAK = "SAMPLE_WEAK";
    public static final String AUTO_VALIDATE_STATUS_ERROR = "ERROR";

    public static final boolean TEACHER_ENABLE = true;
    public static final boolean TEACHER_APPLY_TO_REALTIME = false;
    public static final boolean TEACHER_NEVER_BLOCK_REALTIME = true;
    public static final int TEACHER_MIN_SAMPLE = 120;
    public static final int TEACHER_STRONG_SAMPLE = 500;
    public static final long TEACHER_SNAPSHOT_EXPIRE_MS = 6L * 60L * 60L * 1000L;
    public static final long TEACHER_SNAPSHOT_TTL_MS = TEACHER_SNAPSHOT_EXPIRE_MS;
    public static final double TEACHER_MAX_GATE_ADJUST = 0.018d;
    public static final double TEACHER_MAX_PROB_ADJUST = 0.020d;
    public static final double TEACHER_MAX_WEIGHT_DELTA = 0.030d;
    public static final double TEACHER_MIN_CONFIDENCE = 0.420d;
    public static final double TEACHER_STRONG_CONFIDENCE = 0.620d;
    public static final String TEACHER_STATUS_NONE = "TEACHER_NONE";
    public static final String TEACHER_STATUS_READY = "TEACHER_READY";
    public static final String TEACHER_STATUS_STALE = "TEACHER_STALE";
    public static final String TEACHER_STATUS_WEAK = "TEACHER_WEAK";
    public static final String TEACHER_STATUS_APPLIED = "TEACHER_APPLIED";

    public static final int BACKTEST_MICRO_WINDOW = 60;
    public static final int BACKTEST_MID_WINDOW = 120;
    public static final int BACKTEST_BIG_WINDOW = 500;
    public static final int BACKTEST_DEEP_WINDOW = 1000;

    public static final int AUTO_BACKTEST_LIGHT_WINDOW = 120;
    public static final int AUTO_BACKTEST_LIGHT_MIN_WINDOW = 60;
    public static final int AUTO_BACKTEST_LIGHT_MAX_WINDOW = 240;
    public static final int AUTO_BACKTEST_HEALTH_WINDOW = 500;
    public static final int AUTO_BACKTEST_HEALTH_MAX_WINDOW = 1000;
    public static final boolean AUTO_BACKTEST_ALLOW_DEEP_WINDOW = false;

    public static final String BACKTEST_MODE_FAST = "FAST";
    public static final String BACKTEST_MODE_NORMAL = "NORMAL";
    public static final String BACKTEST_MODE_FULL = "FULL";

    public static final int BACKTEST_MODE_ID_FAST = 1;
    public static final int BACKTEST_MODE_ID_NORMAL = 2;
    public static final int BACKTEST_MODE_ID_FULL = 3;

    public static final int BACKTEST_TRAIN_LOOKBACK_FAST = 240;
    public static final int BACKTEST_TRAIN_LOOKBACK_NORMAL = 500;
    public static final int BACKTEST_TRAIN_LOOKBACK_FULL = 1000;

    public static final int BACKTEST_PAUSE_EVERY_STEPS_FAST = 20;
    public static final int BACKTEST_PAUSE_EVERY_STEPS_NORMAL = 12;
    public static final int BACKTEST_PAUSE_EVERY_STEPS_FULL = 5;

    public static final long BACKTEST_COOPERATIVE_SLEEP_FAST_MS = 1L;
    public static final long BACKTEST_COOPERATIVE_SLEEP_NORMAL_MS = 3L;
    public static final long BACKTEST_COOPERATIVE_SLEEP_FULL_MS = 8L;

    public static final String BACKTEST_STATUS_IDLE = "IDLE";
    public static final String BACKTEST_STATUS_WAITING = "WAITING";
    public static final String BACKTEST_STATUS_RUNNING = "RUNNING";
    public static final String BACKTEST_STATUS_YIELDING = "YIELDING";
    public static final String BACKTEST_STATUS_APPLYING = "APPLYING";
    public static final String BACKTEST_STATUS_OK = "OK";
    public static final String BACKTEST_STATUS_ERROR = "ERROR";
    public static final String BACKTEST_STATUS_CANCELLED = "CANCELLED";
    public static final String BACKTEST_STATUS_SKIPPED = "SKIP";

    public static final int MANUAL_BACKTEST_DEFAULT_WINDOW = 500;
    public static final int MANUAL_BACKTEST_MIN_WINDOW = 300;
    public static final int MANUAL_BACKTEST_MAX_WINDOW = 3000;
    public static final int MANUAL_BACKTEST_SAVE_PROGRESS_EVERY_STEPS = 10;
    public static final int MANUAL_BACKTEST_BROADCAST_EVERY_STEPS = 5;
    public static final int MANUAL_BACKTEST_MAX_SLICE_STEPS = 8;
    public static final long MANUAL_BACKTEST_SLICE_MAX_RUNNING_MS = 180L;
    public static final long MANUAL_BACKTEST_SLICE_DELAY_MS = 300L;
    public static final boolean MANUAL_BACKTEST_INDEPENDENT_CHANNEL = true;
    public static final boolean MANUAL_BACKTEST_NEVER_BLOCK_REALTIME = true;

    public static final boolean AUTO_BACKTEST_DEDUP_ENABLE = true;
    public static final String AUTO_BACKTEST_DEDUP_PREFIX = "ABT";
    public static final int AUTO_BACKTEST_DEDUP_KEEP_RECENT = 200;
    public static final long AUTO_BACKTEST_DEDUP_EXPIRE_MS = 24L * 60L * 60L * 1000L;

    public static final boolean AUTO_BACKTEST_FEED_LEARNING = true;
    public static final boolean AUTO_BACKTEST_FEED_TEACHER = true;
    public static final boolean AUTO_BACKTEST_FEED_GATE = false;
    public static final boolean AUTO_BACKTEST_FEED_PREDRAW_REASON = true;

    public static final double LEARN_AUTO_BACKTEST_WEIGHT = 0.28d;
    public static final double LEARN_MANUAL_BACKTEST_WEIGHT = 0.42d;
    public static final double LEARN_REALTIME_VALIDATE_WEIGHT = 0.35d;

    public static final int SCAN_REALTIME_KEEP_PER_POSITION = 18;
    public static final int SCAN_REALTIME_KEEP_TOTAL = 160;
    public static final int SCAN_BACKTEST_FULL_TOTAL = 480;

    public static final int PROB_REALTIME_ENRICH_LIMIT = 80;
    public static final int PROB_REALTIME_DEEP_LIMIT = 28;
    public static final int PROB_BACKTEST_FAST_ENRICH_LIMIT = 60;
    public static final int PROB_BACKTEST_FAST_DEEP_LIMIT = 20;
    public static final int PROB_BACKTEST_NORMAL_ENRICH_LIMIT = 120;
    public static final int PROB_BACKTEST_NORMAL_DEEP_LIMIT = 40;
    public static final int PROB_BACKTEST_FULL_ENRICH_LIMIT = 240;
    public static final int PROB_BACKTEST_FULL_DEEP_LIMIT = 80;
    public static final int PROB_MANUAL_FULL_ENRICH_LIMIT = 240;
    public static final int PROB_MANUAL_FULL_DEEP_LIMIT = 80;

    public static final int SHADOW_SAVE_TOP_N = 80;
    public static final int HISTORY_SHOW_LIMIT = 30;
    public static final int DRAW_HISTORY_SHOW_LIMIT = 60;
    public static final int REASON_SHOW_LIMIT = 18;

    public static final boolean UI_LONG_TEXT_SCROLL_ENABLE = true;
    public static final boolean UI_LONG_TEXT_EXPAND_ENABLE = true;
    public static final boolean UI_CARD_INNER_SCROLL_ENABLE = true;
    public static final boolean UI_REASON_CARD_SCROLL_ENABLE = true;
    public static final boolean UI_LEARNING_CARD_SCROLL_ENABLE = true;
    public static final boolean UI_BACKTEST_CARD_SCROLL_ENABLE = true;
    public static final boolean UI_TEACHER_CARD_SCROLL_ENABLE = true;
    public static final boolean UI_DEBUG_CARD_SCROLL_ENABLE = true;

    public static final int UI_REASON_COLLAPSED_MAX_CHARS = 420;
    public static final int UI_REASON_EXPANDED_MAX_CHARS = 3600;
    public static final int UI_LEARNING_COLLAPSED_MAX_CHARS = 360;
    public static final int UI_LEARNING_EXPANDED_MAX_CHARS = 3000;
    public static final int UI_BACKTEST_COLLAPSED_MAX_CHARS = 320;
    public static final int UI_BACKTEST_EXPANDED_MAX_CHARS = 2600;
    public static final int UI_TEACHER_COLLAPSED_MAX_CHARS = 360;
    public static final int UI_TEACHER_EXPANDED_MAX_CHARS = 3200;
    public static final int UI_STATUS_COLLAPSED_MAX_CHARS = 260;
    public static final int UI_STATUS_EXPANDED_MAX_CHARS = 1800;

    public static final int UI_REASON_CARD_MAX_LINES_COLLAPSED = 10;
    public static final int UI_REASON_CARD_MAX_LINES_EXPANDED = 100;
    public static final int UI_LEARNING_CARD_MAX_LINES_COLLAPSED = 8;
    public static final int UI_LEARNING_CARD_MAX_LINES_EXPANDED = 90;
    public static final int UI_BACKTEST_CARD_MAX_LINES_COLLAPSED = 7;
    public static final int UI_BACKTEST_CARD_MAX_LINES_EXPANDED = 80;
    public static final int UI_TEACHER_CARD_MAX_LINES_COLLAPSED = 8;
    public static final int UI_TEACHER_CARD_MAX_LINES_EXPANDED = 90;

    public static final int UI_INNER_SCROLL_MAX_HEIGHT_DP = 220;
    public static final int UI_INNER_SCROLL_MIN_HEIGHT_DP = 88;
    public static final int UI_TEXT_REFRESH_MIN_INTERVAL_MS = 700;
    public static final int UI_HEAVY_REFRESH_MIN_INTERVAL_MS = 1200;

    public static final String UI_TEXT_EXPAND_HINT = "点击展开/收起，长文本可上下滑动";
    public static final String UI_TEXT_COLLAPSED_SUFFIX = " ……";

    public static final int FETCH_FAST_WINDOW_SEC = 65;
    public static final int FETCH_MID_WINDOW_SEC = 120;
    public static final int FETCH_FAST_INTERVAL_SEC = 1;
    public static final int FETCH_MID_INTERVAL_SEC = 3;
    public static final int FETCH_SLOW_INTERVAL_SEC = 10;

    public static final int REALTIME_OPEN_PREPARE_SEC = 3;
    public static final int REALTIME_OPEN_AFTER_SEC = 60;
    public static final int REALTIME_MAX_FETCH_PER_ISSUE = 60;
    public static final long REALTIME_FETCH_INTERVAL_MS = 1000L;
    public static final long REALTIME_MIN_LOOP_INTERVAL_MS = 800L;
    public static final int REALTIME_HISTORY100_EVERY_N_FETCH = 10;
    public static final int REALTIME_FORCE_HISTORY100_AFTER_MISS = 8;
    public static final int REALTIME_FORCE_BACKFILL_AFTER_MISS = 30;
    public static final long REALTIME_STALE_WARN_MS = 2L * 60L * 1000L;

    public static final long BACKFILL_NORMAL_INTERVAL_MS = 10L * 60L * 1000L;
    public static final long BACKFILL_FAST_RETRY_MS = 2L * 60L * 1000L;
    public static final int BACKFILL_LATEST_LIMIT = 100;

    public static final long BACKTEST_FAST_INTERVAL_MS = 6L * 60L * 1000L;
    public static final long BACKTEST_NORMAL_INTERVAL_MS = 18L * 60L * 1000L;
    public static final long BACKTEST_FULL_MIN_INTERVAL_MS = 2L * 60L * 60L * 1000L;
    public static final long AUTO_BACKTEST_HEALTH_INTERVAL_MS = 30L * 60L * 1000L;

    public static final long BACKTEST_IDLE_AFTER_REALTIME_MS = 75L * 1000L;
    public static final long BACKTEST_MIN_GAP_AFTER_FETCH_MS = 20L * 1000L;
    public static final long BACKTEST_MAX_SINGLE_RUN_MS = 45000L;

    public static final boolean BACKTEST_AUTO_FAST_ENABLE = true;
    public static final boolean BACKTEST_AUTO_NORMAL_ENABLE = true;
    public static final boolean BACKTEST_AUTO_FULL_ENABLE = false;
    public static final boolean BACKTEST_AUTO_DEEP_ENABLE = false;
    public static final int BACKTEST_MAX_PARALLEL_TASK = 1;

    public static final int SCHED_BLOCK_AUTO_NORMAL_BEFORE_OPEN_SECONDS = 75;
    public static final int SCHED_BLOCK_AUTO_FAST_BEFORE_OPEN_SECONDS = 45;
    public static final int SCHED_BLOCK_MANUAL_BEFORE_OPEN_SECONDS = 0;
    public static final int SCHED_REALTIME_PRIORITY_AFTER_OPEN_SECONDS = 60;

    public static final long SCHED_REALTIME_MIN_INTERVAL_MS = 650L;
    public static final long SCHED_AUTO_FAST_MIN_INTERVAL_MS = 3500L;
    public static final long SCHED_AUTO_NORMAL_MIN_INTERVAL_MS = 8000L;
    public static final long SCHED_MANUAL_SLICE_MIN_INTERVAL_MS = 300L;

    public static final long TICK_INTERVAL_MS = 1000L;
    public static final long SERVICE_LOOP_MIN_INTERVAL_MS = 800L;
    public static final long WAKE_LOCK_TIMEOUT_MS = 2L * 60L * 1000L;

    public static final int NOTIFICATION_ID = 32011;
    public static final String NOTIFICATION_CHANNEL_ID = "eth_k3_nova_one_pro_channel";
    public static final String NOTIFICATION_CHANNEL_NAME = "星核天枢 Binance K3";

    public static final String ACTION_SERVICE_START = PACKAGE_NAME + ".ACTION_SERVICE_START";
    public static final String ACTION_SERVICE_STOP = PACKAGE_NAME + ".ACTION_SERVICE_STOP";
    public static final String ACTION_REFRESH_NOW = PACKAGE_NAME + ".ACTION_REFRESH_NOW";
    public static final String ACTION_SERVICE_TICK = PACKAGE_NAME + ".ACTION_SERVICE_TICK";
    public static final String ACTION_STATE_CHANGED = PACKAGE_NAME + ".ACTION_STATE_CHANGED";
    public static final String ACTION_DECISION_UPDATED = PACKAGE_NAME + ".ACTION_DECISION_UPDATED";
    public static final String ACTION_BACKTEST_UPDATED = PACKAGE_NAME + ".ACTION_BACKTEST_UPDATED";
    public static final String ACTION_BACKTEST_FULL_REQUEST =
            PACKAGE_NAME + ".ACTION_BACKTEST_FULL_REQUEST";
    public static final String ACTION_BOOT_START = PACKAGE_NAME + ".ACTION_BOOT_START";
    public static final String ACTION_BACKTEST_CANCEL_REQUEST =
            PACKAGE_NAME + ".ACTION_BACKTEST_CANCEL_REQUEST";
    public static final String ACTION_BACKTEST_FULL_CANCEL_REQUEST =
            PACKAGE_NAME + ".ACTION_BACKTEST_FULL_CANCEL_REQUEST";
    public static final String ACTION_BACKTEST_PROGRESS_UPDATED =
            PACKAGE_NAME + ".ACTION_BACKTEST_PROGRESS_UPDATED";
    public static final String ACTION_LEARNING_UPDATED = PACKAGE_NAME + ".ACTION_LEARNING_UPDATED";
    public static final String ACTION_AUDIT_UPDATED = PACKAGE_NAME + ".ACTION_AUDIT_UPDATED";
    public static final String ACTION_AI_JUDGE_UPDATED = PACKAGE_NAME + ".ACTION_AI_JUDGE_UPDATED";
    public static final String ACTION_SELF_DIAGNOSIS_UPDATED =
            PACKAGE_NAME + ".ACTION_SELF_DIAGNOSIS_UPDATED";
    public static final String ACTION_PROMOTION_UPDATED =
            PACKAGE_NAME + ".ACTION_PROMOTION_UPDATED";
    public static final String ACTION_A_PLUS_UPDATED = PACKAGE_NAME + ".ACTION_A_PLUS_UPDATED";
    public static final String ACTION_BIAS_EVIDENCE_UPDATED =
            PACKAGE_NAME + ".ACTION_BIAS_EVIDENCE_UPDATED";
    public static final String ACTION_ATTRIBUTION_UPDATED =
            PACKAGE_NAME + ".ACTION_ATTRIBUTION_UPDATED";
    public static final String ACTION_TEACHER_UPDATED = PACKAGE_NAME + ".ACTION_TEACHER_UPDATED";
    public static final String ACTION_REALTIME_VALIDATE_UPDATED =
            PACKAGE_NAME + ".ACTION_REALTIME_VALIDATE_UPDATED";
    public static final String ACTION_AUTO_LEARN_UPDATED =
            PACKAGE_NAME + ".ACTION_AUTO_LEARN_UPDATED";
    public static final String ACTION_RUNTIME_PROFILE_UPDATED =
            PACKAGE_NAME + ".ACTION_RUNTIME_PROFILE_UPDATED";
    public static final String ACTION_TEACHER_MATRIX_UPDATED =
            PACKAGE_NAME + ".ACTION_TEACHER_MATRIX_UPDATED";
    public static final String ACTION_TEACHER_REPLAY_UPDATED =
            PACKAGE_NAME + ".ACTION_TEACHER_REPLAY_UPDATED";
    public static final String ACTION_TEACHER_ASSIST_UPDATED =
            PACKAGE_NAME + ".ACTION_TEACHER_ASSIST_UPDATED";
    public static final String ACTION_DRAWS_UPDATED = PACKAGE_NAME + ".ACTION_DRAWS_UPDATED";
    public static final String ACTION_SETTLEMENT_UPDATED =
            PACKAGE_NAME + ".ACTION_SETTLEMENT_UPDATED";
    public static final String ACTION_PREDRAW_REASON_UPDATED =
            PACKAGE_NAME + ".ACTION_PREDRAW_REASON_UPDATED";
    public static final String ACTION_CONTRASTIVE_REASON_UPDATED =
            PACKAGE_NAME + ".ACTION_CONTRASTIVE_REASON_UPDATED";
    public static final String ACTION_CAUSAL_REASON_UPDATED =
            PACKAGE_NAME + ".ACTION_CAUSAL_REASON_UPDATED";
    public static final String ACTION_K3_ROLE_COMBO_UPDATED =
            PACKAGE_NAME + ".ACTION_K3_ROLE_COMBO_UPDATED";
    public static final String ACTION_ELITE45_UPDATED = PACKAGE_NAME + ".ACTION_ELITE45_UPDATED";
    public static final String ACTION_V20_CAUSE_CHAIN_UPDATED =
            PACKAGE_NAME + ".ACTION_V20_CAUSE_CHAIN_UPDATED";
    public static final String ACTION_V20_SHADOW_HEALTH_UPDATED =
            PACKAGE_NAME + ".ACTION_V20_SHADOW_HEALTH_UPDATED";
    public static final String ACTION_V20_FREEZE_UPDATED =
            PACKAGE_NAME + ".ACTION_V20_FREEZE_UPDATED";

    public static final String EXTRA_STATUS = "extra_status";
    public static final String EXTRA_COUNTDOWN_MS = "extra_countdown_ms";
    public static final String EXTRA_CURRENT_ISSUE = "extra_current_issue";
    public static final String EXTRA_TARGET_ISSUE = "extra_target_issue";
    public static final String EXTRA_DECISION = "extra_decision";
    public static final String EXTRA_BACKTEST_MODE = "extra_backtest_mode";
    public static final String EXTRA_REASON = "extra_reason";
    public static final String EXTRA_FORCE = "extra_force";
    public static final String EXTRA_BACKTEST_STATUS = "extra_backtest_status";
    public static final String EXTRA_BACKTEST_PROGRESS = "extra_backtest_progress";
    public static final String EXTRA_BACKTEST_SUMMARY = "extra_backtest_summary";
    public static final String EXTRA_BACKTEST_ERROR = "extra_backtest_error";
    public static final String EXTRA_LEARNING_SUMMARY = "extra_learning_summary";
    public static final String EXTRA_AUDIT_SUMMARY = "extra_audit_summary";
    public static final String EXTRA_BIAS_SUMMARY = "extra_bias_summary";
    public static final String EXTRA_AI_JUDGE_SUMMARY = "extra_ai_judge_summary";
    public static final String EXTRA_SELF_DIAGNOSIS_SUMMARY = "extra_self_diagnosis_summary";
    public static final String EXTRA_PROMOTION_SUMMARY = "extra_promotion_summary";
    public static final String EXTRA_A_PLUS_SUMMARY = "extra_a_plus_summary";
    public static final String EXTRA_ATTRIBUTION_SUMMARY = "extra_attribution_summary";
    public static final String EXTRA_SCHEDULER_SUMMARY = "extra_scheduler_summary";
    public static final String EXTRA_TEACHER_SUMMARY = "extra_teacher_summary";
    public static final String EXTRA_TEACHER_MATRIX_SUMMARY = "extra_teacher_matrix_summary";
    public static final String EXTRA_TEACHER_REPLAY_SUMMARY = "extra_teacher_replay_summary";
    public static final String EXTRA_TEACHER_ASSIST_SUMMARY = "extra_teacher_assist_summary";
    public static final String EXTRA_REALTIME_VALIDATE_SUMMARY = "extra_realtime_validate_summary";
    public static final String EXTRA_AUTO_LEARN_SUMMARY = "extra_auto_learn_summary";
    public static final String EXTRA_RUNTIME_PROFILE_SUMMARY = "extra_runtime_profile_summary";
    public static final String EXTRA_DRAW_UPDATED_AT = "extra_draw_updated_at";
    public static final String EXTRA_SETTLEMENT_SUMMARY = "extra_settlement_summary";
    public static final String EXTRA_PREDRAW_REASON_SUMMARY = "extra_predraw_reason_summary";
    public static final String EXTRA_CONTRASTIVE_REASON_SUMMARY =
            "extra_contrastive_reason_summary";
    public static final String EXTRA_CAUSAL_REASON_SUMMARY = "extra_causal_reason_summary";
    public static final String EXTRA_K3_ROLE_COMBO_SUMMARY = "extra_k3_role_combo_summary";
    public static final String EXTRA_ELITE45_SUMMARY = "extra_elite45_summary";
    public static final String EXTRA_V20_CAUSE_CHAIN_SUMMARY = "extra_v20_cause_chain_summary";
    public static final String EXTRA_V20_SHADOW_HEALTH_SUMMARY = "extra_v20_shadow_health_summary";
    public static final String EXTRA_V20_FREEZE_SUMMARY = "extra_v20_freeze_summary";

    public static final String DECISION_BET = "BET";
    public static final String DECISION_HOLD = "HOLD";
    public static final String DECISION_SHADOW = "SHADOW";
    public static final String ACTION_BET = DECISION_BET;
    public static final String ACTION_HOLD = DECISION_HOLD;
    public static final String ACTION_SHADOW = DECISION_SHADOW;

    public static final String RESULT_WAIT = "WAIT";
    public static final String RESULT_PENDING = RESULT_WAIT;
    public static final String RESULT_WIN = "WIN";
    public static final String RESULT_HIT = RESULT_WIN;
    public static final String RESULT_MISS = "MISS";
    public static final String RESULT_LOSE = RESULT_MISS;
    public static final String RESULT_VOID = "VOID";
    public static final String RESULT_DATA_ERROR = "DATA_ERROR";
    public static final String RESULT_DATA_OFFSET = "DATA_OFFSET";
    public static final String RESULT_UNKNOWN = "UNKNOWN";

    public static final String STAT_SCOPE_REAL_TRADE = "REAL_TRADE";
    public static final String STAT_SCOPE_REAL = STAT_SCOPE_REAL_TRADE;
    public static final String STAT_SCOPE_SHADOW = "SHADOW";
    public static final String STAT_SCOPE_HOLD = "HOLD";
    public static final String STAT_SCOPE_BACKTEST = "BACKTEST";
    public static final String STAT_SCOPE_MANUAL_BACKTEST = "MANUAL_BACKTEST";
    public static final String STAT_SCOPE_TEACHER = "TEACHER";
    public static final String STAT_SCOPE_TEACHER_REPLAY = "TEACHER_REPLAY";
    public static final String STAT_SCOPE_AUTO_VALIDATE = "AUTO_VALIDATE";
    public static final String STAT_SCOPE_AUTO_LEARN = "AUTO_LEARN";
    public static final String STAT_SCOPE_TEACHER_MATRIX = "TEACHER_MATRIX";
    public static final String STAT_SCOPE_TEACHER_ASSIST = "TEACHER_ASSIST";

    public static final String SERVICE_STATUS_RUNNING = "RUNNING";
    public static final String SERVICE_STATUS_STOPPED = "STOPPED";
    public static final String SERVICE_STATUS_FETCHING = "FETCHING";
    public static final String SERVICE_STATUS_ANALYZING = "ANALYZING";
    public static final String SERVICE_STATUS_SETTLING = "SETTLING";
    public static final String SERVICE_STATUS_LEARNING = "LEARNING";
    public static final String SERVICE_STATUS_BACKTESTING = "BACKTESTING";
    public static final String SERVICE_STATUS_BACKTEST_FAST = "BACKTEST_FAST";
    public static final String SERVICE_STATUS_BACKTEST_NORMAL = "BACKTEST_NORMAL";
    public static final String SERVICE_STATUS_BACKTEST_FULL = "BACKTEST_FULL";
    public static final String SERVICE_STATUS_WAITING = "WAITING";
    public static final String SERVICE_STATUS_DELAY = "DELAY";
    public static final String SERVICE_STATUS_API_ERROR = "API_ERROR";
    public static final String SERVICE_STATUS_DATA_ERROR = "DATA_ERROR";
    public static final String SERVICE_STATUS_AI_JUDGING = "AI_JUDGING";
    public static final String SERVICE_STATUS_DEEP_ANALYZING = "DEEP_ANALYZING";
    public static final String SERVICE_STATUS_DIAGNOSING = "DIAGNOSING";
    public static final String SERVICE_STATUS_VALIDATING = "VALIDATING";
    public static final String SERVICE_STATUS_AUTO_LEARNING = "AUTO_LEARNING";
    public static final String SERVICE_STATUS_TEACHER_MATRIX = "TEACHER_MATRIX";
    public static final String SERVICE_STATUS_TEACHER_REPLAY = "TEACHER_REPLAY";
    public static final String SERVICE_STATUS_TEACHER_ASSIST = "TEACHER_ASSIST";
    public static final String SERVICE_STATUS_PREDRAW_REASON = "PREDRAW_REASON";
    public static final String SERVICE_STATUS_CONTRASTIVE_REASON = "CONTRASTIVE_REASON";
    public static final String SERVICE_STATUS_CAUSAL_REASON = "CAUSAL_REASON";
    public static final String SERVICE_STATUS_K3_ROLE_COMBO = "K3_ROLE_COMBO";
    public static final String SERVICE_STATUS_ELITE45 = "ELITE45";
    public static final String SERVICE_STATUS_V20_CAUSE_CHAIN = "V20_CAUSE_CHAIN";

    public static final String MODE_UNKNOWN = "UNKNOWN";
    public static final String MODE_FORMING = "FORMING";
    public static final String MODE_CONFIRMED = "CONFIRMED";
    public static final String MODE_ACCELERATING = "ACCELERATING";
    public static final String MODE_PULLBACK = "PULLBACK";
    public static final String MODE_RECOVERING = "RECOVERING";
    public static final String MODE_WEAKENING = "WEAKENING";
    public static final String MODE_BROKEN = "BROKEN";
    public static final String MODE_REVERSING = "REVERSING";
    public static final String MODE_REVERSAL = "REVERSAL";
    public static final String MODE_CHAOS = "CHAOS";
    public static final String MODE_STABLE = "STABLE";
    public static final String MODE_TRANSITION = "TRANSITION";

    public static final String STATE_TREND_HOT_UP = "HOT_UP";
    public static final String STATE_TREND_HOT_DECAY = "HOT_DECAY";
    public static final String STATE_TREND_COLD_RECOVER = "COLD_RECOVER";
    public static final String STATE_TREND_COLD_DEEP = "COLD_DEEP";
    public static final String STATE_DRIFT_LOW = "DRIFT_LOW";
    public static final String STATE_DRIFT_MID = "DRIFT_MID";
    public static final String STATE_DRIFT_HIGH = "DRIFT_HIGH";
    public static final String STATE_BREAK_NONE = "BREAK_NONE";
    public static final String STATE_BREAK_WARN = "BREAK_WARN";
    public static final String STATE_BREAK_HARD = "BREAK_HARD";
    public static final String STATE_RECOVERY_WEAK = "RECOVERY_WEAK";
    public static final String STATE_RECOVERY_MID = "RECOVERY_MID";
    public static final String STATE_RECOVERY_STRONG = "RECOVERY_STRONG";
    public static final String STATE_SHAPE_LOW = "LOW_ZONE";
    public static final String STATE_SHAPE_MID = "MID_ZONE";
    public static final String STATE_SHAPE_HIGH = "HIGH_ZONE";
    public static final String STATE_SHAPE_ODD = "ODD";
    public static final String STATE_SHAPE_EVEN = "EVEN";
    public static final String STATE_SHAPE_NEIGHBOR = "NEIGHBOR";
    public static final String STATE_SHAPE_REPEAT = "REPEAT";
    public static final String STATE_SHAPE_BACKFILL = "BACKFILL";
    public static final String STATE_SHAPE_REVERSE = "REVERSE";

    public static final String PREDRAW_LOW_DRIFT = "LOW_DRIFT";
    public static final String PREDRAW_DRIFT_RISING = "DRIFT_RISING";
    public static final String PREDRAW_POS_UP = "POS_UP";
    public static final String PREDRAW_POS_DECAY = "POS_DECAY";
    public static final String PREDRAW_POSITION_CONVERGING = "POSITION_CONVERGING";
    public static final String PREDRAW_POSITION_NOISE_HIGH = "POSITION_NOISE_HIGH";
    public static final String PREDRAW_GAP_RETURN_VALID = "GAP_RETURN_VALID";
    public static final String PREDRAW_GAP_OVERDUE_NOISE = "GAP_OVERDUE_NOISE";
    public static final String PREDRAW_TRANSITION_SUPPORT = "TRANSITION_SUPPORT";
    public static final String PREDRAW_TRANSITION_CONFLICT = "TRANSITION_CONFLICT";
    public static final String PREDRAW_COMBO_LOW_CONFLICT = "COMBO_LOW_CONFLICT";
    public static final String PREDRAW_COMBO_ROLE_CONFLICT = "COMBO_ROLE_CONFLICT";
    public static final String PREDRAW_RECOVERY_VALID = "RECOVERY_VALID";
    public static final String PREDRAW_FALSE_RECOVERY = "FALSE_RECOVERY";
    public static final String PREDRAW_MID_STABLE = "MID_STABLE";
    public static final String PREDRAW_HOT_EXHAUSTED = "HOT_EXHAUSTED";
    public static final String PREDRAW_ENTROPY_STABLE_OR_DOWN = "ENTROPY_STABLE_OR_DOWN";
    public static final String PREDRAW_ENTROPY_RISING = "ENTROPY_RISING";

    public static final String K3_ROLE_MAIN = "MAIN_ATTACK";
    public static final String K3_ROLE_SUPPORT = "SUPPORT";
    public static final String K3_ROLE_DEFENSE = "DEFENSE";
    public static final String K3_ROLE_UNKNOWN = "UNKNOWN";

    public static final String A_PLUS_NONE = "A_PLUS_NONE";
    public static final String A_PLUS_WATCH = "A_PLUS_WATCH";
    public static final String A_PLUS_READY = "A_PLUS_READY";
    public static final String A_PLUS_ACTIVE = "A_PLUS_ACTIVE";
    public static final String A_PLUS_STALE = "A_PLUS_STALE";
    public static final String A_PLUS_RECOVERING = "A_PLUS_RECOVERING";
    public static final String A_PLUS_CANDIDATE = "A_PLUS_CANDIDATE";

    public static final double A_PLUS_MIN_FINAL_P = 0.400d;
    public static final double A_PLUS_MIN_DEEP_SCORE = 0.700d;
    public static final double A_PLUS_MIN_JUDGE_SCORE = 0.720d;
    public static final double A_PLUS_MIN_CONFIDENCE = 0.650d;
    public static final double A_PLUS_MAX_DRIFT_RISK = 0.420d;
    public static final double A_PLUS_MAX_BREAK_RISK = 0.420d;
    public static final double A_PLUS_MAX_AUDIT_ERROR = 0.045d;
    public static final double A_PLUS_MIN_CHAMPION_GAP = 0.040d;

    public static final String PROMOTION_CANDIDATE = "CANDIDATE";
    public static final String PROMOTION_CHALLENGER = "CHALLENGER";
    public static final String PROMOTION_SHADOW_OBSERVING = "SHADOW_OBSERVING";
    public static final String PROMOTION_PROMOTED = "PROMOTED";
    public static final String PROMOTION_LIMITED_ACTIVE = "LIMITED_ACTIVE";
    public static final String PROMOTION_ACTIVE = "ACTIVE";
    public static final String PROMOTION_STALE = "STALE";
    public static final String PROMOTION_REJECTED = "REJECTED";

    public static final int PROMOTION_BASE_SAMPLE_MIN = 30;
    public static final int PROMOTION_BASE_RECENT_SAMPLE_MIN = 12;
    public static final double PROMOTION_BASE_HIT_RATE_MIN = 0.330d;
    public static final double PROMOTION_BASE_RECENT_HIT_RATE_MIN = 0.330d;
    public static final double PROMOTION_BASE_CALIBRATION_ERROR_MAX = 0.090d;

    public static final int PROMOTION_STRONG_SAMPLE_MIN = 60;
    public static final int PROMOTION_STRONG_RECENT_SAMPLE_MIN = 24;
    public static final double PROMOTION_STRONG_HIT_RATE_MIN = 0.360d;
    public static final double PROMOTION_STRONG_RECENT_HIT_RATE_MIN = 0.370d;
    public static final double PROMOTION_STRONG_CALIBRATION_ERROR_MAX = 0.060d;

    public static final int PROMOTION_A_PLUS_SAMPLE_MIN = 100;
    public static final int PROMOTION_A_PLUS_RECENT_SAMPLE_MIN = 40;
    public static final double PROMOTION_A_PLUS_HIT_RATE_MIN = 0.400d;
    public static final double PROMOTION_A_PLUS_RECENT_HIT_RATE_MIN = 0.400d;
    public static final double PROMOTION_A_PLUS_CALIBRATION_ERROR_MAX = 0.045d;

    public static final int PROB_AUDIT_BUCKET_COUNT = 20;
    public static final int PROB_AUDIT_SAMPLE_MIN = 24;
    public static final int PROB_AUDIT_RECENT_SAMPLE_MIN = 10;
    public static final double PROB_AUDIT_ERROR_WARN = 0.060d;
    public static final double PROB_AUDIT_ERROR_HARD = 0.110d;
    public static final double PROB_AUDIT_MAX_BOOST = 0.014d;
    public static final double PROB_AUDIT_MAX_PENALTY = 0.020d;

    public static final String PROB_BUCKET_OK = "OK";
    public static final String PROB_BUCKET_UNDER = "UNDER";
    public static final String PROB_BUCKET_OVER = "OVER";
    public static final String PROB_BUCKET_DANGEROUS_OVER = "DANGEROUS_OVER";
    public static final String PROB_BUCKET_SAMPLE_WEAK = "SAMPLE_WEAK";

    public static final String BIAS_STATUS_STRONG = "STRONG";
    public static final String BIAS_STATUS_WEAK = "WEAK";
    public static final String BIAS_STATUS_NOISY = "NOISY";
    public static final String BIAS_STATUS_NONE = "NONE";

    public static final double BIAS_SCORE_STRONG = 0.600d;
    public static final double BIAS_SCORE_WEAK = 0.420d;
    public static final double BIAS_CONFIDENCE_MIN = 0.420d;
    public static final double BIAS_CONFIDENCE_STRONG = 0.600d;

    public static final String AI_JUDGE_BET = "AI_BET";
    public static final String AI_JUDGE_HOLD = "AI_HOLD";
    public static final String AI_JUDGE_SHADOW = "AI_SHADOW";
    public static final String AI_JUDGE_CONFLICT = "AI_CONFLICT";
    public static final String AI_JUDGE_RISK = "AI_RISK";

    public static final int AI_MODEL_TREND = 1;
    public static final int AI_MODEL_STATE = 2;
    public static final int AI_MODEL_NUMBER = 3;
    public static final int AI_MODEL_POSITION = 4;
    public static final int AI_MODEL_CYCLE = 5;
    public static final int AI_MODEL_TRANSITION = 6;
    public static final int AI_MODEL_COMBO = 7;
    public static final int AI_MODEL_PROFILE = 8;
    public static final int AI_MODEL_AUDIT = 9;
    public static final int AI_MODEL_FREQ_RISK = 10;
    public static final int AI_MODEL_TEACHER_MATRIX = 11;
    public static final int AI_MODEL_TEACHER_REPLAY = 12;
    public static final int AI_MODEL_TEACHER_ASSIST = 13;
    public static final int AI_MODEL_PREDRAW_REASON = 14;
    public static final int AI_MODEL_CONTRASTIVE_REASON = 15;
    public static final int AI_MODEL_CAUSAL_REASON = 16;
    public static final int AI_MODEL_K3_ROLE_COMBO = 17;
    public static final int AI_MODEL_V20_CAUSE_CHAIN = 18;

    public static final String REASON_PASS = "PASS";
    public static final String REASON_BLOCK = "BLOCK";
    public static final String REASON_WARN = "WARN";
    public static final String REASON_INFO = "INFO";

    public static final String BLOCK_LOW_PROBABILITY = "LOW_PROBABILITY";
    public static final String BLOCK_SAMPLE_WEAK = "SAMPLE_WEAK";
    public static final String BLOCK_TREND_WEAK = "TREND_WEAK";
    public static final String BLOCK_DRIFT_HIGH = "DRIFT_HIGH";
    public static final String BLOCK_BREAK_HIGH = "BREAK_HIGH";
    public static final String BLOCK_RECOVERY_WEAK = "RECOVERY_WEAK";
    public static final String BLOCK_POSITION_WEAK = "POSITION_WEAK";
    public static final String BLOCK_COMBO_WEAK = "COMBO_WEAK";
    public static final String BLOCK_FREQ_HIGH = "FREQ_HIGH";
    public static final String BLOCK_DATA_ERROR = "DATA_ERROR";
    public static final String BLOCK_HISTORY_WEAK = "HISTORY_WEAK";
    public static final String BLOCK_OPEN_TRADE_EXISTS = "OPEN_TRADE_EXISTS";
    public static final String BLOCK_BACKTEST_WEAK = "BACKTEST_WEAK";
    public static final String BLOCK_FULL_MODE_BUSY = "FULL_MODE_BUSY";
    public static final String BLOCK_PROFILE_STALE = "PROFILE_STALE";
    public static final String BLOCK_AUDIT_OVER = "AUDIT_OVER";
    public static final String BLOCK_AI_CONFLICT = "AI_CONFLICT";
    public static final String BLOCK_A_PLUS_NOT_READY = "A_PLUS_NOT_READY";
    public static final String BLOCK_BIAS_WEAK = "BIAS_WEAK";
    public static final String BLOCK_SCHEDULER_BUSY = "SCHEDULER_BUSY";
    public static final String BLOCK_AUTO_VALIDATE_WEAK = "AUTO_VALIDATE_WEAK";
    public static final String BLOCK_TEACHER_WEAK = "TEACHER_WEAK";
    public static final String BLOCK_TEACHER_MATRIX_WEAK = "TEACHER_MATRIX_WEAK";
    public static final String BLOCK_TEACHER_REPLAY_WEAK = "TEACHER_REPLAY_WEAK";
    public static final String BLOCK_TEACHER_ASSIST_WEAK = "TEACHER_ASSIST_WEAK";
    public static final String BLOCK_REALTIME_TIMEOUT = "REALTIME_TIMEOUT";
    public static final String BLOCK_AUTO_LEARN_WEAK = "AUTO_LEARN_WEAK";
    public static final String BLOCK_RUNTIME_PROFILE_WEAK = "RUNTIME_PROFILE_WEAK";
    public static final String BLOCK_DRAW_PROTECT = "DRAW_PROTECT";
    public static final String BLOCK_REASON_EDGE_WEAK = "REASON_EDGE_WEAK";
    public static final String BLOCK_HIT_SIMILARITY_WEAK = "HIT_SIMILARITY_WEAK";
    public static final String BLOCK_MISS_SIMILARITY_HIGH = "MISS_SIMILARITY_HIGH";
    public static final String BLOCK_VALID_REASON_WEAK = "VALID_REASON_WEAK";
    public static final String BLOCK_FALSE_REASON = "FALSE_REASON";
    public static final String BLOCK_COMBO_CONFLICT = "COMBO_CONFLICT";
    public static final String BLOCK_FINGERPRINT_COOLING = "FINGERPRINT_COOLING";
    public static final String BLOCK_ELITE45_NOT_PASS = "ELITE45_NOT_PASS";
    public static final String BLOCK_V20_WARMUP = "V20_WARMUP";
    public static final String BLOCK_V20_SHADOW_NOT_HEALTHY = "V20_SHADOW_NOT_HEALTHY";
    public static final String BLOCK_V20_SAME_CLASS_FREEZE = "V20_SAME_CLASS_FREEZE";
    public static final String BLOCK_V20_FALSE_STRONG_FREEZE = "V20_FALSE_STRONG_FREEZE";
    public static final String BLOCK_V20_THIRD_GUARD_DRAG = "V20_THIRD_GUARD_DRAG";
    public static final String BLOCK_V20_RANK_COUNTER_NEG = "V20_RANK_COUNTER_NEG";
    public static final String BLOCK_V20_GRADE_C = "V20_GRADE_C";
    public static final String BLOCK_V20_GLOBAL_FUSE = "V20_GLOBAL_FUSE";

    public static final String HIT_TREND_CONFIRMED = "TREND_CONFIRMED_HIT";
    public static final String HIT_POSITION_STRENGTH = "POSITION_STRENGTH_HIT";
    public static final String HIT_TOP3_MASS = "TOP3_MASS_HIT";
    public static final String HIT_CYCLE_POINT = "CYCLE_POINT_HIT";
    public static final String HIT_TRANSITION_CHAIN = "TRANSITION_CHAIN_HIT";
    public static final String HIT_RECOVERY = "RECOVERY_HIT";
    public static final String HIT_COMBO_MEMORY = "COMBO_MEMORY_HIT";
    public static final String HIT_DEEP_PROFILE = "DEEP_PROFILE_HIT";
    public static final String HIT_LOW_DRIFT = "LOW_DRIFT_HIT";
    public static final String HIT_A_PLUS_SHAPE = "A_PLUS_SHAPE_HIT";
    public static final String HIT_AUTO_VALIDATE_STABLE = "AUTO_VALIDATE_STABLE_HIT";
    public static final String HIT_TEACHER_SUPPORT = "TEACHER_SUPPORT_HIT";
    public static final String HIT_TEACHER_MATRIX_SUPPORT = "TEACHER_MATRIX_SUPPORT_HIT";
    public static final String HIT_TEACHER_REPLAY_SUPPORT = "TEACHER_REPLAY_SUPPORT_HIT";
    public static final String HIT_TEACHER_ASSIST_SUPPORT = "TEACHER_ASSIST_SUPPORT_HIT";
    public static final String HIT_RUNTIME_PROFILE = "RUNTIME_PROFILE_HIT";
    public static final String HIT_STATE_NUMBER_PROFILE = "STATE_NUMBER_PROFILE_HIT";
    public static final String HIT_COLD_RECOVERY_SHAPE = "COLD_RECOVERY_SHAPE_HIT";
    public static final String HIT_PREDRAW_REASON_SUPPORT = "PREDRAW_REASON_SUPPORT_HIT";
    public static final String HIT_CONTRASTIVE_REASON_SUPPORT = "CONTRASTIVE_REASON_SUPPORT_HIT";
    public static final String HIT_CAUSAL_REASON_VALID = "CAUSAL_REASON_VALID_HIT";
    public static final String HIT_K3_ROLE_COMBO_VALID = "K3_ROLE_COMBO_VALID_HIT";

    public static final String MISS_POSITION_LOST = "POSITION_LOST";
    public static final String MISS_K3_RANK_4_6 = "K3_RANK_4_6";
    public static final String MISS_TREND_BROKEN = "TREND_BROKEN";
    public static final String MISS_FALSE_RECOVERY = "FALSE_RECOVERY";
    public static final String MISS_DRIFT_SPIKE = "DRIFT_SPIKE";
    public static final String MISS_HOT_REVERSE = "HOT_REVERSE";
    public static final String MISS_MARKOV_FAILED = "MARKOV_FAILED";
    public static final String MISS_THIRD_WEAK = "THIRD_WEAK";
    public static final String MISS_SAMPLE_WEAK = "STATE_SAMPLE_WEAK";
    public static final String MISS_DATA_OFFSET = "DATA_OFFSET";
    public static final String MISS_GATE_TOO_LOW = "GATE_TOO_LOW";
    public static final String MISS_CALIBRATION_FAILED = "CALIBRATION_FAILED";
    public static final String MISS_TEACHER_MATRIX_FAILED = "TEACHER_MATRIX_FAILED";
    public static final String MISS_TEACHER_REPLAY_FAILED = "TEACHER_REPLAY_FAILED";
    public static final String MISS_TEACHER_ASSIST_FAILED = "TEACHER_ASSIST_FAILED";
    public static final String MISS_PREDRAW_REASON_FAILED = "PREDRAW_REASON_FAILED";
    public static final String MISS_CONTRASTIVE_REASON_FAILED = "CONTRASTIVE_REASON_FAILED";
    public static final String MISS_CAUSAL_REASON_FALSE = "CAUSAL_REASON_FALSE";
    public static final String MISS_K3_ROLE_CONFLICT = "K3_ROLE_CONFLICT";
    public static final String MISS_UNKNOWN = "UNKNOWN";

    public static final String MISS_POSITION_DRIFT = "POSITION_DRIFT_MISS";
    public static final String MISS_TREND_BREAK = "TREND_BREAK_MISS";
    public static final String MISS_FALSE_RECOVERY_DEEP = "FALSE_RECOVERY_MISS";
    public static final String MISS_CYCLE_OFFSET = "CYCLE_OFFSET_MISS";
    public static final String MISS_TRANSITION_FAIL = "TRANSITION_FAIL_MISS";
    public static final String MISS_ACTUAL_IN_LAYER_4_6 = "ACTUAL_IN_LAYER_4_6_MISS";
    public static final String MISS_CALIBRATION_OVER = "CALIBRATION_OVER_MISS";
    public static final String MISS_STATE_SWITCH = "STATE_SWITCH_MISS";
    public static final String MISS_PROFILE_STALE = "PROFILE_STALE_MISS";
    public static final String MISS_HIGH_FREQ_OVERHEAT = "HIGH_FREQ_OVERHEAT_MISS";
    public static final String MISS_CHAOS_NOISE = "CHAOS_NOISE_MISS";
    public static final String MISS_AUTO_VALIDATE_WARN = "AUTO_VALIDATE_WARN_MISS";
    public static final String MISS_TEACHER_STALE = "TEACHER_STALE_MISS";
    public static final String MISS_RUNTIME_PROFILE_WEAK = "RUNTIME_PROFILE_WEAK_MISS";
    public static final String MISS_STATE_NUMBER_PROFILE_WEAK = "STATE_NUMBER_PROFILE_WEAK_MISS";
    public static final String MISS_FALSE_HOT_PROFILE = "FALSE_HOT_PROFILE_MISS";
    public static final String MISS_COLD_BACKFILL_LOST = "COLD_BACKFILL_LOST_MISS";
    public static final String MISS_REASON_EDGE_NEGATIVE = "REASON_EDGE_NEGATIVE_MISS";
    public static final String MISS_HIT_SIM_LOW = "HIT_SIM_LOW_MISS";
    public static final String MISS_MISS_SIM_HIGH = "MISS_SIM_HIGH_MISS";
    public static final String MISS_FINGERPRINT_COOLING = "FINGERPRINT_COOLING_MISS";
    public static final String MISS_V20_THIRD_GUARD_DRAG = "V20_THIRD_GUARD_DRAG_MISS";
    public static final String MISS_V20_RANK_FALSE_STRONG = "V20_RANK_FALSE_STRONG_MISS";
    public static final String MISS_V20_POSITION_WRONG = "V20_POSITION_WRONG_MISS";
    public static final String MISS_V20_SHADOW_SHOULD_HOLD = "V20_SHADOW_SHOULD_HOLD_MISS";

    public static final String ATTR_MODULE_TREND = "TREND";
    public static final String ATTR_MODULE_POSITION = "POSITION";
    public static final String ATTR_MODULE_NUMBER = "NUMBER";
    public static final String ATTR_MODULE_CYCLE = "CYCLE";
    public static final String ATTR_MODULE_TRANSITION = "TRANSITION";
    public static final String ATTR_MODULE_PROFILE = "PROFILE";
    public static final String ATTR_MODULE_AUDIT = "AUDIT";
    public static final String ATTR_MODULE_GATE = "GATE";
    public static final String ATTR_MODULE_AI_JUDGE = "AI_JUDGE";
    public static final String ATTR_MODULE_AUTO_VALIDATE = "AUTO_VALIDATE";
    public static final String ATTR_MODULE_TEACHER = "TEACHER";
    public static final String ATTR_MODULE_TEACHER_MATRIX = "TEACHER_MATRIX";
    public static final String ATTR_MODULE_TEACHER_REPLAY = "TEACHER_REPLAY";
    public static final String ATTR_MODULE_TEACHER_ASSIST = "TEACHER_ASSIST";
    public static final String ATTR_MODULE_AUTO_LEARN = "AUTO_LEARN";
    public static final String ATTR_MODULE_RUNTIME_PROFILE = "RUNTIME_PROFILE";
    public static final String ATTR_MODULE_STATE_NUMBER_PROFILE = "STATE_NUMBER_PROFILE";
    public static final String ATTR_MODULE_PREDRAW_REASON = "PREDRAW_REASON";
    public static final String ATTR_MODULE_CONTRASTIVE_REASON = "CONTRASTIVE_REASON";
    public static final String ATTR_MODULE_CAUSAL_REASON = "CAUSAL_REASON";
    public static final String ATTR_MODULE_K3_ROLE_COMBO = "K3_ROLE_COMBO";
    public static final String ATTR_MODULE_ELITE45 = "ELITE45";
    public static final String ATTR_MODULE_V20_CAUSE_CHAIN = "V20_CAUSE_CHAIN";
    public static final String ATTR_MODULE_UNKNOWN = "UNKNOWN";

    public static final String DIAG_LEVEL_OK = "OK";
    public static final String DIAG_LEVEL_WARN = "WARN";
    public static final String DIAG_LEVEL_DANGER = "DANGER";
    public static final String DIAG_LEVEL_CRITICAL = "CRITICAL";

    public static final String DIAG_FREQ_OVER = "FREQ_OVER";
    public static final String DIAG_PROB_OVER = "PROB_OVER";
    public static final String DIAG_PROFILE_STALE = "PROFILE_STALE";
    public static final String DIAG_POSITION_DRIFT = "POSITION_DRIFT";
    public static final String DIAG_MISS_REASON_SPIKE = "MISS_REASON_SPIKE";
    public static final String DIAG_BACKTEST_OVERFIT = "BACKTEST_OVERFIT";
    public static final String DIAG_A_PLUS_STALE = "A_PLUS_STALE";
    public static final String DIAG_NEED_RECOVERY = "NEED_RECOVERY";
    public static final String DIAG_REALTIME_BLOCKED = "REALTIME_BLOCKED";
    public static final String DIAG_AUTO_VALIDATE_SLOW = "AUTO_VALIDATE_SLOW";
    public static final String DIAG_TEACHER_STALE = "TEACHER_STALE";
    public static final String DIAG_TEACHER_MATRIX_STALE = "TEACHER_MATRIX_STALE";
    public static final String DIAG_TEACHER_REPLAY_STALE = "TEACHER_REPLAY_STALE";
    public static final String DIAG_TEACHER_ASSIST_STALE = "TEACHER_ASSIST_STALE";
    public static final String DIAG_AUTO_LEARN_STALE = "AUTO_LEARN_STALE";
    public static final String DIAG_RUNTIME_PROFILE_WEAK = "RUNTIME_PROFILE_WEAK";
    public static final String DIAG_NORMAL = "NORMAL";
    public static final String DIAG_TRADE_MISSING = "TRADE_MISSING";
    public static final String DIAG_TRADE_NOT_SETTLED = "TRADE_NOT_SETTLED";
    public static final String DIAG_PNL_NOT_WRITTEN = "PNL_NOT_WRITTEN";
    public static final String DIAG_DECISION_TRADE_SPLIT = "DECISION_TRADE_SPLIT";
    public static final String DIAG_TARGET_ISSUE_MISMATCH = "TARGET_ISSUE_MISMATCH";
    public static final String DIAG_STRICT_POSITION_FAIL = "STRICT_POSITION_FAIL";
    public static final String DIAG_PREDRAW_REASON_MISSING = "PREDRAW_REASON_MISSING";
    public static final String DIAG_CAUSAL_REASON_WEAK = "CAUSAL_REASON_WEAK";
    public static final String DIAG_CONTRASTIVE_REASON_WEAK = "CONTRASTIVE_REASON_WEAK";
    public static final String DIAG_FALSE_STRONG_COOLING = "FALSE_STRONG_COOLING";
    public static final String DIAG_ELITE45_BLOCKED = "ELITE45_BLOCKED";
    public static final String DIAG_V20_B_SHADOW_NOT_PROMOTED = "V20_B_SHADOW_NOT_PROMOTED";
    public static final String DIAG_V20_SAME_CLASS_FREEZE = "V20_SAME_CLASS_FREEZE";
    public static final String DIAG_V20_GLOBAL_FUSE = "V20_GLOBAL_FUSE";

    public static final String API_LINE_PRIMARY = "PRIMARY";
    public static final String API_LINE_BACKUP = "BACKUP";

    public static final double LEARN_STEP_HIT = 0.006d;
    public static final double LEARN_STEP_MISS = 0.004d;
    public static final double LEARN_WEIGHT_MIN = 0.030d;
    public static final double LEARN_WEIGHT_MAX = 0.600d;
    public static final double LEARN_RECENT_DECAY = 0.85d;
    public static final double LEARN_PUNISH_MAX_ONCE = 0.020d;
    public static final double LEARN_REWARD_MAX_ONCE = 0.020d;

    public static final double LEARN_SHADOW_WEIGHT = 0.55d;
    public static final double LEARN_HOLD_OBSERVE_WEIGHT = 0.18d;
    public static final double LEARN_REAL_TRADE_WEIGHT = 1.00d;
    public static final double LEARN_AUTO_LEARN_WEIGHT = 0.32d;
    public static final double LEARN_TEACHER_REPLAY_WEIGHT = 0.36d;
    public static final double LEARN_PROFILE_STALE_DECAY = 0.72d;
    public static final double LEARN_PROFILE_RECOVER_BOOST = 0.10d;

    public static final String TABLE_DRAWS = "draws";
    public static final String TABLE_DECISIONS = "decisions";
    public static final String TABLE_TRADES = "trades";
    public static final String TABLE_SHADOWS = "shadows";
    public static final String TABLE_SHADOW = TABLE_SHADOWS;
    public static final String TABLE_BACKTEST_REPORTS = "backtest_reports";
    public static final String TABLE_BACKTEST_REPORT = TABLE_BACKTEST_REPORTS;
    public static final String TABLE_BACKTEST = TABLE_BACKTEST_REPORTS;
    public static final String TABLE_APP_STATE = "app_state";
    public static final String TABLE_SHAPE_VECTOR = "shape_vector";
    public static final String TABLE_SHAPE_MEMORY = "shape_memory";
    public static final String TABLE_COMBO_MEMORY = "combo_memory";
    public static final String TABLE_BREAK_MEMORY = "break_memory";
    public static final String TABLE_CALIBRATION = "calibration";
    public static final String TABLE_MODEL_WEIGHT = "model_weight";

    public static final String TABLE_DEEP_PROFILE = "deep_profile";
    public static final String TABLE_DEEP_ATTRIBUTION = "deep_attribution";
    public static final String TABLE_DEEP_TRANSITION = "deep_transition";
    public static final String TABLE_DEEP_CYCLE = "deep_cycle";
    public static final String TABLE_MANUAL_BACKTEST_TASK = "manual_backtest_task";
    public static final String TABLE_POINT_IN_TIME_SNAPSHOT = "point_in_time_snapshot";
    public static final String TABLE_PROMOTION_CANDIDATE = "promotion_candidate";
    public static final String TABLE_PROBABILITY_AUDIT_BUCKET = "probability_audit_bucket";
    public static final String TABLE_BIAS_EVIDENCE = "bias_evidence";
    public static final String TABLE_LEARNING_SNAPSHOT = "learning_snapshot";
    public static final String TABLE_CANDIDATE_EVIDENCE = "candidate_evidence";
    public static final String TABLE_AI_JUDGE_LOG = "ai_judge_log";
    public static final String TABLE_SELF_DIAGNOSIS_LOG = "self_diagnosis_log";
    public static final String TABLE_TEACHER_SNAPSHOT = "teacher_snapshot";
    public static final String TABLE_REALTIME_VALIDATE = "realtime_validate";

    public static final String TABLE_RUNTIME_PROFILE_CACHE = "runtime_profile_cache";
    public static final String TABLE_STATE_NUMBER_PROFILE = "state_number_profile";
    public static final String TABLE_LEARNING_CASE_LOG = "learning_case_log";
    public static final String TABLE_AUTO_LEARN_CURSOR = "auto_learn_cursor";

    public static final String TABLE_TEACHER_MATRIX_SAMPLE = "teacher_matrix_sample";
    public static final String TABLE_TEACHER_MATRIX_PROFILE = "teacher_matrix_profile";
    public static final String TABLE_TEACHER_MATRIX_STATE_PROFILE = "teacher_matrix_state_profile";
    public static final String TABLE_TEACHER_MATRIX_COMBO_PROFILE = "teacher_matrix_combo_profile";
    public static final String TABLE_TEACHER_MATRIX_AUDIT = "teacher_matrix_audit";
    public static final String TABLE_TEACHER_MATRIX_REPORT = "teacher_matrix_report";

    public static final String TABLE_TEACHER_REPLAY_CASE = "teacher_replay_case";
    public static final String TABLE_TEACHER_REPLAY_CURSOR = "teacher_replay_cursor";
    public static final String TABLE_TEACHER_REPLAY_REPORT = "teacher_replay_report";
    public static final String TABLE_TEACHER_REPLAY_COUNTERFACTUAL =
            "teacher_replay_counterfactual";
    public static final String TABLE_TEACHER_REPLAY_STABILITY = "teacher_replay_stability";
    public static final String TABLE_TEACHER_ASSIST_PROFILE = "teacher_assist_profile";
    public static final String TABLE_UI_TEXT_STATE = "ui_text_state";
    public static final String TABLE_DIAG_LOG = "diagnostic_log";

    public static final String TABLE_PRE_DRAW_REASON_SAMPLE = "pre_draw_reason_sample";
    public static final String TABLE_REASON_CONTRAST_PROFILE = "reason_contrast_profile";
    public static final String TABLE_REASON_CAUSAL_PROFILE = "reason_causal_profile";
    public static final String TABLE_FALSE_STRONG_PROFILE = "false_strong_profile";
    public static final String TABLE_COMBO_ROLE_PROFILE = "combo_role_profile";
    public static final String TABLE_ELITE45_AUDIT_LOG = "elite45_audit_log";

    public static boolean isValidPosition(int position) {
        return position >= MIN_POSITION && position <= MAX_POSITION;
    }

    public static boolean isValidNumber(int number) {
        return number >= MIN_NUMBER && number <= MAX_NUMBER;
    }

    public static boolean isValidK(int k) {
        return k == FIXED_K;
    }

    public static boolean isValidK3Numbers(int n1, int n2, int n3) {
        if (!isValidNumber(n1) || !isValidNumber(n2) || !isValidNumber(n3)) {
            return false;
        }
        return n1 != n2 && n1 != n3 && n2 != n3;
    }

    public static boolean isPlayRuleLegal(int position, int k, int n1, int n2, int n3) {
        return isValidPosition(position) && isValidK(k) && isValidK3Numbers(n1, n2, n3);
    }

    public static boolean isBetDecision(String decision) {
        return DECISION_BET.equals(normalizeDecision(decision));
    }

    public static boolean isHoldDecision(String decision) {
        return DECISION_HOLD.equals(normalizeDecision(decision));
    }

    public static boolean isShadowDecision(String decision) {
        return DECISION_SHADOW.equals(normalizeDecision(decision));
    }

    public static String normalizeDecision(String decision) {
        String d = normalizeToken(decision);
        if (DECISION_BET.equals(d) || "REAL_BET".equals(d) || "TRADE".equals(d)) {
            return DECISION_BET;
        }
        if (DECISION_SHADOW.equals(d) || "OBSERVE".equals(d) || "WATCH".equals(d)) {
            return DECISION_SHADOW;
        }
        return DECISION_HOLD;
    }

    public static String normalizeResult(String result) {
        String r = normalizeToken(result);
        if (RESULT_WIN.equals(r) || "HIT".equals(r) || "命中".equals(result)) {
            return RESULT_WIN;
        }
        if (RESULT_MISS.equals(r) || "LOSE".equals(r) || "LOSS".equals(r) || "未中".equals(result)) {
            return RESULT_MISS;
        }
        if (RESULT_VOID.equals(r) || "CANCEL".equals(r) || "CANCELLED".equals(r)) {
            return RESULT_VOID;
        }
        if (RESULT_DATA_OFFSET.equals(r) || "OFFSET".equals(r) || MISS_DATA_OFFSET.equals(r)) {
            return RESULT_DATA_OFFSET;
        }
        if (RESULT_DATA_ERROR.equals(r) || "ERROR".equals(r) || BLOCK_DATA_ERROR.equals(r)) {
            return RESULT_DATA_ERROR;
        }
        if (RESULT_WAIT.equals(r) || "PENDING".equals(r) || "WAITING".equals(r)) {
            return RESULT_WAIT;
        }
        return RESULT_UNKNOWN;
    }

    public static boolean isTerminalResult(String result) {
        String r = normalizeResult(result);
        return RESULT_WIN.equals(r)
                || RESULT_MISS.equals(r)
                || RESULT_VOID.equals(r)
                || RESULT_DATA_ERROR.equals(r)
                || RESULT_DATA_OFFSET.equals(r);
    }

    public static boolean isSettlementResultText(String result) {
        return isTerminalResult(result);
    }

    public static boolean isWinResult(String result) {
        return RESULT_WIN.equals(normalizeResult(result));
    }

    public static boolean isMissResult(String result) {
        return RESULT_MISS.equals(normalizeResult(result));
    }

    public static boolean isDataBadResult(String result) {
        String r = normalizeResult(result);
        return RESULT_DATA_ERROR.equals(r) || RESULT_DATA_OFFSET.equals(r);
    }

    public static double pnlForResult(String result) {
        String r = normalizeResult(result);
        if (RESULT_WIN.equals(r)) {
            return WIN_PNL_K3;
        }
        if (RESULT_MISS.equals(r)) {
            return MISS_PNL_K3;
        }
        return 0.0d;
    }

    public static double pnlForHit(boolean hit) {
        return hit ? WIN_PNL_K3 : MISS_PNL_K3;
    }

    public static boolean isStrictPositionK3Hit(
            int position, int n1, int n2, int n3, int[] drawNumbers) {
        if (!isPlayRuleLegal(position, FIXED_K, n1, n2, n3)) {
            return false;
        }
        if (drawNumbers == null || drawNumbers.length < POSITION_COUNT) {
            return false;
        }
        int actual = drawNumbers[position - 1];
        return actual == n1 || actual == n2 || actual == n3;
    }

    public static boolean isStrictPositionK3HitByMask(int position, int k3Mask, int[] drawNumbers) {
        if (!isValidPosition(position) || !isValidK3Mask(k3Mask)) {
            return false;
        }
        if (drawNumbers == null || drawNumbers.length < POSITION_COUNT) {
            return false;
        }
        return maskContainsNumber(k3Mask, drawNumbers[position - 1]);
    }

    public static String normalizeStatScope(String scope) {
        String s = normalizeToken(scope);
        if (STAT_SCOPE_REAL_TRADE.equals(s) || "REAL".equals(s)) return STAT_SCOPE_REAL_TRADE;
        if (STAT_SCOPE_SHADOW.equals(s)) return STAT_SCOPE_SHADOW;
        if (STAT_SCOPE_HOLD.equals(s)) return STAT_SCOPE_HOLD;
        if (STAT_SCOPE_BACKTEST.equals(s)) return STAT_SCOPE_BACKTEST;
        if (STAT_SCOPE_MANUAL_BACKTEST.equals(s)) return STAT_SCOPE_MANUAL_BACKTEST;
        if (STAT_SCOPE_TEACHER.equals(s)) return STAT_SCOPE_TEACHER;
        if (STAT_SCOPE_TEACHER_REPLAY.equals(s)) return STAT_SCOPE_TEACHER_REPLAY;
        if (STAT_SCOPE_AUTO_VALIDATE.equals(s)) return STAT_SCOPE_AUTO_VALIDATE;
        if (STAT_SCOPE_AUTO_LEARN.equals(s)) return STAT_SCOPE_AUTO_LEARN;
        if (STAT_SCOPE_TEACHER_MATRIX.equals(s)) return STAT_SCOPE_TEACHER_MATRIX;
        if (STAT_SCOPE_TEACHER_ASSIST.equals(s)) return STAT_SCOPE_TEACHER_ASSIST;
        return "";
    }

    public static String normalizeStatScopeForDecision(String decision, String scope) {
        String s = normalizeStatScope(scope);
        if (s.length() > 0) {
            return s;
        }

        String d = normalizeDecision(decision);

        if (DECISION_SHADOW.equals(d)) {
            return STAT_SCOPE_SHADOW;
        }

        if (DECISION_HOLD.equals(d)) {
            return STAT_SCOPE_HOLD;
        }

        if (DECISION_BET.equals(d)) {
            if (STRICT_NO_AUTO_PROMOTE_BET_TO_REAL_TRADE) {
                return STAT_SCOPE_HOLD;
            }
            return STAT_SCOPE_REAL_TRADE;
        }

        return STAT_SCOPE_HOLD;
    }

    public static boolean isRealTradeScope(String scope) {
        return STAT_SCOPE_REAL_TRADE.equals(normalizeStatScope(scope));
    }

    public static boolean isShadowScope(String scope) {
        return STAT_SCOPE_SHADOW.equals(normalizeStatScope(scope));
    }

    public static boolean isHoldScope(String scope) {
        return STAT_SCOPE_HOLD.equals(normalizeStatScope(scope));
    }

    public static boolean isBacktestScope(String scope) {
        String s = normalizeStatScope(scope);
        return STAT_SCOPE_BACKTEST.equals(s) || STAT_SCOPE_MANUAL_BACKTEST.equals(s);
    }

    public static boolean isTeacherMatrixScope(String scope) {
        String s = normalizeStatScope(scope);
        return STAT_SCOPE_TEACHER_MATRIX.equals(s) || STAT_SCOPE_TEACHER.equals(s);
    }

    public static boolean isTeacherReplayScope(String scope) {
        return STAT_SCOPE_TEACHER_REPLAY.equals(normalizeStatScope(scope));
    }

    public static boolean isTeacherAssistScope(String scope) {
        return STAT_SCOPE_TEACHER_ASSIST.equals(normalizeStatScope(scope));
    }

    public static boolean shouldCountInRealHitRate(String decision, String scope) {
        return DECISION_BET.equals(normalizeDecision(decision)) && isRealTradeScope(scope);
    }

    public static boolean shouldCountInStrictRealStats(
            String decision,
            String scope,
            String sourceType,
            boolean isRealtimeReal,
            boolean settled) {
        if (!DECISION_BET.equals(normalizeDecision(decision))) {
            return false;
        }
        if (STRICT_REAL_STATS_REQUIRE_REAL_TRADE_SCOPE && !isRealTradeScope(scope)) {
            return false;
        }
        if (STRICT_REAL_STATS_REQUIRE_REALTIME_SOURCE
                && !SOURCE_TYPE_REALTIME.equals(normalizeSourceType(sourceType))) {
            return false;
        }
        if (STRICT_REAL_STATS_REQUIRE_IS_REALTIME_REAL && !isRealtimeReal) {
            return false;
        }
        if (STRICT_REAL_STATS_REQUIRE_SETTLED && !settled) {
            return false;
        }
        return true;
    }

    public static String normalizeSourceType(String sourceType) {
        String s = normalizeToken(sourceType);
        if (SOURCE_TYPE_REALTIME.equals(s)) return SOURCE_TYPE_REALTIME;
        if (SOURCE_TYPE_BACKTEST.equals(s)) return SOURCE_TYPE_BACKTEST;
        if (SOURCE_TYPE_MANUAL_BACKTEST.equals(s)) return SOURCE_TYPE_MANUAL_BACKTEST;
        if (SOURCE_TYPE_AUTO_LEARN.equals(s)) return SOURCE_TYPE_AUTO_LEARN;
        if (SOURCE_TYPE_TEACHER_REPLAY.equals(s)) return SOURCE_TYPE_TEACHER_REPLAY;
        if (SOURCE_TYPE_TEACHER_MATRIX.equals(s)) return SOURCE_TYPE_TEACHER_MATRIX;
        if (SOURCE_TYPE_SHADOW.equals(s)) return SOURCE_TYPE_SHADOW;
        return SOURCE_TYPE_UNKNOWN;
    }

    public static boolean isRiskMode(String mode) {
        mode = normalizeMode(mode);
        return MODE_BROKEN.equals(mode)
                || MODE_REVERSING.equals(mode)
                || MODE_REVERSAL.equals(mode)
                || MODE_CHAOS.equals(mode);
    }

    public static boolean isWeakMode(String mode) {
        mode = normalizeMode(mode);
        return MODE_WEAKENING.equals(mode)
                || MODE_PULLBACK.equals(mode)
                || MODE_CHAOS.equals(mode)
                || MODE_TRANSITION.equals(mode);
    }

    public static boolean isStrongMode(String mode) {
        mode = normalizeMode(mode);
        return MODE_ACCELERATING.equals(mode)
                || MODE_CONFIRMED.equals(mode)
                || MODE_RECOVERING.equals(mode)
                || MODE_STABLE.equals(mode);
    }

    public static String normalizeMode(String mode) {
        String m = normalizeToken(mode);
        if (MODE_FORMING.equals(m)) return MODE_FORMING;
        if (MODE_CONFIRMED.equals(m)) return MODE_CONFIRMED;
        if (MODE_ACCELERATING.equals(m)) return MODE_ACCELERATING;
        if (MODE_PULLBACK.equals(m)) return MODE_PULLBACK;
        if (MODE_RECOVERING.equals(m)) return MODE_RECOVERING;
        if (MODE_WEAKENING.equals(m)) return MODE_WEAKENING;
        if (MODE_BROKEN.equals(m)) return MODE_BROKEN;
        if (MODE_REVERSING.equals(m)) return MODE_REVERSING;
        if (MODE_REVERSAL.equals(m)) return MODE_REVERSAL;
        if (MODE_CHAOS.equals(m)) return MODE_CHAOS;
        if (MODE_STABLE.equals(m)) return MODE_STABLE;
        if (MODE_TRANSITION.equals(m)) return MODE_TRANSITION;
        return MODE_UNKNOWN;
    }

    public static String normalizeAPlusStatus(String status) {
        String s = normalizeToken(status);
        if (A_PLUS_CANDIDATE.equals(s)) return A_PLUS_CANDIDATE;
        if (A_PLUS_WATCH.equals(s)) return A_PLUS_WATCH;
        if (A_PLUS_READY.equals(s)) return A_PLUS_READY;
        if (A_PLUS_ACTIVE.equals(s)) return A_PLUS_ACTIVE;
        if (A_PLUS_STALE.equals(s)) return A_PLUS_STALE;
        if (A_PLUS_RECOVERING.equals(s)) return A_PLUS_RECOVERING;
        return A_PLUS_NONE;
    }

    public static boolean isAPlusUsable(String status) {
        status = normalizeAPlusStatus(status);
        return A_PLUS_CANDIDATE.equals(status)
                || A_PLUS_READY.equals(status)
                || A_PLUS_ACTIVE.equals(status);
    }

    public static String normalizePromotionStatus(String status) {
        String s = normalizeToken(status);
        if (PROMOTION_CANDIDATE.equals(s)) return PROMOTION_CANDIDATE;
        if (PROMOTION_CHALLENGER.equals(s)) return PROMOTION_CHALLENGER;
        if (PROMOTION_SHADOW_OBSERVING.equals(s)) return PROMOTION_SHADOW_OBSERVING;
        if (PROMOTION_PROMOTED.equals(s)) return PROMOTION_PROMOTED;
        if (PROMOTION_LIMITED_ACTIVE.equals(s)) return PROMOTION_LIMITED_ACTIVE;
        if (PROMOTION_ACTIVE.equals(s)) return PROMOTION_ACTIVE;
        if (PROMOTION_STALE.equals(s)) return PROMOTION_STALE;
        if (PROMOTION_REJECTED.equals(s)) return PROMOTION_REJECTED;
        return PROMOTION_CANDIDATE;
    }

    public static boolean isPromotionRuntimeUsable(String status) {
        status = normalizePromotionStatus(status);
        return PROMOTION_PROMOTED.equals(status)
                || PROMOTION_LIMITED_ACTIVE.equals(status)
                || PROMOTION_ACTIVE.equals(status);
    }

    public static boolean isPromotionWeakOrRejected(String status) {
        status = normalizePromotionStatus(status);
        return PROMOTION_STALE.equals(status) || PROMOTION_REJECTED.equals(status);
    }

    public static String normalizeGrade(String grade) {
        String g = normalizeToken(grade);
        if (GRADE_S.equals(g)) return GRADE_S;
        if (GRADE_A.equals(g)) return GRADE_A;
        if (GRADE_B.equals(g)) return GRADE_B;
        return GRADE_C;
    }

    public static boolean isGradeS(String grade) {
        return GRADE_S.equals(normalizeGrade(grade));
    }

    public static boolean isGradeA(String grade) {
        return GRADE_A.equals(normalizeGrade(grade));
    }

    public static boolean isGradeB(String grade) {
        return GRADE_B.equals(normalizeGrade(grade));
    }

    public static boolean isGradeC(String grade) {
        return GRADE_C.equals(normalizeGrade(grade));
    }

    public static boolean isBetAllowedByGrade(String grade) {
        grade = normalizeGrade(grade);
        if (GRADE_C.equals(grade)) {
            return false;
        }
        return GRADE_S.equals(grade) || GRADE_A.equals(grade) || GRADE_B.equals(grade);
    }

    public static boolean isWarmupPeriod(int learnedIssues) {
        return learnedIssues < LIVE_SHADOW_WARMUP_ISSUES;
    }

    public static boolean isTeacherStrongSampleWarmupPeriod(int learnedIssues) {
        return learnedIssues < TEACHER_STRONG_SAMPLE_WARMUP_ISSUES;
    }

    public static boolean isRealBetProbePeriodReady(int learnedIssues) {
        if (!ENABLE_REAL_BET_PROBE_DOMAIN || !ENABLE_REAL_BET_PROBE_AFTER_240) {
            return false;
        }
        return learnedIssues >= REAL_BET_PROBE_MIN_ISSUES;
    }

    public static boolean mustShadowDuringWarmup(String grade, int learnedIssues) {
        if (!isWarmupPeriod(learnedIssues)) {
            return false;
        }
        return !isGradeS(grade);
    }

    public static boolean isShadowPromotePass(
            int sample30,
            double hit30,
            int sample60,
            double hit60,
            int sample120,
            double hit120,
            int maxLose) {
        if (maxLose > SHADOW_MAX_LOSE) {
            return false;
        }
        if (sample30 >= SHADOW_PROMOTE_MIN_SAMPLE_15 && hit30 >= SHADOW_PROMOTE_HIT_15) {
            return true;
        }
        if (sample30 >= SHADOW_PROMOTE_MIN_SAMPLE_30 && hit30 < SHADOW_PROMOTE_HIT_30) {
            return false;
        }
        if (sample60 >= SHADOW_PROMOTE_MIN_SAMPLE_60 && hit60 < SHADOW_PROMOTE_HIT_60) {
            return false;
        }
        if (sample120 >= SHADOW_PROMOTE_MIN_SAMPLE_120 && hit120 < SHADOW_PROMOTE_HIT_120) {
            return false;
        }
        return sample30 >= SHADOW_PROMOTE_MIN_SAMPLE_30
                || sample60 >= SHADOW_PROMOTE_MIN_SAMPLE_60
                || sample120 >= SHADOW_PROMOTE_MIN_SAMPLE_120;
    }

    public static boolean isShadowPromoteWatch(int sample30, double hit30, int maxLose) {
        if (maxLose > SHADOW_MAX_LOSE) {
            return false;
        }
        if (sample30 >= SHADOW_PROMOTE_MIN_SAMPLE_10 && hit30 >= SHADOW_PROMOTE_HIT_10) {
            return true;
        }
        return sample30 >= SHADOW_PROMOTE_MIN_SAMPLE_15 && hit30 >= SHADOW_PROMOTE_LIGHT_HIT_15;
    }

    public static boolean isSameClassFreeze(int loseStreak) {
        return loseStreak >= SAME_CLASS_FREEZE_LOSE;
    }

    public static boolean isSameClassHardFreeze(int loseStreak) {
        return loseStreak >= SAME_CLASS_HARD_FREEZE_LOSE;
    }

    public static boolean isGlobalKeepOnlyS(int realLoseStreak) {
        return realLoseStreak >= GLOBAL_KEEP_ONLY_S_LOSE;
    }

    public static boolean isGlobalShadowRecovery(int realLoseStreak) {
        return realLoseStreak >= GLOBAL_SHADOW_RECOVERY_LOSE;
    }

    public static boolean isFalseStrongFreeze(int falseStrongLose) {
        return falseStrongLose >= FALSE_STRONG_FREEZE_LOSE;
    }

    public static boolean isThirdGuardPass(double thirdGuardNet) {
        if (Double.isNaN(thirdGuardNet) || Double.isInfinite(thirdGuardNet)) {
            return false;
        }
        return thirdGuardNet > THIRD_GUARD_MIN_NET;
    }

    public static boolean isRankCounterPass(double rankCounterEdge) {
        if (Double.isNaN(rankCounterEdge) || Double.isInfinite(rankCounterEdge)) {
            return false;
        }
        return rankCounterEdge > RANK_COUNTER_MIN_EDGE;
    }

    public static boolean isPositionHealthPass(double positionHealth) {
        if (Double.isNaN(positionHealth) || Double.isInfinite(positionHealth)) {
            return false;
        }
        return positionHealth >= POSITION_MIN_HEALTH;
    }

    public static boolean isHealthyVolumeModeAllowed(
            double recentHitRate, double recentFreq, int maxLose) {
        if (maxLose > SHADOW_MAX_LOSE) {
            return false;
        }
        if (recentHitRate < TARGET_HIT_RATE_MIN) {
            return false;
        }
        return recentFreq >= TARGET_FREQ_LOW && recentFreq <= TARGET_FREQ_HIGH;
    }

    public static double clampProbability(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return 0.0d;
        }
        if (value < 0.0d) {
            return 0.0d;
        }
        if (value > 1.0d) {
            return 1.0d;
        }
        return value;
    }

    public static double clampScore01(double value) {
        return clampProbability(value);
    }

    public static double clampScore100(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return 0.0d;
        }
        if (value < 0.0d) {
            return 0.0d;
        }
        if (value > 100.0d) {
            return 100.0d;
        }
        return value;
    }

    public static double clampWeight(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return LEARN_WEIGHT_MIN;
        }
        if (value < LEARN_WEIGHT_MIN) {
            return LEARN_WEIGHT_MIN;
        }
        if (value > LEARN_WEIGHT_MAX) {
            return LEARN_WEIGHT_MAX;
        }
        return value;
    }

    public static double clampGate(double gate) {
        if (Double.isNaN(gate) || Double.isInfinite(gate)) {
            return DEFAULT_DYNAMIC_GATE;
        }
        if (gate < MIN_DYNAMIC_GATE) {
            return MIN_DYNAMIC_GATE;
        }
        if (gate > MAX_DYNAMIC_GATE) {
            return MAX_DYNAMIC_GATE;
        }
        return gate;
    }

    public static double clampElite45Gate(double gate) {
        if (Double.isNaN(gate) || Double.isInfinite(gate)) {
            return ELITE45_GATE_DEFAULT;
        }
        if (gate < ELITE45_GATE_FLOOR) {
            return ELITE45_GATE_FLOOR;
        }
        if (gate > MAX_DYNAMIC_GATE) {
            return MAX_DYNAMIC_GATE;
        }
        return gate;
    }

    public static String normalizeBacktestMode(String mode) {
        String m = normalizeToken(mode);
        if (BACKTEST_MODE_FULL.equals(m)) return BACKTEST_MODE_FULL;
        if (BACKTEST_MODE_NORMAL.equals(m)) return BACKTEST_MODE_NORMAL;
        if (BACKTEST_MODE_FAST.equals(m)) return BACKTEST_MODE_FAST;
        return BACKTEST_MODE_FAST;
    }

    public static String normalizeBacktestStatus(String status) {
        String s = normalizeToken(status);
        if (BACKTEST_STATUS_IDLE.equals(s)) return BACKTEST_STATUS_IDLE;
        if (BACKTEST_STATUS_WAITING.equals(s)) return BACKTEST_STATUS_WAITING;
        if (BACKTEST_STATUS_RUNNING.equals(s)) return BACKTEST_STATUS_RUNNING;
        if (BACKTEST_STATUS_YIELDING.equals(s)) return BACKTEST_STATUS_YIELDING;
        if (BACKTEST_STATUS_APPLYING.equals(s)) return BACKTEST_STATUS_APPLYING;
        if (BACKTEST_STATUS_OK.equals(s)) return BACKTEST_STATUS_OK;
        if (BACKTEST_STATUS_ERROR.equals(s)) return BACKTEST_STATUS_ERROR;
        if (BACKTEST_STATUS_CANCELLED.equals(s)) return BACKTEST_STATUS_CANCELLED;
        if (BACKTEST_STATUS_SKIPPED.equals(s)) return BACKTEST_STATUS_SKIPPED;
        return BACKTEST_STATUS_IDLE;
    }

    public static int getProbabilityEnrichLimit(String mode) {
        mode = normalizeBacktestMode(mode);
        if (BACKTEST_MODE_FULL.equals(mode)) return PROB_BACKTEST_FULL_ENRICH_LIMIT;
        if (BACKTEST_MODE_NORMAL.equals(mode)) return PROB_BACKTEST_NORMAL_ENRICH_LIMIT;
        return PROB_BACKTEST_FAST_ENRICH_LIMIT;
    }

    public static int getProbabilityDeepLimit(String mode) {
        mode = normalizeBacktestMode(mode);
        if (BACKTEST_MODE_FULL.equals(mode)) return PROB_BACKTEST_FULL_DEEP_LIMIT;
        if (BACKTEST_MODE_NORMAL.equals(mode)) return PROB_BACKTEST_NORMAL_DEEP_LIMIT;
        return PROB_BACKTEST_FAST_DEEP_LIMIT;
    }

    public static int getBacktestTrainLookback(String mode) {
        mode = normalizeBacktestMode(mode);
        if (BACKTEST_MODE_FULL.equals(mode)) return BACKTEST_TRAIN_LOOKBACK_FULL;
        if (BACKTEST_MODE_NORMAL.equals(mode)) return BACKTEST_TRAIN_LOOKBACK_NORMAL;
        return BACKTEST_TRAIN_LOOKBACK_FAST;
    }

    public static int getBacktestPauseEverySteps(String mode) {
        mode = normalizeBacktestMode(mode);
        if (BACKTEST_MODE_FULL.equals(mode)) return BACKTEST_PAUSE_EVERY_STEPS_FULL;
        if (BACKTEST_MODE_NORMAL.equals(mode)) return BACKTEST_PAUSE_EVERY_STEPS_NORMAL;
        return BACKTEST_PAUSE_EVERY_STEPS_FAST;
    }

    public static long getBacktestCooperativeSleepMs(String mode) {
        mode = normalizeBacktestMode(mode);
        if (BACKTEST_MODE_FULL.equals(mode)) return BACKTEST_COOPERATIVE_SLEEP_FULL_MS;
        if (BACKTEST_MODE_NORMAL.equals(mode)) return BACKTEST_COOPERATIVE_SLEEP_NORMAL_MS;
        return BACKTEST_COOPERATIVE_SLEEP_FAST_MS;
    }

    public static boolean isFullBacktestMode(String mode) {
        return BACKTEST_MODE_FULL.equals(normalizeBacktestMode(mode));
    }

    public static boolean isNormalBacktestMode(String mode) {
        return BACKTEST_MODE_NORMAL.equals(normalizeBacktestMode(mode));
    }

    public static boolean isFastBacktestMode(String mode) {
        return BACKTEST_MODE_FAST.equals(normalizeBacktestMode(mode));
    }

    public static int getMaxWindow() {
        int max = 0;
        for (int i = 0; i < WINDOWS.length; i++) {
            if (WINDOWS[i] > max) {
                max = WINDOWS[i];
            }
        }
        return max;
    }

    public static int normalizeManualBacktestWindow(int window) {
        if (window <= 0) return MANUAL_BACKTEST_DEFAULT_WINDOW;
        if (window < MANUAL_BACKTEST_MIN_WINDOW) return MANUAL_BACKTEST_MIN_WINDOW;
        if (window > MANUAL_BACKTEST_MAX_WINDOW) return MANUAL_BACKTEST_MAX_WINDOW;
        return window;
    }

    public static int normalizeAutoValidateWindow(int window) {
        if (window <= 0) return AUTO_VALIDATE_DEFAULT_WINDOW;
        if (window < AUTO_VALIDATE_MIN_WINDOW) return AUTO_VALIDATE_MIN_WINDOW;
        if (window > AUTO_VALIDATE_MAX_WINDOW) return AUTO_VALIDATE_MAX_WINDOW;
        return window;
    }

    public static int chooseRealtimeValidateWindow(int historyCount) {
        if (historyCount >= AUTO_VALIDATE_DEFAULT_WINDOW) return AUTO_VALIDATE_DEFAULT_WINDOW;
        if (historyCount >= AUTO_VALIDATE_FALLBACK_WINDOW) return AUTO_VALIDATE_FALLBACK_WINDOW;
        if (historyCount >= AUTO_VALIDATE_MIN_WINDOW) return AUTO_VALIDATE_MIN_WINDOW;
        return historyCount;
    }

    public static int normalizeAutoBacktestWindow(int window) {
        if (window <= 0) return AUTO_BACKTEST_LIGHT_WINDOW;
        if (window < AUTO_BACKTEST_LIGHT_MIN_WINDOW) return AUTO_BACKTEST_LIGHT_MIN_WINDOW;
        if (window > AUTO_BACKTEST_LIGHT_MAX_WINDOW) return AUTO_BACKTEST_LIGHT_MAX_WINDOW;
        return window;
    }

    public static int normalizeHealthBacktestWindow(int window) {
        if (window <= 0) return AUTO_BACKTEST_HEALTH_WINDOW;
        if (window < AUTO_BACKTEST_LIGHT_MAX_WINDOW) return AUTO_BACKTEST_LIGHT_MAX_WINDOW;
        if (window > AUTO_BACKTEST_HEALTH_MAX_WINDOW) return AUTO_BACKTEST_HEALTH_MAX_WINDOW;
        return window;
    }

    public static int normalizeAutoLearnWindow(int window) {
        if (window <= 0) return AUTO_LEARN_DEFAULT_WINDOW;
        if (window <= AUTO_LEARN_WINDOW_300) return AUTO_LEARN_WINDOW_300;
        return AUTO_LEARN_WINDOW_500;
    }

    public static String normalizeAutoLearnTask(String task) {
        String t = normalizeToken(task);
        if (AUTO_LEARN_TASK_RECENT_300.equals(t)) return AUTO_LEARN_TASK_RECENT_300;
        if (AUTO_LEARN_TASK_RECENT_500.equals(t)) return AUTO_LEARN_TASK_RECENT_500;
        if (AUTO_LEARN_TASK_RANDOM_300.equals(t)) return AUTO_LEARN_TASK_RANDOM_300;
        if (AUTO_LEARN_TASK_RANDOM_500.equals(t)) return AUTO_LEARN_TASK_RANDOM_500;
        if (AUTO_LEARN_TASK_BAD_CASE_REPLAY.equals(t)) return AUTO_LEARN_TASK_BAD_CASE_REPLAY;
        if (AUTO_LEARN_TASK_TEACHER_MATRIX.equals(t)) return AUTO_LEARN_TASK_TEACHER_MATRIX;
        if (AUTO_LEARN_TASK_TEACHER_REPLAY.equals(t)) return AUTO_LEARN_TASK_TEACHER_REPLAY;
        if (AUTO_LEARN_TASK_PREDRAW_REASON.equals(t)) return AUTO_LEARN_TASK_PREDRAW_REASON;
        return AUTO_LEARN_TASK_DEFAULT;
    }

    public static boolean isRandomAutoLearnTask(String task) {
        task = normalizeAutoLearnTask(task);
        return AUTO_LEARN_TASK_RANDOM_300.equals(task) || AUTO_LEARN_TASK_RANDOM_500.equals(task);
    }

    public static boolean isRecentAutoLearnTask(String task) {
        task = normalizeAutoLearnTask(task);
        return AUTO_LEARN_TASK_RECENT_300.equals(task) || AUTO_LEARN_TASK_RECENT_500.equals(task);
    }

    public static boolean isBadCaseReplayTask(String task) {
        return AUTO_LEARN_TASK_BAD_CASE_REPLAY.equals(normalizeAutoLearnTask(task));
    }

    public static boolean isTeacherMatrixTask(String task) {
        return AUTO_LEARN_TASK_TEACHER_MATRIX.equals(normalizeAutoLearnTask(task));
    }

    public static boolean isTeacherReplayTask(String task) {
        return AUTO_LEARN_TASK_TEACHER_REPLAY.equals(normalizeAutoLearnTask(task));
    }

    public static boolean isPreDrawReasonTask(String task) {
        return AUTO_LEARN_TASK_PREDRAW_REASON.equals(normalizeAutoLearnTask(task));
    }

    public static int getAutoLearnWindowByTask(String task) {
        task = normalizeAutoLearnTask(task);
        if (AUTO_LEARN_TASK_RECENT_500.equals(task) || AUTO_LEARN_TASK_RANDOM_500.equals(task)) {
            return AUTO_LEARN_WINDOW_500;
        }
        return AUTO_LEARN_WINDOW_300;
    }

    public static boolean shouldBlockAutoLearnByCountdown(long countdownMs) {
        if (countdownMs < 0L) {
            return false;
        }
        return countdownMs <= ((long) DRAW_PROTECT_SECONDS) * 1000L;
    }

    public static boolean canRunAutoLearn(
            long countdownMs,
            boolean realtimeRunning,
            boolean learningRunning,
            boolean openTradeExists) {
        if (!AUTO_LEARN_ENABLE) return false;
        if (realtimeRunning) return false;
        if (learningRunning) return false;
        if (shouldBlockAutoLearnByCountdown(countdownMs)) return false;
        if (openTradeExists && countdownMs >= 0L && countdownMs <= REALTIME_PRIORITY_HOLD_MS)
            return false;
        return true;
    }

    public static double getLearningWeightByScope(String scope) {
        String s = normalizeStatScope(scope);
        if (STAT_SCOPE_REAL_TRADE.equals(s)) return LEARN_REAL_TRADE_WEIGHT;
        if (STAT_SCOPE_SHADOW.equals(s)) return LEARN_SHADOW_WEIGHT;
        if (STAT_SCOPE_BACKTEST.equals(s)) return LEARN_AUTO_BACKTEST_WEIGHT;
        if (STAT_SCOPE_MANUAL_BACKTEST.equals(s)) return LEARN_MANUAL_BACKTEST_WEIGHT;
        if (STAT_SCOPE_AUTO_VALIDATE.equals(s)) return LEARN_REALTIME_VALIDATE_WEIGHT;
        if (STAT_SCOPE_AUTO_LEARN.equals(s)) return LEARN_AUTO_LEARN_WEIGHT;
        if (STAT_SCOPE_TEACHER_MATRIX.equals(s)) return TEACHER_MATRIX_BACKTEST_WEIGHT;
        if (STAT_SCOPE_TEACHER_REPLAY.equals(s)) return LEARN_TEACHER_REPLAY_WEIGHT;
        if (STAT_SCOPE_HOLD.equals(s)) return LEARN_HOLD_OBSERVE_WEIGHT;
        return LEARN_HOLD_OBSERVE_WEIGHT;
    }

    public static double getPreDrawLearningWeight(String scope, String sourceType) {
        String s = normalizeStatScope(scope);
        String source = normalizeSourceType(sourceType);
        if (STAT_SCOPE_REAL_TRADE.equals(s) && SOURCE_TYPE_REALTIME.equals(source))
            return LEARN_WEIGHT_PREDRAW_REAL_TRADE;
        if (STAT_SCOPE_SHADOW.equals(s)) return LEARN_WEIGHT_PREDRAW_SHADOW;
        if (STAT_SCOPE_AUTO_LEARN.equals(s) || SOURCE_TYPE_AUTO_LEARN.equals(source))
            return LEARN_WEIGHT_PREDRAW_AUTO_LEARN;
        if (STAT_SCOPE_BACKTEST.equals(s)
                || STAT_SCOPE_MANUAL_BACKTEST.equals(s)
                || SOURCE_TYPE_BACKTEST.equals(source)) return LEARN_WEIGHT_PREDRAW_BACKTEST;
        return LEARN_WEIGHT_PREDRAW_HOLD;
    }

    public static double safeAddProb(double base, double delta) {
        return clampProbability(base + delta);
    }

    public static double safePenaltyProb(double base, double penalty) {
        return clampProbability(base - Math.abs(penalty));
    }

    public static double safeAddGate(double gate, double delta) {
        return clampGate(gate + delta);
    }

    public static double limitAdjust(double value, double maxAbs) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return 0.0d;
        }
        if (maxAbs < 0.0d) {
            maxAbs = -maxAbs;
        }
        if (value > maxAbs) {
            return maxAbs;
        }
        if (value < -maxAbs) {
            return -maxAbs;
        }
        return value;
    }

    public static String safeText(String value) {
        return value == null ? "" : value;
    }

    public static String safeIssue(String value) {
        return value == null ? "" : value.trim();
    }

    public static String normalizeToken(String value) {
        if (value == null) {
            return "";
        }
        String v = value.trim();
        if (v.length() == 0) {
            return "";
        }
        return v.toUpperCase(Locale.US);
    }

    public static String nextIssueText(String issue) {
        if (issue == null || issue.length() == 0) {
            return "";
        }
        try {
            long v = Long.parseLong(issue);
            return String.valueOf(v + 1L);
        } catch (Throwable ignore) {
        }

        int end = issue.length() - 1;
        while (end >= 0) {
            char ch = issue.charAt(end);
            if (ch < '0' || ch > '9') {
                break;
            }
            end--;
        }
        if (end >= issue.length() - 1) {
            return issue;
        }

        String prefix = issue.substring(0, end + 1);
        String numText = issue.substring(end + 1);
        try {
            long n = Long.parseLong(numText);
            String next = String.valueOf(n + 1L);
            StringBuilder sb = new StringBuilder();
            sb.append(prefix);
            int zeroCount = numText.length() - next.length();
            for (int i = 0; i < zeroCount; i++) {
                sb.append('0');
            }
            sb.append(next);
            return sb.toString();
        } catch (Throwable ignore) {
        }
        return issue;
    }

    public static String maskKey(String raw) {
        if (raw == null || raw.length() == 0) {
            return "";
        }
        if (!SECURITY_HIDE_KEY) {
            return raw;
        }
        return KEY_MASK_TEXT;
    }

    public static boolean isHardPlayRuleDisabled() {
        return !ALLOW_K4
                && !ALLOW_DYNAMIC_K
                && !ALLOW_MULTI_POSITION_BET
                && !ALLOW_MARTINGALE
                && !ALLOW_DOUBLE_BET
                && !ALLOW_CHASE_BET
                && !ALLOW_MULTI_ISSUE_CHASE
                && !ALLOW_POSITION_SPLIT_BET
                && !ALLOW_AUTO_INCREASE_STAKE;
    }

    public static boolean isFrequencyHardBlocked(double freq) {
        if (Double.isNaN(freq) || Double.isInfinite(freq)) {
            return false;
        }
        if (!FREQUENCY_HARD_BLOCK_ENABLE) {
            return false;
        }
        return freq >= TARGET_FREQ_HARD_MAX;
    }

    public static boolean isFrequencyHigh(double freq) {
        if (Double.isNaN(freq) || Double.isInfinite(freq)) {
            return false;
        }
        return freq >= TARGET_FREQ_MAX;
    }

    public static boolean isFrequencyLow(double freq) {
        if (Double.isNaN(freq) || Double.isInfinite(freq)) {
            return false;
        }
        return freq < TARGET_FREQ_MIN;
    }

    public static boolean isV20TargetFreqLow(double freq) {
        if (Double.isNaN(freq) || Double.isInfinite(freq)) {
            return false;
        }
        return freq < TARGET_FREQ_LOW;
    }

    public static boolean isV20TargetFreqHigh(double freq) {
        if (Double.isNaN(freq) || Double.isInfinite(freq)) {
            return false;
        }
        return freq > TARGET_FREQ_HIGH;
    }

    public static boolean isV20SafeFreq(double freq) {
        if (Double.isNaN(freq) || Double.isInfinite(freq)) {
            return false;
        }
        return freq >= SAFE_FREQ_LOW && freq <= TARGET_FREQ_HIGH;
    }

    public static boolean isProbabilityEnoughForBet(double p) {
        return clampProbability(p) >= MIN_REAL_BET_PROBABILITY;
    }

    public static boolean isElite45ProbabilityEnough(double p) {
        return clampProbability(p) >= ELITE45_MIN_FINAL_P;
    }

    public static boolean isElite45ReasonEdgeEnough(double reasonEdge) {
        return !Double.isNaN(reasonEdge)
                && !Double.isInfinite(reasonEdge)
                && reasonEdge >= ELITE45_MIN_REASON_EDGE;
    }

    public static boolean isElite45HitSimilarityEnough(double hitSimilarity) {
        return !Double.isNaN(hitSimilarity)
                && !Double.isInfinite(hitSimilarity)
                && hitSimilarity >= ELITE45_MIN_HIT_SIMILARITY;
    }

    public static boolean isElite45MissSimilaritySafe(double missSimilarity) {
        return Double.isNaN(missSimilarity)
                || Double.isInfinite(missSimilarity)
                || missSimilarity <= ELITE45_MAX_MISS_SIMILARITY;
    }

    public static boolean isReasonLevelBoostable(String validLevel) {
        String level = normalizeReasonLevel(validLevel);
        return REASON_LEVEL_A_VALID.equals(level) || REASON_LEVEL_B_USEFUL.equals(level);
    }

    public static boolean isReasonLevelFalseOrDecay(String validLevel) {
        String level = normalizeReasonLevel(validLevel);
        return REASON_LEVEL_D_DECAY.equals(level) || REASON_LEVEL_E_FALSE.equals(level);
    }

    public static String normalizeReasonLevel(String validLevel) {
        String s = normalizeToken(validLevel);
        if (REASON_LEVEL_A_VALID.equals(s)) return REASON_LEVEL_A_VALID;
        if (REASON_LEVEL_B_USEFUL.equals(s)) return REASON_LEVEL_B_USEFUL;
        if (REASON_LEVEL_C_EXPLAIN_ONLY.equals(s)) return REASON_LEVEL_C_EXPLAIN_ONLY;
        if (REASON_LEVEL_D_DECAY.equals(s)) return REASON_LEVEL_D_DECAY;
        if (REASON_LEVEL_E_FALSE.equals(s)) return REASON_LEVEL_E_FALSE;
        return REASON_LEVEL_C_EXPLAIN_ONLY;
    }

    public static boolean isRealtimeValidateUsable(int sampleCount, long costMs) {
        if (!AUTO_VALIDATE_ENABLE) return false;
        if (sampleCount < AUTO_VALIDATE_SAMPLE_MIN) return false;
        return costMs <= AUTO_VALIDATE_HARD_TIMEOUT_MS;
    }

    public static double autoValidateGateAdjust(
            double hitRate, double frequency, double maxDrawdown, int recentMissStreak) {
        double adjust = 0.0d;

        if (hitRate > 0.0d && hitRate < AUTO_VALIDATE_HIT_WARN) {
            adjust += AUTO_VALIDATE_GATE_BOOST_RECENT_MISS;
        }
        if (recentMissStreak >= 2) {
            adjust += AUTO_VALIDATE_GATE_BOOST_RECENT_MISS;
        }
        if (Math.abs(maxDrawdown) >= 12.0d) {
            adjust += AUTO_VALIDATE_GATE_BOOST_DRAWDOWN;
        }
        if (frequency >= TARGET_FREQ_MAX) {
            adjust += AUTO_VALIDATE_GATE_BOOST_FREQ_HIGH;
        }
        if (hitRate >= AUTO_VALIDATE_HIT_GOOD && recentMissStreak <= 1) {
            adjust -= AUTO_VALIDATE_GATE_REDUCE_STABLE;
        }
        return limitAdjust(adjust, AUTO_VALIDATE_GATE_MAX_ADJUST);
    }

    public static boolean shouldRealtimeFailOpen(int sampleCount, long costMs) {
        if (!AUTO_VALIDATE_FAIL_OPEN) return false;
        if (sampleCount < AUTO_VALIDATE_SAMPLE_MIN) return true;
        return costMs > AUTO_VALIDATE_TIMEOUT_MS && costMs <= AUTO_VALIDATE_HARD_TIMEOUT_MS;
    }

    public static boolean canApplyTeacherSnapshot(int sampleCount, double confidence, long ageMs) {
        if (!TEACHER_ENABLE || !TEACHER_APPLY_TO_REALTIME) return false;
        if (sampleCount < TEACHER_MIN_SAMPLE) return false;
        if (confidence < TEACHER_MIN_CONFIDENCE) return false;
        return ageMs >= 0L && ageMs <= TEACHER_SNAPSHOT_EXPIRE_MS;
    }

    public static boolean canApplyTeacherMatrix(int sampleCount, double confidence, long ageMs) {
        if (!TEACHER_MATRIX_ENABLE || !TEACHER_MATRIX_FEED_REALTIME) return false;
        if (sampleCount < TEACHER_MATRIX_MIN_SAMPLE_NUMBER) return false;
        if (confidence < TEACHER_MATRIX_MIN_CONFIDENCE) return false;
        return ageMs >= 0L && ageMs <= TEACHER_MATRIX_PROFILE_TTL_MS;
    }

    public static boolean canApplyTeacherAssist(
            int sampleCount, double confidence, double lift, double conflictRate) {
        if (!TEACHER_ASSIST_ENABLE || !TEACHER_ASSIST_APPLY_TO_REALTIME) return false;
        if (sampleCount < TEACHER_ASSIST_MIN_SAMPLE) return false;
        if (confidence < TEACHER_ASSIST_MIN_CONFIDENCE) return false;
        if (lift < TEACHER_ASSIST_MIN_LIFT) return false;
        return conflictRate <= TEACHER_ASSIST_MAX_CONFLICT_RATE;
    }

    public static boolean isStrongEvidence(
            double evidenceScore,
            double aiScore,
            double confidence,
            double riskScore,
            double conflictLevel) {
        if (!STRONG_EVIDENCE_LOW_GATE_ENABLE) return false;
        if (evidenceScore < STRONG_EVIDENCE_MIN_SCORE) return false;
        if (aiScore < STRONG_EVIDENCE_MIN_AI_SCORE) return false;
        if (confidence < STRONG_EVIDENCE_MIN_CONFIDENCE) return false;
        if (riskScore > STRONG_EVIDENCE_MAX_RISK) return false;
        return conflictLevel <= STRONG_EVIDENCE_MAX_CONFLICT;
    }

    public static double buildEvidenceGate(
            double baseGate,
            boolean strongEvidence,
            boolean riskMode,
            boolean weakMode,
            double freq100,
            double evidenceScore,
            double riskScore,
            double conflictLevel) {
        double gate = clampGate(baseGate);
        if (strongEvidence) {
            double reduce = STRONG_EVIDENCE_GATE_REDUCE_MAX;
            if (evidenceScore < EVIDENCE_SCORE_STRONG) {
                reduce *= 0.60d;
            }
            if (riskScore > EVIDENCE_RISK_MAX || conflictLevel > AI_JUDGE_CONFLICT_MAX) {
                reduce *= 0.50d;
            }
            gate -= reduce;
        }
        if (LOW_FREQ_RELAX_ENABLE && freq100 >= 0.0d && freq100 < TARGET_FREQ_MIN) {
            double gap = TARGET_FREQ_MIN - freq100;
            double relax = LOW_FREQ_RELAX_STEP;
            if (gap > 0.050d) {
                relax += LOW_FREQ_RELAX_STEP;
            }
            if (relax > LOW_FREQ_RELAX_MAX) {
                relax = LOW_FREQ_RELAX_MAX;
            }
            gate -= relax;
        }
        if (freq100 >= SMART_FREQ_OVERHEAT) {
            gate += SMART_FREQ_OVERHEAT_GATE_BOOST;
        } else if (freq100 >= SMART_FREQ_STRONG_HIGH) {
            gate += SMART_FREQ_STRONG_HIGH_GATE_BOOST;
        } else if (freq100 >= TARGET_FREQ_MAX) {
            gate += SMART_FREQ_HIGH_GATE_BOOST;
        }
        if (riskMode) {
            gate += 0.030d;
        }
        if (weakMode) {
            gate += 0.012d;
        }
        if (riskScore >= EVIDENCE_RISK_HARD) {
            gate += 0.025d;
        }
        if (conflictLevel >= AI_JUDGE_CONFLICT_HARD) {
            gate += 0.025d;
        }
        return clampGate(gate);
    }

    public static double buildSmartFrequencyGateAdjust(
            double freq100, double teacherConfidence, double finalP, boolean strongEvidence) {
        if (!SMART_DYNAMIC_FREQUENCY_ENABLE) {
            return 0.0d;
        }
        if (Double.isNaN(freq100) || Double.isInfinite(freq100)) {
            return 0.0d;
        }

        double adjust = 0.0d;

        if (freq100 < SMART_FREQ_LOW) {
            if (strongEvidence && teacherConfidence >= TEACHER_MATRIX_GOOD_CONFIDENCE) {
                adjust -= SMART_FREQ_LOW_RELAX_MAX;
            }
        }

        if (freq100 >= SMART_FREQ_OVERHEAT) {
            adjust += SMART_FREQ_OVERHEAT_GATE_BOOST;
        } else if (freq100 >= SMART_FREQ_STRONG_HIGH) {
            adjust += SMART_FREQ_STRONG_HIGH_GATE_BOOST;
        } else if (freq100 >= TARGET_FREQ_MAX) {
            adjust += SMART_FREQ_HIGH_GATE_BOOST;
        }

        if (finalP >= A_PLUS_MIN_FINAL_P
                && teacherConfidence >= TEACHER_MATRIX_STRONG_CONFIDENCE
                && freq100 <= TARGET_FREQ_IDEAL_HIGH) {
            adjust -= 0.006d;
        }

        return limitAdjust(adjust, 0.040d);
    }

    public static double buildV20FrequencyGateAdjust(
            String grade,
            double recentFreq,
            int globalLoseStreak,
            int sameClassLoseStreak,
            boolean shadowPass) {
        double adjust = 0.0d;
        grade = normalizeGrade(grade);

        if (isGlobalShadowRecovery(globalLoseStreak)) {
            adjust += 0.060d;
        } else if (isGlobalKeepOnlyS(globalLoseStreak)) {
            adjust += 0.040d;
        }

        if (isSameClassHardFreeze(sameClassLoseStreak)) {
            adjust += 0.050d;
        } else if (isSameClassFreeze(sameClassLoseStreak)) {
            adjust += 0.030d;
        }

        if (recentFreq > TARGET_FREQ_HIGH) {
            if (GRADE_B.equals(grade)) {
                adjust += 0.030d;
            } else {
                adjust += 0.014d;
            }
        } else if (recentFreq < SAFE_FREQ_LOW && shadowPass && GRADE_B.equals(grade)) {
            adjust -= 0.008d;
        }

        if (GRADE_C.equals(grade)) {
            adjust += 0.080d;
        }

        return limitAdjust(adjust, 0.080d);
    }

    public static double buildElite45ReasonGateAdjust(
            double reasonEdge,
            double hitSimilarity,
            double missSimilarity,
            int falseReasonCount,
            double comboConflict,
            boolean fingerprintCooling) {
        double adjust = 0.0d;

        if (reasonEdge < ELITE45_MIN_REASON_EDGE) {
            adjust += 0.012d;
        }
        if (hitSimilarity < ELITE45_MIN_HIT_SIMILARITY) {
            adjust += 0.010d;
        }
        if (missSimilarity > ELITE45_MAX_MISS_SIMILARITY) {
            adjust += ELITE45_REASON_GATE_BOOST_MISS_SIM;
        }
        if (missSimilarity > 0.500d) {
            adjust += ELITE45_REASON_GATE_BOOST_MISS_SIM_HARD;
        }
        if (falseReasonCount > ELITE45_MAX_FALSE_REASON_COUNT) {
            adjust += ELITE45_REASON_GATE_BOOST_FALSE;
        }
        if (comboConflict > ELITE45_MAX_COMBO_CONFLICT) {
            adjust += ELITE45_REASON_GATE_BOOST_COMBO_CONFLICT;
        }
        if (fingerprintCooling) {
            adjust += 0.040d;
        }

        return limitAdjust(adjust, 0.080d);
    }

    public static double buildTeacherMatrixProbAdjust(
            int sampleCount, double hitRate, double confidence) {
        if (sampleCount < TEACHER_MATRIX_MIN_SAMPLE_NUMBER) {
            return 0.0d;
        }

        double adjust = 0.0d;

        if (hitRate >= TEACHER_MATRIX_NUMBER_HIT_RATE_STRONG
                && confidence >= TEACHER_MATRIX_GOOD_CONFIDENCE) {
            adjust += TEACHER_MATRIX_MAX_PROB_BOOST;
        } else if (hitRate >= TEACHER_MATRIX_NUMBER_HIT_RATE_GOOD) {
            adjust += TEACHER_MATRIX_MAX_PROB_BOOST * 0.55d;
        } else if (hitRate <= TEACHER_MATRIX_NUMBER_HIT_RATE_WEAK) {
            adjust -= TEACHER_MATRIX_MAX_PROB_PENALTY * 0.70d;
        }

        if (confidence < TEACHER_MATRIX_MIN_CONFIDENCE) {
            adjust *= 0.30d;
        } else if (confidence < TEACHER_MATRIX_GOOD_CONFIDENCE) {
            adjust *= 0.65d;
        }

        return limitAdjust(adjust, TEACHER_MATRIX_MAX_PROB_BOOST);
    }

    public static double buildTeacherMatrixGateAdjust(
            int sampleCount, double hitRate, double confidence) {
        if (sampleCount < TEACHER_MATRIX_MIN_SAMPLE_NUMBER) {
            return TEACHER_MATRIX_MAX_GATE_BOOST * 0.50d;
        }

        double adjust = 0.0d;

        if (hitRate >= TEACHER_MATRIX_NUMBER_HIT_RATE_STRONG
                && confidence >= TEACHER_MATRIX_GOOD_CONFIDENCE) {
            adjust -= TEACHER_MATRIX_MAX_GATE_REDUCE;
        } else if (hitRate >= TEACHER_MATRIX_NUMBER_HIT_RATE_GOOD) {
            adjust -= TEACHER_MATRIX_MAX_GATE_REDUCE * 0.50d;
        } else if (hitRate <= TEACHER_MATRIX_NUMBER_HIT_RATE_WEAK) {
            adjust += TEACHER_MATRIX_MAX_GATE_BOOST * 0.70d;
        }

        if (confidence < TEACHER_MATRIX_MIN_CONFIDENCE) {
            adjust += TEACHER_MATRIX_MAX_GATE_BOOST * 0.35d;
        }

        return limitAdjust(adjust, TEACHER_MATRIX_MAX_GATE_BOOST);
    }

    public static double buildTeacherAssistProbAdjust(
            int sampleCount, double lift, double confidence, double conflictRate) {
        if (!canApplyTeacherAssist(sampleCount, confidence, lift, conflictRate)) {
            return 0.0d;
        }

        double adjust = lift * 0.35d;

        if (confidence >= TEACHER_ASSIST_STRONG_CONFIDENCE) {
            adjust *= 1.20d;
        }

        if (conflictRate > 0.500d) {
            adjust *= 0.50d;
        }

        return limitAdjust(adjust, TEACHER_ASSIST_MAX_PROB_BOOST);
    }

    public static double buildTeacherAssistGateAdjust(
            int sampleCount, double lift, double confidence, double conflictRate) {
        if (!TEACHER_ASSIST_ENABLE) {
            return 0.0d;
        }

        if (sampleCount < TEACHER_ASSIST_MIN_SAMPLE) {
            return 0.0d;
        }

        double adjust = 0.0d;

        if (lift >= TEACHER_ASSIST_STRONG_LIFT && confidence >= TEACHER_ASSIST_STRONG_CONFIDENCE) {
            adjust -= TEACHER_ASSIST_MAX_GATE_REDUCE;
        } else if (lift >= TEACHER_ASSIST_MIN_LIFT && confidence >= TEACHER_ASSIST_MIN_CONFIDENCE) {
            adjust -= TEACHER_ASSIST_MAX_GATE_REDUCE * 0.50d;
        } else if (lift < 0.0d || conflictRate > TEACHER_ASSIST_MAX_CONFLICT_RATE) {
            adjust += TEACHER_ASSIST_MAX_GATE_BOOST;
        }

        return limitAdjust(adjust, TEACHER_ASSIST_MAX_GATE_BOOST);
    }

    public static int numberToMaskBit(int number) {
        if (!isValidNumber(number)) {
            return 0;
        }
        return 1 << (number - 1);
    }

    public static int buildK3Mask(int n1, int n2, int n3) {
        if (!isValidK3Numbers(n1, n2, n3)) {
            return 0;
        }
        int mask = 0;
        mask |= numberToMaskBit(n1);
        mask |= numberToMaskBit(n2);
        mask |= numberToMaskBit(n3);
        return mask;
    }

    public static boolean maskContainsNumber(int mask, int number) {
        int bit = numberToMaskBit(number);
        return bit != 0 && (mask & bit) != 0;
    }

    public static int countK3MaskNumbers(int mask) {
        int count = 0;
        for (int n = MIN_NUMBER; n <= MAX_NUMBER; n++) {
            if (maskContainsNumber(mask, n)) {
                count++;
            }
        }
        return count;
    }

    public static boolean isValidK3Mask(int mask) {
        return (mask & ~1023) == 0 && Integer.bitCount(mask) == FIXED_K;
    }

    public static String k3MaskToText(int mask) {
        StringBuilder sb = new StringBuilder();
        int count = 0;
        for (int n = MIN_NUMBER; n <= MAX_NUMBER; n++) {
            if (maskContainsNumber(mask, n)) {
                if (count > 0) {
                    sb.append(",");
                }
                if (n < 10) {
                    sb.append("0");
                }
                sb.append(n);
                count++;
            }
        }
        return sb.toString();
    }

    public static String buildAutoBacktestDedupKey(
            String mode, int window, String endIssue, int historyCount) {
        StringBuilder sb = new StringBuilder();
        sb.append(AUTO_BACKTEST_DEDUP_PREFIX);
        sb.append("_");
        sb.append(normalizeBacktestMode(mode));
        sb.append("_W");
        sb.append(window);
        sb.append("_E");
        sb.append(endIssue == null ? "" : endIssue);
        sb.append("_H");
        sb.append(historyCount);
        return sb.toString();
    }

    public static String buildAutoLearnTaskKey(String task, int window) {
        StringBuilder sb = new StringBuilder();
        sb.append("ALT");
        sb.append("_");
        sb.append(normalizeAutoLearnTask(task));
        sb.append("_W");
        sb.append(normalizeAutoLearnWindow(window));
        return sb.toString();
    }

    public static String buildAutoLearnSegmentKey(
            String task, int window, int segmentStart, int segmentEnd) {
        StringBuilder sb = new StringBuilder();
        sb.append(buildAutoLearnTaskKey(task, window));
        sb.append("_S");
        sb.append(segmentStart);
        sb.append("_E");
        sb.append(segmentEnd);
        return sb.toString();
    }

    public static String buildLearningCaseKey(String targetIssue, int position) {
        StringBuilder sb = new StringBuilder();
        sb.append(safeIssue(targetIssue));
        sb.append("_P");
        sb.append(position);
        return sb.toString();
    }

    public static String buildRuntimeProfileKey(String stateKey, int position, int k3Mask) {
        StringBuilder sb = new StringBuilder();
        sb.append(safeText(stateKey));
        sb.append("_P");
        sb.append(position);
        sb.append("_K");
        sb.append(k3Mask);
        return sb.toString();
    }

    public static String buildStateNumberProfileKey(String stateKey, int position, int number) {
        StringBuilder sb = new StringBuilder();
        sb.append(safeText(stateKey));
        sb.append("_P");
        sb.append(position);
        sb.append("_N");
        sb.append(number);
        return sb.toString();
    }

    public static String buildTeacherMatrixSampleKey(String issue, int position, int number) {
        StringBuilder sb = new StringBuilder();
        sb.append(safeIssue(issue));
        sb.append("_P");
        sb.append(position);
        sb.append("_N");
        sb.append(number);
        return sb.toString();
    }

    public static String buildTeacherMatrixProfileKey(String stateKey, int position, int number) {
        StringBuilder sb = new StringBuilder();
        sb.append(buildStateKeyPart(stateKey));
        sb.append("_P");
        sb.append(position);
        sb.append("_N");
        sb.append(number);
        return sb.toString();
    }

    public static String buildTeacherMatrixComboKey(String stateKey, int position, int k3Mask) {
        StringBuilder sb = new StringBuilder();
        sb.append(buildStateKeyPart(stateKey));
        sb.append("_P");
        sb.append(position);
        sb.append("_K3");
        sb.append(k3Mask);
        return sb.toString();
    }

    public static String buildTeacherReplayTargetKey(String targetIssue) {
        StringBuilder sb = new StringBuilder();
        sb.append("TR_TARGET_");
        sb.append(safeIssue(targetIssue));
        return sb.toString();
    }

    public static String buildTeacherReplayCaseKey(String targetIssue, int position, int k3Mask) {
        StringBuilder sb = new StringBuilder();
        sb.append("TR_CASE_");
        sb.append(safeIssue(targetIssue));
        sb.append("_P");
        sb.append(position);
        sb.append("_K");
        sb.append(k3Mask);
        return sb.toString();
    }

    public static String buildTeacherReplayNumberCaseKey(
            String targetIssue, int position, int number) {
        StringBuilder sb = new StringBuilder();
        sb.append("TR_NUM_");
        sb.append(safeIssue(targetIssue));
        sb.append("_P");
        sb.append(position);
        sb.append("_N");
        sb.append(number);
        return sb.toString();
    }

    public static String buildTeacherAssistProfileKey(String stateKey, int position, int k3Mask) {
        StringBuilder sb = new StringBuilder();
        sb.append("TA_");
        sb.append(buildStateKeyPart(stateKey));
        sb.append("_P");
        sb.append(position);
        sb.append("_K3");
        sb.append(k3Mask);
        return sb.toString();
    }

    public static String buildPreDrawReasonSampleKey(
            String targetIssue, int position, int number, String reasonFingerprint) {
        StringBuilder sb = new StringBuilder();
        sb.append("PDR");
        sb.append("_T");
        sb.append(safeIssue(targetIssue));
        sb.append("_P");
        sb.append(position);
        sb.append("_N");
        sb.append(number);
        sb.append("_R");
        sb.append(safeHashText(reasonFingerprint));
        return sb.toString();
    }

    public static String buildReasonContrastProfileKey(
            String reasonFingerprint, String stateKey, int position, int number) {
        StringBuilder sb = new StringBuilder();
        sb.append("RCP");
        sb.append("_");
        sb.append(safeHashText(reasonFingerprint));
        sb.append("_");
        sb.append(buildStateKeyPart(stateKey));
        sb.append("_P");
        sb.append(position);
        sb.append("_N");
        sb.append(number);
        return sb.toString();
    }

    public static String buildReasonCausalKey(String reasonAtom, String reasonComboKey) {
        StringBuilder sb = new StringBuilder();
        sb.append("RCM");
        sb.append("_V");
        sb.append(REASON_VERSION);
        sb.append("_A");
        sb.append(safeHashText(reasonAtom));
        sb.append("_C");
        sb.append(safeHashText(reasonComboKey));
        return sb.toString();
    }

    public static String buildFalseStrongKey(
            String stateKey, int position, int number, int k3Mask, String reasonFingerprint) {
        StringBuilder sb = new StringBuilder();
        sb.append("FS");
        sb.append("_");
        sb.append(buildStateKeyPart(stateKey));
        sb.append("_P");
        sb.append(position);
        sb.append("_N");
        sb.append(number);
        sb.append("_K");
        sb.append(k3Mask);
        sb.append("_R");
        sb.append(safeHashText(reasonFingerprint));
        return sb.toString();
    }

    public static String buildV20FalseStrongKey(
            String stateKey,
            String sourceKey,
            int position,
            String rankMode,
            String thirdGuardRole) {
        StringBuilder sb = new StringBuilder();
        sb.append("V20FS");
        sb.append("_");
        sb.append(buildStateKeyPart(stateKey));
        sb.append("_S");
        sb.append(safeHashText(sourceKey));
        sb.append("_P");
        sb.append(position);
        sb.append("_R");
        sb.append(buildStateKeyPart(rankMode));
        sb.append("_G");
        sb.append(buildStateKeyPart(thirdGuardRole));
        return sb.toString();
    }

    public static String buildV20SameClassKey(
            String stateKey,
            String sourceKey,
            int position,
            String rankMode,
            String thirdGuardRole,
            String numberLifeRole,
            String migrationRole) {
        StringBuilder sb = new StringBuilder();
        sb.append("V20SC");
        sb.append("_");
        sb.append(buildStateKeyPart(stateKey));
        sb.append("_S");
        sb.append(safeHashText(sourceKey));
        sb.append("_P");
        sb.append(position);
        sb.append("_R");
        sb.append(buildStateKeyPart(rankMode));
        sb.append("_G");
        sb.append(buildStateKeyPart(thirdGuardRole));
        sb.append("_L");
        sb.append(buildStateKeyPart(numberLifeRole));
        sb.append("_M");
        sb.append(buildStateKeyPart(migrationRole));
        return sb.toString();
    }

    public static String buildV20RuleKey(String stateKey, String sourceKey, String causeKey) {
        StringBuilder sb = new StringBuilder();
        sb.append("V20RULE");
        sb.append("_");
        sb.append(buildStateKeyPart(stateKey));
        sb.append("_S");
        sb.append(safeHashText(sourceKey));
        sb.append("_C");
        sb.append(safeHashText(causeKey));
        return sb.toString();
    }

    public static String buildV20CandidateAuditKey(String targetIssue, int position, int k3Mask) {
        StringBuilder sb = new StringBuilder();
        sb.append("V20CA");
        sb.append("_T");
        sb.append(safeIssue(targetIssue));
        sb.append("_P");
        sb.append(position);
        sb.append("_K");
        sb.append(k3Mask);
        return sb.toString();
    }

    public static String buildV20RankCounterKey(String stateKey, String rankGroup) {
        StringBuilder sb = new StringBuilder();
        sb.append("V20RK");
        sb.append("_");
        sb.append(buildStateKeyPart(stateKey));
        sb.append("_");
        sb.append(buildStateKeyPart(rankGroup));
        return sb.toString();
    }

    public static String buildV20ThirdGuardKey(
            String stateKey, int position, int mainNumber, int trendNumber, int guardNumber) {
        StringBuilder sb = new StringBuilder();
        sb.append("V20TG");
        sb.append("_");
        sb.append(buildStateKeyPart(stateKey));
        sb.append("_P");
        sb.append(position);
        sb.append("_M");
        sb.append(mainNumber);
        sb.append("_T");
        sb.append(trendNumber);
        sb.append("_G");
        sb.append(guardNumber);
        return sb.toString();
    }

    public static String buildComboRoleKey(String stateKey, int position, int k3Mask) {
        StringBuilder sb = new StringBuilder();
        sb.append("K3R");
        sb.append("_");
        sb.append(buildStateKeyPart(stateKey));
        sb.append("_P");
        sb.append(position);
        sb.append("_K");
        sb.append(k3Mask);
        return sb.toString();
    }

    public static String buildElite45AuditKey(String targetIssue, int position, int k3Mask) {
        StringBuilder sb = new StringBuilder();
        sb.append("E45");
        sb.append("_T");
        sb.append(safeIssue(targetIssue));
        sb.append("_P");
        sb.append(position);
        sb.append("_K");
        sb.append(k3Mask);
        return sb.toString();
    }

    public static String buildReasonFingerprintRaw(
            String stateKey,
            String positionPhase,
            String numberPhase,
            String driftPhase,
            String recoveryPhase,
            String relationPhase,
            String riskPhase) {
        StringBuilder sb = new StringBuilder();
        sb.append("RV");
        sb.append(REASON_VERSION);
        sb.append("|");
        sb.append(buildStateKeyPart(stateKey));
        sb.append("|");
        sb.append(buildStateKeyPart(positionPhase));
        sb.append("|");
        sb.append(buildStateKeyPart(numberPhase));
        sb.append("|");
        sb.append(buildStateKeyPart(driftPhase));
        sb.append("|");
        sb.append(buildStateKeyPart(recoveryPhase));
        sb.append("|");
        sb.append(buildStateKeyPart(relationPhase));
        sb.append("|");
        sb.append(buildStateKeyPart(riskPhase));
        return sb.toString();
    }

    public static String buildReasonFingerprint(
            String stateKey,
            String positionPhase,
            String numberPhase,
            String driftPhase,
            String recoveryPhase,
            String relationPhase,
            String riskPhase) {
        return safeHashText(
                buildReasonFingerprintRaw(
                        stateKey,
                        positionPhase,
                        numberPhase,
                        driftPhase,
                        recoveryPhase,
                        relationPhase,
                        riskPhase));
    }

    public static String buildTeacherMatrixStateKey(
            String trend,
            String stateMode,
            String shape,
            String drift,
            String breakState,
            String recovery) {
        StringBuilder sb = new StringBuilder();
        sb.append(buildStateKeyPart(trend));
        sb.append("|");
        sb.append(buildStateKeyPart(stateMode));
        sb.append("|");
        sb.append(buildStateKeyPart(shape));
        sb.append("|");
        sb.append(buildStateKeyPart(drift));
        sb.append("|");
        sb.append(buildStateKeyPart(breakState));
        sb.append("|");
        sb.append(buildStateKeyPart(recovery));
        return sb.toString();
    }

    public static String buildV20StateKey(
            String trend,
            String shape,
            String entropy,
            String drift,
            String flow,
            String cycle60,
            String boardRisk) {
        StringBuilder sb = new StringBuilder();
        sb.append(buildStateKeyPart(trend));
        sb.append("|");
        sb.append(buildStateKeyPart(shape));
        sb.append("|");
        sb.append(buildStateKeyPart(entropy));
        sb.append("|");
        sb.append(buildStateKeyPart(drift));
        sb.append("|");
        sb.append(buildStateKeyPart(flow));
        sb.append("|");
        sb.append(buildStateKeyPart(cycle60));
        sb.append("|");
        sb.append(buildStateKeyPart(boardRisk));
        return sb.toString();
    }

    public static String buildCycleKey(String issue, int cycle) {
        String safe = safeIssue(issue);
        if (cycle <= 0 || safe.length() == 0) {
            return "C" + cycle + "_0";
        }
        try {
            long v = Long.parseLong(safe);
            long mod = v % cycle;
            return "C" + cycle + "_" + mod;
        } catch (Throwable ignore) {
        }
        return "C" + cycle + "_0";
    }

    public static String buildStateKeyPart(String value) {
        if (value == null || value.length() == 0) {
            return MODE_UNKNOWN;
        }
        return value.replace('|', '_').replace(' ', '_');
    }

    public static String safeHashText(String value) {
        String v = safeText(value);
        if (v.length() == 0) {
            return "0";
        }
        int h = v.hashCode();
        String hex = Integer.toHexString(h);
        if (hex == null || hex.length() == 0) {
            return "0";
        }
        return hex.toUpperCase(Locale.US);
    }

    public static String normalizeTeacherReplayStatus(String status) {
        String s = normalizeToken(status);
        if (TEACHER_REPLAY_STATUS_WAITING.equals(s)) return TEACHER_REPLAY_STATUS_WAITING;
        if (TEACHER_REPLAY_STATUS_RUNNING.equals(s)) return TEACHER_REPLAY_STATUS_RUNNING;
        if (TEACHER_REPLAY_STATUS_OK.equals(s)) return TEACHER_REPLAY_STATUS_OK;
        if (TEACHER_REPLAY_STATUS_SKIP.equals(s)) return TEACHER_REPLAY_STATUS_SKIP;
        if (TEACHER_REPLAY_STATUS_ERROR.equals(s)) return TEACHER_REPLAY_STATUS_ERROR;
        return TEACHER_REPLAY_STATUS_IDLE;
    }

    public static String normalizeTeacherAssistStatus(String status) {
        String s = normalizeToken(status);
        if (TEACHER_ASSIST_STATUS_READY.equals(s)) return TEACHER_ASSIST_STATUS_READY;
        if (TEACHER_ASSIST_STATUS_APPLIED.equals(s)) return TEACHER_ASSIST_STATUS_APPLIED;
        if (TEACHER_ASSIST_STATUS_RISK.equals(s)) return TEACHER_ASSIST_STATUS_RISK;
        if (TEACHER_ASSIST_STATUS_STALE.equals(s)) return TEACHER_ASSIST_STATUS_STALE;
        if (TEACHER_ASSIST_STATUS_ERROR.equals(s)) return TEACHER_ASSIST_STATUS_ERROR;
        return TEACHER_ASSIST_STATUS_NONE;
    }

    public static String normalizeAction(String action) {
        if (action == null) {
            return DECISION_HOLD;
        }

        String a = action.trim();

        if (a.length() == 0) {
            return DECISION_HOLD;
        }

        if ("BET".equalsIgnoreCase(a)
                || "下注".equals(a)
                || "出手".equals(a)
                || "REAL_BET".equalsIgnoreCase(a)
                || "REAL_TRADE".equalsIgnoreCase(a)) {
            return DECISION_BET;
        }

        if ("SHADOW".equalsIgnoreCase(a)
                || "观察".equals(a)
                || "影子".equals(a)
                || "影子单".equals(a)
                || "SHADOW_BET".equalsIgnoreCase(a)) {
            return DECISION_SHADOW;
        }

        if ("HOLD".equalsIgnoreCase(a)
                || "NO_BET".equalsIgnoreCase(a)
                || "WAIT".equalsIgnoreCase(a)
                || "PASS".equalsIgnoreCase(a)
                || "跳过".equals(a)
                || "不出手".equals(a)
                || "等待".equals(a)) {
            return DECISION_HOLD;
        }

        return DECISION_HOLD;
    }

    public static double clampDynamicCausalScore(double v) {
        if (Double.isNaN(v) || Double.isInfinite(v)) {
            return 0.0d;
        }
        if (v < 0.0d) {
            return 0.0d;
        }
        if (v > 1.0d) {
            return 1.0d;
        }
        return v;
    }

    public static double safeRate(int hit, int sample) {
        if (sample <= 0) {
            return 0.0d;
        }
        return ((double) hit) / ((double) sample);
    }

    public static double liftFromRandomBaseline(double hitRate) {
        return hitRate - DYNAMIC_CLUSTER_RANDOM_BASELINE;
    }

    public static boolean isDynamicClusterStatusBetEligible(String status) {
        String s = normalizeDynamicClusterStatus(status);
        return CLUSTER_STATUS_READY.equals(s) || CLUSTER_STATUS_STRONG.equals(s);
    }

    public static boolean isDynamicClusterStatusHardRisk(String status) {
        String s = normalizeDynamicClusterStatus(status);
        return CLUSTER_STATUS_RISK.equals(s)
                || CLUSTER_STATUS_WEAK.equals(s)
                || CLUSTER_STATUS_FREEZE.equals(s)
                || CLUSTER_STATUS_DEAD.equals(s)
                || CLUSTER_STATUS_SAMPLE_WEAK.equals(s)
                || CLUSTER_STATUS_RECENT_WEAK.equals(s);
    }

    public static String normalizeDynamicClusterStatus(String status) {
        if (status == null) {
            return CLUSTER_STATUS_UNKNOWN;
        }
        String s = status.trim();
        if (s.length() == 0) {
            return CLUSTER_STATUS_UNKNOWN;
        }
        String u = s.toUpperCase(Locale.US);
        if ("STRONG".equals(u) || "CLUSTER_STRONG".equals(u)) {
            return CLUSTER_STATUS_STRONG;
        }
        if ("READY".equals(u) || "CLUSTER_READY".equals(u) || "PROMOTE_READY".equals(u)) {
            return CLUSTER_STATUS_READY;
        }
        if ("OBSERVE_READY".equals(u) || "WATCH_READY".equals(u)) {
            return CLUSTER_STATUS_OBSERVE_READY;
        }
        if ("WATCH".equals(u) || "NEW_WATCH".equals(u)) {
            return CLUSTER_STATUS_WATCH;
        }
        if ("NEW".equals(u)) {
            return CLUSTER_STATUS_NEW;
        }
        if ("WEAK".equals(u) || "CLUSTER_WEAK".equals(u) || "WEAK_NO_BET".equals(u)) {
            return CLUSTER_STATUS_WEAK;
        }
        if ("RISK".equals(u) || "BELOW_BASELINE".equals(u)) {
            return CLUSTER_STATUS_RISK;
        }
        if ("FREEZE".equals(u) || "FROZEN".equals(u)) {
            return CLUSTER_STATUS_FREEZE;
        }
        if ("DEAD".equals(u) || "DISABLED".equals(u)) {
            return CLUSTER_STATUS_DEAD;
        }
        if ("SAMPLE_WEAK".equals(u) || "NO_SAMPLE".equals(u)) {
            return CLUSTER_STATUS_SAMPLE_WEAK;
        }
        if ("RECENT_WEAK".equals(u) || "RECENT_INVALID".equals(u)) {
            return CLUSTER_STATUS_RECENT_WEAK;
        }
        return u;
    }

    public static String decideDynamicClusterStatus(
            int sample,
            int recentSample,
            double hitRate,
            double recentHitRate,
            int maxLose,
            double confidence,
            double volatility,
            double counterScore) {
        if (sample <= 0) {
            return CLUSTER_STATUS_NEW;
        }
        if (sample < DYNAMIC_CLUSTER_MIN_SAMPLE_WATCH) {
            return CLUSTER_STATUS_SAMPLE_WEAK;
        }
        if (hitRate < DYNAMIC_CLUSTER_FREEZE_HIT_RATE
                || maxLose >= DYNAMIC_CLUSTER_HARD_FREEZE_LOSE
                || counterScore >= 0.900d) {
            return CLUSTER_STATUS_FREEZE;
        }
        if (hitRate < DYNAMIC_CLUSTER_RANDOM_BASELINE || maxLose >= DYNAMIC_CLUSTER_FREEZE_LOSE) {
            return CLUSTER_STATUS_RISK;
        }
        if (recentSample >= DYNAMIC_CLUSTER_RECENT_MIN_SAMPLE
                && recentHitRate < DYNAMIC_CLUSTER_RECENT_FREEZE_HIT_RATE) {
            return CLUSTER_STATUS_RECENT_WEAK;
        }
        if (sample >= DYNAMIC_CLUSTER_MIN_SAMPLE_STRONG
                && hitRate >= DYNAMIC_CLUSTER_STRONG_HIT_RATE
                && (recentSample < DYNAMIC_CLUSTER_RECENT_MIN_SAMPLE
                        || recentHitRate >= DYNAMIC_CLUSTER_RECENT_STRONG_HIT_RATE)
                && confidence >= DYNAMIC_CLUSTER_MIN_CONFIDENCE_STRONG
                && volatility <= DYNAMIC_CLUSTER_MAX_VOLATILITY_STRONG
                && counterScore <= DYNAMIC_CLUSTER_MAX_COUNTER_STRONG
                && maxLose <= DYNAMIC_CLUSTER_MAX_LOSE_STRONG) {
            return CLUSTER_STATUS_STRONG;
        }
        if (sample >= DYNAMIC_CLUSTER_MIN_SAMPLE_READY
                && hitRate >= DYNAMIC_CLUSTER_READY_HIT_RATE
                && (recentSample < DYNAMIC_CLUSTER_RECENT_MIN_SAMPLE
                        || recentHitRate >= DYNAMIC_CLUSTER_RECENT_READY_HIT_RATE)
                && confidence >= DYNAMIC_CLUSTER_MIN_CONFIDENCE_READY
                && volatility <= DYNAMIC_CLUSTER_MAX_VOLATILITY_READY
                && counterScore <= DYNAMIC_CLUSTER_MAX_COUNTER_READY
                && maxLose <= DYNAMIC_CLUSTER_MAX_LOSE_READY) {
            return CLUSTER_STATUS_READY;
        }
        if (hitRate >= DYNAMIC_CLUSTER_OBSERVE_READY_HIT_RATE) {
            return CLUSTER_STATUS_OBSERVE_READY;
        }
        return CLUSTER_STATUS_WATCH;
    }

    public static boolean dynamicClusterAllowsBet(
            int sample,
            int recentSample,
            double hitRate,
            double recentHitRate,
            int maxLose,
            double confidence,
            double volatility,
            double counterScore,
            String status) {
        String s = normalizeDynamicClusterStatus(status);
        if (isDynamicClusterStatusHardRisk(s) || CLUSTER_STATUS_DEAD.equals(s)) {
            return false;
        }
        if (!STRICT_DYNAMIC_CLUSTER_REQUIRED_FOR_BET && REAL_BET_PROBE_ALLOW_DYNAMIC_NEW_WATCH) {
            if (maxLose >= DYNAMIC_CLUSTER_HARD_FREEZE_LOSE) {
                return false;
            }
            if (sample >= DYNAMIC_CLUSTER_MIN_SAMPLE_WATCH
                    && hitRate < DYNAMIC_CLUSTER_RANDOM_BASELINE) {
                return false;
            }
            if (recentSample >= DYNAMIC_CLUSTER_RECENT_MIN_SAMPLE
                    && recentHitRate < DYNAMIC_CLUSTER_RECENT_FREEZE_HIT_RATE) {
                return false;
            }
            if (counterScore >= 0.900d) {
                return false;
            }
            return true;
        }
        if (!isDynamicClusterStatusBetEligible(s)) {
            return false;
        }
        if (sample < DYNAMIC_CLUSTER_MIN_SAMPLE_READY) {
            return false;
        }
        if (hitRate < DYNAMIC_CLUSTER_READY_HIT_RATE) {
            return false;
        }
        if (recentSample >= DYNAMIC_CLUSTER_RECENT_MIN_SAMPLE
                && recentHitRate < DYNAMIC_CLUSTER_RECENT_READY_HIT_RATE) {
            return false;
        }
        if (maxLose > DYNAMIC_CLUSTER_MAX_LOSE_READY) {
            return false;
        }
        if (confidence < DYNAMIC_CLUSTER_MIN_CONFIDENCE_READY) {
            return false;
        }
        if (volatility > DYNAMIC_CLUSTER_MAX_VOLATILITY_READY) {
            return false;
        }
        if (counterScore > DYNAMIC_CLUSTER_MAX_COUNTER_READY) {
            return false;
        }
        return true;
    }

    public static boolean needSupplyAllowsBet(
            double matchScore, double thirdQuality, double supplyRisk, double roleDiversity) {
        if (supplyRisk >= NEED_SUPPLY_HARD_RISK) {
            return false;
        }
        if (matchScore < NEED_SUPPLY_MIN_MATCH_SCORE) {
            return false;
        }
        if (thirdQuality < NEED_SUPPLY_MIN_THIRD_QUALITY) {
            return false;
        }
        if (roleDiversity < NEED_SUPPLY_ROLE_DIVERSITY_MIN * 0.80d) {
            return false;
        }
        return true;
    }

    public static boolean adaptiveActionAllowsBet(String action) {
        if (action == null || action.trim().length() == 0) {
            return !STRICT_ADAPTIVE_JUDGE_REQUIRED_FOR_BET && REAL_BET_PROBE_ALLOW_ADAPTIVE_MISSING;
        }
        String a = action.trim().toUpperCase(Locale.US);
        return ADAPTIVE_ACTION_PASS.equals(a)
                || ADAPTIVE_ACTION_READY.equals(a)
                || ADAPTIVE_ACTION_STRONG.equals(a)
                || ADAPTIVE_ACTION_PROMOTE.equals(a);
    }

    public static boolean adaptiveActionIsRisk(String action) {
        if (action == null || action.trim().length() == 0) {
            return STRICT_ADAPTIVE_JUDGE_REQUIRED_FOR_BET && !REAL_BET_PROBE_ALLOW_ADAPTIVE_MISSING;
        }
        String a = action.trim().toUpperCase(Locale.US);
        return ADAPTIVE_ACTION_DOWNGRADE.equals(a)
                || ADAPTIVE_ACTION_RISK.equals(a)
                || ADAPTIVE_ACTION_FREEZE.equals(a)
                || ADAPTIVE_ACTION_FAKE.equals(a)
                || ADAPTIVE_ACTION_HOLD.equals(a);
    }

    public static boolean isRealBetProbeAllowedByConfig(
            int learnedIssues,
            double finalP,
            double edge,
            double coverQuality,
            double thirdQuality,
            double riskScore) {
        if (!isRealBetProbePeriodReady(learnedIssues)) {
            return false;
        }
        if (clampProbability(finalP) < REAL_BET_PROBE_MIN_FINAL_P) {
            return false;
        }
        if (Double.isNaN(edge) || Double.isInfinite(edge) || edge < REAL_BET_PROBE_MIN_EDGE) {
            return false;
        }
        if (!Double.isNaN(coverQuality)
                && !Double.isInfinite(coverQuality)
                && coverQuality < REAL_BET_PROBE_MIN_COVER_QUALITY) {
            return false;
        }
        if (!Double.isNaN(thirdQuality)
                && !Double.isInfinite(thirdQuality)
                && thirdQuality < REAL_BET_PROBE_MIN_THIRD_QUALITY) {
            return false;
        }
        if (!Double.isNaN(riskScore)
                && !Double.isInfinite(riskScore)
                && riskScore > REAL_BET_PROBE_MAX_RISK_SCORE) {
            return false;
        }
        return true;
    }

    public static boolean isDynamicClusterStatusRisk(String status) {
        return isDynamicClusterStatusHardRisk(status);
    }

    public static String buildDynamicClusterId(String prototypeKey) {
        return "DCW|" + safeKeyPart(prototypeKey);
    }

    public static String buildNeedSupplyMatchKey(
            String targetIssue, int position, int n1, int n2, int n3) {
        return "NSM|"
                + safeKeyPart(targetIssue)
                + "|P"
                + position
                + "|K"
                + normalizeK3Part(n1, n2, n3);
    }

    public static String buildDynamicClusterKey(
            String windowKey,
            int position,
            int n1,
            int n2,
            int n3,
            String needKey,
            String supplyKey,
            String roleKey) {
        return "DCW|P"
                + position
                + "|K"
                + normalizeK3Part(n1, n2, n3)
                + "|"
                + safeKeyPart(windowKey)
                + "|"
                + safeKeyPart(needKey)
                + "|"
                + safeKeyPart(supplyKey)
                + "|"
                + safeKeyPart(roleKey);
    }

    public static String buildNeedSupplyKey(
            int position, int number, String needType, String supplyType, String windowKey) {
        return "NS|P"
                + position
                + "|N"
                + number
                + "|"
                + safeKeyPart(needType)
                + "|"
                + safeKeyPart(supplyType)
                + "|"
                + safeKeyPart(windowKey);
    }

    public static String buildCounterfactualReasonKey(
            String targetIssue, int position, int n1, int n2, int n3, String clusterId) {
        return "CFR|"
                + safeKeyPart(targetIssue)
                + "|P"
                + position
                + "|K"
                + normalizeK3Part(n1, n2, n3)
                + "|"
                + safeKeyPart(clusterId);
    }

    public static String buildMissReasonKey(
            String targetIssue, int position, int n1, int n2, int n3, String missType) {
        return "MISS|"
                + safeKeyPart(targetIssue)
                + "|P"
                + position
                + "|K"
                + normalizeK3Part(n1, n2, n3)
                + "|"
                + safeKeyPart(missType);
    }

    public static String normalizeK3Part(int n1, int n2, int n3) {
        int a = n1;
        int b = n2;
        int c = n3;
        if (a > b) {
            int t = a;
            a = b;
            b = t;
        }
        if (b > c) {
            int t = b;
            b = c;
            c = t;
        }
        if (a > b) {
            int t = a;
            a = b;
            b = t;
        }
        return twoDigit(a) + "_" + twoDigit(b) + "_" + twoDigit(c);
    }

    public static String twoDigit(int n) {
        if (n < 0) {
            return String.valueOf(n);
        }
        if (n < 10) {
            return "0" + n;
        }
        return String.valueOf(n);
    }

    public static String safeKeyPart(String s) {
        if (s == null) {
            return "NA";
        }
        String v = s.trim();
        if (v.length() == 0) {
            return "NA";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < v.length(); i++) {
            char ch = v.charAt(i);
            if ((ch >= '0' && ch <= '9')
                    || (ch >= 'A' && ch <= 'Z')
                    || (ch >= 'a' && ch <= 'z')
                    || ch == '_'
                    || ch == '-'
                    || ch == '|') {
                sb.append(ch);
            } else {
                sb.append('_');
            }
            if (sb.length() >= 80) {
                break;
            }
        }
        if (sb.length() == 0) {
            return "NA";
        }
        return sb.toString();
    }

    /*
     * 链路修改升级 V2 · 第1/18 AppConfig
     *
     * 这里只定义分层调度、频率债务、合格边缘探针、快照新鲜度和兼容别名。
     * 不计算概率，不生成 BET，不创建 Trade，不修改结算，不绕过 DecisionGateEngine。
     */
    public static final String LINK_MODIFICATION_V2_VERSION =
            "V47_CROSSFIT_NUMBER_CHAMPION_FINAL40_FULL_DIAGNOSIS_20260721_47072147";
    public static final int LINK_MODIFICATION_V2_SCHEMA_VERSION = 11;
    public static final boolean ENABLE_LINK_MODIFICATION_V2 = true;
    public static final boolean ENABLE_QUALIFIED_FREQUENCY_FLOOR = false;
    public static final boolean ENABLE_FREQUENCY_DEBT = false;
    public static final boolean ENABLE_TIERED_PROFILE_SNAPSHOT = true;
    public static final boolean ENABLE_REAL_BET_PROBE_BUDGET = false;
    public static final boolean STRICT_FREQUENCY_NEVER_RAISE_FINAL_P = true;
    public static final boolean STRICT_FREQUENCY_NEVER_FORCE_BET = true;
    public static final boolean STRICT_SHADOW_PROMOTION_NEVER_DIRECT_BET = true;

    /* HOT / WARM / COLD / DEEP 分层 */
    public static final int HOT_REALTIME_WINDOW = WINDOW_360;
    public static final int WARM_PROFILE_WINDOW = WINDOW_1440;
    public static final int COLD_PROFILE_WINDOW = WINDOW_5000;
    public static final int DEEP_PROFILE_WINDOW = WINDOW_10000;
    public static final long WARM_REFRESH_MS = 10L * 60L * 1000L;
    public static final long COLD_REFRESH_MS = 60L * 60L * 1000L;
    public static final long DEEP_REFRESH_MS = 3L * 60L * 60L * 1000L;
    public static final long WARM_PROFILE_REFRESH_MS = WARM_REFRESH_MS;
    public static final long COLD_PROFILE_REFRESH_MS = COLD_REFRESH_MS;
    public static final long DEEP_PROFILE_REFRESH_MS = DEEP_REFRESH_MS;
    public static final long WARM_PROFILE_SOFT_STALE_MS = 30L * 60L * 1000L;
    public static final long WARM_PROFILE_HARD_STALE_MS = 60L * 60L * 1000L;
    public static final long COLD_PROFILE_SOFT_STALE_MS = 3L * 60L * 60L * 1000L;
    public static final long COLD_PROFILE_HARD_STALE_MS = 6L * 60L * 60L * 1000L;
    public static final long DEEP_PROFILE_SOFT_STALE_MS = 6L * 60L * 60L * 1000L;
    public static final long DEEP_PROFILE_HARD_STALE_MS = 12L * 60L * 60L * 1000L;
    public static final int WARM_MAX_ISSUE_LAG = 30;
    public static final int COLD_MAX_ISSUE_LAG = 180;
    public static final int DEEP_MAX_ISSUE_LAG = 360;
    public static final int WARM_PROFILE_CANDIDATE_LIMIT = 80;
    public static final int COLD_PROFILE_CANDIDATE_LIMIT = 120;
    public static final int DEEP_PROFILE_CANDIDATE_LIMIT = 160;
    public static final int REALTIME_PROBABILITY_ENRICH_LIMIT = 40;
    public static final int REALTIME_PROBABILITY_AUDIT_LIMIT = 120;
    public static final int REALTIME_PROBABILITY_URGENT_LIMIT = 18;

    /* 三窗口频率目标：15% 下限压力、18% 理想、20% 软高位 */
    public static final double TARGET_FREQ_IDEAL = 0.180d;
    public static final double TARGET_FREQ_SOFT_HIGH = 0.200d;
    public static final int FREQ_LOOKBACK_SHORT = 50;
    public static final int FREQ_LOOKBACK_ISSUES = 100;
    public static final int FREQ_LOOKBACK_MAIN = FREQ_LOOKBACK_ISSUES;
    public static final int FREQ_LOOKBACK_LONG = 300;
    public static final double FREQ_DEBT_TRIGGER = 0.150d;
    public static final double FREQ_DEBT_MAX = 0.120d;
    public static final double FREQ_DEBT_DECAY = 0.010d;
    public static final double FREQ_DEBT_RECOVERY_RATE = 0.020d;
    public static final int FREQ_MIN_VALID_OPPORTUNITIES = 1;
    public static final int FREQ_DEBT_MIN_VALID_ISSUES = 30;
    public static final double FREQ_RECOVERY_MAX_GATE_DISCOUNT = 0.020d;
    public static final double FREQ_RECOVERY_LIGHT_GATE_DISCOUNT = 0.005d;
    public static final double FREQ_RECOVERY_MID_GATE_DISCOUNT = 0.010d;
    public static final double FREQ_RECOVERY_HIGH_GATE_DISCOUNT = 0.015d;

    /* 合格边缘域探针：频率只影响探针门，不允许写回 FinalP */
    public static final double REAL_BET_PROBE_MIN_P = 0.350d;
    public static final double REAL_BET_PROBE_MAX_NEG_EDGE = -0.010d;
    public static final double REAL_BET_PROBE_MAX_GATE_GAP = 0.045d;
    public static final double REAL_BET_PROBE_MIN_COVERAGE = 0.330d;
    public static final double REAL_BET_PROBE_MIN_THIRD = 0.100d;
    public static final double REAL_BET_PROBE_MIN_NEED_SUPPLY = 0.480d;
    public static final double REAL_BET_PROBE_MAX_COUNTER_RISK = 0.580d;
    public static final double REAL_BET_PROBE_MAX_RECENT_LOSE_RISK = 0.620d;
    public static final int PROBE_COOLDOWN_ISSUES = 2;
    public static final int PROBE_MAX_PER_20_ISSUES = Integer.MAX_VALUE;
    public static final double PROBE_MAX_SHARE_OF_REAL_BETS = 0.400d;
    public static final int PROBE_RECENT_WINDOW = 20;

    /* 兼容旧字段名，避免 FIX23 / 后续引擎编译缺常量 */
    public static final double REAL_BET_PROBE_MIN_COVER = REAL_BET_PROBE_MIN_COVERAGE;
    public static final double REAL_BET_PROBE_MIN_COVERAGE_SCORE = REAL_BET_PROBE_MIN_COVERAGE;
    public static final double REAL_BET_PROBE_MIN_THIRD_SCORE = REAL_BET_PROBE_MIN_THIRD;
    public static final double REAL_BET_PROBE_MIN_NEED_SCORE = REAL_BET_PROBE_MIN_NEED_SUPPLY;
    public static final double REAL_BET_PROBE_MAX_COUNTER = REAL_BET_PROBE_MAX_COUNTER_RISK;
    public static final double REAL_BET_PROBE_MAX_NEGATIVE_EDGE = REAL_BET_PROBE_MAX_NEG_EDGE;

    /* SHADOW 域成熟状态；小样本只能观察，不能直接 READY */
    public static final int PROMOTE_WATCH_MIN_SAMPLE = 20;
    public static final int PROMOTE_READY_MIN_SAMPLE = 40;
    public static final int PROMOTE_STRONG_MIN_SAMPLE = 80;
    public static final int PROMOTE_BLOCK_MIN_SAMPLE = 30;
    public static final int PROMOTE_RECENT_MIN_SAMPLE = 12;
    public static final double PROMOTE_WATCH_MIN_POSTERIOR = 0.350d;
    public static final double PROMOTE_READY_MIN_POSTERIOR = 0.370d;
    public static final double PROMOTE_STRONG_MIN_POSTERIOR = 0.390d;
    public static final double PROMOTE_BLOCK_MAX_POSTERIOR = 0.285d;
    public static final double PROMOTE_READY_MIN_WILSON = 0.300d;
    public static final double PROMOTE_STRONG_MIN_WILSON = 0.320d;
    public static final String PROMOTION_NO_SAMPLE = "NO_SAMPLE";
    public static final String PROMOTION_WATCH = "WATCH";
    public static final String PROMOTION_WATCH_POSITIVE = "WATCH_POSITIVE";
    public static final String PROMOTION_PROMOTE_WATCH = "PROMOTE_WATCH";
    public static final String PROMOTION_READY = "READY";
    public static final String PROMOTION_STRONG = "STRONG";
    public static final String PROMOTION_BLOCK = "BLOCK";

    /* BET 域与频率状态 */
    public static final String FREQ_STATE_LOW_PRESSURE = "LOW_FREQ_PRESSURE";
    public static final String FREQ_STATE_NORMAL = "NORMAL";
    public static final String FREQ_STATE_SOFT_HIGH = "SOFT_HIGH";
    public static final String FREQ_STATE_FROZEN = "FROZEN";
    public static final String BET_DOMAIN_NORMAL = "BET_DOMAIN_NORMAL";
    public static final String BET_DOMAIN_FREQ_RECOVERY = "BET_DOMAIN_FREQ_RECOVERY";
    public static final String BET_DOMAIN_REAL_BET_PROBE = "BET_DOMAIN_REAL_BET_PROBE";
    public static final String DECISION_PATH_NORMAL = "NORMAL_GATE";
    public static final String DECISION_PATH_FREQ_RECOVERY = "QUALIFIED_FREQUENCY_FLOOR";
    public static final String DECISION_PATH_REAL_BET_PROBE = "QUALIFIED_REAL_BET_PROBE";

    /* 新增持久化表名 */
    public static final String TABLE_V20_FREQUENCY_STATE = "v20_frequency_state";
    public static final String TABLE_V20_LAYER_SNAPSHOT = "v20_layer_snapshot";
    public static final String TABLE_V20_BACKGROUND_CURSOR = "v20_background_cursor";
    public static final String TABLE_V20_SHADOW_DOMAIN_STAT = "v20_shadow_domain_stat";
    public static final String TABLE_V20_LINK_CANDIDATE_SNAPSHOT = "v20_link_candidate_snapshot";

    /* 后台降频：诊断和智能审判不得每期重扫 */
    public static final long REAL_BET_DIAG_MIN_REFRESH_MS = 60L * 1000L;
    public static final long FULL_DIAG_MIN_REFRESH_MS = 10L * 60L * 1000L;
    public static final long ADAPTIVE_WARM_MIN_REFRESH_MS = 10L * 60L * 1000L;
    public static final long ADAPTIVE_COLD_MIN_REFRESH_MS = 30L * 60L * 1000L;
    public static final int ADAPTIVE_REALTIME_INPUT_LIMIT = 40;
    public static final int ADAPTIVE_BACKGROUND_INPUT_LIMIT = 120;

    public static double clampFrequencyDebt(double debt) {
        if (Double.isNaN(debt) || Double.isInfinite(debt) || debt <= 0.0d) {
            return 0.0d;
        }
        if (debt > FREQ_DEBT_MAX) {
            return FREQ_DEBT_MAX;
        }
        return debt;
    }

    public static double buildFrequencyDebt(
            double currentDebt,
            double realBetFrequency,
            int validIssueCount,
            int qualifiedOpportunityCount,
            boolean frozen) {
        double debt = clampFrequencyDebt(currentDebt);
        if (frozen || validIssueCount < FREQ_DEBT_MIN_VALID_ISSUES) {
            return clampFrequencyDebt(debt - FREQ_DEBT_DECAY);
        }
        double f = clampProbability(realBetFrequency);
        if (f < FREQ_DEBT_TRIGGER) {
            double opportunityFactor = qualifiedOpportunityCount > 0 ? 1.0d : 0.35d;
            debt += (FREQ_DEBT_TRIGGER - f) * FREQ_DEBT_RECOVERY_RATE * 10.0d * opportunityFactor;
        } else {
            debt -= FREQ_DEBT_DECAY;
        }
        return clampFrequencyDebt(debt);
    }

    public static double buildFrequencyGateDiscount(
            double debt, String promotionStatus, boolean hardRisk, boolean snapshotHardStale) {
        if (!ENABLE_QUALIFIED_FREQUENCY_FLOOR || hardRisk || snapshotHardStale) {
            return 0.0d;
        }
        String p = normalizeToken(promotionStatus);
        if (!(PROMOTION_PROMOTE_WATCH.equals(p)
                || PROMOTION_READY.equals(p)
                || PROMOTION_STRONG.equals(p))) {
            return 0.0d;
        }
        double d = clampFrequencyDebt(debt);
        double out = FREQ_RECOVERY_LIGHT_GATE_DISCOUNT;
        if (d >= 0.080d) {
            out = FREQ_RECOVERY_HIGH_GATE_DISCOUNT;
        } else if (d >= 0.040d) {
            out = FREQ_RECOVERY_MID_GATE_DISCOUNT;
        }
        if (PROMOTION_READY.equals(p) && d >= 0.100d) {
            out = FREQ_RECOVERY_MAX_GATE_DISCOUNT;
        }
        if (PROMOTION_STRONG.equals(p)) {
            out = Math.min(FREQ_RECOVERY_MAX_GATE_DISCOUNT, out + 0.005d);
        }
        return Math.min(FREQ_RECOVERY_MAX_GATE_DISCOUNT, Math.max(0.0d, out));
    }

    public static boolean isQualifiedFrequencyProbe(
            double finalP,
            double riskAdjustedGate,
            double rawEdge,
            double coverage,
            double third,
            double needSupply,
            double counterRisk,
            String adaptiveStatus,
            String promotionStatus,
            String ruleHealthStatus,
            boolean recentLoseRiskHigh,
            boolean snapshotHardStale) {
        if (!ENABLE_QUALIFIED_FREQUENCY_FLOOR || snapshotHardStale || recentLoseRiskHigh)
            return false;
        if (clampProbability(finalP) < REAL_BET_PROBE_MIN_P) return false;
        double qualityEdge = qualifiedFloorQualityEdge(finalP);
        if (qualityEdge + 1.0e-12d < REAL_BET_PROBE_MAX_NEG_EDGE) return false;
        /* NORMAL动态门槛只约束NORMAL_BET；合格补频使用独立36%质量参考线，避免35%被38.5%门槛隐式抬成37.5%。 */
        if (coverage < REAL_BET_PROBE_MIN_COVERAGE) return false;
        if (third < REAL_BET_PROBE_MIN_THIRD) return false;
        if (needSupply < REAL_BET_PROBE_MIN_NEED_SUPPLY) return false;
        if (counterRisk > REAL_BET_PROBE_MAX_COUNTER_RISK) return false;
        String adaptive = normalizeToken(adaptiveStatus);
        if (adaptive.indexOf("FREEZE") >= 0 || adaptive.indexOf("REJECT") >= 0) return false;
        /* Promotion成熟度不能作为上游循环前置条件；这里只做结构质量预检。
         * 已结算域证据或冷启动资格由DecisionGateEngine独立裁决。 */
        String rule = normalizeToken(ruleHealthStatus);
        return rule.indexOf("HARD_RISK") < 0 && rule.indexOf("FREEZE") < 0;
    }

    /*
     * 实时裁决提速与有效域修复 · 配置层
     *
     * 不删减1200候选覆盖，不改变单位置固定K3，不修改赔率，不生成BET。
     * 频率债务仍只作用于合格探针门，禁止写回FinalP。
     */
    public static final String REALTIME_VALID_DOMAIN_FIX_VERSION =
            "V47_CROSSFIT_NUMBER_CHAMPION_FINAL40_FULL_DIAGNOSIS_20260721_47072147";
    public static final boolean ENABLE_REALTIME_VALID_DOMAIN_FIX = true;

    /* 正式Decision性能目标：中位数8秒，P95硬上限12秒。 */
    public static final long REALTIME_DECISION_MEDIAN_TARGET_MS = 3500L;
    public static final long REALTIME_DECISION_P95_HARD_LIMIT_MS = 12000L;
    public static final long REALTIME_PRE_GATE_TARGET_MS = 8500L;
    public static final long REALTIME_SCAN_TARGET_MS = 1500L;
    public static final long REALTIME_PROBABILITY_BASE_BUDGET_MS = 1800L;
    public static final long REALTIME_PROBABILITY_DEEP_BUDGET_MS = 3200L;
    public static final long REALTIME_PROBABILITY_HARD_BUDGET_MS = 5200L;
    public static final long REALTIME_DYNAMIC_GATE_TARGET_MS = 900L;
    public static final long REALTIME_DECISION_GATE_TARGET_MS = 1500L;
    public static final int REALTIME_PROBABILITY_MIN_FULL_COUNT = 18;
    public static final int REALTIME_PROBABILITY_MAX_FULL_COUNT = 40;
    public static final boolean REALTIME_PROBABILITY_STOP_HEAVY_ON_BUDGET = true;
    public static final boolean REALTIME_PROBABILITY_USE_SHALLOW_AFTER_BUDGET = true;
    public static final boolean REALTIME_DYNAMIC_GATE_USE_PRECOMPUTED_EVIDENCE = true;

    /* 同一期Teacher只允许一次正式桥接，后续阶段只做字段别名同步。 */
    public static final boolean REALTIME_TEACHER_SINGLE_BRIDGE = true;
    public static final String REALTIME_TEACHER_PRIMARY_STAGE = "BEFORE_PROBABILITY";
    public static final boolean REALTIME_TEACHER_REUSE_NO_HISTORY_RESCAN = true;

    /* Decision前禁止重型诊断、长文本、全量SHADOW重扫和重复候选快照写库。 */
    public static final boolean REALTIME_DEFER_LONG_REASON_TEXT = true;
    public static final boolean REALTIME_DEFER_FULL_DIAGNOSIS = true;
    public static final boolean REALTIME_DEFER_MODULE_SCORE_REPORT = true;
    public static final boolean REALTIME_DEFER_FIX_PLAN_REPORT = true;
    public static final boolean REALTIME_SAVE_CANDIDATE_SNAPSHOT_ONCE = true;
    public static final boolean REALTIME_ADAPTIVE_EMPTY_FAST_RETURN = true;
    public static final long REALTIME_ADAPTIVE_EMPTY_MAX_MS = 20L;

    /* WARM/COLD/DEEP后台最低执行保障；实时让路，但不能永久饿死。 */
    public static final long SNAPSHOT_WORKER_RETRY_MS = 2L * 60L * 1000L;
    public static final long SNAPSHOT_WORKER_MAX_STARVATION_MS = 30L * 60L * 1000L;
    public static final String SNAPSHOT_KEY_TEACHER_WARM = "TEACHER_WARM";
    public static final String SNAPSHOT_KEY_TEACHER_COLD = "TEACHER_COLD";
    public static final String SNAPSHOT_KEY_TEACHER_DEEP = "TEACHER_DEEP";

    /* 新SHADOW字段完整性验收。 */
    public static final double SHADOW_FIELD_COMPLETENESS_TARGET = 0.950d;
    public static final boolean STRICT_NON_BET_REJECT_CODE_REQUIRED = true;
    public static final String TRADE_LINK_SKIP_NOT_REAL_BET = "SKIP_NOT_REAL_BET";

    /*
     * Promotion正式域只能使用开奖前已知字段。
     * HIT/MISS/actual/wouldHit/RANKx_HIT/K3_MISS属于开奖后标签，禁止作为晋级域。
     */
    public static boolean isUnsafePromotionDomainKey(String key) {
        String s = normalizeToken(key);
        if (s.length() == 0) return false;
        return s.indexOf("RANK1_HIT") >= 0
                || s.indexOf("RANK2_HIT") >= 0
                || s.indexOf("RANK3_HIT") >= 0
                || s.indexOf("K3_MISS") >= 0
                || s.indexOf("WOULD_HIT") >= 0
                || s.indexOf("WOULD_MISS") >= 0
                || s.indexOf("ACTUAL_NUMBER") >= 0
                || s.indexOf(":HIT") >= 0
                || s.indexOf(":MISS") >= 0
                || s.indexOf("K3_LAYER") >= 0;
    }

    public static String sanitizePromotionDomainKey(String key) {
        if (key == null) return "";
        String s = key.trim();
        return isUnsafePromotionDomainKey(s) ? "" : s;
    }

    /*
     * V20 后台常驻、证据成熟与概率中性恢复专项。
     * 仅定义调度/缓存/审计参数，不生成BET，不改变固定单位置K3。
     */
    public static final String BACKGROUND_EVIDENCE_PROBABILITY_RECOVERY_VERSION =
            "V47_CROSSFIT_NUMBER_CHAMPION_FINAL40_FULL_DIAGNOSIS_20260721_47072147";

    /* 前台服务心跳与进程内恢复。Manifest/BootReceiver仍需项目原文件配合。 */
    public static final long SERVICE_HEARTBEAT_INTERVAL_MS = 30000L;
    public static final long SERVICE_HEARTBEAT_STALE_MS = 90000L;
    public static final long SERVICE_RESTART_DELAY_MS = 5000L;
    public static final String STATE_SERVICE_USER_ENABLED = "service_user_enabled";
    public static final String STATE_SERVICE_HEARTBEAT_AT = "service_heartbeat_at";
    public static final String STATE_SERVICE_RESTART_COUNT = "service_restart_count";

    /* 后台必须让路实时，但不能永久饥饿。 */
    public static final long BACKGROUND_REALTIME_GUARD_MS = 12000L;
    public static final long BACKGROUND_SLICE_BUDGET_MS = 2200L;
    public static final long BACKGROUND_MIN_SLICE_INTERVAL_MS = 12000L;
    public static final long BACKGROUND_MAX_STARVATION_MS = 120000L;
    public static final int BACKGROUND_WARM_SLICE_ISSUES = 360;
    public static final int BACKGROUND_COLD_SLICE_ISSUES = 500;
    public static final int BACKGROUND_DEEP_SLICE_ISSUES = 700;
    public static final int BACKGROUND_WARM_REQUIRED_ISSUES = 1440;
    public static final int BACKGROUND_COLD_REQUIRED_ISSUES = 5000;
    public static final int BACKGROUND_DEEP_REQUIRED_ISSUES = 10000;

    /* Probability：完整策略链保留，只消除重复查询与“无样本也扣分”。 */
    public static final long PROBABILITY_CONTEXT_CACHE_TTL_MS = 60000L;
    public static final int PROBABILITY_CONTEXT_MAX_ENTRIES = 600;
    public static final double PROBABILITY_NEUTRAL_WATCH_DISCOUNT = 0.000d;
    public static final double PROBABILITY_NEUTRAL_MAX_BELOW_RAW = 0.004d;
    public static final double PROBABILITY_VALIDATED_EVIDENCE_BOOST_MAX = 0.012d;
    public static final double PROBABILITY_EXPLICIT_RISK_DISCOUNT_MAX = 0.024d;
    public static final boolean PROBABILITY_NEGATIVE_EDGE_IS_NOT_EVIDENCE = true;

    /* PreDraw字段缺失只能按中性降级，不能按硬风险默认值处理。 */
    public static final boolean PREDRAW_NEUTRAL_FALLBACK_ENABLED = true;
    public static final double PREDRAW_NEUTRAL_SCORE = 0.500d;
    public static final double PREDRAW_FIELD_COMPLETENESS_TARGET = 0.950d;

    /* Gate：NO_SAMPLE/WATCH本身不抬门，明确RISK/FREEZE才允许收紧。 */
    public static final double NO_SAMPLE_GATE_ADJUST = 0.000d;
    public static final double WATCH_NEUTRAL_GATE_ADJUST = 0.000d;

    public static boolean isNeutralEvidenceStatus(String value) {
        String s = normalizeToken(value);
        return s.length() == 0
                || s.indexOf("NO_SAMPLE") >= 0
                || s.indexOf("WATCH") >= 0
                || s.indexOf("NEUTRAL") >= 0
                || s.indexOf("PENDING") >= 0
                || s.indexOf("UNKNOWN") >= 0;
    }

    /* V20 20260712 最终后台证据泵 + 概率分层 + PreDraw直连。 */
    public static final String V20_FINAL_BACKGROUND_PROBABILITY_FIX_VERSION =
            "V47_CROSSFIT_NUMBER_CHAMPION_FINAL40_FULL_DIAGNOSIS_20260721_47072147";
    public static final String ACTION_SERVICE_WATCHDOG = PACKAGE_NAME + ".ACTION_SERVICE_WATCHDOG";
    public static final String ACTION_SERVICE_TASK_REMOVED =
            PACKAGE_NAME + ".ACTION_SERVICE_TASK_REMOVED";
    public static final long SERVICE_WATCHDOG_INTERVAL_MS = 120000L;
    public static final long SERVICE_TASK_REMOVED_RESTART_MS = 2500L;
    public static final long BACKGROUND_EVIDENCE_PUMP_INTERVAL_MS = 5000L;
    public static final long BACKGROUND_EVIDENCE_RETRY_MS = 8000L;
    public static final long BACKGROUND_EVIDENCE_SLICE_BUDGET_MS = 700L;
    public static final long BACKGROUND_EVIDENCE_FORCE_AFTER_MS = 90000L;
    public static final int PROBABILITY_REALTIME_DEEP_CANDIDATES = 18;
    public static final int PROBABILITY_REALTIME_FULL_CANDIDATES = 40;
    public static final double PROBABILITY_NEUTRAL_ANCHOR_MAX_BELOW_PRIOR = 0.002d;

    /*
     * 新数据库冷启动：Teacher/Dynamic尚无已结算样本时，它们必须真正中性。
     * 仅恢复被“无样本风险层”压低的部分；结构硬风险、覆盖不足、第三号不足仍不恢复。
     */
    public static final boolean COLD_START_NEUTRAL_RESTORE_ENABLE = true;
    public static final int COLD_START_NEUTRAL_MIN_HISTORY = 30;
    public static final double COLD_START_NEUTRAL_MIN_PRE_RISK_P = 0.350d;
    public static final double COLD_START_NEUTRAL_MIN_AFTER_TEACHER_P = 0.345d;
    public static final double COLD_START_NEUTRAL_RESTORE_FINAL_P = 0.350d;
    public static final double COLD_START_NEUTRAL_RESTORE_CAP = 0.365d;
    public static final double COLD_START_NEUTRAL_MAX_DYNAMIC_RISK = 0.620d;
    public static final double COLD_START_NEUTRAL_MAX_COUNTER_RISK = 0.580d;
    public static final double PROBABILITY_PREDRAW_PRIOR_WEIGHT = 0.35d;
    public static final double PROBABILITY_PREDRAW_EVIDENCE_SPREAD = 0.045d;
    public static final boolean PREDRAW_STRICT_DIRECT_FIELD_MODE = true;
    public static final int PREDRAW_EXPECTED_FEATURE_COUNT = 100;
    public static final int FULL_SCAN_EXPECTED_CANDIDATES_V2 = 1200;

    /*
     * V20 真后台常驻与证据成熟最终闭环（14文件）。
     * 只定义调度、切片、缓存、版本与审计参数；不生成BET，不改变固定单位置K3。
     */
    public static final String FINAL_WATCHDOG_EVIDENCE_VERSION =
            "V47_CROSSFIT_NUMBER_CHAMPION_FINAL40_FULL_DIAGNOSIS_20260721_47072147";
    public static final String FIX_COHORT = "NOVA_V46_NUMBER_DISTRIBUTION_COHORT_20260721_46072130";
    public static final String PROBABILITY_PIPELINE_VERSION =
            "V47_CROSSFIT_NUMBER_CHAMPION_FINAL40_FULL_DIAGNOSIS_20260721_47072147";
    public static final String DYNAMIC_LEARNING_CLUSTER_VERSION =
            "V47_CROSSFIT_NUMBER_CHAMPION_FINAL40_FULL_DIAGNOSIS_20260721_47072147";

    /* 前台服务是分钟主链；Alarm与JobScheduler只做进程死亡恢复保险。 */
    public static final int SERVICE_WATCHDOG_JOB_ID = 60518;
    public static final long SERVICE_JOB_MIN_LATENCY_MS = 45L * 1000L;
    public static final long SERVICE_JOB_DEADLINE_MS = 90L * 1000L;
    public static final long SERVICE_WATCHDOG_ALARM_DELAY_MS = 45L * 1000L;
    public static final long SERVICE_WATCHDOG_WINDOW_MS = 60L * 1000L;
    public static final long SERVICE_HEARTBEAT_WRITE_INTERVAL_MS = 30L * 1000L;
    public static final long SERVICE_HEARTBEAT_EXPIRED_MS = 70L * 1000L;
    public static final long SERVICE_RECOVERY_DEBOUNCE_MS = 20L * 1000L;
    public static final String STATE_WATCHDOG_ALARM_STATUS = "service_watchdog_alarm_status";
    public static final String STATE_WATCHDOG_JOB_STATUS = "service_watchdog_job_status";
    public static final String STATE_WATCHDOG_LAST_REASON = "service_watchdog_last_reason";
    public static final String STATE_WATCHDOG_LAST_TRIGGER_AT = "service_watchdog_last_trigger_at";
    public static final String STATE_SERVICE_LAST_DESTROY_REASON = "service_last_destroy_reason";
    public static final String STATE_SERVICE_LAST_TASK_REMOVED_AT = "service_last_task_removed_at";

    /* Teacher三层真实微批次：BUILDING与FRESH严格分离。 */
    public static final int TEACHER_WARM_SLICE_DRAWS = 120;
    public static final int TEACHER_COLD_SLICE_DRAWS = 240;
    public static final int TEACHER_DEEP_SLICE_DRAWS = 240;
    public static final int TEACHER_WARM_REQUIRED_DRAWS = 1440;
    public static final int TEACHER_COLD_REQUIRED_DRAWS = 5000;
    public static final int TEACHER_DEEP_REQUIRED_DRAWS = 10000;
    public static final long TEACHER_BACKGROUND_SLICE_BUDGET_MS = 700L;
    public static final long TEACHER_BACKGROUND_FINALIZE_BUDGET_MS = 900L;
    public static final String TEACHER_WARM_BUILDING_KEY = "TEACHER_WARM_BUILDING";

    /* Probability每期上下文与缓存。所有模块保留，只禁止同一期重复历史扫描/SQLite读取。 */
    public static final int PROBABILITY_BATCH_DEEP_LIMIT = 10;
    public static final int PROBABILITY_BATCH_FULL_LIMIT = 40;
    public static final long PROBABILITY_BATCH_SOFT_BUDGET_MS = 3500L;
    public static final long PROBABILITY_BATCH_HARD_BUDGET_MS = 5000L;
    public static final int PROBABILITY_POSITION_CACHE_MAX = 10;
    public static final int PROBABILITY_NUMBER_CACHE_MAX = 100;
    public static final int PROBABILITY_STATE_CACHE_MAX = 320;
    public static final int PROBABILITY_COMBO_CACHE_MAX = 120;

    /* Coverage与Reject字段明确拆分，禁止兼容别名互相覆盖。 */
    public static final double COVERAGE_REQUIRED_DEFAULT = 0.300d;
    public static final String REJECT_NONE = "";

    /* 动态因果：审计Key保留细节；学习Key使用粗粒度，避免每期都是新簇。 */
    public static final int DYNAMIC_LEARNING_EXACT_MIN_SAMPLE = 18;
    public static final int DYNAMIC_LEARNING_PARENT_MIN_SAMPLE = 30;
    public static final int DYNAMIC_LEARNING_COARSE_MIN_SAMPLE = 50;
    public static final double DYNAMIC_LEARNING_READY_LOWER_BOUND = 0.315d;

    public static boolean isServiceHeartbeatExpired(long heartbeatAt, long now) {
        if (heartbeatAt <= 0L) return true;
        long age = now - heartbeatAt;
        return age < 0L || age > SERVICE_HEARTBEAT_EXPIRED_MS;
    }

    /*
     * V20 稳定性核心重构：数据库单例协调、轻量心跳、后台硬时间片、诊断降频。
     * 这些参数只约束资源与调度，不修改固定单位置K3、概率公式主干或最终BET门。
     */
    public static final String STABILITY_CORE_VERSION =
            "V47_CROSSFIT_NUMBER_CHAMPION_FINAL40_FULL_DIAGNOSIS_20260721_47072147";
    public static final String STABILITY_FIX_COHORT =
            "NOVA_V46_NUMBER_DISTRIBUTION_COHORT_20260721_46072130";
    public static final int DB_BUSY_TIMEOUT_MS = 50;
    public static final int DB_LOCK_RETRY_COUNT = 3;
    public static final long DB_LOCK_RETRY_BASE_MS = 40L;
    public static final long BACKGROUND_SLICE_MAX_MS = 800L;
    public static final long BACKGROUND_SLICE_MIN_GAP_MS = 2500L;
    public static final long DIAGNOSIS_INTERVAL_MS = 12L * 60L * 1000L;
    public static final long DIAGNOSIS_MIN_REALTIME_GAP_MS = 45L * 1000L;
    public static final long SERVICE_RECOVERY_CHECK_INTERVAL_MS = 90L * 1000L;
    public static final long PROBABILITY_CONTEXT_MAX_AGE_MS = 70L * 1000L;
    public static final long REALTIME_TOTAL_HARD_MS = 5000L;
    public static final int PROBABILITY_MIN_DEEP_CANDIDATES = 6;
    public static final int PROBABILITY_MAX_DEEP_CANDIDATES = 10;
    public static final int TEACHER_RECENT_ACCUMULATOR_DRAWS = 240;
    public static final String STATE_STABILITY_CORE_VERSION = "stability_core_version";
    public static final String STATE_DATABASE_QUEUE = "database_queue_state";
    public static final String STATE_CRASH_SNAPSHOT = "crash_snapshot";

    /*
     * 欧曼银河 V20 全闭环二次升级：后台业务活性、Teacher期号新鲜度、
     * 中性不冻结、分级REAL_PROBE、字段落库与诊断统一配置。
     * 仅定义常量与状态键；不生成BET，不创建Trade，不改变固定单位置K3结算。
     */
    public static final String FULL_CLOSED_LOOP_18_VERSION =
            "V47_CROSSFIT_NUMBER_CHAMPION_FINAL40_FULL_DIAGNOSIS_20260721_47072147";
    public static final String FINAL_INTEGRATION_FIX_VERSION = FULL_CLOSED_LOOP_18_VERSION;
    public static final String ACTIVE_FIX_COHORT =
            "NOVA_V46_NUMBER_DISTRIBUTION_COHORT_20260721_46072130";
    public static final String ACTIVE_PROBABILITY_PIPELINE_VERSION =
            "V47_CROSSFIT_NUMBER_CHAMPION_FINAL40_FULL_DIAGNOSIS_20260721_47072147";
    public static final String ACTIVE_DYNAMIC_DOMAIN_VERSION =
            "V47_CROSSFIT_NUMBER_CHAMPION_FINAL40_FULL_DIAGNOSIS_20260721_47072147";
    public static final String ACTIVE_TEACHER_VERSION =
            "V47_CROSSFIT_NUMBER_CHAMPION_FINAL40_FULL_DIAGNOSIS_20260721_47072147";

    /* 端到端业务活性：进程心跳不能代替Fetch/Analysis成功。 */
    public static final long SERVICE_RUNTIME_STALE_MS = 70L * 1000L;
    public static final long SERVICE_BUSINESS_START_GRACE_MS = 45L * 1000L;
    public static final long SERVICE_FETCH_STALE_MS = 75L * 1000L;
    public static final long SERVICE_ANALYSIS_STALE_MS = 90L * 1000L;
    public static final long SERVICE_FAST_RECOVERY_ALARM_MS = 5L * 1000L;
    public static final long SERVICE_HEARTBEAT_ATOMIC_INTERVAL_MS = 30L * 1000L;
    public static final long POST_DECISION_DIAG_DELAY_MS = 5L * 1000L;
    public static final String STATE_LAST_SERVICE_STARTED_AT = "last_service_started_at";
    public static final String STATE_LAST_FETCH_SUCCESS_AT = "last_fetch_success_at";
    public static final String STATE_LAST_ANALYSIS_SUCCESS_AT = "last_analysis_success_at";
    public static final String STATE_LAST_SETTLEMENT_SUCCESS_AT = "last_settlement_success_at";
    public static final String STATE_LAST_ALARM_TRIGGERED_AT = "last_alarm_triggered_at";
    public static final String STATE_LAST_JOB_TRIGGERED_AT = "last_job_triggered_at";
    public static final String STATE_BACKGROUND_BUSINESS_HEALTH = "background_business_health";
    public static final String STATE_EXACT_ALARM_CAPABILITY = "exact_alarm_capability";
    public static final String STATE_ACTIVE_FIX_COHORT = "active_fix_cohort";
    public static final String STATE_ACTIVE_PROBABILITY_PIPELINE = "active_probability_pipeline";
    public static final String STATE_ADAPTIVE_INPUT_COUNT = "adaptive_input_count";
    public static final String STATE_DECISION_UNATTRIBUTED_MS = "decision_unattributed_ms";
    public static final String STATE_FULL_CLOSED_LOOP_VERSION = "full_closed_loop_version";
    public static final String STATE_PROMOTION_MATCH_COUNT = "promotion_match_count";
    public static final String STATE_TEACHER_RESERVED_SLICE = "teacher_reserved_slice";
    public static final String STATE_TYPED_TEACHER_STATUS = "typed_teacher_status";

    /* Teacher快照必须同时通过时间和issue游标；陈旧快照只解释、不扣分。 */
    public static final int TEACHER_SNAPSHOT_MAX_ISSUE_LAG = 180;
    public static final int TEACHER_WARM_MICRO_SLICE_DRAWS = 60;
    public static final int TEACHER_WARM_MIN_PUBLISH_DRAWS = 240;
    public static final long TEACHER_WARM_RESERVED_AFTER_DECISION_MS = 650L;
    public static final long TEACHER_WARM_RESERVED_POST_DELAY_MS = 4L * 1000L;
    public static final long TEACHER_WARM_RESERVED_RETRY_DELAY_MS = 2L * 1000L;
    public static final int TEACHER_EXACT_MIN_SAMPLE = 18;
    public static final int TEACHER_PARENT_MIN_SAMPLE = 30;
    public static final int TEACHER_COARSE_MIN_SAMPLE = 50;
    public static final double TEACHER_READY_POSTERIOR = 0.340d;
    public static final double TEACHER_READY_LOWER_BOUND = 0.280d;
    public static final double TEACHER_STRONG_POSTERIOR = 0.380d;
    public static final double TEACHER_STRONG_LOWER_BOUND = 0.310d;

    /* 受控探针配额：只用于打破零真实样本死锁，仍由DecisionGate唯一裁决。 */
    public static final double PROBE_BOOTSTRAP_MIN_P = 0.345d;
    public static final double PROBE_BOOTSTRAP_MAX_GATE_GAP = 0.045d;
    public static final int PROBE_BOOTSTRAP_MAX_100 = Integer.MAX_VALUE;
    public static final int PROBE_WATCH_MAX_100 = Integer.MAX_VALUE;
    public static final int PROBE_READY_MAX_100 = Integer.MAX_VALUE;
    public static final int PROBE_MATURE_MAX_100 = Integer.MAX_VALUE;
    public static final int PROBE_WATCH_MIN_SETTLED_SAMPLE = 18;
    public static final double PROBE_WATCH_MIN_SETTLED_RATE = 0.340d;
    public static final int PROBE_READY_MIN_SETTLED_SAMPLE = 30;
    public static final double PROBE_READY_MIN_SETTLED_RATE = 0.365d;
    public static final int PROBE_STRONG_MIN_SETTLED_SAMPLE = 50;
    public static final double PROBE_STRONG_MIN_SETTLED_RATE = 0.390d;
    public static final String ACTIVE_PROMOTION_VERSION =
            "V47_CROSSFIT_NUMBER_CHAMPION_FINAL40_FULL_DIAGNOSIS_20260721_47072147";

    /* 欧曼银河第三次全源码重构：真实证据、结构化风险、独立探针、端到端后台。 */
    public static final String OUMAN_GALAXY_REBUILD_VERSION =
            "V47_CROSSFIT_NUMBER_CHAMPION_FINAL40_FULL_DIAGNOSIS_20260721_47072147";
    public static final String OUMAN_GALAXY_REBUILD_COHORT =
            "NOVA_V46_NUMBER_DISTRIBUTION_COHORT_20260721_46072130";
    public static final String EVIDENCE_SEVERITY_NEUTRAL = "NEUTRAL";
    public static final String EVIDENCE_SEVERITY_WATCH = "WATCH";
    public static final String EVIDENCE_SEVERITY_SOFT_RISK = "SOFT_RISK";
    public static final String EVIDENCE_SEVERITY_HARD_BLOCK = "HARD_BLOCK";
    public static final int PROBE_BOOTSTRAP_TOTAL_SHADOW_MIN = 120;
    public static final int PROBE_BOOTSTRAP_COOLDOWN_ISSUES = 4;
    public static final long SERVICE_SETTLEMENT_STALE_MS = 90L * 1000L;
    public static final String BUSINESS_FETCH_RUNNING = "FETCH_RUNNING";
    public static final String BUSINESS_FETCH_SUCCEEDED = "FETCH_SUCCEEDED";
    public static final String BUSINESS_FETCH_FAILED = "FETCH_FAILED";
    public static final String BUSINESS_ANALYSIS_RUNNING = "ANALYSIS_RUNNING";
    public static final String BUSINESS_ANALYSIS_SUCCEEDED = "ANALYSIS_SUCCEEDED";
    public static final String BUSINESS_ANALYSIS_FAILED = "ANALYSIS_FAILED";
    public static final String BUSINESS_SETTLEMENT_RUNNING = "SETTLEMENT_RUNNING";
    public static final String BUSINESS_SETTLEMENT_SUCCEEDED = "SETTLEMENT_SUCCEEDED";
    public static final String BUSINESS_SETTLEMENT_IDLE = "SETTLEMENT_IDLE";
    public static final String BUSINESS_SETTLEMENT_FAILED = "SETTLEMENT_FAILED";

    public static String normalizeEvidenceSeverity(String value) {
        String s = normalizeToken(value);
        if (EVIDENCE_SEVERITY_HARD_BLOCK.equals(s)) return EVIDENCE_SEVERITY_HARD_BLOCK;
        if (EVIDENCE_SEVERITY_SOFT_RISK.equals(s)) return EVIDENCE_SEVERITY_SOFT_RISK;
        if (EVIDENCE_SEVERITY_WATCH.equals(s)) return EVIDENCE_SEVERITY_WATCH;
        return EVIDENCE_SEVERITY_NEUTRAL;
    }

    public static boolean isExplicitHardBlockStatus(String value) {
        String s = normalizeToken(value);
        if (s.length() == 0 || isNeutralEvidenceStatus(s)) return false;
        return EVIDENCE_SEVERITY_HARD_BLOCK.equals(s)
                || "HARD_RISK".equals(s)
                || "HARD_FREEZE".equals(s)
                || "FREEZE_ACTIVE".equals(s)
                || s.indexOf("HARD_BLOCK") >= 0
                || s.indexOf("COUNTER_EVIDENCE_HARD") >= 0
                || s.indexOf("FALSE_SEED_HARD") >= 0
                || s.indexOf("POSITION_NOISE_HARD") >= 0
                || s.indexOf("DATA_CORRUPT") >= 0
                || s.indexOf("FUTURE_LEAK") >= 0
                || s.indexOf("硬风险") >= 0
                || s.indexOf("硬冻结") >= 0;
    }

    public static boolean isPreDecisionOpportunity(
            double finalP,
            double coverage,
            double third,
            double needSupply,
            double counterRisk,
            int hardBlockFlag) {
        /*
         * 73487246：机会统计必须发生在Decision之前，不能再要求候选先达到旧Probe门槛。
         * 只要数据是有限数、没有结构化HARD_BLOCK，就算作本期可排序机会。
         * 质量字段继续参与排序和NORMAL_BET，但不得制造“0机会→0债务→0出手”的循环。
         */
        return hardBlockFlag == 0
                && !Double.isNaN(finalP)
                && !Double.isInfinite(finalP)
                && !Double.isNaN(coverage)
                && !Double.isInfinite(coverage)
                && !Double.isNaN(third)
                && !Double.isInfinite(third)
                && !Double.isNaN(needSupply)
                && !Double.isInfinite(needSupply)
                && !Double.isNaN(counterRisk)
                && !Double.isInfinite(counterRisk);
    }

    /*
     * 73487246：真实频率下限控制器。
     * HARD_MIN=15%，TARGET=20%，不设置上限；NORMAL_BET永远可以在20%以上继续出手。
     * 下限通道只负责“必须从合法候选中选最优一单”，不改变固定单位置K3和唯一DecisionGate出口。
     */
    public static final String FREQUENCY_FLOOR_VERSION =
            "V47_CROSSFIT_NUMBER_CHAMPION_FINAL40_FULL_DIAGNOSIS_20260721_47072147";
    public static final double REAL_BET_HARD_MIN_FREQUENCY = 0.150d;
    public static final double REAL_BET_TARGET_FREQUENCY = 0.200d;
    public static final boolean REAL_BET_FREQUENCY_HAS_UPPER_CAP = false;
    public static final int REAL_BET_FREQUENCY_LOOKBACK = 100;
    public static final String BET_DOMAIN_FREQUENCY_FLOOR = "FREQUENCY_FLOOR_BET";
    public static final String DECISION_PATH_FREQUENCY_FLOOR = "FREQUENCY_FLOOR_BET";
    public static final String FREQ_STATE_HARD_FLOOR = "HARD_FLOOR_DEBT";
    public static final String FREQ_STATE_TARGET_RECOVERY = "TARGET_RECOVERY";

    public static int buildRequiredRealBetCount(int completedDecisionCount) {
        int projected =
                Math.max(1, Math.min(REAL_BET_FREQUENCY_LOOKBACK, completedDecisionCount + 1));
        return (int) Math.ceil(((double) projected) * REAL_BET_HARD_MIN_FREQUENCY - 1.0e-12d);
    }

    public static int buildTargetRealBetCount(int completedDecisionCount) {
        int projected =
                Math.max(1, Math.min(REAL_BET_FREQUENCY_LOOKBACK, completedDecisionCount + 1));
        return (int) Math.ceil(((double) projected) * REAL_BET_TARGET_FREQUENCY - 1.0e-12d);
    }

    public static int buildFrequencyFloorDebtCount(int completedDecisionCount, int realBetCount) {
        return Math.max(
                0, buildRequiredRealBetCount(completedDecisionCount) - Math.max(0, realBetCount));
    }

    public static int buildFrequencyTargetDebtCount(int completedDecisionCount, int realBetCount) {
        return Math.max(
                0, buildTargetRealBetCount(completedDecisionCount) - Math.max(0, realBetCount));
    }

    public static boolean isMandatoryFrequencyFloor(int completedDecisionCount, int realBetCount) {
        return buildFrequencyFloorDebtCount(completedDecisionCount, realBetCount) > 0;
    }

    public static boolean isTargetFrequencyRecovery(int completedDecisionCount, int realBetCount) {
        return !isMandatoryFrequencyFloor(completedDecisionCount, realBetCount)
                && buildFrequencyTargetDebtCount(completedDecisionCount, realBetCount) > 0;
    }

    public static boolean isNonOverridableFloorBlock(String value) {
        String s = normalizeToken(value);
        if (s.length() == 0) return false;
        return isExplicitHardBlockStatus(s)
                || s.indexOf("DATA_ERROR") >= 0
                || s.indexOf("DATA_CORRUPT") >= 0
                || s.indexOf("FUTURE_LEAK") >= 0
                || s.indexOf("INVALID_K3") >= 0
                || s.indexOf("DUPLICATE_REAL_BET") >= 0
                || s.indexOf("OPEN_TRADE") >= 0
                || s.indexOf("TARGET_ISSUE_ERROR") >= 0;
    }

    /* V47_CROSSFIT_NUMBER_CHAMPION_FINAL40_FULL_DIAGNOSIS_20260721_47072147: 后台常驻、Teacher/因果字段穿透、频率下限保存闭环。 */
    public static final String ALWAYS_ON_RUNTIME_FIX_VERSION =
            "V47_CROSSFIT_NUMBER_CHAMPION_FINAL40_FULL_DIAGNOSIS_20260721_47072147";
    public static final String ALWAYS_ON_RUNTIME_COHORT = V49BuildManifest.COHORT_ID;
    public static final String EVIDENCE_DOMAIN_SCHEMA_VERSION = V49BuildManifest.SCHEMA_VERSION;
    public static final String BUILD_IDENTITY_SCHEMA_HOTFIX =
            "V43_SCHEMA_IDENTITY_UNIFIED_20260719_93518426";
    public static final String EVIDENCE_BACKFILL_STATE_KEY =
            "nova_evidence_domain_backfill_v28_72948163";
    public static final String DECISION_TRADE_REPAIR_STATE_KEY =
            "nova_decision_trade_field_repair_v28_72948163";

    /* 屏幕关闭后的主运行依靠前台Service + 可续租PARTIAL_WAKE_LOCK；Alarm/Job只做死亡恢复。 */
    public static final long ALWAYS_ON_WAKE_LEASE_MS = 15L * 60L * 1000L;
    public static final long ALWAYS_ON_WAKE_RENEW_INTERVAL_MS = 30L * 1000L;
    public static final long ALWAYS_ON_LOOP_REPAIR_INTERVAL_MS = 20L * 1000L;
    public static final long BACKGROUND_TEACHER_PUMP_STALE_MS = 120L * 1000L;
    public static final long BACKGROUND_LEARNING_PUMP_STALE_MS = 180L * 1000L;
    public static final long TASK_REMOVED_FETCH_GRACE_MS = 75L * 1000L;
    public static final long RECOVERY_REQUEST_COOLDOWN_MS = 120L * 1000L;
    public static final int EVIDENCE_BACKFILL_BATCH_SIZE = 40;

    public static boolean isFrequencyFloorPath(
            String betDomain, String decisionPath, int mandatoryFlag) {
        String b = normalizeToken(betDomain);
        String p = normalizeToken(decisionPath);
        return mandatoryFlag == 1
                || BET_DOMAIN_FREQUENCY_FLOOR.equals(b)
                || DECISION_PATH_FREQUENCY_FLOOR.equals(p)
                || "FREQUENCY_FLOOR".equals(b)
                || "FREQUENCY_FLOOR".equals(p);
    }

    public static String probabilityBand(double p) {
        double v = clampProbability(p);
        if (v >= 0.38d) return "P38_PLUS";
        if (v >= 0.36d) return "P36_38";
        if (v >= 0.35d) return "P35_36";
        return "P_LT35";
    }

    /* Nova清洁项目质量频率闭环 83/83 · 73487246 */
    public static final String ACTIVE_QUALITY_BUILD_VERSION = R36_RUNTIME_REPAIR_VERSION;
    public static final String ACTIVE_QUALITY_BUILD_COHORT = V49BuildManifest.COHORT_ID;
    public static final String ACTIVE_EVIDENCE_DOMAIN_SCHEMA = V49BuildManifest.SCHEMA_VERSION;
    public static final String TABLE_V20_MODULE_REGISTRY = "v20_module_registry";

    public static final String BET_DOMAIN_QUALIFIED_FLOOR = "QUALIFIED_FLOOR_BET";
    public static final String DECISION_PATH_QUALIFIED_FLOOR = "QUALIFIED_FLOOR_BET";
    public static final String DECISION_QUALITY_NORMAL = "NORMAL_BET";
    public static final String DECISION_QUALITY_FLOOR = "QUALIFIED_FLOOR_BET";
    public static final String DECISION_QUALITY_SHADOW = "SHADOW";
    public static final String DECISION_QUALITY_HOLD = "HOLD";

    public static final double QUALIFIED_FLOOR_MIN_FINAL_P = 0.350d;
    public static final double QUALIFIED_FLOOR_MIN_RAW_P = 0.345d;
    public static final double QUALIFIED_FLOOR_REFERENCE_P = 0.360d;
    public static final double QUALIFIED_FLOOR_MIN_EDGE = -0.010d;
    public static final double QUALIFIED_FLOOR_MIN_COVERAGE = 0.330d;
    public static final double QUALIFIED_FLOOR_MIN_THIRD = 0.100d;
    public static final double QUALIFIED_FLOOR_MAX_COUNTER_RISK = 0.580d;

    /* V23 causal-window realtime and idle micro walk-forward. */
    public static final int V23_REALTIME_FORMAL_CANDIDATES = 60;
    public static final int V23_MICRO_WALK_FORWARD_TARGETS = 10;
    public static final long V23_MICRO_WALK_FORWARD_BUDGET_MS = 8500L;
    public static final int V23_CAUSAL_HISTORY_CAP = 720;
    public static final boolean V23_LEGACY_PROBABILITY_REALTIME_ENABLED = false;
    public static final boolean V23_LEGACY_ADAPTIVE_CONSENSUS_REALTIME_ENABLED = false;
    public static final String V23_ACTIVE_PROBABILITY_OWNER =
            "V47_CROSSFIT_NUMBER_CHAMPION_FINAL40_FULL_DIAGNOSIS_20260721_47072147";

    public static final int FREQ_WINDOW_20 = 20;
    public static final int FREQ_WINDOW_50 = 50;
    public static final int FREQ_WINDOW_100 = 100;
    public static final int FREQ_WINDOW_300 = 300;
    public static final int FREQ_FLOOR_20 = 3;
    public static final int FREQ_TARGET_20 = 4;
    public static final int FREQ_FLOOR_50 = 8;
    public static final int FREQ_TARGET_50 = 10;
    public static final int FREQ_FLOOR_100 = 15;
    public static final int FREQ_TARGET_100 = 20;
    public static final int FREQ_FLOOR_300 = 45;
    public static final int FREQ_TARGET_300 = 60;
    public static final boolean NORMAL_BET_HAS_UPPER_CAP = false;

    public static final int DYNAMIC_WATCH_MIN_SAMPLE = 30;
    public static final int DYNAMIC_SOFT_RISK_MIN_SAMPLE = DYNAMIC_WATCH_MIN_SAMPLE;
    public static final int DYNAMIC_HARD_BLOCK_MIN_SAMPLE = 60;
    public static final double DYNAMIC_HARD_BLOCK_BASELINE = RANDOM_K3_BASELINE;
    public static final long REALTIME_ANALYSIS_TARGET_MS = 3500L;
    public static final long REALTIME_ANALYSIS_HARD_TIMEOUT_MS = 5000L;
    public static final long ANALYSIS_PROGRESS_PULSE_MS = 15L * 1000L;
    public static final long ANALYSIS_PROGRESS_STALE_MS = 45L * 1000L;
    public static final long RECOVERY_LEASE_MS = 120L * 1000L;
    public static final int BACKGROUND_MAINTENANCE_BATCH = 40;

    /* B07: independent runtime guard, bounded recovery leases, OOM serial retry and truthful diagnosis baseline. */
    public static final String B07_RUNTIME_RECOVERY_VERSION =
            "STARCORE_TIANSHU_B07_RUNTIME_OOS_DIAGNOSIS_RECOVERY_20260806";
    public static final long B07_RUNTIME_GUARD_INTERVAL_MS = 10L * 1000L;
    public static final long B07_RUNTIME_LEASE_MS = 45L * 1000L;
    public static final long B07_RECOVERY_LEASE_STEAL_MS = 20L * 1000L;
    public static final long B07_JOB_WORK_TIMEOUT_MS = 8L * 1000L;
    public static final long B07_OOM_RETRY_DELAY_MS = 1200L;
    public static final int B07_OOM_MAX_SERIAL_RETRY = 2;
    public static final long B07_STARTUP_BACKFILL_DELAY_MS = 8L * 1000L;
    public static final String B07_DIAG_BASELINE_SOURCE = "SYSTEM_BASELINE_NO_SAMPLE";

    /* B07R: B01-B07 reinforcement. No B08 capability is introduced here. */
    public static final String B07R_VERSION = "STARCORE_TIANSHU_B01_B07_REINFORCED_20260807";
    public static final String B07R2_VERSION = "STARCORE_TIANSHU_B01_B07_REINFORCED_R2_20260807";
    public static final String B07R3_VERSION = "STARCORE_TIANSHU_B01_B07_REINFORCED_R3_20260807";
    public static final String B07R32_VERSION =
            "STARCORE_TIANSHU_B01_B07_FINAL_REPAIR_R32_20260807";
    public static final String B07R32_STARTUP_POLICY =
            "UI_FIRST_ASYNC_DB_ASYNC_ENGINE_CRASH_CONTAINED";
    public static final String B07R322_VERSION =
            "STARCORE_TIANSHU_B01_B07_TOTAL_REPAIR_R322_20260808";
    public static final String B07R322_RUNTIME_POLICY =
            "NO_PROVIDER_UI_FIRST_FOREGROUND_FIRST_WATCHDOG_RECOVERY";
    public static final String B07R322_DIAG_POLICY =
            "WHOLE_SOFTWARE_ONE_COPY_EVIDENCE_FIRST_NO_FALSE_WINRATE_CLAIMS";
    public static final String B07R323_VERSION =
            "STARCORE_TIANSHU_B01_B07_FIELD_REPAIR_R323_20260808";
    public static final String B07R323_RUNTIME_POLICY =
            "PHYSICAL_DEVICE_HEARTBEAT_TRUTH_STARTUP_HEARTBEAT_INIT_RETRY_LIGHT_UI";
    public static final String B07R323_DIAG_POLICY =
            "CRASH_BACKGROUND_BATCH_CHAIN_EVIDENCE_ONE_COPY_SHA256";
    public static final String B07R3_DIAG_SCHEMA = "B07R3_FULL_STATE_DYNAMIC_AUDIT_V3";
    public static final boolean B07R3_CANDIDATE_LEVEL_REAL_AUTH = true;
    public static final boolean B07R3_GLOBAL_CHAMPION_IS_DIAGNOSTIC_ONLY = true;
    public static final boolean B07R3_NO_FREQUENCY_DEBT_ACTION = true;
    public static final boolean B07R3_FORCE_REARM_ON_TASK_REMOVED = true;
    public static final boolean B07R3_KERNEL_REALTIME_ENABLED = true;
    public static final boolean B07R3_KERNEL_REPLAY_ENABLED = true;
    public static final int B07R3_KERNEL_MIN_GROUPS = 4;
    public static final int B07R3_KERNEL_READY_SAMPLE = 30;
    public static final int B07R3_KERNEL_STRONG_SAMPLE = 80;
    public static final double B07R3_KERNEL_READY_RATE = 0.340d;
    public static final double B07R3_KERNEL_STRONG_RATE = 0.380d;
    public static final double B07R3_KERNEL_FREEZE_RATE = 0.280d;
    public static final long B07R3_KERNEL_SOFT_TIMEOUT_NS = 2000000L;
    public static final int B07R3_REPORT_MAX_CHARS = 240000;
    public static final int B07R3_REPORT_ESSENTIAL_MIN_CHARS = 12000;
    public static final String B07R3_BACKGROUND_SCOPE =
            "SWIPE_EXIT_SCREEN_OFF_LOCK_SUPPORTED_FORCE_STOP_NOT_BYPASSABLE";
    public static final String B07R31_FULL_REPAIR_VERSION =
            "STARCORE_TIANSHU_B01_B07_FULL_REPAIR_R31_20260807";
    public static final boolean V51_RUNTIME_LEGACY_HEAVY_ISOLATION = true;
    public static final long V51_OPTIONAL_EVIDENCE_BUDGET_MS = 700L;
    public static final long V51_REALTIME_SOFT_BUDGET_MS = 3500L;
    public static final long V51_REALTIME_HARD_REDLINE_MS = 5000L;
    public static final String V51_REALTIME_BUDGET_POLICY =
            "CORE_CHAIN_CONTINUES_OPTIONAL_EVIDENCE_SKIPS_AFTER_SOFT_NO_PARALLEL_RESCUE";
    public static final long V51_MISSING_PREDICTION_REPOST_MS = 4500L;
    public static final int B07R31_REPORT_COMPLETE_MAX_CHARS = 210000;
    public static final String B07R31_VERSION = "STARCORE_TIANSHU_B01_B07_REINFORCED_R31_20260807";
    public static final String B07R31_REAL_ACTION_POLICY =
            "MODEL_NOT_PROVEN_CONTROLLED_PROBE_DECISION_GATE_ONLY";
    public static final boolean B07R31_CONTROLLED_PROBE_ENABLED = true;
    public static final double B07R31_PROBE_SELECTED_P_MIN = 0.355d;
    public static final double B07R31_PROBE_PREDICTABILITY_MIN = 0.665d;
    public static final double B07R31_PROBE_CONSENSUS_MIN = 0.58d;
    public static final double B07R31_PROBE_STATE_CONFIDENCE_MIN = 0.56d;
    public static final double B07R31_PROBE_CHANGE_RISK_MAX = 0.36d;
    public static final double B07R31_PROBE_UNCERTAINTY_MAX = 0.42d;
    public static final double B07R31_PROBE_FALSIFIER_MAX = 0.38d;
    public static final double B07R31_PROBE_RETENTION_MIN = 0.62d;
    public static final double B07R31_PROBE_THIRD_ROBUSTNESS_MIN = 0.54d;
    public static final double B07R31_PROBE_RANK_MARGIN_MIN = 0.025d;
    public static final double B07R31_PROBE_THIRD_MARGIN_MIN = 0.015d;
    public static final String B07R2_DIAG_SCHEMA = "B07R2_FULL_STATE_AUDIT_V2";
    public static final boolean B07R2_PROCESS_BOOTSTRAP_REARM = true;
    public static final boolean B07R2_TASK_REMOVED_REARM = true;
    public static final boolean B07R2_LOW_MEMORY_SELF_HEAL = true;
    public static final boolean B07R2_NO_FAKE_MODEL_PROOF = true;
    public static final boolean B07R2_ACTIVE_TIANSHU_REAL_ACTION = true;
    public static final String B07R2_REAL_ACTION_POLICY =
            "DECISION_GATE_STRICT_LIVE_ACTION_NO_PROOF_FORGERY";
    public static final String B07R2_V50_PROOF_SCOPE = "INACTIVE_WHEN_ENABLE_V50_PRIME_FALSE";
    public static final long B07R2_RUNTIME_GUARD_INTERVAL_MS = 3000L;
    public static final long B07R2_BUSINESS_STALE_MS = 20000L;
    public static final int B07R2_REPORT_MAX_CHARS = 360000;
    public static final int B07R2_REPORT_RAW_STATE_CAP = 5000;
    public static final int B07R2_MIN_SETTLED_FOR_RATE_CLAIM = 60;
    public static final double B07R2_TARGET_HIT_40 = 0.400d;
    public static final double B07R2_TARGET_HIT_50 = 0.500d;
    public static final double B07R2_TARGET_HIT_60 = 0.600d;
    public static final double B07R2_TARGET_COVERAGE = 0.150d;
    public static final boolean B07R_BACKGROUND_CONTINUE_AFTER_UI_EXIT = true;
    public static final long B07R_RUNTIME_GUARD_INTERVAL_MS = 5000L;
    public static final long B07R_BUSINESS_STALE_MS = 25000L;
    public static final long B07R_FAST_RECOVERY_MS = 5000L;
    public static final int B07R_REPORT_MAX_CHARS = 420000;
    public static final boolean B07R_ONE_TAP_FULL_REPORT_COPY = true;
    public static final boolean B07R_LIVE_SIGNAL_ENABLED = true;
    public static final boolean B07R_UNATTENDED_MONEY_EXECUTION = false;
    public static final String B07R_EXECUTION_MODE = "LIVE_SIGNAL_MANUAL_CONFIRM";
    public static final String B07R_REAL_ACTION_OWNER = "DecisionGateEngine";
    public static final String B07R_PROOF_POLICY = "PIT_OOS_WILSON_ECE_MULTI_BLOCK_NO_FUTURE_LEAK";

    /* 合格补频只负责平滑填债，不限制NORMAL，也不允许连续突击补单。 */
    public static final int QUALIFIED_FLOOR_CRITICAL_MIN_GAP = 1;
    public static final int QUALIFIED_FLOOR_DEBT_MIN_GAP = 2;
    public static final int QUALIFIED_FLOOR_TARGET_MIN_GAP = 4;
    public static final int QUALIFIED_FLOOR_MAX_CONSECUTIVE = 1;

    public static int qualifiedFloorPacingMinGap(
            int floorDebt20,
            int floorDebt50,
            int floorDebt100,
            int floorDebt300,
            int targetDebt20,
            int targetDebt50,
            int targetDebt100,
            int targetDebt300) {
        if (floorDebt20 >= 2) return QUALIFIED_FLOOR_CRITICAL_MIN_GAP;
        if (floorDebt20 + floorDebt50 + floorDebt100 + floorDebt300 > 0)
            return QUALIFIED_FLOOR_DEBT_MIN_GAP;
        if (targetDebt20 + targetDebt50 + targetDebt100 + targetDebt300 > 0)
            return QUALIFIED_FLOOR_TARGET_MIN_GAP;
        return Integer.MAX_VALUE;
    }

    public static boolean isQualifiedFloorPacingReady(
            int issuesSinceLastRealBet, int consecutiveQualifiedFloorBets, int minGap) {
        if (minGap == Integer.MAX_VALUE) return false;
        if (consecutiveQualifiedFloorBets >= QUALIFIED_FLOOR_MAX_CONSECUTIVE) return false;
        return Math.max(0, issuesSinceLastRealBet) >= Math.max(0, minGap);
    }

    public static int frequencyFloorCount(int window, int observed) {
        int n = Math.max(0, Math.min(window, observed));
        if (window == FREQ_WINDOW_20 && n >= FREQ_WINDOW_20) return FREQ_FLOOR_20;
        if (window == FREQ_WINDOW_50 && n >= FREQ_WINDOW_50) return FREQ_FLOOR_50;
        if (window == FREQ_WINDOW_100 && n >= FREQ_WINDOW_100) return FREQ_FLOOR_100;
        if (window == FREQ_WINDOW_300 && n >= FREQ_WINDOW_300) return FREQ_FLOOR_300;
        return (int) Math.ceil(((double) n) * 0.150d - 1.0e-12d);
    }

    public static int frequencyTargetCount(int window, int observed) {
        int n = Math.max(0, Math.min(window, observed));
        if (window == FREQ_WINDOW_20 && n >= FREQ_WINDOW_20) return FREQ_TARGET_20;
        if (window == FREQ_WINDOW_50 && n >= FREQ_WINDOW_50) return FREQ_TARGET_50;
        if (window == FREQ_WINDOW_100 && n >= FREQ_WINDOW_100) return FREQ_TARGET_100;
        if (window == FREQ_WINDOW_300 && n >= FREQ_WINDOW_300) return FREQ_TARGET_300;
        return (int) Math.ceil(((double) n) * 0.200d - 1.0e-12d);
    }

    public static int frequencyFloorDebt(int window, int observed, int bets) {
        return Math.max(0, frequencyFloorCount(window, observed) - Math.max(0, bets));
    }

    public static int frequencyTargetDebt(int window, int observed, int bets) {
        return Math.max(0, frequencyTargetCount(window, observed) - Math.max(0, bets));
    }

    public static double frequencyDebtUrgency(int window, int observed, int bets) {
        int floor = frequencyFloorCount(window, observed);
        int floorDebt = Math.max(0, floor - Math.max(0, bets));
        int targetDebt = frequencyTargetDebt(window, observed, bets);
        if (floorDebt > 0) return 10.0d + ((double) floorDebt) / Math.max(1.0d, floor);
        int target = frequencyTargetCount(window, observed);
        return targetDebt <= 0 ? 0.0d : ((double) targetDebt) / Math.max(1.0d, target);
    }

    public static String selectUrgentFrequencyWindow(
            int observed20,
            int bets20,
            int observed50,
            int bets50,
            int observed100,
            int bets100,
            int observed300,
            int bets300) {
        int[] windows =
                new int[] {FREQ_WINDOW_20, FREQ_WINDOW_50, FREQ_WINDOW_100, FREQ_WINDOW_300};
        int[] observed = new int[] {observed20, observed50, observed100, observed300};
        int[] bets = new int[] {bets20, bets50, bets100, bets300};
        double best = 0.0d;
        int selected = 0;
        for (int i = 0; i < windows.length; i++) {
            double score = frequencyDebtUrgency(windows[i], observed[i], bets[i]);
            if (score > best + 1.0e-12d) {
                best = score;
                selected = windows[i];
            }
        }
        return selected <= 0 ? "" : String.valueOf(selected);
    }

    public static boolean isQualifiedFloorPath(String betDomain, String decisionPath) {
        String b = normalizeToken(betDomain);
        String p = normalizeToken(decisionPath);
        return BET_DOMAIN_QUALIFIED_FLOOR.equals(b) || DECISION_PATH_QUALIFIED_FLOOR.equals(p);
    }

    public static double qualifiedFloorQualityEdge(double finalP) {
        double p = clampProbability(finalP);
        return p - QUALIFIED_FLOOR_REFERENCE_P;
    }

    public static boolean isQualifiedFloorQuality(
            double finalP,
            double rawP,
            double edge,
            double coverage,
            double third,
            double counterRisk) {
        if (clampProbability(finalP) < QUALIFIED_FLOOR_MIN_FINAL_P) return false;
        if (clampProbability(rawP) < QUALIFIED_FLOOR_MIN_RAW_P) return false;
        if (Double.isNaN(edge)
                || Double.isInfinite(edge)
                || edge + 1.0e-12d < QUALIFIED_FLOOR_MIN_EDGE) return false;
        if (coverage < QUALIFIED_FLOOR_MIN_COVERAGE) return false;
        if (third < QUALIFIED_FLOOR_MIN_THIRD) return false;
        return counterRisk <= QUALIFIED_FLOOR_MAX_COUNTER_RISK;
    }

    /** 新数据库冷启动中性恢复合同。 只允许撤销“无同域结算样本”产生的折扣；不绕过结构硬风险和K3质量线。 */
    public static boolean isColdStartNeutralRestorationEligible(
            int historySize,
            int teacherSettled,
            int dynamicSettled,
            int dynamicClusterSample,
            boolean teacherNeutral,
            boolean dynamicSafe,
            boolean runtimeSafe,
            boolean stateSafe,
            boolean explicitHard,
            int coverageAvailable,
            int thirdAvailable,
            double coverage,
            double third,
            double counterRisk,
            double beforeTeacher,
            double afterTeacher,
            double beforeRisk) {
        if (!COLD_START_NEUTRAL_RESTORE_ENABLE) return false;
        if (historySize < COLD_START_NEUTRAL_MIN_HISTORY) return false;
        if (teacherSettled > 0 || dynamicSettled > 0 || dynamicClusterSample > 0) return false;
        if (!teacherNeutral || !dynamicSafe || !runtimeSafe || !stateSafe || explicitHard)
            return false;
        if (coverageAvailable == 0 || thirdAvailable == 0) return false;
        if (coverage < QUALIFIED_FLOOR_MIN_COVERAGE) return false;
        if (third < QUALIFIED_FLOOR_MIN_THIRD) return false;
        if (counterRisk > COLD_START_NEUTRAL_MAX_COUNTER_RISK) return false;
        if (clampProbability(beforeTeacher) < COLD_START_NEUTRAL_MIN_PRE_RISK_P) return false;
        if (clampProbability(afterTeacher) < COLD_START_NEUTRAL_MIN_AFTER_TEACHER_P) return false;
        return clampProbability(beforeRisk) >= COLD_START_NEUTRAL_MIN_PRE_RISK_P;
    }

    public static double coldStartNeutralRestoredProbability(
            double beforeTeacher, double beforeRisk) {
        double restored =
                Math.min(
                        COLD_START_NEUTRAL_RESTORE_CAP,
                        Math.min(clampProbability(beforeTeacher), clampProbability(beforeRisk)));
        return restored < COLD_START_NEUTRAL_RESTORE_FINAL_P ? 0.0d : restored;
    }

    public static boolean isMatureDynamicHardBlock(
            String status, int sample, double hitRate, double wilsonUpper) {
        String s = normalizeToken(status);
        if (sample < DYNAMIC_HARD_BLOCK_MIN_SAMPLE) return false;
        boolean riskText =
                s.indexOf("HARD") >= 0 || s.indexOf("BLOCK") >= 0 || s.indexOf("FREEZE") >= 0;
        return riskText
                && wilsonUpper > 0.0d
                && wilsonUpper < DYNAMIC_HARD_BLOCK_BASELINE
                && hitRate < DYNAMIC_HARD_BLOCK_BASELINE;
    }

    public static String dynamicRiskTier(
            String status, int sample, double hitRate, double wilsonUpper) {
        if (sample < DYNAMIC_WATCH_MIN_SAMPLE) return "WATCH";
        if (sample < DYNAMIC_HARD_BLOCK_MIN_SAMPLE) {
            String t = normalizeToken(status);
            return (t.indexOf("RISK") >= 0
                            || t.indexOf("HARD") >= 0
                            || t.indexOf("BLOCK") >= 0
                            || t.indexOf("FREEZE") >= 0)
                    ? "SOFT_RISK"
                    : "WATCH";
        }
        if (isMatureDynamicHardBlock(status, sample, hitRate, wilsonUpper)) return "HARD_BLOCK";
        String matureText = normalizeToken(status);
        if (matureText.indexOf("RISK") >= 0
                || matureText.indexOf("HARD") >= 0
                || matureText.indexOf("BLOCK") >= 0
                || matureText.indexOf("FREEZE") >= 0) return "SOFT_RISK";
        return effectiveDynamicRiskTier(status, sample);
    }

    public static String effectiveDynamicRiskTier(String status, int sample) {
        String s = normalizeToken(status);
        if (sample < DYNAMIC_WATCH_MIN_SAMPLE) return "WATCH";
        if (sample < DYNAMIC_HARD_BLOCK_MIN_SAMPLE
                && (s.indexOf("RISK") >= 0 || s.indexOf("HARD") >= 0 || s.indexOf("BLOCK") >= 0))
            return "SOFT_RISK";
        return s.length() == 0 ? "WATCH" : s;
    }

    /* 73487246 finite hierarchy and point-in-time evidence fusion. */
    public static final String HIERARCHICAL_EVIDENCE_FUSION_VERSION =
            "V47_CROSSFIT_NUMBER_CHAMPION_FINAL40_FULL_DIAGNOSIS_20260721_47072147";
    public static final String HIERARCHICAL_EVIDENCE_COHORT = V49BuildManifest.COHORT_ID;
    public static final int HIERARCHICAL_PRIOR_MIN_HISTORY = 30;
    public static final int HIERARCHICAL_PRIOR_READY_HISTORY = 60;
    public static final int REALTIME_EXACT_LIGHT_SCAN_COUNT = 1200;
    public static final int REALTIME_DEEP_FUSION_CANDIDATES = 18;
    public static final int DYNAMIC_MATURE_VETO_MIN_SAMPLE = 60;
    public static final boolean NO_SAMPLE_EVIDENCE_IS_NEUTRAL = true;

    /* 星河 V40：全号码分布、在线残差、快速走前、分层后验和覆盖率受控授权。 */
    public static final String BUILD_ID = V50BuildManifest.BUILD_ID;
    public static final String PIPELINE_VERSION = V50BuildManifest.STRATEGY_VERSION;
    public static final String COHORT_ID = V50BuildManifest.COHORT_ID;
    public static final String SCHEMA_VERSION = V50BuildManifest.SCHEMA_ID;
    public static final String FEATURE_SCHEMA_VERSION = V50BuildManifest.FEATURE_SCHEMA;
    public static final String RANKING_VERSION = V50BuildManifest.RANK_ARTIFACT_ID;
    public static final String CALIBRATION_VERSION = V50BuildManifest.CALIBRATION_ARTIFACT_ID;
    public static final String GATE_POLICY_VERSION = V50BuildManifest.POLICY_ARTIFACT_ID;
    public static final String PREDICTION_CONTRACT_VERSION = V50BuildManifest.ACTION_SCHEMA;
    public static final String OOS_ACTION_LOG_VERSION = "V50_STRICT_WALK_FORWARD_OOS";
    public static final String HIERARCHICAL_POSTERIOR_VERSION =
            "NOVA_HIERARCHICAL_POSTERIOR_V46_46072130";
    public static final String MICRO_WALK_FORWARD_VERSION = "NOVA_MICRO_WALK_FORWARD_V46_46072130";
    public static final String DECISION_AUTHORIZATION_VERSION =
            "NOVA_DECISION_AUTHORIZATION_V46_46072130";
    public static final String REALTIME_CAUSAL_PROBABILITY_VERSION =
            "NOVA_REALTIME_EVENT_PROBABILITY_V46_46072130";
    public static final String ONLINE_ACTION_MODEL_VERSION =
            "NOVA_ONLINE_RESIDUAL_MODEL_V46_46072130";

    public static final double CONTRACT_PROBABILITY_EPSILON = 0.0000001d;
    public static final int CALIBRATION_BUCKET_COUNT = 10;
    public static final int CALIBRATION_PROBE_TOTAL_SAMPLE = 30;
    public static final int CALIBRATION_WARM_TOTAL_SAMPLE = 30;
    public static final int CALIBRATION_READY_TOTAL_SAMPLE = 30;
    public static final int CALIBRATION_FULL_TOTAL_SAMPLE = 480;
    public static final int CALIBRATION_READY_BUCKET_SAMPLE = 4;
    public static final int CALIBRATION_BOOTSTRAP_MIN_SAMPLE = 4;
    public static final double CALIBRATED_PROBABILITY_MIN = 0.180d;
    public static final double CALIBRATED_PROBABILITY_MAX = 0.550d;
    public static final double OOS_BOOTSTRAP_DISPLAY_MIN = 0.235d;
    public static final double OOS_BOOTSTRAP_DISPLAY_MAX = 0.455d;
    public static final double OOS_BOOTSTRAP_RANK_SLOPE = 0.180d;
    public static final double OOS_CALIBRATION_PRIOR_STRENGTH = 20.0d;
    public static final double ACTION_MODEL_INITIAL_BASELINE = RANDOM_K3_BASELINE;
    public static final double ACTION_MODEL_LEARNING_RATE = 0.055d;
    public static final double ACTION_MODEL_L2 = 0.0025d;
    public static final double ACTION_MODEL_WEIGHT_LIMIT = 2.50d;
    public static final int ACTION_MODEL_FEATURE_COUNT = 8;

    public static final int POSTERIOR_FINE_MIN_SAMPLE = 36;
    public static final int POSTERIOR_MID_MIN_SAMPLE = 18;
    public static final int POSTERIOR_COARSE_MIN_SAMPLE = 6;
    public static final int POSTERIOR_POSITION_MIN_SAMPLE = 8;
    public static final int POSTERIOR_READY_MIN_SAMPLE = 8;
    public static final int POSTERIOR_STRONG_MIN_SAMPLE = 36;
    public static final int POSTERIOR_HARD_RISK_MIN_SAMPLE = 60;
    public static final int POSTERIOR_RECENT_WINDOW = 48;
    public static final double POSTERIOR_READY_LCB = 0.180d;
    public static final double POSTERIOR_STRONG_LCB = 0.260d;
    public static final double POSTERIOR_SOFT_RISK_RATE = 0.245d;
    public static final double POSTERIOR_DRIFT_HARD = 0.150d;

    public static final int MICRO_WALK_FORWARD_BATCH_ORIGINS = 8;
    public static final int MICRO_WALK_FORWARD_MAX_ORIGINS = 16;
    public static final boolean MICRO_WALK_FORWARD_HISTORICAL_BOOTSTRAP_ENABLED = true;
    public static final boolean MICRO_WALK_FORWARD_PRIMARY_ACTION_ONLY = false;
    public static final int MICRO_WALK_FORWARD_HISTORY_LOAD = 2400;
    public static final int MICRO_WALK_FORWARD_MIN_TRAIN_HISTORY = 30;
    public static final int MICRO_WALK_FORWARD_VISIBLE_HISTORY = 360;
    public static final int MICRO_WALK_FORWARD_PROBE_SAMPLE = 6;
    public static final int MICRO_WALK_FORWARD_WARM_SAMPLE = 18;
    public static final int MICRO_WALK_FORWARD_READY_SAMPLE = 30;
    public static final long MICRO_WALK_FORWARD_TARGET_BUDGET_MS = 6500L;
    public static final long MICRO_WALK_FORWARD_HARD_BUDGET_MS = 9000L;
    public static final long MICRO_WALK_FORWARD_PROTECTED_SLICE_MS = 6500L;
    public static final long MICRO_WALK_FORWARD_STARVATION_MS = 60000L;
    public static final long MICRO_WALK_FORWARD_START_GUARD_MS = 6000L;
    public static final int MICRO_WALK_FORWARD_MIN_PROGRESS_PER_BATCH = 4;
    public static final int MICRO_WALK_FORWARD_LIGHT_TOP_NUMBERS = 5;

    public static final double AUTH_MIN_REAL_PROBABILITY = 0.340d;
    public static final double AUTH_MIN_FAMILY_LCB = 0.180d;
    public static final double AUTH_MIN_COVERAGE = 0.400d;
    public static final double AUTH_MIN_THIRD_QUALITY = 0.200d;
    public static final double AUTH_NORMAL_MIN_EDGE = 0.000d;
    public static final double AUTH_QUALIFIED_FLOOR_PROBABILITY = 0.305d;
    public static final double AUTH_QUALIFIED_FLOOR_RAW_PROBABILITY = 0.305d;
    public static final double AUTH_QUALIFIED_FLOOR_MIN_EDGE = -0.095d;
    public static final double AUTH_REAL_PROBE_MIN_MODEL_P = 0.300d;
    public static final double AUTH_REAL_PROBE_MIN_RANK_SCORE = 0.560d;
    public static final double AUTH_REAL_PROBE_MIN_COVERAGE = 0.300d;
    public static final double AUTH_REAL_PROBE_MIN_THIRD = 0.100d;
    public static final double AUTH_REAL_PROBE_MAX_COUNTER_RISK = 0.660d;
    public static final double AUTH_REAL_PROBE_MAX_FREQUENCY = 0.405d;
    public static final int AUTH_REAL_PROBE_MIN_GAP = 2;
    public static final boolean STRICT_AUTHORIZATION_REQUIRED_FOR_REAL_BET = true;
    public static final boolean STRICT_CALIBRATION_READY_FOR_NORMAL_BET = true;
    public static final boolean STRICT_MICRO_WALK_FORWARD_READY_FOR_NORMAL_BET = true;

    /* V40：每个历史origin发布Top-K不可变学习候选；真实交易仍最多一单。WATCH为中性，只有显式风险否决。 */
    public static final String OOS_ELIGIBILITY_VERSION =
            "NOVA_OOS_EVIDENCE_ELIGIBILITY_V6_61830574";
    public static final String OOS_TRAINING_COORDINATOR_VERSION =
            "NOVA_OOS_TRAINING_COORDINATOR_V4_61830574";
    public static final int MICRO_WALK_FORWARD_CANDIDATES_PER_ORIGIN = 3;
    public static final int MICRO_WALK_FORWARD_MAX_PREDICTIONS_PER_BATCH = 48;
    public static final int OOS_LIVE_CANDIDATES_PER_ISSUE = 40;
    public static final long MICRO_WALK_FORWARD_REPOST_DELAY_MS = 700L;
    public static final long MICRO_WALK_FORWARD_YIELD_RETRY_MS = 1200L;
    public static final long MICRO_WALK_FORWARD_IDLE_DELAY_MS = 8000L;
    public static final double POSTERIOR_READY_MEAN = 0.305d;
    public static final double POSTERIOR_STRONG_MEAN = 0.375d;
    public static final double POSTERIOR_READY_RECENT_RATE = 0.290d;
    public static final double AUTH_MIN_POSTERIOR_MEAN = 0.305d;
    public static final double AUTH_MIN_POSTERIOR_RECENT_RATE = 0.290d;
    public static final double OOS_FAMILY_ADJUSTMENT_MAX = 0.045d;
    public static final int OOS_FAMILY_ADJUSTMENT_FULL_SAMPLE = 48;

    /* V40：事件概率与动作效用彻底解耦。频率只进入utility，绝不抬高概率。 */
    public static final String ACTION_UTILITY_VERSION = "NOVA_ACTION_UTILITY_V4_61830574";
    public static final String V28_1_RUNTIME_HOTFIX_VERSION =
            "V43_FULL_STRATEGY_RUNTIME_20260719_93518426";
    public static final String PROBE_LANE_POLICY_VERSION = "NOVA_PROBE_LANE_POLICY_V2_61830574";
    public static final double AUTH_REAL_PROBE_MIN_HONEST_P = 0.295d;
    public static final double AUTH_REAL_PROBE_MIN_UTILITY = 0.260d;
    public static final double AUTH_REAL_PROBE_MAX_UNCERTAINTY = 0.900d;
    public static final double AUTH_QUALIFIED_FLOOR_MIN_UTILITY = 0.485d;
    public static final double AUTH_QUALIFIED_FLOOR_MAX_UNCERTAINTY = 0.620d;
    public static final double AUTH_NORMAL_MIN_UTILITY = 0.560d;
    public static final double AUTH_NORMAL_MAX_UNCERTAINTY = 0.480d;
    public static final int OOS_TOPK_MIN_DISTINCT_POSITIONS = 2;
    public static final int OOS_TOPK_MAX_SAME_POSITION = 1;
    public static final double UTILITY_PROBABILITY_FLOOR = 0.270d;
    public static final double UTILITY_PROBABILITY_RANGE = 0.160d;

    /* V40: registered full-distribution prediction. These constants affect only
     * pre-draw probability construction; frequency and action state are excluded. */
    public static final String SEQUENCE_DISTRIBUTION_VERSION =
            "NOVA_SEQUENCE_DISTRIBUTION_VERSION_V47_47072147";
    public static final String REGISTERED_REGIME_VERSION = "NOVA_REGISTERED_REGIME_V1_61830574";
    public static final int SEQUENCE_MAX_HISTORY = 1200;
    public static final int SEQUENCE_MIN_HISTORY = 48;
    public static final int SEQUENCE_MATURE_HISTORY = 360;
    public static final int SEQUENCE_WINDOW_12 = 12;
    public static final int SEQUENCE_WINDOW_24 = 24;
    public static final int SEQUENCE_WINDOW_48 = 48;
    public static final int SEQUENCE_WINDOW_96 = 96;
    public static final int SEQUENCE_WINDOW_360 = 360;
    public static final int SEQUENCE_EWMA_HALF_LIFE = 32;
    public static final int SEQUENCE_EWMA_MAX_HISTORY = 360;
    public static final int SEQUENCE_GAP_CAP = 60;
    public static final double SEQUENCE_DIRICHLET_ALPHA = 1.25d;
    public static final double SEQUENCE_TRANSITION_ALPHA = 1.75d;
    public static final double SEQUENCE_PAIR_ALPHA = 1.25d;
    public static final double SEQUENCE_MOVEMENT_ALPHA = 1.50d;
    public static final double SEQUENCE_GAP_PRIOR_STRENGTH = 18.0d;
    public static final double SEQUENCE_MARKOV_SUPPORT_STRENGTH = 26.0d;
    public static final double SEQUENCE_PAIR_SUPPORT_STRENGTH = 42.0d;
    public static final double SEQUENCE_MOVEMENT_SUPPORT_STRENGTH = 30.0d;
    public static final double SEQUENCE_GAP_SUPPORT_STRENGTH = 40.0d;
    public static final double SEQUENCE_WEIGHT_BASE = 0.08d;
    public static final double SEQUENCE_WEIGHT_RECENT = 0.25d;
    public static final double SEQUENCE_WEIGHT_EWMA = 0.18d;
    public static final double SEQUENCE_WEIGHT_MARKOV = 0.18d;
    public static final double SEQUENCE_WEIGHT_PAIR = 0.08d;
    public static final double SEQUENCE_WEIGHT_MOVEMENT = 0.12d;
    public static final double SEQUENCE_WEIGHT_GAP = 0.11d;
    public static final double SEQUENCE_COMPONENT_DISAGREEMENT_SCALE = 0.075d;
    public static final double SEQUENCE_MIN_SIGNAL_KEEP = 0.30d;
    public static final double SEQUENCE_MATURITY_SIGNAL_WEIGHT = 0.38d;
    public static final double SEQUENCE_AGREEMENT_SIGNAL_WEIGHT = 0.22d;
    public static final double SEQUENCE_MAX_SIGNAL_KEEP = 0.90d;
    public static final double SEQUENCE_RANK_PROBABILITY_SCALE = 0.18d;
    public static final double SEQUENCE_RANK_PROBABILITY_WEIGHT = 0.78d;

    /* V40 authorization is evidence-gated.  It does not claim a guaranteed rate. */
    public static final double V40_NORMAL_MIN_PROBABILITY = 0.345d;
    public static final double V40_NORMAL_MIN_LCB = 0.285d;
    public static final double V40_PROBE_MIN_PROBABILITY = AUTH_REAL_PROBE_MIN_HONEST_P;
    public static final double V40_PROBE_MIN_UTILITY = AUTH_REAL_PROBE_MIN_UTILITY;
    public static final double V40_PROBE_MAX_UNCERTAINTY = AUTH_REAL_PROBE_MAX_UNCERTAINTY;
    public static final int V40_PROBE_MAX_PER_50 = 8;
    public static final String V41_LIVE_ACTION_BUDGET_VERSION =
            "NOVA_LIVE_ACTION_BUDGET_V1_61830574";
    public static final int V41_PROBE_PACING_MIN_GAP = 4;
    public static final int V40_CALIBRATION_MIN_LOCAL = 24;
    public static final int V40_POSTERIOR_MIN_READY = 30;
    public static final double V40_POSTERIOR_BLEND_MAX = 0.28d;
    public static final double V40_CALIBRATION_MAX_LOCAL_WEIGHT = 0.78d;
    public static final double V40_COLD_MODEL_PRIOR_TRUST = 0.15d;

    /* V41 live-action repair: bounded 8/50 probe budget, no probability inflation. */
    public static final String V41_LIVE_ACTION_REPAIR_VERSION =
            "V43_FULL_STRATEGY_LIVE_ACTION_20260719_93518426";
    public static final double V41_PROBE_MIN_STRUCTURE = 0.440d;
    public static final double V41_PROBE_MIN_RANK_SCORE = AUTH_REAL_PROBE_MIN_RANK_SCORE;
    public static final int V41_DIAGNOSIS_CACHE_MAX_CHARS = 220000;
    public static final int V41_CLIPBOARD_SAFE_CHUNK_CHARS = 24000;

    /* =====================================================================
     * V42 unified risk-coverage contract.
     *
     * Probability engines may estimate event probability; feature engines may
     * produce evidence.  Only LanePolicyCompiler classifies NORMAL/FLOOR/PROBE,
     * RiskCoverageController owns coverage pacing, and DecisionGateEngine remains
     * the sole real BET exit.  Frequency never writes back into probability.
     * ===================================================================== */
    public static final String V42_REGIME_REGISTRY_VERSION = "NOVA_REGIME_REGISTRY_V42_72841963";
    public static final String V42_ASSOCIATION_GRAPH_VERSION =
            "NOVA_ASSOCIATION_GRAPH_V42_72841963";
    public static final String V42_COUNTERFACTUAL_DISTILL_VERSION =
            "NOVA_COUNTERFACTUAL_DISTILL_V42_72841963";
    public static final String V42_RISK_COVERAGE_VERSION =
            "NOVA_RISK_COVERAGE_BACKGROUND_CHALLENGER_V46_46072130";
    public static final String V42_LANE_POLICY_VERSION = "NOVA_LANE_POLICY_V42_72841963";
    public static final String V42_OPPORTUNITY_MINER_VERSION =
            "NOVA_OPPORTUNITY_MINER_V42_72841963";
    public static final String V42_LIVE_ACTION_BUDGET_VERSION =
            "NOVA_LIVE_ACTION_BUDGET_BACKGROUND_CHALLENGER_V46_46072130";
    public static final String V42_ACTION_UTILITY_VERSION = "NOVA_ACTION_UTILITY_V42_72841963";
    public static final String V42_OOS_ELIGIBILITY_VERSION = "NOVA_OOS_ELIGIBILITY_V42_72841963";
    public static final String V42_DECISION_AUTHORIZATION_VERSION =
            "NOVA_DECISION_AUTHORIZATION_V42_72841963";

    /* Regime registry: values are normalized to [0,1] inside the registry. */
    public static final double V42_REGIME_CHAOS_DRIFT = 0.700d;
    public static final double V42_REGIME_CHAOS_COUNTER = 0.700d;
    public static final double V42_REGIME_CHAOS_CONFLICT = 0.720d;
    public static final double V42_REGIME_TREND_MIN = 0.520d;
    public static final double V42_REGIME_TREND_MAX_DRIFT = 0.480d;
    public static final double V42_REGIME_TREND_MIN_CONSISTENCY = 0.460d;
    public static final double V42_REGIME_RECOVERY_MIN_BREAK = 0.220d;
    public static final double V42_REGIME_RECOVERY_MIN_SCORE = 0.500d;
    public static final double V42_REGIME_RECOVERY_MAX_DRIFT = 0.620d;

    /* Position-number-state association graph. */
    public static final double V42_ASSOCIATION_HARD_CONFLICT = 0.780d;
    public static final double V42_ASSOCIATION_STRONG = 0.620d;
    public static final double V42_ASSOCIATION_READY = 0.420d;

    /* Shadow/counterfactual distillation. */
    public static final double V42_DISTILL_HARD_RISK = 0.780d;
    public static final double V42_DISTILL_SOFT_RISK = 0.580d;
    public static final double V42_DISTILL_READY_SUPPORT = 0.400d;

    /* Explicit bounded-cover controller. */
    public static final long V42_RISK_COVERAGE_CACHE_MS = 15000L;
    public static final double V42_COVERAGE_MIN = 0.150d;
    public static final double V42_COVERAGE_TARGET = 0.200d;
    public static final double V42_NORMAL_ACCEPT_MIN = 0.535d;
    public static final double V42_FLOOR_ACCEPT_MIN = 0.475d;
    public static final double V42_PROBE_ACCEPT_MIN = 0.425d;
    public static final double V42_COVERAGE_ACCEPT_RELAX_MAX = 0.040d;
    public static final double V42_FLOOR_ACCEPT_HARD_MIN = 0.440d;
    public static final double V42_PROBE_ACCEPT_HARD_MIN = 0.385d;
    public static final int V42_PROBE_CAP_PER_50 = 10;
    public static final int V42_PROBE_MIN_GAP = 3;
    public static final int V42_ZERO_START_PROBE_GAP = 2;
    public static final int V42_RC_MIN_QUALITY_SAMPLE = 20;
    public static final double V42_RC_HARD_CONTRACT_RATE = BREAK_EVEN_PROBABILITY;
    public static final double V42_RC_HARD_CONTRACT_W80 = 0.250d;
    public static final double V42_RC_EXPAND_RATE = 0.400d;
    public static final double V42_RC_EXPAND_W80 = 0.320d;
    public static final int V42_RC_HISTORY_LIMIT = 100;

    /* One and only lane contract.  These are evidence requirements, not
     * guarantees of a future hit rate. */
    public static final double V42_NORMAL_MIN_P = 0.370d;
    public static final double V42_NORMAL_MIN_FAMILY_LCB = 0.310d;
    public static final double V42_NORMAL_MIN_STRUCTURE = 0.360d;
    public static final double V42_NORMAL_MIN_REGIME = 0.480d;
    public static final double V42_NORMAL_MIN_ASSOCIATION = 0.400d;
    public static final double V42_NORMAL_MAX_UNCERTAINTY = 0.440d;
    public static final double V42_NORMAL_MAX_COUNTER = 0.460d;
    public static final double V42_NORMAL_MAX_DISTILL_RISK = 0.440d;
    public static final int V42_NORMAL_MIN_POSTERIOR = 48;
    public static final int V42_NORMAL_MIN_CALIBRATION = 18;
    public static final int V42_NORMAL_MIN_SEQUENCE = 192;
    public static final int V42_NORMAL_MIN_SUPPORT_VOTES = 5;

    public static final double V42_FLOOR_MIN_P = 0.345d;
    public static final double V42_FLOOR_MIN_FAMILY_LCB = 0.290d;
    public static final double V42_FLOOR_MIN_STRUCTURE = 0.300d;
    public static final double V42_FLOOR_MIN_REGIME = 0.420d;
    public static final double V42_FLOOR_MIN_ASSOCIATION = 0.340d;
    public static final double V42_FLOOR_MAX_UNCERTAINTY = 0.540d;
    public static final double V42_FLOOR_MAX_COUNTER = 0.540d;
    public static final double V42_FLOOR_MAX_DISTILL_RISK = 0.540d;
    public static final int V42_FLOOR_MIN_POSTERIOR = 24;
    public static final int V42_FLOOR_MIN_CALIBRATION = 10;
    public static final int V42_FLOOR_MIN_SEQUENCE = 96;
    public static final int V42_FLOOR_MIN_MICRO = 10;
    public static final int V42_FLOOR_MIN_MATURITY_VOTES = 2;
    public static final int V42_FLOOR_MIN_SUPPORT_VOTES = 4;

    public static final double V42_PROBE_MIN_P = 0.315d;
    public static final double V42_PROBE_MIN_MODEL_P = 0.300d;
    public static final double V42_PROBE_MIN_RANK = 0.560d;
    public static final double V42_PROBE_MIN_FAMILY_LCB = 0.270d;
    public static final int V42_PROBE_FAMILY_COLD_SAMPLE = 12;
    public static final double V42_PROBE_COLD_MIN_REGIME = 0.520d;
    public static final double V42_PROBE_COLD_MIN_ASSOCIATION = 0.460d;
    public static final double V42_PROBE_MIN_STRUCTURE = 0.280d;
    public static final double V42_PROBE_MIN_REGIME = 0.380d;
    public static final double V42_PROBE_MIN_ASSOCIATION = 0.300d;
    public static final double V42_PROBE_MAX_UNCERTAINTY = 0.660d;
    public static final double V42_PROBE_MAX_COUNTER = 0.580d;
    public static final double V42_PROBE_MAX_DISTILL_RISK = 0.580d;
    public static final int V42_PROBE_MIN_SUPPORT_VOTES = 4;
    public static final double V42_FAMILY_COLD_PRIOR_LCB = 0.270d;
    public static final double V42_FAMILY_LCB_Z = 1.2815515655446004d;

    /* V44 controlled-bootstrap contract.  Frequency debt may select only candidates
     * that satisfy these quality floors; it never manufactures probability. */
    public static final String V44_DECISION_FUNNEL_VERSION = "LEGACY_DIAGNOSTIC_DISABLED_BY_V46";
    public static final String V44_CONTROLLED_LANE_VERSION = "LEGACY_LANE_DISABLED_BY_V46";
    public static final double V44_CONTROLLED_MIN_FINAL_P = 0.350d;
    public static final double V44_CONTROLLED_MIN_MODEL_P = 0.345d;
    public static final double V44_CONTROLLED_GATE = 0.360d;
    public static final double V44_CONTROLLED_MAX_NEGATIVE_EDGE = 0.010d;
    public static final double V44_CONTROLLED_MIN_COVERAGE = 0.330d;
    public static final double V44_CONTROLLED_MIN_THIRD = 0.088d;
    public static final double V44_CONTROLLED_MIN_CONFIDENCE = 0.280d;
    public static final double V44_CONTROLLED_MAX_FRAGILITY = 0.780d;
    public static final double V44_CONTROLLED_MAX_UNCERTAINTY = 0.620d;
    public static final double V44_CONTROLLED_MAX_COUNTER = 0.580d;
    public static final double V44_CONTROLLED_MAX_DISTILL_RISK = 0.580d;
    public static final double V44_CONTROLLED_MIN_ACCEPT = 0.385d;
    public static final int V44_CONTROLLED_MIN_SUPPORT_VOTES = 3;
    public static final int V44_CONTROLLED_MIN_QUALITY_VOTES = 4;
    public static final int V44_DIAGNOSIS_MAX_ISSUES = 720;
    public static final int V44_DIAGNOSIS_MAX_CANDIDATES = 4000;

    /* =====================================================================
     * V46 number-distribution, conditional-calibration and final-rank contract.
     * 30 issues starts controlled observation/probe eligibility; it never forces a BET.
     * Long 120/240/360 windows remain evidence windows, not startup locks.
     * ===================================================================== */
    public static final String V46_VERSION = "V46_NUMBER_DISTRIBUTION_FINAL_RANK_46072130";
    public static final int V46_FULL_SCAN_CANDIDATE_COUNT = 1200;
    public static final int V46_CONTROLLED_START_MIN_ISSUES = 30;
    public static final int V46_NORMAL_START_MIN_ISSUES = 30;
    public static final int V46_PROBABILITY_CANDIDATE_COUNT = 60;
    public static final int V46_FORMAL_CANDIDATE_COUNT = 40;
    public static final int V46_UI_DISPLAY_CANDIDATE_COUNT = 12;
    public static final int V46_MIN_FORMAL_PER_POSITION = 2;
    public static final int V46_DIAGNOSIS_MAX_ISSUES = 720;
    public static final int V46_DIAGNOSIS_MAX_CANDIDATES = 40000;
    public static final boolean V46_DISABLE_LEGACY_REAL_GATE = true;
    public static final boolean V46_SINGLE_TEACHER_SNAPSHOT_PER_ISSUE = true;
    public static final double V46_NORMAL_MIN_EVENT_P = 0.370d;
    public static final double V46_NORMAL_MIN_ACTION_LCB = 0.315d;
    public static final double V46_FLOOR_MIN_EVENT_P = 0.350d;
    public static final double V46_FLOOR_MIN_ACTION_LCB = 0.300d;
    public static final double V46_PROBE_MIN_EVENT_P = 0.335d;
    public static final double V46_PROBE_MIN_ACTION_LCB = 0.285d;
    public static final double V46_PROBE_MIN_MODEL_P = 0.330d;
    public static final double V46_MIN_COVERAGE = 0.330d;
    public static final double V46_MIN_THIRD_QUALITY = 0.088d;
    public static final double V46_MAX_UNCERTAINTY_FLOOR = 0.620d;
    public static final double V46_MAX_UNCERTAINTY_PROBE = 0.680d;
    public static final double V46_MAX_COUNTER_RISK = 0.580d;
    public static final int V46_MIN_QUALITY_VOTES = 4;
    public static final double V46_MAX_CONTROLLED_NEGATIVE_EDGE = 0.010d;
    public static final double V46_NORMAL_GATE = 0.370d;
    public static final double V46_FLOOR_GATE = 0.345d;
    public static final double V46_PROBE_GATE = BREAK_EVEN_PROBABILITY;
    public static final double V46_MAX_CONDITIONAL_ADJUSTMENT = 0.035d;
    public static final double V46_MAX_ACTION_BOUND_PENALTY = 0.055d;
    public static final double V46_MAX_PROBABILITY_UNCERTAINTY = 0.80d;
    public static final int V46_CONDITIONAL_GLOBAL_MIN = 30;
    public static final int V46_CONDITIONAL_PARTIAL_MIN = 80;
    public static final int V46_CONDITIONAL_FULL_MIN = 240;
    public static final long V46_REALTIME_TARGET_MS = 6500L;
    public static final long V46_REALTIME_P95_MS = 10000L;
    public static final long V46_DECISION_GATE_TARGET_MS = 500L;
    public static final String V46_ROOT_CODE_CONTRACT = "CODE_CONTRACT_FAILURE";
    public static final String V46_ROOT_CANDIDATE_POOL = "CANDIDATE_POOL_INSUFFICIENT";
    public static final String V46_ROOT_MODEL_NO_LIFT = "MODEL_NO_LIFT";
    public static final String V46_ROOT_RANK_INVERSION = "RANK_INVERSION";
    public static final String V46_ROOT_CALIBRATION = "CALIBRATION_COMPRESSION";
    public static final String V46_ROOT_POLICY_GAP = "POLICY_THRESHOLD_GAP";
    public static final String V46_ROOT_RUNTIME = "RUNTIME_STALE";

    /* =====================================================================
     * V47 modern JDK17 cross-fitted number model and exact candidate surface.
     * Compatibility V46 fields remain database aliases only.
     * ===================================================================== */
    public static final String V47_VERSION = V49BuildManifest.MODEL_VERSION; // compatibility alias
    public static final String V48_VERSION = V49BuildManifest.MODEL_VERSION;
    public static final int V47_FULL_SCAN_CANDIDATE_COUNT = 1200;
    public static final int V47_PROBABILITY_CANDIDATE_COUNT = 60;
    public static final int V47_FORMAL_CANDIDATE_COUNT = 40;
    public static final int V47_UI_DISPLAY_CANDIDATE_COUNT = 12;
    public static final int V47_CONTROLLED_START_MIN_ISSUES = 30;
    public static final int V47_MIN_FORMAL_PER_POSITION = 2;
    public static final int V47_CROSSFIT_MAX_HISTORY = 240;
    public static final int V47_CROSSFIT_MIN_PREFIX = 24;
    public static final int V47_CROSSFIT_VALIDATION_TARGETS = 48;
    public static final int V47_CROSSFIT_MIN_VALIDATION_TARGETS = 30;
    public static final double V47_CHAMPION_MIN_TOP3_RATE = 0.305d;
    public static final double V47_CHAMPION_MIN_BRIER_GAIN = 0.0025d;
    public static final double V47_CHALLENGER_BLEND =
            0.00d; // compatibility: unproven challenger has zero live influence
    public static final double V47_CHAMPION_BLEND = 0.78d;
    public static final long V47_REALTIME_TARGET_MS = 6500L;
    public static final long V47_REALTIME_P95_MS = 10000L;
    public static final int V47_MAX_ANALYSIS_LAG_ISSUES = 1;
    public static final String V47_ROOT_SURFACE = "SURFACE_CONTRACT_BROKEN";
    public static final String V47_ROOT_STARTUP = "STARTUP_CONTEXT_LOST";
    public static final String V47_ROOT_OOS_WRITE = "OOS_WRITE_FAILURE";
    public static final String V47_ROOT_MODEL = "MODEL_BELOW_BASELINE";
    public static final String V47_ROOT_FEATURE = "FEATURE_FAMILY_NEGATIVE";
    public static final String V47_ROOT_RANK = "RANK_INVERSION";
    public static final String V47_ROOT_CALIBRATION = "CALIBRATION_OVERCONFIDENT";
    public static final String V47_ROOT_VERSION = "VERSION_SAMPLE_CONTAMINATION";
    public static final String V47_ROOT_RUNTIME = "RUNTIME_STALE";
    public static final String V47_ROOT_POLICY = "POLICY_THRESHOLD_GAP";

    /* V48 point-in-time single-action Champion contract. */
    public static final int V48_FULL_SCAN_CANDIDATE_COUNT = 1200;
    public static final int V48_PROBABILITY_CANDIDATE_COUNT = 60;
    public static final int V48_FORMAL_CANDIDATE_COUNT = 40;
    public static final int V48_UI_DISPLAY_CANDIDATE_COUNT = 12;
    public static final int V48_CONTROLLED_START_MIN_ISSUES = 30;
    public static final int V48_MIN_FORMAL_PER_POSITION = 2;
    public static final int V48_FEATURE_MAX_HISTORY = 360;
    public static final int V48_POINT_IN_TIME_MIN_PREFIX = 60;
    public static final int V48_CHAMPION_MAX_HISTORY = 360;
    public static final int V48_CHAMPION_VALIDATION_TARGETS = 120;
    public static final int V48_CHAMPION_MIN_ACTION_SAMPLE = 100;
    public static final double V48_CHAMPION_MIN_ACTION_RATE = 0.370d;
    public static final double V48_CHAMPION_MIN_BLOCK_RATE = 0.350d;
    public static final long V48_MAX_FETCH_AGE_MS = 90L * 1000L;
    public static final long V48_MAX_ANALYSIS_AGE_MS = 90L * 1000L;
    public static final int V48_MAX_ANALYSIS_LAG_ISSUES = 1;
    public static final String V48_ROOT_IDENTITY = "BUILD_IDENTITY_CONTRACT_FAILURE";
    public static final String V48_ROOT_DIAGNOSIS = "DIAGNOSIS_VERSION_MISMATCH";
    public static final String V48_ROOT_SURFACE = "SURFACE_CONTRACT_BROKEN";
    public static final String V48_ROOT_OOS_WRITE = "OOS_WRITE_FAILURE";
    public static final String V48_ROOT_MODEL = "MODEL_BELOW_BASELINE";
    public static final String V48_ROOT_RANK = "RANK_INVERSION";
    public static final String V48_ROOT_CALIBRATION = "CALIBRATION_SAMPLE_CORRELATION";
    public static final String V48_ROOT_RUNTIME = "RUNTIME_STALE";
    public static final String V48_ROOT_CHALLENGER = "CHALLENGER_LEAKED_INTO_CHAMPION";
    public static final String V48_ROOT_TARGET = "MODEL_TARGET_MISMATCH";

    /* STARCORE_TIANSHU_B01_MATERIALIZED_20260805 */
    public static final class TianshuB01 {
        private TianshuB01() {}

        public static final String CODE_NAME = "STARCORE_TIANSHU";
        public static final String BATCH_ID = "B01";
        public static final String GAME_CODE = "Binance1-PK10";
        public static final String PRIMARY_ENDPOINT = "https://api.duoqu.online/data";
        public static final String BACKUP_ENDPOINT = "https://api1.duoqu.online/data";
        public static final String DATA_PATH = "/data";
        public static final String DATABASE_NAME = "binance_k3_nova_one_pro_v57.db";
        public static final int LATEST_BUDGET_PER_MINUTE = 45;
        public static final int DATE_BUDGET_PER_MINUTE = 4;
        public static final int PROVIDER_LATEST_LIMIT_PER_MINUTE = 50;
        public static final int PROVIDER_DATE_LIMIT_PER_MINUTE = 5;
        public static final long CONNECT_TIMEOUT_MS = 2500L;
        public static final long READ_TIMEOUT_MS = 3500L;
        public static final long REALTIME_REDLINE_MS = 5000L;
        public static final boolean SHADOW_ONLY = true;
        public static final boolean MODEL_PROVEN = false;
        public static final boolean LEGACY_RUNTIME_ENABLED = false;
        public static final boolean REQUIRE_GAME_ISOLATION = true;
        public static final boolean REQUIRE_PIT = true;
        public static final boolean REQUIRE_UNIQUE_OWNER = true;

        public static final class RuleSpec {
            public final String id;
            public final String field;
            public final String group;
            public final boolean mandatory;

            RuleSpec(String id, String field, String group, boolean mandatory) {
                this.id = id;
                this.field = field;
                this.group = group;
                this.mandatory = mandatory;
            }
        }

        public static java.util.ArrayList<RuleSpec> rules() {
            java.util.ArrayList<RuleSpec> rules = new java.util.ArrayList<RuleSpec>();
            rules.add(new RuleSpec("TIANSHU-B01-CONFIG-001", "GAME_CODE", "GROUP_01", true));
            rules.add(new RuleSpec("TIANSHU-B01-CONFIG-002", "PRIMARY_ENDPOINT", "GROUP_01", true));
            rules.add(new RuleSpec("TIANSHU-B01-CONFIG-003", "BACKUP_ENDPOINT", "GROUP_01", true));
            rules.add(new RuleSpec("TIANSHU-B01-CONFIG-004", "DATA_PATH", "GROUP_01", true));
            rules.add(new RuleSpec("TIANSHU-B01-CONFIG-005", "LATEST_BUDGET", "GROUP_01", true));
            rules.add(new RuleSpec("TIANSHU-B01-CONFIG-006", "DATE_BUDGET", "GROUP_01", true));
            rules.add(new RuleSpec("TIANSHU-B01-CONFIG-007", "DB_ISOLATION", "GROUP_01", true));
            rules.add(new RuleSpec("TIANSHU-B01-CONFIG-008", "SHADOW_ONLY", "GROUP_01", true));
            rules.add(new RuleSpec("TIANSHU-B01-CONFIG-009", "SINGLE_POSITION", "GROUP_01", true));
            rules.add(new RuleSpec("TIANSHU-B01-CONFIG-010", "FIXED_K3", "GROUP_01", true));
            rules.add(new RuleSpec("TIANSHU-B01-CONFIG-011", "ONE_REAL_BET", "GROUP_01", true));
            rules.add(
                    new RuleSpec("TIANSHU-B01-CONFIG-012", "NO_FREQUENCY_DEBT", "GROUP_01", true));
            rules.add(new RuleSpec("TIANSHU-B01-CONFIG-013", "NO_MARTINGALE", "GROUP_01", true));
            rules.add(
                    new RuleSpec("TIANSHU-B01-CONFIG-014", "NO_MULTI_POSITION", "GROUP_01", true));
            rules.add(new RuleSpec("TIANSHU-B01-CONFIG-015", "KEY_REDACTION", "GROUP_01", true));
            rules.add(new RuleSpec("TIANSHU-B01-CONFIG-016", "PIT_REQUIRED", "GROUP_01", true));
            rules.add(new RuleSpec("TIANSHU-B01-CONFIG-017", "MODEL_NOT_PROVEN", "GROUP_01", true));
            rules.add(new RuleSpec("TIANSHU-B01-CONFIG-018", "OWNER_UNIQUE", "GROUP_01", true));
            rules.add(new RuleSpec("TIANSHU-B01-CONFIG-019", "PROVIDER_TIME", "GROUP_01", true));
            rules.add(new RuleSpec("TIANSHU-B01-CONFIG-020", "RAW_ARCHIVE", "GROUP_01", true));
            rules.add(new RuleSpec("TIANSHU-B01-CONFIG-021", "GAME_CODE", "GROUP_02", true));
            rules.add(new RuleSpec("TIANSHU-B01-CONFIG-022", "PRIMARY_ENDPOINT", "GROUP_02", true));
            rules.add(new RuleSpec("TIANSHU-B01-CONFIG-023", "BACKUP_ENDPOINT", "GROUP_02", true));
            rules.add(new RuleSpec("TIANSHU-B01-CONFIG-024", "DATA_PATH", "GROUP_02", true));
            rules.add(new RuleSpec("TIANSHU-B01-CONFIG-025", "LATEST_BUDGET", "GROUP_02", true));
            rules.add(new RuleSpec("TIANSHU-B01-CONFIG-026", "DATE_BUDGET", "GROUP_02", true));
            rules.add(new RuleSpec("TIANSHU-B01-CONFIG-027", "DB_ISOLATION", "GROUP_02", true));
            rules.add(new RuleSpec("TIANSHU-B01-CONFIG-028", "SHADOW_ONLY", "GROUP_02", true));
            rules.add(new RuleSpec("TIANSHU-B01-CONFIG-029", "SINGLE_POSITION", "GROUP_02", true));
            rules.add(new RuleSpec("TIANSHU-B01-CONFIG-030", "FIXED_K3", "GROUP_02", true));
            rules.add(new RuleSpec("TIANSHU-B01-CONFIG-031", "ONE_REAL_BET", "GROUP_02", true));
            rules.add(
                    new RuleSpec("TIANSHU-B01-CONFIG-032", "NO_FREQUENCY_DEBT", "GROUP_02", true));
            rules.add(new RuleSpec("TIANSHU-B01-CONFIG-033", "NO_MARTINGALE", "GROUP_02", true));
            rules.add(
                    new RuleSpec("TIANSHU-B01-CONFIG-034", "NO_MULTI_POSITION", "GROUP_02", true));
            rules.add(new RuleSpec("TIANSHU-B01-CONFIG-035", "KEY_REDACTION", "GROUP_02", true));
            rules.add(new RuleSpec("TIANSHU-B01-CONFIG-036", "PIT_REQUIRED", "GROUP_02", true));
            rules.add(new RuleSpec("TIANSHU-B01-CONFIG-037", "MODEL_NOT_PROVEN", "GROUP_02", true));
            rules.add(new RuleSpec("TIANSHU-B01-CONFIG-038", "OWNER_UNIQUE", "GROUP_02", true));
            rules.add(new RuleSpec("TIANSHU-B01-CONFIG-039", "PROVIDER_TIME", "GROUP_02", true));
            rules.add(new RuleSpec("TIANSHU-B01-CONFIG-040", "RAW_ARCHIVE", "GROUP_02", true));
            rules.add(new RuleSpec("TIANSHU-B01-CONFIG-041", "GAME_CODE", "GROUP_03", true));
            rules.add(new RuleSpec("TIANSHU-B01-CONFIG-042", "PRIMARY_ENDPOINT", "GROUP_03", true));
            rules.add(new RuleSpec("TIANSHU-B01-CONFIG-043", "BACKUP_ENDPOINT", "GROUP_03", true));
            rules.add(new RuleSpec("TIANSHU-B01-CONFIG-044", "DATA_PATH", "GROUP_03", true));
            rules.add(new RuleSpec("TIANSHU-B01-CONFIG-045", "LATEST_BUDGET", "GROUP_03", true));
            rules.add(new RuleSpec("TIANSHU-B01-CONFIG-046", "DATE_BUDGET", "GROUP_03", true));
            rules.add(new RuleSpec("TIANSHU-B01-CONFIG-047", "DB_ISOLATION", "GROUP_03", true));
            rules.add(new RuleSpec("TIANSHU-B01-CONFIG-048", "SHADOW_ONLY", "GROUP_03", true));
            rules.add(new RuleSpec("TIANSHU-B01-CONFIG-049", "SINGLE_POSITION", "GROUP_03", true));
            rules.add(new RuleSpec("TIANSHU-B01-CONFIG-050", "FIXED_K3", "GROUP_03", true));
            rules.add(new RuleSpec("TIANSHU-B01-CONFIG-051", "ONE_REAL_BET", "GROUP_03", true));
            rules.add(
                    new RuleSpec("TIANSHU-B01-CONFIG-052", "NO_FREQUENCY_DEBT", "GROUP_03", true));
            rules.add(new RuleSpec("TIANSHU-B01-CONFIG-053", "NO_MARTINGALE", "GROUP_03", true));
            rules.add(
                    new RuleSpec("TIANSHU-B01-CONFIG-054", "NO_MULTI_POSITION", "GROUP_03", true));
            rules.add(new RuleSpec("TIANSHU-B01-CONFIG-055", "KEY_REDACTION", "GROUP_03", true));
            rules.add(new RuleSpec("TIANSHU-B01-CONFIG-056", "PIT_REQUIRED", "GROUP_03", true));
            rules.add(new RuleSpec("TIANSHU-B01-CONFIG-057", "MODEL_NOT_PROVEN", "GROUP_03", true));
            rules.add(new RuleSpec("TIANSHU-B01-CONFIG-058", "OWNER_UNIQUE", "GROUP_03", true));
            rules.add(new RuleSpec("TIANSHU-B01-CONFIG-059", "PROVIDER_TIME", "GROUP_03", true));
            rules.add(new RuleSpec("TIANSHU-B01-CONFIG-060", "RAW_ARCHIVE", "GROUP_03", true));
            rules.add(new RuleSpec("TIANSHU-B01-CONFIG-061", "GAME_CODE", "GROUP_04", true));
            rules.add(new RuleSpec("TIANSHU-B01-CONFIG-062", "PRIMARY_ENDPOINT", "GROUP_04", true));
            rules.add(new RuleSpec("TIANSHU-B01-CONFIG-063", "BACKUP_ENDPOINT", "GROUP_04", true));
            rules.add(new RuleSpec("TIANSHU-B01-CONFIG-064", "DATA_PATH", "GROUP_04", true));
            rules.add(new RuleSpec("TIANSHU-B01-CONFIG-065", "LATEST_BUDGET", "GROUP_04", true));
            rules.add(new RuleSpec("TIANSHU-B01-CONFIG-066", "DATE_BUDGET", "GROUP_04", true));
            rules.add(new RuleSpec("TIANSHU-B01-CONFIG-067", "DB_ISOLATION", "GROUP_04", true));
            rules.add(new RuleSpec("TIANSHU-B01-CONFIG-068", "SHADOW_ONLY", "GROUP_04", true));
            rules.add(new RuleSpec("TIANSHU-B01-CONFIG-069", "SINGLE_POSITION", "GROUP_04", true));
            rules.add(new RuleSpec("TIANSHU-B01-CONFIG-070", "FIXED_K3", "GROUP_04", true));
            rules.add(new RuleSpec("TIANSHU-B01-CONFIG-071", "ONE_REAL_BET", "GROUP_04", true));
            rules.add(
                    new RuleSpec("TIANSHU-B01-CONFIG-072", "NO_FREQUENCY_DEBT", "GROUP_04", true));
            rules.add(new RuleSpec("TIANSHU-B01-CONFIG-073", "NO_MARTINGALE", "GROUP_04", true));
            rules.add(
                    new RuleSpec("TIANSHU-B01-CONFIG-074", "NO_MULTI_POSITION", "GROUP_04", true));
            rules.add(new RuleSpec("TIANSHU-B01-CONFIG-075", "KEY_REDACTION", "GROUP_04", true));
            rules.add(new RuleSpec("TIANSHU-B01-CONFIG-076", "PIT_REQUIRED", "GROUP_04", true));
            rules.add(new RuleSpec("TIANSHU-B01-CONFIG-077", "MODEL_NOT_PROVEN", "GROUP_04", true));
            rules.add(new RuleSpec("TIANSHU-B01-CONFIG-078", "OWNER_UNIQUE", "GROUP_04", true));
            rules.add(new RuleSpec("TIANSHU-B01-CONFIG-079", "PROVIDER_TIME", "GROUP_04", true));
            rules.add(new RuleSpec("TIANSHU-B01-CONFIG-080", "RAW_ARCHIVE", "GROUP_04", true));
            rules.add(new RuleSpec("TIANSHU-B01-CONFIG-081", "GAME_CODE", "GROUP_05", true));
            rules.add(new RuleSpec("TIANSHU-B01-CONFIG-082", "PRIMARY_ENDPOINT", "GROUP_05", true));
            return rules;
        }

        public static String deterministicContractText() {
            StringBuilder sb = new StringBuilder();
            java.util.ArrayList<RuleSpec> items = rules();
            for (int i = 0; i < items.size(); i++) {
                RuleSpec item = items.get(i);
                if (i > 0) sb.append('|');
                sb.append(item.id)
                        .append(':')
                        .append(item.field)
                        .append(':')
                        .append(item.group)
                        .append(':')
                        .append(item.mandatory ? '1' : '0');
            }
            return sb.toString();
        }

        public static boolean isProductionTransportConfigValid() {
            return GAME_CODE.equals(LOTTERY_CODE)
                    && API_PRIMARY_BASE.startsWith("https://")
                    && API_BACKUP_BASE.startsWith("https://")
                    && DATA_PATH.equals(API_PATH_DATA)
                    && DATA_PATH.equals(API_PATH_LATEST)
                    && DATA_PATH.equals(API_PATH_HISTORY)
                    && DATABASE_NAME.equals(DB_NAME)
                    && LATEST_BUDGET_PER_MINUTE < PROVIDER_LATEST_LIMIT_PER_MINUTE
                    && DATE_BUDGET_PER_MINUTE < PROVIDER_DATE_LIMIT_PER_MINUTE
                    && FIXED_K == 3
                    && !ALLOW_K4
                    && !ALLOW_DYNAMIC_K
                    && !ALLOW_MULTI_POSITION_BET
                    && !ALLOW_MARTINGALE;
        }

        public static String status() {
            if (!isProductionTransportConfigValid()) return "B01_CONFIG_INVALID";
            if (MODEL_PROVEN) return "B01_INVALID_MODEL_FLAG";
            if (!SHADOW_ONLY) return "B01_INVALID_RELEASE_FLAG";
            return "B01_CONFIG_READY_SHADOW_ONLY";
        }
    }
}
