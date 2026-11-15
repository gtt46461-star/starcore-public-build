package com.ethk3.novaonepro;

import java.util.ArrayList;
import java.util.List;

public final class ShapeMemoryEngine {
    public static final String B06_CONTRACT_VERSION =
            "STARCORE_TIANSHU_B06_SHAPE_MEMORY_EVIDENCE_V1";
    public static final String B06_OWNER = "SHAPE_MEMORY_EVIDENCE_ONLY";

    public ShapeMemoryEngine() {}

    public MemorySignal evaluate(Models.ShapeVector vector, List<Models.Draw> history) {
        MemorySignal signal = new MemorySignal();

        if (vector == null) {
            signal.position = 0;
            signal.shapeKey = "";
            signal.usable = 0;
            signal.memoryScore = 0.0d;
            signal.probabilityAdjust = -0.012d;
            signal.gateAdjust = 0.018d;
            attachB06Evidence(signal, "", "");
            signal.detail = "形态为空，记忆层保守处理；" + signal.b06Summary;
            return signal;
        }

        signal.position = vector.position;
        signal.shapeKey = vector.shapeKey == null ? "" : vector.shapeKey;
        signal.stateMode = vector.stateMode == null ? AppConfig.MODE_CHAOS : vector.stateMode;

        int topMask = buildTopK3Mask(vector);
        signal.topMask = topMask;

        if (!Models.isValidK3Mask(topMask)) {
            signal.usable = 0;
            signal.memoryScore = 35.0d;
            signal.probabilityAdjust = -0.008d;
            signal.gateAdjust = 0.012d;
            attachB06Evidence(signal, vector.baseIssue, vector.targetIssue);
            signal.detail = "记忆层无法形成有效K3，保守观察；" + signal.b06Summary;
            return signal;
        }

        signal.hitRate64 =
                ShapeMath.historicalK3HitRate(
                        history, vector.position, topMask, AppConfig.WINDOW_64);
        signal.hitRate128 =
                ShapeMath.historicalK3HitRate(
                        history, vector.position, topMask, AppConfig.WINDOW_128);
        signal.hitRate240 =
                ShapeMath.historicalK3HitRate(
                        history, vector.position, topMask, AppConfig.WINDOW_240);
        signal.hitRate480 =
                ShapeMath.historicalK3HitRate(
                        history, vector.position, topMask, AppConfig.WINDOW_480);
        signal.sample64 = Math.min(size(history), AppConfig.WINDOW_64);
        signal.sample128 = Math.min(size(history), AppConfig.WINDOW_128);
        signal.sample240 = Math.min(size(history), AppConfig.WINDOW_240);
        signal.sample480 = Math.min(size(history), AppConfig.WINDOW_480);

        signal.recentMemoryRate = weightedMemoryRate(signal);
        signal.memoryScore = calcMemoryScore(vector, signal);
        signal.usable = signal.sample128 >= 80 ? 1 : 0;

        calcAdjustments(vector, signal);
        attachB06Evidence(signal, vector.baseIssue, vector.targetIssue);
        signal.detail = buildDetail(signal) + "；" + signal.b06Summary;

        return signal;
    }

    public MemorySignal evaluateCandidate(
            Models.ComboCandidate candidate, List<Models.Draw> history) {
        MemorySignal signal = new MemorySignal();

        if (candidate == null || !candidate.isValidK3()) {
            signal.usable = 0;
            signal.memoryScore = 0.0d;
            signal.probabilityAdjust = -0.012d;
            signal.gateAdjust = 0.016d;
            attachB06Evidence(signal, "", "");
            signal.detail = "候选为空或K3非法，记忆层保护；" + signal.b06Summary;
            return signal;
        }

        signal.position = candidate.position;
        signal.shapeKey = candidate.shapeKey == null ? "" : candidate.shapeKey;
        signal.stateMode = candidate.stateMode == null ? AppConfig.MODE_CHAOS : candidate.stateMode;
        signal.topMask = candidate.k3Mask;

        signal.hitRate64 =
                ShapeMath.historicalK3HitRate(
                        history, candidate.position, candidate.k3Mask, AppConfig.WINDOW_64);
        signal.hitRate128 =
                ShapeMath.historicalK3HitRate(
                        history, candidate.position, candidate.k3Mask, AppConfig.WINDOW_128);
        signal.hitRate240 =
                ShapeMath.historicalK3HitRate(
                        history, candidate.position, candidate.k3Mask, AppConfig.WINDOW_240);
        signal.hitRate480 =
                ShapeMath.historicalK3HitRate(
                        history, candidate.position, candidate.k3Mask, AppConfig.WINDOW_480);
        signal.sample64 = Math.min(size(history), AppConfig.WINDOW_64);
        signal.sample128 = Math.min(size(history), AppConfig.WINDOW_128);
        signal.sample240 = Math.min(size(history), AppConfig.WINDOW_240);
        signal.sample480 = Math.min(size(history), AppConfig.WINDOW_480);

        signal.recentMemoryRate = weightedMemoryRate(signal);
        signal.memoryScore = calcCandidateMemoryScore(candidate, signal);
        signal.usable = signal.sample128 >= 80 ? 1 : 0;

        calcCandidateAdjustments(candidate, signal);
        attachB06Evidence(signal, candidate.baseIssue, candidate.targetIssue);
        signal.detail = buildDetail(signal) + "；" + signal.b06Summary;

        return signal;
    }

    public void enrichCandidate(Models.ComboCandidate candidate, List<Models.Draw> history) {
        if (candidate == null || !candidate.isValidK3()) {
            return;
        }

        MemorySignal signal = evaluateCandidate(candidate, history);
        candidate.memoryP = AppConfig.clampProbability(signal.recentMemoryRate);
        candidate.deepStateScore = blend01(candidate.deepStateScore, signal.b06Support, 0.10d);
        candidate.evidenceScore = blend01(candidate.evidenceScore, signal.b06Support, 0.08d);
        candidate.evidenceRisk = blend01(candidate.evidenceRisk, signal.b06Risk, 0.08d);
        candidate.reasonSummary =
                appendReason(
                        candidate.reasonSummary,
                        "B06形态记忆EvidenceOnly：记忆分="
                                + UiFormatter.score(signal.memoryScore)
                                + "，支持="
                                + UiFormatter.probability(signal.b06Support)
                                + "，反证="
                                + UiFormatter.probability(signal.b06CounterEvidence)
                                + "，风险="
                                + UiFormatter.probability(signal.b06Risk)
                                + "，hash="
                                + shortHash(signal.b06Fingerprint));
    }

    public ArrayList<Models.ComboCandidate> enrichAll(
            List<Models.ComboCandidate> candidates, List<Models.Draw> history) {
        ArrayList<Models.ComboCandidate> out = new ArrayList<Models.ComboCandidate>();

        if (candidates == null) {
            return out;
        }

        for (int i = 0; i < candidates.size(); i++) {
            Models.ComboCandidate c = candidates.get(i);
            if (c == null) {
                continue;
            }

            Models.ComboCandidate copy = c.copy();
            enrichCandidate(copy, history);
            out.add(copy);
        }

        return out;
    }

    public void learnDecision(DbStore store, Models.Decision decision, Models.Draw actualDraw) {
        if (store == null || decision == null || actualDraw == null || !actualDraw.isValid()) {
            return;
        }

        if (!AppConfig.isValidPosition(decision.position)) {
            return;
        }

        int actual = actualDraw.getNumberAtPosition(decision.position);
        if (!AppConfig.isValidNumber(actual)) {
            return;
        }

        boolean hit = Models.maskContains(decision.k3Mask, actual);
        boolean fail = !hit;
        boolean reverse =
                isReverseLike(
                        decision.stateMode,
                        decision.driftRisk,
                        decision.breakDepth,
                        decision.reasonSummary);

        if (decision.shapeKey != null && decision.shapeKey.length() > 0) {
            store.updateShapeMemory(decision.shapeKey, decision.position, hit, fail, reverse);
        }

        if (Models.isValidK3Mask(decision.k3Mask)
                && decision.shapeKey != null
                && decision.shapeKey.length() > 0) {
            store.updateComboMemory(
                    decision.shapeKey,
                    decision.position,
                    decision.k3Mask,
                    hit,
                    decision.comboGap,
                    decision.trendLife);
        }

        /* B06: Decision/HOLD/SHADOW observations never enter strict real calibration. */
    }

    public void learnTrade(DbStore store, Models.Trade trade, Models.Draw actualDraw) {
        if (store == null || trade == null || actualDraw == null || !actualDraw.isValid()) {
            return;
        }

        if (!AppConfig.isValidPosition(trade.position)) {
            return;
        }

        int actual = actualDraw.getNumberAtPosition(trade.position);
        if (!AppConfig.isValidNumber(actual)) {
            return;
        }

        boolean hit = Models.maskContains(trade.k3Mask, actual);
        boolean fail = !hit;
        boolean reverse =
                AppConfig.MISS_HOT_REVERSE.equals(trade.missReason)
                        || AppConfig.MISS_TREND_BROKEN.equals(trade.missReason)
                        || AppConfig.MODE_REVERSING.equals(trade.stateMode)
                        || AppConfig.MODE_BROKEN.equals(trade.stateMode);

        if (trade.shapeKey != null && trade.shapeKey.length() > 0) {
            store.updateShapeMemory(trade.shapeKey, trade.position, hit, fail, reverse);
        }

        if (Models.isValidK3Mask(trade.k3Mask)
                && trade.shapeKey != null
                && trade.shapeKey.length() > 0) {
            store.updateComboMemory(trade.shapeKey, trade.position, trade.k3Mask, hit, 0.0d, 0.0d);
        }

        if (AppConfig.isRealTradeScope(trade.statScope)
                && AppConfig.DECISION_BET.equals(AppConfig.normalizeDecision(trade.action))
                && trade.settled == 1) {
            store.updateCalibration(trade.finalP, hit);
        }
    }

    public void learnShadowByIssue(DbStore store, String targetIssue, Models.Draw actualDraw) {
        if (store == null
                || targetIssue == null
                || targetIssue.length() == 0
                || actualDraw == null
                || !actualDraw.isValid()) {
            return;
        }

        List<Models.ComboCandidate> shadows =
                store.getShadowCandidates(targetIssue, AppConfig.SHADOW_SAVE_TOP_N);

        if (shadows == null || shadows.size() == 0) {
            return;
        }

        for (int i = 0; i < shadows.size(); i++) {
            Models.ComboCandidate c = shadows.get(i);
            if (c == null || !c.isValidK3()) {
                continue;
            }

            int actual = actualDraw.getNumberAtPosition(c.position);
            if (!AppConfig.isValidNumber(actual)) {
                continue;
            }

            boolean hit = Models.maskContains(c.k3Mask, actual);
            boolean fail = !hit;
            boolean reverse =
                    isReverseLike(c.stateMode, c.driftRisk, c.breakDepth, c.reasonSummary);

            if (c.shapeKey != null && c.shapeKey.length() > 0) {
                store.updateShapeMemory(c.shapeKey, c.position, hit, fail, reverse);
                store.updateComboMemory(
                        c.shapeKey, c.position, c.k3Mask, hit, c.comboGap, c.trendLife);
            }

            /* B06: SHADOW memory is isolated from strict real calibration. */
        }

        store.markShadowLearned(targetIssue);
    }

    public void learnHoldObservation(
            DbStore store, Models.Decision holdDecision, Models.Draw actualDraw) {
        if (store == null || holdDecision == null || actualDraw == null || !actualDraw.isValid()) {
            return;
        }

        /*
         * HOLD不进入真实胜率、不进入真实trades。
         * 这里只允许做形态观察校准：如果HOLD时仍然有候选内容，
         * 可以把形态表现写入记忆，但不算真实下注。
         */
        if (!AppConfig.DECISION_HOLD.equals(holdDecision.decision)) {
            return;
        }

        if (!AppConfig.isValidPosition(holdDecision.position)
                || !Models.isValidK3Mask(holdDecision.k3Mask)) {
            return;
        }

        int actual = actualDraw.getNumberAtPosition(holdDecision.position);
        if (!AppConfig.isValidNumber(actual)) {
            return;
        }

        boolean hit = Models.maskContains(holdDecision.k3Mask, actual);
        boolean fail = !hit;
        boolean reverse =
                isReverseLike(
                        holdDecision.stateMode,
                        holdDecision.driftRisk,
                        holdDecision.breakDepth,
                        holdDecision.reasonSummary);

        if (holdDecision.shapeKey != null && holdDecision.shapeKey.length() > 0) {
            store.updateShapeMemory(
                    holdDecision.shapeKey, holdDecision.position, hit, fail, reverse);
            store.updateComboMemory(
                    holdDecision.shapeKey,
                    holdDecision.position,
                    holdDecision.k3Mask,
                    hit,
                    holdDecision.comboGap,
                    holdDecision.trendLife);
        }

        /* B06: HOLD observation is isolated from strict real calibration. */
    }

    public void learnCandidateSet(
            DbStore store, List<Models.ComboCandidate> candidates, Models.Draw actualDraw) {
        if (store == null
                || candidates == null
                || candidates.size() == 0
                || actualDraw == null
                || !actualDraw.isValid()) {
            return;
        }

        int limit = Math.min(candidates.size(), AppConfig.SHADOW_SAVE_TOP_N);

        for (int i = 0; i < limit; i++) {
            Models.ComboCandidate c = candidates.get(i);
            if (c == null || !c.isValidK3()) {
                continue;
            }

            int actual = actualDraw.getNumberAtPosition(c.position);
            if (!AppConfig.isValidNumber(actual)) {
                continue;
            }

            boolean hit = Models.maskContains(c.k3Mask, actual);
            boolean fail = !hit;
            boolean reverse =
                    isReverseLike(c.stateMode, c.driftRisk, c.breakDepth, c.reasonSummary);

            if (c.shapeKey != null && c.shapeKey.length() > 0) {
                store.updateShapeMemory(c.shapeKey, c.position, hit, fail, reverse);
                store.updateComboMemory(
                        c.shapeKey, c.position, c.k3Mask, hit, c.comboGap, c.trendLife);
            }

            /* B06: candidate observation is isolated from strict real calibration. */
        }
    }

    public double memoryGateAdjustment(MemorySignal signal) {
        /* B06 compatibility API only; DecisionGateEngine owns the formal gate. */
        return 0.0d;
    }

    public double memoryProbabilityAdjustment(MemorySignal signal) {
        /* B06 compatibility API only; probability owner consumes evidence explicitly. */
        return 0.0d;
    }

    public boolean memoryAllowsBet(MemorySignal signal) {
        if (signal == null) {
            return false;
        }

        if (signal.usable == 0) {
            return signal.memoryScore >= 52.0d && signal.recentMemoryRate >= 0.345d;
        }

        if (signal.memoryScore < 44.0d) {
            return false;
        }

        if (signal.recentMemoryRate < 0.330d && signal.sample128 >= 120) {
            return false;
        }

        return true;
    }

    public String explain(MemorySignal signal) {
        if (signal == null) {
            return "形态记忆为空";
        }
        return signal.detail == null ? "" : signal.detail;
    }

    private int buildTopK3Mask(Models.ShapeVector vector) {
        if (vector == null || vector.numberProbs == null) {
            return 0;
        }

        int[] top = ShapeMath.topNumbers(vector.numberProbs, AppConfig.FIXED_K);
        if (top == null || top.length < AppConfig.FIXED_K) {
            return 0;
        }

        return Models.buildK3Mask(top[0], top[1], top[2]);
    }

    private double weightedMemoryRate(MemorySignal s) {
        if (s == null) {
            return AppConfig.RANDOM_K3_BASELINE;
        }

        double total = 0.0d;
        double weight = 0.0d;

        if (s.sample64 >= 40) {
            total += s.hitRate64 * 0.34d;
            weight += 0.34d;
        }

        if (s.sample128 >= 80) {
            total += s.hitRate128 * 0.30d;
            weight += 0.30d;
        }

        if (s.sample240 >= 120) {
            total += s.hitRate240 * 0.22d;
            weight += 0.22d;
        }

        if (s.sample480 >= 180) {
            total += s.hitRate480 * 0.14d;
            weight += 0.14d;
        }

        if (weight <= 0.0d) {
            return AppConfig.RANDOM_K3_BASELINE;
        }

        return AppConfig.clampProbability(total / weight);
    }

    private double calcMemoryScore(Models.ShapeVector v, MemorySignal s) {
        if (v == null || s == null) {
            return 0.0d;
        }

        double score = 50.0d;

        score += (s.recentMemoryRate - AppConfig.RANDOM_K3_BASELINE) * 180.0d;
        score += v.trendLife * 0.10d;
        score += v.recoveryScore * 0.08d;
        score += (100.0d - v.driftRisk) * 0.10d;
        score += (100.0d - v.breakDepth) * 0.08d;
        score += v.championGap * 100.0d * 0.06d;
        score += v.comboGap * 100.0d * 0.06d;

        if (s.sample128 < 80) {
            score -= 8.0d;
        }

        if (AppConfig.MODE_BROKEN.equals(v.stateMode)
                || AppConfig.MODE_REVERSING.equals(v.stateMode)
                || AppConfig.MODE_CHAOS.equals(v.stateMode)) {
            score -= 18.0d;
        }

        if (v.hotReverseRisk > AppConfig.HOT_REVERSE_RISK_MAX) {
            score -= (v.hotReverseRisk - AppConfig.HOT_REVERSE_RISK_MAX) * 0.25d;
        }

        if (v.falseStrongRisk > AppConfig.FALSE_STRONG_RISK_MAX) {
            score -= (v.falseStrongRisk - AppConfig.FALSE_STRONG_RISK_MAX) * 0.25d;
        }

        return ShapeMath.clamp100(score);
    }

    private double calcCandidateMemoryScore(Models.ComboCandidate c, MemorySignal s) {
        if (c == null || s == null) {
            return 0.0d;
        }

        double score = 50.0d;

        score += (s.recentMemoryRate - AppConfig.RANDOM_K3_BASELINE) * 190.0d;
        score += c.trendLife * 0.09d;
        score += c.recoveryScore * 0.08d;
        score += (100.0d - c.driftRisk) * 0.10d;
        score += (100.0d - c.breakDepth) * 0.08d;
        score += c.championGap * 100.0d * 0.07d;
        score += c.comboGap * 100.0d * 0.07d;

        if (s.sample128 < 80) {
            score -= 7.0d;
        }

        if (AppConfig.MODE_BROKEN.equals(c.stateMode)
                || AppConfig.MODE_REVERSING.equals(c.stateMode)
                || AppConfig.MODE_CHAOS.equals(c.stateMode)) {
            score -= 18.0d;
        }

        if (c.hotReverseRisk > AppConfig.HOT_REVERSE_RISK_MAX) {
            score -= (c.hotReverseRisk - AppConfig.HOT_REVERSE_RISK_MAX) * 0.25d;
        }

        if (c.falseStrongRisk > AppConfig.FALSE_STRONG_RISK_MAX) {
            score -= (c.falseStrongRisk - AppConfig.FALSE_STRONG_RISK_MAX) * 0.25d;
        }

        return ShapeMath.clamp100(score);
    }

    private void calcAdjustments(Models.ShapeVector v, MemorySignal s) {
        if (v == null || s == null) {
            return;
        }

        calcBaseAdjustments(s);

        if (v.driftRisk > AppConfig.DRIFT_RISK_MAX) {
            s.probabilityAdjust -= 0.004d;
            s.gateAdjust += 0.006d;
        }

        if (v.breakDepth > AppConfig.BREAK_DEPTH_MAX) {
            s.probabilityAdjust -= 0.006d;
            s.gateAdjust += 0.008d;
        }

        normalizeAdjustments(s);
    }

    private void calcCandidateAdjustments(Models.ComboCandidate c, MemorySignal s) {
        if (c == null || s == null) {
            return;
        }

        calcBaseAdjustments(s);

        if (c.driftRisk > AppConfig.DRIFT_RISK_MAX) {
            s.probabilityAdjust -= 0.004d;
            s.gateAdjust += 0.006d;
        }

        if (c.breakDepth > AppConfig.BREAK_DEPTH_MAX) {
            s.probabilityAdjust -= 0.006d;
            s.gateAdjust += 0.008d;
        }

        if (c.comboGap < AppConfig.COMBO_GAP_MIN) {
            s.probabilityAdjust -= 0.004d;
            s.gateAdjust += 0.004d;
        }

        normalizeAdjustments(s);
    }

    private void calcBaseAdjustments(MemorySignal s) {
        if (s == null) {
            return;
        }

        double edge = s.recentMemoryRate - AppConfig.RANDOM_K3_BASELINE;

        if (s.usable == 0) {
            s.probabilityAdjust = -0.004d;
            s.gateAdjust = 0.006d;
            return;
        }

        if (edge >= 0.070d && s.memoryScore >= 62.0d) {
            s.probabilityAdjust = 0.012d;
            s.gateAdjust = -0.004d;
        } else if (edge >= 0.045d && s.memoryScore >= 56.0d) {
            s.probabilityAdjust = 0.007d;
            s.gateAdjust = -0.002d;
        } else if (edge >= 0.020d && s.memoryScore >= 50.0d) {
            s.probabilityAdjust = 0.003d;
            s.gateAdjust = 0.000d;
        } else if (edge < -0.030d || s.memoryScore < 42.0d) {
            s.probabilityAdjust = -0.014d;
            s.gateAdjust = 0.014d;
        } else {
            s.probabilityAdjust = -0.002d;
            s.gateAdjust = 0.004d;
        }
    }

    private void normalizeAdjustments(MemorySignal s) {
        if (s == null) {
            return;
        }

        if (s.probabilityAdjust > 0.014d) {
            s.probabilityAdjust = 0.014d;
        }

        if (s.probabilityAdjust < -0.026d) {
            s.probabilityAdjust = -0.026d;
        }

        if (s.gateAdjust > 0.026d) {
            s.gateAdjust = 0.026d;
        }

        if (s.gateAdjust < -0.006d) {
            s.gateAdjust = -0.006d;
        }
    }

    private boolean isReverseLike(
            String stateMode, double driftRisk, double breakDepth, String text) {
        if (AppConfig.MODE_REVERSING.equals(stateMode)
                || AppConfig.MODE_BROKEN.equals(stateMode)
                || AppConfig.MODE_CHAOS.equals(stateMode)) {
            return true;
        }

        if (driftRisk >= AppConfig.DRIFT_RISK_HARD_BLOCK) {
            return true;
        }

        if (breakDepth >= 55.0d) {
            return true;
        }

        if (text != null) {
            if (text.indexOf("反杀") >= 0 || text.indexOf("反转") >= 0 || text.indexOf("破位") >= 0) {
                return true;
            }
        }

        return false;
    }

    private int size(List<Models.Draw> history) {
        return history == null ? 0 : history.size();
    }

    private String buildDetail(MemorySignal s) {
        if (s == null) {
            return "形态记忆为空";
        }

        StringBuilder sb = new StringBuilder();

        sb.append("P");
        sb.append(s.position);

        sb.append(" 形态记忆=");
        sb.append(UiFormatter.score(s.memoryScore));

        sb.append(" 记忆率=");
        sb.append(UiFormatter.probability(s.recentMemoryRate));

        sb.append(" W64=");
        sb.append(UiFormatter.probability(s.hitRate64));

        sb.append(" W128=");
        sb.append(UiFormatter.probability(s.hitRate128));

        sb.append(" W240=");
        sb.append(UiFormatter.probability(s.hitRate240));

        sb.append(" 样本=");
        sb.append(s.sample128);

        sb.append(" 概率修正=");
        sb.append(UiFormatter.signedProbability(s.probabilityAdjust));

        sb.append(" 门槛修正=");
        sb.append(UiFormatter.signedProbability(s.gateAdjust));

        if (memoryAllowsBet(s)) {
            sb.append(" 结论=记忆允许进入候选");
        } else {
            sb.append(" 结论=记忆偏弱，收紧或HOLD");
        }

        return sb.toString();
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

    public static final class MemorySignal {
        public int position;
        public String shapeKey;
        public String stateMode;
        public int topMask;

        public int sample64;
        public int sample128;
        public int sample240;
        public int sample480;

        public double hitRate64;
        public double hitRate128;
        public double hitRate240;
        public double hitRate480;

        public double recentMemoryRate;
        public double memoryScore;
        public double probabilityAdjust;
        public double gateAdjust;

        public int usable;
        public String detail;
        public double b06Support;
        public double b06CounterEvidence;
        public double b06Risk;
        public double b06Confidence;
        public double b06Freshness;
        public int b06EvidenceReady;
        public String b06Summary;
        public String b06Fingerprint;

        public MemorySignal() {
            position = 0;
            shapeKey = "";
            stateMode = AppConfig.MODE_CHAOS;
            topMask = 0;

            sample64 = 0;
            sample128 = 0;
            sample240 = 0;
            sample480 = 0;

            hitRate64 = 0.0d;
            hitRate128 = 0.0d;
            hitRate240 = 0.0d;
            hitRate480 = 0.0d;

            recentMemoryRate = AppConfig.RANDOM_K3_BASELINE;
            memoryScore = 0.0d;
            probabilityAdjust = -0.008d;
            gateAdjust = 0.012d;

            usable = 0;
            detail = "";
            b06Support = 0.0d;
            b06CounterEvidence = 1.0d;
            b06Risk = 1.0d;
            b06Confidence = 0.0d;
            b06Freshness = 0.0d;
            b06EvidenceReady = 0;
            b06Summary = "";
            b06Fingerprint = "";
        }
    }

    private void attachB06Evidence(MemorySignal signal, String baseIssue, String targetIssue) {
        if (signal == null) return;
        signal.b06Support =
                clamp01(
                        (signal.recentMemoryRate - AppConfig.RANDOM_K3_BASELINE + 0.10d)
                                        / 0.20d
                                        * 0.44d
                                + clamp01(signal.memoryScore / 100.0d) * 0.30d
                                + clamp01(signal.hitRate128) * 0.14d
                                + clamp01(signal.hitRate240) * 0.12d);
        signal.b06CounterEvidence =
                clamp01(
                        Math.max(0.0d, AppConfig.RANDOM_K3_BASELINE - signal.recentMemoryRate)
                                        * 3.0d
                                        * 0.48d
                                + Math.max(0.0d, signal.hitRate240 - signal.hitRate64)
                                        * 2.5d
                                        * 0.22d
                                + (signal.sample128 < 80 ? 0.20d : 0.0d)
                                + (signal.memoryScore < 42.0d ? 0.10d : 0.0d));
        signal.b06Risk =
                clamp01(
                        signal.b06CounterEvidence * 0.62d
                                + (1.0d - clamp01(signal.memoryScore / 100.0d)) * 0.23d
                                + (signal.sample128 < 80 ? 0.15d : 0.0d));
        signal.b06Confidence =
                clamp01(
                        clamp01(signal.sample128 / 240.0d) * 0.36d
                                + clamp01(signal.sample240 / 480.0d) * 0.24d
                                + (1.0d - signal.b06Risk) * 0.25d
                                + clamp01(signal.memoryScore / 100.0d) * 0.15d);
        signal.b06Freshness = clamp01(1.0d - Math.abs(signal.hitRate64 - signal.hitRate128) * 3.0d);
        signal.b06EvidenceReady = signal.sample128 >= 80 && signal.b06Confidence >= 0.40d ? 1 : 0;
        String payload =
                B06_CONTRACT_VERSION
                        + "|"
                        + safeText(baseIssue)
                        + "|"
                        + safeText(targetIssue)
                        + "|"
                        + signal.position
                        + "|"
                        + safeText(signal.shapeKey)
                        + "|"
                        + signal.topMask
                        + "|"
                        + signal.sample64
                        + "|"
                        + signal.sample128
                        + "|"
                        + signal.sample240
                        + "|"
                        + signal.sample480
                        + "|"
                        + Double.doubleToLongBits(signal.recentMemoryRate)
                        + "|"
                        + Double.doubleToLongBits(signal.memoryScore)
                        + "|"
                        + Double.doubleToLongBits(signal.b06Support)
                        + "|"
                        + Double.doubleToLongBits(signal.b06CounterEvidence)
                        + "|"
                        + Double.doubleToLongBits(signal.b06Risk)
                        + "|"
                        + Double.doubleToLongBits(signal.b06Confidence)
                        + "|"
                        + Double.doubleToLongBits(signal.b06Freshness)
                        + "|"
                        + signal.b06EvidenceReady
                        + "|B06_SHAPE_MEMORY";
        signal.b06Fingerprint = V51ChecksumEngine.sha256(payload);
        signal.b06Summary =
                "B06记忆证据：支持="
                        + UiFormatter.probability(signal.b06Support)
                        + "，反证="
                        + UiFormatter.probability(signal.b06CounterEvidence)
                        + "，风险="
                        + UiFormatter.probability(signal.b06Risk)
                        + "，置信="
                        + UiFormatter.probability(signal.b06Confidence)
                        + "，所有权=EvidenceOnly";
    }

    public B06MemoryEvidence buildB06Evidence(
            MemorySignal signal, String baseIssue, String targetIssue) {
        B06MemoryEvidence e = new B06MemoryEvidence();
        if (signal == null) {
            e.fingerprint = V51ChecksumEngine.sha256(e.stablePayload());
            return e;
        }
        if (signal.b06Fingerprint == null || signal.b06Fingerprint.length() == 0) {
            attachB06Evidence(signal, baseIssue, targetIssue);
        }
        e.baseIssue = safeText(baseIssue);
        e.targetIssue = safeText(targetIssue);
        e.position = signal.position;
        e.shapeKey = safeText(signal.shapeKey);
        e.k3Mask = signal.topMask;
        e.sample = signal.sample128;
        e.support = signal.b06Support;
        e.counterEvidence = signal.b06CounterEvidence;
        e.risk = signal.b06Risk;
        e.confidence = signal.b06Confidence;
        e.freshness = signal.b06Freshness;
        e.evidenceReady = signal.b06EvidenceReady;
        e.reason = "形态记忆只输出Evidence；SHADOW/HOLD不进入真实校准";
        e.fingerprint = signal.b06Fingerprint;
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

    private String shortHash(String value) {
        if (value == null) return "";
        return value.length() <= 12 ? value : value.substring(0, 12);
    }

    public B06MemoryStabilityAudit auditB06MemoryStability(
            MemorySignal signal,
            String baseIssue,
            String targetIssue,
            String statScope,
            int settled) {
        B06MemoryStabilityAudit audit = new B06MemoryStabilityAudit();
        audit.baseIssue = safeText(baseIssue);
        audit.targetIssue = safeText(targetIssue);
        audit.statScope = safeText(statScope);
        audit.settled = settled == 1 ? 1 : 0;
        if (signal == null) {
            audit.status = "NO_SIGNAL";
            audit.reason = "记忆信号为空";
            audit.fingerprint = V51ChecksumEngine.sha256(audit.stablePayload());
            return audit;
        }
        audit.position = signal.position;
        audit.shapeKey = safeText(signal.shapeKey);
        audit.k3Mask = signal.topMask;
        audit.sample64 = signal.sample64;
        audit.sample128 = signal.sample128;
        audit.sample240 = signal.sample240;
        audit.sample480 = signal.sample480;
        audit.rate64 = clamp01(signal.hitRate64);
        audit.rate128 = clamp01(signal.hitRate128);
        audit.rate240 = clamp01(signal.hitRate240);
        audit.rate480 = clamp01(signal.hitRate480);
        audit.weightedRate = weightedB06Rate(audit);
        audit.shortLongGap = Math.abs(audit.rate64 - audit.rate480);
        audit.midLongGap = Math.abs(audit.rate128 - audit.rate240);
        audit.rateDispersion =
                memoryB06Dispersion(
                        new double[] {audit.rate64, audit.rate128, audit.rate240, audit.rate480});
        audit.sampleMaturity =
                clamp01(
                        clamp01(audit.sample64 / 64.0d) * 0.18d
                                + clamp01(audit.sample128 / 128.0d) * 0.27d
                                + clamp01(audit.sample240 / 240.0d) * 0.30d
                                + clamp01(audit.sample480 / 480.0d) * 0.25d);
        audit.crossHorizonStability =
                clamp01(
                        1.0d
                                - audit.rateDispersion * 3.0d
                                - audit.shortLongGap * 1.3d
                                - audit.midLongGap * 0.9d);
        audit.recentDecayRisk = clamp01(Math.max(0.0d, audit.rate240 - audit.rate64) * 4.0d);
        audit.recentSpikeRisk =
                clamp01(
                        Math.max(0.0d, audit.rate64 - audit.rate240)
                                * 3.0d
                                * (1.0d - clamp01(audit.sample64 / 64.0d)));
        audit.realCalibrationEligible =
                isB06RealCalibrationScope(audit.statScope, audit.settled) ? 1 : 0;
        audit.shadowOrHoldIsolated =
                audit.realCalibrationEligible == 1 || isB06ShadowOrHoldScope(audit.statScope)
                        ? 1
                        : 0;
        audit.support =
                clamp01(
                        (audit.weightedRate - AppConfig.RANDOM_K3_BASELINE + 0.08d) / 0.18d * 0.52d
                                + audit.crossHorizonStability * 0.24d
                                + audit.sampleMaturity * 0.16d
                                + clamp01(signal.memoryScore / 100.0d) * 0.08d);
        audit.counterEvidence =
                clamp01(
                        Math.max(0.0d, AppConfig.RANDOM_K3_BASELINE - audit.weightedRate)
                                        * 4.0d
                                        * 0.44d
                                + audit.recentDecayRisk * 0.24d
                                + audit.recentSpikeRisk * 0.12d
                                + (1.0d - audit.sampleMaturity) * 0.20d);
        audit.risk =
                clamp01(
                        audit.counterEvidence * 0.52d
                                + (1.0d - audit.crossHorizonStability) * 0.28d
                                + audit.recentDecayRisk * 0.12d
                                + (audit.shadowOrHoldIsolated == 1 ? 0.0d : 0.08d));
        audit.confidence =
                clamp01(
                        audit.sampleMaturity * 0.38d
                                + audit.crossHorizonStability * 0.30d
                                + (1.0d - audit.risk) * 0.22d
                                + clamp01(signal.memoryScore / 100.0d) * 0.10d);
        audit.evidenceReady =
                signal.b06EvidenceReady == 1
                                && audit.sample128 >= 80
                                && audit.confidence >= 0.42d
                                && audit.risk <= 0.62d
                                && audit.shadowOrHoldIsolated == 1
                        ? 1
                        : 0;
        if (audit.shadowOrHoldIsolated == 0) {
            audit.status = "SCOPE_CONTAMINATION";
            audit.reason = "统计口径无法证明SHADOW/HOLD与真实校准隔离";
        } else if (audit.recentDecayRisk >= 0.55d) {
            audit.status = "RECENT_DECAY";
            audit.reason = "近期记忆优势衰减，不能沿用长期胜率";
        } else if (audit.crossHorizonStability < 0.42d) {
            audit.status = "HORIZON_UNSTABLE";
            audit.reason = "64/128/240/480窗口差异过大，记忆证据应冻结或降权";
        } else if (audit.evidenceReady == 1) {
            audit.status = "EVIDENCE_READY";
            audit.reason = "多周期稳定且统计口径隔离；仍不直接改FinalP或BET";
        } else {
            audit.status = "EVIDENCE_INSUFFICIENT";
            audit.reason = "样本、置信或风险未达证据门";
        }
        audit.fingerprint = V51ChecksumEngine.sha256(audit.stablePayload());
        return audit;
    }

    private double weightedB06Rate(B06MemoryStabilityAudit a) {
        double weight = 0.0d;
        double sum = 0.0d;
        if (a.sample64 > 0) {
            double w = Math.min(1.0d, a.sample64 / 64.0d) * 0.34d;
            sum += a.rate64 * w;
            weight += w;
        }
        if (a.sample128 > 0) {
            double w = Math.min(1.0d, a.sample128 / 128.0d) * 0.28d;
            sum += a.rate128 * w;
            weight += w;
        }
        if (a.sample240 > 0) {
            double w = Math.min(1.0d, a.sample240 / 240.0d) * 0.23d;
            sum += a.rate240 * w;
            weight += w;
        }
        if (a.sample480 > 0) {
            double w = Math.min(1.0d, a.sample480 / 480.0d) * 0.15d;
            sum += a.rate480 * w;
            weight += w;
        }
        return weight <= 0.0d ? AppConfig.RANDOM_K3_BASELINE : clamp01(sum / weight);
    }

    private double memoryB06Dispersion(double[] values) {
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

    private boolean isB06RealCalibrationScope(String scope, int settled) {
        String s = safeText(scope).toUpperCase(java.util.Locale.US);
        return settled == 1 && (s.indexOf("REAL_TRADE") >= 0 || s.equals("REAL"));
    }

    private boolean isB06ShadowOrHoldScope(String scope) {
        String s = safeText(scope).toUpperCase(java.util.Locale.US);
        return s.length() == 0
                || s.indexOf("SHADOW") >= 0
                || s.indexOf("HOLD") >= 0
                || s.indexOf("WATCH") >= 0
                || s.indexOf("CANDIDATE") >= 0;
    }

    public static final class B06MemoryStabilityAudit {
        public String contractVersion = B06_CONTRACT_VERSION;
        public String owner = B06_OWNER;
        public String baseIssue = "";
        public String targetIssue = "";
        public String statScope = "";
        public int settled = 0;
        public int position = 0;
        public String shapeKey = "";
        public int k3Mask = 0;
        public int sample64 = 0;
        public int sample128 = 0;
        public int sample240 = 0;
        public int sample480 = 0;
        public double rate64 = AppConfig.RANDOM_K3_BASELINE;
        public double rate128 = AppConfig.RANDOM_K3_BASELINE;
        public double rate240 = AppConfig.RANDOM_K3_BASELINE;
        public double rate480 = AppConfig.RANDOM_K3_BASELINE;
        public double weightedRate = AppConfig.RANDOM_K3_BASELINE;
        public double shortLongGap = 1.0d;
        public double midLongGap = 1.0d;
        public double rateDispersion = 1.0d;
        public double sampleMaturity = 0.0d;
        public double crossHorizonStability = 0.0d;
        public double recentDecayRisk = 1.0d;
        public double recentSpikeRisk = 1.0d;
        public int realCalibrationEligible = 0;
        public int shadowOrHoldIsolated = 0;
        public double support = 0.0d;
        public double counterEvidence = 1.0d;
        public double risk = 1.0d;
        public double confidence = 0.0d;
        public int evidenceReady = 0;
        public String status = "UNKNOWN";
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
                    + statScope
                    + "|"
                    + settled
                    + "|"
                    + position
                    + "|"
                    + shapeKey
                    + "|"
                    + k3Mask
                    + "|"
                    + sample64
                    + "|"
                    + sample128
                    + "|"
                    + sample240
                    + "|"
                    + sample480
                    + "|"
                    + Double.doubleToLongBits(rate64)
                    + "|"
                    + Double.doubleToLongBits(rate128)
                    + "|"
                    + Double.doubleToLongBits(rate240)
                    + "|"
                    + Double.doubleToLongBits(rate480)
                    + "|"
                    + Double.doubleToLongBits(weightedRate)
                    + "|"
                    + Double.doubleToLongBits(rateDispersion)
                    + "|"
                    + Double.doubleToLongBits(crossHorizonStability)
                    + "|"
                    + Double.doubleToLongBits(recentDecayRisk)
                    + "|"
                    + realCalibrationEligible
                    + "|"
                    + shadowOrHoldIsolated
                    + "|"
                    + Double.doubleToLongBits(support)
                    + "|"
                    + Double.doubleToLongBits(counterEvidence)
                    + "|"
                    + Double.doubleToLongBits(risk)
                    + "|"
                    + Double.doubleToLongBits(confidence)
                    + "|"
                    + evidenceReady
                    + "|"
                    + status
                    + "|"
                    + reason;
        }
    }

    public static final class B06MemoryEvidence {
        public String contractVersion = B06_CONTRACT_VERSION;
        public String owner = B06_OWNER;
        public String baseIssue = "";
        public String targetIssue = "";
        public int position = 0;
        public String shapeKey = "";
        public int k3Mask = 0;
        public int sample = 0;
        public double support = 0.0d;
        public double counterEvidence = 1.0d;
        public double risk = 1.0d;
        public double confidence = 0.0d;
        public double freshness = 0.0d;
        public int evidenceReady = 0;
        public String ablationGroup = "B06_SHAPE_MEMORY";
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
                    + shapeKey
                    + "|"
                    + k3Mask
                    + "|"
                    + sample
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
                    + evidenceReady
                    + "|"
                    + ablationGroup;
        }
    }
}
