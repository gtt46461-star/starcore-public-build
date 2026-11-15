package com.ethk3.novaonepro;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * TeacherNumberMatrixEngine.java
 *
 * <p>V20 教师号码矩阵 + 相似窗口 + 五级分层闭环修复版（第2/6）
 *
 * <p>核心修复： 1. 后台教师号码矩阵不是没跑，而是实时 ComboCandidate 字段桥没接完整。 2. AutoFetchService 实时主链传入的是
 * Models.ComboCandidate，不是 Models.CandidateK3。 3. 必须把 teacherMatrixSample / teacherMatrixHitRate /
 * teacherMatrixConfidence / teacherMatrixStatus / teacherMatrixSummary 等字段写回 ComboCandidate。 4. 同时写
 * teacherNumber*、teacher*、teacherMatrix* 三套别名，兼容 DecisionGateEngine / ReasonEngine / MainActivity。
 * 5. 第11/14升级：重定义 STRONG / READY / WEAK / RISK / FREEZE，按样本可信、近期有效、前推命中、反证贡献动态降权。 6.
 * 第11/14升级：候选、CauseChain、诊断缓存统一写 teacherRecentValidity / teacherSampleReliability /
 * teacherEvidenceContribution / teacherCounterContribution。
 *
 * <p>红线： - 不创建BET。 - 不结算。 - 不写真实盈亏。 - 不写真实校准。 - 不降低最终门槛。 - 不绕过 DecisionGateEngine。 -
 * 教师矩阵只做候选画像、解释、排序辅助、小幅概率/门槛建议字段。 - READY/STRONG 只提高入围可信，不直接BET；RISK/FREEZE 只给最终门风险信号。
 */
public final class TeacherNumberMatrixEngine {
    public static final String AIDE_COMPILE_FIX_PATCH = "64130852";
    public static final String MODULE_BUILD_VERSION = AppConfig.ACTIVE_QUALITY_BUILD_VERSION;
    public static final String OUMAN_GALAXY_REBUILD_VERSION =
            AppConfig.OUMAN_GALAXY_REBUILD_VERSION;

    public static final String DEADLOCK_RECOVERY_BUILD =
            "V44_NOVA_DIAGNOSIS_FREQUENCY_STABILITY_20260720_44157296";

    private static final int POSITION_MIN = 1;
    private static final int POSITION_MAX = 10;
    private static final int NUMBER_MIN = 1;
    private static final int NUMBER_MAX = 10;

    private static final double BASE_NUMBER_RATE = 0.10d;
    private static final double BASE_K3_RATE = 0.30d;

    private static final int HISTORY_WINDOW_DEFAULT = 240;
    private static final int HISTORY_WINDOW_RECENT = 60;
    private static final int HISTORY_WINDOW_MIN = 30;

    private static final double MAX_PROB_ADJUST = 0.0d;
    private static final double MAX_GATE_ADJUST = 0.0d;
    private static final double SIMILAR_WINDOW_MAX_DISTANCE = 0.32d;
    private static final String V20_TEACHER_VERSION = AppConfig.ACTIVE_QUALITY_BUILD_VERSION;
    private static final String V20_TEACHER_UPSTREAM_VERSION =
            AppConfig.ACTIVE_QUALITY_BUILD_VERSION;
    private static final String LINK_MODIFICATION_V2_TEACHER_VERSION =
            "V44_NOVA_DIAGNOSIS_FREQUENCY_STABILITY_20260720_44157296";
    private static final Object TEACHER_MAP_CACHE_LOCK = new Object();
    private static LinkedHashMap<String, Models.NumberTeacherCell> cachedTeacherMapV5;
    private static String cachedTeacherMapTargetV5 = "";
    private static long cachedTeacherMapAtV5;
    private static final long TEACHER_MAP_CACHE_TTL_MS_V5 = 30L * 60L * 1000L;
    private static final int REALTIME_TEACHER_CANDIDATE_LIMIT =
            AppConfig.REALTIME_PROBABILITY_ENRICH_LIMIT;
    private static final long REALTIME_TEACHER_BUDGET_MS = 900L;
    private static final int TEACHER_READY_MIN_SAMPLE = 30;
    private static final int TEACHER_STABLE_MIN_SAMPLE = 60;
    private static final int TEACHER_STRONG_MIN_SAMPLE = 120;
    private static final int TEACHER_FREEZE_MIN_SAMPLE = 60;
    private static final double TEACHER_STRONG_MIN_RATE = 0.365d;
    private static final double TEACHER_READY_MIN_RATE = 0.330d;
    private static final double TEACHER_RISK_RATE = 0.275d;
    private static final double NUMBER_READY_MIN_RATE = 0.108d;
    private static final double NUMBER_STRONG_MIN_RATE = 0.122d;
    private static final double NUMBER_RISK_RATE = 0.084d;
    private static final double COMBO_READY_MIN_RATE = 0.330d;
    private static final double COMBO_STRONG_MIN_RATE = 0.365d;
    private static final double COMBO_RISK_RATE = 0.285d;
    private static final double TEACHER_WEAK_GATE_PENALTY = 0.006d;
    private static final double TEACHER_RISK_GATE_PENALTY = 0.010d;
    private static final double TEACHER_FREEZE_GATE_PENALTY = 0.016d;
    private static final String STATUS_NO_SAMPLE = "NO_SAMPLE_NEUTRAL";
    private static final String STATUS_WEAK = "WEAK";
    private static final String STATUS_READY = "READY";
    private static final String STATUS_STRONG = "STRONG";
    private static final String STATUS_RISK = "RISK";
    private static final String STATUS_FREEZE = "FREEZE";
    private static final String STATUS_WATCH = "WATCH";

    public TeacherNumberMatrixEngine() {}

    public ArrayList<Models.ComboCandidate> applyToAll(
            DbStore store,
            ArrayList<Models.ComboCandidate> candidates,
            List<Models.Draw> history,
            String baseIssue,
            String targetIssue) {
        return applyToComboCandidatesRealtime(
                store, candidates, history, baseIssue, targetIssue, "applyToAll_upstream_v20");
    }

    public ArrayList<Models.ComboCandidate> enrichCandidates(
            DbStore store,
            ArrayList<Models.ComboCandidate> candidates,
            List<Models.Draw> history,
            String baseIssue,
            String targetIssue) {
        return applyToComboCandidatesRealtime(
                store,
                candidates,
                history,
                baseIssue,
                targetIssue,
                "enrichCandidates_upstream_v20");
    }

    public ArrayList<Models.ComboCandidate> analyzeCandidates(
            DbStore store,
            ArrayList<Models.ComboCandidate> candidates,
            List<Models.Draw> history,
            String baseIssue,
            String targetIssue) {
        return applyToComboCandidatesRealtime(
                store,
                candidates,
                history,
                baseIssue,
                targetIssue,
                "analyzeCandidates_upstream_v20");
    }

    public ArrayList<Models.ComboCandidate> applyRealtime(
            ArrayList<Models.ComboCandidate> candidates,
            List<Models.Draw> history,
            String baseIssue,
            String targetIssue) {
        return applyToComboCandidatesRealtime(
                null, candidates, history, baseIssue, targetIssue, "applyRealtime_upstream_v20");
    }

    public ArrayList<Models.ComboCandidate> runRealtime(
            ArrayList<Models.ComboCandidate> candidates,
            List<Models.Draw> history,
            String baseIssue,
            String targetIssue) {
        return applyToComboCandidatesRealtime(
                null, candidates, history, baseIssue, targetIssue, "runRealtime_upstream_v20");
    }

    public ArrayList<Models.ComboCandidate> apply(
            DbStore store,
            List candidates,
            List history,
            List vectors,
            List traces,
            List recoveries,
            String baseIssue,
            String targetIssue) {
        return applyToComboCandidatesRealtime(
                store, candidates, history, baseIssue, targetIssue, "apply_full_v20");
    }

    public ArrayList<Models.ComboCandidate> apply(
            DbStore store, List candidates, List history, List traces, List recoveries) {
        String baseIssue = resolveBaseIssue(history);
        String targetIssue = buildNextIssue(baseIssue);
        return applyToComboCandidatesRealtime(
                store, candidates, history, baseIssue, targetIssue, "apply_mid_v20");
    }

    public ArrayList<Models.ComboCandidate> applyToCandidates(
            DbStore store, List candidates, List history, List traces, List recoveries) {
        String baseIssue = resolveBaseIssue(history);
        String targetIssue = buildNextIssue(baseIssue);
        return applyToComboCandidatesRealtime(
                store, candidates, history, baseIssue, targetIssue, "applyToCandidates_realtime");
    }

    public ArrayList<Models.ComboCandidate> enrich(
            DbStore store, List candidates, List history, List traces, List recoveries) {
        String baseIssue = resolveBaseIssue(history);
        String targetIssue = buildNextIssue(baseIssue);
        return applyToComboCandidatesRealtime(
                store, candidates, history, baseIssue, targetIssue, "enrich_realtime");
    }

    public ArrayList<Models.ComboCandidate> run(DbStore store, List candidates, List history) {
        String baseIssue = resolveBaseIssue(history);
        String targetIssue = buildNextIssue(baseIssue);
        return applyToComboCandidatesRealtime(
                store, candidates, history, baseIssue, targetIssue, "run_realtime");
    }

    public TeacherRunReport learnLatestDraw(DbStore store, Models.Draw draw) {
        return learnAfterDraw(store, draw);
    }

    public TeacherRunReport learnAfterDraw(DbStore store, Models.Draw draw) {
        return runBackgroundMatrixUpdate(store, draw, "learnAfterDraw");
    }

    public TeacherRunReport learnAfterNewDraw(DbStore store, Models.Draw draw) {
        return runBackgroundMatrixUpdate(store, draw, "learnAfterNewDraw");
    }

    public TeacherRunReport onDrawSaved(DbStore store, Models.Draw draw) {
        return runBackgroundMatrixUpdate(store, draw, "onDrawSaved");
    }

    public TeacherRunReport runAfterDraw(DbStore store, Models.Draw draw) {
        return runBackgroundMatrixUpdate(store, draw, "runAfterDraw");
    }

    public TeacherRunReport learnOneDraw(DbStore store, Models.Draw draw) {
        return runBackgroundMatrixUpdate(store, draw, "learnOneDraw");
    }

    public TeacherRunReport learnLatest(DbStore store) {
        Models.Draw latest = null;

        try {
            if (store != null) {
                latest = store.getLatestDraw();
            }
        } catch (Throwable e) {
            latest = null;
        }

        return runBackgroundMatrixUpdate(store, latest, "learnLatest");
    }

    public TeacherRunReport runAutoLearnBatch(DbStore store, int batchSize) {
        return runAutoBatch(store, batchSize, "runAutoLearnBatch");
    }

    public TeacherRunReport learnAutoSmallBatch(DbStore store, int batchSize) {
        return runAutoBatch(store, batchSize, "learnAutoSmallBatch");
    }

    public TeacherRunReport runSmallBatch(DbStore store, int batchSize) {
        return runAutoBatch(store, batchSize, "runSmallBatch");
    }

    public TeacherRunReport runAutoLearnBatch(DbStore store) {
        return runAutoBatch(store, 1, "runAutoLearnBatch");
    }

    public TeacherRunReport runTick(DbStore store) {
        return runAutoBatch(store, 1, "runTick");
    }

    public TeacherRunReport tick(DbStore store) {
        return runAutoBatch(store, 1, "tick");
    }

    public Map<String, Models.NumberTeacherCell> buildTeacherNumberMap(
            String baseIssue,
            String targetIssue,
            Map<String, Models.PreDrawFeature> featureMap,
            Map<Integer, Models.PositionHealth> positionHealthMap,
            List<Models.CauseChain> chains,
            List<Models.CandidateK3> candidates,
            List<Models.LearningCase> learningCases) {
        LinkedHashMap<String, Models.NumberTeacherCell> out =
                new LinkedHashMap<String, Models.NumberTeacherCell>();

        for (int p = POSITION_MIN; p <= POSITION_MAX; p++) {
            for (int n = NUMBER_MIN; n <= NUMBER_MAX; n++) {
                Models.NumberTeacherCell cell =
                        buildOneCell(
                                baseIssue,
                                targetIssue,
                                p,
                                n,
                                featureMap,
                                positionHealthMap,
                                chains,
                                candidates,
                                learningCases);

                putAllCellKeys(out, targetIssue, p, n, cell);
            }
        }

        return out;
    }

    public Map<String, Models.NumberTeacherCell> buildTeacherNumberMap(
            String baseIssue,
            String targetIssue,
            Map<String, Models.PreDrawFeature> featureMap,
            Map<Integer, Models.PositionHealth> positionHealthMap) {
        return buildTeacherNumberMap(
                baseIssue, targetIssue, featureMap, positionHealthMap, null, null, null);
    }

    public ArrayList<Models.NumberTeacherCell> buildTeacherNumberList(
            String baseIssue,
            String targetIssue,
            Map<String, Models.PreDrawFeature> featureMap,
            Map<Integer, Models.PositionHealth> positionHealthMap,
            List<Models.CauseChain> chains,
            List<Models.CandidateK3> candidates,
            List<Models.LearningCase> learningCases) {
        Map<String, Models.NumberTeacherCell> map =
                buildTeacherNumberMap(
                        baseIssue,
                        targetIssue,
                        featureMap,
                        positionHealthMap,
                        chains,
                        candidates,
                        learningCases);

        ArrayList<Models.NumberTeacherCell> list = new ArrayList<Models.NumberTeacherCell>();

        if (map != null) {
            for (Models.NumberTeacherCell c : map.values()) {
                if (c != null && !containsSameCell(list, c)) {
                    list.add(c);
                }
            }
        }

        sortTeacherCells(list);
        return list;
    }

    public Models.NumberTeacherCell buildOneCell(
            String baseIssue,
            String targetIssue,
            int position,
            int number,
            Map<String, Models.PreDrawFeature> featureMap,
            Map<Integer, Models.PositionHealth> positionHealthMap,
            List<Models.CauseChain> chains,
            List<Models.CandidateK3> candidates,
            List<Models.LearningCase> learningCases) {
        Models.NumberTeacherCell cell = new Models.NumberTeacherCell();

        putString(cell, "cellKey", buildCellKey(targetIssue, position, number));
        putString(cell, "profileKey", buildProfileKey(position, number));
        putString(cell, "baseIssue", safe(baseIssue));
        putString(cell, "targetIssue", safe(targetIssue));
        putLong(cell, "createdAt", System.currentTimeMillis());
        putInt(cell, "position", position);
        putInt(cell, "number", number);

        if (!isValidPosition(position) || !isValidNumber(number)) {
            putString(cell, "status", "INVALID");
            putDouble(cell, "teacherScore", 0.0d);
            putDouble(cell, "teacherRisk", 1.0d);
            putDouble(cell, "confidence", 0.0d);
            putString(cell, "summaryText", "教师号码矩阵：位置或号码非法");
            return cell;
        }

        Models.PreDrawFeature f = getFeature(featureMap, position, number);
        Models.PositionHealth h =
                positionHealthMap == null ? null : positionHealthMap.get(Integer.valueOf(position));

        TeacherStat stat =
                calcTeacherStat(position, number, f, h, chains, candidates, learningCases);

        String status = statusOf(stat);
        String summary = buildSummary(position, number, stat, status);

        fillCellFromStat(cell, baseIssue, targetIssue, position, number, stat, status, summary);

        return cell;
    }

    public void applyToCandidates(
            List<Models.CandidateK3> candidates, Map<String, Models.NumberTeacherCell> teacherMap) {
        if (candidates == null || candidates.size() == 0) {
            return;
        }

        for (int i = 0; i < candidates.size(); i++) {
            Models.CandidateK3 c = candidates.get(i);

            if (c == null) {
                continue;
            }

            applyToCandidate(c, teacherMap);
        }

        rerankCandidates(candidates);
    }

    public void applyToCandidate(
            Models.CandidateK3 c, Map<String, Models.NumberTeacherCell> teacherMap) {
        if (c == null) {
            return;
        }

        int position = getInt(c, "position", 0);
        int n1 = getInt(c, "n1", 0);
        int n2 = getInt(c, "n2", 0);
        int n3 = getInt(c, "n3", 0);

        if (!isValidPosition(position)
                || !isValidNumber(n1)
                || !isValidNumber(n2)
                || !isValidNumber(n3)) {
            putDouble(c, "teacherNumberScore", 0.0d);
            putDouble(c, "teacherNumberRisk", 1.0d);
            putString(c, "teacherNumberStatus", "INVALID");
            return;
        }

        Models.NumberTeacherCell c1 = getCell(teacherMap, position, n1);
        Models.NumberTeacherCell c2 = getCell(teacherMap, position, n2);
        Models.NumberTeacherCell c3 = getCell(teacherMap, position, n3);

        ComboTeacherResult r = evalCombo(position, n1, n2, n3, c1, c2, c3);

        double oldCause = getDouble(c, "causeScore", 0.0d);
        double oldRisk = getDouble(c, "riskScore", 0.0d);
        double oldFinalP = getDouble(c, "finalP", 0.0d);

        double mergedCause = clamp01(oldCause * 0.84d + r.comboScore * 0.16d);
        double mergedRisk = clamp01(Math.max(oldRisk, r.comboRisk));

        writeTeacherFieldsToAnyCandidate(c, r, oldFinalP, mergedCause, mergedRisk);
        appendReason(c, r.summary);
    }

    public void applyToCauseChains(
            List<Models.CauseChain> chains, Map<String, Models.NumberTeacherCell> teacherMap) {
        if (chains == null || chains.size() == 0) {
            return;
        }

        for (int i = 0; i < chains.size(); i++) {
            Models.CauseChain chain = chains.get(i);

            if (chain == null) {
                continue;
            }

            applyToCauseChain(chain, teacherMap);
        }
    }

    public void applyToCauseChain(
            Models.CauseChain chain, Map<String, Models.NumberTeacherCell> teacherMap) {
        if (chain == null) {
            return;
        }

        int position = getInt(chain, "position", 0);
        int number = getInt(chain, "number", 0);

        if (!isValidPosition(position) || !isValidNumber(number)) {
            return;
        }

        Models.NumberTeacherCell cell = getCell(teacherMap, position, number);

        if (cell == null) {
            return;
        }

        double teacherScore =
                getDouble(
                        cell,
                        "teacherScore",
                        getDouble(
                                cell, "numberTeacherScore", getDouble(cell, "supportScore", 0.0d)));
        double teacherRisk =
                getDouble(
                        cell,
                        "teacherRisk",
                        getDouble(cell, "numberTeacherRisk", getDouble(cell, "riskScore", 0.0d)));
        double confidence = getDouble(cell, "confidence", 0.0d);

        double teacherCause =
                clampSigned((teacherScore - teacherRisk) * (0.60d + confidence * 0.40d));
        double oldNumberCause = getDouble(chain, "numberCause", 0.0d);
        double oldFinal = getDouble(chain, "finalCauseScore", getDouble(chain, "causeScore", 0.0d));
        double oldRisk = getDouble(chain, "riskScore", 0.0d);

        double mergedNumberCause = clampSigned(oldNumberCause * 0.68d + teacherCause * 0.32d);
        double newFinal =
                clamp01(
                        oldFinal
                                + posPart(mergedNumberCause) * 0.045d
                                - negPart(mergedNumberCause) * 0.055d);
        double newRisk = clamp01(Math.max(oldRisk, teacherRisk * 0.72d));

        putDouble(chain, "teacherNumberScore", teacherScore);
        putDouble(chain, "teacherMatrixScore", teacherScore);
        putDouble(chain, "teacherNumberRisk", teacherRisk);
        putDouble(chain, "teacherMatrixRisk", teacherRisk);
        putDouble(chain, "teacherNumberConfidence", confidence);
        putDouble(chain, "teacherMatrixConfidence", confidence);
        putString(
                chain,
                "teacherMatrixStatus",
                getString(cell, "teacherMatrixStatus", getString(cell, "status", "")));
        putString(
                chain,
                "teacherStatus",
                getString(cell, "teacherMatrixStatus", getString(cell, "status", "")));
        putDouble(
                chain,
                "teacherEvidenceContribution",
                getDouble(cell, "evidenceContribution", 0.0d));
        putDouble(
                chain, "teacherCounterContribution", getDouble(cell, "counterContribution", 0.0d));
        putDouble(
                chain,
                "teacherSampleReliability",
                getDouble(cell, "sampleReliability", getDouble(cell, "sampleTrust", 0.0d)));
        putDouble(
                chain,
                "teacherRecentValidity",
                getDouble(cell, "recentValidity", getDouble(cell, "recentEffectiveness", 0.0d)));
        putString(chain, "teacherActionAdvice", getString(cell, "actionAdvice", ""));
        putDouble(chain, "numberCause", mergedNumberCause);
        putDouble(chain, "finalCauseScore", newFinal);
        putDouble(chain, "causeScore", newFinal);
        putDouble(chain, "riskScore", newRisk);

        String old = getString(chain, "summaryText", "");
        String add =
                "｜教师号码矩阵：号码="
                        + number
                        + "，教师分="
                        + percent(teacherScore)
                        + "，风险="
                        + percent(teacherRisk)
                        + "，置信="
                        + percent(confidence)
                        + "，号码因="
                        + signed(mergedNumberCause);

        putString(chain, "summaryText", old + add);
    }

    public Models.NumberTeacherCell getCell(
            Map<String, Models.NumberTeacherCell> map, int position, int number) {
        if (map == null) {
            return null;
        }

        Models.NumberTeacherCell c = map.get(keyOf(position, number));

        if (c != null) {
            return c;
        }

        c = map.get(String.valueOf(position) + "|" + String.valueOf(number));

        if (c != null) {
            return c;
        }

        c = map.get("P" + position + "_N" + number);

        if (c != null) {
            return c;
        }

        c = map.get(buildProfileKey(position, number));

        return c;
    }

    public static String keyOf(int position, int number) {
        return String.valueOf(position) + "_" + String.valueOf(number);
    }

    private ArrayList<Models.ComboCandidate> applyToComboCandidatesRealtime(
            DbStore store,
            List rawCandidates,
            List rawHistory,
            String baseIssue,
            String targetIssue,
            String source) {
        ArrayList<Models.ComboCandidate> candidates = castComboCandidates(rawCandidates);
        ArrayList<Models.Draw> history = castDrawList(rawHistory);
        try {
            if (store != null)
                store.registerModuleBuildV20(
                        "TeacherNumberMatrixEngine", MODULE_BUILD_VERSION, source);
        } catch (Throwable ignore) {
        }

        if (baseIssue == null || baseIssue.length() == 0) {
            baseIssue = resolveBaseIssue(history);
        }

        if (targetIssue == null || targetIssue.length() == 0) {
            targetIssue = buildNextIssue(baseIssue);
        }

        if (candidates.size() == 0) {
            writeStoreState(store, "teacher_number_matrix_summary", "教师号码矩阵跳过：实时候选为空");
            writeStoreState(store, "teacher_matrix_candidate_apply", "0");
            writeStoreState(store, "teacher_matrix_candidate_summary", "教师矩阵候选字段为空，中性处理");
            return candidates;
        }

        if (history.size() < HISTORY_WINDOW_MIN && store != null) {
            try {
                List<Models.Draw> more = store.getRecentDraws(HISTORY_WINDOW_DEFAULT);
                history = castDrawList(more);
            } catch (Throwable e) {
            }
        }

        LinkedHashMap<String, Models.NumberTeacherCell> teacherMap =
                loadTeacherMapForRealtime(store, baseIssue, targetIssue, history);

        long realtimeStartedAt = System.currentTimeMillis();
        int applied = 0;
        int deferred = 0;
        int risk = 0;
        int strong = 0;
        int sampleTotal = 0;
        int fieldWritten = 0;
        String bestSummary = "";

        for (int i = 0; i < candidates.size(); i++) {
            Models.ComboCandidate c = candidates.get(i);
            stampTeacherSnapshotMeta73487246(store, c);

            if (c == null) {
                continue;
            }
            if (i >= REALTIME_TEACHER_CANDIDATE_LIMIT
                    || System.currentTimeMillis() - realtimeStartedAt
                            > REALTIME_TEACHER_BUDGET_MS) {
                writeDeferredTeacherWatch(c, baseIssue, targetIssue, i + 1);
                deferred++;
                continue;
            }

            boolean ok = applyToComboCandidate(c, teacherMap);
            int historySample = getInt(c, "teacherMatrixSample", 0);
            putInt(c, "teacherNumberHistorySample", historySample);
            putString(c, "teacherRuntimeApplyVersion", MODULE_BUILD_VERSION);
            putString(c, "moduleBuildVersion", MODULE_BUILD_VERSION);
            if (store != null) applySettledDomainTeacherEvidence(store, c);

            if (ok) {
                applied++;

                int sample = getInt(c, "teacherMatrixSample", 0);
                sampleTotal += sample;

                String status = getString(c, "teacherMatrixStatus", "");

                if ("RISK".equals(status)) {
                    risk++;
                }

                if ("STRONG".equals(status)) {
                    strong++;
                }

                if (getString(c, "teacherMatrixSummary", "").length() > 0) {
                    fieldWritten++;
                }

                if (bestSummary.length() == 0) {
                    bestSummary = getString(c, "teacherMatrixSummary", "");
                }
            }
        }

        rerankComboCandidates(candidates);

        int avgSample = applied <= 0 ? 0 : sampleTotal / applied;

        String summary =
                "教师号码矩阵实时反哺完成：入口="
                        + source
                        + "｜目标="
                        + safe(targetIssue)
                        + "｜候选="
                        + candidates.size()
                        + "｜应用="
                        + applied
                        + "｜字段写入="
                        + fieldWritten
                        + "｜平均样本="
                        + avgSample
                        + "｜强="
                        + strong
                        + "｜风险="
                        + risk
                        + "｜延后="
                        + deferred
                        + "｜耗时="
                        + (System.currentTimeMillis() - realtimeStartedAt)
                        + "ms｜说明=已把 teacherMatrix/teacherNumber/teacher"
                        + " 三套字段写回实时ComboCandidate，不创建BET，不越过最终门";

        if (bestSummary.length() > 0) {
            summary = summary + "｜首候选=" + shortText(bestSummary, 160);
        }

        writeRealtimeState(store, summary, applied, avgSample, strong, risk, fieldWritten);
        if (store != null) {
            store.setState("teacher_typed_input_count", String.valueOf(candidates.size()));
            store.setState("teacher_typed_applied_count", String.valueOf(applied));
            store.setState("teacher_typed_average_history_sample", String.valueOf(avgSample));
            store.setState("teacher_typed_error_count", "0");
            store.setState("teacher_typed_runtime_version", MODULE_BUILD_VERSION);
            String stageKey = source == null ? "UNKNOWN" : source.replaceAll("[^A-Za-z0-9_]", "_");
            store.setState(
                    "teacher_stage_" + stageKey + "_input", String.valueOf(candidates.size()));
            store.setState("teacher_stage_" + stageKey + "_applied", String.valueOf(applied));
            store.setState("teacher_stage_" + stageKey + "_nonzero", String.valueOf(fieldWritten));
            store.setState(
                    "teacher_stage_" + stageKey + "_avg_history_sample", String.valueOf(avgSample));
        }

        return candidates;
    }

    private boolean applyToComboCandidate(
            Models.ComboCandidate c, Map<String, Models.NumberTeacherCell> teacherMap) {
        if (c == null) {
            return false;
        }

        int position = getInt(c, "position", 0);
        int n1 = getInt(c, "n1", 0);
        int n2 = getInt(c, "n2", 0);
        int n3 = getInt(c, "n3", 0);

        if (!isValidPosition(position)
                || !isValidNumber(n1)
                || !isValidNumber(n2)
                || !isValidNumber(n3)) {
            putDouble(c, "teacherMatrixScore", 0.0d);
            putDouble(c, "teacherNumberScore", 0.0d);
            putDouble(c, "teacherMatrixRisk", 1.0d);
            putDouble(c, "teacherNumberRisk", 1.0d);
            putDouble(c, "teacherMatrixConfidence", 0.0d);
            putDouble(c, "teacherNumberConfidence", 0.0d);
            putInt(c, "teacherMatrixSample", 0);
            putInt(c, "teacherNumberSample", 0);
            putString(c, "teacherMatrixStatus", "INVALID");
            putString(c, "teacherNumberStatus", "INVALID");
            putString(c, "teacherMatrixSummary", "教师矩阵候选字段非法：位置或K3号码无效");
            putString(c, "teacherNumberSummary", "教师矩阵候选字段非法：位置或K3号码无效");
            return false;
        }

        Models.NumberTeacherCell c1 = getCell(teacherMap, position, n1);
        Models.NumberTeacherCell c2 = getCell(teacherMap, position, n2);
        Models.NumberTeacherCell c3 = getCell(teacherMap, position, n3);

        ComboTeacherResult r = evalCombo(position, n1, n2, n3, c1, c2, c3);

        /*
         * 73487246 AIDE编译修复：层级历史回退只属于富字段 ComboCandidate。
         * CandidateK3 是轻量扫描对象，不包含 historicalPrior/teacherMatrix 全字段，
         * 因此禁止在 applyToCandidate(CandidateK3) 中直接访问这些字段。
         */
        if (r.sample <= 0 && c.historicalPriorSample > 0) {
            c.teacherNumberHistorySample =
                    Math.max(c.teacherNumberHistorySample, c.historicalPriorSample);
            c.teacherNumberSample = Math.max(c.teacherNumberSample, c.historicalPriorSample);
            c.teacherMatrixSample = Math.max(c.teacherMatrixSample, c.historicalPriorSample);
            c.teacherMatrixStateSample =
                    Math.max(c.teacherMatrixStateSample, c.historicalPriorSample);
            c.teacherMatrixApplied = 1;
            c.teacherNumberHitRate = c.historicalPriorP;
            c.teacherMatrixHitRate = c.historicalPriorP;
            c.teacherMatrixRecentHitRate = c.historicalPriorP;
            c.teacherNumberConfidence =
                    Math.max(c.teacherNumberConfidence, c.historicalPriorConfidence);
            c.teacherMatrixConfidence =
                    Math.max(c.teacherMatrixConfidence, c.historicalPriorConfidence);
            double historyScore = clamp01(0.50d + (c.historicalPriorP - 0.30d) * 4.0d);
            double historyRisk = clamp01(0.50d + (0.30d - c.historicalPriorP) * 4.0d);
            c.teacherNumberScore = Math.max(c.teacherNumberScore, historyScore);
            c.teacherMatrixScore = Math.max(c.teacherMatrixScore, historyScore);
            c.teacherNumberRisk = Math.max(c.teacherNumberRisk, historyRisk);
            c.teacherMatrixRiskPenalty =
                    Math.max(
                            c.teacherMatrixRiskPenalty,
                            Math.max(0.0d, historyRisk - 0.50d) * 0.10d);
            c.teacherMatrixProbAdjust = 0.0d;
            c.teacherMatrixGateAdjust = 0.0d;
            c.teacherMatrixStatus =
                    c.historicalPriorSample >= 60
                            ? "HIERARCHICAL_HISTORY_READY"
                            : "HIERARCHICAL_HISTORY_WARM";
            c.teacherNumberStatus = c.teacherMatrixStatus;
            c.teacherStatus = c.teacherMatrixStatus;
            c.teacherStatusClass = c.historicalPriorP >= 0.30d ? "READY" : "WATCH";
            c.teacherFieldSource = "HISTORICAL_STATE_PRIOR_FALLBACK";
            c.teacherFieldVersion = MODULE_BUILD_VERSION;
            c.teacherRuntimeApplyVersion = MODULE_BUILD_VERSION;
            c.teacherMatrixSummary =
                    appendText(c.teacherMatrixSummary, "细域教师无样本，已回退有限大类/中类历史先验，未清零");
            c.teacherNumberSummary = appendText(c.teacherNumberSummary, c.teacherMatrixSummary);
            return true;
        }

        double oldCause = getDouble(c, "causeScore", 0.0d);
        double oldRisk = getDouble(c, "riskScore", 0.0d);
        double oldFinalP = getDouble(c, "finalP", 0.0d);

        double mergedCause = clamp01(oldCause * 0.86d + r.comboScore * 0.14d);
        double mergedRisk = clamp01(Math.max(oldRisk, r.comboRisk * 0.92d));

        writeTeacherFieldsToAnyCandidate(c, r, oldFinalP, mergedCause, mergedRisk);

        String oldReason = getString(c, "reasonSummary", "");
        if (oldReason.length() == 0) {
            putString(c, "reasonSummary", r.summary);
        } else if (oldReason.indexOf("教师号码矩阵") < 0 && oldReason.indexOf("教师矩阵") < 0) {
            putString(c, "reasonSummary", oldReason + "｜" + r.summary);
        }

        String oldDeep = getString(c, "deepReason", "");
        if (oldDeep.length() == 0) {
            putString(c, "deepReason", r.summary);
        } else if (oldDeep.indexOf("教师号码矩阵") < 0 && oldDeep.indexOf("教师矩阵") < 0) {
            putString(c, "deepReason", oldDeep + "｜" + r.summary);
        }

        return true;
    }

    private void writeTeacherFieldsToAnyCandidate(
            Object c,
            ComboTeacherResult r,
            double oldFinalP,
            double mergedCause,
            double mergedRisk) {
        if (c == null || r == null) {
            return;
        }

        String teacherStatus = normalizeTeacherStatus(r.status);
        int readyFlag = ("STRONG".equals(teacherStatus) || "READY".equals(teacherStatus)) ? 1 : 0;
        int watchFlag = "WATCH".equals(teacherStatus) ? 1 : 0;
        int noSampleFlag = "NO_SAMPLE_NEUTRAL".equals(teacherStatus) ? 1 : 0;
        int weakNoBet =
                ("WEAK".equals(teacherStatus)
                                || "RISK".equals(teacherStatus)
                                || "FREEZE".equals(teacherStatus))
                        ? 1
                        : 0;
        int ordinaryBlock =
                ("RISK".equals(teacherStatus) || "FREEZE".equals(teacherStatus)) ? 1 : 0;
        int riskFlag = ("RISK".equals(teacherStatus) || "FREEZE".equals(teacherStatus)) ? 1 : 0;
        int freezeFlag = "FREEZE".equals(teacherStatus) ? 1 : 0;
        String teacherPolicy = buildTeacherOrdinaryBetPolicy(teacherStatus);

        double probAdjust =
                limitSigned(
                        (r.comboScore - r.comboRisk) * 0.020d * (0.45d + r.confidence * 0.55d),
                        MAX_PROB_ADJUST);
        double gateAdjust =
                limitSigned(
                        (r.comboRisk - r.comboScore) * 0.014d * (0.45d + r.confidence * 0.55d),
                        MAX_GATE_ADJUST);

        if ("NO_SAMPLE_NEUTRAL".equals(teacherStatus)) {
            probAdjust = 0.0d;
            gateAdjust = 0.0d;
        }
        if ("STRONG".equals(teacherStatus)) {
            gateAdjust = Math.min(gateAdjust, 0.0d);
        }

        if ("WEAK".equals(teacherStatus)) {
            probAdjust =
                    Math.min(
                            probAdjust,
                            r.evidenceContribution >= 0.52d && r.counterContribution <= 0.50d
                                    ? 0.0d
                                    : -0.004d);
            gateAdjust = Math.max(gateAdjust, TEACHER_WEAK_GATE_PENALTY);
        }
        if ("RISK".equals(teacherStatus) || "FREEZE".equals(teacherStatus)) {
            probAdjust = Math.min(probAdjust, 0.0d);
            gateAdjust = Math.max(gateAdjust, TEACHER_RISK_GATE_PENALTY);
        }
        if ("FREEZE".equals(teacherStatus)) {
            probAdjust = Math.min(probAdjust, -0.010d);
            gateAdjust = Math.max(gateAdjust, TEACHER_FREEZE_GATE_PENALTY);
        }

        putInt(c, "teacherMatrixApplied", 1);
        putInt(c, "teacherNumberApplied", 1);
        putInt(c, "teacherApplied", 1);
        putInt(c, "teacherMatrixReady", readyFlag);
        putInt(c, "teacherNumberReady", readyFlag);
        putInt(c, "teacherMatrixRiskFlag", riskFlag);
        putInt(c, "teacherNumberRiskFlag", riskFlag);
        putInt(c, "teacherMatrixFreezeFlag", freezeFlag);
        putInt(c, "teacherNumberFreezeFlag", freezeFlag);
        putInt(c, "teacherMatrixStrongFlag", "STRONG".equals(teacherStatus) ? 1 : 0);
        putInt(c, "teacherMatrixReadyFlag", "READY".equals(teacherStatus) ? 1 : 0);
        putInt(c, "teacherMatrixWatchFlag", watchFlag);
        putInt(c, "teacherMatrixNoSampleFlag", noSampleFlag);
        putInt(c, "teacherNoSampleNeutralFlag", noSampleFlag);
        putInt(c, "teacherMatrixNoSampleNeutralFlag", noSampleFlag);
        putInt(c, "teacherMatrixWeakFlag", "WEAK".equals(teacherStatus) ? 1 : 0);
        putInt(c, "teacherWeakFlag", "WEAK".equals(teacherStatus) ? 1 : 0);
        putInt(c, "teacherWeakNoBetFlag", weakNoBet);
        putInt(c, "teacherMatrixWeakNoBetFlag", weakNoBet);
        putInt(c, "teacherMatrixOrdinaryBetBlockFlag", ordinaryBlock);
        putInt(c, "teacherMatrixSupportsFinalGate", readyFlag);
        putInt(c, "teacherMatrixSupportsBet", readyFlag);
        putString(c, "teacherOrdinaryBetPolicy", teacherPolicy);
        putString(c, "teacherMatrixOrdinaryBetPolicy", teacherPolicy);
        putString(c, "teacherFieldSource", "TeacherNumberMatrixEngine.realtime");
        putString(c, "teacherFieldVersion", AppConfig.FREQUENCY_FLOOR_VERSION);
        putString(c, "evidenceSource", "TeacherNumberMatrixEngine.HISTORY_PRE_DRAW");
        putInt(c, "evidenceSettledOnly", 1);
        putString(c, "teacherStatusClass", teacherStatus);
        putString(c, "teacherStatusTier", teacherStatusTier(teacherStatus));
        putString(c, "teacherRiskClass", teacherRiskClass(teacherStatus));

        putDouble(c, "teacherNumberScore", r.comboScore);
        putDouble(c, "teacherComboScore", r.comboScore);
        putDouble(c, "teacherMatrixScore", r.comboScore);
        putDouble(c, "teacherScore", r.comboScore);

        putDouble(c, "teacherNumberRisk", r.comboRisk);
        putDouble(c, "teacherComboRisk", r.comboRisk);
        putDouble(c, "teacherMatrixRisk", r.comboRisk);
        putDouble(c, "teacherRisk", r.comboRisk);

        putDouble(c, "teacherNumberConfidence", r.confidence);
        putDouble(c, "teacherMatrixConfidence", r.confidence);
        putDouble(c, "teacherConfidence", r.confidence);

        putDouble(c, "teacherNumberHitRate", r.k3HitRate);
        putDouble(c, "teacherMatrixHitRate", r.k3HitRate);
        putDouble(c, "teacherHitRate", r.k3HitRate);

        putDouble(c, "teacherNumberAvgHitRate", r.avgHitRate);
        putDouble(c, "teacherMatrixAvgHitRate", r.avgHitRate);
        putDouble(c, "teacherNumberSupport", r.support);
        putDouble(c, "teacherMatrixSupport", r.support);
        putDouble(c, "teacherNumberPenalty", r.penalty);
        putDouble(c, "teacherMatrixPenalty", r.penalty);

        putInt(c, "teacherNumberSample", r.sample);
        putInt(c, "teacherMatrixSample", r.sample);
        putInt(c, "teacherSample", r.sample);
        putInt(c, "teacherNumberHistorySample", r.sample);
        putDouble(c, "teacherNumberHitRate", r.k3HitRate);
        putDouble(c, "teacherNumberConfidence", r.confidence);
        putDouble(c, "teacherNumberScore", r.comboScore);
        putDouble(c, "teacherNumberRisk", r.comboRisk);
        putString(c, "teacherNumberSummary", r.summary);
        putString(c, "teacherRuntimeApplyVersion", AppConfig.ALWAYS_ON_RUNTIME_FIX_VERSION);

        putString(c, "teacherNumberStatus", teacherStatus);
        putString(c, "teacherMatrixStatus", teacherStatus);
        putString(c, "teacherStatus", teacherStatus);
        putString(c, "teacherMatrixLevel", teacherStatus);
        putString(c, "teacherNumberLevel", teacherStatus);

        putString(c, "teacherNumberSummary", r.summary);
        putString(c, "teacherMatrixSummary", r.summary);
        putString(c, "teacherSummary", r.summary);
        putString(c, "teacherMatrixReason", r.summary);
        putString(c, "teacherNumberReason", r.summary);
        putString(c, "teacherMatrixDecisionText", r.summary);
        putString(c, "teacherMatrixRankReason", r.summary);
        putString(c, "teacherMatrixStateKey", r.stateKey);
        putString(c, "teacherWindowStateKey", r.stateKey);
        putString(c, "teacherEvidenceText", r.evidenceText);
        putString(c, "teacherCounterEvidenceText", r.counterText);
        putString(c, "teacherActionAdvice", r.actionAdvice);
        putString(c, "teacherMatrixActionAdvice", r.actionAdvice);
        putString(c, "teacherNumberActionAdvice", r.actionAdvice);

        putDouble(c, "teacherEvidenceContribution", r.evidenceContribution);
        putDouble(c, "teacherMatrixEvidenceContribution", r.evidenceContribution);
        putDouble(c, "teacherNumberEvidenceContribution", r.evidenceContribution);
        putDouble(c, "teacherCounterContribution", r.counterContribution);
        putDouble(c, "teacherMatrixCounterContribution", r.counterContribution);
        putDouble(c, "teacherNumberCounterContribution", r.counterContribution);

        putDouble(c, "teacherSampleTrust", r.sampleTrust);
        putDouble(c, "teacherSampleReliability", r.sampleTrust);
        putDouble(c, "teacherMatrixSampleTrust", r.sampleTrust);
        putDouble(c, "teacherMatrixSampleReliability", r.sampleTrust);
        putDouble(c, "teacherRecentEffectiveness", r.recentEffectiveness);
        putDouble(c, "teacherRecentValidity", r.recentEffectiveness);
        putDouble(c, "teacherMatrixRecentEffectiveness", r.recentEffectiveness);
        putDouble(c, "teacherMatrixRecentValidity", r.recentEffectiveness);
        putDouble(c, "teacherFeatureDistance", r.featureDistance);
        putInt(c, "teacherSimilarWindowSample", r.similarWindowSample);
        putDouble(c, "teacherSimilarWindowHitRate", r.similarWindowHitRate);
        putString(c, "teacherMatrixVersion", V20_TEACHER_UPSTREAM_VERSION);
        putString(c, "teacherNumberVersion", V20_TEACHER_UPSTREAM_VERSION);

        putDouble(c, "teacherNumberProbAdjust", probAdjust);
        putDouble(c, "numberTeacherProbAdjust", probAdjust);
        putDouble(c, "teacherMatrixProbAdjust", probAdjust);
        putDouble(c, "teacherProbAdjust", probAdjust);
        putDouble(c, "teacherMatrixProbBoost", probAdjust);
        putDouble(
                c,
                "teacherDiagnosisAdjustedP",
                oldFinalP > 0.0d ? clamp01(oldFinalP + probAdjust) : 0.0d);

        putDouble(c, "teacherNumberGateAdjust", gateAdjust);
        putDouble(c, "teacherMatrixGateAdjust", gateAdjust);
        putDouble(c, "teacherGateAdjust", gateAdjust);

        putDouble(c, "causeScore", mergedCause);
        putDouble(c, "riskScore", mergedRisk);

        if (oldFinalP > 0.0d) {
            putDouble(c, "teacherNumberPreviewP", clamp01(oldFinalP + probAdjust));
            putDouble(c, "teacherMatrixPreviewP", clamp01(oldFinalP + probAdjust));
        }
    }

    private LinkedHashMap<String, Models.NumberTeacherCell> buildTeacherMapFromHistory(
            String baseIssue, String targetIssue, List<Models.Draw> history) {
        LinkedHashMap<String, Models.NumberTeacherCell> map =
                new LinkedHashMap<String, Models.NumberTeacherCell>();

        int[][] hit = new int[POSITION_MAX + 1][NUMBER_MAX + 1];
        int[][] recentHit = new int[POSITION_MAX + 1][NUMBER_MAX + 1];
        int[] sample = new int[POSITION_MAX + 1];
        int[] recentSample = new int[POSITION_MAX + 1];

        ArrayList<Models.Draw> clean = normalizeHistory(history);

        int startRecent = clean.size() - HISTORY_WINDOW_RECENT;
        if (startRecent < 0) {
            startRecent = 0;
        }

        for (int i = 0; i < clean.size(); i++) {
            Models.Draw d = clean.get(i);

            if (d == null || !d.isValid()) {
                continue;
            }

            boolean recent = i >= startRecent;

            for (int p = POSITION_MIN; p <= POSITION_MAX; p++) {
                int n = getDrawNumber(d, p);

                if (!isValidNumber(n)) {
                    continue;
                }

                sample[p]++;
                hit[p][n]++;

                if (recent) {
                    recentSample[p]++;
                    recentHit[p][n]++;
                }
            }
        }

        for (int p = POSITION_MIN; p <= POSITION_MAX; p++) {
            for (int n = NUMBER_MIN; n <= NUMBER_MAX; n++) {
                TeacherStat stat = new TeacherStat();
                stat.position = p;
                stat.number = n;
                stat.stateKey = "HISTORY_FREQ_P" + p;

                stat.sampleCount = sample[p];
                stat.hitCount = hit[p][n];
                stat.missCount = Math.max(0, stat.sampleCount - stat.hitCount);

                stat.recentSample = recentSample[p];
                stat.recentHit = recentHit[p][n];

                stat.hitRate =
                        stat.sampleCount <= 0 ? 0.0d : safeRate(stat.hitCount, stat.sampleCount);
                stat.recentHitRate =
                        stat.recentSample <= 0
                                ? stat.hitRate
                                : safeRate(stat.recentHit, stat.recentSample);

                stat.confidence = sampleConfidence(stat.sampleCount);

                double liftAll = stat.hitRate - BASE_NUMBER_RATE;
                double liftRecent = stat.recentHitRate - BASE_NUMBER_RATE;

                stat.featureSupport = clamp01(0.36d + liftRecent * 2.80d + liftAll * 1.50d);
                stat.positionSupport = stat.confidence;
                stat.causeSupport = 0.0d;
                stat.candidateSupport = 0.0d;
                stat.learningSupport =
                        clamp01(
                                BASE_NUMBER_RATE
                                        + posPart(liftAll * 4.0d)
                                        + posPart(liftRecent * 3.0d));
                stat.returnSupport = clamp01(posPart(liftRecent * 3.0d));
                stat.transitionSupport =
                        clamp01(posPart((stat.recentHitRate - stat.hitRate) * 4.0d));
                stat.hotColdAdjust = limitSigned(liftRecent * 2.5d + liftAll * 1.2d, 0.35d);
                applyDeterministicTeacherState(stat, null, null);

                stat.riskPenalty =
                        clamp01(
                                0.28d
                                        + negPart(liftAll * 4.0d) * 0.28d
                                        + negPart(liftRecent * 4.0d) * 0.34d);
                stat.falseStrongPenalty = clamp01(posPart(liftRecent * 6.0d) * 0.10d);

                stat.teacherScore =
                        clamp01(
                                0.34d
                                        + stat.confidence * 0.10d
                                        + liftAll * 2.10d
                                        + liftRecent * 1.35d
                                        + posPart(stat.hotColdAdjust) * 0.12d
                                        - stat.riskPenalty * 0.08d);

                stat.teacherRisk =
                        clamp01(
                                0.28d
                                        + negPart(liftAll * 3.2d) * 0.28d
                                        + negPart(liftRecent * 3.6d) * 0.30d
                                        + (1.0d - stat.confidence) * 0.16d);

                String status = statusOf(stat);
                String summary = buildSummary(p, n, stat, status);

                Models.NumberTeacherCell cell = new Models.NumberTeacherCell();
                fillCellFromStat(cell, baseIssue, targetIssue, p, n, stat, status, summary);
                putAllCellKeys(map, targetIssue, p, n, cell);
            }
        }

        return map;
    }

    private void fillCellFromStat(
            Models.NumberTeacherCell cell,
            String baseIssue,
            String targetIssue,
            int position,
            int number,
            TeacherStat stat,
            String status,
            String summary) {
        if (cell == null || stat == null) {
            return;
        }

        putString(cell, "cellKey", buildCellKey(targetIssue, position, number));
        putString(cell, "profileKey", buildProfileKey(position, number));
        putString(cell, "baseIssue", safe(baseIssue));
        putString(cell, "targetIssue", safe(targetIssue));
        putLong(cell, "createdAt", System.currentTimeMillis());
        putInt(cell, "position", position);
        putInt(cell, "number", number);

        putString(cell, "status", status);
        putString(cell, "teacherStatus", status);
        putString(cell, "teacherMatrixStatus", status);
        putString(cell, "teacherNumberStatus", status);
        putInt(cell, "teacherMatrixFreezeFlag", "FREEZE".equals(status) ? 1 : 0);
        putInt(
                cell,
                "teacherMatrixRiskFlag",
                ("RISK".equals(status) || "FREEZE".equals(status)) ? 1 : 0);
        putString(cell, "stateKey", stat.stateKey);
        putString(cell, "windowStateKey", stat.windowStateKey);
        putString(cell, "teacherWindowStateKey", stat.windowStateKey);
        putString(cell, "summaryText", summary);
        putString(cell, "reasonText", summary);

        putInt(cell, "sampleCount", stat.sampleCount);
        putInt(cell, "hitCount", stat.hitCount);
        putInt(cell, "missCount", stat.missCount);
        putInt(cell, "recentSample", stat.recentSample);
        putInt(cell, "recentHit", stat.recentHit);

        putDouble(cell, "hitRate", stat.hitRate);
        putDouble(cell, "recentHitRate", stat.recentHitRate);
        putDouble(cell, "supportScore", stat.teacherScore);
        putDouble(cell, "teacherScore", stat.teacherScore);
        putDouble(cell, "numberTeacherScore", stat.teacherScore);
        putDouble(cell, "riskScore", stat.teacherRisk);
        putDouble(cell, "teacherRisk", stat.teacherRisk);
        putDouble(cell, "numberTeacherRisk", stat.teacherRisk);
        putDouble(cell, "confidence", stat.confidence);
        putDouble(cell, "sampleConfidence", stat.confidence);

        putDouble(cell, "featureSupport", stat.featureSupport);
        putDouble(cell, "positionSupport", stat.positionSupport);
        putDouble(cell, "causeSupport", stat.causeSupport);
        putDouble(cell, "candidateSupport", stat.candidateSupport);
        putDouble(cell, "learningSupport", stat.learningSupport);
        putDouble(cell, "returnSupport", stat.returnSupport);
        putDouble(cell, "transitionSupport", stat.transitionSupport);
        putDouble(cell, "hotColdAdjust", stat.hotColdAdjust);
        putDouble(cell, "riskPenalty", stat.riskPenalty);
        putDouble(cell, "falseStrongPenalty", stat.falseStrongPenalty);
        putDouble(cell, "evidenceContribution", stat.evidenceContribution);
        putDouble(cell, "counterContribution", stat.counterContribution);
        putDouble(cell, "sampleTrust", stat.sampleTrust);
        putDouble(cell, "sampleReliability", stat.sampleTrust);
        putDouble(cell, "teacherSampleReliability", stat.sampleTrust);
        putDouble(cell, "recentEffectiveness", stat.recentEffectiveness);
        putDouble(cell, "recentValidity", stat.recentEffectiveness);
        putDouble(cell, "teacherRecentValidity", stat.recentEffectiveness);
        putDouble(cell, "featureDistance", stat.featureDistance);
        putInt(cell, "similarWindowSample", stat.similarWindowSample);
        putDouble(cell, "similarWindowHitRate", stat.similarWindowHitRate);
        putDouble(cell, "similarWindowRecentRate", stat.similarWindowRecentRate);
        putString(cell, "evidenceText", stat.evidenceText);
        putString(cell, "counterEvidenceText", stat.counterText);
        putString(cell, "actionAdvice", stat.actionAdvice);
        putString(cell, "teacherMatrixVersion", V20_TEACHER_UPSTREAM_VERSION);
        putString(cell, "teacherNumberVersion", V20_TEACHER_UPSTREAM_VERSION);

        putString(cell, "hotColdBucket", stat.hotColdBucket);
        putString(cell, "omissionBucket", stat.omissionBucket);
        putString(cell, "densityBucket", stat.densityBucket);
        putString(cell, "repeatBucket", stat.repeatBucket);
        putString(cell, "oddEvenBias", stat.oddEvenBias);
        putString(cell, "highLowBias", stat.highLowBias);
        putString(cell, "neighborTailState", stat.neighborTailState);
        putString(cell, "positionCrowdState", stat.positionCrowdState);
        putString(cell, "crossPositionSync", stat.crossPositionSync);
        putString(cell, "crossPositionSqueeze", stat.crossPositionSqueeze);
        putString(cell, "timingShift", stat.timingShift);
        putInt(cell, "recentLoseStreak", stat.recentLoseStreak);

        putString(cell, "teacherBucket", bucketTeacher(stat.teacherScore));
        putString(cell, "riskBucket", bucketRisk(stat.teacherRisk));
        putString(cell, "sampleBucket", bucketSample(stat.sampleCount));
        putString(cell, "hotColdBucket", bucketSigned("HC", stat.hotColdAdjust));
    }

    private void writeDeferredTeacherWatch(
            Object obj, String baseIssue, String targetIssue, int rank) {
        if (obj == null) return;
        putString(obj, "baseIssue", safe(baseIssue));
        putString(obj, "targetIssue", safe(targetIssue));
        putString(obj, "teacherMatrixStatus", STATUS_WATCH);
        putString(obj, "teacherNumberStatus", STATUS_WATCH);
        putString(obj, "teacherStatus", STATUS_WATCH);
        putString(obj, "teacherStatusClass", "WATCH_NEUTRAL");
        putDouble(obj, "teacherSampleReliability", 0.0d);
        putDouble(obj, "teacherRecentValidity", 0.0d);
        putDouble(obj, "teacherEvidenceContribution", 0.0d);
        putDouble(obj, "teacherCounterContribution", 0.0d);
        putString(obj, "teacherMatrixSummary", "实时教师矩阵超出前40或预算，WATCH中性快返回；后台温层补齐");
        putString(obj, "teacherFieldVersion", LINK_MODIFICATION_V2_TEACHER_VERSION);
    }

    private void saveTeacherLayerSnapshot(
            DbStore store,
            String layer,
            String baseIssue,
            int sample,
            String summary,
            LinkedHashMap<String, Models.NumberTeacherCell> map) {
        if (store == null) return;
        try {
            Models.LayerSnapshot snap = new Models.LayerSnapshot();
            snap.layer = layer;
            if ("WARM".equals(layer)) snap.snapshotKey = AppConfig.SNAPSHOT_KEY_TEACHER_WARM;
            else if ("COLD".equals(layer)) snap.snapshotKey = AppConfig.SNAPSHOT_KEY_TEACHER_COLD;
            else snap.snapshotKey = AppConfig.SNAPSHOT_KEY_TEACHER_DEEP;
            snap.asOfIssue = safe(baseIssue);
            snap.maxIncludedIssue = safe(baseIssue);
            snap.payload =
                    "WARM".equals(layer) && map != null
                            ? encodeTeacherMap(map)
                            : ("sample=" + sample + "|" + safe(summary));
            snap.sampleCount = sample;
            snap.schemaVersion = String.valueOf(AppConfig.LINK_MODIFICATION_V2_SCHEMA_VERSION);
            snap.strategyVersion = LINK_MODIFICATION_V2_TEACHER_VERSION;
            int need =
                    "WARM".equals(layer)
                            ? HISTORY_WINDOW_MIN
                            : ("COLD".equals(layer)
                                    ? Math.min(300, AppConfig.COLD_PROFILE_WINDOW)
                                    : Math.min(500, AppConfig.DEEP_PROFILE_WINDOW));
            snap.freshnessStatus = sample >= need ? "FRESH" : "NO_SAMPLE_NEUTRAL";
            snap.createdAt = System.currentTimeMillis();
            long ttl =
                    "WARM".equals(layer)
                            ? AppConfig.WARM_REFRESH_MS
                            : ("COLD".equals(layer)
                                    ? AppConfig.COLD_REFRESH_MS
                                    : AppConfig.DEEP_REFRESH_MS);
            snap.expiresAt = snap.createdAt + ttl;
            store.saveLayerSnapshot(snap);
            Models.BackgroundCursor cursor = new Models.BackgroundCursor();
            cursor.taskKey = "TEACHER_" + layer + "_CURSOR";
            cursor.cursorIssue = safe(baseIssue);
            cursor.targetIssue = buildNextIssue(baseIssue);
            cursor.status = snap.freshnessStatus;
            cursor.version = LINK_MODIFICATION_V2_TEACHER_VERSION;
            cursor.processedCount = sample;
            cursor.pendingCount = 0;
            cursor.updatedAt = snap.createdAt;
            store.saveBackgroundCursor(cursor);
        } catch (Throwable e) {
        }
    }

    private TeacherRunReport runBackgroundMatrixUpdate(
            DbStore store, Models.Draw draw, String source) {
        TeacherRunReport report = new TeacherRunReport();
        report.ok = 1;
        report.source = source == null ? "" : source;
        report.startedAt = System.currentTimeMillis();

        if (store == null) {
            report.ok = 0;
            report.summary = "教师号码矩阵后台跳过：DbStore为空";
            report.detail = report.summary;
            report.reason = report.summary;
            report.message = report.summary;
            return report;
        }

        try {
            boolean coldDue =
                    store.isLayerSnapshotDue(
                            AppConfig.SNAPSHOT_KEY_TEACHER_COLD, AppConfig.COLD_REFRESH_MS);
            boolean deepDue =
                    store.isLayerSnapshotDue(
                            AppConfig.SNAPSHOT_KEY_TEACHER_DEEP, AppConfig.DEEP_REFRESH_MS);
            /* 每次后台切片只完整构建WARM，避免一次扫描5000/10000期占满实时窗口。 */
            int historyLimit = AppConfig.WARM_PROFILE_WINDOW;
            List<Models.Draw> history = store.getRecentDraws(historyLimit);
            ArrayList<Models.Draw> clean = normalizeHistory(history);

            if ((draw == null || !draw.isValid()) && clean.size() > 0) {
                draw = clean.get(clean.size() - 1);
            }

            String baseIssue = draw == null ? resolveBaseIssue(clean) : safe(draw.issue);
            String targetIssue = buildNextIssue(baseIssue);

            LinkedHashMap<String, Models.NumberTeacherCell> map =
                    buildTeacherMapFromHistory(baseIssue, targetIssue, clean);
            applyLongLayerEvidence(store, map);
            cacheTeacherMap(targetIssue, map);

            int cellCount = 0;
            int strong = 0;
            int risk = 0;
            int sampleTotal = 0;

            boolean[][] counted = new boolean[POSITION_MAX + 1][NUMBER_MAX + 1];

            for (Models.NumberTeacherCell cell : map.values()) {
                if (cell == null) {
                    continue;
                }

                int p = getInt(cell, "position", 0);
                int n = getInt(cell, "number", 0);

                if (!isValidPosition(p) || !isValidNumber(n)) {
                    continue;
                }

                if (counted[p][n]) {
                    continue;
                }

                counted[p][n] = true;
                cellCount++;

                int sample = getInt(cell, "sampleCount", 0);
                sampleTotal += sample;

                String status = getString(cell, "status", "");

                if ("STRONG".equals(status)) {
                    strong++;
                }

                if ("RISK".equals(status)) {
                    risk++;
                }
            }

            report.sampleCount = clean.size();
            report.profileCount = cellCount;
            report.strongCount = strong;
            report.riskCount = risk;
            report.avgSample = cellCount <= 0 ? 0 : sampleTotal / cellCount;
            report.baseIssue = baseIssue;
            report.targetIssue = targetIssue;
            report.finishedAt = System.currentTimeMillis();

            report.summary =
                    "教师号码矩阵后台完成：开奖="
                            + baseIssue
                            + "｜目标="
                            + targetIssue
                            + "｜历史="
                            + clean.size()
                            + "｜矩阵="
                            + cellCount
                            + "格"
                            + "｜平均样本="
                            + report.avgSample
                            + "｜强="
                            + strong
                            + "｜风险="
                            + risk
                            + "｜说明=后台只刷新号码教师画像，不创建BET，不污染真实校准";

            report.detail = report.summary;
            report.reason = report.summary;
            report.message = report.summary;

            writeStoreState(store, "teacher_number_matrix_status", "OK");
            writeStoreState(store, "teacher_number_matrix_summary", report.summary);
            writeStoreState(store, "teacher_number_matrix_issue", baseIssue);
            writeStoreState(store, "teacher_number_matrix_target_issue", targetIssue);
            writeStoreState(
                    store, "teacher_number_matrix_sample_count", String.valueOf(clean.size()));
            writeStoreState(
                    store, "teacher_number_matrix_profile_count", String.valueOf(cellCount));
            writeStoreState(
                    store, "teacher_number_matrix_strong_profile_count", String.valueOf(strong));
            writeStoreState(
                    store, "teacher_number_matrix_risk_profile_count", String.valueOf(risk));
            writeStoreState(
                    store,
                    "teacher_number_matrix_updated_at",
                    String.valueOf(System.currentTimeMillis()));

            writeStoreState(store, "teacher_matrix_status", "OK");
            writeStoreState(store, "teacher_matrix_summary", report.summary);
            writeStoreState(store, "teacher_matrix_sample_count", String.valueOf(clean.size()));
            writeStoreState(store, "teacher_matrix_profile_count", String.valueOf(cellCount));
            writeStoreState(store, "teacher_matrix_strong_profile_count", String.valueOf(strong));
            writeStoreState(store, "teacher_matrix_risk_profile_count", String.valueOf(risk));
            writeStoreState(
                    store, "teacher_matrix_updated_at", String.valueOf(System.currentTimeMillis()));
            int warmSample = Math.min(clean.size(), AppConfig.WARM_PROFILE_WINDOW);
            saveTeacherLayerSnapshot(store, "WARM", baseIssue, warmSample, report.summary, map);
            advanceBackgroundLayerCursor(
                    store,
                    "COLD",
                    baseIssue,
                    coldDue,
                    AppConfig.BACKGROUND_COLD_SLICE_ISSUES,
                    AppConfig.BACKGROUND_COLD_REQUIRED_ISSUES);
            advanceBackgroundLayerCursor(
                    store,
                    "DEEP",
                    baseIssue,
                    deepDue,
                    AppConfig.BACKGROUND_DEEP_SLICE_ISSUES,
                    AppConfig.BACKGROUND_DEEP_REQUIRED_ISSUES);
        } catch (Throwable e) {
            report.ok = 0;
            report.finishedAt = System.currentTimeMillis();
            report.summary = "教师号码矩阵后台异常：" + safeError(e);
            report.detail = report.summary;
            report.reason = report.summary;
            report.message = report.summary;

            writeStoreState(store, "teacher_number_matrix_status", "ERROR");
            writeStoreState(store, "teacher_number_matrix_summary", report.summary);
            writeStoreState(store, "teacher_matrix_status", "ERROR");
            writeStoreState(store, "teacher_matrix_summary", report.summary);
        }

        return report;
    }

    private LinkedHashMap<String, Models.NumberTeacherCell> loadTeacherMapForRealtime(
            DbStore store, String baseIssue, String targetIssue, List<Models.Draw> history) {
        synchronized (TEACHER_MAP_CACHE_LOCK) {
            if (cachedTeacherMapV5 != null
                    && targetIssue.equals(cachedTeacherMapTargetV5)
                    && System.currentTimeMillis() - cachedTeacherMapAtV5
                            <= TEACHER_MAP_CACHE_TTL_MS_V5) return cachedTeacherMapV5;
        }
        LinkedHashMap<String, Models.NumberTeacherCell> map = null;
        try {
            Models.LayerSnapshot snap =
                    store == null
                            ? null
                            : store.loadLayerSnapshot(AppConfig.SNAPSHOT_KEY_TEACHER_WARM);
            int issueLag =
                    teacherIssueLag(
                            baseIssue,
                            snap == null
                                    ? ""
                                    : firstNonEmptyIssue(snap.maxIncludedIssue, snap.asOfIssue));
            boolean freshByIssue = isTeacherSnapshotFreshByIssue(snap, baseIssue, issueLag);
            if (store != null) {
                store.setState(
                        "teacher_snapshot_as_of_issue",
                        snap == null
                                ? ""
                                : firstNonEmptyIssue(snap.maxIncludedIssue, snap.asOfIssue));
                store.setState("teacher_snapshot_issue_lag", String.valueOf(issueLag));
                store.setState("teacher_snapshot_fresh_by_issue", freshByIssue ? "1" : "0");
                store.setState(
                        "teacher_snapshot_created_at",
                        String.valueOf(snap == null ? 0L : snap.createdAt));
                store.setState(
                        "teacher_snapshot_runtime_status",
                        freshByIssue ? "FRESH_BY_ISSUE" : "STALE_BY_ISSUE_REBUILD_FROM_HISTORY");
            }
            if (freshByIssue
                    && snap != null
                    && snap.payload != null
                    && snap.payload.indexOf("TMAP1|") == 0
                    && (snap.maxIncludedIssue == null
                            || baseIssue.length() == 0
                            || snap.maxIncludedIssue.compareTo(baseIssue) <= 0)) {
                map = decodeTeacherMap(snap.payload, targetIssue);
            }
        } catch (Throwable e) {
            map = null;
        }
        if (map == null || map.size() == 0)
            map = buildTeacherMapFromHistory(baseIssue, targetIssue, history);
        applyLongLayerEvidence(store, map);
        cacheTeacherMap(targetIssue, map);
        return map;
    }

    private void cacheTeacherMap(
            String targetIssue, LinkedHashMap<String, Models.NumberTeacherCell> map) {
        if (map == null || map.size() == 0) return;
        synchronized (TEACHER_MAP_CACHE_LOCK) {
            cachedTeacherMapV5 = map;
            cachedTeacherMapTargetV5 = safe(targetIssue);
            cachedTeacherMapAtV5 = System.currentTimeMillis();
        }
    }

    private String encodeTeacherMap(LinkedHashMap<String, Models.NumberTeacherCell> map) {
        StringBuilder sb = new StringBuilder("TMAP1|");
        boolean[][] used = new boolean[POSITION_MAX + 1][NUMBER_MAX + 1];
        for (Models.NumberTeacherCell cell : map.values()) {
            if (cell == null) continue;
            int p = getInt(cell, "position", 0), n = getInt(cell, "number", 0);
            if (!isValidPosition(p) || !isValidNumber(n) || used[p][n]) continue;
            used[p][n] = true;
            if (sb.length() > 6) sb.append(';');
            sb.append(p)
                    .append(',')
                    .append(n)
                    .append(',')
                    .append(
                            safe(getString(cell, "status", "WATCH"))
                                    .replace(',', '_')
                                    .replace(';', '_'))
                    .append(',')
                    .append(getInt(cell, "sampleCount", 0))
                    .append(',')
                    .append(round6(getDouble(cell, "hitRate", 0.0d)))
                    .append(',')
                    .append(round6(getDouble(cell, "recentHitRate", 0.0d)))
                    .append(',')
                    .append(round6(getDouble(cell, "teacherScore", 0.0d)))
                    .append(',')
                    .append(round6(getDouble(cell, "teacherRisk", 0.0d)))
                    .append(',')
                    .append(round6(getDouble(cell, "confidence", 0.0d)));
        }
        return sb.toString();
    }

    private LinkedHashMap<String, Models.NumberTeacherCell> decodeTeacherMap(
            String payload, String targetIssue) {
        LinkedHashMap<String, Models.NumberTeacherCell> map =
                new LinkedHashMap<String, Models.NumberTeacherCell>();
        if (payload == null || payload.indexOf("TMAP1|") != 0) return map;
        String body = payload.substring(6);
        String[] rows = body.split(";");
        for (int i = 0; i < rows.length; i++) {
            String[] a = rows[i].split(",");
            if (a.length < 9) continue;
            try {
                int p = Integer.parseInt(a[0]), n = Integer.parseInt(a[1]);
                if (!isValidPosition(p) || !isValidNumber(n)) continue;
                Models.NumberTeacherCell c = new Models.NumberTeacherCell();
                putInt(c, "position", p);
                putInt(c, "number", n);
                putString(c, "targetIssue", safe(targetIssue));
                putString(c, "status", a[2]);
                putString(c, "teacherStatus", a[2]);
                putString(c, "teacherMatrixStatus", a[2]);
                putString(c, "teacherNumberStatus", a[2]);
                putInt(c, "sampleCount", Integer.parseInt(a[3]));
                putDouble(c, "hitRate", Double.parseDouble(a[4]));
                putDouble(c, "recentHitRate", Double.parseDouble(a[5]));
                putDouble(c, "teacherScore", Double.parseDouble(a[6]));
                putDouble(c, "numberTeacherScore", Double.parseDouble(a[6]));
                putDouble(c, "teacherRisk", Double.parseDouble(a[7]));
                putDouble(c, "numberTeacherRisk", Double.parseDouble(a[7]));
                putDouble(c, "confidence", Double.parseDouble(a[8]));
                putDouble(c, "sampleConfidence", Double.parseDouble(a[8]));
                putAllCellKeys(map, targetIssue, p, n, c);
            } catch (Throwable e) {
            }
        }
        return map;
    }

    private String round6(double v) {
        return String.format(java.util.Locale.US, "%.6f", Double.valueOf(v));
    }

    private void advanceBackgroundLayerCursor(
            DbStore store, String layer, String baseIssue, boolean due, int slice, int required) {
        if (store == null) return;
        String snapshotKey =
                "COLD".equals(layer)
                        ? AppConfig.SNAPSHOT_KEY_TEACHER_COLD
                        : AppConfig.SNAPSHOT_KEY_TEACHER_DEEP;
        Models.LayerSnapshot existing = null;
        try {
            existing = store.loadLayerSnapshot(snapshotKey);
        } catch (Throwable e) {
            existing = null;
        }
        boolean building =
                existing != null
                        && "BUILDING".equals(existing.freshnessStatus)
                        && isLongAccumulatorPayload(existing.payload);
        if (!due && !building) return;
        LongLayerAccumulator acc =
                building ? decodeLongAccumulator(existing.payload) : new LongLayerAccumulator();
        if (!building) {
            acc.cursorIssue = safe(baseIssue);
            acc.required = required;
        }
        long started = System.currentTimeMillis();
        ArrayList<Models.Draw> clean = new ArrayList<Models.Draw>();
        try {
            clean =
                    normalizeHistory(
                            store.getDrawsBeforeIssue(acc.cursorIssue, Math.max(1, slice)));
        } catch (Throwable e) {
        }
        String oldest = acc.cursorIssue;
        for (int i = 0; i < clean.size(); i++) {
            Models.Draw d = clean.get(i);
            if (d == null || !d.isValid()) continue;
            if (d.issue != null
                    && d.issue.length() > 0
                    && (oldest.length() == 0 || d.issue.compareTo(oldest) < 0)) oldest = d.issue;
            for (int p = POSITION_MIN; p <= POSITION_MAX; p++) {
                int n = d.getNumberAtPosition(p);
                if (!isValidNumber(n)) continue;
                acc.sample[p]++;
                acc.hit[p][n]++;
            }
        }
        acc.processed += clean.size();
        acc.cursorIssue = oldest;
        acc.required = required;
        boolean complete =
                acc.processed >= required || clean.size() < Math.max(1, slice) || clean.size() == 0;
        Models.LayerSnapshot snap = new Models.LayerSnapshot();
        snap.layer = layer;
        snap.snapshotKey = snapshotKey;
        snap.asOfIssue = safe(baseIssue);
        snap.maxIncludedIssue = safe(baseIssue);
        snap.payload = encodeLongAccumulator(acc);
        snap.sampleCount = acc.processed;
        snap.schemaVersion = String.valueOf(AppConfig.LINK_MODIFICATION_V2_SCHEMA_VERSION);
        snap.strategyVersion = LINK_MODIFICATION_V2_TEACHER_VERSION;
        snap.freshnessStatus = complete ? "FRESH" : "BUILDING";
        snap.createdAt = System.currentTimeMillis();
        long ttl = "COLD".equals(layer) ? AppConfig.COLD_REFRESH_MS : AppConfig.DEEP_REFRESH_MS;
        snap.expiresAt = complete ? snap.createdAt + ttl : snap.createdAt;
        store.saveLayerSnapshot(snap);
        Models.BackgroundCursor cursor = new Models.BackgroundCursor();
        cursor.taskKey = "TEACHER_" + layer + "_CURSOR";
        cursor.cursorIssue = acc.cursorIssue;
        cursor.targetIssue = buildNextIssue(baseIssue);
        cursor.processedCount = acc.processed;
        cursor.pendingCount = Math.max(0, required - acc.processed);
        cursor.status = complete ? "FRESH" : "BUILDING";
        cursor.version = LINK_MODIFICATION_V2_TEACHER_VERSION;
        cursor.updatedAt = snap.createdAt;
        store.saveBackgroundCursor(cursor);
        store.saveBackgroundSliceState(
                "TEACHER_" + layer,
                cursor.status,
                acc.processed,
                cursor.pendingCount,
                System.currentTimeMillis() - started,
                "真实历史分片累计；每片=" + clean.size() + "，不是虚拟推进，不阻塞实时");
    }

    private boolean isLongAccumulatorPayload(String payload) {
        return payload != null
                && (payload.indexOf("TLONG2|") == 0 || payload.indexOf("TLONG1|") == 0);
    }

    private String encodeLongAccumulator(LongLayerAccumulator a) {
        StringBuilder sb =
                new StringBuilder("TLONG2|")
                        .append(a.processed)
                        .append('|')
                        .append(a.required)
                        .append('|')
                        .append(safe(a.cursorIssue))
                        .append('|');
        for (int p = 1; p <= 10; p++) {
            if (p > 1) sb.append(',');
            sb.append(a.sample[p]);
        }
        sb.append('|');
        for (int p = 1; p <= 10; p++)
            for (int n = 1; n <= 10; n++) {
                if (p != 1 || n != 1) sb.append(',');
                sb.append(a.hit[p][n]);
            }
        sb.append('|');
        for (int p = 1; p <= 10; p++) {
            if (p > 1) sb.append(',');
            sb.append(a.recentSample[p]);
        }
        sb.append('|');
        for (int p = 1; p <= 10; p++)
            for (int n = 1; n <= 10; n++) {
                if (p != 1 || n != 1) sb.append(',');
                sb.append(a.recentHit[p][n]);
            }
        return sb.toString();
    }

    private LongLayerAccumulator decodeLongAccumulator(String payload) {
        LongLayerAccumulator a = new LongLayerAccumulator();
        try {
            String[] x = payload.split("\\|", 8);
            if (x.length < 6) return a;
            a.processed = Integer.parseInt(x[1]);
            a.required = Integer.parseInt(x[2]);
            a.cursorIssue = x[3];
            String[] ss = x[4].split(",");
            for (int p = 1; p <= 10 && p <= ss.length; p++)
                a.sample[p] = Integer.parseInt(ss[p - 1]);
            String[] hh = x[5].split(",");
            int k = 0;
            for (int p = 1; p <= 10; p++)
                for (int n = 1; n <= 10; n++)
                    if (k < hh.length) a.hit[p][n] = Integer.parseInt(hh[k++]);
            if (x.length >= 8) {
                String[] rs = x[6].split(",");
                for (int p = 1; p <= 10 && p <= rs.length; p++)
                    a.recentSample[p] = Integer.parseInt(rs[p - 1]);
                String[] rh = x[7].split(",");
                k = 0;
                for (int p = 1; p <= 10; p++)
                    for (int n = 1; n <= 10; n++)
                        if (k < rh.length) a.recentHit[p][n] = Integer.parseInt(rh[k++]);
            } else {
                for (int p = 1; p <= 10; p++) {
                    a.recentSample[p] =
                            Math.min(a.sample[p], AppConfig.TEACHER_RECENT_ACCUMULATOR_DRAWS);
                    for (int n = 1; n <= 10; n++)
                        a.recentHit[p][n] = Math.min(a.hit[p][n], a.recentSample[p]);
                }
            }
        } catch (Throwable e) {
            return new LongLayerAccumulator();
        }
        return a;
    }

    private void applyLongLayerEvidence(
            DbStore store, LinkedHashMap<String, Models.NumberTeacherCell> map) {
        if (store == null || map == null || map.size() == 0) return;
        LongLayerAccumulator cold = null, deep = null;
        try {
            Models.LayerSnapshot s = store.loadLayerSnapshot(AppConfig.SNAPSHOT_KEY_TEACHER_COLD);
            if (s != null
                    && "FRESH".equals(s.freshnessStatus)
                    && isLongAccumulatorPayload(s.payload)) cold = decodeLongAccumulator(s.payload);
        } catch (Throwable e) {
        }
        try {
            Models.LayerSnapshot s = store.loadLayerSnapshot(AppConfig.SNAPSHOT_KEY_TEACHER_DEEP);
            if (s != null
                    && "FRESH".equals(s.freshnessStatus)
                    && isLongAccumulatorPayload(s.payload)) deep = decodeLongAccumulator(s.payload);
        } catch (Throwable e) {
        }
        if (cold == null && deep == null) return;
        boolean[][] used = new boolean[11][11];
        for (Models.NumberTeacherCell c : map.values()) {
            if (c == null) continue;
            int p = getInt(c, "position", 0), n = getInt(c, "number", 0);
            if (!isValidPosition(p) || !isValidNumber(n) || used[p][n]) continue;
            used[p][n] = true;
            double adj = 0.0d;
            int total = 0;
            double weighted = 0.0d;
            if (cold != null && cold.sample[p] > 0) {
                double r = ((double) cold.hit[p][n]) / cold.sample[p];
                weighted += r * 0.65d;
                total += cold.sample[p];
                adj += limitSigned((r - BASE_NUMBER_RATE) * 0.18d, 0.015d);
            }
            if (deep != null && deep.sample[p] > 0) {
                double r = ((double) deep.hit[p][n]) / deep.sample[p];
                weighted += r * 0.35d;
                total += deep.sample[p];
                adj += limitSigned((r - BASE_NUMBER_RATE) * 0.10d, 0.010d);
            }
            if (total > 0) {
                putInt(c, "teacherLongSample", total);
                putDouble(c, "teacherLongHitRate", weighted);
                putDouble(c, "teacherScore", clamp01(getDouble(c, "teacherScore", 0.34d) + adj));
                putDouble(c, "numberTeacherScore", getDouble(c, "teacherScore", 0.34d));
            }
        }
    }

    private static final class LongLayerAccumulator {
        int processed;
        int required;
        String cursorIssue = "";
        int[] sample = new int[11];
        int[][] hit = new int[11][11];
        int[] recentSample = new int[11];
        int[][] recentHit = new int[11][11];
    }

    private TeacherRunReport runAutoBatch(DbStore store, int batchSize, String source) {
        Models.Draw latest = null;

        try {
            if (store != null) {
                latest = store.getLatestDraw();
            }
        } catch (Throwable e) {
            latest = null;
        }

        TeacherRunReport report = runBackgroundMatrixUpdate(store, latest, source);

        if (batchSize > 0) {
            report.batchSize = batchSize;
        }

        return report;
    }

    private void writeRealtimeState(
            DbStore store,
            String summary,
            int applied,
            int avgSample,
            int strong,
            int risk,
            int fieldWritten) {
        if (store == null) {
            return;
        }

        try {
            store.setState("teacher_number_matrix_realtime_status", "OK");
            store.setState(
                    "teacher_number_matrix_realtime_version", LINK_MODIFICATION_V2_TEACHER_VERSION);
            store.setState(
                    "teacher_number_matrix_realtime_summary", summary == null ? "" : summary);
            store.setState("teacher_number_matrix_realtime_applied", String.valueOf(applied));
            store.setState(
                    "teacher_number_matrix_realtime_field_written", String.valueOf(fieldWritten));
            store.setState("teacher_number_matrix_realtime_avg_sample", String.valueOf(avgSample));
            store.setState("teacher_number_matrix_realtime_strong", String.valueOf(strong));
            store.setState("teacher_number_matrix_realtime_risk", String.valueOf(risk));
            store.setState(
                    "teacher_number_matrix_realtime_updated_at",
                    String.valueOf(System.currentTimeMillis()));

            store.setState("teacher_matrix_candidate_apply", applied > 0 ? "1" : "0");
            store.setState("teacher_matrix_candidate_apply_count", String.valueOf(applied));
            store.setState("teacher_matrix_candidate_field_written", String.valueOf(fieldWritten));
            store.setState("teacher_matrix_candidate_avg_sample", String.valueOf(avgSample));
            store.setState("teacher_matrix_candidate_strong", String.valueOf(strong));
            store.setState("teacher_matrix_candidate_risk", String.valueOf(risk));
            store.setState("teacher_matrix_candidate_summary", summary == null ? "" : summary);

            store.setState("teacher_matrix_summary", summary == null ? "" : summary);
            store.setState("teacher_matrix_status", "OK");
            store.setState("teacher_matrix_realtime_version", V20_TEACHER_UPSTREAM_VERSION);
            store.setState("teacher_matrix_running", "0");
            store.setState("teacher_matrix_updated_at", String.valueOf(System.currentTimeMillis()));
        } catch (Throwable e) {
        }
    }

    private void writeStoreState(DbStore store, String key, String value) {
        if (store == null || key == null || key.length() == 0) {
            return;
        }

        try {
            store.setState(key, value == null ? "" : value);
        } catch (Throwable e) {
        }
    }

    private TeacherStat calcTeacherStat(
            int position,
            int number,
            Models.PreDrawFeature f,
            Models.PositionHealth h,
            List<Models.CauseChain> chains,
            List<Models.CandidateK3> candidates,
            List<Models.LearningCase> learningCases) {
        TeacherStat s = new TeacherStat();
        s.position = position;
        s.number = number;

        s.stateKey = f == null ? "" : getString(f, "stateKey", "");
        s.featureSupport = calcFeatureSupport(f);
        s.positionSupport = calcPositionSupport(h);
        s.causeSupport = calcCauseSupport(position, number, chains);
        s.candidateSupport = calcCandidateSupport(position, number, candidates);
        s.learningSupport = calcLearningSupport(position, number, learningCases, s);
        s.returnSupport = f == null ? 0.0d : getDouble(f, "returnZoneScore", 0.0d);
        s.transitionSupport = f == null ? 0.0d : getDouble(f, "transitionLift", 0.0d);
        s.hotColdAdjust = calcHotColdAdjust(f);
        applyDeterministicTeacherState(s, f, h);
        s.falseStrongPenalty = calcFalseStrongPenalty(f, h);
        s.riskPenalty = calcRiskPenalty(f, h);

        s.confidence = calcConfidence(s, f, h);

        double support =
                s.featureSupport * 0.22d
                        + s.positionSupport * 0.16d
                        + s.causeSupport * 0.15d
                        + s.candidateSupport * 0.12d
                        + s.learningSupport * 0.18d
                        + s.returnSupport * 0.08d
                        + s.transitionSupport * 0.06d
                        + posPart(s.hotColdAdjust) * 0.03d;

        double penalty =
                s.riskPenalty * 0.34d
                        + s.falseStrongPenalty * 0.26d
                        + negPart(s.hotColdAdjust) * 0.14d
                        + (1.0d - s.confidence) * 0.10d;

        s.teacherScore = clamp01(s.confidence * 0.18d + support - penalty * 0.40d);
        s.teacherRisk = clamp01(penalty + (1.0d - support) * 0.12d);

        if (s.sampleCount > 0) {
            s.hitRate = safeRate(s.hitCount, s.sampleCount);
        } else {
            s.hitRate = 0.0d;
        }

        if (s.recentSample > 0) {
            s.recentHitRate = safeRate(s.recentHit, s.recentSample);
        } else {
            s.recentHitRate = 0.0d;
        }

        if (s.sampleCount >= 20) {
            s.teacherScore =
                    clamp01(s.teacherScore * 0.72d + s.hitRate * 0.20d + s.recentHitRate * 0.08d);
            s.teacherRisk =
                    clamp01(
                            s.teacherRisk * 0.72d
                                    + (1.0d - s.hitRate) * 0.16d
                                    + (1.0d - s.recentHitRate) * 0.12d);
        }

        s.evidenceContribution =
                clamp01(
                        s.teacherScore * 0.34d
                                + s.featureSupport * 0.18d
                                + s.causeSupport * 0.16d
                                + s.similarWindowHitRate * 0.16d
                                + s.sampleTrust * 0.08d
                                + s.recentEffectiveness * 0.08d);
        s.counterContribution =
                clamp01(
                        s.teacherRisk * 0.36d
                                + s.riskPenalty * 0.22d
                                + s.falseStrongPenalty * 0.16d
                                + (1.0d - s.sampleTrust) * 0.12d
                                + negPart(s.recentEffectiveness - BASE_NUMBER_RATE) * 0.14d);
        s.teacherScore =
                clamp01(
                        s.teacherScore * 0.86d
                                + s.evidenceContribution * 0.10d
                                + s.recentEffectiveness * 0.04d);
        s.teacherRisk =
                clamp01(
                        s.teacherRisk * 0.86d
                                + s.counterContribution * 0.10d
                                + (1.0d - s.sampleTrust) * 0.04d);
        s.evidenceText = buildTeacherEvidenceText(s);
        s.counterText = buildTeacherCounterText(s);
        s.actionAdvice = buildTeacherActionAdvice(s);

        return s;
    }

    private double calcFeatureSupport(Models.PreDrawFeature f) {
        if (f == null) {
            return 0.0d;
        }

        double featureScore = getDouble(f, "featureScore", 0.0d);
        double freqSlope = getDouble(f, "freqSlope", 0.0d);
        double freqAccel = getDouble(f, "freqAccel", 0.0d);
        double returnZone = getDouble(f, "returnZoneScore", 0.0d);
        double coldRepair = getDouble(f, "coldRepairPower", 0.0d);
        double transition = getDouble(f, "transitionLift", 0.0d);
        double recovery = getDouble(f, "recoveryQuality", 0.0d);
        double conflict = getDouble(f, "transitionConflict", 0.0d);
        double falseRisk = getDouble(f, "falseRecoveryRisk", 0.0d);
        double noise = getDouble(f, "positionNoiseScore", 0.0d);

        double support = 0.0d;

        support += featureScore * 0.30d;
        support += posPart(freqSlope) * 0.12d;
        support += posPart(freqAccel) * 0.08d;
        support += returnZone * 0.14d;
        support += coldRepair * 0.10d;
        support += transition * 0.12d;
        support += recovery * 0.12d;
        support -= conflict * 0.09d;
        support -= falseRisk * 0.13d;
        support -= noise * 0.08d;

        return clamp01(support);
    }

    private double calcPositionSupport(Models.PositionHealth h) {
        if (h == null) {
            return 0.0d;
        }

        double health = getDouble(h, "healthScore", getDouble(h, "positionHealthScore", 0.0d));
        double stability = getDouble(h, "stabilityScore", getDouble(h, "positionStability", 0.0d));
        double returnPower = getDouble(h, "returnPower", getDouble(h, "positionReturnPower", 0.0d));
        double transition =
                getDouble(h, "transitionPower", getDouble(h, "positionTransitionPower", 0.0d));
        double risk = getDouble(h, "riskScore", getDouble(h, "positionRiskScore", 0.0d));

        return clamp01(
                health * 0.38d
                        + stability * 0.22d
                        + returnPower * 0.18d
                        + transition * 0.12d
                        - risk * 0.18d);
    }

    private double calcCauseSupport(int position, int number, List<Models.CauseChain> chains) {
        if (chains == null || chains.size() == 0) {
            return 0.0d;
        }

        double sum = 0.0d;
        double weight = 0.0d;

        for (int i = 0; i < chains.size(); i++) {
            Models.CauseChain c = chains.get(i);

            if (c == null) {
                continue;
            }

            int p = getInt(c, "position", 0);
            int n = getInt(c, "number", 0);

            if (p != position || n != number) {
                continue;
            }

            double cause = getDouble(c, "finalCauseScore", getDouble(c, "causeScore", 0.0d));
            double risk = getDouble(c, "riskScore", 0.0d);
            double confidence = getDouble(c, "confidence", 0.50d);

            sum += clamp01(cause - risk * 0.35d) * Math.max(0.10d, confidence);
            weight += Math.max(0.10d, confidence);
        }

        if (weight <= 0.0d) {
            return 0.0d;
        }

        return clamp01(sum / weight);
    }

    private double calcCandidateSupport(
            int position, int number, List<Models.CandidateK3> candidates) {
        if (candidates == null || candidates.size() == 0) {
            return 0.0d;
        }

        double sum = 0.0d;
        double weight = 0.0d;

        for (int i = 0; i < candidates.size(); i++) {
            Models.CandidateK3 c = candidates.get(i);

            if (c == null) {
                continue;
            }

            int p = getInt(c, "position", 0);

            if (p != position) {
                continue;
            }

            int n1 = getInt(c, "n1", 0);
            int n2 = getInt(c, "n2", 0);
            int n3 = getInt(c, "n3", 0);

            if (number != n1 && number != n2 && number != n3) {
                continue;
            }

            double cause = getDouble(c, "causeScore", 0.0d);
            double risk = getDouble(c, "riskScore", 0.0d);
            double finalP = getDouble(c, "finalP", 0.0d);
            double edge = getDouble(c, "edge", 0.0d);

            double local =
                    clamp01(
                            cause * 0.42d
                                    + finalP * 0.28d
                                    + posPart(edge * 4.0d) * 0.16d
                                    - risk * 0.22d);
            double w = 1.0d;

            int rank = getInt(c, "rankNo", getInt(c, "rank", 0));

            if (rank > 0) {
                w = 1.0d / Math.max(1.0d, (double) rank);
            }

            sum += local * w;
            weight += w;
        }

        if (weight <= 0.0d) {
            return 0.0d;
        }

        return clamp01(sum / weight);
    }

    private double calcLearningSupport(
            int position, int number, List<Models.LearningCase> learningCases, TeacherStat stat) {
        if (learningCases == null || learningCases.size() == 0) {
            stat.sampleCount = 0;
            stat.hitCount = 0;
            stat.missCount = 0;
            stat.recentSample = 0;
            stat.recentHit = 0;
            return 0.0d;
        }

        int sample = 0;
        int hit = 0;
        int miss = 0;
        int recentSample = 0;
        int recentHit = 0;

        int startRecent = learningCases.size() - 120;

        if (startRecent < 0) {
            startRecent = 0;
        }

        for (int i = 0; i < learningCases.size(); i++) {
            Models.LearningCase x = learningCases.get(i);

            if (x == null) {
                continue;
            }

            int p = getInt(x, "position", 0);

            if (p != position) {
                continue;
            }

            int actual = getInt(x, "actualNumber", 0);
            int hitFlag = actual == number ? 1 : 0;

            if (!isValidNumber(actual)) {
                hitFlag = getInt(x, "hit", 0);
            }

            sample++;

            if (hitFlag == 1) {
                hit++;
            } else {
                miss++;
            }

            if (i >= startRecent) {
                recentSample++;

                if (hitFlag == 1) {
                    recentHit++;
                }
            }
        }

        stat.sampleCount = sample;
        stat.hitCount = hit;
        stat.missCount = miss;
        stat.recentSample = recentSample;
        stat.recentHit = recentHit;

        if (sample <= 0) {
            return 0.0d;
        }

        double hitRate = safeRate(hit, sample);
        double recentRate = recentSample > 0 ? safeRate(recentHit, recentSample) : hitRate;
        double sampleConfidence = sampleConfidence(sample);

        return clamp01(hitRate * 0.52d + recentRate * 0.28d + sampleConfidence * 0.20d);
    }

    private double calcHotColdAdjust(Models.PreDrawFeature f) {
        if (f == null) {
            return 0.0d;
        }

        double freq16 = getDouble(f, "freq16", 0.0d);
        double freq64 = getDouble(f, "freq64", 0.0d);
        double freq240 = getDouble(f, "freq240", BASE_NUMBER_RATE);
        double freqSlope = getDouble(f, "freqSlope", 0.0d);
        double gapZ = getDouble(f, "gapZ", 0.0d);
        double returnZone = getDouble(f, "returnZoneScore", 0.0d);
        double hotExhaustion = getDouble(f, "hotExhaustion", 0.0d);

        double adjust = 0.0d;

        adjust += (freq16 - BASE_NUMBER_RATE) * 0.90d;
        adjust += (freq64 - BASE_NUMBER_RATE) * 0.50d;
        adjust += (freq240 - BASE_NUMBER_RATE) * 0.30d;
        adjust += freqSlope * 0.16d;
        adjust += returnZone * 0.10d;

        if (gapZ > 0.45d && gapZ < 1.80d) {
            adjust += 0.08d;
        }

        adjust -= hotExhaustion * 0.16d;

        return limitSigned(adjust, 0.45d);
    }

    private double calcFalseStrongPenalty(Models.PreDrawFeature f, Models.PositionHealth h) {
        double v = 0.0d;

        if (f != null) {
            v += getDouble(f, "falseRecoveryRisk", 0.0d) * 0.44d;
            v += getDouble(f, "hotExhaustion", 0.0d) * 0.24d;
            v += getDouble(f, "breakDepth", 0.0d) * 0.18d;
        }

        if (h != null) {
            v +=
                    getDouble(h, "falseStrongRisk", getDouble(h, "positionFalseStrongRisk", 0.0d))
                            * 0.28d;
        }

        return clamp01(v);
    }

    private double calcRiskPenalty(Models.PreDrawFeature f, Models.PositionHealth h) {
        double v = 0.0d;

        if (f != null) {
            v += getDouble(f, "transitionConflict", 0.0d) * 0.18d;
            v += getDouble(f, "pairConflict", 0.0d) * 0.14d;
            v += getDouble(f, "positionNoiseScore", 0.0d) * 0.18d;
            v += getDouble(f, "positionDrift", 0.0d) * 0.14d;
            v += getDouble(f, "falseRecoveryRisk", 0.0d) * 0.18d;
        }

        if (h != null) {
            v += getDouble(h, "riskScore", getDouble(h, "positionRiskScore", 0.0d)) * 0.20d;
            v += getDouble(h, "wrongRisk", getDouble(h, "positionWrongRisk", 0.0d)) * 0.22d;
        }

        return clamp01(v);
    }

    private double calcConfidence(TeacherStat s, Models.PreDrawFeature f, Models.PositionHealth h) {
        double sample = sampleConfidence(s.sampleCount);
        double featureConf = f == null ? 0.0d : getDouble(f, "confidence", 0.0d);
        double positionConf =
                h == null
                        ? 0.0d
                        : getDouble(h, "healthScore", getDouble(h, "positionHealthScore", 0.0d));

        return clamp01(sample * 0.42d + featureConf * 0.34d + positionConf * 0.24d);
    }

    private ComboTeacherResult evalCombo(
            int position,
            int n1,
            int n2,
            int n3,
            Models.NumberTeacherCell c1,
            Models.NumberTeacherCell c2,
            Models.NumberTeacherCell c3) {
        ComboTeacherResult r = new ComboTeacherResult();

        double scoreSum = 0.0d;
        double riskSum = 0.0d;
        double confSum = 0.0d;
        double numberHitSum = 0.0d;
        double support = 0.0d;
        double penalty = 0.0d;
        double featureDistanceSum = 0.0d;
        double sampleTrustSum = 0.0d;
        double recentEffectiveSum = 0.0d;
        int similarSampleSum = 0;
        StringBuilder stateSb = new StringBuilder();

        int used = 0;
        int sampleSum = 0;

        Models.NumberTeacherCell[] cells = new Models.NumberTeacherCell[] {c1, c2, c3};
        int[] nums = new int[] {n1, n2, n3};

        for (int i = 0; i < cells.length; i++) {
            Models.NumberTeacherCell cell = cells[i];

            if (cell == null || !isValidNumber(nums[i])) {
                continue;
            }

            double score =
                    getDouble(
                            cell,
                            "teacherScore",
                            getDouble(
                                    cell,
                                    "numberTeacherScore",
                                    getDouble(cell, "supportScore", 0.0d)));
            double risk =
                    getDouble(
                            cell,
                            "teacherRisk",
                            getDouble(
                                    cell, "numberTeacherRisk", getDouble(cell, "riskScore", 0.0d)));
            double conf = getDouble(cell, "confidence", 0.0d);
            double hitRate = getDouble(cell, "hitRate", 0.0d);
            int sample = getInt(cell, "sampleCount", 0);
            double featureDistance = getDouble(cell, "featureDistance", 0.50d);
            double sampleTrust = getDouble(cell, "sampleTrust", sampleConfidence(sample));
            double recentEffective =
                    getDouble(cell, "recentEffectiveness", getDouble(cell, "recentHitRate", 0.0d));
            String stateKey = getString(cell, "windowStateKey", getString(cell, "stateKey", ""));
            if (stateKey.length() > 0) {
                if (stateSb.length() > 0) {
                    stateSb.append("+");
                }
                stateSb.append(shortKey(stateKey, 38));
            }

            scoreSum += score;
            riskSum += risk;
            confSum += conf;
            numberHitSum += hitRate;
            sampleSum += sample;
            featureDistanceSum += featureDistance;
            sampleTrustSum += sampleTrust;
            recentEffectiveSum += recentEffective;
            similarSampleSum += getInt(cell, "similarWindowSample", sample);

            support += score * (0.60d + conf * 0.40d);
            penalty += risk * (0.60d + (1.0d - conf) * 0.40d);

            used++;
        }

        if (used <= 0) {
            r.comboScore = 0.0d;
            r.comboRisk = 0.0d;
            r.confidence = 0.0d;
            r.avgHitRate = 0.0d;
            r.k3HitRate = 0.0d;
            r.support = 0.0d;
            r.penalty = 0.0d;
            r.sample = 0;
            r.featureDistance = 1.0d;
            r.similarWindowSample = 0;
            r.similarWindowHitRate = 0.0d;
            r.sampleTrust = 0.0d;
            r.recentEffectiveness = 0.0d;
            r.evidenceContribution = 0.0d;
            r.counterContribution = 0.0d;
            r.stateKey = "TNM_COMBO|P" + position + "|NO_TEACHER_NEUTRAL";
            r.evidenceText = "证据贡献=无教师样本，中性不加权";
            r.counterText = "反证贡献=无教师样本，中性不硬拦";
            r.actionAdvice = "NO_SAMPLE_NEUTRAL：教师无样本，不支持教师加权，也不作为硬风险，交由V20/DecisionGate裁决";
            r.status = STATUS_NO_SAMPLE;
            r.summary =
                    "教师号码矩阵｜位置="
                            + position
                            + "｜K3="
                            + n1
                            + ","
                            + n2
                            + ","
                            + n3
                            + "｜无号码教师样本，标记NO_SAMPLE中性：不救援BET、不硬拦BET、不计真实校准";
            return r;
        }

        double avgScore = scoreSum / used;
        double avgRisk = riskSum / used;
        double avgConf = confSum / used;
        double avgHit = numberHitSum / used;
        double k3HitRate = clamp01(numberHitSum);

        double k3Lift = k3HitRate - BASE_K3_RATE;
        double avgLift = avgHit - BASE_NUMBER_RATE;

        double comboBase =
                avgScore * 0.46d
                        + posPart(k3Lift * 2.0d) * 0.22d
                        + posPart(avgLift * 4.0d) * 0.12d
                        + avgConf * 0.14d
                        - avgRisk * 0.16d;

        double comboRisk =
                avgRisk * 0.68d
                        + negPart(k3Lift * 2.0d) * 0.16d
                        + negPart(avgLift * 4.0d) * 0.08d
                        + (1.0d - avgConf) * 0.12d;

        int avgSample = used <= 0 ? 0 : sampleSum / used;

        r.comboScore = clamp01(comboBase);
        r.comboRisk = clamp01(comboRisk);
        r.confidence = clamp01(avgConf * 0.70d + sampleConfidence(avgSample) * 0.30d);
        r.avgHitRate = clamp01(avgHit);
        r.k3HitRate = clamp01(k3HitRate);
        r.support = clamp01(support / used);
        r.penalty = clamp01(penalty / used);
        r.sample = avgSample;
        r.featureDistance = used <= 0 ? 1.0d : clamp01(featureDistanceSum / used);
        r.sampleTrust = used <= 0 ? 0.0d : clamp01(sampleTrustSum / used);
        /*
         * FIX02:
         * recentEffectiveSum 是三个号码近期率之和，近似 K3 覆盖近期率。
         * 旧版除以 used 变回单号码 0.10 左右，再拿去跟 0.30 K3阈值比较，
         * 导致 Teacher 长期 WEAK。
         */
        r.recentEffectiveness = used <= 0 ? 0.0d : clamp01(recentEffectiveSum);
        r.similarWindowSample = similarSampleSum;
        r.similarWindowHitRate = r.k3HitRate;
        r.evidenceContribution =
                clamp01(
                        r.support * 0.34d
                                + r.comboScore * 0.28d
                                + r.sampleTrust * 0.16d
                                + r.recentEffectiveness * 0.12d
                                + (1.0d - r.featureDistance) * 0.10d);
        r.counterContribution =
                clamp01(
                        r.penalty * 0.34d
                                + r.comboRisk * 0.30d
                                + (1.0d - r.sampleTrust) * 0.18d
                                + r.featureDistance * 0.10d
                                + negPart(r.recentEffectiveness - BASE_K3_RATE) * 0.08d);
        r.stateKey =
                "TNM_COMBO|P"
                        + position
                        + "|"
                        + shortKey(stateSb.toString(), 120)
                        + "|S"
                        + bucketSample(avgSample)
                        + "|E"
                        + bucketTeacher(r.evidenceContribution)
                        + "|R"
                        + bucketRisk(r.counterContribution);
        r.evidenceText = buildComboEvidenceText(r);
        r.counterText = buildComboCounterText(r);
        r.status =
                comboStatus(
                        r.comboScore,
                        r.comboRisk,
                        r.confidence,
                        r.sample,
                        r.sampleTrust,
                        r.recentEffectiveness,
                        r.evidenceContribution,
                        r.counterContribution);
        r.actionAdvice = buildComboActionAdvice(r);

        r.summary =
                "教师号码矩阵｜位置="
                        + position
                        + "｜K3="
                        + n1
                        + ","
                        + n2
                        + ","
                        + n3
                        + "｜样本="
                        + r.sample
                        + "｜K3历史率="
                        + percent(r.k3HitRate)
                        + "｜号码均率="
                        + percent(r.avgHitRate)
                        + "｜教师分="
                        + percent(r.comboScore)
                        + "｜风险="
                        + percent(r.comboRisk)
                        + "｜置信="
                        + percent(r.confidence)
                        + "｜状态="
                        + r.status
                        + "｜相似窗样本="
                        + r.similarWindowSample
                        + "｜特征距离="
                        + percent(1.0d - r.featureDistance)
                        + "｜证据贡献="
                        + percent(r.evidenceContribution)
                        + "｜反证贡献="
                        + percent(r.counterContribution)
                        + "｜建议="
                        + r.actionAdvice
                        + "｜说明=只写教师矩阵字段，不直接放行BET";

        return r;
    }

    private void applyDeterministicTeacherState(
            TeacherStat s, Models.PreDrawFeature f, Models.PositionHealth h) {
        if (s == null) {
            return;
        }

        double slope = f == null ? s.hotColdAdjust : getDouble(f, "freqSlope", s.hotColdAdjust);
        double gapScore =
                f == null ? s.returnSupport : getDouble(f, "returnZoneScore", s.returnSupport);
        double density =
                f == null
                        ? s.featureSupport
                        : getDouble(
                                f, "densityScore", getDouble(f, "featureScore", s.featureSupport));
        double repeat =
                f == null ? 0.0d : getDouble(f, "repeatScore", getDouble(f, "repeatDensity", 0.0d));
        double neighbor =
                f == null
                        ? 0.0d
                        : getDouble(f, "neighborScore", getDouble(f, "nearNumberScore", 0.0d));
        double tail =
                f == null ? 0.0d : getDouble(f, "sameTailScore", getDouble(f, "tailScore", 0.0d));
        double crowd =
                h == null
                        ? 0.0d
                        : getDouble(h, "crowdScore", getDouble(h, "positionCrowdScore", 0.0d));
        double sync =
                f == null
                        ? 0.0d
                        : getDouble(
                                f, "crossPositionSync", getDouble(f, "positionSyncScore", 0.0d));
        double squeeze =
                f == null
                        ? 0.0d
                        : getDouble(f, "crossPositionSqueeze", getDouble(f, "squeezeScore", 0.0d));
        double timing =
                f == null
                        ? s.transitionSupport
                        : getDouble(
                                f,
                                "timingShiftScore",
                                getDouble(f, "transitionLift", s.transitionSupport));
        int missing = f == null ? 0 : getInt(f, "missingGap", getInt(f, "omissionGap", 0));
        int lose = f == null ? 0 : getInt(f, "recentLoseStreak", getInt(f, "loseStreak", 0));

        s.hotColdBucket = bucketSigned("HC", slope);
        s.omissionBucket = bucketOmission(missing, gapScore);
        s.densityBucket = bucket01("DEN", density);
        s.repeatBucket = bucket01("REP", repeat);
        s.oddEvenBias = (s.number % 2 == 0 ? "OE_EVEN" : "OE_ODD");
        s.highLowBias = (s.number >= 6 ? "HL_HIGH" : "HL_LOW");
        s.neighborTailState = bucket01("NEI", Math.max(neighbor, tail));
        s.positionCrowdState = bucket01("CROWD", crowd);
        s.crossPositionSync = bucket01("SYNC", sync);
        s.crossPositionSqueeze = bucket01("SQUEEZE", squeeze);
        s.timingShift = bucket01("TIME", timing);
        s.recentLoseStreak = lose;
        s.windowStateKey = buildTeacherWindowStateKey(s.position, s.number, s);
        if (s.stateKey == null || s.stateKey.length() == 0) {
            s.stateKey = s.windowStateKey;
        }

        s.featureDistance = calcTeacherFeatureDistance(s, f, h);
        s.similarWindowSample = Math.max(s.sampleCount, s.recentSample);
        s.similarWindowHitRate =
                s.sampleCount <= 0 ? s.hitRate : safeRate(s.hitCount, s.sampleCount);
        s.similarWindowRecentRate =
                s.recentSample <= 0
                        ? s.similarWindowHitRate
                        : safeRate(s.recentHit, s.recentSample);
        s.sampleTrust = sampleConfidence(Math.max(s.sampleCount, s.similarWindowSample));
        s.recentEffectiveness =
                s.similarWindowRecentRate > 0.0d ? s.similarWindowRecentRate : s.recentHitRate;

        if (s.evidenceText == null) {
            s.evidenceText = "";
        }
        if (s.counterText == null) {
            s.counterText = "";
        }
        if (s.actionAdvice == null) {
            s.actionAdvice = "";
        }
    }

    private String buildTeacherWindowStateKey(int position, int number, TeacherStat s) {
        if (s == null) {
            return "TNM_WS|P" + position + "|N" + number + "|EMPTY";
        }
        String key =
                "TNM_WS|P"
                        + position
                        + "|N"
                        + number
                        + "|"
                        + s.hotColdBucket
                        + "|"
                        + s.omissionBucket
                        + "|"
                        + s.densityBucket
                        + "|"
                        + s.repeatBucket
                        + "|"
                        + s.oddEvenBias
                        + "|"
                        + s.highLowBias
                        + "|"
                        + s.neighborTailState
                        + "|"
                        + s.positionCrowdState
                        + "|"
                        + s.crossPositionSync
                        + "|"
                        + s.crossPositionSqueeze
                        + "|L"
                        + s.recentLoseStreak
                        + "|"
                        + s.timingShift;
        if (key.length() > 220) {
            key = key.substring(0, 180) + "|" + Math.abs(key.hashCode());
        }
        return key;
    }

    private double calcTeacherFeatureDistance(
            TeacherStat s, Models.PreDrawFeature f, Models.PositionHealth h) {
        if (s == null) {
            return 1.0d;
        }
        double d = 0.0d;
        d += Math.abs(s.hitRate - BASE_NUMBER_RATE) * 0.16d;
        d += Math.abs(s.recentHitRate - s.hitRate) * 0.18d;
        d += Math.abs(s.hotColdAdjust) * 0.16d;
        d += (1.0d - s.featureSupport) * 0.12d;
        d += s.riskPenalty * 0.12d;
        d += s.falseStrongPenalty * 0.10d;
        d += (1.0d - s.confidence) * 0.10d;
        if (f == null) {
            d += 0.10d;
        }
        if (h == null) {
            d += 0.06d;
        }
        return clamp01(d);
    }

    private String buildTeacherEvidenceText(TeacherStat s) {
        if (s == null) {
            return "证据贡献=空";
        }
        return "证据贡献="
                + percent(s.evidenceContribution)
                + "｜特征="
                + percent(s.featureSupport)
                + "｜因链="
                + percent(s.causeSupport)
                + "｜候选="
                + percent(s.candidateSupport)
                + "｜学习="
                + percent(s.learningSupport)
                + "｜相似窗="
                + percent(s.similarWindowHitRate)
                + "｜样本可信="
                + percent(s.sampleTrust);
    }

    private String buildTeacherCounterText(TeacherStat s) {
        if (s == null) {
            return "反证贡献=空";
        }
        return "反证贡献="
                + percent(s.counterContribution)
                + "｜教师风险="
                + percent(s.teacherRisk)
                + "｜风险扣减="
                + percent(s.riskPenalty)
                + "｜伪强="
                + percent(s.falseStrongPenalty)
                + "｜特征距离="
                + percent(s.featureDistance)
                + "｜近期有效="
                + percent(s.recentEffectiveness);
    }

    private String buildTeacherActionAdvice(TeacherStat s) {
        if (s == null) {
            return "NO_SAMPLE_NEUTRAL：无教师样本，中性不硬拦";
        }
        if (s.counterContribution >= 0.70d || s.teacherRisk >= 0.72d) {
            return "HOLD：反证强，只做风险解释";
        }
        if ("STRONG".equals(statusOf(s))
                && s.evidenceContribution >= 0.56d
                && s.counterContribution <= 0.46d
                && s.sampleTrust >= 0.30d) {
            return "ALLOW_TO_GATE：教师号码矩阵强支持，仍由最终门裁决";
        }
        if ("READY".equals(statusOf(s))
                && s.evidenceContribution >= 0.48d
                && s.counterContribution <= 0.58d) {
            return "WATCH_TO_GATE：教师READY，只允许入围复核，不直接BET";
        }
        return "WEAK_WATCH：教师样本弱或近期有效性不足，只降权观察，不硬冻结";
    }

    private String buildComboEvidenceText(ComboTeacherResult r) {
        if (r == null) {
            return "证据贡献=空";
        }
        return "证据贡献="
                + percent(r.evidenceContribution)
                + "｜K3历史率="
                + percent(r.k3HitRate)
                + "｜号码均率="
                + percent(r.avgHitRate)
                + "｜支撑="
                + percent(r.support)
                + "｜样本可信="
                + percent(r.sampleTrust)
                + "｜相似窗样本="
                + r.similarWindowSample;
    }

    private String buildComboCounterText(ComboTeacherResult r) {
        if (r == null) {
            return "反证贡献=空";
        }
        return "反证贡献="
                + percent(r.counterContribution)
                + "｜风险="
                + percent(r.comboRisk)
                + "｜惩罚="
                + percent(r.penalty)
                + "｜特征距离="
                + percent(r.featureDistance)
                + "｜近期有效="
                + percent(r.recentEffectiveness);
    }

    private String buildComboActionAdvice(ComboTeacherResult r) {
        if (r == null) {
            return "NO_SAMPLE_NEUTRAL：无教师组合，中性不硬拦";
        }
        if ("NO_SAMPLE_NEUTRAL".equals(r.status)) {
            return "NO_SAMPLE_NEUTRAL：教师组合无样本，不加权、不硬拦，交V20/DecisionGate裁决";
        }
        if ("FREEZE".equals(r.status)) {
            return "FREEZE：教师矩阵连续误导或反证过强，只允许SHADOW复核";
        }
        if ("RISK".equals(r.status) || "FREEZE".equals(r.status)) {
            return "HOLD：教师矩阵近期无效或反证偏强";
        }
        if ("STRONG".equals(r.status)
                && r.evidenceContribution >= 0.58d
                && r.counterContribution <= 0.48d
                && r.sampleTrust >= 0.38d) {
            return "ALLOW_TO_GATE：教师强域有效，送最终门复核";
        }
        if ("READY".equals(r.status)
                && r.evidenceContribution >= 0.48d
                && r.counterContribution <= 0.58d) {
            return "WATCH_TO_GATE：教师READY，允许入围但不直接BET";
        }
        return "WEAK_WATCH：教师样本弱或近期有效性不足，只降权观察，不硬冻结";
    }

    private String bucketOmission(int gap, double score) {
        if (gap >= 24 || score >= 0.72d) {
            return "MISS_LONG";
        }
        if (gap >= 12 || score >= 0.52d) {
            return "MISS_MID";
        }
        if (gap >= 6 || score >= 0.28d) {
            return "MISS_SHORT";
        }
        return "MISS_NONE";
    }

    private String bucket01(String prefix, double v) {
        if (prefix == null || prefix.length() == 0) {
            prefix = "B";
        }
        if (v >= 0.72d) {
            return prefix + "_VH";
        }
        if (v >= 0.55d) {
            return prefix + "_H";
        }
        if (v >= 0.35d) {
            return prefix + "_M";
        }
        if (v > 0.0d) {
            return prefix + "_L";
        }
        return prefix + "_N";
    }

    private String shortKey(String s, int max) {
        if (s == null) {
            return "";
        }
        if (max <= 0 || s.length() <= max) {
            return s;
        }
        return s.substring(0, max) + "#" + Math.abs(s.hashCode());
    }

    private String buildTeacherOrdinaryBetPolicy(String status) {
        String s = normalizeTeacherStatus(status);
        if ("STRONG".equals(s)) {
            return "ALLOW_REVIEW_STRONG";
        }
        if ("READY".equals(s)) {
            return "ALLOW_REVIEW_READY";
        }
        if ("WATCH".equals(s)) {
            return "WATCH_ONLY";
        }
        if ("NO_SAMPLE_NEUTRAL".equals(s)) {
            return "NO_SAMPLE_NEUTRAL_NO_HARD_BLOCK";
        }
        if ("RISK".equals(s) || "FREEZE".equals(s)) {
            return "RISK_NO_BET";
        }
        return "WEAK_NO_BET";
    }

    private String buildTeacherOrdinaryBetPolicy(String teacherStatus, ComboTeacherResult r) {
        String s = normalizeTeacherStatus(teacherStatus);
        if ("FREEZE".equals(s)) {
            return "BLOCK_REAL_BET：教师FREEZE，禁止普通BET";
        }
        if ("RISK".equals(s)) {
            return "BLOCK_REAL_BET：教师RISK，禁止普通BET";
        }
        if ("WEAK".equals(s)) {
            return "WEAK_WATCH：教师WEAK只降权观察，非硬冻结，等待V20/SHADOW强域复核";
        }
        if ("NO_SAMPLE_NEUTRAL".equals(s)) {
            return "NO_SAMPLE_NEUTRAL：教师无样本中性，不加权、不硬拦，交V20/DecisionGate复核";
        }
        if ("STRONG".equals(s)) {
            return "SUPPORT_FINAL_GATE：教师STRONG可支持最终门，但不直接BET";
        }
        if ("READY".equals(s)) {
            return "WATCH_TO_GATE：教师READY只支持入围复核";
        }
        return "WATCH_NEUTRAL：教师状态不足，只观察不救援";
    }

    public void applyTeacherFieldsFromCandidateToObject(Object target, Object candidate) {
        if (target == null || candidate == null) {
            return;
        }
        String status =
                normalizeTeacherStatus(
                        getString(
                                candidate,
                                "teacherMatrixStatus",
                                getString(
                                        candidate,
                                        "teacherStatus",
                                        getString(candidate, "teacherNumberStatus", ""))));
        putString(target, "teacherMatrixStatus", status);
        putString(target, "teacherStatus", status);
        putString(target, "teacherNumberStatus", status);
        putString(target, "teacherStatusClass", status);
        putString(target, "teacherOrdinaryBetPolicy", buildTeacherOrdinaryBetPolicy(status));
        putString(target, "teacherMatrixOrdinaryBetPolicy", buildTeacherOrdinaryBetPolicy(status));
        putString(target, "teacherFieldVersion", V20_TEACHER_UPSTREAM_VERSION);
        putString(
                target,
                "teacherFieldSource",
                "TeacherNumberMatrixEngine.applyTeacherResultToObject");
        putInt(
                target,
                "teacherWeakNoBetFlag",
                ("WEAK".equals(status) || "RISK".equals(status) || "FREEZE".equals(status))
                        ? 1
                        : 0);
        putInt(target, "teacherNoSampleNeutralFlag", "NO_SAMPLE_NEUTRAL".equals(status) ? 1 : 0);
        putInt(
                target,
                "teacherMatrixWeakNoBetFlag",
                ("WEAK".equals(status) || "RISK".equals(status) || "FREEZE".equals(status))
                        ? 1
                        : 0);
        putInt(
                target,
                "teacherMatrixNoSampleNeutralFlag",
                "NO_SAMPLE_NEUTRAL".equals(status) ? 1 : 0);
        putInt(
                target,
                "teacherMatrixOrdinaryBetBlockFlag",
                ("STRONG".equals(status)
                                || "READY".equals(status)
                                || "NO_SAMPLE_NEUTRAL".equals(status))
                        ? 0
                        : 1);
        putInt(
                target,
                "teacherMatrixSupportsFinalGate",
                ("STRONG".equals(status) || "READY".equals(status)) ? 1 : 0);
        putInt(
                target,
                "teacherMatrixSupportsBet",
                ("STRONG".equals(status) || "READY".equals(status)) ? 1 : 0);
        putString(target, "teacherStatusClass", status);
        putInt(
                target,
                "teacherMatrixApplied",
                getInt(candidate, "teacherMatrixApplied", getInt(candidate, "teacherApplied", 0)));
        putInt(
                target,
                "teacherMatrixReady",
                ("STRONG".equals(status) || "READY".equals(status)) ? 1 : 0);
        putInt(
                target,
                "teacherMatrixRiskFlag",
                ("RISK".equals(status) || "FREEZE".equals(status)) ? 1 : 0);
        putInt(target, "teacherMatrixFreezeFlag", "FREEZE".equals(status) ? 1 : 0);
        putInt(target, "teacherWeakNoBetFlag", "WEAK".equals(status) ? 1 : 0);
        putInt(target, "teacherMatrixWeakNoBetFlag", "WEAK".equals(status) ? 1 : 0);
        putInt(
                target,
                "teacherMatrixOrdinaryBetBlockFlag",
                "WEAK".equals(status) || "RISK".equals(status) || "FREEZE".equals(status) ? 1 : 0);
        putInt(
                target,
                "teacherMatrixSupportsFinalGate",
                ("STRONG".equals(status) || "READY".equals(status)) ? 1 : 0);
        putDouble(
                target,
                "teacherMatrixScore",
                getDouble(
                        candidate,
                        "teacherMatrixScore",
                        getDouble(
                                candidate,
                                "teacherScore",
                                getDouble(candidate, "teacherNumberScore", 0.0d))));
        putDouble(
                target,
                "teacherNumberScore",
                getDouble(
                        candidate,
                        "teacherNumberScore",
                        getDouble(
                                candidate,
                                "teacherMatrixScore",
                                getDouble(candidate, "teacherScore", 0.0d))));
        putDouble(
                target,
                "teacherScore",
                getDouble(
                        candidate,
                        "teacherScore",
                        getDouble(
                                candidate,
                                "teacherMatrixScore",
                                getDouble(candidate, "teacherNumberScore", 0.0d))));
        putDouble(
                target,
                "teacherMatrixRisk",
                getDouble(
                        candidate,
                        "teacherMatrixRisk",
                        getDouble(
                                candidate,
                                "teacherRisk",
                                getDouble(candidate, "teacherNumberRisk", 0.0d))));
        putDouble(
                target,
                "teacherNumberRisk",
                getDouble(
                        candidate,
                        "teacherNumberRisk",
                        getDouble(
                                candidate,
                                "teacherMatrixRisk",
                                getDouble(candidate, "teacherRisk", 0.0d))));
        putDouble(
                target,
                "teacherRisk",
                getDouble(
                        candidate,
                        "teacherRisk",
                        getDouble(
                                candidate,
                                "teacherMatrixRisk",
                                getDouble(candidate, "teacherNumberRisk", 0.0d))));
        putDouble(
                target,
                "teacherMatrixConfidence",
                getDouble(
                        candidate,
                        "teacherMatrixConfidence",
                        getDouble(
                                candidate,
                                "teacherConfidence",
                                getDouble(candidate, "teacherNumberConfidence", 0.0d))));
        putDouble(
                target,
                "teacherMatrixHitRate",
                getDouble(
                        candidate,
                        "teacherMatrixHitRate",
                        getDouble(
                                candidate,
                                "teacherHitRate",
                                getDouble(candidate, "teacherNumberHitRate", 0.0d))));
        putInt(
                target,
                "teacherMatrixSample",
                getInt(
                        candidate,
                        "teacherMatrixSample",
                        getInt(
                                candidate,
                                "teacherSample",
                                getInt(candidate, "teacherNumberSample", 0))));
        putDouble(
                target,
                "teacherEvidenceContribution",
                getDouble(
                        candidate,
                        "teacherEvidenceContribution",
                        getDouble(candidate, "teacherMatrixEvidenceContribution", 0.0d)));
        putDouble(
                target,
                "teacherCounterContribution",
                getDouble(
                        candidate,
                        "teacherCounterContribution",
                        getDouble(candidate, "teacherMatrixCounterContribution", 0.0d)));
        putDouble(
                target,
                "teacherSampleReliability",
                getDouble(
                        candidate,
                        "teacherSampleReliability",
                        getDouble(candidate, "teacherMatrixSampleReliability", 0.0d)));
        putDouble(
                target,
                "teacherRecentValidity",
                getDouble(
                        candidate,
                        "teacherRecentValidity",
                        getDouble(candidate, "teacherMatrixRecentValidity", 0.0d)));
        putString(
                target,
                "teacherActionAdvice",
                getString(
                        candidate,
                        "teacherActionAdvice",
                        getString(candidate, "teacherMatrixActionAdvice", "")));
        putString(
                target,
                "teacherMatrixActionAdvice",
                getString(
                        candidate,
                        "teacherMatrixActionAdvice",
                        getString(candidate, "teacherActionAdvice", "")));
        putString(
                target,
                "teacherMatrixSummary",
                getString(
                        candidate,
                        "teacherMatrixSummary",
                        getString(
                                candidate,
                                "teacherSummary",
                                getString(candidate, "teacherNumberSummary", ""))));
        putString(
                target,
                "teacherSummary",
                getString(
                        candidate,
                        "teacherSummary",
                        getString(
                                candidate,
                                "teacherMatrixSummary",
                                getString(candidate, "teacherNumberSummary", ""))));
        putString(
                target,
                "teacherMatrixStateKey",
                getString(
                        candidate,
                        "teacherMatrixStateKey",
                        getString(candidate, "teacherWindowStateKey", "")));
        putString(
                target,
                "teacherWindowStateKey",
                getString(
                        candidate,
                        "teacherWindowStateKey",
                        getString(candidate, "teacherMatrixStateKey", "")));
        putString(
                target,
                "teacherOrdinaryBetPolicy",
                getString(
                        candidate,
                        "teacherOrdinaryBetPolicy",
                        buildTeacherOrdinaryBetPolicy(status, null)));
        putString(target, "teacherFieldSource", "TeacherNumberMatrixEngine.copyCandidate");
        putString(target, "teacherFieldVersion", V20_TEACHER_VERSION);
    }

    private String teacherStatusTier(String status) {
        String s = normalizeTeacherStatus(status);
        if (STATUS_STRONG.equals(s)) return "TIER_120_STRONG";
        if (STATUS_READY.equals(s)) return "TIER_30_READY";
        if (STATUS_WATCH.equals(s)) return "TIER_60_WATCH";
        if (STATUS_NO_SAMPLE.equals(s)) return "TIER_0_NO_SAMPLE_NEUTRAL";
        if (STATUS_WEAK.equals(s)) return "TIER_WEAK_SAMPLE";
        if (STATUS_RISK.equals(s)) return "TIER_RISK";
        if (STATUS_FREEZE.equals(s)) return "TIER_FREEZE";
        return "TIER_WATCH";
    }

    private String teacherRiskClass(String status) {
        String s = normalizeTeacherStatus(status);
        if (STATUS_FREEZE.equals(s)) return "HARD_FREEZE";
        if (STATUS_RISK.equals(s)) return "HARD_RISK";
        if (STATUS_NO_SAMPLE.equals(s)) return "NEUTRAL_NO_SAMPLE";
        if (STATUS_WEAK.equals(s)) return "SOFT_WEAK";
        if (STATUS_READY.equals(s) || STATUS_STRONG.equals(s)) return "SUPPORT_ONLY";
        return "WATCH";
    }

    private String normalizeTeacherStatus(String status) {
        if (status == null || status.length() == 0) {
            return "NO_SAMPLE_NEUTRAL";
        }
        String s = status.toUpperCase(Locale.US);
        if (s.indexOf("FREEZE") >= 0 || s.indexOf("冻结") >= 0) {
            return "FREEZE";
        }
        if (s.indexOf("RISK") >= 0 || s.indexOf("风险") >= 0) {
            return "RISK";
        }
        if (s.indexOf("STRONG") >= 0 || s.indexOf("强") >= 0) {
            return "STRONG";
        }
        if (s.indexOf("READY") >= 0
                || s.indexOf("PASS_TO_GATE") >= 0
                || s.indexOf("ALLOW_TO_GATE") >= 0
                || s.indexOf("准备") >= 0) {
            return "READY";
        }
        if (s.indexOf("WATCH") >= 0 || s.indexOf("观察") >= 0 || s.indexOf("NORMAL") >= 0) {
            return "WATCH";
        }
        if (s.indexOf("NO_SAMPLE") >= 0
                || s.indexOf("NO_TEACHER") >= 0
                || s.indexOf("NONE") >= 0
                || s.indexOf("UNKNOWN") >= 0
                || s.indexOf("无样本") >= 0) {
            return "NO_SAMPLE_NEUTRAL";
        }
        if (s.indexOf("WEAK") >= 0 || s.indexOf("弱") >= 0) {
            return "WEAK";
        }
        return "WATCH";
    }

    public void applyTeacherResultToObject(Object target, Object source) {
        if (target == null || source == null) {
            return;
        }
        String status =
                normalizeTeacherStatus(
                        getString(
                                source,
                                "teacherMatrixStatus",
                                getString(
                                        source, "teacherStatus", getString(source, "status", ""))));
        putString(target, "teacherMatrixStatus", status);
        putString(target, "teacherStatus", status);
        putString(target, "teacherNumberStatus", status);
        putString(target, "teacherStatusTier", teacherStatusTier(status));
        putString(target, "teacherRiskClass", teacherRiskClass(status));
        putInt(target, "teacherNoSampleNeutralFlag", "NO_SAMPLE_NEUTRAL".equals(status) ? 1 : 0);
        putInt(
                target,
                "teacherMatrixNoSampleNeutralFlag",
                "NO_SAMPLE_NEUTRAL".equals(status) ? 1 : 0);
        putDouble(
                target,
                "teacherMatrixScore",
                getDouble(
                        source,
                        "teacherMatrixScore",
                        getDouble(
                                source,
                                "teacherScore",
                                getDouble(source, "teacherNumberScore", 0.0d))));
        putDouble(
                target,
                "teacherMatrixRisk",
                getDouble(
                        source,
                        "teacherMatrixRisk",
                        getDouble(
                                source,
                                "teacherRisk",
                                getDouble(source, "teacherNumberRisk", 0.0d))));
        putDouble(
                target,
                "teacherMatrixConfidence",
                getDouble(
                        source,
                        "teacherMatrixConfidence",
                        getDouble(
                                source,
                                "teacherConfidence",
                                getDouble(source, "teacherNumberConfidence", 0.0d))));
        putInt(
                target,
                "teacherMatrixSample",
                getInt(
                        source,
                        "teacherMatrixSample",
                        getInt(source, "teacherSample", getInt(source, "teacherNumberSample", 0))));
        putDouble(
                target,
                "teacherMatrixHitRate",
                getDouble(
                        source,
                        "teacherMatrixHitRate",
                        getDouble(
                                source,
                                "teacherHitRate",
                                getDouble(source, "teacherNumberHitRate", 0.0d))));
        putDouble(
                target,
                "teacherEvidenceContribution",
                getDouble(
                        source,
                        "teacherEvidenceContribution",
                        getDouble(source, "teacherMatrixEvidenceContribution", 0.0d)));
        putDouble(
                target,
                "teacherCounterContribution",
                getDouble(
                        source,
                        "teacherCounterContribution",
                        getDouble(source, "teacherMatrixCounterContribution", 0.0d)));
        putDouble(
                target,
                "teacherSampleReliability",
                getDouble(
                        source,
                        "teacherSampleReliability",
                        getDouble(source, "teacherMatrixSampleReliability", 0.0d)));
        putDouble(
                target,
                "teacherRecentValidity",
                getDouble(
                        source,
                        "teacherRecentValidity",
                        getDouble(source, "teacherMatrixRecentValidity", 0.0d)));
        putString(
                target,
                "teacherActionAdvice",
                getString(
                        source,
                        "teacherActionAdvice",
                        getString(source, "teacherMatrixActionAdvice", "")));
        putString(
                target,
                "teacherMatrixSummary",
                getString(source, "teacherMatrixSummary", getString(source, "teacherSummary", "")));
    }

    private String statusOf(TeacherStat s) {
        if (s == null) {
            return STATUS_NO_SAMPLE;
        }

        int sample = s.sampleCount;
        double hitRate = s.hitRate;
        double recentRate = s.recentSample > 0 ? s.recentHitRate : s.hitRate;
        double trust = s.sampleTrust;
        double recentValidity = s.recentEffectiveness;
        double evidence = s.evidenceContribution;
        double counter = s.counterContribution;

        if (sample <= 0) {
            return STATUS_NO_SAMPLE;
        }

        if (sample >= TEACHER_FREEZE_MIN_SAMPLE
                && (s.teacherRisk >= 0.76d || counter >= 0.70d || s.falseStrongPenalty >= 0.70d)
                && recentRate < NUMBER_RISK_RATE
                && evidence + 0.04d < counter) {
            return STATUS_FREEZE;
        }

        if (sample >= TEACHER_READY_MIN_SAMPLE
                && (s.teacherRisk >= 0.68d || counter >= 0.66d || recentRate < NUMBER_RISK_RATE)
                && evidence <= counter + 0.03d) {
            return STATUS_RISK;
        }

        if (sample >= TEACHER_STRONG_MIN_SAMPLE
                && hitRate >= NUMBER_STRONG_MIN_RATE
                && recentRate >= NUMBER_READY_MIN_RATE
                && s.teacherScore >= 0.50d
                && s.teacherRisk <= 0.56d
                && evidence >= counter + 0.06d
                && trust >= 0.60d
                && recentValidity >= NUMBER_READY_MIN_RATE) {
            return STATUS_STRONG;
        }

        if (sample >= TEACHER_READY_MIN_SAMPLE
                && hitRate >= NUMBER_READY_MIN_RATE
                && recentRate >= NUMBER_RISK_RATE
                && s.teacherScore >= 0.38d
                && s.teacherRisk <= 0.68d
                && evidence >= counter - 0.06d) {
            return STATUS_READY;
        }

        if (sample >= TEACHER_STABLE_MIN_SAMPLE
                && (hitRate >= NUMBER_RISK_RATE
                        || recentRate >= NUMBER_RISK_RATE
                        || evidence >= counter - 0.10d)) {
            return STATUS_WATCH;
        }

        return STATUS_WEAK;
    }

    private String comboStatus(double score, double risk, double confidence) {
        return comboStatus(score, risk, confidence, 0, 0.0d, 0.0d, score, risk);
    }

    private String comboStatus(
            double score,
            double risk,
            double confidence,
            int sample,
            double sampleTrust,
            double recentEffectiveness,
            double evidence,
            double counter) {
        if (sample <= 0) {
            return STATUS_NO_SAMPLE;
        }

        if (sample >= TEACHER_FREEZE_MIN_SAMPLE
                && (risk >= 0.76d || counter >= 0.70d)
                && recentEffectiveness < COMBO_RISK_RATE
                && evidence + 0.04d < counter) {
            return STATUS_FREEZE;
        }

        if (sample >= TEACHER_READY_MIN_SAMPLE
                && (risk >= 0.68d || counter >= 0.66d || recentEffectiveness < COMBO_RISK_RATE)
                && evidence <= counter + 0.03d) {
            return STATUS_RISK;
        }

        if (sample >= TEACHER_STRONG_MIN_SAMPLE
                && score >= 0.50d
                && risk <= 0.56d
                && evidence >= counter + 0.06d
                && sampleTrust >= 0.60d
                && recentEffectiveness >= COMBO_STRONG_MIN_RATE) {
            return STATUS_STRONG;
        }

        if (sample >= TEACHER_READY_MIN_SAMPLE
                && score >= 0.38d
                && risk <= 0.68d
                && evidence >= counter - 0.06d
                && recentEffectiveness >= COMBO_READY_MIN_RATE) {
            return STATUS_READY;
        }

        if (sample >= TEACHER_STABLE_MIN_SAMPLE
                && (score >= 0.30d
                        || recentEffectiveness >= COMBO_RISK_RATE
                        || evidence >= counter - 0.10d)) {
            return STATUS_WATCH;
        }

        return STATUS_WEAK;
    }

    private String buildSummary(int position, int number, TeacherStat s, String status) {
        StringBuilder sb = new StringBuilder();

        sb.append("V20教师号码矩阵｜位置=");
        sb.append(position);
        sb.append("｜号码=");
        sb.append(number);
        sb.append("｜状态=");
        sb.append(status);
        sb.append("｜教师分=");
        sb.append(percent(s.teacherScore));
        sb.append("｜风险=");
        sb.append(percent(s.teacherRisk));
        sb.append("｜置信=");
        sb.append(percent(s.confidence));
        sb.append("｜样本=");
        sb.append(s.sampleCount);
        sb.append("｜命中=");
        sb.append(s.hitCount);
        sb.append("｜命中率=");
        sb.append(percent(s.hitRate));
        sb.append("｜近期=");
        sb.append(percent(s.recentHitRate));
        sb.append("｜特征支撑=");
        sb.append(percent(s.featureSupport));
        sb.append("｜位置支撑=");
        sb.append(percent(s.positionSupport));
        sb.append("｜因链支撑=");
        sb.append(percent(s.causeSupport));
        sb.append("｜候选支撑=");
        sb.append(percent(s.candidateSupport));
        sb.append("｜学习支撑=");
        sb.append(percent(s.learningSupport));
        sb.append("｜回归=");
        sb.append(percent(s.returnSupport));
        sb.append("｜转移=");
        sb.append(percent(s.transitionSupport));
        sb.append("｜热冷纠偏=");
        sb.append(signed(s.hotColdAdjust));
        sb.append("｜风险扣减=");
        sb.append(percent(s.riskPenalty));
        sb.append("｜伪强扣减=");
        sb.append(percent(s.falseStrongPenalty));
        sb.append("｜windowState=");
        sb.append(s.windowStateKey);
        sb.append("｜相似窗样本=");
        sb.append(s.similarWindowSample);
        sb.append("｜相似窗率=");
        sb.append(percent(s.similarWindowHitRate));
        sb.append("｜特征距离=");
        sb.append(percent(1.0d - s.featureDistance));
        sb.append("｜样本可信=");
        sb.append(percent(s.sampleTrust));
        sb.append("｜近期有效=");
        sb.append(percent(s.recentEffectiveness));
        sb.append("｜证据贡献=");
        sb.append(percent(s.evidenceContribution));
        sb.append("｜反证贡献=");
        sb.append(percent(s.counterContribution));
        sb.append("｜建议=");
        sb.append(s.actionAdvice);
        sb.append("｜说明=教师号码矩阵只辅助排序和解释，不创建BET，不污染真实校准");

        return sb.toString();
    }

    private void rerankCandidates(List<Models.CandidateK3> candidates) {
        if (candidates == null || candidates.size() <= 1) {
            return;
        }

        Collections.sort(
                candidates,
                new Comparator<Models.CandidateK3>() {
                    public int compare(Models.CandidateK3 a, Models.CandidateK3 b) {
                        double sa =
                                getDouble(a, "causeScore", 0.0d) * 0.70d
                                        + getDouble(a, "teacherNumberScore", 0.0d) * 0.20d
                                        - getDouble(a, "teacherNumberRisk", 0.0d) * 0.10d;

                        double sb =
                                getDouble(b, "causeScore", 0.0d) * 0.70d
                                        + getDouble(b, "teacherNumberScore", 0.0d) * 0.20d
                                        - getDouble(b, "teacherNumberRisk", 0.0d) * 0.10d;

                        if (sa > sb) {
                            return -1;
                        }

                        if (sa < sb) {
                            return 1;
                        }

                        int pa = getInt(a, "position", 0);
                        int pb = getInt(b, "position", 0);

                        if (pa != pb) {
                            return pa - pb;
                        }

                        double fa = getDouble(a, "finalP", 0.0d);
                        double fb = getDouble(b, "finalP", 0.0d);

                        if (fa > fb) {
                            return -1;
                        }

                        if (fa < fb) {
                            return 1;
                        }

                        return 0;
                    }
                });

        for (int i = 0; i < candidates.size(); i++) {
            Models.CandidateK3 c = candidates.get(i);

            if (c != null) {
                putInt(c, "rankNo", i + 1);
                putInt(c, "rank", i + 1);
            }
        }
    }

    private void rerankComboCandidates(List<Models.ComboCandidate> candidates) {
        if (candidates == null || candidates.size() <= 1) {
            return;
        }

        Collections.sort(
                candidates,
                new Comparator<Models.ComboCandidate>() {
                    public int compare(Models.ComboCandidate a, Models.ComboCandidate b) {
                        double sa = comboSortScore(a);
                        double sb = comboSortScore(b);

                        if (sa > sb) {
                            return -1;
                        }

                        if (sa < sb) {
                            return 1;
                        }

                        int pa = getInt(a, "position", 0);
                        int pb = getInt(b, "position", 0);

                        if (pa != pb) {
                            return pa - pb;
                        }

                        return getInt(a, "rank", 0) - getInt(b, "rank", 0);
                    }
                });

        for (int i = 0; i < candidates.size(); i++) {
            Models.ComboCandidate c = candidates.get(i);

            if (c != null) {
                putInt(c, "rankNo", i + 1);
                putInt(c, "rank", i + 1);
            }
        }
    }

    private double comboSortScore(Models.ComboCandidate c) {
        if (c == null) {
            return -999999.0d;
        }

        double finalP = getDouble(c, "finalP", 0.0d);
        double edge = getDouble(c, "edge", 0.0d);
        double raw = getDouble(c, "rawP", finalP);
        double cause = getDouble(c, "causeScore", 0.0d);
        double risk = getDouble(c, "riskScore", 0.0d);
        double teacher =
                getDouble(c, "teacherMatrixScore", getDouble(c, "teacherNumberScore", 0.0d));
        double teacherRisk =
                getDouble(c, "teacherMatrixRisk", getDouble(c, "teacherNumberRisk", 0.0d));
        double conf =
                getDouble(
                        c,
                        "teacherMatrixConfidence",
                        getDouble(c, "teacherNumberConfidence", 0.0d));

        return finalP * 1.00d
                + raw * 0.12d
                + edge * 0.36d
                + cause * 0.08d
                + teacher * 0.035d * (0.60d + conf * 0.40d)
                - risk * 0.04d
                - teacherRisk * 0.025d;
    }

    private Models.PreDrawFeature getFeature(
            Map<String, Models.PreDrawFeature> featureMap, int position, int number) {
        if (featureMap == null) {
            return null;
        }

        Models.PreDrawFeature f = featureMap.get(keyOf(position, number));

        if (f != null) {
            return f;
        }

        f = featureMap.get(String.valueOf(position) + "|" + String.valueOf(number));

        if (f != null) {
            return f;
        }

        f = featureMap.get("P" + position + "_N" + number);

        if (f != null) {
            return f;
        }

        return featureMap.get(buildProfileKey(position, number));
    }

    private String buildCellKey(String targetIssue, int position, int number) {
        return safe(targetIssue) + "|P" + position + "|N" + number;
    }

    private String buildProfileKey(int position, int number) {
        return keyOf(position, number);
    }

    private void putAllCellKeys(
            LinkedHashMap<String, Models.NumberTeacherCell> map,
            String targetIssue,
            int position,
            int number,
            Models.NumberTeacherCell cell) {
        if (map == null || cell == null) {
            return;
        }

        map.put(keyOf(position, number), cell);
        map.put(String.valueOf(position) + "|" + String.valueOf(number), cell);
        map.put("P" + position + "_N" + number, cell);

        if (targetIssue != null && targetIssue.length() > 0) {
            map.put(buildCellKey(targetIssue, position, number), cell);
        }
    }

    private boolean containsSameCell(
            ArrayList<Models.NumberTeacherCell> list, Models.NumberTeacherCell cell) {
        if (list == null || cell == null) {
            return false;
        }

        int p = getInt(cell, "position", 0);
        int n = getInt(cell, "number", 0);

        for (int i = 0; i < list.size(); i++) {
            Models.NumberTeacherCell old = list.get(i);

            if (old == null) {
                continue;
            }

            if (getInt(old, "position", 0) == p && getInt(old, "number", 0) == n) {
                return true;
            }
        }

        return false;
    }

    private void sortTeacherCells(ArrayList<Models.NumberTeacherCell> list) {
        if (list == null || list.size() <= 1) {
            return;
        }

        Collections.sort(
                list,
                new Comparator<Models.NumberTeacherCell>() {
                    public int compare(Models.NumberTeacherCell a, Models.NumberTeacherCell b) {
                        double sa =
                                getDouble(
                                        a,
                                        "teacherScore",
                                        getDouble(a, "numberTeacherScore", 0.0d));
                        double sb =
                                getDouble(
                                        b,
                                        "teacherScore",
                                        getDouble(b, "numberTeacherScore", 0.0d));

                        if (sa > sb) {
                            return -1;
                        }

                        if (sa < sb) {
                            return 1;
                        }

                        int pa = getInt(a, "position", 0);
                        int pb = getInt(b, "position", 0);

                        if (pa != pb) {
                            return pa - pb;
                        }

                        int na = getInt(a, "number", 0);
                        int nb = getInt(b, "number", 0);

                        return na - nb;
                    }
                });
    }

    private ArrayList<Models.ComboCandidate> castComboCandidates(List raw) {
        ArrayList<Models.ComboCandidate> out = new ArrayList<Models.ComboCandidate>();

        if (raw == null || raw.size() == 0) {
            return out;
        }

        for (int i = 0; i < raw.size(); i++) {
            Object item = raw.get(i);

            if (item instanceof Models.ComboCandidate) {
                out.add((Models.ComboCandidate) item);
            }
        }

        return out;
    }

    private ArrayList<Models.Draw> castDrawList(List raw) {
        ArrayList<Models.Draw> out = new ArrayList<Models.Draw>();

        if (raw == null || raw.size() == 0) {
            return out;
        }

        for (int i = 0; i < raw.size(); i++) {
            Object item = raw.get(i);

            if (item instanceof Models.Draw) {
                Models.Draw d = (Models.Draw) item;

                if (d != null && d.isValid()) {
                    out.add(d);
                }
            }
        }

        return out;
    }

    private ArrayList<Models.Draw> normalizeHistory(List<Models.Draw> history) {
        ArrayList<Models.Draw> out = new ArrayList<Models.Draw>();

        if (history == null || history.size() == 0) {
            return out;
        }

        int start = history.size() - HISTORY_WINDOW_DEFAULT;

        if (start < 0) {
            start = 0;
        }

        for (int i = start; i < history.size(); i++) {
            Models.Draw d = history.get(i);

            if (d != null && d.isValid()) {
                out.add(d);
            }
        }

        return out;
    }

    private int getDrawNumber(Models.Draw d, int position) {
        if (d == null || !isValidPosition(position)) {
            return 0;
        }

        try {
            return d.getNumberAtPosition(position);
        } catch (Throwable e) {
            try {
                if (d.numbers != null && d.numbers.length >= position) {
                    return d.numbers[position - 1];
                }
            } catch (Throwable ignored) {
            }
        }

        return 0;
    }

    private String resolveBaseIssue(List history) {
        if (history == null || history.size() == 0) {
            return "";
        }

        for (int i = history.size() - 1; i >= 0; i--) {
            Object o = history.get(i);

            if (o instanceof Models.Draw) {
                Models.Draw d = (Models.Draw) o;

                if (d != null && d.isValid() && d.issue != null && d.issue.length() > 0) {
                    return d.issue;
                }
            }
        }

        return "";
    }

    private String buildNextIssue(String issue) {
        if (issue == null || issue.length() == 0) {
            return "";
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

    private double sampleConfidence(int sample) {
        if (sample <= 0) {
            return 0.0d;
        }

        if (sample >= 120) {
            return 1.0d;
        }

        return clamp01(((double) sample) / 120.0d);
    }

    private String bucketTeacher(double v) {
        if (v >= 0.68d) {
            return "TEACHER_STRONG";
        }

        if (v >= 0.52d) {
            return "TEACHER_NORMAL";
        }

        if (v >= 0.38d) {
            return "TEACHER_WEAK";
        }

        return "TEACHER_BAD";
    }

    private String bucketRisk(double v) {
        if (v >= 0.72d) {
            return "RISK_HIGH";
        }

        if (v >= 0.55d) {
            return "RISK_MID";
        }

        if (v >= 0.35d) {
            return "RISK_LOW";
        }

        return "RISK_CLEAN";
    }

    private String bucketSample(int sample) {
        if (sample >= 120) {
            return "SAMPLE_STRONG";
        }

        if (sample >= 60) {
            return "SAMPLE_NORMAL";
        }

        if (sample >= 20) {
            return "SAMPLE_WEAK";
        }

        return "SAMPLE_COLD";
    }

    private String bucketSigned(String prefix, double v) {
        if (prefix == null || prefix.length() == 0) {
            prefix = "V";
        }

        if (v >= 0.18d) {
            return prefix + "_UP_STRONG";
        }

        if (v >= 0.06d) {
            return prefix + "_UP";
        }

        if (v <= -0.18d) {
            return prefix + "_DOWN_STRONG";
        }

        if (v <= -0.06d) {
            return prefix + "_DOWN";
        }

        return prefix + "_FLAT";
    }

    private void appendReason(Models.CandidateK3 c, String add) {
        if (c == null || add == null || add.length() == 0) {
            return;
        }

        String old = getString(c, "reasonSummary", "");

        if (old == null || old.length() == 0) {
            putString(c, "reasonSummary", add);
        } else if (old.indexOf(add) < 0) {
            putString(c, "reasonSummary", old + "｜" + add);
        }
    }

    private boolean isValidPosition(int p) {
        return p >= POSITION_MIN && p <= POSITION_MAX;
    }

    private boolean isValidNumber(int n) {
        return n >= NUMBER_MIN && n <= NUMBER_MAX;
    }

    private double safeRate(int hit, int sample) {
        if (sample <= 0) {
            return 0.0d;
        }

        return clamp01(((double) hit) / ((double) sample));
    }

    private double posPart(double v) {
        if (v <= 0.0d) {
            return 0.0d;
        }

        if (v > 1.0d) {
            return 1.0d;
        }

        return v;
    }

    private double negPart(double v) {
        if (v >= 0.0d) {
            return 0.0d;
        }

        v = -v;

        if (v > 1.0d) {
            return 1.0d;
        }

        return v;
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

    private double clampSigned(double v) {
        if (Double.isNaN(v) || Double.isInfinite(v)) {
            return 0.0d;
        }

        if (v < -1.0d) {
            return -1.0d;
        }

        if (v > 1.0d) {
            return 1.0d;
        }

        return v;
    }

    private double limitSigned(double v, double maxAbs) {
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

    private String percent(double v) {
        return String.format(Locale.US, "%.1f%%", Double.valueOf(clamp01(v) * 100.0d));
    }

    private String signed(double v) {
        String sign = v >= 0.0d ? "+" : "";
        return sign + String.format(Locale.US, "%.3f", Double.valueOf(v));
    }

    private String safe(String s) {
        return s == null ? "" : s;
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

    private String safeError(Throwable e) {
        if (e == null) {
            return "";
        }

        String msg = e.getMessage();

        if (msg == null || msg.length() == 0) {
            msg = e.toString();
        }

        if (msg == null) {
            msg = "";
        }

        if (msg.length() > 180) {
            msg = msg.substring(0, 180);
        }

        return msg;
    }

    private static int getInt(Object obj, String field, int fallback) {
        if (obj == null || field == null || field.length() == 0) {
            return fallback;
        }

        try {
            Field f = obj.getClass().getField(field);
            Object v = f.get(obj);

            if (v instanceof Integer) {
                return ((Integer) v).intValue();
            }

            if (v instanceof Long) {
                return (int) ((Long) v).longValue();
            }

            if (v instanceof Number) {
                return ((Number) v).intValue();
            }

            if (v != null) {
                return Integer.parseInt(String.valueOf(v));
            }
        } catch (Throwable e) {
        }

        return fallback;
    }

    private static double getDouble(Object obj, String field, double fallback) {
        if (obj == null || field == null || field.length() == 0) {
            return fallback;
        }

        try {
            Field f = obj.getClass().getField(field);
            Object v = f.get(obj);

            if (v instanceof Double) {
                return ((Double) v).doubleValue();
            }

            if (v instanceof Float) {
                return ((Float) v).doubleValue();
            }

            if (v instanceof Number) {
                return ((Number) v).doubleValue();
            }

            if (v != null) {
                return Double.parseDouble(String.valueOf(v));
            }
        } catch (Throwable e) {
        }

        return fallback;
    }

    private static String getString(Object obj, String field, String fallback) {
        if (obj == null || field == null || field.length() == 0) {
            return fallback;
        }

        try {
            Field f = obj.getClass().getField(field);
            Object v = f.get(obj);

            if (v != null) {
                return String.valueOf(v);
            }
        } catch (Throwable e) {
        }

        return fallback;
    }

    private static void putInt(Object obj, String field, int value) {
        if (obj == null || field == null || field.length() == 0) {
            return;
        }

        try {
            Field f = obj.getClass().getField(field);
            f.setInt(obj, value);
        } catch (Throwable e) {
            try {
                Field f = obj.getClass().getField(field);
                f.set(obj, Integer.valueOf(value));
            } catch (Throwable ignored) {
            }
        }
    }

    private static void putLong(Object obj, String field, long value) {
        if (obj == null || field == null || field.length() == 0) {
            return;
        }

        try {
            Field f = obj.getClass().getField(field);
            f.setLong(obj, value);
        } catch (Throwable e) {
            try {
                Field f = obj.getClass().getField(field);
                f.set(obj, Long.valueOf(value));
            } catch (Throwable ignored) {
            }
        }
    }

    private static void putDouble(Object obj, String field, double value) {
        if (obj == null || field == null || field.length() == 0) {
            return;
        }

        try {
            Field f = obj.getClass().getField(field);
            f.setDouble(obj, value);
        } catch (Throwable e) {
            try {
                Field f = obj.getClass().getField(field);
                f.set(obj, Double.valueOf(value));
            } catch (Throwable ignored) {
            }
        }
    }

    private static void putString(Object obj, String field, String value) {
        if (obj == null || field == null || field.length() == 0) {
            return;
        }

        try {
            Field f = obj.getClass().getField(field);
            f.set(obj, value == null ? "" : value);
        } catch (Throwable e) {
        }
    }

    public static final class TeacherRunReport {
        public int ok;
        public String source;
        public String baseIssue;
        public String targetIssue;
        public int sampleCount;
        public int profileCount;
        public int strongCount;
        public int riskCount;
        public int avgSample;
        public int batchSize;
        public long startedAt;
        public long finishedAt;
        public String summary;
        public String detail;
        public String reason;
        public String message;

        public TeacherRunReport() {
            ok = 1;
            source = "";
            baseIssue = "";
            targetIssue = "";
            sampleCount = 0;
            profileCount = 0;
            strongCount = 0;
            riskCount = 0;
            avgSample = 0;
            batchSize = 0;
            startedAt = 0L;
            finishedAt = 0L;
            summary = "";
            detail = "";
            reason = "";
            message = "";
        }

        public String toString() {
            return summary == null ? "" : summary;
        }
    }

    private static final class TeacherStat {
        int position;
        int number;

        String stateKey;

        int sampleCount;
        int hitCount;
        int missCount;
        int recentSample;
        int recentHit;

        double hitRate;
        double recentHitRate;

        double featureSupport;
        double positionSupport;
        double causeSupport;
        double candidateSupport;
        double learningSupport;
        double returnSupport;
        double transitionSupport;
        double hotColdAdjust;
        double riskPenalty;
        double falseStrongPenalty;

        String windowStateKey;
        String hotColdBucket;
        String omissionBucket;
        String densityBucket;
        String repeatBucket;
        String oddEvenBias;
        String highLowBias;
        String neighborTailState;
        String positionCrowdState;
        String crossPositionSync;
        String crossPositionSqueeze;
        String timingShift;
        int recentLoseStreak;
        double featureDistance;
        int similarWindowSample;
        double similarWindowHitRate;
        double similarWindowRecentRate;
        double sampleTrust;
        double recentEffectiveness;
        double evidenceContribution;
        double counterContribution;
        String evidenceText;
        String counterText;
        String actionAdvice;

        double teacherScore;
        double teacherRisk;
        double confidence;
    }

    private static final class ComboTeacherResult {
        double comboScore;
        double comboRisk;
        double confidence;
        double avgHitRate;
        double k3HitRate;
        double support;
        double penalty;
        double featureDistance;
        int similarWindowSample;
        double similarWindowHitRate;
        double sampleTrust;
        double recentEffectiveness;
        double evidenceContribution;
        double counterContribution;
        String stateKey;
        String evidenceText;
        String counterText;
        String actionAdvice;
        int sample;
        String status;
        String summary;
    }

    private LinkedHashMap<String, Models.NumberTeacherCell> buildTeacherMapFromAccumulator(
            String baseIssue, String targetIssue, LongLayerAccumulator acc) {
        LinkedHashMap<String, Models.NumberTeacherCell> map =
                new LinkedHashMap<String, Models.NumberTeacherCell>();
        if (acc == null) return map;
        for (int p = POSITION_MIN; p <= POSITION_MAX; p++) {
            for (int n = NUMBER_MIN; n <= NUMBER_MAX; n++) {
                TeacherStat stat = new TeacherStat();
                stat.position = p;
                stat.number = n;
                stat.stateKey = "WARM_INCREMENTAL_P" + p;
                stat.sampleCount = acc.sample[p];
                stat.hitCount = acc.hit[p][n];
                stat.missCount = Math.max(0, stat.sampleCount - stat.hitCount);
                stat.recentSample = acc.recentSample[p];
                stat.recentHit = acc.recentHit[p][n];
                stat.hitRate =
                        stat.sampleCount <= 0 ? 0.0d : safeRate(stat.hitCount, stat.sampleCount);
                stat.recentHitRate =
                        stat.recentSample <= 0
                                ? stat.hitRate
                                : safeRate(stat.recentHit, stat.recentSample);
                stat.confidence = sampleConfidence(stat.sampleCount);
                double liftAll = stat.hitRate - BASE_NUMBER_RATE;
                double liftRecent = stat.recentHitRate - BASE_NUMBER_RATE;
                stat.featureSupport = clamp01(0.36d + liftRecent * 2.80d + liftAll * 1.50d);
                stat.positionSupport = stat.confidence;
                stat.learningSupport =
                        clamp01(
                                BASE_NUMBER_RATE
                                        + posPart(liftAll * 4.0d)
                                        + posPart(liftRecent * 3.0d));
                stat.returnSupport = clamp01(posPart(liftRecent * 3.0d));
                stat.transitionSupport =
                        clamp01(posPart((stat.recentHitRate - stat.hitRate) * 4.0d));
                stat.hotColdAdjust = limitSigned(liftRecent * 2.5d + liftAll * 1.2d, 0.35d);
                applyDeterministicTeacherState(stat, null, null);
                stat.riskPenalty =
                        clamp01(
                                0.28d
                                        + negPart(liftAll * 4.0d) * 0.28d
                                        + negPart(liftRecent * 4.0d) * 0.34d);
                stat.falseStrongPenalty = clamp01(posPart(liftRecent * 6.0d) * 0.10d);
                stat.teacherScore =
                        clamp01(
                                0.34d
                                        + stat.confidence * 0.10d
                                        + liftAll * 2.10d
                                        + liftRecent * 1.35d
                                        + posPart(stat.hotColdAdjust) * 0.12d
                                        - stat.riskPenalty * 0.08d);
                stat.teacherRisk =
                        clamp01(
                                0.28d
                                        + negPart(liftAll * 3.2d) * 0.28d
                                        + negPart(liftRecent * 3.6d) * 0.30d
                                        + (1.0d - stat.confidence) * 0.16d);
                String status = statusOf(stat);
                String summary = buildSummary(p, n, stat, status);
                Models.NumberTeacherCell cell = new Models.NumberTeacherCell();
                fillCellFromStat(cell, baseIssue, targetIssue, p, n, stat, status, summary);
                putAllCellKeys(map, targetIssue, p, n, cell);
            }
        }
        return map;
    }

    /** WARM真实微批次：BUILDING写入独立key，完成前不替换当前FRESH快照。 每片只读取有限历史并保存cursor；完成后才原子发布TMAP1正式快照。 */
    private TeacherRunReport runWarmBackgroundSlice(DbStore store, long budgetMs) {
        TeacherRunReport report = new TeacherRunReport();
        report.ok = 1;
        report.source = "warmMicroSlice";
        report.startedAt = System.currentTimeMillis();
        if (store == null) {
            report.ok = 0;
            report.summary = "WARM微批次跳过：DbStore为空";
            return report;
        }
        try {
            Models.Draw latest = store.getLatestDraw();
            if (latest == null || !latest.isValid()) {
                report.ok = 0;
                report.summary = "WARM微批次等待有效开奖";
                return report;
            }
            String baseIssue = safe(latest.issue);
            Models.LayerSnapshot building =
                    store.loadLayerSnapshot(AppConfig.TEACHER_WARM_BUILDING_KEY);
            LongLayerAccumulator acc = null;
            if (building != null
                    && ("BUILDING".equals(building.freshnessStatus)
                            || "BUILDING_PUBLISHED_PARTIAL".equals(building.freshnessStatus))
                    && isLongAccumulatorPayload(building.payload)) {
                acc = decodeLongAccumulator(building.payload);
            }
            if (acc == null || acc.required <= 0 || acc.processed >= acc.required) {
                acc = new LongLayerAccumulator();
                acc.required = AppConfig.TEACHER_WARM_REQUIRED_DRAWS;
                acc.cursorIssue = buildNextIssue(baseIssue);
            }
            int slice =
                    Math.max(
                            1,
                            Math.min(
                                    AppConfig.TEACHER_WARM_MICRO_SLICE_DRAWS,
                                    AppConfig.TEACHER_WARM_SLICE_DRAWS));
            long started = System.currentTimeMillis();
            ArrayList<Models.Draw> clean =
                    normalizeHistory(store.getDrawsBeforeIssue(acc.cursorIssue, slice));
            String oldest = acc.cursorIssue;
            int added = 0;
            for (int i = 0; i < clean.size(); i++) {
                if (System.currentTimeMillis() - started >= Math.max(120L, budgetMs)) break;
                Models.Draw d = clean.get(i);
                if (d == null || !d.isValid()) continue;
                if (d.issue != null
                        && d.issue.length() > 0
                        && (oldest.length() == 0 || d.issue.compareTo(oldest) < 0))
                    oldest = d.issue;
                boolean recent =
                        (acc.processed + added) < AppConfig.TEACHER_RECENT_ACCUMULATOR_DRAWS;
                for (int p = POSITION_MIN; p <= POSITION_MAX; p++) {
                    int n = d.getNumberAtPosition(p);
                    if (!isValidNumber(n)) continue;
                    acc.sample[p]++;
                    acc.hit[p][n]++;
                    if (recent) {
                        acc.recentSample[p]++;
                        acc.recentHit[p][n]++;
                    }
                }
                added++;
            }
            acc.processed += added;
            if (oldest != null && oldest.length() > 0 && added > 0) acc.cursorIssue = oldest;
            /*
             * 完整目标仍为1440期；新数据库达到240期且当前历史已读尽时，发布可审计的
             * FRESH部分WARM矩阵供实时使用，同时保留accumulator和cursor继续累积。
             * 这不是伪造1440样本，每个格子的sampleCount仍写真实已处理数量。
             */
            boolean exhausted = clean.size() < slice || added == 0;
            boolean fullComplete = acc.processed >= acc.required;
            Models.LayerSnapshot publishedWarm =
                    store.loadLayerSnapshot(AppConfig.SNAPSHOT_KEY_TEACHER_WARM);
            int alreadyPublished = publishedWarm == null ? 0 : publishedWarm.sampleCount;
            boolean reachedFirstRealtimeThreshold =
                    alreadyPublished < AppConfig.TEACHER_WARM_MIN_PUBLISH_DRAWS
                            && acc.processed >= AppConfig.TEACHER_WARM_MIN_PUBLISH_DRAWS;
            boolean reachedRefreshStep =
                    acc.processed - alreadyPublished >= AppConfig.TEACHER_WARM_MIN_PUBLISH_DRAWS;
            boolean partialPublish =
                    !fullComplete
                            && acc.processed >= AppConfig.TEACHER_WARM_MIN_PUBLISH_DRAWS
                            && acc.processed > alreadyPublished
                            && (exhausted || reachedFirstRealtimeThreshold || reachedRefreshStep);
            boolean publishNow = fullComplete || partialPublish;

            Models.LayerSnapshot state = new Models.LayerSnapshot();
            state.layer = "WARM_BUILDING";
            state.snapshotKey = AppConfig.TEACHER_WARM_BUILDING_KEY;
            state.asOfIssue = baseIssue;
            state.maxIncludedIssue = baseIssue;
            state.payload = encodeLongAccumulator(acc);
            state.sampleCount = acc.processed;
            state.schemaVersion = String.valueOf(AppConfig.LINK_MODIFICATION_V2_SCHEMA_VERSION);
            state.strategyVersion = LINK_MODIFICATION_V2_TEACHER_VERSION;
            state.freshnessStatus =
                    fullComplete
                            ? "COMPLETE_PENDING_PUBLISH"
                            : (partialPublish
                                    ? "PARTIAL_PENDING_PUBLISH"
                                    : (alreadyPublished >= AppConfig.TEACHER_WARM_MIN_PUBLISH_DRAWS
                                            ? "BUILDING_PUBLISHED_PARTIAL"
                                            : "BUILDING"));
            state.createdAt = System.currentTimeMillis();
            state.expiresAt = state.createdAt;
            store.saveLayerSnapshot(state);

            Models.BackgroundCursor cursor = new Models.BackgroundCursor();
            cursor.taskKey = "TEACHER_WARM_CURSOR";
            cursor.cursorIssue = acc.cursorIssue;
            cursor.targetIssue = buildNextIssue(baseIssue);
            cursor.processedCount = acc.processed;
            cursor.pendingCount = Math.max(0, acc.required - acc.processed);
            cursor.status =
                    fullComplete
                            ? "COMPLETE_PENDING_PUBLISH"
                            : (alreadyPublished >= AppConfig.TEACHER_WARM_MIN_PUBLISH_DRAWS
                                    ? "FRESH_PARTIAL_BUILDING"
                                    : "BUILDING");
            cursor.version = LINK_MODIFICATION_V2_TEACHER_VERSION;
            cursor.updatedAt = state.createdAt;
            store.saveBackgroundCursor(cursor);

            if (publishNow) {
                long publishStarted = System.currentTimeMillis();
                String publishBase = baseIssue;
                String targetIssue = buildNextIssue(publishBase);
                LinkedHashMap<String, Models.NumberTeacherCell> map =
                        buildTeacherMapFromAccumulator(publishBase, targetIssue, acc);
                applyLongLayerEvidence(store, map);
                cacheTeacherMap(targetIssue, map);
                String summary =
                        (fullComplete ? "WARM完整矩阵发布" : "WARM冷启动部分矩阵发布")
                                + "：累计="
                                + acc.processed
                                + "/"
                                + acc.required
                                + "｜矩阵="
                                + map.size()
                                + "｜可实时使用="
                                + (acc.processed >= AppConfig.TEACHER_WARM_MIN_PUBLISH_DRAWS
                                        ? "是"
                                        : "否")
                                + "｜发布耗时="
                                + (System.currentTimeMillis() - publishStarted)
                                + "ms";
                saveTeacherLayerSnapshot(store, "WARM", publishBase, acc.processed, summary, map);
                state.freshnessStatus = fullComplete ? "PUBLISHED" : "BUILDING_PUBLISHED_PARTIAL";
                if (fullComplete) {
                    state.payload = "";
                    state.sampleCount = 0;
                } else {
                    state.payload = encodeLongAccumulator(acc);
                    state.sampleCount = acc.processed;
                }
                state.createdAt = System.currentTimeMillis();
                store.saveLayerSnapshot(state);
                report.sampleCount = acc.processed;
                report.profileCount = map.size();
                report.baseIssue = publishBase;
                report.targetIssue = targetIssue;
                report.summary = summary;
                cursor.status = fullComplete ? "FRESH" : "FRESH_PARTIAL_BUILDING";
                cursor.pendingCount = Math.max(0, acc.required - acc.processed);
                cursor.updatedAt = System.currentTimeMillis();
                store.saveBackgroundCursor(cursor);
            } else {
                report.sampleCount = acc.processed;
                report.baseIssue = baseIssue;
                report.targetIssue = buildNextIssue(baseIssue);
                report.summary =
                        (alreadyPublished >= AppConfig.TEACHER_WARM_MIN_PUBLISH_DRAWS
                                        ? "WARM部分FRESH继续累积"
                                        : "WARM真实微批次BUILDING")
                                + "：本片="
                                + added
                                + "｜累计="
                                + acc.processed
                                + "/"
                                + acc.required
                                + "｜已发布="
                                + alreadyPublished
                                + "｜cursor="
                                + safe(acc.cursorIssue)
                                + (exhausted ? "｜当前历史已读尽，等待后续回补/新期开奖" : "");
            }
            report.finishedAt = System.currentTimeMillis();
            report.detail = report.summary;
            report.reason = report.summary;
            report.message = report.summary;
            String warmSliceStatus =
                    fullComplete
                            ? "FRESH"
                            : ((publishNow
                                            || alreadyPublished
                                                    >= AppConfig.TEACHER_WARM_MIN_PUBLISH_DRAWS)
                                    ? "FRESH_PARTIAL"
                                    : "BUILDING");
            store.saveBackgroundSliceState(
                    "TEACHER_WARM",
                    warmSliceStatus,
                    acc.processed,
                    Math.max(0, acc.required - acc.processed),
                    report.finishedAt - report.startedAt,
                    report.summary);
            store.setState("teacher_warm_last_slice_at", String.valueOf(report.finishedAt));
            store.setState("teacher_warm_last_slice_added", String.valueOf(added));
            store.setState("teacher_warm_last_slice_processed", String.valueOf(acc.processed));
            store.setState("teacher_warm_last_slice_cursor", safe(acc.cursorIssue));
            store.setState("teacher_warm_last_slice_status", warmSliceStatus);
            store.setState("teacher_warm_last_slice_version", LINK_MODIFICATION_V2_TEACHER_VERSION);
            return report;
        } catch (Throwable e) {
            report.ok = 0;
            report.finishedAt = System.currentTimeMillis();
            report.summary = "WARM真实微批次异常：" + safeError(e);
            report.detail = report.summary;
            report.reason = report.summary;
            report.message = report.summary;
            try {
                store.saveBackgroundSliceState(
                        "TEACHER_WARM",
                        "ERROR",
                        0,
                        AppConfig.TEACHER_WARM_REQUIRED_DRAWS,
                        report.finishedAt - report.startedAt,
                        report.summary);
            } catch (Throwable ignore) {
            }
            return report;
        }
    }

    /** AutoFetchService后台证据泵直连入口：每次只跑一片，实时线程不调用。 */
    public TeacherRunReport runBackgroundSlice(DbStore store, long budgetMs) {
        long started = System.currentTimeMillis();
        long effectiveBudget =
                budgetMs <= 0L ? AppConfig.TEACHER_BACKGROUND_SLICE_BUDGET_MS : budgetMs;
        effectiveBudget = Math.min(effectiveBudget, AppConfig.BACKGROUND_SLICE_MAX_MS);
        TeacherRunReport report = runWarmBackgroundSlice(store, effectiveBudget);
        try {
            if (store != null) {
                Models.Draw latest = store.getLatestDraw();
                String baseIssue = latest == null ? "" : safe(latest.issue);
                long elapsed = System.currentTimeMillis() - started;
                /*
                 * 先保证WARM完成。WARM未FRESH时，COLD/DEEP不得争夺同一个手机时间片。
                 * WARM FRESH后才推进COLD；COLD FRESH后才推进DEEP。
                 */
                Models.LayerSnapshot warmFresh =
                        store.loadLayerSnapshot(AppConfig.SNAPSHOT_KEY_TEACHER_WARM);
                boolean warmReady =
                        warmFresh != null
                                && "FRESH".equals(warmFresh.freshnessStatus)
                                && warmFresh.sampleCount
                                        >= AppConfig.TEACHER_WARM_MIN_PUBLISH_DRAWS;
                if (warmReady && elapsed < effectiveBudget * 6L / 10L) {
                    Models.LayerSnapshot coldFresh =
                            store.loadLayerSnapshot(AppConfig.SNAPSHOT_KEY_TEACHER_COLD);
                    boolean coldReady =
                            coldFresh != null && "FRESH".equals(coldFresh.freshnessStatus);
                    if (!coldReady) {
                        boolean coldDue =
                                store.isLayerSnapshotDue(
                                        AppConfig.SNAPSHOT_KEY_TEACHER_COLD,
                                        AppConfig.COLD_REFRESH_MS);
                        advanceBackgroundLayerCursor(
                                store,
                                "COLD",
                                baseIssue,
                                coldDue,
                                AppConfig.TEACHER_COLD_SLICE_DRAWS,
                                AppConfig.TEACHER_COLD_REQUIRED_DRAWS);
                    } else {
                        boolean deepDue =
                                store.isLayerSnapshotDue(
                                        AppConfig.SNAPSHOT_KEY_TEACHER_DEEP,
                                        AppConfig.DEEP_REFRESH_MS);
                        advanceBackgroundLayerCursor(
                                store,
                                "DEEP",
                                baseIssue,
                                deepDue,
                                AppConfig.TEACHER_DEEP_SLICE_DRAWS,
                                AppConfig.TEACHER_DEEP_REQUIRED_DRAWS);
                    }
                } else if (!warmReady) {
                    store.setState("teacher_background_layer_policy", "WARM_FIRST_BUILDING");
                }
                store.setState(
                        "teacher_background_pump_cost_ms",
                        String.valueOf(System.currentTimeMillis() - started));
                store.setState(
                        "teacher_background_pump_budget_ms", String.valueOf(effectiveBudget));
                store.setState(
                        "teacher_background_pump_version", AppConfig.FINAL_INTEGRATION_FIX_VERSION);
                store.setState(
                        "teacher_background_pump_policy",
                        "WARM/COLD/DEEP真实微批次；WARM>=240可部分FRESH实时使用并继续累积；完整目标1440");
            }
        } catch (Throwable e) {
            try {
                if (store != null) store.setState("teacher_background_pump_error", safeError(e));
            } catch (Throwable ignore) {
            }
        }
        return report;
    }

    private void applySettledDomainTeacherEvidence(DbStore store, Models.ComboCandidate c) {
        if (store == null || c == null) return;
        try {
            if (getInt(c, "evidenceDomainFrozen", 0) != 1) {
                putInt(c, "teacherDomainShadowSample", 0);
                putInt(c, "teacherDomainRealSample", 0);
                return;
            }
            String fine =
                    getString(c, "promotionFineDomain", getString(c, "shadowPromotionDomain", ""));
            String mid = getString(c, "promotionMidDomain", "");
            String coarse = getString(c, "promotionCoarseDomain", "");
            String[] keys = new String[] {fine, mid, coarse};
            String[] levels = new String[] {"FINE", "MID", "COARSE"};
            Models.DomainEvidenceStat real = null;
            String matched = "NONE";
            for (int i = 0; i < keys.length; i++)
                if (keys[i] != null && keys[i].length() > 0) {
                    real =
                            store.loadDomainEvidenceStat(
                                    DbHelper.TABLE_TEACHER_DOMAIN_STAT_V20, keys[i]);
                    if (real != null && real.sampleCount > 0) {
                        matched = levels[i];
                        break;
                    }
                }
            Models.ShadowDomainStat shadow = null;
            List<Models.ShadowDomainStat> all = store.getShadowPromotionDomainStats("", 500);
            if (all != null) {
                for (int k = 0; k < keys.length && shadow == null; k++) {
                    String key = keys[k];
                    if (key == null || key.length() == 0) continue;
                    for (int i = 0; i < all.size(); i++) {
                        Models.ShadowDomainStat x = all.get(i);
                        if (x != null && key.equals(x.domainKey)) {
                            shadow = x;
                            if ("NONE".equals(matched)) matched = levels[k];
                            break;
                        }
                    }
                }
            }
            int realN = real == null ? 0 : real.sampleCount,
                    shadowN = shadow == null ? 0 : shadow.sampleCount;
            putInt(c, "teacherDomainRealSample", realN);
            putInt(c, "teacherDomainShadowSample", shadowN);
            putString(
                    c,
                    "teacherMatrixDomainKey",
                    fine.length() > 0 ? fine : (mid.length() > 0 ? mid : coarse));
            putString(c, "teacherFieldSource", "HISTORY+SETTLED_" + matched);
            if (realN <= 0 && shadowN <= 0) return;
            double post = realN > 0 ? real.posteriorMean : shadow.posteriorMean;
            double lower = realN > 0 ? real.lowerBound : shadow.wilsonLowerBound;
            int evidenceN = realN > 0 ? realN : shadowN;
            String st = "WATCH";
            if (realN >= AppConfig.DYNAMIC_HARD_BLOCK_MIN_SAMPLE
                    && AppConfig.isMatureDynamicHardBlock(
                            real.status, realN, post, real.upperBound)) st = "RISK";
            else if (evidenceN >= 30
                    && post >= AppConfig.TEACHER_READY_POSTERIOR
                    && lower >= AppConfig.TEACHER_READY_LOWER_BOUND) st = "READY";
            if (evidenceN >= 60
                    && post >= AppConfig.TEACHER_STRONG_POSTERIOR
                    && lower >= AppConfig.TEACHER_STRONG_LOWER_BOUND) st = "STRONG";
            int history = getInt(c, "teacherNumberHistorySample", 0);
            putInt(c, "teacherMatrixSample", Math.max(history, evidenceN));
            putInt(c, "teacherNumberSample", Math.max(history, evidenceN));
            putDouble(c, "teacherMatrixHitRate", post);
            putDouble(c, "teacherNumberHitRate", post);
            putDouble(c, "teacherMatrixConfidence", clamp01(((double) evidenceN) / 60.0d));
            putDouble(c, "teacherNumberConfidence", clamp01(((double) evidenceN) / 60.0d));
            putString(c, "teacherMatrixStatus", st);
            putString(c, "teacherNumberStatus", st);
            putString(c, "teacherStatus", st);
            putString(c, "teacherMatrixVersion", MODULE_BUILD_VERSION);
            putString(
                    c,
                    "teacherMatrixSummary",
                    "教师分层证据：history="
                            + history
                            + " shadow="
                            + shadowN
                            + " real="
                            + realN
                            + " match="
                            + matched
                            + " posterior="
                            + percent(post)
                            + " lower="
                            + percent(lower)
                            + " status="
                            + st
                            + "；SHADOW只反事实，不直接转BET");
        } catch (Throwable e) {
            putString(c, "teacherApplyError", "TEACHER_DOMAIN_APPLY_ERROR:" + safeError(e));
        }
    }

    /* FIX73487246：Teacher新鲜度必须同时满足状态、时间和期号游标。 */
    private String firstNonEmptyIssue(String a, String b) {
        if (a != null && a.trim().length() > 0) return a.trim();
        return b == null ? "" : b.trim();
    }

    private boolean isTeacherSnapshotFreshByIssue(
            Models.LayerSnapshot snap, String currentIssue, int lag) {
        if (snap == null || snap.payload == null || snap.payload.indexOf("TMAP1|") != 0)
            return false;
        if (!"FRESH".equals(safe(snap.freshnessStatus))) return false;
        long now = System.currentTimeMillis();
        if (snap.expiresAt > 0L && snap.expiresAt < now) return false;
        /* 实时教师最多允许落后180期；超过后只解释，不施加惩罚，并现场按历史重建。 */
        return lag >= 0 && lag <= AppConfig.TEACHER_SNAPSHOT_MAX_ISSUE_LAG;
    }

    private int teacherIssueLag(String currentIssue, String asOfIssue) {
        String cur = safe(currentIssue);
        String old = safe(asOfIssue);
        if (cur.length() < 12 || old.length() < 12) return cur.equals(old) ? 0 : 999999;
        try {
            String curDay = cur.substring(0, 8);
            String oldDay = old.substring(0, 8);
            int curSeq = Integer.parseInt(cur.substring(8));
            int oldSeq = Integer.parseInt(old.substring(8));
            java.text.SimpleDateFormat f =
                    new java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.US);
            f.setLenient(false);
            long dayDiff = (f.parse(curDay).getTime() - f.parse(oldDay).getTime()) / 86400000L;
            long lag = dayDiff * 1440L + (long) curSeq - (long) oldSeq;
            if (lag < 0L) return 999999;
            return lag > 999999L ? 999999 : (int) lag;
        } catch (Throwable e) {
            return cur.equals(old) ? 0 : 999999;
        }
    }

    private void stampTeacherSnapshotMeta73487246(DbStore store, Object c) {
        if (store == null || c == null) return;
        try {
            String asOf = store.getState("teacher_snapshot_as_of_issue", "");
            int lag =
                    parseIntSafe73487246(
                            store.getState("teacher_snapshot_issue_lag", "999999"), 999999);
            int fresh = "1".equals(store.getState("teacher_snapshot_fresh_by_issue", "0")) ? 1 : 0;
            long created =
                    parseLongSafe73487246(store.getState("teacher_snapshot_created_at", "0"), 0L);
            putString(c, "teacherAsOfIssue", asOf);
            putInt(c, "teacherIssueLag", lag);
            putInt(c, "teacherFreshByIssue", fresh);
            putLongReflect73487246(c, "teacherSnapshotCreatedAt", created);
            if (fresh == 0) {
                putString(c, "teacherStatusClass", "STALE_NEUTRAL");
                putString(c, "observeReason", "TEACHER_STALE_BY_ISSUE_EXPLAIN_ONLY");
            }
        } catch (Throwable ignore) {
        }
    }

    private int parseIntSafe73487246(String s, int def) {
        try {
            return Integer.parseInt(s == null ? "" : s.trim());
        } catch (Throwable e) {
            return def;
        }
    }

    private long parseLongSafe73487246(String s, long def) {
        try {
            return Long.parseLong(s == null ? "" : s.trim());
        } catch (Throwable e) {
            return def;
        }
    }

    private void putLongReflect73487246(Object o, String name, long v) {
        try {
            java.lang.reflect.Field f = o.getClass().getField(name);
            f.setLong(o, v);
        } catch (Throwable ignore) {
        }
    }

    private static String appendText(String a, String b) {
        if (a == null || a.length() == 0) return b == null ? "" : b;
        if (b == null || b.length() == 0) return a;
        return a + "｜" + b;
    }

    // V57_B08_EVIDENCE_ONLY_FULL_IMPLEMENTATION

    public static final String V57_B08_OWNER = "EVIDENCE_ONLY:TeacherNumberMatrixEngine";
    public static final boolean V57_B08_CAN_WRITE_FINAL_P = false;
    public static final boolean V57_B08_CAN_WRITE_LANE = false;
    public static final boolean V57_B08_CAN_CREATE_BET = false;
    public static final boolean V57_B08_CAN_SETTLE = false;

    public static final class V57TeacherNumberEvidence {
        public final String targetIssue;
        public final int position;
        public final int number;
        public final double support;
        public final double counter;
        public final double risk;
        public final double confidence;
        public final int sample;
        public final long newestSourceTime;
        public final long decisionCutoffTime;
        public final double freshness;
        public final String lifecycle;
        public final String evidenceHash;

        V57TeacherNumberEvidence(
                String issue,
                int pos,
                int num,
                double s,
                double c,
                double r,
                double conf,
                int n,
                long newest,
                long cutoff,
                double fresh,
                String life) {
            targetIssue = issue == null ? "" : issue;
            position = pos;
            number = num;
            support = v57b08Clamp01(s);
            counter = v57b08Clamp01(c);
            risk = v57b08Clamp01(r);
            confidence = v57b08Clamp01(conf);
            sample = Math.max(0, n);
            newestSourceTime = newest;
            decisionCutoffTime = cutoff;
            freshness = v57b08Clamp01(fresh);
            lifecycle = life == null ? "NO_SAMPLE" : life;
            evidenceHash = v57b08Hash(canonical());
        }

        public boolean pitValid() {
            return newestSourceTime <= decisionCutoffTime;
        }

        public double netEvidence() {
            return v57b08Clamp(support - counter - risk * 0.5d, -1.0d, 1.0d);
        }

        public String canonical() {
            return targetIssue
                    + '|'
                    + position
                    + '|'
                    + number
                    + '|'
                    + v57b08D(support)
                    + '|'
                    + v57b08D(counter)
                    + '|'
                    + v57b08D(risk)
                    + '|'
                    + v57b08D(confidence)
                    + '|'
                    + sample
                    + '|'
                    + newestSourceTime
                    + '|'
                    + decisionCutoffTime
                    + '|'
                    + v57b08D(freshness)
                    + '|'
                    + lifecycle;
        }
    }

    public static V57TeacherNumberEvidence v57b08BuildEvidence(
            String targetIssue,
            int position,
            int number,
            int supportHit,
            int counterHit,
            int sample,
            long newestSourceTime,
            long decisionCutoffTime,
            long nowMs,
            long maxFreshMs) {
        int n = Math.max(0, sample);
        double base = n <= 0 ? 0.0d : (double) Math.max(0, supportHit) / n;
        double ctr = n <= 0 ? 0.0d : (double) Math.max(0, counterHit) / n;
        double reliability = v57b08SampleReliability(n);
        double fresh = v57b08Freshness(newestSourceTime, nowMs, maxFreshMs);
        double driftRisk = Math.max(0.0d, 1.0d - fresh);
        double conf = v57b08Clamp01(reliability * (0.55d + 0.45d * fresh));
        String life = v57b08Lifecycle(n, base, ctr, fresh);
        return new V57TeacherNumberEvidence(
                targetIssue,
                position,
                number,
                base,
                ctr,
                driftRisk,
                conf,
                n,
                newestSourceTime,
                decisionCutoffTime,
                fresh,
                life);
    }

    public static java.util.ArrayList<V57TeacherNumberEvidence> v57b08AuditMatrix(
            String targetIssue,
            int[][] support,
            int[][] counter,
            int[][] sample,
            long newestSourceTime,
            long decisionCutoffTime,
            long nowMs) {
        java.util.ArrayList<V57TeacherNumberEvidence> out =
                new java.util.ArrayList<V57TeacherNumberEvidence>(100);
        for (int p = 0; p < 10; p++)
            for (int n = 0; n < 10; n++) {
                int s =
                        support != null
                                        && p < support.length
                                        && support[p] != null
                                        && n < support[p].length
                                ? support[p][n]
                                : 0;
                int c =
                        counter != null
                                        && p < counter.length
                                        && counter[p] != null
                                        && n < counter[p].length
                                ? counter[p][n]
                                : 0;
                int k =
                        sample != null
                                        && p < sample.length
                                        && sample[p] != null
                                        && n < sample[p].length
                                ? sample[p][n]
                                : 0;
                out.add(
                        v57b08BuildEvidence(
                                targetIssue,
                                p + 1,
                                n + 1,
                                s,
                                c,
                                k,
                                newestSourceTime,
                                decisionCutoffTime,
                                nowMs,
                                30L * 60L * 1000L));
            }
        return out;
    }

    public static boolean v57b08AuthorityInvariant() {
        return MAX_PROB_ADJUST == 0.0d
                && MAX_GATE_ADJUST == 0.0d
                && !V57_B08_CAN_WRITE_FINAL_P
                && !V57_B08_CAN_WRITE_LANE
                && !V57_B08_CAN_CREATE_BET
                && !V57_B08_CAN_SETTLE;
    }

    private static double v57b08SampleReliability(int n) {
        if (n <= 0) return 0.0d;
        return v57b08Clamp01(1.0d - Math.exp(-n / 60.0d));
    }

    private static double v57b08Freshness(long newest, long now, long maxAge) {
        if (newest <= 0 || now < newest) return 0.0d;
        long m = Math.max(1L, maxAge);
        return v57b08Clamp01(1.0d - (double) (now - newest) / m);
    }

    private static String v57b08Lifecycle(int n, double support, double counter, double fresh) {
        if (n < 20) return "NO_SAMPLE";
        if (fresh < 0.15d) return "HARD_FREEZE";
        double net = support - counter;
        if (n >= 120 && net >= 0.08d) return "STRONG";
        if (n >= 60 && net >= 0.03d) return "READY";
        if (net < -0.05d) return "SOFT_RISK";
        return "WATCH";
    }

    private static double v57b08Clamp01(double v) {
        return v57b08Clamp(v, 0.0d, 1.0d);
    }

    private static double v57b08Clamp(double v, double lo, double hi) {
        if (Double.isNaN(v) || Double.isInfinite(v)) return lo;
        return v < lo ? lo : (v > hi ? hi : v);
    }

    private static String v57b08D(double v) {
        return String.format(java.util.Locale.US, "%.8f", v);
    }

    private static String v57b08Hash(String text) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] b = md.digest((text == null ? "" : text).getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder(64);
            for (int i = 0; i < b.length; i++) {
                int v = b[i] & 255;
                if (v < 16) sb.append('0');
                sb.append(Integer.toHexString(v));
            }
            return sb.toString();
        } catch (Throwable t) {
            return "HASH_ERROR";
        }
    }
}
