package com.ethk3.novaonepro;

/**
 * V45 de-correlated evidence aggregation. Many legacy modules describe the same underlying window.
 * They are first pooled into five source groups so one signal cannot vote repeatedly under
 * different names.
 */
public final class EvidenceGroupAggregatorEngine {
    public static final String V46_ROLE = "DEDUPLICATED_EVIDENCE_GROUP_OWNER";

    public static final String MODULE_VERSION = AppConfig.V46_VERSION;

    public static final class Group {
        public String name = "";
        public double support;
        public double risk;
        public double confidence;
        public int sample;
        public boolean available;

        public boolean positive(double supportMin, double confidenceMin) {
            return available
                    && support >= supportMin
                    && confidence >= confidenceMin
                    && risk < 0.68d;
        }

        public String summary() {
            return name
                    + "="
                    + (available ? "READY" : "MISSING")
                    + ":s"
                    + round(support)
                    + "/r"
                    + round(risk)
                    + "/c"
                    + round(confidence)
                    + "/n"
                    + sample;
        }
    }

    public static final class Snapshot {
        public Group probability = new Group();
        public Group structure = new Group();
        public Group history = new Group();
        public Group counterfactual = new Group();
        public Group risk = new Group();
        public int availableGroups;
        public int positiveGroups;
        public int hardRisk;
        public double supportMean;
        public double riskMean;
        public double confidenceMean;
        public double robustSupport;
        public double robustRisk;
        public double robustConfidence;
        public double effectiveGroupCount;
        public double disagreement;
        public String robustMethod = "UNSET";

        public String summary() {
            return probability.summary()
                    + "|"
                    + structure.summary()
                    + "|"
                    + history.summary()
                    + "|"
                    + counterfactual.summary()
                    + "|"
                    + risk.summary()
                    + "|available="
                    + availableGroups
                    + "|positive="
                    + positiveGroups
                    + "|hard="
                    + hardRisk;
        }
    }

    private EvidenceGroupAggregatorEngine() {}

    public static Snapshot evaluate(
            Models.ComboCandidate c,
            ActionUtilityEngine.Result utility,
            RegimeRegistryEngine.Result regime,
            AssociationGraphEngine.Result association,
            CounterfactualDistillationEngine.Result distillation) {
        Snapshot out = new Snapshot();
        if (c == null) return out;

        out.probability.name = "G1_PROBABILITY";
        out.probability.support = normalizeProbabilitySupport(c);
        out.probability.risk =
                clamp01(
                        Math.max(
                                c.calibrationBrier,
                                Math.max(c.v43StrategyRisk, c.v43AblationFragility)));
        out.probability.confidence =
                clamp01(
                        max3(
                                c.v43StrategyConfidence,
                                c.posteriorSample >= 30
                                        ? Math.min(1.0d, c.posteriorSample / 160.0d)
                                        : 0.0d,
                                c.calibrationSample >= 20
                                        ? Math.min(1.0d, c.calibrationSample / 120.0d)
                                        : 0.0d));
        out.probability.sample = Math.max(c.posteriorSample, c.calibrationSample);
        out.probability.available = c.modelP > 0.0d && c.finalP > 0.0d;

        out.structure.name = "G2_STRUCTURE";
        double coverage =
                max3(c.coverageQuality, c.coverageQualityScore, c.dynamicGateCoverageQualityScore);
        double third =
                max3(c.thirdQuality, c.thirdNumberQualityScore, c.dynamicGateThirdNumberQuality);
        double structureScore = utility == null ? 0.0d : utility.structureScore;
        double regimeQuality = regime == null ? 0.0d : regime.quality;
        out.structure.support =
                clamp01(
                        0.32d * coverage
                                + 0.24d * third
                                + 0.24d * structureScore
                                + 0.20d * regimeQuality);
        out.structure.risk =
                clamp01(
                        max3(
                                c.comboConflict,
                                c.dynamicGateCoverageRisk,
                                c.dynamicGateVirtualEdgeRisk));
        out.structure.confidence =
                clamp01(
                        0.50d * Math.max(coverage, third)
                                + 0.50d * Math.max(structureScore, regimeQuality));
        out.structure.sample = Math.max(c.evidenceForestSample, c.historicalPriorSample);
        out.structure.available = coverage > 0.0d || third > 0.0d || structureScore > 0.0d;

        out.history.name = "G3_HISTORY";
        double posterior = max3(c.posteriorMean, c.posteriorRecentRate, c.dynamicClusterHitRate);
        double teacher =
                max3(c.teacherMatrixScore, c.teacherNumberScore, c.teacherEvidenceContribution);
        double prior = max3(c.historicalPriorP, c.evidenceForestP, c.familyWilsonLower);
        out.history.support = clamp01(0.42d * posterior + 0.30d * teacher + 0.28d * prior);
        double teacherRisk =
                Math.max(
                        c.teacherMatrixRiskPenalty,
                        c.teacherMatrixRiskFlag > 0 || c.teacherMatrixFreezeFlag > 0 ? 1.0d : 0.0d);
        out.history.risk =
                clamp01(
                        max3(
                                teacherRisk,
                                c.teacherCounterContribution,
                                c.dynamicClusterCounterScore));
        out.history.confidence =
                clamp01(
                        max3(
                                c.posteriorSample / 160.0d,
                                c.teacherMatrixSample / 160.0d,
                                c.historicalPriorConfidence));
        out.history.sample =
                Math.max(
                        c.posteriorSample,
                        Math.max(c.teacherMatrixSample, c.historicalPriorSample));
        out.history.available = out.history.sample > 0;

        out.counterfactual.name = "G4_COUNTERFACTUAL";
        double assoc = association == null ? 0.0d : association.score;
        double distillSupport = distillation == null ? 0.0d : distillation.support;
        out.counterfactual.support =
                clamp01(
                        max3(
                                c.counterfactualRankScore,
                                c.needSupplyMatchScore,
                                Math.max(assoc, distillSupport)));
        out.counterfactual.risk =
                clamp01(
                        max3(
                                c.counterRisk,
                                c.counterEvidenceScore,
                                distillation == null ? 0.0d : distillation.falseStrongRisk));
        out.counterfactual.confidence =
                clamp01(
                        0.50d * out.counterfactual.support
                                + 0.50d * (1.0d - out.counterfactual.risk));
        out.counterfactual.sample = Math.max(c.dynamicClusterSample, c.shadowDomainSample);
        out.counterfactual.available =
                out.counterfactual.support > 0.0d || out.counterfactual.risk > 0.0d;

        out.risk.name = "G5_RISK";
        out.risk.support =
                clamp01(
                        1.0d
                                - max3(
                                        c.v43StrategyRisk,
                                        c.evidenceRisk,
                                        Math.max(c.driftRisk, c.breakDepth)));
        out.risk.risk =
                clamp01(
                        max3(
                                c.v43StrategyRisk,
                                c.falseStrongRisk,
                                Math.max(c.driftRisk, c.breakDepth)));
        out.risk.confidence =
                clamp01(max3(c.v43StrategyConfidence, c.stateConfidence, 1.0d - out.risk.risk));
        out.risk.sample = Math.max(c.evidenceForestSample, c.dynamicClusterSample);
        out.risk.available = true;

        Group[] groups =
                new Group[] {
                    out.probability, out.structure, out.history, out.counterfactual, out.risk
                };
        double ss = 0.0d, rr = 0.0d, cc = 0.0d;
        for (int i = 0; i < groups.length; i++) {
            Group g = groups[i];
            if (!g.available) continue;
            out.availableGroups++;
            ss += g.support;
            rr += g.risk;
            cc += g.confidence;
            if (g.positive(0.43d, 0.28d)) out.positiveGroups++;
        }
        if (out.availableGroups > 0) {
            out.supportMean = ss / out.availableGroups;
            out.riskMean = rr / out.availableGroups;
            out.confidenceMean = cc / out.availableGroups;
        }
        out.hardRisk = c.hardBlockFlag == 1 || c.v43HardRisk == 1 || out.risk.risk >= 0.82d ? 1 : 0;
        R3RobustAggregation.enrich(out, groups);
        return out;
    }

    private static double normalizeProbabilitySupport(Models.ComboCandidate c) {
        double p = c.finalP > 0.0d ? c.finalP : c.calibratedHitProbability;
        return clamp01(0.50d + (p - AppConfig.RANDOM_K3_BASELINE) / 0.16d);
    }

    private static double max3(double a, double b, double c) {
        return Math.max(a, Math.max(b, c));
    }

    private static double clamp01(double v) {
        if (Double.isNaN(v) || Double.isInfinite(v)) return 0.0d;
        if (v > 1.5d) v /= 100.0d;
        return v < 0.0d ? 0.0d : (v > 1.0d ? 1.0d : v);
    }

    private static String round(double v) {
        return String.valueOf(Math.round(v * 1000.0d) / 1000.0d);
    }

    public static final String B07R3_AGGREGATOR_FULLBLOOD_MARKER = "B07R3_ROBUST_AGGREGATION";

    public static final class RobustMetricSpec {
        public final String id, target, estimator, purpose;
        public final double weight, cap;

        RobustMetricSpec(String i, String t, String e, double w, double c, String p) {
            id = i;
            target = t;
            estimator = e;
            weight = w;
            cap = c;
            purpose = p;
        }

        public String text() {
            return id + "#" + target + "#" + estimator + "#w=" + weight + "#cap=" + cap + "#"
                    + purpose;
        }
    }

    public static java.util.List<RobustMetricSpec> metricCatalogR3() {
        java.util.ArrayList<RobustMetricSpec> x = new java.util.ArrayList<RobustMetricSpec>();
        x.add(
                new RobustMetricSpec(
                        "R3M001",
                        "SUPPORT",
                        "WEIGHTED_MEAN",
                        0.500d,
                        0.550d,
                        "de-correlate same-window experts"));
        x.add(
                new RobustMetricSpec(
                        "R3M002",
                        "RISK",
                        "WEIGHTED_MEDIAN",
                        0.570d,
                        0.620d,
                        "bound counter-evidence tail"));
        x.add(
                new RobustMetricSpec(
                        "R3M003",
                        "CONFIDENCE",
                        "TRIMMED_MEAN",
                        0.640d,
                        0.690d,
                        "stabilize sparse OOS confidence"));
        x.add(
                new RobustMetricSpec(
                        "R3M004",
                        "SUPPORT",
                        "WINSOR_MEAN",
                        0.710d,
                        0.760d,
                        "prevent one group dominance"));
        x.add(
                new RobustMetricSpec(
                        "R3M005",
                        "RISK",
                        "HUBER_LOCATION",
                        0.780d,
                        0.830d,
                        "de-correlate same-window experts"));
        x.add(
                new RobustMetricSpec(
                        "R3M006",
                        "CONFIDENCE",
                        "TUKEY_BIWEIGHT",
                        0.850d,
                        0.550d,
                        "bound counter-evidence tail"));
        x.add(
                new RobustMetricSpec(
                        "R3M007",
                        "SUPPORT",
                        "HARMONIC_CONFIDENCE",
                        0.920d,
                        0.620d,
                        "stabilize sparse OOS confidence"));
        x.add(
                new RobustMetricSpec(
                        "R3M008",
                        "RISK",
                        "GEOMETRIC_CONFIDENCE",
                        0.500d,
                        0.690d,
                        "prevent one group dominance"));
        x.add(
                new RobustMetricSpec(
                        "R3M009",
                        "CONFIDENCE",
                        "ENTROPY",
                        0.570d,
                        0.760d,
                        "de-correlate same-window experts"));
        x.add(
                new RobustMetricSpec(
                        "R3M010",
                        "SUPPORT",
                        "EFFECTIVE_SAMPLE_SIZE",
                        0.640d,
                        0.830d,
                        "bound counter-evidence tail"));
        x.add(
                new RobustMetricSpec(
                        "R3M011",
                        "RISK",
                        "MAD",
                        0.710d,
                        0.550d,
                        "stabilize sparse OOS confidence"));
        x.add(
                new RobustMetricSpec(
                        "R3M012",
                        "CONFIDENCE",
                        "IQR",
                        0.780d,
                        0.620d,
                        "prevent one group dominance"));
        x.add(
                new RobustMetricSpec(
                        "R3M013",
                        "SUPPORT",
                        "PAIRWISE_DISAGREEMENT",
                        0.850d,
                        0.690d,
                        "de-correlate same-window experts"));
        x.add(
                new RobustMetricSpec(
                        "R3M014",
                        "RISK",
                        "RISK_TAIL_MEAN",
                        0.920d,
                        0.760d,
                        "bound counter-evidence tail"));
        x.add(
                new RobustMetricSpec(
                        "R3M015",
                        "CONFIDENCE",
                        "RISK_MAX",
                        0.500d,
                        0.830d,
                        "stabilize sparse OOS confidence"));
        x.add(
                new RobustMetricSpec(
                        "R3M016",
                        "SUPPORT",
                        "SUPPORT_FLOOR",
                        0.570d,
                        0.550d,
                        "prevent one group dominance"));
        x.add(
                new RobustMetricSpec(
                        "R3M017",
                        "RISK",
                        "CONFIDENCE_FLOOR",
                        0.640d,
                        0.620d,
                        "de-correlate same-window experts"));
        x.add(
                new RobustMetricSpec(
                        "R3M018",
                        "CONFIDENCE",
                        "DOMINANCE_RATIO",
                        0.710d,
                        0.690d,
                        "bound counter-evidence tail"));
        x.add(
                new RobustMetricSpec(
                        "R3M019",
                        "SUPPORT",
                        "CORRELATION_PENALTY",
                        0.780d,
                        0.760d,
                        "stabilize sparse OOS confidence"));
        x.add(
                new RobustMetricSpec(
                        "R3M020",
                        "RISK",
                        "GROUP_CAP",
                        0.850d,
                        0.830d,
                        "prevent one group dominance"));
        x.add(
                new RobustMetricSpec(
                        "R3M021",
                        "CONFIDENCE",
                        "SAMPLE_SATURATION",
                        0.920d,
                        0.550d,
                        "de-correlate same-window experts"));
        x.add(
                new RobustMetricSpec(
                        "R3M022",
                        "SUPPORT",
                        "UNCERTAINTY",
                        0.500d,
                        0.620d,
                        "bound counter-evidence tail"));
        x.add(
                new RobustMetricSpec(
                        "R3M023",
                        "RISK",
                        "NET_EVIDENCE",
                        0.570d,
                        0.690d,
                        "stabilize sparse OOS confidence"));
        x.add(
                new RobustMetricSpec(
                        "R3M024",
                        "CONFIDENCE",
                        "ROBUST_MARGIN",
                        0.640d,
                        0.760d,
                        "prevent one group dominance"));
        x.add(
                new RobustMetricSpec(
                        "R3M025",
                        "SUPPORT",
                        "WILSON_PROXY",
                        0.710d,
                        0.830d,
                        "de-correlate same-window experts"));
        x.add(
                new RobustMetricSpec(
                        "R3M026",
                        "RISK",
                        "STABILITY_SCORE",
                        0.780d,
                        0.550d,
                        "bound counter-evidence tail"));
        x.add(
                new RobustMetricSpec(
                        "R3M027",
                        "CONFIDENCE",
                        "DIVERSITY_SCORE",
                        0.850d,
                        0.620d,
                        "stabilize sparse OOS confidence"));
        x.add(
                new RobustMetricSpec(
                        "R3M028",
                        "SUPPORT",
                        "AVAILABILITY_RATIO",
                        0.920d,
                        0.690d,
                        "prevent one group dominance"));
        x.add(
                new RobustMetricSpec(
                        "R3M029",
                        "RISK",
                        "POSITIVE_RATIO",
                        0.500d,
                        0.760d,
                        "de-correlate same-window experts"));
        x.add(
                new RobustMetricSpec(
                        "R3M030",
                        "CONFIDENCE",
                        "HARD_RISK_GUARD",
                        0.570d,
                        0.830d,
                        "bound counter-evidence tail"));
        x.add(
                new RobustMetricSpec(
                        "R3M031",
                        "RISK",
                        "WEIGHTED_MEAN",
                        0.570d,
                        0.690d,
                        "bound counter-evidence tail"));
        x.add(
                new RobustMetricSpec(
                        "R3M032",
                        "CONFIDENCE",
                        "WEIGHTED_MEDIAN",
                        0.640d,
                        0.760d,
                        "stabilize sparse OOS confidence"));
        x.add(
                new RobustMetricSpec(
                        "R3M033",
                        "SUPPORT",
                        "TRIMMED_MEAN",
                        0.710d,
                        0.830d,
                        "prevent one group dominance"));
        x.add(
                new RobustMetricSpec(
                        "R3M034",
                        "RISK",
                        "WINSOR_MEAN",
                        0.780d,
                        0.550d,
                        "de-correlate same-window experts"));
        x.add(
                new RobustMetricSpec(
                        "R3M035",
                        "CONFIDENCE",
                        "HUBER_LOCATION",
                        0.850d,
                        0.620d,
                        "bound counter-evidence tail"));
        x.add(
                new RobustMetricSpec(
                        "R3M036",
                        "SUPPORT",
                        "TUKEY_BIWEIGHT",
                        0.920d,
                        0.690d,
                        "stabilize sparse OOS confidence"));
        x.add(
                new RobustMetricSpec(
                        "R3M037",
                        "RISK",
                        "HARMONIC_CONFIDENCE",
                        0.500d,
                        0.760d,
                        "prevent one group dominance"));
        x.add(
                new RobustMetricSpec(
                        "R3M038",
                        "CONFIDENCE",
                        "GEOMETRIC_CONFIDENCE",
                        0.570d,
                        0.830d,
                        "de-correlate same-window experts"));
        x.add(
                new RobustMetricSpec(
                        "R3M039",
                        "SUPPORT",
                        "ENTROPY",
                        0.640d,
                        0.550d,
                        "bound counter-evidence tail"));
        x.add(
                new RobustMetricSpec(
                        "R3M040",
                        "RISK",
                        "EFFECTIVE_SAMPLE_SIZE",
                        0.710d,
                        0.620d,
                        "stabilize sparse OOS confidence"));
        x.add(
                new RobustMetricSpec(
                        "R3M041",
                        "CONFIDENCE",
                        "MAD",
                        0.780d,
                        0.690d,
                        "prevent one group dominance"));
        x.add(
                new RobustMetricSpec(
                        "R3M042",
                        "SUPPORT",
                        "IQR",
                        0.850d,
                        0.760d,
                        "de-correlate same-window experts"));
        x.add(
                new RobustMetricSpec(
                        "R3M043",
                        "RISK",
                        "PAIRWISE_DISAGREEMENT",
                        0.920d,
                        0.830d,
                        "bound counter-evidence tail"));
        x.add(
                new RobustMetricSpec(
                        "R3M044",
                        "CONFIDENCE",
                        "RISK_TAIL_MEAN",
                        0.500d,
                        0.550d,
                        "stabilize sparse OOS confidence"));
        x.add(
                new RobustMetricSpec(
                        "R3M045",
                        "SUPPORT",
                        "RISK_MAX",
                        0.570d,
                        0.620d,
                        "prevent one group dominance"));
        x.add(
                new RobustMetricSpec(
                        "R3M046",
                        "RISK",
                        "SUPPORT_FLOOR",
                        0.640d,
                        0.690d,
                        "de-correlate same-window experts"));
        x.add(
                new RobustMetricSpec(
                        "R3M047",
                        "CONFIDENCE",
                        "CONFIDENCE_FLOOR",
                        0.710d,
                        0.760d,
                        "bound counter-evidence tail"));
        x.add(
                new RobustMetricSpec(
                        "R3M048",
                        "SUPPORT",
                        "DOMINANCE_RATIO",
                        0.780d,
                        0.830d,
                        "stabilize sparse OOS confidence"));
        x.add(
                new RobustMetricSpec(
                        "R3M049",
                        "RISK",
                        "CORRELATION_PENALTY",
                        0.850d,
                        0.550d,
                        "prevent one group dominance"));
        x.add(
                new RobustMetricSpec(
                        "R3M050",
                        "CONFIDENCE",
                        "GROUP_CAP",
                        0.920d,
                        0.620d,
                        "de-correlate same-window experts"));
        x.add(
                new RobustMetricSpec(
                        "R3M051",
                        "SUPPORT",
                        "SAMPLE_SATURATION",
                        0.500d,
                        0.690d,
                        "bound counter-evidence tail"));
        x.add(
                new RobustMetricSpec(
                        "R3M052",
                        "RISK",
                        "UNCERTAINTY",
                        0.570d,
                        0.760d,
                        "stabilize sparse OOS confidence"));
        x.add(
                new RobustMetricSpec(
                        "R3M053",
                        "CONFIDENCE",
                        "NET_EVIDENCE",
                        0.640d,
                        0.830d,
                        "prevent one group dominance"));
        x.add(
                new RobustMetricSpec(
                        "R3M054",
                        "SUPPORT",
                        "ROBUST_MARGIN",
                        0.710d,
                        0.550d,
                        "de-correlate same-window experts"));
        x.add(
                new RobustMetricSpec(
                        "R3M055",
                        "RISK",
                        "WILSON_PROXY",
                        0.780d,
                        0.620d,
                        "bound counter-evidence tail"));
        x.add(
                new RobustMetricSpec(
                        "R3M056",
                        "CONFIDENCE",
                        "STABILITY_SCORE",
                        0.850d,
                        0.690d,
                        "stabilize sparse OOS confidence"));
        x.add(
                new RobustMetricSpec(
                        "R3M057",
                        "SUPPORT",
                        "DIVERSITY_SCORE",
                        0.920d,
                        0.760d,
                        "prevent one group dominance"));
        x.add(
                new RobustMetricSpec(
                        "R3M058",
                        "RISK",
                        "AVAILABILITY_RATIO",
                        0.500d,
                        0.830d,
                        "de-correlate same-window experts"));
        x.add(
                new RobustMetricSpec(
                        "R3M059",
                        "CONFIDENCE",
                        "POSITIVE_RATIO",
                        0.570d,
                        0.550d,
                        "bound counter-evidence tail"));
        x.add(
                new RobustMetricSpec(
                        "R3M060",
                        "SUPPORT",
                        "HARD_RISK_GUARD",
                        0.640d,
                        0.620d,
                        "stabilize sparse OOS confidence"));
        x.add(
                new RobustMetricSpec(
                        "R3M061",
                        "CONFIDENCE",
                        "WEIGHTED_MEAN",
                        0.640d,
                        0.830d,
                        "stabilize sparse OOS confidence"));
        x.add(
                new RobustMetricSpec(
                        "R3M062",
                        "SUPPORT",
                        "WEIGHTED_MEDIAN",
                        0.710d,
                        0.550d,
                        "prevent one group dominance"));
        x.add(
                new RobustMetricSpec(
                        "R3M063",
                        "RISK",
                        "TRIMMED_MEAN",
                        0.780d,
                        0.620d,
                        "de-correlate same-window experts"));
        x.add(
                new RobustMetricSpec(
                        "R3M064",
                        "CONFIDENCE",
                        "WINSOR_MEAN",
                        0.850d,
                        0.690d,
                        "bound counter-evidence tail"));
        x.add(
                new RobustMetricSpec(
                        "R3M065",
                        "SUPPORT",
                        "HUBER_LOCATION",
                        0.920d,
                        0.760d,
                        "stabilize sparse OOS confidence"));
        x.add(
                new RobustMetricSpec(
                        "R3M066",
                        "RISK",
                        "TUKEY_BIWEIGHT",
                        0.500d,
                        0.830d,
                        "prevent one group dominance"));
        x.add(
                new RobustMetricSpec(
                        "R3M067",
                        "CONFIDENCE",
                        "HARMONIC_CONFIDENCE",
                        0.570d,
                        0.550d,
                        "de-correlate same-window experts"));
        x.add(
                new RobustMetricSpec(
                        "R3M068",
                        "SUPPORT",
                        "GEOMETRIC_CONFIDENCE",
                        0.640d,
                        0.620d,
                        "bound counter-evidence tail"));
        x.add(
                new RobustMetricSpec(
                        "R3M069",
                        "RISK",
                        "ENTROPY",
                        0.710d,
                        0.690d,
                        "stabilize sparse OOS confidence"));
        x.add(
                new RobustMetricSpec(
                        "R3M070",
                        "CONFIDENCE",
                        "EFFECTIVE_SAMPLE_SIZE",
                        0.780d,
                        0.760d,
                        "prevent one group dominance"));
        x.add(
                new RobustMetricSpec(
                        "R3M071",
                        "SUPPORT",
                        "MAD",
                        0.850d,
                        0.830d,
                        "de-correlate same-window experts"));
        x.add(
                new RobustMetricSpec(
                        "R3M072", "RISK", "IQR", 0.920d, 0.550d, "bound counter-evidence tail"));
        x.add(
                new RobustMetricSpec(
                        "R3M073",
                        "CONFIDENCE",
                        "PAIRWISE_DISAGREEMENT",
                        0.500d,
                        0.620d,
                        "stabilize sparse OOS confidence"));
        x.add(
                new RobustMetricSpec(
                        "R3M074",
                        "SUPPORT",
                        "RISK_TAIL_MEAN",
                        0.570d,
                        0.690d,
                        "prevent one group dominance"));
        x.add(
                new RobustMetricSpec(
                        "R3M075",
                        "RISK",
                        "RISK_MAX",
                        0.640d,
                        0.760d,
                        "de-correlate same-window experts"));
        x.add(
                new RobustMetricSpec(
                        "R3M076",
                        "CONFIDENCE",
                        "SUPPORT_FLOOR",
                        0.710d,
                        0.830d,
                        "bound counter-evidence tail"));
        x.add(
                new RobustMetricSpec(
                        "R3M077",
                        "SUPPORT",
                        "CONFIDENCE_FLOOR",
                        0.780d,
                        0.550d,
                        "stabilize sparse OOS confidence"));
        x.add(
                new RobustMetricSpec(
                        "R3M078",
                        "RISK",
                        "DOMINANCE_RATIO",
                        0.850d,
                        0.620d,
                        "prevent one group dominance"));
        x.add(
                new RobustMetricSpec(
                        "R3M079",
                        "CONFIDENCE",
                        "CORRELATION_PENALTY",
                        0.920d,
                        0.690d,
                        "de-correlate same-window experts"));
        x.add(
                new RobustMetricSpec(
                        "R3M080",
                        "SUPPORT",
                        "GROUP_CAP",
                        0.500d,
                        0.760d,
                        "bound counter-evidence tail"));
        x.add(
                new RobustMetricSpec(
                        "R3M081",
                        "RISK",
                        "SAMPLE_SATURATION",
                        0.570d,
                        0.830d,
                        "stabilize sparse OOS confidence"));
        x.add(
                new RobustMetricSpec(
                        "R3M082",
                        "CONFIDENCE",
                        "UNCERTAINTY",
                        0.640d,
                        0.550d,
                        "prevent one group dominance"));
        x.add(
                new RobustMetricSpec(
                        "R3M083",
                        "SUPPORT",
                        "NET_EVIDENCE",
                        0.710d,
                        0.620d,
                        "de-correlate same-window experts"));
        x.add(
                new RobustMetricSpec(
                        "R3M084",
                        "RISK",
                        "ROBUST_MARGIN",
                        0.780d,
                        0.690d,
                        "bound counter-evidence tail"));
        x.add(
                new RobustMetricSpec(
                        "R3M085",
                        "CONFIDENCE",
                        "WILSON_PROXY",
                        0.850d,
                        0.760d,
                        "stabilize sparse OOS confidence"));
        x.add(
                new RobustMetricSpec(
                        "R3M086",
                        "SUPPORT",
                        "STABILITY_SCORE",
                        0.920d,
                        0.830d,
                        "prevent one group dominance"));
        x.add(
                new RobustMetricSpec(
                        "R3M087",
                        "RISK",
                        "DIVERSITY_SCORE",
                        0.500d,
                        0.550d,
                        "de-correlate same-window experts"));
        x.add(
                new RobustMetricSpec(
                        "R3M088",
                        "CONFIDENCE",
                        "AVAILABILITY_RATIO",
                        0.570d,
                        0.620d,
                        "bound counter-evidence tail"));
        x.add(
                new RobustMetricSpec(
                        "R3M089",
                        "SUPPORT",
                        "POSITIVE_RATIO",
                        0.640d,
                        0.690d,
                        "stabilize sparse OOS confidence"));
        x.add(
                new RobustMetricSpec(
                        "R3M090",
                        "RISK",
                        "HARD_RISK_GUARD",
                        0.710d,
                        0.760d,
                        "prevent one group dominance"));
        x.add(
                new RobustMetricSpec(
                        "R3M091",
                        "SUPPORT",
                        "WEIGHTED_MEAN",
                        0.710d,
                        0.620d,
                        "prevent one group dominance"));
        x.add(
                new RobustMetricSpec(
                        "R3M092",
                        "RISK",
                        "WEIGHTED_MEDIAN",
                        0.780d,
                        0.690d,
                        "de-correlate same-window experts"));
        x.add(
                new RobustMetricSpec(
                        "R3M093",
                        "CONFIDENCE",
                        "TRIMMED_MEAN",
                        0.850d,
                        0.760d,
                        "bound counter-evidence tail"));
        x.add(
                new RobustMetricSpec(
                        "R3M094",
                        "SUPPORT",
                        "WINSOR_MEAN",
                        0.920d,
                        0.830d,
                        "stabilize sparse OOS confidence"));
        x.add(
                new RobustMetricSpec(
                        "R3M095",
                        "RISK",
                        "HUBER_LOCATION",
                        0.500d,
                        0.550d,
                        "prevent one group dominance"));
        x.add(
                new RobustMetricSpec(
                        "R3M096",
                        "CONFIDENCE",
                        "TUKEY_BIWEIGHT",
                        0.570d,
                        0.620d,
                        "de-correlate same-window experts"));
        x.add(
                new RobustMetricSpec(
                        "R3M097",
                        "SUPPORT",
                        "HARMONIC_CONFIDENCE",
                        0.640d,
                        0.690d,
                        "bound counter-evidence tail"));
        x.add(
                new RobustMetricSpec(
                        "R3M098",
                        "RISK",
                        "GEOMETRIC_CONFIDENCE",
                        0.710d,
                        0.760d,
                        "stabilize sparse OOS confidence"));
        x.add(
                new RobustMetricSpec(
                        "R3M099",
                        "CONFIDENCE",
                        "ENTROPY",
                        0.780d,
                        0.830d,
                        "prevent one group dominance"));
        x.add(
                new RobustMetricSpec(
                        "R3M100",
                        "SUPPORT",
                        "EFFECTIVE_SAMPLE_SIZE",
                        0.850d,
                        0.550d,
                        "de-correlate same-window experts"));
        x.add(
                new RobustMetricSpec(
                        "R3M101", "RISK", "MAD", 0.920d, 0.620d, "bound counter-evidence tail"));
        x.add(
                new RobustMetricSpec(
                        "R3M102",
                        "CONFIDENCE",
                        "IQR",
                        0.500d,
                        0.690d,
                        "stabilize sparse OOS confidence"));
        x.add(
                new RobustMetricSpec(
                        "R3M103",
                        "SUPPORT",
                        "PAIRWISE_DISAGREEMENT",
                        0.570d,
                        0.760d,
                        "prevent one group dominance"));
        x.add(
                new RobustMetricSpec(
                        "R3M104",
                        "RISK",
                        "RISK_TAIL_MEAN",
                        0.640d,
                        0.830d,
                        "de-correlate same-window experts"));
        x.add(
                new RobustMetricSpec(
                        "R3M105",
                        "CONFIDENCE",
                        "RISK_MAX",
                        0.710d,
                        0.550d,
                        "bound counter-evidence tail"));
        x.add(
                new RobustMetricSpec(
                        "R3M106",
                        "SUPPORT",
                        "SUPPORT_FLOOR",
                        0.780d,
                        0.620d,
                        "stabilize sparse OOS confidence"));
        x.add(
                new RobustMetricSpec(
                        "R3M107",
                        "RISK",
                        "CONFIDENCE_FLOOR",
                        0.850d,
                        0.690d,
                        "prevent one group dominance"));
        x.add(
                new RobustMetricSpec(
                        "R3M108",
                        "CONFIDENCE",
                        "DOMINANCE_RATIO",
                        0.920d,
                        0.760d,
                        "de-correlate same-window experts"));
        x.add(
                new RobustMetricSpec(
                        "R3M109",
                        "SUPPORT",
                        "CORRELATION_PENALTY",
                        0.500d,
                        0.830d,
                        "bound counter-evidence tail"));
        x.add(
                new RobustMetricSpec(
                        "R3M110",
                        "RISK",
                        "GROUP_CAP",
                        0.570d,
                        0.550d,
                        "stabilize sparse OOS confidence"));
        x.add(
                new RobustMetricSpec(
                        "R3M111",
                        "CONFIDENCE",
                        "SAMPLE_SATURATION",
                        0.640d,
                        0.620d,
                        "prevent one group dominance"));
        x.add(
                new RobustMetricSpec(
                        "R3M112",
                        "SUPPORT",
                        "UNCERTAINTY",
                        0.710d,
                        0.690d,
                        "de-correlate same-window experts"));
        x.add(
                new RobustMetricSpec(
                        "R3M113",
                        "RISK",
                        "NET_EVIDENCE",
                        0.780d,
                        0.760d,
                        "bound counter-evidence tail"));
        x.add(
                new RobustMetricSpec(
                        "R3M114",
                        "CONFIDENCE",
                        "ROBUST_MARGIN",
                        0.850d,
                        0.830d,
                        "stabilize sparse OOS confidence"));
        x.add(
                new RobustMetricSpec(
                        "R3M115",
                        "SUPPORT",
                        "WILSON_PROXY",
                        0.920d,
                        0.550d,
                        "prevent one group dominance"));
        x.add(
                new RobustMetricSpec(
                        "R3M116",
                        "RISK",
                        "STABILITY_SCORE",
                        0.500d,
                        0.620d,
                        "de-correlate same-window experts"));
        x.add(
                new RobustMetricSpec(
                        "R3M117",
                        "CONFIDENCE",
                        "DIVERSITY_SCORE",
                        0.570d,
                        0.690d,
                        "bound counter-evidence tail"));
        x.add(
                new RobustMetricSpec(
                        "R3M118",
                        "SUPPORT",
                        "AVAILABILITY_RATIO",
                        0.640d,
                        0.760d,
                        "stabilize sparse OOS confidence"));
        x.add(
                new RobustMetricSpec(
                        "R3M119",
                        "RISK",
                        "POSITIVE_RATIO",
                        0.710d,
                        0.830d,
                        "prevent one group dominance"));
        x.add(
                new RobustMetricSpec(
                        "R3M120",
                        "CONFIDENCE",
                        "HARD_RISK_GUARD",
                        0.780d,
                        0.550d,
                        "de-correlate same-window experts"));
        return x;
    }

    public static final class R3RobustAggregation {
        static void enrich(Snapshot out, Group[] groups) {
            if (out == null || groups == null) return;
            double[] s = collect(groups, 0),
                    r = collect(groups, 1),
                    c = collect(groups, 2),
                    w = weights(groups);
            out.robustSupport =
                    clamp01(
                            0.28d * weightedMean(s, w)
                                    + 0.22d * weightedMedian(s, w)
                                    + 0.18d * trimmedMean(s, 0.20d)
                                    + 0.17d * huber(s, 1.5d)
                                    + 0.15d * tukey(s));
            out.robustRisk =
                    clamp01(
                            0.25d * weightedMean(r, w)
                                    + 0.20d * weightedMedian(r, w)
                                    + 0.20d * tailMean(r, 0.40d)
                                    + 0.20d * max(r)
                                    + 0.15d * huber(r, 1.2d));
            out.robustConfidence =
                    clamp01(
                            0.35d * weightedMean(c, w)
                                    + 0.25d * geometric(c)
                                    + 0.20d * (1.0d - disagreement(s))
                                    + 0.20d * availability(groups));
            out.effectiveGroupCount = effectiveSampleSize(w);
            out.disagreement = disagreement(s);
            out.robustMethod = "WMED+TRIM+HUBER+TUKEY+TAIL+ESS";
            out.supportMean = clamp01(0.55d * out.supportMean + 0.45d * out.robustSupport);
            out.riskMean =
                    clamp01(
                            Math.max(
                                    out.riskMean * 0.60d + out.robustRisk * 0.40d,
                                    out.robustRisk * 0.85d));
            out.confidenceMean = clamp01(0.55d * out.confidenceMean + 0.45d * out.robustConfidence);
        }

        private static double[] collect(Group[] g, int kind) {
            java.util.ArrayList<Double> x = new java.util.ArrayList<Double>();
            for (int i = 0; i < g.length; i++) {
                if (g[i] == null || !g[i].available) continue;
                x.add(
                        Double.valueOf(
                                kind == 0
                                        ? g[i].support
                                        : (kind == 1 ? g[i].risk : g[i].confidence)));
            }
            double[] a = new double[x.size()];
            for (int i = 0; i < a.length; i++) a[i] = x.get(i).doubleValue();
            return a;
        }

        private static double[] weights(Group[] g) {
            java.util.ArrayList<Double> x = new java.util.ArrayList<Double>();
            for (int i = 0; i < g.length; i++) {
                if (g[i] == null || !g[i].available) continue;
                double sample = Math.min(1.0d, Math.sqrt(Math.max(1, g[i].sample) / 120.0d));
                x.add(Double.valueOf(Math.max(0.05d, g[i].confidence * (0.35d + 0.65d * sample))));
            }
            double[] a = new double[x.size()];
            for (int i = 0; i < a.length; i++) a[i] = x.get(i).doubleValue();
            return a;
        }

        private static double weightedMean(double[] x, double[] w) {
            if (x.length == 0) return 0;
            double s = 0, z = 0;
            for (int i = 0; i < x.length; i++) {
                double q = i < w.length ? w[i] : 1;
                s += x[i] * q;
                z += q;
            }
            return z <= 0 ? 0 : s / z;
        }

        private static double weightedMedian(double[] x, double[] w) {
            if (x.length == 0) return 0;
            double[][] a = new double[x.length][2];
            double z = 0;
            for (int i = 0; i < x.length; i++) {
                a[i][0] = x[i];
                a[i][1] = i < w.length ? w[i] : 1;
                z += a[i][1];
            }
            java.util.Arrays.sort(
                    a,
                    new java.util.Comparator<double[]>() {
                        public int compare(double[] p, double[] q) {
                            return Double.compare(p[0], q[0]);
                        }
                    });
            double c = 0;
            for (int i = 0; i < a.length; i++) {
                c += a[i][1];
                if (c >= z * 0.5d) return a[i][0];
            }
            return a[a.length - 1][0];
        }

        private static double trimmedMean(double[] x, double trim) {
            if (x.length == 0) return 0;
            double[] a = x.clone();
            java.util.Arrays.sort(a);
            int cut = (int) Math.floor(a.length * clamp01(trim));
            if (cut * 2 >= a.length) cut = 0;
            double s = 0;
            int n = 0;
            for (int i = cut; i < a.length - cut; i++) {
                s += a[i];
                n++;
            }
            return n == 0 ? 0 : s / n;
        }

        private static double huber(double[] x, double k) {
            if (x.length == 0) return 0;
            double m = median(x);
            for (int it = 0; it < 8; it++) {
                double scale = mad(x, m) + 1e-9d, num = 0, den = 0;
                for (int i = 0; i < x.length; i++) {
                    double r = (x[i] - m) / scale, w = Math.abs(r) <= k ? 1 : k / Math.abs(r);
                    num += x[i] * w;
                    den += w;
                }
                if (den > 0) m = num / den;
            }
            return clamp01(m);
        }

        private static double tukey(double[] x) {
            if (x.length == 0) return 0;
            double m = median(x), scale = mad(x, m) + 1e-9d, num = 0, den = 0;
            for (int i = 0; i < x.length; i++) {
                double u = (x[i] - m) / (4.685d * scale);
                if (Math.abs(u) >= 1) continue;
                double w = 1 - u * u;
                w *= w;
                num += x[i] * w;
                den += w;
            }
            return den <= 0 ? clamp01(m) : clamp01(num / den);
        }

        private static double tailMean(double[] x, double frac) {
            if (x.length == 0) return 0;
            double[] a = x.clone();
            java.util.Arrays.sort(a);
            int start = Math.max(0, (int) Math.floor(a.length * (1 - clamp01(frac))));
            double s = 0;
            int n = 0;
            for (int i = start; i < a.length; i++) {
                s += a[i];
                n++;
            }
            return n == 0 ? 0 : s / n;
        }

        private static double max(double[] x) {
            double m = 0;
            for (int i = 0; i < x.length; i++) m = Math.max(m, x[i]);
            return m;
        }

        private static double median(double[] x) {
            if (x.length == 0) return 0;
            double[] a = x.clone();
            java.util.Arrays.sort(a);
            int n = a.length;
            return n % 2 == 1 ? a[n / 2] : (a[n / 2 - 1] + a[n / 2]) * 0.5d;
        }

        private static double mad(double[] x, double center) {
            if (x.length == 0) return 0;
            double[] d = new double[x.length];
            for (int i = 0; i < x.length; i++) d[i] = Math.abs(x[i] - center);
            return median(d);
        }

        private static double geometric(double[] x) {
            if (x.length == 0) return 0;
            double s = 0;
            for (int i = 0; i < x.length; i++) s += Math.log(Math.max(1e-6d, x[i]));
            return Math.exp(s / x.length);
        }

        private static double disagreement(double[] x) {
            if (x.length <= 1) return 1;
            double m = 0;
            for (int i = 0; i < x.length; i++) m += x[i];
            m /= x.length;
            double v = 0;
            for (int i = 0; i < x.length; i++) {
                double d = x[i] - m;
                v += d * d;
            }
            return clamp01(Math.sqrt(v / x.length) * 2.5d);
        }

        private static double effectiveSampleSize(double[] w) {
            double s = 0, q = 0;
            for (int i = 0; i < w.length; i++) {
                s += w[i];
                q += w[i] * w[i];
            }
            return q <= 0 ? 0 : s * s / q;
        }

        private static double availability(Group[] g) {
            int n = 0;
            for (int i = 0; i < g.length; i++) if (g[i] != null && g[i].available) n++;
            return g.length == 0 ? 0 : (double) n / g.length;
        }
    }
}
