package com.ethk3.novaonepro;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * FullCandidateScanEngine.java
 *
 * <p>V20 第7/22 重型计算升级主链闭环修复文件。
 *
 * <p>作用： 1. 基于 PreDrawFeatureEngine 的 100 个开奖前号码特征，完整扫描候选池。 2. 固定玩法：单位置 + 固定K=3。 3. 每个位置
 * C(10,3)=120 个K3组合，10个位置共 1200 个候选。 4. 只生成 Models.ComboCandidate，不创建BET、不结算、不最终裁决。 5. 输出候选供后续：
 * CausalFactorMatrixEngine PositionCauseMatrixEngine TeacherNumberMatrixEngine
 * CounterfactualRankEngine ThirdGuardMatrixEngine RuleHealthMatrixEngine ShadowHealthGateEngine
 * FalseStrongFreezeEngine SeedCauseExpansionEngine DynamicCauseChainEngine ProbabilityEngine
 * DynamicGateEngine DecisionGateEngine 继续处理。
 *
 * <p>V20 满血升级内容： 1. 1200全候选扫描，不只扫Top号码。 2. 三号码组合评分： seedScore / featureScore / healthScore /
 * supportScore / riskScore / candidatePrior。 3. 组合结构评分： balanceScore / weakestNumberScore /
 * concentrationRisk / overlapRisk / comboSynergy。 4. 位置质量评分： positionEdge / positionHealth /
 * positionNoise / positionDrift。 5. 风险扣分： falseRecoveryRisk / hotExhaustion / transitionConflict /
 * pairConflict / breakDepth。 6. 排名输出： rank / scanScore / rawP / calibratedP / finalP / dynamicGate
 * / edge。 7. 只做候选池，不绕过最终门。 8. 本轮主链闭环修复：稳定输出全量1200轻扫、Top60精算候选、最终40正式候选、Adaptive输入候选； 9.
 * 实时提速专项：100个号码特征只反射读取一次，1200组合复用内存特征，不逐候选查库。 10. 前40采用Top24+位置覆盖+成熟晋级+反事实稳定+第三号优质组合，不牺牲候选覆盖。 11.
 * 扫描层只写scanProbeCandidate，不写realProbeAllowed/qualifiedProbe，DecisionGate仍是唯一BET出口。 12.
 * 1200候选不生成长文本，仅为进入Top60精算池的候选补齐解释； 每个候选必须携带窗口状态、分类簇、K3覆盖、位置需求、号码供给、第三号质量、反证风险。
 *
 * <p>红线： 1. 不直接BET。 2. 不写真实Trade。 3. 不结算。 4. 不使用目标期开奖。 5. 不多位置投注。 6. 不K4、不动态K。 7. Java7 + AIDE 可编译。
 * 8. 不输出BET/SHADOW/HOLD，只输出候选；最终动作仍由 DecisionGateEngine 统一裁决。
 */
public final class FullCandidateScanEngine {
    public static final String OUMAN_GALAXY_REBUILD_VERSION =
            AppConfig.OUMAN_GALAXY_REBUILD_VERSION;

    private static final int POSITION_MIN = 1;
    private static final int POSITION_MAX = 10;
    private static final int NUMBER_MIN = 1;
    private static final int NUMBER_MAX = 10;
    private static final int K_SIZE = 3;

    private static final double BASE_K3_P = 0.300d;
    private static final double DEFAULT_GATE = 0.385d;
    private static final double EPS = 0.0000001d;

    public static final String VERSION = AppConfig.V46_VERSION;
    public static final String MAIN_CHAIN_FIX_VERSION = AppConfig.V46_VERSION;
    public static final String REALTIME_SPEED_VERSION = AppConfig.V46_VERSION;

    private static final int FULL_SCAN_EXPECTED_CANDIDATES = 1200;
    private static final int GLOBAL_DECISION_CANDIDATE_LIMIT =
            AppConfig.V46_PROBABILITY_CANDIDATE_COUNT;
    private static final int REALTIME_TOP40_LIMIT = 40;
    private static final int ADAPTIVE_INPUT_LIMIT = 40;
    private static final int REALTIME_GLOBAL_CORE_LIMIT = 24;
    private static final int REALTIME_POSITION_COVER_MIN = 1;
    private static final int REALTIME_PROMOTION_RESCUE_LIMIT = 6;
    private static final int REALTIME_COUNTERFACTUAL_RESCUE_LIMIT = 6;
    private static final int REALTIME_THIRD_QUALITY_RESCUE_LIMIT = 8;
    private static final int REALTIME_LONG_TEXT_LIMIT = AppConfig.V46_UI_DISPLAY_CANDIDATE_COUNT;
    private static final long REALTIME_SCAN_WARN_MS = 1500L;

    private static final double COUNTERFACTUAL_STABLE_LINE = 0.580d;
    private static final double COUNTERFACTUAL_PROBE_MAX_RISK = 0.620d;
    private static final double THIRD_RESCUE_COVERAGE_MIN = 0.300d;
    private static final double THIRD_RESCUE_QUALITY_MIN = 0.100d;

    private static final double COVERAGE_READY_LINE = 0.300d;
    private static final double THIRD_READY_LINE = 0.100d;
    private static final double COUNTER_RISK_HARD_LINE = 0.700d;

    private final DbStore store;
    private final PreDrawFeatureEngine featureEngine;

    /*
     * 1200候选会高频读写同一批public字段。缓存Field和缺失字段，避免每个候选反复getField。
     * 只缓存反射元数据，不缓存候选值，不改变任何策略结果。
     */
    private static final HashMap<String, Field> PUBLIC_FIELD_CACHE = new HashMap<String, Field>();
    private static final HashMap<String, Boolean> PUBLIC_FIELD_MISS_CACHE =
            new HashMap<String, Boolean>();
    private static final Object PUBLIC_FIELD_CACHE_LOCK = new Object();

    public FullCandidateScanEngine() {
        this.store = null;
        this.featureEngine = new PreDrawFeatureEngine();
    }

    public FullCandidateScanEngine(DbStore store) {
        this.store = store;
        this.featureEngine = new PreDrawFeatureEngine(store);
    }

    /** AutoFetchService反射正式入口。 完整扫描1200候选，但实时主链只返回经过覆盖保护的120候选；不写候选快照、不生成BET。 */
    public Models.CandidateBatch scan(
            DbStore runtimeStore, List history, List vectors, List traces, List recoveries) {
        return buildRealtimeCandidateBatch(runtimeStore, history, "scan_store_5args");
    }

    /** AutoFetchService无DbStore兼容入口。 */
    public Models.CandidateBatch scan(List history, List vectors, List traces, List recoveries) {
        return buildRealtimeCandidateBatch(this.store, history, "scan_4args");
    }

    /** AutoFetchService旧build(DbStore,List,List)兼容入口。 */
    public Models.CandidateBatch build(DbStore runtimeStore, List history, List vectors) {
        return buildRealtimeCandidateBatch(runtimeStore, history, "build_store_3args");
    }

    /** AutoFetchService旧scan(List,List)兼容入口。 */
    public Models.CandidateBatch scan(List history, List vectors) {
        return buildRealtimeCandidateBatch(this.store, history, "scan_2args");
    }

    private Models.CandidateBatch buildRealtimeCandidateBatch(
            DbStore runtimeStore, List rawHistory, String entry) {
        long totalStart = System.currentTimeMillis();
        ArrayList<Models.Draw> history = normalizeDrawHistory(rawHistory);
        String baseIssue = resolveBaseIssue(history);
        String targetIssue = buildNextIssue(baseIssue);

        Models.CandidateBatch batch = new Models.CandidateBatch();
        batch.baseIssue = baseIssue;
        batch.targetIssue = targetIssue;
        batch.scanMode = REALTIME_SPEED_VERSION;
        batch.probabilityMode = "FULL_SCAN_LIGHT_PRIOR_THEN_PROBABILITY";
        batch.fullScan = 1;

        if (history.size() == 0 || targetIssue.length() == 0) {
            writeRuntimeScanState(runtimeStore, targetIssue, entry, 0, 0, 0L, "NO_HISTORY");
            return batch;
        }

        long featureStart = System.currentTimeMillis();
        Map<String, Models.PreDrawFeature> featureMap =
                buildFeaturesForRuntime(runtimeStore, baseIssue, targetIssue, history);
        int featureStateOk = countFeatureStateComplete(featureMap);
        try {
            if (runtimeStore != null) {
                runtimeStore.setState(
                        "full_scan_predraw_count",
                        String.valueOf(featureMap == null ? 0 : featureMap.size()));
                runtimeStore.setState("full_scan_predraw_state_complete", featureStateOk + "/100");
                runtimeStore.setState(
                        "full_scan_predraw_policy",
                        "DIRECT_FIELD_OR_DETERMINISTIC_NEUTRAL_FALLBACK");
            }
        } catch (Throwable ignore) {
        }
        long featureCost = System.currentTimeMillis() - featureStart;
        saveStageCostSafe(runtimeStore, targetIssue, "full_scan_feature_map", featureCost);

        long scanStart = System.currentTimeMillis();
        ArrayList<Models.ComboCandidate> all = scanWithFeatures(baseIssue, targetIssue, featureMap);
        long scanCost = System.currentTimeMillis() - scanStart;
        saveStageCostSafe(runtimeStore, targetIssue, "full_scan_1200_light", scanCost);

        /* V40: before the 60-candidate probability shortlist is cut, every legal K3 is rescored by
         * the strictly pre-draw ten-class distribution.  This prevents the old descriptive
         * feature score from hiding the actually highest event-probability combinations. */
        long sequenceStart = System.currentTimeMillis();
        rescoreBySequenceDistribution(all, history);
        long sequenceCost = System.currentTimeMillis() - sequenceStart;
        saveStageCostSafe(runtimeStore, targetIssue, "v40_sequence_rescore_1200", sequenceCost);

        long selectStart = System.currentTimeMillis();
        ArrayList<Models.ComboCandidate> selected =
                buildDecisionShortlist(all, GLOBAL_DECISION_CANDIDATE_LIMIT);
        long selectCost = System.currentTimeMillis() - selectStart;
        saveStageCostSafe(runtimeStore, targetIssue, "full_scan_select_60", selectCost);

        batch.scannedCount = all.size();
        batch.keptCount = selected.size();
        batch.enrichCount = Math.min(REALTIME_TOP40_LIMIT, selected.size());
        batch.deepCount = 0;
        batch.addAll(selected);
        if (selected.size() > 0) batch.best = selected.get(0);

        long totalCost = System.currentTimeMillis() - totalStart;
        writeRuntimeScanState(
                runtimeStore,
                targetIssue,
                entry,
                all.size(),
                selected.size(),
                totalCost,
                all.size() == FULL_SCAN_EXPECTED_CANDIDATES ? "OK" : "PARTIAL_FEATURES");
        return batch;
    }

    private Map<String, Models.PreDrawFeature> buildFeaturesForRuntime(
            DbStore runtimeStore, String baseIssue, String targetIssue, List<Models.Draw> history) {
        try {
            if (runtimeStore != null && runtimeStore != this.store) {
                PreDrawFeatureEngine e = new PreDrawFeatureEngine(runtimeStore);
                Map<String, Models.PreDrawFeature> m =
                        e.buildFeatures(baseIssue, targetIssue, history);
                if (m != null) return m;
            }
        } catch (Throwable e) {
        }
        try {
            Map<String, Models.PreDrawFeature> m =
                    featureEngine.buildFeatures(baseIssue, targetIssue, history);
            return m == null ? new LinkedHashMap<String, Models.PreDrawFeature>() : m;
        } catch (Throwable e) {
            return new LinkedHashMap<String, Models.PreDrawFeature>();
        }
    }

    /** V40 historical OOS fast path. */
    public Models.ComboCandidate scanPrimaryLight(
            String baseIssue, String targetIssue, List<Models.Draw> history, int topNumbers) {
        ArrayList<Models.ComboCandidate> top =
                scanTopLight(baseIssue, targetIssue, history, topNumbers, 1);
        return top.size() == 0 ? null : top.get(0);
    }

    /**
     * V40 historical OOS fast path. Produces a deterministic diversified Top-K action surface from
     * information available strictly before targetIssue. It does not read the target draw and does
     * not create a real trade.
     */
    public ArrayList<Models.ComboCandidate> scanTopLight(
            String baseIssue,
            String targetIssue,
            List<Models.Draw> history,
            int topNumbers,
            int maxCandidates) {
        ArrayList<Models.ComboCandidate> all = new ArrayList<Models.ComboCandidate>();
        if (history == null || history.size() < AppConfig.MICRO_WALK_FORWARD_MIN_TRAIN_HISTORY)
            return all;
        Map<String, Models.PreDrawFeature> featureMap =
                featureEngine.buildFeatures(baseIssue, targetIssue, history);
        SequenceDistributionEngine sequenceEngine = new SequenceDistributionEngine();
        SequenceDistributionEngine.Forecast sequenceForecast = sequenceEngine.build(history);
        int top = Math.max(3, Math.min(7, topNumbers));
        int cap =
                Math.max(
                        1,
                        Math.min(
                                AppConfig.MICRO_WALK_FORWARD_CANDIDATES_PER_ORIGIN, maxCandidates));
        long now = System.currentTimeMillis();
        for (int position = POSITION_MIN; position <= POSITION_MAX; position++) {
            PositionScanContext ctx = buildPositionScanContext(featureMap, position);
            NumberScore[] ns = new NumberScore[11];
            int[] order = new int[10];
            double[] score = new double[11];
            for (int n = 1; n <= 10; n++) {
                ns[n] = readNumberScore(getFeature(featureMap, position, n));
                order[n - 1] = n;
                score[n] = numberFastScore(ns[n]);
            }
            for (int a = 0; a < order.length; a++) {
                int bi = a;
                for (int b = a + 1; b < order.length; b++) {
                    int nb = order[b], nbi = order[bi];
                    if (score[nb] > score[nbi] + EPS
                            || (Math.abs(score[nb] - score[nbi]) <= EPS && nb < nbi)) bi = b;
                }
                int tmp = order[a];
                order[a] = order[bi];
                order[bi] = tmp;
            }
            for (int i = 0; i < top - 2; i++) {
                for (int j = i + 1; j < top - 1; j++) {
                    for (int k = j + 1; k < top; k++) {
                        int[] nums = new int[] {order[i], order[j], order[k]};
                        java.util.Arrays.sort(nums);
                        Models.ComboCandidate c =
                                buildCandidateFromScores(
                                        baseIssue,
                                        targetIssue,
                                        now,
                                        position,
                                        nums[0],
                                        nums[1],
                                        nums[2],
                                        ns[nums[0]],
                                        ns[nums[1]],
                                        ns[nums[2]],
                                        ctx);
                        if (c == null || !c.isValidK3()) continue;
                        sequenceEngine.applyToCandidate(sequenceForecast, c);
                        c.rawRankScore =
                                RealtimeCausalProbabilityEngine.computeDeterministicRankScore(c);
                        all.add(c);
                    }
                }
            }
        }
        Collections.sort(
                all,
                new Comparator<Models.ComboCandidate>() {
                    public int compare(Models.ComboCandidate a, Models.ComboCandidate b) {
                        int x = Double.compare(b.rawRankScore, a.rawRankScore);
                        if (x != 0) return x;
                        x = a.position - b.position;
                        if (x != 0) return x;
                        return a.k3Mask - b.k3Mask;
                    }
                });
        ArrayList<Models.ComboCandidate> out = new ArrayList<Models.ComboCandidate>();
        boolean[] usedPosition = new boolean[11];
        for (int pass = 0; pass < 2 && out.size() < cap; pass++) {
            for (int i = 0; i < all.size() && out.size() < cap; i++) {
                Models.ComboCandidate c = all.get(i);
                if (out.contains(c)) continue;
                if (pass == 0 && usedPosition[c.position]) continue;
                out.add(c);
                usedPosition[c.position] = true;
            }
        }
        for (int i = 0; i < out.size(); i++) {
            out.get(i).rank = i + 1;
            out.get(i).oosRankSlot = i + 1;
            out.get(i).oosCandidateCount = out.size();
        }
        return out;
    }

    private double numberFastScore(NumberScore s) {
        if (s == null) return 0.0d;
        double value =
                0.30d * clamp01(s.healthScore)
                        + 0.22d * clamp01(s.supportScore)
                        + 0.18d * clamp01(s.featureScore)
                        + 0.12d * clamp01(s.confidence)
                        + 0.10d * clamp01(s.returnZoneScore)
                        + 0.08d * clamp01(0.5d + s.numberEdge * 4.0d);
        value -= 0.18d * clamp01(s.riskScore) + 0.12d * clamp01(s.falseSeedRisk);
        return clamp01(value);
    }

    public ArrayList<Models.ComboCandidate> scan(
            String baseIssue, String targetIssue, List<Models.Draw> history) {
        Map<String, Models.PreDrawFeature> featureMap =
                featureEngine.buildFeatures(baseIssue, targetIssue, history);
        return scanWithFeatures(baseIssue, targetIssue, featureMap);
    }

    public ArrayList<Models.ComboCandidate> scan(List<Models.Draw> history, String targetIssue) {
        return scan("", targetIssue, history);
    }

    public ArrayList<Models.ComboCandidate> build(
            DbStore ignoredStore, List<Models.Draw> history, String baseIssue, String targetIssue) {
        return scanForDecision(baseIssue, targetIssue, history);
    }

    public ArrayList<Models.ComboCandidate> analyze(
            DbStore ignoredStore, List<Models.Draw> history, String baseIssue, String targetIssue) {
        return scanForDecision(baseIssue, targetIssue, history);
    }

    public ArrayList<Models.ComboCandidate> run(
            DbStore ignoredStore, List<Models.Draw> history, String baseIssue, String targetIssue) {
        return scanForDecision(baseIssue, targetIssue, history);
    }

    public ArrayList<Models.ComboCandidate> scanWithFeatures(
            String baseIssue, String targetIssue, Map<String, Models.PreDrawFeature> featureMap) {
        ArrayList<Models.ComboCandidate> out = new ArrayList<Models.ComboCandidate>();

        if (featureMap == null || featureMap.size() == 0) {
            return out;
        }

        long now = System.currentTimeMillis();
        NumberScore[][] numberCache = new NumberScore[POSITION_MAX + 1][NUMBER_MAX + 1];
        PositionScanContext[] positionCache = new PositionScanContext[POSITION_MAX + 1];

        /* 100个号码特征只反射读取一次；1200组合复用内存NumberScore。 */
        for (int p = POSITION_MIN; p <= POSITION_MAX; p++) {
            positionCache[p] = buildPositionScanContext(featureMap, p);
            for (int n = NUMBER_MIN; n <= NUMBER_MAX; n++) {
                Models.PreDrawFeature f = getFeature(featureMap, p, n);
                if (f != null) numberCache[p][n] = readNumberScore(f);
            }
        }

        for (int p = POSITION_MIN; p <= POSITION_MAX; p++) {
            PositionScanContext ctx = positionCache[p];
            for (int a = NUMBER_MIN; a <= NUMBER_MAX - 2; a++) {
                for (int b = a + 1; b <= NUMBER_MAX - 1; b++) {
                    for (int c = b + 1; c <= NUMBER_MAX; c++) {
                        NumberScore s1 = numberCache[p][a];
                        NumberScore s2 = numberCache[p][b];
                        NumberScore s3 = numberCache[p][c];
                        if (s1 == null || s2 == null || s3 == null) continue;
                        Models.ComboCandidate candidate =
                                buildCandidateFromScores(
                                        baseIssue, targetIssue, now, p, a, b, c, s1, s2, s3, ctx);
                        if (candidate != null) out.add(candidate);
                    }
                }
            }
        }

        sortAndRank(out);
        markFullScanRanksOnly(out);
        /* 全量候选不构造长文本；仅为实时可能进入主链的前120补齐解释。 */
        finalizeCandidateTextForTop(out, REALTIME_LONG_TEXT_LIMIT);
        return out;
    }

    public ArrayList<Models.ComboCandidate> scanFromStore(
            String baseIssue, String targetIssue, int limit) {
        List<Models.Draw> history = loadHistoryFromStore(targetIssue, limit);
        return scan(baseIssue, targetIssue, history);
    }

    public ArrayList<Models.ComboCandidate> scanFromStore(String targetIssue) {
        return scanFromStore("", targetIssue, readWindow500());
    }

    public ArrayList<Models.ComboCandidate> scanTop(
            String baseIssue, String targetIssue, List<Models.Draw> history, int limit) {
        ArrayList<Models.ComboCandidate> all = scan(baseIssue, targetIssue, history);
        return top(all, limit);
    }

    public ArrayList<Models.ComboCandidate> scanTopFromStore(
            String baseIssue, String targetIssue, int historyLimit, int topLimit) {
        ArrayList<Models.ComboCandidate> all = scanFromStore(baseIssue, targetIssue, historyLimit);
        return top(all, topLimit);
    }

    public ArrayList<Models.ComboCandidate> top(ArrayList<Models.ComboCandidate> all, int limit) {
        ArrayList<Models.ComboCandidate> out = new ArrayList<Models.ComboCandidate>();

        if (all == null || all.size() == 0) {
            return out;
        }

        if (limit <= 0 || limit > all.size()) {
            limit = all.size();
        }

        for (int i = 0; i < limit; i++) {
            out.add(all.get(i));
        }

        return out;
    }

    public ArrayList<Models.ComboCandidate> scanForDecision(
            String baseIssue, String targetIssue, List<Models.Draw> history) {
        return buildDecisionShortlist(
                scan(baseIssue, targetIssue, history), GLOBAL_DECISION_CANDIDATE_LIMIT);
    }

    public ArrayList<Models.ComboCandidate> scanForDecisionFromStore(
            String baseIssue, String targetIssue, int historyLimit) {
        return buildDecisionShortlist(
                scanFromStore(baseIssue, targetIssue, historyLimit),
                GLOBAL_DECISION_CANDIDATE_LIMIT);
    }

    public ArrayList<Models.ComboCandidate> scanRealtimeTop40(
            String baseIssue, String targetIssue, List<Models.Draw> history) {
        return buildDecisionShortlist(scan(baseIssue, targetIssue, history), REALTIME_TOP40_LIMIT);
    }

    public ArrayList<Models.ComboCandidate> scanAdaptiveInput(
            String baseIssue, String targetIssue, List<Models.Draw> history) {
        ArrayList<Models.ComboCandidate> list =
                buildDecisionShortlist(scan(baseIssue, targetIssue, history), ADAPTIVE_INPUT_LIMIT);
        markAdaptiveInput(list);
        return list;
    }

    public int scanAndSaveRealtimeTop40(
            String baseIssue, String targetIssue, List<Models.Draw> history) {
        ArrayList<Models.ComboCandidate> list = scanRealtimeTop40(baseIssue, targetIssue, history);
        return saveRealtimeTop40Candidates(baseIssue, targetIssue, list);
    }

    public int scanAndSaveAdaptiveInput(
            String baseIssue, String targetIssue, List<Models.Draw> history) {
        ArrayList<Models.ComboCandidate> list = scanAdaptiveInput(baseIssue, targetIssue, history);
        return saveAdaptiveInputCandidates(baseIssue, targetIssue, list);
    }

    public int saveRealtimeTop40Candidates(
            String baseIssue, String targetIssue, List<Models.ComboCandidate> candidates) {
        return saveCandidateListReflect(
                "saveRealtimeTop40Candidates",
                baseIssue,
                targetIssue,
                candidates,
                REALTIME_TOP40_LIMIT,
                "FULL_SCAN_TOP40");
    }

    public int saveAdaptiveInputCandidates(
            String baseIssue, String targetIssue, List<Models.ComboCandidate> candidates) {
        markAdaptiveInput(candidates);
        return saveCandidateListReflect(
                "saveAdaptiveJudgeInputCandidates",
                baseIssue,
                targetIssue,
                candidates,
                ADAPTIVE_INPUT_LIMIT,
                "FULL_SCAN_ADAPTIVE_INPUT");
    }

    private int saveCandidateListReflect(
            String preferredMethod,
            String baseIssue,
            String targetIssue,
            List<Models.ComboCandidate> candidates,
            int limit,
            String phase) {
        if (store == null
                || targetIssue == null
                || targetIssue.length() == 0
                || candidates == null
                || candidates.size() == 0) {
            return 0;
        }
        if (limit <= 0 || limit > candidates.size()) {
            limit = candidates.size();
        }
        ArrayList<Models.ComboCandidate> list = new ArrayList<Models.ComboCandidate>();
        for (int i = 0; i < limit; i++) {
            Models.ComboCandidate c = candidates.get(i);
            if (c == null) {
                continue;
            }
            putString(c, "snapshotPhase", phase);
            list.add(c);
        }
        if (list.size() == 0) {
            return 0;
        }
        String[] methods =
                new String[] {
                    preferredMethod,
                    "saveRealtimeTop40Candidates",
                    "saveAdaptiveJudgeInputCandidates",
                    "saveMainChainCandidateSnapshot",
                    "saveShadowCandidates"
                };
        for (int i = 0; i < methods.length; i++) {
            String name = methods[i];
            if (name == null || name.length() == 0) {
                continue;
            }
            try {
                Method m = store.getClass().getMethod(name, String.class, String.class, List.class);
                Object r = m.invoke(store, safeString(baseIssue), safeString(targetIssue), list);
                return numberResult(r, list.size());
            } catch (Throwable e) {
            }
            try {
                Method m =
                        store.getClass()
                                .getMethod(name, String.class, String.class, List.class, int.class);
                Object r =
                        m.invoke(
                                store,
                                safeString(baseIssue),
                                safeString(targetIssue),
                                list,
                                Integer.valueOf(list.size()));
                return numberResult(r, list.size());
            } catch (Throwable e) {
            }
        }
        return 0;
    }

    public int saveShadowCandidates(
            String baseIssue,
            String targetIssue,
            List<Models.ComboCandidate> candidates,
            int limit) {
        if (store == null
                || targetIssue == null
                || targetIssue.length() == 0
                || candidates == null) {
            return 0;
        }

        try {
            store.saveShadowCandidates(baseIssue, targetIssue, candidates, limit);
            return Math.min(limit <= 0 ? candidates.size() : limit, candidates.size());
        } catch (Throwable e) {
            return 0;
        }
    }

    public int scanAndSaveShadow(
            String baseIssue, String targetIssue, List<Models.Draw> history, int saveLimit) {
        ArrayList<Models.ComboCandidate> all = scan(baseIssue, targetIssue, history);
        return saveShadowCandidates(baseIssue, targetIssue, all, saveLimit);
    }

    public int scanAndSaveShadowFromStore(
            String baseIssue, String targetIssue, int historyLimit, int saveLimit) {
        ArrayList<Models.ComboCandidate> all = scanFromStore(baseIssue, targetIssue, historyLimit);
        return saveShadowCandidates(baseIssue, targetIssue, all, saveLimit);
    }

    private Models.ComboCandidate buildCandidate(
            String baseIssue,
            String targetIssue,
            long now,
            int position,
            int n1,
            int n2,
            int n3,
            Models.PreDrawFeature f1,
            Models.PreDrawFeature f2,
            Models.PreDrawFeature f3,
            PositionScanContext ctx) {
        return buildCandidateFromScores(
                baseIssue,
                targetIssue,
                now,
                position,
                n1,
                n2,
                n3,
                readNumberScore(f1),
                readNumberScore(f2),
                readNumberScore(f3),
                ctx);
    }

    private Models.ComboCandidate buildCandidateFromScores(
            String baseIssue,
            String targetIssue,
            long now,
            int position,
            int n1,
            int n2,
            int n3,
            NumberScore s1,
            NumberScore s2,
            NumberScore s3,
            PositionScanContext ctx) {
        Models.ComboCandidate c = new Models.ComboCandidate();
        int k3Mask = buildK3MaskSafe(n1, n2, n3);
        if (!isValidK3Mask(k3Mask) || s1 == null || s2 == null || s3 == null) return null;
        ComboScore combo = calcComboScore(s1, s2, s3, ctx);

        double rawP = estimateRawProbability(combo, ctx);
        double calibratedP = conservativeCalibrate(rawP, combo);
        double finalP = calcFinalProbability(rawP, calibratedP, combo, ctx);
        double dynamicGate = readDefaultDynamicGate();
        double edge = finalP - dynamicGate;

        String shapeKey = buildShapeKey(position, n1, n2, n3, combo, ctx);
        String stateKey = buildStateKey(position, n1, n2, n3, combo, ctx);
        String windowStateKey = buildWindowStateKey(position, n1, n2, n3, s1, s2, s3, combo, ctx);
        String clusterKey = buildClusterKey(position, n1, n2, n3, s1, s2, s3, combo, ctx);
        String auditClusterKey = windowStateKey;
        String learningClusterKey = buildLearningClusterKey(position, combo, s1, s2, s3, ctx);
        String factorKey = buildFactorKey(position, n1, n2, n3, combo);
        String sourceKey = REALTIME_SPEED_VERSION;
        String ruleKey = buildRuleKey(combo);
        String sameClassKey = buildSameClassKey(position, combo);
        String summary = "";

        c.baseIssue = safeString(baseIssue);
        c.targetIssue = safeString(targetIssue);
        c.createdAt = now;
        c.position = position;
        c.n1 = n1;
        c.n2 = n2;
        c.n3 = n3;
        c.k3Mask = k3Mask;
        c.shapeKey = shapeKey;
        c.stateMode = combo.stateMode;

        c.modelP = rawP;
        c.shapeP = combo.shapeP;
        c.comboP = combo.comboP;
        c.recoveryP = combo.recoveryP;
        c.similarP = combo.similarP;
        c.memoryP = combo.memoryP;

        c.rawP = rawP;
        c.calibratedP = calibratedP;
        c.finalP = finalP;
        c.dynamicGate = dynamicGate;
        c.edge = edge;

        c.similarHitRate = combo.similarHitRate;
        c.comboHitRate = combo.comboHitRate;
        c.effectiveSample = combo.effectiveSample;
        c.comboSample = combo.comboSample;

        c.trendLife = combo.trendLife;
        c.driftRisk = combo.positionDrift;
        c.breakDepth = combo.breakDepth;
        c.recoveryScore = combo.recoveryQuality;
        c.championGap = combo.championGap;
        c.comboGap = combo.comboGap;
        c.hotReverseRisk = combo.hotExhaustion;
        c.falseStrongRisk = combo.falseSeedRisk;

        c.calibrationError = Math.abs(finalP - rawP);
        c.probabilityMode = "FULL_SCAN_LIGHT_PRIOR";
        c.backtestMode = "PREDRAW_ONLY";

        c.deepScore = combo.scanScore;
        c.deepConfidence = combo.confidence;
        c.deepPositionScore = combo.positionHealth;
        c.deepNumberScore = combo.numberSupport;
        c.deepCycleScore = combo.gapSupport;
        c.deepTransitionScore = combo.transitionSupport;
        c.deepStateScore = combo.healthScore;

        c.biasScore = combo.numberBias;
        c.biasConfidence = combo.confidence;
        c.profileScore = combo.profileScore;
        c.fallbackProfileScore = combo.fallbackProfileScore;
        c.auditPenalty = combo.auditPenalty;
        c.auditBoost = combo.auditBoost;

        c.promotionScore = combo.promotionScore;
        c.aPlusScore = combo.aPlusScore;
        c.evidenceScore = combo.supportScore;
        c.evidenceRisk = combo.riskScore;

        c.aiJudgeScore = combo.scanScore;
        c.aiJudgeConfidence = combo.confidence;
        c.weakestNumberScore = combo.weakestNumberScore;
        c.balanceScore = combo.balanceScore;

        c.deepProfileKey = stateKey;
        c.deepProfileHitRate = combo.comboHitRate;
        c.deepProfileSample = combo.effectiveSample;
        c.deepFallbackLevel = combo.riskScore >= 0.70d ? 2 : (combo.riskScore >= 0.50d ? 1 : 0);
        c.fallbackProfileKey = sameClassKey;
        c.candidateEvidenceKey = buildCandidateEvidenceKey(targetIssue, position, k3Mask, combo);
        boolean scanPromotionCandidate = combo.healthScore >= 0.68d && combo.riskScore <= 0.45d;
        c.promotionStatus = "WATCH_SCAN_ONLY";
        putInt(c, "scanPromotionCandidate", scanPromotionCandidate ? 1 : 0);
        c.aPlusStatus =
                combo.aPlusScore >= 0.72d && combo.riskScore <= 0.42d ? "A_PLUS_WATCH" : "NORMAL";
        c.deepReason = "";

        c.stateKey = stateKey;
        c.fixCohort = AppConfig.FIX_COHORT;
        c.probabilityPipelineVersion = AppConfig.PROBABILITY_PIPELINE_VERSION;
        c.auditClusterKey = auditClusterKey;
        c.learningClusterKey = learningClusterKey;
        c.runtimeProfileScore = combo.profileScore;
        c.stateNumberProfileScore = combo.numberSupport;
        c.numberBias = combo.numberBias;
        c.comboBias = combo.comboBias;
        c.teacherWeight = 1.0d;
        c.stateConfidence = combo.confidence;
        c.stateSample = combo.effectiveSample;
        c.cacheSample = combo.comboSample;
        c.recentSample = combo.recentSample;
        c.recentHitRate = combo.recentHitRate;

        fillMainChainCandidateFields(
                c,
                sourceKey,
                stateKey,
                windowStateKey,
                clusterKey,
                factorKey,
                ruleKey,
                sameClassKey,
                summary,
                combo,
                ctx,
                rawP,
                calibratedP,
                finalP,
                dynamicGate,
                edge,
                s1,
                s2,
                s3);

        putString(c, "sourceKey", sourceKey);
        putString(c, "ruleKey", ruleKey);
        putString(c, "sameClassKey", sameClassKey);
        putString(c, "scanSummary", summary);
        putString(c, "summaryText", summary);
        putString(c, "reasonText", summary);
        putString(c, "featureSummary", summary);

        putDouble(c, "scanScore", combo.scanScore);
        putDouble(c, "candidatePrior", combo.candidatePrior);
        putDouble(c, "seedScore", combo.seedScore);
        putDouble(c, "falseSeedRisk", combo.falseSeedRisk);
        putDouble(c, "supportScore", combo.supportScore);
        putDouble(c, "riskScore", combo.riskScore);
        putDouble(c, "healthScore", combo.healthScore);
        putDouble(c, "positionHealth", combo.positionHealth);
        putDouble(c, "positionNoiseScore", combo.positionNoise);
        putDouble(c, "positionEdge", combo.positionEdge);
        putDouble(c, "numberEdge", combo.numberEdge);
        putDouble(c, "comboSynergy", combo.comboSynergy);
        putDouble(c, "concentrationRisk", combo.concentrationRisk);
        putDouble(c, "overlapRisk", combo.overlapRisk);
        putDouble(c, "falseRecoveryRisk", combo.falseRecoveryRisk);
        putDouble(c, "transitionConflict", combo.transitionConflict);
        putDouble(c, "pairConflict", combo.pairConflict);

        putInt(c, "fullScanCandidate", 1);
        putInt(c, "noFutureLeakOk", 1);
        putInt(c, "fixedK", 3);
        putInt(
                c,
                "preDrawStateComplete",
                (s1.stateKey.length() > 0 && s2.stateKey.length() > 0 && s3.stateKey.length() > 0)
                        ? 1
                        : 0);
        putString(c, "preDrawFieldVersion", VERSION);

        return c;
    }

    private void fillMainChainCandidateFields(
            Models.ComboCandidate c,
            String sourceKey,
            String stateKey,
            String windowStateKey,
            String clusterKey,
            String factorKey,
            String ruleKey,
            String sameClassKey,
            String summary,
            ComboScore combo,
            PositionScanContext ctx,
            double rawP,
            double calibratedP,
            double finalP,
            double dynamicGate,
            double edge,
            NumberScore s1,
            NumberScore s2,
            NumberScore s3) {
        if (c == null || combo == null) {
            return;
        }
        double coverageQuality = calcCoverageQuality(combo);
        double thirdQuality = calcThirdQuality(s1, s2, s3, combo);
        double positionNeedScore = calcPositionNeedScore(combo, ctx);
        double numberSupplyScore = calcNumberSupplyScore(combo);
        double counterEvidenceScore = calcCounterEvidenceScore(combo);
        double sampleReliability = calcSampleReliability(combo.effectiveSample, combo.confidence);
        double recentValidity =
                calcRecentValidity(combo.recentSample, combo.recentHitRate, combo.falseSeedRisk);
        double netEvidence =
                clampSigned(
                        numberSupplyScore * 0.36d
                                + positionNeedScore * 0.22d
                                + coverageQuality * 0.20d
                                + thirdQuality * 0.12d
                                - counterEvidenceScore * 0.30d);
        String v20Status =
                buildV20Status(combo, coverageQuality, thirdQuality, counterEvidenceScore, edge);
        String scanRejectCode =
                buildCandidateRejectCode(
                        combo, coverageQuality, thirdQuality, counterEvidenceScore, edge);
        int scanProbeCandidate =
                isProbeCandidate(
                                combo,
                                coverageQuality,
                                thirdQuality,
                                counterEvidenceScore,
                                finalP,
                                dynamicGate,
                                edge)
                        ? 1
                        : 0;
        /* 扫描层只能标注预资格，绝不能写realProbeAllowed/qualifiedProbe。 */
        int realProbeAllowed = 0;
        String actionAdvice =
                scanProbeCandidate == 1
                        ? "SCAN_PREQUALIFIED_ONLY_DECISION_GATE_REQUIRED"
                        : (scanRejectCode.length() == 0
                                ? "WATCH_FOR_DECISION_GATE"
                                : "SCAN_WATCH=" + scanRejectCode);
        String rejectCode = isHardStructuralScanReject(scanRejectCode) ? scanRejectCode : "";

        putString(c, "version", VERSION);
        putString(c, "scanVersion", VERSION);
        putString(c, "fieldChainVersion", MAIN_CHAIN_FIX_VERSION);
        putString(c, "sourceKey", sourceKey);
        putString(c, "windowStateKey", windowStateKey);
        putString(c, "dynamicWindowStateKey", windowStateKey);
        putString(c, "finalDecisionWindowStateKey", windowStateKey);
        putString(c, "probabilityWindowStateKey", windowStateKey);
        putString(c, "clusterKey", clusterKey);
        putString(c, "auditClusterKey", windowStateKey);
        putString(
                c,
                "learningClusterKey",
                buildLearningClusterKey(c.position, combo, s1, s2, s3, ctx));
        putString(c, "fixCohort", AppConfig.FIX_COHORT);
        putString(c, "probabilityPipelineVersion", AppConfig.PROBABILITY_PIPELINE_VERSION);
        putString(c, "factorKey", factorKey);
        putString(c, "ruleKey", ruleKey);
        putString(c, "sameClassKey", sameClassKey);
        putString(c, "stateKey", stateKey);
        putString(c, "v20Status", v20Status);
        putString(c, "v20ActionAdvice", actionAdvice);
        putString(c, "teacherStatus", "NEUTRAL_NO_SAMPLE_FULL_SCAN");
        putString(c, "teacherStatusClass", "NEUTRAL");
        putString(c, "teacherNumberStatus", "NEUTRAL_NO_SAMPLE_FULL_SCAN");
        putString(c, "adaptiveStatus", "PENDING_ADAPTIVE_INPUT");
        putString(c, "shadowPromotionStatus", "WATCH_SCAN_ONLY");
        putString(c, "scanRejectCode", scanRejectCode);
        putString(c, "scanHardRejectCode", rejectCode);
        putString(c, "rejectCode", rejectCode);
        putString(c, "finalDecisionRejectCode", "");
        putString(c, "displayRejectCode", scanRejectCode);
        putString(c, "realProbeReason", actionAdvice);
        putString(c, "mainChainSummary", summary);
        putString(c, "factorSummary", "");

        putDouble(c, "positionNeedScore", positionNeedScore);
        putDouble(c, "positionNeedStrength", positionNeedScore);
        putDouble(c, "numberSupplyScore", numberSupplyScore);
        putDouble(c, "numberSupplyStrength", numberSupplyScore);
        putDouble(c, "coverageQuality", coverageQuality);
        putDouble(c, "coverageQualityScore", coverageQuality);
        putDouble(c, "finalDecisionCoverageQuality", coverageQuality);
        putDouble(c, "coverageRequired", AppConfig.COVERAGE_REQUIRED_DEFAULT);
        putDouble(c, "coverageConfidence", sampleReliability);
        putDouble(
                c,
                "coverageContribution",
                clampSigned((coverageQuality - AppConfig.COVERAGE_REQUIRED_DEFAULT) * 0.20d));
        putDouble(c, "needSupplyMatchScore", coverageQuality);
        putDouble(c, "thirdQuality", thirdQuality);
        putDouble(c, "thirdNumberQualityScore", thirdQuality);
        putDouble(c, "needSupplyThirdQuality", thirdQuality);
        putDouble(c, "finalDecisionThirdNumberQuality", thirdQuality);
        putDouble(c, "counterEvidenceScore", counterEvidenceScore);
        putDouble(c, "antiEvidenceScore", counterEvidenceScore);
        putDouble(c, "evidenceScore", numberSupplyScore);
        putDouble(c, "netEvidenceScore", netEvidence);
        putDouble(c, "similarSampleCount", combo.effectiveSample);
        putDouble(c, "similarHitRate", combo.similarHitRate);
        putDouble(c, "sampleReliability", sampleReliability);
        putDouble(c, "recentValidity", recentValidity);
        putDouble(c, "v20SampleReliability", sampleReliability);
        putDouble(c, "v20RecentValidity", recentValidity);
        putDouble(c, "v20EvidenceContribution", numberSupplyScore);
        putDouble(c, "v20CounterContribution", counterEvidenceScore);
        putDouble(c, "teacherSampleReliability", 0.0d);
        putDouble(c, "teacherRecentValidity", 0.0d);
        putDouble(c, "teacherEvidenceContribution", 0.0d);
        putDouble(c, "teacherCounterContribution", 0.0d);
        putDouble(c, "finalPBeforeDiagnosis", rawP);
        putDouble(c, "finalPAfterDiagnosis", finalP);
        putDouble(c, "gate", dynamicGate);
        putDouble(c, "dynamicGate", dynamicGate);
        putDouble(c, "edge", edge);
        putInt(c, "scanProbeCandidate", scanProbeCandidate);
        putInt(c, "realProbeAllowed", 0);
        putInt(c, "qualifiedProbe", 0);
        putInt(c, "adaptiveInputFlag", 0);
        putInt(c, "realtimeTop40Rank", 0);
        putInt(c, "candidateRank", getInt(c, "rank", 0));
    }

    private double calcCoverageQuality(ComboScore combo) {
        if (combo == null) {
            return 0.0d;
        }
        return clamp01(
                combo.numberSupport * 0.28d
                        + combo.balanceScore * 0.20d
                        + combo.comboSynergy * 0.18d
                        + combo.weakestNumberScore * 0.18d
                        + (1.0d - combo.concentrationRisk) * 0.08d
                        + (1.0d - combo.overlapRisk) * 0.08d);
    }

    private double calcThirdQuality(NumberScore a, NumberScore b, NumberScore c, ComboScore combo) {
        NumberScore weakest = a;
        if (weakest == null || (b != null && b.healthScore < weakest.healthScore)) {
            weakest = b;
        }
        if (weakest == null || (c != null && c.healthScore < weakest.healthScore)) {
            weakest = c;
        }
        if (weakest == null) {
            return 0.0d;
        }
        double base =
                weakest.healthScore * 0.34d
                        + weakest.supportScore * 0.24d
                        + weakest.returnZoneScore * 0.16d
                        + weakest.transitionLift * 0.12d
                        + (1.0d - weakest.falseSeedRisk) * 0.14d;
        if (combo != null) {
            base = base * 0.72d + combo.weakestNumberScore * 0.28d;
        }
        return clamp01(base);
    }

    private double calcPositionNeedScore(ComboScore combo, PositionScanContext ctx) {
        if (combo == null) {
            return 0.0d;
        }
        double p =
                combo.positionHealth * 0.36d
                        + (1.0d - combo.positionNoise) * 0.24d
                        + combo.gapSupport * 0.18d
                        + combo.recoveryQuality * 0.12d
                        + Math.max(0.0d, combo.positionEdge) * 0.80d;
        if (ctx != null) {
            p += (1.0d - ctx.positionDrift) * 0.10d;
        }
        return clamp01(p);
    }

    private double calcNumberSupplyScore(ComboScore combo) {
        if (combo == null) {
            return 0.0d;
        }
        return clamp01(
                combo.numberSupport * 0.34d
                        + combo.seedScore * 0.18d
                        + combo.featureScore * 0.14d
                        + combo.transitionSupport * 0.10d
                        + combo.pairSupport * 0.08d
                        + combo.recoveryQuality * 0.10d
                        + combo.balanceScore * 0.06d);
    }

    private double calcCounterEvidenceScore(ComboScore combo) {
        if (combo == null) {
            return 1.0d;
        }
        return clamp01(
                combo.falseSeedRisk * 0.30d
                        + combo.riskScore * 0.22d
                        + combo.transitionConflict * 0.12d
                        + combo.pairConflict * 0.10d
                        + combo.concentrationRisk * 0.08d
                        + combo.overlapRisk * 0.08d
                        + combo.positionNoise * 0.10d);
    }

    private double calcSampleReliability(int sample, double confidence) {
        double s = sample <= 0 ? 0.0d : Math.min(1.0d, ((double) sample) / 360.0d);
        return clamp01(s * 0.62d + confidence * 0.38d);
    }

    private double calcRecentValidity(int recentSample, double recentHitRate, double risk) {
        double s = recentSample <= 0 ? 0.0d : Math.min(1.0d, ((double) recentSample) / 96.0d);
        double h = clamp01((recentHitRate - BASE_K3_P + 0.18d) / 0.36d);
        return clamp01(s * 0.35d + h * 0.45d + (1.0d - risk) * 0.20d);
    }

    private boolean isProbeCandidate(
            ComboScore combo,
            double coverageQuality,
            double thirdQuality,
            double counterEvidenceScore,
            double finalP,
            double gate,
            double edge) {
        if (combo == null) {
            return false;
        }
        if (edge < 0.0d) {
            return false;
        }
        if (finalP < 0.345d) {
            return false;
        }
        if (coverageQuality < COVERAGE_READY_LINE) {
            return false;
        }
        if (thirdQuality < THIRD_READY_LINE) {
            return false;
        }
        if (counterEvidenceScore >= COUNTER_RISK_HARD_LINE) {
            return false;
        }
        if (combo.falseSeedRisk >= 0.72d || combo.positionNoise >= 0.76d) {
            return false;
        }
        return true;
    }

    private String buildV20Status(
            ComboScore combo,
            double coverageQuality,
            double thirdQuality,
            double counterEvidenceScore,
            double edge) {
        if (combo == null) {
            return "V20_EMPTY";
        }
        if (counterEvidenceScore >= COUNTER_RISK_HARD_LINE
                || combo.falseSeedRisk >= 0.72d
                || combo.positionNoise >= 0.76d) {
            return "V20_HARD_RISK";
        }
        if (edge >= 0.0d
                && coverageQuality >= 0.42d
                && thirdQuality >= 0.22d
                && combo.scanScore >= 0.56d) {
            return "V20_FULL_SCAN_READY";
        }
        if (coverageQuality >= COVERAGE_READY_LINE && thirdQuality >= THIRD_READY_LINE) {
            return "V20_FULL_SCAN_WATCH";
        }
        return "V20_FULL_SCAN_WEAK";
    }

    private String buildCandidateRejectCode(
            ComboScore combo,
            double coverageQuality,
            double thirdQuality,
            double counterEvidenceScore,
            double edge) {
        if (combo == null) {
            return "FULL_SCAN_EMPTY";
        }
        if (counterEvidenceScore >= COUNTER_RISK_HARD_LINE) {
            return "COUNTER_EVIDENCE_HARD";
        }
        if (combo.falseSeedRisk >= 0.72d) {
            return "FALSE_SEED_HARD";
        }
        if (combo.positionNoise >= 0.76d) {
            return "POSITION_NOISE_HARD";
        }
        if (edge < 0.0d) {
            return "EDGE_NEGATIVE";
        }
        if (coverageQuality < COVERAGE_READY_LINE) {
            return "COVER_LOW";
        }
        if (thirdQuality < THIRD_READY_LINE) {
            return "THIRD_WEAK";
        }
        return "";
    }

    private boolean isHardStructuralScanReject(String code) {
        String s = upper(code);
        return "COUNTER_EVIDENCE_HARD".equals(s)
                || "FALSE_SEED_HARD".equals(s)
                || "POSITION_NOISE_HARD".equals(s)
                || "FULL_SCAN_EMPTY".equals(s);
    }

    private String buildFactorSummary(
            double positionNeedScore,
            double numberSupplyScore,
            double coverageQuality,
            double thirdQuality,
            double counterEvidenceScore,
            double sampleReliability,
            double recentValidity) {
        return "位置需求="
                + percent(positionNeedScore)
                + "｜号码供给="
                + percent(numberSupplyScore)
                + "｜K3覆盖="
                + percent(coverageQuality)
                + "｜第三号="
                + percent(thirdQuality)
                + "｜反证="
                + percent(counterEvidenceScore)
                + "｜样本可信="
                + percent(sampleReliability)
                + "｜近期有效="
                + percent(recentValidity);
    }

    private String buildWindowStateKey(
            int position,
            int n1,
            int n2,
            int n3,
            NumberScore a,
            NumberScore b,
            NumberScore c,
            ComboScore combo,
            PositionScanContext ctx) {
        StringBuilder sb = new StringBuilder();
        sb.append("FULL_W12_24_48_96_360");
        sb.append("|P").append(position);
        sb.append("|K3_").append(n1).append("_").append(n2).append("_").append(n3);
        sb.append("|A=").append(nonEmptyNumberState(a));
        sb.append("|B=").append(nonEmptyNumberState(b));
        sb.append("|C=").append(nonEmptyNumberState(c));
        if (combo != null) {
            sb.append("|CV=").append(bucket(calcCoverageQuality(combo)));
            sb.append("|CE=").append(bucket(calcCounterEvidenceScore(combo)));
        }
        if (ctx != null) {
            sb.append("|PN=").append(bucket(ctx.positionNoise));
        }
        return sb.toString();
    }

    private String buildClusterKey(
            int position,
            int n1,
            int n2,
            int n3,
            NumberScore a,
            NumberScore b,
            NumberScore c,
            ComboScore combo,
            PositionScanContext ctx) {
        StringBuilder sb = new StringBuilder();
        sb.append("P").append(position).append("|FULL_CLUSTER");
        if (combo != null) {
            sb.append("|SCAN_").append(bucket(combo.scanScore));
            sb.append("|COVER_").append(bucket(calcCoverageQuality(combo)));
            sb.append("|THIRD_").append(bucket(calcThirdQuality(a, b, c, combo)));
            sb.append("|RISK_").append(bucket(calcCounterEvidenceScore(combo)));
        }
        return sb.toString();
    }

    private String buildLearningClusterKey(
            int position,
            ComboScore combo,
            NumberScore a,
            NumberScore b,
            NumberScore c,
            PositionScanContext ctx) {
        StringBuilder sb = new StringBuilder();
        sb.append("LCL|P").append(position);
        if (combo != null) {
            sb.append("|HC=").append(coarseHotCold(a, b, c));
            sb.append("|TREND=").append(coarseTrend(a, b, c));
            sb.append("|OM=").append(coarseOmission(a, b, c));
            sb.append("|DEN=").append(bucket(combo.concentrationRisk));
            sb.append("|NEED=").append(bucket(calcPositionNeedScore(combo, ctx)));
            sb.append("|SUPPLY=").append(bucket(calcNumberSupplyScore(combo)));
            sb.append("|THIRD=").append(bucket(calcThirdQuality(a, b, c, combo)));
            sb.append("|COUNTER=").append(bucket(calcCounterEvidenceScore(combo)));
            sb.append("|PRIOR=").append(probabilityBucket(combo.candidatePrior));
        }
        return sb.toString();
    }

    private String coarseHotCold(NumberScore a, NumberScore b, NumberScore c) {
        double v =
                ((a == null
                                        ? 0.5d
                                        : clamp01(
                                                0.5d
                                                        + a.hotPressure * 0.25d
                                                        - a.hotExhaustion * 0.20d
                                                        + a.coldRepairPower * 0.20d))
                                + (b == null
                                        ? 0.5d
                                        : clamp01(
                                                0.5d
                                                        + b.hotPressure * 0.25d
                                                        - b.hotExhaustion * 0.20d
                                                        + b.coldRepairPower * 0.20d))
                                + (c == null
                                        ? 0.5d
                                        : clamp01(
                                                0.5d
                                                        + c.hotPressure * 0.25d
                                                        - c.hotExhaustion * 0.20d
                                                        + c.coldRepairPower * 0.20d)))
                        / 3.0d;
        return bucket(v);
    }

    private String coarseTrend(NumberScore a, NumberScore b, NumberScore c) {
        double v =
                ((a == null
                                        ? 0.5d
                                        : clamp01(
                                                0.5d
                                                        + a.trendConsistency * 0.30d
                                                        + a.freqSlope * 0.15d))
                                + (b == null
                                        ? 0.5d
                                        : clamp01(
                                                0.5d
                                                        + b.trendConsistency * 0.30d
                                                        + b.freqSlope * 0.15d))
                                + (c == null
                                        ? 0.5d
                                        : clamp01(
                                                0.5d
                                                        + c.trendConsistency * 0.30d
                                                        + c.freqSlope * 0.15d)))
                        / 3.0d;
        return bucket(v);
    }

    private String coarseOmission(NumberScore a, NumberScore b, NumberScore c) {
        double v =
                ((a == null
                                        ? 0.5d
                                        : clamp01(
                                                0.5d
                                                        + a.gapPressure * 0.22d
                                                        + a.returnZoneScore * 0.18d
                                                        - a.falseRecoveryRisk * 0.15d))
                                + (b == null
                                        ? 0.5d
                                        : clamp01(
                                                0.5d
                                                        + b.gapPressure * 0.22d
                                                        + b.returnZoneScore * 0.18d
                                                        - b.falseRecoveryRisk * 0.15d))
                                + (c == null
                                        ? 0.5d
                                        : clamp01(
                                                0.5d
                                                        + c.gapPressure * 0.22d
                                                        + c.returnZoneScore * 0.18d
                                                        - c.falseRecoveryRisk * 0.15d)))
                        / 3.0d;
        return bucket(v);
    }

    private String probabilityBucket(double p) {
        if (p >= 0.38d) return "P38";
        if (p >= 0.36d) return "P36_38";
        if (p >= 0.34d) return "P34_36";
        if (p >= 0.31d) return "P31_34";
        return "P_LT31";
    }

    private String buildFactorKey(int position, int n1, int n2, int n3, ComboScore combo) {
        StringBuilder sb = new StringBuilder();
        sb.append("P")
                .append(position)
                .append("|K3_")
                .append(n1)
                .append("_")
                .append(n2)
                .append("_")
                .append(n3)
                .append("|FACTOR");
        if (combo != null) {
            sb.append("|NEED_").append(bucket(calcPositionNeedScore(combo, null)));
            sb.append("|SUPPLY_").append(bucket(calcNumberSupplyScore(combo)));
            sb.append("|BAL_").append(bucket(combo.balanceScore));
        }
        return sb.toString();
    }

    private NumberScore readNumberScore(Models.PreDrawFeature f) {
        NumberScore s = new NumberScore();

        if (f == null) {
            return s;
        }

        s.position = getInt(f, "position", 0);
        s.number = getInt(f, "number", 0);
        s.freq16 = getDouble(f, "freq12", getDouble(f, "freq16", 0.0d));
        s.freq32 = getDouble(f, "freq24", getDouble(f, "freq32", 0.0d));
        s.freq64 = getDouble(f, "freq48", getDouble(f, "freq64", 0.0d));
        s.freq120 = getDouble(f, "freq96", getDouble(f, "freq120", 0.0d));
        s.freq240 = getDouble(f, "freq240", 0.0d);
        s.freq500 = getDouble(f, "freq360", getDouble(f, "freq500", 0.0d));

        s.freqSlope = getDouble(f, "freqSlope", 0.0d);
        s.freqAccel = getDouble(f, "freqAccel", 0.0d);
        s.shortVsMid = getDouble(f, "shortVsMid", 0.0d);
        s.midVsLong = getDouble(f, "midVsLong", 0.0d);
        s.trendConsistency = getDouble(f, "trendConsistency", 0.0d);

        s.positionEntropy = getDouble(f, "positionEntropy", 1.0d);
        s.positionTop3Mass = getDouble(f, "positionTop3Mass", 0.0d);
        s.positionStrengthSlope = getDouble(f, "positionStrengthSlope", 0.0d);
        s.positionDrift = getDouble(f, "positionDrift", 1.0d);
        s.positionNoiseScore = getDouble(f, "positionNoiseScore", 1.0d);
        s.positionHealth = getDouble(f, "positionHealth", 0.0d);
        s.positionEdge = getDouble(f, "positionEdge", 0.0d);

        s.currentGap = getInt(f, "currentGap", 0);
        s.avgGap = getDouble(f, "avgGap", 10.0d);
        s.gapZ = getDouble(f, "gapZ", 0.0d);
        s.gapPhase = getDouble(f, "gapPhase", 0.0d);
        s.gapPressure = getDouble(f, "gapPressure", 0.0d);

        s.hotPressure = getDouble(f, "hotPressure", 0.0d);
        s.hotExhaustion = getDouble(f, "hotExhaustion", 0.0d);
        s.coldRepairPower = getDouble(f, "coldRepairPower", 0.0d);
        s.returnZoneScore = getDouble(f, "returnZoneScore", 0.0d);

        s.transitionLift = getDouble(f, "transitionLift", 0.0d);
        s.transitionConflict = getDouble(f, "transitionConflict", 0.0d);
        s.pairCoSupport = getDouble(f, "pairCoSupport", 0.0d);
        s.pairConflict = getDouble(f, "pairConflict", 0.0d);

        s.breakDepth = getDouble(f, "breakDepth", 0.0d);
        s.recoverySpeed = getDouble(f, "recoverySpeed", 0.0d);
        s.recoveryQuality = getDouble(f, "recoveryQuality", 0.0d);
        s.falseRecoveryRisk = getDouble(f, "falseRecoveryRisk", 0.0d);

        s.seedScore = getDouble(f, "seedScore", getDouble(f, "featureScore", 0.0d));
        s.falseSeedRisk = getDouble(f, "falseSeedRisk", s.falseRecoveryRisk);
        s.numberEdge = getDouble(f, "numberEdge", 0.0d);
        s.candidatePrior = getDouble(f, "candidatePrior", BASE_K3_P / 3.0d);
        s.supportScore = getDouble(f, "supportScore", s.seedScore);
        s.riskScore = getDouble(f, "riskScore", s.falseSeedRisk);
        s.healthScore = getDouble(f, "healthScore", 0.0d);
        s.featureScore = getDouble(f, "featureScore", 0.0d);
        s.confidence = getDouble(f, "confidence", 0.0d);

        s.stateKey = safeString(f.stateKey);
        s.windowStateKey = safeString(f.windowStateKey);
        if (s.stateKey.length() == 0) s.stateKey = buildFallbackNumberStateKey(s);
        if (s.windowStateKey.length() == 0) s.windowStateKey = s.stateKey;
        s.clusterKey = safeString(f.clusterKey);
        if (s.clusterKey.length() == 0) s.clusterKey = "PREDRAW_CLUSTER_P" + s.position;
        s.factorKey = safeString(f.factorKey);
        if (s.factorKey.length() == 0)
            s.factorKey = "PREDRAW_FACTOR_P" + s.position + "_N" + s.number;
        s.sameClassKey = safeString(f.sameClassKey);
        if (s.sameClassKey.length() == 0) s.sameClassKey = s.clusterKey;
        s.evidenceScore = getDouble(f, "evidenceScore", s.supportScore);
        s.antiEvidenceScore =
                getDouble(
                        f, "antiEvidenceScore", getDouble(f, "counterEvidenceScore", s.riskScore));
        s.sampleReliability = getDouble(f, "sampleReliability", s.confidence);
        s.recentValidity = getDouble(f, "recentValidity", 0.0d);
        s.trendBucket = getString(f, "trendBucket", "");
        s.gapBucket = getString(f, "gapBucket", "");
        s.riskBucket = getString(f, "riskBucket", "");
        s.transitionBucket = getString(f, "transitionBucket", "");
        s.healthBucket = getString(f, "healthBucket", "");

        return s;
    }

    private ComboScore calcComboScore(
            NumberScore a, NumberScore b, NumberScore c, PositionScanContext ctx) {
        ComboScore out = new ComboScore();

        NumberScore[] arr = new NumberScore[] {a, b, c};

        double seedSum = 0.0d;
        double featureSum = 0.0d;
        double supportSum = 0.0d;
        double riskSum = 0.0d;
        double healthSum = 0.0d;
        double confidenceSum = 0.0d;
        double recoverySum = 0.0d;
        double transitionLiftSum = 0.0d;
        double transitionConflictSum = 0.0d;
        double pairSupportSum = 0.0d;
        double pairConflictSum = 0.0d;
        double gapSupportSum = 0.0d;
        double breakSum = 0.0d;
        double hotExhaustionSum = 0.0d;
        double falseRecoverySum = 0.0d;
        double positionEdgeSum = 0.0d;
        double numberEdgeSum = 0.0d;

        double weakest = 1.0d;
        double strongest = 0.0d;

        for (int i = 0; i < arr.length; i++) {
            NumberScore s = arr[i];

            if (s == null) {
                continue;
            }

            seedSum += s.seedScore;
            featureSum += s.featureScore;
            supportSum += s.supportScore;
            riskSum += s.riskScore;
            healthSum += s.healthScore;
            confidenceSum += s.confidence;
            recoverySum += s.recoveryQuality;
            transitionLiftSum += s.transitionLift;
            transitionConflictSum += s.transitionConflict;
            pairSupportSum += s.pairCoSupport;
            pairConflictSum += s.pairConflict;
            gapSupportSum += s.returnZoneScore;
            breakSum += s.breakDepth;
            hotExhaustionSum += s.hotExhaustion;
            falseRecoverySum += s.falseRecoveryRisk;
            positionEdgeSum += s.positionEdge;
            numberEdgeSum += s.numberEdge;

            if (s.healthScore < weakest) {
                weakest = s.healthScore;
            }

            if (s.healthScore > strongest) {
                strongest = s.healthScore;
            }
        }

        out.seedScore = clamp01(seedSum / 3.0d);
        out.featureScore = clamp01(featureSum / 3.0d);
        out.supportScore = clamp01(supportSum / 3.0d);
        out.riskScore = clamp01(riskSum / 3.0d);
        out.healthScore = clamp01(healthSum / 3.0d);
        out.confidence = clamp01(confidenceSum / 3.0d);
        out.recoveryQuality = clamp01(recoverySum / 3.0d);
        out.transitionSupport = clamp01(transitionLiftSum / 3.0d);
        out.transitionConflict = clamp01(transitionConflictSum / 3.0d);
        out.pairSupport = clamp01(pairSupportSum / 3.0d);
        out.pairConflict = clamp01(pairConflictSum / 3.0d);
        out.gapSupport = clamp01(gapSupportSum / 3.0d);
        out.breakDepth = clamp01(breakSum / 3.0d);
        out.hotExhaustion = clamp01(hotExhaustionSum / 3.0d);
        out.falseRecoveryRisk = clamp01(falseRecoverySum / 3.0d);
        out.positionEdge = limitSigned(positionEdgeSum / 3.0d, 0.080d);
        out.numberEdge = limitSigned(numberEdgeSum / 3.0d, 0.090d);

        out.weakestNumberScore = clamp01(weakest);
        out.strongestNumberScore = clamp01(strongest);
        out.balanceScore = calcBalanceScore(a, b, c);
        out.concentrationRisk = calcConcentrationRisk(a, b, c);
        out.overlapRisk = calcOverlapRisk(a, b, c);
        out.comboSynergy = calcComboSynergy(a, b, c, out);

        out.positionHealth = ctx == null ? out.healthScore : ctx.positionHealth;
        out.positionNoise = ctx == null ? out.riskScore : ctx.positionNoise;
        out.positionDrift = ctx == null ? out.riskScore : ctx.positionDrift;

        out.falseSeedRisk =
                clamp01(
                        out.riskScore * 0.34d
                                + out.falseRecoveryRisk * 0.18d
                                + out.hotExhaustion * 0.14d
                                + out.transitionConflict * 0.12d
                                + out.pairConflict * 0.08d
                                + out.concentrationRisk * 0.08d
                                + out.positionNoise * 0.06d);

        out.numberSupport =
                clamp01(
                        out.seedScore * 0.26d
                                + out.featureScore * 0.18d
                                + out.supportScore * 0.20d
                                + out.recoveryQuality * 0.12d
                                + out.transitionSupport * 0.10d
                                + out.pairSupport * 0.06d
                                + out.balanceScore * 0.08d);

        out.comboBias = limitSigned((out.comboSynergy - out.falseSeedRisk) * 0.26d, 0.18d);
        out.numberBias = limitSigned((out.numberSupport - out.riskScore) * 0.24d, 0.18d);

        out.scanScore =
                clamp01(
                        out.numberSupport * 0.33d
                                + out.healthScore * 0.18d
                                + out.positionHealth * 0.12d
                                + out.comboSynergy * 0.12d
                                + out.balanceScore * 0.08d
                                + out.weakestNumberScore * 0.07d
                                + (1.0d - out.falseSeedRisk) * 0.10d);

        out.shapeP = clamp01(BASE_K3_P + (out.featureScore - 0.50d) * 0.18d);
        out.comboP = clamp01(BASE_K3_P + out.comboBias);
        out.recoveryP = clamp01(BASE_K3_P + (out.recoveryQuality - out.falseRecoveryRisk) * 0.16d);
        out.similarP =
                clamp01(
                        BASE_K3_P
                                + (out.transitionSupport
                                                + out.pairSupport
                                                - out.transitionConflict
                                                - out.pairConflict)
                                        * 0.10d);
        out.memoryP = clamp01(BASE_K3_P + (out.positionHealth - out.positionNoise) * 0.08d);

        out.candidatePrior =
                clamp01(BASE_K3_P + out.positionEdge + out.numberEdge + out.comboBias * 0.24d);

        out.auditPenalty = calcAuditPenalty(out);
        out.auditBoost = calcAuditBoost(out);
        out.profileScore =
                clamp01(
                        out.scanScore * 0.70d
                                + out.confidence * 0.20d
                                + out.positionHealth * 0.10d);
        out.fallbackProfileScore =
                clamp01(
                        out.healthScore * 0.45d
                                + out.balanceScore * 0.25d
                                + out.weakestNumberScore * 0.30d);
        out.promotionScore =
                clamp01(
                        out.scanScore * 0.46d
                                + out.healthScore * 0.22d
                                + out.confidence * 0.16d
                                + (1.0d - out.falseSeedRisk) * 0.16d);
        out.aPlusScore =
                clamp01(
                        out.promotionScore * 0.52d
                                + out.weakestNumberScore * 0.20d
                                + out.balanceScore * 0.14d
                                + out.positionHealth * 0.14d);

        out.similarHitRate = out.similarP;
        out.comboHitRate = out.comboP;
        out.effectiveSample = calcEffectiveSample(a, b, c);
        out.comboSample = out.effectiveSample;
        out.recentSample = Math.min(out.effectiveSample, readWindow120());
        out.recentHitRate =
                clamp01(out.shapeP * 0.34d + out.comboP * 0.33d + out.recoveryP * 0.33d);
        out.trendLife = calcTrendLife(a, b, c);
        out.championGap = out.strongestNumberScore - out.weakestNumberScore;
        out.comboGap = Math.abs(out.comboP - out.shapeP);

        if (out.falseSeedRisk >= 0.70d || out.positionNoise >= 0.72d) {
            out.stateMode = "FULL_SCAN_RISK";
        } else if (out.aPlusScore >= 0.72d && out.weakestNumberScore >= 0.50d) {
            out.stateMode = "FULL_SCAN_A_PLUS_WATCH";
        } else if (out.healthScore >= 0.62d && out.scanScore >= 0.58d) {
            out.stateMode = "FULL_SCAN_HEALTHY";
        } else {
            out.stateMode = "FULL_SCAN_NORMAL";
        }

        return out;
    }

    private double estimateRawProbability(ComboScore combo, PositionScanContext ctx) {
        if (combo == null) {
            return BASE_K3_P;
        }

        double p = BASE_K3_P;

        p += (combo.scanScore - 0.50d) * 0.120d;
        p += (combo.healthScore - 0.50d) * 0.060d;
        p += (combo.positionHealth - 0.50d) * 0.040d;
        p += combo.comboBias * 0.160d;
        p += combo.numberBias * 0.140d;
        p += combo.positionEdge * 0.500d;
        p += combo.numberEdge * 0.500d;
        p += combo.auditBoost;
        p -= combo.auditPenalty;
        p -= combo.falseSeedRisk * 0.045d;

        if (ctx != null && ctx.positionNoise > 0.68d) {
            p -= 0.010d;
        }

        return clampProbability(p);
    }

    private double conservativeCalibrate(double rawP, ComboScore combo) {
        double p = clampProbability(rawP);

        if (combo == null) {
            return p;
        }

        if (combo.confidence < 0.28d) {
            p -= 0.010d;
        } else if (combo.confidence < 0.45d) {
            p -= 0.005d;
        }

        if (combo.weakestNumberScore < 0.28d) {
            p -= 0.008d;
        }

        if (combo.falseSeedRisk > 0.62d) {
            p -= 0.010d;
        }

        if (combo.balanceScore > 0.70d && combo.falseSeedRisk < 0.48d) {
            p += 0.004d;
        }

        return clampProbability(p);
    }

    private double calcFinalProbability(
            double rawP, double calibratedP, ComboScore combo, PositionScanContext ctx) {
        double p = rawP * 0.40d + calibratedP * 0.38d;

        if (combo != null) {
            p += combo.shapeP * 0.06d;
            p += combo.comboP * 0.06d;
            p += combo.recoveryP * 0.05d;
            p += combo.similarP * 0.03d;
            p += combo.memoryP * 0.02d;

            if (combo.falseSeedRisk > 0.68d) {
                p -= 0.008d;
            }

            if (combo.healthScore > 0.70d && combo.weakestNumberScore > 0.48d) {
                p += 0.004d;
            }
        }

        if (ctx != null && ctx.positionHealth < 0.35d) {
            p -= 0.006d;
        }

        return clampProbability(p);
    }

    private double calcBalanceScore(NumberScore a, NumberScore b, NumberScore c) {
        double x1 = a == null ? 0.0d : a.healthScore;
        double x2 = b == null ? 0.0d : b.healthScore;
        double x3 = c == null ? 0.0d : c.healthScore;

        double avg = (x1 + x2 + x3) / 3.0d;
        double var =
                ((x1 - avg) * (x1 - avg) + (x2 - avg) * (x2 - avg) + (x3 - avg) * (x3 - avg))
                        / 3.0d;
        double std = Math.sqrt(var);

        return clamp01(1.0d - std * 2.5d);
    }

    private double calcConcentrationRisk(NumberScore a, NumberScore b, NumberScore c) {
        double h1 = a == null ? 0.0d : a.hotPressure;
        double h2 = b == null ? 0.0d : b.hotPressure;
        double h3 = c == null ? 0.0d : c.hotPressure;

        double max = Math.max(h1, Math.max(h2, h3));
        double avg = (h1 + h2 + h3) / 3.0d;

        double risk = 0.0d;

        if (max > 0.70d) {
            risk += 0.22d;
        }

        if (avg > 0.55d) {
            risk += 0.22d;
        }

        risk += Math.max(0.0d, max - avg) * 0.35d;

        return clamp01(risk);
    }

    private double calcOverlapRisk(NumberScore a, NumberScore b, NumberScore c) {
        double r = 0.0d;

        if (a != null && b != null && safeString(a.gapBucket).equals(safeString(b.gapBucket))) {
            r += 0.08d;
        }

        if (a != null && c != null && safeString(a.gapBucket).equals(safeString(c.gapBucket))) {
            r += 0.08d;
        }

        if (b != null && c != null && safeString(b.gapBucket).equals(safeString(c.gapBucket))) {
            r += 0.08d;
        }

        if (a != null
                && b != null
                && safeString(a.riskBucket).equals("RISK_HIGH")
                && safeString(b.riskBucket).equals("RISK_HIGH")) {
            r += 0.18d;
        }

        if (a != null
                && c != null
                && safeString(a.riskBucket).equals("RISK_HIGH")
                && safeString(c.riskBucket).equals("RISK_HIGH")) {
            r += 0.18d;
        }

        if (b != null
                && c != null
                && safeString(b.riskBucket).equals("RISK_HIGH")
                && safeString(c.riskBucket).equals("RISK_HIGH")) {
            r += 0.18d;
        }

        return clamp01(r);
    }

    private double calcComboSynergy(NumberScore a, NumberScore b, NumberScore c, ComboScore base) {
        double synergy = 0.0d;

        if (base != null) {
            synergy += base.transitionSupport * 0.20d;
            synergy += base.pairSupport * 0.16d;
            synergy += base.recoveryQuality * 0.16d;
            synergy += base.balanceScore * 0.18d;
            synergy += base.weakestNumberScore * 0.14d;
            synergy += (1.0d - base.concentrationRisk) * 0.08d;
            synergy += (1.0d - base.overlapRisk) * 0.08d;
        }

        return clamp01(synergy);
    }

    private double calcAuditPenalty(ComboScore combo) {
        if (combo == null) {
            return 0.0d;
        }

        double p = 0.0d;

        if (combo.falseSeedRisk > 0.55d) {
            p += (combo.falseSeedRisk - 0.55d) * 0.040d;
        }

        if (combo.positionNoise > 0.60d) {
            p += (combo.positionNoise - 0.60d) * 0.030d;
        }

        if (combo.weakestNumberScore < 0.30d) {
            p += (0.30d - combo.weakestNumberScore) * 0.030d;
        }

        if (combo.concentrationRisk > 0.55d) {
            p += (combo.concentrationRisk - 0.55d) * 0.025d;
        }

        return limitSigned(p, 0.030d);
    }

    private double calcAuditBoost(ComboScore combo) {
        if (combo == null) {
            return 0.0d;
        }

        double b = 0.0d;

        if (combo.healthScore > 0.64d) {
            b += (combo.healthScore - 0.64d) * 0.020d;
        }

        if (combo.balanceScore > 0.72d) {
            b += (combo.balanceScore - 0.72d) * 0.012d;
        }

        if (combo.confidence > 0.60d) {
            b += (combo.confidence - 0.60d) * 0.012d;
        }

        return limitSigned(b, 0.018d);
    }

    private int calcEffectiveSample(NumberScore a, NumberScore b, NumberScore c) {
        int s = 0;

        s += sampleFromNumber(a);
        s += sampleFromNumber(b);
        s += sampleFromNumber(c);

        if (s <= 0) {
            return 0;
        }

        return s / 3;
    }

    private int sampleFromNumber(NumberScore s) {
        if (s == null) {
            return 0;
        }

        int sample = 0;

        if (s.freq120 > 0.0d) {
            sample += readWindow120();
        }

        if (s.freq240 > 0.0d) {
            sample += readWindow240();
        }

        if (s.freq500 > 0.0d) {
            sample += Math.min(readWindow500(), 500);
        }

        if (sample <= 0) {
            sample = readWindow120();
        }

        return sample;
    }

    private double calcTrendLife(NumberScore a, NumberScore b, NumberScore c) {
        double x = 0.0d;

        x += a == null ? 0.0d : Math.max(0.0d, a.freqSlope);
        x += b == null ? 0.0d : Math.max(0.0d, b.freqSlope);
        x += c == null ? 0.0d : Math.max(0.0d, c.freqSlope);

        return clamp01(x / 3.0d);
    }

    private String buildFallbackNumberStateKey(NumberScore s) {
        if (s == null) return "NS_NEUTRAL";
        StringBuilder sb = new StringBuilder();
        sb.append("P").append(s.position).append("_N").append(s.number);
        sb.append("_F").append(bucket(clamp01((s.freq32 + s.freq64 + s.freq120) / 3.0d)));
        sb.append("_G").append(bucket(clamp01(s.returnZoneScore)));
        sb.append("_H")
                .append(
                        bucket(
                                clamp01(
                                        s.healthScore > 0.0d
                                                ? s.healthScore
                                                : AppConfig.PREDRAW_NEUTRAL_SCORE)));
        sb.append("_R")
                .append(
                        bucket(
                                clamp01(
                                        s.riskScore > 0.0d
                                                ? s.riskScore
                                                : AppConfig.PREDRAW_NEUTRAL_SCORE)));
        return sb.toString();
    }

    private PositionScanContext buildPositionScanContext(
            Map<String, Models.PreDrawFeature> featureMap, int position) {
        PositionScanContext ctx = new PositionScanContext();
        ctx.position = position;

        double health = 0.0d;
        double noise = 0.0d;
        double drift = 0.0d;
        double edge = 0.0d;
        double entropy = 0.0d;
        double topMass = 0.0d;
        int count = 0;

        for (int n = NUMBER_MIN; n <= NUMBER_MAX; n++) {
            Models.PreDrawFeature f = getFeature(featureMap, position, n);

            if (f == null) {
                continue;
            }

            health += getDouble(f, "positionHealth", getDouble(f, "healthScore", 0.0d));
            noise += getDouble(f, "positionNoiseScore", 1.0d);
            drift += getDouble(f, "positionDrift", 1.0d);
            edge += getDouble(f, "positionEdge", 0.0d);
            entropy += getDouble(f, "positionEntropy", 1.0d);
            topMass += getDouble(f, "positionTop3Mass", 0.0d);
            count++;
        }

        if (count <= 0) {
            count = 1;
        }

        ctx.positionHealth = clamp01(health / count);
        ctx.positionNoise = clamp01(noise / count);
        ctx.positionDrift = clamp01(drift / count);
        ctx.positionEdge = limitSigned(edge / count, 0.080d);
        ctx.positionEntropy = clamp01(entropy / count);
        ctx.positionTop3Mass = clamp01(topMass / count);

        return ctx;
    }

    private Models.PreDrawFeature getFeature(
            Map<String, Models.PreDrawFeature> featureMap, int position, int number) {
        if (featureMap == null) {
            return null;
        }

        return featureMap.get(String.valueOf(position) + "_" + String.valueOf(number));
    }

    private void sortAndRank(ArrayList<Models.ComboCandidate> list) {
        if (list == null || list.size() == 0) {
            return;
        }

        Collections.sort(
                list,
                new Comparator<Models.ComboCandidate>() {
                    public int compare(Models.ComboCandidate a, Models.ComboCandidate b) {
                        double sa = candidateSortScore(a);
                        double sb = candidateSortScore(b);

                        if (sa > sb) {
                            return -1;
                        }

                        if (sa < sb) {
                            return 1;
                        }

                        double ea = a == null ? 0.0d : a.edge;
                        double eb = b == null ? 0.0d : b.edge;

                        if (ea > eb) {
                            return -1;
                        }

                        if (ea < eb) {
                            return 1;
                        }

                        int pa = a == null ? 0 : a.position;
                        int pb = b == null ? 0 : b.position;

                        if (pa != pb) {
                            return pa - pb;
                        }

                        int ma = a == null ? 0 : a.k3Mask;
                        int mb = b == null ? 0 : b.k3Mask;

                        return ma - mb;
                    }
                });

        for (int i = 0; i < list.size(); i++) {
            Models.ComboCandidate c = list.get(i);

            if (c == null) {
                continue;
            }

            c.rank = i + 1;
            putInt(c, "rankNo", i + 1);
            putInt(c, "fullScanRank", i + 1);

            double rankScore = 1.0d - (((double) i) / Math.max(1.0d, (double) list.size()));
            putDouble(c, "rankScore", clamp01(rankScore));
        }
    }

    private static double candidateSortScore(Models.ComboCandidate c) {
        if (c == null) {
            return 0.0d;
        }

        double scan = getDouble(c, "scanScore", c.deepScore);
        double health = getDouble(c, "healthScore", c.deepStateScore);
        double risk = getDouble(c, "riskScore", c.evidenceRisk);
        double weak = c.weakestNumberScore;
        double balance = c.balanceScore;

        return scan * 0.44d
                + c.finalP * 0.22d
                + health * 0.14d
                + weak * 0.08d
                + balance * 0.06d
                - risk * 0.06d;
    }

    private List<Models.Draw> loadHistoryFromStore(String targetIssue, int limit) {
        ArrayList<Models.Draw> out = new ArrayList<Models.Draw>();

        if (store == null) {
            return out;
        }

        if (limit <= 0) {
            limit = readWindow500();
        }

        try {
            if (targetIssue != null && targetIssue.length() > 0) {
                List<Models.Draw> list = store.getDrawsBeforeIssue(targetIssue, limit);

                if (list != null) {
                    out.addAll(list);
                }
            } else {
                List<Models.Draw> list = store.getRecentDraws(limit);

                if (list != null) {
                    out.addAll(list);
                }
            }
        } catch (Throwable e) {
        }

        return out;
    }

    private void markFullScanRanksOnly(ArrayList<Models.ComboCandidate> list) {
        if (list == null || list.size() == 0) return;
        for (int i = 0; i < list.size(); i++) {
            Models.ComboCandidate c = list.get(i);
            if (c == null) continue;
            int rank = i + 1;
            putInt(c, "candidateRank", rank);
            putInt(c, "rank", rank);
            putInt(c, "rankNo", rank);
            putInt(c, "fullScanRank", rank);
            putInt(c, "realtimeTop40Rank", 0);
            putInt(c, "realtime_top40_rank", 0);
            putInt(c, "top40Candidate", 0);
            putInt(c, "adaptiveInputFlag", 0);
            putInt(c, "adaptive_input_flag", 0);
            putInt(c, "decisionCandidate", 0);
            putString(c, "realtimeSelectionReason", "NOT_SELECTED_YET");
        }
    }

    private void rescoreBySequenceDistribution(
            ArrayList<Models.ComboCandidate> all, List<Models.Draw> history) {
        if (all == null || all.size() == 0) return;
        SequenceDistributionEngine sequenceEngine = new SequenceDistributionEngine();
        SequenceDistributionEngine.Forecast forecast = sequenceEngine.build(history);
        for (int i = 0; i < all.size(); i++) {
            Models.ComboCandidate c = all.get(i);
            if (c == null || !c.isValidK3()) continue;
            sequenceEngine.applyToCandidate(forecast, c);
            c.rawRankScore = RealtimeCausalProbabilityEngine.computeDeterministicRankScore(c);
        }
        Collections.sort(
                all,
                new Comparator<Models.ComboCandidate>() {
                    public int compare(Models.ComboCandidate a, Models.ComboCandidate b) {
                        int x = Double.compare(b.rawRankScore, a.rawRankScore);
                        if (x != 0) return x;
                        x = Double.compare(b.modelP, a.modelP);
                        if (x != 0) return x;
                        x = a.position - b.position;
                        if (x != 0) return x;
                        return a.k3Mask - b.k3Mask;
                    }
                });
    }

    private ArrayList<Models.ComboCandidate> buildDecisionShortlist(
            ArrayList<Models.ComboCandidate> all, int limit) {
        ArrayList<Models.ComboCandidate> out = new ArrayList<Models.ComboCandidate>();
        if (all == null || all.size() == 0 || limit <= 0) return out;
        if (limit > all.size()) limit = all.size();
        LinkedHashMap<String, Models.ComboCandidate> selected =
                new LinkedHashMap<String, Models.ComboCandidate>();

        int coreLimit = Math.min(REALTIME_TOP40_LIMIT, limit);
        addFirstCandidates(
                selected,
                all,
                Math.min(REALTIME_GLOBAL_CORE_LIMIT, coreLimit),
                "GLOBAL_TOP24",
                coreLimit);

        /* 每个位置至少保留一个候选，防止全局分数把某个位置完全挤出实时池。 */
        for (int p = POSITION_MIN; p <= POSITION_MAX && selected.size() < coreLimit; p++) {
            if (containsPosition(selected, p)) continue;
            int added = 0;
            for (int i = 0; i < all.size() && added < REALTIME_POSITION_COVER_MIN; i++) {
                Models.ComboCandidate c = all.get(i);
                if (c != null
                        && c.position == p
                        && addSelected(selected, c, "POSITION_COVER_P" + p, coreLimit)) added++;
            }
        }

        addByDomain(
                selected, all, "PROMOTION_MATURE", REALTIME_PROMOTION_RESCUE_LIMIT, coreLimit, 1);
        addByDomain(
                selected,
                all,
                "COUNTERFACTUAL_STABLE",
                REALTIME_COUNTERFACTUAL_RESCUE_LIMIT,
                coreLimit,
                2);
        addByDomain(
                selected, all, "THIRD_QUALITY", REALTIME_THIRD_QUALITY_RESCUE_LIMIT, coreLimit, 3);
        addFirstCandidates(selected, all, coreLimit, "GLOBAL_FILL_TOP40", coreLimit);

        /* 轻量多样性覆盖后，按全局排序补齐到Top60完整精算池。 */
        addFirstCandidates(selected, all, limit, "GLOBAL_AUDIT_FILL", limit);
        for (Models.ComboCandidate c : selected.values()) {
            if (c != null) out.add(c);
            if (out.size() >= limit) break;
        }
        markRealtimeCandidateDomains(out);
        finalizeCandidateTextForList(out);
        return out;
    }

    private void addFirstCandidates(
            LinkedHashMap<String, Models.ComboCandidate> selected,
            ArrayList<Models.ComboCandidate> all,
            int targetSize,
            String reason,
            int hardLimit) {
        if (selected == null || all == null) return;
        for (int i = 0;
                i < all.size() && selected.size() < targetSize && selected.size() < hardLimit;
                i++) {
            addSelected(selected, all.get(i), reason, hardLimit);
        }
    }

    private void addByDomain(
            LinkedHashMap<String, Models.ComboCandidate> selected,
            ArrayList<Models.ComboCandidate> all,
            String reason,
            int maxAdd,
            int hardLimit,
            int domainType) {
        if (selected == null || all == null || maxAdd <= 0) return;
        int added = 0;
        for (int i = 0; i < all.size() && added < maxAdd && selected.size() < hardLimit; i++) {
            Models.ComboCandidate c = all.get(i);
            if (c == null) continue;
            boolean ok =
                    domainType == 1
                            ? isMaturePromotionCandidate(c)
                            : (domainType == 2
                                    ? isCounterfactualStableCandidate(c)
                                    : isThirdQualityRescueCandidate(c));
            if (ok && addSelected(selected, c, reason, hardLimit)) added++;
        }
    }

    private boolean containsPosition(
            LinkedHashMap<String, Models.ComboCandidate> selected, int position) {
        if (selected == null) return false;
        for (Models.ComboCandidate c : selected.values()) {
            if (c != null && c.position == position) return true;
        }
        return false;
    }

    private boolean addSelected(
            LinkedHashMap<String, Models.ComboCandidate> selected,
            Models.ComboCandidate c,
            String reason,
            int hardLimit) {
        if (selected == null || c == null || selected.size() >= hardLimit) return false;
        String key = candidateKey(c);
        if (key.length() == 0 || selected.containsKey(key)) return false;
        putString(c, "realtimeSelectionReason", reason);
        selected.put(key, c);
        return true;
    }

    private String candidateKey(Models.ComboCandidate c) {
        if (c == null || c.position < POSITION_MIN || c.position > POSITION_MAX) return "";
        int mask = c.k3Mask > 0 ? c.k3Mask : buildK3MaskSafe(c.n1, c.n2, c.n3);
        return "P" + c.position + "_K" + mask;
    }

    private boolean isMaturePromotionCandidate(Models.ComboCandidate c) {
        String status =
                upper(getString(c, "shadowPromotionStatus", getString(c, "promotionStatus", "")));
        if (!("PROMOTE_WATCH".equals(status)
                || "READY".equals(status)
                || "STRONG".equals(status)
                || "PROMOTION_READY".equals(status)
                || "PROMOTION_STRONG".equals(status))) return false;
        String domain =
                getString(c, "shadowPromotionDomain", getString(c, "shadowPromotionDomainKey", ""));
        if (domain.length() == 0 || isUnsafePromotionDomain(domain)) return false;
        return getDouble(c, "counterRisk", getDouble(c, "counterEvidenceScore", 1.0d))
                <= COUNTERFACTUAL_PROBE_MAX_RISK;
    }

    private boolean isCounterfactualStableCandidate(Models.ComboCandidate c) {
        if (getInt(c, "counterfactualHardRisk", 0) == 1) return false;
        double stability =
                getDouble(c, "rankStability", getDouble(c, "counterfactualRankStability", 0.0d));
        double risk =
                getDouble(
                        c,
                        "whyNotRisk",
                        getDouble(c, "counterfactualRisk", getDouble(c, "counterRisk", 1.0d)));
        return stability >= COUNTERFACTUAL_STABLE_LINE && risk <= COUNTERFACTUAL_PROBE_MAX_RISK;
    }

    private boolean isThirdQualityRescueCandidate(Models.ComboCandidate c) {
        if (getInt(c, "thirdHardRisk", getInt(c, "thirdGuardHardRisk", 0)) == 1) return false;
        double cover = getDouble(c, "coverageQuality", getDouble(c, "coverageQualityScore", 0.0d));
        double third = getDouble(c, "thirdQuality", getDouble(c, "thirdNumberQualityScore", 0.0d));
        double counter = getDouble(c, "counterRisk", getDouble(c, "counterEvidenceScore", 1.0d));
        return cover >= THIRD_RESCUE_COVERAGE_MIN
                && third >= THIRD_RESCUE_QUALITY_MIN
                && counter <= COUNTERFACTUAL_PROBE_MAX_RISK;
    }

    private void markRealtimeCandidateDomains(ArrayList<Models.ComboCandidate> list) {
        if (list == null || list.size() == 0) return;
        for (int i = 0; i < list.size(); i++) {
            Models.ComboCandidate c = list.get(i);
            if (c == null) continue;
            int rank = i + 1;
            putInt(c, "shortlistRank", rank);
            putInt(c, "decisionCandidate", 1);
            if (rank <= REALTIME_TOP40_LIMIT) {
                putInt(c, "realtimeTop40Rank", rank);
                putInt(c, "realtime_top40_rank", rank);
                putInt(c, "top40Candidate", 1);
                putInt(c, "adaptiveInputFlag", 1);
                putInt(c, "adaptive_input_flag", 1);
            } else {
                putInt(c, "realtimeTop40Rank", 0);
                putInt(c, "realtime_top40_rank", 0);
                putInt(c, "top40Candidate", 0);
                putInt(c, "adaptiveInputFlag", 0);
                putInt(c, "adaptive_input_flag", 0);
            }
        }
    }

    private void finalizeCandidateTextForTop(ArrayList<Models.ComboCandidate> list, int limit) {
        if (list == null || list.size() == 0) return;
        int n = Math.min(Math.max(0, limit), list.size());
        for (int i = 0; i < n; i++) finalizeCandidateText(list.get(i));
    }

    private void finalizeCandidateTextForList(List<Models.ComboCandidate> list) {
        if (list == null) return;
        for (int i = 0; i < list.size(); i++) finalizeCandidateText(list.get(i));
    }

    private void finalizeCandidateText(Models.ComboCandidate c) {
        if (c == null) return;
        String reason = getString(c, "realtimeSelectionReason", "GLOBAL_AUDIT_FILL");
        String summary = buildSelectedSummary(c, reason);
        putString(c, "scanSummary", summary);
        putString(c, "summaryText", summary);
        putString(c, "reasonText", summary);
        putString(c, "featureSummary", summary);
        putString(c, "mainChainSummary", summary);
        c.deepReason = summary;
        putString(
                c,
                "factorSummary",
                buildFactorSummary(
                        getDouble(c, "positionNeedScore", 0.0d),
                        getDouble(c, "numberSupplyScore", 0.0d),
                        getDouble(c, "coverageQuality", 0.0d),
                        getDouble(c, "thirdQuality", 0.0d),
                        getDouble(c, "counterEvidenceScore", 0.0d),
                        getDouble(c, "sampleReliability", 0.0d),
                        getDouble(c, "recentValidity", 0.0d)));
    }

    private String buildSelectedSummary(Models.ComboCandidate c, String selectionReason) {
        if (c == null) return "";
        return "全候选轻扫描｜位置="
                + c.position
                + "｜K3="
                + c.n1
                + ","
                + c.n2
                + ","
                + c.n3
                + "｜全局Rank="
                + getInt(c, "fullScanRank", c.rank)
                + "｜入选="
                + safeString(selectionReason)
                + "｜扫描="
                + percent(getDouble(c, "scanScore", c.deepScore))
                + "｜覆盖="
                + percent(getDouble(c, "coverageQuality", 0.0d))
                + "｜第三号="
                + percent(getDouble(c, "thirdQuality", 0.0d))
                + "｜反证="
                + percent(getDouble(c, "counterEvidenceScore", 0.0d))
                + "｜预P="
                + percent(c.finalP)
                + "｜说明=1200候选完整覆盖，扫描层只做轻量预评分；Probability/DynamicGate/DecisionGate继续完整裁决，扫描层不生成BET";
    }

    private void markAdaptiveInput(List<Models.ComboCandidate> list) {
        if (list == null) {
            return;
        }
        for (int i = 0; i < list.size(); i++) {
            Models.ComboCandidate c = list.get(i);
            if (c == null) {
                continue;
            }
            putInt(c, "adaptiveInputFlag", 1);
            putInt(c, "adaptive_input_flag", 1);
            putInt(c, "realtimeTop40Rank", i + 1);
            putString(c, "adaptiveStatus", "PENDING_ADAPTIVE_INPUT");
        }
    }

    private int numberResult(Object r, int fallback) {
        if (r instanceof Integer) {
            return ((Integer) r).intValue();
        }
        if (r instanceof Long) {
            return (int) ((Long) r).longValue();
        }
        if (r instanceof Number) {
            return ((Number) r).intValue();
        }
        return fallback;
    }

    private String buildShapeKey(
            int position, int n1, int n2, int n3, ComboScore combo, PositionScanContext ctx) {
        StringBuilder sb = new StringBuilder();

        sb.append("FULL_SCAN");
        sb.append("|P");
        sb.append(position);
        sb.append("|K3=");
        sb.append(n1);
        sb.append("-");
        sb.append(n2);
        sb.append("-");
        sb.append(n3);
        sb.append("|H=");
        sb.append(bucket(combo == null ? 0.0d : combo.healthScore));
        sb.append("|R=");
        sb.append(bucket(combo == null ? 0.0d : combo.riskScore));
        sb.append("|B=");
        sb.append(bucket(combo == null ? 0.0d : combo.balanceScore));

        if (ctx != null) {
            sb.append("|PH=");
            sb.append(bucket(ctx.positionHealth));
        }

        return sb.toString();
    }

    private String buildStateKey(
            int position, int n1, int n2, int n3, ComboScore combo, PositionScanContext ctx) {
        StringBuilder sb = new StringBuilder();

        sb.append("FULL_W12_24_48_96_360|P");
        sb.append(position);
        sb.append("|K3_");
        sb.append(n1);
        sb.append("_");
        sb.append(n2);
        sb.append("_");
        sb.append(n3);

        if (combo != null) {
            sb.append("|S=");
            sb.append(bucket(combo.scanScore));
            sb.append("|H=");
            sb.append(bucket(combo.healthScore));
            sb.append("|R=");
            sb.append(bucket(combo.riskScore));
            sb.append("|FS=");
            sb.append(bucket(combo.falseSeedRisk));
            sb.append("|W=");
            sb.append(bucket(combo.weakestNumberScore));
        }

        if (ctx != null) {
            sb.append("|PN=");
            sb.append(bucket(ctx.positionNoise));
        }

        return sb.toString();
    }

    private String buildRuleKey(ComboScore combo) {
        if (combo == null) {
            return "FULL_SCAN_RULE_EMPTY";
        }

        return "FULL_SCAN_RULE"
                + "|SCAN_"
                + bucket(combo.scanScore)
                + "|HEALTH_"
                + bucket(combo.healthScore)
                + "|RISK_"
                + bucket(combo.riskScore)
                + "|WEAK_"
                + bucket(combo.weakestNumberScore)
                + "|BAL_"
                + bucket(combo.balanceScore);
    }

    private String buildSameClassKey(int position, ComboScore combo) {
        if (combo == null) {
            return "P" + position + "|FULL_SCAN_EMPTY";
        }

        return "P"
                + position
                + "|FULL_SCAN"
                + "|S"
                + bucket(combo.scanScore)
                + "|H"
                + bucket(combo.healthScore)
                + "|R"
                + bucket(combo.riskScore)
                + "|B"
                + bucket(combo.balanceScore)
                + "|F"
                + bucket(combo.falseSeedRisk);
    }

    private String buildCandidateEvidenceKey(
            String targetIssue, int position, int k3Mask, ComboScore combo) {
        StringBuilder sb = new StringBuilder();

        sb.append("FSV20_");
        sb.append(safeString(targetIssue));
        sb.append("_P");
        sb.append(position);
        sb.append("_K");
        sb.append(k3Mask);

        if (combo != null) {
            sb.append("_S");
            sb.append((int) Math.round(combo.scanScore * 1000.0d));
            sb.append("_R");
            sb.append((int) Math.round(combo.riskScore * 1000.0d));
        }

        return sb.toString();
    }

    private String buildSummary(
            int position,
            int n1,
            int n2,
            int n3,
            ComboScore combo,
            double rawP,
            double finalP,
            double dynamicGate,
            double edge) {
        StringBuilder sb = new StringBuilder();

        sb.append("全候选扫描V20｜位置=");
        sb.append(position);
        sb.append("｜K3=");
        sb.append(n1);
        sb.append(",");
        sb.append(n2);
        sb.append(",");
        sb.append(n3);

        if (combo != null) {
            sb.append("｜扫描分=");
            sb.append(percent(combo.scanScore));
            sb.append("｜健康=");
            sb.append(percent(combo.healthScore));
            sb.append("｜支撑=");
            sb.append(percent(combo.supportScore));
            sb.append("｜风险=");
            sb.append(percent(combo.riskScore));
            sb.append("｜弱号=");
            sb.append(percent(combo.weakestNumberScore));
            sb.append("｜均衡=");
            sb.append(percent(combo.balanceScore));
            sb.append("｜伪强=");
            sb.append(percent(combo.falseSeedRisk));
            sb.append("｜位置健康=");
            sb.append(percent(combo.positionHealth));
        }

        sb.append("｜rawP=");
        sb.append(percent(rawP));
        sb.append("｜finalP=");
        sb.append(percent(finalP));
        sb.append("｜gate=");
        sb.append(percent(dynamicGate));
        sb.append("｜edge=");
        sb.append(signedPercent(edge));
        sb.append("｜说明=本文件完成1200全覆盖轻扫描并构建覆盖保护短名单，不创建BET，最终出手必须交给DecisionGateEngine");

        return sb.toString();
    }

    private double readDefaultDynamicGate() {
        double v =
                readAppDouble("DEFAULT_DYNAMIC_GATE", readAppDouble("DEFAULT_GATE", DEFAULT_GATE));

        if (v <= 0.0d) {
            v = DEFAULT_GATE;
        }

        return v;
    }

    private static int buildK3MaskSafe(int n1, int n2, int n3) {
        int mask = 0;
        if (isValidNumberStatic(n1)) mask |= 1 << n1;
        if (isValidNumberStatic(n2)) mask |= 1 << n2;
        if (isValidNumberStatic(n3)) mask |= 1 << n3;
        return mask;
    }

    private static boolean isValidK3Mask(int mask) {
        int count = 0;
        for (int n = NUMBER_MIN; n <= NUMBER_MAX; n++) if ((mask & (1 << n)) != 0) count++;
        return count == K_SIZE;
    }

    private static boolean isValidNumberStatic(int n) {
        return n >= NUMBER_MIN && n <= NUMBER_MAX;
    }

    private static double clampProbability(double v) {
        try {
            return AppConfig.clampProbability(v);
        } catch (Throwable e) {
            return clamp01(v);
        }
    }

    private static double readAppDouble(String field, double fallback) {
        try {
            Field f = AppConfig.class.getField(field);
            Object v = f.get(null);

            if (v instanceof Double) {
                return ((Double) v).doubleValue();
            }

            if (v instanceof Float) {
                return ((Float) v).doubleValue();
            }

            if (v instanceof Integer) {
                return ((Integer) v).doubleValue();
            }

            if (v instanceof Long) {
                return ((Long) v).doubleValue();
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

    private static int readAppInt(String field, int fallback) {
        try {
            Field f = AppConfig.class.getField(field);
            Object v = f.get(null);

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

    private static int readWindow120() {
        return readAppInt("WINDOW_120", 120);
    }

    private static int readWindow240() {
        return readAppInt("WINDOW_240", 240);
    }

    private static int readWindow500() {
        int w = readAppInt("WINDOW_500", 500);
        return readAppInt("WINDOW_720", w);
    }

    private static double clamp01(double v) {
        if (v < 0.0d) {
            return 0.0d;
        }

        if (v > 1.0d) {
            return 1.0d;
        }

        return v;
    }

    private static double clampSigned(double v) {
        if (v < -1.0d) {
            return -1.0d;
        }
        if (v > 1.0d) {
            return 1.0d;
        }
        return v;
    }

    private static double limitSigned(double v, double maxAbs) {
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

    private static String bucket(double v) {
        if (v >= 0.72d) {
            return "A";
        }

        if (v >= 0.60d) {
            return "B";
        }

        if (v >= 0.48d) {
            return "C";
        }

        if (v >= 0.34d) {
            return "D";
        }

        return "R";
    }

    private static String percent(double v) {
        return String.format(Locale.US, "%.1f%%", Double.valueOf(clamp01(v) * 100.0d));
    }

    private static String signedPercent(double v) {
        String sign = v >= 0.0d ? "+" : "";
        return sign + String.format(Locale.US, "%.2f%%", Double.valueOf(v * 100.0d));
    }

    private static String safeString(String s) {
        return s == null ? "" : s;
    }

    private static Field findPublicField(Object obj, String fieldName) {
        if (obj == null || fieldName == null || fieldName.length() == 0) return null;
        Class<?> cls = obj.getClass();
        String key = cls.getName() + "#" + fieldName;
        synchronized (PUBLIC_FIELD_CACHE_LOCK) {
            Field cached = PUBLIC_FIELD_CACHE.get(key);
            if (cached != null) return cached;
            if (PUBLIC_FIELD_MISS_CACHE.containsKey(key)) return null;
            try {
                Field f = cls.getField(fieldName);
                PUBLIC_FIELD_CACHE.put(key, f);
                return f;
            } catch (Throwable e) {
                PUBLIC_FIELD_MISS_CACHE.put(key, Boolean.TRUE);
                return null;
            }
        }
    }

    private static void putString(Object obj, String fieldName, String value) {
        try {
            Field f = findPublicField(obj, fieldName);
            if (f != null) f.set(obj, value == null ? "" : value);
        } catch (Throwable e) {
        }
    }

    private static void putInt(Object obj, String fieldName, int value) {
        try {
            Field f = findPublicField(obj, fieldName);
            if (f == null) return;
            try {
                f.setInt(obj, value);
            } catch (Throwable e) {
                f.set(obj, Integer.valueOf(value));
            }
        } catch (Throwable e) {
        }
    }

    private static void putDouble(Object obj, String fieldName, double value) {
        try {
            Field f = findPublicField(obj, fieldName);
            if (f == null) return;
            try {
                f.setDouble(obj, value);
            } catch (Throwable e) {
                f.set(obj, Double.valueOf(value));
            }
        } catch (Throwable e) {
        }
    }

    private static String getString(Object obj, String fieldName, String fallback) {
        try {
            Field f = findPublicField(obj, fieldName);
            Object v = f == null ? null : f.get(obj);
            return v == null ? fallback : String.valueOf(v);
        } catch (Throwable e) {
            return fallback;
        }
    }

    private static int getInt(Object obj, String fieldName, int fallback) {
        try {
            Field f = findPublicField(obj, fieldName);
            Object v = f == null ? null : f.get(obj);
            if (v instanceof Number) return ((Number) v).intValue();
            if (v != null) return Integer.parseInt(String.valueOf(v));
        } catch (Throwable e) {
        }
        return fallback;
    }

    private static double getDouble(Object obj, String fieldName, double fallback) {
        try {
            Field f = findPublicField(obj, fieldName);
            Object v = f == null ? null : f.get(obj);
            if (v instanceof Number) return ((Number) v).doubleValue();
            if (v != null) return Double.parseDouble(String.valueOf(v));
        } catch (Throwable e) {
        }
        return fallback;
    }

    private static String upper(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.US);
    }

    private static boolean isUnsafePromotionDomain(String key) {
        if (key == null || key.length() == 0) return true;
        try {
            return AppConfig.isUnsafePromotionDomainKey(key);
        } catch (Throwable e) {
            String s = upper(key);
            return s.indexOf("RANK1_HIT") >= 0
                    || s.indexOf("RANK2_HIT") >= 0
                    || s.indexOf("RANK3_HIT") >= 0
                    || s.indexOf("K3_MISS") >= 0
                    || s.indexOf("WOULD_HIT") >= 0
                    || s.indexOf("WOULD_MISS") >= 0
                    || s.indexOf("ACTUAL_NUMBER") >= 0
                    || s.indexOf("K3_LAYER") >= 0;
        }
    }

    private ArrayList<Models.Draw> normalizeDrawHistory(List rawHistory) {
        ArrayList<Models.Draw> out = new ArrayList<Models.Draw>();
        if (rawHistory == null) return out;
        for (int i = 0; i < rawHistory.size(); i++) {
            Object o = rawHistory.get(i);
            if (o instanceof Models.Draw) {
                Models.Draw d = (Models.Draw) o;
                if (d != null && d.isValid()) out.add(d);
            }
        }
        return out;
    }

    private String resolveBaseIssue(List<Models.Draw> history) {
        if (history == null) return "";
        for (int i = history.size() - 1; i >= 0; i--) {
            Models.Draw d = history.get(i);
            if (d != null && d.issue != null && d.issue.length() > 0) return d.issue;
        }
        return "";
    }

    private String buildNextIssue(String baseIssue) {
        String s = safeString(baseIssue).trim();
        if (s.length() == 0) return "";
        try {
            java.math.BigInteger n = new java.math.BigInteger(s);
            String next = n.add(java.math.BigInteger.ONE).toString();
            while (next.length() < s.length()) next = "0" + next;
            return next;
        } catch (Throwable e) {
            return "";
        }
    }

    private void saveStageCostSafe(
            DbStore runtimeStore, String targetIssue, String stage, long costMs) {
        if (runtimeStore == null) return;
        try {
            Method m =
                    runtimeStore
                            .getClass()
                            .getMethod(
                                    "saveRealtimeStageCost",
                                    String.class,
                                    String.class,
                                    long.class);
            m.invoke(
                    runtimeStore,
                    safeString(targetIssue),
                    safeString(stage),
                    Long.valueOf(Math.max(0L, costMs)));
        } catch (Throwable e) {
            try {
                runtimeStore.setState(
                        "full_scan_cost_" + safeString(stage),
                        String.valueOf(Math.max(0L, costMs)));
            } catch (Throwable ignore) {
            }
        }
    }

    private void writeRuntimeScanState(
            DbStore runtimeStore,
            String targetIssue,
            String entry,
            int scanned,
            int kept,
            long totalCost,
            String status) {
        if (runtimeStore == null) return;
        try {
            runtimeStore.setState("full_candidate_scan_version", VERSION);
            runtimeStore.setState("full_candidate_scan_speed_version", REALTIME_SPEED_VERSION);
            runtimeStore.setState("full_candidate_scan_target", safeString(targetIssue));
            runtimeStore.setState("full_candidate_scan_entry", safeString(entry));
            runtimeStore.setState("full_candidate_scan_scanned", String.valueOf(scanned));
            runtimeStore.setState("full_candidate_scan_kept", String.valueOf(kept));
            runtimeStore.setState("full_candidate_scan_status", safeString(status));
            runtimeStore.setState(
                    "full_candidate_scan_cost_ms", String.valueOf(Math.max(0L, totalCost)));
            runtimeStore.setState(
                    "full_candidate_scan_warn",
                    totalCost > REALTIME_SCAN_WARN_MS ? "OVER_TARGET" : "OK");
            runtimeStore.setState(
                    "full_candidate_scan_note",
                    "1200全覆盖轻扫描；轻量排序直接形成Top60精算池；最终40必须在全部后置证据完成后由FinalCandidateRankingEngine唯一生成；不写Trade、不生成BET");
        } catch (Throwable e) {
        }
    }

    private static final class PositionScanContext {
        int position;
        double positionHealth;
        double positionNoise;
        double positionDrift;
        double positionEdge;
        double positionEntropy;
        double positionTop3Mass;
    }

    private static final class NumberScore {
        int position;
        int number;

        double freq16;
        double freq32;
        double freq64;
        double freq120;
        double freq240;
        double freq500;

        double freqSlope;
        double freqAccel;
        double shortVsMid;
        double midVsLong;
        double trendConsistency;

        double positionEntropy;
        double positionTop3Mass;
        double positionStrengthSlope;
        double positionDrift;
        double positionNoiseScore;
        double positionHealth;
        double positionEdge;

        int currentGap;
        double avgGap;
        double gapZ;
        double gapPhase;
        double gapPressure;

        double hotPressure;
        double hotExhaustion;
        double coldRepairPower;
        double returnZoneScore;

        double transitionLift;
        double transitionConflict;
        double pairCoSupport;
        double pairConflict;

        double breakDepth;
        double recoverySpeed;
        double recoveryQuality;
        double falseRecoveryRisk;

        double seedScore;
        double falseSeedRisk;
        double numberEdge;
        double candidatePrior;
        double supportScore;
        double riskScore;
        double healthScore;
        double featureScore;
        double confidence;

        String stateKey;
        String windowStateKey;
        String clusterKey;
        String factorKey;
        String sameClassKey;
        double evidenceScore;
        double antiEvidenceScore;
        double sampleReliability;
        double recentValidity;
        String trendBucket;
        String gapBucket;
        String riskBucket;
        String transitionBucket;
        String healthBucket;
    }

    private static final class ComboScore {
        double seedScore;
        double featureScore;
        double supportScore;
        double riskScore;
        double healthScore;
        double confidence;

        double recoveryQuality;
        double transitionSupport;
        double transitionConflict;
        double pairSupport;
        double pairConflict;
        double gapSupport;
        double breakDepth;
        double hotExhaustion;
        double falseRecoveryRisk;

        double positionEdge;
        double numberEdge;
        double weakestNumberScore;
        double strongestNumberScore;
        double balanceScore;
        double concentrationRisk;
        double overlapRisk;
        double comboSynergy;
        double positionHealth;
        double positionNoise;
        double positionDrift;
        double falseSeedRisk;
        double numberSupport;
        double numberBias;
        double comboBias;
        double scanScore;

        double shapeP;
        double comboP;
        double recoveryP;
        double similarP;
        double memoryP;
        double candidatePrior;

        double auditPenalty;
        double auditBoost;
        double profileScore;
        double fallbackProfileScore;
        double promotionScore;
        double aPlusScore;

        double similarHitRate;
        double comboHitRate;
        int effectiveSample;
        int comboSample;
        int recentSample;
        double recentHitRate;
        double trendLife;
        double championGap;
        double comboGap;

        String stateMode;
    }

    private String nonEmptyNumberState(NumberScore s) {
        if (s == null) return "NS_NEUTRAL";
        String k = safeString(s.stateKey);
        if (k.length() == 0) k = buildFallbackNumberStateKey(s);
        return k.length() == 0 ? "NS_NEUTRAL_P" + s.position + "_N" + s.number : k;
    }

    private int countFeatureStateComplete(Map<String, Models.PreDrawFeature> map) {
        if (map == null) return 0;
        int ok = 0;
        for (int p = 1; p <= 10; p++)
            for (int n = 1; n <= 10; n++) {
                Models.PreDrawFeature f = map.get(String.valueOf(p) + "_" + String.valueOf(n));
                if (f != null && f.stateKey != null && f.stateKey.length() > 0) ok++;
            }
        return ok;
    }
}
