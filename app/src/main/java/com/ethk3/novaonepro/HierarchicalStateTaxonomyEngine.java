package com.ethk3.novaonepro;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * V21.3 finite hierarchical state taxonomy.
 *
 * <p>Coarse domains intentionally contain only dozens of reusable regimes. Mid domains add position
 * and quality bands. Fine domains add the exact fixed K3 mask and a short state fingerprint. The
 * three keys are frozen once after FinalP is complete and copied unchanged through
 * Decision/Trade/Settlement.
 */
public final class HierarchicalStateTaxonomyEngine {
    public static final String MODULE_VERSION =
            "V44_NOVA_DIAGNOSIS_FREQUENCY_STABILITY_20260720_44157296";

    public static final class Taxonomy {
        public String coarseKey;
        public String midKey;
        public String fineKey;
        public String regime;
        public String probabilityBand;
        public String positionGroup;
        public String qualityBand;
        public String summary;

        public Taxonomy() {
            coarseKey = "";
            midKey = "";
            fineKey = "";
            regime = "BALANCED";
            probabilityBand = "P30_34";
            positionGroup = "PG_MID";
            qualityBand = "Q_MID";
            summary = "";
        }
    }

    public ArrayList<Models.ComboCandidate> freezeAll(
            List<Models.ComboCandidate> input, List<Models.Draw> history, String targetIssue) {
        ArrayList<Models.ComboCandidate> out = new ArrayList<Models.ComboCandidate>();
        if (input == null) return out;
        for (int i = 0; i < input.size(); i++) {
            Models.ComboCandidate c = input.get(i);
            if (c == null) continue;
            freezeCandidate(c, history, targetIssue);
            out.add(c);
        }
        return out;
    }

    public Taxonomy freezeCandidate(
            Models.ComboCandidate c, List<Models.Draw> history, String targetIssue) {
        Taxonomy t = buildForCandidate(c, history);
        if (c == null) return t;
        String target = safe(targetIssue);
        if (target.length() == 0) target = safe(c.targetIssue);

        /* Never regenerate a domain for the same already frozen target. */
        if (c.evidenceDomainFrozen == 1
                && target.equals(safe(c.evidenceDomainFrozenIssue))
                && safe(c.promotionCoarseDomain).length() > 0
                && safe(c.promotionMidDomain).length() > 0
                && safe(c.promotionFineDomain).length() > 0) {
            t.coarseKey = safe(c.promotionCoarseDomain);
            t.midKey = safe(c.promotionMidDomain);
            t.fineKey = safe(c.promotionFineDomain);
            t.summary = "REUSE_FROZEN_DOMAIN|" + t.coarseKey + "|" + t.midKey + "|" + t.fineKey;
            return t;
        }

        c.promotionCoarseDomain = AppConfig.sanitizePromotionDomainKey(t.coarseKey);
        c.promotionMidDomain = AppConfig.sanitizePromotionDomainKey(t.midKey);
        c.promotionFineDomain = AppConfig.sanitizePromotionDomainKey(t.fineKey);
        c.shadowPromotionDomain = c.promotionFineDomain;
        c.dynamicWindowStateKey = c.promotionMidDomain;
        c.v20WindowStateKey = c.promotionMidDomain;
        c.finalDecisionWindowStateKey = c.promotionMidDomain;
        c.promotionDomainAvailable = c.promotionFineDomain.length() > 0 ? 1 : 0;
        c.evidenceDomainFrozen = c.promotionDomainAvailable;
        c.evidenceDomainFrozenIssue = target;
        c.evidenceDomainFrozenAt = System.currentTimeMillis();
        c.preDecisionDomainVersion = AppConfig.EVIDENCE_DOMAIN_SCHEMA_VERSION;
        c.moduleBuildVersion = MODULE_VERSION;
        return t;
    }

    public Taxonomy buildForCandidate(Models.ComboCandidate c, List<Models.Draw> history) {
        Taxonomy t = new Taxonomy();
        if (c == null || !c.isValidK3()) {
            t.coarseKey = "H3|INVALID";
            t.midKey = "H3|INVALID";
            t.fineKey = "H3|INVALID";
            t.summary = "invalid candidate";
            return t;
        }

        double p = firstPositive(c.finalP, c.calibratedP, c.rawP, c.modelP);
        t.probabilityBand = probabilityBand(p);
        t.positionGroup = positionGroup(c.position);
        t.regime = detectRegime(c, history);
        t.qualityBand = qualityBand(c);

        String trend = trendFamily(c);
        String coverage =
                band(
                        c.coverageQuality > 0.0d ? c.coverageQuality : c.coverageQualityScore,
                        0.29d,
                        0.34d,
                        0.39d,
                        "CV");
        String third =
                band(
                        c.thirdQuality > 0.0d ? c.thirdQuality : c.thirdNumberQualityScore,
                        0.085d,
                        0.100d,
                        0.120d,
                        "T3");
        String risk =
                band(
                        c.counterRisk > 0.0d ? c.counterRisk : c.counterEvidenceScore,
                        0.30d,
                        0.45d,
                        0.58d,
                        "RK");

        /* Around 3 * 4 * 5 = 60 major states. */
        t.coarseKey = sanitize("H3C|" + t.positionGroup + "|" + t.probabilityBand + "|" + t.regime);

        /* Mid is still broad: exact position + trend and three quality bands, no exact K3. */
        t.midKey =
                sanitize(
                        t.coarseKey
                                + "|P"
                                + c.position
                                + "|"
                                + trend
                                + "|"
                                + coverage
                                + "|"
                                + third
                                + "|"
                                + risk);

        String fingerprint = compactFingerprint(c);
        t.fineKey = sanitize(t.midKey + "|K" + c.k3Mask + "|" + fingerprint);
        t.summary =
                "层级域冻结：COARSE="
                        + t.coarseKey
                        + "｜MID="
                        + t.midKey
                        + "｜FINE="
                        + t.fineKey
                        + "｜大类="
                        + t.regime
                        + "｜概率段="
                        + t.probabilityBand
                        + "｜质量="
                        + t.qualityBand;
        return t;
    }

    /** Shared state classifier used by the historical point-in-time prior. */
    public static String buildHistoryCoarseKey(
            List<Models.Draw> history, int endExclusive, int position) {
        String pg = positionGroup(position);
        HistoryState s = historyState(history, endExclusive, position);
        return sanitize("H3HC|" + pg + "|" + s.regime + "|" + s.balanceBand);
    }

    public static String buildHistoryMidKey(
            List<Models.Draw> history, int endExclusive, int position) {
        HistoryState s = historyState(history, endExclusive, position);
        return sanitize(
                buildHistoryCoarseKey(history, endExclusive, position)
                        + "|P"
                        + position
                        + "|L"
                        + s.lastBand
                        + "|SL"
                        + s.slopeBand
                        + "|RP"
                        + s.repeatBand);
    }

    public static int lastNumber(List<Models.Draw> history, int endExclusive, int position) {
        if (history == null) return 0;
        int end = Math.min(endExclusive, history.size());
        for (int i = end - 1; i >= 0; i--) {
            Models.Draw d = history.get(i);
            if (d == null || !d.isValid()) continue;
            int n = d.getNumberAtPosition(position);
            if (AppConfig.isValidNumber(n)) return n;
        }
        return 0;
    }

    private static final class HistoryState {
        String regime = "BALANCED";
        String balanceBand = "BAL";
        String lastBand = "MID";
        String slopeBand = "FLAT";
        String repeatBand = "LOW";
    }

    private static HistoryState historyState(
            List<Models.Draw> history, int endExclusive, int position) {
        HistoryState s = new HistoryState();
        if (history == null || history.size() == 0 || !AppConfig.isValidPosition(position))
            return s;
        int end = Math.min(endExclusive, history.size());
        int start = Math.max(0, end - 24);
        int valid = 0;
        int big = 0;
        int odd = 0;
        int repeats = 0;
        double firstAvg = 0.0d;
        double secondAvg = 0.0d;
        int firstCount = 0;
        int secondCount = 0;
        int previous = 0;
        int last = 0;
        int[] counts = new int[11];
        for (int i = start; i < end; i++) {
            Models.Draw d = history.get(i);
            if (d == null || !d.isValid()) continue;
            int n = d.getNumberAtPosition(position);
            if (!AppConfig.isValidNumber(n)) continue;
            valid++;
            last = n;
            counts[n]++;
            if (n >= 6) big++;
            if ((n & 1) == 1) odd++;
            if (previous == n) repeats++;
            previous = n;
            if (i < start + 12) {
                firstAvg += n;
                firstCount++;
            } else {
                secondAvg += n;
                secondCount++;
            }
        }
        if (valid <= 0) return s;
        double bigRate = ((double) big) / valid;
        double oddRate = ((double) odd) / valid;
        double repeatRate = ((double) repeats) / Math.max(1, valid - 1);
        double avg1 = firstCount <= 0 ? 5.5d : firstAvg / firstCount;
        double avg2 = secondCount <= 0 ? avg1 : secondAvg / secondCount;
        double slope = avg2 - avg1;
        int max = 0;
        int minPositive = Integer.MAX_VALUE;
        for (int n = 1; n <= 10; n++) {
            if (counts[n] > max) max = counts[n];
            if (counts[n] > 0 && counts[n] < minPositive) minPositive = counts[n];
        }
        double concentration = ((double) max) / valid;
        if (slope >= 1.2d) s.slopeBand = "UP";
        else if (slope <= -1.2d) s.slopeBand = "DOWN";
        else s.slopeBand = "FLAT";
        if (repeatRate >= 0.18d) s.repeatBand = "HIGH";
        else if (repeatRate >= 0.08d) s.repeatBand = "MID";
        else s.repeatBand = "LOW";
        if (last <= 3) s.lastBand = "LOW";
        else if (last >= 8) s.lastBand = "HIGH";
        else s.lastBand = "MID";
        if (concentration >= 0.25d) s.regime = "HOT_CLUSTER";
        else if (slope >= 1.2d) s.regime = "RISING";
        else if (slope <= -1.2d) s.regime = "FALLING";
        else if (repeatRate >= 0.18d) s.regime = "REPEAT";
        else s.regime = "BALANCED";
        if (bigRate >= 0.62d) s.balanceBand = "BIG";
        else if (bigRate <= 0.38d) s.balanceBand = "SMALL";
        else if (oddRate >= 0.62d) s.balanceBand = "ODD";
        else if (oddRate <= 0.38d) s.balanceBand = "EVEN";
        else s.balanceBand = "BAL";
        return s;
    }

    private static String detectRegime(Models.ComboCandidate c, List<Models.Draw> history) {
        String text =
                (safe(c.stateMode)
                                + "|"
                                + safe(c.shapeKey)
                                + "|"
                                + safe(c.stateKey)
                                + "|"
                                + safe(c.dynamicStatus)
                                + "|"
                                + safe(c.v20Status))
                        .toUpperCase(Locale.US);
        if (text.indexOf("RECOVER") >= 0 || text.indexOf("REPAIR") >= 0) return "RECOVERY";
        if (text.indexOf("COLD") >= 0) return "COLD";
        if (text.indexOf("HOT") >= 0 || text.indexOf("OVERHEAT") >= 0) return "HOT";
        if (text.indexOf("CHAOS") >= 0 || text.indexOf("DRIFT") >= 0 || c.driftRisk >= 0.60d)
            return "CHAOS";
        if (history != null && history.size() > 0) {
            HistoryState hs = historyState(history, history.size(), c.position);
            return hs.regime;
        }
        return "BALANCED";
    }

    private static String trendFamily(Models.ComboCandidate c) {
        String text =
                (safe(c.stateMode)
                                + "|"
                                + safe(c.shapeKey)
                                + "|"
                                + safe(c.stateKey)
                                + "|"
                                + safe(c.dynamicStatus))
                        .toUpperCase(Locale.US);
        if (text.indexOf("RECOVER") >= 0 || text.indexOf("REPAIR") >= 0) return "TR_REC";
        if (text.indexOf("UP") >= 0 || text.indexOf("RISE") >= 0) return "TR_UP";
        if (text.indexOf("DOWN") >= 0 || text.indexOf("FALL") >= 0) return "TR_DOWN";
        return "TR_FLAT";
    }

    private static String qualityBand(Models.ComboCandidate c) {
        double cover = c.coverageQuality > 0.0d ? c.coverageQuality : c.coverageQualityScore;
        double third = c.thirdQuality > 0.0d ? c.thirdQuality : c.thirdNumberQualityScore;
        if (cover >= 0.39d && third >= 0.12d) return "Q_HIGH";
        if (cover >= 0.33d && third >= 0.10d) return "Q_OK";
        return "Q_LOW";
    }

    private static String compactFingerprint(Models.ComboCandidate c) {
        String source = safe(c.windowStateKey);
        if (source.length() == 0) source = safe(c.stateKey);
        if (source.length() == 0) source = safe(c.shapeKey);
        int h = source.hashCode();
        if (h == Integer.MIN_VALUE) h = 0;
        h = Math.abs(h);
        return "F" + (h % 100000);
    }

    private static String probabilityBand(double p) {
        if (p < 0.30d) return "P_LT30";
        if (p < 0.34d) return "P30_34";
        if (p < 0.36d) return "P34_36";
        if (p < 0.38d) return "P36_38";
        return "P38_PLUS";
    }

    private static String positionGroup(int p) {
        if (p <= 3) return "PG_FRONT";
        if (p <= 7) return "PG_MID";
        return "PG_BACK";
    }

    private static String band(double value, double a, double b, double c, String prefix) {
        if (value < a) return prefix + "0";
        if (value < b) return prefix + "1";
        if (value < c) return prefix + "2";
        return prefix + "3";
    }

    private static double firstPositive(double a, double b, double c, double d) {
        if (validProbability(a)) return a;
        if (validProbability(b)) return b;
        if (validProbability(c)) return c;
        if (validProbability(d)) return d;
        return AppConfig.RANDOM_K3_BASELINE;
    }

    private static boolean validProbability(double p) {
        return !Double.isNaN(p) && !Double.isInfinite(p) && p > 0.0d && p < 1.0d;
    }

    private static String sanitize(String value) {
        String s = safe(value).toUpperCase(Locale.US).replaceAll("[^A-Z0-9_|.-]", "_");
        while (s.indexOf("__") >= 0) s = s.replace("__", "_");
        if (s.length() > 220) s = s.substring(0, 220);
        return s;
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
