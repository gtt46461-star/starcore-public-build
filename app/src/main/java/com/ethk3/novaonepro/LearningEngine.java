package com.ethk3.novaonepro;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

/**
 * LearningEngine
 *
 * <p>V20 严格学习层满血版：
 *
 * <p>1. 真实校准/概率权重只允许 REAL_TRADE + BET + 目标期一致 + 单位置K3合法 + WIN/MISS + 实际开奖可验证进入。 2. SHADOW / HOLD /
 * 后台自动反推 / 教师矩阵观察样本，绝不写真实校准桶。 3. 保留结算后即时学习、TeacherMatrix/TeacherNumberMatrix学习、影子候选反推、状态画像反哺。 4.
 * learnCandidateCalibration 恢复为观察审计/教师审计/画像审计，不污染真实校准。 5. 学习层不直接救援BET，不直接保存Decision，不直接创建Trade。 6.
 * TeacherMatrix/TeacherNumberMatrix 只反哺候选画像、风险、解释，最终BET仍由 DecisionGateEngine 裁决。 7. 命中判断只看预测
 * position 的实际号码是否等于 n1/n2/n3，不依赖旧mask避免错位污染。 8. Java7 / AIDE 兼容，无 lambda、无 stream、无第三方库。
 */
public final class LearningEngine {
    public static final String MODULE_BUILD_VERSION = AppConfig.ACTIVE_QUALITY_BUILD_VERSION;
    public static final String ALWAYS_ON_LEARNING_VERSION = AppConfig.ALWAYS_ON_RUNTIME_FIX_VERSION;
    public static final String OUMAN_GALAXY_REBUILD_VERSION =
            AppConfig.OUMAN_GALAXY_REBUILD_VERSION;

    private static final int MAX_REASON_TEXT = 360;
    private static final int SHADOW_REASON_SAMPLE_LIMIT = 12;
    private static final int NEAR_NUMBER_DISTANCE = 1;

    private static final int AUTO_LEARN_BATCH_MAX = 24;
    private static final int AUTO_LEARN_MIN_TRAIN = 240;
    private static final int AUTO_LEARN_HISTORY_LIMIT = 2200;
    private static final int AUTO_LEARN_POSITION_COUNT = 10;
    private static final int TEACHER_MATRIX_NUMBER_COUNT = 10;
    private static final int TEACHER_MATRIX_SAMPLE_PER_DRAW = 100;

    private static final int AUTO_LEARN_SCORE_WINDOW_SHORT = 32;
    private static final int AUTO_LEARN_SCORE_WINDOW_MID = 80;
    private static final int AUTO_LEARN_SCORE_WINDOW_LONG = 240;
    private static final int AUTO_LEARN_SCORE_WINDOW_DEEP = 500;

    private static final long REALTIME_LEARN_MIN_INTERVAL_MS = 500L;
    private static final long SMALL_BATCH_MIN_INTERVAL_MS = 1500L;
    private static final long SMALL_BATCH_COOLDOWN_MS = 60000L;

    private static final String AUTO_TASK_TYPE = "AUTO_SMALL_BATCH";
    private static final String AUTO_TASK_KEY = "AUTO_SMALL_BATCH_W240_500";
    private static final String TEACHER_TASK_REALTIME = "TEACHER_MATRIX_REALTIME";
    private static final String TEACHER_TASK_AUTO_BATCH = "TEACHER_MATRIX_AUTO_BATCH";
    private static final String TEACHER_TASK_MANUAL_SEGMENT = "TEACHER_MATRIX_MANUAL_SEGMENT";

    private static final String AUTO_STATUS_IDLE = "IDLE";
    private static final String AUTO_STATUS_RUNNING = "RUNNING";
    private static final String AUTO_STATUS_DONE = "DONE";
    private static final String AUTO_STATUS_SKIP = "SKIP";

    private static final Object NO_METHOD = new Object();

    private final ShapeMemoryEngine shapeMemoryEngine;
    private final CalibrationEngine calibrationEngine;
    private final ProbabilityEngine probabilityEngine;
    private final BreakRecoveryEngine breakRecoveryEngine;
    private final DataGuard dataGuard;
    private final Object teacherMatrixEngine;

    private long lastRealtimeLearnAt;
    private long lastSmallBatchAt;

    public LearningEngine() {
        shapeMemoryEngine = new ShapeMemoryEngine();
        calibrationEngine = new CalibrationEngine();
        probabilityEngine = new ProbabilityEngine();
        breakRecoveryEngine = new BreakRecoveryEngine();
        dataGuard = new DataGuard();
        teacherMatrixEngine = createTeacherMatrixEngineSafely();
        lastRealtimeLearnAt = 0L;
        lastSmallBatchAt = 0L;
    }

    /* =========================
     * 实时候选反哺入口
     * ========================= */

    public int applyTeacherMatrixToCandidates(
            DbStore store, List<Models.ComboCandidate> candidates) {
        if (store == null || candidates == null || candidates.size() == 0) {
            return 0;
        }

        if (teacherMatrixEngine == null) {
            return 0;
        }

        Object r =
                invokeTeacherMatrix(
                        "applyTeacherMatrixToCandidates",
                        new Class[] {DbStore.class, List.class},
                        new Object[] {store, candidates});

        int n = objectToInt(r);

        if (n > 0) {
            return n;
        }

        r =
                invokeTeacherMatrix(
                        "applyRealtimeBoost",
                        new Class[] {DbStore.class, List.class},
                        new Object[] {store, candidates});

        n = objectToInt(r);

        if (n > 0) {
            return n;
        }

        r =
                invokeTeacherMatrix(
                        "applyToCandidates",
                        new Class[] {DbStore.class, List.class},
                        new Object[] {store, candidates});

        n = objectToInt(r);

        if (n > 0) {
            return n;
        }

        r =
                invokeTeacherMatrix(
                        "applyTeacherNumberMatrixToCandidates",
                        new Class[] {DbStore.class, List.class},
                        new Object[] {store, candidates});

        return objectToInt(r);
    }

    public Models.ComboCandidate applyTeacherMatrixToCandidate(
            DbStore store, Models.ComboCandidate candidate) {
        if (store == null || candidate == null || teacherMatrixEngine == null) {
            return candidate;
        }

        Object r =
                invokeTeacherMatrix(
                        "applyTeacherMatrixToCandidate",
                        new Class[] {DbStore.class, Models.ComboCandidate.class},
                        new Object[] {store, candidate});

        if (r instanceof Models.ComboCandidate) {
            return (Models.ComboCandidate) r;
        }

        r =
                invokeTeacherMatrix(
                        "applyToCandidate",
                        new Class[] {DbStore.class, Models.ComboCandidate.class},
                        new Object[] {store, candidate});

        if (r instanceof Models.ComboCandidate) {
            return (Models.ComboCandidate) r;
        }

        r =
                invokeTeacherMatrix(
                        "applyTeacherNumberMatrixToCandidate",
                        new Class[] {DbStore.class, Models.ComboCandidate.class},
                        new Object[] {store, candidate});

        if (r instanceof Models.ComboCandidate) {
            return (Models.ComboCandidate) r;
        }

        return candidate;
    }

    /* =========================
     * 开奖后学习入口
     * ========================= */

    public LearningReport learnAfterDraw(
            DbStore store,
            Models.Draw actualDraw,
            Models.Decision latestDecision,
            List<Models.ComboCandidate> shadowCandidates) {
        LearningReport report = new LearningReport();

        if (store == null) {
            report.ok = 0;
            report.detail = "学习失败：DbStore为空";
            return report;
        }

        if (actualDraw == null || !actualDraw.isValid()) {
            report.ok = 0;
            report.detail = "学习失败：实际开奖为空或数据非法";
            saveLearningState(store, report);
            return report;
        }

        report.actualIssue = actualDraw.issue == null ? "" : actualDraw.issue;
        String dedupKey = "learning_settled_v20_" + Math.abs(report.actualIssue.hashCode());
        try {
            if ("1".equals(store.getState(dedupKey, "0"))) {
                report.ok = 1;
                report.detail = "同期开奖已学习，幂等跳过：" + report.actualIssue;
                return report;
            }
            store.registerModuleBuildV20("LearningEngine", MODULE_BUILD_VERSION, "learnAfterDraw");
        } catch (Throwable stateFailure) {
            addSkip(report, "学习幂等/模块状态读取失败：" + stateFailure.getClass().getSimpleName());
        }
        report.learnedAt = System.currentTimeMillis();

        int matrixCells =
                learnTeacherMatrixFromDraw(
                        store,
                        actualDraw,
                        TEACHER_TASK_REALTIME,
                        "REALTIME_" + report.actualIssue,
                        -1);

        if (matrixCells > 0) {
            report.teacherMatrixDrawCount++;
            report.teacherMatrixCellCount += matrixCells;
        }

        if (latestDecision != null) {
            latestDecision.decision = normalizeDecisionText(latestDecision.decision);
        }

        if (latestDecision != null && shouldLearnIssue(latestDecision.targetIssue, actualDraw)) {
            learnDecisionByType(store, latestDecision, actualDraw, report);
        } else if (latestDecision != null) {
            addSkip(report, "最新决策目标期与实际开奖不一致，跳过决策学习，但教师矩阵已学习开奖号");
        }

        if (shadowCandidates != null && shadowCandidates.size() > 0) {
            learnShadowCandidates(store, shadowCandidates, actualDraw, report);
        }

        if (actualDraw.issue != null && actualDraw.issue.length() > 0) {
            learnShadowByStoredIssueInternal(store, actualDraw.issue, actualDraw, report);
        }

        learnRecentTradesByIssueOptional(store, actualDraw, report);

        report.ok = 1;
        report.detail = buildReportDetail(report);
        saveLearningState(store, report);
        try {
            store.setState(dedupKey, "1");
            store.setState("learning_last_real_issue", report.actualIssue);
            store.setState("learning_module_version", MODULE_BUILD_VERSION);
        } catch (Throwable stateFailure) {
            addSkip(report, "学习完成状态持久化失败：" + stateFailure.getClass().getSimpleName());
            report.detail = buildReportDetail(report);
        }
        RuntimeEventHooksR926.learned(store, report.actualIssue, "LearningEngine.learnAfterDraw", report.detail);
        return report;
    }

    public LearningReport learnByTargetIssue(
            DbStore store, String targetIssue, Models.Draw actualDraw) {
        LearningReport report = new LearningReport();

        if (store == null) {
            report.ok = 0;
            report.detail = "学习失败：DbStore为空";
            return report;
        }

        if (targetIssue == null || targetIssue.length() == 0) {
            report.ok = 0;
            report.detail = "学习失败：目标期为空";
            saveLearningState(store, report);
            return report;
        }

        if (actualDraw == null || !actualDraw.isValid()) {
            report.ok = 0;
            report.detail = "学习失败：实际开奖非法";
            saveLearningState(store, report);
            return report;
        }

        if (!dataGuard.isSameIssue(targetIssue, actualDraw.issue)) {
            report.ok = 0;
            report.actualIssue = actualDraw.issue == null ? "" : actualDraw.issue;
            report.detail = "学习跳过：目标期与实际开奖不一致，目标=" + targetIssue + "，实际=" + report.actualIssue;
            saveLearningState(store, report);
            return report;
        }

        report.actualIssue = actualDraw.issue == null ? "" : actualDraw.issue;
        report.learnedAt = System.currentTimeMillis();

        int matrixCells =
                learnTeacherMatrixFromDraw(
                        store, actualDraw, TEACHER_TASK_REALTIME, "TARGET_" + targetIssue, -1);

        if (matrixCells > 0) {
            report.teacherMatrixDrawCount++;
            report.teacherMatrixCellCount += matrixCells;
        }

        Models.Decision matched = null;

        try {
            matched = store.getDecisionByTargetIssue(targetIssue);
        } catch (Throwable e) {
            addSkip(report, "目标期决策读取失败，降级到最近决策扫描：" + e.getClass().getSimpleName());
            matched = null;
        }

        if (matched == null) {
            List<Models.Decision> recent = null;

            try {
                recent = store.getRecentDecisions(160);
            } catch (Throwable e) {
                addSkip(report, "最近决策读取失败：" + e.getClass().getSimpleName());
                recent = null;
            }

            matched = findDecisionForTarget(recent, targetIssue);
        }

        if (matched != null) {
            matched.decision = normalizeDecisionText(matched.decision);
            learnDecisionByType(store, matched, actualDraw, report);
        } else {
            addSkip(report, "未找到目标期决策，只学习教师矩阵和存储影子候选");
        }

        learnShadowByStoredIssueInternal(store, targetIssue, actualDraw, report);
        learnRecentTradesByIssueOptional(store, actualDraw, report);

        report.ok = 1;
        report.detail = buildReportDetail(report);
        saveLearningState(store, report);
        RuntimeEventHooksR926.learned(store, report.actualIssue, "LearningEngine.learnByTargetIssue", report.detail);

        return report;
    }

    public int learnLatest(DbStore store) {
        if (store == null) {
            return 0;
        }

        Models.Draw latest = null;

        try {
            latest = store.getLatestDraw();
        } catch (Throwable e) {
            latest = null;
        }

        if (latest == null || !latest.isValid()) {
            return 0;
        }

        LearningReport r = learnByTargetIssue(store, latest.issue, latest);

        if (r == null) {
            return 0;
        }

        return r.teacherMatrixCellCount
                + r.autoLearnSavedCount
                + r.realBetLearnCount
                + r.shadowLearnCount
                + r.holdObservationCount;
    }

    public int learnAfterSettlement(DbStore store, Models.Draw actualDraw) {
        if (store == null || actualDraw == null || !actualDraw.isValid()) {
            return 0;
        }

        LearningReport r = learnByTargetIssue(store, actualDraw.issue, actualDraw);

        if (r == null) {
            return 0;
        }

        return r.teacherMatrixCellCount
                + r.realBetLearnCount
                + r.shadowLearnCount
                + r.holdObservationCount;
    }

    public int learnAfterNewDraw(DbStore store, Models.Draw actualDraw) {
        return learnAfterSettlement(store, actualDraw);
    }

    public int learnAfterTradeSettlement(
            DbStore store, Models.Trade trade, Models.Draw actualDraw) {
        LearningReport report = new LearningReport();
        learnTrade(store, trade, actualDraw, report);

        if (store != null) {
            report.ok = 1;
            report.detail = buildReportDetail(report);
            saveLearningState(store, report);
        }

        return report.realBetLearnCount + report.teacherMatrixCellCount;
    }

    public int learnAfterTrade(DbStore store, Models.Trade trade, Models.Draw actualDraw) {
        return learnAfterTradeSettlement(store, trade, actualDraw);
    }

    public int learnSettledTrade(DbStore store, Models.Trade trade, Models.Draw actualDraw) {
        return learnAfterTradeSettlement(store, trade, actualDraw);
    }

    /* =========================
     * 自动小批量后台学习
     * ========================= */

    public LearningReport runAutoLearnBatch(DbStore store) {
        return runAutoLearnBatch(store, AUTO_LEARN_BATCH_MAX);
    }

    public LearningReport learnAutoSmallBatch(DbStore store) {
        return runAutoLearnBatch(store, AUTO_LEARN_BATCH_MAX);
    }

    public LearningReport learnAutoSmallBatch(DbStore store, int batchSize) {
        return runAutoLearnBatch(store, batchSize);
    }

    public int runTeacherSmallBatch(DbStore store) {
        LearningReport r = runAutoLearnBatch(store, AUTO_LEARN_BATCH_MAX);

        if (r == null) {
            return 0;
        }

        return r.teacherMatrixCellCount + r.autoLearnSavedCount;
    }

    public int runTeacherSmallBatch(DbStore store, int batchSize) {
        LearningReport r = runAutoLearnBatch(store, batchSize);

        if (r == null) {
            return 0;
        }

        return r.teacherMatrixCellCount + r.autoLearnSavedCount;
    }

    public LearningReport runAutoLearnBatch(DbStore store, int batchSize) {
        LearningReport report = new LearningReport();
        report.learnedAt = System.currentTimeMillis();
        report.lastDecision = "AUTO_LEARN";

        if (store == null) {
            report.ok = 0;
            report.detail = "后台自动学习失败：DbStore为空";
            return report;
        }

        if (batchSize <= 0 || batchSize > AUTO_LEARN_BATCH_MAX) {
            batchSize = AUTO_LEARN_BATCH_MAX;
        }

        long now = System.currentTimeMillis();

        if (lastSmallBatchAt > 0L && now - lastSmallBatchAt < SMALL_BATCH_MIN_INTERVAL_MS) {
            report.ok = 0;
            report.detail = "后台自动学习跳过：刚刚已运行，避免重复触发";
            saveLearningState(store, report);
            return report;
        }

        lastSmallBatchAt = now;

        List<Models.Draw> rawDraws = null;

        try {
            rawDraws = store.getRecentDraws(AUTO_LEARN_HISTORY_LIMIT);
        } catch (Throwable e) {
            rawDraws = null;
        }

        ArrayList<Models.Draw> draws = dataGuard.normalizeHistory(rawDraws);

        if (draws == null || draws.size() < AUTO_LEARN_MIN_TRAIN + 2) {
            report.ok = 0;
            report.detail = "后台自动学习跳过：有效历史不足，至少需要" + AUTO_LEARN_MIN_TRAIN + "期以上";
            saveAutoLearnState(store, report, 0, 0, AUTO_STATUS_SKIP, report.detail);
            saveLearningState(store, report);
            return report;
        }

        int safeStart = AUTO_LEARN_MIN_TRAIN;
        int lastTargetIndex = draws.size() - 1;
        int nextIndex = safeStart;
        Models.AutoLearnTask cursor = null;

        try {
            cursor = store.getAutoLearnCursor(AUTO_TASK_KEY);
        } catch (Throwable e) {
            cursor = null;
        }

        if (cursor != null
                && cursor.nextTargetIndex >= safeStart
                && cursor.nextTargetIndex <= lastTargetIndex) {
            nextIndex = cursor.nextTargetIndex;
        }

        if (nextIndex > lastTargetIndex) {
            nextIndex = safeStart;
        }

        int endIndex = nextIndex + batchSize - 1;

        if (endIndex > lastTargetIndex) {
            endIndex = lastTargetIndex;
        }

        ArrayList<Models.LearningCase> cases = new ArrayList<Models.LearningCase>();
        int issueCount = 0;
        int hitCount = 0;
        int missCount = 0;
        int caseCount = 0;
        int profileWriteCount = 0;
        int teacherCells = 0;
        int teacherDraws = 0;
        String lastIssue = "";
        String lastTrace = "";
        String lastAdvice = "";
        String lastStateKey = "";
        String lastExpectedShape = "";
        String lastActualShape = "";
        String lastWhyActual = "";
        String lastWhyMiss = "";

        saveAutoLearnState(
                store,
                report,
                nextIndex,
                endIndex,
                AUTO_STATUS_RUNNING,
                "后台小批量学习运行中：每期旧画像10位置 + 教师矩阵100号码样本；不写真实校准桶");

        for (int i = nextIndex; i <= endIndex; i++) {
            Models.Draw target = draws.get(i);

            if (target == null || !target.isValid()) {
                continue;
            }

            issueCount++;
            lastIssue = target.issue == null ? "" : target.issue;

            int matrix =
                    learnTeacherMatrixFromDraw(
                            store, target, TEACHER_TASK_AUTO_BATCH, "AUTO_IDX_" + i, i);

            if (matrix > 0) {
                teacherCells += matrix;
                teacherDraws++;
            }

            for (int position = 1; position <= AUTO_LEARN_POSITION_COUNT; position++) {
                AutoBuildResult r = buildAutoLearningCase(draws, i, position);

                if (r == null || r.learningCase == null) {
                    continue;
                }

                Models.LearningCase c = r.learningCase;

                cases.add(c);
                caseCount++;

                if (c.hit == 1) {
                    hitCount++;
                } else {
                    missCount++;
                    addMissReason(report, c.mainReasonCode);
                }

                profileWriteCount += writeAutoProfileLearning(store, c, r.forecast);

                if (lastTrace.length() == 0 || c.hit == 0) {
                    lastTrace = buildCaseShortTrace(c);
                    lastAdvice = c.nextAdvice == null ? "" : c.nextAdvice;
                    lastStateKey = c.stateKey == null ? "" : c.stateKey;
                    lastExpectedShape = c.expectedShape == null ? "" : c.expectedShape;
                    lastActualShape = c.actualShape == null ? "" : c.actualShape;
                    lastWhyActual = c.whyActualHit == null ? "" : c.whyActualHit;
                    lastWhyMiss = c.whyMiss == null ? "" : c.whyMiss;
                }
            }
        }

        int saved = 0;

        if (cases.size() > 0) {
            try {
                saved = store.saveLearningCases(cases);
            } catch (Throwable e) {
                addError(report, "后台学习案例写入异常");
            }
        }

        int newNext = endIndex + 1;
        String status = AUTO_STATUS_IDLE;

        if (newNext > lastTargetIndex) {
            newNext = safeStart;
            status = AUTO_STATUS_DONE;
        }

        report.ok = 1;
        report.autoLearnIssueCount = issueCount;
        report.autoLearnCaseCount = caseCount;
        report.autoLearnSavedCount = saved;
        report.autoLearnHitCount = hitCount;
        report.autoLearnMissCount = missCount;
        report.autoLearnStartIndex = nextIndex;
        report.autoLearnEndIndex = endIndex;
        report.autoLearnNextIndex = newNext;
        report.autoLearnLastIssue = lastIssue;
        report.teacherMatrixDrawCount = teacherDraws;
        report.teacherMatrixCellCount = teacherCells;

        if (caseCount > 0) {
            report.autoLearnHitRate = ((double) hitCount) / ((double) caseCount);
        }

        report.stateKey = lastStateKey;
        report.expectedShape = lastExpectedShape;
        report.actualShape = lastActualShape;
        report.whyActualAppeared = lastWhyActual;
        report.whyPredictedFailed = lastWhyMiss;
        report.nextStateAdvice = lastAdvice;
        report.lastTraceSummary = lastTrace;

        report.detail =
                "后台小批量学习完成：期数="
                        + issueCount
                        + "，旧画像位置样本="
                        + caseCount
                        + "，写入="
                        + saved
                        + "，画像更新="
                        + profileWriteCount
                        + "，教师矩阵期数="
                        + teacherDraws
                        + "，教师矩阵格="
                        + teacherCells
                        + "，理论应学="
                        + (issueCount * TEACHER_MATRIX_SAMPLE_PER_DRAW)
                        + "，命中="
                        + hitCount
                        + "，未中="
                        + missCount
                        + "，样本命中率="
                        + UiFormatter.probability(report.autoLearnHitRate)
                        + "，下次游标="
                        + newNext
                        + "；说明：后台样本命中率不是实盘胜率；教师矩阵是每期开奖10位置×10号码正负样本；本层只反哺状态画像、号码画像、组合画像、风险解释，不救援BET、不降低门槛、不写真实校准桶。";

        saveAutoLearnCursor(store, nextIndex, endIndex, newNext, lastIssue, status, report.detail);
        saveAutoLearnState(store, report, nextIndex, endIndex, status, report.detail);
        saveLearningState(store, report);

        try {
            store.setState(
                    "runtime_profile_summary",
                    "后台画像更新="
                            + profileWriteCount
                            + "；教师矩阵="
                            + teacherCells
                            + "格"
                            + "；状态="
                            + lastStateKey
                            + "；实际为什么出="
                            + safeLongText(lastWhyActual)
                            + "；预测为什么偏="
                            + safeLongText(lastWhyMiss)
                            + "；修正="
                            + safeLongText(lastAdvice)
                            + "；规则=不写真实校准桶/不救援BET/不降低门槛");
            store.setState("learning_next_advice", lastAdvice == null ? "" : lastAdvice);
            store.setState("learning_reason_detail", report.detail);
            store.setState(
                    "teacher_matrix_summary",
                    "教师矩阵小批量学习：期数=" + teacherDraws + "｜样本格=" + teacherCells + "｜只做画像和风险反哺");
        } catch (Throwable e) {
        }

        return report;
    }

    /* =========================
     * 教师矩阵分段学习
     * ========================= */

    public int runTeacherWalkForwardSegment(DbStore store) {
        return runTeacherWalkForwardSegment(store, 300);
    }

    public int runTeacherWalkForwardSegment(DbStore store, int windowSize) {
        if (store == null) {
            return 0;
        }

        if (windowSize <= 0) {
            windowSize = 300;
        }

        if (windowSize > 500) {
            windowSize = 500;
        }

        if (windowSize < 120) {
            windowSize = 120;
        }

        List<Models.Draw> raw = null;

        try {
            raw = store.getRecentDraws(AUTO_LEARN_HISTORY_LIMIT);
        } catch (Throwable e) {
            raw = null;
        }

        ArrayList<Models.Draw> draws = dataGuard.normalizeHistory(raw);

        if (draws == null || draws.size() < windowSize + AUTO_LEARN_MIN_TRAIN) {
            try {
                store.setState("learning_summary", "教师矩阵分段学习跳过：历史不足，窗口=" + windowSize);
            } catch (Throwable e) {
            }
            return 0;
        }

        int end = draws.size() - 1;
        int start = end - windowSize + 1;

        if (start < AUTO_LEARN_MIN_TRAIN) {
            start = AUTO_LEARN_MIN_TRAIN;
        }

        return runTeacherSegment(store, start, end);
    }

    public int runRandomTeacherSegment(DbStore store, int windowSize) {
        return runTeacherWalkForwardSegment(store, windowSize);
    }

    public int runTeacherSegment(DbStore store, int startIndex, int endIndex) {
        if (store == null) {
            return 0;
        }

        List<Models.Draw> raw = null;

        try {
            raw = store.getRecentDraws(AUTO_LEARN_HISTORY_LIMIT);
        } catch (Throwable e) {
            raw = null;
        }

        ArrayList<Models.Draw> draws = dataGuard.normalizeHistory(raw);

        if (draws == null || draws.size() == 0) {
            return 0;
        }

        if (startIndex < AUTO_LEARN_MIN_TRAIN) {
            startIndex = AUTO_LEARN_MIN_TRAIN;
        }

        if (endIndex >= draws.size()) {
            endIndex = draws.size() - 1;
        }

        if (endIndex < startIndex) {
            return 0;
        }

        int cells = 0;
        int drawCount = 0;
        String segment = "SEG_" + startIndex + "_" + endIndex;

        try {
            store.setState("auto_learn_status", AUTO_STATUS_RUNNING);
            store.setState("auto_learn_task", TEACHER_TASK_MANUAL_SEGMENT);
            store.setState("learning_summary", "教师矩阵分段学习开始：" + segment + "；只做画像和风险反哺");
        } catch (Throwable e) {
        }

        for (int i = startIndex; i <= endIndex; i++) {
            Models.Draw d = draws.get(i);

            if (d == null || !d.isValid()) {
                continue;
            }

            int n = learnTeacherMatrixFromDraw(store, d, TEACHER_TASK_MANUAL_SEGMENT, segment, i);

            if (n > 0) {
                cells += n;
                drawCount++;
            }

            if (i % 20 == 0 || i == endIndex) {
                try {
                    int total = endIndex - startIndex + 1;
                    int done = i - startIndex + 1;
                    int progress = total > 0 ? (done * 100 / total) : 100;
                    store.setState("auto_learn_progress", String.valueOf(progress));
                    store.setState(
                            "learning_summary",
                            "教师矩阵分段学习中：" + segment + "｜进度=" + progress + "%｜样本格=" + cells);
                } catch (Throwable e) {
                }
            }
        }

        try {
            store.markAutoLearnSegmentDone(
                    TEACHER_TASK_MANUAL_SEGMENT, segment, draws.get(endIndex).issue);
            store.setState("auto_learn_status", AUTO_STATUS_DONE);
            store.setState("auto_learn_progress", "100");
            store.setState(
                    "learning_summary",
                    "教师矩阵分段学习完成：" + segment + "｜期数=" + drawCount + "｜样本格=" + cells + "｜不救援BET");
            store.setState(
                    "teacher_matrix_summary",
                    "教师矩阵分段完成：期数=" + drawCount + "｜样本格=" + cells + "｜只做画像/风险/解释");
        } catch (Throwable e) {
        }

        return cells;
    }

    public boolean shouldRunSmallBatch(DbStore store) {
        if (store == null) {
            return false;
        }

        try {
            if (store.getValidDrawCount() < AUTO_LEARN_MIN_TRAIN + 2) {
                return false;
            }
        } catch (Throwable e) {
            return false;
        }

        long now = System.currentTimeMillis();

        if (lastSmallBatchAt > 0L && now - lastSmallBatchAt < SMALL_BATCH_COOLDOWN_MS) {
            return false;
        }

        try {
            String status = store.getState("auto_learn_status", AUTO_STATUS_IDLE);

            if (AUTO_STATUS_RUNNING.equals(status)) {
                return false;
            }
        } catch (Throwable e) {
        }

        return true;
    }

    public boolean shouldRunRealtimeLearn(DbStore store, Models.Draw actualDraw) {
        if (store == null || actualDraw == null || !actualDraw.isValid()) {
            return false;
        }

        long now = System.currentTimeMillis();

        if (lastRealtimeLearnAt > 0L && now - lastRealtimeLearnAt < 800L) {
            return false;
        }

        return true;
    }

    public String getLearningStatusText(DbStore store) {
        if (store == null) {
            return "学习状态：未连接数据库";
        }

        try {
            String auto = store.getState("auto_learn_status", AUTO_STATUS_IDLE);
            String progress = store.getState("auto_learn_progress", "0");
            String summary = store.getState("learning_summary", "");
            String matrix = store.getState("teacher_matrix_summary", "");

            StringBuilder sb = new StringBuilder();
            sb.append("学习状态：");
            sb.append(auto);

            if (progress != null && progress.length() > 0) {
                sb.append("｜进度=");
                sb.append(progress);
            }

            if (summary != null && summary.length() > 0) {
                sb.append("\n");
                sb.append(summary);
            }

            if (matrix != null && matrix.length() > 0) {
                sb.append("\n");
                sb.append(matrix);
            }

            return sb.toString();
        } catch (Throwable e) {
            return "学习状态：读取异常";
        }
    }

    /* =========================
     * 后台自动画像样本
     * ========================= */

    private AutoBuildResult buildAutoLearningCase(
            List<Models.Draw> draws, int targetIndex, int position) {
        if (draws == null
                || targetIndex <= 0
                || targetIndex >= draws.size()
                || !AppConfig.isValidPosition(position)) {
            return null;
        }

        Models.Draw target = draws.get(targetIndex);

        if (target == null || !target.isValid()) {
            return null;
        }

        int actual = target.getNumberAtPosition(position);

        if (!AppConfig.isValidNumber(actual)) {
            return null;
        }

        PositionForecast f = buildPositionForecast(draws, targetIndex, position);

        if (f == null || !isValidForecastK3(f)) {
            return null;
        }

        boolean hit = strictForecastK3Hit(f, actual);
        String targetIssue = target.issue == null ? "" : target.issue;
        Models.Draw base = targetIndex > 0 ? draws.get(targetIndex - 1) : null;
        String baseIssue = base == null || base.issue == null ? "" : base.issue;

        Models.LearningCase c = new Models.LearningCase();

        c.caseKey = "AUTO|" + targetIssue + "|P" + position + "|K" + f.k3Mask;
        c.taskKey = AUTO_TASK_KEY;
        c.segmentKey = "IDX_" + targetIndex;
        c.baseIssue = baseIssue;
        c.targetIssue = targetIssue;
        c.createdAt = System.currentTimeMillis();
        c.targetIndex = targetIndex;
        c.position = position;
        c.actualNumber = actual;
        c.predictedK3 = f.k3Mask;
        c.k3Mask = f.k3Mask;
        c.hit = hit ? 1 : 0;

        c.stateKey = f.stateKey;
        c.stateMode = f.stateMode;
        c.trendKey = f.trendKey;
        c.shapeKey = f.shapeKey;
        c.driftKey = f.driftKey;
        c.breakKey = f.breakKey;
        c.recoveryKey = f.recoveryKey;
        c.transitionKey = f.transitionKey;

        c.expectedShape = f.expectedShape;
        c.actualShape = buildActualShapeForAuto(actual, f);
        c.whyExpectedHit = f.whyExpectedHit;
        c.whyActualHit = buildWhyAutoActualAppeared(f, actual, hit);
        c.whyMiss = hit ? "" : buildAutoMissReason(f, actual);
        c.nextAdvice = buildAutoNextAdvice(f, actual, hit);
        c.mainReasonCode = hit ? "AUTO_HIT_STATE_OK" : classifyAutoMissCode(f, actual);
        c.affectedModule = "AUTO_STATE_NUMBER_PROFILE";

        c.finalP = f.finalP;
        c.dynamicGate = f.dynamicGate;
        c.edge = f.finalP - f.dynamicGate;
        c.trendScore = f.trendScore;
        c.shapeScore = f.shapeScore;
        c.driftRisk = f.driftRisk;
        c.breakRisk = f.breakRisk;
        c.recoveryScore = f.recoveryScore;
        c.profileScore = f.profileScore;
        c.numberBias = f.numberBias;
        c.comboBias = f.comboBias;
        c.riskScore = f.riskScore;
        c.teacherWeight = f.teacherWeight;
        c.stateConfidence = f.stateConfidence;
        c.stateSample = f.stateSample;
        c.cacheSample = f.cacheSample;
        c.recentSample = f.recentSample;
        c.recentHitRate = f.recentHitRate;

        AutoBuildResult out = new AutoBuildResult();
        out.learningCase = c;
        out.forecast = f;
        return out;
    }

    private PositionForecast buildPositionForecast(
            List<Models.Draw> draws, int targetIndex, int position) {
        if (draws == null || targetIndex <= 0 || !AppConfig.isValidPosition(position)) {
            return null;
        }

        int start = targetIndex - AUTO_LEARN_SCORE_WINDOW_DEEP;

        if (start < 0) {
            start = 0;
        }

        int sample = targetIndex - start;

        if (sample <= 0) {
            return null;
        }

        double[] score = new double[11];
        int[] countShort = new int[11];
        int[] countMid = new int[11];
        int[] countLong = new int[11];
        int[] countDeep = new int[11];
        int[] markovNext = new int[11];
        int[] lastGap = new int[11];
        int[] transitionFromPrevZone = new int[11];

        for (int n = 1; n <= 10; n++) {
            lastGap[n] = 999;
        }

        Models.Draw prevDraw = draws.get(targetIndex - 1);
        int prevNumber = prevDraw == null ? 0 : prevDraw.getNumberAtPosition(position);
        int prevZone = numberZone(prevNumber);

        for (int i = start; i < targetIndex; i++) {
            Models.Draw d = draws.get(i);

            if (d == null || !d.isValid()) {
                continue;
            }

            int n = d.getNumberAtPosition(position);

            if (!AppConfig.isValidNumber(n)) {
                continue;
            }

            int distanceToTarget = targetIndex - i;

            if (distanceToTarget <= AUTO_LEARN_SCORE_WINDOW_SHORT) {
                countShort[n]++;
            }

            if (distanceToTarget <= AUTO_LEARN_SCORE_WINDOW_MID) {
                countMid[n]++;
            }

            if (distanceToTarget <= AUTO_LEARN_SCORE_WINDOW_LONG) {
                countLong[n]++;
            }

            countDeep[n]++;

            for (int x = 1; x <= 10; x++) {
                if (lastGap[x] < 999) {
                    lastGap[x]++;
                }
            }

            lastGap[n] = 0;

            if (AppConfig.isValidNumber(prevNumber) && i + 1 < targetIndex) {
                Models.Draw next = draws.get(i + 1);
                int current = d.getNumberAtPosition(position);

                if (current == prevNumber && next != null && next.isValid()) {
                    int nx = next.getNumberAtPosition(position);

                    if (AppConfig.isValidNumber(nx)) {
                        markovNext[nx]++;
                    }
                }
            }

            if (prevZone > 0 && i + 1 < targetIndex) {
                Models.Draw next = draws.get(i + 1);
                if (next != null && next.isValid() && numberZone(n) == prevZone) {
                    int nx = next.getNumberAtPosition(position);
                    if (AppConfig.isValidNumber(nx)) {
                        transitionFromPrevZone[nx]++;
                    }
                }
            }
        }

        double shortBase = AUTO_LEARN_SCORE_WINDOW_SHORT;
        double midBase = AUTO_LEARN_SCORE_WINDOW_MID;
        double longBase = Math.min(sample, AUTO_LEARN_SCORE_WINDOW_LONG);
        double deepBase = sample;
        int markovTotal = 0;
        int zoneTransTotal = 0;

        for (int n = 1; n <= 10; n++) {
            markovTotal += markovNext[n];
            zoneTransTotal += transitionFromPrevZone[n];
        }

        for (int n = 1; n <= 10; n++) {
            double s = countShort[n] / shortBase;
            double m = countMid[n] / midBase;
            double l = countLong[n] / longBase;
            double dp = countDeep[n] / deepBase;
            double mk = markovTotal > 0 ? ((double) markovNext[n]) / ((double) markovTotal) : 0.0d;
            double zt =
                    zoneTransTotal > 0
                            ? ((double) transitionFromPrevZone[n]) / ((double) zoneTransTotal)
                            : 0.0d;
            double gapScore = gapToScore(lastGap[n]);
            double zoneBalance = zoneBalanceScore(n, countDeep);
            double rebound = reboundScore(lastGap[n], s, m, l);

            score[n] =
                    s * 0.28d
                            + m * 0.20d
                            + l * 0.15d
                            + dp * 0.08d
                            + mk * 0.17d
                            + zt * 0.05d
                            + gapScore * 0.04d
                            + rebound * 0.02d
                            + zoneBalance * 0.01d;
        }

        int n1 = pickTop(score, 0, 0);
        int n2 = pickTop(score, n1, 0);
        int n3 = pickTop(score, n1, n2);

        if (!isValidK3Numbers(n1, n2, n3)) {
            return null;
        }

        PositionForecast f = new PositionForecast();

        f.position = position;
        f.n1 = n1;
        f.n2 = n2;
        f.n3 = n3;
        f.k3Mask = Models.buildK3Mask(n1, n2, n3);
        f.sample = sample;

        double top1 = score[n1];
        double top2 = score[n2];
        double top3 = score[n3];
        double top4 = score[pickTop(score, n1, n2, n3)];

        double topAvg = (top1 + top2 + top3) / 3.0d;
        double gap = top3 - top4;

        if (gap < 0.0d) {
            gap = 0.0d;
        }

        f.comboGap = gap;
        f.trendScore =
                clamp01(
                        (countShort[n1] + countShort[n2] + countShort[n3])
                                / (AUTO_LEARN_SCORE_WINDOW_SHORT * 0.72d));
        f.shapeScore = clamp01(topAvg * 5.0d);
        f.driftRisk = calcDriftRisk(countShort, countMid, countLong);
        f.breakRisk = calcBreakRisk(countShort, countMid, countLong, n1, n2, n3);
        f.recoveryScore = calcRecoveryScore(lastGap, n1, n2, n3);
        f.riskScore = clamp01(f.driftRisk * 0.55d + f.breakRisk * 0.45d);
        f.numberBias = clampSigned((top1 - top3) * 2.0d, 0.30d);
        f.comboBias = clampSigned(gap * 3.0d, 0.30d);
        f.profileScore = clamp01(topAvg * 4.0d - f.riskScore * 0.20d);
        f.stateConfidence = clamp01(sample / 500.0d);
        f.stateSample = sample;
        f.cacheSample = countLong[n1] + countLong[n2] + countLong[n3];
        f.recentSample = Math.min(sample, AUTO_LEARN_SCORE_WINDOW_SHORT);
        f.recentHitRate =
                clamp01(
                        (countShort[n1] + countShort[n2] + countShort[n3])
                                / (AUTO_LEARN_SCORE_WINDOW_SHORT * 1.0d));
        f.teacherWeight = 1.0d;

        f.finalP = clampProbability(0.300d + topAvg * 1.55d + gap * 0.85d - f.riskScore * 0.045d);
        f.dynamicGate = AppConfig.DEFAULT_DYNAMIC_GATE;

        f.stateMode = buildAutoStateMode(f);
        f.trendKey = bucketKey("TR", f.trendScore);
        f.driftKey = bucketKey("DR", f.driftRisk);
        f.breakKey = bucketKey("BR", f.breakRisk);
        f.recoveryKey = bucketKey("RC", f.recoveryScore);
        f.transitionKey =
                AppConfig.isValidNumber(prevNumber)
                        ? "PREV_" + formatNumber(prevNumber) + "_Z" + prevZone
                        : "PREV_00";
        f.shapeKey = buildAutoShapeKey(f);
        f.stateKey =
                "AUTO|P"
                        + position
                        + "|"
                        + f.stateMode
                        + "|"
                        + f.trendKey
                        + "|"
                        + f.driftKey
                        + "|"
                        + f.breakKey
                        + "|"
                        + f.recoveryKey
                        + "|"
                        + f.transitionKey
                        + "|"
                        + f.shapeKey;

        f.expectedShape =
                "K3="
                        + formatK3(n1, n2, n3)
                        + "，短窗覆盖="
                        + UiFormatter.probability(f.recentHitRate)
                        + "，组合差="
                        + formatDouble(gap)
                        + "，漂移="
                        + UiFormatter.probability(f.driftRisk)
                        + "，断点="
                        + UiFormatter.probability(f.breakRisk)
                        + "，恢复="
                        + UiFormatter.probability(f.recoveryScore);

        f.whyExpectedHit =
                "自动反推只使用目标期之前"
                        + sample
                        + "期，综合短窗频率、中窗稳定、长窗背景、500期深层背景、上一号码转移、上一区间转移、间隔回补和区间平衡，选出该位置K3。";

        return f;
    }

    private int writeAutoProfileLearning(DbStore store, Models.LearningCase c, PositionForecast f) {
        if (store == null || c == null || f == null) {
            return 0;
        }

        int count = 0;
        boolean hit = c.hit == 1;

        count +=
                invokeProfileUpdate(
                        store, c.stateKey, hit, c.profileScore, c.riskScore, c.stateSample);
        count +=
                invokeNumberProfileUpdate(
                        store,
                        c.stateKey,
                        c.position,
                        c.actualNumber,
                        true,
                        actualNumberSupport(f, c.actualNumber),
                        c.riskScore,
                        c.stateSample);

        int[] nums = new int[] {f.n1, f.n2, f.n3};

        for (int i = 0; i < nums.length; i++) {
            int n = nums[i];

            if (!AppConfig.isValidNumber(n)) {
                continue;
            }

            boolean numberHit = n == c.actualNumber;
            double support = actualNumberSupport(f, n);

            count +=
                    invokeNumberProfileUpdate(
                            store,
                            c.stateKey,
                            c.position,
                            n,
                            numberHit,
                            support,
                            c.riskScore,
                            c.stateSample);
        }

        try {
            store.setState("learning_state_key", c.stateKey == null ? "" : c.stateKey);
            store.setState(
                    "learning_expected_shape", c.expectedShape == null ? "" : c.expectedShape);
            store.setState("learning_actual_shape", c.actualShape == null ? "" : c.actualShape);
            store.setState("learning_next_advice", c.nextAdvice == null ? "" : c.nextAdvice);
        } catch (Throwable e) {
        }

        return count;
    }

    /* =========================
     * TeacherMatrix调用
     * ========================= */

    private int learnTeacherMatrixFromDraw(
            DbStore store, Models.Draw draw, String taskKey, String segmentKey, int targetIndex) {
        if (store == null || draw == null || !draw.isValid()) {
            return 0;
        }

        long now = System.currentTimeMillis();

        if (TEACHER_TASK_REALTIME.equals(taskKey)) {
            if (lastRealtimeLearnAt > 0L
                    && now - lastRealtimeLearnAt < REALTIME_LEARN_MIN_INTERVAL_MS) {
                return 0;
            }

            lastRealtimeLearnAt = now;
        }

        if (teacherMatrixEngine == null) {
            try {
                store.setState(
                        "teacher_matrix_summary",
                        "教师矩阵等待：TeacherMatrixEngine/TeacherNumberMatrixEngine 未找到，当前只运行旧学习画像。");
            } catch (Throwable e) {
            }
            return 0;
        }

        Object result =
                invokeTeacherMatrix(
                        "learnFromSettledDrawFullMatrix",
                        new Class[] {
                            DbStore.class,
                            Models.Draw.class,
                            String.class,
                            String.class,
                            Integer.TYPE
                        },
                        new Object[] {
                            store, draw, taskKey, segmentKey, Integer.valueOf(targetIndex)
                        });

        int cells = objectToInt(result);

        if (cells <= 0) {
            result =
                    invokeTeacherMatrix(
                            "learnFromDrawFullMatrix",
                            new Class[] {
                                DbStore.class,
                                Models.Draw.class,
                                String.class,
                                String.class,
                                Integer.TYPE
                            },
                            new Object[] {
                                store, draw, taskKey, segmentKey, Integer.valueOf(targetIndex)
                            });
            cells = objectToInt(result);
        }

        if (cells <= 0) {
            cells = invokeTeacherFallback(store, draw, taskKey, segmentKey, targetIndex);
        }

        if (cells > 0) {
            try {
                store.setState(
                        "teacher_matrix_summary",
                        "教师矩阵学习：期号="
                                + draw.issue
                                + "｜样本格="
                                + cells
                                + "｜应学="
                                + TEACHER_MATRIX_SAMPLE_PER_DRAW
                                + "｜任务="
                                + taskKey
                                + "｜规则=只做画像/风险/解释，不直接救援BET");
            } catch (Throwable e) {
            }
        }

        return cells;
    }

    private int invokeTeacherFallback(
            DbStore store, Models.Draw draw, String taskKey, String segmentKey, int targetIndex) {
        if (teacherMatrixEngine == null) {
            return 0;
        }

        Object result =
                invokeTeacherMatrix(
                        "learnFromDraw",
                        new Class[] {DbStore.class, Models.Draw.class},
                        new Object[] {store, draw});

        int n = objectToInt(result);

        if (n > 0) {
            return n;
        }

        result =
                invokeTeacherMatrix(
                        "learn",
                        new Class[] {DbStore.class, Models.Draw.class},
                        new Object[] {store, draw});

        n = objectToInt(result);

        if (n > 0) {
            return n;
        }

        result =
                invokeTeacherMatrix(
                        "learnTeacherNumberMatrixFromDraw",
                        new Class[] {DbStore.class, Models.Draw.class},
                        new Object[] {store, draw});

        return objectToInt(result);
    }

    private Object createTeacherMatrixEngineSafely() {
        try {
            Class cls = Class.forName("com.ethk3.novaonepro.TeacherNumberMatrixEngine");
            return cls.newInstance();
        } catch (Throwable e) {
        }

        try {
            Class cls = Class.forName("com.ethk3.novaonepro.TeacherMatrixEngine");
            return cls.newInstance();
        } catch (Throwable e) {
        }

        return null;
    }

    private Object invokeTeacherMatrix(String methodName, Class[] types, Object[] args) {
        if (teacherMatrixEngine == null || methodName == null || methodName.length() == 0) {
            return NO_METHOD;
        }

        try {
            Method m = teacherMatrixEngine.getClass().getMethod(methodName, types);
            return m.invoke(teacherMatrixEngine, args);
        } catch (Throwable e) {
            return NO_METHOD;
        }
    }

    private int objectToInt(Object value) {
        if (value == null || value == NO_METHOD) {
            return 0;
        }

        if (value instanceof Integer) {
            return ((Integer) value).intValue();
        }

        if (value instanceof Long) {
            return (int) ((Long) value).longValue();
        }

        if (value instanceof Number) {
            return ((Number) value).intValue();
        }

        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (Throwable e) {
            return 0;
        }
    }

    /* =========================
     * DbStore兼容画像更新
     * ========================= */

    private int invokeProfileUpdate(
            DbStore store, String stateKey, boolean hit, double score, double risk, int sample) {
        if (store == null || stateKey == null || stateKey.length() == 0) {
            return 0;
        }

        Object[] args1 =
                new Object[] {
                    stateKey,
                    Boolean.valueOf(hit),
                    Double.valueOf(score),
                    Double.valueOf(risk),
                    Integer.valueOf(sample)
                };
        Class[] types1 =
                new Class[] {String.class, Boolean.TYPE, Double.TYPE, Double.TYPE, Integer.TYPE};

        if (invokeOptionalStore(store, "updateRuntimeProfileCache", types1, args1) != NO_METHOD) {
            return 1;
        }

        if (invokeOptionalStore(store, "updateRuntimeProfile", types1, args1) != NO_METHOD) {
            return 1;
        }

        if (invokeOptionalStore(store, "learnRuntimeProfile", types1, args1) != NO_METHOD) {
            return 1;
        }

        if (invokeOptionalStore(store, "upsertRuntimeProfile", types1, args1) != NO_METHOD) {
            return 1;
        }

        Object[] args2 =
                new Object[] {
                    stateKey,
                    Integer.valueOf(hit ? 1 : 0),
                    Double.valueOf(score),
                    Double.valueOf(risk),
                    Integer.valueOf(sample)
                };
        Class[] types2 =
                new Class[] {String.class, Integer.TYPE, Double.TYPE, Double.TYPE, Integer.TYPE};

        if (invokeOptionalStore(store, "updateRuntimeProfileCache", types2, args2) != NO_METHOD) {
            return 1;
        }

        if (invokeOptionalStore(store, "updateRuntimeProfile", types2, args2) != NO_METHOD) {
            return 1;
        }

        if (invokeOptionalStore(store, "learnRuntimeProfile", types2, args2) != NO_METHOD) {
            return 1;
        }

        return 0;
    }

    private int invokeNumberProfileUpdate(
            DbStore store,
            String stateKey,
            int position,
            int number,
            boolean hit,
            double support,
            double risk,
            int sample) {
        if (store == null
                || stateKey == null
                || stateKey.length() == 0
                || !AppConfig.isValidPosition(position)
                || !AppConfig.isValidNumber(number)) {
            return 0;
        }

        Object[] args1 =
                new Object[] {
                    stateKey,
                    Integer.valueOf(position),
                    Integer.valueOf(number),
                    Boolean.valueOf(hit),
                    Double.valueOf(support),
                    Double.valueOf(risk),
                    Integer.valueOf(sample)
                };

        Class[] types1 =
                new Class[] {
                    String.class,
                    Integer.TYPE,
                    Integer.TYPE,
                    Boolean.TYPE,
                    Double.TYPE,
                    Double.TYPE,
                    Integer.TYPE
                };

        if (invokeOptionalStore(store, "updateStateNumberProfile", types1, args1) != NO_METHOD) {
            return 1;
        }

        if (invokeOptionalStore(store, "updateStateNumberProfileCache", types1, args1)
                != NO_METHOD) {
            return 1;
        }

        if (invokeOptionalStore(store, "learnStateNumberProfile", types1, args1) != NO_METHOD) {
            return 1;
        }

        if (invokeOptionalStore(store, "upsertStateNumberProfile", types1, args1) != NO_METHOD) {
            return 1;
        }

        Object[] args2 =
                new Object[] {
                    stateKey,
                    Integer.valueOf(position),
                    Integer.valueOf(number),
                    Integer.valueOf(hit ? 1 : 0),
                    Double.valueOf(support),
                    Double.valueOf(risk),
                    Integer.valueOf(sample)
                };

        Class[] types2 =
                new Class[] {
                    String.class,
                    Integer.TYPE,
                    Integer.TYPE,
                    Integer.TYPE,
                    Double.TYPE,
                    Double.TYPE,
                    Integer.TYPE
                };

        if (invokeOptionalStore(store, "updateStateNumberProfile", types2, args2) != NO_METHOD) {
            return 1;
        }

        if (invokeOptionalStore(store, "updateStateNumberProfileCache", types2, args2)
                != NO_METHOD) {
            return 1;
        }

        if (invokeOptionalStore(store, "learnStateNumberProfile", types2, args2) != NO_METHOD) {
            return 1;
        }

        return 0;
    }

    private Object invokeOptionalStore(
            DbStore store, String methodName, Class[] types, Object[] args) {
        if (store == null || methodName == null || methodName.length() == 0) {
            return NO_METHOD;
        }

        try {
            Method m = store.getClass().getMethod(methodName, types);
            return m.invoke(store, args);
        } catch (Throwable e) {
            return NO_METHOD;
        }
    }

    private Object invokeOptionalObject(
            Object target, String methodName, Class[] types, Object[] args) {
        if (target == null || methodName == null || methodName.length() == 0) {
            return NO_METHOD;
        }

        try {
            Method m = target.getClass().getMethod(methodName, types);
            return m.invoke(target, args);
        } catch (Throwable e) {
            return NO_METHOD;
        }
    }

    private double actualNumberSupport(PositionForecast f, int number) {
        if (f == null || !AppConfig.isValidNumber(number)) {
            return 0.0d;
        }

        double support = 0.300d;

        if (number == f.n1) {
            support += 0.090d;
        } else if (number == f.n2) {
            support += 0.070d;
        } else if (number == f.n3) {
            support += 0.050d;
        } else {
            int near = nearestDistanceToForecast(f, number);

            if (near == 1) {
                support += 0.020d;
            } else if (near == 2) {
                support += 0.010d;
            }
        }

        support += f.profileScore * 0.080d;
        support += f.comboGap * 0.400d;
        support -= f.riskScore * 0.050d;

        return clamp01(support);
    }

    /* =========================
     * 决策/交易学习
     * ========================= */

    private void learnDecisionByType(
            DbStore store,
            Models.Decision decision,
            Models.Draw actualDraw,
            LearningReport report) {
        if (decision == null) {
            return;
        }

        decision.decision = normalizeDecisionText(decision.decision);

        if (AppConfig.DECISION_BET.equals(decision.decision)) {
            learnBetDecision(store, decision, actualDraw, report);
            return;
        }

        if (AppConfig.DECISION_SHADOW.equals(decision.decision)) {
            learnShadowDecision(store, decision, actualDraw, report);
            return;
        }

        learnHoldDecision(store, decision, actualDraw, report);
    }

    public void learnBetDecision(
            DbStore store,
            Models.Decision decision,
            Models.Draw actualDraw,
            LearningReport report) {
        if (store == null || decision == null || actualDraw == null || !actualDraw.isValid()) {
            addError(report, "真实BET学习参数无效");
            return;
        }

        decision.decision = normalizeDecisionText(decision.decision);

        if (!AppConfig.DECISION_BET.equals(decision.decision)) {
            return;
        }

        if (!isRealBetDecisionForLearning(decision)) {
            addSkip(report, "跳过非REAL_TRADE正式BET决策学习：" + normalizeStatScope(decision.statScope));
            return;
        }

        if (!shouldLearnIssue(decision.targetIssue, actualDraw)) {
            addSkip(report, "真实BET目标期与实际开奖不一致");
            return;
        }

        repairDecisionK3(decision);

        if (!isValidLearningDecisionK3(decision)) {
            addError(report, "真实BET位置或K3非法");
            return;
        }

        int actualNumber = actualDraw.getNumberAtPosition(decision.position);

        if (!AppConfig.isValidNumber(actualNumber)) {
            addError(report, "真实BET实际号码非法");
            return;
        }

        boolean hit = strictDecisionK3Hit(decision, actualNumber);
        String missReason = hit ? "" : classifyMissReason(decision, actualDraw);
        CognitiveTrace trace =
                buildDecisionTrace(
                        decision,
                        actualDraw,
                        actualNumber,
                        hit,
                        missReason,
                        AppConfig.STAT_SCOPE_REAL_TRADE);

        if (invokeOptionalObject(
                        shapeMemoryEngine,
                        "learnDecision",
                        new Class[] {DbStore.class, Models.Decision.class, Models.Draw.class},
                        new Object[] {store, decision, actualDraw})
                == NO_METHOD) {
            addSkip(report, "形态记忆learnDecision接口不存在，跳过兼容调用");
        }

        if (invokeOptionalObject(
                        calibrationEngine,
                        "learnRealBet",
                        new Class[] {DbStore.class, Models.Decision.class, Models.Draw.class},
                        new Object[] {store, decision, actualDraw})
                == NO_METHOD) {
            addSkip(report, "真实校准learnRealBet接口不存在，跳过兼容调用");
        }

        if (invokeOptionalObject(
                        probabilityEngine,
                        "learnWeightAfterSettle",
                        new Class[] {DbStore.class, Models.Decision.class, Boolean.TYPE},
                        new Object[] {store, decision, Boolean.valueOf(hit)})
                == NO_METHOD) {
            invokeOptionalObject(
                    probabilityEngine,
                    "learnAfterSettle",
                    new Class[] {DbStore.class, Models.Decision.class, Boolean.TYPE},
                    new Object[] {store, decision, Boolean.valueOf(hit)});
        }

        saveDecisionLearningCase(
                store,
                decision,
                actualDraw,
                actualNumber,
                hit,
                missReason,
                trace,
                "SETTLE_REAL_BET");
        writeRealtimeDecisionProfile(store, decision, actualNumber, hit, trace);
        writeRealBetLearningAudit(store, decision, actualDraw, actualNumber, hit, trace);

        if (report != null) {
            report.realBetLearnCount++;

            if (hit) {
                report.realHitCount++;
                report.lastHitReason = trace.whyActualAppeared;
            } else {
                report.realMissCount++;
                report.lastMissReason = missReason;
                addMissReason(report, missReason);
            }

            report.lastActualNumber = actualNumber;
            report.lastDecision = AppConfig.DECISION_BET;
            applyTraceToReport(report, trace);
        }
    }

    public LearningReport learnTrade(DbStore store, Models.Trade trade, Models.Draw actualDraw) {
        LearningReport report = new LearningReport();
        learnTrade(store, trade, actualDraw, report);

        if (report.detail == null || report.detail.length() == 0) {
            report.detail = buildReportDetail(report);
        }

        if (store != null) {
            saveLearningState(store, report);
            if (report.realBetLearnCount > 0 && actualDraw != null)
                RuntimeEventHooksR926.learned(
                        store, safeText(actualDraw.issue), "LearningEngine.learnTrade", report.detail);
        }

        return report;
    }

    public void learnTrade(
            DbStore store, Models.Trade trade, Models.Draw actualDraw, LearningReport report) {
        if (store == null || trade == null || actualDraw == null || !actualDraw.isValid()) {
            addError(report, "交易学习参数无效");
            return;
        }

        int matrixCells =
                learnTeacherMatrixFromDraw(
                        store,
                        actualDraw,
                        TEACHER_TASK_REALTIME,
                        "TRADE_" + safeText(trade.targetIssue),
                        -1);

        if (matrixCells > 0 && report != null) {
            report.teacherMatrixDrawCount++;
            report.teacherMatrixCellCount += matrixCells;
        }

        if (!isRealBetTradeForLearning(trade)) {
            addSkip(report, "非REAL_TRADE + BET交易不进入真实学习：" + normalizeStatScope(trade.statScope));
            return;
        }

        trade.statScope = AppConfig.STAT_SCOPE_REAL_TRADE;
        trade.action = AppConfig.DECISION_BET;
        trade.result = normalizeResultText(trade.result);

        if (AppConfig.RESULT_DATA_ERROR.equals(trade.result)
                || AppConfig.RESULT_VOID.equals(trade.result)) {
            if (report != null) {
                report.errorCount++;
                report.lastErrorReason = "交易为" + trade.result + "，跳过学习，避免污染模型";
                report.detail = report.lastErrorReason;
            }
            return;
        }

        if (!shouldLearnIssue(trade.targetIssue, actualDraw)) {
            addSkip(report, "交易目标期与实际开奖不一致");
            return;
        }

        repairTradeK3(trade);

        if (!isValidLearningTradeK3(trade)) {
            addError(report, "交易位置或K3非法");
            return;
        }

        int actualNumber = actualDraw.getNumberAtPosition(trade.position);

        if (!AppConfig.isValidNumber(actualNumber)) {
            addError(report, "交易实际号码非法");
            return;
        }

        boolean hit = strictTradeK3Hit(trade, actualNumber);

        if (trade.result == null
                || trade.result.length() == 0
                || AppConfig.RESULT_WAIT.equals(trade.result)) {
            trade.result = hit ? AppConfig.RESULT_WIN : AppConfig.RESULT_MISS;
        }

        if (!AppConfig.RESULT_WIN.equals(trade.result)
                && !AppConfig.RESULT_MISS.equals(trade.result)) {
            addSkip(report, "交易结果不是WIN/MISS，不进入真实学习：" + trade.result);
            return;
        }

        if (!hit && (trade.missReason == null || trade.missReason.length() == 0)) {
            trade.missReason = classifyMissReasonByTrade(trade, actualDraw);
        }

        Models.Decision d = decisionFromTrade(trade);
        CognitiveTrace trace =
                buildDecisionTrace(
                        d,
                        actualDraw,
                        actualNumber,
                        hit,
                        hit ? "" : trade.missReason,
                        AppConfig.STAT_SCOPE_REAL_TRADE);

        invokeOptionalObject(
                shapeMemoryEngine,
                "learnTrade",
                new Class[] {DbStore.class, Models.Trade.class, Models.Draw.class},
                new Object[] {store, trade, actualDraw});

        invokeOptionalObject(
                calibrationEngine,
                "learnTrade",
                new Class[] {DbStore.class, Models.Trade.class, Models.Draw.class},
                new Object[] {store, trade, actualDraw});

        if (invokeOptionalObject(
                        probabilityEngine,
                        "learnWeightAfterSettle",
                        new Class[] {DbStore.class, Models.Decision.class, Boolean.TYPE},
                        new Object[] {store, d, Boolean.valueOf(hit)})
                == NO_METHOD) {
            invokeOptionalObject(
                    probabilityEngine,
                    "learnAfterSettle",
                    new Class[] {DbStore.class, Models.Decision.class, Boolean.TYPE},
                    new Object[] {store, d, Boolean.valueOf(hit)});
        }

        saveDecisionLearningCase(
                store,
                d,
                actualDraw,
                actualNumber,
                hit,
                hit ? "" : trade.missReason,
                trace,
                "SETTLE_TRADE");
        writeRealtimeDecisionProfile(store, d, actualNumber, hit, trace);
        writeRealBetLearningAudit(store, d, actualDraw, actualNumber, hit, trace);

        if (report != null) {
            report.realBetLearnCount++;

            if (hit) {
                report.realHitCount++;
                report.lastHitReason = trace.whyActualAppeared;
            } else {
                report.realMissCount++;
                report.lastMissReason =
                        trade.missReason == null || trade.missReason.length() == 0
                                ? AppConfig.MISS_UNKNOWN
                                : trade.missReason;
                addMissReason(report, report.lastMissReason);
            }

            report.lastActualNumber = actualNumber;
            report.lastDecision = AppConfig.DECISION_BET;
            applyTraceToReport(report, trace);
            report.ok = 1;
            report.detail = buildReportDetail(report);
        }
    }

    private void learnRecentTradesByIssueOptional(
            DbStore store, Models.Draw actualDraw, LearningReport report) {
        if (store == null || actualDraw == null || !actualDraw.isValid()) {
            return;
        }

        Object obj =
                invokeOptionalStore(
                        store,
                        "getTradeByTargetIssue",
                        new Class[] {String.class},
                        new Object[] {actualDraw.issue});

        if (obj instanceof Models.Trade) {
            learnTrade(store, (Models.Trade) obj, actualDraw, report);
            return;
        }

        obj =
                invokeOptionalStore(
                        store,
                        "getTradesByTargetIssue",
                        new Class[] {String.class},
                        new Object[] {actualDraw.issue});

        if (obj instanceof List) {
            try {
                List list = (List) obj;

                for (int i = 0; i < list.size(); i++) {
                    Object x = list.get(i);

                    if (x instanceof Models.Trade) {
                        learnTrade(store, (Models.Trade) x, actualDraw, report);
                    }
                }
            } catch (Throwable e) {
            }
        }
    }

    public void learnShadowDecision(
            DbStore store,
            Models.Decision decision,
            Models.Draw actualDraw,
            LearningReport report) {
        if (store == null || decision == null || actualDraw == null || !actualDraw.isValid()) {
            addError(report, "SHADOW学习参数无效");
            return;
        }

        decision.decision = normalizeDecisionText(decision.decision);

        if (!AppConfig.DECISION_SHADOW.equals(decision.decision)) {
            return;
        }

        decision.statScope = AppConfig.STAT_SCOPE_SHADOW;

        if (!shouldLearnIssue(decision.targetIssue, actualDraw)) {
            addSkip(report, "SHADOW目标期与实际开奖不一致");
            return;
        }

        repairDecisionK3(decision);

        if (!isValidLearningDecisionK3(decision)) {
            addSkip(report, "SHADOW没有有效候选内容");
            return;
        }

        Models.Decision obs = shadowToHoldObservation(decision);

        if (obs == null) {
            addSkip(report, "SHADOW转观察失败");
            return;
        }

        learnHoldObservationInternal(store, obs, actualDraw, report, true);
    }

    public void learnHoldDecision(
            DbStore store,
            Models.Decision decision,
            Models.Draw actualDraw,
            LearningReport report) {
        if (store == null || decision == null || actualDraw == null || !actualDraw.isValid()) {
            addError(report, "HOLD学习参数无效");
            return;
        }

        decision.decision = normalizeDecisionText(decision.decision);

        if (!AppConfig.DECISION_HOLD.equals(decision.decision)) {
            return;
        }

        decision.statScope = AppConfig.STAT_SCOPE_HOLD;

        if (!shouldLearnIssue(decision.targetIssue, actualDraw)) {
            addSkip(report, "HOLD目标期与实际开奖不一致");
            return;
        }

        repairDecisionK3(decision);

        if (isValidLearningDecisionK3(decision)) {
            learnHoldObservationInternal(store, decision, actualDraw, report, false);
        } else {
            addSkip(report, "纯HOLD无候选内容，不进入观察学习");
        }
    }

    private void learnHoldObservationInternal(
            DbStore store,
            Models.Decision decision,
            Models.Draw actualDraw,
            LearningReport report,
            boolean fromShadow) {
        if (store == null || decision == null || actualDraw == null || !actualDraw.isValid()) {
            addError(report, "观察学习参数无效");
            return;
        }

        repairDecisionK3(decision);

        if (!isValidLearningDecisionK3(decision)) {
            addSkip(report, "观察候选位置或K3非法");
            return;
        }

        int actual = actualDraw.getNumberAtPosition(decision.position);

        if (!AppConfig.isValidNumber(actual)) {
            addError(report, "观察学习实际号码非法");
            return;
        }

        boolean hit = strictDecisionK3Hit(decision, actual);
        String pseudoMissReason = hit ? "" : classifyMissReason(decision, actualDraw);

        CognitiveTrace trace =
                buildDecisionTrace(
                        decision,
                        actualDraw,
                        actual,
                        hit,
                        pseudoMissReason,
                        fromShadow ? AppConfig.STAT_SCOPE_SHADOW : AppConfig.STAT_SCOPE_HOLD);

        invokeOptionalObject(
                shapeMemoryEngine,
                "learnHoldObservation",
                new Class[] {DbStore.class, Models.Decision.class, Models.Draw.class},
                new Object[] {store, decision, actualDraw});

        invokeOptionalObject(
                calibrationEngine,
                "learnHoldObservation",
                new Class[] {DbStore.class, Models.Decision.class, Models.Draw.class},
                new Object[] {store, decision, actualDraw});

        saveDecisionLearningCase(
                store,
                decision,
                actualDraw,
                actual,
                hit,
                pseudoMissReason,
                trace,
                fromShadow ? "SETTLE_SHADOW_OBSERVE" : "SETTLE_HOLD_OBSERVE");

        writeRealtimeDecisionProfile(store, decision, actual, hit, trace);
        writeObservationAudit(
                store,
                decision,
                actualDraw,
                actual,
                hit,
                trace,
                fromShadow ? AppConfig.STAT_SCOPE_SHADOW : AppConfig.STAT_SCOPE_HOLD);

        if (report != null) {
            if (fromShadow) {
                report.shadowLearnCount++;

                if (hit) {
                    report.shadowHitCount++;
                } else {
                    report.shadowMissCount++;
                }

                report.lastDecision = AppConfig.DECISION_SHADOW;
            } else {
                report.holdObservationCount++;
                report.lastDecision = AppConfig.DECISION_HOLD;
            }

            report.lastActualNumber = actual;
            applyTraceToReport(report, trace);
        }
    }

    public void learnShadowCandidates(
            DbStore store,
            List<Models.ComboCandidate> candidates,
            Models.Draw actualDraw,
            LearningReport report) {
        if (store == null
                || candidates == null
                || candidates.size() == 0
                || actualDraw == null
                || !actualDraw.isValid()) {
            return;
        }

        int limit = Math.min(candidates.size(), AppConfig.SHADOW_SAVE_TOP_N);
        int learned = 0;
        int reasonSample = 0;
        int hitInTop = 0;
        int missInTop = 0;
        String bestShadowText = "";

        for (int i = 0; i < limit; i++) {
            Models.ComboCandidate c = candidates.get(i);

            if (c == null || !c.isValidK3()) {
                continue;
            }

            repairCandidateK3(c);

            if (!c.isValidK3()) {
                continue;
            }

            if (!shouldLearnIssue(c.targetIssue, actualDraw)) {
                continue;
            }

            int actual = actualDraw.getNumberAtPosition(c.position);

            if (!AppConfig.isValidNumber(actual)) {
                continue;
            }

            boolean hit = strictCandidateK3Hit(c, actual);

            learnCandidateMemory(store, c, hit);
            learnCandidateCalibration(store, c, hit);

            if (hit) {
                hitInTop++;
            } else {
                missInTop++;
            }

            if (report != null) {
                if (hit) {
                    report.shadowHitCount++;
                } else {
                    report.shadowMissCount++;
                }
            }

            if (reasonSample < SHADOW_REASON_SAMPLE_LIMIT) {
                Models.Decision obs = candidateToHoldObservation(c);

                if (obs != null) {
                    String missReason = hit ? "" : classifyMissReason(obs, actualDraw);
                    CognitiveTrace trace =
                            buildDecisionTrace(
                                    obs,
                                    actualDraw,
                                    actual,
                                    hit,
                                    missReason,
                                    AppConfig.STAT_SCOPE_SHADOW);

                    saveDecisionLearningCase(
                            store,
                            obs,
                            actualDraw,
                            actual,
                            hit,
                            missReason,
                            trace,
                            "SETTLE_SHADOW_CANDIDATE");
                    writeRealtimeDecisionProfile(store, obs, actual, hit, trace);
                    writeObservationAudit(
                            store,
                            obs,
                            actualDraw,
                            actual,
                            hit,
                            trace,
                            AppConfig.STAT_SCOPE_SHADOW);

                    if (bestShadowText.length() == 0 || hit) {
                        bestShadowText = trace.summary;
                    }

                    if (report != null && (hit || report.lastTraceSummary.length() == 0)) {
                        applyTraceToReport(report, trace);
                    }
                }

                reasonSample++;
            }

            learned++;
        }

        if (report != null) {
            report.shadowLearnCount += learned;
            report.shadowTopHitCount += hitInTop;
            report.shadowTopMissCount += missInTop;

            if (bestShadowText.length() > 0) {
                report.shadowReasonSummary = safeLongText(bestShadowText);
            }

            if (learned > 0) {
                report.lastDecision = AppConfig.DECISION_SHADOW;
            }
        }
    }

    public void learnBreakRecovery(
            DbStore store,
            Models.ComboCandidate candidate,
            BreakRecoveryEngine.RecoveryResult recovery,
            Models.Draw actualDraw,
            LearningReport report) {
        if (store == null
                || candidate == null
                || recovery == null
                || actualDraw == null
                || !actualDraw.isValid()) {
            addError(report, "断点恢复学习参数无效");
            return;
        }

        repairCandidateK3(candidate);

        if (!candidate.isValidK3()) {
            addSkip(report, "断点恢复候选非法");
            return;
        }

        if (!shouldLearnIssue(candidate.targetIssue, actualDraw)) {
            addSkip(report, "断点恢复目标期与实际开奖不一致");
            return;
        }

        int actual = actualDraw.getNumberAtPosition(candidate.position);

        if (!AppConfig.isValidNumber(actual)) {
            addError(report, "断点恢复实际号码非法");
            return;
        }

        Object r =
                invokeOptionalObject(
                        breakRecoveryEngine,
                        "learnAfterSettle",
                        new Class[] {
                            DbStore.class,
                            Models.ComboCandidate.class,
                            BreakRecoveryEngine.RecoveryResult.class,
                            Integer.TYPE
                        },
                        new Object[] {store, candidate, recovery, Integer.valueOf(actual)});

        if (r == NO_METHOD) {
            addSkip(report, "断点恢复learnAfterSettle接口不存在，跳过兼容调用");
        } else if (report != null) {
            report.breakRecoveryLearnCount++;
        }
    }

    public LearningReport learnShadowByStoredIssue(
            DbStore store, String targetIssue, Models.Draw actualDraw) {
        LearningReport report = new LearningReport();

        if (store == null
                || targetIssue == null
                || targetIssue.length() == 0
                || actualDraw == null
                || !actualDraw.isValid()) {
            report.ok = 0;
            report.detail = "存储影子学习失败：参数无效";
            return report;
        }

        if (!dataGuard.isSameIssue(targetIssue, actualDraw.issue)) {
            report.ok = 0;
            report.actualIssue = actualDraw.issue == null ? "" : actualDraw.issue;
            report.detail = "存储影子学习跳过：目标期与实际期不一致";
            saveLearningState(store, report);
            return report;
        }

        report.actualIssue = actualDraw.issue == null ? "" : actualDraw.issue;
        report.learnedAt = System.currentTimeMillis();

        learnShadowByStoredIssueInternal(store, targetIssue, actualDraw, report);

        report.ok = 1;
        report.detail = buildReportDetail(report);
        saveLearningState(store, report);
        return report;
    }

    private void learnShadowByStoredIssueInternal(
            DbStore store, String targetIssue, Models.Draw actualDraw, LearningReport report) {
        if (store == null
                || targetIssue == null
                || targetIssue.length() == 0
                || actualDraw == null
                || !actualDraw.isValid()) {
            return;
        }

        List<Models.ComboCandidate> list = null;

        try {
            list = store.getShadowCandidates(targetIssue, AppConfig.SHADOW_SAVE_TOP_N);
        } catch (Throwable e) {
            list = null;
            addError(report, "读取存储SHADOW候选异常");
        }

        if (list == null || list.size() == 0) {
            return;
        }

        learnShadowCandidates(store, list, actualDraw, report);

        try {
            store.markShadowLearned(targetIssue);
        } catch (Throwable e) {
            addError(report, "标记SHADOW已学习异常");
        }
    }

    private void learnCandidateMemory(DbStore store, Models.ComboCandidate c, boolean hit) {
        if (store == null || c == null || !c.isValidK3()) {
            return;
        }

        if (c.shapeKey == null || c.shapeKey.length() == 0) {
            return;
        }

        boolean fail = !hit;
        boolean reverse = isReverseLike(c.stateMode, c.driftRisk, c.breakDepth, c.reasonSummary);

        try {
            store.updateShapeMemory(c.shapeKey, c.position, hit, fail, reverse);
        } catch (Throwable e) {
        }

        try {
            store.updateComboMemory(c.shapeKey, c.position, c.k3Mask, hit, c.comboGap, c.trendLife);
        } catch (Throwable e) {
        }
    }

    private void learnCandidateCalibration(DbStore store, Models.ComboCandidate c, boolean hit) {
        if (store == null || c == null) {
            return;
        }

        double p = c.finalP > 0.0d ? c.finalP : c.rawP;

        if (p <= 0.0d) {
            return;
        }

        Object r;

        r =
                invokeOptionalStore(
                        store,
                        "updateShadowCandidateCalibrationAudit",
                        new Class[] {Double.TYPE, Boolean.TYPE},
                        new Object[] {Double.valueOf(p), Boolean.valueOf(hit)});

        if (r != NO_METHOD) {
            return;
        }

        r =
                invokeOptionalStore(
                        store,
                        "updateTeacherProbabilityAudit",
                        new Class[] {Double.TYPE, Boolean.TYPE},
                        new Object[] {Double.valueOf(p), Boolean.valueOf(hit)});

        if (r != NO_METHOD) {
            return;
        }

        r =
                invokeOptionalStore(
                        store,
                        "updateTeacherMatrixProbabilityAudit",
                        new Class[] {Double.TYPE, Boolean.TYPE},
                        new Object[] {Double.valueOf(p), Boolean.valueOf(hit)});

        if (r != NO_METHOD) {
            return;
        }

        invokeOptionalStore(
                store,
                "updateProbabilityAuditCache",
                new Class[] {Double.TYPE, Boolean.TYPE},
                new Object[] {Double.valueOf(p), Boolean.valueOf(hit)});
    }

    private void writeRealtimeDecisionProfile(
            DbStore store, Models.Decision d, int actual, boolean hit, CognitiveTrace trace) {
        if (store == null || d == null || trace == null) {
            return;
        }

        String stateKey = trace.stateKey == null ? "" : trace.stateKey;

        if (stateKey.length() == 0) {
            return;
        }

        double score = d.profileScore > 0.0d ? d.profileScore : normalizeScore01(d.finalP);
        double risk = normalizeScore01(Math.max(d.driftRisk, d.breakDepth));
        int sample = d.effectiveSample > 0 ? d.effectiveSample : d.comboSample;

        invokeProfileUpdate(store, stateKey, hit, score, risk, sample);
        invokeNumberProfileUpdate(store, stateKey, d.position, actual, true, score, risk, sample);

        int[] nums = new int[] {d.n1, d.n2, d.n3};

        for (int i = 0; i < nums.length; i++) {
            int n = nums[i];

            if (!AppConfig.isValidNumber(n)) {
                continue;
            }

            invokeNumberProfileUpdate(
                    store, stateKey, d.position, n, n == actual, score, risk, sample);
        }
    }

    private void writeRealBetLearningAudit(
            DbStore store,
            Models.Decision d,
            Models.Draw actualDraw,
            int actual,
            boolean hit,
            CognitiveTrace trace) {
        if (store == null || d == null || actualDraw == null) {
            return;
        }

        try {
            store.setState(
                    "learning_last_real_issue", actualDraw.issue == null ? "" : actualDraw.issue);
            store.setState("learning_last_real_position", String.valueOf(d.position));
            store.setState("learning_last_real_numbers", formatK3(d.n1, d.n2, d.n3));
            store.setState("learning_last_real_actual", formatNumber(actual));
            store.setState(
                    "learning_last_real_result",
                    hit ? AppConfig.RESULT_WIN : AppConfig.RESULT_MISS);
            store.setState("learning_last_real_trace", trace == null ? "" : trace.summary);
            store.setState(
                    "learning_real_rule",
                    "ONLY_REAL_TRADE_BET_TARGET_MATCH_STRICT_POSITION_K3_WIN_MISS");
        } catch (Throwable e) {
        }
    }

    private void writeObservationAudit(
            DbStore store,
            Models.Decision d,
            Models.Draw actualDraw,
            int actual,
            boolean hit,
            CognitiveTrace trace,
            String scope) {
        if (store == null || d == null || actualDraw == null) {
            return;
        }

        try {
            store.setState(
                    "learning_last_observe_issue",
                    actualDraw.issue == null ? "" : actualDraw.issue);
            store.setState("learning_last_observe_scope", scope == null ? "" : scope);
            store.setState("learning_last_observe_position", String.valueOf(d.position));
            store.setState("learning_last_observe_numbers", formatK3(d.n1, d.n2, d.n3));
            store.setState("learning_last_observe_actual", formatNumber(actual));
            store.setState("learning_last_observe_result", hit ? "OBSERVE_HIT" : "OBSERVE_MISS");
            store.setState("learning_last_observe_trace", trace == null ? "" : trace.summary);
            store.setState(
                    "learning_observe_rule", "SHADOW_HOLD_ONLY_PROFILE_AUDIT_NO_REAL_CALIBRATION");
        } catch (Throwable e) {
        }
    }

    private void saveDecisionLearningCase(
            DbStore store,
            Models.Decision d,
            Models.Draw actualDraw,
            int actual,
            boolean hit,
            String missReason,
            CognitiveTrace trace,
            String taskKey) {
        if (store == null || d == null || actualDraw == null || !actualDraw.isValid()) {
            return;
        }

        repairDecisionK3(d);

        if (!isValidLearningDecisionK3(d)) {
            return;
        }

        Models.LearningCase c = new Models.LearningCase();

        String targetIssue = actualDraw.issue == null ? "" : actualDraw.issue;
        String stateKey = trace == null ? buildStateKey(d) : trace.stateKey;

        c.caseKey = taskKey + "|" + targetIssue + "|P" + d.position + "|K" + d.k3Mask;
        c.taskKey = taskKey;
        c.segmentKey = "REALTIME_SETTLE";
        c.baseIssue = d.baseIssue == null ? "" : d.baseIssue;
        c.targetIssue = targetIssue;
        c.createdAt = System.currentTimeMillis();
        c.targetIndex = -1;
        c.position = d.position;
        c.actualNumber = actual;
        c.predictedK3 = d.k3Mask;
        c.k3Mask = d.k3Mask;
        c.hit = hit ? 1 : 0;

        c.stateKey = stateKey;
        c.stateMode = d.stateMode == null ? "" : d.stateMode;
        c.trendKey = bucketKey("TR", normalizeScore01(d.trendLife));
        c.shapeKey = d.shapeKey == null ? "" : d.shapeKey;
        c.driftKey = bucketKey("DR", normalizeScore01(d.driftRisk));
        c.breakKey = bucketKey("BR", normalizeScore01(d.breakDepth));
        c.recoveryKey = bucketKey("RC", normalizeScore01(d.recoveryScore));
        c.transitionKey = d.probabilityMode == null ? "" : d.probabilityMode;

        c.expectedShape = trace == null ? buildExpectedNumberShape(d) : trace.expectedShape;
        c.actualShape = trace == null ? buildActualNumberShape(d, actual) : trace.actualShape;
        c.whyExpectedHit = "结算后学习：保存当期决策状态、K3、概率、门槛、错因，用于后台画像反哺下一次实时分析。";
        c.whyActualHit = trace == null ? "" : trace.whyActualAppeared;
        c.whyMiss = hit ? "" : (missReason == null ? AppConfig.MISS_UNKNOWN : missReason);
        c.nextAdvice = trace == null ? "" : trace.nextAdvice;
        c.mainReasonCode = hit ? "REAL_HIT" : c.whyMiss;
        c.affectedModule =
                AppConfig.STAT_SCOPE_REAL_TRADE.equals(d.statScope) ? "REAL_BET" : d.statScope;

        c.finalP = d.finalP;
        c.dynamicGate = d.dynamicGate;
        c.edge = d.edge == 0.0d ? d.finalP - d.dynamicGate : d.edge;
        c.trendScore = normalizeScore01(d.trendLife);
        c.shapeScore = normalizeScore01(d.deepScore);
        c.driftRisk = normalizeScore01(d.driftRisk);
        c.breakRisk = normalizeScore01(d.breakDepth);
        c.recoveryScore = normalizeScore01(d.recoveryScore);
        c.profileScore = d.profileScore;
        c.numberBias = d.biasScore;
        c.comboBias = d.comboGap;
        c.riskScore = normalizeScore01(Math.max(d.driftRisk, d.breakDepth));
        c.teacherWeight = d.aiJudgeConfidence > 0.0d ? d.aiJudgeConfidence : 1.0d;
        c.stateConfidence = d.deepConfidence;
        c.stateSample = d.effectiveSample;
        c.cacheSample = d.comboSample;
        c.recentSample = d.deepProfileSample;
        c.recentHitRate = d.deepProfileHitRate;

        try {
            store.saveLearningCase(c);
        } catch (Throwable e) {
        }
    }

    private void saveAutoLearnCursor(
            DbStore store,
            int startIndex,
            int endIndex,
            int nextIndex,
            String lastIssue,
            String status,
            String progress) {
        if (store == null) {
            return;
        }

        Models.AutoLearnTask t = new Models.AutoLearnTask();

        t.taskKey = AUTO_TASK_KEY;
        t.taskType = AUTO_TASK_TYPE;
        t.windowSize = AUTO_LEARN_MIN_TRAIN;
        t.segmentStartIndex = startIndex;
        t.segmentEndIndex = endIndex;
        t.nextTargetIndex = nextIndex;
        t.lastStartIndex = startIndex;
        t.randomSeed = System.currentTimeMillis();
        t.lastIssue = lastIssue == null ? "" : lastIssue;
        t.progress = progress == null ? "" : progress;
        t.status = status == null || status.length() == 0 ? AUTO_STATUS_IDLE : status;
        t.usedSegments = String.valueOf(startIndex) + "-" + String.valueOf(endIndex);
        t.updatedAt = System.currentTimeMillis();

        try {
            store.saveAutoLearnCursor(t);
        } catch (Throwable e) {
        }
    }

    private void saveAutoLearnState(
            DbStore store,
            LearningReport report,
            int startIndex,
            int endIndex,
            String status,
            String text) {
        if (store == null) {
            return;
        }

        try {
            store.setState("auto_learn_status", status == null ? "" : status);
            store.setState("auto_learn_running", AUTO_STATUS_RUNNING.equals(status) ? "1" : "0");
            store.setState("auto_learn_task_key", AUTO_TASK_KEY);
            store.setState("auto_learn_progress", text == null ? "" : text);
            store.setState("auto_learn_summary", text == null ? "" : text);
            store.setState("auto_learn_range", startIndex + "-" + endIndex);
            store.setState("auto_learn_updated_at", String.valueOf(System.currentTimeMillis()));
        } catch (Throwable e) {
        }
    }

    /* =========================
     * 未中原因分析
     * ========================= */

    public String classifyMissReason(Models.Decision decision, Models.Draw actualDraw) {
        if (decision == null || actualDraw == null || !actualDraw.isValid()) {
            return AppConfig.MISS_UNKNOWN;
        }

        repairDecisionK3(decision);

        if (!isValidLearningDecisionK3(decision)) {
            return AppConfig.MISS_DATA_OFFSET;
        }

        int actual = actualDraw.getNumberAtPosition(decision.position);

        if (!AppConfig.isValidNumber(actual)) {
            return AppConfig.MISS_DATA_OFFSET;
        }

        if (strictDecisionK3Hit(decision, actual)) {
            return "";
        }

        if (isActualNearPredictedK3(decision, actual)) {
            if (decision.comboGap < AppConfig.COMBO_GAP_MIN + 0.012d) {
                return AppConfig.MISS_THIRD_WEAK;
            }

            return AppConfig.MISS_K3_RANK_4_6;
        }

        int rank = rankActualByDecisionProbability(decision, actual);

        if (rank >= 4 && rank <= 6) {
            return AppConfig.MISS_K3_RANK_4_6;
        }

        if (AppConfig.MODE_BROKEN.equals(decision.stateMode)
                || AppConfig.MODE_REVERSING.equals(decision.stateMode)
                || "REVERSAL".equals(decision.stateMode)
                || decision.breakDepth >= AppConfig.BREAK_DEPTH_MAX + 8.0d) {
            return AppConfig.MISS_TREND_BROKEN;
        }

        if (decision.driftRisk >= AppConfig.DRIFT_RISK_HARD_BLOCK) {
            return AppConfig.MISS_DRIFT_SPIKE;
        }

        if (decision.hotReverseRisk >= AppConfig.HOT_REVERSE_RISK_MAX + 8.0d) {
            return AppConfig.MISS_HOT_REVERSE;
        }

        if (decision.falseStrongRisk >= AppConfig.FALSE_STRONG_RISK_MAX + 10.0d) {
            return AppConfig.MISS_HOT_REVERSE;
        }

        if (decision.recoveryScore < AppConfig.RECOVERY_SCORE_MIN
                && (AppConfig.MODE_RECOVERING.equals(decision.stateMode)
                        || AppConfig.MODE_PULLBACK.equals(decision.stateMode))) {
            return AppConfig.MISS_FALSE_RECOVERY;
        }

        if (decision.effectiveSample > 0
                && decision.effectiveSample < AppConfig.EFFECTIVE_SAMPLE_MIN) {
            return AppConfig.MISS_SAMPLE_WEAK;
        }

        if (decision.comboSample > 0 && decision.comboSample < AppConfig.COMBO_SAMPLE_MIN) {
            return AppConfig.MISS_SAMPLE_WEAK;
        }

        if (decision.comboGap < AppConfig.COMBO_GAP_MIN) {
            return AppConfig.MISS_THIRD_WEAK;
        }

        if (decision.championGap < AppConfig.CHAMPION_GAP_MIN) {
            return AppConfig.MISS_POSITION_LOST;
        }

        if (!selectedCoversActualZone(decision, actual)) {
            return AppConfig.MISS_THIRD_WEAK;
        }

        if (decision.reasonSummary != null && decision.reasonSummary.length() > 0) {
            if (containsBoth(decision.reasonSummary, "Markov", "不足")
                    || containsBoth(decision.reasonSummary, "马尔可夫", "不足")) {
                return AppConfig.MISS_MARKOV_FAILED;
            }

            if (containsBoth(decision.reasonSummary, "样本", "不足")) {
                return AppConfig.MISS_SAMPLE_WEAK;
            }

            if (decision.reasonSummary.indexOf("破位") >= 0
                    || decision.reasonSummary.indexOf("断点") >= 0) {
                return AppConfig.MISS_TREND_BROKEN;
            }

            if (decision.reasonSummary.indexOf("漂移") >= 0
                    && decision.reasonSummary.indexOf("高") >= 0) {
                return AppConfig.MISS_DRIFT_SPIKE;
            }

            if (decision.reasonSummary.indexOf("反杀") >= 0
                    || decision.reasonSummary.indexOf("反转") >= 0) {
                return AppConfig.MISS_HOT_REVERSE;
            }

            if (decision.reasonSummary.indexOf("假恢复") >= 0) {
                return AppConfig.MISS_FALSE_RECOVERY;
            }
        }

        return AppConfig.MISS_UNKNOWN;
    }

    public String classifyMissReasonByTrade(Models.Trade trade, Models.Draw actualDraw) {
        if (trade == null || actualDraw == null || !actualDraw.isValid()) {
            return AppConfig.MISS_UNKNOWN;
        }

        repairTradeK3(trade);

        if (!isValidLearningTradeK3(trade)) {
            return AppConfig.MISS_DATA_OFFSET;
        }

        int actual = actualDraw.getNumberAtPosition(trade.position);

        if (!AppConfig.isValidNumber(actual)) {
            return AppConfig.MISS_DATA_OFFSET;
        }

        if (strictTradeK3Hit(trade, actual)) {
            return "";
        }

        if (trade.missReason != null && trade.missReason.length() > 0) {
            return trade.missReason;
        }

        Models.Decision d = decisionFromTrade(trade);
        return classifyMissReason(d, actualDraw);
    }

    public int rankActualByDecisionProbability(Models.Decision decision, int actualNumber) {
        if (decision == null || !AppConfig.isValidNumber(actualNumber)) {
            return 0;
        }

        repairDecisionK3(decision);

        if (strictDecisionK3Hit(decision, actualNumber)) {
            return 1;
        }

        if (isActualNearPredictedK3(decision, actualNumber)) {
            return 4;
        }

        if (decision.finalP >= decision.dynamicGate
                && decision.comboGap < AppConfig.COMBO_GAP_MIN + 0.010d) {
            return 4;
        }

        if (decision.finalP >= AppConfig.TARGET_HIT_RATE_IDEAL
                && !selectedCoversActualZone(decision, actualNumber)) {
            return 5;
        }

        return 7;
    }

    /* =========================
     * 对象转换
     * ========================= */

    public Models.Decision shadowToHoldObservation(Models.Decision shadow) {
        if (shadow == null) {
            return null;
        }

        Models.Decision d = shadow.copy();
        d.decision = AppConfig.DECISION_HOLD;
        d.action = AppConfig.DECISION_HOLD;
        d.blockCode = AppConfig.DECISION_SHADOW;
        d.statScope = AppConfig.STAT_SCOPE_SHADOW;
        d.reasonSummary = appendReason(d.reasonSummary, "SHADOW转HOLD观察学习，不计真实胜率、不写真实校准");
        return d;
    }

    public Models.Decision candidateToHoldObservation(Models.ComboCandidate c) {
        if (c == null || !c.isValidK3()) {
            return null;
        }

        Models.Decision d = new Models.Decision();
        d.baseIssue = c.baseIssue == null ? "" : c.baseIssue;
        d.targetIssue = c.targetIssue == null ? "" : c.targetIssue;
        d.createdAt = System.currentTimeMillis();
        d.decision = AppConfig.DECISION_HOLD;
        d.action = AppConfig.DECISION_HOLD;
        d.statScope = AppConfig.STAT_SCOPE_SHADOW;
        d.position = c.position;
        d.setNumbers(c.n1, c.n2, c.n3);
        d.k3Mask = Models.buildK3Mask(c.n1, c.n2, c.n3);

        d.rawP = c.rawP;
        d.calibratedP = c.calibratedP;
        d.finalP = c.finalP;
        d.dynamicGate = c.dynamicGate;
        d.edge = c.edge;

        d.modelP = c.modelP;
        d.shapeP = c.shapeP;
        d.comboP = c.comboP;
        d.recoveryP = c.recoveryP;
        d.similarP = c.similarP;
        d.memoryP = c.memoryP;

        d.similarHitRate = c.similarHitRate;
        d.comboHitRate = c.comboHitRate;
        d.effectiveSample = c.effectiveSample;
        d.comboSample = c.comboSample;

        d.trendLife = c.trendLife;
        d.driftRisk = c.driftRisk;
        d.breakDepth = c.breakDepth;
        d.recoveryScore = c.recoveryScore;
        d.championGap = c.championGap;
        d.comboGap = c.comboGap;
        d.hotReverseRisk = c.hotReverseRisk;
        d.falseStrongRisk = c.falseStrongRisk;

        d.calibrationError = c.calibrationError;
        d.probabilityMode = c.probabilityMode;
        d.backtestMode = c.backtestMode;

        d.deepScore = c.deepScore;
        d.deepConfidence = c.deepConfidence;
        d.deepPositionScore = c.deepPositionScore;
        d.deepNumberScore = c.deepNumberScore;
        d.deepCycleScore = c.deepCycleScore;
        d.deepTransitionScore = c.deepTransitionScore;
        d.deepStateScore = c.deepStateScore;
        d.biasScore = c.biasScore;
        d.biasConfidence = c.biasConfidence;
        d.profileScore = c.profileScore;
        d.fallbackProfileScore = c.fallbackProfileScore;
        d.auditPenalty = c.auditPenalty;
        d.auditBoost = c.auditBoost;
        d.aiJudgeScore = c.aiJudgeScore;
        d.aiJudgeConfidence = c.aiJudgeConfidence;
        d.evidenceScore = c.evidenceScore;
        d.evidenceRisk = c.evidenceRisk;
        d.aPlusScore = c.aPlusScore;
        d.promotionScore = c.promotionScore;
        d.weakestNumberScore = c.weakestNumberScore;
        d.balanceScore = c.balanceScore;

        d.stateMode = c.stateMode == null ? AppConfig.MODE_CHAOS : c.stateMode;
        d.shapeKey = c.shapeKey == null ? "" : c.shapeKey;
        d.deepProfileKey = c.deepProfileKey == null ? "" : c.deepProfileKey;
        d.fallbackProfileKey = c.fallbackProfileKey == null ? "" : c.fallbackProfileKey;
        d.candidateEvidenceKey = c.candidateEvidenceKey == null ? "" : c.candidateEvidenceKey;
        d.promotionStatus = c.promotionStatus == null ? "" : c.promotionStatus;
        d.aPlusStatus = c.aPlusStatus == null ? "" : c.aPlusStatus;
        d.blockCode = AppConfig.DECISION_SHADOW;
        d.reasonSummary = appendReason(c.reasonSummary, "候选转HOLD观察学习，不计真实胜率、不写真实校准");

        return d;
    }

    private Models.Decision decisionFromTrade(Models.Trade trade) {
        Models.Decision d = new Models.Decision();

        if (trade == null) {
            return d;
        }

        d.baseIssue = trade.baseIssue == null ? "" : trade.baseIssue;
        d.targetIssue = trade.targetIssue == null ? "" : trade.targetIssue;
        d.createdAt = trade.createdAt;
        d.decision = AppConfig.DECISION_BET;
        d.action = AppConfig.DECISION_BET;
        d.statScope = AppConfig.STAT_SCOPE_REAL_TRADE;

        d.position = trade.position;
        d.setNumbers(trade.n1, trade.n2, trade.n3);
        d.k3Mask = Models.buildK3Mask(trade.n1, trade.n2, trade.n3);

        d.rawP = trade.rawP;
        d.calibratedP = trade.calibratedP;
        d.finalP = trade.finalP;
        d.dynamicGate = trade.dynamicGate;
        d.edge = trade.edge;

        d.modelP = trade.modelP;
        d.shapeP = trade.shapeP;
        d.comboP = trade.comboP;
        d.recoveryP = trade.recoveryP;
        d.similarP = trade.similarP;
        d.memoryP = trade.memoryP;
        d.calibrationError = trade.calibrationError;

        d.similarHitRate = trade.similarHitRate;
        d.comboHitRate = trade.comboHitRate;
        d.effectiveSample = trade.effectiveSample;
        d.comboSample = trade.comboSample;

        d.trendLife = trade.trendLife;
        d.driftRisk = trade.driftRisk;
        d.breakDepth = trade.breakDepth;
        d.recoveryScore = trade.recoveryScore;
        d.championGap = trade.championGap;
        d.comboGap = trade.comboGap;
        d.hotReverseRisk = trade.hotReverseRisk;
        d.falseStrongRisk = trade.falseStrongRisk;

        d.deepScore = trade.deepScore;
        d.deepConfidence = trade.deepConfidence;
        d.deepFeatureP = trade.deepFeatureP;
        d.deepPositionScore = trade.deepPositionScore;
        d.deepNumberScore = trade.deepNumberScore;
        d.deepCycleScore = trade.deepCycleScore;
        d.deepTransitionScore = trade.deepTransitionScore;
        d.deepStateScore = trade.deepStateScore;
        d.aiJudgeScore = trade.aiJudgeScore;
        d.aiJudgeConfidence = trade.aiJudgeConfidence;
        d.evidenceScore = trade.evidenceScore;
        d.evidenceRisk = trade.evidenceRisk;
        d.biasScore = trade.biasScore;
        d.biasConfidence = trade.biasConfidence;
        d.auditPenalty = trade.auditPenalty;
        d.auditBoost = trade.auditBoost;
        d.profileScore = trade.profileScore;
        d.fallbackProfileScore = trade.fallbackProfileScore;
        d.aPlusScore = trade.aPlusScore;
        d.promotionScore = trade.promotionScore;
        d.conflictLevel = trade.conflictLevel;
        d.weakestNumberScore = trade.weakestNumberScore;
        d.balanceScore = trade.balanceScore;

        d.deepProfileKey = trade.deepProfileKey == null ? "" : trade.deepProfileKey;
        d.fallbackProfileKey = trade.fallbackProfileKey == null ? "" : trade.fallbackProfileKey;
        d.candidateEvidenceKey =
                trade.candidateEvidenceKey == null ? "" : trade.candidateEvidenceKey;
        d.promotionStatus = trade.promotionStatus == null ? "" : trade.promotionStatus;
        d.aPlusStatus = trade.aPlusStatus == null ? "" : trade.aPlusStatus;
        d.snapshotId = trade.snapshotId == null ? "" : trade.snapshotId;

        d.stateMode = trade.stateMode == null ? AppConfig.MODE_CHAOS : trade.stateMode;
        d.shapeKey = trade.shapeKey == null ? "" : trade.shapeKey;
        d.reasonSummary = trade.reasonSummary == null ? "" : trade.reasonSummary;
        d.result = normalizeResultText(trade.result);
        d.settled = trade.settled;

        copySameFieldIfExists(trade, d, "teacherMatrixSample");
        copySameFieldIfExists(trade, d, "teacherMatrixHitRate");
        copySameFieldIfExists(trade, d, "teacherMatrixConfidence");
        copySameFieldIfExists(trade, d, "teacherMatrixProbAdjust");
        copySameFieldIfExists(trade, d, "teacherMatrixGateAdjust");
        copySameFieldIfExists(trade, d, "teacherMatrixRiskPenalty");
        copySameFieldIfExists(trade, d, "teacherMatrixApplied");
        copySameFieldIfExists(trade, d, "teacherMatrixStatus");
        copySameFieldIfExists(trade, d, "teacherMatrixProfileKey");
        copySameFieldIfExists(trade, d, "teacherMatrixComboKey");
        copySameFieldIfExists(trade, d, "teacherMatrixSummary");

        return d;
    }

    public boolean shouldLearnIssue(String targetIssue, Models.Draw actualDraw) {
        if (targetIssue == null
                || targetIssue.length() == 0
                || actualDraw == null
                || !actualDraw.isValid()) {
            return false;
        }

        return dataGuard.isSameIssue(targetIssue, actualDraw.issue);
    }

    /* =========================
     * 报告与状态
     * ========================= */

    public String buildReportDetail(LearningReport report) {
        if (report == null) {
            return "学习报告为空";
        }

        report.recalc();

        StringBuilder sb = new StringBuilder();

        sb.append("学习完成：");
        sb.append("真实BET学习=");
        sb.append(report.realBetLearnCount);
        sb.append("，命中=");
        sb.append(report.realHitCount);
        sb.append("，未中=");
        sb.append(report.realMissCount);

        if (report.realBetLearnCount > 0) {
            sb.append("，真实学习胜率=");
            sb.append(UiFormatter.probability(report.realLearnHitRate));
        }

        sb.append("，SHADOW学习=");
        sb.append(report.shadowLearnCount);

        if (report.shadowLearnCount > 0) {
            sb.append("(中");
            sb.append(report.shadowHitCount);
            sb.append("/错");
            sb.append(report.shadowMissCount);
            sb.append(")");
        }

        sb.append("，HOLD观察=");
        sb.append(report.holdObservationCount);
        sb.append("，断点恢复学习=");
        sb.append(report.breakRecoveryLearnCount);

        if (report.teacherMatrixCellCount > 0) {
            sb.append("；教师矩阵=");
            sb.append(report.teacherMatrixDrawCount);
            sb.append("期/");
            sb.append(report.teacherMatrixCellCount);
            sb.append("格");
            sb.append("，口径=每期10位置×10号码正负样本");
        }

        if (report.autoLearnCaseCount > 0) {
            sb.append("；后台小批量=");
            sb.append(report.autoLearnIssueCount);
            sb.append("期/");
            sb.append(report.autoLearnCaseCount);
            sb.append("个位置样本，写入=");
            sb.append(report.autoLearnSavedCount);
            sb.append("，样本HR=");
            sb.append(UiFormatter.probability(report.autoLearnHitRate));
            sb.append("，注意：这是全位置反推覆盖率，不是实盘胜率");
        }

        if (report.stateKey != null && report.stateKey.length() > 0) {
            sb.append("；状态指纹=");
            sb.append(report.stateKey);
        }

        if (report.expectedShape != null && report.expectedShape.length() > 0) {
            sb.append("；预测号码形态=");
            sb.append(report.expectedShape);
        }

        if (report.actualShape != null && report.actualShape.length() > 0) {
            sb.append("；实际号码形态=");
            sb.append(report.actualShape);
        }

        if (report.whyActualAppeared != null && report.whyActualAppeared.length() > 0) {
            sb.append("；实际为什么出=");
            sb.append(report.whyActualAppeared);
        }

        if (report.whyPredictedFailed != null && report.whyPredictedFailed.length() > 0) {
            sb.append("；预测为什么偏=");
            sb.append(report.whyPredictedFailed);
        }

        if (report.nextStateAdvice != null && report.nextStateAdvice.length() > 0) {
            sb.append("；同状态下次修正=");
            sb.append(report.nextStateAdvice);
        }

        if (report.lastMissReason != null && report.lastMissReason.length() > 0) {
            sb.append("；最近未中原因=");
            sb.append(UiFormatter.missReasonText(report.lastMissReason));
        }

        if (report.topMissReason != null && report.topMissReason.length() > 0) {
            sb.append("；主要错因=");
            sb.append(UiFormatter.missReasonText(report.topMissReason));
        }

        if (report.shadowReasonSummary != null && report.shadowReasonSummary.length() > 0) {
            sb.append("；影子候选反推=");
            sb.append(report.shadowReasonSummary);
        }

        if (report.skipCount > 0) {
            sb.append("；跳过=");
            sb.append(report.skipCount);
        }

        if (report.errorCount > 0) {
            sb.append("；错误=");
            sb.append(report.errorCount);
        }

        if (report.lastSkipReason != null && report.lastSkipReason.length() > 0) {
            sb.append("；最近跳过=");
            sb.append(report.lastSkipReason);
        }

        if (report.lastErrorReason != null && report.lastErrorReason.length() > 0) {
            sb.append("；最近错误=");
            sb.append(report.lastErrorReason);
        }

        sb.append("；口径：真实校准/权重只学习REAL_TRADE+BET，HOLD/SHADOW/后台学习/教师矩阵只做画像反哺，不直接救援BET、不降低门槛");

        return safeLongText(sb.toString());
    }

    private CognitiveTrace buildDecisionTrace(
            Models.Decision d,
            Models.Draw actualDraw,
            int actualNumber,
            boolean hit,
            String missReason,
            String scope) {
        CognitiveTrace trace = new CognitiveTrace();

        if (d == null) {
            trace.summary = "无决策对象，无法做状态反推";
            return trace;
        }

        trace.scope = scope == null ? "" : scope;
        trace.hit = hit ? 1 : 0;
        trace.actualNumber = actualNumber;
        trace.missReason = missReason == null ? "" : missReason;
        trace.stateKey = buildStateKey(d);
        trace.expectedShape = buildExpectedNumberShape(d);
        trace.actualShape = buildActualNumberShape(d, actualNumber);
        trace.whyActualAppeared = buildWhyActualAppeared(d, actualNumber, hit);
        trace.whyPredictedFailed = buildWhyPredictedFailed(d, actualNumber, hit, trace.missReason);
        trace.nextAdvice = buildNextStateAdvice(d, actualNumber, hit, trace.missReason);

        StringBuilder sb = new StringBuilder();

        sb.append("状态=");
        sb.append(trace.stateKey);
        sb.append("｜预测=");
        sb.append(formatK3(d.n1, d.n2, d.n3));
        sb.append("｜实际=");
        sb.append(formatNumber(actualNumber));
        sb.append("｜");
        sb.append(hit ? "命中" : "未中");

        if (trace.expectedShape.length() > 0) {
            sb.append("｜预测形态=");
            sb.append(trace.expectedShape);
        }

        if (trace.actualShape.length() > 0) {
            sb.append("｜实际形态=");
            sb.append(trace.actualShape);
        }

        if (trace.whyActualAppeared.length() > 0) {
            sb.append("｜实际原因=");
            sb.append(trace.whyActualAppeared);
        }

        if (!hit && trace.whyPredictedFailed.length() > 0) {
            sb.append("｜偏差原因=");
            sb.append(trace.whyPredictedFailed);
        }

        if (trace.nextAdvice.length() > 0) {
            sb.append("｜修正=");
            sb.append(trace.nextAdvice);
        }

        trace.summary = safeLongText(sb.toString());

        return trace;
    }

    private String buildStateKey(Models.Decision d) {
        if (d == null) {
            return "STATE_EMPTY";
        }

        StringBuilder sb = new StringBuilder();

        sb.append("P");
        sb.append(d.position);
        sb.append("_");
        sb.append(safeMode(d.stateMode));
        sb.append("_TL");
        sb.append(bucketScore(d.trendLife));
        sb.append("_DR");
        sb.append(bucketScore(d.driftRisk));
        sb.append("_BD");
        sb.append(bucketScore(d.breakDepth));
        sb.append("_RC");
        sb.append(bucketScore(d.recoveryScore));
        sb.append("_CG");
        sb.append(bucketProb(d.championGap));
        sb.append("_KG");
        sb.append(bucketProb(d.comboGap));
        sb.append("_FP");
        sb.append(bucketProb(d.finalP));
        sb.append("_G");
        sb.append(bucketProb(d.dynamicGate));

        return sb.toString();
    }

    private String buildExpectedNumberShape(Models.Decision d) {
        if (d == null) {
            return "";
        }

        int low = 0;
        int mid = 0;
        int high = 0;
        int odd = 0;
        int even = 0;
        int min = 99;
        int max = 0;

        int[] nums = new int[] {d.n1, d.n2, d.n3};

        for (int i = 0; i < nums.length; i++) {
            int n = nums[i];

            if (!AppConfig.isValidNumber(n)) {
                continue;
            }

            if (n <= 3) {
                low++;
            } else if (n <= 7) {
                mid++;
            } else {
                high++;
            }

            if (n % 2 == 0) {
                even++;
            } else {
                odd++;
            }

            if (n < min) {
                min = n;
            }

            if (n > max) {
                max = n;
            }
        }

        StringBuilder sb = new StringBuilder();

        sb.append("区间低");
        sb.append(low);
        sb.append("/中");
        sb.append(mid);
        sb.append("/高");
        sb.append(high);
        sb.append("，奇");
        sb.append(odd);
        sb.append("/偶");
        sb.append(even);

        if (min <= max && max > 0) {
            sb.append("，跨度");
            sb.append(max - min);
        }

        if (d.trendLife >= AppConfig.TREND_LIFE_MIN
                && d.driftRisk <= AppConfig.DRIFT_RISK_MAX
                && d.recoveryScore >= AppConfig.RECOVERY_SCORE_MIN) {
            sb.append("，状态偏强续航");
        } else if (d.recoveryScore >= AppConfig.RECOVERY_SCORE_STRONG) {
            sb.append("，状态偏恢复延续");
        } else if (d.driftRisk >= AppConfig.DRIFT_RISK_MAX) {
            sb.append("，状态有漂移风险");
        } else if (d.breakDepth >= AppConfig.BREAK_DEPTH_MAX) {
            sb.append("，状态有断点风险");
        } else {
            sb.append("，状态中性");
        }

        return sb.toString();
    }

    private String buildActualNumberShape(Models.Decision d, int actual) {
        if (!AppConfig.isValidNumber(actual)) {
            return "实际号码非法";
        }

        StringBuilder sb = new StringBuilder();

        sb.append(formatNumber(actual));
        sb.append("=");
        sb.append(numberZoneText(actual));
        sb.append("/");
        sb.append(actual % 2 == 0 ? "偶" : "奇");

        if (d != null && strictDecisionK3Hit(d, actual)) {
            sb.append("，落在K3内");
            return sb.toString();
        }

        if (d != null) {
            int nearest = nearestDistanceToK3(d, actual);

            if (nearest == NEAR_NUMBER_DISTANCE) {
                sb.append("，贴近K3邻号");
            } else if (nearest > 0 && nearest <= 2) {
                sb.append("，距离K3较近");
            } else {
                sb.append("，脱离K3覆盖层");
            }

            if (!selectedCoversActualZone(d, actual)) {
                sb.append("，实际区间未被覆盖");
            }
        }

        return sb.toString();
    }

    private String buildWhyActualAppeared(Models.Decision d, int actual, boolean hit) {
        if (d == null || !AppConfig.isValidNumber(actual)) {
            return "";
        }

        if (hit) {
            if (d.trendLife >= AppConfig.TREND_LIFE_MIN
                    && d.driftRisk <= AppConfig.DRIFT_RISK_MAX) {
                return "趋势寿命仍在，漂移未放大，实际号码落在模型K3覆盖内";
            }

            if (d.recoveryScore >= AppConfig.RECOVERY_SCORE_MIN) {
                return "恢复分有效，实际号码跟随恢复链进入K3覆盖";
            }

            return "实际号码命中K3，说明当前候选层仍有有效偏差";
        }

        if (isActualNearPredictedK3(d, actual)) {
            return "实际号码贴近K3邻号，说明方向判断接近，但第三号码层级选择偏一层";
        }

        if (!selectedCoversActualZone(d, actual)) {
            return "实际号码落在未覆盖区间，说明当期号码结构由另一层区间接管";
        }

        if (d.driftRisk >= AppConfig.DRIFT_RISK_MAX) {
            return "漂移风险偏高，实际号码更容易脱离原热层";
        }

        if (d.breakDepth >= AppConfig.BREAK_DEPTH_MAX) {
            return "断点深度偏高，实际号码更像破位后的新层号码";
        }

        if (d.hotReverseRisk >= AppConfig.HOT_REVERSE_RISK_MAX) {
            return "热反杀风险偏高，实际号码更可能从反向层冒出";
        }

        if (d.recoveryScore < AppConfig.RECOVERY_SCORE_MIN
                && AppConfig.MODE_RECOVERING.equals(d.stateMode)) {
            return "恢复状态不够硬，实际号码没有继续走恢复链";
        }

        return "实际号码来自K3外层，当前决策对号码层级覆盖不足";
    }

    private String buildWhyPredictedFailed(
            Models.Decision d, int actual, boolean hit, String missReason) {
        if (d == null) {
            return "";
        }

        if (hit) {
            return "预测号码覆盖有效，本期不做惩罚，只做命中状态强化";
        }

        StringBuilder sb = new StringBuilder();

        if (missReason != null && missReason.length() > 0) {
            sb.append(UiFormatter.missReasonText(missReason));
        }

        if (isActualNearPredictedK3(d, actual)) {
            appendPart(sb, "K3方向接近但边缘号码未纳入");
        }

        if (d.comboGap < AppConfig.COMBO_GAP_MIN) {
            appendPart(sb, "组合差距太小，第三号码不够硬");
        }

        if (d.championGap < AppConfig.CHAMPION_GAP_MIN) {
            appendPart(sb, "位置冠军优势不足");
        }

        if (d.effectiveSample > 0 && d.effectiveSample < AppConfig.EFFECTIVE_SAMPLE_MIN) {
            appendPart(sb, "有效样本偏少");
        }

        if (d.driftRisk >= AppConfig.DRIFT_RISK_MAX) {
            appendPart(sb, "漂移风险偏高");
        }

        if (d.breakDepth >= AppConfig.BREAK_DEPTH_MAX) {
            appendPart(sb, "断点风险偏高");
        }

        if (!selectedCoversActualZone(d, actual)) {
            appendPart(sb, "实际区间未被K3覆盖");
        }

        if (sb.length() == 0) {
            sb.append("未发现单一强错因，归类为未知噪声或短周期切换");
        }

        return sb.toString();
    }

    private String buildNextStateAdvice(
            Models.Decision d, int actual, boolean hit, String missReason) {
        if (d == null) {
            return "";
        }

        if (hit) {
            if (d.finalP >= AppConfig.TARGET_HIT_RATE_IDEAL
                    && d.driftRisk <= AppConfig.DRIFT_RISK_MAX
                    && d.breakDepth <= AppConfig.BREAK_DEPTH_MAX) {
                return "同状态可轻微强化该形态，但仍保持单位置K3，不扩大下注";
            }

            return "命中但优势不算极强，只强化形态记忆，不盲目降低门槛";
        }

        if (AppConfig.MISS_K3_RANK_4_6.equals(missReason) || isActualNearPredictedK3(d, actual)) {
            return "同状态下优先修正第三号码层，保留主方向，增加邻号/四到六层号码画像权重；实时BET仍需最终门确认";
        }

        if (AppConfig.MISS_TREND_BROKEN.equals(missReason)
                || d.breakDepth >= AppConfig.BREAK_DEPTH_MAX) {
            return "同状态下断点深度偏高必须转HOLD或提高门槛，等待恢复确认";
        }

        if (AppConfig.MISS_DRIFT_SPIKE.equals(missReason)
                || d.driftRisk >= AppConfig.DRIFT_RISK_MAX) {
            return "同状态下漂移高时降低旧热号权重，要求更高championGap和comboGap";
        }

        if (AppConfig.MISS_FALSE_RECOVERY.equals(missReason)) {
            return "恢复分不足时不要提前押恢复链，需等待recoveryScore与trendLife同时增强";
        }

        if (AppConfig.MISS_SAMPLE_WEAK.equals(missReason)) {
            return "样本弱时只做SHADOW，不进真实BET";
        }

        if (AppConfig.MISS_POSITION_LOST.equals(missReason)) {
            return "位置优势不足时减少该位置权重，优先换到强位";
        }

        if (!selectedCoversActualZone(d, actual)) {
            return "同状态下号码结构要补实际区间，不要只押单一区间热层";
        }

        return "同状态下收紧真实BET，先进入SHADOW积累证据";
    }

    private String buildWhyAutoActualAppeared(PositionForecast f, int actual, boolean hit) {
        if (f == null || !AppConfig.isValidNumber(actual)) {
            return "";
        }

        if (hit) {
            if (f.trendScore >= 0.45d && f.riskScore <= 0.45d) {
                return "自动反推命中：该位置短窗、中窗、转移层与间隔回补同向，实际号码进入K3覆盖。";
            }

            if (f.recoveryScore >= 0.30d) {
                return "自动反推命中：实际号码跟随恢复链进入K3。";
            }

            return "自动反推命中：该状态下K3覆盖有效。";
        }

        if (nearestDistanceToForecast(f, actual) <= 1) {
            return "实际号码贴近自动K3邻号，方向接近，但第三号码层偏一格。";
        }

        if (!forecastCoversZone(f, actual)) {
            return "实际号码来自未覆盖区间，说明当期区间结构切换。";
        }

        if (f.driftRisk >= 0.55d) {
            return "短中长窗口漂移偏大，实际号码脱离旧热层。";
        }

        if (f.breakRisk >= 0.45d) {
            return "候选热层断点偏高，实际号码更像破位后的新层。";
        }

        return "实际号码由短周期切换产生，自动K3没有覆盖。";
    }

    private void applyTraceToReport(LearningReport report, CognitiveTrace trace) {
        if (report == null || trace == null) {
            return;
        }

        report.stateKey = trace.stateKey;
        report.expectedShape = trace.expectedShape;
        report.actualShape = trace.actualShape;
        report.whyActualAppeared = trace.whyActualAppeared;
        report.whyPredictedFailed = trace.whyPredictedFailed;
        report.nextStateAdvice = trace.nextAdvice;
        report.lastTraceSummary = trace.summary;

        if (trace.missReason != null && trace.missReason.length() > 0) {
            report.lastMissReason = trace.missReason;
            addMissReason(report, trace.missReason);
        }
    }

    private void saveLearningState(DbStore store, LearningReport report) {
        if (store == null || report == null) {
            return;
        }

        try {
            report.recalc();

            if (report.detail == null || report.detail.length() == 0) {
                report.detail = buildReportDetail(report);
            }

            store.setState("learning_summary", report.toSummaryText());
            store.setState("learning_reason_detail", report.detail);
            store.setState("learning_state_key", report.stateKey == null ? "" : report.stateKey);
            store.setState(
                    "learning_expected_shape",
                    report.expectedShape == null ? "" : report.expectedShape);
            store.setState(
                    "learning_actual_shape", report.actualShape == null ? "" : report.actualShape);
            store.setState(
                    "learning_next_advice",
                    report.nextStateAdvice == null ? "" : report.nextStateAdvice);
            store.setState(
                    "learning_strict_rule",
                    "REAL_CALIBRATION_ONLY_REAL_TRADE_BET_TARGET_MATCH_STRICT_POSITION_K3_WIN_MISS");

            if (report.teacherMatrixCellCount > 0) {
                store.setState(
                        "teacher_matrix_summary",
                        "教师矩阵已更新："
                                + report.teacherMatrixDrawCount
                                + "期/"
                                + report.teacherMatrixCellCount
                                + "格｜只做画像/风险/解释");
            }

            if (report.autoLearnCaseCount > 0) {
                store.setState("auto_learn_summary", report.detail);
                store.setState(
                        "runtime_profile_summary",
                        "后台学习画像已更新："
                                + report.autoLearnSavedCount
                                + "条旧画像样本；教师矩阵="
                                + report.teacherMatrixCellCount
                                + "格；"
                                + report.toSummaryText());
            }

            if (report.topMissReason != null && report.topMissReason.length() > 0) {
                store.setState("learning_top_miss_reason", report.topMissReason);
            }
        } catch (Throwable e) {
        }
    }

    private Models.Decision findDecisionForTarget(
            List<Models.Decision> decisions, String targetIssue) {
        if (decisions == null || targetIssue == null || targetIssue.length() == 0) {
            return null;
        }

        for (int i = 0; i < decisions.size(); i++) {
            Models.Decision d = decisions.get(i);

            if (d == null) {
                continue;
            }

            if (targetIssue.equals(d.targetIssue)) {
                return d;
            }
        }

        return null;
    }

    /* =========================
     * 通用判断与工具
     * ========================= */

    private boolean isReverseLike(
            String stateMode, double driftRisk, double breakDepth, String text) {
        if (AppConfig.MODE_REVERSING.equals(stateMode)
                || AppConfig.MODE_BROKEN.equals(stateMode)
                || "REVERSAL".equals(stateMode)) {
            return true;
        }

        if (driftRisk >= AppConfig.DRIFT_RISK_HARD_BLOCK) {
            return true;
        }

        if (breakDepth >= 55.0d) {
            return true;
        }

        if (text != null) {
            if (text.indexOf("反杀") >= 0 || text.indexOf("反转") >= 0 || text.indexOf("破位") >= 0) {
                return true;
            }
        }

        return false;
    }

    private boolean containsBoth(String text, String a, String b) {
        if (text == null) {
            return false;
        }

        return text.indexOf(a) >= 0 && text.indexOf(b) >= 0;
    }

    private void addMissReason(LearningReport report, String reason) {
        if (report == null || reason == null || reason.length() == 0) {
            return;
        }

        if (report.missReasons == null) {
            report.missReasons = new ArrayList<String>();
        }

        report.missReasons.add(reason);
        report.topMissReason = findTopMissReason(report.missReasons);
    }

    private String findTopMissReason(ArrayList<String> reasons) {
        if (reasons == null || reasons.size() == 0) {
            return "";
        }

        String best = "";
        int bestCount = 0;

        for (int i = 0; i < reasons.size(); i++) {
            String r = reasons.get(i);

            if (r == null || r.length() == 0) {
                continue;
            }

            int count = 0;

            for (int j = 0; j < reasons.size(); j++) {
                String x = reasons.get(j);

                if (r.equals(x)) {
                    count++;
                }
            }

            if (count > bestCount) {
                bestCount = count;
                best = r;
            }
        }

        return best;
    }

    private void addSkip(LearningReport report, String reason) {
        if (report == null) {
            return;
        }

        report.skipCount++;
        report.lastSkipReason = reason == null ? "" : reason;
    }

    private void addError(LearningReport report, String reason) {
        if (report == null) {
            return;
        }

        report.errorCount++;
        report.lastErrorReason = reason == null ? "" : reason;
    }

    private String appendReason(String oldText, String add) {
        if (add == null || add.length() == 0) {
            return oldText == null ? "" : oldText;
        }

        if (oldText == null || oldText.length() == 0) {
            return add;
        }

        return oldText + "；" + add;
    }

    private void appendPart(StringBuilder sb, String text) {
        if (sb == null || text == null || text.length() == 0) {
            return;
        }

        if (sb.length() > 0) {
            sb.append("，");
        }

        sb.append(text);
    }

    private boolean isActualNearPredictedK3(Models.Decision d, int actual) {
        if (d == null || !AppConfig.isValidNumber(actual)) {
            return false;
        }

        return Math.abs(actual - d.n1) <= NEAR_NUMBER_DISTANCE
                || Math.abs(actual - d.n2) <= NEAR_NUMBER_DISTANCE
                || Math.abs(actual - d.n3) <= NEAR_NUMBER_DISTANCE;
    }

    private int nearestDistanceToK3(Models.Decision d, int actual) {
        if (d == null || !AppConfig.isValidNumber(actual)) {
            return 0;
        }

        int best = 99;

        if (AppConfig.isValidNumber(d.n1)) {
            best = Math.min(best, Math.abs(actual - d.n1));
        }

        if (AppConfig.isValidNumber(d.n2)) {
            best = Math.min(best, Math.abs(actual - d.n2));
        }

        if (AppConfig.isValidNumber(d.n3)) {
            best = Math.min(best, Math.abs(actual - d.n3));
        }

        return best == 99 ? 0 : best;
    }

    private boolean selectedCoversActualZone(Models.Decision d, int actual) {
        if (d == null || !AppConfig.isValidNumber(actual)) {
            return false;
        }

        int z = numberZone(actual);
        return numberZone(d.n1) == z || numberZone(d.n2) == z || numberZone(d.n3) == z;
    }

    private int numberZone(int n) {
        if (!AppConfig.isValidNumber(n)) {
            return 0;
        }

        if (n <= 3) {
            return 1;
        }

        if (n <= 7) {
            return 2;
        }

        return 3;
    }

    private String numberZoneText(int n) {
        int z = numberZone(n);

        if (z == 1) {
            return "低区";
        }

        if (z == 2) {
            return "中区";
        }

        if (z == 3) {
            return "高区";
        }

        return "未知区";
    }

    private String formatK3(int n1, int n2, int n3) {
        return formatNumber(n1) + "/" + formatNumber(n2) + "/" + formatNumber(n3);
    }

    private String formatNumber(int n) {
        if (!AppConfig.isValidNumber(n)) {
            return "00";
        }

        if (n < 10) {
            return "0" + n;
        }

        return String.valueOf(n);
    }

    private String safeMode(String mode) {
        if (mode == null || mode.length() == 0) {
            return AppConfig.MODE_UNKNOWN;
        }

        return mode;
    }

    private String safeText(String text) {
        return text == null ? "" : text;
    }

    private String bucketScore(double v) {
        if (v <= 0.0d) {
            return "0";
        }

        if (v < 30.0d) {
            return "L";
        }

        if (v < 55.0d) {
            return "M";
        }

        if (v < 75.0d) {
            return "H";
        }

        return "X";
    }

    private String bucketProb(double v) {
        if (v <= 0.0d) {
            return "0";
        }

        if (v < 0.340d) {
            return "L";
        }

        if (v < 0.375d) {
            return "S";
        }

        if (v < 0.400d) {
            return "B";
        }

        if (v < 0.430d) {
            return "H";
        }

        return "X";
    }

    private double gapToScore(int gap) {
        if (gap <= 0) {
            return 0.08d;
        }

        if (gap <= 2) {
            return 0.12d;
        }

        if (gap <= 5) {
            return 0.16d;
        }

        if (gap <= 12) {
            return 0.10d;
        }

        if (gap <= 24) {
            return 0.05d;
        }

        return 0.02d;
    }

    private double reboundScore(int gap, double s, double m, double l) {
        double score = 0.0d;

        if (gap >= 3 && gap <= 12 && s >= m * 0.80d) {
            score += 0.05d;
        }

        if (gap >= 8 && gap <= 24 && m >= l * 0.85d) {
            score += 0.04d;
        }

        if (gap <= 2 && s > m * 1.35d) {
            score -= 0.03d;
        }

        return score;
    }

    private double zoneBalanceScore(int n, int[] countLong) {
        if (!AppConfig.isValidNumber(n) || countLong == null || countLong.length < 11) {
            return 0.0d;
        }

        int z = numberZone(n);
        int total = 0;
        int zone = 0;

        for (int i = 1; i <= 10; i++) {
            total += countLong[i];

            if (numberZone(i) == z) {
                zone += countLong[i];
            }
        }

        if (total <= 0) {
            return 0.0d;
        }

        double rate = ((double) zone) / ((double) total);

        if (rate < 0.22d) {
            return 0.04d;
        }

        if (rate > 0.46d) {
            return -0.02d;
        }

        return 0.02d;
    }

    private int pickTop(double[] score, int used1, int used2) {
        return pickTop(score, used1, used2, 0);
    }

    private int pickTop(double[] score, int used1, int used2, int used3) {
        if (score == null || score.length < 11) {
            return 0;
        }

        int best = 0;
        double bestScore = -999.0d;

        for (int n = 1; n <= 10; n++) {
            if (n == used1 || n == used2 || n == used3) {
                continue;
            }

            if (score[n] > bestScore) {
                bestScore = score[n];
                best = n;
            }
        }

        return best;
    }

    private double calcDriftRisk(int[] shortCount, int[] midCount, int[] longCount) {
        if (shortCount == null || midCount == null || longCount == null) {
            return 0.0d;
        }

        double diff = 0.0d;

        for (int n = 1; n <= 10; n++) {
            double s = ((double) shortCount[n]) / ((double) AUTO_LEARN_SCORE_WINDOW_SHORT);
            double m = ((double) midCount[n]) / ((double) AUTO_LEARN_SCORE_WINDOW_MID);
            double l = ((double) longCount[n]) / ((double) AUTO_LEARN_SCORE_WINDOW_LONG);
            diff += Math.abs(s - m) * 0.65d + Math.abs(m - l) * 0.35d;
        }

        return clamp01(diff);
    }

    private double calcBreakRisk(
            int[] shortCount, int[] midCount, int[] longCount, int n1, int n2, int n3) {
        if (shortCount == null || midCount == null || longCount == null) {
            return 0.0d;
        }

        double risk = 0.0d;
        int[] nums = new int[] {n1, n2, n3};

        for (int i = 0; i < nums.length; i++) {
            int n = nums[i];

            if (!AppConfig.isValidNumber(n)) {
                continue;
            }

            double s = ((double) shortCount[n]) / ((double) AUTO_LEARN_SCORE_WINDOW_SHORT);
            double m = ((double) midCount[n]) / ((double) AUTO_LEARN_SCORE_WINDOW_MID);
            double l = ((double) longCount[n]) / ((double) AUTO_LEARN_SCORE_WINDOW_LONG);

            if (l > 0.08d && m > 0.08d && s < 0.035d) {
                risk += 0.22d;
            } else if (m > 0.08d && s < 0.04d) {
                risk += 0.12d;
            }
        }

        return clamp01(risk);
    }

    private double calcRecoveryScore(int[] gap, int n1, int n2, int n3) {
        if (gap == null || gap.length < 11) {
            return 0.0d;
        }

        int[] nums = new int[] {n1, n2, n3};
        double score = 0.0d;

        for (int i = 0; i < nums.length; i++) {
            int n = nums[i];

            if (!AppConfig.isValidNumber(n)) {
                continue;
            }

            int g = gap[n];

            if (g >= 3 && g <= 12) {
                score += 0.20d;
            } else if (g >= 13 && g <= 24) {
                score += 0.12d;
            } else if (g <= 2) {
                score += 0.08d;
            }
        }

        return clamp01(score);
    }

    private String buildAutoStateMode(PositionForecast f) {
        if (f == null) {
            return AppConfig.MODE_UNKNOWN;
        }

        if (f.breakRisk >= 0.58d) {
            return AppConfig.MODE_BROKEN;
        }

        if (f.driftRisk >= 0.62d) {
            return AppConfig.MODE_REVERSING;
        }

        if (f.recoveryScore >= 0.34d && f.driftRisk < 0.52d) {
            return AppConfig.MODE_RECOVERING;
        }

        if (f.trendScore >= 0.45d && f.riskScore < 0.45d) {
            return "AUTO_STABLE";
        }

        if (f.trendScore >= 0.36d && f.riskScore < 0.56d) {
            return AppConfig.MODE_FORMING;
        }

        return AppConfig.MODE_CHAOS;
    }

    private String buildAutoShapeKey(PositionForecast f) {
        if (f == null) {
            return "";
        }

        int low = 0;
        int mid = 0;
        int high = 0;
        int odd = 0;
        int even = 0;
        int min = 99;
        int max = 0;
        int[] nums = new int[] {f.n1, f.n2, f.n3};

        for (int i = 0; i < nums.length; i++) {
            int n = nums[i];

            if (!AppConfig.isValidNumber(n)) {
                continue;
            }

            if (numberZone(n) == 1) {
                low++;
            } else if (numberZone(n) == 2) {
                mid++;
            } else {
                high++;
            }

            if (n % 2 == 0) {
                even++;
            } else {
                odd++;
            }

            if (n < min) {
                min = n;
            }

            if (n > max) {
                max = n;
            }
        }

        int span = max >= min && max > 0 ? max - min : 0;

        return "Z" + low + mid + high + "_OE" + odd + even + "_SP" + span;
    }

    private String buildActualShapeForAuto(int actual, PositionForecast f) {
        StringBuilder sb = new StringBuilder();

        sb.append(formatNumber(actual));
        sb.append("=");
        sb.append(numberZoneText(actual));
        sb.append("/");
        sb.append(actual % 2 == 0 ? "偶" : "奇");

        if (f != null && strictForecastK3Hit(f, actual)) {
            sb.append("，在自动K3内");
        } else if (f != null) {
            int near = nearestDistanceToForecast(f, actual);

            if (near == 1) {
                sb.append("，贴近自动K3邻号");
            } else {
                sb.append("，脱离自动K3");
            }
        }

        return sb.toString();
    }

    private int nearestDistanceToForecast(PositionForecast f, int actual) {
        if (f == null || !AppConfig.isValidNumber(actual)) {
            return 99;
        }

        int best = Math.abs(actual - f.n1);
        best = Math.min(best, Math.abs(actual - f.n2));
        best = Math.min(best, Math.abs(actual - f.n3));
        return best;
    }

    private String buildAutoMissReason(PositionForecast f, int actual) {
        if (f == null) {
            return "自动反推失败：状态为空";
        }

        StringBuilder sb = new StringBuilder();

        if (nearestDistanceToForecast(f, actual) <= 1) {
            appendPart(sb, "实际贴近K3邻号，第三号码层偏差");
        }

        if (f.driftRisk >= 0.55d) {
            appendPart(sb, "短窗与中长窗漂移较大");
        }

        if (f.breakRisk >= 0.45d) {
            appendPart(sb, "候选热层有断点风险");
        }

        if (f.comboGap < 0.010d) {
            appendPart(sb, "第三名与第四名差距小");
        }

        if (!forecastCoversZone(f, actual)) {
            appendPart(sb, "实际区间未覆盖");
        }

        if (sb.length() == 0) {
            sb.append("短周期切换，自动K3未覆盖实际号码");
        }

        return sb.toString();
    }

    private String classifyAutoMissCode(PositionForecast f, int actual) {
        if (f == null) {
            return AppConfig.MISS_UNKNOWN;
        }

        if (nearestDistanceToForecast(f, actual) <= 1) {
            return AppConfig.MISS_K3_RANK_4_6;
        }

        if (f.driftRisk >= 0.55d) {
            return AppConfig.MISS_DRIFT_SPIKE;
        }

        if (f.breakRisk >= 0.45d) {
            return AppConfig.MISS_TREND_BROKEN;
        }

        if (f.comboGap < 0.010d) {
            return AppConfig.MISS_THIRD_WEAK;
        }

        if (!forecastCoversZone(f, actual)) {
            return AppConfig.MISS_THIRD_WEAK;
        }

        return AppConfig.MISS_UNKNOWN;
    }

    private boolean forecastCoversZone(PositionForecast f, int actual) {
        if (f == null || !AppConfig.isValidNumber(actual)) {
            return false;
        }

        int z = numberZone(actual);
        return numberZone(f.n1) == z || numberZone(f.n2) == z || numberZone(f.n3) == z;
    }

    private String buildAutoNextAdvice(PositionForecast f, int actual, boolean hit) {
        if (f == null) {
            return "";
        }

        if (hit) {
            return "同状态下强化该位置K3组合画像，但实时BET仍需门槛确认。";
        }

        if (nearestDistanceToForecast(f, actual) <= 1) {
            return "同状态下增加邻号和四到六层号码画像权重，修正第三号码层；不直接救援BET。";
        }

        if (f.driftRisk >= 0.55d) {
            return "同状态下漂移偏高，实时分析应提高风险确认或转SHADOW。";
        }

        if (f.breakRisk >= 0.45d) {
            return "同状态下断点风险高，优先等待恢复确认。";
        }

        if (!forecastCoversZone(f, actual)) {
            return "同状态下补足实际区间覆盖，不要只追单一区间。";
        }

        return "同状态下收紧真实BET，继续积累后台样本。";
    }

    private String buildCaseShortTrace(Models.LearningCase c) {
        if (c == null) {
            return "";
        }

        StringBuilder sb = new StringBuilder();

        sb.append("后台样本：");
        sb.append(c.targetIssue);
        sb.append(" P");
        sb.append(c.position);
        sb.append(" K=");
        sb.append(c.predictedK3);
        sb.append(" 实际=");
        sb.append(formatNumber(c.actualNumber));
        sb.append(c.hit == 1 ? " 命中" : " 未中");

        if (c.whyActualHit != null && c.whyActualHit.length() > 0) {
            sb.append("｜实际原因=");
            sb.append(c.whyActualHit);
        }

        if (c.whyMiss != null && c.whyMiss.length() > 0) {
            sb.append("｜偏差=");
            sb.append(c.whyMiss);
        }

        if (c.nextAdvice != null && c.nextAdvice.length() > 0) {
            sb.append("｜修正=");
            sb.append(c.nextAdvice);
        }

        return safeLongText(sb.toString());
    }

    /* =========================
     * 严格学习口径
     * ========================= */

    private boolean isRealBetDecisionForLearning(Models.Decision d) {
        if (d == null) {
            return false;
        }

        if (!AppConfig.DECISION_BET.equals(normalizeDecisionText(d.decision))) {
            return false;
        }

        if (!isRealTradeScopeStrict(d.statScope)) {
            return false;
        }

        if (!isBetActionForLearning(d.action, d.decision)) {
            return false;
        }

        repairDecisionK3(d);

        if (!isValidLearningDecisionK3(d)) {
            return false;
        }

        return true;
    }

    private boolean isRealBetTradeForLearning(Models.Trade t) {
        if (t == null) {
            return false;
        }

        if (!isRealTradeScopeStrict(t.statScope)) {
            return false;
        }

        if (!isBetActionForLearning(t.action, AppConfig.DECISION_BET)) {
            return false;
        }

        repairTradeK3(t);

        if (!isValidLearningTradeK3(t)) {
            return false;
        }

        return true;
    }

    private boolean isRealTradeScopeStrict(String scope) {
        String s = normalizeStatScope(scope);
        return AppConfig.STAT_SCOPE_REAL_TRADE.equals(s);
    }

    private boolean isBetActionForLearning(String action, String fallbackDecision) {
        String a = action == null ? "" : action.trim();

        if (a.length() == 0) {
            a = fallbackDecision == null ? "" : fallbackDecision.trim();
        }

        if (a.length() == 0) {
            return false;
        }

        return AppConfig.DECISION_BET.equals(a)
                || AppConfig.ACTION_BET.equals(a)
                || "BET".equalsIgnoreCase(a)
                || "REAL_BET".equalsIgnoreCase(a)
                || "REAL_TRADE".equalsIgnoreCase(a)
                || "下注".equals(a)
                || "出手".equals(a);
    }

    private void repairDecisionK3(Models.Decision d) {
        if (d == null) {
            return;
        }

        if (isValidK3Numbers(d.n1, d.n2, d.n3)) {
            try {
                d.k3Mask = Models.buildK3Mask(d.n1, d.n2, d.n3);
            } catch (Throwable e) {
                d.k3Mask = buildK3MaskLocal(d.n1, d.n2, d.n3);
            }
        }
    }

    private void repairTradeK3(Models.Trade t) {
        if (t == null) {
            return;
        }

        if (isValidK3Numbers(t.n1, t.n2, t.n3)) {
            try {
                t.k3Mask = Models.buildK3Mask(t.n1, t.n2, t.n3);
            } catch (Throwable e) {
                t.k3Mask = buildK3MaskLocal(t.n1, t.n2, t.n3);
            }
        }
    }

    private void repairCandidateK3(Models.ComboCandidate c) {
        if (c == null) {
            return;
        }

        if (isValidK3Numbers(c.n1, c.n2, c.n3)) {
            try {
                c.k3Mask = Models.buildK3Mask(c.n1, c.n2, c.n3);
            } catch (Throwable e) {
                c.k3Mask = buildK3MaskLocal(c.n1, c.n2, c.n3);
            }
        }
    }

    private boolean isValidLearningDecisionK3(Models.Decision d) {
        if (d == null) {
            return false;
        }

        if (!AppConfig.isValidPosition(d.position)) {
            return false;
        }

        if (!isValidK3Numbers(d.n1, d.n2, d.n3)) {
            return false;
        }

        repairDecisionK3(d);
        return Models.isValidK3Mask(d.k3Mask);
    }

    private boolean isValidLearningTradeK3(Models.Trade t) {
        if (t == null) {
            return false;
        }

        if (!AppConfig.isValidPosition(t.position)) {
            return false;
        }

        if (!isValidK3Numbers(t.n1, t.n2, t.n3)) {
            return false;
        }

        repairTradeK3(t);
        return Models.isValidK3Mask(t.k3Mask);
    }

    private boolean isValidForecastK3(PositionForecast f) {
        if (f == null) {
            return false;
        }

        if (!AppConfig.isValidPosition(f.position)) {
            return false;
        }

        if (!isValidK3Numbers(f.n1, f.n2, f.n3)) {
            return false;
        }

        f.k3Mask = Models.buildK3Mask(f.n1, f.n2, f.n3);
        return Models.isValidK3Mask(f.k3Mask);
    }

    private boolean isValidK3Numbers(int n1, int n2, int n3) {
        if (!AppConfig.isValidNumber(n1)
                || !AppConfig.isValidNumber(n2)
                || !AppConfig.isValidNumber(n3)) {
            return false;
        }

        if (n1 == n2 || n1 == n3 || n2 == n3) {
            return false;
        }

        return true;
    }

    private boolean strictDecisionK3Hit(Models.Decision d, int actual) {
        if (d == null || !AppConfig.isValidNumber(actual)) {
            return false;
        }

        return actual == d.n1 || actual == d.n2 || actual == d.n3;
    }

    private boolean strictTradeK3Hit(Models.Trade t, int actual) {
        if (t == null || !AppConfig.isValidNumber(actual)) {
            return false;
        }

        return actual == t.n1 || actual == t.n2 || actual == t.n3;
    }

    private boolean strictCandidateK3Hit(Models.ComboCandidate c, int actual) {
        if (c == null || !AppConfig.isValidNumber(actual)) {
            return false;
        }

        return actual == c.n1 || actual == c.n2 || actual == c.n3;
    }

    private boolean strictForecastK3Hit(PositionForecast f, int actual) {
        if (f == null || !AppConfig.isValidNumber(actual)) {
            return false;
        }

        return actual == f.n1 || actual == f.n2 || actual == f.n3;
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

    private String bucketKey(String prefix, double value) {
        if (prefix == null || prefix.length() == 0) {
            prefix = "B";
        }

        if (value <= 0.0d) {
            return prefix + "_0";
        }

        if (value < 0.25d) {
            return prefix + "_LOW";
        }

        if (value < 0.50d) {
            return prefix + "_MID";
        }

        if (value < 0.75d) {
            return prefix + "_HIGH";
        }

        return prefix + "_EXTREME";
    }

    private double normalizeScore01(double v) {
        if (v <= 0.0d) {
            return 0.0d;
        }

        if (v > 1.0d) {
            v = v / 100.0d;
        }

        return clamp01(v);
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

    private double clampSigned(double v, double maxAbs) {
        if (Double.isNaN(v) || Double.isInfinite(v)) {
            return 0.0d;
        }

        if (maxAbs < 0.0d) {
            maxAbs = -maxAbs;
        }

        if (v > maxAbs) {
            return maxAbs;
        }

        if (v < -maxAbs) {
            return -maxAbs;
        }

        return v;
    }

    private double clampProbability(double p) {
        if (Double.isNaN(p) || Double.isInfinite(p)) {
            return 0.0d;
        }

        if (p < 0.0d) {
            return 0.0d;
        }

        if (p > 0.95d) {
            return 0.95d;
        }

        return p;
    }

    private String formatDouble(double v) {
        int x = (int) Math.round(v * 1000.0d);
        return String.valueOf(x);
    }

    private String safeLongText(String text) {
        if (text == null) {
            return "";
        }

        if (text.length() <= MAX_REASON_TEXT) {
            return text;
        }

        return text.substring(0, MAX_REASON_TEXT);
    }

    private String normalizeDecisionText(String decision) {
        return AppConfig.normalizeDecision(decision);
    }

    private String normalizeResultText(String result) {
        if (result == null) {
            return AppConfig.RESULT_WAIT;
        }

        String r = result.trim();

        if (r.length() == 0) {
            return AppConfig.RESULT_WAIT;
        }

        if (AppConfig.RESULT_WIN.equals(r) || "HIT".equalsIgnoreCase(r) || "命中".equals(r)) {
            return AppConfig.RESULT_WIN;
        }

        if (AppConfig.RESULT_MISS.equals(r)
                || "LOSE".equalsIgnoreCase(r)
                || "LOSS".equalsIgnoreCase(r)
                || "未中".equals(r)
                || "不中".equals(r)) {
            return AppConfig.RESULT_MISS;
        }

        if (AppConfig.RESULT_DATA_ERROR.equals(r)
                || "DATA_ERROR".equalsIgnoreCase(r)
                || "数据异常".equals(r)) {
            return AppConfig.RESULT_DATA_ERROR;
        }

        if (AppConfig.RESULT_VOID.equals(r) || "VOID".equalsIgnoreCase(r) || "作废".equals(r)) {
            return AppConfig.RESULT_VOID;
        }

        if (AppConfig.RESULT_WAIT.equals(r)
                || "WAIT".equalsIgnoreCase(r)
                || "等待".equals(r)
                || "等待结算".equals(r)) {
            return AppConfig.RESULT_WAIT;
        }

        return r;
    }

    private String normalizeStatScope(String scope) {
        if (scope == null) {
            return "";
        }

        String s = scope.trim();

        if (s.length() == 0) {
            return "";
        }

        if (AppConfig.STAT_SCOPE_REAL_TRADE.equals(s)
                || "REAL".equalsIgnoreCase(s)
                || "REAL_TRADE".equalsIgnoreCase(s)
                || "REAL_BET".equalsIgnoreCase(s)
                || "REALBET".equalsIgnoreCase(s)) {
            return AppConfig.STAT_SCOPE_REAL_TRADE;
        }

        if (AppConfig.STAT_SCOPE_SHADOW.equals(s) || "SHADOW".equalsIgnoreCase(s)) {
            return AppConfig.STAT_SCOPE_SHADOW;
        }

        if (AppConfig.STAT_SCOPE_HOLD.equals(s) || "HOLD".equalsIgnoreCase(s)) {
            return AppConfig.STAT_SCOPE_HOLD;
        }

        return s;
    }

    private void copySameFieldIfExists(Object from, Object to, String fieldName) {
        if (from == null || to == null || fieldName == null || fieldName.length() == 0) {
            return;
        }

        try {
            Field f1 = from.getClass().getField(fieldName);
            Field f2 = to.getClass().getField(fieldName);

            if (Modifier.isStatic(f1.getModifiers()) || Modifier.isStatic(f2.getModifiers())) {
                return;
            }

            Object value = f1.get(from);
            f2.set(to, value);
        } catch (Throwable e) {
        }
    }

    private static final class AutoBuildResult {
        public Models.LearningCase learningCase;
        public PositionForecast forecast;

        public AutoBuildResult() {
            learningCase = null;
            forecast = null;
        }
    }

    private static final class PositionForecast {
        public int position;
        public int n1;
        public int n2;
        public int n3;
        public int k3Mask;
        public int sample;

        public double finalP;
        public double dynamicGate;
        public double trendScore;
        public double shapeScore;
        public double driftRisk;
        public double breakRisk;
        public double recoveryScore;
        public double profileScore;
        public double numberBias;
        public double comboBias;
        public double comboGap;
        public double riskScore;
        public double teacherWeight;
        public double stateConfidence;
        public int stateSample;
        public int cacheSample;
        public int recentSample;
        public double recentHitRate;

        public String stateKey;
        public String stateMode;
        public String trendKey;
        public String shapeKey;
        public String driftKey;
        public String breakKey;
        public String recoveryKey;
        public String transitionKey;
        public String expectedShape;
        public String whyExpectedHit;

        public PositionForecast() {
            position = 0;
            n1 = 0;
            n2 = 0;
            n3 = 0;
            k3Mask = 0;
            sample = 0;

            finalP = 0.0d;
            dynamicGate = 0.0d;
            trendScore = 0.0d;
            shapeScore = 0.0d;
            driftRisk = 0.0d;
            breakRisk = 0.0d;
            recoveryScore = 0.0d;
            profileScore = 0.0d;
            numberBias = 0.0d;
            comboBias = 0.0d;
            comboGap = 0.0d;
            riskScore = 0.0d;
            teacherWeight = 1.0d;
            stateConfidence = 0.0d;
            stateSample = 0;
            cacheSample = 0;
            recentSample = 0;
            recentHitRate = 0.0d;

            stateKey = "";
            stateMode = "";
            trendKey = "";
            shapeKey = "";
            driftKey = "";
            breakKey = "";
            recoveryKey = "";
            transitionKey = "";
            expectedShape = "";
            whyExpectedHit = "";
        }
    }

    private static final class CognitiveTrace {
        public String scope;
        public int hit;
        public int actualNumber;
        public String missReason;
        public String stateKey;
        public String expectedShape;
        public String actualShape;
        public String whyActualAppeared;
        public String whyPredictedFailed;
        public String nextAdvice;
        public String summary;

        public CognitiveTrace() {
            scope = "";
            hit = 0;
            actualNumber = 0;
            missReason = "";
            stateKey = "";
            expectedShape = "";
            actualShape = "";
            whyActualAppeared = "";
            whyPredictedFailed = "";
            nextAdvice = "";
            summary = "";
        }
    }

    public static final class LearningReport {
        public int ok;
        public String actualIssue;
        public long learnedAt;

        public int realBetLearnCount;
        public int realHitCount;
        public int realMissCount;
        public double realLearnHitRate;

        public int shadowLearnCount;
        public int shadowHitCount;
        public int shadowMissCount;
        public double shadowHitRate;

        public int shadowTopHitCount;
        public int shadowTopMissCount;

        public int holdObservationCount;
        public int breakRecoveryLearnCount;

        public int autoLearnIssueCount;
        public int autoLearnCaseCount;
        public int autoLearnSavedCount;
        public int autoLearnHitCount;
        public int autoLearnMissCount;
        public double autoLearnHitRate;
        public int autoLearnStartIndex;
        public int autoLearnEndIndex;
        public int autoLearnNextIndex;
        public String autoLearnLastIssue;

        public int teacherMatrixDrawCount;
        public int teacherMatrixCellCount;

        public int skipCount;
        public int errorCount;

        public int lastActualNumber;
        public String lastDecision;
        public String lastHitReason;
        public String lastMissReason;
        public String topMissReason;
        public String lastSkipReason;
        public String lastErrorReason;
        public String detail;

        public String stateKey;
        public String expectedShape;
        public String actualShape;
        public String whyActualAppeared;
        public String whyPredictedFailed;
        public String nextStateAdvice;
        public String lastTraceSummary;
        public String shadowReasonSummary;

        public ArrayList<String> missReasons;

        public LearningReport() {
            ok = 0;
            actualIssue = "";
            learnedAt = 0L;

            realBetLearnCount = 0;
            realHitCount = 0;
            realMissCount = 0;
            realLearnHitRate = 0.0d;

            shadowLearnCount = 0;
            shadowHitCount = 0;
            shadowMissCount = 0;
            shadowHitRate = 0.0d;

            shadowTopHitCount = 0;
            shadowTopMissCount = 0;

            holdObservationCount = 0;
            breakRecoveryLearnCount = 0;

            autoLearnIssueCount = 0;
            autoLearnCaseCount = 0;
            autoLearnSavedCount = 0;
            autoLearnHitCount = 0;
            autoLearnMissCount = 0;
            autoLearnHitRate = 0.0d;
            autoLearnStartIndex = 0;
            autoLearnEndIndex = 0;
            autoLearnNextIndex = 0;
            autoLearnLastIssue = "";

            teacherMatrixDrawCount = 0;
            teacherMatrixCellCount = 0;

            skipCount = 0;
            errorCount = 0;

            lastActualNumber = 0;
            lastDecision = "";
            lastHitReason = "";
            lastMissReason = "";
            topMissReason = "";
            lastSkipReason = "";
            lastErrorReason = "";
            detail = "";

            stateKey = "";
            expectedShape = "";
            actualShape = "";
            whyActualAppeared = "";
            whyPredictedFailed = "";
            nextStateAdvice = "";
            lastTraceSummary = "";
            shadowReasonSummary = "";

            missReasons = new ArrayList<String>();
        }

        public void recalc() {
            realLearnHitRate =
                    realBetLearnCount > 0
                            ? ((double) realHitCount) / ((double) realBetLearnCount)
                            : 0.0d;

            shadowHitRate =
                    shadowLearnCount > 0
                            ? ((double) shadowHitCount) / ((double) shadowLearnCount)
                            : 0.0d;

            autoLearnHitRate =
                    autoLearnCaseCount > 0
                            ? ((double) autoLearnHitCount) / ((double) autoLearnCaseCount)
                            : autoLearnHitRate;
        }

        public String toSummaryText() {
            recalc();

            StringBuilder sb = new StringBuilder();

            sb.append("真实BET学习=");
            sb.append(realBetLearnCount);
            sb.append(" 命中=");
            sb.append(realHitCount);
            sb.append(" 未中=");
            sb.append(realMissCount);
            sb.append(" HR=");
            sb.append(UiFormatter.probability(realLearnHitRate));

            sb.append("｜SHADOW=");
            sb.append(shadowLearnCount);
            sb.append(" HR=");
            sb.append(UiFormatter.probability(shadowHitRate));

            sb.append("｜HOLD观察=");
            sb.append(holdObservationCount);

            if (teacherMatrixCellCount > 0) {
                sb.append("｜教师矩阵=");
                sb.append(teacherMatrixDrawCount);
                sb.append("期/");
                sb.append(teacherMatrixCellCount);
                sb.append("格");
            }

            if (autoLearnCaseCount > 0) {
                sb.append("｜后台=");
                sb.append(autoLearnIssueCount);
                sb.append("期/");
                sb.append(autoLearnCaseCount);
                sb.append("样本 HR=");
                sb.append(UiFormatter.probability(autoLearnHitRate));
                sb.append(" 画像反推");
            }

            if (stateKey != null && stateKey.length() > 0) {
                sb.append("｜状态=");
                sb.append(stateKey);
            }

            if (nextStateAdvice != null && nextStateAdvice.length() > 0) {
                sb.append("｜修正=");
                sb.append(nextStateAdvice);
            }

            if (topMissReason != null && topMissReason.length() > 0) {
                sb.append("｜错因=");
                sb.append(UiFormatter.missReasonText(topMissReason));
            }

            if (errorCount > 0) {
                sb.append("｜错误=");
                sb.append(errorCount);
            }

            return sb.toString();
        }
    }

    public void markRuntimePulse(DbStore store, String status, String detail) {
        if (store == null) return;
        store.setState("learning_pump_state", status == null ? "" : status);
        store.setState("learning_pump_last_at", String.valueOf(System.currentTimeMillis()));
        store.setState("learning_pump_detail", detail == null ? "" : detail);
    }
}
