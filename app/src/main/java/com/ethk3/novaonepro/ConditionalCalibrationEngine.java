package com.ethk3.novaonepro;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.HashMap;
import java.util.Locale;

/**
 * V46 conditional calibration. EventP is the estimated event probability. ActionLCB is a separate
 * conservative action bound. Risk never overwrites EventP.
 */
public final class ConditionalCalibrationEngine {
    public static final String MODULE_VERSION =
            "V47_LEGACY_CONDITIONAL_CALIBRATION_DISABLED_20260721_47072147";

    public static final class Cell {
        public int sample;
        public int hit;

        public double rate() {
            return (hit + 3.0d) / (sample + 10.0d);
        }
    }

    public static final class Snapshot {
        public Cell global = new Cell();
        public Cell[] position = new Cell[10];
        public Cell[] rank = new Cell[13];
        public HashMap<String, Cell> regime = new HashMap<String, Cell>();
        public int rows;
        public String status = "EMPTY";

        Snapshot() {
            for (int i = 0; i < position.length; i++) position[i] = new Cell();
            for (int i = 0; i < rank.length; i++) rank[i] = new Cell();
        }
    }

    public Snapshot load(DbStore store) {
        Snapshot out = new Snapshot();
        if (store == null) return out;
        Cursor c = null;
        try {
            SQLiteDatabase db = store.readable();
            c =
                    db.query(
                            DbHelper.TABLE_OOS_ACTION_LOG,
                            new String[] {
                                "position", "rank_slot", "hit", "window_state_key", "version"
                            },
                            "settled=1 AND sample_source=? AND version=?",
                            new String[] {
                                OosActionLogEngine.SOURCE_LIVE_REALTIME,
                                AppConfig.OOS_ACTION_LOG_VERSION
                            },
                            null,
                            null,
                            "target_issue DESC",
                            "4000");
            while (c != null && c.moveToNext()) {
                int position = c.getInt(0), rank = c.getInt(1), hit = c.getInt(2) == 1 ? 1 : 0;
                String regime = safe(c.getString(3));
                out.global.sample++;
                out.global.hit += hit;
                out.rows++;
                if (position >= 1 && position <= 10) {
                    out.position[position - 1].sample++;
                    out.position[position - 1].hit += hit;
                }
                if (rank >= 1 && rank < out.rank.length) {
                    out.rank[rank].sample++;
                    out.rank[rank].hit += hit;
                }
                if (regime.length() > 0) {
                    String key = compact(regime, 72);
                    Cell x = out.regime.get(key);
                    if (x == null) {
                        x = new Cell();
                        out.regime.put(key, x);
                    }
                    x.sample++;
                    x.hit += hit;
                }
            }
            out.status = out.rows >= 30 ? "READY" : "WARM";
        } catch (Throwable e) {
            out.status = "ERROR:" + e.getClass().getSimpleName();
        } finally {
            if (c != null)
                try {
                    c.close();
                } catch (Throwable ignore) {
                }
        }
        return out;
    }

    public void apply(Snapshot snapshot, Models.ComboCandidate c, int rankHint) {
        if (c == null) return;
        Snapshot s = snapshot == null ? new Snapshot() : snapshot;
        double global =
                prob(
                        c.calibratedHitProbability,
                        prob(c.finalP, prob(c.modelP, AppConfig.RANDOM_K3_BASELINE)));
        double globalReference =
                s.global.sample > 0 ? s.global.rate() : AppConfig.RANDOM_K3_BASELINE;
        Cell pos = c.position >= 1 && c.position <= 10 ? s.position[c.position - 1] : new Cell();
        Cell rank = rankHint >= 1 && rankHint < s.rank.length ? s.rank[rankHint] : new Cell();
        Cell regime =
                s.regime.get(
                        compact(
                                first(c.probabilityWindowStateKey, c.windowStateKey, c.stateKey),
                                72));
        if (regime == null) regime = new Cell();
        double pa = weight(pos.sample) * (pos.rate() - globalReference);
        double ra = weight(rank.sample) * (rank.rate() - globalReference);
        double ga = weight(regime.sample) * (regime.rate() - globalReference);
        double adjustment =
                clamp(
                        0.45d * pa + 0.35d * ra + 0.20d * ga,
                        -AppConfig.V46_MAX_CONDITIONAL_ADJUSTMENT,
                        AppConfig.V46_MAX_CONDITIONAL_ADJUSTMENT);
        double eventP = clamp(global + adjustment, 0.05d, 0.75d);
        int effective =
                Math.max(1, Math.max(c.calibrationSample, Math.max(pos.sample, rank.sample)));
        double stderr =
                Math.sqrt(Math.max(0.000001d, eventP * (1.0d - eventP) / (effective + 1.0d)));
        double disagreement = Math.abs(eventP - prob(c.modelP, eventP));
        double confidence = clamp(1.0d - (2.4d * stderr + 1.5d * disagreement), 0.0d, 1.0d);
        double penalty =
                clamp(
                        0.45d * stderr + 0.30d * disagreement + 0.025d * (1.0d - confidence),
                        0.0d,
                        AppConfig.V46_MAX_ACTION_BOUND_PENALTY);
        double actionLcb = clamp(eventP - penalty, 0.001d, eventP);
        c.v46GlobalCalibratedProbability = global;
        c.v46PositionAdjustment = 0.45d * pa;
        c.v46RankAdjustment = 0.35d * ra;
        c.v46RegimeAdjustment = 0.20d * ga;
        c.v46EventProbability = eventP;
        c.v46ProbabilityUncertainty =
                clamp(penalty / AppConfig.V46_MAX_ACTION_BOUND_PENALTY, 0.0d, 1.0d);
        c.v46ActionLcb = actionLcb;
        c.v46ExpectedValue = AppConfig.ODDS * eventP - AppConfig.BET_COST;
        c.v46CalibrationConfidence = confidence;
        c.v46Version = AppConfig.V46_VERSION;
        c.finalP = eventP;
        c.calibratedP = eventP;
        c.calibratedHitProbability = eventP;
        c.honestProbability = eventP;
        c.v43SafeProbability = actionLcb; // legacy DB alias only; never authorization EventP
        c.v46ProbabilityTrace =
                append(
                        c.v46ProbabilityTrace,
                        "Global="
                                + pct(global)
                                + ";Pos="
                                + signed(c.v46PositionAdjustment)
                                + ";Rank="
                                + signed(c.v46RankAdjustment)
                                + ";Regime="
                                + signed(c.v46RegimeAdjustment)
                                + ";EventP="
                                + pct(eventP)
                                + ";ActionLCB="
                                + pct(actionLcb));
    }

    public static void refreshActionBound(
            Models.ComboCandidate c, StrategyEvidenceContract.Snapshot evidence) {
        if (c == null) return;
        double eventP = prob(c.v46EventProbability, prob(c.finalP, AppConfig.RANDOM_K3_BASELINE));
        double risk =
                evidence == null
                        ? clamp(c.evidenceRisk, 0.0d, 1.0d)
                        : clamp(evidence.risk, 0.0d, 1.0d);
        double frag =
                evidence == null
                        ? clamp(c.v43AblationFragility, 0.0d, 1.0d)
                        : clamp(evidence.fragility, 0.0d, 1.0d);
        double basePenalty = Math.max(0.0d, eventP - prob(c.v46ActionLcb, eventP));
        double evidencePenalty =
                AppConfig.V46_MAX_ACTION_BOUND_PENALTY
                        * clamp(
                                0.55d * risk
                                        + 0.25d * frag
                                        + 0.20d
                                                * (1.0d
                                                        - clamp(
                                                                c.v43StrategyConfidence,
                                                                0.0d,
                                                                1.0d)),
                                0.0d,
                                1.0d);
        double penalty = Math.max(basePenalty, evidencePenalty);
        c.v46ActionLcb = clamp(eventP - penalty, 0.001d, eventP);
        c.v43SafeProbability = c.v46ActionLcb;
        c.v46ExpectedValue = AppConfig.ODDS * eventP - AppConfig.BET_COST;
        c.v46ProbabilityTrace =
                append(c.v46ProbabilityTrace, "FinalRiskBound=" + pct(c.v46ActionLcb));
    }

    private static double weight(int n) {
        if (n < AppConfig.V46_CONDITIONAL_GLOBAL_MIN) return 0.0d;
        if (n < AppConfig.V46_CONDITIONAL_PARTIAL_MIN) return 0.20d;
        if (n < AppConfig.V46_CONDITIONAL_FULL_MIN) return 0.40d;
        return 0.65d;
    }

    private static double prob(double v, double d) {
        return valid(v) ? v : d;
    }

    private static boolean valid(double v) {
        return !Double.isNaN(v) && !Double.isInfinite(v) && v > 0.0d && v < 1.0d;
    }

    private static double clamp(double v, double lo, double hi) {
        if (Double.isNaN(v) || Double.isInfinite(v)) return lo;
        return v < lo ? lo : (v > hi ? hi : v);
    }

    private static String first(String a, String b, String c) {
        return safe(a).length() > 0 ? a : (safe(b).length() > 0 ? b : safe(c));
    }

    private static String compact(String s, int n) {
        s = safe(s);
        return s.length() <= n ? s : s.substring(0, n);
    }

    private static String append(String a, String b) {
        if (safe(a).length() == 0) return safe(b);
        if (safe(b).length() == 0 || a.indexOf(b) >= 0) return safe(a);
        return a + "|" + b;
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    private static String pct(double v) {
        return String.format(Locale.US, "%.2f%%", 100.0d * v);
    }

    private static String signed(double v) {
        return String.format(Locale.US, "%+.2f%%", 100.0d * v);
    }
}
