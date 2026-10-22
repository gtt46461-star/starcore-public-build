package com.ethk3.novaonepro;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * V49 realtime owner.
 *
 * <p>The live path evaluates exactly one normalized ten-number distribution for each of the ten
 * positions. Because number events inside a position are mutually exclusive, its legal K3 action is
 * the three largest calibrated probabilities. The 1200 overlapping combinations remain a background
 * diagnostic surface only.
 */
public final class V49RealtimePipelineEngine {
    public static final String MODULE_VERSION =
            V49BuildManifest.STRATEGY_VERSION + ":REALTIME_100_TO_10";

    private final V49NumberFeatureEngine featureEngine = new V49NumberFeatureEngine();
    private final MixtureOfExpertsGateEngine mixtureEngine = new MixtureOfExpertsGateEngine();
    private final V49LanePolicyEngine laneEngine = new V49LanePolicyEngine();
    private final PreDrawHypothesisLedgerEngine ledgerEngine = new PreDrawHypothesisLedgerEngine();

    public Result run(DbStore store, List<Models.Draw> history, String requestedTargetIssue) {
        long started = System.currentTimeMillis();
        Result out = new Result();
        ArrayList<Models.Draw> clean = normalize(history);
        if (clean.size() < 12) {
            out.status = "V49_HISTORY_LT12";
            return out;
        }
        Models.Draw latest = clean.get(clean.size() - 1);
        String targetIssue = safe(requestedTargetIssue);
        if (targetIssue.length() == 0) targetIssue = nextIssue(safe(latest.issue));

        long tFeature = System.currentTimeMillis();
        V49FeatureSnapshot features = featureEngine.build(clean, targetIssue);
        out.featureCostMs = System.currentTimeMillis() - tFeature;
        if (!features.isValid()) {
            out.status = "V49_FEATURE_SNAPSHOT_INVALID";
            return out;
        }

        long tExpert = System.currentTimeMillis();
        MixtureOfExpertsGateEngine.FusionResult fusion =
                mixtureEngine.evaluate(store, clean, features);
        out.expertCostMs = System.currentTimeMillis() - tExpert;
        if (fusion == null || !fusion.valid) {
            out.status = "V49_EXPERT_FUSION_INVALID";
            return out;
        }

        long tAction = System.currentTimeMillis();
        for (int p = 0; p < 10; p++) {
            int[] order = descendingOrder(fusion.probability[p]);
            Models.ComboCandidate c = new Models.ComboCandidate();
            c.baseIssue = safe(latest.issue);
            c.targetIssue = targetIssue;
            c.createdAt = System.currentTimeMillis();
            c.position = p + 1;
            c.setNumbers(order[0] + 1, order[1] + 1, order[2] + 1);

            double p1 = fusion.probability[p][order[0]];
            double p2 = fusion.probability[p][order[1]];
            double p3 = fusion.probability[p][order[2]];
            double p4 = fusion.probability[p][order[3]];
            double actionP = FeatureContractRegistry.clamp01(p1 + p2 + p3);
            double thirdMargin = p3 - p4;
            double agreement = FeatureContractRegistry.clamp01(fusion.agreement[p]);
            double uncertainty = FeatureContractRegistry.clamp01(fusion.uncertainty[p]);
            double stateStrength = FeatureContractRegistry.clamp01(features.familyStrength[p]);
            double switchRisk = FeatureContractRegistry.clamp01(features.switchRisk[p]);
            double concentration = FeatureContractRegistry.clamp01(1.0d - features.entropy[p]);
            double thirdQuality =
                    FeatureContractRegistry.clamp01(p3 * 3.5d + Math.max(0.0d, thirdMargin) * 5.0d);
            double rankMargin =
                    Math.max(0.0d, actionP - top3WithoutBest(fusion.probability[p], order));
            double opportunity =
                    opportunity(
                            actionP,
                            agreement,
                            uncertainty,
                            stateStrength,
                            switchRisk,
                            thirdQuality,
                            rankMargin,
                            concentration);

            V49LanePolicyEngine.Result lane =
                    laneEngine.compile(
                            store,
                            actionP,
                            opportunity,
                            agreement,
                            uncertainty,
                            switchRisk,
                            stateStrength,
                            features.family[p],
                            features.lifecycle[p]);

            // Canonical probability owner fields.  No downstream evidence may rewrite these.
            c.rawP = actionP;
            c.modelP = actionP;
            c.calibratedP = actionP;
            c.finalP = actionP;
            c.finalPBeforeDiagnosis = actionP;
            c.finalPAfterDiagnosis = actionP;
            c.finalPAfterFusion = actionP;
            c.calibratedHitProbability = actionP;
            c.honestProbability = actionP;
            c.v46EventProbability = actionP;
            c.v46GlobalCalibratedProbability = actionP;
            c.v46ProbabilityUncertainty = uncertainty;
            c.v46ActionLcb = Math.max(0.001d, actionP - (0.018d + 0.050d * uncertainty));
            c.v43SafeProbability = c.v46ActionLcb;
            c.v46ExpectedValue = AppConfig.ODDS * actionP - AppConfig.BET_COST;
            c.dynamicGate = lane.gate;
            c.gate = lane.gate;
            c.riskAdjustedGate = lane.gate;
            c.edge = actionP - lane.gate;
            c.rawEdge = c.edge;
            c.v46Lane = lane.lane;
            c.laneCandidate = lane.lane;
            c.betDomain = lane.lane;
            c.finalDecisionBetDomain = lane.lane;
            c.finalDecisionRejectCode = lane.eligible ? "" : lane.code;
            c.rejectCode = c.finalDecisionRejectCode;
            c.blockCode = "";

            // Reliability, state, third-number and reason evidence are separate from P.
            c.v43StrategySupport = opportunity;
            c.rawRankScore = opportunity;
            c.finalRankScore = opportunity;
            c.rankScore = opportunity;
            c.coverageQuality = agreement;
            c.coverageQualityScore = agreement;
            c.dynamicGateCoverageQualityScore = agreement;
            c.thirdQuality = thirdQuality;
            c.thirdNumberQualityScore = thirdQuality;
            c.dynamicGateThirdNumberQuality = thirdQuality;
            c.k3CoverageRisk = uncertainty;
            c.dynamicGateCoverageRisk = uncertainty;
            c.driftRisk = switchRisk;
            c.v43StrategyMaturity = stateStrength;
            c.stateConfidence = stateStrength;
            c.stateKey =
                    features.family[p]
                            + "|"
                            + features.lifecycle[p]
                            + "|run="
                            + features.stateRunLength[p];
            c.windowStateKey = c.stateKey;
            c.probabilityWindowStateKey = c.stateKey;
            c.finalDecisionWindowStateKey = c.stateKey;
            c.stateMode = features.lifecycle[p];
            c.trendLife = features.stateRunLength[p];
            c.comboGap = thirdMargin;
            c.championGap = rankMargin;
            c.weakestNumberScore = p3;
            c.balanceScore = concentration;
            c.effectiveSample = features.historySize;
            c.posteriorSample = features.historySize;
            c.coverageAvailable = 1;
            c.thirdQualityAvailable = 1;
            c.probabilityMode = "V49_MOE_DIRICHLET_TOP3";
            c.probabilityFieldVersion = MODULE_VERSION;
            c.probabilityPipelineVersion = V49BuildManifest.STRATEGY_VERSION;
            c.moduleBuildVersion = MODULE_VERSION;
            c.v46Version = V49BuildManifest.MODEL_VERSION;
            c.v43StrategyVersion = V49BuildManifest.STRATEGY_VERSION;
            c.v43PipelineStage = "V49_FINAL_10_POSITION_ACTIONS";
            c.teacherOrdinaryBetPolicy = "READY";
            c.teacherMatrixOrdinaryBetPolicy = "READY";
            c.teacherMatrixStatus = "V49_HIERARCHICAL_POSTERIOR_READY";
            c.teacherStatus = "READY";
            c.teacherNumberStatus = "READY";
            c.dynamicStatus = switchRisk > AppConfig.V49_MAX_SWITCH_RISK ? "SOFT_RISK" : "READY";
            c.dynamicCauseStatus = c.dynamicStatus;
            c.adaptiveStatus = lane.eligible ? "READY" : "WATCH";
            c.adaptiveJudgeStatus = c.adaptiveStatus;
            c.adaptiveJudgeAction = lane.eligible ? "ALLOW_TO_GATE" : "WATCH";
            c.ruleHealthStatus = "READY";
            c.shadowPromotionStatus = lane.eligible ? "SHADOW_PROMOTE" : "WATCH";
            c.hardBlockFlag = 0;
            c.hardBlockSource = "";
            c.v43HardRisk = 0;
            c.v43EvidenceGroupCount = 5;
            c.v43DominantEvidence = causesPlaceholder(features, p);
            c.v43MissingEvidence = "";
            c.preDecisionOpportunity = lane.eligible ? 1 : 0;
            c.qualifiedProbe = V49LanePolicyEngine.PROBE.equals(lane.lane) ? 1 : 0;
            c.realProbeAllowed = c.qualifiedProbe;
            c.realTrade = 0;

            String[] causes = causes(features, fusion, p, order, thirdMargin);
            c.whyBet = causes[0];
            c.preDrawReasonText = causes[0] + "｜次因=" + causes[1] + "｜最大反证=" + causes[2];
            c.preDrawReasonFingerprint = FeatureContractRegistry.sha256(c.preDrawReasonText);
            c.reasonSummary =
                    "V49单一概率Owner｜"
                            + c.preDrawReasonText
                            + "｜EventP="
                            + actionP
                            + "｜Third="
                            + p3
                            + "｜ThirdMargin="
                            + thirdMargin
                            + "｜Agreement="
                            + agreement
                            + "｜Uncertainty="
                            + uncertainty
                            + "｜Opportunity="
                            + opportunity
                            + "｜Lane="
                            + lane.lane
                            + "｜LaneCode="
                            + lane.code
                            + "｜Experts="
                            + fusion.weightSummary[p];
            c.v46ProbabilityTrace = c.reasonSummary;
            c.fusionSummary = c.reasonSummary;

            PredictionContractEngine.stampCandidate(c, clean);
            String v49EvidenceHash = V49ContractEngine.candidateHash(c, features.checksum);
            c.v43EvidenceFingerprint = v49EvidenceHash;
            c.v43ProbabilityLock = actionP;
            String lockStatus =
                    PipelineInvariantGuard.lockFinalPrediction(c, "V49_FINAL_CANONICAL_LOCK");
            if (!PipelineInvariantGuard.STATUS_LOCKED.equals(lockStatus)) {
                c.finalDecisionRejectCode = lockStatus;
                c.rejectCode = lockStatus;
                c.preDecisionOpportunity = 0;
            }
            out.candidates.add(c);
        }
        Collections.sort(out.candidates, candidateComparator());
        for (int i = 0; i < out.candidates.size(); i++) {
            Models.ComboCandidate c = out.candidates.get(i);
            c.rank = i + 1;
            c.v46FinalRank = i + 1;
            c.oosRankSlot = i + 1;
            c.oosCandidateCount = out.candidates.size();
            c.v46FormalCandidateCount = out.candidates.size();
        }
        out.actionCostMs = System.currentTimeMillis() - tAction;
        out.features = features;
        out.fusion = fusion;
        out.selected = out.candidates.size() == 0 ? null : out.candidates.get(0);
        if (out.selected != null) {
            V49Repository.recordOpportunity(
                    store,
                    targetIssue,
                    out.selected.v43StrategySupport,
                    out.selected.finalP,
                    safe(out.selected.v46Lane));
            String[] causes =
                    causes(
                            features,
                            fusion,
                            out.selected.position - 1,
                            new int[] {
                                out.selected.n1 - 1, out.selected.n2 - 1, out.selected.n3 - 1, 0
                            },
                            out.selected.comboGap);
            ledgerEngine.persist(
                    store,
                    features,
                    fusion,
                    out.candidates,
                    out.selected,
                    causes[0],
                    causes[1],
                    causes[2]);
        }
        out.totalCostMs = System.currentTimeMillis() - started;
        out.status =
                out.candidates.size() == 10
                        ? "V49_OK"
                        : "V49_ACTION_COUNT_" + out.candidates.size();
        persistRuntime(store, out);
        return out;
    }

    private static double opportunity(
            double actionP,
            double agreement,
            double uncertainty,
            double stateStrength,
            double switchRisk,
            double thirdQuality,
            double rankMargin,
            double concentration) {
        double x =
                0.34d * normalizeActionP(actionP)
                        + 0.17d * agreement
                        + 0.14d * stateStrength
                        + 0.10d * thirdQuality
                        + 0.08d * FeatureContractRegistry.clamp01(rankMargin * 12.0d)
                        + 0.07d * concentration
                        + 0.10d * (1.0d - uncertainty)
                        - 0.14d * switchRisk;
        return FeatureContractRegistry.clamp01(x);
    }

    private static double normalizeActionP(double p) {
        return FeatureContractRegistry.clamp01((p - 0.25d) / 0.22d);
    }

    private static double top3WithoutBest(double[] p, int[] order) {
        if (p == null || order == null || order.length < 4) return 0.0d;
        return p[order[1]] + p[order[2]] + p[order[3]];
    }

    private static String causesPlaceholder(V49FeatureSnapshot f, int p) {
        if (f == null || p < 0 || p >= 10) return "V49_MOE";
        return "WINDOW:"
                + f.family[p]
                + "/"
                + f.lifecycle[p]
                + "|NINE_EXPERT_MOE|TOP3_MUTUALLY_EXCLUSIVE";
    }

    private static String[] causes(
            V49FeatureSnapshot f,
            MixtureOfExpertsGateEngine.FusionResult fusion,
            int p,
            int[] order,
            double thirdMargin) {
        String family = f.family[p];
        String lifecycle = f.lifecycle[p];
        String primary =
                "窗口="
                        + family
                        + "/"
                        + lifecycle
                        + "，状态强度="
                        + round(f.familyStrength[p])
                        + "，Top号码="
                        + number(order[0] + 1);
        String secondary = "九专家共识=" + round(fusion.agreement[p]) + "，第三号独立边际=" + round(thirdMargin);
        String counter =
                f.switchRisk[p] > 0.55d
                        ? "状态切换风险=" + round(f.switchRisk[p])
                        : (thirdMargin < 0.006d
                                ? "第三号与第四号边界过近=" + round(thirdMargin)
                                : "专家不确定性=" + round(fusion.uncertainty[p]));
        return new String[] {primary, secondary, counter};
    }

    private static Comparator<Models.ComboCandidate> candidateComparator() {
        return new Comparator<Models.ComboCandidate>() {
            public int compare(Models.ComboCandidate a, Models.ComboCandidate b) {
                int x = Double.compare(b.v43StrategySupport, a.v43StrategySupport);
                if (x != 0) return x;
                x = Double.compare(b.finalP, a.finalP);
                if (x != 0) return x;
                x = Double.compare(b.coverageQualityScore, a.coverageQualityScore);
                if (x != 0) return x;
                return a.position - b.position;
            }
        };
    }

    private static int[] descendingOrder(final double[] p) {
        Integer[] idx = new Integer[10];
        for (int i = 0; i < 10; i++) idx[i] = Integer.valueOf(i);
        java.util.Arrays.sort(
                idx,
                new Comparator<Integer>() {
                    public int compare(Integer a, Integer b) {
                        int x = Double.compare(p[b.intValue()], p[a.intValue()]);
                        return x != 0 ? x : a.intValue() - b.intValue();
                    }
                });
        int[] out = new int[10];
        for (int i = 0; i < 10; i++) out[i] = idx[i].intValue();
        return out;
    }

    private static ArrayList<Models.Draw> normalize(List<Models.Draw> history) {
        ArrayList<Models.Draw> out = new ArrayList<Models.Draw>();
        if (history != null)
            for (int i = 0; i < history.size(); i++) {
                Models.Draw d = history.get(i);
                if (d != null && d.isValid()) out.add(d);
            }
        Collections.sort(
                out,
                new Comparator<Models.Draw>() {
                    public int compare(Models.Draw a, Models.Draw b) {
                        return safe(a.issue).compareTo(safe(b.issue));
                    }
                });
        int from = Math.max(0, out.size() - AppConfig.V49_HISTORY_LIMIT);
        return new ArrayList<Models.Draw>(out.subList(from, out.size()));
    }

    private static String nextIssue(String issue) {
        try {
            return new BigInteger(issue).add(BigInteger.ONE).toString();
        } catch (Throwable ignore) {
            return issue + "1";
        }
    }

    private static void persistRuntime(DbStore store, Result r) {
        if (store == null || r == null) return;
        try {
            store.setState("v49_runtime_status", r.status);
            store.setState("v49_runtime_version", MODULE_VERSION);
            store.setState("v49_runtime_pipeline", V49BuildManifest.PIPELINE);
            store.setState("v49_feature_checksum", r.features == null ? "" : r.features.checksum);
            store.setState("v49_candidate_count", String.valueOf(r.candidates.size()));
            store.setState(
                    "v49_selected_summary",
                    r.selected == null ? "EMPTY" : r.selected.reasonSummary);
            store.setState("v49_cost_feature_ms", String.valueOf(r.featureCostMs));
            store.setState("v49_cost_expert_ms", String.valueOf(r.expertCostMs));
            store.setState("v49_cost_action_ms", String.valueOf(r.actionCostMs));
            store.setState("v49_cost_total_ms", String.valueOf(r.totalCostMs));
            store.setState("active_realtime_probability_owner", MODULE_VERSION);
            store.setState("v49_legacy_1200_realtime", "BACKGROUND_DIAGNOSTIC_ONLY");
        } catch (Throwable ignore) {
        }
    }

    private static String number(int n) {
        return n < 10 ? "0" + n : String.valueOf(n);
    }

    private static String round(double v) {
        return String.valueOf(Math.round(v * 10000.0d) / 10000.0d);
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    public static final class Result {
        public final ArrayList<Models.ComboCandidate> candidates =
                new ArrayList<Models.ComboCandidate>();
        public V49FeatureSnapshot features;
        public MixtureOfExpertsGateEngine.FusionResult fusion;
        public Models.ComboCandidate selected;
        public String status = "V49_NOT_RUN";
        public long featureCostMs;
        public long expertCostMs;
        public long actionCostMs;
        public long totalCostMs;
    }
}
