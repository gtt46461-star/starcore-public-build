package com.ethk3.novaonepro;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Process;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/*
 * AutoFetchService
 *
 * V20 满级诊断 + 智能审判闭环调度层满血版
 *
 * 本层只负责：
 * 1. 抓取开奖、保存开奖、广播状态、倒计时、回补历史；
 * 2. 触发严格单位置结算；
 * 3. 调度实时正式分析；
 * 4. BET保存后立即创建真实Trade；
 * 5. 调度后台小批量学习、教师矩阵、V20因链画像、回测；
 * 6. UI状态、通知栏、运行状态写入。
 *
 * 本层绝对不做：
 * 1. 不救援BET；
 * 2. 不快裁BET；
 * 3. 不绕过 DecisionGateEngine；
 * 4. 不把 SHADOW/HOLD 写成真实BET；
 * 5. 不把后台教师矩阵/因链反推污染真实胜率、真实盈亏、真实校准；
 * 6. 不用旧预测顶替当前目标期。
 *
 * 本次满血修复重点：
 * 1. normalizeDecisionScope 不再把已结算结果重置为 WAIT；
 * 2. 深层原因后补不再污染 settled/result；
 * 3. 看门狗确认 BET 时只确认 Trade 存在，不回写旧结算状态；
 * 4. 结算入口增强：优先调用严格单位置结算入口；
 * 5. V20后台画像继续让路实时，不写真实盈亏/校准；
 * 6. 分析线程换代继续禁止旧目标期落库；
 * 7. 教师号码矩阵不再只显示后台摘要，必须桥接到实时候选字段；
 * 8. 概率精算后、动态门后、最终DecisionGate前强制三次桥接 teacherNumber/teacherMatrix；
 * 9. 如果 TeacherNumberMatrixEngine 反射入口没写字段，本层用 clean 历史现场兜底写入候选字段；
 * 10. 所有 UI/摘要读取 teacherMatrix 字段时同步兼容 teacherNumber 字段。
 * 11. 运行补丁：结算优先于分析，抓到新开奖先补建 Trade、补结算所有已开奖 WAIT BET。
 * 12. 运行补丁：实时出号不快裁、不乱出号，只跳过重复桥接和后置长原因，完整 DecisionGate 仍是唯一 BET 出口。
 * 13. 诊断补丁：结算/空闲/决策后低优先级刷新真实BET复盘诊断，诊断只读不改策略。
 * 14. 闭环补丁：服务启动/开奖/结算/Decision保存/SHADOW保存/空闲/手动刷新后自动刷新满级诊断闭环。
 * 15. 闭环补丁：模块评分、SHADOW晋级、动态冻结、智能审判、修复计划全部低优先级调度，让路实时出号。
 * 16. 闭环红线：AutoFetchService 只调度，不直接BET，不结算，不创建Trade，不保存Decision，不绕过DecisionGateEngine。
 *
 * Java7 / AIDE / 原生 Android 兼容：
 * - 无 lambda
 * - 无 stream
 * - 无 AndroidX
 * - 无第三方库
 */
public final class AutoFetchService extends Service {
    private static volatile boolean lifecycleActiveR82;
    private static volatile AutoFetchService activeInstanceR82;
    private static volatile Thread[] teardownThreadsR82 = new Thread[0];
    /** Reads existing live owners and validated provider facts; performs no SQL or side effects. */
    public static String runtimeHealthBlockerR82(String baseIssue,String targetIssue) {
        AutoFetchService service=activeInstanceR82;
        if(service==null||!lifecycleActiveR82||!service.running||service.userRequestedStopV5||!service.runtimeInitialized)
            return "RUNTIME_SERVICE_NOT_RUNNING";
        if(service.fetchThread==null||!service.fetchThread.isAlive())return "RUNTIME_FETCH_OWNER_NOT_ALIVE";
        if(service.analysisThread==null||!service.analysisThread.isAlive())return "RUNTIME_ANALYSIS_OWNER_NOT_ALIVE";
        if(!TargetIssueResolver.exact(baseIssue,targetIssue).valid)return "RUNTIME_TARGET_IDENTITY_INVALID";
        if(TimeSourceR811.detectSkew().excessive)return "RUNTIME_CLOCK_SKEW";
        ProviderFreshnessGateR3611.Candidate selected=ProviderFreshnessGateR3611.snapshot().selected;
        if(selected==null||!baseIssue.equals(selected.issue)||!"FRESH".equals(selected.code))return "RUNTIME_PROVIDER_BASE_NOT_FRESH";
        long now=System.currentTimeMillis(),age=now-selected.observedAt;
        if(selected.observedAt<=0L||age<0L||selected.ageMs<0L||age>ProviderFreshnessGateR3611.STALE_MS
                ||selected.ageMs>ProviderFreshnessGateR3611.STALE_MS-age)return "RUNTIME_PROVIDER_OBSERVATION_STALE";
        return "";
    }
    public static boolean isLifecycleActiveR82() {
        if (lifecycleActiveR82) return true;
        // quitSafely() is asynchronous. Maintenance must wait for those workers too.
        for (Thread thread : teardownThreadsR82) if (thread != null && thread.isAlive()) return true;
        return false;
    }

    public static final String DEADLOCK_RECOVERY_BUILD =
            "V47_CROSSFIT_NUMBER_CHAMPION_FINAL40_FULL_DIAGNOSIS_20260721_47072147";

    private static final String ACTION_BACKTEST_FAST_NOW =
            AppConfig.PACKAGE_NAME + ".ACTION_BACKTEST_FAST_NOW";
    private static final String ACTION_BACKTEST_NORMAL_NOW =
            AppConfig.PACKAGE_NAME + ".ACTION_BACKTEST_NORMAL_NOW";
    private static final String ACTION_BACKTEST_FULL_NOW =
            AppConfig.PACKAGE_NAME + ".ACTION_BACKTEST_FULL_NOW";
    private static final String BACKGROUND_EVIDENCE_AUTOFETCH_VERSION =
            AppConfig.ACTIVE_QUALITY_BUILD_VERSION;
    private volatile boolean userRequestedStopV5;
    private volatile boolean backgroundEvidencePumpPostedV6;
    private volatile long lastBackgroundEvidencePumpAtV6;
    private volatile long lastBackgroundSliceAtV5;
    private volatile long lastLegacyLearningAtV23;
    private volatile long lastServiceHeartbeatAtV5;
    private volatile long lastAlwaysOnLoopRepairAtV17;
    private volatile long lastLearningPumpPulseAtV17;
    private volatile String lastOomIssueB07 = "";
    private volatile int oomRetryCountB07;
    private volatile long lastRuntimeGuardAtB07;
    private volatile boolean b07StartupBackfillPosted;
    private volatile int runtimeInitRetryCountR323;
    private volatile boolean startWorkDispatchQueuedR3610;
    private volatile Thread serviceMainThreadR3610;
    private volatile long analysisOomBackoffUntilR3610;
    private Handler startupHeartbeatHandlerR323;

    private ServiceHeartbeatStore heartbeatStoreV8;
    private volatile boolean reservedTeacherSlicePostedV8;
    private volatile String lastReservedTeacherSliceTargetV8 = "";
    private volatile long analysisRecordedCostSumV8;
    private volatile String analysisRecordedTargetV8 = "";

    private static final long FETCH_IDLE_DELAY_MS = 1500L;
    private static final long FETCH_REALTIME_DELAY_MS = 420L;

    private static final long ANALYSIS_RETRY_DELAY_MS = 260L;
    private static final long ANALYSIS_WATCHDOG_INTERVAL_MS = 1000L;
    private static final long R816_ANALYSIS_RECONCILE_INTERVAL_MS = 3000L;
    private static final long R816_SETTLEMENT_WATCHDOG_INTERVAL_MS = 1000L;
    private static final long R816_PRODUCTION_MAINTENANCE_INTERVAL_MS = 10000L;
    private static final long R816_RESEARCH_TICK_INTERVAL_MS = 5000L;
    private static final long R817_RESEARCH_SCHEMA_INTERVAL_MS = 30000L;
    private static final long ANALYSIS_WAITING_STALE_MS = 12000L;
    private static final long ANALYSIS_BUSY_NOTICE_MS = 1200L;
    private static final long ANALYSIS_BUSY_WARN_MS = 6500L;
    private static final long ANALYSIS_BUSY_REPLACE_MS = 9000L;
    private static final long ANALYSIS_BUSY_HARD_RESET_MS = 10000L;
    private static final long ANALYSIS_DIRECT_TAKEOVER_MIN_INTERVAL_MS = 1800L;

    private static final int REALTIME_ANALYSIS_HISTORY_LIMIT = 96;
    private static final int REALTIME_DB_HISTORY_LOAD_LIMIT = 360;

    private static final int REALTIME_FORMAL_SYNC_CANDIDATE_CAP =
            AppConfig.V47_PROBABILITY_CANDIDATE_COUNT;
    private static final int REALTIME_BACKGROUND_COMPLETION_CANDIDATE_CAP =
            AppConfig.V47_PROBABILITY_CANDIDATE_COUNT;
    private static final int REALTIME_PROBABILITY_ENRICH_CAP =
            AppConfig.V47_PROBABILITY_CANDIDATE_COUNT;
    /* Keep all 60 probability candidates through final evidence.  The only final rank then
     * selects 40 formal contracts.  UI display count is independent. */
    private static final int REALTIME_DECISION_CANDIDATE_CAP =
            AppConfig.V47_PROBABILITY_CANDIDATE_COUNT;
    private static final int REALTIME_FINAL_DISPLAY_TOP = AppConfig.V47_UI_DISPLAY_CANDIDATE_COUNT;

    private static final int SAFE_FULL_HISTORY_LIMIT = 900;
    private static final int SAFE_FULL_W50 = 50;
    private static final int SAFE_FULL_W120 = 120;
    private static final int SAFE_FULL_W240 = 240;
    private static final int SAFE_FULL_W500 = 500;

    private static final int V20_BACKGROUND_HISTORY_LIMIT = 260;
    private static final int V20_BACKGROUND_CANDIDATE_LIMIT =
            REALTIME_BACKGROUND_COMPLETION_CANDIDATE_CAP;
    private static final int V20_BACKGROUND_SUMMARY_LIMIT = 260;

    private static final long BACKFILL_INTERVAL_MS = AppConfig.BACKFILL_NORMAL_INTERVAL_MS;
    private static final long BACKFILL_FAST_RETRY_MS = AppConfig.BACKFILL_FAST_RETRY_MS;
    private static final long REALTIME_FORCE_BACKFILL_MIN_INTERVAL_MS = 2200L;
    private static final long QUICK_HISTORY_SYNC_MIN_INTERVAL_MS = 1800L;
    private static final long R3611_DATE_FRESHNESS_RECOVERY_MIN_INTERVAL_MS = 15000L;
    /* R3.6.7: a one-minute game must not remain on the same draw for multiple periods even if
     * CountdownClock drift incorrectly says we are outside the realtime window. */
    private static final long STALE_LATEST_FORCE_FETCH_MS = 95000L;
    private static final long STALE_LATEST_HISTORY_RECOVERY_MIN_INTERVAL_MS = 8000L;

    private static final long AUTO_FAST_BACKTEST_INTERVAL_MS = AppConfig.BACKTEST_FAST_INTERVAL_MS;
    private static final long AUTO_NORMAL_BACKTEST_INTERVAL_MS =
            AppConfig.BACKTEST_NORMAL_INTERVAL_MS;
    private static final long AUTO_DEEP_BACKTEST_INTERVAL_MS =
            AppConfig.BACKTEST_FULL_MIN_INTERVAL_MS;
    private static final long BACKTEST_MIN_IDLE_BEFORE_OPEN_MS =
            AppConfig.BACKTEST_IDLE_AFTER_REALTIME_MS;
    private static final long BACKTEST_HEARTBEAT_INTERVAL_MS = 3500L;
    private static final long BACKTEST_REALTIME_WAIT_SLICE_MS = 250L;
    private static final long BACKTEST_REALTIME_WAIT_MAX_MS = 5200L;
    private static final long AUTO_BACKTEST_DELAY_AFTER_DRAW_MS = 90000L;
    private static final long AUTO_BACKTEST_MIN_TRY_INTERVAL_MS = 180000L;

    private static final long AUTO_LEARNING_MIN_INTERVAL_MS = 10000L;
    private static final long AUTO_LEARNING_AFTER_NEW_DRAW_DELAY_MS = 1800L;
    private static final long AUTO_LEARNING_IDLE_DELAY_MS = 10000L;
    private static final long V23_LEGACY_BACKGROUND_INTERVAL_MS = 120000L;

    private static final int AUTO_LEARNING_REALTIME_BATCH = 1;

    private static final long TEACHER_MATRIX_MIN_INTERVAL_MS = 120000L;
    private static final long TEACHER_MATRIX_AFTER_ANALYSIS_DELAY_MS = 45000L;
    private static final long TEACHER_MATRIX_IDLE_DELAY_MS = 90000L;

    private static final long V20_CAUSE_CHAIN_MIN_INTERVAL_MS = 120000L;
    private static final long V20_CAUSE_CHAIN_AFTER_ANALYSIS_DELAY_MS = 45000L;
    private static final long V20_CAUSE_CHAIN_IDLE_DELAY_MS = 90000L;

    private static final long REALTIME_PRIORITY_HOLD_MS = 18000L;
    private static final long STATE_BROADCAST_MIN_INTERVAL_MS = 900L;
    private static final long RUNTIME_WRITE_MIN_INTERVAL_MS = 2000L;
    private static final long TICK_BROADCAST_MIN_INTERVAL_MS = 900L;
    private static final long NOTIFICATION_MIN_INTERVAL_MS = 10000L;

    private static final long DECISION_FAST_STATE_DELAY_MS = 1200L;
    private static final long DECISION_DEEP_FINALIZE_DELAY_MS = 1500L;
    private static final long ANALYSIS_STATE_MIN_WRITE_MS = 850L;
    private static final String V20_RUNTIME_PATCH_VERSION = AppConfig.ACTIVE_QUALITY_BUILD_VERSION;
    private static final String V20_REAL_BET_DIAG_PATCH_VERSION =
            AppConfig.ACTIVE_QUALITY_BUILD_VERSION;
    private static final String V20_FULL_DIAG_LOOP_PATCH_VERSION =
            AppConfig.ACTIVE_QUALITY_BUILD_VERSION;
    private static final String V20_FIELD_CHAIN_PATCH_VERSION =
            AppConfig.ACTIVE_QUALITY_BUILD_VERSION;
    private static final String V20_UPSTREAM_EVIDENCE_PATCH_VERSION =
            AppConfig.ACTIVE_QUALITY_BUILD_VERSION;
    private static final String V20_DYNAMIC_CAUSAL_AUTOFETCH_PATCH_VERSION =
            AppConfig.ACTIVE_QUALITY_BUILD_VERSION;
    private static final String V20_MAIN_CHAIN_AUTOFETCH_VERSION =
            AppConfig.ACTIVE_QUALITY_BUILD_VERSION;
    private static final String V20_TIERED_REALTIME_SCHEDULER_VERSION =
            "V47_CROSSFIT_NUMBER_CHAMPION_FINAL40_FULL_DIAGNOSIS_20260721_47072147";
    private static final String REALTIME_VALID_DOMAIN_AUTOFETCH_VERSION =
            "V47_CROSSFIT_NUMBER_CHAMPION_FINAL40_FULL_DIAGNOSIS_20260721_47072147";
    private volatile String teacherBridgeTargetIssueV3 = "";
    private volatile boolean teacherBridgeCompletedV3 = false;
    private static final String LINK_MODIFICATION_V2_AUTOFETCH_VERSION =
            AppConfig.LINK_MODIFICATION_V2_VERSION;
    private static final boolean REALTIME_FORCE_DEFER_REFLECT_HEAVY = true;
    private static final boolean REALTIME_RUN_ADAPTIVE_REFLECTION = false;
    private static final long REALTIME_HOT_MAX_BLOCKING_MS = 6500L;
    private static final int REALTIME_HOT_HISTORY_LIMIT = AppConfig.HOT_REALTIME_WINDOW;
    private static final int WARM_PROFILE_HISTORY_LIMIT = AppConfig.WARM_PROFILE_WINDOW;
    private static final int COLD_PROFILE_HISTORY_LIMIT = AppConfig.COLD_PROFILE_WINDOW;
    private static final int DEEP_PROFILE_HISTORY_LIMIT = AppConfig.DEEP_PROFILE_WINDOW;
    private static final long WARM_PROFILE_REFRESH_MS = AppConfig.WARM_REFRESH_MS;
    private static final long COLD_PROFILE_REFRESH_MS = AppConfig.COLD_REFRESH_MS;
    private static final long DEEP_PROFILE_REFRESH_MS = AppConfig.DEEP_REFRESH_MS;
    private static final long WARM_PROFILE_SOFT_STALE_MS = AppConfig.WARM_PROFILE_SOFT_STALE_MS;
    private static final long WARM_PROFILE_HARD_STALE_MS = AppConfig.WARM_PROFILE_HARD_STALE_MS;
    private static final long COLD_PROFILE_SOFT_STALE_MS = AppConfig.COLD_PROFILE_SOFT_STALE_MS;
    private static final long COLD_PROFILE_HARD_STALE_MS = AppConfig.COLD_PROFILE_HARD_STALE_MS;
    private static final int WARM_PROFILE_CANDIDATE_LIMIT = AppConfig.WARM_PROFILE_CANDIDATE_LIMIT;
    private static final int COLD_PROFILE_CANDIDATE_LIMIT = AppConfig.COLD_PROFILE_CANDIDATE_LIMIT;
    private static final int DEEP_PROFILE_CANDIDATE_LIMIT = AppConfig.DEEP_PROFILE_CANDIDATE_LIMIT;
    private static final String PROFILE_TIER_HOT = "HOT_REALTIME";
    private static final String PROFILE_TIER_WARM = "WARM_1440";
    private static final String PROFILE_TIER_COLD = "COLD_5000";
    private static final String PROFILE_TIER_DEEP = "DEEP_10000";
    private static final String PROFILE_TIER_NONE = "NOT_DUE";

    private static final int DYNAMIC_CAUSAL_REALTIME_CANDIDATE_CAP =
            AppConfig.V47_PROBABILITY_CANDIDATE_COUNT;
    private static final long DYNAMIC_CAUSAL_REALTIME_SOFT_BUDGET_MS = 700L;
    private static final long DYNAMIC_CAUSAL_REALTIME_HARD_BUDGET_MS = 2200L;
    private static final long SHADOW_PROMOTION_REALTIME_REFRESH_MIN_INTERVAL_MS = 180000L;
    private static final long REALTIME_FORMAL_DECISION_TARGET_MS = AppConfig.R35_REALTIME_TARGET_MS;
    private static final long REALTIME_HEAVY_STAGE_DEFER_MS = 1800L;
    private static final long REALTIME_FINAL_GATE_PROTECT_MS = 3600L;
    private static final long REALTIME_DYNAMIC_MINI_CORE_BUDGET_MS = 2200L;
    private static final double REALTIME_MINI_CORE_READY_P = 0.370d;
    private static final double REALTIME_MINI_CORE_EDGE_FLOOR = 0.000d;
    private static final double REALTIME_MINI_CORE_COVER_READY = 0.300d;
    private static final double REALTIME_MINI_CORE_NEED_READY = 0.500d;
    private static final double REALTIME_MINI_CORE_THIRD_READY = 0.100d;
    private static final double REALTIME_MINI_CORE_COUNTER_READY = 0.620d;
    private static final long REALTIME_REPOST_STUCK_MS = 15000L;
    private static final long ANALYSIS_RESCUE_TAKEOVER_MS = 15000L;
    private static final long ANALYSIS_RESCUE_MIN_INTERVAL_MS = 60000L;
    private static volatile boolean analysisRescueBusy = false;
    private static volatile long analysisRescueStartedAt = 0L;
    private static volatile long analysisRescueLastStartAt = 0L;
    private static volatile String analysisRescueBaseIssue = "";
    private static final boolean ANALYSIS_OBSERVE_FULL_RUN_MODE = false;
    private static final long ANALYSIS_OBSERVE_LOG_INTERVAL_MS = 10000L;
    private static volatile long analysisObserveLastLogAt = 0L;
    private static volatile long lastShadowPromotionRealtimeRefreshAt = 0L;
    private static final double UPSTREAM_BASE_K3_RATE = AppConfig.RANDOM_K3_BASELINE;
    private static final double UPSTREAM_BASE_NUMBER_RATE = 0.100d;
    private static final double UPSTREAM_SAMPLE_RISK = 0.650d;
    private static final int UPSTREAM_COVER_W32 = 32;
    private static final int UPSTREAM_COVER_W64 = 64;
    private static final int UPSTREAM_COVER_W128 = 128;
    private static final double FIELD_CHAIN_EDGE_FALSE_STRONG_LINE = 0.020d;
    private static final double FIELD_CHAIN_P_WEAK_LINE = 0.380d;
    private static final double FIELD_CHAIN_P_NO_STRONG_CAUSE_LINE = 0.415d;
    private static final double FIELD_CHAIN_K3_COVERAGE_RISK_LINE = 0.620d;
    private static final double FIELD_CHAIN_K3_COVERAGE_HARD_RISK = 0.700d;
    private static final double FIELD_CHAIN_THIRD_NUMBER_MIN = 0.088d;
    private static final long REAL_BET_DIAG_MIN_INTERVAL_MS =
            AppConfig.REAL_BET_DIAG_MIN_REFRESH_MS;
    private static final long REAL_BET_DIAG_AFTER_SETTLEMENT_DELAY_MS =
            AppConfig.DIAGNOSIS_INTERVAL_MS;
    private static final long REAL_BET_DIAG_AFTER_DECISION_DELAY_MS =
            AppConfig.DIAGNOSIS_INTERVAL_MS;
    private static final long REAL_BET_DIAG_IDLE_DELAY_MS = AppConfig.DIAGNOSIS_INTERVAL_MS;
    private static final int REAL_BET_DIAG_BET_LIMIT = 120;
    private static final int REAL_BET_DIAG_SHADOW_LIMIT = 240;
    private static final long FULL_DIAG_LOOP_MIN_INTERVAL_MS = AppConfig.FULL_DIAG_MIN_REFRESH_MS;
    private static final long FULL_DIAG_LOOP_AFTER_SETTLEMENT_DELAY_MS =
            AppConfig.DIAGNOSIS_INTERVAL_MS;
    private static final long FULL_DIAG_LOOP_AFTER_DECISION_DELAY_MS =
            AppConfig.DIAGNOSIS_INTERVAL_MS;
    private static final long FULL_DIAG_LOOP_IDLE_DELAY_MS = AppConfig.DIAGNOSIS_INTERVAL_MS;
    // A twelve-minute cold-start delay left the real-time freeze snapshot empty for hundreds of
    // issues.  Startup materialization is prompt; subsequent diagnosis cadence remains unchanged.
    private static final long FULL_DIAG_LOOP_STARTUP_DELAY_MS = 20_000L;
    private static final int FULL_DIAG_LOOP_RECENT_JUDGE_LIMIT = 80;
    private static final int SETTLEMENT_CATCHUP_OPEN_LIMIT = 80;
    private static final int SETTLEMENT_RECENT_DECISION_REPAIR_LIMIT = 80;
    private static final int REALTIME_POST_GATE_V20_CANDIDATE_CAP = REALTIME_DECISION_CANDIDATE_CAP;
    private static final int TEACHER_BRIDGE_READY_PERCENT_TO_SKIP_REDUNDANT = 60;

    private static final Object REFLECTION_NO_METHOD = new Object();

    private HandlerThread fetchThread;
    private Handler fetchHandler;
    private final Object fetchWorkerRepairLockR811 = new Object();

    /* B07 guard is independent from fetch/SQLite work so a blocked fetch cannot hide liveness. */
    private HandlerThread runtimeGuardThread;
    private Handler runtimeGuardHandler;
    private HandlerThread livenessThreadR817;
    private Handler livenessHandlerR817;
    private HandlerThread analysisWatchdogThread;
    private Handler analysisWatchdogHandler;
    private final java.util.concurrent.atomic.AtomicBoolean countdownWorkPostedR964 =
            new java.util.concurrent.atomic.AtomicBoolean(false);
    private volatile long countdownValueR964;
    private HandlerThread startupInitThread;
    private Handler startupInitHandler;
    private volatile boolean runtimeInitialized;
    private volatile boolean runtimeInitializationRunning;
    private volatile boolean runtimeInitializationCancelled;
    private volatile String runtimeInitializationError = "";
    private volatile String pendingStartAction = AppConfig.ACTION_SERVICE_START;
    private volatile int pendingStartId;

    private HandlerThread analysisThread;
    // R77: failed terminal/retry writes cannot strand an exited owner for the 45s lease.
    private final java.util.concurrent.ConcurrentHashMap<String, ExitedAnalysisReleaseR77>
            exitedAnalysisReleasesR77 = new java.util.concurrent.ConcurrentHashMap<String, ExitedAnalysisReleaseR77>();

    private Handler analysisHandler;
    private final Object analysisWorkerRepairLockR811 = new Object();

    private HandlerThread backfillThread;
    private Handler backfillHandler;

    private HandlerThread backtestThread;
    private Handler backtestHandler;

    private HandlerThread learningThread;
    private Handler learningHandler;

    /* R908: Production settlement recovery and autonomous Research must never share the old
     * learning/backfill queue.  Settlement is correctness-critical; Research is best-effort. */
    private HandlerThread settlementRecoveryThreadR908;
    private Handler settlementRecoveryHandlerR908;
    private HandlerThread countdownThreadR973; // Clock/Binder only: never SQLite or research.
    private HandlerThread researchThreadR908;
    private Handler researchHandlerR908;
    private final ResearchCaptureQueueR946 researchCaptureQueueR946 = new ResearchCaptureQueueR946();
    private final java.util.concurrent.atomic.AtomicBoolean researchTickPostedR908 =
            new java.util.concurrent.atomic.AtomicBoolean(false);
    private final java.util.concurrent.atomic.AtomicBoolean currentExactSettlementPendingR908 =
            new java.util.concurrent.atomic.AtomicBoolean(false);
    private volatile String currentExactSettlementIssueR908 = "";
    private static final long R908_SETTLEMENT_PULSE_MS = 1200L;
    // Continuity runs every pulse while expensive invention is still round-robin phased. Five
    // seconds closes settled-label/learner lag without letting Research occupy the live lanes.
    private static final long R908_RESEARCH_SELF_PULSE_MS = 5000L;

    /* V26 dedicated strict-OOS publisher.  It must not share the diagnostic/teacher queue. */
    private HandlerThread microThread;
    private Handler microHandler;


    private DbStore store;
    private ApiClient apiClient;
    private DataGuard dataGuard;
    private CountdownClock countdownClock;

    private WindowFeatureEngine windowFeatureEngine;
    private ShapeStateEngine shapeStateEngine;
    private TrendStateMachine trendStateMachine;
    private MarkovEngine markovEngine;
    private BreakRecoveryEngine breakRecoveryEngine;
    private ComboScannerEngine comboScannerEngine;
    private ProbabilityEngine probabilityEngine;
    private CalibrationEngine calibrationEngine;
    private DynamicGateEngine dynamicGateEngine;
    private DecisionGateEngine decisionGateEngine;
    private V50RuntimeOrchestrator v50RuntimeOrchestrator;
    private SettlementEngine settlementEngine;
    private final java.util.concurrent.atomic.AtomicBoolean settlementMaintenanceRunningR3611 =
            new java.util.concurrent.atomic.AtomicBoolean(false);
    private final java.util.concurrent.atomic.AtomicBoolean settlementMaintenancePostedR817 =
            new java.util.concurrent.atomic.AtomicBoolean(false);
    private volatile long lastDateFreshnessRecoveryAtR3611;
    private AutonomousResearchEvolutionEngine researchEvolutionR811;
    private volatile ResearchExpertPredrawSnapshotR811.Snapshot pendingExpertSnapshotR811;
    private WalkForwardBacktestEngine backtestEngine;
    private ReasonEngine reasonEngine;
    private RealBetDiagnosisEngine realBetDiagnosisEngine;
    private LearningEngine learningEngine;
    private HistoricalStatePriorEngine historicalStatePriorEngine;
    private OnlineEvidenceForestEngine onlineEvidenceForestEngine;
    private HierarchicalStateTaxonomyEngine hierarchicalStateTaxonomyEngine;
    private AdaptiveExpertConsensusEngine adaptiveExpertConsensusEngine;
    private RealtimeCausalProbabilityEngine realtimeCausalProbabilityEngine;
    private UnifiedStrategyEvidenceEngine unifiedStrategyEvidenceEngine;
    private RollingCausalBacktestEngine rollingCausalBacktestEngine;
    private MicroWalkForwardEngine microWalkForwardEngine;

    private Object teacherMatrixEngine;
    private Object deepLearningBridgeEngine;

    private Object preDrawFeatureEngine;
    private Object fullCandidateScanEngine;
    private Object causalFactorMatrixEngine;
    private Object positionCauseMatrixEngine;
    private TeacherNumberMatrixEngine teacherNumberMatrixEngine;
    private Object counterfactualRankEngine;
    private Object thirdGuardMatrixEngine;
    private Object ruleHealthMatrixEngine;
    private Object shadowHealthGateEngine;
    private Object falseStrongFreezeEngine;
    private Object seedCauseExpansionEngine;
    private Object dynamicCauseChainEngine;
    private Object dynamicWindowClusterEngine;
    private Object needSupplyReasonEngine;
    private Object counterfactualReasonEngine;
    private Object diagnosisFixPlanEngine;
    private Object moduleEffectScoreEngine;
    private ShadowPromotionVerifierEngine shadowPromotionVerifierEngine;
    private Object dynamicFreezeMatrixEngine;
    private AdaptiveLearningJudgeEngine adaptiveLearningJudgeEngine;

    private volatile boolean running;
    private volatile boolean forceRefresh;

    private volatile boolean fetchBusy;
    private volatile boolean analysisBusy;
    private volatile boolean backfillBusy;
    private volatile boolean backtestBusy;
    private volatile boolean learningBusy;
    private volatile boolean learningPostPending;
    private volatile boolean microBusy;
    private volatile boolean microPostPending;
    private volatile boolean autoBacktestPostPending;
    private volatile boolean teacherMatrixBusy;
    private volatile boolean teacherMatrixPostPending;
    private volatile boolean v20CauseChainBusy;
    private volatile boolean v20CauseChainPostPending;
    private volatile boolean realBetDiagnosisBusy;
    private volatile boolean realBetDiagnosisPostPending;
    private volatile String pendingRealBetDiagnosisReason;
    private volatile boolean fullDiagnosisLoopBusy;
    private volatile boolean fullDiagnosisLoopPostPending;
    private volatile boolean pendingFullDiagnosisLoopForce;
    private volatile String pendingFullDiagnosisLoopReason;

    private volatile String pendingAnalysisBaseIssue;
    private volatile String pendingAnalysisReason;
    private volatile boolean pendingAnalysisForce;
    private volatile long pendingAnalysisGeneration;

    private volatile Models.Draw pendingTeacherMatrixDraw;
    private volatile String pendingTeacherMatrixReason;
    private volatile boolean pendingTeacherMatrixForce;

    private volatile Models.Draw pendingV20CauseChainDraw;
    private volatile String pendingV20CauseChainReason;
    private volatile boolean pendingV20CauseChainForce;

    private volatile long analysisGeneration;
    private volatile long analysisPostedAt;
    private volatile long analysisStartedAt;
    private volatile long analysisLastWatchdogKickAt;
    private volatile String analysisCurrentBaseIssue;
    private volatile String analysisCurrentTargetIssue;

    private volatile long lastAnalysisDirectTakeoverAt;

    private volatile boolean backtestYieldRequested;
    private volatile String backtestYieldReason;
    private volatile boolean backtestStoppedForRealtime;
    private volatile int backtestStoppedProgress;
    private volatile int backtestStoppedWindow;
    private volatile String backtestStoppedSummary;

    private volatile boolean backtestHeartbeatRunning;
    private volatile boolean backtestHeartbeatManual;
    private volatile int backtestHeartbeatStartProgress;
    private volatile int backtestHeartbeatEndProgress;
    private volatile int backtestHeartbeatWindow;
    private volatile String backtestHeartbeatMode;
    private volatile String backtestHeartbeatSummary;
    private volatile long backtestHeartbeatStartedAt;

    private volatile long lastLearningAt;
    private volatile String lastLearningSummary;

    private volatile long lastTeacherMatrixAt;
    private volatile String lastTeacherMatrixSummary;
    private volatile String lastTeacherMatrixIssue;

    private volatile long lastV20CauseChainAt;
    private volatile String lastV20CauseChainSummary;
    private volatile String lastV20CauseChainIssue;
    private volatile long lastWarmProfileAt;
    private volatile String lastWarmProfileIssue;
    private volatile long lastColdProfileAt;
    private volatile String lastColdProfileIssue;
    private volatile long lastDeepProfileAt;
    private volatile String lastDeepProfileIssue;
    private volatile String currentBackgroundProfileTier;
    private volatile long currentBackgroundProfileStartedAt;

    private volatile long lastRealBetDiagnosisAt;
    private volatile String lastRealBetDiagnosisSummary;
    private volatile long lastFullDiagnosisLoopAt;
    private volatile String lastFullDiagnosisLoopSummary;

    private volatile long realtimePriorityUntilAt;

    private long lastFetchLoopAt;
    private long lastNotificationAt;
    private long lastBackfillAt;
    private long lastBackfillFailedAt;
    private long lastHistoryFetchAt;
    private long lastForcedBackfill100At;
    private long lastQuickHistorySyncAt;
    private long lastStaleLatestHistoryRecoveryAt;
    private long lastSimpleStateBroadcastAt;
    private long lastRuntimeStateWriteAt;
    private long lastTickBroadcastAt;
    private String lastTickTargetR72="";
    private long lastAutoBacktestTryAt;

    private long lastBacktestAt;
    private long lastFastBacktestAt;
    private long lastNormalBacktestAt;
    private long lastDeepBacktestAt;

    private long lastAnalysisStateWriteAt;
    private String lastAnalysisStateKey;

    private boolean networkWasUnavailable;
    private boolean startupBackfillDone;
    private final java.util.concurrent.atomic.AtomicBoolean deferredDrainPostedR367 =
            new java.util.concurrent.atomic.AtomicBoolean();
    private volatile long lastSettlementWatchdogAtR816;
    private volatile long lastAnalysisReconcileAtR816;
    private volatile long lastProductionMaintenanceAtR816;
    private volatile long lastResearchTickAtR816;
    private volatile long lastResearchSchemaAttemptAtR817;
    private volatile long lastB13ServicePulseAtR817;

    private String lastStatus;
    private String lastError;
    private String lastProcessedDrawIssue;
    private String lastFinalDecisionTargetIssue;
    private String lastBacktestMode;
    private String lastRuntimeStateKey;

    private String currentManualTaskId;
    private String currentManualMode;
    private long currentManualStartedAt;

    private int realtimeMissCount;
    private boolean realtimeHistory100Posted;

    private final Runnable fetchLoop =
            new Runnable() {
                @Override
                public void run() {
                    if (!running) {
                        return;
                    }

                    try {
                        runFetchChannelOnce();
                    } catch (Throwable e) {
                        if (isOomR3610(e)) {
                            handleRuntimeOomR3610(e, "FETCH_LOOP");
                        } else {
                            lastError = safeError(e);
                            updateRuntimeState(AppConfig.SERVICE_STATUS_API_ERROR, lastError);
                        }
                    } finally {
                        // Error reporting can itself fail during a busy DB interval.
                        // Always retain the existing serial fetch worker's next wake-up.
                        scheduleNextFetchLoop();
                    }
                }
            };

    private final Runnable analysisLoop =
            new Runnable() {
                @Override
                public void run() {
                    if (!running) {
                        return;
                    }

                    try {
                        runAnalysisChannelOnce();
                    } catch (Throwable e) {
                        if (isOomR3610(e)) {
                            handleRuntimeOomR3610(e, "ANALYSIS_LOOP");
                        } else {
                            lastError = safeError(e);
                            setAnalysisState("ERROR", 0, "分析线程异常：" + lastError);
                            updateRuntimeState(AppConfig.SERVICE_STATUS_API_ERROR, lastError);
                        }
                    }
                }
            };

    private final Runnable analysisWatchdogLoop =
            new Runnable() {
                @Override
                public void run() {
                    if (!running) {
                        return;
                    }

                    try {
                        runAnalysisWatchdogOnce();
                    } catch (Throwable e) {
                    }

                    if (running && analysisWatchdogHandler != null) {
                        analysisWatchdogHandler.postDelayed(this, ANALYSIS_WATCHDOG_INTERVAL_MS);
                    }
                }
            };

    private final Runnable teacherMatrixLoop =
            new Runnable() {
                @Override
                public void run() {
                    if (!running) {
                        return;
                    }

                    teacherMatrixPostPending = false;

                    Models.Draw draw = pendingTeacherMatrixDraw;
                    String reason = pendingTeacherMatrixReason;
                    boolean force = pendingTeacherMatrixForce;

                    pendingTeacherMatrixDraw = null;
                    pendingTeacherMatrixReason = "";
                    pendingTeacherMatrixForce = false;

                    runTeacherMatrixWorker(draw, reason, force);
                }
            };

    private final Runnable v20CauseChainLoop =
            new Runnable() {
                @Override
                public void run() {
                    if (!running) {
                        return;
                    }

                    v20CauseChainPostPending = false;

                    Models.Draw draw = pendingV20CauseChainDraw;
                    String reason = pendingV20CauseChainReason;
                    boolean force = pendingV20CauseChainForce;

                    pendingV20CauseChainDraw = null;
                    pendingV20CauseChainReason = "";
                    pendingV20CauseChainForce = false;

                    runV20CauseChainWorker(draw, reason, force);
                }
            };

    private final Runnable realBetDiagnosisLoop =
            new Runnable() {
                @Override
                public void run() {
                    if (!running) {
                        return;
                    }
                    realBetDiagnosisPostPending = false;
                    String reason =
                            pendingRealBetDiagnosisReason == null
                                    ? ""
                                    : pendingRealBetDiagnosisReason;
                    pendingRealBetDiagnosisReason = "";
                    runRealBetDiagnosisWorker(reason);
                }
            };

    private final Runnable fullDiagnosisLoop =
            new Runnable() {
                @Override
                public void run() {
                    if (!running) {
                        return;
                    }
                    fullDiagnosisLoopPostPending = false;
                    String reason =
                            pendingFullDiagnosisLoopReason == null
                                    ? ""
                                    : pendingFullDiagnosisLoopReason;
                    boolean force = pendingFullDiagnosisLoopForce;
                    pendingFullDiagnosisLoopReason = "";
                    pendingFullDiagnosisLoopForce = false;
                    runFullDiagnosisLoopWorker(reason, force);
                }
            };

    private final Runnable backtestHeartbeatLoop =
            new Runnable() {
                @Override
                public void run() {
                    if (!running || !backtestHeartbeatRunning || store == null) {
                        return;
                    }

                    try {
                        long now = System.currentTimeMillis();
                        long cost = now - backtestHeartbeatStartedAt;

                        int start = backtestHeartbeatStartProgress;
                        int end = backtestHeartbeatEndProgress;

                        if (start < 0) {
                            start = 0;
                        }

                        if (end <= start) {
                            end = start + 1;
                        }

                        int move = (int) (cost / 3500L);
                        int p = start + move;

                        if (p >= end) {
                            p = end - 1;
                        }

                        if (p < start) {
                            p = start;
                        }

                        String summary =
                                backtestHeartbeatSummary == null ? "" : backtestHeartbeatSummary;

                        if (backtestYieldRequested || shouldDeferHeavyBackgroundNow()) {
                            summary = appendReason(summary, "实时分析优先，回测准备让路");
                        } else {
                            summary =
                                    appendReason(
                                            summary, "已运行 " + formatElapsed(cost) + "，安全分层回测计算中");
                        }

                        setBacktestState(
                                "RUNNING",
                                backtestHeartbeatMode,
                                p,
                                backtestHeartbeatWindow,
                                summary,
                                "");

                        if (backtestHeartbeatManual) {
                            saveManualTaskState(backtestHeartbeatMode, "RUNNING", p, summary, "");
                        }

                        broadcastBacktestUpdated();
                    } catch (Throwable e) {
                    }

                    if (running && backtestHeartbeatRunning && runtimeGuardHandler != null) {
                        runtimeGuardHandler.postDelayed(this, BACKTEST_HEARTBEAT_INTERVAL_MS);
                    }
                }
            };

    private final Runnable decisionStateRefreshR77 = new Runnable() {
        @Override public void run() {
            if (!running) return;
            broadcastSimpleStateLimited(true);
            refreshNotificationIfNeeded();
        }
    };

    public AutoFetchService() {}

    private final Runnable startupHeartbeatLoopR323 =
            new Runnable() {
                @Override
                public void run() {
                    if (runtimeInitializationCancelled) return;
                    try {
                        if (!runtimeInitialized) {
                            heartbeatStateV8()
                                    .writeHeartbeat("AutoFetchService:R323_STARTUP_GUARD");
                            heartbeatStateV8().renewRuntimeLease("R323_STARTUP_GUARD");
                        }
                    } catch (Throwable e) {
                        CrashRecorder.recordCaught(
                                getApplicationContext(), e, "R323_STARTUP_HEARTBEAT");
                    }
                    if (!runtimeInitialized
                            && !runtimeInitializationCancelled
                            && startupHeartbeatHandlerR323 != null) {
                        startupHeartbeatHandlerR323.postDelayed(this, 1500L);
                    }
                }
            };

    /**
     * R817 independent liveness pulse.  The older service heartbeat shares the runtime-guard
     * HandlerThread with lease repair and scheduler reconciliation; a long SQLite write could
     * therefore make a healthy process look dead.  This independent serial pulse performs no
     * scans, network or recovery work.  It is deliberately off the main Looper: SharedPreferences
     * commit and the tiny B13 state write must never add UI frames or ANR pressure.
     */
    private final Runnable lightweightHeartbeatLoopR817 =
            new Runnable() {
                @Override
                public void run() {
                    if (!running || runtimeInitializationCancelled) return;
                    try {
                        // SQLite contention on the analysis watchdog must not lose the
                        // research wake-up. This only posts to the existing single owner.
                        postAutonomousResearchTickR908("R66_INDEPENDENT_RESEARCH_REARM");
                        heartbeatStateV8().writeHeartbeat("AutoFetchService:R817_LIVENESS_PULSE");
                        long now = System.currentTimeMillis();
                        if (now - lastB13ServicePulseAtR817 >= 30000L) {
                            lastB13ServicePulseAtR817 = now;
                            b13RecordBusinessBeat(store, "SERVICE_PULSE", "R817_LIVENESS");
                            persistBackgroundCapabilityR919(now);
                        }
                    } catch (Throwable error) {
                        CrashRecorder.recordCaught(
                                getApplicationContext(), error, "R817_LIGHT_HEARTBEAT");
                    }
                    if (running
                            && !runtimeInitializationCancelled
                            && livenessHandlerR817 != null)
                        livenessHandlerR817.postDelayed(this, 5000L);
                }
            };

    private void startLightweightHeartbeatR817() {
        try {
            if (livenessHandlerR817 == null) return;
            livenessHandlerR817.removeCallbacks(lightweightHeartbeatLoopR817);
            livenessHandlerR817.post(lightweightHeartbeatLoopR817);
        } catch (Throwable error) {
            CrashRecorder.recordCaught(
                    getApplicationContext(), error, "R817_LIGHT_HEARTBEAT_ARM");
        }
    }

    /** Persist runtime grants, not manifest wishes, so the UI can explain OEM background risk. */
    private void persistBackgroundCapabilityR919(long observedAt) {
        if (store == null) return;
        try {
            BackgroundRuntimeCapabilityR811.Snapshot c =
                    BackgroundRuntimeCapabilityR811.snapshot(getApplicationContext());
            HashMap<String,String> values = new HashMap<String,String>();
            values.put("r919_background_capability_status", safe(c.status));
            values.put("r919_background_battery_unrestricted",
                    c.ignoringBatteryOptimizations ? "1" : "0");
            values.put("r919_background_exact_alarm_granted",
                    c.exactAlarmRuntimeGranted ? "1" : "0");
            values.put("r919_background_runtime_sdk", String.valueOf(c.runtimeSdk));
            values.put("r919_background_target_sdk", String.valueOf(c.targetSdk));
            values.put("r919_background_permission_summary",
                    c.grantedPermissions + "/" + c.declaredPermissions);
            values.put("r919_background_capability_at", String.valueOf(observedAt));
            values.put("r919_background_capability_hash", c.certificateFingerprint());
            store.setStates(values);
        } catch (Throwable e) {
            try {
                store.setState("r919_background_capability_status",
                        "READ_ERROR:" + e.getClass().getSimpleName());
            } catch (Throwable ignored) {}
        }
    }

    private void startStartupHeartbeatR323() {
        try {
            if (startupHeartbeatHandlerR323 == null)
                startupHeartbeatHandlerR323 = new Handler(getMainLooper());
            startupHeartbeatHandlerR323.removeCallbacks(startupHeartbeatLoopR323);
            startupHeartbeatHandlerR323.post(startupHeartbeatLoopR323);
        } catch (Throwable e) {
            CrashRecorder.recordCaught(getApplicationContext(), e, "R323_STARTUP_HEARTBEAT_ARM");
        }
    }

    private void scheduleRuntimeInitializationRetryR323(final String error) {
        if (runtimeInitializationCancelled || runtimeInitialized) return;
        runtimeInitRetryCountR323++;
        final int attempt = runtimeInitRetryCountR323;
        boolean oomRetry = error != null && error.indexOf("OutOfMemoryError") >= 0;
        long delay =
                oomRetry
                        ? Math.min(60000L, 15000L * Math.max(1, attempt))
                        : Math.min(60000L, 1800L * Math.max(1, attempt));
        try {
            heartbeatStateV8().markInitRetry(attempt, error);
        } catch (Throwable ignore) {
        }
        try {
            BootReceiver.scheduleAllRecovery(getApplicationContext(), "r323_init_retry_" + attempt);
        } catch (Throwable ignore) {
        }
        if (startupHeartbeatHandlerR323 == null)
            startupHeartbeatHandlerR323 = new Handler(getMainLooper());
        startupHeartbeatHandlerR323.postDelayed(
                new Runnable() {
                    @Override
                    public void run() {
                        if (runtimeInitializationCancelled || runtimeInitialized) return;
                        try {
                            heartbeatStateV8()
                                    .markBusinessStage("SERVICE_INIT_RETRY", "attempt=" + attempt);
                        } catch (Throwable ignore) {
                        }
                        ensureRuntimeInitializationR32();
                    }
                },
                delay);
    }

    /** Ensures a failed heavy-init attempt cannot leave duplicate HandlerThread owners behind. */
    private void resetPartialRuntimeAfterInitFailureR363() {
        running = false;
        researchCaptureQueueR946.stop();
        clearHandlerCallbacksR363(fetchHandler);
        clearHandlerCallbacksR363(runtimeGuardHandler);
        clearHandlerCallbacksR363(livenessHandlerR817);
        clearHandlerCallbacksR363(analysisWatchdogHandler);
        countdownWorkPostedR964.set(false);
        clearHandlerCallbacksR363(analysisHandler);
        clearHandlerCallbacksR363(learningHandler);
        clearHandlerCallbacksR363(settlementRecoveryHandlerR908);
        clearHandlerCallbacksR363(researchHandlerR908);
        clearHandlerCallbacksR363(microHandler);
        quitThread(fetchThread);
        quitThread(runtimeGuardThread);
        quitThread(livenessThreadR817);
        quitThread(analysisWatchdogThread);
        quitThread(analysisThread);
        quitThread(backfillThread);
        quitThread(backtestThread);
        quitThread(learningThread);
        quitThread(settlementRecoveryThreadR908);
        quitThread(countdownThreadR973);
        quitThread(researchThreadR908);
        quitThread(microThread);
        fetchThread = null;
        fetchHandler = null;
        runtimeGuardThread = null;
        runtimeGuardHandler = null;
        livenessThreadR817 = null;
        livenessHandlerR817 = null;
        analysisWatchdogThread = null;
        analysisWatchdogHandler = null;
        analysisThread = null;
        analysisHandler = null;
        backfillThread = null;
        backfillHandler = null;
        backtestThread = null;
        backtestHandler = null;
        learningThread = null;
        learningHandler = null;
        settlementRecoveryThreadR908 = null;
        settlementRecoveryHandlerR908 = null;
        countdownThreadR973 = null;
        researchThreadR908 = null;
        researchHandlerR908 = null;
        microThread = null;
        microHandler = null;
        releasePartialObjectGraphR3610();
    }

    private void releasePartialObjectGraphR3610() {
        try {
            if (store != null) store.close();
        } catch (Throwable ignore) {
        }
        store = null;
        apiClient = null;
        dataGuard = null;
        countdownClock = null;
        windowFeatureEngine = null;
        shapeStateEngine = null;
        trendStateMachine = null;
        markovEngine = null;
        breakRecoveryEngine = null;
        comboScannerEngine = null;
        probabilityEngine = null;
        calibrationEngine = null;
        dynamicGateEngine = null;
        decisionGateEngine = null;
        v50RuntimeOrchestrator = null;
        settlementEngine = null;
        researchEvolutionR811 = null;
        pendingExpertSnapshotR811 = null;
        backtestEngine = null;
        reasonEngine = null;
        realBetDiagnosisEngine = null;
        learningEngine = null;
        historicalStatePriorEngine = null;
        onlineEvidenceForestEngine = null;
        hierarchicalStateTaxonomyEngine = null;
        adaptiveExpertConsensusEngine = null;
        realtimeCausalProbabilityEngine = null;
        unifiedStrategyEvidenceEngine = null;
        rollingCausalBacktestEngine = null;
        microWalkForwardEngine = null;
        teacherMatrixEngine = null;
        deepLearningBridgeEngine = null;
        preDrawFeatureEngine = null;
        fullCandidateScanEngine = null;
        causalFactorMatrixEngine = null;
        positionCauseMatrixEngine = null;
        teacherNumberMatrixEngine = null;
        counterfactualRankEngine = null;
        thirdGuardMatrixEngine = null;
        ruleHealthMatrixEngine = null;
        shadowHealthGateEngine = null;
        falseStrongFreezeEngine = null;
        seedCauseExpansionEngine = null;
        dynamicCauseChainEngine = null;
        dynamicWindowClusterEngine = null;
        needSupplyReasonEngine = null;
        counterfactualReasonEngine = null;
        diagnosisFixPlanEngine = null;
        moduleEffectScoreEngine = null;
        shadowPromotionVerifierEngine = null;
        dynamicFreezeMatrixEngine = null;
        adaptiveLearningJudgeEngine = null;
        try {
            Runtime.getRuntime().gc();
        } catch (Throwable ignore) {
        }
    }

    private static void clearHandlerCallbacksR363(Handler handler) {
        if (handler == null) return;
        try {
            handler.removeCallbacksAndMessages(null);
        } catch (Throwable ignore) {
        }
    }

    @Override
    public void onCreate() {
        lifecycleActiveR82 = true;
        activeInstanceR82 = this;
        super.onCreate();
        serviceMainThreadR3610 = Thread.currentThread();
        RuntimeOwnerAuditR815.register(
                "service-main", serviceMainThreadR3610, RecoveryCoordinatorR815.currentGeneration(this), 0);
        RecoveryCoordinatorR815.markServiceStarted(getApplicationContext());
        CrashRecorder.install(getApplicationContext());
        CrashRecorder.setRuntimeContext("", "SERVICE_CREATE_FOREGROUND_FIRST_R32");
        heartbeatStoreV8 = new ServiceHeartbeatStore(getApplicationContext());
        if (!heartbeatStoreV8.isUserEnabled()) {
            runtimeInitializationCancelled = true;
            stopSelf();
            return;
        }
        runtimeInitRetryCountR323 = 0;
        try {
            heartbeatStateV8().markServiceStarting("SERVICE_ON_CREATE_R323");
        } catch (Throwable ignore) {
        }
        startStartupHeartbeatR323();
        runtimeInitialized = false;
        runtimeInitializationRunning = false;
        runtimeInitializationCancelled = false;
        runtimeInitializationError = "";
        analysisOomBackoffUntilR3610 = 0L;
        pendingStartAction = AppConfig.ACTION_SERVICE_START;
        pendingStartId = 0;
        try {
            createNotificationChannel();
            startForeground(AppConfig.NOTIFICATION_ID, buildNotification("启动保护", "后台引擎正在异步初始化"));
            try {
                heartbeatStateV8().markForegroundNotification();
                heartbeatStateV8().markActualProcessStart("AutoFetchService.onCreate");
                heartbeatStateV8().markStart("AutoFetchService.onCreate");
                heartbeatStateV8().markInitStage("FOREGROUND_ENTERED", "heavy_init_pending");
            } catch (Throwable ignore) {
            }
        } catch (Throwable foregroundError) {
            CrashRecorder.recordCaught(
                    getApplicationContext(), foregroundError, "SERVICE_FOREGROUND_ENTRY");
            try {
                heartbeatStateV8()
                        .markRecoveryResult(
                                "service_foreground_entry", false, safeError(foregroundError));
            } catch (Throwable ignore) {
            }
            stopSelf();
            return;
        }
        ensureRuntimeInitializationR32();
    }

    private synchronized Handler ensureStartupInitOwnerR94() {
        if (startupInitThread == null || startupInitHandler == null
                || !startupInitThread.isAlive()
                || startupInitHandler.getLooper().getThread() != startupInitThread) {
            clearHandlerCallbacksR363(startupInitHandler);
            quitThread(startupInitThread);
            startupInitHandler = null;
            startupInitThread = new HandlerThread(
                    "starcore_r32_startup_init", Process.THREAD_PRIORITY_BACKGROUND);
            try {
                startupInitThread.start();
                startupInitHandler = new Handler(startupInitThread.getLooper());
            } catch (Throwable failure) {
                quitThread(startupInitThread);
                startupInitThread = null;
                throw failure;
            }
        }
        return startupInitHandler;
    }

    private synchronized void ensureRuntimeInitializationR32() {
        if (runtimeInitialized || runtimeInitializationRunning || runtimeInitializationCancelled)
            return;
        runtimeInitializationRunning = true;
        try {
            boolean accepted = ensureStartupInitOwnerR94().post(
                    new Runnable() {
                        @Override
                        public void run() {
                            initializeRuntimeAfterForegroundR32();
                        }
                    });
            if (!accepted) throw new IllegalStateException("STARTUP_INIT_OWNER_REJECTED");
        } catch (Throwable e) {
            runtimeInitializationRunning = false;
            runtimeInitializationError = safeError(e);
            clearHandlerCallbacksR363(startupInitHandler);
            quitThread(startupInitThread);
            startupInitHandler = null;
            startupInitThread = null;
            CrashRecorder.recordCaught(getApplicationContext(), e, "SERVICE_INIT_THREAD_CREATE");
            try { heartbeatStateV8().markInitFailure(runtimeInitializationError); }
            catch (Throwable ignored) {}
            scheduleRuntimeInitializationRetryR323(runtimeInitializationError);
        }
    }

    private boolean finishCancelledRuntimeInitializationR94() {
        if (!runtimeInitializationCancelled) return false;
        runtimeInitializationRunning = false;
        resetPartialRuntimeAfterInitFailureR363();
        return true;
    }

    private void initializeRuntimeAfterForegroundR32() {
        if (finishCancelledRuntimeInitializationR94()) return;
        CrashRecorder.setRuntimeContext("", "SERVICE_HEAVY_INIT_BACKGROUND_R32");
        try {
            heartbeatStateV8().markInitStage("DB_CRITICAL_OPEN", "R35_R36_V51_B03");
            store = new DbStore(getApplicationContext());
            if (finishCancelledRuntimeInitializationR94()) return;
            heartbeatStateV8()
                    .markInitStage("DB_CRITICAL_READY", store.startupSchemaSummaryR363());

            // R820: clear only durable RUNNING leases left by a dead process before any new worker
            // can enter the process-wide settlement gate. This turns stranded LIVE_CURRENT rows
            // back into ordinary durable retries without fabricating a settlement.
            int recoveredSettlementLeases =
                    SettlementTaskStoreR36.recoverOrphanedRunningLeasesOnProcessStart(store);
            store.setState(
                    "r820_settlement_orphan_leases_recovered",
                    String.valueOf(recoveredSettlementLeases));
            store.setState(
                    "r820_research_schema_startup",
                    store.coordinatorR36().isR811ResearchSchemaReady() ? "READY" : "FAILED");
            if (store.coordinatorR36().isR811ResearchSchemaReady()) {
                store.setState("r817_research_schema_status", "READY");
                store.setState("r811_research_tick_status", "STARTUP_SCHEMA_READY_WAIT_TICK");
            }
            heartbeatStateV8()
                    .markInitStage(
                            "R820_DURABLE_RECOVERY",
                            "settlement=" + recoveredSettlementLeases
                                    + "|researchSchema="
                                    + (store.coordinatorR36().isR811ResearchSchemaReady()
                                            ? "READY"
                                            : "FAILED"));
            BuildIdentity.verifyAndPersist(getApplicationContext(), store);
            heartbeatStateV8()
                    .markInitStage(
                            "BUILD_IDENTITY_READY", BuildIdentity.MATERIALIZATION_MARKER);
            RealtimeProvenanceRepairR923.Result provenanceRepair =
                    RealtimeProvenanceRepairR923.repair(store);
            store.setState(
                    "r923_realtime_provenance_repair_summary",
                    provenanceRepair.canonical());
            heartbeatStateV8().markInitStage(
                    "R923_REALTIME_PROVENANCE_REPAIR", provenanceRepair.canonical());

            running = false;
            forceRefresh = false;

            fetchBusy = false;
            analysisBusy = false;
            backfillBusy = false;
            backtestBusy = false;
            learningBusy = false;
            learningPostPending = false;
            microBusy = false;
            microPostPending = false;
            autoBacktestPostPending = false;
            teacherMatrixBusy = false;
            teacherMatrixPostPending = false;
            v20CauseChainBusy = false;
            v20CauseChainPostPending = false;
            realBetDiagnosisBusy = false;
            realBetDiagnosisPostPending = false;
            pendingRealBetDiagnosisReason = "";
            fullDiagnosisLoopBusy = false;
            fullDiagnosisLoopPostPending = false;
            pendingFullDiagnosisLoopForce = false;
            pendingFullDiagnosisLoopReason = "";
            fullDiagnosisLoopBusy = false;
            fullDiagnosisLoopPostPending = false;
            pendingFullDiagnosisLoopForce = false;
            pendingFullDiagnosisLoopReason = "";

            pendingAnalysisBaseIssue = "";
            pendingAnalysisReason = "";
            pendingAnalysisForce = false;
            pendingAnalysisGeneration = 0L;

            pendingTeacherMatrixDraw = null;
            pendingTeacherMatrixReason = "";
            pendingTeacherMatrixForce = false;

            pendingV20CauseChainDraw = null;
            pendingV20CauseChainReason = "";
            pendingV20CauseChainForce = false;

            analysisGeneration = 0L;
            analysisPostedAt = 0L;
            analysisStartedAt = 0L;
            analysisLastWatchdogKickAt = 0L;
            analysisCurrentBaseIssue = "";
            analysisCurrentTargetIssue = "";
            lastAnalysisDirectTakeoverAt = 0L;

            backtestYieldRequested = false;
            backtestYieldReason = "";
            backtestStoppedForRealtime = false;
            backtestStoppedProgress = 0;
            backtestStoppedWindow = 0;
            backtestStoppedSummary = "";

            backtestHeartbeatRunning = false;
            backtestHeartbeatManual = false;
            backtestHeartbeatStartProgress = 0;
            backtestHeartbeatEndProgress = 0;
            backtestHeartbeatWindow = 0;
            backtestHeartbeatMode = "";
            backtestHeartbeatSummary = "";
            backtestHeartbeatStartedAt = 0L;

            lastLearningAt = 0L;
            lastLearningSummary = "";

            lastTeacherMatrixAt = 0L;
            lastTeacherMatrixSummary = "";
            lastTeacherMatrixIssue = "";

            lastV20CauseChainAt = 0L;
            lastV20CauseChainSummary = "";
            lastV20CauseChainIssue = "";
            lastWarmProfileAt = 0L;
            lastWarmProfileIssue = "";
            lastColdProfileAt = 0L;
            lastColdProfileIssue = "";
            lastDeepProfileAt = 0L;
            lastDeepProfileIssue = "";
            currentBackgroundProfileTier = "";
            currentBackgroundProfileStartedAt = 0L;

            lastRealBetDiagnosisAt = 0L;
            lastRealBetDiagnosisSummary = "";
            lastFullDiagnosisLoopAt = 0L;
            lastFullDiagnosisLoopSummary = "";

            realtimePriorityUntilAt = 0L;

            userRequestedStopV5 = false;
            backgroundEvidencePumpPostedV6 = false;
            lastBackgroundEvidencePumpAtV6 = 0L;
            lastBackgroundSliceAtV5 = 0L;
            lastServiceHeartbeatAtV5 = 0L;
            reservedTeacherSlicePostedV8 = false;
            lastReservedTeacherSliceTargetV8 = "";
            analysisRecordedCostSumV8 = 0L;
            analysisRecordedTargetV8 = "";
            lastFetchLoopAt = 0L;
            lastNotificationAt = 0L;
            lastBackfillAt = 0L;
            lastBackfillFailedAt = 0L;
            lastHistoryFetchAt = 0L;
            lastForcedBackfill100At = 0L;
            lastQuickHistorySyncAt = 0L;
            lastSimpleStateBroadcastAt = 0L;
            lastRuntimeStateWriteAt = 0L;
            lastTickBroadcastAt = 0L;
            lastAutoBacktestTryAt = 0L;

            lastBacktestAt = 0L;
            lastFastBacktestAt = 0L;
            lastNormalBacktestAt = 0L;
            lastDeepBacktestAt = 0L;

            lastAnalysisStateWriteAt = 0L;
            lastAnalysisStateKey = "";

            networkWasUnavailable = false;
            startupBackfillDone = false;

            lastStatus = AppConfig.SERVICE_STATUS_STOPPED;
            lastError = "";
            lastProcessedDrawIssue = "";
            lastFinalDecisionTargetIssue = "";
            lastBacktestMode = "";
            lastRuntimeStateKey = "";

            currentManualTaskId = "";
            currentManualMode = "";
            currentManualStartedAt = 0L;

            realtimeMissCount = 0;
            realtimeHistory100Posted = false;
            b07StartupBackfillPosted = false;

            heartbeatStateV8().markInitStage("WORKER_THREADS", "create_serial_owners");

            fetchThread =
                    new HandlerThread("eth_k3_fetch_channel", Process.THREAD_PRIORITY_DEFAULT);
            fetchThread.start();
            fetchHandler = new Handler(fetchThread.getLooper());

            runtimeGuardThread =
                    new HandlerThread(
                            "starcore_b07_runtime_guard", Process.THREAD_PRIORITY_DEFAULT);
            runtimeGuardThread.start();
            runtimeGuardHandler = new Handler(runtimeGuardThread.getLooper());
            livenessThreadR817 =
                    new HandlerThread(
                            "starcore_r817_liveness", Process.THREAD_PRIORITY_BACKGROUND);
            livenessThreadR817.start();
            livenessHandlerR817 = new Handler(livenessThreadR817.getLooper());
            analysisWatchdogThread =
                    new HandlerThread(
                            "starcore_b07_analysis_watchdog", Process.THREAD_PRIORITY_DEFAULT);
            analysisWatchdogThread.start();
            analysisWatchdogHandler = new Handler(analysisWatchdogThread.getLooper());

            analysisThread =
                    new HandlerThread("eth_k3_analysis_channel", Process.THREAD_PRIORITY_DEFAULT);
            analysisThread.start();
            analysisHandler = new Handler(analysisThread.getLooper());

            /* 所有低优先级任务共用一个串行后台线程，避免5线程同时抢SQLite与CPU。 */
            backfillThread = null;
            backtestThread = null;
            learningThread =
                    new HandlerThread(
                            "eth_k3_background_serial_channel", Process.THREAD_PRIORITY_BACKGROUND);
            learningThread.start();
            learningHandler = new Handler(learningThread.getLooper());

            settlementRecoveryThreadR908 =
                    new HandlerThread(
                            "starcore_r908_settlement_recovery",
                            Process.THREAD_PRIORITY_DEFAULT);
            settlementRecoveryThreadR908.start();
            settlementRecoveryHandlerR908 = new Handler(settlementRecoveryThreadR908.getLooper());

            countdownThreadR973 = new HandlerThread("starcore_r973_clock_only", Process.THREAD_PRIORITY_DEFAULT);
            countdownThreadR973.start();

            researchThreadR908 =
                    new HandlerThread(
                            "starcore_r908_autonomous_research",
                            Process.THREAD_PRIORITY_BACKGROUND);
            researchThreadR908.start();
            researchHandlerR908 = new Handler(researchThreadR908.getLooper());

            microThread =
                    new HandlerThread(
                            "eth_k3_v25_oos_micro_channel", Process.THREAD_PRIORITY_BACKGROUND);
            microThread.start();
            microHandler = new Handler(microThread.getLooper());
            backfillHandler = learningHandler;
            backtestHandler = learningHandler;

            if ((AppConfig.ENABLE_V50_PRIME || AppConfig.ENABLE_V49_FULL_MOE)
                    && learningHandler != null) {
                learningHandler.postDelayed(
                        new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    HistoricalDrawImportEngine.Report report =
                                            new HistoricalDrawImportEngine()
                                                    .importIfNeeded(getApplicationContext(), store);
                                    store.setState(
                                            "v49_history_import_runtime",
                                            report == null ? "NULL" : report.summary());
                                } catch (Throwable e) {
                                    try {
                                        store.setState(
                                                "v49_history_import_runtime",
                                                "ERROR:" + e.getClass().getSimpleName());
                                    } catch (Throwable ignore) {
                                    }
                                }
                            }
                        },
                        AppConfig.ENABLE_V50_PRIME ? 1000L : 15000L);
            }
            writeBusinessHeartbeatState("last_service_started_at", "", "SERVICE_STARTED");
            try {
                store.setState(
                        AppConfig.STATE_STABILITY_CORE_VERSION, AppConfig.STABILITY_CORE_VERSION);
                store.setState(
                        AppConfig.STATE_FULL_CLOSED_LOOP_VERSION,
                        AppConfig.FULL_CLOSED_LOOP_18_VERSION);
            } catch (Throwable ignore) {
            }
            heartbeatStateV8().markInitStage("ENGINE_GRAPH", "construct_active_runtime");
            apiClient = new ApiClient(store);
            dataGuard = new DataGuard();
            countdownClock = new CountdownClock(countdownThreadR973.getLooper());

            windowFeatureEngine = new WindowFeatureEngine();
            shapeStateEngine = new ShapeStateEngine();
            trendStateMachine = new TrendStateMachine();
            markovEngine = new MarkovEngine();
            breakRecoveryEngine = new BreakRecoveryEngine();
            comboScannerEngine = new ComboScannerEngine();
            probabilityEngine = new ProbabilityEngine();
            calibrationEngine = new CalibrationEngine();
            dynamicGateEngine = new DynamicGateEngine();
            decisionGateEngine = new DecisionGateEngine();
            v50RuntimeOrchestrator = new V50RuntimeOrchestrator();
            settlementEngine = new SettlementEngine();
            researchEvolutionR811 = new AutonomousResearchEvolutionEngine();
            backtestEngine = new WalkForwardBacktestEngine();
            reasonEngine = new ReasonEngine();
            realBetDiagnosisEngine = new RealBetDiagnosisEngine();
            learningEngine = new LearningEngine();
            historicalStatePriorEngine = new HistoricalStatePriorEngine();
            onlineEvidenceForestEngine = new OnlineEvidenceForestEngine();
            hierarchicalStateTaxonomyEngine = new HierarchicalStateTaxonomyEngine();
            adaptiveExpertConsensusEngine = new AdaptiveExpertConsensusEngine();
            realtimeCausalProbabilityEngine = new RealtimeCausalProbabilityEngine();
            unifiedStrategyEvidenceEngine = new UnifiedStrategyEvidenceEngine();
            StrategyModuleCatalog.validateAndPersist(store);
            rollingCausalBacktestEngine = new RollingCausalBacktestEngine();
            microWalkForwardEngine = new MicroWalkForwardEngine();

            if (!AppConfig.ENABLE_V51_TIANSHU || !AppConfig.V51_RUNTIME_LEGACY_HEAVY_ISOLATION) {
                teacherMatrixEngine =
                        createOptionalEngine("com.ethk3.novaonepro.TeacherMatrixEngine");
                deepLearningBridgeEngine =
                        createOptionalEngine("com.ethk3.novaonepro.DeepLearningBridgeEngine");

                preDrawFeatureEngine =
                        createOptionalEngine("com.ethk3.novaonepro.PreDrawFeatureEngine");
                fullCandidateScanEngine =
                        createOptionalEngine("com.ethk3.novaonepro.FullCandidateScanEngine");
                causalFactorMatrixEngine =
                        createOptionalEngine("com.ethk3.novaonepro.CausalFactorMatrixEngine");
                positionCauseMatrixEngine =
                        createOptionalEngine("com.ethk3.novaonepro.PositionCauseMatrixEngine");
                teacherNumberMatrixEngine = new TeacherNumberMatrixEngine();
                counterfactualRankEngine =
                        createOptionalEngine("com.ethk3.novaonepro.CounterfactualRankEngine");
                thirdGuardMatrixEngine =
                        createOptionalEngine("com.ethk3.novaonepro.ThirdGuardMatrixEngine");
                ruleHealthMatrixEngine =
                        createOptionalEngine("com.ethk3.novaonepro.RuleHealthMatrixEngine");
                shadowHealthGateEngine =
                        createOptionalEngine("com.ethk3.novaonepro.ShadowHealthGateEngine");
                falseStrongFreezeEngine =
                        createOptionalEngine("com.ethk3.novaonepro.FalseStrongFreezeEngine");
                seedCauseExpansionEngine =
                        createOptionalEngine("com.ethk3.novaonepro.SeedCauseExpansionEngine");
                dynamicCauseChainEngine =
                        createOptionalEngine("com.ethk3.novaonepro.DynamicCauseChainEngine");
                dynamicWindowClusterEngine =
                        createOptionalEngine("com.ethk3.novaonepro.DynamicWindowClusterEngine");
                needSupplyReasonEngine =
                        createOptionalEngine("com.ethk3.novaonepro.NeedSupplyReasonEngine");
                counterfactualReasonEngine =
                        createOptionalEngine("com.ethk3.novaonepro.CounterfactualReasonEngine");
                diagnosisFixPlanEngine =
                        createOptionalEngine("com.ethk3.novaonepro.DiagnosisFixPlanEngine");
                moduleEffectScoreEngine =
                        createOptionalEngine("com.ethk3.novaonepro.ModuleEffectScoreEngine");
                shadowPromotionVerifierEngine = new ShadowPromotionVerifierEngine();
                dynamicFreezeMatrixEngine =
                        createOptionalEngine("com.ethk3.novaonepro.DynamicFreezeMatrixEngine");
                adaptiveLearningJudgeEngine = new AdaptiveLearningJudgeEngine();
            } else {
                teacherMatrixEngine = null;
                deepLearningBridgeEngine = null;
                preDrawFeatureEngine = null;
                fullCandidateScanEngine = null;
                causalFactorMatrixEngine = null;
                positionCauseMatrixEngine = null;
                teacherNumberMatrixEngine = null;
                counterfactualRankEngine = null;
                thirdGuardMatrixEngine = null;
                ruleHealthMatrixEngine = null;
                shadowHealthGateEngine = null;
                falseStrongFreezeEngine = null;
                seedCauseExpansionEngine = null;
                dynamicCauseChainEngine = null;
                dynamicWindowClusterEngine = null;
                needSupplyReasonEngine = null;
                counterfactualReasonEngine = null;
                diagnosisFixPlanEngine = null;
                moduleEffectScoreEngine = null;
                shadowPromotionVerifierEngine = null;
                dynamicFreezeMatrixEngine = null;
                adaptiveLearningJudgeEngine = null;
                try {
                    store.setState(
                            "v51_runtime_isolation",
                            "V51_ONLY_ACTIVE_LEGACY_HEAVY_NOT_INSTANTIATED");
                } catch (Throwable ignore) {
                }
            }

            writeEngineBootState();


            countdownClock.setListener(
                    new CountdownClock.TickListener() {
                        @Override
                        public void onTick(
                                long countdownMs,
                                long nextOpenTime,
                                int nextFetchIntervalSec,
                                boolean fastMode) {
                            // Broadcast the fresh clock sample before any watchdog queue admission.
                            // A blocked DB watchdog must not suppress or re-stamp countdown ticks.
                            if (running) broadcastTick(countdownMs);
                            postCountdownWorkR964(countdownMs);
                        }
                    });
            if (finishCancelledRuntimeInitializationR94()) return;
            runtimeInitialized = true;
            runtimeInitializationRunning = false;
            runtimeInitializationError = "";
            runtimeInitRetryCountR323 = 0;
            try {
                heartbeatStateV8()
                        .markBusinessStage("SERVICE_INIT_READY", AppConfig.B07R32_VERSION);
                heartbeatStateV8().markInitStage("COMMAND_REPLAY_PENDING", safe(pendingStartAction));
                if (store != null) {
                    store.setState("r32_service_startup_policy", AppConfig.B07R32_STARTUP_POLICY);
                    store.setState("r32_service_init_status", "READY");
                }
            } catch (Throwable ignore) {
            }
            Handler replayOwner = runtimeGuardHandler != null ? runtimeGuardHandler : startupInitHandler;
            if (replayOwner != null) {
                replayOwner.post(
                        new Runnable() {
                            @Override
                            public void run() {
                                replayPendingStartAfterInitR32();
                            }
                        });
            } else {
                replayPendingStartAfterInitR32();
            }
        } catch (Throwable initError) {
            runtimeInitializationRunning = false;
            runtimeInitialized = false;
            runtimeInitializationError =
                    isOomR3610(initError) ? "OutOfMemoryError" : safeError(initError);
            resetPartialRuntimeAfterInitFailureR363();
            CrashRecorder.recordCaught(
                    getApplicationContext(), initError, "SERVICE_HEAVY_INIT_BACKGROUND_R32");
            try {
                heartbeatStateV8()
                        .markBusinessStage("SERVICE_INIT_FAILED", runtimeInitializationError);
                heartbeatStateV8()
                        .markRecoveryResult(
                                "service_heavy_init", false, runtimeInitializationError);
            } catch (Throwable ignore) {
            }
            try {
                heartbeatStateV8().markInitFailure(runtimeInitializationError);
            } catch (Throwable ignore) {
            }
            try {
                if (store != null) {
                    store.setState("r32_service_init_status", "FAILED");
                    store.setState("r32_service_init_error", runtimeInitializationError);
                    store.setState(
                            "r323_service_init_retry_count",
                            String.valueOf(runtimeInitRetryCountR323 + 1));
                }
            } catch (Throwable ignore) {
            }
            new Handler(getMainLooper())
                    .post(
                            new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        startForeground(
                                                AppConfig.NOTIFICATION_ID,
                                                buildNotification("启动保护", "后台初始化失败，正在自动重试"));
                                    } catch (Throwable ignore) {
                                    }
                                }
                            });
            scheduleRuntimeInitializationRetryR323(runtimeInitializationError);
        }
    }

    private void replayPendingStartAfterInitR32() {
        if (!runtimeInitialized || runtimeInitializationCancelled) return;
        String action = pendingStartAction;
        if (action == null || action.length() == 0) action = AppConfig.ACTION_SERVICE_START;
        if (!AppConfig.ACTION_SERVICE_STOP.equals(action)
                && (userRequestedStopV5 || !heartbeatStateV8().isUserEnabled())) {
            runtimeInitializationCancelled = true;
            BootReceiver.cancelAllRecovery(getApplicationContext(), "USER_DISABLED_LATE_START_REJECTED");
            stopSelf();
            return;
        }
        Intent replay = new Intent(this, AutoFetchService.class);
        replay.setAction(action);
        replay.setPackage(AppConfig.PACKAGE_NAME);
        onStartCommand(replay, 0, pendingStartId);
    }

    @Override
    public int onStartCommand(final Intent intent, final int flags, final int startId) {
        if (runtimeInitialized && runtimeGuardHandler != null
                && android.os.Looper.myLooper() == getMainLooper()) {
            final boolean stop = intent != null && AppConfig.ACTION_SERVICE_STOP.equals(intent.getAction());
            if (stop) {
                userRequestedStopV5 = true;
                heartbeatStateV8().setUserEnabled(false);
            }
            boolean accepted = runtimeGuardHandler.post(new Runnable() {
                @Override public void run() { handleStartCommandR924(intent, flags, startId); }
            });
            if (accepted) return stop ? START_NOT_STICKY : START_STICKY;
            CrashRecorder.recordCaught(getApplicationContext(),
                    new IllegalStateException("R924_COMMAND_LOOPER_REJECTED"), "R924_SERVICE_COMMAND");
            new Thread(new Runnable() {
                @Override public void run() { handleStartCommandR924(intent, flags, startId); }
            }, "R924-Command-Recovery").start();
            return stop ? START_NOT_STICKY : START_STICKY;
        }
        return handleStartCommandR924(intent, flags, startId);
    }

    private int handleStartCommandR924(Intent intent, int flags, int startId) {
        String action = intent == null ? AppConfig.ACTION_SERVICE_START : intent.getAction();
        if (action == null || action.length() == 0) action = AppConfig.ACTION_SERVICE_START;
        if (!AppConfig.ACTION_SERVICE_STOP.equals(action)
                && (userRequestedStopV5 || !heartbeatStateV8().isUserEnabled())) {
            runtimeInitializationCancelled = true;
            BootReceiver.cancelAllRecovery(getApplicationContext(), "USER_DISABLED_LATE_START_REJECTED");
            stopSelf();
            return START_NOT_STICKY;
        }
        try {
            heartbeatStateV8().markOnStartCommand(action);
        } catch (Throwable ignore) {
        }
        if (AppConfig.ACTION_SERVICE_STOP.equals(action) && !runtimeInitialized) {
            runtimeInitializationCancelled = true;
            try {
                heartbeatStateV8().setUserEnabled(false);
                heartbeatStateV8().markStop("user_stop_during_init");
            } catch (Throwable ignore) {
            }
            try {
                BootReceiver.cancelAllRecovery(getApplicationContext(), "user_stop_during_init");
            } catch (Throwable ignore) {
            }
            stopSelf();
            return START_NOT_STICKY;
        }
        if (!runtimeInitialized) {
            pendingStartAction = action;
            pendingStartId = startId;
            try {
                // User enable state is owned by explicit UI start.
                heartbeatStateV8().writeHeartbeat("AutoFetchService:R323_COMMAND_DEFERRED");
                heartbeatStateV8().renewRuntimeLease("R323_COMMAND_DEFERRED");
                heartbeatStateV8().markBusinessStage("SERVICE_COMMAND_DEFERRED_INIT", safe(action));
            } catch (Throwable ignore) {
            }
            try {
                // Explicit user enable state is not changed by automatic service callbacks.
                BootReceiver.scheduleAllRecovery(getApplicationContext(), "r323_command_deferred");
            } catch (Throwable ignore) {
            }
            ensureRuntimeInitializationR32();
            return START_STICKY;
        }
        writeBusinessHeartbeatState("last_service_command_at", action, "SERVICE_COMMAND");
        try {
            heartbeatStateV8().markOnStartCommand(action);
        } catch (Throwable ignore) {
        }

        if (AppConfig.ACTION_SERVICE_STOP.equals(action)) {
            userRequestedStopV5 = true;
            try {
                heartbeatStateV8().setUserEnabled(false);
                heartbeatStateV8().markStop("user_stop");
            } catch (Throwable e) {
            }
            try {
                BootReceiver.persistServiceUserEnabled(getApplicationContext(), false);
            } catch (Throwable e) {
            }
            cancelScheduledServiceRestartV5();
            try {
                BootReceiver.cancelAllRecovery(getApplicationContext(), "service_user_stop");
            } catch (Throwable e) {
            }
            stopWork();
            stopSelf();
            return START_NOT_STICKY;
        }

        userRequestedStopV5 = false;
        try {
            // Retain the current explicit user enable state.
        } catch (Throwable e) {
        }
        try {
            // Explicit user enable state is not changed by automatic service callbacks.
        } catch (Throwable e) {
        }
        try {
            BootReceiver.scheduleAllRecovery(getApplicationContext(), "onStartCommand");
            heartbeatStateV8().setLastReason("V40_ALWAYS_ON_START_COMMAND");
        } catch (Throwable e) {
        }

        if (AppConfig.ACTION_REFRESH_NOW.equals(action)) {
            forceRefresh = true;
            startWork();
            postFetchNow();
            postAnalysisForLatest("手动立即刷新，强制补齐目标期预测", true);
            postLearningTick("手动刷新后后台学习检查", true);
            postFullDiagnosisRefresh("手动刷新后刷新满级诊断闭环", FULL_DIAG_LOOP_IDLE_DELAY_MS, true);
            return START_STICKY;
        }

        if (ACTION_BACKTEST_FAST_NOW.equals(action)) {
            startWork();
            runManualBacktest(AppConfig.BACKTEST_MODE_FAST);
            return START_STICKY;
        }

        if (ACTION_BACKTEST_NORMAL_NOW.equals(action)) {
            startWork();
            runManualBacktest(AppConfig.BACKTEST_MODE_NORMAL);
            return START_STICKY;
        }

        if (ACTION_BACKTEST_FULL_NOW.equals(action)
                || AppConfig.ACTION_BACKTEST_FULL_REQUEST.equals(action)) {
            startWork();
            runManualBacktest(AppConfig.BACKTEST_MODE_FULL);
            return START_STICKY;
        }

        startWork();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        teardownThreadsR82 = new Thread[] { startupInitThread,fetchThread,runtimeGuardThread,
                livenessThreadR817,analysisWatchdogThread,analysisThread,backfillThread,backtestThread,
                learningThread,settlementRecoveryThreadR908,countdownThreadR973,researchThreadR908,microThread };
        try {
        cancelExitedAnalysisReleasesR77();
        RuntimeOwnerAuditR815.unregister("service-main", serviceMainThreadR3610);
        RecoveryCoordinatorR815.markServiceCompleted(getApplicationContext());
        try {
            heartbeatStateV8().markDestroy(userRequestedStopV5 ? "USER_STOP" : "SYSTEM_DESTROY");
        } catch (Throwable ignore) {
        }
        boolean shouldRestart = !userRequestedStopV5 && isServiceUserEnabledV5();
        if (shouldRestart) {
            scheduleServiceRestartV5("onDestroy");
            try {
                BootReceiver.scheduleWatchdog(getApplicationContext(), "onDestroy_fast", 2000L);
                BootReceiver.scheduleWatchdog(
                        getApplicationContext(),
                        "onDestroy",
                        AppConfig.SERVICE_FAST_RECOVERY_ALARM_MS);
                BootReceiver.scheduleJobWatchdog(getApplicationContext(), "onDestroy");
                heartbeatStateV8().setLastReason("DESTROY_V40_RECOVERY_ARMED");
            } catch (Throwable e) {
            }
        }
        runtimeInitializationCancelled = true;
        try {
            if (startupHeartbeatHandlerR323 != null)
                startupHeartbeatHandlerR323.removeCallbacksAndMessages(null);
        } catch (Throwable ignore) {
        }
        if (runtimeInitialized) stopWork();
        quitThread(startupInitThread);
        quitThread(fetchThread);
        quitThread(runtimeGuardThread);
        quitThread(livenessThreadR817);
        quitThread(analysisWatchdogThread);
        quitThread(analysisThread);
        quitThread(backfillThread);
        quitThread(backtestThread);
        quitThread(learningThread);
        quitThread(settlementRecoveryThreadR908);
        quitThread(countdownThreadR973);
        quitThread(researchThreadR908);
        quitThread(microThread);
        try {
            if (store != null) store.close();
        } catch (Throwable e) {
            CrashRecorder.recordCaught(getApplicationContext(), e, "R815_SERVICE_STORE_CLOSE");
        }
        try {
            RuntimeOwnerAuditR815.assertDestroyed();
        } catch (RuntimeException ownerFailure) {
            CrashRecorder.recordCaught(getApplicationContext(), ownerFailure, "R815_OWNER_DESTROY");
        }
        super.onDestroy();
    
        } finally { if(activeInstanceR82==this)activeInstanceR82=null; lifecycleActiveR82 = false; }
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        // Android calls this on the main looper. Database/recovery work belongs to our worker.
        if (!userRequestedStopV5 && running && runtimeGuardHandler != null) {
            runtimeGuardHandler.post(new Runnable() {
                @Override public void run() {
                    if (!running || userRequestedStopV5 || !isServiceUserEnabledV5()) return;
                    ensureRuntimeLoopsAliveV17("R924_TASK_REMOVED");
                    postFetchNow();
                    postAnalysisForLatest("R924_TASK_REMOVED", true);
                    ensureR908BackgroundLoops();
                    BootReceiver.scheduleAllRecovery(getApplicationContext(), "R924_TASK_REMOVED");
                    heartbeatStateV8().markTaskRemoved();
                    java.util.HashMap<String,String> state = new java.util.HashMap<String,String>();
                    state.put("r922_task_removed_reconcile", "R924_WORKER_RECONCILED");
                    state.put("r922_task_removed_reconcile_at", String.valueOf(System.currentTimeMillis()));
                    store.trySetTelemetryStatesR77(state);
                }
            });
        }
        super.onTaskRemoved(rootIntent);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private final Runnable serviceHeartbeatLoopV5 =
            new Runnable() {
                @Override
                public void run() {
                    if (!running) return;
                    try {
                        lastServiceHeartbeatAtV5 = System.currentTimeMillis();
                        heartbeatStateV8().writeHeartbeat("AutoFetchService:B07_GUARD");
                        heartbeatStateV8().markRuntimeGuardPulse("B07_RUNTIME_GUARD");
                        lastRuntimeGuardAtB07 = System.currentTimeMillis();
                        ensureRuntimeLoopsAliveV17("heartbeat");
                    } catch (Throwable e) {
                        CrashRecorder.recordCaught(getApplicationContext(), e, "HEARTBEAT");
                    }
                    if (running && runtimeGuardHandler != null)
                        runtimeGuardHandler.postDelayed(
                                this, AppConfig.B07R2_RUNTIME_GUARD_INTERVAL_MS);
                }
            };

    private void postServiceHeartbeatV5() {
        if (runtimeGuardHandler == null) return;
        runtimeGuardHandler.removeCallbacks(serviceHeartbeatLoopV5);
        runtimeGuardHandler.post(serviceHeartbeatLoopV5);
    }

    private boolean isServiceUserEnabledV5() {
        try {
            return heartbeatStateV8().isUserEnabled();
        } catch (Throwable e) {
            return true;
        }
    }

    private int serviceRestartPendingIntentFlagsV5() {
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= 23) flags |= PendingIntent.FLAG_IMMUTABLE;
        return flags;
    }

    private PendingIntent buildServiceRestartPendingIntentV5() {
        Intent i = new Intent(getApplicationContext(), AutoFetchService.class);
        i.setAction(AppConfig.ACTION_SERVICE_START);
        if (Build.VERSION.SDK_INT >= 26)
            return PendingIntent.getForegroundService(
                    getApplicationContext(), 9107, i, serviceRestartPendingIntentFlagsV5());
        return PendingIntent.getService(
                getApplicationContext(), 9107, i, serviceRestartPendingIntentFlagsV5());
    }

    private void scheduleServiceRestartV5(String reason) {
        if (userRequestedStopV5 || !isServiceUserEnabledV5()) return;
        try {
            heartbeatStateV8().markRecoveryRequest(reason, heartbeatStateV8().getHeartbeatAgeMs());
            BootReceiver.scheduleAllRecovery(getApplicationContext(), reason);
        } catch (Throwable e) {
            try {
                heartbeatStateV8().markRecoveryResult("service_schedule", false, safeError(e));
            } catch (Throwable ignore) {
            }
        }
    }

    private void cancelScheduledServiceRestartV5() {
        try {
            BootReceiver.cancelAllRecovery(getApplicationContext(), "service_user_stop");
        } catch (Throwable e) {
        }
    }

    /** 每次启动/恢复/正式裁决前都重新核验，避免数据库状态为空或旧状态造成整期FinalP=0。 */
    private String refreshBuildIdentityAndModuleState(String stage) {
        String identity = "BUILD_IDENTITY_ERROR:STORE_NULL";
        try {
            if (store == null) return identity;
            identity = BuildIdentity.verifyAndPersist(getApplicationContext(), store);
            store.setState("module_build_AppConfig", AppConfig.ACTIVE_QUALITY_BUILD_VERSION);
            store.setState("module_build_AutoFetchService", AppConfig.ACTIVE_QUALITY_BUILD_VERSION);
            store.setState("module_build_DbStore", AppConfig.ACTIVE_QUALITY_BUILD_VERSION);
            store.setState("field_chain_patch_version", AppConfig.ACTIVE_QUALITY_BUILD_VERSION);
            store.setState(
                    "field_chain_auto_downgrade_version", AppConfig.ACTIVE_QUALITY_BUILD_VERSION);
            store.setState("autofetch_build_identity_status", identity);
            store.setState("autofetch_build_identity_stage", safe(stage));
            store.registerModuleBuildV20(
                    "AppConfig", AppConfig.ACTIVE_QUALITY_BUILD_VERSION, safe(stage));
            store.registerModuleBuildV20(
                    "AutoFetchService", AppConfig.ACTIVE_QUALITY_BUILD_VERSION, safe(stage));
            store.registerModuleBuildV20(
                    "DbStore", AppConfig.ACTIVE_QUALITY_BUILD_VERSION, safe(stage));
        } catch (Throwable e) {
            try {
                if (store != null) store.setState("autofetch_build_identity_error", safeError(e));
            } catch (Throwable ignore) {
            }
        }
        return identity;
    }

    private void scheduleStartupMaintenanceR3611() {
        final Handler owner =
                settlementRecoveryHandlerR908 != null
                        ? settlementRecoveryHandlerR908
                        : (backfillHandler != null
                                ? backfillHandler
                                : (learningHandler != null ? learningHandler : analysisWatchdogHandler));
        if (owner == null || store == null) return;
        owner.postDelayed(
                new Runnable() {
                    @Override
                    public void run() {
                        if (!running || runtimeInitializationCancelled || store == null) return;
                        try {
                            heartbeatStateV8()
                                    .markBusinessStage(
                                            "STARTUP_MAINTENANCE",
                                            "R817_BOUNDED_SETTLEMENT_BEGIN");
                            runSettlementBacklogMaintenanceR3611("STARTUP_R817");
                            int compacted = DrawRawPayloadCompactorR817.compactBatch(store, 16);
                            heartbeatStateV8()
                                    .markBusinessStage(
                                            "STARTUP_MAINTENANCE",
                                            "R817_BOUNDED_SETTLEMENT_DONE|rawCompacted="
                                                    + compacted);
                        } catch (Throwable e) {
                            heartbeatStateV8()
                                    .markBusinessStage(
                                            "STARTUP_MAINTENANCE_FAILED", safeError(e));
                        }
                    }
                },
                5000L);
    }

    /** Queue backlog work on the low-priority owner; fetch and watchdog threads never drain it. */
    private void scheduleSettlementBacklogMaintenanceR817(final String reason, long delayMs) {
        final Handler owner = settlementRecoveryHandlerR908 != null
                ? settlementRecoveryHandlerR908
                : (backfillHandler != null ? backfillHandler : learningHandler);
        if (owner == null || store == null || !running) return;
        if (!settlementMaintenancePostedR817.compareAndSet(false, true)) return;
        final boolean[] admittedR58 = {false};
        synchronized (admittedR58) {
            try {
                admittedR58[0] = owner.postDelayed(
                        new Runnable() {
                            @Override public void run() {
                                synchronized (admittedR58) {
                                    if (!admittedR58[0]) return;
                                }
                                try {
                                    // The existing sole settlement owner retains all durable work.
                                    if (running) runSettlementBacklogMaintenanceR3611(reason);
                                    if (running && !RealtimePressureGateR811.hasActiveCriticalWork())
                                        DrawRawPayloadCompactorR817.compactBatch(store, 8);
                                } finally {
                                    settlementMaintenancePostedR817.set(false);
                                }
                            }
                        }, Math.max(0L, delayMs));
            } catch (RuntimeException dispatchFailureR58) {
                try { store.setState("r58_settlement_dispatch_error", safeError(dispatchFailureR58)); }
                catch (Throwable ignoredTelemetryR58) {}
            } finally {
                // A quitting/rejecting Handler owns no callback. Let the watchdog retry.
                if (!admittedR58[0]) settlementMaintenancePostedR817.set(false);
            }
        }
    }

    private void runSettlementBacklogMaintenanceR3611(String reason) {
        runSettlementBacklogMaintenanceR912(
                reason, RealtimePressureGateR811.hasActiveCriticalWork() ? 700L : 2000L);
    }

    /**
     * R912 bounded correctness slice.  A live-current retry keeps priority, but it may no longer
     * turn into a global boolean lock that starves hundreds of already-runnable older issues.
     */
    private void runSettlementBacklogMaintenanceR912(String reason, long budgetMs) {
        if (!settlementMaintenanceRunningR3611.compareAndSet(false, true)) return;
        try {
            if (store == null || settlementEngine == null) return;
            // R906: one supervisor owns all historical recovery/catch-up.  The current exact draw
            // path never scans or drains historical rows.  Keep this maintenance lane bounded.
            int retired = 0; // R908 supervisor performs P2 retirement only after ready rows drain.
            int supervised = SettlementRecoverySupervisorR906.shared().pulse(
                    store, Math.max(100L, Math.min(2000L, budgetMs)), reason);
            int reopened = 0;
            int seeded = 0;
            int drainedReady = supervised;
            int drainedLive = 0;
            int drainedBacklog = 0;
            int drained = supervised;
            SettlementBacklogAuditR3611.Summary audit =
                    SettlementBacklogAuditR3611.scan(store, 256);
            store.setState("r3611_shadow_backlog_total", String.valueOf(audit.total));
            store.setState("r3611_shadow_ready_to_settle", String.valueOf(audit.ready));
            store.setState("r3611_shadow_wait_draw", String.valueOf(audit.waitDraw));
            store.setState("r3611_shadow_lease_active", String.valueOf(audit.leaseActive));
            store.setState("r3611_shadow_pit_invalid", String.valueOf(audit.pitInvalid));
            store.setState("r3611_shadow_data_corrupt", String.valueOf(audit.dataCorrupt));
            store.setState("r3611_shadow_oldest_pending_issue", safe(audit.oldestTarget));
            store.setState("r3611_shadow_backlog_at", String.valueOf(System.currentTimeMillis()));
            store.setState("r3611_shadow_backlog_reason", safe(reason));
            store.setState("r3611_shadow_seeded", String.valueOf(seeded));
            store.setState("r905_false_no_predraw_reopened", String.valueOf(reopened));
            store.setState("r819_legacy_backlog_retired", String.valueOf(retired));
            store.setState("r905_ready_predraw_drained", String.valueOf(drainedReady));
            store.setState("r3611_shadow_drained", String.valueOf(drained));
            store.setState("r816_shadow_live_retry_drained", String.valueOf(drainedLive));
            store.setState("r816_shadow_backlog_drained", String.valueOf(drainedBacklog));
            store.setState("r3611_shadow_backlog_read_error", safe(audit.readError));
        } catch (Throwable e) {
            try {
                if (store != null)
                    store.setState("r3611_shadow_backlog_error", safeError(e));
            } catch (Throwable ignore) {
            }
        } finally {
            settlementMaintenanceRunningR3611.set(false);
        }
    }

    private void persistProviderFreshnessR3611(String stage) {
        if (store == null) return;
        try {
            java.util.LinkedHashMap<String,String> values=new java.util.LinkedHashMap<String,String>();
            ProviderFreshnessGateR3611.Snapshot snap = ProviderFreshnessGateR3611.snapshot();
            ProviderFreshnessGateR3611.Candidate p = snap.primary;
            ProviderFreshnessGateR3611.Candidate b = snap.backup;
            ProviderFreshnessGateR3611.Candidate x = snap.selected;
            values.put("r3611_provider_primary_issue", safe(p.issue));
            values.put("r3611_provider_primary_age_ms", String.valueOf(p.ageMs));
            values.put("r3611_provider_primary_code", safe(p.code));
            values.put("r3611_provider_backup_issue", safe(b.issue));
            values.put("r3611_provider_backup_age_ms", String.valueOf(b.ageMs));
            values.put("r3611_provider_backup_code", safe(b.code));
            values.put("r3611_provider_selected_issue", safe(x.issue));
            values.put("r3611_provider_selected_line", safe(x.sourceLine));
            values.put("r3611_provider_selected_age_ms", String.valueOf(x.ageMs));
            values.put("r3611_provider_selected_code", safe(x.code));
            values.put("r3611_provider_stage", safe(stage));
            values.put("r3611_provider_at", String.valueOf(System.currentTimeMillis()));
            store.setStates(values);
        } catch (Throwable ignore) {
        }
    }

    /**
     * A validated provider response is a fetch success even when it repeats the current issue.
     * R816 only refreshed this beat after the save/advance path, so restart diagnostics could
     * report FETCH_STALE while the selected provider sample was demonstrably fresh.
     */
    private boolean recordValidatedFetchResponseR817(
            Models.Draw draw, String expectedTargetIssue, String sourceLine) {
        if (draw == null || !draw.isValid()) return false;
        long now = System.currentTimeMillis();
        ProviderFreshnessGateR3611.Decision freshness =
                ProviderFreshnessGateR3611.evaluate(draw, expectedTargetIssue, now);
        if (!freshness.accepted) return false;
        try {
            heartbeatStateV8().markFetchSuccess(draw.issue);
        } catch (Throwable ignore) {
        }
        b13RecordBusinessBeat(store, "FETCH_RESPONSE_VALIDATED", draw.issue);
        writeBusinessHeartbeatState(
                "last_fetch_success_at", draw.issue, "FETCH_RESPONSE_VALIDATED");
        try {
            if (store != null) {
                java.util.LinkedHashMap<String,String> receipt=new java.util.LinkedHashMap<String,String>();
                receipt.put("r817_fetch_receipt_issue",safe(draw.issue));
                receipt.put("r817_fetch_receipt_source",safe(sourceLine));
                receipt.put("r817_fetch_receipt_age_ms",String.valueOf(freshness.ageMs));
                receipt.put("r817_fetch_receipt_at",String.valueOf(now));
                store.setStates(receipt);
            }
        } catch (Throwable ignore) {
        }
        return true;
    }

    /** R908 correctness heartbeat: settlement recovery owns an independent, continuously re-posted
     * serial lane. It never waits for Teacher/Research/Deferred and it runs even when the watchdog
     * happens to observe realtime pressure. Each pulse remains bounded and single-flight. */
    private final Runnable settlementRecoveryLoopR908 = new Runnable() {
        @Override public void run() {
            if (!running || store == null || settlementRecoveryHandlerR908 == null) return;
            try {
                boolean exactPending = currentExactSettlementPendingR908.get();
                // Current exact remains the highest-priority task in the durable queue.  A small
                // interleaved slice nevertheless guarantees that a stuck/retrying current task
                // cannot freeze all older exact-draw + pre-draw rows forever.
                runSettlementBacklogMaintenanceR912(
                        exactPending
                                ? "R912_CURRENT_EXACT_PRIORITY_WITH_BACKLOG_FAIRNESS"
                                : "R912_CONTINUOUS_CORRECTNESS",
                        exactPending ? 260L : 1800L);
                // A failed live event remains durable after startup. Retry it on this existing
                // serial background lane, independently of the next live event's exact drain.
                RuntimeExternalEventWriterR926.recoverPendingWhenIdleR81(store);
                store.setState("r908_settlement_loop_at", String.valueOf(System.currentTimeMillis()));
                store.setState("r908_settlement_loop_status",
                        currentExactSettlementPendingR908.get()
                                ? "CURRENT_EXACT_PRIORITY_AND_BACKLOG_SLICE:" + safe(currentExactSettlementIssueR908)
                                : "PULSE_DONE");
                heartbeatStateV8().markTeacherPump(
                        "V51_OWNER_OK",
                        "SettlementRecovery+postdraw-sidecars|R922");
            } catch (Throwable e) {
                try { store.setState("r908_settlement_loop_status", "ERROR:" + safeError(e)); }
                catch (Throwable ignore) {}
                try { heartbeatStateV8().markTeacherPump("V51_OWNER_ERROR", safeError(e)); }
                catch (Throwable ignore) {}
            } finally {
                if (running && settlementRecoveryHandlerR908 != null)
                    settlementRecoveryHandlerR908.postDelayed(this, R908_SETTLEMENT_PULSE_MS);
            }
        }
    };

    /** R908 Research heartbeat is deliberately independent from the production watchdog.  If a
     * pulse collides with live analysis/settlement, the Research tick records YIELD and the next
     * self-pulse retries; this prevents the R907 WAIT_TICK/IDLE starvation mode. */
    private final Runnable researchSelfLoopR908 = new Runnable() {
        @Override public void run() {
            if (!running || researchHandlerR908 == null) return;
            try { postAutonomousResearchTickR908("R908_SELF_HEARTBEAT"); }
            catch (Throwable ignore) {}
            if (running && researchHandlerR908 != null)
                researchHandlerR908.postDelayed(this, R908_RESEARCH_SELF_PULSE_MS);
        }
    };

    private static final int R915_CURRENT_EXACT_IMMEDIATE_RETRY_LIMIT = 8;

    private void handoffCurrentExactSettlementR915(Models.Draw draw, String detail) {
        if (draw == null || !draw.isValid() || store == null) return;
        boolean durable = SettlementTaskStoreR36.handoffCurrentExactR914(store, draw);
        currentExactSettlementPendingR908.set(false);
        currentExactSettlementIssueR908 = "";
        try {
            store.setState("r908_current_exact_retry_status",
                    "R915_DURABLE_HANDOFF:" + (durable ? "QUEUED" : "ALREADY_OWNED") + ':' + safe(detail));
            store.setState("r908_current_exact_retry_issue", draw.issue);
        } catch (Throwable ignored) {}
        scheduleSettlementBacklogMaintenanceR817("R915_CURRENT_EXACT_DURABLE_HANDOFF", 0L);
    }

    private void postCurrentExactSettlementRetryR908(final Models.Draw draw, final int attempt) {
        final Handler owner = settlementRecoveryHandlerR908;
        if (!running || owner == null || store == null || draw == null || !draw.isValid()) return;
        Runnable task = new Runnable() {
            @Override public void run() {
                if (!running || store == null) return;
                try {
                    SettlementEngine.SettlementReport report =
                            SettlementCoordinatorR815.closeCurrentExactR906(
                                    store, draw, SettlementCoordinatorR815.Reason.RECONCILER);
                    SettlementTaskStoreR36.Terminal terminal = SettlementTaskStoreR36.terminal(store, draw.issue);
                    if (terminal != null) {
                        if (SettlementTaskStoreR36.COMPLETED.equals(terminal.status)
                                || SettlementTaskStoreR36.NO_PREDRAW_CANONICAL_TERMINAL.equals(terminal.status)) {
                            currentExactSettlementPendingR908.set(false);
                            currentExactSettlementIssueR908 = "";
                            store.setState("r908_current_exact_retry_status", "CLOSED:" + terminal.status);
                            store.setState("r908_current_exact_retry_issue", draw.issue);
                            return;
                        }
                        if (SettlementTaskStoreR36.FAILED_TERMINAL.equals(terminal.status)
                                && SettlementTaskStoreR36.reopenCurrentRecoverableFailedR911(store, draw.issue)) {
                            terminal = null;
                            store.setState("r908_current_exact_retry_status",
                                    "R911_REOPENED_RETRYABLE_FAILED:" + attempt);
                        } else if (SettlementTaskStoreR36.INVALID_DRAW_TERMINAL.equals(terminal.status)
                                || SettlementTaskStoreR36.FAILED_TERMINAL.equals(terminal.status)) {
                            currentExactSettlementPendingR908.set(false);
                            currentExactSettlementIssueR908 = "";
                            store.setState("r908_current_exact_retry_status",
                                    "R911_BLOCKED_PERMANENT_TERMINAL:" + terminal.status + ':' + terminal.error);
                            return;
                        }
                    }
                    String detail = report == null ? "NULL" : safe(report.detail);
                    if (terminal == null) {
                        String activeIssue = safe(currentExactSettlementIssueR908);
                        if (activeIssue.length() > 0 && !draw.issue.equals(activeIssue)
                                && TargetIssueResolver.compare(activeIssue, draw.issue) > 0) {
                            currentExactSettlementPendingR908.set(false);
                            store.setState("r908_current_exact_retry_status",
                                    "R911_SUPERSEDED_BY_NEWER_DRAW:" + activeIssue);
                            return;
                        }
                        if (attempt >= R915_CURRENT_EXACT_IMMEDIATE_RETRY_LIMIT) {
                            handoffCurrentExactSettlementR915(draw,
                                    "BOUNDED_RETRY_EXHAUSTED:" + attempt + ':' + detail);
                            return;
                        }
                        store.setState("r908_current_exact_retry_status",
                                "R911_RETRY_UNTIL_TERMINAL:" + attempt + ':' + detail);
                        // One strictly bounded fairness slice per retry.  The supervisor orders
                        // LIVE_CURRENT first, so this can also close the exact row itself.
                        runSettlementBacklogMaintenanceR912(
                                "R912_AFTER_CURRENT_EXACT_ATTEMPT_" + attempt, 180L);
                        final int nextAttempt = attempt + 1;
                        owner.postDelayed(new Runnable() {
                            @Override public void run() {
                                postCurrentExactSettlementRetryR908(draw, nextAttempt);
                            }
                        }, Math.min(1200L, 100L + 25L * Math.min(40, attempt)));
                    }
                } catch (Throwable e) {
                    try { store.setState("r908_current_exact_retry_status", "ERROR:" + safeError(e)); }
                    catch (Throwable ignore) {}
                    if (attempt >= R915_CURRENT_EXACT_IMMEDIATE_RETRY_LIMIT) {
                        handoffCurrentExactSettlementR915(draw,
                                "BOUNDED_ERROR_RETRY_EXHAUSTED:" + attempt + ':' + safeError(e));
                        return;
                    }
                    final int nextAttempt = attempt + 1;
                    owner.postDelayed(new Runnable() {
                        @Override public void run() {
                            postCurrentExactSettlementRetryR908(draw, nextAttempt);
                        }
                    }, Math.min(1500L, 140L + 30L * Math.min(40, attempt)));
                }
            }
        };
        if (attempt == 0) owner.postAtFrontOfQueue(task); else owner.post(task);
    }

    private void startWork() {
        if (Thread.currentThread() == serviceMainThreadR3610) {
            final Handler owner = runtimeGuardHandler != null ? runtimeGuardHandler : startupInitHandler;
            if (owner != null) {
                synchronized (this) {
                    if (startWorkDispatchQueuedR3610) return;
                    startWorkDispatchQueuedR3610 = true;
                }
                owner.post(
                        new Runnable() {
                            @Override
                            public void run() {
                                startWorkDispatchQueuedR3610 = false;
                                startWorkR3610OffMain();
                            }
                        });
                return;
            }
        }
        startWorkR3610OffMain();
    }

    private void startWorkR3610OffMain() {
        if (userRequestedStopV5 || !heartbeatStateV8().isUserEnabled()) {
            stopSelf();
            return;
        }
        if (running) {
            refreshBuildIdentityAndModuleState("startWork_running");
            forceRefresh = true;
            ensureRuntimeLoopsAliveV17("startWork_running");
            postFetchNow();
            postAnalysisForLatest("服务已运行，优先最新目标期", true);
            if (!AppConfig.ENABLE_V51_TIANSHU)
                postMicroWalkForward("startWork_running", true, 1200L);
            ensureAnalysisWatchdog();
            ensureR908BackgroundLoops();
            return;
        }
        running = true;
        forceRefresh = true;
        startLightweightHeartbeatR817();
        refreshBuildIdentityAndModuleState("startup");
        try {
            if (store != null) {
                store.setState("realtime_analysis_busy_v20", "0");
                store.setState("service_foreground_main_loop", "1");
            }
        } catch (Throwable ignore) {
        }
        try {
            // Retain the current explicit user enable state.
            heartbeatStateV8().markServiceReady("startWork");
        } catch (Throwable ignore) {
        }
        scheduleStartupMaintenanceR3611();
        try {
            // Explicit user enable state is not changed by automatic service callbacks.
        } catch (Throwable ignore) {
        }
        postServiceHeartbeatV5();
        try {
            BootReceiver.scheduleAllRecovery(getApplicationContext(), "startWork");
        } catch (Throwable ignore) {
        }
        countdownClock.start();
        setAnalysisState("IDLE", 0, "等待新期开奖或最新目标期");
        setBacktestState("IDLE", "", 0, 0, "等待回测任务", "");
        setLearningState("IDLE", "等待后台学习任务");
        setTeacherMatrixState("IDLE", "等待实时后后台教师矩阵");
        setV20CauseChainState("IDLE", "等待实时后后台因果画像");
        setRealBetDiagnosisState("IDLE", "等待诊断刷新");
        setFullDiagnosisLoopState("IDLE", "等待诊断闭环刷新");
        updateRuntimeState(AppConfig.SERVICE_STATUS_RUNNING, "");
        RealtimePressureGateR811.pulseLiveRequest();
        postFetchNow();
        // Historical settlement seeding/diagnostic baseline are intentionally deferred to the
        // existing serial background owner; cold-start must never block the Android main thread.
        scheduleDeferredDrainR36();
        postAnalysisForLatest("R35_STARTUP_RECONCILE", true);
        postB07StartupBackfill();
        if (!AppConfig.ENABLE_V51_TIANSHU) {
            postBackgroundEvidencePumpV6(1800L, "startWork_low_priority");
            postReservedTeacherSliceAfterDecisionV8("");
            postLearningTick("服务启动后后台学习检查", true);
            postMicroWalkForward("服务启动严格OOS冷启动", true, 1200L);
        }
        postRealBetDiagnosisRefresh("启动后延迟诊断", AppConfig.DIAGNOSIS_INTERVAL_MS, false);
        postFullDiagnosisRefresh(
                "R923启动冻结快照与满级诊断", FULL_DIAG_LOOP_STARTUP_DELAY_MS, false);
        ensureRuntimeLoopsAliveV17("startWork_new");
        ensureAnalysisWatchdog();
        ensureR908BackgroundLoops();
    }

    private void ensureR908BackgroundLoops() {
        if (!running) return;
        if (settlementRecoveryHandlerR908 != null) {
            settlementRecoveryHandlerR908.removeCallbacks(settlementRecoveryLoopR908);
            settlementRecoveryHandlerR908.post(settlementRecoveryLoopR908);
        }
        if (researchHandlerR908 != null) {
            researchHandlerR908.removeCallbacks(researchSelfLoopR908);
            researchHandlerR908.postDelayed(researchSelfLoopR908, 1200L);
        }
        try {
            if (store != null) {
                store.setState("r908_background_loops", "ARMED");
                store.setState("r908_background_loops_at", String.valueOf(System.currentTimeMillis()));
            }
        } catch (Throwable ignore) {}
    }

    private static boolean isOomR3610(Throwable error) {
        if (error == null) return false;
        if (error instanceof OutOfMemoryError) return true;
        Throwable cause = error.getCause();
        return cause instanceof OutOfMemoryError;
    }

    private void handleRuntimeOomR3610(Throwable error, String stage) {
        // Low-allocation fail-closed path.  Do not build large diagnostic strings while heap is
        // exhausted; unwind the current task and let the serial owners recover after GC.
        analysisBusy = false;
        fetchBusy = false;
        lastError = "OutOfMemoryError";
        analysisOomBackoffUntilR3610 =
                Math.max(analysisOomBackoffUntilR3610, System.currentTimeMillis() + 60000L);
        RuntimeMemoryGuardR811.recoverAfterFailure();
        CrashRecorder.recordCaught(getApplicationContext(), error, stage);
        Handler owner = runtimeGuardHandler;
        if (running && owner != null) {
            owner.postDelayed(
                    new Runnable() {
                        @Override
                        public void run() {
                            if (!running) return;
                            try {
                                // Keep the lightweight business path alive immediately.  Analysis
                                // itself is gated by analysisOomBackoffUntilR3610.
                                postFetchNow();
                            } catch (Throwable ignore) {
                            }
                        }
                    },
                    5000L);
            owner.postDelayed(
                    new Runnable() {
                        @Override
                        public void run() {
                            if (!running) return;
                            try {
                                if (System.currentTimeMillis() >= analysisOomBackoffUntilR3610)
                                    postAnalysisForLatest("R3610_OOM_RECOVERY", true);
                            } catch (Throwable ignore) {
                            }
                        }
                    },
                    61000L);
        }
    }

    private void stopWork() {
        cancelExitedAnalysisReleasesR77();
        running = false;
        // Preserve the active legal pre-draw issue before callbacks are cleared.
        researchCaptureQueueR946.stop();
        forceRefresh = false;

        fetchBusy = false;
        analysisBusy = false;
        backfillBusy = false;
        backtestBusy = false;
        learningBusy = false;
        learningPostPending = false;
        microBusy = false;
        microPostPending = false;
        autoBacktestPostPending = false;
        teacherMatrixBusy = false;
        teacherMatrixPostPending = false;
        v20CauseChainBusy = false;
        v20CauseChainPostPending = false;
        realBetDiagnosisBusy = false;
        realBetDiagnosisPostPending = false;
        pendingRealBetDiagnosisReason = "";
        fullDiagnosisLoopBusy = false;
        fullDiagnosisLoopPostPending = false;
        pendingFullDiagnosisLoopForce = false;
        pendingFullDiagnosisLoopReason = "";
        backtestHeartbeatRunning = false;
        backgroundEvidencePumpPostedV6 = false;
        reservedTeacherSlicePostedV8 = false;
        realtimeHistory100Posted = false;
        deferredDrainPostedR367.set(false);
        settlementMaintenancePostedR817.set(false);
        currentExactSettlementPendingR908.set(false);
        currentExactSettlementIssueR908 = "";
        researchTickPostedR908.set(false);
        lastSettlementWatchdogAtR816 = 0L;
        lastAnalysisReconcileAtR816 = 0L;
        lastProductionMaintenanceAtR816 = 0L;
        lastResearchTickAtR816 = 0L;
        lastResearchSchemaAttemptAtR817 = 0L;
        lastB13ServicePulseAtR817 = 0L;
        b07StartupBackfillPosted = false;
        backtestYieldRequested = false;
        backtestStoppedForRealtime = false;
        realtimePriorityUntilAt = 0L;
        lastAnalysisDirectTakeoverAt = 0L;

        if (fetchHandler != null) {
            fetchHandler.removeCallbacksAndMessages(null);
        }
        if (runtimeGuardHandler != null) {
            runtimeGuardHandler.removeCallbacksAndMessages(null);
        }
        if (analysisWatchdogHandler != null) {
            analysisWatchdogHandler.removeCallbacksAndMessages(null);
            countdownWorkPostedR964.set(false);
        }
        if (livenessHandlerR817 != null) {
            livenessHandlerR817.removeCallbacks(lightweightHeartbeatLoopR817);
        }

        if (analysisHandler != null) {
            analysisHandler.removeCallbacksAndMessages(null);
        }

        if (backfillHandler != null) {
            backfillHandler.removeCallbacksAndMessages(null);
        }
        quickHistoryQueuedR71.set(false); // cancelled queued callback will not execute its finally

        if (backtestHandler != null) {
            backtestHandler.removeCallbacksAndMessages(null);
        }

        if (learningHandler != null) {
            learningHandler.removeCallbacksAndMessages(null);
        }
        if (settlementRecoveryHandlerR908 != null) settlementRecoveryHandlerR908.removeCallbacksAndMessages(null);
        if (researchHandlerR908 != null) researchHandlerR908.removeCallbacksAndMessages(null);
        if (microHandler != null) {
            microHandler.removeCallbacksAndMessages(null);
        }

        if (countdownClock != null) {
            countdownClock.stop();
        }

        setAnalysisState("IDLE", 0, "服务已停止");
        setLearningState("IDLE", "服务已停止");
        setTeacherMatrixState("IDLE", "服务已停止");
        setV20CauseChainState("IDLE", "服务已停止");
        setRealBetDiagnosisState("IDLE", "服务已停止");
        setFullDiagnosisLoopState("IDLE", "服务已停止");
        updateRuntimeState(AppConfig.SERVICE_STATUS_STOPPED, "");
    }

    private void ensureAnalysisWatchdog() {
        if (!running || analysisWatchdogHandler == null) {
            return;
        }
        analysisWatchdogHandler.removeCallbacks(analysisWatchdogLoop);
        analysisWatchdogHandler.postDelayed(analysisWatchdogLoop, ANALYSIS_WATCHDOG_INTERVAL_MS);
    }

    private void postFetchNow() {
        if (!running || fetchHandler == null) {
            return;
        }

        fetchHandler.removeCallbacks(fetchLoop);
        fetchHandler.post(fetchLoop);
    }

    private void scheduleNextFetchLoop() {
        if (!running || fetchHandler == null) {
            return;
        }

        long delay = FETCH_IDLE_DELAY_MS;

        if (isRealtimeLikeWindow()) {
            delay = FETCH_REALTIME_DELAY_MS;
        } else {
            try {
                if (countdownClock != null) {
                    long clockDelay = countdownClock.getNextFetchDelayMs();
                    if (clockDelay > 0L && clockDelay < delay) {
                        delay = clockDelay;
                    }
                }
            } catch (Throwable e) {
                delay = FETCH_IDLE_DELAY_MS;
            }
        }

        // Scheduling the next transport wake must be DB-free. Admission and commit still
        // validate the exact durable draw/target in their existing owners.
        if (analysisBusy
                || realtimePriorityUntilAt > System.currentTimeMillis()
                || (pendingAnalysisBaseIssue != null && pendingAnalysisBaseIssue.length() > 0)) {
            if (delay > 650L) {
                delay = 650L;
            }
        }

        if (delay < AppConfig.REALTIME_MIN_LOOP_INTERVAL_MS) {
            delay = AppConfig.REALTIME_MIN_LOOP_INTERVAL_MS;
        }

        if (delay > 3000L) {
            delay = 3000L;
        }

        fetchHandler.postDelayed(fetchLoop, delay);
    }

    private void runFetchChannelOnce() {
        if (!running || fetchBusy) {
            return;
        }

        fetchBusy = true;

        try {
            long now = System.currentTimeMillis();
            lastFetchLoopAt = now;

            boolean realtimeLike = isRealtimeLikeWindow();
            Models.Draw localLatestBeforeFetch = store == null ? null : store.getLatestDraw();
            boolean staleLatestBeforeFetch = isLatestDrawStaleForRecovery(localLatestBeforeFetch, now);
            boolean shouldFetch =
                    forceRefresh || realtimeLike || shouldClockFetchNow() || staleLatestBeforeFetch;

            if (!shouldFetch) {
                updateRuntimeState(AppConfig.SERVICE_STATUS_WAITING, "");
                postAnalysisForLatest("空闲期补齐正式预测", false);
                postLearningTick("空闲期后台小批量学习", false);
                postRealBetDiagnosisRefresh("空闲期刷新真实BET复盘诊断", REAL_BET_DIAG_IDLE_DELAY_MS, false);
                postFullDiagnosisRefresh("空闲期刷新满级诊断闭环", FULL_DIAG_LOOP_IDLE_DELAY_MS, false);
                return;
            }

            forceRefresh = false;

            if (!isNetworkAvailable()) {
                networkWasUnavailable = true;
                lastError = "网络不可用";
                updateRuntimeState(AppConfig.SERVICE_STATUS_DELAY, lastError);
                postAnalysisForLatest("网络异常，本地补齐正式预测", false);
                postLearningTick("网络异常时本地后台学习检查", false);
                return;
            }

            boolean recovered = networkWasUnavailable;
            networkWasUnavailable = false;

            // Fetch admission must not wait for the full status snapshot (backtest/learning
            // reads and app_state writes). Clock ticks already have their own DB-free owner;
            // publish the durable runtime snapshot after this network attempt completes.
            lastStatus = AppConfig.SERVICE_STATUS_FETCHING;
            lastError = "";

            Models.Draw oldLatest = localLatestBeforeFetch;
            Models.ApiResult latestResult = fetchLatestRealtime();

            if (countdownClock != null) {
                countdownClock.markFetched(latestResult != null && latestResult.ok);
            }

            if (latestResult == null || !latestResult.ok || latestResult.latestDraw == null) {
                lastError=ApiClient.failureSummary(latestResult);
                heartbeatStateV8().markFetchFailed(safeIssue(oldLatest),lastError);
                if (realtimeLike || forceRefresh || realtimeMissCount >= 1) {
                    realtimeMissCount++;

                    scheduleQuickHistorySyncR71("latest无返回，异步history100兜底");

                    postAnalysisForLatest("latest暂未返回，本地继续保持当前正式预测", false);
                    updateRuntimeState(AppConfig.SERVICE_STATUS_API_ERROR, lastError);
                } else {
                    postBackfillCheck(true, "latest异常，异步回补最新100");
                    postLearningTick("latest异常后本地后台学习检查", false);
                    updateRuntimeState(AppConfig.SERVICE_STATUS_API_ERROR, lastError);
                }
                return;
            }

            lastError = "";

            Models.Draw latestFromApi = latestResult.latestDraw;
            Models.DataCheck latestCheck = dataGuard.checkLatest(latestFromApi);

            if (latestCheck == null || !latestCheck.ok) {
                lastError = latestCheck == null ? "最新开奖校验失败" : latestCheck.reason;

                if (realtimeLike || forceRefresh) {
                    scheduleQuickHistorySyncR71("latest校验异常，异步history100兜底");
                }

                updateRuntimeState(AppConfig.SERVICE_STATUS_DATA_ERROR, lastError);
                postAnalysisForLatest("开奖校验异常，本地保持正式预测", false);
                return;
            }

            String expectedTarget =
                    oldLatest == null || !oldLatest.isValid()
                            ? ""
                            : dataGuard.buildNextIssue(oldLatest.issue);

            final Models.Draw latestForDispatchR57 = latestFromApi;
            final Models.Draw previousForDispatchR57 = oldLatest;
            final boolean recoveredForDispatchR57 = recovered;
            final String[] dispatchedIssueR57 = {""};
            // Submit the finite-window request and clock update as soon as the latest
            // draw is durable. Historical writes must not precede this callback.
            saveApiResultDraws(latestResult, new Runnable() {
                @Override public void run() {
                    Models.Draw durable = getAuthoritativeLatest(latestForDispatchR57);
                    handleLatestDrawAfterSave(durable, previousForDispatchR57,
                            recoveredForDispatchR57, "R57_DURABLE_LATEST_BEFORE_HISTORY");
                    if (durable != null) dispatchedIssueR57[0] = safe(durable.issue);
                }
            });

            // The current draw and finite-window analysis are already durable/dispatched.
            recordValidatedFetchResponseR817(latestFromApi, expectedTarget, latestResult.apiLine);
            persistProviderFreshnessR3611("LATEST");
            Models.Draw authoritativeLatest = getAuthoritativeLatest(latestFromApi);

            if (authoritativeLatest != null && authoritativeLatest.isValid()) {
                saveLatestDrawState(authoritativeLatest, "latest");
                broadcastDrawsUpdated(true);
            }

            List<Models.Draw> history = store.getRecentDraws(300);
            ArrayList<Models.Draw> cleanHistory = dataGuard.normalizeHistory(history);

            if (cleanHistory.size() > 0) {
                Models.Draw lastClean = cleanHistory.get(cleanHistory.size() - 1);
                if (isDrawNewer(lastClean, authoritativeLatest)) {
                    authoritativeLatest = lastClean;
                }
                countdownClock.syncByHistory(cleanHistory);
            } else {
                countdownClock.syncByLatestDraw(authoritativeLatest, cleanHistory);
            }

            if (authoritativeLatest != null
                    && !safe(authoritativeLatest.issue).equals(dispatchedIssueR57[0])) {
                handleLatestDrawAfterSave(authoritativeLatest, oldLatest, recovered, "latest");
            }

            boolean staleRecoveryAttempted = false;
            if (authoritativeLatest != null
                    && authoritativeLatest.isValid()
                    && !dataGuard.isNewDraw(authoritativeLatest, oldLatest)
                    && isLatestDrawStaleForRecovery(authoritativeLatest, System.currentTimeMillis())
                    && shouldRunStaleLatestHistoryRecovery()) {
                staleRecoveryAttempted = true;
                scheduleQuickHistorySyncR71("开奖跨期滞后，异步history100校正");
            }

            if (authoritativeLatest != null
                    && authoritativeLatest.isValid()
                    && !dataGuard.isNewDraw(authoritativeLatest, oldLatest)
                    && realtimeLike
                    && !staleRecoveryAttempted) {
                realtimeMissCount++;

                if (shouldPostRealtimeHistory100()) {
                    scheduleQuickHistorySyncR71("实时窗口latest未见新期，异步history100兜底");
                }
            }

            updateRuntimeState(AppConfig.SERVICE_STATUS_RUNNING, "");
            refreshNotificationIfNeeded();
        } catch (Throwable e) {
            lastError = safeError(e);
            updateRuntimeState(AppConfig.SERVICE_STATUS_API_ERROR, lastError);
        } finally {
            fetchBusy = false;
        }
    }

    private void handleLatestDrawAfterSave(
            Models.Draw authoritativeLatest,
            Models.Draw oldLatest,
            boolean recovered,
            String source) {
        if (authoritativeLatest == null || !authoritativeLatest.isValid()) {
            return;
        }

        ProviderFreshnessGateR3611.Decision acceptedDraw = ProviderFreshnessGateR3611.evaluate(
                authoritativeLatest,AppConfig.nextIssueText(authoritativeLatest.issue),System.currentTimeMillis());
        if(!acceptedDraw.accepted){
            // Freshness controls the next prediction, not settlement of an already
            // committed draw. Keep the independent, durable settlement owner awake.
            queueConfirmedSettlementR57(authoritativeLatest);
            scheduleSettlementBacklogMaintenanceR817("R57_STALE_CONFIRMED_DRAW", 450L);
            lastError="PROVIDER_STALE_NO_ADVANCE|"+acceptedDraw.compact();
            heartbeatStateV8().markFetchFailed(authoritativeLatest.issue,lastError);
            updateRuntimeState(AppConfig.SERVICE_STATUS_API_ERROR,lastError);return;
        }

        // The draw is durable already. Deliver the clock before best-effort DB telemetry,
        // which may be waiting behind a research commit on a large device database.
        boolean isNew = dataGuard.isNewDraw(authoritativeLatest, oldLatest);
        if (countdownClock != null) {
            countdownClock.syncByLatestDraw(authoritativeLatest, null);
            broadcastTick(countdownClock.getCountdownMs());
        }
        if (isNew) {
            realtimePriorityUntilAt = System.currentTimeMillis() + REALTIME_PRIORITY_HOLD_MS;
            // R71: finite-window owners run before telemetry or optional history queries.
            try {
                postAnalysisForLatest("R71_DURABLE_DRAW_IMMEDIATE_ANALYSIS", true);
            } finally {
                queueConfirmedSettlementR57(authoritativeLatest);
            }
        }
        b13RecordBusinessBeat(store, "FETCH_SUCCEEDED", authoritativeLatest.issue);
        writeBusinessHeartbeatState(
                "last_fetch_success_at", authoritativeLatest.issue, "FETCH_SUCCEEDED");
        try {
            if (store != null)
                store.setState("v47_latest_fetch_issue", safe(authoritativeLatest.issue));
        } catch (Throwable ignore) {
        }
        try {
            heartbeatStateV8().markFetchSuccess(authoritativeLatest.issue);
        } catch (Throwable ignore) {
        }
        saveLatestDrawState(authoritativeLatest, source);
        broadcastDrawsUpdated(true);

        if (countdownClock != null) {
            try {
                List<Models.Draw> history = store == null ? null : store.getRecentDraws(300);
                ArrayList<Models.Draw> clean = dataGuard.normalizeHistory(history);

                if (isNew) {
                    countdownClock.onNewDrawArrived(authoritativeLatest, clean);
                } else {
                    countdownClock.syncByLatestDraw(authoritativeLatest, clean);
                }
            } catch (Throwable e) {
            }
        }

        /* R3.6 settlement and next-target analysis are owned by BusinessReconcilerR36 below. */
        postRealBetDiagnosisRefresh("开奖保存后低频刷新真实BET诊断", AppConfig.DIAGNOSIS_INTERVAL_MS, false);
        postFullDiagnosisRefresh("开奖保存后低频刷新满级诊断", AppConfig.DIAGNOSIS_INTERVAL_MS, false);

        String baseIssue = authoritativeLatest.issue == null ? "" : authoritativeLatest.issue;
        String targetIssue = dataGuard.buildNextIssue(baseIssue);

        try {
            if (store != null) {
                java.util.LinkedHashMap<String, String> liveState =
                        new java.util.LinkedHashMap<String, String>();
                liveState.put("realtime_hard_isolate_mode", "1");
                liveState.put(
                        "realtime_hard_isolate_version",
                        V20_DYNAMIC_CAUSAL_AUTOFETCH_PATCH_VERSION);
                liveState.put("realtime_hard_isolate_target", targetIssue);
                liveState.put(
                        "realtime_hard_isolate_policy",
                        "前40候选实时小核裁决：当前窗口+概率+动态门+动态因果小核；V20/教师/动态因果长画像后台补完；不保护HOLD不快裁BET");
                liveState.put("analysis_target_issue", targetIssue);
                liveState.put("target_issue", targetIssue);
                liveState.put("current_issue", baseIssue);
                store.setStates(liveState);
            }
        } catch (Throwable ignoredTelemetry) {
        }

        if (isNew) {
            realtimeMissCount = 0;
            realtimeHistory100Posted = false;
            lastProcessedDrawIssue = baseIssue;

            if (store != null) {
                java.util.LinkedHashMap<String, String> newDrawState =
                        new java.util.LinkedHashMap<String, String>();
                newDrawState.put("last_new_draw_issue", baseIssue);
                newDrawState.put("last_new_draw_at", String.valueOf(System.currentTimeMillis()));
                newDrawState.put("fetch_new_draw_source", source == null ? "" : source);
                store.trySetTelemetryStatesR77(newDrawState);
            }

            requestRealtimePriority("新期开奖，先出正式预测，再做后台学习/回测");
            requestBacktestYield("新期开奖，实时正式分析优先");

            // Current-target analysis and settlement were dispatched at the durable callback entry.
            scheduleSettlementBacklogMaintenanceR817("NEW_DRAW_R906_AFTER_CURRENT_CORE", 450L);

            postAutoBacktestLazyIfIdle("新期开奖后延迟自动回测");
            postBackfillCheck(shouldBackfillAfterLatest(true, recovered), "新期开奖后异步回补检查");
        } else {
            if (isRealtimeLikeWindow()) {
                postAnalysisForLatest("实时窗口补齐当前目标期正式预测", false);
                updateRuntimeState(AppConfig.SERVICE_STATUS_FETCHING, "");
            } else {
                postAnalysisForLatest("普通轮询补齐正式预测", false);
                postLearningTick("普通轮询后台学习检查", false);
                postBackfillCheck(shouldBackfillAfterLatest(false, recovered), "普通轮询回补检查");
            }
        }
    }

    private void queueConfirmedSettlementR57(Models.Draw authoritativeLatest) {
        if (store == null || authoritativeLatest == null || !authoritativeLatest.isValid()) return;
        try {
            if (store != null) {
                currentExactSettlementPendingR908.set(true);
                currentExactSettlementIssueR908 = safe(authoritativeLatest.issue);
                boolean queued = SettlementCoordinatorR815.queueCurrentExactR914(
                        store, authoritativeLatest, SettlementCoordinatorR815.Reason.RECONCILER);
                SettlementTaskStoreR36.Terminal exactTerminal = SettlementTaskStoreR36.terminal(store, safe(authoritativeLatest.issue));
                boolean closed = exactTerminal != null
                        && (SettlementTaskStoreR36.COMPLETED.equals(exactTerminal.status)
                            || SettlementTaskStoreR36.NO_PREDRAW_CANONICAL_TERMINAL.equals(exactTerminal.status));
                if (closed) {
                    currentExactSettlementPendingR908.set(false);
                    currentExactSettlementIssueR908 = "";
                    store.setState("r908_current_exact_retry_status", "ALREADY_CLOSED:" + exactTerminal.status);
                } else {
                    store.setState("r908_current_exact_retry_status",
                            queued ? "R914_DURABLE_QUEUED_PRIORITY" : "R914_QUEUE_REJECTED_RETRY");
                    postCurrentExactSettlementRetryR908(authoritativeLatest, 0);
                }
                store.setState("r906_new_draw_current_exact", safe(authoritativeLatest.issue));
            }
        } catch (Throwable settlementFirstFailure) {
            try {
                store.setState("r905_new_draw_settlement_error", safeError(settlementFirstFailure));
                currentExactSettlementPendingR908.set(true);
                currentExactSettlementIssueR908 = safe(authoritativeLatest.issue);
                postCurrentExactSettlementRetryR908(authoritativeLatest, 0);
            } catch (Throwable ignored) {}
        }
    }

    private boolean isLatestDrawStaleForRecovery(Models.Draw draw, long now) {
        if (draw == null || !draw.isValid() || now <= 0L) return false;
        long openAt = draw.openTime;
        if (openAt > 0L && openAt < 100000000000L) openAt *= 1000L;
        if (openAt > 0L && openAt <= now) {
            long age = now - openAt;
            if (age >= STALE_LATEST_FORCE_FETCH_MS && age < 7L * 24L * 60L * 60L * 1000L)
                return true;
        }
        try {
            if (store != null) {
                long lastNewAt = parseLong(store.getState("last_new_draw_at", "0"), 0L);
                if (lastNewAt > 0L && now - lastNewAt >= STALE_LATEST_FORCE_FETCH_MS) return true;
            }
        } catch (Throwable ignore) {
        }
        return false;
    }

    private boolean shouldRunStaleLatestHistoryRecovery() {
        long now = System.currentTimeMillis();
        if (lastStaleLatestHistoryRecoveryAt > 0L
                && now - lastStaleLatestHistoryRecoveryAt
                        < STALE_LATEST_HISTORY_RECOVERY_MIN_INTERVAL_MS) return false;
        lastStaleLatestHistoryRecoveryAt = now;
        return true;
    }

    private boolean shouldClockFetchNow() {
        if (countdownClock == null) {
            return true;
        }

        try {
            return countdownClock.shouldFetchNow();
        } catch (Throwable e) {
            return true;
        }
    }

    private boolean isRealtimeLikeWindow() {
        if (countdownClock == null) {
            return false;
        }

        try {
            if (countdownClock.isRealtimeFetchWindow()) {
                return true;
            }

            long left = countdownClock.getCountdownMs();

            if (left <= 0L) {
                return true;
            }

            if (left <= AppConfig.REALTIME_OPEN_PREPARE_SEC * 1000L) {
                return true;
            }
        } catch (Throwable e) {
            return false;
        }

        return false;
    }

    private int getRealtimeProbabilityEnrichCap() {
        return REALTIME_PROBABILITY_ENRICH_CAP;
    }

    private int getRealtimeDecisionCandidateCap() {
        return REALTIME_DECISION_CANDIDATE_CAP;
    }

    private Models.ApiResult fetchLatestRealtime() {
        try {
            Models.Draw local = store == null ? null : store.getLatestDraw();
            String expectedTarget =
                    local == null || !local.isValid()
                            ? ""
                            : AppConfig.nextIssueText(local.issue);
            return apiClient.fetchLatest(expectedTarget);
        } catch (Throwable e) {
            lastError = safeError(e);
            return null;
        }
    }

    private void saveApiResultDraws(Models.ApiResult result) {
        saveApiResultDraws(result, null);
    }

    private void saveApiResultDraws(Models.ApiResult result, Runnable durableLatestConsumerR57) {
        if (result == null || store == null) {
            return;
        }

        int inserted = 0;
        boolean latestDurableR50 = false;

        try {
            // Latest settlement/clock progress must not wait behind historical backfill.
            if (result.latestDraw != null && result.latestDraw.isValid()) {
                Models.Draw previous = store.getDrawByIssue(result.latestDraw.issue);
                long row = DrawPollingReplayR67.alreadyDurable(previous,result.latestDraw)
                        && !FetchLedgerR925.hasDrawBindingR82(result.latestDraw) ? 0L : store.saveDraw(result.latestDraw);
                Models.Draw committed = store.getDrawByIssue(result.latestDraw.issue);
                latestDurableR50 = committed != null && committed.isValid()
                        && java.util.Arrays.equals(committed.numbers, result.latestDraw.numbers);
                if (!latestDurableR50) {
                    getAuthoritativeLatest(result.latestDraw); // exact pending state + existing retention owner
                    throw new IllegalStateException("R50_LATEST_DRAW_NOT_DURABLE|issue=" + result.latestDraw.issue);
                }
                if (row > 0L) inserted++;
                if (durableLatestConsumerR57 != null) durableLatestConsumerR57.run();
            }
            if (result.draws != null && !result.draws.isEmpty()) {
                try {
                    ArrayList<Models.Draw> changed = new ArrayList<Models.Draw>();
                    for(Models.Draw draw:result.draws) {
                        if(draw!=null&&(!DrawPollingReplayR67.alreadyDurable(
                                store.getDrawByIssue(draw.issue),draw)||FetchLedgerR925.hasDrawBindingR82(draw)))changed.add(draw);
                    }
                    if(!changed.isEmpty())inserted += store.saveDraws(changed);
                    store.setState("draw_history_backfill_error", "");
                } catch (RuntimeException historyFailure) {
                    if (!latestDurableR50) throw historyFailure;
                    // Keep the real failure visible; normal post-fetch backfill retries this page.
                    recordCriticalCatchR926("R50_HISTORY_BACKFILL_DEFERRED", result.latestDraw.issue, historyFailure);
                    store.setState("draw_history_backfill_error", safeError(historyFailure));
                }
            }
            store.setState("draw_save_error", "");

            if (result.apiLine != null && result.apiLine.length() > 0) {
                store.setState("api_line", result.apiLine);
            }

            Models.Draw latest = getAuthoritativeLatest(result.latestDraw);
            if (latest != null && latest.isValid()) {
                saveLatestDrawState(latest, "api_latest");
            }

            store.setState("draw_save_inserted", String.valueOf(inserted));
            store.setState("draw_save_updated_at", String.valueOf(System.currentTimeMillis()));
            broadcastDrawsUpdated(true);
        } catch (Throwable e) {
            lastError = safeError(e);
            try {
                if (store != null) store.setState("draw_save_error", lastError);
            } catch (Throwable diagnosticFailure) {
                recordCriticalCatchR926("DRAW_SAVE_FAILURE_STATE", "", diagnosticFailure);
            }
            throw new IllegalStateException("R48_DRAW_SAVE_RETRY_REQUIRED", e);
        }
    }

    private void postAnalysisForLatest(String reason, boolean force) {
        if (store == null) {
            return;
        }
        long nowR3610 = System.currentTimeMillis();
        final boolean oomBackoffR947 = analysisOomBackoffUntilR3610 > nowR3610;
        if (useR36Runtime()) {
            try {
                Models.Draw latest = store.getLatestDraw();
                if (latest != null && latest.isValid()) {
                    ProviderFreshnessGateR3611.Decision historyFreshnessR46 =
                            ProviderFreshnessGateR3611.evaluate(latest,
                                    AppConfig.nextIssueText(latest.issue), System.currentTimeMillis());
                    if (!historyFreshnessR46.accepted) {
                        setAnalysisState("WAITING", 0,
                                "历史待同步，禁止对旧目标期补写预测：" + historyFreshnessR46.compact());
                        postAutonomousResearchTickR908("R46_HISTORY_TAIL_RECOVERY");
                        postBackfillCheck(true, "R46历史滞后，补齐最近真实开奖");
                        return;
                    }
                    BusinessReconcilerR36.ReconcileResult r =
                            new BusinessReconcilerR36()
                                    .reconcile(
                                            store,
                                            latest,
                                            reason,
                                            new BusinessReconcilerR36.AnalysisRequester() {
                                                @Override
                                                public void request(
                                                        AnalysisSingleFlightR36.Request request) {
                                                    if (!oomBackoffR947) submitAnalysisRequestR36(request);
                                                    else {
                                                        setAnalysisState("WAITING",0,"R947请求已持久化；OOM熔断仅暂停执行：" + request.targetIssue);
                                                        try {
                                                            store.setState("r947_oom_request_preserved_target", request.targetIssue);
                                                            store.setState("r947_oom_request_preserved_until", String.valueOf(analysisOomBackoffUntilR3610));
                                                        } catch (Throwable ignored) {}
                                                    }
                                                }
                                            });
                    if (r.settlementTerminalizedCount > 0) {
                        broadcastSettlementUpdated();
                    }
                    try {
                        heartbeatStateV8()
                                .markBusinessStage(
                                        r.businessClosed
                                                ? "R36_BUSINESS_CLOSED"
                                                : (r.canonicalReady
                                                        ? "R36_CANONICAL_READY_WAITING_SETTLEMENT"
                                                        : "R36_ANALYSIS_REQUESTED"),
                                        "base="
                                                + r.baseIssue
                                                + ",target="
                                                + r.targetIssue
                                                + ",closed="
                                                + r.businessClosed);
                    } catch (Throwable ignore) {
                        recordCriticalCatchR926("R36_RECOVERY_STATE", r.targetIssue, ignore);
                    }
                    if (r.canonicalReady) postAnalysisForIssueR36(latest.issue, reason, force);
                }
            } catch (Throwable e) {
                lastError = safeError(e);
                try {
                    Models.Draw latestFailure = store.getLatestDraw();
                    String failedTarget = latestFailure == null ? ""
                            : TargetIssueResolver.next(latestFailure.issue).targetIssue;
                    java.util.LinkedHashMap<String,String> patch = new java.util.LinkedHashMap<String,String>();
                    patch.put("r365_last_analysis_failure_target", failedTarget);
                    patch.put("r365_last_analysis_failure", "RECONCILE_FAILED:" + lastError);
                    patch.put("r365_last_analysis_transition", "RECONCILE_FAILED_RETRY_REQUIRED");
                    store.setStates(patch);
                } catch (Throwable ignoredDiagnostic) {}
                setAnalysisState("ERROR", 0, "本期分析协调失败，等待安全重试：" + lastError);
                CrashRecorder.recordCaught(getApplicationContext(), e, "R45_ANALYSIS_RECONCILE");
                ensureAnalysisWatchdog();
            }
            return;
        }

        if (oomBackoffR947) {
            try {
                setAnalysisState(
                        "WAITING", 0,
                        "R3.6.10内存熔断保护：保留抓取/结算，分析将在内存恢复后重试");
            } catch (Throwable ignore) {}
            return;
        }

        try {
            Models.Draw latest = store.getLatestDraw();
            if (latest == null || !latest.isValid()) {
                return;
            }

            postAnalysisForIssue(latest.issue, reason, force);
        } catch (Throwable e) {
            lastError = safeError(e);
        }
    }

    private synchronized long prepareAnalysisGenerationForPost(String baseIssue, boolean force) {
        analysisGeneration++;
        if (analysisGeneration <= 0L) {
            analysisGeneration = 1L;
        }

        return analysisGeneration;
    }

    private boolean canDirectTakeoverAnalysisNow() {
        long now = System.currentTimeMillis();

        if (lastAnalysisDirectTakeoverAt > 0L
                && now - lastAnalysisDirectTakeoverAt < ANALYSIS_DIRECT_TAKEOVER_MIN_INTERVAL_MS) {
            return false;
        }

        lastAnalysisDirectTakeoverAt = now;
        return true;
    }

    private void postAnalysisForIssue(String baseIssue, String reason, boolean force) {
        if (useR36Runtime()) {
            postAnalysisForIssueR36(baseIssue, reason, force);
            return;
        }
        if (!running || analysisHandler == null) {
            return;
        }

        if (baseIssue == null || baseIssue.length() == 0) {
            return;
        }

        String targetIssue = "";

        try {
            targetIssue = dataGuard == null ? "" : dataGuard.buildNextIssue(baseIssue);
        } catch (Throwable e) {
            targetIssue = "";
        }

        if (analysisBusy && sameText(analysisCurrentBaseIssue, baseIssue)) {
            long busyCostNow =
                    analysisStartedAt <= 0L ? 0L : System.currentTimeMillis() - analysisStartedAt;
            recordObserveFullRunState(
                    baseIssue,
                    targetIssue,
                    busyCostNow,
                    "SAME_ISSUE_BUSY_NO_RESTART",
                    "同目标期正在轻主链运行中：不重复投递、不快裁BET、不救援BET，等待DecisionGate返回");
            if (store != null) {
                try {
                    store.setState("analysis_pending_skip_same_issue", baseIssue);
                    store.setState("analysis_pending_skip_reason", reason == null ? "" : reason);
                    store.setState(
                            "analysis_pending_skip_at", String.valueOf(System.currentTimeMillis()));
                    store.setState("analysis_same_issue_busy_cost_ms", String.valueOf(busyCostNow));
                    store.setState(
                            "analysis_same_issue_policy",
                            "实时热修复：同目标期不重复投递；10秒无结果由业务看门狗换代重跑，不并发BET救援");
                    if (targetIssue.length() > 0) {
                        store.setState("analysis_target_issue", targetIssue);
                    }
                } catch (Throwable e) {
                    recordCriticalCatchR926("ANALYSIS_BUSY_STATE", targetIssue, e);
                }
            }
            return;
        }

        if (targetIssue.length() > 0 && store != null) {
            try {
                Models.Decision old = store.getDecisionByTargetIssue(targetIssue);

                if (old != null
                        && !isWeakPlaceholderDecision(old)
                        && sameText(old.baseIssue, baseIssue)
                        && AppConfig.DECISION_BET.equals(old.decision)) {
                    store.setState("analysis_done_target_issue", targetIssue);
                    store.setState("analysis_skip_reason", "目标期已有正式BET，不重复分析");
                    return;
                }

                if (!force
                        && old != null
                        && !isWeakPlaceholderDecision(old)
                        && sameText(old.baseIssue, baseIssue)) {
                    store.setState("analysis_done_target_issue", targetIssue);
                    return;
                }
            } catch (Throwable e) {
                recordCriticalCatchR926("EXISTING_DECISION_CHECK", targetIssue, e);
            }
        }

        if (analysisBusy && !sameText(analysisCurrentBaseIssue, baseIssue) && force) {
            if (!canDirectTakeoverAnalysisNow()) {
                return;
            }
        }

        requestRealtimePriority("实时正式分析任务已投递，后台慢任务让路");

        long now = System.currentTimeMillis();
        long gen = prepareAnalysisGenerationForPost(baseIssue, force);

        pendingAnalysisBaseIssue = baseIssue;
        pendingAnalysisReason = reason == null ? "" : reason;
        pendingAnalysisForce = force;
        pendingAnalysisGeneration = gen;
        analysisPostedAt = now;

        if (store != null) {
            store.setState("analysis_target_issue", targetIssue);
            store.setState("analysis_posted_at", String.valueOf(now));
            store.setState("analysis_generation", String.valueOf(gen));
            store.setState("target_issue", targetIssue);
            store.setState("current_issue", baseIssue);

            if (!analysisBusy) {
                setAnalysisState(
                        "WAITING",
                        0,
                        "正式分析任务已投递，等待分析线程接管目标期 " + targetIssue + "，原因：" + pendingAnalysisReason);
            } else {
                store.setState("analysis_pending_takeover_issue", baseIssue);
                store.setState("analysis_pending_takeover_target", targetIssue);
                store.setState("analysis_pending_takeover_reason", reason == null ? "" : reason);
            }
        }

        requestBacktestYield("实时正式分析任务已投递，目标期 " + targetIssue);

        analysisHandler.removeCallbacks(analysisLoop);
        analysisHandler.post(analysisLoop);
        ensureAnalysisWatchdog();
    }

    private boolean useR36Runtime() {
        return AppConfig.ENABLE_V51_TIANSHU && AppConfig.DB_VERSION >= 36;
    }

    private void postAnalysisForIssueR36(String baseIssue, String reason, boolean force) {
        if (!running
                || analysisHandler == null
                || store == null
                || !TargetIssueResolver.isValid(baseIssue)) return;
        final boolean oomBackoffR947 = analysisOomBackoffUntilR3610 > System.currentTimeMillis();
        TargetIssueResolver.Resolution resolution = TargetIssueResolver.next(baseIssue);
        if (!resolution.valid) return;
        String target = resolution.targetIssue;
        if (CanonicalObservationWriterR36.hasCanonical(store, target)) {
            Models.Decision view = CanonicalDecisionViewAdapterR36.read(store, target);
            setAnalysisState("OK", 100, "R3.6 canonical已存在：" + target);
            try {
                heartbeatStateV8()
                        .markAnalysisCanonicalCommitted(target, System.currentTimeMillis());
            } catch (Throwable ignore) {
                recordCriticalCatchR926("HEARTBEAT_CANONICAL_EXISTING", target, ignore);
            }
            if (view != null) broadcastDecision(view);
            scheduleDeferredDrainR36();
            return;
        }
        // Reading an already committed prediction is not a new critical analysis.
        // Frequent polling used to preempt the research worker even on this no-work path.
        // Only new request preparation and the admitted single-flight own pressure.
        RealtimePressureGateR811.beginAnalysis();
        try {
        long now = System.currentTimeMillis();
        Models.Draw durableBase=store.getDrawByIssue(baseIssue);
        long deadline=TargetIssueResolver.predrawDeadlineWallR20261008(durableBase);
        if(deadline<=now) {
            setAnalysisState("WAITING",0,"TARGET_EXPIRED_WAIT_LATEST:"+target);
            return;
        }
        AnalysisRequestStoreR36 requests = new AnalysisRequestStoreR36(store);
        AnalysisSingleFlightR36.Request request;
        try {
            request = requests.ensurePending(baseIssue, target, deadline, safe(reason));
        } catch (Throwable e) {
            lastError = safeError(e);
            setAnalysisState("ERROR", 0, "R3.6请求落库失败：" + lastError);
            return;
        }
        setAnalysisState(
                "WAITING", 0,
                oomBackoffR947
                        ? "R947分析请求已落库；OOM熔断仅暂停执行：" + target
                        : "R3.6分析已请求，等待唯一执行权：" + target);
        if (!oomBackoffR947) submitAnalysisRequestR36(request);
        else {
            try {
                store.setState("r947_oom_request_preserved_target", target);
                store.setState("r947_oom_request_preserved_until", String.valueOf(analysisOomBackoffUntilR3610));
            } catch (Throwable ignored) {}
        }
        } finally {
            RealtimePressureGateR811.endAnalysis();
        }
    }

    private void submitAnalysisRequestR36(final AnalysisSingleFlightR36.Request request) {
        if (request == null || !request.valid() || analysisHandler == null || store == null) return;
        AnalysisSingleFlightR36.Request prepared = request;
        try {
            prepared = new AnalysisRequestStoreR36(store).refreshExecutionWindow(
                    request, 12000L, System.currentTimeMillis(), "SINGLE_FLIGHT_SUBMIT");
        } catch (Throwable refreshFailure) {
            try { store.setState("r947_analysis_window_refresh_error", safeError(refreshFailure)); }
            catch (Throwable ignored) {}
        }
        final AnalysisSingleFlightR36.Request effectiveRequest = prepared == null ? request : prepared;
        final AnalysisSingleFlightR36.SubmitResult submit;
        try {
            submit = AnalysisSingleFlightR36.shared().submit(effectiveRequest);
        } catch (Throwable e) {
            lastError = safeError(e);
            setAnalysisState("ERROR", 0, "R3.6 single-flight提交失败：" + lastError);
            return;
        }
        if (!submit.startNow || submit.lease == null) {
            RuntimeEventHooksR926.queued(store, effectiveRequest, submit.ownerTarget);
            setAnalysisState(
                    "WAITING",
                    0,
                    "R3.6已排队：owner=" + submit.ownerTarget + "，pending=" + submit.pendingTarget);
            return;
        }
        // Serialize admission with the callback: a throwing/rejecting Handler must never leave
        // an unbound single-flight owner, and an accidentally retained callback must not execute.
        final boolean[] admittedR45 = new boolean[] {false};
        boolean posted = false;
        RuntimeException dispatchFailureR45 = null;
        AnalysisSingleFlightR36.Request queuedR45 = null;
        synchronized (submit.lease) {
            try {
                RuntimeEventHooksR926.accepted(store, effectiveRequest, submit.lease.ownerId);
                posted = analysisHandler.post(new Runnable() {
                    @Override public void run() {
                        synchronized (submit.lease) {
                            if (!admittedR45[0]) return;
                        }
                        runAnalysisRequestR36(submit.lease);
                    }
                });
                admittedR45[0] = posted;
            } catch (RuntimeException failure) {
                dispatchFailureR45 = failure;
            } finally {
                if (!posted) {
                    submit.lease.token.requestCancelOnce("R45_DISPATCH_NOT_ADMITTED");
                    queuedR45 = AnalysisSingleFlightR36.shared().releaseAndTakeNext(submit.lease);
                }
            }
        }
        if (!posted) {
            String code = dispatchFailureR45 == null ? "HANDLER_POST_REJECTED"
                    : "HANDLER_POST_FAILED:" + safeError(dispatchFailureR45);
            try { RuntimeEventHooksR926.rejected(store, effectiveRequest, code); }
            catch (Throwable ignoredEvent) {
                recordCriticalCatchR926("DISPATCH_REJECTED_EVENT", effectiveRequest.targetIssue, ignoredEvent);
            }
            try {
                java.util.LinkedHashMap<String,String> patch = new java.util.LinkedHashMap<String,String>();
                patch.put("r914_analysis_handler_post", "REJECTED_RETRY_DURABLE");
                patch.put("r914_analysis_handler_post_target", effectiveRequest.targetIssue);
                patch.put("r365_last_analysis_failure_target", effectiveRequest.targetIssue);
                patch.put("r365_last_analysis_failure", code);
                patch.put("r365_last_analysis_transition", "REQUEST_PRESERVED_NO_EXECUTION_OWNER");
                patch.put("r45_dispatch_pending_target", queuedR45 == null ? "" : queuedR45.targetIssue);
                store.setStates(patch);
            } catch (Throwable ignoredState) {
                recordCriticalCatchR926("HANDLER_REJECTED_STATE", effectiveRequest.targetIssue, ignoredState);
            }
            setAnalysisState("ERROR", 0, "本期分析入队失败，已释放执行权，持久化请求等待重试：" + code);
            repairDeadAnalysisWorkerR811("R45_HANDLER_POST_FAILED");
            // The latest target is still durable; the watchdog retries it without recursion.
            ensureAnalysisWatchdog();
            return;
        }
        ensureAnalysisWatchdog();
    }

    private void runAnalysisRequestR36(AnalysisSingleFlightR36.Lease lease) {
        if (lease == null || store == null) return;
        AnalysisSingleFlightR36.Request request = lease.request;
        AnalysisRequestStoreR36 requests = new AnalysisRequestStoreR36(store);
        final long workerEnteredR959=TimeSourceR811.monoNowMs();
        analysisBusy = true;
        analysisStartedAt = System.currentTimeMillis();
        analysisCurrentBaseIssue = request.baseIssue;
        analysisCurrentTargetIssue = request.targetIssue;
        requestRealtimePriority("R3.6唯一分析运行中");
        boolean durableOwned = false;
        boolean durableClaimedR77 = false;
        boolean durableExitFinishedR77 = false;
        String durableExitReasonR77 = "R77_OWNER_EXITED_BEFORE_COMMIT";
        boolean canonicalCommitted = false;
        RuntimeResourceAuditR927.Scope resourceScopeR927 = null;
        try {
            lease.bindCurrentThread();
            final long claimStartedR959=TimeSourceR811.monoNowMs();
            AnalysisRequestStoreR36.ClaimResult claim =
                    requests.claimDetailed(request, lease.ownerId, System.currentTimeMillis());
            if (!claim.claimed) {
                try {
                    store.setState("r365_last_claim_decision", claim.decision.name());
                    store.setState("r365_last_claim_code", claim.code);
                    store.setState("r365_last_claim_target", request.targetIssue);
                    store.setState("r365_last_claim_retry_at", String.valueOf(claim.retryAt));
                    store.setState(
                            "r365_last_claim_durable", requests.diagnostic(request.targetIssue));
                } catch (Throwable ignore) {
                    recordCriticalCatchR926("LEASE_CLAIM_STATE", request.targetIssue, ignore);
                }
                if (claim.deferred()) {
                    setAnalysisState(
                            "WAITING",
                            0,
                            "R3.6.6持久化Owner仍有效，安全等待："
                                    + request.targetIssue
                                    + "，"
                                    + claim.code);
                    return;
                }
                if (claim.decision
                        == R36AnalysisLeasePolicyR365.Decision.CANONICAL_ALREADY) {
                    Models.Decision view =
                            CanonicalDecisionViewAdapterR36.read(store, request.targetIssue);
                    setAnalysisState("OK", 100, "R3.6.6 canonical已存在：" + request.targetIssue);
                    if (view != null) broadcastDecision(view);
                    scheduleDeferredDrainR36();
                    return;
                }
                if (claim.decision
                        == R36AnalysisLeasePolicyR365.Decision.TARGET_ALREADY_DRAWN) {
                    setAnalysisState(
                            "WAITING", 0, "目标期已开奖，禁止事后重算：" + request.targetIssue);
                    return;
                }
                if (!claim.databaseError
                        && (claim.decision
                                        == R36AnalysisLeasePolicyR365.Decision.STALE_REQUEST
                                || claim.decision
                                        == R36AnalysisLeasePolicyR365.Decision.TERMINAL)) {
                    setAnalysisState(
                            "WAITING",
                            0,
                            "R3.6.6旧请求已失效，等待精确下一期："
                                    + request.targetIssue
                                    + "，"
                                    + claim.code);
                    return;
                }
                throw new IllegalStateException("R366_DURABLE_CLAIM_ERROR:" + claim.code);
            }
            final long claimCostR959=TimeSourceR811.monoNowMs()-claimStartedR959;
            durableOwned = true;
            durableClaimedR77 = true;
            // R77: the successful claim already renewed the durable lease. History checkpoints
            // retain cancellation/deadline checks without immediately writing the same lease twice.
            lease.token.claimDurablePulse(System.currentTimeMillis(), true);
            // Measure only an accepted durable execution. Auditing before claim used to
            // exhaust an old request's SLO before its retry could acquire ownership.
            resourceScopeR927 = RuntimeResourceAuditR927.begin(store, lease);
            RuntimeResourceAuditR927.completedBeforeScopeR79("OWNER_DISPATCH",Math.max(0L,workerEnteredR959-lease.acquiredMonoAt));
            RuntimeResourceAuditR927.measuredQueueSpanCP12("OWNER_DISPATCH",lease.acquiredMonoAt,workerEnteredR959);
            RuntimeResourceAuditR927.completedBeforeScopeR79("DURABLE_CLAIM",claimCostR959);
            long requestOriginR959=RuntimeObservabilityR925.requestOrigin(request);
            if(requestOriginR959>=0)RuntimeResourceAuditR927.completedBeforeScopeR79("REQUEST_TO_WORKER",Math.max(0L,workerEnteredR959-requestOriginR959));
            if(requestOriginR959>=0&&requestOriginR959<=lease.acquiredMonoAt)RuntimeResourceAuditR927.measuredQueueSpanCP12("REQUEST_BEFORE_OWNER",requestOriginR959,lease.acquiredMonoAt);
            final long preparationStartedR959=TimeSourceR811.monoNowMs();
            RuntimeResourceAuditR927.stageBegin("PRE_HISTORY_METADATA");
            AnalysisSingleFlightR36.Request claimed = claim.request;
            RuntimeEventHooksR926.started(store, claimed, lease.ownerId);
            if (store.getDrawByIssue(request.targetIssue) != null) {
                requests.markExpired(
                        request,
                        lease.ownerId,
                        "TARGET_ALREADY_DRAWN",
                        System.currentTimeMillis());
                throw new IllegalStateException("R36_TARGET_ALREADY_DRAWN");
            }
            try {
                heartbeatStateV8().markAnalysisRunning(request.targetIssue);
            } catch (Throwable ignore) {
                recordCriticalCatchR926("HEARTBEAT_RUNNING", request.targetIssue, ignore);
            }
            b13RecordBusinessBeat(store, "ANALYSIS_RUNNING", request.targetIssue);
            setAnalysisState("RUNNING", 10, "R3.6冻结PIT历史：" + request.targetIssue);
            RuntimeMemoryGuardR811.Snapshot memoryPreflight =
                    RuntimeMemoryGuardR811.prepareForAnalysis();
            java.util.LinkedHashMap<String,String> memoryProjectionR81 = new java.util.LinkedHashMap<String,String>();
            memoryProjectionR81.put("r811_runtime_memory_preflight", memoryPreflight.canonical());
            memoryProjectionR81.put("r811_runtime_memory_preflight_at", String.valueOf(System.currentTimeMillis()));
            LiveTelemetryProjectionR81.tryWrite(store, memoryProjectionR81);
            renewAnalysisLeaseOrThrowR365(requests, request, lease, "HISTORY_PREPARE_ENTER");
            RuntimeResourceAuditR927.stageEnd("PRE_HISTORY_METADATA",TimeSourceR811.monoNowMs()-preparationStartedR959,true);
            RuntimeResourceAuditR927.stageBegin("HISTORY");
            long historyStartedR927 = TimeSourceR811.monoNowMs();
            List<Models.Draw> analysisHistory =
                    prepareAnalysisHistoryR364(request, requests, lease);
            RuntimeResourceAuditR927.stageEnd("HISTORY", TimeSourceR811.monoNowMs()-historyStartedR927, true);
            renewAnalysisLeaseOrThrowR365(requests, request, lease, "HISTORY_PREPARE_DONE");
            final long inputStartedR959=TimeSourceR811.monoNowMs();
            V51RuntimeOrchestrator.OutcomeR36 outcome;
            try (PredrawWakeLeaseR81 predrawWakeR81 =
                    PredrawWakeLeaseR81.acquire(this, claimed.deadlineMonoAt)) {
            RuntimeResourceAuditR927.stageBegin("PIT_FREEZE_AND_BIND");
            V51RuntimeInputR36 input =
                    V51RuntimeInputR36.freeze(analysisHistory, claimed);
            RuntimeEventHooksR926.frozen(store, claimed, input);
            claimed = requests.bindHistoryHash(claimed, input.historyHash, lease.ownerId);
            setAnalysisState("RUNNING", 35, "R3.6计算号码100与正式动作1200：" + request.targetIssue);
            RuntimeResourceAuditR927.stageEnd("PIT_FREEZE_AND_BIND",TimeSourceR811.monoNowMs()-inputStartedR959,true);
            outcome = V51AutoFetchBridge.runR36(store, input, claimed, lease, predrawWakeR81);
            }
            if (outcome == null
                    || outcome.runtime == null
                    || outcome.commit == null
                    || !outcome.commit.success)
                throw new IllegalStateException("R36_RUNTIME_OUTCOME_INVALID");
            try {
                pendingExpertSnapshotR811 =
                        ResearchExpertPredrawSnapshotR811.freezeFromRuntime(outcome.runtime);
            } catch (Throwable researchSnapshotError) {
                store.setState(
                        "r811_expert_predraw_freeze_error",
                        safeError(researchSnapshotError));
            }
            canonicalCommitted = lease.token.isCanonicalCommitted();
            durableOwned = !canonicalCommitted;
            store.setState(
                    "r811_runtime_memory_after_commit",
                    RuntimeMemoryGuardR811.snapshot("AFTER_COMMIT").canonical());
            setAnalysisState(
                    "OK",
                    100,
                    "R3.6 canonical原子提交完成："
                            + request.targetIssue
                            + "，request→commit="
                            + outcome.commit.requestToCommitMs
                            + "ms；1200证据可恢复补写中");
            try {
                heartbeatStateV8()
                        .markAnalysisCanonicalCommitted(
                                request.targetIssue, outcome.commit.commitAt);
            } catch (Throwable ignore) {
                recordCriticalCatchR926("HEARTBEAT_CANONICAL_COMMITTED", request.targetIssue, ignore);
            }
            b13RecordBusinessBeat(store, "ANALYSIS_CANONICAL_COMMITTED", request.targetIssue);
            writeBusinessHeartbeatState(
                    "last_analysis_success_at",
                    request.targetIssue,
                    "ANALYSIS_CANONICAL_COMMITTED");
            Models.Decision view = CanonicalDecisionViewAdapterR36.read(store, request.targetIssue);
            if (view != null) broadcastDecision(view);
            // R907: only after Decision is visible do we enqueue Research. It runs on the low
            // priority serial background owner and can never delay or invalidate the prediction.
            postResearchCaptureR907(request.targetIssue, 0);
            scheduleDeferredDrainR36();
            RuntimeMemoryGuardR811.releaseAfterAnalysis();
            updateRuntimeState(AppConfig.SERVICE_STATUS_RUNNING, "");
        } catch (OutOfMemoryError oom) {
            RuntimeResourceAuditR927.beginRecovery();
            // Do not enter the normal diagnostic formatter while the heap is exhausted.  The
            // durable request remains fail-closed and will be retried after its lease/backoff;
            // fetch + exact settlement keep running so the whole process does not die with it.
            analysisOomBackoffUntilR3610 = System.currentTimeMillis() + 60000L;
            lastError = "OutOfMemoryError";
            RuntimeMemoryGuardR811.recoverAfterFailure();
            CrashRecorder.recordCaught(
                    getApplicationContext(), oom, "R3610_ANALYSIS_REQUEST_OOM");
            durableExitReasonR77 = "R817_OOM_RETRY_RELEASED|stage=" + lease.token.stage();
            AnalysisRequestStoreR36.TransitionResult oomTransition = null;
            try {
                if (durableOwned)
                    oomTransition =
                            requests.markRetry(
                                    request,
                                    lease.ownerId,
                                    "R817_OOM_RETRY_RELEASED|stage="
                                            + lease.token.stage(),
                                    System.currentTimeMillis());
                durableOwned = false;
                durableExitFinishedR77 = !retryableExitTransitionR77(oomTransition);
                store.setState(
                        "r817_oom_durable_transition",
                        oomTransition == null
                                ? "NO_DURABLE_OWNERSHIP"
                                : oomTransition.status + ':' + oomTransition.code);
                store.setState("r817_oom_target", request.targetIssue);
                store.setState("r817_oom_at", String.valueOf(System.currentTimeMillis()));
            } catch (Throwable ignoredTransition) {
                recordCriticalCatchR926("OOM_DURABLE_TRANSITION", request.targetIssue, ignoredTransition);
            }
            try {
                heartbeatStateV8().markAnalysisFailed(request.targetIssue, "OutOfMemoryError");
            } catch (Throwable ignore) {
                recordCriticalCatchR926("HEARTBEAT_OOM_FAILED", request.targetIssue, ignore);
            }
            try {
                RuntimeHealthLedgerR36.record(
                        store,
                        "ANALYSIS_ATTEMPT_FAILED",
                        request.attemptId(),
                        request.targetIssue,
                        "OOM",
                        request.requestAt,
                        0L,
                        Math.max(0L, System.currentTimeMillis() - request.requestAt),
                        AnalysisSingleFlightR36.shared().activeCount(),
                        AnalysisSingleFlightR36.shared().maximumObserved(),
                        false,
                        "OOM_LOW_ALLOCATION_R3610");
            } catch (Throwable ignore) {
                recordCriticalCatchR926("OOM_HEALTH_LEDGER", request.targetIssue, ignore);
            }
            try {
                setAnalysisState(
                        "WAITING",
                        0,
                        "R3.6.10内存保护已接管：抓取/结算继续，分析延迟重试，不退出进程");
            } catch (Throwable ignore) {
                recordCriticalCatchR926("OOM_WAITING_STATE", request.targetIssue, ignore);
            }
        } catch (Throwable e) {
            RuntimeResourceAuditR927.beginRecovery();
            // Wrapped OutOfMemoryError must remain an explicit OOM event. R815 initialized this
            // flag to false and therefore mislabeled a causal-chain OOM as an ordinary retry.
            boolean actualOom = isOomR3610(e);
            boolean memoryPressure = e instanceof RuntimeMemoryGuardR811.MemoryPressureException;
            if (memoryPressure || actualOom) RuntimeMemoryGuardR811.recoverAfterFailure();
            lastError = safeError(e);
            durableExitReasonR77 = lastError;
            long now = System.currentTimeMillis();
            if (canonicalCommitted || lease.token.isCanonicalCommitted()) {
                try {
                    store.setState("r365_post_commit_warning", lastError);
                    store.setState("r365_post_commit_warning_target", request.targetIssue);
                    store.setState("r365_post_commit_warning_at", String.valueOf(now));
                } catch (Throwable ignore) {
                    recordCriticalCatchR926("POST_COMMIT_WARNING_STATE", request.targetIssue, ignore);
                }
                setAnalysisState(
                        "OK",
                        100,
                        "R3.6.6 canonical已提交；仅后置通知失败："
                                + request.targetIssue
                                + "，"
                                + lastError);
                scheduleDeferredDrainR36();
                return;
            }
            AnalysisRequestStoreR36.TransitionResult transition = null;
            if (durableOwned) {
                if (store.getDrawByIssue(request.targetIssue) != null
                        || (request.deadlineAt>0L && now>=request.deadlineAt))
                    transition =
                            requests.markExpired(
                                    request,
                                    lease.ownerId,
                                    "DRAWN_OR_FIXED_DEADLINE_EXPIRED:" + lastError,
                                    now);
                else if (e instanceof AnalysisSingleFlightR36.CancelledBeforeCommitException
                        && lease.token.supersededByNewerTarget())
                    transition =
                            requests.markExpired(
                                    request,
                                    lease.ownerId,
                                    "SUPERSEDED_BEFORE_COMMIT:" + lease.token.cancelReason(),
                                    now);
                else
                    transition =
                            requests.markRetry(
                                    request,
                                    lease.ownerId,
                                    (e instanceof AnalysisSingleFlightR36.CancelledBeforeCommitException
                                                    ? "COOPERATIVE_CANCEL:"
                                                    : "")
                                            + lastError,
                                    now);
            }
            durableExitFinishedR77 = !retryableExitTransitionR77(transition);
            String transitionStatus =
                    transition == null
                            ? "NO_DURABLE_OWNERSHIP"
                            : transition.status + ':' + transition.code;
            String healthStatus =
                    actualOom
                            ? "OOM"
                            : (memoryPressure ? "MEMORY_PRESSURE_RETRY" : transitionStatus);
            RuntimeHealthLedgerR36.record(
                    store,
                    "ANALYSIS_ATTEMPT_FAILED",
                    request.attemptId(),
                    request.targetIssue,
                    healthStatus,
                    request.requestAt,
                    0L,
                    Math.max(0L, now - request.requestAt),
                    AnalysisSingleFlightR36.shared().activeCount(),
                    AnalysisSingleFlightR36.shared().maximumObserved(),
                    transition != null
                            && AnalysisRequestStoreR36.FAILED_TERMINAL.equals(
                                    transition.status),
                    lastError
                            + "|memory="
                            + RuntimeMemoryGuardR811.snapshot("FAILURE_RECOVERED").canonical()
                            + "|durable="
                            + requests.diagnostic(request.targetIssue));
            try {
                heartbeatStateV8().markAnalysisFailed(request.targetIssue, lastError);
            } catch (Throwable ignore) {
                recordCriticalCatchR926("HEARTBEAT_ANALYSIS_FAILED", request.targetIssue, ignore);
            }
            try {
                store.setState("b13_analysis_error", lastError);
            } catch (Throwable ignore) {
                recordCriticalCatchR926("ANALYSIS_ERROR_STATE", request.targetIssue, ignore);
            }
            b13RecordBusinessBeat(store, "ANALYSIS_FAILED", request.targetIssue);
            try {
                store.setState("r365_last_analysis_failure", lastError);
                store.setState("r365_last_analysis_failure_target", request.targetIssue);
                store.setState("r365_last_analysis_failure_at", String.valueOf(now));
                store.setState("r365_last_analysis_transition", transitionStatus);
                store.setState(
                        "r365_last_analysis_durable", requests.diagnostic(request.targetIssue));
            } catch (Throwable ignore) {
                recordCriticalCatchR926("ANALYSIS_FAILURE_DURABLE_STATE", request.targetIssue, ignore);
            }
            setAnalysisState("ERROR", 0, "R3.6分析未提交 canonical：" + lastError);
        } finally {
            try { RuntimeResourceAuditR927.finish(store, resourceScopeR927, lease.token.isCanonicalCommitted(), lease.token.isCanonicalCommitted() ? "DURABLE" : lastError); }
            catch (Throwable auditErrorR927) { recordCriticalCatchR926("RESOURCE_AUDIT_FINISH", request.targetIssue, auditErrorR927); }
            analysisBusy = false;
            analysisStartedAt = 0L;
            analysisCurrentBaseIssue = "";
            analysisCurrentTargetIssue = "";
            AnalysisSingleFlightR36.Request next =
                    AnalysisSingleFlightR36.shared().releaseAndTakeNext(lease);
            if (durableClaimedR77 && !lease.token.isCanonicalCommitted() && !durableExitFinishedR77)
                scheduleExitedAnalysisReleaseR77(lease, durableExitReasonR77);
            if (next == null) {
                try {
                    next = requests.latestRunnable(System.currentTimeMillis());
                } catch (Throwable ignore) {
                    recordCriticalCatchR926("ANALYSIS_NEXT_RUNNABLE", request.targetIssue, ignore);
                    next = null;
                }
            }
            if (next != null && running) submitAnalysisRequestR36(next);
        }
    }

    /**
     * A fresh installation receives only five rows from fetchLatest(). Bootstrap the history
     * endpoint on the sole analysis HandlerThread before freezing B04 input. The 240-row value is
     * only an upper bound; it is never a prediction gate.
     */
    private List<Models.Draw> prepareAnalysisHistoryR364(
            AnalysisSingleFlightR36.Request request,
            AnalysisRequestStoreR36 requests,
            AnalysisSingleFlightR36.Lease lease) {
        List<Models.Draw> history =
                store.getRecentDraws(R36AnalysisHistoryPolicyR364.MAX_FROZEN_HISTORY);
        int before = R36AnalysisHistoryPolicyR364.validCount(history);
        int suffixBefore =
                R36AnalysisHistoryPolicyR364.contiguousSuffixCount(
                        history, request.baseIssue);
        try {
            java.util.LinkedHashMap<String, String> state =
                    new java.util.LinkedHashMap<String, String>();
            state.put("r364_history_before", String.valueOf(before));
            state.put("r816_history_suffix_before", String.valueOf(suffixBefore));
            state.put("r364_history_bootstrap_target", safe(request.targetIssue));
            LiveTelemetryProjectionR81.tryWrite(store, state);
        } catch (Throwable ignore) {
            recordCriticalCatchR926("HISTORY_BOOTSTRAP_STATE_BEFORE", request.targetIssue, ignore);
        }
        if (R36AnalysisHistoryPolicyR364.needsBootstrap(suffixBefore)) {
            renewAnalysisLeaseOrThrowR365(requests, request, lease, "HISTORY_BOOTSTRAP_BEGIN");
            setAnalysisState(
                    "RUNNING",
                    12,
                    "R3.6.6新安装历史补齐："
                            + before
                            + " total/"
                            + suffixBefore
                            + " contiguous"
                            + "/"
                            + R36AnalysisHistoryPolicyR364.MIN_LIVE_HISTORY);
            Models.ApiResult bootstrap =
                    apiClient == null
                            ? Models.ApiResult.fail("API_CLIENT_NULL", "history bootstrap unavailable")
                            : apiClient.fetchHistory(
                                    R36AnalysisHistoryPolicyR364.PREFERRED_BOOTSTRAP_HISTORY);
            boolean apiOk = bootstrap != null && (bootstrap.success || bootstrap.ok);
            // Keep an authenticated response durable even if this prediction expires.
            // Saving data cannot create a canonical prediction or REAL authorization.
            if (apiOk) {
                final Models.Draw beforeIngress=store.getLatestDraw();
                try(RuntimeResourceAuditR927.DataIngressScopeCP12 dataLane=RuntimeResourceAuditR927.dataIngressCP12()) {
                    // Bootstrap can span an opening. Hand the newest durable draw to
                    // clock/request/settlement consumers before writing its history page.
                    saveApiResultDraws(bootstrap,new Runnable(){
                        @Override public void run(){
                            handleLatestDrawAfterSave(store.getLatestDraw(),beforeIngress,false,
                                    "R60_BOOTSTRAP_LATEST_BEFORE_HISTORY");
                        }
                    });
                }
            }
            renewAnalysisLeaseOrThrowR365(requests, request, lease, "HISTORY_BOOTSTRAP_SAVED");
            try {
                java.util.LinkedHashMap<String, String> state =
                        new java.util.LinkedHashMap<String, String>();
                state.put("r364_history_bootstrap_status", apiOk ? "OK" : "FAILED");
                state.put(
                        "r364_history_bootstrap_error",
                        bootstrap == null
                                ? "RESULT_NULL"
                                : safe(bootstrap.errorCode) + ":" + safe(bootstrap.errorMessage));
                state.put(
                        "r364_history_bootstrap_at", String.valueOf(System.currentTimeMillis()));
                LiveTelemetryProjectionR81.tryWrite(store, state);
            } catch (Throwable ignore) {
                recordCriticalCatchR926("HISTORY_BOOTSTRAP_STATE_RESULT", request.targetIssue, ignore);
            }
            if (store.getDrawByIssue(request.targetIssue) != null)
                throw new IllegalStateException("R364_TARGET_DRAWN_DURING_HISTORY_BOOTSTRAP");
            history = store.getRecentDraws(R36AnalysisHistoryPolicyR364.MAX_FROZEN_HISTORY);
        }
        int after = R36AnalysisHistoryPolicyR364.validCount(history);
        int suffixAfter =
                R36AnalysisHistoryPolicyR364.contiguousSuffixCount(
                        history, request.baseIssue);
        try {
            java.util.LinkedHashMap<String, String> state =
                    new java.util.LinkedHashMap<String, String>();
            state.put("r364_history_after", String.valueOf(after));
            state.put("r816_history_suffix_after", String.valueOf(suffixAfter));
            state.put(
                    "r364_history_policy",
                    "MIN="
                            + R36AnalysisHistoryPolicyR364.MIN_LIVE_HISTORY
                            + "|BOOTSTRAP="
                            + R36AnalysisHistoryPolicyR364.PREFERRED_BOOTSTRAP_HISTORY
                            + "|MAX="
                            + R36AnalysisHistoryPolicyR364.MAX_FROZEN_HISTORY
                            + "|240_IS_NOT_MINIMUM");
            LiveTelemetryProjectionR81.tryWrite(store, state);
        } catch (Throwable ignore) {
            recordCriticalCatchR926("HISTORY_BOOTSTRAP_STATE_AFTER", request.targetIssue, ignore);
        }
        if (!R36AnalysisHistoryPolicyR364.usable(suffixAfter))
            throw new IllegalStateException(
                    "R817_HISTORY_CONTIGUOUS_SUFFIX_LT_12|total="
                            + after
                            + "|suffix="
                            + suffixAfter
                            + "|required=12");
        return history;
    }

    static boolean retryableExitTransitionR77(AnalysisRequestStoreR36.TransitionResult result) {
        return result == null || (!result.updated &&
                ("WRITE_COORDINATION_FAILED".equals(result.code) || "TRANSITION_NULL".equals(result.code)));
    }

    private void scheduleExitedAnalysisReleaseR77(AnalysisSingleFlightR36.Lease exited, String error) {
        final Handler handler = analysisHandler;
        if (!running || userRequestedStopV5 || store == null || handler == null || exited == null
                || exited.token.isCanonicalCommitted()
                || AnalysisSingleFlightR36.shared().currentLease() == exited) return;
        ExitedAnalysisReleaseR77 ticket = new ExitedAnalysisReleaseR77(exited, error, handler);
        if (exitedAnalysisReleasesR77.putIfAbsent(ticket.key, ticket) != null) return;
        if (!handler.postDelayed(ticket, 100L)) exitedAnalysisReleasesR77.remove(ticket.key, ticket);
    }

    private void cancelExitedAnalysisReleasesR77() {
        for (ExitedAnalysisReleaseR77 ticket : exitedAnalysisReleasesR77.values()) {
            ticket.handler.removeCallbacks(ticket);
            exitedAnalysisReleasesR77.remove(ticket.key, ticket);
        }
    }

    private final class ExitedAnalysisReleaseR77 implements Runnable {
        final AnalysisSingleFlightR36.Lease exited;
        final String key, error;
        final Handler handler;
        boolean released;
        ExitedAnalysisReleaseR77(AnalysisSingleFlightR36.Lease lease, String reason, Handler owner) {
            exited = lease; error = safe(reason); handler = owner;
            key = lease.request.attemptId() + "|" + lease.ownerId;
        }
        @Override public void run() {
            if (exitedAnalysisReleasesR77.get(key) != this) return;
            if (!running || userRequestedStopV5 || store == null || handler != analysisHandler
                    || AnalysisSingleFlightR36.shared().currentLease() == exited
                    || exited.token.isCanonicalCommitted()) {
                exitedAnalysisReleasesR77.remove(key, this); return;
            }
            long now = System.currentTimeMillis();
            boolean expired = now >= exited.request.deadlineAt
                    || TimeSourceR811.monoNowMs() >= exited.request.deadlineMonoAt;
            try {
                if (!released && !expired)
                    expired = exited.token.supersededByNewerTarget()
                            || store.getDrawByIssue(exited.targetIssue) != null;
                if (!released) {
                    AnalysisRequestStoreR36 requests = new AnalysisRequestStoreR36(store);
                    AnalysisRequestStoreR36.TransitionResult result = expired
                            ? requests.markExpired(exited.request, exited.ownerId, error, now)
                            : requests.markRetry(exited.request, exited.ownerId, error, now);
                    if (retryableExitTransitionR77(result)) {
                        if (!expired && handler.postDelayed(this, 250L)) return;
                        lastError = "R77_EXIT_RELEASE_UNRESOLVED:" + exited.targetIssue;
                    } else if (result.updated) {
                        // Release is complete before waking the existing pipeline. A wake failure
                        // may retry only the wake, never mutate the durable request a second time.
                        released = true;
                        if (AnalysisRequestStoreR36.RETRY_WAIT.equals(result.status) && !expired) {
                            long delay = Math.max(1L, result.retryAt - now);
                            if (handler.postDelayed(this, delay)) return;
                        }
                    }
                } else if (!expired) {
                    postAnalysisForLatest("R77_EXITED_OWNER_RELEASED", false);
                }
            } catch (RuntimeException failure) {
                lastError = "R77_EXIT_RELEASE:" + safeError(failure);
                if (!expired && handler.postDelayed(this, 250L)) return;
            }
            exitedAnalysisReleasesR77.remove(key, this);
        }
    }

    private static void renewAnalysisLeaseOrThrowR365(
            AnalysisRequestStoreR36 requests,
            AnalysisSingleFlightR36.Request request,
            AnalysisSingleFlightR36.Lease lease,
            String stage) {
        lease.token.checkpoint(stage);
        long now = System.currentTimeMillis();
        // Match the runtime owner: at most one lease write per 10 seconds. The final commit
        // still forces a durable CAS check; a stale owner never gains commit authority here.
        if (!lease.token.claimDurablePulse(now, false)) return;
        if (!requests.pulse(request, stage, lease.ownerId, now))
            throw new IllegalStateException("R366_DURABLE_LEASE_FENCE_LOST:" + stage);
    }

    private void postResearchCaptureR907(final String requestedTarget, final int attempt) {
        if (!running || userRequestedStopV5 || store == null) return;
        ResearchCaptureQueueR946.Ticket ticket = researchCaptureQueueR946.offer(requestedTarget);
        if (ticket != null) dispatchResearchCaptureR946(ticket, attempt <= 0 ? 10L : 450L);
    }

    private void dispatchResearchCaptureR946(
            final ResearchCaptureQueueR946.Ticket ticket, long delay) {
        final Handler owner = researchHandlerR908 != null ? researchHandlerR908 : learningHandler;
        if (!running || userRequestedStopV5 || owner == null || store == null) {
            researchCaptureQueueR946.rejected(ticket);
            return;
        }
        boolean posted = owner.postDelayed(
                new Runnable() {
                    @Override public void run() {
                        String target = researchCaptureQueueR946.begin(ticket);
                        if (target.isEmpty()) return;
                        boolean terminal = false;
                        ResearchWorkBudgetR927.Scope captureScope = ResearchWorkBudgetR927.enter(1800L);
                        try {
                            if (!running || userRequestedStopV5 || store == null) return;
                            if (ResearchCaptureQueueR946.drawn(store, target)) {
                                terminal = true;
                                store.setState("r907_research_async_status", "EXPIRED_NO_POSTDRAW_RECOMPUTE");
                                store.setState("r907_research_async_target", target);
                                return;
                            }
                            if (ResearchCaptureQueueR946.complete(store, target)) {
                                terminal = true;
                                store.setState("r907_research_async_status", "ALL_ROSTER_MEMBERS_FROZEN");
                                store.setState("r907_research_async_target", target);
                                return;
                            }
                            if (RealtimePressureGateR811.hasActiveCriticalWork()
                                    || SettlementExecutionGateR819.isActive()) {
                                store.setState("r907_research_async_status", "WAIT_P0_RETRY_QUEUE");
                                store.setState("r907_research_async_target", target);
                                return;
                            }
                            boolean evidenceOk = SelectedEvidenceContinuityR912.exists(store, target);
                            ResearchPredrawCaptureR923.Result result =
                                    ResearchPredrawCaptureR923.captureTarget(
                                            store,
                                            target,
                                            ResearchPredrawCaptureR923.MAX_TOTAL_BUDGET_MS);
                            java.util.LinkedHashMap<String, String> states =
                                    new java.util.LinkedHashMap<String, String>();
                            states.put(
                                    "r907_selected_evidence_async",
                                    evidenceOk ? "CANONICAL_ATOMIC" : "CANONICAL_MISSING");
                            states.put("r907_selected_evidence_target", target);
                            states.put(
                                    "r915_genesis_async_frozen",
                                    String.valueOf(result.genesisAssets));
                            states.put(
                                    "r908_algorithm_async_frozen",
                                    String.valueOf(result.algorithms));
                            states.put("r909_tiny_deep_async_frozen", result.tinyDeep);
                            states.put("r907_research_async_status", result.canonical());
                            states.put("r907_research_async_target", target);
                            states.put(
                                    "r907_research_async_cost_ms",
                                    String.valueOf(result.costMs));
                            store.trySetTelemetryStatesR77(states);
                            terminal = ResearchCaptureQueueR946.drawn(store, target)
                                    || ResearchCaptureQueueR946.complete(store, target)
                                    || "COMPLETE".equals(result.status)
                                    || result.status.contains("ALREADY_DRAWN")
                                    || result.status.contains("NO_RETROACTIVE_CAPTURE");
                        } catch (ResearchWorkBudgetR927.Yield yielded) {
                            try {
                                store.setState(
                                        "r907_research_async_status",
                                        "YIELD_RETRY_QUEUE:" + safe(yielded.getMessage()));
                                store.setState("r907_research_async_target", target);
                            } catch (Throwable ignored) {}
                        } catch (Throwable failure) {
                            try {
                                java.util.LinkedHashMap<String, String> failed =
                                        new java.util.LinkedHashMap<String, String>();
                                failed.put(
                                        "r907_research_async_status",
                                        "RETRY_ERROR:" + safeError(failure));
                                failed.put(
                                        "r907_research_async_error",
                                        ResearchPredrawCaptureR923.boundedFailure(failure));
                                failed.put("r907_research_async_target", target);
                                store.setStates(failed);
                            } catch (Throwable ignored) {}
                        } finally {
                            ResearchWorkBudgetR927.leave(captureScope);
                            if (!running || userRequestedStopV5 || store == null) {
                                researchCaptureQueueR946.stop();
                            } else {
                                ResearchCaptureQueueR946.Ticket next =
                                        researchCaptureQueueR946.finish(ticket, target, terminal);
                                if (next != null) dispatchResearchCaptureR946(next, 450L);
                            }
                        }
                    }
                },
                Math.max(0L, delay));
        if (!posted) researchCaptureQueueR946.rejected(ticket);
    }

    private void scheduleDeferredDrainR36() {
        final Handler h = backfillHandler != null ? backfillHandler : analysisHandler;
        if (h == null || store == null || !running
                || !deferredDrainPostedR367.compareAndSet(false, true)) return;
        boolean posted = h.post(
                new Runnable() {
                    @Override
                    public void run() {
                        if (!running || store == null) {
                            deferredDrainPostedR367.set(false);
                            return;
                        }
                        // Unknown is not empty: a transient read/write failure must not
                        // permanently stop recovery while durable jobs remain on disk.
                        int pending = -1;
                        try {
                            /* Deferred 100/1200 evidence is catch-up work.  Never take the DB
                             * writer away from a live canonical analysis, but do not leave a
                             * permanent backlog after one failed scheduling opportunity. */
                            boolean liveNeed = hasRealtimeAnalysisNeed();
                            boolean activeAnalysis =
                                    AnalysisSingleFlightR36.shared().activeCount() > 0;
                            if (!analysisBusy && !activeAnalysis) {
                                DeferredEvidenceWriterR36.recoverResourceFailures(store);
                                EssentialSettlementFeedbackR923.Result feedback =
                                        EssentialSettlementFeedbackR923.drain(store, 4, 500L);
                                store.setState(
                                        "r923_essential_feedback_drain", feedback.canonical());
                            }
                            RuntimeMemoryGuardR811.Snapshot memory =
                                    RuntimeMemoryGuardR811.snapshot("R817_DEFERRED_PREFLIGHT");
                            boolean memoryReady =
                                    memory.headroomBytes >= 48L * RuntimeMemoryGuardR811.MIB;
                            if (!analysisBusy
                                    && !activeAnalysis
                                    && !RealtimePressureGateR811.hasActiveCriticalWork()
                                    && memoryReady) {
                                ResearchExpertPredrawSnapshotR811.Snapshot expertSnapshot =
                                        pendingExpertSnapshotR811;
                                if (expertSnapshot != null
                                        && ResearchExpertPredrawSnapshotR811.persist(
                                                store, expertSnapshot)
                                        && pendingExpertSnapshotR811 == expertSnapshot) {
                                    pendingExpertSnapshotR811 = null;
                                }
                                DeferredEvidenceWriterR36.DrainReport deferred =
                                        DeferredEvidenceWriterR36.drainAvailable(
                                                store,
                                                liveNeed ? 2 : 8,
                                                "AutoFetchService-R902-"
                                                        + Thread.currentThread().getId());
                                store.setState("r902_deferred_drain_status", deferred.status);
                            } else if (!analysisBusy && !activeAnalysis && memoryReady) {
                                DeferredEvidenceWriterR36.DrainReport urgent =
                                        DeferredEvidenceWriterR36.drainSettledCatchup(
                                                store,
                                                4,
                                                "AutoFetchService-R902-settled-"
                                                        + Thread.currentThread().getId());
                                store.setState("r902_deferred_urgent_status", urgent.status);
                                store.setState("r902_deferred_urgent_slices",
                                        String.valueOf(urgent.attempted));
                            }
                            if (!analysisBusy
                                    && !activeAnalysis
                                    && memory.headroomBytes
                                            >= 96L * RuntimeMemoryGuardR811.MIB
                                    && store.coordinatorR36().isR811ResearchSchemaReady()) {
                                ResearchSettlementSidecarR811.Result researchSidecar =
                                        ResearchSettlementSidecarR811.drainBatch(store, 4, 1200L);
                                store.setState(
                                        "r902_research_settlement_status", researchSidecar.status);
                                store.setState(
                                        "r902_research_settlement_target",
                                        researchSidecar.targetIssue);
                            }
                            pending = DeferredEvidenceWriterR36.pendingCount(store);
                            DeferredEvidenceWriterR36.BacklogStats backlog =
                                    DeferredEvidenceWriterR36.backlogStats(store);
                            store.setState("r367_deferred_pending", String.valueOf(pending));
                            store.setState("r922_deferred_backlog_truth", backlog.summary());
                            store.setState(
                                    "r817_deferred_memory_ready", memoryReady ? "1" : "0");
                            store.setState(
                                    "r817_deferred_headroom_bytes",
                                    String.valueOf(memory.headroomBytes));
                        } catch (Throwable e) {
                            lastError = safeError(e);
                        } finally {
                            deferredDrainPostedR367.set(false);
                        }
                        if (DeferredEvidenceWriterR36.shouldContinueDrain(running, pending)) {
                            final long delay =
                                    DeferredEvidenceWriterR36.continuationDelayMs(pending,
                                            analysisBusy
                                                    || hasRealtimeAnalysisNeed()
                                                    || RealtimePressureGateR811.hasActiveCriticalWork());
                            h.postDelayed(
                                    new Runnable() {
                                        @Override
                                        public void run() {
                                            scheduleDeferredDrainR36();
                                        }
                                    },
                                    delay);
                        }
                    }
                });
        if (!posted) deferredDrainPostedR367.set(false);
    }

    private void runAnalysisChannelOnce() {
        if (!running) {
            return;
        }

        if (analysisBusy) {
            if (analysisHandler != null) {
                analysisHandler.postDelayed(analysisLoop, ANALYSIS_RETRY_DELAY_MS);
            }
            return;
        }

        String baseIssue = pendingAnalysisBaseIssue == null ? "" : pendingAnalysisBaseIssue;
        String reason = pendingAnalysisReason == null ? "" : pendingAnalysisReason;
        boolean force = pendingAnalysisForce;
        long myGeneration = pendingAnalysisGeneration;

        pendingAnalysisBaseIssue = "";
        pendingAnalysisReason = "";
        pendingAnalysisForce = false;
        pendingAnalysisGeneration = 0L;

        if (baseIssue.length() == 0) {
            return;
        }

        if (myGeneration <= 0L) {
            myGeneration = analysisGeneration;
        }

        analysisBusy = true;
        analysisStartedAt = System.currentTimeMillis();
        analysisCurrentBaseIssue = baseIssue;
        analysisCurrentTargetIssue = "";
        requestRealtimePriority("正式分析线程运行中，后台慢任务让路");

        long startedAt = analysisStartedAt;

        try {
            String targetIssueQuick = dataGuard.buildNextIssue(baseIssue);
            analysisCurrentTargetIssue = targetIssueQuick;

            if (store != null) {
                store.setState("analysis_target_issue", targetIssueQuick);
                store.setState("target_issue", targetIssueQuick);
                store.setState("current_issue", baseIssue);
                store.setState(
                        "analysis_core_chain", "V20_MAIN_CHAIN_DECISION_FIRST_LIGHT_REALTIME");
                store.setState("analysis_speed_mode", "REALTIME_LIGHT_MAIN_CHAIN_DECISION_FIRST");
                store.setState(
                        "analysis_formal_decision_target_ms",
                        String.valueOf(REALTIME_FORMAL_DECISION_TARGET_MS));
                store.setState("analysis_running_generation", String.valueOf(myGeneration));
            }

            setAnalysisState(
                    "RUNNING",
                    1,
                    "正式分析线程已接管，目标期 "
                            + targetIssueQuick
                            + "；主链=96期PIT实时核→完整1200动作→唯一DecisionGate；长窗口/画像后台补全");

            Models.Draw latestNow = store == null ? null : store.getLatestDraw();
            if (latestNow == null || !latestNow.isValid()) {
                setAnalysisState("SKIP", 0, "本地暂无有效开奖，无法分析");
                return;
            }

            if (!sameText(latestNow.issue, baseIssue)) {
                postAnalysisForIssue(latestNow.issue, "分析期间新期开奖，丢弃旧分析，重排最新期", true);
                return;
            }

            String targetIssue = dataGuard.buildNextIssue(baseIssue);
            analysisCurrentTargetIssue = targetIssue;

            String doneTarget =
                    store == null ? "" : store.getState("analysis_done_target_issue", "");
            Models.Decision existing =
                    store == null ? null : store.getDecisionByTargetIssue(targetIssue);

            if (existing != null
                    && !isWeakPlaceholderDecision(existing)
                    && sameText(existing.baseIssue, baseIssue)
                    && AppConfig.DECISION_BET.equals(existing.decision)) {
                setAnalysisState(
                        "OK",
                        100,
                        "目标期 " + targetIssue + " 已有正式BET，不重复分析：" + buildDecisionBrief(existing));
                store.setState("analysis_done_target_issue", targetIssue);
                clearRealtimePriorityIfDecisionReady(existing);
                ensureTradeCreatedForBetDecision(existing, "已有正式BET，确认Trade存在");
                return;
            }

            if (!force
                    && sameText(doneTarget, targetIssue)
                    && existing != null
                    && !isWeakPlaceholderDecision(existing)
                    && sameText(existing.baseIssue, baseIssue)) {
                setAnalysisState(
                        "OK",
                        100,
                        "目标期 " + targetIssue + " 已有正式预测，不重复分析：" + buildDecisionBrief(existing));
                return;
            }

            if (!force
                    && existing != null
                    && !isWeakPlaceholderDecision(existing)
                    && sameText(existing.baseIssue, baseIssue)) {
                setAnalysisState(
                        "OK",
                        100,
                        "目标期 " + targetIssue + " 已有正式预测：" + buildDecisionBrief(existing));
                store.setState("analysis_done_target_issue", targetIssue);
                return;
            }

            setAnalysisState("RUNNING", 5, "开始生成目标期 " + targetIssue + " 正式预测，原因：" + reason);
            updateRuntimeState(AppConfig.SERVICE_STATUS_ANALYZING, "");

            long tLoad = System.currentTimeMillis();
            List<Models.Draw> history = loadAnalysisHistory();
            ArrayList<Models.Draw> clean = dataGuard.normalizeHistory(history);
            clean = limitRealtimeHistory(clean);
            recordAnalysisCost("load_history", tLoad);

            if (!isAnalysisGenerationCurrent(myGeneration, baseIssue)) {
                setAnalysisState("SKIP", 0, "旧分析已作废：目标期已换代，不保存旧结果");
                return;
            }

            if (clean.size() == 0) {
                Models.Decision hold =
                        dataGuard.holdDecision(
                                baseIssue,
                                targetIssue,
                                AppConfig.BLOCK_HISTORY_WEAK,
                                "历史为空，无法生成正式候选，本期正式HOLD");

                if (isAnalysisGenerationCurrent(myGeneration, baseIssue)) {
                    hold = saveFinalDecision(hold);
                    setAnalysisState("OK", 100, "历史为空，已输出正式HOLD：" + buildDecisionBrief(hold));
                }
                return;
            }

            Models.Draw lastClean = clean.get(clean.size() - 1);
            if (lastClean == null || !sameText(lastClean.issue, baseIssue)) {
                Models.Draw newest = store.getLatestDraw();
                if (newest != null && newest.isValid()) {
                    postAnalysisForIssue(newest.issue, "分析历史已更新，重排最新期", true);
                }
                return;
            }

            setAnalysisState("RUNNING", 18, "历史准备完成，进入V20正式主链");

            Models.Decision decision = buildDecision(clean);

            if (!isAnalysisGenerationCurrent(myGeneration, baseIssue)) {
                setAnalysisState("SKIP", 0, "旧分析完成但已过期，禁止保存旧目标期结果");
                return;
            }

            if (decision == null) {
                decision =
                        dataGuard.holdDecision(
                                baseIssue,
                                targetIssue,
                                AppConfig.BLOCK_LOW_PROBABILITY,
                                "正式模型未生成有效BET/SHADOW，当前目标期正式HOLD");
            }

            if (decision.baseIssue == null || decision.baseIssue.length() == 0) {
                decision.baseIssue = baseIssue;
            }

            if (decision.targetIssue == null || decision.targetIssue.length() == 0) {
                decision.targetIssue = targetIssue;
            }

            Models.Draw afterAnalysisLatest = store.getLatestDraw();
            if (afterAnalysisLatest != null
                    && afterAnalysisLatest.isValid()
                    && !sameText(afterAnalysisLatest.issue, baseIssue)) {
                postAnalysisForIssue(afterAnalysisLatest.issue, "分析完成时新期开奖，立即重排最新期", true);
                return;
            }

            if (!isAnalysisGenerationCurrent(myGeneration, baseIssue)) {
                setAnalysisState("SKIP", 0, "保存前发现目标期已换代，旧结果不落库");
                return;
            }

            setAnalysisState("RUNNING", 88, "V20核心决策链已生成结果，正在完整保存并广播最终预测");

            decision = saveFinalDecision(decision);
            if (sameText(lastOomIssueB07, baseIssue)) {
                oomRetryCountB07 = 0;
                lastOomIssueB07 = "";
            }
            markRealtimeLayeredDecisionSaved(
                    baseIssue, decision.targetIssue, System.currentTimeMillis() - startedAt);

            long cost = System.currentTimeMillis() - startedAt;
            setAnalysisState(
                    "OK",
                    100,
                    "目标期 "
                            + decision.targetIssue
                            + " 正式预测完成，用时 "
                            + cost
                            + "ms："
                            + buildDecisionBrief(decision)
                            + "｜实时小核：前40完整小核裁决，160候选后台补全");

            backtestYieldRequested = false;
            backtestYieldReason = "";

            if (!AppConfig.ENABLE_V50_PRIME) {
                postTeacherMatrixAfterAnalysisSafely(baseIssue, "实时正式预测已完成，后台执行教师矩阵学习");
                postV20CauseChainAfterAnalysisSafely(baseIssue, "实时正式预测已完成，后台执行V20因链画像学习");
                postLearningTick("实时分析完成后后台学习同步", false);
                postMicroWalkForward("实时分析完成后OOS发布", true, 1200L);
            }
            updateRuntimeState(AppConfig.SERVICE_STATUS_RUNNING, "");
            refreshNotificationIfNeeded();

        } catch (Throwable e) {
            lastError = safeError(e);
            if (e instanceof OutOfMemoryError && handleAnalysisOomV7(baseIssue, reason)) {
                setAnalysisState("WAITING", 0, "B07 OOM清缓存串行重试：" + baseIssue + "｜" + lastError);
                updateRuntimeState(AppConfig.SERVICE_STATUS_ANALYZING, lastError);
                return;
            }
            setAnalysisState("ERROR", 0, "正式分析异常：" + lastError);

            try {
                String targetIssue = dataGuard == null ? "" : dataGuard.buildNextIssue(baseIssue);

                Models.Decision hold =
                        dataGuard.holdDecision(
                                baseIssue,
                                targetIssue,
                                AppConfig.BLOCK_DATA_ERROR,
                                "正式分析异常，保护性HOLD：" + lastError);

                if (isAnalysisGenerationCurrent(myGeneration, baseIssue)) {
                    hold = saveFinalDecision(hold);
                    setAnalysisState("OK", 100, "正式分析异常，已输出保护性HOLD：" + buildDecisionBrief(hold));
                }
            } catch (Throwable ignore) {
            }

            updateRuntimeState(AppConfig.SERVICE_STATUS_API_ERROR, lastError);
        } finally {
            analysisBusy = false;
            analysisStartedAt = 0L;
            analysisCurrentBaseIssue = "";
            analysisCurrentTargetIssue = "";

            if (pendingAnalysisBaseIssue != null
                    && pendingAnalysisBaseIssue.length() > 0
                    && analysisHandler != null) {
                analysisHandler.post(analysisLoop);
            }
        }
    }

    /**
     * B07 OOM recovery is serial on the existing analysis handler; it never starts a parallel
     * DecisionGate chain.
     */
    private boolean handleAnalysisOomV7(final String baseIssue, final String reason) {
        try {
            RollingWindowCache.shared().clear();
        } catch (Throwable ignore) {
        }
        try {
            FullInformationFeatureGraphEngine.shared().clear();
        } catch (Throwable ignore) {
        }
        try {
            System.gc();
        } catch (Throwable ignore) {
        }
        if (!sameText(lastOomIssueB07, baseIssue)) {
            lastOomIssueB07 = safe(baseIssue);
            oomRetryCountB07 = 0;
        }
        oomRetryCountB07++;
        try {
            if (store != null) {
                store.setState("b07_oom_issue", safe(baseIssue));
                store.setState("b07_oom_retry_count", String.valueOf(oomRetryCountB07));
                store.setState("b07_oom_policy", "CLEAR_CACHE_SERIAL_RETRY_NO_BET_RESCUE");
                store.setState("b07_oom_at", String.valueOf(System.currentTimeMillis()));
            }
            heartbeatStateV8()
                    .markBusinessStage(
                            "B07_OOM_RECOVERY",
                            "issue=" + safe(baseIssue) + ",retry=" + oomRetryCountB07);
        } catch (Throwable ignore) {
        }
        if (!running
                || analysisHandler == null
                || oomRetryCountB07 > AppConfig.B07_OOM_MAX_SERIAL_RETRY) return false;
        analysisHandler.postDelayed(
                new Runnable() {
                    @Override
                    public void run() {
                        if (running)
                            postAnalysisForIssue(baseIssue, "B07 OOM清缓存串行重试：" + safe(reason), true);
                    }
                },
                AppConfig.B07_OOM_RETRY_DELAY_MS);
        return true;
    }

    private boolean isAnalysisGenerationCurrent(long gen, String baseIssue) {
        if (gen != analysisGeneration) {
            return false;
        }

        try {
            if (store != null) {
                Models.Draw latest = store.getLatestDraw();
                if (latest != null && latest.isValid()) {
                    return sameText(latest.issue, baseIssue);
                }
            }
        } catch (Throwable e) {
        }

        return true;
    }

    private void recordObserveFullRunState(
            String baseIssue, String targetIssue, long costMs, String status, String detail) {
        if (!ANALYSIS_OBSERVE_FULL_RUN_MODE) {
            return;
        }
        long now = System.currentTimeMillis();
        if (analysisObserveLastLogAt > 0L
                && now - analysisObserveLastLogAt < ANALYSIS_OBSERVE_LOG_INTERVAL_MS) {
            return;
        }
        analysisObserveLastLogAt = now;
        try {
            if (store != null) {
                store.setState("analysis_observe_full_run_mode", "1");
                store.setState("analysis_observe_status", safe(status));
                store.setState("analysis_observe_base", safe(baseIssue));
                store.setState("analysis_observe_target", safe(targetIssue));
                store.setState("analysis_observe_cost_ms", String.valueOf(costMs));
                store.setState("analysis_observe_detail", safe(detail));
                store.setState(
                        "analysis_observe_policy", "不限制18秒重启；不输出保护HOLD；不快裁；不砍策略；记录完整链路到底多久跑完");
                store.setState("analysis_observe_at", String.valueOf(now));
            }
        } catch (Throwable e) {
        }
        setAnalysisState("RUNNING", 18, safe(detail) + "｜已运行 " + costMs + "ms");
    }

    private void startAnalysisRescueThread(
            final String baseIssue, final String reason, boolean force) {
        if (!running
                || baseIssue == null
                || baseIssue.length() == 0
                || dataGuard == null
                || store == null) {
            return;
        }
        if (System.currentTimeMillis() >= 0L) {
            try {
                store.setState("analysis_rescue_status", "DISABLED_FIX05_DECISION_FIRST");
                store.setState(
                        "analysis_rescue_policy",
                        "FIX05禁用AutoFetch救援线程：不并行重跑buildDecision，不快裁BET，不救援BET；只等待正式分析线程的DecisionGate唯一出口");
                setAnalysisState("RUNNING", 18, "正式分析仍在运行：FIX05不启动救援线程，不输出保护HOLD，不快裁BET");
            } catch (Throwable e) {
            }
            return;
        }
        long now = System.currentTimeMillis();
        if (analysisRescueBusy && sameText(analysisRescueBaseIssue, baseIssue)) {
            return;
        }
        if (analysisRescueLastStartAt > 0L
                && now - analysisRescueLastStartAt < ANALYSIS_RESCUE_MIN_INTERVAL_MS) {
            return;
        }
        analysisRescueBusy = true;
        analysisRescueStartedAt = now;
        analysisRescueLastStartAt = now;
        analysisRescueBaseIssue = baseIssue;
        final long rescueGeneration = prepareAnalysisGenerationForPost(baseIssue, true);
        final String rescueReason = reason == null ? "" : reason;
        try {
            store.setState("analysis_rescue_status", "START");
            store.setState("analysis_rescue_base", baseIssue);
            store.setState("analysis_rescue_generation", String.valueOf(rescueGeneration));
            store.setState("analysis_rescue_reason", rescueReason);
            store.setState(
                    "analysis_rescue_policy",
                    "完整正式策略救援线程：同一个buildDecision，同一个DecisionGate，不保护HOLD，不快裁BET");
            setAnalysisState(
                    "RUNNING", 12, "主分析线程未及时产出，启动完整正式救援线程；目标基期 " + baseIssue + "；不牺牲策略，不输出保护HOLD");
        } catch (Throwable e) {
        }
        Thread t =
                new Thread(
                        new Runnable() {
                            public void run() {
                                long started = System.currentTimeMillis();
                                try {
                                    try {
                                        Process.setThreadPriority(
                                                Process.THREAD_PRIORITY_URGENT_DISPLAY);
                                    } catch (Throwable ignore) {
                                    }
                                    requestRealtimePriority("正式分析救援线程运行中，后台慢任务让路");
                                    String targetIssue = dataGuard.buildNextIssue(baseIssue);
                                    if (targetIssue == null || targetIssue.length() == 0) {
                                        return;
                                    }
                                    Models.Decision existing =
                                            store.getDecisionByTargetIssue(targetIssue);
                                    if (existing != null
                                            && !isWeakPlaceholderDecision(existing)
                                            && sameText(existing.baseIssue, baseIssue)) {
                                        store.setState("analysis_rescue_status", "SKIP_EXISTING");
                                        store.setState("analysis_done_target_issue", targetIssue);
                                        setAnalysisState(
                                                "OK",
                                                100,
                                                "救援线程发现目标期已有正式预测：" + buildDecisionBrief(existing));
                                        return;
                                    }
                                    Models.Draw latestNow = store.getLatestDraw();
                                    if (latestNow == null
                                            || !latestNow.isValid()
                                            || !sameText(latestNow.issue, baseIssue)) {
                                        store.setState(
                                                "analysis_rescue_status", "SKIP_ISSUE_CHANGED");
                                        return;
                                    }
                                    store.setState("analysis_rescue_status", "RUNNING");
                                    store.setState("analysis_rescue_target", targetIssue);
                                    store.setState(
                                            "analysis_rescue_started_at", String.valueOf(started));
                                    setAnalysisState(
                                            "RUNNING",
                                            18,
                                            "正式分析救援线程已接管目标期 "
                                                    + targetIssue
                                                    + "，开始跑完整buildDecision主链");
                                    List<Models.Draw> history = loadAnalysisHistory();
                                    ArrayList<Models.Draw> clean =
                                            dataGuard.normalizeHistory(history);
                                    clean = limitRealtimeHistory(clean);
                                    if (clean == null || clean.size() == 0) {
                                        store.setState(
                                                "analysis_rescue_status", "SKIP_EMPTY_HISTORY");
                                        return;
                                    }
                                    Models.Draw last = clean.get(clean.size() - 1);
                                    if (last == null || !sameText(last.issue, baseIssue)) {
                                        store.setState(
                                                "analysis_rescue_status", "SKIP_HISTORY_CHANGED");
                                        return;
                                    }
                                    Models.Decision decision = buildDecision(clean);
                                    if (!isAnalysisGenerationCurrent(rescueGeneration, baseIssue)) {
                                        store.setState(
                                                "analysis_rescue_status", "SKIP_GENERATION_OLD");
                                        return;
                                    }
                                    latestNow = store.getLatestDraw();
                                    if (latestNow == null
                                            || !latestNow.isValid()
                                            || !sameText(latestNow.issue, baseIssue)) {
                                        store.setState(
                                                "analysis_rescue_status",
                                                "SKIP_SAVE_ISSUE_CHANGED");
                                        return;
                                    }
                                    if (decision == null) {
                                        decision =
                                                dataGuard.holdDecision(
                                                        baseIssue,
                                                        targetIssue,
                                                        AppConfig.BLOCK_LOW_PROBABILITY,
                                                        "完整正式救援线程跑完后 DecisionGate 未生成有效"
                                                            + " BET/SHADOW，当前目标期正式HOLD");
                                    }
                                    if (decision.baseIssue == null
                                            || decision.baseIssue.length() == 0) {
                                        decision.baseIssue = baseIssue;
                                    }
                                    if (decision.targetIssue == null
                                            || decision.targetIssue.length() == 0) {
                                        decision.targetIssue = targetIssue;
                                    }
                                    existing = store.getDecisionByTargetIssue(targetIssue);
                                    if (existing != null
                                            && !isWeakPlaceholderDecision(existing)
                                            && sameText(existing.baseIssue, baseIssue)) {
                                        store.setState(
                                                "analysis_rescue_status",
                                                "SKIP_EXISTING_AFTER_RUN");
                                        store.setState("analysis_done_target_issue", targetIssue);
                                        setAnalysisState(
                                                "OK",
                                                100,
                                                "救援线程完成时目标期已有正式预测：" + buildDecisionBrief(existing));
                                        return;
                                    }
                                    decision = saveFinalDecision(decision);
                                    long cost = System.currentTimeMillis() - started;
                                    markRealtimeLayeredDecisionSaved(
                                            baseIssue, decision.targetIssue, cost);
                                    store.setState("analysis_rescue_status", "OK");
                                    store.setState("analysis_rescue_cost_ms", String.valueOf(cost));
                                    store.setState(
                                            "analysis_done_target_issue", decision.targetIssue);
                                    setAnalysisState(
                                            "OK",
                                            100,
                                            "救援线程完成正式预测，用时 "
                                                    + cost
                                                    + "ms："
                                                    + buildDecisionBrief(decision));
                                    if (!AppConfig.ENABLE_V50_PRIME) {
                                        postTeacherMatrixAfterAnalysisSafely(
                                                baseIssue, "救援线程正式预测完成，后台执行教师矩阵学习");
                                        postV20CauseChainAfterAnalysisSafely(
                                                baseIssue, "救援线程正式预测完成，后台执行V20因链画像学习");
                                        postLearningTick("救援线程实时分析完成后后台学习同步", false);
                                        postMicroWalkForward("救援分析完成后OOS发布", true, 1200L);
                                    }
                                    updateRuntimeState(AppConfig.SERVICE_STATUS_RUNNING, "");
                                    refreshNotificationIfNeeded();
                                } catch (Throwable e) {
                                    lastError = safeError(e);
                                    try {
                                        store.setState("analysis_rescue_status", "ERROR");
                                        store.setState("analysis_rescue_error", lastError);
                                    } catch (Throwable ignore) {
                                    }
                                    setAnalysisState("ERROR", 0, "正式分析救援线程异常：" + lastError);
                                } finally {
                                    analysisRescueBusy = false;
                                    analysisRescueStartedAt = 0L;
                                    analysisRescueBaseIssue = "";
                                    try {
                                    } catch (Throwable e) {
                                    }
                                }
                            }
                        },
                        "eth_k3_formal_rescue_" + System.currentTimeMillis());
        try {
            t.setPriority(Thread.MAX_PRIORITY);
        } catch (Throwable e) {
        }
        t.start();
    }

    private void hardResetAnalysisThreadAndRepost(String baseIssue, String reason) {
        if (!running || baseIssue == null || baseIssue.length() == 0) {
            return;
        }
        /* R3.5 forbids thread replacement while a complete owner is alive.  The
         * watchdog only asks the current token to stop at a cooperative checkpoint
         * and queues one successor on the same HandlerThread. */
        try {
            long nextGeneration = prepareAnalysisGenerationForPost(baseIssue, true);
            pendingAnalysisBaseIssue = baseIssue;
            pendingAnalysisReason = reason == null ? "R35_COOPERATIVE_REQUEUE" : reason;
            pendingAnalysisForce = true;
            pendingAnalysisGeneration = nextGeneration;
            analysisPostedAt = System.currentTimeMillis();
            AnalysisSingleFlightR36.shared().cancelIfNoProgress(System.currentTimeMillis());
            if (store != null) {
                store.setState(
                        "analysis_hard_reset_at", String.valueOf(System.currentTimeMillis()));
                store.setState("analysis_hard_reset_reason", pendingAnalysisReason);
                store.setState(
                        "analysis_hard_reset_policy",
                        "R35_COOPERATIVE_CANCEL_NO_THREAD_REPLACEMENT");
                store.setState("analysis_generation", String.valueOf(nextGeneration));
            }
            setAnalysisState("WAITING", 0, "R3.5协作取消并串行重排：" + baseIssue + "；不重建线程、不并发主链");
            if (analysisHandler != null) analysisHandler.post(analysisLoop);
        } catch (Throwable e) {
            lastError = safeError(e);
            setAnalysisState("ERROR", 0, "R3.5串行重排失败：" + lastError);
        }
    }

    private void postCountdownWorkR964(long countdownMs) {
        countdownValueR964=countdownMs;
        final Handler owner=analysisWatchdogHandler;
        if(!running||owner==null||!countdownWorkPostedR964.compareAndSet(false,true))return;
        if(!owner.post(new Runnable(){public void run(){
            try{
                if(!running||owner!=analysisWatchdogHandler)return;
                long countdown=countdownValueR964;
                if(countdown<=0L||isRealtimeLikeWindow())postFetchNow();
                runAnalysisWatchdogOnce();
                ensureRuntimeLoopsAliveV17("countdown_tick");
                refreshNotificationIfNeeded();
            }catch(RuntimeException failure){
                lastError="COUNTDOWN_WORK:"+safeError(failure);
                android.util.Log.e("AutoFetchService",lastError,failure);
            }finally{if(owner==analysisWatchdogHandler)countdownWorkPostedR964.set(false);}
        }}))countdownWorkPostedR964.set(false);
    }

    private void runAnalysisWatchdogOnce() {
        // Countdown/manual callbacks must never enter SQLite on Android's UI Looper.
        if(android.os.Looper.myLooper()==android.os.Looper.getMainLooper()){
            postCountdownWorkR964(countdownValueR964);return;
        }
        if (!running || store == null || dataGuard == null) {
            return;
        }
        if (useR36Runtime()) {
            runAnalysisWatchdogR36();
            return;
        }

        long now = System.currentTimeMillis();

        try {
            Models.Draw latest = store.getLatestDraw();
            if (latest == null || !latest.isValid()) {
                return;
            }

            String baseIssue = latest.issue == null ? "" : latest.issue;
            String targetIssue = dataGuard.buildNextIssue(baseIssue);

            if (targetIssue == null || targetIssue.length() == 0) {
                return;
            }

            Models.Decision existing = store.getDecisionByTargetIssue(targetIssue);

            if (existing != null
                    && !isWeakPlaceholderDecision(existing)
                    && sameText(existing.baseIssue, baseIssue)) {
                if (!"OK".equals(store.getState("analysis_status", ""))) {
                    setAnalysisState(
                            "OK",
                            100,
                            "目标期 " + targetIssue + " 已有正式预测：" + buildDecisionBrief(existing));
                }
                store.setState("analysis_done_target_issue", targetIssue);

                if (AppConfig.DECISION_BET.equals(existing.decision)) {
                    ensureTradeCreatedForBetDecision(existing, "看门狗确认正式BET Trade存在");
                }

                return;
            }

            if (existing != null
                    && isWeakPlaceholderDecision(existing)
                    && sameText(existing.baseIssue, baseIssue)
                    && !analysisBusy) {
                if (now - analysisLastWatchdogKickAt >= ANALYSIS_WAITING_STALE_MS) {
                    analysisLastWatchdogKickAt = now;
                    requestRealtimePriority("当前目标期只有弱占位，强制重跑正式分析");
                    requestBacktestYield("当前目标期只有弱占位，强制重跑正式分析");
                    setAnalysisState("WAITING", 0, "目标期 " + targetIssue + " 只有弱占位预测，正在强制重跑正式模型");
                    postAnalysisForIssue(baseIssue, "弱占位不能作为最终预测，强制重跑正式模型", true);
                }
                return;
            }

            String status = store.getState("analysis_status", "");
            long updatedAt = parseLong(store.getState("analysis_updated_at", "0"), 0L);

            if (updatedAt <= 0L) {
                updatedAt = analysisPostedAt;
            }

            if (analysisBusy) {
                long busyCost = analysisStartedAt <= 0L ? 0L : now - analysisStartedAt;
                boolean changedBase =
                        analysisCurrentBaseIssue != null
                                && analysisCurrentBaseIssue.length() > 0
                                && !sameText(analysisCurrentBaseIssue, baseIssue);

                if (changedBase && busyCost >= ANALYSIS_BUSY_REPLACE_MS) {
                    analysisLastWatchdogKickAt = now;
                    hardResetAnalysisThreadAndRepost(
                            baseIssue,
                            "旧分析卡在 " + analysisCurrentBaseIssue + "，当前最新=" + baseIssue + "，强制换代");
                    return;
                }

                if (!changedBase && busyCost >= ANALYSIS_BUSY_HARD_RESET_MS) {
                    analysisLastWatchdogKickAt = now;
                    try {
                        store.setState("analysis_stall_recovery", "HARD_RESET_SAME_ISSUE");
                        store.setState("analysis_stall_cost_ms", String.valueOf(busyCost));
                        heartbeatStateV8()
                                .markBusinessStage(
                                        "ANALYSIS_STALL_RECOVERY",
                                        "issue=" + baseIssue + ",cost=" + busyCost);
                    } catch (Throwable ignore) {
                    }
                    hardResetAnalysisThreadAndRepost(
                            baseIssue,
                            "同一期实时分析超过" + ANALYSIS_BUSY_HARD_RESET_MS + "ms无结果，硬换代重跑；禁止并发BET救援");
                    return;
                }

                if (busyCost >= ANALYSIS_RESCUE_TAKEOVER_MS) {
                    recordObserveFullRunState(
                            baseIssue,
                            targetIssue,
                            busyCost,
                            "WATCHDOG_LONG_RUNNING_NO_RESTART",
                            "轻主链超时观察：记录耗时，不快裁BET，不救援BET，等待DecisionGate返回");
                }

                if (busyCost >= ANALYSIS_BUSY_NOTICE_MS
                        && now - analysisLastWatchdogKickAt >= ANALYSIS_WATCHDOG_INTERVAL_MS) {
                    analysisLastWatchdogKickAt = now;
                    requestRealtimePriority("正式分析运行中，后台慢任务继续让路");
                    requestBacktestYield("正式分析线程运行中，要求回测让路");

                    int p = 8 + (int) (busyCost / 650L);
                    if (p > 86) {
                        p = 86;
                    }

                    String msg =
                            "正式分析线程仍在运行，目标期 "
                                    + analysisCurrentTargetIssue
                                    + "，已运行 "
                                    + busyCost
                                    + "ms；核心链路不降级，后台任务已让路";

                    if (busyCost >= ANALYSIS_BUSY_WARN_MS) {
                        msg =
                                appendReason(
                                        msg, "轻主链耗时偏长：重型画像已后台化；不快裁BET、不救援BET，只等DecisionGate唯一出口返回");
                    }

                    setAnalysisState("RUNNING", p, msg);
                }

                return;
            }

            boolean staleWaiting =
                    "WAITING".equals(status)
                            && updatedAt > 0L
                            && now - updatedAt >= ANALYSIS_WAITING_STALE_MS;
            boolean noStatusButNeed =
                    (status == null
                                    || status.length() == 0
                                    || "IDLE".equals(status)
                                    || "SKIP".equals(status))
                            && existing == null;

            if (staleWaiting || noStatusButNeed) {
                if (now - analysisLastWatchdogKickAt < ANALYSIS_WAITING_STALE_MS) {
                    return;
                }

                analysisLastWatchdogKickAt = now;
                long observeWaitingCost = updatedAt > 0L ? now - updatedAt : 0L;
                requestRealtimePriority("观察模式：分析等待偏久，后台继续让路，目标期 " + targetIssue);
                requestBacktestYield("观察模式：分析等待偏久，后台继续让路，目标期 " + targetIssue);
                setAnalysisState("WAITING", 0, "目标期 " + targetIssue + " 缺正式预测，主线程空闲，按唯一V51主链重新排队");
                recordObserveFullRunState(
                        baseIssue,
                        targetIssue,
                        observeWaitingCost,
                        "MISSING_PREDICTION_REPOST",
                        "只重排同一唯一主链，不并发救援BET");
                postAnalysisForIssue(baseIssue, "缺预测看门狗重排：唯一V51主链", true);
            }
        } catch (Throwable e) {
        }
    }

    private void runAnalysisWatchdogR36() {
        long now = System.currentTimeMillis();
        if (!running || userRequestedStopV5 || store == null) return;
        try {
            repairDeadAnalysisWorkerR811("R924_ANALYSIS_WATCHDOG");
            AnalysisSingleFlightR36.shared().cancelIfNoProgress(now);
            // Always reconcile the latest draw before considering a stale durable request.
            // Reconcile is idempotent and the sole analysis owner cancels a superseded target.
            if (now - lastAnalysisReconcileAtR816 >= R816_ANALYSIS_RECONCILE_INTERVAL_MS) {
                lastAnalysisReconcileAtR816 = now;
                postAnalysisForLatest("R924_LATEST_FIRST_WATCHDOG", false);
            }
            if (now - lastSettlementWatchdogAtR816 >= 5000L) {
                lastSettlementWatchdogAtR816 = now;
                scheduleSettlementBacklogMaintenanceR817("R924_WATCHDOG_DISPATCH", 0L);
            }
            if (!RealtimePressureGateR811.hasActiveCriticalWork()) {
                if (now - lastProductionMaintenanceAtR816 >= R816_PRODUCTION_MAINTENANCE_INTERVAL_MS) {
                    lastProductionMaintenanceAtR816 = now;
                    scheduleDeferredDrainR36();
                }
                if (now - lastResearchTickAtR816 >= R816_RESEARCH_TICK_INTERVAL_MS) {
                    lastResearchTickAtR816 = now;
                    postAutonomousResearchTickR908("R924_WATCHDOG_DISPATCH");
                }
            }
        } catch (Throwable error) {
            lastError = safeError(error);
            CrashRecorder.recordCaught(getApplicationContext(), error, "R924_RECONCILE_WATCHDOG");
        }
    }

    /**
     * R908 autonomous Research dispatcher.  The production watchdog only posts here; all schema,
     * sidecar settlement, invention, evaluation and evolution work executes on the isolated
     * background Research looper.  Failure/yield can never delay fetch, canonical commit or
     * Production settlement.
     */
    // One delayed continuation per owner. Fresh draw/watchdog triggers may arrive
    // during the delay; anonymous self-posts used to leave another live retry chain.
    private final Runnable researchContinuationR70 = new Runnable() {
        @Override public void run() {
            postAutonomousResearchTickR908("R959_SAME_PERIOD_CONTINUATION");
        }
    };

    /** Independent callback turns keep maintenance from consuming every continuity budget.
     * Advance durably before work so a yield, error or process restart cannot pin the wheel.
     * No turn receipt is training, settlement or certificate evidence. */
    static int claimResearchCallbackTurnR75(final DbStore store) {
        return store.coordinatorR36().runAtomicWriteOrThrowF01("R75_RESEARCH_CALLBACK_TURN",
                new DatabaseCoordinator.WriteCallable<Integer>() {
                    @Override public Integer call(android.database.sqlite.SQLiteDatabase db) {
                        int turn = 0;
                        try (android.database.Cursor c = db.rawQuery(
                                "SELECT state_value FROM app_state WHERE state_key='r75_research_callback_turn'", null)) {
                            if (c.moveToFirst()) turn = Integer.parseInt(c.getString(0));
                        }
                        if (turn < 0 || turn > 3) throw new IllegalStateException("R75_RESEARCH_CALLBACK_TURN_INVALID");
                        db.execSQL("INSERT OR REPLACE INTO app_state(state_key,state_value,updated_at) VALUES(?,?,?)",
                                new Object[] {"r75_research_callback_turn", Integer.toString((turn + 1) % 4), System.currentTimeMillis()});
                        return turn;
                    }
                });
    }

    private void postAutonomousResearchTickR908(final String reason) {
        final Handler owner = researchHandlerR908 != null ? researchHandlerR908 : learningHandler;
        if (!running || owner == null || store == null || researchEvolutionR811 == null) return;
        if (!researchTickPostedR908.compareAndSet(false, true)) return;
        try { heartbeatStateV8().markLearningAttempt("POSTED_R955", safe(reason)); }
        catch (Throwable ignored) {}
        boolean accepted = owner.post(new Runnable() {
            @Override public void run() {
                owner.removeCallbacks(researchContinuationR70);
                long started = TimeSourceR811.monoNowMs();
                ResearchWorkBudgetR927.Scope callbackScope=ResearchWorkBudgetR927.enter(2000L);
                boolean maintenanceTurnR75 = false;
                try {
                    if (!running || store == null) return;
                    if (RealtimePressureGateR811.hasActiveCriticalWork()) {
                        store.setState("r908_research_tick_status", "YIELD_P0_PRESSURE");
                        heartbeatStateV8().markLearningAttempt(
                                "V51_OWNER_YIELD", "P0_PRESSURE|" + safe(reason));
                        return;
                    }
                    boolean schemaReady = store.coordinatorR36().isR811ResearchSchemaReady();
                    if (!schemaReady) schemaReady = store.ensureResearchSidecarR811();
                    if (!schemaReady) {
                        store.setState("r908_research_tick_status", "YIELD_SCHEMA_NOT_READY");
                        heartbeatStateV8().markLearningAttempt(
                                "V51_OWNER_YIELD", "SCHEMA_NOT_READY");
                        return;
                    }
                    // C / retention / C / reclaim. A maintenance callback never spends the
                    // following continuity owner's 1000ms child slice. If maintenance is not
                    // due its reserved turn is immediately lent to continuity.
                    int callbackTurnR75 = claimResearchCallbackTurnR75(store);
                    if (callbackTurnR75 == 1) {
                        long retentionNow=System.currentTimeMillis();
                        long retentionDue=0L;
                        try{retentionDue=Long.parseLong(store.getState("r979_retention_due_at","0"));}catch(NumberFormatException ignored){}
                        boolean retentionRequestedCP4="1".equals(store.getState("cp4_retention_requested","0"));
                        if(retentionRequestedCP4||retentionNow>=retentionDue) {
                            maintenanceTurnR75 = true;
                            ResearchWorkBudgetR927.Scope retentionScope=ResearchWorkBudgetR927.enter(750L);
                            try {
                                String retentionResultR48 = DataRetentionGovernorR900.trim(store,150L);
                                // Pending pages and preemption continue in short bounded slices. A
                                // minute cooldown belongs only to a fully scanned, empty backlog.
                                long retentionDelayR70=retentionResultR48.equals("RETENTION_NO_ELIGIBLE_ROWS")
                                        ?60000L:5000L;
                                store.setState("r979_retention_due_at",Long.toString(System.currentTimeMillis()+retentionDelayR70));
                                if (retentionResultR48.startsWith("RETENTION_DEFERRED_DRAWS_APPLIED:")) {
                                    store.setState("r48_draw_ingest_status", "DEFERRED_DRAW_DURABLY_REPLAYED");
                                    postFetchNow(); // fresh durable history restores clock and normal current-draw dispatch
                                    postAnalysisForLatest("R48_RETENTION_REPLAY_REQUEST_FIRST", true);
                                }
                            }
                            catch(ResearchWorkBudgetR927.Yield bounded){ /* Existing pinned rows remain for the next slice. */ }
                            finally {ResearchWorkBudgetR927.leave(retentionScope);}
                            return;
                        }
                    }
                    if (callbackTurnR75 == 3) {
                        long lastReclaimR75=0L;
                        try{lastReclaimR75=Long.parseLong(store.getState("r943_storage_reclaim_at","0"));}catch(NumberFormatException ignored){}
                        if(System.currentTimeMillis()-lastReclaimR75>=120000L) {
                            maintenanceTurnR75 = true;
                            StorageReclaimR943.quiet(store);
                            return;
                        }
                    }
                    ResearchRuntimeSqlGateR911.Report sqlGate =
                            ResearchRuntimeSqlGateR911.verifyAndPublish(store);
                    if (!sqlGate.passed) {
                        store.setState(
                                "r908_research_tick_status",
                                "YIELD_R911_SQL_CONTRACT:" + sqlGate.detail);
                        heartbeatStateV8().markLearningAttempt(
                                "V51_OWNER_YIELD", "SQL_CONTRACT:" + sqlGate.detail);
                        return;
                    }
                    // One research owner replaces the six-phase path. The capture queue remains the only
                    // pre-draw owner; continuity advances every period in bounded independent lanes.
                    String pendingCapture=ResearchCaptureQueueR946.recover(store);
                    if(!pendingCapture.isEmpty())postResearchCaptureR907(pendingCapture,0);
                    ResearchLearningContinuityR912.Result continuity=ResearchLearningContinuityR912.pulse(store,1000L);
                    store.setState("r908_research_tick_status",continuity.status);
                    store.setState("r908_research_predraw_settle",continuity.status);
                    if(continuity.success)heartbeatStateV8().markLearningPump("R959_PERIOD_PROGRESS",continuity.status);
                    else heartbeatStateV8().markLearningAttempt("R959_PERIOD_PENDING",continuity.status);
                    if(!ResearchLearningContinuityR912.needsContinuation(store)
                            && continuity.status.startsWith("PERIOD_COMPLETE")
                            && continuity.completedLanes>0) {
                        DiagnosticFileStoreR941.prune(new java.io.File(getCacheDir(),"r923_reports"),null);
                    }
                } catch (ResearchWorkBudgetR927.Yield yielded) {
                    try {store.setState("r908_research_tick_status",yielded.getMessage());}catch(Throwable diagnosticYield){}
                    try { heartbeatStateV8().markLearningAttempt("V51_OWNER_YIELD", safe(yielded.getMessage())); }
                    catch (Throwable ignore) {}
                } catch (Throwable error) {
                    try { store.setState("r908_research_tick_status", "ERROR:" + safeError(error)); } catch (Throwable ignore) {}
                    try { heartbeatStateV8().markLearningAttempt("V51_OWNER_ERROR", safeError(error)); }
                    catch (Throwable ignore) {}
                } finally {
                    ResearchWorkBudgetR927.leave(callbackScope);
                    researchTickPostedR908.set(false);
                    try {
                        if (store != null) {
                            store.setState("r908_research_tick_cost_ms", String.valueOf(Math.max(0L, TimeSourceR811.monoNowMs()-started)));
                            store.setState("r908_research_tick_at", String.valueOf(System.currentTimeMillis()));
                        }
                    } catch (Throwable ignore) {}
                    owner.removeCallbacks(researchContinuationR70);
                    if(running && store!=null && (maintenanceTurnR75 || ResearchLearningContinuityR912.needsContinuation(store)))
                        owner.postDelayed(researchContinuationR70,
                                maintenanceTurnR75 ? 500L : ResearchLearningContinuityR912.nextContinuationDelayMs(store));
                }
            }
        });
        if(!accepted){
            researchTickPostedR908.set(false);
            try { heartbeatStateV8().markLearningAttempt("POST_REJECTED_R955", safe(reason)); }
            catch (Throwable ignored) {}
        }
    }

    private List<Models.Draw> loadAnalysisHistory() {
        int limit =
                AppConfig.ENABLE_V50_PRIME ? AppConfig.WINDOW_3000 : REALTIME_DB_HISTORY_LOAD_LIMIT;

        if (limit < REALTIME_ANALYSIS_HISTORY_LIMIT + 60) {
            limit = REALTIME_ANALYSIS_HISTORY_LIMIT + 60;
        }

        if (limit > AppConfig.WINDOW_3000) {
            limit = AppConfig.WINDOW_3000;
        }

        List<Models.Draw> local = store.getRecentDraws(limit);

        long now = System.currentTimeMillis();

        int required =
                AppConfig.ENABLE_V50_PRIME
                        ? HistoricalDrawImportEngine.V50_RUNTIME_MINIMUM_ROWS
                        : AppConfig.MIN_DRAWS_ANALYZE;

        int localContinuous =
                AppConfig.ENABLE_V50_PRIME
                        ? V50RuntimeOrchestrator.recentContinuousCount(local)
                        : (local == null ? 0 : local.size());
        if (localContinuous >= required) {
            try {
                Models.Draw latestLocal = store.getLatestDraw();
                if (latestLocal != null && latestLocal.isValid()) {
                    saveLatestDrawState(latestLocal, "local_history_fast");
                }
            } catch (Throwable e) {
            }
            return local;
        }

        if (now - lastHistoryFetchAt < 20L * 1000L) {
            return ensureV50HistoryBootstrap(local, limit, "HISTORY_FETCH_DEBOUNCED");
        }

        try {
            Models.ApiResult r = apiClient.fetchHistory(AppConfig.BACKFILL_LATEST_LIMIT);
            if (r != null && r.ok && r.draws != null && r.draws.size() > 0) {
                store.saveDraws(r.draws);
                lastHistoryFetchAt = now;

                if (r.apiLine != null && r.apiLine.length() > 0) {
                    store.setState("api_line", r.apiLine);
                }

                Models.Draw latest = store.getLatestDraw();
                if (latest != null && latest.isValid()) {
                    saveLatestDrawState(latest, "analysis_history_fetch");
                    broadcastDrawsUpdated(true);
                    settleLatestSafely(latest);
                    postRealBetDiagnosisRefresh(
                            "分析补历史结算后刷新真实BET复盘诊断", REAL_BET_DIAG_AFTER_SETTLEMENT_DELAY_MS, true);
                    postFullDiagnosisRefresh(
                            "分析补历史结算后刷新满级诊断闭环", FULL_DIAG_LOOP_AFTER_SETTLEMENT_DELAY_MS, true);

                    if (!shouldDeferHeavyBackgroundNow()) {
                        postTeacherMatrixForDraw(
                                latest, "分析补历史后后台同步教师矩阵", false, TEACHER_MATRIX_IDLE_DELAY_MS);
                        postV20CauseChainForDraw(
                                latest, "分析补历史后后台同步V20因链", false, V20_CAUSE_CHAIN_IDLE_DELAY_MS);
                    }
                }

                return ensureV50HistoryBootstrap(
                        store.getRecentDraws(limit), limit, "API_HISTORY_FETCHED");
            }
        } catch (Throwable e) {
            lastError = safeError(e);
        }

        return ensureV50HistoryBootstrap(local, limit, "API_HISTORY_UNAVAILABLE");
    }

    /**
     * V50 needs 96 consecutive completed draws for full PIT mode. A recent continuous prefix of
     * 12-95 draws still produces a SHADOW NumberP/K3, but a legacy-valid 30-row prefix must not
     * short-circuit bootstrap.
     */
    private List<Models.Draw> ensureV50HistoryBootstrap(
            List<Models.Draw> current, int limit, String source) {
        if (!AppConfig.ENABLE_V50_PRIME) return current;
        int before = current == null ? 0 : current.size();
        int beforeContinuous = V50RuntimeOrchestrator.recentContinuousCount(current);
        if (beforeContinuous >= HistoricalDrawImportEngine.V50_RUNTIME_MINIMUM_ROWS) {
            return current;
        }
        try {
            store.setState("v50_history_bootstrap_status", "RUNNING");
            store.setState("v50_history_bootstrap_source", safe(source));
            store.setState("v50_history_bootstrap_before", String.valueOf(before));
            store.setState(
                    "v50_history_bootstrap_before_continuous", String.valueOf(beforeContinuous));
            HistoricalDrawImportEngine.Report report =
                    new HistoricalDrawImportEngine().importIfNeeded(getApplicationContext(), store);
            List<Models.Draw> reloaded = store.getRecentDraws(limit);
            int after = reloaded == null ? 0 : reloaded.size();
            int afterContinuous = V50RuntimeOrchestrator.recentContinuousCount(reloaded);
            store.setState("v50_history_bootstrap_after", String.valueOf(after));
            store.setState(
                    "v50_history_bootstrap_after_continuous", String.valueOf(afterContinuous));
            store.setState(
                    "v50_history_bootstrap_import", report == null ? "NULL" : report.summary());
            store.setState(
                    "v50_history_bootstrap_status",
                    afterContinuous >= HistoricalDrawImportEngine.V50_RUNTIME_MINIMUM_ROWS
                            ? "READY"
                            : "BLOCKED_HISTORY_LT_96");
            return reloaded;
        } catch (Throwable error) {
            try {
                store.setState(
                        "v50_history_bootstrap_status",
                        "ERROR:" + error.getClass().getSimpleName());
                store.setState("v50_history_bootstrap_error", safeError(error));
            } catch (Throwable ignore) {
            }
            return current;
        }
    }

    private ArrayList<Models.Draw> limitRealtimeHistory(ArrayList<Models.Draw> clean) {
        ArrayList<Models.Draw> out = new ArrayList<Models.Draw>();

        if (clean == null || clean.size() == 0) {
            return out;
        }

        int keep =
                AppConfig.ENABLE_V50_PRIME
                        ? Math.min(2880, AppConfig.WINDOW_3000)
                        : REALTIME_HOT_HISTORY_LIMIT;

        if (keep < AppConfig.MIN_DRAWS_ANALYZE + 2) {
            keep = AppConfig.MIN_DRAWS_ANALYZE + 2;
        }

        if (!AppConfig.ENABLE_V50_PRIME && keep > REALTIME_HOT_HISTORY_LIMIT) {
            keep = REALTIME_HOT_HISTORY_LIMIT;
        }

        int start = clean.size() - keep;

        if (start < 0) {
            start = 0;
        }

        for (int i = start; i < clean.size(); i++) {
            Models.Draw d = clean.get(i);
            if (d != null && d.isValid()) {
                out.add(d);
            }
        }

        return out;
    }

    private void writeRealtimeLayeredState(
            String baseIssue, String targetIssue, int fullCount, int formalCount, String stage) {
        try {
            if (store == null) return;
            int backgroundCount = fullCount - formalCount;
            if (backgroundCount < 0) backgroundCount = 0;
            store.setState("realtime_layered_mode", "FORMAL_40_SNAPSHOT_BACKGROUND_160");
            store.setState("realtime_layered_version", V20_DYNAMIC_CAUSAL_AUTOFETCH_PATCH_VERSION);
            store.setState("realtime_layered_base", safe(baseIssue));
            store.setState("realtime_layered_target", safe(targetIssue));
            store.setState("realtime_layered_stage", safe(stage));
            store.setState("realtime_layered_full_scan_count", String.valueOf(fullCount));
            store.setState("realtime_layered_formal_sync_count", String.valueOf(formalCount));
            store.setState("realtime_layered_background_count", String.valueOf(backgroundCount));
            store.setState(
                    "realtime_layered_background_limit",
                    String.valueOf(V20_BACKGROUND_CANDIDATE_LIMIT));
            store.setState(
                    "realtime_layered_policy",
                    "全量候选不删除；前40同步概率/动态门/实时动态因果收敛小核进DecisionGate；同步重型V20/教师/动态因果长画像后台；全量160后台补全");
            store.setState(
                    "realtime_layered_updated_at", String.valueOf(System.currentTimeMillis()));
        } catch (Throwable e) {
        }
    }

    private void markRealtimeLayeredDecisionSaved(
            String baseIssue, String targetIssue, long costMs) {
        try {
            if (store == null) return;
            store.setState("realtime_layered_decision_saved", "1");
            store.setState("realtime_layered_decision_base", safe(baseIssue));
            store.setState("realtime_layered_decision_target", safe(targetIssue));
            store.setState("realtime_layered_decision_cost_ms", String.valueOf(costMs));
            store.setState("realtime_layered_after_save_background", "REQUESTED");
            store.setState(
                    "realtime_layered_after_save_note",
                    "正式预测已先保存广播；后台继续补全160候选V20因链画像/教师矩阵/动态因果长诊断，不阻塞出号");
            store.setState(
                    "realtime_layered_decision_saved_at",
                    String.valueOf(System.currentTimeMillis()));
        } catch (Throwable e) {
        }
    }

    private void markRealtimeLayeredBackgroundRequested(String baseIssue, String reason) {
        try {
            if (store == null) return;
            String targetIssue = dataGuard == null ? "" : dataGuard.buildNextIssue(baseIssue);
            store.setState("realtime_layered_background_requested", "1");
            store.setState("realtime_layered_background_base", safe(baseIssue));
            store.setState("realtime_layered_background_target", safe(targetIssue));
            store.setState(
                    "realtime_layered_background_limit",
                    String.valueOf(V20_BACKGROUND_CANDIDATE_LIMIT));
            store.setState("realtime_layered_background_reason", safe(reason));
            store.setState(
                    "realtime_layered_background_scope",
                    "补全160候选：V20因链画像、教师矩阵桥接、动态因果样本、SHADOW/学习反哺；不写真账本");
            store.setState(
                    "realtime_layered_background_requested_at",
                    String.valueOf(System.currentTimeMillis()));
        } catch (Throwable e) {
        }
    }

    private boolean shouldDeferRealtimeHeavyStage(
            long totalStart, long limitMs, String stage, String targetIssue) {
        long cost = System.currentTimeMillis() - totalStart;

        /*
         * FIX23_71049382：
         * 实时正式出号链绝不再同步反射跑 V20/Teacher/Counterfactual/RuleHealth 等重链。
         * 这些重链只读上一轮已完成快照或写 WATCH/NEUTRAL 字段，完整刷新交给温层/冷层后台。
         * 目的：把 v20_before_probability 从几十秒压回毫秒级，避免目标期过期。
         */
        if (REALTIME_FORCE_DEFER_REFLECT_HEAVY) {
            try {
                if (store != null) {
                    store.setState("analysis_defer_heavy_stage", safe(stage));
                    store.setState("analysis_defer_heavy_stage_target", safe(targetIssue));
                    store.setState("analysis_defer_heavy_stage_cost_ms", String.valueOf(cost));
                    store.setState("analysis_heavy_realtime_isolated", "1");
                    store.setState(
                            "analysis_heavy_realtime_isolated_version",
                            V20_TIERED_REALTIME_SCHEDULER_VERSION);
                    store.setState(
                            "analysis_defer_heavy_stage_policy",
                            "FIX23分层调度：实时HOT链只做360窗口/候选/概率/动态门/DecisionGate；V20/Teacher/反事实/规则健康/SHADOW晋级不在当期同步重跑，读取快照或中性字段，温层10分钟、冷层5000期1小时刷新。");
                }
            } catch (Throwable e) {
            }
            return true;
        }

        if (limitMs > 0L && cost < limitMs) {
            return false;
        }
        try {
            if (store != null) {
                store.setState("analysis_defer_heavy_stage", safe(stage));
                store.setState("analysis_defer_heavy_stage_target", safe(targetIssue));
                store.setState("analysis_defer_heavy_stage_cost_ms", String.valueOf(cost));
                store.setState(
                        "analysis_defer_heavy_stage_policy", "实时主链先出当前目标期正式Decision；长画像后台分层补完");
                store.setState("analysis_heavy_realtime_isolated", "1");
                store.setState(
                        "analysis_heavy_realtime_isolated_version",
                        V20_TIERED_REALTIME_SCHEDULER_VERSION);
            }
        } catch (Throwable e) {
        }
        return true;
    }

    private ArrayList<Models.ComboCandidate> keepRealtimeStageWithSnapshotBridge(
            ArrayList<Models.ComboCandidate> candidates,
            List<Models.Draw> clean,
            String baseIssue,
            String targetIssue,
            String stage) {
        ArrayList<Models.ComboCandidate> out =
                candidates == null ? new ArrayList<Models.ComboCandidate>() : candidates;
        out =
                ensureRealtimeUpstreamEvidenceFields(
                        out, clean, baseIssue, targetIssue, stage + "_SNAPSHOT_BRIDGE");
        out =
                ensureRealtimeDynamicCausalFields(
                        out, clean, baseIssue, targetIssue, stage + "_SNAPSHOT_BRIDGE");
        applyTieredSnapshotFreshnessToCandidates(out, baseIssue, targetIssue, stage);
        try {
            if (store != null) {
                store.setState("analysis_snapshot_bridge_stage", safe(stage));
                store.setState(
                        "analysis_snapshot_bridge_policy",
                        "实时必跑当前动态因果小核；重型长画像使用后台快照补充进入DecisionGate");
                store.setState("analysis_snapshot_bridge_target", safe(targetIssue));
                store.setState(
                        "analysis_snapshot_bridge_candidate_count", String.valueOf(out.size()));
            }
        } catch (Throwable e) {
        }
        return out;
    }

    private void markRealtimeHeavyDeferred(String stage, String targetIssue) {
        try {
            if (store != null) {
                store.setState("analysis_heavy_deferred_stage", safe(stage));
                store.setState("analysis_heavy_deferred_target", safe(targetIssue));
                store.setState(
                        "analysis_heavy_deferred_at", String.valueOf(System.currentTimeMillis()));
                store.setState(
                        "analysis_heavy_deferred_note",
                        "策略功能不取消；为保证20秒内正式出号，本期实时只读取已完成快照/候选字段，完整刷新在保存预测后后台继续");
            }
        } catch (Throwable e) {
        }
    }

    private Models.Decision buildDecision(List<Models.Draw> history) {
        long totalStart = System.currentTimeMillis();

        ArrayList<Models.Draw> clean = dataGuard.normalizeHistory(history);
        clean = limitRealtimeHistory(clean);

        if (clean.size() == 0) {
            return dataGuard.holdDecision("", "", AppConfig.BLOCK_HISTORY_WEAK, "历史为空，当前目标期正式HOLD");
        }

        Models.Draw latest = clean.get(clean.size() - 1);
        String baseIssue = latest.issue == null ? "" : latest.issue;
        String targetIssue = dataGuard.buildNextIssue(baseIssue);
        analysisRecordedCostSumV8 = 0L;
        analysisRecordedTargetV8 = targetIssue == null ? "" : targetIssue;
        try {
            store.resetRealtimeStageCosts(targetIssue);
            store.setState(
                    "realtime_valid_domain_autofetch_version",
                    REALTIME_VALID_DOMAIN_AUTOFETCH_VERSION);
        } catch (Throwable e) {
        }

        if (V51BuildManifest.ACTIVE_RUNTIME) {
            return buildV51ApexDecision(clean, baseIssue, targetIssue, totalStart);
        }
        if (AppConfig.ENABLE_V50_PRIME) {
            return buildV50PrimeDecision(clean, baseIssue, targetIssue, totalStart);
        }

        teacherBridgeTargetIssueV3 = targetIssue;
        teacherBridgeCompletedV3 = false;
        refreshFrequencyStateForAnalysis(baseIssue, targetIssue);

        long tCheck = System.currentTimeMillis();
        Models.DataCheck check = dataGuard.checkCanAnalyze(clean);
        recordAnalysisCost("check_can_analyze", tCheck);

        if (check == null || !check.ok) {
            return dataGuard.holdDecision(
                    baseIssue,
                    targetIssue,
                    AppConfig.BLOCK_HISTORY_WEAK,
                    check == null ? "历史校验失败，当前目标期正式HOLD" : check.reason);
        }

        Models.DataCheck fullRunCheck = dataGuard.checkCanFullRun(clean);

        if (AppConfig.ENABLE_V49_FULL_MOE) {
            return buildV49FullWindowMoeDecision(
                    clean, baseIssue, targetIssue, fullRunCheck, totalStart);
        }

        boolean shadowOnly = false;

        setAnalysisState("RUNNING", 25, "构建预开奖特征与位置形态向量");

        long tPreDraw = System.currentTimeMillis();
        runV20PreDrawFeatureSafely(clean, baseIssue, targetIssue);
        recordAnalysisCost("v20_predraw_feature", tPreDraw);

        long tWindow = System.currentTimeMillis();
        List<Models.ShapeVector> vectors = windowFeatureEngine.buildAllPositions(clean);
        vectors = shapeStateEngine.refineAll(vectors);
        recordAnalysisCost("window_shape_refine", tWindow);

        setAnalysisState("RUNNING", 36, "形态向量完成，计算Markov和趋势");

        long tMarkov = System.currentTimeMillis();
        vectors = markovEngine.applyToVectors(vectors, clean);
        recordAnalysisCost("markov_apply", tMarkov);

        long tRecentVectors = System.currentTimeMillis();
        List<Models.ShapeVector> recentVectors =
                store.getRecentShapeVectors(AppConfig.POSITION_COUNT * 80);
        recordAnalysisCost("load_recent_shape_vectors", tRecentVectors);

        long tTrend = System.currentTimeMillis();
        ArrayList<TrendStateMachine.TrendTrace> traces =
                trendStateMachine.analyzeAll(recentVectors, vectors);
        vectors = trendStateMachine.refineAllByTrace(recentVectors, vectors);
        recordAnalysisCost("trend_trace_refine", tTrend);

        setAnalysisState("RUNNING", 46, "趋势完成，计算破位恢复");

        long tRecovery = System.currentTimeMillis();
        ArrayList<BreakRecoveryEngine.RecoveryResult> recoveries =
                breakRecoveryEngine.analyzeAll(vectors, traces);
        recordAnalysisCost("break_recovery", tRecovery);

        postSaveShapeVectorsAsync(vectors);

        setAnalysisState("RUNNING", 54, "开始完整扫描正式候选池");

        long tScan = System.currentTimeMillis();
        Models.CandidateBatch batch =
                runV20FullCandidateScanOrFallback(clean, vectors, traces, recoveries);
        recordAnalysisCost("combo_scan_v20", tScan);

        if (batch == null || batch.candidates == null || batch.candidates.size() == 0) {
            return dataGuard.holdDecision(
                    baseIssue, targetIssue, AppConfig.BLOCK_COMBO_WEAK, "正式候选池为空，当前目标期正式HOLD");
        }

        Models.DataCheck candidateListCheck = dataGuard.checkCandidateList(batch.candidates, 1);
        if (candidateListCheck == null || !candidateListCheck.ok) {
            return dataGuard.holdDecision(
                    baseIssue,
                    targetIssue,
                    AppConfig.BLOCK_COMBO_WEAK,
                    candidateListCheck == null ? "候选池校验失败，当前目标期正式HOLD" : candidateListCheck.reason);
        }

        V47CandidateSurfaceContext.begin(
                store, clean, baseIssue, targetIssue, batch.candidates.size());
        V48PipelineLedger.scan(store, batch.candidates.size());

        int probabilityCap = getRealtimeProbabilityEnrichCap();
        int decisionCap = getRealtimeDecisionCandidateCap();

        ArrayList<Models.ComboCandidate> rawForProbability =
                capCandidatesByScore(batch.candidates, probabilityCap);
        V47CandidateSurfaceContext.probability(store, rawForProbability.size());
        rawForProbability =
                ensureRealtimeUpstreamEvidenceFields(
                        rawForProbability, clean, baseIssue, targetIssue, "AFTER_SCAN_BEFORE_V20");

        writeRealtimeLayeredState(
                baseIssue,
                targetIssue,
                batch.candidates.size(),
                rawForProbability.size(),
                "AFTER_SCAN_FORMAL_SHORTLIST");
        setAnalysisState(
                "RUNNING",
                61,
                "候选池="
                        + batch.candidates.size()
                        + "，实时正式门完整分析前"
                        + rawForProbability.size()
                        + "个；其余候选不删除；实时先跑必要小核，重型长画像预测后后台补全V20因链画像、教师矩阵、动态因果和学习反哺");

        long tV20BeforeProbability = System.currentTimeMillis();
        if (!shouldDeferRealtimeHeavyStage(
                totalStart, REALTIME_HEAVY_STAGE_DEFER_MS, "v20_before_probability", targetIssue)) {
            rawForProbability =
                    runV20CandidateCauseEnginesBeforeProbability(
                            rawForProbability,
                            clean,
                            vectors,
                            traces,
                            recoveries,
                            baseIssue,
                            targetIssue);
        } else {
            markRealtimeHeavyDeferred("v20_before_probability", targetIssue);
            rawForProbability =
                    keepRealtimeStageWithSnapshotBridge(
                            rawForProbability,
                            clean,
                            baseIssue,
                            targetIssue,
                            "v20_before_probability");
        }
        recordAnalysisCost("v20_before_probability", tV20BeforeProbability);
        rawForProbability =
                ensureRealtimeUpstreamEvidenceFields(
                        rawForProbability,
                        clean,
                        baseIssue,
                        targetIssue,
                        "AFTER_V20_BEFORE_TEACHER");

        long tHistoricalPrior = System.currentTimeMillis();
        /* V23 realtime: finite-family prior is one fast pass.  The old evidence forest
         * and full ProbabilityEngine remain available to background walk-forward jobs,
         * but no longer block the one-minute realtime path. */
        rawForProbability = historicalStatePriorEngine.apply(rawForProbability, clean, targetIssue);
        recordAnalysisCost("hierarchical_history_prior_fast", tHistoricalPrior);
        try {
            if (store != null) {
                store.setState(
                        "hierarchical_prior_candidate_count",
                        String.valueOf(rawForProbability.size()));
                store.setState(
                        "hierarchical_prior_version", HistoricalStatePriorEngine.MODULE_VERSION);
                store.setState(
                        "legacy_online_forest_realtime",
                        "BACKGROUND_OOS_CHALLENGER_SNAPSHOT_ACTIVE");
            }
        } catch (Throwable ignore) {
        }

        if (!shouldDeferRealtimeHeavyStage(
                totalStart,
                REALTIME_HEAVY_STAGE_DEFER_MS,
                "teacher_before_probability",
                targetIssue)) {
            rawForProbability =
                    runTeacherNumberMatrixFinalBridge(
                            rawForProbability,
                            clean,
                            vectors,
                            traces,
                            recoveries,
                            baseIssue,
                            targetIssue,
                            "BEFORE_V43_FULL_STRATEGY_PROBABILITY");
        } else {
            markRealtimeHeavyDeferred("teacher_before_probability", targetIssue);
            rawForProbability =
                    keepRealtimeStageWithSnapshotBridge(
                            rawForProbability,
                            clean,
                            baseIssue,
                            targetIssue,
                            "teacher_before_probability");
        }
        rawForProbability =
                ensureRealtimeUpstreamEvidenceFields(
                        rawForProbability,
                        clean,
                        baseIssue,
                        targetIssue,
                        "AFTER_TEACHER_BEFORE_V43_FULL_STRATEGY");
        rawForProbability = capCandidatesByScore(rawForProbability, probabilityCap);
        long tV43PreEvidence =
                StrategyExecutionTraceEngine.begin(
                        store, "V43_PRE_PROBABILITY", rawForProbability.size());
        if (unifiedStrategyEvidenceEngine != null) {
            rawForProbability =
                    unifiedStrategyEvidenceEngine.applyPreProbability(
                            store, rawForProbability, clean, baseIssue, targetIssue);
        }
        StrategyExecutionTraceEngine.end(
                store, "V43_PRE_PROBABILITY", rawForProbability.size(), tV43PreEvidence, "OK");

        long tProb = System.currentTimeMillis();
        ArrayList<Models.ComboCandidate> candidates =
                realtimeCausalProbabilityEngine == null
                        ? rawForProbability
                        : realtimeCausalProbabilityEngine.apply(store, rawForProbability, clean);
        candidates =
                mergeUpstreamEvidenceFromPrevious(
                        rawForProbability, candidates, "AFTER_V43_FULL_STRATEGY_PROBABILITY");
        candidates =
                ensureRealtimeUpstreamEvidenceFields(
                        candidates,
                        clean,
                        baseIssue,
                        targetIssue,
                        "AFTER_V43_FULL_STRATEGY_PROBABILITY");
        recordAnalysisCost("v23_causal_probability", tProb);

        /* V43 has one active realtime probability owner. Legacy ProbabilityEngine,
         * Calibration, OnlineEvidenceForest and AdaptiveExpertConsensus remain complete
         * background/OOS challengers; their verified snapshots contribute evidence, but
         * they may not re-compress or overwrite this target issue's locked realtime P. */
        long tCalib = System.currentTimeMillis();
        recordAnalysisCost("legacy_probability_and_calibration_skipped", tCalib);
        try {
            if (store != null) {
                store.setState(
                        "active_realtime_probability_owner",
                        RealtimeCausalProbabilityEngine.MODULE_VERSION);
                store.setState(
                        "legacy_probability_realtime_status", "BACKGROUND_OOS_CHALLENGER_ACTIVE");
                store.setState(
                        "adaptive_consensus_realtime_status", "BACKGROUND_OOS_CHALLENGER_ACTIVE");
            }
        } catch (Throwable ignore) {
        }
        candidates = capCandidatesByScore(candidates, probabilityCap);

        long tGate = System.currentTimeMillis();
        candidates =
                dynamicGateEngine.applyToCandidates(store, candidates, traces, recoveries, clean);
        candidates = hierarchicalStateTaxonomyEngine.freezeAll(candidates, clean, targetIssue);
        candidates =
                ensureRealtimeUpstreamEvidenceFields(
                        candidates, clean, baseIssue, targetIssue, "AFTER_DYNAMIC_GATE");
        recordAnalysisCost("dynamic_gate", tGate);

        candidates = capCandidatesByScore(candidates, REALTIME_POST_GATE_V20_CANDIDATE_CAP);
        setAnalysisState(
                "RUNNING",
                74,
                "动态门完成，后置V20风险/证据只对最终门候选深算，候选=" + candidates.size() + "；不快裁，不绕过DecisionGate");
        long tV20AfterGate = System.currentTimeMillis();
        if (!shouldDeferRealtimeHeavyStage(
                totalStart, REALTIME_FINAL_GATE_PROTECT_MS, "v20_after_gate", targetIssue)) {
            candidates =
                    runV20CandidateCauseEnginesAfterGate(
                            candidates, clean, vectors, traces, recoveries, baseIssue, targetIssue);
        } else {
            markRealtimeHeavyDeferred("v20_after_gate", targetIssue);
            candidates =
                    keepRealtimeStageWithSnapshotBridge(
                            candidates, clean, baseIssue, targetIssue, "v20_after_gate");
        }
        recordAnalysisCost("v20_after_gate", tV20AfterGate);
        candidates =
                ensureRealtimeUpstreamEvidenceFields(
                        candidates, clean, baseIssue, targetIssue, "AFTER_V20_AFTER_GATE");

        /* V46: Teacher snapshot is captured once before probability.  Re-running or
         * re-bridging it after probability would create a second evidence owner and
         * could reorder/overwrite the live contract. */
        try {
            if (store != null) {
                store.setState("v46_teacher_after_gate", "SKIPPED_SINGLE_SNAPSHOT_OWNER");
                store.setState(
                        "v46_teacher_snapshot_policy",
                        "同一targetIssue只允许一次Teacher快照；后续只读，不重复桥接、不改EventP");
            }
        } catch (Throwable ignore) {
        }

        candidates = capCandidatesByScore(candidates, decisionCap);

        /* V46: no third Teacher pass before DecisionGate. */
        try {
            if (store != null)
                store.setState("v46_teacher_before_decision", "SKIPPED_SINGLE_SNAPSHOT_OWNER");
        } catch (Throwable ignore) {
        }
        candidates =
                ensureRealtimeUpstreamEvidenceFields(
                        candidates, clean, baseIssue, targetIssue, "BEFORE_DECISION_GATE");

        candidates = capCandidatesByScore(candidates, decisionCap);

        setAnalysisState("RUNNING", 77, "接入实时动态因果收敛小核：当前窗口→NeedSupply→反事实→Adaptive轻审判；长画像后台补全");
        long tDynamicCausalRealtime = System.currentTimeMillis();
        candidates =
                runDynamicCausalWindowRealtimeEngines(candidates, clean, baseIssue, targetIssue);
        recordAnalysisCost("dynamic_causal_realtime", tDynamicCausalRealtime);
        candidates =
                ensureRealtimeDynamicCausalFields(
                        candidates,
                        clean,
                        baseIssue,
                        targetIssue,
                        "BEFORE_DECISION_GATE_DYNAMIC_CAUSAL");
        candidates =
                applyRealtimeDynamicMiniCore(candidates, clean, baseIssue, targetIssue, totalStart);
        if (shadowPromotionVerifierEngine != null) {
            candidates = shadowPromotionVerifierEngine.applyRealtime(store, candidates);
            try {
                store.setState(
                        AppConfig.STATE_PROMOTION_MATCH_COUNT,
                        String.valueOf(candidates == null ? 0 : candidates.size()));
            } catch (Throwable ignore) {
            }
        }
        /* 已结算同域证据回写候选，只改变证据字段，不直接BET。 */
        try {
            if (dynamicCauseChainEngine instanceof DynamicCauseChainEngine) {
                candidates =
                        ((DynamicCauseChainEngine) dynamicCauseChainEngine)
                                .applyForwardDomainEvidence(store, candidates);
            }
        } catch (Throwable e) {
            try {
                store.setState("dynamic_domain_realtime_error", safeError(e));
            } catch (Throwable ignore) {
            }
        }
        runAdaptiveJudgeRealtime(candidates, baseIssue, targetIssue);
        candidates = capCandidatesByScore(candidates, decisionCap);
        candidates =
                ensureSafePreDrawPromotionDomains(
                        candidates, baseIssue, targetIssue, "BEFORE_FINAL_BATCH");
        long tV43PreDecision =
                StrategyExecutionTraceEngine.begin(
                        store, "V47_FINAL_EVIDENCE", candidates == null ? 0 : candidates.size());
        if (unifiedStrategyEvidenceEngine != null) {
            candidates =
                    unifiedStrategyEvidenceEngine.applyPreDecision(
                            store, candidates, clean, baseIssue, targetIssue);
        }
        StrategyExecutionTraceEngine.end(
                store,
                "V47_FINAL_EVIDENCE",
                candidates == null ? 0 : candidates.size(),
                tV43PreDecision,
                "OK");

        /* V48 diagnostic Rank1 is selected from the complete 60-candidate model surface,
         * before diversity retention to formal40. It never creates a Trade. */
        String v48IdentityAtObservation =
                store == null ? "" : store.getState(BuildIdentity.STATE_STATUS, "");
        DiagnosticObservationEngine.persist(store, candidates, v48IdentityAtObservation);
        ArrayList<Models.ComboCandidate> b07PreFinalOosSurface =
                candidates == null
                        ? new ArrayList<Models.ComboCandidate>()
                        : new ArrayList<Models.ComboCandidate>(candidates);
        /* V48唯一最终排序：所有概率和风险证据完成后只排一次。 */
        long tV46FinalRank = System.currentTimeMillis();
        candidates = Final40ContractAuditor.rank(store, candidates, targetIssue);
        recordAnalysisCost("v48_final_rank_40_from_60", tV46FinalRank);
        int v48OosInserted =
                CandidateDecisionFunnelLogEngine.persist(
                        store, candidates, b07PreFinalOosSurface, targetIssue);
        RootCauseAttributionEngine.Result v46Root =
                RootCauseAttributionEngine.diagnose(
                        candidates,
                        System.currentTimeMillis() - totalStart,
                        store == null ? "" : store.getState(BuildIdentity.STATE_STATUS, ""));
        if (candidates != null) {
            for (int i = 0; i < candidates.size(); i++) {
                RootCauseAttributionEngine.apply(candidates.get(i), v46Root);
            }
        }
        try {
            if (store != null) {
                store.setState("v46_root_cause_version", RootCauseAttributionEngine.MODULE_VERSION);
                store.setState("v46_root_cause_last", v46Root == null ? "" : v46Root.summary());
                store.setState(
                        "v47_live_chain",
                        "FULL_SCAN1200_LIGHT->CROSSFIT_NUMBER_MODEL_100_STATE_SHARED->PROBABILITY60->VERSION_SCOPED_CAL->FINAL_EVIDENCE60->FINAL_RANK40->UI12->FRESH_SINGLE_LANE->SIGN_ONLY_AUTH");
                store.setState("v47_legacy_real_gate", "DISABLED");
            }
        } catch (Throwable ignore) {
        }

        V47CandidateSurfaceContext.ui(
                store,
                Math.min(
                        AppConfig.V48_UI_DISPLAY_CANDIDATE_COUNT,
                        candidates == null ? 0 : candidates.size()));
        saveCandidateRuntimeSummary(candidates);

        Models.CandidateBatch finalBatch = new Models.CandidateBatch();
        finalBatch.baseIssue = baseIssue;
        finalBatch.targetIssue = targetIssue;
        finalBatch.scanMode = batch.scanMode;
        finalBatch.fullScan = batch.fullScan;
        finalBatch.scannedCount = batch.scannedCount;
        finalBatch.keptCount = batch.candidates == null ? 0 : batch.candidates.size();
        finalBatch.probabilityMode = AppConfig.BACKTEST_MODE_NORMAL;
        finalBatch.enrichCount = candidates == null ? 0 : candidates.size();
        finalBatch.deepCount = candidates == null ? 0 : candidates.size();

        if (candidates != null) {
            for (int i = 0; i < candidates.size(); i++) {
                Models.ComboCandidate c = candidates.get(i);
                if (c == null) {
                    continue;
                }

                c.baseIssue = baseIssue;
                c.targetIssue = targetIssue;
                finalBatch.add(c);
            }

            if (candidates.size() > 0) {
                finalBatch.best = candidates.get(0);
            }
        }

        if (finalBatch.best == null && rawForProbability.size() > 0) {
            finalBatch.best = rawForProbability.get(0);
        }

        if (shadowOnly) {
            Models.ComboCandidate bestShadow = finalBatch.best;
            if (bestShadow != null) {
                Models.Decision shadowDecision =
                        dataGuard.shadowDecisionFromCandidate(
                                bestShadow,
                                "历史阶段="
                                        + dataGuard.getRunStage(clean)
                                        + "，只做SHADOW学习，不真实出手；后台矩阵继续积累样本");
                shadowDecision.baseIssue = baseIssue;
                shadowDecision.targetIssue = targetIssue;
                shadowDecision.learningSummary = "SHADOW阶段：保存候选用于开奖后学习，但不计真实BET胜率";
                recordAnalysisCost("build_decision_total_shadow", totalStart);
                return shadowDecision;
            }

            recordAnalysisCost("build_decision_total_shadow_hold", totalStart);
            return dataGuard.holdDecision(
                    baseIssue,
                    targetIssue,
                    AppConfig.BLOCK_HISTORY_WEAK,
                    "历史处于SHADOW学习阶段，但没有有效SHADOW候选，当前目标期正式HOLD");
        }

        String preDecisionIdentity = refreshBuildIdentityAndModuleState("before_decision_gate");
        try {
            if (store != null) store.setState("pre_decision_build_identity", preDecisionIdentity);
        } catch (Throwable ignore) {
        }
        setAnalysisState(
                "RUNNING",
                80,
                "V46唯一Lane开始裁决，最终正式候选数="
                        + finalBatch.candidates.size()
                        + "，当前窗口/NeedSupply/反事实/Adaptive轻审判已完成，旧真实回退已禁用，仅V46合同裁决");

        long tDecisionGate = System.currentTimeMillis();
        Models.Decision decision =
                decisionGateEngine.decide(store, finalBatch, clean, traces, recoveries);
        recordAnalysisCost("decision_gate", tDecisionGate);

        long tPostDecisionSync = System.currentTimeMillis();
        decision =
                syncDecisionClosedLoopFieldsFromCandidates(
                        decision, candidates, "AFTER_DECISION_GATE");
        decision = enforceAutoFetchFinalBetSafety(decision, "AFTER_DECISION_GATE");
        recordAnalysisCost("post_decision_sync", tPostDecisionSync);

        if (decision == null) {
            decision =
                    dataGuard.holdDecision(
                            baseIssue,
                            targetIssue,
                            AppConfig.BLOCK_LOW_PROBABILITY,
                            "最终决策门未生成BET/SHADOW，当前目标期正式HOLD");
        }

        if (fullRunCheck != null && AppConfig.REASON_WARN.equals(fullRunCheck.code)) {
            decision.reasonSummary = appendReason(decision.reasonSummary, fullRunCheck.message);
        }

        decision.baseIssue = baseIssue;
        decision.targetIssue = targetIssue;

        decision =
                recoverCandidateBackedShadowIfSoftHold(
                        decision,
                        candidates,
                        clean,
                        baseIssue,
                        targetIssue,
                        "AFTER_TARGET_NORMALIZE");
        decision =
                syncDecisionClosedLoopFieldsFromCandidates(
                        decision, candidates, "AFTER_TARGET_NORMALIZE");
        decision = enforceAutoFetchFinalBetSafety(decision, "AFTER_TARGET_NORMALIZE");

        if (decision.learningSummary == null || decision.learningSummary.length() == 0) {
            decision.learningSummary =
                    "V46唯一实时链：30期受控启动→10位置×10号码归一化分布→K3"
                        + " EventP→全局/位置/排名/Regime条件校准→最终证据只生成Uncertainty与ActionLCB→最终重排40个正式候选（UI只显示12个）→单次Lane合同→Authorization只签名；旧DecisionGate真实回退与重复Teacher桥接已退出实时链";
        }

        if (lastTeacherMatrixSummary != null && lastTeacherMatrixSummary.length() > 0) {
            decision.learningSummary =
                    appendReason(decision.learningSummary, "教师矩阵后台摘要：" + lastTeacherMatrixSummary);
        }

        if (lastV20CauseChainSummary != null && lastV20CauseChainSummary.length() > 0) {
            decision.learningSummary =
                    appendReason(decision.learningSummary, "V20因链后台摘要：" + lastV20CauseChainSummary);
        }

        decision.selfDiagnosisText =
                appendReason(
                        decision.selfDiagnosisText,
                        "V22调度自检：AutoFetchService只调度、保存和广播，不救援BET；实时保留60概率候选/40正式候选，历史先验/森林只算一次，Teacher无样本与Dynamic"
                            + " WATCH严格中性；统一专家共识根据已结算REAL/SHADOW自动降权或反向使用负相关旧专家；频率债务不改概率；DecisionGate仍是唯一BET出口；BET保存后立即确认Trade；后台任务让路；结算结果保护开启");

        if (isWeakPlaceholderDecision(decision)) {
            decision =
                    dataGuard.holdDecision(
                            baseIssue,
                            targetIssue,
                            AppConfig.BLOCK_LOW_PROBABILITY,
                            "最终结果被识别为弱占位/快速观察，已转为正式HOLD");
        }

        decision = enforceAutoFetchFinalBetSafety(decision, "BEFORE_BUILD_DECISION_RETURN");
        decision = PipelineInvariantGuard.enforceDecision(decision);
        ensureSafePreDrawPromotionDomainOnObject(decision);
        long tShadowPersist = System.currentTimeMillis();
        persistSelectedShadowCandidateForSettlement(decision, candidates, baseIssue, targetIssue);
        recordAnalysisCost("shadow_candidate_persist", tShadowPersist);

        V48PipelineLedger.finalizeIssue(store);
        recordAnalysisCost("build_decision_total", totalStart);
        return decision;
    }

    /** V49 fast path: 100 number events -> ten position Top3 actions -> one DecisionGate. */
    private Models.Decision buildV49FullWindowMoeDecision(
            ArrayList<Models.Draw> clean,
            String baseIssue,
            String targetIssue,
            Models.DataCheck fullRunCheck,
            long totalStart) {
        setAnalysisState("RUNNING", 32, "V49构建100个位置号码条件概率与窗口生命周期");
        long tPipeline = System.currentTimeMillis();
        V49RealtimePipelineEngine.Result result =
                new V49RealtimePipelineEngine().run(store, clean, targetIssue);
        recordAnalysisCost("v49_100_number_moe_top3", tPipeline);
        if (result == null || result.candidates == null || result.candidates.size() != 10) {
            String status = result == null ? "NULL" : result.status;
            return dataGuard.holdDecision(
                    baseIssue, targetIssue, AppConfig.BLOCK_COMBO_WEAK, "V49十位置动作构建失败：" + status);
        }

        Models.CandidateBatch batch = new Models.CandidateBatch();
        batch.baseIssue = baseIssue;
        batch.targetIssue = targetIssue;
        batch.scanMode = "V49_100_NUMBER_TO_10_TOP3";
        batch.probabilityMode = "V49_MOE_DIRICHLET";
        batch.fullScan = 1;
        batch.scannedCount = AppConfig.V49_NUMBER_EVENT_COUNT;
        batch.keptCount = result.candidates.size();
        batch.enrichCount = result.candidates.size();
        batch.deepCount = result.candidates.size();
        batch.addAll(result.candidates);
        batch.best = result.selected;

        try {
            store.setState(
                    "v49_live_contract", "100_NUMBER_EVENTS->10_POSITION_TOP3->1_DECISION_GATE");
            store.setState(
                    "v49_realtime_candidate_count", String.valueOf(result.candidates.size()));
            store.setState("v49_realtime_result", result.status);
            store.setState("v49_realtime_cost_ms", String.valueOf(result.totalCostMs));
            store.setState("v49_old_1200_surface", "BACKGROUND_ONLY");
            store.setState("v49_probability_owner", V49RealtimePipelineEngine.MODULE_VERSION);
            store.setState("v49_lane_owner", V49BuildManifest.LANE_SCHEMA);
            store.setState("v49_bet_owner", "DecisionGateEngine");
        } catch (Throwable ignore) {
        }

        String identity = refreshBuildIdentityAndModuleState("v49_before_decision_gate");
        try {
            store.setState("pre_decision_build_identity", identity);
        } catch (Throwable ignore) {
        }
        setAnalysisState("RUNNING", 80, "V49十位置动作完成，DecisionGateEngine唯一真实出口开始裁决");
        long tGate = System.currentTimeMillis();
        Models.Decision decision =
                decisionGateEngine.decide(
                        store,
                        batch,
                        clean,
                        new ArrayList<TrendStateMachine.TrendTrace>(),
                        new ArrayList<BreakRecoveryEngine.RecoveryResult>());
        recordAnalysisCost("v49_decision_gate", tGate);
        if (decision == null) {
            decision =
                    dataGuard.holdDecision(
                            baseIssue,
                            targetIssue,
                            AppConfig.BLOCK_LOW_PROBABILITY,
                            "V49 DecisionGate未生成结果");
        }
        decision.baseIssue = baseIssue;
        decision.targetIssue = targetIssue;
        if (fullRunCheck != null && AppConfig.REASON_WARN.equals(fullRunCheck.code)) {
            decision.reasonSummary = appendReason(decision.reasonSummary, fullRunCheck.message);
        }
        decision.learningSummary =
                "V49全信息学习：每期开奖后更新10位置、100号码、九专家可靠性、Teacher分层后验、Dirichlet校准和为什么中/不中；真实盈亏仍只统计DecisionGate签名BET";
        decision.selfDiagnosisText =
                appendReason(
                        decision.selfDiagnosisText,
                        "V49字段Owner：WindowState=V49WindowStateEngine；NumberP=V49RealtimePipelineEngine；Lane=LanePolicyCompiler；Authorization=DecisionAuthorizationEngine；BET=DecisionGateEngine");
        decision =
                syncDecisionClosedLoopFieldsFromCandidates(
                        decision, result.candidates, "V49_AFTER_DECISION_GATE");
        decision = enforceAutoFetchFinalBetSafety(decision, "V49_BEFORE_RETURN");
        decision = PipelineInvariantGuard.enforceDecision(decision);
        ensureSafePreDrawPromotionDomainOnObject(decision);
        persistSelectedShadowCandidateForSettlement(
                decision, result.candidates, baseIssue, targetIssue);
        recordAnalysisCost("build_decision_total_v49", totalStart);
        return decision;
    }

    /**
     * Pure V50 route. No legacy probability, Teacher bridge, V20 rescue, frequency debt, invariant
     * rewriter or post-gate field copier is reachable from this method.
     */
    /**
     * Sole active V51 route. It never invokes V50/V49/V47 prediction, rescue, frequency debt or
     * post-gate mutation. Unproven artifacts produce SHADOW.
     */
    private Models.Decision buildV51ApexDecision(
            ArrayList<Models.Draw> clean, String baseIssue, String targetIssue, long totalStart) {
        Models.Decision decision = CanonicalDecisionViewAdapterR36.read(store, targetIssue);
        if (decision == null)
            decision =
                    dataGuard.holdDecision(
                            baseIssue,
                            targetIssue,
                            AppConfig.BLOCK_DATA_ERROR,
                            "R3.6 active compute仅允许由durable request/single-flight入口启动");
        recordAnalysisCost("build_decision_total_v51", totalStart);
        return decision;
    }

    private Models.Decision buildV50PrimeDecision(
            ArrayList<Models.Draw> clean, String baseIssue, String targetIssue, long totalStart) {
        setAnalysisState("RUNNING", 24, "V50 PRIME：时点特征→状态轨迹→九专家→10位置动作→排序→选择后校准");
        long started = System.currentTimeMillis();
        V50Models.RuntimeResult runtime =
                v50RuntimeOrchestrator == null
                        ? null
                        : v50RuntimeOrchestrator.run(store, clean, analysisGeneration);
        recordAnalysisCost("v50_typed_runtime", started);
        Models.Decision decision = decisionGateEngine.toLegacyDecisionV50(runtime);
        if (decision.baseIssue == null || decision.baseIssue.length() == 0) {
            decision.baseIssue = baseIssue;
        }
        if (decision.targetIssue == null || decision.targetIssue.length() == 0) {
            decision.targetIssue = targetIssue;
        }
        if (runtime == null || !runtime.success) {
            decision.decision = AppConfig.DECISION_HOLD;
            decision.action = AppConfig.ACTION_HOLD;
            decision.statScope = AppConfig.STAT_SCOPE_HOLD;
            decision.isRealtimeReal = 0;
            decision.authorizationValid = 0;
            decision.buildId = V50BuildManifest.BUILD_ID;
            decision.cohortId = V50BuildManifest.COHORT_ID;
            decision.schemaVersion = V50BuildManifest.SCHEMA_ID;
            decision.featureSchemaVersion = V50BuildManifest.FEATURE_SCHEMA;
            decision.rankingVersion = V50ActionRankerArtifact.ARTIFACT_ID;
            decision.calibrationVersion = V50SelectedCalibrationArtifact.ARTIFACT_ID;
            decision.gatePolicyVersion = V50SelectivePolicyArtifact.ARTIFACT_ID;
            decision.pipelineVersion = V50BuildManifest.STRATEGY_VERSION;
            decision.blockCode = runtime == null ? "V50_RUNTIME_NULL" : runtime.status;
            decision.finalDecisionRejectCode = decision.blockCode;
            decision.reasonSummary = "V50保护性HOLD：" + decision.blockCode;
            decision.reasonText = decision.reasonSummary;
        }
        try {
            store.setState("active_runtime", "V50_ACTIVE_RUNTIME");
            store.setState("legacy_runtime", "LEGACY_READ_ONLY_OBSERVER");
            store.setState("v50_runtime_status", runtime == null ? "NULL" : runtime.status);
            store.setState("v50_runtime_checksum", runtime == null ? "" : runtime.checksum);
            store.setState(
                    "v50_runtime_cost_ms",
                    runtime == null ? "0" : String.valueOf(runtime.totalCostMs));
            store.setState("v50_release_state", V50BuildManifest.RELEASE_STATE);
            store.setState(
                    "v50_model_gate", V50BuildManifest.MODEL_PROVEN ? "PASSED" : "NOT_PROVEN");
        } catch (Throwable ignore) {
        }
        recordAnalysisCost("build_decision_total_v50", totalStart);
        return decision;
    }

    private ArrayList<Models.ComboCandidate> ensureSafePreDrawPromotionDomains(
            ArrayList<Models.ComboCandidate> candidates,
            String baseIssue,
            String targetIssue,
            String stage) {
        ArrayList<Models.ComboCandidate> out =
                candidates == null ? new ArrayList<Models.ComboCandidate>() : candidates;
        int valid = 0;
        for (int i = 0; i < out.size(); i++) {
            Models.ComboCandidate c = out.get(i);
            if (c == null || !c.isValidK3()) continue;
            c.baseIssue = baseIssue == null ? "" : baseIssue;
            c.targetIssue = targetIssue == null ? "" : targetIssue;
            String domain = ensureSafePreDrawPromotionDomainOnObject(c);
            if (domain.length() > 0) valid++;
        }
        try {
            if (store != null) {
                store.setState("safe_promotion_domain_stage", stage == null ? "" : stage);
                store.setState("safe_promotion_domain_total", String.valueOf(out.size()));
                store.setState("safe_promotion_domain_valid", String.valueOf(valid));
                store.setState(
                        "safe_promotion_domain_version",
                        "V47_CROSSFIT_NUMBER_CHAMPION_FINAL40_FULL_DIAGNOSIS_20260721_47072147");
            }
        } catch (Throwable ignore) {
        }
        return out;
    }

    private String ensureSafePreDrawPromotionDomainOnObject(Object obj) {
        if (obj == null) return "";
        String domain = "";
        try {
            if (store != null) domain = store.ensureSafePreDrawPromotionDomain(obj);
        } catch (Throwable e) {
            domain = "";
        }
        if (domain.length() > 0) return domain;
        int position = readInt(obj, "position", 0);
        if (!AppConfig.isValidPosition(position)) return "";
        double p = maxDoubleFields(obj, new String[] {"finalP", "calibratedP", "rawP", "modelP"});
        if (p <= 0.0d) p = AppConfig.RANDOM_K3_BASELINE;
        double gate =
                maxDoubleFields(obj, new String[] {"riskAdjustedGate", "dynamicGate", "gate"});
        if (gate <= 0.0d) gate = AppConfig.DEFAULT_DYNAMIC_GATE;
        double gap = gate - p;
        double cover =
                maxDoubleFields(
                        obj,
                        new String[] {
                            "coverageQuality",
                            "coverageQualityScore",
                            "finalDecisionCoverageQuality",
                            "needSupplyMatchScore"
                        });
        double third =
                maxDoubleFields(
                        obj,
                        new String[] {
                            "thirdQuality",
                            "thirdNumberQualityScore",
                            "finalDecisionThirdNumberQuality",
                            "needSupplyThirdQuality"
                        });
        double counter =
                maxDoubleFields(
                        obj,
                        new String[] {
                            "counterRisk",
                            "counterEvidenceScore",
                            "counterfactualRisk",
                            "counterfactualWhyNotRisk"
                        });
        double need =
                maxDoubleFields(
                        obj,
                        new String[] {
                            "needSupplyScore",
                            "positionNeedScore",
                            "numberSupplyScore",
                            "needSupplyMatchScore"
                        });
        String stateKey =
                readFirstString(
                        obj, new String[] {"windowStateKey", "dynamicWindowStateKey", "stateKey"});
        if (stateKey.length() == 0) stateKey = "STATE_ANY";
        String state = "S" + Math.abs(stateKey.hashCode() % 97);
        String pBand = p >= 0.38d ? "P38P" : (p >= 0.36d ? "P36_38" : "P_LT36");
        String gapBand = gap <= 0.010d ? "GAP01" : (gap <= 0.020d ? "GAP02" : "GAP_GT02");
        String qualityBand =
                cover >= AppConfig.REAL_BET_PROBE_MIN_COVERAGE
                                && third >= AppConfig.REAL_BET_PROBE_MIN_THIRD
                        ? "Q_OK"
                        : "Q_WEAK";
        String riskBand = counter <= AppConfig.REAL_BET_PROBE_MAX_COUNTER_RISK ? "R_OK" : "R_HIGH";
        String needBand = need >= AppConfig.REAL_BET_PROBE_MIN_NEED_SUPPLY ? "N_OK" : "N_WEAK";
        String adaptive = readString(obj, "adaptiveStatus", "").toUpperCase(java.util.Locale.ROOT);
        String adaptiveBand =
                adaptive.indexOf("FREEZE") >= 0 || adaptive.indexOf("REJECT") >= 0
                        ? "A_RISK"
                        : "A_OK";
        String rule = readString(obj, "ruleHealthStatus", "").toUpperCase(java.util.Locale.ROOT);
        String ruleBand =
                rule.indexOf("HARD_RISK") >= 0 || rule.indexOf("FREEZE") >= 0 ? "RH_RISK" : "RH_OK";
        domain =
                AppConfig.sanitizePromotionDomainKey(
                        "SD_P"
                                + position
                                + "_"
                                + state
                                + "_"
                                + pBand
                                + "_"
                                + gapBand
                                + "_"
                                + qualityBand
                                + "_"
                                + riskBand
                                + "_"
                                + needBand
                                + "_"
                                + adaptiveBand
                                + "_"
                                + ruleBand);
        writeString(obj, "shadowPromotionDomain", domain);
        writeString(obj, "shadowPromotionDomainKey", domain);
        writeInt(obj, "promotionDomainAvailable", domain.length() > 0 ? 1 : 0);
        return domain;
    }

    private void persistSelectedShadowCandidateForSettlement(
            Models.Decision decision,
            ArrayList<Models.ComboCandidate> candidates,
            String baseIssue,
            String targetIssue) {
        if (store == null
                || decision == null
                || !AppConfig.DECISION_SHADOW.equals(decision.decision)) return;
        Models.ComboCandidate selected = findMatchingClosedLoopCandidate(decision, candidates);
        if (selected == null || !selected.isValidK3()) return;
        ensureSafePreDrawPromotionDomainOnObject(selected);
        ArrayList<Models.ComboCandidate> one = new ArrayList<Models.ComboCandidate>();
        one.add(selected);
        try {
            store.saveShadowCandidates(baseIssue, targetIssue, one, 1);
            store.setState("selected_shadow_saved", "1");
            store.setState("selected_shadow_saved_target", targetIssue == null ? "" : targetIssue);
            store.setState(
                    "selected_shadow_saved_domain",
                    readString(selected, "shadowPromotionDomain", ""));
        } catch (Throwable e) {
            try {
                store.setState("selected_shadow_saved", "0");
                store.setState("selected_shadow_save_error", safeError(e));
            } catch (Throwable ignore) {
            }
        }
    }

    private ArrayList<Models.ComboCandidate> ensureRealtimeUpstreamEvidenceFields(
            ArrayList<Models.ComboCandidate> candidates,
            List<Models.Draw> clean,
            String baseIssue,
            String targetIssue,
            String stage) {

        ArrayList<Models.ComboCandidate> out =
                candidates == null ? new ArrayList<Models.ComboCandidate>() : candidates;
        int total = 0;
        int v20Filled = 0;
        int windowFilled = 0;
        int coverageFilled = 0;
        int thirdFilled = 0;
        int teacherFilled = 0;
        int teacherWeak = 0;

        for (int i = 0; i < out.size(); i++) {
            Models.ComboCandidate c = out.get(i);
            if (c == null) {
                continue;
            }
            total++;
            if (baseIssue != null && baseIssue.length() > 0) {
                c.baseIssue = baseIssue;
            }
            if (targetIssue != null && targetIssue.length() > 0) {
                c.targetIssue = targetIssue;
            }
            normalizeCandidateForMainlineSort(c);
            syncTeacherAliasFields(c);
            syncClosedLoopAliases(c);
            if (ensureV20WindowFields(c, clean, stage)) {
                v20Filled++;
            }
            if (readFirstString(
                                    c,
                                    new String[] {
                                        "windowStateKey",
                                        "finalDecisionWindowStateKey",
                                        "dynamicWindowStateKey",
                                        "probabilityWindowStateKey"
                                    })
                            .length()
                    > 0) {
                windowFilled++;
            }
            if (ensureCoverageQualityFields(c, clean, stage)) {
                coverageFilled++;
            }
            if (maxDoubleFields(
                            c,
                            new String[] {
                                "thirdNumberQualityScore",
                                "dynamicGateThirdNumberQuality",
                                "finalDecisionThirdNumberQuality"
                            })
                    > 0.0d) {
                thirdFilled++;
            }
            if (ensureTeacherPolicyFields(c, stage)) {
                teacherFilled++;
            }
            if (maxIntFields(
                            c,
                            new String[] {
                                "teacherWeakNoBetFlag",
                                "teacherMatrixWeakNoBetFlag",
                                "teacherMatrixOrdinaryBetBlockFlag"
                            })
                    > 0) {
                teacherWeak++;
            }
            syncClosedLoopAliases(c);
        }

        if (store != null) {
            try {
                store.setState(
                        "upstream_evidence_patch_version", V20_UPSTREAM_EVIDENCE_PATCH_VERSION);
                store.setState("upstream_evidence_stage", stage == null ? "" : stage);
                store.setState("upstream_evidence_total", String.valueOf(total));
                store.setState("upstream_evidence_v20_filled", String.valueOf(v20Filled));
                store.setState("upstream_evidence_window_filled", String.valueOf(windowFilled));
                store.setState("upstream_evidence_coverage_filled", String.valueOf(coverageFilled));
                store.setState("upstream_evidence_third_filled", String.valueOf(thirdFilled));
                store.setState("upstream_evidence_teacher_filled", String.valueOf(teacherFilled));
                store.setState("upstream_evidence_teacher_weak", String.valueOf(teacherWeak));
                if (out.size() > 0) {
                    store.setState(
                            "upstream_evidence_best_summary",
                            buildClosedLoopFieldSummary(out.get(0)));
                }
            } catch (Throwable e) {
            }
        }

        return out;
    }

    private ArrayList<Models.ComboCandidate> mergeUpstreamEvidenceFromPrevious(
            List<Models.ComboCandidate> previous,
            List<Models.ComboCandidate> current,
            String stage) {

        ArrayList<Models.ComboCandidate> out = new ArrayList<Models.ComboCandidate>();
        if (current == null || current.size() == 0) {
            return out;
        }
        for (int i = 0; i < current.size(); i++) {
            Models.ComboCandidate c = current.get(i);
            if (c == null) {
                continue;
            }
            Models.ComboCandidate old = findMatchingCandidateInList(c, previous);
            if (old != null) {
                copyMissingClosedLoopFields(old, c);
            }
            out.add(c);
        }
        if (store != null) {
            try {
                store.setState("upstream_evidence_merge_stage", stage == null ? "" : stage);
                store.setState("upstream_evidence_merge_count", String.valueOf(out.size()));
            } catch (Throwable e) {
            }
        }
        return out;
    }

    private Models.ComboCandidate findMatchingCandidateInList(
            Models.ComboCandidate target, List<Models.ComboCandidate> list) {
        if (target == null || list == null || list.size() == 0) {
            return null;
        }
        for (int i = 0; i < list.size(); i++) {
            Models.ComboCandidate c = list.get(i);
            if (c == null) continue;
            if (sameText(c.targetIssue, target.targetIssue)
                    && c.position == target.position
                    && sameK3Numbers(c, target)) {
                return c;
            }
            if (c.position == target.position && sameK3Numbers(c, target)) {
                return c;
            }
        }
        return null;
    }

    private void copyMissingClosedLoopFields(Object from, Object to) {
        if (from == null || to == null) {
            return;
        }
        String[] names = closedLoopFieldNames();
        for (int i = 0; i < names.length; i++) {
            String name = names[i];
            String sv = readString(to, name, "");
            double dv = readDouble(to, name, 0.0d);
            int iv = readInt(to, name, 0);
            if ((sv == null || sv.length() == 0) && dv == 0.0d && iv == 0) {
                copyFieldByReflection(from, to, name);
            }
        }
        syncClosedLoopAliases(to);
    }

    private boolean ensureV20WindowFields(
            Models.ComboCandidate c, List<Models.Draw> clean, String stage) {
        if (c == null) {
            return false;
        }
        boolean wrote = false;
        String v20 =
                readFirstString(
                        c, new String[] {"v20Status", "dynamicStatus", "dynamicCauseStatus"});
        String window =
                readFirstString(
                        c,
                        new String[] {
                            "windowStateKey",
                            "finalDecisionWindowStateKey",
                            "dynamicWindowStateKey",
                            "probabilityWindowStateKey",
                            "v20WindowStateKey"
                        });
        if (window == null || window.length() == 0) {
            window = buildFallbackWindowStateKey(c, clean);
            writeString(c, "windowStateKey", window);
            writeString(c, "v20WindowStateKey", window);
            writeString(c, "dynamicWindowStateKey", window);
            wrote = true;
        }
        if (v20 == null || v20.length() == 0 || "空".equals(v20)) {
            writeString(c, "v20Status", "V20_UNKNOWN");
            writeString(c, "dynamicStatus", "V20_UNKNOWN");
            writeString(c, "dynamicCauseStatus", "V20_UNKNOWN");
            writeInt(c, "v20EmptyFlag", 1);
            writeString(c, "v20FieldSource", "AUTOFETCH_UPSTREAM_FALLBACK");
            writeString(c, "v20FieldVersion", V20_UPSTREAM_EVIDENCE_PATCH_VERSION);
            writeString(
                    c,
                    "v20ActionAdvice",
                    "V20字段未由DynamicCauseChainEngine写入，AutoFetch仅补UNKNOWN和Window兜底，不支持普通BET");
            if (readDouble(c, "v20SupportScore", 0.0d) <= 0.0d) {
                writeDouble(c, "v20SupportScore", 0.18d);
            }
            if (readDouble(c, "v20RiskScore", 0.0d) <= 0.0d) {
                writeDouble(c, "v20RiskScore", 0.42d);
            }
            wrote = true;
        }
        return wrote;
    }

    private String buildFallbackWindowStateKey(Models.ComboCandidate c, List<Models.Draw> clean) {
        int sample = countValidPositionSamples(clean, c == null ? 0 : c.position, 48);
        double k3r = recentK3HitRateForCandidate(c, clean, 48);
        String bucket = "MID";
        if (k3r >= 0.360d) {
            bucket = "K3_HOT";
        } else if (k3r > 0.0d && k3r <= 0.240d) {
            bucket = "K3_COLD";
        }
        int mask = c == null ? 0 : c.k3Mask;
        return "WINDOW_FALLBACK_P"
                + (c == null ? 0 : c.position)
                + "_"
                + bucket
                + "_S"
                + sample
                + "_K"
                + Math.abs(mask);
    }

    private boolean ensureCoverageQualityFields(
            Models.ComboCandidate c, List<Models.Draw> clean, String stage) {
        if (c == null || !c.isValidK3()) {
            return false;
        }
        double oldCover =
                maxDoubleFields(
                        c,
                        new String[] {
                            "coverageQualityScore",
                            "dynamicGateCoverageQualityScore",
                            "finalDecisionCoverageQuality"
                        });
        double oldRisk =
                maxDoubleFields(
                        c,
                        new String[] {
                            "k3CoverageRisk", "dynamicGateCoverageRisk", "finalDecisionCoverageRisk"
                        });
        double oldThird =
                maxDoubleFields(
                        c,
                        new String[] {
                            "thirdNumberQualityScore",
                            "dynamicGateThirdNumberQuality",
                            "finalDecisionThirdNumberQuality"
                        });
        boolean need = oldCover <= 0.0001d || oldThird <= 0.0001d;
        double r32 = recentK3HitRateForCandidate(c, clean, UPSTREAM_COVER_W32);
        double r64 = recentK3HitRateForCandidate(c, clean, UPSTREAM_COVER_W64);
        double r128 = recentK3HitRateForCandidate(c, clean, UPSTREAM_COVER_W128);
        double n1 = recentNumberHitRate(c.position, c.n1, clean, UPSTREAM_COVER_W128);
        double n2 = recentNumberHitRate(c.position, c.n2, clean, UPSTREAM_COVER_W128);
        double n3 = recentNumberHitRate(c.position, c.n3, clean, UPSTREAM_COVER_W128);
        int s128 = countValidPositionSamples(clean, c.position, UPSTREAM_COVER_W128);
        if (r32 <= 0.0d) r32 = UPSTREAM_BASE_K3_RATE;
        if (r64 <= 0.0d) r64 = UPSTREAM_BASE_K3_RATE;
        if (r128 <= 0.0d) r128 = UPSTREAM_BASE_K3_RATE;
        if (n1 <= 0.0d) n1 = UPSTREAM_BASE_NUMBER_RATE;
        if (n2 <= 0.0d) n2 = UPSTREAM_BASE_NUMBER_RATE;
        if (n3 <= 0.0d) n3 = UPSTREAM_BASE_NUMBER_RATE;
        double cover = r32 * 0.42d + r64 * 0.33d + r128 * 0.25d;
        double third = n3;
        double risk = oldRisk;
        if (risk <= 0.0d) {
            risk = buildCoverageRiskBySampleAndRate(s128, cover, third);
        }
        writeDouble(c, "coverageK3HitRate32", r32);
        writeDouble(c, "coverageK3HitRate64", r64);
        writeDouble(c, "coverageK3HitRate128", r128);
        writeDouble(c, "coverageNumberRate1", n1);
        writeDouble(c, "coverageNumberRate2", n2);
        writeDouble(c, "coverageNumberRate3", n3);
        if (oldCover <= 0.0001d) {
            writeDouble(c, "coverageQualityScore", clamp01(cover));
            writeDouble(c, "dynamicGateCoverageQualityScore", clamp01(cover));
            writeDouble(c, "finalDecisionCoverageQuality", clamp01(cover));
        }
        if (oldThird <= 0.0001d) {
            writeDouble(c, "thirdNumberQualityScore", clamp01(third));
            writeDouble(c, "dynamicGateThirdNumberQuality", clamp01(third));
            writeDouble(c, "finalDecisionThirdNumberQuality", clamp01(third));
        }
        writeDouble(c, "k3CoverageRisk", clamp01(risk));
        writeDouble(c, "dynamicGateCoverageRisk", clamp01(risk));
        writeDouble(c, "finalDecisionCoverageRisk", clamp01(risk));
        writeString(
                c,
                "probabilityCoverageSummary",
                "AutoFetch覆盖字段兜底"
                        + (stage == null ? "" : stage)
                        + "：K3[32/64/128]="
                        + UiFormatter.probability(r32)
                        + "/"
                        + UiFormatter.probability(r64)
                        + "/"
                        + UiFormatter.probability(r128)
                        + "，N3="
                        + UiFormatter.probability(third)
                        + "，risk="
                        + UiFormatter.probability(risk)
                        + "；只补字段，不放行BET");
        writeString(c, "coverageQualitySummary", readString(c, "probabilityCoverageSummary", ""));
        writeString(c, "probabilityFieldVersion", V20_UPSTREAM_EVIDENCE_PATCH_VERSION);
        int falseHigh = shouldMarkUpstreamFalseHigh(c) ? 1 : 0;
        if (falseHigh > 0) {
            writeInt(c, "falseHighPFlag", 1);
            writeInt(c, "probabilityFalseHighPFlag", 1);
            writeDouble(c, "probabilityFalseHighRisk", 0.72d);
        }
        return need;
    }

    private double buildCoverageRiskBySampleAndRate(int sample, double cover, double third) {
        double risk;
        if (sample < 30) {
            risk = UPSTREAM_SAMPLE_RISK;
        } else if (cover < 0.250d || third < 0.075d) {
            risk = 0.700d;
        } else if (cover < 0.285d || third < 0.088d) {
            risk = 0.620d;
        } else if (cover >= 0.340d && third >= 0.105d) {
            risk = 0.360d;
        } else {
            risk = 0.480d;
        }
        return clamp01(risk);
    }

    private boolean ensureTeacherPolicyFields(Models.ComboCandidate c, String stage) {
        if (c == null) return false;
        String status = readTeacherStatus(c);
        if (status == null || status.length() == 0) status = "NO_SAMPLE_NEUTRAL";
        String s = status.toUpperCase(java.util.Locale.US);
        boolean hard =
                AppConfig.isExplicitHardBlockStatus(s) || "RISK".equals(s) || "FREEZE".equals(s);
        boolean softWeak = "WEAK".equals(s);
        writeInt(c, "teacherWeakNoBetFlag", softWeak ? 1 : 0);
        writeInt(c, "teacherMatrixWeakNoBetFlag", softWeak ? 1 : 0);
        writeInt(c, "teacherMatrixOrdinaryBetBlockFlag", hard ? 1 : 0);
        writeInt(c, "teacherMatrixRiskFlag", hard ? 1 : 0);
        writeInt(c, "teacherMatrixFreezeFlag", "FREEZE".equals(s) ? 1 : 0);
        if (hard) {
            writeInt(c, "teacherMatrixSupportsBet", 0);
            writeInt(c, "teacherMatrixSupportsFinalGate", 0);
            writeString(c, "teacherOrdinaryBetPolicy", "HARD_BLOCK");
            writeString(c, "teacherMatrixOrdinaryBetPolicy", "HARD_BLOCK");
            writeString(c, "riskSeverity", AppConfig.EVIDENCE_SEVERITY_HARD_BLOCK);
            writeInt(c, "hardBlockFlag", 1);
            writeString(c, "hardBlockSource", "TeacherNumberMatrixEngine");
        } else if ("READY".equals(s) || "STRONG".equals(s)) {
            writeInt(c, "teacherMatrixSupportsBet", 1);
            writeInt(c, "teacherMatrixSupportsFinalGate", 1);
            writeString(c, "teacherOrdinaryBetPolicy", "ALLOW_REVIEW");
            writeString(c, "teacherMatrixOrdinaryBetPolicy", "ALLOW_REVIEW");
        } else {
            writeInt(c, "teacherMatrixSupportsBet", 0);
            writeInt(c, "teacherMatrixSupportsFinalGate", 0);
            writeString(c, "teacherOrdinaryBetPolicy", "WATCH_ONLY");
            writeString(c, "teacherMatrixOrdinaryBetPolicy", "WATCH_ONLY");
            if (readInt(c, "hardBlockFlag", 0) == 0)
                writeString(
                        c,
                        "riskSeverity",
                        softWeak
                                ? AppConfig.EVIDENCE_SEVERITY_SOFT_RISK
                                : AppConfig.EVIDENCE_SEVERITY_NEUTRAL);
        }
        writeString(c, "teacherFieldSource", "TEACHER_NUMBER_MATRIX_ONLY");
        writeString(c, "teacherFieldVersion", AppConfig.OUMAN_GALAXY_REBUILD_VERSION);
        writeString(c, "teacherStatusClass", s);
        return true;
    }

    private boolean isTeacherStatusActionable(String status) {
        if (status == null || status.length() == 0) {
            return false;
        }
        String s = status.toUpperCase(java.util.Locale.ROOT);
        return "STRONG".equals(s)
                || "READY".equals(s)
                || "WATCH".equals(s)
                || "WEAK".equals(s)
                || "RISK".equals(s)
                || "FREEZE".equals(s)
                || "NO_SAMPLE".equals(s);
    }

    private boolean shouldMarkUpstreamFalseHigh(Models.ComboCandidate c) {
        if (c == null) {
            return false;
        }
        double edge = c.edge;
        if (Double.isNaN(edge) || Double.isInfinite(edge) || Math.abs(edge) > 1.0d) {
            edge = c.finalP - c.dynamicGate;
        }
        String teacher = readTeacherStatus(c);
        String v20 =
                readFirstString(
                        c, new String[] {"v20Status", "dynamicStatus", "dynamicCauseStatus"});
        return c.finalP >= 0.400d
                && (teacherWeakOrEmptyText(teacher)
                        || weakOrEmptyV20Text(v20)
                        || edge > FIELD_CHAIN_EDGE_FALSE_STRONG_LINE);
    }

    private double recentK3HitRateForCandidate(
            Models.ComboCandidate c, List<Models.Draw> clean, int window) {
        if (c == null
                || !AppConfig.isValidPosition(c.position)
                || clean == null
                || clean.size() == 0) {
            return 0.0d;
        }
        int sample = 0;
        int hit = 0;
        int start = clean.size() - window;
        if (start < 0) start = 0;
        for (int i = start; i < clean.size(); i++) {
            Models.Draw d = clean.get(i);
            if (d == null || !d.isValid()) continue;
            int actual = d.getNumberAtPosition(c.position);
            if (!AppConfig.isValidNumber(actual)) continue;
            sample++;
            if (actual == c.n1 || actual == c.n2 || actual == c.n3) {
                hit++;
            }
        }
        return sample <= 0 ? 0.0d : ((double) hit) / ((double) sample);
    }

    private double recentNumberHitRate(
            int position, int number, List<Models.Draw> clean, int window) {
        if (!AppConfig.isValidPosition(position)
                || !AppConfig.isValidNumber(number)
                || clean == null
                || clean.size() == 0) {
            return 0.0d;
        }
        int sample = 0;
        int hit = 0;
        int start = clean.size() - window;
        if (start < 0) start = 0;
        for (int i = start; i < clean.size(); i++) {
            Models.Draw d = clean.get(i);
            if (d == null || !d.isValid()) continue;
            int actual = d.getNumberAtPosition(position);
            if (!AppConfig.isValidNumber(actual)) continue;
            sample++;
            if (actual == number) {
                hit++;
            }
        }
        return sample <= 0 ? 0.0d : ((double) hit) / ((double) sample);
    }

    private int countValidPositionSamples(List<Models.Draw> clean, int position, int window) {
        if (!AppConfig.isValidPosition(position) || clean == null || clean.size() == 0) {
            return 0;
        }
        int sample = 0;
        int start = clean.size() - window;
        if (start < 0) start = 0;
        for (int i = start; i < clean.size(); i++) {
            Models.Draw d = clean.get(i);
            if (d == null || !d.isValid()) continue;
            int actual = d.getNumberAtPosition(position);
            if (AppConfig.isValidNumber(actual)) {
                sample++;
            }
        }
        return sample;
    }

    private double clamp01(double v) {
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

    private void runV20PreDrawFeatureSafely(
            List<Models.Draw> clean, String baseIssue, String targetIssue) {
        if (preDrawFeatureEngine == null || store == null || clean == null || clean.size() == 0) {
            return;
        }

        Object result = REFLECTION_NO_METHOD;

        result =
                invokeOptional(
                        preDrawFeatureEngine,
                        "build",
                        new Class[] {DbStore.class, List.class, String.class, String.class},
                        new Object[] {store, clean, baseIssue, targetIssue});
        if (result == REFLECTION_NO_METHOD) {
            result =
                    invokeOptional(
                            preDrawFeatureEngine,
                            "analyze",
                            new Class[] {DbStore.class, List.class, String.class, String.class},
                            new Object[] {store, clean, baseIssue, targetIssue});
        }
        if (result == REFLECTION_NO_METHOD) {
            result =
                    invokeOptional(
                            preDrawFeatureEngine,
                            "run",
                            new Class[] {DbStore.class, List.class, String.class, String.class},
                            new Object[] {store, clean, baseIssue, targetIssue});
        }
        if (result == REFLECTION_NO_METHOD) {
            result =
                    invokeOptional(
                            preDrawFeatureEngine,
                            "buildFeatures",
                            new Class[] {List.class},
                            new Object[] {clean});
        }

        if (result != REFLECTION_NO_METHOD) {
            try {
                store.setState(
                        "v20_predraw_feature_summary",
                        buildGenericInvokeSummary(result, "V20预开奖特征已生成"));
            } catch (Throwable e) {
            }
        }
    }

    private Models.CandidateBatch runV20FullCandidateScanOrFallback(
            List<Models.Draw> clean,
            List<Models.ShapeVector> vectors,
            List<TrendStateMachine.TrendTrace> traces,
            List<BreakRecoveryEngine.RecoveryResult> recoveries) {

        if (fullCandidateScanEngine != null) {
            Object result = REFLECTION_NO_METHOD;

            result =
                    invokeOptional(
                            fullCandidateScanEngine,
                            "scan",
                            new Class[] {
                                DbStore.class, List.class, List.class, List.class, List.class
                            },
                            new Object[] {store, clean, vectors, traces, recoveries});
            if (result == REFLECTION_NO_METHOD) {
                result =
                        invokeOptional(
                                fullCandidateScanEngine,
                                "scan",
                                new Class[] {List.class, List.class, List.class, List.class},
                                new Object[] {clean, vectors, traces, recoveries});
            }
            if (result == REFLECTION_NO_METHOD) {
                result =
                        invokeOptional(
                                fullCandidateScanEngine,
                                "build",
                                new Class[] {DbStore.class, List.class, List.class},
                                new Object[] {store, clean, vectors});
            }
            if (result == REFLECTION_NO_METHOD) {
                result =
                        invokeOptional(
                                fullCandidateScanEngine,
                                "scan",
                                new Class[] {List.class, List.class},
                                new Object[] {clean, vectors});
            }

            if (result instanceof Models.CandidateBatch) {
                Models.CandidateBatch b = (Models.CandidateBatch) result;
                if (b != null && b.candidates != null && b.candidates.size() > 0) {
                    try {
                        store.setState(
                                "v20_full_scan_summary", "V20全候选扫描完成：候选=" + b.candidates.size());
                    } catch (Throwable e) {
                    }
                    return b;
                }
            }

            ArrayList<Models.ComboCandidate> list = castCandidateList(result);
            if (list.size() > 0) {
                Models.CandidateBatch b = new Models.CandidateBatch();
                for (int i = 0; i < list.size(); i++) {
                    b.add(list.get(i));
                }
                b.scannedCount = list.size();
                b.keptCount = list.size();
                b.fullScan = 1;
                b.scanMode = "V20_FULL_CANDIDATE_SCAN";
                try {
                    store.setState("v20_full_scan_summary", "V20全候选扫描完成：候选=" + list.size());
                } catch (Throwable e) {
                }
                return b;
            }
        }

        return comboScannerEngine.scan(clean, vectors);
    }

    private ArrayList<Models.ComboCandidate> runV20CandidateCauseEnginesBeforeProbability(
            ArrayList<Models.ComboCandidate> candidates,
            List<Models.Draw> clean,
            List<Models.ShapeVector> vectors,
            List<TrendStateMachine.TrendTrace> traces,
            List<BreakRecoveryEngine.RecoveryResult> recoveries,
            String baseIssue,
            String targetIssue) {

        ArrayList<Models.ComboCandidate> out =
                candidates == null ? new ArrayList<Models.ComboCandidate>() : candidates;

        out =
                runCandidateEngineListReturn(
                        causalFactorMatrixEngine,
                        "causal_factor_matrix",
                        out,
                        clean,
                        vectors,
                        traces,
                        recoveries,
                        baseIssue,
                        targetIssue);
        out =
                runCandidateEngineListReturn(
                        positionCauseMatrixEngine,
                        "position_cause_matrix",
                        out,
                        clean,
                        vectors,
                        traces,
                        recoveries,
                        baseIssue,
                        targetIssue);
        out =
                runCandidateEngineListReturn(
                        teacherNumberMatrixEngine,
                        "teacher_number_matrix",
                        out,
                        clean,
                        vectors,
                        traces,
                        recoveries,
                        baseIssue,
                        targetIssue);
        out =
                runCandidateEngineListReturn(
                        counterfactualRankEngine,
                        "counterfactual_rank",
                        out,
                        clean,
                        vectors,
                        traces,
                        recoveries,
                        baseIssue,
                        targetIssue);
        out =
                runCandidateEngineListReturn(
                        seedCauseExpansionEngine,
                        "seed_cause_expansion",
                        out,
                        clean,
                        vectors,
                        traces,
                        recoveries,
                        baseIssue,
                        targetIssue);
        out =
                runCandidateEngineListReturn(
                        dynamicCauseChainEngine,
                        "dynamic_cause_chain_pre",
                        out,
                        clean,
                        vectors,
                        traces,
                        recoveries,
                        baseIssue,
                        targetIssue);

        return out;
    }

    private ArrayList<Models.ComboCandidate> runV20CandidateCauseEnginesAfterGate(
            ArrayList<Models.ComboCandidate> candidates,
            List<Models.Draw> clean,
            List<Models.ShapeVector> vectors,
            List<TrendStateMachine.TrendTrace> traces,
            List<BreakRecoveryEngine.RecoveryResult> recoveries,
            String baseIssue,
            String targetIssue) {

        ArrayList<Models.ComboCandidate> out =
                candidates == null ? new ArrayList<Models.ComboCandidate>() : candidates;

        out =
                runCandidateEngineListReturn(
                        teacherNumberMatrixEngine,
                        "teacher_number_matrix_after_gate_pre_guard",
                        out,
                        clean,
                        vectors,
                        traces,
                        recoveries,
                        baseIssue,
                        targetIssue);
        out =
                runCandidateEngineListReturn(
                        thirdGuardMatrixEngine,
                        "third_guard_matrix",
                        out,
                        clean,
                        vectors,
                        traces,
                        recoveries,
                        baseIssue,
                        targetIssue);
        out =
                runCandidateEngineListReturn(
                        ruleHealthMatrixEngine,
                        "rule_health_matrix",
                        out,
                        clean,
                        vectors,
                        traces,
                        recoveries,
                        baseIssue,
                        targetIssue);
        out =
                runCandidateEngineListReturn(
                        shadowHealthGateEngine,
                        "shadow_health_gate",
                        out,
                        clean,
                        vectors,
                        traces,
                        recoveries,
                        baseIssue,
                        targetIssue);
        out =
                runCandidateEngineListReturn(
                        falseStrongFreezeEngine,
                        "false_strong_freeze",
                        out,
                        clean,
                        vectors,
                        traces,
                        recoveries,
                        baseIssue,
                        targetIssue);
        out =
                runCandidateEngineListReturn(
                        dynamicCauseChainEngine,
                        "dynamic_cause_chain_post",
                        out,
                        clean,
                        vectors,
                        traces,
                        recoveries,
                        baseIssue,
                        targetIssue);
        out =
                runCandidateEngineListReturn(
                        teacherNumberMatrixEngine,
                        "teacher_number_matrix_after_gate_post_guard",
                        out,
                        clean,
                        vectors,
                        traces,
                        recoveries,
                        baseIssue,
                        targetIssue);

        return out;
    }

    private ArrayList<Models.ComboCandidate> runCandidateEngineListReturn(
            Object engine,
            String statePrefix,
            ArrayList<Models.ComboCandidate> candidates,
            List<Models.Draw> clean,
            List<Models.ShapeVector> vectors,
            List<TrendStateMachine.TrendTrace> traces,
            List<BreakRecoveryEngine.RecoveryResult> recoveries,
            String baseIssue,
            String targetIssue) {
        ArrayList<Models.ComboCandidate> current =
                candidates == null ? new ArrayList<Models.ComboCandidate>() : candidates;
        if (engine instanceof TeacherNumberMatrixEngine) {
            try {
                ArrayList<Models.ComboCandidate> typed =
                        ((TeacherNumberMatrixEngine) engine)
                                .applyToAll(store, current, clean, baseIssue, targetIssue);
                heartbeatStateV8()
                        .markTeacherPump(
                                "REALTIME_OK", "candidates=" + (typed == null ? 0 : typed.size()));
                return typed == null ? current : typed;
            } catch (Throwable e) {
                try {
                    store.setState("teacher_realtime_apply_error", safeError(e));
                } catch (Throwable ignore) {
                }
                try {
                    heartbeatStateV8().markTeacherPump("REALTIME_ERROR", safeError(e));
                } catch (Throwable ignore) {
                }
                for (int i = 0; i < current.size(); i++)
                    if (current.get(i) != null) current.get(i).teacherApplyError = safeError(e);
                return current;
            }
        }

        /* V43: the formal realtime chain uses typed adapters. Reflection remains only
         * for third-party/older compatibility and no longer decides whether core
         * strategy modules silently run or silently disappear. */
        try {
            ArrayList<Models.ComboCandidate> typed = null;
            if (engine instanceof CausalFactorMatrixEngine) {
                typed =
                        ((CausalFactorMatrixEngine) engine)
                                .applyToAll(store, current, clean, baseIssue, targetIssue);
            } else if (engine instanceof PositionCauseMatrixEngine) {
                typed =
                        ((PositionCauseMatrixEngine) engine)
                                .applyToAll(store, current, clean, baseIssue, targetIssue);
            } else if (engine instanceof CounterfactualRankEngine) {
                typed =
                        ((CounterfactualRankEngine) engine)
                                .applyToAll(store, current, clean, baseIssue, targetIssue);
            } else if (engine instanceof SeedCauseExpansionEngine) {
                typed =
                        ((SeedCauseExpansionEngine) engine)
                                .applyToAll(store, current, clean, baseIssue, targetIssue);
            } else if (engine instanceof DynamicCauseChainEngine) {
                typed =
                        ((DynamicCauseChainEngine) engine)
                                .applyToAll(store, current, clean, baseIssue, targetIssue);
            } else if (engine instanceof ThirdGuardMatrixEngine) {
                typed =
                        ((ThirdGuardMatrixEngine) engine)
                                .applyToAll(store, current, clean, baseIssue, targetIssue);
            } else if (engine instanceof RuleHealthMatrixEngine) {
                typed =
                        ((RuleHealthMatrixEngine) engine)
                                .applyToAll(store, current, clean, baseIssue, targetIssue);
            } else if (engine instanceof ShadowHealthGateEngine) {
                typed =
                        ((ShadowHealthGateEngine) engine)
                                .applyToAll(store, current, clean, baseIssue, targetIssue);
            } else if (engine instanceof FalseStrongFreezeEngine) {
                typed =
                        ((FalseStrongFreezeEngine) engine)
                                .applyToAll(store, current, clean, baseIssue, targetIssue);
            }
            if (typed != null) {
                try {
                    store.setState("v43_typed_stage_" + statePrefix + "_status", "OK");
                    store.setState(
                            "v43_typed_stage_" + statePrefix + "_count",
                            String.valueOf(typed.size()));
                } catch (Throwable ignore) {
                }
                return mergeUpstreamEvidenceFromPrevious(
                        current, typed, "V43_TYPED_" + statePrefix);
            }
        } catch (Throwable typedError) {
            try {
                store.setState("v43_typed_stage_" + statePrefix + "_status", "ERROR");
                store.setState("v43_typed_stage_" + statePrefix + "_error", safeError(typedError));
            } catch (Throwable ignore) {
            }
            for (int i = 0; i < current.size(); i++) {
                Models.ComboCandidate c = current.get(i);
                if (c != null) {
                    c.v43InvariantStatus = "WATCH:MODULE_ERROR_" + statePrefix;
                    c.v43MissingEvidence = appendReason(c.v43MissingEvidence, statePrefix);
                }
            }
            return current;
        }

        if (engine == null || current.size() == 0) {
            return current;
        }

        Object result = REFLECTION_NO_METHOD;

        result =
                invokeOptional(
                        engine,
                        "apply",
                        new Class[] {
                            DbStore.class,
                            List.class,
                            List.class,
                            List.class,
                            List.class,
                            List.class,
                            String.class,
                            String.class
                        },
                        new Object[] {
                            store,
                            current,
                            clean,
                            vectors,
                            traces,
                            recoveries,
                            baseIssue,
                            targetIssue
                        });
        if (result == REFLECTION_NO_METHOD) {
            result =
                    invokeOptional(
                            engine,
                            "apply",
                            new Class[] {
                                DbStore.class, List.class, List.class, List.class, List.class
                            },
                            new Object[] {store, current, clean, traces, recoveries});
        }
        if (result == REFLECTION_NO_METHOD) {
            result =
                    invokeOptional(
                            engine,
                            "applyToCandidates",
                            new Class[] {
                                DbStore.class, List.class, List.class, List.class, List.class
                            },
                            new Object[] {store, current, clean, traces, recoveries});
        }
        if (result == REFLECTION_NO_METHOD) {
            result =
                    invokeOptional(
                            engine,
                            "enrich",
                            new Class[] {
                                DbStore.class, List.class, List.class, List.class, List.class
                            },
                            new Object[] {store, current, clean, traces, recoveries});
        }
        if (result == REFLECTION_NO_METHOD) {
            result =
                    invokeOptional(
                            engine,
                            "run",
                            new Class[] {DbStore.class, List.class, List.class},
                            new Object[] {store, current, clean});
        }
        if (result == REFLECTION_NO_METHOD) {
            result =
                    invokeOptional(
                            engine,
                            "apply",
                            new Class[] {List.class, List.class},
                            new Object[] {current, clean});
        }

        if (result == REFLECTION_NO_METHOD) {
            result =
                    invokeOptional(
                            engine,
                            "applyToAll",
                            new Class[] {
                                DbStore.class,
                                ArrayList.class,
                                List.class,
                                String.class,
                                String.class
                            },
                            new Object[] {store, current, clean, baseIssue, targetIssue});
        }
        if (result == REFLECTION_NO_METHOD) {
            result =
                    invokeOptional(
                            engine,
                            "enrichCandidates",
                            new Class[] {
                                DbStore.class,
                                ArrayList.class,
                                List.class,
                                String.class,
                                String.class
                            },
                            new Object[] {store, current, clean, baseIssue, targetIssue});
        }
        if (result == REFLECTION_NO_METHOD) {
            result =
                    invokeOptional(
                            engine,
                            "analyzeCandidates",
                            new Class[] {
                                DbStore.class,
                                ArrayList.class,
                                List.class,
                                String.class,
                                String.class
                            },
                            new Object[] {store, current, clean, baseIssue, targetIssue});
        }
        if (result == REFLECTION_NO_METHOD) {
            result =
                    invokeOptional(
                            engine,
                            "applyRealtime",
                            new Class[] {ArrayList.class, List.class, String.class, String.class},
                            new Object[] {current, clean, baseIssue, targetIssue});
        }
        if (result == REFLECTION_NO_METHOD) {
            result =
                    invokeOptional(
                            engine,
                            "runRealtime",
                            new Class[] {ArrayList.class, List.class, String.class, String.class},
                            new Object[] {current, clean, baseIssue, targetIssue});
        }

        ArrayList<Models.ComboCandidate> beforeEngine = current;
        ArrayList<Models.ComboCandidate> list = castCandidateList(result);

        if (list.size() > 0) {
            current = mergeUpstreamEvidenceFromPrevious(beforeEngine, list, statePrefix);
        }

        if (store != null && result != REFLECTION_NO_METHOD) {
            try {
                store.setState(
                        "v20_" + statePrefix + "_summary",
                        buildGenericInvokeSummary(
                                result, "V20 " + statePrefix + " 已执行，候选=" + current.size()));
                store.setState("v20_" + statePrefix + "_count", String.valueOf(current.size()));
                store.setState(
                        "v20_" + statePrefix + "_updated_at",
                        String.valueOf(System.currentTimeMillis()));
            } catch (Throwable e) {
            }
        }

        return current;
    }

    private ArrayList<Models.ComboCandidate> runTeacherNumberMatrixFinalBridge(
            ArrayList<Models.ComboCandidate> candidates,
            List<Models.Draw> clean,
            List<Models.ShapeVector> vectors,
            List<TrendStateMachine.TrendTrace> traces,
            List<BreakRecoveryEngine.RecoveryResult> recoveries,
            String baseIssue,
            String targetIssue,
            String stage) {

        ArrayList<Models.ComboCandidate> current =
                candidates == null ? new ArrayList<Models.ComboCandidate>() : candidates;

        if (current.size() == 0) {
            return current;
        }

        String stageName = stage == null ? "" : stage;
        boolean sameTarget = targetIssue != null && targetIssue.equals(teacherBridgeTargetIssueV3);
        if (AppConfig.REALTIME_TEACHER_SINGLE_BRIDGE
                && sameTarget
                && teacherBridgeCompletedV3
                && !AppConfig.REALTIME_TEACHER_PRIMARY_STAGE.equalsIgnoreCase(stageName)
                && allCandidatesKeepTeacherEvidence73487246(current)) {
            for (int i = 0; i < current.size(); i++) syncTeacherAliasFields(current.get(i));
            writeTeacherBridgeReuseState(stageName, current);
            return current;
        }
        if (shouldReuseTeacherBridgeForRedundantStage(stageName, current)) {
            for (int i = 0; i < current.size(); i++) syncTeacherAliasFields(current.get(i));
            writeTeacherBridgeReuseState(stageName, current);
            return current;
        }

        if (teacherNumberMatrixEngine != null) {
            current =
                    runCandidateEngineListReturn(
                            teacherNumberMatrixEngine,
                            "teacher_number_matrix_bridge_" + stageName.toLowerCase(java.util.Locale.ROOT),
                            current,
                            clean,
                            vectors,
                            traces,
                            recoveries,
                            baseIssue,
                            targetIssue);
        }

        forceTeacherMatrixFieldsFromHistory(current, clean, baseIssue, targetIssue, stageName);
        if (sameTarget && AppConfig.REALTIME_TEACHER_PRIMARY_STAGE.equalsIgnoreCase(stageName))
            teacherBridgeCompletedV3 = true;

        if (store != null) {
            try {
                int filled = 0;
                int empty = 0;
                int sampleSum = 0;

                for (int i = 0; i < current.size(); i++) {
                    Models.ComboCandidate c = current.get(i);
                    if (c == null) {
                        continue;
                    }

                    int s = readTeacherSample(c);
                    if (s > 0) {
                        filled++;
                        sampleSum += s;
                    } else {
                        empty++;
                    }
                }

                store.setState("teacher_matrix_bridge_stage", stageName);
                store.setState(
                        "teacher_matrix_bridge_candidate_count", String.valueOf(current.size()));
                store.setState("teacher_matrix_bridge_filled", String.valueOf(filled));
                store.setState("teacher_matrix_bridge_empty", String.valueOf(empty));
                store.setState("teacher_matrix_bridge_sample_sum", String.valueOf(sampleSum));
                store.setState(
                        "teacher_matrix_bridge_summary",
                        "教师矩阵实时桥接完成：stage="
                                + stageName
                                + "，候选="
                                + current.size()
                                + "，已写字段="
                                + filled
                                + "，空字段="
                                + empty
                                + "，样本和="
                                + sampleSum
                                + "；已同时写 teacherMatrix/teacherNumber 双字段");
                store.setState(
                        "teacher_matrix_bridge_updated_at",
                        String.valueOf(System.currentTimeMillis()));
            } catch (Throwable e) {
            }
        }

        return current;
    }

    private boolean allCandidatesKeepTeacherEvidence73487246(
            ArrayList<Models.ComboCandidate> candidates) {
        if (candidates == null || candidates.size() == 0) return false;
        int valid = 0;
        int withSample = 0;
        for (int i = 0; i < candidates.size(); i++) {
            Models.ComboCandidate c = candidates.get(i);
            if (c == null) continue;
            valid++;
            if (readTeacherSample(c) > 0 && readTeacherConfidence(c) > 0.0d) withSample++;
        }
        return valid > 0 && withSample * 100 >= valid * 90;
    }

    private boolean shouldReuseTeacherBridgeForRedundantStage(
            String stageName, ArrayList<Models.ComboCandidate> candidates) {
        if (stageName == null || candidates == null || candidates.size() == 0) {
            return false;
        }

        String s = stageName.trim().toUpperCase(java.util.Locale.ROOT);
        if (AppConfig.REALTIME_TEACHER_SINGLE_BRIDGE
                && !AppConfig.REALTIME_TEACHER_PRIMARY_STAGE.equals(s)
                && allCandidatesKeepTeacherEvidence73487246(candidates)) return true;
        if (!"AFTER_PROBABILITY".equals(s)
                && !"AFTER_CALIBRATION".equals(s)
                && !"AFTER_GATE".equals(s)
                && !"BEFORE_DECISION_GATE".equals(s)) {
            return false;
        }

        int ready = 0;
        int total = 0;
        for (int i = 0; i < candidates.size(); i++) {
            Models.ComboCandidate c = candidates.get(i);
            if (c == null) {
                continue;
            }
            total++;
            if (readTeacherSample(c) > 0 && readTeacherConfidence(c) > 0.0d) {
                ready++;
            }
        }

        if (total <= 0) {
            return false;
        }

        return ready * 100 >= total * TEACHER_BRIDGE_READY_PERCENT_TO_SKIP_REDUNDANT;
    }

    private void writeTeacherBridgeReuseState(
            String stageName, ArrayList<Models.ComboCandidate> candidates) {
        if (store == null) {
            return;
        }
        try {
            int total = candidates == null ? 0 : candidates.size();
            store.setState("teacher_matrix_bridge_stage", stageName == null ? "" : stageName);
            store.setState("teacher_matrix_bridge_reuse", "1");
            store.setState("teacher_matrix_bridge_candidate_count", String.valueOf(total));
            store.setState(
                    "teacher_matrix_bridge_summary",
                    "教师矩阵桥接复用已写字段：stage="
                            + (stageName == null ? "" : stageName)
                            + "，候选="
                            + total
                            + "；跳过重复反射重算，不快裁、不绕过DecisionGate");
            store.setState(
                    "teacher_matrix_bridge_updated_at", String.valueOf(System.currentTimeMillis()));
        } catch (Throwable e) {
        }
    }

    private void forceTeacherMatrixFieldsFromHistory(
            ArrayList<Models.ComboCandidate> candidates,
            List<Models.Draw> clean,
            String baseIssue,
            String targetIssue,
            String stage) {
        if (candidates == null) return;
        for (int i = 0; i < candidates.size(); i++) {
            Models.ComboCandidate c = candidates.get(i);
            if (c == null || !c.isValidK3()) continue;
            syncTeacherAliasFields(c);
            if (readTeacherSample(c) <= 0) {
                writeTeacherFields(
                        c,
                        "NO_SAMPLE_NEUTRAL",
                        0,
                        0.0d,
                        0.0d,
                        0.0d,
                        0.0d,
                        0.0d,
                        0.0d,
                        "TeacherNumberMatrix尚无已结算同域样本；AutoFetch不再现场按当前历史频率覆盖教师证据");
                writeString(c, "teacherAsOfIssue", "");
                writeInt(c, "teacherFreshByIssue", 0);
                writeString(c, "evidenceSource", "NO_SETTLED_TEACHER_EVIDENCE");
                writeInt(c, "evidenceSettledOnly", 1);
            }
            ensureTeacherPolicyFields(c, stage);
        }
    }

    private void syncTeacherAliasFields(Object obj) {
        if (obj == null) {
            return;
        }

        String status = readTeacherStatus(obj);
        int sample = readTeacherSample(obj);
        double hitRate = readTeacherHitRate(obj);
        double confidence = readTeacherConfidence(obj);
        double risk = readTeacherRisk(obj);
        double score = readTeacherScore(obj);
        double probAdjust = readTeacherProbAdjust(obj);
        double gateAdjust = readTeacherGateAdjust(obj);
        String summary = readTeacherSummary(obj);

        writeTeacherFields(
                obj,
                status,
                sample,
                hitRate,
                confidence,
                risk,
                score,
                probAdjust,
                gateAdjust,
                summary);
    }

    private void writeTeacherFields(
            Object obj,
            String status,
            int sample,
            double hitRate,
            double confidence,
            double risk,
            double score,
            double probAdjust,
            double gateAdjust,
            String summary) {

        if (obj == null) {
            return;
        }

        if (status == null || status.length() == 0) {
            status = "NEUTRAL";
        }

        if (summary == null) {
            summary = "";
        }

        writeString(obj, "teacherMatrixStatus", status);
        writeInt(obj, "teacherMatrixSample", sample);
        writeDouble(obj, "teacherMatrixHitRate", hitRate);
        writeDouble(obj, "teacherMatrixConfidence", confidence);
        writeDouble(obj, "teacherMatrixRisk", risk);
        writeDouble(obj, "teacherMatrixScore", score);
        writeDouble(obj, "teacherMatrixProbAdjust", probAdjust);
        writeDouble(obj, "teacherMatrixGateAdjust", gateAdjust);
        writeString(obj, "teacherMatrixSummary", summary);

        writeString(obj, "teacherNumberStatus", status);
        writeInt(obj, "teacherNumberSample", sample);
        writeDouble(obj, "teacherNumberHitRate", hitRate);
        writeDouble(obj, "teacherNumberConfidence", confidence);
        writeDouble(obj, "teacherNumberRisk", risk);
        writeDouble(obj, "teacherNumberScore", score);
        writeDouble(obj, "teacherNumberProbAdjust", probAdjust);
        writeDouble(obj, "teacherNumberGateAdjust", gateAdjust);
        writeString(obj, "teacherNumberSummary", summary);

        writeString(obj, "teacherStatus", status);
        writeInt(obj, "teacherSample", sample);
        writeDouble(obj, "teacherHitRate", hitRate);
        writeDouble(obj, "teacherConfidence", confidence);
        writeDouble(obj, "teacherRisk", risk);
        writeDouble(obj, "teacherScore", score);

        writeDouble(
                obj,
                "teacherSortBoost",
                teacherBridgeSortBoost(status, sample, hitRate, confidence, risk, score));
    }

    private double teacherBridgeSortBoost(
            String status,
            int sample,
            double hitRate,
            double confidence,
            double risk,
            double score) {
        if (sample <= 0) {
            return 0.0d;
        }

        double boost = 0.0d;
        boost += (score - 0.50d) * 0.030d;
        boost += (hitRate - 0.10d) * 0.070d;
        boost += confidence * 0.004d;
        boost -= risk * 0.018d;

        if ("RISK".equals(status)) {
            boost -= 0.018d;
        } else if ("READY".equals(status)) {
            boost += 0.008d;
        }

        if (boost > 0.018d) {
            boost = 0.018d;
        }

        if (boost < -0.025d) {
            boost = -0.025d;
        }

        return boost;
    }

    private String readTeacherStatus(Object obj) {
        String v = readString(obj, "teacherMatrixStatus", "");
        if (v == null || v.length() == 0) {
            v = readString(obj, "teacherNumberStatus", "");
        }
        if (v == null || v.length() == 0) {
            v = readString(obj, "teacherStatus", "");
        }
        return v == null ? "" : v;
    }

    private int readTeacherSample(Object obj) {
        int v = readInt(obj, "teacherMatrixSample", 0);
        if (v <= 0) {
            v = readInt(obj, "teacherNumberSample", 0);
        }
        if (v <= 0) {
            v = readInt(obj, "teacherSample", 0);
        }
        return v;
    }

    private double readTeacherHitRate(Object obj) {
        double v = readDouble(obj, "teacherMatrixHitRate", 0.0d);
        if (v <= 0.0d) {
            v = readDouble(obj, "teacherNumberHitRate", 0.0d);
        }
        if (v <= 0.0d) {
            v = readDouble(obj, "teacherHitRate", 0.0d);
        }
        return normalizeProbLike(v);
    }

    private double readTeacherConfidence(Object obj) {
        double v = readDouble(obj, "teacherMatrixConfidence", 0.0d);
        if (v <= 0.0d) {
            v = readDouble(obj, "teacherNumberConfidence", 0.0d);
        }
        if (v <= 0.0d) {
            v = readDouble(obj, "teacherConfidence", 0.0d);
        }
        return normalizeProbLike(v);
    }

    private double readTeacherRisk(Object obj) {
        double v = readDouble(obj, "teacherMatrixRisk", 0.0d);
        if (v <= 0.0d) {
            v = readDouble(obj, "teacherNumberRisk", 0.0d);
        }
        if (v <= 0.0d) {
            v = readDouble(obj, "teacherRisk", 0.0d);
        }
        return normalizeProbLike(v);
    }

    private double readTeacherScore(Object obj) {
        double v = readDouble(obj, "teacherMatrixScore", 0.0d);
        if (v <= 0.0d) {
            v = readDouble(obj, "teacherNumberScore", 0.0d);
        }
        if (v <= 0.0d) {
            v = readDouble(obj, "teacherScore", 0.0d);
        }
        return normalizeProbLike(v);
    }

    private double readTeacherProbAdjust(Object obj) {
        double v = readDouble(obj, "teacherMatrixProbAdjust", 0.0d);
        if (v == 0.0d) {
            v = readDouble(obj, "teacherNumberProbAdjust", 0.0d);
        }
        return v;
    }

    private double readTeacherGateAdjust(Object obj) {
        double v = readDouble(obj, "teacherMatrixGateAdjust", 0.0d);
        if (v == 0.0d) {
            v = readDouble(obj, "teacherNumberGateAdjust", 0.0d);
        }
        return v;
    }

    private String readTeacherSummary(Object obj) {
        String v = readString(obj, "teacherMatrixSummary", "");
        if (v == null || v.length() == 0) {
            v = readString(obj, "teacherNumberSummary", "");
        }
        if (v == null || v.length() == 0) {
            v = readString(obj, "teacherSummary", "");
        }
        return v == null ? "" : v;
    }

    private double normalizeProbLike(double v) {
        if (Double.isNaN(v) || Double.isInfinite(v)) {
            return 0.0d;
        }

        if (v > 1.0d && v <= 100.0d) {
            v = v / 100.0d;
        }

        if (v < 0.0d) {
            v = 0.0d;
        }

        if (v > 1.0d) {
            v = 1.0d;
        }

        return v;
    }

    private ArrayList<Models.ComboCandidate> enrichRealtimeCandidatesFullMainChain(
            List<Models.ComboCandidate> rawForProbability,
            List<Models.Draw> clean,
            List<TrendStateMachine.TrendTrace> traces,
            List<BreakRecoveryEngine.RecoveryResult> recoveries,
            int probabilityCap,
            int decisionCap) {
        ArrayList<Models.ComboCandidate> out = new ArrayList<Models.ComboCandidate>();

        if (rawForProbability == null
                || rawForProbability.size() == 0
                || probabilityEngine == null) {
            return out;
        }

        if (probabilityCap <= 0) {
            probabilityCap = REALTIME_PROBABILITY_ENRICH_CAP;
        }

        if (decisionCap <= 0) {
            decisionCap = REALTIME_DECISION_CANDIDATE_CAP;
        }

        try {
            Method m =
                    probabilityEngine
                            .getClass()
                            .getDeclaredMethod(
                                    "enrichAllWithMode",
                                    new Class[] {
                                        DbStore.class,
                                        List.class,
                                        List.class,
                                        List.class,
                                        List.class,
                                        String.class,
                                        Integer.TYPE,
                                        Integer.TYPE,
                                        Boolean.TYPE,
                                        Boolean.TYPE
                                    });

            m.setAccessible(true);

            Object r =
                    m.invoke(
                            probabilityEngine,
                            new Object[] {
                                store,
                                rawForProbability,
                                clean,
                                traces,
                                recoveries,
                                AppConfig.BACKTEST_MODE_FAST,
                                Integer.valueOf(probabilityCap),
                                Integer.valueOf(decisionCap),
                                Boolean.TRUE,
                                Boolean.TRUE
                            });

            if (r instanceof ArrayList) {
                out = (ArrayList<Models.ComboCandidate>) r;
            } else if (r instanceof List) {
                List list = (List) r;
                for (int i = 0; i < list.size(); i++) {
                    Object item = list.get(i);
                    if (item instanceof Models.ComboCandidate) {
                        out.add((Models.ComboCandidate) item);
                    }
                }
            }

            if (store != null) {
                store.setState("probability_enrich_entry", "reflect_enrichAllWithMode");
                store.setState("probability_enrich_requested_cap", String.valueOf(probabilityCap));
                store.setState("probability_enrich_output_count", String.valueOf(out.size()));
            }

            if (out.size() > 0) {
                return out;
            }
        } catch (Throwable e) {
            try {
                if (store != null) {
                    store.setState("probability_enrich_entry", "fallback_enrichAllRealtime");
                    store.setState("probability_enrich_reflect_error", safeError(e));
                }
            } catch (Throwable ignore) {
            }
        }

        try {
            out =
                    probabilityEngine.enrichAllRealtime(
                            store, rawForProbability, clean, traces, recoveries);

            if (out == null) {
                out = new ArrayList<Models.ComboCandidate>();
            }

            if (store != null) {
                store.setState("probability_enrich_fallback_count", String.valueOf(out.size()));
            }
        } catch (Throwable e) {
            out = new ArrayList<Models.ComboCandidate>();

            try {
                if (store != null) {
                    store.setState("probability_enrich_fallback_error", safeError(e));
                }
            } catch (Throwable ignore) {
            }
        }

        return out;
    }

    private ArrayList<Models.ComboCandidate> capCandidatesByScore(
            List<Models.ComboCandidate> list, int cap) {
        ArrayList<Models.ComboCandidate> out = new ArrayList<Models.ComboCandidate>();

        if (list == null || list.size() == 0 || cap <= 0) {
            return out;
        }

        for (int i = 0; i < list.size(); i++) {
            Models.ComboCandidate c = list.get(i);

            if (c == null || !c.isValidK3()) {
                continue;
            }

            normalizeCandidateForMainlineSort(c);

            double score = mainlineCandidateScore(c);

            if (out.size() == 0) {
                out.add(c);
            } else {
                int insertAt = out.size();

                for (int j = 0; j < out.size(); j++) {
                    Models.ComboCandidate old = out.get(j);
                    double oldScore = old == null ? -999999.0d : mainlineCandidateScore(old);

                    if (score > oldScore) {
                        insertAt = j;
                        break;
                    }
                }

                out.add(insertAt, c);
            }

            if (out.size() > cap) {
                out.remove(out.size() - 1);
            }
        }

        for (int i = 0; i < out.size(); i++) {
            Models.ComboCandidate c = out.get(i);
            if (c != null) {
                c.rank = i + 1;
            }
        }

        return out;
    }

    private void normalizeCandidateForMainlineSort(Models.ComboCandidate c) {
        if (c == null) {
            return;
        }

        if (c.k3Mask <= 0
                && AppConfig.isValidNumber(c.n1)
                && AppConfig.isValidNumber(c.n2)
                && AppConfig.isValidNumber(c.n3)) {
            try {
                c.k3Mask = Models.buildK3Mask(c.n1, c.n2, c.n3);
            } catch (Throwable e) {
            }
        }

        if (c.dynamicGate <= 0.0d) {
            c.dynamicGate = AppConfig.DEFAULT_DYNAMIC_GATE;
        }

        if (c.finalP <= 0.0d) {
            if (c.rawP > 0.0d) {
                c.finalP = c.rawP;
            } else {
                c.finalP =
                        c.modelP * 0.38d
                                + c.shapeP * 0.22d
                                + c.comboP * 0.24d
                                + c.recoveryP * 0.10d
                                + c.similarP * 0.03d
                                + c.memoryP * 0.03d;
            }

            if (c.finalP <= 0.0d) {
                c.finalP = AppConfig.RANDOM_K3_BASELINE;
            }
        }

        c.finalP = AppConfig.clampProbability(c.finalP);

        if (c.rawP <= 0.0d) {
            c.rawP = c.finalP;
        }

        c.rawP = AppConfig.clampProbability(c.rawP);
        c.edge = c.finalP - c.dynamicGate;
    }

    private double mainlineCandidateScore(Models.ComboCandidate c) {
        if (c == null) {
            return -999999.0d;
        }

        double score = 0.0d;

        score += c.finalP * 1.00d;
        score += c.rawP * 0.22d;
        score += c.edge * 0.45d;
        score += c.championGap * 0.08d;
        score += c.comboGap * 0.12d;
        score += c.trendLife / 100.0d * 0.012d;
        score += c.recoveryScore / 100.0d * 0.010d;

        score -= c.driftRisk / 100.0d * 0.016d;
        score -= c.breakDepth / 100.0d * 0.014d;
        score -= c.hotReverseRisk / 100.0d * 0.008d;
        score -= c.falseStrongRisk / 100.0d * 0.008d;

        if (AppConfig.MODE_BROKEN.equals(c.stateMode)
                || AppConfig.MODE_REVERSING.equals(c.stateMode)) {
            score -= 0.050d;
        } else if (AppConfig.MODE_CHAOS.equals(c.stateMode)) {
            score -= 0.018d;
        } else if (AppConfig.MODE_ACCELERATING.equals(c.stateMode)
                || AppConfig.MODE_CONFIRMED.equals(c.stateMode)) {
            score += 0.006d;
        }

        if (c.blockCode != null && c.blockCode.length() > 0) {
            if (AppConfig.BLOCK_DRIFT_HIGH.equals(c.blockCode)
                    || AppConfig.BLOCK_BREAK_HIGH.equals(c.blockCode)
                    || AppConfig.BLOCK_DATA_ERROR.equals(c.blockCode)) {
                score -= 0.060d;
            } else {
                score -= 0.012d;
            }
        }

        String tm = readTeacherStatus(c);
        if ("RISK".equals(tm)) {
            score -= 0.020d;
        } else if ("READY".equals(tm)) {
            score += 0.006d;
        }

        score += teacherMatrixSortBoost(c);

        double causeScore =
                maxDoubleFields(
                        c,
                        new String[] {
                            "dynamicCauseScore", "causeChainScore", "causeScore", "finalCauseScore"
                        });

        double ruleHealth =
                maxDoubleFields(c, new String[] {"ruleHealthScore", "healthScore", "ruleScore"});

        double thirdGuard =
                maxDoubleFields(
                        c, new String[] {"thirdGuardScore", "thirdGuardHealth", "guardScore"});

        double falseRisk =
                maxDoubleFields(
                        c, new String[] {"falseStrongRisk", "freezeRisk", "falseStrongScore"});

        double shadowHealth = maxDoubleFields(c, new String[] {"shadowHealthScore", "shadowScore"});

        score += causeScore * 0.018d;
        score += ruleHealth * 0.012d;
        score += thirdGuard * 0.010d;
        score += shadowHealth * 0.006d;
        score -= falseRisk * 0.018d;

        return score;
    }

    private double teacherMatrixSortBoost(Models.ComboCandidate c) {
        if (c == null) {
            return 0.0d;
        }

        double explicit = readDouble(c, "teacherSortBoost", 0.0d);
        if (explicit != 0.0d) {
            if (explicit > 0.025d) {
                explicit = 0.025d;
            }
            if (explicit < -0.030d) {
                explicit = -0.030d;
            }
            return explicit;
        }

        int sample = readTeacherSample(c);
        double hit = readTeacherHitRate(c);
        double conf = readTeacherConfidence(c);
        double risk = readTeacherRisk(c);
        double score = readTeacherScore(c);
        String status = readTeacherStatus(c);

        return teacherBridgeSortBoost(status, sample, hit, conf, risk, score);
    }

    /*
     * V21.2：候选已经完整算出时，软元数据/构建器警告不能把整期变成FinalP=0的HOLD。
     * 这里绝不救援BET，只把可恢复的HOLD转成候选背书的SHADOW，保证字段、结算和
     * Teacher/Dynamic反事实样本持续积累。真正包名错、未来泄漏、号码非法、重复Trade仍不恢复。
     */
    private Models.Decision recoverCandidateBackedShadowIfSoftHold(
            Models.Decision decision,
            ArrayList<Models.ComboCandidate> candidates,
            List<Models.Draw> history,
            String baseIssue,
            String targetIssue,
            String stage) {
        if (!AppConfig.SOFT_DATA_ERROR_RECOVER_TO_SHADOW || decision == null) return decision;
        String normalized = AppConfig.normalizeDecision(decision.decision);
        if (AppConfig.DECISION_BET.equals(normalized)) return decision;
        if (AppConfig.DECISION_SHADOW.equals(normalized)
                && decision.position >= 1
                && decision.position <= 10
                && AppConfig.isValidK3Numbers(decision.n1, decision.n2, decision.n3))
            return decision;
        if (candidates == null || candidates.size() == 0) return decision;

        String identity = store == null ? "" : store.getState(BuildIdentity.STATE_STATUS, "");
        if (!BuildIdentity.isBetCompatible(identity)) {
            try {
                if (store != null)
                    store.setState("v48_identity_shadow_recovery", "DIAGNOSTIC_ONLY|" + identity);
            } catch (Throwable ignore) {
            }
        }
        String reason = safe(decision.reasonSummary) + "|" + safe(decision.reasonText);
        String upper = reason.toUpperCase(java.util.Locale.ROOT);
        if (upper.indexOf("FUTURE_LEAK") >= 0
                || upper.indexOf("未来泄漏") >= 0
                || upper.indexOf("未结算真实BET") >= 0
                || upper.indexOf("OPEN_TRADE") >= 0
                || upper.indexOf("号码非法") >= 0
                || upper.indexOf("期号缺失") >= 0
                || upper.indexOf("BUILD_IDENTITY_BLOCK") >= 0) return decision;

        Models.Draw latest = null;
        if (history != null && history.size() > 0) latest = history.get(history.size() - 1);
        Models.DataCheck latestCheck = dataGuard.checkLatest(latest);
        if (latestCheck == null || !latestCheck.ok) return decision;

        Models.ComboCandidate best = null;
        double bestScore = -999.0d;
        for (int i = 0; i < candidates.size(); i++) {
            Models.ComboCandidate c = candidates.get(i);
            if (c == null || !c.isValidK3()) continue;
            String hard = readString(c, "hardRiskCode", "");
            if (readInt(c, "hardBlockFlag", 0) == 1 || AppConfig.isNonOverridableFloorBlock(hard))
                continue;
            double score =
                    c.finalP * 1000.0d
                            + c.rawP * 200.0d
                            + readDouble(
                                            c,
                                            "coverageQuality",
                                            readDouble(c, "coverageQualityScore", 0.0d))
                                    * 20.0d
                            + readDouble(
                                            c,
                                            "thirdQuality",
                                            readDouble(c, "thirdNumberQualityScore", 0.0d))
                                    * 10.0d
                            - readDouble(
                                            c,
                                            "counterRisk",
                                            readDouble(c, "counterEvidenceScore", 0.0d))
                                    * 12.0d;
            if (best == null || score > bestScore) {
                best = c;
                bestScore = score;
            }
        }
        if (best == null) return decision;

        best.baseIssue = baseIssue == null ? "" : baseIssue;
        best.targetIssue = targetIssue == null ? "" : targetIssue;
        Models.Decision shadow =
                dataGuard.shadowDecisionFromCandidate(
                        best,
                        "软HOLD已恢复为候选背书SHADOW：不创建真实Trade、不计真实频率/盈亏；"
                                + "保留完整FinalP/Teacher/Dynamic/域键供开奖后反事实学习｜原阻断="
                                + safe(decision.blockCode)
                                + "｜阶段="
                                + safe(stage));
        shadow.baseIssue = best.baseIssue;
        shadow.targetIssue = best.targetIssue;
        shadow.blockCode = "SOFT_HOLD_RECOVERED_TO_SHADOW";
        shadow.dataOk = 1;
        copyClosedLoopFields(best, shadow);
        syncTeacherAliasFields(shadow);
        syncClosedLoopAliases(shadow);
        try {
            if (store != null) {
                store.setState("soft_hold_shadow_recovery", "RECOVERED");
                store.setState("soft_hold_shadow_recovery_stage", safe(stage));
                store.setState("soft_hold_shadow_recovery_target", safe(targetIssue));
                store.setState(
                        "soft_hold_shadow_recovery_original_block", safe(decision.blockCode));
                store.setState(
                        "soft_hold_shadow_recovery_version",
                        AppConfig.ACTIVE_QUALITY_BUILD_VERSION);
            }
        } catch (Throwable ignore) {
        }
        return shadow;
    }

    private Models.Decision syncDecisionClosedLoopFieldsFromCandidates(
            Models.Decision decision, ArrayList<Models.ComboCandidate> candidates, String stage) {
        if (decision == null) {
            return null;
        }
        syncClosedLoopAliases(decision);
        if (candidates == null || candidates.size() == 0) {
            try {
                if (store != null) {
                    store.setState("field_chain_candidate_match", "NO_CANDIDATES");
                    store.setState("field_chain_candidate_stage", stage == null ? "" : stage);
                }
            } catch (Throwable e) {
            }
            return decision;
        }

        Models.ComboCandidate best = findMatchingClosedLoopCandidate(decision, candidates);
        if (best != null) {
            copyClosedLoopFields(best, decision);
            syncTeacherAliasFields(decision);
            syncClosedLoopAliases(decision);
            try {
                if (store != null) {
                    store.setState("field_chain_candidate_match", "MATCHED");
                    store.setState("field_chain_candidate_stage", stage == null ? "" : stage);
                    store.setState(
                            "field_chain_candidate_target",
                            decision.targetIssue == null ? "" : decision.targetIssue);
                    store.setState(
                            "field_chain_candidate_summary", buildClosedLoopFieldSummary(decision));
                    store.setState("field_chain_patch_version", V20_FIELD_CHAIN_PATCH_VERSION);
                }
            } catch (Throwable e) {
            }
        } else {
            try {
                if (store != null) {
                    store.setState("field_chain_candidate_match", "NO_MATCH");
                    store.setState("field_chain_candidate_stage", stage == null ? "" : stage);
                    store.setState(
                            "field_chain_candidate_target",
                            decision.targetIssue == null ? "" : decision.targetIssue);
                }
            } catch (Throwable e) {
            }
        }
        return decision;
    }

    private Models.ComboCandidate findMatchingClosedLoopCandidate(
            Models.Decision d, ArrayList<Models.ComboCandidate> candidates) {
        if (d == null || candidates == null || candidates.size() == 0) {
            return null;
        }
        repairDecisionK3(d);
        for (int i = 0; i < candidates.size(); i++) {
            Models.ComboCandidate c = candidates.get(i);
            if (c == null) continue;
            if (sameText(c.targetIssue, d.targetIssue)
                    && c.position == d.position
                    && sameK3Numbers(c, d)) {
                return c;
            }
            if (c.position == d.position && sameK3Numbers(c, d)) {
                return c;
            }
        }
        return null;
    }

    private boolean sameK3Numbers(Object a, Object b) {
        if (a == null || b == null) {
            return false;
        }
        int am = readInt(a, "k3Mask", 0);
        int bm = readInt(b, "k3Mask", 0);
        if (am > 0 && bm > 0 && am == bm) {
            return true;
        }
        int a1 = readInt(a, "n1", 0);
        int a2 = readInt(a, "n2", 0);
        int a3 = readInt(a, "n3", 0);
        int b1 = readInt(b, "n1", 0);
        int b2 = readInt(b, "n2", 0);
        int b3 = readInt(b, "n3", 0);
        int ma = AppConfig.buildK3Mask(a1, a2, a3);
        int mb = AppConfig.buildK3Mask(b1, b2, b3);
        return ma > 0 && ma == mb;
    }

    private Models.Decision enforceAutoFetchFinalBetSafety(Models.Decision decision, String stage) {
        if (decision == null) return null;
        syncTeacherAliasFields(decision);
        syncClosedLoopAliases(decision);
        repairDecisionK3(decision);
        normalizeDecisionScope(decision);
        if (AppConfig.DECISION_BET.equals(decision.decision)) {
            String audit = autoFetchFinalBetBlockCode(decision);
            try {
                if (store != null) {
                    store.setState("autofetch_post_gate_audit_stage", safe(stage));
                    store.setState("autofetch_post_gate_audit_code", audit == null ? "" : audit);
                    store.setState("autofetch_post_gate_policy", "AUDIT_ONLY_DECISION_GATE_UNIQUE");
                }
            } catch (Throwable ignore) {
            }
            /* DecisionGateEngine是唯一动作写入者；AutoFetch不得把BET二次降级为SHADOW。 */
        }
        return decision;
    }

    private String autoFetchFinalBetBlockCode(Models.Decision d) {
        if (d == null || !AppConfig.DECISION_BET.equals(d.decision)) {
            return "";
        }
        syncClosedLoopAliases(d);
        repairDecisionK3(d);

        /*
         * FIX05 主链闭环原则：AutoFetchService 只做最终字段安全校验，
         * 不能把 Teacher 无样本、V20 WATCH、Adaptive 缺报告当成硬杀。
         * 真正是否 BET 仍由 DecisionGateEngine 唯一裁决；这里只拦截
         * 负 Edge、硬风险、冻结、假高 P、覆盖硬风险、第三号硬弱等明显危险字段。
         */
        String reject = readString(d, "finalDecisionRejectCode", "");
        String teacherPolicy =
                readFirstString(
                        d,
                        new String[] {
                            "teacherOrdinaryBetPolicy",
                            "teacherMatrixOrdinaryBetPolicy",
                            "finalDecisionTeacherPolicy"
                        });
        String teacherStatus =
                readFirstString(
                        d,
                        new String[] {
                            "teacherMatrixStatus", "teacherNumberStatus", "teacherStatus"
                        });
        String v20Status =
                readFirstString(
                        d,
                        new String[] {
                            "v20Status",
                            "dynamicStatus",
                            "dynamicCauseStatus",
                            "finalDecisionV20Policy"
                        });
        String adaptiveStatus =
                readFirstString(
                        d,
                        new String[] {
                            "adaptiveStatus", "adaptiveJudgeAction", "adaptiveJudgeStatus"
                        });
        String promotionStatus =
                readFirstString(
                        d,
                        new String[] {
                            "shadowPromotionStatus", "promotionClusterStatus", "promotionStatus"
                        });
        String edgePolicy =
                readFirstString(
                        d, new String[] {"dynamicGateEdgePolicy", "finalDecisionEdgePolicy"});

        double coverageRisk =
                maxDoubleFields(
                        d,
                        new String[] {
                            "k3CoverageRisk", "dynamicGateCoverageRisk", "finalDecisionCoverageRisk"
                        });
        double coverageQuality =
                maxDoubleFields(
                        d,
                        new String[] {
                            "coverageQuality",
                            "coverageQualityScore",
                            "dynamicGateCoverageQualityScore",
                            "finalDecisionCoverageQuality"
                        });
        double thirdQuality =
                maxDoubleFields(
                        d,
                        new String[] {
                            "thirdQuality",
                            "thirdNumberQualityScore",
                            "dynamicGateThirdNumberQuality",
                            "finalDecisionThirdNumberQuality"
                        });
        int falseHigh =
                maxIntFields(
                        d,
                        new String[] {
                            "falseHighPFlag",
                            "probabilityFalseHighPFlag",
                            "dynamicGateFalseHighPFlag",
                            "finalDecisionFalseHighPFlag"
                        });
        double edge = safeDecisionEdge(d);
        boolean strongOverride = autoFetchStrongOverride(d);

        if (edge < AppConfig.MIN_EDGE_FOR_BET) {
            return "AUTO_FETCH_NEGATIVE_EDGE";
        }
        if (containsText(reject, "FREEZE")
                || containsText(reject, "HARD_RISK")
                || containsText(reject, "RISK_HARD")
                || containsText(reject, "ILLEGAL")
                || containsText(reject, "NEGATIVE_EDGE")) {
            return "AUTO_FETCH_FINAL_HARD_REJECT_CODE";
        }
        if (containsText(teacherPolicy, "RISK")
                || containsText(teacherPolicy, "FREEZE")
                || containsText(teacherStatus, "RISK")
                || containsText(teacherStatus, "FREEZE")) {
            return "AUTO_FETCH_TEACHER_HARD_RISK";
        }
        if (containsText(v20Status, "HARD_RISK")
                || containsText(v20Status, "FREEZE")
                || containsText(v20Status, "FROZEN")) {
            return "AUTO_FETCH_V20_HARD_RISK";
        }
        if (containsText(adaptiveStatus, "FREEZE") || containsText(adaptiveStatus, "HARD_RISK")) {
            return "AUTO_FETCH_ADAPTIVE_HARD_RISK";
        }
        if (containsText(promotionStatus, "FREEZE") || containsText(promotionStatus, "HARD_RISK")) {
            return "AUTO_FETCH_PROMOTION_HARD_RISK";
        }
        if (falseHigh > 0 && (!strongOverride || d.finalP < 0.425d)) {
            return "AUTO_FETCH_FALSE_HIGH_P";
        }
        if (coverageRisk >= FIELD_CHAIN_K3_COVERAGE_HARD_RISK && d.finalP < 0.430d) {
            return "AUTO_FETCH_K3_COVERAGE_HARD_RISK";
        }
        if (coverageQuality > 0.0d
                && coverageQuality < AppConfig.REAL_BET_PROBE_MIN_COVERAGE
                && !strongOverride) {
            return "AUTO_FETCH_COVER_QUALITY_LOW";
        }
        if (thirdQuality > 0.0d
                && thirdQuality < AppConfig.REAL_BET_PROBE_MIN_THIRD
                && !strongOverride) {
            return "AUTO_FETCH_THIRD_NUMBER_WEAK";
        }
        if ((containsText(edgePolicy, "虚强") || containsText(edgePolicy, "FAKE"))
                && !strongOverride) {
            return "AUTO_FETCH_EDGE_POLICY_FAKE";
        }
        return "";
    }

    private boolean autoFetchStrongOverride(Models.Decision d) {
        if (d == null) {
            return false;
        }
        if (maxIntFields(
                        d,
                        new String[] {
                            "probabilityStrongCauseFlag",
                            "decisionEvidenceStrong",
                            "decisionEvidenceElite",
                            "teacherMatrixSupportsBet",
                            "teacherMatrixSupportsFinalGate"
                        })
                > 0) {
            return maxDoubleFields(d, new String[] {"k3CoverageRisk", "dynamicGateCoverageRisk"})
                    < FIELD_CHAIN_K3_COVERAGE_HARD_RISK;
        }
        String teacher =
                readFirstString(
                        d,
                        new String[] {
                            "teacherMatrixStatus", "teacherNumberStatus", "teacherStatus"
                        });
        String v20 =
                readFirstString(
                        d,
                        new String[] {
                            "v20Status",
                            "dynamicStatus",
                            "dynamicCauseStatus",
                            "finalDecisionV20Policy"
                        });
        if (containsStrongReady(teacher) && containsStrongReady(v20)) {
            return true;
        }
        return false;
    }

    private boolean autoFetchPreferredDomain(Models.Decision d) {
        if (d == null) {
            return false;
        }
        if (maxIntFields(
                        d,
                        new String[] {
                            "dynamicGatePreferredEdgeBand", "probabilityPreferredEdgeBand"
                        })
                > 0) {
            return maxDoubleFields(d, new String[] {"k3CoverageRisk", "dynamicGateCoverageRisk"})
                    < 0.50d;
        }
        double edge = safeDecisionEdge(d);
        return d.finalP >= 0.380d
                && d.finalP <= 0.400d
                && edge >= 0.010d
                && edge <= 0.020d
                && maxDoubleFields(d, new String[] {"k3CoverageRisk", "dynamicGateCoverageRisk"})
                        < 0.50d
                && maxDoubleFields(
                                d,
                                new String[] {
                                    "thirdNumberQualityScore", "dynamicGateThirdNumberQuality"
                                })
                        >= FIELD_CHAIN_THIRD_NUMBER_MIN;
    }

    private String buildAutoFetchFinalBetBlockReason(Models.Decision d, String block) {
        StringBuilder sb = new StringBuilder();
        sb.append("code=").append(block == null ? "" : block);
        sb.append("；P=").append(d == null ? "" : UiFormatter.probability(d.finalP));
        sb.append("；Gate=").append(d == null ? "" : UiFormatter.probability(d.dynamicGate));
        sb.append("；Edge=")
                .append(d == null ? "" : UiFormatter.signedProbability(safeDecisionEdge(d)));
        sb.append("；Teacher=")
                .append(
                        readFirstString(
                                d,
                                new String[] {
                                    "teacherMatrixStatus", "teacherNumberStatus", "teacherStatus"
                                }));
        sb.append("；TeacherPolicy=")
                .append(
                        readFirstString(
                                d,
                                new String[] {
                                    "teacherOrdinaryBetPolicy", "teacherMatrixOrdinaryBetPolicy"
                                }));
        sb.append("；V20=")
                .append(
                        readFirstString(
                                d,
                                new String[] {"v20Status", "dynamicStatus", "dynamicCauseStatus"}));
        sb.append("；Window=")
                .append(
                        readFirstString(
                                d,
                                new String[] {
                                    "windowStateKey",
                                    "finalDecisionWindowStateKey",
                                    "dynamicWindowStateKey"
                                }));
        sb.append("；Cover=")
                .append(
                        UiFormatter.probability(
                                maxDoubleFields(
                                        d,
                                        new String[] {
                                            "coverageQualityScore",
                                            "dynamicGateCoverageQualityScore"
                                        })));
        sb.append("/")
                .append(
                        UiFormatter.probability(
                                maxDoubleFields(
                                        d,
                                        new String[] {
                                            "k3CoverageRisk", "dynamicGateCoverageRisk"
                                        })));
        sb.append("；Third=")
                .append(
                        UiFormatter.probability(
                                maxDoubleFields(
                                        d,
                                        new String[] {
                                            "thirdNumberQualityScore",
                                            "dynamicGateThirdNumberQuality"
                                        })));
        sb.append("；Reject=").append(readString(d, "finalDecisionRejectCode", ""));
        return sb.toString();
    }

    private void copyClosedLoopFields(Object from, Object to) {
        if (from == null || to == null) {
            return;
        }
        String[] names = closedLoopFieldNames();
        for (int i = 0; i < names.length; i++) {
            copyFieldByReflection(from, to, names[i]);
        }
        syncClosedLoopAliases(to);
    }

    private void copyFieldByReflection(Object from, Object to, String name) {
        if (from == null || to == null || name == null || name.length() == 0) {
            return;
        }
        try {
            Field f = from.getClass().getField(name);
            Object v = f.get(from);
            if (v == null) {
                return;
            }
            Field t = to.getClass().getField(name);
            Class type = t.getType();
            if (type == String.class) {
                t.set(to, String.valueOf(v));
            } else if (v instanceof Number) {
                Number n = (Number) v;
                if (type == Integer.TYPE || type == Integer.class) {
                    t.set(to, Integer.valueOf(n.intValue()));
                } else if (type == Long.TYPE || type == Long.class) {
                    t.set(to, Long.valueOf(n.longValue()));
                } else if (type == Double.TYPE || type == Double.class) {
                    t.set(to, Double.valueOf(n.doubleValue()));
                } else if (type == Float.TYPE || type == Float.class) {
                    t.set(to, Float.valueOf(n.floatValue()));
                }
            }
        } catch (Throwable e) {
        }
    }

    private String[] closedLoopFieldNames() {
        return new String[] {
            "teacherWeakNoBetFlag",
            "teacherMatrixWeakNoBetFlag",
            "teacherMatrixOrdinaryBetBlockFlag",
            "teacherMatrixSupportsFinalGate",
            "teacherMatrixSupportsBet",
            "teacherOrdinaryBetPolicy",
            "teacherMatrixOrdinaryBetPolicy",
            "teacherFieldSource",
            "teacherFieldVersion",
            "teacherMatrixStatus",
            "teacherNumberStatus",
            "teacherStatus",
            "teacherEvidenceContribution",
            "teacherMatrixEvidenceContribution",
            "teacherCounterContribution",
            "teacherMatrixCounterContribution",
            "teacherSampleReliability",
            "teacherRecentValidity",
            "teacherMatrixSample",
            "teacherNumberSample",
            "teacherSample",
            "teacherMatrixHitRate",
            "teacherNumberHitRate",
            "teacherHitRate",
            "teacherMatrixRecentHitRate",
            "teacherMatrixConfidence",
            "teacherNumberConfidence",
            "teacherConfidence",
            "teacherMatrixScore",
            "teacherNumberScore",
            "teacherScore",
            "teacherMatrixRisk",
            "teacherNumberRisk",
            "teacherRisk",
            "teacherAsOfIssue",
            "teacherIssueLag",
            "teacherFreshByIssue",
            "teacherSnapshotCreatedAt",
            "teacherMatrixSummary",
            "teacherNumberSummary",
            "teacherSummary",
            "v20Status",
            "dynamicStatus",
            "dynamicCauseStatus",
            "v20SupportScore",
            "v20RiskScore",
            "v20EvidenceContribution",
            "v20CounterContribution",
            "v20SampleReliability",
            "v20RecentValidity",
            "v20ActionAdvice",
            "v20WindowStateKey",
            "dynamicClusterId",
            "dynamicClusterStatus",
            "dynamicClusterSample",
            "dynamicClusterHitCount",
            "dynamicClusterMissCount",
            "dynamicClusterRecentSample",
            "dynamicClusterRecentHitCount",
            "dynamicClusterHitRate",
            "dynamicClusterRecentHitRate",
            "dynamicClusterConfidence",
            "dynamicClusterCounterScore",
            "dynamicClusterReason",
            "dynamicClusterVersion",
            "learningClusterKey",
            "auditClusterKey",
            "positionNeedScore",
            "numberSupplyScore",
            "needSupplyMatchScore",
            "needSupplyScore",
            "counterfactualRisk",
            "counterfactualWhyNotRisk",
            "counterEvidenceScore",
            "shadowPromotionStatus",
            "promotionStatus",
            "promotionClusterStatus",
            "promotionClusterSample",
            "promotionClusterHitRate",
            "promotionClusterRecentHitRate",
            "promotionWilsonLowerBound",
            "promotionCoarseDomain",
            "promotionMidDomain",
            "promotionFineDomain",
            "windowStateKey",
            "finalDecisionWindowStateKey",
            "dynamicWindowStateKey",
            "probabilityWindowStateKey",
            "coverageQualityScore",
            "thirdNumberQualityScore",
            "k3CoverageRisk",
            "falseHighPFlag",
            "probabilityFalseHighPFlag",
            "probabilityFalseHighRisk",
            "probabilityPreferredEdgeBand",
            "probabilityHighEdgeFlag",
            "probabilityPreferredPBand",
            "probabilityTeacherWeakOrEmpty",
            "probabilityV20UnknownOrEmpty",
            "probabilityStrongCauseFlag",
            "coverageProbDiscount",
            "coverageProbBoost",
            "coverageK3HitRate32",
            "coverageK3HitRate64",
            "coverageK3HitRate128",
            "coverageNumberRate1",
            "coverageNumberRate2",
            "coverageNumberRate3",
            "finalPBeforeCoverage",
            "finalPAfterCoverage",
            "coverageAdjustedP",
            "probabilityCoverageSummary",
            "coverageQualitySummary",
            "probabilityCoverageActionAdvice",
            "probabilityActionAdvice",
            "probabilityFieldVersion",
            "dynamicGateQualityStatus",
            "dynamicGateFalseHighPFlag",
            "dynamicGatePreferredEdgeBand",
            "dynamicGateTeacherV20WeakFlag",
            "dynamicGateEdgePolicy",
            "dynamicGateCoverageRisk",
            "dynamicGateCoverageQualityScore",
            "dynamicGateThirdNumberQuality",
            "dynamicGateVirtualEdgeRisk",
            "dynamicGateActionAdvice",
            "dynamicGateFieldVersion",
            "dynamicGateVersion",
            "finalDecisionVersion",
            "finalDecisionEdgePolicy",
            "finalDecisionTeacherPolicy",
            "finalDecisionV20Policy",
            "finalDecisionBetDomain",
            "finalDecisionRejectCode",
            "finalDecisionShadowReason",
            "finalDecisionStrongOverrideTrace",
            "finalDecisionCoverageRisk",
            "finalDecisionCoverageQuality",
            "finalDecisionThirdNumberQuality",
            "finalDecisionFalseHighPFlag"
        };
    }

    private void syncClosedLoopAliases(Object obj) {
        if (obj == null) {
            return;
        }
        if (readString(obj, "teacherMatrixStatus", "").length() == 0) {
            writeString(
                    obj,
                    "teacherMatrixStatus",
                    readString(obj, "teacherNumberStatus", readString(obj, "teacherStatus", "")));
        }
        if (readString(obj, "teacherStatus", "").length() == 0) {
            writeString(
                    obj,
                    "teacherStatus",
                    readString(
                            obj,
                            "teacherMatrixStatus",
                            readString(obj, "teacherNumberStatus", "")));
        }
        if (readString(obj, "v20Status", "").length() == 0) {
            writeString(
                    obj,
                    "v20Status",
                    readString(obj, "dynamicStatus", readString(obj, "dynamicCauseStatus", "")));
        }
        if (readString(obj, "dynamicStatus", "").length() == 0) {
            writeString(
                    obj,
                    "dynamicStatus",
                    readString(obj, "v20Status", readString(obj, "dynamicCauseStatus", "")));
        }
        if (readString(obj, "windowStateKey", "").length() == 0) {
            writeString(
                    obj,
                    "windowStateKey",
                    readString(
                            obj,
                            "finalDecisionWindowStateKey",
                            readString(
                                    obj,
                                    "dynamicWindowStateKey",
                                    readString(obj, "probabilityWindowStateKey", ""))));
        }
        double cq =
                maxDoubleFields(
                        obj,
                        new String[] {
                            "coverageQualityScore",
                            "dynamicGateCoverageQualityScore",
                            "finalDecisionCoverageQuality"
                        });
        if (cq > 0.0d) {
            writeDouble(obj, "coverageQualityScore", cq);
            writeDouble(obj, "dynamicGateCoverageQualityScore", cq);
            writeDouble(obj, "finalDecisionCoverageQuality", cq);
        }
        double cr =
                maxDoubleFields(
                        obj,
                        new String[] {
                            "k3CoverageRisk", "dynamicGateCoverageRisk", "finalDecisionCoverageRisk"
                        });
        if (cr > 0.0d) {
            writeDouble(obj, "k3CoverageRisk", cr);
            writeDouble(obj, "dynamicGateCoverageRisk", cr);
        }
        double tq =
                maxDoubleFields(
                        obj,
                        new String[] {
                            "thirdNumberQualityScore",
                            "dynamicGateThirdNumberQuality",
                            "finalDecisionThirdNumberQuality"
                        });
        if (tq > 0.0d) {
            writeDouble(obj, "thirdNumberQualityScore", tq);
            writeDouble(obj, "dynamicGateThirdNumberQuality", tq);
        }
        int fh =
                maxIntFields(
                        obj,
                        new String[] {
                            "falseHighPFlag",
                            "probabilityFalseHighPFlag",
                            "dynamicGateFalseHighPFlag",
                            "finalDecisionFalseHighPFlag"
                        });
        if (fh > 0) {
            writeInt(obj, "falseHighPFlag", fh);
            writeInt(obj, "dynamicGateFalseHighPFlag", fh);
        }
    }

    private void repairTradeClosedLoopFieldsByTarget(Models.Decision decision, String reason) {
        if (store == null
                || decision == null
                || decision.targetIssue == null
                || decision.targetIssue.length() == 0) {
            return;
        }
        try {
            Models.Trade t = store.getTradeByTargetIssue(decision.targetIssue);
            if (t != null) {
                repairExistingTradeClosedLoopFields(t, decision);
                store.setState("field_chain_trade_repair_reason", reason == null ? "" : reason);
            }
        } catch (Throwable e) {
        }
    }

    private void repairExistingTradeClosedLoopFields(Models.Trade trade, Models.Decision decision) {
        if(store==null || trade==null || decision==null)return;
        store.setState("field_chain_trade_repair","READ_ONLY_CANONICAL_FIELDS_IMMUTABLE");
        store.setState("field_chain_trade_repair_target",decision.targetIssue==null?"":decision.targetIssue);
        // Deliberately no reflective replaceTradeFull/updateTradeFull/updateTrade call.
    }

    private String buildClosedLoopFieldSummary(Object obj) {
        if (obj == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Teacher=")
                .append(
                        readFirstString(
                                obj,
                                new String[] {
                                    "teacherMatrixStatus", "teacherNumberStatus", "teacherStatus"
                                }));
        sb.append(" Policy=")
                .append(
                        readFirstString(
                                obj,
                                new String[] {
                                    "teacherOrdinaryBetPolicy", "teacherMatrixOrdinaryBetPolicy"
                                }));
        sb.append(" V20=")
                .append(
                        readFirstString(
                                obj,
                                new String[] {"v20Status", "dynamicStatus", "dynamicCauseStatus"}));
        sb.append(" Window=")
                .append(
                        readFirstString(
                                obj,
                                new String[] {
                                    "windowStateKey",
                                    "finalDecisionWindowStateKey",
                                    "dynamicWindowStateKey"
                                }));
        sb.append(" Cover=")
                .append(
                        UiFormatter.probability(
                                maxDoubleFields(
                                        obj,
                                        new String[] {
                                            "coverageQualityScore",
                                            "dynamicGateCoverageQualityScore"
                                        })));
        sb.append("/")
                .append(
                        UiFormatter.probability(
                                maxDoubleFields(
                                        obj,
                                        new String[] {
                                            "k3CoverageRisk", "dynamicGateCoverageRisk"
                                        })));
        sb.append(" Third=")
                .append(
                        UiFormatter.probability(
                                maxDoubleFields(
                                        obj,
                                        new String[] {
                                            "thirdNumberQualityScore",
                                            "dynamicGateThirdNumberQuality"
                                        })));
        sb.append(" Reject=").append(readString(obj, "finalDecisionRejectCode", ""));
        return sb.toString();
    }

    private double safeDecisionEdge(Models.Decision d) {
        if (d == null) {
            return 0.0d;
        }
        double edge = d.edge;
        if (Double.isNaN(edge) || Double.isInfinite(edge) || Math.abs(edge) > 1.0d) {
            edge = d.finalP - d.dynamicGate;
        }
        return edge;
    }

    private boolean teacherWeakOrEmptyText(String text) {
        if (text == null || text.length() == 0 || "空".equals(text)) {
            return true;
        }
        String s = text.toUpperCase(java.util.Locale.ROOT);
        return s.indexOf("STRONG") < 0
                && s.indexOf("READY") < 0
                && s.indexOf("PASS") < 0
                && s.indexOf("RISK") < 0
                && s.indexOf("FREEZE") < 0;
    }

    private boolean weakOrEmptyV20Text(String text) {
        if (text == null || text.length() == 0 || "空".equals(text)) {
            return true;
        }
        String s = text.toUpperCase(java.util.Locale.ROOT);
        return s.indexOf("STRONG") < 0
                && s.indexOf("READY") < 0
                && s.indexOf("PASS") < 0
                && !"S".equals(s)
                && !"A".equals(s);
    }

    private boolean containsStrongReady(String text) {
        if (text == null) {
            return false;
        }
        String s = text.toUpperCase(java.util.Locale.ROOT);
        return s.indexOf("STRONG") >= 0
                || s.indexOf("READY") >= 0
                || s.indexOf("PASS") >= 0
                || "S".equals(s)
                || "A".equals(s);
    }

    private boolean containsText(String text, String key) {
        if (text == null || key == null || key.length() == 0) {
            return false;
        }
        return text.toUpperCase(java.util.Locale.ROOT).indexOf(key.toUpperCase(java.util.Locale.ROOT)) >= 0;
    }

    private String readFirstString(Object obj, String[] names) {
        if (obj == null || names == null) {
            return "";
        }
        for (int i = 0; i < names.length; i++) {
            String v = readString(obj, names[i], "");
            if (v != null && v.length() > 0) {
                return v;
            }
        }
        return "";
    }

    private int maxIntFields(Object obj, String[] fields) {
        if (obj == null || fields == null) {
            return 0;
        }
        int best = 0;
        for (int i = 0; i < fields.length; i++) {
            int v = readInt(obj, fields[i], 0);
            if (v > best) {
                best = v;
            }
        }
        return best;
    }

    private synchronized Models.Decision saveFinalDecision(Models.Decision decision) {
        long saveFinalStartedV8 = System.currentTimeMillis();
        if (decision == null || store == null) return decision;

        if (decision.targetIssue == null || decision.targetIssue.length() == 0) return decision;

        if (V50BuildManifest.COHORT_ID.equals(safe(decision.cohortId))) {
            return saveFinalV50Decision(decision, saveFinalStartedV8);
        }

        syncTeacherAliasFields(decision);
        repairDecisionK3(decision);
        normalizeDecisionScope(decision);
        ensureSafePreDrawPromotionDomainOnObject(decision);

        syncClosedLoopAliases(decision);
        decision = enforceAutoFetchFinalBetSafety(decision, "SAVE_FINAL_DECISION入口");
        normalizeDecisionScope(decision);

        Models.Decision old = null;

        try {
            old = store.getDecisionByTargetIssue(decision.targetIssue);
        } catch (Throwable e) {
            old = null;
        }

        if (old != null
                && old.id > 0L
                && !isWeakPlaceholderDecision(old)
                && sameText(old.baseIssue, decision.baseIssue)
                && AppConfig.DECISION_BET.equals(old.decision)
                && !AppConfig.DECISION_BET.equals(decision.decision)) {

            store.setState("analysis_done_target_issue", old.targetIssue);
            store.setState("analysis_last_base_issue", old.baseIssue == null ? "" : old.baseIssue);
            store.setState("analysis_last_decision", old.decision == null ? "" : old.decision);
            store.setState("analysis_last_summary", buildDecisionBrief(old));
            store.setState("last_decision_target_issue", old.targetIssue);
            store.setState("last_decision_summary", buildDecisionBrief(old));
            store.setState("decision_overwrite_blocked", "1");
            store.setState("decision_overwrite_blocked_reason", "已有正式BET，拒绝后来的HOLD/SHADOW/弱占位覆盖");

            clearRealtimePriorityIfDecisionReady(old);
            ensureTradeCreatedForBetDecision(old, "已有正式BET，确认Trade存在");
            broadcastDecision(old);
            postDecisionDeepFinalize(old);
            return old;
        }

        if (isWeakPlaceholderDecision(decision)) {
            decision =
                    dataGuard.holdDecision(
                            decision.baseIssue,
                            decision.targetIssue,
                            AppConfig.BLOCK_LOW_PROBABILITY,
                            "弱占位/快速观察/快裁结果不允许保存为最终预测，已转为正式HOLD");
            normalizeDecisionScope(decision);
        }

        old = store.getDecisionByTargetIssue(decision.targetIssue);

        if (old != null
                && old.id > 0L
                && !isWeakPlaceholderDecision(old)
                && isWeakPlaceholderDecision(decision)) {

            store.setState("analysis_done_target_issue", old.targetIssue);
            store.setState("analysis_last_base_issue", old.baseIssue == null ? "" : old.baseIssue);
            store.setState("analysis_last_decision", old.decision == null ? "" : old.decision);
            store.setState("analysis_last_summary", buildDecisionBrief(old));
            store.setState("last_decision_target_issue", old.targetIssue);
            store.setState("last_decision_summary", buildDecisionBrief(old));

            clearRealtimePriorityIfDecisionReady(old);
            ensureTradeCreatedForBetDecision(old, "已有正式决策，确认Trade存在");
            broadcastDecision(old);
            postDecisionDeepFinalize(old);
            return old;
        }

        Models.Trade sameTargetTrade = null;
        try {
            sameTargetTrade = store.getTradeByTargetIssue(decision.targetIssue);
        } catch (Throwable e) {
            sameTargetTrade = null;
        }

        Models.DataCheck check = dataGuard.checkDecisionBeforeSave(decision, sameTargetTrade);

        if (check == null || !check.ok) {
            decision = dataGuard.preserveAsShadowOnSaveBlock(decision, check);
            normalizeDecisionScope(decision);
        }

        if (decision.createdAt <= 0L) {
            decision.createdAt = System.currentTimeMillis();
        }

        if (decision.learningSummary == null || decision.learningSummary.length() == 0) {
            decision.learningSummary =
                    "实时正式分析生成；BET/HOLD/SHADOW口径分离；主链=形态趋势→V20因链→教师矩阵桥接→120概率精算→动态门槛→40最终门";
        }

        if (lastTeacherMatrixSummary != null && lastTeacherMatrixSummary.length() > 0) {
            decision.learningSummary =
                    appendReason(decision.learningSummary, "教师矩阵后台摘要：" + lastTeacherMatrixSummary);
        }

        if (lastV20CauseChainSummary != null && lastV20CauseChainSummary.length() > 0) {
            decision.learningSummary =
                    appendReason(decision.learningSummary, "V20因链后台摘要：" + lastV20CauseChainSummary);
        }

        if (decision.selfDiagnosisText == null || decision.selfDiagnosisText.length() == 0) {
            decision.selfDiagnosisText = buildSelfDiagnosis(decision);
        }

        if (decision.reasonSummary == null || decision.reasonSummary.length() == 0) {
            decision.reasonSummary = buildDecisionBrief(decision);
        }

        syncTeacherAliasFields(decision);
        repairDecisionK3(decision);
        normalizeDecisionScope(decision);

        syncClosedLoopAliases(decision);
        decision = enforceAutoFetchFinalBetSafety(decision, "SAVE_FINAL_DECISION保存前");
        normalizeDecisionScope(decision);

        long tDecisionSaveV8 = System.currentTimeMillis();
        saveDecisionFullSafely(decision);
        recordAnalysisCost("decision_save", tDecisionSaveV8);

        Models.Decision savedDecision = null;
        try {
            savedDecision = store.getDecisionByTargetIssue(decision.targetIssue);
        } catch (Throwable e) {
            savedDecision = null;
        }

        if (savedDecision != null && !isWeakPlaceholderDecision(savedDecision)) {
            copyClosedLoopFields(decision, savedDecision);
            decision = savedDecision;
            syncTeacherAliasFields(decision);
            syncClosedLoopAliases(decision);
            decision = enforceAutoFetchFinalBetSafety(decision, "SAVE_FINAL_DECISION读回后");
            normalizeDecisionScope(decision);
        }

        lastFinalDecisionTargetIssue = decision.targetIssue;

        store.setState("analysis_done_target_issue", decision.targetIssue);
        store.setState(
                "analysis_last_base_issue", decision.baseIssue == null ? "" : decision.baseIssue);
        store.setState(
                "analysis_last_decision", decision.decision == null ? "" : decision.decision);
        store.setState("analysis_last_summary", buildDecisionBrief(decision));
        store.setState("last_decision_target_issue", decision.targetIssue);
        store.setState("last_decision_summary", buildDecisionBrief(decision));
        store.setState("last_decision", decision.decision == null ? "" : decision.decision);
        store.setState(
                "last_decision_target", decision.targetIssue == null ? "" : decision.targetIssue);
        store.setState("last_decision_position", String.valueOf(decision.position));
        store.setState(
                "last_decision_numbers",
                UiFormatter.numberGroup(decision.n1, decision.n2, decision.n3));
        store.setState("last_decision_final_p", String.valueOf(decision.finalP));
        store.setState("last_decision_gate", String.valueOf(decision.dynamicGate));
        store.setState("last_decision_edge", String.valueOf(decision.edge));
        store.setState("last_decision_block", decision.blockCode == null ? "" : decision.blockCode);
        store.setState("last_decision_scope", decision.statScope == null ? "" : decision.statScope);
        store.setState("decision_saved_at", String.valueOf(System.currentTimeMillis()));
        store.setState("link_modification_v2_version", LINK_MODIFICATION_V2_AUTOFETCH_VERSION);
        store.setState(
                "link_modification_v2_policy",
                "HOT360实时；WARM1440/10分钟；COLD5000/1小时；DEEP10000/3小时；频率债务只作用合格边缘域；DecisionGate唯一BET出口");
        refreshFrequencyStateForAnalysis(decision.baseIssue, decision.targetIssue);

        saveDecisionReasonState(decision);

        long tTradeConfirmV8 = System.currentTimeMillis();
        ensureTradeCreatedForBetDecision(decision, "BET保存后立即创建真实Trade");
        recordAnalysisCost("trade_confirm", tTradeConfirmV8);

        clearRealtimePriorityIfDecisionReady(decision);
        long tBroadcastV8 = System.currentTimeMillis();
        broadcastDecision(decision);
        recordAnalysisCost("decision_broadcast", tBroadcastV8);
        postDecisionDeepFinalize(decision);
        try {
            heartbeatStateV8().markAnalysisSuccess(decision.targetIssue);
        } catch (Throwable ignore) {
        }
        writeBusinessHeartbeatState(
                "last_analysis_success_at", decision.targetIssue, "ANALYSIS_SUCCEEDED");
        try {
            heartbeatStateV8().markAnalysisSuccess(decision.targetIssue);
        } catch (Throwable ignore) {
        }
        postReservedTeacherSliceAfterDecisionV8(decision.targetIssue);
        recordAnalysisCost("save_final_decision_total", saveFinalStartedV8);
        postRealBetDiagnosisRefresh(
                "最终决策后低频刷新真实BET诊断", AppConfig.POST_DECISION_DIAG_DELAY_MS, false);
        postFullDiagnosisRefresh("最终决策后低频刷新满级诊断", AppConfig.POST_DECISION_DIAG_DELAY_MS, false);
        return decision;
    }

    private Models.Decision saveFinalV50Decision(Models.Decision decision, long startedAt) {
        decision.decision = AppConfig.normalizeDecision(decision.decision);
        decision.action = decision.decision;
        decision.k3Mask = Models.buildK3Mask(decision.n1, decision.n2, decision.n3);
        decision.sourceType = Models.SOURCE_TYPE_REALTIME;
        decision.isRealtimeReal = AppConfig.DECISION_BET.equals(decision.decision) ? 1 : 0;
        decision.statScope =
                AppConfig.DECISION_BET.equals(decision.decision)
                        ? AppConfig.STAT_SCOPE_REAL_TRADE
                        : (AppConfig.DECISION_SHADOW.equals(decision.decision)
                                ? AppConfig.STAT_SCOPE_SHADOW
                                : AppConfig.STAT_SCOPE_HOLD);
        if (AppConfig.DECISION_BET.equals(decision.decision)
                && !DecisionAuthorizationEngine.isDecisionAuthorized(decision)) {
            DecisionAuthorizationEngine.downgradeUnauthorized(
                    decision, "V50_PERSISTED_AUTHORIZATION_INVALID");
        }
        Models.Decision persisted = store.getDecisionByTargetIssue(decision.targetIssue);
        if (persisted == null
                || !safe(persisted.candidateHash).equals(safe(decision.candidateHash))
                || !safe(persisted.v46ContractHash).equals(safe(decision.v46ContractHash))) {
            decision.decision = AppConfig.DECISION_HOLD;
            decision.action = AppConfig.ACTION_HOLD;
            decision.statScope = AppConfig.STAT_SCOPE_HOLD;
            decision.isRealtimeReal = 0;
            decision.authorizationValid = 0;
            decision.blockCode = "V50_DECISION_PERSIST_FAILED";
            decision.finalDecisionRejectCode = decision.blockCode;
            store.setState("v50_decision_persist_status", decision.blockCode);
            return decision;
        }
        decision = persisted;
        lastFinalDecisionTargetIssue = decision.targetIssue;
        store.setState("analysis_done_target_issue", decision.targetIssue);
        store.setState("analysis_last_base_issue", safe(decision.baseIssue));
        store.setState("analysis_last_decision", safe(decision.decision));
        store.setState("analysis_last_summary", buildDecisionBrief(decision));
        store.setState("last_decision_target_issue", decision.targetIssue);
        store.setState("last_decision_summary", buildDecisionBrief(decision));
        store.setState("last_decision", safe(decision.decision));
        store.setState("last_decision_target", safe(decision.targetIssue));
        store.setState("last_decision_position", String.valueOf(decision.position));
        store.setState(
                "last_decision_numbers",
                UiFormatter.numberGroup(decision.n1, decision.n2, decision.n3));
        store.setState("last_decision_final_p", String.valueOf(decision.finalP));
        store.setState("last_decision_gate", String.valueOf(decision.dynamicGate));
        store.setState("last_decision_edge", String.valueOf(decision.edge));
        store.setState("last_decision_block", safe(decision.blockCode));
        store.setState("last_decision_scope", safe(decision.statScope));
        store.setState("v50_decision_persist_status", "OK");
        store.setState("v50_decision_persisted_at", String.valueOf(System.currentTimeMillis()));
        /* The V50 compatibility Decision and optional Trade were committed
         * with the 1/10/100 ledgers.  This UI/broadcast stage is read-only. */
        clearRealtimePriorityIfDecisionReady(decision);
        broadcastDecision(decision);
        try {
            heartbeatStateV8().markAnalysisSuccess(decision.targetIssue);
        } catch (Throwable ignore) {
        }
        writeBusinessHeartbeatState(
                "last_analysis_success_at", decision.targetIssue, "V50_ANALYSIS_SUCCEEDED");
        recordAnalysisCost("save_final_decision_v50", startedAt);
        return decision;
    }

    private void saveDecisionFullSafely(Models.Decision decision) {
        if (store == null || decision == null) {
            return;
        }

        syncTeacherAliasFields(decision);
        repairDecisionK3(decision);
        normalizeDecisionScope(decision);
        // saveDecisionFullSafely_syncClosedLoop_01of04
        syncClosedLoopAliases(decision);

        boolean savedByFull = false;

        try {
            Object r =
                    invokeOptional(
                            store,
                            "replaceDecisionFull",
                            new Class[] {Models.Decision.class},
                            new Object[] {decision});

            if (r != REFLECTION_NO_METHOD) {
                if (isInvokeSuccess(r)) {
                    Models.Decision verify = null;
                    try {
                        verify = store.getDecisionByTargetIssue(decision.targetIssue);
                    } catch (Throwable e) {
                        verify = null;
                    }

                    if (verify != null && !isWeakPlaceholderDecision(verify)) {
                        savedByFull = true;
                    } else if (r == null) {
                        savedByFull = false;
                    }
                }

                if (savedByFull) {
                    try {
                        store.setState("decision_save_full_mode", "replaceDecisionFull");
                    } catch (Throwable ignore) {
                    }
                    return;
                }

                try {
                    store.setState("decision_save_full_mode", "replaceDecisionFull_FALLBACK");
                    store.setState(
                            "decision_save_full_warning",
                            "replaceDecisionFull返回失败或保存后查不到正式Decision，已fallback");
                } catch (Throwable ignore) {
                }
            }
        } catch (Throwable e) {
        }

        try {
            store.saveDecision(decision);
            try {
                store.setState("decision_save_full_mode", "saveDecision");
            } catch (Throwable ignore) {
            }
        } catch (Throwable e) {
            try {
                store.setState("decision_save_full_mode", "ERROR");
                store.setState("decision_save_full_error", safeError(e));
            } catch (Throwable ignore) {
            }
        }
    }

    private boolean isInvokeSuccess(Object r) {
        if (r == REFLECTION_NO_METHOD) {
            return false;
        }

        if (r == null) {
            return true;
        }

        if (r instanceof Boolean) {
            return ((Boolean) r).booleanValue();
        }

        if (r instanceof Number) {
            return ((Number) r).longValue() > 0L;
        }

        if (r instanceof String) {
            String s = String.valueOf(r).trim().toUpperCase(java.util.Locale.ROOT);
            if ("0".equals(s) || "FALSE".equals(s) || "FAIL".equals(s) || "ERROR".equals(s)) {
                return false;
            }
            return true;
        }

        return true;
    }

    private void ensureTradeCreatedForBetDecision(Models.Decision decision, String reason) {
        if(store==null || decision==null || decision.targetIssue==null)return;
        Models.Trade row=store.getTradeByTargetIssue(decision.targetIssue);
        store.setState("cp4_trade_receipt_check",row==null?"MISSING_CANONICAL_TRADE":"CANONICAL_TRADE_PRESENT");
        // A missing row is an integrity diagnostic; this Service may never create it.
    }

    private boolean forceSaveTradeFromDecision(Models.Decision decision) {
        if(store!=null)store.setState("cp4_legacy_trade_create","RETIRED_CANONICAL_RECEIPT_REQUIRED");
        return false;
    }

    private boolean isDecisionValidRealBet(Models.Decision d) {
        if (d == null) {
            return false;
        }

        if (!AppConfig.DECISION_BET.equals(d.decision)) {
            return false;
        }
        if (!DecisionAuthorizationEngine.isDecisionAuthorized(d)) {
            return false;
        }

        /* AutoFetch只消费DecisionGate签发的授权，不做第二策略裁决。
         * 概率/Edge/Teacher/V20等策略字段不能成为AutoFetch第二最终门。 */

        if (!AppConfig.isValidPosition(d.position)) {
            return false;
        }

        repairDecisionK3(d);

        if (!Models.isValidK3Mask(d.k3Mask)) {
            return false;
        }

        if (!AppConfig.isValidNumber(d.n1)
                || !AppConfig.isValidNumber(d.n2)
                || !AppConfig.isValidNumber(d.n3)) {
            return false;
        }

        if (d.n1 == d.n2 || d.n1 == d.n3 || d.n2 == d.n3) {
            return false;
        }

        if (!AppConfig.STAT_SCOPE_REAL_TRADE.equals(d.statScope)) {
            return false;
        }

        if (d.targetIssue == null || d.targetIssue.length() == 0) {
            return false;
        }

        return true;
    }

    private boolean isTradeMatchedDecision(Models.Trade t, Models.Decision d) {
        if (t == null || d == null) {
            return false;
        }

        repairDecisionK3(d);

        if (!sameText(t.targetIssue, d.targetIssue)) {
            return false;
        }

        if (t.position != d.position) {
            return false;
        }

        if (t.k3Mask != d.k3Mask) {
            return false;
        }

        if (t.n1 != d.n1 || t.n2 != d.n2 || t.n3 != d.n3) {
            return false;
        }

        if (!AppConfig.STAT_SCOPE_REAL_TRADE.equals(t.statScope)) {
            return false;
        }

        if (t.result != null && t.result.length() > 0) {
            if (!AppConfig.RESULT_WAIT.equals(t.result)
                    && !"WAIT".equals(t.result)
                    && !"PENDING".equals(t.result)
                    && t.settled == 0) {
                return false;
            }
        }

        return true;
    }

    private String buildTradeMismatchDetail(Models.Trade t, Models.Decision d) {
        if (t == null || d == null) {
            return "";
        }

        StringBuilder sb = new StringBuilder();

        sb.append("tradeIssue=");
        sb.append(t.targetIssue == null ? "" : t.targetIssue);
        sb.append(",decisionIssue=");
        sb.append(d.targetIssue == null ? "" : d.targetIssue);
        sb.append(",tradeP=");
        sb.append(t.position);
        sb.append(",decisionP=");
        sb.append(d.position);
        sb.append(",tradeK=");
        sb.append(UiFormatter.numberGroup(t.n1, t.n2, t.n3));
        sb.append(",decisionK=");
        sb.append(UiFormatter.numberGroup(d.n1, d.n2, d.n3));
        sb.append(",tradeMask=");
        sb.append(t.k3Mask);
        sb.append(",decisionMask=");
        sb.append(d.k3Mask);
        sb.append(",tradeScope=");
        sb.append(t.statScope == null ? "" : t.statScope);
        sb.append(",decisionScope=");
        sb.append(d.statScope == null ? "" : d.statScope);
        sb.append(",tradeSettled=");
        sb.append(t.settled);
        sb.append(",tradeResult=");
        sb.append(t.result == null ? "" : t.result);

        String out = sb.toString();
        if (out.length() > 240) {
            out = out.substring(0, 240);
        }

        return out;
    }

    private void postDecisionDeepFinalize(final Models.Decision sourceDecision) {
        if (!running || sourceDecision == null || store == null) {
            return;
        }

        Handler h = learningHandler;
        if (h == null) {
            h = fetchHandler;
        }

        if (h == null) {
            runDecisionDeepFinalize(sourceDecision);
            return;
        }

        h.postDelayed(
                new Runnable() {
                    @Override
                    public void run() {
                        runDecisionDeepFinalize(sourceDecision);
                    }
                },
                DECISION_DEEP_FINALIZE_DELAY_MS);
    }

    private void runDecisionDeepFinalize(Models.Decision sourceDecision) {
        if (!running || sourceDecision == null || store == null) {
            return;
        }

        try {
            Models.Decision decision = store.getDecisionByTargetIssue(sourceDecision.targetIssue);

            if (decision == null) {
                decision = sourceDecision;
            }

            if (decision == null
                    || decision.targetIssue == null
                    || decision.targetIssue.length() == 0) {
                return;
            }

            if (lastFinalDecisionTargetIssue != null
                    && lastFinalDecisionTargetIssue.length() > 0
                    && !sameText(lastFinalDecisionTargetIssue, decision.targetIssue)) {
                return;
            }

            normalizeDecisionScopePreserveSettlement(decision);
            repairDecisionK3(decision);
            syncTeacherAliasFields(decision);

            if (reasonEngine != null) {
                try {
                    String reasonText = reasonEngine.buildDecisionSummary(decision);
                    if (reasonText != null && reasonText.length() > 0) {
                        decision.reasonText = reasonText;
                    }
                } catch (Throwable e) {
                }
            }

            if (decision.selfDiagnosisText == null || decision.selfDiagnosisText.length() == 0) {
                decision.selfDiagnosisText = buildSelfDiagnosis(decision);
            }

            if (decision.learningSummary == null || decision.learningSummary.length() == 0) {
                decision.learningSummary =
                        "实时正式分析生成；主链=形态趋势→V20因链→教师矩阵桥接→120概率精算→动态门槛→40最终门；后台学习/回测/矩阵后补；结算状态不被原因后补重置";
            }

            if (lastTeacherMatrixSummary != null && lastTeacherMatrixSummary.length() > 0) {
                decision.learningSummary =
                        appendReason(
                                decision.learningSummary, "教师矩阵后台摘要：" + lastTeacherMatrixSummary);
            }

            if (lastV20CauseChainSummary != null && lastV20CauseChainSummary.length() > 0) {
                decision.learningSummary =
                        appendReason(
                                decision.learningSummary, "V20因链后台摘要：" + lastV20CauseChainSummary);
            }

            try {
                if (decision.id > 0L) {
                    store.updateDecisionDeepFields(decision);
                }
            } catch (Throwable e) {
            }

            saveDecisionReasonState(decision);
            ensureTradeCreatedForBetDecision(decision, "深层后补确认BET Trade存在");

            broadcastSimpleStateLimited(true);
            refreshNotificationIfNeeded();
        } catch (Throwable e) {
            try {
                store.setState("decision_deep_finalize_error", safeError(e));
            } catch (Throwable ignore) {
            }
        }
    }

    private void saveDecisionReasonState(Models.Decision decision) {
        if (store == null || decision == null) {
            return;
        }

        try {
            syncTeacherAliasFields(decision);

            store.setState(
                    "decision_why", decision.reasonSummary == null ? "" : decision.reasonSummary);
            store.setState(
                    "decision_deep_reason", decision.deepReason == null ? "" : decision.deepReason);
            store.setState(
                    "decision_learning_summary",
                    decision.learningSummary == null ? "" : decision.learningSummary);
            store.setState(
                    "decision_self_diagnosis",
                    decision.selfDiagnosisText == null ? "" : decision.selfDiagnosisText);
            store.setState("decision_probability_text", buildDecisionBrief(decision));
            store.setState(
                    "decision_block_text",
                    decision.blockCode == null
                            ? ""
                            : UiFormatter.blockReasonText(decision.blockCode));
            store.setState(
                    "decision_stat_scope", decision.statScope == null ? "" : decision.statScope);
            store.setState("decision_mode", decision.stateMode == null ? "" : decision.stateMode);

            String teacherSummary = readTeacherSummary(decision);
            if ((teacherSummary == null || teacherSummary.length() == 0)
                    && lastTeacherMatrixSummary != null) {
                teacherSummary = lastTeacherMatrixSummary;
            }

            int teacherSample = readTeacherSample(decision);
            double teacherHit = readTeacherHitRate(decision);
            double teacherConfidence = readTeacherConfidence(decision);
            double teacherRisk = readTeacherRisk(decision);
            double teacherScore = readTeacherScore(decision);

            store.setState(
                    "decision_teacher_matrix_summary",
                    teacherSummary == null ? "" : teacherSummary);
            store.setState("decision_teacher_matrix_status", readTeacherStatus(decision));
            store.setState("decision_teacher_matrix_sample", String.valueOf(teacherSample));
            store.setState("decision_teacher_matrix_hit_rate", String.valueOf(teacherHit));
            store.setState("decision_teacher_matrix_confidence", String.valueOf(teacherConfidence));
            store.setState("decision_teacher_matrix_risk", String.valueOf(teacherRisk));
            store.setState("decision_teacher_matrix_score", String.valueOf(teacherScore));

            store.setState(
                    "decision_teacher_number_summary",
                    teacherSummary == null ? "" : teacherSummary);
            store.setState("decision_teacher_number_status", readTeacherStatus(decision));
            store.setState("decision_teacher_number_sample", String.valueOf(teacherSample));
            store.setState("decision_teacher_number_hit_rate", String.valueOf(teacherHit));
            store.setState("decision_teacher_number_confidence", String.valueOf(teacherConfidence));
            store.setState("decision_teacher_number_risk", String.valueOf(teacherRisk));
            store.setState("decision_teacher_number_score", String.valueOf(teacherScore));

            store.setState(
                    "decision_v20_cause_summary",
                    lastV20CauseChainSummary == null ? "" : lastV20CauseChainSummary);
            store.setState("decision_saved_at", String.valueOf(System.currentTimeMillis()));
        } catch (Throwable e) {
        }
    }

    private String buildSelfDiagnosis(Models.Decision d) {
        if (d == null) {
            return "";
        }

        syncTeacherAliasFields(d);

        StringBuilder sb = new StringBuilder();

        sb.append("自检：");

        if (AppConfig.DECISION_BET.equals(d.decision)) {
            sb.append("真实BET候选");
        } else if (AppConfig.DECISION_SHADOW.equals(d.decision)) {
            sb.append("SHADOW观察，不计真实胜率");
        } else {
            sb.append("HOLD不出手");
        }

        sb.append("；FinalP=");
        sb.append(UiFormatter.probability(d.finalP));
        sb.append("，Gate=");
        sb.append(UiFormatter.probability(d.dynamicGate));
        sb.append("，Edge=");
        sb.append(UiFormatter.signedProbability(d.edge));

        if (d.blockCode != null && d.blockCode.length() > 0) {
            sb.append("；拦截=");
            sb.append(UiFormatter.blockReasonText(d.blockCode));
        }

        sb.append("；状态=");
        sb.append(d.stateMode == null ? "" : d.stateMode);

        String tm = readTeacherSummary(d);
        if (tm != null && tm.length() > 0) {
            sb.append("；教师矩阵=");
            sb.append(shortText(tm, 180));
        } else if (lastTeacherMatrixSummary != null && lastTeacherMatrixSummary.length() > 0) {
            sb.append("；教师矩阵后台=");
            sb.append(shortText(lastTeacherMatrixSummary, 180));
        }

        if (lastV20CauseChainSummary != null && lastV20CauseChainSummary.length() > 0) {
            sb.append("；V20因链=");
            sb.append(shortText(lastV20CauseChainSummary, 180));
        }

        sb.append(
                "；调度=V20正式主链；120候选进概率精算；40候选进最终门；教师矩阵已做实时桥接；AutoFetch不救援BET；不快裁BET；不超时HOLD覆盖BET；BET保存后立即建Trade；深层后补不重置结算状态；后台任务让路");

        return sb.toString();
    }

    private void recordAnalysisCost(String stage, long startAt) {
        if (store == null || stage == null || stage.length() == 0) return;
        try {
            long cost = System.currentTimeMillis() - startAt;
            if (cost < 0L) cost = 0L;
            String targetIssueForCost =
                    teacherBridgeTargetIssueV3 == null ? "" : teacherBridgeTargetIssueV3;
            store.saveRealtimeStageCost(targetIssueForCost, stage, cost);
            store.setState("analysis_cost_" + stage, String.valueOf(cost));
            store.setState("analysis_cost_last_stage", stage);
            store.setState("analysis_cost_last_ms", String.valueOf(cost));
            store.setState("analysis_cost_updated_at", String.valueOf(System.currentTimeMillis()));
            if ("build_decision_total".equals(stage)
                    || "build_decision_total_shadow".equals(stage)
                    || "build_decision_total_shadow_hold".equals(stage)) {
                long un = cost - analysisRecordedCostSumV8;
                if (un < 0L) un = 0L;
                store.setState(AppConfig.STATE_DECISION_UNATTRIBUTED_MS, String.valueOf(un));
                store.setState(
                        "decision_recorded_stage_sum_ms",
                        String.valueOf(analysisRecordedCostSumV8));
                store.setState("decision_total_measured_ms", String.valueOf(cost));
                store.setState("decision_cost_target", analysisRecordedTargetV8);
            } else {
                analysisRecordedCostSumV8 += cost;
            }
        } catch (Throwable e) {
        }
    }

    private void postSaveShapeVectorsAsync(final List<Models.ShapeVector> vectors) {
        if (vectors == null || vectors.size() == 0 || learningHandler == null || store == null) {
            return;
        }

        learningHandler.postDelayed(
                new Runnable() {
                    @Override
                    public void run() {
                        if (!running || store == null) {
                            return;
                        }

                        if (shouldDeferHeavyBackgroundNow()) {
                            if (learningHandler != null) {
                                learningHandler.postDelayed(this, 3500L);
                            }
                            return;
                        }

                        saveShapeVectorsOnce(vectors);
                    }
                },
                2500L);
    }

    private ArrayList<Models.ComboCandidate> applyRealtimeDynamicMiniCore(
            ArrayList<Models.ComboCandidate> candidates,
            List<Models.Draw> clean,
            String baseIssue,
            String targetIssue,
            long totalStart) {
        if (candidates == null) {
            return new ArrayList<Models.ComboCandidate>();
        }
        long start = System.currentTimeMillis();
        int ready = 0;
        int strong = 0;
        int watch = 0;
        int freeze = 0;
        for (int i = 0; i < candidates.size(); i++) {
            Models.ComboCandidate c = candidates.get(i);
            if (c == null) {
                continue;
            }
            applySingleRealtimeDynamicMiniCore(c, clean, baseIssue, targetIssue, i + 1, start);
            String s = readString(c, "dynamicClusterStatus", "");
            if (containsSimple(s, "STRONG")) {
                strong++;
            } else if (containsSimple(s, "READY")) {
                ready++;
            } else if (containsSimple(s, "FREEZE") || containsSimple(s, "RISK")) {
                freeze++;
            } else {
                watch++;
            }
            if (System.currentTimeMillis() - start > REALTIME_DYNAMIC_MINI_CORE_BUDGET_MS) {
                writeDynamicCausalState(
                        "MINI_CORE_BUDGET_STOP",
                        "实时动态因果收敛小核达到预算，已处理="
                                + (i + 1)
                                + "/"
                                + candidates.size()
                                + "；剩余候选保留快照字段，不阻塞DecisionGate");
                break;
            }
        }
        long cost = System.currentTimeMillis() - start;
        try {
            if (store != null) {
                store.setState("realtime_mini_core_mode", "CURRENT_WINDOW_REQUIRED");
                store.setState(
                        "realtime_mini_core_version", V20_DYNAMIC_CAUSAL_AUTOFETCH_PATCH_VERSION);
                store.setState("realtime_mini_core_base", safe(baseIssue));
                store.setState("realtime_mini_core_target", safe(targetIssue));
                store.setState("realtime_mini_core_count", String.valueOf(candidates.size()));
                store.setState("realtime_mini_core_ready", String.valueOf(ready));
                store.setState("realtime_mini_core_strong", String.valueOf(strong));
                store.setState("realtime_mini_core_watch", String.valueOf(watch));
                store.setState("realtime_mini_core_freeze", String.valueOf(freeze));
                store.setState("realtime_mini_core_cost_ms", String.valueOf(cost));
                store.setState(
                        "realtime_mini_core_policy",
                        "收敛版：实时必跑当前窗口形态、当前候选K3、NeedSupply、Counterfactual、Adaptive轻量小核；不动DecisionGate；长画像/全量报告后台补全");
                store.setState("realtime_converged_fix14", "1");
                store.setState(
                        "realtime_converged_fix14_note",
                        "单文件收敛：不快照空裁决，不回到30秒全量同步，不改核心策略门，只修调度与字段桥");
                store.setState(
                        "adaptive_judge_realtime_candidate_count",
                        String.valueOf(candidates.size()));
                store.setState("adaptive_judge_realtime_mode", "MINI_CORE_CANDIDATES_PASSED");
                store.setState(
                        "dynamic_causal_realtime_candidate_count",
                        String.valueOf(candidates.size()));
                store.setState("dynamic_causal_realtime_mode", "CURRENT_MINI_CORE");
            }
        } catch (Throwable e) {
        }
        recordAnalysisCost("realtime_dynamic_mini_core", start);
        return candidates;
    }

    private void applySingleRealtimeDynamicMiniCore(
            Models.ComboCandidate c,
            List<Models.Draw> clean,
            String baseIssue,
            String targetIssue,
            int rank,
            long startAt) {
        if (c == null) return;
        c.baseIssue = baseIssue;
        c.targetIssue = targetIssue;
        double cover =
                max3(
                        readDouble(c, "coverageQualityScore", 0.0d),
                        readDouble(c, "dynamicGateCoverageQualityScore", 0.0d),
                        readDouble(c, "v20CoverageScore", 0.0d));
        double third =
                max3(
                        readDouble(c, "thirdNumberQualityScore", 0.0d),
                        readDouble(c, "dynamicGateThirdNumberQuality", 0.0d),
                        readDouble(c, "needSupplyThirdQuality", 0.0d));
        double need =
                Math.max(
                        readDouble(c, "needSupplyMatchScore", 0.0d),
                        Math.max(
                                readDouble(c, "positionNeedScore", 0.0d),
                                readDouble(c, "numberSupplyScore", 0.0d)));
        double counter =
                Math.max(
                        readDouble(c, "counterfactualWhyNotRisk", 0.0d),
                        Math.max(
                                readDouble(c, "counterEvidenceScore", 0.0d),
                                readDouble(c, "counterRisk", 0.0d)));
        String window =
                readFirstString(
                        c,
                        new String[] {
                            "windowStateKey",
                            "dynamicWindowStateKey",
                            "probabilityWindowStateKey",
                            "v20WindowStateKey"
                        });
        if (window == null || window.length() == 0) window = "P" + c.position + "_HOT_WINDOW";
        if (readString(c, "dynamicClusterId", "").length() == 0) {
            String key =
                    "P"
                            + c.position
                            + "|"
                            + probabilityBucketForBridge(c.finalP)
                            + "|"
                            + coarseWindowForBridge(window);
            writeString(c, "dynamicClusterId", AppConfig.buildDynamicClusterId(key));
        }
        int sample = readInt(c, "dynamicClusterSample", 0);
        if (sample <= 0) {
            writeString(c, "dynamicClusterStatus", "UNKNOWN_NEUTRAL");
            writeInt(c, "dynamicClusterSample", 0);
            writeInt(c, "dynamicClusterRecentSample", 0);
            writeDouble(c, "dynamicClusterHitRate", 0.0d);
            writeDouble(c, "dynamicClusterRecentHitRate", 0.0d);
            writeDouble(c, "dynamicClusterConfidence", 0.0d);
            writeInt(c, "dynamicClusterMaxLose", 0);
            writeDouble(c, "dynamicClusterCounterScore", 0.0d);
            writeString(c, "evidenceSource", "NO_SETTLED_DYNAMIC_EVIDENCE");
            writeInt(c, "evidenceSettledOnly", 1);
            writeInt(c, "evidenceSampleCount", 0);
        }
        if (readDouble(c, "needSupplyMatchScore", 0.0d) <= 0.0d)
            writeDouble(c, "needSupplyMatchScore", clamp01(need));
        if (readDouble(c, "needSupplyThirdQuality", 0.0d) <= 0.0d)
            writeDouble(c, "needSupplyThirdQuality", clamp01(third));
        if (readDouble(c, "counterfactualWhyNotRisk", 0.0d) <= 0.0d)
            writeDouble(c, "counterfactualWhyNotRisk", clamp01(counter));
        if (readString(c, "adaptiveJudgeStatus", "").length() == 0) {
            writeString(c, "adaptiveJudgeStatus", "NO_SAMPLE_NEUTRAL");
            writeString(c, "adaptiveJudgeAction", "REVIEW_BY_DECISION_GATE");
        }
        if (readString(c, "promotionClusterStatus", "").length() == 0) {
            writeString(c, "promotionClusterStatus", AppConfig.PROMOTION_NO_SAMPLE);
            writeInt(c, "promotionClusterSample", 0);
            writeDouble(c, "promotionClusterHitRate", 0.0d);
        }
        if (readInt(c, "hardBlockFlag", 0) != 1
                && !AppConfig.isExplicitHardBlockStatus(readString(c, "riskSeverity", ""))) {
            writeString(c, "riskSeverity", AppConfig.EVIDENCE_SEVERITY_NEUTRAL);
            writeInt(c, "hardBlockFlag", 0);
        }
        writeString(c, "dynamicCausalBridgeStage", "TRUE_EVIDENCE_ONLY");
        writeString(c, "dynamicCausalBridgeVersion", AppConfig.OUMAN_GALAXY_REBUILD_VERSION);
        syncClosedLoopAliases(c);
    }

    private String probabilityBucketForBridge(double p) {
        if (p >= 0.380d) return "P38_PLUS";
        if (p >= 0.360d) return "P36_38";
        if (p >= 0.340d) return "P34_36";
        return "P_LT34";
    }

    private String coarseWindowForBridge(String window) {
        if (window == null) return "UNKNOWN";
        int i = window.indexOf('|');
        String s = i > 0 ? window.substring(0, i) : window;
        return s.length() > 80 ? s.substring(0, 80) : s;
    }

    private boolean containsSimple(String text, String key) {
        if (text == null || key == null || key.length() == 0) {
            return false;
        }
        return text.toUpperCase(java.util.Locale.ROOT).indexOf(key.toUpperCase(java.util.Locale.ROOT)) >= 0;
    }

    private double max3(double a, double b, double c) {
        return Math.max(a, Math.max(b, c));
    }

    private ArrayList<Models.ComboCandidate> runDynamicCausalWindowRealtimeEngines(
            ArrayList<Models.ComboCandidate> candidates,
            List<Models.Draw> clean,
            String baseIssue,
            String targetIssue) {
        long start = System.currentTimeMillis();
        if (candidates == null) {
            return new ArrayList<Models.ComboCandidate>();
        }
        if (candidates.size() == 0) {
            writeDynamicCausalState("NO_CANDIDATES", "动态因果轻量实时链未运行：候选为空");
            return candidates;
        }
        int before = candidates.size();
        candidates = capCandidatesByScore(candidates, DYNAMIC_CAUSAL_REALTIME_CANDIDATE_CAP);
        try {
            if (store != null) {
                store.setState(
                        "dynamic_causal_autofetch_version",
                        V20_DYNAMIC_CAUSAL_AUTOFETCH_PATCH_VERSION);
                store.setState("dynamic_causal_realtime_target", safe(targetIssue));
                store.setState(
                        "dynamic_causal_realtime_candidate_count",
                        String.valueOf(candidates.size()));
                store.setState("dynamic_causal_realtime_mode", "REDUCED_CURRENT_ONLY");
            }
        } catch (Throwable e) {
        }

        /*
         * 运行期修复：
         * 第11/14 原版把 NeedSupply、DynamicCluster、Counterfactual、
         * ShadowPromotion、AdaptiveJudge 全部同步反射进实时主链。
         * 低端手机/AIDE 环境会导致分析线程超过 30 秒仍不返回。
         *
         * 本版实时只做轻量字段桥接：
         * - 不跑全量 SHADOW 晋级刷新；
         * - 不同步跑全量 AdaptiveJudge 报告；
         * - 不逐候选反射重引擎；
         * - 只用已完成的概率/动态门/上游证据补齐动态因果字段；
         * - 让 DecisionGateEngine 继续做唯一最终 BET 裁决。
         */
        candidates =
                ensureRealtimeDynamicCausalFields(
                        candidates,
                        clean,
                        baseIssue,
                        targetIssue,
                        "REALTIME_REDUCED_CURRENT_ONLY_START");
        /*
         * V20_REDUCED:
         * 不在1分钟实时链里逐候选反射跑 DynamicCause/Cluster/NeedSupply/Counterfactual 四个重型引擎。
         * 当前期实时只做字段桥 + 轻量动态小核，避免 30~60 秒卡死。
         * 完整策略字段仍由 applyRealtimeDynamicMiniCore / DecisionGate 读取，长画像不抢实时。
         */
        applyFastDynamicCausalBridge(candidates, clean, baseIssue, targetIssue, start);

        long cost = System.currentTimeMillis() - start;
        recordAnalysisCost("dynamic_causal_realtime_lite", start);
        writeDynamicCausalState(
                "LITE_OK",
                "动态因果实时减载小核完成｜候选="
                        + before
                        + "→"
                        + candidates.size()
                        + "｜耗时="
                        + cost
                        + "ms｜必要动态小核已实时执行，重型逐候选引擎已跳过，实时收敛小核分析40最终门候选；160候选后台补全；真实BET仍由DecisionGateEngine唯一裁决");
        return candidates;
    }

    private void ensureDynamicCausalEnginesReady() {
        if (dynamicWindowClusterEngine == null) {
            dynamicWindowClusterEngine =
                    createOptionalEngine("com.ethk3.novaonepro.DynamicWindowClusterEngine");
        }
        if (needSupplyReasonEngine == null) {
            needSupplyReasonEngine =
                    createOptionalEngine("com.ethk3.novaonepro.NeedSupplyReasonEngine");
        }
        if (counterfactualReasonEngine == null) {
            counterfactualReasonEngine =
                    createOptionalEngine("com.ethk3.novaonepro.CounterfactualReasonEngine");
        }
        if (shadowPromotionVerifierEngine == null) {
            shadowPromotionVerifierEngine = new ShadowPromotionVerifierEngine();
        }
        if (adaptiveLearningJudgeEngine == null) {
            adaptiveLearningJudgeEngine = new AdaptiveLearningJudgeEngine();
        }
    }

    private void runNeedSupplyReasonRealtime(
            ArrayList<Models.ComboCandidate> candidates,
            List<Models.Draw> clean,
            String baseIssue,
            String targetIssue) {
        if (candidates == null) return;
        ensureRealtimeDynamicCausalFields(
                candidates, clean, baseIssue, targetIssue, "NEED_SUPPLY_LITE");
    }

    private void runDynamicWindowClusterRealtime(
            ArrayList<Models.ComboCandidate> candidates, String baseIssue, String targetIssue) {
        if (candidates == null) return;
        ensureRealtimeDynamicCausalFields(
                candidates, null, baseIssue, targetIssue, "DYNAMIC_CLUSTER_LITE");
    }

    private void runCounterfactualReasonRealtime(
            ArrayList<Models.ComboCandidate> candidates,
            List<Models.Draw> clean,
            String baseIssue,
            String targetIssue) {
        if (candidates == null) return;
        ensureRealtimeDynamicCausalFields(
                candidates, clean, baseIssue, targetIssue, "COUNTERFACTUAL_LITE");
    }

    private void refreshShadowPromotionDynamicSnapshots(String reason) {
        long now = System.currentTimeMillis();
        if (store == null) return;
        if (now - lastShadowPromotionRealtimeRefreshAt
                < SHADOW_PROMOTION_REALTIME_REFRESH_MIN_INTERVAL_MS) {
            try {
                store.setState("shadow_promotion_dynamic_refresh", "SKIP_THROTTLE");
                store.setState(
                        "dynamic_causal_shadow_promotion_refresh_reason",
                        "实时链节流：避免每期重刷SHADOW晋级导致超时");
            } catch (Throwable e) {
            }
            return;
        }
        lastShadowPromotionRealtimeRefreshAt = now;
        try {
            store.setState("shadow_promotion_dynamic_refresh", "DEFER_BACKGROUND");
            store.setState("dynamic_causal_shadow_promotion_refresh_reason", safe(reason));
            store.setState("dynamic_causal_shadow_promotion_refresh_at", String.valueOf(now));
        } catch (Throwable e) {
        }
    }

    private void runAdaptiveJudgeRealtime(
            ArrayList<Models.ComboCandidate> candidates, String baseIssue, String targetIssue) {
        if (candidates == null || candidates.size() == 0) {
            writeDynamicCausalState("ADAPTIVE_SKIP", "AdaptiveJudge未收到候选");
            return;
        }
        try {
            if (adaptiveLearningJudgeEngine == null)
                adaptiveLearningJudgeEngine = new AdaptiveLearningJudgeEngine();
            AdaptiveJudgeReport report =
                    adaptiveLearningJudgeEngine.judgeRealtimeCandidates(
                            candidates, baseIssue, targetIssue);
            if (store != null) {
                store.setState(
                        AppConfig.STATE_ADAPTIVE_INPUT_COUNT, String.valueOf(candidates.size()));
                store.setState(
                        "adaptive_judge_realtime_candidate_count",
                        String.valueOf(candidates.size()));
                store.setState("adaptive_judge_realtime_target", safe(targetIssue));
                store.setState("adaptive_judge_realtime_base", safe(baseIssue));
                store.setState("adaptive_judge_status", "CANDIDATES_JUDGED");
                store.setState(
                        "adaptive_judge_summary",
                        report == null ? "REPORT_NULL" : safe(report.summary));
            }
        } catch (Throwable e) {
            try {
                if (store != null) store.setState("adaptive_judge_status", "ERROR:" + safeError(e));
            } catch (Throwable ignore) {
            }
        }
    }

    private void applyFastDynamicCausalBridge(
            ArrayList<Models.ComboCandidate> candidates,
            List<Models.Draw> clean,
            String baseIssue,
            String targetIssue,
            long startAt) {
        if (candidates == null) return;
        for (int i = 0; i < candidates.size(); i++) {
            if (System.currentTimeMillis() - startAt > DYNAMIC_CAUSAL_REALTIME_HARD_BUDGET_MS) {
                writeDynamicCausalState("LITE_BUDGET_STOP", "动态因果轻量桥达到硬预算，剩余候选只保留原字段，不阻塞正式分析");
                break;
            }
            Models.ComboCandidate c = candidates.get(i);
            if (c == null) continue;
            ensureSingleCandidateDynamicCausalFields(
                    c, clean, baseIssue, targetIssue, "FAST_DYNAMIC_BRIDGE");
        }
    }

    private ArrayList<Models.ComboCandidate> ensureRealtimeDynamicCausalFields(
            ArrayList<Models.ComboCandidate> candidates,
            List<Models.Draw> clean,
            String baseIssue,
            String targetIssue,
            String stage) {
        if (candidates == null) return new ArrayList<Models.ComboCandidate>();
        for (int i = 0; i < candidates.size(); i++) {
            Models.ComboCandidate c = candidates.get(i);
            if (c == null) continue;
            ensureSingleCandidateDynamicCausalFields(c, clean, baseIssue, targetIssue, stage);
        }
        return candidates;
    }

    private void ensureSingleCandidateDynamicCausalFields(
            Models.ComboCandidate c,
            List<Models.Draw> clean,
            String baseIssue,
            String targetIssue,
            String stage) {
        applySingleRealtimeDynamicMiniCore(
                c, clean, baseIssue, targetIssue, 0, System.currentTimeMillis());
        if (c != null) {
            writeString(c, "dynamicCausalBridgeStage", safe(stage));
            writeString(c, "dynamicCausalSummary", "真实证据桥：缺失样本保持0/中性，不再由当前FinalP伪造历史命中率");
        }
    }

    private void writeDynamicCausalState(String status, String summary) {
        if (store == null) return;
        try {
            store.setState("dynamic_causal_autofetch_status", safe(status));
            store.setState("dynamic_causal_autofetch_summary", safe(summary));
            store.setState(
                    "dynamic_causal_autofetch_at", String.valueOf(System.currentTimeMillis()));
        } catch (Throwable e) {
        }
    }

    private void saveRealtimeCandidateSnapshots(
            List<Models.ComboCandidate> candidates,
            String baseIssue,
            String targetIssue,
            String stage,
            boolean adaptiveInput) {
        if (store == null || candidates == null || candidates.size() == 0) {
            return;
        }
        ArrayList<Models.ComboCandidate> top =
                capCandidatesByScore(candidates, REALTIME_DECISION_CANDIDATE_CAP);
        for (int i = 0; i < top.size(); i++) {
            Models.ComboCandidate c = top.get(i);
            if (c == null) {
                continue;
            }
            if (baseIssue != null && baseIssue.length() > 0) {
                c.baseIssue = baseIssue;
            }
            if (targetIssue != null && targetIssue.length() > 0) {
                c.targetIssue = targetIssue;
            }
            writeInt(c, "candidateRank", i + 1);
            writeInt(c, "realtimeTop40Rank", i + 1);
            writeInt(c, "adaptiveInputFlag", adaptiveInput ? 1 : 0);
            writeString(c, "fieldChainVersion", V20_MAIN_CHAIN_AUTOFETCH_VERSION);
            syncClosedLoopAliases(c);
        }
        try {
            Object r;
            if (adaptiveInput) {
                r =
                        invokeOptional(
                                store,
                                "saveAdaptiveJudgeInputCandidates",
                                new Class[] {List.class, String.class, String.class, String.class},
                                new Object[] {top, baseIssue, targetIssue, stage});
                if (r == REFLECTION_NO_METHOD) {
                    r =
                            invokeOptional(
                                    store,
                                    "saveAdaptiveJudgeInputCandidates",
                                    new Class[] {List.class, String.class, String.class},
                                    new Object[] {top, targetIssue, stage});
                }
            } else {
                r = REFLECTION_NO_METHOD;
            }
            if (r == REFLECTION_NO_METHOD) {
                r =
                        invokeOptional(
                                store,
                                "saveRealtimeTop40Candidates",
                                new Class[] {List.class, String.class, String.class, String.class},
                                new Object[] {top, baseIssue, targetIssue, stage});
            }
            if (r == REFLECTION_NO_METHOD) {
                r =
                        invokeOptional(
                                store,
                                "saveRealtimeTop40Candidates",
                                new Class[] {List.class, String.class, String.class},
                                new Object[] {top, targetIssue, stage});
            }
            if (r == REFLECTION_NO_METHOD) {
                for (int i = 0; i < top.size(); i++) {
                    invokeOptional(
                            store,
                            "saveMainChainCandidateSnapshot",
                            new Class[] {
                                Models.ComboCandidate.class, int.class, boolean.class, String.class
                            },
                            new Object[] {
                                top.get(i),
                                Integer.valueOf(i + 1),
                                Boolean.valueOf(adaptiveInput),
                                stage
                            });
                }
            }
            store.setState(
                    adaptiveInput
                            ? "adaptive_realtime_input_saved"
                            : "realtime_top40_snapshot_saved",
                    String.valueOf(top.size()));
            store.setState(
                    adaptiveInput
                            ? "adaptive_realtime_input_stage"
                            : "realtime_top40_snapshot_stage",
                    safe(stage));
            store.setState("main_chain_autofetch_version", V20_MAIN_CHAIN_AUTOFETCH_VERSION);
        } catch (Throwable e) {
            try {
                store.setState(
                        adaptiveInput
                                ? "adaptive_realtime_input_error"
                                : "realtime_top40_snapshot_error",
                        safeError(e));
            } catch (Throwable ignore) {
            }
        }
    }

    private void saveCandidateRuntimeSummary(List<Models.ComboCandidate> candidates) {
        if (store == null || candidates == null || candidates.size() == 0) {
            return;
        }

        try {
            StringBuilder sb = new StringBuilder();
            int count = 0;

            for (int i = 0; i < candidates.size(); i++) {
                Models.ComboCandidate c = candidates.get(i);
                if (c == null || !c.isValidK3()) {
                    continue;
                }

                if (count >= REALTIME_FINAL_DISPLAY_TOP) {
                    break;
                }

                syncTeacherAliasFields(c);

                if (sb.length() > 0) {
                    sb.append(" | ");
                }

                sb.append("#");
                sb.append(count + 1);
                sb.append(" P");
                sb.append(c.position);
                sb.append(" ");
                sb.append(UiFormatter.numberGroup(c.n1, c.n2, c.n3));
                sb.append(" F=");
                sb.append(UiFormatter.probability(c.finalP));
                sb.append(" G=");
                sb.append(UiFormatter.probability(c.dynamicGate));
                sb.append(" E=");
                sb.append(UiFormatter.signedProbability(c.edge));

                int tmSample = readTeacherSample(c);
                double tmHit = readTeacherHitRate(c);
                String tmStatus = readTeacherStatus(c);

                if (tmSample > 0 || tmStatus.length() > 0) {
                    sb.append(" TM=");
                    sb.append(tmStatus);
                    sb.append("/");
                    sb.append(tmSample);
                    sb.append("/");
                    sb.append(UiFormatter.probability(tmHit));
                }

                double causeScore =
                        maxDoubleFields(
                                c,
                                new String[] {
                                    "dynamicCauseScore",
                                    "causeChainScore",
                                    "causeScore",
                                    "finalCauseScore"
                                });
                if (causeScore > 0.0d) {
                    sb.append(" Cause=");
                    sb.append(UiFormatter.score(causeScore));
                }

                if (c.blockCode != null && c.blockCode.length() > 0) {
                    sb.append(" B=");
                    sb.append(UiFormatter.blockReasonText(c.blockCode));
                }

                count++;
            }

            store.setState("candidate_top_summary", sb.toString());
            store.setState("candidate_top_count", String.valueOf(candidates.size()));
            store.setState("candidate_top_updated_at", String.valueOf(System.currentTimeMillis()));
        } catch (Throwable e) {
        }
    }

    private void saveShapeVectorsOnce(List<Models.ShapeVector> vectors) {
        if (vectors == null || vectors.size() == 0 || store == null) {
            return;
        }

        String baseIssue = "";

        for (int i = 0; i < vectors.size(); i++) {
            Models.ShapeVector v = vectors.get(i);

            if (v != null && v.baseIssue != null && v.baseIssue.length() > 0) {
                baseIssue = v.baseIssue;
                break;
            }
        }

        if (baseIssue.length() > 0) {
            List<Models.ShapeVector> old = store.getShapeVectorsForIssue(baseIssue);

            if (old != null && old.size() >= AppConfig.POSITION_COUNT) {
                return;
            }
        }

        for (int i = 0; i < vectors.size(); i++) {
            Models.ShapeVector v = vectors.get(i);

            if (v != null) {
                store.saveShapeVector(v);
            }
        }
    }

    private final java.util.concurrent.atomic.AtomicBoolean quickHistoryQueuedR71 = new java.util.concurrent.atomic.AtomicBoolean();

    /** Reuses the one backfill lane. A slow history/date request cannot stop latest polling. */
    private void scheduleQuickHistorySyncR71(final String reason) {
        final Handler owner=backfillHandler;
        if (!running||owner==null||!quickHistoryQueuedR71.compareAndSet(false,true)) return;
        boolean posted=false;
        try {
            posted=owner.postAtFrontOfQueue(new Runnable(){@Override public void run(){
                try {
                    if (!running||store==null) return;
                    performQuickHistorySync(reason);
                } finally { quickHistoryQueuedR71.set(false); }
            }});
        } finally { if(!posted)quickHistoryQueuedR71.set(false); }
    }

    private Models.Draw performQuickHistorySync(String reason) {
        if (store == null || apiClient == null) {
            return null;
        }

        long now = System.currentTimeMillis();

        if (lastQuickHistorySyncAt > 0L
                && now - lastQuickHistorySyncAt < QUICK_HISTORY_SYNC_MIN_INTERVAL_MS) {
            try {
                Models.Draw cached=store.getLatestDraw();
                return ProviderFreshnessGateR3611.evaluate(cached,"",now).accepted?cached:null;
            } catch (Throwable e) {
                return null;
            }
        }

        lastQuickHistorySyncAt = now;
        int inserted = 0;

        try {
            store.updateBackfillStatus("RUNNING", 0, reason == null ? "快速同步最新开奖" : reason);
            store.setState("quick_history_sync_status", "RUNNING");
            store.setState("quick_history_sync_reason", reason == null ? "" : reason);
            store.setState("quick_history_sync_at", String.valueOf(now));

            Models.Draw localBefore = store.getLatestDraw();
            String expectedTarget =
                    localBefore == null || !localBefore.isValid()
                            ? ""
                            : AppConfig.nextIssueText(localBefore.issue);

            Models.ApiResult r =
                    apiClient.fetchHistoryFresh(
                            AppConfig.BACKFILL_LATEST_LIMIT, expectedTarget);
            String failedResponse=ApiClient.failureSummary(r);

            if (r == null || !r.ok || r.draws == null || r.draws.size() == 0) {
                long freshNow = System.currentTimeMillis();
                if (lastDateFreshnessRecoveryAtR3611 <= 0L
                        || freshNow - lastDateFreshnessRecoveryAtR3611
                                >= R3611_DATE_FRESHNESS_RECOVERY_MIN_INTERVAL_MS) {
                    lastDateFreshnessRecoveryAtR3611 = freshNow;
                    Models.ApiResult dated = apiClient.fetchTodayBackfillFresh(expectedTarget);
                    if (dated != null && dated.ok) r = dated;
                    else failedResponse+="|TODAY="+ApiClient.failureSummary(dated);
                }
            }

            if(r==null||!r.ok||r.draws==null||r.draws.isEmpty()) {
                lastError=failedResponse;
                store.updateBackfillStatus("ERROR",0,lastError);
                store.setState("quick_history_sync_status","ERROR");
                store.setState("quick_history_sync_error",lastError);
                heartbeatStateV8().markFetchFailed(safeIssue(localBefore),lastError);
                broadcastDrawsUpdated(true);return null;
            }

            if (r != null && r.ok && r.draws != null && r.draws.size() > 0) {
                final Models.Draw previousR71=localBefore;
                final Models.ApiResult pageR71=r;
                saveApiResultDraws(r,new Runnable(){@Override public void run(){
                    handleLatestDrawAfterSave(getAuthoritativeLatest(pageR71.latestDraw),previousR71,false,"R71_HISTORY_LATEST_FIRST");
                }});
                inserted = parseInt(store.getState("draw_save_inserted","0"),0);
                persistProviderFreshnessR3611("QUICK_HISTORY");
                if (r.apiLine != null && r.apiLine.length() > 0) {
                    store.setState("api_line", r.apiLine);
                }
            }

            Models.Draw latest = store.getLatestDraw();
            if (latest != null && latest.isValid()) {
                saveLatestDrawState(latest, "quick_history100");
            }

            int count = store.getDrawCount();
            ProviderFreshnessGateR3611.Decision latestFreshness =
                    ProviderFreshnessGateR3611.evaluate(
                            latest, expectedTarget, System.currentTimeMillis());
            if (latestFreshness.accepted)
                recordValidatedFetchResponseR817(
                        latest, expectedTarget, r == null ? "" : r.apiLine);
            String syncStatus = latestFreshness.accepted ? "OK" : "STALE_NO_ADVANCE";

            store.updateBackfillStatus(
                    syncStatus,
                    inserted,
                    "快速history100同步完成，新增"
                            + inserted
                            + "期，当前共"
                            + count
                            + "期|"
                            + latestFreshness.compact());
            store.setState("quick_history_sync_status", syncStatus);
            store.setState("quick_history_sync_inserted", String.valueOf(inserted));
            store.setState(
                    "quick_history_sync_summary",
                    "快速history100同步完成，新增"
                            + inserted
                            + "期|"
                            + latestFreshness.compact());

            broadcastDrawsUpdated(true);
            if(!latestFreshness.accepted){
                lastError="PROVIDER_STALE_NO_ADVANCE|"+latestFreshness.compact();
                heartbeatStateV8().markFetchFailed(latest==null?"":latest.issue,lastError);
                return null;
            }
            return latest;
        } catch (Throwable e) {
            lastError = safeError(e);
            store.updateBackfillStatus("ERROR", inserted, "快速history100同步异常：" + lastError);
            store.setState("quick_history_sync_status", "ERROR");
            store.setState("quick_history_sync_error", lastError);
            broadcastDrawsUpdated(true);

            heartbeatStateV8().markFetchFailed("",lastError);
            return null;
        }
    }

    private void saveLatestDrawState(Models.Draw draw, String source) {
        if (store == null || draw == null || !draw.isValid()) {
            return;
        }

        try {
            String issue = draw.issue == null ? "" : draw.issue;
            String target = dataGuard == null ? "" : dataGuard.buildNextIssue(issue);
            String now = String.valueOf(System.currentTimeMillis());
            java.util.LinkedHashMap<String, String> values =
                    new java.util.LinkedHashMap<String, String>();
            values.put("current_issue", issue);
            values.put("target_issue", target);
            values.put("latest_draw_issue", issue);
            values.put("latest_draw_target_issue", target);
            values.put("latest_draw_numbers", buildDrawNumbersText(draw));
            values.put("latest_draw_open_time", String.valueOf(draw.openTime));
            values.put("latest_draw_updated_at", now);
            values.put("draws_updated_at", now);
            values.put("draws_latest_source", source == null ? "" : source);
            values.put("draw_count", String.valueOf(store.getDrawCount()));

            if (countdownClock != null) {
                values.put("countdown_text", countdownClock.countdownText());
                values.put("fetch_mode_text", countdownClock.fetchModeText());
            }
            store.setStates(values);
        } catch (Throwable e) {
        }
    }

    private String buildDrawNumbersText(Models.Draw draw) {
        if (draw == null) {
            return "";
        }

        StringBuilder sb = new StringBuilder();

        for (int i = 1; i <= AppConfig.POSITION_COUNT; i++) {
            int n = 0;

            try {
                n = draw.getNumberAtPosition(i);
            } catch (Throwable e) {
                n = 0;
            }

            if (sb.length() > 0) {
                sb.append(" ");
            }

            sb.append(UiFormatter.number(n));
        }

        return sb.toString();
    }

    private void settleLatestSafely(Models.Draw authoritativeLatest) {
        if (settlementEngine == null
                || store == null
                || authoritativeLatest == null
                || !authoritativeLatest.isValid()) {
            return;
        }
        if (useR36Runtime()) {
            SettlementEngine.SettlementReport report = null;
            String error = "";
            String issue = safeIssue(authoritativeLatest);
            try {
                RealtimePressureGateR811.pulseLiveRequest();
                SettlementCoordinatorR815.enqueueDraw(
                        store,
                        authoritativeLatest,
                        SettlementCoordinatorR815.Reason.AUTHORITATIVE_LATEST);
                heartbeatStateV8().markSettlementRunning(issue, 1);
                report =
                        SettlementCoordinatorR815.settleNow(
                                store,
                                authoritativeLatest,
                                SettlementCoordinatorR815.Reason.AUTHORITATIVE_LATEST);
            } catch (Throwable e) {
                error = safeError(e);
                lastError = error;
            } finally {
                try {
                    SettlementTaskStoreR36.Terminal terminal =
                            SettlementTaskStoreR36.terminal(store, issue);
                    if (terminal != null) {
                        heartbeatStateV8()
                                .markSettlementTerminal(
                                        issue,
                                        terminal.status,
                                        terminal.error.length() > 0
                                                ? terminal.error
                                                : (report == null ? error : report.detail),
                                        terminal.completedAt > 0L
                                                ? terminal.completedAt
                                                : System.currentTimeMillis());
                        broadcastSettlementUpdated();
                    } else {
                        heartbeatStateV8().markSettlementRunning(issue, 1);
                        if (store != null) {
                            store.setState(
                                    "r367_settlement_pending_detail",
                                    report == null ? error : safe(report.detail));
                            store.setState("r367_settlement_pending_issue", issue);
                        }
                    }
                } catch (Throwable ignore) {
                }
            }
            return;
        }

        try {
            store.setState("settlement_status", "CHECKING");
            try {
                heartbeatStateV8().markSettlementRunning(safeIssue(authoritativeLatest), 0);
            } catch (Throwable ignore) {
            }
            store.setState(
                    "settlement_issue",
                    authoritativeLatest.issue == null ? "" : authoritativeLatest.issue);
            store.setState("settlement_check_at", String.valueOf(System.currentTimeMillis()));
            store.setState("settlement_patch_version", V20_RUNTIME_PATCH_VERSION);
            int oosSettled = 0;
            int v48DiagnosticSettled = 0;
            if (AppConfig.ENABLE_V50_PRIME) {
                V50Models.LearningReport report =
                        new V50FullInformationLearningEngine()
                                .settleLabels(store, authoritativeLatest);
                store.setState(
                        "v50_learning_last",
                        report == null
                                ? "NULL"
                                : report.failureCode.name()
                                        + '|'
                                        + report.detail
                                        + '|'
                                        + report.checksum);
                store.setState(
                        "v50_learning_success", report != null && report.success ? "1" : "0");
            } else if (AppConfig.ENABLE_V51_TIANSHU) {
                V51Repository.SettlementReport v51Settlement =
                        SettlementCoordinatorR815.settleV51Compatibility(
                                store,
                                authoritativeLatest,
                                SettlementCoordinatorR815.Reason.AUTHORITATIVE_LATEST);
                store.setState(
                        "v51_settlement_last",
                        v51Settlement == null
                                ? "NULL"
                                : v51Settlement.detail + '|' + v51Settlement.result);
                store.setState(
                        "v51_settlement_success",
                        v51Settlement != null && v51Settlement.success ? "1" : "0");
                store.setState("v57_b13_settlement_path", "SettlementEngine_ONLY");
                store.setState("v57_b12_legacy_settlement_rescue", "DISABLED_FOR_V51");
                return;
            } else {
                SettlementCoordinatorR815.enqueueDraw(
                        store,
                        authoritativeLatest,
                        SettlementCoordinatorR815.Reason.LEGACY_API);
                oosSettled = 0;
                v48DiagnosticSettled = 0;
                FullInformationLearningEngine.Report v49Learning =
                        new FullInformationLearningEngine()
                                .settleAndLearn(store, authoritativeLatest);
                store.setState(
                        "v49_learning_last", v49Learning == null ? "NULL" : v49Learning.summary());
                store.setState("v48_diagnostic_settled_last", String.valueOf(v48DiagnosticSettled));
                store.setState("v24_oos_settled_count", String.valueOf(oosSettled));
                if (oosSettled > 0) {
                    postMicroWalkForward("新OOS结果已结算:" + safeIssue(authoritativeLatest), true, 800L);
                }
            }

            int repairedTrade = repairMissingTradeForRecentBetDecisions(authoritativeLatest);
            int localSettledBefore =
                    settlePendingRealTradesCatchup(authoritativeLatest, "BEFORE_ENGINE");

            Object report = REFLECTION_NO_METHOD;

            report =
                    invokeOptional(
                            settlementEngine,
                            "settleStrictSinglePositionByLatestDraw",
                            new Class[] {DbStore.class, Models.Draw.class},
                            new Object[] {store, authoritativeLatest});

            if (report == REFLECTION_NO_METHOD) {
                report =
                        invokeOptional(
                                settlementEngine,
                                "settleRealTradesByLatestDraw",
                                new Class[] {DbStore.class, Models.Draw.class},
                                new Object[] {store, authoritativeLatest});
            }

            if (report == REFLECTION_NO_METHOD) {
                report =
                        invokeOptional(
                                settlementEngine,
                                "settleFastByLatestDraw",
                                new Class[] {DbStore.class, Models.Draw.class},
                                new Object[] {store, authoritativeLatest});
            }

            if (report == REFLECTION_NO_METHOD) {
                report =
                        invokeOptional(
                                settlementEngine,
                                "fastSettle",
                                new Class[] {DbStore.class, Models.Draw.class},
                                new Object[] {store, authoritativeLatest});
            }

            if (report == REFLECTION_NO_METHOD) {
                report =
                        invokeOptional(
                                settlementEngine,
                                "settleLatest",
                                new Class[] {DbStore.class, Models.Draw.class},
                                new Object[] {store, authoritativeLatest});
            }

            if (report == REFLECTION_NO_METHOD) {
                report =
                        invokeOptional(
                                settlementEngine,
                                "settleAfterDrawSaved",
                                new Class[] {DbStore.class, Models.Draw.class},
                                new Object[] {store, authoritativeLatest});
            }

            int localSettledAfter =
                    settlePendingRealTradesCatchup(authoritativeLatest, "AFTER_ENGINE");
            int localSettled = localSettledBefore + localSettledAfter;

            if (report == REFLECTION_NO_METHOD || report == null) {
                if (localSettled > 0 || repairedTrade > 0) {
                    String localSummary =
                            "本地结算兜底完成：补建Trade="
                                    + repairedTrade
                                    + "，严格单位置K3补结算="
                                    + localSettled
                                    + "，latest="
                                    + safeIssue(authoritativeLatest);
                    store.setState("settlement_status", localSettled > 0 ? "OK" : "IDLE");
                    store.setState("settlement_summary", localSummary);
                    store.setState("last_settlement_detail", localSummary);
                    store.setState(
                            "settlement_updated_at", String.valueOf(System.currentTimeMillis()));
                    if (localSettled > 0) {
                        store.setState(
                                "last_settlement_success_at",
                                String.valueOf(System.currentTimeMillis()));
                        store.setState(
                                "last_settlement_success_issue", safeIssue(authoritativeLatest));
                        store.setState(
                                "background_business_health",
                                AppConfig.BUSINESS_SETTLEMENT_SUCCEEDED);
                        try {
                            heartbeatStateV8()
                                    .markSettlementSuccess(safeIssue(authoritativeLatest));
                        } catch (Throwable ignore) {
                        }
                    } else {
                        store.setState(
                                "background_business_health", AppConfig.BUSINESS_SETTLEMENT_IDLE);
                        try {
                            heartbeatStateV8().markSettlementIdle(safeIssue(authoritativeLatest));
                        } catch (Throwable ignore) {
                        }
                    }
                    store.setState("settlement_scope", "STRICT_SINGLE_POSITION_K3_REAL_TRADE_ONLY");
                    refreshRuntimeStatsAfterSettlement();
                    if (localSettled > 0) {
                        broadcastSettlementUpdated();
                    } else {
                        broadcastSimpleStateLimited(false);
                    }
                    postRealBetDiagnosisRefresh(
                            "本地结算兜底后刷新真实BET复盘诊断", REAL_BET_DIAG_AFTER_SETTLEMENT_DELAY_MS, true);
                    return;
                }

                store.setState("settlement_status", "SKIP");
                store.setState(
                        "settlement_summary", "结算跳过：SettlementEngine没有可用入口，且本地兜底未发现可结算真实BET");
                store.setState("settlement_updated_at", String.valueOf(System.currentTimeMillis()));
                postRealBetDiagnosisRefresh(
                        "结算检查完成后刷新真实BET复盘诊断", REAL_BET_DIAG_IDLE_DELAY_MS, false);
                return;
            }

            String summary = buildSettlementInvokeSummary(report);
            int settled = readInt(report, "settledCount", 0);
            int waiting = readInt(report, "waitingCount", 0);
            int open = readInt(report, "openCount", 0);
            int ok = readInt(report, "ok", 1);

            settled += localSettled;

            if (ok == 0) {
                store.setState("settlement_status", localSettled > 0 ? "OK" : "ERROR");
            } else if (settled > 0) {
                store.setState("settlement_status", "OK");
            } else if (waiting > 0 || open > 0) {
                store.setState("settlement_status", "WAIT");
            } else {
                store.setState("settlement_status", "IDLE");
            }

            summary =
                    appendReason(
                            summary,
                            "结算优先补丁：补建Trade="
                                    + repairedTrade
                                    + "，本地兜底结算="
                                    + localSettled
                                    + "，latest="
                                    + safeIssue(authoritativeLatest));
            store.setState("settlement_summary", summary);
            store.setState("last_settlement_detail", summary);
            store.setState("settlement_updated_at", String.valueOf(System.currentTimeMillis()));
            if (settled > 0) {
                store.setState(
                        "last_settlement_success_at", String.valueOf(System.currentTimeMillis()));
                store.setState("last_settlement_success_issue", safeIssue(authoritativeLatest));
                store.setState(
                        "background_business_health", AppConfig.BUSINESS_SETTLEMENT_SUCCEEDED);
                try {
                    heartbeatStateV8().markSettlementSuccess(safeIssue(authoritativeLatest));
                } catch (Throwable ignore) {
                }
            } else if (waiting > 0 || open > 0) {
                store.setState("background_business_health", AppConfig.BUSINESS_SETTLEMENT_RUNNING);
                try {
                    heartbeatStateV8()
                            .markSettlementRunning(
                                    safeIssue(authoritativeLatest), Math.max(waiting, open));
                } catch (Throwable ignore) {
                }
            } else {
                store.setState("background_business_health", AppConfig.BUSINESS_SETTLEMENT_IDLE);
                try {
                    heartbeatStateV8().markSettlementIdle(safeIssue(authoritativeLatest));
                } catch (Throwable ignore) {
                }
            }
            store.setState("settlement_scope", "STRICT_SINGLE_POSITION_K3_REAL_TRADE_ONLY");
            refreshRuntimeStatsAfterSettlement();

            if (settled > 0) {
                broadcastSettlementUpdated();
            } else {
                broadcastSimpleStateLimited(false);
            }
            postRealBetDiagnosisRefresh(
                    "SettlementEngine结算后刷新真实BET复盘诊断",
                    REAL_BET_DIAG_AFTER_SETTLEMENT_DELAY_MS,
                    true);
        } catch (Throwable e) {
            lastError = safeError(e);
            if (store != null) {
                store.setState("settlement_status", "ERROR");
                store.setState("last_settlement_error", lastError);
                store.setState("settlement_updated_at", String.valueOf(System.currentTimeMillis()));
                try {
                    heartbeatStateV8()
                            .markSettlementFailed(safeIssue(authoritativeLatest), lastError);
                } catch (Throwable ignore) {
                }
            }
        }
    }

    private int repairMissingTradeForRecentBetDecisions(Models.Draw latestDraw) {
        return 0; // Historical absence is recorded by audit, never filled after the draw.
    }

    private int settlePendingRealTradesCatchup(Models.Draw latestDraw, String stage) {
        if (store == null || latestDraw == null || !latestDraw.isValid()) {
            return 0;
        }
        int settled = 0;
        int checked = 0;
        try {
            List<Models.Trade> list = store.getOpenTrades(SETTLEMENT_CATCHUP_OPEN_LIMIT);
            if (list == null || list.size() == 0) {
                return 0;
            }
            for (int i = 0; i < list.size(); i++) {
                Models.Trade t = list.get(i);
                if (t == null || t.targetIssue == null || t.targetIssue.length() == 0) {
                    continue;
                }
                if (!isIssueDue(t.targetIssue, latestDraw.issue)) {
                    continue;
                }
                checked++;
                Models.Draw draw = null;
                if (sameText(t.targetIssue, latestDraw.issue)) {
                    draw = latestDraw;
                } else {
                    try {
                        draw = store.getDrawByIssue(t.targetIssue);
                    } catch (Throwable e) {
                        draw = null;
                    }
                }
                if (draw == null || !draw.isValid()) {
                    continue;
                }
                int r = store.settleTradeByDraw(t, draw);
                if (r > 0) {
                    settled++;
                }
            }
            if (checked > 0 || settled > 0) {
                store.setState("settlement_catchup_stage", stage == null ? "" : stage);
                store.setState("settlement_catchup_checked", String.valueOf(checked));
                store.setState("settlement_catchup_settled", String.valueOf(settled));
                store.setState(
                        "settlement_catchup_summary",
                        "结算优先补扫：stage="
                                + (stage == null ? "" : stage)
                                + "，检查="
                                + checked
                                + "，结算="
                                + settled
                                + "，latest="
                                + safeIssue(latestDraw));
                store.setState("settlement_catchup_at", String.valueOf(System.currentTimeMillis()));
            }
        } catch (Throwable e) {
            try {
                store.setState("settlement_catchup_error", safeError(e));
            } catch (Throwable ignore) {
            }
        }
        return settled;
    }

    private void refreshRuntimeStatsAfterSettlement() {
        if (store == null) {
            return;
        }
        try {
            Object stats = null;
            Object r =
                    invokeOptional(store, "rebuildRuntimeStats", new Class[] {}, new Object[] {});
            if (r == REFLECTION_NO_METHOD) {
                r = invokeOptional(store, "refreshRuntimeStats", new Class[] {}, new Object[] {});
            }
            if (r == REFLECTION_NO_METHOD) {
                r = invokeOptional(store, "updateRuntimeStats", new Class[] {}, new Object[] {});
            }
            try {
                stats = store.getRuntimeStats();
            } catch (Throwable e) {
                stats = null;
            }
            if (stats != null) {
                store.setState("settlement_runtime_stats_refreshed", "1");
                store.setState(
                        "settlement_runtime_stats_at", String.valueOf(System.currentTimeMillis()));
            }
        } catch (Throwable e) {
        }
    }

    private boolean isIssueDue(String targetIssue, String latestIssue) {
        if (targetIssue == null
                || targetIssue.length() == 0
                || latestIssue == null
                || latestIssue.length() == 0) {
            return false;
        }
        return compareIssueText(targetIssue, latestIssue) <= 0;
    }

    private int compareIssueText(String a, String b) {
        String x = a == null ? "" : a.trim();
        String y = b == null ? "" : b.trim();
        if (x.length() == 0 && y.length() == 0) {
            return 0;
        }
        if (x.length() == 0) {
            return -1;
        }
        if (y.length() == 0) {
            return 1;
        }
        try {
            long lx = Long.parseLong(x);
            long ly = Long.parseLong(y);
            if (lx < ly) {
                return -1;
            }
            if (lx > ly) {
                return 1;
            }
            return 0;
        } catch (Throwable e) {
            return x.compareTo(y);
        }
    }

    private String safeIssue(Models.Draw d) {
        if (d == null || d.issue == null) {
            return "";
        }
        return d.issue;
    }

    private String buildSettlementInvokeSummary(Object report) {
        if (report == null) {
            return "结算完成：无返回详情";
        }

        String detail = readString(report, "detail", "");

        if (detail == null || detail.length() == 0) {
            try {
                Method m = report.getClass().getMethod("toSummaryText", new Class[] {});
                Object value = m.invoke(report, new Object[] {});
                if (value != null) {
                    detail = String.valueOf(value);
                }
            } catch (Throwable e) {
            }
        }

        if (detail == null || detail.length() == 0 || detail.indexOf("@") >= 0) {
            int open = readInt(report, "openCount", 0);
            int settled = readInt(report, "settledCount", 0);
            int wait = readInt(report, "waitingCount", 0);
            int win = readInt(report, "winCount", 0);
            int miss = readInt(report, "missCount", 0);
            double pnl = readDouble(report, "pnl", 0.0d);

            detail =
                    "结算报告：open="
                            + open
                            + " settled="
                            + settled
                            + " wait="
                            + wait
                            + " win="
                            + win
                            + " miss="
                            + miss
                            + " pnl="
                            + UiFormatter.money(pnl);
        }

        if (detail.length() > 500) {
            detail = detail.substring(0, 500);
        }

        return detail;
    }

    private void postB07StartupBackfill() {
        if (!running || backfillHandler == null || b07StartupBackfillPosted) return;
        b07StartupBackfillPosted = true;
        backfillHandler.postDelayed(
                new Runnable() {
                    @Override
                    public void run() {
                        b07StartupBackfillPosted = false;
                        if (running) postBackfillCheck(true, "B07启动强制回补");
                    }
                },
                AppConfig.B07_STARTUP_BACKFILL_DELAY_MS);
    }

    private void postBackfillCheck(final boolean force, final String reason) {
        if (!running || backfillHandler == null || store == null || apiClient == null) {
            return;
        }

        if (!force && shouldDeferHeavyBackgroundNow()) {
            return;
        }

        if (force) {
            long now = System.currentTimeMillis();
            if (lastForcedBackfill100At > 0L
                    && now - lastForcedBackfill100At < REALTIME_FORCE_BACKFILL_MIN_INTERVAL_MS) {
                return;
            }
            lastForcedBackfill100At = now;
        }

        backfillHandler.post(
                new Runnable() {
                    @Override
                    public void run() {
                        runBackfillChannelOnce(force, reason);
                    }
                });
    }

    private void runBackfillChannelOnce(boolean force, String reason) {
        if (!running || backfillBusy) {
            return;
        }

        if (!force && shouldDeferHeavyBackgroundNow()) {
            return;
        }

        backfillBusy = true;

        try {
            long now = System.currentTimeMillis();

            if (!force) {
                if (lastBackfillAt > 0L && now - lastBackfillAt < BACKFILL_INTERVAL_MS) {
                    return;
                }

                if (lastBackfillFailedAt > 0L
                        && now - lastBackfillFailedAt < BACKFILL_FAST_RETRY_MS) {
                    return;
                }
            }

            boolean need = force;

            if (!startupBackfillDone) {
                need = true;
                reason = "启动首次回补";
            }

            if (store.getDrawRemainTo240() > 0) {
                need = true;
                reason = "历史不足30期受控启动回补";
            }

            if (store.getRecentIssueGapCount(300) > 0) {
                need = true;
                reason = "发现期号缺口回补";
            }

            if (!need) {
                store.updateBackfillStatus("SKIP", 0, "本轮无需回补");
                return;
            }

            performBackfill(reason, force);

            postAnalysisForLatest("回补完成后补齐最新正式预测", true);
            postLearningTick("回补完成后后台补学历史片段", true);
        } finally {
            backfillBusy = false;
        }
    }

    private void performBackfill(String reason, boolean forceFullDateBackfill) {
        if (store == null || apiClient == null) {
            return;
        }

        long now = System.currentTimeMillis();

        int totalInserted = 0;
        int latest100Count = 0;
        int todayCount = 0;
        int yesterdayCount = 0;
        int dayMinus2Count = 0;
        int dayMinus3Count = 0;
        boolean b07ForcedBackfill =
                forceFullDateBackfill
                        || (reason != null && reason.indexOf("B07启动强制回补") >= 0);

        store.updateBackfillStatus("RUNNING", 0, reason == null ? "正在回补" : reason);
        broadcastSimpleStateLimited(false);

        try {
            Models.ApiResult latest100 = apiClient.fetchHistory(AppConfig.BACKFILL_LATEST_LIMIT);

            if (latest100 != null
                    && latest100.ok
                    && latest100.draws != null
                    && latest100.draws.size() > 0) {
                latest100Count = latest100.draws.size();
                totalInserted += store.saveDraws(latest100.draws);

                if (latest100.apiLine != null && latest100.apiLine.length() > 0) {
                    store.setState("api_line", latest100.apiLine);
                }

                Models.Draw latest = store.getLatestDraw();
                if (latest != null && latest.isValid()) {
                    saveLatestDrawState(latest, "backfill_latest100");
                    broadcastDrawsUpdated(true);
                    settleLatestSafely(latest);

                    if (!shouldDeferHeavyBackgroundNow()) {
                        postTeacherMatrixForDraw(
                                latest,
                                "回补latest100后后台同步教师矩阵",
                                false,
                                TEACHER_MATRIX_IDLE_DELAY_MS);
                        postV20CauseChainForDraw(
                                latest,
                                "回补latest100后后台同步V20因链",
                                false,
                                V20_CAUSE_CHAIN_IDLE_DELAY_MS);
                    }
                }
            }

            if (b07ForcedBackfill || !shouldDeferHeavyBackgroundNow()) {
                Models.ApiResult today = apiClient.fetchTodayBackfill();

                if (today != null && today.ok && today.draws != null && today.draws.size() > 0) {
                    todayCount = today.draws.size();
                    totalInserted += store.saveDraws(today.draws);

                    if (today.apiLine != null && today.apiLine.length() > 0) {
                        store.setState("api_line", today.apiLine);
                    }
                }
            }

            if ((b07ForcedBackfill || !shouldDeferHeavyBackgroundNow())
                    && (b07ForcedBackfill || store.getDrawRemainTo240() > 0 || store.getRecentIssueGapCount(300) > 0)) {
                Models.ApiResult yesterday = apiClient.fetchYesterdayBackfill();

                if (yesterday != null
                        && yesterday.ok
                        && yesterday.draws != null
                        && yesterday.draws.size() > 0) {
                    yesterdayCount = yesterday.draws.size();
                    totalInserted += store.saveDraws(yesterday.draws);

                    if (yesterday.apiLine != null && yesterday.apiLine.length() > 0) {
                        store.setState("api_line", yesterday.apiLine);
                    }
                }
            }

            if ((b07ForcedBackfill || !shouldDeferHeavyBackgroundNow())
                    && (b07ForcedBackfill || store.getDrawRemainTo240() > 0 || store.getRecentIssueGapCount(300) > 0)) {
                // fetchTwoDayBackfill() only concatenates today and yesterday.  It never fetched
                // the calendar day at -2, so a rolling three-day outage could look contiguous in
                // the newest 300 rows while most of the requested recovery window stayed absent.
                Models.ApiResult dayMinus2 =
                        apiClient.fetchBackfillDate(apiClient.offsetDateText(-2));
                if (dayMinus2 != null && dayMinus2.ok && dayMinus2.draws != null
                        && dayMinus2.draws.size() > 0) {
                    dayMinus2Count = dayMinus2.draws.size();
                    totalInserted += store.saveDraws(dayMinus2.draws);
                    if (dayMinus2.apiLine != null && dayMinus2.apiLine.length() > 0) {
                        store.setState("api_line", dayMinus2.apiLine);
                    }
                }
            }

            // One additional UTC date protects the leading edge of a 72-hour rolling window.
            // Forced startup repair always attempts it; normal maintenance does so only for a gap.
            if ((b07ForcedBackfill || !shouldDeferHeavyBackgroundNow())
                    && (b07ForcedBackfill || store.getRecentIssueGapCount(300) > 0)) {
                Models.ApiResult dayMinus3 =
                        apiClient.fetchBackfillDate(apiClient.offsetDateText(-3));
                if (dayMinus3 != null && dayMinus3.ok && dayMinus3.draws != null
                        && dayMinus3.draws.size() > 0) {
                    dayMinus3Count = dayMinus3.draws.size();
                    totalInserted += store.saveDraws(dayMinus3.draws);
                    if (dayMinus3.apiLine != null && dayMinus3.apiLine.length() > 0) {
                        store.setState("api_line", dayMinus3.apiLine);
                    }
                }
            }

            Models.Draw latestAfter = store.getLatestDraw();
            if (latestAfter != null && latestAfter.isValid()) {
                saveLatestDrawState(latestAfter, "backfill_done");
                settleLatestSafely(latestAfter);

                if (!shouldDeferHeavyBackgroundNow()) {
                    postTeacherMatrixForDraw(
                            latestAfter, "回补完成后后台确认教师矩阵", false, TEACHER_MATRIX_IDLE_DELAY_MS);
                    postV20CauseChainForDraw(
                            latestAfter, "回补完成后后台确认V20因链", false, V20_CAUSE_CHAIN_IDLE_DELAY_MS);
                }
            }

            int afterTotal = store.getDrawCount();
            int afterGap = store.getRecentIssueGapCount(300);
            int afterRemain = store.getDrawRemainTo240();

            lastBackfillAt = now;
            startupBackfillDone = true;

            String msg =
                    "最新100返回"
                            + latest100Count
                            + "条，今日返回"
                            + todayCount
                            + "条，昨日返回"
                            + yesterdayCount
                            + "条，前第2日返回"
                            + dayMinus2Count
                            + "条，前第3日返回"
                            + dayMinus3Count
                            + "条；新增"
                            + totalInserted
                            + "期；当前共"
                            + afterTotal
                            + "期，距240还差"
                            + afterRemain
                            + "期";

            if (afterGap > 0) {
                msg += "；近300期仍缺" + afterGap + "期";
            } else {
                msg += "；近300期缺口已补齐";
            }

            if (afterGap > 0 && totalInserted <= 0) {
                lastBackfillFailedAt = now;
                store.updateBackfillStatus("ERROR", totalInserted, "回补接口未补入新数据，" + msg);
            } else {
                lastBackfillFailedAt = 0L;
                store.updateBackfillStatus("OK", totalInserted, msg);
            }

            broadcastDrawsUpdated(true);
        } catch (Throwable e) {
            lastBackfillFailedAt = now;
            lastError = safeError(e);

            store.updateBackfillStatus("ERROR", totalInserted, "回补异常：" + lastError);
            broadcastSimpleStateLimited(true);
        }
    }

    private boolean shouldBackfillAfterLatest(boolean isNew, boolean recoveredFromNetworkBreak) {
        if (store == null) {
            return false;
        }

        if (shouldDeferHeavyBackgroundNow()) {
            return false;
        }

        if (!startupBackfillDone) {
            return true;
        }

        if (recoveredFromNetworkBreak) {
            return true;
        }

        if (store.getDrawRemainTo240() > 0) {
            return true;
        }

        if (store.getRecentIssueGapCount(300) > 0) {
            return true;
        }

        long now = System.currentTimeMillis();

        return isNew && now - lastBackfillAt >= BACKFILL_INTERVAL_MS;
    }

    private void postAutoBacktestLazyIfIdle(final String reason) {
        if (!running || backtestHandler == null || backtestEngine == null || store == null) {
            return;
        }

        if (autoBacktestPostPending) {
            return;
        }

        long now = System.currentTimeMillis();
        if (lastAutoBacktestTryAt > 0L
                && now - lastAutoBacktestTryAt < AUTO_BACKTEST_MIN_TRY_INTERVAL_MS) {
            return;
        }

        if (shouldDeferHeavyBackgroundNow() || isRealtimeUnsafeForAutoBacktest()) {
            return;
        }

        autoBacktestPostPending = true;
        lastAutoBacktestTryAt = now;

        backtestHandler.postDelayed(
                new Runnable() {
                    @Override
                    public void run() {
                        autoBacktestPostPending = false;

                        if (!running || store == null) {
                            return;
                        }

                        if (analysisBusy
                                || fetchBusy
                                || backfillBusy
                                || learningBusy
                                || shouldDeferHeavyBackgroundNow()) {
                            return;
                        }

                        if (isRealtimeUnsafeForAutoBacktest()) {
                            return;
                        }

                        try {
                            List<Models.Draw> history = store.getRecentDraws(AppConfig.WINDOW_3000);
                            runBacktestIfNeeded(history);
                        } catch (Throwable e) {
                            lastError = safeError(e);
                            if (store != null) {
                                store.setState("auto_backtest_lazy_error", lastError);
                            }
                        }
                    }
                },
                AUTO_BACKTEST_DELAY_AFTER_DRAW_MS);
    }

    private void runBacktestIfNeeded(List<Models.Draw> history) {
        if (history == null || history.size() < AppConfig.MIN_DRAWS_ANALYZE + 2) {
            return;
        }

        if (backtestHandler == null || backtestEngine == null || store == null) {
            return;
        }

        if (backtestBusy || analysisBusy || fetchBusy || backfillBusy) {
            return;
        }

        if (shouldDeferHeavyBackgroundNow()) {
            return;
        }

        if (isRealtimeUnsafeForAutoBacktest()) {
            return;
        }

        long now = System.currentTimeMillis();

        String mode = chooseAutoBacktestMode(history, now);
        if (mode == null || mode.length() == 0) {
            return;
        }

        ArrayList<Models.Draw> snapshot = buildHistorySnapshot(history, AppConfig.WINDOW_3000);

        if (snapshot.size() < AppConfig.MIN_DRAWS_ANALYZE + 2) {
            return;
        }

        scheduleBacktest(snapshot, mode, false, "自动后台轻量回测");
    }

    private String chooseAutoBacktestMode(List<Models.Draw> history, long now) {
        int size = history == null ? 0 : history.size();

        if (size < AppConfig.MIN_DRAWS_ANALYZE + 2) {
            return "";
        }

        if (now - lastNormalBacktestAt >= AUTO_NORMAL_BACKTEST_INTERVAL_MS) {
            return AppConfig.BACKTEST_MODE_NORMAL;
        }

        if (now - lastFastBacktestAt >= AUTO_FAST_BACKTEST_INTERVAL_MS) {
            return AppConfig.BACKTEST_MODE_FAST;
        }

        if (size >= AppConfig.BACKTEST_DEEP_WINDOW + AppConfig.MIN_DRAWS_ANALYZE + 2
                && now - lastDeepBacktestAt >= AUTO_DEEP_BACKTEST_INTERVAL_MS) {
            return AppConfig.BACKTEST_MODE_NORMAL;
        }

        return "";
    }

    private void runManualBacktest(String mode) {
        if (store == null || backtestEngine == null || backtestHandler == null) {
            return;
        }

        // Prepare the history snapshot and persisted task state on the existing worker too.
        // Android invokes onStartCommand on the UI thread, even for a background service.
        if (android.os.Looper.myLooper() != backtestHandler.getLooper()) {
            final String requestedMode = mode;
            backtestHandler.post(new Runnable() {
                @Override public void run() { runManualBacktest(requestedMode); }
            });
            return;
        }

        mode = AppConfig.normalizeBacktestMode(mode);

        if (backtestBusy) {
            setBacktestState("BUSY", mode, 0, 0, "已有回测正在运行", "已有回测正在运行");
            saveManualTaskState(mode, "BUSY", 0, "已有回测正在运行", "");
            broadcastBacktestUpdated();
            return;
        }

        currentManualTaskId = "MANUAL_" + System.currentTimeMillis();
        currentManualMode = mode;
        currentManualStartedAt = System.currentTimeMillis();

        int historyLimit = AppConfig.WINDOW_3000;

        if (AppConfig.BACKTEST_MODE_FULL.equals(mode)) {
            historyLimit = getSafeFullHistoryLimit();
        }

        List<Models.Draw> history = store.getRecentDraws(historyLimit);
        ArrayList<Models.Draw> snapshot = buildHistorySnapshot(history, historyLimit);

        if (snapshot.size() < AppConfig.MIN_DRAWS_ANALYZE + 2) {
            setBacktestState("SKIP", mode, 0, 0, "历史不足，无法手动回测", "历史不足，无法手动回测");
            store.updateBackfillStatus("SKIP", 0, "历史不足，无法手动回测");
            saveManualTaskState(mode, "SKIP", 0, "历史不足，无法手动回测", "");
            broadcastBacktestUpdated();
            return;
        }

        String text;
        if (AppConfig.BACKTEST_MODE_FULL.equals(mode)) {
            text = "手动完整深度回测已提交：本机安全分层模式，只跑W50/W120/W240/W500；实时出号优先";
        } else {
            text = "手动回测已提交，独立回测线程运行；实时出号优先";
        }

        scheduleBacktest(snapshot, mode, true, text);
    }

    private int getSafeFullHistoryLimit() {
        int limit = SAFE_FULL_HISTORY_LIMIT;

        if (AppConfig.MANUAL_BACKTEST_MAX_WINDOW > 0
                && AppConfig.MANUAL_BACKTEST_MAX_WINDOW < limit) {
            limit = AppConfig.MANUAL_BACKTEST_MAX_WINDOW + AppConfig.MIN_DRAWS_ANALYZE + 10;
        }

        if (limit < SAFE_FULL_W500 + AppConfig.MIN_DRAWS_ANALYZE + 10) {
            limit = SAFE_FULL_W500 + AppConfig.MIN_DRAWS_ANALYZE + 10;
        }

        return limit;
    }

    private void scheduleBacktest(
            final ArrayList<Models.Draw> snapshot,
            final String modeValue,
            final boolean manual,
            final String reason) {

        if (backtestBusy || backtestHandler == null || snapshot == null || snapshot.size() == 0) {
            return;
        }

        final String mode = AppConfig.normalizeBacktestMode(modeValue);

        backtestBusy = true;
        backtestStoppedForRealtime = false;
        backtestStoppedProgress = 0;
        backtestStoppedWindow = 0;
        backtestStoppedSummary = "";

        lastBacktestAt = System.currentTimeMillis();
        lastBacktestMode = mode;

        if (AppConfig.BACKTEST_MODE_FAST.equals(mode)) {
            lastFastBacktestAt = lastBacktestAt;
        } else if (AppConfig.BACKTEST_MODE_NORMAL.equals(mode)) {
            lastNormalBacktestAt = lastBacktestAt;
        } else if (AppConfig.BACKTEST_MODE_FULL.equals(mode)) {
            lastDeepBacktestAt = lastBacktestAt;
        }

        setBacktestState("RUNNING", mode, 0, 0, reason == null ? "回测启动" : reason, "");

        if (manual) {
            saveManualTaskState(mode, "RUNNING", 0, reason, "");
        }

        broadcastBacktestUpdated();

        backtestHandler.post(
                new Runnable() {
                    @Override
                    public void run() {
                        long startedAt = System.currentTimeMillis();

                        try {
                            if (AppConfig.BACKTEST_MODE_FULL.equals(mode)) {
                                runFullBacktestJob(snapshot, manual);
                            } else if (AppConfig.BACKTEST_MODE_NORMAL.equals(mode)) {
                                runNormalBacktestJob(snapshot);
                            } else {
                                runFastBacktestJob(snapshot);
                            }

                            long cost = System.currentTimeMillis() - startedAt;
                            backtestHeartbeatRunning = false;

                            if (backtestStoppedForRealtime) {
                                String summary =
                                        backtestStoppedSummary == null
                                                        || backtestStoppedSummary.length() == 0
                                                ? "回测已让路实时分析，避免卡住出号"
                                                : backtestStoppedSummary;

                                setBacktestState(
                                        "SKIP",
                                        mode,
                                        backtestStoppedProgress,
                                        backtestStoppedWindow,
                                        summary,
                                        "");

                                if (manual) {
                                    saveManualTaskState(
                                            mode, "SKIP", backtestStoppedProgress, summary, "");
                                }
                            } else {
                                String finishText =
                                        AppConfig.BACKTEST_MODE_FULL.equals(mode)
                                                ? "安全分层深度回测完成，用时 " + cost + "ms；已更新学习/回测报告"
                                                : "回测完成，用时 " + cost + "ms";

                                setBacktestState("OK", mode, 100, 0, finishText, "");

                                if (manual) {
                                    saveManualTaskState(mode, "OK", 100, finishText, "");
                                }
                            }
                        } catch (Throwable e) {
                            String err = safeError(e);

                            backtestHeartbeatRunning = false;
                            setBacktestState("ERROR", mode, 0, 0, "回测异常", err);

                            if (manual) {
                                saveManualTaskState(mode, "ERROR", 0, "手动回测异常", err);
                            }
                        } finally {
                            backtestBusy = false;
                            backtestHeartbeatRunning = false;
                            broadcastBacktestUpdated();
                            postLearningTick("回测完成后后台学习同步", false);
                        }
                    }
                });
    }

    private void runFastBacktestJob(ArrayList<Models.Draw> snapshot) {
        if (!saveBacktestReportSafely(
                snapshot, AppConfig.BACKTEST_MICRO_WINDOW, AppConfig.BACKTEST_MODE_FAST, 25)) {
            return;
        }

        cooperativeBacktestPause(AppConfig.BACKTEST_COOPERATIVE_SLEEP_FAST_MS);

        if (!isRealtimeUnsafeForAutoBacktest() && !shouldDeferHeavyBackgroundNow()) {
            saveBacktestReportSafely(
                    snapshot, AppConfig.BACKTEST_MID_WINDOW, AppConfig.BACKTEST_MODE_FAST, 50);
        }
    }

    private void runNormalBacktestJob(ArrayList<Models.Draw> snapshot) {
        if (!saveBacktestReportSafely(
                snapshot, AppConfig.BACKTEST_MICRO_WINDOW, AppConfig.BACKTEST_MODE_FAST, 15)) {
            return;
        }

        cooperativeBacktestPause(AppConfig.BACKTEST_COOPERATIVE_SLEEP_FAST_MS);

        if (isRealtimeUnsafeForAutoBacktest() || shouldDeferHeavyBackgroundNow()) {
            return;
        }

        if (!saveBacktestReportSafely(
                snapshot, AppConfig.BACKTEST_MID_WINDOW, AppConfig.BACKTEST_MODE_NORMAL, 35)) {
            return;
        }

        cooperativeBacktestPause(AppConfig.BACKTEST_COOPERATIVE_SLEEP_NORMAL_MS);

        if (isRealtimeUnsafeForAutoBacktest() || shouldDeferHeavyBackgroundNow()) {
            return;
        }

        if (snapshot.size() >= AppConfig.BACKTEST_BIG_WINDOW + AppConfig.MIN_DRAWS_ANALYZE + 2) {
            if (!saveBacktestReportSafely(
                    snapshot, AppConfig.BACKTEST_BIG_WINDOW, AppConfig.BACKTEST_MODE_NORMAL, 65)) {
                return;
            }
            cooperativeBacktestPause(AppConfig.BACKTEST_COOPERATIVE_SLEEP_NORMAL_MS);
        }

        if (isRealtimeUnsafeForAutoBacktest() || shouldDeferHeavyBackgroundNow()) {
            return;
        }

        if (snapshot.size() >= AppConfig.BACKTEST_DEEP_WINDOW + AppConfig.MIN_DRAWS_ANALYZE + 2) {
            saveBacktestReportSafely(
                    snapshot, AppConfig.BACKTEST_DEEP_WINDOW, AppConfig.BACKTEST_MODE_NORMAL, 90);
            lastDeepBacktestAt = System.currentTimeMillis();
        }
    }

    private void runFullBacktestJob(ArrayList<Models.Draw> snapshot, boolean manual) {
        if (!manual) {
            return;
        }

        if (snapshot == null || snapshot.size() < AppConfig.MIN_DRAWS_ANALYZE + 2) {
            return;
        }

        if (!saveBacktestReportSafely(
                snapshot,
                SAFE_FULL_W50,
                AppConfig.BACKTEST_MODE_FULL,
                20,
                AppConfig.BACKTEST_MODE_FAST)) {
            return;
        }

        cooperativeBacktestPause(AppConfig.BACKTEST_COOPERATIVE_SLEEP_FAST_MS);

        if (isRealtimeUnsafeForAutoBacktest() || shouldDeferHeavyBackgroundNow()) {
            return;
        }

        if (!saveBacktestReportSafely(
                snapshot,
                SAFE_FULL_W120,
                AppConfig.BACKTEST_MODE_FULL,
                40,
                AppConfig.BACKTEST_MODE_FAST)) {
            return;
        }

        cooperativeBacktestPause(AppConfig.BACKTEST_COOPERATIVE_SLEEP_FAST_MS);

        if (isRealtimeUnsafeForAutoBacktest() || shouldDeferHeavyBackgroundNow()) {
            return;
        }

        if (!saveBacktestReportSafely(
                snapshot,
                SAFE_FULL_W240,
                AppConfig.BACKTEST_MODE_FULL,
                70,
                AppConfig.BACKTEST_MODE_NORMAL)) {
            return;
        }

        cooperativeBacktestPause(AppConfig.BACKTEST_COOPERATIVE_SLEEP_NORMAL_MS);

        if (isRealtimeUnsafeForAutoBacktest() || shouldDeferHeavyBackgroundNow()) {
            return;
        }

        if (snapshot.size() >= SAFE_FULL_W500 + AppConfig.MIN_DRAWS_ANALYZE + 2) {
            saveBacktestReportSafely(
                    snapshot,
                    SAFE_FULL_W500,
                    AppConfig.BACKTEST_MODE_FULL,
                    95,
                    AppConfig.BACKTEST_MODE_NORMAL);
        }

        try {
            if (store != null) {
                store.setState("teacher_snapshot_status", "OK");
                store.setState(
                        "teacher_snapshot_summary",
                        "安全分层深度回测已完成：W50/W120/W240/W500；大历史保留用于滚动抽样，不再阻塞实时出号");
                store.setState("teacher_confidence", "SAFE_LAYERED");
            }
        } catch (Throwable e) {
        }
    }

    private boolean saveBacktestReportSafely(
            ArrayList<Models.Draw> snapshot, int window, String mode, int progress) {
        return saveBacktestReportSafely(snapshot, window, mode, progress, mode);
    }

    private boolean saveBacktestReportSafely(
            ArrayList<Models.Draw> snapshot,
            int window,
            String displayMode,
            int progress,
            String engineModeValue) {
        if (store == null || backtestEngine == null || snapshot == null || snapshot.size() == 0) {
            return false;
        }

        if (window <= 0) {
            return false;
        }

        if (snapshot.size() < AppConfig.MIN_DRAWS_ANALYZE + 2) {
            return false;
        }

        int maxWindow = snapshot.size() - AppConfig.MIN_DRAWS_ANALYZE - 2;

        if (maxWindow <= 0) {
            return false;
        }

        if (window > maxWindow) {
            window = maxWindow;
        }

        String normalizedDisplayMode = AppConfig.normalizeBacktestMode(displayMode);
        String engineMode = AppConfig.normalizeBacktestMode(engineModeValue);

        if (engineMode == null || engineMode.length() == 0) {
            engineMode = normalizedDisplayMode;
        }

        int startProgress = stageStartProgress(progress);
        boolean manualFull = AppConfig.BACKTEST_MODE_FULL.equals(normalizedDisplayMode);

        if (shouldDeferHeavyBackgroundNow()) {
            boolean cleared =
                    waitRealtimeAnalysisClear(
                            normalizedDisplayMode, startProgress, window, manualFull);

            if (!cleared) {
                backtestStoppedForRealtime = true;
                backtestStoppedProgress = startProgress;
                backtestStoppedWindow = window;
                backtestStoppedSummary = "回测已让路：当前目标期还没有生成正式预测，先保证实时出号";
                setBacktestState(
                        "SKIP",
                        normalizedDisplayMode,
                        startProgress,
                        window,
                        backtestStoppedSummary,
                        "");

                if (manualFull) {
                    saveManualTaskState(
                            normalizedDisplayMode,
                            "SKIP",
                            startProgress,
                            backtestStoppedSummary,
                            "");
                }

                broadcastBacktestUpdated();
                return false;
            }
        }

        try {
            if (!manualFull && isRealtimeUnsafeForAutoBacktest()) {
                return false;
            }

            String startSummary = "正在回测窗口 W" + window;

            if (manualFull) {
                startSummary =
                        "安全分层深度回测：正在计算窗口 W" + window + "；显示模式=完整深度；计算引擎=" + engineMode + "；实时出号优先";
            }

            startBacktestHeartbeat(
                    normalizedDisplayMode,
                    window,
                    startProgress,
                    progress,
                    startSummary,
                    manualFull);

            setBacktestState(
                    "RUNNING", normalizedDisplayMode, startProgress, window, startSummary, "");

            if (manualFull) {
                saveManualTaskState(
                        normalizedDisplayMode, "RUNNING", startProgress, startSummary, "");
            }

            broadcastBacktestUpdated();

            String baseIssue = "";
            String targetIssue = "";

            if (snapshot.size() > 0) {
                Models.Draw last = snapshot.get(snapshot.size() - 1);
                if (last != null) {
                    baseIssue = last.issue == null ? "" : last.issue;
                    targetIssue = dataGuard.buildNextIssue(baseIssue);
                }
            }

            Models.PointInTimeSnapshot pit =
                    dataGuard.buildPointInTimeSnapshot(
                            snapshot,
                            baseIssue,
                            targetIssue,
                            0,
                            System.currentTimeMillis(),
                            System.currentTimeMillis());

            if (pit != null) {
                store.savePointInTimeSnapshot(pit);
            }

            Models.BacktestReport report =
                    backtestEngine.buildModelReportWithMode(null, snapshot, window, engineMode);

            if (report != null) {
                report.progress = progress;
                report.manualTriggered = manualFull ? 1 : 0;
                report.mode = normalizedDisplayMode;

                if (pit != null) {
                    report.auditSummary =
                            appendReason(report.auditSummary, "PIT=" + pit.snapshotId);
                }

                if (manualFull) {
                    report.summary =
                            appendReason(
                                    report.summary,
                                    "安全分层深度：显示FULL，底层使用"
                                            + engineMode
                                            + "引擎计算W"
                                            + window
                                            + "，避免手机暴力全量卡死");
                }

                store.saveBacktestReport(report);

                String summary =
                        report.summary == null || report.summary.length() == 0
                                ? buildBacktestSummary(report)
                                : report.summary;

                store.setState("backtest_last_summary", summary);
                store.setState("backtest_last_window", String.valueOf(window));
                store.setState("backtest_progress", String.valueOf(progress));

                if (manualFull) {
                    store.setState("teacher_snapshot_status", "RUNNING");
                    store.setState(
                            "teacher_snapshot_summary",
                            "已完成安全分层窗口W" + window + "；结果已写入回测报告，供实时门槛和学习层参考");
                }

                stopBacktestHeartbeat(progress, summary);
                setBacktestState("RUNNING", normalizedDisplayMode, progress, window, summary, "");

                if (manualFull) {
                    saveManualTaskState(normalizedDisplayMode, "RUNNING", progress, summary, "");
                }
            }

            broadcastBacktestUpdated();
            return true;
        } catch (Throwable e) {
            String err = safeError(e);

            stopBacktestHeartbeat(progress, "回测窗口异常：" + err);
            setBacktestState("ERROR", normalizedDisplayMode, progress, window, "回测窗口异常", err);

            if (manualFull) {
                saveManualTaskState(normalizedDisplayMode, "ERROR", progress, "完整深度回测窗口异常", err);
            }

            broadcastBacktestUpdated();
            return false;
        }
    }

    private boolean waitRealtimeAnalysisClear(
            String mode, int progress, int window, boolean manual) {
        if (!shouldDeferHeavyBackgroundNow()) {
            return true;
        }

        long start = System.currentTimeMillis();

        requestBacktestYield("回测检测到目标期未出正式预测，先让实时分析");
        postAnalysisForLatest("回测让路，优先生成当前目标期正式预测", true);

        while (running && shouldDeferHeavyBackgroundNow()) {
            long cost = System.currentTimeMillis() - start;

            if (cost >= BACKTEST_REALTIME_WAIT_MAX_MS) {
                return false;
            }

            String summary = "回测让路实时分析｜目标期正式预测未生成｜已等待 " + formatElapsed(cost);
            setBacktestState("RUNNING", mode, progress, window, summary, "");

            if (manual) {
                saveManualTaskState(mode, "RUNNING", progress, summary, "");
            }

            broadcastBacktestUpdated();
            cooperativeBacktestPause(BACKTEST_REALTIME_WAIT_SLICE_MS);
            postAnalysisForLatest("回测让路中，再次确认实时分析任务", true);
        }

        backtestYieldRequested = false;
        backtestYieldReason = "";
        return true;
    }

    private boolean hasRealtimeAnalysisNeed() {
        if (store == null || dataGuard == null) {
            return false;
        }

        try {
            Models.Draw latest = store.getLatestDraw();

            if (latest == null || !latest.isValid()) {
                return false;
            }

            String baseIssue = latest.issue == null ? "" : latest.issue;
            String targetIssue = dataGuard.buildNextIssue(baseIssue);

            if (targetIssue == null || targetIssue.length() == 0) {
                return false;
            }

            Models.Decision d = store.getDecisionByTargetIssue(targetIssue);

            if (d != null && !isWeakPlaceholderDecision(d) && sameText(d.baseIssue, baseIssue)) {
                return false;
            }

            return true;
        } catch (Throwable e) {
            return false;
        }
    }

    private boolean shouldDeferHeavyBackgroundNow() {
        if (!running) {
            return true;
        }

        if (analysisBusy) {
            return true;
        }

        if (hasRealtimeAnalysisNeed()) {
            return true;
        }

        long now = System.currentTimeMillis();

        if (realtimePriorityUntilAt > 0L && now < realtimePriorityUntilAt) {
            return true;
        }

        try {
            if (countdownClock != null) {
                long left = countdownClock.getCountdownMs();

                if (left <= 0L) {
                    return true;
                }

                if (left <= AppConfig.BACKGROUND_REALTIME_GUARD_MS) {
                    return true;
                }
                /* Decision已完成且距离下期仍有安全窗口时，允许短后台切片，避免永久饥饿。 */
                if (lastBackgroundSliceAtV5 > 0L
                        && now - lastBackgroundSliceAtV5
                                < AppConfig.BACKGROUND_MIN_SLICE_INTERVAL_MS) return true;
            }
        } catch (Throwable e) {
            return true;
        }

        return false;
    }

    private void requestRealtimePriority(String reason) {
        realtimePriorityUntilAt = System.currentTimeMillis() + REALTIME_PRIORITY_HOLD_MS;
        requestBacktestYield(reason);
        if (store == null) return;
        java.util.LinkedHashMap<String,String> projection = new java.util.LinkedHashMap<String,String>();
        projection.put("realtime_priority", "1");
        projection.put("realtime_priority_reason", reason == null ? "" : reason);
        projection.put("realtime_priority_until", String.valueOf(realtimePriorityUntilAt));
        projection.put("realtime_priority_at", String.valueOf(System.currentTimeMillis()));
        LiveTelemetryProjectionR81.tryWrite(store, projection);
    }

    private void clearRealtimePriorityIfDecisionReady(Models.Decision decision) {
        if (decision == null || isWeakPlaceholderDecision(decision)) {
            return;
        }

        realtimePriorityUntilAt = 0L;

        try {
            if (store != null) {
                store.setState("realtime_priority", "0");
                store.setState("realtime_priority_reason", "当前目标期正式预测已保存");
                store.setState("realtime_priority_until", "0");
            }
        } catch (Throwable e) {
        }
    }

    private void postTeacherMatrixAfterAnalysisSafely(String baseIssue, String reason) {
        if (store == null || baseIssue == null || baseIssue.length() == 0) {
            return;
        }

        try {
            Models.Draw latest = store.getLatestDraw();

            if (latest != null && latest.isValid() && sameText(latest.issue, baseIssue)) {
                postTeacherMatrixForDraw(
                        latest, reason, false, TEACHER_MATRIX_AFTER_ANALYSIS_DELAY_MS);
            }
        } catch (Throwable e) {
        }
    }

    private void postV20CauseChainAfterAnalysisSafely(String baseIssue, String reason) {
        if (store == null || baseIssue == null || baseIssue.length() == 0) {
            return;
        }

        try {
            markRealtimeLayeredBackgroundRequested(baseIssue, reason);
            Models.Draw latest = store.getLatestDraw();

            if (latest != null && latest.isValid() && sameText(latest.issue, baseIssue)) {
                postV20CauseChainForDraw(
                        latest, reason, false, V20_CAUSE_CHAIN_AFTER_ANALYSIS_DELAY_MS);
            }
        } catch (Throwable e) {
        }
    }

    private void requestBacktestYield(String reason) {
        backtestYieldRequested = true;
        backtestYieldReason = reason == null ? "" : reason;

        if (store != null && backtestBusy) {
            try {
                String old = store.getState("backtest_last_summary", "");
                if (old == null) {
                    old = "";
                }
                if (old.indexOf("让路") < 0) {
                    store.setState("backtest_last_summary", appendReason(old, "实时分析优先，回测准备让路"));
                }
            } catch (Throwable e) {
            }
        }
    }

    private int stageStartProgress(int endProgress) {
        if (endProgress <= 0) {
            return 0;
        }

        if (endProgress <= 20) {
            return 2;
        }

        if (endProgress <= 40) {
            return 21;
        }

        if (endProgress <= 70) {
            return 41;
        }

        if (endProgress <= 95) {
            return 71;
        }

        return 96;
    }

    private void startBacktestHeartbeat(
            String mode,
            int window,
            int startProgress,
            int endProgress,
            String summary,
            boolean manual) {

        backtestHeartbeatRunning = true;
        backtestHeartbeatManual = manual;
        backtestHeartbeatStartProgress = startProgress;
        backtestHeartbeatEndProgress = endProgress;
        backtestHeartbeatWindow = window;
        backtestHeartbeatMode = mode == null ? "" : mode;
        backtestHeartbeatSummary = summary == null ? "" : summary;
        backtestHeartbeatStartedAt = System.currentTimeMillis();

        if (runtimeGuardHandler != null) {
            runtimeGuardHandler.removeCallbacks(backtestHeartbeatLoop);
            runtimeGuardHandler.postDelayed(backtestHeartbeatLoop, BACKTEST_HEARTBEAT_INTERVAL_MS);
        }
    }

    private void stopBacktestHeartbeat(int progress, String summary) {
        backtestHeartbeatRunning = false;

        if (runtimeGuardHandler != null) {
            runtimeGuardHandler.removeCallbacks(backtestHeartbeatLoop);
        }
    }

    private ArrayList<Models.Draw> buildHistorySnapshot(List<Models.Draw> history, int maxKeep) {
        ArrayList<Models.Draw> snapshot = new ArrayList<Models.Draw>();

        if (history == null || history.size() == 0) {
            return snapshot;
        }

        ArrayList<Models.Draw> clean = dataGuard.normalizeHistory(history);

        if (maxKeep <= 0) {
            maxKeep = AppConfig.WINDOW_3000;
        }

        int start = clean.size() - maxKeep;
        if (start < 0) {
            start = 0;
        }

        for (int i = start; i < clean.size(); i++) {
            Models.Draw d = clean.get(i);

            if (d != null && d.isValid()) {
                snapshot.add(d.copy());
            }
        }

        return snapshot;
    }

    private boolean isRealtimeUnsafeForAutoBacktest() {
        if (countdownClock == null) {
            return false;
        }

        if (isRealtimeLikeWindow()) {
            return true;
        }

        long left = countdownClock.getCountdownMs();

        if (left <= 0L) {
            return true;
        }

        return left < BACKTEST_MIN_IDLE_BEFORE_OPEN_MS;
    }

    private void cooperativeBacktestPause(long sleepMs) {
        try {
            Thread.yield();

            if (sleepMs < 1L) {
                sleepMs = 1L;
            }

            if (backtestYieldRequested) {
                sleepMs += 350L;
            }

            Thread.sleep(sleepMs);
        } catch (Throwable e) {
        }
    }

    private void saveManualTaskState(
            String mode, String status, int progress, String summary, String error) {
        if (store == null) {
            return;
        }

        try {
            if (currentManualTaskId == null || currentManualTaskId.length() == 0) {
                currentManualTaskId = "MANUAL_" + System.currentTimeMillis();
                currentManualStartedAt = System.currentTimeMillis();
                currentManualMode = AppConfig.normalizeBacktestMode(mode);
            }

            Models.ManualBacktestTask task = new Models.ManualBacktestTask();
            task.taskId = currentManualTaskId;
            task.mode = AppConfig.normalizeBacktestMode(mode);
            task.status = status == null ? "" : status;
            task.progress = progress;
            task.windowSize = SAFE_FULL_W500;
            task.totalSteps = 100;
            task.doneSteps = progress;
            task.startedAt =
                    currentManualStartedAt > 0L
                            ? currentManualStartedAt
                            : System.currentTimeMillis();
            task.summary = summary == null ? "" : summary;
            task.error = error == null ? "" : error;

            if ("OK".equals(status)
                    || "ERROR".equals(status)
                    || "SKIP".equals(status)
                    || "BUSY".equals(status)) {
                task.finishedAt = System.currentTimeMillis();
            }

            store.saveManualBacktestTask(task);
        } catch (Throwable e) {
        }
    }

    private boolean shouldRunRealtimeAdaptiveReflection() {
        return REALTIME_RUN_ADAPTIVE_REFLECTION;
    }

    private String getLatestIssueSafe() {
        try {
            if (store != null) {
                Models.Draw d = store.getLatestDraw();
                if (d != null && d.isValid() && d.issue != null) {
                    return d.issue;
                }
            }
        } catch (Throwable e) {
        }
        return "";
    }

    private boolean shouldRunTieredBackgroundProfile(String issue) {
        return !PROFILE_TIER_NONE.equals(selectBackgroundProfileTier(issue, false));
    }

    private String selectBackgroundProfileTier(String issue, boolean force) {
        long now = System.currentTimeMillis();
        if (force) {
            return PROFILE_TIER_WARM;
        }
        if (lastDeepProfileAt <= 0L || now - lastDeepProfileAt >= DEEP_PROFILE_REFRESH_MS) {
            return PROFILE_TIER_DEEP;
        }
        if (lastColdProfileAt <= 0L || now - lastColdProfileAt >= COLD_PROFILE_REFRESH_MS) {
            return PROFILE_TIER_COLD;
        }
        if (lastWarmProfileAt <= 0L || now - lastWarmProfileAt >= WARM_PROFILE_REFRESH_MS) {
            return PROFILE_TIER_WARM;
        }
        return PROFILE_TIER_NONE;
    }

    private String buildTieredBackgroundSkipSummary(String module, String issue) {
        long now = System.currentTimeMillis();
        long warmLeft =
                lastWarmProfileAt <= 0L ? 0L : WARM_PROFILE_REFRESH_MS - (now - lastWarmProfileAt);
        long coldLeft =
                lastColdProfileAt <= 0L ? 0L : COLD_PROFILE_REFRESH_MS - (now - lastColdProfileAt);
        if (warmLeft < 0L) warmLeft = 0L;
        if (coldLeft < 0L) coldLeft = 0L;
        return module
                + " FIX23分层调度跳过：issue="
                + safe(issue)
                + "；10分钟温层剩余="
                + formatElapsed(warmLeft)
                + "；5000期冷层剩余="
                + formatElapsed(coldLeft)
                + "；不逐期扫5000，不排旧债，实时HOT链已按当前360窗口出号";
    }

    private int getTieredBackgroundHistoryLimit(String issue) {
        String tier =
                currentBackgroundProfileTier == null || currentBackgroundProfileTier.length() == 0
                        ? selectBackgroundProfileTier(issue, false)
                        : currentBackgroundProfileTier;
        if (PROFILE_TIER_DEEP.equals(tier)) {
            return DEEP_PROFILE_HISTORY_LIMIT;
        }
        if (PROFILE_TIER_COLD.equals(tier)) {
            return COLD_PROFILE_HISTORY_LIMIT;
        }
        if (PROFILE_TIER_WARM.equals(tier)) {
            return WARM_PROFILE_HISTORY_LIMIT;
        }
        return V20_BACKGROUND_HISTORY_LIMIT;
    }

    private int getTieredBackgroundCandidateLimit(String issue) {
        String tier =
                currentBackgroundProfileTier == null || currentBackgroundProfileTier.length() == 0
                        ? selectBackgroundProfileTier(issue, false)
                        : currentBackgroundProfileTier;
        if (PROFILE_TIER_DEEP.equals(tier)) {
            return DEEP_PROFILE_CANDIDATE_LIMIT;
        }
        if (PROFILE_TIER_COLD.equals(tier)) {
            return COLD_PROFILE_CANDIDATE_LIMIT;
        }
        if (PROFILE_TIER_WARM.equals(tier)) {
            return WARM_PROFILE_CANDIDATE_LIMIT;
        }
        return V20_BACKGROUND_CANDIDATE_LIMIT;
    }

    private void markTieredBackgroundProfileDone(
            String tier, String issue, long costMs, String summary) {
        long now = System.currentTimeMillis();
        if (tier == null || tier.length() == 0 || PROFILE_TIER_NONE.equals(tier)) {
            return;
        }
        if (PROFILE_TIER_WARM.equals(tier)
                || PROFILE_TIER_COLD.equals(tier)
                || PROFILE_TIER_DEEP.equals(tier)) {
            lastWarmProfileAt = now;
            lastWarmProfileIssue = issue == null ? "" : issue;
        }
        if (PROFILE_TIER_COLD.equals(tier) || PROFILE_TIER_DEEP.equals(tier)) {
            lastColdProfileAt = now;
            lastColdProfileIssue = issue == null ? "" : issue;
        }
        if (PROFILE_TIER_DEEP.equals(tier)) {
            lastDeepProfileAt = now;
            lastDeepProfileIssue = issue == null ? "" : issue;
        }
        try {
            if (store != null) {
                store.setState("tiered_background_version", V20_TIERED_REALTIME_SCHEDULER_VERSION);
                store.setState("tiered_background_last_tier", safe(tier));
                store.setState("tiered_background_last_issue", safe(issue));
                store.setState("tiered_background_last_cost_ms", String.valueOf(costMs));
                store.setState("tiered_background_last_at", String.valueOf(now));
                store.setState(
                        "tiered_background_history_limit",
                        String.valueOf(getTieredBackgroundHistoryLimit(issue)));
                store.setState(
                        "tiered_background_candidate_limit",
                        String.valueOf(getTieredBackgroundCandidateLimit(issue)));
                store.setState("tiered_background_summary", safe(summary));
                store.setState(
                        "tiered_background_policy",
                        "HOT每期只跑360实时；WARM约10分钟更新1440；COLD约1小时更新5000；DEEP约3小时/空闲更新10000；旧任务合并追最新，不逐期排队。");
            }
        } catch (Throwable e) {
        }
    }

    private void applyTieredSnapshotFreshnessToCandidates(
            ArrayList<Models.ComboCandidate> candidates,
            String baseIssue,
            String targetIssue,
            String stage) {
        if (candidates == null || candidates.size() == 0) {
            return;
        }
        long now = System.currentTimeMillis();
        Models.FrequencyState frequencyState = null;
        try {
            if (store != null) frequencyState = store.loadFrequencyState();
        } catch (Throwable e) {
            frequencyState = null;
        }
        long warmAge = lastWarmProfileAt <= 0L ? Long.MAX_VALUE : now - lastWarmProfileAt;
        long coldAge = lastColdProfileAt <= 0L ? Long.MAX_VALUE : now - lastColdProfileAt;
        String warmStatus =
                warmAge <= WARM_PROFILE_SOFT_STALE_MS
                        ? "FRESH"
                        : (warmAge <= WARM_PROFILE_HARD_STALE_MS ? "WATCH_STALE" : "NEUTRAL_STALE");
        String coldStatus =
                coldAge <= COLD_PROFILE_SOFT_STALE_MS
                        ? "FRESH"
                        : (coldAge <= COLD_PROFILE_HARD_STALE_MS ? "WATCH_STALE" : "NEUTRAL_STALE");
        for (int i = 0; i < candidates.size(); i++) {
            Models.ComboCandidate c = candidates.get(i);
            if (c == null) continue;
            writeString(c, "backgroundProfileTier", PROFILE_TIER_HOT);
            writeString(c, "warmProfileFreshness", warmStatus);
            writeString(c, "coldProfileFreshness", coldStatus);
            writeString(c, "longProfileStatus", coldStatus);
            writeString(
                    c,
                    "snapshotFreshnessPolicy",
                    "链路修改升级V2：实时只读快照；过期快照降为WATCH/NEUTRAL，不硬杀、不救援、不强加权");
            writeString(c, "snapshotFreshness", "W=" + warmStatus + "|C=" + coldStatus);
            writeString(c, "warmProfileFreshness", warmStatus);
            writeString(c, "coldProfileFreshness", coldStatus);
            writeString(c, "decisionVersionV2", LINK_MODIFICATION_V2_AUTOFETCH_VERSION);
            writeString(c, "evidenceAsOfIssue", baseIssue);
            writeString(c, "snapshotAsOfIssue", baseIssue);
            writeLong(c, "snapshotCreatedAt", now);
            if (frequencyState != null) {
                writeDouble(c, "realBetFrequency50", frequencyState.frequency50);
                writeDouble(c, "realBetFrequency100", frequencyState.frequency100);
                writeDouble(c, "realBetFrequency300", frequencyState.frequency300);
                writeDouble(c, "frequencyDebtScore", frequencyState.frequencyDebt);
                writeString(c, "frequencyState", frequencyState.status);
                writeString(c, "frequencyStateVersion", frequencyState.version);
                writeLong(c, "frequencyStateUpdatedAt", frequencyState.updatedAt);
            }
            writeString(c, "fieldChainVersion", V20_TIERED_REALTIME_SCHEDULER_VERSION);
        }
        try {
            if (store != null) {
                store.setState("tiered_snapshot_stage", safe(stage));
                store.setState("tiered_snapshot_target", safe(targetIssue));
                store.setState("tiered_snapshot_warm_status", warmStatus);
                store.setState("tiered_snapshot_cold_status", coldStatus);
                store.setState("tiered_snapshot_policy", "过期快照不当强信号使用；WARM/COLD后台分层刷新，实时HOT链按时出号");
            }
        } catch (Throwable e) {
        }
    }

    private void postLearningTick(final String reason, final boolean force) {
        if (AppConfig.ENABLE_V51_TIANSHU && AppConfig.V51_RUNTIME_LEGACY_HEAVY_ISOLATION) return;
        if (AppConfig.ENABLE_V50_PRIME) return;
        try {
            heartbeatStateV8().markLearningAttempt("LEGACY_POSTED", reason);
        } catch (Throwable ignore) {
        }

        if (!running || learningHandler == null || store == null) {
            return;
        }

        if (learningPostPending) {
            return;
        }

        long now = System.currentTimeMillis();

        if (!force && lastLearningAt > 0L && now - lastLearningAt < AUTO_LEARNING_MIN_INTERVAL_MS) {
            return;
        }

        boolean deferredNow = shouldDeferHeavyBackgroundNow();

        learningPostPending = true;

        long delay = force ? AUTO_LEARNING_AFTER_NEW_DRAW_DELAY_MS : AUTO_LEARNING_IDLE_DELAY_MS;
        if (deferredNow) delay = Math.max(delay, AppConfig.BACKGROUND_MIN_SLICE_INTERVAL_MS);
        learningHandler.postDelayed(
                new Runnable() {
                    @Override
                    public void run() {
                        learningPostPending = false;
                        runLearningChannelOnce(reason, force);
                    }
                },
                delay);
    }

    private void runLearningChannelOnce(String reason, boolean force) {
        if (!running || store == null) {
            return;
        }

        if (learningBusy) {
            return;
        }

        if (shouldDeferHeavyBackgroundNow()) {
            postLearningTick("实时出号优先，后台学习延后", false);
            return;
        }

        learningBusy = true;
        lastLearningAt = System.currentTimeMillis();
        lastBackgroundSliceAtV5 = lastLearningAt;

        try {
            setLearningState("RUNNING", "V26严格OOS微型走前启动：" + (reason == null ? "" : reason));

            /* The causal micro walk-forward always runs first and has its own 8.5s
             * budget.  Legacy teacher/V20/deep maintenance is not allowed to consume
             * every ten-second idle slice; it is gated to one low-priority pass every
             * two minutes and immediately yields to realtime analysis. */
            String causalWalkForwardSummary = runRollingCausalMicroSlice();
            long nowV23 = System.currentTimeMillis();
            boolean runLegacyMaintenance =
                    !shouldDeferHeavyBackgroundNow()
                            && (lastLegacyLearningAtV23 <= 0L
                                    || nowV23 - lastLegacyLearningAtV23
                                            >= V23_LEGACY_BACKGROUND_INTERVAL_MS);

            String teacherSummary =
                    runLegacyMaintenance
                            ? runTeacherMatrixAutoBatchByReflection(reason, force)
                            : "旧教师维护本片跳过：V26严格OOS微型走前优先";
            String v20Summary =
                    runLegacyMaintenance
                            ? runV20CauseChainAutoBatchByReflection(reason, force)
                            : "旧V20长因链维护本片跳过：防止阻塞实时出号";
            String learningSummary =
                    runLegacyMaintenance
                            ? runLearningEngineByReflection()
                            : "旧学习重链本片跳过：由V40 Top-K微型走前更新行动残差、有限情境族与事件概率校准桶";
            if (runLegacyMaintenance) lastLegacyLearningAtV23 = nowV23;

            String summary = appendReason(causalWalkForwardSummary, teacherSummary);
            summary = appendReason(summary, v20Summary);
            summary = appendReason(summary, learningSummary);

            if (summary == null || summary.length() == 0) {
                summary = "后台学习完成：本轮无可执行批次";
            }

            lastLearningSummary = summary;

            setLearningState("IDLE", summary);

            if (runLegacyMaintenance && !shouldDeferHeavyBackgroundNow()) {
                runDeepLearningBridgeByReflection();
            }

            if (store != null) {
                store.setState(
                        "service_core_summary",
                        "实时分析优先；主链=轻扫描→rawRank排序→已发布分层后验→严格OOS单调概率→动态门→Decision唯一授权；后台只更新OOS后验与校准；"
                                + summary);
            }

            broadcastSimpleStateLimited(true);
        } catch (Throwable e) {
            lastLearningSummary = "后台学习异常：" + safeError(e);
            setLearningState("ERROR", lastLearningSummary);
        } finally {
            learningBusy = false;
        }
    }

    private String runRollingCausalMicroSlice() {
        postMicroWalkForward("后台学习通道请求", false, 300L);
        if (store == null) return "V26行动级微型走前已请求：store为空";
        return "V26行动级微型走前由独立线程执行｜"
                + store.getState(MicroWalkForwardEngine.STATE_LAST_SUMMARY, "WAITING");
    }

    private void postMicroWalkForward(final String reason, final boolean urgent, long delayMs) {
        if (AppConfig.ENABLE_V51_TIANSHU && AppConfig.V51_RUNTIME_LEGACY_HEAVY_ISOLATION) return;
        if (AppConfig.ENABLE_V50_PRIME) return;
        if (!running || microHandler == null || store == null || microWalkForwardEngine == null)
            return;
        if (microPostPending) return;
        microPostPending = true;
        if (delayMs < 0L) delayMs = 0L;
        final long delay = delayMs;
        try {
            store.setState("v26_micro_pump_state", "POSTED");
            store.setState("v26_micro_pump_reason", safe(reason));
            store.setState("v26_micro_pump_posted_at", String.valueOf(System.currentTimeMillis()));
        } catch (Throwable ignore) {
        }
        microHandler.postDelayed(
                new Runnable() {
                    public void run() {
                        microPostPending = false;
                        runMicroWalkForwardDedicated(reason, urgent);
                    }
                },
                delay);
    }

    private void runMicroWalkForwardDedicated(String reason, boolean urgent) {
        if (!running || store == null || microWalkForwardEngine == null) return;
        if (microBusy) return;

        long countdownMs = getMicroCountdownMs();
        boolean realtimeNeed = hasRealtimeAnalysisNeed();
        boolean mayStart =
                OosTrainingCoordinatorEngine.mayStart(
                        store,
                        running,
                        analysisBusy,
                        fetchBusy,
                        backfillBusy,
                        realtimeNeed,
                        countdownMs,
                        urgent);
        if (!mayStart) {
            try {
                store.setState("v26_micro_pump_state", "WAIT_REALTIME_SAFE_WINDOW");
                store.setState("v26_micro_pump_reason", safe(reason));
                store.setState("v26_micro_pump_countdown_ms", String.valueOf(countdownMs));
                store.setState("v26_micro_pump_realtime_need", realtimeNeed ? "1" : "0");
            } catch (Throwable ignore) {
            }
            postMicroWalkForward(
                    "等待真实安全窗口:" + safe(reason),
                    urgent,
                    AppConfig.MICRO_WALK_FORWARD_YIELD_RETRY_MS);
            return;
        }

        microBusy = true;
        long start = System.currentTimeMillis();
        int beforeSample = 0;
        try {
            beforeSample =
                    parseIntSafe(store.getState(MicroWalkForwardEngine.STATE_SAMPLE, "0"), 0);
            OosTrainingCoordinatorEngine.noteStart(store);
            store.setState("v26_micro_pump_state", "RUNNING_PROTECTED_SLICE");
            store.setState("v26_micro_pump_reason", safe(reason));
            store.setState("v26_micro_pump_started_at", String.valueOf(start));
            store.setState("v26_micro_pump_countdown_ms", String.valueOf(countdownMs));

            MicroWalkForwardEngine.Report report =
                    microWalkForwardEngine.runBatch(
                            store,
                            AppConfig.MICRO_WALK_FORWARD_BATCH_ORIGINS,
                            AppConfig.MICRO_WALK_FORWARD_PROTECTED_SLICE_MS,
                            new MicroWalkForwardEngine.YieldSignal() {
                                public boolean shouldYield() {
                                    return OosTrainingCoordinatorEngine.shouldYieldInside(
                                            running, analysisBusy, fetchBusy, backfillBusy);
                                }
                            });

            OosTrainingCoordinatorEngine.noteResult(store, beforeSample, report);
            String summary = report == null ? "V26行动级微型走前无报告" : report.summary();
            store.setState("v26_micro_pump_state", report == null ? "NO_REPORT" : report.status);
            store.setState("v26_micro_pump_summary", summary);
            store.setState(
                    "v26_micro_pump_cost_ms", String.valueOf(System.currentTimeMillis() - start));
            store.setState(
                    "v26_micro_pump_completed_at", String.valueOf(System.currentTimeMillis()));

            boolean ready = MicroWalkForwardEngine.isReady(store);
            int pending = OosActionLogEngine.countSettledUnprocessedForCurrentBuild(store);
            int afterSample =
                    parseIntSafe(store.getState(MicroWalkForwardEngine.STATE_SAMPLE, "0"), 0);
            boolean progressed = afterSample > beforeSample;
            if (!ready || pending > 0) {
                long delay =
                        progressed
                                ? AppConfig.MICRO_WALK_FORWARD_REPOST_DELAY_MS
                                : AppConfig.MICRO_WALK_FORWARD_YIELD_RETRY_MS;
                postMicroWalkForward(
                        "继续行动OOS训练:sample=" + afterSample + ",pending=" + pending, true, delay);
            } else {
                postMicroWalkForward(
                        "行动OOS已就绪，保持增量发布", false, AppConfig.MICRO_WALK_FORWARD_IDLE_DELAY_MS);
            }
        } catch (Throwable e) {
            try {
                store.setState("v26_micro_pump_state", "ERROR");
                store.setState("v26_micro_pump_error", safeError(e));
            } catch (Throwable ignore) {
            }
            OosTrainingCoordinatorEngine.noteResult(store, beforeSample, null);
            postMicroWalkForward("行动OOS异常重试", true, AppConfig.MICRO_WALK_FORWARD_YIELD_RETRY_MS);
        } finally {
            microBusy = false;
        }
    }

    /** Returns a scheduling hint only. It is never consulted inside a protected slice. */
    private long getMicroCountdownMs() {
        try {
            return countdownClock == null ? Long.MAX_VALUE : countdownClock.getCountdownMs();
        } catch (Throwable e) {
            return Long.MAX_VALUE;
        }
    }

    private int parseIntSafe(String text, int def) {
        try {
            return Integer.parseInt(text);
        } catch (Throwable e) {
            return def;
        }
    }

    private void postTeacherMatrixForDraw(
            Models.Draw draw, String reason, boolean force, long delayMs) {
        if (AppConfig.ENABLE_V50_PRIME) return;
        if (!running
                || learningHandler == null
                || draw == null
                || !draw.isValid()
                || store == null
                || getEffectiveTeacherMatrixEngine() == null) {
            return;
        }

        String issue = draw.issue == null ? "" : draw.issue;

        if (issue.length() == 0) {
            return;
        }

        long now = System.currentTimeMillis();

        if (!force
                && sameText(issue, lastTeacherMatrixIssue)
                && now - lastTeacherMatrixAt < TEACHER_MATRIX_MIN_INTERVAL_MS) {
            return;
        }

        pendingTeacherMatrixDraw = draw.copy();
        pendingTeacherMatrixReason = reason == null ? "" : reason;
        pendingTeacherMatrixForce = force;

        if (teacherMatrixPostPending) {
            return;
        }

        teacherMatrixPostPending = true;

        if (delayMs < 0L) {
            delayMs = 0L;
        }

        if (shouldDeferHeavyBackgroundNow()) {
            delayMs = Math.max(delayMs, TEACHER_MATRIX_IDLE_DELAY_MS);
        }

        learningHandler.removeCallbacks(teacherMatrixLoop);
        learningHandler.postDelayed(teacherMatrixLoop, delayMs);

        setTeacherMatrixState("WAITING", "教师矩阵已排队后台执行：issue=" + issue + "；实时正式预测优先");
    }

    private void runTeacherMatrixWorker(Models.Draw draw, String reason, boolean force) {
        Object tmEngine = getEffectiveTeacherMatrixEngine();

        if (draw == null || !draw.isValid() || store == null || tmEngine == null) {
            return;
        }

        String issue = draw.issue == null ? "" : draw.issue;

        if (issue.length() == 0) {
            return;
        }

        if (!force && !shouldRunTieredBackgroundProfile(issue)) {
            lastTeacherMatrixSummary = buildTieredBackgroundSkipSummary("教师矩阵", issue);
            setTeacherMatrixState("SKIP_NOT_DUE_FIX23", lastTeacherMatrixSummary);
            broadcastTeacherMatrixUpdated();
            return;
        }
        currentBackgroundProfileTier = selectBackgroundProfileTier(issue, force);
        currentBackgroundProfileStartedAt = System.currentTimeMillis();

        if (shouldDeferHeavyBackgroundNow()) {
            postTeacherMatrixForDraw(
                    draw, "实时目标期还未完成，教师矩阵继续排队让路", false, TEACHER_MATRIX_IDLE_DELAY_MS + 2500L);
            return;
        }

        long now = System.currentTimeMillis();

        if (!force
                && sameText(issue, lastTeacherMatrixIssue)
                && now - lastTeacherMatrixAt < TEACHER_MATRIX_MIN_INTERVAL_MS) {
            return;
        }

        if (teacherMatrixBusy) {
            postTeacherMatrixForDraw(draw, reason, force, TEACHER_MATRIX_IDLE_DELAY_MS);
            return;
        }

        teacherMatrixBusy = true;
        lastBackgroundSliceAtV5 = now;
        long start = now;

        try {
            setTeacherMatrixState("RUNNING", reason == null ? "教师矩阵后台运行中" : reason);

            Object result = REFLECTION_NO_METHOD;

            result =
                    invokeOptional(
                            tmEngine,
                            "learnLatestDraw",
                            new Class[] {DbStore.class, Models.Draw.class},
                            new Object[] {store, draw});
            if (result == REFLECTION_NO_METHOD) {
                result =
                        invokeOptional(
                                tmEngine,
                                "learnAfterDraw",
                                new Class[] {DbStore.class, Models.Draw.class},
                                new Object[] {store, draw});
            }
            if (result == REFLECTION_NO_METHOD) {
                result =
                        invokeOptional(
                                tmEngine,
                                "learnAfterNewDraw",
                                new Class[] {DbStore.class, Models.Draw.class},
                                new Object[] {store, draw});
            }
            if (result == REFLECTION_NO_METHOD) {
                result =
                        invokeOptional(
                                tmEngine,
                                "onDrawSaved",
                                new Class[] {DbStore.class, Models.Draw.class},
                                new Object[] {store, draw});
            }
            if (result == REFLECTION_NO_METHOD) {
                result =
                        invokeOptional(
                                tmEngine,
                                "runAfterDraw",
                                new Class[] {DbStore.class, Models.Draw.class},
                                new Object[] {store, draw});
            }
            if (result == REFLECTION_NO_METHOD) {
                result =
                        invokeOptional(
                                tmEngine,
                                "learnOneDraw",
                                new Class[] {DbStore.class, Models.Draw.class},
                                new Object[] {store, draw});
            }
            if (result == REFLECTION_NO_METHOD) {
                result =
                        invokeOptional(
                                tmEngine,
                                "learnLatest",
                                new Class[] {DbStore.class},
                                new Object[] {store});
            }
            if (result == REFLECTION_NO_METHOD) {
                result =
                        invokeOptional(
                                tmEngine,
                                "runTick",
                                new Class[] {DbStore.class},
                                new Object[] {store});
            }
            if (result == REFLECTION_NO_METHOD) {
                result =
                        invokeOptional(
                                tmEngine,
                                "tick",
                                new Class[] {DbStore.class},
                                new Object[] {store});
            }

            long cost = System.currentTimeMillis() - start;
            String summary;

            if (result == REFLECTION_NO_METHOD) {
                summary = runTeacherMatrixManualProfileFallback(draw, issue);
            } else {
                summary =
                        buildTeacherMatrixInvokeSummary(
                                result, "教师矩阵完成：issue=" + issue + "，后台反推闭环已执行");
            }

            summary = appendReason(summary, "耗时" + cost + "ms，后台执行不阻塞抓取/出号");

            lastTeacherMatrixAt = System.currentTimeMillis();
            lastTeacherMatrixIssue = issue;
            lastTeacherMatrixSummary = summary;
            markTieredBackgroundProfileDone(
                    currentBackgroundProfileTier,
                    issue,
                    System.currentTimeMillis() - start,
                    summary);

            setTeacherMatrixState("OK", summary);
            broadcastTeacherMatrixUpdated();
        } catch (Throwable e) {
            lastTeacherMatrixSummary = "教师矩阵异常：" + safeError(e);
            setTeacherMatrixState("ERROR", lastTeacherMatrixSummary);
        } finally {
            teacherMatrixBusy = false;
        }
    }

    private void postV20CauseChainForDraw(
            Models.Draw draw, String reason, boolean force, long delayMs) {
        if (AppConfig.ENABLE_V50_PRIME) return;
        if (!running
                || learningHandler == null
                || draw == null
                || !draw.isValid()
                || store == null) {
            return;
        }

        if (dynamicCauseChainEngine == null
                && causalFactorMatrixEngine == null
                && positionCauseMatrixEngine == null
                && teacherNumberMatrixEngine == null
                && counterfactualRankEngine == null
                && thirdGuardMatrixEngine == null
                && ruleHealthMatrixEngine == null
                && shadowHealthGateEngine == null
                && falseStrongFreezeEngine == null
                && seedCauseExpansionEngine == null) {
            return;
        }

        String issue = draw.issue == null ? "" : draw.issue;

        if (issue.length() == 0) {
            return;
        }

        long now = System.currentTimeMillis();

        if (!force
                && sameText(issue, lastV20CauseChainIssue)
                && now - lastV20CauseChainAt < V20_CAUSE_CHAIN_MIN_INTERVAL_MS) {
            return;
        }

        if (!force && !shouldRunTieredBackgroundProfile(issue)) {
            lastV20CauseChainSummary = buildTieredBackgroundSkipSummary("V20因链", issue);
            setV20CauseChainState("SKIP_NOT_DUE_FIX23", lastV20CauseChainSummary);
            return;
        }

        pendingV20CauseChainDraw = draw.copy();
        pendingV20CauseChainReason = reason == null ? "" : reason;
        pendingV20CauseChainForce = force;

        if (v20CauseChainPostPending) {
            return;
        }

        v20CauseChainPostPending = true;

        if (delayMs < 0L) {
            delayMs = 0L;
        }

        if (shouldDeferHeavyBackgroundNow()) {
            delayMs = Math.max(delayMs, V20_CAUSE_CHAIN_IDLE_DELAY_MS);
        }

        learningHandler.removeCallbacks(v20CauseChainLoop);
        learningHandler.postDelayed(v20CauseChainLoop, delayMs);

        setV20CauseChainState("WAITING", "V20因链画像已排队后台执行：issue=" + issue + "；实时正式预测优先");
    }

    private void runV20CauseChainWorker(Models.Draw draw, String reason, boolean force) {
        if (draw == null || !draw.isValid() || store == null) {
            return;
        }

        String issue = draw.issue == null ? "" : draw.issue;

        if (issue.length() == 0) {
            return;
        }

        if (!force && !shouldRunTieredBackgroundProfile(issue)) {
            lastV20CauseChainSummary = buildTieredBackgroundSkipSummary("V20因链", issue);
            setV20CauseChainState("SKIP_NOT_DUE_FIX23", lastV20CauseChainSummary);
            broadcastV20CauseChainUpdated();
            return;
        }
        currentBackgroundProfileTier = selectBackgroundProfileTier(issue, force);
        currentBackgroundProfileStartedAt = System.currentTimeMillis();

        if (shouldDeferHeavyBackgroundNow()) {
            postV20CauseChainForDraw(
                    draw, "实时目标期还未完成，V20因链继续排队让路", false, V20_CAUSE_CHAIN_IDLE_DELAY_MS + 2500L);
            return;
        }

        long now = System.currentTimeMillis();

        if (!force
                && sameText(issue, lastV20CauseChainIssue)
                && now - lastV20CauseChainAt < V20_CAUSE_CHAIN_MIN_INTERVAL_MS) {
            return;
        }

        if (v20CauseChainBusy) {
            postV20CauseChainForDraw(draw, reason, force, V20_CAUSE_CHAIN_IDLE_DELAY_MS);
            return;
        }

        v20CauseChainBusy = true;
        long start = now;

        try {
            setV20CauseChainState("RUNNING", reason == null ? "V20因链后台运行中" : reason);

            String summary = "";

            summary =
                    appendReason(
                            summary,
                            runOneV20LearnAfterDraw(
                                    dynamicCauseChainEngine, "DynamicCauseChain", draw));
            summary =
                    appendReason(
                            summary,
                            runOneV20LearnAfterDraw(
                                    causalFactorMatrixEngine, "CausalFactorMatrix", draw));
            summary =
                    appendReason(
                            summary,
                            runOneV20LearnAfterDraw(
                                    positionCauseMatrixEngine, "PositionCauseMatrix", draw));
            summary =
                    appendReason(
                            summary,
                            runOneV20LearnAfterDraw(
                                    teacherNumberMatrixEngine, "TeacherNumberMatrix", draw));
            summary =
                    appendReason(
                            summary,
                            runOneV20LearnAfterDraw(
                                    counterfactualRankEngine, "CounterfactualRank", draw));
            summary =
                    appendReason(
                            summary,
                            runOneV20LearnAfterDraw(
                                    thirdGuardMatrixEngine, "ThirdGuardMatrix", draw));
            summary =
                    appendReason(
                            summary,
                            runOneV20LearnAfterDraw(
                                    ruleHealthMatrixEngine, "RuleHealthMatrix", draw));
            summary =
                    appendReason(
                            summary,
                            runOneV20LearnAfterDraw(
                                    shadowHealthGateEngine, "ShadowHealthGate", draw));
            summary =
                    appendReason(
                            summary,
                            runOneV20LearnAfterDraw(
                                    falseStrongFreezeEngine, "FalseStrongFreeze", draw));
            summary =
                    appendReason(
                            summary,
                            runOneV20LearnAfterDraw(
                                    seedCauseExpansionEngine, "SeedCauseExpansion", draw));

            if (summary == null || summary.length() == 0) {
                summary = runV20BackgroundProfileFallback(draw, issue);
            }

            long cost = System.currentTimeMillis() - start;

            if (summary == null || summary.length() == 0) {
                summary = "V20因链后台完成：当前引擎未返回摘要，但后台画像兜底已检查";
            }

            summary =
                    appendReason(
                            summary,
                            "issue=" + issue + "，耗时" + cost + "ms，后台执行不阻塞抓取/出号，不写真实BET/盈亏/校准");

            lastV20CauseChainAt = System.currentTimeMillis();
            lastV20CauseChainIssue = issue;
            lastV20CauseChainSummary = summary;
            markTieredBackgroundProfileDone(
                    currentBackgroundProfileTier,
                    issue,
                    System.currentTimeMillis() - start,
                    summary);

            setV20CauseChainState("OK", summary);
            broadcastV20CauseChainUpdated();
        } catch (Throwable e) {
            lastV20CauseChainSummary = "V20因链异常：" + safeError(e);
            setV20CauseChainState("ERROR", lastV20CauseChainSummary);
        } finally {
            v20CauseChainBusy = false;
        }
    }

    private String runOneV20LearnAfterDraw(Object engine, String name, Models.Draw draw) {
        if (engine == null || draw == null || store == null) {
            return "";
        }

        Object result = REFLECTION_NO_METHOD;

        result =
                invokeOptional(
                        engine,
                        "learnLatestDraw",
                        new Class[] {DbStore.class, Models.Draw.class},
                        new Object[] {store, draw});
        if (result == REFLECTION_NO_METHOD) {
            result =
                    invokeOptional(
                            engine,
                            "learnAfterDraw",
                            new Class[] {DbStore.class, Models.Draw.class},
                            new Object[] {store, draw});
        }
        if (result == REFLECTION_NO_METHOD) {
            result =
                    invokeOptional(
                            engine,
                            "learnAfterNewDraw",
                            new Class[] {DbStore.class, Models.Draw.class},
                            new Object[] {store, draw});
        }
        if (result == REFLECTION_NO_METHOD) {
            result =
                    invokeOptional(
                            engine,
                            "observeAfterDraw",
                            new Class[] {DbStore.class, Models.Draw.class},
                            new Object[] {store, draw});
        }
        if (result == REFLECTION_NO_METHOD) {
            result =
                    invokeOptional(
                            engine,
                            "profileAfterDraw",
                            new Class[] {DbStore.class, Models.Draw.class},
                            new Object[] {store, draw});
        }
        if (result == REFLECTION_NO_METHOD) {
            result =
                    invokeOptional(
                            engine,
                            "onDrawSaved",
                            new Class[] {DbStore.class, Models.Draw.class},
                            new Object[] {store, draw});
        }
        if (result == REFLECTION_NO_METHOD) {
            result =
                    invokeOptional(
                            engine,
                            "runAfterDraw",
                            new Class[] {DbStore.class, Models.Draw.class},
                            new Object[] {store, draw});
        }
        if (result == REFLECTION_NO_METHOD) {
            result =
                    invokeOptional(
                            engine,
                            "learnOneDraw",
                            new Class[] {DbStore.class, Models.Draw.class},
                            new Object[] {store, draw});
        }
        if (result == REFLECTION_NO_METHOD) {
            result =
                    invokeOptional(
                            engine,
                            "learnLatest",
                            new Class[] {DbStore.class},
                            new Object[] {store});
        }
        if (result == REFLECTION_NO_METHOD) {
            result =
                    invokeOptional(
                            engine, "runTick", new Class[] {DbStore.class}, new Object[] {store});
        }
        if (result == REFLECTION_NO_METHOD) {
            result =
                    invokeOptional(
                            engine, "tick", new Class[] {DbStore.class}, new Object[] {store});
        }

        if (result == REFLECTION_NO_METHOD) {
            return "";
        }

        return name + "：" + buildGenericInvokeSummary(result, "已执行");
    }

    private String runTeacherMatrixAutoBatchByReflection(String reason, boolean force) {
        Object tmEngine = getEffectiveTeacherMatrixEngine();

        if (tmEngine == null || store == null) {
            return "教师矩阵跳过：引擎为空";
        }

        String latestIssueForTier = getLatestIssueSafe();
        if (!force && !shouldRunTieredBackgroundProfile(latestIssueForTier)) {
            return buildTieredBackgroundSkipSummary("教师矩阵批次", latestIssueForTier);
        }
        currentBackgroundProfileTier = selectBackgroundProfileTier(latestIssueForTier, force);
        currentBackgroundProfileStartedAt = System.currentTimeMillis();

        if (shouldDeferHeavyBackgroundNow()) {
            return "教师矩阵后台批次延后：实时目标期优先";
        }

        Object result;

        result =
                invokeOptional(
                        tmEngine,
                        "runAutoLearnBatch",
                        new Class[] {DbStore.class, int.class},
                        new Object[] {store, Integer.valueOf(AUTO_LEARNING_REALTIME_BATCH)});
        if (result != REFLECTION_NO_METHOD) {
            String s = buildTeacherMatrixInvokeSummary(result, "教师矩阵后台小批量完成");
            lastTeacherMatrixSummary = s;
            setTeacherMatrixState("OK", s);
            broadcastTeacherMatrixUpdated();
            return s;
        }

        result =
                invokeOptional(
                        tmEngine,
                        "learnAutoSmallBatch",
                        new Class[] {DbStore.class, int.class},
                        new Object[] {store, Integer.valueOf(AUTO_LEARNING_REALTIME_BATCH)});
        if (result != REFLECTION_NO_METHOD) {
            String s = buildTeacherMatrixInvokeSummary(result, "教师矩阵后台小批量完成");
            lastTeacherMatrixSummary = s;
            setTeacherMatrixState("OK", s);
            broadcastTeacherMatrixUpdated();
            return s;
        }

        result =
                invokeOptional(
                        tmEngine,
                        "runSmallBatch",
                        new Class[] {DbStore.class, int.class},
                        new Object[] {store, Integer.valueOf(AUTO_LEARNING_REALTIME_BATCH)});
        if (result != REFLECTION_NO_METHOD) {
            String s = buildTeacherMatrixInvokeSummary(result, "教师矩阵后台小批量完成");
            lastTeacherMatrixSummary = s;
            setTeacherMatrixState("OK", s);
            broadcastTeacherMatrixUpdated();
            return s;
        }

        result =
                invokeOptional(
                        tmEngine,
                        "runAutoLearnBatch",
                        new Class[] {DbStore.class},
                        new Object[] {store});
        if (result != REFLECTION_NO_METHOD) {
            String s = buildTeacherMatrixInvokeSummary(result, "教师矩阵后台批次完成");
            lastTeacherMatrixSummary = s;
            setTeacherMatrixState("OK", s);
            broadcastTeacherMatrixUpdated();
            return s;
        }

        result =
                invokeOptional(
                        tmEngine, "runTick", new Class[] {DbStore.class}, new Object[] {store});
        if (result != REFLECTION_NO_METHOD) {
            String s = buildTeacherMatrixInvokeSummary(result, "教师矩阵后台tick完成");
            lastTeacherMatrixSummary = s;
            setTeacherMatrixState("OK", s);
            broadcastTeacherMatrixUpdated();
            return s;
        }

        result =
                invokeOptional(tmEngine, "tick", new Class[] {DbStore.class}, new Object[] {store});
        if (result != REFLECTION_NO_METHOD) {
            String s = buildTeacherMatrixInvokeSummary(result, "教师矩阵后台tick完成");
            lastTeacherMatrixSummary = s;
            setTeacherMatrixState("OK", s);
            broadcastTeacherMatrixUpdated();
            return s;
        }

        try {
            Models.Draw latest = store.getLatestDraw();
            if (latest != null && latest.isValid()) {
                String s =
                        runTeacherMatrixManualProfileFallback(
                                latest, latest.issue == null ? "" : latest.issue);
                lastTeacherMatrixSummary = s;
                setTeacherMatrixState("OK", s);
                broadcastTeacherMatrixUpdated();
                return s;
            }
        } catch (Throwable e) {
        }

        return "教师矩阵跳过：未找到后台学习入口";
    }

    private String runV20CauseChainAutoBatchByReflection(String reason, boolean force) {
        if (store == null) {
            return "V20因链跳过：DbStore为空";
        }

        String latestIssueForTier = getLatestIssueSafe();
        if (!force && !shouldRunTieredBackgroundProfile(latestIssueForTier)) {
            return buildTieredBackgroundSkipSummary("V20因链批次", latestIssueForTier);
        }
        currentBackgroundProfileTier = selectBackgroundProfileTier(latestIssueForTier, force);
        currentBackgroundProfileStartedAt = System.currentTimeMillis();

        if (dynamicCauseChainEngine == null
                && causalFactorMatrixEngine == null
                && positionCauseMatrixEngine == null
                && teacherNumberMatrixEngine == null
                && counterfactualRankEngine == null
                && thirdGuardMatrixEngine == null
                && ruleHealthMatrixEngine == null
                && shadowHealthGateEngine == null
                && falseStrongFreezeEngine == null
                && seedCauseExpansionEngine == null) {
            return "V20因链跳过：引擎为空";
        }

        if (shouldDeferHeavyBackgroundNow()) {
            return "V20因链后台批次延后：实时目标期优先";
        }

        String summary = "";

        summary =
                appendReason(
                        summary, runOneV20AutoBatch(dynamicCauseChainEngine, "DynamicCauseChain"));
        summary =
                appendReason(
                        summary,
                        runOneV20AutoBatch(causalFactorMatrixEngine, "CausalFactorMatrix"));
        summary =
                appendReason(
                        summary,
                        runOneV20AutoBatch(positionCauseMatrixEngine, "PositionCauseMatrix"));
        summary =
                appendReason(
                        summary,
                        runOneV20AutoBatch(teacherNumberMatrixEngine, "TeacherNumberMatrix"));
        summary =
                appendReason(
                        summary,
                        runOneV20AutoBatch(counterfactualRankEngine, "CounterfactualRank"));
        summary =
                appendReason(
                        summary, runOneV20AutoBatch(thirdGuardMatrixEngine, "ThirdGuardMatrix"));
        summary =
                appendReason(
                        summary, runOneV20AutoBatch(ruleHealthMatrixEngine, "RuleHealthMatrix"));
        summary =
                appendReason(
                        summary, runOneV20AutoBatch(shadowHealthGateEngine, "ShadowHealthGate"));
        summary =
                appendReason(
                        summary, runOneV20AutoBatch(falseStrongFreezeEngine, "FalseStrongFreeze"));
        summary =
                appendReason(
                        summary,
                        runOneV20AutoBatch(seedCauseExpansionEngine, "SeedCauseExpansion"));

        if (summary == null || summary.length() == 0) {
            try {
                Models.Draw latest = store.getLatestDraw();
                if (latest != null && latest.isValid()) {
                    summary =
                            runV20BackgroundProfileFallback(
                                    latest, latest.issue == null ? "" : latest.issue);
                }
            } catch (Throwable e) {
            }
        }

        if (summary == null || summary.length() == 0) {
            summary = "V20因链后台完成：未找到批量入口，但已保持实时主链画像入口";
        }

        lastV20CauseChainSummary = summary;
        setV20CauseChainState("OK", summary);
        broadcastV20CauseChainUpdated();
        return summary;
    }

    private String runOneV20AutoBatch(Object engine, String name) {
        if (engine == null || store == null) {
            return "";
        }

        Object result = REFLECTION_NO_METHOD;

        result =
                invokeOptional(
                        engine,
                        "runAutoLearnBatch",
                        new Class[] {DbStore.class, int.class},
                        new Object[] {store, Integer.valueOf(AUTO_LEARNING_REALTIME_BATCH)});
        if (result == REFLECTION_NO_METHOD) {
            result =
                    invokeOptional(
                            engine,
                            "learnAutoSmallBatch",
                            new Class[] {DbStore.class, int.class},
                            new Object[] {store, Integer.valueOf(AUTO_LEARNING_REALTIME_BATCH)});
        }
        if (result == REFLECTION_NO_METHOD) {
            result =
                    invokeOptional(
                            engine,
                            "runSmallBatch",
                            new Class[] {DbStore.class, int.class},
                            new Object[] {store, Integer.valueOf(AUTO_LEARNING_REALTIME_BATCH)});
        }
        if (result == REFLECTION_NO_METHOD) {
            result =
                    invokeOptional(
                            engine,
                            "runAutoLearnBatch",
                            new Class[] {DbStore.class},
                            new Object[] {store});
        }
        if (result == REFLECTION_NO_METHOD) {
            result =
                    invokeOptional(
                            engine,
                            "observeBatch",
                            new Class[] {DbStore.class},
                            new Object[] {store});
        }
        if (result == REFLECTION_NO_METHOD) {
            result =
                    invokeOptional(
                            engine,
                            "profileBatch",
                            new Class[] {DbStore.class},
                            new Object[] {store});
        }
        if (result == REFLECTION_NO_METHOD) {
            result =
                    invokeOptional(
                            engine, "runTick", new Class[] {DbStore.class}, new Object[] {store});
        }
        if (result == REFLECTION_NO_METHOD) {
            result =
                    invokeOptional(
                            engine, "tick", new Class[] {DbStore.class}, new Object[] {store});
        }

        if (result == REFLECTION_NO_METHOD) {
            return "";
        }

        return name + "：" + buildGenericInvokeSummary(result, "后台批次完成");
    }

    private Object getEffectiveTeacherMatrixEngine() {
        return teacherNumberMatrixEngine;
    }

    private String runTeacherMatrixManualProfileFallback(Models.Draw draw, String issue) {
        if (store == null || draw == null || !draw.isValid()) {
            return "教师矩阵兜底跳过：开奖无效";
        }

        try {
            List<Models.Draw> history =
                    store.getRecentDraws(getTieredBackgroundHistoryLimit(issue));
            ArrayList<Models.Draw> clean =
                    dataGuard == null
                            ? new ArrayList<Models.Draw>()
                            : dataGuard.normalizeHistory(history);

            if (clean == null || clean.size() < AppConfig.MIN_DRAWS_ANALYZE + 2) {
                return "教师矩阵兜底跳过：历史不足";
            }

            if (teacherNumberMatrixEngine != null) {
                Object result =
                        invokeOptional(
                                teacherNumberMatrixEngine,
                                "apply",
                                new Class[] {
                                    DbStore.class,
                                    List.class,
                                    List.class,
                                    List.class,
                                    List.class,
                                    List.class,
                                    String.class,
                                    String.class
                                },
                                new Object[] {
                                    store,
                                    new ArrayList<Models.ComboCandidate>(),
                                    clean,
                                    new ArrayList<Models.ShapeVector>(),
                                    new ArrayList<TrendStateMachine.TrendTrace>(),
                                    new ArrayList<BreakRecoveryEngine.RecoveryResult>(),
                                    issue,
                                    dataGuard == null ? "" : dataGuard.buildNextIssue(issue)
                                });

                if (result != REFLECTION_NO_METHOD) {
                    return "教师矩阵兜底完成："
                            + buildGenericInvokeSummary(result, "TeacherNumberMatrix已写入画像");
                }
            }

            store.setState("teacher_matrix_fallback_issue", issue == null ? "" : issue);
            store.setState(
                    "teacher_matrix_fallback_updated_at",
                    String.valueOf(System.currentTimeMillis()));
            store.setState("teacher_matrix_fallback_scope", "OBSERVE_ONLY_NO_REAL_CALIBRATION");

            return "教师矩阵兜底完成：历史=" + clean.size() + "，仅写观察画像，不救援BET，不污染真实校准；实时字段桥接由正式分析链完成";
        } catch (Throwable e) {
            return "教师矩阵兜底异常：" + safeError(e);
        }
    }

    private String runV20BackgroundProfileFallback(Models.Draw draw, String issue) {
        if (store == null || draw == null || !draw.isValid()) {
            return "V20因链兜底跳过：开奖无效";
        }

        try {
            List<Models.Draw> history =
                    store.getRecentDraws(getTieredBackgroundHistoryLimit(issue));
            ArrayList<Models.Draw> clean =
                    dataGuard == null
                            ? new ArrayList<Models.Draw>()
                            : dataGuard.normalizeHistory(history);

            if (clean == null || clean.size() < AppConfig.MIN_DRAWS_ANALYZE + 2) {
                return "V20因链兜底跳过：历史不足";
            }

            Models.Draw latest = clean.get(clean.size() - 1);
            if (latest == null || !latest.isValid()) {
                return "V20因链兜底跳过：最新开奖无效";
            }

            String baseIssue = latest.issue == null ? "" : latest.issue;
            String targetIssue = dataGuard == null ? "" : dataGuard.buildNextIssue(baseIssue);

            List<Models.ShapeVector> vectors = windowFeatureEngine.buildAllPositions(clean);
            vectors = shapeStateEngine.refineAll(vectors);
            vectors = markovEngine.applyToVectors(vectors, clean);

            List<Models.ShapeVector> recentVectors =
                    store.getRecentShapeVectors(AppConfig.POSITION_COUNT * 80);
            ArrayList<TrendStateMachine.TrendTrace> traces =
                    trendStateMachine.analyzeAll(recentVectors, vectors);
            vectors = trendStateMachine.refineAllByTrace(recentVectors, vectors);

            ArrayList<BreakRecoveryEngine.RecoveryResult> recoveries =
                    breakRecoveryEngine.analyzeAll(vectors, traces);

            Models.CandidateBatch batch =
                    runV20FullCandidateScanOrFallback(clean, vectors, traces, recoveries);

            ArrayList<Models.ComboCandidate> candidates = new ArrayList<Models.ComboCandidate>();

            if (batch != null && batch.candidates != null) {
                candidates =
                        capCandidatesByScore(
                                batch.candidates, getTieredBackgroundCandidateLimit(issue));
            }

            if (candidates.size() == 0) {
                store.setState("v20_cause_chain_fallback_issue", issue == null ? "" : issue);
                store.setState("v20_cause_chain_fallback_summary", "V20因链兜底完成：候选为空，仅记录观察状态");
                store.setState(
                        "v20_cause_chain_fallback_scope", "OBSERVE_ONLY_NO_REAL_CALIBRATION");
                store.setState(
                        "v20_cause_chain_fallback_updated_at",
                        String.valueOf(System.currentTimeMillis()));
                return "V20因链兜底完成：候选为空，仅记录观察状态";
            }

            candidates =
                    runOneV20BackgroundCandidatePass(
                            causalFactorMatrixEngine,
                            "causal_factor_matrix_bg",
                            candidates,
                            clean,
                            vectors,
                            traces,
                            recoveries,
                            baseIssue,
                            targetIssue);
            candidates =
                    runOneV20BackgroundCandidatePass(
                            positionCauseMatrixEngine,
                            "position_cause_matrix_bg",
                            candidates,
                            clean,
                            vectors,
                            traces,
                            recoveries,
                            baseIssue,
                            targetIssue);
            candidates =
                    runOneV20BackgroundCandidatePass(
                            teacherNumberMatrixEngine,
                            "teacher_number_matrix_bg",
                            candidates,
                            clean,
                            vectors,
                            traces,
                            recoveries,
                            baseIssue,
                            targetIssue);
            candidates =
                    runOneV20BackgroundCandidatePass(
                            counterfactualRankEngine,
                            "counterfactual_rank_bg",
                            candidates,
                            clean,
                            vectors,
                            traces,
                            recoveries,
                            baseIssue,
                            targetIssue);
            candidates =
                    runOneV20BackgroundCandidatePass(
                            seedCauseExpansionEngine,
                            "seed_cause_expansion_bg",
                            candidates,
                            clean,
                            vectors,
                            traces,
                            recoveries,
                            baseIssue,
                            targetIssue);
            candidates =
                    runOneV20BackgroundCandidatePass(
                            dynamicCauseChainEngine,
                            "dynamic_cause_chain_bg_pre",
                            candidates,
                            clean,
                            vectors,
                            traces,
                            recoveries,
                            baseIssue,
                            targetIssue);
            candidates =
                    runOneV20BackgroundCandidatePass(
                            thirdGuardMatrixEngine,
                            "third_guard_matrix_bg",
                            candidates,
                            clean,
                            vectors,
                            traces,
                            recoveries,
                            baseIssue,
                            targetIssue);
            candidates =
                    runOneV20BackgroundCandidatePass(
                            ruleHealthMatrixEngine,
                            "rule_health_matrix_bg",
                            candidates,
                            clean,
                            vectors,
                            traces,
                            recoveries,
                            baseIssue,
                            targetIssue);
            candidates =
                    runOneV20BackgroundCandidatePass(
                            shadowHealthGateEngine,
                            "shadow_health_gate_bg",
                            candidates,
                            clean,
                            vectors,
                            traces,
                            recoveries,
                            baseIssue,
                            targetIssue);
            candidates =
                    runOneV20BackgroundCandidatePass(
                            falseStrongFreezeEngine,
                            "false_strong_freeze_bg",
                            candidates,
                            clean,
                            vectors,
                            traces,
                            recoveries,
                            baseIssue,
                            targetIssue);
            candidates =
                    runOneV20BackgroundCandidatePass(
                            dynamicCauseChainEngine,
                            "dynamic_cause_chain_bg_post",
                            candidates,
                            clean,
                            vectors,
                            traces,
                            recoveries,
                            baseIssue,
                            targetIssue);
            candidates =
                    runTeacherNumberMatrixFinalBridge(
                            candidates,
                            clean,
                            vectors,
                            traces,
                            recoveries,
                            baseIssue,
                            targetIssue,
                            "BACKGROUND_PROFILE");

            candidates = capCandidatesByScore(candidates, getTieredBackgroundCandidateLimit(issue));

            String top = buildV20BackgroundCandidateSummary(candidates);

            store.setState("v20_cause_chain_fallback_issue", issue == null ? "" : issue);
            store.setState("v20_cause_chain_fallback_base_issue", baseIssue);
            store.setState("v20_cause_chain_fallback_target_issue", targetIssue);
            store.setState(
                    "v20_cause_chain_fallback_candidate_count", String.valueOf(candidates.size()));
            store.setState("v20_cause_chain_fallback_top", top);
            store.setState("v20_cause_chain_fallback_scope", "OBSERVE_ONLY_NO_REAL_CALIBRATION");
            store.setState(
                    "v20_cause_chain_fallback_updated_at",
                    String.valueOf(System.currentTimeMillis()));

            return "V20因链画像兜底完成：issue="
                    + issue
                    + "，历史="
                    + clean.size()
                    + "，候选="
                    + candidates.size()
                    + "，"
                    + top
                    + "；仅观察画像，不救援BET，不写真实盈亏/校准";
        } catch (Throwable e) {
            return "V20因链兜底异常：" + safeError(e);
        }
    }

    private ArrayList<Models.ComboCandidate> runOneV20BackgroundCandidatePass(
            Object engine,
            String statePrefix,
            ArrayList<Models.ComboCandidate> candidates,
            List<Models.Draw> clean,
            List<Models.ShapeVector> vectors,
            List<TrendStateMachine.TrendTrace> traces,
            List<BreakRecoveryEngine.RecoveryResult> recoveries,
            String baseIssue,
            String targetIssue) {
        if (engine == null || candidates == null || candidates.size() == 0) {
            return candidates == null ? new ArrayList<Models.ComboCandidate>() : candidates;
        }

        ArrayList<Models.ComboCandidate> out =
                runCandidateEngineListReturn(
                        engine,
                        statePrefix,
                        candidates,
                        clean,
                        vectors,
                        traces,
                        recoveries,
                        baseIssue,
                        targetIssue);

        if (out == null || out.size() == 0) {
            out = candidates;
        }

        return out;
    }

    private String buildV20BackgroundCandidateSummary(List<Models.ComboCandidate> candidates) {
        if (candidates == null || candidates.size() == 0) {
            return "候选为空";
        }

        StringBuilder sb = new StringBuilder();
        int shown = 0;

        for (int i = 0; i < candidates.size(); i++) {
            Models.ComboCandidate c = candidates.get(i);
            if (c == null || !c.isValidK3()) {
                continue;
            }

            if (shown >= 3) {
                break;
            }

            normalizeCandidateForMainlineSort(c);
            syncTeacherAliasFields(c);

            if (sb.length() > 0) {
                sb.append(" | ");
            }

            sb.append("#");
            sb.append(shown + 1);
            sb.append(" P");
            sb.append(c.position);
            sb.append(" ");
            sb.append(UiFormatter.numberGroup(c.n1, c.n2, c.n3));
            sb.append(" F=");
            sb.append(UiFormatter.probability(c.finalP));
            sb.append(" G=");
            sb.append(UiFormatter.probability(c.dynamicGate));
            sb.append(" E=");
            sb.append(UiFormatter.signedProbability(c.edge));

            int tmSample = readTeacherSample(c);
            String tmStatus = readTeacherStatus(c);
            double tmHit = readTeacherHitRate(c);
            if (tmSample > 0 || tmStatus.length() > 0) {
                sb.append(" TM=");
                sb.append(tmStatus);
                sb.append("/");
                sb.append(tmSample);
                sb.append("/");
                sb.append(UiFormatter.probability(tmHit));
            }

            double cause =
                    maxDoubleFields(
                            c,
                            new String[] {
                                "dynamicCauseScore",
                                "causeChainScore",
                                "causeScore",
                                "finalCauseScore"
                            });
            if (cause > 0.0d) {
                sb.append(" Cause=");
                sb.append(UiFormatter.score(cause));
            }

            shown++;
        }

        String out = sb.toString();
        if (out.length() == 0) {
            out = "候选未形成有效K3";
        }

        if (out.length() > V20_BACKGROUND_SUMMARY_LIMIT) {
            out = out.substring(0, V20_BACKGROUND_SUMMARY_LIMIT);
        }

        return out;
    }

    private String buildTeacherMatrixInvokeSummary(Object result, String fallback) {
        String saved = "";

        try {
            if (store != null) {
                saved = store.getState("teacher_matrix_summary", "");
            }
        } catch (Throwable e) {
            saved = "";
        }

        if (saved != null
                && saved.length() > 0
                && saved.indexOf("RUNNING") < 0
                && saved.indexOf("启动") < 0) {
            return saved;
        }

        return buildGenericInvokeSummary(result, fallback == null ? "教师矩阵已执行" : fallback);
    }

    private String buildGenericInvokeSummary(Object result, String fallback) {
        if (result == null) {
            return fallback == null ? "已执行" : fallback;
        }

        String summary = readString(result, "summary", "");
        if (summary.length() == 0) {
            summary = readString(result, "detail", "");
        }
        if (summary.length() == 0) {
            summary = readString(result, "reason", "");
        }
        if (summary.length() == 0) {
            summary = readString(result, "message", "");
        }

        int samples = readInt(result, "sampleCount", 0);
        if (samples <= 0) {
            samples = readInt(result, "samples", 0);
        }

        int profiles = readInt(result, "profileCount", 0);
        if (profiles <= 0) {
            profiles = readInt(result, "updatedProfiles", 0);
        }

        if (summary.length() == 0) {
            summary = String.valueOf(result);
        }

        if (summary == null || summary.length() == 0 || summary.indexOf("@") >= 0) {
            summary = fallback == null ? "已执行" : fallback;
        }

        if (samples > 0 || profiles > 0) {
            summary = appendReason(summary, "样本=" + samples + "，画像=" + profiles);
        }

        if (summary.length() > 220) {
            summary = summary.substring(0, 220);
        }

        return summary;
    }

    private void setTeacherMatrixState(String status, String summary) {
        if (store == null) {
            return;
        }

        try {
            store.setState("teacher_matrix_status", status == null ? "" : status);
            store.setState("teacher_matrix_running", "RUNNING".equals(status) ? "1" : "0");
            store.setState("teacher_matrix_summary", summary == null ? "" : summary);
            store.setState("teacher_matrix_updated_at", String.valueOf(System.currentTimeMillis()));
        } catch (Throwable e) {
        }
    }

    private void setV20CauseChainState(String status, String summary) {
        if (store == null) {
            return;
        }

        try {
            store.setState("v20_cause_chain_status", status == null ? "" : status);
            store.setState("v20_cause_chain_running", "RUNNING".equals(status) ? "1" : "0");
            store.setState("v20_cause_chain_summary", summary == null ? "" : summary);
            store.setState(
                    "v20_cause_chain_updated_at", String.valueOf(System.currentTimeMillis()));
        } catch (Throwable e) {
        }
    }

    private String runLearningEngineByReflection() {
        if (learningEngine == null) {
            return "后台学习跳过：LearningEngine为空";
        }

        if (shouldDeferHeavyBackgroundNow()) {
            return "后台学习延后：实时目标期优先";
        }

        Object result;

        result =
                invokeOptional(
                        learningEngine,
                        "runAutoLearnBatch",
                        new Class[] {DbStore.class, int.class},
                        new Object[] {store, Integer.valueOf(AUTO_LEARNING_REALTIME_BATCH)});
        if (result != REFLECTION_NO_METHOD) {
            return buildLearningInvokeSummary(result);
        }

        result =
                invokeOptional(
                        learningEngine,
                        "learnAutoSmallBatch",
                        new Class[] {DbStore.class, int.class},
                        new Object[] {store, Integer.valueOf(AUTO_LEARNING_REALTIME_BATCH)});
        if (result != REFLECTION_NO_METHOD) {
            return buildLearningInvokeSummary(result);
        }

        result =
                invokeOptional(
                        learningEngine,
                        "runSmallRandomSegments",
                        new Class[] {DbStore.class, int.class, int.class},
                        new Object[] {
                            store,
                            Integer.valueOf(300),
                            Integer.valueOf(AUTO_LEARNING_REALTIME_BATCH)
                        });
        if (result != REFLECTION_NO_METHOD) {
            return buildLearningInvokeSummary(result);
        }

        result =
                invokeOptional(
                        learningEngine,
                        "runSmallRandomSegments",
                        new Class[] {DbStore.class, int.class},
                        new Object[] {store, Integer.valueOf(300)});
        if (result != REFLECTION_NO_METHOD) {
            return buildLearningInvokeSummary(result);
        }

        result =
                invokeOptional(
                        learningEngine,
                        "runAutoLearnBatch",
                        new Class[] {DbStore.class},
                        new Object[] {store});
        if (result != REFLECTION_NO_METHOD) {
            return buildLearningInvokeSummary(result);
        }

        result =
                invokeOptional(
                        learningEngine,
                        "learnAutoSmallBatch",
                        new Class[] {DbStore.class},
                        new Object[] {store});
        if (result != REFLECTION_NO_METHOD) {
            return buildLearningInvokeSummary(result);
        }

        result =
                invokeOptional(
                        learningEngine,
                        "runOneBatch",
                        new Class[] {DbStore.class},
                        new Object[] {store});
        if (result != REFLECTION_NO_METHOD) {
            return buildLearningInvokeSummary(result);
        }

        result =
                invokeOptional(
                        learningEngine,
                        "runAutoLearnTick",
                        new Class[] {DbStore.class},
                        new Object[] {store});
        if (result != REFLECTION_NO_METHOD) {
            return buildLearningInvokeSummary(result);
        }

        result =
                invokeOptional(
                        learningEngine,
                        "runTick",
                        new Class[] {DbStore.class},
                        new Object[] {store});
        if (result != REFLECTION_NO_METHOD) {
            return buildLearningInvokeSummary(result);
        }

        result =
                invokeOptional(
                        learningEngine, "tick", new Class[] {DbStore.class}, new Object[] {store});
        if (result != REFLECTION_NO_METHOD) {
            return buildLearningInvokeSummary(result);
        }

        result = invokeOptional(learningEngine, "runOneBatch", new Class[] {}, new Object[] {});
        if (result != REFLECTION_NO_METHOD) {
            return buildLearningInvokeSummary(result);
        }

        result =
                invokeOptional(learningEngine, "runAutoLearnTick", new Class[] {}, new Object[] {});
        if (result != REFLECTION_NO_METHOD) {
            return buildLearningInvokeSummary(result);
        }

        result = invokeOptional(learningEngine, "tick", new Class[] {}, new Object[] {});
        if (result != REFLECTION_NO_METHOD) {
            return buildLearningInvokeSummary(result);
        }

        return "后台学习跳过：LearningEngine没有找到可调用入口，实时分析不受影响";
    }

    private String buildLearningInvokeSummary(Object result) {
        String saved = "";

        try {
            if (store != null) {
                saved = store.getState("auto_learn_summary", "");
            }
        } catch (Throwable e) {
            saved = "";
        }

        if (saved != null && saved.length() > 0 && saved.indexOf("启动") < 0) {
            return saved;
        }

        if (result == null) {
            return "后台学习批次已执行：本轮入口无返回值";
        }

        String text = String.valueOf(result);

        if (text == null || text.length() == 0) {
            return "后台学习批次已执行";
        }

        if (text.length() > 160) {
            text = text.substring(0, 160);
        }

        return "后台学习完成：" + text;
    }

    private void runDeepLearningBridgeByReflection() {
        if (deepLearningBridgeEngine == null || store == null) {
            return;
        }

        if (shouldDeferHeavyBackgroundNow()) {
            return;
        }

        Object result;

        result =
                invokeOptional(
                        deepLearningBridgeEngine,
                        "runOneBatch",
                        new Class[] {DbStore.class},
                        new Object[] {store});
        if (result != REFLECTION_NO_METHOD) {
            store.setState("deep_learning_bridge_summary", buildBridgeInvokeSummary(result));
            return;
        }

        result =
                invokeOptional(
                        deepLearningBridgeEngine,
                        "syncFromLearning",
                        new Class[] {DbStore.class},
                        new Object[] {store});
        if (result != REFLECTION_NO_METHOD) {
            store.setState("deep_learning_bridge_summary", buildBridgeInvokeSummary(result));
            return;
        }

        result =
                invokeOptional(
                        deepLearningBridgeEngine,
                        "tick",
                        new Class[] {DbStore.class},
                        new Object[] {store});
        if (result != REFLECTION_NO_METHOD) {
            store.setState("deep_learning_bridge_summary", buildBridgeInvokeSummary(result));
            return;
        }

        result =
                invokeOptional(
                        deepLearningBridgeEngine, "runOneBatch", new Class[] {}, new Object[] {});
        if (result != REFLECTION_NO_METHOD) {
            store.setState("deep_learning_bridge_summary", buildBridgeInvokeSummary(result));
            return;
        }

        result = invokeOptional(deepLearningBridgeEngine, "tick", new Class[] {}, new Object[] {});
        if (result != REFLECTION_NO_METHOD) {
            store.setState("deep_learning_bridge_summary", buildBridgeInvokeSummary(result));
        }
    }

    private String buildBridgeInvokeSummary(Object result) {
        if (result == null) {
            return "深度学习桥接已同步";
        }

        String text = String.valueOf(result);

        if (text == null || text.length() == 0) {
            return "深度学习桥接已同步";
        }

        if (text.length() > 160) {
            text = text.substring(0, 160);
        }

        return text;
    }

    private Object createOptionalEngine(String className) {
        try {
            Class cls = Class.forName(className);
            return cls.newInstance();
        } catch (Throwable e) {
            return null;
        }
    }

    private Object invokeOptional(Object target, String methodName, Class[] types, Object[] args) {
        if (target == null || methodName == null || methodName.length() == 0) {
            return REFLECTION_NO_METHOD;
        }

        try {
            Method m = target.getClass().getMethod(methodName, types);
            return m.invoke(target, args);
        } catch (Throwable e) {
            try {
                Method m = target.getClass().getDeclaredMethod(methodName, types);
                m.setAccessible(true);
                return m.invoke(target, args);
            } catch (Throwable ignored) {
                return REFLECTION_NO_METHOD;
            }
        }
    }

    private ArrayList<Models.ComboCandidate> castCandidateList(Object obj) {
        ArrayList<Models.ComboCandidate> out = new ArrayList<Models.ComboCandidate>();

        if (obj == null || obj == REFLECTION_NO_METHOD) {
            return out;
        }

        if (obj instanceof Models.CandidateBatch) {
            Models.CandidateBatch b = (Models.CandidateBatch) obj;
            if (b.candidates != null) {
                for (int i = 0; i < b.candidates.size(); i++) {
                    Models.ComboCandidate c = b.candidates.get(i);
                    if (c != null) {
                        out.add(c);
                    }
                }
            }
            return out;
        }

        if (obj instanceof List) {
            List list = (List) obj;
            for (int i = 0; i < list.size(); i++) {
                Object x = list.get(i);
                if (x instanceof Models.ComboCandidate) {
                    out.add((Models.ComboCandidate) x);
                }
            }
        }

        return out;
    }

    private void postRealBetDiagnosisRefresh(String reason, long delayMs, boolean force) {
        if (!running || store == null || learningHandler == null) {
            return;
        }
        long now = System.currentTimeMillis();
        if (!force
                && lastRealBetDiagnosisAt > 0L
                && now - lastRealBetDiagnosisAt < REAL_BET_DIAG_MIN_INTERVAL_MS) {
            return;
        }
        if (delayMs < 0L) {
            delayMs = 0L;
        }
        pendingRealBetDiagnosisReason = reason == null ? "" : reason;
        if (realBetDiagnosisPostPending) {
            return;
        }
        realBetDiagnosisPostPending = true;
        try {
            learningHandler.postDelayed(realBetDiagnosisLoop, delayMs);
            setRealBetDiagnosisState("WAITING", "真实BET复盘诊断已排队：" + pendingRealBetDiagnosisReason);
        } catch (Throwable e) {
            realBetDiagnosisPostPending = false;
        }
    }

    private void runRealBetDiagnosisWorker(String reason) {
        if (!running || store == null) {
            return;
        }
        if (realBetDiagnosisBusy) {
            postRealBetDiagnosisRefresh("诊断线程忙，延后刷新", REAL_BET_DIAG_IDLE_DELAY_MS, false);
            return;
        }
        if (analysisBusy || isRealtimePriorityActive()) {
            postRealBetDiagnosisRefresh("实时出号优先，真实BET诊断延后", REAL_BET_DIAG_IDLE_DELAY_MS, false);
            return;
        }
        realBetDiagnosisBusy = true;
        long started = System.currentTimeMillis();
        try {
            if (realBetDiagnosisEngine == null) {
                realBetDiagnosisEngine = new RealBetDiagnosisEngine();
            }
            setRealBetDiagnosisState("RUNNING", "正在刷新真实BET复盘诊断：" + (reason == null ? "" : reason));
            RealBetDiagnosisEngine.DiagnosisReport report = null;
            try {
                report =
                        realBetDiagnosisEngine.diagnose(
                                store, REAL_BET_DIAG_BET_LIMIT, REAL_BET_DIAG_SHADOW_LIMIT);
            } catch (Throwable e) {
                report = null;
            }
            if (report != null) {
                try {
                    realBetDiagnosisEngine.saveDiagnosisState(store, report);
                } catch (Throwable e) {
                }
                lastRealBetDiagnosisSummary = report.summary == null ? "" : report.summary;
            } else {
                try {
                    realBetDiagnosisEngine.saveDiagnosisState(store);
                } catch (Throwable e) {
                }
                lastRealBetDiagnosisSummary = readRealBetDiagnosisSummarySafe();
            }
            lastRealBetDiagnosisAt = System.currentTimeMillis();
            long cost = lastRealBetDiagnosisAt - started;
            setRealBetDiagnosisState(
                    "OK",
                    "真实BET复盘诊断已刷新，用时" + cost + "ms；" + limitText(lastRealBetDiagnosisSummary, 180));
            broadcastSimpleStateLimited(false);
            postFullDiagnosisRefresh("真实BET复盘诊断完成后刷新满级诊断闭环", 1200L, true);
        } catch (Throwable e) {
            lastError = safeError(e);
            setRealBetDiagnosisState("ERROR", "真实BET复盘诊断异常：" + lastError);
        } finally {
            realBetDiagnosisBusy = false;
        }
    }

    private void setRealBetDiagnosisState(String status, String summary) {
        if (store == null) {
            return;
        }
        try {
            store.setState("real_bet_diagnosis_worker_status", status == null ? "" : status);
            store.setState("real_bet_diagnosis_worker_summary", summary == null ? "" : summary);
            store.setState(
                    "real_bet_diagnosis_worker_updated_at",
                    String.valueOf(System.currentTimeMillis()));
            store.setState("real_bet_diagnosis_patch_version", V20_REAL_BET_DIAG_PATCH_VERSION);
        } catch (Throwable e) {
        }
    }

    private String readRealBetDiagnosisSummarySafe() {
        if (store == null) {
            return "";
        }
        try {
            return store.getState("real_bet_diagnosis_summary", "");
        } catch (Throwable e) {
            return "";
        }
    }

    private boolean isRealtimePriorityActive() {
        long until = realtimePriorityUntilAt;
        return until > 0L && System.currentTimeMillis() < until;
    }

    private void postModuleEffectRefresh(String reason, long delayMs, boolean force) {
        postFullDiagnosisRefresh(appendReason("模块评分刷新", reason), delayMs, force);
    }

    private void postShadowPromotionRefresh(String reason, long delayMs, boolean force) {
        postFullDiagnosisRefresh(appendReason("SHADOW晋级刷新", reason), delayMs, force);
    }

    private void postFreezeMatrixRefresh(String reason, long delayMs, boolean force) {
        postFullDiagnosisRefresh(appendReason("动态冻结矩阵刷新", reason), delayMs, force);
    }

    private void postAdaptiveJudgeRefresh(String reason, long delayMs, boolean force) {
        postFullDiagnosisRefresh(appendReason("智能审判刷新", reason), delayMs, force);
    }

    private void postFixPlanRefresh(String reason, long delayMs, boolean force) {
        postFullDiagnosisRefresh(appendReason("文件修复计划刷新", reason), delayMs, force);
    }

    private void postFullDiagnosisRefresh(String reason, long delayMs, boolean force) {
        if (!running || store == null || learningHandler == null) {
            return;
        }
        long now = System.currentTimeMillis();
        if (!force
                && lastFullDiagnosisLoopAt > 0L
                && now - lastFullDiagnosisLoopAt < FULL_DIAG_LOOP_MIN_INTERVAL_MS) {
            return;
        }
        if (delayMs < 0L) {
            delayMs = 0L;
        }
        pendingFullDiagnosisLoopReason = reason == null ? "" : reason;
        pendingFullDiagnosisLoopForce = force;
        if (fullDiagnosisLoopPostPending) {
            return;
        }
        fullDiagnosisLoopPostPending = true;
        try {
            learningHandler.postDelayed(fullDiagnosisLoop, delayMs);
            setFullDiagnosisLoopState("WAITING", "满级诊断闭环已排队：" + pendingFullDiagnosisLoopReason);
        } catch (Throwable e) {
            fullDiagnosisLoopPostPending = false;
        }
    }

    private void runFullDiagnosisLoopWorker(String reason, boolean force) {
        if (!running || store == null) {
            return;
        }
        if (fullDiagnosisLoopBusy) {
            postFullDiagnosisRefresh("满级诊断闭环线程忙，延后刷新", FULL_DIAG_LOOP_IDLE_DELAY_MS, false);
            return;
        }
        if (!force && (analysisBusy || isRealtimePriorityActive())) {
            boolean snapshotReady = "READY".equals(
                    store.getState("r923_freeze_snapshot_status", ""));
            postFullDiagnosisRefresh(
                    "实时出号优先，满级诊断闭环延后",
                    snapshotReady ? FULL_DIAG_LOOP_IDLE_DELAY_MS : 5000L,
                    false);
            return;
        }

        fullDiagnosisLoopBusy = true;
        long started = System.currentTimeMillis();
        try {
            setFullDiagnosisLoopState("RUNNING", "正在刷新满级诊断闭环：" + (reason == null ? "" : reason));
            ensureDiagnosisLoopStorageReady();
            if (AppConfig.ENABLE_V50_PRIME) {
                Object report = refreshRealBetDiagnosisForLoop();
                DynamicFreezeMatrixEngine.FreezeReport freeze =
                        new DynamicFreezeMatrixEngine().buildFreezeReport(store);
                DynamicFreezeRealtimeSnapshotR923.PersistResult snapshot =
                        DynamicFreezeRealtimeSnapshotR923.persist(store, freeze);
                lastFullDiagnosisLoopAt = System.currentTimeMillis();
                lastFullDiagnosisLoopSummary =
                        "V50纯链诊断｜"
                                + diagnosisObjectSummary(report, lastRealBetDiagnosisSummary)
                                + "｜实时冻结快照=" + snapshot.canonical();
                try {
                    store.setState("diagnosis_closed_loop_summary", lastFullDiagnosisLoopSummary);
                    store.setState(
                            "diagnosis_closed_loop_updated_at",
                            String.valueOf(lastFullDiagnosisLoopAt));
                    store.setState(
                            "diagnosis_closed_loop_patch_version",
                            RealBetDiagnosisEngine.V50_DIAGNOSIS_VERSION);
                    store.setState("module_effect_summary", "V50只认开奖后真实消融；旧模块评分未进入实时链");
                    store.setState("shadow_promotion_summary", "V50 SHADOW只由域证书和严格OOS证据晋级");
                    store.setState("dynamic_freeze_summary", "V50冻结对象=专家/域/关系边；旧冻结矩阵只读");
                    store.setState("adaptive_judge_summary", "V50发布门未通过；禁止频率救援和降门槛");
                    store.setState(
                            "diagnosis_fix_plan_summary", "当前优先级=历史引导→每期K3 SHADOW→1/10/100结算");
                } catch (Throwable ignore) {
                }
                setFullDiagnosisLoopState(
                        "OK", "V50纯链诊断已刷新，用时" + (lastFullDiagnosisLoopAt - started) + "ms");
                broadcastSimpleStateLimited(false);
                return;
            }

            StringBuilder sb = new StringBuilder();
            appendDiagStep(sb, "存储", safeInvokeStoreString("refreshDiagnosisClosedLoopState"));

            Object realReport = refreshRealBetDiagnosisForLoop();
            appendDiagStep(
                    sb, "真实BET复盘", diagnosisObjectSummary(realReport, lastRealBetDiagnosisSummary));

            Object moduleReport = refreshModuleEffectForLoop(realReport);
            appendDiagStep(sb, "模块评分", diagnosisObjectSummary(moduleReport, ""));

            Object shadowReport = refreshShadowPromotionForLoop(realReport, moduleReport);
            appendDiagStep(sb, "SHADOW晋级", diagnosisObjectSummary(shadowReport, ""));

            Object freezeReport =
                    refreshFreezeMatrixForLoop(realReport, moduleReport, shadowReport);
            appendDiagStep(sb, "冻结矩阵", diagnosisObjectSummary(freezeReport, ""));
            if (freezeReport instanceof DynamicFreezeMatrixEngine.FreezeReport) {
                DynamicFreezeRealtimeSnapshotR923.PersistResult snapshot =
                        DynamicFreezeRealtimeSnapshotR923.persist(
                                store, (DynamicFreezeMatrixEngine.FreezeReport) freezeReport);
                appendDiagStep(sb, "实时冻结快照", snapshot.canonical());
            } else {
                appendDiagStep(sb, "实时冻结快照", "INVALID_REPORT_TYPE");
            }

            Object judgeReport =
                    refreshAdaptiveJudgeForLoop(moduleReport, shadowReport, freezeReport);
            appendDiagStep(sb, "智能审判", diagnosisObjectSummary(judgeReport, ""));

            Object fixReport =
                    refreshDiagnosisFixPlanForLoop(
                            realReport, moduleReport, shadowReport, freezeReport, judgeReport);
            appendDiagStep(sb, "修复计划", diagnosisObjectSummary(fixReport, ""));

            lastFullDiagnosisLoopAt = System.currentTimeMillis();
            lastFullDiagnosisLoopSummary = limitText(sb.toString(), 360);

            try {
                store.setState("diagnosis_closed_loop_summary", lastFullDiagnosisLoopSummary);
                store.setState(
                        "diagnosis_closed_loop_updated_at",
                        String.valueOf(lastFullDiagnosisLoopAt));
                store.setState(
                        "diagnosis_closed_loop_patch_version", V20_FULL_DIAG_LOOP_PATCH_VERSION);
            } catch (Throwable e) {
            }

            setFullDiagnosisLoopState(
                    "OK",
                    "满级诊断闭环已刷新，用时"
                            + (lastFullDiagnosisLoopAt - started)
                            + "ms；"
                            + limitText(lastFullDiagnosisLoopSummary, 220));
            broadcastSimpleStateLimited(false);
        } catch (Throwable e) {
            lastError = safeError(e);
            setFullDiagnosisLoopState("ERROR", "满级诊断闭环异常：" + lastError);
        } finally {
            fullDiagnosisLoopBusy = false;
        }
    }

    private void ensureDiagnosisLoopStorageReady() {
        if (store == null) {
            return;
        }
        try {
            invokeOptional(store, "ensureDiagnosisLoopTablesNow", new Class[] {}, new Object[] {});
        } catch (Throwable e) {
        }
        try {
            store.setState(
                    "diagnosis_closed_loop_service_version", V20_FULL_DIAG_LOOP_PATCH_VERSION);
            store.setState("diagnosis_closed_loop_service_ready", "1");
        } catch (Throwable e) {
        }
    }

    private Object refreshRealBetDiagnosisForLoop() {
        Object report = null;
        try {
            if (realBetDiagnosisEngine == null) {
                realBetDiagnosisEngine = new RealBetDiagnosisEngine();
            }
            report =
                    realBetDiagnosisEngine.diagnose(
                            store, REAL_BET_DIAG_BET_LIMIT, REAL_BET_DIAG_SHADOW_LIMIT);
            if (report != null) {
                realBetDiagnosisEngine.saveDiagnosisState(
                        store, (RealBetDiagnosisEngine.DiagnosisReport) report);
                lastRealBetDiagnosisSummary =
                        readString(report, "summary", readRealBetDiagnosisSummarySafe());
            } else {
                realBetDiagnosisEngine.saveDiagnosisState(store);
                lastRealBetDiagnosisSummary = readRealBetDiagnosisSummarySafe();
            }
            lastRealBetDiagnosisAt = System.currentTimeMillis();
        } catch (Throwable e) {
            report = null;
        }
        return report;
    }

    private Object refreshModuleEffectForLoop(Object diagnosisReport) {
        if (moduleEffectScoreEngine == null) {
            moduleEffectScoreEngine =
                    createOptionalEngine("com.ethk3.novaonepro.ModuleEffectScoreEngine");
        }
        Object report =
                invokeReportEngine(
                        moduleEffectScoreEngine,
                        new String[] {
                            "score", "buildReport", "buildModuleEffectReport", "analyze", "refresh"
                        },
                        diagnosisReport,
                        null,
                        null,
                        null);
        saveDiagnosisObjectToStore(
                new String[] {"saveModuleEffectCache", "saveModuleEffectReport"}, report);
        saveEngineStateIfSupported(
                moduleEffectScoreEngine,
                report,
                new String[] {"saveModuleEffectCache", "saveReport", "saveState"});
        return report;
    }

    private Object refreshShadowPromotionForLoop(Object diagnosisReport, Object moduleReport) {
        if (shadowPromotionVerifierEngine == null) {
            shadowPromotionVerifierEngine = new ShadowPromotionVerifierEngine();
        }
        Object report =
                invokeReportEngine(
                        shadowPromotionVerifierEngine,
                        new String[] {
                            "verify",
                            "buildReport",
                            "buildShadowPromotionReport",
                            "analyze",
                            "refresh"
                        },
                        diagnosisReport,
                        moduleReport,
                        null,
                        null);
        saveDiagnosisObjectToStore(
                new String[] {"saveShadowPromotionCache", "saveShadowPromotionReport"}, report);
        saveEngineStateIfSupported(
                shadowPromotionVerifierEngine,
                report,
                new String[] {"saveShadowPromotionCache", "saveReport", "saveState"});
        return report;
    }

    private Object refreshFreezeMatrixForLoop(
            Object diagnosisReport, Object moduleReport, Object shadowReport) {
        if (dynamicFreezeMatrixEngine == null) {
            dynamicFreezeMatrixEngine =
                    createOptionalEngine("com.ethk3.novaonepro.DynamicFreezeMatrixEngine");
        }
        Object report =
                invokeReportEngine(
                        dynamicFreezeMatrixEngine,
                        new String[] {
                            "buildFreezeReport",
                            "buildReport",
                            "refresh",
                            "analyze",
                            "updateFreezeMatrix"
                        },
                        diagnosisReport,
                        moduleReport,
                        shadowReport,
                        null);
        saveDiagnosisObjectToStore(
                new String[] {"saveDynamicFreezeState", "saveFreezeReport"}, report);
        saveEngineStateIfSupported(
                dynamicFreezeMatrixEngine,
                report,
                new String[] {"saveFreezeState", "saveReport", "saveState"});
        return report;
    }

    private Object refreshAdaptiveJudgeForLoop(
            Object moduleReport, Object shadowReport, Object freezeReport) {
        if (adaptiveLearningJudgeEngine == null) {
            adaptiveLearningJudgeEngine = new AdaptiveLearningJudgeEngine();
        }
        Object report =
                invokeReportEngine(
                        adaptiveLearningJudgeEngine,
                        new String[] {
                            "buildJudgeReport", "judgeReport", "buildReport", "refresh", "analyze"
                        },
                        moduleReport,
                        shadowReport,
                        freezeReport,
                        null);
        saveDiagnosisObjectToStore(
                new String[] {"saveAdaptiveJudgeLog", "saveAdaptiveJudgeReport"}, report);
        saveEngineStateIfSupported(
                adaptiveLearningJudgeEngine,
                report,
                new String[] {"saveJudgeState", "saveReport", "saveState"});
        return report;
    }

    private Object refreshDiagnosisFixPlanForLoop(
            Object diagnosisReport,
            Object moduleReport,
            Object shadowReport,
            Object freezeReport,
            Object judgeReport) {
        if (diagnosisFixPlanEngine == null) {
            diagnosisFixPlanEngine =
                    createOptionalEngine("com.ethk3.novaonepro.DiagnosisFixPlanEngine");
        }
        Object report =
                invokeReportEngine(
                        diagnosisFixPlanEngine,
                        new String[] {"buildFixPlan", "plan", "buildReport", "refresh", "analyze"},
                        diagnosisReport,
                        moduleReport,
                        shadowReport,
                        freezeReport);
        if (report == null || report == REFLECTION_NO_METHOD) {
            report =
                    invokeReportEngine(
                            diagnosisFixPlanEngine,
                            new String[] {"buildFixPlan", "plan", "buildReport"},
                            judgeReport,
                            moduleReport,
                            shadowReport,
                            freezeReport);
        }
        saveDiagnosisObjectToStore(
                new String[] {"saveDiagnosisFixPlan", "saveFixPlanReport"}, report);
        saveEngineStateIfSupported(
                diagnosisFixPlanEngine,
                report,
                new String[] {"saveFixPlan", "saveReport", "saveState"});
        return report;
    }

    private Object invokeReportEngine(
            Object engine, String[] methodNames, Object a, Object b, Object c, Object d) {
        if (engine == null || methodNames == null || store == null) {
            return null;
        }
        for (int i = 0; i < methodNames.length; i++) {
            String name = methodNames[i];
            Object r;
            r = invokeOptional(engine, name, new Class[] {DbStore.class}, new Object[] {store});
            if (isValidReportResult(r)) return r;
            if (a != null) {
                r =
                        invokeOptional(
                                engine,
                                name,
                                new Class[] {DbStore.class, a.getClass()},
                                new Object[] {store, a});
                if (isValidReportResult(r)) return r;
                r =
                        invokeOptional(
                                engine,
                                name,
                                new Class[] {a.getClass(), DbStore.class},
                                new Object[] {a, store});
                if (isValidReportResult(r)) return r;
            }
            if (a != null && b != null) {
                r =
                        invokeOptional(
                                engine,
                                name,
                                new Class[] {DbStore.class, a.getClass(), b.getClass()},
                                new Object[] {store, a, b});
                if (isValidReportResult(r)) return r;
            }
            if (a != null && b != null && c != null) {
                r =
                        invokeOptional(
                                engine,
                                name,
                                new Class[] {
                                    DbStore.class, a.getClass(), b.getClass(), c.getClass()
                                },
                                new Object[] {store, a, b, c});
                if (isValidReportResult(r)) return r;
            }
            if (a != null && b != null && c != null && d != null) {
                r =
                        invokeOptional(
                                engine,
                                name,
                                new Class[] {
                                    DbStore.class,
                                    a.getClass(),
                                    b.getClass(),
                                    c.getClass(),
                                    d.getClass()
                                },
                                new Object[] {store, a, b, c, d});
                if (isValidReportResult(r)) return r;
            }
        }
        return null;
    }

    private boolean isValidReportResult(Object r) {
        return r != null && r != REFLECTION_NO_METHOD;
    }

    private void saveDiagnosisObjectToStore(String[] methodNames, Object report) {
        if (store == null
                || report == null
                || report == REFLECTION_NO_METHOD
                || methodNames == null) {
            return;
        }
        for (int i = 0; i < methodNames.length; i++) {
            String name = methodNames[i];
            if (name == null || name.length() == 0) {
                continue;
            }
            try {
                Object r =
                        invokeOptional(
                                store,
                                name,
                                new Class[] {report.getClass()},
                                new Object[] {report});
                if (r != REFLECTION_NO_METHOD) {
                    return;
                }
            } catch (Throwable e) {
            }
            try {
                Object r =
                        invokeOptional(
                                store, name, new Class[] {Object.class}, new Object[] {report});
                if (r != REFLECTION_NO_METHOD) {
                    return;
                }
            } catch (Throwable e) {
            }
        }
    }

    private void saveEngineStateIfSupported(Object engine, Object report, String[] methodNames) {
        if (engine == null || store == null || methodNames == null) {
            return;
        }
        for (int i = 0; i < methodNames.length; i++) {
            String name = methodNames[i];
            if (name == null || name.length() == 0) {
                continue;
            }
            Object r = REFLECTION_NO_METHOD;
            if (report != null && report != REFLECTION_NO_METHOD) {
                r =
                        invokeOptional(
                                engine,
                                name,
                                new Class[] {DbStore.class, report.getClass()},
                                new Object[] {store, report});
                if (r != REFLECTION_NO_METHOD) {
                    return;
                }
            }
            r = invokeOptional(engine, name, new Class[] {DbStore.class}, new Object[] {store});
            if (r != REFLECTION_NO_METHOD) {
                return;
            }
        }
    }

    private String safeInvokeStoreString(String methodName) {
        if (store == null || methodName == null || methodName.length() == 0) {
            return "";
        }
        try {
            Object r = invokeOptional(store, methodName, new Class[] {}, new Object[] {});
            if (r != null && r != REFLECTION_NO_METHOD) {
                return String.valueOf(r);
            }
        } catch (Throwable e) {
        }
        return "";
    }

    private String diagnosisObjectSummary(Object obj, String fallback) {
        if (obj == null || obj == REFLECTION_NO_METHOD) {
            return fallback == null ? "" : fallback;
        }
        String s = readString(obj, "summary", "");
        if (s.length() == 0) s = readString(obj, "detail", "");
        if (s.length() == 0) s = readString(obj, "status", "");
        if (s.length() == 0) s = readString(obj, "effectStatus", "");
        if (s.length() == 0) s = readString(obj, "promotionStatus", "");
        if (s.length() == 0) s = readString(obj, "freezeLevel", "");
        if (s.length() == 0) s = String.valueOf(obj);
        return limitText(s, 180);
    }

    private void appendDiagStep(StringBuilder sb, String title, String text) {
        if (sb == null || title == null || title.length() == 0) {
            return;
        }
        if (text == null || text.length() == 0) {
            text = "OK";
        }
        if (sb.length() > 0) {
            sb.append("｜");
        }
        sb.append(title).append("=").append(limitText(text, 120));
    }

    private void setFullDiagnosisLoopState(String status, String summary) {
        if (store == null) {
            return;
        }
        try {
            store.setState("diagnosis_closed_loop_worker_status", status == null ? "" : status);
            store.setState("diagnosis_closed_loop_worker_summary", summary == null ? "" : summary);
            store.setState(
                    "diagnosis_closed_loop_worker_updated_at",
                    String.valueOf(System.currentTimeMillis()));
            store.setState("diagnosis_closed_loop_patch_version", V20_FULL_DIAG_LOOP_PATCH_VERSION);
        } catch (Throwable e) {
        }
    }

    private void setLearningState(String status, String summary) {
        if (store == null) {
            return;
        }

        try {
            store.setState("auto_learn_status", status == null ? "" : status);
            store.setState("auto_learn_running", "RUNNING".equals(status) ? "1" : "0");
            store.setState("auto_learn_summary", summary == null ? "" : summary);
            store.setState("auto_learn_updated_at", String.valueOf(System.currentTimeMillis()));
        } catch (Throwable e) {
        }
    }

    private Models.Draw getAuthoritativeLatest(Models.Draw fallback) {
        if (store == null) return null;
        final Models.Draw durable = store.getLatestDraw();
        // A provider response can be confirmed in provenance while retention still defers draws.
        // Only the committed draw has immutable first_seen_at and may advance business consumers.
        if (fallback != null && fallback.isValid()
                && (durable == null || isDrawNewer(fallback, durable))) {
            try {
                java.util.LinkedHashMap<String,String> pending = new java.util.LinkedHashMap<String,String>();
                pending.put("r48_draw_ingest_status", "PROVIDER_CONFIRMED_WAIT_DURABLE_DRAW");
                pending.put("r48_draw_ingest_pending_issue", fallback.issue);
                pending.put("r48_draw_ingest_durable_issue", durable == null ? "" : durable.issue);
                store.setStates(pending);
            } catch (Throwable diagnosticFailure) {
                recordCriticalCatchR926("DRAW_PENDING_DURABLE_STATE", fallback.issue, diagnosticFailure);
            }
            postAutonomousResearchTickR908("R48_DRAW_RETENTION_HEADROOM_RECOVERY");
            return durable;
        }
        return durable;
    }

    private boolean isDrawNewer(Models.Draw a, Models.Draw b) {
        if (a == null) {
            return false;
        }

        if (b == null) {
            return true;
        }

        if (dataGuard != null) {
            if (dataGuard.isIssueAfter(a.issue, b.issue)) {
                return true;
            }

            if (dataGuard.isIssueAfter(b.issue, a.issue)) {
                return false;
            }
        }

        return a.openTime > b.openTime;
    }

    private void setAnalysisState(String status, int progress, String summary) {
        if (store == null) return;
        long now = System.currentTimeMillis();
        String s = status == null ? "" : status;
        String m = summary == null ? "" : summary;
        boolean force =
                "OK".equals(s)
                        || "ERROR".equals(s)
                        || "SKIP".equals(s)
                        || "WAITING".equals(s)
                        || progress >= 100
                        || progress <= 0;
        String key = s + "|" + progress + "|" + m;
        if (!force && "RUNNING".equals(s)) {
            if (sameText(lastAnalysisStateKey, key)
                    && lastAnalysisStateWriteAt > 0L
                    && now - lastAnalysisStateWriteAt < ANALYSIS_STATE_MIN_WRITE_MS) return;
            if (lastAnalysisStateWriteAt > 0L
                    && now - lastAnalysisStateWriteAt < ANALYSIS_STATE_MIN_WRITE_MS
                    && progress < 80) return;
        }
        lastAnalysisStateKey = key;
        lastAnalysisStateWriteAt = now;
        try {
            java.util.LinkedHashMap<String,String> patch=new java.util.LinkedHashMap<String,String>();
            patch.put("analysis_status",s);patch.put("analysis_progress",String.valueOf(progress));patch.put("analysis_summary",m);patch.put("analysis_updated_at",String.valueOf(now));
            AnalysisSingleFlightR36.Lease owner=AnalysisSingleFlightR36.shared().currentLease();
            patch.put("analysis_execution_target",owner==null?"":owner.targetIssue);
            patch.put("analysis_execution_stage",owner==null?"":owner.token.stage());
            patch.put("realtime_analysis_busy_v20",owner!=null&&!owner.token.isCanonicalCommitted()?"1":"0");
            LiveTelemetryProjectionR81.tryWrite(store, patch);
        } catch (Throwable ignore) {
        }
        try {
            heartbeatStateV8().markAnalysisProgress(s, progress, m, analysisCurrentTargetIssue);
        } catch (Throwable ignore) {
        }
        broadcastSimpleStateLimited(force);
    }

    private void setBacktestState(
            String status, String mode, int progress, int window, String summary, String error) {
        if (store == null) {
            return;
        }

        try {
            store.setState("backtest_status", status == null ? "" : status);
            store.setState("backtest_mode", mode == null ? "" : mode);
            store.setState("backtest_progress", String.valueOf(progress));
            store.setState("backtest_window", String.valueOf(window));
            store.setState("backtest_last_summary", summary == null ? "" : summary);
            store.setState("backtest_error", error == null ? "" : error);
            store.setState("backtest_updated_at", String.valueOf(System.currentTimeMillis()));

            if ("RUNNING".equals(status)) {
                String old = store.getState("backtest_started_at", "");
                if (old == null || old.length() == 0) {
                    store.setState(
                            "backtest_started_at", String.valueOf(System.currentTimeMillis()));
                }
            }

            if ("OK".equals(status) || "ERROR".equals(status) || "SKIP".equals(status)) {
                store.setState("backtest_finished_at", String.valueOf(System.currentTimeMillis()));
            }
        } catch (Throwable e) {
        }
    }

    private String buildDecisionBrief(Models.Decision d) {
        if (d == null) {
            return "暂无决策";
        }

        repairDecisionK3(d);
        syncTeacherAliasFields(d);

        StringBuilder sb = new StringBuilder();
        sb.append(d.decision == null ? "" : d.decision);

        if (d.position > 0 && Models.isValidK3Mask(d.k3Mask)) {
            sb.append(" ");
            sb.append(UiFormatter.positionAndNumbers(d.position, d.n1, d.n2, d.n3));
        }

        sb.append(" FinalP=");
        sb.append(UiFormatter.probability(d.finalP));
        sb.append(" Gate=");
        sb.append(UiFormatter.probability(d.dynamicGate));
        sb.append(" Edge=");
        sb.append(UiFormatter.signedProbability(d.edge));

        int tmSample = readTeacherSample(d);
        double tmHit = readTeacherHitRate(d);
        String tmStatus = readTeacherStatus(d);

        if (tmSample > 0 || tmStatus.length() > 0) {
            sb.append(" TM=");
            sb.append(tmStatus);
            sb.append("/");
            sb.append(tmSample);
            sb.append("/");
            sb.append(UiFormatter.probability(tmHit));
        }

        double causeScore =
                maxDoubleFields(
                        d,
                        new String[] {
                            "dynamicCauseScore", "causeChainScore", "causeScore", "finalCauseScore"
                        });
        if (causeScore > 0.0d) {
            sb.append(" Cause=");
            sb.append(UiFormatter.score(causeScore));
        }

        if (d.blockCode != null && d.blockCode.length() > 0) {
            sb.append(" Block=");
            sb.append(UiFormatter.blockReasonText(d.blockCode));
        }

        if (isWeakPlaceholderDecision(d)) {
            sb.append(" Weak=YES");
        }

        return sb.toString();
    }

    private String buildBacktestSummary(Models.BacktestReport r) {
        if (r == null) {
            return "";
        }

        StringBuilder sb = new StringBuilder();

        sb.append("WalkForward");

        if (r.window > 0) {
            sb.append(" W");
            sb.append(r.window);
        }

        sb.append("：出手=");
        sb.append(r.betCount);
        sb.append("/");
        sb.append(r.totalCount);
        sb.append("，频率=");
        sb.append(UiFormatter.probability(r.frequency));
        sb.append("，胜率=");
        sb.append(UiFormatter.probability(r.winRate > 0.0d ? r.winRate : r.hitRate));
        sb.append("，盈利=");
        sb.append(UiFormatter.money(r.pnl));
        sb.append("，最大回撤=");
        sb.append(UiFormatter.money(r.maxDrawdown));

        return sb.toString();
    }

    private void updateRuntimeState(String status, String error) {
        lastStatus = status == null ? AppConfig.SERVICE_STATUS_RUNNING : status;

        if (error != null) {
            lastError = error;
        }

        long now = System.currentTimeMillis();
        String stateKey =
                String.valueOf(lastStatus)
                        + "|"
                        + String.valueOf(lastError)
                        + "|"
                        + fetchBusy
                        + "|"
                        + analysisBusy
                        + "|"
                        + backfillBusy
                        + "|"
                        + backtestBusy
                        + "|"
                        + learningBusy
                        + "|"
                        + teacherMatrixBusy
                        + "|"
                        + v20CauseChainBusy;

        boolean important =
                AppConfig.SERVICE_STATUS_API_ERROR.equals(lastStatus)
                        || AppConfig.SERVICE_STATUS_DATA_ERROR.equals(lastStatus)
                        || AppConfig.SERVICE_STATUS_SETTLING.equals(lastStatus)
                        || AppConfig.SERVICE_STATUS_ANALYZING.equals(lastStatus)
                        || AppConfig.SERVICE_STATUS_STOPPED.equals(lastStatus);

        if (!important
                && sameText(lastRuntimeStateKey, stateKey)
                && lastRuntimeStateWriteAt > 0L
                && now - lastRuntimeStateWriteAt < RUNTIME_WRITE_MIN_INTERVAL_MS) {
            return;
        }

        lastRuntimeStateKey = stateKey;
        lastRuntimeStateWriteAt = now;

        try {
            Models.AppRuntimeState state = new Models.AppRuntimeState();
            state.serviceStatus = lastStatus;
            state.lastError = lastError == null ? "" : lastError;
            state.updatedAt = System.currentTimeMillis();

            Models.Draw latest = store == null ? null : store.getLatestDraw();

            if (latest != null) {
                state.currentIssue = latest.issue == null ? "" : latest.issue;
                state.targetIssue = dataGuard.buildNextIssue(state.currentIssue);
            }

            if (countdownClock != null) {
                state.countdownMs = countdownClock.getCountdownMs();
                state.nextOpenTime = countdownClock.getNextOpenTime();

                if (state.currentIssue == null || state.currentIssue.length() == 0) {
                    state.currentIssue = countdownClock.getCurrentIssue();
                }

                if (state.targetIssue == null || state.targetIssue.length() == 0) {
                    state.targetIssue = countdownClock.getTargetIssue();
                }

                state.schedulerSummary = countdownClock.buildShortStatusText();
            }

            String channelText = buildChannelStatusText();
            if (channelText.length() > 0) {
                state.schedulerSummary = appendReason(state.schedulerSummary, channelText);
            }

            state.apiLine = store == null ? "" : store.getState("api_line", "");

            Models.Decision latestDecision = getCurrentTargetDecision();

            if (latestDecision != null && latestDecision.decision != null) {
                state.lastDecision = latestDecision.decision;
            }

            if (store != null) {
                state.backtestStatus = store.getState("backtest_status", "");
                state.backtestMode = store.getState("backtest_mode", "");
                state.backtestRunning = backtestBusy ? 1 : 0;
                state.backtestProgress = parseInt(store.getState("backtest_progress", "0"), 0);
                state.backtestSummary = store.getState("backtest_last_summary", "");

                Models.ManualBacktestTask manual = store.getLatestManualBacktestTask();
                if (manual != null) {
                    state.manualBacktestStatus = manual.status;
                    state.manualBacktestProgress = manual.progress;
                    state.manualBacktestRunning = "RUNNING".equals(manual.status) ? 1 : 0;
                    state.manualBacktestSummary = manual.summary;
                }

                // R71: fetch/status publication must never run full-table statistics. The
                // diagnostics owner and UI history lane compute those independently.
                state.learningSummary = store.getState("learning_summary", "");
                state.biasSummary = store.getState("bias_summary", "");
                state.auditSummary = store.getState("audit_summary", "");
                state.aiJudgeSummary = store.getState("ai_judge_summary", "");
                state.selfDiagnosisSummary = store.getState("self_diagnosis_summary", "");
                state.promotionSummary = store.getState("promotion_summary", "");
                state.aPlusSummary = store.getState("a_plus_summary", "");
                state.lastSnapshotId = store.getState("last_snapshot_id", "");

                if (lastLearningSummary != null && lastLearningSummary.length() > 0) {
                    state.learningSummary =
                            appendReason(state.learningSummary, lastLearningSummary);
                }

                if (lastTeacherMatrixSummary != null && lastTeacherMatrixSummary.length() > 0) {
                    state.learningSummary =
                            appendReason(state.learningSummary, "TM：" + lastTeacherMatrixSummary);
                }

                if (lastV20CauseChainSummary != null && lastV20CauseChainSummary.length() > 0) {
                    state.learningSummary =
                            appendReason(
                                    state.learningSummary, "V20因链：" + lastV20CauseChainSummary);
                }

                try {
                    store.updateRuntimeState(state);
                } catch (RuntimeException unavailableProjection) {
                    // The coordinator records the failed atomic projection. Its failure must
                    // not suppress this already-observed in-memory state/clock broadcast.
                }
            }

            broadcastState(state);
        } catch (Throwable e) {
        }
    }

    private Models.Decision getCurrentTargetDecision() {
        if (store == null || dataGuard == null) {
            return null;
        }

        try {
            Models.Draw latest = store.getLatestDraw();

            if (latest != null && latest.isValid()) {
                String baseIssue = latest.issue == null ? "" : latest.issue;
                String targetIssue = dataGuard.buildNextIssue(baseIssue);

                Models.Decision d = store.getDecisionByTargetIssue(targetIssue);
                if (d != null) {
                    return d;
                }
            }

            return store.getLatestDecision();
        } catch (Throwable e) {
            return null;
        }
    }

    private String buildChannelStatusText() {
        StringBuilder sb = new StringBuilder();

        sb.append("通道：抓取=");
        sb.append(fetchBusy ? "运行中" : "空闲");
        sb.append("，分析=");
        sb.append(analysisBusy ? "运行中" : "空闲");
        sb.append("，回补=");
        sb.append(backfillBusy ? "运行中" : "空闲");
        sb.append("，回测=");
        sb.append(backtestBusy ? "运行中" : "空闲");
        sb.append("，学习=");
        sb.append(learningBusy ? "运行中" : "空闲");
        sb.append("，矩阵=");
        sb.append(teacherMatrixBusy ? "运行中" : "空闲");
        sb.append("，V20因链=");
        sb.append(v20CauseChainBusy ? "运行中" : "空闲");

        if (analysisBusy
                && analysisCurrentTargetIssue != null
                && analysisCurrentTargetIssue.length() > 0) {
            sb.append("，分析目标=");
            sb.append(analysisCurrentTargetIssue);
        }

        if (backtestBusy && backtestHeartbeatWindow > 0) {
            sb.append(" W");
            sb.append(backtestHeartbeatWindow);
        }

        if (backtestYieldRequested) {
            sb.append("，回测让路=");
            sb.append(backtestYieldReason == null ? "实时分析优先" : backtestYieldReason);
        }

        if (lastTeacherMatrixSummary != null && lastTeacherMatrixSummary.length() > 0) {
            sb.append("，TM=");
            sb.append(lastTeacherMatrixSummary);
        }

        if (lastV20CauseChainSummary != null && lastV20CauseChainSummary.length() > 0) {
            sb.append("，V20=");
            sb.append(lastV20CauseChainSummary);
        }

        if (lastLearningSummary != null && lastLearningSummary.length() > 0) {
            sb.append("，学习摘要=");
            sb.append(lastLearningSummary);
        }

        String cost = "";
        try {
            if (store != null) {
                cost = store.getState("analysis_cost_summary", "");
            }
        } catch (Throwable e) {
            cost = "";
        }

        if (cost != null && cost.length() > 0) {
            sb.append("，耗时=");
            sb.append(cost);
        }

        String text = sb.toString();
        if (text.length() > 260) {
            text = text.substring(0, 260);
        }

        return text;
    }

    private void broadcastTick(long ignoredQueuedCountdownMs) {
        final CountdownClock clock = countdownClock;
        if (clock == null || !running) return;
        final long sampledAt;
        final long countdown;
        final String base, target;
        final CountdownProjectionR971.Anchor anchor;
        synchronized (clock) {
            sampledAt = android.os.SystemClock.elapsedRealtime();
            countdown = clock.getCountdownMs();
            base = clock.getCurrentIssue();
            target = clock.getTargetIssue();
            anchor=clock.clockAnchorCP12();
        }
        // The limiter and sample timestamps share a monotonic time base. Wall-clock corrections
        // cannot stop ticks or turn an old queued countdown into a fresh, extended deadline.
        if (target.equals(lastTickTargetR72) && lastTickBroadcastAt > 0L && sampledAt >= lastTickBroadcastAt
                && sampledAt - lastTickBroadcastAt < TICK_BROADCAST_MIN_INTERVAL_MS) return;
        lastTickBroadcastAt = sampledAt;
        lastTickTargetR72=target;
        try {
            Intent intent = new Intent(AppConfig.ACTION_SERVICE_TICK);
            intent.setPackage(getPackageName());
            intent.putExtra(AppConfig.EXTRA_COUNTDOWN_MS, countdown);
            intent.putExtra("r971_tick_elapsed", sampledAt);
            if(anchor!=null&&target.equals(anchor.targetIssue)){
                intent.putExtra("clock_session_cp12",anchor.session);
                intent.putExtra("clock_revision_cp12",anchor.revision);
                intent.putExtra("clock_deadline_wall_cp12",anchor.deadlineWallMs);
                intent.putExtra("clock_deadline_mono_cp12",anchor.deadlineMonoMs);
                intent.putExtra("clock_anchor_sampled_cp12",anchor.sampledMonoMs);
            }
            intent.putExtra(AppConfig.EXTRA_CURRENT_ISSUE, base);
            intent.putExtra(AppConfig.EXTRA_TARGET_ISSUE, target);
            sendBroadcast(intent);
        } catch (RuntimeException failure) {
            android.util.Log.e("AutoFetchService", "R973_CLOCK_BROADCAST_FAILED", failure);
        }
    }

    private void broadcastDecision(final Models.Decision decision) {
        try {
            if (decision != null) {
                syncTeacherAliasFields(decision);
            }

            Intent intent = new Intent(AppConfig.ACTION_DECISION_UPDATED);
            intent.setPackage(getPackageName());

            intent.putExtra(AppConfig.EXTRA_DECISION, decision == null ? "" : decision.decision);
            intent.putExtra(
                    AppConfig.EXTRA_CURRENT_ISSUE, decision == null ? "" : decision.baseIssue);
            intent.putExtra(
                    AppConfig.EXTRA_TARGET_ISSUE, decision == null ? "" : decision.targetIssue);

            if (decision != null) {
                intent.putExtra("position", decision.position);
                intent.putExtra("n1", decision.n1);
                intent.putExtra("n2", decision.n2);
                intent.putExtra("n3", decision.n3);
                intent.putExtra("k3_mask", decision.k3Mask);
                intent.putExtra("final_p", decision.finalP);
                intent.putExtra("raw_p", decision.rawP);
                intent.putExtra("model_p", decision.modelP);
                intent.putExtra("dynamic_gate", decision.dynamicGate);
                intent.putExtra("edge", decision.edge);
                intent.putExtra("block_code", decision.blockCode == null ? "" : decision.blockCode);
                intent.putExtra(
                        "reason_summary",
                        decision.reasonSummary == null ? "" : decision.reasonSummary);
                intent.putExtra("decision_brief", buildDecisionBrief(decision));
                intent.putExtra("teacher_matrix_status", readTeacherStatus(decision));
                intent.putExtra("teacher_matrix_sample", readTeacherSample(decision));
                intent.putExtra("teacher_matrix_hit_rate", readTeacherHitRate(decision));
                intent.putExtra("teacher_matrix_summary", readTeacherSummary(decision));
            }

            sendBroadcast(intent);
        } catch (Throwable e) {
        }

        if (running && runtimeGuardHandler != null) {
            runtimeGuardHandler.removeCallbacks(decisionStateRefreshR77);
            runtimeGuardHandler.postDelayed(decisionStateRefreshR77, DECISION_FAST_STATE_DELAY_MS);
        }
    }

    private void broadcastSettlementUpdated() {
        try {
            Intent intent = new Intent(AppConfig.ACTION_STATE_CHANGED);
            intent.setPackage(getPackageName());
            intent.putExtra(AppConfig.EXTRA_STATUS, AppConfig.SERVICE_STATUS_SETTLING);
            intent.putExtra(
                    AppConfig.EXTRA_COUNTDOWN_MS,
                    countdownClock == null ? 0L : countdownClock.getCountdownMs());

            if (store != null) {
                Models.Draw latest = store.getLatestDraw();
                if (latest != null && latest.isValid()) {
                    String issue = latest.issue == null ? "" : latest.issue;
                    intent.putExtra(AppConfig.EXTRA_CURRENT_ISSUE, issue);
                    if (dataGuard != null) {
                        intent.putExtra(
                                AppConfig.EXTRA_TARGET_ISSUE, dataGuard.buildNextIssue(issue));
                    }
                }
            }

            sendBroadcast(intent);
        } catch (Throwable e) {
        }

        broadcastSimpleStateLimited(true);
        refreshNotificationNow();
    }

    private void broadcastDrawsUpdated(boolean force) {
        try {
            Intent intent = new Intent(AppConfig.ACTION_STATE_CHANGED);
            intent.setPackage(getPackageName());

            if (store != null) {
                Models.Draw latest = store.getLatestDraw();
                if (latest != null && latest.isValid()) {
                    String issue = latest.issue == null ? "" : latest.issue;
                    intent.putExtra(AppConfig.EXTRA_CURRENT_ISSUE, issue);
                    if (dataGuard != null) {
                        intent.putExtra(
                                AppConfig.EXTRA_TARGET_ISSUE, dataGuard.buildNextIssue(issue));
                    }
                }
            }

            intent.putExtra(AppConfig.EXTRA_STATUS, lastStatus == null ? "" : lastStatus);
            intent.putExtra(
                    AppConfig.EXTRA_COUNTDOWN_MS,
                    countdownClock == null ? 0L : countdownClock.getCountdownMs());
            sendBroadcast(intent);
        } catch (Throwable e) {
        }

        broadcastSimpleStateLimited(force);
    }

    private void broadcastTeacherMatrixUpdated() {
        try {
            Intent intent = new Intent(AppConfig.PACKAGE_NAME + ".ACTION_TEACHER_MATRIX_UPDATED");
            intent.setPackage(getPackageName());
            intent.putExtra(
                    "teacher_matrix_summary",
                    lastTeacherMatrixSummary == null ? "" : lastTeacherMatrixSummary);
            sendBroadcast(intent);
        } catch (Throwable e) {
        }

        broadcastSimpleStateLimited(true);
    }

    private void broadcastV20CauseChainUpdated() {
        try {
            Intent intent = new Intent(AppConfig.PACKAGE_NAME + ".ACTION_V20_CAUSE_CHAIN_UPDATED");
            intent.setPackage(getPackageName());
            intent.putExtra(
                    "v20_cause_chain_summary",
                    lastV20CauseChainSummary == null ? "" : lastV20CauseChainSummary);
            sendBroadcast(intent);
        } catch (Throwable e) {
        }

        broadcastSimpleStateLimited(true);
    }

    private void broadcastSimpleState() {
        broadcastSimpleStateLimited(false);
    }

    private void broadcastSimpleStateLimited(boolean force) {
        long now = System.currentTimeMillis();

        if (!force
                && lastSimpleStateBroadcastAt > 0L
                && now - lastSimpleStateBroadcastAt < STATE_BROADCAST_MIN_INTERVAL_MS) {
            return;
        }

        lastSimpleStateBroadcastAt = now;

        try {
            Models.AppRuntimeState state = store == null ? null : store.getRuntimeState();
            broadcastState(state);
        } catch (Throwable e) {
        }
    }

    private void broadcastState(Models.AppRuntimeState state) {
        try {
            Intent intent = new Intent(AppConfig.ACTION_STATE_CHANGED);
            intent.setPackage(getPackageName());
            intent.putExtra(AppConfig.EXTRA_STATUS, state == null ? "" : state.serviceStatus);
            intent.putExtra(AppConfig.EXTRA_CURRENT_ISSUE, state == null ? "" : state.currentIssue);
            intent.putExtra(AppConfig.EXTRA_TARGET_ISSUE, state == null ? "" : state.targetIssue);
            intent.putExtra(AppConfig.EXTRA_COUNTDOWN_MS, state == null ? 0L : state.countdownMs);
            sendBroadcast(intent);
        } catch (Throwable e) {
        }
    }

    private void broadcastBacktestUpdated() {
        try {
            Intent intent = new Intent(AppConfig.ACTION_BACKTEST_UPDATED);
            intent.setPackage(getPackageName());
            intent.putExtra(
                    AppConfig.EXTRA_STATUS,
                    store == null ? "" : store.getState("backtest_status", ""));
            sendBroadcast(intent);
        } catch (Throwable e) {
        }

        broadcastSimpleStateLimited(false);
        refreshNotificationIfNeeded();
    }

    private boolean isNetworkAvailable() {
        try {
            ConnectivityManager cm =
                    (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

            if (cm == null) {
                return true;
            }

            NetworkInfo info = cm.getActiveNetworkInfo();
            return info != null && info.isConnected();
        } catch (Throwable e) {
            return true;
        }
    }

    // R81: no service-lifetime or heartbeat wake lease. Each current prediction
    // owns a bounded PredrawWakeLeaseR81 from PIT freeze to durable canonical commit.

    private void ensureRuntimeLoopsAliveV17(String reason) {
        if (!running || userRequestedStopV5 || !runtimeInitialized) return;
        repairDeadFetchWorkerR811(reason);
        repairDeadAnalysisWorkerR811(reason);
        repairR908BackgroundWorkers(reason);
        long now = System.currentTimeMillis();
        if (now - lastAlwaysOnLoopRepairAtV17 < AppConfig.ALWAYS_ON_LOOP_REPAIR_INTERVAL_MS) return;
        lastAlwaysOnLoopRepairAtV17 = now;
        try {
            ServiceHeartbeatStore hb = heartbeatStateV8();
            String stale = hb.getBusinessStaleReason();
            if (hb.isTeacherPumpStale()) {
                backgroundEvidencePumpPostedV6 = false;
                postBackgroundEvidencePumpV6(200L, "repair:" + reason);
            }
            if (hb.isLearningPumpStale()) {
                // R955: repair the actual autonomous Research owner.  The legacy learning
                // channel is feature-isolated in TianShu and cannot repair a stalled R908 loop.
                learningPostPending = false;
                postLearningTick("repair:" + reason, true);
                ensureR908BackgroundLoops();
                postAutonomousResearchTickR908("R955_RESEARCH_PROGRESS_REPAIR:" + safe(reason));
                if (store != null) {
                    BackgroundResearchProgressContractR955.Result rt = hb.researchProgressTruth();
                    store.setState(
                            "r955_research_progress_repair",
                            rt.canonical()
                                    + "|attempt=" + hb.getLearningAttemptState()
                                    + "|detail=" + hb.getLearningAttemptDetail()
                                    + "|reason=" + safe(reason));
                }
            }
            ensureAnalysisWatchdog();
            if (store != null) {
                B13RuntimeHealth health = b13Health(store);
                if (!health.fetchAlive && !fetchBusy) {
                    forceRefresh = true;
                    postFetchNow();
                    store.setState(
                            "r811_fetch_stale_repost",
                            safe(reason) + "|age=" + health.fetchAge + "|at=" + now);
                }
                if ((!health.analysisAlive || health.analysisFailed)
                        && !analysisBusy
                        && AnalysisSingleFlightR36.shared().currentLease() == null) {
                    postAnalysisForLatest("R811_STALE_ANALYSIS_RECONCILE:" + safe(reason), true);
                    store.setState(
                            "r811_analysis_stale_repost",
                            safe(reason) + "|age=" + health.analysisAge + "|at=" + now);
                }
                if (!health.settlementAlive && analysisWatchdogHandler != null) {
                    analysisWatchdogHandler.removeCallbacks(analysisWatchdogLoop);
                    analysisWatchdogHandler.post(analysisWatchdogLoop);
                    store.setState(
                            "r811_settlement_stale_repost",
                            safe(reason) + "|age=" + health.settlementAge + "|at=" + now);
                }
            }
            if (reason == null || reason.indexOf("heartbeat") < 0) postServiceHeartbeatV5();
            hb.markLoopRepair(reason + "|stale=" + stale);
            if (store != null) {
                hb.writeBusinessTruth(
                        RuntimeHealthLedgerR36.businessTruth(store),
                        teacherMatrixBusy || backgroundEvidencePumpPostedV6 ? "RUNNING" : "IDLE",
                        learningBusy || learningPostPending ? "RUNNING" : "IDLE");
            }
            if (store != null)
                store.setState("always_on_loop_repair", reason + "|" + now + "|" + stale);
        } catch (Throwable e) {
            try {
                if (store != null) store.setState("always_on_loop_repair_error", safeError(e));
            } catch (Throwable ignore) {
            }
        }
    }

    private void repairR908BackgroundWorkers(String reason) {
        boolean repaired = false;
        if (settlementRecoveryThreadR908 == null || !((Thread) settlementRecoveryThreadR908).isAlive()
                || settlementRecoveryHandlerR908 == null) {
            try { quitThread(settlementRecoveryThreadR908); } catch (Throwable ignore) {}
            settlementRecoveryThreadR908 = new HandlerThread(
                    "starcore_r908_settlement_recovery_repaired", Process.THREAD_PRIORITY_DEFAULT);
            settlementRecoveryThreadR908.start();
            settlementRecoveryHandlerR908 = new Handler(settlementRecoveryThreadR908.getLooper());
            settlementMaintenancePostedR817.set(false);
            repaired = true;
        }
        if (researchThreadR908 == null || !((Thread) researchThreadR908).isAlive()
                || researchHandlerR908 == null) {
            try { quitThread(researchThreadR908); } catch (Throwable ignore) {}
            researchThreadR908 = new HandlerThread(
                    "starcore_r908_autonomous_research_repaired", Process.THREAD_PRIORITY_BACKGROUND);
            researchThreadR908.start();
            researchHandlerR908 = new Handler(researchThreadR908.getLooper());
            researchTickPostedR908.set(false);
            repaired = true;
        }
        if (repaired) {
            ensureR908BackgroundLoops();
            try { if (store != null) store.setState("r908_background_worker_repair",
                    "RECOVERED|" + safe(reason) + '|' + System.currentTimeMillis()); }
            catch (Throwable ignore) {}
        }
    }

    /** Restores the sole fetch Looper only after its previous HandlerThread is physically dead. */
    private boolean repairDeadFetchWorkerR811(String reason) {
        HandlerThread observed = fetchThread;
        if (observed != null && ((Thread) observed).isAlive()) return false;
        synchronized (fetchWorkerRepairLockR811) {
            observed = fetchThread;
            if (observed != null && ((Thread) observed).isAlive()) return false;
            fetchBusy = false;
            fetchThread =
                    new HandlerThread(
                            "eth_k3_fetch_channel_r811_recovered",
                            Process.THREAD_PRIORITY_DEFAULT);
            fetchThread.start();
            fetchHandler = new Handler(fetchThread.getLooper());
        }
        long now = System.currentTimeMillis();
        forceRefresh = true;
        try {
            if (store != null) {
                store.setState(
                        "r811_fetch_worker_repair",
                        "RECOVERED_PHYSICALLY_DEAD|reason=" + safe(reason) + "|at=" + now);
                store.setState("r811_fetch_worker_repair_at", String.valueOf(now));
            }
            heartbeatStateV8().markBusinessStage("R811_FETCH_WORKER_RECOVERED", safe(reason));
        } catch (Throwable ignored) {
        }
        postFetchNow();
        return true;
    }

    /**
     * Recreates the sole analysis HandlerThread only after the old thread is physically dead and
     * its in-memory owner has been atomically reaped. A live/stuck worker is never replaced; it is
     * handled only by cooperative cancellation and the durable lease fence.
     */
    private boolean repairDeadAnalysisWorkerR811(String reason) {
        HandlerThread observed = analysisThread;
        if (observed != null && ((Thread) observed).isAlive() && analysisHandler != null)
            return false;
        AnalysisSingleFlightR36.ReapResult reaped;
        synchronized (analysisWorkerRepairLockR811) {
            observed = analysisThread;
            if (observed != null && ((Thread) observed).isAlive() && analysisHandler != null)
                return false;
            reaped =
                    AnalysisSingleFlightR36.shared()
                            .reapDeadWorker(System.currentTimeMillis(), true);
            if (AnalysisSingleFlightR36.shared().currentLease() != null && !reaped.reaped) {
                try {
                    if (store != null)
                        store.setState(
                                "r811_analysis_worker_repair",
                                "BLOCKED_ACTIVE_OWNER|" + reaped.status + '|' + safe(reason));
                } catch (Throwable ignored) {
                }
                return false;
            }
            analysisBusy = false;
            analysisStartedAt = 0L;
            analysisCurrentBaseIssue = "";
            analysisCurrentTargetIssue = "";
            analysisThread =
                    new HandlerThread(
                            "eth_k3_analysis_channel_r811_recovered",
                            Process.THREAD_PRIORITY_DEFAULT);
            analysisThread.start();
            analysisHandler = new Handler(analysisThread.getLooper());
        }
        long now = System.currentTimeMillis();
        if (reaped.released != null && store != null) {
            try {
                AnalysisRequestStoreR36.TransitionResult transition =
                        new AnalysisRequestStoreR36(store)
                                .markRetry(
                                        reaped.released.request,
                                        reaped.released.ownerId,
                                        "WORKER_THREAD_DIED_BEFORE_COMMIT",
                                        now);
                RuntimeHealthLedgerR36.record(
                        store,
                        "ANALYSIS_WORKER_RECOVERED",
                        reaped.released.request.attemptId(),
                        reaped.released.targetIssue,
                        "DEAD_WORKER_REAPED",
                        reaped.released.request.requestAt,
                        0L,
                        Math.max(0L, now - reaped.released.request.requestAt),
                        AnalysisSingleFlightR36.shared().activeCount(),
                        AnalysisSingleFlightR36.shared().maximumObserved(),
                        false,
                        "transition="
                                + transition.status
                                + ':'
                                + transition.code
                                + "|reason="
                                + safe(reason));
            } catch (Throwable error) {
                lastError = safeError(error);
            }
        }
        try {
            if (store != null) {
                store.setState(
                        "r811_analysis_worker_repair",
                        "RECOVERED|"
                                + reaped.status
                                + "|reason="
                                + safe(reason)
                                + "|at="
                                + now);
                store.setState("r811_analysis_worker_repair_at", String.valueOf(now));
            }
            heartbeatStateV8().markBusinessStage("R811_ANALYSIS_WORKER_RECOVERED", safe(reason));
        } catch (Throwable ignored) {
        }
        if (reaped.next != null) submitAnalysisRequestR36(reaped.next);
        else postAnalysisForLatest("R811_DEAD_WORKER_RECOVERY", true);
        ensureAnalysisWatchdog();
        return true;
    }

    private void refreshNotificationIfNeeded() {
        long now = System.currentTimeMillis();

        if (now - lastNotificationAt < NOTIFICATION_MIN_INTERVAL_MS) {
            return;
        }

        refreshNotificationNow();
    }

    private void refreshNotificationNow() {
        try {
            lastNotificationAt = System.currentTimeMillis();

            NotificationManager nm =
                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

            if (nm == null) {
                return;
            }

            Models.Decision latestDecision = getCurrentTargetDecision();
            String title = AppConfig.PROJECT_NAME;
            String text;

            if (latestDecision != null) {
                syncTeacherAliasFields(latestDecision);
                text = buildDecisionBrief(latestDecision);
            } else {
                text =
                        lastStatus
                                + " "
                                + (countdownClock == null ? "" : countdownClock.countdownText());
            }

            if (countdownClock != null) {
                text = text + "｜" + countdownClock.fetchModeText();
            }

            if (analysisBusy) {
                text = text + "｜正式分析中";
            }

            if (teacherMatrixBusy) {
                text = text + "｜矩阵后台学习中";
            }

            if (v20CauseChainBusy) {
                text = text + "｜V20因链学习中";
            }

            if (learningBusy) {
                text = text + "｜学习中";
            }

            if (store != null) {
                text = text + "｜" + store.getDrawCount() + "期";
            }

            if (backtestBusy && lastBacktestMode != null && lastBacktestMode.length() > 0) {
                text = text + "｜回测" + lastBacktestMode;

                if (backtestHeartbeatWindow > 0) {
                    text = text + " W" + backtestHeartbeatWindow;
                }
            }

            if (lastTeacherMatrixSummary != null && lastTeacherMatrixSummary.length() > 0) {
                text = text + "｜TM " + lastTeacherMatrixSummary;
            }

            if (lastV20CauseChainSummary != null && lastV20CauseChainSummary.length() > 0) {
                text = text + "｜V20 " + lastV20CauseChainSummary;
            }

            if (text != null && text.length() > 180) {
                text = text.substring(0, 180);
            }

            nm.notify(AppConfig.NOTIFICATION_ID, buildNotification(title, text));
        } catch (Throwable e) {
        }
    }

    private Notification buildNotification(String title, String text) {
        Intent openIntent = new Intent(this, MainActivity.class);
        openIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pi;

        if (Build.VERSION.SDK_INT >= 23) {
            pi =
                    PendingIntent.getActivity(
                            this,
                            1001,
                            openIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        } else {
            pi =
                    PendingIntent.getActivity(
                            this, 1001, openIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        }

        Notification.Builder builder;

        if (Build.VERSION.SDK_INT >= 26) {
            builder = new Notification.Builder(this, AppConfig.NOTIFICATION_CHANNEL_ID);
        } else {
            builder = new Notification.Builder(this);
        }

        builder.setContentTitle(title == null ? AppConfig.PROJECT_NAME : title);
        builder.setContentText(text == null ? "" : text);
        builder.setSmallIcon(R.drawable.ic_notification_nova);
        builder.setContentIntent(pi);
        builder.setOngoing(true);
        builder.setOnlyAlertOnce(true);
        builder.setShowWhen(false);

        if (Build.VERSION.SDK_INT >= 21) {
            builder.setCategory(Notification.CATEGORY_SERVICE);
            builder.setPriority(Notification.PRIORITY_LOW);
        }

        return builder.build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT < 26) {
            return;
        }

        try {
            NotificationManager nm =
                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

            if (nm == null) {
                return;
            }

            NotificationChannel channel =
                    new NotificationChannel(
                            AppConfig.NOTIFICATION_CHANNEL_ID,
                            AppConfig.NOTIFICATION_CHANNEL_NAME,
                            NotificationManager.IMPORTANCE_LOW);

            channel.setDescription(AppConfig.PROJECT_NAME);
            channel.setShowBadge(false);

            nm.createNotificationChannel(channel);
        } catch (Throwable e) {
        }
    }

    private void quitThread(HandlerThread thread) {
        try {
            if (thread == null) {
                return;
            }

            if (Build.VERSION.SDK_INT >= 18) {
                thread.quitSafely();
            } else {
                thread.quit();
            }
        } catch (Throwable e) {
        }
    }

    private boolean shouldPostRealtimeHistory100() {
        long now = System.currentTimeMillis();

        if (lastForcedBackfill100At > 0L
                && now - lastForcedBackfill100At < REALTIME_FORCE_BACKFILL_MIN_INTERVAL_MS) {
            return false;
        }

        boolean need = false;

        try {
            if (countdownClock != null && countdownClock.shouldForceHistory100(realtimeMissCount)) {
                need = true;
            }
        } catch (Throwable e) {
            need = false;
        }

        if (realtimeMissCount >= 1) {
            need = true;
        }

        if (need) {
            realtimeHistory100Posted = true;
            lastForcedBackfill100At = now;
        }

        return need;
    }

    private boolean isWeakPlaceholderDecision(Models.Decision d) {
        if (d == null) {
            return true;
        }

        String decision = d.decision == null ? "" : d.decision;

        if (AppConfig.DECISION_BET.equals(decision)) {
            return !isDecisionValidRealBet(d);
        }

        boolean isShadow = AppConfig.DECISION_SHADOW.equals(decision);

        String reason = "";

        if (d.reasonSummary != null) {
            reason += d.reasonSummary;
        }

        if (d.reasonText != null) {
            reason += " " + d.reasonText;
        }

        if (d.deepReason != null) {
            reason += " " + d.deepReason;
        }

        if (d.selfDiagnosisText != null) {
            reason += " " + d.selfDiagnosisText;
        }

        if (d.learningSummary != null) {
            reason += " " + d.learningSummary;
        }

        if (d.aiJudgeSummary != null) {
            reason += " " + d.aiJudgeSummary;
        }

        if (d.blockCode != null) {
            reason += " " + d.blockCode;
        }

        if ("QUICK_OBSERVE".equals(d.probabilityMode)
                || (d.shapeKey != null && d.shapeKey.indexOf("QUICK_OBSERVE") >= 0)
                || (d.snapshotId != null && d.snapshotId.indexOf("QUICK_") == 0)) {
            return true;
        }

        if (reason.indexOf("快速观察") >= 0
                || reason.indexOf("正式模型继续精算") >= 0
                || reason.indexOf("正式结果完成后会自动覆盖") >= 0
                || reason.indexOf("快速观察兜底") >= 0
                || reason.indexOf("只解决界面有预测") >= 0
                || reason.indexOf("快裁门") >= 0
                || reason.indexOf("实时快裁") >= 0) {
            return true;
        }

        if (reason.indexOf("临时") >= 0
                || reason.indexOf("占位") >= 0
                || reason.indexOf("正在生成") >= 0
                || reason.indexOf("未生成有效决策") >= 0
                || reason.indexOf("分析线程未生成") >= 0) {
            return true;
        }

        if (isShadow) {
            repairDecisionK3(d);

            if (AppConfig.isValidPosition(d.position) && Models.isValidK3Mask(d.k3Mask)) {
                return false;
            }

            return true;
        }

        if (!AppConfig.DECISION_HOLD.equals(decision)) {
            return false;
        }

        boolean emptyReason = reason.length() == 0;
        boolean emptyBlock = d.blockCode == null || d.blockCode.length() == 0;

        if (d.finalP <= 0.0001d
                && d.position <= 0
                && !Models.isValidK3Mask(d.k3Mask)
                && emptyReason
                && emptyBlock) {
            return true;
        }

        return false;
    }

    private void normalizeDecisionScope(Models.Decision d) {
        if (d == null) {
            return;
        }

        String oldResult = d.result == null ? "" : d.result;
        int oldSettled = d.settled;
        boolean alreadySettled = oldSettled == 1 || isFinalSettlementResultText(oldResult);

        if (d.decision == null || d.decision.length() == 0) {
            if (d.action != null && d.action.length() > 0) {
                d.decision = d.action;
            } else {
                d.decision = AppConfig.DECISION_HOLD;
            }
        }

        d.decision = normalizeDecisionText(d.decision);
        d.action = d.decision;

        if (AppConfig.DECISION_BET.equals(d.decision)) {
            d.statScope = AppConfig.STAT_SCOPE_REAL_TRADE;
        } else if (AppConfig.DECISION_SHADOW.equals(d.decision)) {
            d.statScope = AppConfig.STAT_SCOPE_SHADOW;
        } else {
            d.statScope = AppConfig.STAT_SCOPE_HOLD;
        }

        if (alreadySettled) {
            d.result = oldResult;
            d.settled = 1;
        } else {
            d.result = AppConfig.RESULT_WAIT;
            d.settled = 0;
        }

        try {
            d.syncAction();
        } catch (Throwable e) {
        }
    }

    private void normalizeDecisionScopePreserveSettlement(Models.Decision d) {
        normalizeDecisionScope(d);
    }

    private boolean isFinalSettlementResultText(String r) {
        if (r == null) {
            return false;
        }

        String x = r.trim();

        if (x.length() == 0) {
            return false;
        }

        if ("WIN".equalsIgnoreCase(x)
                || "MISS".equalsIgnoreCase(x)
                || "HIT".equalsIgnoreCase(x)
                || "LOSE".equalsIgnoreCase(x)
                || "命中".equals(x)
                || "未中".equals(x)) {
            return true;
        }

        return false;
    }

    private String normalizeDecisionText(String text) {
        if (text == null || text.length() == 0) {
            return AppConfig.DECISION_HOLD;
        }

        String x = text.trim().toUpperCase(java.util.Locale.ROOT);

        if ("BET".equals(x)
                || "REAL".equals(x)
                || "REAL_TRADE".equals(x)
                || "投注".equals(x)
                || "出手".equals(x)) {
            return AppConfig.DECISION_BET;
        }

        if ("SHADOW".equals(x)
                || "OBSERVE".equals(x)
                || "WATCH".equals(x)
                || "影子".equals(x)
                || "观察".equals(x)) {
            return AppConfig.DECISION_SHADOW;
        }

        return AppConfig.DECISION_HOLD;
    }

    private void repairDecisionK3(Models.Decision d) {
        if (d == null) {
            return;
        }

        if (!Models.isValidK3Mask(d.k3Mask)
                && AppConfig.isValidNumber(d.n1)
                && AppConfig.isValidNumber(d.n2)
                && AppConfig.isValidNumber(d.n3)) {
            try {
                d.k3Mask = Models.buildK3Mask(d.n1, d.n2, d.n3);
            } catch (Throwable e) {
                d.k3Mask = buildK3MaskLocal(d.n1, d.n2, d.n3);
            }
        }
    }

    private int buildK3MaskLocal(int n1, int n2, int n3) {
        int mask = 0;

        if (AppConfig.isValidNumber(n1)) {
            mask |= (1 << n1);
        }

        if (AppConfig.isValidNumber(n2)) {
            mask |= (1 << n2);
        }

        if (AppConfig.isValidNumber(n3)) {
            mask |= (1 << n3);
        }

        return mask;
    }

    private void writeEngineBootState() {
        if (store == null) {
            return;
        }

        try {
            store.setState("v20_engine_predraw", preDrawFeatureEngine == null ? "0" : "1");
            store.setState("v20_engine_full_scan", fullCandidateScanEngine == null ? "0" : "1");
            store.setState(
                    "v20_engine_causal_factor", causalFactorMatrixEngine == null ? "0" : "1");
            store.setState(
                    "v20_engine_position_cause", positionCauseMatrixEngine == null ? "0" : "1");
            store.setState(
                    "v20_engine_teacher_number", teacherNumberMatrixEngine == null ? "0" : "1");
            store.setState(
                    "v20_engine_counterfactual", counterfactualRankEngine == null ? "0" : "1");
            store.setState("v20_engine_third_guard", thirdGuardMatrixEngine == null ? "0" : "1");
            store.setState("v20_engine_rule_health", ruleHealthMatrixEngine == null ? "0" : "1");
            store.setState("v20_engine_shadow_health", shadowHealthGateEngine == null ? "0" : "1");
            store.setState("v20_engine_false_freeze", falseStrongFreezeEngine == null ? "0" : "1");
            store.setState("v20_engine_seed_expand", seedCauseExpansionEngine == null ? "0" : "1");
            store.setState("v20_engine_dynamic_chain", dynamicCauseChainEngine == null ? "0" : "1");
            store.setState(
                    "v20_engine_diagnosis_fix_plan", diagnosisFixPlanEngine == null ? "0" : "1");
            store.setState(
                    "v20_engine_module_effect_score", moduleEffectScoreEngine == null ? "0" : "1");
            store.setState(
                    "v20_engine_shadow_promotion",
                    shadowPromotionVerifierEngine == null ? "0" : "1");
            store.setState(
                    "v20_engine_dynamic_freeze_matrix",
                    dynamicFreezeMatrixEngine == null ? "0" : "1");
            store.setState(
                    "v20_engine_adaptive_judge", adaptiveLearningJudgeEngine == null ? "0" : "1");
            store.setState(
                    "v20_teacher_matrix_effective",
                    getEffectiveTeacherMatrixEngine() == null ? "0" : "1");
            store.setState(
                    "v20_engine_boot_summary",
                    "V20引擎加载：PreDraw="
                            + (preDrawFeatureEngine == null ? "0" : "1")
                            + " FullScan="
                            + (fullCandidateScanEngine == null ? "0" : "1")
                            + " Cause="
                            + (causalFactorMatrixEngine == null ? "0" : "1")
                            + " PosCause="
                            + (positionCauseMatrixEngine == null ? "0" : "1")
                            + " TeacherNum="
                            + (teacherNumberMatrixEngine == null ? "0" : "1")
                            + " DynamicChain="
                            + (dynamicCauseChainEngine == null ? "0" : "1")
                            + " TeacherEffective="
                            + (getEffectiveTeacherMatrixEngine() == null ? "0" : "1")
                            + " Bridge=1");
            store.setState("v20_engine_boot_at", String.valueOf(System.currentTimeMillis()));
        } catch (Throwable e) {
        }
    }

    /** Thin service delegate; the recovery phase itself is owned by RuntimeRecoveryCoordinatorR926. */
    private void recordCriticalCatchR926(String stage, String issue, Throwable failure) {
        RuntimeRecoveryCoordinatorR926.Record record =
                RuntimeRecoveryCoordinatorR926.record(store, stage, issue, failure);
        lastError = record.canonical();
    }

    private String appendReason(String oldText, String addText) {
        if (oldText == null || oldText.length() == 0) {
            return addText == null ? "" : addText;
        }

        if (addText == null || addText.length() == 0) {
            return oldText;
        }

        if (oldText.indexOf(addText) >= 0) {
            return oldText;
        }

        return oldText + "｜" + addText;
    }

    private String formatElapsed(long ms) {
        if (ms < 0L) {
            ms = 0L;
        }

        long sec = ms / 1000L;
        long min = sec / 60L;
        long leftSec = sec % 60L;

        if (min > 0L) {
            return min + "分" + leftSec + "秒";
        }

        return leftSec + "秒";
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }

    private String safeError(Throwable e) {
        if (e == null) {
            return "";
        }
        return safeError(DatabaseCoordinator.describeCauseChain(e, 600));
    }

    private String safeError(String msg) {
        if (msg == null) {
            return "";
        }

        String out = msg;

        try {
            String key = KeyProvider.getApiKey();
            if (key != null && key.length() > 0) {
                out = out.replace(key, "***");
            }
        } catch (Throwable e) {
        }

        if (out.length() > 300) {
            out = out.substring(0, 300);
        }

        return out;
    }

    private int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (Throwable e) {
            return fallback;
        }
    }

    private long parseLong(String value, long fallback) {
        try {
            return Long.parseLong(value);
        } catch (Throwable e) {
            return fallback;
        }
    }

    private int readInt(Object obj, String fieldName, int def) {
        if (obj == null || fieldName == null || fieldName.length() == 0) {
            return def;
        }

        try {
            Field f = obj.getClass().getField(fieldName);
            Object v = f.get(obj);

            if (v instanceof Number) {
                return ((Number) v).intValue();
            }

            if (v instanceof String) {
                return Integer.parseInt((String) v);
            }
        } catch (Throwable e) {
        }

        return def;
    }

    private double readDouble(Object obj, String fieldName, double def) {
        if (obj == null || fieldName == null || fieldName.length() == 0) {
            return def;
        }

        try {
            Field f = obj.getClass().getField(fieldName);
            Object v = f.get(obj);

            if (v instanceof Number) {
                double d = ((Number) v).doubleValue();

                if (Double.isNaN(d) || Double.isInfinite(d)) {
                    return def;
                }

                return d;
            }

            if (v instanceof String) {
                double d = Double.parseDouble((String) v);

                if (Double.isNaN(d) || Double.isInfinite(d)) {
                    return def;
                }

                return d;
            }
        } catch (Throwable e) {
        }

        return def;
    }

    private String readString(Object obj, String fieldName, String def) {
        if (obj == null || fieldName == null || fieldName.length() == 0) {
            return def;
        }

        try {
            Field f = obj.getClass().getField(fieldName);
            Object v = f.get(obj);

            if (v == null) {
                return def;
            }

            return String.valueOf(v);
        } catch (Throwable e) {
        }

        return def;
    }

    private void writeInt(Object obj, String fieldName, int value) {
        if (obj == null || fieldName == null || fieldName.length() == 0) {
            return;
        }

        try {
            Field f = obj.getClass().getField(fieldName);
            Class type = f.getType();

            if (type == Integer.TYPE || type == Integer.class) {
                f.set(obj, Integer.valueOf(value));
            } else if (type == Long.TYPE || type == Long.class) {
                f.set(obj, Long.valueOf((long) value));
            } else if (type == Double.TYPE || type == Double.class) {
                f.set(obj, Double.valueOf((double) value));
            } else if (type == Float.TYPE || type == Float.class) {
                f.set(obj, Float.valueOf((float) value));
            } else if (type == String.class) {
                f.set(obj, String.valueOf(value));
            }
        } catch (Throwable e) {
        }
    }

    private void writeLong(Object obj, String fieldName, long value) {
        if (obj == null || fieldName == null || fieldName.length() == 0) return;
        try {
            Field f = obj.getClass().getField(fieldName);
            Class type = f.getType();
            if (type == Long.TYPE || type == Long.class) f.set(obj, Long.valueOf(value));
            else if (type == Integer.TYPE || type == Integer.class)
                f.set(obj, Integer.valueOf((int) value));
            else if (type == Double.TYPE || type == Double.class)
                f.set(obj, Double.valueOf((double) value));
            else if (type == String.class) f.set(obj, String.valueOf(value));
        } catch (Throwable e) {
        }
    }

    private void writeDouble(Object obj, String fieldName, double value) {
        if (obj == null || fieldName == null || fieldName.length() == 0) {
            return;
        }

        if (Double.isNaN(value) || Double.isInfinite(value)) {
            value = 0.0d;
        }

        try {
            Field f = obj.getClass().getField(fieldName);
            Class type = f.getType();

            if (type == Double.TYPE || type == Double.class) {
                f.set(obj, Double.valueOf(value));
            } else if (type == Float.TYPE || type == Float.class) {
                f.set(obj, Float.valueOf((float) value));
            } else if (type == Integer.TYPE || type == Integer.class) {
                f.set(obj, Integer.valueOf((int) value));
            } else if (type == Long.TYPE || type == Long.class) {
                f.set(obj, Long.valueOf((long) value));
            } else if (type == String.class) {
                f.set(obj, String.valueOf(value));
            }
        } catch (Throwable e) {
        }
    }

    private void writeString(Object obj, String fieldName, String value) {
        if (obj == null || fieldName == null || fieldName.length() == 0) {
            return;
        }

        if (value == null) {
            value = "";
        }

        try {
            Field f = obj.getClass().getField(fieldName);
            Class type = f.getType();

            if (type == String.class) {
                f.set(obj, value);
            } else if (type == Integer.TYPE || type == Integer.class) {
                f.set(obj, Integer.valueOf(parseInt(value, 0)));
            } else if (type == Long.TYPE || type == Long.class) {
                f.set(obj, Long.valueOf(parseLong(value, 0L)));
            } else if (type == Double.TYPE || type == Double.class) {
                f.set(obj, Double.valueOf(parseDouble(value, 0.0d)));
            } else if (type == Float.TYPE || type == Float.class) {
                f.set(obj, Float.valueOf((float) parseDouble(value, 0.0d)));
            }
        } catch (Throwable e) {
        }
    }

    private double parseDouble(String value, double fallback) {
        try {
            return Double.parseDouble(value);
        } catch (Throwable e) {
            return fallback;
        }
    }

    private double maxDoubleFields(Object obj, String[] fields) {
        if (obj == null || fields == null || fields.length == 0) {
            return 0.0d;
        }

        double best = 0.0d;

        for (int i = 0; i < fields.length; i++) {
            String f = fields[i];

            if (f == null || f.length() == 0) {
                continue;
            }

            double v = readDouble(obj, f, 0.0d);

            if (Double.isNaN(v) || Double.isInfinite(v)) {
                continue;
            }

            if (v > best) {
                best = v;
            }
        }

        if (best < 0.0d) {
            return 0.0d;
        }

        if (best > 1.0d && best <= 100.0d) {
            best = best / 100.0d;
        }

        if (best > 1.0d) {
            best = 1.0d;
        }

        return best;
    }

    private String limitText(String text, int max) {
        return shortText(text, max);
    }

    private String shortText(String text, int max) {
        if (text == null) {
            return "";
        }

        if (max <= 0) {
            max = 120;
        }

        if (text.length() <= max) {
            return text;
        }

        return text.substring(0, max);
    }

    private boolean sameText(String a, String b) {
        String x = a == null ? "" : a;
        String y = b == null ? "" : b;
        return x.equals(y);
    }

    /*
     * 链路修改升级V2频率状态刷新。
     * 这里只读取真实Decision统计并持久化频率状态；不改FinalP、不改Gate、不生成BET。
     */
    private void refreshFrequencyStateForAnalysis(String baseIssue, String targetIssue) {
        if (store == null) return;
        try {
            store.ensureLinkModificationV2TablesNow();
            Models.FrequencyState old = store.loadFrequencyState();
            Models.FrequencyState x = old == null ? new Models.FrequencyState() : old;
            if (!V51BuildManifest.MODEL_PROVEN || !AppConfig.R35_REAL_AUTO_UNLOCK_ENABLED) {
                x.stateKey = "MAIN";
                x.asOfIssue = baseIssue == null ? "" : baseIssue;
                x.requiredFloorBetCount = 0;
                x.targetBetCount = 0;
                x.floorDebtCount = 0;
                x.targetDebtCount = 0;
                x.frequencyDebt = 0.0d;
                x.debtFrozen = 1;
                x.status = "NOT_APPLICABLE_MODEL_NOT_PROVEN";
                x.version = AppConfig.R35_RUNTIME_REPAIR_VERSION;
                x.updatedAt = System.currentTimeMillis();
                store.saveFrequencyState(x);
                store.setState(
                        "frequency_state_target_issue", targetIssue == null ? "" : targetIssue);
                store.setState("frequency_state_status", x.status);
                store.setState("frequency_state_debt", "0.0");
                store.setState("frequency_state_required_bets", "0");
                store.setState("frequency_state_target_bets", "0");
                store.setState("frequency_state_floor_debt_count", "0");
                store.setState("frequency_state_target_debt_count", "0");
                store.setState(
                        "frequency_state_policy",
                        "NOT_APPLICABLE_MODEL_NOT_PROVEN;REAL_LOCKED;NO_FREQUENCY_DEBT");
                return;
            }
            x.stateKey = "MAIN";
            x.asOfIssue = baseIssue == null ? "" : baseIssue;
            x.frequency50 = store.getRecentRealBetFrequency(AppConfig.FREQ_LOOKBACK_SHORT);
            x.frequency100 = store.getRecentRealBetFrequency(AppConfig.FREQ_LOOKBACK_MAIN);
            x.frequency300 = store.getRecentRealBetFrequency(AppConfig.FREQ_LOOKBACK_LONG);
            x.validIssueCount = store.getRecentDecisionCount(AppConfig.FREQ_LOOKBACK_MAIN);
            x.realBetCount = store.getRecentRealBetCount(AppConfig.REAL_BET_FREQUENCY_LOOKBACK);
            x.recentProbeCount =
                    (int)
                            Math.round(
                                    store.getRecentProbeFrequency(AppConfig.PROBE_RECENT_WINDOW)
                                            * (double)
                                                    Math.max(
                                                            1,
                                                            store.getRecentDecisionCount(
                                                                    AppConfig
                                                                            .PROBE_RECENT_WINDOW)));
            x.qualifiedOpportunityCount =
                    store.getRecentQualifiedOpportunityCount(AppConfig.REAL_BET_FREQUENCY_LOOKBACK);
            x.requiredFloorBetCount = AppConfig.buildRequiredRealBetCount(x.validIssueCount);
            x.targetBetCount = AppConfig.buildTargetRealBetCount(x.validIssueCount);
            x.floorDebtCount =
                    AppConfig.buildFrequencyFloorDebtCount(x.validIssueCount, x.realBetCount);
            x.targetDebtCount =
                    AppConfig.buildFrequencyTargetDebtCount(x.validIssueCount, x.realBetCount);
            x.noUpperCap = AppConfig.REAL_BET_FREQUENCY_HAS_UPPER_CAP ? 0 : 1;
            x.recentLoseCount = store.getRecentRealLoseCount(AppConfig.PROBE_RECENT_WINDOW);
            boolean frozen = isFrequencyDebtFrozenSafe();
            x.debtFrozen = frozen ? 1 : 0;
            x.frequencyDebt =
                    AppConfig.buildFrequencyDebt(
                            x.frequencyDebt,
                            x.frequency100,
                            x.validIssueCount,
                            x.qualifiedOpportunityCount,
                            frozen);
            if (frozen) x.status = AppConfig.FREQ_STATE_FROZEN;
            else if (x.floorDebtCount > 0) x.status = AppConfig.FREQ_STATE_HARD_FLOOR;
            else if (x.targetDebtCount > 0) x.status = AppConfig.FREQ_STATE_TARGET_RECOVERY;
            else x.status = AppConfig.FREQ_STATE_NORMAL;
            x.version = LINK_MODIFICATION_V2_AUTOFETCH_VERSION;
            x.updatedAt = System.currentTimeMillis();
            store.saveFrequencyState(x);
            store.setState("frequency_state_target_issue", targetIssue == null ? "" : targetIssue);
            store.setState("frequency_state_status", x.status);
            store.setState("frequency_state_freq50", String.valueOf(x.frequency50));
            store.setState("frequency_state_freq100", String.valueOf(x.frequency100));
            store.setState("frequency_state_freq300", String.valueOf(x.frequency300));
            store.setState("frequency_state_debt", String.valueOf(x.frequencyDebt));
            store.setState(
                    "frequency_state_required_bets", String.valueOf(x.requiredFloorBetCount));
            store.setState("frequency_state_target_bets", String.valueOf(x.targetBetCount));
            store.setState("frequency_state_actual_bets", String.valueOf(x.realBetCount));
            store.setState("frequency_state_floor_debt_count", String.valueOf(x.floorDebtCount));
            store.setState("frequency_state_target_debt_count", String.valueOf(x.targetDebtCount));
            store.setState("frequency_state_no_upper_cap", x.noUpperCap == 1 ? "1" : "0");
            store.setState(
                    "frequency_state_policy", "硬下限15%｜目标20%｜无上限；AutoFetch只统计，DecisionGate唯一出手出口");
        } catch (Throwable e) {
            try {
                store.setState("frequency_state_error", safeError(e));
            } catch (Throwable ignore) {
            }
        }
    }

    private boolean isFrequencyDebtFrozenSafe() {
        if (store == null) return true;
        try {
            String data = store.getState("data_guard_status", "");
            String snapshot = store.getState("tiered_snapshot_cold_status", "");
            String adaptive = store.getState("adaptive_judge_status", "");
            String target = store.getState("analysis_target_issue", "");
            if (data != null && (data.indexOf("ERROR") >= 0 || data.indexOf("BROKEN") >= 0))
                return true;
            if (snapshot != null && snapshot.indexOf("HARD_STALE") >= 0) return true;
            if (adaptive != null
                    && (adaptive.indexOf("FREEZE") >= 0 || adaptive.indexOf("REJECT") >= 0))
                return true;
            return target == null;
        } catch (Throwable e) {
            return true;
        }
    }

    private ServiceHeartbeatStore heartbeatStateV8() {
        if (heartbeatStoreV8 == null) {
            heartbeatStoreV8 = new ServiceHeartbeatStore(getApplicationContext());
        }
        return heartbeatStoreV8;
    }

    /** 正式Decision落库后预留一个确定的WARM微片，不依赖“有空再试”的饥饿调度。 只跑Teacher BUILDING累计，不生成BET，不改变当前Decision。 */
    private void postReservedTeacherSliceAfterDecisionV8(final String targetIssue) {
        if (AppConfig.ENABLE_V51_TIANSHU && AppConfig.V51_RUNTIME_LEGACY_HEAVY_ISOLATION) return;
        if (!running || learningHandler == null || store == null) return;
        final String t = targetIssue == null ? "" : targetIssue;
        if (t.length() > 0 && t.equals(lastReservedTeacherSliceTargetV8)) return;
        if (reservedTeacherSlicePostedV8) return;
        reservedTeacherSlicePostedV8 = true;
        learningHandler.postDelayed(
                new Runnable() {
                    @Override
                    public void run() {
                        reservedTeacherSlicePostedV8 = false;
                        if (!running || store == null) return;
                        if (analysisBusy) {
                            postReservedTeacherSliceAfterDecisionV8(t);
                            return;
                        }
                        long started = System.currentTimeMillis();
                        String summary = "NO_ENGINE";
                        int processed = 0;
                        String cursor = "";
                        try {
                            if (teacherNumberMatrixEngine == null)
                                throw new IllegalStateException("TEACHER_TYPED_ENGINE_MISSING");
                            TeacherNumberMatrixEngine.TeacherRunReport r =
                                    teacherNumberMatrixEngine.runBackgroundSlice(
                                            store,
                                            AppConfig.TEACHER_WARM_RESERVED_AFTER_DECISION_MS);
                            if (r == null)
                                throw new IllegalStateException("TEACHER_TYPED_REPORT_NULL");
                            if (r.ok == 0)
                                throw new IllegalStateException(
                                        "TEACHER_TYPED_SLICE_ERROR:" + String.valueOf(r.summary));
                            summary = String.valueOf(r.summary);
                            processed = r.sampleCount;
                            Models.BackgroundCursor c =
                                    store.loadBackgroundCursor("TEACHER_WARM_CURSOR");
                            if (c != null) {
                                processed = c.processedCount;
                                cursor = c.cursorIssue == null ? "" : c.cursorIssue;
                            }
                            long cost = System.currentTimeMillis() - started;
                            lastReservedTeacherSliceTargetV8 = t;
                            heartbeatStateV8()
                                    .markTeacherSlice("RESERVED_OK", processed, cursor, cost);
                            store.setState(
                                    AppConfig.STATE_TEACHER_RESERVED_SLICE,
                                    "OK|target="
                                            + t
                                            + "|processed="
                                            + processed
                                            + "|cursor="
                                            + cursor
                                            + "|cost="
                                            + cost
                                            + "ms|summary="
                                            + summary);
                        } catch (Throwable e) {
                            long cost = System.currentTimeMillis() - started;
                            heartbeatStateV8()
                                    .markTeacherSlice("RESERVED_ERROR", processed, cursor, cost);
                            try {
                                store.setState(
                                        AppConfig.STATE_TEACHER_RESERVED_SLICE,
                                        "ERROR|target=" + t + "|" + safeError(e));
                            } catch (Throwable ignore) {
                            }
                        }
                    }
                },
                AppConfig.TEACHER_WARM_RESERVED_POST_DELAY_MS);
    }

    private void postBackgroundEvidencePumpV6(long delayMs, String reason) {
        if (AppConfig.ENABLE_V51_TIANSHU && AppConfig.V51_RUNTIME_LEGACY_HEAVY_ISOLATION) return;
        if (!running || learningHandler == null || store == null) return;
        if (backgroundEvidencePumpPostedV6) return;
        backgroundEvidencePumpPostedV6 = true;
        learningHandler.postDelayed(
                new Runnable() {
                    @Override
                    public void run() {
                        backgroundEvidencePumpPostedV6 = false;
                        runBackgroundEvidencePumpV6();
                    }
                },
                Math.max(500L, delayMs));
        try {
            store.setState("background_evidence_pump_wait_reason", reason == null ? "" : reason);
        } catch (Throwable e) {
        }
    }

    private void runBackgroundEvidencePumpV6() {
        if (!running || store == null) return;
        long now = System.currentTimeMillis();
        if (now - lastBackgroundEvidencePumpAtV6 < AppConfig.BACKGROUND_SLICE_MIN_GAP_MS) {
            postBackgroundEvidencePumpV6(AppConfig.BACKGROUND_EVIDENCE_RETRY_MS, "slice_gap");
            return;
        }
        if (shouldDeferHeavyBackgroundNow()) {
            postBackgroundEvidencePumpV6(AppConfig.BACKGROUND_EVIDENCE_RETRY_MS, "realtime_guard");
            return;
        }
        if (analysisBusy || hasRealtimeAnalysisNeed()) {
            postBackgroundEvidencePumpV6(
                    AppConfig.BACKGROUND_EVIDENCE_RETRY_MS, "analysis_pending");
            return;
        }
        lastBackgroundEvidencePumpAtV6 = now;
        try {
            heartbeatStateV8().markTeacherPump("RUNNING", "background_evidence_pump");
        } catch (Throwable ignore) {
        }
        lastBackgroundSliceAtV5 = now;
        long start = now;
        String summary = "NO_ENGINE";
        try {
            Object engine = getEffectiveTeacherMatrixEngine();
            if (engine instanceof TeacherNumberMatrixEngine) {
                TeacherNumberMatrixEngine.TeacherRunReport r =
                        ((TeacherNumberMatrixEngine) engine)
                                .runBackgroundSlice(
                                        store,
                                        Math.min(
                                                AppConfig.TEACHER_BACKGROUND_SLICE_BUDGET_MS,
                                                AppConfig.BACKGROUND_SLICE_MAX_MS));
                summary = r == null ? "OK" : String.valueOf(r.summary);
                store.setState("background_evidence_pump_call_mode", "DIRECT_TYPED_TEACHER_SLICE");
            } else if (engine != null) {
                Object r =
                        invokeOptional(
                                engine,
                                "runBackgroundSlice",
                                new Class[] {DbStore.class, Long.TYPE},
                                new Object[] {
                                    store,
                                    Long.valueOf(
                                            Math.min(
                                                    AppConfig.TEACHER_BACKGROUND_SLICE_BUDGET_MS,
                                                    AppConfig.BACKGROUND_SLICE_MAX_MS))
                                });
                if (r == REFLECTION_NO_METHOD)
                    r =
                            invokeOptional(
                                    engine,
                                    "runTick",
                                    new Class[] {DbStore.class},
                                    new Object[] {store});
                summary = r == null ? "OK" : String.valueOf(r);
                store.setState("background_evidence_pump_call_mode", "REFLECTION_COMPAT");
            }
            HashMap<String, String> pumpState = new HashMap<String, String>();
            pumpState.put("background_evidence_pump_status", "OK");
            pumpState.put(
                    "background_evidence_pump_last_at", String.valueOf(System.currentTimeMillis()));
            pumpState.put(
                    "background_evidence_pump_cost_ms",
                    String.valueOf(System.currentTimeMillis() - start));
            pumpState.put("background_evidence_pump_summary", summary);
            pumpState.put(
                    "background_evidence_pump_version",
                    AppConfig.V20_FINAL_BACKGROUND_PROBABILITY_FIX_VERSION);
            store.trySetTelemetryStatesR77(pumpState);
            try {
                heartbeatStateV8().markTeacherPump("OK", summary);
            } catch (Throwable ignore) {
            }
        } catch (Throwable e) {
            CrashRecorder.recordCaught(getApplicationContext(), e, "BACKGROUND_EVIDENCE_PUMP");
            try {
                store.setState("background_evidence_pump_status", "ERROR");
                store.setState("background_evidence_pump_summary", safeError(e));
                heartbeatStateV8().markTeacherPump("ERROR", safeError(e));
            } catch (Throwable ignore) {
            }
        }
        long cost = System.currentTimeMillis() - start;
        if (cost > AppConfig.BACKGROUND_SLICE_MAX_MS * 2L) {
            try {
                store.setState("background_evidence_pump_overrun", String.valueOf(cost));
            } catch (Throwable ignore) {
            }
        }
        postBackgroundEvidencePumpV6(
                Math.max(15000L, AppConfig.BACKGROUND_EVIDENCE_PUMP_INTERVAL_MS), "loop");
    }

    // Android invokes these callbacks on the UI thread. Release caches before
    // telemetry; all SQLite work stays on the existing runtime-guard owner.
    private final java.util.concurrent.atomic.AtomicBoolean memoryRecoveryPostedR70 =
            new java.util.concurrent.atomic.AtomicBoolean(false);
    private volatile int lastMemoryTrimLevelR70;
    private volatile boolean lowMemoryObservedR70;

    private void memoryPressureR70(final int level, final boolean lowMemory) {
        lastMemoryTrimLevelR70=level;
        if(lowMemory)lowMemoryObservedR70=true;
        if (AppConfig.B07R2_LOW_MEMORY_SELF_HEAL) {
            RollingWindowCache.shared().clear();
            FullInformationFeatureGraphEngine.shared().clear();
        }
        final Handler owner=runtimeGuardHandler;
        if(owner==null||!running||!memoryRecoveryPostedR70.compareAndSet(false,true))return;
        boolean accepted=owner.post(new Runnable(){@Override public void run(){
            try {
                if(!running||store==null||userRequestedStopV5)return;
                final int trimLevel=lastMemoryTrimLevelR70;
                HashMap<String,String> state=new HashMap<String,String>();
                state.put("b07r2_trim_memory_at",Long.toString(System.currentTimeMillis()));
                state.put("b07r2_trim_memory_level",Integer.toString(trimLevel));
                if(lowMemoryObservedR70){
                    state.put("b07r2_low_memory_at",Long.toString(System.currentTimeMillis()));
                    state.put("b07r2_low_memory_action","CACHE_RELEASED_ASYNC_RUNTIME_RECOVERY");
                }
                store.trySetTelemetryStatesR77(state);
                if(isServiceUserEnabledV5()){
                    BootReceiver.scheduleAllRecovery(getApplicationContext(),"R70:memory:"+trimLevel);
                    ensureRuntimeLoopsAliveV17("R70:memory:"+trimLevel);
                    writeBusinessHeartbeatState("last_memory_trim_at","level="+trimLevel,"MEMORY_TRIM_RECOVERY");
                    if(!analysisBusy)postAnalysisForLatest("R70低内存恢复补预测",true);
                }
            } catch(Throwable error){
                CrashRecorder.recordCaught(getApplicationContext(),error,"R70_ASYNC_MEMORY_RECOVERY");
            } finally {memoryRecoveryPostedR70.set(false);}
        }});
        if(!accepted)memoryRecoveryPostedR70.set(false);
    }

    @Override public void onLowMemory() {
        try {memoryPressureR70(android.content.ComponentCallbacks2.TRIM_MEMORY_COMPLETE,true);}
        finally {super.onLowMemory();}
    }

    @Override public void onTrimMemory(int level) {
        try {memoryPressureR70(level,false);}
        finally {super.onTrimMemory(level);}
    }

    /* FIX73487246：进程心跳不等于业务心跳；每个端到端阶段单独持久化。 */
    private void writeBusinessHeartbeatState(String timeKey, String issueOrAction, String health) {
        try {
            if (store == null) return;
            long now = System.currentTimeMillis();
            HashMap<String, String> patch = new HashMap<String, String>();
            patch.put(timeKey, String.valueOf(now));
            if (issueOrAction != null && issueOrAction.length() > 0)
                patch.put(timeKey + "_detail", issueOrAction);
            patch.put("background_business_health", health == null ? "" : health);
            patch.put("background_business_health_at", String.valueOf(now));
            patch.put("background_business_health_version", DEADLOCK_RECOVERY_BUILD);
            LiveTelemetryProjectionR81.tryWrite(store, patch);
            try {
                heartbeatStateV8().markBusinessStage(health, issueOrAction);
            } catch (Throwable ignore) {
            }
        } catch (Throwable ignore) {
        }
    }

    private ArrayList<Models.ComboCandidate> freezeEvidenceDomainsAfterFinalPV20(
            ArrayList<Models.ComboCandidate> candidates, String targetIssue) {
        ArrayList<Models.ComboCandidate> out =
                candidates == null ? new ArrayList<Models.ComboCandidate>() : candidates;
        int frozen = 0;
        for (int i = 0; i < out.size(); i++) {
            Models.ComboCandidate c = out.get(i);
            if (c == null) continue;
            Models.freezeStableEvidenceDomains(c, targetIssue);
            if (c.evidenceDomainFrozen == 1) frozen++;
        }
        try {
            if (store != null) {
                store.setState("domain_frozen_stage_v20", "AFTER_DYNAMIC_GATE");
                store.setState("domain_frozen_count_v20", String.valueOf(frozen));
                store.setState("domain_frozen_issue_v20", safe(targetIssue));
                store.setState(
                        "domain_frozen_version_v20", AppConfig.ACTIVE_EVIDENCE_DOMAIN_SCHEMA);
            }
        } catch (Throwable ignore) {
        }
        return out;
    }

    public static final String B13_UNIQUE_PRODUCTION_RUNTIME =
            "AutoFetchService->V51RuntimeOrchestrator";
    public static final long B13_SERVICE_STALE_MS = 180000L,
            B13_FETCH_STALE_MS = 180000L,
            B13_DRAW_DATA_STALE_MS = 125000L,
            B13_ANALYSIS_STALE_MS = 180000L,
            B13_SETTLEMENT_STALE_MS = 360000L;

    public static final class B13RuntimeHealth {
        public final long now, serviceBeat, fetchBeat, analysisBeat, settlementBeat, latestDrawAt;
        public final long serviceAge, fetchAge, analysisAge, settlementAge, latestDrawAge;
        public final boolean serviceAlive,
                fetchAlive,
                dataFresh,
                providerCorroborated,
                analysisAlive,
                settlementAlive,
                analysisFailed,
                businessAlive;
        public final String latestIssue, status, rootCause, analysisStage, analysisError, checksum;
        public final String committedIssue, predictionState;

        B13RuntimeHealth(
                long now,
                long service,
                long fetch,
                long analysis,
                long settlement,
                long latestDrawAt,
                String latestIssue,
                boolean providerCorroborated,
                String analysisStage,
                String analysisError, String committedIssue) {
            this.now = now;
            serviceBeat = service;
            fetchBeat = fetch;
            analysisBeat = analysis;
            settlementBeat = settlement;
            this.latestDrawAt = latestDrawAt;
            this.latestIssue = latestIssue == null ? "" : latestIssue.trim();
            serviceAge = age(now, service);
            fetchAge = age(now, fetch);
            analysisAge = age(now, analysis);
            settlementAge = age(now, settlement);
            latestDrawAge = age(now, latestDrawAt);
            serviceAlive = serviceAge <= B13_SERVICE_STALE_MS;
            fetchAlive = fetchAge <= B13_FETCH_STALE_MS;
            dataFresh = latestDrawAt > 0L && latestDrawAge <= B13_DRAW_DATA_STALE_MS;
            this.providerCorroborated = providerCorroborated;
            analysisAlive = analysisAge <= B13_ANALYSIS_STALE_MS;
            settlementAlive = settlement <= 0 || settlementAge <= B13_SETTLEMENT_STALE_MS;
            this.analysisStage = analysisStage == null ? "" : analysisStage.trim();
            this.analysisError = analysisError == null ? "" : analysisError.trim();
            analysisFailed = "ANALYSIS_FAILED".equals(this.analysisStage);
            this.committedIssue = committedIssue == null ? "" : committedIssue;
            predictionState = ResearchScheduleR924.predictionState(this.latestIssue, this.committedIssue, latestDrawAge);
            boolean predictionCurrent = "CURRENT_COMMITTED".equals(predictionState);
            businessAlive = serviceAlive && fetchAlive && dataFresh && analysisAlive
                    && !analysisFailed && predictionCurrent;
            rootCause =
                    !serviceAlive
                            ? "SERVICE_STALE"
                            : (!fetchAlive
                                    ? "FETCH_STALE"
                                    : (!dataFresh
                                            ? "DRAW_DATA_STALE"
                                            : (analysisFailed
                                                    ? "ANALYSIS_FAILED"
                                                    : (!analysisAlive
                                                            ? "BUSINESS_LOOP_STALE_ANALYSIS"
                                                            : (!predictionCurrent ? predictionState : (!settlementAlive
                                                                    ? "SETTLEMENT_STALE"
                                                                    : "NONE"))))));
            status =
                    businessAlive
                            ? (settlementAlive ? "HEALTHY" : "BUSINESS_ALIVE_SETTLEMENT_WATCH")
                            : "BUSINESS_LOOP_STALE";
            checksum = V51ChecksumEngine.sha256(canonical());
        }

        public String canonical() {
            return now
                    + "|"
                    + serviceBeat
                    + "|"
                    + fetchBeat
                    + "|"
                    + analysisBeat
                    + "|"
                    + settlementBeat
                    + "|"
                    + latestDrawAt
                    + "|"
                    + latestIssue
                    + "|"
                    + serviceAge
                    + "|"
                    + fetchAge
                    + "|"
                    + analysisAge
                    + "|"
                    + settlementAge
                    + "|"
                    + latestDrawAge
                    + "|"
                    + serviceAlive
                    + "|"
                    + fetchAlive
                    + "|"
                    + dataFresh
                    + "|"
                    + providerCorroborated
                    + "|"
                    + analysisAlive
                    + "|"
                    + settlementAlive
                    + "|"
                    + businessAlive
                    + "|"
                    + analysisStage
                    + "|"
                    + analysisError
                    + "|"
                    + status
                    + "|"
                    + rootCause + "|" + committedIssue + "|" + predictionState;
        }
    }

    public static void b13RecordBusinessBeat(DbStore store, String stage, String targetIssue) {
        if (store == null) return;
        long now = System.currentTimeMillis();
        String s = stage == null ? "UNKNOWN" : stage;
        try {
            HashMap<String, String> patch = new HashMap<String, String>();
            patch.put("b13_service_beat_ms", String.valueOf(now));
            patch.put("b13_last_stage", s);
            patch.put("b13_last_target_issue", targetIssue == null ? "" : targetIssue);
            patch.put("b13_last_stage_ms", String.valueOf(now));
            if (s.startsWith("FETCH") || s.startsWith("DRAW"))
                patch.put("b13_fetch_beat_ms", String.valueOf(now));
            if (s.startsWith("ANALYSIS"))
                patch.put("b13_analysis_beat_ms", String.valueOf(now));
            if (s.startsWith("ANALYSIS")) {
                patch.put("b13_analysis_stage", s);
                if (!"ANALYSIS_FAILED".equals(s)) patch.put("b13_analysis_error", "");
            }
            if (s.startsWith("SETTLEMENT"))
                patch.put("b13_settlement_beat_ms", String.valueOf(now));
            LiveTelemetryProjectionR81.tryWrite(store, patch);
        } catch (Throwable ignore) {
        }
    }

    public static B13RuntimeHealth b13Health(DbStore store) {
        long now = System.currentTimeMillis();
        if (store == null)
            return new B13RuntimeHealth(
                    now, 0, 0, 0, 0, 0, "", false, "UNKNOWN", "STORE_NULL", "");
        long fetch =
                maxTimestamp(
                        parseLong(store.getState("b13_fetch_beat_ms", "0")),
                        parseLong(store.getState("last_fetch_success_at", "0")));
        long analysis =
                maxTimestamp(
                        parseLong(store.getState("b13_analysis_beat_ms", "0")),
                        parseLong(store.getState("last_analysis_success_at", "0")));
        long settlement =
                maxTimestamp(
                        parseLong(store.getState("b13_settlement_beat_ms", "0")),
                        parseLong(store.getState("last_settlement_success_at", "0")));
        long service =
                maxTimestamp(
                        parseLong(store.getState("b13_service_beat_ms", "0")),
                        parseLong(store.getState("last_service_started_at", "0")),
                        parseLong(store.getState("last_service_command_at", "0")),
                        fetch,
                        analysis,
                        settlement);
        Models.Draw latestDraw = null;
        long latestDrawAt = 0L;
        String latestIssue = "";
        try {
            latestDraw = store.getLatestDraw();
            if (latestDraw != null && latestDraw.isValid()) {
                latestIssue = latestDraw.issue == null ? "" : latestDraw.issue.trim();
                latestDrawAt = normalizeB13DrawTime(latestDraw.openTime, now);
            }
        } catch (Throwable ignore) {
        }
        boolean providerCorroborated = false;
        try {
            ProviderFreshnessGateR3611.Candidate selected =
                    ProviderFreshnessGateR3611.snapshot().selected;
            String selectedIssue = selected.issue == null ? "" : selected.issue.trim();
            long observationAge = age(now, selected.observedAt);
            if (latestIssue.length() > 0
                    && latestIssue.equals(selectedIssue)
                    && "FRESH".equals(selected.code)
                    && selected.observedAt > 0L
                    && observationAge <= B13_FETCH_STALE_MS
                    && selected.ageMs >= 0L
                    && selected.ageMs <= ProviderFreshnessGateR3611.STALE_MS) {
                long providerOpenAt = selected.observedAt - selected.ageMs;
                if (providerOpenAt > 0L) {
                    latestDrawAt = Math.max(latestDrawAt, providerOpenAt);
                    providerCorroborated = true;
                }
            }
        } catch (Throwable ignore) {
        }
        return new B13RuntimeHealth(
                now,
                service,
                fetch,
                analysis,
                settlement,
                latestDrawAt,
                latestIssue,
                providerCorroborated,
                store.getState("b13_analysis_stage", "UNKNOWN"),
                store.getState("b13_analysis_error", ""), latestCommittedIssueR924(store));
    }

    private static String latestCommittedIssueR924(DbStore store) {
        android.database.Cursor cursor = null;
        try {
            cursor = store.readable().rawQuery("SELECT target_issue FROM "
                    + DbHelper.TABLE_R35_SHADOW_OBSERVATION
                    + " WHERE pre_draw_frozen=1 ORDER BY target_issue DESC LIMIT 1", null);
            return cursor.moveToFirst() ? cursor.getString(0) : "";
        } catch (Throwable unavailable) { return ""; }
        finally { if (cursor != null) cursor.close(); }
    }

    public static String b13Diagnostics(DbStore store) {
        B13RuntimeHealth h = b13Health(store);
        return "status="
                + h.status
                + "|rootCause="
                + h.rootCause
                + "|committedIssue=" + h.committedIssue + "|predictionState=" + h.predictionState
                + "|serviceAge="
                + h.serviceAge
                + "|fetchAge="
                + h.fetchAge
                + "|latestIssue="
                + h.latestIssue
                + "|latestDrawAge="
                + h.latestDrawAge
                + "|dataFresh="
                + (h.dataFresh ? 1 : 0)
                + "|providerCorroborated="
                + (h.providerCorroborated ? 1 : 0)
                + "|analysisAge="
                + h.analysisAge
                + "|analysisStage="
                + h.analysisStage
                + "|analysisError="
                + h.analysisError
                + "|settlementAge="
                + h.settlementAge
                + "|businessAlive="
                + (h.businessAlive ? 1 : 0)
                + "|runtime="
                + B13_UNIQUE_PRODUCTION_RUNTIME
                + "|hash="
                + h.checksum;
    }

    private static long normalizeB13DrawTime(long openTime, long now) {
        long t = openTime;
        if (t > 0L && t < 100000000000L) t *= 1000L;
        if (t <= 0L || t > now + 10L * 60L * 1000L) return 0L;
        if (now - t > 7L * 24L * 60L * 60L * 1000L) return 0L;
        return t;
    }

    private static long age(long now, long t) {
        return t <= 0 ? Long.MAX_VALUE : Math.max(0, now - t);
    }

    private static long parseLong(String x) {
        try {
            return Long.parseLong(x == null ? "0" : x);
        } catch (Throwable e) {
            return 0;
        }
    }

    private static long maxTimestamp(long... values) {
        long out = 0L;
        if (values == null) return out;
        for (int i = 0; i < values.length; i++) out = Math.max(out, values[i]);
        return out;
    }

public static B13RuntimeHealth b13HealthSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, com.ethk3.novaonepro.ReportReadContextCP4 store) {
        long now = report.nowMs();
        if (store == null)
            return new B13RuntimeHealth(
                    now, 0, 0, 0, 0, 0, "", false, "UNKNOWN", "STORE_NULL", "");
        long fetch =
                maxTimestampSnapshotCP4(report, 
                        parseLongSnapshotCP4(report, store.getState("b13_fetch_beat_ms", "0")),
                        parseLongSnapshotCP4(report, store.getState("last_fetch_success_at", "0")));
        long analysis =
                maxTimestampSnapshotCP4(report, 
                        parseLongSnapshotCP4(report, store.getState("b13_analysis_beat_ms", "0")),
                        parseLongSnapshotCP4(report, store.getState("last_analysis_success_at", "0")));
        long settlement =
                maxTimestampSnapshotCP4(report, 
                        parseLongSnapshotCP4(report, store.getState("b13_settlement_beat_ms", "0")),
                        parseLongSnapshotCP4(report, store.getState("last_settlement_success_at", "0")));
        long service =
                maxTimestampSnapshotCP4(report, 
                        parseLongSnapshotCP4(report, store.getState("b13_service_beat_ms", "0")),
                        parseLongSnapshotCP4(report, store.getState("last_service_started_at", "0")),
                        parseLongSnapshotCP4(report, store.getState("last_service_command_at", "0")),
                        fetch,
                        analysis,
                        settlement);
        Models.Draw latestDraw = null;
        long latestDrawAt = 0L;
        String latestIssue = "";
        try {
            latestDraw = store.getLatestDraw();
            if (latestDraw != null && latestDraw.isValid()) {
                latestIssue = latestDraw.issue == null ? "" : latestDraw.issue.trim();
                latestDrawAt = normalizeB13DrawTimeSnapshotCP4(report, latestDraw.openTime, now);
            }
        } catch (Throwable ignore) {throw new com.ethk3.novaonepro.ReportReadContextCP4.ReportReadFailureCP4("REPORT_QUERY_OR_FORMAT_FAILED",ignore);}
        boolean providerCorroborated = false;
        try {
            ProviderFreshnessGateR3611.Candidate selected =
                    ProviderFreshnessGateR3611.snapshot().selected;
            String selectedIssue = selected.issue == null ? "" : selected.issue.trim();
            long observationAge = ageSnapshotCP4(report, now, selected.observedAt);
            if (latestIssue.length() > 0
                    && latestIssue.equals(selectedIssue)
                    && "FRESH".equals(selected.code)
                    && selected.observedAt > 0L
                    && observationAge <= B13_FETCH_STALE_MS
                    && selected.ageMs >= 0L
                    && selected.ageMs <= ProviderFreshnessGateR3611.STALE_MS) {
                long providerOpenAt = selected.observedAt - selected.ageMs;
                if (providerOpenAt > 0L) {
                    latestDrawAt = Math.max(latestDrawAt, providerOpenAt);
                    providerCorroborated = true;
                }
            }
        } catch (Throwable ignore) {throw new com.ethk3.novaonepro.ReportReadContextCP4.ReportReadFailureCP4("REPORT_QUERY_OR_FORMAT_FAILED",ignore);}
        return new B13RuntimeHealth(
                now,
                service,
                fetch,
                analysis,
                settlement,
                latestDrawAt,
                latestIssue,
                providerCorroborated,
                store.getState("b13_analysis_stage", "UNKNOWN"),
                store.getState("b13_analysis_error", ""), latestCommittedIssueR924SnapshotCP4(report, store));
    }

public static String latestCommittedIssueR924SnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, com.ethk3.novaonepro.ReportReadContextCP4 store) {
        android.database.Cursor cursor = null;
        try {
            cursor = report.select("SELECT target_issue FROM "
                    + DbHelper.TABLE_R35_SHADOW_OBSERVATION
                    + " WHERE pre_draw_frozen=1 ORDER BY target_issue DESC LIMIT 1", null);
            return cursor.moveToFirst() ? cursor.getString(0) : "";
        } catch (Throwable unavailable) {throw new com.ethk3.novaonepro.ReportReadContextCP4.ReportReadFailureCP4("REPORT_QUERY_OR_FORMAT_FAILED",unavailable);}
        finally { if (cursor != null) cursor.close(); }
    }

public static long ageSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, long now, long t) {
        return t <= 0 ? Long.MAX_VALUE : Math.max(0, now - t);
    }

public static long normalizeB13DrawTimeSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, long openTime, long now) {
        long t = openTime;
        if (t > 0L && t < 100000000000L) t *= 1000L;
        if (t <= 0L || t > now + 10L * 60L * 1000L) return 0L;
        if (now - t > 7L * 24L * 60L * 60L * 1000L) return 0L;
        return t;
    }

public long parseLongSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, String value, long fallback) {
        try {
            return Long.parseLong(value);
        } catch (Throwable e) {throw new com.ethk3.novaonepro.ReportReadContextCP4.ReportReadFailureCP4("REPORT_QUERY_OR_FORMAT_FAILED",e);}
    }

public static long parseLongSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, String x) {
        try {
            return Long.parseLong(x == null ? "0" : x);
        } catch (Throwable e) {throw new com.ethk3.novaonepro.ReportReadContextCP4.ReportReadFailureCP4("REPORT_QUERY_OR_FORMAT_FAILED",e);}
    }

public static long maxTimestampSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, long... values) {
        long out = 0L;
        if (values == null) return out;
        for (int i = 0; i < values.length; i++) out = Math.max(out, values[i]);
        return out;
    }

public static String b13DiagnosticsSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, com.ethk3.novaonepro.ReportReadContextCP4 store) {
        B13RuntimeHealth h = b13HealthSnapshotCP4(report, store);
        return "status="
                + h.status
                + "|rootCause="
                + h.rootCause
                + "|committedIssue=" + h.committedIssue + "|predictionState=" + h.predictionState
                + "|serviceAge="
                + h.serviceAge
                + "|fetchAge="
                + h.fetchAge
                + "|latestIssue="
                + h.latestIssue
                + "|latestDrawAge="
                + h.latestDrawAge
                + "|dataFresh="
                + (h.dataFresh ? 1 : 0)
                + "|providerCorroborated="
                + (h.providerCorroborated ? 1 : 0)
                + "|analysisAge="
                + h.analysisAge
                + "|analysisStage="
                + h.analysisStage
                + "|analysisError="
                + h.analysisError
                + "|settlementAge="
                + h.settlementAge
                + "|businessAlive="
                + (h.businessAlive ? 1 : 0)
                + "|runtime="
                + B13_UNIQUE_PRODUCTION_RUNTIME
                + "|hash="
                + h.checksum;
    }
}
