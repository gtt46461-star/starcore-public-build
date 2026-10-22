package com.ethk3.novaonepro;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * ProbabilityAuditEngine
 *
 * <p>旧版高胜率概率审计恢复版
 *
 * <p>核心原则： 1. 概率审计是风控层，不是增强层。 2. 不修改 rawP。 3. 不修改 finalP。 4. 不降低 dynamicGate。 5. 不产生 BET。 6. 不写
 * auditBoost 救援。 7. 不写 aPlusScore 救援。 8. 不重新洗牌候选主链。 9. 只写 auditPenalty / blockCode / reasonSummary。
 * 10. 最终 BET / SHADOW / HOLD 仍只由 DecisionGateEngine 裁决。 11. Java7 / AIDE 兼容，无 lambda，无
 * stream，无第三方库。
 */
public final class ProbabilityAuditEngine {
    public static final String OUMAN_GALAXY_REBUILD_VERSION =
            AppConfig.OUMAN_GALAXY_REBUILD_VERSION;

    /*
     * 当前真实出手区重点在 0.385~0.420。
     * 原版 0.430 才开始审计太晚。
     */
    private static final double RAW_P_WATCH_HIGH = 0.385d;
    private static final double RAW_P_SOFT_HIGH = 0.400d;
    private static final double RAW_P_HARD_HIGH = 0.430d;
    private static final double RAW_P_EXTREME_HIGH = 0.460d;

    private static final double FINAL_P_WATCH_HIGH = 0.385d;
    private static final double FINAL_P_SOFT_HIGH = 0.400d;
    private static final double FINAL_P_HARD_HIGH = 0.430d;
    private static final double FINAL_P_EXTREME_HIGH = 0.460d;

    private static final double DEFAULT_CALIBRATION_ERROR_WARN = 0.060d;
    private static final double DEFAULT_CALIBRATION_ERROR_HARD = 0.095d;
    private static final double CALIBRATION_ERROR_EXTREME = 0.120d;

    private static final double EDGE_THIN = 0.003d;
    private static final double EDGE_WARN = 0.010d;
    private static final double EDGE_STRONG = 0.020d;

    private static final double AUDIT_USABLE = 54.0d;
    private static final double AUDIT_STRONG = 66.0d;
    private static final double AUDIT_A_PLUS = 76.0d;

    private static final double PENALTY_WEIGHT_NEW = 0.42d;
    private static final double MAX_AUDIT_PENALTY = 0.880d;

    private static final int MAX_REASON_LEN = 1600;

    public ProbabilityAuditEngine() {}

    public AuditResult auditCandidate(Models.ComboCandidate candidate) {
        AuditResult r = new AuditResult();
        r.createdAt = System.currentTimeMillis();

        if (candidate == null) {
            r.status = "INVALID";
            r.blockCode = AppConfig.BLOCK_COMBO_WEAK;
            r.summaryText = "概率审计：候选为空";
            return r;
        }

        normalizeCandidate(candidate);

        r.position = candidate.position;
        r.n1 = candidate.n1;
        r.n2 = candidate.n2;
        r.n3 = candidate.n3;
        r.k3Mask = candidate.k3Mask;

        r.baseIssue = safe(candidate.baseIssue);
        r.targetIssue = safe(candidate.targetIssue);

        r.rawP = candidate.rawP;
        r.calibratedP = candidate.calibratedP;
        r.finalP = candidate.finalP;
        r.dynamicGate = candidate.dynamicGate;
        r.edge = candidate.edge;
        r.calibrationError = candidate.calibrationError;

        r.effectiveSample = candidate.effectiveSample;
        r.comboSample = candidate.comboSample;

        r.driftRisk = candidate.driftRisk;
        r.breakDepth = candidate.breakDepth;
        r.evidenceRisk = candidate.evidenceRisk;
        r.evidenceScore = candidate.evidenceScore;
        r.auditPenaltyInput = candidate.auditPenalty;
        r.auditBoostInput = candidate.auditBoost;

        r.stateMode = safe(candidate.stateMode);
        r.aPlusScore = candidate.aPlusScore;
        r.aiJudgeScore = candidate.aiJudgeScore;
        r.aiJudgeConfidence = candidate.aiJudgeConfidence;

        if (!candidate.isValidK3()) {
            r.status = "INVALID";
            r.blockCode = AppConfig.BLOCK_COMBO_WEAK;
            r.riskScore = 100.0d;
            r.penalty = 1.0d;
            r.summaryText = "概率审计：候选不是单位置固定K3";
            return r;
        }

        r.rawRisk = rawProbabilityRisk(candidate);
        r.calibrationRisk = calibrationRisk(candidate);
        r.finalRisk = finalProbabilityRisk(candidate);
        r.gateRisk = gateRisk(candidate);
        r.edgeRisk = edgeRisk(candidate);
        r.sampleRisk = sampleRisk(candidate);
        r.shapeRisk = shapeRisk(candidate);
        r.evidenceAuditRisk = evidenceRisk(candidate);
        r.overfitRisk = overfitRisk(candidate);

        r.reliabilityScore = reliabilityScore(r, candidate);
        r.riskScore = finalRiskScore(r, candidate);
        r.boost = auditBoost(r, candidate);
        r.penalty = auditPenalty(r, candidate);
        r.status = statusByAudit(r, candidate);
        r.blockCode = blockByAudit(r, candidate);
        r.auditKey = buildAuditKey(r);
        r.summaryText = buildSummary(r);

        return r;
    }

    public ArrayList<AuditResult> auditCandidates(List<Models.ComboCandidate> candidates) {
        ArrayList<AuditResult> out = new ArrayList<AuditResult>();

        if (candidates == null || candidates.size() == 0) {
            return out;
        }

        for (int i = 0; i < candidates.size(); i++) {
            Models.ComboCandidate c = candidates.get(i);

            if (c == null) {
                continue;
            }

            out.add(auditCandidate(c));
        }

        sortAuditResults(out);
        return out;
    }

    public ArrayList<Models.ComboCandidate> applyToCandidates(
            List<Models.ComboCandidate> candidates) {
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
            normalizeCandidate(copy);

            AuditResult r = auditCandidate(copy);
            applyToCandidate(copy, r);

            /*
             * 不重新排序主链，只保持输入顺序。
             * 这样不会让审计模块接管候选池排序。
             */
            out.add(copy);
        }

        rerankCandidatesKeepOrder(out);

        return out;
    }

    public Models.CandidateBatch applyToBatch(Models.CandidateBatch batch) {
        Models.CandidateBatch out = new Models.CandidateBatch();

        if (batch == null) {
            return out;
        }

        out.baseIssue = batch.baseIssue;
        out.targetIssue = batch.targetIssue;
        out.scanMode = batch.scanMode;
        out.probabilityMode = batch.probabilityMode;
        out.fullScan = batch.fullScan;
        out.scannedCount = batch.scannedCount;
        out.enrichCount = batch.enrichCount;
        out.deepCount = batch.deepCount;

        ArrayList<Models.ComboCandidate> list = applyToCandidates(batch.candidates);
        out.addAll(list);

        if (list != null && list.size() > 0) {
            out.best = list.get(0);
        }

        return out;
    }

    public void applyToCandidate(Models.ComboCandidate candidate, AuditResult audit) {
        if (candidate == null) {
            return;
        }

        normalizeCandidate(candidate);

        if (audit == null) {
            candidate.reasonSummary =
                    append(candidate.reasonSummary, "概率审计：暂无，按中性处理，不修改FinalP/RawP/Gate");
            return;
        }

        /*
         * 核心红线：
         * 审计只写风险，不写救援。
         */
        double oldPenalty = AppConfig.clampScore01(candidate.auditPenalty);
        double newPenalty = AppConfig.clampScore01(audit.penalty);

        candidate.auditPenalty = blend01(oldPenalty, newPenalty, PENALTY_WEIGHT_NEW);

        if (audit.riskScore >= 72.0d) {
            candidate.auditPenalty = Math.max(candidate.auditPenalty, newPenalty);
        }

        if (audit.riskScore >= 86.0d) {
            candidate.auditPenalty = Math.max(candidate.auditPenalty, 0.520d);
        }

        if (candidate.auditPenalty > MAX_AUDIT_PENALTY) {
            candidate.auditPenalty = MAX_AUDIT_PENALTY;
        }

        /*
         * 不允许 auditBoost 成为隐形救援。
         * 清掉由本审计层造成的增强倾向。
         */
        candidate.auditBoost = 0.0d;

        /*
         * 不写 aPlusScore。
         * 不写 promotionScore。
         * 不写 evidenceScore。
         * 不改 finalP/rawP/dynamicGate。
         */
        candidate.edge = candidate.finalP - candidate.dynamicGate;

        if (audit.blockCode != null && audit.blockCode.length() > 0) {
            candidate.blockCode = pickBlockCode(candidate.blockCode, audit.blockCode);
        }

        if (audit.riskScore >= 82.0d) {
            candidate.reasonSummary =
                    append(
                            candidate.reasonSummary,
                            "概率审计：高风险候选，已写入auditPenalty与必要blockCode；不修改FinalP/RawP，不产生BET");
        } else if (audit.riskScore >= 72.0d) {
            candidate.reasonSummary =
                    append(candidate.reasonSummary, "概率审计：风险偏高，作为最终门风险参考；不救援、不提分、不改门槛");
        } else if (audit.reliabilityScore >= AUDIT_STRONG && audit.riskScore <= 48.0d) {
            candidate.reasonSummary =
                    append(
                            candidate.reasonSummary,
                            "概率审计：候选可靠性尚可，但本模块不写auditBoost、不写A+，最终仍由DecisionGateEngine裁决");
        }

        candidate.reasonSummary =
                append(
                        candidate.reasonSummary,
                        "概率审计：可靠="
                                + UiFormatter.score(audit.reliabilityScore)
                                + "，风险="
                                + UiFormatter.score(audit.riskScore)
                                + "，惩罚="
                                + UiFormatter.probability(audit.penalty)
                                + "，增强=0"
                                + "，raw风险="
                                + UiFormatter.score(audit.rawRisk)
                                + "，校准风险="
                                + UiFormatter.score(audit.calibrationRisk)
                                + "，样本风险="
                                + UiFormatter.score(audit.sampleRisk)
                                + "，形态风险="
                                + UiFormatter.score(audit.shapeRisk)
                                + "，过拟合风险="
                                + UiFormatter.score(audit.overfitRisk)
                                + "，状态="
                                + audit.status
                                + "，说明=审计只写风险字段，不改FinalP/RawP/Gate，不写A+，不重排候选，不产生真实BET");
    }

    public AuditResult bestAudit(List<AuditResult> list) {
        if (list == null || list.size() == 0) {
            return null;
        }

        AuditResult best = null;

        for (int i = 0; i < list.size(); i++) {
            AuditResult r = list.get(i);

            if (r == null) {
                continue;
            }

            if (best == null || r.sortScore() > best.sortScore()) {
                best = r;
            }
        }

        return best;
    }

    public Models.DeepFeatureSet applyToDeepFeatureSet(
            Models.DeepFeatureSet set, Models.ComboCandidate candidate) {
        if (set == null) {
            set = new Models.DeepFeatureSet();
        }

        if (candidate == null) {
            return set;
        }

        AuditResult r = auditCandidate(candidate);

        /*
         * 不把审计可靠性写成趋势增强。
         * 只写全局风险，防止 DeepFeatureSet 反向救援候选。
         */
        set.globalDriftRisk = Math.max(set.globalDriftRisk, r.riskScore);
        set.summaryText = append(set.summaryText, r.summaryText);

        return set;
    }

    public String buildSummary(List<AuditResult> list) {
        if (list == null || list.size() == 0) {
            return "概率审计：暂无";
        }

        StringBuilder sb = new StringBuilder();

        int limit = Math.min(3, list.size());

        for (int i = 0; i < limit; i++) {
            AuditResult r = list.get(i);

            if (r == null) {
                continue;
            }

            if (sb.length() > 0) {
                sb.append(" | ");
            }

            sb.append("P");
            sb.append(r.position);
            sb.append(" ");
            sb.append(UiFormatter.numberGroup(r.n1, r.n2, r.n3));
            sb.append(" 可靠=");
            sb.append(UiFormatter.score(r.reliabilityScore));
            sb.append(" 风险=");
            sb.append(UiFormatter.score(r.riskScore));
            sb.append(" ");
            sb.append(r.status);
        }

        return sb.length() == 0 ? "概率审计：暂无有效结果" : sb.toString();
    }

    private double rawProbabilityRisk(Models.ComboCandidate c) {
        if (c == null) {
            return 100.0d;
        }

        double risk = 0.0d;

        if (c.rawP >= RAW_P_EXTREME_HIGH) {
            risk += 58.0d;
        } else if (c.rawP >= RAW_P_HARD_HIGH) {
            risk += 42.0d;
        } else if (c.rawP >= RAW_P_SOFT_HIGH) {
            risk += 26.0d;
        } else if (c.rawP >= RAW_P_WATCH_HIGH) {
            risk += 12.0d;
        } else if (c.rawP < 0.300d) {
            risk += 10.0d;
        }

        if (c.rawP > c.finalP + 0.040d && c.finalP > 0.0d) {
            risk += 12.0d;
        }

        if (c.rawP > c.calibratedP + 0.040d && c.calibratedP > 0.0d) {
            risk += 12.0d;
        }

        if (c.rawP >= 0.385d && c.effectiveSample < AppConfig.EFFECTIVE_SAMPLE_MIN) {
            risk += 18.0d;
        }

        if (c.rawP >= 0.400d && c.comboSample < AppConfig.COMBO_SAMPLE_MIN) {
            risk += 18.0d;
        }

        if (c.rawP >= 0.410d
                && (c.driftRisk > AppConfig.DRIFT_RISK_MAX
                        || c.breakDepth > AppConfig.BREAK_DEPTH_MAX)) {
            risk += 16.0d;
        }

        return AppConfig.clampScore100(risk);
    }

    private double calibrationRisk(Models.ComboCandidate c) {
        if (c == null) {
            return 100.0d;
        }

        double risk = 0.0d;
        double warn = calibrationErrorWarn();
        double hard = calibrationErrorHard();

        if (c.calibrationError >= CALIBRATION_ERROR_EXTREME) {
            risk += 70.0d;
        } else if (c.calibrationError >= hard) {
            risk += 52.0d;
        } else if (c.calibrationError >= warn) {
            risk += 34.0d;
        } else {
            risk += c.calibrationError * 320.0d;
        }

        if (c.calibratedP > c.rawP + 0.020d && c.rawP > 0.0d) {
            risk += 16.0d;
        }

        if (c.calibratedP > c.finalP + 0.025d && c.finalP > 0.0d) {
            risk += 10.0d;
        }

        if (c.finalP >= 0.385d && c.calibrationError >= warn) {
            risk += 12.0d;
        }

        if (c.finalP >= 0.400d && c.calibrationError >= hard) {
            risk += 18.0d;
        }

        if (c.calibratedP >= 0.400d && c.comboSample < AppConfig.COMBO_SAMPLE_MIN) {
            risk += 12.0d;
        }

        return AppConfig.clampScore100(risk);
    }

    private double finalProbabilityRisk(Models.ComboCandidate c) {
        if (c == null) {
            return 100.0d;
        }

        double risk = 0.0d;

        if (c.finalP >= FINAL_P_EXTREME_HIGH) {
            risk += 58.0d;
        } else if (c.finalP >= FINAL_P_HARD_HIGH) {
            risk += 42.0d;
        } else if (c.finalP >= FINAL_P_SOFT_HIGH) {
            risk += 25.0d;
        } else if (c.finalP >= FINAL_P_WATCH_HIGH) {
            risk += 10.0d;
        }

        if (c.finalP < AppConfig.MIN_REAL_BET_PROBABILITY) {
            risk += 22.0d;
        }

        if (c.finalP > c.rawP + 0.020d && c.rawP > 0.0d) {
            risk += 14.0d;
        }

        if (c.finalP > c.calibratedP + 0.020d && c.calibratedP > 0.0d) {
            risk += 14.0d;
        }

        if (c.finalP >= 0.385d && c.edge < EDGE_THIN) {
            risk += 14.0d;
        }

        if (c.finalP >= 0.400d && c.edge < EDGE_WARN) {
            risk += 16.0d;
        }

        if (c.finalP >= 0.400d && c.driftRisk >= AppConfig.DRIFT_RISK_MAX) {
            risk += 14.0d;
        }

        if (c.finalP >= 0.400d && c.breakDepth >= AppConfig.BREAK_DEPTH_MAX) {
            risk += 14.0d;
        }

        if (c.finalP >= 0.410d && c.evidenceRisk >= 0.620d) {
            risk += 16.0d;
        }

        return AppConfig.clampScore100(risk);
    }

    private double gateRisk(Models.ComboCandidate c) {
        if (c == null) {
            return 100.0d;
        }

        double risk = 0.0d;

        if (c.dynamicGate <= 0.0d) {
            risk += 40.0d;
        }

        if (c.dynamicGate < 0.340d) {
            risk += 24.0d;
        }

        if (c.dynamicGate < AppConfig.MIN_REAL_BET_PROBABILITY - 0.010d) {
            risk += 18.0d;
        }

        if (c.dynamicGate > 0.470d && c.finalP < c.dynamicGate) {
            risk += 10.0d;
        }

        if (c.finalP < c.dynamicGate) {
            risk += 22.0d;
        }

        if (c.finalP >= c.dynamicGate && c.edge >= EDGE_STRONG) {
            risk -= 6.0d;
        }

        return AppConfig.clampScore100(risk);
    }

    private double edgeRisk(Models.ComboCandidate c) {
        if (c == null) {
            return 100.0d;
        }

        double risk = 0.0d;

        if (c.edge < 0.0d) {
            risk += 36.0d;
        } else if (c.edge < EDGE_THIN) {
            risk += 24.0d;
        } else if (c.edge < EDGE_WARN) {
            risk += 16.0d;
        } else if (c.edge >= EDGE_STRONG) {
            risk -= 5.0d;
        }

        if (c.finalP >= 0.400d && c.edge < EDGE_WARN) {
            risk += 14.0d;
        }

        if (c.finalP >= 0.420d && c.edge < EDGE_STRONG) {
            risk += 12.0d;
        }

        return AppConfig.clampScore100(risk);
    }

    private double sampleRisk(Models.ComboCandidate c) {
        if (c == null) {
            return 100.0d;
        }

        double risk = 0.0d;

        if (c.effectiveSample <= 0) {
            risk += 42.0d;
        } else if (c.effectiveSample < AppConfig.EFFECTIVE_SAMPLE_MIN) {
            risk += 34.0d;
        } else if (c.effectiveSample < AppConfig.EFFECTIVE_SAMPLE_HIGH) {
            risk += 16.0d;
        }

        if (c.comboSample <= 0) {
            risk += 38.0d;
        } else if (c.comboSample < AppConfig.COMBO_SAMPLE_MIN) {
            risk += 30.0d;
        } else if (c.comboSample < AppConfig.COMBO_SAMPLE_HIGH) {
            risk += 14.0d;
        }

        if (c.effectiveSample < AppConfig.EFFECTIVE_SAMPLE_MIN && c.finalP >= 0.385d) {
            risk += 14.0d;
        }

        if (c.comboSample < AppConfig.COMBO_SAMPLE_MIN && c.finalP >= 0.385d) {
            risk += 14.0d;
        }

        if (c.comboSample < AppConfig.COMBO_SAMPLE_MIN && c.comboHitRate >= 0.400d) {
            risk += 12.0d;
        }

        return AppConfig.clampScore100(risk);
    }

    private double shapeRisk(Models.ComboCandidate c) {
        if (c == null) {
            return 100.0d;
        }

        double risk = 0.0d;

        risk += c.driftRisk * 0.30d;
        risk += c.breakDepth * 0.28d;
        risk += c.hotReverseRisk * 0.15d;
        risk += c.falseStrongRisk * 0.15d;

        if (AppConfig.MODE_BROKEN.equals(c.stateMode)
                || AppConfig.MODE_REVERSING.equals(c.stateMode)
                || "REVERSAL".equals(c.stateMode)) {
            risk += 20.0d;
        } else if (AppConfig.MODE_CHAOS.equals(c.stateMode)) {
            risk += 12.0d;
        } else if (AppConfig.MODE_CONFIRMED.equals(c.stateMode)
                || AppConfig.MODE_ACCELERATING.equals(c.stateMode)) {
            risk -= 6.0d;
        }

        if (c.finalP >= 0.400d && c.driftRisk > AppConfig.DRIFT_RISK_MAX) {
            risk += 10.0d;
        }

        if (c.finalP >= 0.400d && c.breakDepth > AppConfig.BREAK_DEPTH_MAX) {
            risk += 10.0d;
        }

        return AppConfig.clampScore100(risk);
    }

    private double evidenceRisk(Models.ComboCandidate c) {
        if (c == null) {
            return 100.0d;
        }

        double risk = 0.0d;

        risk += c.evidenceRisk * 100.0d * 0.50d;
        risk += c.auditPenalty * 100.0d * 0.16d;

        if (c.evidenceScore < 0.420d) {
            risk += 16.0d;
        } else if (c.evidenceScore >= 0.620d && c.evidenceRisk <= 0.560d) {
            risk -= 6.0d;
        }

        if (c.aiJudgeScore > 0.0d && c.aiJudgeConfidence > 0.0d && c.aiJudgeScore < 0.420d) {
            risk += 8.0d;
        }

        if (c.finalP >= 0.400d && c.evidenceRisk >= 0.660d) {
            risk += 14.0d;
        }

        return AppConfig.clampScore100(risk);
    }

    private double overfitRisk(Models.ComboCandidate c) {
        if (c == null) {
            return 100.0d;
        }

        double risk = 0.0d;

        if (c.finalP >= 0.385d && c.effectiveSample < AppConfig.EFFECTIVE_SAMPLE_MIN) {
            risk += 16.0d;
        }

        if (c.finalP >= 0.385d && c.comboSample < AppConfig.COMBO_SAMPLE_MIN) {
            risk += 16.0d;
        }

        if (c.finalP >= 0.400d && c.effectiveSample < AppConfig.EFFECTIVE_SAMPLE_HIGH) {
            risk += 14.0d;
        }

        if (c.finalP >= 0.400d && c.comboSample < AppConfig.COMBO_SAMPLE_HIGH) {
            risk += 14.0d;
        }

        if (c.comboHitRate >= 0.500d && c.comboSample < AppConfig.COMBO_SAMPLE_HIGH) {
            risk += 12.0d;
        }

        if (c.similarHitRate >= 0.500d && c.effectiveSample < AppConfig.EFFECTIVE_SAMPLE_HIGH) {
            risk += 10.0d;
        }

        if (c.calibrationError >= calibrationErrorWarn() && c.finalP >= 0.385d) {
            risk += 14.0d;
        }

        if (c.deepScore >= 0.720d && c.deepConfidence < 0.420d) {
            risk += 10.0d;
        }

        return AppConfig.clampScore100(risk);
    }

    private double reliabilityScore(AuditResult r, Models.ComboCandidate c) {
        if (r == null || c == null) {
            return 0.0d;
        }

        double score = 100.0d;

        score -= r.rawRisk * 0.12d;
        score -= r.calibrationRisk * 0.18d;
        score -= r.finalRisk * 0.16d;
        score -= r.gateRisk * 0.10d;
        score -= r.edgeRisk * 0.10d;
        score -= r.sampleRisk * 0.18d;
        score -= r.shapeRisk * 0.12d;
        score -= r.evidenceAuditRisk * 0.10d;
        score -= r.overfitRisk * 0.10d;

        if (c.finalP >= c.dynamicGate && c.edge >= EDGE_WARN) {
            score += 2.0d;
        }

        if (c.evidenceScore >= 0.620d && c.evidenceRisk <= 0.480d) {
            score += 2.0d;
        }

        return AppConfig.clampScore100(score);
    }

    private double finalRiskScore(AuditResult r, Models.ComboCandidate c) {
        if (r == null || c == null) {
            return 100.0d;
        }

        double risk = 0.0d;

        risk += r.rawRisk * 0.12d;
        risk += r.calibrationRisk * 0.18d;
        risk += r.finalRisk * 0.16d;
        risk += r.gateRisk * 0.10d;
        risk += r.edgeRisk * 0.08d;
        risk += r.sampleRisk * 0.18d;
        risk += r.shapeRisk * 0.13d;
        risk += r.evidenceAuditRisk * 0.10d;
        risk += r.overfitRisk * 0.10d;

        if (c.finalP >= c.dynamicGate && c.edge >= EDGE_STRONG && c.evidenceRisk <= 0.560d) {
            risk -= 3.0d;
        }

        return AppConfig.clampScore100(risk);
    }

    private double auditPenalty(AuditResult r, Models.ComboCandidate c) {
        if (r == null || c == null) {
            return 1.0d;
        }

        double penalty = 0.0d;

        if (r.riskScore >= 45.0d) {
            penalty += (r.riskScore - 45.0d) / 100.0d;
        }

        if (r.rawRisk >= 60.0d) {
            penalty += 0.060d;
        }

        if (r.calibrationRisk >= 60.0d) {
            penalty += 0.090d;
        }

        if (r.finalRisk >= 60.0d) {
            penalty += 0.070d;
        }

        if (r.sampleRisk >= 60.0d) {
            penalty += 0.080d;
        }

        if (r.overfitRisk >= 60.0d) {
            penalty += 0.070d;
        }

        if (r.shapeRisk >= 70.0d) {
            penalty += 0.060d;
        }

        if (r.evidenceAuditRisk >= 70.0d) {
            penalty += 0.060d;
        }

        if (c.finalP < c.dynamicGate) {
            penalty += 0.060d;
        }

        if (c.finalP >= 0.400d && c.edge < EDGE_WARN) {
            penalty += 0.050d;
        }

        if (penalty > MAX_AUDIT_PENALTY) {
            penalty = MAX_AUDIT_PENALTY;
        }

        return AppConfig.clampScore01(penalty);
    }

    private double auditBoost(AuditResult r, Models.ComboCandidate c) {
        /*
         * 固定红线：
         * ProbabilityAuditEngine 不再产生增强分。
         * 防止 auditBoost / A+ 成为隐形救援 BET。
         */
        return 0.0d;
    }

    private String statusByAudit(AuditResult r, Models.ComboCandidate c) {
        if (r == null || c == null) {
            return "INVALID";
        }

        if (r.riskScore >= 86.0d) {
            return "HARD_RISK";
        }

        if (r.riskScore >= 74.0d) {
            return "HIGH_RISK";
        }

        if (r.riskScore >= 62.0d) {
            return "WATCH_RISK";
        }

        if (r.reliabilityScore >= AUDIT_A_PLUS && r.riskScore <= 38.0d) {
            return "A_PLUS_AUDIT";
        }

        if (r.reliabilityScore >= AUDIT_STRONG && r.riskScore <= 48.0d) {
            return "STRONG_AUDIT";
        }

        if (r.reliabilityScore >= AUDIT_USABLE && r.riskScore <= 64.0d) {
            return "USABLE_AUDIT";
        }

        return "WEAK_AUDIT";
    }

    private String blockByAudit(AuditResult r, Models.ComboCandidate c) {
        if (r == null || c == null) {
            return AppConfig.BLOCK_COMBO_WEAK;
        }

        if (!c.isValidK3()) {
            return AppConfig.BLOCK_COMBO_WEAK;
        }

        if (c.driftRisk >= AppConfig.DRIFT_RISK_HARD_BLOCK) {
            return AppConfig.BLOCK_DRIFT_HIGH;
        }

        if (c.breakDepth >= breakDepthHardBlock()) {
            return AppConfig.BLOCK_BREAK_HIGH;
        }

        if (AppConfig.MODE_BROKEN.equals(c.stateMode)
                || AppConfig.MODE_REVERSING.equals(c.stateMode)
                || "REVERSAL".equals(c.stateMode)) {
            return trendWeakBlockCode();
        }

        if (r.calibrationRisk >= 86.0d && c.finalP < 0.430d && c.edge < EDGE_STRONG) {
            return AppConfig.BLOCK_LOW_PROBABILITY;
        }

        if (r.sampleRisk >= 86.0d && c.finalP < 0.430d && c.edge < EDGE_STRONG) {
            return AppConfig.BLOCK_SAMPLE_WEAK;
        }

        if (r.overfitRisk >= 86.0d && c.finalP < 0.430d && c.edge < EDGE_STRONG) {
            return AppConfig.BLOCK_BACKTEST_WEAK;
        }

        if (r.evidenceAuditRisk >= 86.0d && c.finalP < 0.430d && c.edge < EDGE_STRONG) {
            return AppConfig.BLOCK_LOW_PROBABILITY;
        }

        if (r.riskScore >= 88.0d && c.finalP < 0.435d && c.edge < EDGE_STRONG) {
            return AppConfig.BLOCK_COMBO_WEAK;
        }

        if (c.finalP < AppConfig.MIN_REAL_BET_PROBABILITY) {
            return AppConfig.BLOCK_LOW_PROBABILITY;
        }

        return "";
    }

    private void sortAuditResults(List<AuditResult> list) {
        if (list == null || list.size() <= 1) {
            return;
        }

        Collections.sort(
                list,
                new Comparator<AuditResult>() {
                    @Override
                    public int compare(AuditResult a, AuditResult b) {
                        double sa = a == null ? -999.0d : a.sortScore();
                        double sb = b == null ? -999.0d : b.sortScore();

                        if (sa < sb) {
                            return 1;
                        }

                        if (sa > sb) {
                            return -1;
                        }

                        int pa = a == null ? 999 : a.position;
                        int pb = b == null ? 999 : b.position;

                        return pa - pb;
                    }
                });
    }

    private void rerankCandidatesKeepOrder(List<Models.ComboCandidate> list) {
        if (list == null) {
            return;
        }

        for (int i = 0; i < list.size(); i++) {
            Models.ComboCandidate c = list.get(i);

            if (c != null) {
                c.rank = i + 1;
            }
        }
    }

    /*
     * 保留方法，避免外部旧调用缺失。
     * 但本版不再用于 applyToCandidates 主链。
     */
    private void sortCandidates(List<Models.ComboCandidate> list) {
        if (list == null || list.size() <= 1) {
            return;
        }

        Collections.sort(
                list,
                new Comparator<Models.ComboCandidate>() {
                    @Override
                    public int compare(Models.ComboCandidate a, Models.ComboCandidate b) {
                        double sa = candidateSortScore(a);
                        double sb = candidateSortScore(b);

                        if (sa < sb) {
                            return 1;
                        }

                        if (sa > sb) {
                            return -1;
                        }

                        int pa = a == null ? 999 : a.position;
                        int pb = b == null ? 999 : b.position;

                        return pa - pb;
                    }
                });

        rerankCandidatesKeepOrder(list);
    }

    private double candidateSortScore(Models.ComboCandidate c) {
        if (c == null) {
            return -999.0d;
        }

        /*
         * 审计排序只用于审计报告，不用于实时主链。
         */
        double score = c.finalP;
        score += c.edge * 0.60d;
        score += c.comboGap * 0.10d;
        score += c.championGap * 0.08d;
        score -= c.auditPenalty * 100.0d * 0.10d;
        score -= c.evidenceRisk * 100.0d * 0.04d;
        score -= c.driftRisk * 0.010d;
        score -= c.breakDepth * 0.010d;

        return score;
    }

    private String buildAuditKey(AuditResult r) {
        if (r == null) {
            return "";
        }

        return "PA_P"
                + r.position
                + "_N"
                + r.n1
                + "_"
                + r.n2
                + "_"
                + r.n3
                + "_R"
                + bucket(r.riskScore)
                + "_S"
                + bucket(r.reliabilityScore);
    }

    private int bucket(double score) {
        if (score >= 80.0d) {
            return 8;
        }

        if (score >= 70.0d) {
            return 7;
        }

        if (score >= 60.0d) {
            return 6;
        }

        if (score >= 50.0d) {
            return 5;
        }

        if (score >= 40.0d) {
            return 4;
        }

        if (score >= 30.0d) {
            return 3;
        }

        if (score >= 20.0d) {
            return 2;
        }

        return 1;
    }

    private String pickBlockCode(String oldCode, String newCode) {
        if (oldCode != null
                && oldCode.length() > 0
                && !AppConfig.BLOCK_LOW_PROBABILITY.equals(oldCode)
                && !AppConfig.BLOCK_COMBO_WEAK.equals(oldCode)
                && !AppConfig.BLOCK_SAMPLE_WEAK.equals(oldCode)) {
            return oldCode;
        }

        if (newCode == null || newCode.length() == 0) {
            return oldCode == null ? "" : oldCode;
        }

        return newCode;
    }

    private double blend01(double oldValue, double newValue, double weightNew) {
        oldValue = AppConfig.clampScore01(oldValue);
        newValue = AppConfig.clampScore01(newValue);

        if (weightNew < 0.0d) {
            weightNew = 0.0d;
        }

        if (weightNew > 1.0d) {
            weightNew = 1.0d;
        }

        return AppConfig.clampScore01(oldValue * (1.0d - weightNew) + newValue * weightNew);
    }

    private String buildSummary(AuditResult r) {
        if (r == null) {
            return "概率审计为空";
        }

        StringBuilder sb = new StringBuilder();

        sb.append("概率风控审计P");
        sb.append(r.position);
        sb.append(" ");
        sb.append(UiFormatter.numberGroup(r.n1, r.n2, r.n3));

        sb.append("：可靠=");
        sb.append(UiFormatter.score(r.reliabilityScore));

        sb.append("，风险=");
        sb.append(UiFormatter.score(r.riskScore));

        sb.append("，RawP=");
        sb.append(UiFormatter.probability(r.rawP));

        sb.append("，CalP=");
        sb.append(UiFormatter.probability(r.calibratedP));

        sb.append("，FinalP=");
        sb.append(UiFormatter.probability(r.finalP));

        sb.append("，Gate=");
        sb.append(UiFormatter.probability(r.dynamicGate));

        sb.append("，Edge=");
        sb.append(UiFormatter.signedProbability(r.edge));

        sb.append("，校准误差=");
        sb.append(UiFormatter.probability(r.calibrationError));

        sb.append("，样本=");
        sb.append(r.effectiveSample);
        sb.append("/");
        sb.append(r.comboSample);

        sb.append("，惩罚=");
        sb.append(UiFormatter.probability(r.penalty));

        sb.append("，增强=0");

        sb.append("，状态=");
        sb.append(r.status);

        sb.append("，口径=审计只写auditPenalty/blockCode/reason，不修改FinalP/RawP/Gate，不写A+，不重排候选，不产生真实BET");

        if (r.blockCode != null && r.blockCode.length() > 0) {
            sb.append("，拦截=");
            sb.append(UiFormatter.blockReasonText(r.blockCode));
        }

        return safeReason(sb.toString());
    }

    private void normalizeCandidate(Models.ComboCandidate c) {
        if (c == null) {
            return;
        }

        if (c.k3Mask <= 0
                && AppConfig.isValidNumber(c.n1)
                && AppConfig.isValidNumber(c.n2)
                && AppConfig.isValidNumber(c.n3)) {
            try {
                c.k3Mask = Models.buildK3Mask(c.n1, c.n2, c.n3);
            } catch (Throwable e) {
                c.k3Mask = buildK3MaskLocal(c.n1, c.n2, c.n3);
            }
        }

        if (Double.isNaN(c.rawP) || Double.isInfinite(c.rawP) || c.rawP < 0.0d) {
            c.rawP = 0.0d;
        }

        if (Double.isNaN(c.calibratedP)
                || Double.isInfinite(c.calibratedP)
                || c.calibratedP < 0.0d) {
            c.calibratedP = 0.0d;
        }

        if (Double.isNaN(c.finalP) || Double.isInfinite(c.finalP) || c.finalP < 0.0d) {
            c.finalP = 0.0d;
        }

        if (Double.isNaN(c.dynamicGate)
                || Double.isInfinite(c.dynamicGate)
                || c.dynamicGate <= 0.0d) {
            c.dynamicGate = AppConfig.DEFAULT_DYNAMIC_GATE;
        }

        if (Double.isNaN(c.calibrationError)
                || Double.isInfinite(c.calibrationError)
                || c.calibrationError < 0.0d) {
            c.calibrationError = 0.0d;
        }

        c.rawP = AppConfig.clampProbability(c.rawP);
        c.calibratedP = AppConfig.clampProbability(c.calibratedP);
        c.finalP = AppConfig.clampProbability(c.finalP);
        c.dynamicGate = AppConfig.clampProbability(c.dynamicGate);
        c.edge = c.finalP - c.dynamicGate;
    }

    private int buildK3MaskLocal(int n1, int n2, int n3) {
        int mask = 0;

        if (AppConfig.isValidNumber(n1)) {
            mask |= (1 << n1);
        }

        if (AppConfig.isValidNumber(n2)) {
            mask |= (1 << n2);
        }

        if (AppConfig.isValidNumber(n3)) {
            mask |= (1 << n3);
        }

        return mask;
    }

    private String append(String oldText, String addText) {
        if (addText == null || addText.length() == 0) {
            return oldText == null ? "" : oldText;
        }

        if (oldText == null || oldText.length() == 0) {
            return safeReason(addText);
        }

        if (oldText.indexOf(addText) >= 0) {
            return safeReason(oldText);
        }

        return safeReason(oldText + "；" + addText);
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String safeReason(String text) {
        if (text == null) {
            return "";
        }

        if (text.length() <= MAX_REASON_LEN) {
            return text;
        }

        return text.substring(0, MAX_REASON_LEN);
    }

    private double calibrationErrorWarn() {
        return readAppDouble("CALIBRATION_ERROR_MAX", DEFAULT_CALIBRATION_ERROR_WARN);
    }

    private double calibrationErrorHard() {
        return readAppDouble("CALIBRATION_ERROR_HARD", DEFAULT_CALIBRATION_ERROR_HARD);
    }

    private double breakDepthHardBlock() {
        return readAppDouble("BREAK_DEPTH_HARD_BLOCK", AppConfig.BREAK_DEPTH_MAX + 18.0d);
    }

    private String trendWeakBlockCode() {
        return readAppString("BLOCK_TREND_WEAK", "TREND_WEAK");
    }

    private double readAppDouble(String fieldName, double def) {
        if (fieldName == null || fieldName.length() == 0) {
            return def;
        }

        try {
            Field f = AppConfig.class.getField(fieldName);
            Object v = f.get(null);

            if (v instanceof Number) {
                double d = ((Number) v).doubleValue();

                if (!Double.isNaN(d) && !Double.isInfinite(d)) {
                    return d;
                }
            }

            if (v instanceof String) {
                double d2 = Double.parseDouble((String) v);

                if (!Double.isNaN(d2) && !Double.isInfinite(d2)) {
                    return d2;
                }
            }
        } catch (Throwable e) {
        }

        return def;
    }

    private String readAppString(String fieldName, String def) {
        if (fieldName == null || fieldName.length() == 0) {
            return def;
        }

        try {
            Field f = AppConfig.class.getField(fieldName);
            Object v = f.get(null);

            if (v != null) {
                return String.valueOf(v);
            }
        } catch (Throwable e) {
        }

        return def;
    }

    public static final class AuditResult {
        public long createdAt;

        public String auditKey;
        public String baseIssue;
        public String targetIssue;

        public int position;
        public int n1;
        public int n2;
        public int n3;
        public int k3Mask;

        public double rawP;
        public double calibratedP;
        public double finalP;
        public double dynamicGate;
        public double edge;
        public double calibrationError;

        public int effectiveSample;
        public int comboSample;

        public double driftRisk;
        public double breakDepth;
        public double evidenceRisk;
        public double evidenceScore;
        public double auditPenaltyInput;
        public double auditBoostInput;

        public String stateMode;
        public double aPlusScore;
        public double aiJudgeScore;
        public double aiJudgeConfidence;

        public double rawRisk;
        public double calibrationRisk;
        public double finalRisk;
        public double gateRisk;
        public double edgeRisk;
        public double sampleRisk;
        public double shapeRisk;
        public double evidenceAuditRisk;
        public double overfitRisk;

        public double reliabilityScore;
        public double riskScore;
        public double penalty;
        public double boost;

        public String status;
        public String blockCode;
        public String summaryText;

        public AuditResult() {
            createdAt = 0L;

            auditKey = "";
            baseIssue = "";
            targetIssue = "";

            position = 0;
            n1 = 0;
            n2 = 0;
            n3 = 0;
            k3Mask = 0;

            rawP = 0.0d;
            calibratedP = 0.0d;
            finalP = 0.0d;
            dynamicGate = 0.0d;
            edge = 0.0d;
            calibrationError = 0.0d;

            effectiveSample = 0;
            comboSample = 0;

            driftRisk = 0.0d;
            breakDepth = 0.0d;
            evidenceRisk = 0.0d;
            evidenceScore = 0.0d;
            auditPenaltyInput = 0.0d;
            auditBoostInput = 0.0d;

            stateMode = "";
            aPlusScore = 0.0d;
            aiJudgeScore = 0.0d;
            aiJudgeConfidence = 0.0d;

            rawRisk = 0.0d;
            calibrationRisk = 0.0d;
            finalRisk = 0.0d;
            gateRisk = 0.0d;
            edgeRisk = 0.0d;
            sampleRisk = 0.0d;
            shapeRisk = 0.0d;
            evidenceAuditRisk = 0.0d;
            overfitRisk = 0.0d;

            reliabilityScore = 0.0d;
            riskScore = 100.0d;
            penalty = 0.0d;
            boost = 0.0d;

            status = "";
            blockCode = "";
            summaryText = "";
        }

        public boolean isUsable() {
            return reliabilityScore >= AUDIT_USABLE
                    && riskScore <= 72.0d
                    && (blockCode == null || blockCode.length() == 0);
        }

        public boolean isStrong() {
            return isUsable() && reliabilityScore >= AUDIT_STRONG && riskScore <= 52.0d;
        }

        public double sortScore() {
            double score = reliabilityScore;

            score -= riskScore * 0.12d;
            score -= penalty * 100.0d * 0.10d;

            /*
             * boost 固定为 0，不参与救援。
             */
            if (isStrong()) {
                score += 2.0d;
            }

            if (blockCode != null && blockCode.length() > 0) {
                score -= 10.0d;
            }

            return score;
        }
    }
}
