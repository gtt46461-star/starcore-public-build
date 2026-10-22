package com.ethk3.novaonepro;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * PreDrawFeatureEngine.java
 *
 * <p>V20 主链闭环修复第 6/22 文件：开奖前窗口特征实体化满血版。
 *
 * <p>本文件只负责： 1. 使用 targetIssue 之前的历史开奖计算开奖前特征。 2. 每个目标期生成 10 个位置 × 10 个号码 = 100 个 PreDrawFeature。
 * 3. 输出确定性 stateKey / clusterKey / factorKey / evidence / antiEvidence / actionAdvice。 4. 给后续
 * FullCandidateScan / CausalFactor / PositionCause / Probability / DecisionGate 提供真实字段。
 *
 * <p>本文件绝对不负责： 1. 不生成 BET / SHADOW / HOLD。 2. 不创建 Trade。 3. 不结算。 4. 不修改概率 finalP / dynamicGate /
 * edge。 5. 不绕过 DecisionGateEngine。
 *
 * <p>Java7 / AIDE / 原生 Android 兼容：无 lambda，无 stream，无第三方库。
 */
public final class PreDrawFeatureEngine {
    public static final String VERSION = "V44_NOVA_DIAGNOSIS_FREQUENCY_STABILITY_20260720_44157296";
    public static final String ROLE = "PRE_DRAW_FEATURE_ONLY_NO_BET_NO_SETTLE_NO_GATE";

    private static final int POSITION_MIN = 1;
    private static final int POSITION_MAX = 10;
    private static final int NUMBER_MIN = 1;
    private static final int NUMBER_MAX = 10;
    private static final double BASE_RATE = 0.100000d;
    private static final double EPS = 0.0000001d;

    /* 反射只用于兼容旧Models；同一字段只查找一次，避免100特征重复数万次getField。 */
    private static final Map<String, Field> FIELD_CACHE = new HashMap<String, Field>();
    private static final Set<String> FIELD_MISS_CACHE = new HashSet<String>();
    private static final Object FIELD_CACHE_LOCK = new Object();

    private final DbStore store;

    public PreDrawFeatureEngine() {
        this.store = null;
    }

    public PreDrawFeatureEngine(DbStore store) {
        this.store = store;
    }

    public Map<String, Models.PreDrawFeature> buildFeatures(
            String baseIssue, String targetIssue, List<Models.Draw> history) {
        long startedAt = System.currentTimeMillis();
        LinkedHashMap<String, Models.PreDrawFeature> out =
                new LinkedHashMap<String, Models.PreDrawFeature>();
        ArrayList<Models.Draw> visible = normalizeVisibleHistory(history, targetIssue);
        String realBaseIssue = normalizeBaseIssue(baseIssue, visible);
        String realTargetIssue = normalizeTargetIssue(targetIssue, realBaseIssue);

        if (visible.size() <= 0) {
            for (int p = POSITION_MIN; p <= POSITION_MAX; p++) {
                for (int n = NUMBER_MIN; n <= NUMBER_MAX; n++) {
                    Models.PreDrawFeature f =
                            buildEmptyFeature(realBaseIssue, realTargetIssue, p, n, "NO_HISTORY");
                    out.put(keyOf(p, n), f);
                }
            }
            ensureFeatureIntegrity(out, realBaseIssue, realTargetIssue);
            writeCompletenessState(
                    store,
                    realBaseIssue,
                    realTargetIssue,
                    out,
                    System.currentTimeMillis() - startedAt);
            return out;
        }

        PositionContext[] contexts = new PositionContext[POSITION_MAX + 1];
        for (int p = POSITION_MIN; p <= POSITION_MAX; p++) {
            contexts[p] = buildPositionContext(visible, p);
        }
        BoardContext board = buildBoardContext(contexts, visible);

        for (int p = POSITION_MIN; p <= POSITION_MAX; p++) {
            PositionContext ctx = contexts[p];
            for (int n = NUMBER_MIN; n <= NUMBER_MAX; n++) {
                Models.PreDrawFeature f =
                        buildFeature(realBaseIssue, realTargetIssue, visible, ctx, board, p, n);
                out.put(keyOf(p, n), f);
            }
        }
        ensureFeatureIntegrity(out, realBaseIssue, realTargetIssue);
        writeCompletenessState(
                store, realBaseIssue, realTargetIssue, out, System.currentTimeMillis() - startedAt);
        return out;
    }

    public Map<String, Models.PreDrawFeature> buildFeatures(
            List<Models.Draw> history, String targetIssue) {
        return buildFeatures("", targetIssue, history);
    }

    /** AutoFetchService 反射兼容入口：buildFeatures(List)。 */
    public Map<String, Models.PreDrawFeature> buildFeatures(List<Models.Draw> history) {
        String baseIssue = inferBaseIssue(history);
        String targetIssue = buildNextIssue(baseIssue);
        return buildFeatures(baseIssue, targetIssue, history);
    }

    public Map<String, Models.PreDrawFeature> buildFeaturesFromStore(
            String baseIssue, String targetIssue, int limit) {
        List<Models.Draw> history = loadHistoryFromStore(store, targetIssue, limit);
        return buildFeatures(baseIssue, targetIssue, history);
    }

    public Map<String, Models.PreDrawFeature> buildFeaturesFromStore(String targetIssue) {
        return buildFeaturesFromStore("", targetIssue, window360());
    }

    public ArrayList<Models.PreDrawFeature> buildFeatureList(
            String baseIssue, String targetIssue, List<Models.Draw> history) {
        Map<String, Models.PreDrawFeature> map = buildFeatures(baseIssue, targetIssue, history);
        return toSortedFeatureList(map);
    }

    public ArrayList<Models.PreDrawFeature> buildFeatureList(
            List<Models.Draw> history, String targetIssue) {
        return buildFeatureList("", targetIssue, history);
    }

    public ArrayList<Models.PreDrawFeature> buildFeatureList(List<Models.Draw> history) {
        String baseIssue = inferBaseIssue(history);
        String targetIssue = buildNextIssue(baseIssue);
        return buildFeatureList(baseIssue, targetIssue, history);
    }

    public ArrayList<Models.PreDrawFeature> buildFeatureListFromStore(
            String baseIssue, String targetIssue, int limit) {
        Map<String, Models.PreDrawFeature> map =
                buildFeaturesFromStore(baseIssue, targetIssue, limit);
        return toSortedFeatureList(map);
    }

    public ArrayList<Models.PreDrawFeature> buildFeatureListFromStore(String targetIssue) {
        return buildFeatureListFromStore("", targetIssue, window360());
    }

    public int buildAndSaveFromStore(String baseIssue, String targetIssue, int limit) {
        ArrayList<Models.PreDrawFeature> list =
                buildFeatureListFromStore(baseIssue, targetIssue, limit);
        return saveFeatureListReflect(store, list);
    }

    public int buildAndSave(String baseIssue, String targetIssue, List<Models.Draw> history) {
        ArrayList<Models.PreDrawFeature> list = buildFeatureList(baseIssue, targetIssue, history);
        return saveFeatureListReflect(store, list);
    }

    /** AutoFetchService 反射兼容入口：build(DbStore,List,String,String)。 */
    public ArrayList<Models.PreDrawFeature> build(
            DbStore dbStore, List<Models.Draw> history, String baseIssue, String targetIssue) {
        ArrayList<Models.PreDrawFeature> list = buildFeatureList(baseIssue, targetIssue, history);
        saveFeatureListReflect(dbStore, list);
        writeRuntimeSummary(dbStore, baseIssue, targetIssue, list, "build");
        return list;
    }

    /** AutoFetchService 反射兼容入口：analyze(DbStore,List,String,String)。 */
    public ArrayList<Models.PreDrawFeature> analyze(
            DbStore dbStore, List<Models.Draw> history, String baseIssue, String targetIssue) {
        return build(dbStore, history, baseIssue, targetIssue);
    }

    /** AutoFetchService 反射兼容入口：run(DbStore,List,String,String)。 */
    public ArrayList<Models.PreDrawFeature> run(
            DbStore dbStore, List<Models.Draw> history, String baseIssue, String targetIssue) {
        return build(dbStore, history, baseIssue, targetIssue);
    }

    public Models.PreDrawFeature getFeature(
            Map<String, Models.PreDrawFeature> map, int position, int number) {
        if (map == null) {
            return null;
        }
        return map.get(keyOf(position, number));
    }

    public static String keyOf(int position, int number) {
        return String.valueOf(position) + "_" + String.valueOf(number);
    }

    public static ArrayList<Models.PreDrawFeature> toSortedFeatureList(
            Map<String, Models.PreDrawFeature> map) {
        ArrayList<Models.PreDrawFeature> list = new ArrayList<Models.PreDrawFeature>();
        if (map == null || map.size() == 0) {
            return list;
        }
        for (Models.PreDrawFeature f : map.values()) {
            if (f != null) {
                list.add(f);
            }
        }
        Collections.sort(
                list,
                new Comparator<Models.PreDrawFeature>() {
                    public int compare(Models.PreDrawFeature a, Models.PreDrawFeature b) {
                        double ea =
                                getDoubleField(
                                        a,
                                        "featureScore",
                                        getDoubleField(a, "netEvidenceScore", 0.0d));
                        double eb =
                                getDoubleField(
                                        b,
                                        "featureScore",
                                        getDoubleField(b, "netEvidenceScore", 0.0d));
                        if (ea > eb) return -1;
                        if (ea < eb) return 1;

                        double ha = getDoubleField(a, "healthScore", 0.0d);
                        double hb = getDoubleField(b, "healthScore", 0.0d);
                        if (ha > hb) return -1;
                        if (ha < hb) return 1;

                        int pa = getIntField(a, "position", 0);
                        int pb = getIntField(b, "position", 0);
                        if (pa != pb) return pa - pb;

                        int na = getIntField(a, "number", 0);
                        int nb = getIntField(b, "number", 0);
                        return na - nb;
                    }
                });
        return list;
    }

    private Models.PreDrawFeature buildFeature(
            String baseIssue,
            String targetIssue,
            ArrayList<Models.Draw> history,
            PositionContext ctx,
            BoardContext board,
            int position,
            int number) {
        Models.PreDrawFeature f = new Models.PreDrawFeature();
        int size = history == null ? 0 : history.size();

        int w12 = window12();
        int w24 = window24();
        int w48 = window48();
        int w96 = window96();
        int w360 = window360();

        double freq12 = rate(history, position, number, w12);
        double freq24 = rate(history, position, number, w24);
        double freq48 = rate(history, position, number, w48);
        double freq96 = rate(history, position, number, w96);
        double freq360 = rate(history, position, number, w360);

        double freqSlope = calcFreqSlope(freq12, freq24, freq48, freq96, freq360);
        double freqAccel = calcFreqAccel(freq12, freq24, freq48, freq96);
        double shortVsMid = freq12 - freq48;
        double midVsLong = freq48 - freq360;
        double trendConsistency = calcTrendConsistency(freq12, freq24, freq48, freq96, freq360);

        int currentGap = currentGap(history, position, number);
        GapProfile gapProfile = buildGapProfile(history, position, number);
        double avgGap = gapProfile.avgGap;
        double gapZ = gapProfile.gapZ;
        double gapPhase = gapProfile.gapPhase;
        double gapPressure = calcGapPressure(currentGap, avgGap, gapZ);
        String omissionBucket = bucketOmission(currentGap, avgGap, gapZ);

        DensityProfile density = buildDensityProfile(history, position, number);
        RepeatProfile repeat = buildRepeatProfile(history, position, number);
        BiasProfile bias = buildBiasProfile(history, position, number);
        RelationProfile relation = buildRelationProfile(history, position, number);
        TransitionProfile transition = buildTransitionProfile(history, position, number);
        PairProfile pair = buildPairProfile(history, position, number);
        StreakProfile streak = buildStreakProfile(history, position, number);
        TimingProfile timing =
                buildTimingProfile(history, position, number, currentGap, avgGap, freqSlope);

        double hotPressure = calcHotPressure(freq12, freq24, freq48);
        double hotExhaustion = calcHotExhaustion(hotPressure, currentGap, avgGap, freqSlope, ctx);
        double returnZoneScore = calcReturnZoneScore(currentGap, avgGap, gapZ, freqSlope, ctx);
        double coldRepairPower =
                calcColdRepairPower(currentGap, avgGap, gapZ, freqSlope, returnZoneScore, ctx);

        double breakDepth = calcBreakDepth(freq12, freq48, freq360, ctx);
        double recoverySpeed = calcRecoverySpeed(freq12, freq24, freq48, currentGap, avgGap, gapZ);
        double falseRecoveryRisk =
                calcFalseRecoveryRisk(
                        recoverySpeed, returnZoneScore, hotExhaustion, breakDepth, ctx);
        double recoveryQuality =
                calcRecoveryQuality(
                        recoverySpeed,
                        returnZoneScore,
                        transition.transitionLift,
                        falseRecoveryRisk,
                        ctx);

        double positionEntropy = ctx == null ? 1.0d : ctx.entropy96;
        double positionTop3Mass = ctx == null ? 0.30d : ctx.top3Mass96;
        double positionStrengthSlope = ctx == null ? 0.0d : ctx.strengthSlope;
        double positionDrift = ctx == null ? 1.0d : ctx.positionDrift;
        double positionNoiseScore = ctx == null ? 1.0d : ctx.noiseScore;
        double positionHealth = ctx == null ? 0.0d : ctx.healthScore;
        double positionCongestion = ctx == null ? 0.0d : ctx.congestionScore;
        double crossPositionSync = relation.crossPositionSync;
        double crossPositionCrowd = relation.crossPositionCrowd;

        double positionNeedScore =
                calcPositionNeedScore(
                        gapPressure,
                        coldRepairPower,
                        returnZoneScore,
                        positionStrengthSlope,
                        relation.neighborPressure,
                        relation.tailPressure,
                        1.0d - positionNoiseScore,
                        crossPositionSync,
                        timing.timingScore,
                        positionCongestion);

        double numberSupplyScore =
                calcNumberSupplyScore(
                        freqSlope,
                        trendConsistency,
                        coldRepairPower,
                        returnZoneScore,
                        transition.transitionLift,
                        pair.pairCoSupport,
                        density.densityScore,
                        repeat.repeatScore,
                        relation.neighborSupport,
                        relation.tailSupport,
                        recoveryQuality,
                        bias.biasFitScore);

        double evidenceScore =
                calcEvidenceScore(
                        positionNeedScore,
                        numberSupplyScore,
                        trendConsistency,
                        transition.transitionLift,
                        pair.pairCoSupport,
                        recoveryQuality,
                        relation.neighborSupport,
                        relation.tailSupport,
                        density.densityScore,
                        timing.timingScore);

        double antiEvidenceScore =
                calcAntiEvidenceScore(
                        hotExhaustion,
                        transition.transitionConflict,
                        pair.pairConflict,
                        breakDepth,
                        falseRecoveryRisk,
                        positionNoiseScore,
                        positionDrift,
                        crossPositionCrowd,
                        density.densityRisk,
                        repeat.repeatRisk,
                        streak.recentLosePressure,
                        board);

        double netEvidenceScore = clampSigned(evidenceScore - antiEvidenceScore);
        double sampleReliability = calcSampleReliability(size, transition.sample, pair.sample, ctx);
        double recentValidity =
                calcRecentValidity(
                        freq12,
                        freq24,
                        freq48,
                        streak.recentHitSupport,
                        streak.recentLosePressure,
                        antiEvidenceScore);
        double confidence =
                calcConfidence(
                        size,
                        freq96,
                        transition.sample,
                        pair.sample,
                        ctx,
                        sampleReliability,
                        recentValidity);

        double supportScore =
                clamp01(
                        evidenceScore * 0.54d
                                + numberSupplyScore * 0.26d
                                + positionNeedScore * 0.20d);
        double riskScore =
                clamp01(
                        antiEvidenceScore * 0.60d
                                + falseRecoveryRisk * 0.18d
                                + positionNoiseScore * 0.12d
                                + crossPositionCrowd * 0.10d);
        double healthScore =
                clamp01(
                        supportScore * 0.44d
                                + (1.0d - riskScore) * 0.30d
                                + sampleReliability * 0.14d
                                + recentValidity * 0.12d);
        double featureScore =
                clamp01(0.50d + netEvidenceScore * 0.35d + confidence * 0.15d - riskScore * 0.12d);
        double positionEdge =
                limitSigned(
                        (positionNeedScore - 0.50d) * 0.16d - positionNoiseScore * 0.025d, 0.080d);
        double numberEdge = limitSigned((numberSupplyScore - antiEvidenceScore) * 0.18d, 0.090d);
        double candidatePrior = clamp01(BASE_RATE + positionEdge + numberEdge);

        String hotColdBucket = bucketHotCold(freqSlope, coldRepairPower, hotPressure);
        String densityBucket = bucketDensity(density.densityScore, density.densityRisk);
        String repeatBucket = bucketRepeat(repeat.repeatScore, repeat.repeatRisk);
        String parityBucket = bucketParity(bias.oddEvenBias, bias.highLowBias);
        String neighborTailBucket =
                bucketNeighborTail(
                        relation.neighborSupport,
                        relation.tailSupport,
                        relation.neighborPressure,
                        relation.tailPressure);
        String crowdBucket = bucketCrowd(positionCongestion, crossPositionCrowd);
        String syncBucket = bucketSync(crossPositionSync);
        String timingBucket = bucketTiming(timing.timingScore, timing.timingRisk);
        String riskBucket = bucketRisk(antiEvidenceScore, positionNoiseScore);
        String healthBucket = bucketHealth(healthScore);

        String stateKey =
                buildWindowStateKey(
                        position,
                        number,
                        hotColdBucket,
                        omissionBucket,
                        densityBucket,
                        repeatBucket,
                        parityBucket,
                        neighborTailBucket,
                        crowdBucket,
                        syncBucket,
                        streak.loseBucket,
                        timingBucket);
        String clusterKey =
                buildClusterKey(
                        hotColdBucket,
                        omissionBucket,
                        densityBucket,
                        repeatBucket,
                        parityBucket,
                        neighborTailBucket,
                        crowdBucket,
                        syncBucket,
                        timingBucket,
                        riskBucket);
        String factorKey =
                buildFactorKey(
                        position,
                        number,
                        positionNeedScore,
                        numberSupplyScore,
                        evidenceScore,
                        antiEvidenceScore,
                        sampleReliability,
                        recentValidity);
        String actionAdvice =
                buildActionAdvice(
                        evidenceScore,
                        antiEvidenceScore,
                        positionNeedScore,
                        numberSupplyScore,
                        sampleReliability,
                        recentValidity,
                        riskScore);
        String summary =
                buildSummaryText(
                        position,
                        number,
                        freq12,
                        freq24,
                        freq48,
                        freq96,
                        freq360,
                        freqSlope,
                        currentGap,
                        avgGap,
                        gapZ,
                        density,
                        repeat,
                        bias,
                        relation,
                        transition,
                        recoveryQuality,
                        falseRecoveryRisk,
                        evidenceScore,
                        antiEvidenceScore,
                        netEvidenceScore,
                        sampleReliability,
                        recentValidity,
                        healthScore,
                        candidatePrior,
                        actionAdvice);

        putString(f, "featureKey", keyOf(position, number));
        putString(f, "key", keyOf(position, number));
        putString(f, "baseIssue", safeString(baseIssue));
        putString(f, "targetIssue", safeString(targetIssue));
        putString(f, "sourceKey", "PREDRAW_MAIN_CHAIN_FIX06");
        putString(f, "sourceType", "PRE_DRAW_FEATURE");
        putString(f, "version", VERSION);
        putString(f, "stateKey", stateKey);
        putString(f, "windowStateKey", stateKey);
        putString(f, "dynamicWindowStateKey", stateKey);
        putString(f, "sameClassKey", clusterKey);
        putString(f, "clusterKey", clusterKey);
        putString(f, "factorKey", factorKey);
        putString(f, "ruleKey", hotColdBucket + "|" + omissionBucket + "|" + riskBucket);
        putString(f, "summaryText", summary);
        putString(f, "featureSummary", summary);
        putString(f, "reasonText", summary);
        putString(f, "actionAdvice", actionAdvice);
        putString(f, "status", statusOf(healthScore, riskScore, evidenceScore, antiEvidenceScore));
        putString(f, "hotColdBucket", hotColdBucket);
        putString(f, "omissionBucket", omissionBucket);
        putString(f, "densityBucket", densityBucket);
        putString(f, "repeatBucket", repeatBucket);
        putString(f, "parityBucket", parityBucket);
        putString(f, "neighborTailBucket", neighborTailBucket);
        putString(f, "crowdBucket", crowdBucket);
        putString(f, "syncBucket", syncBucket);
        putString(f, "timingBucket", timingBucket);
        putString(f, "riskBucket", riskBucket);
        putString(f, "healthBucket", healthBucket);
        putString(f, "trendBucket", hotColdBucket);
        putString(f, "gapBucket", omissionBucket);
        putString(f, "transitionBucket", bucketSigned("TRANS", transition.transitionLift));

        putInt(f, "position", position);
        putInt(f, "number", number);
        putLong(f, "createdAt", System.currentTimeMillis());
        putInt(f, "historySize", size);
        putInt(f, "visibleDrawCount", size);
        putInt(f, "noFutureLeakOk", 1);
        putInt(f, "sampleCount", size);
        putInt(f, "recentSampleCount", Math.min(size, w96));
        putInt(f, "similarSampleCount", Math.max(0, transition.sample + pair.sample));

        putDouble(f, "freq12", freq12);
        putDouble(f, "freq24", freq24);
        putDouble(f, "freq48", freq48);
        putDouble(f, "freq96", freq96);
        putDouble(f, "freq360", freq360);
        putDouble(f, "freq16", freq12);
        putDouble(f, "freq32", freq24);
        putDouble(f, "freq64", freq48);
        putDouble(f, "freq120", freq96);
        putDouble(f, "freq240", rate(history, position, number, window240Compat()));
        putDouble(f, "freq500", freq360);
        putDouble(f, "freqSlope", freqSlope);
        putDouble(f, "freqAccel", freqAccel);
        putDouble(f, "shortVsMid", shortVsMid);
        putDouble(f, "midVsLong", midVsLong);
        putDouble(f, "trendConsistency", trendConsistency);

        putDouble(f, "positionEntropy", positionEntropy);
        putDouble(f, "positionTop3Mass", positionTop3Mass);
        putDouble(f, "positionStrengthSlope", positionStrengthSlope);
        putDouble(f, "positionDrift", positionDrift);
        putDouble(f, "positionNoiseScore", positionNoiseScore);
        putDouble(f, "positionHealth", positionHealth);
        putDouble(f, "positionCongestion", positionCongestion);
        putDouble(f, "positionNeedScore", positionNeedScore);
        putDouble(f, "positionNeedStrength", positionNeedScore);
        putDouble(f, "positionEdge", positionEdge);

        putInt(f, "currentGap", currentGap);
        putDouble(f, "avgGap", avgGap);
        putDouble(f, "gapZ", gapZ);
        putDouble(f, "gapPhase", gapPhase);
        putDouble(f, "gapPressure", gapPressure);
        putDouble(f, "omissionPressure", gapPressure);

        putDouble(f, "hotPressure", hotPressure);
        putDouble(f, "hotExhaustion", hotExhaustion);
        putDouble(f, "coldRepairPower", coldRepairPower);
        putDouble(f, "returnZoneScore", returnZoneScore);
        putDouble(f, "densityScore", density.densityScore);
        putDouble(f, "densityRisk", density.densityRisk);
        putDouble(f, "repeatRhythm", repeat.repeatScore);
        putDouble(f, "repeatRisk", repeat.repeatRisk);
        putDouble(f, "oddEvenBias", bias.oddEvenBias);
        putDouble(f, "highLowBias", bias.highLowBias);
        putDouble(f, "biasFitScore", bias.biasFitScore);
        putDouble(f, "tailPressure", relation.tailPressure);
        putDouble(f, "neighborPressure", relation.neighborPressure);
        putDouble(f, "tailSupport", relation.tailSupport);
        putDouble(f, "neighborSupport", relation.neighborSupport);
        putDouble(f, "crossPositionSync", crossPositionSync);
        putDouble(f, "crossPositionFlow", crossPositionSync);
        putDouble(f, "crossPositionCrowd", crossPositionCrowd);
        putDouble(f, "sameNumberCrowd", relation.sameNumberCrowd);
        putDouble(f, "timingOffset", timing.timingOffset);
        putDouble(f, "timingScore", timing.timingScore);
        putDouble(f, "timingRisk", timing.timingRisk);
        putDouble(f, "recentLosePressure", streak.recentLosePressure);
        putDouble(f, "recentHitSupport", streak.recentHitSupport);

        putDouble(f, "transitionLift", transition.transitionLift);
        putDouble(f, "transitionConflict", transition.transitionConflict);
        putDouble(f, "transitionSample", transition.sample);
        putDouble(f, "transitionConditionalRate", transition.conditionalRate);
        putDouble(f, "transitionBaselineRate", transition.baselineRate);
        putDouble(f, "pairCoSupport", pair.pairCoSupport);
        putDouble(f, "pairConflict", pair.pairConflict);
        putDouble(f, "pairSample", pair.sample);

        putDouble(f, "breakDepth", breakDepth);
        putDouble(f, "recoverySpeed", recoverySpeed);
        putDouble(f, "recoveryQuality", recoveryQuality);
        putDouble(f, "falseRecoveryRisk", falseRecoveryRisk);
        putDouble(f, "seedScore", evidenceScore);
        putDouble(f, "falseSeedRisk", antiEvidenceScore);
        putDouble(f, "numberSupplyScore", numberSupplyScore);
        putDouble(f, "numberSupplyStrength", numberSupplyScore);
        putDouble(f, "numberEdge", numberEdge);
        putDouble(f, "candidatePrior", candidatePrior);
        putDouble(f, "supportScore", supportScore);
        putDouble(f, "riskScore", riskScore);
        putDouble(f, "healthScore", healthScore);
        putDouble(f, "featureScore", featureScore);
        putDouble(f, "confidence", confidence);

        putDouble(f, "evidenceScore", evidenceScore);
        putDouble(f, "antiEvidenceScore", antiEvidenceScore);
        putDouble(f, "counterEvidenceScore", antiEvidenceScore);
        putDouble(f, "netEvidenceScore", netEvidenceScore);
        putDouble(f, "sampleReliability", sampleReliability);
        putDouble(f, "recentValidity", recentValidity);
        putDouble(
                f,
                "similarHitRate",
                transition.conditionalRate > 0.0d
                        ? transition.conditionalRate
                        : pair.conditionalRate);
        return f;
    }

    private Models.PreDrawFeature buildEmptyFeature(
            String baseIssue, String targetIssue, int position, int number, String reason) {
        Models.PreDrawFeature f = new Models.PreDrawFeature();
        String stateKey = "P" + position + "_W12_24_48_96_360_NO_HISTORY_N" + number;
        putString(f, "featureKey", keyOf(position, number));
        putString(f, "key", keyOf(position, number));
        putString(f, "baseIssue", safeString(baseIssue));
        putString(f, "targetIssue", safeString(targetIssue));
        putString(f, "sourceKey", "PREDRAW_MAIN_CHAIN_FIX06");
        putString(f, "sourceType", "PRE_DRAW_FEATURE");
        putString(f, "version", VERSION);
        putString(f, "stateKey", stateKey);
        putString(f, "windowStateKey", stateKey);
        putString(f, "dynamicWindowStateKey", stateKey);
        putString(f, "clusterKey", "NO_HISTORY_CLUSTER");
        putString(f, "sameClassKey", "NO_HISTORY_CLUSTER");
        putString(f, "factorKey", "NO_HISTORY_FACTOR_P" + position + "_N" + number);
        putString(f, "ruleKey", "NO_HISTORY");
        putString(f, "summaryText", "开奖前特征不足：" + reason + "｜本文件只生成开奖前特征，不出号不结算不创建BET");
        putString(f, "featureSummary", "开奖前特征不足：" + reason);
        putString(f, "reasonText", "开奖前特征不足：" + reason);
        putString(f, "actionAdvice", "WATCH_NO_HISTORY");
        putString(f, "status", "NO_HISTORY");
        putInt(f, "position", position);
        putInt(f, "number", number);
        putLong(f, "createdAt", System.currentTimeMillis());
        putInt(f, "historySize", 0);
        putInt(f, "visibleDrawCount", 0);
        putInt(f, "noFutureLeakOk", 1);
        putInt(f, "sampleCount", 0);
        putInt(f, "recentSampleCount", 0);
        putDouble(f, "candidatePrior", BASE_RATE);
        putDouble(f, "riskScore", 1.0d);
        putDouble(f, "healthScore", 0.0d);
        putDouble(f, "featureScore", 0.0d);
        putDouble(f, "confidence", 0.0d);
        putDouble(f, "sampleReliability", 0.0d);
        putDouble(f, "recentValidity", 0.0d);
        putDouble(f, "evidenceScore", 0.0d);
        putDouble(f, "antiEvidenceScore", 1.0d);
        putDouble(f, "counterEvidenceScore", 1.0d);
        putDouble(f, "netEvidenceScore", -1.0d);
        putDouble(f, "positionNeedScore", 0.0d);
        putDouble(f, "numberSupplyScore", 0.0d);
        putDouble(f, "coverageQuality", 0.0d);
        putDouble(f, "thirdQuality", 0.0d);
        return f;
    }

    private void ensureFeatureIntegrity(
            Map<String, Models.PreDrawFeature> map, String baseIssue, String targetIssue) {
        if (map == null) return;
        for (int p = POSITION_MIN; p <= POSITION_MAX; p++) {
            for (int n = NUMBER_MIN; n <= NUMBER_MAX; n++) {
                String key = keyOf(p, n);
                Models.PreDrawFeature f = map.get(key);
                if (f == null) {
                    f = buildEmptyFeature(baseIssue, targetIssue, p, n, "FEATURE_MISSING");
                    map.put(key, f);
                    continue;
                }
                String state = f.stateKey == null ? "" : f.stateKey;
                if (state.length() == 0) state = "P" + p + "_W12_24_48_96_360_NEUTRAL_N" + n;
                f.stateKey = state;
                f.windowStateKey =
                        (f.windowStateKey == null || f.windowStateKey.length() == 0)
                                ? state
                                : f.windowStateKey;
                f.dynamicWindowStateKey =
                        (f.dynamicWindowStateKey == null || f.dynamicWindowStateKey.length() == 0)
                                ? state
                                : f.dynamicWindowStateKey;
                String cluster = f.clusterKey == null ? "" : f.clusterKey;
                if (cluster.length() == 0) cluster = "PREDRAW_NEUTRAL_CLUSTER_P" + p;
                f.clusterKey = cluster;
                f.sameClassKey =
                        (f.sameClassKey == null || f.sameClassKey.length() == 0)
                                ? cluster
                                : f.sameClassKey;
                if (f.factorKey == null || f.factorKey.length() == 0)
                    f.factorKey = "PREDRAW_NEUTRAL_FACTOR_P" + p + "_N" + n;
                f.position = p;
                f.number = n;
                f.noFutureLeakOk = 1;
                if (Double.isNaN(f.candidatePrior)
                        || Double.isInfinite(f.candidatePrior)
                        || f.candidatePrior <= 0.0d) f.candidatePrior = BASE_RATE;
                if (Double.isNaN(f.featureScore) || Double.isInfinite(f.featureScore))
                    f.featureScore = AppConfig.PREDRAW_NEUTRAL_SCORE;
                if (Double.isNaN(f.healthScore) || Double.isInfinite(f.healthScore))
                    f.healthScore = AppConfig.PREDRAW_NEUTRAL_SCORE;
                if (Double.isNaN(f.riskScore) || Double.isInfinite(f.riskScore))
                    f.riskScore = AppConfig.PREDRAW_NEUTRAL_SCORE;
            }
        }
    }

    private void writeCompletenessState(
            DbStore dbStore,
            String baseIssue,
            String targetIssue,
            Map<String, Models.PreDrawFeature> map,
            long costMs) {
        if (dbStore == null) return;
        int total = 0;
        int stateComplete = 0;
        int numericComplete = 0;
        if (map != null) {
            for (Models.PreDrawFeature f : map.values()) {
                if (f == null) continue;
                total++;
                if (getStringField(f, "stateKey", "").length() > 0
                        && getStringField(f, "clusterKey", "").length() > 0
                        && getStringField(f, "factorKey", "").length() > 0) stateComplete++;
                if (getIntField(f, "position", 0) >= 1
                        && getIntField(f, "number", 0) >= 1
                        && getDoubleField(f, "candidatePrior", -1.0d) >= 0.0d
                        && getDoubleField(f, "featureScore", -1.0d) >= 0.0d) numericComplete++;
            }
        }
        try {
            dbStore.setState("predraw_feature_version", VERSION);
            dbStore.setState("predraw_feature_base_issue", safeString(baseIssue));
            dbStore.setState("predraw_feature_target_issue", safeString(targetIssue));
            dbStore.setState("predraw_feature_count", String.valueOf(total));
            dbStore.setState("predraw_feature_state_complete", stateComplete + "/" + total);
            dbStore.setState("predraw_feature_numeric_complete", numericComplete + "/" + total);
            dbStore.setState("predraw_feature_cost_ms", String.valueOf(costMs));
            dbStore.setState(
                    "predraw_feature_policy", "100_NUMBER_FEATURES_NO_FUTURE_NO_BET_FIELD_CACHE");
        } catch (Throwable ignored) {
        }
    }

    private List<Models.Draw> loadHistoryFromStore(DbStore dbStore, String targetIssue, int limit) {
        ArrayList<Models.Draw> out = new ArrayList<Models.Draw>();
        if (dbStore == null) {
            return out;
        }
        if (limit <= 0) {
            limit = window360();
        }
        try {
            if (targetIssue != null && targetIssue.length() > 0) {
                List<Models.Draw> list = dbStore.getDrawsBeforeIssue(targetIssue, limit);
                if (list != null) {
                    out.addAll(list);
                }
            } else {
                List<Models.Draw> list = dbStore.getRecentDraws(limit);
                if (list != null) {
                    out.addAll(list);
                }
            }
        } catch (Throwable e) {
        }
        return out;
    }

    private int saveFeatureListReflect(DbStore dbStore, ArrayList<Models.PreDrawFeature> list) {
        if (dbStore == null || list == null || list.size() == 0) {
            return 0;
        }
        String[] methodNames =
                new String[] {
                    "savePreDrawFeatures",
                    "savePreDrawFeatureList",
                    "savePreDrawFeatureSamples",
                    "insertPreDrawFeatures"
                };
        for (int i = 0; i < methodNames.length; i++) {
            try {
                Method m = dbStore.getClass().getMethod(methodNames[i], List.class);
                Object r = m.invoke(dbStore, list);
                if (r instanceof Integer) {
                    return ((Integer) r).intValue();
                }
                if (r instanceof Long) {
                    return (int) ((Long) r).longValue();
                }
                if (r instanceof Number) {
                    return ((Number) r).intValue();
                }
                return list.size();
            } catch (Throwable e) {
            }
        }
        return 0;
    }

    private void writeRuntimeSummary(
            DbStore dbStore,
            String baseIssue,
            String targetIssue,
            ArrayList<Models.PreDrawFeature> list,
            String mode) {
        if (dbStore == null) {
            return;
        }
        try {
            Method m = dbStore.getClass().getMethod("setState", String.class, String.class);
            m.invoke(dbStore, "predraw_feature_version", VERSION);
            m.invoke(dbStore, "predraw_feature_mode", mode == null ? "" : mode);
            m.invoke(dbStore, "predraw_feature_base_issue", safeString(baseIssue));
            m.invoke(dbStore, "predraw_feature_target_issue", safeString(targetIssue));
            m.invoke(
                    dbStore,
                    "predraw_feature_count",
                    String.valueOf(list == null ? 0 : list.size()));
            m.invoke(
                    dbStore,
                    "predraw_feature_policy",
                    "W12/24/48/96/360 stateKey+clusterKey+evidence+antiEvidence only; no BET/no"
                        + " settle/no gate");
        } catch (Throwable e) {
        }
    }

    private ArrayList<Models.Draw> normalizeVisibleHistory(
            List<Models.Draw> history, String targetIssue) {
        ArrayList<Models.Draw> list = new ArrayList<Models.Draw>();
        if (history == null || history.size() == 0) {
            return list;
        }
        for (int i = 0; i < history.size(); i++) {
            Models.Draw d = history.get(i);
            if (!isValidDraw(d)) {
                continue;
            }
            if (targetIssue != null
                    && targetIssue.length() > 0
                    && d.issue != null
                    && d.issue.length() > 0) {
                if (compareIssue(d.issue, targetIssue) >= 0) {
                    continue;
                }
            }
            list.add(d);
        }
        Collections.sort(
                list,
                new Comparator<Models.Draw>() {
                    public int compare(Models.Draw a, Models.Draw b) {
                        String ia = a == null ? "" : safeString(a.issue);
                        String ib = b == null ? "" : safeString(b.issue);
                        int c = compareIssue(ia, ib);
                        if (c != 0) {
                            return c;
                        }
                        long ta = a == null ? 0L : a.openTime;
                        long tb = b == null ? 0L : b.openTime;
                        if (ta < tb) return -1;
                        if (ta > tb) return 1;
                        return 0;
                    }
                });
        int max = window360();
        if (max <= 0) {
            max = 360;
        }
        if (list.size() > max) {
            ArrayList<Models.Draw> tail = new ArrayList<Models.Draw>();
            for (int i = list.size() - max; i < list.size(); i++) {
                tail.add(list.get(i));
            }
            return tail;
        }
        return list;
    }

    private PositionContext buildPositionContext(ArrayList<Models.Draw> history, int position) {
        PositionContext ctx = new PositionContext();
        ctx.position = position;
        int[] count12 = countNumbers(history, position, window12());
        int[] count24 = countNumbers(history, position, window24());
        int[] count48 = countNumbers(history, position, window48());
        int[] count96 = countNumbers(history, position, window96());
        int[] count360 = countNumbers(history, position, window360());
        ctx.size12 = effectiveWindow(history, window12());
        ctx.size24 = effectiveWindow(history, window24());
        ctx.size48 = effectiveWindow(history, window48());
        ctx.size96 = effectiveWindow(history, window96());
        ctx.size360 = effectiveWindow(history, window360());
        ctx.entropy12 = entropy(count12, ctx.size12);
        ctx.entropy24 = entropy(count24, ctx.size24);
        ctx.entropy48 = entropy(count48, ctx.size48);
        ctx.entropy96 = entropy(count96, ctx.size96);
        ctx.entropy360 = entropy(count360, ctx.size360);
        ctx.top3Mass12 = topMass(count12, ctx.size12, 3);
        ctx.top3Mass24 = topMass(count24, ctx.size24, 3);
        ctx.top3Mass48 = topMass(count48, ctx.size48, 3);
        ctx.top3Mass96 = topMass(count96, ctx.size96, 3);
        ctx.top3Mass360 = topMass(count360, ctx.size360, 3);
        ctx.strengthSlope =
                clampSigned(
                        (ctx.top3Mass12 - ctx.top3Mass96) * 2.4d
                                + (ctx.top3Mass24 - ctx.top3Mass360) * 0.9d);
        ctx.positionDrift = distributionDrift(count12, ctx.size12, count96, ctx.size96);
        ctx.congestionScore =
                clamp01((ctx.top3Mass12 - 0.30d) * 2.2d + (ctx.top3Mass24 - 0.34d) * 1.2d);
        ctx.noiseScore = calcPositionNoise(ctx);
        ctx.healthScore = calcPositionHealth(ctx);
        return ctx;
    }

    private BoardContext buildBoardContext(
            PositionContext[] contexts, ArrayList<Models.Draw> history) {
        BoardContext b = new BoardContext();
        if (contexts == null || contexts.length <= 0) {
            b.boardNoise = 1.0d;
            b.boardHealth = 0.0d;
            return b;
        }
        double entropy = 0.0d;
        double drift = 0.0d;
        double noise = 0.0d;
        double health = 0.0d;
        double strength = 0.0d;
        double crowd = 0.0d;
        int count = 0;
        for (int p = POSITION_MIN; p <= POSITION_MAX; p++) {
            PositionContext ctx = contexts[p];
            if (ctx == null) {
                continue;
            }
            entropy += ctx.entropy96;
            drift += ctx.positionDrift;
            noise += ctx.noiseScore;
            health += ctx.healthScore;
            strength += ctx.strengthSlope;
            crowd += ctx.congestionScore;
            count++;
        }
        if (count <= 0) {
            count = 1;
        }
        b.boardEntropy = entropy / count;
        b.boardDrift = drift / count;
        b.boardNoise = noise / count;
        b.boardHealth = health / count;
        b.boardStrengthSlope = strength / count;
        b.boardCrowd = crowd / count;
        b.historySize = history == null ? 0 : history.size();
        b.boardShock =
                clamp01(
                        b.boardDrift * 0.36d
                                + b.boardNoise * 0.34d
                                + b.boardCrowd * 0.18d
                                + Math.max(0.0d, -b.boardStrengthSlope) * 0.12d);
        return b;
    }

    private int[] countNumbers(ArrayList<Models.Draw> history, int position, int window) {
        int[] count = new int[NUMBER_MAX + 1];
        if (history == null || history.size() == 0 || !isValidPosition(position)) {
            return count;
        }
        int start = history.size() - window;
        if (start < 0) {
            start = 0;
        }
        for (int i = start; i < history.size(); i++) {
            int n = getNumber(history.get(i), position);
            if (isValidNumber(n)) {
                count[n]++;
            }
        }
        return count;
    }

    private double rate(ArrayList<Models.Draw> history, int position, int number, int window) {
        int total = effectiveWindow(history, window);
        if (total <= 0 || !isValidPosition(position) || !isValidNumber(number)) {
            return 0.0d;
        }
        int count = 0;
        int start = history.size() - window;
        if (start < 0) {
            start = 0;
        }
        for (int i = start; i < history.size(); i++) {
            int n = getNumber(history.get(i), position);
            if (n == number) {
                count++;
            }
        }
        return safeRate(count, total);
    }

    private int effectiveWindow(ArrayList<Models.Draw> history, int window) {
        if (history == null || history.size() == 0 || window <= 0) {
            return 0;
        }
        return Math.min(history.size(), window);
    }

    private double calcFreqSlope(double f12, double f24, double f48, double f96, double f360) {
        double a = f12 - f48;
        double b = f24 - f96;
        double c = f96 - f360;
        return clampSigned((a * 0.55d + b * 0.32d + c * 0.13d) * 4.2d);
    }

    private double calcFreqAccel(double f12, double f24, double f48, double f96) {
        double shortMove = f12 - f24;
        double midMove = f48 - f96;
        return clampSigned((shortMove - midMove) * 5.2d);
    }

    private double calcTrendConsistency(
            double f12, double f24, double f48, double f96, double f360) {
        double[] arr = new double[] {f12, f24, f48, f96, f360};
        int same = 0;
        int total = 0;
        int direction = 0;
        for (int i = 0; i < arr.length - 1; i++) {
            double d = arr[i] - arr[i + 1];
            if (Math.abs(d) < 0.004d) {
                continue;
            }
            int curDir = d > 0.0d ? 1 : -1;
            if (direction == 0) {
                direction = curDir;
            }
            total++;
            if (curDir == direction) {
                same++;
            }
        }
        if (total <= 0) {
            return 0.50d;
        }
        return clamp01(((double) same) / ((double) total));
    }

    private int currentGap(ArrayList<Models.Draw> history, int position, int number) {
        if (history == null
                || history.size() == 0
                || !isValidPosition(position)
                || !isValidNumber(number)) {
            return 0;
        }
        int gap = 0;
        for (int i = history.size() - 1; i >= 0; i--) {
            int n = getNumber(history.get(i), position);
            if (n == number) {
                return gap;
            }
            gap++;
        }
        return history.size();
    }

    private GapProfile buildGapProfile(ArrayList<Models.Draw> history, int position, int number) {
        GapProfile g = new GapProfile();
        g.currentGap = currentGap(history, position, number);
        g.avgGap = 10.0d;
        g.gapStd = 6.0d;
        g.gapZ = 0.0d;
        g.gapPhase = 0.0d;
        if (history == null
                || history.size() <= 1
                || !isValidPosition(position)
                || !isValidNumber(number)) {
            return g;
        }
        ArrayList<Integer> hits = new ArrayList<Integer>();
        for (int i = 0; i < history.size(); i++) {
            int n = getNumber(history.get(i), position);
            if (n == number) {
                hits.add(Integer.valueOf(i));
            }
        }
        if (hits.size() < 2) {
            g.gapZ = clampSigned((g.currentGap - g.avgGap) / Math.max(1.0d, g.gapStd));
            g.gapPhase = clamp01(((double) g.currentGap) / 20.0d);
            return g;
        }
        double sum = 0.0d;
        ArrayList<Integer> gaps = new ArrayList<Integer>();
        for (int i = 1; i < hits.size(); i++) {
            int gap = hits.get(i).intValue() - hits.get(i - 1).intValue();
            gaps.add(Integer.valueOf(gap));
            sum += gap;
        }
        g.avgGap = sum / Math.max(1, gaps.size());
        double var = 0.0d;
        for (int i = 0; i < gaps.size(); i++) {
            double d = gaps.get(i).intValue() - g.avgGap;
            var += d * d;
        }
        var = var / Math.max(1, gaps.size());
        g.gapStd = Math.sqrt(var);
        if (g.gapStd < 1.0d) {
            g.gapStd = 1.0d;
        }
        g.gapZ = clampSigned((g.currentGap - g.avgGap) / g.gapStd);
        g.gapPhase = clamp01(((double) g.currentGap) / Math.max(1.0d, g.avgGap * 2.0d));
        return g;
    }

    private DensityProfile buildDensityProfile(
            ArrayList<Models.Draw> history, int position, int number) {
        DensityProfile d = new DensityProfile();
        if (history == null || history.size() == 0) {
            return d;
        }
        int w12 = effectiveWindow(history, window12());
        int w24 = effectiveWindow(history, window24());
        int c12 = countHits(history, position, number, window12());
        int c24 = countHits(history, position, number, window24());
        double r12 = safeRate(c12, w12);
        double r24 = safeRate(c24, w24);
        d.densityScore =
                clamp01(
                        (r12 - BASE_RATE) * 3.0d
                                + (r24 - BASE_RATE) * 1.5d
                                + safeRate(c12, 3) * 0.18d);
        d.densityRisk = clamp01((r12 - BASE_RATE * 1.8d) * 2.0d + Math.max(0.0d, c12 - 3) * 0.14d);
        d.hitCount12 = c12;
        d.hitCount24 = c24;
        return d;
    }

    private RepeatProfile buildRepeatProfile(
            ArrayList<Models.Draw> history, int position, int number) {
        RepeatProfile r = new RepeatProfile();
        if (history == null || history.size() <= 1) {
            return r;
        }
        int last = getNumber(history.get(history.size() - 1), position);
        int prev = history.size() > 1 ? getNumber(history.get(history.size() - 2), position) : 0;
        int repeatCount = 0;
        int recentHit = 0;
        int start = Math.max(1, history.size() - window24());
        for (int i = start; i < history.size(); i++) {
            int a = getNumber(history.get(i - 1), position);
            int b = getNumber(history.get(i), position);
            if (a == b) {
                repeatCount++;
                if (b == number) {
                    recentHit++;
                }
            }
        }
        r.repeatScore =
                clamp01(
                        safeRate(recentHit, Math.max(1, repeatCount)) * 0.55d
                                + (last == number ? 0.18d : 0.0d));
        r.repeatRisk =
                clamp01(
                        (last == number && prev == number ? 0.42d : 0.0d)
                                + Math.max(0, repeatCount - 5) * 0.045d);
        r.repeatCount = repeatCount;
        return r;
    }

    private BiasProfile buildBiasProfile(ArrayList<Models.Draw> history, int position, int number) {
        BiasProfile b = new BiasProfile();
        int total = effectiveWindow(history, window48());
        if (total <= 0) {
            return b;
        }
        int odd = 0;
        int high = 0;
        int start = history.size() - window48();
        if (start < 0) start = 0;
        for (int i = start; i < history.size(); i++) {
            int n = getNumber(history.get(i), position);
            if (!isValidNumber(n)) continue;
            if ((n % 2) == 1) odd++;
            if (n >= 6) high++;
        }
        double oddRate = safeRate(odd, total);
        double highRate = safeRate(high, total);
        b.oddEvenBias = clampSigned((oddRate - 0.50d) * 2.0d);
        b.highLowBias = clampSigned((highRate - 0.50d) * 2.0d);
        boolean numOdd = (number % 2) == 1;
        boolean numHigh = number >= 6;
        double fit = 0.50d;
        fit += (numOdd ? b.oddEvenBias : -b.oddEvenBias) * 0.18d;
        fit += (numHigh ? b.highLowBias : -b.highLowBias) * 0.18d;
        b.biasFitScore = clamp01(fit);
        return b;
    }

    private RelationProfile buildRelationProfile(
            ArrayList<Models.Draw> history, int position, int number) {
        RelationProfile r = new RelationProfile();
        if (history == null || history.size() <= 3) {
            return r;
        }
        int tail = number % 10;
        int neighborA = number - 1;
        int neighborB = number + 1;
        int tailHit = 0;
        int neighborHit = 0;
        int sameNumberOtherPos = 0;
        int sample = 0;
        int start = Math.max(0, history.size() - window48());
        for (int i = start; i < history.size(); i++) {
            Models.Draw d = history.get(i);
            int cur = getNumber(d, position);
            if (!isValidNumber(cur)) continue;
            sample++;
            if ((cur % 10) == tail) tailHit++;
            if (cur == neighborA || cur == neighborB) neighborHit++;
            for (int p = POSITION_MIN; p <= POSITION_MAX; p++) {
                if (p == position) continue;
                if (getNumber(d, p) == number) {
                    sameNumberOtherPos++;
                    break;
                }
            }
        }
        r.tailSupport = clamp01((safeRate(tailHit, sample) - 0.10d) * 3.0d + 0.10d);
        r.neighborSupport = clamp01((safeRate(neighborHit, sample) - 0.20d) * 2.6d + 0.10d);
        r.tailPressure = clamp01((0.10d - safeRate(tailHit, sample)) * 3.0d + 0.12d);
        r.neighborPressure = clamp01((0.20d - safeRate(neighborHit, sample)) * 2.4d + 0.10d);
        r.sameNumberCrowd = clamp01(safeRate(sameNumberOtherPos, sample) * 1.8d);

        Models.Draw last = history.get(history.size() - 1);
        int acrossNow = 0;
        for (int p = POSITION_MIN; p <= POSITION_MAX; p++) {
            if (p == position) continue;
            int other = getNumber(last, p);
            if (other == number
                    || other == neighborA
                    || other == neighborB
                    || (isValidNumber(other) && other % 10 == tail)) {
                acrossNow++;
            }
        }
        r.crossPositionSync = clamp01(((double) acrossNow) / 4.0d);
        r.crossPositionCrowd =
                clamp01(Math.max(0, acrossNow - 2) / 5.0d + r.sameNumberCrowd * 0.25d);
        return r;
    }

    private TransitionProfile buildTransitionProfile(
            ArrayList<Models.Draw> history, int position, int number) {
        TransitionProfile t = new TransitionProfile();
        if (history == null
                || history.size() < 3
                || !isValidPosition(position)
                || !isValidNumber(number)) {
            return t;
        }
        int lastNumber = getNumber(history.get(history.size() - 1), position);
        if (!isValidNumber(lastNumber)) {
            return t;
        }
        int prevCount = 0;
        int nextHit = 0;
        int start = Math.max(1, history.size() - window360());
        for (int i = start; i < history.size(); i++) {
            int prev = getNumber(history.get(i - 1), position);
            int cur = getNumber(history.get(i), position);
            if (prev == lastNumber) {
                prevCount++;
                if (cur == number) {
                    nextHit++;
                }
            }
        }
        t.sample = prevCount;
        if (prevCount <= 0) {
            return t;
        }
        double conditional = safeRate(nextHit, prevCount);
        double baseline = rate(history, position, number, Math.min(window96(), history.size()));
        double lift = conditional - baseline;
        if (lift > 0.0d) {
            t.transitionLift = clamp01(lift * 5.0d);
            t.transitionConflict = 0.0d;
        } else {
            t.transitionLift = 0.0d;
            t.transitionConflict = clamp01(Math.abs(lift) * 5.0d);
        }
        t.conditionalRate = conditional;
        t.baselineRate = baseline;
        return t;
    }

    private PairProfile buildPairProfile(ArrayList<Models.Draw> history, int position, int number) {
        PairProfile p = new PairProfile();
        if (history == null
                || history.size() < 5
                || !isValidPosition(position)
                || !isValidNumber(number)) {
            return p;
        }
        Models.Draw last = history.get(history.size() - 1);
        int leftPos = position - 1;
        int rightPos = position + 1;
        if (leftPos < POSITION_MIN) leftPos = POSITION_MAX;
        if (rightPos > POSITION_MAX) rightPos = POSITION_MIN;
        int leftValue = getNumber(last, leftPos);
        int rightValue = getNumber(last, rightPos);
        int sample = 0;
        int support = 0;
        int start = Math.max(0, history.size() - window360());
        for (int i = start; i < history.size(); i++) {
            Models.Draw d = history.get(i);
            boolean matched = false;
            if (isValidNumber(leftValue)) {
                matched = getNumber(d, leftPos) == leftValue;
            }
            if (!matched && isValidNumber(rightValue)) {
                matched = getNumber(d, rightPos) == rightValue;
            }
            if (!matched) {
                continue;
            }
            sample++;
            if (getNumber(d, position) == number) {
                support++;
            }
        }
        p.sample = sample;
        if (sample <= 0) {
            return p;
        }
        double conditional = safeRate(support, sample);
        double baseline = rate(history, position, number, Math.min(window96(), history.size()));
        double diff = conditional - baseline;
        if (diff > 0.0d) {
            p.pairCoSupport = clamp01(diff * 4.0d);
            p.pairConflict = 0.0d;
        } else {
            p.pairCoSupport = 0.0d;
            p.pairConflict = clamp01(Math.abs(diff) * 4.0d);
        }
        p.conditionalRate = conditional;
        return p;
    }

    private StreakProfile buildStreakProfile(
            ArrayList<Models.Draw> history, int position, int number) {
        StreakProfile s = new StreakProfile();
        if (history == null || history.size() == 0) {
            s.loseBucket = "LOSE_0";
            return s;
        }
        int gap = currentGap(history, position, number);
        s.recentLosePressure = clamp01(((double) gap) / 28.0d);
        int hit12 = countHits(history, position, number, window12());
        int hit24 = countHits(history, position, number, window24());
        s.recentHitSupport =
                clamp01(
                        safeRate(hit12, effectiveWindow(history, window12())) * 1.4d
                                + safeRate(hit24, effectiveWindow(history, window24())) * 0.4d);
        if (gap <= 3) s.loseBucket = "LOSE_0_3";
        else if (gap <= 8) s.loseBucket = "LOSE_4_8";
        else if (gap <= 18) s.loseBucket = "LOSE_9_18";
        else s.loseBucket = "LOSE_19_PLUS";
        return s;
    }

    private TimingProfile buildTimingProfile(
            ArrayList<Models.Draw> history,
            int position,
            int number,
            int currentGap,
            double avgGap,
            double freqSlope) {
        TimingProfile t = new TimingProfile();
        if (avgGap <= 0.0d) avgGap = 10.0d;
        double ratio = ((double) currentGap) / Math.max(1.0d, avgGap);
        t.timingOffset = ratio - 1.0d;
        if (ratio >= 0.70d && ratio <= 1.80d) {
            t.timingScore = 0.62d;
        } else if (ratio > 1.80d && ratio <= 2.70d) {
            t.timingScore = 0.44d;
        } else {
            t.timingScore = 0.22d;
        }
        if (freqSlope < -0.12d && ratio > 1.90d) {
            t.timingRisk += 0.32d;
        }
        if (ratio < 0.30d) {
            t.timingRisk += 0.28d;
        }
        t.timingRisk = clamp01(t.timingRisk);
        return t;
    }

    private int countHits(ArrayList<Models.Draw> history, int position, int number, int window) {
        if (history == null
                || history.size() == 0
                || !isValidPosition(position)
                || !isValidNumber(number)) {
            return 0;
        }
        int count = 0;
        int start = history.size() - window;
        if (start < 0) start = 0;
        for (int i = start; i < history.size(); i++) {
            if (getNumber(history.get(i), position) == number) {
                count++;
            }
        }
        return count;
    }

    private double calcGapPressure(int currentGap, double avgGap, double gapZ) {
        double pressure = 0.0d;
        if (avgGap <= 0.0d) {
            avgGap = 10.0d;
        }
        double ratio = ((double) currentGap) / avgGap;
        if (ratio >= 0.80d && ratio <= 2.20d) {
            pressure += 0.35d;
        }
        if (gapZ > 0.0d) {
            pressure += Math.min(0.45d, gapZ * 0.22d);
        }
        if (currentGap >= 8 && currentGap <= 36) {
            pressure += 0.20d;
        }
        return clamp01(pressure);
    }

    private double calcHotPressure(double f12, double f24, double f48) {
        double shortHot = Math.max(f12, f24);
        double stableHot = f48;
        double hot = (shortHot - BASE_RATE) * 3.0d + (stableHot - BASE_RATE) * 1.2d;
        return clamp01(hot);
    }

    private double calcHotExhaustion(
            double hotPressure,
            int currentGap,
            double avgGap,
            double freqSlope,
            PositionContext ctx) {
        double exhaustion = 0.0d;
        if (hotPressure > 0.30d) {
            exhaustion += hotPressure * 0.38d;
        }
        if (currentGap > Math.max(6.0d, avgGap * 0.80d)) {
            exhaustion += 0.20d;
        }
        if (freqSlope < -0.08d) {
            exhaustion += Math.min(0.30d, Math.abs(freqSlope));
        }
        if (ctx != null && ctx.positionDrift > 0.35d) {
            exhaustion += ctx.positionDrift * 0.20d;
        }
        return clamp01(exhaustion);
    }

    private double calcReturnZoneScore(
            int currentGap, double avgGap, double gapZ, double freqSlope, PositionContext ctx) {
        double score = 0.0d;
        if (avgGap <= 0.0d) {
            avgGap = 10.0d;
        }
        double ratio = ((double) currentGap) / Math.max(1.0d, avgGap);
        if (ratio >= 0.75d && ratio <= 1.80d) {
            score += 0.35d;
        } else if (ratio > 1.80d && ratio <= 2.80d) {
            score += 0.20d;
        }
        if (gapZ >= -0.20d && gapZ <= 1.60d) {
            score += 0.25d;
        }
        if (freqSlope >= -0.04d) {
            score += 0.18d;
        }
        if (ctx != null) {
            score += (1.0d - ctx.noiseScore) * 0.18d;
        }
        return clamp01(score);
    }

    private double calcColdRepairPower(
            int currentGap,
            double avgGap,
            double gapZ,
            double freqSlope,
            double returnZoneScore,
            PositionContext ctx) {
        double power = 0.0d;
        if (currentGap >= 8) {
            power += 0.16d;
        }
        if (gapZ > 0.40d) {
            power += Math.min(0.30d, gapZ * 0.16d);
        }
        if (returnZoneScore > 0.40d) {
            power += returnZoneScore * 0.30d;
        }
        if (freqSlope > -0.06d) {
            power += 0.12d;
        }
        if (ctx != null && ctx.noiseScore < 0.55d) {
            power += (0.55d - ctx.noiseScore) * 0.25d;
        }
        return clamp01(power);
    }

    private double calcBreakDepth(double f12, double f48, double f360, PositionContext ctx) {
        double breakDepth = 0.0d;
        if (f12 < f48) {
            breakDepth += (f48 - f12) * 3.0d;
        }
        if (f48 < f360) {
            breakDepth += (f360 - f48) * 2.0d;
        }
        if (ctx != null) {
            breakDepth += ctx.positionDrift * 0.30d;
            breakDepth += ctx.noiseScore * 0.20d;
        }
        return clamp01(breakDepth);
    }

    private double calcRecoverySpeed(
            double f12, double f24, double f48, int currentGap, double avgGap, double gapZ) {
        double speed = 0.0d;
        if (f12 > f24) {
            speed += (f12 - f24) * 4.0d;
        }
        if (f24 >= f48) {
            speed += 0.12d;
        }
        if (currentGap >= Math.max(5.0d, avgGap * 0.60d)) {
            speed += 0.12d;
        }
        if (gapZ > -0.30d && gapZ < 1.80d) {
            speed += 0.14d;
        }
        return clamp01(speed);
    }

    private double calcRecoveryQuality(
            double recoverySpeed,
            double returnZoneScore,
            double transitionLift,
            double falseRecoveryRisk,
            PositionContext ctx) {
        double quality = 0.0d;
        quality += recoverySpeed * 0.32d;
        quality += returnZoneScore * 0.28d;
        quality += transitionLift * 0.18d;
        quality += (1.0d - falseRecoveryRisk) * 0.16d;
        if (ctx != null) {
            quality += (1.0d - ctx.noiseScore) * 0.12d;
        }
        return clamp01(quality);
    }

    private double calcFalseRecoveryRisk(
            double recoverySpeed,
            double returnZoneScore,
            double hotExhaustion,
            double breakDepth,
            PositionContext ctx) {
        double risk = 0.0d;
        if (recoverySpeed > 0.20d && returnZoneScore < 0.25d) {
            risk += 0.24d;
        }
        risk += hotExhaustion * 0.28d;
        risk += breakDepth * 0.26d;
        if (ctx != null) {
            risk += ctx.noiseScore * 0.20d;
            risk += ctx.positionDrift * 0.16d;
        }
        return clamp01(risk);
    }

    private double calcPositionNeedScore(
            double gapPressure,
            double coldRepairPower,
            double returnZoneScore,
            double positionStrengthSlope,
            double neighborPressure,
            double tailPressure,
            double stableHealth,
            double crossPositionSync,
            double timingScore,
            double positionCongestion) {
        double score = 0.0d;
        score += gapPressure * 0.18d;
        score += coldRepairPower * 0.16d;
        score += returnZoneScore * 0.14d;
        score += clamp01((positionStrengthSlope + 1.0d) / 2.0d) * 0.12d;
        score += neighborPressure * 0.08d;
        score += tailPressure * 0.08d;
        score += stableHealth * 0.12d;
        score += crossPositionSync * 0.06d;
        score += timingScore * 0.08d;
        score -= positionCongestion * 0.06d;
        return clamp01(score);
    }

    private double calcNumberSupplyScore(
            double freqSlope,
            double trendConsistency,
            double coldRepairPower,
            double returnZoneScore,
            double transitionLift,
            double pairCoSupport,
            double densityScore,
            double repeatScore,
            double neighborSupport,
            double tailSupport,
            double recoveryQuality,
            double biasFitScore) {
        double score = 0.0d;
        score += clamp01((freqSlope + 1.0d) / 2.0d) * 0.10d;
        score += trendConsistency * 0.08d;
        score += coldRepairPower * 0.13d;
        score += returnZoneScore * 0.12d;
        score += transitionLift * 0.12d;
        score += pairCoSupport * 0.08d;
        score += densityScore * 0.08d;
        score += repeatScore * 0.06d;
        score += neighborSupport * 0.07d;
        score += tailSupport * 0.05d;
        score += recoveryQuality * 0.07d;
        score += biasFitScore * 0.04d;
        return clamp01(score);
    }

    private double calcEvidenceScore(
            double positionNeedScore,
            double numberSupplyScore,
            double trendConsistency,
            double transitionLift,
            double pairCoSupport,
            double recoveryQuality,
            double neighborSupport,
            double tailSupport,
            double densityScore,
            double timingScore) {
        double score = 0.0d;
        score += positionNeedScore * 0.24d;
        score += numberSupplyScore * 0.30d;
        score += trendConsistency * 0.07d;
        score += transitionLift * 0.08d;
        score += pairCoSupport * 0.06d;
        score += recoveryQuality * 0.08d;
        score += neighborSupport * 0.05d;
        score += tailSupport * 0.04d;
        score += densityScore * 0.04d;
        score += timingScore * 0.04d;
        return clamp01(score);
    }

    private double calcAntiEvidenceScore(
            double hotExhaustion,
            double transitionConflict,
            double pairConflict,
            double breakDepth,
            double falseRecoveryRisk,
            double positionNoiseScore,
            double positionDrift,
            double crossPositionCrowd,
            double densityRisk,
            double repeatRisk,
            double recentLosePressure,
            BoardContext board) {
        double risk = 0.0d;
        risk += hotExhaustion * 0.10d;
        risk += transitionConflict * 0.12d;
        risk += pairConflict * 0.10d;
        risk += breakDepth * 0.10d;
        risk += falseRecoveryRisk * 0.14d;
        risk += positionNoiseScore * 0.12d;
        risk += positionDrift * 0.08d;
        risk += crossPositionCrowd * 0.08d;
        risk += densityRisk * 0.08d;
        risk += repeatRisk * 0.06d;
        risk += recentLosePressure * 0.06d;
        if (board != null) {
            risk += board.boardShock * 0.06d;
        }
        return clamp01(risk);
    }

    private double calcSampleReliability(
            int historySize, int transitionSample, int pairSample, PositionContext ctx) {
        double historyScore;
        if (historySize >= window360()) {
            historyScore = 1.0d;
        } else if (historySize <= 0) {
            historyScore = 0.0d;
        } else {
            historyScore = ((double) historySize) / ((double) window360());
        }
        double transitionScore =
                transitionSample <= 0 ? 0.0d : Math.min(1.0d, ((double) transitionSample) / 36.0d);
        double pairScore = pairSample <= 0 ? 0.0d : Math.min(1.0d, ((double) pairSample) / 50.0d);
        double stability = ctx == null ? 0.0d : (1.0d - ctx.noiseScore);
        return clamp01(
                historyScore * 0.58d
                        + transitionScore * 0.14d
                        + pairScore * 0.10d
                        + stability * 0.18d);
    }

    private double calcRecentValidity(
            double f12,
            double f24,
            double f48,
            double recentHitSupport,
            double recentLosePressure,
            double antiEvidenceScore) {
        double score = 0.45d;
        score += clampSigned((f12 - f48) * 4.0d) * 0.18d;
        score += clampSigned((f24 - f48) * 3.0d) * 0.12d;
        score += recentHitSupport * 0.14d;
        score -= recentLosePressure * 0.12d;
        score -= antiEvidenceScore * 0.10d;
        return clamp01(score);
    }

    private double calcConfidence(
            int historySize,
            double freq96,
            int transitionSample,
            int pairSample,
            PositionContext ctx,
            double sampleReliability,
            double recentValidity) {
        double sampleScore = sampleReliability;
        double transitionScore =
                transitionSample <= 0 ? 0.0d : Math.min(1.0d, ((double) transitionSample) / 40.0d);
        double pairScore = pairSample <= 0 ? 0.0d : Math.min(1.0d, ((double) pairSample) / 60.0d);
        double stability = ctx == null ? 0.0d : (1.0d - ctx.noiseScore);
        double active = freq96 > 0.0d ? 0.12d : 0.0d;
        return clamp01(
                sampleScore * 0.42d
                        + transitionScore * 0.13d
                        + pairScore * 0.10d
                        + stability * 0.14d
                        + recentValidity * 0.18d
                        + active);
    }

    private double entropy(int[] count, int total) {
        if (count == null || total <= 0) {
            return 1.0d;
        }
        double e = 0.0d;
        for (int n = NUMBER_MIN; n <= NUMBER_MAX; n++) {
            if (count[n] <= 0) {
                continue;
            }
            double p = ((double) count[n]) / ((double) total);
            e -= p * Math.log(p);
        }
        double max = Math.log(10.0d);
        if (max <= 0.0d) {
            return 1.0d;
        }
        return clamp01(e / max);
    }

    private double topMass(int[] count, int total, int topN) {
        if (count == null || total <= 0 || topN <= 0) {
            return 0.0d;
        }
        ArrayList<Integer> list = new ArrayList<Integer>();
        for (int n = NUMBER_MIN; n <= NUMBER_MAX; n++) {
            list.add(Integer.valueOf(count[n]));
        }
        Collections.sort(
                list,
                new Comparator<Integer>() {
                    public int compare(Integer a, Integer b) {
                        return b.intValue() - a.intValue();
                    }
                });
        int sum = 0;
        int max = Math.min(topN, list.size());
        for (int i = 0; i < max; i++) {
            sum += list.get(i).intValue();
        }
        return safeRate(sum, total);
    }

    private double distributionDrift(
            int[] shortCount, int shortTotal, int[] longCount, int longTotal) {
        if (shortCount == null || longCount == null || shortTotal <= 0 || longTotal <= 0) {
            return 1.0d;
        }
        double sum = 0.0d;
        for (int n = NUMBER_MIN; n <= NUMBER_MAX; n++) {
            double a = ((double) shortCount[n]) / ((double) shortTotal);
            double b = ((double) longCount[n]) / ((double) longTotal);
            sum += Math.abs(a - b);
        }
        return clamp01(sum / 2.0d);
    }

    private double calcPositionNoise(PositionContext ctx) {
        if (ctx == null) {
            return 1.0d;
        }
        double noise = 0.0d;
        noise += ctx.entropy96 * 0.28d;
        noise += ctx.positionDrift * 0.32d;
        noise += ctx.congestionScore * 0.14d;
        if (ctx.top3Mass96 < 0.30d) {
            noise += 0.12d;
        } else {
            noise += (1.0d - ctx.top3Mass96) * 0.10d;
        }
        if (ctx.strengthSlope < -0.10d) {
            noise += Math.abs(ctx.strengthSlope) * 0.10d;
        }
        return clamp01(noise);
    }

    private double calcPositionHealth(PositionContext ctx) {
        if (ctx == null) {
            return 0.0d;
        }
        double health = 0.50d;
        health += ctx.top3Mass96 * 0.14d;
        health += Math.max(0.0d, ctx.strengthSlope) * 0.14d;
        health += (1.0d - ctx.positionDrift) * 0.17d;
        health += (1.0d - ctx.noiseScore) * 0.18d;
        health -= ctx.entropy96 * 0.06d;
        health -= ctx.congestionScore * 0.06d;
        return clamp01(health);
    }

    private int getNumber(Models.Draw d, int position) {
        if (d == null || !isValidPosition(position)) {
            return 0;
        }
        try {
            if (d.numbers == null || d.numbers.length < POSITION_MAX) {
                return 0;
            }
            return d.numbers[position - 1];
        } catch (Throwable e) {
            return 0;
        }
    }

    private boolean isValidDraw(Models.Draw d) {
        if (d == null) {
            return false;
        }
        try {
            if (d.numbers == null || d.numbers.length < POSITION_MAX) {
                return false;
            }
            for (int p = POSITION_MIN; p <= POSITION_MAX; p++) {
                if (!isValidNumber(d.numbers[p - 1])) {
                    return false;
                }
            }
            return true;
        } catch (Throwable e) {
            return false;
        }
    }

    private boolean isValidPosition(int position) {
        return position >= POSITION_MIN && position <= POSITION_MAX;
    }

    private boolean isValidNumber(int number) {
        return number >= NUMBER_MIN && number <= NUMBER_MAX;
    }

    private double safeRate(int count, int total) {
        if (total <= 0) {
            return 0.0d;
        }
        return clamp01(((double) count) / ((double) total));
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

    private String buildWindowStateKey(
            int position,
            int number,
            String hotColdBucket,
            String omissionBucket,
            String densityBucket,
            String repeatBucket,
            String parityBucket,
            String neighborTailBucket,
            String crowdBucket,
            String syncBucket,
            String loseBucket,
            String timingBucket) {
        StringBuilder sb = new StringBuilder();
        sb.append("P").append(position);
        sb.append("_N").append(number);
        sb.append("_W12_24_48_96_360");
        sb.append("_").append(safeString(hotColdBucket));
        sb.append("_").append(safeString(omissionBucket));
        sb.append("_").append(safeString(densityBucket));
        sb.append("_").append(safeString(repeatBucket));
        sb.append("_").append(safeString(parityBucket));
        sb.append("_").append(safeString(neighborTailBucket));
        sb.append("_").append(safeString(crowdBucket));
        sb.append("_").append(safeString(syncBucket));
        sb.append("_").append(safeString(loseBucket));
        sb.append("_").append(safeString(timingBucket));
        return compactKey(sb.toString());
    }

    private String buildClusterKey(
            String hotColdBucket,
            String omissionBucket,
            String densityBucket,
            String repeatBucket,
            String parityBucket,
            String neighborTailBucket,
            String crowdBucket,
            String syncBucket,
            String timingBucket,
            String riskBucket) {
        return compactKey(
                "CL_"
                        + safeString(hotColdBucket)
                        + "_"
                        + safeString(omissionBucket)
                        + "_"
                        + safeString(densityBucket)
                        + "_"
                        + safeString(repeatBucket)
                        + "_"
                        + safeString(parityBucket)
                        + "_"
                        + safeString(neighborTailBucket)
                        + "_"
                        + safeString(crowdBucket)
                        + "_"
                        + safeString(syncBucket)
                        + "_"
                        + safeString(timingBucket)
                        + "_"
                        + safeString(riskBucket));
    }

    private String buildFactorKey(
            int position,
            int number,
            double positionNeedScore,
            double numberSupplyScore,
            double evidenceScore,
            double antiEvidenceScore,
            double sampleReliability,
            double recentValidity) {
        return compactKey(
                "F_P"
                        + position
                        + "_N"
                        + number
                        + "_NEED_"
                        + levelOf(positionNeedScore)
                        + "_SUP_"
                        + levelOf(numberSupplyScore)
                        + "_EV_"
                        + levelOf(evidenceScore)
                        + "_ANTI_"
                        + levelOf(antiEvidenceScore)
                        + "_SAMP_"
                        + levelOf(sampleReliability)
                        + "_REC_"
                        + levelOf(recentValidity));
    }

    private String buildActionAdvice(
            double evidenceScore,
            double antiEvidenceScore,
            double positionNeedScore,
            double numberSupplyScore,
            double sampleReliability,
            double recentValidity,
            double riskScore) {
        if (riskScore >= 0.72d || antiEvidenceScore >= 0.70d) {
            return "RISK：反证强，后续只允许进入风控/冻结检查，不得由本文件出手";
        }
        if (evidenceScore >= 0.62d
                && antiEvidenceScore <= 0.42d
                && sampleReliability >= 0.48d
                && recentValidity >= 0.46d) {
            return "PASS_FEATURE：位置需求和号码供给匹配，交给后续概率/动态门/DecisionGate复核";
        }
        if (positionNeedScore >= 0.55d
                && numberSupplyScore >= 0.52d
                && antiEvidenceScore <= 0.56d) {
            return "WATCH_STRONG：可进入候选池和SHADOW验证，是否BET仍由DecisionGate决定";
        }
        return "WATCH：证据不足或样本不足，仅作为候选排序输入";
    }

    private String buildSummaryText(
            int position,
            int number,
            double freq12,
            double freq24,
            double freq48,
            double freq96,
            double freq360,
            double freqSlope,
            int currentGap,
            double avgGap,
            double gapZ,
            DensityProfile density,
            RepeatProfile repeat,
            BiasProfile bias,
            RelationProfile relation,
            TransitionProfile transition,
            double recoveryQuality,
            double falseRecoveryRisk,
            double evidenceScore,
            double antiEvidenceScore,
            double netEvidenceScore,
            double sampleReliability,
            double recentValidity,
            double healthScore,
            double candidatePrior,
            String actionAdvice) {
        StringBuilder sb = new StringBuilder();
        sb.append("开奖前特征实体化｜P=").append(position).append("｜N=").append(number);
        sb.append("｜W12=").append(percent(freq12));
        sb.append("｜W24=").append(percent(freq24));
        sb.append("｜W48=").append(percent(freq48));
        sb.append("｜W96=").append(percent(freq96));
        sb.append("｜W360=").append(percent(freq360));
        sb.append("｜斜率=").append(signed(freqSlope));
        sb.append("｜遗漏=")
                .append(currentGap)
                .append("/均")
                .append(format(avgGap))
                .append("/Z")
                .append(format(gapZ));
        sb.append("｜密集=").append(percent(density == null ? 0.0d : density.densityScore));
        sb.append("｜重复=").append(percent(repeat == null ? 0.0d : repeat.repeatScore));
        sb.append("｜单双偏=").append(signed(bias == null ? 0.0d : bias.oddEvenBias));
        sb.append("｜大小偏=").append(signed(bias == null ? 0.0d : bias.highLowBias));
        sb.append("｜邻号支撑=").append(percent(relation == null ? 0.0d : relation.neighborSupport));
        sb.append("｜同尾支撑=").append(percent(relation == null ? 0.0d : relation.tailSupport));
        sb.append("｜跨位同步=").append(percent(relation == null ? 0.0d : relation.crossPositionSync));
        sb.append("｜转移支撑=").append(percent(transition == null ? 0.0d : transition.transitionLift));
        sb.append("｜恢复质量=").append(percent(recoveryQuality));
        sb.append("｜伪恢复风险=").append(percent(falseRecoveryRisk));
        sb.append("｜证据=").append(percent(evidenceScore));
        sb.append("｜反证=").append(percent(antiEvidenceScore));
        sb.append("｜净证据=").append(signed(netEvidenceScore));
        sb.append("｜样本可信=").append(percent(sampleReliability));
        sb.append("｜近期有效=").append(percent(recentValidity));
        sb.append("｜健康=").append(percent(healthScore));
        sb.append("｜先验=").append(percent(candidatePrior));
        sb.append("｜建议=").append(actionAdvice);
        sb.append("｜红线=本文件只生成开奖前特征，不出号、不结算、不创建BET、不改门槛");
        return sb.toString();
    }

    private String bucketHotCold(double freqSlope, double coldRepairPower, double hotPressure) {
        if (coldRepairPower >= 0.56d && freqSlope >= -0.08d) return "HC_COLD_REPAIR_STRONG";
        if (coldRepairPower >= 0.38d) return "HC_COLD_REPAIR";
        if (hotPressure >= 0.56d && freqSlope >= 0.04d) return "HC_HOT_CONTINUE_STRONG";
        if (hotPressure >= 0.36d) return "HC_HOT_CONTINUE";
        if (freqSlope >= 0.10d) return "HC_UP";
        if (freqSlope <= -0.10d) return "HC_DOWN";
        return "HC_BAL";
    }

    private String bucketOmission(int gap, double avgGap, double gapZ) {
        if (gap <= 2) return "MISS_0_2";
        if (gap <= 5) return "MISS_3_5";
        if (gapZ >= 1.80d) return "MISS_OVERDUE";
        if (gapZ >= 0.45d) return "MISS_RETURN";
        if (gap > Math.max(18.0d, avgGap * 2.2d)) return "MISS_LONG";
        return "MISS_MID";
    }

    private String bucketDensity(double densityScore, double densityRisk) {
        if (densityRisk >= 0.62d) return "DEN_RISK_HIGH";
        if (densityScore >= 0.62d) return "DEN_DENSE_STRONG";
        if (densityScore >= 0.38d) return "DEN_DENSE_MID";
        return "DEN_LIGHT";
    }

    private String bucketRepeat(double repeatScore, double repeatRisk) {
        if (repeatRisk >= 0.48d) return "REP_RISK";
        if (repeatScore >= 0.45d) return "REP_SUPPORT";
        return "REP_LOW";
    }

    private String bucketParity(double oddEvenBias, double highLowBias) {
        String a = Math.abs(oddEvenBias) >= 0.22d ? "PAR_BIAS" : "PAR_BAL";
        String b = Math.abs(highLowBias) >= 0.22d ? "BS_BIAS" : "BS_BAL";
        return a + "_" + b;
    }

    private String bucketNeighborTail(
            double neighborSupport,
            double tailSupport,
            double neighborPressure,
            double tailPressure) {
        if (neighborSupport >= 0.45d || tailSupport >= 0.45d) return "NT_SUPPORT_HIGH";
        if (neighborPressure >= 0.45d || tailPressure >= 0.45d) return "NT_REPAIR";
        return "NT_MID";
    }

    private String bucketCrowd(double congestion, double crossCrowd) {
        double v = Math.max(congestion, crossCrowd);
        if (v >= 0.65d) return "CROWD_HIGH";
        if (v >= 0.38d) return "CROWD_MID";
        return "CROWD_TINY";
    }

    private String bucketSync(double sync) {
        if (sync >= 0.62d) return "SYNC_S_HIGH";
        if (sync >= 0.34d) return "SYNC_S_MID";
        return "SYNC_S_LOW";
    }

    private String bucketTiming(double timingScore, double timingRisk) {
        if (timingRisk >= 0.55d) return "TIME_T_RISK";
        if (timingScore >= 0.55d) return "TIME_T_READY";
        return "TIME_T_LOW";
    }

    private String bucketRisk(double anti, double noise) {
        double r = Math.max(anti, noise);
        if (r >= 0.70d) return "RISK_HIGH";
        if (r >= 0.45d) return "RISK_MID";
        if (r >= 0.25d) return "RISK_LOW";
        return "RISK_CLEAN";
    }

    private String bucketHealth(double v) {
        if (v >= 0.72d) return "HEALTH_A";
        if (v >= 0.60d) return "HEALTH_B";
        if (v >= 0.48d) return "HEALTH_C";
        return "HEALTH_RISK";
    }

    private String bucketSigned(String prefix, double v) {
        if (prefix == null || prefix.length() == 0) {
            prefix = "V";
        }
        if (v >= 0.18d) return prefix + "_UP_STRONG";
        if (v >= 0.06d) return prefix + "_UP";
        if (v <= -0.18d) return prefix + "_DOWN_STRONG";
        if (v <= -0.06d) return prefix + "_DOWN";
        return prefix + "_FLAT";
    }

    private String levelOf(double v) {
        if (v >= 0.70d) return "HIGH";
        if (v >= 0.45d) return "MID";
        if (v >= 0.20d) return "LOW";
        return "CLEAN";
    }

    private String statusOf(
            double healthScore, double riskScore, double evidenceScore, double antiEvidenceScore) {
        if (riskScore >= 0.72d || antiEvidenceScore >= 0.72d) return "RISK";
        if (healthScore >= 0.62d && evidenceScore > antiEvidenceScore) return "HEALTHY";
        if (evidenceScore >= 0.50d && antiEvidenceScore <= 0.56d) return "WATCH_STRONG";
        return "NORMAL";
    }

    private String percent(double v) {
        return String.format(Locale.US, "%.1f%%", Double.valueOf(clamp01(v) * 100.0d));
    }

    private String signed(double v) {
        String sign = v >= 0.0d ? "+" : "";
        return sign + String.format(Locale.US, "%.3f", Double.valueOf(v));
    }

    private String format(double v) {
        return String.format(Locale.US, "%.2f", Double.valueOf(v));
    }

    private static int compareIssue(String a, String b) {
        if (a == null) a = "";
        if (b == null) b = "";
        try {
            long la = Long.parseLong(a);
            long lb = Long.parseLong(b);
            if (la < lb) return -1;
            if (la > lb) return 1;
            return 0;
        } catch (Throwable e) {
            return a.compareTo(b);
        }
    }

    private String normalizeBaseIssue(String baseIssue, ArrayList<Models.Draw> visible) {
        if (baseIssue != null && baseIssue.length() > 0) {
            return baseIssue;
        }
        if (visible != null && visible.size() > 0) {
            Models.Draw last = visible.get(visible.size() - 1);
            return last == null ? "" : safeString(last.issue);
        }
        return "";
    }

    private String normalizeTargetIssue(String targetIssue, String baseIssue) {
        if (targetIssue != null && targetIssue.length() > 0) {
            return targetIssue;
        }
        return buildNextIssue(baseIssue);
    }

    private String inferBaseIssue(List<Models.Draw> history) {
        if (history == null || history.size() == 0) {
            return "";
        }
        for (int i = history.size() - 1; i >= 0; i--) {
            Models.Draw d = history.get(i);
            if (d != null && d.issue != null && d.issue.length() > 0) {
                return d.issue;
            }
        }
        return "";
    }

    private String buildNextIssue(String issue) {
        if (issue == null || issue.length() == 0) {
            return "";
        }
        try {
            long v = Long.parseLong(issue);
            return String.valueOf(v + 1L);
        } catch (Throwable e) {
            return issue + "_NEXT";
        }
    }

    private static String safeString(String s) {
        return s == null ? "" : s;
    }

    private String compactKey(String s) {
        if (s == null) return "";
        String out =
                s.replace('|', '_')
                        .replace(' ', '_')
                        .replace(':', '_')
                        .replace('，', '_')
                        .replace('｜', '_');
        while (out.indexOf("__") >= 0) {
            out = out.replace("__", "_");
        }
        if (out.length() > 180) {
            out = out.substring(0, 180);
        }
        return out;
    }

    private static int window12() {
        return readAppInt("WINDOW_12", 12);
    }

    private static int window24() {
        return readAppInt("WINDOW_24", 24);
    }

    private static int window48() {
        return readAppInt("WINDOW_48", 48);
    }

    private static int window96() {
        return readAppInt("WINDOW_96", 96);
    }

    private static int window360() {
        return readAppInt("WINDOW_360", 360);
    }

    private static int window240Compat() {
        return readAppInt("WINDOW_240", 240);
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

    private static Field cachedField(Class<?> type, String fieldName) {
        if (type == null || fieldName == null || fieldName.length() == 0) return null;
        String key = type.getName() + "#" + fieldName;
        synchronized (FIELD_CACHE_LOCK) {
            if (FIELD_MISS_CACHE.contains(key)) return null;
            Field f = FIELD_CACHE.get(key);
            if (f != null) return f;
            try {
                f = type.getField(fieldName);
                FIELD_CACHE.put(key, f);
                return f;
            } catch (Throwable e) {
                FIELD_MISS_CACHE.add(key);
                return null;
            }
        }
    }

    private static void putString(Object obj, String fieldName, String value) {
        if (obj == null) return;
        try {
            Field f = cachedField(obj.getClass(), fieldName);
            if (f != null) f.set(obj, value == null ? "" : value);
        } catch (Throwable ignored) {
        }
    }

    private static void putInt(Object obj, String fieldName, int value) {
        if (obj == null) return;
        try {
            Field f = cachedField(obj.getClass(), fieldName);
            if (f == null) return;
            if (f.getType() == Integer.TYPE) f.setInt(obj, value);
            else f.set(obj, Integer.valueOf(value));
        } catch (Throwable ignored) {
        }
    }

    private static void putLong(Object obj, String fieldName, long value) {
        if (obj == null) return;
        try {
            Field f = cachedField(obj.getClass(), fieldName);
            if (f == null) return;
            if (f.getType() == Long.TYPE) f.setLong(obj, value);
            else f.set(obj, Long.valueOf(value));
        } catch (Throwable ignored) {
        }
    }

    private static void putDouble(Object obj, String fieldName, double value) {
        if (obj == null) return;
        try {
            Field f = cachedField(obj.getClass(), fieldName);
            if (f == null) return;
            if (f.getType() == Double.TYPE) f.setDouble(obj, value);
            else f.set(obj, Double.valueOf(value));
        } catch (Throwable ignored) {
        }
    }

    private static int getIntField(Object obj, String fieldName, int fallback) {
        if (obj == null) return fallback;
        try {
            Field f = cachedField(obj.getClass(), fieldName);
            if (f == null) return fallback;
            Object v = f.get(obj);
            if (v instanceof Number) return ((Number) v).intValue();
            if (v != null) return Integer.parseInt(String.valueOf(v));
        } catch (Throwable ignored) {
        }
        return fallback;
    }

    private static double getDoubleField(Object obj, String fieldName, double fallback) {
        if (obj == null) return fallback;
        try {
            Field f = cachedField(obj.getClass(), fieldName);
            if (f == null) return fallback;
            Object v = f.get(obj);
            if (v instanceof Number) return ((Number) v).doubleValue();
            if (v != null) return Double.parseDouble(String.valueOf(v));
        } catch (Throwable ignored) {
        }
        return fallback;
    }

    private static String getStringField(Object obj, String fieldName, String fallback) {
        if (obj == null) return fallback == null ? "" : fallback;
        try {
            Field f = cachedField(obj.getClass(), fieldName);
            if (f == null) return fallback == null ? "" : fallback;
            Object v = f.get(obj);
            return v == null ? (fallback == null ? "" : fallback) : String.valueOf(v);
        } catch (Throwable ignored) {
        }
        return fallback == null ? "" : fallback;
    }

    private static final class PositionContext {
        int position;
        int size12;
        int size24;
        int size48;
        int size96;
        int size360;
        double entropy12;
        double entropy24;
        double entropy48;
        double entropy96;
        double entropy360;
        double top3Mass12;
        double top3Mass24;
        double top3Mass48;
        double top3Mass96;
        double top3Mass360;
        double strengthSlope;
        double positionDrift;
        double noiseScore;
        double healthScore;
        double congestionScore;
    }

    private static final class BoardContext {
        int historySize;
        double boardEntropy;
        double boardDrift;
        double boardNoise;
        double boardHealth;
        double boardStrengthSlope;
        double boardCrowd;
        double boardShock;
    }

    private static final class GapProfile {
        int currentGap;
        double avgGap;
        double gapStd;
        double gapZ;
        double gapPhase;
    }

    private static final class DensityProfile {
        int hitCount12;
        int hitCount24;
        double densityScore;
        double densityRisk;
    }

    private static final class RepeatProfile {
        int repeatCount;
        double repeatScore;
        double repeatRisk;
    }

    private static final class BiasProfile {
        double oddEvenBias;
        double highLowBias;
        double biasFitScore;
    }

    private static final class RelationProfile {
        double tailSupport;
        double neighborSupport;
        double tailPressure;
        double neighborPressure;
        double sameNumberCrowd;
        double crossPositionSync;
        double crossPositionCrowd;
    }

    private static final class TransitionProfile {
        int sample;
        double conditionalRate;
        double baselineRate;
        double transitionLift;
        double transitionConflict;
    }

    private static final class PairProfile {
        int sample;
        double conditionalRate;
        double pairCoSupport;
        double pairConflict;
    }

    private static final class StreakProfile {
        String loseBucket;
        double recentLosePressure;
        double recentHitSupport;
    }

    private static final class TimingProfile {
        double timingOffset;
        double timingScore;
        double timingRisk;
    }

    public int validateFeatureMap(Map<String, Models.PreDrawFeature> map) {
        if (map == null) return 0;
        int ok = 0;
        for (int p = POSITION_MIN; p <= POSITION_MAX; p++) {
            for (int n = NUMBER_MIN; n <= NUMBER_MAX; n++) {
                Models.PreDrawFeature f = map.get(keyOf(p, n));
                if (f != null
                        && f.stateKey != null
                        && f.stateKey.length() > 0
                        && f.clusterKey != null
                        && f.clusterKey.length() > 0
                        && f.factorKey != null
                        && f.factorKey.length() > 0
                        && f.position == p
                        && f.number == n) ok++;
            }
        }
        return ok;
    }
}
