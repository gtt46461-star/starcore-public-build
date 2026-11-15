package com.ethk3.novaonepro;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;

/** B11 strict settled-OOS certification for the selected action after selection. */
public final class SelectedHitCertificationEngine {
    public static final String V57_B11_OWNER = "SELECTED_HIT_OOS_CERTIFICATION";
    public static final int MIN_SAMPLE_40 = 120;
    public static final double MIN_COVERAGE = 0.15d, MAX_ECE = 0.04d, MAX_DRIFT = 0.08d;

    public static final class Sample {
        public final String issue, domain;
        public final double p;
        public final int y;
        public final long settledAt;

        Sample(String i, String d, double p, int y, long at) {
            issue = i;
            domain = d;
            this.p = p;
            this.y = y;
            settledAt = at;
        }
    }

    public static final class Certificate {
        public final String domain;
        public final int sample, hit, totalPredictions;
        public final double hitRate,
                ece,
                brier,
                wilsonLower,
                betaLower,
                coverage,
                rolling30,
                rolling60,
                rolling120,
                blockMinimum,
                driftRisk;
        public final int stableBlocks;
        public final boolean artifactProven, proven40, proven50, proven60;
        public final String reason, checksum;

        Certificate(
                String d,
                int n,
                int h,
                int total,
                double hr,
                double e,
                double b,
                double wl,
                double bl,
                double cov,
                double r30,
                double r60,
                double r120,
                double block,
                int stable,
                double drift,
                boolean artifact) {
            domain = d;
            sample = n;
            hit = h;
            totalPredictions = total;
            hitRate = hr;
            ece = e;
            brier = b;
            wilsonLower = wl;
            betaLower = bl;
            coverage = cov;
            rolling30 = r30;
            rolling60 = r60;
            rolling120 = r120;
            blockMinimum = block;
            stableBlocks = stable;
            driftRisk = drift;
            artifactProven = artifact;
            proven40 =
                    artifact
                            && n >= MIN_SAMPLE_40
                            && cov >= MIN_COVERAGE
                            && hr >= 0.40d
                            && wl >= 0.35d
                            && bl >= 0.35d
                            && e <= MAX_ECE
                            && block >= 0.33d
                            && stable >= 3
                            && drift <= MAX_DRIFT;
            proven50 =
                    artifact
                            && n >= 240
                            && cov >= MIN_COVERAGE
                            && hr >= 0.50d
                            && wl >= 0.44d
                            && e <= 0.035d
                            && block >= 0.42d
                            && stable >= 3
                            && drift <= 0.07d;
            proven60 =
                    artifact
                            && n >= 480
                            && cov >= MIN_COVERAGE
                            && hr >= 0.60d
                            && wl >= 0.54d
                            && e <= 0.03d
                            && block >= 0.52d
                            && stable >= 4
                            && drift <= 0.06d;
            reason = reason(proven40, artifact, n, cov, e, block, stable, drift);
            checksum = V51ChecksumEngine.sha256(canonical());
        }

        public String canonical() {
            return domain
                    + '|'
                    + sample
                    + '|'
                    + hit
                    + '|'
                    + totalPredictions
                    + '|'
                    + hitRate
                    + '|'
                    + ece
                    + '|'
                    + brier
                    + '|'
                    + wilsonLower
                    + '|'
                    + betaLower
                    + '|'
                    + coverage
                    + '|'
                    + rolling30
                    + '|'
                    + rolling60
                    + '|'
                    + rolling120
                    + '|'
                    + blockMinimum
                    + '|'
                    + stableBlocks
                    + '|'
                    + driftRisk
                    + '|'
                    + artifactProven
                    + '|'
                    + proven40
                    + '|'
                    + proven50
                    + '|'
                    + proven60
                    + '|'
                    + reason;
        }
    }

    public Certificate certify(DbStore store, String domain) {
        ArrayList<Sample> samples = new ArrayList<Sample>();
        int total = 0;
        if (store != null) {
            SQLiteDatabase db = store.readable();
            R811SchemaIntegrityVerifier.assertV51ReadyReadOnly(db);
            Cursor t = null, c = null;
            try {
                t =
                        db.rawQuery(
                                "SELECT COUNT(1) FROM "
                                        + V51Repository.ISSUE
                                        + " WHERE game_code=?",
                                new String[] {V51Repository.GAME_CODE});
                if (t.moveToFirst()) total = t.getInt(0);
                c =
                        db.rawQuery(
                                "SELECT target_issue,domain_id,selected_hit_p,result,rowid FROM "
                                        + V51Repository.SELECTED
                                        + " WHERE game_code=? AND settled=1 AND selected_hit_p>=0"
                                        + " AND selected_hit_p<=1 AND result IN ('HIT','MISS') AND"
                                        + " domain_id=? ORDER BY target_issue ASC,rowid ASC",
                                new String[] {
                                    V51Repository.GAME_CODE, domain == null ? "" : domain
                                });
                while (c.moveToNext())
                    samples.add(
                            new Sample(
                                    c.getString(0),
                                    c.getString(1),
                                    c.getDouble(2),
                                    "HIT".equals(c.getString(3)) ? 1 : 0,
                                    c.getLong(4)));
            } finally {
                if (t != null) t.close();
                if (c != null) c.close();
            }
        }
        return compute(
                domain,
                samples,
                total,
                V51SelectedCalibrationArtifact.PROVEN
                        && V51ActionRankerArtifact.PROVEN
                        && V51ActionRankerArtifact.RANK_ORDER_PASSED
                        && V51NumberModelArtifact.PROVEN
                        && V51ExpertGateArtifact.PROVEN);
    }

    public Certificate compute(
            String domain,
            java.util.List<Sample> xs,
            int totalPredictions,
            boolean artifactProven) {
        int n = xs == null ? 0 : xs.size(), hit = 0;
        double brier = 0;
        int[] bn = new int[10];
        double[] bp = new double[10], by = new double[10];
        if (xs != null)
            for (int i = 0; i < n; i++) {
                Sample s = xs.get(i);
                hit += s.y;
                double d = s.p - s.y;
                brier += d * d;
                int k = Math.min(9, Math.max(0, (int) Math.floor(s.p * 10)));
                bn[k]++;
                bp[k] += s.p;
                by[k] += s.y;
            }
        double hr = n == 0 ? 0 : (double) hit / n;
        brier = n == 0 ? 1 : brier / n;
        double ece = 0;
        for (int k = 0; k < 10; k++)
            if (bn[k] > 0)
                ece += ((double) bn[k] / Math.max(1, n)) * Math.abs(bp[k] / bn[k] - by[k] / bn[k]);
        double[] block = blocks(xs, 4);
        double min = 1;
        int stable = 0;
        for (int i = 0; i < block.length; i++) {
            min = Math.min(min, block[i]);
            if (block[i] >= 0.30d) stable++;
        }
        if (n == 0) min = 0;
        double drift = n < 20 ? 1.0d : Math.abs(rate(xs, 0, n / 2) - rate(xs, n / 2, n));
        double cov = totalPredictions <= 0 ? 0 : (double) n / totalPredictions;
        return new Certificate(
                domain == null ? "" : domain,
                n,
                hit,
                totalPredictions,
                hr,
                ece,
                brier,
                wilson(hit, n),
                betaLower(hit, n),
                cov,
                rolling(xs, 30),
                rolling(xs, 60),
                rolling(xs, 120),
                min,
                stable,
                drift,
                artifactProven);
    }

    private static double rolling(java.util.List<Sample> x, int w) {
        if (x == null || x.size() == 0) return 0;
        int from = Math.max(0, x.size() - w), h = 0;
        for (int i = from; i < x.size(); i++) h += x.get(i).y;
        return (double) h / (x.size() - from);
    }

    private static double rate(java.util.List<Sample> x, int a, int b) {
        if (x == null || b <= a) return 0;
        int h = 0;
        for (int i = a; i < b; i++) h += x.get(i).y;
        return (double) h / (b - a);
    }

    private static double[] blocks(java.util.List<Sample> x, int k) {
        double[] out = new double[k];
        int n = x == null ? 0 : x.size();
        for (int b = 0; b < k; b++) {
            int a = n * b / k, z = n * (b + 1) / k;
            out[b] = rate(x, a, z);
        }
        return out;
    }

    private static double wilson(int h, int n) {
        if (n <= 0) return 0;
        double z = 1.959963984540054,
                p = (double) h / n,
                z2 = z * z,
                den = 1 + z2 / n,
                center = p + z2 / (2 * n),
                adj = z * Math.sqrt((p * (1 - p) + z2 / (4 * n)) / n);
        return Math.max(0, (center - adj) / den);
    }

    private static double betaLower(int h, int n) {
        if (n <= 0) return 0;
        return BetaMath.quantile(0.025d, h + 1.0d, n - h + 1.0d);
    }

    private static String reason(
            boolean p,
            boolean artifact,
            int n,
            double cov,
            double ece,
            double block,
            int stable,
            double drift) {
        if (p) return "PROVEN_40_STRICT_OOS";
        if (!artifact) return "ARTIFACT_CHAIN_NOT_PROVEN";
        if (n < MIN_SAMPLE_40) return "SAMPLE_LT_120";
        if (cov < MIN_COVERAGE) return "COVERAGE_LT_15PCT";
        if (ece > MAX_ECE) return "ECE_GT_4PCT";
        if (block < 0.33d || stable < 3) return "CROSS_BLOCK_UNSTABLE";
        if (drift > MAX_DRIFT) return "DRIFT_TOO_HIGH";
        return "HIT_OR_LCB_NOT_PROVEN";
    }

    static final class BetaMath {
        static double quantile(double q, double a, double b) {
            double lo = 0, hi = 1;
            for (int i = 0; i < 70; i++) {
                double m = (lo + hi) * 0.5d;
                if (ibeta(m, a, b) < q) lo = m;
                else hi = m;
            }
            return (lo + hi) * 0.5d;
        }

        static double ibeta(double x, double a, double b) {
            if (x <= 0) return 0;
            if (x >= 1) return 1;
            double bt =
                    Math.exp(
                            logGamma(a + b)
                                    - logGamma(a)
                                    - logGamma(b)
                                    + a * Math.log(x)
                                    + b * Math.log(1 - x));
            return x < (a + 1) / (a + b + 2) ? bt * cf(a, b, x) / a : 1 - bt * cf(b, a, 1 - x) / b;
        }

        static double cf(double a, double b, double x) {
            final int MAX = 200;
            final double EPS = 3e-14, FPMIN = 1e-300;
            double qab = a + b, qap = a + 1, qam = a - 1, c = 1, d = 1 - qab * x / qap;
            if (Math.abs(d) < FPMIN) d = FPMIN;
            d = 1 / d;
            double h = d;
            for (int m = 1; m <= MAX; m++) {
                int m2 = 2 * m;
                double aa = m * (b - m) * x / ((qam + m2) * (a + m2));
                d = 1 + aa * d;
                if (Math.abs(d) < FPMIN) d = FPMIN;
                c = 1 + aa / c;
                if (Math.abs(c) < FPMIN) c = FPMIN;
                d = 1 / d;
                h *= d * c;
                aa = -(a + m) * (qab + m) * x / ((a + m2) * (qap + m2));
                d = 1 + aa * d;
                if (Math.abs(d) < FPMIN) d = FPMIN;
                c = 1 + aa / c;
                if (Math.abs(c) < FPMIN) c = FPMIN;
                d = 1 / d;
                double del = d * c;
                h *= del;
                if (Math.abs(del - 1) < EPS) break;
            }
            return h;
        }

        static double logGamma(double z) {
            double[] p = {
                676.5203681218851,
                -1259.1392167224028,
                771.3234287776531,
                -176.6150291621406,
                12.507343278686905,
                -0.13857109526572012,
                9.9843695780195716e-6,
                1.5056327351493116e-7
            };
            if (z < 0.5)
                return Math.log(Math.PI) - Math.log(Math.sin(Math.PI * z)) - logGamma(1 - z);
            z -= 1;
            double x = 0.99999999999980993;
            for (int i = 0; i < p.length; i++) x += p[i] / (z + i + 1);
            double t = z + p.length - 0.5;
            return 0.5 * Math.log(2 * Math.PI) + (z + 0.5) * Math.log(t) - t + Math.log(x);
        }
    }
}
