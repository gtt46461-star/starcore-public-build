package com.ethk3.novaonepro;

/** Issue-level ablation comparison; it never treats action rows as independent observations. */
public final class ResearchAblationR815 {
    public static final class Pair {
        public final String targetIssue;
        public final boolean baselineHit;
        public final boolean candidateHit;
        public final double baselineBrier;
        public final double candidateBrier;

        public Pair(
                String targetIssue,
                boolean baselineHit,
                boolean candidateHit,
                double baselineBrier,
                double candidateBrier) {
            this.targetIssue = targetIssue == null ? "" : targetIssue.trim();
            this.baselineHit = baselineHit;
            this.candidateHit = candidateHit;
            this.baselineBrier = finite(baselineBrier);
            this.candidateBrier = finite(candidateBrier);
            if (!TargetIssueResolver.isValid(this.targetIssue))
                throw new IllegalArgumentException("R815_ABLATION_ISSUE_INVALID");
        }
    }

    public static final class Result {
        public final int issues;
        public final int candidateWins;
        public final int baselineWins;
        public final int ties;
        public final double hitDelta;
        public final double brierDelta;
        public final String checksum;

        Result(int issues, int candidateWins, int baselineWins, int ties, double hitDelta, double brierDelta) {
            this.issues = issues;
            this.candidateWins = candidateWins;
            this.baselineWins = baselineWins;
            this.ties = ties;
            this.hitDelta = hitDelta;
            this.brierDelta = brierDelta;
            checksum = V51ChecksumEngine.sha256(issues + "|" + candidateWins + "|" + baselineWins + "|" + ties + "|" + hitDelta + "|" + brierDelta);
        }
    }

    private ResearchAblationR815() {}

    public static Result compare(Pair[] pairs) {
        if (pairs == null || pairs.length == 0) return new Result(0, 0, 0, 0, 0.0d, 0.0d);
        int candidateWins = 0, baselineWins = 0, ties = 0, baselineHits = 0, candidateHits = 0;
        double baselineBrier = 0.0d, candidateBrier = 0.0d;
        String prior = "";
        for (int i = 0; i < pairs.length; i++) {
            Pair pair = pairs[i];
            if (pair == null || !TargetIssueResolver.isAfter(pair.targetIssue, prior) && prior.length() > 0)
                throw new IllegalArgumentException("R815_ABLATION_ISSUE_ORDER");
            prior = pair.targetIssue;
            if (pair.candidateHit) candidateHits++;
            if (pair.baselineHit) baselineHits++;
            if (pair.candidateHit && !pair.baselineHit) candidateWins++;
            else if (!pair.candidateHit && pair.baselineHit) baselineWins++;
            else ties++;
            baselineBrier += pair.baselineBrier;
            candidateBrier += pair.candidateBrier;
        }
        int issues = pairs.length;
        return new Result(
                issues,
                candidateWins,
                baselineWins,
                ties,
                (candidateHits - baselineHits) / (double) issues,
                (candidateBrier - baselineBrier) / issues);
    }

    private static double finite(double value) {
        return Double.isNaN(value) || Double.isInfinite(value) ? 1.0d : Math.max(0.0d, value);
    }
}
