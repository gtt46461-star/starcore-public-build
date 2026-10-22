package com.ethk3.novaonepro;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * V20 第8/14：CounterfactualReasonEngine
 *
 * <p>目标： 把“为什么是这个位置、为什么是这三个号码、为什么不是其他位置、 为什么不是其他号码、哪些原因是假因、未中到底错在哪里”做成 可计算、可解释、可保存、可回放的反事实系统。
 *
 * <p>红线： - 不创建 Trade。 - 不结算。 - 不写真账本。 - 不改真实盈亏。 - 不绕过 DecisionGateEngine。 - 只输出证据、解释、字段桥和样本。
 */
public final class CounterfactualReasonEngine {
    public static final String OUMAN_GALAXY_REBUILD_VERSION =
            AppConfig.OUMAN_GALAXY_REBUILD_VERSION;

    public static final String VERSION = "V44_NOVA_DIAGNOSIS_FREQUENCY_STABILITY_20260720_44157296";

    private static final int MAX_POSITION = 10;
    private static final int MIN_NUMBER = 1;
    private static final int MAX_NUMBER = 10;
    private static final double K3_BASELINE = 0.300d;
    private static final double NUMBER_BASELINE = 0.100d;
    private static final double READY_RANK_SCORE = 0.540d;
    private static final double HARD_WHY_NOT_RISK = 0.680d;
    private static final double FALSE_CAUSE_DROP_LINE = 0.035d;

    private final DbStore store;

    public CounterfactualReasonEngine() {
        this(null);
    }

    public CounterfactualReasonEngine(DbStore s) {
        store = s;
    }

    public String getVersion() {
        return VERSION;
    }

    public CounterfactualBundle analyzeAndApply(
            Models.ComboCandidate c,
            List<Models.Draw> history,
            String baseIssue,
            String targetIssue) {
        if (c == null) {
            return CounterfactualBundle.empty("EMPTY_CANDIDATE");
        }
        ArrayList<Models.Draw> clean = cleanHistory(history);
        CounterfactualBundle b = analyzeCandidate(c, clean, baseIssue, targetIssue);
        applyBundleToCandidate(c, b);
        saveBundle(c, b, false, baseIssue);
        return b;
    }

    public ArrayList<CounterfactualBundle> analyzeCandidates(
            List<Models.ComboCandidate> candidates,
            List<Models.Draw> history,
            String baseIssue,
            String targetIssue) {
        ArrayList<CounterfactualBundle> out = new ArrayList<CounterfactualBundle>();
        if (candidates == null || candidates.size() == 0) {
            return out;
        }
        ArrayList<Models.Draw> clean = cleanHistory(history);
        ArrayList<PositionCounterState> positions = buildPositionCounterStates(clean);
        ArrayList<NumberCounterState> numbers = buildNumberCounterStates(clean);

        for (int i = 0; i < candidates.size(); i++) {
            Models.ComboCandidate c = candidates.get(i);
            CounterfactualBundle b =
                    analyzeCandidateWithStates(
                            c, clean, positions, numbers, baseIssue, targetIssue);
            applyBundleToCandidate(c, b);
            saveBundle(c, b, false, baseIssue);
            out.add(b);
        }
        sortBundles(out);
        return out;
    }

    public CounterfactualBundle analyzeCandidate(
            Models.ComboCandidate c,
            List<Models.Draw> clean,
            String baseIssue,
            String targetIssue) {
        ArrayList<PositionCounterState> positions = buildPositionCounterStates(clean);
        ArrayList<NumberCounterState> numbers = buildNumberCounterStates(clean);
        return analyzeCandidateWithStates(c, clean, positions, numbers, baseIssue, targetIssue);
    }

    public CounterfactualBundle analyzeCandidateWithStates(
            Models.ComboCandidate c,
            List<Models.Draw> clean,
            ArrayList<PositionCounterState> positions,
            ArrayList<NumberCounterState> numbers,
            String baseIssue,
            String targetIssue) {
        if (c == null) {
            return CounterfactualBundle.empty("EMPTY_CANDIDATE");
        }
        CounterfactualBundle b = new CounterfactualBundle();
        b.baseIssue = safe(baseIssue);
        b.targetIssue = firstNonEmpty(targetIssue, c.targetIssue);
        b.position = normalizePosition(c.position);
        b.n1 = normalizeNumber(c.n1);
        b.n2 = normalizeNumber(c.n2);
        b.n3 = normalizeNumber(c.n3);
        b.k3Mask = buildK3Mask(b.n1, b.n2, b.n3);
        b.positionState = getPositionState(positions, b.position);
        b.n1State = getNumberState(numbers, b.position, b.n1);
        b.n2State = getNumberState(numbers, b.position, b.n2);
        b.n3State = getNumberState(numbers, b.position, b.n3);
        b.positionRank = rankPosition(positions, b.position);
        b.k3Rank = rankK3(numbers, b.position, b.n1, b.n2, b.n3, c);
        b.altPosition = bestAlternativePosition(positions, b.position);
        b.altK3 = bestAlternativeK3(numbers, b.position, b.n1, b.n2, b.n3);
        b.mirrorMissSimilarity = mirrorMissSimilarity(b, clean);
        b.reason = buildCounterfactualReasonResult(b, c);
        b.ablation = buildCauseAblationResult(b, clean, c);
        b.missReason = buildProspectiveMissReason(c, b, 0, b.targetIssue);
        b.finalRankScore = finalRankScore(b, c);
        b.finalWhyNotRisk = finalWhyNotRisk(b, c);
        b.action = decideAction(b, c);
        b.summary = buildSummary(b, c);
        return b;
    }

    public Models.CounterfactualReasonResult buildCounterfactualReasonResult(
            CounterfactualBundle b, Models.ComboCandidate c) {
        Models.CounterfactualReasonResult r = new Models.CounterfactualReasonResult();
        if (b == null) {
            return r;
        }
        r.targetIssue = b.targetIssue;
        r.position = b.position;
        r.n1 = b.n1;
        r.n2 = b.n2;
        r.n3 = b.n3;
        r.k3Mask = b.k3Mask;
        r.positionRank = b.positionRank;
        r.k3Rank = b.k3Rank;
        r.rankScore = finalRankScore(b, c);
        r.whyNotRisk = finalWhyNotRisk(b, c);
        r.mirrorMissSimilarity = b.mirrorMissSimilarity;
        r.causeScore = causeScore(b, c);
        r.counterScore = r.whyNotRisk;

        r.whyThisPosition =
                "为什么是这个位置：P"
                        + b.position
                        + "｜位置排名="
                        + b.positionRank
                        + "｜需求分="
                        + percent(b.positionState == null ? 0.0d : b.positionState.needScore)
                        + "｜稳定分="
                        + percent(b.positionState == null ? 0.0d : b.positionState.stabilityScore)
                        + "｜反证="
                        + percent(b.positionState == null ? 0.0d : b.positionState.counterRisk);

        r.whyTheseNumbers =
                "为什么是这三个号码："
                        + AppConfig.normalizeK3Part(b.n1, b.n2, b.n3)
                        + "｜K3排名="
                        + b.k3Rank
                        + "｜N1供给="
                        + percent(b.n1State == null ? 0.0d : b.n1State.supplyScore)
                        + "｜N2供给="
                        + percent(b.n2State == null ? 0.0d : b.n2State.supplyScore)
                        + "｜N3供给="
                        + percent(b.n3State == null ? 0.0d : b.n3State.supplyScore)
                        + "｜NeedSupply="
                        + percent(c == null ? 0.0d : c.needSupplyMatchScore);

        r.whyNotOtherPosition =
                "为什么不是其他位置：最佳替代="
                        + (b.altPosition == null
                                ? "无"
                                : ("P"
                                        + b.altPosition.position
                                        + "/"
                                        + percent(b.altPosition.needScore)))
                        + "；若替代位置需求更强但反证也更强，则不抢位；单位置玩法禁止多位置扩散。";

        r.whyNotOtherNumbers =
                "为什么不是其他号码：最佳替代K3="
                        + (b.altK3 == null ? "无" : b.altK3.text)
                        + "；若替代号码供给不足、第三号拖拽或镜像未中相似度高，则不能替换当前K3。";

        r.summary =
                r.whyThisPosition
                        + "｜"
                        + r.whyTheseNumbers
                        + "｜whyNotRisk="
                        + percent(r.whyNotRisk)
                        + "｜mirrorMiss="
                        + percent(r.mirrorMissSimilarity)
                        + "｜只给DecisionGate证据，不直接BET";
        try {
            r.recalc();
        } catch (Throwable e) {
        }
        return r;
    }

    public Models.CauseAblationResult buildCauseAblationResult(
            CounterfactualBundle b, List<Models.Draw> clean, Models.ComboCandidate c) {
        Models.CauseAblationResult r = new Models.CauseAblationResult();
        if (b == null) {
            return r;
        }
        r.clusterId = c == null ? "" : safe(c.dynamicClusterId);
        r.sampleCount = clean == null ? 0 : Math.min(clean.size(), 96);
        r.omissionDrop = ablationDrop(b, c, "OMISSION");
        r.tailDrop = ablationDrop(b, c, "TAIL");
        r.neighborDrop = ablationDrop(b, c, "NEIGHBOR");
        r.crossPositionDrop = ablationDrop(b, c, "CROSS");
        r.needSupplyDrop = ablationDrop(b, c, "NEED_SUPPLY");
        r.thirdQualityDrop = ablationDrop(b, c, "THIRD");

        r.falseCauseCount = 0;
        if (r.omissionDrop < FALSE_CAUSE_DROP_LINE) r.falseCauseCount++;
        if (r.tailDrop < FALSE_CAUSE_DROP_LINE) r.falseCauseCount++;
        if (r.neighborDrop < FALSE_CAUSE_DROP_LINE) r.falseCauseCount++;
        if (r.crossPositionDrop < FALSE_CAUSE_DROP_LINE) r.falseCauseCount++;
        if (r.needSupplyDrop < FALSE_CAUSE_DROP_LINE) r.falseCauseCount++;
        if (r.thirdQualityDrop < FALSE_CAUSE_DROP_LINE) r.falseCauseCount++;

        r.falseCauseSummary =
                "因子剥离｜遗漏drop="
                        + percent(r.omissionDrop)
                        + "｜同尾drop="
                        + percent(r.tailDrop)
                        + "｜邻号drop="
                        + percent(r.neighborDrop)
                        + "｜跨位drop="
                        + percent(r.crossPositionDrop)
                        + "｜NeedSupplyDrop="
                        + percent(r.needSupplyDrop)
                        + "｜第三号drop="
                        + percent(r.thirdQualityDrop)
                        + "｜疑似假因="
                        + r.falseCauseCount;
        r.summary = r.falseCauseSummary;
        try {
            r.recalc();
        } catch (Throwable e) {
        }
        return r;
    }

    public Models.MissReasonResult buildProspectiveMissReason(
            Models.ComboCandidate c, CounterfactualBundle b, int actualNumber, String resultIssue) {
        Models.MissReasonResult m = new Models.MissReasonResult();
        if (c == null && b == null) {
            return m;
        }
        m.targetIssue = b == null ? "" : b.targetIssue;
        if (resultIssue != null && resultIssue.length() > 0) {
            m.targetIssue = resultIssue;
        }
        m.position = c == null ? b.position : c.position;
        m.n1 = c == null ? b.n1 : c.n1;
        m.n2 = c == null ? b.n2 : c.n2;
        m.n3 = c == null ? b.n3 : c.n3;
        m.k3Mask = buildK3Mask(m.n1, m.n2, m.n3);
        m.actualNumber = actualNumber;
        m.clusterId = c == null ? "" : safe(c.dynamicClusterId);

        double positionRisk =
                b == null || b.positionState == null ? 0.0d : b.positionState.counterRisk;
        double nRisk = avgNumberRisk(b);
        double thirdDrag =
                c == null
                        ? thirdDragFromBundle(b)
                        : Math.max(0.0d, 0.100d - c.needSupplyThirdQuality);
        double counterIgnored = b == null ? 0.0d : b.finalWhyNotRisk;

        m.windowWrongScore =
                c == null
                        ? 0.0d
                        : clamp01(
                                c.dynamicClusterCounterScore + c.dynamicClusterVolatility * 0.50d);
        m.positionWrongScore = clamp01(positionRisk);
        m.mainNumberWrongScore = clamp01(nRisk);
        m.thirdDragScore = clamp01(thirdDrag);
        m.counterIgnoredScore = clamp01(counterIgnored);
        m.missType = detectMissType(m);
        m.detail =
                "失败原因预判｜type="
                        + m.missType
                        + "｜窗口错="
                        + percent(m.windowWrongScore)
                        + "｜位置错="
                        + percent(m.positionWrongScore)
                        + "｜号码错="
                        + percent(m.mainNumberWrongScore)
                        + "｜第三号拖拽="
                        + percent(m.thirdDragScore)
                        + "｜反证忽略="
                        + percent(m.counterIgnoredScore);
        m.nextAdvice = nextAdvice(m);
        try {
            m.recalc();
        } catch (Throwable e) {
        }
        return m;
    }

    public Models.MissReasonResult updateAfterMiss(
            Models.ComboCandidate c,
            List<Models.Draw> history,
            int actualNumber,
            String resultIssue) {
        CounterfactualBundle b = analyzeCandidate(c, cleanHistory(history), "", resultIssue);
        Models.MissReasonResult m = buildProspectiveMissReason(c, b, actualNumber, resultIssue);
        if (store != null) {
            try {
                store.saveMissReasonSample(m);
                store.saveCounterfactualReasonSample(
                        b.reason, c == null ? "" : c.dynamicClusterId, false);
                store.saveCauseAblationSample(b.ablation);
                store.updateDynamicCausalState(
                        AppConfig.STATE_COUNTERFACTUAL_LAST_RUN,
                        String.valueOf(System.currentTimeMillis()));
                store.updateDynamicCausalState(
                        AppConfig.STATE_DYNAMIC_CAUSAL_DIAGNOSIS,
                        "Counterfactual MISS｜" + m.detail);
            } catch (Throwable e) {
            }
        }
        return m;
    }

    private void applyBundleToCandidate(Models.ComboCandidate c, CounterfactualBundle b) {
        if (c == null || b == null) {
            return;
        }
        try {
            c.applyCounterfactualReason(b.reason);
        } catch (Throwable e) {
        }
        if (b.ablation != null) {
            c.causeAblationKey = b.ablation.ablationKey;
            c.causeAblationScore = b.ablation.totalAblationScore;
            c.causeAblationFalseCauseCount = b.ablation.falseCauseCount;
            c.causeAblationSummary = b.ablation.summary;
        }
        c.missReasonType = b.missReason == null ? "" : b.missReason.missType;
        c.missReasonMatrixKey = b.missReason == null ? "" : b.missReason.missKey;
        c.missReasonDetail = b.missReason == null ? "" : b.missReason.detail;
        String why = buildCandidateWhy(b, c);
        if (AppConfig.adaptiveActionAllowsBet(b.action)) {
            c.whyBet = why;
            c.whyNoBet = "";
        } else {
            c.whyBet = "";
            c.whyNoBet = why;
        }
        c.dynamicCausalSummary = why;
        try {
            Models.normalizeDynamicCausalBridgeFields(c);
        } catch (Throwable e) {
        }
    }

    private void saveBundle(
            Models.ComboCandidate c, CounterfactualBundle b, boolean wouldHit, String baseIssue) {
        if (store == null || b == null) {
            return;
        }
        try {
            store.saveCounterfactualReasonSample(
                    b.reason, c == null ? "" : c.dynamicClusterId, wouldHit);
            store.saveCauseAblationSample(b.ablation);
            if (b.missReason != null) {
                store.saveMissReasonSample(b.missReason);
            }
            store.updateDynamicCausalState(
                    AppConfig.STATE_COUNTERFACTUAL_LAST_RUN,
                    String.valueOf(System.currentTimeMillis()));
            store.updateDynamicCausalState(
                    AppConfig.STATE_DYNAMIC_CAUSAL_DIAGNOSIS,
                    "CounterfactualReasonEngine｜" + b.summary);
        } catch (Throwable e) {
        }
    }

    private ArrayList<PositionCounterState> buildPositionCounterStates(List<Models.Draw> clean) {
        ArrayList<PositionCounterState> list = new ArrayList<PositionCounterState>();
        for (int p = 1; p <= MAX_POSITION; p++) {
            PositionCounterState s = new PositionCounterState();
            s.position = p;
            s.rate12 = positionRate(p, clean, 12);
            s.rate24 = positionRate(p, clean, 24);
            s.rate48 = positionRate(p, clean, 48);
            s.rate96 = positionRate(p, clean, 96);
            s.omissionPressure = positionOmissionPressure(p, clean);
            s.stabilityScore = positionStability(p, clean, 48);
            s.counterRisk = positionCounterRisk(s);
            s.needScore =
                    clamp01(
                            Math.max(0.0d, s.rate24 - K3_BASELINE) * 0.70d
                                    + Math.max(0.0d, K3_BASELINE - s.rate96) * 0.55d
                                    + s.omissionPressure * 0.22d
                                    + s.stabilityScore * 0.18d
                                    - s.counterRisk * 0.30d);
            list.add(s);
        }
        sortPositionStates(list);
        return list;
    }

    private ArrayList<NumberCounterState> buildNumberCounterStates(List<Models.Draw> clean) {
        ArrayList<NumberCounterState> list = new ArrayList<NumberCounterState>();
        for (int p = 1; p <= MAX_POSITION; p++) {
            for (int n = MIN_NUMBER; n <= MAX_NUMBER; n++) {
                NumberCounterState s = new NumberCounterState();
                s.position = p;
                s.number = n;
                s.rate12 = numberRate(p, n, clean, 12);
                s.rate24 = numberRate(p, n, clean, 24);
                s.rate48 = numberRate(p, n, clean, 48);
                s.rate96 = numberRate(p, n, clean, 96);
                s.omissionPressure = numberOmissionPressure(p, n, clean);
                s.neighborScore = numberNeighborScore(p, n, clean, 24);
                s.tailScore = numberTailScore(n, clean, 48);
                s.crossScore = numberCrossScore(n, clean, 24);
                s.counterRisk = numberCounterRisk(s);
                s.supplyScore =
                        clamp01(
                                Math.max(0.0d, s.rate24 - NUMBER_BASELINE) * 0.62d
                                        + Math.max(0.0d, NUMBER_BASELINE - s.rate96) * 0.48d
                                        + s.omissionPressure * 0.20d
                                        + s.neighborScore * 0.15d
                                        + s.tailScore * 0.10d
                                        + s.crossScore * 0.12d
                                        - s.counterRisk * 0.28d);
                list.add(s);
            }
        }
        sortNumberStates(list);
        return list;
    }

    private int rankPosition(ArrayList<PositionCounterState> states, int position) {
        if (states == null || states.size() == 0) {
            return normalizePosition(position);
        }
        for (int i = 0; i < states.size(); i++) {
            PositionCounterState s = states.get(i);
            if (s != null && s.position == position) {
                return i + 1;
            }
        }
        return MAX_POSITION;
    }

    private int rankK3(
            ArrayList<NumberCounterState> states,
            int position,
            int n1,
            int n2,
            int n3,
            Models.ComboCandidate c) {
        double score = k3Score(states, position, n1, n2, n3, c);
        if (score >= 0.760d) return 1;
        if (score >= 0.660d) return 2;
        if (score >= 0.560d) return 3;
        if (score >= 0.460d) return 5;
        if (score >= 0.360d) return 7;
        return 9;
    }

    private double k3Score(
            ArrayList<NumberCounterState> states,
            int position,
            int n1,
            int n2,
            int n3,
            Models.ComboCandidate c) {
        NumberCounterState a = getNumberState(states, position, n1);
        NumberCounterState b = getNumberState(states, position, n2);
        NumberCounterState d = getNumberState(states, position, n3);
        double supply = (supply(a) + supply(b) + supply(d)) / 3.0d;
        double risk = (risk(a) + risk(b) + risk(d)) / 3.0d;
        double needSupply = c == null ? 0.0d : c.needSupplyMatchScore;
        double third = c == null ? 0.0d : c.needSupplyThirdQuality;
        return clamp01(supply * 0.44d + needSupply * 0.28d + third * 0.10d + (1.0d - risk) * 0.18d);
    }

    private PositionCounterState bestAlternativePosition(
            ArrayList<PositionCounterState> states, int currentPosition) {
        if (states == null) {
            return null;
        }
        for (int i = 0; i < states.size(); i++) {
            PositionCounterState s = states.get(i);
            if (s != null && s.position != currentPosition) {
                return s;
            }
        }
        return null;
    }

    private AlternativeK3 bestAlternativeK3(
            ArrayList<NumberCounterState> states, int position, int n1, int n2, int n3) {
        ArrayList<NumberCounterState> samePosition = new ArrayList<NumberCounterState>();
        if (states != null) {
            for (int i = 0; i < states.size(); i++) {
                NumberCounterState s = states.get(i);
                if (s != null && s.position == position) {
                    samePosition.add(s);
                }
            }
        }
        sortNumberStates(samePosition);
        AlternativeK3 k = new AlternativeK3();
        int count = 0;
        for (int i = 0; i < samePosition.size() && count < 3; i++) {
            NumberCounterState s = samePosition.get(i);
            if (s == null) continue;
            if (s.number == n1 || s.number == n2 || s.number == n3) continue;
            if (count == 0) k.n1 = s.number;
            if (count == 1) k.n2 = s.number;
            if (count == 2) k.n3 = s.number;
            k.score += s.supplyScore;
            count++;
        }
        if (count <= 0) {
            k.text = "无";
            k.score = 0.0d;
        } else {
            k.score = clamp01(k.score / Math.max(1, count));
            k.text = AppConfig.normalizeK3Part(k.n1, k.n2, k.n3) + "/" + percent(k.score);
        }
        return k;
    }

    private double finalRankScore(CounterfactualBundle b, Models.ComboCandidate c) {
        if (b == null) {
            return 0.0d;
        }
        double pos = b.positionState == null ? 0.0d : b.positionState.needScore;
        double n = (supply(b.n1State) + supply(b.n2State) + supply(b.n3State)) / 3.0d;
        double k3 =
                b.k3Rank <= 1 ? 0.90d : (b.k3Rank <= 3 ? 0.72d : (b.k3Rank <= 5 ? 0.55d : 0.35d));
        double ns = c == null ? 0.0d : c.needSupplyMatchScore;
        double third = c == null ? 0.0d : c.needSupplyThirdQuality;
        return clamp01(pos * 0.22d + n * 0.26d + k3 * 0.18d + ns * 0.24d + third * 0.10d);
    }

    private double finalWhyNotRisk(CounterfactualBundle b, Models.ComboCandidate c) {
        if (b == null) {
            return 1.0d;
        }
        double altPositionPressure = 0.0d;
        if (b.altPosition != null && b.positionState != null) {
            altPositionPressure =
                    Math.max(0.0d, b.altPosition.needScore - b.positionState.needScore);
        }
        double altK3Pressure =
                b.altK3 == null ? 0.0d : Math.max(0.0d, b.altK3.score - k3ScoreLocal(b));
        double mirror = b.mirrorMissSimilarity;
        double supplyRisk = avgNumberRisk(b);
        double needRisk = c == null ? 0.0d : c.needSupplyRisk;
        double cf = c == null ? 0.0d : c.counterfactualWhyNotRisk;
        return clamp01(
                altPositionPressure * 0.22d
                        + altK3Pressure * 0.20d
                        + mirror * 0.22d
                        + supplyRisk * 0.16d
                        + needRisk * 0.12d
                        + cf * 0.08d);
    }

    private double causeScore(CounterfactualBundle b, Models.ComboCandidate c) {
        if (b == null) {
            return 0.0d;
        }
        double pos = b.positionState == null ? 0.0d : b.positionState.needScore;
        double k = k3ScoreLocal(b);
        double ns = c == null ? 0.0d : c.needSupplyMatchScore;
        return clamp01(
                pos * 0.30d + k * 0.34d + ns * 0.24d + (1.0d - b.mirrorMissSimilarity) * 0.12d);
    }

    private double k3ScoreLocal(CounterfactualBundle b) {
        if (b == null) {
            return 0.0d;
        }
        return clamp01((supply(b.n1State) + supply(b.n2State) + supply(b.n3State)) / 3.0d);
    }

    private double ablationDrop(CounterfactualBundle b, Models.ComboCandidate c, String factor) {
        if (b == null) {
            return 0.0d;
        }
        double before = causeScore(b, c);
        double after = before;
        if ("OMISSION".equals(factor)) {
            after -= avgOmission(b) * 0.090d;
        } else if ("TAIL".equals(factor)) {
            after -= avgTail(b) * 0.070d;
        } else if ("NEIGHBOR".equals(factor)) {
            after -= avgNeighbor(b) * 0.080d;
        } else if ("CROSS".equals(factor)) {
            after -= avgCross(b) * 0.085d;
        } else if ("NEED_SUPPLY".equals(factor)) {
            after -= (c == null ? 0.0d : c.needSupplyMatchScore) * 0.120d;
        } else if ("THIRD".equals(factor)) {
            after -= (c == null ? 0.0d : c.needSupplyThirdQuality) * 0.100d;
        }
        return clamp01(before - Math.max(0.0d, after));
    }

    private String decideAction(CounterfactualBundle b, Models.ComboCandidate c) {
        if (b == null) {
            return AppConfig.ADAPTIVE_ACTION_WATCH;
        }
        if (b.finalWhyNotRisk >= HARD_WHY_NOT_RISK
                || (b.reason != null && b.reason.whyNotRisk >= HARD_WHY_NOT_RISK)
                || (b.ablation != null && b.ablation.falseCauseCount >= 4)) {
            return AppConfig.ADAPTIVE_ACTION_FREEZE;
        }
        if (b.finalRankScore >= READY_RANK_SCORE
                && b.finalWhyNotRisk <= 0.560d
                && (c == null || c.needSupplyRisk <= 0.620d)) {
            return AppConfig.ADAPTIVE_ACTION_READY;
        }
        return AppConfig.ADAPTIVE_ACTION_WATCH;
    }

    private String buildSummary(CounterfactualBundle b, Models.ComboCandidate c) {
        if (b == null) {
            return "";
        }
        return "反事实审判｜P"
                + b.position
                + "｜K3="
                + AppConfig.normalizeK3Part(b.n1, b.n2, b.n3)
                + "｜action="
                + b.action
                + "｜positionRank="
                + b.positionRank
                + "｜k3Rank="
                + b.k3Rank
                + "｜rankScore="
                + percent(b.finalRankScore)
                + "｜whyNotRisk="
                + percent(b.finalWhyNotRisk)
                + "｜mirrorMiss="
                + percent(b.mirrorMissSimilarity)
                + "｜falseCause="
                + (b.ablation == null ? 0 : b.ablation.falseCauseCount)
                + "｜真实BET仍由DecisionGateEngine唯一裁决";
    }

    private String buildCandidateWhy(CounterfactualBundle b, Models.ComboCandidate c) {
        if (b == null) {
            return "";
        }
        if (AppConfig.adaptiveActionAllowsBet(b.action)) {
            return "反事实可送最终门复核｜" + b.summary + "｜" + (b.reason == null ? "" : b.reason.summary);
        }
        return "反事实暂不晋级｜" + b.summary + "｜" + (b.reason == null ? "" : b.reason.summary);
    }

    private double mirrorMissSimilarity(CounterfactualBundle b, List<Models.Draw> clean) {
        if (b == null || clean == null || clean.size() == 0) {
            return 0.0d;
        }
        int sample = 0;
        int miss = 0;
        int start = Math.max(0, clean.size() - 60);
        for (int i = start; i < clean.size(); i++) {
            Models.Draw d = clean.get(i);
            if (!validDraw(d)) continue;
            int a = d.getNumberAtPosition(b.position);
            if (!validNumber(a)) continue;
            sample++;
            if (a != b.n1 && a != b.n2 && a != b.n3) miss++;
        }
        return sample <= 0 ? 0.0d : clamp01(((double) miss) / sample);
    }

    private String detectMissType(Models.MissReasonResult m) {
        if (m == null) {
            return AppConfig.MISS_REASON_UNKNOWN;
        }
        double best = m.windowWrongScore;
        String type = AppConfig.MISS_REASON_WINDOW_WRONG;
        if (m.positionWrongScore > best) {
            best = m.positionWrongScore;
            type = AppConfig.MISS_REASON_POSITION_WRONG;
        }
        if (m.mainNumberWrongScore > best) {
            best = m.mainNumberWrongScore;
            type = AppConfig.MISS_REASON_MAIN_NUMBER_WRONG;
        }
        if (m.thirdDragScore > best) {
            best = m.thirdDragScore;
            type = AppConfig.MISS_REASON_THIRD_DRAG;
        }
        if (m.counterIgnoredScore > best) {
            type = AppConfig.MISS_REASON_COUNTER_IGNORED;
        }
        return type;
    }

    private String nextAdvice(Models.MissReasonResult m) {
        if (m == null) {
            return "";
        }
        if (AppConfig.MISS_REASON_WINDOW_WRONG.equals(m.missType)) {
            return "降低同类窗口簇权重，必须重新通过近期有效性。";
        }
        if (AppConfig.MISS_REASON_POSITION_WRONG.equals(m.missType)) {
            return "提高位置反事实排名要求，替代位置更强时只WATCH。";
        }
        if (AppConfig.MISS_REASON_MAIN_NUMBER_WRONG.equals(m.missType)) {
            return "主号供给不足，下一轮降低该号码角色权重。";
        }
        if (AppConfig.MISS_REASON_THIRD_DRAG.equals(m.missType)) {
            return "第三号拖拽，下一轮提高thirdQuality要求。";
        }
        if (AppConfig.MISS_REASON_COUNTER_IGNORED.equals(m.missType)) {
            return "反证被忽略，下一轮提高whyNotRisk权重。";
        }
        return "保持观察，禁止直接BET。";
    }

    private double positionRate(int position, List<Models.Draw> clean, int window) {
        if (clean == null || clean.size() == 0) {
            return K3_BASELINE;
        }
        int sample = 0;
        int change = 0;
        int prev = -1;
        int start = Math.max(0, clean.size() - Math.max(1, window));
        for (int i = start; i < clean.size(); i++) {
            Models.Draw d = clean.get(i);
            if (!validDraw(d)) continue;
            int a = d.getNumberAtPosition(position);
            if (!validNumber(a)) continue;
            if (prev >= 0) {
                sample++;
                if (a != prev) change++;
            }
            prev = a;
        }
        return sample <= 0 ? K3_BASELINE : clamp01(((double) change) / sample);
    }

    private double positionOmissionPressure(int position, List<Models.Draw> clean) {
        if (clean == null || clean.size() == 0) {
            return 0.0d;
        }
        int sample = 0;
        int gapSum = 0;
        int prevIndex = -1;
        int start = Math.max(0, clean.size() - 96);
        for (int i = start; i < clean.size(); i++) {
            Models.Draw d = clean.get(i);
            if (!validDraw(d)) continue;
            int a = d.getNumberAtPosition(position);
            if (!validNumber(a)) continue;
            if (prevIndex >= 0) {
                gapSum += i - prevIndex;
                sample++;
            }
            prevIndex = i;
        }
        double avgGap = sample <= 0 ? 5.0d : ((double) gapSum) / sample;
        return clamp01(avgGap / 18.0d);
    }

    private double positionStability(int position, List<Models.Draw> clean, int window) {
        if (clean == null || clean.size() < 2) {
            return 0.45d;
        }
        int sample = 0;
        int stable = 0;
        int start = Math.max(1, clean.size() - Math.max(2, window));
        for (int i = start; i < clean.size(); i++) {
            Models.Draw cur = clean.get(i);
            Models.Draw prev = clean.get(i - 1);
            if (!validDraw(cur) || !validDraw(prev)) continue;
            int a = cur.getNumberAtPosition(position);
            int b = prev.getNumberAtPosition(position);
            if (!validNumber(a) || !validNumber(b)) continue;
            sample++;
            if (Math.abs(a - b) <= 3) stable++;
        }
        return sample <= 0 ? 0.45d : clamp01(((double) stable) / sample);
    }

    private double positionCounterRisk(PositionCounterState s) {
        if (s == null) {
            return 0.0d;
        }
        double risk = 0.0d;
        risk += Math.abs(s.rate12 - s.rate96) * 0.28d;
        risk += Math.max(0.0d, K3_BASELINE - s.rate24) * 0.40d;
        risk += (1.0d - s.stabilityScore) * 0.18d;
        risk += s.omissionPressure > 0.80d ? 0.10d : 0.0d;
        return clamp01(risk);
    }

    private double numberRate(int position, int number, List<Models.Draw> clean, int window) {
        if (clean == null || clean.size() == 0) {
            return NUMBER_BASELINE;
        }
        int sample = 0;
        int hit = 0;
        int start = Math.max(0, clean.size() - Math.max(1, window));
        for (int i = start; i < clean.size(); i++) {
            Models.Draw d = clean.get(i);
            if (!validDraw(d)) continue;
            int a = d.getNumberAtPosition(position);
            if (!validNumber(a)) continue;
            sample++;
            if (a == number) hit++;
        }
        return sample <= 0 ? NUMBER_BASELINE : clamp01(((double) hit) / sample);
    }

    private double numberOmissionPressure(int position, int number, List<Models.Draw> clean) {
        if (clean == null || clean.size() == 0) {
            return 0.0d;
        }
        int miss = 0;
        for (int i = clean.size() - 1; i >= 0 && i >= clean.size() - 80; i--) {
            Models.Draw d = clean.get(i);
            if (!validDraw(d)) continue;
            int a = d.getNumberAtPosition(position);
            if (a == number) break;
            miss++;
        }
        return clamp01(miss / 50.0d);
    }

    private double numberNeighborScore(
            int position, int number, List<Models.Draw> clean, int window) {
        if (clean == null || clean.size() == 0) {
            return 0.0d;
        }
        int nA = number > MIN_NUMBER ? number - 1 : number;
        int nB = number < MAX_NUMBER ? number + 1 : number;
        int sample = 0;
        int hit = 0;
        int start = Math.max(0, clean.size() - Math.max(1, window));
        for (int i = start; i < clean.size(); i++) {
            Models.Draw d = clean.get(i);
            if (!validDraw(d)) continue;
            int a = d.getNumberAtPosition(position);
            if (!validNumber(a)) continue;
            sample++;
            if (a == nA || a == nB) hit++;
        }
        return sample <= 0 ? 0.0d : clamp01(((double) hit) / sample * 2.0d);
    }

    private double numberTailScore(int number, List<Models.Draw> clean, int window) {
        if (clean == null || clean.size() == 0) {
            return 0.0d;
        }
        int tail = number % 5;
        int sample = 0;
        int hit = 0;
        int start = Math.max(0, clean.size() - Math.max(1, window));
        for (int i = start; i < clean.size(); i++) {
            Models.Draw d = clean.get(i);
            if (!validDraw(d)) continue;
            for (int p = 1; p <= MAX_POSITION; p++) {
                int a = d.getNumberAtPosition(p);
                if (!validNumber(a)) continue;
                sample++;
                if (a % 5 == tail) hit++;
            }
        }
        return sample <= 0 ? 0.0d : clamp01(((double) hit) / sample * 5.0d);
    }

    private double numberCrossScore(int number, List<Models.Draw> clean, int window) {
        if (clean == null || clean.size() == 0) {
            return 0.0d;
        }
        int sample = 0;
        int hit = 0;
        int start = Math.max(0, clean.size() - Math.max(1, window));
        for (int i = start; i < clean.size(); i++) {
            Models.Draw d = clean.get(i);
            if (!validDraw(d)) continue;
            for (int p = 1; p <= MAX_POSITION; p++) {
                int a = d.getNumberAtPosition(p);
                if (!validNumber(a)) continue;
                sample++;
                if (a == number) hit++;
            }
        }
        return sample <= 0 ? 0.0d : clamp01(((double) hit) / sample * 10.0d);
    }

    private double numberCounterRisk(NumberCounterState s) {
        if (s == null) {
            return 0.0d;
        }
        double risk = 0.0d;
        risk += Math.max(0.0d, NUMBER_BASELINE * 0.50d - s.rate96) * 1.80d;
        risk += Math.max(0.0d, s.rate12 - s.rate48) * 0.65d;
        risk += Math.max(0.0d, s.crossScore - 0.75d) * 0.30d;
        risk += s.omissionPressure > 0.90d ? 0.10d : 0.0d;
        return clamp01(risk);
    }

    private PositionCounterState getPositionState(
            ArrayList<PositionCounterState> list, int position) {
        if (list == null) {
            return null;
        }
        for (int i = 0; i < list.size(); i++) {
            PositionCounterState s = list.get(i);
            if (s != null && s.position == position) {
                return s;
            }
        }
        return null;
    }

    private NumberCounterState getNumberState(
            ArrayList<NumberCounterState> list, int position, int number) {
        if (list == null) {
            return null;
        }
        for (int i = 0; i < list.size(); i++) {
            NumberCounterState s = list.get(i);
            if (s != null && s.position == position && s.number == number) {
                return s;
            }
        }
        return null;
    }

    private void sortPositionStates(ArrayList<PositionCounterState> list) {
        if (list == null || list.size() < 2) {
            return;
        }
        Collections.sort(
                list,
                new Comparator<PositionCounterState>() {
                    public int compare(PositionCounterState a, PositionCounterState b) {
                        double sa = a == null ? 0.0d : a.needScore - a.counterRisk;
                        double sb = b == null ? 0.0d : b.needScore - b.counterRisk;
                        if (sa > sb) return -1;
                        if (sa < sb) return 1;
                        return 0;
                    }
                });
    }

    private void sortNumberStates(ArrayList<NumberCounterState> list) {
        if (list == null || list.size() < 2) {
            return;
        }
        Collections.sort(
                list,
                new Comparator<NumberCounterState>() {
                    public int compare(NumberCounterState a, NumberCounterState b) {
                        double sa = a == null ? 0.0d : a.supplyScore - a.counterRisk;
                        double sb = b == null ? 0.0d : b.supplyScore - b.counterRisk;
                        if (sa > sb) return -1;
                        if (sa < sb) return 1;
                        return 0;
                    }
                });
    }

    private void sortBundles(ArrayList<CounterfactualBundle> list) {
        if (list == null || list.size() < 2) {
            return;
        }
        Collections.sort(
                list,
                new Comparator<CounterfactualBundle>() {
                    public int compare(CounterfactualBundle a, CounterfactualBundle b) {
                        double sa = a == null ? 0.0d : a.finalRankScore - a.finalWhyNotRisk;
                        double sb = b == null ? 0.0d : b.finalRankScore - b.finalWhyNotRisk;
                        if (sa > sb) return -1;
                        if (sa < sb) return 1;
                        return 0;
                    }
                });
    }

    private double supply(NumberCounterState s) {
        return s == null ? 0.0d : clamp01(s.supplyScore);
    }

    private double risk(NumberCounterState s) {
        return s == null ? 0.0d : clamp01(s.counterRisk);
    }

    private double avgNumberRisk(CounterfactualBundle b) {
        if (b == null) {
            return 0.0d;
        }
        return clamp01((risk(b.n1State) + risk(b.n2State) + risk(b.n3State)) / 3.0d);
    }

    private double thirdDragFromBundle(CounterfactualBundle b) {
        if (b == null) {
            return 0.0d;
        }
        double third = supply(b.n3State);
        return clamp01(Math.max(0.0d, 0.100d - third));
    }

    private double avgOmission(CounterfactualBundle b) {
        if (b == null) return 0.0d;
        double a = b.n1State == null ? 0.0d : b.n1State.omissionPressure;
        double x = b.n2State == null ? 0.0d : b.n2State.omissionPressure;
        double y = b.n3State == null ? 0.0d : b.n3State.omissionPressure;
        return clamp01((a + x + y) / 3.0d);
    }

    private double avgTail(CounterfactualBundle b) {
        if (b == null) return 0.0d;
        double a = b.n1State == null ? 0.0d : b.n1State.tailScore;
        double x = b.n2State == null ? 0.0d : b.n2State.tailScore;
        double y = b.n3State == null ? 0.0d : b.n3State.tailScore;
        return clamp01((a + x + y) / 3.0d);
    }

    private double avgNeighbor(CounterfactualBundle b) {
        if (b == null) return 0.0d;
        double a = b.n1State == null ? 0.0d : b.n1State.neighborScore;
        double x = b.n2State == null ? 0.0d : b.n2State.neighborScore;
        double y = b.n3State == null ? 0.0d : b.n3State.neighborScore;
        return clamp01((a + x + y) / 3.0d);
    }

    private double avgCross(CounterfactualBundle b) {
        if (b == null) return 0.0d;
        double a = b.n1State == null ? 0.0d : b.n1State.crossScore;
        double x = b.n2State == null ? 0.0d : b.n2State.crossScore;
        double y = b.n3State == null ? 0.0d : b.n3State.crossScore;
        return clamp01((a + x + y) / 3.0d);
    }

    private ArrayList<Models.Draw> cleanHistory(List<Models.Draw> history) {
        ArrayList<Models.Draw> out = new ArrayList<Models.Draw>();
        if (history == null) {
            return out;
        }
        for (int i = 0; i < history.size(); i++) {
            Models.Draw d = history.get(i);
            if (validDraw(d)) {
                out.add(d);
            }
        }
        return out;
    }

    private boolean validDraw(Models.Draw d) {
        try {
            return d != null && d.isValid();
        } catch (Throwable e) {
            return false;
        }
    }

    private boolean validNumber(int n) {
        return n >= MIN_NUMBER && n <= MAX_NUMBER;
    }

    private int normalizePosition(int p) {
        if (p < 1) return 1;
        if (p > MAX_POSITION) return MAX_POSITION;
        return p;
    }

    private int normalizeNumber(int n) {
        if (n < MIN_NUMBER) return MIN_NUMBER;
        if (n > MAX_NUMBER) return MAX_NUMBER;
        return n;
    }

    private int buildK3Mask(int n1, int n2, int n3) {
        int m = 0;
        if (validNumber(n1)) m |= (1 << n1);
        if (validNumber(n2)) m |= (1 << n2);
        if (validNumber(n3)) m |= (1 << n3);
        return m;
    }

    private String firstNonEmpty(String a, String b) {
        return a != null && a.length() > 0 ? a : safe(b);
    }

    private double clamp01(double v) {
        return AppConfig.clampDynamicCausalScore(v);
    }

    private String percent(double v) {
        return String.format(java.util.Locale.US, "%.1f%%", v * 100.0d);
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }

    private static final class PositionCounterState {
        int position;
        double rate12;
        double rate24;
        double rate48;
        double rate96;
        double omissionPressure;
        double stabilityScore;
        double needScore;
        double counterRisk;
    }

    private static final class NumberCounterState {
        int position;
        int number;
        double rate12;
        double rate24;
        double rate48;
        double rate96;
        double omissionPressure;
        double neighborScore;
        double tailScore;
        double crossScore;
        double supplyScore;
        double counterRisk;
    }

    private static final class AlternativeK3 {
        int n1;
        int n2;
        int n3;
        double score;
        String text;

        AlternativeK3() {
            text = "";
        }
    }

    public static final class CounterfactualBundle {
        public String baseIssue;
        public String targetIssue;
        public int position;
        public int n1;
        public int n2;
        public int n3;
        public int k3Mask;
        PositionCounterState positionState;
        NumberCounterState n1State;
        NumberCounterState n2State;
        NumberCounterState n3State;
        PositionCounterState altPosition;
        AlternativeK3 altK3;
        public int positionRank;
        public int k3Rank;
        public double mirrorMissSimilarity;
        public Models.CounterfactualReasonResult reason;
        public Models.CauseAblationResult ablation;
        public Models.MissReasonResult missReason;
        public double finalRankScore;
        public double finalWhyNotRisk;
        public String action;
        public String summary;

        public CounterfactualBundle() {
            baseIssue = "";
            targetIssue = "";
            action = AppConfig.ADAPTIVE_ACTION_WATCH;
            summary = "";
        }

        public static CounterfactualBundle empty(String reason) {
            CounterfactualBundle b = new CounterfactualBundle();
            b.summary = reason == null ? "" : reason;
            b.finalWhyNotRisk = 1.0d;
            return b;
        }
    }

    // V57_B08_EVIDENCE_ONLY_FULL_IMPLEMENTATION

    public static final String V57_B08_OWNER =
            "DIAGNOSTIC_EVIDENCE_ONLY:CounterfactualReasonEngine";

    public static final class V57CounterfactualReason {
        public String hypothesis = "";
        public String observed = "";
        public String alternate = "";
        public String positiveEvidence = "";
        public String negativeEvidence = "";
        public double confidence;
        public String hash = "";

        public String canonical() {
            return hypothesis
                    + '|'
                    + observed
                    + '|'
                    + alternate
                    + '|'
                    + positiveEvidence
                    + '|'
                    + negativeEvidence
                    + '|'
                    + confidence;
        }
    }

    public static V57CounterfactualReason v57b08Reason(
            String hypothesis,
            String observed,
            String alternate,
            String positive,
            String negative,
            double confidence) {
        V57CounterfactualReason r = new V57CounterfactualReason();
        r.hypothesis = hypothesis == null ? "" : hypothesis;
        r.observed = observed == null ? "" : observed;
        r.alternate = alternate == null ? "" : alternate;
        r.positiveEvidence = positive == null ? "" : positive;
        r.negativeEvidence = negative == null ? "" : negative;
        r.confidence = Double.isNaN(confidence) ? 0.0d : Math.max(0.0d, Math.min(1.0d, confidence));
        r.hash = v57b08Hash(r.canonical());
        return r;
    }

    public static boolean v57b08MayRewriteChampion() {
        return false;
    }

    public static boolean v57b08MayRewritePolicy() {
        return false;
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
