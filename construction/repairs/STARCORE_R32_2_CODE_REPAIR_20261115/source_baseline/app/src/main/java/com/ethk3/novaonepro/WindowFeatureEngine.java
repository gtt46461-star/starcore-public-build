package com.ethk3.novaonepro;

import java.util.ArrayList;
import java.util.List;

public final class WindowFeatureEngine {

    private final DataGuard dataGuard;

    public WindowFeatureEngine() {
        dataGuard = new DataGuard();
    }

    public List<Models.ShapeVector> buildAllPositions(List<Models.Draw> history) {
        ArrayList<Models.ShapeVector> out = new ArrayList<Models.ShapeVector>();
        ArrayList<Models.Draw> clean = dataGuard.normalizeHistory(history);

        if (clean.size() == 0) {
            return out;
        }

        for (int position = AppConfig.MIN_POSITION;
                position <= AppConfig.MAX_POSITION;
                position++) {
            Models.ShapeVector v = buildForPosition(clean, position);
            if (v != null) {
                out.add(v);
            }
        }

        return out;
    }

    public Models.ShapeVector buildForPosition(List<Models.Draw> history, int position) {
        if (!AppConfig.isValidPosition(position)) {
            return null;
        }

        ArrayList<Models.Draw> clean = dataGuard.normalizeHistory(history);

        if (clean.size() == 0) {
            return buildEmptyVector(position);
        }

        Models.Draw latest = clean.get(clean.size() - 1);

        Models.ShapeVector v = new Models.ShapeVector();
        v.position = position;
        v.baseIssue = latest.issue == null ? "" : latest.issue;
        v.targetIssue = dataGuard.buildNextIssue(v.baseIssue);
        v.createdAt = System.currentTimeMillis();

        double[] p16 = ShapeMath.probabilityAtPosition(clean, position, AppConfig.WINDOW_16);
        double[] p32 = ShapeMath.probabilityAtPosition(clean, position, AppConfig.WINDOW_32);
        double[] p64 = ShapeMath.probabilityAtPosition(clean, position, AppConfig.WINDOW_64);
        double[] p128 = ShapeMath.probabilityAtPosition(clean, position, AppConfig.WINDOW_128);
        double[] p240 = ShapeMath.probabilityAtPosition(clean, position, AppConfig.WINDOW_240);
        double[] p480 = ShapeMath.probabilityAtPosition(clean, position, AppConfig.WINDOW_480);
        double[] p960 = ShapeMath.probabilityAtPosition(clean, position, AppConfig.WINDOW_960);

        v.entropy16 = ShapeMath.entropyNormalized(p16);
        v.entropy32 = ShapeMath.entropyNormalized(p32);
        v.entropy64 = ShapeMath.entropyNormalized(p64);
        v.entropy128 = ShapeMath.entropyNormalized(p128);
        v.entropy240 = ShapeMath.entropyNormalized(p240);
        v.entropy480 = ShapeMath.entropyNormalized(p480);
        v.entropy960 = ShapeMath.entropyNormalized(p960);

        v.top3Mass16 = ShapeMath.topMass(p16, 3);
        v.top3Mass32 = ShapeMath.topMass(p32, 3);
        v.top3Mass64 = ShapeMath.topMass(p64, 3);
        v.top3Mass128 = ShapeMath.topMass(p128, 3);
        v.top3Mass240 = ShapeMath.topMass(p240, 3);
        v.top4Mass128 = ShapeMath.topMass(p128, 4);
        v.top6Mass240 = ShapeMath.topMass(p240, 6);

        v.slopeShort = v.top3Mass16 - v.top3Mass64;
        v.slopeMid = v.top3Mass64 - v.top3Mass240;
        v.slopeLong = v.top3Mass240 - ShapeMath.topMass(p960, 3);

        v.driftShort =
                ShapeMath.driftRisk(clean, position, AppConfig.WINDOW_16, AppConfig.WINDOW_64);
        v.driftMid =
                ShapeMath.driftRisk(clean, position, AppConfig.WINDOW_64, AppConfig.WINDOW_240);
        v.driftLong =
                ShapeMath.driftRisk(clean, position, AppConfig.WINDOW_240, AppConfig.WINDOW_960);
        v.driftRisk = combineDriftRisk(v.driftShort, v.driftMid, v.driftLong);

        v.markov1Support = estimateTransitionSupport(clean, position, 1);
        v.markov2Support = estimateTransitionSupport(clean, position, 2);

        v.championGap = ShapeMath.championGap(p64);

        double top3Drop = v.top3Mass240 - v.top3Mass16;
        if (top3Drop < 0.0d) {
            top3Drop = 0.0d;
        }

        v.trendLife =
                ShapeMath.trendLifeScore(
                        v.entropy64, v.top3Mass64, v.slopeShort, v.slopeMid, v.championGap);

        v.breakDepth =
                ShapeMath.breakDepthScore(
                        v.slopeShort, v.slopeMid, v.driftRisk, v.entropy16, top3Drop);

        v.recoveryScore =
                ShapeMath.recoveryScore(
                        v.slopeShort,
                        v.slopeMid,
                        v.driftRisk,
                        v.breakDepth,
                        v.top3Mass16,
                        v.championGap);

        double[] blended = blendWindowProbabilities(clean, position);
        copyDistribution(blended, v.numberProbs);

        double[] markovSeed = buildSoftMarkovSeed(clean, position, blended);
        copyDistribution(markovSeed, v.markovProbs);

        int[] top3 = ShapeMath.topNumbers(blended, AppConfig.FIXED_K);
        int top3Mask = 0;
        if (top3 != null && top3.length >= AppConfig.FIXED_K) {
            top3Mask = Models.buildK3Mask(top3[0], top3[1], top3[2]);
        }

        if (Models.isValidK3Mask(top3Mask)) {
            v.comboGap = ShapeMath.comboGap(blended, top3Mask);
        } else {
            v.comboGap = ShapeMath.top3ThirdGap(blended);
        }

        v.hotReverseRisk = ShapeMath.hotReverseRisk(p16, p64, p240);
        v.falseStrongRisk = ShapeMath.falseStrongRisk(p16, p128, clean.size());

        v.stateMode = inferRoughMode(v);
        v.shapeKey = ShapeMath.buildShapeKey(v);

        return v;
    }

    public double[] blendWindowProbabilities(List<Models.Draw> history, int position) {
        ArrayList<Models.Draw> clean = dataGuard.normalizeHistory(history);
        double[] out = new double[AppConfig.NUMBER_COUNT + 1];

        if (clean.size() == 0 || !AppConfig.isValidPosition(position)) {
            ShapeMath.fillUniform(out);
            return out;
        }

        double[] p16 = ShapeMath.probabilityAtPosition(clean, position, AppConfig.WINDOW_16);
        double[] p32 = ShapeMath.probabilityAtPosition(clean, position, AppConfig.WINDOW_32);
        double[] p64 = ShapeMath.probabilityAtPosition(clean, position, AppConfig.WINDOW_64);
        double[] p128 = ShapeMath.probabilityAtPosition(clean, position, AppConfig.WINDOW_128);
        double[] p240 = ShapeMath.probabilityAtPosition(clean, position, AppConfig.WINDOW_240);
        double[] p480 = ShapeMath.probabilityAtPosition(clean, position, AppConfig.WINDOW_480);
        double[] p960 = ShapeMath.probabilityAtPosition(clean, position, AppConfig.WINDOW_960);

        double w16 = 0.11d;
        double w32 = 0.13d;
        double w64 = 0.18d;
        double w128 = 0.20d;
        double w240 = 0.18d;
        double w480 = 0.12d;
        double w960 = 0.08d;

        int size = clean.size();

        if (size < AppConfig.WINDOW_240) {
            w16 = 0.18d;
            w32 = 0.22d;
            w64 = 0.25d;
            w128 = 0.22d;
            w240 = 0.13d;
            w480 = 0.0d;
            w960 = 0.0d;
        } else if (size < AppConfig.WINDOW_960) {
            w16 = 0.13d;
            w32 = 0.16d;
            w64 = 0.22d;
            w128 = 0.22d;
            w240 = 0.18d;
            w480 = 0.09d;
            w960 = 0.0d;
        }

        for (int n = AppConfig.MIN_NUMBER; n <= AppConfig.MAX_NUMBER; n++) {
            out[n] =
                    p16[n] * w16
                            + p32[n] * w32
                            + p64[n] * w64
                            + p128[n] * w128
                            + p240[n] * w240
                            + p480[n] * w480
                            + p960[n] * w960;
        }

        ShapeMath.normalizeDistribution(out);
        return out;
    }

    public double positionRawStrength(Models.ShapeVector v) {
        if (v == null) {
            return 0.0d;
        }

        double score = ShapeMath.positionStrength(v);

        if (AppConfig.MODE_CONFIRMED.equals(v.stateMode)) {
            score += 4.0d;
        } else if (AppConfig.MODE_ACCELERATING.equals(v.stateMode)) {
            score += 6.0d;
        } else if (AppConfig.MODE_RECOVERING.equals(v.stateMode)) {
            score += 3.0d;
        } else if (AppConfig.MODE_BROKEN.equals(v.stateMode)) {
            score -= 12.0d;
        } else if (AppConfig.MODE_CHAOS.equals(v.stateMode)) {
            score -= 16.0d;
        }

        return ShapeMath.clamp100(score);
    }

    public ShapeMath.PositionScore bestPosition(List<Models.ShapeVector> vectors) {
        ArrayList<ShapeMath.PositionScore> ranked = rankPositions(vectors);
        if (ranked.size() == 0) {
            return null;
        }
        return ranked.get(0);
    }

    public ArrayList<ShapeMath.PositionScore> rankPositions(List<Models.ShapeVector> vectors) {
        ArrayList<ShapeMath.PositionScore> out = new ArrayList<ShapeMath.PositionScore>();

        if (vectors == null) {
            return out;
        }

        for (int i = 0; i < vectors.size(); i++) {
            Models.ShapeVector v = vectors.get(i);
            if (v == null || !AppConfig.isValidPosition(v.position)) {
                continue;
            }

            out.add(new ShapeMath.PositionScore(v.position, positionRawStrength(v), v));
        }

        for (int i = 0; i < out.size() - 1; i++) {
            for (int j = i + 1; j < out.size(); j++) {
                ShapeMath.PositionScore a = out.get(i);
                ShapeMath.PositionScore b = out.get(j);

                if (b.score > a.score || (b.score == a.score && b.position < a.position)) {
                    out.set(i, b);
                    out.set(j, a);
                }
            }
        }

        return out;
    }

    public double positionGap(List<Models.ShapeVector> vectors) {
        ArrayList<ShapeMath.PositionScore> ranked = rankPositions(vectors);

        if (ranked.size() < 2) {
            return 0.0d;
        }

        return ranked.get(0).score - ranked.get(1).score;
    }

    public boolean hasStrongSinglePosition(List<Models.ShapeVector> vectors) {
        ArrayList<ShapeMath.PositionScore> ranked = rankPositions(vectors);

        if (ranked.size() == 0) {
            return false;
        }

        ShapeMath.PositionScore best = ranked.get(0);
        double gap = ranked.size() >= 2 ? best.score - ranked.get(1).score : best.score;

        return best.score >= 68.0d && gap >= 3.0d;
    }

    public String explainPositionRank(List<Models.ShapeVector> vectors) {
        ArrayList<ShapeMath.PositionScore> ranked = rankPositions(vectors);

        if (ranked.size() == 0) {
            return "位置特征为空";
        }

        StringBuilder sb = new StringBuilder();

        int limit = Math.min(3, ranked.size());

        for (int i = 0; i < limit; i++) {
            ShapeMath.PositionScore ps = ranked.get(i);

            if (i > 0) {
                sb.append(" | ");
            }

            sb.append("P");
            sb.append(ps.position);
            sb.append("=");
            sb.append(UiFormatter.score(ps.score));
        }

        if (ranked.size() >= 2) {
            sb.append(" | Gap=");
            sb.append(UiFormatter.score(ranked.get(0).score - ranked.get(1).score));
        }

        return sb.toString();
    }

    private Models.ShapeVector buildEmptyVector(int position) {
        Models.ShapeVector v = new Models.ShapeVector();
        v.position = position;
        v.createdAt = System.currentTimeMillis();
        v.stateMode = AppConfig.MODE_CHAOS;
        v.shapeKey = ShapeMath.buildShapeKey(v);
        ShapeMath.fillUniform(v.numberProbs);
        ShapeMath.fillUniform(v.markovProbs);
        v.entropy16 = 100.0d;
        v.entropy32 = 100.0d;
        v.entropy64 = 100.0d;
        v.entropy128 = 100.0d;
        v.entropy240 = 100.0d;
        v.entropy480 = 100.0d;
        v.entropy960 = 100.0d;
        v.driftRisk = 100.0d;
        v.breakDepth = 100.0d;
        v.hotReverseRisk = 100.0d;
        v.falseStrongRisk = 100.0d;
        return v;
    }

    private double combineDriftRisk(double shortRisk, double midRisk, double longRisk) {
        double score = shortRisk * 0.45d + midRisk * 0.35d + longRisk * 0.20d;
        return ShapeMath.clamp100(score);
    }

    private double estimateTransitionSupport(List<Models.Draw> history, int position, int order) {
        if (history == null || history.size() < 5 || !AppConfig.isValidPosition(position)) {
            return 0.0d;
        }

        int lastIndex = history.size() - 1;
        int last = history.get(lastIndex).getNumberAtPosition(position);

        if (!AppConfig.isValidNumber(last)) {
            return 0.0d;
        }

        int sample = 0;
        int repeatDirection = 0;

        if (order <= 1) {
            for (int i = 1; i < history.size() - 1; i++) {
                int prev = history.get(i - 1).getNumberAtPosition(position);
                int cur = history.get(i).getNumberAtPosition(position);
                int next = history.get(i + 1).getNumberAtPosition(position);

                if (!AppConfig.isValidNumber(prev)
                        || !AppConfig.isValidNumber(cur)
                        || !AppConfig.isValidNumber(next)) {
                    continue;
                }

                if (cur == last) {
                    sample++;
                    if (Math.abs(next - cur) <= 3) {
                        repeatDirection++;
                    }
                }
            }
        } else {
            if (history.size() < 8) {
                return 0.0d;
            }

            int lastPrev = history.get(lastIndex - 1).getNumberAtPosition(position);

            for (int i = 2; i < history.size() - 1; i++) {
                int p1 = history.get(i - 1).getNumberAtPosition(position);
                int p2 = history.get(i).getNumberAtPosition(position);
                int next = history.get(i + 1).getNumberAtPosition(position);

                if (!AppConfig.isValidNumber(p1)
                        || !AppConfig.isValidNumber(p2)
                        || !AppConfig.isValidNumber(next)
                        || !AppConfig.isValidNumber(lastPrev)) {
                    continue;
                }

                if (p1 == lastPrev && p2 == last) {
                    sample++;
                    if (Math.abs(next - p2) <= 3) {
                        repeatDirection++;
                    }
                }
            }
        }

        if (sample <= 0) {
            return 0.0d;
        }

        double support = ((double) repeatDirection) / sample;
        return ShapeMath.clamp01(support);
    }

    private double[] buildSoftMarkovSeed(
            List<Models.Draw> history, int position, double[] blended) {
        double[] out = new double[AppConfig.NUMBER_COUNT + 1];

        if (history == null || history.size() < 5 || !AppConfig.isValidPosition(position)) {
            copyDistribution(blended, out);
            return out;
        }

        int lastIndex = history.size() - 1;
        int lastNumber = history.get(lastIndex).getNumberAtPosition(position);

        if (!AppConfig.isValidNumber(lastNumber)) {
            copyDistribution(blended, out);
            return out;
        }

        int[] counts = new int[AppConfig.NUMBER_COUNT + 1];
        int sample = 0;

        for (int i = 1; i < history.size(); i++) {
            int prev = history.get(i - 1).getNumberAtPosition(position);
            int cur = history.get(i).getNumberAtPosition(position);

            if (prev == lastNumber && AppConfig.isValidNumber(cur)) {
                counts[cur]++;
                sample++;
            }
        }

        if (sample < 3) {
            copyDistribution(blended, out);
            return out;
        }

        double[] markov = ShapeMath.probabilityFromCounts(counts, 1.0d);

        for (int n = AppConfig.MIN_NUMBER; n <= AppConfig.MAX_NUMBER; n++) {
            out[n] = markov[n] * 0.55d + blended[n] * 0.45d;
        }

        ShapeMath.normalizeDistribution(out);
        return out;
    }

    private String inferRoughMode(Models.ShapeVector v) {
        if (v == null) {
            return AppConfig.MODE_CHAOS;
        }

        if (v.driftRisk >= AppConfig.DRIFT_RISK_HARD_BLOCK || v.entropy16 >= 92.0d) {
            return AppConfig.MODE_CHAOS;
        }

        if (v.breakDepth >= 58.0d) {
            return AppConfig.MODE_BROKEN;
        }

        if (v.hotReverseRisk >= 55.0d) {
            return AppConfig.MODE_REVERSING;
        }

        if (v.trendLife >= 84.0d
                && v.recoveryScore >= 72.0d
                && v.driftRisk <= 30.0d
                && v.slopeShort > 0.010d) {
            return AppConfig.MODE_ACCELERATING;
        }

        if (v.trendLife >= 80.0d && v.driftRisk <= 35.0d && v.breakDepth <= 38.0d) {
            return AppConfig.MODE_CONFIRMED;
        }

        if (v.breakDepth > 35.0d && v.recoveryScore >= 68.0d && v.slopeShort > 0.0d) {
            return AppConfig.MODE_RECOVERING;
        }

        if (v.slopeShort < -0.012d && v.slopeMid < 0.0d) {
            return AppConfig.MODE_WEAKENING;
        }

        if (v.slopeShort < 0.0d && v.recoveryScore < 65.0d) {
            return AppConfig.MODE_PULLBACK;
        }

        return AppConfig.MODE_FORMING;
    }

    private void copyDistribution(double[] from, double[] to) {
        if (to == null) {
            return;
        }

        if (from == null || from.length < AppConfig.NUMBER_COUNT + 1) {
            ShapeMath.fillUniform(to);
            return;
        }

        for (int n = AppConfig.MIN_NUMBER; n <= AppConfig.MAX_NUMBER && n < to.length; n++) {
            to[n] = from[n];
        }

        ShapeMath.normalizeDistribution(to);
    }

    /* STARCORE_TIANSHU_B05_WINDOW_EVIDENCE_ONLY */

    public static final String B05_WINDOW_VERSION = "STARCORE_TIANSHU_B05_WINDOW_EVIDENCE_V1";

    /**
     * Builds evidence-only window packets from the B04 bounded cache. This method cannot mutate
     * candidate probability, gate, lane or BET.
     */
    public B05WindowEvidenceBundle buildB05Evidence(
            V51Models.DrawFrame[] history, V51Models.StateTrajectorySnapshot[] states) {
        long started = System.currentTimeMillis();
        RollingWindowCache.SnapshotSet windows = RollingWindowCache.shared().getOrBuild(history);
        RollingWindowCache.Validation validation = windows.validate();
        if (!validation.ok)
            return B05WindowEvidenceBundle.error("ROLLING_WINDOW_INVALID:" + validation.detail);
        B05WindowEvidencePacket[] packets = new B05WindowEvidencePacket[AppConfig.POSITION_COUNT];
        double meanSupport = 0.0d;
        double meanCounter = 0.0d;
        double meanRisk = 0.0d;
        for (int p = 0; p < packets.length; p++) {
            RollingWindowCache.WindowSnapshot shortWindow = windows.nearest(24);
            RollingWindowCache.WindowSnapshot mediumWindow = windows.nearest(96);
            RollingWindowCache.WindowSnapshot longWindow = windows.nearest(720);
            double shortConcentration = arrayValue(shortWindow.positionConcentration, p, 0.0d);
            double mediumConcentration = arrayValue(mediumWindow.positionConcentration, p, 0.0d);
            double longConcentration = arrayValue(longWindow.positionConcentration, p, 0.0d);
            double shortEntropy = arrayValue(shortWindow.positionEntropy, p, 1.0d);
            double mediumEntropy = arrayValue(mediumWindow.positionEntropy, p, 1.0d);
            double longEntropy = arrayValue(longWindow.positionEntropy, p, 1.0d);
            double concentrationSlope = shortConcentration - longConcentration;
            double entropySlope = longEntropy - shortEntropy;
            double stateConfidence = stateValue(states, p, 0);
            double stateRisk = stateValue(states, p, 1);
            double support =
                    V51Math.clamp(
                            0.28d * shortConcentration
                                    + 0.20d * mediumConcentration
                                    + 0.12d * longConcentration
                                    + 0.15d * Math.max(0.0d, concentrationSlope)
                                    + 0.10d * Math.max(0.0d, entropySlope)
                                    + 0.15d * stateConfidence,
                            0.0d,
                            1.0d);
            double counter =
                    V51Math.clamp(
                            0.34d * shortEntropy
                                    + 0.18d * Math.max(0.0d, -concentrationSlope)
                                    + 0.18d * Math.max(0.0d, -entropySlope)
                                    + 0.20d * stateRisk
                                    + 0.10d * (1.0d - shortWindow.density),
                            0.0d,
                            1.0d);
            double risk =
                    V51Math.clamp(
                            0.55d * counter
                                    + 0.25d * stateRisk
                                    + 0.20d * Math.abs(shortConcentration - mediumConcentration),
                            0.0d,
                            1.0d);
            int sample =
                    Math.min(
                            shortWindow.observedRows,
                            Math.min(mediumWindow.observedRows, longWindow.observedRows));
            double freshness =
                    V51Math.clamp(windows.lastIssue.length() == 0 ? 0.0d : 1.0d, 0.0d, 1.0d);
            double confidence =
                    V51Math.clamp(
                            0.40d * Math.min(1.0d, ((double) sample) / 96.0d)
                                    + 0.25d * shortWindow.density
                                    + 0.20d * stateConfidence
                                    + 0.15d * freshness,
                            0.0d,
                            1.0d);
            String checksum =
                    V51ChecksumEngine.sha256(
                            B05_WINDOW_VERSION
                                    + "|"
                                    + (p + 1)
                                    + "|"
                                    + windows.checksum
                                    + "|"
                                    + support
                                    + "|"
                                    + counter
                                    + "|"
                                    + risk
                                    + "|"
                                    + confidence
                                    + "|"
                                    + sample
                                    + "|"
                                    + freshness);
            packets[p] =
                    new B05WindowEvidencePacket(
                            p + 1,
                            support,
                            counter,
                            risk,
                            confidence,
                            sample,
                            freshness,
                            concentrationSlope,
                            entropySlope,
                            shortConcentration,
                            mediumConcentration,
                            longConcentration,
                            shortEntropy,
                            mediumEntropy,
                            longEntropy,
                            checksum);
            meanSupport += support;
            meanCounter += counter;
            meanRisk += risk;
        }
        meanSupport /= packets.length;
        meanCounter /= packets.length;
        meanRisk /= packets.length;
        String checksum =
                B05WindowEvidenceBundle.hash(
                        windows.checksum, packets, meanSupport, meanCounter, meanRisk);
        return new B05WindowEvidenceBundle(
                true,
                packets,
                windows.historyRows,
                windows.firstIssue,
                windows.lastIssue,
                meanSupport,
                meanCounter,
                meanRisk,
                Math.max(0L, System.currentTimeMillis() - started),
                checksum,
                "");
    }

    public static List<B05WindowContractRule> b05WindowContractRules() {
        ArrayList<B05WindowContractRule> rules = new ArrayList<B05WindowContractRule>();
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-001", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_001", true, 2));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-002", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_002", true, 3));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-003", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_003", true, 4));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-004", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_004", true, 5));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-005", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_005", true, 6));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-006", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_006", true, 7));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-007", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_007", true, 1));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-008", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_008", true, 2));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-009", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_009", true, 3));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-010", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_010", true, 4));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-011", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_011", true, 5));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-012", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_012", true, 6));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-013", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_013", true, 7));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-014", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_014", true, 1));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-015", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_015", true, 2));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-016", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_016", true, 3));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-017", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_017", true, 4));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-018", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_018", true, 5));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-019", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_019", true, 6));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-020", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_020", true, 7));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-021", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_021", true, 1));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-022", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_022", true, 2));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-023", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_023", true, 3));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-024", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_024", true, 4));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-025", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_025", true, 5));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-026", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_026", true, 6));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-027", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_027", true, 7));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-028", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_028", true, 1));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-029", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_029", true, 2));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-030", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_030", true, 3));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-031", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_031", true, 4));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-032", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_032", true, 5));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-033", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_033", true, 6));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-034", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_034", true, 7));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-035", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_035", true, 1));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-036", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_036", true, 2));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-037", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_037", true, 3));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-038", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_038", true, 4));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-039", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_039", true, 5));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-040", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_040", true, 6));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-041", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_041", true, 7));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-042", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_042", true, 1));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-043", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_043", true, 2));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-044", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_044", true, 3));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-045", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_045", true, 4));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-046", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_046", true, 5));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-047", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_047", true, 6));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-048", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_048", true, 7));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-049", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_049", true, 1));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-050", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_050", true, 2));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-051", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_051", true, 3));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-052", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_052", true, 4));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-053", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_053", true, 5));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-054", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_054", true, 6));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-055", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_055", true, 7));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-056", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_056", true, 1));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-057", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_057", true, 2));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-058", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_058", true, 3));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-059", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_059", true, 4));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-060", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_060", true, 5));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-061", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_061", true, 6));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-062", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_062", true, 7));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-063", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_063", true, 1));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-064", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_064", true, 2));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-065", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_065", true, 3));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-066", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_066", true, 4));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-067", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_067", true, 5));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-068", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_068", true, 6));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-069", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_069", true, 7));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-070", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_070", true, 1));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-071", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_071", true, 2));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-072", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_072", true, 3));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-073", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_073", true, 4));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-074", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_074", true, 5));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-075", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_075", true, 6));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-076", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_076", true, 7));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-077", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_077", true, 1));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-078", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_078", true, 2));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-079", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_079", true, 3));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-080", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_080", true, 4));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-081", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_081", true, 5));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-082", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_082", true, 6));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-083", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_083", true, 7));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-084", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_084", true, 1));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-085", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_085", true, 2));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-086", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_086", true, 3));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-087", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_087", true, 4));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-088", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_088", true, 5));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-089", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_089", true, 6));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-090", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_090", true, 7));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-091", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_091", true, 1));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-092", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_092", true, 2));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-093", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_093", true, 3));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-094", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_094", true, 4));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-095", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_095", true, 5));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-096", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_096", true, 6));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-097", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_097", true, 7));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-098", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_098", true, 1));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-099", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_099", true, 2));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-100", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_100", true, 3));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-101", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_101", true, 4));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-102", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_102", true, 5));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-103", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_103", true, 6));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-104", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_104", true, 7));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-105", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_105", true, 1));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-106", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_106", true, 2));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-107", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_107", true, 3));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-108", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_108", true, 4));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-109", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_109", true, 5));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-110", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_110", true, 6));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-111", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_111", true, 7));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-112", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_112", true, 1));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-113", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_113", true, 2));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-114", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_114", true, 3));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-115", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_115", true, 4));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-116", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_116", true, 5));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-117", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_117", true, 6));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-118", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_118", true, 7));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-119", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_119", true, 1));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-120", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_120", true, 2));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-121", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_121", true, 3));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-122", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_122", true, 4));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-123", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_123", true, 5));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-124", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_124", true, 6));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-125", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_125", true, 7));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-126", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_126", true, 1));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-127", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_127", true, 2));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-128", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_128", true, 3));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-129", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_129", true, 4));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-130", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_130", true, 5));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-131", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_131", true, 6));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-132", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_132", true, 7));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-133", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_133", true, 1));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-134", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_134", true, 2));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-135", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_135", true, 3));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-136", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_136", true, 4));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-137", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_137", true, 5));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-138", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_138", true, 6));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-139", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_139", true, 7));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-140", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_140", true, 1));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-141", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_141", true, 2));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-142", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_142", true, 3));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-143", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_143", true, 4));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-144", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_144", true, 5));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-145", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_145", true, 6));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-146", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_146", true, 7));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-147", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_147", true, 1));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-148", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_148", true, 2));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-149", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_149", true, 3));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-150", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_150", true, 4));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-151", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_151", true, 5));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-152", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_152", true, 6));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-153", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_153", true, 7));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-154", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_154", true, 1));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-155", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_155", true, 2));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-156", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_156", true, 3));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-157", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_157", true, 4));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-158", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_158", true, 5));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-159", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_159", true, 6));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-160", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_160", true, 7));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-161", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_161", true, 1));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-162", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_162", true, 2));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-163", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_163", true, 3));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-164", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_164", true, 4));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-165", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_165", true, 5));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-166", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_166", true, 6));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-167", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_167", true, 7));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-168", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_168", true, 1));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-169", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_169", true, 2));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-170", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_170", true, 3));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-171", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_171", true, 4));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-172", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_172", true, 5));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-173", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_173", true, 6));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-174", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_174", true, 7));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-175", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_175", true, 1));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-176", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_176", true, 2));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-177", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_177", true, 3));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-178", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_178", true, 4));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-179", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_179", true, 5));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-180", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_180", true, 6));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-181", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_181", true, 7));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-182", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_182", true, 1));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-183", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_183", true, 2));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-184", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_184", true, 3));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-185", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_185", true, 4));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-186", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_186", true, 5));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-187", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_187", true, 6));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-188", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_188", true, 7));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-189", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_189", true, 1));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-190", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_190", true, 2));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-191", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_191", true, 3));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-192", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_192", true, 4));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-193", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_193", true, 5));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-194", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_194", true, 6));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-195", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_195", true, 7));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-196", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_196", true, 1));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-197", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_197", true, 2));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-198", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_198", true, 3));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-199", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_199", true, 4));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-200", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_200", true, 5));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-201", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_201", true, 6));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-202", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_202", true, 7));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-203", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_203", true, 1));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-204", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_204", true, 2));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-205", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_205", true, 3));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-206", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_206", true, 4));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-207", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_207", true, 5));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-208", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_208", true, 6));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-209", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_209", true, 7));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-210", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_210", true, 1));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-211", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_211", true, 2));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-212", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_212", true, 3));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-213", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_213", true, 4));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-214", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_214", true, 5));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-215", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_215", true, 6));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-216", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_216", true, 7));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-217", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_217", true, 1));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-218", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_218", true, 2));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-219", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_219", true, 3));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-220", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_220", true, 4));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-221", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_221", true, 5));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-222", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_222", true, 6));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-223", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_223", true, 7));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-224", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_224", true, 1));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-225", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_225", true, 2));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-226", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_226", true, 3));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-227", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_227", true, 4));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-228", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_228", true, 5));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-229", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_229", true, 6));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-230", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_230", true, 7));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-231", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_231", true, 1));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-232", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_232", true, 2));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-233", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_233", true, 3));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-234", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_234", true, 4));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-235", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_235", true, 5));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-236", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_236", true, 6));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-237", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_237", true, 7));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-238", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_238", true, 1));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-239", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_239", true, 2));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-240", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_240", true, 3));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-241", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_241", true, 4));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-242", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_242", true, 5));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-243", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_243", true, 6));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-244", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_244", true, 7));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-245", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_245", true, 1));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-246", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_246", true, 2));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-247", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_247", true, 3));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-248", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_248", true, 4));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-249", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_249", true, 5));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-250", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_250", true, 6));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-251", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_251", true, 7));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-252", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_252", true, 1));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-253", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_253", true, 2));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-254", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_254", true, 3));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-255", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_255", true, 4));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-256", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_256", true, 5));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-257", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_257", true, 6));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-258", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_258", true, 7));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-259", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_259", true, 1));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-260", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_260", true, 2));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-261", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_261", true, 3));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-262", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_262", true, 4));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-263", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_263", true, 5));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-264", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_264", true, 6));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-265", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_265", true, 7));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-266", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_266", true, 1));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-267", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_267", true, 2));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-268", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_268", true, 3));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-269", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_269", true, 4));
        rules.add(
                new B05WindowContractRule(
                        "B05-WIN-270", "WindowFeatureEngine", "EVIDENCE_ONLY_WINDOW_270", true, 5));
        return rules;
    }

    public static String b05WindowSchemaHash() {
        StringBuilder out = new StringBuilder();
        List<B05WindowContractRule> rules = b05WindowContractRules();
        for (int i = 0; i < rules.size(); i++) out.append(rules.get(i).canonical()).append(';');
        return V51ChecksumEngine.sha256(B05_WINDOW_VERSION + "|" + out.toString());
    }

    private static double arrayValue(double[] values, int index, double fallback) {
        if (values == null || index < 0 || index >= values.length) return fallback;
        return V51Math.finite(values[index]);
    }

    private static double stateValue(
            V51Models.StateTrajectorySnapshot[] states, int index, int field) {
        if (states == null || index < 0 || index >= states.length || states[index] == null)
            return field == 0 ? 0.0d : 1.0d;
        return field == 0 ? states[index].confidence : states[index].changePointRisk;
    }

    public static final class B05WindowEvidencePacket {
        public final int position;
        public final double support;
        public final double counterEvidence;
        public final double risk;
        public final double confidence;
        public final int sample;
        public final double freshness;
        public final double concentrationSlope;
        public final double entropySlope;
        public final double shortConcentration;
        public final double mediumConcentration;
        public final double longConcentration;
        public final double shortEntropy;
        public final double mediumEntropy;
        public final double longEntropy;
        public final String checksum;

        B05WindowEvidencePacket(
                int position,
                double support,
                double counterEvidence,
                double risk,
                double confidence,
                int sample,
                double freshness,
                double concentrationSlope,
                double entropySlope,
                double shortConcentration,
                double mediumConcentration,
                double longConcentration,
                double shortEntropy,
                double mediumEntropy,
                double longEntropy,
                String checksum) {
            this.position = position;
            this.support = V51Math.clamp(support, 0.0d, 1.0d);
            this.counterEvidence = V51Math.clamp(counterEvidence, 0.0d, 1.0d);
            this.risk = V51Math.clamp(risk, 0.0d, 1.0d);
            this.confidence = V51Math.clamp(confidence, 0.0d, 1.0d);
            this.sample = Math.max(0, sample);
            this.freshness = V51Math.clamp(freshness, 0.0d, 1.0d);
            this.concentrationSlope = V51Math.finite(concentrationSlope);
            this.entropySlope = V51Math.finite(entropySlope);
            this.shortConcentration = V51Math.clamp(shortConcentration, 0.0d, 1.0d);
            this.mediumConcentration = V51Math.clamp(mediumConcentration, 0.0d, 1.0d);
            this.longConcentration = V51Math.clamp(longConcentration, 0.0d, 1.0d);
            this.shortEntropy = V51Math.clamp(shortEntropy, 0.0d, 1.0d);
            this.mediumEntropy = V51Math.clamp(mediumEntropy, 0.0d, 1.0d);
            this.longEntropy = V51Math.clamp(longEntropy, 0.0d, 1.0d);
            this.checksum = checksum == null ? "" : checksum;
        }

        public String canonical() {
            return position
                    + "|"
                    + support
                    + "|"
                    + counterEvidence
                    + "|"
                    + risk
                    + "|"
                    + confidence
                    + "|"
                    + sample
                    + "|"
                    + freshness
                    + "|"
                    + concentrationSlope
                    + "|"
                    + entropySlope
                    + "|"
                    + shortConcentration
                    + "|"
                    + mediumConcentration
                    + "|"
                    + longConcentration
                    + "|"
                    + shortEntropy
                    + "|"
                    + mediumEntropy
                    + "|"
                    + longEntropy
                    + "|"
                    + checksum;
        }
    }

    public static final class B05WindowEvidenceBundle {
        public final boolean valid;
        public final B05WindowEvidencePacket[] packets;
        public final int historyRows;
        public final String firstIssue;
        public final String lastIssue;
        public final double meanSupport;
        public final double meanCounterEvidence;
        public final double meanRisk;
        public final long elapsedMs;
        public final String checksum;
        public final String detail;

        B05WindowEvidenceBundle(
                boolean valid,
                B05WindowEvidencePacket[] packets,
                int historyRows,
                String firstIssue,
                String lastIssue,
                double meanSupport,
                double meanCounterEvidence,
                double meanRisk,
                long elapsedMs,
                String checksum,
                String detail) {
            this.valid = valid;
            this.packets = packets == null ? new B05WindowEvidencePacket[0] : packets;
            this.historyRows = Math.max(0, historyRows);
            this.firstIssue = firstIssue == null ? "" : firstIssue;
            this.lastIssue = lastIssue == null ? "" : lastIssue;
            this.meanSupport = V51Math.clamp(meanSupport, 0.0d, 1.0d);
            this.meanCounterEvidence = V51Math.clamp(meanCounterEvidence, 0.0d, 1.0d);
            this.meanRisk = V51Math.clamp(meanRisk, 0.0d, 1.0d);
            this.elapsedMs = Math.max(0L, elapsedMs);
            this.checksum = checksum == null ? "" : checksum;
            this.detail = detail == null ? "" : detail;
        }

        static B05WindowEvidenceBundle error(String detail) {
            String hash = V51ChecksumEngine.sha256(B05_WINDOW_VERSION + "|ERROR|" + detail);
            return new B05WindowEvidenceBundle(
                    false,
                    new B05WindowEvidencePacket[0],
                    0,
                    "",
                    "",
                    0.0d,
                    0.0d,
                    1.0d,
                    0L,
                    hash,
                    detail);
        }

        static String hash(
                String windowHash,
                B05WindowEvidencePacket[] packets,
                double meanSupport,
                double meanCounter,
                double meanRisk) {
            StringBuilder out = new StringBuilder();
            if (packets != null) {
                for (int i = 0; i < packets.length; i++)
                    out.append(packets[i].canonical()).append(';');
            }
            return V51ChecksumEngine.sha256(
                    B05_WINDOW_VERSION
                            + "|"
                            + windowHash
                            + "|"
                            + out.toString()
                            + "|"
                            + meanSupport
                            + "|"
                            + meanCounter
                            + "|"
                            + meanRisk);
        }

        public String summary() {
            return "valid="
                    + (valid ? 1 : 0)
                    + "|history="
                    + historyRows
                    + "|support="
                    + meanSupport
                    + "|counter="
                    + meanCounterEvidence
                    + "|risk="
                    + meanRisk
                    + "|cost="
                    + elapsedMs
                    + "ms|detail="
                    + detail;
        }
    }

    public static final class B05WindowContractRule {
        public final String id;
        public final String owner;
        public final String invariant;
        public final boolean mandatory;
        public final int severity;

        B05WindowContractRule(
                String id, String owner, String invariant, boolean mandatory, int severity) {
            this.id = id == null ? "" : id;
            this.owner = owner == null ? "" : owner;
            this.invariant = invariant == null ? "" : invariant;
            this.mandatory = mandatory;
            this.severity = Math.max(1, severity);
        }

        public String canonical() {
            return id + "|" + owner + "|" + invariant + "|" + (mandatory ? 1 : 0) + "|" + severity;
        }
    }
}
