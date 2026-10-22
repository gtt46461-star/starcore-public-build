package com.ethk3.novaonepro;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public final class SimilarShapeEngine {
    public static final String B06_CONTRACT_VERSION = "STARCORE_TIANSHU_B06_SIMILAR_EVIDENCE_V1";
    public static final String B06_OWNER = "SIMILAR_WINDOW_EVIDENCE_ONLY";

    private static final int MIN_REBUILD_WINDOW = 240;
    private static final int MAX_SCAN_HISTORY = 1800;
    private static final int MAX_MATCHES = 260;
    private static final double GOOD_DISTANCE = 28.0d;
    private static final double MAX_USABLE_DISTANCE = 48.0d;

    private final DataGuard dataGuard;
    private final WindowFeatureEngine windowEngine;

    public SimilarShapeEngine() {
        dataGuard = new DataGuard();
        windowEngine = new WindowFeatureEngine();
    }

    public Models.SimilarResult evaluate(
            List<Models.Draw> history, Models.ShapeVector current, int k3Mask) {
        Models.SimilarResult result = new Models.SimilarResult();

        if (current == null) {
            result.usable = 0;
            result.shapeKey = "";
            result.confidence = 0.0d;
            return result;
        }

        result.shapeKey = current.shapeKey == null ? "" : current.shapeKey;

        if (!AppConfig.isValidPosition(current.position) || !Models.isValidK3Mask(k3Mask)) {
            result.usable = 0;
            result.confidence = 0.0d;
            return result;
        }

        ArrayList<Models.Draw> clean = dataGuard.normalizeHistory(history);

        if (clean.size() < MIN_REBUILD_WINDOW + 2) {
            result.usable = 0;
            result.rawSample = clean.size();
            result.effectiveSample = 0;
            result.similarHitRate = AppConfig.RANDOM_K3_BASELINE;
            result.recentHitRate = AppConfig.RANDOM_K3_BASELINE;
            result.avgDistance = 100.0d;
            result.confidence = 0.0d;
            return result;
        }

        ArrayList<SimilarMatch> matches = findMatches(clean, current, k3Mask);
        result.rawSample = matches.size();

        if (matches.size() == 0) {
            result.usable = 0;
            result.effectiveSample = 0;
            result.similarHitRate = AppConfig.RANDOM_K3_BASELINE;
            result.recentHitRate = AppConfig.RANDOM_K3_BASELINE;
            result.avgDistance = 100.0d;
            result.confidence = 0.0d;
            return result;
        }

        Collections.sort(
                matches,
                new Comparator<SimilarMatch>() {
                    @Override
                    public int compare(SimilarMatch a, SimilarMatch b) {
                        if (a.distance < b.distance) {
                            return -1;
                        }
                        if (a.distance > b.distance) {
                            return 1;
                        }
                        return b.index - a.index;
                    }
                });

        int limit = Math.min(MAX_MATCHES, matches.size());
        double weightSum = 0.0d;
        double hitWeightSum = 0.0d;
        double distanceSum = 0.0d;
        int effective = 0;

        int recentTotal = 0;
        int recentHit = 0;

        for (int i = 0; i < limit; i++) {
            SimilarMatch m = matches.get(i);
            if (m == null) {
                continue;
            }

            if (m.distance > MAX_USABLE_DISTANCE) {
                continue;
            }

            double weight = distanceWeight(m.distance) * recencyWeight(i, limit);
            if (weight <= 0.0d) {
                continue;
            }

            effective++;
            weightSum += weight;
            distanceSum += m.distance;

            if (m.hit == 1) {
                hitWeightSum += weight;
            }

            if (i < 80) {
                recentTotal++;
                if (m.hit == 1) {
                    recentHit++;
                }
            }
        }

        if (effective <= 0 || weightSum <= 0.0d) {
            result.usable = 0;
            result.effectiveSample = 0;
            result.similarHitRate = AppConfig.RANDOM_K3_BASELINE;
            result.recentHitRate = AppConfig.RANDOM_K3_BASELINE;
            result.avgDistance = 100.0d;
            result.confidence = 0.0d;
            return result;
        }

        result.effectiveSample = effective;
        result.similarHitRate = AppConfig.clampProbability(hitWeightSum / weightSum);
        result.recentHitRate =
                recentTotal > 0 ? ((double) recentHit) / recentTotal : result.similarHitRate;
        result.avgDistance = distanceSum / effective;
        result.confidence = calcConfidence(result);

        result.usable = result.isStrongEnough() && result.confidence >= 52.0d ? 1 : 0;

        return result;
    }

    public Models.SimilarResult evaluateCandidate(
            List<Models.Draw> history, Models.ComboCandidate candidate) {
        if (candidate == null || !candidate.isValidK3()) {
            Models.SimilarResult r = new Models.SimilarResult();
            r.usable = 0;
            r.similarHitRate = AppConfig.RANDOM_K3_BASELINE;
            r.recentHitRate = AppConfig.RANDOM_K3_BASELINE;
            r.avgDistance = 100.0d;
            r.confidence = 0.0d;
            return r;
        }

        ArrayList<Models.Draw> clean = dataGuard.normalizeHistory(history);
        Models.ShapeVector current = windowEngine.buildForPosition(clean, candidate.position);

        if (current == null) {
            Models.SimilarResult r = new Models.SimilarResult();
            r.usable = 0;
            r.similarHitRate = AppConfig.RANDOM_K3_BASELINE;
            r.recentHitRate = AppConfig.RANDOM_K3_BASELINE;
            r.avgDistance = 100.0d;
            r.confidence = 0.0d;
            return r;
        }

        current.stateMode = candidate.stateMode;
        current.shapeKey = candidate.shapeKey;
        current.trendLife = candidate.trendLife;
        current.driftRisk = candidate.driftRisk;
        current.breakDepth = candidate.breakDepth;
        current.recoveryScore = candidate.recoveryScore;
        current.championGap = candidate.championGap;
        current.comboGap = candidate.comboGap;
        current.hotReverseRisk = candidate.hotReverseRisk;
        current.falseStrongRisk = candidate.falseStrongRisk;

        return evaluate(clean, current, candidate.k3Mask);
    }

    public void enrichCandidate(List<Models.Draw> history, Models.ComboCandidate candidate) {
        if (candidate == null || !candidate.isValidK3()) {
            return;
        }

        Models.SimilarResult r = evaluateCandidate(history, candidate);
        B06SimilarEvidence evidence =
                buildB06Evidence(
                        r,
                        candidate.position,
                        candidate.k3Mask,
                        candidate.baseIssue,
                        candidate.targetIssue);

        candidate.effectiveSample = r.effectiveSample;
        candidate.similarHitRate = r.similarHitRate;
        candidate.deepStateScore = blend01(candidate.deepStateScore, evidence.support, 0.10d);
        candidate.evidenceScore = blend01(candidate.evidenceScore, evidence.support, 0.08d);
        candidate.evidenceRisk = blend01(candidate.evidenceRisk, evidence.risk, 0.08d);
        candidate.reasonSummary =
                appendReason(
                        candidate.reasonSummary,
                        "B06相似窗口EvidenceOnly：样本="
                                + r.effectiveSample
                                + "，胜率="
                                + UiFormatter.probability(r.similarHitRate)
                                + "，反证="
                                + UiFormatter.probability(evidence.counterEvidence)
                                + "，风险="
                                + UiFormatter.probability(evidence.risk)
                                + "，hash="
                                + shortHash(evidence.fingerprint));
    }

    public ArrayList<Models.ComboCandidate> enrichAll(
            List<Models.Draw> history, List<Models.ComboCandidate> candidates) {
        ArrayList<Models.ComboCandidate> out = new ArrayList<Models.ComboCandidate>();

        if (candidates == null || candidates.size() == 0) {
            return out;
        }

        for (int i = 0; i < candidates.size(); i++) {
            Models.ComboCandidate c = candidates.get(i);
            if (c == null) {
                continue;
            }

            Models.ComboCandidate copy = c.copy();
            enrichCandidate(history, copy);
            out.add(copy);
        }

        return out;
    }

    public boolean similarAllowsBet(Models.SimilarResult result) {
        if (result == null) {
            return false;
        }

        if (result.effectiveSample < AppConfig.EFFECTIVE_SAMPLE_MIN) {
            return false;
        }

        if (result.similarHitRate < AppConfig.SIMILAR_HIT_RATE_MIN) {
            return false;
        }

        if (result.avgDistance > MAX_USABLE_DISTANCE) {
            return false;
        }

        if (result.confidence < 52.0d) {
            return false;
        }

        return true;
    }

    public double gateAdjustment(Models.SimilarResult result) {
        /* B06 compatibility projection only. DecisionGateEngine owns the gate. */
        return 0.0d;
    }

    public double probabilityAdjustment(Models.SimilarResult result) {
        /* B06 compatibility projection only. Probability owner consumes evidence explicitly. */
        return 0.0d;
    }

    public String explain(Models.SimilarResult result) {
        if (result == null) {
            return "相似形态为空";
        }

        StringBuilder sb = new StringBuilder();

        sb.append("相似样本=");
        sb.append(result.effectiveSample);

        sb.append(" 原始样本=");
        sb.append(result.rawSample);

        sb.append(" 相似胜率=");
        sb.append(UiFormatter.probability(result.similarHitRate));

        sb.append(" 近样本胜率=");
        sb.append(UiFormatter.probability(result.recentHitRate));

        sb.append(" 平均距离=");
        sb.append(UiFormatter.score(result.avgDistance));

        sb.append(" 置信度=");
        sb.append(UiFormatter.score(result.confidence));

        if (similarAllowsBet(result)) {
            sb.append(" 结论=相似形态支持候选");
        } else {
            sb.append(" 结论=相似形态不足，收紧或HOLD");
        }

        return sb.toString();
    }

    private ArrayList<SimilarMatch> findMatches(
            ArrayList<Models.Draw> clean, Models.ShapeVector current, int k3Mask) {
        ArrayList<SimilarMatch> matches = new ArrayList<SimilarMatch>();

        if (clean == null || current == null || !Models.isValidK3Mask(k3Mask)) {
            return matches;
        }

        int size = clean.size();
        int start = Math.max(MIN_REBUILD_WINDOW, size - MAX_SCAN_HISTORY - 1);

        int step = 1;
        if (size - start > 1200) {
            step = 2;
        }

        for (int i = start; i < size - 1; i += step) {
            ArrayList<Models.Draw> past = new ArrayList<Models.Draw>();
            int from = i - AppConfig.WINDOW_960;
            if (from < 0) {
                from = 0;
            }

            for (int j = from; j <= i; j++) {
                past.add(clean.get(j));
            }

            if (past.size() < MIN_REBUILD_WINDOW) {
                continue;
            }

            Models.ShapeVector old = windowEngine.buildForPosition(past, current.position);
            if (old == null) {
                continue;
            }

            old.stateMode = normalizeComparableMode(old.stateMode);
            current.stateMode = normalizeComparableMode(current.stateMode);

            double distance = shapeDistance(current, old);

            if (distance > MAX_USABLE_DISTANCE) {
                continue;
            }

            Models.Draw nextDraw = clean.get(i + 1);
            int actual = nextDraw.getNumberAtPosition(current.position);
            if (!AppConfig.isValidNumber(actual)) {
                continue;
            }

            SimilarMatch m = new SimilarMatch();
            m.index = i;
            m.distance = distance;
            m.position = current.position;
            m.actualNumber = actual;
            m.hit = Models.maskContains(k3Mask, actual) ? 1 : 0;
            m.issue = clean.get(i).issue;
            m.nextIssue = nextDraw.issue;
            matches.add(m);
        }

        return matches;
    }

    private double shapeDistance(Models.ShapeVector a, Models.ShapeVector b) {
        if (a == null || b == null) {
            return 100.0d;
        }

        double d = 0.0d;

        d += Math.abs(a.trendLife - b.trendLife) * 0.16d;
        d += Math.abs(a.driftRisk - b.driftRisk) * 0.18d;
        d += Math.abs(a.breakDepth - b.breakDepth) * 0.16d;
        d += Math.abs(a.recoveryScore - b.recoveryScore) * 0.13d;
        d += Math.abs(a.entropy64 - b.entropy64) * 0.08d;
        d += Math.abs(a.top3Mass64 - b.top3Mass64) * 100.0d * 0.08d;
        d += Math.abs(a.championGap - b.championGap) * 100.0d * 0.07d;
        d += Math.abs(a.comboGap - b.comboGap) * 100.0d * 0.06d;
        d += Math.abs(a.hotReverseRisk - b.hotReverseRisk) * 0.04d;
        d += Math.abs(a.falseStrongRisk - b.falseStrongRisk) * 0.04d;

        if (!safeEquals(a.stateMode, b.stateMode)) {
            if (sameFamilyMode(a.stateMode, b.stateMode)) {
                d += 5.0d;
            } else {
                d += 12.0d;
            }
        }

        double distProb = ShapeMath.distributionDistance(a.numberProbs, b.numberProbs);
        d += distProb * 0.12d;

        if (d < 0.0d) {
            d = 0.0d;
        }

        if (d > 100.0d) {
            d = 100.0d;
        }

        return d;
    }

    private double distanceWeight(double distance) {
        if (distance <= 0.0d) {
            return 1.0d;
        }

        if (distance >= MAX_USABLE_DISTANCE) {
            return 0.05d;
        }

        double x = 1.0d - distance / MAX_USABLE_DISTANCE;
        if (x < 0.05d) {
            x = 0.05d;
        }

        return x * x;
    }

    private double recencyWeight(int rank, int total) {
        if (total <= 0) {
            return 1.0d;
        }

        double x = 1.0d - ((double) rank) / ((double) total);
        return 0.70d + x * 0.30d;
    }

    private double calcConfidence(Models.SimilarResult r) {
        if (r == null) {
            return 0.0d;
        }

        double score = 0.0d;

        score += Math.min(40.0d, r.effectiveSample / 10.0d);
        score +=
                (MAX_USABLE_DISTANCE - Math.min(MAX_USABLE_DISTANCE, r.avgDistance))
                        / MAX_USABLE_DISTANCE
                        * 30.0d;
        score += Math.abs(r.similarHitRate - AppConfig.RANDOM_K3_BASELINE) * 100.0d * 0.20d;

        if (r.recentHitRate >= r.similarHitRate - 0.020d) {
            score += 8.0d;
        } else {
            score -= 8.0d;
        }

        if (r.effectiveSample >= AppConfig.EFFECTIVE_SAMPLE_HIGH) {
            score += 8.0d;
        }

        return ShapeMath.clamp100(score);
    }

    private String normalizeComparableMode(String mode) {
        if (mode == null || mode.length() == 0) {
            return AppConfig.MODE_CHAOS;
        }

        if (AppConfig.MODE_ACCELERATING.equals(mode)) {
            return AppConfig.MODE_CONFIRMED;
        }

        return mode;
    }

    private boolean sameFamilyMode(String a, String b) {
        if (a == null || b == null) {
            return false;
        }

        if (isGood(a) && isGood(b)) {
            return true;
        }

        if (isWeak(a) && isWeak(b)) {
            return true;
        }

        if (isBad(a) && isBad(b)) {
            return true;
        }

        return false;
    }

    private boolean isGood(String mode) {
        return AppConfig.MODE_CONFIRMED.equals(mode)
                || AppConfig.MODE_ACCELERATING.equals(mode)
                || AppConfig.MODE_RECOVERING.equals(mode);
    }

    private boolean isWeak(String mode) {
        return AppConfig.MODE_FORMING.equals(mode)
                || AppConfig.MODE_PULLBACK.equals(mode)
                || AppConfig.MODE_WEAKENING.equals(mode);
    }

    private boolean isBad(String mode) {
        return AppConfig.MODE_BROKEN.equals(mode)
                || AppConfig.MODE_REVERSING.equals(mode)
                || AppConfig.MODE_CHAOS.equals(mode);
    }

    private boolean safeEquals(String a, String b) {
        if (a == null) {
            a = "";
        }
        if (b == null) {
            b = "";
        }
        return a.equals(b);
    }

    private String appendReason(String oldText, String add) {
        if (add == null || add.length() == 0) {
            return oldText == null ? "" : oldText;
        }

        if (oldText == null || oldText.length() == 0) {
            return add;
        }

        return oldText + "；" + add;
    }

    private static final class SimilarMatch {
        public int index;
        public String issue;
        public String nextIssue;
        public int position;
        public int actualNumber;
        public int hit;
        public double distance;

        public SimilarMatch() {
            index = 0;
            issue = "";
            nextIssue = "";
            position = 0;
            actualNumber = 0;
            hit = 0;
            distance = 100.0d;
        }
    }

    public B06SimilarEvidence evaluateB06(
            List<Models.Draw> history, Models.ShapeVector current, int k3Mask) {
        Models.SimilarResult result = evaluate(history, current, k3Mask);
        return buildB06Evidence(
                result,
                current == null ? 0 : current.position,
                k3Mask,
                current == null ? "" : current.baseIssue,
                current == null ? "" : current.targetIssue);
    }

    public B06SimilarEvidence evaluateCandidateB06(
            List<Models.Draw> history, Models.ComboCandidate candidate) {
        Models.SimilarResult result = evaluateCandidate(history, candidate);
        return buildB06Evidence(
                result,
                candidate == null ? 0 : candidate.position,
                candidate == null ? 0 : candidate.k3Mask,
                candidate == null ? "" : candidate.baseIssue,
                candidate == null ? "" : candidate.targetIssue);
    }

    public B06SimilarEvidence buildB06Evidence(
            Models.SimilarResult result,
            int position,
            int k3Mask,
            String baseIssue,
            String targetIssue) {
        B06SimilarEvidence e = new B06SimilarEvidence();
        e.baseIssue = safeText(baseIssue);
        e.targetIssue = safeText(targetIssue);
        e.position = position;
        e.k3Mask = k3Mask;
        if (result == null) {
            e.reason = "相似窗口结果为空；不改概率、不改门槛、不写BET";
            e.fingerprint = V51ChecksumEngine.sha256(e.stablePayload());
            return e;
        }
        e.shapeKey = safeText(result.shapeKey);
        e.sample = result.effectiveSample;
        e.rawSample = result.rawSample;
        e.hitRate = result.similarHitRate;
        e.recentHitRate = result.recentHitRate;
        e.avgDistance = result.avgDistance;
        double confidence01 =
                result.confidence > 1.5d ? result.confidence / 100.0d : result.confidence;
        e.support =
                clamp01(
                        (result.similarHitRate - AppConfig.RANDOM_K3_BASELINE + 0.10d)
                                        / 0.20d
                                        * 0.46d
                                + (result.recentHitRate - AppConfig.RANDOM_K3_BASELINE + 0.10d)
                                        / 0.20d
                                        * 0.24d
                                + clamp01(confidence01) * 0.20d
                                + clamp01(1.0d - result.avgDistance / 100.0d) * 0.10d);
        e.counterEvidence =
                clamp01(
                        Math.max(0.0d, AppConfig.RANDOM_K3_BASELINE - result.similarHitRate)
                                        * 3.5d
                                        * 0.42d
                                + Math.max(0.0d, result.similarHitRate - result.recentHitRate)
                                        * 3.0d
                                        * 0.28d
                                + clamp01(result.avgDistance / 100.0d) * 0.20d
                                + (result.effectiveSample < AppConfig.EFFECTIVE_SAMPLE_MIN
                                        ? 0.10d
                                        : 0.0d));
        e.risk =
                clamp01(
                        e.counterEvidence * 0.58d
                                + clamp01(result.avgDistance / MAX_USABLE_DISTANCE) * 0.22d
                                + (1.0d - clamp01(confidence01)) * 0.20d);
        e.confidence =
                clamp01(
                        clamp01(confidence01) * 0.55d
                                + clamp01(
                                                result.effectiveSample
                                                        / (double) AppConfig.EFFECTIVE_SAMPLE_HIGH)
                                        * 0.30d
                                + (1.0d - e.risk) * 0.15d);
        e.freshness = clamp01(1.0d - Math.abs(result.similarHitRate - result.recentHitRate) * 3.0d);
        e.diversity =
                clamp01(
                        result.rawSample <= 0
                                ? 0.0d
                                : result.effectiveSample / (double) result.rawSample);
        e.evidenceReady =
                result.effectiveSample >= AppConfig.EFFECTIVE_SAMPLE_MIN
                                && result.avgDistance <= MAX_USABLE_DISTANCE
                                && e.confidence >= 0.45d
                        ? 1
                        : 0;
        e.advisoryAllowed = similarAllowsBet(result) ? 1 : 0;
        e.reason = "相似窗口只输出支持/反证/风险/置信/样本/新鲜度；DecisionGate独占BET";
        e.fingerprint = V51ChecksumEngine.sha256(e.stablePayload());
        return e;
    }

    private double blend01(double oldValue, double newValue, double weight) {
        return clamp01(clamp01(oldValue) * (1.0d - weight) + clamp01(newValue) * weight);
    }

    private double clamp01(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) return 0.0d;
        if (value < 0.0d) return 0.0d;
        if (value > 1.0d) return 1.0d;
        return value;
    }

    private String safeText(String value) {
        return value == null ? "" : value;
    }

    private String shortHash(String hash) {
        if (hash == null) return "";
        return hash.length() <= 12 ? hash : hash.substring(0, 12);
    }

    public B06SimilarTemporalAudit auditB06TemporalNeighbors(
            List<Models.Draw> history, Models.ShapeVector current, int k3Mask) {
        B06SimilarTemporalAudit audit = new B06SimilarTemporalAudit();
        audit.baseIssue = current == null ? "" : safeText(current.baseIssue);
        audit.targetIssue = current == null ? "" : safeText(current.targetIssue);
        audit.position = current == null ? 0 : current.position;
        audit.k3Mask = k3Mask;
        if (current == null
                || !AppConfig.isValidPosition(audit.position)
                || !Models.isValidK3Mask(k3Mask)) {
            audit.status = "INVALID_INPUT";
            audit.reason = "位置、K3或当前形态无效";
            audit.fingerprint = V51ChecksumEngine.sha256(audit.stablePayload());
            return audit;
        }
        audit.targetAfterBase =
                PredictionContractEngine.compareIssue(audit.targetIssue, audit.baseIssue) > 0
                        ? 1
                        : 0;
        ArrayList<Models.Draw> visible = selectB06VisibleHistory(history, audit.baseIssue, audit);
        audit.visibleSample = visible.size();
        if (audit.targetAfterBase != 1 || visible.size() < MIN_REBUILD_WINDOW + 2) {
            audit.status =
                    audit.targetAfterBase == 1 ? "VISIBLE_SAMPLE_WEAK" : "TARGET_BOUNDARY_FAILED";
            audit.reason =
                    audit.targetAfterBase == 1
                            ? "开奖前可见历史不足，不能形成稳定相似证据"
                            : "targetIssue必须严格晚于baseIssue";
            audit.fingerprint = V51ChecksumEngine.sha256(audit.stablePayload());
            return audit;
        }
        ArrayList<SimilarMatch> matches = findMatches(visible, current, k3Mask);
        Collections.sort(
                matches,
                new Comparator<SimilarMatch>() {
                    @Override
                    public int compare(SimilarMatch a, SimilarMatch b) {
                        if (a.index != b.index) return b.index - a.index;
                        if (a.distance < b.distance) return -1;
                        if (a.distance > b.distance) return 1;
                        return safeIssue(a.nextIssue).compareTo(safeIssue(b.nextIssue));
                    }
                });
        audit.rawMatchCount = matches.size();
        audit.horizon24 = summarizeB06Horizon(matches, 24, "MATCH_24");
        audit.horizon48 = summarizeB06Horizon(matches, 48, "MATCH_48");
        audit.horizon96 = summarizeB06Horizon(matches, 96, "MATCH_96");
        audit.horizon240 = summarizeB06Horizon(matches, 240, "MATCH_240");
        audit.duplicateNextIssueCount = countB06DuplicateNextIssues(matches, 240);
        audit.overlapRisk =
                clamp01(
                        audit.rawMatchCount <= 0
                                ? 1.0d
                                : audit.duplicateNextIssueCount
                                        / (double) Math.min(240, audit.rawMatchCount));
        audit.hitRateDispersion =
                b06Dispersion(
                        new double[] {
                            audit.horizon24.hitRate, audit.horizon48.hitRate,
                            audit.horizon96.hitRate, audit.horizon240.hitRate
                        });
        audit.distanceDispersion =
                b06Dispersion(
                        new double[] {
                            normalizeDistance(audit.horizon24.avgDistance),
                                    normalizeDistance(audit.horizon48.avgDistance),
                            normalizeDistance(audit.horizon96.avgDistance),
                                    normalizeDistance(audit.horizon240.avgDistance)
                        });
        audit.crossHorizonStability =
                clamp01(
                        1.0d
                                - audit.hitRateDispersion * 3.0d
                                - audit.distanceDispersion * 0.80d
                                - audit.overlapRisk * 0.50d);
        audit.shortLongGap = Math.abs(audit.horizon24.hitRate - audit.horizon240.hitRate);
        audit.recentDecayRisk =
                clamp01(Math.max(0.0d, audit.horizon240.hitRate - audit.horizon24.hitRate) * 4.0d);
        audit.support =
                clamp01(
                        horizonSupport(audit.horizon24) * 0.32d
                                + horizonSupport(audit.horizon48) * 0.28d
                                + horizonSupport(audit.horizon96) * 0.24d
                                + horizonSupport(audit.horizon240) * 0.16d);
        audit.counterEvidence =
                clamp01(
                        horizonCounter(audit.horizon24) * 0.34d
                                + horizonCounter(audit.horizon48) * 0.28d
                                + horizonCounter(audit.horizon96) * 0.22d
                                + horizonCounter(audit.horizon240) * 0.16d
                                + audit.recentDecayRisk * 0.18d);
        audit.risk =
                clamp01(
                        audit.counterEvidence * 0.50d
                                + (1.0d - audit.crossHorizonStability) * 0.28d
                                + audit.overlapRisk * 0.12d
                                + clamp01(audit.futureExcludedCount / 10.0d) * 0.10d);
        audit.confidence =
                clamp01(
                        sampleMaturity(audit.horizon96.sample, 96) * 0.32d
                                + sampleMaturity(audit.horizon240.sample, 160) * 0.18d
                                + audit.crossHorizonStability * 0.28d
                                + (1.0d - audit.risk) * 0.22d);
        audit.minimumLeaveOneOutSupport = minimumB06LeaveOneOut(audit);
        audit.ablationFragility =
                clamp01(
                        (audit.support - audit.minimumLeaveOneOutSupport) * 1.45d
                                + (1.0d - audit.crossHorizonStability) * 0.35d);
        audit.evidenceReady =
                audit.targetAfterBase == 1
                                && audit.futureVisibleCount == 0
                                && audit.horizon96.sample >= AppConfig.EFFECTIVE_SAMPLE_MIN
                                && audit.confidence >= 0.45d
                                && audit.risk <= 0.62d
                        ? 1
                        : 0;
        if (audit.futureVisibleCount > 0) {
            audit.status = "FUTURE_LEAK_BLOCKED";
            audit.reason = "检测到未来开奖进入可见窗口，证据强制不可用";
        } else if (audit.ablationFragility >= 0.68d) {
            audit.status = "ABLATION_FRAGILE";
            audit.reason = "相似优势依赖单一时间尺度，必须降权或冻结";
        } else if (audit.recentDecayRisk >= 0.55d) {
            audit.status = "RECENT_DECAY";
            audit.reason = "近期相似窗口明显衰减，历史优势不可外推";
        } else if (audit.evidenceReady == 1) {
            audit.status = "EVIDENCE_READY";
            audit.reason = "时点边界、样本、跨尺度稳定性和风险均可审计";
        } else {
            audit.status = "EVIDENCE_INSUFFICIENT";
            audit.reason = "仅保留观察，不改变FinalP、Gate或BET";
        }
        audit.fingerprint = V51ChecksumEngine.sha256(audit.stablePayload());
        return audit;
    }

    public B06SimilarTemporalAudit auditCandidateB06TemporalNeighbors(
            List<Models.Draw> history, Models.ComboCandidate candidate) {
        if (candidate == null || !candidate.isValidK3()) {
            B06SimilarTemporalAudit audit = new B06SimilarTemporalAudit();
            audit.status = "INVALID_CANDIDATE";
            audit.reason = "候选为空或不是固定K3";
            audit.fingerprint = V51ChecksumEngine.sha256(audit.stablePayload());
            return audit;
        }
        ArrayList<Models.Draw> visible = dataGuard.normalizeHistory(history);
        Models.ShapeVector current = windowEngine.buildForPosition(visible, candidate.position);
        if (current != null) {
            current.baseIssue = candidate.baseIssue;
            current.targetIssue = candidate.targetIssue;
            current.stateMode = candidate.stateMode;
            current.shapeKey = candidate.shapeKey;
            current.trendLife = candidate.trendLife;
            current.driftRisk = candidate.driftRisk;
            current.breakDepth = candidate.breakDepth;
            current.recoveryScore = candidate.recoveryScore;
            current.championGap = candidate.championGap;
            current.comboGap = candidate.comboGap;
            current.hotReverseRisk = candidate.hotReverseRisk;
            current.falseStrongRisk = candidate.falseStrongRisk;
        }
        return auditB06TemporalNeighbors(visible, current, candidate.k3Mask);
    }

    private ArrayList<Models.Draw> selectB06VisibleHistory(
            List<Models.Draw> history, String baseIssue, B06SimilarTemporalAudit audit) {
        ArrayList<Models.Draw> clean = dataGuard.normalizeHistory(history);
        ArrayList<Models.Draw> visible = new ArrayList<Models.Draw>();
        for (int i = 0; i < clean.size(); i++) {
            Models.Draw draw = clean.get(i);
            if (draw == null || !draw.isValid()) {
                audit.invalidDrawCount++;
                continue;
            }
            int cmp = PredictionContractEngine.compareIssue(draw.issue, baseIssue);
            if (baseIssue == null || baseIssue.length() == 0 || cmp <= 0) {
                visible.add(draw);
            } else {
                audit.futureExcludedCount++;
            }
        }
        for (int i = 0; i < visible.size(); i++) {
            Models.Draw draw = visible.get(i);
            if (PredictionContractEngine.compareIssue(draw.issue, baseIssue) > 0) {
                audit.futureVisibleCount++;
            }
        }
        return visible;
    }

    private B06SimilarHorizon summarizeB06Horizon(
            List<SimilarMatch> matches, int limit, String name) {
        B06SimilarHorizon h = new B06SimilarHorizon();
        h.name = name;
        if (matches == null || matches.size() == 0) {
            h.fingerprint = V51ChecksumEngine.sha256(h.stablePayload());
            return h;
        }
        int usable = Math.min(limit, matches.size());
        double weightedHit = 0.0d;
        double weightSum = 0.0d;
        double distanceSum = 0.0d;
        double distanceSqSum = 0.0d;
        int rawHit = 0;
        int accepted = 0;
        String firstIssue = "";
        String lastIssue = "";
        for (int i = 0; i < usable; i++) {
            SimilarMatch m = matches.get(i);
            if (m == null || m.distance > MAX_USABLE_DISTANCE) continue;
            double w = distanceWeight(m.distance) * (1.0d - i / (double) (usable + 2));
            if (w <= 0.0d) continue;
            if (accepted == 0) firstIssue = safeIssue(m.nextIssue);
            lastIssue = safeIssue(m.nextIssue);
            accepted++;
            if (m.hit == 1) rawHit++;
            weightedHit += m.hit * w;
            weightSum += w;
            distanceSum += m.distance;
            distanceSqSum += m.distance * m.distance;
        }
        h.sample = accepted;
        h.rawHit = rawHit;
        h.hitRate =
                weightSum <= 0.0d ? AppConfig.RANDOM_K3_BASELINE : clamp01(weightedHit / weightSum);
        h.avgDistance = accepted <= 0 ? 100.0d : distanceSum / accepted;
        double meanSq = accepted <= 0 ? 10000.0d : distanceSqSum / accepted;
        h.distanceStd = Math.sqrt(Math.max(0.0d, meanSq - h.avgDistance * h.avgDistance));
        h.wilsonLower95 = b06WilsonLower(rawHit, accepted);
        h.edge = h.hitRate - AppConfig.RANDOM_K3_BASELINE;
        h.firstIssue = firstIssue;
        h.lastIssue = lastIssue;
        h.fingerprint = V51ChecksumEngine.sha256(h.stablePayload());
        return h;
    }

    private int countB06DuplicateNextIssues(List<SimilarMatch> matches, int limit) {
        if (matches == null || matches.size() == 0) return 0;
        ArrayList<String> seen = new ArrayList<String>();
        int duplicate = 0;
        int n = Math.min(limit, matches.size());
        for (int i = 0; i < n; i++) {
            SimilarMatch m = matches.get(i);
            String issue = m == null ? "" : safeIssue(m.nextIssue);
            if (issue.length() == 0) continue;
            if (seen.contains(issue)) duplicate++;
            else seen.add(issue);
        }
        return duplicate;
    }

    private double horizonSupport(B06SimilarHorizon h) {
        if (h == null || h.sample <= 0) return 0.0d;
        return clamp01(
                (h.hitRate - AppConfig.RANDOM_K3_BASELINE + 0.08d) / 0.18d * 0.62d
                        + sampleMaturity(h.sample, 80) * 0.18d
                        + clamp01(1.0d - h.avgDistance / MAX_USABLE_DISTANCE) * 0.20d);
    }

    private double horizonCounter(B06SimilarHorizon h) {
        if (h == null || h.sample <= 0) return 1.0d;
        return clamp01(
                Math.max(0.0d, AppConfig.RANDOM_K3_BASELINE - h.hitRate) * 4.0d * 0.55d
                        + clamp01(h.avgDistance / MAX_USABLE_DISTANCE) * 0.25d
                        + (1.0d - sampleMaturity(h.sample, 80)) * 0.20d);
    }

    private double minimumB06LeaveOneOut(B06SimilarTemporalAudit a) {
        double s24 = horizonSupport(a.horizon24);
        double s48 = horizonSupport(a.horizon48);
        double s96 = horizonSupport(a.horizon96);
        double s240 = horizonSupport(a.horizon240);
        double leave24 = clamp01(s48 * 0.40d + s96 * 0.35d + s240 * 0.25d);
        double leave48 = clamp01(s24 * 0.42d + s96 * 0.36d + s240 * 0.22d);
        double leave96 = clamp01(s24 * 0.44d + s48 * 0.36d + s240 * 0.20d);
        double leave240 = clamp01(s24 * 0.42d + s48 * 0.33d + s96 * 0.25d);
        a.leave24OutSupport = leave24;
        a.leave48OutSupport = leave48;
        a.leave96OutSupport = leave96;
        a.leave240OutSupport = leave240;
        return Math.min(Math.min(leave24, leave48), Math.min(leave96, leave240));
    }

    private double b06WilsonLower(int hit, int sample) {
        if (sample <= 0) return 0.0d;
        double z = 1.959963984540054d;
        double n = sample;
        double p = hit / n;
        double z2 = z * z;
        double center = p + z2 / (2.0d * n);
        double spread = z * Math.sqrt((p * (1.0d - p) + z2 / (4.0d * n)) / n);
        return clamp01((center - spread) / (1.0d + z2 / n));
    }

    private double b06Dispersion(double[] values) {
        if (values == null || values.length == 0) return 1.0d;
        double mean = 0.0d;
        for (int i = 0; i < values.length; i++) mean += clamp01(values[i]);
        mean /= values.length;
        double sum = 0.0d;
        for (int i = 0; i < values.length; i++) {
            double d = clamp01(values[i]) - mean;
            sum += d * d;
        }
        return Math.sqrt(sum / values.length);
    }

    private double sampleMaturity(int sample, int target) {
        if (target <= 0) return 0.0d;
        return clamp01(sample / (double) target);
    }

    private double normalizeDistance(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) return 1.0d;
        return clamp01(value / MAX_USABLE_DISTANCE);
    }

    private static String safeIssue(String issue) {
        return issue == null ? "" : issue;
    }

    public static final class B06SimilarHorizon {
        public String name = "";
        public int sample = 0;
        public int rawHit = 0;
        public double hitRate = AppConfig.RANDOM_K3_BASELINE;
        public double wilsonLower95 = 0.0d;
        public double edge = 0.0d;
        public double avgDistance = 100.0d;
        public double distanceStd = 0.0d;
        public String firstIssue = "";
        public String lastIssue = "";
        public String fingerprint = "";

        public String stablePayload() {
            return name
                    + "|"
                    + sample
                    + "|"
                    + rawHit
                    + "|"
                    + Double.doubleToLongBits(hitRate)
                    + "|"
                    + Double.doubleToLongBits(wilsonLower95)
                    + "|"
                    + Double.doubleToLongBits(edge)
                    + "|"
                    + Double.doubleToLongBits(avgDistance)
                    + "|"
                    + Double.doubleToLongBits(distanceStd)
                    + "|"
                    + firstIssue
                    + "|"
                    + lastIssue;
        }
    }

    public static final class B06SimilarTemporalAudit {
        public String contractVersion = B06_CONTRACT_VERSION;
        public String owner = B06_OWNER;
        public String baseIssue = "";
        public String targetIssue = "";
        public int position = 0;
        public int k3Mask = 0;
        public int targetAfterBase = 0;
        public int visibleSample = 0;
        public int rawMatchCount = 0;
        public int invalidDrawCount = 0;
        public int futureExcludedCount = 0;
        public int futureVisibleCount = 0;
        public int duplicateNextIssueCount = 0;
        public double overlapRisk = 1.0d;
        public double hitRateDispersion = 1.0d;
        public double distanceDispersion = 1.0d;
        public double crossHorizonStability = 0.0d;
        public double shortLongGap = 1.0d;
        public double recentDecayRisk = 1.0d;
        public double support = 0.0d;
        public double counterEvidence = 1.0d;
        public double risk = 1.0d;
        public double confidence = 0.0d;
        public double leave24OutSupport = 0.0d;
        public double leave48OutSupport = 0.0d;
        public double leave96OutSupport = 0.0d;
        public double leave240OutSupport = 0.0d;
        public double minimumLeaveOneOutSupport = 0.0d;
        public double ablationFragility = 1.0d;
        public int evidenceReady = 0;
        public String status = "UNKNOWN";
        public String reason = "";
        public B06SimilarHorizon horizon24 = new B06SimilarHorizon();
        public B06SimilarHorizon horizon48 = new B06SimilarHorizon();
        public B06SimilarHorizon horizon96 = new B06SimilarHorizon();
        public B06SimilarHorizon horizon240 = new B06SimilarHorizon();
        public String fingerprint = "";

        public String stablePayload() {
            return contractVersion
                    + "|"
                    + owner
                    + "|"
                    + baseIssue
                    + "|"
                    + targetIssue
                    + "|"
                    + position
                    + "|"
                    + k3Mask
                    + "|"
                    + targetAfterBase
                    + "|"
                    + visibleSample
                    + "|"
                    + rawMatchCount
                    + "|"
                    + invalidDrawCount
                    + "|"
                    + futureExcludedCount
                    + "|"
                    + futureVisibleCount
                    + "|"
                    + duplicateNextIssueCount
                    + "|"
                    + Double.doubleToLongBits(overlapRisk)
                    + "|"
                    + Double.doubleToLongBits(hitRateDispersion)
                    + "|"
                    + Double.doubleToLongBits(distanceDispersion)
                    + "|"
                    + Double.doubleToLongBits(crossHorizonStability)
                    + "|"
                    + Double.doubleToLongBits(shortLongGap)
                    + "|"
                    + Double.doubleToLongBits(recentDecayRisk)
                    + "|"
                    + Double.doubleToLongBits(support)
                    + "|"
                    + Double.doubleToLongBits(counterEvidence)
                    + "|"
                    + Double.doubleToLongBits(risk)
                    + "|"
                    + Double.doubleToLongBits(confidence)
                    + "|"
                    + Double.doubleToLongBits(minimumLeaveOneOutSupport)
                    + "|"
                    + Double.doubleToLongBits(ablationFragility)
                    + "|"
                    + evidenceReady
                    + "|"
                    + status
                    + "|"
                    + horizon24.fingerprint
                    + "|"
                    + horizon48.fingerprint
                    + "|"
                    + horizon96.fingerprint
                    + "|"
                    + horizon240.fingerprint;
        }
    }

    public static final class B06SimilarEvidence {
        public String contractVersion = B06_CONTRACT_VERSION;
        public String owner = B06_OWNER;
        public String baseIssue = "";
        public String targetIssue = "";
        public int position = 0;
        public int k3Mask = 0;
        public String shapeKey = "";
        public int sample = 0;
        public int rawSample = 0;
        public double hitRate = AppConfig.RANDOM_K3_BASELINE;
        public double recentHitRate = AppConfig.RANDOM_K3_BASELINE;
        public double avgDistance = 100.0d;
        public double support = 0.0d;
        public double counterEvidence = 1.0d;
        public double risk = 1.0d;
        public double confidence = 0.0d;
        public double freshness = 0.0d;
        public double diversity = 0.0d;
        public int evidenceReady = 0;
        public int advisoryAllowed = 0;
        public String ablationGroup = "B06_SIMILAR_WINDOW";
        public String reason = "";
        public String fingerprint = "";

        public String stablePayload() {
            return contractVersion
                    + "|"
                    + owner
                    + "|"
                    + baseIssue
                    + "|"
                    + targetIssue
                    + "|"
                    + position
                    + "|"
                    + k3Mask
                    + "|"
                    + shapeKey
                    + "|"
                    + sample
                    + "|"
                    + rawSample
                    + "|"
                    + Double.doubleToLongBits(hitRate)
                    + "|"
                    + Double.doubleToLongBits(recentHitRate)
                    + "|"
                    + Double.doubleToLongBits(avgDistance)
                    + "|"
                    + Double.doubleToLongBits(support)
                    + "|"
                    + Double.doubleToLongBits(counterEvidence)
                    + "|"
                    + Double.doubleToLongBits(risk)
                    + "|"
                    + Double.doubleToLongBits(confidence)
                    + "|"
                    + Double.doubleToLongBits(freshness)
                    + "|"
                    + Double.doubleToLongBits(diversity)
                    + "|"
                    + evidenceReady
                    + "|"
                    + advisoryAllowed
                    + "|"
                    + ablationGroup;
        }
    }
}
