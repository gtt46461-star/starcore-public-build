package com.ethk3.novaonepro;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

/** Proper-scoring reliability ledger for expert/position/state/lifecycle/domain. */
public final class V50ExpertRegretEngine {
    public static final class Reliability {
        public final int sample;
        public final double historical;
        public final double recent;
        public final double state;
        public final V50Enums.ExpertStatus status;
        public final double weight;

        Reliability(
                int sample,
                double historical,
                double recent,
                double state,
                V50Enums.ExpertStatus status) {
            this.sample = sample;
            this.historical = clamp01(historical);
            this.recent = clamp01(recent);
            this.state = clamp01(state);
            this.status = status == null ? V50Enums.ExpertStatus.WATCH : status;
            this.weight = V50ExpertHealthRegistry.weight(this.status);
        }
    }

    public Reliability[][] load(
            SQLiteDatabase db,
            V50Models.ExpertEvidence[] experts,
            V50Models.StateTrajectorySnapshot[] states) {
        int count = experts == null ? 0 : experts.length;
        Reliability[][] out = new Reliability[count][10];
        int[][] specificity = new int[count][10];
        for (int e = 0; e < count; e++) {
            boolean exportedOos =
                    experts[e] != null
                            && "OFFLINE_EXPORTED_NUMBER_MODEL".equals(experts[e].expertId);
            for (int p = 0; p < 10; p++) {
                out[e][p] =
                        exportedOos
                                ? new Reliability(
                                        0, 0.58d, 0.58d, 0.58d, V50Enums.ExpertStatus.READY)
                                : new Reliability(
                                        0, 0.0d, 0.0d, 0.0d, V50Enums.ExpertStatus.ZERO_WEIGHT);
                specificity[e][p] = -1;
            }
        }
        if (db == null || count == 0) return out;
        Cursor c = null;
        try {
            c =
                    db.query(
                            SchemaContractRegistry.TABLE_EXPERT,
                            new String[] {
                                "expert_id",
                                "position",
                                "state_family",
                                "lifecycle",
                                "domain_id",
                                "uncertainty_bucket",
                                "sample",
                                "log_loss_sum",
                                "recent_loss",
                                "mislead_run",
                                "status"
                            },
                            null,
                            null,
                            null,
                            null,
                            null);
            while (c.moveToNext()) {
                int expertIndex = expertIndex(experts, safe(c.getString(0)));
                int position = c.getInt(1);
                if (expertIndex < 0 || position < 1 || position > 10) continue;
                V50Models.StateTrajectorySnapshot state = V50ExpertSupport.state(states, position);
                String family =
                        state == null || state.prototypes.length == 0
                                ? "*"
                                : state.prototypes[0].prototypeId;
                String lifecycle =
                        state == null ? V50Enums.Lifecycle.UNKNOWN.name() : state.lifecycle.name();
                String domain = state == null ? "*" : state.dynamicDomain;
                String bucket = uncertaintyBucket(experts[expertIndex].uncertainty[position - 1]);
                int score =
                        matchScore(safe(c.getString(2)), family)
                                + matchScore(safe(c.getString(3)), lifecycle)
                                + matchScore(safe(c.getString(4)), domain)
                                + matchScore(safe(c.getString(5)), bucket);
                if (score < 0 || score < specificity[expertIndex][position - 1]) continue;
                int sample = c.getInt(6);
                double meanLog = sample <= 0 ? Math.log(10.0d) : c.getDouble(7) / sample;
                double recentLoss = c.getDouble(8);
                int mislead = c.getInt(9);
                V50Enums.ExpertStatus previous = parseStatus(c.getString(10));
                V50Enums.ExpertStatus status =
                        V50ExpertHealthRegistry.resolve(
                                sample, meanLog, recentLoss, mislead, previous);
                double historical =
                        1.0d - Math.max(0.0d, Math.min(1.0d, (meanLog - 1.75d) / 1.25d));
                double recent = 1.0d - Math.max(0.0d, Math.min(1.0d, (recentLoss - 1.75d) / 1.25d));
                double stateReliability =
                        Math.min(1.0d, sample / 80.0d) * (0.55d * historical + 0.45d * recent);
                out[expertIndex][position - 1] =
                        new Reliability(sample, historical, recent, stateReliability, status);
                specificity[expertIndex][position - 1] = score;
            }
        } catch (Throwable ignore) {
            return out;
        } finally {
            if (c != null) c.close();
        }
        return out;
    }

    public void update(
            SQLiteDatabase db,
            String expertId,
            int position,
            String stateFamily,
            V50Enums.Lifecycle lifecycle,
            String domainId,
            double uncertainty,
            double[] probability,
            int actualNumber) {
        if (db == null
                || probability == null
                || probability.length != 10
                || actualNumber < 1
                || actualNumber > 10
                || position < 1
                || position > 10) return;
        String bucket = uncertaintyBucket(uncertainty);
        String lifecycleName =
                lifecycle == null ? V50Enums.Lifecycle.UNKNOWN.name() : lifecycle.name();
        Row row = read(db, expertId, position, stateFamily, lifecycleName, domainId, bucket);
        int top3Hit = containsTop3(probability, actualNumber) ? 1 : 0;
        double logLoss = -Math.log(Math.max(1.0e-12d, probability[actualNumber - 1]));
        double brier = 0.0d;
        for (int n = 0; n < 10; n++) {
            double label = n == actualNumber - 1 ? 1.0d : 0.0d;
            double error = probability[n] - label;
            brier += error * error;
        }
        double top3P = top3P(probability);
        double calibrationError = Math.abs(top3P - top3Hit);
        int sample = row.sample + 1;
        double recent = row.sample == 0 ? logLoss : 0.90d * row.recentLoss + 0.10d * logLoss;
        int misleading = top3Hit == 0 ? row.misleadRun + 1 : 0;
        double meanLog = (row.logLossSum + logLoss) / sample;
        V50Enums.ExpertStatus status =
                V50ExpertHealthRegistry.resolve(sample, meanLog, recent, misleading, row.status);

        ContentValues cv = new ContentValues();
        cv.put("sample", sample);
        cv.put("log_loss_sum", row.logLossSum + logLoss);
        cv.put("brier_sum", row.brierSum + brier);
        cv.put("top3_hit", row.top3Hit + top3Hit);
        cv.put("rank_contribution", row.rankContribution + (top3Hit == 1 ? 1.0d : -1.0d));
        cv.put("calibration_error", row.calibrationError + calibrationError);
        cv.put("recent_loss", recent);
        cv.put("mislead_run", misleading);
        cv.put("status", status.name());
        cv.put("updated_at", System.currentTimeMillis());
        int changed =
                db.update(
                        SchemaContractRegistry.TABLE_EXPERT,
                        cv,
                        keyWhere(),
                        keyArgs(expertId, position, stateFamily, lifecycleName, domainId, bucket));
        if (changed == 0) {
            cv.put("expert_id", safe(expertId));
            cv.put("position", position);
            cv.put("state_family", safeOrStar(stateFamily));
            cv.put("lifecycle", safe(lifecycleName));
            cv.put("domain_id", safeOrStar(domainId));
            cv.put("uncertainty_bucket", bucket);
            long inserted = db.insert(SchemaContractRegistry.TABLE_EXPERT, null, cv);
            if (inserted < 0L) throw new IllegalStateException("V50_EXPERT_REGRET_INSERT_FAILED");
        } else if (changed != 1) {
            throw new IllegalStateException("V50_EXPERT_REGRET_UPDATE_ROWS=" + changed);
        }
    }

    private static Row read(
            SQLiteDatabase db,
            String expertId,
            int position,
            String stateFamily,
            String lifecycle,
            String domain,
            String bucket) {
        Cursor c = null;
        try {
            c =
                    db.query(
                            SchemaContractRegistry.TABLE_EXPERT,
                            new String[] {
                                "sample",
                                "log_loss_sum",
                                "brier_sum",
                                "top3_hit",
                                "rank_contribution",
                                "calibration_error",
                                "recent_loss",
                                "mislead_run",
                                "status"
                            },
                            keyWhere(),
                            keyArgs(expertId, position, stateFamily, lifecycle, domain, bucket),
                            null,
                            null,
                            null,
                            "1");
            if (c.moveToFirst()) {
                Row row = new Row();
                row.sample = c.getInt(0);
                row.logLossSum = c.getDouble(1);
                row.brierSum = c.getDouble(2);
                row.top3Hit = c.getInt(3);
                row.rankContribution = c.getDouble(4);
                row.calibrationError = c.getDouble(5);
                row.recentLoss = c.getDouble(6);
                row.misleadRun = c.getInt(7);
                row.status = parseStatus(c.getString(8));
                return row;
            }
        } finally {
            if (c != null) c.close();
        }
        return new Row();
    }

    private static int matchScore(String row, String target) {
        if ("*".equals(row) || "UNKNOWN".equals(row)) return 0;
        return safe(target).equals(row) ? 2 : -100;
    }

    private static int expertIndex(V50Models.ExpertEvidence[] experts, String id) {
        for (int i = 0; i < experts.length; i++) {
            if (experts[i] != null && safe(experts[i].expertId).equals(id)) return i;
        }
        return -1;
    }

    private static boolean containsTop3(double[] p, int actual) {
        int[] top = V49ExpertSupport.top3(p);
        return top[0] == actual - 1 || top[1] == actual - 1 || top[2] == actual - 1;
    }

    private static double top3P(double[] p) {
        int[] top = V49ExpertSupport.top3(p);
        return p[top[0]] + p[top[1]] + p[top[2]];
    }

    static String uncertaintyBucket(double uncertainty) {
        if (uncertainty < 0.78d) return "LOW";
        if (uncertainty > 0.94d) return "HIGH";
        return "MID";
    }

    private static V50Enums.ExpertStatus parseStatus(String value) {
        try {
            return V50Enums.ExpertStatus.valueOf(safe(value));
        } catch (Throwable ignore) {
            return V50Enums.ExpertStatus.WATCH;
        }
    }

    private static String keyWhere() {
        return "expert_id=? AND position=? AND state_family=? AND lifecycle=?"
                + " AND domain_id=? AND uncertainty_bucket=?";
    }

    private static String[] keyArgs(
            String expertId,
            int position,
            String stateFamily,
            String lifecycle,
            String domain,
            String bucket) {
        return new String[] {
            safe(expertId),
            String.valueOf(position),
            safeOrStar(stateFamily),
            safe(lifecycle),
            safeOrStar(domain),
            safe(bucket)
        };
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static String safeOrStar(String value) {
        String out = safe(value);
        return out.length() == 0 ? "*" : out;
    }

    private static double clamp01(double value) {
        return FeatureContractRegistry.clamp01(value);
    }

    private static final class Row {
        int sample;
        double logLossSum;
        double brierSum;
        int top3Hit;
        double rankContribution;
        double calibrationError;
        double recentLoss;
        int misleadRun;
        V50Enums.ExpertStatus status = V50Enums.ExpertStatus.WATCH;
    }
}
