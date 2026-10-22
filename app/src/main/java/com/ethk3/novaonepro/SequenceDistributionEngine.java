package com.ethk3.novaonepro;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * V40 strictly pre-draw full-distribution forecaster.
 *
 * <p>The engine predicts a complete ten-number distribution for every position and only then
 * derives a K=3 event probability by summing the three selected number probabilities. It never
 * consumes settlement fields, BET/HOLD/SHADOW mode, frequency debt, Teacher labels or post-draw
 * diagnostics. Every feature is a deterministic function of draws whose issue is not later than the
 * base issue.
 *
 * <p>The component models are deliberately low freedom and auditable:
 *
 * <ul>
 *   <li>multi-window frequency with Dirichlet smoothing,
 *   <li>exponentially weighted frequency,
 *   <li>same-position first-order and second-order transitions,
 *   <li>number movement between positions,
 *   <li>omission-gap hazard.
 * </ul>
 *
 * Each component is normalized to a ten-class probability distribution. Sparse components are
 * automatically shrunk by their own support and the final ensemble is shrunk toward the uniform 10%
 * number prior according to history maturity and component agreement. This prevents a descriptive
 * score from being mislabeled as an event probability.
 */
public final class SequenceDistributionEngine {
    public static final String V46_ROLE = "FULL_TEN_NUMBER_EVENT_DISTRIBUTION_OWNER";
    public static final String MODULE_VERSION = AppConfig.SEQUENCE_DISTRIBUTION_VERSION;
    public static final int COMPONENT_BASE = 0;
    public static final int COMPONENT_RECENT = 1;
    public static final int COMPONENT_EWMA = 2;
    public static final int COMPONENT_MARKOV = 3;
    public static final int COMPONENT_PAIR = 4;
    public static final int COMPONENT_MOVEMENT = 5;
    public static final int COMPONENT_GAP = 6;
    public static final int COMPONENT_COUNT = 7;

    public static final class Forecast {
        public final double[][] probability =
                new double[AppConfig.POSITION_COUNT][AppConfig.NUMBER_COUNT];
        public final double[][][] component =
                new double[COMPONENT_COUNT][AppConfig.POSITION_COUNT][AppConfig.NUMBER_COUNT];
        public final double[][] componentReliability =
                new double[COMPONENT_COUNT][AppConfig.POSITION_COUNT];
        public final double[] positionEntropy = new double[AppConfig.POSITION_COUNT];
        public final double[] positionDrift = new double[AppConfig.POSITION_COUNT];
        public final double[] positionAgreement = new double[AppConfig.POSITION_COUNT];
        public final String[] regimeKey = new String[AppConfig.POSITION_COUNT];
        public final int[] lastNumber = new int[AppConfig.POSITION_COUNT];
        public int historySize;
        public String baseIssue = "";
        public long costMs;
        public String status = "EMPTY";
        public String version = MODULE_VERSION;

        public double numberProbability(int position, int number) {
            if (!AppConfig.isValidPosition(position) || !AppConfig.isValidNumber(number))
                return 0.10d;
            return probability[position - 1][number - 1];
        }

        public double comboProbability(int position, int n1, int n2, int n3) {
            if (!Models.isValidK3Numbers(n1, n2, n3) || !AppConfig.isValidPosition(position)) {
                return AppConfig.RANDOM_K3_BASELINE;
            }
            return clamp(
                    numberProbability(position, n1)
                            + numberProbability(position, n2)
                            + numberProbability(position, n3),
                    0.0d,
                    1.0d);
        }

        public double componentComboProbability(
                int componentIndex, int position, int n1, int n2, int n3) {
            if (componentIndex < 0
                    || componentIndex >= COMPONENT_COUNT
                    || !AppConfig.isValidPosition(position)
                    || !Models.isValidK3Numbers(n1, n2, n3)) {
                return AppConfig.RANDOM_K3_BASELINE;
            }
            int p = position - 1;
            return clamp(
                    component[componentIndex][p][n1 - 1]
                            + component[componentIndex][p][n2 - 1]
                            + component[componentIndex][p][n3 - 1],
                    0.0d,
                    1.0d);
        }
    }

    public Forecast build(List<Models.Draw> input) {
        long started = System.currentTimeMillis();
        ArrayList<Models.Draw> history = cleanHistory(input, AppConfig.SEQUENCE_MAX_HISTORY);
        Forecast out = new Forecast();
        out.historySize = history.size();
        if (history.size() > 0) {
            Models.Draw last = history.get(history.size() - 1);
            out.baseIssue = safe(last.issue);
            for (int p = 0; p < AppConfig.POSITION_COUNT; p++) {
                out.lastNumber[p] = last.getNumberAtPosition(p + 1);
            }
        }
        initializeUniform(out);
        if (history.size() < 2) {
            out.status = history.size() == 0 ? "EMPTY" : "BASELINE_ONLY";
            out.costMs = System.currentTimeMillis() - started;
            return out;
        }

        buildRecent(history, out);
        buildEwma(history, out);
        buildMarkov(history, out);
        buildPairTransition(history, out);
        buildMovement(history, out);
        buildGapHazard(history, out);
        combine(out);
        buildRegimes(history, out);
        out.status = history.size() >= AppConfig.SEQUENCE_MIN_HISTORY ? "READY" : "WARM";
        out.costMs = System.currentTimeMillis() - started;
        return out;
    }

    public void applyToCandidate(Forecast forecast, Models.ComboCandidate c) {
        if (c == null || !c.isValidK3()) return;
        Forecast f = forecast == null ? new Forecast() : forecast;
        int p = c.position - 1;
        double eventP = f.comboProbability(c.position, c.n1, c.n2, c.n3);
        double recentP =
                f.componentComboProbability(COMPONENT_RECENT, c.position, c.n1, c.n2, c.n3);
        double ewmaP = f.componentComboProbability(COMPONENT_EWMA, c.position, c.n1, c.n2, c.n3);
        double markovP =
                f.componentComboProbability(COMPONENT_MARKOV, c.position, c.n1, c.n2, c.n3);
        double pairP = f.componentComboProbability(COMPONENT_PAIR, c.position, c.n1, c.n2, c.n3);
        double movementP =
                f.componentComboProbability(COMPONENT_MOVEMENT, c.position, c.n1, c.n2, c.n3);
        double gapP = f.componentComboProbability(COMPONENT_GAP, c.position, c.n1, c.n2, c.n3);
        double dispersion =
                standardDeviation(new double[] {recentP, ewmaP, markovP, pairP, movementP, gapP});

        /* modelP/rawP are event probabilities. rawRankScore is only an ordering score. */
        c.basePBeforeFusion = eventP;
        c.modelP = eventP;
        c.rawP = eventP;
        c.deepFeatureP = eventP;
        c.evidenceForestP = eventP;
        c.evidenceForestConfidence =
                p >= 0 && p < f.positionAgreement.length ? f.positionAgreement[p] : 0.0d;
        c.evidenceForestSample = f.historySize;
        c.evidenceForestTreeCount = COMPONENT_COUNT - 1;
        c.historicalPriorP = recentP;
        c.historicalPriorConfidence =
                p >= 0 && p < f.positionAgreement.length ? f.positionAgreement[p] : 0.0d;
        c.historicalPriorSample = f.historySize;
        c.similarP = Math.max(c.similarP, ewmaP);
        c.memoryP = Math.max(c.memoryP, markovP);
        c.calibrationError = dispersion;
        c.deepFeatureRisk = Math.max(c.deepFeatureRisk, clamp(dispersion / 0.12d, 0.0d, 1.0d));
        c.evidenceAsOfIssue = f.baseIssue;
        c.snapshotAsOfIssue = f.baseIssue;
        c.historicalPriorCoarseKey = p >= 0 && p < f.regimeKey.length ? f.regimeKey[p] : "";
        c.historicalPriorMidKey =
                c.historicalPriorCoarseKey + "|K3SPAN=" + spanBand(c.n1, c.n2, c.n3);
        c.probabilityWindowStateKey = c.historicalPriorMidKey;
        c.finalDecisionWindowStateKey = c.probabilityWindowStateKey;
        if (safe(c.windowStateKey).length() == 0) c.windowStateKey = c.probabilityWindowStateKey;
        if (safe(c.stateKey).length() == 0) c.stateKey = c.probabilityWindowStateKey;

        double structural = structuralScore(c);
        double probabilityRank =
                clamp(
                        0.50d
                                + (eventP - AppConfig.RANDOM_K3_BASELINE)
                                        / AppConfig.SEQUENCE_RANK_PROBABILITY_SCALE,
                        0.0d,
                        1.0d);
        c.rawRankScore =
                clamp(
                        AppConfig.SEQUENCE_RANK_PROBABILITY_WEIGHT * probabilityRank
                                + (1.0d - AppConfig.SEQUENCE_RANK_PROBABILITY_WEIGHT) * structural,
                        0.0d,
                        1.0d);
        c.evidenceScore =
                Math.max(
                        c.evidenceScore,
                        clamp(
                                0.55d * probabilityRank
                                        + 0.45d * f.positionAgreement[Math.max(0, Math.min(p, 9))],
                                0.0d,
                                1.0d));
        c.fusionWeightBase = AppConfig.SEQUENCE_WEIGHT_BASE;
        c.fusionWeightPrior = AppConfig.SEQUENCE_WEIGHT_RECENT + AppConfig.SEQUENCE_WEIGHT_EWMA;
        c.fusionWeightDynamic = AppConfig.SEQUENCE_WEIGHT_MARKOV + AppConfig.SEQUENCE_WEIGHT_PAIR;
        c.fusionWeightForest = AppConfig.SEQUENCE_WEIGHT_MOVEMENT + AppConfig.SEQUENCE_WEIGHT_GAP;
        c.fusionSummary =
                buildCandidateSummary(
                        f, c, eventP, recentP, ewmaP, markovP, pairP, movementP, gapP, dispersion);
        c.evidenceForestSummary = c.fusionSummary;
        c.probabilityCoverageSummary = append(c.probabilityCoverageSummary, c.fusionSummary);
    }

    public static String buildCandidateSummary(
            Forecast f,
            Models.ComboCandidate c,
            double eventP,
            double recentP,
            double ewmaP,
            double markovP,
            double pairP,
            double movementP,
            double gapP,
            double dispersion) {
        int p = c == null ? -1 : c.position - 1;
        String regime = p >= 0 && p < f.regimeKey.length ? f.regimeKey[p] : "";
        double agreement = p >= 0 && p < f.positionAgreement.length ? f.positionAgreement[p] : 0.0d;
        return "V40全分布概率：P="
                + pct(eventP)
                + "｜近期="
                + pct(recentP)
                + "｜EWMA="
                + pct(ewmaP)
                + "｜Markov="
                + pct(markovP)
                + "｜Pair="
                + pct(pairP)
                + "｜Move="
                + pct(movementP)
                + "｜Gap="
                + pct(gapP)
                + "｜分歧="
                + pct(dispersion)
                + "｜一致="
                + pct(agreement)
                + "｜历史="
                + f.historySize
                + "｜Regime="
                + regime
                + "｜纪律=先预测10类分布再求K3事件概率";
    }

    private void initializeUniform(Forecast out) {
        for (int k = 0; k < COMPONENT_COUNT; k++) {
            for (int p = 0; p < AppConfig.POSITION_COUNT; p++) {
                out.componentReliability[k][p] = k == COMPONENT_BASE ? 1.0d : 0.0d;
                for (int n = 0; n < AppConfig.NUMBER_COUNT; n++) {
                    out.component[k][p][n] = 0.10d;
                    out.probability[p][n] = 0.10d;
                }
            }
        }
    }

    private void buildRecent(ArrayList<Models.Draw> h, Forecast out) {
        int[] windows =
                new int[] {
                    AppConfig.SEQUENCE_WINDOW_12,
                    AppConfig.SEQUENCE_WINDOW_24,
                    AppConfig.SEQUENCE_WINDOW_48,
                    AppConfig.SEQUENCE_WINDOW_96,
                    AppConfig.SEQUENCE_WINDOW_360
                };
        double[] weights = new double[] {0.20d, 0.22d, 0.22d, 0.20d, 0.16d};
        for (int p = 0; p < AppConfig.POSITION_COUNT; p++) {
            double[] mix = new double[AppConfig.NUMBER_COUNT];
            double totalWeight = 0.0d;
            for (int wi = 0; wi < windows.length; wi++) {
                int take = Math.min(windows[wi], h.size());
                double[] counts = new double[AppConfig.NUMBER_COUNT];
                for (int n = 0; n < counts.length; n++)
                    counts[n] = AppConfig.SEQUENCE_DIRICHLET_ALPHA;
                for (int i = h.size() - take; i < h.size(); i++) {
                    int number = h.get(i).getNumberAtPosition(p + 1);
                    if (AppConfig.isValidNumber(number)) counts[number - 1] += 1.0d;
                }
                normalize(counts);
                double w = weights[wi] * Math.min(1.0d, take / (double) Math.max(1, windows[wi]));
                for (int n = 0; n < counts.length; n++) mix[n] += w * counts[n];
                totalWeight += w;
            }
            if (totalWeight <= 0.0d) totalWeight = 1.0d;
            for (int n = 0; n < mix.length; n++) mix[n] /= totalWeight;
            normalize(mix);
            copy(mix, out.component[COMPONENT_RECENT][p]);
            out.componentReliability[COMPONENT_RECENT][p] =
                    Math.min(1.0d, h.size() / (double) Math.max(1, AppConfig.SEQUENCE_WINDOW_96));
        }
    }

    private void buildEwma(ArrayList<Models.Draw> h, Forecast out) {
        double decay =
                Math.exp(-Math.log(2.0d) / Math.max(1.0d, AppConfig.SEQUENCE_EWMA_HALF_LIFE));
        for (int p = 0; p < AppConfig.POSITION_COUNT; p++) {
            double[] counts = new double[AppConfig.NUMBER_COUNT];
            for (int n = 0; n < counts.length; n++) counts[n] = AppConfig.SEQUENCE_DIRICHLET_ALPHA;
            double w = 1.0d;
            int used = 0;
            for (int i = h.size() - 1;
                    i >= 0 && used < AppConfig.SEQUENCE_EWMA_MAX_HISTORY;
                    i--, used++) {
                int number = h.get(i).getNumberAtPosition(p + 1);
                if (AppConfig.isValidNumber(number)) counts[number - 1] += w;
                w *= decay;
            }
            normalize(counts);
            copy(counts, out.component[COMPONENT_EWMA][p]);
            out.componentReliability[COMPONENT_EWMA][p] =
                    Math.min(
                            1.0d,
                            used / (double) Math.max(1, AppConfig.SEQUENCE_EWMA_HALF_LIFE * 2));
        }
    }

    private void buildMarkov(ArrayList<Models.Draw> h, Forecast out) {
        for (int p = 0; p < AppConfig.POSITION_COUNT; p++) {
            int condition = h.get(h.size() - 1).getNumberAtPosition(p + 1);
            double[] counts = new double[AppConfig.NUMBER_COUNT];
            for (int n = 0; n < counts.length; n++) counts[n] = AppConfig.SEQUENCE_TRANSITION_ALPHA;
            int sample = 0;
            for (int i = 1; i < h.size(); i++) {
                int previous = h.get(i - 1).getNumberAtPosition(p + 1);
                int current = h.get(i).getNumberAtPosition(p + 1);
                if (previous == condition && AppConfig.isValidNumber(current)) {
                    counts[current - 1] += 1.0d;
                    sample++;
                }
            }
            normalize(counts);
            copy(counts, out.component[COMPONENT_MARKOV][p]);
            out.componentReliability[COMPONENT_MARKOV][p] =
                    supportReliability(sample, AppConfig.SEQUENCE_MARKOV_SUPPORT_STRENGTH);
        }
    }

    private void buildPairTransition(ArrayList<Models.Draw> h, Forecast out) {
        if (h.size() < 3) return;
        for (int p = 0; p < AppConfig.POSITION_COUNT; p++) {
            int a = h.get(h.size() - 2).getNumberAtPosition(p + 1);
            int b = h.get(h.size() - 1).getNumberAtPosition(p + 1);
            double[] counts = new double[AppConfig.NUMBER_COUNT];
            for (int n = 0; n < counts.length; n++) counts[n] = AppConfig.SEQUENCE_PAIR_ALPHA;
            int sample = 0;
            for (int i = 2; i < h.size(); i++) {
                if (h.get(i - 2).getNumberAtPosition(p + 1) == a
                        && h.get(i - 1).getNumberAtPosition(p + 1) == b) {
                    int current = h.get(i).getNumberAtPosition(p + 1);
                    if (AppConfig.isValidNumber(current)) {
                        counts[current - 1] += 1.0d;
                        sample++;
                    }
                }
            }
            normalize(counts);
            copy(counts, out.component[COMPONENT_PAIR][p]);
            out.componentReliability[COMPONENT_PAIR][p] =
                    supportReliability(sample, AppConfig.SEQUENCE_PAIR_SUPPORT_STRENGTH);
        }
    }

    private void buildMovement(ArrayList<Models.Draw> h, Forecast out) {
        if (h.size() < 2) return;
        Models.Draw last = h.get(h.size() - 1);
        int[] previousPosition = inversePositions(last);
        double[][] raw = new double[AppConfig.POSITION_COUNT][AppConfig.NUMBER_COUNT];
        int[][] support = new int[AppConfig.POSITION_COUNT][AppConfig.NUMBER_COUNT];
        for (int n = 1; n <= AppConfig.NUMBER_COUNT; n++) {
            int conditionPosition = previousPosition[n - 1];
            double[] positionCounts = new double[AppConfig.POSITION_COUNT];
            for (int p = 0; p < positionCounts.length; p++)
                positionCounts[p] = AppConfig.SEQUENCE_MOVEMENT_ALPHA;
            int sample = 0;
            for (int i = 1; i < h.size(); i++) {
                int[] before = inversePositions(h.get(i - 1));
                if (before[n - 1] != conditionPosition) continue;
                int[] after = inversePositions(h.get(i));
                int nextPosition = after[n - 1];
                if (AppConfig.isValidPosition(nextPosition)) {
                    positionCounts[nextPosition - 1] += 1.0d;
                    sample++;
                }
            }
            normalize(positionCounts);
            for (int p = 0; p < AppConfig.POSITION_COUNT; p++) {
                raw[p][n - 1] = positionCounts[p];
                support[p][n - 1] = sample;
            }
        }
        for (int p = 0; p < AppConfig.POSITION_COUNT; p++) {
            normalize(raw[p]);
            copy(raw[p], out.component[COMPONENT_MOVEMENT][p]);
            int total = 0;
            for (int n = 0; n < AppConfig.NUMBER_COUNT; n++) total += support[p][n];
            int mean = total / Math.max(1, AppConfig.NUMBER_COUNT);
            out.componentReliability[COMPONENT_MOVEMENT][p] =
                    supportReliability(mean, AppConfig.SEQUENCE_MOVEMENT_SUPPORT_STRENGTH);
        }
    }

    private void buildGapHazard(ArrayList<Models.Draw> h, Forecast out) {
        for (int p = 0; p < AppConfig.POSITION_COUNT; p++) {
            double[] hazard = new double[AppConfig.NUMBER_COUNT];
            int totalSupport = 0;
            for (int number = 1; number <= AppConfig.NUMBER_COUNT; number++) {
                int currentGap = currentGap(h, p + 1, number);
                int currentBand = gapBand(currentGap);
                int[] gap = new int[AppConfig.NUMBER_COUNT];
                for (int n = 0; n < gap.length; n++) gap[n] = AppConfig.SEQUENCE_GAP_CAP;
                int trials = 0;
                int hits = 0;
                for (int i = 0; i < h.size(); i++) {
                    if (i > 0 && gapBand(gap[number - 1]) == currentBand) {
                        trials++;
                        if (h.get(i).getNumberAtPosition(p + 1) == number) hits++;
                    }
                    int actual = h.get(i).getNumberAtPosition(p + 1);
                    for (int n = 0; n < gap.length; n++) {
                        if (n + 1 == actual) gap[n] = 0;
                        else gap[n] = Math.min(AppConfig.SEQUENCE_GAP_CAP, gap[n] + 1);
                    }
                }
                double priorStrength = AppConfig.SEQUENCE_GAP_PRIOR_STRENGTH;
                hazard[number - 1] = (hits + priorStrength * 0.10d) / (trials + priorStrength);
                totalSupport += trials;
            }
            normalize(hazard);
            copy(hazard, out.component[COMPONENT_GAP][p]);
            out.componentReliability[COMPONENT_GAP][p] =
                    supportReliability(
                            totalSupport / Math.max(1, AppConfig.NUMBER_COUNT),
                            AppConfig.SEQUENCE_GAP_SUPPORT_STRENGTH);
        }
    }

    private void combine(Forecast out) {
        double[] baseWeight =
                new double[] {
                    AppConfig.SEQUENCE_WEIGHT_BASE,
                    AppConfig.SEQUENCE_WEIGHT_RECENT,
                    AppConfig.SEQUENCE_WEIGHT_EWMA,
                    AppConfig.SEQUENCE_WEIGHT_MARKOV,
                    AppConfig.SEQUENCE_WEIGHT_PAIR,
                    AppConfig.SEQUENCE_WEIGHT_MOVEMENT,
                    AppConfig.SEQUENCE_WEIGHT_GAP
                };
        double maturity =
                Math.min(
                        1.0d,
                        out.historySize / (double) Math.max(1, AppConfig.SEQUENCE_MATURE_HISTORY));
        for (int p = 0; p < AppConfig.POSITION_COUNT; p++) {
            double[] mix = new double[AppConfig.NUMBER_COUNT];
            double totalWeight = 0.0d;
            for (int k = 0; k < COMPONENT_COUNT; k++) {
                double reliability = k == COMPONENT_BASE ? 1.0d : out.componentReliability[k][p];
                double w = baseWeight[k] * clamp(reliability, 0.0d, 1.0d);
                if (k == COMPONENT_RECENT || k == COMPONENT_EWMA) {
                    w = baseWeight[k] * (0.55d + 0.45d * reliability);
                }
                for (int n = 0; n < AppConfig.NUMBER_COUNT; n++) {
                    mix[n] += w * out.component[k][p][n];
                }
                totalWeight += w;
            }
            if (totalWeight <= 0.0d) totalWeight = 1.0d;
            for (int n = 0; n < mix.length; n++) mix[n] /= totalWeight;
            normalize(mix);

            double deviation = 0.0d;
            int count = 0;
            for (int k = 1; k < COMPONENT_COUNT; k++) {
                if (out.componentReliability[k][p] <= 0.0d) continue;
                for (int n = 0; n < AppConfig.NUMBER_COUNT; n++) {
                    deviation += Math.abs(out.component[k][p][n] - mix[n]);
                    count++;
                }
            }
            deviation = count > 0 ? deviation / count : 0.10d;
            double agreement =
                    clamp(
                            1.0d - deviation / AppConfig.SEQUENCE_COMPONENT_DISAGREEMENT_SCALE,
                            0.0d,
                            1.0d);
            out.positionAgreement[p] = agreement;
            double keep =
                    clamp(
                            AppConfig.SEQUENCE_MIN_SIGNAL_KEEP
                                    + AppConfig.SEQUENCE_MATURITY_SIGNAL_WEIGHT * maturity
                                    + AppConfig.SEQUENCE_AGREEMENT_SIGNAL_WEIGHT * agreement,
                            AppConfig.SEQUENCE_MIN_SIGNAL_KEEP,
                            AppConfig.SEQUENCE_MAX_SIGNAL_KEEP);
            for (int n = 0; n < mix.length; n++) {
                out.probability[p][n] = 0.10d + keep * (mix[n] - 0.10d);
            }
            normalize(out.probability[p]);
            out.positionEntropy[p] = normalizedEntropy(out.probability[p]);
        }
    }

    private void buildRegimes(ArrayList<Models.Draw> history, Forecast out) {
        for (int p = 0; p < AppConfig.POSITION_COUNT; p++) {
            double drift =
                    l1Distance(out.component[COMPONENT_RECENT][p], out.component[COMPONENT_EWMA][p])
                            * 0.5d;
            out.positionDrift[p] = drift;
            double top3 = topKSum(out.probability[p], 3);
            String entropy =
                    out.positionEntropy[p] < 0.94d
                            ? "ENT_LOW"
                            : (out.positionEntropy[p] < 0.975d ? "ENT_MID" : "ENT_HIGH");
            String driftBand =
                    drift < 0.035d
                            ? "DRIFT_STABLE"
                            : (drift < 0.075d ? "DRIFT_MOVE" : "DRIFT_SHIFT");
            String concentration =
                    top3 >= 0.36d ? "TOP3_HIGH" : (top3 >= 0.325d ? "TOP3_MID" : "TOP3_FLAT");
            String agreement =
                    out.positionAgreement[p] >= 0.72d
                            ? "AGR_HIGH"
                            : (out.positionAgreement[p] >= 0.48d ? "AGR_MID" : "AGR_LOW");
            out.regimeKey[p] =
                    "REG40|P"
                            + (p + 1)
                            + "|"
                            + entropy
                            + "|"
                            + driftBand
                            + "|"
                            + concentration
                            + "|"
                            + agreement
                            + "|LAST="
                            + out.lastNumber[p];
        }
    }

    private static ArrayList<Models.Draw> cleanHistory(List<Models.Draw> input, int max) {
        ArrayList<Models.Draw> all = new ArrayList<Models.Draw>();
        if (input == null) return all;
        int start = Math.max(0, input.size() - Math.max(2, max));
        for (int i = start; i < input.size(); i++) {
            Models.Draw d = input.get(i);
            if (d != null && d.isValid()) all.add(d);
        }
        return all;
    }

    private static int[] inversePositions(Models.Draw draw) {
        int[] out = new int[AppConfig.NUMBER_COUNT];
        if (draw == null || !draw.isValid()) return out;
        for (int p = 1; p <= AppConfig.POSITION_COUNT; p++) {
            int n = draw.getNumberAtPosition(p);
            if (AppConfig.isValidNumber(n)) out[n - 1] = p;
        }
        return out;
    }

    private static int currentGap(ArrayList<Models.Draw> h, int position, int number) {
        int gap = 0;
        for (int i = h.size() - 1; i >= 0 && gap < AppConfig.SEQUENCE_GAP_CAP; i--, gap++) {
            if (h.get(i).getNumberAtPosition(position) == number) return gap;
        }
        return AppConfig.SEQUENCE_GAP_CAP;
    }

    private static int gapBand(int gap) {
        if (gap <= 1) return 0;
        if (gap <= 3) return 1;
        if (gap <= 6) return 2;
        if (gap <= 10) return 3;
        if (gap <= 18) return 4;
        return 5;
    }

    private static double supportReliability(int sample, double strength) {
        if (sample <= 0) return 0.0d;
        return sample / (sample + Math.max(1.0d, strength));
    }

    private static double structuralScore(Models.ComboCandidate c) {
        double quality =
                0.5d * clamp(Math.max(c.coverageQuality, c.coverageQualityScore), 0.0d, 1.0d)
                        + 0.5d
                                * clamp(
                                        Math.max(c.thirdQuality, c.thirdNumberQualityScore),
                                        0.0d,
                                        1.0d);
        double support =
                clamp(
                        Math.max(c.evidenceScore, Math.max(c.balanceScore, c.needSupplyMatchScore)),
                        0.0d,
                        1.0d);
        double risk =
                clamp(
                        Math.max(
                                c.counterRisk, Math.max(c.counterEvidenceScore, c.falseStrongRisk)),
                        0.0d,
                        1.0d);
        return clamp(0.56d * support + 0.44d * quality - 0.28d * risk, 0.0d, 1.0d);
    }

    private static String spanBand(int a, int b, int c) {
        int min = Math.min(a, Math.min(b, c));
        int max = Math.max(a, Math.max(b, c));
        int span = max - min;
        return span <= 3 ? "TIGHT" : (span <= 6 ? "MID" : "WIDE");
    }

    private static double topKSum(double[] p, int k) {
        double[] x = new double[p.length];
        copy(p, x);
        for (int i = 0; i < x.length; i++) {
            for (int j = i + 1; j < x.length; j++) {
                if (x[j] > x[i]) {
                    double t = x[i];
                    x[i] = x[j];
                    x[j] = t;
                }
            }
        }
        double sum = 0.0d;
        for (int i = 0; i < Math.min(k, x.length); i++) sum += x[i];
        return sum;
    }

    private static double normalizedEntropy(double[] p) {
        double h = 0.0d;
        for (int i = 0; i < p.length; i++) {
            double x = clamp(p[i], 0.0000001d, 1.0d);
            h -= x * Math.log(x);
        }
        return h / Math.log(Math.max(2, p.length));
    }

    private static double l1Distance(double[] a, double[] b) {
        double s = 0.0d;
        for (int i = 0; i < Math.min(a.length, b.length); i++) s += Math.abs(a[i] - b[i]);
        return s;
    }

    private static double standardDeviation(double[] x) {
        if (x == null || x.length == 0) return 0.0d;
        double mean = 0.0d;
        for (int i = 0; i < x.length; i++) mean += x[i];
        mean /= x.length;
        double v = 0.0d;
        for (int i = 0; i < x.length; i++) {
            double d = x[i] - mean;
            v += d * d;
        }
        return Math.sqrt(v / x.length);
    }

    private static void normalize(double[] x) {
        double sum = 0.0d;
        for (int i = 0; i < x.length; i++) {
            if (Double.isNaN(x[i]) || Double.isInfinite(x[i]) || x[i] < 0.0d) x[i] = 0.0d;
            sum += x[i];
        }
        if (sum <= 0.0d) {
            for (int i = 0; i < x.length; i++) x[i] = 1.0d / Math.max(1, x.length);
            return;
        }
        for (int i = 0; i < x.length; i++) x[i] /= sum;
    }

    private static void copy(double[] from, double[] to) {
        int n = Math.min(from.length, to.length);
        for (int i = 0; i < n; i++) to[i] = from[i];
    }

    private static String append(String a, String b) {
        if (safe(a).length() == 0) return safe(b);
        if (safe(b).length() == 0) return safe(a);
        return a + "｜" + b;
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    private static String pct(double v) {
        return String.format(Locale.US, "%.1f%%", 100.0d * v);
    }

    private static double clamp(double v, double lo, double hi) {
        if (Double.isNaN(v) || Double.isInfinite(v)) return lo;
        return v < lo ? lo : (v > hi ? hi : v);
    }
}
