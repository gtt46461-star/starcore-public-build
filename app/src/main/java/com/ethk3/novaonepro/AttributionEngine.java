package com.ethk3.novaonepro;

import java.util.ArrayList;
import java.util.List;

public final class AttributionEngine {
    public static final String B06_CONTRACT_VERSION =
            "STARCORE_TIANSHU_B06_ATTRIBUTION_DIAGNOSIS_V1";
    public static final String B06_OWNER = "ATTRIBUTION_DIAGNOSIS_EVIDENCE_ONLY";

    public static final String MODULE_UNKNOWN = "UNKNOWN";
    public static final String MODULE_POSITION = "POSITION";
    public static final String MODULE_NUMBER = "NUMBER";
    public static final String MODULE_COMBO = "COMBO";
    public static final String MODULE_PROBABILITY = "PROBABILITY";
    public static final String MODULE_CALIBRATION = "CALIBRATION";
    public static final String MODULE_TREND = "TREND";
    public static final String MODULE_DRIFT = "DRIFT";
    public static final String MODULE_BREAK = "BREAK";
    public static final String MODULE_RECOVERY = "RECOVERY";
    public static final String MODULE_CYCLE = "CYCLE";
    public static final String MODULE_TRANSITION = "TRANSITION";
    public static final String MODULE_EVIDENCE = "EVIDENCE";
    public static final String MODULE_AI = "AI_JUDGE";
    public static final String MODULE_PROFILE = "PROFILE";
    public static final String MODULE_A_PLUS = "A_PLUS";

    public static final String REASON_HIT_STRONG_PROBABILITY = "HIT_STRONG_PROBABILITY";
    public static final String REASON_HIT_COMBO_EDGE = "HIT_COMBO_EDGE";
    public static final String REASON_HIT_TREND_OK = "HIT_TREND_OK";
    public static final String REASON_HIT_RECOVERY_OK = "HIT_RECOVERY_OK";
    public static final String REASON_HIT_EVIDENCE_RESONANCE = "HIT_EVIDENCE_RESONANCE";
    public static final String REASON_HIT_AI_OK = "HIT_AI_OK";
    public static final String REASON_HIT_A_PLUS = "HIT_A_PLUS";

    public static final String REASON_MISS_POSITION_LOST = "MISS_POSITION_LOST";
    public static final String REASON_MISS_NUMBER_LAYER = "MISS_NUMBER_LAYER";
    public static final String REASON_MISS_DRIFT_HIGH = "MISS_DRIFT_HIGH";
    public static final String REASON_MISS_BREAK_HIGH = "MISS_BREAK_HIGH";
    public static final String REASON_MISS_HOT_REVERSE = "MISS_HOT_REVERSE";
    public static final String REASON_MISS_FALSE_STRONG = "MISS_FALSE_STRONG";
    public static final String REASON_MISS_SAMPLE_WEAK = "MISS_SAMPLE_WEAK";
    public static final String REASON_MISS_CALIBRATION_OVER = "MISS_CALIBRATION_OVER";
    public static final String REASON_MISS_AI_CONFLICT = "MISS_AI_CONFLICT";
    public static final String REASON_MISS_EVIDENCE_WEAK = "MISS_EVIDENCE_WEAK";
    public static final String REASON_MISS_UNKNOWN = "MISS_UNKNOWN";

    private static final double STRONG_PROBABILITY = 0.410d;
    private static final double HIGH_CALIBRATION_ERROR = 0.090d;
    private static final double HARD_CALIBRATION_ERROR = 0.120d;

    public AttributionEngine() {}

    public Models.AttributionResult analyzeTrade(Models.Trade trade, Models.Draw actualDraw) {
        AttributionInput input = fromTrade(trade, actualDraw);
        return analyze(input);
    }

    public Models.AttributionResult analyzeDecision(
            Models.Decision decision, Models.Draw actualDraw) {
        AttributionInput input = fromDecision(decision, actualDraw);
        return analyze(input);
    }

    public Models.AttributionResult analyzeCandidate(
            Models.ComboCandidate candidate, Models.Draw actualDraw) {
        AttributionInput input = fromCandidate(candidate, actualDraw);
        return analyze(input);
    }

    public Models.AttributionResult analyze(AttributionInput input) {
        Models.AttributionResult r = new Models.AttributionResult();
        r.createdAt = System.currentTimeMillis();

        if (input == null) {
            r.mainReasonCode = REASON_MISS_UNKNOWN;
            r.mainReasonText = "归因失败：输入为空";
            r.affectedModule = MODULE_UNKNOWN;
            r.confidence = 0.0d;
            r.shouldReward = 0;
            r.shouldPunish = 0;
            r.learningText = r.mainReasonText;
            return r;
        }

        r.targetIssue = safe(input.targetIssue);
        r.profileKey = safe(input.profileKey);

        AttributionSignal signal = buildSignal(input);

        r.hit = signal.hit;
        r.mainReasonCode = signal.mainReasonCode;
        r.mainReasonText = signal.mainReasonText;
        r.confidence = signal.confidence;
        r.shouldReward = signal.shouldReward;
        r.shouldPunish = signal.shouldPunish;
        r.affectedModule = signal.affectedModule;
        r.positionImpact = signal.positionImpact;
        r.numberImpact = signal.numberImpact;
        r.cycleImpact = signal.cycleImpact;
        r.transitionImpact = signal.transitionImpact;
        r.calibrationImpact = signal.calibrationImpact;
        r.attributionId =
                V51ChecksumEngine.sha256(
                        B06_CONTRACT_VERSION
                                + "|"
                                + r.targetIssue
                                + "|"
                                + r.mainReasonCode
                                + "|"
                                + r.affectedModule
                                + "|"
                                + Double.doubleToLongBits(r.confidence));
        r.learningText =
                signal.learningText + "；B06归因所有权=EvidenceOnly，归因hash=" + shortHash(r.attributionId);

        return r;
    }

    public Models.LearningSample buildLearningSample(Models.Trade trade, Models.Draw actualDraw) {
        AttributionInput input = fromTrade(trade, actualDraw);
        Models.AttributionResult attribution = analyze(input);
        return buildLearningSample(input, attribution);
    }

    public Models.LearningSample buildLearningSample(
            Models.Decision decision, Models.Draw actualDraw) {
        AttributionInput input = fromDecision(decision, actualDraw);
        Models.AttributionResult attribution = analyze(input);
        return buildLearningSample(input, attribution);
    }

    public Models.LearningSample buildLearningSample(
            Models.ComboCandidate candidate, Models.Draw actualDraw) {
        AttributionInput input = fromCandidate(candidate, actualDraw);
        Models.AttributionResult attribution = analyze(input);
        return buildLearningSample(input, attribution);
    }

    public Models.LearningSample buildLearningSample(
            AttributionInput input, Models.AttributionResult attribution) {
        Models.LearningSample s = new Models.LearningSample();

        if (input == null) {
            return s;
        }

        s.baseIssue = safe(input.baseIssue);
        s.targetIssue = safe(input.targetIssue);
        s.position = input.position;
        s.k3Mask = input.k3Mask;
        s.shapeKey = safe(input.shapeKey);
        s.stateMode = safe(input.stateMode);
        s.hit = input.hit;
        s.actualNumber = input.actualNumber;
        s.actualRank = actualRankInCandidate(input.actualNumber, input);
        s.finalP = input.finalP;
        s.rawP = input.rawP;
        s.trendLife = input.trendLife;
        s.driftRisk = input.driftRisk;
        s.breakDepth = input.breakDepth;
        s.recoveryScore = input.recoveryScore;
        s.championGap = input.championGap;
        s.comboGap = input.comboGap;
        s.createdAt = System.currentTimeMillis();

        if (attribution != null) {
            s.missReason = attribution.mainReasonCode;
            s.attributionReasonCode = attribution.mainReasonCode;
            s.affectedModule = attribution.affectedModule;
        } else {
            s.missReason = input.hit == 1 ? "" : REASON_MISS_UNKNOWN;
            s.attributionReasonCode = s.missReason;
            s.affectedModule = MODULE_UNKNOWN;
        }

        s.statScope = safe(input.statScope);
        s.deepProfileKey = safe(input.profileKey);

        return s;
    }

    public void applyAttributionToTrade(Models.Trade trade, Models.AttributionResult attribution) {
        if (trade == null || attribution == null) {
            return;
        }

        trade.attributionReasonCode = safe(attribution.mainReasonCode);
        trade.attributionText = safe(attribution.mainReasonText);
        trade.affectedModule = safe(attribution.affectedModule);

        if (trade.hit == 0 && (trade.missReason == null || trade.missReason.length() == 0)) {
            trade.missReason = safe(attribution.mainReasonCode);
        }
    }

    public void applyAttributionToDecision(
            Models.Decision decision, Models.AttributionResult attribution) {
        if (decision == null || attribution == null) {
            return;
        }

        String text =
                "归因："
                        + safe(attribution.mainReasonText)
                        + "，模块="
                        + safe(attribution.affectedModule)
                        + "，置信="
                        + UiFormatter.probability(attribution.confidence);

        decision.learningSummary = append(decision.learningSummary, text);

        if (attribution.hit == 0
                && attribution.mainReasonCode != null
                && attribution.mainReasonCode.length() > 0) {
            decision.selfDiagnosisText =
                    append(decision.selfDiagnosisText, "未中归因：" + attribution.mainReasonCode);
        }
    }

    public ArrayList<Models.AttributionResult> analyzeTrades(
            List<Models.Trade> trades, Models.Draw actualDraw) {
        ArrayList<Models.AttributionResult> out = new ArrayList<Models.AttributionResult>();

        if (trades == null || trades.size() == 0) {
            return out;
        }

        for (int i = 0; i < trades.size(); i++) {
            Models.Trade t = trades.get(i);
            if (t == null) {
                continue;
            }

            out.add(analyzeTrade(t, actualDraw));
        }

        return out;
    }

    public String buildSummary(Models.AttributionResult r) {
        if (r == null) {
            return "归因：暂无";
        }

        StringBuilder sb = new StringBuilder();

        sb.append("归因：");

        if (r.hit == 1) {
            sb.append("命中");
        } else {
            sb.append("未中");
        }

        sb.append("，主因=");
        sb.append(safe(r.mainReasonText));

        sb.append("，模块=");
        sb.append(safe(r.affectedModule));

        sb.append("，置信=");
        sb.append(UiFormatter.probability(r.confidence));

        if (r.shouldReward == 1) {
            sb.append("，建议奖励模块");
        }

        if (r.shouldPunish == 1) {
            sb.append("，建议惩罚模块");
        }

        return sb.toString();
    }

    public String buildSummaryList(List<Models.AttributionResult> list) {
        if (list == null || list.size() == 0) {
            return "归因：暂无结果";
        }

        StringBuilder sb = new StringBuilder();

        int limit = Math.min(list.size(), 5);

        for (int i = 0; i < limit; i++) {
            Models.AttributionResult r = list.get(i);
            if (r == null) {
                continue;
            }

            if (sb.length() > 0) {
                sb.append(" | ");
            }

            sb.append(r.hit == 1 ? "中" : "未中");
            sb.append(":");
            sb.append(safe(r.mainReasonCode));
            sb.append("/");
            sb.append(safe(r.affectedModule));
        }

        return sb.length() == 0 ? "归因：暂无有效结果" : sb.toString();
    }

    private AttributionInput fromTrade(Models.Trade t, Models.Draw d) {
        AttributionInput input = new AttributionInput();

        if (t == null) {
            input.invalidReason = "Trade为空";
            return input;
        }

        input.baseIssue = safe(t.baseIssue);
        input.targetIssue = safe(t.targetIssue);
        input.position = t.position;
        input.n1 = t.n1;
        input.n2 = t.n2;
        input.n3 = t.n3;
        input.k3Mask = t.k3Mask;

        input.rawP = t.rawP;
        input.calibratedP = t.calibratedP;
        input.finalP = t.finalP;
        input.dynamicGate = t.dynamicGate;
        input.edge = t.edge;

        input.modelP = t.modelP;
        input.shapeP = t.shapeP;
        input.comboP = t.comboP;
        input.recoveryP = t.recoveryP;
        input.similarP = t.similarP;
        input.memoryP = t.memoryP;
        input.calibrationError = t.calibrationError;

        input.similarHitRate = t.similarHitRate;
        input.comboHitRate = t.comboHitRate;
        input.effectiveSample = t.effectiveSample;
        input.comboSample = t.comboSample;

        input.trendLife = t.trendLife;
        input.driftRisk = t.driftRisk;
        input.breakDepth = t.breakDepth;
        input.recoveryScore = t.recoveryScore;
        input.championGap = t.championGap;
        input.comboGap = t.comboGap;
        input.hotReverseRisk = t.hotReverseRisk;
        input.falseStrongRisk = t.falseStrongRisk;

        input.stateMode = safe(t.stateMode);
        input.shapeKey = safe(t.shapeKey);

        input.deepScore = t.deepScore;
        input.deepConfidence = t.deepConfidence;
        input.aiJudgeScore = t.aiJudgeScore;
        input.aiJudgeConfidence = t.aiJudgeConfidence;
        input.evidenceScore = t.evidenceScore;
        input.evidenceRisk = t.evidenceRisk;
        input.biasScore = t.biasScore;
        input.auditPenalty = t.auditPenalty;
        input.profileScore = t.profileScore;
        input.aPlusScore = t.aPlusScore;

        input.profileKey = safe(t.deepProfileKey);
        input.statScope = safe(t.statScope);

        fillActual(input, d);

        if (t.result != null && AppConfig.RESULT_WIN.equals(t.result)) {
            input.hit = 1;
        } else if (t.result != null && AppConfig.RESULT_MISS.equals(t.result)) {
            input.hit = 0;
        } else {
            input.hit = Models.maskContains(input.k3Mask, input.actualNumber) ? 1 : 0;
        }

        return input;
    }

    private AttributionInput fromDecision(Models.Decision d, Models.Draw actualDraw) {
        AttributionInput input = new AttributionInput();

        if (d == null) {
            input.invalidReason = "Decision为空";
            return input;
        }

        input.baseIssue = safe(d.baseIssue);
        input.targetIssue = safe(d.targetIssue);
        input.position = d.position;
        input.n1 = d.n1;
        input.n2 = d.n2;
        input.n3 = d.n3;
        input.k3Mask = d.k3Mask;

        input.rawP = d.rawP;
        input.calibratedP = d.calibratedP;
        input.finalP = d.finalP;
        input.dynamicGate = d.dynamicGate;
        input.edge = d.edge;

        input.modelP = d.modelP;
        input.shapeP = d.shapeP;
        input.comboP = d.comboP;
        input.recoveryP = d.recoveryP;
        input.similarP = d.similarP;
        input.memoryP = d.memoryP;
        input.calibrationError = d.calibrationError;

        input.similarHitRate = d.similarHitRate;
        input.comboHitRate = d.comboHitRate;
        input.effectiveSample = d.effectiveSample;
        input.comboSample = d.comboSample;

        input.trendLife = d.trendLife;
        input.driftRisk = d.driftRisk;
        input.breakDepth = d.breakDepth;
        input.recoveryScore = d.recoveryScore;
        input.championGap = d.championGap;
        input.comboGap = d.comboGap;
        input.hotReverseRisk = d.hotReverseRisk;
        input.falseStrongRisk = d.falseStrongRisk;

        input.stateMode = safe(d.stateMode);
        input.shapeKey = safe(d.shapeKey);

        input.deepScore = d.deepScore;
        input.deepConfidence = d.deepConfidence;
        input.aiJudgeScore = d.aiJudgeScore;
        input.aiJudgeConfidence = d.aiJudgeConfidence;
        input.evidenceScore = d.evidenceScore;
        input.evidenceRisk = d.evidenceRisk;
        input.biasScore = d.biasScore;
        input.auditPenalty = d.auditPenalty;
        input.profileScore = d.profileScore;
        input.aPlusScore = d.aPlusScore;

        input.profileKey = safe(d.deepProfileKey);
        input.statScope = safe(d.statScope);

        fillActual(input, actualDraw);

        input.hit = Models.maskContains(input.k3Mask, input.actualNumber) ? 1 : 0;

        return input;
    }

    private AttributionInput fromCandidate(Models.ComboCandidate c, Models.Draw actualDraw) {
        AttributionInput input = new AttributionInput();

        if (c == null) {
            input.invalidReason = "ComboCandidate为空";
            return input;
        }

        input.baseIssue = safe(c.baseIssue);
        input.targetIssue = safe(c.targetIssue);
        input.position = c.position;
        input.n1 = c.n1;
        input.n2 = c.n2;
        input.n3 = c.n3;
        input.k3Mask = c.k3Mask;

        input.rawP = c.rawP;
        input.calibratedP = c.calibratedP;
        input.finalP = c.finalP;
        input.dynamicGate = c.dynamicGate;
        input.edge = c.edge;

        input.modelP = c.modelP;
        input.shapeP = c.shapeP;
        input.comboP = c.comboP;
        input.recoveryP = c.recoveryP;
        input.similarP = c.similarP;
        input.memoryP = c.memoryP;
        input.calibrationError = c.calibrationError;

        input.similarHitRate = c.similarHitRate;
        input.comboHitRate = c.comboHitRate;
        input.effectiveSample = c.effectiveSample;
        input.comboSample = c.comboSample;

        input.trendLife = c.trendLife;
        input.driftRisk = c.driftRisk;
        input.breakDepth = c.breakDepth;
        input.recoveryScore = c.recoveryScore;
        input.championGap = c.championGap;
        input.comboGap = c.comboGap;
        input.hotReverseRisk = c.hotReverseRisk;
        input.falseStrongRisk = c.falseStrongRisk;

        input.stateMode = safe(c.stateMode);
        input.shapeKey = safe(c.shapeKey);

        input.deepScore = c.deepScore;
        input.deepConfidence = c.deepConfidence;
        input.aiJudgeScore = c.aiJudgeScore;
        input.aiJudgeConfidence = c.aiJudgeConfidence;
        input.evidenceScore = c.evidenceScore;
        input.evidenceRisk = c.evidenceRisk;
        input.biasScore = c.biasScore;
        input.auditPenalty = c.auditPenalty;
        input.profileScore = c.profileScore;
        input.aPlusScore = c.aPlusScore;

        input.profileKey = safe(c.deepProfileKey);
        input.statScope = AppConfig.STAT_SCOPE_SHADOW;

        fillActual(input, actualDraw);

        input.hit = Models.maskContains(input.k3Mask, input.actualNumber) ? 1 : 0;

        return input;
    }

    private void fillActual(AttributionInput input, Models.Draw actualDraw) {
        if (input == null) {
            return;
        }

        if (actualDraw == null || !actualDraw.isValid()) {
            input.actualIssue = "";
            input.actualNumber = 0;
            input.actualValid = 0;
            return;
        }

        input.actualIssue = safe(actualDraw.issue);
        input.actualNumber = actualDraw.getNumberAtPosition(input.position);
        input.actualValid = AppConfig.isValidNumber(input.actualNumber) ? 1 : 0;
    }

    private AttributionSignal buildSignal(AttributionInput input) {
        AttributionSignal s = new AttributionSignal();

        if (input == null) {
            s.hit = 0;
            s.mainReasonCode = REASON_MISS_UNKNOWN;
            s.mainReasonText = "归因失败：输入为空";
            s.affectedModule = MODULE_UNKNOWN;
            return s;
        }

        if (!isInputValid(input)) {
            s.hit = 0;
            s.mainReasonCode = REASON_MISS_UNKNOWN;
            s.mainReasonText = "归因失败：" + safe(input.invalidReason);
            s.affectedModule = MODULE_UNKNOWN;
            s.confidence = 0.0d;
            s.shouldReward = 0;
            s.shouldPunish = 0;
            s.learningText = s.mainReasonText;
            return s;
        }

        s.hit = input.hit;

        s.positionImpact = positionImpact(input);
        s.numberImpact = numberImpact(input);
        s.cycleImpact = cycleImpact(input);
        s.transitionImpact = transitionImpact(input);
        s.calibrationImpact = calibrationImpact(input);

        if (input.hit == 1) {
            fillHitSignal(input, s);
        } else {
            fillMissSignal(input, s);
        }

        s.learningText = buildLearningText(input, s);

        return s;
    }

    private boolean isInputValid(AttributionInput input) {
        if (input == null) {
            return false;
        }

        if (input.invalidReason != null && input.invalidReason.length() > 0) {
            return false;
        }

        if (!AppConfig.isValidPosition(input.position)) {
            input.invalidReason = "位置非法";
            return false;
        }

        if (!Models.isValidK3Mask(input.k3Mask)) {
            input.invalidReason = "K3号码非法";
            return false;
        }

        if (input.actualValid != 1) {
            input.invalidReason = "实际开奖号码无效";
            return false;
        }

        return true;
    }

    private void fillHitSignal(AttributionInput input, AttributionSignal s) {
        s.shouldReward = 1;
        s.shouldPunish = 0;

        HitScore hit = scoreHitReasons(input);

        s.mainReasonCode = hit.reasonCode;
        s.mainReasonText = hit.reasonText;
        s.affectedModule = hit.module;
        s.confidence = hit.confidence;
    }

    private void fillMissSignal(AttributionInput input, AttributionSignal s) {
        s.shouldReward = 0;
        s.shouldPunish = 1;

        MissScore miss = scoreMissReasons(input);

        s.mainReasonCode = miss.reasonCode;
        s.mainReasonText = miss.reasonText;
        s.affectedModule = miss.module;
        s.confidence = miss.confidence;
    }

    private HitScore scoreHitReasons(AttributionInput input) {
        ArrayList<HitScore> list = new ArrayList<HitScore>();

        HitScore p = new HitScore();
        p.reasonCode = REASON_HIT_STRONG_PROBABILITY;
        p.reasonText = "概率判断有效，FinalP超过门槛后命中";
        p.module = MODULE_PROBABILITY;
        p.score = clamp100(input.finalP * 100.0d + Math.max(0.0d, input.edge) * 120.0d);
        p.confidence = clamp01((p.score + sampleConfidence(input)) / 200.0d);
        list.add(p);

        HitScore combo = new HitScore();
        combo.reasonCode = REASON_HIT_COMBO_EDGE;
        combo.reasonText = "K3组合优势有效，实际号码落入候选组合";
        combo.module = MODULE_COMBO;
        combo.score =
                clamp100(
                        input.comboGap * 100.0d * 0.55d
                                + input.comboHitRate * 100.0d * 0.25d
                                + input.championGap * 100.0d * 0.20d);
        combo.confidence = clamp01((combo.score + sampleConfidence(input)) / 200.0d);
        list.add(combo);

        HitScore trend = new HitScore();
        trend.reasonCode = REASON_HIT_TREND_OK;
        trend.reasonText = "趋势状态没有破坏，位置走势延续后命中";
        trend.module = MODULE_TREND;
        trend.score =
                clamp100(
                        input.trendLife * 0.42d
                                + Math.max(0.0d, 100.0d - input.driftRisk) * 0.28d
                                + Math.max(0.0d, 100.0d - input.breakDepth) * 0.20d
                                + input.recoveryScore * 0.10d);
        trend.confidence = clamp01(trend.score / 100.0d);
        list.add(trend);

        HitScore recovery = new HitScore();
        recovery.reasonCode = REASON_HIT_RECOVERY_OK;
        recovery.reasonText = "回补/恢复信号有效，号码落入恢复区";
        recovery.module = MODULE_RECOVERY;
        recovery.score =
                clamp100(
                        input.recoveryScore * 0.70d
                                + Math.max(0.0d, 100.0d - input.breakDepth) * 0.18d
                                + Math.max(0.0d, 100.0d - input.falseStrongRisk) * 0.12d);
        recovery.confidence = clamp01(recovery.score / 100.0d);
        list.add(recovery);

        HitScore evidence = new HitScore();
        evidence.reasonCode = REASON_HIT_EVIDENCE_RESONANCE;
        evidence.reasonText = "多证据共振有效，证据分支持后命中";
        evidence.module = MODULE_EVIDENCE;
        evidence.score =
                clamp100(
                        input.evidenceScore * 100.0d * 0.64d
                                + Math.max(0.0d, 1.0d - input.evidenceRisk) * 100.0d * 0.26d
                                + input.biasScore * 100.0d * 0.10d);
        evidence.confidence = clamp01(evidence.score / 100.0d);
        list.add(evidence);

        HitScore ai = new HitScore();
        ai.reasonCode = REASON_HIT_AI_OK;
        ai.reasonText = "AI裁决有效，AI判断方向与开奖结果一致";
        ai.module = MODULE_AI;
        ai.score =
                clamp100(
                        input.aiJudgeScore * 100.0d * 0.56d
                                + input.aiJudgeConfidence * 100.0d * 0.34d
                                + Math.max(0.0d, 1.0d - input.evidenceRisk) * 100.0d * 0.10d);
        ai.confidence = clamp01(ai.score / 100.0d);
        list.add(ai);

        HitScore aplus = new HitScore();
        aplus.reasonCode = REASON_HIT_A_PLUS;
        aplus.reasonText = "A+强候选有效，强证据候选命中";
        aplus.module = MODULE_A_PLUS;
        aplus.score =
                clamp100(
                        input.aPlusScore * 100.0d * 0.68d
                                + input.deepScore * 100.0d * 0.18d
                                + input.profileScore * 100.0d * 0.14d);
        aplus.confidence = clamp01(aplus.score / 100.0d);
        list.add(aplus);

        return bestHit(list);
    }

    private MissScore scoreMissReasons(AttributionInput input) {
        ArrayList<MissScore> list = new ArrayList<MissScore>();

        MissScore position = new MissScore();
        position.reasonCode = REASON_MISS_POSITION_LOST;
        position.reasonText = "位置判断失效，目标位置没有落入候选号码";
        position.module = MODULE_POSITION;
        position.score =
                clamp100(
                        58.0d
                                + lowScorePenalty(input.trendLife, 45.0d)
                                + highScorePenalty(input.driftRisk, 55.0d)
                                + highScorePenalty(input.breakDepth, 55.0d));
        position.confidence = clamp01(position.score / 100.0d);
        list.add(position);

        MissScore numberLayer = new MissScore();
        numberLayer.reasonCode = REASON_MISS_NUMBER_LAYER;
        numberLayer.reasonText = "号码层判断偏差，实际号码落在K3之外";
        numberLayer.module = MODULE_NUMBER;
        numberLayer.score =
                clamp100(
                        54.0d
                                + lowScorePenalty(input.comboGap * 100.0d, 8.0d)
                                + lowScorePenalty(input.championGap * 100.0d, 8.0d)
                                + lowScorePenalty(input.comboHitRate * 100.0d, 32.0d));
        numberLayer.confidence = clamp01(numberLayer.score / 100.0d);
        list.add(numberLayer);

        MissScore drift = new MissScore();
        drift.reasonCode = REASON_MISS_DRIFT_HIGH;
        drift.reasonText = "分布漂移过高，短窗热区和真实开奖发生偏移";
        drift.module = MODULE_DRIFT;
        drift.score = clamp100(input.driftRisk);
        drift.confidence = clamp01((drift.score + highRiskConfidence(input.driftRisk)) / 200.0d);
        list.add(drift);

        MissScore breakRisk = new MissScore();
        breakRisk.reasonCode = REASON_MISS_BREAK_HIGH;
        breakRisk.reasonText = "破位深度过高，原有走势结构被打断";
        breakRisk.module = MODULE_BREAK;
        breakRisk.score = clamp100(input.breakDepth);
        breakRisk.confidence =
                clamp01((breakRisk.score + highRiskConfidence(input.breakDepth)) / 200.0d);
        list.add(breakRisk);

        MissScore hotReverse = new MissScore();
        hotReverse.reasonCode = REASON_MISS_HOT_REVERSE;
        hotReverse.reasonText = "热号反杀，候选热度过强后出现反向开奖";
        hotReverse.module = MODULE_TREND;
        hotReverse.score = clamp100(input.hotReverseRisk);
        hotReverse.confidence = clamp01(hotReverse.score / 100.0d);
        list.add(hotReverse);

        MissScore falseStrong = new MissScore();
        falseStrong.reasonCode = REASON_MISS_FALSE_STRONG;
        falseStrong.reasonText = "假强形态，表面证据强但实际延续不足";
        falseStrong.module = MODULE_EVIDENCE;
        falseStrong.score =
                clamp100(input.falseStrongRisk * 0.70d + input.evidenceRisk * 100.0d * 0.30d);
        falseStrong.confidence = clamp01(falseStrong.score / 100.0d);
        list.add(falseStrong);

        MissScore sample = new MissScore();
        sample.reasonCode = REASON_MISS_SAMPLE_WEAK;
        sample.reasonText = "样本支撑不足，候选优势可能不稳定";
        sample.module = MODULE_PROFILE;
        sample.score = sampleWeakScore(input);
        sample.confidence = clamp01(sample.score / 100.0d);
        list.add(sample);

        MissScore calibration = new MissScore();
        calibration.reasonCode = REASON_MISS_CALIBRATION_OVER;
        calibration.reasonText = "概率校准偏高，FinalP可能被高估";
        calibration.module = MODULE_CALIBRATION;
        calibration.score = calibrationMissScore(input);
        calibration.confidence = clamp01(calibration.score / 100.0d);
        list.add(calibration);

        MissScore ai = new MissScore();
        ai.reasonCode = REASON_MISS_AI_CONFLICT;
        ai.reasonText = "AI裁决与风险证据冲突，判断分可能虚高";
        ai.module = MODULE_AI;
        ai.score = aiConflictScore(input);
        ai.confidence = clamp01(ai.score / 100.0d);
        list.add(ai);

        MissScore evidence = new MissScore();
        evidence.reasonCode = REASON_MISS_EVIDENCE_WEAK;
        evidence.reasonText = "证据共振不足，候选支持力度不够";
        evidence.module = MODULE_EVIDENCE;
        evidence.score = evidenceWeakScore(input);
        evidence.confidence = clamp01(evidence.score / 100.0d);
        list.add(evidence);

        return bestMiss(list);
    }

    private HitScore bestHit(ArrayList<HitScore> list) {
        HitScore best = null;

        if (list == null || list.size() == 0) {
            best = new HitScore();
            best.reasonCode = REASON_HIT_STRONG_PROBABILITY;
            best.reasonText = "命中，但归因证据不足";
            best.module = MODULE_UNKNOWN;
            best.score = 30.0d;
            best.confidence = 0.30d;
            return best;
        }

        for (int i = 0; i < list.size(); i++) {
            HitScore s = list.get(i);
            if (s == null) {
                continue;
            }

            if (best == null || s.score > best.score) {
                best = s;
            }
        }

        if (best == null) {
            best = new HitScore();
            best.reasonCode = REASON_HIT_STRONG_PROBABILITY;
            best.reasonText = "命中，但归因证据不足";
            best.module = MODULE_UNKNOWN;
            best.score = 30.0d;
            best.confidence = 0.30d;
        }

        return best;
    }

    private MissScore bestMiss(ArrayList<MissScore> list) {
        MissScore best = null;

        if (list == null || list.size() == 0) {
            best = new MissScore();
            best.reasonCode = REASON_MISS_UNKNOWN;
            best.reasonText = "未中，但没有明显错因";
            best.module = MODULE_UNKNOWN;
            best.score = 30.0d;
            best.confidence = 0.30d;
            return best;
        }

        for (int i = 0; i < list.size(); i++) {
            MissScore s = list.get(i);
            if (s == null) {
                continue;
            }

            if (best == null || s.score > best.score) {
                best = s;
            }
        }

        if (best == null) {
            best = new MissScore();
            best.reasonCode = REASON_MISS_UNKNOWN;
            best.reasonText = "未中，但没有明显错因";
            best.module = MODULE_UNKNOWN;
            best.score = 30.0d;
            best.confidence = 0.30d;
        }

        return best;
    }

    private int actualRankInCandidate(int actualNumber, AttributionInput input) {
        if (input == null || !AppConfig.isValidNumber(actualNumber)) {
            return 0;
        }

        if (actualNumber == input.n1) {
            return 1;
        }

        if (actualNumber == input.n2) {
            return 2;
        }

        if (actualNumber == input.n3) {
            return 3;
        }

        return 4;
    }

    private double positionImpact(AttributionInput input) {
        if (input == null) {
            return 0.0d;
        }

        double score = 0.0d;

        score += input.trendLife * 0.32d;
        score += Math.max(0.0d, 100.0d - input.driftRisk) * 0.24d;
        score += Math.max(0.0d, 100.0d - input.breakDepth) * 0.18d;
        score += input.recoveryScore * 0.14d;
        score += input.shapeP * 100.0d * 0.12d;

        return clamp01(score / 100.0d);
    }

    private double numberImpact(AttributionInput input) {
        if (input == null) {
            return 0.0d;
        }

        double score = 0.0d;

        score += input.comboP * 100.0d * 0.30d;
        score += input.comboHitRate * 100.0d * 0.22d;
        score += input.comboGap * 100.0d * 0.20d;
        score += input.championGap * 100.0d * 0.14d;
        score += input.similarP * 100.0d * 0.14d;

        return clamp01(score / 100.0d);
    }

    private double cycleImpact(AttributionInput input) {
        if (input == null) {
            return 0.0d;
        }

        double score = 0.0d;

        score += input.recoveryScore * 0.44d;
        score += Math.max(0.0d, 100.0d - input.breakDepth) * 0.20d;
        score += input.deepScore * 100.0d * 0.20d;
        score += input.biasScore * 100.0d * 0.16d;

        return clamp01(score / 100.0d);
    }

    private double transitionImpact(AttributionInput input) {
        if (input == null) {
            return 0.0d;
        }

        double score = 0.0d;

        score += input.modelP * 100.0d * 0.34d;
        score += input.memoryP * 100.0d * 0.22d;
        score += input.similarP * 100.0d * 0.18d;
        score += input.deepConfidence * 100.0d * 0.14d;
        score += input.aiJudgeConfidence * 100.0d * 0.12d;

        return clamp01(score / 100.0d);
    }

    private double calibrationImpact(AttributionInput input) {
        if (input == null) {
            return 0.0d;
        }

        double score = 100.0d - Math.min(100.0d, input.calibrationError * 1000.0d);
        score += input.calibratedP * 100.0d * 0.12d;

        return clamp01(clamp100(score) / 100.0d);
    }

    private double sampleConfidence(AttributionInput input) {
        if (input == null) {
            return 0.0d;
        }

        double score = 0.0d;

        score +=
                Math.min(
                                1.0d,
                                ((double) input.effectiveSample)
                                        / ((double) AppConfig.EFFECTIVE_SAMPLE_HIGH))
                        * 50.0d;
        score +=
                Math.min(
                                1.0d,
                                ((double) input.comboSample)
                                        / ((double) AppConfig.COMBO_SAMPLE_HIGH))
                        * 50.0d;

        return clamp100(score);
    }

    private double highRiskConfidence(double risk) {
        if (risk >= 85.0d) {
            return 96.0d;
        }

        if (risk >= 75.0d) {
            return 86.0d;
        }

        if (risk >= 65.0d) {
            return 76.0d;
        }

        if (risk >= 55.0d) {
            return 62.0d;
        }

        return 40.0d;
    }

    private double sampleWeakScore(AttributionInput input) {
        if (input == null) {
            return 0.0d;
        }

        double score = 0.0d;

        if (input.effectiveSample < AppConfig.EFFECTIVE_SAMPLE_MIN) {
            score += 42.0d;
        } else if (input.effectiveSample < AppConfig.EFFECTIVE_SAMPLE_HIGH) {
            score += 20.0d;
        }

        if (input.comboSample < AppConfig.COMBO_SAMPLE_MIN) {
            score += 36.0d;
        } else if (input.comboSample < AppConfig.COMBO_SAMPLE_HIGH) {
            score += 16.0d;
        }

        if (input.similarHitRate < 0.300d && input.similarHitRate > 0.0d) {
            score += 10.0d;
        }

        return clamp100(score);
    }

    private double calibrationMissScore(AttributionInput input) {
        if (input == null) {
            return 0.0d;
        }

        double score = 0.0d;

        if (input.calibrationError >= HARD_CALIBRATION_ERROR) {
            score += 78.0d;
        } else if (input.calibrationError >= HIGH_CALIBRATION_ERROR) {
            score += 62.0d;
        } else {
            score += input.calibrationError * 520.0d;
        }

        if (input.finalP >= STRONG_PROBABILITY && input.hit == 0) {
            score += 8.0d;
        }

        if (input.calibratedP > input.rawP + 0.025d && input.hit == 0) {
            score += 6.0d;
        }

        return clamp100(score);
    }

    private double aiConflictScore(AttributionInput input) {
        if (input == null) {
            return 0.0d;
        }

        double score = 0.0d;

        if (input.aiJudgeScore >= AppConfig.AI_JUDGE_SCORE_MIN
                && input.aiJudgeConfidence >= AppConfig.AI_JUDGE_CONFIDENCE_MIN
                && input.hit == 0) {
            score += 42.0d;
        }

        if (input.evidenceRisk >= 0.620d) {
            score += input.evidenceRisk * 42.0d;
        }

        if (input.auditPenalty >= 0.100d) {
            score += input.auditPenalty * 100.0d * 0.28d;
        }

        return clamp100(score);
    }

    private double evidenceWeakScore(AttributionInput input) {
        if (input == null) {
            return 0.0d;
        }

        double score = 0.0d;

        if (input.evidenceScore < 0.420d) {
            score += 44.0d;
        } else if (input.evidenceScore < 0.520d) {
            score += 24.0d;
        }

        if (input.evidenceRisk > 0.560d) {
            score += input.evidenceRisk * 34.0d;
        }

        if (input.biasScore < 0.380d && input.biasScore > 0.0d) {
            score += 8.0d;
        }

        return clamp100(score);
    }

    private double lowScorePenalty(double value, double threshold) {
        if (value >= threshold) {
            return 0.0d;
        }

        return Math.min(24.0d, threshold - value);
    }

    private double highScorePenalty(double value, double threshold) {
        if (value <= threshold) {
            return 0.0d;
        }

        return Math.min(24.0d, value - threshold);
    }

    private String buildLearningText(AttributionInput input, AttributionSignal signal) {
        if (input == null || signal == null) {
            return "";
        }

        StringBuilder sb = new StringBuilder();

        if (signal.hit == 1) {
            sb.append("命中学习：奖励 ");
            sb.append(signal.affectedModule);
            sb.append("，原因=");
            sb.append(signal.mainReasonCode);
            sb.append("，实际号=");
            sb.append(UiFormatter.number(input.actualNumber));
        } else {
            sb.append("未中学习：惩罚 ");
            sb.append(signal.affectedModule);
            sb.append("，原因=");
            sb.append(signal.mainReasonCode);
            sb.append("，实际号=");
            sb.append(UiFormatter.number(input.actualNumber));
            sb.append(" 不在 ");
            sb.append(UiFormatter.positionAndNumbers(input.position, input.n1, input.n2, input.n3));
        }

        sb.append("，FinalP=");
        sb.append(UiFormatter.probability(input.finalP));

        sb.append("，Gate=");
        sb.append(UiFormatter.probability(input.dynamicGate));

        sb.append("，漂移=");
        sb.append(UiFormatter.score(input.driftRisk));

        sb.append("，破位=");
        sb.append(UiFormatter.score(input.breakDepth));

        return sb.toString();
    }

    private double clamp01(double value) {
        if (value < 0.0d) {
            return 0.0d;
        }

        if (value > 1.0d) {
            return 1.0d;
        }

        return value;
    }

    private double clamp100(double value) {
        if (value < 0.0d) {
            return 0.0d;
        }

        if (value > 100.0d) {
            return 100.0d;
        }

        return value;
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

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String shortHash(String value) {
        String text = safe(value);
        return text.length() <= 12 ? text : text.substring(0, 12);
    }

    public static final class AttributionInput {
        public String invalidReason;

        public String baseIssue;
        public String targetIssue;
        public String actualIssue;

        public int position;
        public int n1;
        public int n2;
        public int n3;
        public int k3Mask;

        public int actualNumber;
        public int actualValid;
        public int hit;

        public double rawP;
        public double calibratedP;
        public double finalP;
        public double dynamicGate;
        public double edge;

        public double modelP;
        public double shapeP;
        public double comboP;
        public double recoveryP;

        public double similarP;
        public double memoryP;
        public double calibrationError;

        public double similarHitRate;
        public double comboHitRate;
        public int effectiveSample;
        public int comboSample;

        public double trendLife;
        public double driftRisk;
        public double breakDepth;
        public double recoveryScore;
        public double championGap;
        public double comboGap;
        public double hotReverseRisk;
        public double falseStrongRisk;

        public String stateMode;
        public String shapeKey;

        public double deepScore;
        public double deepConfidence;
        public double aiJudgeScore;
        public double aiJudgeConfidence;
        public double evidenceScore;
        public double evidenceRisk;
        public double biasScore;
        public double auditPenalty;
        public double profileScore;
        public double aPlusScore;

        public String profileKey;
        public String statScope;

        public AttributionInput() {
            invalidReason = "";

            baseIssue = "";
            targetIssue = "";
            actualIssue = "";

            position = 0;
            n1 = 0;
            n2 = 0;
            n3 = 0;
            k3Mask = 0;

            actualNumber = 0;
            actualValid = 0;
            hit = 0;

            rawP = 0.0d;
            calibratedP = 0.0d;
            finalP = 0.0d;
            dynamicGate = 0.0d;
            edge = 0.0d;

            modelP = 0.0d;
            shapeP = 0.0d;
            comboP = 0.0d;
            recoveryP = 0.0d;

            similarP = 0.0d;
            memoryP = 0.0d;
            calibrationError = 0.0d;

            similarHitRate = 0.0d;
            comboHitRate = 0.0d;
            effectiveSample = 0;
            comboSample = 0;

            trendLife = 0.0d;
            driftRisk = 0.0d;
            breakDepth = 0.0d;
            recoveryScore = 0.0d;
            championGap = 0.0d;
            comboGap = 0.0d;
            hotReverseRisk = 0.0d;
            falseStrongRisk = 0.0d;

            stateMode = "";
            shapeKey = "";

            deepScore = 0.0d;
            deepConfidence = 0.0d;
            aiJudgeScore = 0.0d;
            aiJudgeConfidence = 0.0d;
            evidenceScore = 0.0d;
            evidenceRisk = 0.0d;
            biasScore = 0.0d;
            auditPenalty = 0.0d;
            profileScore = 0.0d;
            aPlusScore = 0.0d;

            profileKey = "";
            statScope = "";
        }
    }

    private static final class AttributionSignal {
        public int hit;

        public String mainReasonCode;
        public String mainReasonText;
        public String affectedModule;

        public double confidence;

        public int shouldReward;
        public int shouldPunish;

        public double positionImpact;
        public double numberImpact;
        public double cycleImpact;
        public double transitionImpact;
        public double calibrationImpact;

        public String learningText;

        public AttributionSignal() {
            hit = 0;

            mainReasonCode = REASON_MISS_UNKNOWN;
            mainReasonText = "";
            affectedModule = MODULE_UNKNOWN;

            confidence = 0.0d;

            shouldReward = 0;
            shouldPunish = 0;

            positionImpact = 0.0d;
            numberImpact = 0.0d;
            cycleImpact = 0.0d;
            transitionImpact = 0.0d;
            calibrationImpact = 0.0d;

            learningText = "";
        }
    }

    private static final class HitScore {
        public String reasonCode;
        public String reasonText;
        public String module;
        public double score;
        public double confidence;

        public HitScore() {
            reasonCode = "";
            reasonText = "";
            module = MODULE_UNKNOWN;
            score = 0.0d;
            confidence = 0.0d;
        }
    }

    private static final class MissScore {
        public String reasonCode;
        public String reasonText;
        public String module;
        public double score;
        public double confidence;

        public MissScore() {
            reasonCode = "";
            reasonText = "";
            module = MODULE_UNKNOWN;
            score = 0.0d;
            confidence = 0.0d;
        }
    }

    public B06DiagnosticReport diagnoseSystem(B06DiagnosticInput in) {
        B06DiagnosticInput x = in == null ? new B06DiagnosticInput() : in;
        B06DiagnosticReport r = new B06DiagnosticReport();
        r.settledRealSample = Math.max(0, x.settledRealSample);
        r.settledRealHit = Math.max(0, Math.min(x.settledRealHit, r.settledRealSample));
        r.shadowSample = Math.max(0, x.shadowSample);
        r.shadowHit = Math.max(0, Math.min(x.shadowHit, r.shadowSample));
        r.eligibleIssues = Math.max(0, x.eligibleIssues);
        r.realBetIssues = Math.max(0, Math.min(x.realBetIssues, r.eligibleIssues));
        r.realHitRate = rate(r.settledRealHit, r.settledRealSample);
        r.shadowHitRate = rate(r.shadowHit, r.shadowSample);
        r.frequency = rate(r.realBetIssues, r.eligibleIssues);
        r.wilsonLower95 = wilsonLower95(r.settledRealHit, r.settledRealSample);
        r.ece = clamp01(x.ece);
        r.maxLoseStreak = Math.max(0, x.maxLoseStreak);
        r.backgroundHealthy = x.backgroundHealthy;
        r.predictionHealthy = x.predictionHealthy;
        r.databaseHealthy = x.databaseHealthy;
        r.apiHealthy = x.apiHealthy;
        r.uiSmooth = x.uiSmooth;
        r.uniqueChainHealthy = x.uniqueChainHealthy;
        r.ownerViolationCount = Math.max(0, x.ownerViolationCount);
        r.futureLeakCount = Math.max(0, x.futureLeakCount);
        r.shadowRealContaminationCount = Math.max(0, x.shadowRealContaminationCount);
        r.pass39 = proofPass(r, 0.39d, 0.15d) ? 1 : 0;
        r.pass40 = proofPass(r, 0.40d, 0.15d) ? 1 : 0;
        r.pass50 = proofPass(r, 0.50d, 0.15d) ? 1 : 0;
        r.pass60 = proofPass(r, 0.60d, 0.15d) ? 1 : 0;
        r.frequency15 = r.frequency >= 0.15d ? 1 : 0;
        r.frequency20 = r.frequency >= 0.20d ? 1 : 0;
        r.frequency30 = r.frequency >= 0.30d ? 1 : 0;
        r.frequency40 = r.frequency >= 0.40d ? 1 : 0;
        r.modelProofStatus = r.pass40 == 1 ? "PROVEN_AT_40_15" : "NOT_PROVEN";
        r.overallStatus = overallStatus(r);
        r.primaryCause = primaryCause(x, r);
        r.secondaryCause = secondaryCause(x, r);
        r.requiredOptimization = requiredOptimization(x, r);
        r.chineseReport = buildChineseDiagnostic(r);
        r.fingerprint = V51ChecksumEngine.sha256(r.stablePayload());
        return r;
    }

    private boolean proofPass(B06DiagnosticReport r, double hit, double freq) {
        return r != null
                && r.settledRealSample >= 120
                && r.wilsonLower95 >= hit
                && r.frequency >= freq
                && r.ece <= 0.04d
                && r.uniqueChainHealthy == 1
                && r.ownerViolationCount == 0
                && r.futureLeakCount == 0
                && r.shadowRealContaminationCount == 0
                && r.backgroundHealthy == 1
                && r.predictionHealthy == 1
                && r.databaseHealthy == 1
                && r.apiHealthy == 1;
    }

    private String overallStatus(B06DiagnosticReport r) {
        if (r.ownerViolationCount > 0
                || r.futureLeakCount > 0
                || r.shadowRealContaminationCount > 0) return "CHAIN_CONTRACT_FAILED";
        if (r.backgroundHealthy == 0
                || r.predictionHealthy == 0
                || r.databaseHealthy == 0
                || r.apiHealthy == 0) return "RUNTIME_FAILED";
        if (r.settledRealSample < 120) return "EVIDENCE_INSUFFICIENT";
        if (r.pass40 == 1) return "TARGET_40_15_PROVEN";
        return r.frequency < 0.15d ? "FREQUENCY_BELOW_15" : "HIT_RATE_NOT_PROVEN";
    }

    private String primaryCause(B06DiagnosticInput x, B06DiagnosticReport r) {
        if (r.ownerViolationCount > 0) return "唯一主链被旁路：存在非DecisionGate概率/门槛/BET所有者";
        if (r.shadowRealContaminationCount > 0) return "SHADOW/HOLD污染真实校准或真实统计";
        if (r.futureLeakCount > 0) return "时点边界失败：存在未来数据读取";
        if (r.backgroundHealthy == 0) return "后台调度或Watchdog失活，导致抓取/预测链中断";
        if (r.predictionHealthy == 0) return "实时预测链未持续产出，需检查调度、超时、OOM与异常隔离";
        if (r.databaseHealthy == 0) return "数据库迁移/锁/写入/结算账本异常";
        if (r.apiHealthy == 0) return "主备接口、限流、解析或脏数据隔离异常";
        if (r.settledRealSample < 120) return "严格已结算真实样本不足，无法证明胜率目标";
        if (r.wilsonLower95 < 0.40d) return weakestModule(x) + "证据贡献不足或反证过强，40%下界未通过";
        if (r.frequency < 0.15d) return "选择性动作覆盖不足，且禁止用频率债务硬补单";
        if (r.ece > 0.04d) return "概率校准误差过高，FinalP不可作为可靠出手证据";
        return "未发现单一故障，需继续严格OOS与消融验证";
    }

    private String secondaryCause(B06DiagnosticInput x, B06DiagnosticReport r) {
        if (r.frequency < 0.20d) return "频率尚未达到20%；先扩大被严格OOS证明的可行动状态域，而不是降低安全门";
        if (r.uiSmooth == 0) return "界面滑动不流畅：主线程绑定/格式化/查询/超长文本渲染或列表未分页";
        if (x.ablationFragility > 0.25d) return "模块消融脆弱，多个相关模块重复计权，泛化稳定性不足";
        if (x.driftRisk > 0.55d) return "近期分布漂移高，历史优势不能直接外推";
        return x.calibrationSample < 80 ? "校准样本不足，概率可信边界宽" : "需要扩大四块时间冻结验证并持续观察最长连挂";
    }

    private String requiredOptimization(B06DiagnosticInput x, B06DiagnosticReport r) {
        StringBuilder s = new StringBuilder();
        if (r.ownerViolationCount > 0 || r.shadowRealContaminationCount > 0)
            s.append("先修所有权与统计隔离；");
        if (r.backgroundHealthy == 0 || r.predictionHealthy == 0)
            s.append("加强前台服务、开机恢复、Alarm/Job双Watchdog、租约心跳、超时降级和OOM隔离；");
        if (r.wilsonLower95 < 0.40d) s.append("对位置/号码/第三号/状态/相似窗口/记忆/校准逐模块做PIT消融，冻结负贡献模块；");
        if (r.frequency < 0.15d) s.append("只扩展被证明的状态原型与受控Probe域，禁止降低门槛硬凑频率；");
        if (r.frequency >= 0.15d && r.frequency < 0.30d)
            s.append("若要20%–30%，需要更多互补低相关专家、域证书和跨块一致性；");
        if (r.pass50 == 0) s.append("50%/60%不能靠单次调参承诺，必须有更强信息增益、长期真实证据和显著Wilson下界；");
        if (r.ece > 0.04d) s.append("重做PIT分层校准并限制小样本概率抬升；");
        if (r.uiSmooth == 0) s.append("数据库查询和报告生成移出主线程，分页/差量刷新并限制TextView长度；");
        return s.length() == 0 ? "继续严格OOS、真实影子和漂移监控，不做未经证据的胜率声明；" : s.toString();
    }

    private String weakestModule(B06DiagnosticInput x) {
        double min = x.positionModuleScore;
        String m = MODULE_POSITION;
        if (x.numberModuleScore < min) {
            min = x.numberModuleScore;
            m = MODULE_NUMBER;
        }
        if (x.comboModuleScore < min) {
            min = x.comboModuleScore;
            m = MODULE_COMBO;
        }
        if (x.transitionModuleScore < min) {
            min = x.transitionModuleScore;
            m = MODULE_TRANSITION;
        }
        if (x.similarModuleScore < min) {
            min = x.similarModuleScore;
            m = "SIMILAR";
        }
        if (x.memoryModuleScore < min) {
            min = x.memoryModuleScore;
            m = "MEMORY";
        }
        if (x.calibrationModuleScore < min) m = MODULE_CALIBRATION;
        return m;
    }

    private String buildChineseDiagnostic(B06DiagnosticReport r) {
        StringBuilder s = new StringBuilder("【星核天枢B06动态检测报告】\n");
        s.append("模型证据=")
                .append(r.modelProofStatus)
                .append("；总体状态=")
                .append(r.overallStatus)
                .append('\n');
        s.append("严格真实已结算=")
                .append(r.settledRealHit)
                .append('/')
                .append(r.settledRealSample)
                .append("，命中率=")
                .append(UiFormatter.probability(r.realHitRate))
                .append("，95% Wilson下界=")
                .append(UiFormatter.probability(r.wilsonLower95))
                .append('\n');
        s.append("真实频率=")
                .append(r.realBetIssues)
                .append('/')
                .append(r.eligibleIssues)
                .append('=')
                .append(UiFormatter.probability(r.frequency))
                .append("；ECE=")
                .append(UiFormatter.probability(r.ece))
                .append("；最长连挂=")
                .append(r.maxLoseStreak)
                .append('\n');
        s.append("39/40/50/60%门=")
                .append(r.pass39)
                .append('/')
                .append(r.pass40)
                .append('/')
                .append(r.pass50)
                .append('/')
                .append(r.pass60)
                .append("；频率15/20/30/40=")
                .append(r.frequency15)
                .append('/')
                .append(r.frequency20)
                .append('/')
                .append(r.frequency30)
                .append('/')
                .append(r.frequency40)
                .append('\n');
        s.append("主因=")
                .append(r.primaryCause)
                .append("\n次因=")
                .append(r.secondaryCause)
                .append("\n优化施工=")
                .append(r.requiredOptimization)
                .append('\n');
        s.append(
                "红线：未经严格PIT/OOS、真实影子、统计隔离和多块一致性证明，不得宣称40%、50%或60%已达到。\n"
                    + "B06所有权：形态/相似/记忆/关系/归因只输出Evidence；DecisionGate是唯一真实BET出口。");
        return s.toString();
    }

    private double rate(int hit, int sample) {
        return sample <= 0 ? 0.0d : clamp01(hit / (double) sample);
    }

    private double wilsonLower95(int hit, int sample) {
        if (sample <= 0) return 0.0d;
        double z = 1.959963984540054d, n = sample, p = hit / n, z2 = z * z;
        return clamp01(
                (p + z2 / (2 * n) - z * Math.sqrt((p * (1 - p) + z2 / (4 * n)) / n))
                        / (1 + z2 / n));
    }

    public static final class B06DiagnosticInput {
        public int settledRealSample,
                settledRealHit,
                shadowSample,
                shadowHit,
                eligibleIssues,
                realBetIssues,
                maxLoseStreak;
        public int backgroundHealthy,
                predictionHealthy,
                databaseHealthy,
                apiHealthy,
                uiSmooth,
                uniqueChainHealthy;
        public int ownerViolationCount,
                futureLeakCount,
                shadowRealContaminationCount,
                calibrationSample;
        public double ece,
                positionModuleScore,
                numberModuleScore,
                comboModuleScore,
                transitionModuleScore;
        public double similarModuleScore,
                memoryModuleScore,
                calibrationModuleScore,
                ablationFragility,
                driftRisk;
    }

    public static final class B06DiagnosticReport {
        public String contractVersion = B06_CONTRACT_VERSION,
                owner = B06_OWNER,
                overallStatus = "UNKNOWN",
                modelProofStatus = "NOT_PROVEN";
        public int settledRealSample,
                settledRealHit,
                shadowSample,
                shadowHit,
                eligibleIssues,
                realBetIssues,
                maxLoseStreak;
        public int backgroundHealthy,
                predictionHealthy,
                databaseHealthy,
                apiHealthy,
                uiSmooth,
                uniqueChainHealthy;
        public int ownerViolationCount,
                futureLeakCount,
                shadowRealContaminationCount,
                pass39,
                pass40,
                pass50,
                pass60;
        public int frequency15, frequency20, frequency30, frequency40;
        public double realHitRate, shadowHitRate, frequency, wilsonLower95, ece;
        public String primaryCause = "",
                secondaryCause = "",
                requiredOptimization = "",
                chineseReport = "",
                fingerprint = "";

        public String stablePayload() {
            return contractVersion
                    + "|"
                    + owner
                    + "|"
                    + overallStatus
                    + "|"
                    + modelProofStatus
                    + "|"
                    + settledRealSample
                    + "|"
                    + settledRealHit
                    + "|"
                    + shadowSample
                    + "|"
                    + shadowHit
                    + "|"
                    + eligibleIssues
                    + "|"
                    + realBetIssues
                    + "|"
                    + Double.doubleToLongBits(realHitRate)
                    + "|"
                    + Double.doubleToLongBits(frequency)
                    + "|"
                    + Double.doubleToLongBits(wilsonLower95)
                    + "|"
                    + Double.doubleToLongBits(ece)
                    + "|"
                    + maxLoseStreak
                    + "|"
                    + backgroundHealthy
                    + "|"
                    + predictionHealthy
                    + "|"
                    + databaseHealthy
                    + "|"
                    + apiHealthy
                    + "|"
                    + uiSmooth
                    + "|"
                    + uniqueChainHealthy
                    + "|"
                    + ownerViolationCount
                    + "|"
                    + futureLeakCount
                    + "|"
                    + shadowRealContaminationCount
                    + "|"
                    + pass39
                    + "|"
                    + pass40
                    + "|"
                    + pass50
                    + "|"
                    + pass60
                    + "|"
                    + frequency15
                    + "|"
                    + frequency20
                    + "|"
                    + frequency30
                    + "|"
                    + frequency40
                    + "|"
                    + primaryCause
                    + "|"
                    + secondaryCause
                    + "|"
                    + requiredOptimization;
        }
    }
}
