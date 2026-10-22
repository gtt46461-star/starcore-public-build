package com.ethk3.novaonepro;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Process;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicLong;

/*
 * MainActivity
 *
 * 第25/25文件：V20 UI承接满血版
 *
 * 只做：
 * 1. 界面显示
 * 2. 状态读取
 * 3. 长文本滑动/展开
 * 4. 历史开奖/历史决策显示
 * 5. 线程状态展示
 * 6. V20动态因链后台画像状态展示
 *
 * 绝不做：
 * 1. 不在UI层生成BET
 * 2. 不在UI层救援BET
 * 3. 不在UI层修改概率
 * 4. 不在UI层修改动态门槛
 * 5. 不在UI层修改DecisionGate裁决
 * 6. 不在UI层修改结算规则
 * 7. 不用旧预测顶替当前目标期
 *
 * V20新增展示：
 * 1. DynamicCauseChain 动态因链摘要
 * 2. CausalFactorMatrix 因果因子矩阵摘要
 * 3. PositionCauseMatrix 位置因果矩阵摘要
 * 4. TeacherNumberMatrix 教师号码矩阵摘要
 * 5. CounterfactualRank 反事实排名摘要
 * 6. ThirdGuardMatrix 第三防线摘要
 * 7. RuleHealthMatrix 规则健康摘要
 * 8. ShadowHealthGate 影子健康摘要
 * 9. FalseStrongFreeze 伪强冻结摘要
 * 10. SeedCauseExpansion 种子扩展摘要
 * 11. RealBetDiagnosis 逐单复盘诊断摘要
 * 12. ModuleEffectScore 模块效果评分摘要
 * 13. ShadowPromotionVerifier SHADOW晋级摘要
 * 14. DynamicFreezeMatrix 动态冻结矩阵摘要
 * 15. AdaptiveLearningJudge 智能审判摘要
 * 16. DiagnosisFixPlan 文件级修复建议摘要
 *
 * 真实统计口径：
 * 只显示 store/RuntimeStats/Trade 已结算结果。
 * 真实胜率/盈亏只认 BET + REAL_TRADE + settled=1。
 * HOLD/SHADOW/后台学习/教师矩阵/V20画像不计真实胜率/盈亏。
 *
 * Java7 / AIDE / Android 原生兼容。
 */
public final class MainActivity extends Activity {
    public static final String OUMAN_GALAXY_REBUILD_VERSION =
            AppConfig.OUMAN_GALAXY_REBUILD_VERSION;

    public static final String DEADLOCK_RECOVERY_BUILD = V49BuildManifest.STRATEGY_VERSION;

    private static final long UI_LIGHT_NORMAL_REFRESH_MS = 1000L;
    private static final long UI_LIGHT_BUSY_REFRESH_MS = 1000L;
    private static final long UI_RUNTIME_TRUTH_REFRESH_MS = 15000L;
    private static final long UI_HEAVY_REFRESH_MS = 120000L;
    private static final long UI_SCROLL_RELEASE_DELAY_MS = 1400L;
    private static final long UI_SCROLL_RECOVERY_HEAVY_DELAY_MS = 2600L;
    // R971: broadcast values are aged from producer elapsedRealtime, not wall time.

    private static final long UI_BROADCAST_LIGHT_DEBOUNCE_MS = 260L;
    private static final long UI_BROADCAST_HEAVY_DEBOUNCE_MS = 1500L;
    private static final long UI_SCROLL_BLOCK_HEAVY_MS = 450L;
    private static final long UI_DECISION_HEAVY_DELAY_MS = 2200L;

    private static final int DRAW_HISTORY_QUERY_LIMIT = 80;
    private static final int DRAW_HISTORY_RENDER_LIMIT = 30;
    private static final int DECISION_HISTORY_QUERY_LIMIT = 120;
    private static final int DECISION_HISTORY_RENDER_LIMIT = 5;
    private static final int REASON_RENDER_SAFE_LIMIT = 12;

    private static final String TAG_EXPANDED = "EXPANDED";
    private static final String TAG_COLLAPSED = "COLLAPSED";

    private static final String ACTION_V20_CAUSE_CHAIN_UPDATED =
            AppConfig.PACKAGE_NAME + ".ACTION_V20_CAUSE_CHAIN_UPDATED";
    private static final String UI_V20_VERSION = AppConfig.ACTIVE_QUALITY_BUILD_VERSION;
    private static final int UI_V20_EVIDENCE_LIMIT = 520;
    private static final int UI_V20_EVIDENCE_SHORT_LIMIT = 220;
    private static final int UI_DIAGNOSIS_SUMMARY_LIMIT = 520;
    private static final int UI_DIAGNOSIS_DETAIL_LIMIT = 1800;
    private static final int UI_DIAGNOSIS_COPY_LIMIT = AppConfig.V41_DIAGNOSIS_CACHE_MAX_CHARS;
    private static final int UI_FULL_DIAGNOSIS_LINE_LIMIT = 180;
    private static final int UI_FULL_DIAGNOSIS_DETAIL_LIMIT = 6000;
    private static final int UI_FULL_DIAGNOSIS_COPY_LIMIT =
            AppConfig.B07R31_REPORT_COMPLETE_MAX_CHARS;
    private static final int UI_DIAGNOSIS_CLIPBOARD_CHUNK =
            AppConfig.V41_CLIPBOARD_SAFE_CHUNK_CHARS;
    private static final int UI_DYNAMIC_CAUSAL_LINE_LIMIT = 220;
    private static final int UI_DYNAMIC_CAUSAL_DETAIL_LIMIT = 1200;
    private static final int UI_DYNAMIC_CAUSAL_COPY_LIMIT = 8000;
    private static final int UI_TEXT_RENDER_HARD_LIMIT = 10000;
    private static final int UI_HEAVY_RENDER_TOTAL_CHAR_BUDGET = 18000;
    private static final int UI_LOW_MEMORY_RENDER_TOTAL_CHAR_BUDGET = 8000;

    private DbStore store;
    private DbStore readStore;
    private DataGuard dataGuard;
    private ReasonEngine reasonEngine;
    private volatile boolean startupDbInitRunning;
    private volatile boolean startupDbReady;
    private volatile String startupDbError = "";
    private volatile boolean startupServiceArmPosted;
    /** True only after the complete R923 layout and every launch-critical binding succeed. */
    private volatile boolean launchUiReadyR922;

    private Handler uiHandler;
    private HandlerThread uiReadThread;
    private Handler uiReadHandler;
    private HandlerThread uiDetailReadThreadR71;
    private Handler uiDetailReadHandlerR71;
    private volatile LightSnapshot diagnosticSnapshotR71;

    private TextView txtAppTitle;
    private TextView txtAppSubtitle;

    private TextView txtServiceBadge;
    private TextView txtCountdown;
    private TextView txtCurrentIssue;
    private TextView txtTargetIssue;
    private TextView txtApiLine;
    private TextView txtRuntimeStatus;

    private TextView txtDecisionLabel;
    private TextView txtDecision;
    private TextView txtPositionNumbers;
    private TextView txtDecisionSummary;

    private TextView txtFinalP;
    private TextView txtDynamicGate;
    private TextView txtEdge;

    private TextView txtStateMode;
    private TextView txtTrendLife;
    private TextView txtDriftRisk;
    private TextView txtBreakDepth;
    private TextView txtRecoveryScore;

    private TextView txtSimilarHitRate;
    private TextView txtEffectiveSample;
    private TextView txtComboHitRate;
    private TextView txtChampionGap;
    private TextView txtComboGap;

    private TextView txtStatsLine1;
    private TextView txtStatsLine2;
    private TextView txtStatsLine3;
    private TextView txtBacktestSummary;

    private LinearLayout listReasons;
    private LinearLayout whyTruthHostR906;
    private LinearLayout diagnosticStateHostR906;
    private WhyTruthSectionRendererR906 whyTruthRendererR906;
    private DiagnosticStateRendererR906 diagnosticStateRendererR906;
    private final HomeTruthPresenterR906 homeTruthPresenterR906 = new HomeTruthPresenterR906();
    private final FrameMetricsR906 frameMetricsR906 = new FrameMetricsR906();
    // R907: R906 truth snapshot is diagnostic, not a 4-7s polling workload. Cache it and refresh
    // only when target changes or the bounded truth interval expires, avoiding SQLite contention.
    private static final class TruthObservationR76 {
        final RuntimeTruthSnapshotR906 snapshot;
        final String target;
        final long sampledMono;
        TruthObservationR76(RuntimeTruthSnapshotR906 snapshot,String target,long sampledMono) {
            this.snapshot=snapshot;this.target=target;this.sampledMono=sampledMono;
        }
    }
    // Atomic publication lets the light worker reuse the same completed observation as the
    // dashboard, without waiting for the unrelated heavy history/diagnostic read to finish.
    private volatile TruthObservationR76 cachedTruthR907;
    private static final long R907_TRUTH_CACHE_MS = 15000L;
    private LinearLayout listDrawHistory;
    private LinearLayout listHistory;

    private View scrollDrawHistory;
    private View scrollHistory;

    private Button btnStart;
    private Button btnStop;
    private Button btnRefresh;
    private Button btnBacktestFull;
    private Button btnOpenHistory;
    private Button btnOpenDiagnosis;
    private Button btnOpenModelLearning;
    private Button btnOpenDatabaseHealth;
    private Button btnBackgroundReliability;

    private boolean receiverRegistered;
    private boolean destroyed;

    private long lastUiRefreshAt;
    private long lastLightRefreshAt;
    private long lastHeavyRefreshAt;
    /* R3.6.7: reject async DB snapshots that started before a newer draw/decision broadcast. */
    private long lastAuthoritativeUiSignalAt;
    private final AtomicLong uiRevision = new AtomicLong(0L);

    private long liveCountdownMs;
    private long liveCountdownAt;
    private String liveCountdownTargetR971 = "";
    private CountdownProjectionR971.Anchor liveClockAnchorCP12;

    private boolean userScrolling;
    private boolean pendingLightRefresh;
    private boolean pendingHeavyRefresh;
    private boolean lightRefreshPosted;
    private boolean heavyRefreshPosted;
    private boolean lightReadRunning;
    private boolean heavyReadRunning;
    private boolean lightReadDirty;
    private boolean heavyReadDirty;
    private volatile boolean forceHeavyRenderOnce;
    private volatile boolean suppressScrollGuard;
    private boolean heavyRenderBudgetActive;
    private int heavyRenderBudgetRemaining;
    private int consecutiveUiOom;

    private long lastUserScrollAt;

    private String uiLockedTargetIssue;
    private String uiLockedCurrentIssue;
    private long uiLockedTargetAt;

    private String lastReasonRenderKey;
    private String lastDrawHistoryRenderKey;
    private String lastDecisionHistoryRenderKey;
    private String lastRuntimeRenderKey;
    private String lastDecisionRenderKey;
    private String lastStatsRenderKey;
    private String lastBacktestRenderKey;
    private String lastDiagnosisRenderKey;
    private String lastWhyTruthRenderKeyR905;

    private LightSnapshot lastLightSnapshot;
    private volatile String cachedFullDiagnosisCopyText = "";
    private int diagnosisClipboardChunkIndex = 0;

    // R919: one immutable snapshot feeds every read-only runtime truth surface.
    private TextView txtR919PipelineTruth;
    private TextView txtR919ShadowTruth;
    private TextView txtR919ResearchTruth;
    private TextView txtR919RealGateTruth;
    private TextView txtResearchSnapshotR76;
    private volatile boolean r919TruthReadRunning;
    private volatile boolean r941UiResumed;
    /** Installed only after the complete diagnostic panel has joined the activity hierarchy. */
    private volatile boolean r940DiagnosticPanelInstalled;
    private long lastR919TruthRefreshAt;
    private volatile RuntimeTruthSnapshotR906 lastR919TruthSnapshot;
    private volatile ResearchPipelineStatusR76.Snapshot cachedResearchStatusR76;

    private ViewTreeObserver.OnScrollChangedListener globalScrollListener;

    private final Runnable scrollReleaseRunnable =
            new Runnable() {
                @Override
                public void run() {
                    userScrolling = false;
                    lastUserScrollAt = 0L;

                    if (pendingLightRefresh) {
                        pendingLightRefresh = false;
                        requestLightRefreshSoon(true);
                    }
                    if (pendingHeavyRefresh) {
                        pendingHeavyRefresh = false;
                        requestHeavyRefreshDelayed(UI_SCROLL_RECOVERY_HEAVY_DELAY_MS);
                    }
                }
            };

    private final Runnable lightRefreshRunnable =
            new Runnable() {
                @Override
                public void run() {
                    lightRefreshPosted = false;
                    refreshLightOnly();
                }
            };

    private final Runnable heavyRefreshRunnable =
            new Runnable() {
                @Override
                public void run() {
                    heavyRefreshPosted = false;

                    if (!forceHeavyRenderOnce && isUserScrollingNow()) {
                        pendingHeavyRefresh = true;
                        return;
                    }

                    refreshHeavyOnly();
                }
            };

    private final Runnable uiTicker =
            new Runnable() {
                @Override
                public void run() {
                    if (destroyed || !r941UiResumed) {
                        return;
                    }

                    try {
                        refreshCountdownOnly();
                        renderHeartbeatTruthR323();

                        long now = System.currentTimeMillis();
                        long lightInterval =
                                isUiBusyStateBySnapshot()
                                        ? UI_LIGHT_BUSY_REFRESH_MS
                                        : UI_LIGHT_NORMAL_REFRESH_MS;

                        if (now - lastLightRefreshAt >= lightInterval) {
                            requestLightRefreshSoon(false);
                        }

                        if (now - lastHeavyRefreshAt >= UI_HEAVY_REFRESH_MS) {
                            requestHeavyRefreshSoon();
                        }

                        if (!lightReadRunning
                                && !heavyReadRunning
                                && !isUserScrollingNow()
                                && now - lastR919TruthRefreshAt >= UI_RUNTIME_TRUTH_REFRESH_MS) {
                            requestR919TruthRefresh(false);
                        }

                    } catch (RuntimeException error) {
                        android.util.Log.e("STARCORE_UI_TICK", "R978_TICK_REFRESH_FAILED", error);
                    } finally {
                        // A rendering failure cannot permanently remove the UI timer;
                        // a paused/destroyed Activity must not re-arm itself.
                        if (!destroyed && r941UiResumed && uiHandler != null) {
                            uiHandler.postDelayed(this, 1000L);
                        }
                    }
                }
            };

    private final BroadcastReceiver serviceReceiver =
            new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if (intent == null) {
                        return;
                    }

                    String action = intent.getAction();

                    if (AppConfig.ACTION_SERVICE_TICK.equals(action)) {
                        long ms = intent.getLongExtra(AppConfig.EXTRA_COUNTDOWN_MS, 0L);
                        String currentIssue = intent.getStringExtra(AppConfig.EXTRA_CURRENT_ISSUE);
                        String targetIssue = intent.getStringExtra(AppConfig.EXTRA_TARGET_ISSUE);

                        try{
                            CountdownProjectionR971.Anchor incoming=CountdownProjectionR971.Anchor.received(
                                targetIssue,intent.getLongExtra("clock_revision_cp12",-1L),intent.getStringExtra("clock_session_cp12"),
                                intent.getLongExtra("clock_deadline_wall_cp12",0L),intent.getLongExtra("clock_deadline_mono_cp12",0L),intent.getLongExtra("clock_anchor_sampled_cp12",-1L));
                            if(!acceptClockAnchorR76(incoming,TimeSourceR811.monoNowMs()))return;
                        }catch(IllegalArgumentException rejectedClock){return;}
                        liveCountdownMs = ms;
                        liveCountdownAt = intent.getLongExtra("r971_tick_elapsed", android.os.SystemClock.elapsedRealtime());
                        liveCountdownTargetR971 = targetIssue == null ? "" : targetIssue;

                        handleTargetIssueChanged(currentIssue, targetIssue, "tick");

                        refreshCountdownOnly();

                        if (currentIssue != null && currentIssue.length() > 0) {
                            setText(txtCurrentIssue, "当前期：" + uiLockedCurrentIssue);
                        }

                        if (targetIssue != null && targetIssue.length() > 0) {
                            setText(txtTargetIssue, "目标期：" + uiLockedTargetIssue);
                        }

                        if (ms <= 1800L) {
                            requestLightRefreshSoon(false);
                        }

                        return;
                    }

                    if (AppConfig.ACTION_DECISION_UPDATED.equals(action)) {
                        /* R901 anti-flicker invariant: the current decision card has exactly one
                         * rendering owner.  Broadcasts are only invalidation/target signals; they
                         * must never paint a second copy of the same decision with a different
                         * field population while the canonical DB snapshot is being read. */
                        lastAuthoritativeUiSignalAt = System.currentTimeMillis();
                        String currentIssue = intent.getStringExtra(AppConfig.EXTRA_CURRENT_ISSUE);
                        String targetIssue = intent.getStringExtra(AppConfig.EXTRA_TARGET_ISSUE);
                        handleTargetIssueChanged(currentIssue, targetIssue, "decision_broadcast_r901");
                        requestLightRefreshSoon(false);
                        requestHeavyRefreshDelayed(UI_DECISION_HEAVY_DELAY_MS);
                        return;
                    }

                    if (AppConfig.ACTION_STATE_CHANGED.equals(action)) {
                        String currentIssue = intent.getStringExtra(AppConfig.EXTRA_CURRENT_ISSUE);
                        String targetIssue = intent.getStringExtra(AppConfig.EXTRA_TARGET_ISSUE);

                        lastAuthoritativeUiSignalAt = System.currentTimeMillis();
                        handleTargetIssueChanged(currentIssue, targetIssue, "state");
                        if (currentIssue != null && currentIssue.length() > 0) {
                            setText(txtCurrentIssue, "当前期：" + uiLockedCurrentIssue);
                        }
                        if (targetIssue != null && targetIssue.length() > 0) {
                            setText(txtTargetIssue, "目标期：" + uiLockedTargetIssue);
                        }

                        requestLightRefreshSoon(false);

                        String status = intent.getStringExtra(AppConfig.EXTRA_STATUS);
                        if (AppConfig.SERVICE_STATUS_SETTLING.equals(status)
                                || "SETTLING".equals(status)) {
                            requestHeavyRefreshDelayed(900L);
                        }

                        return;
                    }

                    if (AppConfig.ACTION_BACKTEST_UPDATED.equals(action)
                            || AppConfig.ACTION_TEACHER_MATRIX_UPDATED.equals(action)
                            || ACTION_V20_CAUSE_CHAIN_UPDATED.equals(action)) {
                        requestLightRefreshSoon(false);
                        requestHeavyRefreshDelayed(UI_DECISION_HEAVY_DELAY_MS);
                    }
                }
            };

    public MainActivity() {}

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        CrashRecorder.install(getApplicationContext());
        CrashRecorder.setRuntimeContext("", "UI_CREATE_PRE_INFLATE");
        destroyed = false;
        startupDbInitRunning = false;
        startupDbReady = false;
        startupDbError = "";
        startupServiceArmPosted = false;
        launchUiReadyR922 = false;

        prepareWindow();
        boolean layoutReady = true;
        try {
            setContentView(R.layout.activity_main);
        } catch (Throwable inflateError) {
            CrashRecorder.recordCaught(getApplicationContext(), inflateError, "UI_LAYOUT_INFLATE");
            layoutReady = false;
            installLaunchSafeModeR922("UI_LAYOUT_INFLATE", inflateError);
        }

        uiHandler = new Handler(Looper.getMainLooper());
        uiReadThread =
                new HandlerThread("eth_k3_ui_read_channel", Process.THREAD_PRIORITY_BACKGROUND);
        uiReadThread.start();
        uiReadHandler = new Handler(uiReadThread.getLooper());
        uiDetailReadThreadR71 = new HandlerThread("starcore_ui_diagnostics", Process.THREAD_PRIORITY_BACKGROUND);
        uiDetailReadThreadR71.start();
        uiDetailReadHandlerR71 = new Handler(uiDetailReadThreadR71.getLooper());
        dataGuard = new DataGuard();
        reasonEngine = new ReasonEngine();
        if (layoutReady) {
            try {
                resetUiState();
                bindViews();
                bindButtons();
                bindInnerScrolls();
                bindGlobalScrollGuard();
                launchUiReadyR922 = true;
                initUiDataLayerAsync();
            } catch (Throwable launchError) {
                launchUiReadyR922 = false;
                CrashRecorder.recordCaught(
                        getApplicationContext(), launchError, "R922_UI_BIND_OR_BOOTSTRAP");
                installLaunchSafeModeR922("R922_UI_BIND_OR_BOOTSTRAP", launchError);
            }
        }
        armBackgroundServiceAfterUiStable();
        if (launchUiReadyR922) {
            requestLightRefreshSoon(true);
            requestHeavyRefreshDelayed(3500L);
            // R912: this diagnostic panel is useful but not part of the launch-critical first frame.
            uiHandler.postDelayed(
                    new Runnable() {
                        @Override public void run() {
                            if (!destroyed && launchUiReadyR922) b15InstallCompleteDiagnosticPanel();
                        }
                    },
                    2200L);
        }
    }

    /**
     * Last-resort launch surface. R923 fixes the resource-ID root cause before compilation; this
     * guard is deliberately separate and exists so a future OEM/resource failure is visible to
     * the user and CrashRecorder instead of becoming another open-and-close loop.
     */
    private void installLaunchSafeModeR922(String stage, Throwable failure) {
        try {
            TextView emergency = new TextView(this);
            emergency.setPadding(32, 48, 32, 48);
            emergency.setTextSize(15.0f);
            emergency.setTextColor(0xfff4f7ff);
            emergency.setBackgroundColor(0xff071225);
            emergency.setTextIsSelectable(true);
            emergency.setText(
                    "星核天枢 R923 启动保护模式\n"
                            + "主界面初始化异常已被拦截，应用没有静默闪退。\n"
                            + "阶段=" + String.valueOf(stage) + "\n"
                            + "版本=" + V51BuildManifest.VERSION_NAME + "\n"
                            + "错误=" + limitText(String.valueOf(failure), 1200) + "\n\n"
                            + "后台服务仍按用户开关尝试恢复；请复制本页错误用于检测报告。");
            setContentView(emergency);
        } catch (Throwable ignored) {
            // Activity itself is no longer allowed to throw from the emergency renderer.
        }
    }

    private void initUiDataLayerAsync() {
        if (destroyed || startupDbInitRunning || startupDbReady || uiReadHandler == null) return;
        startupDbInitRunning = true;
        uiReadHandler.post(
                new Runnable() {
                    @Override
                    public void run() {
                        DbStore opened = null;
                        String error = "";
                        try {
                            CrashRecorder.setRuntimeContext("", "UI_DB_INIT_BACKGROUND");
                            opened = new DbStore(getApplicationContext());
                            try {
                                opened.setState(
                                        "r32_ui_startup_policy", AppConfig.B07R32_STARTUP_POLICY);
                            } catch (Throwable ignore) {
                            }
                        } catch (Throwable e) {
                            error = DatabaseCoordinator.describeCauseChain(e, 600);
                            CrashRecorder.recordCaught(
                                    getApplicationContext(), e, "UI_DB_INIT_BACKGROUND");
                        }
                        final DbStore result = opened;
                        final String resultError = error;
                        if (uiHandler == null) {
                            try {
                                if (result != null) result.close();
                            } catch (Throwable ignore) {
                            }
                            return;
                        }
                        uiHandler.post(
                                new Runnable() {
                                    @Override
                                    public void run() {
                                        startupDbInitRunning = false;
                                        if (destroyed) {
                                            try {
                                                if (result != null) result.close();
                                            } catch (Throwable ignore) {
                                            }
                                            return;
                                        }
                                        store = result;
                                        readStore = result;
                                        startupDbReady = result != null;
                                        startupDbError = resultError == null ? "" : resultError;
                                        if (!startupDbReady && txtRuntimeStatus != null)
                                            txtRuntimeStatus.setText(
                                                    "启动保护：数据库初始化失败，界面保持可用。\n"
                                                            + limitText(startupDbError, 320));
                                        if (startupDbReady) suggestBatteryIgnoreIfNeeded();
                                        else if (!destroyed && uiHandler != null)
                                            uiHandler.postDelayed(() -> initUiDataLayerAsync(), 2000L);

                                        requestLightRefreshSoon(true);
                                        requestHeavyRefreshDelayed(2200L);
                                    }
                                });
                    }
                });
    }

    private void armBackgroundServiceAfterUiStable() {
        armBackgroundServiceAfterUiStableR3610(0);
    }

    private void armBackgroundServiceAfterUiStableR3610(final int attempt) {
        if (destroyed || uiHandler == null || startupServiceArmPosted) return;
        startupServiceArmPosted = true;
        uiHandler.postDelayed(
                new Runnable() {
                    @Override
                    public void run() {
                        startupServiceArmPosted = false;
                        if (destroyed) return;
                        // Never race the Activity's first DB open with the Service's heavy DB open.
                        // This preserves auto-resume semantics; it only serializes cold-start I/O.
                        if (startupDbInitRunning && attempt < 12) {
                            armBackgroundServiceAfterUiStableR3610(attempt + 1);
                            return;
                        }
                        requestNotificationPermissionIfNeeded();
                        try {
                            ServiceHeartbeatStore hb =
                                    new ServiceHeartbeatStore(getApplicationContext());
                            if (hb.isUserEnabled()) {
                                hb.markBusinessStage(
                                        "UI_FIRST_FRAME_STABLE_R3610", AppConfig.B07R32_VERSION);
                                BootReceiver.startAutoFetchService(
                                        getApplicationContext(), "ui_first_frame_stable_r3610");
                            }
                        } catch (Throwable e) {
                            CrashRecorder.recordCaught(
                                    getApplicationContext(), e, "UI_DELAYED_SERVICE_ARM_R3610");
                        }
                    }
                },
                attempt == 0 ? 1800L : 650L);
    }

    @Override
    protected void onResume() {
        super.onResume();
        r941UiResumed = true;
        frameMetricsR906.start();
        if (!launchUiReadyR922) {
            armBackgroundServiceAfterUiStable();
            return;
        }
        registerServiceReceiver();
        if (uiHandler != null) {
            uiHandler.removeCallbacks(uiTicker);
            uiHandler.post(uiTicker);
        }
        renderHeartbeatTruthR323();
        refreshBackgroundReliabilityButton();
        requestLightRefreshSoon(true);
        requestHeavyRefreshDelayed(2600L);
    }

    @Override
    protected void onPause() {
        r941UiResumed = false;
        keepAutoServiceAliveOnUiExit("onPause");
        frameMetricsR906.stop();
        super.onPause();

        unregisterServiceReceiver();

        if (uiHandler != null) {
            uiHandler.removeCallbacks(uiTicker);
            uiHandler.removeCallbacks(scrollReleaseRunnable);
            uiHandler.removeCallbacks(lightRefreshRunnable);
            uiHandler.removeCallbacks(heavyRefreshRunnable);
        }
    }

    @Override
    protected void onDestroy() {
        keepAutoServiceAliveOnUiExit("onDestroy");
        destroyed = true;

        unregisterServiceReceiver();
        unbindGlobalScrollGuard();

        if (uiHandler != null) {
            uiHandler.removeCallbacksAndMessages(null);
        }

        if (uiReadHandler != null) {
            uiReadHandler.removeCallbacksAndMessages(null);
        }
        if (uiDetailReadHandlerR71 != null) uiDetailReadHandlerR71.removeCallbacksAndMessages(null);
        if (uiDetailReadThreadR71 != null) uiDetailReadThreadR71.quitSafely();

        try {
            if (store != null) {
                store.close();
            }
        } catch (Throwable e) {
        }

        try {
            if (readStore != null && readStore != store) readStore.close();
        } catch (Throwable e) {
        }

        try {
            if (uiReadThread != null) {
                if (Build.VERSION.SDK_INT >= 18) {
                    uiReadThread.quitSafely();
                } else {
                    uiReadThread.quit();
                }
            }
        } catch (Throwable e) {
        }

        try {
            new ServiceHeartbeatStore(getApplicationContext())
                    .setLastReason("ui_destroy_only_service_untouched");
        } catch (Throwable ignore) {
        }
        super.onDestroy();
    }

    private void keepAutoServiceAliveOnUiExit(String reason) {
        LifecycleHealthR963.keepService(getApplicationContext(),reason);
    }

    @Override
    public void onTrimMemory(int level) {
        try {
            if (level >= TRIM_MEMORY_RUNNING_LOW) {
                cachedFullDiagnosisCopyText = "";
                diagnosisClipboardChunkIndex = 0;
                pendingHeavyRefresh = false;
                lastReasonRenderKey = "";
                lastDrawHistoryRenderKey = "";
                lastDecisionHistoryRenderKey = "";
                lastDiagnosisRenderKey = "";
                RollingWindowCache.shared().clear();
                FullInformationFeatureGraphEngine.shared().clear();
            }
            ServiceHeartbeatStore hb = new ServiceHeartbeatStore(getApplicationContext());
            hb.markBusinessStage("UI_MEMORY_TRIM", "level=" + level);
        } catch (Throwable ignore) {
        }
        super.onTrimMemory(level);
    }

    @Override
    protected void onStop() {
        keepAutoServiceAliveOnUiExit("onStop");
        super.onStop();
    }

    private void resetUiState() {
        receiverRegistered = false;

        lastUiRefreshAt = 0L;
        lastLightRefreshAt = 0L;
        lastHeavyRefreshAt = 0L;

        liveCountdownMs = 0L;
        liveCountdownAt = 0L;

        userScrolling = false;
        pendingHeavyRefresh = false;
        lightRefreshPosted = false;
        heavyRefreshPosted = false;
        lightReadRunning = false;
        heavyReadRunning = false;
        lightReadDirty = false;
        heavyReadDirty = false;
        forceHeavyRenderOnce = false;
        suppressScrollGuard = false;

        lastUserScrollAt = 0L;

        uiLockedTargetIssue = "";
        uiLockedCurrentIssue = "";
        uiLockedTargetAt = 0L;

        lastReasonRenderKey = "";
        lastDrawHistoryRenderKey = "";
        lastDecisionHistoryRenderKey = "";
        lastRuntimeRenderKey = "";
        lastDecisionRenderKey = "";
        lastStatsRenderKey = "";
        lastBacktestRenderKey = "";
        lastDiagnosisRenderKey = "";

        lastLightSnapshot = null;
    }

    private void prepareWindow() {
        try {
            Window w = getWindow();

            if (w == null) {
                return;
            }

            w.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

            if (Build.VERSION.SDK_INT >= 16) {
                w.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
            }
            if (Build.VERSION.SDK_INT >= 21) {
                w.clearFlags(
                        WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS
                                | WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
                w.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
                w.setStatusBarColor(getColorCompat(R.color.colorRoot));
                w.setNavigationBarColor(getColorCompat(R.color.colorRoot));
            }
        } catch (Throwable e) {
        }
    }

    private void bindViews() {
        txtAppTitle = findText(R.id.txt_app_title);
        txtAppSubtitle = findText(R.id.txt_app_subtitle);
        String releaseLabel=ReleaseManifestR926.VERSION_NAME;
        int versionStart=releaseLabel.indexOf("-R");
        if(versionStart>=0)releaseLabel=releaseLabel.substring(versionStart+2).replace("-"," / R");
        setText(findText(R.id.txt_build_badge),releaseLabel+" · 双接口 / 完整报告");

        txtServiceBadge = findText(R.id.txt_service_badge);
        txtCountdown = findText(R.id.txt_countdown);
        txtCurrentIssue = findText(R.id.txt_current_issue);
        txtTargetIssue = findText(R.id.txt_target_issue);
        txtApiLine = findText(R.id.txt_api_line);
        txtRuntimeStatus = findText(R.id.txt_runtime_status);

        txtDecisionLabel = findText(R.id.txt_decision_label);
        txtDecision = findText(R.id.txt_decision);
        txtPositionNumbers = findText(R.id.txt_position_numbers);
        txtDecisionSummary = findText(R.id.txt_decision_summary);

        txtFinalP = findText(R.id.txt_final_p);
        txtDynamicGate = findText(R.id.txt_dynamic_gate);
        txtEdge = findText(R.id.txt_edge);

        txtStateMode = findText(R.id.txt_state_mode);
        txtTrendLife = findText(R.id.txt_trend_life);
        txtDriftRisk = findText(R.id.txt_drift_risk);
        txtBreakDepth = findText(R.id.txt_break_depth);
        txtRecoveryScore = findText(R.id.txt_recovery_score);

        txtSimilarHitRate = findText(R.id.txt_similar_hit_rate);
        txtEffectiveSample = findText(R.id.txt_effective_sample);
        txtComboHitRate = findText(R.id.txt_combo_hit_rate);
        txtChampionGap = findText(R.id.txt_champion_gap);
        txtComboGap = findText(R.id.txt_combo_gap);

        txtStatsLine1 = findText(R.id.txt_stats_line1);
        txtStatsLine2 = findText(R.id.txt_stats_line2);
        txtStatsLine3 = findText(R.id.txt_stats_line3);
        txtBacktestSummary = findText(R.id.txt_backtest_summary);

        listReasons = (LinearLayout) findViewById(R.id.list_reasons);
        whyTruthHostR906 = (LinearLayout) findViewById(R.id.why_truth_host);
        diagnosticStateHostR906 = (LinearLayout) findViewById(R.id.diagnostic_state_host);
        whyTruthRendererR906 = new WhyTruthSectionRendererR906(this, whyTruthHostR906);
        diagnosticStateRendererR906 = new DiagnosticStateRendererR906(this, diagnosticStateHostR906);
        // Never leave the four truth cards / sixteen state boxes blank while the asynchronous
        // database snapshot is starting.
        RuntimeTruthSnapshotR906 loadingTruth = RuntimeTruthSnapshotR906.pending("");
        whyTruthRendererR906.render(loadingTruth);
        diagnosticStateRendererR906.render(loadingTruth);
        listHistory = (LinearLayout) findViewById(R.id.list_history);
        listDrawHistory = (LinearLayout) findViewById(R.id.list_draw_history);

        scrollDrawHistory = findViewById(R.id.scroll_draw_history);
        scrollHistory = findViewById(R.id.scroll_history);

        btnStart = (Button) findViewById(R.id.btn_start);
        btnStop = (Button) findViewById(R.id.btn_stop);
        btnRefresh = (Button) findViewById(R.id.btn_refresh);
        btnBacktestFull = (Button) findViewById(R.id.btn_backtest_full);
        btnOpenHistory = (Button) findViewById(R.id.btn_open_history);
        btnOpenDiagnosis = (Button) findViewById(R.id.btn_open_diagnosis);
        btnOpenModelLearning = (Button) findViewById(R.id.btn_open_model_learning);
        btnOpenDatabaseHealth = (Button) findViewById(R.id.btn_open_database_health);
        btnBackgroundReliability = (Button) findViewById(R.id.btn_background_reliability);

        fixRuntimeTopTextViews();
        bindDiagnosisCopyTargets();

        if (txtAppTitle != null) {
            txtAppTitle.setText(AppConfig.PROJECT_NAME + " " + V51BuildManifest.VERSION_NAME);
        }

        if (txtAppSubtitle != null) {
            txtAppSubtitle.setText(
                    "MODEL_NOT_PROVEN｜受控候选精确门｜单位置固定K3｜BuildIdentity="
                            + BuildIdentity.STATUS_OK);
        }
    }

    private void fixRuntimeTopTextViews() {
        // R3.6.10 runtime/UI-only repair: collapsed regions keep a deterministic height so
        // heartbeat/light/heavy refreshes cannot make the cards repeatedly grow/shrink.
        fixMultiLineText(txtApiLine, 2);
        fixMultiLineText(txtRuntimeStatus, 6);
        fixMultiLineText(txtDecision, 1);
        fixMultiLineText(txtPositionNumbers, 2);
        fixMultiLineText(txtDecisionSummary, 4);
        fixMultiLineText(txtStatsLine1, 2);
        fixMultiLineText(txtStatsLine2, 2);
        fixMultiLineText(txtStatsLine3, 3);
        fixMultiLineText(txtBacktestSummary, 6);
        fixMultiLineText(txtStateMode, 1);
        fixMultiLineText(txtTrendLife, 1);
        fixMultiLineText(txtDriftRisk, 1);
        fixMultiLineText(txtBreakDepth, 1);
        fixMultiLineText(txtRecoveryScore, 1);
        fixMultiLineText(txtSimilarHitRate, 1);
        fixMultiLineText(txtEffectiveSample, 1);
        fixMultiLineText(txtComboHitRate, 1);
        fixMultiLineText(txtChampionGap, 1);
        fixMultiLineText(txtComboGap, 1);
    }

    private void fixMultiLineText(TextView tv, int maxLines) {
        if (tv == null) {
            return;
        }

        try {
            tv.setSingleLine(false);
            int stableLines = Math.max(1, maxLines);
            /* R901: setLines() is stronger than separate min/max hints on several OEM TextView
             * implementations.  The dashboard must not remeasure between 4 and 20 lines when
             * live broadcasts and DB snapshots arrive close together. */
            tv.setLines(stableLines);
            tv.setMinLines(stableLines);
            tv.setMaxLines(stableLines);
            tv.setEllipsize(TextUtils.TruncateAt.END);
            tv.setIncludeFontPadding(false);
            tv.setTextIsSelectable(false);
            int fixedHeight = Math.max(1, tv.getLineHeight()) * stableLines
                    + tv.getPaddingTop() + tv.getPaddingBottom();
            if (fixedHeight > 0) {
                tv.setMinHeight(fixedHeight);
                tv.setMaxHeight(fixedHeight);
            }
            bindExpandableText(tv, maxLines);
        } catch (Throwable e) {
        }
    }

    private void bindExpandableText(final TextView tv, final int collapsedLines) {
        if (tv == null) {
            return;
        }

        try {
            tv.setTag(TAG_COLLAPSED);
            tv.setOnClickListener(
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            try {
                                if (!(v instanceof TextView)) return;
                                TextView t = (TextView) v;
                                CharSequence full = t.getText();
                                if (full == null) return;
                                String text = full.toString();
                                // R919: long status boxes must never be trapped behind ellipsis.
                                // A dedicated DiagnosisActivity is the canonical scrollable full-text view.
                                if (text.length() > 120 || text.indexOf('\n') >= 0) {
                                    DiagnosisActivity.openText(
                                            MainActivity.this, "完整状态内容", text);
                                    return;
                                }
                                Object tag = t.getTag();
                                boolean expanded =
                                        tag != null && TAG_EXPANDED.equals(String.valueOf(tag));
                                if (expanded) {
                                    t.setMaxLines(collapsedLines <= 0 ? 6 : collapsedLines);
                                    t.setEllipsize(TextUtils.TruncateAt.END);
                                    t.setTag(TAG_COLLAPSED);
                                } else {
                                    t.setMaxLines(Integer.MAX_VALUE);
                                    t.setEllipsize(null);
                                    t.setTag(TAG_EXPANDED);
                                }
                            } catch (Throwable ignored) {
                            }
                        }
                    });
        } catch (Throwable ignored) {
        }
    }

    private void bindButtons() {
        if(txtApiLine!=null){
            txtApiLine.setContentDescription("数据接口状态，点击配置凭据");
            txtApiLine.setOnClickListener(view->{
                java.lang.ref.WeakReference<MainActivity> owner=new java.lang.ref.WeakReference<>(this);
                ApiCredentialSettingsCP12.show(this,()->{MainActivity current=owner.get();if(current!=null&&!current.isFinishing()&&!current.isDestroyed())current.refreshNow();});
            });
        }
        if (btnStart != null) {
            btnStart.setOnClickListener(
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            startAutoService();
                        }
                    });
        }

        if (btnStop != null) {
            btnStop.setOnClickListener(
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            stopAutoService();
                        }
                    });
        }

        if (btnRefresh != null) {
            btnRefresh.setOnClickListener(
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            refreshNow();
                        }
                    });
        }

        if (btnBacktestFull != null) {
            btnBacktestFull.setOnClickListener(
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            requestFullBacktest();
                        }
                    });
        }
        if (btnOpenHistory != null) {
            btnOpenHistory.setOnClickListener(
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            startActivity(new Intent(MainActivity.this, HistoryActivity.class));
                        }
                    });
        }
        if (btnOpenDiagnosis != null) {
            btnOpenDiagnosis.setOnClickListener(
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            startActivity(new Intent(MainActivity.this, DiagnosisActivity.class));
                        }
                    });
        }
        if (btnOpenModelLearning != null) {
            btnOpenModelLearning.setOnClickListener(
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            startActivity(
                                    new Intent(MainActivity.this, ModelLearningActivity.class));
                        }
                    });
        }
        View su1Research=findViewById(R.id.btn_su1_research);
        if(su1Research!=null)su1Research.setOnClickListener(v->startActivity(new Intent(MainActivity.this,com.ethk3.novaonepro.strategyv2.StrategyResearchActivitySU1.class)));
        if (btnOpenDatabaseHealth != null) {
            btnOpenDatabaseHealth.setOnClickListener(
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            startActivity(
                                    new Intent(MainActivity.this, DatabaseHealthActivity.class));
                        }
                    });
        }
        if (btnBackgroundReliability != null) {
            btnBackgroundReliability.setOnClickListener(
                    new View.OnClickListener() {
                        @Override public void onClick(View v) {
                            openBackgroundReliabilitySettings();
                        }
                    });
        }
    }

    /** User-triggered OEM/Android capability setup; the app never changes system policy silently. */
    private void openBackgroundReliabilitySettings() {
        try {
            BackgroundRuntimeCapabilityR811.Snapshot capability =
                    BackgroundRuntimeCapabilityR811.snapshot(getApplicationContext());
            Intent intent;
            android.net.Uri packageUri =
                    android.net.Uri.parse("package:" + getPackageName());
            if (Build.VERSION.SDK_INT >= 23 && !capability.ignoringBatteryOptimizations) {
                intent = new Intent(android.provider.Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
            } else if (Build.VERSION.SDK_INT >= 31 && !capability.exactAlarmRuntimeGranted) {
                intent = new Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                intent.setData(packageUri);
            } else if (Build.VERSION.SDK_INT >= 26 && capability.status.contains("NOTIFICATIONS_DISABLED")) {
                intent = new Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS);
                intent.putExtra(android.provider.Settings.EXTRA_APP_PACKAGE,getPackageName());
            } else {
                intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                intent.setData(packageUri);
            }
            startActivity(intent);
        } catch (Throwable primary) {
            try {
                Intent fallback = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                fallback.setData(android.net.Uri.parse("package:" + getPackageName()));
                startActivity(fallback);
            } catch (Throwable fallback) {
                Toast.makeText(this, "无法打开系统后台设置，请手动允许自启动和无限制电池使用", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void refreshBackgroundReliabilityButton() {
        if (btnBackgroundReliability == null) return;
        BackgroundRuntimeCapabilityR811.Snapshot capability =
                BackgroundRuntimeCapabilityR811.snapshot(getApplicationContext());
        if (!capability.ignoringBatteryOptimizations) {
            btnBackgroundReliability.setText("后台续跑授权：关闭电池优化");
        } else if (Build.VERSION.SDK_INT >= 31 && !capability.exactAlarmRuntimeGranted) {
            btnBackgroundReliability.setText("后台续跑授权：允许精确闹钟");
        } else if (capability.status.contains("NOTIFICATIONS_DISABLED")) {
            btnBackgroundReliability.setText("后台续跑授权：开启通知");
        } else {
            btnBackgroundReliability.setText("后台续跑授权：系统能力已就绪，后台实测待验收");
        }
    }

    private void bindDiagnosisCopyTargets() {
        bindDiagnosisCopyTarget(txtRuntimeStatus);
        bindDiagnosisCopyTarget(txtDecisionSummary);
        bindDiagnosisCopyTarget(txtStatsLine3);
        bindDiagnosisCopyTarget(txtBacktestSummary);
    }

    private void bindDiagnosisCopyTarget(View view) {
        if (view == null) {
            return;
        }
        try {
            view.setOnLongClickListener(
                    new View.OnLongClickListener() {
                        @Override
                        public boolean onLongClick(View v) {
                            copyFullDiagnosisClosedLoopReport();
                            return true;
                        }
                    });
        } catch (Throwable e) {
        }
    }

    private void bindInnerScrolls() {
        // V49 removes nested ScrollViews. The root dashboard is the only scroll owner.
    }

    private boolean isUserScrollingNow() {
        if (suppressScrollGuard) return false;
        long now = System.currentTimeMillis();
        return userScrolling
                || (lastUserScrollAt > 0L && now - lastUserScrollAt < UI_SCROLL_BLOCK_HEAVY_MS);
    }

    private void bindGlobalScrollGuard() {
        if (globalScrollListener != null) return;
        final View root = findViewById(android.R.id.content);
        if (root == null) return;
        globalScrollListener =
                new ViewTreeObserver.OnScrollChangedListener() {
                    @Override
                    public void onScrollChanged() {
                        if (destroyed || suppressScrollGuard) return;
                        userScrolling = true;
                        lastUserScrollAt = System.currentTimeMillis();
                        frameMetricsR906.markScrollActivity();
                        if (uiHandler != null) {
                            uiHandler.removeCallbacks(scrollReleaseRunnable);
                            uiHandler.postDelayed(
                                    scrollReleaseRunnable, UI_SCROLL_RELEASE_DELAY_MS);
                        }
                    }
                };
        try {
            ViewTreeObserver observer = root.getViewTreeObserver();
            if (observer != null && observer.isAlive())
                observer.addOnScrollChangedListener(globalScrollListener);
        } catch (Throwable ignore) {
            globalScrollListener = null;
        }
    }

    private void unbindGlobalScrollGuard() {
        if (globalScrollListener == null) return;
        try {
            View root = findViewById(android.R.id.content);
            if (root != null) {
                ViewTreeObserver observer = root.getViewTreeObserver();
                if (observer != null && observer.isAlive())
                    observer.removeOnScrollChangedListener(globalScrollListener);
            }
        } catch (Throwable ignore) {
        }
        globalScrollListener = null;
    }

    private void requestLightRefreshSoon(boolean force) {
        if (destroyed || uiHandler == null) return;
        if (force) {
            uiHandler.removeCallbacks(lightRefreshRunnable);
            lightRefreshPosted = true;
            uiHandler.post(lightRefreshRunnable);
            return;
        }
        if (lightRefreshPosted) return;
        lightRefreshPosted = true;
        uiHandler.postDelayed(lightRefreshRunnable, UI_BROADCAST_LIGHT_DEBOUNCE_MS);
    }

    private void requestHeavyRefreshSoon() {
        requestHeavyRefreshDelayed(UI_BROADCAST_HEAVY_DEBOUNCE_MS);
    }

    private void requestHeavyRefreshDelayed(long delayMs) {
        if (destroyed || uiHandler == null) return;
        if (isUserScrollingNow()) {
            pendingHeavyRefresh = true;
            return;
        }
        uiHandler.removeCallbacks(heavyRefreshRunnable);
        heavyRefreshPosted = true;
        uiHandler.postDelayed(heavyRefreshRunnable, Math.max(0L, delayMs));
    }

    private void forceHeavyRefreshNow() {
        if (destroyed || uiHandler == null) return;
        pendingHeavyRefresh = false;
        forceHeavyRenderOnce = true;
        uiHandler.removeCallbacks(heavyRefreshRunnable);
        heavyRefreshPosted = true;
        uiHandler.post(heavyRefreshRunnable);
    }

    private void startAutoService() {
        if (DatabaseCoordinator.isExplicitMaintenanceActiveR82()) {
            toast("数据库空间维护进行中，请完成后再手动启动");
            return;
        }
        final long requestedAt = System.currentTimeMillis();
        try {
            ServiceHeartbeatStore hb = new ServiceHeartbeatStore(getApplicationContext());
            hb.setUserEnabled(true);
            hb.markUiStartRequested("ui_manual_start_r323");
        } catch (Throwable e) {
            CrashRecorder.recordCaught(getApplicationContext(), e, "R323_UI_START_ENABLE");
        }
        boolean ok = BootReceiver.forceRearmAutoFetchService(this, "ui_manual_start_r323");
        if (ok) {
            writeUiState("service_last_action", "手动启动请求已提交，等待真实心跳确认");
            toast("已请求启动，正在等待后台真实心跳");
            verifyManualServiceStartR323(requestedAt, 0);
        } else {
            toast("后台服务启动请求失败；检测报告会记录失败原因");
            renderHeartbeatTruthR323();
        }
        requestLightRefreshSoon(true);
    }

    private void verifyManualServiceStartR323(final long requestedAt, final int attempt) {
        if (destroyed || uiHandler == null) return;
        long delay = attempt == 0 ? 900L : (attempt < 3 ? 1500L : 2500L);
        uiHandler.postDelayed(
                new Runnable() {
                    @Override
                    public void run() {
                        if (destroyed) return;
                        ServiceHeartbeatStore hb =
                                new ServiceHeartbeatStore(getApplicationContext());
                        String truth = hb.runtimeTruthState();
                        renderHeartbeatTruthR323();
                        if ("RUNNING".equals(truth)) {
                            toast("后台服务运行已由真实心跳确认");
                            writeUiState(
                                    "r323_manual_start_verify",
                                    "RUNNING|attempt="
                                            + attempt
                                            + "|cost="
                                            + (System.currentTimeMillis() - requestedAt));
                            requestLightRefreshSoon(true);
                            return;
                        }
                        if (attempt >= 5) {
                            String detail = hb.runtimeTruthSummary();
                            writeUiState(
                                    "r323_manual_start_verify",
                                    "FAILED_OR_STILL_STARTING|" + limitText(detail, 420));
                            toast("后台未进入稳定运行；请复制检测报告查看初始化/心跳根因");
                            try {
                                BootReceiver.scheduleAllRecovery(
                                        getApplicationContext(), "r323_ui_verify_timeout");
                            } catch (Throwable ignore) {
                            }
                            return;
                        }
                        if (attempt == 3
                                && ("STALE_WAIT_RECOVERY".equals(truth)
                                        || "INIT_FAILED".equals(truth))) {
                            try {
                                BootReceiver.forceRearmAutoFetchService(
                                        getApplicationContext(), "ui_manual_verify_rearm_r323");
                            } catch (Throwable ignore) {
                            }
                        }
                        verifyManualServiceStartR323(requestedAt, attempt + 1);
                    }
                },
                delay);
    }

    private volatile boolean heartbeatReadR963;
    private void renderHeartbeatTruthR323() {
        if(txtServiceBadge==null||destroyed||heartbeatReadR963)return;
        heartbeatReadR963=true;
        final String badgeCurrentR71=uiLockedCurrentIssue, badgeTargetR71=uiLockedTargetIssue;
        final LightSnapshot badgeSnapshotR71=lastLightSnapshot;
        final boolean visiblePredictionR71=badgeSnapshotR71!=null
                &&isDecisionForTarget(badgeSnapshotR71.decision,badgeTargetR71)
                &&badgeSnapshotR71.decision.position>0&&Models.isValidK3Mask(badgeSnapshotR71.decision.k3Mask);
        if(!LifecycleHealthR963.post(getApplicationContext(),new Runnable(){public void run(){
            String label;int color=R.color.colorWarn;
            try{
                ServiceHeartbeatStore hb=new ServiceHeartbeatStore(getApplicationContext());
                String truth=hb.runtimeTruthState();ServiceHeartbeatStore.BusinessTruthSnapshot business=hb.readBusinessTruth();
                if("RUNNING".equals(truth)){
                    if(business.currentClosureR71(badgeCurrentR71,badgeTargetR71,System.currentTimeMillis())) {
                        label=visiblePredictionR71?"后台运行中｜本期预测与结算正常":"后台运行中｜本期预测已落库，界面同步中";
                        color=visiblePredictionR71?R.color.colorWin:R.color.colorWarn;
                    } else if (!safeText(badgeTargetR71).equals(business.latestCanonical)) label="后台运行中｜当前目标期待分析";
                    else label="后台运行中｜业务状态待更新："+business.rootCause;
                }else if("STARTING".equals(truth))label="后台服务启动中";
                else if("INIT_STALLED".equals(truth))label="后台初始化耗时较长·等待初始化完成或恢复";
                else if("INIT_FAILED_ALIVE".equals(truth)||"INIT_FAILED".equals(truth))label="后台初始化异常·自动重试中";
                else if("STOPPED_BY_USER".equals(truth))label="后台服务已停止";
                else label="后台心跳异常·等待恢复";
            }catch(Exception error){label="后台状态读取异常";}
            final String value=label;final int tint=color;
            if(uiHandler==null||!uiHandler.post(new Runnable(){public void run(){
                heartbeatReadR963=false;if(destroyed||!r941UiResumed)return;
                if(!safeText(badgeTargetR71).equals(safeText(uiLockedTargetIssue))
                        ||!safeText(badgeCurrentR71).equals(safeText(uiLockedCurrentIssue)))return;
                setText(txtServiceBadge,value);setTextColor(txtServiceBadge,tint);
            }}))heartbeatReadR963=false;
        }}))heartbeatReadR963=false;
    }

    private void stopAutoService() {
        boolean ok = BootReceiver.stopAutoFetchService(this);

        if (ok) {
            writeUiState("service_last_action", "手动停止后台服务");
            toast("已请求停止后台服务");
        } else {
            toast("停止服务请求失败");
        }

        refreshAll();
    }

    private void refreshNow() {
        try {
            Intent intent = new Intent(this, AutoFetchService.class);
            intent.setAction(AppConfig.ACTION_REFRESH_NOW);
            intent.setPackage(AppConfig.PACKAGE_NAME);
            if (Build.VERSION.SDK_INT >= 26) startForegroundService(intent);
            else startService(intent);
            writeUiState("ui_last_manual_refresh_at", String.valueOf(System.currentTimeMillis()));
            toast("已请求立即刷新");
        } catch (RuntimeException error) {
            CrashRecorder.recordCaught(getApplicationContext(), error, "R944_UI_REFRESH_REQUEST");
            toast("立即刷新请求失败：" + error.getClass().getSimpleName());
        }
        refreshAll();
    }

    private void requestFullBacktest() {
        try {
            Intent intent = new Intent(this, AutoFetchService.class);
            intent.setAction(AppConfig.ACTION_BACKTEST_FULL_REQUEST);
            intent.setPackage(AppConfig.PACKAGE_NAME);
            if (Build.VERSION.SDK_INT >= 26) startForegroundService(intent);
            else startService(intent);
            writeUiState("ui_last_full_backtest_request_at", String.valueOf(System.currentTimeMillis()));
            toast("完整深度回测已请求");
        } catch (RuntimeException error) {
            CrashRecorder.recordCaught(getApplicationContext(), error, "R944_UI_BACKTEST_REQUEST");
            toast("完整深度回测请求失败：" + error.getClass().getSimpleName());
        }
        refreshAll();
    }

    private void refreshAll() {
        lastUiRefreshAt = System.currentTimeMillis();
        requestLightRefreshSoon(true);
        requestHeavyRefreshDelayed(900L);
    }

    private void refreshLightOnly() {
        if (destroyed) {
            return;
        }

        if (lightReadRunning) {
            lightReadDirty = true;
            return;
        }

        if (uiReadHandler == null) {
            return;
        }

        // Coalesced invalidations request the next read without revoking this one.
        final long requestedRevision = uiRevision.incrementAndGet();
        lightReadRunning = true;
        lightReadDirty = false;

        uiReadHandler.post(
                new Runnable() {
                    @Override
                    public void run() {
                        final LightSnapshot snapshot = loadLightSnapshotSafely();
                        snapshot.uiRevision = requestedRevision;

                        if (uiHandler == null || destroyed) {
                            lightReadRunning = false;
                            return;
                        }

                        uiHandler.post(
                                new Runnable() {
                                    @Override
                                    public void run() {
                                        lightReadRunning = false;

                                        if (destroyed) {
                                            return;
                                        }

                                        renderLightSnapshot(snapshot);

                                        if (lightReadDirty) {
                                            lightReadDirty = false;
                                            requestLightRefreshSoon(false);
                                        }
                                    }
                                });
                    }
                });
    }

    private void refreshHeavyOnly() {
        if (destroyed) {
            return;
        }

        if (!forceHeavyRenderOnce && isUserScrollingNow()) {
            pendingHeavyRefresh = true;
            return;
        }

        if (heavyReadRunning) {
            heavyReadDirty = true;
            return;
        }

        if (uiDetailReadHandlerR71 == null) {
            return;
        }

        heavyReadRunning = true;
        heavyReadDirty = false;

        uiDetailReadHandlerR71.post(
                new Runnable() {
                    @Override
                    public void run() {
                        final HeavySnapshot snapshot = loadHeavySnapshotSafely();

                        if (uiHandler == null || destroyed) {
                            heavyReadRunning = false;
                            return;
                        }

                        uiHandler.post(
                                new Runnable() {
                                    @Override
                                    public void run() {
                                        heavyReadRunning = false;

                                        if (destroyed) {
                                            return;
                                        }

                                        if (!forceHeavyRenderOnce && isUserScrollingNow()) {
                                            pendingHeavyRefresh = true;
                                            return;
                                        }

                                        renderHeavySnapshot(snapshot);
                                        requestLightRefreshSoon(false);
                                        forceHeavyRenderOnce = false;

                                        if (heavyReadDirty) {
                                            heavyReadDirty = false;
                                            requestHeavyRefreshSoon();
                                        }
                                    }
                                });
                    }
                });
    }

    private LightSnapshot loadLightSnapshotSafely() { return loadLightSnapshotSafely(false); }

    private LightSnapshot loadLightSnapshotSafely(boolean includeDiagnostics) {
        LightSnapshot s = new LightSnapshot();
        s.loadedAt = System.currentTimeMillis();
        s.loadedElapsedAtR971 = android.os.SystemClock.elapsedRealtime();

        DbStore db = readStore == null ? store : readStore;

        if (db == null) {
            return s;
        }

        try {
            LightSnapshot diagnosticsR71=diagnosticSnapshotR71;
            if(includeDiagnostics)loadCommonStates(db,s.states);
            else {
                if(diagnosticsR71!=null)s.states.putAll(diagnosticsR71.states);
                db.readRealtimeUiStateR71(s.states);
            }
            s.state = db.getUiRuntimeStateR945(s.states);
            s.drawCount = db.getDrawCount();
            s.drawRemainTo240 = Math.max(0,AppConfig.WINDOW_240-s.drawCount);
            s.latestDraw = db.getLatestDraw();

            AlgorithmAttributionR943.load(db,s.states);

            s.currentIssue = resolveCurrentIssue(s.state, s.states, s.latestDraw);
            s.targetIssue = resolveTargetIssue(s.state, s.states, s.latestDraw);
            s.states.put("r46_history_read_issue", s.latestDraw == null ? "" : s.latestDraw.issue);
            s.states.put("cp12_draw_retention_reason", db.getState("cp12_draw_retention_reason", ""));

            s.decision = loadCurrentDecisionFromStore(db, s.state, s.states, s.latestDraw);

            try {
                if (includeDiagnostics) {
                    s.truthR906 = loadTruthCachedR907(db, s.targetIssue);
                } else {
                    TruthObservationR76 observed=cachedTruthR907;
                    if(observed!=null&&s.targetIssue.equals(observed.target))s.truthR906=observed.snapshot;
                }
                s.whyTruthText = s.truthR906 == null ? "本期诊断待更新" : compactWhyTruthR906(s.truthR906);
            } catch (Throwable whyError) {
                s.truthR906 = RuntimeTruthSnapshotR906.error(whyError.getClass().getSimpleName());
                s.whyTruthText = compactWhyTruthR906(s.truthR906);
            }

            if (!isDecisionForTarget(s.decision, s.targetIssue)) {
                s.decision = null;
            }

            if (s.decision != null
                    && s.decision.targetIssue != null
                    && s.decision.targetIssue.length() > 0) {
                try {
                    s.currentTrade = db.getTradeByTargetIssue(s.decision.targetIssue);
                } catch (Throwable e) {
                    s.currentTrade = null;
                }

                if (!isTradeForTarget(s.currentTrade, s.decision.targetIssue)) {
                    s.currentTrade = null;
                }
            }

            try {
                s.stats = includeDiagnostics ? db.getRuntimeStats() : diagnosticsR71==null ? null : diagnosticsR71.stats;
            } catch (Throwable e) {
                s.stats = null;
            }

            try {
                s.backtest = includeDiagnostics ? db.getLatestBacktestReport() : diagnosticsR71==null ? null : diagnosticsR71.backtest;
            } catch (Throwable e) {
                s.backtest = null;
            }

            try {
                s.manualTask = includeDiagnostics ? db.getLatestManualBacktestTask() : diagnosticsR71==null ? null : diagnosticsR71.manualTask;
            } catch (Throwable e) {
                s.manualTask = null;
            }

            if (s.latestDraw != null && s.latestDraw.isValid()) {
                s.latestDrawNumbersText = drawNumbersText(s.latestDraw);
            }

            s.drawDataProgressText = includeDiagnostics ? db.getDrawDataProgressText() : diagnosticsR71==null ? "历史统计读取中" : diagnosticsR71.drawDataProgressText;
            s.backfillStatusText = db.getBackfillStatusText();
        } catch (Throwable e) {
            s.error = "界面读取状态异常：" + limitText(e.toString(), 120);
        }

        return s;
    }


    private RuntimeTruthSnapshotR906 loadTruthCachedR907(DbStore db, String expectedTarget) {
        long now = TimeSourceR811.monoNowMs();
        String target = expectedTarget == null ? "" : expectedTarget.trim();
        TruthObservationR76 observed=cachedTruthR907;
        if(observed!=null&&target.equals(observed.target)&&now>=observed.sampledMono
                &&now-observed.sampledMono<R907_TRUTH_CACHE_MS)return observed.snapshot;
        RuntimeTruthSnapshotR906 fresh = RuntimeTruthSnapshotR906.captureSummary(db, target);
        cachedTruthR907 = new TruthObservationR76(fresh,target,TimeSourceR811.monoNowMs());
        return fresh;
    }

    private HeavySnapshot loadHeavySnapshotSafely() {
        HeavySnapshot h = new HeavySnapshot();
        h.loadedAt = System.currentTimeMillis();
        h.light = loadLightSnapshotSafely(true);

        DbStore db = readStore == null ? store : readStore;

        if (db == null) {
            return h;
        }

        try {
            Models.Decision decision = h.light == null ? null : h.light.decision;

            try {
                if (reasonEngine != null) {
                    h.reasons = reasonEngine.buildDecisionReasons(decision);
                }
            } catch (Throwable e) {
                h.reasons = null;
            }

            if (h.reasons == null) {
                h.reasons = new ArrayList<Models.ReasonItem>();
            }

            loadRealBetDiagnosisForSnapshot(db, h);
            h.whyTruthText = h.light == null ? "" : compactWhyTruthR906(h.light.truthR906);

            List<Models.Draw> draws = db.getRecentDraws(DRAW_HISTORY_QUERY_LIMIT);
            if (draws != null) {
                h.draws.addAll(draws);
            }

            // R940: preload the bounded REAL ledger once.  The previous per-decision lookup made
            // up to 120 SQLite round trips and amplified DB_LOCK_WAIT during scroll refreshes.
            List<Models.Trade> trades = db.getRecentTrades(DECISION_HISTORY_QUERY_LIMIT);
            if (trades != null) {
                for (int i = 0; i < trades.size(); i++) {
                    Models.Trade trade = trades.get(i);
                    if (trade != null
                            && trade.targetIssue != null
                            && trade.targetIssue.length() > 0
                            && !h.tradeMap.containsKey(trade.targetIssue)) {
                        h.tradeMap.put(trade.targetIssue, trade);
                    }
                }
            }

            List<Models.Decision> decisions = db.getRecentDecisions(DECISION_HISTORY_QUERY_LIMIT);
            if (decisions != null) {
                for (int i = 0; i < decisions.size(); i++) {
                    Models.Decision d = decisions.get(i);

                    if (d == null) {
                        continue;
                    }
                    h.decisions.add(d);
                }
            }
        } catch (Throwable e) {
            h.error = "界面读取历史异常：" + limitText(e.toString(), 120);
        }

        // Publish only after every diagnostic map write; live reader receives a stable snapshot.
        diagnosticSnapshotR71=h.light;
        return h;
    }

    private void loadRealBetDiagnosisForSnapshot(DbStore db, HeavySnapshot h) {
        if (db == null || h == null) {
            return;
        }

        try {
            if (h.light != null && h.light.states != null) {
                loadRealBetDiagnosisStates(db, h.light.states);
            }
            if (AppConfig.ENABLE_V50_PRIME) {
                h.diagnosisSummary = safeState(db, "real_bet_diagnosis_summary");
                h.diagnosisDetail = safeState(db, "real_bet_diagnosis_detail");
                h.diagnosisCopyText =
                        ""; // B04: avoid retaining a duplicate large report in HeavySnapshot.
                h.diagnosisReasons = new ArrayList<Models.ReasonItem>();
                loadFullDiagnosisClosedLoopForSnapshot(db, h);
                return;
            }

            if (reasonEngine != null) {
                try {
                    h.diagnosisReasons = reasonEngine.buildRealBetDiagnosisReasons(db);
                } catch (Throwable e) {
                    h.diagnosisReasons = new ArrayList<Models.ReasonItem>();
                }

                try {
                    h.diagnosisSummary = reasonEngine.buildRealBetDiagnosisSummary(db);
                } catch (Throwable e) {
                    h.diagnosisSummary = "";
                }

                try {
                    h.diagnosisDetail = reasonEngine.buildRealBetDiagnosisDetail(db);
                } catch (Throwable e) {
                    h.diagnosisDetail = "";
                }

                try {
                    h.diagnosisCopyText = reasonEngine.buildRealBetDiagnosisCopyText(db);
                } catch (Throwable e) {
                    h.diagnosisCopyText = "";
                }
            }

            if (h.diagnosisSummary == null || h.diagnosisSummary.length() == 0) {
                h.diagnosisSummary = safeState(db, "real_bet_diagnosis_summary");
            }
            if (h.diagnosisDetail == null || h.diagnosisDetail.length() == 0) {
                h.diagnosisDetail = safeState(db, "real_bet_diagnosis_detail");
            }
            if (h.diagnosisCopyText == null || h.diagnosisCopyText.length() == 0) {
                h.diagnosisCopyText =
                        ""; // B04: avoid retaining a duplicate large report in HeavySnapshot.
            }
            if (h.diagnosisReasons == null) {
                h.diagnosisReasons = new ArrayList<Models.ReasonItem>();
            }
            loadFullDiagnosisClosedLoopForSnapshot(db, h);
        } catch (Throwable e) {
            if (h.diagnosisSummary == null || h.diagnosisSummary.length() == 0) {
                h.diagnosisSummary = "真实BET诊断读取异常：" + limitText(String.valueOf(e), 120);
            }
        }
    }

    private void loadFullDiagnosisClosedLoopForSnapshot(DbStore db, HeavySnapshot h) {
        if (db == null || h == null) {
            return;
        }
        if (AppConfig.ENABLE_V50_PRIME) {
            String summary =
                    firstNonEmpty(
                            safeState(db, "diagnosis_closed_loop_summary"),
                            safeState(db, "real_bet_diagnosis_summary"));
            String detail = firstNonEmpty(safeState(db, "real_bet_diagnosis_detail"), summary);
            h.fullDiagnosisSummary = summary;
            h.moduleEffectText = "";
            h.shadowPromotionText = "";
            h.freezeMatrixText = "";
            h.adaptiveJudgeText = "";
            h.fixPlanText = "";
            h.fullDiagnosisDetail = limitText(detail, UI_FULL_DIAGNOSIS_DETAIL_LIMIT);
            h.fullDiagnosisCopyText =
                    detail.length() == 0
                            ? ""
                            : limitText(
                                    detail + "\n\n红线：UI只显示和复制报告；真实BET仍只认" + "DecisionGateEngine。",
                                    UI_FULL_DIAGNOSIS_COPY_LIMIT);
            return;
        }
        // R940: this is the UI read channel.  Do not create tables or refresh persisted diagnosis
        // state here; those writes belong to the background owner and can block the prediction
        // commit path.  The UI only consumes the last committed snapshot below.
        try {
            Object module = invokeNoArgObject(db, "getModuleEffectCache");
            Object shadow = invokeNoArgObject(db, "getShadowPromotionCache");
            Object freeze = invokeNoArgObject(db, "getDynamicFreezeState");
            Object fix = invokeNoArgObject(db, "getDiagnosisFixPlan");

            String moduleText = contentValuesBrief(module, "模块评分");
            String shadowText = contentValuesBrief(shadow, "SHADOW晋级");
            String freezeText = contentValuesBrief(freeze, "冻结矩阵");
            String fixText = contentValuesBrief(fix, "文件修复建议");

            h.moduleEffectText =
                    firstNonEmpty(
                            safeState(db, "module_effect_detail"),
                            safeState(db, "module_effect_summary"),
                            moduleText);

            h.shadowPromotionText =
                    firstNonEmpty(
                            safeState(db, "shadow_promotion_detail"),
                            safeState(db, "shadow_promotion_summary"),
                            shadowText);

            h.freezeMatrixText =
                    firstNonEmpty(
                            safeState(db, "dynamic_freeze_detail"),
                            safeState(db, "dynamic_freeze_summary"),
                            freezeText);

            h.adaptiveJudgeText =
                    firstNonEmpty(
                            safeState(db, "adaptive_judge_detail"),
                            safeState(db, "adaptive_judge_summary"),
                            buildAdaptiveJudgeStateText(db));

            h.fixPlanText =
                    firstNonEmpty(
                            safeState(db, "diagnosis_fix_plan_detail"),
                            safeState(db, "diagnosis_fix_plan_summary"),
                            fixText);

            StringBuilder copy = new StringBuilder();
            appendFullDiagnosisSection(
                    copy,
                    "满级诊断总览",
                    firstNonEmpty(
                            h.fullDiagnosisSummary,
                            safeState(db, "diagnosis_closed_loop_summary"),
                            h.diagnosisSummary));
            appendFullDiagnosisSection(
                    copy,
                    "真实BET逐单复盘",
                    firstNonEmpty(h.diagnosisCopyText, h.diagnosisDetail, h.diagnosisSummary));
            appendFullDiagnosisSection(copy, "模块归因评分", h.moduleEffectText);
            appendFullDiagnosisSection(copy, "SHADOW反事实晋级", h.shadowPromotionText);
            appendFullDiagnosisSection(copy, "动态冻结矩阵", h.freezeMatrixText);
            appendFullDiagnosisSection(copy, "智能审判结果", h.adaptiveJudgeText);
            appendFullDiagnosisSection(copy, "动态因果窗口诊断", buildDynamicCausalDiagnosisText(db));
            appendFullDiagnosisSection(copy, "文件修复建议", h.fixPlanText);
            if (copy.length() > 0) {
                copy.append(
                        "\n\n"
                            + "红线：UI只显示和复制报告，不生成BET，不救援BET，不改概率，不改门槛，不改结算，真实BET仍只认DecisionGateEngine。");
            }
            h.fullDiagnosisCopyText = limitText(copy.toString(), UI_FULL_DIAGNOSIS_COPY_LIMIT);
            h.fullDiagnosisDetail =
                    buildFullDiagnosisClosedLoopTextFromParts(
                            firstNonEmpty(
                                    h.fullDiagnosisSummary,
                                    safeState(db, "diagnosis_closed_loop_summary")),
                            h.moduleEffectText,
                            h.shadowPromotionText,
                            h.freezeMatrixText,
                            h.adaptiveJudgeText,
                            h.fixPlanText,
                            UI_FULL_DIAGNOSIS_DETAIL_LIMIT);
        } catch (Throwable e) {
            if (h.fullDiagnosisSummary == null || h.fullDiagnosisSummary.length() == 0) {
                h.fullDiagnosisSummary = "满级诊断闭环读取异常：" + limitText(String.valueOf(e), 120);
            }
        }
    }

    private String buildAdaptiveJudgeStateText(DbStore db) {
        if (db == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        String status = safeState(db, "adaptive_judge_status");
        String summary = safeState(db, "adaptive_judge_summary");
        String total = safeState(db, "adaptive_judge_total");
        String pass = safeState(db, "adaptive_judge_pass");
        String watch = safeState(db, "adaptive_judge_watch");
        String downgrade = safeState(db, "adaptive_judge_downgrade");
        String freeze = safeState(db, "adaptive_judge_freeze");
        String fake = safeState(db, "adaptive_judge_fake_strong");
        String promote = safeState(db, "adaptive_judge_promote");
        if (summary.length() > 0) {
            sb.append(summary);
        }
        if (total.length() > 0) {
            if (sb.length() > 0) {
                sb.append("｜");
            }
            sb.append("候选=")
                    .append(total)
                    .append(" PASS=")
                    .append(pass)
                    .append(" WATCH=")
                    .append(watch)
                    .append(" DOWN=")
                    .append(downgrade)
                    .append(" FREEZE=")
                    .append(freeze)
                    .append(" FAKE=")
                    .append(fake)
                    .append(" PROMOTE=")
                    .append(promote);
        }
        if (status.length() > 0) {
            if (sb.length() > 0) {
                sb.append("｜");
            }
            sb.append("状态=").append(status);
        }
        return sb.toString();
    }

    private String buildFullDiagnosisClosedLoopLine(LightSnapshot snapshot) {
        String summary = stateOf(snapshot, "diagnosis_closed_loop_summary");
        String module =
                firstNonEmpty(
                        stateOf(snapshot, "module_effect_summary"),
                        stateOf(snapshot, "module_effect_status"));
        String shadow =
                firstNonEmpty(
                        stateOf(snapshot, "shadow_promotion_summary"),
                        stateOf(snapshot, "shadow_promotion_status"));
        String freeze =
                firstNonEmpty(
                        stateOf(snapshot, "dynamic_freeze_summary"),
                        stateOf(snapshot, "dynamic_freeze_status"));
        String judge =
                firstNonEmpty(
                        stateOf(snapshot, "adaptive_judge_summary"),
                        buildAdaptiveJudgeStateTextFromSnapshot(snapshot));
        String fix =
                firstNonEmpty(
                        stateOf(snapshot, "diagnosis_fix_plan_summary"),
                        stateOf(snapshot, "diagnosis_fix_plan_next_file"));

        if (summary.length() == 0
                && module.length() == 0
                && shadow.length() == 0
                && freeze.length() == 0
                && judge.length() == 0
                && fix.length() == 0) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("满级诊断闭环：");
        if (summary.length() > 0) {
            sb.append(translateLooseText(limitText(summary, 150)));
        }
        if (judge.length() > 0) {
            sb.append("｜审判=");
            sb.append(translateLooseText(limitText(judge, 110)));
        }
        if (freeze.length() > 0) {
            sb.append("｜冻结=");
            sb.append(translateLooseText(limitText(freeze, 90)));
        }
        if (shadow.length() > 0) {
            sb.append("｜SHADOW=");
            sb.append(translateLooseText(limitText(shadow, 90)));
        }
        if (fix.length() > 0) {
            sb.append("｜修复=");
            sb.append(translateLooseText(limitText(fix, 100)));
        }
        return limitText(sb.toString(), UI_FULL_DIAGNOSIS_LINE_LIMIT);
    }

    private String buildAdaptiveJudgeStateTextFromSnapshot(LightSnapshot snapshot) {
        String total = stateOf(snapshot, "adaptive_judge_total");
        if (total.length() == 0) {
            return stateOf(snapshot, "adaptive_judge_status");
        }
        return "候选="
                + total
                + " PASS="
                + stateOf(snapshot, "adaptive_judge_pass")
                + " WATCH="
                + stateOf(snapshot, "adaptive_judge_watch")
                + " DOWN="
                + stateOf(snapshot, "adaptive_judge_downgrade")
                + " FREEZE="
                + stateOf(snapshot, "adaptive_judge_freeze")
                + " PROMOTE="
                + stateOf(snapshot, "adaptive_judge_promote");
    }

    private String buildFullDiagnosisClosedLoopDisplayText(LightSnapshot snapshot, int maxLen) {
        if (snapshot == null) {
            return "";
        }
        if (AppConfig.ENABLE_V50_PRIME) {
            return limitText(
                    firstNonEmpty(
                            stateOf(snapshot, "real_bet_diagnosis_detail"),
                            stateOf(snapshot, "real_bet_diagnosis_summary"),
                            stateOf(snapshot, "diagnosis_closed_loop_summary")),
                    maxLen <= 0 ? UI_FULL_DIAGNOSIS_DETAIL_LIMIT : maxLen);
        }
        String overview = stateOf(snapshot, "diagnosis_closed_loop_summary");
        String module =
                firstNonEmpty(
                        stateOf(snapshot, "module_effect_detail"),
                        stateOf(snapshot, "module_effect_summary"),
                        stateOf(snapshot, "module_effect_status"));
        String shadow =
                firstNonEmpty(
                        stateOf(snapshot, "shadow_promotion_detail"),
                        stateOf(snapshot, "shadow_promotion_summary"),
                        stateOf(snapshot, "shadow_promotion_status"));
        String freeze =
                firstNonEmpty(
                        stateOf(snapshot, "dynamic_freeze_detail"),
                        stateOf(snapshot, "dynamic_freeze_summary"),
                        stateOf(snapshot, "dynamic_freeze_status"));
        String judge =
                firstNonEmpty(
                        stateOf(snapshot, "adaptive_judge_detail"),
                        stateOf(snapshot, "adaptive_judge_summary"),
                        buildAdaptiveJudgeStateTextFromSnapshot(snapshot));
        String fix =
                firstNonEmpty(
                        stateOf(snapshot, "diagnosis_fix_plan_detail"),
                        stateOf(snapshot, "diagnosis_fix_plan_summary"),
                        stateOf(snapshot, "diagnosis_fix_plan_next_file"));
        return buildFullDiagnosisClosedLoopTextFromParts(
                overview, module, shadow, freeze, judge, fix, maxLen);
    }

    private String buildFullDiagnosisClosedLoopTextFromParts(
            String overview,
            String module,
            String shadow,
            String freeze,
            String judge,
            String fix,
            int maxLen) {
        StringBuilder sb = new StringBuilder();
        appendFullDiagnosisSection(sb, "满级诊断总览", overview);
        appendFullDiagnosisSection(sb, "模块归因", module);
        appendFullDiagnosisSection(sb, "SHADOW晋级", shadow);
        appendFullDiagnosisSection(sb, "冻结矩阵", freeze);
        appendFullDiagnosisSection(sb, "智能审判", judge);
        appendFullDiagnosisSection(sb, "文件修复建议", fix);
        String text = sb.toString();
        return limitText(text, maxLen <= 0 ? UI_FULL_DIAGNOSIS_DETAIL_LIMIT : maxLen);
    }

    private void appendFullDiagnosisSection(StringBuilder sb, String title, String text) {
        if (sb == null || text == null || text.length() == 0) {
            return;
        }
        if (sb.length() >= UI_FULL_DIAGNOSIS_COPY_LIMIT) {
            return;
        }
        if (sb.length() > 0) {
            sb.append("\n\n");
        }
        int remain = UI_FULL_DIAGNOSIS_COPY_LIMIT - sb.length() - 64;
        if (remain <= 0) return;
        String safe = limitText(text, remain);
        sb.append("【").append(title).append("】\n");
        sb.append(translateLooseText(safe));
    }

    private void addFullDiagnosisClosedLoopRows(
            HeavySnapshot snapshot, Models.AppRuntimeState state) {
        if (snapshot == null) {
            return;
        }
        if (snapshot.fullDiagnosisDetail == null || snapshot.fullDiagnosisDetail.length() == 0) {
            String text =
                    snapshot.light == null
                            ? ""
                            : buildFullDiagnosisClosedLoopDisplayText(
                                    snapshot.light, UI_FULL_DIAGNOSIS_DETAIL_LIMIT);
            snapshot.fullDiagnosisDetail = text;
        }
        if (snapshot.fullDiagnosisDetail == null || snapshot.fullDiagnosisDetail.length() == 0) {
            return;
        }
        Models.ReasonItem item = new Models.ReasonItem();
        item.level = AppConfig.REASON_INFO;
        item.code = "V20_FULL_DIAGNOSIS_JUDGE_UI";
        item.title = "满级诊断 + 智能审判闭环";
        item.detail =
                translateLooseText(
                        limitText(snapshot.fullDiagnosisDetail, UI_FULL_DIAGNOSIS_DETAIL_LIMIT));
        item.value = 0.0d;
        item.passed = 1;
        item.createdAt = System.currentTimeMillis();
        addReasonRow(item, state, snapshot.light);
    }

    private String buildDynamicCausalRealtimeLine(LightSnapshot snapshot) {
        String realtime =
                firstNonEmpty(
                        stateOf(snapshot, "dynamic_causal_realtime"),
                        stateOf(snapshot, "last_dynamic_causal_summary"),
                        stateOf(snapshot, "dynamic_causal_summary"),
                        stateOf(snapshot, "dynamic_causal_diagnosis"));
        String need = stateOf(snapshot, "dynamic_need_supply_realtime");
        String cluster = stateOf(snapshot, "dynamic_window_cluster_realtime");
        String cf = stateOf(snapshot, "counterfactual_reason_realtime");
        String promo = stateOf(snapshot, "shadow_promotion_dynamic_refresh");
        String judge = stateOf(snapshot, "adaptive_judge_realtime");
        String cost = stateOf(snapshot, "dynamic_causal_realtime_cost_ms");
        String error = stateOf(snapshot, "dynamic_causal_realtime_error");
        if (realtime.length() == 0
                && need.length() == 0
                && cluster.length() == 0
                && cf.length() == 0
                && promo.length() == 0
                && judge.length() == 0
                && error.length() == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("动态因果实时链：");
        if (realtime.length() > 0) {
            sb.append(translateLooseText(limitText(realtime, 180)));
        } else {
            sb.append("已接入");
        }
        appendInline(sb, "NeedSupply", need, 90);
        appendInline(sb, "动态簇", cluster, 90);
        appendInline(sb, "反事实", cf, 90);
        appendInline(sb, "晋级", promo, 80);
        appendInline(sb, "审判", judge, 90);
        if (cost.length() > 0) {
            sb.append("｜耗时=").append(cost).append("ms");
        }
        if (error.length() > 0) {
            sb.append("｜异常=").append(translateLooseText(limitText(error, 120)));
        }
        return limitText(sb.toString(), UI_DYNAMIC_CAUSAL_LINE_LIMIT);
    }

    private String buildDynamicCausalDiagnosisLine(LightSnapshot snapshot) {
        String summary =
                firstNonEmpty(
                        stateOf(snapshot, "real_bet_dynamic_causal_summary"),
                        stateOf(snapshot, "dynamic_causal_decision_summary"),
                        stateOf(snapshot, "final_decision_dynamic_causal_summary"),
                        stateOf(snapshot, "last_decision_dynamic_causal_summary"));
        String ready = stateOf(snapshot, "real_bet_dynamic_causal_ready_rate");
        String shadowReady = stateOf(snapshot, "real_bet_dynamic_causal_shadow_ready_rate");
        String bad = stateOf(snapshot, "real_bet_dynamic_causal_bad_total");
        String main = stateOf(snapshot, "real_bet_dynamic_causal_main_problem");
        String next = stateOf(snapshot, "real_bet_dynamic_causal_next_fix");
        if (summary.length() == 0
                && ready.length() == 0
                && shadowReady.length() == 0
                && bad.length() == 0
                && main.length() == 0
                && next.length() == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("动态因果诊断：");
        if (summary.length() > 0) {
            sb.append(translateLooseText(limitText(summary, 180)));
        }
        if (ready.length() > 0) {
            sb.append("｜真实READY=").append(formatMaybeProbability(ready));
        }
        if (shadowReady.length() > 0) {
            sb.append("｜影子READY=").append(formatMaybeProbability(shadowReady));
        }
        if (bad.length() > 0) {
            sb.append("｜坏单=").append(bad);
        }
        if (main.length() > 0) {
            sb.append("｜问题=").append(translateLooseText(limitText(main, 120)));
        }
        if (next.length() > 0) {
            sb.append("｜建议=").append(translateLooseText(limitText(next, 120)));
        }
        return limitText(sb.toString(), UI_DYNAMIC_CAUSAL_LINE_LIMIT);
    }

    private String buildDynamicCausalDiagnosisDisplayText(LightSnapshot snapshot, int maxLen) {
        String detail = stateOf(snapshot, "real_bet_dynamic_causal_detail");
        String line = buildDynamicCausalDiagnosisLine(snapshot);
        String clusterBad = stateOf(snapshot, "real_bet_dynamic_causal_cluster_bad");
        String needBad = stateOf(snapshot, "real_bet_dynamic_causal_need_supply_bad");
        String cfBad = stateOf(snapshot, "real_bet_dynamic_causal_counterfactual_bad");
        String adaptiveBad = stateOf(snapshot, "real_bet_dynamic_causal_adaptive_freeze_bet");
        String promotionBad = stateOf(snapshot, "real_bet_dynamic_causal_promotion_freeze_bet");
        String hardRisk = stateOf(snapshot, "real_bet_dynamic_causal_decision_hard_risk_bet");
        StringBuilder sb = new StringBuilder();
        if (line.length() > 0) appendLine(sb, line);
        if (detail.length() > 0 && !detail.equals(line)) appendLine(sb, translateLooseText(detail));
        String matrix = "";
        if (clusterBad.length() > 0) matrix = appendReason(matrix, "动态簇坏单=" + clusterBad);
        if (needBad.length() > 0) matrix = appendReason(matrix, "NeedSupply坏单=" + needBad);
        if (cfBad.length() > 0) matrix = appendReason(matrix, "反事实坏单=" + cfBad);
        if (adaptiveBad.length() > 0)
            matrix = appendReason(matrix, "Adaptive冻结仍BET=" + adaptiveBad);
        if (promotionBad.length() > 0)
            matrix = appendReason(matrix, "Promotion冻结仍BET=" + promotionBad);
        if (hardRisk.length() > 0) matrix = appendReason(matrix, "动态硬风险仍BET=" + hardRisk);
        if (matrix.length() > 0) {
            appendLine(sb, "问题矩阵：" + matrix);
        }
        return limitText(sb.toString(), maxLen <= 0 ? UI_DYNAMIC_CAUSAL_DETAIL_LIMIT : maxLen);
    }

    private String buildDynamicCausalDiagnosisText(DbStore db) {
        if (db == null) return "";
        if (AppConfig.ENABLE_V50_PRIME) return "";
        HashMap<String, String> map = new HashMap<String, String>();
        try {
            loadRealBetDiagnosisStates(db, map);
        } catch (Throwable e) {
        }
        try {
            putState(db, map, "dynamic_causal_realtime");
            putState(db, map, "dynamic_need_supply_realtime");
            putState(db, map, "dynamic_window_cluster_realtime");
            putState(db, map, "counterfactual_reason_realtime");
            putState(db, map, "shadow_promotion_dynamic_refresh");
            putState(db, map, "adaptive_judge_realtime");
        } catch (Throwable e) {
        }
        LightSnapshot s = new LightSnapshot();
        s.states = map;
        String text = buildDynamicCausalDiagnosisDisplayText(s, UI_DYNAMIC_CAUSAL_COPY_LIMIT);
        String realtime = buildDynamicCausalRealtimeLine(s);
        if (realtime.length() > 0) {
            text = appendReason(text, realtime);
        }
        return text;
    }

    private void addDynamicCausalDiagnosisRows(
            HeavySnapshot snapshot, Models.AppRuntimeState state) {
        if (snapshot == null) return;
        if (AppConfig.ENABLE_V50_PRIME) return;
        String text =
                snapshot.light == null
                        ? ""
                        : buildDynamicCausalDiagnosisDisplayText(
                                snapshot.light, UI_DYNAMIC_CAUSAL_DETAIL_LIMIT);
        if (text.length() == 0) {
            return;
        }
        Models.ReasonItem item = new Models.ReasonItem();
        item.level = AppConfig.REASON_INFO;
        item.code = "V20_DYNAMIC_CAUSAL_DIAGNOSIS_UI";
        item.title = "动态因果窗口诊断";
        item.detail = translateLooseText(limitText(text, UI_DYNAMIC_CAUSAL_DETAIL_LIMIT));
        item.value = 0.0d;
        item.passed = 1;
        item.createdAt = System.currentTimeMillis();
        addReasonRow(item, state, snapshot.light);
    }

    private String buildDynamicCausalDecisionLine(Object decision, LightSnapshot snapshot) {
        String summary =
                firstNonEmpty(
                        getFieldText(decision, "dynamicCausalDecisionSummary"),
                        getFieldText(decision, "finalDecisionDynamicCausalSummary"),
                        getFieldText(decision, "dynamicCausalSummary"),
                        stateOf(snapshot, "final_decision_dynamic_causal_summary"),
                        stateOf(snapshot, "dynamic_causal_decision_summary"));
        String clusterId =
                firstNonEmpty(
                        getFieldText(decision, "dynamicClusterId"),
                        stateOf(snapshot, "dynamic_cluster_id"));
        String clusterStatus =
                firstNonEmpty(
                        getFieldText(decision, "dynamicClusterStatus"),
                        stateOf(snapshot, "dynamic_cluster_status"));
        double clusterHit =
                firstPositiveDouble(
                        readDouble(decision, "dynamicClusterHitRate", -999.0d),
                        parseDouble(stateOf(snapshot, "dynamic_cluster_hit_rate"), -999.0d));
        int clusterSample =
                firstPositiveInt(
                        readInt(decision, "dynamicClusterSample", 0),
                        parseIntText(stateOf(snapshot, "dynamic_cluster_sample"), 0));
        double needScore =
                firstPositiveDouble(
                        readDouble(decision, "needSupplyMatchScore", -999.0d),
                        parseDouble(stateOf(snapshot, "need_supply_match_score"), -999.0d));
        double third =
                firstPositiveDouble(
                        readDouble(decision, "needSupplyThirdQuality", -999.0d),
                        parseDouble(stateOf(snapshot, "need_supply_third_quality"), -999.0d));
        double needRisk =
                firstPositiveDouble(
                        readDouble(decision, "needSupplyRisk", -999.0d),
                        parseDouble(stateOf(snapshot, "need_supply_risk"), -999.0d));
        double whyNot =
                firstPositiveDouble(
                        readDouble(decision, "counterfactualWhyNotRisk", -999.0d),
                        parseDouble(stateOf(snapshot, "counterfactual_why_not_risk"), -999.0d));
        String adaptive =
                firstNonEmpty(
                        getFieldText(decision, "adaptiveJudgeAction"),
                        stateOf(snapshot, "adaptive_judge_action"));
        String promotion =
                firstNonEmpty(
                        getFieldText(decision, "promotionClusterStatus"),
                        stateOf(snapshot, "promotion_cluster_status"));
        double q = readDouble(decision, "dynamicCausalDecisionQuality", -999.0d);
        double r = readDouble(decision, "dynamicCausalDecisionRisk", -999.0d);
        String block =
                firstNonEmpty(
                        getFieldText(decision, "dynamicCausalDecisionBlockCode"),
                        stateOf(snapshot, "dynamic_causal_decision_block_code"));
        if (summary.length() == 0
                && clusterId.length() == 0
                && clusterStatus.length() == 0
                && needScore <= -998.0d
                && whyNot <= -998.0d
                && adaptive.length() == 0
                && promotion.length() == 0
                && q <= -998.0d
                && r <= -998.0d) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("动态因果最终门：");
        if (summary.length() > 0) {
            sb.append(translateLooseText(limitText(summary, 180)));
        } else {
            sb.append("已接入");
        }
        if (clusterStatus.length() > 0 || clusterId.length() > 0) {
            sb.append("｜簇=")
                    .append(
                            translateLooseText(
                                    shortBucket(firstNonEmpty(clusterStatus, clusterId))));
        }
        if (clusterHit > -998.0d) sb.append("/命中=").append(UiFormatter.probability(clusterHit));
        if (clusterSample > 0) sb.append("/样本=").append(clusterSample);
        if (needScore > -998.0d)
            sb.append("｜NeedSupply=").append(UiFormatter.probability(needScore));
        if (third > -998.0d) sb.append("/第三=").append(UiFormatter.probability(third));
        if (needRisk > -998.0d) sb.append("/风险=").append(UiFormatter.probability(needRisk));
        if (whyNot > -998.0d) sb.append("｜反证=").append(UiFormatter.probability(whyNot));
        if (adaptive.length() > 0)
            sb.append("｜审判=").append(translateLooseText(shortBucket(adaptive)));
        if (promotion.length() > 0)
            sb.append("｜晋级=").append(translateLooseText(shortBucket(promotion)));
        if (q > -998.0d) sb.append("｜质量=").append(UiFormatter.probability(q));
        if (r > -998.0d) sb.append("｜总风险=").append(UiFormatter.probability(r));
        if (block.length() > 0 && !"NONE".equals(block))
            sb.append("｜拦截=").append(translateLooseText(shortBucket(block)));
        return limitText(sb.toString(), UI_DYNAMIC_CAUSAL_LINE_LIMIT);
    }

    private String buildDynamicCausalMiniText(Object decision) {
        if (decision == null) return "";
        double need = readDouble(decision, "needSupplyMatchScore", -999.0d);
        double third = readDouble(decision, "needSupplyThirdQuality", -999.0d);
        double cf = readDouble(decision, "counterfactualWhyNotRisk", -999.0d);
        String action = getFieldText(decision, "adaptiveJudgeAction");
        String status = getFieldText(decision, "dynamicClusterStatus");
        StringBuilder sb = new StringBuilder();
        if (status.length() > 0) sb.append("动态簇=").append(translateLooseText(shortBucket(status)));
        if (need > -998.0d) {
            if (sb.length() > 0) sb.append("｜");
            sb.append("需供=").append(UiFormatter.probability(need));
        }
        if (third > -998.0d) {
            if (sb.length() > 0) sb.append("｜");
            sb.append("第三=").append(UiFormatter.probability(third));
        }
        if (cf > -998.0d) {
            if (sb.length() > 0) sb.append("｜");
            sb.append("反证=").append(UiFormatter.probability(cf));
        }
        if (action.length() > 0) {
            if (sb.length() > 0) sb.append("｜");
            sb.append("审判=").append(translateLooseText(shortBucket(action)));
        }
        return limitText(sb.toString(), 180);
    }

    private String buildBroadcastDynamicCausalText(Intent intent) {
        if (intent == null) return "";
        String summary =
                firstIntentString(
                        intent,
                        new String[] {
                            "dynamic_causal_summary",
                            "dynamic_causal_decision_summary",
                            "final_decision_dynamic_causal_summary"
                        },
                        null);
        String cluster =
                firstIntentString(
                        intent,
                        new String[] {
                            "dynamic_cluster_status",
                            "dynamicClusterStatus",
                            "dynamic_cluster_id",
                            "dynamicClusterId"
                        },
                        null);
        double need =
                firstIntentDouble(
                        intent,
                        new String[] {"need_supply_match_score", "needSupplyMatchScore"},
                        null,
                        -999.0d);
        double third =
                firstIntentDouble(
                        intent,
                        new String[] {"need_supply_third_quality", "needSupplyThirdQuality"},
                        null,
                        -999.0d);
        double whyNot =
                firstIntentDouble(
                        intent,
                        new String[] {"counterfactual_why_not_risk", "counterfactualWhyNotRisk"},
                        null,
                        -999.0d);
        String action =
                firstIntentString(
                        intent,
                        new String[] {"adaptive_judge_action", "adaptiveJudgeAction"},
                        null);
        if (summary.length() == 0
                && cluster.length() == 0
                && need <= -998.0d
                && third <= -998.0d
                && whyNot <= -998.0d
                && action.length() == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("动态因果：");
        if (summary.length() > 0) sb.append(translateLooseText(limitText(summary, 140)));
        if (cluster.length() > 0) sb.append("｜簇=").append(translateLooseText(shortBucket(cluster)));
        if (need > -998.0d) sb.append("｜需供=").append(UiFormatter.probability(need));
        if (third > -998.0d) sb.append("｜第三=").append(UiFormatter.probability(third));
        if (whyNot > -998.0d) sb.append("｜反证=").append(UiFormatter.probability(whyNot));
        if (action.length() > 0) sb.append("｜审判=").append(translateLooseText(shortBucket(action)));
        return limitText(sb.toString(), UI_DYNAMIC_CAUSAL_LINE_LIMIT);
    }

    private String buildDynamicCausalStateKey(LightSnapshot snapshot) {
        if (snapshot == null) return "";
        StringBuilder sb = new StringBuilder();
        sb.append(stateOf(snapshot, "dynamic_causal_realtime")).append("|");
        sb.append(stateOf(snapshot, "dynamic_need_supply_realtime")).append("|");
        sb.append(stateOf(snapshot, "dynamic_window_cluster_realtime")).append("|");
        sb.append(stateOf(snapshot, "counterfactual_reason_realtime")).append("|");
        sb.append(stateOf(snapshot, "shadow_promotion_dynamic_refresh")).append("|");
        sb.append(stateOf(snapshot, "adaptive_judge_realtime")).append("|");
        sb.append(stateOf(snapshot, "real_bet_dynamic_causal_summary")).append("|");
        sb.append(stateOf(snapshot, "real_bet_dynamic_causal_ready_rate")).append("|");
        sb.append(stateOf(snapshot, "real_bet_dynamic_causal_shadow_ready_rate")).append("|");
        sb.append(stateOf(snapshot, "real_bet_dynamic_causal_bad_total")).append("|");
        sb.append(stateOf(snapshot, "real_bet_dynamic_causal_main_problem")).append("|");
        sb.append(stateOf(snapshot, "final_decision_dynamic_causal_summary")).append("|");
        sb.append(stateOf(snapshot, "dynamic_causal_decision_summary")).append("|");
        sb.append(stateOf(snapshot, "adaptive_judge_summary")).append("|");
        sb.append(stateOf(snapshot, "shadow_promotion_dynamic_summary"));
        return sb.toString();
    }

    private double firstPositiveDouble(double a, double b) {
        if (a > -998.0d) return a;
        return b;
    }

    private int firstPositiveInt(int a, int b) {
        if (a > 0) {
            return a;
        }
        return b;
    }

    private int parseIntText(String s, int def) {
        try {
            if (s == null || s.length() == 0) return def;
            return Integer.parseInt(s);
        } catch (Throwable e) {
            return def;
        }
    }

    private String shortBucket(String s) {
        if (s == null) return "";
        String v = s;
        if (v.length() > 48) {
            return v.substring(0, 48);
        }
        return v;
    }

    private String contentValuesBrief(Object obj, String title) {
        if (obj == null) {
            return "";
        }
        String summary =
                firstNonEmpty(
                        getContentValueString(obj, "summary_text"),
                        getContentValueString(obj, "summary"),
                        getContentValueString(obj, "detail_text"));
        String status =
                firstNonEmpty(
                        getContentValueString(obj, "effect_status"),
                        getContentValueString(obj, "promotion_status"),
                        getContentValueString(obj, "freeze_level"),
                        getContentValueString(obj, "judge_status"),
                        getContentValueString(obj, "status"));
        String key =
                firstNonEmpty(
                        getContentValueString(obj, "module_name"),
                        getContentValueString(obj, "domain_key"),
                        getContentValueString(obj, "freeze_key"),
                        getContentValueString(obj, "file_name"),
                        getContentValueString(obj, "record_key"));
        if (summary.length() == 0 && status.length() == 0 && key.length() == 0) {
            String s = String.valueOf(obj);
            if (s == null || s.length() == 0 || "null".equals(s)) {
                return "";
            }
            return limitText(title + "：" + s, 420);
        }
        StringBuilder sb = new StringBuilder();
        sb.append(title).append("：");
        if (key.length() > 0) {
            sb.append(key);
        }
        if (status.length() > 0) {
            if (key.length() > 0) {
                sb.append("｜");
            }
            sb.append(status);
        }
        if (summary.length() > 0) {
            sb.append("｜");
            sb.append(summary);
        }
        return limitText(sb.toString(), 600);
    }

    private String getContentValueString(Object obj, String key) {
        if (obj == null || key == null || key.length() == 0) {
            return "";
        }
        try {
            java.lang.reflect.Method m =
                    obj.getClass().getMethod("getAsString", new Class[] {String.class});
            Object v = m.invoke(obj, new Object[] {key});
            return v == null ? "" : String.valueOf(v);
        } catch (Throwable e) {
        }
        try {
            java.lang.reflect.Method m =
                    obj.getClass().getMethod("get", new Class[] {String.class});
            Object v = m.invoke(obj, new Object[] {key});
            return v == null ? "" : String.valueOf(v);
        } catch (Throwable e) {
        }
        return getFieldText(obj, key);
    }

    private Object invokeNoArgObject(Object obj, String methodName) {
        if (obj == null || methodName == null || methodName.length() == 0) {
            return null;
        }
        try {
            java.lang.reflect.Method m = obj.getClass().getMethod(methodName, new Class[] {});
            return m.invoke(obj, new Object[] {});
        } catch (Throwable e) {
            return null;
        }
    }

    private String invokeNoArgString(Object obj, String methodName) {
        Object v = invokeNoArgObject(obj, methodName);
        return v == null ? "" : String.valueOf(v);
    }

    private void loadRealBetDiagnosisStates(DbStore db, HashMap<String, String> map) {
        if (db == null || map == null) {
            return;
        }
        putState(db, map, "real_bet_diagnosis_status");
        putState(db, map, "real_bet_diagnosis_version");
        putState(db, map, "v48_decision_funnel_detail");
        putState(db, map, "v48_ledger_summary");
        putState(db, map, "v48_champion_summary");
        putState(db, map, "v48_runtime_freshness");

        putState(db, map, "real_bet_diagnosis_updated_at");
        putState(db, map, "real_bet_diagnosis_last_refresh_at");
        putState(db, map, "real_bet_diagnosis_summary");
        putState(db, map, "real_bet_diagnosis_detail");
        putState(db, map, "real_bet_diagnosis_total");
        putState(db, map, "real_bet_diagnosis_hit");
        putState(db, map, "real_bet_diagnosis_miss");
        putState(db, map, "real_bet_diagnosis_win_rate");
        putState(db, map, "real_bet_diagnosis_pnl");
        putState(db, map, "real_bet_diagnosis_shadow_total");
        putState(db, map, "real_bet_diagnosis_shadow_hit");
        putState(db, map, "real_bet_diagnosis_shadow_rate");
        putState(db, map, "real_bet_diagnosis_main_problem");
        putState(db, map, "real_bet_diagnosis_next_fix");
        putState(db, map, "real_bet_dynamic_causal_summary");
        putState(db, map, "real_bet_dynamic_causal_detail");
        putState(db, map, "real_bet_dynamic_causal_total");
        putState(db, map, "real_bet_dynamic_causal_ready_total");
        putState(db, map, "real_bet_dynamic_causal_ready_hit");
        putState(db, map, "real_bet_dynamic_causal_ready_rate");
        putState(db, map, "real_bet_dynamic_causal_shadow_ready_total");
        putState(db, map, "real_bet_dynamic_causal_shadow_ready_hit");
        putState(db, map, "real_bet_dynamic_causal_shadow_ready_rate");
        putState(db, map, "real_bet_dynamic_causal_bad_total");
        putState(db, map, "real_bet_dynamic_causal_main_problem");
        putState(db, map, "real_bet_dynamic_causal_next_fix");
        putState(db, map, "real_bet_dynamic_causal_cluster_bad");
        putState(db, map, "real_bet_dynamic_causal_need_supply_bad");
        putState(db, map, "real_bet_dynamic_causal_counterfactual_bad");
        putState(db, map, "real_bet_dynamic_causal_adaptive_freeze_bet");
        putState(db, map, "real_bet_dynamic_causal_promotion_freeze_bet");
        putState(db, map, "real_bet_dynamic_causal_decision_hard_risk_bet");
        putState(db, map, "real_bet_diagnosis_link_v2_version");
        putState(db, map, "real_bet_diagnosis_freq50");
        putState(db, map, "real_bet_diagnosis_freq100");
        putState(db, map, "real_bet_diagnosis_freq300");
        putState(db, map, "real_bet_diagnosis_frequency_debt");
        putState(db, map, "real_bet_diagnosis_frequency_state");
        putState(db, map, "real_bet_diagnosis_probe_frequency");
        putState(db, map, "real_bet_diagnosis_qualified_opportunities");
        putState(db, map, "real_bet_diagnosis_normal_bet_count");
        putState(db, map, "real_bet_diagnosis_freq_recovery_bet_count");
        putState(db, map, "real_bet_diagnosis_real_probe_bet_count");
        putState(db, map, "real_bet_diagnosis_promotion_watch_domains");
        putState(db, map, "real_bet_diagnosis_promotion_ready_domains");
        putState(db, map, "real_bet_diagnosis_promotion_strong_domains");
        putState(db, map, "real_bet_diagnosis_promotion_block_domains");
        putState(db, map, "real_bet_diagnosis_snapshot_freshness");
        putState(db, map, "real_bet_diagnosis_performance_summary");
        putState(db, map, "v24_realtime_probability_best");
        putState(db, map, "v24_realtime_probability_policy");
        putState(db, map, MicroWalkForwardEngine.STATE_LAST_SUMMARY);
        putState(db, map, MicroWalkForwardEngine.STATE_STATUS);
        putState(db, map, MicroWalkForwardEngine.STATE_SAMPLE);
        putState(db, map, "v24_authorization_last_mode");
        putState(db, map, "v24_authorization_last_reject");
        putState(db, map, "v24_authorization_last_probability");
        putState(db, map, "v24_authorization_last_gate");
        putState(db, map, "v24_authorization_last_edge");
        putState(db, map, "real_bet_diagnosis_v24_unauthorized_bet");
        putState(db, map, "real_bet_diagnosis_v24_watch_bet");
        putState(db, map, "real_bet_diagnosis_v24_hard_risk_bet");
        putState(db, map, "real_bet_diagnosis_v24_version_mismatch_bet");
    }

    private String safeState(DbStore db, String key) {
        if (db == null || key == null || key.length() == 0) {
            return "";
        }
        try {
            return db.getState(key, "");
        } catch (Throwable e) {
            return "";
        }
    }

    private void loadCommonStates(DbStore db, HashMap<String, String> map) {
        if (db == null || map == null) {
            return;
        }

        // Normal R816 path: a single bounded SQLite cursor. The explicit key-by-key code below is
        // retained as a fail-safe for a damaged/partially migrated app_state table.
        if (db.readUiStateSnapshotR816(map, 2048, 4096)) return;

        putState(db, map, "service_status");
        putState(db, map, "service_last_action");
        putState(db, map, "service_core_summary");

        putState(db, map, "current_issue");
        putState(db, map, "target_issue");
        putState(db, map, "api_line");

        putState(db, map, "fetch_status");
        putState(db, map, "fetch_count");
        putState(db, map, "fetch_summary");
        putState(db, map, "last_fetch_at");
        putState(db, map, "open_fetch_try");

        putState(db, map, "analysis_status");
        putState(db, map, "analysis_target_issue");
        putState(db, map, "analysis_progress");
        putState(db, map, "analysis_summary");
        putState(db, map, "analysis_cost_ms");
        putState(db, map, "analysis_cost_summary");
        putState(db, map, "analysis_done_target_issue");
        putState(db, map, "analysis_last_decision");
        putState(db, map, "analysis_last_summary");
        putState(db, map, "r365_last_analysis_failure");
        putState(db, map, "r365_last_analysis_failure_target");
        putState(db, map, "r365_last_analysis_transition");
        putState(db, map, "analysis_execution_target");
        putState(db, map, "analysis_execution_stage");

        putState(db, map, "last_decision");
        putState(db, map, "last_decision_target_issue");
        putState(db, map, "last_decision_summary");

        putState(db, map, "last_decision_final_p");
        putState(db, map, "last_decision_gate");
        putState(db, map, "last_decision_edge");
        putState(db, map, "last_decision_block");
        putState(db, map, "last_decision_scope");
        putState(db, map, "last_decision_position");
        putState(db, map, "last_decision_numbers");
        putState(db, map, "frequency_state_status");
        putState(db, map, "frequency_state_freq50");
        putState(db, map, "frequency_state_freq100");
        putState(db, map, "frequency_state_freq300");
        putState(db, map, "frequency_state_debt");
        putState(db, map, "frequency_state_policy");
        putState(db, map, "realtime_layered_decision_cost_ms");
        putState(db, map, "adaptive_judge_elapsed_ms");
        putState(db, map, "adaptive_judge_deferred");
        putState(db, map, "rule_health_realtime_elapsed_ms");
        putState(db, map, "rule_health_realtime_freshness");
        putState(db, map, "third_guard_matrix_summary");
        putState(db, map, "shadow_promotion_candidate_summary");

        putState(db, map, "backtest_status");
        putState(db, map, "backtest_mode");
        putState(db, map, "backtest_cost_ms");
        putState(db, map, "backtest_progress");
        putState(db, map, "backtest_window");
        putState(db, map, "backtest_error");
        putState(db, map, "backtest_last_summary");
        putState(db, map, "backtest_reason");

        putState(db, map, "manual_backtest_status");
        putState(db, map, "manual_backtest_progress");
        putState(db, map, "manual_backtest_summary");

        putState(db, map, "backfill_status");
        putState(db, map, "backfill_message");
        putState(db, map, "backfill_inserted");

        putState(db, map, "learning_status");
        putState(db, map, "learning_summary");
        putState(db, map, "learning_reason_detail");
        putState(db, map, "learning_state_key");
        putState(db, map, "learning_expected_shape");
        putState(db, map, "learning_actual_shape");
        putState(db, map, "learning_next_advice");
        putState(db, map, "learning_top_miss_reason");

        putState(db, map, "auto_learn_status");
        putState(db, map, "auto_learn_running");
        putState(db, map, "auto_learn_progress");
        putState(db, map, "auto_learn_summary");
        putState(db, map, "auto_learn_range");
        putState(db, map, "auto_learn_updated_at");

        putState(db, map, "teacher_matrix_status");
        putState(db, map, "teacher_matrix_running");
        putState(db, map, "teacher_matrix_summary");
        putState(db, map, "teacher_matrix_issue");
        putState(db, map, "teacher_matrix_last_issue");
        putState(db, map, "teacher_matrix_sample_count");
        putState(db, map, "teacher_matrix_profile_count");
        putState(db, map, "teacher_matrix_strong_profile_count");
        putState(db, map, "teacher_matrix_risk_profile_count");
        putState(db, map, "teacher_matrix_last_cells");
        putState(db, map, "teacher_matrix_last_positive");
        putState(db, map, "teacher_matrix_last_negative");
        putState(db, map, "teacher_matrix_last_combo_hit");
        putState(db, map, "teacher_matrix_last_case_saved");
        putState(db, map, "teacher_matrix_last_matrix_saved");
        putState(db, map, "teacher_matrix_last_cost_ms");
        putState(db, map, "teacher_matrix_updated_at");

        putState(db, map, "teacher_snapshot_status");
        putState(db, map, "teacher_snapshot_running");
        putState(db, map, "teacher_snapshot_id");
        putState(db, map, "teacher_snapshot_issue");
        putState(db, map, "teacher_snapshot_summary");
        putState(db, map, "teacher_snapshot_confidence");
        putState(db, map, "teacher_snapshot_updated_at");
        putState(db, map, "teacher_snapshot_valid");
        putState(db, map, "teacher_snapshot_valid_until");
        putState(db, map, "teacher_confidence");
        putState(db, map, "teacher_weight");

        putState(db, map, "runtime_profile_summary");
        putState(db, map, "runtime_learning_snapshot_id");
        putState(db, map, "runtime_snapshot_id");
        putState(db, map, "last_snapshot_id");
        putState(db, map, "lastSnapshotId");
        putState(db, map, "deep_snapshot_id");

        putState(db, map, "settlement_status");
        putState(db, map, "settlement_summary");
        putState(db, map, "settlement_detail");
        putState(db, map, "settlement_result");
        putState(db, map, "settlement_pnl");
        putState(db, map, "settlement_win_count");
        putState(db, map, "settlement_miss_count");
        putState(db, map, "last_settlement_detail");
        putState(db, map, "last_strict_settlement_detail");
        putState(db, map, "last_strict_trade_settlement_detail");
        putState(db, map, "settlement_catchup_summary");

        putState(db, map, "real_bet_diagnosis_status");
        putState(db, map, "real_bet_diagnosis_version");
        putState(db, map, "real_bet_diagnosis_updated_at");
        putState(db, map, "real_bet_diagnosis_last_refresh_at");
        putState(db, map, "real_bet_diagnosis_summary");
        putState(db, map, "real_bet_diagnosis_detail");
        putState(db, map, "real_bet_diagnosis_total");
        putState(db, map, "real_bet_diagnosis_hit");
        putState(db, map, "real_bet_diagnosis_miss");
        putState(db, map, "real_bet_diagnosis_win_rate");
        putState(db, map, "real_bet_diagnosis_pnl");
        putState(db, map, "real_bet_diagnosis_shadow_total");
        putState(db, map, "real_bet_diagnosis_shadow_hit");
        putState(db, map, "real_bet_diagnosis_shadow_rate");
        putState(db, map, "real_bet_diagnosis_main_problem");
        putState(db, map, "real_bet_diagnosis_next_fix");

        putState(db, map, "v20_cause_chain_status");
        putState(db, map, "v20_cause_chain_running");
        putState(db, map, "v20_cause_chain_summary");
        putState(db, map, "v20_cause_chain_issue");
        putState(db, map, "v20_cause_chain_last_issue");
        putState(db, map, "v20_cause_chain_updated_at");
        putState(db, map, "v20_cause_chain_cost_ms");
        putState(db, map, "v20_cause_chain_sample_count");
        putState(db, map, "v20_cause_chain_profile_count");

        putState(db, map, "dynamic_cause_chain_status");
        putState(db, map, "dynamic_cause_chain_summary");
        putState(db, map, "dynamic_cause_chain_score");
        putState(db, map, "dynamic_cause_chain_risk");
        putState(db, map, "dynamic_cause_chain_sample");

        putState(db, map, "causal_factor_matrix_status");
        putState(db, map, "causal_factor_matrix_summary");
        putState(db, map, "causal_factor_matrix_score");
        putState(db, map, "causal_factor_matrix_risk");
        putState(db, map, "causal_factor_matrix_sample");

        putState(db, map, "position_cause_matrix_status");
        putState(db, map, "position_cause_matrix_summary");
        putState(db, map, "position_cause_matrix_score");
        putState(db, map, "position_cause_matrix_risk");
        putState(db, map, "position_cause_matrix_sample");

        putState(db, map, "teacher_number_matrix_status");
        putState(db, map, "teacher_number_matrix_summary");
        putState(db, map, "teacher_number_matrix_score");
        putState(db, map, "teacher_number_matrix_risk");
        putState(db, map, "teacher_number_matrix_sample");

        putState(db, map, "counterfactual_rank_status");
        putState(db, map, "counterfactual_rank_summary");
        putState(db, map, "counterfactual_rank_score");
        putState(db, map, "counterfactual_rank_risk");
        putState(db, map, "counterfactual_rank_sample");

        putState(db, map, "third_guard_matrix_status");
        putState(db, map, "third_guard_matrix_summary");
        putState(db, map, "third_guard_matrix_score");
        putState(db, map, "third_guard_matrix_risk");
        putState(db, map, "third_guard_matrix_sample");

        putState(db, map, "rule_health_matrix_status");
        putState(db, map, "rule_health_matrix_summary");
        putState(db, map, "rule_health_matrix_score");
        putState(db, map, "rule_health_matrix_risk");
        putState(db, map, "rule_health_matrix_sample");

        putState(db, map, "shadow_health_gate_status");
        putState(db, map, "shadow_health_gate_summary");
        putState(db, map, "shadow_health_gate_score");
        putState(db, map, "shadow_health_gate_risk");
        putState(db, map, "shadow_health_gate_sample");

        putState(db, map, "false_strong_freeze_status");
        putState(db, map, "false_strong_freeze_summary");
        putState(db, map, "false_strong_freeze_score");
        putState(db, map, "false_strong_freeze_risk");
        putState(db, map, "false_strong_freeze_sample");

        putState(db, map, "seed_cause_expansion_status");
        putState(db, map, "seed_cause_expansion_summary");
        putState(db, map, "seed_cause_expansion_score");
        putState(db, map, "seed_cause_expansion_risk");
        putState(db, map, "seed_cause_expansion_sample");

        putState(db, map, "decision_v20_cause_summary");
        putState(db, map, "decision_cause_chain_summary");
        putState(db, map, "decision_rule_health_summary");
        putState(db, map, "decision_third_guard_summary");
        putState(db, map, "decision_shadow_health_summary");
        putState(db, map, "decision_false_strong_summary");
        putState(db, map, "decision_counterfactual_summary");
        putState(db, map, "decision_seed_expand_summary");
        putState(db, map, "decision_window_state_key");
        putState(db, map, "final_decision_window_state_key");
        putState(db, map, "dynamic_gate_window_state_key");
        putState(db, map, "probability_window_state_key");
        putState(db, map, "teacher_window_state_key");
        putState(db, map, "dynamic_window_state_key");
        putState(db, map, "window_state_key");
        putState(db, map, "state_key_12");
        putState(db, map, "state_key_24");
        putState(db, map, "state_key_48");
        putState(db, map, "state_key_96");
        putState(db, map, "state_key_360");
        putState(db, map, "decision_state_key_12");
        putState(db, map, "decision_state_key_24");
        putState(db, map, "decision_state_key_48");
        putState(db, map, "decision_state_key_96");
        putState(db, map, "decision_state_key_360");
        putState(db, map, "evidence_contribution");
        putState(db, map, "counter_contribution");
        putState(db, map, "decision_evidence_contribution");
        putState(db, map, "decision_counter_contribution");
        putState(db, map, "dynamic_gate_evidence_contribution");
        putState(db, map, "dynamic_gate_counter_contribution");
        putState(db, map, "probability_evidence_contribution");
        putState(db, map, "probability_counter_contribution");
        putState(db, map, "sample_reliability");
        putState(db, map, "sample_trust");
        putState(db, map, "recent_validity");
        putState(db, map, "recent_effectiveness");
        putState(db, map, "feature_distance");
        putState(db, map, "similar_window_sample");
        putState(db, map, "similar_window_hit_rate");
        putState(db, map, "evidence_text");
        putState(db, map, "counter_evidence_text");
        putState(db, map, "action_advice");
        putState(db, map, "decision_evidence_text");
        putState(db, map, "decision_counter_evidence_text");
        putState(db, map, "decision_action_advice");
        putState(db, map, "dynamic_causal_realtime");
        putState(db, map, "dynamic_causal_realtime_cost_ms");
        putState(db, map, "dynamic_causal_realtime_error");
        putState(db, map, "dynamic_need_supply_realtime");
        putState(db, map, "dynamic_window_cluster_realtime");
        putState(db, map, "counterfactual_reason_realtime");
        putState(db, map, "shadow_promotion_dynamic_refresh");
        putState(db, map, "adaptive_judge_realtime");
        putState(db, map, "dynamic_causal_state");
        putState(db, map, "dynamic_causal_summary");
        putState(db, map, "dynamic_causal_diagnosis");
        putState(db, map, "dynamic_cluster_summary");
        putState(db, map, "dynamic_window_cluster_summary");
        putState(db, map, "need_supply_summary");
        putState(db, map, "counterfactual_reason_summary");
        putState(db, map, "last_dynamic_causal_summary");
        putState(db, map, "last_decision_dynamic_causal_summary");
        putState(db, map, "final_decision_dynamic_causal_summary");
        putState(db, map, "dynamic_causal_decision_summary");
        putState(db, map, "dynamic_causal_decision_quality");
        putState(db, map, "dynamic_causal_decision_risk");
        putState(db, map, "dynamic_causal_decision_block_code");
        putState(db, map, "dynamic_cluster_id");
        putState(db, map, "dynamic_cluster_status");
        putState(db, map, "dynamic_cluster_hit_rate");
        putState(db, map, "dynamic_cluster_sample");
        putState(db, map, "need_supply_match_score");
        putState(db, map, "need_supply_third_quality");
        putState(db, map, "need_supply_risk");
        putState(db, map, "counterfactual_why_not_risk");
        putState(db, map, "adaptive_judge_action");
        putState(db, map, "promotion_cluster_status");
        putState(db, map, "shadow_promotion_dynamic_summary");
        putState(db, map, "shadow_promotion_dynamic_detail");
    }

    private void putState(DbStore db, HashMap<String, String> map, String key) {
        try {
            map.put(key, db.getState(key, ""));
        } catch (Throwable e) {
            map.put(key, "");
        }
    }

    private Models.Decision loadCurrentDecisionFromStore(
            DbStore db,
            Models.AppRuntimeState state,
            HashMap<String, String> states,
            Models.Draw latestDraw) {
        if (db == null) {
            return null;
        }

        try {
            String targetIssue = resolveTargetIssue(state, states, latestDraw);

            if (targetIssue == null || targetIssue.length() == 0) {
                return null;
            }

            Models.Decision d = null;

            try {
                d = db.getDecisionByTargetIssue(targetIssue);
            } catch (Throwable e) {
                d = null;
            }

            if (isDecisionForTarget(d, targetIssue)) {
                return d;
            }

            /* Never leak an older target into the current card.  While the R3.6 canonical
             * transaction is still running, show the store's explicit same-target pending HOLD
             * instead of rendering a blank decision card. */
            try {
                Models.Decision pending = db.getLatestDecision();
                if (isDecisionForTarget(pending, targetIssue)) {
                    return pending;
                }
            } catch (Throwable ignore) {
            }

            return null;
        } catch (Throwable e) {
            return null;
        }
    }

    static String visibleCurrentIssueR46(String observed, String persisted) {
        boolean o = TargetIssueResolver.isValid(observed), p = TargetIssueResolver.isValid(persisted);
        if (!o) return p ? persisted : "";
        if (!p) return observed;
        return observed.compareTo(persisted) >= 0 ? observed : persisted;
    }

    private String resolveCurrentIssue(
            Models.AppRuntimeState state, HashMap<String, String> states, Models.Draw latestDraw) {
        String observed = state == null ? "" : state.currentIssue;
        if (!TargetIssueResolver.isValid(observed) && states != null) observed = states.get("current_issue");
        String visible = visibleCurrentIssueR46(observed, latestDraw == null ? "" : latestDraw.issue);
        if (visible.length() > 0) return visible;
        String currentIssue = "";

        if (state != null && state.currentIssue != null && state.currentIssue.length() > 0) {
            currentIssue = state.currentIssue;
        }

        if (currentIssue.length() == 0 && states != null) {
            currentIssue = safeText(states.get("current_issue"));
        }

        if (currentIssue.length() == 0 && latestDraw != null && latestDraw.issue != null) {
            currentIssue = latestDraw.issue;
        }

        return currentIssue == null ? "" : currentIssue;
    }

    private String resolveTargetIssue(
            Models.AppRuntimeState state, HashMap<String, String> states, Models.Draw latestDraw) {
        String visibleCurrent = resolveCurrentIssue(state, states, latestDraw);
        TargetIssueResolver.Resolution visibleNext = TargetIssueResolver.next(visibleCurrent);
        if (visibleNext.valid) return visibleNext.targetIssue;
        String targetIssue = "";

        if (state != null && state.targetIssue != null && state.targetIssue.length() > 0) {
            targetIssue = state.targetIssue;
        }

        if (targetIssue.length() == 0 && states != null) {
            targetIssue = safeText(states.get("target_issue"));
        }

        if (targetIssue.length() == 0 && states != null) {
            targetIssue = safeText(states.get("analysis_target_issue"));
        }

        if (targetIssue.length() == 0
                && latestDraw != null
                && latestDraw.issue != null
                && latestDraw.issue.length() > 0) {
            targetIssue = buildNextIssueText(latestDraw.issue);
        }

        return targetIssue == null ? "" : targetIssue;
    }

    private void handleTargetIssueChanged(String currentIssue, String targetIssue, String source) {
        if (currentIssue != null && currentIssue.length() > 0
                && (safeText(uiLockedCurrentIssue).isEmpty()||compareIssueText(currentIssue,uiLockedCurrentIssue)>=0)) {
            uiLockedCurrentIssue = currentIssue;
        }

        if (targetIssue == null || targetIssue.length() == 0) {
            return;
        }

        if (uiLockedTargetIssue != null && uiLockedTargetIssue.length() > 0) {
            if (sameIssueText(uiLockedTargetIssue, targetIssue)) {
                return;
            }

            int cmp = compareIssueText(targetIssue, uiLockedTargetIssue);

            if (cmp < 0) {
                return;
            }
        }

        uiLockedTargetIssue = targetIssue;
        uiLockedTargetAt = System.currentTimeMillis();

        lastDecisionRenderKey = "";
        lastReasonRenderKey = "";

        clearDecisionCardForTarget(
                uiLockedCurrentIssue, uiLockedTargetIssue, "目标期已切换，等待当前目标期实时分析完成");
        setText(txtServiceBadge,"目标期已切换｜业务状态刷新中");
        setTextColor(txtServiceBadge,R.color.colorWarn);
        LightSnapshot pendingTruth=new LightSnapshot();
        pendingTruth.targetIssue=uiLockedTargetIssue;
        pendingTruth.uiRevision=uiRevision.get();
        renderR906Truth(pendingTruth);
    }

    private boolean isDecisionForTarget(Models.Decision d, String targetIssue) {
        if (d == null || targetIssue == null || targetIssue.length() == 0) {
            return false;
        }

        if (d.targetIssue == null || d.targetIssue.length() == 0) {
            return false;
        }

        return sameIssueText(d.targetIssue, targetIssue);
    }

    private boolean isTradeForTarget(Models.Trade t, String targetIssue) {
        if (t == null || targetIssue == null || targetIssue.length() == 0) {
            return false;
        }

        if (t.targetIssue == null || t.targetIssue.length() == 0) {
            return false;
        }

        return sameIssueText(t.targetIssue, targetIssue);
    }

    private boolean shouldAcceptBroadcastTarget(String broadcastTarget) {
        if (broadcastTarget == null || broadcastTarget.length() == 0) {
            return true;
        }

        String locked = getBestKnownTargetIssue();

        if (locked.length() == 0) {
            return true;
        }

        if (sameIssueText(locked, broadcastTarget)) {
            return true;
        }

        int cmp = compareIssueText(broadcastTarget, locked);

        return cmp > 0;
    }

    private String getBestKnownTargetIssue() {
        if (uiLockedTargetIssue != null && uiLockedTargetIssue.length() > 0) {
            return uiLockedTargetIssue;
        }

        LightSnapshot s = lastLightSnapshot;

        if (s != null) {
            if (s.targetIssue != null && s.targetIssue.length() > 0) {
                return s.targetIssue;
            }

            String target = stateOf(s, "target_issue");
            if (target.length() > 0) {
                return target;
            }
        }

        return "";
    }

    private void clearDecisionCardForTarget(
            String currentIssue, String targetIssue, String reason) {
        if (destroyed) {
            return;
        }

        String target = targetIssue == null ? "" : targetIssue;

        setText(txtDecisionLabel, "目标期：" + safeDash(target));
        setText(txtDecision, "等待分析");
        setTextColor(txtDecision, R.color.colorHold);
        setText(txtPositionNumbers, "当前目标期尚未生成正式预测");
        setText(
                txtDecisionSummary,
                "目标期："
                        + safeDash(target)
                        + "。"
                        + (reason == null || reason.length() == 0
                                ? "等待实时分析完成，不会用上一期预测替代当前期。"
                                : reason)
                        + "，不会用上一期预测替代当前期。");

        setText(txtFinalP, "--");
        setText(txtDynamicGate, "--");
        setText(txtEdge, "--");
        setTextColor(txtEdge, R.color.colorTextSub);

        setText(txtStateMode, "状态：--");
        setText(txtTrendLife, "趋势寿命：--");
        setText(txtDriftRisk, "漂移风险：--");
        setText(txtBreakDepth, "破位深度：--");
        setText(txtRecoveryScore, "恢复分：--");

        setText(txtSimilarHitRate, "相似命中率：--");
        setText(txtEffectiveSample, "有效样本：--");
        setText(txtComboHitRate, "组合命中率：--");
        setText(txtChampionGap, "主优势差：--");
        setText(txtComboGap, "组合优势差：--");

        if (currentIssue != null && currentIssue.length() > 0) {
            setText(txtCurrentIssue, "当前期：" + uiLockedCurrentIssue);
        }

        if (target.length() > 0) {
            setText(txtTargetIssue, "目标期：" + target);
        }

        lastDecisionRenderKey = "CLEAR|" + target + "|" + reason;
    }

    private void renderLightSnapshot(LightSnapshot snapshot) {
        if (snapshot == null) {
            return;
        }

        if (snapshot.uiRevision != uiRevision.get()) {
            requestLightRefreshSoon(false);
            return;
        }

        // Clock adoption can advance the target. Do it before the stale-target fence so an
        // old completed DB read cannot paint its decision after the clock changes the header.
        acceptClockAnchorR76(CountdownClock.liveAnchorR72(),TimeSourceR811.monoNowMs());

        String displayedTarget = getBestKnownTargetIssue();
        if (displayedTarget.length() > 0
                && snapshot.targetIssue != null
                && snapshot.targetIssue.length() > 0
                && compareIssueText(snapshot.targetIssue, displayedTarget) < 0) {
            // Global connection/runtime/statistics remain useful while the historical watermark
            // catches up. Never bind an older decision, issue, probability or truth card here.
            renderIndependentPanelsR46(snapshot, displayedTarget);
            return;
        }

        // Newer targets are rejected above and same-target canonical evidence is retained
        // below. Ordinary state heartbeats must not starve a completed same-target read.
        if (lastLightSnapshot != null
                && lastLightSnapshot.loadedAt > 0L
                && snapshot.loadedAt > 0L
                && snapshot.loadedAt < lastLightSnapshot.loadedAt) {
            return;
        }

        LightSnapshot previousStableSnapshot = lastLightSnapshot;
        lastLightRefreshAt = System.currentTimeMillis();
        lastUiRefreshAt = lastLightRefreshAt;

        if (snapshot.error != null && snapshot.error.length() > 0) {
            setText(txtRuntimeStatus, snapshot.error);
            return;
        }

        handleTargetIssueChanged(snapshot.currentIssue, snapshot.targetIssue, "light_r901");

        if (!isDecisionForTarget(snapshot.decision, snapshot.targetIssue)) {
            snapshot.decision = null;
            snapshot.currentTrade = null;
        }

        /* R901 same-target sticky snapshot. A transient SQLITE_BUSY/read miss must not erase a
         * previously rendered immutable canonical decision and make the card flash to WAITING. */
        if (snapshot.decision == null
                && previousStableSnapshot != null
                && sameIssueText(previousStableSnapshot.targetIssue, snapshot.targetIssue)
                && isDecisionForTarget(previousStableSnapshot.decision, snapshot.targetIssue)) {
            snapshot.decision = previousStableSnapshot.decision;
            snapshot.currentTrade = previousStableSnapshot.currentTrade;
        } else if (snapshot.decision != null
                && previousStableSnapshot != null
                && sameIssueText(previousStableSnapshot.targetIssue, snapshot.targetIssue)
                && isDecisionForTarget(previousStableSnapshot.decision, snapshot.targetIssue)) {
            stabilizeSameTargetDecisionEvidenceR901(snapshot.decision, previousStableSnapshot.decision);
        }

        if (snapshot.currentTrade != null
                && snapshot.decision != null
                && !isTradeForTarget(snapshot.currentTrade, snapshot.decision.targetIssue)) {
            snapshot.currentTrade = null;
        }

        lastLightSnapshot = snapshot;

        refreshRuntimeCard(snapshot.state, snapshot);
        // The service can publish a newer anchor between the entry fence and the runtime
        // card's countdown read. That read has already cleared the older target's cards.
        if(!safeText(snapshot.targetIssue).equals(safeText(uiLockedTargetIssue)))return;
        refreshDecisionCard(snapshot.decision, snapshot.currentTrade, snapshot.state, snapshot);
        refreshProbabilityCard(snapshot.decision);
        refreshShapeCard(snapshot.decision);
        refreshProbabilityDetailCard(snapshot.decision);
        if (!isUserScrollingNow()) {
            refreshStatsCard(snapshot.stats, snapshot);
            refreshBacktestCard(snapshot.backtest, snapshot);
            renderR906Truth(snapshot);
        }
        renderHeartbeatTruthR323();
    }

    private void renderIndependentPanelsR46(LightSnapshot snapshot, String displayedTarget) {
        String status = snapshot.state == null ? stateOf(snapshot, "service_status")
                : snapshot.state.serviceStatus;
        String apiLine = snapshot.state == null ? stateOf(snapshot, "api_line")
                : snapshot.state.apiLine;
        setText(txtApiLine, KeyProvider.isKeyReady()
                ? "接口：" + UiFormatter.apiLineText(apiLine)
                    + (KeyProvider.usesReleaseDefault() ? "｜已内置密钥，可点击修改" : "｜设备密钥，可点击修改")
                : "接口凭据未就绪｜点击配置");
        setText(txtRuntimeStatus, buildRuntimeText(snapshot.state, status, snapshot)
                + "\n状态读取目标=" + safeDash(snapshot.targetIssue)
                + "｜当前显示目标=" + safeDash(displayedTarget)
                + "｜历史同步中，旧期预测不用于当前期。");
        refreshStatsCard(snapshot.stats, snapshot);
        refreshBacktestCard(snapshot.backtest, snapshot);
    }

    private void renderR906Truth(LightSnapshot snapshot) {
        if (snapshot == null) return;
        RuntimeTruthSnapshotR906 truth = snapshot.truthR906;
        TruthObservationR76 observed=cachedTruthR907;
        if(truth==null&&observed!=null&&snapshot.targetIssue.equals(observed.target))truth=observed.snapshot;
        if (truth == null || (!truth.targetIssue.isEmpty()&&!snapshot.targetIssue.equals(truth.targetIssue))) {
            truth = RuntimeTruthSnapshotR906.pending(snapshot.targetIssue);
            requestR919TruthRefresh(true);
        }
        if (!homeTruthPresenterR906.accept(snapshot.uiRevision, truth)) return;
        long started = frameMetricsR906.beginBind();
        try {
            if (whyTruthRendererR906 != null) whyTruthRendererR906.render(truth);
            if (diagnosticStateRendererR906 != null) diagnosticStateRendererR906.render(truth);
            if (snapshot.states != null) {
                snapshot.states.put("r906_truth_snapshot_id", truth.snapshotId);
                snapshot.states.put("r906_truth_snapshot_hash", truth.hash);
                snapshot.states.put("r906_frame_metrics", frameMetricsR906.summary());
            }
        } finally {
            frameMetricsR906.endBind(started);
        }
    }

    private static String compactWhyTruthR906(RuntimeTruthSnapshotR906 truth) {
        if (truth == null || truth.whyCards == null) return "";
        StringBuilder b = new StringBuilder(900);
        for (int i = 0; i < truth.whyCards.length; i++) {
            RuntimeTruthSnapshotR906.TruthCard c = truth.whyCards[i];
            if (c == null) continue;
            if (i > 0) b.append('\n');
            b.append(c.title).append('：').append(c.detail);
        }
        return b.toString();
    }

    /** R902 keeps the core why-result visible even when the heavy diagnosis worker is delayed. */
    private void renderLightReasonTruthR902(LightSnapshot snapshot) {
        if (listReasons == null || snapshot == null) return;
        String detail = firstNonEmpty(
                snapshot.whyTruthText,
                stateOf(snapshot, "last_decision_summary"),
                "等待赛前冻结预测与精确结算真值");
        renderWhyTruthCardsR903(detail, snapshot);
    }

    /** R903 presents the three independent truth questions as full evidence cards. */
    private void renderWhyTruthCardsR903(String text, LightSnapshot snapshot) {
        if (listReasons == null) return;
        String source = firstNonEmpty(text, "等待赛前冻结预测与精确结算真值");
        String r905Key = Integer.toHexString(source.hashCode()) + "|"
                + (snapshot == null || snapshot.decision == null ? "" : (snapshot.decision.targetIssue == null ? "" : snapshot.decision.targetIssue));
        if (r905Key.equals(lastWhyTruthRenderKeyR905) && listReasons.getChildCount() > 0) return;
        lastWhyTruthRenderKeyR905 = r905Key;
        listReasons.removeAllViews();
        String[] markers =
                new String[] {"为什么中：", "为什么不中：", "为什么不出手：", "结算真值："};
        String[] titles =
                new String[] {"为什么中", "为什么不中", "为什么不出手", "逐期结算统计"};
        String[] levels =
                new String[] {
                    AppConfig.REASON_PASS,
                    AppConfig.REASON_WARN,
                    AppConfig.REASON_INFO,
                    AppConfig.REASON_INFO
                };
        int added = 0;
        for (int i = 0; i < markers.length; i++) {
            int start = source.indexOf(markers[i]);
            if (start < 0) continue;
            start += markers[i].length();
            int end = source.length();
            for (int j = 0; j < markers.length; j++) {
                int next = source.indexOf(markers[j], start);
                if (next >= 0 && next < end) end = next;
            }
            String body = source.substring(start, end).trim();
            // R905 home card intentionally stays compact; full causal/Teacher evidence lives in
            // the dedicated diagnostic pages and complete copyable report, not in this scrolling card.
            Models.ReasonItem item = new Models.ReasonItem();
            item.level = levels[i];
            item.code = "R903_WHY_TRUTH_" + i;
            item.title = titles[i];
            item.detail = limitText(body, 420);
            item.value = 0.0d;
            item.passed = i == 1 ? 0 : 1;
            item.createdAt = System.currentTimeMillis();
            addReasonRow(item, snapshot == null ? null : snapshot.state, snapshot);
            added++;
        }
        if (added == 0) {
            Models.ReasonItem fallback = new Models.ReasonItem();
            fallback.level = AppConfig.REASON_INFO;
            fallback.code = "R903_WHY_TRUTH_WAIT";
            fallback.title = "因果证据等待区";
            fallback.detail = limitText(source, 1800);
            fallback.passed = 1;
            fallback.createdAt = System.currentTimeMillis();
            addReasonRow(fallback, snapshot == null ? null : snapshot.state, snapshot);
        }
    }

    private void renderHeavySnapshot(HeavySnapshot snapshot) {
        if (snapshot == null) {
            return;
        }

        lastHeavyRefreshAt = System.currentTimeMillis();
        heavyRenderBudgetActive = true;
        heavyRenderBudgetRemaining =
                isLowMemoryPressure()
                        ? UI_LOW_MEMORY_RENDER_TOTAL_CHAR_BUDGET
                        : UI_HEAVY_RENDER_TOTAL_CHAR_BUDGET;
        if (snapshot.fullDiagnosisCopyText != null && snapshot.fullDiagnosisCopyText.length() > 0) {
            cachedFullDiagnosisCopyText =
                    limitText(snapshot.fullDiagnosisCopyText, UI_FULL_DIAGNOSIS_COPY_LIMIT);
            diagnosisClipboardChunkIndex = 0;
        }

        boolean oldSuppress = suppressScrollGuard;
        suppressScrollGuard = true;

        try {
            /* Heavy diagnosis/history is deliberately forbidden from repainting live cards.
             * snapshot.light was captured before slow DB/diagnosis work and can be stale by the
             * time this callback reaches the UI thread. */
            if (snapshot.error != null && snapshot.error.length() > 0) {
                setText(txtRuntimeStatus, snapshot.error);
                return;
            }

            // R906: heavy/Teacher/V20 diagnosis has no write authority over the four truth cards.
            refreshDrawHistoryList(snapshot);
            refreshHistoryList(snapshot);
        } finally {
            heavyRenderBudgetActive = false;
            heavyRenderBudgetRemaining = 0;
            suppressScrollGuard = oldSuppress;
        }
    }

    private void renderFastDecisionFromBroadcast(Intent intent) {
        if (intent == null || destroyed) {
            return;
        }

        try {
            String decision = intent.getStringExtra(AppConfig.EXTRA_DECISION);
            String currentIssue = intent.getStringExtra(AppConfig.EXTRA_CURRENT_ISSUE);
            String targetIssue = intent.getStringExtra(AppConfig.EXTRA_TARGET_ISSUE);

            if (targetIssue == null || targetIssue.length() == 0) {
                targetIssue = getBestKnownTargetIssue();
            }

            if (!shouldAcceptBroadcastTarget(targetIssue)) {
                return;
            }

            handleTargetIssueChanged(currentIssue, targetIssue, "decision_broadcast");

            String lockedTarget = getBestKnownTargetIssue();

            if (lockedTarget.length() > 0
                    && targetIssue.length() > 0
                    && !sameIssueText(lockedTarget, targetIssue)) {
                return;
            }

            int position = intent.getIntExtra("position", 0);
            int n1 = intent.getIntExtra("n1", 0);
            int n2 = intent.getIntExtra("n2", 0);
            int n3 = intent.getIntExtra("n3", 0);
            int k3Mask = intent.getIntExtra("k3_mask", 0);

            double finalP = intent.getDoubleExtra("final_p", Double.NaN);
            double rawP = intent.getDoubleExtra("raw_p", Double.NaN);
            double modelP = intent.getDoubleExtra("model_p", Double.NaN);
            double gate = intent.getDoubleExtra("dynamic_gate", Double.NaN);
            double edge = intent.getDoubleExtra("edge", Double.NaN);

            String blockCode = intent.getStringExtra("block_code");
            String reasonSummary = intent.getStringExtra("reason_summary");
            String decisionBrief = intent.getStringExtra("decision_brief");
            int settled = intent.getIntExtra("settled", 0);
            String settleResult = intent.getStringExtra("settle_result");
            String settlementSummary = intent.getStringExtra("settlement_summary");

            if (decision == null || decision.length() == 0) {
                decision = AppConfig.DECISION_HOLD;
            }

            setText(txtDecisionLabel, "目标期：" + safeDash(targetIssue));
            setText(txtDecision, UiFormatter.decisionText(decision));
            setTextColor(txtDecision, UiFormatter.decisionColor(decision));

            if (AppConfig.DECISION_BET.equals(decision)) {
                setText(
                        txtPositionNumbers,
                        "第" + position + "位置  " + UiFormatter.numberGroup(n1, n2, n3));
            } else if (AppConfig.DECISION_SHADOW.equals(decision)) {
                if (position > 0 && Models.isValidK3Mask(k3Mask)) {
                    setText(
                            txtPositionNumbers,
                            "影子观察 第" + position + "位置  " + UiFormatter.numberGroup(n1, n2, n3));
                } else {
                    setText(txtPositionNumbers, "影子观察");
                }
            } else {
                if (position > 0 && Models.isValidK3Mask(k3Mask)) {
                    setText(
                            txtPositionNumbers,
                            "不出手观察 第" + position + "位置  " + UiFormatter.numberGroup(n1, n2, n3));
                } else {
                    setText(txtPositionNumbers, "本期不出手");
                }
            }

            if ((!isFiniteUiR3610(edge) || edge == 0.0d)
                    && isPositiveFiniteUiR3610(finalP)
                    && isPositiveFiniteUiR3610(gate)) {
                edge = finalP - gate;
            }

            if (isPositiveFiniteUiR3610(finalP)) {
                setText(txtFinalP, UiFormatter.probability(finalP));
            } else {
                double upstreamP =
                        isPositiveFiniteUiR3610(rawP)
                                ? rawP
                                : (isPositiveFiniteUiR3610(modelP) ? modelP : Double.NaN);
                setText(
                        txtFinalP,
                        isPositiveFiniteUiR3610(upstreamP)
                                ? "未校准\n原始 " + UiFormatter.probability(upstreamP)
                                : "未生成");
            }
            setText(txtDynamicGate, probabilityUiR3610(gate));
            setText(txtEdge, signedProbabilityUiR3610(edge));

            if (isFiniteUiR3610(edge) && edge >= 0.0d) {
                setTextColor(txtEdge, R.color.colorWin);
            } else {
                setTextColor(txtEdge, R.color.colorMiss);
            }

            String summary = "";

            if (settled > 0 || isSettlementResultText(settleResult)) {
                summary = "已结算：" + UiFormatter.resultText(settleResult);
                if (position > 0) {
                    summary =
                            appendReason(
                                    summary,
                                    "第"
                                            + position
                                            + "位置 投注="
                                            + UiFormatter.numberGroup(n1, n2, n3));
                }
                if (settlementSummary != null && settlementSummary.length() > 0) {
                    summary = appendReason(summary, settlementSummary);
                }
            } else if (decisionBrief != null && decisionBrief.length() > 0) {
                summary = decisionBrief;
            } else if (reasonSummary != null && reasonSummary.length() > 0) {
                summary = reasonSummary;
            } else if (blockCode != null && blockCode.length() > 0) {
                summary = UiFormatter.blockReasonText(blockCode);
            } else {
                summary = "当前目标期正式预测已生成，完整原因正在后台补全";
            }

            String broadcastEvidence = buildBroadcastV20EvidenceText(intent);
            if (broadcastEvidence.length() > 0) {
                summary = appendReason(summary, broadcastEvidence);
            }
            String broadcastDynamic = buildBroadcastDynamicCausalText(intent);
            if (broadcastDynamic.length() > 0) {
                summary = appendReason(summary, broadcastDynamic);
            }
            setText(txtDecisionSummary, AlgorithmAttributionR943.line(lastLightSnapshot==null?null:lastLightSnapshot.states,targetIssue)+"\n"+translateLooseText(limitText(summary, 520)));

            if (currentIssue != null && currentIssue.length() > 0) {
                setText(txtCurrentIssue, "当前期：" + uiLockedCurrentIssue);
            }

            if (targetIssue.length() > 0) {
                setText(txtTargetIssue, "目标期：" + uiLockedTargetIssue);
            }

            lastDecisionRenderKey =
                    "FAST_BROADCAST|"
                            + targetIssue
                            + "|"
                            + decision
                            + "|"
                            + position
                            + "|"
                            + k3Mask
                            + "|"
                            + finalP
                            + "|"
                            + gate
                            + "|"
                            + edge
                            + "|"
                            + settled
                            + "|"
                            + settleResult;
        } catch (Throwable e) {
        }
    }

    private void refreshRuntimeCard(Models.AppRuntimeState state, LightSnapshot snapshot) {
        if (state == null && snapshot == null) {
            renderHeartbeatTruthR323();
            setText(txtCountdown, "--:--");
            setText(txtCurrentIssue, "当前期：--");
            setText(txtTargetIssue, "目标期：--");
            setText(txtApiLine, "接口：--｜点击配置凭据");
            setText(txtRuntimeStatus, "运行状态：暂无后台状态");
            setTextColor(txtServiceBadge, R.color.colorWarn);
            return;
        }

        String status = state == null ? "" : state.serviceStatus;

        if (status == null || status.length() == 0) {
            status = stateOf(snapshot, "service_status");
        }

        if (status == null || status.length() == 0) {
            status = AppConfig.SERVICE_STATUS_STOPPED;
        }

        String currentIssue = snapshot == null ? "" : snapshot.currentIssue;
        String targetIssue = snapshot == null ? "" : snapshot.targetIssue;

        if (currentIssue.length() == 0 && state != null && state.currentIssue != null) {
            currentIssue = state.currentIssue;
        }

        if (targetIssue.length() == 0 && state != null && state.targetIssue != null) {
            targetIssue = state.targetIssue;
        }

        String renderKey =
                status
                        + "|" + KeyProvider.isKeyReady() + "|" + KeyProvider.credentialSource()
                        + "|"
                        + currentIssue
                        + "|"
                        + targetIssue
                        + "|"
                        + (state == null ? "" : state.apiLine)
                        + "|"
                        + (snapshot == null ? 0 : snapshot.drawCount)
                        + "|"
                        + (snapshot == null ? 0 : snapshot.drawRemainTo240)
                        + "|"
                        + stateOf(snapshot, "analysis_status")
                        + "|"
                        + stateOf(snapshot, "analysis_progress")
                        + "|"
                        + stateOf(snapshot, "backtest_status")
                        + "|"
                        + stateOf(snapshot, "backtest_progress")
                        + "|"
                        + stateOf(snapshot, "teacher_matrix_summary")
                        + "|"
                        + stateOf(snapshot, "teacher_snapshot_summary")
                        + "|"
                        + stateOf(snapshot, "runtime_profile_summary")
                        + "|"
                        + stateOf(snapshot, "learning_reason_detail")
                        + "|"
                        + stateOf(snapshot, "settlement_summary")
                        + "|"
                        + stateOf(snapshot, "settlement_pnl")
                        + "|"
                        + stateOf(snapshot, "last_strict_trade_settlement_detail")
                        + "|"
                        + stateOf(snapshot, "settlement_catchup_summary")
                        + "|"
                        + buildV20StateKey(snapshot)
                        + "|" + stateOf(snapshot, "r46_history_read_issue")
                        + "|" + stateOf(snapshot, "cp12_draw_retention_reason");

        if (!renderKey.equals(lastRuntimeRenderKey)) {
            lastRuntimeRenderKey = renderKey;

            renderHeartbeatTruthR323();

            setText(txtCurrentIssue, "当前期：" + safeDash(currentIssue));
            setText(txtTargetIssue, "目标期：" + safeDash(targetIssue));

            String apiLine = state == null ? stateOf(snapshot, "api_line") : state.apiLine;
            setText(txtApiLine, KeyProvider.isKeyReady()?"接口：" + UiFormatter.apiLineText(apiLine)
                    + (KeyProvider.usesReleaseDefault()?"｜已内置密钥，可点击修改":"｜设备密钥，可点击修改")
                    :"接口凭据未就绪｜点击配置");

            String runtime = buildRuntimeText(state, status, snapshot);
            setText(txtRuntimeStatus, runtime);
        }

        long countdown = getBestCountdownFromState(state, snapshot);
        setText(txtCountdown, countdownTextR994(countdown));
    }

    private String buildRuntimeText(
            Models.AppRuntimeState state, String status, LightSnapshot snapshot) {
        StringBuilder sb = new StringBuilder();

        sb.append("运行状态：");
        try {
            ServiceHeartbeatStore hbTruth = new ServiceHeartbeatStore(getApplicationContext());
            String truth = hbTruth.runtimeTruthState();
            if ("RUNNING".equals(truth)) sb.append("后台服务运行中");
            else if ("STARTING".equals(truth)) sb.append("后台服务启动中");
            else if ("INIT_FAILED_ALIVE".equals(truth) || "INIT_FAILED".equals(truth))
                sb.append("后台初始化异常·自动重试中");
            else if ("STOPPED_BY_USER".equals(truth)) sb.append("后台服务已停止");
            else sb.append("后台心跳异常·等待恢复");
            sb.append("｜真值=").append(truth);
        } catch (Throwable e) {
            sb.append(UiFormatter.serviceStatusText(status));
            sb.append("｜真值读取异常=").append(e.getClass().getSimpleName());
        }

        if (snapshot != null) {
            sb.append("｜已抓");
            sb.append(snapshot.drawCount);
            sb.append("期");

            if (snapshot.drawRemainTo240 > 0) {
                sb.append("｜距240还差");
                sb.append(snapshot.drawRemainTo240);
                sb.append("期");
            } else {
                sb.append("｜已满30期受控启动");
            }
        }

        String serviceAction = stateOf(snapshot, "service_last_action");
        if (serviceAction.length() > 0) {
            sb.append("\n操作：");
            sb.append(serviceAction);
        }

        String scheduler = state == null ? "" : safeText(getFieldText(state, "schedulerSummary"));
        if (scheduler.length() > 0) {
            sb.append("\n上次调度记录：");
            sb.append(translateLooseText(limitText(scheduler, 260)));
        }

        String historyReadR46 = stateOf(snapshot, "r46_history_read_issue");
        if (historyReadR46.length() > 0 && snapshot != null
                && !sameIssueText(historyReadR46, snapshot.currentIssue)) {
            sb.append("\n已确认开奖=").append(safeDash(snapshot.currentIssue))
                    .append("｜历史入库=").append(historyReadR46)
                    .append("｜待补齐连续历史；").append(stateOf(snapshot, "cp12_draw_retention_reason"));
        }
        appendLine(sb, buildBusinessHealthLine(snapshot));
        appendLine(sb, buildFetchLine(snapshot));
        appendLine(sb, buildAnalysisLine(status, snapshot));
        appendLine(sb, buildDecisionStateLine(snapshot));
        appendLine(sb, buildSettlementLine(snapshot));
        appendLine(sb, buildRealBetDiagnosisLine(snapshot));
        appendLine(sb, buildFullDiagnosisClosedLoopLine(snapshot));
        appendLine(sb, buildDynamicCausalRealtimeLine(snapshot));
        appendLine(sb, buildDynamicCausalDiagnosisLine(snapshot));
        appendLine(sb, buildV20CauseLine(snapshot));
        appendLine(sb, buildTeacherMatrixLine(snapshot));
        appendLine(sb, buildTeacherSnapshotLine(snapshot));
        appendLine(sb, buildLearningLine(snapshot));
        appendLine(sb, buildRuntimeProfileLine(snapshot));
        appendLine(sb, buildBacktestLine(snapshot));
        appendLine(sb, buildBackfillLine(snapshot));

        if (state != null && state.lastError != null && state.lastError.length() > 0) {
            sb.append("\n异常：");
            sb.append(translateLooseText(limitText(state.lastError, 120)));
        }

        return sb.toString();
    }

    private String buildFetchLine(LightSnapshot snapshot) {
        String fetchStatus = stateOf(snapshot, "fetch_status");
        String fetchCount = stateOf(snapshot, "fetch_count");
        String fetchSummary = stateOf(snapshot, "fetch_summary");
        String lastFetchAt = stateOf(snapshot, "last_fetch_at");
        String openTry = stateOf(snapshot, "open_fetch_try");

        if (fetchStatus.length() == 0
                && fetchCount.length() == 0
                && fetchSummary.length() == 0
                && lastFetchAt.length() == 0
                && openTry.length() == 0) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("抓取线程：");

        if (fetchStatus.length() > 0) {
            sb.append(translateLooseText(fetchStatus));
        } else {
            sb.append("有状态");
        }

        if (fetchCount.length() > 0) {
            sb.append("｜次数=");
            sb.append(fetchCount);
        }

        if (openTry.length() > 0) {
            sb.append("｜开奖尝试=");
            sb.append(openTry);
        }

        if (lastFetchAt.length() > 0) {
            long t = parseLong(lastFetchAt, 0L);
            if (t > 0L) {
                sb.append("｜");
                sb.append(formatShortTime(t));
            }
        }

        if (fetchSummary.length() > 0) {
            sb.append("｜");
            sb.append(translateLooseText(limitText(fetchSummary, 140)));
        }

        return sb.toString();
    }

    private String buildAnalysisLine(String serviceStatus, LightSnapshot snapshot) {
        String status = stateOf(snapshot, "analysis_status");
        String target = stateOf(snapshot, "analysis_target_issue");
        String progress = stateOf(snapshot, "analysis_progress");
        String summary = stateOf(snapshot, "analysis_summary");
        String cost = stateOf(snapshot, "analysis_cost_ms");
        String costSummary = stateOf(snapshot, "analysis_cost_summary");
        String doneTarget = stateOf(snapshot, "analysis_done_target_issue");
        String lastDecision = stateOf(snapshot, "analysis_last_decision");
        String lastSummary = stateOf(snapshot, "analysis_last_summary");

        if ((status == null || status.length() == 0)
                && AppConfig.SERVICE_STATUS_ANALYZING.equals(serviceStatus)) {
            status = "RUNNING";
        }

        if ((status == null || status.length() == 0)
                && (summary == null || summary.length() == 0)
                && (doneTarget == null || doneTarget.length() == 0)
                && (lastDecision == null || lastDecision.length() == 0)) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("分析线程：");
        sb.append(translateLooseText(status));

        if (target != null && target.length() > 0) {
            sb.append("｜目标=");
            sb.append(target);
        }

        if (progress != null && progress.length() > 0) {
            sb.append("｜");
            sb.append(progress);
            sb.append("%");
        }

        long costMs = parseLong(cost, 0L);
        if (costMs > 0L) {
            sb.append("｜耗时");
            sb.append(costMs);
            sb.append("ms");
        } else if (costSummary != null && costSummary.length() > 0) {
            sb.append("｜耗时=");
            sb.append(limitText(costSummary, 80));
        }

        if (doneTarget != null && doneTarget.length() > 0) {
            sb.append("｜完成=");
            sb.append(doneTarget);
        }

        if (lastDecision != null && lastDecision.length() > 0) {
            sb.append("｜结果=");
            sb.append(translateLooseText(lastDecision));
        }

        if (summary != null && summary.length() > 0) {
            sb.append("｜");
            sb.append(translateLooseText(limitText(summary, 160)));
        } else if (lastSummary != null && lastSummary.length() > 0) {
            sb.append("｜");
            sb.append(translateLooseText(limitText(lastSummary, 160)));
        }

        return sb.toString();
    }

    private String buildDecisionStateLine(LightSnapshot snapshot) {
        String currentTarget = stateOf(snapshot, "target_issue");
        if (currentTarget.length() == 0 && snapshot != null) {
            currentTarget = snapshot.targetIssue;
        }

        String target = stateOf(snapshot, "last_decision_target_issue");
        String decision = stateOf(snapshot, "last_decision");
        String summary = stateOf(snapshot, "last_decision_summary");

        if (decision.length() == 0) {
            decision = stateOf(snapshot, "analysis_last_decision");
        }

        if (summary.length() == 0) {
            summary = stateOf(snapshot, "analysis_last_summary");
        }

        if (target.length() == 0 && decision.length() == 0 && summary.length() == 0) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("最新决策：");

        if (currentTarget.length() > 0
                && target.length() > 0
                && !sameIssueText(currentTarget, target)) {
            sb.append("当前目标=");
            sb.append(currentTarget);
            sb.append("｜等待当前期分析完成");
            sb.append("｜旧决策=");
            sb.append(target);
            return sb.toString();
        }

        if (target.length() > 0) {
            sb.append(target);
            sb.append("｜");
        }

        if (decision.length() > 0) {
            sb.append(translateLooseText(decision));
        } else {
            sb.append("--");
        }

        if (summary.length() > 0) {
            sb.append("｜");
            sb.append(translateLooseText(limitText(summary, 140)));
        }

        return sb.toString();
    }

    private String buildSettlementLine(LightSnapshot snapshot) {
        String status = stateOf(snapshot, "settlement_status");
        String summary = stateOf(snapshot, "settlement_summary");

        if (summary.length() == 0) {
            summary = stateOf(snapshot, "settlement_detail");
        }

        if (summary.length() == 0) {
            summary = stateOf(snapshot, "last_strict_trade_settlement_detail");
        }

        if (summary.length() == 0) {
            summary = stateOf(snapshot, "last_strict_settlement_detail");
        }

        if (summary.length() == 0) {
            summary = stateOf(snapshot, "last_settlement_detail");
        }

        String catchup = stateOf(snapshot, "settlement_catchup_summary");

        if (status.length() == 0 && summary.length() == 0 && catchup.length() == 0) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("结算线程：");
        sb.append(status.length() == 0 ? "空闲" : translateLooseText(status));

        if (summary.length() > 0) {
            sb.append("｜");
            sb.append(removePnlText(translateLooseText(limitText(summary, 180))));
        }

        if (catchup.length() > 0) {
            sb.append("｜补扫=");
            sb.append(translateLooseText(limitText(catchup, 120)));
        }

        return sb.toString();
    }

    private String buildRealBetDiagnosisLine(LightSnapshot snapshot) {
        String summary = stateOf(snapshot, "real_bet_diagnosis_summary");
        String mainProblem = stateOf(snapshot, "real_bet_diagnosis_main_problem");
        String nextFix = stateOf(snapshot, "real_bet_diagnosis_next_fix");
        String total = stateOf(snapshot, "real_bet_diagnosis_total");
        String hit = stateOf(snapshot, "real_bet_diagnosis_hit");
        String miss = stateOf(snapshot, "real_bet_diagnosis_miss");
        String winRate = stateOf(snapshot, "real_bet_diagnosis_win_rate");
        String shadowTotal = stateOf(snapshot, "real_bet_diagnosis_shadow_total");
        String shadowRate = stateOf(snapshot, "real_bet_diagnosis_shadow_rate");

        if (summary.length() == 0 && mainProblem.length() == 0 && total.length() == 0) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("真实BET诊断：");
        if (total.length() > 0) {
            sb.append("BET=");
            sb.append(total);
            if (hit.length() > 0 || miss.length() > 0) {
                sb.append(" 中/挂=");
                sb.append(hit.length() == 0 ? "0" : hit);
                sb.append("/");
                sb.append(miss.length() == 0 ? "0" : miss);
            }
            if (winRate.length() > 0) {
                sb.append(" 胜率=");
                sb.append(formatMaybeProbability(winRate));
            }
            if (shadowTotal.length() > 0 && !"0".equals(shadowTotal)) {
                sb.append("｜SHADOW=");
                sb.append(shadowTotal);
                if (shadowRate.length() > 0) {
                    sb.append(" 对照=");
                    sb.append(formatMaybeProbability(shadowRate));
                }
            }
            if (mainProblem.length() > 0) {
                sb.append("｜问题=");
                sb.append(translateLooseText(limitText(mainProblem, 130)));
            } else if (summary.length() > 0) {
                sb.append("｜");
                sb.append(translateLooseText(limitText(summary, 160)));
            }
        } else {
            sb.append(
                    translateLooseText(
                            limitText(firstNonEmpty(mainProblem, summary, "诊断正在生成"), 220)));
        }
        if (nextFix.length() > 0) {
            sb.append("｜建议=");
            sb.append(translateLooseText(limitText(nextFix, 130)));
        }
        return limitText(sb.toString(), 520);
    }

    private String buildV20CauseLine(LightSnapshot snapshot) {
        String block = buildV20StateBlock(snapshot);

        if (block.length() == 0) {
            return "";
        }

        return "V20动态因链：" + block;
    }

    private String buildV20StateBlock(LightSnapshot snapshot) {
        if (snapshot == null) {
            return "";
        }

        StringBuilder sb = new StringBuilder();

        appendV20One(
                sb,
                "因链",
                stateOf(snapshot, "dynamic_cause_chain_status"),
                stateOf(snapshot, "dynamic_cause_chain_summary"),
                stateOf(snapshot, "dynamic_cause_chain_score"),
                stateOf(snapshot, "dynamic_cause_chain_risk"),
                stateOf(snapshot, "dynamic_cause_chain_sample"));
        appendV20One(
                sb,
                "因子矩阵",
                stateOf(snapshot, "causal_factor_matrix_status"),
                stateOf(snapshot, "causal_factor_matrix_summary"),
                stateOf(snapshot, "causal_factor_matrix_score"),
                stateOf(snapshot, "causal_factor_matrix_risk"),
                stateOf(snapshot, "causal_factor_matrix_sample"));
        appendV20One(
                sb,
                "位置因果",
                stateOf(snapshot, "position_cause_matrix_status"),
                stateOf(snapshot, "position_cause_matrix_summary"),
                stateOf(snapshot, "position_cause_matrix_score"),
                stateOf(snapshot, "position_cause_matrix_risk"),
                stateOf(snapshot, "position_cause_matrix_sample"));
        appendV20One(
                sb,
                "教师号码",
                stateOf(snapshot, "teacher_number_matrix_status"),
                stateOf(snapshot, "teacher_number_matrix_summary"),
                stateOf(snapshot, "teacher_number_matrix_score"),
                stateOf(snapshot, "teacher_number_matrix_risk"),
                stateOf(snapshot, "teacher_number_matrix_sample"));
        appendV20One(
                sb,
                "反事实",
                stateOf(snapshot, "counterfactual_rank_status"),
                stateOf(snapshot, "counterfactual_rank_summary"),
                stateOf(snapshot, "counterfactual_rank_score"),
                stateOf(snapshot, "counterfactual_rank_risk"),
                stateOf(snapshot, "counterfactual_rank_sample"));
        appendV20One(
                sb,
                "第三防线",
                stateOf(snapshot, "third_guard_matrix_status"),
                stateOf(snapshot, "third_guard_matrix_summary"),
                stateOf(snapshot, "third_guard_matrix_score"),
                stateOf(snapshot, "third_guard_matrix_risk"),
                stateOf(snapshot, "third_guard_matrix_sample"));
        appendV20One(
                sb,
                "规则健康",
                stateOf(snapshot, "rule_health_matrix_status"),
                stateOf(snapshot, "rule_health_matrix_summary"),
                stateOf(snapshot, "rule_health_matrix_score"),
                stateOf(snapshot, "rule_health_matrix_risk"),
                stateOf(snapshot, "rule_health_matrix_sample"));
        appendV20One(
                sb,
                "影子健康",
                stateOf(snapshot, "shadow_health_gate_status"),
                stateOf(snapshot, "shadow_health_gate_summary"),
                stateOf(snapshot, "shadow_health_gate_score"),
                stateOf(snapshot, "shadow_health_gate_risk"),
                stateOf(snapshot, "shadow_health_gate_sample"));
        appendV20One(
                sb,
                "伪强冻结",
                stateOf(snapshot, "false_strong_freeze_status"),
                stateOf(snapshot, "false_strong_freeze_summary"),
                stateOf(snapshot, "false_strong_freeze_score"),
                stateOf(snapshot, "false_strong_freeze_risk"),
                stateOf(snapshot, "false_strong_freeze_sample"));
        appendV20One(
                sb,
                "种子扩展",
                stateOf(snapshot, "seed_cause_expansion_status"),
                stateOf(snapshot, "seed_cause_expansion_summary"),
                stateOf(snapshot, "seed_cause_expansion_score"),
                stateOf(snapshot, "seed_cause_expansion_risk"),
                stateOf(snapshot, "seed_cause_expansion_sample"));

        String service = stateOf(snapshot, "v20_cause_chain_summary");
        if (service.length() == 0) {
            service = stateOf(snapshot, "decision_v20_cause_summary");
        }

        if (service.length() > 0) {
            appendLine(sb, "总览：" + translateLooseText(limitText(service, 180)));
        }

        String evidence = buildV20EvidenceStateText(snapshot);
        if (evidence.length() > 0) {
            appendLine(sb, "可回放证据：" + evidence);
        }

        return sb.toString();
    }

    private void appendV20One(
            StringBuilder sb,
            String name,
            String status,
            String summary,
            String score,
            String risk,
            String sample) {
        if (sb == null) {
            return;
        }

        if (status.length() == 0
                && summary.length() == 0
                && score.length() == 0
                && risk.length() == 0
                && sample.length() == 0) {
            return;
        }

        StringBuilder line = new StringBuilder();
        line.append(name);
        line.append("=");

        if (status.length() > 0) {
            line.append(translateLooseText(status));
        } else {
            line.append("有数据");
        }

        if (score.length() > 0) {
            line.append("/分=");
            line.append(formatMaybeProbability(score));
        }

        if (risk.length() > 0) {
            line.append("/险=");
            line.append(formatMaybeProbability(risk));
        }

        if (sample.length() > 0) {
            line.append("/样本=");
            line.append(sample);
        }

        if (summary.length() > 0) {
            line.append("｜");
            line.append(translateLooseText(limitText(summary, 120)));
        }

        appendLine(sb, line.toString());
    }

    private String buildV20StateKey(LightSnapshot snapshot) {
        if (snapshot == null) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append(stateOf(snapshot, "v20_cause_chain_summary")).append("|");
        sb.append(stateOf(snapshot, "dynamic_cause_chain_summary")).append("|");
        sb.append(stateOf(snapshot, "causal_factor_matrix_summary")).append("|");
        sb.append(stateOf(snapshot, "position_cause_matrix_summary")).append("|");
        sb.append(stateOf(snapshot, "teacher_number_matrix_summary")).append("|");
        sb.append(stateOf(snapshot, "counterfactual_rank_summary")).append("|");
        sb.append(stateOf(snapshot, "third_guard_matrix_summary")).append("|");
        sb.append(stateOf(snapshot, "rule_health_matrix_summary")).append("|");
        sb.append(stateOf(snapshot, "shadow_health_gate_summary")).append("|");
        sb.append(stateOf(snapshot, "false_strong_freeze_summary")).append("|");
        sb.append(stateOf(snapshot, "seed_cause_expansion_summary")).append("|");
        sb.append(stateOf(snapshot, "decision_window_state_key")).append("|");
        sb.append(stateOf(snapshot, "final_decision_window_state_key")).append("|");
        sb.append(stateOf(snapshot, "dynamic_gate_window_state_key")).append("|");
        sb.append(stateOf(snapshot, "probability_window_state_key")).append("|");
        sb.append(stateOf(snapshot, "teacher_window_state_key")).append("|");
        sb.append(stateOf(snapshot, "dynamic_window_state_key")).append("|");
        sb.append(stateOf(snapshot, "state_key_12")).append("|");
        sb.append(stateOf(snapshot, "state_key_24")).append("|");
        sb.append(stateOf(snapshot, "state_key_48")).append("|");
        sb.append(stateOf(snapshot, "state_key_96")).append("|");
        sb.append(stateOf(snapshot, "state_key_360")).append("|");
        sb.append(stateOf(snapshot, "evidence_contribution")).append("|");
        sb.append(stateOf(snapshot, "counter_contribution")).append("|");
        sb.append(stateOf(snapshot, "sample_reliability")).append("|");
        sb.append(stateOf(snapshot, "recent_validity")).append("|");
        sb.append(stateOf(snapshot, "action_advice"));
        return sb.toString();
    }

    private String buildTeacherMatrixLine(LightSnapshot snapshot) {
        String status = stateOf(snapshot, "teacher_matrix_status");
        String running = stateOf(snapshot, "teacher_matrix_running");
        String summary = stateOf(snapshot, "teacher_matrix_summary");
        String sample = stateOf(snapshot, "teacher_matrix_sample_count");
        String profile = stateOf(snapshot, "teacher_matrix_profile_count");
        String strong = stateOf(snapshot, "teacher_matrix_strong_profile_count");
        String risk = stateOf(snapshot, "teacher_matrix_risk_profile_count");
        String cells = stateOf(snapshot, "teacher_matrix_last_cells");
        String cost = stateOf(snapshot, "teacher_matrix_last_cost_ms");

        if (status.length() == 0
                && summary.length() == 0
                && sample.length() == 0
                && profile.length() == 0
                && cells.length() == 0) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("教师矩阵：");
        sb.append(status.length() == 0 ? "空闲" : translateLooseText(status));

        if ("1".equals(running)) {
            sb.append("｜运行中");
        }

        if (sample.length() > 0) {
            sb.append("｜样本=");
            sb.append(sample);
        }

        if (profile.length() > 0) {
            sb.append("｜画像=");
            sb.append(profile);
        }

        if (strong.length() > 0) {
            sb.append("｜强=");
            sb.append(strong);
        }

        if (risk.length() > 0) {
            sb.append("｜风险=");
            sb.append(risk);
        }

        if (cells.length() > 0) {
            sb.append("｜本期格=");
            sb.append(cells);
        }

        if (cost.length() > 0) {
            sb.append("｜耗时=");
            sb.append(cost);
            sb.append("ms");
        }

        if (summary.length() > 0) {
            sb.append("｜");
            sb.append(translateLooseText(limitText(summary, 180)));
        }

        return sb.toString();
    }

    private String buildTeacherSnapshotLine(LightSnapshot snapshot) {
        String status = stateOf(snapshot, "teacher_snapshot_status");
        String summary = stateOf(snapshot, "teacher_snapshot_summary");
        String snapshotId = stateOf(snapshot, "teacher_snapshot_id");
        String conf = stateOf(snapshot, "teacher_snapshot_confidence");
        String valid = stateOf(snapshot, "teacher_snapshot_valid");

        if (status.length() == 0
                && summary.length() == 0
                && snapshotId.length() == 0
                && conf.length() == 0) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("教师快照：");
        sb.append(status.length() == 0 ? "空闲" : translateLooseText(status));

        if (snapshotId.length() > 0) {
            sb.append("｜ID=");
            sb.append(limitText(snapshotId, 32));
        }

        if (conf.length() > 0) {
            double c = parseDouble(conf, -1.0d);
            if (c >= 0.0d) {
                sb.append("｜置信=");
                sb.append(UiFormatter.probability(c));
            } else {
                sb.append("｜置信=");
                sb.append(conf);
            }
        }

        if (valid.length() > 0) {
            sb.append("｜有效=");
            sb.append("1".equals(valid) ? "是" : "否");
        }

        if (summary.length() > 0) {
            sb.append("｜");
            sb.append(translateLooseText(limitText(summary, 180)));
        }

        return sb.toString();
    }

    private String buildLearningLine(LightSnapshot snapshot) {
        String status = stateOf(snapshot, "auto_learn_status");
        String progress = stateOf(snapshot, "auto_learn_progress");
        String summary = stateOf(snapshot, "auto_learn_summary");
        String learning = stateOf(snapshot, "learning_summary");
        String reason = stateOf(snapshot, "learning_reason_detail");
        String topMiss = stateOf(snapshot, "learning_top_miss_reason");

        if (status.length() == 0
                && summary.length() == 0
                && learning.length() == 0
                && reason.length() == 0) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("学习线程：");
        sb.append(status.length() == 0 ? "空闲" : translateLooseText(status));

        if (progress.length() > 0 && progress.length() < 16) {
            sb.append("｜进度=");
            sb.append(progress);
        }

        if (summary.length() > 0) {
            sb.append("｜");
            sb.append(translateLooseText(limitText(summary, 160)));
        } else if (learning.length() > 0) {
            sb.append("｜");
            sb.append(translateLooseText(limitText(learning, 160)));
        }

        if (reason.length() > 0) {
            sb.append("\n学习反推：");
            sb.append(translateLooseText(limitText(reason, 180)));
        }

        if (topMiss.length() > 0) {
            sb.append("\n主要错因：");
            sb.append(translateLooseText(topMiss));
        }

        return sb.toString();
    }

    private String buildRuntimeProfileLine(LightSnapshot snapshot) {
        String runtime = stateOf(snapshot, "runtime_profile_summary");
        String sid = stateOf(snapshot, "runtime_learning_snapshot_id");

        if (sid.length() == 0) {
            sid = stateOf(snapshot, "runtime_snapshot_id");
        }

        if (sid.length() == 0) {
            sid = stateOf(snapshot, "last_snapshot_id");
        }

        if (runtime.length() == 0 && sid.length() == 0) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("画像反哺：");

        if (sid.length() > 0) {
            sb.append("快照=");
            sb.append(limitText(sid, 34));
        }

        if (runtime.length() > 0) {
            if (sid.length() > 0) {
                sb.append("｜");
            }
            sb.append(translateLooseText(limitText(runtime, 180)));
        }

        return sb.toString();
    }

    private String buildBacktestLine(LightSnapshot snapshot) {
        String status = stateOf(snapshot, "backtest_status");
        String mode = stateOf(snapshot, "backtest_mode");
        String progress = stateOf(snapshot, "backtest_progress");
        String window = stateOf(snapshot, "backtest_window");
        String summary = stateOf(snapshot, "backtest_last_summary");
        String error = stateOf(snapshot, "backtest_error");

        if (summary.length() == 0) {
            summary = stateOf(snapshot, "backtest_reason");
        }

        if ((status == null || status.length() == 0 || "IDLE".equals(status))
                && summary.length() == 0
                && error.length() == 0) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("回测线程：");
        sb.append(translateLooseText(status));

        if (mode != null && mode.length() > 0) {
            sb.append("｜");
            sb.append(translateLooseText(mode));
        }

        if (progress != null && progress.length() > 0) {
            sb.append("｜");
            sb.append(progress);
            sb.append("%");
        }

        if (window != null && window.length() > 0 && !"0".equals(window)) {
            sb.append("｜W");
            sb.append(window);
        }

        if (summary != null && summary.length() > 0) {
            sb.append("｜");
            sb.append(translateLooseText(limitText(summary, 160)));
        }

        if (error != null && error.length() > 0) {
            sb.append("｜错误=");
            sb.append(translateLooseText(limitText(error, 100)));
        }

        return sb.toString();
    }

    private String buildBackfillLine(LightSnapshot snapshot) {
        String status = stateOf(snapshot, "backfill_status");
        String msg = stateOf(snapshot, "backfill_message");
        String inserted = stateOf(snapshot, "backfill_inserted");

        if ((status == null || status.length() == 0)
                && (msg == null || msg.length() == 0)
                && (inserted == null || inserted.length() == 0)) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("回补线程：");
        sb.append(translateLooseText(status));

        if (inserted != null && inserted.length() > 0) {
            sb.append("｜新增=");
            sb.append(inserted);
        }

        if (msg != null && msg.length() > 0) {
            sb.append("｜");
            sb.append(translateLooseText(limitText(msg, 120)));
        }

        return sb.toString();
    }

    private void refreshCountdownOnly() {
        LightSnapshot snapshot = lastLightSnapshot;
        setText(txtCountdown, countdownTextR994(getBestCountdownFromState(
                snapshot == null ? null : snapshot.state, snapshot)));
    }

    private String countdownTextR994(long remaining) {
        if(remaining>0L)return UiFormatter.countdown(remaining);
        if(!KeyProvider.isKeyReady())return "等待接口配置";
        if(liveClockAnchorCP12==null||liveClockAnchorCP12.signedRemaining(uiLockedTargetIssue,
                liveClockAnchorCP12.revision,TimeSourceR811.clockSessionCP12(),android.os.SystemClock.elapsedRealtime())==CountdownProjectionR971.STALE_CLOCK)
            return "等待最新开奖";
        return "等待本期开奖";
    }

    private long getBestCountdownFromState(Models.AppRuntimeState state, LightSnapshot snapshot) {
        long now = android.os.SystemClock.elapsedRealtime();
        CountdownProjectionR971.Anchor current=CountdownClock.liveAnchorR72();
        acceptClockAnchorR76(current,now);
        // An old queued broadcast or an older DB read cannot move the active target backwards.
        boolean liveTarget = sameIssueText(liveCountdownTargetR971, uiLockedTargetIssue);
        // A DB refresh contains no clock anchor. Its later read timestamp cannot invalidate
        // the still-current anchor received from the sole clock owner.
        if (liveCountdownAt >= 0L && liveTarget) {
            if(liveClockAnchorCP12==null)return 0L;
            long remain=liveClockAnchorCP12.remaining(uiLockedTargetIssue,liveClockAnchorCP12.revision,TimeSourceR811.clockSessionCP12(),now);
            return remain==CountdownProjectionR971.STALE_CLOCK?0L:remain;
        }
        // Persistedwall timestamps remain diagnostic evidence. Display waits for the Clockowner's same-session anchor after process/reboot; UIdoesnotcreate another anchor from walltime.
        return 0L; // Waiting for a verified target, never fabricate another 60-second cycle.
    }

    /** The process clock already verified its source issue. Import that exact immutable anchor
     * even before a DB read or after a missed broadcast; never derive a new deadline in the UI. */
    private boolean acceptClockAnchorR76(CountdownProjectionR971.Anchor incoming,long now) {
        if(incoming==null||!shouldAcceptBroadcastTarget(incoming.targetIssue)
                ||incoming.signedRemaining(incoming.targetIssue,incoming.revision,
                    TimeSourceR811.clockSessionCP12(),now)==CountdownProjectionR971.STALE_CLOCK)return false;
        if(liveClockAnchorCP12!=null&&incoming.targetIssue.equals(liveClockAnchorCP12.targetIssue)
                &&incoming.session.equals(liveClockAnchorCP12.session)&&incoming.revision<liveClockAnchorCP12.revision)return false;
        liveClockAnchorCP12=incoming;liveCountdownTargetR971=incoming.targetIssue;
        liveCountdownAt=incoming.sampledMonoMs;
        handleTargetIssueChanged(null,incoming.targetIssue,"clock_owner_r76");
        return true;
    }

    private void stabilizeSameTargetDecisionEvidenceR901(Models.Decision current, Models.Decision previous) {
        if (current == null || previous == null) return;
        if (!sameIssueText(current.targetIssue, previous.targetIssue)) return;
        /* All copied fields belong to a first-write-wins pre-draw canonical row.  If a single
         * read omits them, retaining the last same-target value is safer than visually regressing
         * to zero/N/A and does not invent or recompute any model output. */
        if ((current.shapeKey == null || current.shapeKey.length() == 0)
                && previous.shapeKey != null && previous.shapeKey.length() > 0) {
            current.shapeKey = previous.shapeKey;
            current.rawP = previous.rawP;
            current.modelP = previous.modelP;
            current.effectiveSample = previous.effectiveSample;
            current.championGap = previous.championGap;
            current.trendLife = previous.trendLife;
            current.driftRisk = previous.driftRisk;
            current.comboGap = previous.comboGap;
            current.recoveryScore = previous.recoveryScore;
        }
        if (!isPositiveFiniteUiR3610(current.dynamicGate)
                && isPositiveFiniteUiR3610(previous.dynamicGate)) {
            current.dynamicGate = previous.dynamicGate;
            current.riskAdjustedGate = previous.riskAdjustedGate;
        }
        if (!isFiniteUiR3610(current.edge) && isFiniteUiR3610(previous.edge)) {
            current.edge = previous.edge;
            current.rawEdge = previous.rawEdge;
        }
    }

    private void refreshDecisionCard(
            Models.Decision decision,
            Models.Trade trade,
            Models.AppRuntimeState state,
            LightSnapshot snapshot) {
        String targetIssue = snapshot == null ? "" : snapshot.targetIssue;

        if (targetIssue.length() == 0) {
            targetIssue =
                    resolveTargetIssue(
                            state,
                            snapshot == null ? null : snapshot.states,
                            snapshot == null ? null : snapshot.latestDraw);
        }

        if (!isDecisionForTarget(decision, targetIssue)) {
            decision = null;
            trade = null;
        }

        if (trade != null && !isTradeForTarget(trade, targetIssue)) {
            trade = null;
        }

        String renderKey = buildDecisionRenderKey(decision, trade, state, snapshot);

        if (renderKey.equals(lastDecisionRenderKey)) {
            return;
        }

        lastDecisionRenderKey = renderKey;

        if (decision == null) {
            setText(txtDecisionLabel, "目标期：" + safeDash(targetIssue));
            setText(txtDecision, noDecisionTitleR45(
                    targetIssue, stateOf(snapshot, "analysis_status"),
                    stateOf(snapshot, "r365_last_analysis_failure_target"),
                    stateOf(snapshot, "r365_last_analysis_failure")));
            setTextColor(txtDecision, R.color.colorHold);

            if (targetIssue != null && targetIssue.length() > 0) {
                setText(txtPositionNumbers, "当前目标期尚未生成正式预测");
            } else {
                setText(txtPositionNumbers, "等待数据");
            }

            setText(txtDecisionSummary, getNoDecisionText(snapshot));
            return;
        }

        String d =
                decision.decision == null || decision.decision.length() == 0
                        ? AppConfig.DECISION_HOLD
                        : decision.decision;

        setText(txtDecisionLabel, "目标期：" + safeDash(decision.targetIssue));
        setText(txtDecision, UiFormatter.decisionText(d));
        setTextColor(txtDecision, UiFormatter.decisionColor(d));

        if (AppConfig.DECISION_BET.equals(d)) {
            setText(
                    txtPositionNumbers,
                    "第"
                            + decision.position
                            + "位置  "
                            + UiFormatter.numberGroup(decision.n1, decision.n2, decision.n3));
        } else if (AppConfig.DECISION_SHADOW.equals(d)) {
            if (decision.position > 0 && Models.isValidK3Mask(decision.k3Mask)) {
                setText(
                        txtPositionNumbers,
                        "影子观察 第"
                                + decision.position
                                + "位置  "
                                + UiFormatter.numberGroup(decision.n1, decision.n2, decision.n3));
            } else {
                setText(txtPositionNumbers, "影子观察");
            }
        } else {
            if (decision.position > 0 && Models.isValidK3Mask(decision.k3Mask)) {
                setText(
                        txtPositionNumbers,
                        "不出手观察 第"
                                + decision.position
                                + "位置  "
                                + UiFormatter.numberGroup(decision.n1, decision.n2, decision.n3));
            } else {
                setText(txtPositionNumbers, "本期不出手");
            }
        }

        String summary = buildFastDecisionSummary(decision, trade, snapshot);
        setText(txtDecisionSummary, AlgorithmAttributionR943.line(snapshot.states,decision.targetIssue)+"\n"+translateLooseText(summary));
    }

    private String buildFastDecisionSummary(
            Models.Decision decision, Models.Trade trade, LightSnapshot snapshot) {
        if (decision == null) {
            return getNoDecisionText(snapshot);
        }

        /* R901 home-card contract: compact immutable canonical truth only. Detailed V20,
         * Teacher, learning, dynamic-domain and counterfactual text remains available in the
         * diagnosis/reason screens and is deliberately excluded here so background evidence
         * materialization can never resize the main card. */
        if (trade != null && isSettlementResultText(trade.result)) {
            return limitText(removePnlText(buildTradeSettlementUiText(trade)), 300);
        }
        if (isSettlementResultText(decision.result)) {
            return limitText(removePnlText(buildDecisionSettlementUiText(decision)), 300);
        }

        StringBuilder sb = new StringBuilder(320);
        sb.append("赛前冻结=").append(safeDash(decision.targetIssue));
        sb.append("｜动作=").append(UiFormatter.decisionText(decision.decision));
        if (decision.blockCode != null && decision.blockCode.length() > 0) {
            sb.append("｜拦截=").append(translateLooseText(limitText(decision.blockCode, 48)));
        }

        sb.append('\n');
        sb.append("K3=");
        if (decision.position > 0 && Models.isValidK3Mask(decision.k3Mask)) {
            sb.append("P").append(decision.position).append(':')
                    .append(UiFormatter.numberGroup(decision.n1, decision.n2, decision.n3));
        } else {
            sb.append("N/A");
        }
        sb.append("｜SelectedHitP=").append(probabilityUiR3610(decision.finalP));
        sb.append("｜Gate=").append(probabilityUiR3610(decision.dynamicGate));

        if ("R900_V51_SELECTED_EVIDENCE".equals(decision.shapeKey)) {
            sb.append('\n');
            sb.append("证据 Raw=").append(probabilityUiR3610(decision.rawP));
            sb.append("｜Rank=").append(decision.effectiveSample > 0 ? decision.effectiveSample : 0).append("/1200");
            sb.append("｜Third=").append(signedUiR900(decision.championGap));
            sb.append("｜Stable=").append(decision.trendLife >= 0 ? UiFormatter.probability(decision.trendLife) : "N/A");
        } else {
            sb.append("\n证据=等待同一期Canonical证据物化");
        }
        sb.append("\n完整因果/Teacher/动态域请打开‘为什么中/不中/不出手’或完整诊断");
        return limitText(removePnlText(sb.toString()), 380);
    }

    private String buildLinkModificationV2DecisionLine(Object decision, LightSnapshot snapshot) {
        if (decision == null) return "";
        double baseGate = firstV20Double(decision, snapshot, new String[] {"baseGate"}, null, 0.0d);
        double riskGate =
                firstV20Double(
                        decision,
                        snapshot,
                        new String[] {"riskAdjustedGate", "dynamicGate"},
                        null,
                        0.0d);
        double adjust =
                firstV20Double(
                        decision, snapshot, new String[] {"frequencyGateAdjust"}, null, 0.0d);
        double probeGate =
                firstV20Double(decision, snapshot, new String[] {"effectiveProbeGate"}, null, 0.0d);
        double rawEdge =
                firstV20Double(decision, snapshot, new String[] {"rawEdge", "edge"}, null, 0.0d);
        double cover =
                firstV20Double(
                        decision,
                        snapshot,
                        new String[] {"coverageQuality", "coverageQualityScore"},
                        null,
                        -1.0d);
        double third =
                firstV20Double(
                        decision,
                        snapshot,
                        new String[] {"thirdQuality", "thirdNumberQualityScore"},
                        null,
                        -1.0d);
        double need =
                firstV20Double(
                        decision,
                        snapshot,
                        new String[] {"needSupplyScore", "needSupplyMatchScore"},
                        null,
                        -1.0d);
        double counter =
                firstV20Double(
                        decision,
                        snapshot,
                        new String[] {
                            "counterRisk", "counterfactualWhyNotRisk", "counterfactualRisk"
                        },
                        null,
                        -1.0d);
        String domain =
                firstNonEmpty(
                        getFieldText(decision, "betDomain"),
                        getFieldText(decision, "finalDecisionBetDomain"));
        String path = getFieldText(decision, "decisionPath");
        String promotion =
                firstNonEmpty(
                        getFieldText(decision, "shadowPromotionStatus"),
                        getFieldText(decision, "promotionStatus"));
        String adaptive =
                firstNonEmpty(
                        getFieldText(decision, "adaptiveStatus"),
                        getFieldText(decision, "adaptiveJudgeStatus"));
        String rule = getFieldText(decision, "ruleHealthStatus");
        String snapshotFresh = getFieldText(decision, "snapshotFreshness");
        StringBuilder sb = new StringBuilder();
        sb.append("链路V2");
        if (domain.length() > 0) sb.append("｜BET域=").append(domain);
        if (path.length() > 0) sb.append("｜路径=").append(path);
        if (baseGate > 0.0d) sb.append("｜BaseGate=").append(UiFormatter.probability(baseGate));
        if (riskGate > 0.0d) sb.append("｜正常Gate=").append(UiFormatter.probability(riskGate));
        if (probeGate > 0.0d) sb.append("｜探针Gate=").append(UiFormatter.probability(probeGate));
        if (adjust != 0.0d) sb.append("｜频率调门=").append(UiFormatter.signedProbability(adjust));
        sb.append("｜RawEdge=").append(UiFormatter.signedProbability(rawEdge));
        sb.append("｜Cover=").append(formatOptionalProbability(cover));
        sb.append("｜Third=").append(formatOptionalProbability(third));
        sb.append("｜Need=").append(formatOptionalProbability(need));
        sb.append("｜Counter=").append(formatOptionalProbability(counter));
        if (promotion.length() > 0) sb.append("｜Promotion=").append(promotion);
        if (adaptive.length() > 0) sb.append("｜Adaptive=").append(adaptive);
        if (rule.length() > 0) sb.append("｜Rule=").append(rule);
        if (snapshotFresh.length() > 0) sb.append("｜Snapshot=").append(snapshotFresh);
        sb.append("｜质量底线=").append(UiFormatter.probability(AppConfig.QUALIFIED_FLOOR_MIN_FINAL_P));
        sb.append("｜Freq20/50/100/300=")
                .append(
                        formatOptionalProbability(
                                readDouble(decision, "realBetFrequency20", -1.0d)))
                .append("/")
                .append(
                        formatOptionalProbability(
                                readDouble(decision, "realBetFrequency50", -1.0d)))
                .append("/")
                .append(
                        formatOptionalProbability(
                                readDouble(decision, "realBetFrequency100", -1.0d)))
                .append("/")
                .append(
                        formatOptionalProbability(
                                readDouble(decision, "realBetFrequency300", -1.0d)));
        sb.append("｜补频节奏=")
                .append(getFieldText(decision, "frequencyFloorPacingReason"))
                .append(" age=")
                .append(readInt(decision, "frequencyIssuesSinceLastRealBet", 999))
                .append(" gap=")
                .append(readInt(decision, "frequencyFloorMinGap", 0));
        sb.append("｜Teacher样本=")
                .append(readInt(decision, "teacherNumberHistorySample", 0))
                .append("+")
                .append(readInt(decision, "teacherDomainShadowSample", 0))
                .append("+")
                .append(readInt(decision, "teacherDomainRealSample", 0));
        String dm = getFieldText(decision, "dynamicMatchedLevel");
        if (dm.length() > 0)
            sb.append("｜Dynamic=")
                    .append(dm)
                    .append(" R/S=")
                    .append(readInt(decision, "dynamicRealSample", 0))
                    .append("/")
                    .append(readInt(decision, "dynamicShadowSample", 0));
        return limitText(sb.toString(), 720);
    }

    private String formatOptionalProbability(double v) {
        if (v < 0.0d || Double.isNaN(v) || Double.isInfinite(v)) return "未计算";
        return UiFormatter.probability(v);
    }

    private String buildDecisionV20Text(Models.Decision decision, LightSnapshot snapshot) {
        StringBuilder sb = new StringBuilder();

        String cause =
                firstNonEmpty(
                        getFieldText(decision, "v20Summary"),
                        getFieldText(decision, "v20CauseSummary"),
                        getFieldText(decision, "causeChainSummary"),
                        stateOf(snapshot, "decision_v20_cause_summary"),
                        stateOf(snapshot, "decision_cause_chain_summary"));

        String rule =
                firstNonEmpty(
                        getFieldText(decision, "ruleHealthSummary"),
                        stateOf(snapshot, "decision_rule_health_summary"));

        String third =
                firstNonEmpty(
                        getFieldText(decision, "thirdGuardSummary"),
                        stateOf(snapshot, "decision_third_guard_summary"));

        String shadow =
                firstNonEmpty(
                        getFieldText(decision, "shadowHealthSummary"),
                        stateOf(snapshot, "decision_shadow_health_summary"));

        String freeze =
                firstNonEmpty(
                        getFieldText(decision, "falseStrongSummary"),
                        getFieldText(decision, "falseStrongFreezeSummary"),
                        stateOf(snapshot, "decision_false_strong_summary"));

        String cf =
                firstNonEmpty(
                        getFieldText(decision, "counterfactualSummary"),
                        getFieldText(decision, "counterfactualRankSummary"),
                        stateOf(snapshot, "decision_counterfactual_summary"));

        String seed =
                firstNonEmpty(
                        getFieldText(decision, "seedExpandSummary"),
                        getFieldText(decision, "seedCauseSummary"),
                        stateOf(snapshot, "decision_seed_expand_summary"));

        if (cause.length() > 0) {
            appendLine(sb, "V20因链：" + translateLooseText(limitText(cause, 120)));
        }

        if (rule.length() > 0) {
            appendLine(sb, "规则健康：" + translateLooseText(limitText(rule, 100)));
        }

        if (third.length() > 0) {
            appendLine(sb, "第三防线：" + translateLooseText(limitText(third, 100)));
        }

        if (shadow.length() > 0) {
            appendLine(sb, "影子健康：" + translateLooseText(limitText(shadow, 100)));
        }

        if (freeze.length() > 0) {
            appendLine(sb, "伪强冻结：" + translateLooseText(limitText(freeze, 100)));
        }

        if (cf.length() > 0) {
            appendLine(sb, "反事实：" + translateLooseText(limitText(cf, 100)));
        }

        if (seed.length() > 0) {
            appendLine(sb, "种子扩展：" + translateLooseText(limitText(seed, 100)));
        }

        String evidence = buildV20EvidenceUiText(decision, snapshot, UI_V20_EVIDENCE_SHORT_LIMIT);
        if (evidence.length() > 0) {
            appendLine(sb, "可回放证据：" + evidence);
        }

        return sb.toString();
    }

    private String buildTradeSettlementUiText(Models.Trade trade) {
        if (trade == null) {
            return "";
        }

        StringBuilder sb = new StringBuilder();

        sb.append("已结算：");
        sb.append(UiFormatter.resultText(trade.result));

        sb.append("｜目标期=");
        sb.append(safeDash(trade.targetIssue));

        sb.append("｜第");
        sb.append(trade.position);
        sb.append("位置");

        sb.append("｜投注=");
        sb.append(UiFormatter.numberGroup(trade.n1, trade.n2, trade.n3));

        if (trade.actualNumber > 0) {
            sb.append("｜实际=");
            sb.append(UiFormatter.number(trade.actualNumber));
        }

        if (AppConfig.RESULT_MISS.equals(trade.result)
                && trade.missReason != null
                && trade.missReason.length() > 0) {
            sb.append("｜错因=");
            sb.append(translateLooseText(trade.missReason));
        }

        return sb.toString();
    }

    private String buildDecisionSettlementUiText(Models.Decision d) {
        if (d == null) {
            return "";
        }

        StringBuilder sb = new StringBuilder();

        sb.append("已结算：");
        sb.append(UiFormatter.resultText(d.result));

        sb.append("｜目标期=");
        sb.append(safeDash(d.targetIssue));

        sb.append("｜第");
        sb.append(d.position);
        sb.append("位置");

        sb.append("｜投注=");
        sb.append(UiFormatter.numberGroup(d.n1, d.n2, d.n3));

        int actual = readInt(d, "actualNumber", 0);
        if (actual <= 0) {
            actual = readInt(d, "settleNumber", 0);
        }

        if (actual > 0) {
            sb.append("｜实际=");
            sb.append(UiFormatter.number(actual));
        }

        String missReason = getFieldText(d, "missReason");
        if (AppConfig.RESULT_MISS.equals(d.result)
                && missReason != null
                && missReason.length() > 0) {
            sb.append("｜错因=");
            sb.append(translateLooseText(missReason));
        }

        return sb.toString();
    }

    private String buildDecisionRenderKey(
            Models.Decision d,
            Models.Trade t,
            Models.AppRuntimeState state,
            LightSnapshot snapshot) {
        String currentTarget = snapshot == null ? "" : snapshot.targetIssue;

        if (currentTarget.length() == 0 && state != null && state.targetIssue != null) {
            currentTarget = state.targetIssue;
        }

        if (d == null) {
            return "NONE|"
                    + currentTarget
                    + "|"
                    + stateOf(snapshot, "analysis_status")
                    + "|"
                    + stateOf(snapshot, "analysis_summary")
                    + "|"
                    + buildV20StateKey(snapshot)
                    + "|"
                    + buildDynamicCausalStateKey(snapshot);
        }

        StringBuilder sb = new StringBuilder();
        sb.append("CURRENT=").append(currentTarget).append("|");
        sb.append(d.targetIssue).append("|");
        sb.append(d.decision).append("|");
        sb.append(d.position).append("|");
        sb.append(d.n1).append("|");
        sb.append(d.n2).append("|");
        sb.append(d.n3).append("|");
        sb.append(d.k3Mask).append("|");
        sb.append(d.finalP).append("|");
        sb.append(d.dynamicGate).append("|");
        sb.append(d.edge).append("|");
        sb.append(d.result).append("|");
        sb.append(d.settled).append("|");
        sb.append(readInt(d, "actualNumber", 0)).append("|");
        sb.append(d.blockCode).append("|");
        sb.append(limitText(d.reasonSummary, 100)).append("|");
        sb.append(getFieldText(d, "teacherMatrixSummary")).append("|");
        sb.append(getFieldText(d, "learningSummary")).append("|");
        sb.append(buildDecisionV20Text(d, snapshot)).append("|");
        sb.append(buildV20EvidenceUiText(d, snapshot, 260)).append("|");
        sb.append(buildDynamicCausalDecisionLine(d, snapshot)).append("|");
        sb.append(buildLinkModificationV2DecisionLine(d, snapshot)).append("|");
        sb.append(buildV20StateKey(snapshot)).append("|");
        sb.append(buildDynamicCausalStateKey(snapshot)).append("|");

        if (t != null) {
            sb.append(t.result).append("|");
            sb.append(t.settled).append("|");
            sb.append(t.position).append("|");
            sb.append(t.n1).append("|");
            sb.append(t.n2).append("|");
            sb.append(t.n3).append("|");
            sb.append(t.actualNumber).append("|");
            sb.append(t.missReason);
        }

        return sb.toString();
    }

    /** Pending UI is tied to this exact target; it never constructs a prediction. */
    static String noDecisionTitleR45(String target, String status, String failedTarget, String failure) {
        boolean currentFailure = target != null && !target.isEmpty() && target.equals(failedTarget)
                && failure != null && !failure.isEmpty();
        if ("RUNNING".equals(status)) return "正在分析";
        if (currentFailure || "ERROR".equals(status)) return "分析异常，等待重试";
        if ("WAITING".equals(status)) return "分析已排队";
        return "等待分析";
    }

    private String getNoDecisionText(LightSnapshot snapshot) {
        if (snapshot == null) {
            return "暂无决策，等待后台服务运行";
        }

        String analysisStatus = stateOf(snapshot, "analysis_status");
        String analysisSummary = stateOf(snapshot, "analysis_summary");
        String target = snapshot.targetIssue == null ? "" : snapshot.targetIssue;
        String current = snapshot.currentIssue == null ? "" : snapshot.currentIssue;

        if (target.length() == 0) {
            target = stateOf(snapshot, "target_issue");
        }

        if (current.length() == 0) {
            current = stateOf(snapshot, "current_issue");
        }

        StringBuilder sb = new StringBuilder();

        if (target.length() > 0) {
            sb.append("目标期：");
            sb.append(target);
            sb.append("。");
        }

        if (current.length() > 0) {
            sb.append("当前开奖：");
            sb.append(current);
            sb.append("。");
        }

        String failedTarget=stateOf(snapshot,"r365_last_analysis_failure_target");
        String failure=stateOf(snapshot,"r365_last_analysis_failure");
        if(sameIssueText(target,failedTarget)&&!failure.isEmpty()&&!"RUNNING".equals(analysisStatus)){
            sb.append("本期分析未完成，原因：").append(failure).append("。状态：").append(stateOf(snapshot,"r365_last_analysis_transition")).append("。后台仅在本期开奖前重试；详细证据见完整报告。");return sb.toString();
        }
        String analysisTarget=stateOf(snapshot,"analysis_execution_target");
        if(!analysisTarget.isEmpty()&&!sameIssueText(target,analysisTarget)){
            sb.append("分析线程正在处理期号 ").append(analysisTarget).append("；当前目标期 ").append(target).append(" 等待单一分析线程接手。");
            sb.append("阶段：").append(stateOf(snapshot,"analysis_execution_stage"));return sb.toString();
        }
        if (analysisStatus.length() > 0 || analysisSummary.length() > 0) {
            sb.append("当前目标期正在等待或分析中：");
            sb.append(translateLooseText(analysisStatus));
            if (analysisSummary.length() > 0) {
                sb.append("｜");
                sb.append(translateLooseText(limitText(analysisSummary, 160)));
            }
            sb.append("。不会用上一期预测替代当前期。");
            return sb.toString();
        }

        if (snapshot.drawCount < AppConfig.V46_CONTROLLED_START_MIN_ISSUES) {
            sb.append("历史数据不足30期，当前");
            sb.append(snapshot.drawCount);
            sb.append("期，先抓取回补。");
        } else {
            sb.append("已有数据，等待当前目标期分析线程生成最终决策。不会显示上一期旧号码。");
        }

        return sb.toString();
    }

    private static boolean isFiniteUiR3610(double value) {
        return !Double.isNaN(value) && !Double.isInfinite(value);
    }

    private static boolean isPositiveFiniteUiR3610(double value) {
        return isFiniteUiR3610(value) && value > 0.0d;
    }

    private static String probabilityUiR3610(double value) {
        return isPositiveFiniteUiR3610(value) ? UiFormatter.probability(value) : "--";
    }

    private static String signedProbabilityUiR3610(double value) {
        return isFiniteUiR3610(value) ? UiFormatter.signedProbability(value) : "--";
    }

    private void refreshProbabilityCard(Models.Decision d) {
        if (d == null) {
            setText(txtFinalP, "--");
            setText(txtDynamicGate, "--");
            setText(txtEdge, "--");
            setTextColor(txtEdge, R.color.colorTextSub);
            return;
        }

        double edge = d.edge;
        if (edge == 0.0d && d.finalP > 0.0d && d.dynamicGate > 0.0d) {
            edge = d.finalP - d.dynamicGate;
        }

        double riskGate =
                firstV20Double(
                        d,
                        null,
                        new String[] {"riskAdjustedGate", "dynamicGate"},
                        null,
                        d.dynamicGate);
        double probeGate = firstV20Double(d, null, new String[] {"effectiveProbeGate"}, null, 0.0d);
        double rawEdge = firstV20Double(d, null, new String[] {"rawEdge", "edge"}, null, edge);
        String betDomain =
                firstNonEmpty(
                        getFieldText(d, "betDomain"), getFieldText(d, "finalDecisionBetDomain"));
        String finalText;
        if (isPositiveFiniteUiR3610(d.finalP)) {
            finalText = UiFormatter.probability(d.finalP);
        } else {
            double rawP = firstV20Double(d, null, new String[] {"rawP", "modelP"}, null, Double.NaN);
            finalText =
                    isPositiveFiniteUiR3610(rawP)
                            ? "未校准\n原始 " + UiFormatter.probability(rawP)
                            : "未生成";
        }
        setText(txtFinalP, finalText);
        setText(txtDynamicGate, probabilityUiR3610(riskGate));
        setText(txtEdge, signedProbabilityUiR3610(rawEdge));

        if (rawEdge >= 0.0d) {
            setTextColor(txtEdge, R.color.colorWin);
        } else {
            setTextColor(txtEdge, R.color.colorMiss);
        }
    }

    private void refreshShapeCard(Models.Decision d) {
        if (d == null) {
            setText(txtStateMode, "状态：--");
            setText(txtTrendLife, "趋势寿命：--");
            setText(txtDriftRisk, "漂移风险：--");
            setText(txtBreakDepth, "破位深度：--");
            setText(txtRecoveryScore, "恢复分：--");

            setTextColor(txtStateMode, R.color.colorTextSub);
            setTextColor(txtDriftRisk, R.color.colorTextSub);
            setTextColor(txtBreakDepth, R.color.colorTextSub);
            return;
        }

        if ("R900_V51_SELECTED_EVIDENCE".equals(d.shapeKey)) {
            String lifecycle = firstNonEmpty(d.stateMode, "UNKNOWN");
            String prototype = firstNonEmpty(d.stateKey, "未冻结原型");
            setText(txtStateMode, "形态：" + lifecycle + "｜" + prototype + "｜" + firstNonEmpty(d.trendKey, "run=N/A"));
            setText(txtTrendLife, d.trendLife >= 0 ? "动作稳定性：" + UiFormatter.probability(d.trendLife) : "动作稳定性：N/A（冻结字段缺失）");
            setText(txtDriftRisk, d.driftRisk >= 0 ? "号码熵风险：" + UiFormatter.probability(d.driftRisk) : "号码熵风险：N/A（冻结字段缺失）");
            setText(txtBreakDepth, "第三边界：" + signedUiR900(d.championGap));
            setText(txtRecoveryScore, d.recoveryScore >= 0
                    ? "跨位支持：" + UiFormatter.probability(d.recoveryScore) + "｜" + firstNonEmpty(d.breakKey, "changeRisk=N/A")
                    : "跨位支持：N/A（冻结字段缺失）｜" + firstNonEmpty(d.breakKey, "changeRisk=N/A"));
            setTextColor(txtStateMode, R.color.colorWin);
            setTextColor(txtDriftRisk, R.color.colorTextMain);
            setTextColor(txtBreakDepth, d.championGap < 0 ? R.color.colorMiss : R.color.colorTextMain);
            return;
        }
        if (d.reasonSummary != null && d.reasonSummary.indexOf("R3.6预开奖冻结Canonical") >= 0) {
            setText(txtStateMode, "状态：等待100号码/1200动作延后证据");
            setText(txtTrendLife, "动作稳定性：N/A");
            setText(txtDriftRisk, "号码熵风险：N/A");
            setText(txtBreakDepth, "第三边界：N/A");
            setText(txtRecoveryScore, "跨位支持：N/A");
            setTextColor(txtStateMode, R.color.colorTextSub);
            setTextColor(txtDriftRisk, R.color.colorTextSub);
            setTextColor(txtBreakDepth, R.color.colorTextSub);
            return;
        }

        String mode =
                d.stateMode == null || d.stateMode.length() == 0
                        ? AppConfig.MODE_CHAOS
                        : d.stateMode;

        setText(txtStateMode, "状态：" + UiFormatter.modeText(mode));
        setText(txtTrendLife, "趋势寿命：" + UiFormatter.score(d.trendLife));
        setText(txtDriftRisk, "漂移风险：" + UiFormatter.score(d.driftRisk));
        setText(txtBreakDepth, "破位深度：" + UiFormatter.score(d.breakDepth));
        setText(txtRecoveryScore, "恢复分：" + UiFormatter.score(d.recoveryScore));

        setTextColor(txtStateMode, UiFormatter.modeColor(mode));
        setTextColor(
                txtDriftRisk,
                d.driftRisk >= AppConfig.DRIFT_RISK_MAX
                        ? R.color.colorMiss
                        : R.color.colorTextMain);
        setTextColor(
                txtBreakDepth,
                d.breakDepth >= AppConfig.BREAK_DEPTH_MAX
                        ? R.color.colorMiss
                        : R.color.colorTextMain);
    }

    private static String signedUiR900(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) return "N/A";
        return (value >= 0.0d ? "+" : "") + String.format(java.util.Locale.US, "%.4f", value);
    }

    private void refreshProbabilityDetailCard(Models.Decision d) {
        if (d == null) {
            setText(txtSimilarHitRate, "相似命中率：--");
            setText(txtEffectiveSample, "有效样本：--");
            setText(txtComboHitRate, "组合命中率：--");
            setText(txtChampionGap, "主优势差：--");
            setText(txtComboGap, "组合优势差：--");
            return;
        }

        if ("R900_V51_SELECTED_EVIDENCE".equals(d.shapeKey)) {
            setText(txtSimilarHitRate, d.rawP >= 0 ? "原始动作K3概率（未校准）：" + UiFormatter.probability(d.rawP) : "原始动作K3概率（未校准）：N/A（冻结字段缺失）");
            setText(txtEffectiveSample, "同一期动作Rank：" + (d.effectiveSample > 0 ? d.effectiveSample : 0) + "/1200");
            setText(txtComboHitRate, d.comboGap >= 0 ? "窗口共识：" + UiFormatter.probability(d.comboGap) : "窗口共识：N/A");
            setText(txtChampionGap, "第三边界：" + signedUiR900(d.championGap));
            setText(txtComboGap, d.recoveryScore >= 0 ? "跨位支持：" + UiFormatter.probability(d.recoveryScore) : "跨位支持：N/A");
            return;
        }
        setText(txtSimilarHitRate, "相似命中率：" + UiFormatter.probability(d.similarHitRate));
        String v20EvidenceBrief = buildV20EvidenceBriefForMiniCard(d);
        String dynamicBrief = buildDynamicCausalMiniText(d);
        String extra = v20EvidenceBrief;
        if (dynamicBrief.length() > 0) {
            extra = appendReason(extra, dynamicBrief);
        }
        if (extra.length() > 0) {
            setText(txtEffectiveSample, "有效样本：" + d.effectiveSample + "｜" + extra);
        } else {
            setText(txtEffectiveSample, "有效样本：" + d.effectiveSample);
        }
        setText(txtComboHitRate, "组合命中率：" + UiFormatter.probability(d.comboHitRate));
        setText(txtChampionGap, "主优势差：" + UiFormatter.probability(d.championGap));
        setText(txtComboGap, "组合优势差：" + UiFormatter.probability(d.comboGap));
    }

    private void refreshStatsCard(Models.RuntimeStats stats, LightSnapshot snapshot) {
        String key = buildStatsRenderKey(stats, snapshot);

        if (key.equals(lastStatsRenderKey)) {
            return;
        }

        lastStatsRenderKey = key;

        if (stats == null) {
            setText(txtStatsLine1, "真实出手：0  命中：0  未中：0");
            setText(txtStatsLine2, "胜率：--  频率：--  盈亏：--");
            setText(txtStatsLine3, "最大回撤：--｜HOLD/SHADOW不计真实胜率与盈亏");
            return;
        }

        setText(txtStatsLine1, UiFormatter.statsLine1(stats));
        setText(txtStatsLine2, UiFormatter.statsLine2(stats));

        StringBuilder line3 = new StringBuilder();
        line3.append(UiFormatter.statsLine3(stats));
        line3.append("｜真实统计=trades + settled=1 + BET + REAL_TRADE");
        line3.append("｜HOLD/SHADOW/后台学习/教师矩阵/V20画像不计真实胜率/盈亏");
        line3.append("\n冻结SHADOW闭环：预测=")
                .append(stats.shadowObservationCount)
                .append("｜已结算=")
                .append(stats.shadowSettledCount)
                .append("｜待结算=")
                .append(Math.max(0, stats.shadowObservationCount - stats.shadowSettledCount))
                .append("｜命中/未中=")
                .append(stats.shadowHitCount)
                .append('/')
                .append(stats.shadowMissCount)
                .append("｜命中率=")
                .append(
                        stats.shadowSettledCount > 0
                                ? UiFormatter.probability(stats.shadowHitRate)
                                : "N/A")
                .append("（仅SHADOW，禁止计入真实盈亏；小样本不作模型证明）");
        int observedR900 = Math.max(0, stats.betCount + stats.holdCount + stats.shadowCount);
        line3.append("\nREAL频率合同：证明后最近")
                .append(AppConfig.REAL_BET_FREQUENCY_LOOKBACK)
                .append("期下限=").append(UiFormatter.probability(AppConfig.REAL_BET_HARD_MIN_FREQUENCY))
                .append("｜目标=").append(UiFormatter.probability(AppConfig.REAL_BET_TARGET_FREQUENCY))
                .append("｜当前=").append(observedR900 > 0 ? UiFormatter.probability(((double) stats.betCount)/observedR900) : "N/A")
                .append("｜MODEL_PROVEN=false时只统计不补单、不欠债。");

        String audit = safeText(getFieldText(stats, "auditSummary"));
        String diagnosis = safeText(getFieldText(stats, "selfDiagnosisSummary"));
        String realBetDiagnosis = stateOf(snapshot, "real_bet_diagnosis_summary");
        String dynamicCausalDiagnosis = buildDynamicCausalDiagnosisLine(snapshot);
        String learning = safeText(getFieldText(stats, "learningSummary"));
        String teacher = stateOf(snapshot, "teacher_matrix_summary");
        String snap = stateOf(snapshot, "teacher_snapshot_summary");
        String runtime = stateOf(snapshot, "runtime_profile_summary");
        String v20 = buildV20StateBlock(snapshot);
        String settlement = stateOf(snapshot, "last_strict_trade_settlement_detail");

        if (settlement.length() == 0) {
            settlement = stateOf(snapshot, "last_settlement_detail");
        }

        if (settlement.length() > 0) {
            line3.append("\n最近结算：");
            line3.append(removePnlText(translateLooseText(limitText(settlement, 120))));
        }

        if (v20.length() > 0) {
            line3.append("\nV20因链：");
            line3.append(translateLooseText(limitText(v20, 260)));
        }

        if (teacher.length() > 0) {
            line3.append("\n教师矩阵：");
            line3.append(translateLooseText(limitText(teacher, 110)));
        }

        if (snap.length() > 0) {
            line3.append("\n教师快照：");
            line3.append(translateLooseText(limitText(snap, 110)));
        }

        if (runtime.length() > 0) {
            line3.append("\n画像反哺：");
            line3.append(translateLooseText(limitText(runtime, 110)));
        }

        if (audit.length() > 0) {
            line3.append("\n审计：");
            line3.append(translateLooseText(limitText(audit, 90)));
        }

        if (diagnosis.length() > 0) {
            line3.append("\n诊断：");
            line3.append(translateLooseText(limitText(diagnosis, 90)));
        }

        if (realBetDiagnosis.length() > 0) {
            line3.append("\n真实BET复盘：");
            line3.append(translateLooseText(limitText(realBetDiagnosis, 220)));
        }
        if (dynamicCausalDiagnosis.length() > 0) {
            line3.append("\n动态因果诊断：");
            line3.append(translateLooseText(limitText(dynamicCausalDiagnosis, 260)));
        }

        if (learning.length() > 0) {
            line3.append("\n学习：");
            line3.append(translateLooseText(limitText(learning, 90)));
        }

        setText(txtStatsLine3, line3.toString());
    }

    private String buildStatsRenderKey(Models.RuntimeStats stats, LightSnapshot snapshot) {
        if (stats == null) {
            return "NO_STATS|"
                    + stateOf(snapshot, "teacher_matrix_summary")
                    + "|"
                    + stateOf(snapshot, "teacher_snapshot_summary")
                    + "|"
                    + stateOf(snapshot, "last_settlement_detail")
                    + "|"
                    + stateOf(snapshot, "real_bet_diagnosis_summary")
                    + "|"
                    + buildV20StateKey(snapshot)
                    + "|"
                    + buildDynamicCausalStateKey(snapshot);
        }

        StringBuilder sb = new StringBuilder();
        sb.append(getFieldText(stats, "realTradeCount")).append("|");
        sb.append(getFieldText(stats, "realWinCount")).append("|");
        sb.append(getFieldText(stats, "realMissCount")).append("|");
        sb.append(getFieldText(stats, "realWinRate")).append("|");
        sb.append(getFieldText(stats, "realFrequency")).append("|");
        sb.append(getFieldText(stats, "realPnl")).append("|");
        sb.append(getFieldText(stats, "pnl")).append("|");
        sb.append(getFieldText(stats, "winRate")).append("|");
        sb.append(getFieldText(stats, "frequency")).append("|");
        sb.append(stats.shadowObservationCount).append("|");
        sb.append(stats.shadowSettledCount).append("|");
        sb.append(stats.shadowHitCount).append("|");
        sb.append(stats.shadowMissCount).append("|");
        sb.append(getFieldText(stats, "learningSummary")).append("|");
        sb.append(stateOf(snapshot, "teacher_matrix_summary")).append("|");
        sb.append(stateOf(snapshot, "teacher_snapshot_summary")).append("|");
        sb.append(stateOf(snapshot, "runtime_profile_summary")).append("|");
        sb.append(stateOf(snapshot, "last_settlement_detail")).append("|");
        sb.append(stateOf(snapshot, "last_strict_trade_settlement_detail")).append("|");
        sb.append(stateOf(snapshot, "real_bet_diagnosis_summary")).append("|");
        sb.append(stateOf(snapshot, "real_bet_diagnosis_win_rate")).append("|");
        sb.append(stateOf(snapshot, "real_bet_diagnosis_shadow_rate")).append("|");
        sb.append(stateOf(snapshot, "real_bet_diagnosis_main_problem")).append("|");
        sb.append(buildV20StateKey(snapshot)).append("|");
        sb.append(buildDynamicCausalStateKey(snapshot));
        return sb.toString();
    }

    private String buildRealBetDiagnosisDisplayText(LightSnapshot snapshot, int maxLen) {
        String detail = stateOf(snapshot, "real_bet_diagnosis_detail");
        String summary = stateOf(snapshot, "real_bet_diagnosis_summary");
        String mainProblem = stateOf(snapshot, "real_bet_diagnosis_main_problem");
        String nextFix = stateOf(snapshot, "real_bet_diagnosis_next_fix");
        String updatedAt = stateOf(snapshot, "real_bet_diagnosis_updated_at");

        StringBuilder sb = new StringBuilder();
        if (summary.length() > 0) {
            sb.append(translateLooseText(summary));
        }
        if (detail.length() > 0 && !detail.equals(summary)) {
            if (sb.length() > 0) {
                sb.append("\n");
            }
            sb.append(translateLooseText(detail));
        }
        if (mainProblem.length() > 0 && sb.indexOf(mainProblem) < 0) {
            if (sb.length() > 0) {
                sb.append("\n");
            }
            sb.append("核心问题：");
            sb.append(translateLooseText(mainProblem));
        }
        if (nextFix.length() > 0 && sb.indexOf(nextFix) < 0) {
            if (sb.length() > 0) {
                sb.append("\n");
            }
            sb.append("下一步建议：");
            sb.append(translateLooseText(nextFix));
        }
        long t = parseLong(updatedAt, 0L);
        if (t > 0L) {
            if (sb.length() > 0) {
                sb.append("\n");
            }
            sb.append("更新时间：");
            sb.append(formatShortTime(t));
        }
        return limitText(sb.toString(), maxLen <= 0 ? UI_DIAGNOSIS_DETAIL_LIMIT : maxLen);
    }

    private void refreshBacktestCard(Models.BacktestReport report, LightSnapshot snapshot) {
        String key = buildBacktestRenderKey(report, snapshot);

        if (key.equals(lastBacktestRenderKey)) {
            return;
        }

        lastBacktestRenderKey = key;

        StringBuilder sb = new StringBuilder();

        String status = stateOf(snapshot, "backtest_status");
        String mode = stateOf(snapshot, "backtest_mode");
        String cost = stateOf(snapshot, "backtest_cost_ms");
        String progress = stateOf(snapshot, "backtest_progress");
        String window = stateOf(snapshot, "backtest_window");
        String err = stateOf(snapshot, "backtest_error");
        String lastSummary = stateOf(snapshot, "backtest_last_summary");

        if (lastSummary.length() == 0) {
            lastSummary = stateOf(snapshot, "backtest_reason");
        }

        if (isBacktestRunningStatus(status)) {
            sb.append("回测正在运行");
            if (mode != null && mode.length() > 0) {
                sb.append("｜模式=");
                sb.append(translateLooseText(mode));
            }
            sb.append("｜进度=");
            sb.append(progress == null || progress.length() == 0 ? "0" : progress);
            sb.append("%");
            if (window != null && window.length() > 0 && !"0".equals(window)) {
                sb.append("｜窗口=W");
                sb.append(window);
            }

            if (lastSummary != null && lastSummary.length() > 0) {
                sb.append("\n说明：");
                sb.append(translateLooseText(limitText(lastSummary, 180)));
            }
        } else if (report == null) {
            if (lastSummary != null && lastSummary.length() > 0) {
                sb.append(translateLooseText(limitText(lastSummary, 190)));
            } else {
                sb.append("暂无回测结果，等待后台自动生成；也可点完整深度回测");
            }
        } else if (report.summary != null && report.summary.length() > 0) {
            sb.append(translateLooseText(limitText(report.summary, 220)));
        } else {
            sb.append("窗口=");
            sb.append(report.window);
            sb.append("  出手=");
            sb.append(report.betCount);
            sb.append("/");
            sb.append(report.totalCount);
            sb.append("  频率=");
            sb.append(UiFormatter.probability(report.frequency));
            sb.append("  胜率=");
            sb.append(UiFormatter.probability(report.winRate));
            sb.append("  盈亏=");
            sb.append(UiFormatter.money(report.pnl));
            sb.append("  回撤=");
            sb.append(UiFormatter.money(report.maxDrawdown));
        }

        if (report != null) {
            String audit = safeText(getFieldText(report, "auditSummary"));
            if (audit.length() > 0) {
                sb.append("\n审计：");
                sb.append(translateLooseText(limitText(audit, 110)));
            }

            String missReasons = safeText(getFieldText(report, "topMissReasons"));
            if (missReasons.length() > 0) {
                sb.append("\n错因Top：");
                sb.append(translateLooseText(limitText(missReasons, 110)));
            }

            String teacher = safeText(getFieldText(report, "teacherMatrixSummary"));
            if (teacher.length() > 0) {
                sb.append("\n教师矩阵：");
                sb.append(translateLooseText(limitText(teacher, 120)));
            }
        }

        String auto = stateOf(snapshot, "auto_learn_summary");
        String teacherState = stateOf(snapshot, "teacher_matrix_summary");
        String v20 = buildV20StateBlock(snapshot);

        if (auto.length() > 0) {
            sb.append("\n自动学习：");
            sb.append(translateLooseText(limitText(auto, 130)));
        }

        if (teacherState.length() > 0) {
            sb.append("\n矩阵状态：");
            sb.append(translateLooseText(limitText(teacherState, 130)));
        }

        if (v20.length() > 0) {
            sb.append("\nV20动态因链：");
            sb.append(translateLooseText(limitText(v20, 260)));
        }

        String realBetDiagnosis =
                buildRealBetDiagnosisDisplayText(snapshot, UI_DIAGNOSIS_DETAIL_LIMIT);
        if (realBetDiagnosis.length() > 0) {
            sb.append("\n\n【真实BET复盘诊断】长按本区域复制完整报告\n");
            sb.append(realBetDiagnosis);
        }
        String fullDiagnosis =
                buildFullDiagnosisClosedLoopDisplayText(snapshot, UI_FULL_DIAGNOSIS_DETAIL_LIMIT);
        if (fullDiagnosis.length() > 0) {
            sb.append("\n\n【满级诊断 + 智能审判闭环】长按本区域复制完整报告\n");
            sb.append(fullDiagnosis);
        }

        if (status != null && status.length() > 0 && !"IDLE".equals(status)) {
            sb.append("\n后台=");
            sb.append(translateLooseText(status));
        }

        if (mode != null && mode.length() > 0) {
            sb.append("｜模式=");
            sb.append(translateLooseText(mode));
        }

        if (progress != null && progress.length() > 0) {
            sb.append("｜进度=");
            sb.append(progress);
            sb.append("%");
        }

        long costMs = parseLong(cost, 0L);
        if (costMs > 0L) {
            sb.append("｜耗时=");
            sb.append(costMs);
            sb.append("ms");
        }

        Models.ManualBacktestTask task = snapshot == null ? null : snapshot.manualTask;
        if (task != null) {
            sb.append("\n手动深测=");
            sb.append(translateLooseText(task.status));
            sb.append("｜");
            sb.append(task.progress);
            sb.append("%");
            if (task.summary != null && task.summary.length() > 0) {
                sb.append("｜");
                sb.append(translateLooseText(limitText(task.summary, 100)));
            }
            if (task.error != null && task.error.length() > 0) {
                sb.append("｜错误=");
                sb.append(translateLooseText(limitText(task.error, 100)));
            }
        }

        if (err != null && err.length() > 0) {
            sb.append("\n错误=");
            sb.append(translateLooseText(limitText(err, 110)));
        }

        setText(txtBacktestSummary, sb.toString());
    }

    private String buildBacktestRenderKey(Models.BacktestReport report, LightSnapshot snapshot) {
        StringBuilder sb = new StringBuilder();
        sb.append(stateOf(snapshot, "backtest_status")).append("|");
        sb.append(stateOf(snapshot, "backtest_mode")).append("|");
        sb.append(stateOf(snapshot, "backtest_progress")).append("|");
        sb.append(stateOf(snapshot, "backtest_window")).append("|");
        sb.append(stateOf(snapshot, "backtest_last_summary")).append("|");
        sb.append(stateOf(snapshot, "auto_learn_summary")).append("|");
        sb.append(stateOf(snapshot, "teacher_matrix_summary")).append("|");
        sb.append(stateOf(snapshot, "real_bet_diagnosis_summary")).append("|");
        sb.append(stateOf(snapshot, "real_bet_diagnosis_updated_at")).append("|");
        sb.append(stateOf(snapshot, "real_bet_diagnosis_main_problem")).append("|");
        sb.append(stateOf(snapshot, "diagnosis_closed_loop_updated_at")).append("|");
        sb.append(stateOf(snapshot, "adaptive_judge_updated_at")).append("|");
        sb.append(stateOf(snapshot, "diagnosis_fix_plan_updated_at")).append("|");
        sb.append(buildV20StateKey(snapshot)).append("|");
        sb.append(buildDynamicCausalStateKey(snapshot)).append("|");

        if (report != null) {
            sb.append(report.window).append("|");
            sb.append(report.betCount).append("|");
            sb.append(report.totalCount).append("|");
            sb.append(report.winRate).append("|");
            sb.append(report.frequency).append("|");
            sb.append(report.pnl).append("|");
            sb.append(report.summary);
        }

        return sb.toString();
    }

    private void renderV49ReasonPreview(HeavySnapshot snapshot) {
        String text =
                snapshot == null
                        ? "等待冻结预测与结算真值"
                        : firstNonEmpty(
                                snapshot.whyTruthText,
                                snapshot.diagnosisSummary,
                                stateOf(snapshot.light, "last_decision_summary"),
                                "尚无可结算原因样本");
        renderWhyTruthCardsR903(text, snapshot == null ? null : snapshot.light);
    }

    private void renderV49DecisionPreview(HeavySnapshot snapshot) {
        Models.Decision d =
                snapshot == null || snapshot.light == null ? null : snapshot.light.decision;
        String text =
                d == null
                        ? "暂无决策"
                        : ("期="
                                + safeDash(d.targetIssue)
                                + "｜"
                                + safeDash(d.action)
                                + "｜P"
                                + d.position
                                + " ["
                                + d.n1
                                + ","
                                + d.n2
                                + ","
                                + d.n3
                                + "]");
        ensureSinglePreview(
                listHistory,
                "最近决策",
                text,
                new View.OnClickListener() {
                    public void onClick(View v) {
                        startActivity(new Intent(MainActivity.this, HistoryActivity.class));
                    }
                });
    }

    private void renderV49DrawPreview(HeavySnapshot snapshot) {
        String text = "暂无开奖";
        if (snapshot != null && snapshot.draws != null && snapshot.draws.size() > 0) {
            Models.Draw d = snapshot.draws.get(snapshot.draws.size() - 1);
            if (d != null) text = "期=" + safeDash(d.issue) + "｜" + drawNumbersText(d);
        }
        ensureSinglePreview(
                listDrawHistory,
                "最近开奖",
                text,
                new View.OnClickListener() {
                    public void onClick(View v) {
                        startActivity(new Intent(MainActivity.this, HistoryActivity.class));
                    }
                });
    }

    private void ensureSinglePreview(
            LinearLayout parent, String title, String detail, View.OnClickListener click) {
        if (parent == null) return;

        // R820: never reinterpret an inflated row_reason.xml as the two-TextView preview shape.
        // R819 reused any single LinearLayout child, but row_reason starts with an accent View and
        // nested content. title/detail therefore resolved to null/wrong badge and the card looked
        // empty even though diagnosis text existed. A tiny dedicated preview is deterministic.
        while (parent.getChildCount() > 0) parent.removeViewAt(parent.getChildCount() - 1);
        LinearLayout row = makeSimpleRow();
        TextView titleView = makeText("", R.color.colorTextMain, 14, true);
        TextView detailView = makeText("", R.color.colorTextSub, 13, false);
        detailView.setMaxLines(12);
        detailView.setEllipsize(TextUtils.TruncateAt.END);
        titleView.setText(title == null ? "" : title);
        detailView.setText(detail == null || detail.length() == 0 ? "暂无可用真值（不是0）" : detail);
        row.addView(titleView);
        row.addView(detailView);
        row.setTag("R820_SINGLE_PREVIEW");
        row.setOnClickListener(click);
        parent.addView(row);
    }

    private void refreshReasonList(HeavySnapshot snapshot) {
        if (AppConfig.ENABLE_V50_PRIME || AppConfig.ENABLE_V49_FULL_MOE) {
            renderV49ReasonPreview(snapshot);
            return;
        }
        if (listReasons == null) {
            return;
        }

        if (!forceHeavyRenderOnce && !suppressScrollGuard && isUserScrollingNow()) {
            pendingHeavyRefresh = true;
            return;
        }

        Models.Decision decision =
                snapshot == null || snapshot.light == null ? null : snapshot.light.decision;
        Models.AppRuntimeState state =
                snapshot == null || snapshot.light == null ? null : snapshot.light.state;

        String key = buildReasonRenderKey(decision, state, snapshot);

        if (key.equals(lastReasonRenderKey) && listReasons.getChildCount() > 0) {
            return;
        }

        lastReasonRenderKey = key;
        listReasons.removeAllViews();

        ArrayList<Models.ReasonItem> reasons = snapshot == null ? null : snapshot.reasons;

        if (reasons == null || reasons.size() == 0) {
            addReasonRow(null, state, snapshot == null ? null : snapshot.light);
            return;
        }

        int limit = Math.min(reasons.size(), AppConfig.REASON_SHOW_LIMIT);
        if (limit > REASON_RENDER_SAFE_LIMIT) {
            limit = REASON_RENDER_SAFE_LIMIT;
        }

        for (int i = 0; i < limit; i++) {
            addReasonRow(reasons.get(i), state, snapshot == null ? null : snapshot.light);
        }

        if (decision != null) {
            String evidence =
                    buildV20EvidenceUiText(
                            decision,
                            snapshot == null ? null : snapshot.light,
                            UI_V20_EVIDENCE_LIMIT);
            if (evidence.length() > 0) {
                Models.ReasonItem item = new Models.ReasonItem();
                item.level = AppConfig.REASON_INFO;
                item.code = "V20_DECISION_EVIDENCE_UI";
                item.title = "V20最终证据矩阵";
                item.detail = evidence;
                item.value = 0.0d;
                item.passed = 1;
                item.createdAt = System.currentTimeMillis();
                addReasonRow(item, state, snapshot == null ? null : snapshot.light);
            }
        }

        if (snapshot != null && snapshot.light != null) {
            String v20 = buildV20StateBlock(snapshot.light);
            if (v20.length() > 0) {
                Models.ReasonItem item = new Models.ReasonItem();
                item.level = AppConfig.REASON_INFO;
                item.code = "V20_CAUSE_CHAIN_UI";
                item.title = "V20动态因链后台状态";
                item.detail = v20;
                item.value = 0.0d;
                item.passed = 1;
                item.createdAt = System.currentTimeMillis();
                addReasonRow(item, state, snapshot.light);
            }
        }

        addRealBetDiagnosisRows(snapshot, state);
        addDynamicCausalDiagnosisRows(snapshot, state);
        addFullDiagnosisClosedLoopRows(snapshot, state);
    }

    private void addRealBetDiagnosisRows(HeavySnapshot snapshot, Models.AppRuntimeState state) {
        if (snapshot == null) {
            return;
        }

        ArrayList<Models.ReasonItem> items = snapshot.diagnosisReasons;

        if (items != null && items.size() > 0) {
            int limit = items.size();
            if (limit > 8) {
                limit = 8;
            }
            for (int i = 0; i < limit; i++) {
                addReasonRow(items.get(i), state, snapshot.light);
            }
            return;
        }

        String text = snapshot.diagnosisSummary;
        if ((text == null || text.length() == 0) && snapshot.light != null) {
            text = stateOf(snapshot.light, "real_bet_diagnosis_summary");
        }
        if (text == null || text.length() == 0) {
            return;
        }

        Models.ReasonItem item = new Models.ReasonItem();
        item.level = AppConfig.REASON_INFO;
        item.code = "REAL_BET_DIAGNOSIS_UI";
        item.title = "真实BET复盘诊断";
        item.detail = translateLooseText(limitText(text, UI_DIAGNOSIS_SUMMARY_LIMIT));
        item.value = 0.0d;
        item.passed = 1;
        item.createdAt = System.currentTimeMillis();
        addReasonRow(item, state, snapshot.light);
    }

    private String buildReasonRenderKey(
            Models.Decision d, Models.AppRuntimeState state, HeavySnapshot snapshot) {
        StringBuilder sb = new StringBuilder();

        if (d != null) {
            sb.append(d.targetIssue).append("|");
            sb.append(d.decision).append("|");
            sb.append(d.position).append("|");
            sb.append(d.n1).append("|");
            sb.append(d.n2).append("|");
            sb.append(d.n3).append("|");
            sb.append(d.k3Mask).append("|");
            sb.append(d.result).append("|");
            sb.append(d.settled).append("|");
            sb.append(d.blockCode).append("|");
            sb.append(d.finalP).append("|");
            sb.append(d.dynamicGate).append("|");
            sb.append(limitText(d.reasonSummary, 90)).append("|");
            sb.append(getFieldText(d, "teacherMatrixSummary")).append("|");
            sb.append(getFieldText(d, "learningSummary")).append("|");
            sb.append(buildDecisionV20Text(d, snapshot == null ? null : snapshot.light))
                    .append("|");
            sb.append(buildV20EvidenceUiText(d, snapshot == null ? null : snapshot.light, 260));
        } else if (state != null) {
            sb.append(state.targetIssue).append("|");
            sb.append(state.serviceStatus).append("|");
            sb.append(state.lastDecision);
        } else {
            sb.append("EMPTY");
        }

        if (snapshot != null) {
            sb.append("|R=").append(snapshot.reasons == null ? 0 : snapshot.reasons.size());
            sb.append("|V20=").append(buildV20StateKey(snapshot.light));
            sb.append("|DYN_CAUSAL=").append(buildDynamicCausalStateKey(snapshot.light));
            sb.append("|DIAG=")
                    .append(
                            snapshot.diagnosisSummary == null
                                    ? ""
                                    : limitText(snapshot.diagnosisSummary, 180));
            sb.append("|DIAG_AT=").append(stateOf(snapshot.light, "real_bet_diagnosis_updated_at"));
            sb.append("|FULL_DIAG_AT=")
                    .append(stateOf(snapshot.light, "diagnosis_closed_loop_updated_at"));
            sb.append("|JUDGE=").append(stateOf(snapshot.light, "adaptive_judge_summary"));
            sb.append("|FIX=").append(stateOf(snapshot.light, "diagnosis_fix_plan_summary"));
        }

        return sb.toString();
    }

    private void addReasonRow(
            Models.ReasonItem item, Models.AppRuntimeState state, LightSnapshot snapshot) {
        if (listReasons == null) {
            return;
        }

        View row = LayoutInflater.from(this).inflate(R.layout.row_reason, listReasons, false);

        TextView badge = (TextView) row.findViewById(R.id.txt_reason_badge);
        TextView title = (TextView) row.findViewById(R.id.txt_reason_title);
        TextView detail = (TextView) row.findViewById(R.id.txt_reason_detail);

        if (badge == null || title == null || detail == null) {
            return;
        }

        if (item == null) {
            badge.setText("提示");
            title.setText("暂无当前目标期原因");
            detail.setText(getNoDecisionText(snapshot));
            badge.setTextColor(getColorCompat(R.color.colorInfo));
        } else {
            badge.setText(UiFormatter.reasonLevelText(item.level));
            title.setText(item.title == null ? "" : translateLooseText(item.title));
            detail.setText(
                    item.detail == null ? "" : removePnlText(translateLooseText(item.detail)));
            badge.setTextColor(getColorCompat(UiFormatter.reasonColor(item.level)));
        }

        fixHistoryLineText(title, 3);
        fixHistoryLineText(detail, 8);
        bindExpandableText(detail, 8);

        listReasons.addView(row);
    }

    private void refreshDrawHistoryList(HeavySnapshot snapshot) {
        if (AppConfig.ENABLE_V50_PRIME || AppConfig.ENABLE_V49_FULL_MOE) {
            renderV49DrawPreview(snapshot);
            return;
        }
        if (listDrawHistory == null) {
            return;
        }

        if (!forceHeavyRenderOnce && !suppressScrollGuard && isUserScrollingNow()) {
            pendingHeavyRefresh = true;
            return;
        }

        String key = buildDrawHistoryRenderKey(snapshot);

        if (key.equals(lastDrawHistoryRenderKey) && listDrawHistory.getChildCount() > 0) {
            return;
        }

        lastDrawHistoryRenderKey = key;
        listDrawHistory.removeAllViews();

        addDataProgressRow(snapshot == null ? null : snapshot.light);
        addThreadStatusRow(snapshot == null ? null : snapshot.light);
        addBackfillStatusRow(snapshot == null ? null : snapshot.light);
        addRuntimeDeepRow(snapshot == null ? null : snapshot.light);

        ArrayList<Models.Draw> draws = snapshot == null ? null : snapshot.draws;

        if (draws == null || draws.size() == 0) {
            addEmptyDrawHistoryRow();
            return;
        }

        int added = 0;

        for (int i = draws.size() - 1; i >= 0 && added < DRAW_HISTORY_RENDER_LIMIT; i--) {
            Models.Draw d = draws.get(i);

            if (d == null || !d.isValid()) {
                continue;
            }

            addDrawHistoryRow(d);
            added++;
        }

        if (added == 0) {
            addEmptyDrawHistoryRow();
        }
    }

    private String buildDrawHistoryRenderKey(HeavySnapshot snapshot) {
        if (snapshot == null || snapshot.light == null) {
            return "NO_SNAPSHOT";
        }

        LightSnapshot light = snapshot.light;
        String latestIssue = "";

        if (light.latestDraw != null && light.latestDraw.issue != null) {
            latestIssue = light.latestDraw.issue;
        }

        StringBuilder sb = new StringBuilder();
        sb.append(latestIssue).append("|");
        sb.append(light.drawCount).append("|");
        sb.append(stateOf(light, "backfill_status")).append("|");
        sb.append(stateOf(light, "backfill_inserted")).append("|");
        sb.append(stateOf(light, "analysis_status")).append("|");
        sb.append(stateOf(light, "analysis_progress")).append("|");
        sb.append(stateOf(light, "backtest_status")).append("|");
        sb.append(stateOf(light, "backtest_progress")).append("|");
        sb.append(stateOf(light, "teacher_matrix_summary")).append("|");
        sb.append(stateOf(light, "teacher_snapshot_summary")).append("|");
        sb.append(stateOf(light, "runtime_profile_summary")).append("|");
        sb.append(stateOf(light, "settlement_summary")).append("|");
        sb.append(buildV20StateKey(light)).append("|");
        sb.append(snapshot.draws == null ? 0 : snapshot.draws.size());

        return sb.toString();
    }

    private void addDataProgressRow(LightSnapshot snapshot) {
        if (listDrawHistory == null) {
            return;
        }

        LinearLayout row = makeSimpleRow();

        TextView title = makeText("数据进度", R.color.colorInfo, 16, true);
        String text = snapshot == null ? "" : snapshot.drawDataProgressText;
        if (text == null || text.length() == 0) {
            text = "已抓取：" + (snapshot == null ? 0 : snapshot.drawCount) + "期";
        }

        TextView detail = makeText(text, R.color.colorTextMain, 14, false);
        bindExpandableText(detail, 5);

        row.addView(title);
        row.addView(detail);

        listDrawHistory.addView(row);
    }

    private void addThreadStatusRow(LightSnapshot snapshot) {
        if (listDrawHistory == null) {
            return;
        }

        String fetchLine = buildFetchLine(snapshot);
        String analysisLine = buildAnalysisLine(stateOf(snapshot, "service_status"), snapshot);
        String decisionLine = buildDecisionStateLine(snapshot);
        String settlementLine = buildSettlementLine(snapshot);
        String v20Line = buildV20CauseLine(snapshot);
        String teacherLine = buildTeacherMatrixLine(snapshot);
        String teacherSnapshotLine = buildTeacherSnapshotLine(snapshot);
        String runtimeProfileLine = buildRuntimeProfileLine(snapshot);
        String learningLine = buildLearningLine(snapshot);
        String backtestLine = buildBacktestLine(snapshot);

        if (fetchLine.length() == 0
                && analysisLine.length() == 0
                && decisionLine.length() == 0
                && settlementLine.length() == 0
                && v20Line.length() == 0
                && teacherLine.length() == 0
                && teacherSnapshotLine.length() == 0
                && runtimeProfileLine.length() == 0
                && learningLine.length() == 0
                && backtestLine.length() == 0) {
            return;
        }

        LinearLayout row = makeSimpleRow();

        TextView title = makeText("线程状态 / V20因链 / 学习闭环", R.color.colorInfo, 16, true);

        StringBuilder detail = new StringBuilder();

        appendLine(detail, fetchLine);
        appendLine(detail, analysisLine);
        appendLine(detail, decisionLine);
        appendLine(detail, settlementLine);
        appendLine(detail, v20Line);
        appendLine(detail, teacherLine);
        appendLine(detail, teacherSnapshotLine);
        appendLine(detail, runtimeProfileLine);
        appendLine(detail, learningLine);
        appendLine(detail, backtestLine);

        TextView tv = makeText(detail.toString(), R.color.colorTextSub, 14, false);
        bindExpandableText(tv, 10);

        row.addView(title);
        row.addView(tv);

        listDrawHistory.addView(row);
    }

    private void addBackfillStatusRow(LightSnapshot snapshot) {
        if (listDrawHistory == null) {
            return;
        }

        LinearLayout row = makeSimpleRow();

        TextView title = makeText("自动回补", R.color.colorHold, 16, true);
        String text = snapshot == null ? "" : snapshot.backfillStatusText;

        if (text == null || text.length() == 0) {
            text = buildBackfillLine(snapshot);
        }

        TextView detail = makeText(text, R.color.colorTextSub, 14, false);
        bindExpandableText(detail, 5);

        row.addView(title);
        row.addView(detail);

        listDrawHistory.addView(row);
    }

    private void addRuntimeDeepRow(LightSnapshot snapshot) {
        if (listDrawHistory == null || snapshot == null) {
            return;
        }

        Models.RuntimeStats stats = snapshot.stats;

        String learning = stats == null ? "" : safeText(getFieldText(stats, "learningSummary"));
        String bias = stats == null ? "" : safeText(getFieldText(stats, "biasSummary"));
        String audit = stats == null ? "" : safeText(getFieldText(stats, "auditSummary"));
        String ai = stats == null ? "" : safeText(getFieldText(stats, "aiJudgeSummary"));
        String promotion = stats == null ? "" : safeText(getFieldText(stats, "promotionSummary"));
        String aPlus = stats == null ? "" : safeText(getFieldText(stats, "aPlusSummary"));
        String snapshotId = stats == null ? "" : safeText(getFieldText(stats, "lastSnapshotId"));

        String teacher = stateOf(snapshot, "teacher_matrix_summary");
        String teacherSnapshot = stateOf(snapshot, "teacher_snapshot_summary");
        String runtimeProfile = stateOf(snapshot, "runtime_profile_summary");
        String learningReason = stateOf(snapshot, "learning_reason_detail");
        String v20 = buildV20StateBlock(snapshot);

        if (learning.length() == 0
                && bias.length() == 0
                && audit.length() == 0
                && ai.length() == 0
                && promotion.length() == 0
                && aPlus.length() == 0
                && snapshotId.length() == 0
                && teacher.length() == 0
                && teacherSnapshot.length() == 0
                && runtimeProfile.length() == 0
                && learningReason.length() == 0
                && v20.length() == 0) {
            return;
        }

        LinearLayout row = makeSimpleRow();

        TextView title = makeText("V20动态因链 / 教师矩阵 / 画像反哺", R.color.colorInfo, 16, true);
        StringBuilder detailText = new StringBuilder();

        if (v20.length() > 0) {
            appendLine(detailText, "V20因链：" + translateLooseText(limitText(v20, 260)));
        }

        if (learning.length() > 0) {
            appendLine(detailText, "学习：" + translateLooseText(limitText(learning, 110)));
        }

        if (teacher.length() > 0) {
            appendLine(detailText, "教师矩阵：" + translateLooseText(limitText(teacher, 130)));
        }

        if (teacherSnapshot.length() > 0) {
            appendLine(detailText, "教师快照：" + translateLooseText(limitText(teacherSnapshot, 130)));
        }

        if (runtimeProfile.length() > 0) {
            appendLine(detailText, "画像反哺：" + translateLooseText(limitText(runtimeProfile, 130)));
        }

        if (learningReason.length() > 0) {
            appendLine(detailText, "推理反推：" + translateLooseText(limitText(learningReason, 130)));
        }

        if (bias.length() > 0) {
            appendLine(detailText, "偏差：" + translateLooseText(limitText(bias, 100)));
        }

        if (audit.length() > 0) {
            appendLine(detailText, "审计：" + translateLooseText(limitText(audit, 100)));
        }

        if (ai.length() > 0) {
            appendLine(detailText, "AI裁决：" + translateLooseText(limitText(ai, 100)));
        }

        if (promotion.length() > 0) {
            appendLine(detailText, "晋级门：" + translateLooseText(limitText(promotion, 100)));
        }

        if (aPlus.length() > 0) {
            appendLine(detailText, "A+：" + translateLooseText(limitText(aPlus, 100)));
        }

        if (snapshotId.length() > 0) {
            appendLine(detailText, "快照：" + snapshotId);
        }

        TextView detail = makeText(detailText.toString(), R.color.colorTextSub, 14, false);
        bindExpandableText(detail, 10);

        row.addView(title);
        row.addView(detail);

        listDrawHistory.addView(row);
    }

    private void addEmptyDrawHistoryRow() {
        if (listDrawHistory == null) {
            return;
        }

        LinearLayout row = makeSimpleRow();

        TextView title = makeText("暂无开奖历史", R.color.colorTextMain, 16, true);
        TextView detail = makeText("启动后台服务后自动抓取真实开奖数据", R.color.colorTextSub, 14, false);

        row.addView(title);
        row.addView(detail);

        listDrawHistory.addView(row);
    }

    private void addDrawHistoryRow(Models.Draw draw) {
        if (listDrawHistory == null || draw == null) {
            return;
        }

        LinearLayout row = makeSimpleRow();

        TextView issue = makeText("期号：" + safeDash(draw.issue), R.color.colorTextSub, 14, false);
        TextView nums = makeText(drawNumbersText(draw), R.color.colorTextMain, 18, true);
        TextView time = makeText(formatDrawTime(draw.openTime), R.color.colorTextMuted, 12, false);

        row.addView(issue);
        row.addView(nums);
        row.addView(time);

        listDrawHistory.addView(row);
    }

    private void refreshHistoryList(HeavySnapshot snapshot) {
        if (AppConfig.ENABLE_V50_PRIME || AppConfig.ENABLE_V49_FULL_MOE) {
            renderV49DecisionPreview(snapshot);
            return;
        }
        if (listHistory == null) {
            return;
        }

        if (!forceHeavyRenderOnce && !suppressScrollGuard && isUserScrollingNow()) {
            pendingHeavyRefresh = true;
            return;
        }

        String key = buildDecisionHistoryRenderKey(snapshot);

        if (key.equals(lastDecisionHistoryRenderKey) && listHistory.getChildCount() > 0) {
            return;
        }

        lastDecisionHistoryRenderKey = key;
        listHistory.removeAllViews();

        ArrayList<Models.Decision> decisions = snapshot == null ? null : snapshot.decisions;

        if (decisions == null || decisions.size() == 0) {
            addEmptyHistoryRow();
            return;
        }

        ArrayList<String> seenTargetIssues = new ArrayList<String>();
        int added = 0;

        for (int i = 0; i < decisions.size() && added < DECISION_HISTORY_RENDER_LIMIT; i++) {
            Models.Decision d = decisions.get(i);

            if (d == null) {
                continue;
            }

            String issueKey = d.targetIssue == null ? "" : d.targetIssue;

            if (issueKey.length() > 0 && seenTargetIssues.contains(issueKey)) {
                continue;
            }

            if (issueKey.length() > 0) {
                seenTargetIssues.add(issueKey);
            }

            Models.Trade trade =
                    issueKey.length() == 0 || snapshot == null
                            ? null
                            : snapshot.tradeMap.get(issueKey);
            addHistoryRow(d, trade);
            added++;
        }

        if (added == 0) {
            addEmptyHistoryRow();
        }
    }

    private String buildDecisionHistoryRenderKey(HeavySnapshot snapshot) {
        if (snapshot == null) {
            return "NO_SNAPSHOT";
        }

        StringBuilder sb = new StringBuilder();

        if (snapshot.decisions != null && snapshot.decisions.size() > 0) {
            int limit = Math.min(snapshot.decisions.size(), DECISION_HISTORY_RENDER_LIMIT);

            for (int i = 0; i < limit; i++) {
                Models.Decision d = snapshot.decisions.get(i);

                if (d == null) {
                    continue;
                }

                sb.append(d.targetIssue).append("|");
                sb.append(d.decision).append("|");
                sb.append(d.position).append("|");
                sb.append(d.n1).append("|");
                sb.append(d.n2).append("|");
                sb.append(d.n3).append("|");
                sb.append(d.k3Mask).append("|");
                sb.append(d.result).append("|");
                sb.append(d.settled).append("|");
                sb.append(readInt(d, "actualNumber", 0)).append("|");
                sb.append(d.finalP).append("|");
                sb.append(d.dynamicGate).append("|");
                sb.append(d.blockCode).append("|");
                sb.append(getFieldText(d, "teacherMatrixSummary")).append("|");
                sb.append(getFieldText(d, "learningSummary")).append("|");
                sb.append(buildDecisionV20Text(d, snapshot.light)).append("|");

                Models.Trade t =
                        d.targetIssue == null ? null : snapshot.tradeMap.get(d.targetIssue);
                if (t != null) {
                    sb.append(t.result).append("|");
                    sb.append(t.position).append("|");
                    sb.append(t.n1).append("|");
                    sb.append(t.n2).append("|");
                    sb.append(t.n3).append("|");
                    sb.append(t.actualNumber).append("|");
                    sb.append(t.missReason).append("|");
                }
            }
        } else {
            sb.append("EMPTY");
        }

        if (snapshot.light != null) {
            sb.append("|");
            sb.append(stateOf(snapshot.light, "last_decision_target_issue"));
            sb.append("|");
            sb.append(stateOf(snapshot.light, "last_settlement_detail"));
            sb.append("|");
            sb.append(stateOf(snapshot.light, "last_strict_trade_settlement_detail"));
            sb.append("|");
            sb.append(stateOf(snapshot.light, "teacher_matrix_summary"));
            sb.append("|");
            sb.append(stateOf(snapshot.light, "teacher_snapshot_summary"));
            sb.append("|");
            sb.append(stateOf(snapshot.light, "runtime_profile_summary"));
            sb.append("|");
            sb.append(buildV20StateKey(snapshot.light));
        }

        return sb.toString();
    }

    private void addEmptyHistoryRow() {
        if (listHistory == null) {
            return;
        }

        View row = LayoutInflater.from(this).inflate(R.layout.row_history, listHistory, false);

        TextView issue = (TextView) row.findViewById(R.id.txt_history_issue);
        TextView decision = (TextView) row.findViewById(R.id.txt_history_decision);
        TextView detail = (TextView) row.findViewById(R.id.txt_history_detail);
        TextView result = (TextView) row.findViewById(R.id.txt_history_result);
        TextView probability = (TextView) row.findViewById(R.id.txt_history_probability);
        TextView reason = (TextView) row.findViewById(R.id.txt_history_reason);

        fixHistoryRowText(issue, decision, detail, result, probability, reason);

        if (issue != null) {
            issue.setText("暂无决策历史");
        }
        if (decision != null) {
            decision.setText("等待分析");
        }
        if (detail != null) {
            detail.setText("启动后台服务后自动生成决策历史");
        }
        if (result != null) {
            result.setText("--");
        }
        if (probability != null) {
            probability.setText("--");
        }
        if (reason != null) {
            reason.setText("");
        }

        listHistory.addView(row);
    }

    private void addHistoryRow(Models.Decision d, Models.Trade trade) {
        if (d == null || listHistory == null) {
            return;
        }

        View row = LayoutInflater.from(this).inflate(R.layout.row_history, listHistory, false);

        TextView issue = (TextView) row.findViewById(R.id.txt_history_issue);
        TextView decision = (TextView) row.findViewById(R.id.txt_history_decision);
        TextView detail = (TextView) row.findViewById(R.id.txt_history_detail);
        TextView result = (TextView) row.findViewById(R.id.txt_history_result);
        TextView probability = (TextView) row.findViewById(R.id.txt_history_probability);
        TextView reason = (TextView) row.findViewById(R.id.txt_history_reason);

        fixHistoryRowText(issue, decision, detail, result, probability, reason);

        if (issue != null) {
            issue.setText("目标期：" + safeDash(d.targetIssue)+"\n"+AlgorithmAttributionR943.line(lastLightSnapshot==null?null:lastLightSnapshot.states,d.targetIssue));
        }

        String decisionRaw = d.decision == null ? AppConfig.DECISION_HOLD : d.decision;

        if (decision != null) {
            decision.setText(UiFormatter.decisionText(decisionRaw));
            decision.setTextColor(getColorCompat(UiFormatter.decisionColor(decisionRaw)));
        }

        if (detail != null) {
            if (AppConfig.DECISION_BET.equals(decisionRaw)) {
                detail.setText(
                        "第" + d.position + "位置 投注=" + UiFormatter.numberGroup(d.n1, d.n2, d.n3));
            } else if (AppConfig.DECISION_SHADOW.equals(decisionRaw)) {
                if (d.position > 0 && Models.isValidK3Mask(d.k3Mask)) {
                    detail.setText(
                            "影子观察 第"
                                    + d.position
                                    + "位置 "
                                    + UiFormatter.numberGroup(d.n1, d.n2, d.n3));
                } else {
                    detail.setText("影子观察");
                }
            } else if (d.position > 0 && Models.isValidK3Mask(d.k3Mask)) {
                detail.setText(
                        "不出手观察 第" + d.position + "位置 " + UiFormatter.numberGroup(d.n1, d.n2, d.n3));
            } else {
                detail.setText("不出手");
            }
        }

        String resultText = "";
        int resultColor = R.color.colorTextSub;

        if (trade != null && isSettlementResultText(trade.result)) {
            resultText = UiFormatter.resultText(trade.result);
            resultText += "｜投注=" + UiFormatter.numberGroup(trade.n1, trade.n2, trade.n3);

            if (trade.actualNumber > 0) {
                resultText += "｜实际=" + UiFormatter.number(trade.actualNumber);
            }

            if (AppConfig.RESULT_MISS.equals(trade.result)
                    && trade.missReason != null
                    && trade.missReason.length() > 0) {
                resultText += "｜" + translateLooseText(trade.missReason);
            }

            resultColor = UiFormatter.resultColor(trade.result);
        } else if (isSettlementResultText(d.result)) {
            resultText = UiFormatter.resultText(d.result);
            resultText += "｜投注=" + UiFormatter.numberGroup(d.n1, d.n2, d.n3);

            int actual = readInt(d, "actualNumber", 0);
            if (actual <= 0) {
                actual = readInt(d, "settleNumber", 0);
            }

            if (actual > 0) {
                resultText += "｜实际=" + UiFormatter.number(actual);
            }

            String missReason = getFieldText(d, "missReason");
            if (AppConfig.RESULT_MISS.equals(d.result)
                    && missReason != null
                    && missReason.length() > 0) {
                resultText += "｜" + translateLooseText(missReason);
            }

            resultColor = UiFormatter.resultColor(d.result);
        } else {
            if (AppConfig.DECISION_BET.equals(decisionRaw)) {
                resultText = "等待结算｜投注=" + UiFormatter.numberGroup(d.n1, d.n2, d.n3);
                resultColor = R.color.colorWarn;
            } else if (AppConfig.DECISION_SHADOW.equals(decisionRaw)) {
                resultText = "影子，不计真实";
                resultColor = R.color.colorTextSub;
            } else {
                resultText = "未下注";
                resultColor = R.color.colorTextSub;
            }
        }

        if (result != null) {
            result.setText(removePnlText(resultText));
            result.setTextColor(getColorCompat(resultColor));
        }

        double edge = d.edge;
        if (edge == 0.0d && d.finalP > 0.0d && d.dynamicGate > 0.0d) {
            edge = d.finalP - d.dynamicGate;
        }

        if (probability != null) {
            probability.setText(
                    "最终概率="
                            + UiFormatter.probability(d.finalP)
                            + "  门槛="
                            + UiFormatter.probability(d.dynamicGate)
                            + "  优势="
                            + UiFormatter.signedProbability(edge));
        }

        if (reason != null) {
            String reasonText = "";

            if (trade != null && isSettlementResultText(trade.result)) {
                reasonText = buildTradeSettlementUiText(trade);
            } else if (isSettlementResultText(d.result)) {
                reasonText = buildDecisionSettlementUiText(d);
            } else if (d.reasonSummary != null && d.reasonSummary.length() > 0) {
                reasonText = translateLooseText(limitText(d.reasonSummary, 280));
            } else if (d.blockCode != null && d.blockCode.length() > 0) {
                reasonText = UiFormatter.blockReasonText(d.blockCode);
            }

            if (reasonText.length() == 0 && AppConfig.DECISION_BET.equals(decisionRaw)) {
                reasonText =
                        "投注明细：第"
                                + d.position
                                + "位置 投注="
                                + UiFormatter.numberGroup(d.n1, d.n2, d.n3)
                                + "；开奖后只按第"
                                + d.position
                                + "位置的实际号码结算。";
            }

            String v20 = buildDecisionV20Text(d, lastLightSnapshot);
            if (v20.length() > 0) {
                reasonText = appendReason(reasonText, v20);
            }
            String finalEvidence = buildV20EvidenceUiText(d, lastLightSnapshot, 220);
            if (finalEvidence.length() > 0 && reasonText.indexOf(finalEvidence) < 0) {
                reasonText = appendReason(reasonText, "证据矩阵：" + finalEvidence);
            }

            String tm = getFieldText(d, "teacherMatrixSummary");
            if (tm != null && tm.length() > 0) {
                reasonText = appendReason(reasonText, "教师矩阵：" + limitText(tm, 120));
            }

            String learning = getFieldText(d, "learningSummary");
            if (learning != null && learning.length() > 0) {
                reasonText = appendReason(reasonText, "学习反推：" + limitText(learning, 120));
            }

            reason.setText(removePnlText(reasonText));
        }

        listHistory.addView(row);
    }

    private void fixHistoryRowText(
            TextView issue,
            TextView decision,
            TextView detail,
            TextView result,
            TextView probability,
            TextView reason) {
        fixHistoryLineText(issue, 2);
        fixHistoryLineText(decision, 2);
        fixHistoryLineText(detail, 4);
        fixHistoryLineText(result, 5);
        fixHistoryLineText(probability, 3);
        fixHistoryLineText(reason, 8);
        bindExpandableText(reason, 8);
    }

    private void fixHistoryLineText(TextView tv, int maxLines) {
        if (tv == null) {
            return;
        }

        try {
            tv.setSingleLine(false);
            tv.setMinLines(1);
            tv.setMaxLines(maxLines <= 0 ? 4 : maxLines);
            tv.setEllipsize(TextUtils.TruncateAt.END);
            tv.setIncludeFontPadding(false);
        } catch (Throwable e) {
        }
    }

    private LinearLayout makeSimpleRow() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(dp(12), dp(10), dp(12), dp(10));
        row.setBackgroundResource(R.drawable.bg_card);

        LinearLayout.LayoutParams lp =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, dp(8));
        row.setLayoutParams(lp);

        return row;
    }

    private TextView makeText(String text, int colorRes, int sp, boolean bold) {
        TextView tv = new TextView(this);
        tv.setText(text == null ? "" : text);
        tv.setTextColor(getColorCompat(colorRes));
        tv.setTextSize(sp);
        tv.setSingleLine(false);
        tv.setMinLines(1);
        tv.setMaxLines(12);
        tv.setEllipsize(TextUtils.TruncateAt.END);

        if (bold) {
            tv.setTypeface(null, android.graphics.Typeface.BOLD);
        }

        return tv;
    }

    private String drawNumbersText(Models.Draw draw) {
        if (draw == null || draw.numbers == null || draw.numbers.length < AppConfig.NUMBER_COUNT) {
            return "--";
        }

        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < AppConfig.NUMBER_COUNT; i++) {
            if (i > 0) {
                sb.append(" ");
            }
            sb.append(UiFormatter.number(draw.numbers[i]));
        }

        return sb.toString();
    }

    private String formatDrawTime(long time) {
        if (time <= 0L) {
            return "开奖时间：--";
        }

        try {
            SimpleDateFormat sdf = new SimpleDateFormat("MM-dd HH:mm:ss", Locale.US);
            return "开奖时间：" + sdf.format(Long.valueOf(time));
        } catch (Throwable e) {
            return "开奖时间：--";
        }
    }

    private String formatShortTime(long time) {
        if (time <= 0L) {
            return "";
        }

        try {
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.US);
            return sdf.format(Long.valueOf(time));
        } catch (Throwable e) {
            return "";
        }
    }

    // API<33 uses a signature permission on every internal action; API33+ additionally
    // uses NOT_EXPORTED. The four-argument legacy overload has no flags parameter.
    @android.annotation.SuppressLint("UnspecifiedRegisterReceiverFlag")
    private void registerServiceReceiver() {
        if (receiverRegistered) {
            return;
        }

        try {
            IntentFilter filter = new IntentFilter();
            filter.addAction(AppConfig.ACTION_SERVICE_TICK);
            filter.addAction(AppConfig.ACTION_DECISION_UPDATED);
            filter.addAction(AppConfig.ACTION_STATE_CHANGED);
            filter.addAction(AppConfig.ACTION_BACKTEST_UPDATED);
            filter.addAction(AppConfig.ACTION_TEACHER_MATRIX_UPDATED);
            filter.addAction(ACTION_V20_CAUSE_CHAIN_UPDATED);

            if(Build.VERSION.SDK_INT>=33)registerReceiver(serviceReceiver,filter,Context.RECEIVER_NOT_EXPORTED);
            else registerReceiver(serviceReceiver,filter,getPackageName()+".permission.INTERNAL_EVENT",null);
            receiverRegistered = true;
        } catch (Throwable e) {
        }
    }

    private void unregisterServiceReceiver() {
        if (!receiverRegistered) {
            return;
        }

        try {
            unregisterReceiver(serviceReceiver);
        } catch (Throwable e) {
        }

        receiverRegistered = false;
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < 33) {
            return;
        }

        try {
            String permission = "android.permission.POST_NOTIFICATIONS";

            if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[] {permission}, 3301);
            }
        } catch (Throwable e) {
        }
    }

    private void suggestBatteryIgnoreIfNeeded() {
        if (Build.VERSION.SDK_INT < 23 || store == null) return;
        try {
            android.os.PowerManager pm = (android.os.PowerManager) getSystemService(POWER_SERVICE);
            boolean ignored = pm != null && pm.isIgnoringBatteryOptimizations(getPackageName());
            writeUiState("battery_ignore_status", ignored ? "GRANTED" : "NEEDED_USER_ACTION");
            writeUiState(
                    "battery_ignore_prompt_policy", "R322_NO_AUTO_SETTINGS_DURING_COLD_START");
            writeUiState("battery_ignore_checked_at", String.valueOf(System.currentTimeMillis()));
        } catch (Throwable e) {
            try {
                writeUiState("battery_ignore_status", "CHECK_ERROR");
            } catch (Throwable ignore) {
            }
            try {
                CrashRecorder.recordCaught(getApplicationContext(), e, "BATTERY_STATUS_CHECK_R322");
            } catch (Throwable ignore) {
            }
        }
    }

    private boolean isUiBusyStateBySnapshot() {
        LightSnapshot snapshot = lastLightSnapshot;

        if (snapshot == null) {
            return false;
        }

        try {
            String service = snapshot.state == null ? "" : snapshot.state.serviceStatus;
            if (service == null || service.length() == 0) {
                service = stateOf(snapshot, "service_status");
            }

            String analysis = stateOf(snapshot, "analysis_status");
            String backtest = stateOf(snapshot, "backtest_status");
            String manual = stateOf(snapshot, "manual_backtest_status");
            String backfill = stateOf(snapshot, "backfill_status");
            String teacher = stateOf(snapshot, "teacher_matrix_status");
            String settlement = stateOf(snapshot, "settlement_status");
            String v20 = stateOf(snapshot, "v20_cause_chain_status");

            if (AppConfig.SERVICE_STATUS_FETCHING.equals(service)
                    || AppConfig.SERVICE_STATUS_ANALYZING.equals(service)
                    || AppConfig.SERVICE_STATUS_SETTLING.equals(service)
                    || "SETTLING".equals(service)) {
                return true;
            }

            return isRunningLike(analysis)
                    || isRunningLike(backtest)
                    || isRunningLike(manual)
                    || isRunningLike(backfill)
                    || isRunningLike(teacher)
                    || isRunningLike(settlement)
                    || isRunningLike(v20);
        } catch (Throwable e) {
            return false;
        }
    }

    private boolean isBacktestRunningStatus(String status) {
        return isRunningLike(status) || "BACKTESTING".equals(status);
    }

    private boolean isRunningLike(String status) {
        if (status == null) {
            return false;
        }

        return "RUNNING".equals(status)
                || "BUSY".equals(status)
                || "FETCHING".equals(status)
                || "ANALYZING".equals(status)
                || "BACKTESTING".equals(status)
                || "WAITING".equals(status)
                || "CHECKING".equals(status)
                || "SETTLING".equals(status);
    }

    private boolean isSettlementResultText(String r) {
        if (r == null) {
            return false;
        }

        try {
            r = AppConfig.normalizeResult(r);
        } catch (Throwable e) {
            r = r.trim().toUpperCase(java.util.Locale.ROOT);
        }

        return AppConfig.RESULT_WIN.equals(r)
                || AppConfig.RESULT_MISS.equals(r)
                || AppConfig.RESULT_DATA_ERROR.equals(r)
                || AppConfig.RESULT_VOID.equals(r)
                || "HIT".equals(r)
                || "LOSE".equals(r)
                || "LOSS".equals(r)
                || "命中".equals(r)
                || "未中".equals(r);
    }

    private String buildNextIssueText(String issue) {
        if (issue == null || issue.length() == 0) {
            return "";
        }

        try {
            if (dataGuard != null) {
                String next = dataGuard.buildNextIssue(issue);
                if (next != null && next.length() > 0) {
                    return next;
                }
            }
        } catch (Throwable e) {
        }

        String s = issue.trim();
        int end = s.length() - 1;

        while (end >= 0) {
            char ch = s.charAt(end);
            if (ch < '0' || ch > '9') {
                break;
            }
            end--;
        }

        String prefix = s.substring(0, end + 1);
        String digits = s.substring(end + 1);

        if (digits.length() == 0) {
            return s + "_NEXT";
        }

        try {
            long value = Long.parseLong(digits);
            long next = value + 1L;
            String nextText = String.valueOf(next);

            while (nextText.length() < digits.length()) {
                nextText = "0" + nextText;
            }

            return prefix + nextText;
        } catch (Throwable e) {
            return "";
        }
    }

    private boolean sameIssueText(String a, String b) {
        if (a == null || b == null) {
            return false;
        }

        String aa = a.trim();
        String bb = b.trim();

        if (aa.length() == 0 || bb.length() == 0) {
            return false;
        }

        if (aa.equals(bb)) {
            return true;
        }

        return normalizeIssueText(aa).equals(normalizeIssueText(bb));
    }

    private int compareIssueText(String a, String b) {
        String aa = normalizeIssueText(a);
        String bb = normalizeIssueText(b);

        if (aa.length() == 0 && bb.length() == 0) {
            return 0;
        }

        if (aa.length() == 0) {
            return -1;
        }

        if (bb.length() == 0) {
            return 1;
        }

        if (aa.length() > bb.length()) {
            return 1;
        }

        if (aa.length() < bb.length()) {
            return -1;
        }

        return aa.compareTo(bb);
    }

    private String normalizeIssueText(String issue) {
        if (issue == null) {
            return "";
        }

        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < issue.length(); i++) {
            char c = issue.charAt(i);

            if (c >= '0' && c <= '9') {
                sb.append(c);
            }
        }

        String s = sb.toString();

        while (s.length() > 1 && s.startsWith("0")) {
            s = s.substring(1);
        }

        return s;
    }

    private TextView findText(int id) {
        return (TextView) findViewById(id);
    }

    private void copyRealBetDiagnosisReport() {
        copyFullDiagnosisClosedLoopReport();
    }

    private void copyFullDiagnosisClosedLoopReport() {
        CompleteReportFileR927.export(this);
    }

    private String buildFullDiagnosisClosedLoopCopyText(DbStore db) {
        if (db == null) return "";
        StringBuilder sb = new StringBuilder(32768);
        sb.append("【星核天枢 R3.2.2 全软件智能检测报告】\n");
        sb.append("版本=").append(AppConfig.B07R322_VERSION).append('\n');
        sb.append("运行策略=").append(AppConfig.B07R322_RUNTIME_POLICY).append('\n');
        sb.append("报告策略=").append(AppConfig.B07R322_DIAG_POLICY).append('\n');
        sb.append("范围=B01-B15整体集成返修完成；运行与模型效果证据独立判定\n");
        sb.append("玩法红线=Binance1-PK10｜单位置｜固定K=3｜每期最多1个REAL BET｜DecisionGateEngine唯一真实BET出口\n");
        sb.append("统计红线=REAL与SHADOW严格分账；未被严格OOS/已结算证据证明的40/50/60%目标不得写成已达成\n");

        if (AppConfig.ENABLE_V50_PRIME) {
            appendFullDiagnosisSection(
                    sb,
                    "当前正式主链摘要",
                    firstNonEmpty(
                            safeState(db, "real_bet_diagnosis_detail"),
                            safeState(db, "real_bet_diagnosis_summary"),
                            safeState(db, "diagnosis_closed_loop_summary")));
        }
        appendFullDiagnosisSection(
                sb,
                "满级诊断总览",
                firstNonEmpty(
                        safeState(db, "diagnosis_closed_loop_summary"),
                        safeState(db, "real_bet_diagnosis_summary")));
        appendFullDiagnosisSection(
                sb,
                "真实BET完整复盘与统一动作漏斗",
                firstNonEmpty(
                        safeState(db, "real_bet_diagnosis_detail"),
                        safeState(db, "v44_decision_funnel_detail"),
                        safeState(db, "real_bet_diagnosis_summary")));
        appendFullDiagnosisSection(
                sb,
                "模块归因评分",
                firstNonEmpty(
                        safeState(db, "module_effect_detail"),
                        safeState(db, "module_effect_summary"),
                        contentValuesBrief(invokeNoArgObject(db, "getModuleEffectCache"), "模块评分")));
        appendFullDiagnosisSection(
                sb,
                "SHADOW反事实晋级",
                firstNonEmpty(
                        safeState(db, "shadow_promotion_detail"),
                        safeState(db, "shadow_promotion_summary"),
                        contentValuesBrief(
                                invokeNoArgObject(db, "getShadowPromotionCache"), "SHADOW晋级")));
        appendFullDiagnosisSection(
                sb,
                "动态冻结矩阵",
                firstNonEmpty(
                        safeState(db, "dynamic_freeze_detail"),
                        safeState(db, "dynamic_freeze_summary"),
                        contentValuesBrief(
                                invokeNoArgObject(db, "getDynamicFreezeState"), "冻结矩阵")));
        appendFullDiagnosisSection(
                sb,
                "智能审判",
                firstNonEmpty(
                        safeState(db, "adaptive_judge_detail"),
                        safeState(db, "adaptive_judge_summary"),
                        buildAdaptiveJudgeStateText(db)));
        appendFullDiagnosisSection(sb, "动态因果窗口诊断", buildDynamicCausalDiagnosisText(db));
        appendFullDiagnosisSection(
                sb,
                "文件修复建议",
                firstNonEmpty(
                        safeState(db, "diagnosis_fix_plan_detail"),
                        safeState(db, "diagnosis_fix_plan_summary"),
                        contentValuesBrief(
                                invokeNoArgObject(db, "getDiagnosisFixPlan"), "文件修复建议")));
        appendFullDiagnosisSection(
                sb,
                "R3.2.3后台存活真值",
                new ServiceHeartbeatStore(getApplicationContext()).runtimeTruthSummary());
        appendFullDiagnosisSection(
                sb, "现场崩溃/OOM/运行异常证据", CrashRecorder.detailedSummary(getApplicationContext()));
        appendFullDiagnosisSection(sb, "B01-B15逐批证据矩阵", buildR323BatchEvidenceAudit(db));
        appendFullDiagnosisSection(sb, "唯一正式主链与冲突检查", buildR323MainChainConflictAudit(db));
        appendFullDiagnosisSection(sb, "R3.2.2/R3.2.3全软件状态矩阵", buildR322WholeSoftwareStatus(db));
        appendFullDiagnosisSection(
                sb, "40/50/60胜率与15/30/40频率证据审判", buildR322TargetEvidenceJudge(db));
        appendFullDiagnosisSection(sb, "动态根因与下一步建议", buildR322RootCauseAdvice(db));
        sb.append("\n\n【不可绕过红线】UI/诊断只读；不得创建BET、救援BET、改概率、改门槛或代替结算；真实BET仍只认DecisionGateEngine。\n");
        sb.append("【真实性】报告只能解释当前证据，不承诺未来胜率；50%/60%只能作为研究候选门，必须重新经过严格PIT/OOS和实时已结算证据。\n");
        return sb.toString();
    }

    private void appendCompleteDiagnosisSection(
            StringBuilder sb, String title, String text, int valueCap) {
        if (sb == null || text == null || text.length() == 0) return;
        if (sb.length() > 0) sb.append("\n\n");
        sb.append("【").append(title).append("】\n");
        sb.append(translateLooseText(boundDiagnosisValueForCopy(text, valueCap)));
    }

    private String boundDiagnosisValueForCopy(String text, int cap) {
        String value = text == null ? "" : text;
        int safeCap = Math.max(2048, cap);
        if (value.length() <= safeCap) return value;
        int head = Math.max(1024, (safeCap - 220) * 3 / 4);
        int tail = Math.max(512, safeCap - head - 220);
        if (head + tail >= value.length()) return value;
        return value.substring(0, head)
                + "\n...[字段值有界化：原始长度="
                + value.length()
                + "，SHA256="
                + V51ChecksumEngine.sha256(value)
                + "；字段本身保留首尾用于审计，不删除任何诊断栏目]...\n"
                + value.substring(value.length() - tail);
    }

    private String buildR322WholeSoftwareStatus(DbStore db) {
        String[] keys =
                new String[] {
                    "active_runtime",
                    "legacy_runtime",
                    "service_status",
                    "service_core_summary",
                    "background_business_health",
                    "background_business_health_at",
                    "background_evidence_pump_status",
                    "api_line",
                    "last_settlement_issue",
                    "last_settlement_result",
                    "last_settlement_error",
                    "last_strict_trade_settlement_detail",
                    "decision_gate_status",
                    "decision_gate_summary",
                    "decision_stat_scope",
                    "decision_build_identity_status",
                    "decision_overwrite_blocked",
                    "dynamic_gate_status",
                    "dynamic_gate_summary",
                    "dynamic_gate_input_count",
                    "dynamic_gate_output_count",
                    "real_bet_diagnosis_status",
                    "real_bet_diagnosis_total",
                    "real_bet_diagnosis_hit",
                    "real_bet_diagnosis_miss",
                    "real_bet_diagnosis_win_rate",
                    "real_bet_diagnosis_pnl",
                    "real_bet_diagnosis_shadow_total",
                    "real_bet_diagnosis_shadow_hit",
                    "real_bet_diagnosis_shadow_rate",
                    "frequency_state_status",
                    "frequency_state_freq50",
                    "frequency_state_freq100",
                    "frequency_state_freq300",
                    "frequency_state_actual_bets",
                    "frequency_state_required_bets",
                    "frequency_state_debt",
                    "oos_last_settled_count",
                    "v28_oos_audit_sample",
                    "v28_oos_audit_hit_rate",
                    "v28_oos_audit_ece",
                    "v28_oos_audit_rank_status",
                    "v42_risk_coverage_mode",
                    "v42_risk_coverage_summary",
                    "module_effect_summary",
                    "shadow_promotion_summary",
                    "dynamic_freeze_summary",
                    "adaptive_judge_summary",
                    "real_bet_diagnosis_main_problem",
                    "real_bet_diagnosis_next_fix",
                    "r32_ui_startup_policy",
                    "r32_service_startup_policy",
                    "r32_service_init_status",
                    "r32_service_init_error"
                };
        StringBuilder out = new StringBuilder(8192);
        for (int i = 0; i < keys.length; i++) {
            String v = safeState(db, keys[i]);
            out.append(keys[i])
                    .append('=')
                    .append(v.length() == 0 ? "--" : limitText(v, 1800))
                    .append('\n');
        }
        out.append("B01-B15工程身份=").append(AppConfig.R36_RUNTIME_REPAIR_VERSION).append('\n');
        out.append("冷启动Provider=REMOVED_R322\\n");
        out.append("Application=LIGHT_ONLY_NO_DB_NO_MODEL\\n");
        out.append("UI_DB=BACKGROUND_INIT\\n");
        out.append("Service=FOREGROUND_FIRST_BACKGROUND_HEAVY_INIT\\n");
        out.append("真实BET出口=DecisionGateEngine_ONLY\\n");
        return out.toString();
    }

    private String buildR322TargetEvidenceJudge(DbStore db) {
        double realRate = 100.0d * parseDouble(safeState(db, "real_bet_diagnosis_win_rate"), -1.0d);
        double f50 = 100.0d * parseDouble(safeState(db, "frequency_state_freq50"), -1.0d);
        double f100 = 100.0d * parseDouble(safeState(db, "frequency_state_freq100"), -1.0d);
        double f300 = 100.0d * parseDouble(safeState(db, "frequency_state_freq300"), -1.0d);
        double ece = parseDouble(safeState(db, "v28_oos_audit_ece"), -1.0d);
        double oosHit = 100.0d * parseDouble(safeState(db, "v28_oos_audit_hit_rate"), -1.0d);
        int realN = (int) parseDouble(safeState(db, "real_bet_diagnosis_total"), 0.0d);
        int oosN =
                (int)
                        parseDouble(
                                firstNonEmpty(
                                        safeState(db, "v28_oos_audit_sample"),
                                        safeState(db, "oos_last_settled_count")),
                                0.0d);
        double stableFreq = f300 >= 0.0d ? f300 : (f100 >= 0.0d ? f100 : f50);
        StringBuilder out = new StringBuilder(4096);
        out.append("REAL已结算样本=")
                .append(realN)
                .append("｜REAL胜率=")
                .append(realRate < 0 ? "无证据" : String.valueOf(realRate))
                .append('\n');
        out.append("严格OOS样本=")
                .append(oosN)
                .append("｜OOS命中=")
                .append(oosHit < 0 ? "无证据" : String.valueOf(oosHit))
                .append("｜ECE=")
                .append(ece < 0 ? "无证据" : String.valueOf(ece))
                .append('\n');
        out.append("频率50/100/300=")
                .append(f50)
                .append('/')
                .append(f100)
                .append('/')
                .append(f300)
                .append('\n');
        out.append("40%胜率门=")
                .append(r322EvidenceGate(realRate, realN, oosHit, oosN, 40.0d))
                .append('\n');
        out.append("50%胜率研究门=")
                .append(r322EvidenceGate(realRate, realN, oosHit, oosN, 50.0d))
                .append('\n');
        out.append("60%胜率研究门=")
                .append(r322EvidenceGate(realRate, realN, oosHit, oosN, 60.0d))
                .append('\n');
        out.append("15%频率门=").append(r322FrequencyGate(stableFreq, 15.0d)).append('\n');
        out.append("30%频率研究门=").append(r322FrequencyGate(stableFreq, 30.0d)).append('\n');
        out.append("40%频率研究门=").append(r322FrequencyGate(stableFreq, 40.0d)).append('\n');
        out.append("判定原则=不能靠放宽DecisionGate或硬补频率宣称达标；必须同时看样本量、严格OOS、实时已结算、校准和频率。\\n");
        return out.toString();
    }

    private String r322EvidenceGate(
            double realRate, int realN, double oosHit, int oosN, double target) {
        if (realN < 30 || oosN < 30 || realRate < 0.0d || oosHit < 0.0d)
            return "NOT_PROVEN｜原因=样本或OOS证据不足";
        if (realRate >= target && oosHit >= target) return "EVIDENCE_PASS_CANDIDATE｜仍需持续实时漂移监控";
        return "NOT_PROVEN｜原因=REAL或严格OOS未同时达到" + target + "%";
    }

    private String r322FrequencyGate(double freq, double target) {
        if (freq < 0.0d) return "NOT_PROVEN｜原因=缺少稳定频率统计";
        return freq >= target
                ? "PASS_CANDIDATE｜频率=" + freq
                : "NOT_PROVEN｜当前频率=" + freq + "，目标=" + target;
    }

    private String buildR322RootCauseAdvice(DbStore db) {
        String service =
                firstNonEmpty(
                        safeState(db, "background_business_health"),
                        safeState(db, "service_status"));
        String api = safeState(db, "api_line");
        String settleError = safeState(db, "last_settlement_error");
        String gate =
                firstNonEmpty(
                        safeState(db, "decision_gate_status"),
                        safeState(db, "decision_gate_summary"));
        String modelProblem =
                firstNonEmpty(
                        safeState(db, "real_bet_diagnosis_main_problem"),
                        safeState(db, "real_bet_diagnosis_next_fix"));
        double rate = 100.0d * parseDouble(safeState(db, "real_bet_diagnosis_win_rate"), -1.0d);
        double freq =
                100.0d
                        * parseDouble(
                                firstNonEmpty(
                                        safeState(db, "frequency_state_freq300"),
                                        safeState(db, "frequency_state_freq100"),
                                        safeState(db, "frequency_state_freq50")),
                                -1.0d);
        int n = (int) parseDouble(safeState(db, "real_bet_diagnosis_total"), 0.0d);
        StringBuilder out = new StringBuilder(4096);
        if (service.length() == 0 || service.indexOf("FAIL") >= 0 || service.indexOf("DEAD") >= 0) {
            out.append(
                    "P0_BACKGROUND：后台证据异常，先修前台服务/Alarm/Job/Watchdog/厂商电池策略；后台不连续时任何胜率统计都不可信。\\n");
        }
        if (api.length() == 0 || api.indexOf("FAIL") >= 0 || api.indexOf("ERROR") >= 0) {
            out.append("P0_API：接口链证据异常，优先修主备切换、开奖完整排列校验、补漏与限流，不允许用脏数据训练或结算。\\n");
        }
        if (settleError.length() > 0 && !"--".equals(settleError) && !"0".equals(settleError)) {
            out.append("P0_SETTLEMENT：存在结算错误=")
                    .append(limitText(settleError, 600))
                    .append("；先修targetIssue/position/K3严格结算，禁止把SHADOW混入REAL。\\n");
        }
        if (n < 30) {
            out.append("EVIDENCE_SHORTAGE：REAL已结算样本不足30，当前不能判断策略公式好坏，也不能据此追求50/60%。\\n");
        } else if (rate >= 0.0d && rate < 40.0d) {
            out.append(
                    "MODEL_OR_SELECTION：REAL胜率低于40时，不应硬放宽门。优先检查PIT特征泄漏、概率校准、Rank/Third/Window/Regime/Motif/Relation/Consensus失败桶、Teacher层级、ExpertRegret与状态漂移；新增候选策略必须先走SHADOW→严格OOS→晋级。\\n");
        }
        if (freq >= 0.0d && freq < 15.0d) {
            out.append(
                    "FREQUENCY_LOW：稳定频率低于15%。先统计‘有优势但被哪一道门拦截’，只对经OOS证明过度保守的门做分状态校准；禁止频率债务补单。要研究30/40%频率，应增加互补且低相关的已证明专家/状态域覆盖，而不是降低统一门槛。\\n");
        }
        out.append("主链检查=").append(gate.length() == 0 ? "无状态" : limitText(gate, 1000)).append('\n');
        if (modelProblem.length() > 0)
            out.append("现有诊断根因=").append(limitText(modelProblem, 1500)).append('\n');
        out.append(
                "50/60%研究路径=只能作为候选研究目标：扩展互补专家（状态/因果/排序/校准/漂移/反事实），每个新专家必须有PIT输入、独立OOS证书、SHADOW晋级和ExpertRegret淘汰；没有证据不得接入REAL主链。\\n");
        out.append(
                "频率提升路径=优先扩大‘已证明有效状态域’覆盖，做域路由与专家组合，而不是从40%胜率门向下放水；每次提高频率都同时复核Wilson下界/ECE/最长连挂/分块稳定性。\\n");
        return out.toString();
    }

    private String buildR323BatchEvidenceAudit(DbStore db) {
        StringBuilder out = new StringBuilder(4096);
        ServiceHeartbeatStore hb = new ServiceHeartbeatStore(getApplicationContext());
        out.append("版本=").append(AppConfig.B07R323_VERSION).append('\n');
        out.append("B01_工程身份/红线=IMPLEMENTED｜固定Binance1-PK10/单位置/K3/DecisionGate唯一BET出口\n");
        out.append("B02_数据库与账本=")
                .append(
                        startupDbReady
                                ? "RUNTIME_READY"
                                : (startupDbError.length() == 0
                                        ? "NO_RUNTIME_EVIDENCE"
                                        : "FAULT:" + limitText(startupDbError, 180)))
                .append('\n');
        String api =
                firstNonEmpty(
                        safeState(db, "api_line"),
                        safeState(db, "last_api_line"),
                        safeState(db, "last_fetch_issue"));
        out.append("B03_API抓取/补漏=")
                .append(
                        api.length() == 0
                                ? "NO_CURRENT_RUNTIME_EVIDENCE"
                                : "EVIDENCE_PRESENT:" + limitText(api, 240))
                .append('\n');
        String b04 =
                firstNonEmpty(
                        safeState(db, "b04_feature_graph_status"),
                        safeState(db, "v51_feature_status"));
        out.append("B04_特征图/滚动缓存=")
                .append(b04.length() == 0 ? "NO_CURRENT_RUNTIME_EVIDENCE" : limitText(b04, 240))
                .append('\n');
        String b05 =
                firstNonEmpty(
                        safeState(db, "b05_state_status"), safeState(db, "b05_state_summary"));
        out.append("B05_状态/趋势/漂移=")
                .append(b05.length() == 0 ? "NO_CURRENT_RUNTIME_EVIDENCE" : limitText(b05, 240))
                .append('\n');
        String b06 =
                firstNonEmpty(
                        safeState(db, "decision_gate_status"),
                        safeState(db, "decision_gate_summary"),
                        safeState(db, "v51_last_authorized_action"));
        out.append("B06_概率/校准/选择性动作/DecisionGate=")
                .append(b06.length() == 0 ? "NO_CURRENT_RUNTIME_EVIDENCE" : limitText(b06, 320))
                .append('\n');
        out.append("B07_后台/恢复/诊断=").append(hb.runtimeTruthSummary()).append('\n');
        out.append("B08=ENGINEERING_COMPLETE｜Evidence/Causal/Counterfactual isolated\n");
        out.append(
                "判定规则=IMPLEMENTED只代表代码存在；RUNTIME_READY/EVIDENCE_PRESENT才代表本机有运行证据；NO_CURRENT_RUNTIME_EVIDENCE不得伪报完成效果。\n");
        return out.toString();
    }

    private String buildR323MainChainConflictAudit(DbStore db) {
        StringBuilder out = new StringBuilder(3072);
        out.append("正式实时链=V51_TIANSHU_ONLY\n");
        out.append("ENABLE_V51_TIANSHU=").append(AppConfig.ENABLE_V51_TIANSHU).append('\n');
        out.append("ENABLE_V50_PRIME=").append(AppConfig.ENABLE_V50_PRIME).append('\n');
        out.append("LEGACY_READ_ONLY_OBSERVER=")
                .append(AppConfig.LEGACY_READ_ONLY_OBSERVER)
                .append('\n');
        out.append("DecisionGate唯一真实BET出口=")
                .append(AppConfig.STRICT_DECISION_GATE_ONLY_CAN_BET)
                .append('\n');
        out.append("SHADOW不计真实统计=")
                .append(AppConfig.STRICT_SHADOW_NOT_COUNT_REAL_STATS)
                .append('\n');
        out.append("K4=")
                .append(AppConfig.ALLOW_K4)
                .append("｜动态K=")
                .append(AppConfig.ALLOW_DYNAMIC_K)
                .append("｜多位置BET=")
                .append(AppConfig.ALLOW_MULTI_POSITION_BET)
                .append("｜追期=")
                .append(AppConfig.ALLOW_MULTI_ISSUE_CHASE)
                .append("｜马丁=")
                .append(AppConfig.ALLOW_MARTINGALE)
                .append("｜加倍=")
                .append(AppConfig.ALLOW_DOUBLE_BET)
                .append('\n');
        out.append("buildIdentity=")
                .append(
                        limitText(
                                firstNonEmpty(
                                        safeState(db, "decision_build_identity_status"),
                                        safeState(db, "autofetch_build_identity_status")),
                                600))
                .append('\n');
        out.append("overwriteBlocked=")
                .append(limitText(safeState(db, "decision_overwrite_blocked"), 300))
                .append('\n');
        return out.toString();
    }

    private void copyTextToClipboard(String label, String text) {
        try {
            String safeText = text == null ? "" : text;
            Object service = getSystemService(Context.CLIPBOARD_SERVICE);
            if (Build.VERSION.SDK_INT >= 11
                    && service instanceof android.content.ClipboardManager) {
                android.content.ClipboardManager cm = (android.content.ClipboardManager) service;
                cm.setPrimaryClip(
                        android.content.ClipData.newPlainText(
                                label == null ? "text" : label, safeText));
            } else if (service instanceof android.text.ClipboardManager) {
                android.text.ClipboardManager old = (android.text.ClipboardManager) service;
                old.setText(safeText);
            }
        } catch (Throwable e) {
            try {
                toast("一次复制失败：" + limitText(String.valueOf(e), 180));
            } catch (Throwable ignore) {
            }
        }
    }

    private void setText(TextView tv, String text) {
        if (tv == null) {
            return;
        }

        String value = text == null ? "" : text;
        int localLimit = UI_TEXT_RENDER_HARD_LIMIT;
        if (heavyRenderBudgetActive) {
            localLimit = Math.min(localLimit, Math.max(0, heavyRenderBudgetRemaining));
            if (localLimit <= 0) {
                value = "【界面轻量显示】本轮重型内容已达到内存预算，完整内容请分段复制。";
            } else if (value.length() > localLimit) {
                value = value.substring(0, localLimit) + "\n\n【界面内存保护】本区域已按本轮总预算截断；完整诊断请分段复制。";
            }
            heavyRenderBudgetRemaining = Math.max(0, heavyRenderBudgetRemaining - value.length());
        } else if (value.length() > localLimit) {
            value =
                    value.substring(0, localLimit)
                            + "\n\n【界面内存保护】正文过长，界面只显示前"
                            + localLimit
                            + "字；完整诊断请使用分段复制。";
        }
        CharSequence old = tv.getText();
        if (TextUtils.equals(old, value)) {
            return;
        }
        try {
            tv.setText(value);
            consecutiveUiOom = 0;
        } catch (OutOfMemoryError oom) {
            consecutiveUiOom++;
            heavyRenderBudgetRemaining = 0;
            cachedFullDiagnosisCopyText = "";
            try {
                RollingWindowCache.shared().clear();
                FullInformationFeatureGraphEngine.shared().clear();
                CrashRecorder.recordCaught(
                        getApplicationContext(), oom, "UI_TEXT_OOM_GUARD_" + consecutiveUiOom);
            } catch (Throwable ignore) {
            }
            try {
                tv.setText("【界面内存保护已启动】该区域内容过大，已停止渲染；后台服务不应因此中断。请使用分段复制报告。");
            } catch (Throwable ignore) {
            }
        }
    }

    private boolean isLowMemoryPressure() {
        try {
            Runtime runtime = Runtime.getRuntime();
            long max = Math.max(1L, runtime.maxMemory());
            long used = runtime.totalMemory() - runtime.freeMemory();
            long remaining = Math.max(0L, max - used);
            return remaining < Math.max(2L * 1024L * 1024L, max / 10L);
        } catch (Throwable ignore) {
            return false;
        }
    }

    private void setTextColor(TextView tv, int colorRes) {
        if (tv != null) {
            tv.setTextColor(getColorCompat(colorRes));
        }
    }

    private int getColorCompat(int colorRes) {
        try {
            if (Build.VERSION.SDK_INT >= 23) {
                return getResources().getColor(colorRes, getTheme());
            }
            return getResources().getColor(colorRes);
        } catch (Throwable e) {
            return 0xffffffff;
        }
    }

    private String safeDash(String s) {
        if (s == null || s.length() == 0) {
            return "--";
        }
        return s;
    }

    private String safeText(String s) {
        return s == null ? "" : s;
    }

    private String limitText(String s, int max) {
        if (s == null) {
            return "";
        }

        if (max <= 0 || s.length() <= max) {
            return s;
        }

        return s.substring(0, max) + "...";
    }

    private String removePnlText(String text) {
        if (text == null || text.length() == 0) {
            return "";
        }

        String s = text;

        try {
            s = s.replaceAll("｜真实盈亏=[^｜\\n]*", "");
            s = s.replaceAll("｜盈亏=[^｜\\n]*", "");
            s = s.replaceAll("，盈亏=[^，；｜\\n]*", "");
            s = s.replaceAll(" pnl=[^｜\\n]*", "");
            s = s.replaceAll(" PNL=[^｜\\n]*", "");
            s = s.replaceAll("pnl=[^｜\\n]*", "");
            s = s.replaceAll("PNL=[^｜\\n]*", "");
        } catch (Throwable e) {
        }

        return s;
    }

    private int dp(int value) {
        float density = getResources().getDisplayMetrics().density;
        return (int) (value * density + 0.5f);
    }

    private void toast(String text) {
        Toast.makeText(this, text == null ? "" : text, Toast.LENGTH_SHORT).show();
    }

    private long parseLong(String text, long fallback) {
        if (text == null || text.length() == 0) {
            return fallback;
        }

        try {
            return Long.parseLong(text);
        } catch (Throwable e) {
            return fallback;
        }
    }

    private double parseDouble(String text, double fallback) {
        if (text == null || text.length() == 0) {
            return fallback;
        }

        try {
            String s = text.trim();

            if (s.endsWith("%")) {
                s = s.substring(0, s.length() - 1);
                double p = Double.parseDouble(s) / 100.0d;
                if (Double.isNaN(p) || Double.isInfinite(p)) {
                    return fallback;
                }
                return p;
            }

            double d = Double.parseDouble(s);
            if (Double.isNaN(d) || Double.isInfinite(d)) {
                return fallback;
            }
            return d;
        } catch (Throwable e) {
            return fallback;
        }
    }

    private int readInt(Object obj, String fieldName, int fallback) {
        if (obj == null || fieldName == null || fieldName.length() == 0) {
            return fallback;
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

        return fallback;
    }

    private double readDouble(Object obj, String fieldName, double fallback) {
        if (obj == null || fieldName == null || fieldName.length() == 0) {
            return fallback;
        }

        try {
            Field f = obj.getClass().getField(fieldName);
            Object v = f.get(obj);

            if (v instanceof Number) {
                double d = ((Number) v).doubleValue();
                if (Double.isNaN(d) || Double.isInfinite(d)) {
                    return fallback;
                }
                return d;
            }

            if (v instanceof String) {
                double d = Double.parseDouble((String) v);
                if (Double.isNaN(d) || Double.isInfinite(d)) {
                    return fallback;
                }
                return d;
            }
        } catch (Throwable e) {
        }

        return fallback;
    }

    private String getFieldText(Object obj, String fieldName) {
        if (obj == null || fieldName == null || fieldName.length() == 0) {
            return "";
        }

        try {
            Field f = obj.getClass().getField(fieldName);
            Object v = f.get(obj);
            return v == null ? "" : String.valueOf(v);
        } catch (Throwable e) {
            return "";
        }
    }

    private void writeUiState(final String key, final String value) {
        final DbStore target = store;
        Handler background = uiReadHandler;
        if (target == null || background == null || destroyed) return;
        background.post(new Runnable() {
            @Override public void run() {
                try {
                    target.setState(key, value == null ? "" : value);
                } catch (RuntimeException error) {
                    CrashRecorder.recordCaught(getApplicationContext(), error,
                            "R944_UI_REQUEST_STATE_WRITE");
                }
            }
        });
    }

    private String stateOf(LightSnapshot snapshot, String key) {
        if (snapshot == null || snapshot.states == null || key == null) {
            return "";
        }

        String v = snapshot.states.get(key);
        return v == null ? "" : v;
    }

    private void appendLine(StringBuilder sb, String text) {
        if (sb == null || text == null || text.length() == 0) {
            return;
        }

        if (sb.length() > 0) {
            sb.append("\n");
        }

        sb.append(text);
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

    private String firstNonEmpty(String a, String b) {
        if (a != null && a.length() > 0) {
            return a;
        }

        if (b != null && b.length() > 0) {
            return b;
        }

        return "";
    }

    private String firstNonEmpty(String a, String b, String c) {
        String x = firstNonEmpty(a, b);
        if (x.length() > 0) {
            return x;
        }

        return c == null ? "" : c;
    }

    private String firstNonEmpty(String a, String b, String c, String d) {
        String x = firstNonEmpty(a, b, c);
        if (x.length() > 0) {
            return x;
        }

        return d == null ? "" : d;
    }

    private String firstNonEmpty(String a, String b, String c, String d, String e) {
        String x = firstNonEmpty(a, b, c, d);
        if (x.length() > 0) {
            return x;
        }

        return e == null ? "" : e;
    }

    private String formatMaybeProbability(String text) {
        if (text == null || text.length() == 0) {
            return "";
        }

        double d = parseDouble(text, -999.0d);

        if (d > -998.0d) {
            if (d >= 0.0d && d <= 1.0d) {
                return UiFormatter.probability(d);
            }

            return UiFormatter.score(d);
        }

        return text;
    }

    private String buildV20EvidenceStateText(LightSnapshot snapshot) {
        return buildV20EvidenceUiText(null, snapshot, UI_V20_EVIDENCE_SHORT_LIMIT);
    }

    private String buildV20EvidenceBriefForMiniCard(Object obj) {
        String action =
                firstV20Text(
                        obj,
                        null,
                        new String[] {
                            "decisionActionAdvice",
                            "finalDecisionActionAdvice",
                            "dynamicGateActionAdvice",
                            "probabilityActionAdvice",
                            "teacherActionAdvice",
                            "dynamicActionAdvice",
                            "actionAdvice"
                        },
                        null);
        double sample =
                firstV20Double(
                        obj,
                        null,
                        new String[] {
                            "decisionSampleReliability",
                            "finalDecisionSampleReliability",
                            "dynamicGateSampleReliability",
                            "probabilitySampleReliability",
                            "teacherSampleTrust",
                            "sampleReliability",
                            "sampleTrust"
                        },
                        null,
                        -999.0d);
        double recent =
                firstV20Double(
                        obj,
                        null,
                        new String[] {
                            "decisionRecentValidity",
                            "finalDecisionRecentValidity",
                            "dynamicGateRecentValidity",
                            "probabilityRecentValidity",
                            "teacherRecentEffectiveness",
                            "recentValidity",
                            "recentEffectiveness"
                        },
                        null,
                        -999.0d);
        StringBuilder sb = new StringBuilder();
        if (sample > -998.0d) {
            sb.append("样本可信=");
            sb.append(formatV20Metric(sample));
        }
        if (recent > -998.0d) {
            if (sb.length() > 0) {
                sb.append(" ");
            }
            sb.append("近期有效=");
            sb.append(formatV20Metric(recent));
        }
        if (action.length() > 0) {
            if (sb.length() > 0) {
                sb.append(" ");
            }
            sb.append("建议=");
            sb.append(limitText(translateLooseText(action), 46));
        }
        return limitText(sb.toString(), 120);
    }

    private String buildV20EvidenceUiText(Object obj, LightSnapshot snapshot, int maxLen) {
        String windowKey =
                firstV20Text(
                        obj,
                        snapshot,
                        new String[] {
                            "finalDecisionWindowStateKey",
                            "decisionWindowStateKey",
                            "dynamicGateWindowStateKey",
                            "probabilityWindowStateKey",
                            "teacherWindowStateKey",
                            "dynamicWindowStateKey",
                            "windowStateKey",
                            "stateKey"
                        },
                        new String[] {
                            "final_decision_window_state_key",
                            "decision_window_state_key",
                            "dynamic_gate_window_state_key",
                            "probability_window_state_key",
                            "teacher_window_state_key",
                            "dynamic_window_state_key",
                            "window_state_key"
                        });
        String s12 =
                firstV20Text(
                        obj,
                        snapshot,
                        new String[] {
                            "decisionStateKey12",
                            "dynamicGateStateKey12",
                            "probabilityStateKey12",
                            "teacherStateKey12",
                            "dynamicStateKey12",
                            "stateKey12"
                        },
                        new String[] {
                            "decision_state_key_12",
                            "dynamic_gate_state_key_12",
                            "probability_state_key_12",
                            "teacher_state_key_12",
                            "dynamic_state_key_12",
                            "state_key_12"
                        });
        String s24 =
                firstV20Text(
                        obj,
                        snapshot,
                        new String[] {
                            "decisionStateKey24",
                            "dynamicGateStateKey24",
                            "probabilityStateKey24",
                            "teacherStateKey24",
                            "dynamicStateKey24",
                            "stateKey24"
                        },
                        new String[] {
                            "decision_state_key_24",
                            "dynamic_gate_state_key_24",
                            "probability_state_key_24",
                            "teacher_state_key_24",
                            "dynamic_state_key_24",
                            "state_key_24"
                        });
        String s48 =
                firstV20Text(
                        obj,
                        snapshot,
                        new String[] {
                            "decisionStateKey48",
                            "dynamicGateStateKey48",
                            "probabilityStateKey48",
                            "teacherStateKey48",
                            "dynamicStateKey48",
                            "stateKey48"
                        },
                        new String[] {
                            "decision_state_key_48",
                            "dynamic_gate_state_key_48",
                            "probability_state_key_48",
                            "teacher_state_key_48",
                            "dynamic_state_key_48",
                            "state_key_48"
                        });
        String s96 =
                firstV20Text(
                        obj,
                        snapshot,
                        new String[] {
                            "decisionStateKey96",
                            "dynamicGateStateKey96",
                            "probabilityStateKey96",
                            "teacherStateKey96",
                            "dynamicStateKey96",
                            "stateKey96"
                        },
                        new String[] {
                            "decision_state_key_96",
                            "dynamic_gate_state_key_96",
                            "probability_state_key_96",
                            "teacher_state_key_96",
                            "dynamic_state_key_96",
                            "state_key_96"
                        });
        String s360 =
                firstV20Text(
                        obj,
                        snapshot,
                        new String[] {
                            "decisionStateKey360",
                            "dynamicGateStateKey360",
                            "probabilityStateKey360",
                            "teacherStateKey360",
                            "dynamicStateKey360",
                            "stateKey360"
                        },
                        new String[] {
                            "decision_state_key_360",
                            "dynamic_gate_state_key_360",
                            "probability_state_key_360",
                            "teacher_state_key_360",
                            "dynamic_state_key_360",
                            "state_key_360"
                        });
        double evidence =
                firstV20Double(
                        obj,
                        snapshot,
                        new String[] {
                            "finalDecisionEvidenceContribution",
                            "decisionEvidenceContribution",
                            "dynamicGateEvidenceContribution",
                            "probabilityEvidenceContribution",
                            "teacherEvidenceContribution",
                            "dynamicEvidenceContribution",
                            "evidenceContribution"
                        },
                        new String[] {
                            "decision_evidence_contribution",
                            "dynamic_gate_evidence_contribution",
                            "probability_evidence_contribution",
                            "teacher_evidence_contribution",
                            "dynamic_evidence_contribution",
                            "evidence_contribution"
                        },
                        -999.0d);
        double counter =
                firstV20Double(
                        obj,
                        snapshot,
                        new String[] {
                            "finalDecisionCounterContribution",
                            "decisionCounterContribution",
                            "dynamicGateCounterContribution",
                            "probabilityCounterContribution",
                            "teacherCounterContribution",
                            "dynamicCounterContribution",
                            "counterContribution"
                        },
                        new String[] {
                            "decision_counter_contribution",
                            "dynamic_gate_counter_contribution",
                            "probability_counter_contribution",
                            "teacher_counter_contribution",
                            "dynamic_counter_contribution",
                            "counter_contribution"
                        },
                        -999.0d);
        double sample =
                firstV20Double(
                        obj,
                        snapshot,
                        new String[] {
                            "finalDecisionSampleReliability",
                            "decisionSampleReliability",
                            "dynamicGateSampleReliability",
                            "probabilitySampleReliability",
                            "teacherSampleTrust",
                            "dynamicSampleTrust",
                            "sampleReliability",
                            "sampleTrust"
                        },
                        new String[] {
                            "decision_sample_reliability",
                            "dynamic_gate_sample_reliability",
                            "probability_sample_reliability",
                            "teacher_sample_trust",
                            "dynamic_sample_trust",
                            "sample_reliability",
                            "sample_trust"
                        },
                        -999.0d);
        double recent =
                firstV20Double(
                        obj,
                        snapshot,
                        new String[] {
                            "finalDecisionRecentValidity",
                            "decisionRecentValidity",
                            "dynamicGateRecentValidity",
                            "probabilityRecentValidity",
                            "teacherRecentEffectiveness",
                            "dynamicRecentEffectiveness",
                            "recentValidity",
                            "recentEffectiveness"
                        },
                        new String[] {
                            "decision_recent_validity",
                            "dynamic_gate_recent_validity",
                            "probability_recent_validity",
                            "teacher_recent_effectiveness",
                            "dynamic_recent_effectiveness",
                            "recent_validity",
                            "recent_effectiveness"
                        },
                        -999.0d);
        double distance =
                firstV20Double(
                        obj,
                        snapshot,
                        new String[] {
                            "decisionFeatureDistance",
                            "dynamicGateFeatureDistance",
                            "probabilityFeatureDistance",
                            "teacherFeatureDistance",
                            "dynamicFeatureDistance",
                            "featureDistance"
                        },
                        new String[] {
                            "decision_feature_distance",
                            "dynamic_gate_feature_distance",
                            "probability_feature_distance",
                            "teacher_feature_distance",
                            "dynamic_feature_distance",
                            "feature_distance"
                        },
                        -999.0d);
        int simSample =
                firstV20Int(
                        obj,
                        snapshot,
                        new String[] {
                            "decisionSimilarWindowSample",
                            "dynamicGateSimilarWindowSample",
                            "probabilitySimilarWindowSample",
                            "teacherSimilarWindowSample",
                            "dynamicSimilarWindowSample",
                            "similarWindowSample"
                        },
                        new String[] {
                            "decision_similar_window_sample",
                            "dynamic_gate_similar_window_sample",
                            "probability_similar_window_sample",
                            "teacher_similar_window_sample",
                            "dynamic_similar_window_sample",
                            "similar_window_sample"
                        },
                        0);
        double simRate =
                firstV20Double(
                        obj,
                        snapshot,
                        new String[] {
                            "decisionSimilarWindowHitRate",
                            "dynamicGateSimilarWindowHitRate",
                            "probabilitySimilarWindowHitRate",
                            "teacherSimilarWindowHitRate",
                            "dynamicSimilarWindowHitRate",
                            "similarWindowHitRate"
                        },
                        new String[] {
                            "decision_similar_window_hit_rate",
                            "dynamic_gate_similar_window_hit_rate",
                            "probability_similar_window_hit_rate",
                            "teacher_similar_window_hit_rate",
                            "dynamic_similar_window_hit_rate",
                            "similar_window_hit_rate"
                        },
                        -999.0d);
        String evidenceText =
                firstV20Text(
                        obj,
                        snapshot,
                        new String[] {
                            "finalDecisionEvidenceText",
                            "decisionEvidenceText",
                            "dynamicGateEvidenceText",
                            "probabilityEvidenceText",
                            "teacherEvidenceText",
                            "dynamicEvidenceText",
                            "evidenceText",
                            "evidenceSupportText"
                        },
                        new String[] {
                            "decision_evidence_text",
                            "dynamic_gate_evidence_text",
                            "probability_evidence_text",
                            "teacher_evidence_text",
                            "dynamic_evidence_text",
                            "evidence_text",
                            "evidence_support_text"
                        });
        String counterText =
                firstV20Text(
                        obj,
                        snapshot,
                        new String[] {
                            "finalDecisionCounterEvidenceText",
                            "decisionCounterEvidenceText",
                            "dynamicGateCounterEvidenceText",
                            "probabilityCounterEvidenceText",
                            "teacherCounterEvidenceText",
                            "dynamicCounterEvidenceText",
                            "counterEvidenceText",
                            "counterText"
                        },
                        new String[] {
                            "decision_counter_evidence_text",
                            "dynamic_gate_counter_evidence_text",
                            "probability_counter_evidence_text",
                            "teacher_counter_evidence_text",
                            "dynamic_counter_evidence_text",
                            "counter_evidence_text",
                            "counter_text"
                        });
        String action =
                firstV20Text(
                        obj,
                        snapshot,
                        new String[] {
                            "finalDecisionActionAdvice",
                            "decisionActionAdvice",
                            "dynamicGateActionAdvice",
                            "probabilityActionAdvice",
                            "teacherActionAdvice",
                            "dynamicActionAdvice",
                            "actionAdvice"
                        },
                        new String[] {
                            "decision_action_advice",
                            "dynamic_gate_action_advice",
                            "probability_action_advice",
                            "teacher_action_advice",
                            "dynamic_action_advice",
                            "action_advice"
                        });
        boolean has =
                windowKey.length() > 0
                        || s12.length() > 0
                        || s24.length() > 0
                        || s48.length() > 0
                        || s96.length() > 0
                        || s360.length() > 0
                        || evidence > -998.0d
                        || counter > -998.0d
                        || sample > -998.0d
                        || recent > -998.0d
                        || distance > -998.0d
                        || simSample > 0
                        || simRate > -998.0d
                        || evidenceText.length() > 0
                        || counterText.length() > 0
                        || action.length() > 0;
        if (!has) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        if (windowKey.length() > 0) {
            appendInline(sb, "WindowState=" + limitText(translateLooseText(windowKey), 110));
        }
        String windows = buildWindowStateCompact(s12, s24, s48, s96, s360);
        if (windows.length() > 0) {
            appendInline(sb, windows);
        }
        if (evidence > -998.0d) {
            appendInline(sb, "证据贡献=" + formatV20Metric(evidence));
        }
        if (counter > -998.0d) {
            appendInline(sb, "反证贡献=" + formatV20Metric(counter));
        }
        if (sample > -998.0d) {
            appendInline(sb, "样本可信=" + formatV20Metric(sample));
        }
        if (recent > -998.0d) {
            appendInline(sb, "近期有效=" + formatV20Metric(recent));
        }
        if (distance > -998.0d) {
            appendInline(sb, "特征距离=" + formatV20Metric(distance));
        }
        if (simSample > 0 || simRate > -998.0d) {
            String sim = "相似窗=" + simSample;
            if (simRate > -998.0d) {
                sim += "/" + formatV20Metric(simRate);
            }
            appendInline(sb, sim);
        }
        if (evidenceText.length() > 0) {
            appendInline(sb, "证据=" + limitText(translateLooseText(evidenceText), 120));
        }
        if (counterText.length() > 0) {
            appendInline(sb, "反证=" + limitText(translateLooseText(counterText), 120));
        }
        if (action.length() > 0) {
            appendInline(sb, "建议=" + limitText(translateLooseText(action), 110));
        }
        appendInline(sb, "说明=同输入同输出，可复查可回放；UI只展示，不生成BET");
        return limitText(
                removePnlText(sb.toString()), maxLen <= 0 ? UI_V20_EVIDENCE_LIMIT : maxLen);
    }

    private String buildBroadcastV20EvidenceText(Intent intent) {
        if (intent == null) {
            return "";
        }
        String windowKey =
                firstIntentString(
                        intent,
                        new String[] {
                            "finalDecisionWindowStateKey",
                            "decisionWindowStateKey",
                            "dynamicGateWindowStateKey",
                            "probabilityWindowStateKey",
                            "windowStateKey",
                            "state_key",
                            "window_state_key"
                        });
        double evidence =
                firstIntentDouble(
                        intent,
                        new String[] {
                            "decisionEvidenceContribution",
                            "dynamicGateEvidenceContribution",
                            "probabilityEvidenceContribution",
                            "evidenceContribution",
                            "evidence_contribution"
                        },
                        -999.0d);
        double counter =
                firstIntentDouble(
                        intent,
                        new String[] {
                            "decisionCounterContribution",
                            "dynamicGateCounterContribution",
                            "probabilityCounterContribution",
                            "counterContribution",
                            "counter_contribution"
                        },
                        -999.0d);
        double sample =
                firstIntentDouble(
                        intent,
                        new String[] {
                            "decisionSampleReliability",
                            "dynamicGateSampleReliability",
                            "probabilitySampleReliability",
                            "sampleReliability",
                            "sampleTrust",
                            "sample_reliability"
                        },
                        -999.0d);
        double recent =
                firstIntentDouble(
                        intent,
                        new String[] {
                            "decisionRecentValidity",
                            "dynamicGateRecentValidity",
                            "probabilityRecentValidity",
                            "recentValidity",
                            "recentEffectiveness",
                            "recent_validity"
                        },
                        -999.0d);
        String action =
                firstIntentString(
                        intent,
                        new String[] {
                            "decisionActionAdvice",
                            "dynamicGateActionAdvice",
                            "probabilityActionAdvice",
                            "actionAdvice",
                            "action_advice"
                        });
        String evidenceText =
                firstIntentString(
                        intent,
                        new String[] {
                            "decisionEvidenceText",
                            "dynamicGateEvidenceText",
                            "probabilityEvidenceText",
                            "evidenceText",
                            "evidence_text"
                        });
        String counterText =
                firstIntentString(
                        intent,
                        new String[] {
                            "decisionCounterEvidenceText",
                            "dynamicGateCounterEvidenceText",
                            "probabilityCounterEvidenceText",
                            "counterEvidenceText",
                            "counter_evidence_text"
                        });
        if (windowKey.length() == 0
                && evidence <= -998.0d
                && counter <= -998.0d
                && sample <= -998.0d
                && recent <= -998.0d
                && action.length() == 0
                && evidenceText.length() == 0
                && counterText.length() == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        if (windowKey.length() > 0) {
            appendInline(sb, "WindowState=" + limitText(translateLooseText(windowKey), 80));
        }
        if (evidence > -998.0d) {
            appendInline(sb, "证据贡献=" + formatV20Metric(evidence));
        }
        if (counter > -998.0d) {
            appendInline(sb, "反证贡献=" + formatV20Metric(counter));
        }
        if (sample > -998.0d) {
            appendInline(sb, "样本可信=" + formatV20Metric(sample));
        }
        if (recent > -998.0d) {
            appendInline(sb, "近期有效=" + formatV20Metric(recent));
        }
        if (evidenceText.length() > 0) {
            appendInline(sb, "证据=" + limitText(translateLooseText(evidenceText), 90));
        }
        if (counterText.length() > 0) {
            appendInline(sb, "反证=" + limitText(translateLooseText(counterText), 90));
        }
        if (action.length() > 0) {
            appendInline(sb, "建议=" + limitText(translateLooseText(action), 90));
        }
        return limitText(sb.toString(), UI_V20_EVIDENCE_SHORT_LIMIT);
    }

    private String buildWindowStateCompact(
            String s12, String s24, String s48, String s96, String s360) {
        StringBuilder sb = new StringBuilder();
        if (s12 != null && s12.length() > 0) {
            appendInline(sb, "12=" + limitText(translateLooseText(s12), 36));
        }
        if (s24 != null && s24.length() > 0) {
            appendInline(sb, "24=" + limitText(translateLooseText(s24), 36));
        }
        if (s48 != null && s48.length() > 0) {
            appendInline(sb, "48=" + limitText(translateLooseText(s48), 36));
        }
        if (s96 != null && s96.length() > 0) {
            appendInline(sb, "96=" + limitText(translateLooseText(s96), 36));
        }
        if (s360 != null && s360.length() > 0) {
            appendInline(sb, "360=" + limitText(translateLooseText(s360), 36));
        }
        return sb.toString();
    }

    private void appendInline(StringBuilder sb, String text) {
        if (sb == null || text == null || text.length() == 0) {
            return;
        }
        if (sb.length() > 0) {
            sb.append("；");
        }
        sb.append(text);
    }

    private void appendInline(StringBuilder sb, String title, String text, int maxLen) {
        appendInline(sb, title, text, "", maxLen);
    }

    private void appendInline(
            StringBuilder sb, String title, String text, String fallback, int maxLen) {
        if (sb == null) {
            return;
        }
        String v = text == null ? "" : text;
        if (v.length() == 0 && fallback != null) {
            v = fallback;
        }
        if (v.length() == 0) {
            return;
        }
        String t = title == null ? "" : title;
        String value = translateLooseText(limitText(v, maxLen <= 0 ? 80 : maxLen));
        if (t.length() > 0) {
            appendInline(sb, t + "=" + value);
        } else {
            appendInline(sb, value);
        }
    }

    private String firstV20Text(
            Object obj, LightSnapshot snapshot, String[] fields, String[] states) {
        if (fields != null && obj != null) {
            for (int i = 0; i < fields.length; i++) {
                String v = getFieldText(obj, fields[i]);
                if (v != null && v.length() > 0) {
                    return v;
                }
            }
        }
        if (states != null && snapshot != null) {
            for (int i = 0; i < states.length; i++) {
                String v = stateOf(snapshot, states[i]);
                if (v != null && v.length() > 0) {
                    return v;
                }
            }
        }
        return "";
    }

    private double firstV20Double(
            Object obj, LightSnapshot snapshot, String[] fields, String[] states, double fallback) {
        if (fields != null && obj != null) {
            for (int i = 0; i < fields.length; i++) {
                double v = readDouble(obj, fields[i], -999.0d);
                if (v > -998.0d) {
                    return v;
                }
            }
        }
        if (states != null && snapshot != null) {
            for (int i = 0; i < states.length; i++) {
                String t = stateOf(snapshot, states[i]);
                if (t != null && t.length() > 0) {
                    double v = parseDouble(t, -999.0d);
                    if (v > -998.0d) {
                        return v;
                    }
                }
            }
        }
        return fallback;
    }

    private int firstV20Int(
            Object obj, LightSnapshot snapshot, String[] fields, String[] states, int fallback) {
        if (fields != null && obj != null) {
            for (int i = 0; i < fields.length; i++) {
                int v = readInt(obj, fields[i], Integer.MIN_VALUE);
                if (v != Integer.MIN_VALUE) {
                    return v;
                }
            }
        }
        if (states != null && snapshot != null) {
            for (int i = 0; i < states.length; i++) {
                String t = stateOf(snapshot, states[i]);
                if (t != null && t.length() > 0) {
                    try {
                        return Integer.parseInt(t.trim());
                    } catch (Throwable e) {
                    }
                }
            }
        }
        return fallback;
    }

    private String firstIntentString(Intent intent, String[] keys) {
        if (intent == null || keys == null) {
            return "";
        }
        for (int i = 0; i < keys.length; i++) {
            try {
                String v = intent.getStringExtra(keys[i]);
                if (v != null && v.length() > 0) {
                    return v;
                }
            } catch (Throwable e) {
            }
        }
        return "";
    }

    private String firstIntentString(Intent intent, String[] keys, String fallback) {
        String v = firstIntentString(intent, keys);
        if (v != null && v.length() > 0) {
            return v;
        }
        return fallback == null ? "" : fallback;
    }

    private double firstIntentDouble(Intent intent, String[] keys, double fallback) {
        if (intent == null || keys == null) {
            return fallback;
        }
        for (int i = 0; i < keys.length; i++) {
            try {
                if (intent.hasExtra(keys[i])) {
                    double v = intent.getDoubleExtra(keys[i], -999.0d);
                    if (v > -998.0d && !Double.isNaN(v) && !Double.isInfinite(v)) {
                        return v;
                    }
                    String s = intent.getStringExtra(keys[i]);
                    if (s != null && s.length() > 0) {
                        v = parseDouble(s, -999.0d);
                        if (v > -998.0d) {
                            return v;
                        }
                    }
                }
            } catch (Throwable e) {
            }
        }
        return fallback;
    }

    private double firstIntentDouble(
            Intent intent, String[] keys, String[] unused, double fallback) {
        return firstIntentDouble(intent, keys, fallback);
    }

    private String formatV20Metric(double value) {
        if (value <= -998.0d) {
            return "--";
        }
        if (value >= -1.0d && value <= 1.0d) {
            return UiFormatter.signedProbability(value);
        }
        return UiFormatter.score(value);
    }

    private String translateLooseText(String text) {
        if (text == null || text.length() == 0) {
            return "";
        }

        String s = text;

        s = s.replace("RUNNING", "运行中");
        s = s.replace("STOPPED", "已停止");
        s = s.replace("FETCHING", "抓取中");
        s = s.replace("ANALYZING", "分析中");
        s = s.replace("SETTLING", "结算中");
        s = s.replace("LEARNING", "学习中");
        s = s.replace("BACKTESTING", "回测中");
        s = s.replace("BACKTEST_FAST", "快速回测");
        s = s.replace("BACKTEST_NORMAL", "普通回测");
        s = s.replace("BACKTEST_FULL", "完整深测");
        s = s.replace("WAITING", "等待中");
        s = s.replace("CHECKING", "检查中");
        s = s.replace("DELAY", "延迟等待");
        s = s.replace("API_ERROR", "接口异常");
        s = s.replace("DATA_ERROR", "数据异常");

        s = s.replace("FAST", "快线");
        s = s.replace("NORMAL", "普通");
        s = s.replace("FULL", "完整深度");
        s = s.replace("IDLE", "空闲");
        s = s.replace("BUSY", "忙碌");
        s = s.replace("SKIP", "跳过");
        s = s.replace("FINISHED", "已完成");
        s = s.replace("DONE", "已完成");
        s = s.replace("REPLACED", "已被新任务接管");
        s = s.replace("ERROR", "异常");
        s = s.replace("OK", "正常");

        s = s.replace("DynamicCauseChain", "动态因链");
        s = s.replace("DYNAMIC_CAUSE_CHAIN", "动态因链");
        s = s.replace("dynamic_cause_chain", "动态因链");
        s = s.replace("CausalFactorMatrix", "因果因子矩阵");
        s = s.replace("CAUSAL_FACTOR_MATRIX", "因果因子矩阵");
        s = s.replace("causal_factor_matrix", "因果因子矩阵");
        s = s.replace("PositionCauseMatrix", "位置因果矩阵");
        s = s.replace("POSITION_CAUSE_MATRIX", "位置因果矩阵");
        s = s.replace("position_cause_matrix", "位置因果矩阵");
        s = s.replace("TeacherNumberMatrix", "教师号码矩阵");
        s = s.replace("TEACHER_NUMBER_MATRIX", "教师号码矩阵");
        s = s.replace("teacher_number_matrix", "教师号码矩阵");
        s = s.replace("CounterfactualRank", "反事实排名");
        s = s.replace("COUNTERFACTUAL_RANK", "反事实排名");
        s = s.replace("counterfactual_rank", "反事实排名");
        s = s.replace("ThirdGuardMatrix", "第三防线矩阵");
        s = s.replace("THIRD_GUARD_MATRIX", "第三防线矩阵");
        s = s.replace("third_guard_matrix", "第三防线矩阵");
        s = s.replace("RuleHealthMatrix", "规则健康矩阵");
        s = s.replace("RULE_HEALTH_MATRIX", "规则健康矩阵");
        s = s.replace("rule_health_matrix", "规则健康矩阵");
        s = s.replace("ShadowHealthGate", "影子健康门");
        s = s.replace("SHADOW_HEALTH_GATE", "影子健康门");
        s = s.replace("shadow_health_gate", "影子健康门");
        s = s.replace("FalseStrongFreeze", "伪强冻结");
        s = s.replace("FALSE_STRONG_FREEZE", "伪强冻结");
        s = s.replace("false_strong_freeze", "伪强冻结");
        s = s.replace("SeedCauseExpansion", "种子因果扩展");
        s = s.replace("SEED_CAUSE_EXPANSION", "种子因果扩展");
        s = s.replace("seed_cause_expansion", "种子因果扩展");
        s = s.replace("V20_CAUSE_CHAIN", "V20动态因链");
        s = s.replace("v20_cause_chain", "V20动态因链");

        s = s.replace("Teacher Matrix", "教师矩阵");
        s = s.replace("TEACHER_MATRIX", "教师矩阵");
        s = s.replace("teacher_matrix", "教师矩阵");
        s = s.replace("Teacher Replay", "教师回放");
        s = s.replace("TEACHER_SNAPSHOT", "教师快照");
        s = s.replace("teacher_snapshot", "教师快照");
        s = s.replace("Runtime Profile", "实时画像");
        s = s.replace("runtime_profile", "实时画像");
        s = s.replace("STRONG", "强支持");
        s = s.replace("RISK", "风险");
        s = s.replace("WEAK", "偏弱");
        s = s.replace("NONE", "无");
        s = s.replace("READY", "就绪");
        s = s.replace("ELITE", "精英");
        s = s.replace("HARD", "硬风险");
        s = s.replace("FREEZE", "冻结");

        s = s.replace("FinalP", "最终概率");
        s = s.replace("RawP", "原始概率");
        s = s.replace("CalP", "校准概率");
        s = s.replace("Gate", "出手门槛");
        s = s.replace("FinalGate", "最终门槛");
        s = s.replace("Edge", "概率优势");
        s = s.replace("Block", "拦截原因");

        s = s.replace("TrendLife", "趋势寿命");
        s = s.replace("DriftRisk", "漂移风险");
        s = s.replace("BreakDepth", "破位深度");
        s = s.replace("RecoveryPower", "恢复力量");
        s = s.replace("Recovery", "恢复分");
        s = s.replace("BreakRisk", "断点风险");
        s = s.replace("FalseRecovery", "假恢复风险");

        s = s.replace("Similar", "相似形态");
        s = s.replace("Sample", "样本");
        s = s.replace("ComboHR", "组合命中率");
        s = s.replace("ChampionGap", "主优势差");
        s = s.replace("ComboGap", "组合优势差");

        s = s.replace("SHADOW", "影子观察");
        s = s.replace("HOLD", "不出手");
        s = s.replace("BET", "出手");
        s = s.replace("WIN", "命中");
        s = s.replace("MISS", "未中");
        s = s.replace("WAIT", "等待");
        s = s.replace("VOID", "作废");

        s = s.replace("CHAOS", "无有效形态");
        s = s.replace("FORMING", "形态形成中");
        s = s.replace("CONFIRMED", "形态已确认");
        s = s.replace("ACCELERATING", "趋势加速");
        s = s.replace("PULLBACK", "趋势回拉");
        s = s.replace("RECOVERING", "恢复中");
        s = s.replace("WEAKENING", "趋势转弱");
        s = s.replace("BROKEN", "形态破位");
        s = s.replace("REVERSING", "反转风险");
        s = s.replace("REVERSAL", "反转风险");
        s = s.replace("STABLE", "稳定形态");
        s = s.replace("TRANSITION", "过渡形态");

        s = s.replace("HISTORY_WEAK", "历史数据不足");
        s = s.replace("LOW_PROBABILITY", "概率不足");
        s = s.replace("SAMPLE_WEAK", "样本不足");
        s = s.replace("TREND_WEAK", "趋势偏弱");
        s = s.replace("DRIFT_HIGH", "漂移过高");
        s = s.replace("BREAK_HIGH", "破位过深");
        s = s.replace("RECOVERY_WEAK", "恢复不足");
        s = s.replace("POSITION_WEAK", "位置优势不足");
        s = s.replace("COMBO_WEAK", "组合优势不足");
        s = s.replace("FREQ_HIGH", "频率过高");
        s = s.replace("DATA_OFFSET", "数据错位");
        s = s.replace("OPEN_TRADE_EXISTS", "已有未结算交易");

        s = s.replace("MARKOV_FAILED", "Markov失效");
        s = s.replace("TREND_BROKEN", "趋势破坏");
        s = s.replace("DRIFT_SPIKE", "漂移突增");
        s = s.replace("HOT_REVERSE", "热号反杀");
        s = s.replace("FALSE_RECOVERY", "假恢复");
        s = s.replace("THIRD_WEAK", "第三号偏弱");
        s = s.replace("K3_RANK_4_6", "落在第4-6名");
        s = s.replace("POSITION_LOST", "位置优势丢失");
        s = s.replace("UNKNOWN", "未知原因");

        s = s.replace("PASS", "通过");
        s = s.replace("BLOCK", "拦截");
        s = s.replace("WARN", "警告");
        s = s.replace("INFO", "提示");

        s = s.replace("REAL_TRADE", "真实交易");
        s = s.replace("STRICT_SINGLE_POSITION_K3", "严格单位置K3");
        s = s.replace("STRICT_SINGLE_POSITION_K3_FAST_CATCHUP", "严格单位置K3+历史补扫");

        s = s.replace("A_PLUS", "A+形态");
        s = s.replace("PROMOTION", "晋级门");
        s = s.replace("AUDIT", "审计");
        s = s.replace("SNAPSHOT", "快照");
        s = s.replace("NO_FUTURE_LEAK", "无未来泄露");
        s = s.replace("FUTURE_LEAK_BLOCK", "未来数据阻断");
        s = s.replace("WindowState", "窗口状态");
        s = s.replace("window_state_key", "窗口状态键");
        s = s.replace("state_key", "状态键");
        s = s.replace("evidenceContribution", "证据贡献");
        s = s.replace("counterContribution", "反证贡献");
        s = s.replace("sampleReliability", "样本可信");
        s = s.replace("sampleTrust", "样本可信");
        s = s.replace("recentValidity", "近期有效");
        s = s.replace("recentEffectiveness", "近期有效");
        s = s.replace("featureDistance", "特征距离");
        s = s.replace("similarWindow", "相似窗口");
        s = s.replace("actionAdvice", "动作建议");
        s = s.replace("ALLOW_TO_GATE", "允许进入最终门");
        s = s.replace("WATCH_TO_GATE", "观察进入最终门");
        s = s.replace("SHADOW_HOLD", "影子或持有");
        s = s.replace("HOLD", "不出手");

        s = s.replace("dynamicCluster", "动态窗口簇");
        s = s.replace("DynamicCluster", "动态窗口簇");
        s = s.replace("DYNAMIC_CLUSTER", "动态窗口簇");
        s = s.replace("NeedSupply", "需求供给");
        s = s.replace("NEED_SUPPLY", "需求供给");
        s = s.replace("Counterfactual", "反事实");
        s = s.replace("COUNTERFACTUAL", "反事实");
        s = s.replace("AdaptiveJudge", "智能审判");
        s = s.replace("ADAPTIVE", "智能审判");
        s = s.replace("Promotion", "晋级验证");
        s = s.replace("PROMOTION", "晋级验证");
        s = s.replace("whyNotRisk", "反证风险");
        s = s.replace("thirdQuality", "第三号质量");
        s = s.replace("BET_DOMAIN_FREQ_RECOVERY", "频率恢复BET");
        s = s.replace("BET_DOMAIN_REAL_BET_PROBE", "真实探针BET");
        s = s.replace("BET_DOMAIN_NORMAL", "正常BET");
        s = s.replace("QUALIFIED_FREQUENCY_FLOOR", "合格频率下限");
        s = s.replace("FREQUENCY_FLOOR_BET", "频率下限真实出手");
        s = s.replace("HARD_FLOOR_DEBT", "低于15%硬下限");
        s = s.replace("TARGET_RECOVERY", "向20%目标恢复");
        s = s.replace("MANDATORY_15_PERCENT_FREQUENCY_FLOOR", "低于15%时从合法候选中选择综合最优真实出手");
        s = s.replace("WATCH_STALE", "观察过期");
        s = s.replace("falseCause", "假因");
        return s;
    }

    private static final class LightSnapshot {
        Models.AppRuntimeState state;
        Models.Decision decision;
        Models.Trade currentTrade;
        Models.RuntimeStats stats;
        Models.BacktestReport backtest;
        Models.ManualBacktestTask manualTask;
        Models.Draw latestDraw;
        RuntimeTruthSnapshotR906 truthR906;

        HashMap<String, String> states;

        int drawCount;
        int drawRemainTo240;
        long loadedAt;
        long loadedElapsedAtR971;
        long uiRevision;

        String currentIssue;
        String targetIssue;
        String latestDrawNumbersText;
        String drawDataProgressText;
        String backfillStatusText;
        String whyTruthText;
        String error;

        LightSnapshot() {
            state = null;
            decision = null;
            currentTrade = null;
            stats = null;
            backtest = null;
            manualTask = null;
            latestDraw = null;
            truthR906 = null;
            states = new HashMap<String, String>();
            drawCount = 0;
            drawRemainTo240 = 0;
            loadedAt = 0L;
            uiRevision = 0L;
            currentIssue = "";
            targetIssue = "";
            latestDrawNumbersText = "";
            drawDataProgressText = "";
            backfillStatusText = "";
            whyTruthText = "";
            error = "";
        }
    }

    private static final class HeavySnapshot {
        LightSnapshot light;
        ArrayList<Models.ReasonItem> reasons;
        ArrayList<Models.Draw> draws;
        ArrayList<Models.Decision> decisions;
        HashMap<String, Models.Trade> tradeMap;
        long loadedAt;
        String error;
        ArrayList<Models.ReasonItem> diagnosisReasons;
        String diagnosisSummary;
        String whyTruthText;
        String diagnosisDetail;
        String diagnosisCopyText;
        String fullDiagnosisSummary;
        String fullDiagnosisDetail;
        String fullDiagnosisCopyText;
        String moduleEffectText;
        String shadowPromotionText;
        String freezeMatrixText;
        String adaptiveJudgeText;
        String fixPlanText;

        HeavySnapshot() {
            light = null;
            reasons = new ArrayList<Models.ReasonItem>();
            draws = new ArrayList<Models.Draw>();
            decisions = new ArrayList<Models.Decision>();
            tradeMap = new HashMap<String, Models.Trade>();
            loadedAt = 0L;
            error = "";
            diagnosisReasons = new ArrayList<Models.ReasonItem>();
            diagnosisSummary = "";
            whyTruthText = "";
            diagnosisDetail = "";
            diagnosisCopyText = "";
            fullDiagnosisSummary = "";
            fullDiagnosisDetail = "";
            fullDiagnosisCopyText = "";
            moduleEffectText = "";
            shadowPromotionText = "";
            freezeMatrixText = "";
            adaptiveJudgeText = "";
            fixPlanText = "";
        }
    }

    private String buildBusinessHealthLine(LightSnapshot snapshot) {
        long now = System.currentTimeMillis();
        long serviceAt = parseLong(stateOf(snapshot, "last_service_started_at"), 0L);
        long taskAt = parseLong(stateOf(snapshot, "last_task_removed_at"), 0L);
        long fetchAt = parseLong(stateOf(snapshot, "last_fetch_success_at"), 0L);
        long analysisAt = parseLong(stateOf(snapshot, "last_analysis_success_at"), 0L);
        long settleAt = parseLong(stateOf(snapshot, "last_settlement_success_at"), 0L);
        String health = stateOf(snapshot, "background_business_health");
        String teacherLag = stateOf(snapshot, "teacher_snapshot_issue_lag");
        String teacherFresh = stateOf(snapshot, "teacher_snapshot_fresh_by_issue");
        if (serviceAt <= 0L && fetchAt <= 0L && analysisAt <= 0L && health.length() == 0) return "";
        long fetchAge = fetchAt <= 0L ? Long.MAX_VALUE : Math.max(0L, now - fetchAt);
        long analysisAge = analysisAt <= 0L ? Long.MAX_VALUE : Math.max(0L, now - analysisAt);
        String live =
                (fetchAge <= 150000L && analysisAge <= 180000L) ? "ALIVE" : "FAKE_ALIVE_OR_STALE";
        StringBuilder sb = new StringBuilder("后台业务活性：");
        sb.append(live).append("｜阶段=").append(health.length() == 0 ? "--" : health);
        sb.append("｜Fetch=").append(ageText(fetchAge));
        sb.append("｜Analysis=").append(ageText(analysisAge));
        if (settleAt > 0L) sb.append("｜Settlement=").append(ageText(Math.max(0L, now - settleAt)));
        if (taskAt > 0L) sb.append("｜退出回调=").append(ageText(Math.max(0L, now - taskAt)));
        if (teacherLag.length() > 0) sb.append("｜Teacher落后=").append(teacherLag).append("期");
        if (teacherFresh.length() > 0) sb.append("｜TeacherFreshByIssue=").append(teacherFresh);
        sb.append("｜Build=").append(AppConfig.ACTIVE_QUALITY_BUILD_VERSION);
        try {
            sb.append("｜进程外=").append(ServiceHeartbeatStore.peekLastDetailedSummary());
        } catch (Throwable ignore) {
        }
        return sb.toString();
    }

    private String ageText(long age) {
        if (age == Long.MAX_VALUE) return "无成功记录";
        if (age < 1000L) return age + "ms";
        if (age < 60000L) return (age / 1000L) + "秒前";
        return (age / 60000L) + "分钟前";
    }

    private String buildAlwaysOnPermissionStatusV17() {
        boolean batteryIgnored = android.os.Build.VERSION.SDK_INT < 23;
        boolean exact = true;
        try {
            android.os.PowerManager pm = (android.os.PowerManager) getSystemService(POWER_SERVICE);
            if(android.os.Build.VERSION.SDK_INT >= 23)batteryIgnored = pm != null && pm.isIgnoringBatteryOptimizations(getPackageName());
        } catch (Throwable e) {
        }
        try {
            if (android.os.Build.VERSION.SDK_INT >= 31) {
                android.app.AlarmManager am =
                        (android.app.AlarmManager) getSystemService(ALARM_SERVICE);
                if (am != null) {
                    java.lang.reflect.Method m =
                            android.app.AlarmManager.class.getMethod("canScheduleExactAlarms");
                    Object v = m.invoke(am);
                    exact = v instanceof Boolean && ((Boolean) v).booleanValue();
                }
            }
        } catch (Throwable e) {
            exact = false;
        }
        ServiceHeartbeatStore hb = new ServiceHeartbeatStore(getApplicationContext());
        return "后台常驻="
                + (hb.isUserEnabled() ? "已启用" : "已停止")
                + "｜电池豁免="
                + (batteryIgnored ? "是" : "否")
                + "｜精确闹钟="
                + (exact ? "可用" : "不可用")
                + "｜TeacherPumpAge="
                + ageTextSafeV17(hb.getTeacherPumpAt())
                + "｜ResearchProgressAge="
                + ageTextSafeV17(hb.getLearningPumpAt())
                + "｜ResearchAttempt="
                + hb.getLearningAttemptState()
                + "@"
                + ageTextSafeV17(hb.getLearningAttemptAt())
                + "｜ResearchTruth="
                + hb.researchProgressTruth().state
                + "/"
                + hb.researchProgressTruth().reason
                + "｜ResearchCompletion="
                + store.getState("r956_research_owner_completion", "WAITING")
                + "｜业务异常="
                + hb.getBusinessStaleReason();
    }

    private String ageTextSafeV17(long at) {
        if (at <= 0L) return "--";
        long a = Math.max(0L, System.currentTimeMillis() - at);
        return (a / 1000L) + "s";
    }

    private TextView txtR925Background, txtR925Shadow, txtR925Pipeline;

    private void b15InstallCompleteDiagnosticPanel() {
        if (destroyed || r940DiagnosticPanelInstalled) return;
        try {
            final LinearLayout panel = new LinearLayout(this);
            panel.setOrientation(LinearLayout.VERTICAL);
            panel.setPadding(16, 16, 16, 16);

            final TextView title = new TextView(this);
            title.setText("运行与研究总览｜点击各框查看完整明细");
            title.setTextSize(16.0f);
            title.setPadding(8, 8, 8, 12);
            panel.addView(title);
            txtR925Background = r919CreateTruthCard("后台能力与业务状态｜读取中");
            txtR925Shadow = r919CreateTruthCard("独立影子统计｜读取中");
            txtR925Pipeline = r919CreateTruthCard("最近20期流水｜读取中");
            txtR925Background.setMaxLines(3);
            txtR925Background.setTextSize(12f);
            panel.addView(txtR925Background);
            panel.addView(txtR925Shadow);
            panel.addView(txtR925Pipeline);
            txtR925Background.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) { openBackgroundReliabilitySettings(); }
            });
            String[] labels = {"实时流水（500期）", "抓取记录（100条）", "影子统计", "研究证书漏斗", "淘汰原因明细", "导出500期流水 CSV", "导出完整1200动作训练数据"};
            for (int i=0; i<labels.length; i++) {
                final int pageKind=6+i;
                Button button=new Button(this); button.setText(labels[i]); panel.addView(button);
                button.setOnClickListener(new View.OnClickListener(){
                    public void onClick(View v){if(pageKind==12) TrainingSnapshotExportR925.start(MainActivity.this,readStore!=null?readStore:store); else requestR919TruthDetail(pageKind);}
                });
            }

            txtResearchSnapshotR76 = r919CreateTruthCard("研究快照｜读取中……");
            txtResearchSnapshotR76.setMaxLines(4);
            panel.addView(txtResearchSnapshotR76);
            txtR919PipelineTruth = r919CreateTruthCard("① 逐期训练与数值提交｜读取中……");
            txtR919ShadowTruth = r919CreateTruthCard("② 迭代・变异・融合・创造｜读取中……");
            txtR919ResearchTruth = r919CreateTruthCard("③ 原子・因子・矩阵｜读取中……");
            // Preserve the complete bounded atom/matrix section; the worker limits the text
            // and the tap target opens this same immutable observation with its scope/time.
            txtR919ResearchTruth.setMaxLines(Integer.MAX_VALUE);
            txtR919ResearchTruth.setEllipsize(null);
            txtR919ResearchTruth.setTextSize(12.5f);
            txtR919RealGateTruth = r919CreateTruthCard("④ 主链应用与阻塞原因｜读取中……");
            panel.addView(txtR919PipelineTruth);
            panel.addView(txtR919ShadowTruth);
            panel.addView(txtR919ResearchTruth);
            panel.addView(txtR919RealGateTruth);

            String[] capabilityNames={"打开深度学习与训练明细","打开动态迭代与证书","打开创造能力明细","打开原子与分子质量"};
            final String[] capabilityPages={"deep","evolution","creation","atoms"};
            for(int page=0;page<capabilityNames.length;page++){
                final int selectedPage=page;Button open=new Button(this);open.setText(capabilityNames[page]);panel.addView(open);
                open.setOnClickListener(new View.OnClickListener(){public void onClick(View view){
                    try{startActivity(com.ethk3.novaonepro.r952.FullbloodPageProviderR952.intent(MainActivity.this,capabilityPages[selectedPage],"",""));}
                    catch(RuntimeException failure){android.widget.Toast.makeText(MainActivity.this,"页面打开失败："+failure.getClass().getSimpleName(),android.widget.Toast.LENGTH_LONG).show();}
                }});
            }

            final TextView summary = new TextView(this);
            summary.setPadding(8, 12, 8, 8);
            summary.setText(
                    "完整报告、决策摘要、深度学习页与四个状态框使用相同数据库口径；刷新时间不同，数值可能随运行变化。"
                            + "读取失败显示 READ_ERROR，不把零样本伪装成PASS。\n"
                            + "REAL与SHADOW严格分账；模型仍未证明，仅精确受控候选可由唯一DecisionGate写入真实账本，raw不冒充校准胜率。");
            panel.addView(summary);

            final Button copy = new Button(this);
            copy.setText("导出完整报告（含数据库和训练附件）");
            panel.addView(copy);
            final Button forensic = new Button(this);
            forensic.setText("查看运行、结算与真实出手详情");
            panel.addView(forensic);

            // R919: insert into the single root ScrollView instead of addContentView().
            // This makes the entire panel participate in the page's vertical scrolling.
            View content = findViewById(android.R.id.content);
            android.view.ViewGroup contentGroup =
                    content instanceof android.view.ViewGroup
                            ? (android.view.ViewGroup) content
                            : null;
            View rootView =
                    contentGroup != null && contentGroup.getChildCount() > 0
                            ? contentGroup.getChildAt(0)
                            : null;
            android.view.ViewGroup page = null;
            if (rootView instanceof ScrollView) {
                ScrollView scroll = (ScrollView) rootView;
                if (scroll.getChildCount() > 0
                        && scroll.getChildAt(0) instanceof android.view.ViewGroup) {
                    page = (android.view.ViewGroup) scroll.getChildAt(0);
                }
            }
            if (page != null) {
                int index = Math.max(0, page.getChildCount() - 1);
                panel.setLayoutParams(
                        new android.view.ViewGroup.LayoutParams(
                                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                                android.view.ViewGroup.LayoutParams.WRAP_CONTENT));
                // R940: every card keeps one parent inside the existing ScrollView.  The previous
                // code added txtR925Background to panel and then tried to attach it to fixedRoot,
                // throwing "child already has a parent" after detaching the activity root.
                page.addView(panel, index);
            } else {
                addContentView(
                        panel,
                        new android.view.ViewGroup.LayoutParams(
                                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                                android.view.ViewGroup.LayoutParams.WRAP_CONTENT));
            }

            r919BindTruthOpen(txtR919PipelineTruth, 1);
            r919BindTruthOpen(txtR919ShadowTruth, 2);
            r919BindTruthOpen(txtR919ResearchTruth, 3);
            r919BindTruthOpen(txtR919RealGateTruth, 4);

            copy.setOnClickListener(
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            CompleteReportFileR927.openExportMenu(MainActivity.this, copy, summary);
                        }
                    });
            forensic.setOnClickListener(
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            requestR919TruthDetail(5);
                        }
                    });
            r940DiagnosticPanelInstalled = true;
            requestR919TruthRefresh(true);
        } catch (Throwable error) {
            r940DiagnosticPanelInstalled = false;
            CrashRecorder.recordCaught(
                    getApplicationContext(), error, "R940_DIAGNOSTIC_PANEL_INSTALL");
            android.util.Log.e("R925_UI","Diagnostic panel installation failed",error);
            Toast.makeText(this,"运行面板加载失败："+error.getClass().getSimpleName(),Toast.LENGTH_LONG).show();
        }
    }

    private TextView r919CreateTruthCard(String initial) {
        TextView tv = new TextView(this);
        tv.setText(initial);
        tv.setTextSize(13.0f);
        tv.setSingleLine(false);
        tv.setMaxLines(12);
        tv.setEllipsize(android.text.TextUtils.TruncateAt.END);
        tv.setPadding(14, 12, 14, 12);
        try {
            tv.setBackgroundResource(R.drawable.bg_console);
        } catch (Throwable ignored) {
        }
        LinearLayout.LayoutParams lp =
                new LinearLayout.LayoutParams(
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                        android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 6, 0, 6);
        tv.setLayoutParams(lp);
        return tv;
    }

    private void r919BindTruthOpen(final TextView tv, final int kind) {
        if (tv == null) return;
        tv.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ResearchPipelineStatusR76.Snapshot s = cachedResearchStatusR76;
                        if (s == null) {
                            Toast.makeText(
                                            MainActivity.this,
                                            "运行真值正在读取，请稍后再点",
                                            Toast.LENGTH_SHORT)
                                    .show();
                            requestR919TruthRefresh(true);
                            return;
                        }
                        String[] titles={"逐期训练与数值提交","迭代・变异・融合・创造","原子・因子・矩阵","主链应用与阻塞原因"};
                        String[] detail={s.training,s.evolution,s.atomsMatrices,s.mainline};
                        if(kind>=1&&kind<=detail.length)DiagnosisActivity.openText(MainActivity.this,
                                titles[kind-1],s.summary+"\n\n"+detail[kind-1]);
                    }
                });
    }

    private void requestR919TruthRefresh(boolean force) {
        if (destroyed || !r941UiResumed || !r940DiagnosticPanelInstalled
                || uiDetailReadHandlerR71 == null || r919TruthReadRunning || isUserScrollingNow()) return;
        long now = System.currentTimeMillis();
        if (!force && now - lastR919TruthRefreshAt < UI_RUNTIME_TRUTH_REFRESH_MS) return;
        final String requestedTargetR76=safeText(getBestKnownTargetIssue());
        r919TruthReadRunning = true; lastR919TruthRefreshAt = now;
        boolean posted = uiDetailReadHandlerR71.post(new Runnable() {
            @Override public void run() {
                try {
                    if (destroyed || !r941UiResumed) { r919TruthReadRunning = false; return; }
                    DbStore truthStore = readStore != null ? readStore : store;
                    final RuntimeTruthSnapshotR906 snapshot = truthStore == null
                            ? RuntimeTruthSnapshotR906.pending(requestedTargetR76) : loadTruthCachedR907(truthStore,requestedTargetR76);
                    final ResearchPipelineStatusR76.Snapshot research=ResearchPipelineStatusR76.read(truthStore);
                    final String[] cards={research.training,research.evolution,research.atomsMatrices,research.mainline};
                    final String[] operational = OperationalDashboardR925.capture(getApplicationContext(), truthStore);
                    // Full details live in their file-backed pages; never lay out unbounded dashboard text.
                    for (int i = 0; i < cards.length; i++) cards[i] = limitText(cards[i], 1000);
                    for (int i = 0; i < operational.length; i++) operational[i] = limitText(operational[i], 1200);
                    Handler handler = uiHandler;
                    if (handler == null || !handler.post(new Runnable() {
                        private int cardIndex;
                        @Override public void run() {
                            if (destroyed || !r941UiResumed || !r940DiagnosticPanelInstalled) { r919TruthReadRunning = false; return; }
                            if(!requestedTargetR76.equals(safeText(getBestKnownTargetIssue()))) {
                                r919TruthReadRunning=false;requestR919TruthRefresh(true);return;
                            }
                            // Reading may finish during a gesture. Keep one bounded pending snapshot.
                            if (isUserScrollingNow()) {
                                if (uiHandler == null || !uiHandler.postDelayed(this, 450L)) r919TruthReadRunning = false;
                                return;
                            }
                            long bind = frameMetricsR906.beginBind();
                            try {
                                lastR919TruthSnapshot = snapshot;
                                cachedResearchStatusR76 = research;
                                if (cardIndex == 0) {
                                txtR925Background.setTag(operational[0]);
                                String[] lines = operational[0].split("\\n", 4);
                                StringBuilder summary = new StringBuilder();
                                for (int i = 0; i < Math.min(3, lines.length); i++) { if (i > 0) summary.append('\n'); summary.append(lines[i]); }
                                setText(txtR925Background, summary.toString());
                                txtR925Background.setBackgroundColor(operational[0].contains("CAPABILITIES_READY") ? 0xFFDDEEDF
                                        : operational[0].contains("SETUP_REQUIRED") || operational[0].contains("读取失败") ? 0xFFFFDDDD : 0xFFFFEFC8);
                                } else if (cardIndex == 1) setText(txtR925Shadow, operational[1]);
                                else if (cardIndex == 2) setText(txtR925Pipeline, operational[2]);
                                else if (cardIndex == 3) setText(txtResearchSnapshotR76,limitText(research.summary,800));
                                else if (cardIndex == 4) setText(txtR919PipelineTruth, cards[0]);
                                else if (cardIndex == 5) setText(txtR919ShadowTruth, cards[1]);
                                else if (cardIndex == 6) setText(txtR919ResearchTruth, cards[2]);
                                else setText(txtR919RealGateTruth, cards[3]);
                            } catch (Throwable failure) {
                                cardIndex = 7;
                                CrashRecorder.recordCaught(getApplicationContext(), failure, "R941_DASHBOARD_BIND");
                            } finally { frameMetricsR906.endBind(bind); }
                            // Let Android lay out one bounded card before binding the next.
                            // Keep the single-flight flag until the whole snapshot is consumed.
                            if (++cardIndex >= 8 || uiHandler == null
                                    || !uiHandler.postDelayed(this, 32L)) r919TruthReadRunning = false;
                        }
                    })) r919TruthReadRunning = false;
                } catch (Throwable failure) {
                    r919TruthReadRunning = false;
                    CrashRecorder.recordCaught(getApplicationContext(), failure, "R941_DASHBOARD_READ");
                }
            }
        });
        if (!posted) r919TruthReadRunning = false;
    }

    private void requestR919TruthDetail(final int kind) {
        if (destroyed || uiDetailReadHandlerR71 == null || r919TruthReadRunning) return;
        r919TruthReadRunning = true;
        Toast.makeText(this, "正在读取完整真值…", Toast.LENGTH_SHORT).show();
        uiDetailReadHandlerR71.post(
                new Runnable() {
                    @Override
                    public void run() {
                        MainActivityTruthPresenterR926.RenderedDetail detail;
                        try {
                            DbStore truthStore = readStore != null ? readStore : store;
                            detail = MainActivityTruthPresenterR926.render(truthStore, kind);
                        } catch (Throwable e) {
                            detail =
                                    new MainActivityTruthPresenterR926.RenderedDetail(
                                            "R926运行真值",
                                            "READ_ERROR|" + e.getClass().getSimpleName(),
                                            false);
                        }
                        final MainActivityTruthPresenterR926.RenderedDetail rendered = detail;
                        if (uiHandler != null) {
                            uiHandler.post(
                                    new Runnable() {
                                        @Override
                                        public void run() {
                                            r919TruthReadRunning = false;
                                            if (destroyed) return;
                                            if (rendered.csv) {
                                                DiagnosticTextExportR923.shareCsv(
                                                        MainActivity.this,
                                                        rendered.title,
                                                        rendered.body);
                                            } else {
                                                DiagnosisActivity.openText(
                                                        MainActivity.this,
                                                        rendered.title,
                                                        rendered.body);
                                            }
                                        }
                                    });
                        } else {
                            r919TruthReadRunning = false;
                        }
                    }
                });
    }

    private DbStore b15ResolveStore() {
        try {
            java.lang.reflect.Field[] f = getClass().getDeclaredFields();
            for (int i = 0; i < f.length; i++) {
                if (DbStore.class.isAssignableFrom(f[i].getType())) {
                    f[i].setAccessible(true);
                    Object x = f[i].get(this);
                    if (x instanceof DbStore) return (DbStore) x;
                }
            }
        } catch (Throwable ignore) {
        }
        try {
            java.lang.reflect.Constructor<?> c =
                    DbStore.class.getDeclaredConstructor(android.content.Context.class);
            c.setAccessible(true);
            return (DbStore) c.newInstance(this);
        } catch (Throwable e) {
            throw new IllegalStateException("B15_DBSTORE_RESOLVE_FAILED", e);
        }
    }
    @Override public boolean onCreateOptionsMenu(android.view.Menu menu) {
        menu.add(0,0x535531,0,R.string.su1_research_title);
        return super.onCreateOptionsMenu(menu);
    }
    @Override public boolean onOptionsItemSelected(android.view.MenuItem item) {
        if(item.getItemId()==0x535531){startActivity(new Intent(this,com.ethk3.novaonepro.strategyv2.StrategyResearchActivitySU1.class));return true;}
        return super.onOptionsItemSelected(item);
    }
}
