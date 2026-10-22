package com.ethk3.novaonepro;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public final class TransitionRelationEngine {
    public static final String B06_CONTRACT_VERSION =
            "STARCORE_TIANSHU_B06_TRANSITION_RELATION_EVIDENCE_V1";
    public static final String B06_OWNER = "TRANSITION_RELATION_EVIDENCE_ONLY";

    private static final int RECENT_WINDOW = 80;
    private static final int MID_WINDOW = 240;
    private static final int DEEP_WINDOW = 1000;

    private static final double SCORE_USABLE = 52.0d;
    private static final double SCORE_STRONG = 62.0d;
    private static final double SCORE_A_PLUS = 72.0d;

    public TransitionRelationEngine() {}

    public ArrayList<TransitionRelationResult> analyzeAll(List<Models.Draw> history) {
        ArrayList<TransitionRelationResult> out = new ArrayList<TransitionRelationResult>();
        ArrayList<Models.Draw> clean = cleanHistory(history);
        PatternStateEngine.PatternStateResult pattern = buildPatternEvidence(clean);
        NumberBehaviorEngine.NumberBehaviorResult behavior = buildNumberBehaviorEvidence(clean);
        for (int p = 1; p <= AppConfig.POSITION_COUNT; p++) {
            TransitionRelationResult r = analyzePositionCore(clean, p);
            attachB06Evidence(r, pattern, behavior);
            out.add(r);
        }
        sortAndRank(out);
        return out;
    }

    public TransitionRelationResult analyzePosition(List<Models.Draw> history, int position) {
        ArrayList<Models.Draw> clean = cleanHistory(history);
        TransitionRelationResult r = analyzePositionCore(clean, position);
        attachB06Evidence(r, buildPatternEvidence(clean), buildNumberBehaviorEvidence(clean));
        if (r != null) r.summaryText = buildSummary(r) + "；" + r.b06Summary;
        return r;
    }

    private TransitionRelationResult analyzePositionCore(List<Models.Draw> history, int position) {
        TransitionRelationResult r = new TransitionRelationResult();
        r.position = position;
        r.createdAt = System.currentTimeMillis();

        ArrayList<Models.Draw> clean = cleanHistory(history);
        r.sampleCount = clean.size();

        if (!AppConfig.isValidPosition(position)) {
            r.blockCode = AppConfig.BLOCK_POSITION_WEAK;
            r.summaryText = "转移关系：位置非法";
            return r;
        }

        if (clean.size() < 12) {
            r.blockCode = AppConfig.BLOCK_HISTORY_WEAK;
            r.summaryText = "转移关系：历史少于12期，不能判断上一期到下一期关系";
            return r;
        }

        Models.Draw latest = clean.get(clean.size() - 1);
        r.baseIssue = latest.issue == null ? "" : latest.issue;
        r.fromNumber = latest.getNumberAtPosition(position);

        if (clean.size() >= 2) {
            Models.Draw prev = clean.get(clean.size() - 2);
            r.prevFromNumber = prev.getNumberAtPosition(position);
        }

        if (!AppConfig.isValidNumber(r.fromNumber)) {
            r.blockCode = AppConfig.BLOCK_DATA_ERROR;
            r.summaryText = "转移关系：最近一期该位置号码异常";
            return r;
        }

        int[][] matrixAll = buildMarkov1Matrix(clean, position, DEEP_WINDOW);
        int[][] matrixMid = buildMarkov1Matrix(clean, position, MID_WINDOW);
        int[][] matrixRecent = buildMarkov1Matrix(clean, position, RECENT_WINDOW);
        int[][][] matrix2 = buildMarkov2Matrix(clean, position, DEEP_WINDOW);

        int[] freqAll = countNumbers(clean, position, DEEP_WINDOW);
        int[] freqRecent = countNumbers(clean, position, RECENT_WINDOW);

        int totalAll = countTotal(freqAll);
        int totalRecent = countTotal(freqRecent);

        r.fromSampleAll = rowTotal(matrixAll, r.fromNumber);
        r.fromSampleMid = rowTotal(matrixMid, r.fromNumber);
        r.fromSampleRecent = rowTotal(matrixRecent, r.fromNumber);

        double[] score = new double[AppConfig.NUMBER_COUNT + 1];

        for (int n = AppConfig.MIN_NUMBER; n <= AppConfig.MAX_NUMBER; n++) {
            double pAll = rowProbability(matrixAll, r.fromNumber, n);
            double pMid = rowProbability(matrixMid, r.fromNumber, n);
            double pRecent = rowProbability(matrixRecent, r.fromNumber, n);

            double baseAll = totalAll > 0 ? ((double) freqAll[n]) / ((double) totalAll) : 0.0d;
            double baseRecent =
                    totalRecent > 0 ? ((double) freqRecent[n]) / ((double) totalRecent) : baseAll;

            double liftAll = pAll - baseAll;
            double liftRecent = pRecent - baseRecent;

            double s = 0.0d;
            s += pRecent * 0.46d;
            s += pMid * 0.28d;
            s += pAll * 0.16d;
            s += Math.max(0.0d, liftRecent) * 0.07d;
            s += Math.max(0.0d, liftAll) * 0.03d;

            if (AppConfig.isValidNumber(r.prevFromNumber)) {
                double p2 = markov2Probability(matrix2, r.prevFromNumber, r.fromNumber, n);
                s += p2 * 0.18d;
            }

            if (pRecent > pMid && pMid >= pAll) {
                s += 0.006d;
            }

            if (pRecent < pMid && pMid < pAll) {
                s -= 0.004d;
            }

            score[n] = s;
        }

        int[] top = top3(score);
        r.n1 = top[0];
        r.n2 = top[1];
        r.n3 = top[2];
        r.k3Mask = Models.buildK3Mask(r.n1, r.n2, r.n3);

        fillTopStats(
                r,
                matrixAll,
                matrixMid,
                matrixRecent,
                matrix2,
                freqAll,
                freqRecent,
                totalAll,
                totalRecent);

        r.transitionSupport = supportScore(r);
        r.transitionLift = liftScore(r);
        r.recentTransitionScore = recentScore(r);
        r.relationScore = finalRelationScore(r);
        r.score01 = AppConfig.clampScore01(r.relationScore / 100.0d);

        r.stateMode = stateByScore(r);
        r.blockCode = blockByRisk(r);
        r.usable = r.blockCode.length() == 0 ? 1 : 0;
        r.strong = r.usable == 1 && r.relationScore >= SCORE_STRONG ? 1 : 0;
        r.aPlusSupport = r.usable == 1 && r.relationScore >= SCORE_A_PLUS ? 1 : 0;
        r.relationKey = buildRelationKey(r);
        r.summaryText = buildSummary(r);

        return r;
    }

    public TransitionRelationResult analyzeCandidate(
            List<Models.Draw> history, Models.ComboCandidate candidate) {
        if (candidate == null || !candidate.isValidK3()) {
            TransitionRelationResult r = new TransitionRelationResult();
            r.blockCode = AppConfig.BLOCK_COMBO_WEAK;
            r.summaryText = "转移关系：候选为空或K3非法";
            return r;
        }

        TransitionRelationResult base = analyzePosition(history, candidate.position);
        if (base == null) {
            return null;
        }

        TransitionRelationResult r = base.copy();

        r.candidateMask = candidate.k3Mask;
        r.candidateHitMass = candidateHitMass(base, candidate.k3Mask);
        r.candidateNumberCoverage = candidateCoverage(base, candidate.k3Mask);
        r.candidateRelationScore =
                AppConfig.clampScore100(
                        base.relationScore * 0.62d
                                + r.candidateHitMass * 100.0d * 0.26d
                                + r.candidateNumberCoverage * 100.0d * 0.12d);

        if (r.candidateHitMass <= 0.0d) {
            r.b06CounterEvidence = AppConfig.clampScore01(r.b06CounterEvidence + 0.12d);
            r.b06Risk = AppConfig.clampScore01(r.b06Risk + 0.10d);
            r.b06EvidenceReady = 0;
        }

        r.summaryText = buildCandidateSummary(r, candidate) + "；候选关系只作为反证，不读取FinalP、不写Block";

        return r;
    }

    public ArrayList<Models.ComboCandidate> applyToCandidates(
            List<Models.ComboCandidate> candidates, List<Models.Draw> history) {
        ArrayList<Models.ComboCandidate> out = new ArrayList<Models.ComboCandidate>();

        if (candidates == null || candidates.size() == 0) {
            return out;
        }

        ArrayList<TransitionRelationResult> all = analyzeAll(history);

        for (int i = 0; i < candidates.size(); i++) {
            Models.ComboCandidate c = candidates.get(i);
            if (c == null) {
                continue;
            }

            Models.ComboCandidate copy = c.copy();
            TransitionRelationResult r = findForPosition(all, copy.position);
            applyToCandidate(copy, r);
            out.add(copy);
        }

        sortCandidates(out);
        return out;
    }

    public void applyToCandidate(
            Models.ComboCandidate candidate, TransitionRelationResult relation) {
        if (candidate == null || !candidate.isValidK3()) return;
        if (relation == null) {
            candidate.deepTransitionScore = 0.0d;
            candidate.reasonSummary = append(candidate.reasonSummary, "B06转移关系EvidenceOnly：暂无");
            return;
        }
        double mass = candidateHitMass(relation, candidate.k3Mask);
        double coverage = candidateCoverage(relation, candidate.k3Mask);
        double candidateScore =
                AppConfig.clampScore100(
                        relation.relationScore * 0.56d
                                + mass * 100.0d * 0.30d
                                + coverage * 100.0d * 0.14d);
        candidate.deepTransitionScore = AppConfig.clampScore01(candidateScore / 100.0d);
        candidate.evidenceScore =
                blend01(candidate.evidenceScore, candidate.deepTransitionScore, 0.16d);
        candidate.evidenceRisk = blend01(candidate.evidenceRisk, relation.b06Risk, 0.10d);
        candidate.deepScore = blend01(candidate.deepScore, candidate.deepTransitionScore, 0.08d);
        candidate.reasonSummary =
                append(
                        candidate.reasonSummary,
                        "B06转移关系EvidenceOnly P"
                                + relation.position
                                + "："
                                + UiFormatter.number(relation.fromNumber)
                                + "->"
                                + UiFormatter.numberGroup(relation.n1, relation.n2, relation.n3)
                                + "，候选覆盖="
                                + UiFormatter.probability(coverage)
                                + "，支持="
                                + UiFormatter.probability(relation.b06Support)
                                + "，反证="
                                + UiFormatter.probability(relation.b06CounterEvidence)
                                + "，hash="
                                + shortHash(relation.b06Fingerprint));
    }

    public Models.TransitionFeature buildTransitionFeature(
            List<Models.Draw> history, int position) {
        TransitionRelationResult r = analyzePosition(history, position);

        Models.TransitionFeature f = new Models.TransitionFeature();

        if (r == null) {
            f.summaryText = "转移关系为空";
            return f;
        }

        f.position = r.position;
        f.fromNumber = r.fromNumber;
        f.toNumber = r.n1;
        f.sampleCount = r.fromSampleAll;
        f.markov1Prob = r.bestMarkov1P;
        f.markov2Prob = r.bestMarkov2P;
        f.transitionLift = r.bestLift;
        f.recentTransitionProb = r.bestRecentP;
        f.transitionSupport = r.transitionSupport;
        f.relationScore = r.relationScore;
        f.summaryText = r.summaryText;

        return f;
    }

    public Models.DeepFeatureSet applyToDeepFeatureSet(
            Models.DeepFeatureSet set, List<Models.Draw> history) {
        if (set == null) set = new Models.DeepFeatureSet();
        ArrayList<TransitionRelationResult> all = analyzeAll(history);
        TransitionRelationResult best = best(all);
        if (best == null) return set;
        set.transitionFeature = buildTransitionFeature(history, best.position);
        set.summaryText =
                append(
                        set.summaryText,
                        "B06转移关系EvidenceOnly："
                                + best.summaryText
                                + "，hash="
                                + shortHash(best.b06Fingerprint));
        return set;
    }

    public TransitionRelationResult findForPosition(
            List<TransitionRelationResult> list, int position) {
        if (list == null || list.size() == 0) {
            return null;
        }

        for (int i = 0; i < list.size(); i++) {
            TransitionRelationResult r = list.get(i);
            if (r != null && r.position == position) {
                return r;
            }
        }

        return null;
    }

    public TransitionRelationResult best(List<TransitionRelationResult> list) {
        if (list == null || list.size() == 0) {
            return null;
        }

        TransitionRelationResult best = null;

        for (int i = 0; i < list.size(); i++) {
            TransitionRelationResult r = list.get(i);
            if (r == null) {
                continue;
            }

            if (best == null || r.sortScore() > best.sortScore()) {
                best = r;
            }
        }

        return best;
    }

    public String buildSummary(List<TransitionRelationResult> list) {
        if (list == null || list.size() == 0) {
            return "转移关系：暂无";
        }

        StringBuilder sb = new StringBuilder();
        int limit = Math.min(list.size(), 3);

        for (int i = 0; i < limit; i++) {
            TransitionRelationResult r = list.get(i);
            if (r == null) {
                continue;
            }

            if (sb.length() > 0) {
                sb.append(" | ");
            }

            sb.append("P");
            sb.append(r.position);
            sb.append(" ");
            sb.append(UiFormatter.number(r.fromNumber));
            sb.append("->");
            sb.append(UiFormatter.numberGroup(r.n1, r.n2, r.n3));
            sb.append(" 分=");
            sb.append(UiFormatter.score(r.relationScore));

            if (r.blockCode != null && r.blockCode.length() > 0) {
                sb.append(" ");
                sb.append(UiFormatter.blockReasonText(r.blockCode));
            }
        }

        return sb.length() == 0 ? "转移关系：暂无有效位置" : sb.toString();
    }

    private ArrayList<Models.Draw> cleanHistory(List<Models.Draw> history) {
        ArrayList<Models.Draw> out = new ArrayList<Models.Draw>();

        if (history == null || history.size() == 0) {
            return out;
        }

        for (int i = 0; i < history.size(); i++) {
            Models.Draw d = history.get(i);
            if (d != null && d.isValid()) {
                out.add(d);
            }
        }

        return out;
    }

    private int[][] buildMarkov1Matrix(List<Models.Draw> history, int position, int window) {
        int[][] matrix = new int[AppConfig.NUMBER_COUNT + 1][AppConfig.NUMBER_COUNT + 1];

        if (history == null || history.size() < 2 || !AppConfig.isValidPosition(position)) {
            return matrix;
        }

        int start = history.size() - window;
        if (start < 1) {
            start = 1;
        }

        for (int i = start; i < history.size(); i++) {
            Models.Draw prev = history.get(i - 1);
            Models.Draw cur = history.get(i);

            if (prev == null || cur == null || !prev.isValid() || !cur.isValid()) {
                continue;
            }

            int from = prev.getNumberAtPosition(position);
            int to = cur.getNumberAtPosition(position);

            if (AppConfig.isValidNumber(from) && AppConfig.isValidNumber(to)) {
                matrix[from][to]++;
            }
        }

        return matrix;
    }

    private int[][][] buildMarkov2Matrix(List<Models.Draw> history, int position, int window) {
        int[][][] matrix =
                new int[AppConfig.NUMBER_COUNT + 1][AppConfig.NUMBER_COUNT + 1]
                        [AppConfig.NUMBER_COUNT + 1];

        if (history == null || history.size() < 3 || !AppConfig.isValidPosition(position)) {
            return matrix;
        }

        int start = history.size() - window;
        if (start < 2) {
            start = 2;
        }

        for (int i = start; i < history.size(); i++) {
            Models.Draw a = history.get(i - 2);
            Models.Draw b = history.get(i - 1);
            Models.Draw c = history.get(i);

            if (a == null
                    || b == null
                    || c == null
                    || !a.isValid()
                    || !b.isValid()
                    || !c.isValid()) {
                continue;
            }

            int n0 = a.getNumberAtPosition(position);
            int n1 = b.getNumberAtPosition(position);
            int n2 = c.getNumberAtPosition(position);

            if (AppConfig.isValidNumber(n0)
                    && AppConfig.isValidNumber(n1)
                    && AppConfig.isValidNumber(n2)) {
                matrix[n0][n1][n2]++;
            }
        }

        return matrix;
    }

    private int[] countNumbers(List<Models.Draw> history, int position, int window) {
        int[] counts = new int[AppConfig.NUMBER_COUNT + 1];

        if (history == null || history.size() == 0 || !AppConfig.isValidPosition(position)) {
            return counts;
        }

        int start = history.size() - window;
        if (start < 0) {
            start = 0;
        }

        for (int i = start; i < history.size(); i++) {
            Models.Draw d = history.get(i);
            if (d == null || !d.isValid()) {
                continue;
            }

            int n = d.getNumberAtPosition(position);
            if (AppConfig.isValidNumber(n)) {
                counts[n]++;
            }
        }

        return counts;
    }

    private int countTotal(int[] counts) {
        if (counts == null) {
            return 0;
        }

        int total = 0;
        for (int n = AppConfig.MIN_NUMBER; n <= AppConfig.MAX_NUMBER; n++) {
            total += counts[n];
        }
        return total;
    }

    private int rowTotal(int[][] matrix, int from) {
        if (matrix == null || !AppConfig.isValidNumber(from)) {
            return 0;
        }

        int total = 0;
        for (int n = AppConfig.MIN_NUMBER; n <= AppConfig.MAX_NUMBER; n++) {
            total += matrix[from][n];
        }

        return total;
    }

    private int row2Total(int[][][] matrix, int prev, int from) {
        if (matrix == null || !AppConfig.isValidNumber(prev) || !AppConfig.isValidNumber(from)) {
            return 0;
        }

        int total = 0;
        for (int n = AppConfig.MIN_NUMBER; n <= AppConfig.MAX_NUMBER; n++) {
            total += matrix[prev][from][n];
        }

        return total;
    }

    private double rowProbability(int[][] matrix, int from, int to) {
        if (matrix == null || !AppConfig.isValidNumber(from) || !AppConfig.isValidNumber(to)) {
            return 0.0d;
        }

        int total = rowTotal(matrix, from);
        if (total <= 0) {
            return 0.0d;
        }

        return ((double) matrix[from][to]) / ((double) total);
    }

    private double markov2Probability(int[][][] matrix, int prev, int from, int to) {
        if (matrix == null
                || !AppConfig.isValidNumber(prev)
                || !AppConfig.isValidNumber(from)
                || !AppConfig.isValidNumber(to)) {
            return 0.0d;
        }

        int total = row2Total(matrix, prev, from);
        if (total <= 0) {
            return 0.0d;
        }

        return ((double) matrix[prev][from][to]) / ((double) total);
    }

    private int[] top3(double[] score) {
        int[] nums = new int[] {1, 2, 3};

        for (int i = 0; i < AppConfig.FIXED_K; i++) {
            int best = 1;
            double bestScore = -999.0d;

            for (int n = AppConfig.MIN_NUMBER; n <= AppConfig.MAX_NUMBER; n++) {
                boolean used = false;
                for (int k = 0; k < i; k++) {
                    if (nums[k] == n) {
                        used = true;
                        break;
                    }
                }

                if (used) {
                    continue;
                }

                double s = score == null || n >= score.length ? 0.0d : score[n];

                if (s > bestScore) {
                    best = n;
                    bestScore = s;
                }
            }

            nums[i] = best;
        }

        sortNumbers(nums);
        return nums;
    }

    private void sortNumbers(int[] nums) {
        if (nums == null) {
            return;
        }

        for (int i = 0; i < nums.length - 1; i++) {
            for (int j = i + 1; j < nums.length; j++) {
                if (nums[j] < nums[i]) {
                    int t = nums[i];
                    nums[i] = nums[j];
                    nums[j] = t;
                }
            }
        }
    }

    private void fillTopStats(
            TransitionRelationResult r,
            int[][] matrixAll,
            int[][] matrixMid,
            int[][] matrixRecent,
            int[][][] matrix2,
            int[] freqAll,
            int[] freqRecent,
            int totalAll,
            int totalRecent) {
        if (r == null) {
            return;
        }

        double p1 = rowProbability(matrixAll, r.fromNumber, r.n1);
        double p2 = rowProbability(matrixAll, r.fromNumber, r.n2);
        double p3 = rowProbability(matrixAll, r.fromNumber, r.n3);

        double m1 = rowProbability(matrixMid, r.fromNumber, r.n1);
        double m2 = rowProbability(matrixMid, r.fromNumber, r.n2);
        double m3 = rowProbability(matrixMid, r.fromNumber, r.n3);

        double q1 = rowProbability(matrixRecent, r.fromNumber, r.n1);
        double q2 = rowProbability(matrixRecent, r.fromNumber, r.n2);
        double q3 = rowProbability(matrixRecent, r.fromNumber, r.n3);

        r.markov1Mass = p1 + p2 + p3;
        r.midMass = m1 + m2 + m3;
        r.recentMass = q1 + q2 + q3;

        r.bestMarkov1P = max3(p1, p2, p3);
        r.bestRecentP = max3(q1, q2, q3);

        if (AppConfig.isValidNumber(r.prevFromNumber)) {
            double a = markov2Probability(matrix2, r.prevFromNumber, r.fromNumber, r.n1);
            double b = markov2Probability(matrix2, r.prevFromNumber, r.fromNumber, r.n2);
            double c = markov2Probability(matrix2, r.prevFromNumber, r.fromNumber, r.n3);

            r.markov2Mass = a + b + c;
            r.bestMarkov2P = max3(a, b, c);
            r.markov2Sample = row2Total(matrix2, r.prevFromNumber, r.fromNumber);
        }

        double baseAll = 0.0d;
        double baseRecent = 0.0d;

        if (Models.maskContains(r.k3Mask, r.n1)) {
            baseAll += totalAll > 0 ? ((double) freqAll[r.n1]) / ((double) totalAll) : 0.0d;
            baseRecent +=
                    totalRecent > 0 ? ((double) freqRecent[r.n1]) / ((double) totalRecent) : 0.0d;
        }

        if (Models.maskContains(r.k3Mask, r.n2)) {
            baseAll += totalAll > 0 ? ((double) freqAll[r.n2]) / ((double) totalAll) : 0.0d;
            baseRecent +=
                    totalRecent > 0 ? ((double) freqRecent[r.n2]) / ((double) totalRecent) : 0.0d;
        }

        if (Models.maskContains(r.k3Mask, r.n3)) {
            baseAll += totalAll > 0 ? ((double) freqAll[r.n3]) / ((double) totalAll) : 0.0d;
            baseRecent +=
                    totalRecent > 0 ? ((double) freqRecent[r.n3]) / ((double) totalRecent) : 0.0d;
        }

        r.baseMass = baseAll;
        r.recentBaseMass = baseRecent;
        r.bestLift = Math.max(0.0d, r.markov1Mass - baseAll);
        r.recentLift = Math.max(0.0d, r.recentMass - baseRecent);
    }

    private double supportScore(TransitionRelationResult r) {
        if (r == null) {
            return 0.0d;
        }

        double score = 0.0d;

        score += Math.min(1.0d, ((double) r.fromSampleAll) / 80.0d) * 38.0d;
        score += Math.min(1.0d, ((double) r.fromSampleMid) / 28.0d) * 28.0d;
        score += Math.min(1.0d, ((double) r.fromSampleRecent) / 10.0d) * 22.0d;
        score += Math.min(1.0d, ((double) r.markov2Sample) / 8.0d) * 12.0d;

        return AppConfig.clampScore100(score);
    }

    private double liftScore(TransitionRelationResult r) {
        if (r == null) {
            return 0.0d;
        }

        double score = 50.0d;
        score += r.bestLift * 180.0d;
        score += r.recentLift * 220.0d;

        if (r.markov1Mass >= 0.360d) {
            score += 6.0d;
        }

        if (r.markov2Mass >= 0.420d && r.markov2Sample >= 6) {
            score += 5.0d;
        }

        if (r.recentMass < r.midMass && r.midMass < r.markov1Mass) {
            score -= 5.0d;
        }

        return AppConfig.clampScore100(score);
    }

    private double recentScore(TransitionRelationResult r) {
        if (r == null) {
            return 0.0d;
        }

        double score = 0.0d;

        score += r.recentMass * 70.0d;
        score += r.midMass * 22.0d;
        score += r.markov1Mass * 8.0d;

        if (r.recentMass >= r.midMass && r.midMass >= r.markov1Mass) {
            score += 5.0d;
        }

        if (r.fromSampleRecent < 4) {
            score -= 8.0d;
        }

        return AppConfig.clampScore100(score);
    }

    private double finalRelationScore(TransitionRelationResult r) {
        if (r == null) {
            return 0.0d;
        }

        double score = 0.0d;

        score += r.transitionSupport * 0.28d;
        score += r.transitionLift * 0.28d;
        score += r.recentTransitionScore * 0.24d;
        score += AppConfig.clampScore100(r.markov1Mass * 100.0d) * 0.10d;
        score += AppConfig.clampScore100(r.markov2Mass * 100.0d) * 0.10d;

        if (r.fromSampleAll < 8) {
            score -= 12.0d;
        }

        if (r.fromSampleRecent <= 1 && r.fromSampleAll < 18) {
            score -= 8.0d;
        }

        if (r.markov1Mass < 0.260d && r.markov2Mass < 0.260d) {
            score -= 6.0d;
        }

        return AppConfig.clampScore100(score);
    }

    private String stateByScore(TransitionRelationResult r) {
        if (r == null) {
            return AppConfig.MODE_CHAOS;
        }

        if (r.relationScore >= SCORE_A_PLUS) {
            return AppConfig.MODE_ACCELERATING;
        }

        if (r.relationScore >= SCORE_STRONG) {
            return AppConfig.MODE_CONFIRMED;
        }

        if (r.relationScore >= SCORE_USABLE) {
            return AppConfig.MODE_FORMING;
        }

        if (r.transitionSupport >= 48.0d && r.recentTransitionScore >= 45.0d) {
            return AppConfig.MODE_RECOVERING;
        }

        if (r.relationScore >= 42.0d) {
            return AppConfig.MODE_WEAKENING;
        }

        return AppConfig.MODE_CHAOS;
    }

    private String blockByRisk(TransitionRelationResult r) {
        if (r == null) {
            return AppConfig.BLOCK_COMBO_WEAK;
        }

        if (r.fromSampleAll < 5) {
            return AppConfig.BLOCK_SAMPLE_WEAK;
        }

        if (r.relationScore < 38.0d) {
            return AppConfig.BLOCK_COMBO_WEAK;
        }

        if (r.relationScore < SCORE_USABLE && r.markov1Mass < 0.300d && r.markov2Mass < 0.300d) {
            return AppConfig.BLOCK_COMBO_WEAK;
        }

        return "";
    }

    private double candidateHitMass(TransitionRelationResult r, int mask) {
        if (r == null || !Models.isValidK3Mask(mask)) {
            return 0.0d;
        }

        double mass = 0.0d;

        if (Models.maskContains(mask, r.n1)) {
            mass += r.bestMarkov1P * 0.34d;
            mass += r.bestRecentP * 0.28d;
            mass += r.bestMarkov2P * 0.18d;
        }

        if (Models.maskContains(mask, r.n2)) {
            mass += r.markov1Mass * 0.08d;
            mass += r.recentMass * 0.07d;
        }

        if (Models.maskContains(mask, r.n3)) {
            mass += r.markov1Mass * 0.03d;
            mass += r.recentMass * 0.02d;
        }

        return AppConfig.clampProbability(mass);
    }

    private double candidateCoverage(TransitionRelationResult r, int mask) {
        if (r == null || !Models.isValidK3Mask(mask)) {
            return 0.0d;
        }

        int hit = 0;

        if (Models.maskContains(mask, r.n1)) hit++;
        if (Models.maskContains(mask, r.n2)) hit++;
        if (Models.maskContains(mask, r.n3)) hit++;

        return ((double) hit) / 3.0d;
    }

    private double max3(double a, double b, double c) {
        double m = a;
        if (b > m) m = b;
        if (c > m) m = c;
        return m;
    }

    private double blend01(double oldValue, double newValue, double weightNew) {
        oldValue = AppConfig.clampScore01(oldValue);
        newValue = AppConfig.clampScore01(newValue);

        if (weightNew < 0.0d) weightNew = 0.0d;
        if (weightNew > 1.0d) weightNew = 1.0d;

        return AppConfig.clampScore01(oldValue * (1.0d - weightNew) + newValue * weightNew);
    }

    private void sortAndRank(List<TransitionRelationResult> list) {
        if (list == null || list.size() <= 1) {
            return;
        }

        Collections.sort(
                list,
                new Comparator<TransitionRelationResult>() {
                    @Override
                    public int compare(TransitionRelationResult a, TransitionRelationResult b) {
                        double sa = a == null ? -999.0d : a.sortScore();
                        double sb = b == null ? -999.0d : b.sortScore();

                        if (sa < sb) return 1;
                        if (sa > sb) return -1;

                        int pa = a == null ? 999 : a.position;
                        int pb = b == null ? 999 : b.position;

                        return pa - pb;
                    }
                });

        for (int i = 0; i < list.size(); i++) {
            TransitionRelationResult r = list.get(i);
            if (r != null) {
                r.rank = i + 1;
            }
        }
    }

    private void sortCandidates(List<Models.ComboCandidate> list) {
        if (list == null || list.size() <= 1) {
            return;
        }

        Collections.sort(
                list,
                new Comparator<Models.ComboCandidate>() {
                    @Override
                    public int compare(Models.ComboCandidate a, Models.ComboCandidate b) {
                        double sa = a == null ? -999.0d : a.sortScore();
                        double sb = b == null ? -999.0d : b.sortScore();

                        if (sa < sb) return 1;
                        if (sa > sb) return -1;

                        int pa = a == null ? 999 : a.position;
                        int pb = b == null ? 999 : b.position;

                        return pa - pb;
                    }
                });

        for (int i = 0; i < list.size(); i++) {
            Models.ComboCandidate c = list.get(i);
            if (c != null) {
                c.rank = i + 1;
            }
        }
    }

    private String buildRelationKey(TransitionRelationResult r) {
        if (r == null) {
            return "";
        }

        return "TR_P" + r.position + "_F" + r.fromNumber + "_T" + r.n1 + "_" + r.n2 + "_" + r.n3;
    }

    private String buildSummary(TransitionRelationResult r) {
        if (r == null) {
            return "转移关系为空";
        }

        StringBuilder sb = new StringBuilder();

        sb.append("转移关系P");
        sb.append(r.position);
        sb.append("：上一号");
        sb.append(UiFormatter.number(r.fromNumber));
        sb.append(" -> ");
        sb.append(UiFormatter.numberGroup(r.n1, r.n2, r.n3));

        sb.append("，关系分=");
        sb.append(UiFormatter.score(r.relationScore));

        sb.append("，支持=");
        sb.append(UiFormatter.score(r.transitionSupport));

        sb.append("，提升=");
        sb.append(UiFormatter.score(r.transitionLift));

        sb.append("，近期=");
        sb.append(UiFormatter.score(r.recentTransitionScore));

        sb.append("，M1=");
        sb.append(UiFormatter.probability(r.markov1Mass));

        sb.append("，M2=");
        sb.append(UiFormatter.probability(r.markov2Mass));

        sb.append("，样本=");
        sb.append(r.fromSampleAll);

        if (r.blockCode != null && r.blockCode.length() > 0) {
            sb.append("，拦截=");
            sb.append(UiFormatter.blockReasonText(r.blockCode));
        } else {
            sb.append("，可作为候选证据");
        }

        return sb.toString();
    }

    private String buildCandidateSummary(TransitionRelationResult r, Models.ComboCandidate c) {
        if (r == null) {
            return "转移关系候选为空";
        }

        StringBuilder sb = new StringBuilder();

        sb.append(buildSummary(r));

        if (c != null) {
            sb.append("；候选=");
            sb.append(UiFormatter.positionAndNumbers(c.position, c.n1, c.n2, c.n3));
            sb.append("，候选转移覆盖=");
            sb.append(UiFormatter.probability(r.candidateNumberCoverage));
            sb.append("，候选转移分=");
            sb.append(UiFormatter.score(r.candidateRelationScore));
        }

        return sb.toString();
    }

    private String append(String oldText, String addText) {
        if (addText == null || addText.length() == 0) {
            return oldText == null ? "" : oldText;
        }

        if (oldText == null || oldText.length() == 0) {
            return addText;
        }

        return oldText + "；" + addText;
    }

    public static final class TransitionRelationResult {
        public int rank;
        public int position;
        public long createdAt;

        public String baseIssue;

        public int sampleCount;
        public int fromNumber;
        public int prevFromNumber;

        public int n1;
        public int n2;
        public int n3;
        public int k3Mask;

        public int fromSampleAll;
        public int fromSampleMid;
        public int fromSampleRecent;
        public int markov2Sample;

        public double markov1Mass;
        public double markov2Mass;
        public double midMass;
        public double recentMass;

        public double baseMass;
        public double recentBaseMass;

        public double bestMarkov1P;
        public double bestMarkov2P;
        public double bestRecentP;

        public double bestLift;
        public double recentLift;

        public double transitionSupport;
        public double transitionLift;
        public double recentTransitionScore;
        public double relationScore;
        public double score01;

        public int candidateMask;
        public double candidateHitMass;
        public double candidateNumberCoverage;
        public double candidateRelationScore;

        public int usable;
        public int strong;
        public int aPlusSupport;

        public String relationKey;
        public String stateMode;
        public String blockCode;
        public String summaryText;
        public double b06Support;
        public double b06CounterEvidence;
        public double b06Risk;
        public double b06Confidence;
        public double b06Freshness;
        public int b06EvidenceReady;
        public String b06PatternFingerprint;
        public String b06NumberFingerprint;
        public String b06Fingerprint;
        public String b06Summary;

        public TransitionRelationResult() {
            rank = 0;
            position = 0;
            createdAt = 0L;

            baseIssue = "";

            sampleCount = 0;
            fromNumber = 0;
            prevFromNumber = 0;

            n1 = 0;
            n2 = 0;
            n3 = 0;
            k3Mask = 0;

            fromSampleAll = 0;
            fromSampleMid = 0;
            fromSampleRecent = 0;
            markov2Sample = 0;

            markov1Mass = 0.0d;
            markov2Mass = 0.0d;
            midMass = 0.0d;
            recentMass = 0.0d;

            baseMass = 0.0d;
            recentBaseMass = 0.0d;

            bestMarkov1P = 0.0d;
            bestMarkov2P = 0.0d;
            bestRecentP = 0.0d;

            bestLift = 0.0d;
            recentLift = 0.0d;

            transitionSupport = 0.0d;
            transitionLift = 0.0d;
            recentTransitionScore = 0.0d;
            relationScore = 0.0d;
            score01 = 0.0d;

            candidateMask = 0;
            candidateHitMass = 0.0d;
            candidateNumberCoverage = 0.0d;
            candidateRelationScore = 0.0d;

            usable = 0;
            strong = 0;
            aPlusSupport = 0;

            relationKey = "";
            stateMode = AppConfig.MODE_CHAOS;
            blockCode = "";
            summaryText = "";
            b06Support = 0.0d;
            b06CounterEvidence = 1.0d;
            b06Risk = 1.0d;
            b06Confidence = 0.0d;
            b06Freshness = 0.0d;
            b06EvidenceReady = 0;
            b06PatternFingerprint = "";
            b06NumberFingerprint = "";
            b06Fingerprint = "";
            b06Summary = "";
        }

        public boolean isValidK3Numbers() {
            return AppConfig.isValidK3Numbers(n1, n2, n3) && Models.isValidK3Mask(k3Mask);
        }

        public boolean isUsableForBet() {
            return usable == 1 && relationScore >= SCORE_USABLE && isValidK3Numbers();
        }

        public double sortScore() {
            double s = relationScore;
            s += transitionSupport * 0.08d;
            s += transitionLift * 0.10d;
            s += recentTransitionScore * 0.08d;
            s += markov1Mass * 8.0d;
            s += markov2Mass * 6.0d;

            if (usable == 0) {
                s -= 10.0d;
            }

            if (strong == 1) {
                s += 3.0d;
            }

            if (aPlusSupport == 1) {
                s += 4.0d;
            }

            return s;
        }

        public TransitionRelationResult copy() {
            TransitionRelationResult r = new TransitionRelationResult();

            r.rank = rank;
            r.position = position;
            r.createdAt = createdAt;

            r.baseIssue = baseIssue;

            r.sampleCount = sampleCount;
            r.fromNumber = fromNumber;
            r.prevFromNumber = prevFromNumber;

            r.n1 = n1;
            r.n2 = n2;
            r.n3 = n3;
            r.k3Mask = k3Mask;

            r.fromSampleAll = fromSampleAll;
            r.fromSampleMid = fromSampleMid;
            r.fromSampleRecent = fromSampleRecent;
            r.markov2Sample = markov2Sample;

            r.markov1Mass = markov1Mass;
            r.markov2Mass = markov2Mass;
            r.midMass = midMass;
            r.recentMass = recentMass;

            r.baseMass = baseMass;
            r.recentBaseMass = recentBaseMass;

            r.bestMarkov1P = bestMarkov1P;
            r.bestMarkov2P = bestMarkov2P;
            r.bestRecentP = bestRecentP;

            r.bestLift = bestLift;
            r.recentLift = recentLift;

            r.transitionSupport = transitionSupport;
            r.transitionLift = transitionLift;
            r.recentTransitionScore = recentTransitionScore;
            r.relationScore = relationScore;
            r.score01 = score01;

            r.candidateMask = candidateMask;
            r.candidateHitMass = candidateHitMass;
            r.candidateNumberCoverage = candidateNumberCoverage;
            r.candidateRelationScore = candidateRelationScore;

            r.usable = usable;
            r.strong = strong;
            r.aPlusSupport = aPlusSupport;

            r.relationKey = relationKey;
            r.stateMode = stateMode;
            r.blockCode = blockCode;
            r.summaryText = summaryText;
            r.b06Support = b06Support;
            r.b06CounterEvidence = b06CounterEvidence;
            r.b06Risk = b06Risk;
            r.b06Confidence = b06Confidence;
            r.b06Freshness = b06Freshness;
            r.b06EvidenceReady = b06EvidenceReady;
            r.b06PatternFingerprint = b06PatternFingerprint;
            r.b06NumberFingerprint = b06NumberFingerprint;
            r.b06Fingerprint = b06Fingerprint;
            r.b06Summary = b06Summary;

            return r;
        }
    }

    private PatternStateEngine.PatternStateResult buildPatternEvidence(List<Models.Draw> clean) {
        PatternStateEngine engine = new PatternStateEngine();
        ArrayList<PatternStateEngine.DrawRecord> records =
                new ArrayList<PatternStateEngine.DrawRecord>();
        if (clean != null)
            for (int i = 0; i < clean.size(); i++) {
                Models.Draw d = clean.get(i);
                if (d != null && d.isValid())
                    records.add(
                            new PatternStateEngine.DrawRecord(
                                    d.issue, d.toOpenCodeText(), d.openTime));
            }
        String baseIssue =
                clean == null || clean.size() == 0 ? "" : clean.get(clean.size() - 1).issue;
        return engine.analyze(records, baseIssue);
    }

    private NumberBehaviorEngine.NumberBehaviorResult buildNumberBehaviorEvidence(
            List<Models.Draw> clean) {
        NumberBehaviorEngine engine = new NumberBehaviorEngine();
        ArrayList<NumberBehaviorEngine.DrawRecord> records =
                new ArrayList<NumberBehaviorEngine.DrawRecord>();
        if (clean != null)
            for (int i = 0; i < clean.size(); i++) {
                Models.Draw d = clean.get(i);
                if (d != null && d.isValid())
                    records.add(
                            new NumberBehaviorEngine.DrawRecord(
                                    d.issue, d.toOpenCodeText(), d.openTime));
            }
        String baseIssue =
                clean == null || clean.size() == 0 ? "" : clean.get(clean.size() - 1).issue;
        return engine.analyze(records, baseIssue);
    }

    private void attachB06Evidence(
            TransitionRelationResult r,
            PatternStateEngine.PatternStateResult pattern,
            NumberBehaviorEngine.NumberBehaviorResult behavior) {
        if (r == null) return;
        PatternStateEngine.B06PatternEvidencePacket pp = null;
        NumberBehaviorEngine.B06NumberEvidencePacket[] np = null;
        if (pattern != null
                && pattern.b06Evidence != null
                && AppConfig.isValidPosition(r.position)) {
            pp = pattern.b06Evidence.packets[r.position - 1];
            r.b06PatternFingerprint = pattern.b06Evidence.fingerprint;
        }
        if (behavior != null
                && behavior.b06Evidence != null
                && AppConfig.isValidPosition(r.position)) {
            np = behavior.b06Evidence.packets[r.position - 1];
            r.b06NumberFingerprint = behavior.b06Evidence.fingerprint;
        }
        double numberSupport = 0.0d,
                numberCounter = 0.0d,
                numberRisk = 0.0d,
                numberConfidence = 0.0d,
                numberFreshness = 0.0d;
        int numberCount = 0;
        int[] nums = new int[] {r.n1, r.n2, r.n3};
        if (np != null)
            for (int i = 0; i < nums.length; i++) {
                int n = nums[i];
                if (!AppConfig.isValidNumber(n) || np[n - 1] == null) continue;
                NumberBehaviorEngine.B06NumberEvidencePacket p = np[n - 1];
                numberSupport += p.support;
                numberCounter += p.counterEvidence;
                numberRisk += p.risk;
                numberConfidence += p.confidence;
                numberFreshness += p.freshness;
                numberCount++;
            }
        if (numberCount > 0) {
            numberSupport /= numberCount;
            numberCounter /= numberCount;
            numberRisk /= numberCount;
            numberConfidence /= numberCount;
            numberFreshness /= numberCount;
        }
        double relationSupport = AppConfig.clampScore01(r.relationScore / 100.0d);
        double relationCounter = AppConfig.clampScore01(1.0d - relationSupport);
        r.b06Support =
                AppConfig.clampScore01(
                        relationSupport * 0.50d
                                + (pp == null ? 0.0d : pp.support) * 0.25d
                                + numberSupport * 0.25d);
        r.b06CounterEvidence =
                AppConfig.clampScore01(
                        relationCounter * 0.40d
                                + (pp == null ? 1.0d : pp.counterEvidence) * 0.30d
                                + numberCounter * 0.30d);
        r.b06Risk =
                AppConfig.clampScore01(
                        r.b06CounterEvidence * 0.45d
                                + (pp == null ? 1.0d : pp.risk) * 0.30d
                                + numberRisk * 0.25d);
        r.b06Confidence =
                AppConfig.clampScore01(
                        Math.min(1.0d, r.fromSampleAll / 80.0d) * 0.35d
                                + (pp == null ? 0.0d : pp.confidence) * 0.30d
                                + numberConfidence * 0.25d
                                + (1.0d - r.b06Risk) * 0.10d);
        r.b06Freshness =
                AppConfig.clampScore01(
                        r.recentTransitionScore / 100.0d * 0.45d
                                + (pp == null ? 0.0d : pp.freshness) * 0.25d
                                + numberFreshness * 0.30d);
        r.b06EvidenceReady = r.fromSampleAll >= 12 && r.b06Confidence >= 0.35d ? 1 : 0;
        String payload =
                B06_CONTRACT_VERSION
                        + "|"
                        + r.baseIssue
                        + "|"
                        + r.position
                        + "|"
                        + r.fromNumber
                        + "|"
                        + r.k3Mask
                        + "|"
                        + r.fromSampleAll
                        + "|"
                        + Double.doubleToLongBits(r.b06Support)
                        + "|"
                        + Double.doubleToLongBits(r.b06CounterEvidence)
                        + "|"
                        + Double.doubleToLongBits(r.b06Risk)
                        + "|"
                        + Double.doubleToLongBits(r.b06Confidence)
                        + "|"
                        + Double.doubleToLongBits(r.b06Freshness)
                        + "|"
                        + r.b06EvidenceReady
                        + "|"
                        + r.b06PatternFingerprint
                        + "|"
                        + r.b06NumberFingerprint
                        + "|B06_TRANSITION_RELATION";
        r.b06Fingerprint = V51ChecksumEngine.sha256(payload);
        r.b06Summary =
                "B06转移证据：支持="
                        + UiFormatter.probability(r.b06Support)
                        + "，反证="
                        + UiFormatter.probability(r.b06CounterEvidence)
                        + "，风险="
                        + UiFormatter.probability(r.b06Risk)
                        + "，置信="
                        + UiFormatter.probability(r.b06Confidence)
                        + "，所有权=EvidenceOnly";
    }

    private String shortHash(String hash) {
        if (hash == null) return "";
        return hash.length() <= 12 ? hash : hash.substring(0, 12);
    }
}
