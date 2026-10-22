package com.ethk3.novaonepro;

/**
 * Prospective ablation audit. It measures whether a candidate is supported by several independent
 * evidence families or is a fragile single-story candidate. It never uses the target result and
 * therefore is safe before the draw.
 */
public final class StrategyAblationAuditEngine {
    public static final String MODULE_VERSION = AppConfig.V43_ABLATION_VERSION;

    public static final class Result {
        public double fullScore;
        public double maxDrop;
        public double fragility;
        public String dominantGroup = "";
        public String summary = "";
    }

    private StrategyAblationAuditEngine() {}

    public static Result evaluateStrictOos(
            DbStore store,
            String targetIssue,
            Models.ComboCandidate c,
            StrategyEvidenceContract.Snapshot snapshot) {
        Result unavailable = new Result();
        if (store == null || targetIssue == null || targetIssue.length() == 0) {
            unavailable.summary = "UNAVAILABLE|STRICT_OOS_REQUIRED";
            return unavailable;
        }
        android.database.Cursor cursor = null;
        try {
            cursor =
                    store.readable()
                            .rawQuery(
                                    "SELECT 1 FROM "
                                            + DbHelper.TABLE_R35_OOS_LABEL
                                            + " WHERE target_issue=? AND strict_oos=1 LIMIT 1",
                                    new String[] {targetIssue});
            if (!cursor.moveToFirst()) {
                unavailable.summary = "UNAVAILABLE|NO_STRICT_OOS|target=" + targetIssue;
                return unavailable;
            }
            return evaluate(c, snapshot);
        } finally {
            if (cursor != null) cursor.close();
        }
    }

    public static Result evaluate(Models.ComboCandidate c, StrategyEvidenceContract.Snapshot s) {
        Result r = new Result();
        if (s == null) s = StrategyEvidenceContract.capture(c);
        double[] v =
                new double[] {
                    s.shape,
                    s.sequence,
                    s.causal,
                    s.teacher,
                    s.counterfactualSupport,
                    s.dynamic,
                    s.calibration
                };
        String[] n =
                new String[] {
                    StrategyEvidenceContract.GROUP_SHAPE,
                    StrategyEvidenceContract.GROUP_SEQUENCE,
                    StrategyEvidenceContract.GROUP_CAUSAL,
                    StrategyEvidenceContract.GROUP_TEACHER,
                    StrategyEvidenceContract.GROUP_COUNTERFACTUAL,
                    StrategyEvidenceContract.GROUP_DYNAMIC,
                    StrategyEvidenceContract.GROUP_CALIBRATION
                };
        r.fullScore = combined(v, -1, s.risk);
        double largestDrop = 0.0d;
        String dominant = "NONE";
        for (int i = 0; i < v.length; i++) {
            double without = combined(v, i, s.risk);
            double drop = Math.max(0.0d, r.fullScore - without);
            if (drop > largestDrop) {
                largestDrop = drop;
                dominant = n[i];
            }
        }
        r.maxDrop = largestDrop;
        r.fragility =
                clamp01(
                        largestDrop * 2.6d
                                + Math.max(0.0d, 0.58d - s.diversity) * 0.55d
                                + s.risk * 0.20d);
        r.dominantGroup = dominant;
        r.summary =
                "V43消融：full="
                        + f(r.fullScore)
                        + "|maxDrop="
                        + f(r.maxDrop)
                        + "|fragility="
                        + f(r.fragility)
                        + "|dominant="
                        + dominant;
        if (c != null) {
            c.v43AblationFragility = r.fragility;
            c.v43DominantEvidence = dominant;
            if (c.reasonSummary == null || c.reasonSummary.length() == 0)
                c.reasonSummary = r.summary;
            else if (c.reasonSummary.indexOf(r.summary) < 0) c.reasonSummary += "；" + r.summary;
        }
        return r;
    }

    private static double combined(double[] v, int skip, double risk) {
        double sum = 0.0d;
        double weight = 0.0d;
        for (int i = 0; i < v.length; i++) {
            if (i == skip) continue;
            double w = (i == 2 || i == 4 || i == 6) ? 1.12d : 1.0d;
            sum += w * clamp01(v[i]);
            weight += w;
        }
        return clamp01((weight <= 0.0d ? 0.0d : sum / weight) - 0.28d * clamp01(risk));
    }

    private static double clamp01(double v) {
        return v < 0.0d ? 0.0d : (v > 1.0d ? 1.0d : v);
    }

    private static String f(double v) {
        return String.valueOf(Math.round(v * 1000.0d) / 1000.0d);
    }
}
