package com.ethk3.novaonepro;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Issue-grouped chronological walk-forward lab; no row shuffle and no future feature access. */
public final class TimeOrderedExperimentLabEngine {
    public interface Evaluator {
        Prediction predict(Example example);
    }

    private TimeOrderedExperimentLabEngine() {}

    public static ExperimentResult run(
            StrategyChallengerFactoryEngine.CandidateSpec candidate,
            Dataset dataset,
            Evaluator evaluator) {
        if (candidate == null || dataset == null || evaluator == null || !dataset.valid())
            return ExperimentResult.invalid("INPUT_INVALID");
        Split split = split(dataset);
        List<Metric> folds = walkForward(split.trainValidation, evaluator, 4);
        Metric frozen = frozenTest(split.frozenTest, evaluator);
        Metric stress = regimeStress(dataset.examples, evaluator);
        String resultHash =
                V51ChecksumEngine.sha256(
                        candidate.candidateHash
                                + '|'
                                + dataset.hash
                                + '|'
                                + metricSetCanonical(folds)
                                + '|'
                                + frozen.canonical()
                                + '|'
                                + stress.canonical());
        return new ExperimentResult(
                candidate.candidateId,
                dataset.hash,
                folds,
                frozen,
                stress,
                "PIT_PASS",
                resultHash);
    }

    public static ExperimentResult nestedWalkForward(
            StrategyChallengerFactoryEngine.CandidateSpec candidate,
            Dataset dataset,
            Evaluator evaluator) {
        return run(candidate, dataset, evaluator);
    }

    /** Splits only on target_issue boundaries, so 1200 action rows can never cross folds. */
    public static Split split(Dataset dataset) {
        if (dataset == null || !dataset.valid())
            return new Split(Collections.<Example>emptyList(), Collections.<Example>emptyList());
        ArrayList<Example> sorted = sorted(dataset.examples);
        List<String> issues = uniqueIssues(sorted);
        int issueCut = Math.max(1, (int) Math.floor(issues.size() * 0.8d));
        issueCut = Math.min(issues.size() - 1, issueCut);
        String firstFrozenIssue = issues.get(issueCut);
        ArrayList<Example> train = new ArrayList<Example>();
        ArrayList<Example> frozen = new ArrayList<Example>();
        for (int i = 0; i < sorted.size(); i++) {
            Example example = sorted.get(i);
            if (TargetIssueResolver.compare(example.targetIssue, firstFrozenIssue) < 0)
                train.add(example);
            else frozen.add(example);
        }
        return new Split(
                Collections.unmodifiableList(train), Collections.unmodifiableList(frozen));
    }

    public static List<Metric> walkForward(
            List<Example> rows, Evaluator evaluator, int requestedFolds) {
        if (rows == null || evaluator == null || rows.isEmpty())
            return Collections.emptyList();
        ArrayList<Example> sorted = sorted(rows);
        List<String> issues = uniqueIssues(sorted);
        int folds = Math.max(2, Math.min(Math.min(8, requestedFolds), issues.size()));
        ArrayList<Metric> output = new ArrayList<Metric>();
        for (int fold = 1; fold < folds; fold++) {
            int from = fold * issues.size() / folds;
            int to = (fold + 1) * issues.size() / folds;
            if (to <= from) continue;
            Set<String> testIssues =
                    new HashSet<String>(issues.subList(from, to));
            ArrayList<Example> test = new ArrayList<Example>();
            for (int i = 0; i < sorted.size(); i++)
                if (testIssues.contains(sorted.get(i).targetIssue)) test.add(sorted.get(i));
            if (!test.isEmpty()) output.add(metric(test, evaluator, "WF_" + fold));
        }
        return Collections.unmodifiableList(output);
    }

    public static Metric frozenTest(List<Example> rows, Evaluator evaluator) {
        return metric(rows, evaluator, "FROZEN_TEST");
    }

    public static Metric blockStress(List<Example> rows, Evaluator evaluator) {
        return metric(rows, evaluator, "BLOCK_STRESS");
    }

    public static Metric regimeStress(List<Example> rows, Evaluator evaluator) {
        Map<String, List<Example>> groups = new HashMap<String, List<Example>>();
        if (rows != null)
            for (int i = 0; i < rows.size(); i++) {
                Example example = rows.get(i);
                if (example == null || !example.valid()) continue;
                List<Example> group = groups.get(example.regime);
                if (group == null) {
                    group = new ArrayList<Example>();
                    groups.put(example.regime, group);
                }
                group.add(example);
            }
        Metric worst = null;
        for (List<Example> group : groups.values()) {
            Metric current = metric(group, evaluator, "REGIME");
            if (worst == null || current.hitRate < worst.hitRate) worst = current;
        }
        return worst == null ? Metric.empty("REGIME") : worst;
    }

    public static double[] bootstrap(
            List<Example> rows, Evaluator evaluator, int replicates, long seed) {
        ArrayList<ResearchStatisticsGuardR811.Row> values =
                new ArrayList<ResearchStatisticsGuardR811.Row>();
        if (rows != null && evaluator != null)
            for (int i = 0; i < rows.size(); i++) {
                Example example = rows.get(i);
                if (example == null || !example.valid()) continue;
                Prediction prediction = evaluator.predict(example);
                if (prediction != null && prediction.valid() && prediction.selected)
                    values.add(
                            new ResearchStatisticsGuardR811.Row(
                                    example.targetIssue,
                                    prediction.label == example.label ? 1.0d : 0.0d,
                                    true));
            }
        ResearchStatisticsGuardR811.Bootstrap bootstrap =
                ResearchStatisticsGuardR811.blockBootstrap(values, replicates, seed);
        return new double[] {bootstrap.estimate, bootstrap.lower, bootstrap.upper};
    }

    /** Benjamini-Hochberg at issue-level p-values. */
    public static boolean[] fdr(double[] pValues, double q) {
        boolean[] passed = new boolean[pValues == null ? 0 : pValues.length];
        if (pValues == null || pValues.length == 0 || !(q > 0.0d && q <= 1.0d))
            return passed;
        ArrayList<Integer> indexes = new ArrayList<Integer>();
        for (int i = 0; i < pValues.length; i++)
            if (pValues[i] >= 0.0d && pValues[i] <= 1.0d && !Double.isNaN(pValues[i]))
                indexes.add(Integer.valueOf(i));
        Collections.sort(
                indexes,
                new Comparator<Integer>() {
                    public int compare(Integer left, Integer right) {
                        return Double.compare(pValues[left.intValue()], pValues[right.intValue()]);
                    }
                });
        int largestRank = -1;
        for (int rank = 1; rank <= indexes.size(); rank++)
            if (pValues[indexes.get(rank - 1).intValue()]
                    <= q * rank / indexes.size()) largestRank = rank - 1;
        for (int i = 0; i <= largestRank; i++) passed[indexes.get(i).intValue()] = true;
        return passed;
    }

    public static boolean hardGate(ExperimentResult result) {
        return result != null
                && "PIT_PASS".equals(result.pitStatus)
                && result.resultHash.length() == 64
                && result.frozen.valid
                && result.stress.valid
                && result.frozen.eligible > 0
                && result.frozen.selected > 0
                && finite01(result.frozen.coverage)
                && finite01(result.frozen.hitRate)
                && finite01(result.frozen.ece)
                && result.frozen.brier >= 0.0d
                && !Double.isNaN(result.frozen.brier)
                && !Double.isInfinite(result.frozen.brier);
    }

    /** Hard-gate first; only then compute the non-dominated research frontier. */
    public static List<ExperimentResult> paretoFront(List<ExperimentResult> results) {
        ArrayList<ExperimentResult> eligible = new ArrayList<ExperimentResult>();
        if (results != null)
            for (int i = 0; i < results.size(); i++)
                if (hardGate(results.get(i))) eligible.add(results.get(i));
        ArrayList<ExperimentResult> output = new ArrayList<ExperimentResult>();
        for (int i = 0; i < eligible.size(); i++) {
            ExperimentResult candidate = eligible.get(i);
            boolean dominated = false;
            for (int j = 0; j < eligible.size(); j++) {
                ExperimentResult other = eligible.get(j);
                if (candidate != other && dominates(other, candidate)) {
                    dominated = true;
                    break;
                }
            }
            if (!dominated) output.add(candidate);
        }
        return Collections.unmodifiableList(output);
    }

    public static List<ExperimentResult> pareto(List<ExperimentResult> results) {
        return paretoFront(results);
    }

    public static String compareChampion(
            ExperimentResult challenger, ExperimentResult champion) {
        if (!hardGate(challenger)) return "REJECT_HARD_GATE";
        if (!hardGate(champion)) return "CHALLENGER_ONLY_VALID_RESULT";
        if (dominates(challenger, champion)) return "CHALLENGER_PARETO_DOMINATES";
        if (dominates(champion, challenger)) return "KEEP_CHAMPION";
        return "PARETO_TRADEOFF_REQUIRES_FUTURE_OOS";
    }

    public static double coverageCv(double[] blockCoverage) {
        if (blockCoverage == null || blockCoverage.length < 4) return Double.NaN;
        double mean = 0.0d;
        for (int i = 0; i < blockCoverage.length; i++) {
            if (!finite01(blockCoverage[i])) return Double.NaN;
            mean += blockCoverage[i];
        }
        mean /= blockCoverage.length;
        if (mean <= 1e-15d) return Double.NaN;
        double variance = 0.0d;
        for (int i = 0; i < blockCoverage.length; i++) {
            double delta = blockCoverage[i] - mean;
            variance += delta * delta;
        }
        return Math.sqrt(variance / blockCoverage.length) / mean;
    }

    public static double blockMinimum(double[] blocks) {
        if (blocks == null || blocks.length < 4) return Double.NaN;
        double minimum = Double.MAX_VALUE;
        for (int i = 0; i < blocks.length; i++) {
            if (Double.isNaN(blocks[i]) || Double.isInfinite(blocks[i])) return Double.NaN;
            minimum = Math.min(minimum, blocks[i]);
        }
        return minimum;
    }

    public static boolean sameIssueSamePartition(Split split) {
        if (split == null) return false;
        Set<String> train = new HashSet<String>();
        for (int i = 0; i < split.trainValidation.size(); i++)
            train.add(split.trainValidation.get(i).targetIssue);
        for (int i = 0; i < split.frozenTest.size(); i++)
            if (train.contains(split.frozenTest.get(i).targetIssue)) return false;
        return true;
    }

    private static boolean dominates(ExperimentResult left, ExperimentResult right) {
        return left.frozen.hitRate >= right.frozen.hitRate
                && left.frozen.coverage >= right.frozen.coverage
                && left.frozen.ece <= right.frozen.ece
                && left.frozen.brier <= right.frozen.brier
                && (left.frozen.hitRate > right.frozen.hitRate
                        || left.frozen.coverage > right.frozen.coverage
                        || left.frozen.ece < right.frozen.ece
                        || left.frozen.brier < right.frozen.brier);
    }

    private static Metric metric(List<Example> rows, Evaluator evaluator, String role) {
        if (rows == null || evaluator == null) return Metric.invalid(role);
        int eligible = 0, selected = 0, hit = 0;
        double brier = 0.0d, probabilitySum = 0.0d;
        for (int i = 0; i < rows.size(); i++) {
            Example example = rows.get(i);
            if (example == null || !example.valid()) return Metric.invalid(role);
            Prediction prediction = evaluator.predict(example);
            if (prediction == null || !prediction.valid()) return Metric.invalid(role);
            eligible++;
            if (prediction.selected) {
                selected++;
                hit += prediction.label == example.label ? 1 : 0;
                double error = prediction.probability - example.label;
                brier += error * error;
                probabilitySum += prediction.probability;
            }
        }
        double hitRate = selected == 0 ? 0.0d : ((double) hit) / selected;
        double coverage = eligible == 0 ? 0.0d : ((double) selected) / eligible;
        double ece =
                selected == 0
                        ? 1.0d
                        : Math.abs(probabilitySum / selected - hitRate);
        return new Metric(
                role,
                eligible,
                selected,
                hit,
                hitRate,
                coverage,
                ece,
                selected == 0 ? -1.0d : brier / selected,
                true);
    }

    private static ArrayList<Example> sorted(List<Example> examples) {
        ArrayList<Example> output = new ArrayList<Example>(examples);
        Collections.sort(
                output,
                new Comparator<Example>() {
                    public int compare(Example left, Example right) {
                        return TargetIssueResolver.compare(left.targetIssue, right.targetIssue);
                    }
                });
        return output;
    }

    private static List<String> uniqueIssues(List<Example> sorted) {
        ArrayList<String> issues = new ArrayList<String>();
        String previous = "";
        for (int i = 0; i < sorted.size(); i++) {
            String issue = sorted.get(i).targetIssue;
            if (!issue.equals(previous)) {
                issues.add(issue);
                previous = issue;
            }
        }
        return issues;
    }

    private static String metricSetCanonical(List<Metric> metrics) {
        StringBuilder output = new StringBuilder();
        for (int i = 0; i < metrics.size(); i++)
            output.append(metrics.get(i).canonical()).append('\n');
        return output.toString();
    }

    private static boolean finite01(double value) {
        return !Double.isNaN(value)
                && !Double.isInfinite(value)
                && value >= 0.0d
                && value <= 1.0d;
    }

    public static final class Example {
        public final String targetIssue, regime;
        public final int label;
        public final Map<String, Double> features;

        public Example(
                String issue, String regime, int label, Map<String, Double> features) {
            targetIssue = issue == null ? "" : issue;
            this.regime = regime == null ? "" : regime;
            this.label = label;
            this.features =
                    Collections.unmodifiableMap(
                            features == null
                                    ? new LinkedHashMap<String, Double>()
                                    : new LinkedHashMap<String, Double>(features));
        }

        boolean valid() {
            if (!TargetIssueResolver.isValid(targetIssue) || (label != 0 && label != 1))
                return false;
            for (Double value : features.values())
                if (value == null
                        || Double.isNaN(value.doubleValue())
                        || Double.isInfinite(value.doubleValue())) return false;
            return true;
        }
    }

    public static final class Dataset {
        public final List<Example> examples;
        public final String hash;

        public Dataset(List<Example> examples, String hash) {
            this.examples =
                    Collections.unmodifiableList(
                            examples == null
                                    ? new ArrayList<Example>()
                                    : new ArrayList<Example>(examples));
            this.hash = hash == null ? "" : hash;
        }

        boolean valid() {
            if (hash.length() != 64 || examples.isEmpty()) return false;
            Set<String> issues = new HashSet<String>();
            for (int i = 0; i < examples.size(); i++) {
                Example example = examples.get(i);
                if (example == null || !example.valid()) return false;
                issues.add(example.targetIssue);
            }
            return issues.size() >= 12;
        }
    }

    public static final class Prediction {
        public final boolean selected;
        public final int label;
        public final double probability;

        public Prediction(boolean selected, int label, double probability) {
            this.selected = selected;
            this.label = label;
            this.probability = probability;
        }

        boolean valid() {
            return (label == 0 || label == 1)
                    && finite01(probability);
        }
    }

    public static final class Split {
        public final List<Example> trainValidation, frozenTest;

        Split(List<Example> train, List<Example> frozen) {
            trainValidation = train;
            frozenTest = frozen;
        }
    }

    public static final class Metric {
        public final String role;
        public final int eligible, selected, hit;
        public final double hitRate, coverage, ece, brier;
        public final boolean valid;

        Metric(
                String role,
                int eligible,
                int selected,
                int hit,
                double hitRate,
                double coverage,
                double ece,
                double brier,
                boolean valid) {
            this.role = role;
            this.eligible = eligible;
            this.selected = selected;
            this.hit = hit;
            this.hitRate = hitRate;
            this.coverage = coverage;
            this.ece = ece;
            this.brier = brier;
            this.valid = valid;
        }

        static Metric empty(String role) {
            return new Metric(role, 0, 0, 0, 0.0d, 0.0d, 1.0d, -1.0d, true);
        }

        static Metric invalid(String role) {
            return new Metric(role, 0, 0, 0, 0.0d, 0.0d, 1.0d, -1.0d, false);
        }

        String canonical() {
            return role
                    + '|'
                    + eligible
                    + '|'
                    + selected
                    + '|'
                    + hit
                    + '|'
                    + hitRate
                    + '|'
                    + coverage
                    + '|'
                    + ece
                    + '|'
                    + brier
                    + '|'
                    + valid;
        }
    }

    public static final class ExperimentResult {
        public final String candidateId, datasetHash, pitStatus, resultHash;
        public final List<Metric> walkForward;
        public final Metric frozen, stress;

        ExperimentResult(
                String candidateId,
                String datasetHash,
                List<Metric> walkForward,
                Metric frozen,
                Metric stress,
                String pitStatus,
                String resultHash) {
            this.candidateId = candidateId;
            this.datasetHash = datasetHash;
            this.walkForward =
                    Collections.unmodifiableList(new ArrayList<Metric>(walkForward));
            this.frozen = frozen;
            this.stress = stress;
            this.pitStatus = pitStatus;
            this.resultHash = resultHash;
        }

        static ExperimentResult invalid(String code) {
            return new ExperimentResult(
                    "",
                    "",
                    Collections.<Metric>emptyList(),
                    Metric.invalid("INVALID"),
                    Metric.invalid("INVALID"),
                    code,
                    "");
        }
    }
}
