package com.ethk3.novaonepro;

import java.util.ArrayList;
import java.util.List;

public final class Models {
    public static final String OUMAN_GALAXY_REBUILD_VERSION =
            AppConfig.OUMAN_GALAXY_REBUILD_VERSION;

    /*
     * 上游证据补实修复 01/06：Models 只负责字段实体承接和复制同步。
     * 不计算概率，不决定 BET，不创建 Trade，不结算，不绕过 DecisionGateEngine。
     */
    public static final String V20_MODELS_UPSTREAM_EVIDENCE_VERSION =
            "V48_POINT_IN_TIME_MODEL_LAB_SINGLE_ACTION_CHAMPION_FULL_DIAGNOSIS_20260721_48072148";
    public static final String V20_MODELS_DYNAMIC_CAUSAL_WINDOW_VERSION =
            "V48_POINT_IN_TIME_MODEL_LAB_SINGLE_ACTION_CHAMPION_FULL_DIAGNOSIS_20260721_48072148";
    public static final String V20_MODELS_MAIN_CHAIN_CLOSED_LOOP_VERSION =
            "V48_POINT_IN_TIME_MODEL_LAB_SINGLE_ACTION_CHAMPION_FULL_DIAGNOSIS_20260721_48072148";

    private Models() {}

    public static final String SOURCE_TYPE_UNKNOWN = "UNKNOWN";
    public static final String SOURCE_TYPE_REALTIME = "REALTIME";
    public static final String SOURCE_TYPE_BACKTEST = "BACKTEST";
    public static final String SOURCE_TYPE_MANUAL_BACKTEST = "MANUAL_BACKTEST";
    public static final String SOURCE_TYPE_AUTO_LEARN = "AUTO_LEARN";
    public static final String SOURCE_TYPE_TEACHER_MATRIX = "TEACHER_MATRIX";
    public static final String SOURCE_TYPE_TEACHER_REPLAY = "TEACHER_REPLAY";
    public static final String SOURCE_TYPE_SHADOW = "SHADOW";

    public static final String REASON_LEVEL_A_VALID = "A_VALID";
    public static final String REASON_LEVEL_B_USEFUL = "B_USEFUL";
    public static final String REASON_LEVEL_C_EXPLAIN_ONLY = "C_EXPLAIN_ONLY";
    public static final String REASON_LEVEL_D_WEAK = "D_WEAK";
    public static final String REASON_LEVEL_E_FALSE = "E_FALSE";

    public static final String K3_ROLE_UNKNOWN = "UNKNOWN";
    public static final String K3_ROLE_MAIN = "MAIN";
    public static final String K3_ROLE_SUPPORT = "SUPPORT";
    public static final String K3_ROLE_DEFENSE = "DEFENSE";

    public static final String BLOCK_ELITE45_NOT_PASS = "ELITE45_NOT_PASS";
    public static final String BLOCK_REASON_EDGE_WEAK = "REASON_EDGE_WEAK";
    public static final String BLOCK_HIT_SIMILARITY_WEAK = "HIT_SIMILARITY_WEAK";
    public static final String BLOCK_MISS_SIMILARITY_HIGH = "MISS_SIMILARITY_HIGH";
    public static final String BLOCK_VALID_REASON_WEAK = "VALID_REASON_WEAK";
    public static final String BLOCK_FALSE_REASON = "FALSE_REASON";
    public static final String BLOCK_COMBO_CONFLICT = "COMBO_CONFLICT";
    public static final String BLOCK_FINGERPRINT_COOLING = "FINGERPRINT_COOLING";

    public static final double REASON_HIT_RATE_BASELINE = 0.300d;
    public static final int REASON_MIN_SAMPLE = 18;
    public static final int REASON_USEFUL_SAMPLE = 36;
    public static final int REASON_STRONG_SAMPLE = 80;
    public static final int REASON_STABLE_SAMPLE = 120;
    public static final int REASON_FORWARD_VALIDATE_MIN = 24;

    public static final double REASON_A_WILSON_MIN = 0.360d;
    public static final double REASON_B_WILSON_MIN = 0.330d;
    public static final double REASON_FALSE_WILSON_MAX = 0.285d;

    public static final double ELITE45_GATE_DEFAULT = 0.420d;
    public static final double ELITE45_GATE_MIN = 0.385d;
    public static final double ELITE45_GATE_MAX = 0.520d;
    public static final double ELITE45_MIN_FINAL_P = 0.400d;
    public static final double ELITE45_MIN_REASON_EDGE = 0.025d;
    public static final double ELITE45_MIN_HIT_SIMILARITY = 0.560d;
    public static final double ELITE45_MAX_MISS_SIMILARITY = 0.440d;
    public static final double ELITE45_MAX_CONTRADICTION_SCORE = 0.520d;
    public static final double ELITE45_MAX_NEGATIVE_REASON_SCORE = 0.450d;
    public static final int ELITE45_MIN_VALID_REASON_COUNT = 2;
    public static final int ELITE45_MAX_FALSE_REASON_COUNT = 0;
    public static final double ELITE45_MAX_COMBO_CONFLICT = 0.560d;
    public static final double ELITE45_MIN_COMBO_ROLE_DIVERSITY = 0.420d;
    public static final double ELITE45_MAX_DRIFT_RISK = 0.520d;
    public static final double ELITE45_MAX_BREAK_RISK = 0.560d;

    public static final int SAME_FINGERPRINT_MISS_SHADOW = 2;
    public static final int SAME_FINGERPRINT_MISS_COOLDOWN = 3;
    public static final int SAME_FINGERPRINT_MISS_FALSE_STRONG = 4;
    public static final double FINGERPRINT_COOLDOWN_RISK_PENALTY = 0.035d;
    public static final double FINGERPRINT_FALSE_STRONG_RISK_PENALTY = 0.060d;

    /*
     * 重要修复：
     * 全项目 k3Mask 必须统一走 AppConfig 口径。
     * 当前 AppConfig 的真实口径是：号码 01-10 对应 bit 0-9，也就是 1 << (number - 1)。
     * Models 禁止自己再写 1 << number，避免结算、教师矩阵、候选画像、原因矩阵全部错位。
     */
    public static int numberToMaskBit(int number) {
        return AppConfig.numberToMaskBit(number);
    }

    public static int buildK3Mask(int n1, int n2, int n3) {
        return AppConfig.buildK3Mask(n1, n2, n3);
    }

    public static int buildMaskFromArray(int[] numbers) {
        int mask = 0;
        if (numbers == null) {
            return mask;
        }
        for (int i = 0; i < numbers.length; i++) {
            mask |= numberToMaskBit(numbers[i]);
        }
        return mask;
    }

    public static boolean maskContains(int mask, int number) {
        return AppConfig.maskContainsNumber(mask, number);
    }

    public static boolean k3MaskContains(int mask, int number) {
        return maskContains(mask, number);
    }

    public static int countMaskNumbers(int mask) {
        return AppConfig.countK3MaskNumbers(mask);
    }

    public static int[] maskToNumbers(int mask) {
        int[] out = new int[AppConfig.FIXED_K];
        int index = 0;
        for (int n = AppConfig.MIN_NUMBER; n <= AppConfig.MAX_NUMBER; n++) {
            if (maskContains(mask, n)) {
                if (index < out.length) {
                    out[index] = n;
                }
                index++;
            }
        }
        return out;
    }

    public static boolean isValidK3Mask(int mask) {
        return (mask & ~1023)==0 && AppConfig.isValidK3Mask(mask);
    }

    public static boolean isValidK3Numbers(int n1, int n2, int n3) {
        return AppConfig.isValidK3Numbers(n1, n2, n3);
    }

    public static String maskToText(int mask) {
        return AppConfig.k3MaskToText(mask);
    }

    public static double safeProbability(double value) {
        return AppConfig.clampProbability(value);
    }

    public static double safeScore(double value) {
        return AppConfig.clampScore100(value);
    }

    public static double safeScore01(double value) {
        return AppConfig.clampScore01(value);
    }

    public static String safeText(String value) {
        return value == null ? "" : value;
    }

    public static String safeIssue(String value) {
        return AppConfig.safeIssue(value);
    }

    public static String two(int number) {
        return UiFormatter.number(number);
    }

    public static double clampAbs(double value, double maxAbs) {
        return AppConfig.limitAdjust(value, maxAbs);
    }

    public static double clampTeacherWeight(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return 1.0d;
        }
        if (value < 0.70d) {
            return 0.70d;
        }
        if (value > 1.30d) {
            return 1.30d;
        }
        return value;
    }

    public static String appendReason(String oldText, String addText) {
        if (addText == null || addText.length() == 0) {
            return oldText == null ? "" : oldText;
        }
        if (oldText == null || oldText.length() == 0) {
            return addText;
        }
        return oldText + "｜" + addText;
    }

    public static double teacherNumberBaseline() {
        return 1.0d / ((double) AppConfig.NUMBER_COUNT);
    }

    public static double teacherComboBaseline() {
        return AppConfig.RANDOM_K3_BASELINE;
    }

    public static double teacherMatrixProbScale() {
        return AppConfig.TEACHER_MATRIX_PROB_SCALE;
    }

    public static double teacherMatrixComboProbScale() {
        return AppConfig.TEACHER_MATRIX_COMBO_PROB_SCALE;
    }

    public static int teacherMatrixRecentMinSample() {
        return AppConfig.TEACHER_MATRIX_STREAK_MIN_SAMPLE;
    }

    public static double teacherMatrixWeakRecentHitRate() {
        return AppConfig.TEACHER_MATRIX_NUMBER_HIT_RATE_WEAK;
    }

    public static double teacherMatrixStrongRecentHitRate() {
        return AppConfig.TEACHER_MATRIX_NUMBER_HIT_RATE_STRONG;
    }

    public static double teacherMatrixMaxRiskPenalty() {
        return AppConfig.TEACHER_MATRIX_MAX_RISK_PENALTY;
    }

    public static double teacherMatrixBlockHitRateForNumber() {
        return AppConfig.TEACHER_MATRIX_NUMBER_HIT_RATE_WEAK * 0.80d;
    }

    public static double teacherMatrixBlockHitRateForCombo() {
        return 0.240d;
    }

    public static double buildTeacherMatrixScore(
            double numberScore, double positionScore, double stateScore, double comboScore) {
        return AppConfig.clampProbability(
                numberScore * 0.45d
                        + positionScore * 0.20d
                        + stateScore * 0.20d
                        + comboScore * 0.15d);
    }

    public static String normalizeSourceType(String value) {
        String s = AppConfig.normalizeToken(value);
        if (SOURCE_TYPE_REALTIME.equals(s)) return SOURCE_TYPE_REALTIME;
        if (SOURCE_TYPE_BACKTEST.equals(s)) return SOURCE_TYPE_BACKTEST;
        if (SOURCE_TYPE_MANUAL_BACKTEST.equals(s)) return SOURCE_TYPE_MANUAL_BACKTEST;
        if (SOURCE_TYPE_AUTO_LEARN.equals(s)) return SOURCE_TYPE_AUTO_LEARN;
        if (SOURCE_TYPE_TEACHER_MATRIX.equals(s)) return SOURCE_TYPE_TEACHER_MATRIX;
        if (SOURCE_TYPE_TEACHER_REPLAY.equals(s)) return SOURCE_TYPE_TEACHER_REPLAY;
        if (SOURCE_TYPE_SHADOW.equals(s)) return SOURCE_TYPE_SHADOW;
        return SOURCE_TYPE_UNKNOWN;
    }

    public static String normalizeReasonLevel(String value) {
        String s = AppConfig.normalizeToken(value);
        if (REASON_LEVEL_A_VALID.equals(s)) return REASON_LEVEL_A_VALID;
        if (REASON_LEVEL_B_USEFUL.equals(s)) return REASON_LEVEL_B_USEFUL;
        if (REASON_LEVEL_D_WEAK.equals(s)) return REASON_LEVEL_D_WEAK;
        if (REASON_LEVEL_E_FALSE.equals(s)) return REASON_LEVEL_E_FALSE;
        return REASON_LEVEL_C_EXPLAIN_ONLY;
    }

    public static boolean isReasonLevelBoostable(String value) {
        String s = normalizeReasonLevel(value);
        return REASON_LEVEL_A_VALID.equals(s) || REASON_LEVEL_B_USEFUL.equals(s);
    }

    public static double clampElite45Gate(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return ELITE45_GATE_DEFAULT;
        }
        if (value < ELITE45_GATE_MIN) return ELITE45_GATE_MIN;
        if (value > ELITE45_GATE_MAX) return ELITE45_GATE_MAX;
        return value;
    }

    public static boolean shouldCountInStrictRealStats(
            String decision,
            String scope,
            String sourceType,
            boolean isRealtimeReal,
            boolean settled) {
        return AppConfig.DECISION_BET.equals(AppConfig.normalizeDecision(decision))
                && AppConfig.isRealTradeScope(scope)
                && SOURCE_TYPE_REALTIME.equals(normalizeSourceType(sourceType))
                && isRealtimeReal
                && settled;
    }

    public static double buildElite45ReasonGateAdjust(
            double reasonEdge,
            double hitSimilarity,
            double missSimilarity,
            int falseReasonCount,
            double comboConflict,
            boolean fingerprintCooling) {
        double adjust = 0.0d;

        if (reasonEdge >= 0.050d && hitSimilarity >= 0.620d && missSimilarity <= 0.380d) {
            adjust -= 0.018d;
        } else if (reasonEdge >= ELITE45_MIN_REASON_EDGE
                && hitSimilarity >= ELITE45_MIN_HIT_SIMILARITY) {
            adjust -= 0.008d;
        }

        if (missSimilarity > ELITE45_MAX_MISS_SIMILARITY) {
            adjust += 0.018d;
        }
        if (falseReasonCount > 0) {
            adjust += 0.026d;
        }
        if (comboConflict > ELITE45_MAX_COMBO_CONFLICT) {
            adjust += 0.018d;
        }
        if (fingerprintCooling) {
            adjust += 0.040d;
        }

        return AppConfig.limitAdjust(adjust, 0.060d);
    }

    public static String buildReasonFingerprint(
            String stateKey,
            String trendKey,
            String shapeKey,
            String driftKey,
            String recoveryKey,
            String relationKey,
            String mainReasonCode) {
        StringBuilder sb = new StringBuilder();
        sb.append(AppConfig.buildStateKeyPart(stateKey));
        sb.append("|T=").append(AppConfig.buildStateKeyPart(trendKey));
        sb.append("|S=").append(AppConfig.buildStateKeyPart(shapeKey));
        sb.append("|D=").append(AppConfig.buildStateKeyPart(driftKey));
        sb.append("|R=").append(AppConfig.buildStateKeyPart(recoveryKey));
        sb.append("|REL=").append(AppConfig.buildStateKeyPart(relationKey));
        sb.append("|M=").append(AppConfig.buildStateKeyPart(mainReasonCode));
        return sb.toString();
    }

    public static String buildPreDrawReasonSampleKey(
            String targetIssue, int position, int number, String fingerprint) {
        return "PDRS_"
                + safeIssue(targetIssue)
                + "_P"
                + position
                + "_N"
                + number
                + "_"
                + Math.abs(safeText(fingerprint).hashCode());
    }

    public static String buildReasonContrastProfileKey(
            String fingerprint, String stateKey, int position, int number) {
        return "RCP_"
                + Math.abs(safeText(fingerprint).hashCode())
                + "_P"
                + position
                + "_N"
                + number
                + "_"
                + Math.abs(safeText(stateKey).hashCode());
    }

    public static String buildReasonCausalKey(String reasonAtom, String comboKey) {
        return "RCA_"
                + Math.abs(safeText(reasonAtom).hashCode())
                + "_"
                + Math.abs(safeText(comboKey).hashCode());
    }

    public static String buildFalseStrongKey(
            String stateKey, int position, int number, int k3Mask, String fingerprint) {
        return "FS_"
                + Math.abs(safeText(stateKey).hashCode())
                + "_P"
                + position
                + "_N"
                + number
                + "_K"
                + k3Mask
                + "_"
                + Math.abs(safeText(fingerprint).hashCode());
    }

    public static String buildComboRoleKey(String stateKey, int position, int k3Mask) {
        return "CR_" + Math.abs(safeText(stateKey).hashCode()) + "_P" + position + "_K" + k3Mask;
    }

    public static String buildElite45AuditKey(String targetIssue, int position, int k3Mask) {
        return "E45_" + safeIssue(targetIssue) + "_P" + position + "_K" + k3Mask;
    }

    public static double calcWilsonLowerBound(int hit, int sample) {
        if (sample <= 0 || hit <= 0) {
            return 0.0d;
        }

        double n = (double) sample;
        double phat = ((double) hit) / n;
        double z = 1.96d;
        double z2 = z * z;

        double numerator =
                phat
                        + z2 / (2.0d * n)
                        - z * Math.sqrt((phat * (1.0d - phat) + z2 / (4.0d * n)) / n);
        double denominator = 1.0d + z2 / n;

        return AppConfig.clampProbability(numerator / denominator);
    }

    public static double calcWilsonUpperBound(int hit, int sample) {
        if (sample <= 0) return 1.0d;
        double n = (double) sample,
                phat = ((double) Math.max(0, Math.min(hit, sample))) / n,
                z = 1.96d,
                z2 = z * z;
        double numerator =
                phat
                        + z2 / (2.0d * n)
                        + z * Math.sqrt((phat * (1.0d - phat) + z2 / (4.0d * n)) / n);
        double denominator = 1.0d + z2 / n;
        return AppConfig.clampProbability(numerator / denominator);
    }

    /*
     * V20 动态因果窗口自学习闭环系统 · 第2/14 Models实体承接
     *
     * Models 只负责实体字段、复制同步、状态归一化、样本承接。
     * 不计算概率，不创建 Trade，不决定 BET，不结算，不绕过 DecisionGateEngine。
     */
    public static double clampDynamicCausalScore(double v) {
        return AppConfig.clampDynamicCausalScore(v);
    }

    public static String normalizeDynamicClusterStatus(String status) {
        return AppConfig.normalizeDynamicClusterStatus(status);
    }

    public static boolean dynamicClusterAllowsBet(
            int sample,
            int recentSample,
            double hitRate,
            double recentHitRate,
            int maxLose,
            double confidence,
            double volatility,
            double counterScore,
            String status) {
        return AppConfig.dynamicClusterAllowsBet(
                sample,
                recentSample,
                hitRate,
                recentHitRate,
                maxLose,
                confidence,
                volatility,
                counterScore,
                status);
    }

    public static String buildDynamicCausalSummary(
            String clusterId,
            String clusterStatus,
            double hitRate,
            double recentHitRate,
            int sample,
            double needSupplyMatch,
            double thirdQuality,
            String adaptiveAction) {
        StringBuilder sb = new StringBuilder();
        sb.append("Cluster=").append(safeText(clusterId));
        sb.append("/").append(normalizeDynamicClusterStatus(clusterStatus));
        sb.append(" 样本=").append(sample);
        sb.append(" 命中=").append(UiFormatter.probability(hitRate));
        sb.append(" 近期=").append(UiFormatter.probability(recentHitRate));
        sb.append(" NeedSupply=").append(UiFormatter.probability(needSupplyMatch));
        sb.append(" Third=").append(UiFormatter.probability(thirdQuality));
        sb.append(" Judge=").append(safeText(adaptiveAction));
        return sb.toString();
    }

    public static void initDynamicCausalBridgeFields(Object obj) {
        if (obj == null) {
            return;
        }
        writeStringField(obj, "dynamicClusterId", "");
        writeStringField(obj, "dynamicClusterStatus", AppConfig.CLUSTER_STATUS_UNKNOWN);
        writeStringField(obj, "dynamicClusterPrototypeKey", "");
        writeStringField(obj, "dynamicClusterFeatureKey", "");
        writeStringField(obj, "dynamicClusterReason", "");
        writeStringField(obj, "dynamicClusterVersion", V20_MODELS_DYNAMIC_CAUSAL_WINDOW_VERSION);
        writeStringField(obj, "positionNeedKey", "");
        writeStringField(obj, "positionNeedSummary", "");
        writeStringField(obj, "numberSupplyKey", "");
        writeStringField(obj, "numberSupplySummary", "");
        writeStringField(obj, "needSupplyMatchKey", "");
        writeStringField(obj, "needSupplySummary", "");
        writeStringField(obj, "k3RoleMain", AppConfig.DYNAMIC_K3_ROLE_UNKNOWN);
        writeStringField(obj, "k3RoleAssist", AppConfig.DYNAMIC_K3_ROLE_UNKNOWN);
        writeStringField(obj, "k3RoleDefense", AppConfig.DYNAMIC_K3_ROLE_UNKNOWN);
        writeStringField(obj, "k3RoleStructure", "");
        writeStringField(obj, "counterfactualReasonKey", "");
        writeStringField(obj, "counterfactualWhyNot", "");
        writeStringField(obj, "counterfactualSummary", "");
        writeStringField(obj, "causeAblationKey", "");
        writeStringField(obj, "causeAblationSummary", "");
        writeStringField(obj, "missReasonType", AppConfig.MISS_REASON_UNKNOWN);
        writeStringField(obj, "missReasonMatrixKey", "");
        writeStringField(obj, "missReasonDetail", "");
        writeStringField(obj, "adaptiveJudgeStatus", AppConfig.CLUSTER_STATUS_UNKNOWN);
        writeStringField(obj, "adaptiveJudgeAction", AppConfig.ADAPTIVE_ACTION_WATCH);
        writeStringField(obj, "adaptiveJudgeDomain", "");
        writeStringField(obj, "adaptiveJudgeReason", "");
        writeStringField(obj, "adaptiveJudgeVersion", V20_MODELS_DYNAMIC_CAUSAL_WINDOW_VERSION);
        writeStringField(obj, "promotionClusterStatus", AppConfig.CLUSTER_STATUS_UNKNOWN);
        writeStringField(obj, "promotionClusterReason", "");
        writeStringField(obj, "whyBet", "");
        writeStringField(obj, "whyNoBet", "");
        writeStringField(obj, "whyHit", "");
        writeStringField(obj, "whyMiss", "");
        writeStringField(obj, "dynamicCausalSummary", "");
    }

    public static void normalizeDynamicCausalBridgeFields(Object obj) {
        if (obj == null) {
            return;
        }
        String status =
                readStringField(obj, "dynamicClusterStatus", AppConfig.CLUSTER_STATUS_UNKNOWN);
        writeStringField(
                obj, "dynamicClusterStatus", AppConfig.normalizeDynamicClusterStatus(status));
        String promoteStatus =
                readStringField(obj, "promotionClusterStatus", AppConfig.CLUSTER_STATUS_UNKNOWN);
        writeStringField(
                obj,
                "promotionClusterStatus",
                AppConfig.normalizeDynamicClusterStatus(promoteStatus));
        String judgeAction =
                readStringField(obj, "adaptiveJudgeAction", AppConfig.ADAPTIVE_ACTION_WATCH);
        if (judgeAction == null || judgeAction.length() == 0) {
            judgeAction = AppConfig.ADAPTIVE_ACTION_WATCH;
        }
        writeStringField(obj, "adaptiveJudgeAction", judgeAction.toUpperCase(java.util.Locale.US));

        clampDoubleField(obj, "dynamicClusterHitRate");
        clampDoubleField(obj, "dynamicClusterRecentHitRate");
        clampDoubleField(obj, "dynamicClusterConfidence");
        clampDoubleField(obj, "dynamicClusterVolatility");
        clampDoubleField(obj, "dynamicClusterCounterScore");
        clampDoubleField(obj, "positionNeedStrength");
        clampDoubleField(obj, "numberSupplyStrength");
        clampDoubleField(obj, "needSupplyMatchScore");
        clampDoubleField(obj, "needSupplyThirdQuality");
        clampDoubleField(obj, "needSupplyRoleDiversity");
        clampDoubleField(obj, "needSupplyRisk");
        clampDoubleField(obj, "counterfactualRankScore");
        clampDoubleField(obj, "counterfactualWhyNotRisk");
        clampDoubleField(obj, "causeAblationScore");
        clampDoubleField(obj, "adaptiveCauseScore");
        clampDoubleField(obj, "adaptiveCounterScore");
        clampDoubleField(obj, "adaptivePromoteScore");
        clampDoubleField(obj, "adaptiveFreezeScore");
        clampDoubleField(obj, "promotionClusterHitRate");
        clampDoubleField(obj, "promotionClusterRecentHitRate");
    }

    public static void copyDynamicCausalBridgeFields(Object from, Object to) {
        if (from == null || to == null) {
            return;
        }
        String[] names = dynamicCausalBridgeFieldNames();
        for (int i = 0; i < names.length; i++) {
            copyPublicField(from, to, names[i]);
        }
        normalizeDynamicCausalBridgeFields(to);
    }

    /*
     * V20主链闭环字段桥接：只负责字段承接、归一、复制、别名同步。
     * 不计算概率，不决定BET，不创建Trade，不结算，不绕过DecisionGateEngine。
     */
    public static String[] mainChainBridgeFieldNames() {
        return new String[] {
            "windowStateKey",
            "clusterKey",
            "factorKey",
            "positionNeedScore",
            "numberSupplyScore",
            "coverageQuality",
            "thirdQuality",
            "counterEvidenceScore",
            "similarSampleCount",
            "similarHitRate",
            "teacherStatus",
            "v20Status",
            "adaptiveStatus",
            "shadowPromotionStatus",
            "finalPBeforeDiagnosis",
            "finalPAfterDiagnosis",
            "gate",
            "edge",
            "rejectCode",
            "realProbeAllowed",
            "teacherAsOfIssue",
            "teacherIssueLag",
            "teacherFreshByIssue",
            "teacherSnapshotCreatedAt",
            "blockTier",
            "hardRiskCode",
            "observeReason",
            "probeEligible",
            "shadowProbeEligible",
            "preDecisionOpportunity",
            "riskSeverity",
            "hardBlockFlag",
            "hardBlockSource",
            "evidenceSource",
            "evidenceSettledOnly",
            "evidenceSampleCount",
            "evidenceHitCount",
            "evidenceMissCount",
            "promotionCoarseDomain",
            "promotionMidDomain",
            "promotionFineDomain",
            "probeLevel",
            "probeRejectReason",
            "shadowSettledCount",
            "shadowDomainSample",
            "promotionWilsonLowerBound",
            "promotionEligibilityReason"
        };
    }

    public static void initMainChainBridgeFields(Object obj) {
        if (obj == null) return;
        writeStringField(obj, "clusterKey", "");
        writeStringField(obj, "factorKey", "");
        writeStringField(obj, "adaptiveStatus", AppConfig.ADAPTIVE_ACTION_WATCH);
        writeStringField(obj, "shadowPromotionStatus", AppConfig.ADAPTIVE_ACTION_WATCH);
        writeStringField(obj, "rejectCode", "");
        writeStringField(obj, "teacherAsOfIssue", "");
        writeStringField(obj, "blockTier", "OBSERVE");
        writeStringField(obj, "hardRiskCode", "");
        writeStringField(obj, "observeReason", "");
        writeStringField(obj, "probeLevel", "NONE");
        writeStringField(obj, "probeRejectReason", "");
        writeStringField(obj, "promotionEligibilityReason", "");
        writeDoubleFieldIfEmpty(obj, "positionNeedScore", 0.0d);
        writeDoubleFieldIfEmpty(obj, "numberSupplyScore", 0.0d);
        writeDoubleFieldIfEmpty(obj, "coverageQuality", 0.0d);
        writeDoubleFieldIfEmpty(obj, "thirdQuality", 0.0d);
        writeDoubleFieldIfEmpty(obj, "counterEvidenceScore", 0.0d);
        writeDoubleFieldIfEmpty(obj, "finalPBeforeDiagnosis", 0.0d);
        writeDoubleFieldIfEmpty(obj, "finalPAfterDiagnosis", 0.0d);
        writeDoubleFieldIfEmpty(obj, "gate", AppConfig.DEFAULT_DYNAMIC_GATE);
        writeIntFieldIfEmpty(obj, "similarSampleCount", 0);
        writeIntFieldIfEmpty(obj, "realProbeAllowed", 0);
        writeIntFieldIfEmpty(obj, "teacherIssueLag", 0);
        writeIntFieldIfEmpty(obj, "teacherFreshByIssue", 0);
        writeIntFieldIfEmpty(obj, "probeEligible", 0);
        writeIntFieldIfEmpty(obj, "preDecisionOpportunity", 0);
        writeStringField(obj, "riskSeverity", AppConfig.EVIDENCE_SEVERITY_NEUTRAL);
        writeIntFieldIfEmpty(obj, "hardBlockFlag", 0);
        writeStringField(obj, "hardBlockSource", "");
        writeStringField(obj, "evidenceSource", "NONE");
        writeIntFieldIfEmpty(obj, "evidenceSettledOnly", 1);
        writeIntFieldIfEmpty(obj, "evidenceSampleCount", 0);
        writeIntFieldIfEmpty(obj, "evidenceHitCount", 0);
        writeIntFieldIfEmpty(obj, "evidenceMissCount", 0);
        writeStringField(obj, "promotionCoarseDomain", "");
        writeStringField(obj, "promotionMidDomain", "");
        writeStringField(obj, "promotionFineDomain", "");
        writeIntFieldIfEmpty(obj, "shadowProbeEligible", 0);
        writeIntFieldIfEmpty(obj, "shadowSettledCount", 0);
        writeIntFieldIfEmpty(obj, "shadowDomainSample", 0);
        normalizeMainChainBridgeFields(obj);
    }

    public static void normalizeMainChainBridgeFields(Object obj) {
        if (obj == null) return;
        double finalPValue = readDoubleField(obj, "finalP", 0.0d);
        double dynamicGateValue =
                readDoubleField(obj, "dynamicGate", AppConfig.DEFAULT_DYNAMIC_GATE);
        double gateValue = readDoubleField(obj, "gate", 0.0d);
        if (gateValue <= 0.0d) {
            gateValue = dynamicGateValue > 0.0d ? dynamicGateValue : AppConfig.DEFAULT_DYNAMIC_GATE;
            writeDoubleField(obj, "gate", gateValue);
        }
        if (dynamicGateValue <= 0.0d && gateValue > 0.0d) {
            writeDoubleField(obj, "dynamicGate", gateValue);
        }
        double before = readDoubleField(obj, "finalPBeforeDiagnosis", 0.0d);
        if (before <= 0.0d) {
            before = readDoubleField(obj, "finalPBeforeCoverage", 0.0d);
            if (before <= 0.0d) before = readDoubleField(obj, "rawP", finalPValue);
            writeDoubleField(obj, "finalPBeforeDiagnosis", AppConfig.clampProbability(before));
        }
        double after = readDoubleField(obj, "finalPAfterDiagnosis", 0.0d);
        if (after <= 0.0d) {
            after = readDoubleField(obj, "coverageAdjustedP", 0.0d);
            if (after <= 0.0d) after = readDoubleField(obj, "finalPAfterCoverage", 0.0d);
            if (after <= 0.0d) after = finalPValue;
            writeDoubleField(obj, "finalPAfterDiagnosis", AppConfig.clampProbability(after));
        }
        double coverage = readDoubleField(obj, "coverageQuality", 0.0d);
        if (coverage <= 0.0d) {
            coverage = readDoubleField(obj, "coverageQualityScore", 0.0d);
            if (coverage <= 0.0d)
                coverage = readDoubleField(obj, "finalDecisionCoverageQuality", 0.0d);
            if (coverage <= 0.0d) coverage = readDoubleField(obj, "needSupplyMatchScore", 0.0d);
            writeDoubleField(obj, "coverageQuality", AppConfig.clampDynamicCausalScore(coverage));
        }
        double third = readDoubleField(obj, "thirdQuality", 0.0d);
        if (third <= 0.0d) {
            third = readDoubleField(obj, "thirdNumberQualityScore", 0.0d);
            if (third <= 0.0d) third = readDoubleField(obj, "needSupplyThirdQuality", 0.0d);
            if (third <= 0.0d)
                third = readDoubleField(obj, "finalDecisionThirdNumberQuality", 0.0d);
            writeDoubleField(obj, "thirdQuality", AppConfig.clampDynamicCausalScore(third));
        }
        double positionNeed = readDoubleField(obj, "positionNeedScore", 0.0d);
        if (positionNeed <= 0.0d)
            writeDoubleField(
                    obj,
                    "positionNeedScore",
                    AppConfig.clampDynamicCausalScore(
                            readDoubleField(obj, "positionNeedStrength", 0.0d)));
        double numberSupply = readDoubleField(obj, "numberSupplyScore", 0.0d);
        if (numberSupply <= 0.0d)
            writeDoubleField(
                    obj,
                    "numberSupplyScore",
                    AppConfig.clampDynamicCausalScore(
                            readDoubleField(obj, "numberSupplyStrength", 0.0d)));
        double counter = readDoubleField(obj, "counterEvidenceScore", 0.0d);
        if (counter <= 0.0d) {
            counter = Math.max(counter, readDoubleField(obj, "evidenceRisk", 0.0d));
            counter = Math.max(counter, readDoubleField(obj, "k3CoverageRisk", 0.0d));
            counter = Math.max(counter, readDoubleField(obj, "dynamicClusterCounterScore", 0.0d));
            counter = Math.max(counter, readDoubleField(obj, "counterfactualWhyNotRisk", 0.0d));
            writeDoubleField(
                    obj, "counterEvidenceScore", AppConfig.clampDynamicCausalScore(counter));
        }
        int ss = readIntField(obj, "similarSampleCount", 0);
        if (ss <= 0) {
            ss = readIntField(obj, "similarWindowSample", 0);
            if (ss <= 0) ss = readIntField(obj, "dynamicClusterSample", 0);
            writeIntField(obj, "similarSampleCount", ss);
        }
        String adaptive = readStringField(obj, "adaptiveStatus", "");
        if (adaptive.length() == 0 || AppConfig.CLUSTER_STATUS_UNKNOWN.equals(adaptive)) {
            adaptive = readStringField(obj, "adaptiveJudgeAction", "");
            if (adaptive.length() == 0)
                adaptive =
                        readStringField(
                                obj, "adaptiveJudgeStatus", AppConfig.ADAPTIVE_ACTION_WATCH);
            writeStringFieldAlways(obj, "adaptiveStatus", adaptive);
        }
        String promote = readStringField(obj, "shadowPromotionStatus", "");
        if (promote.length() == 0 || AppConfig.CLUSTER_STATUS_UNKNOWN.equals(promote)) {
            promote = readStringField(obj, "promotionClusterStatus", "");
            if (promote.length() == 0)
                promote = readStringField(obj, "promotionStatus", AppConfig.ADAPTIVE_ACTION_WATCH);
            writeStringFieldAlways(obj, "shadowPromotionStatus", promote);
        }
        String reject = readStringField(obj, "rejectCode", "");
        if (reject.length() == 0) {
            reject = readStringField(obj, "finalDecisionRejectCode", "");
            if (reject.length() == 0) reject = readStringField(obj, "blockCode", "");
            writeStringFieldAlways(obj, "rejectCode", reject);
        }
        clampDoubleField(obj, "positionNeedScore");
        clampDoubleField(obj, "numberSupplyScore");
        clampDoubleField(obj, "coverageQuality");
        clampDoubleField(obj, "thirdQuality");
        clampDoubleField(obj, "counterEvidenceScore");
        writeDoubleField(
                obj,
                "finalPBeforeDiagnosis",
                AppConfig.clampProbability(readDoubleField(obj, "finalPBeforeDiagnosis", 0.0d)));
        writeDoubleField(
                obj,
                "finalPAfterDiagnosis",
                AppConfig.clampProbability(readDoubleField(obj, "finalPAfterDiagnosis", 0.0d)));
    }

    public static void copyMainChainBridgeFields(Object from, Object to) {
        if (from == null || to == null) return;
        String[] names = mainChainBridgeFieldNames();
        for (int i = 0; i < names.length; i++) copyPublicField(from, to, names[i]);
        normalizeMainChainBridgeFields(to);
    }

    public static boolean hasExecutableMainChainBridge(Object obj) {
        if (obj == null) return false;
        normalizeMainChainBridgeFields(obj);
        return readDoubleField(obj, "gate", 0.0d) > 0.0d
                && readDoubleField(obj, "finalPAfterDiagnosis", 0.0d) >= 0.0d
                && readStringField(obj, "windowStateKey", "").length() > 0;
    }

    private static void writeStringFieldAlways(Object obj, String name, String value) {
        try {
            java.lang.reflect.Field f = obj.getClass().getField(name);
            f.set(obj, value == null ? "" : value);
        } catch (Throwable ignored) {
        }
    }

    private static void writeDoubleField(Object obj, String name, double value) {
        try {
            java.lang.reflect.Field f = obj.getClass().getField(name);
            f.setDouble(obj, value);
        } catch (Throwable ignored) {
        }
    }

    private static void writeDoubleFieldIfEmpty(Object obj, String name, double value) {
        try {
            java.lang.reflect.Field f = obj.getClass().getField(name);
            double old = f.getDouble(obj);
            if (old == 0.0d) f.setDouble(obj, value);
        } catch (Throwable ignored) {
        }
    }

    private static double readDoubleField(Object obj, String name, double def) {
        try {
            java.lang.reflect.Field f = obj.getClass().getField(name);
            double v = f.getDouble(obj);
            if (Double.isNaN(v) || Double.isInfinite(v)) return def;
            return v;
        } catch (Throwable ignored) {
            return def;
        }
    }

    private static void writeIntField(Object obj, String name, int value) {
        try {
            java.lang.reflect.Field f = obj.getClass().getField(name);
            f.setInt(obj, value);
        } catch (Throwable ignored) {
        }
    }

    private static void writeIntFieldIfEmpty(Object obj, String name, int value) {
        try {
            java.lang.reflect.Field f = obj.getClass().getField(name);
            int old = f.getInt(obj);
            if (old == 0) f.setInt(obj, value);
        } catch (Throwable ignored) {
        }
    }

    private static int readIntField(Object obj, String name, int def) {
        try {
            java.lang.reflect.Field f = obj.getClass().getField(name);
            return f.getInt(obj);
        } catch (Throwable ignored) {
            return def;
        }
    }

    public static String[] dynamicCausalBridgeFieldNames() {
        return new String[] {
            "dynamicClusterId",
            "dynamicClusterStatus",
            "dynamicClusterPrototypeKey",
            "dynamicClusterFeatureKey",
            "dynamicClusterSample",
            "dynamicClusterHitCount",
            "dynamicClusterMissCount",
            "dynamicClusterRecentSample",
            "dynamicClusterRecentHitCount",
            "dynamicClusterHitRate",
            "dynamicClusterRecentHitRate",
            "dynamicClusterConfidence",
            "dynamicClusterDistance",
            "dynamicClusterVolatility",
            "dynamicClusterMaxLose",
            "dynamicClusterCounterScore",
            "dynamicClusterVersion",
            "dynamicClusterReason",
            "positionNeedKey",
            "positionNeedSummary",
            "positionNeedStrength",
            "positionNeedChaosRisk",
            "positionNeedRepairScore",
            "positionNeedRelayScore",
            "numberSupplyKey",
            "numberSupplySummary",
            "numberSupplyStrength",
            "numberSupplyRisk",
            "needSupplyMatchKey",
            "needSupplyMatchScore",
            "needSupplyThirdQuality",
            "needSupplyRoleDiversity",
            "needSupplyRisk",
            "needSupplySummary",
            "k3RoleMain",
            "k3RoleAssist",
            "k3RoleDefense",
            "k3RoleMainNumber",
            "k3RoleAssistNumber",
            "k3RoleDefenseNumber",
            "k3RoleStructure",
            "counterfactualReasonKey",
            "counterfactualRank",
            "counterfactualRankScore",
            "counterfactualWhyNotRisk",
            "counterfactualWhyNot",
            "counterfactualSummary",
            "causeAblationKey",
            "causeAblationScore",
            "causeAblationFalseCauseCount",
            "causeAblationSummary",
            "missReasonType",
            "missReasonMatrixKey",
            "missReasonDetail",
            "adaptiveJudgeCandidateCount",
            "adaptiveJudgeStatus",
            "adaptiveJudgeAction",
            "adaptiveJudgeDomain",
            "adaptiveClusterHitRate",
            "adaptiveClusterRecentHitRate",
            "adaptiveClusterSample",
            "adaptiveClusterConfidence",
            "adaptiveCauseScore",
            "adaptiveCounterScore",
            "adaptivePromoteScore",
            "adaptiveFreezeScore",
            "adaptiveJudgeReason",
            "adaptiveJudgeVersion",
            "promotionClusterStatus",
            "promotionClusterHitRate",
            "promotionClusterRecentHitRate",
            "promotionClusterSample",
            "promotionClusterReason",
            "whyBet",
            "whyNoBet",
            "whyHit",
            "whyMiss",
            "dynamicCausalSummary"
        };
    }

    private static void copyPublicField(Object from, Object to, String name) {
        try {
            java.lang.reflect.Field f = from.getClass().getField(name);
            java.lang.reflect.Field t = to.getClass().getField(name);
            t.set(to, f.get(from));
        } catch (Throwable ignored) {
        }
    }

    private static void writeStringField(Object obj, String name, String value) {
        try {
            java.lang.reflect.Field f = obj.getClass().getField(name);
            Object old = f.get(obj);
            if (old == null || String.valueOf(old).length() == 0) {
                f.set(obj, value == null ? "" : value);
            }
        } catch (Throwable ignored) {
        }
    }

    private static String readStringField(Object obj, String name, String def) {
        try {
            java.lang.reflect.Field f = obj.getClass().getField(name);
            Object v = f.get(obj);
            if (v == null) {
                return def == null ? "" : def;
            }
            String s = String.valueOf(v);
            return s.length() == 0 ? (def == null ? "" : def) : s;
        } catch (Throwable ignored) {
            return def == null ? "" : def;
        }
    }

    private static void clampDoubleField(Object obj, String name) {
        try {
            java.lang.reflect.Field f = obj.getClass().getField(name);
            double v = f.getDouble(obj);
            f.setDouble(obj, AppConfig.clampDynamicCausalScore(v));
        } catch (Throwable ignored) {
        }
    }

    public static final class WindowFeatureVector {
        public String vectorKey;
        public String targetIssue;
        public int position;
        public int n1;
        public int n2;
        public int n3;
        public int k3Mask;
        public double trendSlope12;
        public double trendSlope24;
        public double trendSlope48;
        public double trendSlope96;
        public double trendSlope360;
        public double trendLife;
        public double trendFatigue;
        public double omissionPressure;
        public double hotColdTransition;
        public double densityScore;
        public double repeatRhythm;
        public double oddEvenBias;
        public double highLowBias;
        public double tailPressure;
        public double neighborPressure;
        public double crossPositionFlow;
        public double crossPositionCrowd;
        public double sameNumberCrowd;
        public double timingOffset;
        public double counterEvidence;
        public double thirdQuality;
        public String rawKey;
        public String summary;

        public WindowFeatureVector() {
            vectorKey = "";
            targetIssue = "";
            position = 0;
            n1 = 0;
            n2 = 0;
            n3 = 0;
            k3Mask = 0;
            rawKey = "";
            summary = "";
        }

        public void rebuildKey() {
            k3Mask = buildK3Mask(n1, n2, n3);
            rawKey =
                    "P"
                            + position
                            + "|K"
                            + AppConfig.normalizeK3Part(n1, n2, n3)
                            + "|TS12="
                            + bucket(trendSlope12)
                            + "|TS96="
                            + bucket(trendSlope96)
                            + "|OM="
                            + bucket(omissionPressure)
                            + "|TAIL="
                            + bucket(tailPressure)
                            + "|NB="
                            + bucket(neighborPressure)
                            + "|CP="
                            + bucket(crossPositionFlow)
                            + "|CE="
                            + bucket(counterEvidence);
            vectorKey = AppConfig.safeKeyPart(rawKey);
        }

        public String bucket(double v) {
            if (v >= 0.75d) return "H";
            if (v >= 0.55d) return "M";
            if (v >= 0.35d) return "L";
            return "V";
        }

        public WindowFeatureVector copy() {
            WindowFeatureVector w = new WindowFeatureVector();
            w.vectorKey = vectorKey;
            w.targetIssue = targetIssue;
            w.position = position;
            w.n1 = n1;
            w.n2 = n2;
            w.n3 = n3;
            w.k3Mask = k3Mask;
            w.trendSlope12 = trendSlope12;
            w.trendSlope24 = trendSlope24;
            w.trendSlope48 = trendSlope48;
            w.trendSlope96 = trendSlope96;
            w.trendSlope360 = trendSlope360;
            w.trendLife = trendLife;
            w.trendFatigue = trendFatigue;
            w.omissionPressure = omissionPressure;
            w.hotColdTransition = hotColdTransition;
            w.densityScore = densityScore;
            w.repeatRhythm = repeatRhythm;
            w.oddEvenBias = oddEvenBias;
            w.highLowBias = highLowBias;
            w.tailPressure = tailPressure;
            w.neighborPressure = neighborPressure;
            w.crossPositionFlow = crossPositionFlow;
            w.crossPositionCrowd = crossPositionCrowd;
            w.sameNumberCrowd = sameNumberCrowd;
            w.timingOffset = timingOffset;
            w.counterEvidence = counterEvidence;
            w.thirdQuality = thirdQuality;
            w.rawKey = rawKey;
            w.summary = summary;
            return w;
        }
    }

    public static final class PositionNeedState {
        public String needKey;
        public String targetIssue;
        public int position;
        public double needHotContinue;
        public double needColdRepair;
        public double needOmissionReturn;
        public double needHighLowBalance;
        public double needOddEvenBalance;
        public double needTailRepair;
        public double needNeighborRelay;
        public double needCrossPositionFlow;
        public double needCrowdRelease;
        public double needReverseBreak;
        public double needStableRepeat;
        public double needChaosAvoid;
        public double totalNeedStrength;
        public double chaosRisk;
        public String mainNeedType;
        public String summary;

        public PositionNeedState() {
            needKey = "";
            targetIssue = "";
            position = 0;
            mainNeedType = "";
            summary = "";
        }

        public void recalc() {
            needHotContinue = clampDynamicCausalScore(needHotContinue);
            needColdRepair = clampDynamicCausalScore(needColdRepair);
            needOmissionReturn = clampDynamicCausalScore(needOmissionReturn);
            needHighLowBalance = clampDynamicCausalScore(needHighLowBalance);
            needOddEvenBalance = clampDynamicCausalScore(needOddEvenBalance);
            needTailRepair = clampDynamicCausalScore(needTailRepair);
            needNeighborRelay = clampDynamicCausalScore(needNeighborRelay);
            needCrossPositionFlow = clampDynamicCausalScore(needCrossPositionFlow);
            needCrowdRelease = clampDynamicCausalScore(needCrowdRelease);
            needReverseBreak = clampDynamicCausalScore(needReverseBreak);
            needStableRepeat = clampDynamicCausalScore(needStableRepeat);
            needChaosAvoid = clampDynamicCausalScore(needChaosAvoid);
            chaosRisk = needChaosAvoid;
            totalNeedStrength =
                    clampDynamicCausalScore(
                            (needHotContinue
                                            + needColdRepair
                                            + needOmissionReturn
                                            + needHighLowBalance
                                            + needOddEvenBalance
                                            + needTailRepair
                                            + needNeighborRelay
                                            + needCrossPositionFlow
                                            + needCrowdRelease
                                            + needReverseBreak
                                            + needStableRepeat)
                                    / 11.0d);
            mainNeedType = detectMainNeedType();
            needKey =
                    AppConfig.buildNeedSupplyKey(
                            position, 0, mainNeedType, "POSITION_NEED", targetIssue);
        }

        private String detectMainNeedType() {
            double best = needHotContinue;
            String type = AppConfig.NEED_TYPE_HOT_CONTINUE;
            if (needColdRepair > best) {
                best = needColdRepair;
                type = AppConfig.NEED_TYPE_COLD_REPAIR;
            }
            if (needOmissionReturn > best) {
                best = needOmissionReturn;
                type = AppConfig.NEED_TYPE_OMISSION_RETURN;
            }
            if (needHighLowBalance > best) {
                best = needHighLowBalance;
                type = AppConfig.NEED_TYPE_HIGH_LOW_BALANCE;
            }
            if (needOddEvenBalance > best) {
                best = needOddEvenBalance;
                type = AppConfig.NEED_TYPE_ODD_EVEN_BALANCE;
            }
            if (needTailRepair > best) {
                best = needTailRepair;
                type = AppConfig.NEED_TYPE_TAIL_REPAIR;
            }
            if (needNeighborRelay > best) {
                best = needNeighborRelay;
                type = AppConfig.NEED_TYPE_NEIGHBOR_RELAY;
            }
            if (needCrossPositionFlow > best) {
                best = needCrossPositionFlow;
                type = AppConfig.NEED_TYPE_CROSS_POSITION_FLOW;
            }
            if (needCrowdRelease > best) {
                best = needCrowdRelease;
                type = AppConfig.NEED_TYPE_CROWD_RELEASE;
            }
            if (needReverseBreak > best) {
                best = needReverseBreak;
                type = AppConfig.NEED_TYPE_REVERSE_BREAK;
            }
            if (needStableRepeat > best) {
                best = needStableRepeat;
                type = AppConfig.NEED_TYPE_STABLE_REPEAT;
            }
            return type;
        }

        public PositionNeedState copy() {
            PositionNeedState p = new PositionNeedState();
            p.needKey = needKey;
            p.targetIssue = targetIssue;
            p.position = position;
            p.needHotContinue = needHotContinue;
            p.needColdRepair = needColdRepair;
            p.needOmissionReturn = needOmissionReturn;
            p.needHighLowBalance = needHighLowBalance;
            p.needOddEvenBalance = needOddEvenBalance;
            p.needTailRepair = needTailRepair;
            p.needNeighborRelay = needNeighborRelay;
            p.needCrossPositionFlow = needCrossPositionFlow;
            p.needCrowdRelease = needCrowdRelease;
            p.needReverseBreak = needReverseBreak;
            p.needStableRepeat = needStableRepeat;
            p.needChaosAvoid = needChaosAvoid;
            p.totalNeedStrength = totalNeedStrength;
            p.chaosRisk = chaosRisk;
            p.mainNeedType = mainNeedType;
            p.summary = summary;
            return p;
        }
    }

    public static final class NumberSupplyState {
        public String supplyKey;
        public String targetIssue;
        public int position;
        public int number;
        public double supplyHotContinue;
        public double supplyColdRepair;
        public double supplyOmissionReturn;
        public double supplyHighLowBalance;
        public double supplyOddEvenBalance;
        public double supplyTailRepair;
        public double supplyNeighborRelay;
        public double supplyCrossPositionFlow;
        public double supplyCrowdRelease;
        public double supplyReverseBreak;
        public double supplyStableRepeat;
        public double supplyRisk;
        public double totalSupplyStrength;
        public String mainSupplyType;
        public String summary;

        public NumberSupplyState() {
            supplyKey = "";
            targetIssue = "";
            position = 0;
            number = 0;
            mainSupplyType = "";
            summary = "";
        }

        public void recalc() {
            supplyHotContinue = clampDynamicCausalScore(supplyHotContinue);
            supplyColdRepair = clampDynamicCausalScore(supplyColdRepair);
            supplyOmissionReturn = clampDynamicCausalScore(supplyOmissionReturn);
            supplyHighLowBalance = clampDynamicCausalScore(supplyHighLowBalance);
            supplyOddEvenBalance = clampDynamicCausalScore(supplyOddEvenBalance);
            supplyTailRepair = clampDynamicCausalScore(supplyTailRepair);
            supplyNeighborRelay = clampDynamicCausalScore(supplyNeighborRelay);
            supplyCrossPositionFlow = clampDynamicCausalScore(supplyCrossPositionFlow);
            supplyCrowdRelease = clampDynamicCausalScore(supplyCrowdRelease);
            supplyReverseBreak = clampDynamicCausalScore(supplyReverseBreak);
            supplyStableRepeat = clampDynamicCausalScore(supplyStableRepeat);
            supplyRisk = clampDynamicCausalScore(supplyRisk);
            totalSupplyStrength =
                    clampDynamicCausalScore(
                            (supplyHotContinue
                                            + supplyColdRepair
                                            + supplyOmissionReturn
                                            + supplyHighLowBalance
                                            + supplyOddEvenBalance
                                            + supplyTailRepair
                                            + supplyNeighborRelay
                                            + supplyCrossPositionFlow
                                            + supplyCrowdRelease
                                            + supplyReverseBreak
                                            + supplyStableRepeat)
                                    / 11.0d);
            mainSupplyType = detectMainSupplyType();
            supplyKey =
                    AppConfig.buildNeedSupplyKey(
                            position, number, "NUMBER_SUPPLY", mainSupplyType, targetIssue);
        }

        private String detectMainSupplyType() {
            double best = supplyHotContinue;
            String type = AppConfig.NEED_TYPE_HOT_CONTINUE;
            if (supplyColdRepair > best) {
                best = supplyColdRepair;
                type = AppConfig.NEED_TYPE_COLD_REPAIR;
            }
            if (supplyOmissionReturn > best) {
                best = supplyOmissionReturn;
                type = AppConfig.NEED_TYPE_OMISSION_RETURN;
            }
            if (supplyHighLowBalance > best) {
                best = supplyHighLowBalance;
                type = AppConfig.NEED_TYPE_HIGH_LOW_BALANCE;
            }
            if (supplyOddEvenBalance > best) {
                best = supplyOddEvenBalance;
                type = AppConfig.NEED_TYPE_ODD_EVEN_BALANCE;
            }
            if (supplyTailRepair > best) {
                best = supplyTailRepair;
                type = AppConfig.NEED_TYPE_TAIL_REPAIR;
            }
            if (supplyNeighborRelay > best) {
                best = supplyNeighborRelay;
                type = AppConfig.NEED_TYPE_NEIGHBOR_RELAY;
            }
            if (supplyCrossPositionFlow > best) {
                best = supplyCrossPositionFlow;
                type = AppConfig.NEED_TYPE_CROSS_POSITION_FLOW;
            }
            if (supplyCrowdRelease > best) {
                best = supplyCrowdRelease;
                type = AppConfig.NEED_TYPE_CROWD_RELEASE;
            }
            if (supplyReverseBreak > best) {
                best = supplyReverseBreak;
                type = AppConfig.NEED_TYPE_REVERSE_BREAK;
            }
            if (supplyStableRepeat > best) {
                best = supplyStableRepeat;
                type = AppConfig.NEED_TYPE_STABLE_REPEAT;
            }
            return type;
        }

        public NumberSupplyState copy() {
            NumberSupplyState n = new NumberSupplyState();
            n.supplyKey = supplyKey;
            n.targetIssue = targetIssue;
            n.position = position;
            n.number = number;
            n.supplyHotContinue = supplyHotContinue;
            n.supplyColdRepair = supplyColdRepair;
            n.supplyOmissionReturn = supplyOmissionReturn;
            n.supplyHighLowBalance = supplyHighLowBalance;
            n.supplyOddEvenBalance = supplyOddEvenBalance;
            n.supplyTailRepair = supplyTailRepair;
            n.supplyNeighborRelay = supplyNeighborRelay;
            n.supplyCrossPositionFlow = supplyCrossPositionFlow;
            n.supplyCrowdRelease = supplyCrowdRelease;
            n.supplyReverseBreak = supplyReverseBreak;
            n.supplyStableRepeat = supplyStableRepeat;
            n.supplyRisk = supplyRisk;
            n.totalSupplyStrength = totalSupplyStrength;
            n.mainSupplyType = mainSupplyType;
            n.summary = summary;
            return n;
        }
    }

    public static final class NeedSupplyMatchResult {
        public String matchKey;
        public String targetIssue;
        public int position;
        public int n1;
        public int n2;
        public int n3;
        public int k3Mask;
        public int mainRoleNumber;
        public int assistRoleNumber;
        public int defenseRoleNumber;
        public String mainRole;
        public String assistRole;
        public String defenseRole;
        public double matchScore;
        public double mainRoleScore;
        public double assistRoleScore;
        public double defenseRoleScore;
        public double thirdQuality;
        public double roleDiversity;
        public double supplyRisk;
        public String whyMatch;
        public String whyNotMatch;
        public String summary;

        public NeedSupplyMatchResult() {
            matchKey = "";
            targetIssue = "";
            position = 0;
            n1 = 0;
            n2 = 0;
            n3 = 0;
            k3Mask = 0;
            mainRole = AppConfig.DYNAMIC_K3_ROLE_UNKNOWN;
            assistRole = AppConfig.DYNAMIC_K3_ROLE_UNKNOWN;
            defenseRole = AppConfig.DYNAMIC_K3_ROLE_UNKNOWN;
            whyMatch = "";
            whyNotMatch = "";
            summary = "";
        }

        public void recalc() {
            k3Mask = buildK3Mask(n1, n2, n3);
            matchScore = clampDynamicCausalScore(matchScore);
            mainRoleScore = clampDynamicCausalScore(mainRoleScore);
            assistRoleScore = clampDynamicCausalScore(assistRoleScore);
            defenseRoleScore = clampDynamicCausalScore(defenseRoleScore);
            thirdQuality = clampDynamicCausalScore(thirdQuality);
            roleDiversity = clampDynamicCausalScore(roleDiversity);
            supplyRisk = clampDynamicCausalScore(supplyRisk);
            matchKey =
                    AppConfig.buildNeedSupplyKey(
                            position,
                            0,
                            "K3_MATCH",
                            AppConfig.normalizeK3Part(n1, n2, n3),
                            targetIssue);
        }

        public boolean allowsBet() {
            return AppConfig.needSupplyAllowsBet(
                    matchScore, thirdQuality, supplyRisk, roleDiversity);
        }

        public NeedSupplyMatchResult copy() {
            NeedSupplyMatchResult r = new NeedSupplyMatchResult();
            r.matchKey = matchKey;
            r.targetIssue = targetIssue;
            r.position = position;
            r.n1 = n1;
            r.n2 = n2;
            r.n3 = n3;
            r.k3Mask = k3Mask;
            r.mainRoleNumber = mainRoleNumber;
            r.assistRoleNumber = assistRoleNumber;
            r.defenseRoleNumber = defenseRoleNumber;
            r.mainRole = mainRole;
            r.assistRole = assistRole;
            r.defenseRole = defenseRole;
            r.matchScore = matchScore;
            r.mainRoleScore = mainRoleScore;
            r.assistRoleScore = assistRoleScore;
            r.defenseRoleScore = defenseRoleScore;
            r.thirdQuality = thirdQuality;
            r.roleDiversity = roleDiversity;
            r.supplyRisk = supplyRisk;
            r.whyMatch = whyMatch;
            r.whyNotMatch = whyNotMatch;
            r.summary = summary;
            return r;
        }
    }

    public static final class DynamicWindowCluster {
        public String clusterId;
        public String prototypeKey;
        public String prototypeVector;
        public String status;
        public int sampleCount;
        public int hitCount;
        public int missCount;
        public int recentSample;
        public int recentHitCount;
        public double hitRate;
        public double recentHitRate;
        public int maxLoseStreak;
        public double confidence;
        public double volatility;
        public double counterScore;
        public double prototypeDistance;
        public long createdAt;
        public long updatedAt;
        public String reason;

        public DynamicWindowCluster() {
            clusterId = "";
            prototypeKey = "";
            prototypeVector = "";
            status = AppConfig.CLUSTER_STATUS_NEW;
            reason = "";
        }

        public void recalc() {
            hitRate = AppConfig.safeRate(hitCount, sampleCount);
            recentHitRate = AppConfig.safeRate(recentHitCount, recentSample);
            confidence = clampDynamicCausalScore(confidence);
            volatility = clampDynamicCausalScore(volatility);
            counterScore = clampDynamicCausalScore(counterScore);
            prototypeDistance = clampDynamicCausalScore(prototypeDistance);
            status =
                    AppConfig.decideDynamicClusterStatus(
                            sampleCount,
                            recentSample,
                            hitRate,
                            recentHitRate,
                            maxLoseStreak,
                            confidence,
                            volatility,
                            counterScore);
        }

        public boolean allowsBet() {
            return AppConfig.dynamicClusterAllowsBet(
                    sampleCount,
                    recentSample,
                    hitRate,
                    recentHitRate,
                    maxLoseStreak,
                    confidence,
                    volatility,
                    counterScore,
                    status);
        }

        public DynamicWindowCluster copy() {
            DynamicWindowCluster c = new DynamicWindowCluster();
            c.clusterId = clusterId;
            c.prototypeKey = prototypeKey;
            c.prototypeVector = prototypeVector;
            c.status = status;
            c.sampleCount = sampleCount;
            c.hitCount = hitCount;
            c.missCount = missCount;
            c.recentSample = recentSample;
            c.recentHitCount = recentHitCount;
            c.hitRate = hitRate;
            c.recentHitRate = recentHitRate;
            c.maxLoseStreak = maxLoseStreak;
            c.confidence = confidence;
            c.volatility = volatility;
            c.counterScore = counterScore;
            c.prototypeDistance = prototypeDistance;
            c.createdAt = createdAt;
            c.updatedAt = updatedAt;
            c.reason = reason;
            return c;
        }
    }

    public static final class ClusterMatchResult {
        public String clusterId;
        public String clusterStatus;
        public String prototypeKey;
        public double distance;
        public int sampleCount;
        public int recentSample;
        public double hitRate;
        public double recentHitRate;
        public double confidence;
        public double counterScore;
        public int maxLoseStreak;
        public String reason;

        public ClusterMatchResult() {
            clusterId = "";
            clusterStatus = AppConfig.CLUSTER_STATUS_UNKNOWN;
            prototypeKey = "";
            reason = "";
        }

        public boolean matched() {
            return clusterId != null
                    && clusterId.length() > 0
                    && distance <= AppConfig.DYNAMIC_CLUSTER_MAX_DISTANCE_MATCH;
        }

        public boolean allowsBet() {
            return AppConfig.dynamicClusterAllowsBet(
                    sampleCount,
                    recentSample,
                    hitRate,
                    recentHitRate,
                    maxLoseStreak,
                    confidence,
                    0.0d,
                    counterScore,
                    clusterStatus);
        }
    }

    public static final class CounterfactualReasonResult {
        public String reasonKey;
        public String targetIssue;
        public int position;
        public int n1;
        public int n2;
        public int n3;
        public int k3Mask;
        public int positionRank;
        public int k3Rank;
        public double rankScore;
        public double whyNotRisk;
        public double mirrorMissSimilarity;
        public double causeScore;
        public double counterScore;
        public String whyThisPosition;
        public String whyTheseNumbers;
        public String whyNotOtherPosition;
        public String whyNotOtherNumbers;
        public String summary;

        public CounterfactualReasonResult() {
            reasonKey = "";
            targetIssue = "";
            summary = "";
            whyThisPosition = "";
            whyTheseNumbers = "";
            whyNotOtherPosition = "";
            whyNotOtherNumbers = "";
        }

        public void recalc() {
            k3Mask = buildK3Mask(n1, n2, n3);
            rankScore = clampDynamicCausalScore(rankScore);
            whyNotRisk = clampDynamicCausalScore(whyNotRisk);
            mirrorMissSimilarity = clampDynamicCausalScore(mirrorMissSimilarity);
            causeScore = clampDynamicCausalScore(causeScore);
            counterScore = clampDynamicCausalScore(counterScore);
            reasonKey =
                    AppConfig.buildCounterfactualReasonKey(targetIssue, position, n1, n2, n3, "");
        }

        public boolean allowsBet() {
            return rankScore >= AppConfig.COUNTERFACTUAL_READY_RANK_SCORE
                    && whyNotRisk <= AppConfig.COUNTERFACTUAL_MAX_WHY_NOT_RISK
                    && mirrorMissSimilarity <= AppConfig.NEGATIVE_MIRROR_MAX_SIMILARITY;
        }
    }

    public static final class CauseAblationResult {
        public String ablationKey;
        public String clusterId;
        public int sampleCount;
        public double omissionDrop;
        public double tailDrop;
        public double neighborDrop;
        public double crossPositionDrop;
        public double needSupplyDrop;
        public double thirdQualityDrop;
        public double totalAblationScore;
        public int falseCauseCount;
        public String falseCauseSummary;
        public String summary;

        public CauseAblationResult() {
            ablationKey = "";
            clusterId = "";
            falseCauseSummary = "";
            summary = "";
        }

        public void recalc() {
            omissionDrop = clampDynamicCausalScore(omissionDrop);
            tailDrop = clampDynamicCausalScore(tailDrop);
            neighborDrop = clampDynamicCausalScore(neighborDrop);
            crossPositionDrop = clampDynamicCausalScore(crossPositionDrop);
            needSupplyDrop = clampDynamicCausalScore(needSupplyDrop);
            thirdQualityDrop = clampDynamicCausalScore(thirdQualityDrop);
            totalAblationScore =
                    clampDynamicCausalScore(
                            (omissionDrop
                                            + tailDrop
                                            + neighborDrop
                                            + crossPositionDrop
                                            + needSupplyDrop
                                            + thirdQualityDrop)
                                    / 6.0d);
        }

        public boolean hasUsefulCause() {
            return sampleCount >= AppConfig.CAUSE_ABLATION_MIN_SAMPLE
                    && totalAblationScore >= AppConfig.CAUSE_ABLATION_MIN_DROP
                    && falseCauseCount <= 1;
        }
    }

    public static final class MissReasonResult {
        public String missKey;
        public String targetIssue;
        public int position;
        public int n1;
        public int n2;
        public int n3;
        public int k3Mask;
        public int actualNumber;
        public String missType;
        public String clusterId;
        public double windowWrongScore;
        public double positionWrongScore;
        public double mainNumberWrongScore;
        public double thirdDragScore;
        public double counterIgnoredScore;
        public String detail;
        public String nextAdvice;

        public MissReasonResult() {
            missKey = "";
            targetIssue = "";
            missType = AppConfig.MISS_REASON_UNKNOWN;
            clusterId = "";
            detail = "";
            nextAdvice = "";
        }

        public void recalc() {
            k3Mask = buildK3Mask(n1, n2, n3);
            windowWrongScore = clampDynamicCausalScore(windowWrongScore);
            positionWrongScore = clampDynamicCausalScore(positionWrongScore);
            mainNumberWrongScore = clampDynamicCausalScore(mainNumberWrongScore);
            thirdDragScore = clampDynamicCausalScore(thirdDragScore);
            counterIgnoredScore = clampDynamicCausalScore(counterIgnoredScore);
            if (missType == null || missType.length() == 0) {
                missType = AppConfig.MISS_REASON_UNKNOWN;
            }
            missKey = AppConfig.buildMissReasonKey(targetIssue, position, n1, n2, n3, missType);
        }
    }

    public static final class AdaptiveJudgeResult {
        public String judgeId;
        public String targetIssue;
        public String clusterId;
        public String clusterStatus;
        public int candidateCount;
        public String status;
        public String action;
        public String domain;
        public double clusterHitRate;
        public double clusterRecentHitRate;
        public int clusterSample;
        public double clusterConfidence;
        public double causeScore;
        public double counterScore;
        public double promoteScore;
        public double freezeScore;
        public String reason;
        public String version;

        public AdaptiveJudgeResult() {
            judgeId = "";
            targetIssue = "";
            clusterId = "";
            clusterStatus = AppConfig.CLUSTER_STATUS_UNKNOWN;
            status = AppConfig.CLUSTER_STATUS_UNKNOWN;
            action = AppConfig.ADAPTIVE_ACTION_WATCH;
            domain = "";
            reason = "";
            version = V20_MODELS_DYNAMIC_CAUSAL_WINDOW_VERSION;
        }

        public void recalc() {
            clusterStatus = AppConfig.normalizeDynamicClusterStatus(clusterStatus);
            status = AppConfig.normalizeDynamicClusterStatus(status);
            clusterHitRate = clampDynamicCausalScore(clusterHitRate);
            clusterRecentHitRate = clampDynamicCausalScore(clusterRecentHitRate);
            clusterConfidence = clampDynamicCausalScore(clusterConfidence);
            causeScore = clampDynamicCausalScore(causeScore);
            counterScore = clampDynamicCausalScore(counterScore);
            promoteScore = clampDynamicCausalScore(promoteScore);
            freezeScore = clampDynamicCausalScore(freezeScore);
            if (action == null || action.length() == 0) {
                action = AppConfig.ADAPTIVE_ACTION_WATCH;
            }
        }

        public boolean allowsBet() {
            return AppConfig.adaptiveActionAllowsBet(action)
                    && AppConfig.isDynamicClusterStatusBetEligible(clusterStatus)
                    && candidateCount > 0
                    && freezeScore < 0.500d;
        }
    }

    public static final class PromotionDomainState {
        public String domainKey;
        public String clusterId;
        public String status;
        public int sampleCount;
        public int hitCount;
        public int recentSample;
        public int recentHitCount;
        public double hitRate;
        public double recentHitRate;
        public int maxLoseStreak;
        public double confidence;
        public String action;
        public String reason;

        public PromotionDomainState() {
            domainKey = "";
            clusterId = "";
            status = AppConfig.CLUSTER_STATUS_UNKNOWN;
            action = AppConfig.ADAPTIVE_ACTION_WATCH;
            reason = "";
        }

        public void recalc() {
            hitRate = AppConfig.safeRate(hitCount, sampleCount);
            recentHitRate = AppConfig.safeRate(recentHitCount, recentSample);
            status =
                    AppConfig.decideDynamicClusterStatus(
                            sampleCount,
                            recentSample,
                            hitRate,
                            recentHitRate,
                            maxLoseStreak,
                            confidence,
                            0.0d,
                            0.0d);
            action =
                    AppConfig.isDynamicClusterStatusBetEligible(status)
                            ? AppConfig.ADAPTIVE_ACTION_READY
                            : AppConfig.ADAPTIVE_ACTION_WATCH;
        }
    }

    public static final class Draw {
        public long id;
        public String issue;
        public long openTime;
        public long fetchTime;
        public String sourceLine;
        public int[] numbers;
        public String rawJson;
        public int dirty;
        public String dirtyReason;

        public Draw() {
            id = 0L;
            issue = "";
            openTime = 0L;
            fetchTime = 0L;
            sourceLine = AppConfig.API_LINE_PRIMARY;
            numbers = new int[AppConfig.NUMBER_COUNT];
            rawJson = "";
            dirty = 0;
            dirtyReason = "";
        }

        public boolean isValid() {
            if (issue == null || issue.length() == 0) return false;
            if (numbers == null || numbers.length != AppConfig.NUMBER_COUNT) return false;

            boolean[] seen = new boolean[AppConfig.NUMBER_COUNT + 1];
            for (int i = 0; i < numbers.length; i++) {
                int n = numbers[i];
                if (!AppConfig.isValidNumber(n)) return false;
                if (seen[n]) return false;
                seen[n] = true;
            }
            return true;
        }

        public void normalizeDirty() {
            if (isValid()) {
                dirty = 0;
                dirtyReason = "";
            } else {
                dirty = 1;
                if (dirtyReason == null || dirtyReason.length() == 0) {
                    dirtyReason = AppConfig.RESULT_DATA_ERROR;
                }
            }
        }

        public int getNumberAtPosition(int position) {
            if (!AppConfig.isValidPosition(position) || numbers == null) return 0;
            int index = position - 1;
            if (index < 0 || index >= numbers.length) return 0;
            return numbers[index];
        }

        public boolean hitStrictPositionK3(int position, int n1, int n2, int n3) {
            if (!isValid()) return false;
            return AppConfig.isStrictPositionK3Hit(position, n1, n2, n3, numbers);
        }

        public boolean hitStrictPositionK3Mask(int position, int k3Mask) {
            if (!isValid()) return false;
            return AppConfig.isStrictPositionK3HitByMask(position, k3Mask, numbers);
        }

        public String numbersText() {
            if (numbers == null || numbers.length != AppConfig.NUMBER_COUNT) return "";
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < numbers.length; i++) {
                if (i > 0) sb.append(",");
                sb.append(UiFormatter.number(numbers[i]));
            }
            return sb.toString();
        }

        public String toOpenCodeText() {
            return numbersText();
        }

        public Draw copy() {
            Draw d = new Draw();
            d.id = id;
            d.issue = issue;
            d.openTime = openTime;
            d.fetchTime = fetchTime;
            d.sourceLine = sourceLine;
            d.rawJson = rawJson;
            d.dirty = dirty;
            d.dirtyReason = dirtyReason;
            if (numbers != null) {
                d.numbers = new int[numbers.length];
                for (int i = 0; i < numbers.length; i++) {
                    d.numbers[i] = numbers[i];
                }
            }
            return d;
        }
    }

    public static final class ApiResult {
        public boolean success;
        public boolean ok;
        public boolean fromBackup;
        public String sourceLine;
        public String apiLine;
        public String errorCode;
        public String errorMessage;
        public String error;
        public String rawJson;
        public Draw latestDraw;
        public List<Draw> history;
        public List<Draw> draws;

        public ApiResult() {
            success = false;
            ok = false;
            fromBackup = false;
            sourceLine = AppConfig.API_LINE_PRIMARY;
            apiLine = AppConfig.API_LINE_PRIMARY;
            errorCode = "";
            errorMessage = "";
            error = "";
            rawJson = "";
            latestDraw = null;
            history = new ArrayList<Draw>();
            draws = history;
        }

        public void setOk(boolean value) {
            success = value;
            ok = value;
        }

        public void setSourceLine(String source) {
            sourceLine =
                    source == null || source.length() == 0 ? AppConfig.API_LINE_PRIMARY : source;
            apiLine = sourceLine;
            fromBackup = AppConfig.API_LINE_BACKUP.equals(sourceLine);
        }

        public void setError(String code, String message) {
            errorCode = code == null ? "" : code;
            errorMessage = sanitizeError(message);
            error = errorMessage;
        }

        public void setDraws(List<Draw> list) {
            history = list == null ? new ArrayList<Draw>() : list;
            draws = history;
        }

        public static ApiResult fail(String code, String message) {
            ApiResult r = new ApiResult();
            r.setOk(false);
            r.setError(code, message);
            return r;
        }

        public static ApiResult ok(Draw draw, String source) {
            ApiResult r = new ApiResult();
            r.setOk(draw != null && draw.isValid());
            r.latestDraw = draw;
            r.setSourceLine(source);
            if (!r.ok) r.setError(AppConfig.RESULT_DATA_ERROR, "开奖数据无效");
            return r;
        }

        public static ApiResult okList(List<Draw> list, String source) {
            ApiResult r = new ApiResult();
            r.setOk(true);
            r.setSourceLine(source);
            r.setDraws(list);
            if (r.draws != null && r.draws.size() > 0) {
                r.latestDraw = r.draws.get(r.draws.size() - 1);
            }
            return r;
        }

        private static String sanitizeError(String value) {
            if (value == null) return "";
            if (AppConfig.SECURITY_SHOW_KEY_IN_ERROR) return value;
            return value.replace("key=", "key=" + AppConfig.KEY_MASK_TEXT);
        }
    }

    public static final class DataCheck {
        public boolean ok;
        public String code;
        public String message;
        public String reason;
        public String currentIssue;
        public String targetIssue;
        public String actualIssue;
        public int shouldHold;

        public DataCheck() {
            ok = false;
            code = "";
            message = "";
            reason = "";
            currentIssue = "";
            targetIssue = "";
            actualIssue = "";
            shouldHold = 1;
        }

        public static DataCheck pass() {
            DataCheck c = new DataCheck();
            c.ok = true;
            c.code = AppConfig.REASON_PASS;
            c.message = "OK";
            c.reason = "OK";
            c.shouldHold = 0;
            return c;
        }

        public static DataCheck block(String code, String message) {
            DataCheck c = new DataCheck();
            c.ok = false;
            c.code = code == null ? "" : code;
            c.message = message == null ? "" : message;
            c.reason = c.message;
            c.shouldHold = 1;
            return c;
        }
    }

    public static final class Decision {
        /* V48 stage-rank and diagnostic observation audit. */
        public int v48InitialRank;
        public int v48TeacherRank;
        public int v48DynamicRank;
        public int v48CalibratedRank;
        public String v48ChampionStatus;
        public String v48ModelId;
        public String v48FailureCode;

        /* V46唯一实时合同：EventP与ActionLCB严格分离。 */
        public double v46EventProbability;
        public double v46ActionLcb;
        public double v46ProbabilityUncertainty;
        public double v46ExpectedValue;
        public double v46GlobalCalibratedProbability;
        public double v46PositionAdjustment;
        public double v46RankAdjustment;
        public double v46RegimeAdjustment;
        public double v46CalibrationConfidence;
        public int v46FinalRank;
        public int v46FormalCandidateCount;
        public String v46Version;
        public String v46NumberDistributionSummary;
        public String v46ProbabilityTrace;
        public String v46RootCauseCode;
        public String v46RootCauseOwner;
        public String v46FinalContractHash;
        public String v46Lane;

        /* 星河 V40：全号码分布、OOS校准、分层后验、不可变契约与唯一授权。 */
        public double rawRankScore;
        public double calibratedHitProbability;
        public double honestProbability;
        public double actionUtility;
        public double actionStructureScore;
        public double actionDiscriminationScore;
        public double actionMaturityScore;
        public double actionUncertaintyPenalty;
        public double actionFrequencySupport;
        /* V42 unified risk-coverage evidence.  These fields are immutable audit
         * evidence after lane compilation; they never create a BET. */
        public double v42AcceptScore;
        public double v42FamilyLcb;
        public double v42RegimeQuality;
        public double v42AssociationScore;
        public double v42DistillationRisk;
        public int v42SupportVotes;
        public String v42RegimeFamily;
        public String v42RiskCoverageMode;
        public String v42PrimaryReject;
        public String v42SecondaryReject;
        public String v42TertiaryReject;
        public String v42LanePolicyVersion;
        /* V43 canonical full-strategy evidence contract. */
        public double v43StrategySupport;
        public double v43StrategyRisk;
        public double v43StrategyConfidence;
        public double v43StrategyDiversity;
        public double v43StrategyMaturity;
        public double v43SafeProbability;
        public double v43ProbabilityResidual;
        public double v43AblationFragility;
        public double v43ProbabilityLock;
        public int v43EvidenceGroupCount;
        public int v43HardRisk;
        public String v43DominantEvidence;
        public String v43MissingEvidence;
        public String v43EvidenceFingerprint;
        public String v43ProbabilityLockHash;
        public String v43PipelineStage;
        public String v43InvariantStatus;
        public String v43StrategyVersion;
        /* V46 separated event probability, conservative action bound and final funnel. */
        public double v46ConditionalAdjustment;
        public int v46ConditionalSample;
        public String v46ConditionalCalibrationSummary;
        public String v46ContractHash;
        public String actionUtilityVersion;
        public String laneCandidate;
        public int oosRankSlot;
        public int oosCandidateCount;
        public String buildId;
        public String cohortId;
        public String schemaVersion;
        public String contractDecisionId;
        public String candidateHash;
        public String snapshotMaxIssue;
        public String trainedUntilIssue;
        public String featureSchemaVersion;
        public String rankingVersion;
        public String calibrationVersion;
        public String gatePolicyVersion;
        public String pipelineVersion;
        public int contractValid;
        public String contractStatus;
        public String coarseFamilyKey;
        public String midFamilyKey;
        public String fineFamilyKey;
        public String positionFamilyKey;
        public String posteriorMatchedLevel;
        public String posteriorFamilyKey;
        public String posteriorPolicy;
        public String posteriorVersion;
        public int posteriorSample;
        public int posteriorHit;
        public double posteriorMean;
        public double posteriorWilsonLower;
        public double posteriorRecentRate;
        public double posteriorDrift;
        public double familyWilsonLower;
        public int calibrationBucket;
        public int calibrationSample;
        public int calibrationTotalSample;
        public int calibrationReady;
        public String calibrationStatus;
        public double calibrationBrier;
        public String authorizationId;
        public int authorizationValid;
        public String authorizedMode;
        public String authorizationRejectCode;
        public long authorizedAt;
        public double authorizationProbability;
        public double authorizationGate;
        public double authorizationEdge;
        public String teacherPolicy;
        public String dynamicPolicy;
        public String adaptivePolicy;
        public String microWalkForwardStatus;
        public String microWalkForwardVersion;
        public String microWalkForwardAsOfIssue;
        public int microWalkForwardSample;

        public long id;
        public long tradeId;
        public String baseIssue;
        public String targetIssue;
        public long createdAt;
        public long settleTime;
        public long settledAt;

        public String decision;
        public String action;

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

        public double freq100;
        public double freq500;
        public double freq1000;

        public double backtestWinRate;
        public double backtestFrequency;
        public double backtestPnl;
        public double backtestMaxDrawdown;

        public String stateMode;
        public String shapeKey;
        public String stateKey;
        public String trendKey;
        public String driftKey;
        public String breakKey;
        public String recoveryKey;
        public String blockCode;
        public String reasonSummary;
        public String reasonText;

        public String backtestMode;
        public String gateMode;
        public String probabilityMode;

        public int dataOk;
        public int settled;
        public String result;
        public int actualNumber;
        public int hit;
        public double pnl;
        public String missReason;
        public String settlementReason;

        public double deepScore;
        public double deepConfidence;
        public double deepFeatureP;
        public double deepPositionScore;
        public double deepNumberScore;
        public double deepCycleScore;
        public double deepTransitionScore;
        public double deepStateScore;

        public double biasScore;
        public double biasConfidence;
        public double profileScore;
        public double fallbackProfileScore;
        public double runtimeProfileScore;
        public double stateNumberProfileScore;
        public double teacherWeight;
        public int teacherNumberSample;
        public int teacherNumberHistorySample;
        public int teacherDomainShadowSample;
        public int teacherDomainRealSample;
        public double teacherNumberHitRate;
        public double teacherNumberConfidence;
        public double teacherNumberScore;
        public double teacherNumberRisk;
        public String teacherNumberSummary;
        public String teacherApplyError;
        public String preDecisionDomainVersion;
        public String teacherRuntimeApplyVersion;
        public double stateConfidence;
        public double auditPenalty;
        public double auditBoost;
        public double aiJudgeScore;
        public double aiJudgeConfidence;
        public double evidenceScore;
        public double evidenceRisk;
        public double conflictLevel;
        public double aPlusScore;
        public double promotionScore;
        public double weakestNumberScore;
        public double balanceScore;

        public int stateSample;
        public int cacheSample;
        public int recentSample;
        public double recentHitRate;
        public double runtimeHitRate;
        public double numberProfileHitRate;
        public long realtimeCostMs;
        public int realtimeTimeout;

        public String deepProfileKey;
        public double deepProfileHitRate;
        public int deepProfileSample;
        public int deepFallbackLevel;
        public String fallbackProfileKey;

        public String promotionStatus;
        public String aPlusStatus;
        public String candidateEvidenceKey;
        public String snapshotId;

        public String deepAttribution;
        public String deepReason;
        public String aiJudgeSummary;
        public String learningSummary;
        public String selfDiagnosisText;

        public int manualBacktestSupport;
        public String statScope;

        public int teacherMatrixSample;
        public int teacherMatrixPositive;
        public int teacherMatrixNegative;
        public int teacherMatrixStateSample;
        public int teacherMatrixComboSample;
        public int teacherMatrixApplied;
        public double teacherMatrixHitRate;
        public double teacherMatrixRecentHitRate;
        public double teacherMatrixConfidence;
        public double teacherMatrixProbAdjust;
        public double teacherMatrixGateAdjust;
        public double teacherMatrixRiskPenalty;
        public double teacherMatrixScore;
        public double teacherMatrixPositionScore;
        public double teacherMatrixNumberScore;
        public double teacherMatrixComboScore;
        public double teacherMatrixStateScore;
        public String teacherMatrixStatus;
        public String teacherMatrixProfileKey;
        public String teacherMatrixComboKey;
        public String teacherMatrixSummary;

        /* V20 上游证据补实字段：只承接字段，不决定 BET */
        public int teacherWeakNoBetFlag;
        public int teacherMatrixWeakNoBetFlag;
        public int teacherMatrixOrdinaryBetBlockFlag;
        public int teacherMatrixSupportsFinalGate;
        public int teacherMatrixSupportsBet;
        public int teacherMatrixRiskFlag;
        public int teacherMatrixFreezeFlag;
        public String teacherOrdinaryBetPolicy;
        public String teacherMatrixOrdinaryBetPolicy;
        public String teacherFieldSource;
        public String teacherFieldVersion;
        public String teacherNumberStatus;
        public String teacherStatus;
        public String teacherStatusClass;
        public double teacherEvidenceContribution;
        public double teacherMatrixEvidenceContribution;
        public double teacherCounterContribution;
        public double teacherMatrixCounterContribution;
        public double teacherSampleReliability;
        public double teacherRecentValidity;

        public String v20Status;
        public String dynamicStatus;
        public String dynamicCauseStatus;
        public String v20ActionAdvice;
        public String v20FieldSource;
        public String v20FieldVersion;
        public int v20EmptyFlag;
        public double v20SupportScore;
        public double v20RiskScore;
        public double v20EvidenceContribution;
        public double v20CounterContribution;
        public double v20SampleReliability;
        public double v20RecentValidity;
        public int similarWindowSample;
        public double similarWindowHitRate;
        public String v20WindowStateKey;
        public String windowStateKey;
        public String finalDecisionWindowStateKey;
        public String dynamicWindowStateKey;
        public String probabilityWindowStateKey;

        public double coverageQualityScore;
        public double thirdNumberQualityScore;
        public double k3CoverageRisk;
        public int falseHighPFlag;
        public int probabilityFalseHighPFlag;
        public double probabilityFalseHighRisk;
        public int probabilityPreferredEdgeBand;
        public int probabilityHighEdgeFlag;
        public int probabilityPreferredPBand;
        public int probabilityTeacherWeakOrEmpty;
        public int probabilityV20UnknownOrEmpty;
        public int probabilityStrongCauseFlag;
        public double coverageProbDiscount;
        public double coverageProbBoost;
        public double coverageK3HitRate32;
        public double coverageK3HitRate64;
        public double coverageK3HitRate128;
        public double coverageNumberRate1;
        public double coverageNumberRate2;
        public double coverageNumberRate3;
        public double finalPBeforeCoverage;
        public double finalPAfterCoverage;
        public double coverageAdjustedP;
        public String probabilityCoverageSummary;
        public String coverageQualitySummary;
        public String probabilityCoverageActionAdvice;
        public String probabilityActionAdvice;
        public String probabilityFieldVersion;

        public String dynamicGateQualityStatus;
        public int dynamicGateFalseHighPFlag;
        public int dynamicGatePreferredEdgeBand;
        public int dynamicGateTeacherV20WeakFlag;
        public String dynamicGateEdgePolicy;
        public double dynamicGateCoverageRisk;
        public double dynamicGateCoverageQualityScore;
        public double dynamicGateThirdNumberQuality;
        public double dynamicGateVirtualEdgeRisk;
        public String dynamicGateActionAdvice;
        public String dynamicGateFieldVersion;
        public String dynamicGateVersion;

        public String finalDecisionVersion;
        public String finalDecisionEdgePolicy;
        public String finalDecisionTeacherPolicy;
        public String finalDecisionV20Policy;
        public String finalDecisionBetDomain;
        public String finalDecisionRejectCode;
        public String finalDecisionShadowReason;
        public String finalDecisionStrongOverrideTrace;
        public double finalDecisionCoverageRisk;
        public double finalDecisionCoverageQuality;
        public double finalDecisionThirdNumberQuality;
        public int finalDecisionFalseHighPFlag;
        /* V20主链闭环字段：只承接字段，不计算概率，不决定BET */
        public String clusterKey;
        public String factorKey;
        public double positionNeedScore;
        public double numberSupplyScore;
        public double coverageQuality;
        public double thirdQuality;
        public double counterEvidenceScore;
        public int similarSampleCount;
        public String adaptiveStatus;
        public String shadowPromotionStatus;
        public double finalPBeforeDiagnosis;
        public double finalPAfterDiagnosis;
        public double gate;
        public String rejectCode;
        public int realProbeAllowed;

        /* 73487246：统一真实证据与结构化风险合同。 */
        public int preDecisionOpportunity;
        public String riskSeverity;
        public int hardBlockFlag;
        public String hardBlockSource;
        public String evidenceSource;
        public int evidenceSettledOnly;
        public int evidenceSampleCount;
        public int evidenceHitCount;
        public int evidenceMissCount;
        public String promotionCoarseDomain;
        public String promotionMidDomain;
        public String promotionFineDomain;

        /* 链路修改升级V2字段：只承接，不计算概率，不决定BET */
        public double realBetFrequency50;
        public double realBetFrequency100;
        public double realBetFrequency300;
        public double frequencyDebtScore;
        public double frequencySupportScore;
        public double frequencyGateAdjust;
        public double qualifiedProbeScore;
        public double shadowPromotionScore;
        public double baseGate;
        public double riskAdjustedGate;
        public double effectiveProbeGate;
        public double rawEdge;
        public double effectiveProbeEdge;
        public double needSupplyScore;
        public double counterRisk;
        public int qualifiedProbe;
        public int realTrade;
        public int coverageAvailable;
        public int thirdQualityAvailable;
        public int promotionDomainAvailable;
        public String frequencyState;
        public String frequencyStateVersion;
        public String shadowPromotionDomain;
        public String decisionPath;
        public String betDomain;
        public String probeRejectReason;
        public String probeEligibilityVersion;
        public String ruleHealthStatus;
        public String snapshotFreshness;
        public String warmProfileFreshness;
        public String coldProfileFreshness;
        public String deepProfileFreshness;
        public String decisionVersionV2;
        public String evidenceAsOfIssue;
        public String snapshotAsOfIssue;
        public long snapshotCreatedAt;
        public long frequencyStateUpdatedAt;
        public int frequencyFloorRequiredCount;
        public int frequencyFloorTargetCount;
        public int frequencyFloorActualCount;
        public int frequencyFloorDebtCount;
        public int frequencyTargetDebtCount;
        public int frequencyFloorMandatory;
        public int frequencyFloorNoUpperCap;
        public double frequencyFloorRankScore;
        public String frequencyFloorVersion;

        /* 73487246：四窗口平滑频率、冻结域、Teacher/Dynamic分层审计。 */
        public double realBetFrequency20;
        public int frequencyObserved20;
        public int frequencyObserved50;
        public int frequencyObserved100;
        public int frequencyObserved300;
        public int frequencyBetCount20;
        public int frequencyBetCount50;
        public int frequencyBetCount100;
        public int frequencyBetCount300;
        public int frequencyDebt20;
        public int frequencyDebt50;
        public int frequencyDebt100;
        public int frequencyDebt300;
        public int frequencyTargetDebt20;
        public int frequencyTargetDebt50;
        public int frequencyTargetDebt100;
        public int frequencyTargetDebt300;
        public String frequencyUrgentWindow;
        public int frequencyIssuesSinceLastRealBet;
        public int frequencyIssuesSinceLastFloorBet;
        public int frequencyConsecutiveFloorBets;
        public int frequencyFloorMinGap;
        public int frequencyFloorPacingReady;
        public String frequencyFloorPacingReason;
        public String decisionQualityPath;
        public int evidenceDomainFrozen;
        public String evidenceDomainFrozenIssue;
        public long evidenceDomainFrozenAt;
        public String dynamicMatchedLevel;
        public int dynamicRealSample;
        public int dynamicShadowSample;
        public String moduleBuildVersion;

        /* 73487246：有限大类→中类→细类历史先验及贝叶斯融合审计。 */
        public double historicalPriorP;
        public double historicalPriorConfidence;
        public int historicalPriorSample;
        public String historicalPriorCoarseKey;
        public String historicalPriorMidKey;
        public String historicalPriorSummary;
        public double basePBeforeFusion;
        public double finalPAfterFusion;
        public double fusionWeightBase;
        public double fusionWeightPrior;
        public double fusionWeightTeacher;
        public double fusionWeightDynamic;
        public String fusionSummary;
        /* V40 full-distribution audit carried from Candidate into Decision. */
        public double evidenceForestP;
        public double evidenceForestConfidence;
        public int evidenceForestSample;
        public int evidenceForestTreeCount;
        public String evidenceForestSummary;

        /* V20动态因果窗口字段：只承接字段，不直接决定BET */
        public String dynamicClusterId;
        public String dynamicClusterStatus;
        public String dynamicClusterPrototypeKey;
        public String dynamicClusterFeatureKey;
        public int dynamicClusterSample;
        public int dynamicClusterHitCount;
        public int dynamicClusterMissCount;
        public int dynamicClusterRecentSample;
        public int dynamicClusterRecentHitCount;
        public double dynamicClusterHitRate;
        public double dynamicClusterRecentHitRate;
        public double dynamicClusterConfidence;
        public double dynamicClusterDistance;
        public double dynamicClusterVolatility;
        public int dynamicClusterMaxLose;
        public double dynamicClusterCounterScore;
        public String dynamicClusterVersion;
        public String dynamicClusterReason;

        public String positionNeedKey;
        public String positionNeedSummary;
        public double positionNeedStrength;
        public double positionNeedChaosRisk;
        public double positionNeedRepairScore;
        public double positionNeedRelayScore;
        public String numberSupplyKey;
        public String numberSupplySummary;
        public double numberSupplyStrength;
        public double numberSupplyRisk;
        public String needSupplyMatchKey;
        public double needSupplyMatchScore;
        public double needSupplyThirdQuality;
        public double needSupplyRoleDiversity;
        public double needSupplyRisk;
        public String needSupplySummary;

        public String k3RoleMain;
        public String k3RoleAssist;
        public String k3RoleDefense;
        public int k3RoleMainNumber;
        public int k3RoleAssistNumber;
        public int k3RoleDefenseNumber;
        public String k3RoleStructure;

        public String counterfactualReasonKey;
        public int counterfactualRank;
        public double counterfactualRankScore;
        public double counterfactualWhyNotRisk;
        public String counterfactualWhyNot;
        public String counterfactualSummary;
        public String causeAblationKey;
        public double causeAblationScore;
        public int causeAblationFalseCauseCount;
        public String causeAblationSummary;
        public String missReasonType;
        public String missReasonMatrixKey;
        public String missReasonDetail;

        public int adaptiveJudgeCandidateCount;
        public String adaptiveJudgeStatus;
        public String adaptiveJudgeAction;
        public String adaptiveJudgeDomain;
        public double adaptiveClusterHitRate;
        public double adaptiveClusterRecentHitRate;
        public int adaptiveClusterSample;
        public double adaptiveClusterConfidence;
        public double adaptiveCauseScore;
        public double adaptiveCounterScore;
        public double adaptivePromoteScore;
        public double adaptiveFreezeScore;
        public String adaptiveJudgeReason;
        public String adaptiveJudgeVersion;

        public String promotionClusterStatus;
        public double promotionClusterHitRate;
        public double promotionClusterRecentHitRate;
        public int promotionClusterSample;
        public String promotionClusterReason;

        public String whyBet;
        public String whyNoBet;
        public String whyHit;
        public String whyMiss;
        public String dynamicCausalSummary;

        public String sourceType;
        public int isRealtimeReal;

        public String preDrawReasonFingerprint;
        public String preDrawReasonText;
        public String reasonValidLevel;
        public double reasonScore;
        public double reasonEdge;
        public double hitSimilarity;
        public double missSimilarity;
        public double contradictionScore;
        public double causalLift;
        public double causalConfidence;
        public int validReasonCount;
        public int falseReasonCount;
        public int forwardReasonSample;
        public double forwardReasonHitRate;
        public double reasonWilsonScore;

        public String k3RoleSummary;
        public int k3MainNumber;
        public int k3SupportNumber;
        public int k3DefenseNumber;
        public double comboRoleDiversity;
        public double comboConflict;

        public int fingerprintCooling;
        public int elite45Passed;
        public String elite45Status;
        public String elite45BlockCode;
        public double elite45Gate;
        public String elite45Summary;

        /* V20最终后台常驻与证据成熟审计字段 */
        public String fixCohort;
        public String probabilityPipelineVersion;
        public String auditClusterKey;
        public String learningClusterKey;
        public double coverageRequired;
        public double coverageConfidence;
        public double coverageContribution;
        public String scanRejectCode;
        public String displayRejectCode;
        /* 73487246：Teacher新鲜度、观察/硬风险分层、受控探针实体字段。 */
        public String teacherAsOfIssue;
        public int teacherIssueLag;
        public int teacherFreshByIssue;
        public long teacherSnapshotCreatedAt;
        public String blockTier;
        public String hardRiskCode;
        public String observeReason;
        public int probeEligible;
        public int shadowProbeEligible;
        public String probeLevel;
        public int shadowSettledCount;
        public int shadowDomainSample;
        public double promotionWilsonLowerBound;
        public String promotionEligibilityReason;

        public Decision() {
            fixCohort = AppConfig.FIX_COHORT;
            probabilityPipelineVersion = AppConfig.PROBABILITY_PIPELINE_VERSION;
            auditClusterKey = "";
            learningClusterKey = "";
            coverageQuality = 0.0d;
            coverageRequired = AppConfig.COVERAGE_REQUIRED_DEFAULT;
            coverageConfidence = 0.0d;
            coverageContribution = 0.0d;
            scanRejectCode = "";
            displayRejectCode = "";
            id = 0L;
            tradeId = 0L;
            baseIssue = "";
            targetIssue = "";
            createdAt = 0L;
            settleTime = 0L;
            settledAt = 0L;

            decision = AppConfig.DECISION_HOLD;
            action = AppConfig.ACTION_HOLD;

            position = 0;
            n1 = 0;
            n2 = 0;
            n3 = 0;
            k3Mask = 0;

            rawP = 0.0d;
            calibratedP = 0.0d;
            finalP = 0.0d;
            dynamicGate = AppConfig.DEFAULT_DYNAMIC_GATE;
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

            freq100 = 0.0d;
            freq500 = 0.0d;
            freq1000 = 0.0d;

            backtestWinRate = 0.0d;
            backtestFrequency = 0.0d;
            backtestPnl = 0.0d;
            backtestMaxDrawdown = 0.0d;

            stateMode = AppConfig.MODE_CHAOS;
            shapeKey = "";
            stateKey = "";
            trendKey = "";
            driftKey = "";
            breakKey = "";
            recoveryKey = "";
            blockCode = "";
            reasonSummary = "";
            reasonText = "";

            backtestMode = AppConfig.BACKTEST_MODE_FAST;
            gateMode = "";
            probabilityMode = "";

            dataOk = 0;
            settled = 0;
            result = AppConfig.RESULT_WAIT;
            actualNumber = 0;
            hit = 0;
            pnl = 0.0d;
            missReason = "";
            settlementReason = "";

            deepScore = 0.0d;
            deepConfidence = 0.0d;
            deepFeatureP = 0.0d;
            deepPositionScore = 0.0d;
            deepNumberScore = 0.0d;
            deepCycleScore = 0.0d;
            deepTransitionScore = 0.0d;
            deepStateScore = 0.0d;

            biasScore = 0.0d;
            biasConfidence = 0.0d;
            profileScore = 0.0d;
            fallbackProfileScore = 0.0d;
            runtimeProfileScore = 0.0d;
            stateNumberProfileScore = 0.0d;
            teacherWeight = 1.0d;
            stateConfidence = 0.0d;
            auditPenalty = 0.0d;
            auditBoost = 0.0d;
            aiJudgeScore = 0.0d;
            aiJudgeConfidence = 0.0d;
            evidenceScore = 0.0d;
            evidenceRisk = 0.0d;
            conflictLevel = 0.0d;
            aPlusScore = 0.0d;
            promotionScore = 0.0d;
            weakestNumberScore = 0.0d;
            balanceScore = 0.0d;

            stateSample = 0;
            cacheSample = 0;
            recentSample = 0;
            recentHitRate = 0.0d;
            runtimeHitRate = 0.0d;
            numberProfileHitRate = 0.0d;
            realtimeCostMs = 0L;
            realtimeTimeout = 0;

            deepProfileKey = "";
            deepProfileHitRate = 0.0d;
            deepProfileSample = 0;
            deepFallbackLevel = 0;
            fallbackProfileKey = "";

            promotionStatus = AppConfig.PROMOTION_CANDIDATE;
            aPlusStatus = AppConfig.A_PLUS_NONE;
            candidateEvidenceKey = "";
            snapshotId = "";

            deepAttribution = "";
            deepReason = "";
            aiJudgeSummary = "";
            learningSummary = "";
            selfDiagnosisText = "";

            manualBacktestSupport = 0;
            statScope = AppConfig.STAT_SCOPE_HOLD;

            teacherMatrixSample = 0;
            teacherMatrixPositive = 0;
            teacherMatrixNegative = 0;
            teacherMatrixStateSample = 0;
            teacherMatrixComboSample = 0;
            teacherMatrixApplied = 0;
            teacherMatrixHitRate = 0.0d;
            teacherMatrixRecentHitRate = 0.0d;
            teacherMatrixConfidence = 0.0d;
            teacherMatrixProbAdjust = 0.0d;
            teacherMatrixGateAdjust = 0.0d;
            teacherMatrixRiskPenalty = 0.0d;
            teacherMatrixScore = 0.0d;
            teacherMatrixPositionScore = 0.0d;
            teacherMatrixNumberScore = 0.0d;
            teacherMatrixComboScore = 0.0d;
            teacherMatrixStateScore = 0.0d;
            teacherMatrixStatus = AppConfig.TEACHER_MATRIX_STATUS_NONE;
            teacherMatrixProfileKey = "";
            teacherMatrixComboKey = "";
            teacherMatrixSummary = "";

            teacherWeakNoBetFlag = 0;
            teacherMatrixWeakNoBetFlag = 0;
            teacherMatrixOrdinaryBetBlockFlag = 0;
            teacherMatrixSupportsFinalGate = 0;
            teacherMatrixSupportsBet = 0;
            teacherMatrixRiskFlag = 0;
            teacherMatrixFreezeFlag = 0;
            teacherOrdinaryBetPolicy = "";
            teacherMatrixOrdinaryBetPolicy = "";
            teacherFieldSource = "";
            teacherFieldVersion = "";
            teacherNumberStatus = "";
            teacherStatus = "";
            teacherStatusClass = "";
            teacherEvidenceContribution = 0.0d;
            teacherMatrixEvidenceContribution = 0.0d;
            teacherCounterContribution = 0.0d;
            teacherMatrixCounterContribution = 0.0d;
            teacherSampleReliability = 0.0d;
            teacherRecentValidity = 0.0d;

            v20Status = "";
            dynamicStatus = "";
            dynamicCauseStatus = "";
            v20ActionAdvice = "";
            v20FieldSource = "";
            v20FieldVersion = "";
            v20EmptyFlag = 0;
            v20SupportScore = 0.0d;
            v20RiskScore = 0.0d;
            v20EvidenceContribution = 0.0d;
            v20CounterContribution = 0.0d;
            v20SampleReliability = 0.0d;
            v20RecentValidity = 0.0d;
            similarWindowSample = 0;
            similarWindowHitRate = 0.0d;
            v20WindowStateKey = "";
            windowStateKey = "";
            finalDecisionWindowStateKey = "";
            dynamicWindowStateKey = "";
            probabilityWindowStateKey = "";

            coverageQualityScore = 0.0d;
            thirdNumberQualityScore = 0.0d;
            k3CoverageRisk = 0.0d;
            falseHighPFlag = 0;
            probabilityFalseHighPFlag = 0;
            probabilityFalseHighRisk = 0.0d;
            probabilityPreferredEdgeBand = 0;
            probabilityHighEdgeFlag = 0;
            probabilityPreferredPBand = 0;
            probabilityTeacherWeakOrEmpty = 0;
            probabilityV20UnknownOrEmpty = 0;
            probabilityStrongCauseFlag = 0;
            coverageProbDiscount = 0.0d;
            coverageProbBoost = 0.0d;
            coverageK3HitRate32 = 0.0d;
            coverageK3HitRate64 = 0.0d;
            coverageK3HitRate128 = 0.0d;
            coverageNumberRate1 = 0.0d;
            coverageNumberRate2 = 0.0d;
            coverageNumberRate3 = 0.0d;
            finalPBeforeCoverage = 0.0d;
            finalPAfterCoverage = 0.0d;
            coverageAdjustedP = 0.0d;
            probabilityCoverageSummary = "";
            coverageQualitySummary = "";
            probabilityCoverageActionAdvice = "";
            probabilityActionAdvice = "";
            probabilityFieldVersion = "";

            dynamicGateQualityStatus = "";
            dynamicGateFalseHighPFlag = 0;
            dynamicGatePreferredEdgeBand = 0;
            dynamicGateTeacherV20WeakFlag = 0;
            dynamicGateEdgePolicy = "";
            dynamicGateCoverageRisk = 0.0d;
            dynamicGateCoverageQualityScore = 0.0d;
            dynamicGateThirdNumberQuality = 0.0d;
            dynamicGateVirtualEdgeRisk = 0.0d;
            dynamicGateActionAdvice = "";
            dynamicGateFieldVersion = "";
            dynamicGateVersion = "";

            finalDecisionVersion = "";
            finalDecisionEdgePolicy = "";
            finalDecisionTeacherPolicy = "";
            finalDecisionV20Policy = "";
            finalDecisionBetDomain = "";
            finalDecisionRejectCode = "";
            finalDecisionShadowReason = "";
            finalDecisionStrongOverrideTrace = "";
            finalDecisionCoverageRisk = 0.0d;
            finalDecisionCoverageQuality = 0.0d;
            finalDecisionThirdNumberQuality = 0.0d;
            finalDecisionFalseHighPFlag = 0;
            initDynamicCausalBridgeFields(this);
            initMainChainBridgeFields(this);

            sourceType = SOURCE_TYPE_UNKNOWN;
            isRealtimeReal = 0;

            preDrawReasonFingerprint = "";
            preDrawReasonText = "";
            reasonValidLevel = REASON_LEVEL_C_EXPLAIN_ONLY;
            reasonScore = 0.0d;
            reasonEdge = 0.0d;
            hitSimilarity = 0.0d;
            missSimilarity = 0.0d;
            contradictionScore = 0.0d;
            causalLift = 0.0d;
            causalConfidence = 0.0d;
            validReasonCount = 0;
            falseReasonCount = 0;
            forwardReasonSample = 0;
            forwardReasonHitRate = 0.0d;
            reasonWilsonScore = 0.0d;

            k3RoleSummary = "";
            k3MainNumber = 0;
            k3SupportNumber = 0;
            k3DefenseNumber = 0;
            comboRoleDiversity = 0.0d;
            comboConflict = 0.0d;

            fingerprintCooling = 0;
            elite45Passed = 0;
            elite45Status = BLOCK_ELITE45_NOT_PASS;
            elite45BlockCode = BLOCK_ELITE45_NOT_PASS;
            elite45Gate = ELITE45_GATE_DEFAULT;
            elite45Summary = "";
        }

        public boolean isBet() {
            return AppConfig.isBetDecision(decision) || AppConfig.isBetDecision(action);
        }

        public boolean isHold() {
            return AppConfig.isHoldDecision(decision) || AppConfig.isHoldDecision(action);
        }

        public boolean isShadow() {
            return AppConfig.isShadowDecision(decision) || AppConfig.isShadowDecision(action);
        }

        public void setNumbers(int a, int b, int c) {
            n1 = a;
            n2 = b;
            n3 = c;
            k3Mask = buildK3Mask(a, b, c);
        }

        public void normalize() {
            decision = AppConfig.normalizeDecision(decision);
            action = decision;
            result = AppConfig.normalizeResult(result);
            statScope = AppConfig.normalizeStatScopeForDecision(decision, statScope);
            sourceType = normalizeSourceType(sourceType);
            k3Mask = buildK3Mask(n1, n2, n3);

            finalP = AppConfig.clampProbability(finalP);
            rawP = AppConfig.clampProbability(rawP);
            calibratedP = AppConfig.clampProbability(calibratedP);
            dynamicGate = AppConfig.clampGate(dynamicGate);
            edge = finalP - dynamicGate;

            reasonValidLevel = normalizeReasonLevel(reasonValidLevel);
            normalizeDynamicCausalBridgeFields(this);
            reasonScore = AppConfig.clampProbability(reasonScore);
            hitSimilarity = AppConfig.clampProbability(hitSimilarity);
            missSimilarity = AppConfig.clampProbability(missSimilarity);
            contradictionScore = AppConfig.clampProbability(contradictionScore);
            causalConfidence = AppConfig.clampProbability(causalConfidence);
            comboRoleDiversity = AppConfig.clampProbability(comboRoleDiversity);
            comboConflict = AppConfig.clampProbability(comboConflict);
            elite45Gate = clampElite45Gate(elite45Gate);

            if (reasonText == null || reasonText.length() == 0) {
                reasonText = reasonSummary == null ? "" : reasonSummary;
            }
            if (createdAt <= 0L) {
                createdAt = System.currentTimeMillis();
            }
        }

        public boolean hasValidBetContent() {
            return AppConfig.isValidPosition(position) && isValidK3Mask(k3Mask);
        }

        public boolean hasLegalPlayRule() {
            return AppConfig.isPlayRuleLegal(position, AppConfig.FIXED_K, n1, n2, n3);
        }

        public boolean isRealStatBet() {
            normalize();
            return shouldCountInStrictRealStats(
                    decision, statScope, sourceType, isRealtimeReal == 1, settled == 1);
        }

        public boolean shouldCreateRealTrade() {
            normalize();
            return AppConfig.DECISION_BET.equals(decision)
                    && AppConfig.isRealTradeScope(statScope)
                    && SOURCE_TYPE_REALTIME.equals(sourceType)
                    && isRealtimeReal == 1
                    && hasLegalPlayRule()
                    && DecisionAuthorizationEngine.isDecisionAuthorized(this)
                    && targetIssue != null
                    && targetIssue.length() > 0;
        }

        public String displayNumbers() {
            return UiFormatter.numberGroup(n1, n2, n3);
        }

        public String displayPositionNumbers() {
            return UiFormatter.positionAndNumbers(position, n1, n2, n3);
        }

        public void syncAction() {
            normalize();
        }

        public void applySettlementFromTrade(Trade t) {
            if (t == null) return;
            tradeId = t.id;
            settled = t.settled;
            result = AppConfig.normalizeResult(t.result);
            actualNumber = t.actualNumber;
            hit = t.hit;
            pnl = t.pnl;
            missReason = t.missReason == null ? "" : t.missReason;
            settleTime = t.settleTime;
            settledAt = t.settledAt;
            settlementReason = t.settlementReason == null ? "" : t.settlementReason;
        }

        public void applyAiJudge(AiJudgeResult judge) {
            if (judge == null) return;
            aiJudgeScore = judge.judgeScore;
            aiJudgeConfidence = judge.judgeConfidence;
            conflictLevel = judge.conflictLevel;
            aiJudgeSummary = judge.summaryText == null ? "" : judge.summaryText;

            if (judge.bestCandidate != null) {
                candidateEvidenceKey = judge.bestCandidate.evidenceKey;
                evidenceScore = judge.bestCandidate.evidenceScore;
                evidenceRisk = judge.bestCandidate.riskScore;
            }
        }

        public void applyTeacherMatrix(
                TeacherMatrixProfile profile, TeacherMatrixComboProfile comboProfile) {
            if (profile != null) {
                profile.recalc();
                teacherMatrixSample = profile.sampleCount;
                teacherMatrixPositive = profile.positiveCount;
                teacherMatrixNegative = profile.negativeCount;
                teacherMatrixHitRate = profile.hitRate;
                teacherMatrixRecentHitRate = profile.recentHitRate;
                teacherMatrixConfidence = profile.confidence;
                teacherMatrixProbAdjust = profile.probAdjust;
                teacherMatrixGateAdjust = profile.gateAdjust;
                teacherMatrixRiskPenalty = profile.riskPenalty;
                teacherMatrixPositionScore = profile.positionScore;
                teacherMatrixNumberScore = profile.numberScore;
                teacherMatrixStateScore = profile.stateScore;
                teacherMatrixStatus = profile.status;
                teacherMatrixProfileKey = profile.profileKey;
                teacherMatrixSummary = appendReason(teacherMatrixSummary, profile.summaryText);

                if (profile.canApplyRealtime()) {
                    teacherMatrixApplied = 1;
                    finalP =
                            AppConfig.clampProbability(
                                    finalP + teacherMatrixProbAdjust - teacherMatrixRiskPenalty);
                    dynamicGate = AppConfig.clampGate(dynamicGate + teacherMatrixGateAdjust);
                    edge = finalP - dynamicGate;
                }
            }

            if (comboProfile != null) {
                comboProfile.recalc();
                teacherMatrixComboSample = comboProfile.sampleCount;
                teacherMatrixComboScore = comboProfile.comboScore;
                teacherMatrixComboKey = comboProfile.comboKey;
                teacherMatrixSummary = appendReason(teacherMatrixSummary, comboProfile.summaryText);
                if (comboProfile.status != null
                        && comboProfile.status.length() > 0
                        && (teacherMatrixStatus == null
                                || teacherMatrixStatus.length() == 0
                                || AppConfig.TEACHER_MATRIX_STATUS_NONE.equals(
                                        teacherMatrixStatus))) {
                    teacherMatrixStatus = comboProfile.status;
                }
                if (comboProfile.canApplyRealtime()) {
                    teacherMatrixApplied = 1;
                }
            }
            teacherMatrixScore =
                    buildTeacherMatrixScore(
                            teacherMatrixNumberScore,
                            teacherMatrixPositionScore,
                            teacherMatrixStateScore,
                            teacherMatrixComboScore);
        }

        public void applyRealtimeValidation(RealtimeValidationReport report) {
            if (report == null) return;
            report.recalc();
            backtestWinRate = report.hitRate;
            backtestFrequency = report.frequency;
            backtestPnl = report.pnl;
            backtestMaxDrawdown = report.maxDrawdown;
            backtestMode = AppConfig.BACKTEST_MODE_FAST;
            if (report.usable == 1) {
                dynamicGate = AppConfig.clampGate(dynamicGate + report.gateAdjust);
                edge = finalP - dynamicGate;
            }
            String text = report.summary == null ? "" : report.summary;
            reasonSummary = appendReason(reasonSummary, text);
            reasonText = reasonSummary;
        }

        public void applyRealtimeFastResult(RealtimeFastResult r) {
            if (r == null) return;

            baseIssue = r.baseIssue;
            targetIssue = r.targetIssue;
            decision = r.decisionType;
            action = r.decisionType;
            position = r.position;
            setNumbers(r.n1, r.n2, r.n3);

            finalP = r.finalP;
            dynamicGate = r.dynamicGate;
            edge = r.edge;
            evidenceRisk = AppConfig.clampProbability(r.riskScore);

            stateKey = r.stateKey;
            shapeKey = r.shapeKey;
            trendKey = r.trendKey;
            driftKey = r.driftKey;
            breakKey = r.breakKey;
            recoveryKey = r.recoveryKey;
            stateMode =
                    r.stateMode == null || r.stateMode.length() == 0
                            ? AppConfig.MODE_UNKNOWN
                            : r.stateMode;

            stateSample = r.stateSample;
            cacheSample = r.cacheSample;
            recentSample = r.recentSample;
            recentHitRate = r.recentHitRate;
            runtimeProfileScore = r.profileScore;
            stateNumberProfileScore = r.numberBias;
            teacherWeight = r.teacherWeight;
            stateConfidence = r.stateConfidence;
            realtimeCostMs = r.usedTimeMs;
            realtimeTimeout = r.timeoutFlag;
            blockCode = r.blockCode;
            reasonSummary = r.reasonSummary;
            reasonText = r.reasonText;

            teacherMatrixSample = r.teacherMatrixSample;
            teacherMatrixStateSample = r.teacherMatrixStateSample;
            teacherMatrixComboSample = r.teacherMatrixComboSample;
            teacherMatrixHitRate = r.teacherMatrixHitRate;
            teacherMatrixRecentHitRate = r.teacherMatrixRecentHitRate;
            teacherMatrixConfidence = r.teacherMatrixConfidence;
            teacherMatrixProbAdjust = r.teacherMatrixProbAdjust;
            teacherMatrixGateAdjust = r.teacherMatrixGateAdjust;
            teacherMatrixRiskPenalty = r.teacherMatrixRiskPenalty;
            teacherMatrixScore =
                    r.teacherMatrixScore > 0.0d
                            ? r.teacherMatrixScore
                            : buildTeacherMatrixScore(
                                    r.teacherMatrixNumberScore,
                                    r.teacherMatrixPositionScore,
                                    r.teacherMatrixStateScore,
                                    r.teacherMatrixComboScore);
            teacherMatrixPositionScore = r.teacherMatrixPositionScore;
            teacherMatrixNumberScore = r.teacherMatrixNumberScore;
            teacherMatrixComboScore = r.teacherMatrixComboScore;
            teacherMatrixStateScore = r.teacherMatrixStateScore;
            teacherMatrixApplied = r.teacherMatrixApplied;
            teacherMatrixStatus = r.teacherMatrixStatus;
            teacherMatrixProfileKey = r.teacherMatrixProfileKey;
            teacherMatrixComboKey = r.teacherMatrixComboKey;
            teacherMatrixSummary = r.teacherMatrixSummary;
            teacherWeakNoBetFlag = r.teacherWeakNoBetFlag;
            teacherMatrixWeakNoBetFlag = r.teacherMatrixWeakNoBetFlag;
            teacherMatrixOrdinaryBetBlockFlag = r.teacherMatrixOrdinaryBetBlockFlag;
            teacherMatrixSupportsFinalGate = r.teacherMatrixSupportsFinalGate;
            teacherMatrixSupportsBet = r.teacherMatrixSupportsBet;
            teacherMatrixRiskFlag = r.teacherMatrixRiskFlag;
            teacherMatrixFreezeFlag = r.teacherMatrixFreezeFlag;
            teacherOrdinaryBetPolicy = r.teacherOrdinaryBetPolicy;
            teacherMatrixOrdinaryBetPolicy = r.teacherMatrixOrdinaryBetPolicy;
            teacherFieldSource = r.teacherFieldSource;
            teacherFieldVersion = r.teacherFieldVersion;
            teacherNumberStatus = r.teacherNumberStatus;
            teacherStatus = r.teacherStatus;
            teacherStatusClass = r.teacherStatusClass;
            teacherEvidenceContribution = r.teacherEvidenceContribution;
            teacherMatrixEvidenceContribution = r.teacherMatrixEvidenceContribution;
            teacherCounterContribution = r.teacherCounterContribution;
            teacherMatrixCounterContribution = r.teacherMatrixCounterContribution;
            teacherSampleReliability = r.teacherSampleReliability;
            teacherRecentValidity = r.teacherRecentValidity;
            v20Status = r.v20Status;
            dynamicStatus = r.dynamicStatus;
            dynamicCauseStatus = r.dynamicCauseStatus;
            v20ActionAdvice = r.v20ActionAdvice;
            v20FieldSource = r.v20FieldSource;
            v20FieldVersion = r.v20FieldVersion;
            v20EmptyFlag = r.v20EmptyFlag;
            v20SupportScore = r.v20SupportScore;
            v20RiskScore = r.v20RiskScore;
            v20EvidenceContribution = r.v20EvidenceContribution;
            v20CounterContribution = r.v20CounterContribution;
            v20SampleReliability = r.v20SampleReliability;
            v20RecentValidity = r.v20RecentValidity;
            similarWindowSample = r.similarWindowSample;
            similarWindowHitRate = r.similarWindowHitRate;
            v20WindowStateKey = r.v20WindowStateKey;
            windowStateKey = r.windowStateKey;
            finalDecisionWindowStateKey = r.finalDecisionWindowStateKey;
            dynamicWindowStateKey = r.dynamicWindowStateKey;
            probabilityWindowStateKey = r.probabilityWindowStateKey;
            coverageQualityScore = r.coverageQualityScore;
            thirdNumberQualityScore = r.thirdNumberQualityScore;
            k3CoverageRisk = r.k3CoverageRisk;
            falseHighPFlag = r.falseHighPFlag;
            probabilityFalseHighPFlag = r.probabilityFalseHighPFlag;
            probabilityFalseHighRisk = r.probabilityFalseHighRisk;
            probabilityPreferredEdgeBand = r.probabilityPreferredEdgeBand;
            probabilityHighEdgeFlag = r.probabilityHighEdgeFlag;
            probabilityPreferredPBand = r.probabilityPreferredPBand;
            probabilityTeacherWeakOrEmpty = r.probabilityTeacherWeakOrEmpty;
            probabilityV20UnknownOrEmpty = r.probabilityV20UnknownOrEmpty;
            probabilityStrongCauseFlag = r.probabilityStrongCauseFlag;
            coverageProbDiscount = r.coverageProbDiscount;
            coverageProbBoost = r.coverageProbBoost;
            coverageK3HitRate32 = r.coverageK3HitRate32;
            coverageK3HitRate64 = r.coverageK3HitRate64;
            coverageK3HitRate128 = r.coverageK3HitRate128;
            coverageNumberRate1 = r.coverageNumberRate1;
            coverageNumberRate2 = r.coverageNumberRate2;
            coverageNumberRate3 = r.coverageNumberRate3;
            finalPBeforeCoverage = r.finalPBeforeCoverage;
            finalPAfterCoverage = r.finalPAfterCoverage;
            coverageAdjustedP = r.coverageAdjustedP;
            probabilityCoverageSummary = r.probabilityCoverageSummary;
            coverageQualitySummary = r.coverageQualitySummary;
            probabilityCoverageActionAdvice = r.probabilityCoverageActionAdvice;
            probabilityActionAdvice = r.probabilityActionAdvice;
            probabilityFieldVersion = r.probabilityFieldVersion;
            dynamicGateQualityStatus = r.dynamicGateQualityStatus;
            dynamicGateFalseHighPFlag = r.dynamicGateFalseHighPFlag;
            dynamicGatePreferredEdgeBand = r.dynamicGatePreferredEdgeBand;
            dynamicGateTeacherV20WeakFlag = r.dynamicGateTeacherV20WeakFlag;
            dynamicGateEdgePolicy = r.dynamicGateEdgePolicy;
            dynamicGateCoverageRisk = r.dynamicGateCoverageRisk;
            dynamicGateCoverageQualityScore = r.dynamicGateCoverageQualityScore;
            dynamicGateThirdNumberQuality = r.dynamicGateThirdNumberQuality;
            dynamicGateVirtualEdgeRisk = r.dynamicGateVirtualEdgeRisk;
            dynamicGateActionAdvice = r.dynamicGateActionAdvice;
            dynamicGateFieldVersion = r.dynamicGateFieldVersion;
            dynamicGateVersion = r.dynamicGateVersion;
            finalDecisionVersion = r.finalDecisionVersion;
            finalDecisionEdgePolicy = r.finalDecisionEdgePolicy;
            finalDecisionTeacherPolicy = r.finalDecisionTeacherPolicy;
            finalDecisionV20Policy = r.finalDecisionV20Policy;
            finalDecisionBetDomain = r.finalDecisionBetDomain;
            finalDecisionRejectCode = r.finalDecisionRejectCode;
            finalDecisionShadowReason = r.finalDecisionShadowReason;
            finalDecisionStrongOverrideTrace = r.finalDecisionStrongOverrideTrace;
            finalDecisionCoverageRisk = r.finalDecisionCoverageRisk;
            finalDecisionCoverageQuality = r.finalDecisionCoverageQuality;
            finalDecisionThirdNumberQuality = r.finalDecisionThirdNumberQuality;
            finalDecisionFalseHighPFlag = r.finalDecisionFalseHighPFlag;

            preDrawReasonFingerprint = r.preDrawReasonFingerprint;
            preDrawReasonText = r.preDrawReasonText;
            reasonValidLevel = r.reasonValidLevel;
            reasonScore = r.reasonScore;
            reasonEdge = r.reasonEdge;
            hitSimilarity = r.hitSimilarity;
            missSimilarity = r.missSimilarity;
            contradictionScore = r.contradictionScore;
            causalLift = r.causalLift;
            causalConfidence = r.causalConfidence;
            validReasonCount = r.validReasonCount;
            falseReasonCount = r.falseReasonCount;
            forwardReasonSample = r.forwardReasonSample;
            forwardReasonHitRate = r.forwardReasonHitRate;
            reasonWilsonScore = r.reasonWilsonScore;

            k3RoleSummary = r.k3RoleSummary;
            k3MainNumber = r.k3MainNumber;
            k3SupportNumber = r.k3SupportNumber;
            k3DefenseNumber = r.k3DefenseNumber;
            comboRoleDiversity = r.comboRoleDiversity;
            comboConflict = r.comboConflict;

            fingerprintCooling = r.fingerprintCooling;
            elite45Passed = r.elite45Passed;
            elite45Status = r.elite45Status;
            elite45BlockCode = r.elite45BlockCode;
            elite45Gate = r.elite45Gate;
            elite45Summary = r.elite45Summary;

            sourceType = SOURCE_TYPE_REALTIME;
            isRealtimeReal =
                    AppConfig.DECISION_BET.equals(AppConfig.normalizeDecision(decision)) ? 1 : 0;
            statScope =
                    AppConfig.DECISION_BET.equals(AppConfig.normalizeDecision(decision))
                            ? AppConfig.STAT_SCOPE_REAL_TRADE
                            : (AppConfig.DECISION_SHADOW.equals(
                                            AppConfig.normalizeDecision(decision))
                                    ? AppConfig.STAT_SCOPE_SHADOW
                                    : AppConfig.STAT_SCOPE_HOLD);

            syncAction();
        }

        public String upstreamEvidenceSummary() {
            StringBuilder sb = new StringBuilder();
            sb.append("Teacher=").append(teacherMatrixStatus == null ? "" : teacherMatrixStatus);
            sb.append(" Policy=")
                    .append(teacherOrdinaryBetPolicy == null ? "" : teacherOrdinaryBetPolicy);
            sb.append(" V20=").append(v20Status == null ? "" : v20Status);
            sb.append(" Window=").append(windowStateKey == null ? "" : windowStateKey);
            sb.append(" Cover=").append(coverageQualityScore).append("/").append(k3CoverageRisk);
            sb.append(" Third=").append(thirdNumberQualityScore);
            sb.append(" Reject=")
                    .append(finalDecisionRejectCode == null ? "" : finalDecisionRejectCode);
            sb.append(" Cluster=")
                    .append(dynamicClusterId == null ? "" : dynamicClusterId)
                    .append("/")
                    .append(dynamicClusterStatus == null ? "" : dynamicClusterStatus);
            sb.append(" NeedSupply=")
                    .append(needSupplyMatchScore)
                    .append(" ThirdQ=")
                    .append(needSupplyThirdQuality);
            sb.append(" Judge=").append(adaptiveJudgeAction == null ? "" : adaptiveJudgeAction);
            return sb.toString();
        }

        public Trade toTrade() {
            normalize();
            Trade t = new Trade();
            t.applyDecision(this);
            PredictionContractEngine.stampTrade(this, t);
            t.createdAt = System.currentTimeMillis();
            t.betTime = t.createdAt;
            t.result = AppConfig.RESULT_WAIT;
            t.settled = 0;
            t.hit = 0;
            t.pnl = 0.0d;
            if (DecisionAuthorizationEngine.isDecisionAuthorized(this) && t.contractValid == 1) {
                t.statScope = AppConfig.STAT_SCOPE_REAL_TRADE;
                t.sourceType = SOURCE_TYPE_REALTIME;
                t.isRealtimeReal = 1;
            } else {
                t.statScope = AppConfig.STAT_SCOPE_SHADOW;
                t.sourceType = SOURCE_TYPE_REALTIME;
                t.isRealtimeReal = 0;
                t.authorizationValid = 0;
            }
            return t;
        }

        public Decision copy() {
            Decision d = new Decision();
            d.v46EventProbability = v46EventProbability;
            d.v46ActionLcb = v46ActionLcb;
            d.v46ProbabilityUncertainty = v46ProbabilityUncertainty;
            d.v46ExpectedValue = v46ExpectedValue;
            d.v46ConditionalAdjustment = v46ConditionalAdjustment;
            d.v46ConditionalSample = v46ConditionalSample;
            d.v46FinalRank = v46FinalRank;
            d.v46FormalCandidateCount = v46FormalCandidateCount;
            d.v46ProbabilityTrace = v46ProbabilityTrace;
            d.v46NumberDistributionSummary = v46NumberDistributionSummary;
            d.v46ConditionalCalibrationSummary = v46ConditionalCalibrationSummary;
            d.v46RootCauseCode = v46RootCauseCode;
            d.v46RootCauseOwner = v46RootCauseOwner;
            d.v46ContractHash = v46ContractHash;
            d.v46Version = v46Version;
            d.fixCohort = fixCohort;
            d.probabilityPipelineVersion = probabilityPipelineVersion;
            d.auditClusterKey = auditClusterKey;
            d.learningClusterKey = learningClusterKey;
            d.coverageQuality = coverageQuality;
            d.coverageRequired = coverageRequired;
            d.coverageConfidence = coverageConfidence;
            d.coverageContribution = coverageContribution;
            d.scanRejectCode = scanRejectCode;
            d.displayRejectCode = displayRejectCode;
            d.id = id;
            d.tradeId = tradeId;
            d.baseIssue = baseIssue;
            d.targetIssue = targetIssue;
            d.createdAt = createdAt;
            d.settleTime = settleTime;
            d.settledAt = settledAt;
            d.decision = decision;
            d.action = action;
            d.position = position;
            d.n1 = n1;
            d.n2 = n2;
            d.n3 = n3;
            d.k3Mask = k3Mask;
            d.rawP = rawP;
            d.calibratedP = calibratedP;
            d.finalP = finalP;
            d.dynamicGate = dynamicGate;
            d.edge = edge;
            d.modelP = modelP;
            d.shapeP = shapeP;
            d.comboP = comboP;
            d.recoveryP = recoveryP;
            d.similarP = similarP;
            d.memoryP = memoryP;
            d.calibrationError = calibrationError;
            d.similarHitRate = similarHitRate;
            d.comboHitRate = comboHitRate;
            d.effectiveSample = effectiveSample;
            d.comboSample = comboSample;
            d.trendLife = trendLife;
            d.driftRisk = driftRisk;
            d.breakDepth = breakDepth;
            d.recoveryScore = recoveryScore;
            d.championGap = championGap;
            d.comboGap = comboGap;
            d.hotReverseRisk = hotReverseRisk;
            d.falseStrongRisk = falseStrongRisk;
            d.freq100 = freq100;
            d.freq500 = freq500;
            d.freq1000 = freq1000;
            d.backtestWinRate = backtestWinRate;
            d.backtestFrequency = backtestFrequency;
            d.backtestPnl = backtestPnl;
            d.backtestMaxDrawdown = backtestMaxDrawdown;
            d.stateMode = stateMode;
            d.shapeKey = shapeKey;
            d.stateKey = stateKey;
            d.trendKey = trendKey;
            d.driftKey = driftKey;
            d.breakKey = breakKey;
            d.recoveryKey = recoveryKey;
            d.blockCode = blockCode;
            d.reasonSummary = reasonSummary;
            d.reasonText = reasonText;
            d.backtestMode = backtestMode;
            d.gateMode = gateMode;
            d.probabilityMode = probabilityMode;
            d.dataOk = dataOk;
            d.settled = settled;
            d.result = result;
            d.actualNumber = actualNumber;
            d.hit = hit;
            d.pnl = pnl;
            d.missReason = missReason;
            d.settlementReason = settlementReason;

            d.deepScore = deepScore;
            d.deepConfidence = deepConfidence;
            d.deepFeatureP = deepFeatureP;
            d.deepPositionScore = deepPositionScore;
            d.deepNumberScore = deepNumberScore;
            d.deepCycleScore = deepCycleScore;
            d.deepTransitionScore = deepTransitionScore;
            d.deepStateScore = deepStateScore;

            d.biasScore = biasScore;
            d.biasConfidence = biasConfidence;
            d.profileScore = profileScore;
            d.fallbackProfileScore = fallbackProfileScore;
            d.runtimeProfileScore = runtimeProfileScore;
            d.stateNumberProfileScore = stateNumberProfileScore;
            d.teacherWeight = teacherWeight;
            d.teacherNumberSample = teacherNumberSample;
            d.teacherNumberHistorySample = teacherNumberHistorySample;
            d.teacherDomainShadowSample = teacherDomainShadowSample;
            d.teacherDomainRealSample = teacherDomainRealSample;
            d.teacherNumberHitRate = teacherNumberHitRate;
            d.teacherNumberConfidence = teacherNumberConfidence;
            d.teacherNumberScore = teacherNumberScore;
            d.teacherNumberRisk = teacherNumberRisk;
            d.teacherNumberSummary = teacherNumberSummary;
            d.teacherApplyError = teacherApplyError;
            d.preDecisionDomainVersion = preDecisionDomainVersion;
            d.teacherRuntimeApplyVersion = teacherRuntimeApplyVersion;
            d.stateConfidence = stateConfidence;
            d.auditPenalty = auditPenalty;
            d.auditBoost = auditBoost;
            d.aiJudgeScore = aiJudgeScore;
            d.aiJudgeConfidence = aiJudgeConfidence;
            d.evidenceScore = evidenceScore;
            d.evidenceRisk = evidenceRisk;
            d.conflictLevel = conflictLevel;
            d.aPlusScore = aPlusScore;
            d.promotionScore = promotionScore;
            d.weakestNumberScore = weakestNumberScore;
            d.balanceScore = balanceScore;

            d.stateSample = stateSample;
            d.cacheSample = cacheSample;
            d.recentSample = recentSample;
            d.recentHitRate = recentHitRate;
            d.runtimeHitRate = runtimeHitRate;
            d.numberProfileHitRate = numberProfileHitRate;
            d.realtimeCostMs = realtimeCostMs;
            d.realtimeTimeout = realtimeTimeout;

            d.deepProfileKey = deepProfileKey;
            d.deepProfileHitRate = deepProfileHitRate;
            d.deepProfileSample = deepProfileSample;
            d.deepFallbackLevel = deepFallbackLevel;
            d.fallbackProfileKey = fallbackProfileKey;

            d.promotionStatus = promotionStatus;
            d.aPlusStatus = aPlusStatus;
            d.candidateEvidenceKey = candidateEvidenceKey;
            d.snapshotId = snapshotId;

            d.deepAttribution = deepAttribution;
            d.deepReason = deepReason;
            d.aiJudgeSummary = aiJudgeSummary;
            d.learningSummary = learningSummary;
            d.selfDiagnosisText = selfDiagnosisText;

            d.manualBacktestSupport = manualBacktestSupport;
            d.statScope = statScope;

            d.teacherMatrixSample = teacherMatrixSample;
            d.teacherMatrixPositive = teacherMatrixPositive;
            d.teacherMatrixNegative = teacherMatrixNegative;
            d.teacherMatrixStateSample = teacherMatrixStateSample;
            d.teacherMatrixComboSample = teacherMatrixComboSample;
            d.teacherMatrixApplied = teacherMatrixApplied;
            d.teacherMatrixHitRate = teacherMatrixHitRate;
            d.teacherMatrixRecentHitRate = teacherMatrixRecentHitRate;
            d.teacherMatrixConfidence = teacherMatrixConfidence;
            d.teacherMatrixProbAdjust = teacherMatrixProbAdjust;
            d.teacherMatrixGateAdjust = teacherMatrixGateAdjust;
            d.teacherMatrixRiskPenalty = teacherMatrixRiskPenalty;
            d.teacherMatrixScore = teacherMatrixScore;
            d.teacherMatrixPositionScore = teacherMatrixPositionScore;
            d.teacherMatrixNumberScore = teacherMatrixNumberScore;
            d.teacherMatrixComboScore = teacherMatrixComboScore;
            d.teacherMatrixStateScore = teacherMatrixStateScore;
            d.teacherMatrixStatus = teacherMatrixStatus;
            d.teacherMatrixProfileKey = teacherMatrixProfileKey;
            d.teacherMatrixComboKey = teacherMatrixComboKey;
            d.teacherMatrixSummary = teacherMatrixSummary;

            d.sourceType = sourceType;
            d.isRealtimeReal = isRealtimeReal;
            d.preDrawReasonFingerprint = preDrawReasonFingerprint;
            d.preDrawReasonText = preDrawReasonText;
            d.reasonValidLevel = reasonValidLevel;
            d.reasonScore = reasonScore;
            d.reasonEdge = reasonEdge;
            d.hitSimilarity = hitSimilarity;
            d.missSimilarity = missSimilarity;
            d.contradictionScore = contradictionScore;
            d.causalLift = causalLift;
            d.causalConfidence = causalConfidence;
            d.validReasonCount = validReasonCount;
            d.falseReasonCount = falseReasonCount;
            d.forwardReasonSample = forwardReasonSample;
            d.forwardReasonHitRate = forwardReasonHitRate;
            d.reasonWilsonScore = reasonWilsonScore;
            d.k3RoleSummary = k3RoleSummary;
            d.k3MainNumber = k3MainNumber;
            d.k3SupportNumber = k3SupportNumber;
            d.k3DefenseNumber = k3DefenseNumber;
            d.comboRoleDiversity = comboRoleDiversity;
            d.comboConflict = comboConflict;
            d.fingerprintCooling = fingerprintCooling;
            d.elite45Passed = elite45Passed;
            d.elite45Status = elite45Status;
            d.elite45BlockCode = elite45BlockCode;
            d.elite45Gate = elite45Gate;
            d.elite45Summary = elite45Summary;
            d.teacherWeakNoBetFlag = teacherWeakNoBetFlag;
            d.teacherMatrixWeakNoBetFlag = teacherMatrixWeakNoBetFlag;
            d.teacherMatrixOrdinaryBetBlockFlag = teacherMatrixOrdinaryBetBlockFlag;
            d.teacherMatrixSupportsFinalGate = teacherMatrixSupportsFinalGate;
            d.teacherMatrixSupportsBet = teacherMatrixSupportsBet;
            d.teacherMatrixRiskFlag = teacherMatrixRiskFlag;
            d.teacherMatrixFreezeFlag = teacherMatrixFreezeFlag;
            d.teacherOrdinaryBetPolicy = teacherOrdinaryBetPolicy;
            d.teacherMatrixOrdinaryBetPolicy = teacherMatrixOrdinaryBetPolicy;
            d.teacherFieldSource = teacherFieldSource;
            d.teacherFieldVersion = teacherFieldVersion;
            d.teacherNumberStatus = teacherNumberStatus;
            d.teacherStatus = teacherStatus;
            d.teacherStatusClass = teacherStatusClass;
            d.teacherEvidenceContribution = teacherEvidenceContribution;
            d.teacherMatrixEvidenceContribution = teacherMatrixEvidenceContribution;
            d.teacherCounterContribution = teacherCounterContribution;
            d.teacherMatrixCounterContribution = teacherMatrixCounterContribution;
            d.teacherSampleReliability = teacherSampleReliability;
            d.teacherRecentValidity = teacherRecentValidity;
            d.v20Status = v20Status;
            d.dynamicStatus = dynamicStatus;
            d.dynamicCauseStatus = dynamicCauseStatus;
            d.v20ActionAdvice = v20ActionAdvice;
            d.v20FieldSource = v20FieldSource;
            d.v20FieldVersion = v20FieldVersion;
            d.v20EmptyFlag = v20EmptyFlag;
            d.v20SupportScore = v20SupportScore;
            d.v20RiskScore = v20RiskScore;
            d.v20EvidenceContribution = v20EvidenceContribution;
            d.v20CounterContribution = v20CounterContribution;
            d.v20SampleReliability = v20SampleReliability;
            d.v20RecentValidity = v20RecentValidity;
            d.similarWindowSample = similarWindowSample;
            d.similarWindowHitRate = similarWindowHitRate;
            d.v20WindowStateKey = v20WindowStateKey;
            d.windowStateKey = windowStateKey;
            d.finalDecisionWindowStateKey = finalDecisionWindowStateKey;
            d.dynamicWindowStateKey = dynamicWindowStateKey;
            d.probabilityWindowStateKey = probabilityWindowStateKey;
            d.coverageQualityScore = coverageQualityScore;
            d.thirdNumberQualityScore = thirdNumberQualityScore;
            d.k3CoverageRisk = k3CoverageRisk;
            d.falseHighPFlag = falseHighPFlag;
            d.probabilityFalseHighPFlag = probabilityFalseHighPFlag;
            d.probabilityFalseHighRisk = probabilityFalseHighRisk;
            d.probabilityPreferredEdgeBand = probabilityPreferredEdgeBand;
            d.probabilityHighEdgeFlag = probabilityHighEdgeFlag;
            d.probabilityPreferredPBand = probabilityPreferredPBand;
            d.probabilityTeacherWeakOrEmpty = probabilityTeacherWeakOrEmpty;
            d.probabilityV20UnknownOrEmpty = probabilityV20UnknownOrEmpty;
            d.probabilityStrongCauseFlag = probabilityStrongCauseFlag;
            d.coverageProbDiscount = coverageProbDiscount;
            d.coverageProbBoost = coverageProbBoost;
            d.coverageK3HitRate32 = coverageK3HitRate32;
            d.coverageK3HitRate64 = coverageK3HitRate64;
            d.coverageK3HitRate128 = coverageK3HitRate128;
            d.coverageNumberRate1 = coverageNumberRate1;
            d.coverageNumberRate2 = coverageNumberRate2;
            d.coverageNumberRate3 = coverageNumberRate3;
            d.finalPBeforeCoverage = finalPBeforeCoverage;
            d.finalPAfterCoverage = finalPAfterCoverage;
            d.coverageAdjustedP = coverageAdjustedP;
            d.probabilityCoverageSummary = probabilityCoverageSummary;
            d.coverageQualitySummary = coverageQualitySummary;
            d.probabilityCoverageActionAdvice = probabilityCoverageActionAdvice;
            d.probabilityActionAdvice = probabilityActionAdvice;
            d.probabilityFieldVersion = probabilityFieldVersion;
            d.dynamicGateQualityStatus = dynamicGateQualityStatus;
            d.dynamicGateFalseHighPFlag = dynamicGateFalseHighPFlag;
            d.dynamicGatePreferredEdgeBand = dynamicGatePreferredEdgeBand;
            d.dynamicGateTeacherV20WeakFlag = dynamicGateTeacherV20WeakFlag;
            d.dynamicGateEdgePolicy = dynamicGateEdgePolicy;
            d.dynamicGateCoverageRisk = dynamicGateCoverageRisk;
            d.dynamicGateCoverageQualityScore = dynamicGateCoverageQualityScore;
            d.dynamicGateThirdNumberQuality = dynamicGateThirdNumberQuality;
            d.dynamicGateVirtualEdgeRisk = dynamicGateVirtualEdgeRisk;
            d.dynamicGateActionAdvice = dynamicGateActionAdvice;
            d.dynamicGateFieldVersion = dynamicGateFieldVersion;
            d.dynamicGateVersion = dynamicGateVersion;
            d.finalDecisionVersion = finalDecisionVersion;
            d.finalDecisionEdgePolicy = finalDecisionEdgePolicy;
            d.finalDecisionTeacherPolicy = finalDecisionTeacherPolicy;
            d.finalDecisionV20Policy = finalDecisionV20Policy;
            d.finalDecisionBetDomain = finalDecisionBetDomain;
            d.finalDecisionRejectCode = finalDecisionRejectCode;
            d.finalDecisionShadowReason = finalDecisionShadowReason;
            d.finalDecisionStrongOverrideTrace = finalDecisionStrongOverrideTrace;
            d.finalDecisionCoverageRisk = finalDecisionCoverageRisk;
            d.finalDecisionCoverageQuality = finalDecisionCoverageQuality;
            d.finalDecisionThirdNumberQuality = finalDecisionThirdNumberQuality;
            d.finalDecisionFalseHighPFlag = finalDecisionFalseHighPFlag;
            copyDynamicCausalBridgeFields(this, d);
            copyMainChainBridgeFields(this, d);
            Models.copySharedPublicFieldsV20(this, d);
            return d;
        }
    }

    public static final class Trade {

        /* 星河 V40：全号码分布、OOS校准、分层后验、不可变契约与唯一授权。 */
        public double rawRankScore;
        public double calibratedHitProbability;
        public double honestProbability;
        public double actionUtility;
        public double actionStructureScore;
        public double actionDiscriminationScore;
        public double actionMaturityScore;
        public double actionUncertaintyPenalty;
        public double actionFrequencySupport;
        /* V42 unified risk-coverage evidence.  These fields are immutable audit
         * evidence after lane compilation; they never create a BET. */
        public double v42AcceptScore;
        public double v42FamilyLcb;
        public double v42RegimeQuality;
        public double v42AssociationScore;
        public double v42DistillationRisk;
        public int v42SupportVotes;
        public String v42RegimeFamily;
        public String v42RiskCoverageMode;
        public String v42PrimaryReject;
        public String v42SecondaryReject;
        public String v42TertiaryReject;
        public String v42LanePolicyVersion;
        /* V43 canonical full-strategy evidence contract. */
        public double v43StrategySupport;
        public double v43StrategyRisk;
        public double v43StrategyConfidence;
        public double v43StrategyDiversity;
        public double v43StrategyMaturity;
        public double v43SafeProbability;
        public double v43ProbabilityResidual;
        public double v43AblationFragility;
        public double v43ProbabilityLock;
        public int v43EvidenceGroupCount;
        public int v43HardRisk;
        public String v43DominantEvidence;
        public String v43MissingEvidence;
        public String v43EvidenceFingerprint;
        public String v43ProbabilityLockHash;
        public String v43PipelineStage;
        public String v43InvariantStatus;
        public String v43StrategyVersion;
        /* V46 separated event probability, conservative action bound and final funnel. */
        public double v46EventProbability;
        public double v46ActionLcb;
        public double v46ProbabilityUncertainty;
        public double v46ExpectedValue;
        public double v46ConditionalAdjustment;
        public int v46ConditionalSample;
        public int v46FinalRank;
        public int v46FormalCandidateCount;
        public String v46ProbabilityTrace;
        public String v46NumberDistributionSummary;
        public String v46ConditionalCalibrationSummary;
        public String v46RootCauseCode;
        public String v46RootCauseOwner;
        public String v46ContractHash;
        public String v46Version;
        public String actionUtilityVersion;
        public String laneCandidate;
        public int oosRankSlot;
        public int oosCandidateCount;
        public String buildId;
        public String cohortId;
        public String schemaVersion;
        public String contractDecisionId;
        public String candidateHash;
        public String snapshotMaxIssue;
        public String trainedUntilIssue;
        public String featureSchemaVersion;
        public String rankingVersion;
        public String calibrationVersion;
        public String gatePolicyVersion;
        public String pipelineVersion;
        public int contractValid;
        public String contractStatus;
        public String coarseFamilyKey;
        public String midFamilyKey;
        public String fineFamilyKey;
        public String positionFamilyKey;
        public String posteriorMatchedLevel;
        public String posteriorFamilyKey;
        public String posteriorPolicy;
        public String posteriorVersion;
        public int posteriorSample;
        public int posteriorHit;
        public double posteriorMean;
        public double posteriorWilsonLower;
        public double posteriorRecentRate;
        public double posteriorDrift;
        public double familyWilsonLower;
        public int calibrationBucket;
        public int calibrationSample;
        public int calibrationTotalSample;
        public int calibrationReady;
        public String calibrationStatus;
        public double calibrationBrier;
        public String authorizationId;
        public int authorizationValid;
        public String authorizedMode;
        public String authorizationRejectCode;
        public long authorizedAt;
        public double authorizationProbability;
        public double authorizationGate;
        public double authorizationEdge;
        public String teacherPolicy;
        public String dynamicPolicy;
        public String adaptivePolicy;
        public String microWalkForwardStatus;
        public String microWalkForwardVersion;
        public String microWalkForwardAsOfIssue;
        public int microWalkForwardSample;

        public long id;
        public long decisionId;
        public String baseIssue;
        public String targetIssue;
        public String action;
        public long betTime;
        public long settleTime;
        public long createdAt;
        public long settledAt;
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
        public String stateKey;
        public String reasonSummary;
        public String result;
        public int actualNumber;
        public int hit;
        public double pnl;
        public String missReason;
        public String settlementReason;
        public int settled;
        public double deepScore;
        public double deepConfidence;
        public double deepFeatureP;
        public double deepPositionScore;
        public double deepNumberScore;
        public double deepCycleScore;
        public double deepTransitionScore;
        public double deepStateScore;
        public double aiJudgeScore;
        public double aiJudgeConfidence;
        public double evidenceScore;
        public double evidenceRisk;
        public double biasScore;
        public double biasConfidence;
        public double auditPenalty;
        public double auditBoost;
        public double profileScore;
        public double fallbackProfileScore;
        public double runtimeProfileScore;
        public double stateNumberProfileScore;
        public double teacherWeight;
        public int teacherNumberSample;
        public int teacherNumberHistorySample;
        public int teacherDomainShadowSample;
        public int teacherDomainRealSample;
        public double teacherNumberHitRate;
        public double teacherNumberConfidence;
        public double teacherNumberScore;
        public double teacherNumberRisk;
        public String teacherNumberSummary;
        public String teacherApplyError;
        public String preDecisionDomainVersion;
        public String teacherRuntimeApplyVersion;
        public double aPlusScore;
        public double promotionScore;
        public double conflictLevel;
        public double weakestNumberScore;
        public double balanceScore;
        public String deepProfileKey;
        public double deepProfileHitRate;
        public int deepProfileSample;
        public int deepFallbackLevel;
        public String fallbackProfileKey;
        public String promotionStatus;
        public String aPlusStatus;
        public String candidateEvidenceKey;
        public String snapshotId;
        public String attributionReasonCode;
        public String attributionText;
        public String affectedModule;
        public String statScope;
        public String deepAttribution;
        public String deepReason;
        public String aiJudgeSummary;
        public String learningSummary;
        public String selfDiagnosisText;
        public int manualBacktestSupport;

        public int teacherMatrixSample;
        public int teacherMatrixStateSample;
        public int teacherMatrixComboSample;
        public double teacherMatrixHitRate;
        public double teacherMatrixRecentHitRate;
        public double teacherMatrixConfidence;
        public double teacherMatrixProbAdjust;
        public double teacherMatrixGateAdjust;
        public double teacherMatrixRiskPenalty;
        public double teacherMatrixScore;
        public double teacherMatrixPositionScore;
        public double teacherMatrixNumberScore;
        public double teacherMatrixComboScore;
        public double teacherMatrixStateScore;
        public String teacherMatrixStatus;
        public String teacherMatrixProfileKey;
        public String teacherMatrixComboKey;
        public String teacherMatrixSummary;

        /* V20 上游证据补实字段：只承接字段，不决定 BET */
        public int teacherWeakNoBetFlag;
        public int teacherMatrixWeakNoBetFlag;
        public int teacherMatrixOrdinaryBetBlockFlag;
        public int teacherMatrixSupportsFinalGate;
        public int teacherMatrixSupportsBet;
        public int teacherMatrixRiskFlag;
        public int teacherMatrixFreezeFlag;
        public String teacherOrdinaryBetPolicy;
        public String teacherMatrixOrdinaryBetPolicy;
        public String teacherFieldSource;
        public String teacherFieldVersion;
        public String teacherNumberStatus;
        public String teacherStatus;
        public String teacherStatusClass;
        public double teacherEvidenceContribution;
        public double teacherMatrixEvidenceContribution;
        public double teacherCounterContribution;
        public double teacherMatrixCounterContribution;
        public double teacherSampleReliability;
        public double teacherRecentValidity;

        public String v20Status;
        public String dynamicStatus;
        public String dynamicCauseStatus;
        public String v20ActionAdvice;
        public String v20FieldSource;
        public String v20FieldVersion;
        public int v20EmptyFlag;
        public double v20SupportScore;
        public double v20RiskScore;
        public double v20EvidenceContribution;
        public double v20CounterContribution;
        public double v20SampleReliability;
        public double v20RecentValidity;
        public int similarWindowSample;
        public double similarWindowHitRate;
        public String v20WindowStateKey;
        public String windowStateKey;
        public String finalDecisionWindowStateKey;
        public String dynamicWindowStateKey;
        public String probabilityWindowStateKey;

        public double coverageQualityScore;
        public double thirdNumberQualityScore;
        public double k3CoverageRisk;
        public int falseHighPFlag;
        public int probabilityFalseHighPFlag;
        public double probabilityFalseHighRisk;
        public int probabilityPreferredEdgeBand;
        public int probabilityHighEdgeFlag;
        public int probabilityPreferredPBand;
        public int probabilityTeacherWeakOrEmpty;
        public int probabilityV20UnknownOrEmpty;
        public int probabilityStrongCauseFlag;
        public double coverageProbDiscount;
        public double coverageProbBoost;
        public double coverageK3HitRate32;
        public double coverageK3HitRate64;
        public double coverageK3HitRate128;
        public double coverageNumberRate1;
        public double coverageNumberRate2;
        public double coverageNumberRate3;
        public double finalPBeforeCoverage;
        public double finalPAfterCoverage;
        public double coverageAdjustedP;
        public String probabilityCoverageSummary;
        public String coverageQualitySummary;
        public String probabilityCoverageActionAdvice;
        public String probabilityActionAdvice;
        public String probabilityFieldVersion;

        public String dynamicGateQualityStatus;
        public int dynamicGateFalseHighPFlag;
        public int dynamicGatePreferredEdgeBand;
        public int dynamicGateTeacherV20WeakFlag;
        public String dynamicGateEdgePolicy;
        public double dynamicGateCoverageRisk;
        public double dynamicGateCoverageQualityScore;
        public double dynamicGateThirdNumberQuality;
        public double dynamicGateVirtualEdgeRisk;
        public String dynamicGateActionAdvice;
        public String dynamicGateFieldVersion;
        public String dynamicGateVersion;

        public String finalDecisionVersion;
        public String finalDecisionEdgePolicy;
        public String finalDecisionTeacherPolicy;
        public String finalDecisionV20Policy;
        public String finalDecisionBetDomain;
        public String finalDecisionRejectCode;
        public String finalDecisionShadowReason;
        public String finalDecisionStrongOverrideTrace;
        public double finalDecisionCoverageRisk;
        public double finalDecisionCoverageQuality;
        public double finalDecisionThirdNumberQuality;
        public int finalDecisionFalseHighPFlag;
        /* V20主链闭环字段：只承接字段，不计算概率，不决定BET */
        public String clusterKey;
        public String factorKey;
        public double positionNeedScore;
        public double numberSupplyScore;
        public double coverageQuality;
        public double thirdQuality;
        public double counterEvidenceScore;
        public int similarSampleCount;
        public String adaptiveStatus;
        public String shadowPromotionStatus;
        public double finalPBeforeDiagnosis;
        public double finalPAfterDiagnosis;
        public double gate;
        public String rejectCode;
        public int realProbeAllowed;

        /* 链路修改升级V2字段：只承接，不计算概率，不决定BET */
        public double realBetFrequency50;
        public double realBetFrequency100;
        public double realBetFrequency300;
        public double frequencyDebtScore;
        public double frequencySupportScore;
        public double frequencyGateAdjust;
        public double qualifiedProbeScore;
        public double shadowPromotionScore;
        public double baseGate;
        public double riskAdjustedGate;
        public double effectiveProbeGate;
        public double rawEdge;
        public double effectiveProbeEdge;
        public double needSupplyScore;
        public double counterRisk;
        public int qualifiedProbe;
        public int realTrade;
        public int coverageAvailable;
        public int thirdQualityAvailable;
        public int promotionDomainAvailable;
        public String frequencyState;
        public String frequencyStateVersion;
        public String shadowPromotionDomain;
        public String promotionCoarseDomain;
        public String promotionMidDomain;
        public String promotionFineDomain;
        public String decisionPath;
        public String betDomain;
        public String probeRejectReason;
        public String probeEligibilityVersion;
        public String ruleHealthStatus;
        public String snapshotFreshness;
        public String warmProfileFreshness;
        public String coldProfileFreshness;
        public String deepProfileFreshness;
        public String decisionVersionV2;
        public String evidenceAsOfIssue;
        public String snapshotAsOfIssue;
        public long snapshotCreatedAt;
        public long frequencyStateUpdatedAt;
        public int frequencyFloorRequiredCount;
        public int frequencyFloorTargetCount;
        public int frequencyFloorActualCount;
        public int frequencyFloorDebtCount;
        public int frequencyTargetDebtCount;
        public int frequencyFloorMandatory;
        public int frequencyFloorNoUpperCap;
        public double frequencyFloorRankScore;
        public String frequencyFloorVersion;

        /* 73487246：四窗口平滑频率、冻结域、Teacher/Dynamic分层审计。 */
        public double realBetFrequency20;
        public int frequencyObserved20;
        public int frequencyObserved50;
        public int frequencyObserved100;
        public int frequencyObserved300;
        public int frequencyBetCount20;
        public int frequencyBetCount50;
        public int frequencyBetCount100;
        public int frequencyBetCount300;
        public int frequencyDebt20;
        public int frequencyDebt50;
        public int frequencyDebt100;
        public int frequencyDebt300;
        public int frequencyTargetDebt20;
        public int frequencyTargetDebt50;
        public int frequencyTargetDebt100;
        public int frequencyTargetDebt300;
        public String frequencyUrgentWindow;
        public int frequencyIssuesSinceLastRealBet;
        public int frequencyIssuesSinceLastFloorBet;
        public int frequencyConsecutiveFloorBets;
        public int frequencyFloorMinGap;
        public int frequencyFloorPacingReady;
        public String frequencyFloorPacingReason;
        public String decisionQualityPath;
        public int evidenceDomainFrozen;
        public String evidenceDomainFrozenIssue;
        public long evidenceDomainFrozenAt;
        public String dynamicMatchedLevel;
        public int dynamicRealSample;
        public int dynamicShadowSample;
        public String moduleBuildVersion;

        public String sourceType;
        public int isRealtimeReal;
        public String preDrawReasonFingerprint;
        public String preDrawReasonText;
        public String reasonValidLevel;
        public double reasonScore;
        public double reasonEdge;
        public double hitSimilarity;
        public double missSimilarity;
        public double contradictionScore;
        public double causalLift;
        public double causalConfidence;
        public int validReasonCount;
        public int falseReasonCount;
        public int forwardReasonSample;
        public double forwardReasonHitRate;
        public double reasonWilsonScore;
        public String k3RoleSummary;
        public int k3MainNumber;
        public int k3SupportNumber;
        public int k3DefenseNumber;
        public double comboRoleDiversity;
        public double comboConflict;
        public int fingerprintCooling;
        public int elite45Passed;
        public String elite45Status;
        public String elite45BlockCode;
        public double elite45Gate;
        public String elite45Summary;
        /* 73487246：Teacher新鲜度、观察/硬风险分层、受控探针实体字段。 */
        public String teacherAsOfIssue;
        public int teacherIssueLag;
        public int teacherFreshByIssue;
        public long teacherSnapshotCreatedAt;
        public String blockTier;
        public String hardRiskCode;
        public String observeReason;
        public int probeEligible;
        public int shadowProbeEligible;
        public String probeLevel;
        public int shadowSettledCount;
        public int shadowDomainSample;
        public double promotionWilsonLowerBound;
        public String promotionEligibilityReason;

        public Trade() {
            id = 0L;
            decisionId = 0L;
            baseIssue = "";
            targetIssue = "";
            action = AppConfig.ACTION_BET;
            betTime = 0L;
            settleTime = 0L;
            createdAt = 0L;
            settledAt = 0L;
            position = 0;
            n1 = 0;
            n2 = 0;
            n3 = 0;
            k3Mask = 0;
            rawP = 0.0d;
            calibratedP = 0.0d;
            finalP = 0.0d;
            dynamicGate = AppConfig.DEFAULT_DYNAMIC_GATE;
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
            stateMode = AppConfig.MODE_CHAOS;
            shapeKey = "";
            stateKey = "";
            reasonSummary = "";
            result = AppConfig.RESULT_WAIT;
            actualNumber = 0;
            hit = 0;
            pnl = 0.0d;
            missReason = "";
            settlementReason = "";
            settled = 0;
            deepScore = 0.0d;
            deepConfidence = 0.0d;
            deepFeatureP = 0.0d;
            deepPositionScore = 0.0d;
            deepNumberScore = 0.0d;
            deepCycleScore = 0.0d;
            deepTransitionScore = 0.0d;
            deepStateScore = 0.0d;
            aiJudgeScore = 0.0d;
            aiJudgeConfidence = 0.0d;
            evidenceScore = 0.0d;
            evidenceRisk = 0.0d;
            biasScore = 0.0d;
            biasConfidence = 0.0d;
            auditPenalty = 0.0d;
            auditBoost = 0.0d;
            profileScore = 0.0d;
            fallbackProfileScore = 0.0d;
            runtimeProfileScore = 0.0d;
            stateNumberProfileScore = 0.0d;
            teacherWeight = 1.0d;
            aPlusScore = 0.0d;
            promotionScore = 0.0d;
            conflictLevel = 0.0d;
            weakestNumberScore = 0.0d;
            balanceScore = 0.0d;
            deepProfileKey = "";
            deepProfileHitRate = 0.0d;
            deepProfileSample = 0;
            deepFallbackLevel = 0;
            fallbackProfileKey = "";
            promotionStatus = AppConfig.PROMOTION_CANDIDATE;
            aPlusStatus = AppConfig.A_PLUS_NONE;
            candidateEvidenceKey = "";
            snapshotId = "";
            attributionReasonCode = "";
            attributionText = "";
            affectedModule = AppConfig.ATTR_MODULE_UNKNOWN;
            statScope = AppConfig.STAT_SCOPE_REAL_TRADE;
            deepAttribution = "";
            deepReason = "";
            aiJudgeSummary = "";
            learningSummary = "";
            selfDiagnosisText = "";
            manualBacktestSupport = 0;

            teacherMatrixSample = 0;
            teacherMatrixStateSample = 0;
            teacherMatrixComboSample = 0;
            teacherMatrixHitRate = 0.0d;
            teacherMatrixRecentHitRate = 0.0d;
            teacherMatrixConfidence = 0.0d;
            teacherMatrixProbAdjust = 0.0d;
            teacherMatrixGateAdjust = 0.0d;
            teacherMatrixRiskPenalty = 0.0d;
            teacherMatrixScore = 0.0d;
            teacherMatrixPositionScore = 0.0d;
            teacherMatrixNumberScore = 0.0d;
            teacherMatrixComboScore = 0.0d;
            teacherMatrixStateScore = 0.0d;
            teacherMatrixStatus = AppConfig.TEACHER_MATRIX_STATUS_NONE;
            teacherMatrixProfileKey = "";
            teacherMatrixComboKey = "";
            teacherMatrixSummary = "";

            teacherWeakNoBetFlag = 0;
            teacherMatrixWeakNoBetFlag = 0;
            teacherMatrixOrdinaryBetBlockFlag = 0;
            teacherMatrixSupportsFinalGate = 0;
            teacherMatrixSupportsBet = 0;
            teacherMatrixRiskFlag = 0;
            teacherMatrixFreezeFlag = 0;
            teacherOrdinaryBetPolicy = "";
            teacherMatrixOrdinaryBetPolicy = "";
            teacherFieldSource = "";
            teacherFieldVersion = "";
            teacherNumberStatus = "";
            teacherStatus = "";
            teacherStatusClass = "";
            teacherEvidenceContribution = 0.0d;
            teacherMatrixEvidenceContribution = 0.0d;
            teacherCounterContribution = 0.0d;
            teacherMatrixCounterContribution = 0.0d;
            teacherSampleReliability = 0.0d;
            teacherRecentValidity = 0.0d;

            v20Status = "";
            dynamicStatus = "";
            dynamicCauseStatus = "";
            v20ActionAdvice = "";
            v20FieldSource = "";
            v20FieldVersion = "";
            v20EmptyFlag = 0;
            v20SupportScore = 0.0d;
            v20RiskScore = 0.0d;
            v20EvidenceContribution = 0.0d;
            v20CounterContribution = 0.0d;
            v20SampleReliability = 0.0d;
            v20RecentValidity = 0.0d;
            similarWindowSample = 0;
            similarWindowHitRate = 0.0d;
            v20WindowStateKey = "";
            windowStateKey = "";
            finalDecisionWindowStateKey = "";
            dynamicWindowStateKey = "";
            probabilityWindowStateKey = "";

            coverageQualityScore = 0.0d;
            thirdNumberQualityScore = 0.0d;
            k3CoverageRisk = 0.0d;
            falseHighPFlag = 0;
            probabilityFalseHighPFlag = 0;
            probabilityFalseHighRisk = 0.0d;
            probabilityPreferredEdgeBand = 0;
            probabilityHighEdgeFlag = 0;
            probabilityPreferredPBand = 0;
            probabilityTeacherWeakOrEmpty = 0;
            probabilityV20UnknownOrEmpty = 0;
            probabilityStrongCauseFlag = 0;
            coverageProbDiscount = 0.0d;
            coverageProbBoost = 0.0d;
            coverageK3HitRate32 = 0.0d;
            coverageK3HitRate64 = 0.0d;
            coverageK3HitRate128 = 0.0d;
            coverageNumberRate1 = 0.0d;
            coverageNumberRate2 = 0.0d;
            coverageNumberRate3 = 0.0d;
            finalPBeforeCoverage = 0.0d;
            finalPAfterCoverage = 0.0d;
            coverageAdjustedP = 0.0d;
            probabilityCoverageSummary = "";
            coverageQualitySummary = "";
            probabilityCoverageActionAdvice = "";
            probabilityActionAdvice = "";
            probabilityFieldVersion = "";

            dynamicGateQualityStatus = "";
            dynamicGateFalseHighPFlag = 0;
            dynamicGatePreferredEdgeBand = 0;
            dynamicGateTeacherV20WeakFlag = 0;
            dynamicGateEdgePolicy = "";
            dynamicGateCoverageRisk = 0.0d;
            dynamicGateCoverageQualityScore = 0.0d;
            dynamicGateThirdNumberQuality = 0.0d;
            dynamicGateVirtualEdgeRisk = 0.0d;
            dynamicGateActionAdvice = "";
            dynamicGateFieldVersion = "";
            dynamicGateVersion = "";

            finalDecisionVersion = "";
            finalDecisionEdgePolicy = "";
            finalDecisionTeacherPolicy = "";
            finalDecisionV20Policy = "";
            finalDecisionBetDomain = "";
            finalDecisionRejectCode = "";
            finalDecisionShadowReason = "";
            finalDecisionStrongOverrideTrace = "";
            finalDecisionCoverageRisk = 0.0d;
            finalDecisionCoverageQuality = 0.0d;
            finalDecisionThirdNumberQuality = 0.0d;
            finalDecisionFalseHighPFlag = 0;
            initDynamicCausalBridgeFields(this);
            initMainChainBridgeFields(this);
            promotionCoarseDomain = "";
            promotionMidDomain = "";
            promotionFineDomain = "";

            sourceType = SOURCE_TYPE_REALTIME;
            isRealtimeReal = 1;
            preDrawReasonFingerprint = "";
            preDrawReasonText = "";
            reasonValidLevel = REASON_LEVEL_C_EXPLAIN_ONLY;
            reasonScore = 0.0d;
            reasonEdge = 0.0d;
            hitSimilarity = 0.0d;
            missSimilarity = 0.0d;
            contradictionScore = 0.0d;
            causalLift = 0.0d;
            causalConfidence = 0.0d;
            validReasonCount = 0;
            falseReasonCount = 0;
            forwardReasonSample = 0;
            forwardReasonHitRate = 0.0d;
            reasonWilsonScore = 0.0d;
            k3RoleSummary = "";
            k3MainNumber = 0;
            k3SupportNumber = 0;
            k3DefenseNumber = 0;
            comboRoleDiversity = 0.0d;
            comboConflict = 0.0d;
            fingerprintCooling = 0;
            elite45Passed = 0;
            elite45Status = BLOCK_ELITE45_NOT_PASS;
            elite45BlockCode = BLOCK_ELITE45_NOT_PASS;
            elite45Gate = ELITE45_GATE_DEFAULT;
            elite45Summary = "";
        }

        public void normalize() {
            action = AppConfig.ACTION_BET;
            result = AppConfig.normalizeResult(result);
            statScope = AppConfig.STAT_SCOPE_REAL_TRADE;
            sourceType = SOURCE_TYPE_REALTIME;
            isRealtimeReal = 1;
            k3Mask = buildK3Mask(n1, n2, n3);
            finalP = AppConfig.clampProbability(finalP);
            rawP = AppConfig.clampProbability(rawP);
            calibratedP = AppConfig.clampProbability(calibratedP);
            dynamicGate = AppConfig.clampGate(dynamicGate);
            edge = finalP - dynamicGate;
            reasonValidLevel = normalizeReasonLevel(reasonValidLevel);
            normalizeDynamicCausalBridgeFields(this);
            elite45Gate = clampElite45Gate(elite45Gate);

            if (createdAt <= 0L) createdAt = betTime > 0L ? betTime : System.currentTimeMillis();
            if (betTime <= 0L) betTime = createdAt;
        }

        public boolean isOpen() {
            return settled == 0 || AppConfig.RESULT_WAIT.equals(AppConfig.normalizeResult(result));
        }

        public boolean hasLegalPlayRule() {
            return AppConfig.isPlayRuleLegal(position, AppConfig.FIXED_K, n1, n2, n3);
        }

        public boolean isHitByNumber(int number) {
            return maskContains(k3Mask, number);
        }

        public boolean isTargetIssue(String issue) {
            if (issue == null || targetIssue == null) return false;
            return targetIssue.equals(issue);
        }

        public void setNumbers(int a, int b, int c) {
            n1 = a;
            n2 = b;
            n3 = c;
            k3Mask = buildK3Mask(a, b, c);
        }

        public void settle(int actual) {
            if (!AppConfig.isValidNumber(actual)) {
                markDataError(AppConfig.RESULT_DATA_ERROR, "结算号码无效");
                return;
            }
            actualNumber = actual;
            settled = 1;
            settleTime = System.currentTimeMillis();
            settledAt = settleTime;

            boolean ok = isHitByNumber(actual);
            hit = ok ? 1 : 0;
            result = ok ? AppConfig.RESULT_WIN : AppConfig.RESULT_MISS;
            pnl = AppConfig.pnlForHit(ok);

            if (ok) {
                missReason = "";
                settlementReason = "STRICT_POSITION_K3_WIN";
            } else {
                if (missReason == null || missReason.length() == 0) {
                    missReason = AppConfig.MISS_UNKNOWN;
                }
                settlementReason = "STRICT_POSITION_K3_MISS";
            }
        }

        public void settleByDraw(Draw draw) {
            if (draw == null || !draw.isValid()) {
                markDataError(AppConfig.RESULT_DATA_ERROR, "开奖数据无效，不能结算");
                return;
            }
            if (!isTargetIssue(draw.issue)) {
                markDataError(AppConfig.RESULT_DATA_OFFSET, "目标期不一致，禁止错期结算");
                return;
            }
            if (!hasLegalPlayRule()) {
                markDataError(AppConfig.RESULT_DATA_ERROR, "投注内容不符合单位置K3");
                return;
            }
            int actual = draw.getNumberAtPosition(position);
            settle(actual);
        }

        public void markDataError(String code, String reason) {
            settled = 1;
            hit = 0;
            actualNumber = 0;
            pnl = 0.0d;
            result = AppConfig.normalizeResult(code);
            if (!AppConfig.isDataBadResult(result)) {
                result = AppConfig.RESULT_DATA_ERROR;
            }
            missReason = code == null ? AppConfig.RESULT_DATA_ERROR : code;
            settlementReason = reason == null ? "" : reason;
            settleTime = System.currentTimeMillis();
            settledAt = settleTime;
        }

        public String displayContent() {
            return UiFormatter.positionAndNumbers(position, n1, n2, n3);
        }

        public LearningSample toLearningSample() {
            LearningSample s = new LearningSample();
            s.baseIssue = baseIssue == null ? "" : baseIssue;
            s.targetIssue = targetIssue == null ? "" : targetIssue;
            s.position = position;
            s.k3Mask = k3Mask;
            s.shapeKey = shapeKey == null ? "" : shapeKey;
            s.stateKey = stateKey == null ? "" : stateKey;
            s.stateMode = stateMode == null ? AppConfig.MODE_CHAOS : stateMode;
            s.hit = hit;
            s.actualNumber = actualNumber;
            s.actualRank = hit == 1 ? 1 : 0;
            s.finalP = finalP;
            s.rawP = rawP;
            s.trendLife = trendLife;
            s.driftRisk = driftRisk;
            s.breakDepth = breakDepth;
            s.recoveryScore = recoveryScore;
            s.championGap = championGap;
            s.comboGap = comboGap;
            s.missReason = missReason == null ? "" : missReason;
            s.createdAt = settledAt > 0L ? settledAt : System.currentTimeMillis();
            s.statScope = statScope == null ? AppConfig.STAT_SCOPE_REAL_TRADE : statScope;
            s.sourceType = sourceType == null ? SOURCE_TYPE_REALTIME : sourceType;
            s.isRealtimeReal = isRealtimeReal;
            s.deepProfileKey = deepProfileKey == null ? "" : deepProfileKey;
            s.attributionReasonCode = attributionReasonCode == null ? "" : attributionReasonCode;
            s.affectedModule =
                    affectedModule == null ? AppConfig.ATTR_MODULE_UNKNOWN : affectedModule;
            s.preDrawReasonFingerprint = preDrawReasonFingerprint;
            s.preDrawReasonText = preDrawReasonText;
            s.reasonValidLevel = reasonValidLevel;
            s.reasonScore = reasonScore;
            s.reasonEdge = reasonEdge;
            s.hitSimilarity = hitSimilarity;
            s.missSimilarity = missSimilarity;
            s.contradictionScore = contradictionScore;
            s.causalLift = causalLift;
            s.causalConfidence = causalConfidence;
            s.validReasonCount = validReasonCount;
            s.falseReasonCount = falseReasonCount;
            s.k3RoleSummary = k3RoleSummary;
            s.elite45Passed = elite45Passed;
            s.elite45Summary = elite45Summary;
            return s;
        }

        public void applyDecision(Decision d) {
            if (d == null) return;
            d.normalize();

            decisionId = d.id;
            baseIssue = d.baseIssue == null ? "" : d.baseIssue;
            targetIssue = d.targetIssue == null ? "" : d.targetIssue;
            action = AppConfig.ACTION_BET;
            position = d.position;
            setNumbers(d.n1, d.n2, d.n3);
            rawP = d.rawP;
            calibratedP = d.calibratedP;
            finalP = d.finalP;
            dynamicGate = d.dynamicGate;
            edge = d.edge;
            modelP = d.modelP;
            shapeP = d.shapeP;
            comboP = d.comboP;
            recoveryP = d.recoveryP;
            similarP = d.similarP;
            memoryP = d.memoryP;
            calibrationError = d.calibrationError;
            similarHitRate = d.similarHitRate;
            comboHitRate = d.comboHitRate;
            effectiveSample = d.effectiveSample;
            comboSample = d.comboSample;
            trendLife = d.trendLife;
            driftRisk = d.driftRisk;
            breakDepth = d.breakDepth;
            recoveryScore = d.recoveryScore;
            championGap = d.championGap;
            comboGap = d.comboGap;
            hotReverseRisk = d.hotReverseRisk;
            falseStrongRisk = d.falseStrongRisk;
            stateMode = d.stateMode == null ? AppConfig.MODE_CHAOS : d.stateMode;
            shapeKey = d.shapeKey == null ? "" : d.shapeKey;
            stateKey = d.stateKey == null ? "" : d.stateKey;
            reasonSummary = d.reasonSummary == null ? "" : d.reasonSummary;
            if (reasonSummary.length() == 0 && d.reasonText != null) {
                reasonSummary = d.reasonText;
            }

            deepScore = d.deepScore;
            deepConfidence = d.deepConfidence;
            deepFeatureP = d.deepFeatureP;
            deepPositionScore = d.deepPositionScore;
            deepNumberScore = d.deepNumberScore;
            deepCycleScore = d.deepCycleScore;
            deepTransitionScore = d.deepTransitionScore;
            deepStateScore = d.deepStateScore;
            aiJudgeScore = d.aiJudgeScore;
            aiJudgeConfidence = d.aiJudgeConfidence;
            evidenceScore = d.evidenceScore;
            evidenceRisk = d.evidenceRisk;
            biasScore = d.biasScore;
            biasConfidence = d.biasConfidence;
            auditPenalty = d.auditPenalty;
            auditBoost = d.auditBoost;
            profileScore = d.profileScore;
            fallbackProfileScore = d.fallbackProfileScore;
            runtimeProfileScore = d.runtimeProfileScore;
            stateNumberProfileScore = d.stateNumberProfileScore;
            teacherWeight = d.teacherWeight;
            aPlusScore = d.aPlusScore;
            promotionScore = d.promotionScore;
            conflictLevel = d.conflictLevel;
            weakestNumberScore = d.weakestNumberScore;
            balanceScore = d.balanceScore;
            deepProfileKey = d.deepProfileKey == null ? "" : d.deepProfileKey;
            deepProfileHitRate = d.deepProfileHitRate;
            deepProfileSample = d.deepProfileSample;
            deepFallbackLevel = d.deepFallbackLevel;
            fallbackProfileKey = d.fallbackProfileKey == null ? "" : d.fallbackProfileKey;
            promotionStatus =
                    d.promotionStatus == null ? AppConfig.PROMOTION_CANDIDATE : d.promotionStatus;
            aPlusStatus = d.aPlusStatus == null ? AppConfig.A_PLUS_NONE : d.aPlusStatus;
            candidateEvidenceKey = d.candidateEvidenceKey == null ? "" : d.candidateEvidenceKey;
            snapshotId = d.snapshotId == null ? "" : d.snapshotId;
            deepAttribution = d.deepAttribution == null ? "" : d.deepAttribution;
            deepReason = d.deepReason == null ? "" : d.deepReason;
            aiJudgeSummary = d.aiJudgeSummary == null ? "" : d.aiJudgeSummary;
            learningSummary = d.learningSummary == null ? "" : d.learningSummary;
            selfDiagnosisText = d.selfDiagnosisText == null ? "" : d.selfDiagnosisText;
            manualBacktestSupport = d.manualBacktestSupport;
            statScope = AppConfig.STAT_SCOPE_REAL_TRADE;

            teacherMatrixSample = d.teacherMatrixSample;
            teacherMatrixStateSample = d.teacherMatrixStateSample;
            teacherMatrixComboSample = d.teacherMatrixComboSample;
            teacherMatrixHitRate = d.teacherMatrixHitRate;
            teacherMatrixRecentHitRate = d.teacherMatrixRecentHitRate;
            teacherMatrixConfidence = d.teacherMatrixConfidence;
            teacherMatrixProbAdjust = d.teacherMatrixProbAdjust;
            teacherMatrixGateAdjust = d.teacherMatrixGateAdjust;
            teacherMatrixRiskPenalty = d.teacherMatrixRiskPenalty;
            teacherMatrixScore = d.teacherMatrixScore;
            teacherMatrixPositionScore = d.teacherMatrixPositionScore;
            teacherMatrixNumberScore = d.teacherMatrixNumberScore;
            teacherMatrixComboScore = d.teacherMatrixComboScore;
            teacherMatrixStateScore = d.teacherMatrixStateScore;
            teacherMatrixStatus = d.teacherMatrixStatus;
            teacherMatrixProfileKey = d.teacherMatrixProfileKey;
            teacherMatrixComboKey = d.teacherMatrixComboKey;
            teacherMatrixSummary = d.teacherMatrixSummary;

            sourceType = SOURCE_TYPE_REALTIME;
            isRealtimeReal = 1;

            preDrawReasonFingerprint = d.preDrawReasonFingerprint;
            preDrawReasonText = d.preDrawReasonText;
            reasonValidLevel = d.reasonValidLevel;
            reasonScore = d.reasonScore;
            reasonEdge = d.reasonEdge;
            hitSimilarity = d.hitSimilarity;
            missSimilarity = d.missSimilarity;
            contradictionScore = d.contradictionScore;
            causalLift = d.causalLift;
            causalConfidence = d.causalConfidence;
            validReasonCount = d.validReasonCount;
            falseReasonCount = d.falseReasonCount;
            forwardReasonSample = d.forwardReasonSample;
            forwardReasonHitRate = d.forwardReasonHitRate;
            reasonWilsonScore = d.reasonWilsonScore;
            k3RoleSummary = d.k3RoleSummary;
            k3MainNumber = d.k3MainNumber;
            k3SupportNumber = d.k3SupportNumber;
            k3DefenseNumber = d.k3DefenseNumber;
            comboRoleDiversity = d.comboRoleDiversity;
            comboConflict = d.comboConflict;
            fingerprintCooling = d.fingerprintCooling;
            elite45Passed = d.elite45Passed;
            elite45Status = d.elite45Status;
            elite45BlockCode = d.elite45BlockCode;
            elite45Gate = d.elite45Gate;
            elite45Summary = d.elite45Summary;
            teacherWeakNoBetFlag = d.teacherWeakNoBetFlag;
            teacherMatrixWeakNoBetFlag = d.teacherMatrixWeakNoBetFlag;
            teacherMatrixOrdinaryBetBlockFlag = d.teacherMatrixOrdinaryBetBlockFlag;
            teacherMatrixSupportsFinalGate = d.teacherMatrixSupportsFinalGate;
            teacherMatrixSupportsBet = d.teacherMatrixSupportsBet;
            teacherMatrixRiskFlag = d.teacherMatrixRiskFlag;
            teacherMatrixFreezeFlag = d.teacherMatrixFreezeFlag;
            teacherOrdinaryBetPolicy = d.teacherOrdinaryBetPolicy;
            teacherMatrixOrdinaryBetPolicy = d.teacherMatrixOrdinaryBetPolicy;
            teacherFieldSource = d.teacherFieldSource;
            teacherFieldVersion = d.teacherFieldVersion;
            teacherNumberStatus = d.teacherNumberStatus;
            teacherStatus = d.teacherStatus;
            teacherStatusClass = d.teacherStatusClass;
            teacherEvidenceContribution = d.teacherEvidenceContribution;
            teacherMatrixEvidenceContribution = d.teacherMatrixEvidenceContribution;
            teacherCounterContribution = d.teacherCounterContribution;
            teacherMatrixCounterContribution = d.teacherMatrixCounterContribution;
            teacherSampleReliability = d.teacherSampleReliability;
            teacherRecentValidity = d.teacherRecentValidity;
            v20Status = d.v20Status;
            dynamicStatus = d.dynamicStatus;
            dynamicCauseStatus = d.dynamicCauseStatus;
            v20ActionAdvice = d.v20ActionAdvice;
            v20FieldSource = d.v20FieldSource;
            v20FieldVersion = d.v20FieldVersion;
            v20EmptyFlag = d.v20EmptyFlag;
            v20SupportScore = d.v20SupportScore;
            v20RiskScore = d.v20RiskScore;
            v20EvidenceContribution = d.v20EvidenceContribution;
            v20CounterContribution = d.v20CounterContribution;
            v20SampleReliability = d.v20SampleReliability;
            v20RecentValidity = d.v20RecentValidity;
            similarWindowSample = d.similarWindowSample;
            similarWindowHitRate = d.similarWindowHitRate;
            v20WindowStateKey = d.v20WindowStateKey;
            windowStateKey = d.windowStateKey;
            finalDecisionWindowStateKey = d.finalDecisionWindowStateKey;
            dynamicWindowStateKey = d.dynamicWindowStateKey;
            probabilityWindowStateKey = d.probabilityWindowStateKey;
            coverageQualityScore = d.coverageQualityScore;
            thirdNumberQualityScore = d.thirdNumberQualityScore;
            k3CoverageRisk = d.k3CoverageRisk;
            falseHighPFlag = d.falseHighPFlag;
            probabilityFalseHighPFlag = d.probabilityFalseHighPFlag;
            probabilityFalseHighRisk = d.probabilityFalseHighRisk;
            probabilityPreferredEdgeBand = d.probabilityPreferredEdgeBand;
            probabilityHighEdgeFlag = d.probabilityHighEdgeFlag;
            probabilityPreferredPBand = d.probabilityPreferredPBand;
            probabilityTeacherWeakOrEmpty = d.probabilityTeacherWeakOrEmpty;
            probabilityV20UnknownOrEmpty = d.probabilityV20UnknownOrEmpty;
            probabilityStrongCauseFlag = d.probabilityStrongCauseFlag;
            coverageProbDiscount = d.coverageProbDiscount;
            coverageProbBoost = d.coverageProbBoost;
            coverageK3HitRate32 = d.coverageK3HitRate32;
            coverageK3HitRate64 = d.coverageK3HitRate64;
            coverageK3HitRate128 = d.coverageK3HitRate128;
            coverageNumberRate1 = d.coverageNumberRate1;
            coverageNumberRate2 = d.coverageNumberRate2;
            coverageNumberRate3 = d.coverageNumberRate3;
            finalPBeforeCoverage = d.finalPBeforeCoverage;
            finalPAfterCoverage = d.finalPAfterCoverage;
            coverageAdjustedP = d.coverageAdjustedP;
            probabilityCoverageSummary = d.probabilityCoverageSummary;
            coverageQualitySummary = d.coverageQualitySummary;
            probabilityCoverageActionAdvice = d.probabilityCoverageActionAdvice;
            probabilityActionAdvice = d.probabilityActionAdvice;
            probabilityFieldVersion = d.probabilityFieldVersion;
            dynamicGateQualityStatus = d.dynamicGateQualityStatus;
            dynamicGateFalseHighPFlag = d.dynamicGateFalseHighPFlag;
            dynamicGatePreferredEdgeBand = d.dynamicGatePreferredEdgeBand;
            dynamicGateTeacherV20WeakFlag = d.dynamicGateTeacherV20WeakFlag;
            dynamicGateEdgePolicy = d.dynamicGateEdgePolicy;
            dynamicGateCoverageRisk = d.dynamicGateCoverageRisk;
            dynamicGateCoverageQualityScore = d.dynamicGateCoverageQualityScore;
            dynamicGateThirdNumberQuality = d.dynamicGateThirdNumberQuality;
            dynamicGateVirtualEdgeRisk = d.dynamicGateVirtualEdgeRisk;
            dynamicGateActionAdvice = d.dynamicGateActionAdvice;
            dynamicGateFieldVersion = d.dynamicGateFieldVersion;
            dynamicGateVersion = d.dynamicGateVersion;
            finalDecisionVersion = d.finalDecisionVersion;
            finalDecisionEdgePolicy = d.finalDecisionEdgePolicy;
            finalDecisionTeacherPolicy = d.finalDecisionTeacherPolicy;
            finalDecisionV20Policy = d.finalDecisionV20Policy;
            finalDecisionBetDomain = d.finalDecisionBetDomain;
            finalDecisionRejectCode = d.finalDecisionRejectCode;
            finalDecisionShadowReason = d.finalDecisionShadowReason;
            finalDecisionStrongOverrideTrace = d.finalDecisionStrongOverrideTrace;
            finalDecisionCoverageRisk = d.finalDecisionCoverageRisk;
            finalDecisionCoverageQuality = d.finalDecisionCoverageQuality;
            finalDecisionThirdNumberQuality = d.finalDecisionThirdNumberQuality;
            finalDecisionFalseHighPFlag = d.finalDecisionFalseHighPFlag;
            copyDynamicCausalBridgeFields(d, this);
            copyMainChainBridgeFields(d, this);

            Models.copySharedPublicFieldsV20(d, this);
        }
    }

    public static final class StateKey {
        public String stateKey;
        public int position;
        public String stateMode;
        public String trendKey;
        public String shapeKey;
        public String driftKey;
        public String breakKey;
        public String recoveryKey;
        public String transitionKey;
        public String hotColdKey;
        public String oddEvenKey;
        public String highLowKey;
        public String neighborKey;
        public String repeatKey;
        public String missKey;
        public String riskKey;
        public double trendScore;
        public double shapeScore;
        public double driftRisk;
        public double breakRisk;
        public double recoveryScore;
        public double confidence;
        public long createdAt;

        public StateKey() {
            stateKey = "";
            position = 0;
            stateMode = AppConfig.MODE_UNKNOWN;
            trendKey = "";
            shapeKey = "";
            driftKey = "";
            breakKey = "";
            recoveryKey = "";
            transitionKey = "";
            hotColdKey = "";
            oddEvenKey = "";
            highLowKey = "";
            neighborKey = "";
            repeatKey = "";
            missKey = "";
            riskKey = "";
            trendScore = 0.0d;
            shapeScore = 0.0d;
            driftRisk = 0.0d;
            breakRisk = 0.0d;
            recoveryScore = 0.0d;
            confidence = 0.0d;
            createdAt = 0L;
        }

        public void rebuild() {
            StringBuilder sb = new StringBuilder();
            sb.append("P").append(position);
            sb.append("|").append(AppConfig.buildStateKeyPart(stateMode));
            sb.append("|").append(AppConfig.buildStateKeyPart(trendKey));
            sb.append("|").append(AppConfig.buildStateKeyPart(shapeKey));
            sb.append("|").append(AppConfig.buildStateKeyPart(driftKey));
            sb.append("|").append(AppConfig.buildStateKeyPart(breakKey));
            sb.append("|").append(AppConfig.buildStateKeyPart(recoveryKey));
            sb.append("|").append(AppConfig.buildStateKeyPart(transitionKey));
            sb.append("|").append(AppConfig.buildStateKeyPart(hotColdKey));
            sb.append("|").append(AppConfig.buildStateKeyPart(oddEvenKey));
            sb.append("|").append(AppConfig.buildStateKeyPart(highLowKey));
            sb.append("|").append(AppConfig.buildStateKeyPart(riskKey));
            stateKey = sb.toString();
        }

        public boolean isUsable() {
            return AppConfig.isValidPosition(position) && stateKey != null && stateKey.length() > 0;
        }

        public StateKey copy() {
            StateKey k = new StateKey();
            k.stateKey = stateKey;
            k.position = position;
            k.stateMode = stateMode;
            k.trendKey = trendKey;
            k.shapeKey = shapeKey;
            k.driftKey = driftKey;
            k.breakKey = breakKey;
            k.recoveryKey = recoveryKey;
            k.transitionKey = transitionKey;
            k.hotColdKey = hotColdKey;
            k.oddEvenKey = oddEvenKey;
            k.highLowKey = highLowKey;
            k.neighborKey = neighborKey;
            k.repeatKey = repeatKey;
            k.missKey = missKey;
            k.riskKey = riskKey;
            k.trendScore = trendScore;
            k.shapeScore = shapeScore;
            k.driftRisk = driftRisk;
            k.breakRisk = breakRisk;
            k.recoveryScore = recoveryScore;
            k.confidence = confidence;
            k.createdAt = createdAt;
            return k;
        }
    }

    public static final class LearningCase {
        public long id;
        public String caseKey;
        public String taskKey;
        public String segmentKey;
        public String baseIssue;
        public String targetIssue;
        public long createdAt;
        public int targetIndex;
        public int position;
        public int actualNumber;
        public int predictedK3;
        public int k3Mask;
        public int hit;
        public String stateKey;
        public String stateMode;
        public String trendKey;
        public String shapeKey;
        public String driftKey;
        public String breakKey;
        public String recoveryKey;
        public String transitionKey;
        public String expectedShape;
        public String actualShape;
        public String whyExpectedHit;
        public String whyActualHit;
        public String whyMiss;
        public String nextAdvice;
        public String mainReasonCode;
        public String affectedModule;
        public double finalP;
        public double dynamicGate;
        public double edge;
        public double trendScore;
        public double shapeScore;
        public double driftRisk;
        public double breakRisk;
        public double recoveryScore;
        public double profileScore;
        public double numberBias;
        public double comboBias;
        public double riskScore;
        public double teacherWeight;
        public double stateConfidence;
        public int stateSample;
        public int cacheSample;
        public int recentSample;
        public double recentHitRate;
        public int saved;

        public String preDrawReasonFingerprint;
        public String preDrawReasonText;
        public String reasonValidLevel;
        public double reasonScore;
        public double reasonEdge;
        public double hitSimilarity;
        public double missSimilarity;
        public double contradictionScore;
        public double causalLift;
        public double causalConfidence;
        public int validReasonCount;
        public int falseReasonCount;
        public String k3RoleSummary;
        public int elite45Passed;
        public String elite45Summary;

        public LearningCase() {
            id = 0L;
            caseKey = "";
            taskKey = "";
            segmentKey = "";
            baseIssue = "";
            targetIssue = "";
            createdAt = 0L;
            targetIndex = -1;
            position = 0;
            actualNumber = 0;
            predictedK3 = 0;
            k3Mask = 0;
            hit = 0;
            stateKey = "";
            stateMode = AppConfig.MODE_UNKNOWN;
            trendKey = "";
            shapeKey = "";
            driftKey = "";
            breakKey = "";
            recoveryKey = "";
            transitionKey = "";
            expectedShape = "";
            actualShape = "";
            whyExpectedHit = "";
            whyActualHit = "";
            whyMiss = "";
            nextAdvice = "";
            mainReasonCode = AppConfig.MISS_UNKNOWN;
            affectedModule = AppConfig.ATTR_MODULE_UNKNOWN;
            finalP = 0.0d;
            dynamicGate = AppConfig.DEFAULT_DYNAMIC_GATE;
            edge = 0.0d;
            trendScore = 0.0d;
            shapeScore = 0.0d;
            driftRisk = 0.0d;
            breakRisk = 0.0d;
            recoveryScore = 0.0d;
            profileScore = 0.0d;
            numberBias = 0.0d;
            comboBias = 0.0d;
            riskScore = 0.0d;
            teacherWeight = 1.0d;
            stateConfidence = 0.0d;
            stateSample = 0;
            cacheSample = 0;
            recentSample = 0;
            recentHitRate = 0.0d;
            saved = 0;

            preDrawReasonFingerprint = "";
            preDrawReasonText = "";
            reasonValidLevel = REASON_LEVEL_C_EXPLAIN_ONLY;
            reasonScore = 0.0d;
            reasonEdge = 0.0d;
            hitSimilarity = 0.0d;
            missSimilarity = 0.0d;
            contradictionScore = 0.0d;
            causalLift = 0.0d;
            causalConfidence = 0.0d;
            validReasonCount = 0;
            falseReasonCount = 0;
            k3RoleSummary = "";
            elite45Passed = 0;
            elite45Summary = "";
        }

        public void rebuildCaseKey() {
            caseKey = AppConfig.buildLearningCaseKey(targetIssue, position);
            normalizeK3Mask();
        }

        public void normalizeK3Mask() {
            if (isValidK3Mask(predictedK3)) {
                k3Mask = predictedK3;
            } else if (isValidK3Mask(k3Mask)) {
                predictedK3 = k3Mask;
            } else {
                predictedK3 = 0;
                k3Mask = 0;
            }
        }

        public int effectiveK3Mask() {
            normalizeK3Mask();
            return isValidK3Mask(predictedK3) ? predictedK3 : k3Mask;
        }

        public boolean isHit() {
            return hit == 1;
        }

        public boolean predictedContainsActual() {
            return maskContains(effectiveK3Mask(), actualNumber);
        }

        public String buildSummaryText() {
            normalizeK3Mask();
            StringBuilder sb = new StringBuilder();
            sb.append("目标期").append(targetIssue);
            sb.append(" P").append(position);
            sb.append(" 实开").append(UiFormatter.number(actualNumber));
            sb.append("，预测").append(maskToText(effectiveK3Mask()));
            sb.append(isHit() ? "，命中" : "，未中");

            if (preDrawReasonText != null && preDrawReasonText.length() > 0) {
                sb.append("，开奖前原因=").append(preDrawReasonText);
            }

            if (isHit() && whyActualHit != null && whyActualHit.length() > 0) {
                sb.append("：").append(whyActualHit);
            } else if (!isHit() && whyMiss != null && whyMiss.length() > 0) {
                sb.append("：").append(whyMiss);
            }

            return sb.toString();
        }

        public StateNumberProfile toActualNumberProfile() {
            StateNumberProfile p = new StateNumberProfile();
            p.stateKey = stateKey;
            p.position = position;
            p.number = actualNumber;
            p.sampleCount = 1;
            p.hitCount = 1;
            p.recentSample = 1;
            p.recentHit = 1;
            p.trendScore = trendScore;
            p.shapeScore = shapeScore;
            p.riskScore = riskScore;
            p.whyText = isHit() ? whyActualHit : nextAdvice;
            p.updatedAt = createdAt > 0L ? createdAt : System.currentTimeMillis();
            p.recalc();
            return p;
        }

        public RuntimeProfileCache toRuntimeProfile() {
            normalizeK3Mask();
            RuntimeProfileCache p = new RuntimeProfileCache();
            p.stateKey = stateKey;
            p.position = position;
            p.k3Mask = effectiveK3Mask();
            p.sampleCount = 1;
            p.hitCount = hit == 1 ? 1 : 0;
            p.recentSample = 1;
            p.recentHit = hit == 1 ? 1 : 0;
            p.avgFinalP = finalP;
            p.avgGate = dynamicGate;
            p.riskScore = riskScore;
            p.teacherWeight = teacherWeight;
            p.summaryText = buildSummaryText();
            p.updatedAt = createdAt > 0L ? createdAt : System.currentTimeMillis();
            p.recalc();
            return p;
        }
    }

    public static final class RuntimeProfileCache {
        public String profileKey;
        public String stateKey;
        public int position;
        public int k3Mask;
        public int sampleCount;
        public int hitCount;
        public double hitRate;
        public int recentSample;
        public int recentHit;
        public double recentHitRate;
        public double avgFinalP;
        public double avgGate;
        public double riskScore;
        public double teacherWeight;
        public double profileScore;
        public double stateConfidence;
        public long updatedAt;
        public String summaryText;

        public RuntimeProfileCache() {
            profileKey = "";
            stateKey = "";
            position = 0;
            k3Mask = 0;
            sampleCount = 0;
            hitCount = 0;
            hitRate = 0.0d;
            recentSample = 0;
            recentHit = 0;
            recentHitRate = 0.0d;
            avgFinalP = 0.0d;
            avgGate = AppConfig.DEFAULT_DYNAMIC_GATE;
            riskScore = 0.0d;
            teacherWeight = 1.0d;
            profileScore = 0.0d;
            stateConfidence = 0.0d;
            updatedAt = 0L;
            summaryText = "";
        }

        public void rebuildKey() {
            profileKey = AppConfig.buildRuntimeProfileKey(stateKey, position, k3Mask);
        }

        public void recalc() {
            hitRate = sampleCount > 0 ? ((double) hitCount) / ((double) sampleCount) : 0.0d;
            recentHitRate =
                    recentSample > 0 ? ((double) recentHit) / ((double) recentSample) : 0.0d;

            double sampleScore =
                    sampleCount >= AppConfig.COMBO_SAMPLE_HIGH
                            ? 1.0d
                            : ((double) sampleCount) / ((double) AppConfig.COMBO_SAMPLE_HIGH);
            double rateScore = hitRate;
            double recentScore = recentHitRate > 0.0d ? recentHitRate : hitRate;

            profileScore =
                    AppConfig.clampProbability(
                            rateScore * 0.45d + recentScore * 0.35d + sampleScore * 0.20d);
            stateConfidence =
                    AppConfig.clampProbability(sampleScore * 0.60d + (1.0d - riskScore) * 0.40d);
            teacherWeight = clampTeacherWeight(teacherWeight);
            rebuildKey();
        }

        public boolean isStrongEnoughForBet() {
            recalc();
            return sampleCount >= AppConfig.MIN_CACHE_SAMPLE_FOR_BET
                    && hitRate >= AppConfig.COMBO_HIT_RATE_MIN
                    && riskScore <= AppConfig.MAX_RISK_FOR_BET;
        }

        public boolean isFresh(long now) {
            return updatedAt > 0L && now - updatedAt <= AppConfig.RUNTIME_PROFILE_CACHE_TTL_MS;
        }
    }

    public static final class StateNumberProfile {
        public String profileKey;
        public String stateKey;
        public int position;
        public int number;
        public int sampleCount;
        public int hitCount;
        public double hitRate;
        public int recentSample;
        public int recentHit;
        public double recentHitRate;
        public double trendScore;
        public double shapeScore;
        public double riskScore;
        public double numberBias;
        public double confidence;
        public String whyText;
        public long updatedAt;

        public StateNumberProfile() {
            profileKey = "";
            stateKey = "";
            position = 0;
            number = 0;
            sampleCount = 0;
            hitCount = 0;
            hitRate = 0.0d;
            recentSample = 0;
            recentHit = 0;
            recentHitRate = 0.0d;
            trendScore = 0.0d;
            shapeScore = 0.0d;
            riskScore = 0.0d;
            numberBias = 0.0d;
            confidence = 0.0d;
            whyText = "";
            updatedAt = 0L;
        }

        public void rebuildKey() {
            profileKey = AppConfig.buildStateNumberProfileKey(stateKey, position, number);
        }

        public void recalc() {
            hitRate = sampleCount > 0 ? ((double) hitCount) / ((double) sampleCount) : 0.0d;
            recentHitRate =
                    recentSample > 0 ? ((double) recentHit) / ((double) recentSample) : 0.0d;

            double sampleScore =
                    sampleCount >= AppConfig.EFFECTIVE_SAMPLE_HIGH
                            ? 1.0d
                            : ((double) sampleCount) / ((double) AppConfig.EFFECTIVE_SAMPLE_HIGH);
            double recentScore = recentHitRate > 0.0d ? recentHitRate : hitRate;
            numberBias =
                    AppConfig.clampProbability(
                            hitRate * 0.30d
                                    + recentScore * 0.30d
                                    + trendScore * 0.18d
                                    + shapeScore * 0.12d
                                    + sampleScore * 0.10d);
            numberBias = AppConfig.safePenaltyProb(numberBias, riskScore * 0.10d);
            confidence =
                    AppConfig.clampProbability(sampleScore * 0.70d + (1.0d - riskScore) * 0.30d);
            rebuildKey();
        }

        public boolean isUsableForRealtime() {
            recalc();
            return AppConfig.isValidPosition(position)
                    && AppConfig.isValidNumber(number)
                    && sampleCount >= AppConfig.MIN_STATE_SAMPLE_FOR_BET / 2
                    && riskScore <= AppConfig.MAX_RISK_FOR_BET;
        }

        public boolean isFresh(long now) {
            return updatedAt > 0L && now - updatedAt <= AppConfig.STATE_NUMBER_PROFILE_CACHE_TTL_MS;
        }
    }

    public static final class RealtimeFastContext {
        public String baseIssue;
        public String targetIssue;
        public long startedAt;
        public long now;
        public long countdownMs;
        public int historyCount;
        public int fastWindow;
        public int midWindow;
        public int longWindow;
        public List<Draw> recentDraws;
        public TeacherSnapshot teacherSnapshot;
        public TeacherMatrixReport teacherMatrixReport;
        public RuntimeStats runtimeStats;
        public int realtimeRunning;
        public int learningRunning;
        public int openTradeExists;
        public int timeoutFlag;
        public String statusText;
        public String warningText;

        public RealtimeFastContext() {
            baseIssue = "";
            targetIssue = "";
            startedAt = 0L;
            now = 0L;
            countdownMs = -1L;
            historyCount = 0;
            fastWindow = AppConfig.REALTIME_WINDOW_FAST;
            midWindow = AppConfig.REALTIME_WINDOW_MID;
            longWindow = AppConfig.REALTIME_WINDOW_LONG;
            recentDraws = new ArrayList<Draw>();
            teacherSnapshot = null;
            teacherMatrixReport = null;
            runtimeStats = null;
            realtimeRunning = 0;
            learningRunning = 0;
            openTradeExists = 0;
            timeoutFlag = 0;
            statusText = "";
            warningText = "";
        }

        public void markStart(String base, String target) {
            baseIssue = safeIssue(base);
            targetIssue = safeIssue(target);
            startedAt = System.currentTimeMillis();
            now = startedAt;
            realtimeRunning = 1;
        }

        public long costMs() {
            long t = System.currentTimeMillis();
            if (startedAt <= 0L) return 0L;
            return t - startedAt;
        }

        public boolean isTimeout() {
            return costMs() >= AppConfig.REALTIME_HARD_TIMEOUT_MS;
        }

        public boolean hasEnoughHistory() {
            return historyCount >= AppConfig.MIN_DRAWS_ANALYZE;
        }

        public Draw latestDraw() {
            if (recentDraws == null || recentDraws.size() == 0) return null;
            return recentDraws.get(recentDraws.size() - 1);
        }
    }

    public static final class RealtimeFastResult {
        public String baseIssue;
        public String targetIssue;
        public long startedAt;
        public long finishedAt;
        public long usedTimeMs;
        public String decisionType;
        public int position;
        public int n1;
        public int n2;
        public int n3;
        public int k3Mask;
        public double baseP;
        public double finalP;
        public double dynamicGate;
        public double edge;
        public double riskScore;
        public double profileScore;
        public double numberBias;
        public double comboBias;
        public double teacherWeight;
        public double stateConfidence;
        public int stateSample;
        public int cacheSample;
        public int recentSample;
        public double recentHitRate;
        public String stateKey;
        public String stateMode;
        public String trendKey;
        public String shapeKey;
        public String driftKey;
        public String breakKey;
        public String recoveryKey;
        public String blockCode;
        public String reasonSummary;
        public String reasonText;
        public int timeoutFlag;
        public int saved;

        public int teacherMatrixSample;
        public int teacherMatrixStateSample;
        public int teacherMatrixComboSample;
        public double teacherMatrixHitRate;
        public double teacherMatrixRecentHitRate;
        public double teacherMatrixConfidence;
        public double teacherMatrixProbAdjust;
        public double teacherMatrixGateAdjust;
        public double teacherMatrixRiskPenalty;
        public double teacherMatrixScore;
        public double teacherMatrixPositionScore;
        public double teacherMatrixNumberScore;
        public double teacherMatrixComboScore;
        public double teacherMatrixStateScore;
        public int teacherMatrixApplied;
        public String teacherMatrixStatus;
        public String teacherMatrixProfileKey;
        public String teacherMatrixComboKey;
        public String teacherMatrixSummary;

        /* V20 上游证据补实字段：只承接字段，不决定 BET */
        public int teacherWeakNoBetFlag;
        public int teacherMatrixWeakNoBetFlag;
        public int teacherMatrixOrdinaryBetBlockFlag;
        public int teacherMatrixSupportsFinalGate;
        public int teacherMatrixSupportsBet;
        public int teacherMatrixRiskFlag;
        public int teacherMatrixFreezeFlag;
        public String teacherOrdinaryBetPolicy;
        public String teacherMatrixOrdinaryBetPolicy;
        public String teacherFieldSource;
        public String teacherFieldVersion;
        public String teacherNumberStatus;
        public String teacherStatus;
        public String teacherStatusClass;
        public double teacherEvidenceContribution;
        public double teacherMatrixEvidenceContribution;
        public double teacherCounterContribution;
        public double teacherMatrixCounterContribution;
        public double teacherSampleReliability;
        public double teacherRecentValidity;

        public String v20Status;
        public String dynamicStatus;
        public String dynamicCauseStatus;
        public String v20ActionAdvice;
        public String v20FieldSource;
        public String v20FieldVersion;
        public int v20EmptyFlag;
        public double v20SupportScore;
        public double v20RiskScore;
        public double v20EvidenceContribution;
        public double v20CounterContribution;
        public double v20SampleReliability;
        public double v20RecentValidity;
        public int similarWindowSample;
        public double similarWindowHitRate;
        public String v20WindowStateKey;
        public String windowStateKey;
        public String finalDecisionWindowStateKey;
        public String dynamicWindowStateKey;
        public String probabilityWindowStateKey;

        public double coverageQualityScore;
        public double thirdNumberQualityScore;
        public double k3CoverageRisk;
        public int falseHighPFlag;
        public int probabilityFalseHighPFlag;
        public double probabilityFalseHighRisk;
        public int probabilityPreferredEdgeBand;
        public int probabilityHighEdgeFlag;
        public int probabilityPreferredPBand;
        public int probabilityTeacherWeakOrEmpty;
        public int probabilityV20UnknownOrEmpty;
        public int probabilityStrongCauseFlag;
        public double coverageProbDiscount;
        public double coverageProbBoost;
        public double coverageK3HitRate32;
        public double coverageK3HitRate64;
        public double coverageK3HitRate128;
        public double coverageNumberRate1;
        public double coverageNumberRate2;
        public double coverageNumberRate3;
        public double finalPBeforeCoverage;
        public double finalPAfterCoverage;
        public double coverageAdjustedP;
        public String probabilityCoverageSummary;
        public String coverageQualitySummary;
        public String probabilityCoverageActionAdvice;
        public String probabilityActionAdvice;
        public String probabilityFieldVersion;

        public String dynamicGateQualityStatus;
        public int dynamicGateFalseHighPFlag;
        public int dynamicGatePreferredEdgeBand;
        public int dynamicGateTeacherV20WeakFlag;
        public String dynamicGateEdgePolicy;
        public double dynamicGateCoverageRisk;
        public double dynamicGateCoverageQualityScore;
        public double dynamicGateThirdNumberQuality;
        public double dynamicGateVirtualEdgeRisk;
        public String dynamicGateActionAdvice;
        public String dynamicGateFieldVersion;
        public String dynamicGateVersion;

        public String finalDecisionVersion;
        public String finalDecisionEdgePolicy;
        public String finalDecisionTeacherPolicy;
        public String finalDecisionV20Policy;
        public String finalDecisionBetDomain;
        public String finalDecisionRejectCode;
        public String finalDecisionShadowReason;
        public String finalDecisionStrongOverrideTrace;
        public double finalDecisionCoverageRisk;
        public double finalDecisionCoverageQuality;
        public double finalDecisionThirdNumberQuality;
        public int finalDecisionFalseHighPFlag;

        /* V20动态因果窗口字段：只承接字段，不直接决定BET */
        public String dynamicClusterId;
        public String dynamicClusterStatus;
        public String dynamicClusterPrototypeKey;
        public String dynamicClusterFeatureKey;
        public int dynamicClusterSample;
        public int dynamicClusterHitCount;
        public int dynamicClusterMissCount;
        public int dynamicClusterRecentSample;
        public int dynamicClusterRecentHitCount;
        public double dynamicClusterHitRate;
        public double dynamicClusterRecentHitRate;
        public double dynamicClusterConfidence;
        public double dynamicClusterDistance;
        public double dynamicClusterVolatility;
        public int dynamicClusterMaxLose;
        public double dynamicClusterCounterScore;
        public String dynamicClusterVersion;
        public String dynamicClusterReason;

        public String positionNeedKey;
        public String positionNeedSummary;
        public double positionNeedStrength;
        public double positionNeedChaosRisk;
        public double positionNeedRepairScore;
        public double positionNeedRelayScore;
        public String numberSupplyKey;
        public String numberSupplySummary;
        public double numberSupplyStrength;
        public double numberSupplyRisk;
        public String needSupplyMatchKey;
        public double needSupplyMatchScore;
        public double needSupplyThirdQuality;
        public double needSupplyRoleDiversity;
        public double needSupplyRisk;
        public String needSupplySummary;

        public String k3RoleMain;
        public String k3RoleAssist;
        public String k3RoleDefense;
        public int k3RoleMainNumber;
        public int k3RoleAssistNumber;
        public int k3RoleDefenseNumber;
        public String k3RoleStructure;

        public String counterfactualReasonKey;
        public int counterfactualRank;
        public double counterfactualRankScore;
        public double counterfactualWhyNotRisk;
        public String counterfactualWhyNot;
        public String counterfactualSummary;
        public String causeAblationKey;
        public double causeAblationScore;
        public int causeAblationFalseCauseCount;
        public String causeAblationSummary;
        public String missReasonType;
        public String missReasonMatrixKey;
        public String missReasonDetail;

        public int adaptiveJudgeCandidateCount;
        public String adaptiveJudgeStatus;
        public String adaptiveJudgeAction;
        public String adaptiveJudgeDomain;
        public double adaptiveClusterHitRate;
        public double adaptiveClusterRecentHitRate;
        public int adaptiveClusterSample;
        public double adaptiveClusterConfidence;
        public double adaptiveCauseScore;
        public double adaptiveCounterScore;
        public double adaptivePromoteScore;
        public double adaptiveFreezeScore;
        public String adaptiveJudgeReason;
        public String adaptiveJudgeVersion;

        public String promotionClusterStatus;
        public double promotionClusterHitRate;
        public double promotionClusterRecentHitRate;
        public int promotionClusterSample;
        public String promotionClusterReason;

        public String whyBet;
        public String whyNoBet;
        public String whyHit;
        public String whyMiss;
        public String dynamicCausalSummary;

        public String preDrawReasonFingerprint;
        public String preDrawReasonText;
        public String reasonValidLevel;
        public double reasonScore;
        public double reasonEdge;
        public double hitSimilarity;
        public double missSimilarity;
        public double contradictionScore;
        public double causalLift;
        public double causalConfidence;
        public int validReasonCount;
        public int falseReasonCount;
        public int forwardReasonSample;
        public double forwardReasonHitRate;
        public double reasonWilsonScore;

        public String k3RoleSummary;
        public int k3MainNumber;
        public int k3SupportNumber;
        public int k3DefenseNumber;
        public double comboRoleDiversity;
        public double comboConflict;

        public int fingerprintCooling;
        public int elite45Passed;
        public String elite45Status;
        public String elite45BlockCode;
        public double elite45Gate;
        public String elite45Summary;

        public RealtimeFastResult() {
            baseIssue = "";
            targetIssue = "";
            startedAt = 0L;
            finishedAt = 0L;
            usedTimeMs = 0L;
            decisionType = AppConfig.DECISION_HOLD;
            position = 0;
            n1 = 0;
            n2 = 0;
            n3 = 0;
            k3Mask = 0;
            baseP = 0.0d;
            finalP = 0.0d;
            dynamicGate = AppConfig.DEFAULT_DYNAMIC_GATE;
            edge = 0.0d;
            riskScore = 0.0d;
            profileScore = 0.0d;
            numberBias = 0.0d;
            comboBias = 0.0d;
            teacherWeight = 1.0d;
            stateConfidence = 0.0d;
            stateSample = 0;
            cacheSample = 0;
            recentSample = 0;
            recentHitRate = 0.0d;
            stateKey = "";
            stateMode = AppConfig.MODE_UNKNOWN;
            trendKey = "";
            shapeKey = "";
            driftKey = "";
            breakKey = "";
            recoveryKey = "";
            blockCode = "";
            reasonSummary = "";
            reasonText = "";
            timeoutFlag = 0;
            saved = 0;

            teacherMatrixSample = 0;
            teacherMatrixStateSample = 0;
            teacherMatrixComboSample = 0;
            teacherMatrixHitRate = 0.0d;
            teacherMatrixRecentHitRate = 0.0d;
            teacherMatrixConfidence = 0.0d;
            teacherMatrixProbAdjust = 0.0d;
            teacherMatrixGateAdjust = 0.0d;
            teacherMatrixRiskPenalty = 0.0d;
            teacherMatrixScore = 0.0d;
            teacherMatrixPositionScore = 0.0d;
            teacherMatrixNumberScore = 0.0d;
            teacherMatrixComboScore = 0.0d;
            teacherMatrixStateScore = 0.0d;
            teacherMatrixApplied = 0;
            teacherMatrixStatus = AppConfig.TEACHER_MATRIX_STATUS_NONE;
            teacherMatrixProfileKey = "";
            teacherMatrixComboKey = "";
            teacherMatrixSummary = "";

            teacherWeakNoBetFlag = 0;
            teacherMatrixWeakNoBetFlag = 0;
            teacherMatrixOrdinaryBetBlockFlag = 0;
            teacherMatrixSupportsFinalGate = 0;
            teacherMatrixSupportsBet = 0;
            teacherMatrixRiskFlag = 0;
            teacherMatrixFreezeFlag = 0;
            teacherOrdinaryBetPolicy = "";
            teacherMatrixOrdinaryBetPolicy = "";
            teacherFieldSource = "";
            teacherFieldVersion = "";
            teacherNumberStatus = "";
            teacherStatus = "";
            teacherStatusClass = "";
            teacherEvidenceContribution = 0.0d;
            teacherMatrixEvidenceContribution = 0.0d;
            teacherCounterContribution = 0.0d;
            teacherMatrixCounterContribution = 0.0d;
            teacherSampleReliability = 0.0d;
            teacherRecentValidity = 0.0d;

            v20Status = "";
            dynamicStatus = "";
            dynamicCauseStatus = "";
            v20ActionAdvice = "";
            v20FieldSource = "";
            v20FieldVersion = "";
            v20EmptyFlag = 0;
            v20SupportScore = 0.0d;
            v20RiskScore = 0.0d;
            v20EvidenceContribution = 0.0d;
            v20CounterContribution = 0.0d;
            v20SampleReliability = 0.0d;
            v20RecentValidity = 0.0d;
            similarWindowSample = 0;
            similarWindowHitRate = 0.0d;
            v20WindowStateKey = "";
            windowStateKey = "";
            finalDecisionWindowStateKey = "";
            dynamicWindowStateKey = "";
            probabilityWindowStateKey = "";

            coverageQualityScore = 0.0d;
            thirdNumberQualityScore = 0.0d;
            k3CoverageRisk = 0.0d;
            falseHighPFlag = 0;
            probabilityFalseHighPFlag = 0;
            probabilityFalseHighRisk = 0.0d;
            probabilityPreferredEdgeBand = 0;
            probabilityHighEdgeFlag = 0;
            probabilityPreferredPBand = 0;
            probabilityTeacherWeakOrEmpty = 0;
            probabilityV20UnknownOrEmpty = 0;
            probabilityStrongCauseFlag = 0;
            coverageProbDiscount = 0.0d;
            coverageProbBoost = 0.0d;
            coverageK3HitRate32 = 0.0d;
            coverageK3HitRate64 = 0.0d;
            coverageK3HitRate128 = 0.0d;
            coverageNumberRate1 = 0.0d;
            coverageNumberRate2 = 0.0d;
            coverageNumberRate3 = 0.0d;
            finalPBeforeCoverage = 0.0d;
            finalPAfterCoverage = 0.0d;
            coverageAdjustedP = 0.0d;
            probabilityCoverageSummary = "";
            coverageQualitySummary = "";
            probabilityCoverageActionAdvice = "";
            probabilityActionAdvice = "";
            probabilityFieldVersion = "";

            dynamicGateQualityStatus = "";
            dynamicGateFalseHighPFlag = 0;
            dynamicGatePreferredEdgeBand = 0;
            dynamicGateTeacherV20WeakFlag = 0;
            dynamicGateEdgePolicy = "";
            dynamicGateCoverageRisk = 0.0d;
            dynamicGateCoverageQualityScore = 0.0d;
            dynamicGateThirdNumberQuality = 0.0d;
            dynamicGateVirtualEdgeRisk = 0.0d;
            dynamicGateActionAdvice = "";
            dynamicGateFieldVersion = "";
            dynamicGateVersion = "";

            finalDecisionVersion = "";
            finalDecisionEdgePolicy = "";
            finalDecisionTeacherPolicy = "";
            finalDecisionV20Policy = "";
            finalDecisionBetDomain = "";
            finalDecisionRejectCode = "";
            finalDecisionShadowReason = "";
            finalDecisionStrongOverrideTrace = "";
            finalDecisionCoverageRisk = 0.0d;
            finalDecisionCoverageQuality = 0.0d;
            finalDecisionThirdNumberQuality = 0.0d;
            finalDecisionFalseHighPFlag = 0;
            initDynamicCausalBridgeFields(this);
            initMainChainBridgeFields(this);

            preDrawReasonFingerprint = "";
            preDrawReasonText = "";
            reasonValidLevel = REASON_LEVEL_C_EXPLAIN_ONLY;
            reasonScore = 0.0d;
            reasonEdge = 0.0d;
            hitSimilarity = 0.0d;
            missSimilarity = 0.0d;
            contradictionScore = 0.0d;
            causalLift = 0.0d;
            causalConfidence = 0.0d;
            validReasonCount = 0;
            falseReasonCount = 0;
            forwardReasonSample = 0;
            forwardReasonHitRate = 0.0d;
            reasonWilsonScore = 0.0d;
            k3RoleSummary = "";
            k3MainNumber = 0;
            k3SupportNumber = 0;
            k3DefenseNumber = 0;
            comboRoleDiversity = 0.0d;
            comboConflict = 0.0d;
            fingerprintCooling = 0;
            elite45Passed = 0;
            elite45Status = BLOCK_ELITE45_NOT_PASS;
            elite45BlockCode = BLOCK_ELITE45_NOT_PASS;
            elite45Gate = ELITE45_GATE_DEFAULT;
            elite45Summary = "";
        }

        public void setNumbers(int a, int b, int c) {
            n1 = a;
            n2 = b;
            n3 = c;
            k3Mask = buildK3Mask(a, b, c);
        }

        public void finish() {
            finishedAt = System.currentTimeMillis();
            if (startedAt > 0L) {
                usedTimeMs = finishedAt - startedAt;
                if (usedTimeMs < 0L) usedTimeMs = 0L;
            }
            decisionType = AppConfig.normalizeDecision(decisionType);
            if (teacherMatrixScore <= 0.0d) {
                teacherMatrixScore =
                        buildTeacherMatrixScore(
                                teacherMatrixNumberScore,
                                teacherMatrixPositionScore,
                                teacherMatrixStateScore,
                                teacherMatrixComboScore);
            }
            edge = finalP - dynamicGate;
            normalizeDynamicCausalBridgeFields(this);
        }

        public Decision toDecision() {
            Decision d = new Decision();
            d.applyRealtimeFastResult(this);
            d.createdAt = finishedAt > 0L ? finishedAt : System.currentTimeMillis();
            d.dataOk = 1;
            d.syncAction();
            Models.copySharedPublicFieldsV20(this, d);
            return d;
        }

        public boolean isBet() {
            return AppConfig.DECISION_BET.equals(decisionType);
        }

        public boolean isShadow() {
            return AppConfig.DECISION_SHADOW.equals(decisionType);
        }
    }

    public static final class AutoLearnTask {
        public String taskKey;
        public String taskType;
        public int windowSize;
        public int segmentStartIndex;
        public int segmentEndIndex;
        public int nextTargetIndex;
        public int lastStartIndex;
        public String usedSegments;
        public int batchStartIndex;
        public int batchEndIndex;
        public int maxTargets;
        public int maxPositionSamples;
        public long randomSeed;
        public String startIssue;
        public String endIssue;
        public String lastIssue;
        public String progress;
        public String status;
        public long createdAt;
        public long updatedAt;
        public String summaryText;

        public AutoLearnTask() {
            taskKey = "";
            taskType = AppConfig.AUTO_LEARN_TASK_DEFAULT;
            windowSize = AppConfig.AUTO_LEARN_DEFAULT_WINDOW;
            segmentStartIndex = 0;
            segmentEndIndex = 0;
            nextTargetIndex = 0;
            lastStartIndex = 0;
            usedSegments = "";
            batchStartIndex = 0;
            batchEndIndex = 0;
            maxTargets = AppConfig.AUTO_LEARN_MAX_TARGETS_PER_BATCH;
            maxPositionSamples = AppConfig.AUTO_LEARN_MAX_POSITION_SAMPLES_PER_BATCH;
            randomSeed = AppConfig.AUTO_LEARN_RANDOM_SEED_DEFAULT;
            startIssue = "";
            endIssue = "";
            lastIssue = "";
            progress = "";
            status = AppConfig.AUTO_LEARN_STATUS_IDLE;
            createdAt = 0L;
            updatedAt = 0L;
            summaryText = "";
        }

        public void normalize() {
            taskType = AppConfig.normalizeAutoLearnTask(taskType);
            windowSize = AppConfig.getAutoLearnWindowByTask(taskType);
            taskKey = AppConfig.buildAutoLearnTaskKey(taskType, windowSize);
            if (maxTargets <= 0 || maxTargets > AppConfig.AUTO_LEARN_MAX_TARGETS_PER_BATCH) {
                maxTargets = AppConfig.AUTO_LEARN_MAX_TARGETS_PER_BATCH;
            }
            maxPositionSamples = maxTargets * AppConfig.POSITION_COUNT;
            if (maxPositionSamples > AppConfig.AUTO_LEARN_MAX_POSITION_SAMPLES_PER_BATCH) {
                maxPositionSamples = AppConfig.AUTO_LEARN_MAX_POSITION_SAMPLES_PER_BATCH;
            }
        }

        public boolean hasOpenSegment() {
            return segmentEndIndex > segmentStartIndex && nextTargetIndex <= segmentEndIndex;
        }

        public int calcBatchEnd() {
            int end = nextTargetIndex + maxTargets - 1;
            if (end > segmentEndIndex) end = segmentEndIndex;
            return end;
        }

        public void prepareBatch() {
            normalize();
            batchStartIndex = nextTargetIndex;
            batchEndIndex = calcBatchEnd();
            lastStartIndex = batchStartIndex;
            status = AppConfig.AUTO_LEARN_STATUS_RUNNING;
            updatedAt = System.currentTimeMillis();
        }

        public String segmentKey() {
            return AppConfig.buildAutoLearnSegmentKey(
                    taskType, windowSize, segmentStartIndex, segmentEndIndex);
        }

        public boolean containsUsedSegment(int start, int end) {
            if (usedSegments == null || usedSegments.length() == 0) return false;
            String token = start + "-" + end;
            return usedSegments.indexOf(token) >= 0;
        }

        public void appendUsedSegment(int start, int end) {
            String token = start + "-" + end;
            if (containsUsedSegment(start, end)) return;
            if (usedSegments == null || usedSegments.length() == 0) {
                usedSegments = token;
            } else {
                usedSegments = usedSegments + "," + token;
            }
        }
    }

    public static final class AutoLearnCursor {
        public String taskKey;
        public String taskType;
        public int windowSize;
        public int segmentStartIndex;
        public int segmentEndIndex;
        public int nextTargetIndex;
        public int lastStartIndex;
        public long randomSeed;
        public String lastIssue;
        public String progress;
        public String status;
        public String usedSegments;
        public long updatedAt;

        public AutoLearnCursor() {
            taskKey = "";
            taskType = AppConfig.AUTO_LEARN_TASK_DEFAULT;
            windowSize = AppConfig.AUTO_LEARN_DEFAULT_WINDOW;
            segmentStartIndex = 0;
            segmentEndIndex = 0;
            nextTargetIndex = 0;
            lastStartIndex = 0;
            randomSeed = AppConfig.AUTO_LEARN_RANDOM_SEED_DEFAULT;
            lastIssue = "";
            progress = "";
            status = AppConfig.AUTO_LEARN_STATUS_IDLE;
            usedSegments = "";
            updatedAt = 0L;
        }

        public void fromTask(AutoLearnTask task) {
            if (task == null) return;
            taskKey = task.taskKey;
            taskType = task.taskType;
            windowSize = task.windowSize;
            segmentStartIndex = task.segmentStartIndex;
            segmentEndIndex = task.segmentEndIndex;
            nextTargetIndex = task.nextTargetIndex;
            lastStartIndex = task.lastStartIndex > 0 ? task.lastStartIndex : task.batchStartIndex;
            randomSeed = task.randomSeed;
            lastIssue = task.lastIssue;
            progress = task.progress;
            status = task.status;
            usedSegments = task.usedSegments == null ? "" : task.usedSegments;
            updatedAt = System.currentTimeMillis();
        }

        public AutoLearnTask toTask() {
            AutoLearnTask t = new AutoLearnTask();
            t.taskKey = taskKey;
            t.taskType = taskType;
            t.windowSize = windowSize;
            t.segmentStartIndex = segmentStartIndex;
            t.segmentEndIndex = segmentEndIndex;
            t.nextTargetIndex = nextTargetIndex;
            t.lastStartIndex = lastStartIndex;
            t.usedSegments = usedSegments == null ? "" : usedSegments;
            t.lastIssue = lastIssue;
            t.progress = progress;
            t.status = status;
            t.randomSeed = randomSeed;
            t.updatedAt = updatedAt;
            t.normalize();
            return t;
        }

        public boolean containsUsedSegment(int start, int end) {
            if (usedSegments == null || usedSegments.length() == 0) return false;
            String token = start + "-" + end;
            return usedSegments.indexOf(token) >= 0;
        }

        public void appendUsedSegment(int start, int end) {
            String token = start + "-" + end;
            if (containsUsedSegment(start, end)) return;
            if (usedSegments == null || usedSegments.length() == 0) {
                usedSegments = token;
            } else {
                usedSegments = usedSegments + "," + token;
            }
        }
    }

    public static final class LearningBatchResult {
        public String taskKey;
        public String segmentKey;
        public int windowSize;
        public int targetCount;
        public int positionSampleCount;
        public int savedCaseCount;
        public int duplicatedCaseCount;
        public int hitCount;
        public int missCount;
        public int profileUpdateCount;
        public int numberProfileUpdateCount;
        public int teacherMatrixSampleCount;
        public int teacherMatrixProfileUpdateCount;
        public int reasonSampleCount;
        public int reasonProfileUpdateCount;
        public long startedAt;
        public long finishedAt;
        public long costMs;
        public String status;
        public String summaryText;
        public String errorText;
        public ArrayList<LearningCase> cases;

        public LearningBatchResult() {
            taskKey = "";
            segmentKey = "";
            windowSize = 0;
            targetCount = 0;
            positionSampleCount = 0;
            savedCaseCount = 0;
            duplicatedCaseCount = 0;
            hitCount = 0;
            missCount = 0;
            profileUpdateCount = 0;
            numberProfileUpdateCount = 0;
            teacherMatrixSampleCount = 0;
            teacherMatrixProfileUpdateCount = 0;
            reasonSampleCount = 0;
            reasonProfileUpdateCount = 0;
            startedAt = 0L;
            finishedAt = 0L;
            costMs = 0L;
            status = AppConfig.AUTO_LEARN_STATUS_IDLE;
            summaryText = "";
            errorText = "";
            cases = new ArrayList<LearningCase>();
        }

        public void addCase(LearningCase c) {
            if (c == null) return;
            cases.add(c);
            positionSampleCount++;
            if (c.hit == 1) hitCount++;
            else missCount++;
        }

        public void finishOk() {
            finishedAt = System.currentTimeMillis();
            if (startedAt > 0L) costMs = finishedAt - startedAt;
            if (costMs < 0L) costMs = 0L;
            status = AppConfig.AUTO_LEARN_STATUS_OK;
            buildSummary();
        }

        public void finishError(String text) {
            finishedAt = System.currentTimeMillis();
            if (startedAt > 0L) costMs = finishedAt - startedAt;
            if (costMs < 0L) costMs = 0L;
            status = AppConfig.AUTO_LEARN_STATUS_ERROR;
            errorText = text == null ? "" : text;
            buildSummary();
        }

        public void buildSummary() {
            StringBuilder sb = new StringBuilder();
            sb.append("后台学习").append(segmentKey);
            sb.append("：目标期=").append(targetCount);
            sb.append("，位置样本=").append(positionSampleCount);
            sb.append("，教师矩阵样本=").append(teacherMatrixSampleCount);
            sb.append("，原因样本=").append(reasonSampleCount);
            sb.append("，保存=").append(savedCaseCount);
            sb.append("，重复=").append(duplicatedCaseCount);
            sb.append("，命中样本=").append(hitCount);
            sb.append("，未中样本=").append(missCount);
            sb.append("，画像更新=")
                    .append(
                            profileUpdateCount
                                    + teacherMatrixProfileUpdateCount
                                    + reasonProfileUpdateCount);
            sb.append("，耗时=").append(costMs).append("ms");
            if (errorText != null && errorText.length() > 0) {
                sb.append("，错误=").append(errorText);
            }
            summaryText = sb.toString();
        }
    }

    public static final class GateAudit {
        public String targetIssue;
        public long createdAt;
        public double baseGate;
        public double dynamicGate;
        public double teacherAdjust;
        public double teacherMatrixAdjust;
        public double reasonAdjust;
        public double elite45Adjust;
        public double auditAdjust;
        public double riskAdjust;
        public double frequencyAdjust;
        public double sampleAdjust;
        public double finalP;
        public double edge;
        public double riskScore;
        public int stateSample;
        public int cacheSample;
        public int teacherMatrixSample;
        public double teacherMatrixConfidence;
        public int allowBet;
        public String blockCode;
        public String gateReason;

        public GateAudit() {
            targetIssue = "";
            createdAt = 0L;
            baseGate = AppConfig.DEFAULT_DYNAMIC_GATE;
            dynamicGate = AppConfig.DEFAULT_DYNAMIC_GATE;
            teacherAdjust = 0.0d;
            teacherMatrixAdjust = 0.0d;
            reasonAdjust = 0.0d;
            elite45Adjust = 0.0d;
            auditAdjust = 0.0d;
            riskAdjust = 0.0d;
            frequencyAdjust = 0.0d;
            sampleAdjust = 0.0d;
            finalP = 0.0d;
            edge = 0.0d;
            riskScore = 0.0d;
            stateSample = 0;
            cacheSample = 0;
            teacherMatrixSample = 0;
            teacherMatrixConfidence = 0.0d;
            allowBet = 0;
            blockCode = "";
            gateReason = "";
        }

        public void recalc() {
            dynamicGate =
                    AppConfig.clampGate(
                            baseGate
                                    + teacherAdjust
                                    + teacherMatrixAdjust
                                    + reasonAdjust
                                    + elite45Adjust
                                    + auditAdjust
                                    + riskAdjust
                                    + frequencyAdjust
                                    + sampleAdjust);
            edge = finalP - dynamicGate;
        }
    }

    public static final class DecisionAuthorization {
        public String authorizationId = "";
        public String decisionId = "";
        public String buildId = "";
        public String cohortId = "";
        public String schemaVersion = "";
        public String pipelineVersion = "";
        public String calibrationVersion = "";
        public String authorizedMode = "SHADOW";
        public long authorizedAt;
        public double calibratedProbability;
        public double gate;
        public double edge;
        public double familyLCB;
        public double coverage;
        public double thirdQuality;
        public double actionUtility;
        public double uncertaintyPenalty;
        public double structureScore;
        public String laneCandidate = "SHADOW";
        public String teacherPolicy = "UNKNOWN";
        public String dynamicPolicy = "UNKNOWN";
        public String adaptivePolicy = "UNKNOWN";
        public String rejectCode = "";
        public int valid;
    }

    public static final class DecisionResult {

        /* 星河 V40：全号码分布、OOS校准、分层后验、不可变契约与唯一授权。 */
        public double rawRankScore;
        public double calibratedHitProbability;
        public double honestProbability;
        public double actionUtility;
        public double actionStructureScore;
        public double actionDiscriminationScore;
        public double actionMaturityScore;
        public double actionUncertaintyPenalty;
        public double actionFrequencySupport;
        /* V42 unified risk-coverage evidence.  These fields are immutable audit
         * evidence after lane compilation; they never create a BET. */
        public double v42AcceptScore;
        public double v42FamilyLcb;
        public double v42RegimeQuality;
        public double v42AssociationScore;
        public double v42DistillationRisk;
        public int v42SupportVotes;
        public String v42RegimeFamily;
        public String v42RiskCoverageMode;
        public String v42PrimaryReject;
        public String v42SecondaryReject;
        public String v42TertiaryReject;
        public String v42LanePolicyVersion;
        /* V43 canonical full-strategy evidence contract. */
        public double v43StrategySupport;
        public double v43StrategyRisk;
        public double v43StrategyConfidence;
        public double v43StrategyDiversity;
        public double v43StrategyMaturity;
        public double v43SafeProbability;
        public double v43ProbabilityResidual;
        public double v43AblationFragility;
        public double v43ProbabilityLock;
        public int v43EvidenceGroupCount;
        public int v43HardRisk;
        public String v43DominantEvidence;
        public String v43MissingEvidence;
        public String v43EvidenceFingerprint;
        public String v43ProbabilityLockHash;
        public String v43PipelineStage;
        public String v43InvariantStatus;
        public String v43StrategyVersion;
        public String actionUtilityVersion;
        public String laneCandidate;
        public int oosRankSlot;
        public int oosCandidateCount;
        public String buildId;
        public String cohortId;
        public String schemaVersion;
        public String contractDecisionId;
        public String candidateHash;
        public String snapshotMaxIssue;
        public String trainedUntilIssue;
        public String featureSchemaVersion;
        public String rankingVersion;
        public String calibrationVersion;
        public String gatePolicyVersion;
        public String pipelineVersion;
        public int contractValid;
        public String contractStatus;
        public String coarseFamilyKey;
        public String midFamilyKey;
        public String fineFamilyKey;
        public String positionFamilyKey;
        public String posteriorMatchedLevel;
        public String posteriorFamilyKey;
        public String posteriorPolicy;
        public String posteriorVersion;
        public int posteriorSample;
        public int posteriorHit;
        public double posteriorMean;
        public double posteriorWilsonLower;
        public double posteriorRecentRate;
        public double posteriorDrift;
        public double familyWilsonLower;
        public int calibrationBucket;
        public int calibrationSample;
        public int calibrationTotalSample;
        public int calibrationReady;
        public String calibrationStatus;
        public double calibrationBrier;
        public String authorizationId;
        public int authorizationValid;
        public String authorizedMode;
        public String authorizationRejectCode;
        public long authorizedAt;
        public double authorizationProbability;
        public double authorizationGate;
        public double authorizationEdge;
        public String teacherPolicy;
        public String dynamicPolicy;
        public String adaptivePolicy;
        public String microWalkForwardStatus;
        public String microWalkForwardVersion;
        public String microWalkForwardAsOfIssue;
        public int microWalkForwardSample;

        public String targetIssue;
        public String decisionType;
        public int position;
        public int n1;
        public int n2;
        public int n3;
        public int k3Mask;
        public double finalP;
        public double dynamicGate;
        public double edge;
        public double riskScore;
        /* V20主链闭环字段：只承接字段，不计算概率，不决定BET */
        public String clusterKey;
        public String factorKey;
        public double positionNeedScore;
        public double numberSupplyScore;
        public double coverageQuality;
        public double thirdQuality;
        public double counterEvidenceScore;
        public int similarSampleCount;
        public String adaptiveStatus;
        public String shadowPromotionStatus;
        public double finalPBeforeDiagnosis;
        public double finalPAfterDiagnosis;
        public double gate;
        public String rejectCode;
        public int realProbeAllowed;

        /* 链路修改升级V2字段：只承接，不计算概率，不决定BET */
        public double realBetFrequency50;
        public double realBetFrequency100;
        public double realBetFrequency300;
        public double frequencyDebtScore;
        public double frequencySupportScore;
        public double frequencyGateAdjust;
        public double qualifiedProbeScore;
        public double shadowPromotionScore;
        public double baseGate;
        public double riskAdjustedGate;
        public double effectiveProbeGate;
        public double rawEdge;
        public double effectiveProbeEdge;
        public double needSupplyScore;
        public double counterRisk;
        public int qualifiedProbe;
        public int realTrade;
        public int coverageAvailable;
        public int thirdQualityAvailable;
        public int promotionDomainAvailable;
        public String frequencyState;
        public String frequencyStateVersion;
        public String shadowPromotionDomain;
        public String decisionPath;
        public String betDomain;
        public String probeRejectReason;
        public String probeEligibilityVersion;
        public String ruleHealthStatus;
        public String snapshotFreshness;
        public String warmProfileFreshness;
        public String coldProfileFreshness;
        public String deepProfileFreshness;
        public String decisionVersionV2;
        public String evidenceAsOfIssue;
        public String snapshotAsOfIssue;
        public long snapshotCreatedAt;
        public long frequencyStateUpdatedAt;
        public int frequencyFloorRequiredCount;
        public int frequencyFloorTargetCount;
        public int frequencyFloorActualCount;
        public int frequencyFloorDebtCount;
        public int frequencyTargetDebtCount;
        public int frequencyFloorMandatory;
        public int frequencyFloorNoUpperCap;
        public double frequencyFloorRankScore;
        public String frequencyFloorVersion;

        /* 73487246：四窗口平滑频率、冻结域、Teacher/Dynamic分层审计。 */
        public double realBetFrequency20;
        public int frequencyObserved20;
        public int frequencyObserved50;
        public int frequencyObserved100;
        public int frequencyObserved300;
        public int frequencyBetCount20;
        public int frequencyBetCount50;
        public int frequencyBetCount100;
        public int frequencyBetCount300;
        public int frequencyDebt20;
        public int frequencyDebt50;
        public int frequencyDebt100;
        public int frequencyDebt300;
        public int frequencyTargetDebt20;
        public int frequencyTargetDebt50;
        public int frequencyTargetDebt100;
        public int frequencyTargetDebt300;
        public String frequencyUrgentWindow;
        public int frequencyIssuesSinceLastRealBet;
        public int frequencyIssuesSinceLastFloorBet;
        public int frequencyConsecutiveFloorBets;
        public int frequencyFloorMinGap;
        public int frequencyFloorPacingReady;
        public String frequencyFloorPacingReason;
        public String decisionQualityPath;
        public int evidenceDomainFrozen;
        public String evidenceDomainFrozenIssue;
        public long evidenceDomainFrozenAt;
        public String dynamicMatchedLevel;
        public int dynamicRealSample;
        public int dynamicShadowSample;
        public String moduleBuildVersion;

        public String blockCode;
        public String reasonText;
        public long createdAt;

        public DecisionResult() {
            targetIssue = "";
            decisionType = AppConfig.DECISION_HOLD;
            position = 0;
            n1 = 0;
            n2 = 0;
            n3 = 0;
            k3Mask = 0;
            finalP = 0.0d;
            dynamicGate = AppConfig.DEFAULT_DYNAMIC_GATE;
            edge = 0.0d;
            riskScore = 0.0d;
            blockCode = "";
            reasonText = "";
            createdAt = 0L;
            initMainChainBridgeFields(this);
        }

        public void setNumbers(int a, int b, int c) {
            n1 = a;
            n2 = b;
            n3 = c;
            k3Mask = buildK3Mask(a, b, c);
        }

        public boolean isBet() {
            return AppConfig.DECISION_BET.equals(decisionType);
        }
    }

    public static final class ReasonPack {
        public String targetIssue;
        public String decisionType;
        public String mainReason;
        public String positionReason;
        public String numberReason;
        public String comboReason;
        public String stateReason;
        public String trendReason;
        public String shapeReason;
        public String gateReason;
        public String riskReason;
        public String learnReason;
        public String teacherMatrixReason;
        public String preDrawReason;
        public String elite45Reason;
        public String lastHitMissReason;
        public String nextAdvice;
        public ArrayList<ReasonItem> items;

        public ReasonPack() {
            targetIssue = "";
            decisionType = AppConfig.DECISION_HOLD;
            mainReason = "";
            positionReason = "";
            numberReason = "";
            comboReason = "";
            stateReason = "";
            trendReason = "";
            shapeReason = "";
            gateReason = "";
            riskReason = "";
            learnReason = "";
            teacherMatrixReason = "";
            preDrawReason = "";
            elite45Reason = "";
            lastHitMissReason = "";
            nextAdvice = "";
            items = new ArrayList<ReasonItem>();
        }

        public void add(String level, String title, String detail, String code, boolean pass) {
            items.add(new ReasonItem(level, title, detail, code, pass));
        }

        public String buildText() {
            StringBuilder sb = new StringBuilder();
            appendLine(sb, mainReason);
            appendLine(sb, positionReason);
            appendLine(sb, numberReason);
            appendLine(sb, comboReason);
            appendLine(sb, stateReason);
            appendLine(sb, trendReason);
            appendLine(sb, shapeReason);
            appendLine(sb, gateReason);
            appendLine(sb, riskReason);
            appendLine(sb, learnReason);
            appendLine(sb, teacherMatrixReason);
            appendLine(sb, preDrawReason);
            appendLine(sb, elite45Reason);
            appendLine(sb, lastHitMissReason);
            appendLine(sb, nextAdvice);

            if (items != null) {
                for (int i = 0; i < items.size() && i < AppConfig.REASON_SHOW_LIMIT; i++) {
                    ReasonItem item = items.get(i);
                    if (item != null) {
                        appendLine(sb, item.title + "：" + item.detail);
                    }
                }
            }
            return sb.toString();
        }

        private void appendLine(StringBuilder sb, String text) {
            if (text == null || text.length() == 0) return;
            if (sb.length() > 0) sb.append("\n");
            sb.append(text);
        }
    }

    public static final class ShapeVector {
        public long id;
        public String baseIssue;
        public String targetIssue;
        public long createdAt;
        public int position;
        public double entropy16;
        public double entropy32;
        public double entropy64;
        public double entropy128;
        public double entropy240;
        public double entropy480;
        public double entropy960;
        public double top3Mass16;
        public double top3Mass32;
        public double top3Mass64;
        public double top3Mass128;
        public double top3Mass240;
        public double top4Mass128;
        public double top6Mass240;
        public double slopeShort;
        public double slopeMid;
        public double slopeLong;
        public double driftShort;
        public double driftMid;
        public double driftLong;
        public double driftRisk;
        public double markov1Support;
        public double markov2Support;
        public double trendLife;
        public double breakDepth;
        public double recoveryScore;
        public double championGap;
        public double comboGap;
        public double hotReverseRisk;
        public double falseStrongRisk;
        public String stateMode;
        public String shapeKey;
        public String stateKey;
        public double[] numberProbs;
        public double[] markovProbs;
        public double deepScore;
        public double stateScore;
        public double cycleScore;
        public double transitionScore;
        public double biasScore;
        public double profileScore;
        public double runtimeProfileScore;
        public double teacherMatrixScore;
        public double reasonScore;
        public double reasonEdge;
        public String reasonFingerprint;
        public String deepProfileKey;

        public ShapeVector() {
            id = 0L;
            baseIssue = "";
            targetIssue = "";
            createdAt = 0L;
            position = 0;
            entropy16 = 0.0d;
            entropy32 = 0.0d;
            entropy64 = 0.0d;
            entropy128 = 0.0d;
            entropy240 = 0.0d;
            entropy480 = 0.0d;
            entropy960 = 0.0d;
            top3Mass16 = 0.0d;
            top3Mass32 = 0.0d;
            top3Mass64 = 0.0d;
            top3Mass128 = 0.0d;
            top3Mass240 = 0.0d;
            top4Mass128 = 0.0d;
            top6Mass240 = 0.0d;
            slopeShort = 0.0d;
            slopeMid = 0.0d;
            slopeLong = 0.0d;
            driftShort = 0.0d;
            driftMid = 0.0d;
            driftLong = 0.0d;
            driftRisk = 0.0d;
            markov1Support = 0.0d;
            markov2Support = 0.0d;
            trendLife = 0.0d;
            breakDepth = 0.0d;
            recoveryScore = 0.0d;
            championGap = 0.0d;
            comboGap = 0.0d;
            hotReverseRisk = 0.0d;
            falseStrongRisk = 0.0d;
            stateMode = AppConfig.MODE_CHAOS;
            shapeKey = "";
            stateKey = "";
            numberProbs = new double[AppConfig.NUMBER_COUNT + 1];
            markovProbs = new double[AppConfig.NUMBER_COUNT + 1];
            deepScore = 0.0d;
            stateScore = 0.0d;
            cycleScore = 0.0d;
            transitionScore = 0.0d;
            biasScore = 0.0d;
            profileScore = 0.0d;
            runtimeProfileScore = 0.0d;
            teacherMatrixScore = 0.0d;
            reasonScore = 0.0d;
            reasonEdge = 0.0d;
            reasonFingerprint = "";
            deepProfileKey = "";
        }

        public boolean isUsableForBet() {
            return trendLife >= AppConfig.TREND_LIFE_MIN
                    && driftRisk <= AppConfig.DRIFT_RISK_MAX
                    && breakDepth <= AppConfig.BREAK_DEPTH_MAX;
        }

        public double getNumberProbability(int number) {
            if (!AppConfig.isValidNumber(number)
                    || numberProbs == null
                    || number >= numberProbs.length) return 0.0d;
            return numberProbs[number];
        }

        public double getMarkovProbability(int number) {
            if (!AppConfig.isValidNumber(number)
                    || markovProbs == null
                    || number >= markovProbs.length) return 0.0d;
            return markovProbs[number];
        }

        public ShapeVector copyLight() {
            ShapeVector v = new ShapeVector();
            v.id = id;
            v.baseIssue = baseIssue;
            v.targetIssue = targetIssue;
            v.createdAt = createdAt;
            v.position = position;
            v.entropy16 = entropy16;
            v.entropy32 = entropy32;
            v.entropy64 = entropy64;
            v.entropy128 = entropy128;
            v.entropy240 = entropy240;
            v.entropy480 = entropy480;
            v.entropy960 = entropy960;
            v.top3Mass16 = top3Mass16;
            v.top3Mass32 = top3Mass32;
            v.top3Mass64 = top3Mass64;
            v.top3Mass128 = top3Mass128;
            v.top3Mass240 = top3Mass240;
            v.top4Mass128 = top4Mass128;
            v.top6Mass240 = top6Mass240;
            v.slopeShort = slopeShort;
            v.slopeMid = slopeMid;
            v.slopeLong = slopeLong;
            v.driftShort = driftShort;
            v.driftMid = driftMid;
            v.driftLong = driftLong;
            v.driftRisk = driftRisk;
            v.markov1Support = markov1Support;
            v.markov2Support = markov2Support;
            v.trendLife = trendLife;
            v.breakDepth = breakDepth;
            v.recoveryScore = recoveryScore;
            v.championGap = championGap;
            v.comboGap = comboGap;
            v.hotReverseRisk = hotReverseRisk;
            v.falseStrongRisk = falseStrongRisk;
            v.stateMode = stateMode;
            v.shapeKey = shapeKey;
            v.stateKey = stateKey;

            if (numberProbs != null) {
                v.numberProbs = new double[numberProbs.length];
                for (int i = 0; i < numberProbs.length; i++) v.numberProbs[i] = numberProbs[i];
            }
            if (markovProbs != null) {
                v.markovProbs = new double[markovProbs.length];
                for (int i = 0; i < markovProbs.length; i++) v.markovProbs[i] = markovProbs[i];
            }

            v.deepScore = deepScore;
            v.stateScore = stateScore;
            v.cycleScore = cycleScore;
            v.transitionScore = transitionScore;
            v.biasScore = biasScore;
            v.profileScore = profileScore;
            v.runtimeProfileScore = runtimeProfileScore;
            v.teacherMatrixScore = teacherMatrixScore;
            v.reasonScore = reasonScore;
            v.reasonEdge = reasonEdge;
            v.reasonFingerprint = reasonFingerprint;
            v.deepProfileKey = deepProfileKey;
            return v;
        }

        public ShapeVector copy() {
            return copyLight();
        }
    }

    public static final class ShapeMemory {
        public String shapeKey;
        public int position;
        public int sampleCount;
        public int hitCount;
        public double hitRate;
        public int last240Sample;
        public int last240Hit;
        public double last240HitRate;
        public int last1000Sample;
        public int last1000Hit;
        public double last1000HitRate;
        public int failCount;
        public int reverseCount;
        public int activeFlag;
        public long updatedAt;

        public ShapeMemory() {
            shapeKey = "";
            position = 0;
            sampleCount = 0;
            hitCount = 0;
            hitRate = 0.0d;
            last240Sample = 0;
            last240Hit = 0;
            last240HitRate = 0.0d;
            last1000Sample = 0;
            last1000Hit = 0;
            last1000HitRate = 0.0d;
            failCount = 0;
            reverseCount = 0;
            activeFlag = 1;
            updatedAt = 0L;
        }

        public void recalc() {
            hitRate = sampleCount > 0 ? ((double) hitCount) / sampleCount : 0.0d;
            last240HitRate = last240Sample > 0 ? ((double) last240Hit) / last240Sample : 0.0d;
            last1000HitRate = last1000Sample > 0 ? ((double) last1000Hit) / last1000Sample : 0.0d;
        }
    }

    public static final class ComboMemory {
        public String shapeKey;
        public int position;
        public int k3Mask;
        public int sampleCount;
        public int hitCount;
        public double hitRate;
        public int last240Sample;
        public int last240Hit;
        public double last240HitRate;
        public int last1000Sample;
        public int last1000Hit;
        public double last1000HitRate;
        public double avgComboGap;
        public double avgTrendLife;
        public int activeFlag;
        public long updatedAt;

        public ComboMemory() {
            shapeKey = "";
            position = 0;
            k3Mask = 0;
            sampleCount = 0;
            hitCount = 0;
            hitRate = 0.0d;
            last240Sample = 0;
            last240Hit = 0;
            last240HitRate = 0.0d;
            last1000Sample = 0;
            last1000Hit = 0;
            last1000HitRate = 0.0d;
            avgComboGap = 0.0d;
            avgTrendLife = 0.0d;
            activeFlag = 1;
            updatedAt = 0L;
        }

        public void recalc() {
            hitRate = sampleCount > 0 ? ((double) hitCount) / sampleCount : 0.0d;
            last240HitRate = last240Sample > 0 ? ((double) last240Hit) / last240Sample : 0.0d;
            last1000HitRate = last1000Sample > 0 ? ((double) last1000Hit) / last1000Sample : 0.0d;
        }
    }

    public static final class BreakMemory {
        public String breakKey;
        public int position;
        public String stateModeBefore;
        public String breakType;
        public int breakDepthBucket;
        public int trendLifeBucket;
        public int driftBucket;
        public int markovBucket;
        public int sampleCount;
        public int recover1Count;
        public int recover2Count;
        public int recover3Count;
        public int failCount;
        public int reverseCount;
        public double recoverRate;
        public double avgRecoverSteps;
        public double last240RecoverRate;
        public double last1000RecoverRate;
        public long updatedAt;

        public BreakMemory() {
            breakKey = "";
            position = 0;
            stateModeBefore = "";
            breakType = "";
            breakDepthBucket = 0;
            trendLifeBucket = 0;
            driftBucket = 0;
            markovBucket = 0;
            sampleCount = 0;
            recover1Count = 0;
            recover2Count = 0;
            recover3Count = 0;
            failCount = 0;
            reverseCount = 0;
            recoverRate = 0.0d;
            avgRecoverSteps = 0.0d;
            last240RecoverRate = 0.0d;
            last1000RecoverRate = 0.0d;
            updatedAt = 0L;
        }

        public void recalc() {
            int recoverTotal = recover1Count + recover2Count + recover3Count;
            recoverRate = sampleCount > 0 ? ((double) recoverTotal) / sampleCount : 0.0d;
        }
    }

    public static final class CalibrationBucket {
        public String bucketKey;
        public double rawMin;
        public double rawMax;
        public int sampleCount;
        public int hitCount;
        public double realHitRate;
        public int last240Sample;
        public int last240Hit;
        public double last240HitRate;
        public int last1000Sample;
        public int last1000Hit;
        public double last1000HitRate;
        public long updatedAt;

        public CalibrationBucket() {
            bucketKey = "";
            rawMin = 0.0d;
            rawMax = 0.0d;
            sampleCount = 0;
            hitCount = 0;
            realHitRate = 0.0d;
            last240Sample = 0;
            last240Hit = 0;
            last240HitRate = 0.0d;
            last1000Sample = 0;
            last1000Hit = 0;
            last1000HitRate = 0.0d;
            updatedAt = 0L;
        }

        public boolean contains(double rawP) {
            return rawP >= rawMin && rawP < rawMax;
        }

        public void recalc() {
            realHitRate = sampleCount > 0 ? ((double) hitCount) / sampleCount : 0.0d;
            last240HitRate = last240Sample > 0 ? ((double) last240Hit) / last240Sample : 0.0d;
            last1000HitRate = last1000Sample > 0 ? ((double) last1000Hit) / last1000Sample : 0.0d;
        }
    }

    public static final class ModelWeight {
        public double windowWeight;
        public double markovWeight;
        public double shapeWeight;
        public double comboWeight;
        public double recoveryWeight;
        public double similarWeight;
        public double memoryWeight;
        public double positionWeight;
        public double frequencyWeight;
        public double driftPenaltyWeight;
        public double hotReversePenaltyWeight;
        public double falseStrongPenaltyWeight;
        public double breakPenaltyWeight;
        public double deepFeatureWeight;
        public double biasWeight;
        public double profileWeight;
        public double runtimeProfileWeight;
        public double stateNumberProfileWeight;
        public double teacherWeight;
        public double teacherMatrixWeight;
        public double reasonWeight;
        public double elite45Weight;
        public double cycleWeight;
        public double transitionWeight;
        public double aiJudgeWeight;
        public double auditWeight;
        public double recentError;
        public double recentWinRate;
        public double recentFrequency;
        public double recentPnl;
        public long updatedAt;

        public ModelWeight() {
            windowWeight = 0.30d;
            markovWeight = 0.08d;
            shapeWeight = 0.22d;
            comboWeight = 0.26d;
            recoveryWeight = 0.10d;
            similarWeight = 0.06d;
            memoryWeight = 0.045d;
            positionWeight = 0.04d;
            frequencyWeight = 0.0d;
            driftPenaltyWeight = 1.0d;
            hotReversePenaltyWeight = 1.0d;
            falseStrongPenaltyWeight = 1.0d;
            breakPenaltyWeight = 1.0d;
            deepFeatureWeight = 0.04d;
            biasWeight = 0.025d;
            profileWeight = 0.025d;
            runtimeProfileWeight = 0.025d;
            stateNumberProfileWeight = 0.025d;
            teacherWeight = 1.0d;
            teacherMatrixWeight = 0.025d;
            reasonWeight = 0.035d;
            elite45Weight = 0.030d;
            cycleWeight = 0.025d;
            transitionWeight = 0.025d;
            aiJudgeWeight = 0.025d;
            auditWeight = 0.04d;
            recentError = 0.0d;
            recentWinRate = 0.0d;
            recentFrequency = 0.0d;
            recentPnl = 0.0d;
            updatedAt = 0L;
        }

        public void normalizePositive() {
            if (windowWeight < 0.0d) windowWeight = 0.0d;
            if (markovWeight < 0.0d) markovWeight = 0.0d;
            if (shapeWeight < 0.0d) shapeWeight = 0.0d;
            if (comboWeight < 0.0d) comboWeight = 0.0d;
            if (recoveryWeight < 0.0d) recoveryWeight = 0.0d;

            double sum = windowWeight + markovWeight + shapeWeight + comboWeight + recoveryWeight;
            if (sum <= 0.0d) {
                windowWeight = 0.30d;
                markovWeight = 0.08d;
                shapeWeight = 0.22d;
                comboWeight = 0.26d;
                recoveryWeight = 0.10d;
                sum = windowWeight + markovWeight + shapeWeight + comboWeight + recoveryWeight;
            }

            windowWeight = windowWeight / sum;
            markovWeight = markovWeight / sum;
            shapeWeight = shapeWeight / sum;
            comboWeight = comboWeight / sum;
            recoveryWeight = recoveryWeight / sum;
            similarWeight = AppConfig.clampProbability(similarWeight);
            memoryWeight = AppConfig.clampProbability(memoryWeight);
            positionWeight = AppConfig.clampProbability(positionWeight);
            frequencyWeight = 0.0d;
            driftPenaltyWeight = clampTeacherWeight(driftPenaltyWeight);
            hotReversePenaltyWeight = clampTeacherWeight(hotReversePenaltyWeight);
            falseStrongPenaltyWeight = clampTeacherWeight(falseStrongPenaltyWeight);
            breakPenaltyWeight = clampTeacherWeight(breakPenaltyWeight);
            deepFeatureWeight = AppConfig.clampProbability(deepFeatureWeight);
            biasWeight = AppConfig.clampProbability(biasWeight);
            profileWeight = AppConfig.clampProbability(profileWeight);
            runtimeProfileWeight = AppConfig.clampProbability(runtimeProfileWeight);
            stateNumberProfileWeight = AppConfig.clampProbability(stateNumberProfileWeight);
            teacherWeight = clampTeacherWeight(teacherWeight);
            teacherMatrixWeight = AppConfig.clampProbability(teacherMatrixWeight);
            if (teacherMatrixWeight > 0.04d) teacherMatrixWeight = 0.04d;
            reasonWeight = AppConfig.clampProbability(reasonWeight);
            if (reasonWeight > 0.055d) reasonWeight = 0.055d;
            elite45Weight = AppConfig.clampProbability(elite45Weight);
            if (elite45Weight > 0.045d) elite45Weight = 0.045d;
            cycleWeight = AppConfig.clampProbability(cycleWeight);
            transitionWeight = AppConfig.clampProbability(transitionWeight);
            aiJudgeWeight = AppConfig.clampProbability(aiJudgeWeight);
            auditWeight = AppConfig.clampProbability(auditWeight);
        }

        public ModelWeight copy() {
            ModelWeight w = new ModelWeight();
            w.windowWeight = windowWeight;
            w.markovWeight = markovWeight;
            w.shapeWeight = shapeWeight;
            w.comboWeight = comboWeight;
            w.recoveryWeight = recoveryWeight;
            w.similarWeight = similarWeight;
            w.memoryWeight = memoryWeight;
            w.positionWeight = positionWeight;
            w.frequencyWeight = frequencyWeight;
            w.driftPenaltyWeight = driftPenaltyWeight;
            w.hotReversePenaltyWeight = hotReversePenaltyWeight;
            w.falseStrongPenaltyWeight = falseStrongPenaltyWeight;
            w.breakPenaltyWeight = breakPenaltyWeight;
            w.deepFeatureWeight = deepFeatureWeight;
            w.biasWeight = biasWeight;
            w.profileWeight = profileWeight;
            w.runtimeProfileWeight = runtimeProfileWeight;
            w.stateNumberProfileWeight = stateNumberProfileWeight;
            w.teacherWeight = teacherWeight;
            w.teacherMatrixWeight = teacherMatrixWeight;
            w.reasonWeight = reasonWeight;
            w.elite45Weight = elite45Weight;
            w.cycleWeight = cycleWeight;
            w.transitionWeight = transitionWeight;
            w.aiJudgeWeight = aiJudgeWeight;
            w.auditWeight = auditWeight;
            w.recentError = recentError;
            w.recentWinRate = recentWinRate;
            w.recentFrequency = recentFrequency;
            w.recentPnl = recentPnl;
            w.updatedAt = updatedAt;
            return w;
        }
    }

    public static final class BacktestReport {
        public long id;
        public String type;
        public String mode;
        public String rangeStart;
        public String rangeEnd;
        public String startIssue;
        public String endIssue;
        public int ok;
        public int window;
        public int trainCount;
        public int testCount;
        public int totalCount;
        public int betCount;
        public int hitCount;
        public int missCount;
        public int holdCount;
        public int shadowCount;
        public int candidateCount;
        public int enrichCount;
        public int deepCount;
        public int learningCaseCount;
        public int teacherMatrixSampleCount;
        public int teacherMatrixProfileCount;
        public int teacherMatrixPositiveCount;
        public int teacherMatrixNegativeCount;
        public int reasonSampleCount;
        public int reasonProfileCount;
        public int elite45PassCount;
        public double hitRate;
        public double winRate;
        public double frequency;
        public double pnl;
        public double maxDrawdown;
        public int longestLose;
        public double avgRawP;
        public double avgFinalP;
        public double avgGate;
        public double avgEdge;
        public double calibrationError;
        public long startedAt;
        public long finishedAt;
        public long costMs;
        public String status;
        public String summary;
        public long createdAt;
        public int manualTriggered;
        public int profileCount;
        public int attributionCount;
        public int promotedCount;
        public int rejectedCount;
        public int aPlusCount;
        public int progress;
        public double aPlusHitRate;
        public double biasScoreAvg;
        public double brierScore;
        public double aiJudgeAvgScore;
        public double deepScoreAvg;
        public double reasonAvgEdge;
        public double reasonAvgHitSimilarity;
        public double reasonAvgMissSimilarity;
        public String biasSummary;
        public String auditSummary;
        public String promotionSummary;
        public String aPlusSummary;
        public String learningImpactSummary;
        public String teacherMatrixSummary;
        public String reasonSummary;
        public String elite45Summary;
        public String topProfiles;
        public String topMissReasons;
        public String topHitReasons;

        public BacktestReport() {
            id = 0L;
            type = "";
            mode = AppConfig.BACKTEST_MODE_FAST;
            rangeStart = "";
            rangeEnd = "";
            startIssue = "";
            endIssue = "";
            ok = 0;
            window = 0;
            trainCount = 0;
            testCount = 0;
            totalCount = 0;
            betCount = 0;
            hitCount = 0;
            missCount = 0;
            holdCount = 0;
            shadowCount = 0;
            candidateCount = 0;
            enrichCount = 0;
            deepCount = 0;
            learningCaseCount = 0;
            teacherMatrixSampleCount = 0;
            teacherMatrixProfileCount = 0;
            teacherMatrixPositiveCount = 0;
            teacherMatrixNegativeCount = 0;
            reasonSampleCount = 0;
            reasonProfileCount = 0;
            elite45PassCount = 0;
            hitRate = 0.0d;
            winRate = 0.0d;
            frequency = 0.0d;
            pnl = 0.0d;
            maxDrawdown = 0.0d;
            longestLose = 0;
            avgRawP = 0.0d;
            avgFinalP = 0.0d;
            avgGate = 0.0d;
            avgEdge = 0.0d;
            calibrationError = 0.0d;
            startedAt = 0L;
            finishedAt = 0L;
            costMs = 0L;
            status = "";
            summary = "";
            createdAt = 0L;
            manualTriggered = 0;
            profileCount = 0;
            attributionCount = 0;
            promotedCount = 0;
            rejectedCount = 0;
            aPlusCount = 0;
            progress = 0;
            aPlusHitRate = 0.0d;
            biasScoreAvg = 0.0d;
            brierScore = 0.0d;
            aiJudgeAvgScore = 0.0d;
            deepScoreAvg = 0.0d;
            reasonAvgEdge = 0.0d;
            reasonAvgHitSimilarity = 0.0d;
            reasonAvgMissSimilarity = 0.0d;
            biasSummary = "";
            auditSummary = "";
            promotionSummary = "";
            aPlusSummary = "";
            learningImpactSummary = "";
            teacherMatrixSummary = "";
            reasonSummary = "";
            elite45Summary = "";
            topProfiles = "";
            topMissReasons = "";
            topHitReasons = "";
        }

        public void recalc() {
            hitRate = betCount > 0 ? ((double) hitCount) / betCount : 0.0d;
            winRate = hitRate;
            missCount = betCount - hitCount;
            if (missCount < 0) missCount = 0;
            if (totalCount <= 0) totalCount = testCount;
            frequency = totalCount > 0 ? ((double) betCount) / totalCount : 0.0d;
            if (finishedAt > 0L && startedAt > 0L) {
                costMs = finishedAt - startedAt;
                if (costMs < 0L) costMs = 0L;
            }
        }

        public boolean hasRealBet() {
            return betCount > 0;
        }

        public boolean isTargetFrequencyOk() {
            recalc();
            return frequency >= AppConfig.TARGET_FREQ_MIN
                    && frequency <= AppConfig.TARGET_FREQ_HARD_MAX;
        }

        public boolean isHitRateOk() {
            recalc();
            return hitRate >= AppConfig.TARGET_HIT_RATE_MIN;
        }

        public boolean isIdealHitRate() {
            recalc();
            return hitRate >= AppConfig.TARGET_HIT_RATE_IDEAL;
        }

        public boolean isPositivePnl() {
            return pnl > 0.0d;
        }

        public boolean isPassTarget() {
            recalc();
            return ok == 1 && betCount > 0 && isTargetFrequencyOk() && isHitRateOk() && pnl >= 0.0d;
        }

        public String healthText() {
            recalc();
            if (ok == 0) return "回测未通过或数据不足";
            if (teacherMatrixSampleCount <= 0) return "教师矩阵样本未生成，学习闭环未跑通";
            if (reasonSampleCount <= 0) return "开奖前原因样本未生成，原因反推闭环未跑通";
            if (betCount <= 0) return "近期无真实BET，策略偏保守";
            if (isIdealHitRate() && isTargetFrequencyOk() && pnl > 0.0d) return "胜率、频率、盈亏达标";
            if (isHitRateOk() && pnl >= 0.0d) return "胜率达最低线，继续观察频率和回撤";
            if (frequency < AppConfig.TARGET_FREQ_MIN) return "频率偏低，候选或门槛偏保守";
            return "回测偏弱，需要收紧风险或优化教师矩阵/原因矩阵画像";
        }
    }

    public static final class RealtimeValidationReport {
        public long id;
        public String reportId;
        public String baseIssue;
        public String targetIssue;
        public long startedAt;
        public long finishedAt;
        public long createdAt;
        public long costMs;
        public int window;
        public int sampleCount;
        public int totalCount;
        public int betCount;
        public int hitCount;
        public int missCount;
        public int holdCount;
        public int shadowCount;
        public double hitRate;
        public double winRate;
        public double frequency;
        public double pnl;
        public double maxDrawdown;
        public int longestLose;
        public int recentLoseStreak;
        public double avgFinalP;
        public double avgGate;
        public double avgEdge;
        public double avgDriftRisk;
        public double avgBreakDepth;
        public double avgTrendLife;
        public double avgRecoveryScore;
        public int sampleWeak;
        public int freqHigh;
        public int freqHard;
        public int hitWeak;
        public int recentMissWarn;
        public int drawdownWarn;
        public int stableGood;
        public double gateAdjust;
        public double recommendGateAdjust;
        public int ok;
        public int usable;
        public String status;
        public String summary;
        public String detail;
        public String warningText;
        public String source;
        public int teacherMatrixSampleCount;
        public int teacherMatrixUsedCount;
        public double teacherMatrixAvgConfidence;
        public String teacherMatrixSummary;
        public int reasonSampleCount;
        public int elite45PassCount;
        public double reasonAvgEdge;
        public String reasonSummary;

        public RealtimeValidationReport() {
            id = 0L;
            reportId = "";
            baseIssue = "";
            targetIssue = "";
            startedAt = 0L;
            finishedAt = 0L;
            createdAt = 0L;
            costMs = 0L;
            window = AppConfig.AUTO_VALIDATE_DEFAULT_WINDOW;
            sampleCount = 0;
            totalCount = 0;
            betCount = 0;
            hitCount = 0;
            missCount = 0;
            holdCount = 0;
            shadowCount = 0;
            hitRate = 0.0d;
            winRate = 0.0d;
            frequency = 0.0d;
            pnl = 0.0d;
            maxDrawdown = 0.0d;
            longestLose = 0;
            recentLoseStreak = 0;
            avgFinalP = 0.0d;
            avgGate = 0.0d;
            avgEdge = 0.0d;
            avgDriftRisk = 0.0d;
            avgBreakDepth = 0.0d;
            avgTrendLife = 0.0d;
            avgRecoveryScore = 0.0d;
            sampleWeak = 1;
            freqHigh = 0;
            freqHard = 0;
            hitWeak = 0;
            recentMissWarn = 0;
            drawdownWarn = 0;
            stableGood = 0;
            gateAdjust = 0.0d;
            recommendGateAdjust = 0.0d;
            ok = 0;
            usable = 0;
            status = AppConfig.BACKTEST_STATUS_IDLE;
            summary = "";
            detail = "";
            warningText = "";
            source = "REALTIME_VALIDATE";
            teacherMatrixSampleCount = 0;
            teacherMatrixUsedCount = 0;
            teacherMatrixAvgConfidence = 0.0d;
            teacherMatrixSummary = "";
            reasonSampleCount = 0;
            elite45PassCount = 0;
            reasonAvgEdge = 0.0d;
            reasonSummary = "";
        }

        public void markStart(String base, String target, int windowValue) {
            baseIssue = base == null ? "" : base;
            targetIssue = target == null ? "" : target;
            window = AppConfig.normalizeAutoValidateWindow(windowValue);
            startedAt = System.currentTimeMillis();
            createdAt = startedAt;
            finishedAt = 0L;
            costMs = 0L;
            status = AppConfig.BACKTEST_STATUS_RUNNING;
            ok = 0;
            usable = 0;
            if (reportId == null || reportId.length() == 0) {
                reportId = "RV_" + startedAt + "_" + targetIssue;
            }
        }

        public void markFinish() {
            finishedAt = System.currentTimeMillis();
            if (startedAt > 0L) {
                costMs = finishedAt - startedAt;
                if (costMs < 0L) costMs = 0L;
            }
            status = AppConfig.BACKTEST_STATUS_OK;
            recalc();
        }

        public void markError(String text) {
            finishedAt = System.currentTimeMillis();
            if (startedAt > 0L) {
                costMs = finishedAt - startedAt;
                if (costMs < 0L) costMs = 0L;
            }
            status = AppConfig.BACKTEST_STATUS_ERROR;
            ok = 0;
            usable = 0;
            warningText = text == null ? "" : text;
            summary = "实时轻量验证异常：" + warningText;
        }

        public void recalc() {
            if (sampleCount <= 0) sampleCount = totalCount;
            if (totalCount <= 0) totalCount = sampleCount;
            if (missCount <= 0 && betCount >= hitCount) missCount = betCount - hitCount;
            if (missCount < 0) missCount = 0;
            hitRate = betCount > 0 ? ((double) hitCount) / ((double) betCount) : 0.0d;
            winRate = hitRate;
            frequency = totalCount > 0 ? ((double) betCount) / ((double) totalCount) : 0.0d;
            sampleWeak = sampleCount < AppConfig.AUTO_VALIDATE_SAMPLE_MIN ? 1 : 0;
            freqHigh = frequency > AppConfig.AUTO_VALIDATE_FREQ_WARN ? 1 : 0;
            freqHard = frequency > AppConfig.AUTO_VALIDATE_FREQ_HARD ? 1 : 0;
            hitWeak = betCount > 0 && hitRate < AppConfig.AUTO_VALIDATE_HIT_WARN ? 1 : 0;
            recentMissWarn = recentLoseStreak >= 3 || longestLose >= 4 ? 1 : 0;
            drawdownWarn = maxDrawdown >= Math.abs(AppConfig.LOSE_NET_PROFIT) * 8.0d ? 1 : 0;
            stableGood = 0;
            if (sampleWeak == 0
                    && betCount > 0
                    && hitRate >= AppConfig.AUTO_VALIDATE_HIT_GOOD
                    && frequency <= AppConfig.TARGET_FREQ_MAX
                    && maxDrawdown < Math.abs(AppConfig.LOSE_NET_PROFIT) * 8.0d) {
                stableGood = 1;
            }
            recommendGateAdjust = buildGateAdjust();
            gateAdjust = recommendGateAdjust;
            ok = AppConfig.BACKTEST_STATUS_OK.equals(status) ? 1 : 0;
            usable = isUsableForRealtimeGate() ? 1 : 0;
            summary = buildSummaryText();
            detail = buildDetailText();
        }

        private double buildGateAdjust() {
            double adjust = 0.0d;
            if (sampleWeak == 1) {
                if (AppConfig.AUTO_VALIDATE_FAIL_OPEN) return 0.0d;
                adjust += AppConfig.AUTO_VALIDATE_GATE_BOOST_FREQ_HIGH;
            }
            if (freqHard == 1) adjust += AppConfig.AUTO_VALIDATE_GATE_BOOST_FREQ_HIGH * 1.5d;
            else if (freqHigh == 1) adjust += AppConfig.AUTO_VALIDATE_GATE_BOOST_FREQ_HIGH;
            if (hitWeak == 1) adjust += AppConfig.AUTO_VALIDATE_GATE_BOOST_RECENT_MISS;
            if (recentMissWarn == 1) adjust += AppConfig.AUTO_VALIDATE_GATE_BOOST_RECENT_MISS;
            if (drawdownWarn == 1) adjust += AppConfig.AUTO_VALIDATE_GATE_BOOST_DRAWDOWN;
            if (stableGood == 1) adjust -= AppConfig.AUTO_VALIDATE_GATE_REDUCE_STABLE;
            return clampAbs(adjust, AppConfig.AUTO_VALIDATE_GATE_MAX_ADJUST);
        }

        public boolean isWindowLegal() {
            return window >= AppConfig.AUTO_VALIDATE_MIN_WINDOW
                    && window <= AppConfig.AUTO_VALIDATE_MAX_WINDOW;
        }

        public boolean isUsableForRealtimeGate() {
            if (!AppConfig.AUTO_VALIDATE_ENABLE) return false;
            if (!AppConfig.BACKTEST_STATUS_OK.equals(status)) return false;
            if (!isWindowLegal()) return false;
            if (sampleWeak == 1 && !AppConfig.AUTO_VALIDATE_FAIL_OPEN) return false;
            return true;
        }

        public String buildSummaryText() {
            StringBuilder sb = new StringBuilder();
            sb.append("轻量验证W").append(window).append("：");
            if (sampleWeak == 1) sb.append("样本偏少");
            else if (stableGood == 1) sb.append("近期稳定");
            else if (freqHard == 1) sb.append("频率过热");
            else if (freqHigh == 1) sb.append("频率偏高");
            else if (hitWeak == 1) sb.append("近期命中偏弱");
            else sb.append("状态正常");

            sb.append("，BET=").append(betCount).append("/").append(totalCount);
            sb.append("，胜率=").append(UiFormatter.probability(hitRate));
            sb.append("，频率=").append(UiFormatter.probability(frequency));
            sb.append("，门槛修正=").append(UiFormatter.signedProbability(gateAdjust));
            if (teacherMatrixSampleCount > 0) {
                sb.append("，教师矩阵样本=").append(teacherMatrixSampleCount);
                sb.append("，均置信=").append(UiFormatter.probability(teacherMatrixAvgConfidence));
            }
            if (reasonSampleCount > 0) {
                sb.append("，原因样本=").append(reasonSampleCount);
                sb.append("，原因边际=").append(UiFormatter.signedProbability(reasonAvgEdge));
            }
            return sb.toString();
        }

        public String buildDetailText() {
            StringBuilder sb = new StringBuilder();
            sb.append("pnl=").append(UiFormatter.money(pnl));
            sb.append("，maxDD=").append(UiFormatter.money(maxDrawdown));
            sb.append("，最长连错=").append(longestLose);
            sb.append("，最近连错=").append(recentLoseStreak);
            if (teacherMatrixSummary != null && teacherMatrixSummary.length() > 0) {
                sb.append("，教师矩阵=").append(teacherMatrixSummary);
            }
            if (reasonSummary != null && reasonSummary.length() > 0) {
                sb.append("，原因矩阵=").append(reasonSummary);
            }
            if (warningText != null && warningText.length() > 0) {
                sb.append("，警告=").append(warningText);
            }
            return sb.toString();
        }
    }

    public static final class TeacherMatrixSample {
        public long id;
        public String sampleKey;
        public String sourceType;
        public String baseIssue;
        public String targetIssue;
        public long createdAt;
        public int targetIndex;
        public int position;
        public int number;
        public int actualNumber;
        public int label;
        public int hit;
        public int k3Mask;
        public String stateKey;
        public String stateMode;
        public String trendKey;
        public String shapeKey;
        public String driftKey;
        public String breakKey;
        public String recoveryKey;
        public String transitionKey;
        public String hotColdKey;
        public String oddEvenKey;
        public String highLowKey;
        public String relationKey;
        public String profileKey;
        public String comboKey;
        public double trendScore;
        public double shapeScore;
        public double driftRisk;
        public double breakRisk;
        public double recoveryScore;
        public double numberScore;
        public double positionScore;
        public double riskScore;
        public double confidence;
        public double finalP;
        public double dynamicGate;
        public double edge;
        public String whyPositive;
        public String whyNegative;
        public String whyMiss;
        public String nextAdvice;
        public String mainReasonCode;
        public String affectedModule;
        public int saved;

        public String reasonFingerprint;
        public double reasonEdge;
        public double hitSimilarity;
        public double missSimilarity;
        public String reasonLevel;

        public TeacherMatrixSample() {
            id = 0L;
            sampleKey = "";
            sourceType = AppConfig.STAT_SCOPE_AUTO_LEARN;
            baseIssue = "";
            targetIssue = "";
            createdAt = 0L;
            targetIndex = -1;
            position = 0;
            number = 0;
            actualNumber = 0;
            label = 0;
            hit = 0;
            k3Mask = 0;
            stateKey = "";
            stateMode = AppConfig.MODE_UNKNOWN;
            trendKey = "";
            shapeKey = "";
            driftKey = "";
            breakKey = "";
            recoveryKey = "";
            transitionKey = "";
            hotColdKey = "";
            oddEvenKey = "";
            highLowKey = "";
            relationKey = "";
            profileKey = "";
            comboKey = "";
            trendScore = 0.0d;
            shapeScore = 0.0d;
            driftRisk = 0.0d;
            breakRisk = 0.0d;
            recoveryScore = 0.0d;
            numberScore = 0.0d;
            positionScore = 0.0d;
            riskScore = 0.0d;
            confidence = 0.0d;
            finalP = 0.0d;
            dynamicGate = AppConfig.DEFAULT_DYNAMIC_GATE;
            edge = 0.0d;
            whyPositive = "";
            whyNegative = "";
            whyMiss = "";
            nextAdvice = "";
            mainReasonCode = AppConfig.MISS_UNKNOWN;
            affectedModule = AppConfig.ATTR_MODULE_TEACHER;
            saved = 0;
            reasonFingerprint = "";
            reasonEdge = 0.0d;
            hitSimilarity = 0.0d;
            missSimilarity = 0.0d;
            reasonLevel = REASON_LEVEL_C_EXPLAIN_ONLY;
        }

        public void rebuildKeys() {
            if (stateKey == null || stateKey.length() == 0) {
                stateKey =
                        AppConfig.buildStateKeyPart(stateMode)
                                + "|P"
                                + position
                                + "|"
                                + AppConfig.buildStateKeyPart(trendKey)
                                + "|"
                                + AppConfig.buildStateKeyPart(shapeKey)
                                + "|"
                                + AppConfig.buildStateKeyPart(driftKey)
                                + "|"
                                + AppConfig.buildStateKeyPart(breakKey)
                                + "|"
                                + AppConfig.buildStateKeyPart(recoveryKey);
            }
            profileKey = buildTeacherProfileKey(stateKey, position, number);
            comboKey = buildTeacherComboKey(stateKey, position, k3Mask);
            sampleKey = AppConfig.buildTeacherMatrixSampleKey(targetIssue, position, number);
            if (reasonFingerprint == null || reasonFingerprint.length() == 0) {
                reasonFingerprint =
                        buildReasonFingerprint(
                                stateKey,
                                trendKey,
                                shapeKey,
                                driftKey,
                                recoveryKey,
                                relationKey,
                                mainReasonCode);
            }
        }

        public boolean isPositive() {
            return label == 1 || hit == 1;
        }

        public boolean isUsable() {
            return targetIssue != null
                    && targetIssue.length() > 0
                    && AppConfig.isValidPosition(position)
                    && AppConfig.isValidNumber(number);
        }

        public String buildSummaryText() {
            StringBuilder sb = new StringBuilder();
            sb.append("教师样本 ").append(targetIssue);
            sb.append(" P").append(position);
            sb.append(" N").append(UiFormatter.number(number));
            sb.append(isPositive() ? " 正样本" : " 负样本");
            sb.append("，实开=").append(UiFormatter.number(actualNumber));
            if (isPositive()) sb.append("，原因=").append(whyPositive == null ? "" : whyPositive);
            else sb.append("，未出原因=").append(whyNegative == null ? "" : whyNegative);
            return sb.toString();
        }
    }

    public static final class TeacherMatrixProfile {
        public long id;
        public String profileKey;
        public String stateKey;
        public int position;
        public int number;
        public int sampleCount;
        public int positiveCount;
        public int negativeCount;
        public int recentSample;
        public int recentPositive;
        public int recentNegative;
        public double hitRate;
        public double recentHitRate;
        public double confidence;
        public double positionScore;
        public double numberScore;
        public double stateScore;
        public double trendScore;
        public double shapeScore;
        public double riskScore;
        public double probAdjust;
        public double gateAdjust;
        public double riskPenalty;
        public double teacherWeight;
        public long updatedAt;
        public long lastHitAt;
        public long lastMissAt;
        public String status;
        public String whyText;
        public String summaryText;

        public TeacherMatrixProfile() {
            id = 0L;
            profileKey = "";
            stateKey = "";
            position = 0;
            number = 0;
            sampleCount = 0;
            positiveCount = 0;
            negativeCount = 0;
            recentSample = 0;
            recentPositive = 0;
            recentNegative = 0;
            hitRate = 0.0d;
            recentHitRate = 0.0d;
            confidence = 0.0d;
            positionScore = 0.0d;
            numberScore = 0.0d;
            stateScore = 0.0d;
            trendScore = 0.0d;
            shapeScore = 0.0d;
            riskScore = 0.0d;
            probAdjust = 0.0d;
            gateAdjust = 0.0d;
            riskPenalty = 0.0d;
            teacherWeight = 1.0d;
            updatedAt = 0L;
            lastHitAt = 0L;
            lastMissAt = 0L;
            status = AppConfig.TEACHER_MATRIX_STATUS_NONE;
            whyText = "";
            summaryText = "";
        }

        public void rebuildKey() {
            profileKey = buildTeacherProfileKey(stateKey, position, number);
        }

        public void recalc() {
            if (sampleCount <= 0) sampleCount = positiveCount + negativeCount;
            if (recentSample <= 0) recentSample = recentPositive + recentNegative;

            hitRate = sampleCount > 0 ? ((double) positiveCount) / ((double) sampleCount) : 0.0d;
            recentHitRate =
                    recentSample > 0 ? ((double) recentPositive) / ((double) recentSample) : 0.0d;

            double baseRate = teacherNumberBaseline();
            double sampleScore =
                    sampleCount >= AppConfig.TEACHER_MATRIX_STRONG_SAMPLE
                            ? 1.0d
                            : ((double) sampleCount)
                                    / ((double) AppConfig.TEACHER_MATRIX_STRONG_SAMPLE);
            if (sampleScore < 0.0d) sampleScore = 0.0d;
            if (sampleScore > 1.0d) sampleScore = 1.0d;

            double recentScore = recentSample > 0 ? recentHitRate : hitRate;
            double rateEdge = ((hitRate * 0.62d) + (recentScore * 0.38d)) - baseRate;

            confidence =
                    AppConfig.clampProbability(
                            sampleScore * 0.58d
                                    + (1.0d - riskScore) * 0.22d
                                    + Math.abs(rateEdge) * 1.80d);
            numberScore =
                    AppConfig.clampProbability(
                            hitRate * 0.38d
                                    + recentScore * 0.36d
                                    + sampleScore * 0.16d
                                    + trendScore * 0.05d
                                    + shapeScore * 0.05d);
            stateScore =
                    AppConfig.clampProbability(
                            sampleScore * 0.40d + numberScore * 0.40d + (1.0d - riskScore) * 0.20d);
            positionScore = AppConfig.clampProbability(numberScore * 0.70d + stateScore * 0.30d);

            probAdjust =
                    AppConfig.limitAdjust(
                            rateEdge * teacherMatrixProbScale(),
                            AppConfig.TEACHER_MATRIX_MAX_PROB_ADJUST);
            if (confidence < AppConfig.TEACHER_MATRIX_MIN_CONFIDENCE) probAdjust *= 0.35d;

            gateAdjust = 0.0d;
            if (sampleCount >= AppConfig.TEACHER_MATRIX_MIN_SAMPLE) {
                if (hitRate < AppConfig.TEACHER_MATRIX_NUMBER_HIT_RATE_WEAK)
                    gateAdjust += AppConfig.TEACHER_MATRIX_MAX_GATE_ADJUST * 0.65d;
                if (recentSample >= teacherMatrixRecentMinSample()
                        && recentHitRate < teacherMatrixWeakRecentHitRate())
                    gateAdjust += AppConfig.TEACHER_MATRIX_MAX_GATE_ADJUST * 0.45d;
                if (hitRate >= AppConfig.TEACHER_MATRIX_NUMBER_HIT_RATE_STRONG
                        && recentScore >= teacherMatrixStrongRecentHitRate())
                    gateAdjust -= AppConfig.TEACHER_MATRIX_MAX_GATE_ADJUST * 0.55d;
            }
            gateAdjust =
                    AppConfig.limitAdjust(gateAdjust, AppConfig.TEACHER_MATRIX_MAX_GATE_ADJUST);

            riskPenalty = 0.0d;
            if (riskScore >= AppConfig.TEACHER_MATRIX_RISK_HARD)
                riskPenalty += teacherMatrixMaxRiskPenalty();
            else if (riskScore >= AppConfig.TEACHER_MATRIX_RISK_WARN)
                riskPenalty += teacherMatrixMaxRiskPenalty() * 0.55d;
            if (sampleCount >= AppConfig.TEACHER_MATRIX_MIN_SAMPLE
                    && hitRate < AppConfig.TEACHER_MATRIX_NUMBER_HIT_RATE_WEAK)
                riskPenalty += teacherMatrixMaxRiskPenalty() * 0.40d;
            riskPenalty = AppConfig.limitAdjust(riskPenalty, teacherMatrixMaxRiskPenalty());

            teacherWeight = clampTeacherWeight(1.0d + probAdjust - riskPenalty);

            if (sampleCount < AppConfig.TEACHER_MATRIX_MIN_SAMPLE)
                status = AppConfig.TEACHER_MATRIX_STATUS_WEAK;
            else if (riskScore >= AppConfig.TEACHER_MATRIX_RISK_HARD
                    || hitRate < teacherMatrixBlockHitRateForNumber())
                status = AppConfig.TEACHER_MATRIX_STATUS_RISK;
            else if (hitRate >= AppConfig.TEACHER_MATRIX_NUMBER_HIT_RATE_STRONG
                    && confidence >= AppConfig.TEACHER_MATRIX_STRONG_CONFIDENCE)
                status = AppConfig.TEACHER_MATRIX_STATUS_STRONG;
            else status = AppConfig.TEACHER_MATRIX_STATUS_READY;

            rebuildKey();
            summaryText = buildSummaryText();
        }

        public boolean canApplyRealtime() {
            recalc();
            return sampleCount >= AppConfig.TEACHER_MATRIX_MIN_SAMPLE
                    && confidence >= AppConfig.TEACHER_MATRIX_MIN_CONFIDENCE
                    && !AppConfig.TEACHER_MATRIX_STATUS_RISK.equals(status);
        }

        public String buildSummaryText() {
            StringBuilder sb = new StringBuilder();
            sb.append("教师矩阵P").append(position);
            sb.append(" N").append(UiFormatter.number(number));
            sb.append("：样本=").append(sampleCount);
            sb.append("，命中率=").append(UiFormatter.probability(hitRate));
            sb.append("，近期=").append(UiFormatter.probability(recentHitRate));
            sb.append("，置信=").append(UiFormatter.probability(confidence));
            sb.append("，概率修正=").append(UiFormatter.signedProbability(probAdjust));
            sb.append("，门槛修正=").append(UiFormatter.signedProbability(gateAdjust));
            sb.append("，状态=").append(status);
            return sb.toString();
        }
    }

    public static final class TeacherMatrixComboProfile {
        public long id;
        public String comboKey;
        public String stateKey;
        public int position;
        public int k3Mask;
        public int sampleCount;
        public int hitCount;
        public int missCount;
        public int recentSample;
        public int recentHit;
        public double hitRate;
        public double recentHitRate;
        public double confidence;
        public double comboScore;
        public double riskScore;
        public double probAdjust;
        public double gateAdjust;
        public double riskPenalty;
        public long updatedAt;
        public String status;
        public String summaryText;

        public TeacherMatrixComboProfile() {
            id = 0L;
            comboKey = "";
            stateKey = "";
            position = 0;
            k3Mask = 0;
            sampleCount = 0;
            hitCount = 0;
            missCount = 0;
            recentSample = 0;
            recentHit = 0;
            hitRate = 0.0d;
            recentHitRate = 0.0d;
            confidence = 0.0d;
            comboScore = 0.0d;
            riskScore = 0.0d;
            probAdjust = 0.0d;
            gateAdjust = 0.0d;
            riskPenalty = 0.0d;
            updatedAt = 0L;
            status = AppConfig.TEACHER_MATRIX_STATUS_NONE;
            summaryText = "";
        }

        public void rebuildKey() {
            comboKey = buildTeacherComboKey(stateKey, position, k3Mask);
        }

        public void recalc() {
            if (sampleCount <= 0) sampleCount = hitCount + missCount;
            hitRate = sampleCount > 0 ? ((double) hitCount) / ((double) sampleCount) : 0.0d;
            recentHitRate =
                    recentSample > 0 ? ((double) recentHit) / ((double) recentSample) : hitRate;

            double sampleScore =
                    sampleCount >= AppConfig.TEACHER_MATRIX_COMBO_STRONG_SAMPLE
                            ? 1.0d
                            : ((double) sampleCount)
                                    / ((double) AppConfig.TEACHER_MATRIX_COMBO_STRONG_SAMPLE);
            if (sampleScore < 0.0d) sampleScore = 0.0d;
            if (sampleScore > 1.0d) sampleScore = 1.0d;

            comboScore =
                    AppConfig.clampProbability(
                            hitRate * 0.45d + recentHitRate * 0.35d + sampleScore * 0.20d);
            confidence =
                    AppConfig.clampProbability(
                            sampleScore * 0.65d
                                    + (1.0d - riskScore) * 0.20d
                                    + Math.abs(hitRate - teacherComboBaseline()) * 0.40d);

            double edge = hitRate - teacherComboBaseline();
            probAdjust =
                    AppConfig.limitAdjust(
                            edge * teacherMatrixComboProbScale(),
                            AppConfig.TEACHER_MATRIX_MAX_PROB_ADJUST);

            gateAdjust = 0.0d;
            if (sampleCount >= AppConfig.TEACHER_MATRIX_COMBO_MIN_SAMPLE
                    && hitRate < AppConfig.TEACHER_MATRIX_WEAK_HIT_RATE)
                gateAdjust += AppConfig.TEACHER_MATRIX_MAX_GATE_ADJUST * 0.50d;
            if (sampleCount >= AppConfig.TEACHER_MATRIX_COMBO_MIN_SAMPLE
                    && hitRate >= AppConfig.TEACHER_MATRIX_STRONG_HIT_RATE)
                gateAdjust -= AppConfig.TEACHER_MATRIX_MAX_GATE_ADJUST * 0.45d;
            gateAdjust =
                    AppConfig.limitAdjust(gateAdjust, AppConfig.TEACHER_MATRIX_MAX_GATE_ADJUST);

            riskPenalty = 0.0d;
            if (riskScore >= AppConfig.TEACHER_MATRIX_RISK_HARD)
                riskPenalty += teacherMatrixMaxRiskPenalty();
            else if (riskScore >= AppConfig.TEACHER_MATRIX_RISK_WARN)
                riskPenalty += teacherMatrixMaxRiskPenalty() * 0.55d;
            if (sampleCount >= AppConfig.TEACHER_MATRIX_COMBO_MIN_SAMPLE
                    && hitRate < teacherMatrixBlockHitRateForCombo())
                riskPenalty += teacherMatrixMaxRiskPenalty() * 0.40d;
            riskPenalty = AppConfig.limitAdjust(riskPenalty, teacherMatrixMaxRiskPenalty());

            if (sampleCount < AppConfig.TEACHER_MATRIX_COMBO_MIN_SAMPLE)
                status = AppConfig.TEACHER_MATRIX_STATUS_WEAK;
            else if (riskScore >= AppConfig.TEACHER_MATRIX_RISK_HARD
                    || hitRate < teacherMatrixBlockHitRateForCombo())
                status = AppConfig.TEACHER_MATRIX_STATUS_RISK;
            else if (hitRate >= AppConfig.TEACHER_MATRIX_STRONG_HIT_RATE
                    && confidence >= AppConfig.TEACHER_MATRIX_STRONG_CONFIDENCE)
                status = AppConfig.TEACHER_MATRIX_STATUS_STRONG;
            else status = AppConfig.TEACHER_MATRIX_STATUS_READY;

            rebuildKey();
            summaryText = buildSummaryText();
        }

        public boolean canApplyRealtime() {
            recalc();
            return sampleCount >= AppConfig.TEACHER_MATRIX_COMBO_MIN_SAMPLE
                    && confidence >= AppConfig.TEACHER_MATRIX_MIN_CONFIDENCE
                    && !AppConfig.TEACHER_MATRIX_STATUS_RISK.equals(status);
        }

        public String buildSummaryText() {
            StringBuilder sb = new StringBuilder();
            sb.append("教师组合P").append(position);
            sb.append(" ").append(maskToText(k3Mask));
            sb.append("：样本=").append(sampleCount);
            sb.append("，命中率=").append(UiFormatter.probability(hitRate));
            sb.append("，近期=").append(UiFormatter.probability(recentHitRate));
            sb.append("，置信=").append(UiFormatter.probability(confidence));
            sb.append("，状态=").append(status);
            return sb.toString();
        }
    }

    public static final class TeacherMatrixReport {
        public long id;
        public String reportId;
        public String taskKey;
        public String segmentKey;
        public String sourceType;
        public long createdAt;
        public long startedAt;
        public long finishedAt;
        public long costMs;
        public int windowSize;
        public int targetCount;
        public int sampleCount;
        public int positiveCount;
        public int negativeCount;
        public int profileCount;
        public int comboProfileCount;
        public int strongProfileCount;
        public int riskProfileCount;
        public int appliedCount;
        public int reasonSampleCount;
        public int reasonProfileCount;
        public double avgHitRate;
        public double avgConfidence;
        public double avgProbAdjust;
        public double avgGateAdjust;
        public double avgReasonEdge;
        public String status;
        public String summaryText;
        public String errorText;

        public TeacherMatrixReport() {
            id = 0L;
            reportId = "";
            taskKey = "";
            segmentKey = "";
            sourceType = AppConfig.STAT_SCOPE_AUTO_LEARN;
            createdAt = 0L;
            startedAt = 0L;
            finishedAt = 0L;
            costMs = 0L;
            windowSize = 0;
            targetCount = 0;
            sampleCount = 0;
            positiveCount = 0;
            negativeCount = 0;
            profileCount = 0;
            comboProfileCount = 0;
            strongProfileCount = 0;
            riskProfileCount = 0;
            appliedCount = 0;
            reasonSampleCount = 0;
            reasonProfileCount = 0;
            avgHitRate = 0.0d;
            avgConfidence = 0.0d;
            avgProbAdjust = 0.0d;
            avgGateAdjust = 0.0d;
            avgReasonEdge = 0.0d;
            status = AppConfig.TEACHER_MATRIX_STATUS_NONE;
            summaryText = "";
            errorText = "";
        }

        public void markStart() {
            startedAt = System.currentTimeMillis();
            createdAt = startedAt;
            status = AppConfig.TEACHER_MATRIX_STATUS_RUNNING;
            if (reportId == null || reportId.length() == 0) reportId = "TMR_" + startedAt;
        }

        public void markFinishOk() {
            finishedAt = System.currentTimeMillis();
            if (startedAt > 0L) {
                costMs = finishedAt - startedAt;
                if (costMs < 0L) costMs = 0L;
            }
            status = AppConfig.TEACHER_MATRIX_STATUS_READY;
            buildSummary();
        }

        public void markError(String text) {
            finishedAt = System.currentTimeMillis();
            if (startedAt > 0L) {
                costMs = finishedAt - startedAt;
                if (costMs < 0L) costMs = 0L;
            }
            status = AppConfig.TEACHER_MATRIX_STATUS_ERROR;
            errorText = text == null ? "" : text;
            buildSummary();
        }

        public void buildSummary() {
            StringBuilder sb = new StringBuilder();
            sb.append("教师矩阵报告：窗口=").append(windowSize);
            sb.append("，目标期=").append(targetCount);
            sb.append("，样本=").append(sampleCount);
            sb.append("，正样本=").append(positiveCount);
            sb.append("，负样本=").append(negativeCount);
            sb.append("，画像=").append(profileCount);
            sb.append("，组合画像=").append(comboProfileCount);
            sb.append("，原因样本=").append(reasonSampleCount);
            sb.append("，原因画像=").append(reasonProfileCount);
            sb.append("，强画像=").append(strongProfileCount);
            sb.append("，风险画像=").append(riskProfileCount);
            sb.append("，均命中=").append(UiFormatter.probability(avgHitRate));
            sb.append("，均置信=").append(UiFormatter.probability(avgConfidence));
            sb.append("，原因边际=").append(UiFormatter.signedProbability(avgReasonEdge));
            sb.append("，耗时=").append(costMs).append("ms");
            if (errorText != null && errorText.length() > 0) sb.append("，错误=").append(errorText);
            summaryText = sb.toString();
        }
    }

    public static String buildTeacherProfileKey(String stateKey, int position, int number) {
        StringBuilder sb = new StringBuilder();
        sb.append("TM|");
        sb.append(stateKey == null ? "" : stateKey);
        sb.append("|P").append(position);
        sb.append("|N").append(number);
        return sb.toString();
    }

    public static String buildTeacherComboKey(String stateKey, int position, int k3Mask) {
        StringBuilder sb = new StringBuilder();
        sb.append("TC|");
        sb.append(stateKey == null ? "" : stateKey);
        sb.append("|P").append(position);
        sb.append("|K").append(k3Mask);
        return sb.toString();
    }

    public static final class TeacherSnapshot {
        public long id;
        public String snapshotId;
        public String sourceType;
        public String status;
        public long createdAt;
        public long startedAt;
        public long finishedAt;
        public long costMs;
        public int windowSize;
        public int totalHistory;
        public int totalSteps;
        public int testCount;
        public String startIssue;
        public String endIssue;
        public int betCount;
        public int hitCount;
        public int missCount;
        public int holdCount;
        public int shadowCount;
        public double hitRate;
        public double winRate;
        public double frequency;
        public double pnl;
        public double totalProfit;
        public double maxDrawdown;
        public double avgProfitPerBet;
        public double calibrationError;
        public double brierScore;
        public int aPlusCount;
        public double aPlusHitRate;
        public double biasScoreAvg;
        public double gateAdjust;
        public double probabilityAdjust;
        public double riskAdjust;
        public double teacherConfidence;
        public double stabilityScore;
        public double overfitRisk;
        public double profileFreshness;
        public double[] positionWeights;
        public double[] numberWeights;
        public double stableModeWeight;
        public double confirmedModeWeight;
        public double acceleratingModeWeight;
        public double recoveringModeWeight;
        public double pullbackModeWeight;
        public double transitionModeWeight;
        public double weakeningModeWeight;
        public double chaosModeWeight;
        public double brokenModeWeight;
        public double reversingModeWeight;
        public int teacherMatrixSampleCount;
        public int teacherMatrixProfileCount;
        public double teacherMatrixAvgHitRate;
        public double teacherMatrixAvgConfidence;
        public double teacherMatrixGateAdjust;
        public double teacherMatrixProbAdjust;
        public int reasonSampleCount;
        public int reasonProfileCount;
        public double reasonAvgEdge;
        public double reasonAvgConfidence;
        public int elite45PassCount;
        public String topProfiles;
        public String topMissReasons;
        public String topHitReasons;
        public String promotedProfiles;
        public String rejectedProfiles;
        public String highWinShapeText;
        public String highRiskShapeText;
        public String learningSummary;
        public String gateSummary;
        public String probabilitySummary;
        public String auditSummary;
        public String biasSummary;
        public String promotionSummary;
        public String aPlusSummary;
        public String teacherMatrixSummary;
        public String reasonSummary;
        public String elite45Summary;
        public String summaryText;
        public String errorText;

        public TeacherSnapshot() {
            id = 0L;
            snapshotId = "";
            sourceType = "TEACHER";
            status = AppConfig.BACKTEST_STATUS_IDLE;
            createdAt = 0L;
            startedAt = 0L;
            finishedAt = 0L;
            costMs = 0L;
            windowSize = AppConfig.MANUAL_BACKTEST_DEFAULT_WINDOW;
            totalHistory = 0;
            totalSteps = 0;
            testCount = 0;
            startIssue = "";
            endIssue = "";
            betCount = 0;
            hitCount = 0;
            missCount = 0;
            holdCount = 0;
            shadowCount = 0;
            hitRate = 0.0d;
            winRate = 0.0d;
            frequency = 0.0d;
            pnl = 0.0d;
            totalProfit = 0.0d;
            maxDrawdown = 0.0d;
            avgProfitPerBet = 0.0d;
            calibrationError = 0.0d;
            brierScore = 0.0d;
            aPlusCount = 0;
            aPlusHitRate = 0.0d;
            biasScoreAvg = 0.0d;
            gateAdjust = 0.0d;
            probabilityAdjust = 0.0d;
            riskAdjust = 0.0d;
            teacherConfidence = 0.0d;
            stabilityScore = 0.0d;
            overfitRisk = 0.0d;
            profileFreshness = 1.0d;
            positionWeights = new double[AppConfig.POSITION_COUNT + 1];
            numberWeights = new double[AppConfig.NUMBER_COUNT + 1];
            for (int i = 0; i < positionWeights.length; i++) positionWeights[i] = 1.0d;
            for (int i = 0; i < numberWeights.length; i++) numberWeights[i] = 1.0d;
            stableModeWeight = 1.0d;
            confirmedModeWeight = 1.0d;
            acceleratingModeWeight = 1.0d;
            recoveringModeWeight = 1.0d;
            pullbackModeWeight = 1.0d;
            transitionModeWeight = 1.0d;
            weakeningModeWeight = 1.0d;
            chaosModeWeight = 1.0d;
            brokenModeWeight = 1.0d;
            reversingModeWeight = 1.0d;
            teacherMatrixSampleCount = 0;
            teacherMatrixProfileCount = 0;
            teacherMatrixAvgHitRate = 0.0d;
            teacherMatrixAvgConfidence = 0.0d;
            teacherMatrixGateAdjust = 0.0d;
            teacherMatrixProbAdjust = 0.0d;
            reasonSampleCount = 0;
            reasonProfileCount = 0;
            reasonAvgEdge = 0.0d;
            reasonAvgConfidence = 0.0d;
            elite45PassCount = 0;
            topProfiles = "";
            topMissReasons = "";
            topHitReasons = "";
            promotedProfiles = "";
            rejectedProfiles = "";
            highWinShapeText = "";
            highRiskShapeText = "";
            learningSummary = "";
            gateSummary = "";
            probabilitySummary = "";
            auditSummary = "";
            biasSummary = "";
            promotionSummary = "";
            aPlusSummary = "";
            teacherMatrixSummary = "";
            reasonSummary = "";
            elite45Summary = "";
            summaryText = "";
            errorText = "";
        }

        public void recalc() {
            if (missCount <= 0 && betCount >= hitCount) missCount = betCount - hitCount;
            if (missCount < 0) missCount = 0;
            if (betCount > 0) hitRate = ((double) hitCount) / ((double) betCount);
            winRate = hitRate;
            if (testCount > 0) frequency = ((double) betCount) / ((double) testCount);
            if (betCount > 0 && avgProfitPerBet == 0.0d)
                avgProfitPerBet = pnl / ((double) betCount);
            teacherConfidence = calcTeacherConfidence();
            stabilityScore = calcStabilityScore();
            overfitRisk = calcOverfitRisk();
            gateAdjust = buildTeacherGateAdjust();
            probabilityAdjust = buildTeacherProbabilityAdjust();
            riskAdjust = buildTeacherRiskAdjust();
            gateSummary =
                    "教师门槛修正="
                            + UiFormatter.signedProbability(gateAdjust)
                            + "，矩阵门槛="
                            + UiFormatter.signedProbability(teacherMatrixGateAdjust)
                            + "，原因边际="
                            + UiFormatter.signedProbability(reasonAvgEdge)
                            + "，稳定分="
                            + UiFormatter.probability(stabilityScore)
                            + "，过拟合风险="
                            + UiFormatter.probability(overfitRisk);
            probabilitySummary =
                    "深测胜率="
                            + UiFormatter.probability(hitRate)
                            + "，频率="
                            + UiFormatter.probability(frequency)
                            + "，校准误差="
                            + UiFormatter.probability(calibrationError)
                            + "，概率修正="
                            + UiFormatter.signedProbability(probabilityAdjust)
                            + "，矩阵概率="
                            + UiFormatter.signedProbability(teacherMatrixProbAdjust);
        }

        private double calcTeacherConfidence() {
            double score = 0.0d;
            if (windowSize >= 1000) score += 0.18d;
            else if (windowSize >= 500) score += 0.12d;
            else if (windowSize >= 300) score += 0.08d;

            if (testCount >= 1000) score += 0.18d;
            else if (testCount >= 500) score += 0.12d;
            else if (testCount >= 300) score += 0.08d;

            if (betCount >= 120) score += 0.16d;
            else if (betCount >= 80) score += 0.12d;
            else if (betCount >= 40) score += 0.07d;

            if (teacherMatrixSampleCount >= AppConfig.TEACHER_MATRIX_STRONG_SAMPLE) score += 0.14d;
            else if (teacherMatrixSampleCount >= AppConfig.TEACHER_MATRIX_MIN_SAMPLE)
                score += 0.08d;

            if (reasonSampleCount >= REASON_STRONG_SAMPLE) score += 0.14d;
            else if (reasonSampleCount >= REASON_MIN_SAMPLE) score += 0.07d;

            if (hitRate >= AppConfig.TARGET_HIT_RATE_IDEAL) score += 0.10d;
            else if (hitRate >= AppConfig.TARGET_HIT_RATE_MIN) score += 0.06d;

            if (frequency >= AppConfig.TARGET_FREQ_MIN && frequency <= AppConfig.TARGET_FREQ_MAX)
                score += 0.06d;
            if (profileFreshness >= 0.70d) score += 0.08d;
            return AppConfig.clampProbability(score);
        }

        private double calcStabilityScore() {
            double score = 0.0d;
            if (hitRate >= AppConfig.TARGET_HIT_RATE_IDEAL) score += 0.30d;
            else if (hitRate >= AppConfig.TARGET_HIT_RATE_MIN) score += 0.22d;
            else if (hitRate >= 0.340d) score += 0.10d;

            if (teacherMatrixAvgHitRate >= AppConfig.TEACHER_MATRIX_STRONG_HIT_RATE) score += 0.14d;
            else if (teacherMatrixAvgHitRate >= AppConfig.TEACHER_MATRIX_WEAK_HIT_RATE)
                score += 0.06d;

            if (reasonAvgEdge >= ELITE45_MIN_REASON_EDGE) score += 0.12d;

            if (frequency >= AppConfig.TARGET_FREQ_MIN && frequency <= AppConfig.TARGET_FREQ_MAX)
                score += 0.20d;
            else if (frequency <= AppConfig.TARGET_FREQ_HARD_MAX) score += 0.10d;

            if (pnl >= 0.0d) score += 0.16d;
            if (calibrationError <= AppConfig.CALIBRATION_ERROR_MAX) score += 0.10d;
            else if (calibrationError <= AppConfig.CALIBRATION_ERROR_HARD) score += 0.05d;
            if (overfitRisk <= 0.35d) score += 0.06d;
            return AppConfig.clampProbability(score);
        }

        private double calcOverfitRisk() {
            double risk = 0.0d;
            if (betCount > 0 && hitRate >= 0.520d) risk += 0.22d;
            if (frequency < 0.030d && hitRate >= 0.450d) risk += 0.18d;
            if (testCount < 300) risk += 0.18d;
            if (betCount < 40) risk += 0.18d;
            if (teacherMatrixSampleCount < AppConfig.TEACHER_MATRIX_MIN_SAMPLE) risk += 0.12d;
            if (reasonSampleCount < REASON_MIN_SAMPLE) risk += 0.12d;
            if (calibrationError >= AppConfig.CALIBRATION_ERROR_HARD) risk += 0.18d;
            if (pnl < 0.0d) risk += 0.12d;
            return AppConfig.clampProbability(risk);
        }

        private double buildTeacherGateAdjust() {
            double adjust = 0.0d;
            if (teacherConfidence < 0.30d) return 0.0d;
            if (overfitRisk >= 0.60d) adjust += 0.012d;
            else if (overfitRisk >= 0.45d) adjust += 0.008d;
            if (frequency > AppConfig.TARGET_FREQ_HARD_MAX) adjust += 0.012d;
            else if (frequency > AppConfig.TARGET_FREQ_MAX) adjust += 0.008d;
            if (hitRate < AppConfig.TARGET_HIT_RATE_MIN && betCount >= 40) adjust += 0.010d;
            if (teacherMatrixAvgHitRate > 0.0d
                    && teacherMatrixAvgHitRate < AppConfig.TEACHER_MATRIX_WEAK_HIT_RATE)
                adjust += 0.008d;
            if (reasonSampleCount > 0 && reasonAvgEdge < 0.0d) adjust += 0.008d;
            if (pnl < 0.0d && betCount >= 40) adjust += 0.008d;
            if (stabilityScore >= 0.75d
                    && hitRate >= AppConfig.TARGET_HIT_RATE_IDEAL
                    && frequency >= AppConfig.TARGET_FREQ_MIN
                    && frequency <= AppConfig.TARGET_FREQ_MAX
                    && pnl >= 0.0d) {
                adjust -= 0.004d;
            }
            return clampAbs(adjust, AppConfig.TEACHER_MAX_GATE_ADJUST);
        }

        private double buildTeacherProbabilityAdjust() {
            double adjust = 0.0d;
            if (teacherConfidence < AppConfig.TEACHER_MIN_CONFIDENCE) return 0.0d;
            if (stabilityScore >= 0.70d && hitRate >= AppConfig.TARGET_HIT_RATE_MIN)
                adjust += 0.006d;
            if (hitRate >= AppConfig.TARGET_HIT_RATE_IDEAL && pnl >= 0.0d) adjust += 0.006d;
            if (teacherMatrixAvgConfidence >= AppConfig.TEACHER_MATRIX_STRONG_CONFIDENCE
                    && teacherMatrixAvgHitRate >= AppConfig.TEACHER_MATRIX_STRONG_HIT_RATE)
                adjust += 0.006d;
            if (reasonAvgEdge >= ELITE45_MIN_REASON_EDGE && reasonAvgConfidence >= 0.50d)
                adjust += 0.006d;
            if (overfitRisk >= 0.55d) adjust -= 0.008d;
            if (calibrationError >= AppConfig.CALIBRATION_ERROR_MAX) adjust -= 0.006d;
            return clampAbs(adjust, AppConfig.TEACHER_MAX_PROB_ADJUST);
        }

        private double buildTeacherRiskAdjust() {
            double adjust = 0.0d;
            if (overfitRisk >= 0.60d) adjust += 0.040d;
            else if (overfitRisk >= 0.45d) adjust += 0.025d;
            if (calibrationError >= AppConfig.CALIBRATION_ERROR_HARD) adjust += 0.030d;
            if (frequency > AppConfig.TARGET_FREQ_HARD_MAX) adjust += 0.030d;
            return AppConfig.clampProbability(adjust);
        }

        public boolean canFeedRealtime() {
            if (!AppConfig.BACKTEST_STATUS_OK.equals(status)) return false;
            if (createdAt <= 0L && finishedAt <= 0L) return false;
            return teacherConfidence >= 0.20d;
        }

        public double getPositionWeight(int position) {
            if (!AppConfig.isValidPosition(position)
                    || positionWeights == null
                    || position >= positionWeights.length) return 1.0d;
            return clampTeacherWeight(positionWeights[position]);
        }

        public void setPositionWeight(int position, double weight) {
            if (!AppConfig.isValidPosition(position)) return;
            if (positionWeights == null || positionWeights.length <= AppConfig.POSITION_COUNT) {
                positionWeights = new double[AppConfig.POSITION_COUNT + 1];
                for (int i = 0; i < positionWeights.length; i++) positionWeights[i] = 1.0d;
            }
            positionWeights[position] = clampTeacherWeight(weight);
        }

        public double getNumberWeight(int number) {
            if (!AppConfig.isValidNumber(number)
                    || numberWeights == null
                    || number >= numberWeights.length) return 1.0d;
            return clampTeacherWeight(numberWeights[number]);
        }

        public void setNumberWeight(int number, double weight) {
            if (!AppConfig.isValidNumber(number)) return;
            if (numberWeights == null || numberWeights.length <= AppConfig.NUMBER_COUNT) {
                numberWeights = new double[AppConfig.NUMBER_COUNT + 1];
                for (int i = 0; i < numberWeights.length; i++) numberWeights[i] = 1.0d;
            }
            numberWeights[number] = clampTeacherWeight(weight);
        }

        public double getStateModeWeight(String mode) {
            mode = AppConfig.normalizeMode(mode);
            if (AppConfig.MODE_STABLE.equals(mode)) return clampTeacherWeight(stableModeWeight);
            if (AppConfig.MODE_CONFIRMED.equals(mode))
                return clampTeacherWeight(confirmedModeWeight);
            if (AppConfig.MODE_ACCELERATING.equals(mode))
                return clampTeacherWeight(acceleratingModeWeight);
            if (AppConfig.MODE_RECOVERING.equals(mode))
                return clampTeacherWeight(recoveringModeWeight);
            if (AppConfig.MODE_PULLBACK.equals(mode)) return clampTeacherWeight(pullbackModeWeight);
            if (AppConfig.MODE_TRANSITION.equals(mode))
                return clampTeacherWeight(transitionModeWeight);
            if (AppConfig.MODE_WEAKENING.equals(mode))
                return clampTeacherWeight(weakeningModeWeight);
            if (AppConfig.MODE_CHAOS.equals(mode)) return clampTeacherWeight(chaosModeWeight);
            if (AppConfig.MODE_BROKEN.equals(mode)) return clampTeacherWeight(brokenModeWeight);
            if (AppConfig.MODE_REVERSING.equals(mode) || AppConfig.MODE_REVERSAL.equals(mode))
                return clampTeacherWeight(reversingModeWeight);
            return 1.0d;
        }

        public String buildSummaryText() {
            StringBuilder sb = new StringBuilder();
            sb.append("教师快照W").append(windowSize);
            sb.append("：BET=").append(betCount).append("/").append(testCount);
            sb.append("，胜率=").append(UiFormatter.probability(hitRate));
            sb.append("，频率=").append(UiFormatter.probability(frequency));
            sb.append("，盈亏=").append(UiFormatter.money(pnl));
            sb.append("，回撤=").append(UiFormatter.money(maxDrawdown));
            sb.append("，矩阵样本=").append(teacherMatrixSampleCount);
            sb.append("，矩阵置信=").append(UiFormatter.probability(teacherMatrixAvgConfidence));
            sb.append("，原因样本=").append(reasonSampleCount);
            sb.append("，原因边际=").append(UiFormatter.signedProbability(reasonAvgEdge));
            sb.append("，Elite45通过=").append(elite45PassCount);
            sb.append("，置信=").append(UiFormatter.probability(teacherConfidence));
            sb.append("，门槛修正=")
                    .append(UiFormatter.signedProbability(gateAdjust + teacherMatrixGateAdjust));
            return sb.toString();
        }
    }

    public static final class SimilarResult {
        public int effectiveSample;
        public int rawSample;
        public double similarHitRate;
        public double recentHitRate;
        public double avgDistance;
        public double confidence;
        public String shapeKey;
        public int usable;

        public SimilarResult() {
            effectiveSample = 0;
            rawSample = 0;
            similarHitRate = 0.0d;
            recentHitRate = 0.0d;
            avgDistance = 0.0d;
            confidence = 0.0d;
            shapeKey = "";
            usable = 0;
        }

        public boolean isStrongEnough() {
            return effectiveSample >= AppConfig.EFFECTIVE_SAMPLE_MIN
                    && similarHitRate >= AppConfig.SIMILAR_HIT_RATE_MIN;
        }
    }

    public static final class ComboCandidate {
        /* V48 stage-rank and diagnostic observation audit. */
        public int v48InitialRank;
        public int v48TeacherRank;
        public int v48DynamicRank;
        public int v48CalibratedRank;
        public String v48ChampionStatus;
        public String v48ModelId;
        public String v48FailureCode;

        /* V46唯一实时合同：EventP与ActionLCB严格分离。 */
        public double v46EventProbability;
        public double v46ActionLcb;
        public double v46ProbabilityUncertainty;
        public double v46ExpectedValue;
        public double v46GlobalCalibratedProbability;
        public double v46PositionAdjustment;
        public double v46RankAdjustment;
        public double v46RegimeAdjustment;
        public double v46CalibrationConfidence;
        public int v46FinalRank;
        public int v46FormalCandidateCount;
        public String v46Version;
        public String v46NumberDistributionSummary;
        public String v46ProbabilityTrace;
        public String v46RootCauseCode;
        public String v46RootCauseOwner;
        public String v46FinalContractHash;
        public String v46Lane;
        public String trendKey;

        /* 星河 V40：全号码分布、OOS校准、分层后验、不可变契约与唯一授权。 */
        public double rawRankScore;
        public double calibratedHitProbability;
        public double honestProbability;
        public double actionUtility;
        public double actionStructureScore;
        public double actionDiscriminationScore;
        public double actionMaturityScore;
        public double actionUncertaintyPenalty;
        public double actionFrequencySupport;
        /* V42 unified risk-coverage evidence.  These fields are immutable audit
         * evidence after lane compilation; they never create a BET. */
        public double v42AcceptScore;
        public double v42FamilyLcb;
        public double v42RegimeQuality;
        public double v42AssociationScore;
        public double v42DistillationRisk;
        public int v42SupportVotes;
        public String v42RegimeFamily;
        public String v42RiskCoverageMode;
        public String v42PrimaryReject;
        public String v42SecondaryReject;
        public String v42TertiaryReject;
        public String v42LanePolicyVersion;
        /* V43 canonical full-strategy evidence contract. */
        public double v43StrategySupport;
        public double v43StrategyRisk;
        public double v43StrategyConfidence;
        public double v43StrategyDiversity;
        public double v43StrategyMaturity;
        public double v43SafeProbability;
        public double v43ProbabilityResidual;
        public double v43AblationFragility;
        public double v43ProbabilityLock;
        public int v43EvidenceGroupCount;
        public int v43HardRisk;
        public String v43DominantEvidence;
        public String v43MissingEvidence;
        public String v43EvidenceFingerprint;
        public String v43ProbabilityLockHash;
        public String v43PipelineStage;
        public String v43InvariantStatus;
        public String v43StrategyVersion;
        /* V46 separated event probability, conservative action bound and final funnel. */
        public double v46ConditionalAdjustment;
        public int v46ConditionalSample;
        public String v46ConditionalCalibrationSummary;
        public String v46ContractHash;
        public String actionUtilityVersion;
        public String laneCandidate;
        public int oosRankSlot;
        public int oosCandidateCount;
        public String buildId;
        public String cohortId;
        public String schemaVersion;
        public String contractDecisionId;
        public String candidateHash;
        public String snapshotMaxIssue;
        public String trainedUntilIssue;
        public String featureSchemaVersion;
        public String rankingVersion;
        public String calibrationVersion;
        public String gatePolicyVersion;
        public String pipelineVersion;
        public int contractValid;
        public String contractStatus;
        public String coarseFamilyKey;
        public String midFamilyKey;
        public String fineFamilyKey;
        public String positionFamilyKey;
        public String posteriorMatchedLevel;
        public String posteriorFamilyKey;
        public String posteriorPolicy;
        public String posteriorVersion;
        public int posteriorSample;
        public int posteriorHit;
        public double posteriorMean;
        public double posteriorWilsonLower;
        public double posteriorRecentRate;
        public double posteriorDrift;
        public double familyWilsonLower;
        public int calibrationBucket;
        public int calibrationSample;
        public int calibrationTotalSample;
        public int calibrationReady;
        public String calibrationStatus;
        public double calibrationBrier;
        public String authorizationId;
        public int authorizationValid;
        public String authorizedMode;
        public String authorizationRejectCode;
        public long authorizedAt;
        public double authorizationProbability;
        public double authorizationGate;
        public double authorizationEdge;
        public String teacherPolicy;
        public String dynamicPolicy;
        public String adaptivePolicy;
        public String microWalkForwardStatus;
        public String microWalkForwardVersion;
        public String microWalkForwardAsOfIssue;
        public int microWalkForwardSample;

        public int rank;
        public String baseIssue;
        public String targetIssue;
        public long createdAt;
        public int position;
        public int n1;
        public int n2;
        public int n3;
        public int k3Mask;
        public String shapeKey;
        public String stateKey;
        public String stateMode;
        public double modelP;
        public double shapeP;
        public double comboP;
        public double recoveryP;
        public double rawP;
        public double finalP;
        public double dynamicGate;
        public double edge;
        public double similarP;
        public double memoryP;
        public double calibratedP;
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
        public double freq100;
        public double freq500;
        public double freq1000;
        public String blockCode;
        public String reasonSummary;
        public String probabilityMode;
        public String backtestMode;
        public double deepScore;
        public double deepConfidence;
        public double deepFeatureP;
        public double deepFeatureScore;
        public double deepFeatureConfidence;
        public double deepFeatureRisk;
        public double deepFeatureGateAdjust;
        public double deepFeatureProbAdjust;
        public String deepFeatureStatus;
        public String deepFeatureSummary;
        public double deepPositionScore;
        public double deepNumberScore;
        public double deepCycleScore;
        public double deepTransitionScore;
        public double deepStateScore;
        public double biasScore;
        public double biasConfidence;
        public double profileScore;
        public double fallbackProfileScore;
        public double runtimeProfileScore;
        public double stateNumberProfileScore;
        public double numberBias;
        public double comboBias;
        public double teacherWeight;
        public int teacherNumberSample;
        public int teacherNumberHistorySample;
        public int teacherDomainShadowSample;
        public int teacherDomainRealSample;
        public double teacherNumberHitRate;
        public double teacherNumberConfidence;
        public double teacherNumberScore;
        public double teacherNumberRisk;
        public String teacherNumberSummary;
        public String teacherApplyError;
        public String preDecisionDomainVersion;
        public String teacherRuntimeApplyVersion;
        public double teacherMatrixWeight;
        public double teacherMatrixScore;
        public double teacherMatrixHitRate;
        public double teacherMatrixRecentHitRate;
        public double teacherMatrixConfidence;
        public double teacherMatrixProbAdjust;
        public double teacherMatrixGateAdjust;
        public double teacherMatrixRiskPenalty;
        public double teacherMatrixPositionScore;
        public double teacherMatrixNumberScore;
        public double teacherMatrixComboScore;
        public double teacherMatrixStateScore;
        public int teacherMatrixSample;
        public int teacherMatrixStateSample;
        public int teacherMatrixComboSample;
        public int teacherMatrixApplied;
        public String teacherMatrixStatus;
        public String teacherMatrixProfileKey;
        public String teacherMatrixComboKey;
        public String teacherMatrixSummary;

        /* V20 上游证据补实字段：只承接字段，不决定 BET */
        public int teacherWeakNoBetFlag;
        public int teacherMatrixWeakNoBetFlag;
        public int teacherMatrixOrdinaryBetBlockFlag;
        public int teacherMatrixSupportsFinalGate;
        public int teacherMatrixSupportsBet;
        public int teacherMatrixRiskFlag;
        public int teacherMatrixFreezeFlag;
        public String teacherOrdinaryBetPolicy;
        public String teacherMatrixOrdinaryBetPolicy;
        public String teacherFieldSource;
        public String teacherFieldVersion;
        public String teacherNumberStatus;
        public String teacherStatus;
        public String teacherStatusClass;
        public double teacherEvidenceContribution;
        public double teacherMatrixEvidenceContribution;
        public double teacherCounterContribution;
        public double teacherMatrixCounterContribution;
        public double teacherSampleReliability;
        public double teacherRecentValidity;

        public String v20Status;
        public String dynamicStatus;
        public String dynamicCauseStatus;
        public String v20ActionAdvice;
        public String v20FieldSource;
        public String v20FieldVersion;
        public int v20EmptyFlag;
        public double v20SupportScore;
        public double v20RiskScore;
        public double v20EvidenceContribution;
        public double v20CounterContribution;
        public double v20SampleReliability;
        public double v20RecentValidity;
        public int similarWindowSample;
        public double similarWindowHitRate;
        public String v20WindowStateKey;
        public String windowStateKey;
        public String finalDecisionWindowStateKey;
        public String dynamicWindowStateKey;
        public String probabilityWindowStateKey;

        public double coverageQualityScore;
        public double thirdNumberQualityScore;
        public double k3CoverageRisk;
        public int falseHighPFlag;
        public int probabilityFalseHighPFlag;
        public double probabilityFalseHighRisk;
        public int probabilityPreferredEdgeBand;
        public int probabilityHighEdgeFlag;
        public int probabilityPreferredPBand;
        public int probabilityTeacherWeakOrEmpty;
        public int probabilityV20UnknownOrEmpty;
        public int probabilityStrongCauseFlag;
        public double coverageProbDiscount;
        public double coverageProbBoost;
        public double coverageK3HitRate32;
        public double coverageK3HitRate64;
        public double coverageK3HitRate128;
        public double coverageNumberRate1;
        public double coverageNumberRate2;
        public double coverageNumberRate3;
        public double finalPBeforeCoverage;
        public double finalPAfterCoverage;
        public double coverageAdjustedP;
        public String probabilityCoverageSummary;
        public String coverageQualitySummary;
        public String probabilityCoverageActionAdvice;
        public String probabilityActionAdvice;
        public String probabilityFieldVersion;

        public String dynamicGateQualityStatus;
        public int dynamicGateFalseHighPFlag;
        public int dynamicGatePreferredEdgeBand;
        public int dynamicGateTeacherV20WeakFlag;
        public String dynamicGateEdgePolicy;
        public double dynamicGateCoverageRisk;
        public double dynamicGateCoverageQualityScore;
        public double dynamicGateThirdNumberQuality;
        public double dynamicGateVirtualEdgeRisk;
        public String dynamicGateActionAdvice;
        public String dynamicGateFieldVersion;
        public String dynamicGateVersion;

        public String finalDecisionVersion;
        public String finalDecisionEdgePolicy;
        public String finalDecisionTeacherPolicy;
        public String finalDecisionV20Policy;
        public String finalDecisionBetDomain;
        public String finalDecisionRejectCode;
        public String finalDecisionShadowReason;
        public String finalDecisionStrongOverrideTrace;
        public double finalDecisionCoverageRisk;
        public double finalDecisionCoverageQuality;
        public double finalDecisionThirdNumberQuality;
        public int finalDecisionFalseHighPFlag;
        /* V20主链闭环字段：只承接字段，不计算概率，不决定BET */
        public String clusterKey;
        public String factorKey;
        public double positionNeedScore;
        public double numberSupplyScore;
        public double coverageQuality;
        public double thirdQuality;
        public double counterEvidenceScore;
        public int similarSampleCount;
        public String adaptiveStatus;
        public String shadowPromotionStatus;
        public double finalPBeforeDiagnosis;
        public double finalPAfterDiagnosis;
        public double gate;
        public String rejectCode;
        public int realProbeAllowed;

        /* 73487246：统一真实证据与结构化风险合同。 */
        public int preDecisionOpportunity;
        public String riskSeverity;
        public int hardBlockFlag;
        public String hardBlockSource;
        public String evidenceSource;
        public int evidenceSettledOnly;
        public int evidenceSampleCount;
        public int evidenceHitCount;
        public int evidenceMissCount;
        public String promotionCoarseDomain;
        public String promotionMidDomain;
        public String promotionFineDomain;

        /* 链路修改升级V2字段：只承接，不计算概率，不决定BET */
        public double realBetFrequency50;
        public double realBetFrequency100;
        public double realBetFrequency300;
        public double frequencyDebtScore;
        public double frequencySupportScore;
        public double frequencyGateAdjust;
        public double qualifiedProbeScore;
        public double shadowPromotionScore;
        public double baseGate;
        public double riskAdjustedGate;
        public double effectiveProbeGate;
        public double rawEdge;
        public double effectiveProbeEdge;
        public double needSupplyScore;
        public double counterRisk;
        public int qualifiedProbe;
        public int realTrade;
        public int coverageAvailable;
        public int thirdQualityAvailable;
        public int promotionDomainAvailable;
        public String frequencyState;
        public String frequencyStateVersion;
        public String shadowPromotionDomain;
        public String decisionPath;
        public String betDomain;
        public String probeRejectReason;
        public String probeEligibilityVersion;
        public String ruleHealthStatus;
        public String snapshotFreshness;
        public String warmProfileFreshness;
        public String coldProfileFreshness;
        public String deepProfileFreshness;
        public String decisionVersionV2;
        public String evidenceAsOfIssue;
        public String snapshotAsOfIssue;
        public long snapshotCreatedAt;
        public long frequencyStateUpdatedAt;
        public int frequencyFloorRequiredCount;
        public int frequencyFloorTargetCount;
        public int frequencyFloorActualCount;
        public int frequencyFloorDebtCount;
        public int frequencyTargetDebtCount;
        public int frequencyFloorMandatory;
        public int frequencyFloorNoUpperCap;
        public double frequencyFloorRankScore;
        public String frequencyFloorVersion;

        /* 73487246：四窗口平滑频率、冻结域、Teacher/Dynamic分层审计。 */
        public double realBetFrequency20;
        public int frequencyObserved20;
        public int frequencyObserved50;
        public int frequencyObserved100;
        public int frequencyObserved300;
        public int frequencyBetCount20;
        public int frequencyBetCount50;
        public int frequencyBetCount100;
        public int frequencyBetCount300;
        public int frequencyDebt20;
        public int frequencyDebt50;
        public int frequencyDebt100;
        public int frequencyDebt300;
        public int frequencyTargetDebt20;
        public int frequencyTargetDebt50;
        public int frequencyTargetDebt100;
        public int frequencyTargetDebt300;
        public String frequencyUrgentWindow;
        public int frequencyIssuesSinceLastRealBet;
        public int frequencyIssuesSinceLastFloorBet;
        public int frequencyConsecutiveFloorBets;
        public int frequencyFloorMinGap;
        public int frequencyFloorPacingReady;
        public String frequencyFloorPacingReason;
        public String decisionQualityPath;
        public int evidenceDomainFrozen;
        public String evidenceDomainFrozenIssue;
        public long evidenceDomainFrozenAt;
        public String dynamicMatchedLevel;
        public int dynamicRealSample;
        public int dynamicShadowSample;
        public String moduleBuildVersion;

        /* 73487246：有限大类→中类→细类历史先验及贝叶斯融合审计。 */
        public double historicalPriorP;
        public double historicalPriorConfidence;
        public int historicalPriorSample;
        public String historicalPriorCoarseKey;
        public String historicalPriorMidKey;
        public String historicalPriorSummary;
        public double basePBeforeFusion;
        public double finalPAfterFusion;
        public double fusionWeightBase;
        public double fusionWeightPrior;
        public double fusionWeightTeacher;
        public double fusionWeightDynamic;
        public double fusionWeightForest;
        public String fusionSummary;
        public double evidenceForestP;
        public double evidenceForestConfidence;
        public int evidenceForestSample;
        public int evidenceForestTreeCount;
        public String evidenceForestSummary;

        /* V20动态因果窗口字段：只承接字段，不直接决定BET */
        public String dynamicClusterId;
        public String dynamicClusterStatus;
        public String dynamicClusterPrototypeKey;
        public String dynamicClusterFeatureKey;
        public int dynamicClusterSample;
        public int dynamicClusterHitCount;
        public int dynamicClusterMissCount;
        public int dynamicClusterRecentSample;
        public int dynamicClusterRecentHitCount;
        public double dynamicClusterHitRate;
        public double dynamicClusterRecentHitRate;
        public double dynamicClusterConfidence;
        public double dynamicClusterDistance;
        public double dynamicClusterVolatility;
        public int dynamicClusterMaxLose;
        public double dynamicClusterCounterScore;
        public String dynamicClusterVersion;
        public String dynamicClusterReason;

        public String positionNeedKey;
        public String positionNeedSummary;
        public double positionNeedStrength;
        public double positionNeedChaosRisk;
        public double positionNeedRepairScore;
        public double positionNeedRelayScore;
        public String numberSupplyKey;
        public String numberSupplySummary;
        public double numberSupplyStrength;
        public double numberSupplyRisk;
        public String needSupplyMatchKey;
        public double needSupplyMatchScore;
        public double needSupplyThirdQuality;
        public double needSupplyRoleDiversity;
        public double needSupplyRisk;
        public String needSupplySummary;

        public String k3RoleMain;
        public String k3RoleAssist;
        public String k3RoleDefense;
        public int k3RoleMainNumber;
        public int k3RoleAssistNumber;
        public int k3RoleDefenseNumber;
        public String k3RoleStructure;

        public String counterfactualReasonKey;
        public int counterfactualRank;
        public double counterfactualRankScore;
        public double counterfactualWhyNotRisk;
        public String counterfactualWhyNot;
        public String counterfactualSummary;
        public String causeAblationKey;
        public double causeAblationScore;
        public int causeAblationFalseCauseCount;
        public String causeAblationSummary;
        public String missReasonType;
        public String missReasonMatrixKey;
        public String missReasonDetail;

        public int adaptiveJudgeCandidateCount;
        public String adaptiveJudgeStatus;
        public String adaptiveJudgeAction;
        public String adaptiveJudgeDomain;
        public double adaptiveClusterHitRate;
        public double adaptiveClusterRecentHitRate;
        public int adaptiveClusterSample;
        public double adaptiveClusterConfidence;
        public double adaptiveCauseScore;
        public double adaptiveCounterScore;
        public double adaptivePromoteScore;
        public double adaptiveFreezeScore;
        public String adaptiveJudgeReason;
        public String adaptiveJudgeVersion;

        public String promotionClusterStatus;
        public double promotionClusterHitRate;
        public double promotionClusterRecentHitRate;
        public int promotionClusterSample;
        public String promotionClusterReason;

        public String whyBet;
        public String whyNoBet;
        public String whyHit;
        public String whyMiss;
        public String dynamicCausalSummary;
        public double stateConfidence;
        public double auditPenalty;
        public double auditBoost;
        public double promotionScore;
        public double aPlusScore;
        public double evidenceScore;
        public double evidenceRisk;
        public double aiJudgeScore;
        public double aiJudgeConfidence;
        public double conflictLevel;
        public double weakestNumberScore;
        public double balanceScore;
        public double rankScore;
        public double finalRankScore;
        public int stateSample;
        public int cacheSample;
        public int recentSample;
        public double recentHitRate;
        public String deepProfileKey;
        public double deepProfileHitRate;
        public int deepProfileSample;
        public int deepFallbackLevel;
        public String fallbackProfileKey;
        public String candidateEvidenceKey;
        public String promotionStatus;
        public String aPlusStatus;
        public String deepReason;
        public String aiJudgeSummary;

        public String preDrawReasonFingerprint;
        public String preDrawReasonText;
        public String reasonValidLevel;
        public double reasonScore;
        public double reasonEdge;
        public double hitSimilarity;
        public double missSimilarity;
        public double contradictionScore;
        public double causalLift;
        public double causalConfidence;
        public int validReasonCount;
        public int falseReasonCount;
        public int forwardReasonSample;
        public double forwardReasonHitRate;
        public double reasonWilsonScore;

        public String k3RoleSummary;
        public int k3MainNumber;
        public int k3SupportNumber;
        public int k3DefenseNumber;
        public double comboRoleDiversity;
        public double comboConflict;

        public int fingerprintCooling;
        public int elite45Passed;
        public String elite45Status;
        public String elite45BlockCode;
        public double elite45Gate;
        public String elite45Summary;

        public ComboCandidate candidate;

        /* V20最终后台常驻与证据成熟审计字段 */
        public String fixCohort;
        public String probabilityPipelineVersion;
        public String auditClusterKey;
        public String learningClusterKey;
        public double coverageRequired;
        public double coverageConfidence;
        public double coverageContribution;
        public String scanRejectCode;
        public String displayRejectCode;
        public double probabilityBeforeTeacher;
        public double probabilityAfterTeacher;
        public double probabilityBeforeRisk;
        public double probabilityAfterRisk;
        /* 73487246：Teacher新鲜度、观察/硬风险分层、受控探针实体字段。 */
        public String teacherAsOfIssue;
        public int teacherIssueLag;
        public int teacherFreshByIssue;
        public long teacherSnapshotCreatedAt;
        public String blockTier;
        public String hardRiskCode;
        public String observeReason;
        public int probeEligible;
        public int shadowProbeEligible;
        public String probeLevel;
        public int shadowSettledCount;
        public int shadowDomainSample;
        public double promotionWilsonLowerBound;
        public String promotionEligibilityReason;

        public ComboCandidate() {
            fixCohort = AppConfig.FIX_COHORT;
            probabilityPipelineVersion = AppConfig.PROBABILITY_PIPELINE_VERSION;
            auditClusterKey = "";
            learningClusterKey = "";
            coverageQuality = 0.0d;
            coverageRequired = AppConfig.COVERAGE_REQUIRED_DEFAULT;
            coverageConfidence = 0.0d;
            coverageContribution = 0.0d;
            scanRejectCode = "";
            displayRejectCode = "";
            probabilityBeforeTeacher = 0.0d;
            probabilityAfterTeacher = 0.0d;
            probabilityBeforeRisk = 0.0d;
            probabilityAfterRisk = 0.0d;

            rank = 0;
            baseIssue = "";
            targetIssue = "";
            createdAt = 0L;
            position = 0;
            n1 = 0;
            n2 = 0;
            n3 = 0;
            k3Mask = 0;
            shapeKey = "";
            stateKey = "";
            stateMode = AppConfig.MODE_CHAOS;
            modelP = 0.0d;
            shapeP = 0.0d;
            comboP = 0.0d;
            recoveryP = 0.0d;
            rawP = 0.0d;
            finalP = 0.0d;
            dynamicGate = AppConfig.DEFAULT_DYNAMIC_GATE;
            edge = 0.0d;
            similarP = 0.0d;
            memoryP = 0.0d;
            calibratedP = 0.0d;
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
            freq100 = 0.0d;
            freq500 = 0.0d;
            freq1000 = 0.0d;
            blockCode = "";
            reasonSummary = "";
            probabilityMode = "";
            backtestMode = AppConfig.BACKTEST_MODE_FAST;
            deepScore = 0.0d;
            deepConfidence = 0.0d;
            deepFeatureP = 0.0d;
            deepFeatureScore = 0.0d;
            deepFeatureConfidence = 0.0d;
            deepFeatureRisk = 0.0d;
            deepFeatureGateAdjust = 0.0d;
            deepFeatureProbAdjust = 0.0d;
            deepFeatureStatus = "";
            deepFeatureSummary = "";
            deepPositionScore = 0.0d;
            deepNumberScore = 0.0d;
            deepCycleScore = 0.0d;
            deepTransitionScore = 0.0d;
            deepStateScore = 0.0d;
            biasScore = 0.0d;
            biasConfidence = 0.0d;
            profileScore = 0.0d;
            fallbackProfileScore = 0.0d;
            runtimeProfileScore = 0.0d;
            stateNumberProfileScore = 0.0d;
            numberBias = 0.0d;
            comboBias = 0.0d;
            teacherWeight = 1.0d;
            teacherMatrixWeight = 1.0d;
            teacherMatrixScore = 0.0d;
            teacherMatrixHitRate = 0.0d;
            teacherMatrixRecentHitRate = 0.0d;
            teacherMatrixConfidence = 0.0d;
            teacherMatrixProbAdjust = 0.0d;
            teacherMatrixGateAdjust = 0.0d;
            teacherMatrixRiskPenalty = 0.0d;
            teacherMatrixPositionScore = 0.0d;
            teacherMatrixNumberScore = 0.0d;
            teacherMatrixComboScore = 0.0d;
            teacherMatrixStateScore = 0.0d;
            teacherMatrixSample = 0;
            teacherMatrixStateSample = 0;
            teacherMatrixComboSample = 0;
            teacherMatrixApplied = 0;
            teacherMatrixStatus = AppConfig.TEACHER_MATRIX_STATUS_NONE;
            teacherMatrixProfileKey = "";
            teacherMatrixComboKey = "";
            teacherMatrixSummary = "";

            teacherWeakNoBetFlag = 0;
            teacherMatrixWeakNoBetFlag = 0;
            teacherMatrixOrdinaryBetBlockFlag = 0;
            teacherMatrixSupportsFinalGate = 0;
            teacherMatrixSupportsBet = 0;
            teacherMatrixRiskFlag = 0;
            teacherMatrixFreezeFlag = 0;
            teacherOrdinaryBetPolicy = "";
            teacherMatrixOrdinaryBetPolicy = "";
            teacherFieldSource = "";
            teacherFieldVersion = "";
            teacherNumberStatus = "";
            teacherStatus = "";
            teacherStatusClass = "";
            teacherEvidenceContribution = 0.0d;
            teacherMatrixEvidenceContribution = 0.0d;
            teacherCounterContribution = 0.0d;
            teacherMatrixCounterContribution = 0.0d;
            teacherSampleReliability = 0.0d;
            teacherRecentValidity = 0.0d;

            v20Status = "";
            dynamicStatus = "";
            dynamicCauseStatus = "";
            v20ActionAdvice = "";
            v20FieldSource = "";
            v20FieldVersion = "";
            v20EmptyFlag = 0;
            v20SupportScore = 0.0d;
            v20RiskScore = 0.0d;
            v20EvidenceContribution = 0.0d;
            v20CounterContribution = 0.0d;
            v20SampleReliability = 0.0d;
            v20RecentValidity = 0.0d;
            similarWindowSample = 0;
            similarWindowHitRate = 0.0d;
            v20WindowStateKey = "";
            windowStateKey = "";
            finalDecisionWindowStateKey = "";
            dynamicWindowStateKey = "";
            probabilityWindowStateKey = "";

            coverageQualityScore = 0.0d;
            thirdNumberQualityScore = 0.0d;
            k3CoverageRisk = 0.0d;
            falseHighPFlag = 0;
            probabilityFalseHighPFlag = 0;
            probabilityFalseHighRisk = 0.0d;
            probabilityPreferredEdgeBand = 0;
            probabilityHighEdgeFlag = 0;
            probabilityPreferredPBand = 0;
            probabilityTeacherWeakOrEmpty = 0;
            probabilityV20UnknownOrEmpty = 0;
            probabilityStrongCauseFlag = 0;
            coverageProbDiscount = 0.0d;
            coverageProbBoost = 0.0d;
            coverageK3HitRate32 = 0.0d;
            coverageK3HitRate64 = 0.0d;
            coverageK3HitRate128 = 0.0d;
            coverageNumberRate1 = 0.0d;
            coverageNumberRate2 = 0.0d;
            coverageNumberRate3 = 0.0d;
            finalPBeforeCoverage = 0.0d;
            finalPAfterCoverage = 0.0d;
            coverageAdjustedP = 0.0d;
            probabilityCoverageSummary = "";
            coverageQualitySummary = "";
            probabilityCoverageActionAdvice = "";
            probabilityActionAdvice = "";
            probabilityFieldVersion = "";

            dynamicGateQualityStatus = "";
            dynamicGateFalseHighPFlag = 0;
            dynamicGatePreferredEdgeBand = 0;
            dynamicGateTeacherV20WeakFlag = 0;
            dynamicGateEdgePolicy = "";
            dynamicGateCoverageRisk = 0.0d;
            dynamicGateCoverageQualityScore = 0.0d;
            dynamicGateThirdNumberQuality = 0.0d;
            dynamicGateVirtualEdgeRisk = 0.0d;
            dynamicGateActionAdvice = "";
            dynamicGateFieldVersion = "";
            dynamicGateVersion = "";

            finalDecisionVersion = "";
            finalDecisionEdgePolicy = "";
            finalDecisionTeacherPolicy = "";
            finalDecisionV20Policy = "";
            finalDecisionBetDomain = "";
            finalDecisionRejectCode = "";
            finalDecisionShadowReason = "";
            finalDecisionStrongOverrideTrace = "";
            finalDecisionCoverageRisk = 0.0d;
            finalDecisionCoverageQuality = 0.0d;
            finalDecisionThirdNumberQuality = 0.0d;
            finalDecisionFalseHighPFlag = 0;
            initDynamicCausalBridgeFields(this);
            initMainChainBridgeFields(this);
            stateConfidence = 0.0d;
            auditPenalty = 0.0d;
            auditBoost = 0.0d;
            promotionScore = 0.0d;
            aPlusScore = 0.0d;
            evidenceScore = 0.0d;
            evidenceRisk = 0.0d;
            aiJudgeScore = 0.0d;
            aiJudgeConfidence = 0.0d;
            conflictLevel = 0.0d;
            weakestNumberScore = 0.0d;
            balanceScore = 0.0d;
            rankScore = 0.0d;
            finalRankScore = 0.0d;
            stateSample = 0;
            cacheSample = 0;
            recentSample = 0;
            recentHitRate = 0.0d;
            deepProfileKey = "";
            deepProfileHitRate = 0.0d;
            deepProfileSample = 0;
            deepFallbackLevel = 0;
            fallbackProfileKey = "";
            candidateEvidenceKey = "";
            promotionStatus = AppConfig.PROMOTION_CANDIDATE;
            aPlusStatus = AppConfig.A_PLUS_NONE;
            deepReason = "";
            aiJudgeSummary = "";

            preDrawReasonFingerprint = "";
            preDrawReasonText = "";
            reasonValidLevel = REASON_LEVEL_C_EXPLAIN_ONLY;
            reasonScore = 0.0d;
            reasonEdge = 0.0d;
            hitSimilarity = 0.0d;
            missSimilarity = 0.0d;
            contradictionScore = 0.0d;
            causalLift = 0.0d;
            causalConfidence = 0.0d;
            validReasonCount = 0;
            falseReasonCount = 0;
            forwardReasonSample = 0;
            forwardReasonHitRate = 0.0d;
            reasonWilsonScore = 0.0d;
            k3RoleSummary = "";
            k3MainNumber = 0;
            k3SupportNumber = 0;
            k3DefenseNumber = 0;
            comboRoleDiversity = 0.0d;
            comboConflict = 0.0d;
            fingerprintCooling = 0;
            elite45Passed = 0;
            elite45Status = BLOCK_ELITE45_NOT_PASS;
            elite45BlockCode = BLOCK_ELITE45_NOT_PASS;
            elite45Gate = ELITE45_GATE_DEFAULT;
            elite45Summary = "";
            candidate = this;
        }

        public void setNumbers(int a, int b, int c) {
            n1 = a;
            n2 = b;
            n3 = c;
            k3Mask = buildK3Mask(a, b, c);
        }

        public boolean isValidK3() {
            return AppConfig.isValidPosition(position) && isValidK3Mask(k3Mask);
        }

        public boolean isBlocked() {
            return blockCode != null && blockCode.length() > 0;
        }

        public boolean isStrongShape() {
            if (AppConfig.MODE_BROKEN.equals(stateMode)
                    || AppConfig.MODE_REVERSING.equals(stateMode)) return false;
            return trendLife >= 70.0d
                    && driftRisk <= AppConfig.DRIFT_RISK_MAX
                    && breakDepth <= AppConfig.BREAK_DEPTH_MAX
                    && recoveryScore >= AppConfig.RECOVERY_SCORE_MIN;
        }

        public void applyTeacherMatrix(
                TeacherMatrixProfile profile, TeacherMatrixComboProfile comboProfile) {
            if (profile != null) {
                profile.recalc();
                teacherMatrixSample = profile.sampleCount;
                teacherMatrixHitRate = profile.hitRate;
                teacherMatrixRecentHitRate = profile.recentHitRate;
                teacherMatrixConfidence = profile.confidence;
                teacherMatrixProbAdjust = profile.probAdjust;
                teacherMatrixGateAdjust = profile.gateAdjust;
                teacherMatrixRiskPenalty = profile.riskPenalty;
                teacherMatrixPositionScore = profile.positionScore;
                teacherMatrixNumberScore = profile.numberScore;
                teacherMatrixStateScore = profile.stateScore;
                teacherMatrixScore = profile.numberScore;
                teacherMatrixStatus = profile.status;
                teacherMatrixProfileKey = profile.profileKey;
                teacherMatrixSummary = appendReason(teacherMatrixSummary, profile.summaryText);
                teacherMatrixApplied = profile.canApplyRealtime() ? 1 : 0;
            }

            if (comboProfile != null) {
                comboProfile.recalc();
                teacherMatrixComboSample = comboProfile.sampleCount;
                teacherMatrixComboScore = comboProfile.comboScore;
                teacherMatrixComboKey = comboProfile.comboKey;
                teacherMatrixSummary = appendReason(teacherMatrixSummary, comboProfile.summaryText);

                if (teacherMatrixScore <= 0.0d) {
                    teacherMatrixScore = comboProfile.comboScore;
                } else {
                    teacherMatrixScore =
                            AppConfig.clampProbability(
                                    teacherMatrixScore * 0.60d + comboProfile.comboScore * 0.40d);
                }

                if (comboProfile.status != null
                        && comboProfile.status.length() > 0
                        && (teacherMatrixStatus == null
                                || teacherMatrixStatus.length() == 0
                                || AppConfig.TEACHER_MATRIX_STATUS_NONE.equals(
                                        teacherMatrixStatus))) {
                    teacherMatrixStatus = comboProfile.status;
                }

                if (comboProfile.canApplyRealtime()) teacherMatrixApplied = 1;
            }
        }

        public void applyNeedSupply(NeedSupplyMatchResult r) {
            if (r == null) return;
            r.recalc();
            needSupplyMatchKey = r.matchKey;
            needSupplyMatchScore = r.matchScore;
            needSupplyThirdQuality = r.thirdQuality;
            needSupplyRoleDiversity = r.roleDiversity;
            needSupplyRisk = r.supplyRisk;
            needSupplySummary = r.summary;
            k3RoleMain = r.mainRole;
            k3RoleAssist = r.assistRole;
            k3RoleDefense = r.defenseRole;
            k3RoleMainNumber = r.mainRoleNumber;
            k3RoleAssistNumber = r.assistRoleNumber;
            k3RoleDefenseNumber = r.defenseRoleNumber;
            k3RoleStructure = r.whyMatch;
            thirdNumberQualityScore = Math.max(thirdNumberQualityScore, r.thirdQuality);
            comboRoleDiversity = Math.max(comboRoleDiversity, r.roleDiversity);
        }

        public void applyClusterMatch(ClusterMatchResult r) {
            if (r == null) return;
            dynamicClusterId = r.clusterId;
            dynamicClusterStatus = AppConfig.normalizeDynamicClusterStatus(r.clusterStatus);
            dynamicClusterPrototypeKey = r.prototypeKey;
            dynamicClusterDistance = r.distance;
            dynamicClusterSample = r.sampleCount;
            dynamicClusterRecentSample = r.recentSample;
            dynamicClusterHitRate = r.hitRate;
            dynamicClusterRecentHitRate = r.recentHitRate;
            dynamicClusterConfidence = r.confidence;
            dynamicClusterCounterScore = r.counterScore;
            dynamicClusterMaxLose = r.maxLoseStreak;
            dynamicClusterReason = r.reason;
            dynamicClusterVersion = V20_MODELS_DYNAMIC_CAUSAL_WINDOW_VERSION;
        }

        public void applyCounterfactualReason(CounterfactualReasonResult r) {
            if (r == null) return;
            r.recalc();
            counterfactualReasonKey = r.reasonKey;
            counterfactualRank = r.k3Rank;
            counterfactualRankScore = r.rankScore;
            counterfactualWhyNotRisk = r.whyNotRisk;
            counterfactualWhyNot = appendReason(r.whyNotOtherPosition, r.whyNotOtherNumbers);
            counterfactualSummary = r.summary;
            causalConfidence = Math.max(causalConfidence, r.causeScore);
            conflictLevel = Math.max(conflictLevel, r.counterScore);
        }

        public void applyAdaptiveJudge(AdaptiveJudgeResult r) {
            if (r == null) return;
            r.recalc();
            adaptiveJudgeCandidateCount = r.candidateCount;
            adaptiveJudgeStatus = r.status;
            adaptiveJudgeAction = r.action;
            adaptiveJudgeDomain = r.domain;
            adaptiveClusterHitRate = r.clusterHitRate;
            adaptiveClusterRecentHitRate = r.clusterRecentHitRate;
            adaptiveClusterSample = r.clusterSample;
            adaptiveClusterConfidence = r.clusterConfidence;
            adaptiveCauseScore = r.causeScore;
            adaptiveCounterScore = r.counterScore;
            adaptivePromoteScore = r.promoteScore;
            adaptiveFreezeScore = r.freezeScore;
            adaptiveJudgeReason = r.reason;
            adaptiveJudgeVersion = r.version;
            if (dynamicClusterId == null || dynamicClusterId.length() == 0)
                dynamicClusterId = r.clusterId;
            if (dynamicClusterStatus == null
                    || dynamicClusterStatus.length() == 0
                    || AppConfig.CLUSTER_STATUS_UNKNOWN.equals(dynamicClusterStatus)) {
                dynamicClusterStatus = r.clusterStatus;
            }
        }

        public double sortScore() {
            double score = finalP;
            score += edge * 0.60d;
            score += championGap * 0.20d;
            score += comboGap * 0.15d;
            score += (trendLife / 100.0d) * 0.05d;
            score -= (driftRisk / 100.0d) * 0.08d;
            score -= (breakDepth / 100.0d) * 0.06d;
            score += deepScore * 0.04d;
            score += deepFeatureP * 0.04d;
            score += evidenceScore * 0.04d;
            score += aiJudgeScore * 0.035d;
            score += profileScore * 0.025d;
            score += runtimeProfileScore * 0.025d;
            score += stateNumberProfileScore * 0.025d;
            score += teacherMatrixScore * 0.035d;
            score += teacherMatrixConfidence * 0.020d;
            score += reasonScore * 0.035d;
            score += reasonEdge * 0.080d;
            score += hitSimilarity * 0.030d;
            score += causalConfidence * 0.020d;
            score += comboRoleDiversity * 0.020d;
            score -= missSimilarity * 0.050d;
            score -= contradictionScore * 0.060d;
            score -= comboConflict * 0.050d;
            score += numberBias * 0.025d;
            score += comboBias * 0.025d;
            score += stateConfidence * 0.020d;
            score += fallbackProfileScore * 0.020d;
            score += biasScore * 0.020d;
            score += biasConfidence * 0.015d;
            score += aPlusScore * 0.020d;
            score += promotionScore * 0.020d;
            score += auditBoost * 0.020d;
            score += finalRankScore * 0.030d;
            score += needSupplyMatchScore * 0.030d;
            score += dynamicClusterHitRate * 0.020d;
            score += adaptivePromoteScore * 0.020d;
            score -= adaptiveFreezeScore * 0.050d;
            score -= dynamicClusterCounterScore * 0.035d;
            score -= evidenceRisk * 0.050d;
            score -= auditPenalty * 0.080d;
            score -= conflictLevel * 0.040d;
            score -= teacherMatrixRiskPenalty * 0.100d;

            if (elite45Passed == 1) score += 0.030d;
            if (fingerprintCooling == 1) score -= 0.060d;
            if (falseReasonCount > 0) score -= 0.050d;

            if (AppConfig.TEACHER_MATRIX_STATUS_STRONG.equals(teacherMatrixStatus)) score += 0.008d;
            else if (AppConfig.TEACHER_MATRIX_STATUS_READY.equals(teacherMatrixStatus))
                score += 0.004d;
            else if (AppConfig.TEACHER_MATRIX_STATUS_RISK.equals(teacherMatrixStatus))
                score -= 0.030d;

            if (blockCode != null
                    && blockCode.length() > 0
                    && !AppConfig.BLOCK_LOW_PROBABILITY.equals(blockCode)
                    && !AppConfig.BLOCK_COMBO_WEAK.equals(blockCode)) {
                score -= 0.03d;
            }
            return score;
        }

        public String upstreamEvidenceSummary() {
            StringBuilder sb = new StringBuilder();
            sb.append("Teacher=").append(teacherMatrixStatus == null ? "" : teacherMatrixStatus);
            sb.append(" Policy=")
                    .append(teacherOrdinaryBetPolicy == null ? "" : teacherOrdinaryBetPolicy);
            sb.append(" V20=").append(v20Status == null ? "" : v20Status);
            sb.append(" Window=").append(windowStateKey == null ? "" : windowStateKey);
            sb.append(" Cover=").append(coverageQualityScore).append("/").append(k3CoverageRisk);
            sb.append(" Third=").append(thirdNumberQualityScore);
            sb.append(" Reject=")
                    .append(finalDecisionRejectCode == null ? "" : finalDecisionRejectCode);
            sb.append(" Cluster=")
                    .append(dynamicClusterId == null ? "" : dynamicClusterId)
                    .append("/")
                    .append(dynamicClusterStatus == null ? "" : dynamicClusterStatus);
            sb.append(" NeedSupply=")
                    .append(needSupplyMatchScore)
                    .append(" ThirdQ=")
                    .append(needSupplyThirdQuality);
            sb.append(" Judge=").append(adaptiveJudgeAction == null ? "" : adaptiveJudgeAction);
            return sb.toString();
        }

        public String displayText() {
            return UiFormatter.positionAndNumbers(position, n1, n2, n3);
        }

        public ComboCandidate copy() {
            ComboCandidate c = new ComboCandidate();
            c.v46EventProbability = v46EventProbability;
            c.v46ActionLcb = v46ActionLcb;
            c.v46ProbabilityUncertainty = v46ProbabilityUncertainty;
            c.v46ExpectedValue = v46ExpectedValue;
            c.v46ConditionalAdjustment = v46ConditionalAdjustment;
            c.v46ConditionalSample = v46ConditionalSample;
            c.v46FinalRank = v46FinalRank;
            c.v46FormalCandidateCount = v46FormalCandidateCount;
            c.v46ProbabilityTrace = v46ProbabilityTrace;
            c.v46NumberDistributionSummary = v46NumberDistributionSummary;
            c.v46ConditionalCalibrationSummary = v46ConditionalCalibrationSummary;
            c.v46RootCauseCode = v46RootCauseCode;
            c.v46RootCauseOwner = v46RootCauseOwner;
            c.v46ContractHash = v46ContractHash;
            c.v46Version = v46Version;
            c.fixCohort = fixCohort;
            c.probabilityPipelineVersion = probabilityPipelineVersion;
            c.auditClusterKey = auditClusterKey;
            c.learningClusterKey = learningClusterKey;
            c.coverageQuality = coverageQuality;
            c.coverageRequired = coverageRequired;
            c.coverageConfidence = coverageConfidence;
            c.coverageContribution = coverageContribution;
            c.scanRejectCode = scanRejectCode;
            c.displayRejectCode = displayRejectCode;
            c.probabilityBeforeTeacher = probabilityBeforeTeacher;
            c.probabilityAfterTeacher = probabilityAfterTeacher;
            c.probabilityBeforeRisk = probabilityBeforeRisk;
            c.probabilityAfterRisk = probabilityAfterRisk;
            c.rank = rank;
            c.baseIssue = baseIssue;
            c.targetIssue = targetIssue;
            c.createdAt = createdAt;
            c.position = position;
            c.n1 = n1;
            c.n2 = n2;
            c.n3 = n3;
            c.k3Mask = k3Mask;
            c.shapeKey = shapeKey;
            c.stateKey = stateKey;
            c.stateMode = stateMode;
            c.modelP = modelP;
            c.shapeP = shapeP;
            c.comboP = comboP;
            c.recoveryP = recoveryP;
            c.rawP = rawP;
            c.finalP = finalP;
            c.dynamicGate = dynamicGate;
            c.edge = edge;
            c.similarP = similarP;
            c.memoryP = memoryP;
            c.calibratedP = calibratedP;
            c.calibrationError = calibrationError;
            c.similarHitRate = similarHitRate;
            c.comboHitRate = comboHitRate;
            c.effectiveSample = effectiveSample;
            c.comboSample = comboSample;
            c.trendLife = trendLife;
            c.driftRisk = driftRisk;
            c.breakDepth = breakDepth;
            c.recoveryScore = recoveryScore;
            c.championGap = championGap;
            c.comboGap = comboGap;
            c.hotReverseRisk = hotReverseRisk;
            c.falseStrongRisk = falseStrongRisk;
            c.freq100 = freq100;
            c.freq500 = freq500;
            c.freq1000 = freq1000;
            c.blockCode = blockCode;
            c.reasonSummary = reasonSummary;
            c.probabilityMode = probabilityMode;
            c.backtestMode = backtestMode;
            c.deepScore = deepScore;
            c.deepConfidence = deepConfidence;
            c.deepFeatureP = deepFeatureP;
            c.deepFeatureScore = deepFeatureScore;
            c.deepFeatureConfidence = deepFeatureConfidence;
            c.deepFeatureRisk = deepFeatureRisk;
            c.deepFeatureGateAdjust = deepFeatureGateAdjust;
            c.deepFeatureProbAdjust = deepFeatureProbAdjust;
            c.deepFeatureStatus = deepFeatureStatus;
            c.deepFeatureSummary = deepFeatureSummary;
            c.deepPositionScore = deepPositionScore;
            c.deepNumberScore = deepNumberScore;
            c.deepCycleScore = deepCycleScore;
            c.deepTransitionScore = deepTransitionScore;
            c.deepStateScore = deepStateScore;
            c.biasScore = biasScore;
            c.biasConfidence = biasConfidence;
            c.profileScore = profileScore;
            c.fallbackProfileScore = fallbackProfileScore;
            c.runtimeProfileScore = runtimeProfileScore;
            c.stateNumberProfileScore = stateNumberProfileScore;
            c.numberBias = numberBias;
            c.comboBias = comboBias;
            c.teacherWeight = teacherWeight;
            c.teacherNumberSample = teacherNumberSample;
            c.teacherNumberHistorySample = teacherNumberHistorySample;
            c.teacherDomainShadowSample = teacherDomainShadowSample;
            c.teacherDomainRealSample = teacherDomainRealSample;
            c.teacherNumberHitRate = teacherNumberHitRate;
            c.teacherNumberConfidence = teacherNumberConfidence;
            c.teacherNumberScore = teacherNumberScore;
            c.teacherNumberRisk = teacherNumberRisk;
            c.teacherNumberSummary = teacherNumberSummary;
            c.teacherApplyError = teacherApplyError;
            c.preDecisionDomainVersion = preDecisionDomainVersion;
            c.teacherRuntimeApplyVersion = teacherRuntimeApplyVersion;
            c.teacherMatrixWeight = teacherMatrixWeight;
            c.teacherMatrixScore = teacherMatrixScore;
            c.teacherMatrixHitRate = teacherMatrixHitRate;
            c.teacherMatrixRecentHitRate = teacherMatrixRecentHitRate;
            c.teacherMatrixConfidence = teacherMatrixConfidence;
            c.teacherMatrixProbAdjust = teacherMatrixProbAdjust;
            c.teacherMatrixGateAdjust = teacherMatrixGateAdjust;
            c.teacherMatrixRiskPenalty = teacherMatrixRiskPenalty;
            c.teacherMatrixPositionScore = teacherMatrixPositionScore;
            c.teacherMatrixNumberScore = teacherMatrixNumberScore;
            c.teacherMatrixComboScore = teacherMatrixComboScore;
            c.teacherMatrixStateScore = teacherMatrixStateScore;
            c.teacherMatrixSample = teacherMatrixSample;
            c.teacherMatrixStateSample = teacherMatrixStateSample;
            c.teacherMatrixComboSample = teacherMatrixComboSample;
            c.teacherMatrixApplied = teacherMatrixApplied;
            c.teacherMatrixStatus = teacherMatrixStatus;
            c.teacherMatrixProfileKey = teacherMatrixProfileKey;
            c.teacherMatrixComboKey = teacherMatrixComboKey;
            c.teacherMatrixSummary = teacherMatrixSummary;
            c.teacherWeakNoBetFlag = teacherWeakNoBetFlag;
            c.teacherMatrixWeakNoBetFlag = teacherMatrixWeakNoBetFlag;
            c.teacherMatrixOrdinaryBetBlockFlag = teacherMatrixOrdinaryBetBlockFlag;
            c.teacherMatrixSupportsFinalGate = teacherMatrixSupportsFinalGate;
            c.teacherMatrixSupportsBet = teacherMatrixSupportsBet;
            c.teacherMatrixRiskFlag = teacherMatrixRiskFlag;
            c.teacherMatrixFreezeFlag = teacherMatrixFreezeFlag;
            c.teacherOrdinaryBetPolicy = teacherOrdinaryBetPolicy;
            c.teacherMatrixOrdinaryBetPolicy = teacherMatrixOrdinaryBetPolicy;
            c.teacherFieldSource = teacherFieldSource;
            c.teacherFieldVersion = teacherFieldVersion;
            c.teacherNumberStatus = teacherNumberStatus;
            c.teacherStatus = teacherStatus;
            c.teacherStatusClass = teacherStatusClass;
            c.teacherEvidenceContribution = teacherEvidenceContribution;
            c.teacherMatrixEvidenceContribution = teacherMatrixEvidenceContribution;
            c.teacherCounterContribution = teacherCounterContribution;
            c.teacherMatrixCounterContribution = teacherMatrixCounterContribution;
            c.teacherSampleReliability = teacherSampleReliability;
            c.teacherRecentValidity = teacherRecentValidity;
            c.v20Status = v20Status;
            c.dynamicStatus = dynamicStatus;
            c.dynamicCauseStatus = dynamicCauseStatus;
            c.v20ActionAdvice = v20ActionAdvice;
            c.v20FieldSource = v20FieldSource;
            c.v20FieldVersion = v20FieldVersion;
            c.v20EmptyFlag = v20EmptyFlag;
            c.v20SupportScore = v20SupportScore;
            c.v20RiskScore = v20RiskScore;
            c.v20EvidenceContribution = v20EvidenceContribution;
            c.v20CounterContribution = v20CounterContribution;
            c.v20SampleReliability = v20SampleReliability;
            c.v20RecentValidity = v20RecentValidity;
            c.similarWindowSample = similarWindowSample;
            c.similarWindowHitRate = similarWindowHitRate;
            c.v20WindowStateKey = v20WindowStateKey;
            c.windowStateKey = windowStateKey;
            c.finalDecisionWindowStateKey = finalDecisionWindowStateKey;
            c.dynamicWindowStateKey = dynamicWindowStateKey;
            c.probabilityWindowStateKey = probabilityWindowStateKey;
            c.coverageQualityScore = coverageQualityScore;
            c.thirdNumberQualityScore = thirdNumberQualityScore;
            c.k3CoverageRisk = k3CoverageRisk;
            c.falseHighPFlag = falseHighPFlag;
            c.probabilityFalseHighPFlag = probabilityFalseHighPFlag;
            c.probabilityFalseHighRisk = probabilityFalseHighRisk;
            c.probabilityPreferredEdgeBand = probabilityPreferredEdgeBand;
            c.probabilityHighEdgeFlag = probabilityHighEdgeFlag;
            c.probabilityPreferredPBand = probabilityPreferredPBand;
            c.probabilityTeacherWeakOrEmpty = probabilityTeacherWeakOrEmpty;
            c.probabilityV20UnknownOrEmpty = probabilityV20UnknownOrEmpty;
            c.probabilityStrongCauseFlag = probabilityStrongCauseFlag;
            c.coverageProbDiscount = coverageProbDiscount;
            c.coverageProbBoost = coverageProbBoost;
            c.coverageK3HitRate32 = coverageK3HitRate32;
            c.coverageK3HitRate64 = coverageK3HitRate64;
            c.coverageK3HitRate128 = coverageK3HitRate128;
            c.coverageNumberRate1 = coverageNumberRate1;
            c.coverageNumberRate2 = coverageNumberRate2;
            c.coverageNumberRate3 = coverageNumberRate3;
            c.finalPBeforeCoverage = finalPBeforeCoverage;
            c.finalPAfterCoverage = finalPAfterCoverage;
            c.coverageAdjustedP = coverageAdjustedP;
            c.probabilityCoverageSummary = probabilityCoverageSummary;
            c.coverageQualitySummary = coverageQualitySummary;
            c.probabilityCoverageActionAdvice = probabilityCoverageActionAdvice;
            c.probabilityActionAdvice = probabilityActionAdvice;
            c.probabilityFieldVersion = probabilityFieldVersion;
            c.dynamicGateQualityStatus = dynamicGateQualityStatus;
            c.dynamicGateFalseHighPFlag = dynamicGateFalseHighPFlag;
            c.dynamicGatePreferredEdgeBand = dynamicGatePreferredEdgeBand;
            c.dynamicGateTeacherV20WeakFlag = dynamicGateTeacherV20WeakFlag;
            c.dynamicGateEdgePolicy = dynamicGateEdgePolicy;
            c.dynamicGateCoverageRisk = dynamicGateCoverageRisk;
            c.dynamicGateCoverageQualityScore = dynamicGateCoverageQualityScore;
            c.dynamicGateThirdNumberQuality = dynamicGateThirdNumberQuality;
            c.dynamicGateVirtualEdgeRisk = dynamicGateVirtualEdgeRisk;
            c.dynamicGateActionAdvice = dynamicGateActionAdvice;
            c.dynamicGateFieldVersion = dynamicGateFieldVersion;
            c.dynamicGateVersion = dynamicGateVersion;
            c.finalDecisionVersion = finalDecisionVersion;
            c.finalDecisionEdgePolicy = finalDecisionEdgePolicy;
            c.finalDecisionTeacherPolicy = finalDecisionTeacherPolicy;
            c.finalDecisionV20Policy = finalDecisionV20Policy;
            c.finalDecisionBetDomain = finalDecisionBetDomain;
            c.finalDecisionRejectCode = finalDecisionRejectCode;
            c.finalDecisionShadowReason = finalDecisionShadowReason;
            c.finalDecisionStrongOverrideTrace = finalDecisionStrongOverrideTrace;
            c.finalDecisionCoverageRisk = finalDecisionCoverageRisk;
            c.finalDecisionCoverageQuality = finalDecisionCoverageQuality;
            c.finalDecisionThirdNumberQuality = finalDecisionThirdNumberQuality;
            c.finalDecisionFalseHighPFlag = finalDecisionFalseHighPFlag;
            c.stateConfidence = stateConfidence;
            c.auditPenalty = auditPenalty;
            c.auditBoost = auditBoost;
            c.promotionScore = promotionScore;
            c.aPlusScore = aPlusScore;
            c.evidenceScore = evidenceScore;
            c.evidenceRisk = evidenceRisk;
            c.aiJudgeScore = aiJudgeScore;
            c.aiJudgeConfidence = aiJudgeConfidence;
            c.conflictLevel = conflictLevel;
            c.weakestNumberScore = weakestNumberScore;
            c.balanceScore = balanceScore;
            c.rankScore = rankScore;
            c.finalRankScore = finalRankScore;
            c.stateSample = stateSample;
            c.cacheSample = cacheSample;
            c.recentSample = recentSample;
            c.recentHitRate = recentHitRate;
            c.deepProfileKey = deepProfileKey;
            c.deepProfileHitRate = deepProfileHitRate;
            c.deepProfileSample = deepProfileSample;
            c.deepFallbackLevel = deepFallbackLevel;
            c.fallbackProfileKey = fallbackProfileKey;
            c.candidateEvidenceKey = candidateEvidenceKey;
            c.promotionStatus = promotionStatus;
            c.aPlusStatus = aPlusStatus;
            c.deepReason = deepReason;
            c.aiJudgeSummary = aiJudgeSummary;

            c.preDrawReasonFingerprint = preDrawReasonFingerprint;
            c.preDrawReasonText = preDrawReasonText;
            c.reasonValidLevel = reasonValidLevel;
            c.reasonScore = reasonScore;
            c.reasonEdge = reasonEdge;
            c.hitSimilarity = hitSimilarity;
            c.missSimilarity = missSimilarity;
            c.contradictionScore = contradictionScore;
            c.causalLift = causalLift;
            c.causalConfidence = causalConfidence;
            c.validReasonCount = validReasonCount;
            c.falseReasonCount = falseReasonCount;
            c.forwardReasonSample = forwardReasonSample;
            c.forwardReasonHitRate = forwardReasonHitRate;
            c.reasonWilsonScore = reasonWilsonScore;
            c.k3RoleSummary = k3RoleSummary;
            c.k3MainNumber = k3MainNumber;
            c.k3SupportNumber = k3SupportNumber;
            c.k3DefenseNumber = k3DefenseNumber;
            c.comboRoleDiversity = comboRoleDiversity;
            c.comboConflict = comboConflict;
            c.fingerprintCooling = fingerprintCooling;
            c.elite45Passed = elite45Passed;
            c.elite45Status = elite45Status;
            c.elite45BlockCode = elite45BlockCode;
            c.elite45Gate = elite45Gate;
            c.elite45Summary = elite45Summary;
            copyDynamicCausalBridgeFields(this, c);
            copyMainChainBridgeFields(this, c);

            c.candidate = c;
            return c;
        }

        public Decision toDecision(String decisionValue) {
            Decision d = new Decision();
            d.v46EventProbability = v46EventProbability;
            d.v46ActionLcb = v46ActionLcb;
            d.v46ProbabilityUncertainty = v46ProbabilityUncertainty;
            d.v46ExpectedValue = v46ExpectedValue;
            d.v46ConditionalAdjustment = v46ConditionalAdjustment;
            d.v46ConditionalSample = v46ConditionalSample;
            d.v46FinalRank = v46FinalRank;
            d.v46FormalCandidateCount = v46FormalCandidateCount;
            d.v46ProbabilityTrace = v46ProbabilityTrace;
            d.v46NumberDistributionSummary = v46NumberDistributionSummary;
            d.v46ConditionalCalibrationSummary = v46ConditionalCalibrationSummary;
            d.v46RootCauseCode = v46RootCauseCode;
            d.v46RootCauseOwner = v46RootCauseOwner;
            d.v46ContractHash = v46ContractHash;
            d.v46Version = v46Version;
            d.baseIssue = baseIssue == null ? "" : baseIssue;
            d.targetIssue = targetIssue == null ? "" : targetIssue;
            d.createdAt = System.currentTimeMillis();
            d.decision =
                    decisionValue == null
                            ? AppConfig.DECISION_HOLD
                            : AppConfig.normalizeDecision(decisionValue);
            d.action = d.decision;
            d.statScope =
                    AppConfig.DECISION_BET.equals(d.decision)
                            ? AppConfig.STAT_SCOPE_REAL_TRADE
                            : (AppConfig.DECISION_SHADOW.equals(d.decision)
                                    ? AppConfig.STAT_SCOPE_SHADOW
                                    : AppConfig.STAT_SCOPE_HOLD);
            d.sourceType = SOURCE_TYPE_REALTIME;
            d.isRealtimeReal = AppConfig.DECISION_BET.equals(d.decision) ? 1 : 0;
            d.position = position;
            d.setNumbers(n1, n2, n3);
            d.rawP = rawP;
            d.calibratedP = calibratedP;
            d.finalP = finalP;
            d.dynamicGate = dynamicGate;
            d.edge = edge;
            d.modelP = modelP;
            d.shapeP = shapeP;
            d.comboP = comboP;
            d.recoveryP = recoveryP;
            d.similarP = similarP;
            d.memoryP = memoryP;
            d.calibrationError = calibrationError;
            d.similarHitRate = similarHitRate;
            d.comboHitRate = comboHitRate;
            d.effectiveSample = effectiveSample;
            d.comboSample = comboSample;
            d.trendLife = trendLife;
            d.driftRisk = driftRisk;
            d.breakDepth = breakDepth;
            d.recoveryScore = recoveryScore;
            d.championGap = championGap;
            d.comboGap = comboGap;
            d.hotReverseRisk = hotReverseRisk;
            d.falseStrongRisk = falseStrongRisk;
            d.freq100 = freq100;
            d.freq500 = freq500;
            d.freq1000 = freq1000;
            d.stateMode = stateMode == null ? AppConfig.MODE_CHAOS : stateMode;
            d.shapeKey = shapeKey == null ? "" : shapeKey;
            d.stateKey = stateKey == null ? "" : stateKey;
            d.blockCode = blockCode == null ? "" : blockCode;
            d.reasonSummary = reasonSummary == null ? "" : reasonSummary;
            d.reasonText = d.reasonSummary;
            d.backtestMode = backtestMode == null ? AppConfig.BACKTEST_MODE_FAST : backtestMode;
            d.probabilityMode = probabilityMode == null ? "" : probabilityMode;
            d.deepScore = deepScore;
            d.deepConfidence = deepConfidence;
            d.deepFeatureP = deepFeatureP;
            d.deepPositionScore = deepPositionScore;
            d.deepNumberScore = deepNumberScore;
            d.deepCycleScore = deepCycleScore;
            d.deepTransitionScore = deepTransitionScore;
            d.deepStateScore = deepStateScore;
            d.biasScore = biasScore;
            d.biasConfidence = biasConfidence;
            d.profileScore = profileScore;
            d.fallbackProfileScore = fallbackProfileScore;
            d.runtimeProfileScore = runtimeProfileScore;
            d.stateNumberProfileScore = stateNumberProfileScore;
            d.teacherWeight = teacherWeight;
            d.stateConfidence = stateConfidence;
            d.auditPenalty = auditPenalty;
            d.auditBoost = auditBoost;
            d.aiJudgeScore = aiJudgeScore;
            d.aiJudgeConfidence = aiJudgeConfidence;
            d.conflictLevel = conflictLevel;
            d.evidenceScore = evidenceScore;
            d.evidenceRisk = evidenceRisk;
            d.aPlusScore = aPlusScore;
            d.promotionScore = promotionScore;
            d.weakestNumberScore = weakestNumberScore;
            d.balanceScore = balanceScore;
            d.stateSample = stateSample;
            d.cacheSample = cacheSample;
            d.recentSample = recentSample;
            d.recentHitRate = recentHitRate;
            d.deepProfileKey = deepProfileKey == null ? "" : deepProfileKey;
            d.deepProfileHitRate = deepProfileHitRate;
            d.deepProfileSample = deepProfileSample;
            d.deepFallbackLevel = deepFallbackLevel;
            d.fallbackProfileKey = fallbackProfileKey == null ? "" : fallbackProfileKey;
            d.promotionStatus =
                    promotionStatus == null ? AppConfig.PROMOTION_CANDIDATE : promotionStatus;
            d.aPlusStatus = aPlusStatus == null ? AppConfig.A_PLUS_NONE : aPlusStatus;
            d.candidateEvidenceKey = candidateEvidenceKey == null ? "" : candidateEvidenceKey;
            d.deepReason = deepReason == null ? "" : deepReason;
            d.aiJudgeSummary = aiJudgeSummary == null ? "" : aiJudgeSummary;
            d.teacherMatrixSample = teacherMatrixSample;
            d.teacherMatrixStateSample = teacherMatrixStateSample;
            d.teacherMatrixComboSample = teacherMatrixComboSample;
            d.teacherMatrixHitRate = teacherMatrixHitRate;
            d.teacherMatrixRecentHitRate = teacherMatrixRecentHitRate;
            d.teacherMatrixConfidence = teacherMatrixConfidence;
            d.teacherMatrixProbAdjust = teacherMatrixProbAdjust;
            d.teacherMatrixGateAdjust = teacherMatrixGateAdjust;
            d.teacherMatrixRiskPenalty = teacherMatrixRiskPenalty;
            d.teacherMatrixScore =
                    teacherMatrixScore > 0.0d
                            ? teacherMatrixScore
                            : buildTeacherMatrixScore(
                                    teacherMatrixNumberScore,
                                    teacherMatrixPositionScore,
                                    teacherMatrixStateScore,
                                    teacherMatrixComboScore);
            d.teacherMatrixPositionScore = teacherMatrixPositionScore;
            d.teacherMatrixNumberScore = teacherMatrixNumberScore;
            d.teacherMatrixComboScore = teacherMatrixComboScore;
            d.teacherMatrixStateScore = teacherMatrixStateScore;
            d.teacherMatrixApplied = teacherMatrixApplied;
            d.teacherMatrixProfileKey = teacherMatrixProfileKey;
            d.teacherMatrixComboKey = teacherMatrixComboKey;
            d.teacherMatrixSummary = teacherMatrixSummary;
            d.teacherMatrixStatus =
                    teacherMatrixStatus == null || teacherMatrixStatus.length() == 0
                            ? AppConfig.TEACHER_MATRIX_STATUS_NONE
                            : teacherMatrixStatus;
            d.teacherWeakNoBetFlag = teacherWeakNoBetFlag;
            d.teacherMatrixWeakNoBetFlag = teacherMatrixWeakNoBetFlag;
            d.teacherMatrixOrdinaryBetBlockFlag = teacherMatrixOrdinaryBetBlockFlag;
            d.teacherMatrixSupportsFinalGate = teacherMatrixSupportsFinalGate;
            d.teacherMatrixSupportsBet = teacherMatrixSupportsBet;
            d.teacherMatrixRiskFlag = teacherMatrixRiskFlag;
            d.teacherMatrixFreezeFlag = teacherMatrixFreezeFlag;
            d.teacherOrdinaryBetPolicy = teacherOrdinaryBetPolicy;
            d.teacherMatrixOrdinaryBetPolicy = teacherMatrixOrdinaryBetPolicy;
            d.teacherFieldSource = teacherFieldSource;
            d.teacherFieldVersion = teacherFieldVersion;
            d.teacherNumberStatus = teacherNumberStatus;
            d.teacherStatus = teacherStatus;
            d.teacherStatusClass = teacherStatusClass;
            d.teacherEvidenceContribution = teacherEvidenceContribution;
            d.teacherMatrixEvidenceContribution = teacherMatrixEvidenceContribution;
            d.teacherCounterContribution = teacherCounterContribution;
            d.teacherMatrixCounterContribution = teacherMatrixCounterContribution;
            d.teacherSampleReliability = teacherSampleReliability;
            d.teacherRecentValidity = teacherRecentValidity;
            d.v20Status = v20Status;
            d.dynamicStatus = dynamicStatus;
            d.dynamicCauseStatus = dynamicCauseStatus;
            d.v20ActionAdvice = v20ActionAdvice;
            d.v20FieldSource = v20FieldSource;
            d.v20FieldVersion = v20FieldVersion;
            d.v20EmptyFlag = v20EmptyFlag;
            d.v20SupportScore = v20SupportScore;
            d.v20RiskScore = v20RiskScore;
            d.v20EvidenceContribution = v20EvidenceContribution;
            d.v20CounterContribution = v20CounterContribution;
            d.v20SampleReliability = v20SampleReliability;
            d.v20RecentValidity = v20RecentValidity;
            d.similarWindowSample = similarWindowSample;
            d.similarWindowHitRate = similarWindowHitRate;
            d.v20WindowStateKey = v20WindowStateKey;
            d.windowStateKey = windowStateKey;
            d.finalDecisionWindowStateKey = finalDecisionWindowStateKey;
            d.dynamicWindowStateKey = dynamicWindowStateKey;
            d.probabilityWindowStateKey = probabilityWindowStateKey;
            d.coverageQualityScore = coverageQualityScore;
            d.thirdNumberQualityScore = thirdNumberQualityScore;
            d.k3CoverageRisk = k3CoverageRisk;
            d.falseHighPFlag = falseHighPFlag;
            d.probabilityFalseHighPFlag = probabilityFalseHighPFlag;
            d.probabilityFalseHighRisk = probabilityFalseHighRisk;
            d.probabilityPreferredEdgeBand = probabilityPreferredEdgeBand;
            d.probabilityHighEdgeFlag = probabilityHighEdgeFlag;
            d.probabilityPreferredPBand = probabilityPreferredPBand;
            d.probabilityTeacherWeakOrEmpty = probabilityTeacherWeakOrEmpty;
            d.probabilityV20UnknownOrEmpty = probabilityV20UnknownOrEmpty;
            d.probabilityStrongCauseFlag = probabilityStrongCauseFlag;
            d.coverageProbDiscount = coverageProbDiscount;
            d.coverageProbBoost = coverageProbBoost;
            d.coverageK3HitRate32 = coverageK3HitRate32;
            d.coverageK3HitRate64 = coverageK3HitRate64;
            d.coverageK3HitRate128 = coverageK3HitRate128;
            d.coverageNumberRate1 = coverageNumberRate1;
            d.coverageNumberRate2 = coverageNumberRate2;
            d.coverageNumberRate3 = coverageNumberRate3;
            d.finalPBeforeCoverage = finalPBeforeCoverage;
            d.finalPAfterCoverage = finalPAfterCoverage;
            d.coverageAdjustedP = coverageAdjustedP;
            d.probabilityCoverageSummary = probabilityCoverageSummary;
            d.coverageQualitySummary = coverageQualitySummary;
            d.probabilityCoverageActionAdvice = probabilityCoverageActionAdvice;
            d.probabilityActionAdvice = probabilityActionAdvice;
            d.probabilityFieldVersion = probabilityFieldVersion;
            d.dynamicGateQualityStatus = dynamicGateQualityStatus;
            d.dynamicGateFalseHighPFlag = dynamicGateFalseHighPFlag;
            d.dynamicGatePreferredEdgeBand = dynamicGatePreferredEdgeBand;
            d.dynamicGateTeacherV20WeakFlag = dynamicGateTeacherV20WeakFlag;
            d.dynamicGateEdgePolicy = dynamicGateEdgePolicy;
            d.dynamicGateCoverageRisk = dynamicGateCoverageRisk;
            d.dynamicGateCoverageQualityScore = dynamicGateCoverageQualityScore;
            d.dynamicGateThirdNumberQuality = dynamicGateThirdNumberQuality;
            d.dynamicGateVirtualEdgeRisk = dynamicGateVirtualEdgeRisk;
            d.dynamicGateActionAdvice = dynamicGateActionAdvice;
            d.dynamicGateFieldVersion = dynamicGateFieldVersion;
            d.dynamicGateVersion = dynamicGateVersion;
            d.finalDecisionVersion = finalDecisionVersion;
            d.finalDecisionEdgePolicy = finalDecisionEdgePolicy;
            d.finalDecisionTeacherPolicy = finalDecisionTeacherPolicy;
            d.finalDecisionV20Policy = finalDecisionV20Policy;
            d.finalDecisionBetDomain = finalDecisionBetDomain;
            d.finalDecisionRejectCode = finalDecisionRejectCode;
            d.finalDecisionShadowReason = finalDecisionShadowReason;
            d.finalDecisionStrongOverrideTrace = finalDecisionStrongOverrideTrace;
            d.finalDecisionCoverageRisk = finalDecisionCoverageRisk;
            d.finalDecisionCoverageQuality = finalDecisionCoverageQuality;
            d.finalDecisionThirdNumberQuality = finalDecisionThirdNumberQuality;
            d.finalDecisionFalseHighPFlag = finalDecisionFalseHighPFlag;

            d.preDrawReasonFingerprint = preDrawReasonFingerprint;
            d.preDrawReasonText = preDrawReasonText;
            d.reasonValidLevel = reasonValidLevel;
            d.reasonScore = reasonScore;
            d.reasonEdge = reasonEdge;
            d.hitSimilarity = hitSimilarity;
            d.missSimilarity = missSimilarity;
            d.contradictionScore = contradictionScore;
            d.causalLift = causalLift;
            d.causalConfidence = causalConfidence;
            d.validReasonCount = validReasonCount;
            d.falseReasonCount = falseReasonCount;
            d.forwardReasonSample = forwardReasonSample;
            d.forwardReasonHitRate = forwardReasonHitRate;
            d.reasonWilsonScore = reasonWilsonScore;
            d.k3RoleSummary = k3RoleSummary;
            d.k3MainNumber = k3MainNumber;
            d.k3SupportNumber = k3SupportNumber;
            d.k3DefenseNumber = k3DefenseNumber;
            d.comboRoleDiversity = comboRoleDiversity;
            d.comboConflict = comboConflict;
            d.fingerprintCooling = fingerprintCooling;
            d.elite45Passed = elite45Passed;
            d.elite45Status = elite45Status;
            d.elite45BlockCode = elite45BlockCode;
            d.elite45Gate = elite45Gate;
            d.elite45Summary = elite45Summary;
            copyDynamicCausalBridgeFields(this, d);
            copyMainChainBridgeFields(this, d);

            d.dataOk = 1;
            d.syncAction();
            Models.copySharedPublicFieldsV20(this, d);
            return d;
        }
    }

    public static final class ProbabilityResult {

        /* 星河 V40：全号码分布、OOS校准、分层后验、不可变契约与唯一授权。 */
        public double rawRankScore;
        public double calibratedHitProbability;
        public double honestProbability;
        public double actionUtility;
        public double actionStructureScore;
        public double actionDiscriminationScore;
        public double actionMaturityScore;
        public double actionUncertaintyPenalty;
        public double actionFrequencySupport;
        /* V42 unified risk-coverage evidence.  These fields are immutable audit
         * evidence after lane compilation; they never create a BET. */
        public double v42AcceptScore;
        public double v42FamilyLcb;
        public double v42RegimeQuality;
        public double v42AssociationScore;
        public double v42DistillationRisk;
        public int v42SupportVotes;
        public String v42RegimeFamily;
        public String v42RiskCoverageMode;
        public String v42PrimaryReject;
        public String v42SecondaryReject;
        public String v42TertiaryReject;
        public String v42LanePolicyVersion;
        /* V43 canonical full-strategy evidence contract. */
        public double v43StrategySupport;
        public double v43StrategyRisk;
        public double v43StrategyConfidence;
        public double v43StrategyDiversity;
        public double v43StrategyMaturity;
        public double v43SafeProbability;
        public double v43ProbabilityResidual;
        public double v43AblationFragility;
        public double v43ProbabilityLock;
        public int v43EvidenceGroupCount;
        public int v43HardRisk;
        public String v43DominantEvidence;
        public String v43MissingEvidence;
        public String v43EvidenceFingerprint;
        public String v43ProbabilityLockHash;
        public String v43PipelineStage;
        public String v43InvariantStatus;
        public String v43StrategyVersion;
        public String actionUtilityVersion;
        public String laneCandidate;
        public int oosRankSlot;
        public int oosCandidateCount;
        public String buildId;
        public String cohortId;
        public String schemaVersion;
        public String contractDecisionId;
        public String candidateHash;
        public String snapshotMaxIssue;
        public String trainedUntilIssue;
        public String featureSchemaVersion;
        public String rankingVersion;
        public String calibrationVersion;
        public String gatePolicyVersion;
        public String pipelineVersion;
        public int contractValid;
        public String contractStatus;
        public String coarseFamilyKey;
        public String midFamilyKey;
        public String fineFamilyKey;
        public String positionFamilyKey;
        public String posteriorMatchedLevel;
        public String posteriorFamilyKey;
        public String posteriorPolicy;
        public String posteriorVersion;
        public int posteriorSample;
        public int posteriorHit;
        public double posteriorMean;
        public double posteriorWilsonLower;
        public double posteriorRecentRate;
        public double posteriorDrift;
        public double familyWilsonLower;
        public int calibrationBucket;
        public int calibrationSample;
        public int calibrationTotalSample;
        public int calibrationReady;
        public String calibrationStatus;
        public double calibrationBrier;
        public String authorizationId;
        public int authorizationValid;
        public String authorizedMode;
        public String authorizationRejectCode;
        public long authorizedAt;
        public double authorizationProbability;
        public double authorizationGate;
        public double authorizationEdge;
        public String teacherPolicy;
        public String dynamicPolicy;
        public String adaptivePolicy;
        public String microWalkForwardStatus;
        public String microWalkForwardVersion;
        public String microWalkForwardAsOfIssue;
        public int microWalkForwardSample;

        public double modelP;
        public double shapeP;
        public double comboP;
        public double recoveryP;
        public double similarP;
        public double memoryP;
        public double rawP;
        public double calibratedP;
        public double finalP;
        public double dynamicGate;
        public double calibrationError;
        public double gateAdjust;
        public String bucketKey;
        public String mode;
        public int enrichLimit;
        public int deepLimit;
        public double deepFeatureP;
        public double biasEvidenceP;
        public double profileP;
        public double runtimeProfileP;
        public double stateNumberProfileP;
        public double teacherMatrixP;
        public double teacherMatrixAdjust;
        public double reasonP;
        public double reasonAdjust;
        public double fallbackProfileP;
        public double transitionP;
        public double cycleP;
        public double auditAdjustedP;
        public double promotionP;
        public double aPlusP;
        public double aiJudgeP;
        public double auditPenalty;
        public double auditBoost;
        /* V20主链闭环字段：只承接字段，不计算概率，不决定BET */
        public String clusterKey;
        public String factorKey;
        public double positionNeedScore;
        public double numberSupplyScore;
        public double coverageQuality;
        public double thirdQuality;
        public double counterEvidenceScore;
        public int similarSampleCount;
        public String adaptiveStatus;
        public String shadowPromotionStatus;
        public double finalPBeforeDiagnosis;
        public double finalPAfterDiagnosis;
        public double gate;
        public String rejectCode;
        public int realProbeAllowed;

        /* 链路修改升级V2字段：只承接，不计算概率，不决定BET */
        public double realBetFrequency50;
        public double realBetFrequency100;
        public double realBetFrequency300;
        public double frequencyDebtScore;
        public double frequencySupportScore;
        public double frequencyGateAdjust;
        public double qualifiedProbeScore;
        public double shadowPromotionScore;
        public double baseGate;
        public double riskAdjustedGate;
        public double effectiveProbeGate;
        public double rawEdge;
        public double effectiveProbeEdge;
        public double needSupplyScore;
        public double counterRisk;
        public int qualifiedProbe;
        public int realTrade;
        public int coverageAvailable;
        public int thirdQualityAvailable;
        public int promotionDomainAvailable;
        public String frequencyState;
        public String frequencyStateVersion;
        public String shadowPromotionDomain;
        public String decisionPath;
        public String betDomain;
        public String probeRejectReason;
        public String probeEligibilityVersion;
        public String ruleHealthStatus;
        public String snapshotFreshness;
        public String warmProfileFreshness;
        public String coldProfileFreshness;
        public String deepProfileFreshness;
        public String decisionVersionV2;
        public String evidenceAsOfIssue;
        public String snapshotAsOfIssue;
        public long snapshotCreatedAt;
        public long frequencyStateUpdatedAt;
        public int frequencyFloorRequiredCount;
        public int frequencyFloorTargetCount;
        public int frequencyFloorActualCount;
        public int frequencyFloorDebtCount;
        public int frequencyTargetDebtCount;
        public int frequencyFloorMandatory;
        public int frequencyFloorNoUpperCap;
        public double frequencyFloorRankScore;
        public String frequencyFloorVersion;

        /* 73487246：四窗口平滑频率、冻结域、Teacher/Dynamic分层审计。 */
        public double realBetFrequency20;
        public int frequencyObserved20;
        public int frequencyObserved50;
        public int frequencyObserved100;
        public int frequencyObserved300;
        public int frequencyBetCount20;
        public int frequencyBetCount50;
        public int frequencyBetCount100;
        public int frequencyBetCount300;
        public int frequencyDebt20;
        public int frequencyDebt50;
        public int frequencyDebt100;
        public int frequencyDebt300;
        public int frequencyTargetDebt20;
        public int frequencyTargetDebt50;
        public int frequencyTargetDebt100;
        public int frequencyTargetDebt300;
        public String frequencyUrgentWindow;
        public int frequencyIssuesSinceLastRealBet;
        public int frequencyIssuesSinceLastFloorBet;
        public int frequencyConsecutiveFloorBets;
        public int frequencyFloorMinGap;
        public int frequencyFloorPacingReady;
        public String frequencyFloorPacingReason;
        public String decisionQualityPath;
        public int evidenceDomainFrozen;
        public String evidenceDomainFrozenIssue;
        public long evidenceDomainFrozenAt;
        public String dynamicMatchedLevel;
        public int dynamicRealSample;
        public int dynamicShadowSample;
        public String moduleBuildVersion;

        public ProbabilityResult() {
            modelP = 0.0d;
            shapeP = 0.0d;
            comboP = 0.0d;
            recoveryP = 0.0d;
            similarP = 0.0d;
            memoryP = 0.0d;
            rawP = 0.0d;
            calibratedP = 0.0d;
            finalP = 0.0d;
            dynamicGate = AppConfig.DEFAULT_DYNAMIC_GATE;
            calibrationError = 0.0d;
            gateAdjust = 0.0d;
            bucketKey = "";
            mode = "";
            enrichLimit = 0;
            deepLimit = 0;
            deepFeatureP = 0.0d;
            biasEvidenceP = 0.0d;
            profileP = 0.0d;
            runtimeProfileP = 0.0d;
            stateNumberProfileP = 0.0d;
            teacherMatrixP = 0.0d;
            teacherMatrixAdjust = 0.0d;
            reasonP = 0.0d;
            reasonAdjust = 0.0d;
            fallbackProfileP = 0.0d;
            transitionP = 0.0d;
            cycleP = 0.0d;
            auditAdjustedP = 0.0d;
            promotionP = 0.0d;
            aPlusP = 0.0d;
            aiJudgeP = 0.0d;
            auditPenalty = 0.0d;
            auditBoost = 0.0d;
            initMainChainBridgeFields(this);
        }

        public boolean passesGate() {
            return finalP >= dynamicGate;
        }
    }

    public static final class ReasonItem {
        public String level;
        public String title;
        public String detail;
        public String code;
        public int passed;
        public double value;
        public long createdAt;

        public ReasonItem() {
            level = AppConfig.REASON_INFO;
            title = "";
            detail = "";
            code = "";
            passed = 0;
            value = 0.0d;
            createdAt = 0L;
        }

        public ReasonItem(
                String levelValue,
                String titleValue,
                String detailValue,
                String codeValue,
                boolean pass) {
            level = levelValue == null ? AppConfig.REASON_INFO : levelValue;
            title = titleValue == null ? "" : titleValue;
            detail = detailValue == null ? "" : detailValue;
            code = codeValue == null ? "" : codeValue;
            passed = pass ? 1 : 0;
            value = 0.0d;
            createdAt = System.currentTimeMillis();
        }
    }

    public static final class RuntimeStats {
        public int betCount;
        public int hitCount;
        public int missCount;
        public int holdCount;
        public int shadowCount;
        /** Frozen R3.6 SHADOW ledger; deliberately separate from REAL decision statistics. */
        public int shadowObservationCount;
        public int shadowSettledCount;
        public int shadowHitCount;
        public int shadowMissCount;
        public double shadowHitRate;
        public double hitRate;
        public double winRate;
        public double freq100;
        public double freq500;
        public double freq1000;
        public double frequency;
        public double pnl;
        public double maxDrawdown;
        public int loseStreak;
        public int openTrades;
        public String lastBacktestSummary;
        public String lastBacktestMode;
        public double lastBacktestWinRate;
        public double lastBacktestFrequency;
        public double lastBacktestPnl;
        public double lastBacktestMaxDrawdown;
        public String lastRealtimeValidationSummary;
        public String lastTeacherSummary;
        public int teacherMatrixSampleCount;
        public int teacherMatrixProfileCount;
        public int teacherMatrixStrongCount;
        public int teacherMatrixRiskCount;
        public double teacherMatrixAvgHitRate;
        public double teacherMatrixAvgConfidence;
        public String teacherMatrixSummary;
        public int reasonSampleCount;
        public int reasonProfileCount;
        public double reasonAvgEdge;
        public String reasonSummary;
        public int elite45PassCount;
        public String elite45Summary;
        public String learningSummary;
        public String autoLearnSummary;
        public String runtimeProfileSummary;
        public String biasSummary;
        public String auditSummary;
        public String promotionSummary;
        public String aPlusSummary;
        public String aiJudgeSummary;
        public String selfDiagnosisSummary;
        public String lastSnapshotId;
        public String lastTeacherSnapshotId;
        public long updatedAt;

        public RuntimeStats() {
            betCount = 0;
            hitCount = 0;
            missCount = 0;
            holdCount = 0;
            shadowCount = 0;
            shadowObservationCount = 0;
            shadowSettledCount = 0;
            shadowHitCount = 0;
            shadowMissCount = 0;
            shadowHitRate = 0.0d;
            hitRate = 0.0d;
            winRate = 0.0d;
            freq100 = 0.0d;
            freq500 = 0.0d;
            freq1000 = 0.0d;
            frequency = 0.0d;
            pnl = 0.0d;
            maxDrawdown = 0.0d;
            loseStreak = 0;
            openTrades = 0;
            lastBacktestSummary = "";
            lastBacktestMode = "";
            lastBacktestWinRate = 0.0d;
            lastBacktestFrequency = 0.0d;
            lastBacktestPnl = 0.0d;
            lastBacktestMaxDrawdown = 0.0d;
            lastRealtimeValidationSummary = "";
            lastTeacherSummary = "";
            teacherMatrixSampleCount = 0;
            teacherMatrixProfileCount = 0;
            teacherMatrixStrongCount = 0;
            teacherMatrixRiskCount = 0;
            teacherMatrixAvgHitRate = 0.0d;
            teacherMatrixAvgConfidence = 0.0d;
            teacherMatrixSummary = "";
            reasonSampleCount = 0;
            reasonProfileCount = 0;
            reasonAvgEdge = 0.0d;
            reasonSummary = "";
            elite45PassCount = 0;
            elite45Summary = "";
            learningSummary = "";
            autoLearnSummary = "";
            runtimeProfileSummary = "";
            biasSummary = "";
            auditSummary = "";
            promotionSummary = "";
            aPlusSummary = "";
            aiJudgeSummary = "";
            selfDiagnosisSummary = "";
            lastSnapshotId = "";
            lastTeacherSnapshotId = "";
            updatedAt = 0L;
        }

        public void recalc() {
            hitRate = betCount > 0 ? ((double) hitCount) / betCount : 0.0d;
            winRate = hitRate;
            missCount = betCount - hitCount;
            if (missCount < 0) missCount = 0;
            shadowMissCount = Math.max(0, shadowSettledCount - shadowHitCount);
            shadowHitRate =
                    shadowSettledCount > 0
                            ? ((double) shadowHitCount) / ((double) shadowSettledCount)
                            : 0.0d;
            int totalDecision = betCount + holdCount + shadowCount;
            if (totalDecision > 0) frequency = ((double) betCount) / ((double) totalDecision);
        }

        public boolean hasRealBet() {
            return betCount > 0;
        }

        public String realStatText() {
            recalc();
            return "真实BET="
                    + betCount
                    + "，命中="
                    + hitCount
                    + "，未中="
                    + missCount
                    + "，真实胜率="
                    + UiFormatter.probability(hitRate)
                    + "，真实盈亏="
                    + UiFormatter.money(pnl)
                    + "，HOLD="
                    + holdCount
                    + "，SHADOW="
                    + shadowCount
                    + "；HOLD/SHADOW不计真实胜率"
                    + "；冻结SHADOW预测/结算/命中/未中="
                    + shadowObservationCount
                    + '/'
                    + shadowSettledCount
                    + '/'
                    + shadowHitCount
                    + '/'
                    + shadowMissCount
                    + "；教师矩阵样本="
                    + teacherMatrixSampleCount
                    + "，画像="
                    + teacherMatrixProfileCount
                    + "；原因样本="
                    + reasonSampleCount
                    + "，Elite45通过="
                    + elite45PassCount;
        }
    }

    public static final class AppRuntimeState {
        public String serviceStatus;
        public String currentIssue;
        public String targetIssue;
        public String apiLine;
        public long nextOpenTime;
        public long countdownMs;
        public long updatedAt;
        public String lastDecision;
        public String lastError;
        public int runningFlag;
        public int running;
        public int backgroundAlive;
        public int countdownSeconds;
        public int consecutiveError;
        public long lastFetchAt;
        public long lastSuccessAt;
        public long latestDrawAt;
        public String apiStatus;
        public String backtestStatus;
        public String backtestMode;
        public int backtestRunning;
        public int backtestProgress;
        public String backtestSummary;
        public String realtimeValidationStatus;
        public int realtimeValidationRunning;
        public int realtimeValidationWindow;
        public String realtimeValidationSummary;
        public String manualBacktestStatus;
        public int manualBacktestRunning;
        public int manualBacktestProgress;
        public String manualBacktestSummary;
        public String teacherSnapshotStatus;
        public String teacherSnapshotSummary;
        public String teacherMatrixStatus;
        public int teacherMatrixSampleCount;
        public int teacherMatrixProfileCount;
        public String teacherMatrixSummary;
        public String reasonStatus;
        public int reasonSampleCount;
        public int reasonProfileCount;
        public String reasonSummary;
        public String elite45Summary;
        public String autoLearnStatus;
        public int autoLearnRunning;
        public int autoLearnProgress;
        public String autoLearnTaskKey;
        public String autoLearnSummary;
        public String runtimeProfileSummary;
        public String learningSummary;
        public String biasSummary;
        public String auditSummary;
        public String aiJudgeSummary;
        public String selfDiagnosisSummary;
        public String promotionSummary;
        public String aPlusSummary;
        public String schedulerSummary;
        public String lastSnapshotId;

        public AppRuntimeState() {
            serviceStatus = AppConfig.SERVICE_STATUS_STOPPED;
            currentIssue = "";
            targetIssue = "";
            apiLine = AppConfig.API_LINE_PRIMARY;
            nextOpenTime = 0L;
            countdownMs = 0L;
            updatedAt = 0L;
            lastDecision = AppConfig.DECISION_HOLD;
            lastError = "";
            runningFlag = 0;
            running = 0;
            backgroundAlive = 1;
            countdownSeconds = -1;
            consecutiveError = 0;
            lastFetchAt = 0L;
            lastSuccessAt = 0L;
            latestDrawAt = 0L;
            apiStatus = "";
            backtestStatus = "";
            backtestMode = "";
            backtestRunning = 0;
            backtestProgress = 0;
            backtestSummary = "";
            realtimeValidationStatus = AppConfig.BACKTEST_STATUS_IDLE;
            realtimeValidationRunning = 0;
            realtimeValidationWindow = AppConfig.AUTO_VALIDATE_DEFAULT_WINDOW;
            realtimeValidationSummary = "";
            manualBacktestStatus = AppConfig.BACKTEST_STATUS_IDLE;
            manualBacktestRunning = 0;
            manualBacktestProgress = 0;
            manualBacktestSummary = "";
            teacherSnapshotStatus = AppConfig.BACKTEST_STATUS_IDLE;
            teacherSnapshotSummary = "";
            teacherMatrixStatus = AppConfig.TEACHER_MATRIX_STATUS_NONE;
            teacherMatrixSampleCount = 0;
            teacherMatrixProfileCount = 0;
            teacherMatrixSummary = "";
            reasonStatus = REASON_LEVEL_C_EXPLAIN_ONLY;
            reasonSampleCount = 0;
            reasonProfileCount = 0;
            reasonSummary = "";
            elite45Summary = "";
            autoLearnStatus = AppConfig.AUTO_LEARN_STATUS_IDLE;
            autoLearnRunning = 0;
            autoLearnProgress = 0;
            autoLearnTaskKey = "";
            autoLearnSummary = "";
            runtimeProfileSummary = "";
            learningSummary = "";
            biasSummary = "";
            auditSummary = "";
            aiJudgeSummary = "";
            selfDiagnosisSummary = "";
            promotionSummary = "";
            aPlusSummary = "";
            schedulerSummary = "";
            lastSnapshotId = "";
        }
    }

    public static final class LearningSample {
        public String baseIssue;
        public String targetIssue;
        public int position;
        public int k3Mask;
        public String shapeKey;
        public String stateKey;
        public String stateMode;
        public int hit;
        public int actualNumber;
        public int actualRank;
        public double finalP;
        public double rawP;
        public double trendLife;
        public double driftRisk;
        public double breakDepth;
        public double recoveryScore;
        public double championGap;
        public double comboGap;
        public String missReason;
        public long createdAt;
        public String statScope;
        public String sourceType;
        public int isRealtimeReal;
        public String deepProfileKey;
        public String attributionReasonCode;
        public String affectedModule;
        public String preDrawReasonFingerprint;
        public String preDrawReasonText;
        public String reasonValidLevel;
        public double reasonScore;
        public double reasonEdge;
        public double hitSimilarity;
        public double missSimilarity;
        public double contradictionScore;
        public double causalLift;
        public double causalConfidence;
        public int validReasonCount;
        public int falseReasonCount;
        public String k3RoleSummary;
        public int elite45Passed;
        public String elite45Summary;

        public LearningSample() {
            baseIssue = "";
            targetIssue = "";
            position = 0;
            k3Mask = 0;
            shapeKey = "";
            stateKey = "";
            stateMode = AppConfig.MODE_CHAOS;
            hit = 0;
            actualNumber = 0;
            actualRank = 0;
            finalP = 0.0d;
            rawP = 0.0d;
            trendLife = 0.0d;
            driftRisk = 0.0d;
            breakDepth = 0.0d;
            recoveryScore = 0.0d;
            championGap = 0.0d;
            comboGap = 0.0d;
            missReason = "";
            createdAt = 0L;
            statScope = AppConfig.STAT_SCOPE_REAL_TRADE;
            sourceType = SOURCE_TYPE_UNKNOWN;
            isRealtimeReal = 0;
            deepProfileKey = "";
            attributionReasonCode = "";
            affectedModule = AppConfig.ATTR_MODULE_UNKNOWN;
            preDrawReasonFingerprint = "";
            preDrawReasonText = "";
            reasonValidLevel = REASON_LEVEL_C_EXPLAIN_ONLY;
            reasonScore = 0.0d;
            reasonEdge = 0.0d;
            hitSimilarity = 0.0d;
            missSimilarity = 0.0d;
            contradictionScore = 0.0d;
            causalLift = 0.0d;
            causalConfidence = 0.0d;
            validReasonCount = 0;
            falseReasonCount = 0;
            k3RoleSummary = "";
            elite45Passed = 0;
            elite45Summary = "";
        }

        public boolean isRealTradeSample() {
            return AppConfig.isRealTradeScope(statScope)
                    && SOURCE_TYPE_REALTIME.equals(normalizeSourceType(sourceType))
                    && isRealtimeReal == 1;
        }

        public boolean isHit() {
            return hit == 1;
        }
    }

    public static final class CandidateBatch {
        public String baseIssue;
        public String targetIssue;
        public ArrayList<ComboCandidate> candidates;
        public ComboCandidate best;
        public String scanMode;
        public String probabilityMode;
        public int fullScan;
        public int scannedCount;
        public int keptCount;
        public int enrichCount;
        public int deepCount;

        public CandidateBatch() {
            baseIssue = "";
            targetIssue = "";
            candidates = new ArrayList<ComboCandidate>();
            best = null;
            scanMode = "";
            probabilityMode = "";
            fullScan = 0;
            scannedCount = 0;
            keptCount = 0;
            enrichCount = 0;
            deepCount = 0;
        }

        public void add(ComboCandidate c) {
            if (c == null) return;
            candidates.add(c);
            keptCount = candidates.size();
            if (best == null || c.sortScore() > best.sortScore()) {
                best = c;
            }
        }

        public void addAll(List<ComboCandidate> list) {
            if (list == null) return;
            for (int i = 0; i < list.size(); i++) add(list.get(i));
        }

        public int size() {
            return candidates == null ? 0 : candidates.size();
        }

        public boolean hasBest() {
            return best != null && best.isValidK3();
        }
    }

    public static final class BacktestRuntimeState {
        public String mode;
        public String status;
        public int running;
        public int progress;
        public int window;
        public int totalSteps;
        public int doneSteps;
        public int candidateCount;
        public int enrichCount;
        public int deepCount;
        public long startedAt;
        public long updatedAt;
        public long finishedAt;
        public String summary;

        public BacktestRuntimeState() {
            mode = AppConfig.BACKTEST_MODE_FAST;
            status = "";
            running = 0;
            progress = 0;
            window = 0;
            totalSteps = 0;
            doneSteps = 0;
            candidateCount = 0;
            enrichCount = 0;
            deepCount = 0;
            startedAt = 0L;
            updatedAt = 0L;
            finishedAt = 0L;
            summary = "";
        }

        public void markStart(String modeValue, int windowValue, int total) {
            mode = AppConfig.normalizeBacktestMode(modeValue);
            window = windowValue;
            totalSteps = total;
            doneSteps = 0;
            progress = 0;
            running = 1;
            status = AppConfig.BACKTEST_STATUS_RUNNING;
            startedAt = System.currentTimeMillis();
            updatedAt = startedAt;
            finishedAt = 0L;
            summary = "";
        }

        public void updateProgress(int done) {
            doneSteps = done;
            if (totalSteps > 0) {
                progress = (int) (((double) doneSteps) * 100.0d / ((double) totalSteps));
                if (progress < 0) progress = 0;
                if (progress > 100) progress = 100;
            }
            updatedAt = System.currentTimeMillis();
        }

        public void markFinish(String text) {
            running = 0;
            progress = 100;
            status = AppConfig.BACKTEST_STATUS_OK;
            finishedAt = System.currentTimeMillis();
            updatedAt = finishedAt;
            summary = text == null ? "" : text;
        }

        public void markError(String text) {
            running = 0;
            status = AppConfig.BACKTEST_STATUS_ERROR;
            finishedAt = System.currentTimeMillis();
            updatedAt = finishedAt;
            summary = text == null ? "" : text;
        }
    }

    public static final class BiasEvidenceResult {
        public String evidenceId;
        public String issue;
        public String baseIssue;
        public String targetIssue;
        public double biasScore;
        public double positionBiasScore;
        public double numberBiasScore;
        public double transitionBiasScore;
        public double cycleBiasScore;
        public double comboBiasScore;
        public double stateBiasScore;
        public int sampleCount;
        public int recentSample;
        public double confidence;
        public String status;
        public String summaryText;
        public long createdAt;

        public BiasEvidenceResult() {
            evidenceId = "";
            issue = "";
            baseIssue = "";
            targetIssue = "";
            biasScore = 0.0d;
            positionBiasScore = 0.0d;
            numberBiasScore = 0.0d;
            transitionBiasScore = 0.0d;
            cycleBiasScore = 0.0d;
            comboBiasScore = 0.0d;
            stateBiasScore = 0.0d;
            sampleCount = 0;
            recentSample = 0;
            confidence = 0.0d;
            status = AppConfig.BIAS_STATUS_NONE;
            summaryText = "";
            createdAt = 0L;
        }

        public boolean isStrongBias() {
            return biasScore >= AppConfig.BIAS_SCORE_STRONG
                    && confidence >= AppConfig.BIAS_CONFIDENCE_STRONG;
        }

        public boolean isNoisy() {
            return AppConfig.BIAS_STATUS_NOISY.equals(status);
        }
    }

    public static final class PointInTimeSnapshot {
        public String snapshotId;
        public String baseIssue;
        public String targetIssue;
        public String trainStartIssue;
        public String trainEndIssue;
        public int visibleDrawCount;
        public int visibleTradeCount;
        public long visibleProfileVersion;
        public long visibleCalibrationVersion;
        public long visibleTeacherMatrixVersion;
        public long visibleReasonVersion;
        public String featureSummary;
        public String candidateSummary;
        public String gateSummary;
        public String decisionSummary;
        public String reasonSummary;
        public String dataHash;
        public long createdAt;
        public int noFutureLeakOk;
        public String status;
        public String warningText;

        public PointInTimeSnapshot() {
            snapshotId = "";
            baseIssue = "";
            targetIssue = "";
            trainStartIssue = "";
            trainEndIssue = "";
            visibleDrawCount = 0;
            visibleTradeCount = 0;
            visibleProfileVersion = 0L;
            visibleCalibrationVersion = 0L;
            visibleTeacherMatrixVersion = 0L;
            visibleReasonVersion = 0L;
            featureSummary = "";
            candidateSummary = "";
            gateSummary = "";
            decisionSummary = "";
            reasonSummary = "";
            dataHash = "";
            createdAt = 0L;
            noFutureLeakOk = 0;
            status = AppConfig.BACKTEST_STATUS_IDLE;
            warningText = "";
        }
    }

    public static final class DeepFeatureSet {
        public String baseIssue;
        public int drawCount;
        public PositionFeature[] positionFeatures;
        public NumberFeature[][] numberFeatures;
        public TransitionFeature transitionFeature;
        public CycleFeature cycleFeature;
        public StateFeature stateFeature;
        public DriftFeature driftFeature;
        public int bestPosition;
        public int[] bestNumbers;
        public double globalDriftRisk;
        public double globalEntropy;
        public double globalBiasScore;
        public double globalTrendStrength;
        public double teacherMatrixScore;
        public double reasonScore;
        public String stateMode;
        public String profileKey;
        public String summaryText;

        public DeepFeatureSet() {
            baseIssue = "";
            drawCount = 0;
            positionFeatures = new PositionFeature[AppConfig.POSITION_COUNT];
            numberFeatures = new NumberFeature[AppConfig.POSITION_COUNT][AppConfig.NUMBER_COUNT];
            transitionFeature = new TransitionFeature();
            cycleFeature = new CycleFeature();
            stateFeature = new StateFeature();
            driftFeature = new DriftFeature();
            bestPosition = 1;
            bestNumbers = new int[] {1, 2, 3};
            globalDriftRisk = 0.0d;
            globalEntropy = 0.0d;
            globalBiasScore = 0.0d;
            globalTrendStrength = 0.0d;
            teacherMatrixScore = 0.0d;
            reasonScore = 0.0d;
            stateMode = AppConfig.MODE_UNKNOWN;
            profileKey = "";
            summaryText = "";
        }
    }

    public static final class PositionFeature {
        public int position;
        public int sampleCount;
        public double hitMass;
        public double top3Mass;
        public double top5Mass;
        public double entropy;
        public double stabilityScore;
        public double driftRisk;
        public double trendLife;
        public double trendStrength;
        public double trendSlope;
        public double breakDepth;
        public double recoveryScore;
        public int positionRank;
        public double positionMomentum;
        public double positionScore;
        public double teacherMatrixScore;
        public double reasonScore;
        public String summaryText;

        public PositionFeature() {
            position = 0;
            sampleCount = 0;
            hitMass = 0.0d;
            top3Mass = 0.0d;
            top5Mass = 0.0d;
            entropy = 0.0d;
            stabilityScore = 0.0d;
            driftRisk = 0.0d;
            trendLife = 0.0d;
            trendStrength = 0.0d;
            trendSlope = 0.0d;
            breakDepth = 0.0d;
            recoveryScore = 0.0d;
            positionRank = 0;
            positionMomentum = 0.0d;
            positionScore = 0.0d;
            teacherMatrixScore = 0.0d;
            reasonScore = 0.0d;
            summaryText = "";
        }
    }

    public static final class NumberFeature {
        public int position;
        public int number;
        public int sampleCount;
        public double frequency32;
        public double frequency64;
        public double frequency160;
        public double frequency500;
        public int recentGap;
        public double avgGap;
        public double gapVariance;
        public double cycleScore;
        public double hotScore;
        public double coldScore;
        public double reverseRisk;
        public double recoverySignal;
        public double stabilityScore;
        public double confidence;
        public double numberScore;
        public double teacherMatrixScore;
        public double reasonScore;
        public String signalType;
        public String summaryText;

        public NumberFeature() {
            position = 0;
            number = 0;
            sampleCount = 0;
            frequency32 = 0.0d;
            frequency64 = 0.0d;
            frequency160 = 0.0d;
            frequency500 = 0.0d;
            recentGap = -1;
            avgGap = 0.0d;
            gapVariance = 0.0d;
            cycleScore = 0.0d;
            hotScore = 0.0d;
            coldScore = 0.0d;
            reverseRisk = 0.0d;
            recoverySignal = 0.0d;
            stabilityScore = 0.0d;
            confidence = 0.0d;
            numberScore = 0.0d;
            teacherMatrixScore = 0.0d;
            reasonScore = 0.0d;
            signalType = "";
            summaryText = "";
        }
    }

    public static final class TransitionFeature {
        public int position;
        public int fromNumber;
        public int toNumber;
        public int sampleCount;
        public double markov1Prob;
        public double markov2Prob;
        public double transitionLift;
        public double recentTransitionProb;
        public double transitionSupport;
        public double relationScore;
        public String summaryText;

        public TransitionFeature() {
            position = 0;
            fromNumber = 0;
            toNumber = 0;
            sampleCount = 0;
            markov1Prob = 0.0d;
            markov2Prob = 0.0d;
            transitionLift = 0.0d;
            recentTransitionProb = 0.0d;
            transitionSupport = 0.0d;
            relationScore = 0.0d;
            summaryText = "";
        }
    }

    public static final class CycleFeature {
        public int position;
        public int number;
        public double avgGap;
        public int lastSeenGap;
        public double gapVariance;
        public double cycleScore;
        public int cycleBucket;
        public int isNearCyclePoint;
        public String summaryText;

        public CycleFeature() {
            position = 0;
            number = 0;
            avgGap = 0.0d;
            lastSeenGap = -1;
            gapVariance = 0.0d;
            cycleScore = 0.0d;
            cycleBucket = 0;
            isNearCyclePoint = 0;
            summaryText = "";
        }
    }

    public static final class StateFeature {
        public String stateMode;
        public int trendBucket;
        public int driftBucket;
        public int breakBucket;
        public int recoveryBucket;
        public int cycleBucket;
        public int relationBucket;
        public String shapeKey;
        public String profileKey;
        public double stateScore;
        public double teacherMatrixScore;
        public double reasonScore;
        public String summaryText;

        public StateFeature() {
            stateMode = AppConfig.MODE_UNKNOWN;
            trendBucket = 0;
            driftBucket = 0;
            breakBucket = 0;
            recoveryBucket = 0;
            cycleBucket = 0;
            relationBucket = 0;
            shapeKey = "";
            profileKey = "";
            stateScore = 0.0d;
            teacherMatrixScore = 0.0d;
            reasonScore = 0.0d;
            summaryText = "";
        }
    }

    public static final class DriftFeature {
        public int position;
        public double drift16To64;
        public double drift32To160;
        public double drift64To500;
        public double entropyRise;
        public double strongNumberShift;
        public double driftRisk;
        public String summaryText;

        public DriftFeature() {
            position = 0;
            drift16To64 = 0.0d;
            drift32To160 = 0.0d;
            drift64To500 = 0.0d;
            entropyRise = 0.0d;
            strongNumberShift = 0.0d;
            driftRisk = 0.0d;
            summaryText = "";
        }
    }

    public static final class CandidateEvidence {
        public String evidenceKey;
        public String baseIssue;
        public String targetIssue;
        public int position;
        public int n1;
        public int n2;
        public int n3;
        public int k3Mask;
        public double positionEvidence;
        public double numberEvidence;
        public double cycleEvidence;
        public double transitionEvidence;
        public double patternEvidence;
        public double comboEvidence;
        public double profileEvidence;
        public double runtimeProfileEvidence;
        public double stateNumberEvidence;
        public double teacherMatrixEvidence;
        public double reasonEvidence;
        public double auditEvidence;
        public double riskScore;
        public double evidenceScore;
        public double confidence;
        public String supportText;
        public String riskText;
        public String summaryText;
        public long createdAt;

        public CandidateEvidence() {
            evidenceKey = "";
            baseIssue = "";
            targetIssue = "";
            position = 0;
            n1 = 0;
            n2 = 0;
            n3 = 0;
            k3Mask = 0;
            positionEvidence = 0.0d;
            numberEvidence = 0.0d;
            cycleEvidence = 0.0d;
            transitionEvidence = 0.0d;
            patternEvidence = 0.0d;
            comboEvidence = 0.0d;
            profileEvidence = 0.0d;
            runtimeProfileEvidence = 0.0d;
            stateNumberEvidence = 0.0d;
            teacherMatrixEvidence = 0.0d;
            reasonEvidence = 0.0d;
            auditEvidence = 0.0d;
            riskScore = 0.0d;
            evidenceScore = 0.0d;
            confidence = 0.0d;
            supportText = "";
            riskText = "";
            summaryText = "";
            createdAt = 0L;
        }

        public void setNumbers(int a, int b, int c) {
            n1 = a;
            n2 = b;
            n3 = c;
            k3Mask = buildK3Mask(a, b, c);
        }

        public boolean isUsable() {
            return evidenceScore >= AppConfig.EVIDENCE_SCORE_MIN
                    && riskScore <= AppConfig.EVIDENCE_RISK_MAX
                    && confidence >= AppConfig.AI_JUDGE_CONFIDENCE_MIN;
        }
    }

    public static final class AiJudgeResult {
        public String judgeId;
        public String baseIssue;
        public String targetIssue;
        public CandidateEvidence bestCandidate;
        public CandidateEvidence secondCandidate;
        public double championGap;
        public double judgeScore;
        public double judgeConfidence;
        public double conflictLevel;
        public String betAdvice;
        public String holdReason;
        public String summaryText;
        public long createdAt;

        public AiJudgeResult() {
            judgeId = "";
            baseIssue = "";
            targetIssue = "";
            bestCandidate = null;
            secondCandidate = null;
            championGap = 0.0d;
            judgeScore = 0.0d;
            judgeConfidence = 0.0d;
            conflictLevel = 0.0d;
            betAdvice = AppConfig.AI_JUDGE_HOLD;
            holdReason = "";
            summaryText = "";
            createdAt = 0L;
        }

        public boolean shouldBet() {
            return AppConfig.AI_JUDGE_BET.equals(betAdvice)
                    && judgeScore >= AppConfig.AI_JUDGE_SCORE_MIN
                    && judgeConfidence >= AppConfig.AI_JUDGE_CONFIDENCE_MIN
                    && conflictLevel <= AppConfig.AI_JUDGE_CONFLICT_MAX;
        }
    }

    public static final class AttributionResult {
        public String attributionId;
        public String targetIssue;
        public int hit;
        public String mainReasonCode;
        public String mainReasonText;
        public double confidence;
        public int shouldReward;
        public int shouldPunish;
        public String affectedModule;
        public String profileKey;
        public double positionImpact;
        public double numberImpact;
        public double cycleImpact;
        public double transitionImpact;
        public double calibrationImpact;
        public double teacherMatrixImpact;
        public double reasonImpact;
        public String learningText;
        public long createdAt;

        public AttributionResult() {
            attributionId = "";
            targetIssue = "";
            hit = 0;
            mainReasonCode = AppConfig.MISS_UNKNOWN;
            mainReasonText = "";
            confidence = 0.0d;
            shouldReward = 0;
            shouldPunish = 0;
            affectedModule = AppConfig.ATTR_MODULE_UNKNOWN;
            profileKey = "";
            positionImpact = 0.0d;
            numberImpact = 0.0d;
            cycleImpact = 0.0d;
            transitionImpact = 0.0d;
            calibrationImpact = 0.0d;
            teacherMatrixImpact = 0.0d;
            reasonImpact = 0.0d;
            learningText = "";
            createdAt = 0L;
        }
    }

    public static final class DeepProfile {
        public String profileKey;
        public int position;
        public String stateMode;
        public int trendBucket;
        public int driftBucket;
        public int breakBucket;
        public int recoveryBucket;
        public int cycleBucket;
        public int relationBucket;
        public int totalSample;
        public int totalHit;
        public double totalHitRate;
        public int recentSample;
        public int recentHit;
        public double recentHitRate;
        public double decayWeight;
        public int activeFlag;
        public int staleFlag;
        public int recoverFlag;
        public long lastHitAt;
        public long lastMissAt;
        public int failCount;
        public int recoverCount;
        public double avgFinalP;
        public double avgEdge;
        public double avgDeepScore;
        public double teacherMatrixScore;
        public double reasonScore;
        public long updatedAt;

        public DeepProfile() {
            profileKey = "";
            position = 0;
            stateMode = AppConfig.MODE_UNKNOWN;
            trendBucket = 0;
            driftBucket = 0;
            breakBucket = 0;
            recoveryBucket = 0;
            cycleBucket = 0;
            relationBucket = 0;
            totalSample = 0;
            totalHit = 0;
            totalHitRate = 0.0d;
            recentSample = 0;
            recentHit = 0;
            recentHitRate = 0.0d;
            decayWeight = 1.0d;
            activeFlag = 1;
            staleFlag = 0;
            recoverFlag = 0;
            lastHitAt = 0L;
            lastMissAt = 0L;
            failCount = 0;
            recoverCount = 0;
            avgFinalP = 0.0d;
            avgEdge = 0.0d;
            avgDeepScore = 0.0d;
            teacherMatrixScore = 0.0d;
            reasonScore = 0.0d;
            updatedAt = 0L;
        }

        public void recalc() {
            totalHitRate = totalSample > 0 ? ((double) totalHit) / totalSample : 0.0d;
            recentHitRate = recentSample > 0 ? ((double) recentHit) / recentSample : 0.0d;
        }

        public boolean isUsable() {
            recalc();
            return activeFlag == 1
                    && staleFlag == 0
                    && totalSample >= AppConfig.PROMOTION_BASE_SAMPLE_MIN;
        }
    }

    public static final class FallbackProfileResult {
        public String selectedProfileKey;
        public int selectedLevel;
        public int sampleCount;
        public double hitRate;
        public double recentHitRate;
        public double confidence;
        public int fallbackUsed;
        public String summaryText;

        public FallbackProfileResult() {
            selectedProfileKey = "";
            selectedLevel = 0;
            sampleCount = 0;
            hitRate = 0.0d;
            recentHitRate = 0.0d;
            confidence = 0.0d;
            fallbackUsed = 0;
            summaryText = "";
        }
    }

    public static final class ProbabilityAuditBucket {
        public String bucketKey;
        public double bucketLow;
        public double bucketHigh;
        public int sampleCount;
        public int hitCount;
        public double hitRate;
        public double avgPredictedP;
        public double calibrationError;
        public double brierScore;
        public int recentSample;
        public int recentHit;
        public double recentHitRate;
        public double recentError;
        public String status;
        public long updatedAt;

        public ProbabilityAuditBucket() {
            bucketKey = "";
            bucketLow = 0.0d;
            bucketHigh = 0.0d;
            sampleCount = 0;
            hitCount = 0;
            hitRate = 0.0d;
            avgPredictedP = 0.0d;
            calibrationError = 0.0d;
            brierScore = 0.0d;
            recentSample = 0;
            recentHit = 0;
            recentHitRate = 0.0d;
            recentError = 0.0d;
            status = AppConfig.PROB_BUCKET_SAMPLE_WEAK;
            updatedAt = 0L;
        }

        public void recalc() {
            hitRate = sampleCount > 0 ? ((double) hitCount) / sampleCount : 0.0d;
            recentHitRate = recentSample > 0 ? ((double) recentHit) / recentSample : 0.0d;
            calibrationError = Math.abs(hitRate - avgPredictedP);
            recentError = Math.abs(recentHitRate - avgPredictedP);
            if (sampleCount < AppConfig.PROB_AUDIT_SAMPLE_MIN)
                status = AppConfig.PROB_BUCKET_SAMPLE_WEAK;
            else if (calibrationError >= AppConfig.PROB_AUDIT_ERROR_HARD && avgPredictedP > hitRate)
                status = AppConfig.PROB_BUCKET_DANGEROUS_OVER;
            else if (avgPredictedP > hitRate + AppConfig.PROB_AUDIT_ERROR_WARN)
                status = AppConfig.PROB_BUCKET_OVER;
            else if (hitRate > avgPredictedP + AppConfig.PROB_AUDIT_ERROR_WARN)
                status = AppConfig.PROB_BUCKET_UNDER;
            else status = AppConfig.PROB_BUCKET_OK;
        }

        public double getPenalty() {
            recalc();
            if (AppConfig.PROB_BUCKET_DANGEROUS_OVER.equals(status))
                return AppConfig.PROB_AUDIT_MAX_PENALTY;
            if (AppConfig.PROB_BUCKET_OVER.equals(status))
                return Math.min(AppConfig.PROB_AUDIT_MAX_PENALTY, calibrationError * 0.50d);
            return 0.0d;
        }

        public double getBoost() {
            recalc();
            if (AppConfig.PROB_BUCKET_UNDER.equals(status))
                return Math.min(AppConfig.PROB_AUDIT_MAX_BOOST, calibrationError * 0.30d);
            return 0.0d;
        }
    }

    public static final class PromotionCandidate {
        public String promotionKey;
        public String profileKey;
        public String status;
        public int sampleCount;
        public int hitCount;
        public double hitRate;
        public int recentSample;
        public int recentHit;
        public double recentHitRate;
        public double calibrationError;
        public double biasScore;
        public double aPlusScore;
        public double teacherMatrixScore;
        public double reasonScore;
        public String promoteReason;
        public String rejectReason;
        public long updatedAt;

        public PromotionCandidate() {
            promotionKey = "";
            profileKey = "";
            status = AppConfig.PROMOTION_CANDIDATE;
            sampleCount = 0;
            hitCount = 0;
            hitRate = 0.0d;
            recentSample = 0;
            recentHit = 0;
            recentHitRate = 0.0d;
            calibrationError = 0.0d;
            biasScore = 0.0d;
            aPlusScore = 0.0d;
            teacherMatrixScore = 0.0d;
            reasonScore = 0.0d;
            promoteReason = "";
            rejectReason = "";
            updatedAt = 0L;
        }

        public void recalc() {
            hitRate = sampleCount > 0 ? ((double) hitCount) / sampleCount : 0.0d;
            recentHitRate = recentSample > 0 ? ((double) recentHit) / recentSample : 0.0d;
        }

        public boolean isStrongEnough() {
            recalc();
            return sampleCount >= AppConfig.PROMOTION_STRONG_SAMPLE_MIN
                    && recentSample >= AppConfig.PROMOTION_STRONG_RECENT_SAMPLE_MIN
                    && hitRate >= AppConfig.PROMOTION_STRONG_HIT_RATE_MIN
                    && recentHitRate >= AppConfig.PROMOTION_STRONG_RECENT_HIT_RATE_MIN
                    && calibrationError <= AppConfig.PROMOTION_STRONG_CALIBRATION_ERROR_MAX;
        }
    }

    public static final class ManualBacktestTask {
        public String taskId;
        public String taskName;
        public String mode;
        public String status;
        public int progress;
        public int windowSize;
        public int startIndex;
        public int endIndex;
        public int minTrain;
        public int maxTest;
        public int topKeep;
        public int totalSteps;
        public int doneSteps;
        public long startedAt;
        public long finishedAt;
        public String summary;
        public String error;

        public ManualBacktestTask() {
            taskId = "";
            taskName = "手动深度回测";
            mode = AppConfig.BACKTEST_MODE_FULL;
            status = AppConfig.BACKTEST_STATUS_IDLE;
            progress = 0;
            windowSize = AppConfig.MANUAL_BACKTEST_DEFAULT_WINDOW;
            startIndex = 0;
            endIndex = 0;
            minTrain = 240;
            maxTest = 2000;
            topKeep = 80;
            totalSteps = 0;
            doneSteps = 0;
            startedAt = 0L;
            finishedAt = 0L;
            summary = "";
            error = "";
        }

        public void updateProgress(int done) {
            doneSteps = done;
            if (totalSteps > 0) {
                progress = (int) (((double) doneSteps) * 100.0d / totalSteps);
                if (progress < 0) progress = 0;
                if (progress > 100) progress = 100;
            }
        }
    }

    public static final class ManualDeepBacktestReport {
        public String reportId;
        public String taskName;
        public String mode;
        public int windowSize;
        public String startIssue;
        public String endIssue;
        public int startIndex;
        public int endIndex;
        public int minTrain;
        public int maxTest;
        public int totalHistory;
        public int totalSteps;
        public int testCount;
        public int betCount;
        public int holdCount;
        public int shadowCount;
        public int hitCount;
        public int missCount;
        public int winCount;
        public int loseCount;
        public double hitRate;
        public double frequency;
        public double betRate;
        public double holdRate;
        public double shadowRate;
        public double pnl;
        public double totalProfit;
        public double maxDrawdown;
        public double avgProfitPerBet;
        public double expectedBreakEvenHitRate;
        public int aPlusCount;
        public double aPlusHitRate;
        public double biasScoreAvg;
        public double calibrationError;
        public double brierScore;
        public int teacherMatrixSampleCount;
        public int teacherMatrixProfileCount;
        public double teacherMatrixAvgHitRate;
        public double teacherMatrixAvgConfidence;
        public int reasonSampleCount;
        public int reasonProfileCount;
        public double reasonAvgEdge;
        public int elite45PassCount;
        public String status;
        public String summaryText;
        public String topProfiles;
        public String topMissReasons;
        public String topHitReasons;
        public String promotedProfiles;
        public String rejectedProfiles;
        public String learningSummary;
        public String teacherMatrixSummary;
        public String reasonSummary;
        public String elite45Summary;
        public String holdReasonText;
        public String hitReasonText;
        public String missReasonText;
        public String lastBestText;
        public String lastStepSummary;
        public long createdAt;

        public ManualDeepBacktestReport() {
            reportId = "";
            taskName = "";
            mode = AppConfig.BACKTEST_MODE_FULL;
            windowSize = 0;
            startIssue = "";
            endIssue = "";
            startIndex = 0;
            endIndex = 0;
            minTrain = 0;
            maxTest = 0;
            totalHistory = 0;
            totalSteps = 0;
            testCount = 0;
            betCount = 0;
            holdCount = 0;
            shadowCount = 0;
            hitCount = 0;
            missCount = 0;
            winCount = 0;
            loseCount = 0;
            hitRate = 0.0d;
            frequency = 0.0d;
            betRate = 0.0d;
            holdRate = 0.0d;
            shadowRate = 0.0d;
            pnl = 0.0d;
            totalProfit = 0.0d;
            maxDrawdown = 0.0d;
            avgProfitPerBet = 0.0d;
            expectedBreakEvenHitRate = 0.0d;
            aPlusCount = 0;
            aPlusHitRate = 0.0d;
            biasScoreAvg = 0.0d;
            calibrationError = 0.0d;
            brierScore = 0.0d;
            teacherMatrixSampleCount = 0;
            teacherMatrixProfileCount = 0;
            teacherMatrixAvgHitRate = 0.0d;
            teacherMatrixAvgConfidence = 0.0d;
            reasonSampleCount = 0;
            reasonProfileCount = 0;
            reasonAvgEdge = 0.0d;
            elite45PassCount = 0;
            status = "";
            summaryText = "";
            topProfiles = "";
            topMissReasons = "";
            topHitReasons = "";
            promotedProfiles = "";
            rejectedProfiles = "";
            learningSummary = "";
            teacherMatrixSummary = "";
            reasonSummary = "";
            elite45Summary = "";
            holdReasonText = "";
            hitReasonText = "";
            missReasonText = "";
            lastBestText = "";
            lastStepSummary = "";
            createdAt = 0L;
        }

        public void recalc() {
            hitRate = betCount > 0 ? ((double) hitCount) / betCount : 0.0d;
            frequency = testCount > 0 ? ((double) betCount) / testCount : 0.0d;
            betRate = frequency;
            holdRate = testCount > 0 ? ((double) holdCount) / testCount : 0.0d;
            shadowRate = testCount > 0 ? ((double) shadowCount) / testCount : 0.0d;
            pnl = totalProfit;
        }

        public TeacherSnapshot toTeacherSnapshot() {
            TeacherSnapshot snapshot = new TeacherSnapshot();
            snapshot.sourceType = "MANUAL_BACKTEST";
            snapshot.windowSize = AppConfig.normalizeManualBacktestWindow(windowSize);
            snapshot.totalHistory = totalHistory;
            snapshot.totalSteps = totalSteps;
            snapshot.testCount = testCount;
            snapshot.startIssue = startIssue;
            snapshot.endIssue = endIssue;
            snapshot.betCount = betCount;
            snapshot.hitCount = hitCount;
            snapshot.missCount = missCount;
            snapshot.holdCount = holdCount;
            snapshot.shadowCount = shadowCount;
            snapshot.hitRate = hitRate;
            snapshot.winRate = hitRate;
            snapshot.frequency = frequency;
            snapshot.pnl = pnl;
            snapshot.totalProfit = totalProfit;
            snapshot.maxDrawdown = maxDrawdown;
            snapshot.avgProfitPerBet = avgProfitPerBet;
            snapshot.calibrationError = calibrationError;
            snapshot.brierScore = brierScore;
            snapshot.aPlusCount = aPlusCount;
            snapshot.aPlusHitRate = aPlusHitRate;
            snapshot.biasScoreAvg = biasScoreAvg;
            snapshot.teacherMatrixSampleCount = teacherMatrixSampleCount;
            snapshot.teacherMatrixProfileCount = teacherMatrixProfileCount;
            snapshot.teacherMatrixAvgHitRate = teacherMatrixAvgHitRate;
            snapshot.teacherMatrixAvgConfidence = teacherMatrixAvgConfidence;
            snapshot.teacherMatrixSummary = teacherMatrixSummary;
            snapshot.reasonSampleCount = reasonSampleCount;
            snapshot.reasonProfileCount = reasonProfileCount;
            snapshot.reasonAvgEdge = reasonAvgEdge;
            snapshot.reasonSummary = reasonSummary;
            snapshot.elite45PassCount = elite45PassCount;
            snapshot.elite45Summary = elite45Summary;
            snapshot.topProfiles = topProfiles;
            snapshot.topMissReasons = topMissReasons;
            snapshot.topHitReasons = topHitReasons;
            snapshot.promotedProfiles = promotedProfiles;
            snapshot.rejectedProfiles = rejectedProfiles;
            snapshot.learningSummary = learningSummary;
            snapshot.highWinShapeText = hitReasonText;
            snapshot.highRiskShapeText = missReasonText;
            snapshot.status = AppConfig.BACKTEST_STATUS_OK;
            snapshot.createdAt = System.currentTimeMillis();
            snapshot.finishedAt = snapshot.createdAt;
            snapshot.snapshotId = "TS_" + snapshot.createdAt;
            snapshot.recalc();
            snapshot.summaryText = snapshot.buildSummaryText();
            return snapshot;
        }
    }

    public static final class RuntimeLearningSnapshot {
        public String snapshotId;
        public long createdAt;
        public String sourceType;
        public String targetIssue;
        public String profileKey;
        public int hit;
        public String affectedModule;
        public String moduleAction;
        public double moduleWeightDelta;
        public double confidence;
        public String mainReasonCode;
        public String summaryText;
        public int totalSample;
        public double totalHitRate;
        public int recentSample;
        public double recentHitRate;
        public double decayWeight;
        public int failCount;
        public int staleFlag;
        public int recoverFlag;
        public int teacherMatrixSampleCount;
        public double teacherMatrixHitRate;
        public double teacherMatrixConfidence;
        public int reasonSampleCount;
        public double reasonEdge;
        public double reasonConfidence;
        public String profileSummary;
        public String gateAdjustSummary;
        public String probabilityAdjustSummary;
        public String auditSummary;
        public String biasSummary;
        public String promotionSummary;
        public String aPlusSummary;
        public String teacherMatrixSummary;
        public String reasonSummary;
        public String activeJson;
        public String learningSummaryText;

        public RuntimeLearningSnapshot() {
            snapshotId = "";
            createdAt = 0L;
            sourceType = "";
            targetIssue = "";
            profileKey = "";
            hit = 0;
            affectedModule = "";
            moduleAction = "";
            moduleWeightDelta = 0.0d;
            confidence = 0.0d;
            mainReasonCode = "";
            summaryText = "";
            totalSample = 0;
            totalHitRate = 0.0d;
            recentSample = 0;
            recentHitRate = 0.0d;
            decayWeight = 1.0d;
            failCount = 0;
            staleFlag = 0;
            recoverFlag = 0;
            teacherMatrixSampleCount = 0;
            teacherMatrixHitRate = 0.0d;
            teacherMatrixConfidence = 0.0d;
            reasonSampleCount = 0;
            reasonEdge = 0.0d;
            reasonConfidence = 0.0d;
            profileSummary = "";
            gateAdjustSummary = "";
            probabilityAdjustSummary = "";
            auditSummary = "";
            biasSummary = "";
            promotionSummary = "";
            aPlusSummary = "";
            teacherMatrixSummary = "";
            reasonSummary = "";
            activeJson = "";
            learningSummaryText = "";
        }
    }

    public static final class SelfDiagnosisResult {
        public String diagnosisId;
        public long createdAt;
        public String level;
        public String mainCode;
        public String mainReasonCode;
        public String mainProblem;
        public String affectedModule;
        public String suggestedAction;
        public String shortText;
        public String summaryText;
        public String diagnosisText;
        public String repairAdvice;
        public int historyCount;
        public int validDrawCount;
        public int invalidDrawCount;
        public int duplicateIssueCount;
        public String latestIssue;
        public int candidateCount;
        public int validCandidateCount;
        public int invalidCandidateCount;
        public int overGateCandidateCount;
        public int blockedCandidateCount;
        public int realTradeCount;
        public int shadowTradeCount;
        public int holdTradeCount;
        public int tradeHitCount;
        public int tradeMissCount;
        public int mixedStatCount;
        public int profileCount;
        public int activeProfileCount;
        public int staleProfileCount;
        public int weakProfileCount;
        public int teacherMatrixSampleCount;
        public int teacherMatrixProfileCount;
        public int teacherMatrixStrongCount;
        public int teacherMatrixRiskCount;
        public int reasonSampleCount;
        public int reasonProfileCount;
        public int falseReasonCount;
        public int elite45PassCount;
        public int elite45BlockCount;
        public int fatalCount;
        public int badCount;
        public int warnCount;
        public String dataText;
        public String candidateText;
        public String tradeText;
        public String profileText;
        public String teacherMatrixText;
        public String reasonText;
        public String elite45Text;
        public String decisionText;
        public String runtimeText;
        public String bestCandidateText;

        public SelfDiagnosisResult() {
            diagnosisId = "";
            createdAt = 0L;
            level = AppConfig.DIAG_LEVEL_OK;
            mainCode = AppConfig.DIAG_NORMAL;
            mainReasonCode = AppConfig.DIAG_NORMAL;
            mainProblem = AppConfig.DIAG_NORMAL;
            affectedModule = AppConfig.ATTR_MODULE_UNKNOWN;
            suggestedAction = "";
            shortText = "";
            summaryText = "";
            diagnosisText = "";
            repairAdvice = "";
            historyCount = 0;
            validDrawCount = 0;
            invalidDrawCount = 0;
            duplicateIssueCount = 0;
            latestIssue = "";
            candidateCount = 0;
            validCandidateCount = 0;
            invalidCandidateCount = 0;
            overGateCandidateCount = 0;
            blockedCandidateCount = 0;
            realTradeCount = 0;
            shadowTradeCount = 0;
            holdTradeCount = 0;
            tradeHitCount = 0;
            tradeMissCount = 0;
            mixedStatCount = 0;
            profileCount = 0;
            activeProfileCount = 0;
            staleProfileCount = 0;
            weakProfileCount = 0;
            teacherMatrixSampleCount = 0;
            teacherMatrixProfileCount = 0;
            teacherMatrixStrongCount = 0;
            teacherMatrixRiskCount = 0;
            reasonSampleCount = 0;
            reasonProfileCount = 0;
            falseReasonCount = 0;
            elite45PassCount = 0;
            elite45BlockCount = 0;
            fatalCount = 0;
            badCount = 0;
            warnCount = 0;
            dataText = "";
            candidateText = "";
            tradeText = "";
            profileText = "";
            teacherMatrixText = "";
            reasonText = "";
            elite45Text = "";
            decisionText = "";
            runtimeText = "";
            bestCandidateText = "";
        }

        public boolean isDanger() {
            return AppConfig.DIAG_LEVEL_DANGER.equals(level)
                    || AppConfig.DIAG_LEVEL_CRITICAL.equals(level)
                    || "BAD".equals(level)
                    || "FATAL".equals(level);
        }
    }

    public static final class PreDrawReasonSample {
        public long id;
        public String sampleKey;
        public String sourceType;
        public String baseIssue;
        public String targetIssue;
        public long createdAt;
        public int targetIndex;

        public int position;
        public int number;
        public int actualNumber;
        public int k3Mask;
        public int label;
        public int hit;

        public String stateKey;
        public String stateMode;
        public String trendKey;
        public String shapeKey;
        public String driftKey;
        public String breakKey;
        public String recoveryKey;
        public String transitionKey;
        public String relationKey;

        public String reasonFingerprint;
        public String reasonAtomText;
        public String reasonComboKey;
        public String validLevel;

        public double finalP;
        public double dynamicGate;
        public double edge;

        public double reasonScore;
        public double reasonEdge;
        public double hitSimilarity;
        public double missSimilarity;
        public double contradictionScore;
        public double causalLift;
        public double causalConfidence;
        public double comboConflict;
        public double comboRoleDiversity;
        public double driftRisk;
        public double breakRisk;
        public double recoveryScore;
        public double riskScore;

        public int validReasonCount;
        public int falseReasonCount;
        public int forwardSample;
        public int forwardHit;
        public double forwardHitRate;
        public double wilsonScore;

        public String k3Role;
        public String mainReasonCode;
        public String whyHitText;
        public String whyMissText;
        public String nextAdvice;
        public String summaryText;
        public int saved;

        public PreDrawReasonSample() {
            id = 0L;
            sampleKey = "";
            sourceType = SOURCE_TYPE_UNKNOWN;
            baseIssue = "";
            targetIssue = "";
            createdAt = 0L;
            targetIndex = -1;
            position = 0;
            number = 0;
            actualNumber = 0;
            k3Mask = 0;
            label = 0;
            hit = 0;
            stateKey = "";
            stateMode = AppConfig.MODE_UNKNOWN;
            trendKey = "";
            shapeKey = "";
            driftKey = "";
            breakKey = "";
            recoveryKey = "";
            transitionKey = "";
            relationKey = "";
            reasonFingerprint = "";
            reasonAtomText = "";
            reasonComboKey = "";
            validLevel = REASON_LEVEL_C_EXPLAIN_ONLY;
            finalP = 0.0d;
            dynamicGate = AppConfig.DEFAULT_DYNAMIC_GATE;
            edge = 0.0d;
            reasonScore = 0.0d;
            reasonEdge = 0.0d;
            hitSimilarity = 0.0d;
            missSimilarity = 0.0d;
            contradictionScore = 0.0d;
            causalLift = 0.0d;
            causalConfidence = 0.0d;
            comboConflict = 0.0d;
            comboRoleDiversity = 0.0d;
            driftRisk = 0.0d;
            breakRisk = 0.0d;
            recoveryScore = 0.0d;
            riskScore = 0.0d;
            validReasonCount = 0;
            falseReasonCount = 0;
            forwardSample = 0;
            forwardHit = 0;
            forwardHitRate = 0.0d;
            wilsonScore = 0.0d;
            k3Role = K3_ROLE_UNKNOWN;
            mainReasonCode = AppConfig.MISS_UNKNOWN;
            whyHitText = "";
            whyMissText = "";
            nextAdvice = "";
            summaryText = "";
            saved = 0;
        }

        public void rebuildKey() {
            if (reasonFingerprint == null || reasonFingerprint.length() == 0) {
                reasonFingerprint =
                        buildReasonFingerprint(
                                stateKey,
                                trendKey,
                                shapeKey,
                                driftKey,
                                recoveryKey,
                                relationKey,
                                mainReasonCode);
            }
            sampleKey =
                    buildPreDrawReasonSampleKey(targetIssue, position, number, reasonFingerprint);
        }

        public void recalc() {
            finalP = AppConfig.clampProbability(finalP);
            dynamicGate = AppConfig.clampGate(dynamicGate);
            edge = finalP - dynamicGate;
            reasonScore = AppConfig.clampProbability(reasonScore);
            reasonEdge = reasonScore - REASON_HIT_RATE_BASELINE;
            hitSimilarity = AppConfig.clampProbability(hitSimilarity);
            missSimilarity = AppConfig.clampProbability(missSimilarity);
            contradictionScore = AppConfig.clampProbability(contradictionScore);
            causalLift = AppConfig.limitAdjust(causalLift, 1.0d);
            causalConfidence = AppConfig.clampProbability(causalConfidence);
            comboConflict = AppConfig.clampProbability(comboConflict);
            comboRoleDiversity = AppConfig.clampProbability(comboRoleDiversity);
            riskScore = AppConfig.clampProbability(riskScore);
            forwardHitRate =
                    forwardSample > 0 ? ((double) forwardHit) / ((double) forwardSample) : 0.0d;
            wilsonScore = calcWilsonLowerBound(forwardHit, forwardSample);
            validLevel = buildReasonLevel();
            rebuildKey();
            summaryText = buildSummaryText();
        }

        private String buildReasonLevel() {
            if (forwardSample < REASON_MIN_SAMPLE) return REASON_LEVEL_C_EXPLAIN_ONLY;
            if (falseReasonCount > 0
                    || missSimilarity > ELITE45_MAX_MISS_SIMILARITY
                    || contradictionScore > ELITE45_MAX_CONTRADICTION_SCORE)
                return REASON_LEVEL_E_FALSE;
            if (wilsonScore >= REASON_A_WILSON_MIN
                    && causalConfidence >= 0.55d
                    && reasonEdge >= ELITE45_MIN_REASON_EDGE) return REASON_LEVEL_A_VALID;
            if (wilsonScore >= REASON_B_WILSON_MIN && reasonEdge > 0.0d)
                return REASON_LEVEL_B_USEFUL;
            if (forwardSample >= REASON_USEFUL_SAMPLE && forwardHitRate < REASON_FALSE_WILSON_MAX)
                return REASON_LEVEL_E_FALSE;
            return REASON_LEVEL_C_EXPLAIN_ONLY;
        }

        public boolean isPositive() {
            return label == 1 || hit == 1;
        }

        public boolean isUsableForElite45() {
            recalc();
            return isReasonLevelBoostable(validLevel)
                    && reasonEdge >= ELITE45_MIN_REASON_EDGE
                    && hitSimilarity >= ELITE45_MIN_HIT_SIMILARITY
                    && missSimilarity <= ELITE45_MAX_MISS_SIMILARITY
                    && contradictionScore <= ELITE45_MAX_CONTRADICTION_SCORE
                    && falseReasonCount <= ELITE45_MAX_FALSE_REASON_COUNT;
        }

        public String buildSummaryText() {
            StringBuilder sb = new StringBuilder();
            sb.append("开奖前原因样本 P").append(position);
            sb.append(" N").append(UiFormatter.number(number));
            sb.append(isPositive() ? " 正样本" : " 负样本");
            sb.append("，评级=").append(validLevel);
            sb.append("，原因边际=").append(UiFormatter.signedProbability(reasonEdge));
            sb.append("，命中相似=").append(UiFormatter.probability(hitSimilarity));
            sb.append("，未中相似=").append(UiFormatter.probability(missSimilarity));
            sb.append("，前推样本=").append(forwardSample);
            sb.append("，前推命中=").append(UiFormatter.probability(forwardHitRate));
            return sb.toString();
        }
    }

    public static final class ReasonContrastProfile {
        public long id;
        public String profileKey;
        public String reasonFingerprint;
        public String stateKey;
        public int position;
        public int number;

        public int sampleCount;
        public int hitCount;
        public int missCount;
        public int recentSample;
        public int recentHit;
        public int recentMiss;

        public double hitRate;
        public double missRate;
        public double recentHitRate;
        public double reasonEdge;
        public double hitSimilarity;
        public double missSimilarity;
        public double contradictionScore;
        public double wilsonScore;
        public double confidence;

        public String validLevel;
        public String whyText;
        public String summaryText;
        public long updatedAt;

        public ReasonContrastProfile() {
            id = 0L;
            profileKey = "";
            reasonFingerprint = "";
            stateKey = "";
            position = 0;
            number = 0;
            sampleCount = 0;
            hitCount = 0;
            missCount = 0;
            recentSample = 0;
            recentHit = 0;
            recentMiss = 0;
            hitRate = 0.0d;
            missRate = 0.0d;
            recentHitRate = 0.0d;
            reasonEdge = 0.0d;
            hitSimilarity = 0.0d;
            missSimilarity = 0.0d;
            contradictionScore = 0.0d;
            wilsonScore = 0.0d;
            confidence = 0.0d;
            validLevel = REASON_LEVEL_C_EXPLAIN_ONLY;
            whyText = "";
            summaryText = "";
            updatedAt = 0L;
        }

        public void rebuildKey() {
            profileKey =
                    buildReasonContrastProfileKey(reasonFingerprint, stateKey, position, number);
        }

        public void recalc() {
            if (sampleCount <= 0) sampleCount = hitCount + missCount;
            if (missCount <= 0 && sampleCount >= hitCount) missCount = sampleCount - hitCount;
            if (recentSample <= 0) recentSample = recentHit + recentMiss;

            hitRate = sampleCount > 0 ? ((double) hitCount) / ((double) sampleCount) : 0.0d;
            missRate = sampleCount > 0 ? ((double) missCount) / ((double) sampleCount) : 0.0d;
            recentHitRate =
                    recentSample > 0 ? ((double) recentHit) / ((double) recentSample) : hitRate;
            reasonEdge = hitRate - REASON_HIT_RATE_BASELINE;
            wilsonScore = calcWilsonLowerBound(hitCount, sampleCount);

            hitSimilarity =
                    AppConfig.clampProbability(
                            hitRate * 0.60d + recentHitRate * 0.25d + wilsonScore * 0.15d);
            missSimilarity =
                    AppConfig.clampProbability(missRate * 0.70d + (1.0d - recentHitRate) * 0.30d);
            contradictionScore = AppConfig.clampProbability(missSimilarity - hitSimilarity + 0.30d);

            double sampleScore =
                    sampleCount >= REASON_STABLE_SAMPLE
                            ? 1.0d
                            : ((double) sampleCount) / ((double) REASON_STABLE_SAMPLE);
            confidence =
                    AppConfig.clampProbability(
                            sampleScore * 0.55d
                                    + Math.abs(reasonEdge) * 1.20d
                                    + wilsonScore * 0.30d);

            if (sampleCount < REASON_MIN_SAMPLE) validLevel = REASON_LEVEL_C_EXPLAIN_ONLY;
            else if (wilsonScore >= REASON_A_WILSON_MIN && reasonEdge >= ELITE45_MIN_REASON_EDGE)
                validLevel = REASON_LEVEL_A_VALID;
            else if (wilsonScore >= REASON_B_WILSON_MIN && reasonEdge > 0.0d)
                validLevel = REASON_LEVEL_B_USEFUL;
            else if (hitRate <= REASON_FALSE_WILSON_MAX
                    || contradictionScore > ELITE45_MAX_CONTRADICTION_SCORE)
                validLevel = REASON_LEVEL_E_FALSE;
            else validLevel = REASON_LEVEL_C_EXPLAIN_ONLY;

            rebuildKey();
            summaryText = buildSummaryText();
        }

        public boolean canSupportElite45() {
            recalc();
            return isReasonLevelBoostable(validLevel)
                    && reasonEdge >= ELITE45_MIN_REASON_EDGE
                    && hitSimilarity >= ELITE45_MIN_HIT_SIMILARITY
                    && missSimilarity <= ELITE45_MAX_MISS_SIMILARITY;
        }

        public String buildSummaryText() {
            return "原因对照画像：样本="
                    + sampleCount
                    + "，命中率="
                    + UiFormatter.probability(hitRate)
                    + "，近期="
                    + UiFormatter.probability(recentHitRate)
                    + "，Wilson="
                    + UiFormatter.probability(wilsonScore)
                    + "，评级="
                    + validLevel;
        }
    }

    public static final class ReasonCausalProfile {
        public long id;
        public String causalKey;
        public String reasonAtom;
        public String reasonComboKey;

        public int sampleCount;
        public int hitCount;
        public int missCount;
        public int forwardSample;
        public int forwardHit;
        public int forwardMiss;

        public double hitRate;
        public double forwardHitRate;
        public double baselineHitRate;
        public double causalLift;
        public double causalConfidence;
        public double decayScore;
        public double riskScore;
        public double probAdjust;
        public double gateAdjust;

        public String validLevel;
        public String summaryText;
        public long updatedAt;

        public ReasonCausalProfile() {
            id = 0L;
            causalKey = "";
            reasonAtom = "";
            reasonComboKey = "";
            sampleCount = 0;
            hitCount = 0;
            missCount = 0;
            forwardSample = 0;
            forwardHit = 0;
            forwardMiss = 0;
            hitRate = 0.0d;
            forwardHitRate = 0.0d;
            baselineHitRate = REASON_HIT_RATE_BASELINE;
            causalLift = 0.0d;
            causalConfidence = 0.0d;
            decayScore = 0.0d;
            riskScore = 0.0d;
            probAdjust = 0.0d;
            gateAdjust = 0.0d;
            validLevel = REASON_LEVEL_C_EXPLAIN_ONLY;
            summaryText = "";
            updatedAt = 0L;
        }

        public void rebuildKey() {
            causalKey = buildReasonCausalKey(reasonAtom, reasonComboKey);
        }

        public void recalc() {
            if (sampleCount <= 0) sampleCount = hitCount + missCount;
            if (forwardSample <= 0) forwardSample = forwardHit + forwardMiss;

            hitRate = sampleCount > 0 ? ((double) hitCount) / ((double) sampleCount) : 0.0d;
            forwardHitRate =
                    forwardSample > 0 ? ((double) forwardHit) / ((double) forwardSample) : hitRate;
            causalLift = ((hitRate * 0.55d) + (forwardHitRate * 0.45d)) - baselineHitRate;

            double sampleScore =
                    sampleCount >= REASON_STRONG_SAMPLE
                            ? 1.0d
                            : ((double) sampleCount) / ((double) REASON_STRONG_SAMPLE);
            double forwardScore =
                    forwardSample >= REASON_FORWARD_VALIDATE_MIN
                            ? 1.0d
                            : ((double) forwardSample) / ((double) REASON_FORWARD_VALIDATE_MIN);

            causalConfidence =
                    AppConfig.clampProbability(
                            sampleScore * 0.45d
                                    + forwardScore * 0.35d
                                    + Math.abs(causalLift) * 1.20d);
            decayScore = AppConfig.clampProbability(hitRate - forwardHitRate);
            riskScore =
                    AppConfig.clampProbability(
                            (causalLift < 0.0d ? -causalLift : 0.0d) + decayScore * 0.70d);

            probAdjust =
                    AppConfig.limitAdjust(
                            causalLift * 0.30d, AppConfig.TEACHER_MATRIX_MAX_PROB_ADJUST);
            gateAdjust = 0.0d;

            if (causalLift >= ELITE45_MIN_REASON_EDGE && causalConfidence >= 0.55d) {
                gateAdjust -= AppConfig.TEACHER_MATRIX_MAX_GATE_REDUCE * 0.45d;
            } else if (causalLift < 0.0d || riskScore > 0.45d) {
                gateAdjust += AppConfig.TEACHER_MATRIX_MAX_GATE_BOOST * 0.45d;
            }

            gateAdjust = AppConfig.limitAdjust(gateAdjust, AppConfig.TEACHER_MATRIX_MAX_GATE_BOOST);

            if (sampleCount < REASON_MIN_SAMPLE) validLevel = REASON_LEVEL_C_EXPLAIN_ONLY;
            else if (causalLift >= ELITE45_MIN_REASON_EDGE && causalConfidence >= 0.60d)
                validLevel = REASON_LEVEL_A_VALID;
            else if (causalLift > 0.0d && causalConfidence >= 0.40d)
                validLevel = REASON_LEVEL_B_USEFUL;
            else if (causalLift < 0.0d || riskScore > 0.55d) validLevel = REASON_LEVEL_E_FALSE;
            else validLevel = REASON_LEVEL_C_EXPLAIN_ONLY;

            rebuildKey();
            summaryText = buildSummaryText();
        }

        public boolean canSupportElite45() {
            recalc();
            return isReasonLevelBoostable(validLevel)
                    && causalLift >= ELITE45_MIN_REASON_EDGE
                    && causalConfidence >= 0.50d
                    && riskScore <= ELITE45_MAX_NEGATIVE_REASON_SCORE;
        }

        public String buildSummaryText() {
            return "原因因果画像：样本="
                    + sampleCount
                    + "，命中率="
                    + UiFormatter.probability(hitRate)
                    + "，前推="
                    + UiFormatter.probability(forwardHitRate)
                    + "，lift="
                    + UiFormatter.signedProbability(causalLift)
                    + "，置信="
                    + UiFormatter.probability(causalConfidence)
                    + "，评级="
                    + validLevel;
        }
    }

    public static final class FalseStrongProfile {
        public long id;
        public String falseKey;
        public String stateKey;
        public int position;
        public int number;
        public int k3Mask;
        public String reasonFingerprint;

        public int sampleCount;
        public int missCount;
        public int hitCount;
        public int consecutiveMiss;
        public int cooldownUntilIndex;
        public long cooldownUntilTime;

        public double missRate;
        public double hitRate;
        public double falseStrongScore;
        public double riskPenalty;
        public double gateBoost;

        public String status;
        public String summaryText;
        public long updatedAt;

        public FalseStrongProfile() {
            id = 0L;
            falseKey = "";
            stateKey = "";
            position = 0;
            number = 0;
            k3Mask = 0;
            reasonFingerprint = "";
            sampleCount = 0;
            missCount = 0;
            hitCount = 0;
            consecutiveMiss = 0;
            cooldownUntilIndex = -1;
            cooldownUntilTime = 0L;
            missRate = 0.0d;
            hitRate = 0.0d;
            falseStrongScore = 0.0d;
            riskPenalty = 0.0d;
            gateBoost = 0.0d;
            status = AppConfig.TEACHER_MATRIX_STATUS_NONE;
            summaryText = "";
            updatedAt = 0L;
        }

        public void rebuildKey() {
            falseKey = buildFalseStrongKey(stateKey, position, number, k3Mask, reasonFingerprint);
        }

        public void recalc() {
            if (sampleCount <= 0) sampleCount = hitCount + missCount;

            hitRate = sampleCount > 0 ? ((double) hitCount) / ((double) sampleCount) : 0.0d;
            missRate = sampleCount > 0 ? ((double) missCount) / ((double) sampleCount) : 0.0d;

            falseStrongScore =
                    AppConfig.clampProbability(
                            missRate * 0.55d + ((double) consecutiveMiss) * 0.12d);
            riskPenalty = 0.0d;
            gateBoost = 0.0d;

            if (consecutiveMiss >= SAME_FINGERPRINT_MISS_FALSE_STRONG) {
                status = REASON_LEVEL_E_FALSE;
                riskPenalty = FINGERPRINT_FALSE_STRONG_RISK_PENALTY;
                gateBoost = 0.060d;
            } else if (consecutiveMiss >= SAME_FINGERPRINT_MISS_COOLDOWN) {
                status = BLOCK_FINGERPRINT_COOLING;
                riskPenalty = FINGERPRINT_COOLDOWN_RISK_PENALTY;
                gateBoost = 0.040d;
            } else if (consecutiveMiss >= SAME_FINGERPRINT_MISS_SHADOW) {
                status = AppConfig.DECISION_SHADOW;
                riskPenalty = 0.018d;
                gateBoost = 0.020d;
            } else {
                status = AppConfig.TEACHER_MATRIX_STATUS_READY;
            }

            riskPenalty = AppConfig.clampProbability(riskPenalty);
            gateBoost = AppConfig.limitAdjust(gateBoost, 0.080d);
            rebuildKey();
            summaryText = buildSummaryText();
        }

        public boolean isCooling(int currentIndex, long now) {
            if (cooldownUntilIndex >= 0 && currentIndex >= 0 && currentIndex <= cooldownUntilIndex)
                return true;
            return cooldownUntilTime > 0L && now > 0L && now <= cooldownUntilTime;
        }

        public String buildSummaryText() {
            return "同指纹冷却：样本="
                    + sampleCount
                    + "，连错="
                    + consecutiveMiss
                    + "，未中率="
                    + UiFormatter.probability(missRate)
                    + "，状态="
                    + status
                    + "，风险惩罚="
                    + UiFormatter.probability(riskPenalty);
        }
    }

    public static final class ComboRoleProfile {
        public long id;
        public String comboRoleKey;
        public String stateKey;
        public int position;
        public int k3Mask;

        public int mainNumber;
        public int supportNumber;
        public int defenseNumber;

        public int sampleCount;
        public int hitCount;
        public int mainHitCount;
        public int supportHitCount;
        public int defenseHitCount;
        public int missCount;

        public double hitRate;
        public double mainHitRate;
        public double supportHitRate;
        public double defenseHitRate;
        public double roleDiversity;
        public double comboConflict;
        public double roleScore;
        public double confidence;

        public String status;
        public String summaryText;
        public long updatedAt;

        public ComboRoleProfile() {
            id = 0L;
            comboRoleKey = "";
            stateKey = "";
            position = 0;
            k3Mask = 0;
            mainNumber = 0;
            supportNumber = 0;
            defenseNumber = 0;
            sampleCount = 0;
            hitCount = 0;
            mainHitCount = 0;
            supportHitCount = 0;
            defenseHitCount = 0;
            missCount = 0;
            hitRate = 0.0d;
            mainHitRate = 0.0d;
            supportHitRate = 0.0d;
            defenseHitRate = 0.0d;
            roleDiversity = 0.0d;
            comboConflict = 0.0d;
            roleScore = 0.0d;
            confidence = 0.0d;
            status = AppConfig.TEACHER_MATRIX_STATUS_NONE;
            summaryText = "";
            updatedAt = 0L;
        }

        public void rebuildKey() {
            comboRoleKey = buildComboRoleKey(stateKey, position, k3Mask);
        }

        public void recalc() {
            if (sampleCount <= 0) sampleCount = hitCount + missCount;
            if (missCount <= 0 && sampleCount >= hitCount) missCount = sampleCount - hitCount;

            hitRate = sampleCount > 0 ? ((double) hitCount) / ((double) sampleCount) : 0.0d;
            mainHitRate = sampleCount > 0 ? ((double) mainHitCount) / ((double) sampleCount) : 0.0d;
            supportHitRate =
                    sampleCount > 0 ? ((double) supportHitCount) / ((double) sampleCount) : 0.0d;
            defenseHitRate =
                    sampleCount > 0 ? ((double) defenseHitCount) / ((double) sampleCount) : 0.0d;

            double maxRole = Math.max(mainHitRate, Math.max(supportHitRate, defenseHitRate));
            double minRole = Math.min(mainHitRate, Math.min(supportHitRate, defenseHitRate));
            roleDiversity = AppConfig.clampProbability(1.0d - Math.abs(maxRole - minRole));
            comboConflict =
                    AppConfig.clampProbability(
                            (double) missCount / (double) Math.max(1, sampleCount));

            double sampleScore =
                    sampleCount >= AppConfig.TEACHER_MATRIX_COMBO_STRONG_SAMPLE
                            ? 1.0d
                            : ((double) sampleCount)
                                    / ((double) AppConfig.TEACHER_MATRIX_COMBO_STRONG_SAMPLE);
            confidence =
                    AppConfig.clampProbability(
                            sampleScore * 0.55d + roleDiversity * 0.25d + hitRate * 0.20d);
            roleScore =
                    AppConfig.clampProbability(
                            hitRate * 0.45d + roleDiversity * 0.30d + confidence * 0.25d);

            if (sampleCount < AppConfig.TEACHER_MATRIX_COMBO_MIN_SAMPLE)
                status = AppConfig.TEACHER_MATRIX_STATUS_WEAK;
            else if (comboConflict > ELITE45_MAX_COMBO_CONFLICT)
                status = AppConfig.TEACHER_MATRIX_STATUS_RISK;
            else if (hitRate >= AppConfig.TEACHER_MATRIX_STRONG_HIT_RATE
                    && roleDiversity >= ELITE45_MIN_COMBO_ROLE_DIVERSITY)
                status = AppConfig.TEACHER_MATRIX_STATUS_STRONG;
            else status = AppConfig.TEACHER_MATRIX_STATUS_READY;

            rebuildKey();
            summaryText = buildSummaryText();
        }

        public boolean canSupportElite45() {
            recalc();
            return sampleCount >= AppConfig.TEACHER_MATRIX_COMBO_MIN_SAMPLE
                    && roleDiversity >= ELITE45_MIN_COMBO_ROLE_DIVERSITY
                    && comboConflict <= ELITE45_MAX_COMBO_CONFLICT
                    && !AppConfig.TEACHER_MATRIX_STATUS_RISK.equals(status);
        }

        public String roleText() {
            StringBuilder sb = new StringBuilder();
            sb.append("主攻").append(UiFormatter.number(mainNumber));
            sb.append("，辅助").append(UiFormatter.number(supportNumber));
            sb.append("，防守").append(UiFormatter.number(defenseNumber));
            return sb.toString();
        }

        public String buildSummaryText() {
            return "K3角色画像："
                    + roleText()
                    + "，样本="
                    + sampleCount
                    + "，组合命中="
                    + UiFormatter.probability(hitRate)
                    + "，角色分散="
                    + UiFormatter.probability(roleDiversity)
                    + "，冲突="
                    + UiFormatter.probability(comboConflict)
                    + "，状态="
                    + status;
        }
    }

    public static final class Elite45Result {
        public String targetIssue;
        public int position;
        public int k3Mask;

        public int passed;
        public String status;
        public String blockCode;

        public double finalP;
        public double dynamicGate;
        public double eliteGate;
        public double reasonEdge;
        public double hitSimilarity;
        public double missSimilarity;
        public double contradictionScore;
        public double comboConflict;
        public double comboRoleDiversity;
        public double driftRisk;
        public double breakRisk;
        public double riskScore;

        public int validReasonCount;
        public int falseReasonCount;
        public int fingerprintCooling;

        public String reasonLevel;
        public String reasonFingerprint;
        public String k3RoleSummary;
        public String summaryText;

        public Elite45Result() {
            targetIssue = "";
            position = 0;
            k3Mask = 0;
            passed = 0;
            status = BLOCK_ELITE45_NOT_PASS;
            blockCode = BLOCK_ELITE45_NOT_PASS;
            finalP = 0.0d;
            dynamicGate = AppConfig.DEFAULT_DYNAMIC_GATE;
            eliteGate = ELITE45_GATE_DEFAULT;
            reasonEdge = 0.0d;
            hitSimilarity = 0.0d;
            missSimilarity = 0.0d;
            contradictionScore = 0.0d;
            comboConflict = 0.0d;
            comboRoleDiversity = 0.0d;
            driftRisk = 0.0d;
            breakRisk = 0.0d;
            riskScore = 0.0d;
            validReasonCount = 0;
            falseReasonCount = 0;
            fingerprintCooling = 0;
            reasonLevel = REASON_LEVEL_C_EXPLAIN_ONLY;
            reasonFingerprint = "";
            k3RoleSummary = "";
            summaryText = "";
        }

        public void judge() {
            passed = 0;
            status = BLOCK_ELITE45_NOT_PASS;
            blockCode = BLOCK_ELITE45_NOT_PASS;

            eliteGate =
                    clampElite45Gate(
                            ELITE45_GATE_DEFAULT
                                    + buildElite45ReasonGateAdjust(
                                            reasonEdge,
                                            hitSimilarity,
                                            missSimilarity,
                                            falseReasonCount,
                                            comboConflict,
                                            fingerprintCooling == 1));

            if (fingerprintCooling == 1) {
                blockCode = BLOCK_FINGERPRINT_COOLING;
            } else if (finalP < ELITE45_MIN_FINAL_P || finalP < eliteGate) {
                blockCode = AppConfig.BLOCK_LOW_PROBABILITY;
            } else if (reasonEdge < ELITE45_MIN_REASON_EDGE) {
                blockCode = BLOCK_REASON_EDGE_WEAK;
            } else if (hitSimilarity < ELITE45_MIN_HIT_SIMILARITY) {
                blockCode = BLOCK_HIT_SIMILARITY_WEAK;
            } else if (missSimilarity > ELITE45_MAX_MISS_SIMILARITY) {
                blockCode = BLOCK_MISS_SIMILARITY_HIGH;
            } else if (validReasonCount < ELITE45_MIN_VALID_REASON_COUNT) {
                blockCode = BLOCK_VALID_REASON_WEAK;
            } else if (falseReasonCount > ELITE45_MAX_FALSE_REASON_COUNT) {
                blockCode = BLOCK_FALSE_REASON;
            } else if (comboConflict > ELITE45_MAX_COMBO_CONFLICT) {
                blockCode = BLOCK_COMBO_CONFLICT;
            } else if (comboRoleDiversity < ELITE45_MIN_COMBO_ROLE_DIVERSITY) {
                blockCode = BLOCK_COMBO_CONFLICT;
            } else if (driftRisk > ELITE45_MAX_DRIFT_RISK) {
                blockCode = AppConfig.BLOCK_DRIFT_HIGH;
            } else if (breakRisk > ELITE45_MAX_BREAK_RISK) {
                blockCode = AppConfig.BLOCK_BREAK_HIGH;
            } else if (contradictionScore > ELITE45_MAX_CONTRADICTION_SCORE) {
                blockCode = BLOCK_FALSE_REASON;
            } else {
                passed = 1;
                status = AppConfig.TEACHER_MATRIX_STATUS_ELITE;
                blockCode = "";
            }

            summaryText = buildSummaryText();
        }

        public String buildSummaryText() {
            StringBuilder sb = new StringBuilder();
            sb.append("Elite45审核：").append(passed == 1 ? "通过" : "不通过");
            if (blockCode != null && blockCode.length() > 0) {
                sb.append("，拦截=").append(blockCode);
            }
            sb.append("，finalP=").append(UiFormatter.probability(finalP));
            sb.append("，eliteGate=").append(UiFormatter.probability(eliteGate));
            sb.append("，原因边际=").append(UiFormatter.signedProbability(reasonEdge));
            sb.append("，命中相似=").append(UiFormatter.probability(hitSimilarity));
            sb.append("，未中相似=").append(UiFormatter.probability(missSimilarity));
            sb.append("，有效原因=").append(validReasonCount);
            sb.append("，虚强原因=").append(falseReasonCount);
            if (k3RoleSummary != null && k3RoleSummary.length() > 0) {
                sb.append("，").append(k3RoleSummary);
            }
            return sb.toString();
        }
    }

    public static final class Elite45AuditLog {
        public long id;
        public String auditKey;
        public String targetIssue;
        public long createdAt;

        public int position;
        public int n1;
        public int n2;
        public int n3;
        public int k3Mask;

        public int passed;
        public String status;
        public String blockCode;

        public double finalP;
        public double dynamicGate;
        public double eliteGate;
        public double reasonEdge;
        public double hitSimilarity;
        public double missSimilarity;
        public double contradictionScore;
        public double comboConflict;
        public double comboRoleDiversity;
        public double driftRisk;
        public double breakRisk;
        public double riskScore;

        public int validReasonCount;
        public int falseReasonCount;
        public int fingerprintCooling;

        public String reasonLevel;
        public String reasonFingerprint;
        public String k3RoleSummary;
        public String summaryText;

        public Elite45AuditLog() {
            id = 0L;
            auditKey = "";
            targetIssue = "";
            createdAt = 0L;
            position = 0;
            n1 = 0;
            n2 = 0;
            n3 = 0;
            k3Mask = 0;
            passed = 0;
            status = BLOCK_ELITE45_NOT_PASS;
            blockCode = BLOCK_ELITE45_NOT_PASS;
            finalP = 0.0d;
            dynamicGate = AppConfig.DEFAULT_DYNAMIC_GATE;
            eliteGate = ELITE45_GATE_DEFAULT;
            reasonEdge = 0.0d;
            hitSimilarity = 0.0d;
            missSimilarity = 0.0d;
            contradictionScore = 0.0d;
            comboConflict = 0.0d;
            comboRoleDiversity = 0.0d;
            driftRisk = 0.0d;
            breakRisk = 0.0d;
            riskScore = 0.0d;
            validReasonCount = 0;
            falseReasonCount = 0;
            fingerprintCooling = 0;
            reasonLevel = REASON_LEVEL_C_EXPLAIN_ONLY;
            reasonFingerprint = "";
            k3RoleSummary = "";
            summaryText = "";
            initMainChainBridgeFields(this);
        }

        public void setNumbers(int a, int b, int c) {
            n1 = a;
            n2 = b;
            n3 = c;
            k3Mask = buildK3Mask(a, b, c);
        }

        public void rebuildKey() {
            auditKey = buildElite45AuditKey(targetIssue, position, k3Mask);
        }

        public void fromResult(Elite45Result r, int a, int b, int c) {
            if (r == null) return;
            targetIssue = r.targetIssue;
            position = r.position;
            setNumbers(a, b, c);
            passed = r.passed;
            status = r.status;
            blockCode = r.blockCode;
            finalP = r.finalP;
            dynamicGate = r.dynamicGate;
            eliteGate = r.eliteGate;
            reasonEdge = r.reasonEdge;
            hitSimilarity = r.hitSimilarity;
            missSimilarity = r.missSimilarity;
            contradictionScore = r.contradictionScore;
            comboConflict = r.comboConflict;
            comboRoleDiversity = r.comboRoleDiversity;
            driftRisk = r.driftRisk;
            breakRisk = r.breakRisk;
            riskScore = r.riskScore;
            validReasonCount = r.validReasonCount;
            falseReasonCount = r.falseReasonCount;
            fingerprintCooling = r.fingerprintCooling;
            reasonLevel = r.reasonLevel;
            reasonFingerprint = r.reasonFingerprint;
            k3RoleSummary = r.k3RoleSummary;
            summaryText = r.summaryText;
            createdAt = System.currentTimeMillis();
            rebuildKey();
        }
    }

    // ========== 17文件升级新增：引擎间传输DTO（不影响既有持久化模型） ==========

    public static final class PreDrawFeature {
        public String baseIssue;
        public String targetIssue;
        public String actionAdvice;
        public String clusterKey;
        public String crowdBucket;
        public String densityBucket;
        public String dynamicWindowStateKey;
        public String factorKey;
        public String featureKey;
        public String featureSummary;
        public String gapBucket;
        public String healthBucket;
        public String hotColdBucket;
        public String key;
        public String neighborTailBucket;
        public String omissionBucket;
        public String parityBucket;
        public String reasonText;
        public String repeatBucket;
        public String riskBucket;
        public String ruleKey;
        public String sameClassKey;
        public String sourceKey;
        public String sourceType;
        public String stateKey;
        public String status;
        public String summaryText;
        public String syncBucket;
        public String timingBucket;
        public String transitionBucket;
        public String trendBucket;
        public String version;
        public String windowStateKey;

        public int position;
        public int number;
        public int currentGap;
        public int historySize;
        public int noFutureLeakOk;
        public int recentSampleCount;
        public int sampleCount;
        public int similarSampleCount;
        public int visibleDrawCount;

        public long createdAt;

        public double antiEvidenceScore;
        public double avgGap;
        public double biasFitScore;
        public double breakDepth;
        public double candidatePrior;
        public double coldRepairPower;
        public double confidence;
        public double counterEvidenceScore;
        public double coverageQuality;
        public double crossPositionCrowd;
        public double crossPositionFlow;
        public double crossPositionSync;
        public double densityRisk;
        public double densityScore;
        public double evidenceScore;
        public double falseRecoveryRisk;
        public double falseSeedRisk;
        public double featureScore;
        public double freq12;
        public double freq120;
        public double freq16;
        public double freq24;
        public double freq240;
        public double freq32;
        public double freq360;
        public double freq48;
        public double freq500;
        public double freq64;
        public double freq96;
        public double freqAccel;
        public double freqSlope;
        public double gapPhase;
        public double gapPressure;
        public double gapZ;
        public double healthScore;
        public double highLowBias;
        public double hotExhaustion;
        public double hotPressure;
        public double midVsLong;
        public double neighborPressure;
        public double neighborSupport;
        public double netEvidenceScore;
        public double numberEdge;
        public double numberSupplyScore;
        public double numberSupplyStrength;
        public double oddEvenBias;
        public double omissionPressure;
        public double pairCoSupport;
        public double pairConflict;
        public double pairSample;
        public double positionCongestion;
        public double positionDrift;
        public double positionEdge;
        public double positionEntropy;
        public double positionHealth;
        public double positionNeedScore;
        public double positionNeedStrength;
        public double positionNoiseScore;
        public double positionStrengthSlope;
        public double positionTop3Mass;
        public double recentHitSupport;
        public double recentLosePressure;
        public double recentValidity;
        public double recoveryQuality;
        public double recoverySpeed;
        public double repeatRhythm;
        public double repeatRisk;
        public double returnZoneScore;
        public double riskScore;
        public double sameNumberCrowd;
        public double sampleReliability;
        public double seedScore;
        public double shortVsMid;
        public double similarHitRate;
        public double supportScore;
        public double tailPressure;
        public double tailSupport;
        public double thirdQuality;
        public double timingOffset;
        public double timingRisk;
        public double timingScore;
        public double transitionBaselineRate;
        public double transitionConditionalRate;
        public double transitionConflict;
        public double transitionLift;
        public double transitionSample;
        public double trendConsistency;
        public double entropyAfterRecovery;

        public PreDrawFeature() {
            baseIssue = "";
            targetIssue = "";
            actionAdvice = "";
            clusterKey = "";
            crowdBucket = "";
            densityBucket = "";
            dynamicWindowStateKey = "";
            factorKey = "";
            featureKey = "";
            featureSummary = "";
            gapBucket = "";
            healthBucket = "";
            hotColdBucket = "";
            key = "";
            neighborTailBucket = "";
            omissionBucket = "";
            parityBucket = "";
            reasonText = "";
            repeatBucket = "";
            riskBucket = "";
            ruleKey = "";
            sameClassKey = "";
            sourceKey = "";
            sourceType = "";
            stateKey = "";
            status = "";
            summaryText = "";
            syncBucket = "";
            timingBucket = "";
            transitionBucket = "";
            trendBucket = "";
            version = "";
            windowStateKey = "";
            position = 0;
            number = 0;
            currentGap = 0;
            historySize = 0;
            noFutureLeakOk = 0;
            recentSampleCount = 0;
            sampleCount = 0;
            similarSampleCount = 0;
            visibleDrawCount = 0;
            createdAt = 0L;
            antiEvidenceScore = 0.0d;
            avgGap = 0.0d;
            biasFitScore = 0.0d;
            breakDepth = 0.0d;
            candidatePrior = 0.0d;
            coldRepairPower = 0.0d;
            confidence = 0.0d;
            counterEvidenceScore = 0.0d;
            coverageQuality = 0.0d;
            crossPositionCrowd = 0.0d;
            crossPositionFlow = 0.0d;
            crossPositionSync = 0.0d;
            densityRisk = 0.0d;
            densityScore = 0.0d;
            evidenceScore = 0.0d;
            falseRecoveryRisk = 0.0d;
            falseSeedRisk = 0.0d;
            featureScore = 0.0d;
            freq12 = 0.0d;
            freq120 = 0.0d;
            freq16 = 0.0d;
            freq24 = 0.0d;
            freq240 = 0.0d;
            freq32 = 0.0d;
            freq360 = 0.0d;
            freq48 = 0.0d;
            freq500 = 0.0d;
            freq64 = 0.0d;
            freq96 = 0.0d;
            freqAccel = 0.0d;
            freqSlope = 0.0d;
            gapPhase = 0.0d;
            gapPressure = 0.0d;
            gapZ = 0.0d;
            healthScore = 0.0d;
            highLowBias = 0.0d;
            hotExhaustion = 0.0d;
            hotPressure = 0.0d;
            midVsLong = 0.0d;
            neighborPressure = 0.0d;
            neighborSupport = 0.0d;
            netEvidenceScore = 0.0d;
            numberEdge = 0.0d;
            numberSupplyScore = 0.0d;
            numberSupplyStrength = 0.0d;
            oddEvenBias = 0.0d;
            omissionPressure = 0.0d;
            pairCoSupport = 0.0d;
            pairConflict = 0.0d;
            pairSample = 0.0d;
            positionCongestion = 0.0d;
            positionDrift = 0.0d;
            positionEdge = 0.0d;
            positionEntropy = 0.0d;
            positionHealth = 0.0d;
            positionNeedScore = 0.0d;
            positionNeedStrength = 0.0d;
            positionNoiseScore = 0.0d;
            positionStrengthSlope = 0.0d;
            positionTop3Mass = 0.0d;
            recentHitSupport = 0.0d;
            recentLosePressure = 0.0d;
            recentValidity = 0.0d;
            recoveryQuality = 0.0d;
            recoverySpeed = 0.0d;
            repeatRhythm = 0.0d;
            repeatRisk = 0.0d;
            returnZoneScore = 0.0d;
            riskScore = 0.0d;
            sameNumberCrowd = 0.0d;
            sampleReliability = 0.0d;
            seedScore = 0.0d;
            shortVsMid = 0.0d;
            similarHitRate = 0.0d;
            supportScore = 0.0d;
            tailPressure = 0.0d;
            tailSupport = 0.0d;
            thirdQuality = 0.0d;
            timingOffset = 0.0d;
            timingRisk = 0.0d;
            timingScore = 0.0d;
            transitionBaselineRate = 0.0d;
            transitionConditionalRate = 0.0d;
            transitionConflict = 0.0d;
            transitionLift = 0.0d;
            transitionSample = 0.0d;
            trendConsistency = 0.0d;
            entropyAfterRecovery = 0.0d;
        }
    }

    public static final class PreDrawReason {
        public String baseIssue;
        public String targetIssue;
        public int position;
        public int number;

        public String macroState;
        public String trendPhase;
        public String driftPhase;
        public String breakPhase;
        public String recoveryPhase;
        public String positionPhase;
        public String numberPhase;
        public String relationPhase;
        public String riskPhase;

        public String reasonAtoms;
        public String positiveAtoms;
        public String negativeAtoms;
        public String contradictionAtoms;

        public String reasonFingerprint;
        public String reasonTextPreDraw;

        public int validReasonCount;
        public int falseReasonCount;
        public double positiveReasonScore;
        public double negativeReasonScore;
        public double contradictionScore;

        public long createdAt;

        public PreDrawReason() {
            baseIssue = "";
            targetIssue = "";
            position = 0;
            number = 0;
            macroState = "";
            trendPhase = "";
            driftPhase = "";
            breakPhase = "";
            recoveryPhase = "";
            positionPhase = "";
            numberPhase = "";
            relationPhase = "";
            riskPhase = "";
            reasonAtoms = "";
            positiveAtoms = "";
            negativeAtoms = "";
            contradictionAtoms = "";
            reasonFingerprint = "";
            reasonTextPreDraw = "";
            validReasonCount = 0;
            falseReasonCount = 0;
            positiveReasonScore = 0.0d;
            negativeReasonScore = 0.0d;
            contradictionScore = 0.0d;
            createdAt = 0L;
        }
    }

    public static final class ContrastiveProfile {
        public String reasonFingerprint;
        public String stateKey;
        public int position;
        public int number;

        public int hitCount;
        public int missCount;
        public int sampleCount;

        public String hitFactorList;
        public String missFactorList;

        public double hitSimilarityScore;
        public double missSimilarityScore;
        public double reasonEdge;

        public double positiveFactorScore;
        public double negativeFactorScore;
        public double contradictionScore;

        public String differenceSignature;
        public String summaryText;
        public long updatedAt;

        public ContrastiveProfile() {
            reasonFingerprint = "";
            stateKey = "";
            position = 0;
            number = 0;
            hitCount = 0;
            missCount = 0;
            sampleCount = 0;
            hitFactorList = "";
            missFactorList = "";
            hitSimilarityScore = 0.0d;
            missSimilarityScore = 0.0d;
            reasonEdge = 0.0d;
            positiveFactorScore = 0.0d;
            negativeFactorScore = 0.0d;
            contradictionScore = 0.0d;
            differenceSignature = "";
            summaryText = "";
            updatedAt = 0L;
        }
    }

    public static final class K3RoleCombo {
        public String stateKey;
        public String targetIssue;
        public int position;

        public int mainNumber;
        public int supportNumber;
        public int defenseNumber;

        public int k3Mask;

        public double roleDiversity;
        public double pairConflict;
        public double coverageBalance;
        public double comboForwardHitRate;
        public double comboWilsonLower;

        public String comboValidLevel;
        public String comboReasonText;

        public K3RoleCombo() {
            stateKey = "";
            targetIssue = "";
            position = 0;
            mainNumber = 0;
            supportNumber = 0;
            defenseNumber = 0;
            k3Mask = 0;
            roleDiversity = 0.0d;
            pairConflict = 0.0d;
            coverageBalance = 0.0d;
            comboForwardHitRate = 0.0d;
            comboWilsonLower = 0.0d;
            comboValidLevel = REASON_LEVEL_C_EXPLAIN_ONLY;
            comboReasonText = "";
        }

        public void setNumbers(int main, int support, int defense) {
            mainNumber = main;
            supportNumber = support;
            defenseNumber = defense;
            k3Mask = buildK3Mask(main, support, defense);
        }
    }

    public static final class MissFingerprint {
        public String fingerprintKey;
        public String stateKey;
        public int position;
        public int k3Mask;
        public String reasonFingerprint;

        public int missCountRecent;
        public long cooldownUntilIssue;
        public double riskPenalty;
        public String falseReason;

        public MissFingerprint() {
            fingerprintKey = "";
            stateKey = "";
            position = 0;
            k3Mask = 0;
            reasonFingerprint = "";
            missCountRecent = 0;
            cooldownUntilIssue = 0L;
            riskPenalty = 0.0d;
            falseReason = "";
        }
    }

    public static final class BacktestSummary {
        public long id;
        public String summaryKey;
        public String mode;
        public int window;
        public String startIssue;
        public String endIssue;
        public int testCount;
        public int betCount;
        public int hitCount;
        public int missCount;
        public double hitRate;
        public double frequency;
        public double pnl;
        public double maxDrawdown;
        public long createdAt;
        public String summaryText;

        public BacktestSummary() {
            id = 0L;
            summaryKey = "";
            mode = AppConfig.BACKTEST_MODE_FAST;
            window = 0;
            startIssue = "";
            endIssue = "";
            testCount = 0;
            betCount = 0;
            hitCount = 0;
            missCount = 0;
            hitRate = 0.0d;
            frequency = 0.0d;
            pnl = 0.0d;
            maxDrawdown = 0.0d;
            createdAt = 0L;
            summaryText = "";
        }

        public void recalc() {
            hitRate = betCount > 0 ? ((double) hitCount) / ((double) betCount) : 0.0d;
            frequency = testCount > 0 ? ((double) betCount) / ((double) testCount) : 0.0d;
        }
    }

    // ========== V20 动态高胜率因链系统：核心模型骨架 ==========
    // 说明：
    // 1. 这一段只新增模型，不删除旧模型，避免旧文件编译失败。
    // 2. 12个新增引擎、ProbabilityEngine、DynamicGateEngine、DecisionGateEngine 后续都走这些结构。
    // 3. 所有真实BET最终仍然必须交给 DecisionGateEngine 裁决，模型本身不直接下单。

    public static final double V20_GRADE_S_MIN_SCORE = 0.720d;
    public static final double V20_GRADE_A_MIN_SCORE = 0.580d;
    public static final double V20_GRADE_B_MIN_SCORE = 0.450d;
    public static final double V20_FALSE_STRONG_LIGHT = 0.350d;
    public static final double V20_FALSE_STRONG_HARD = 0.620d;
    public static final double V20_POSITION_HEALTH_MIN = 0.330d;
    public static final double V20_SHADOW_HIT_30_MIN = 0.370d;
    public static final double V20_SHADOW_HIT_60_MIN = 0.350d;
    public static final double V20_SHADOW_HIT_120_MIN = 0.330d;
    public static final int V20_SHADOW_MAX_LOSE = 7;

    public static enum CandidateGrade {
        S,
        A,
        B,
        C,
        UNKNOWN
    }

    public static CandidateGrade normalizeCandidateGrade(CandidateGrade grade) {
        return grade == null ? CandidateGrade.UNKNOWN : grade;
    }

    public static CandidateGrade normalizeCandidateGrade(String grade) {
        String g = AppConfig.normalizeToken(grade);
        if ("S".equals(g)) return CandidateGrade.S;
        if ("A".equals(g)) return CandidateGrade.A;
        if ("B".equals(g)) return CandidateGrade.B;
        if ("C".equals(g)) return CandidateGrade.C;
        return CandidateGrade.UNKNOWN;
    }

    public static String gradeText(CandidateGrade grade) {
        grade = normalizeCandidateGrade(grade);
        if (CandidateGrade.S == grade) return "S";
        if (CandidateGrade.A == grade) return "A";
        if (CandidateGrade.B == grade) return "B";
        if (CandidateGrade.C == grade) return "C";
        return "UNKNOWN";
    }

    public static boolean isGradeRealBetCapable(CandidateGrade grade) {
        grade = normalizeCandidateGrade(grade);
        return CandidateGrade.S == grade || CandidateGrade.A == grade || CandidateGrade.B == grade;
    }

    public static boolean isGradeStrong(CandidateGrade grade) {
        grade = normalizeCandidateGrade(grade);
        return CandidateGrade.S == grade || CandidateGrade.A == grade;
    }

    public static double safeRate(int hit, int sample) {
        if (sample <= 0) {
            return 0.0d;
        }
        return AppConfig.clampProbability(((double) hit) / ((double) sample));
    }

    public static double scoreFromEdge(double edge) {
        if (Double.isNaN(edge) || Double.isInfinite(edge)) {
            return 0.0d;
        }
        return AppConfig.clampProbability(0.50d + edge);
    }

    public static final class CandidateK3 {
        public long id;
        public String baseIssue;
        public String targetIssue;
        public long createdAt;

        public int position;
        public int n1;
        public int n2;
        public int n3;
        public int k3Mask;

        public int rank;
        public int rankGroup;
        public double rawScore;
        public double baseScore;
        public double baseK3P;
        public double rawK3P;
        public double finalP;
        public double dynamicGate;
        public double edge;
        public double confidence;
        public double riskScore;

        public double gap12;
        public double gap23;
        public double margin;

        public CandidateGrade grade;
        public String decisionAction;

        public double finalCauseScore;
        public double positionEdge;
        public double numberTeacherEdge;
        public double comboRoleEdge;
        public double thirdGuardEdge;
        public double rankCounterEdge;
        public double sourceHealthEdge;
        public double stateHealthEdge;
        public double cycleHealthEdge;
        public double seedSimilarity;
        public double shadowHealth;

        public double falseStrongRisk;
        public double ruleDecayRisk;
        public double sameClassLoseRisk;
        public double guardDragRisk;
        public double positionWrongRisk;

        public boolean positionHealthy;
        public boolean numberTeacherPass;
        public boolean rankPass;
        public boolean thirdGuardPass;
        public boolean shadowPass;
        public boolean falseStrongFrozen;
        public boolean sameClassFrozen;
        public boolean sourceHealthy;
        public boolean ruleHealthy;
        public boolean stateHealthy;
        public boolean riskOk;

        public String stateKey;
        public String sourceKey;
        public String ruleKey;
        public String sameClassKey;
        public String falseStrongKey;
        public String reasonAtoms;
        public String sourceVotes;
        public String riskTags;
        public String decisionReason;

        public CandidateK3() {
            id = 0L;
            baseIssue = "";
            targetIssue = "";
            createdAt = 0L;
            position = 0;
            n1 = 0;
            n2 = 0;
            n3 = 0;
            k3Mask = 0;
            rank = 0;
            rankGroup = 0;
            rawScore = 0.0d;
            baseScore = 0.0d;
            baseK3P = AppConfig.RANDOM_K3_BASELINE;
            rawK3P = AppConfig.RANDOM_K3_BASELINE;
            finalP = 0.0d;
            dynamicGate = AppConfig.DEFAULT_DYNAMIC_GATE;
            edge = 0.0d;
            confidence = 0.0d;
            riskScore = 0.0d;
            gap12 = 0.0d;
            gap23 = 0.0d;
            margin = 0.0d;
            grade = CandidateGrade.UNKNOWN;
            decisionAction = AppConfig.DECISION_HOLD;
            finalCauseScore = 0.0d;
            positionEdge = 0.0d;
            numberTeacherEdge = 0.0d;
            comboRoleEdge = 0.0d;
            thirdGuardEdge = 0.0d;
            rankCounterEdge = 0.0d;
            sourceHealthEdge = 0.0d;
            stateHealthEdge = 0.0d;
            cycleHealthEdge = 0.0d;
            seedSimilarity = 0.0d;
            shadowHealth = 0.0d;
            falseStrongRisk = 0.0d;
            ruleDecayRisk = 0.0d;
            sameClassLoseRisk = 0.0d;
            guardDragRisk = 0.0d;
            positionWrongRisk = 0.0d;
            positionHealthy = false;
            numberTeacherPass = false;
            rankPass = false;
            thirdGuardPass = false;
            shadowPass = false;
            falseStrongFrozen = false;
            sameClassFrozen = false;
            sourceHealthy = false;
            ruleHealthy = false;
            stateHealthy = false;
            riskOk = false;
            stateKey = "";
            sourceKey = "";
            ruleKey = "";
            sameClassKey = "";
            falseStrongKey = "";
            reasonAtoms = "";
            sourceVotes = "";
            riskTags = "";
            decisionReason = "";
        }

        public void setNumbers(int a, int b, int c) {
            n1 = a;
            n2 = b;
            n3 = c;
            k3Mask = buildK3Mask(a, b, c);
        }

        public void normalize() {
            k3Mask = buildK3Mask(n1, n2, n3);
            baseK3P = AppConfig.clampProbability(baseK3P);
            rawK3P = AppConfig.clampProbability(rawK3P);
            finalP = AppConfig.clampProbability(finalP);
            dynamicGate = AppConfig.clampGate(dynamicGate);
            edge = finalP - dynamicGate;
            confidence = AppConfig.clampProbability(confidence);
            riskScore = AppConfig.clampProbability(riskScore);
            finalCauseScore = AppConfig.clampProbability(finalCauseScore);
            shadowHealth = AppConfig.clampProbability(shadowHealth);
            falseStrongRisk = AppConfig.clampProbability(falseStrongRisk);
            grade = normalizeCandidateGrade(grade);
            decisionAction = AppConfig.normalizeDecision(decisionAction);
            if (createdAt <= 0L) {
                createdAt = System.currentTimeMillis();
            }
        }

        public boolean isLegal() {
            return AppConfig.isPlayRuleLegal(position, AppConfig.FIXED_K, n1, n2, n3);
        }

        public boolean isS() {
            return CandidateGrade.S == normalizeCandidateGrade(grade);
        }

        public boolean isA() {
            return CandidateGrade.A == normalizeCandidateGrade(grade);
        }

        public boolean isB() {
            return CandidateGrade.B == normalizeCandidateGrade(grade);
        }

        public boolean isC() {
            return CandidateGrade.C == normalizeCandidateGrade(grade);
        }

        public boolean canEnterDecisionGate() {
            normalize();
            if (!isLegal()) return false;
            if (falseStrongFrozen || sameClassFrozen) return false;
            if (CandidateGrade.C == grade || CandidateGrade.UNKNOWN == grade) return false;
            return true;
        }

        public ComboCandidate toComboCandidate() {
            normalize();
            ComboCandidate c = new ComboCandidate();
            c.baseIssue = baseIssue;
            c.targetIssue = targetIssue;
            c.createdAt = createdAt;
            c.position = position;
            c.setNumbers(n1, n2, n3);
            c.rank = rank;
            c.rawP = rawK3P;
            c.finalP = finalP;
            c.dynamicGate = dynamicGate;
            c.edge = edge;
            c.rankScore = rawScore;
            c.finalRankScore = finalCauseScore;
            c.stateKey = stateKey;
            c.reasonSummary = decisionReason;
            c.reasonScore = finalCauseScore;
            c.reasonEdge = positionEdge + numberTeacherEdge + rankCounterEdge + thirdGuardEdge;
            c.falseStrongRisk = falseStrongRisk;
            c.teacherMatrixScore = numberTeacherEdge;
            c.teacherMatrixConfidence = confidence;
            c.teacherMatrixStatus =
                    numberTeacherPass
                            ? AppConfig.TEACHER_MATRIX_STATUS_READY
                            : AppConfig.TEACHER_MATRIX_STATUS_WEAK;
            c.elite45Passed = isS() || isA() ? 1 : 0;
            c.elite45Status = gradeText(grade);
            c.elite45Summary = decisionReason;
            return c;
        }

        public Decision toDecision(String actionValue) {
            normalize();
            ComboCandidate c = toComboCandidate();
            return c.toDecision(actionValue);
        }

        public CandidateK3 copy() {
            CandidateK3 c = new CandidateK3();
            c.id = id;
            c.baseIssue = baseIssue;
            c.targetIssue = targetIssue;
            c.createdAt = createdAt;
            c.position = position;
            c.n1 = n1;
            c.n2 = n2;
            c.n3 = n3;
            c.k3Mask = k3Mask;
            c.rank = rank;
            c.rankGroup = rankGroup;
            c.rawScore = rawScore;
            c.baseScore = baseScore;
            c.baseK3P = baseK3P;
            c.rawK3P = rawK3P;
            c.finalP = finalP;
            c.dynamicGate = dynamicGate;
            c.edge = edge;
            c.confidence = confidence;
            c.riskScore = riskScore;
            c.gap12 = gap12;
            c.gap23 = gap23;
            c.margin = margin;
            c.grade = grade;
            c.decisionAction = decisionAction;
            c.finalCauseScore = finalCauseScore;
            c.positionEdge = positionEdge;
            c.numberTeacherEdge = numberTeacherEdge;
            c.comboRoleEdge = comboRoleEdge;
            c.thirdGuardEdge = thirdGuardEdge;
            c.rankCounterEdge = rankCounterEdge;
            c.sourceHealthEdge = sourceHealthEdge;
            c.stateHealthEdge = stateHealthEdge;
            c.cycleHealthEdge = cycleHealthEdge;
            c.seedSimilarity = seedSimilarity;
            c.shadowHealth = shadowHealth;
            c.falseStrongRisk = falseStrongRisk;
            c.ruleDecayRisk = ruleDecayRisk;
            c.sameClassLoseRisk = sameClassLoseRisk;
            c.guardDragRisk = guardDragRisk;
            c.positionWrongRisk = positionWrongRisk;
            c.positionHealthy = positionHealthy;
            c.numberTeacherPass = numberTeacherPass;
            c.rankPass = rankPass;
            c.thirdGuardPass = thirdGuardPass;
            c.shadowPass = shadowPass;
            c.falseStrongFrozen = falseStrongFrozen;
            c.sameClassFrozen = sameClassFrozen;
            c.sourceHealthy = sourceHealthy;
            c.ruleHealthy = ruleHealthy;
            c.stateHealthy = stateHealthy;
            c.riskOk = riskOk;
            c.stateKey = stateKey;
            c.sourceKey = sourceKey;
            c.ruleKey = ruleKey;
            c.sameClassKey = sameClassKey;
            c.falseStrongKey = falseStrongKey;
            c.reasonAtoms = reasonAtoms;
            c.sourceVotes = sourceVotes;
            c.riskTags = riskTags;
            c.decisionReason = decisionReason;
            return c;
        }

        public String buildShortText() {
            StringBuilder sb = new StringBuilder();
            sb.append("P").append(position);
            sb.append(" ").append(maskToText(k3Mask));
            sb.append(" ").append(gradeText(grade));
            sb.append(" finalP=").append(UiFormatter.probability(finalP));
            sb.append(" gate=").append(UiFormatter.probability(dynamicGate));
            sb.append(" cause=").append(UiFormatter.probability(finalCauseScore));
            if (decisionReason != null && decisionReason.length() > 0) {
                sb.append(" ").append(decisionReason);
            }
            return sb.toString();
        }
    }

    public static final class CauseChain {
        public String targetIssue;
        public int position;
        public int k3Mask;
        public String stateKey;
        public String sourceKey;
        public String ruleKey;
        public String sameClassKey;

        public double positionEdge;
        public double numberTeacherEdge;
        public double comboRoleEdge;
        public double thirdGuardEdge;
        public double rankCounterfactualEdge;
        public double sourceHealthEdge;
        public double stateHealthEdge;
        public double cycleHealthEdge;
        public double seedSimilarity;
        public double shadowHealth;

        public double falseStrongRisk;
        public double ruleDecayRisk;
        public double sameClassLoseRisk;
        public double guardDragRisk;
        public double positionWrongRisk;

        public double finalCauseScore;
        public CandidateGrade grade;
        public String decisionReason;
        public String riskTags;

        public CauseChain() {
            targetIssue = "";
            position = 0;
            k3Mask = 0;
            stateKey = "";
            sourceKey = "";
            ruleKey = "";
            sameClassKey = "";
            positionEdge = 0.0d;
            numberTeacherEdge = 0.0d;
            comboRoleEdge = 0.0d;
            thirdGuardEdge = 0.0d;
            rankCounterfactualEdge = 0.0d;
            sourceHealthEdge = 0.0d;
            stateHealthEdge = 0.0d;
            cycleHealthEdge = 0.0d;
            seedSimilarity = 0.0d;
            shadowHealth = 0.0d;
            falseStrongRisk = 0.0d;
            ruleDecayRisk = 0.0d;
            sameClassLoseRisk = 0.0d;
            guardDragRisk = 0.0d;
            positionWrongRisk = 0.0d;
            finalCauseScore = 0.0d;
            grade = CandidateGrade.UNKNOWN;
            decisionReason = "";
            riskTags = "";
        }

        public void recalc() {
            double positive =
                    positionEdge
                            + numberTeacherEdge
                            + comboRoleEdge
                            + thirdGuardEdge
                            + rankCounterfactualEdge
                            + sourceHealthEdge
                            + stateHealthEdge
                            + cycleHealthEdge
                            + seedSimilarity
                            + shadowHealth;

            double negative =
                    falseStrongRisk
                            + ruleDecayRisk
                            + sameClassLoseRisk
                            + guardDragRisk
                            + positionWrongRisk;

            finalCauseScore = AppConfig.clampProbability(0.50d + positive - negative);

            if (finalCauseScore >= V20_GRADE_S_MIN_SCORE) {
                grade = CandidateGrade.S;
            } else if (finalCauseScore >= V20_GRADE_A_MIN_SCORE) {
                grade = CandidateGrade.A;
            } else if (finalCauseScore >= V20_GRADE_B_MIN_SCORE) {
                grade = CandidateGrade.B;
            } else {
                grade = CandidateGrade.C;
            }

            if (decisionReason == null || decisionReason.length() == 0) {
                decisionReason = buildReasonText();
            }
        }

        public boolean hasStrongCause() {
            recalc();
            return CandidateGrade.S == grade || CandidateGrade.A == grade;
        }

        public boolean hasBettableCause() {
            recalc();
            return isGradeRealBetCapable(grade);
        }

        public String buildReasonText() {
            StringBuilder sb = new StringBuilder();
            sb.append("因链等级=").append(gradeText(grade));
            sb.append("，因链分=").append(UiFormatter.probability(finalCauseScore));
            sb.append("，位置边际=").append(UiFormatter.signedProbability(positionEdge));
            sb.append("，号码教师=").append(UiFormatter.signedProbability(numberTeacherEdge));
            sb.append("，Rank反事实=").append(UiFormatter.signedProbability(rankCounterfactualEdge));
            sb.append("，第三保护=").append(UiFormatter.signedProbability(thirdGuardEdge));
            sb.append("，SHADOW健康=").append(UiFormatter.probability(shadowHealth));
            sb.append("，假强风险=").append(UiFormatter.probability(falseStrongRisk));
            if (riskTags != null && riskTags.length() > 0) {
                sb.append("，风险=").append(riskTags);
            }
            return sb.toString();
        }

        public void applyToCandidate(CandidateK3 c) {
            if (c == null) return;
            recalc();
            c.positionEdge = positionEdge;
            c.numberTeacherEdge = numberTeacherEdge;
            c.comboRoleEdge = comboRoleEdge;
            c.thirdGuardEdge = thirdGuardEdge;
            c.rankCounterEdge = rankCounterfactualEdge;
            c.sourceHealthEdge = sourceHealthEdge;
            c.stateHealthEdge = stateHealthEdge;
            c.cycleHealthEdge = cycleHealthEdge;
            c.seedSimilarity = seedSimilarity;
            c.shadowHealth = shadowHealth;
            c.falseStrongRisk = falseStrongRisk;
            c.ruleDecayRisk = ruleDecayRisk;
            c.sameClassLoseRisk = sameClassLoseRisk;
            c.guardDragRisk = guardDragRisk;
            c.positionWrongRisk = positionWrongRisk;
            c.finalCauseScore = finalCauseScore;
            c.grade = grade;
            c.decisionReason = decisionReason;
            c.riskTags = riskTags;
            if (c.stateKey == null || c.stateKey.length() == 0) c.stateKey = stateKey;
            if (c.sourceKey == null || c.sourceKey.length() == 0) c.sourceKey = sourceKey;
            if (c.ruleKey == null || c.ruleKey.length() == 0) c.ruleKey = ruleKey;
            if (c.sameClassKey == null || c.sameClassKey.length() == 0)
                c.sameClassKey = sameClassKey;
            c.normalize();
        }
    }

    public static final class PositionHealth {
        public String targetIssue;
        public int position;
        public String stateKey;
        public String cycleKey;

        public int sample30;
        public int hit30;
        public int sample60;
        public int hit60;
        public int sample120;
        public int hit120;

        public double posRecent30HitRate;
        public double posRecent60HitRate;
        public double posRecent120HitRate;
        public double posShadowHitRate;
        public int posMaxLoseStreak;
        public double posFalseStrongRate;
        public double posRecoveryRate;
        public double posCycleHealth;
        public double posStateHealth;

        public double positionEdge;
        public double positionRisk;
        public double healthScore;
        public boolean positionHealthy;
        public boolean positionFreeze;
        public String summaryText;
        public long updatedAt;

        public PositionHealth() {
            targetIssue = "";
            position = 0;
            stateKey = "";
            cycleKey = "";
            sample30 = 0;
            hit30 = 0;
            sample60 = 0;
            hit60 = 0;
            sample120 = 0;
            hit120 = 0;
            posRecent30HitRate = 0.0d;
            posRecent60HitRate = 0.0d;
            posRecent120HitRate = 0.0d;
            posShadowHitRate = 0.0d;
            posMaxLoseStreak = 0;
            posFalseStrongRate = 0.0d;
            posRecoveryRate = 0.0d;
            posCycleHealth = 0.0d;
            posStateHealth = 0.0d;
            positionEdge = 0.0d;
            positionRisk = 0.0d;
            healthScore = 0.0d;
            positionHealthy = false;
            positionFreeze = false;
            summaryText = "";
            updatedAt = 0L;
        }

        public void recalc() {
            posRecent30HitRate = safeRate(hit30, sample30);
            posRecent60HitRate = safeRate(hit60, sample60);
            posRecent120HitRate = safeRate(hit120, sample120);

            double rateScore =
                    posRecent30HitRate * 0.40d
                            + posRecent60HitRate * 0.35d
                            + posRecent120HitRate * 0.25d;
            double health =
                    rateScore * 0.45d
                            + posShadowHitRate * 0.20d
                            + posRecoveryRate * 0.12d
                            + posCycleHealth * 0.12d
                            + posStateHealth * 0.11d;

            double risk =
                    posFalseStrongRate * 0.45d
                            + Math.min(1.0d, ((double) posMaxLoseStreak) / 10.0d) * 0.55d;

            healthScore = AppConfig.clampProbability(health - risk * 0.35d);
            positionRisk = AppConfig.clampProbability(risk);
            positionEdge = healthScore - AppConfig.RANDOM_K3_BASELINE;
            positionHealthy =
                    healthScore >= V20_POSITION_HEALTH_MIN
                            && posMaxLoseStreak < 6
                            && positionRisk < 0.70d;
            positionFreeze = posMaxLoseStreak >= 6 || positionRisk >= 0.80d;
            updatedAt = updatedAt <= 0L ? System.currentTimeMillis() : updatedAt;
            summaryText = buildSummaryText();
        }

        public String buildSummaryText() {
            return "位置健康P"
                    + position
                    + "：30="
                    + UiFormatter.probability(posRecent30HitRate)
                    + "，60="
                    + UiFormatter.probability(posRecent60HitRate)
                    + "，120="
                    + UiFormatter.probability(posRecent120HitRate)
                    + "，SHADOW="
                    + UiFormatter.probability(posShadowHitRate)
                    + "，连挂="
                    + posMaxLoseStreak
                    + "，健康="
                    + UiFormatter.probability(healthScore)
                    + "，"
                    + (positionHealthy ? "可参与BET" : "降级/观察");
        }
    }

    public static final class NumberTeacherCell {
        public String targetIssue;
        public int position;
        public int number;
        public String stateKey;
        public String cycleKey;
        public String trendState;
        public String shapeState;
        public String heatColdState;
        public String missGapState;
        public String markovState;
        public String migrationState;
        public String overheatState;

        public int sampleCount;
        public int positiveCount;
        public int negativeCount;
        public int recentSample;
        public int recentPositive;

        public double hitRate;
        public double recentHitRate;
        public double edge;
        public double confidence;
        public double supportScore;
        public double riskScore;
        /* Strongly typed aliases used by the realtime bridge; no reflection-only phantom fields. */
        public double teacherScore;
        public double teacherRisk;
        public double numberTeacherScore;
        public double numberTeacherRisk;
        public boolean teacherPass;
        public String summaryText;
        public long updatedAt;

        public NumberTeacherCell() {
            targetIssue = "";
            position = 0;
            number = 0;
            stateKey = "";
            cycleKey = "";
            trendState = "";
            shapeState = "";
            heatColdState = "";
            missGapState = "";
            markovState = "";
            migrationState = "";
            overheatState = "";
            sampleCount = 0;
            positiveCount = 0;
            negativeCount = 0;
            recentSample = 0;
            recentPositive = 0;
            hitRate = 0.0d;
            recentHitRate = 0.0d;
            edge = 0.0d;
            confidence = 0.0d;
            supportScore = 0.0d;
            riskScore = 0.0d;
            teacherScore = 0.0d;
            teacherRisk = 0.0d;
            numberTeacherScore = 0.0d;
            numberTeacherRisk = 0.0d;
            teacherPass = false;
            summaryText = "";
            updatedAt = 0L;
        }

        public void recalc() {
            if (sampleCount <= 0) {
                sampleCount = positiveCount + negativeCount;
            }
            hitRate = safeRate(positiveCount, sampleCount);
            recentHitRate = safeRate(recentPositive, recentSample);
            double base = teacherNumberBaseline();
            edge =
                    ((hitRate * 0.65d)
                                    + (recentHitRate > 0.0d
                                            ? recentHitRate * 0.35d
                                            : hitRate * 0.35d))
                            - base;
            double sampleScore = Math.min(1.0d, ((double) sampleCount) / 60.0d);
            confidence =
                    AppConfig.clampProbability(
                            sampleScore * 0.60d
                                    + Math.abs(edge) * 2.0d
                                    + (1.0d - riskScore) * 0.20d);
            supportScore =
                    AppConfig.clampProbability(
                            0.50d + edge + confidence * 0.15d - riskScore * 0.20d);
            teacherScore = supportScore;
            teacherRisk = riskScore;
            numberTeacherScore = supportScore;
            numberTeacherRisk = riskScore;
            teacherPass =
                    sampleCount >= 18 && edge > 0.0d && confidence >= 0.30d && riskScore < 0.65d;
            updatedAt = updatedAt <= 0L ? System.currentTimeMillis() : updatedAt;
            summaryText = buildSummaryText();
        }

        public String profileKey() {
            return buildTeacherProfileKey(stateKey, position, number);
        }

        public String buildSummaryText() {
            return "号码教师P"
                    + position
                    + " N"
                    + UiFormatter.number(number)
                    + "：样本="
                    + sampleCount
                    + "，命中="
                    + UiFormatter.probability(hitRate)
                    + "，近期="
                    + UiFormatter.probability(recentHitRate)
                    + "，edge="
                    + UiFormatter.signedProbability(edge)
                    + "，置信="
                    + UiFormatter.probability(confidence)
                    + "，"
                    + (teacherPass ? "支持" : "不足/风险");
        }
    }

    public static final class RankCounterfactual {
        public String targetIssue;
        public String stateKey;
        public int position;
        public int rank;
        public int k3Mask;
        public int sampleCount;
        public int hitCount;
        public int recentSample;
        public int recentHit;

        public double hitRate;
        public double recentHitRate;
        public double rankCounterEdge;
        public double rankFalseStrongRisk;
        public double rank4to6OvertakeRisk;
        public String rankTruthMode;
        public boolean rankPass;
        public String summaryText;
        public long updatedAt;

        public RankCounterfactual() {
            targetIssue = "";
            stateKey = "";
            position = 0;
            rank = 0;
            k3Mask = 0;
            sampleCount = 0;
            hitCount = 0;
            recentSample = 0;
            recentHit = 0;
            hitRate = 0.0d;
            recentHitRate = 0.0d;
            rankCounterEdge = 0.0d;
            rankFalseStrongRisk = 0.0d;
            rank4to6OvertakeRisk = 0.0d;
            rankTruthMode = "";
            rankPass = false;
            summaryText = "";
            updatedAt = 0L;
        }

        public void recalc() {
            hitRate = safeRate(hitCount, sampleCount);
            recentHitRate = safeRate(recentHit, recentSample);
            double mixed =
                    hitRate * 0.60d + (recentSample > 0 ? recentHitRate * 0.40d : hitRate * 0.40d);
            rankCounterEdge = mixed - AppConfig.RANDOM_K3_BASELINE;
            rankFalseStrongRisk =
                    AppConfig.clampProbability(
                            (rank <= 3 && rankCounterEdge < 0.0d) ? -rankCounterEdge * 2.0d : 0.0d);
            rankPass = rankCounterEdge > 0.0d && rankFalseStrongRisk < 0.45d;
            if (rank <= 3 && rankFalseStrongRisk >= 0.45d) rankTruthMode = "TOP_FALSE_STRONG";
            else if (rank >= 4 && rank <= 6 && rankCounterEdge > 0.0d)
                rankTruthMode = "RANK4_6_HIDDEN_STRONG";
            else if (rankPass) rankTruthMode = "RANK_SUPPORT";
            else rankTruthMode = "RANK_NEUTRAL";
            updatedAt = updatedAt <= 0L ? System.currentTimeMillis() : updatedAt;
            summaryText = buildSummaryText();
        }

        public String buildSummaryText() {
            return "Rank反事实R"
                    + rank
                    + "：样本="
                    + sampleCount
                    + "，命中="
                    + UiFormatter.probability(hitRate)
                    + "，近期="
                    + UiFormatter.probability(recentHitRate)
                    + "，edge="
                    + UiFormatter.signedProbability(rankCounterEdge)
                    + "，模式="
                    + rankTruthMode;
        }
    }

    public static final class ThirdGuardStat {
        public String targetIssue;
        public String stateKey;
        public int position;
        public int mainNumber;
        public int supportNumber;
        public int guardNumber;
        public String guardRole;

        public int sampleCount;
        public int saveCount;
        public int dragCount;
        public int neutralCount;

        public double guardSaveRate;
        public double guardDragRate;
        public double thirdGuardNet;
        public double guardRisk;
        public boolean thirdGuardPass;
        public String summaryText;
        public long updatedAt;

        public ThirdGuardStat() {
            targetIssue = "";
            stateKey = "";
            position = 0;
            mainNumber = 0;
            supportNumber = 0;
            guardNumber = 0;
            guardRole = K3_ROLE_DEFENSE;
            sampleCount = 0;
            saveCount = 0;
            dragCount = 0;
            neutralCount = 0;
            guardSaveRate = 0.0d;
            guardDragRate = 0.0d;
            thirdGuardNet = 0.0d;
            guardRisk = 0.0d;
            thirdGuardPass = false;
            summaryText = "";
            updatedAt = 0L;
        }

        public void recalc() {
            if (sampleCount <= 0) {
                sampleCount = saveCount + dragCount + neutralCount;
            }
            guardSaveRate = safeRate(saveCount, sampleCount);
            guardDragRate = safeRate(dragCount, sampleCount);
            thirdGuardNet = guardSaveRate - guardDragRate;
            guardRisk = AppConfig.clampProbability(guardDragRate - guardSaveRate + 0.30d);
            thirdGuardPass = sampleCount < 18 || thirdGuardNet > 0.0d;
            updatedAt = updatedAt <= 0L ? System.currentTimeMillis() : updatedAt;
            summaryText = buildSummaryText();
        }

        public String buildSummaryText() {
            return "第三保护号P"
                    + position
                    + " 主"
                    + UiFormatter.number(mainNumber)
                    + " 辅"
                    + UiFormatter.number(supportNumber)
                    + " 保"
                    + UiFormatter.number(guardNumber)
                    + "：救援="
                    + UiFormatter.probability(guardSaveRate)
                    + "，拖累="
                    + UiFormatter.probability(guardDragRate)
                    + "，净值="
                    + UiFormatter.signedProbability(thirdGuardNet)
                    + "，"
                    + (thirdGuardPass ? "通过" : "拖累降级");
        }
    }

    public static final class RuleHealth {
        public String ruleKey;
        public String stateKey;
        public String sourceKey;

        public int sampleCount;
        public int hitCount;
        public int recent30Sample;
        public int recent30Hit;
        public int recent60Sample;
        public int recent60Hit;
        public int shadowSample;
        public int shadowHit;
        public int maxLoseStreak;

        public double hitRate;
        public double recent30HitRate;
        public double recent60HitRate;
        public double shadowHitRate;
        public double edgeDecay;
        public double healthScore;
        public boolean isActive;
        public boolean isFrozen;
        public String freezeReason;
        public String summaryText;
        public long updatedAt;

        public RuleHealth() {
            ruleKey = "";
            stateKey = "";
            sourceKey = "";
            sampleCount = 0;
            hitCount = 0;
            recent30Sample = 0;
            recent30Hit = 0;
            recent60Sample = 0;
            recent60Hit = 0;
            shadowSample = 0;
            shadowHit = 0;
            maxLoseStreak = 0;
            hitRate = 0.0d;
            recent30HitRate = 0.0d;
            recent60HitRate = 0.0d;
            shadowHitRate = 0.0d;
            edgeDecay = 0.0d;
            healthScore = 0.0d;
            isActive = false;
            isFrozen = false;
            freezeReason = "";
            summaryText = "";
            updatedAt = 0L;
        }

        public void recalc() {
            hitRate = safeRate(hitCount, sampleCount);
            recent30HitRate = safeRate(recent30Hit, recent30Sample);
            recent60HitRate = safeRate(recent60Hit, recent60Sample);
            shadowHitRate = safeRate(shadowHit, shadowSample);
            edgeDecay = AppConfig.clampProbability(Math.max(0.0d, hitRate - recent30HitRate));
            healthScore =
                    AppConfig.clampProbability(
                            hitRate * 0.30d
                                    + recent30HitRate * 0.30d
                                    + recent60HitRate * 0.20d
                                    + shadowHitRate * 0.20d
                                    - edgeDecay * 0.35d);
            isFrozen =
                    maxLoseStreak >= 4
                            || edgeDecay >= 0.15d
                            || (recent30Sample >= 18 && recent30HitRate < 0.30d);
            isActive = !isFrozen && sampleCount >= 18 && healthScore >= 0.33d;
            if (isFrozen && (freezeReason == null || freezeReason.length() == 0)) {
                freezeReason = "规则近期衰退/同类连挂";
            }
            updatedAt = updatedAt <= 0L ? System.currentTimeMillis() : updatedAt;
            summaryText = buildSummaryText();
        }

        public String buildSummaryText() {
            return "规则健康 "
                    + ruleKey
                    + "：样本="
                    + sampleCount
                    + "，命中="
                    + UiFormatter.probability(hitRate)
                    + "，近30="
                    + UiFormatter.probability(recent30HitRate)
                    + "，近60="
                    + UiFormatter.probability(recent60HitRate)
                    + "，SHADOW="
                    + UiFormatter.probability(shadowHitRate)
                    + "，连挂="
                    + maxLoseStreak
                    + "，"
                    + (isFrozen ? "冻结" : "可用");
        }
    }

    public static final class ShadowHealth {
        public String sameClassKey;
        public String stateKey;
        public String sourceKey;
        public String ruleKey;

        public int sample30;
        public int hit30;
        public int sample60;
        public int hit60;
        public int sample120;
        public int hit120;
        public int maxLoseStreak;

        public double hitRate30;
        public double hitRate60;
        public double hitRate120;
        public double healthScore;
        public boolean promotePass;
        public boolean decayRisk;
        public String summaryText;
        public long updatedAt;

        public ShadowHealth() {
            sameClassKey = "";
            stateKey = "";
            sourceKey = "";
            ruleKey = "";
            sample30 = 0;
            hit30 = 0;
            sample60 = 0;
            hit60 = 0;
            sample120 = 0;
            hit120 = 0;
            maxLoseStreak = 0;
            hitRate30 = 0.0d;
            hitRate60 = 0.0d;
            hitRate120 = 0.0d;
            healthScore = 0.0d;
            promotePass = false;
            decayRisk = false;
            summaryText = "";
            updatedAt = 0L;
        }

        public void recalc() {
            hitRate30 = safeRate(hit30, sample30);
            hitRate60 = safeRate(hit60, sample60);
            hitRate120 = safeRate(hit120, sample120);
            healthScore =
                    AppConfig.clampProbability(
                            hitRate30 * 0.45d + hitRate60 * 0.35d + hitRate120 * 0.20d);
            decayRisk = sample60 >= 30 && hitRate30 + 0.05d < hitRate60;
            promotePass =
                    sample30 >= 18
                            && sample60 >= 30
                            && hitRate30 >= V20_SHADOW_HIT_30_MIN
                            && hitRate60 >= V20_SHADOW_HIT_60_MIN
                            && (sample120 < 60 || hitRate120 >= V20_SHADOW_HIT_120_MIN)
                            && maxLoseStreak <= V20_SHADOW_MAX_LOSE
                            && !decayRisk;
            updatedAt = updatedAt <= 0L ? System.currentTimeMillis() : updatedAt;
            summaryText = buildSummaryText();
        }

        public String buildSummaryText() {
            return "SHADOW健康 "
                    + sameClassKey
                    + "：30="
                    + UiFormatter.probability(hitRate30)
                    + "，60="
                    + UiFormatter.probability(hitRate60)
                    + "，120="
                    + UiFormatter.probability(hitRate120)
                    + "，最长连挂="
                    + maxLoseStreak
                    + "，"
                    + (promotePass ? "允许B级晋级" : "继续观察");
        }
    }

    public static final class FalseStrongRecord {
        public String falseStrongKey;
        public String stateKey;
        public String sourceKey;
        public int position;
        public int rankMode;
        public int k3Mask;
        public int missCount;
        public int hitCount;
        public int consecutiveMiss;
        public double recentFalseStrongRate;
        public long freezeUntilIssue;
        public long freezeUntilTime;
        public int freezeLevel;
        public boolean frozen;
        public double riskPenalty;
        public String summaryText;
        public long updatedAt;

        public FalseStrongRecord() {
            falseStrongKey = "";
            stateKey = "";
            sourceKey = "";
            position = 0;
            rankMode = 0;
            k3Mask = 0;
            missCount = 0;
            hitCount = 0;
            consecutiveMiss = 0;
            recentFalseStrongRate = 0.0d;
            freezeUntilIssue = 0L;
            freezeUntilTime = 0L;
            freezeLevel = 0;
            frozen = false;
            riskPenalty = 0.0d;
            summaryText = "";
            updatedAt = 0L;
        }

        public void recalc(long currentIssueValue, long now) {
            int total = missCount + hitCount;
            recentFalseStrongRate =
                    total > 0 ? ((double) missCount) / ((double) total) : recentFalseStrongRate;

            if (consecutiveMiss >= 6 || recentFalseStrongRate >= V20_FALSE_STRONG_HARD) {
                freezeLevel = 3;
            } else if (consecutiveMiss >= 4 || recentFalseStrongRate >= 0.50d) {
                freezeLevel = 2;
            } else if (consecutiveMiss >= 3 || recentFalseStrongRate >= V20_FALSE_STRONG_LIGHT) {
                freezeLevel = 1;
            } else {
                freezeLevel = 0;
            }

            frozen = false;
            if (freezeLevel >= 2) {
                frozen = true;
            }
            if (freezeUntilIssue > 0L
                    && currentIssueValue > 0L
                    && currentIssueValue <= freezeUntilIssue) {
                frozen = true;
            }
            if (freezeUntilTime > 0L && now > 0L && now <= freezeUntilTime) {
                frozen = true;
            }

            riskPenalty =
                    AppConfig.clampProbability(
                            recentFalseStrongRate * 0.45d + ((double) consecutiveMiss) * 0.10d);
            updatedAt = updatedAt <= 0L ? System.currentTimeMillis() : updatedAt;
            summaryText = buildSummaryText();
        }

        public String buildSummaryText() {
            return "假强冻结 "
                    + falseStrongKey
                    + "：连挂="
                    + consecutiveMiss
                    + "，假强率="
                    + UiFormatter.probability(recentFalseStrongRate)
                    + "，等级="
                    + freezeLevel
                    + "，"
                    + (frozen ? "冻结/SHADOW" : "未冻结");
        }
    }

    public static final class DecisionAudit {
        public String auditKey;
        public String baseIssue;
        public String targetIssue;
        public long createdAt;

        public int position;
        public int n1;
        public int n2;
        public int n3;
        public int k3Mask;

        public CandidateGrade grade;
        public String decisionAction;
        public String statScope;
        public String sourceType;

        public double finalP;
        public double dynamicGate;
        public double edge;
        public double finalCauseScore;
        public double positionEdge;
        public double numberTeacherEdge;
        public double rankCounterEdge;
        public double thirdGuardNet;
        public double shadowHealth;
        public double falseStrongRisk;

        public int positionHealthy;
        public int numberTeacherPass;
        public int rankPass;
        public int thirdGuardPass;
        public int shadowPass;
        public int falseStrongFrozen;
        public int sameClassFrozen;
        public int riskOk;

        public String stateKey;
        public String sourceKey;
        public String ruleKey;
        public String sameClassKey;
        public String decisionReason;
        public String blockCode;
        public String riskTags;

        public int settled;
        public String result;
        public int actualNumber;
        public int hit;
        public double pnl;

        public DecisionAudit() {
            auditKey = "";
            baseIssue = "";
            targetIssue = "";
            createdAt = 0L;
            position = 0;
            n1 = 0;
            n2 = 0;
            n3 = 0;
            k3Mask = 0;
            grade = CandidateGrade.UNKNOWN;
            decisionAction = AppConfig.DECISION_HOLD;
            statScope = AppConfig.STAT_SCOPE_HOLD;
            sourceType = SOURCE_TYPE_REALTIME;
            finalP = 0.0d;
            dynamicGate = AppConfig.DEFAULT_DYNAMIC_GATE;
            edge = 0.0d;
            finalCauseScore = 0.0d;
            positionEdge = 0.0d;
            numberTeacherEdge = 0.0d;
            rankCounterEdge = 0.0d;
            thirdGuardNet = 0.0d;
            shadowHealth = 0.0d;
            falseStrongRisk = 0.0d;
            positionHealthy = 0;
            numberTeacherPass = 0;
            rankPass = 0;
            thirdGuardPass = 0;
            shadowPass = 0;
            falseStrongFrozen = 0;
            sameClassFrozen = 0;
            riskOk = 0;
            stateKey = "";
            sourceKey = "";
            ruleKey = "";
            sameClassKey = "";
            decisionReason = "";
            blockCode = "";
            riskTags = "";
            settled = 0;
            result = AppConfig.RESULT_WAIT;
            actualNumber = 0;
            hit = 0;
            pnl = 0.0d;
        }

        public void fromCandidate(CandidateK3 c) {
            if (c == null) return;
            c.normalize();
            baseIssue = c.baseIssue;
            targetIssue = c.targetIssue;
            createdAt = System.currentTimeMillis();
            position = c.position;
            n1 = c.n1;
            n2 = c.n2;
            n3 = c.n3;
            k3Mask = c.k3Mask;
            grade = c.grade;
            decisionAction = c.decisionAction;
            statScope =
                    AppConfig.DECISION_BET.equals(decisionAction)
                            ? AppConfig.STAT_SCOPE_REAL_TRADE
                            : (AppConfig.DECISION_SHADOW.equals(decisionAction)
                                    ? AppConfig.STAT_SCOPE_SHADOW
                                    : AppConfig.STAT_SCOPE_HOLD);
            sourceType = SOURCE_TYPE_REALTIME;
            finalP = c.finalP;
            dynamicGate = c.dynamicGate;
            edge = c.edge;
            finalCauseScore = c.finalCauseScore;
            positionEdge = c.positionEdge;
            numberTeacherEdge = c.numberTeacherEdge;
            rankCounterEdge = c.rankCounterEdge;
            thirdGuardNet = c.thirdGuardEdge;
            shadowHealth = c.shadowHealth;
            falseStrongRisk = c.falseStrongRisk;
            positionHealthy = c.positionHealthy ? 1 : 0;
            numberTeacherPass = c.numberTeacherPass ? 1 : 0;
            rankPass = c.rankPass ? 1 : 0;
            thirdGuardPass = c.thirdGuardPass ? 1 : 0;
            shadowPass = c.shadowPass ? 1 : 0;
            falseStrongFrozen = c.falseStrongFrozen ? 1 : 0;
            sameClassFrozen = c.sameClassFrozen ? 1 : 0;
            riskOk = c.riskOk ? 1 : 0;
            stateKey = c.stateKey;
            sourceKey = c.sourceKey;
            ruleKey = c.ruleKey;
            sameClassKey = c.sameClassKey;
            decisionReason = c.decisionReason;
            riskTags = c.riskTags;
            rebuildKey();
        }

        public void applySettlement(SettlementResult r) {
            if (r == null) return;
            settled = r.settled ? 1 : 0;
            result = r.result;
            actualNumber = r.actualNumber;
            hit = r.hit ? 1 : 0;
            pnl = r.pnl;
        }

        public void rebuildKey() {
            auditKey = "DA_" + safeIssue(targetIssue) + "_P" + position + "_K" + k3Mask;
        }
    }

    public static final class SettlementResult {
        public String targetIssue;
        public int position;
        public int n1;
        public int n2;
        public int n3;
        public int k3Mask;
        public int actualNumber;
        public boolean hit;
        public boolean settled;
        public String result;
        public double pnl;
        public String missReason;
        public String settlementReason;
        public long settledAt;

        public SettlementResult() {
            targetIssue = "";
            position = 0;
            n1 = 0;
            n2 = 0;
            n3 = 0;
            k3Mask = 0;
            actualNumber = 0;
            hit = false;
            settled = false;
            result = AppConfig.RESULT_WAIT;
            pnl = 0.0d;
            missReason = "";
            settlementReason = "";
            settledAt = 0L;
        }

        public void setNumbers(int a, int b, int c) {
            n1 = a;
            n2 = b;
            n3 = c;
            k3Mask = buildK3Mask(a, b, c);
        }

        public void settleByDraw(Draw draw) {
            if (draw == null || !draw.isValid()) {
                markDataError(AppConfig.RESULT_DATA_ERROR, "开奖数据无效");
                return;
            }
            if (targetIssue != null
                    && targetIssue.length() > 0
                    && !targetIssue.equals(draw.issue)) {
                markDataError(AppConfig.RESULT_DATA_OFFSET, "目标期不一致");
                return;
            }
            if (!AppConfig.isPlayRuleLegal(position, AppConfig.FIXED_K, n1, n2, n3)) {
                markDataError(AppConfig.RESULT_DATA_ERROR, "非单位置K3合法投注");
                return;
            }
            actualNumber = draw.getNumberAtPosition(position);
            hit = maskContains(k3Mask, actualNumber);
            settled = true;
            result = hit ? AppConfig.RESULT_WIN : AppConfig.RESULT_MISS;
            pnl = AppConfig.pnlForHit(hit);
            missReason = hit ? "" : AppConfig.MISS_UNKNOWN;
            settlementReason = hit ? "STRICT_POSITION_K3_WIN" : "STRICT_POSITION_K3_MISS";
            settledAt = System.currentTimeMillis();
        }

        public void markDataError(String code, String reason) {
            actualNumber = 0;
            hit = false;
            settled = true;
            result = AppConfig.normalizeResult(code);
            if (!AppConfig.isDataBadResult(result)) {
                result = AppConfig.RESULT_DATA_ERROR;
            }
            pnl = 0.0d;
            missReason = code == null ? AppConfig.RESULT_DATA_ERROR : code;
            settlementReason = reason == null ? "" : reason;
            settledAt = System.currentTimeMillis();
        }

        public String buildSummaryText() {
            return "结算"
                    + targetIssue
                    + " P"
                    + position
                    + " 预测"
                    + maskToText(k3Mask)
                    + " 实开"
                    + UiFormatter.number(actualNumber)
                    + "，结果="
                    + result
                    + "，盈亏="
                    + UiFormatter.money(pnl)
                    + "，规则=严格单位置K3";
        }
    }

    // ========== 重启第一 / V20 动态高胜率因链系统：确定性窗口状态 + 原因矩阵增强 第1/3 ==========
    // 规则：
    // 1. 只新增模型和工具方法，不删除旧类，不改旧签名。
    // 2. 所有模型只负责承载、归因、解释、回放，不直接产生真实BET。
    // 3. 真实BET仍然只能由 DecisionGateEngine 最终裁决。
    // 4. SHADOW/HOLD/教师/后台回测样本不得污染真实胜率、真实盈亏。

    public static final String V20_ACTION_BET_NOW = "BET_NOW";
    public static final String V20_ACTION_HOLD = "HOLD";
    public static final String V20_ACTION_WAIT_CONFIRM = "WAIT_CONFIRM";
    public static final String V20_ACTION_SHADOW = "SHADOW";
    public static final String V20_ACTION_FREEZE = "FREEZE";
    public static final String V20_ACTION_REJECT = "REJECT";

    public static final String V20_TIMING_UNKNOWN = "TIMING_UNKNOWN";
    public static final String V20_TIMING_NOW_OK = "NOW_OK";
    public static final String V20_TIMING_EARLY_RISK = "EARLY_RISK";
    public static final String V20_TIMING_WAIT_NEXT1 = "WAIT_NEXT1";
    public static final String V20_TIMING_WAIT_NEXT2 = "WAIT_NEXT2";
    public static final String V20_TIMING_LATE = "LATE";
    public static final String V20_TIMING_NOISE = "NOISE";

    public static final String V20_SIM_EXACT = "SIM_EXACT";
    public static final String V20_SIM_NEAR = "SIM_NEAR";
    public static final String V20_SIM_FUZZY = "SIM_FUZZY";
    public static final String V20_SIM_REJECT = "SIM_REJECT";

    public static final String V20_TEACHER_EMPTY = "TEACHER_EMPTY";
    public static final String V20_TEACHER_WEAK = "TEACHER_WEAK";
    public static final String V20_TEACHER_READY = "TEACHER_READY";
    public static final String V20_TEACHER_STRONG = "TEACHER_STRONG";
    public static final String V20_TEACHER_RISK = "TEACHER_RISK";
    public static final String V20_TEACHER_DECAY = "TEACHER_DECAY";

    public static final String V20_ROLE_UNKNOWN = "ROLE_UNKNOWN";
    public static final String V20_ROLE_MAIN_ATTACK = "MAIN_ATTACK";
    public static final String V20_ROLE_REBOUND = "REBOUND";
    public static final String V20_ROLE_DEFENSE = "DEFENSE";
    public static final String V20_ROLE_REVERSAL = "REVERSAL";
    public static final String V20_ROLE_TEACHER_SUPPORT = "TEACHER_SUPPORT";
    public static final String V20_ROLE_TIMING_SUPPORT = "TIMING_SUPPORT";

    public static final String V20_MISS_UNKNOWN = "UNKNOWN";
    public static final String V20_MISS_EARLY_1 = "EARLY_1";
    public static final String V20_MISS_EARLY_2 = "EARLY_2";
    public static final String V20_MISS_LATE = "LATE";
    public static final String V20_MISS_FALSE_STRONG = "FALSE_STRONG";
    public static final String V20_MISS_COUNTERFACTUAL_MISORDER = "COUNTERFACTUAL_MISORDER";
    public static final String V20_MISS_TEACHER_DECAY = "TEACHER_DECAY";
    public static final String V20_MISS_RULE_DECAY = "RULE_DECAY";
    public static final String V20_MISS_ROLE_COLLAPSE = "ROLE_COLLAPSE";
    public static final String V20_MISS_SAMPLE_WEAK = "SAMPLE_WEAK";
    public static final String V20_MISS_NOISE = "NOISE";
    public static final String V20_MISS_POSITION_WRONG = "POSITION_WRONG";
    public static final String V20_MISS_NUMBER_SCOPE_MISS = "NUMBER_SCOPE_MISS";

    public static final String V20_STATE_UNKNOWN = "STATE_UNKNOWN";
    public static final String V20_HOT_UP = "HOT_UP";
    public static final String V20_HOT_FLAT = "HOT_FLAT";
    public static final String V20_HOT_DOWN = "HOT_DOWN";
    public static final String V20_COLD_RECOVER = "COLD_RECOVER";
    public static final String V20_COLD_DEEP = "COLD_DEEP";

    public static final String V20_DENSITY_LOW = "DENSITY_LOW";
    public static final String V20_DENSITY_MID = "DENSITY_MID";
    public static final String V20_DENSITY_HIGH = "DENSITY_HIGH";

    public static final String V20_BIAS_LOW = "BIAS_LOW";
    public static final String V20_BIAS_MID = "BIAS_MID";
    public static final String V20_BIAS_HIGH = "BIAS_HIGH";

    public static final double V20_CAUSE_BASELINE = AppConfig.RANDOM_K3_BASELINE;
    public static final double V20_MIN_CAUSE_CONFIDENCE = 0.300d;
    public static final double V20_READY_CAUSE_CONFIDENCE = 0.450d;
    public static final double V20_STRONG_CAUSE_CONFIDENCE = 0.600d;
    public static final double V20_MAX_REASON_BOOST = 0.035d;
    public static final double V20_MAX_TEACHER_BOOST = 0.025d;
    public static final double V20_MAX_TIMING_BOOST = 0.018d;
    public static final double V20_MAX_SIMILAR_BOOST = 0.022d;
    public static final double V20_MAX_COUNTER_BOOST = 0.018d;
    public static final double V20_MAX_FALSE_STRONG_PENALTY = 0.060d;
    public static final double V20_MAX_RULE_DECAY_PENALTY = 0.045d;
    public static final double V20_MAX_EARLY_TIMING_PENALTY = 0.040d;
    public static final double V20_MAX_WEAK_SAMPLE_PENALTY = 0.030d;

    public static String normalizeV20Action(String value) {
        String s = AppConfig.normalizeToken(value);
        if (V20_ACTION_BET_NOW.equals(s)
                || AppConfig.DECISION_BET.equals(s)
                || AppConfig.ACTION_BET.equals(s)) return V20_ACTION_BET_NOW;
        if (V20_ACTION_WAIT_CONFIRM.equals(s)) return V20_ACTION_WAIT_CONFIRM;
        if (V20_ACTION_SHADOW.equals(s) || AppConfig.DECISION_SHADOW.equals(s))
            return V20_ACTION_SHADOW;
        if (V20_ACTION_FREEZE.equals(s)) return V20_ACTION_FREEZE;
        if (V20_ACTION_REJECT.equals(s)) return V20_ACTION_REJECT;
        return V20_ACTION_HOLD;
    }

    public static String normalizeTimingStatus(String value) {
        String s = AppConfig.normalizeToken(value);
        if (V20_TIMING_NOW_OK.equals(s)) return V20_TIMING_NOW_OK;
        if (V20_TIMING_EARLY_RISK.equals(s)) return V20_TIMING_EARLY_RISK;
        if (V20_TIMING_WAIT_NEXT1.equals(s)) return V20_TIMING_WAIT_NEXT1;
        if (V20_TIMING_WAIT_NEXT2.equals(s)) return V20_TIMING_WAIT_NEXT2;
        if (V20_TIMING_LATE.equals(s)) return V20_TIMING_LATE;
        if (V20_TIMING_NOISE.equals(s)) return V20_TIMING_NOISE;
        return V20_TIMING_UNKNOWN;
    }

    public static String normalizeSimilarityClass(String value) {
        String s = AppConfig.normalizeToken(value);
        if (V20_SIM_EXACT.equals(s)) return V20_SIM_EXACT;
        if (V20_SIM_NEAR.equals(s)) return V20_SIM_NEAR;
        if (V20_SIM_FUZZY.equals(s)) return V20_SIM_FUZZY;
        return V20_SIM_REJECT;
    }

    public static String normalizeTeacherStatusV20(String value) {
        String s = AppConfig.normalizeToken(value);
        if (V20_TEACHER_EMPTY.equals(s)) return V20_TEACHER_EMPTY;
        if (V20_TEACHER_WEAK.equals(s)) return V20_TEACHER_WEAK;
        if (V20_TEACHER_READY.equals(s)) return V20_TEACHER_READY;
        if (V20_TEACHER_STRONG.equals(s)) return V20_TEACHER_STRONG;
        if (V20_TEACHER_RISK.equals(s)) return V20_TEACHER_RISK;
        if (V20_TEACHER_DECAY.equals(s)) return V20_TEACHER_DECAY;

        if (AppConfig.TEACHER_MATRIX_STATUS_WEAK.equals(s)) return V20_TEACHER_WEAK;
        if (AppConfig.TEACHER_MATRIX_STATUS_READY.equals(s)) return V20_TEACHER_READY;
        if (AppConfig.TEACHER_MATRIX_STATUS_STRONG.equals(s)) return V20_TEACHER_STRONG;
        if (AppConfig.TEACHER_MATRIX_STATUS_RISK.equals(s)) return V20_TEACHER_RISK;
        return V20_TEACHER_EMPTY;
    }

    public static String normalizeMissReasonTypeV20(String value) {
        String s = AppConfig.normalizeToken(value);
        if (V20_MISS_EARLY_1.equals(s)) return V20_MISS_EARLY_1;
        if (V20_MISS_EARLY_2.equals(s)) return V20_MISS_EARLY_2;
        if (V20_MISS_LATE.equals(s)) return V20_MISS_LATE;
        if (V20_MISS_FALSE_STRONG.equals(s)) return V20_MISS_FALSE_STRONG;
        if (V20_MISS_COUNTERFACTUAL_MISORDER.equals(s)) return V20_MISS_COUNTERFACTUAL_MISORDER;
        if (V20_MISS_TEACHER_DECAY.equals(s)) return V20_MISS_TEACHER_DECAY;
        if (V20_MISS_RULE_DECAY.equals(s)) return V20_MISS_RULE_DECAY;
        if (V20_MISS_ROLE_COLLAPSE.equals(s)) return V20_MISS_ROLE_COLLAPSE;
        if (V20_MISS_SAMPLE_WEAK.equals(s)) return V20_MISS_SAMPLE_WEAK;
        if (V20_MISS_NOISE.equals(s)) return V20_MISS_NOISE;
        if (V20_MISS_POSITION_WRONG.equals(s)) return V20_MISS_POSITION_WRONG;
        if (V20_MISS_NUMBER_SCOPE_MISS.equals(s)) return V20_MISS_NUMBER_SCOPE_MISS;
        return V20_MISS_UNKNOWN;
    }

    public static String normalizeNumberRoleV20(String value) {
        String s = AppConfig.normalizeToken(value);
        if (V20_ROLE_MAIN_ATTACK.equals(s) || K3_ROLE_MAIN.equals(s)) return V20_ROLE_MAIN_ATTACK;
        if (V20_ROLE_REBOUND.equals(s) || K3_ROLE_SUPPORT.equals(s)) return V20_ROLE_REBOUND;
        if (V20_ROLE_DEFENSE.equals(s) || K3_ROLE_DEFENSE.equals(s)) return V20_ROLE_DEFENSE;
        if (V20_ROLE_REVERSAL.equals(s)) return V20_ROLE_REVERSAL;
        if (V20_ROLE_TEACHER_SUPPORT.equals(s)) return V20_ROLE_TEACHER_SUPPORT;
        if (V20_ROLE_TIMING_SUPPORT.equals(s)) return V20_ROLE_TIMING_SUPPORT;
        return V20_ROLE_UNKNOWN;
    }

    public static double safeDistanceScore(double distance) {
        if (Double.isNaN(distance) || Double.isInfinite(distance)) return 0.0d;
        if (distance <= 0.0d) return 1.0d;
        return AppConfig.clampProbability(1.0d - distance);
    }

    public static double calcSampleConfidenceV20(int sample, int strongSample) {
        if (strongSample <= 0) strongSample = 120;
        if (sample <= 0) return 0.0d;
        double v = ((double) sample) / ((double) strongSample);
        if (v < 0.0d) return 0.0d;
        if (v > 1.0d) return 1.0d;
        return v;
    }

    public static String buildV20StateKeyPart(String prefix, String value) {
        String p = prefix == null ? "" : prefix;
        String v = AppConfig.buildStateKeyPart(value);
        return p + "=" + v;
    }

    public static String buildV20ExactStateKey(
            int position,
            String w12,
            String w24,
            String w48,
            String w96,
            String w360,
            String missBucket,
            String densityBucket,
            String oddEvenBucket,
            String bigSmallBucket,
            String timingStatus,
            String riskStatus) {
        StringBuilder sb = new StringBuilder();
        sb.append("P").append(position);
        sb.append("|").append(buildV20StateKeyPart("W12", w12));
        sb.append("|").append(buildV20StateKeyPart("W24", w24));
        sb.append("|").append(buildV20StateKeyPart("W48", w48));
        sb.append("|").append(buildV20StateKeyPart("W96", w96));
        sb.append("|").append(buildV20StateKeyPart("W360", w360));
        sb.append("|").append(buildV20StateKeyPart("MISS", missBucket));
        sb.append("|").append(buildV20StateKeyPart("DEN", densityBucket));
        sb.append("|").append(buildV20StateKeyPart("OE", oddEvenBucket));
        sb.append("|").append(buildV20StateKeyPart("BS", bigSmallBucket));
        sb.append("|").append(buildV20StateKeyPart("TIME", timingStatus));
        sb.append("|").append(buildV20StateKeyPart("RISK", riskStatus));
        return sb.toString();
    }

    public static String buildV20RelaxedStateKey(
            int position,
            String w24,
            String w48,
            String missBucket,
            String densityBucket,
            String timingStatus,
            String riskStatus) {
        StringBuilder sb = new StringBuilder();
        sb.append("P").append(position);
        sb.append("|").append(buildV20StateKeyPart("W24", w24));
        sb.append("|").append(buildV20StateKeyPart("W48", w48));
        sb.append("|").append(buildV20StateKeyPart("MISS", missBucket));
        sb.append("|").append(buildV20StateKeyPart("DEN", densityBucket));
        sb.append("|").append(buildV20StateKeyPart("TIME", timingStatus));
        sb.append("|").append(buildV20StateKeyPart("RISK", riskStatus));
        return sb.toString();
    }

    public static String buildV20MatrixKey(
            String prefix,
            String stateKey,
            int position,
            int number,
            int k3Mask,
            String role,
            String timing) {
        StringBuilder sb = new StringBuilder();
        sb.append(prefix == null ? "V20" : prefix);
        sb.append("|S=").append(Math.abs(safeText(stateKey).hashCode()));
        sb.append("|P").append(position);
        if (AppConfig.isValidNumber(number)) {
            sb.append("|N").append(number);
        }
        if (isValidK3Mask(k3Mask)) {
            sb.append("|K").append(k3Mask);
        }
        if (role != null && role.length() > 0) {
            sb.append("|R=").append(AppConfig.buildStateKeyPart(role));
        }
        if (timing != null && timing.length() > 0) {
            sb.append("|T=").append(AppConfig.buildStateKeyPart(timing));
        }
        return sb.toString();
    }

    public static final class WindowFeature {
        public String baseIssue;
        public String targetIssue;
        public int position;
        public int number;
        public int windowSize;
        public long createdAt;

        public int sampleCount;
        public int hitCount;
        public int missGap;
        public int maxGap;
        public int repeatCount;
        public int adjacentCount;
        public int sameTailCount;
        public int oddCount;
        public int evenCount;
        public int bigCount;
        public int smallCount;

        public double frequency;
        public double expectedFrequency;
        public double hotScore;
        public double coldScore;
        public double slope;
        public double density;
        public double repeatRate;
        public double oddRate;
        public double bigRate;
        public double adjacentRate;
        public double sameTailRate;
        public double entropy;
        public double stability;
        public double volatility;
        public double crowdingScore;
        public double crossPositionSync;
        public double crossPositionSqueeze;
        public double recentHitRate;
        public double recentMissRisk;
        public double timingScore;
        public double riskScore;
        public double featureScore;

        public String hotColdBucket;
        public String slopeBucket;
        public String missBucket;
        public String densityBucket;
        public String repeatBucket;
        public String oddEvenBucket;
        public String bigSmallBucket;
        public String adjacentBucket;
        public String sameTailBucket;
        public String crowdingBucket;
        public String syncBucket;
        public String squeezeBucket;
        public String timingBucket;
        public String riskBucket;
        public String featureKey;
        public String summaryText;

        public WindowFeature() {
            baseIssue = "";
            targetIssue = "";
            position = 0;
            number = 0;
            windowSize = 0;
            createdAt = 0L;
            sampleCount = 0;
            hitCount = 0;
            missGap = 0;
            maxGap = 0;
            repeatCount = 0;
            adjacentCount = 0;
            sameTailCount = 0;
            oddCount = 0;
            evenCount = 0;
            bigCount = 0;
            smallCount = 0;
            frequency = 0.0d;
            expectedFrequency = teacherNumberBaseline();
            hotScore = 0.0d;
            coldScore = 0.0d;
            slope = 0.0d;
            density = 0.0d;
            repeatRate = 0.0d;
            oddRate = 0.0d;
            bigRate = 0.0d;
            adjacentRate = 0.0d;
            sameTailRate = 0.0d;
            entropy = 0.0d;
            stability = 0.0d;
            volatility = 0.0d;
            crowdingScore = 0.0d;
            crossPositionSync = 0.0d;
            crossPositionSqueeze = 0.0d;
            recentHitRate = 0.0d;
            recentMissRisk = 0.0d;
            timingScore = 0.0d;
            riskScore = 0.0d;
            featureScore = 0.0d;
            hotColdBucket = V20_STATE_UNKNOWN;
            slopeBucket = V20_STATE_UNKNOWN;
            missBucket = "MISS_UNKNOWN";
            densityBucket = V20_DENSITY_MID;
            repeatBucket = V20_BIAS_MID;
            oddEvenBucket = V20_BIAS_MID;
            bigSmallBucket = V20_BIAS_MID;
            adjacentBucket = V20_BIAS_MID;
            sameTailBucket = V20_BIAS_MID;
            crowdingBucket = V20_BIAS_MID;
            syncBucket = V20_BIAS_MID;
            squeezeBucket = V20_BIAS_MID;
            timingBucket = V20_TIMING_UNKNOWN;
            riskBucket = "RISK_UNKNOWN";
            featureKey = "";
            summaryText = "";
        }

        public void recalc() {
            if (sampleCount <= 0) {
                sampleCount = windowSize;
            }
            if (sampleCount <= 0) {
                sampleCount = 1;
            }

            frequency = AppConfig.clampProbability(((double) hitCount) / ((double) sampleCount));
            if (expectedFrequency <= 0.0d) {
                expectedFrequency = teacherNumberBaseline();
            }

            repeatRate =
                    AppConfig.clampProbability(((double) repeatCount) / ((double) sampleCount));
            adjacentRate =
                    AppConfig.clampProbability(((double) adjacentCount) / ((double) sampleCount));
            sameTailRate =
                    AppConfig.clampProbability(((double) sameTailCount) / ((double) sampleCount));
            oddRate =
                    AppConfig.clampProbability(
                            ((double) oddCount) / ((double) Math.max(1, oddCount + evenCount)));
            bigRate =
                    AppConfig.clampProbability(
                            ((double) bigCount) / ((double) Math.max(1, bigCount + smallCount)));

            double edge = frequency - expectedFrequency;
            hotScore = AppConfig.clampProbability(0.50d + edge * 2.0d);
            coldScore = AppConfig.clampProbability(0.50d - edge * 2.0d);

            density =
                    AppConfig.clampProbability(
                            frequency * 0.55d
                                    + repeatRate * 0.20d
                                    + adjacentRate * 0.15d
                                    + sameTailRate * 0.10d);
            stability = AppConfig.clampProbability(1.0d - Math.abs(slope) - volatility * 0.50d);
            recentMissRisk = AppConfig.clampProbability(((double) missGap) / 30.0d);
            timingScore =
                    AppConfig.clampProbability(
                            0.50d + edge - recentMissRisk * 0.10d + coldScore * 0.10d);
            riskScore =
                    AppConfig.clampProbability(
                            volatility * 0.30d
                                    + crowdingScore * 0.20d
                                    + crossPositionSqueeze * 0.20d
                                    + recentMissRisk * 0.30d);
            featureScore =
                    AppConfig.clampProbability(
                            hotScore * 0.25d
                                    + coldScore * 0.12d
                                    + density * 0.18d
                                    + stability * 0.20d
                                    + timingScore * 0.15d
                                    + (1.0d - riskScore) * 0.10d);

            hotColdBucket = buildHotColdBucket(edge, coldScore);
            slopeBucket = buildSlopeBucket(slope);
            missBucket = buildMissBucket(missGap);
            densityBucket = buildDensityBucket(density);
            repeatBucket = buildBiasBucket(repeatRate);
            oddEvenBucket = buildBiasBucket(oddRate);
            bigSmallBucket = buildBiasBucket(bigRate);
            adjacentBucket = buildBiasBucket(adjacentRate);
            sameTailBucket = buildBiasBucket(sameTailRate);
            crowdingBucket = buildBiasBucket(crowdingScore);
            syncBucket = buildBiasBucket(crossPositionSync);
            squeezeBucket = buildBiasBucket(crossPositionSqueeze);
            timingBucket = buildTimingBucket(timingScore, recentMissRisk);
            riskBucket = buildRiskBucket(riskScore);
            rebuildFeatureKey();
            summaryText = buildSummaryText();
        }

        private String buildHotColdBucket(double edge, double cold) {
            if (edge >= 0.060d && slope >= 0.010d) return V20_HOT_UP;
            if (edge >= 0.025d) return V20_HOT_FLAT;
            if (edge <= -0.050d && cold >= 0.600d) return V20_COLD_DEEP;
            if (edge <= -0.020d && slope >= 0.0d) return V20_COLD_RECOVER;
            if (slope < -0.015d) return V20_HOT_DOWN;
            return V20_HOT_FLAT;
        }

        private String buildSlopeBucket(double v) {
            if (v >= 0.025d) return "SLOPE_UP_STRONG";
            if (v >= 0.008d) return "SLOPE_UP";
            if (v <= -0.025d) return "SLOPE_DOWN_STRONG";
            if (v <= -0.008d) return "SLOPE_DOWN";
            return "SLOPE_FLAT";
        }

        private String buildMissBucket(int gap) {
            if (gap <= 3) return "MISS_0_3";
            if (gap <= 7) return "MISS_4_7";
            if (gap <= 15) return "MISS_8_15";
            if (gap <= 24) return "MISS_16_24";
            if (gap <= 36) return "MISS_25_36";
            return "MISS_37_PLUS";
        }

        private String buildDensityBucket(double v) {
            if (v >= 0.660d) return V20_DENSITY_HIGH;
            if (v <= 0.330d) return V20_DENSITY_LOW;
            return V20_DENSITY_MID;
        }

        private String buildBiasBucket(double v) {
            if (v >= 0.620d) return V20_BIAS_HIGH;
            if (v <= 0.380d) return V20_BIAS_LOW;
            return V20_BIAS_MID;
        }

        private String buildTimingBucket(double t, double missRisk) {
            if (t >= 0.620d && missRisk <= 0.680d) return V20_TIMING_NOW_OK;
            if (missRisk >= 0.720d) return V20_TIMING_EARLY_RISK;
            if (t >= 0.520d) return V20_TIMING_WAIT_NEXT1;
            if (t <= 0.360d) return V20_TIMING_LATE;
            return V20_TIMING_UNKNOWN;
        }

        private String buildRiskBucket(double v) {
            if (v >= 0.720d) return "RISK_HIGH";
            if (v >= 0.450d) return "RISK_MID";
            return "RISK_LOW";
        }

        public void rebuildFeatureKey() {
            StringBuilder sb = new StringBuilder();
            sb.append("WF");
            sb.append("|P").append(position);
            sb.append("|N").append(number);
            sb.append("|W").append(windowSize);
            sb.append("|").append(hotColdBucket);
            sb.append("|").append(slopeBucket);
            sb.append("|").append(missBucket);
            sb.append("|").append(densityBucket);
            sb.append("|").append(oddEvenBucket);
            sb.append("|").append(bigSmallBucket);
            sb.append("|").append(timingBucket);
            sb.append("|").append(riskBucket);
            featureKey = sb.toString();
        }

        public String buildSummaryText() {
            return "W"
                    + windowSize
                    + " P"
                    + position
                    + " N"
                    + UiFormatter.number(number)
                    + "："
                    + hotColdBucket
                    + "，"
                    + missBucket
                    + "，密集="
                    + densityBucket
                    + "，时机="
                    + timingBucket
                    + "，风险="
                    + riskBucket
                    + "，特征分="
                    + UiFormatter.probability(featureScore);
        }
    }

    public static final class WindowState {
        public String baseIssue;
        public String targetIssue;
        public long createdAt;
        public int position;

        public WindowFeature w12;
        public WindowFeature w24;
        public WindowFeature w48;
        public WindowFeature w96;
        public WindowFeature w360;

        public String exactStateKey;
        public String relaxedStateKey;
        public String stateKey;
        public String stateMode;

        public String w12State;
        public String w24State;
        public String w48State;
        public String w96State;
        public String w360State;

        public String missBucket;
        public String densityBucket;
        public String oddEvenBucket;
        public String bigSmallBucket;
        public String adjacentBucket;
        public String sameTailBucket;
        public String crowdingBucket;
        public String syncBucket;
        public String squeezeBucket;
        public String timingStatus;
        public String riskStatus;

        public double heatScore;
        public double coldScore;
        public double slopeScore;
        public double densityScore;
        public double repeatScore;
        public double oddEvenBias;
        public double bigSmallBias;
        public double adjacentSupport;
        public double sameTailSupport;
        public double crowdingRisk;
        public double crossSyncScore;
        public double squeezeRisk;
        public double timingScore;
        public double stabilityScore;
        public double riskScore;
        public double confidence;

        public int recentLoseStreak;
        public int recentHitStreak;
        public int falseStrongCount;
        public int sampleTrust;
        public String summaryText;

        public WindowState() {
            baseIssue = "";
            targetIssue = "";
            createdAt = 0L;
            position = 0;
            w12 = null;
            w24 = null;
            w48 = null;
            w96 = null;
            w360 = null;
            exactStateKey = "";
            relaxedStateKey = "";
            stateKey = "";
            stateMode = AppConfig.MODE_UNKNOWN;
            w12State = V20_STATE_UNKNOWN;
            w24State = V20_STATE_UNKNOWN;
            w48State = V20_STATE_UNKNOWN;
            w96State = V20_STATE_UNKNOWN;
            w360State = V20_STATE_UNKNOWN;
            missBucket = "MISS_UNKNOWN";
            densityBucket = V20_DENSITY_MID;
            oddEvenBucket = V20_BIAS_MID;
            bigSmallBucket = V20_BIAS_MID;
            adjacentBucket = V20_BIAS_MID;
            sameTailBucket = V20_BIAS_MID;
            crowdingBucket = V20_BIAS_MID;
            syncBucket = V20_BIAS_MID;
            squeezeBucket = V20_BIAS_MID;
            timingStatus = V20_TIMING_UNKNOWN;
            riskStatus = "RISK_UNKNOWN";
            heatScore = 0.0d;
            coldScore = 0.0d;
            slopeScore = 0.0d;
            densityScore = 0.0d;
            repeatScore = 0.0d;
            oddEvenBias = 0.0d;
            bigSmallBias = 0.0d;
            adjacentSupport = 0.0d;
            sameTailSupport = 0.0d;
            crowdingRisk = 0.0d;
            crossSyncScore = 0.0d;
            squeezeRisk = 0.0d;
            timingScore = 0.0d;
            stabilityScore = 0.0d;
            riskScore = 0.0d;
            confidence = 0.0d;
            recentLoseStreak = 0;
            recentHitStreak = 0;
            falseStrongCount = 0;
            sampleTrust = 0;
            summaryText = "";
        }

        public void recalc() {
            if (w12 != null) w12.recalc();
            if (w24 != null) w24.recalc();
            if (w48 != null) w48.recalc();
            if (w96 != null) w96.recalc();
            if (w360 != null) w360.recalc();

            w12State = featureState(w12);
            w24State = featureState(w24);
            w48State = featureState(w48);
            w96State = featureState(w96);
            w360State = featureState(w360);

            WindowFeature primary = w24 != null ? w24 : (w12 != null ? w12 : w48);
            WindowFeature background = w360 != null ? w360 : (w96 != null ? w96 : primary);

            if (primary != null) {
                missBucket = primary.missBucket;
                densityBucket = primary.densityBucket;
                oddEvenBucket = primary.oddEvenBucket;
                bigSmallBucket = primary.bigSmallBucket;
                adjacentBucket = primary.adjacentBucket;
                sameTailBucket = primary.sameTailBucket;
                crowdingBucket = primary.crowdingBucket;
                syncBucket = primary.syncBucket;
                squeezeBucket = primary.squeezeBucket;
                timingStatus = primary.timingBucket;
                riskStatus = primary.riskBucket;
            }

            heatScore = mixFeatureScore("hot");
            coldScore = mixFeatureScore("cold");
            slopeScore = mixSlopeScore();
            densityScore = mixFeatureScore("density");
            repeatScore = mixFeatureScore("repeat");
            oddEvenBias = primary == null ? 0.0d : primary.oddRate;
            bigSmallBias = primary == null ? 0.0d : primary.bigRate;
            adjacentSupport = primary == null ? 0.0d : primary.adjacentRate;
            sameTailSupport = primary == null ? 0.0d : primary.sameTailRate;
            crowdingRisk = primary == null ? 0.0d : primary.crowdingScore;
            crossSyncScore = primary == null ? 0.0d : primary.crossPositionSync;
            squeezeRisk = primary == null ? 0.0d : primary.crossPositionSqueeze;
            timingScore = primary == null ? 0.0d : primary.timingScore;

            double bgStability = background == null ? 0.0d : background.stability;
            stabilityScore =
                    AppConfig.clampProbability(
                            (primary == null ? 0.0d : primary.stability) * 0.65d
                                    + bgStability * 0.35d);
            riskScore =
                    AppConfig.clampProbability(
                            (primary == null ? 0.0d : primary.riskScore) * 0.55d
                                    + squeezeRisk * 0.20d
                                    + crowdingRisk * 0.15d
                                    + ((double) recentLoseStreak) * 0.025d);
            confidence =
                    AppConfig.clampProbability(
                            stabilityScore * 0.35d
                                    + timingScore * 0.25d
                                    + (1.0d - riskScore) * 0.25d
                                    + calcSampleConfidenceV20(sampleTrust, 120) * 0.15d);

            if (riskScore >= 0.720d || falseStrongCount >= 3) {
                stateMode = AppConfig.MODE_BROKEN;
            } else if (V20_TIMING_NOW_OK.equals(timingStatus) && confidence >= 0.560d) {
                stateMode = AppConfig.MODE_CONFIRMED;
            } else if (V20_TIMING_EARLY_RISK.equals(timingStatus)) {
                stateMode = AppConfig.MODE_TRANSITION;
            } else if (coldScore >= 0.620d && slopeScore >= 0.500d) {
                stateMode = AppConfig.MODE_RECOVERING;
            } else {
                stateMode = AppConfig.MODE_CHAOS;
            }

            rebuildKeys();
            summaryText = buildSummaryText();
        }

        private String featureState(WindowFeature f) {
            if (f == null) return V20_STATE_UNKNOWN;
            f.recalc();
            return f.hotColdBucket + "_" + f.slopeBucket;
        }

        private double mixFeatureScore(String type) {
            double v = 0.0d;
            double w = 0.0d;
            if (w12 != null) {
                v += getFeatureValue(w12, type) * 0.18d;
                w += 0.18d;
            }
            if (w24 != null) {
                v += getFeatureValue(w24, type) * 0.25d;
                w += 0.25d;
            }
            if (w48 != null) {
                v += getFeatureValue(w48, type) * 0.24d;
                w += 0.24d;
            }
            if (w96 != null) {
                v += getFeatureValue(w96, type) * 0.18d;
                w += 0.18d;
            }
            if (w360 != null) {
                v += getFeatureValue(w360, type) * 0.15d;
                w += 0.15d;
            }
            return w > 0.0d ? AppConfig.clampProbability(v / w) : 0.0d;
        }

        private double getFeatureValue(WindowFeature f, String type) {
            if (f == null) return 0.0d;
            if ("hot".equals(type)) return f.hotScore;
            if ("cold".equals(type)) return f.coldScore;
            if ("density".equals(type)) return f.density;
            if ("repeat".equals(type)) return f.repeatRate;
            return f.featureScore;
        }

        private double mixSlopeScore() {
            double s = 0.0d;
            int count = 0;
            if (w12 != null) {
                s += scoreFromEdge(w12.slope);
                count++;
            }
            if (w24 != null) {
                s += scoreFromEdge(w24.slope);
                count++;
            }
            if (w48 != null) {
                s += scoreFromEdge(w48.slope);
                count++;
            }
            if (w96 != null) {
                s += scoreFromEdge(w96.slope);
                count++;
            }
            if (w360 != null) {
                s += scoreFromEdge(w360.slope);
                count++;
            }
            return count > 0 ? AppConfig.clampProbability(s / ((double) count)) : 0.0d;
        }

        public void rebuildKeys() {
            exactStateKey =
                    buildV20ExactStateKey(
                            position,
                            w12State,
                            w24State,
                            w48State,
                            w96State,
                            w360State,
                            missBucket,
                            densityBucket,
                            oddEvenBucket,
                            bigSmallBucket,
                            timingStatus,
                            riskStatus);

            relaxedStateKey =
                    buildV20RelaxedStateKey(
                            position,
                            w24State,
                            w48State,
                            missBucket,
                            densityBucket,
                            timingStatus,
                            riskStatus);

            stateKey = exactStateKey;
        }

        public boolean isUsable() {
            recalc();
            return AppConfig.isValidPosition(position)
                    && stateKey != null
                    && stateKey.length() > 0
                    && confidence >= V20_MIN_CAUSE_CONFIDENCE;
        }

        public String buildSummaryText() {
            StringBuilder sb = new StringBuilder();
            sb.append("窗口状态P").append(position);
            sb.append("：W12=").append(w12State);
            sb.append("，W24=").append(w24State);
            sb.append("，W48=").append(w48State);
            sb.append("，W96=").append(w96State);
            sb.append("，W360=").append(w360State);
            sb.append("，遗漏=").append(missBucket);
            sb.append("，密集=").append(densityBucket);
            sb.append("，时机=").append(timingStatus);
            sb.append("，风险=").append(riskStatus);
            sb.append("，置信=").append(UiFormatter.probability(confidence));
            return sb.toString();
        }

        public StateKey toOldStateKey() {
            recalc();
            StateKey k = new StateKey();
            k.position = position;
            k.stateMode = stateMode;
            k.stateKey = stateKey;
            k.trendKey = w24State;
            k.shapeKey = densityBucket;
            k.driftKey = squeezeBucket;
            k.breakKey = riskStatus;
            k.recoveryKey = timingStatus;
            k.hotColdKey = w24State;
            k.oddEvenKey = oddEvenBucket;
            k.highLowKey = bigSmallBucket;
            k.neighborKey = adjacentBucket;
            k.repeatKey = buildV20StateKeyPart("REP", "0");
            k.missKey = missBucket;
            k.riskKey = riskStatus;
            k.trendScore = slopeScore;
            k.shapeScore = densityScore;
            k.driftRisk = squeezeRisk;
            k.breakRisk = riskScore;
            k.recoveryScore = timingScore;
            k.confidence = confidence;
            k.createdAt = createdAt;
            return k;
        }
    }

    public static final class NumberRole {
        public String targetIssue;
        public int position;
        public int number;
        public int k3Mask;
        public String stateKey;
        public String role;
        public String roleKey;

        public double featureSupport;
        public double teacherSupport;
        public double timingSupport;
        public double similarSupport;
        public double counterSupport;
        public double roleSupport;
        public double roleRisk;
        public double roleConfidence;
        public double roleEdge;

        public int sampleCount;
        public int hitCount;
        public int missCount;
        public int recentSample;
        public int recentHit;

        public String summaryText;
        public long updatedAt;

        public NumberRole() {
            targetIssue = "";
            position = 0;
            number = 0;
            k3Mask = 0;
            stateKey = "";
            role = V20_ROLE_UNKNOWN;
            roleKey = "";
            featureSupport = 0.0d;
            teacherSupport = 0.0d;
            timingSupport = 0.0d;
            similarSupport = 0.0d;
            counterSupport = 0.0d;
            roleSupport = 0.0d;
            roleRisk = 0.0d;
            roleConfidence = 0.0d;
            roleEdge = 0.0d;
            sampleCount = 0;
            hitCount = 0;
            missCount = 0;
            recentSample = 0;
            recentHit = 0;
            summaryText = "";
            updatedAt = 0L;
        }

        public void recalc() {
            role = normalizeNumberRoleV20(role);
            if (sampleCount <= 0) sampleCount = hitCount + missCount;

            double hitRate = safeRate(hitCount, sampleCount);
            double recentRate = safeRate(recentHit, recentSample);
            double mixedRate =
                    sampleCount > 0
                            ? (hitRate * 0.65d
                                    + (recentSample > 0 ? recentRate * 0.35d : hitRate * 0.35d))
                            : 0.0d;

            roleSupport =
                    AppConfig.clampProbability(
                            featureSupport * 0.22d
                                    + teacherSupport * 0.24d
                                    + timingSupport * 0.18d
                                    + similarSupport * 0.16d
                                    + counterSupport * 0.10d
                                    + mixedRate * 0.10d);

            roleRisk = AppConfig.clampProbability(roleRisk);
            roleConfidence =
                    AppConfig.clampProbability(
                            calcSampleConfidenceV20(sampleCount, 80) * 0.45d
                                    + roleSupport * 0.35d
                                    + (1.0d - roleRisk) * 0.20d);
            roleEdge = roleSupport - roleRisk - teacherNumberBaseline();
            rebuildKey();
            updatedAt = updatedAt <= 0L ? System.currentTimeMillis() : updatedAt;
            summaryText = buildSummaryText();
        }

        public void rebuildKey() {
            roleKey = buildV20MatrixKey("NR", stateKey, position, number, k3Mask, role, "");
        }

        public boolean isUsable() {
            recalc();
            return AppConfig.isValidPosition(position)
                    && AppConfig.isValidNumber(number)
                    && roleConfidence >= V20_MIN_CAUSE_CONFIDENCE;
        }

        public String buildSummaryText() {
            return "号码角色P"
                    + position
                    + " N"
                    + UiFormatter.number(number)
                    + "："
                    + role
                    + "，支持="
                    + UiFormatter.probability(roleSupport)
                    + "，风险="
                    + UiFormatter.probability(roleRisk)
                    + "，置信="
                    + UiFormatter.probability(roleConfidence)
                    + "，edge="
                    + UiFormatter.signedProbability(roleEdge);
        }
    }

    public static final class SimilarWindowResult {
        public String targetIssue;
        public String exactStateKey;
        public String relaxedStateKey;
        public String matchedStateKey;
        public String similarityClass;
        public int position;
        public int number;
        public int k3Mask;

        public int exactSample;
        public int exactHit;
        public int nearSample;
        public int nearHit;
        public int fuzzySample;
        public int fuzzyHit;

        public double exactHitRate;
        public double nearHitRate;
        public double fuzzyHitRate;
        public double featureDistance;
        public double similarityScore;
        public double trustScore;
        public double supportEdge;
        public double riskScore;
        public double confidence;

        public String matchedReason;
        public String nextAction;
        public String summaryText;
        public long updatedAt;

        public SimilarWindowResult() {
            targetIssue = "";
            exactStateKey = "";
            relaxedStateKey = "";
            matchedStateKey = "";
            similarityClass = V20_SIM_REJECT;
            position = 0;
            number = 0;
            k3Mask = 0;
            exactSample = 0;
            exactHit = 0;
            nearSample = 0;
            nearHit = 0;
            fuzzySample = 0;
            fuzzyHit = 0;
            exactHitRate = 0.0d;
            nearHitRate = 0.0d;
            fuzzyHitRate = 0.0d;
            featureDistance = 1.0d;
            similarityScore = 0.0d;
            trustScore = 0.0d;
            supportEdge = 0.0d;
            riskScore = 0.0d;
            confidence = 0.0d;
            matchedReason = "";
            nextAction = V20_ACTION_HOLD;
            summaryText = "";
            updatedAt = 0L;
        }

        public void recalc() {
            exactHitRate = safeRate(exactHit, exactSample);
            nearHitRate = safeRate(nearHit, nearSample);
            fuzzyHitRate = safeRate(fuzzyHit, fuzzySample);
            similarityClass = buildSimilarityClass();
            similarityScore = buildSimilarityScore();

            double sampleScore =
                    AppConfig.clampProbability(
                            calcSampleConfidenceV20(exactSample, 60) * 0.50d
                                    + calcSampleConfidenceV20(nearSample, 90) * 0.30d
                                    + calcSampleConfidenceV20(fuzzySample, 120) * 0.20d);

            double mixedHit = exactHitRate * 0.55d + nearHitRate * 0.30d + fuzzyHitRate * 0.15d;
            trustScore =
                    AppConfig.clampProbability(
                            sampleScore * 0.45d + similarityScore * 0.35d + mixedHit * 0.20d);
            supportEdge = mixedHit - V20_CAUSE_BASELINE;
            riskScore =
                    AppConfig.clampProbability(
                            (1.0d - similarityScore) * 0.45d
                                    + Math.max(0.0d, V20_CAUSE_BASELINE - mixedHit) * 1.50d);
            confidence =
                    AppConfig.clampProbability(trustScore * 0.65d + (1.0d - riskScore) * 0.35d);
            nextAction = buildAction();
            updatedAt = updatedAt <= 0L ? System.currentTimeMillis() : updatedAt;
            summaryText = buildSummaryText();
        }

        private String buildSimilarityClass() {
            if (exactSample > 0 && featureDistance <= 0.010d) return V20_SIM_EXACT;
            if (nearSample > 0 && featureDistance <= 0.180d) return V20_SIM_NEAR;
            if (fuzzySample > 0 && featureDistance <= 0.360d) return V20_SIM_FUZZY;
            return V20_SIM_REJECT;
        }

        private double buildSimilarityScore() {
            double base = safeDistanceScore(featureDistance);
            if (V20_SIM_EXACT.equals(similarityClass))
                return AppConfig.clampProbability(base * 0.70d + 0.30d);
            if (V20_SIM_NEAR.equals(similarityClass))
                return AppConfig.clampProbability(base * 0.75d + 0.15d);
            if (V20_SIM_FUZZY.equals(similarityClass))
                return AppConfig.clampProbability(base * 0.70d);
            return 0.0d;
        }

        private String buildAction() {
            if (V20_SIM_REJECT.equals(similarityClass)) return V20_ACTION_SHADOW;
            if (riskScore >= 0.700d) return V20_ACTION_FREEZE;
            if (confidence >= 0.560d && supportEdge > 0.0d) return V20_ACTION_BET_NOW;
            if (confidence >= 0.420d && supportEdge > -0.015d) return V20_ACTION_WAIT_CONFIRM;
            return V20_ACTION_SHADOW;
        }

        public boolean canSupportRealtime() {
            recalc();
            return !V20_SIM_REJECT.equals(similarityClass)
                    && confidence >= V20_MIN_CAUSE_CONFIDENCE
                    && riskScore < 0.700d;
        }

        public boolean isExactStrong() {
            recalc();
            return V20_SIM_EXACT.equals(similarityClass)
                    && exactSample >= 18
                    && exactHitRate >= 0.350d
                    && confidence >= 0.500d;
        }

        public String buildSummaryText() {
            return "相似窗口："
                    + similarityClass
                    + "，exact="
                    + exactSample
                    + "/"
                    + exactHit
                    + "("
                    + UiFormatter.probability(exactHitRate)
                    + ")"
                    + "，near="
                    + nearSample
                    + "/"
                    + nearHit
                    + "("
                    + UiFormatter.probability(nearHitRate)
                    + ")"
                    + "，fuzzy="
                    + fuzzySample
                    + "/"
                    + fuzzyHit
                    + "("
                    + UiFormatter.probability(fuzzyHitRate)
                    + ")"
                    + "，distance="
                    + UiFormatter.probability(featureDistance)
                    + "，置信="
                    + UiFormatter.probability(confidence)
                    + "，动作="
                    + nextAction;
        }

        public SimilarResult toOldSimilarResult() {
            recalc();
            SimilarResult r = new SimilarResult();
            r.rawSample = exactSample + nearSample + fuzzySample;
            r.effectiveSample = exactSample + nearSample;
            r.similarHitRate = exactSample > 0 ? exactHitRate : nearHitRate;
            r.recentHitRate = nearHitRate;
            r.avgDistance = featureDistance;
            r.confidence = confidence;
            r.shapeKey = matchedStateKey;
            r.usable = canSupportRealtime() ? 1 : 0;
            return r;
        }
    }

    // ========== 重启第一 / V20 动态高胜率因链系统：因果矩阵 + 教师矩阵 + 反推矩阵 第2/3 ==========

    public static final class CauseMatrixCell {
        public long id;
        public String matrixKey;
        public String stateKey;
        public String relaxedStateKey;
        public String exactStateKey;
        public int position;
        public int number;
        public int k3Mask;
        public String numberRole;
        public String timingStatus;

        public int sampleCount;
        public int hitCount;
        public int missCount;
        public int recent30Sample;
        public int recent30Hit;
        public int recent60Sample;
        public int recent60Hit;
        public int recent120Sample;
        public int recent120Hit;

        public int early1Count;
        public int early2Count;
        public int lateCount;
        public int falseStrongCount;
        public int misorderCount;
        public int teacherSupportCount;
        public int teacherFailCount;
        public int realBetSample;
        public int realBetHit;
        public int shadowSample;
        public int shadowHit;
        public int holdSample;
        public int holdAfterHit;

        public double hitRate;
        public double recent30HitRate;
        public double recent60HitRate;
        public double recent120HitRate;
        public double realBetHitRate;
        public double shadowHitRate;
        public double holdAfterHitRate;
        public double earlyRate;
        public double falseStrongRate;
        public double misorderRate;
        public double teacherSupportRate;
        public double teacherFailRate;

        public double supportScore;
        public double riskScore;
        public double confidence;
        public double causeEdge;
        public double probBoost;
        public double gateAdjust;
        public double actionScore;

        public String causeStatus;
        public String recommendedAction;
        public String supportReason;
        public String riskReason;
        public String summaryText;
        public long updatedAt;

        public CauseMatrixCell() {
            id = 0L;
            matrixKey = "";
            stateKey = "";
            relaxedStateKey = "";
            exactStateKey = "";
            position = 0;
            number = 0;
            k3Mask = 0;
            numberRole = V20_ROLE_UNKNOWN;
            timingStatus = V20_TIMING_UNKNOWN;
            sampleCount = 0;
            hitCount = 0;
            missCount = 0;
            recent30Sample = 0;
            recent30Hit = 0;
            recent60Sample = 0;
            recent60Hit = 0;
            recent120Sample = 0;
            recent120Hit = 0;
            early1Count = 0;
            early2Count = 0;
            lateCount = 0;
            falseStrongCount = 0;
            misorderCount = 0;
            teacherSupportCount = 0;
            teacherFailCount = 0;
            realBetSample = 0;
            realBetHit = 0;
            shadowSample = 0;
            shadowHit = 0;
            holdSample = 0;
            holdAfterHit = 0;
            hitRate = 0.0d;
            recent30HitRate = 0.0d;
            recent60HitRate = 0.0d;
            recent120HitRate = 0.0d;
            realBetHitRate = 0.0d;
            shadowHitRate = 0.0d;
            holdAfterHitRate = 0.0d;
            earlyRate = 0.0d;
            falseStrongRate = 0.0d;
            misorderRate = 0.0d;
            teacherSupportRate = 0.0d;
            teacherFailRate = 0.0d;
            supportScore = 0.0d;
            riskScore = 0.0d;
            confidence = 0.0d;
            causeEdge = 0.0d;
            probBoost = 0.0d;
            gateAdjust = 0.0d;
            actionScore = 0.0d;
            causeStatus = REASON_LEVEL_C_EXPLAIN_ONLY;
            recommendedAction = V20_ACTION_HOLD;
            supportReason = "";
            riskReason = "";
            summaryText = "";
            updatedAt = 0L;
        }

        public void rebuildKey() {
            matrixKey =
                    buildV20MatrixKey(
                            "CAUSE", stateKey, position, number, k3Mask, numberRole, timingStatus);
        }

        public void recalc() {
            numberRole = normalizeNumberRoleV20(numberRole);
            timingStatus = normalizeTimingStatus(timingStatus);

            if (sampleCount <= 0) sampleCount = hitCount + missCount;
            if (missCount <= 0 && sampleCount >= hitCount) missCount = sampleCount - hitCount;

            hitRate = safeRate(hitCount, sampleCount);
            recent30HitRate = safeRate(recent30Hit, recent30Sample);
            recent60HitRate = safeRate(recent60Hit, recent60Sample);
            recent120HitRate = safeRate(recent120Hit, recent120Sample);
            realBetHitRate = safeRate(realBetHit, realBetSample);
            shadowHitRate = safeRate(shadowHit, shadowSample);
            holdAfterHitRate = safeRate(holdAfterHit, holdSample);

            earlyRate = safeRate(early1Count + early2Count, sampleCount);
            falseStrongRate = safeRate(falseStrongCount, sampleCount);
            misorderRate = safeRate(misorderCount, sampleCount);
            teacherSupportRate = safeRate(teacherSupportCount, sampleCount);
            teacherFailRate =
                    safeRate(teacherFailCount, Math.max(1, teacherSupportCount + teacherFailCount));

            double recentMixed =
                    recent30Sample > 0
                            ? recent30HitRate
                            : (recent60Sample > 0 ? recent60HitRate : hitRate);
            double longMixed =
                    hitRate * 0.42d
                            + recentMixed * 0.34d
                            + recent120HitRate * 0.14d
                            + shadowHitRate * 0.10d;

            causeEdge = longMixed - V20_CAUSE_BASELINE;

            double sampleScore =
                    AppConfig.clampProbability(
                            calcSampleConfidenceV20(sampleCount, 120) * 0.45d
                                    + calcSampleConfidenceV20(recent60Sample, 60) * 0.25d
                                    + calcSampleConfidenceV20(shadowSample, 60) * 0.15d
                                    + calcSampleConfidenceV20(realBetSample, 40) * 0.15d);

            riskScore =
                    AppConfig.clampProbability(
                            falseStrongRate * 0.32d
                                    + earlyRate * 0.22d
                                    + misorderRate * 0.16d
                                    + teacherFailRate * 0.18d
                                    + Math.max(0.0d, V20_CAUSE_BASELINE - longMixed) * 1.25d);

            supportScore =
                    AppConfig.clampProbability(
                            longMixed * 0.45d
                                    + realBetHitRate * 0.16d
                                    + shadowHitRate * 0.12d
                                    + teacherSupportRate * 0.12d
                                    + sampleScore * 0.15d
                                    - riskScore * 0.25d);

            confidence =
                    AppConfig.clampProbability(
                            sampleScore * 0.48d
                                    + Math.abs(causeEdge) * 1.40d
                                    + (1.0d - riskScore) * 0.22d);

            probBoost = AppConfig.limitAdjust(causeEdge * 0.45d, V20_MAX_REASON_BOOST);
            gateAdjust = 0.0d;

            if (confidence >= V20_READY_CAUSE_CONFIDENCE
                    && causeEdge > 0.0d
                    && riskScore <= 0.450d) {
                gateAdjust -= 0.006d;
            }
            if (riskScore >= 0.550d || falseStrongRate >= 0.300d) {
                gateAdjust += 0.018d;
            }
            if (earlyRate >= 0.300d) {
                gateAdjust += 0.012d;
            }
            gateAdjust = AppConfig.limitAdjust(gateAdjust, 0.040d);

            actionScore =
                    AppConfig.clampProbability(
                            supportScore * 0.55d + confidence * 0.25d + (1.0d - riskScore) * 0.20d);

            if (sampleCount < REASON_MIN_SAMPLE) {
                causeStatus = REASON_LEVEL_C_EXPLAIN_ONLY;
                recommendedAction = V20_ACTION_SHADOW;
            } else if (riskScore >= 0.700d || falseStrongRate >= 0.450d) {
                causeStatus = REASON_LEVEL_E_FALSE;
                recommendedAction = V20_ACTION_FREEZE;
            } else if (earlyRate >= 0.300d) {
                causeStatus = REASON_LEVEL_B_USEFUL;
                recommendedAction = V20_ACTION_WAIT_CONFIRM;
            } else if (causeEdge >= ELITE45_MIN_REASON_EDGE
                    && confidence >= V20_STRONG_CAUSE_CONFIDENCE) {
                causeStatus = REASON_LEVEL_A_VALID;
                recommendedAction = V20_ACTION_BET_NOW;
            } else if (causeEdge > 0.0d && confidence >= V20_READY_CAUSE_CONFIDENCE) {
                causeStatus = REASON_LEVEL_B_USEFUL;
                recommendedAction = V20_ACTION_WAIT_CONFIRM;
            } else {
                causeStatus = REASON_LEVEL_C_EXPLAIN_ONLY;
                recommendedAction = V20_ACTION_SHADOW;
            }

            buildReasonTexts();
            updatedAt = updatedAt <= 0L ? System.currentTimeMillis() : updatedAt;
            rebuildKey();
            summaryText = buildSummaryText();
        }

        private void buildReasonTexts() {
            StringBuilder support = new StringBuilder();
            StringBuilder risk = new StringBuilder();

            if (causeEdge > 0.0d) support.append("同类窗口命中边际为正");
            if (teacherSupportRate > teacherFailRate) {
                if (support.length() > 0) support.append("；");
                support.append("教师支持多于失败");
            }
            if (shadowHitRate >= V20_SHADOW_HIT_60_MIN) {
                if (support.length() > 0) support.append("；");
                support.append("SHADOW观察表现可用");
            }
            if (realBetSample > 0 && realBetHitRate >= AppConfig.RANDOM_K3_BASELINE) {
                if (support.length() > 0) support.append("；");
                support.append("真实BET历史不弱");
            }

            if (falseStrongRate >= 0.300d) {
                if (risk.length() > 0) risk.append("；");
                risk.append("假强率偏高");
            }
            if (earlyRate >= 0.250d) {
                if (risk.length() > 0) risk.append("；");
                risk.append("早进风险偏高");
            }
            if (misorderRate >= 0.250d) {
                if (risk.length() > 0) risk.append("；");
                risk.append("反事实排序易错");
            }
            if (teacherFailRate >= 0.500d) {
                if (risk.length() > 0) risk.append("；");
                risk.append("教师近期失败偏多");
            }

            supportReason = support.length() == 0 ? "无强支持，只能观察" : support.toString();
            riskReason = risk.length() == 0 ? "无明显硬风险" : risk.toString();
        }

        public boolean canBoostRealtime() {
            recalc();
            return isReasonLevelBoostable(causeStatus)
                    && confidence >= V20_READY_CAUSE_CONFIDENCE
                    && riskScore < 0.600d
                    && !V20_ACTION_FREEZE.equals(recommendedAction);
        }

        public String buildSummaryText() {
            return "因果矩阵P"
                    + position
                    + " N"
                    + UiFormatter.number(number)
                    + " role="
                    + numberRole
                    + "：样本="
                    + sampleCount
                    + "，命中="
                    + UiFormatter.probability(hitRate)
                    + "，近30="
                    + UiFormatter.probability(recent30HitRate)
                    + "，近60="
                    + UiFormatter.probability(recent60HitRate)
                    + "，edge="
                    + UiFormatter.signedProbability(causeEdge)
                    + "，风险="
                    + UiFormatter.probability(riskScore)
                    + "，动作="
                    + recommendedAction;
        }
    }

    public static final class TeacherMatrixCell {
        public long id;
        public String cellKey;
        public String stateKey;
        public String relaxedStateKey;
        public int position;
        public int number;
        public int k3Mask;
        public String numberRole;

        public int longSample;
        public int longHit;
        public int longMiss;
        public int recentSample;
        public int recentHit;
        public int recentMiss;
        public int stateSample;
        public int stateHit;
        public int comboSample;
        public int comboHit;

        public double longHitRate;
        public double recentHitRate;
        public double stateHitRate;
        public double comboHitRate;
        public double decayScore;
        public double riskScore;
        public double confidence;
        public double teacherScore;
        public double teacherEdge;
        public double probAdjust;
        public double gateAdjust;
        public double riskPenalty;

        public String teacherStatus;
        public String teacherReason;
        public String summaryText;
        public long updatedAt;

        public TeacherMatrixCell() {
            id = 0L;
            cellKey = "";
            stateKey = "";
            relaxedStateKey = "";
            position = 0;
            number = 0;
            k3Mask = 0;
            numberRole = V20_ROLE_UNKNOWN;
            longSample = 0;
            longHit = 0;
            longMiss = 0;
            recentSample = 0;
            recentHit = 0;
            recentMiss = 0;
            stateSample = 0;
            stateHit = 0;
            comboSample = 0;
            comboHit = 0;
            longHitRate = 0.0d;
            recentHitRate = 0.0d;
            stateHitRate = 0.0d;
            comboHitRate = 0.0d;
            decayScore = 0.0d;
            riskScore = 0.0d;
            confidence = 0.0d;
            teacherScore = 0.0d;
            teacherEdge = 0.0d;
            probAdjust = 0.0d;
            gateAdjust = 0.0d;
            riskPenalty = 0.0d;
            teacherStatus = V20_TEACHER_EMPTY;
            teacherReason = "";
            summaryText = "";
            updatedAt = 0L;
        }

        public void rebuildKey() {
            cellKey =
                    buildV20MatrixKey(
                            "TEACHER", stateKey, position, number, k3Mask, numberRole, "");
        }

        public void recalc() {
            numberRole = normalizeNumberRoleV20(numberRole);
            if (longSample <= 0) longSample = longHit + longMiss;
            if (recentSample <= 0) recentSample = recentHit + recentMiss;

            longHitRate = safeRate(longHit, longSample);
            recentHitRate = safeRate(recentHit, recentSample);
            stateHitRate = safeRate(stateHit, stateSample);
            comboHitRate = safeRate(comboHit, comboSample);

            double base = teacherNumberBaseline();
            double recentForCalc = recentSample > 0 ? recentHitRate : longHitRate;
            double stateForCalc = stateSample > 0 ? stateHitRate : longHitRate;
            double comboForCalc = comboSample > 0 ? comboHitRate : AppConfig.RANDOM_K3_BASELINE;

            teacherEdge =
                    (longHitRate * 0.38d
                                    + recentForCalc * 0.32d
                                    + stateForCalc * 0.18d
                                    + comboForCalc * 0.12d)
                            - base;
            decayScore = AppConfig.clampProbability(Math.max(0.0d, longHitRate - recentForCalc));
            riskScore =
                    AppConfig.clampProbability(
                            decayScore * 0.45d
                                    + Math.max(0.0d, base - recentForCalc) * 1.70d
                                    + Math.max(0.0d, base - stateForCalc) * 0.80d);

            double sampleScore =
                    AppConfig.clampProbability(
                            calcSampleConfidenceV20(longSample, 120) * 0.40d
                                    + calcSampleConfidenceV20(recentSample, 60) * 0.25d
                                    + calcSampleConfidenceV20(stateSample, 80) * 0.20d
                                    + calcSampleConfidenceV20(comboSample, 60) * 0.15d);

            confidence =
                    AppConfig.clampProbability(
                            sampleScore * 0.55d
                                    + Math.abs(teacherEdge) * 1.35d
                                    + (1.0d - riskScore) * 0.20d);
            teacherScore =
                    AppConfig.clampProbability(
                            0.50d + teacherEdge + confidence * 0.15d - riskScore * 0.25d);

            probAdjust = AppConfig.limitAdjust(teacherEdge * 0.32d, V20_MAX_TEACHER_BOOST);
            if (confidence < V20_MIN_CAUSE_CONFIDENCE) probAdjust *= 0.35d;

            gateAdjust = 0.0d;
            riskPenalty = 0.0d;

            if (longSample <= 0 && stateSample <= 0 && comboSample <= 0) {
                teacherStatus = V20_TEACHER_EMPTY;
            } else if (longSample < AppConfig.TEACHER_MATRIX_MIN_SAMPLE
                    && stateSample < REASON_MIN_SAMPLE) {
                teacherStatus = V20_TEACHER_WEAK;
            } else if (decayScore >= 0.120d && recentSample >= 18) {
                teacherStatus = V20_TEACHER_DECAY;
                gateAdjust += 0.018d;
                riskPenalty += 0.026d;
            } else if (riskScore >= 0.650d || teacherEdge < -0.030d) {
                teacherStatus = V20_TEACHER_RISK;
                gateAdjust += 0.024d;
                riskPenalty += 0.035d;
            } else if (teacherEdge >= ELITE45_MIN_REASON_EDGE
                    && confidence >= V20_STRONG_CAUSE_CONFIDENCE) {
                teacherStatus = V20_TEACHER_STRONG;
                gateAdjust -= 0.006d;
            } else {
                teacherStatus = V20_TEACHER_READY;
            }

            gateAdjust = AppConfig.limitAdjust(gateAdjust, 0.040d);
            riskPenalty = AppConfig.clampProbability(riskPenalty);
            teacherReason = buildTeacherReasonText();
            updatedAt = updatedAt <= 0L ? System.currentTimeMillis() : updatedAt;
            rebuildKey();
            summaryText = buildSummaryText();
        }

        private String buildTeacherReasonText() {
            if (V20_TEACHER_EMPTY.equals(teacherStatus)) return "教师矩阵为空，不加不减，不拦截";
            if (V20_TEACHER_WEAK.equals(teacherStatus)) return "教师样本弱，只提示，不作为硬放行依据";
            if (V20_TEACHER_STRONG.equals(teacherStatus)) return "长期与近期同类状态支持，可软加权";
            if (V20_TEACHER_READY.equals(teacherStatus)) return "教师矩阵可参考，只做软反哺";
            if (V20_TEACHER_DECAY.equals(teacherStatus)) return "过去有效但近期衰退，需要抬门槛或降权";
            if (V20_TEACHER_RISK.equals(teacherStatus)) return "长期或近期表现差，作为风险提示";
            return "";
        }

        public boolean canSoftFeedRealtime() {
            recalc();
            return V20_TEACHER_READY.equals(teacherStatus)
                    || V20_TEACHER_STRONG.equals(teacherStatus);
        }

        public TeacherMatrixProfile toOldTeacherMatrixProfile() {
            recalc();
            TeacherMatrixProfile p = new TeacherMatrixProfile();
            p.stateKey = stateKey;
            p.position = position;
            p.number = number;
            p.sampleCount = longSample;
            p.positiveCount = longHit;
            p.negativeCount = longMiss;
            p.recentSample = recentSample;
            p.recentPositive = recentHit;
            p.recentNegative = recentMiss;
            p.hitRate = longHitRate;
            p.recentHitRate = recentHitRate;
            p.confidence = confidence;
            p.numberScore = teacherScore;
            p.stateScore = stateHitRate;
            p.positionScore = teacherScore;
            p.riskScore = riskScore;
            p.probAdjust = probAdjust;
            p.gateAdjust = gateAdjust;
            p.riskPenalty = riskPenalty;
            if (V20_TEACHER_STRONG.equals(teacherStatus))
                p.status = AppConfig.TEACHER_MATRIX_STATUS_STRONG;
            else if (V20_TEACHER_READY.equals(teacherStatus))
                p.status = AppConfig.TEACHER_MATRIX_STATUS_READY;
            else if (V20_TEACHER_RISK.equals(teacherStatus)
                    || V20_TEACHER_DECAY.equals(teacherStatus))
                p.status = AppConfig.TEACHER_MATRIX_STATUS_RISK;
            else p.status = AppConfig.TEACHER_MATRIX_STATUS_WEAK;
            p.whyText = teacherReason;
            p.summaryText = summaryText;
            p.updatedAt = updatedAt;
            p.rebuildKey();
            return p;
        }

        public String buildSummaryText() {
            return "教师矩阵Cell P"
                    + position
                    + " N"
                    + UiFormatter.number(number)
                    + "：长期="
                    + longSample
                    + "/"
                    + longHit
                    + "("
                    + UiFormatter.probability(longHitRate)
                    + ")"
                    + "，近期="
                    + recentSample
                    + "/"
                    + recentHit
                    + "("
                    + UiFormatter.probability(recentHitRate)
                    + ")"
                    + "，edge="
                    + UiFormatter.signedProbability(teacherEdge)
                    + "，置信="
                    + UiFormatter.probability(confidence)
                    + "，状态="
                    + teacherStatus;
        }
    }

    public static final class TimingProfile {
        public long id;
        public String timingKey;
        public String stateKey;
        public String relaxedStateKey;
        public int position;
        public int number;
        public int k3Mask;
        public String numberRole;

        public int nowSample;
        public int nowHit;
        public int next1Sample;
        public int next1Hit;
        public int next2Sample;
        public int next2Hit;
        public int lateSample;
        public int lateHit;
        public int missSample;

        public double nowHitRate;
        public double next1HitRate;
        public double next2HitRate;
        public double lateHitRate;
        public double earlyRisk;
        public double timingConfidence;
        public double timingBoost;
        public double timingPenalty;
        public String timingStatus;
        public String recommendedAction;
        public String summaryText;
        public long updatedAt;

        public TimingProfile() {
            id = 0L;
            timingKey = "";
            stateKey = "";
            relaxedStateKey = "";
            position = 0;
            number = 0;
            k3Mask = 0;
            numberRole = V20_ROLE_UNKNOWN;
            nowSample = 0;
            nowHit = 0;
            next1Sample = 0;
            next1Hit = 0;
            next2Sample = 0;
            next2Hit = 0;
            lateSample = 0;
            lateHit = 0;
            missSample = 0;
            nowHitRate = 0.0d;
            next1HitRate = 0.0d;
            next2HitRate = 0.0d;
            lateHitRate = 0.0d;
            earlyRisk = 0.0d;
            timingConfidence = 0.0d;
            timingBoost = 0.0d;
            timingPenalty = 0.0d;
            timingStatus = V20_TIMING_UNKNOWN;
            recommendedAction = V20_ACTION_HOLD;
            summaryText = "";
            updatedAt = 0L;
        }

        public void rebuildKey() {
            timingKey =
                    buildV20MatrixKey(
                            "TIMING", stateKey, position, number, k3Mask, numberRole, timingStatus);
        }

        public void recalc() {
            numberRole = normalizeNumberRoleV20(numberRole);
            nowHitRate = safeRate(nowHit, nowSample);
            next1HitRate = safeRate(next1Hit, next1Sample);
            next2HitRate = safeRate(next2Hit, next2Sample);
            lateHitRate = safeRate(lateHit, lateSample);

            double nextBest = Math.max(next1HitRate, next2HitRate);
            earlyRisk = AppConfig.clampProbability(Math.max(0.0d, nextBest - nowHitRate) * 1.50d);
            timingConfidence =
                    AppConfig.clampProbability(
                            calcSampleConfidenceV20(nowSample + next1Sample + next2Sample, 90)
                                            * 0.55d
                                    + Math.abs(nowHitRate - nextBest) * 0.95d
                                    + (1.0d
                                                    - safeRate(
                                                            missSample,
                                                            Math.max(
                                                                    1,
                                                                    nowSample
                                                                            + next1Sample
                                                                            + next2Sample
                                                                            + missSample)))
                                            * 0.20d);

            timingBoost = 0.0d;
            timingPenalty = 0.0d;

            if (nowHitRate >= next1HitRate
                    && nowHitRate >= next2HitRate
                    && nowHitRate >= V20_CAUSE_BASELINE
                    && timingConfidence >= V20_MIN_CAUSE_CONFIDENCE) {
                timingStatus = V20_TIMING_NOW_OK;
                timingBoost =
                        AppConfig.limitAdjust(
                                (nowHitRate - V20_CAUSE_BASELINE) * 0.25d, V20_MAX_TIMING_BOOST);
                recommendedAction = V20_ACTION_BET_NOW;
            } else if (next1HitRate > nowHitRate && next1HitRate >= V20_CAUSE_BASELINE) {
                timingStatus = V20_TIMING_WAIT_NEXT1;
                timingPenalty =
                        AppConfig.limitAdjust(earlyRisk * 0.18d, V20_MAX_EARLY_TIMING_PENALTY);
                recommendedAction = V20_ACTION_WAIT_CONFIRM;
            } else if (next2HitRate > nowHitRate && next2HitRate >= V20_CAUSE_BASELINE) {
                timingStatus = V20_TIMING_WAIT_NEXT2;
                timingPenalty =
                        AppConfig.limitAdjust(earlyRisk * 0.18d, V20_MAX_EARLY_TIMING_PENALTY);
                recommendedAction = V20_ACTION_WAIT_CONFIRM;
            } else if (lateHitRate > nowHitRate) {
                timingStatus = V20_TIMING_LATE;
                timingPenalty =
                        AppConfig.limitAdjust(
                                0.018d + earlyRisk * 0.10d, V20_MAX_EARLY_TIMING_PENALTY);
                recommendedAction = V20_ACTION_SHADOW;
            } else if (timingConfidence < V20_MIN_CAUSE_CONFIDENCE) {
                timingStatus = V20_TIMING_UNKNOWN;
                recommendedAction = V20_ACTION_SHADOW;
            } else {
                timingStatus = V20_TIMING_NOISE;
                recommendedAction = V20_ACTION_HOLD;
            }

            updatedAt = updatedAt <= 0L ? System.currentTimeMillis() : updatedAt;
            rebuildKey();
            summaryText = buildSummaryText();
        }

        public boolean isNowOk() {
            recalc();
            return V20_TIMING_NOW_OK.equals(timingStatus);
        }

        public String buildSummaryText() {
            return "时机画像P"
                    + position
                    + " N"
                    + UiFormatter.number(number)
                    + "：本期="
                    + UiFormatter.probability(nowHitRate)
                    + "，下1="
                    + UiFormatter.probability(next1HitRate)
                    + "，下2="
                    + UiFormatter.probability(next2HitRate)
                    + "，早进风险="
                    + UiFormatter.probability(earlyRisk)
                    + "，状态="
                    + timingStatus
                    + "，动作="
                    + recommendedAction;
        }
    }

    public static final class CounterfactualResult {
        public long id;
        public String counterKey;
        public String targetIssue;
        public String stateKey;
        public int position;
        public int finalRank;
        public int finalK3Mask;
        public int backupRank;
        public int backupK3Mask;
        public int actualNumber;

        public int top1Sample;
        public int top1Hit;
        public int top3Sample;
        public int top3Hit;
        public int rank4to6Sample;
        public int rank4to6Hit;
        public int backupContainsActual;
        public int finalContainsActual;
        public int misorderCount;

        public double top1HitRate;
        public double top3HitRate;
        public double rank4to6HitRate;
        public double misorderRisk;
        public double hiddenStrongScore;
        public double counterSupport;
        public double counterRisk;
        public double counterConfidence;
        public double probBoost;
        public double gateAdjust;

        public String counterStatus;
        public String missReasonType;
        public String recommendedAction;
        public String summaryText;
        public long updatedAt;

        public CounterfactualResult() {
            id = 0L;
            counterKey = "";
            targetIssue = "";
            stateKey = "";
            position = 0;
            finalRank = 0;
            finalK3Mask = 0;
            backupRank = 0;
            backupK3Mask = 0;
            actualNumber = 0;
            top1Sample = 0;
            top1Hit = 0;
            top3Sample = 0;
            top3Hit = 0;
            rank4to6Sample = 0;
            rank4to6Hit = 0;
            backupContainsActual = 0;
            finalContainsActual = 0;
            misorderCount = 0;
            top1HitRate = 0.0d;
            top3HitRate = 0.0d;
            rank4to6HitRate = 0.0d;
            misorderRisk = 0.0d;
            hiddenStrongScore = 0.0d;
            counterSupport = 0.0d;
            counterRisk = 0.0d;
            counterConfidence = 0.0d;
            probBoost = 0.0d;
            gateAdjust = 0.0d;
            counterStatus = "COUNTER_UNKNOWN";
            missReasonType = V20_MISS_UNKNOWN;
            recommendedAction = V20_ACTION_HOLD;
            summaryText = "";
            updatedAt = 0L;
        }

        public void rebuildKey() {
            counterKey =
                    "CF_"
                            + safeIssue(targetIssue)
                            + "_P"
                            + position
                            + "_S"
                            + Math.abs(safeText(stateKey).hashCode())
                            + "_K"
                            + finalK3Mask;
        }

        public void recalc() {
            top1HitRate = safeRate(top1Hit, top1Sample);
            top3HitRate = safeRate(top3Hit, top3Sample);
            rank4to6HitRate = safeRate(rank4to6Hit, rank4to6Sample);

            misorderRisk =
                    AppConfig.clampProbability(
                            safeRate(misorderCount, Math.max(1, top3Sample)) * 0.70d
                                    + (backupContainsActual == 1 && finalContainsActual == 0
                                            ? 0.25d
                                            : 0.0d));
            hiddenStrongScore =
                    AppConfig.clampProbability(
                            Math.max(0.0d, rank4to6HitRate - top3HitRate) * 1.60d
                                    + (backupContainsActual == 1 ? 0.20d : 0.0d));

            counterSupport =
                    AppConfig.clampProbability(
                            top3HitRate * 0.45d
                                    + top1HitRate * 0.25d
                                    + hiddenStrongScore * 0.20d
                                    + (1.0d - misorderRisk) * 0.10d);
            counterRisk =
                    AppConfig.clampProbability(
                            misorderRisk * 0.65d
                                    + Math.max(0.0d, rank4to6HitRate - top1HitRate) * 0.80d);

            counterConfidence =
                    AppConfig.clampProbability(
                            calcSampleConfidenceV20(top3Sample + rank4to6Sample, 100) * 0.52d
                                    + Math.abs(top3HitRate - rank4to6HitRate) * 0.88d
                                    + (1.0d - counterRisk) * 0.18d);

            probBoost =
                    AppConfig.limitAdjust(
                            (counterSupport - V20_CAUSE_BASELINE) * 0.18d, V20_MAX_COUNTER_BOOST);
            gateAdjust = 0.0d;

            if (backupContainsActual == 1 && finalContainsActual == 0) {
                counterStatus = V20_MISS_COUNTERFACTUAL_MISORDER;
                missReasonType = V20_MISS_COUNTERFACTUAL_MISORDER;
                recommendedAction = V20_ACTION_WAIT_CONFIRM;
                gateAdjust += 0.012d;
            } else if (counterRisk >= 0.650d) {
                counterStatus = "COUNTER_RISK";
                missReasonType = V20_MISS_FALSE_STRONG;
                recommendedAction = V20_ACTION_SHADOW;
                gateAdjust += 0.016d;
            } else if (counterConfidence >= V20_READY_CAUSE_CONFIDENCE
                    && counterSupport >= 0.420d) {
                counterStatus = "COUNTER_SUPPORT";
                missReasonType = "";
                recommendedAction = V20_ACTION_BET_NOW;
                gateAdjust -= 0.004d;
            } else {
                counterStatus = "COUNTER_NEUTRAL";
                missReasonType = "";
                recommendedAction = V20_ACTION_HOLD;
            }

            gateAdjust = AppConfig.limitAdjust(gateAdjust, 0.030d);
            updatedAt = updatedAt <= 0L ? System.currentTimeMillis() : updatedAt;
            rebuildKey();
            summaryText = buildSummaryText();
        }

        public boolean isMisorder() {
            recalc();
            return V20_MISS_COUNTERFACTUAL_MISORDER.equals(missReasonType);
        }

        public String buildSummaryText() {
            return "反事实排序P"
                    + position
                    + "：Top1="
                    + UiFormatter.probability(top1HitRate)
                    + "，Top3="
                    + UiFormatter.probability(top3HitRate)
                    + "，R4-6="
                    + UiFormatter.probability(rank4to6HitRate)
                    + "，错排风险="
                    + UiFormatter.probability(misorderRisk)
                    + "，隐藏强号="
                    + UiFormatter.probability(hiddenStrongScore)
                    + "，状态="
                    + counterStatus;
        }
    }

    public static final class RuleHealthProfile {
        public long id;
        public String ruleKey;
        public String stateKey;
        public String sourceKey;
        public String ruleType;

        public int sampleCount;
        public int hitCount;
        public int missCount;
        public int recent30Sample;
        public int recent30Hit;
        public int recent60Sample;
        public int recent60Hit;
        public int recent120Sample;
        public int recent120Hit;
        public int shadowSample;
        public int shadowHit;
        public int consecutiveMiss;
        public int maxLoseStreak;
        public int falseStrongCount;
        public int decayCount;

        public double hitRate;
        public double recent30HitRate;
        public double recent60HitRate;
        public double recent120HitRate;
        public double shadowHitRate;
        public double falseStrongRate;
        public double decayRisk;
        public double ruleEdge;
        public double healthScore;
        public double confidence;
        public double gateAdjust;
        public double probPenalty;

        public boolean ruleHealthy;
        public boolean ruleFrozen;
        public String ruleStatus;
        public String nextAction;
        public String freezeReason;
        public String summaryText;
        public long updatedAt;

        public RuleHealthProfile() {
            id = 0L;
            ruleKey = "";
            stateKey = "";
            sourceKey = "";
            ruleType = "";
            sampleCount = 0;
            hitCount = 0;
            missCount = 0;
            recent30Sample = 0;
            recent30Hit = 0;
            recent60Sample = 0;
            recent60Hit = 0;
            recent120Sample = 0;
            recent120Hit = 0;
            shadowSample = 0;
            shadowHit = 0;
            consecutiveMiss = 0;
            maxLoseStreak = 0;
            falseStrongCount = 0;
            decayCount = 0;
            hitRate = 0.0d;
            recent30HitRate = 0.0d;
            recent60HitRate = 0.0d;
            recent120HitRate = 0.0d;
            shadowHitRate = 0.0d;
            falseStrongRate = 0.0d;
            decayRisk = 0.0d;
            ruleEdge = 0.0d;
            healthScore = 0.0d;
            confidence = 0.0d;
            gateAdjust = 0.0d;
            probPenalty = 0.0d;
            ruleHealthy = false;
            ruleFrozen = false;
            ruleStatus = "RULE_UNKNOWN";
            nextAction = V20_ACTION_SHADOW;
            freezeReason = "";
            summaryText = "";
            updatedAt = 0L;
        }

        public void recalc() {
            if (sampleCount <= 0) sampleCount = hitCount + missCount;
            if (missCount <= 0 && sampleCount >= hitCount) missCount = sampleCount - hitCount;

            hitRate = safeRate(hitCount, sampleCount);
            recent30HitRate = safeRate(recent30Hit, recent30Sample);
            recent60HitRate = safeRate(recent60Hit, recent60Sample);
            recent120HitRate = safeRate(recent120Hit, recent120Sample);
            shadowHitRate = safeRate(shadowHit, shadowSample);
            falseStrongRate = safeRate(falseStrongCount, sampleCount);

            ruleEdge =
                    (hitRate * 0.30d
                                    + recent30HitRate * 0.28d
                                    + recent60HitRate * 0.22d
                                    + shadowHitRate * 0.20d)
                            - V20_CAUSE_BASELINE;
            decayRisk =
                    AppConfig.clampProbability(
                            Math.max(0.0d, hitRate - recent30HitRate) * 1.60d
                                    + safeRate(decayCount, Math.max(1, sampleCount)) * 0.35d);

            healthScore =
                    AppConfig.clampProbability(
                            hitRate * 0.24d
                                    + recent30HitRate * 0.26d
                                    + recent60HitRate * 0.18d
                                    + recent120HitRate * 0.12d
                                    + shadowHitRate * 0.12d
                                    + calcSampleConfidenceV20(sampleCount, 120) * 0.08d
                                    - decayRisk * 0.25d
                                    - falseStrongRate * 0.20d);

            confidence =
                    AppConfig.clampProbability(
                            calcSampleConfidenceV20(sampleCount, 120) * 0.50d
                                    + healthScore * 0.30d
                                    + (1.0d - decayRisk) * 0.20d);

            ruleFrozen = false;
            ruleHealthy = false;
            gateAdjust = 0.0d;
            probPenalty = 0.0d;

            if (consecutiveMiss >= 4
                    || maxLoseStreak >= 5
                    || falseStrongRate >= 0.420d
                    || decayRisk >= 0.380d) {
                ruleFrozen = true;
                ruleStatus = V20_MISS_RULE_DECAY;
                nextAction = V20_ACTION_FREEZE;
                gateAdjust += 0.026d;
                probPenalty += V20_MAX_RULE_DECAY_PENALTY;
                if (freezeReason == null || freezeReason.length() == 0)
                    freezeReason = "规则漂移/同类连挂/假强风险";
            } else if (sampleCount < REASON_MIN_SAMPLE) {
                ruleStatus = V20_MISS_SAMPLE_WEAK;
                nextAction = V20_ACTION_SHADOW;
                gateAdjust += 0.006d;
            } else if (healthScore >= 0.420d
                    && ruleEdge >= 0.0d
                    && confidence >= V20_MIN_CAUSE_CONFIDENCE) {
                ruleHealthy = true;
                ruleStatus = "RULE_READY";
                nextAction = V20_ACTION_BET_NOW;
                gateAdjust -= 0.004d;
            } else {
                ruleStatus = "RULE_WEAK";
                nextAction = V20_ACTION_WAIT_CONFIRM;
                gateAdjust += 0.006d;
            }

            gateAdjust = AppConfig.limitAdjust(gateAdjust, 0.040d);
            probPenalty = AppConfig.clampProbability(probPenalty);
            updatedAt = updatedAt <= 0L ? System.currentTimeMillis() : updatedAt;
            summaryText = buildSummaryText();
        }

        public RuleHealth toOldRuleHealth() {
            recalc();
            RuleHealth r = new RuleHealth();
            r.ruleKey = ruleKey;
            r.stateKey = stateKey;
            r.sourceKey = sourceKey;
            r.sampleCount = sampleCount;
            r.hitCount = hitCount;
            r.recent30Sample = recent30Sample;
            r.recent30Hit = recent30Hit;
            r.recent60Sample = recent60Sample;
            r.recent60Hit = recent60Hit;
            r.shadowSample = shadowSample;
            r.shadowHit = shadowHit;
            r.maxLoseStreak = maxLoseStreak;
            r.hitRate = hitRate;
            r.recent30HitRate = recent30HitRate;
            r.recent60HitRate = recent60HitRate;
            r.shadowHitRate = shadowHitRate;
            r.edgeDecay = decayRisk;
            r.healthScore = healthScore;
            r.isActive = ruleHealthy;
            r.isFrozen = ruleFrozen;
            r.freezeReason = freezeReason;
            r.summaryText = summaryText;
            r.updatedAt = updatedAt;
            return r;
        }

        public String buildSummaryText() {
            return "规则健康Profile "
                    + ruleKey
                    + "：样本="
                    + sampleCount
                    + "，命中="
                    + UiFormatter.probability(hitRate)
                    + "，近30="
                    + UiFormatter.probability(recent30HitRate)
                    + "，近60="
                    + UiFormatter.probability(recent60HitRate)
                    + "，SHADOW="
                    + UiFormatter.probability(shadowHitRate)
                    + "，衰退="
                    + UiFormatter.probability(decayRisk)
                    + "，状态="
                    + ruleStatus
                    + "，动作="
                    + nextAction;
        }
    }

    public static final class ReverseReasonSample {
        public long id;
        public String reverseKey;
        public String targetIssue;
        public String actualIssue;
        public long createdAt;
        public long settledAt;

        public int position;
        public int n1;
        public int n2;
        public int n3;
        public int k3Mask;
        public int actualNumber;
        public boolean hit;
        public int hitInt;

        public String hitRole;
        public String stateKey;
        public String exactStateKey;
        public String relaxedStateKey;
        public String similarityClass;
        public String teacherStatus;
        public String timingStatus;
        public String actionAtDecision;
        public String missReasonType;
        public String nextAction;

        public double finalP;
        public double dynamicGate;
        public double edge;
        public double causeScore;
        public double causeConfidence;
        public double teacherScore;
        public double timingScore;
        public double similarScore;
        public double falseStrongRisk;
        public double ruleHealth;
        public double counterfactualRankScore;
        public double riskScore;

        public int rankAtDecision;
        public int backupContainsActual;
        public int early1Hit;
        public int early2Hit;
        public int positionWrong;
        public int numberScopeMiss;
        public int noiseFlag;

        public String supportReason;
        public String riskReason;
        public String counterfactualSummary;
        public String reverseSummary;
        public String learningAdvice;
        public String summaryText;

        public ReverseReasonSample() {
            id = 0L;
            reverseKey = "";
            targetIssue = "";
            actualIssue = "";
            createdAt = 0L;
            settledAt = 0L;
            position = 0;
            n1 = 0;
            n2 = 0;
            n3 = 0;
            k3Mask = 0;
            actualNumber = 0;
            hit = false;
            hitInt = 0;
            hitRole = V20_ROLE_UNKNOWN;
            stateKey = "";
            exactStateKey = "";
            relaxedStateKey = "";
            similarityClass = V20_SIM_REJECT;
            teacherStatus = V20_TEACHER_EMPTY;
            timingStatus = V20_TIMING_UNKNOWN;
            actionAtDecision = V20_ACTION_HOLD;
            missReasonType = V20_MISS_UNKNOWN;
            nextAction = V20_ACTION_HOLD;
            finalP = 0.0d;
            dynamicGate = AppConfig.DEFAULT_DYNAMIC_GATE;
            edge = 0.0d;
            causeScore = 0.0d;
            causeConfidence = 0.0d;
            teacherScore = 0.0d;
            timingScore = 0.0d;
            similarScore = 0.0d;
            falseStrongRisk = 0.0d;
            ruleHealth = 0.0d;
            counterfactualRankScore = 0.0d;
            riskScore = 0.0d;
            rankAtDecision = 0;
            backupContainsActual = 0;
            early1Hit = 0;
            early2Hit = 0;
            positionWrong = 0;
            numberScopeMiss = 0;
            noiseFlag = 0;
            supportReason = "";
            riskReason = "";
            counterfactualSummary = "";
            reverseSummary = "";
            learningAdvice = "";
            summaryText = "";
        }

        public void setNumbers(int a, int b, int c) {
            n1 = a;
            n2 = b;
            n3 = c;
            k3Mask = buildK3Mask(a, b, c);
        }

        public void rebuildKey() {
            reverseKey = "RRS_" + safeIssue(targetIssue) + "_P" + position + "_K" + k3Mask;
        }

        public void recalc() {
            k3Mask = buildK3Mask(n1, n2, n3);
            hitInt = hit ? 1 : 0;
            finalP = AppConfig.clampProbability(finalP);
            dynamicGate = AppConfig.clampGate(dynamicGate);
            edge = finalP - dynamicGate;
            causeScore = AppConfig.clampProbability(causeScore);
            causeConfidence = AppConfig.clampProbability(causeConfidence);
            teacherScore = AppConfig.clampProbability(teacherScore);
            timingScore = AppConfig.clampProbability(timingScore);
            similarScore = AppConfig.clampProbability(similarScore);
            falseStrongRisk = AppConfig.clampProbability(falseStrongRisk);
            ruleHealth = AppConfig.clampProbability(ruleHealth);
            counterfactualRankScore = AppConfig.clampProbability(counterfactualRankScore);
            riskScore = AppConfig.clampProbability(riskScore);
            hitRole = normalizeNumberRoleV20(hitRole);
            similarityClass = normalizeSimilarityClass(similarityClass);
            teacherStatus = normalizeTeacherStatusV20(teacherStatus);
            timingStatus = normalizeTimingStatus(timingStatus);
            actionAtDecision = normalizeV20Action(actionAtDecision);

            if (hit) {
                missReasonType = "";
                nextAction = buildHitNextAction();
                reverseSummary = buildHitReverseText();
            } else {
                missReasonType = buildMissReasonType();
                nextAction = buildMissNextAction(missReasonType);
                reverseSummary = buildMissReverseText(missReasonType);
            }

            learningAdvice = buildLearningAdvice();
            if (settledAt <= 0L) settledAt = System.currentTimeMillis();
            if (createdAt <= 0L) createdAt = settledAt;
            rebuildKey();
            summaryText = buildSummaryText();
        }

        private String buildHitNextAction() {
            if (V20_ROLE_REBOUND.equals(hitRole) || V20_TIMING_NOW_OK.equals(timingStatus))
                return V20_ACTION_BET_NOW;
            if (V20_TEACHER_STRONG.equals(teacherStatus) || V20_TEACHER_READY.equals(teacherStatus))
                return V20_ACTION_WAIT_CONFIRM;
            return V20_ACTION_SHADOW;
        }

        private String buildMissReasonType() {
            if (positionWrong == 1) return V20_MISS_POSITION_WRONG;
            if (numberScopeMiss == 1) return V20_MISS_NUMBER_SCOPE_MISS;
            if (early1Hit == 1) return V20_MISS_EARLY_1;
            if (early2Hit == 1) return V20_MISS_EARLY_2;
            if (falseStrongRisk >= 0.620d || riskScore >= 0.760d) return V20_MISS_FALSE_STRONG;
            if (backupContainsActual == 1) return V20_MISS_COUNTERFACTUAL_MISORDER;
            if (V20_TEACHER_DECAY.equals(teacherStatus) || V20_TEACHER_RISK.equals(teacherStatus))
                return V20_MISS_TEACHER_DECAY;
            if (ruleHealth > 0.0d && ruleHealth < 0.300d) return V20_MISS_RULE_DECAY;
            if (causeConfidence < V20_MIN_CAUSE_CONFIDENCE) return V20_MISS_SAMPLE_WEAK;
            if (V20_TIMING_LATE.equals(timingStatus)) return V20_MISS_LATE;
            if (noiseFlag == 1) return V20_MISS_NOISE;
            return V20_MISS_NOISE;
        }

        private String buildMissNextAction(String missType) {
            if (V20_MISS_EARLY_1.equals(missType) || V20_MISS_EARLY_2.equals(missType))
                return V20_ACTION_WAIT_CONFIRM;
            if (V20_MISS_FALSE_STRONG.equals(missType)) return V20_ACTION_FREEZE;
            if (V20_MISS_TEACHER_DECAY.equals(missType)) return V20_ACTION_SHADOW;
            if (V20_MISS_RULE_DECAY.equals(missType)) return V20_ACTION_FREEZE;
            if (V20_MISS_COUNTERFACTUAL_MISORDER.equals(missType)) return V20_ACTION_WAIT_CONFIRM;
            if (V20_MISS_SAMPLE_WEAK.equals(missType)) return V20_ACTION_SHADOW;
            if (V20_MISS_NOISE.equals(missType)) return V20_ACTION_HOLD;
            return V20_ACTION_SHADOW;
        }

        private String buildHitReverseText() {
            StringBuilder sb = new StringBuilder();
            sb.append("命中归因：");
            sb.append("角色=").append(hitRole);
            sb.append("，时机=").append(timingStatus);
            sb.append("，教师=").append(teacherStatus);
            sb.append("，相似=").append(similarityClass);
            sb.append("，说明该窗口下该角色本次有效");
            return sb.toString();
        }

        private String buildMissReverseText(String missType) {
            StringBuilder sb = new StringBuilder();
            sb.append("未中归因：").append(missType);
            if (V20_MISS_EARLY_1.equals(missType)) sb.append("，同位置下1期才出，降低NOW_OK，提高WAIT_NEXT1");
            else if (V20_MISS_EARLY_2.equals(missType))
                sb.append("，同位置下2期才出，降低NOW_OK，提高WAIT_NEXT2");
            else if (V20_MISS_FALSE_STRONG.equals(missType)) sb.append("，高分高证据仍挂，增加假强风险并冻结同类");
            else if (V20_MISS_COUNTERFACTUAL_MISORDER.equals(missType))
                sb.append("，备用候选覆盖实际号，说明排序权重错");
            else if (V20_MISS_TEACHER_DECAY.equals(missType)) sb.append("，教师矩阵近期失效，降权或抬门槛");
            else if (V20_MISS_RULE_DECAY.equals(missType)) sb.append("，规则健康衰退，同类状态先冻结或SHADOW");
            else if (V20_MISS_SAMPLE_WEAK.equals(missType)) sb.append("，样本不足，只记录不强学");
            else if (V20_MISS_NOISE.equals(missType)) sb.append("，无明确证据，不硬改规则，避免过拟合");
            else if (V20_MISS_POSITION_WRONG.equals(missType)) sb.append("，位置选择错误，不能用其它位置冒充命中");
            else if (V20_MISS_NUMBER_SCOPE_MISS.equals(missType)) sb.append("，位置对但三码覆盖错，修正号码角色/排序");
            return sb.toString();
        }

        private String buildLearningAdvice() {
            if (hit) {
                return "同类窗口可保留该角色权重；若后续连续有效，再提升教师/因果置信";
            }
            String m = normalizeMissReasonTypeV20(missReasonType);
            if (V20_MISS_EARLY_1.equals(m) || V20_MISS_EARLY_2.equals(m))
                return "下次同类窗口不要急进，改WAIT_CONFIRM或SHADOW观察时机";
            if (V20_MISS_FALSE_STRONG.equals(m)) return "同类状态进入假强冻结，冻结期只允许SHADOW";
            if (V20_MISS_COUNTERFACTUAL_MISORDER.equals(m)) return "提高备用正确证据权重，降低最终候选错误证据权重";
            if (V20_MISS_TEACHER_DECAY.equals(m))
                return "教师矩阵从STRONG/READY降级，ProbabilityEngine取消加分";
            if (V20_MISS_RULE_DECAY.equals(m)) return "规则健康降分，DynamicGateEngine抬门槛";
            if (V20_MISS_SAMPLE_WEAK.equals(m) || V20_MISS_NOISE.equals(m)) return "只记录，不强学，避免噪声污染";
            return "按错因分类更新矩阵，不写入真实胜率以外的样本";
        }

        public void fromDecision(Decision d) {
            if (d == null) return;
            targetIssue = d.targetIssue == null ? "" : d.targetIssue;
            position = d.position;
            setNumbers(d.n1, d.n2, d.n3);
            finalP = d.finalP;
            dynamicGate = d.dynamicGate;
            edge = d.edge;
            stateKey = d.stateKey == null ? "" : d.stateKey;
            exactStateKey = stateKey;
            relaxedStateKey = stateKey;
            teacherStatus = normalizeTeacherStatusV20(d.teacherMatrixStatus);
            timingStatus = V20_TIMING_UNKNOWN;
            actionAtDecision =
                    AppConfig.DECISION_BET.equals(AppConfig.normalizeDecision(d.decision))
                            ? V20_ACTION_BET_NOW
                            : (AppConfig.DECISION_SHADOW.equals(
                                            AppConfig.normalizeDecision(d.decision))
                                    ? V20_ACTION_SHADOW
                                    : V20_ACTION_HOLD);
            causeScore = d.reasonScore;
            causeConfidence = d.causalConfidence;
            teacherScore =
                    d.teacherMatrixScore > 0.0d
                            ? d.teacherMatrixScore
                            : buildTeacherMatrixScore(
                                    d.teacherMatrixNumberScore,
                                    d.teacherMatrixPositionScore,
                                    d.teacherMatrixStateScore,
                                    d.teacherMatrixComboScore);
            falseStrongRisk = d.falseStrongRisk;
            riskScore = d.evidenceRisk;
            supportReason = d.reasonSummary == null ? "" : d.reasonSummary;
        }

        public void applySettlementResult(SettlementResult r) {
            if (r == null) return;
            actualIssue = r.targetIssue;
            actualNumber = r.actualNumber;
            hit = r.hit;
            hitInt = hit ? 1 : 0;
            if (!hit && maskContains(k3Mask, actualNumber)) {
                numberScopeMiss = 0;
            }
            settledAt = r.settledAt;
            recalc();
        }

        public String buildSummaryText() {
            return "反推样本"
                    + targetIssue
                    + " P"
                    + position
                    + " 预测"
                    + maskToText(k3Mask)
                    + " 实开"
                    + UiFormatter.number(actualNumber)
                    + "，结果="
                    + (hit ? "命中" : "未中")
                    + "，错因="
                    + missReasonType
                    + "，下次动作="
                    + nextAction
                    + "，"
                    + reverseSummary;
        }
    }

    // ========== 重启第一 / V20 动态高胜率因链系统：动作矩阵 + 决策上下文 + 兼容工具 第3/3 ==========

    public static final class V20ActionMatrix {
        public long id;
        public String actionKey;
        public String targetIssue;
        public String stateKey;
        public String relaxedStateKey;
        public String exactStateKey;
        public String sameClassKey;
        public String ruleKey;
        public String sourceKey;
        public int position;
        public int number;
        public int k3Mask;

        public String teacherStatus;
        public String timingStatus;
        public String similarityClass;
        public String ruleStatus;
        public String missReasonType;
        public String gradeText;
        public String finalAction;

        public double finalP;
        public double dynamicGate;
        public double edge;
        public double causeScore;
        public double causeConfidence;
        public double teacherScore;
        public double timingScore;
        public double similarScore;
        public double counterScore;
        public double ruleHealthScore;
        public double shadowHealthScore;
        public double falseStrongRisk;
        public double ruleDecayRisk;
        public double earlyTimingRisk;
        public double weakSampleRisk;
        public double totalSupport;
        public double totalRisk;
        public double actionConfidence;

        public int finalPOverGate;
        public int causePass;
        public int teacherPass;
        public int timingPass;
        public int similarPass;
        public int rulePass;
        public int falseStrongFrozen;
        public int sameClassFrozen;
        public int backtestTrustPass;
        public int k3Legal;
        public int targetIssueOk;
        public int noOpenRealBet;

        public String supportText;
        public String riskText;
        public String blockCode;
        public String summaryText;
        public long updatedAt;

        public V20ActionMatrix() {
            id = 0L;
            actionKey = "";
            targetIssue = "";
            stateKey = "";
            relaxedStateKey = "";
            exactStateKey = "";
            sameClassKey = "";
            ruleKey = "";
            sourceKey = "";
            position = 0;
            number = 0;
            k3Mask = 0;
            teacherStatus = V20_TEACHER_EMPTY;
            timingStatus = V20_TIMING_UNKNOWN;
            similarityClass = V20_SIM_REJECT;
            ruleStatus = "RULE_UNKNOWN";
            missReasonType = V20_MISS_UNKNOWN;
            gradeText = "UNKNOWN";
            finalAction = V20_ACTION_HOLD;
            finalP = 0.0d;
            dynamicGate = AppConfig.DEFAULT_DYNAMIC_GATE;
            edge = 0.0d;
            causeScore = 0.0d;
            causeConfidence = 0.0d;
            teacherScore = 0.0d;
            timingScore = 0.0d;
            similarScore = 0.0d;
            counterScore = 0.0d;
            ruleHealthScore = 0.0d;
            shadowHealthScore = 0.0d;
            falseStrongRisk = 0.0d;
            ruleDecayRisk = 0.0d;
            earlyTimingRisk = 0.0d;
            weakSampleRisk = 0.0d;
            totalSupport = 0.0d;
            totalRisk = 0.0d;
            actionConfidence = 0.0d;
            finalPOverGate = 0;
            causePass = 0;
            teacherPass = 0;
            timingPass = 0;
            similarPass = 0;
            rulePass = 0;
            falseStrongFrozen = 0;
            sameClassFrozen = 0;
            backtestTrustPass = 0;
            k3Legal = 0;
            targetIssueOk = 0;
            noOpenRealBet = 1;
            supportText = "";
            riskText = "";
            blockCode = "";
            summaryText = "";
            updatedAt = 0L;
        }

        public void rebuildKey() {
            actionKey =
                    "ACT_"
                            + safeIssue(targetIssue)
                            + "_P"
                            + position
                            + "_K"
                            + k3Mask
                            + "_S"
                            + Math.abs(safeText(stateKey).hashCode());
        }

        public void recalc() {
            teacherStatus = normalizeTeacherStatusV20(teacherStatus);
            timingStatus = normalizeTimingStatus(timingStatus);
            similarityClass = normalizeSimilarityClass(similarityClass);
            missReasonType = normalizeMissReasonTypeV20(missReasonType);

            finalP = AppConfig.clampProbability(finalP);
            dynamicGate = AppConfig.clampGate(dynamicGate);
            edge = finalP - dynamicGate;

            causeScore = AppConfig.clampProbability(causeScore);
            causeConfidence = AppConfig.clampProbability(causeConfidence);
            teacherScore = AppConfig.clampProbability(teacherScore);
            timingScore = AppConfig.clampProbability(timingScore);
            similarScore = AppConfig.clampProbability(similarScore);
            counterScore = AppConfig.clampProbability(counterScore);
            ruleHealthScore = AppConfig.clampProbability(ruleHealthScore);
            shadowHealthScore = AppConfig.clampProbability(shadowHealthScore);
            falseStrongRisk = AppConfig.clampProbability(falseStrongRisk);
            ruleDecayRisk = AppConfig.clampProbability(ruleDecayRisk);
            earlyTimingRisk = AppConfig.clampProbability(earlyTimingRisk);
            weakSampleRisk = AppConfig.clampProbability(weakSampleRisk);

            int[] actionNums = maskToNumbers(k3Mask);
            k3Legal =
                    actionNums != null
                                    && actionNums.length >= 3
                                    && AppConfig.isPlayRuleLegal(
                                            position,
                                            AppConfig.FIXED_K,
                                            actionNums[0],
                                            actionNums[1],
                                            actionNums[2])
                            ? 1
                            : 0;
            finalPOverGate = finalP >= dynamicGate ? 1 : 0;
            causePass =
                    causeScore >= V20_GRADE_B_MIN_SCORE
                                    && causeConfidence >= V20_MIN_CAUSE_CONFIDENCE
                            ? 1
                            : 0;

            teacherPass = 0;
            if (V20_TEACHER_EMPTY.equals(teacherStatus) || V20_TEACHER_WEAK.equals(teacherStatus))
                teacherPass = 1;
            if (V20_TEACHER_READY.equals(teacherStatus) || V20_TEACHER_STRONG.equals(teacherStatus))
                teacherPass = 1;
            if (V20_TEACHER_RISK.equals(teacherStatus) || V20_TEACHER_DECAY.equals(teacherStatus))
                teacherPass = 0;

            timingPass = V20_TIMING_NOW_OK.equals(timingStatus) ? 1 : 0;
            similarPass =
                    !V20_SIM_REJECT.equals(similarityClass)
                                    && similarScore >= V20_MIN_CAUSE_CONFIDENCE
                            ? 1
                            : 0;
            rulePass = ruleHealthScore >= 0.300d && ruleDecayRisk < 0.600d ? 1 : 0;

            totalSupport =
                    AppConfig.clampProbability(
                            finalP * 0.22d
                                    + causeScore * 0.22d
                                    + causeConfidence * 0.12d
                                    + teacherScore * 0.12d
                                    + timingScore * 0.10d
                                    + similarScore * 0.08d
                                    + counterScore * 0.06d
                                    + ruleHealthScore * 0.04d
                                    + shadowHealthScore * 0.04d);

            totalRisk =
                    AppConfig.clampProbability(
                            falseStrongRisk * 0.28d
                                    + ruleDecayRisk * 0.22d
                                    + earlyTimingRisk * 0.18d
                                    + weakSampleRisk * 0.14d
                                    + (falseStrongFrozen == 1 ? 0.25d : 0.0d)
                                    + (sameClassFrozen == 1 ? 0.20d : 0.0d)
                                    + (k3Legal == 0 ? 0.30d : 0.0d));

            actionConfidence =
                    AppConfig.clampProbability(totalSupport * 0.60d + (1.0d - totalRisk) * 0.40d);

            finalAction = buildFinalAction();
            buildTexts();
            rebuildKey();
            updatedAt = updatedAt <= 0L ? System.currentTimeMillis() : updatedAt;
            summaryText = buildSummaryText();
        }

        private String buildFinalAction() {
            blockCode = "";

            if (k3Legal == 0) {
                blockCode = "K3_ILLEGAL";
                return V20_ACTION_REJECT;
            }
            if (targetIssueOk == 0) {
                blockCode = "TARGET_ISSUE_NOT_OK";
                return V20_ACTION_HOLD;
            }
            if (noOpenRealBet == 0) {
                blockCode = "OPEN_REAL_BET_EXISTS";
                return V20_ACTION_HOLD;
            }
            if (falseStrongFrozen == 1 || sameClassFrozen == 1) {
                blockCode = "FALSE_STRONG_OR_SAME_CLASS_FROZEN";
                return V20_ACTION_FREEZE;
            }
            if (rulePass == 0 && ruleDecayRisk >= 0.600d) {
                blockCode = V20_MISS_RULE_DECAY;
                return V20_ACTION_FREEZE;
            }
            if (teacherPass == 0) {
                blockCode = teacherStatus;
                return V20_ACTION_SHADOW;
            }
            if (timingPass == 0) {
                if (V20_TIMING_WAIT_NEXT1.equals(timingStatus)
                        || V20_TIMING_WAIT_NEXT2.equals(timingStatus)
                        || earlyTimingRisk >= 0.350d) {
                    blockCode = timingStatus;
                    return V20_ACTION_WAIT_CONFIRM;
                }
                blockCode = timingStatus;
                return V20_ACTION_SHADOW;
            }
            if (backtestTrustPass == 0) {
                blockCode = "BACKTEST_TRUST_WEAK";
                return V20_ACTION_SHADOW;
            }
            if (weakSampleRisk >= 0.650d) {
                blockCode = V20_MISS_SAMPLE_WEAK;
                return V20_ACTION_SHADOW;
            }
            if (finalPOverGate == 1
                    && causePass == 1
                    && timingPass == 1
                    && teacherPass == 1
                    && rulePass == 1
                    && totalRisk < 0.620d
                    && actionConfidence >= V20_READY_CAUSE_CONFIDENCE) {
                return V20_ACTION_BET_NOW;
            }
            if (causePass == 1 && actionConfidence >= V20_MIN_CAUSE_CONFIDENCE) {
                blockCode = "WAIT_MORE_CONFIRM";
                return V20_ACTION_WAIT_CONFIRM;
            }
            blockCode = "CAUSE_OR_GATE_NOT_ENOUGH";
            return V20_ACTION_HOLD;
        }

        private void buildTexts() {
            StringBuilder sup = new StringBuilder();
            StringBuilder risk = new StringBuilder();

            if (finalPOverGate == 1) sup.append("finalP过动态门槛");
            if (causePass == 1) {
                if (sup.length() > 0) sup.append("；");
                sup.append("因链置信达标");
            }
            if (teacherPass == 1) {
                if (sup.length() > 0) sup.append("；");
                sup.append("教师矩阵未给硬风险");
            }
            if (timingPass == 1) {
                if (sup.length() > 0) sup.append("；");
                sup.append("时机NOW_OK");
            }
            if (similarPass == 1) {
                if (sup.length() > 0) sup.append("；");
                sup.append("相似窗口可参考");
            }
            if (rulePass == 1) {
                if (sup.length() > 0) sup.append("；");
                sup.append("规则健康可用");
            }

            if (falseStrongFrozen == 1) risk.append("假强冻结");
            if (sameClassFrozen == 1) {
                if (risk.length() > 0) risk.append("；");
                risk.append("同类状态冻结");
            }
            if (teacherPass == 0) {
                if (risk.length() > 0) risk.append("；");
                risk.append("教师风险/衰退");
            }
            if (timingPass == 0) {
                if (risk.length() > 0) risk.append("；");
                risk.append("时机未确认");
            }
            if (rulePass == 0) {
                if (risk.length() > 0) risk.append("；");
                risk.append("规则健康不足");
            }
            if (weakSampleRisk >= 0.650d) {
                if (risk.length() > 0) risk.append("；");
                risk.append("样本弱");
            }
            if (finalPOverGate == 0) {
                if (risk.length() > 0) risk.append("；");
                risk.append("finalP未过门槛");
            }

            supportText = sup.length() == 0 ? "支持证据不足" : sup.toString();
            riskText = risk.length() == 0 ? "无硬风险" : risk.toString();
        }

        public boolean isBetNow() {
            recalc();
            return V20_ACTION_BET_NOW.equals(finalAction);
        }

        public boolean isShadowOnly() {
            recalc();
            return V20_ACTION_SHADOW.equals(finalAction);
        }

        public boolean isFrozen() {
            recalc();
            return V20_ACTION_FREEZE.equals(finalAction);
        }

        public String toDecisionValue() {
            recalc();
            if (V20_ACTION_BET_NOW.equals(finalAction)) return AppConfig.DECISION_BET;
            if (V20_ACTION_SHADOW.equals(finalAction)) return AppConfig.DECISION_SHADOW;
            return AppConfig.DECISION_HOLD;
        }

        public String buildSummaryText() {
            return "动作矩阵P"
                    + position
                    + " "
                    + maskToText(k3Mask)
                    + "：动作="
                    + finalAction
                    + "，finalP="
                    + UiFormatter.probability(finalP)
                    + "，gate="
                    + UiFormatter.probability(dynamicGate)
                    + "，因链="
                    + UiFormatter.probability(causeScore)
                    + "，支持="
                    + UiFormatter.probability(totalSupport)
                    + "，风险="
                    + UiFormatter.probability(totalRisk)
                    + "，block="
                    + blockCode;
        }
    }

    public static final class V20ProbabilityFusion {
        public String targetIssue;
        public int position;
        public int k3Mask;
        public String stateKey;

        public double baseP;
        public double causeBoost;
        public double teacherBoost;
        public double timingBoost;
        public double similarBoost;
        public double counterBoost;
        public double shadowBoost;

        public double falseStrongPenalty;
        public double ruleDecayPenalty;
        public double earlyTimingPenalty;
        public double weakSamplePenalty;
        public double teacherRiskPenalty;
        public double totalBoost;
        public double totalPenalty;

        public double rawP;
        public double finalP;
        public double dynamicGate;
        public double edge;
        public double confidence;
        public String probabilityMode;
        public String summaryText;

        public V20ProbabilityFusion() {
            targetIssue = "";
            position = 0;
            k3Mask = 0;
            stateKey = "";
            baseP = AppConfig.RANDOM_K3_BASELINE;
            causeBoost = 0.0d;
            teacherBoost = 0.0d;
            timingBoost = 0.0d;
            similarBoost = 0.0d;
            counterBoost = 0.0d;
            shadowBoost = 0.0d;
            falseStrongPenalty = 0.0d;
            ruleDecayPenalty = 0.0d;
            earlyTimingPenalty = 0.0d;
            weakSamplePenalty = 0.0d;
            teacherRiskPenalty = 0.0d;
            totalBoost = 0.0d;
            totalPenalty = 0.0d;
            rawP = AppConfig.RANDOM_K3_BASELINE;
            finalP = 0.0d;
            dynamicGate = AppConfig.DEFAULT_DYNAMIC_GATE;
            edge = 0.0d;
            confidence = 0.0d;
            probabilityMode = "V20_CAUSE_FUSION";
            summaryText = "";
        }

        public void recalc() {
            baseP =
                    AppConfig.clampProbability(
                            baseP <= 0.0d ? AppConfig.RANDOM_K3_BASELINE : baseP);

            causeBoost = AppConfig.limitAdjust(causeBoost, V20_MAX_REASON_BOOST);
            teacherBoost = AppConfig.limitAdjust(teacherBoost, V20_MAX_TEACHER_BOOST);
            timingBoost = AppConfig.limitAdjust(timingBoost, V20_MAX_TIMING_BOOST);
            similarBoost = AppConfig.limitAdjust(similarBoost, V20_MAX_SIMILAR_BOOST);
            counterBoost = AppConfig.limitAdjust(counterBoost, V20_MAX_COUNTER_BOOST);
            shadowBoost = AppConfig.limitAdjust(shadowBoost, 0.012d);

            falseStrongPenalty = AppConfig.clampProbability(falseStrongPenalty);
            if (falseStrongPenalty > V20_MAX_FALSE_STRONG_PENALTY)
                falseStrongPenalty = V20_MAX_FALSE_STRONG_PENALTY;

            ruleDecayPenalty = AppConfig.clampProbability(ruleDecayPenalty);
            if (ruleDecayPenalty > V20_MAX_RULE_DECAY_PENALTY)
                ruleDecayPenalty = V20_MAX_RULE_DECAY_PENALTY;

            earlyTimingPenalty = AppConfig.clampProbability(earlyTimingPenalty);
            if (earlyTimingPenalty > V20_MAX_EARLY_TIMING_PENALTY)
                earlyTimingPenalty = V20_MAX_EARLY_TIMING_PENALTY;

            weakSamplePenalty = AppConfig.clampProbability(weakSamplePenalty);
            if (weakSamplePenalty > V20_MAX_WEAK_SAMPLE_PENALTY)
                weakSamplePenalty = V20_MAX_WEAK_SAMPLE_PENALTY;

            teacherRiskPenalty = AppConfig.clampProbability(teacherRiskPenalty);
            if (teacherRiskPenalty > teacherMatrixMaxRiskPenalty())
                teacherRiskPenalty = teacherMatrixMaxRiskPenalty();

            totalBoost =
                    causeBoost
                            + teacherBoost
                            + timingBoost
                            + similarBoost
                            + counterBoost
                            + shadowBoost;
            totalPenalty =
                    falseStrongPenalty
                            + ruleDecayPenalty
                            + earlyTimingPenalty
                            + weakSamplePenalty
                            + teacherRiskPenalty;

            rawP = AppConfig.clampProbability(baseP + totalBoost - totalPenalty);
            finalP = rawP;
            dynamicGate = AppConfig.clampGate(dynamicGate);
            edge = finalP - dynamicGate;
            confidence =
                    AppConfig.clampProbability(0.50d + totalBoost * 3.0d - totalPenalty * 2.5d);
            summaryText = buildSummaryText();
        }

        public ProbabilityResult toProbabilityResult() {
            recalc();
            ProbabilityResult r = new ProbabilityResult();
            r.rawP = rawP;
            r.calibratedP = finalP;
            r.finalP = finalP;
            r.dynamicGate = dynamicGate;
            r.reasonAdjust = causeBoost;
            r.teacherMatrixAdjust = teacherBoost - teacherRiskPenalty;
            r.reasonP = AppConfig.clampProbability(baseP + causeBoost);
            r.teacherMatrixP =
                    AppConfig.clampProbability(baseP + teacherBoost - teacherRiskPenalty);
            r.similarP = AppConfig.clampProbability(baseP + similarBoost);
            r.auditAdjustedP = finalP;
            r.gateAdjust = 0.0d;
            r.mode = probabilityMode;
            return r;
        }

        public String buildSummaryText() {
            return "V20概率融合：base="
                    + UiFormatter.probability(baseP)
                    + "，因链+"
                    + UiFormatter.signedProbability(causeBoost)
                    + "，教师+"
                    + UiFormatter.signedProbability(teacherBoost)
                    + "，时机+"
                    + UiFormatter.signedProbability(timingBoost)
                    + "，相似+"
                    + UiFormatter.signedProbability(similarBoost)
                    + "，反事实+"
                    + UiFormatter.signedProbability(counterBoost)
                    + "，假强-"
                    + UiFormatter.probability(falseStrongPenalty)
                    + "，规则-"
                    + UiFormatter.probability(ruleDecayPenalty)
                    + "，早进-"
                    + UiFormatter.probability(earlyTimingPenalty)
                    + "，样本-"
                    + UiFormatter.probability(weakSamplePenalty)
                    + "，finalP="
                    + UiFormatter.probability(finalP);
        }
    }

    public static final class V20DecisionContext {
        public String baseIssue;
        public String targetIssue;
        public long createdAt;
        public int position;
        public int n1;
        public int n2;
        public int n3;
        public int k3Mask;

        public WindowState windowState;
        public NumberRole role1;
        public NumberRole role2;
        public NumberRole role3;
        public CauseMatrixCell cause1;
        public CauseMatrixCell cause2;
        public CauseMatrixCell cause3;
        public TeacherMatrixCell teacher1;
        public TeacherMatrixCell teacher2;
        public TeacherMatrixCell teacher3;
        public TimingProfile timingProfile;
        public SimilarWindowResult similarWindow;
        public CounterfactualResult counterfactual;
        public RuleHealthProfile ruleHealth;
        public ShadowHealth shadowHealth;
        public FalseStrongRecord falseStrongRecord;
        public V20ProbabilityFusion fusion;
        public V20ActionMatrix actionMatrix;

        public CandidateGrade grade;
        public double finalCauseScore;
        public double finalP;
        public double dynamicGate;
        public double edge;
        public double confidence;
        public double riskScore;
        /* V20主链闭环字段：只承接字段，不计算概率，不决定BET */
        public String clusterKey;
        public String factorKey;
        public double positionNeedScore;
        public double numberSupplyScore;
        public double coverageQuality;
        public double thirdQuality;
        public double counterEvidenceScore;
        public int similarSampleCount;
        public String adaptiveStatus;
        public String shadowPromotionStatus;
        public double finalPBeforeDiagnosis;
        public double finalPAfterDiagnosis;
        public double gate;
        public String rejectCode;
        public int realProbeAllowed;

        /* 链路修改升级V2字段：只承接，不计算概率，不决定BET */
        public double realBetFrequency50;
        public double realBetFrequency100;
        public double realBetFrequency300;
        public double frequencyDebtScore;
        public double frequencySupportScore;
        public double frequencyGateAdjust;
        public double qualifiedProbeScore;
        public double shadowPromotionScore;
        public double baseGate;
        public double riskAdjustedGate;
        public double effectiveProbeGate;
        public double rawEdge;
        public double effectiveProbeEdge;
        public double needSupplyScore;
        public double counterRisk;
        public int qualifiedProbe;
        public int realTrade;
        public int coverageAvailable;
        public int thirdQualityAvailable;
        public int promotionDomainAvailable;
        public String frequencyState;
        public String frequencyStateVersion;
        public String shadowPromotionDomain;
        public String decisionPath;
        public String betDomain;
        public String probeRejectReason;
        public String probeEligibilityVersion;
        public String ruleHealthStatus;
        public String snapshotFreshness;
        public String warmProfileFreshness;
        public String coldProfileFreshness;
        public String deepProfileFreshness;
        public String decisionVersionV2;
        public String evidenceAsOfIssue;
        public String snapshotAsOfIssue;
        public long snapshotCreatedAt;
        public long frequencyStateUpdatedAt;
        public int frequencyFloorRequiredCount;
        public int frequencyFloorTargetCount;
        public int frequencyFloorActualCount;
        public int frequencyFloorDebtCount;
        public int frequencyTargetDebtCount;
        public int frequencyFloorMandatory;
        public int frequencyFloorNoUpperCap;
        public double frequencyFloorRankScore;
        public String frequencyFloorVersion;

        /* 73487246：四窗口平滑频率、冻结域、Teacher/Dynamic分层审计。 */
        public double realBetFrequency20;
        public int frequencyObserved20;
        public int frequencyObserved50;
        public int frequencyObserved100;
        public int frequencyObserved300;
        public int frequencyBetCount20;
        public int frequencyBetCount50;
        public int frequencyBetCount100;
        public int frequencyBetCount300;
        public int frequencyDebt20;
        public int frequencyDebt50;
        public int frequencyDebt100;
        public int frequencyDebt300;
        public int frequencyTargetDebt20;
        public int frequencyTargetDebt50;
        public int frequencyTargetDebt100;
        public int frequencyTargetDebt300;
        public String frequencyUrgentWindow;
        public int frequencyIssuesSinceLastRealBet;
        public int frequencyIssuesSinceLastFloorBet;
        public int frequencyConsecutiveFloorBets;
        public int frequencyFloorMinGap;
        public int frequencyFloorPacingReady;
        public String frequencyFloorPacingReason;
        public String decisionQualityPath;
        public int evidenceDomainFrozen;
        public String evidenceDomainFrozenIssue;
        public long evidenceDomainFrozenAt;
        public String dynamicMatchedLevel;
        public int dynamicRealSample;
        public int dynamicShadowSample;
        public String moduleBuildVersion;

        public String finalAction;
        public String blockCode;
        public String reasonText;
        public String summaryText;

        public V20DecisionContext() {
            baseIssue = "";
            targetIssue = "";
            createdAt = 0L;
            position = 0;
            n1 = 0;
            n2 = 0;
            n3 = 0;
            k3Mask = 0;
            windowState = null;
            role1 = null;
            role2 = null;
            role3 = null;
            cause1 = null;
            cause2 = null;
            cause3 = null;
            teacher1 = null;
            teacher2 = null;
            teacher3 = null;
            timingProfile = null;
            similarWindow = null;
            counterfactual = null;
            ruleHealth = null;
            shadowHealth = null;
            falseStrongRecord = null;
            fusion = new V20ProbabilityFusion();
            actionMatrix = new V20ActionMatrix();
            grade = CandidateGrade.UNKNOWN;
            finalCauseScore = 0.0d;
            finalP = 0.0d;
            dynamicGate = AppConfig.DEFAULT_DYNAMIC_GATE;
            edge = 0.0d;
            confidence = 0.0d;
            riskScore = 0.0d;
            finalAction = V20_ACTION_HOLD;
            blockCode = "";
            reasonText = "";
            summaryText = "";
        }

        public void setNumbers(int a, int b, int c) {
            n1 = a;
            n2 = b;
            n3 = c;
            k3Mask = buildK3Mask(a, b, c);
        }

        public void recalc() {
            if (windowState != null) windowState.recalc();
            if (role1 != null) role1.recalc();
            if (role2 != null) role2.recalc();
            if (role3 != null) role3.recalc();
            if (cause1 != null) cause1.recalc();
            if (cause2 != null) cause2.recalc();
            if (cause3 != null) cause3.recalc();
            if (teacher1 != null) teacher1.recalc();
            if (teacher2 != null) teacher2.recalc();
            if (teacher3 != null) teacher3.recalc();
            if (timingProfile != null) timingProfile.recalc();
            if (similarWindow != null) similarWindow.recalc();
            if (counterfactual != null) counterfactual.recalc();
            if (ruleHealth != null) ruleHealth.recalc();
            if (shadowHealth != null) shadowHealth.recalc();
            if (falseStrongRecord != null) falseStrongRecord.recalc(0L, System.currentTimeMillis());

            finalCauseScore = buildCauseScore();
            grade = buildGrade(finalCauseScore);
            riskScore = buildRiskScore();
            confidence = buildConfidence();

            if (fusion == null) fusion = new V20ProbabilityFusion();
            fusion.targetIssue = targetIssue;
            fusion.position = position;
            fusion.k3Mask = k3Mask;
            fusion.stateKey = windowState == null ? "" : windowState.stateKey;
            if (fusion.baseP <= 0.0d) fusion.baseP = AppConfig.RANDOM_K3_BASELINE;
            fusion.causeBoost =
                    AppConfig.limitAdjust((finalCauseScore - 0.50d) * 0.070d, V20_MAX_REASON_BOOST);
            fusion.teacherBoost = buildTeacherBoost();
            fusion.timingBoost = timingProfile == null ? 0.0d : timingProfile.timingBoost;
            fusion.similarBoost =
                    similarWindow == null
                            ? 0.0d
                            : AppConfig.limitAdjust(
                                    similarWindow.supportEdge * 0.18d, V20_MAX_SIMILAR_BOOST);
            fusion.counterBoost = counterfactual == null ? 0.0d : counterfactual.probBoost;
            fusion.shadowBoost = shadowHealth != null && shadowHealth.promotePass ? 0.006d : 0.0d;
            fusion.falseStrongPenalty =
                    falseStrongRecord == null ? 0.0d : falseStrongRecord.riskPenalty;
            fusion.ruleDecayPenalty = ruleHealth == null ? 0.0d : ruleHealth.probPenalty;
            fusion.earlyTimingPenalty = timingProfile == null ? 0.0d : timingProfile.timingPenalty;
            fusion.weakSamplePenalty =
                    confidence < V20_MIN_CAUSE_CONFIDENCE ? V20_MAX_WEAK_SAMPLE_PENALTY : 0.0d;
            fusion.teacherRiskPenalty = buildTeacherRiskPenalty();
            fusion.dynamicGate = dynamicGate;
            fusion.recalc();

            finalP = fusion.finalP;
            dynamicGate = fusion.dynamicGate;
            edge = fusion.edge;

            if (actionMatrix == null) actionMatrix = new V20ActionMatrix();
            fillActionMatrix();
            actionMatrix.recalc();

            finalAction = actionMatrix.finalAction;
            blockCode = actionMatrix.blockCode;
            reasonText = buildReasonText();
            summaryText = buildSummaryText();
            if (createdAt <= 0L) createdAt = System.currentTimeMillis();
        }

        private double buildCauseScore() {
            double support = 0.0d;
            double weight = 0.0d;

            if (windowState != null) {
                support += windowState.confidence * 0.16d;
                weight += 0.16d;
            }
            if (role1 != null) {
                support += role1.roleSupport * 0.10d;
                weight += 0.10d;
            }
            if (role2 != null) {
                support += role2.roleSupport * 0.10d;
                weight += 0.10d;
            }
            if (role3 != null) {
                support += role3.roleSupport * 0.10d;
                weight += 0.10d;
            }
            if (cause1 != null) {
                support += cause1.supportScore * 0.13d;
                weight += 0.13d;
            }
            if (cause2 != null) {
                support += cause2.supportScore * 0.13d;
                weight += 0.13d;
            }
            if (cause3 != null) {
                support += cause3.supportScore * 0.13d;
                weight += 0.13d;
            }
            if (similarWindow != null) {
                support += similarWindow.trustScore * 0.08d;
                weight += 0.08d;
            }
            if (counterfactual != null) {
                support += counterfactual.counterSupport * 0.07d;
                weight += 0.07d;
            }
            if (timingProfile != null) {
                support += timingProfile.timingConfidence * 0.05d;
                weight += 0.05d;
            }
            if (ruleHealth != null) {
                support += ruleHealth.healthScore * 0.05d;
                weight += 0.05d;
            }

            if (weight <= 0.0d) return 0.0d;
            return AppConfig.clampProbability(support / weight);
        }

        private CandidateGrade buildGrade(double score) {
            if (score >= V20_GRADE_S_MIN_SCORE) return CandidateGrade.S;
            if (score >= V20_GRADE_A_MIN_SCORE) return CandidateGrade.A;
            if (score >= V20_GRADE_B_MIN_SCORE) return CandidateGrade.B;
            if (score >= V20_GRADE_B_MIN_SCORE * 0.80d) return CandidateGrade.C;
            return CandidateGrade.UNKNOWN;
        }

        private double buildRiskScore() {
            double risk = 0.0d;
            int count = 0;

            if (windowState != null) {
                risk += windowState.riskScore;
                count++;
            }
            if (cause1 != null) {
                risk += cause1.riskScore;
                count++;
            }
            if (cause2 != null) {
                risk += cause2.riskScore;
                count++;
            }
            if (cause3 != null) {
                risk += cause3.riskScore;
                count++;
            }
            if (similarWindow != null) {
                risk += similarWindow.riskScore;
                count++;
            }
            if (counterfactual != null) {
                risk += counterfactual.counterRisk;
                count++;
            }
            if (ruleHealth != null) {
                risk += ruleHealth.decayRisk;
                count++;
            }
            if (falseStrongRecord != null) {
                risk += falseStrongRecord.riskPenalty;
                count++;
            }
            if (timingProfile != null) {
                risk += timingProfile.earlyRisk;
                count++;
            }

            return count > 0 ? AppConfig.clampProbability(risk / ((double) count)) : 0.0d;
        }

        private double buildConfidence() {
            double c = 0.0d;
            int count = 0;
            if (windowState != null) {
                c += windowState.confidence;
                count++;
            }
            if (cause1 != null) {
                c += cause1.confidence;
                count++;
            }
            if (cause2 != null) {
                c += cause2.confidence;
                count++;
            }
            if (cause3 != null) {
                c += cause3.confidence;
                count++;
            }
            if (teacher1 != null) {
                c += teacher1.confidence;
                count++;
            }
            if (teacher2 != null) {
                c += teacher2.confidence;
                count++;
            }
            if (teacher3 != null) {
                c += teacher3.confidence;
                count++;
            }
            if (similarWindow != null) {
                c += similarWindow.confidence;
                count++;
            }
            if (timingProfile != null) {
                c += timingProfile.timingConfidence;
                count++;
            }
            if (ruleHealth != null) {
                c += ruleHealth.confidence;
                count++;
            }
            return count > 0 ? AppConfig.clampProbability(c / ((double) count)) : 0.0d;
        }

        private double buildTeacherBoost() {
            double b = 0.0d;
            int count = 0;
            if (teacher1 != null && teacher1.canSoftFeedRealtime()) {
                b += teacher1.probAdjust;
                count++;
            }
            if (teacher2 != null && teacher2.canSoftFeedRealtime()) {
                b += teacher2.probAdjust;
                count++;
            }
            if (teacher3 != null && teacher3.canSoftFeedRealtime()) {
                b += teacher3.probAdjust;
                count++;
            }
            if (count <= 0) return 0.0d;
            return AppConfig.limitAdjust(b / ((double) count), V20_MAX_TEACHER_BOOST);
        }

        private double buildTeacherRiskPenalty() {
            double p = 0.0d;
            if (teacher1 != null) p += teacher1.riskPenalty;
            if (teacher2 != null) p += teacher2.riskPenalty;
            if (teacher3 != null) p += teacher3.riskPenalty;
            return AppConfig.clampProbability(p);
        }

        private String buildTeacherStatusForAction() {
            if (teacher1 == null && teacher2 == null && teacher3 == null) return V20_TEACHER_EMPTY;
            if (isTeacherBad(teacher1) || isTeacherBad(teacher2) || isTeacherBad(teacher3))
                return V20_TEACHER_RISK;
            if (isTeacherStrong(teacher1) || isTeacherStrong(teacher2) || isTeacherStrong(teacher3))
                return V20_TEACHER_STRONG;
            if (isTeacherReady(teacher1) || isTeacherReady(teacher2) || isTeacherReady(teacher3))
                return V20_TEACHER_READY;
            return V20_TEACHER_WEAK;
        }

        private boolean isTeacherBad(TeacherMatrixCell t) {
            if (t == null) return false;
            return V20_TEACHER_RISK.equals(t.teacherStatus)
                    || V20_TEACHER_DECAY.equals(t.teacherStatus);
        }

        private boolean isTeacherStrong(TeacherMatrixCell t) {
            return t != null && V20_TEACHER_STRONG.equals(t.teacherStatus);
        }

        private boolean isTeacherReady(TeacherMatrixCell t) {
            return t != null && V20_TEACHER_READY.equals(t.teacherStatus);
        }

        private void fillActionMatrix() {
            actionMatrix.targetIssue = targetIssue;
            actionMatrix.stateKey = windowState == null ? "" : windowState.stateKey;
            actionMatrix.relaxedStateKey = windowState == null ? "" : windowState.relaxedStateKey;
            actionMatrix.exactStateKey = windowState == null ? "" : windowState.exactStateKey;
            actionMatrix.sameClassKey = actionMatrix.relaxedStateKey;
            actionMatrix.ruleKey = ruleHealth == null ? "" : ruleHealth.ruleKey;
            actionMatrix.sourceKey = ruleHealth == null ? "" : ruleHealth.sourceKey;
            actionMatrix.position = position;
            actionMatrix.k3Mask = k3Mask;
            actionMatrix.teacherStatus = buildTeacherStatusForAction();
            actionMatrix.timingStatus =
                    timingProfile == null ? V20_TIMING_UNKNOWN : timingProfile.timingStatus;
            actionMatrix.similarityClass =
                    similarWindow == null ? V20_SIM_REJECT : similarWindow.similarityClass;
            actionMatrix.ruleStatus = ruleHealth == null ? "RULE_UNKNOWN" : ruleHealth.ruleStatus;
            actionMatrix.gradeText = Models.gradeText(grade);
            actionMatrix.finalP = finalP;
            actionMatrix.dynamicGate = dynamicGate;
            actionMatrix.causeScore = finalCauseScore;
            actionMatrix.causeConfidence = confidence;
            actionMatrix.teacherScore = averageTeacherScore();
            actionMatrix.timingScore =
                    timingProfile == null ? 0.0d : timingProfile.timingConfidence;
            actionMatrix.similarScore = similarWindow == null ? 0.0d : similarWindow.confidence;
            actionMatrix.counterScore =
                    counterfactual == null ? 0.0d : counterfactual.counterConfidence;
            actionMatrix.ruleHealthScore = ruleHealth == null ? 0.0d : ruleHealth.healthScore;
            actionMatrix.shadowHealthScore = shadowHealth == null ? 0.0d : shadowHealth.healthScore;
            actionMatrix.falseStrongRisk =
                    falseStrongRecord == null ? 0.0d : falseStrongRecord.recentFalseStrongRate;
            actionMatrix.ruleDecayRisk = ruleHealth == null ? 0.0d : ruleHealth.decayRisk;
            actionMatrix.earlyTimingRisk = timingProfile == null ? 0.0d : timingProfile.earlyRisk;
            actionMatrix.weakSampleRisk = confidence < V20_MIN_CAUSE_CONFIDENCE ? 0.800d : 0.0d;
            actionMatrix.falseStrongFrozen =
                    falseStrongRecord != null && falseStrongRecord.frozen ? 1 : 0;
            actionMatrix.sameClassFrozen = 0;
            actionMatrix.backtestTrustPass = 1;
            actionMatrix.k3Legal =
                    AppConfig.isPlayRuleLegal(position, AppConfig.FIXED_K, n1, n2, n3) ? 1 : 0;
            actionMatrix.targetIssueOk = targetIssue != null && targetIssue.length() > 0 ? 1 : 0;
            actionMatrix.noOpenRealBet = 1;
        }

        private double averageTeacherScore() {
            double s = 0.0d;
            int c = 0;
            if (teacher1 != null) {
                s += teacher1.teacherScore;
                c++;
            }
            if (teacher2 != null) {
                s += teacher2.teacherScore;
                c++;
            }
            if (teacher3 != null) {
                s += teacher3.teacherScore;
                c++;
            }
            return c > 0 ? AppConfig.clampProbability(s / ((double) c)) : 0.0d;
        }

        public CandidateK3 toCandidateK3() {
            recalc();
            CandidateK3 c = new CandidateK3();
            c.baseIssue = baseIssue;
            c.targetIssue = targetIssue;
            c.createdAt = createdAt;
            c.position = position;
            c.setNumbers(n1, n2, n3);
            c.finalP = finalP;
            c.dynamicGate = dynamicGate;
            c.edge = edge;
            c.confidence = confidence;
            c.riskScore = riskScore;
            c.grade = grade;
            c.decisionAction = actionMatrix.toDecisionValue();
            c.finalCauseScore = finalCauseScore;
            c.positionEdge =
                    windowState == null ? 0.0d : (windowState.confidence - V20_CAUSE_BASELINE);
            c.numberTeacherEdge = averageTeacherScore() - V20_CAUSE_BASELINE;
            c.comboRoleEdge = finalCauseScore - V20_CAUSE_BASELINE;
            c.thirdGuardEdge = 0.0d;
            c.rankCounterEdge =
                    counterfactual == null
                            ? 0.0d
                            : (counterfactual.counterSupport - V20_CAUSE_BASELINE);
            c.sourceHealthEdge =
                    ruleHealth == null ? 0.0d : (ruleHealth.healthScore - V20_CAUSE_BASELINE);
            c.stateHealthEdge =
                    windowState == null ? 0.0d : (windowState.confidence - V20_CAUSE_BASELINE);
            c.cycleHealthEdge =
                    timingProfile == null
                            ? 0.0d
                            : (timingProfile.timingConfidence - V20_CAUSE_BASELINE);
            c.seedSimilarity = similarWindow == null ? 0.0d : similarWindow.similarityScore;
            c.shadowHealth = shadowHealth == null ? 0.0d : shadowHealth.healthScore;
            c.falseStrongRisk =
                    falseStrongRecord == null ? 0.0d : falseStrongRecord.recentFalseStrongRate;
            c.ruleDecayRisk = ruleHealth == null ? 0.0d : ruleHealth.decayRisk;
            c.sameClassLoseRisk = 0.0d;
            c.guardDragRisk = 0.0d;
            c.positionWrongRisk = 0.0d;
            c.positionHealthy = windowState != null && windowState.isUsable();
            c.numberTeacherPass = actionMatrix.teacherPass == 1;
            c.rankPass = counterfactual == null || counterfactual.counterRisk < 0.650d;
            c.thirdGuardPass = true;
            c.shadowPass =
                    shadowHealth == null || shadowHealth.promotePass || shadowHealth.sample60 <= 0;
            c.falseStrongFrozen = falseStrongRecord != null && falseStrongRecord.frozen;
            c.sameClassFrozen = false;
            c.sourceHealthy = ruleHealth == null || ruleHealth.ruleHealthy;
            c.ruleHealthy = ruleHealth == null || ruleHealth.ruleHealthy;
            c.stateHealthy =
                    windowState != null && windowState.confidence >= V20_MIN_CAUSE_CONFIDENCE;
            c.riskOk = riskScore < 0.650d;
            c.stateKey = windowState == null ? "" : windowState.stateKey;
            c.sourceKey = ruleHealth == null ? "" : ruleHealth.sourceKey;
            c.ruleKey = ruleHealth == null ? "" : ruleHealth.ruleKey;
            c.sameClassKey = actionMatrix.sameClassKey;
            c.falseStrongKey = falseStrongRecord == null ? "" : falseStrongRecord.falseStrongKey;
            c.reasonAtoms = reasonText;
            c.sourceVotes = actionMatrix.supportText;
            c.riskTags = actionMatrix.riskText;
            c.decisionReason = summaryText;
            c.normalize();
            return c;
        }

        public Decision toDecision() {
            CandidateK3 c = toCandidateK3();
            return c.toDecision(actionMatrix.toDecisionValue());
        }

        public String buildReasonText() {
            StringBuilder sb = new StringBuilder();
            sb.append("V20因链闭环：");
            sb.append("窗口=").append(windowState == null ? "无" : windowState.summaryText);
            sb.append("；因链等级=").append(gradeText(grade));
            sb.append("，因链分=").append(UiFormatter.probability(finalCauseScore));
            sb.append("，finalP=").append(UiFormatter.probability(finalP));
            sb.append("，gate=").append(UiFormatter.probability(dynamicGate));
            sb.append("，动作=").append(finalAction);
            if (actionMatrix != null) {
                sb.append("；支持=").append(actionMatrix.supportText);
                sb.append("；风险=").append(actionMatrix.riskText);
            }
            if (fusion != null && fusion.summaryText != null && fusion.summaryText.length() > 0) {
                sb.append("；").append(fusion.summaryText);
            }
            return sb.toString();
        }

        public String buildSummaryText() {
            return "V20决策上下文 P"
                    + position
                    + " "
                    + maskToText(k3Mask)
                    + "：grade="
                    + gradeText(grade)
                    + "，cause="
                    + UiFormatter.probability(finalCauseScore)
                    + "，finalP="
                    + UiFormatter.probability(finalP)
                    + "，gate="
                    + UiFormatter.probability(dynamicGate)
                    + "，risk="
                    + UiFormatter.probability(riskScore)
                    + "，action="
                    + finalAction
                    + "，block="
                    + blockCode;
        }
    }

    public static final class V20ClosedLoopSnapshot {
        public String snapshotKey;
        public String baseIssue;
        public String targetIssue;
        public long createdAt;

        public int position;
        public int k3Mask;
        public String stateKey;
        public String exactStateKey;
        public String relaxedStateKey;

        public int decisionCount;
        public int betCount;
        public int holdCount;
        public int shadowCount;
        public int freezeCount;
        public int hitCount;
        public int missCount;

        public int causeSampleCount;
        public int teacherSampleCount;
        public int reverseSampleCount;
        public int similarSampleCount;
        public int timingSampleCount;
        public int counterSampleCount;
        public int ruleHealthCount;

        public double realHitRate;
        public double shadowHitRate;
        public double causeAvgConfidence;
        public double teacherAvgConfidence;
        public double ruleAvgHealth;
        public double falseStrongAvgRisk;
        public double timingNowRate;
        public double earlyRiskRate;
        public double counterMisorderRate;
        public double loopHealthScore;

        public String topHitReason;
        public String topMissReason;
        public String topRisk;
        public String nextAction;
        public String summaryText;

        public V20ClosedLoopSnapshot() {
            snapshotKey = "";
            baseIssue = "";
            targetIssue = "";
            createdAt = 0L;
            position = 0;
            k3Mask = 0;
            stateKey = "";
            exactStateKey = "";
            relaxedStateKey = "";
            decisionCount = 0;
            betCount = 0;
            holdCount = 0;
            shadowCount = 0;
            freezeCount = 0;
            hitCount = 0;
            missCount = 0;
            causeSampleCount = 0;
            teacherSampleCount = 0;
            reverseSampleCount = 0;
            similarSampleCount = 0;
            timingSampleCount = 0;
            counterSampleCount = 0;
            ruleHealthCount = 0;
            realHitRate = 0.0d;
            shadowHitRate = 0.0d;
            causeAvgConfidence = 0.0d;
            teacherAvgConfidence = 0.0d;
            ruleAvgHealth = 0.0d;
            falseStrongAvgRisk = 0.0d;
            timingNowRate = 0.0d;
            earlyRiskRate = 0.0d;
            counterMisorderRate = 0.0d;
            loopHealthScore = 0.0d;
            topHitReason = "";
            topMissReason = "";
            topRisk = "";
            nextAction = V20_ACTION_HOLD;
            summaryText = "";
        }

        public void rebuildKey() {
            snapshotKey =
                    "CLS_"
                            + safeIssue(targetIssue)
                            + "_P"
                            + position
                            + "_K"
                            + k3Mask
                            + "_S"
                            + Math.abs(safeText(stateKey).hashCode());
        }

        public void recalc() {
            if (decisionCount <= 0) {
                decisionCount = betCount + holdCount + shadowCount + freezeCount;
            }
            if (missCount <= 0 && betCount >= hitCount) {
                missCount = betCount - hitCount;
            }
            if (missCount < 0) missCount = 0;

            realHitRate = safeRate(hitCount, betCount);

            double sampleScore =
                    AppConfig.clampProbability(
                            calcSampleConfidenceV20(causeSampleCount, 120) * 0.20d
                                    + calcSampleConfidenceV20(teacherSampleCount, 120) * 0.20d
                                    + calcSampleConfidenceV20(reverseSampleCount, 120) * 0.18d
                                    + calcSampleConfidenceV20(similarSampleCount, 80) * 0.14d
                                    + calcSampleConfidenceV20(timingSampleCount, 80) * 0.14d
                                    + calcSampleConfidenceV20(ruleHealthCount, 60) * 0.14d);

            loopHealthScore =
                    AppConfig.clampProbability(
                            realHitRate * 0.22d
                                    + shadowHitRate * 0.12d
                                    + causeAvgConfidence * 0.18d
                                    + teacherAvgConfidence * 0.14d
                                    + ruleAvgHealth * 0.14d
                                    + timingNowRate * 0.08d
                                    + sampleScore * 0.12d
                                    - falseStrongAvgRisk * 0.18d
                                    - earlyRiskRate * 0.12d
                                    - counterMisorderRate * 0.08d);

            if (loopHealthScore >= 0.560d
                    && falseStrongAvgRisk < 0.450d
                    && earlyRiskRate < 0.350d) {
                nextAction = V20_ACTION_BET_NOW;
            } else if (falseStrongAvgRisk >= 0.620d || ruleAvgHealth < 0.250d) {
                nextAction = V20_ACTION_FREEZE;
            } else if (earlyRiskRate >= 0.350d || counterMisorderRate >= 0.300d) {
                nextAction = V20_ACTION_WAIT_CONFIRM;
            } else if (sampleScore < V20_MIN_CAUSE_CONFIDENCE) {
                nextAction = V20_ACTION_SHADOW;
            } else {
                nextAction = V20_ACTION_HOLD;
            }

            if (createdAt <= 0L) createdAt = System.currentTimeMillis();
            rebuildKey();
            summaryText = buildSummaryText();
        }

        public String buildSummaryText() {
            return "闭环快照P"
                    + position
                    + " "
                    + maskToText(k3Mask)
                    + "：BET="
                    + betCount
                    + "，胜率="
                    + UiFormatter.probability(realHitRate)
                    + "，因果样本="
                    + causeSampleCount
                    + "，教师样本="
                    + teacherSampleCount
                    + "，反推样本="
                    + reverseSampleCount
                    + "，规则健康="
                    + UiFormatter.probability(ruleAvgHealth)
                    + "，假强="
                    + UiFormatter.probability(falseStrongAvgRisk)
                    + "，闭环健康="
                    + UiFormatter.probability(loopHealthScore)
                    + "，建议="
                    + nextAction;
        }
    }

    public static void applyV20FusionToCandidate(ComboCandidate c, V20ProbabilityFusion f) {
        if (c == null || f == null) return;
        f.recalc();
        c.finalP = f.finalP;
        c.rawP = f.rawP;
        c.calibratedP = f.finalP;
        c.dynamicGate = f.dynamicGate;
        c.edge = f.edge;
        c.reasonScore =
                AppConfig.clampProbability(
                        0.50d + f.causeBoost - f.falseStrongPenalty - f.ruleDecayPenalty);
        c.teacherMatrixScore =
                AppConfig.clampProbability(0.50d + f.teacherBoost - f.teacherRiskPenalty);
        c.probabilityMode = f.probabilityMode;
        c.reasonSummary = appendReason(c.reasonSummary, f.summaryText);
    }

    public static void applyV20ContextToComboCandidate(ComboCandidate c, V20DecisionContext ctx) {
        if (c == null || ctx == null) return;
        ctx.recalc();
        c.baseIssue = ctx.baseIssue;
        c.targetIssue = ctx.targetIssue;
        c.position = ctx.position;
        c.setNumbers(ctx.n1, ctx.n2, ctx.n3);
        c.finalP = ctx.finalP;
        c.dynamicGate = ctx.dynamicGate;
        c.edge = ctx.edge;
        c.reasonScore = ctx.finalCauseScore;
        c.causalConfidence = ctx.confidence;
        c.evidenceRisk = ctx.riskScore;
        c.stateKey = ctx.windowState == null ? c.stateKey : ctx.windowState.stateKey;
        c.teacherMatrixScore = ctx.averageTeacherScore();
        c.teacherMatrixConfidence = ctx.confidence;
        c.teacherMatrixStatus =
                ctx.actionMatrix == null
                        ? AppConfig.TEACHER_MATRIX_STATUS_NONE
                        : ctx.actionMatrix.teacherStatus;
        c.elite45Passed = isGradeStrong(ctx.grade) ? 1 : 0;
        c.elite45Status = gradeText(ctx.grade);
        c.elite45Summary = ctx.summaryText;
        c.reasonSummary = appendReason(c.reasonSummary, ctx.reasonText);
        c.blockCode = ctx.blockCode == null ? "" : ctx.blockCode;
        c.finalRankScore = ctx.finalCauseScore;
        c.rankScore = ctx.finalCauseScore;
    }

    public static void applyV20ContextToDecision(Decision d, V20DecisionContext ctx) {
        if (d == null || ctx == null) return;
        ctx.recalc();

        d.baseIssue = ctx.baseIssue;
        d.targetIssue = ctx.targetIssue;
        d.createdAt = ctx.createdAt > 0L ? ctx.createdAt : System.currentTimeMillis();
        d.position = ctx.position;
        d.setNumbers(ctx.n1, ctx.n2, ctx.n3);
        d.decision =
                ctx.actionMatrix == null
                        ? AppConfig.DECISION_HOLD
                        : ctx.actionMatrix.toDecisionValue();
        d.action = d.decision;
        d.statScope =
                AppConfig.DECISION_BET.equals(d.decision)
                        ? AppConfig.STAT_SCOPE_REAL_TRADE
                        : (AppConfig.DECISION_SHADOW.equals(d.decision)
                                ? AppConfig.STAT_SCOPE_SHADOW
                                : AppConfig.STAT_SCOPE_HOLD);
        d.sourceType = SOURCE_TYPE_REALTIME;
        d.isRealtimeReal = AppConfig.DECISION_BET.equals(d.decision) ? 1 : 0;

        d.rawP = ctx.fusion == null ? ctx.finalP : ctx.fusion.rawP;
        d.calibratedP = ctx.finalP;
        d.finalP = ctx.finalP;
        d.dynamicGate = ctx.dynamicGate;
        d.edge = ctx.edge;
        d.reasonScore = ctx.finalCauseScore;
        d.causalConfidence = ctx.confidence;
        d.evidenceRisk = ctx.riskScore;
        d.stateKey = ctx.windowState == null ? "" : ctx.windowState.stateKey;
        d.reasonSummary = ctx.summaryText;
        d.reasonText = ctx.reasonText;
        d.blockCode = ctx.blockCode;

        d.teacherMatrixScore = ctx.averageTeacherScore();
        d.teacherMatrixConfidence = ctx.confidence;
        d.teacherMatrixStatus =
                ctx.actionMatrix == null ? V20_TEACHER_EMPTY : ctx.actionMatrix.teacherStatus;
        d.teacherMatrixSummary = ctx.actionMatrix == null ? "" : ctx.actionMatrix.supportText;

        d.preDrawReasonText = ctx.reasonText;
        d.reasonValidLevel =
                isGradeStrong(ctx.grade)
                        ? REASON_LEVEL_A_VALID
                        : (isGradeRealBetCapable(ctx.grade)
                                ? REASON_LEVEL_B_USEFUL
                                : REASON_LEVEL_C_EXPLAIN_ONLY);
        d.reasonEdge = ctx.finalCauseScore - V20_CAUSE_BASELINE;
        d.validReasonCount = ctx.finalCauseScore >= V20_GRADE_B_MIN_SCORE ? 1 : 0;
        d.falseReasonCount = ctx.riskScore >= 0.650d ? 1 : 0;
        d.elite45Passed = isGradeStrong(ctx.grade) ? 1 : 0;
        d.elite45Status = gradeText(ctx.grade);
        d.elite45Summary = ctx.summaryText;
        d.dataOk = 1;
        d.syncAction();
    }

    public static DecisionAudit buildV20DecisionAuditFromCandidate(CandidateK3 c) {
        DecisionAudit a = new DecisionAudit();
        if (c == null) return a;
        c.normalize();
        a.fromCandidate(c);
        a.blockCode = c.canEnterDecisionGate() ? "" : "V20_CANDIDATE_NOT_READY";
        return a;
    }

    public static ReverseReasonSample buildV20ReverseFromDecisionAndTrade(Decision d, Trade t) {
        ReverseReasonSample r = new ReverseReasonSample();
        if (d != null) {
            r.fromDecision(d);
        }
        if (t != null) {
            r.actualIssue = t.targetIssue == null ? "" : t.targetIssue;
            r.actualNumber = t.actualNumber;
            r.hit = t.hit == 1;
            r.hitInt = t.hit;
            r.missReasonType = normalizeMissReasonTypeV20(t.missReason);
            r.settledAt = t.settledAt;
            r.finalP = t.finalP;
            r.dynamicGate = t.dynamicGate;
            r.edge = t.edge;
            r.falseStrongRisk = t.falseStrongRisk;
            r.supportReason = t.reasonSummary == null ? "" : t.reasonSummary;
            r.riskReason = t.settlementReason == null ? "" : t.settlementReason;
        }
        r.recalc();
        return r;
    }

    public static ReverseReasonSample buildV20ReverseFromSettlement(
            Decision d, SettlementResult s) {
        ReverseReasonSample r = new ReverseReasonSample();
        if (d != null) {
            r.fromDecision(d);
        }
        if (s != null) {
            r.applySettlementResult(s);
        }
        r.recalc();
        return r;
    }

    public static boolean isV20HardBlocked(V20ActionMatrix a) {
        if (a == null) return false;
        a.recalc();
        return V20_ACTION_FREEZE.equals(a.finalAction)
                || V20_ACTION_REJECT.equals(a.finalAction)
                || "K3_ILLEGAL".equals(a.blockCode)
                || "TARGET_ISSUE_NOT_OK".equals(a.blockCode)
                || "OPEN_REAL_BET_EXISTS".equals(a.blockCode);
    }

    public static boolean canV20CreateRealBet(V20ActionMatrix a) {
        if (a == null) return false;
        a.recalc();
        return V20_ACTION_BET_NOW.equals(a.finalAction)
                && a.finalPOverGate == 1
                && a.causePass == 1
                && a.teacherPass == 1
                && a.timingPass == 1
                && a.rulePass == 1
                && a.k3Legal == 1
                && a.targetIssueOk == 1
                && a.noOpenRealBet == 1
                && a.falseStrongFrozen == 0
                && a.sameClassFrozen == 0;
    }

    public static String buildV20DecisionDebugText(V20DecisionContext ctx) {
        if (ctx == null) return "";
        ctx.recalc();
        StringBuilder sb = new StringBuilder();
        sb.append("【V20动态因链】\n");
        sb.append(ctx.summaryText);
        sb.append("\n");
        if (ctx.windowState != null) {
            sb.append("窗口：").append(ctx.windowState.summaryText).append("\n");
        }
        if (ctx.actionMatrix != null) {
            sb.append("动作：").append(ctx.actionMatrix.summaryText).append("\n");
            sb.append("支持：").append(ctx.actionMatrix.supportText).append("\n");
            sb.append("风险：").append(ctx.actionMatrix.riskText).append("\n");
        }
        if (ctx.fusion != null) {
            sb.append("概率：").append(ctx.fusion.summaryText).append("\n");
        }
        if (ctx.timingProfile != null) {
            sb.append("时机：").append(ctx.timingProfile.summaryText).append("\n");
        }
        if (ctx.similarWindow != null) {
            sb.append("相似：").append(ctx.similarWindow.summaryText).append("\n");
        }
        if (ctx.counterfactual != null) {
            sb.append("反事实：").append(ctx.counterfactual.summaryText).append("\n");
        }
        if (ctx.ruleHealth != null) {
            sb.append("规则：").append(ctx.ruleHealth.summaryText).append("\n");
        }
        if (ctx.shadowHealth != null) {
            sb.append("SHADOW：").append(ctx.shadowHealth.summaryText).append("\n");
        }
        if (ctx.falseStrongRecord != null) {
            sb.append("假强：").append(ctx.falseStrongRecord.summaryText).append("\n");
        }
        return sb.toString();
    }

    public static String buildV20ReverseDebugText(ReverseReasonSample r) {
        if (r == null) return "";
        r.recalc();
        StringBuilder sb = new StringBuilder();
        sb.append("【V20结算反推】\n");
        sb.append(r.summaryText).append("\n");
        sb.append("反推：").append(r.reverseSummary).append("\n");
        sb.append("学习建议：").append(r.learningAdvice).append("\n");
        if (r.supportReason != null && r.supportReason.length() > 0) {
            sb.append("原支持：").append(r.supportReason).append("\n");
        }
        if (r.riskReason != null && r.riskReason.length() > 0) {
            sb.append("原风险：").append(r.riskReason).append("\n");
        }
        if (r.counterfactualSummary != null && r.counterfactualSummary.length() > 0) {
            sb.append("反事实：").append(r.counterfactualSummary).append("\n");
        }
        return sb.toString();
    }

    public static String buildV20ClosedLoopText(V20ClosedLoopSnapshot s) {
        if (s == null) return "";
        s.recalc();
        return s.summaryText;
    }

    /*
     * 链路修改升级V2公共上下文。
     * Models只承接实体；所有概率、Gate和BET裁决仍由对应引擎负责。
     */
    public static final class FrequencyState {
        public String stateKey;
        public String asOfIssue;
        public String status;
        public String version;
        public double frequency20;
        public double frequency50;
        public double frequency100;
        public double frequency300;
        public int observed20, observed50, observed100, observed300;
        public int betCount20, betCount50, betCount100, betCount300;
        public int floorDebt20, floorDebt50, floorDebt100, floorDebt300;
        public int targetDebt20, targetDebt50, targetDebt100, targetDebt300;
        public int qualifiedFloorCandidateCount;
        public String urgentWindow;
        public int issuesSinceLastRealBet;
        public int issuesSinceLastQualifiedFloorBet;
        public int consecutiveQualifiedFloorBets;
        public int floorPacingMinGap;
        public int floorPacingReady;
        public String floorPacingReason;
        public double frequencyDebt;
        public int validIssueCount;
        public int realBetCount;
        public int qualifiedOpportunityCount;
        public int recentProbeCount;
        public int recentLoseCount;
        public int debtFrozen;
        public int requiredFloorBetCount;
        public int targetBetCount;
        public int floorDebtCount;
        public int targetDebtCount;
        public int noUpperCap;
        public long updatedAt;

        public FrequencyState() {
            stateKey = "MAIN";
            asOfIssue = "";
            status = AppConfig.FREQ_STATE_NORMAL;
            version = AppConfig.LINK_MODIFICATION_V2_VERSION;
            frequency20 = 0.0d;
            frequency50 = 0.0d;
            frequency100 = 0.0d;
            frequency300 = 0.0d;
            observed20 = observed50 = observed100 = observed300 = 0;
            betCount20 = betCount50 = betCount100 = betCount300 = 0;
            floorDebt20 = floorDebt50 = floorDebt100 = floorDebt300 = 0;
            targetDebt20 = targetDebt50 = targetDebt100 = targetDebt300 = 0;
            qualifiedFloorCandidateCount = 0;
            urgentWindow = "";
            issuesSinceLastRealBet = 999;
            issuesSinceLastQualifiedFloorBet = 999;
            consecutiveQualifiedFloorBets = 0;
            floorPacingMinGap = Integer.MAX_VALUE;
            floorPacingReady = 0;
            floorPacingReason = "NO_DEBT";
            frequencyDebt = 0.0d;
            validIssueCount = 0;
            realBetCount = 0;
            qualifiedOpportunityCount = 0;
            recentProbeCount = 0;
            recentLoseCount = 0;
            debtFrozen = 0;
            requiredFloorBetCount = 0;
            targetBetCount = 0;
            floorDebtCount = 0;
            targetDebtCount = 0;
            noUpperCap = 1;
            updatedAt = 0L;
        }

        public boolean isLowFrequencyPressure() {
            return AppConfig.FREQ_STATE_LOW_PRESSURE.equals(status)
                    && frequency100 < AppConfig.FREQ_DEBT_TRIGGER
                    && debtFrozen == 0;
        }
    }

    public static final class LayerSnapshot {
        public String snapshotKey;
        public String layer;
        public String asOfIssue;
        public String maxIncludedIssue;
        public String schemaVersion;
        public String strategyVersion;
        public String freshnessStatus;
        public String payload;
        public int sampleCount;
        public long createdAt;
        public long expiresAt;

        public LayerSnapshot() {
            snapshotKey = "";
            layer = "";
            asOfIssue = "";
            maxIncludedIssue = "";
            schemaVersion = String.valueOf(AppConfig.LINK_MODIFICATION_V2_SCHEMA_VERSION);
            strategyVersion = AppConfig.STRATEGY_VERSION;
            freshnessStatus = "NO_SAMPLE";
            payload = "";
            sampleCount = 0;
            createdAt = 0L;
            expiresAt = 0L;
        }

        public boolean isSafeForTarget(String targetIssue) {
            String target = AppConfig.safeIssue(targetIssue);
            String maxIssue = AppConfig.safeIssue(maxIncludedIssue);
            if (target.length() == 0 || maxIssue.length() == 0) return false;
            return maxIssue.compareTo(target) < 0;
        }
    }

    public static final class BackgroundCursor {
        public String taskKey;
        public String cursorIssue;
        public String targetIssue;
        public String status;
        public String version;
        public int processedCount;
        public int pendingCount;
        public long updatedAt;

        public BackgroundCursor() {
            taskKey = "";
            cursorIssue = "";
            targetIssue = "";
            status = "IDLE";
            version = AppConfig.LINK_MODIFICATION_V2_VERSION;
            processedCount = 0;
            pendingCount = 0;
            updatedAt = 0L;
        }
    }

    public static final class ShadowDomainStat {
        public String domainKey;
        public String parentDomainKey;
        public String promotionStatus;
        public String version;
        public int sampleCount;
        public int hitCount;
        public int missCount;
        public int recentSampleCount;
        public int recentHitCount;
        public int misleadingStreak;
        public double hitRate;
        public double recentHitRate;
        public double posteriorMean;
        public double wilsonLowerBound;
        public double driftScore;
        public String lastSettledIssue;
        public long updatedAt;

        public ShadowDomainStat() {
            domainKey = "";
            parentDomainKey = "";
            promotionStatus = AppConfig.PROMOTION_NO_SAMPLE;
            version = AppConfig.LINK_MODIFICATION_V2_VERSION;
            sampleCount = 0;
            hitCount = 0;
            missCount = 0;
            recentSampleCount = 0;
            recentHitCount = 0;
            misleadingStreak = 0;
            hitRate = 0.0d;
            recentHitRate = 0.0d;
            posteriorMean = 0.0d;
            wilsonLowerBound = 0.0d;
            driftScore = 0.0d;
            lastSettledIssue = "";
            updatedAt = 0L;
        }
    }

    public static final class SettlementEvent {
        public String eventKey;
        public String targetIssue;
        public String decision;
        public String statScope;
        public String betDomain;
        public String shadowPromotionDomain;
        public int position;
        public int n1;
        public int n2;
        public int n3;
        public int actualNumber;
        public int hit;
        public int realTrade;
        public int settled;
        public double finalP;
        public double riskAdjustedGate;
        public double rawEdge;
        public double coverageQuality;
        public double thirdQuality;
        public double needSupplyScore;
        public double counterRisk;
        public long settledAt;
        public String version;

        public SettlementEvent() {
            eventKey = "";
            targetIssue = "";
            decision = "";
            statScope = "";
            betDomain = "";
            shadowPromotionDomain = "";
            position = 0;
            n1 = 0;
            n2 = 0;
            n3 = 0;
            actualNumber = 0;
            hit = 0;
            realTrade = 0;
            settled = 0;
            finalP = 0.0d;
            riskAdjustedGate = 0.0d;
            rawEdge = 0.0d;
            coverageQuality = 0.0d;
            thirdQuality = 0.0d;
            needSupplyScore = 0.0d;
            counterRisk = 0.0d;
            settledAt = 0L;
            version = AppConfig.LINK_MODIFICATION_V2_VERSION;
        }
    }

    public static final class AnalysisContext {
        public String baseIssue;
        public String targetIssue;
        public String latestKnownIssue;
        public String strategyVersion;
        public String snapshotFreshness;
        public List<Draw> hotDraws;
        public LayerSnapshot warmSnapshot;
        public LayerSnapshot coldSnapshot;
        public LayerSnapshot deepSnapshot;
        public FrequencyState frequencyState;
        public long realtimeStartTime;
        public long realtimeDeadlineAt;

        public AnalysisContext() {
            baseIssue = "";
            targetIssue = "";
            latestKnownIssue = "";
            strategyVersion = AppConfig.STRATEGY_VERSION;
            snapshotFreshness = "NO_SAMPLE";
            hotDraws = new ArrayList<Draw>();
            warmSnapshot = null;
            coldSnapshot = null;
            deepSnapshot = null;
            frequencyState = new FrequencyState();
            realtimeStartTime = 0L;
            realtimeDeadlineAt = 0L;
        }

        public boolean isTargetConsistent() {
            return targetIssue != null
                    && targetIssue.length() > 0
                    && latestKnownIssue != null
                    && latestKnownIssue.length() > 0
                    && targetIssue.compareTo(latestKnownIssue) > 0;
        }
    }

    /** 前台服务/Alarm/Job恢复链只读状态。 */
    public static final class ServiceHealthState {
        public int userEnabled;
        public long heartbeatAt;
        public long heartbeatAgeMs;
        public String alarmStatus;
        public String jobStatus;
        public String lastReason;
        public int restartCount;
        public long updatedAt;
        /* 73487246：端到端业务活性。 */
        public long lastFetchSuccessAt;
        public long lastAnalysisSuccessAt;
        public long lastSettlementSuccessAt;
        public long lastAlarmTriggeredAt;
        public long lastJobTriggeredAt;
        public String backgroundHealthState;
        public String fetchStageState;
        public String analysisStageState;
        public String settlementStageState;
        public int pendingSettlementCount;
        public String businessStaleReason;

        public ServiceHealthState() {
            userEnabled = 0;
            heartbeatAt = 0L;
            heartbeatAgeMs = Long.MAX_VALUE;
            alarmStatus = "UNKNOWN";
            jobStatus = "UNKNOWN";
            lastReason = "";
            restartCount = 0;
            lastFetchSuccessAt = 0L;
            lastAnalysisSuccessAt = 0L;
            lastSettlementSuccessAt = 0L;
            lastAlarmTriggeredAt = 0L;
            lastJobTriggeredAt = 0L;
            backgroundHealthState = "UNKNOWN";
            fetchStageState = "UNKNOWN";
            analysisStageState = "UNKNOWN";
            settlementStageState = "UNKNOWN";
            pendingSettlementCount = 0;
            businessStaleReason = "";
            updatedAt = 0L;
        }
    }

    /** Teacher真实微批次状态；BUILDING半成品不得进入实时概率。 */
    public static final class TeacherSliceState {
        public String layer;
        public String status;
        public String cursorIssue;
        public int processed;
        public int required;
        public int pending;
        public long costMs;
        public long updatedAt;
        public String summary;

        public TeacherSliceState() {
            layer = "";
            status = "IDLE";
            cursorIssue = "";
            processed = 0;
            required = 0;
            pending = 0;
            costMs = 0L;
            updatedAt = 0L;
            summary = "";
        }
    }

    /** Probability每层变化审计，不参与BET裁决。 */
    public static final class ProbabilityAudit {
        public String targetIssue;
        public String comboKey;
        public String pipelineVersion;
        public double priorP;
        public double rawP;
        public double afterTeacherP;
        public double afterRiskP;
        public double finalP;
        public long costMs;
        public String summary;

        public ProbabilityAudit() {
            targetIssue = "";
            comboKey = "";
            pipelineVersion = AppConfig.PROBABILITY_PIPELINE_VERSION;
            priorP = 0.0d;
            rawP = 0.0d;
            afterTeacherP = 0.0d;
            afterRiskP = 0.0d;
            finalP = 0.0d;
            costMs = 0L;
            summary = "";
        }
    }

    public static final class DatabaseQueueState {
        public int waitingWriters;
        public int backgroundQueue;
        public long lastWriteCostMs;
        public long longestWriteCostMs;
        public String lastOperation;
        public String lastError;
        public long updatedAt;

        public DatabaseQueueState() {
            waitingWriters = 0;
            backgroundQueue = 0;
            lastWriteCostMs = 0L;
            longestWriteCostMs = 0L;
            lastOperation = "";
            lastError = "";
            updatedAt = 0L;
        }
    }

    public static final class ServiceRuntimeState {
        public int userEnabled;
        public long heartbeatAt;
        public long heartbeatAgeMs;
        public long startAt;
        public long taskRemovedAt;
        public long destroyAt;
        public int restartCount;
        public String alarmStatus;
        public String jobStatus;
        public String lastReason;
        public String lastReceiverAction;
        public long updatedAt;
        /* 73487246：端到端业务活性。 */
        public long lastFetchSuccessAt;
        public long lastAnalysisSuccessAt;
        public long lastSettlementSuccessAt;
        public long lastAlarmTriggeredAt;
        public long lastJobTriggeredAt;
        public String backgroundHealthState;
        public String fetchStageState;
        public String analysisStageState;
        public String settlementStageState;
        public int pendingSettlementCount;
        public String businessStaleReason;
        public long lastTeacherPumpAt;
        public long lastLearningPumpAt;
        public long lastLoopRepairAt;
        public String teacherPumpState;
        public String learningPumpState;
        public String loopRepairReason;
        public long serviceGeneration;
        public long processStartId;
        public long analysisProgressAt;
        public int analysisProgressPercent;
        public String analysisProgressStage;
        public long recoveryLeaseUntil;
        public int restartRequestCount;
        public int actualProcessStartCount;

        public ServiceRuntimeState() {
            userEnabled = 0;
            heartbeatAt = 0L;
            heartbeatAgeMs = Long.MAX_VALUE;
            startAt = 0L;
            taskRemovedAt = 0L;
            destroyAt = 0L;
            restartCount = 0;
            alarmStatus = "UNKNOWN";
            jobStatus = "UNKNOWN";
            lastReason = "";
            lastReceiverAction = "";
            lastFetchSuccessAt = 0L;
            lastAnalysisSuccessAt = 0L;
            lastSettlementSuccessAt = 0L;
            lastAlarmTriggeredAt = 0L;
            lastJobTriggeredAt = 0L;
            backgroundHealthState = "UNKNOWN";
            fetchStageState = "UNKNOWN";
            analysisStageState = "UNKNOWN";
            settlementStageState = "UNKNOWN";
            pendingSettlementCount = 0;
            businessStaleReason = "";
            lastTeacherPumpAt = 0L;
            lastLearningPumpAt = 0L;
            lastLoopRepairAt = 0L;
            teacherPumpState = "UNKNOWN";
            learningPumpState = "UNKNOWN";
            loopRepairReason = "";
            serviceGeneration = 0L;
            processStartId = 0L;
            analysisProgressAt = 0L;
            analysisProgressPercent = 0;
            analysisProgressStage = "";
            recoveryLeaseUntil = 0L;
            restartRequestCount = 0;
            actualProcessStartCount = 0;
            updatedAt = 0L;
        }
    }

    public static final class BackgroundSliceState {
        public String taskKey;
        public String status;
        public int processed;
        public int pending;
        public long costMs;
        public String cursorIssue;
        public String summary;
        public long updatedAt;

        public BackgroundSliceState() {
            taskKey = "";
            status = "IDLE";
            processed = 0;
            pending = 0;
            costMs = 0L;
            cursorIssue = "";
            summary = "";
            updatedAt = 0L;
        }
    }

    public static final class ProbabilityIssueAudit {
        public String targetIssue;
        public String cohort;
        public String pipelineVersion;
        public int candidateCount;
        public int deepCandidateCount;
        public int shallowCandidateCount;
        public int storeCacheHit;
        public int storeCacheMiss;
        public double rawP;
        public double afterTeacherP;
        public double afterRiskP;
        public double finalP;
        public long totalCostMs;
        public String summary;
        public long updatedAt;

        public ProbabilityIssueAudit() {
            targetIssue = "";
            cohort = "";
            pipelineVersion = "";
            candidateCount = 0;
            deepCandidateCount = 0;
            shallowCandidateCount = 0;
            storeCacheHit = 0;
            storeCacheMiss = 0;
            rawP = 0.0d;
            afterTeacherP = 0.0d;
            afterRiskP = 0.0d;
            finalP = 0.0d;
            totalCostMs = 0L;
            summary = "";
            updatedAt = 0L;
        }
    }

    public static final class CrashSnapshot {
        public long at;
        public String threadName;
        public String errorType;
        public String message;
        public String stack;
        public String targetIssue;
        public String stage;
        public int databaseQueueLength;
        public long freeMemory;
        public long totalMemory;

        public CrashSnapshot() {
            at = 0L;
            threadName = "";
            errorType = "";
            message = "";
            stack = "";
            targetIssue = "";
            stage = "";
            databaseQueueLength = 0;
            freeMemory = 0L;
            totalMemory = 0L;
        }
    }

    public static class DomainEvidenceStat {
        public String domainKey;
        public String parentDomainKey;
        public String coarseDomainKey;
        public String status;
        public String version;
        public int sampleCount;
        public int hitCount;
        public int missCount;
        public int recentSampleCount;
        public int recentHitCount;
        public int misleadingStreak;
        public double hitRate;
        public double recentHitRate;
        public double posteriorMean;
        public double lowerBound;
        public double upperBound;
        public double driftScore;
        public long updatedAt;

        public DomainEvidenceStat() {
            domainKey = "";
            parentDomainKey = "";
            coarseDomainKey = "";
            status = "NO_SAMPLE";
            version = AppConfig.FULL_CLOSED_LOOP_18_VERSION;
            sampleCount = 0;
            hitCount = 0;
            missCount = 0;
            recentSampleCount = 0;
            recentHitCount = 0;
            misleadingStreak = 0;
            hitRate = 0.0d;
            recentHitRate = 0.0d;
            posteriorMean = 0.0d;
            lowerBound = 0.0d;
            upperBound = 1.0d;
            driftScore = 0.0d;
            updatedAt = 0L;
        }
    }

    public static final class TeacherDomainStat extends DomainEvidenceStat {
        public double probabilityAdjust;
        public double riskScore;

        public TeacherDomainStat() {
            super();
            probabilityAdjust = 0.0d;
            riskScore = 0.0d;
        }
    }

    public static final class DynamicDomainStat extends DomainEvidenceStat {
        public double evidenceContribution;
        public double counterContribution;

        public DynamicDomainStat() {
            super();
            evidenceContribution = 0.0d;
            counterContribution = 0.0d;
        }
    }

    public static final class PromotionDomainStat extends DomainEvidenceStat {
        public double promotionScore;
        public int forwardValidated;

        public PromotionDomainStat() {
            super();
            promotionScore = 0.0d;
            forwardValidated = 0;
        }
    }

    public static final class CandidateEvidenceSnapshot {
        public String snapshotKey,
                baseIssue,
                targetIssue,
                auditClusterKey,
                learningClusterKey,
                promotionDomain,
                cohort,
                version;
        public int position, n1, n2, n3, rankNo, settled, wouldHit, actualNumber;
        public double finalP,
                gate,
                edge,
                coverageQuality,
                coverageRequired,
                coverageRisk,
                thirdQuality,
                needSupplyScore,
                counterRisk;
        public long createdAt, settledAt;

        public CandidateEvidenceSnapshot() {
            snapshotKey = "";
            baseIssue = "";
            targetIssue = "";
            auditClusterKey = "";
            learningClusterKey = "";
            promotionDomain = "";
            cohort = AppConfig.ACTIVE_FIX_COHORT;
            version = AppConfig.FULL_CLOSED_LOOP_18_VERSION;
            position = 0;
            n1 = 0;
            n2 = 0;
            n3 = 0;
            rankNo = 0;
            settled = 0;
            wouldHit = 0;
            actualNumber = 0;
            finalP = 0.0d;
            gate = 0.0d;
            edge = 0.0d;
            coverageQuality = 0.0d;
            coverageRequired = AppConfig.COVERAGE_REQUIRED_DEFAULT;
            coverageRisk = 0.0d;
            thirdQuality = 0.0d;
            needSupplyScore = 0.0d;
            counterRisk = 0.0d;
            createdAt = 0L;
            settledAt = 0L;
        }
    }

    public static final class ForwardValidationStat extends DomainEvidenceStat {
        public int forwardOnly;

        public ForwardValidationStat() {
            super();
            forwardOnly = 1;
        }
    }

    public static final class ServiceRecoveryEvent {
        public long eventAt, heartbeatAgeMs;
        public String source, stage, result, detail, version;

        public ServiceRecoveryEvent() {
            eventAt = 0L;
            heartbeatAgeMs = 0L;
            source = "";
            stage = "";
            result = "";
            detail = "";
            version = AppConfig.FULL_CLOSED_LOOP_18_VERSION;
        }
    }

    /** 统一开奖前固定域键。必须在Calibration/Gate之后生成，结算时禁止重新分桶。 */
    public static String[] buildStableEvidenceDomains(Object obj) {
        if (obj != null && readPublicInt(obj, "evidenceDomainFrozen", 0) == 1) {
            String c = readPublicString(obj, "promotionCoarseDomain", "");
            String m = readPublicString(obj, "promotionMidDomain", "");
            String f = readPublicString(obj, "promotionFineDomain", "");
            if (c.length() > 0 && m.length() > 0 && f.length() > 0) return new String[] {c, m, f};
        }
        int position = readPublicInt(obj, "position", 0);
        int mask = readPublicInt(obj, "k3Mask", 0);
        double p = readPublicDouble(obj, "finalP", 0.0d);
        if (Double.isNaN(p) || Double.isInfinite(p) || p <= 0.0d)
            p = readPublicDouble(obj, "calibratedP", 0.0d);
        if (Double.isNaN(p) || Double.isInfinite(p) || p <= 0.0d)
            p = readPublicDouble(obj, "rawP", AppConfig.RANDOM_K3_BASELINE);
        p = AppConfig.clampProbability(p);
        double cover =
                maxFinite(
                        readPublicDouble(obj, "coverageQuality", 0.0d),
                        readPublicDouble(obj, "coverageQualityScore", 0.0d));
        double third =
                maxFinite(
                        readPublicDouble(obj, "thirdQuality", 0.0d),
                        readPublicDouble(obj, "thirdNumberQualityScore", 0.0d));
        double counter =
                maxFinite(
                        readPublicDouble(obj, "counterRisk", 0.0d),
                        readPublicDouble(obj, "counterEvidenceScore", 0.0d));
        String band = AppConfig.probabilityBand(p);
        String quality =
                cover >= AppConfig.REAL_BET_PROBE_MIN_COVERAGE
                                && third >= AppConfig.REAL_BET_PROBE_MIN_THIRD
                        ? "Q_OK"
                        : "Q_WEAK";
        String risk = counter <= AppConfig.REAL_BET_PROBE_MAX_COUNTER_RISK ? "R_OK" : "R_HIGH";
        String state =
                safeText(
                        readPublicString(
                                obj, "windowStateKey", readPublicString(obj, "stateKey", "")));
        String stateHash = state.length() == 0 ? "S_ANY" : "S" + Math.abs(state.hashCode() % 997);
        String coarse = AppConfig.sanitizePromotionDomainKey("COARSE|P" + position + "|" + band);
        String mid =
                AppConfig.sanitizePromotionDomainKey(
                        "MID|P" + position + "|" + band + "|" + quality + "|" + risk);
        String fine =
                AppConfig.sanitizePromotionDomainKey(
                        "FINE|P" + position + "|" + band + "|" + quality + "|" + risk + "|"
                                + stateHash + "|K=" + mask);
        return new String[] {coarse, mid, fine};
    }

    public static void stampStableEvidenceDomains(Object obj) {
        freezeStableEvidenceDomains(obj, readPublicString(obj, "targetIssue", ""));
    }

    public static void freezeStableEvidenceDomains(Object obj, String targetIssue) {
        if (obj == null) return;
        int frozen = readPublicInt(obj, "evidenceDomainFrozen", 0);
        String coarse0 = readPublicString(obj, "promotionCoarseDomain", "");
        String mid0 = readPublicString(obj, "promotionMidDomain", "");
        String fine0 = readPublicString(obj, "promotionFineDomain", "");
        if (frozen == 1 && coarse0.length() > 0 && mid0.length() > 0 && fine0.length() > 0) return;
        String[] d = buildStableEvidenceDomains(obj);
        writePublicField(obj, "promotionCoarseDomain", d[0]);
        writePublicField(obj, "promotionMidDomain", d[1]);
        writePublicField(obj, "promotionFineDomain", d[2]);
        writePublicField(obj, "shadowPromotionDomain", d[2]);
        writePublicField(
                obj, "promotionDomainAvailable", Integer.valueOf(d[2].length() > 0 ? 1 : 0));
        writePublicField(obj, "preDecisionDomainVersion", AppConfig.ACTIVE_EVIDENCE_DOMAIN_SCHEMA);
        writePublicField(obj, "evidenceDomainFrozen", Integer.valueOf(1));
        writePublicField(obj, "evidenceDomainFrozenIssue", safeText(targetIssue));
        writePublicField(obj, "evidenceDomainFrozenAt", Long.valueOf(System.currentTimeMillis()));
        writePublicField(obj, "moduleBuildVersion", AppConfig.ACTIVE_QUALITY_BUILD_VERSION);
    }

    private static int readPublicInt(Object o, String n, int d) {
        try {
            return o.getClass().getField(n).getInt(o);
        } catch (Throwable e) {
            return d;
        }
    }

    private static double readPublicDouble(Object o, String n, double d) {
        try {
            return o.getClass().getField(n).getDouble(o);
        } catch (Throwable e) {
            return d;
        }
    }

    private static String readPublicString(Object o, String n, String d) {
        try {
            Object v = o.getClass().getField(n).get(o);
            return v == null ? d : String.valueOf(v);
        } catch (Throwable e) {
            return d;
        }
    }

    private static void writePublicField(Object o, String n, Object v) {
        try {
            java.lang.reflect.Field f = o.getClass().getField(n);
            if (f.getType() == Integer.TYPE) f.setInt(o, ((Number) v).intValue());
            else if (f.getType() == Long.TYPE) f.setLong(o, ((Number) v).longValue());
            else if (f.getType() == Double.TYPE) f.setDouble(o, ((Number) v).doubleValue());
            else f.set(o, v);
        } catch (Throwable e) {
        }
    }

    private static double maxFinite(double a, double b, double c) {
        double x = Double.isNaN(a) || Double.isInfinite(a) ? 0.0d : a;
        double y = Double.isNaN(b) || Double.isInfinite(b) ? 0.0d : b;
        double z = Double.isNaN(c) || Double.isInfinite(c) ? 0.0d : c;
        return Math.max(x, Math.max(y, z));
    }

    private static double maxFinite(double a, double b) {
        return maxFinite(a, b, 0.0d);
    }

    public static final class EngineStageAudit {
        public String targetIssue, stage, module, status, detail, version;
        public int inputCount, appliedCount, nonZeroCount, errorCount;
        public long startedAt, finishedAt, costMs;

        public EngineStageAudit() {
            targetIssue = "";
            stage = "";
            module = "";
            status = "IDLE";
            detail = "";
            version = AppConfig.ACTIVE_QUALITY_BUILD_VERSION;
        }
    }

    public static final class ModuleBuildStatus {
        public String moduleName, version, cohort, status, detail;
        public long registeredAt;

        public ModuleBuildStatus() {
            moduleName = "";
            version = "";
            cohort = "";
            status = "MISSING";
            detail = "";
            registeredAt = 0L;
        }
    }

    /** 73487246：Candidate/Decision/Trade只复制同名公共字段，防止新增证据在桥接阶段丢失。 */
    public static void copySharedPublicFieldsV20(Object from, Object to) {
        if (from == null || to == null) return;
        java.lang.reflect.Field[] fs = from.getClass().getFields();
        for (int i = 0; i < fs.length; i++) {
            try {
                java.lang.reflect.Field a = fs[i];
                if (java.lang.reflect.Modifier.isStatic(a.getModifiers())) continue;
                java.lang.reflect.Field b = to.getClass().getField(a.getName());
                if (java.lang.reflect.Modifier.isStatic(b.getModifiers())) continue;
                if (!b.getType().isAssignableFrom(a.getType())
                        && !a.getType().isAssignableFrom(b.getType())) continue;
                b.set(to, a.get(from));
            } catch (Throwable ignore) {
            }
        }
    }

    /* STARCORE_TIANSHU_B02_MATERIALIZED_20260805 */
    public static final String B02_GAME_CODE = "Binance1-PK10";
    public static final String B02_SCHEMA_VERSION = "STARCORE_TIANSHU_B02_MODELS_V1";
    public static final int B02_ACTION_SPACE = 1200;

    private static String safeB02(String value) {
        return value == null ? "" : value;
    }

    private static int[] copyB02(int[] value) {
        if (value == null) return new int[0];
        int[] out = new int[value.length];
        System.arraycopy(value, 0, out, 0, value.length);
        return out;
    }

    private static double[] copyB02(double[] value) {
        if (value == null) return new double[0];
        double[] out = new double[value.length];
        System.arraycopy(value, 0, out, 0, value.length);
        return out;
    }

    private static <T> java.util.List<T> immutableListB02(java.util.List<T> value) {
        java.util.ArrayList<T> out = new java.util.ArrayList<T>();
        if (value != null) out.addAll(value);
        return java.util.Collections.unmodifiableList(out);
    }

    private static String arrayCanonicalB02(int[] values) {
        StringBuilder sb = new StringBuilder();
        if (values != null)
            for (int i = 0; i < values.length; i++) {
                if (i > 0) sb.append(',');
                sb.append(values[i]);
            }
        return sb.toString();
    }

    private static String arrayCanonicalB02(double[] values) {
        StringBuilder sb = new StringBuilder();
        if (values != null)
            for (int i = 0; i < values.length; i++) {
                if (i > 0) sb.append(',');
                sb.append(Double.toString(values[i]));
            }
        return sb.toString();
    }

    private static String listCanonicalB02(java.util.List<?> values) {
        StringBuilder sb = new StringBuilder();
        if (values != null)
            for (int i = 0; i < values.size(); i++) {
                if (i > 0) sb.append(',');
                sb.append(String.valueOf(values.get(i)));
            }
        return sb.toString();
    }

    private static boolean probabilityB02(double value) {
        return !Double.isNaN(value) && !Double.isInfinite(value) && value >= 0.0d && value <= 1.0d;
    }

    private static boolean validK3B02(int n1, int n2, int n3) {
        return n1 >= 1 && n1 <= 10 && n2 >= 1 && n2 <= 10 && n3 >= 1 && n3 <= 10 && n1 != n2
                && n1 != n3 && n2 != n3;
    }

    private static boolean isPermutation10B02(int[] numbers) {
        if (numbers == null || numbers.length != 10) return false;
        boolean[] seen = new boolean[11];
        for (int i = 0; i < numbers.length; i++) {
            int n = numbers[i];
            if (n < 1 || n > 10 || seen[n]) return false;
            seen[n] = true;
        }
        return true;
    }

    public static String b02Sha256(String text) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] data = md.digest(safeB02(text).getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder(data.length * 2);
            for (int i = 0; i < data.length; i++) {
                int x = data[i] & 0xff;
                if (x < 16) sb.append('0');
                sb.append(Integer.toHexString(x));
            }
            return sb.toString();
        } catch (Throwable e) {
            throw new IllegalStateException("B02_SHA256_UNAVAILABLE", e);
        }
    }

    public static int b02ActionIndex(int position, int n1, int n2, int n3) {
        if (!validK3B02(n1, n2, n3) || position < 1 || position > 10) return -1;
        int[] a = new int[] {n1, n2, n3};
        java.util.Arrays.sort(a);
        int combo = 0;
        for (int x = 1; x <= 8; x++)
            for (int y = x + 1; y <= 9; y++)
                for (int z = y + 1; z <= 10; z++) {
                    if (a[0] == x && a[1] == y && a[2] == z) return (position - 1) * 120 + combo;
                    combo++;
                }
        return -1;
    }

    public static java.util.List<B02K3Action> b02EnumerateActions(String targetIssue) {
        java.util.ArrayList<B02K3Action> out = new java.util.ArrayList<B02K3Action>(1200);
        for (int p = 1; p <= 10; p++)
            for (int a = 1; a <= 8; a++)
                for (int b = a + 1; b <= 9; b++)
                    for (int c = b + 1; c <= 10; c++) {
                        int index = b02ActionIndex(p, a, b, c);
                        out.add(
                                new B02K3Action(
                                        "B02-A-" + index,
                                        targetIssue,
                                        p,
                                        a,
                                        b,
                                        c,
                                        index,
                                        0.3d,
                                        0.3d,
                                        0.0d,
                                        "SHADOW",
                                        b02Sha256(targetIssue + "|" + index),
                                        true));
                    }
        return java.util.Collections.unmodifiableList(out);
    }

    public static final class B02DrawIdentity {
        public final String gameCode;
        public final String providerIssue;
        public final String gmtIssue;
        public final String issue;
        public final long rawOpenTime;
        public final long providerOpenTime;
        public final long fetchTime;
        public final String sourceLine;
        public final String canonicalDrawHash;
        public final String rawPayloadHash;
        public final int[] numbers;

        public B02DrawIdentity(
                String gameCode,
                String providerIssue,
                String gmtIssue,
                String issue,
                long rawOpenTime,
                long providerOpenTime,
                long fetchTime,
                String sourceLine,
                String canonicalDrawHash,
                String rawPayloadHash,
                int[] numbers) {
            this.gameCode = safeB02(gameCode);
            this.providerIssue = safeB02(providerIssue);
            this.gmtIssue = safeB02(gmtIssue);
            this.issue = safeB02(issue);
            this.rawOpenTime = rawOpenTime;
            this.providerOpenTime = providerOpenTime;
            this.fetchTime = fetchTime;
            this.sourceLine = safeB02(sourceLine);
            this.canonicalDrawHash = safeB02(canonicalDrawHash);
            this.rawPayloadHash = safeB02(rawPayloadHash);
            this.numbers = copyB02(numbers);
            validate();
        }

        private void validate() {
            if (!"Binance1-PK10".equals(gameCode))
                throw new IllegalArgumentException("B02_GAME_CODE");
            if (issue.length() == 0 || providerIssue.length() == 0)
                throw new IllegalArgumentException("B02_ISSUE");
            if (!isPermutation10B02(numbers)) throw new IllegalArgumentException("B02_NUMBERS");
            if (canonicalDrawHash.length() != 64)
                throw new IllegalArgumentException("B02_DRAW_HASH");
        }

        public String canonical() {
            return String.valueOf(gameCode)
                    + "|"
                    + String.valueOf(providerIssue)
                    + "|"
                    + String.valueOf(gmtIssue)
                    + "|"
                    + String.valueOf(issue)
                    + "|"
                    + String.valueOf(rawOpenTime)
                    + "|"
                    + String.valueOf(providerOpenTime)
                    + "|"
                    + String.valueOf(fetchTime)
                    + "|"
                    + String.valueOf(sourceLine)
                    + "|"
                    + String.valueOf(canonicalDrawHash)
                    + "|"
                    + String.valueOf(rawPayloadHash)
                    + "|"
                    + arrayCanonicalB02(numbers);
        }

        public String contractHash() {
            return b02Sha256(canonical());
        }
    }

    public static final class B02PitSnapshot {
        public final String gameCode;
        public final String baseIssue;
        public final String targetIssue;
        public final String historyMaxIssue;
        public final long frozenAt;
        public final long providerCutoffTime;
        public final String historyHash;
        public final String drawContractHash;
        public final String featureSchemaHash;
        public final String stateSchemaHash;
        public final String modelBundleHash;
        public final java.util.List<String> visibleIssues;
        public final boolean futureLeakFree;
        public final boolean shadowOnly;

        public B02PitSnapshot(
                String gameCode,
                String baseIssue,
                String targetIssue,
                String historyMaxIssue,
                long frozenAt,
                long providerCutoffTime,
                String historyHash,
                String drawContractHash,
                String featureSchemaHash,
                String stateSchemaHash,
                String modelBundleHash,
                java.util.List<String> visibleIssues,
                boolean futureLeakFree,
                boolean shadowOnly) {
            this.gameCode = safeB02(gameCode);
            this.baseIssue = safeB02(baseIssue);
            this.targetIssue = safeB02(targetIssue);
            this.historyMaxIssue = safeB02(historyMaxIssue);
            this.frozenAt = frozenAt;
            this.providerCutoffTime = providerCutoffTime;
            this.historyHash = safeB02(historyHash);
            this.drawContractHash = safeB02(drawContractHash);
            this.featureSchemaHash = safeB02(featureSchemaHash);
            this.stateSchemaHash = safeB02(stateSchemaHash);
            this.modelBundleHash = safeB02(modelBundleHash);
            this.visibleIssues = immutableListB02(visibleIssues);
            this.futureLeakFree = futureLeakFree;
            this.shadowOnly = shadowOnly;
            validate();
        }

        private void validate() {
            if (!"Binance1-PK10".equals(gameCode))
                throw new IllegalArgumentException("B02_PIT_GAME");
            if (baseIssue.length() == 0 || targetIssue.length() == 0)
                throw new IllegalArgumentException("B02_PIT_ISSUE");
            if (frozenAt <= 0L || providerCutoffTime > frozenAt)
                throw new IllegalArgumentException("B02_PIT_TIME");
            if (!futureLeakFree || !shadowOnly) throw new IllegalArgumentException("B02_PIT_LOCK");
        }

        public String canonical() {
            return String.valueOf(gameCode)
                    + "|"
                    + String.valueOf(baseIssue)
                    + "|"
                    + String.valueOf(targetIssue)
                    + "|"
                    + String.valueOf(historyMaxIssue)
                    + "|"
                    + String.valueOf(frozenAt)
                    + "|"
                    + String.valueOf(providerCutoffTime)
                    + "|"
                    + String.valueOf(historyHash)
                    + "|"
                    + String.valueOf(drawContractHash)
                    + "|"
                    + String.valueOf(featureSchemaHash)
                    + "|"
                    + String.valueOf(stateSchemaHash)
                    + "|"
                    + String.valueOf(modelBundleHash)
                    + "|"
                    + listCanonicalB02(visibleIssues)
                    + "|"
                    + String.valueOf(futureLeakFree)
                    + "|"
                    + String.valueOf(shadowOnly);
        }

        public String contractHash() {
            return b02Sha256(canonical());
        }
    }

    public static final class B02EvidencePacket {
        public final String packetId;
        public final String gameCode;
        public final String baseIssue;
        public final String targetIssue;
        public final long createdAt;
        public final String pitHash;
        public final String drawHash;
        public final String featureHash;
        public final String stateHash;
        public final String expertHash;
        public final String numberProbabilityHash;
        public final String actionHash;
        public final String certificateHash;
        public final java.util.List<String> evidenceIds;
        public final java.util.List<String> counterEvidenceIds;
        public final boolean complete;
        public final boolean shadowOnly;

        public B02EvidencePacket(
                String packetId,
                String gameCode,
                String baseIssue,
                String targetIssue,
                long createdAt,
                String pitHash,
                String drawHash,
                String featureHash,
                String stateHash,
                String expertHash,
                String numberProbabilityHash,
                String actionHash,
                String certificateHash,
                java.util.List<String> evidenceIds,
                java.util.List<String> counterEvidenceIds,
                boolean complete,
                boolean shadowOnly) {
            this.packetId = safeB02(packetId);
            this.gameCode = safeB02(gameCode);
            this.baseIssue = safeB02(baseIssue);
            this.targetIssue = safeB02(targetIssue);
            this.createdAt = createdAt;
            this.pitHash = safeB02(pitHash);
            this.drawHash = safeB02(drawHash);
            this.featureHash = safeB02(featureHash);
            this.stateHash = safeB02(stateHash);
            this.expertHash = safeB02(expertHash);
            this.numberProbabilityHash = safeB02(numberProbabilityHash);
            this.actionHash = safeB02(actionHash);
            this.certificateHash = safeB02(certificateHash);
            this.evidenceIds = immutableListB02(evidenceIds);
            this.counterEvidenceIds = immutableListB02(counterEvidenceIds);
            this.complete = complete;
            this.shadowOnly = shadowOnly;
            validate();
        }

        private void validate() {
            if (packetId.length() == 0 || targetIssue.length() == 0)
                throw new IllegalArgumentException("B02_PACKET_ID");
            if (!"Binance1-PK10".equals(gameCode))
                throw new IllegalArgumentException("B02_PACKET_GAME");
            if (!complete || !shadowOnly) throw new IllegalArgumentException("B02_PACKET_LOCK");
        }

        public String canonical() {
            return String.valueOf(packetId)
                    + "|"
                    + String.valueOf(gameCode)
                    + "|"
                    + String.valueOf(baseIssue)
                    + "|"
                    + String.valueOf(targetIssue)
                    + "|"
                    + String.valueOf(createdAt)
                    + "|"
                    + String.valueOf(pitHash)
                    + "|"
                    + String.valueOf(drawHash)
                    + "|"
                    + String.valueOf(featureHash)
                    + "|"
                    + String.valueOf(stateHash)
                    + "|"
                    + String.valueOf(expertHash)
                    + "|"
                    + String.valueOf(numberProbabilityHash)
                    + "|"
                    + String.valueOf(actionHash)
                    + "|"
                    + String.valueOf(certificateHash)
                    + "|"
                    + listCanonicalB02(evidenceIds)
                    + "|"
                    + listCanonicalB02(counterEvidenceIds)
                    + "|"
                    + String.valueOf(complete)
                    + "|"
                    + String.valueOf(shadowOnly);
        }

        public String contractHash() {
            return b02Sha256(canonical());
        }
    }

    public static final class B02QMatrix10 {
        public final String targetIssue;
        public final double[] values;
        public final String normalization;
        public final String matrixHash;
        public final boolean rowColumnNormalized;

        public B02QMatrix10(
                String targetIssue,
                double[] values,
                String normalization,
                String matrixHash,
                boolean rowColumnNormalized) {
            this.targetIssue = safeB02(targetIssue);
            this.values = copyB02(values);
            this.normalization = safeB02(normalization);
            this.matrixHash = safeB02(matrixHash);
            this.rowColumnNormalized = rowColumnNormalized;
            validate();
        }

        private void validate() {
            if (targetIssue.length() == 0) throw new IllegalArgumentException("B02_Q_ISSUE");
            if (values == null || values.length != 100)
                throw new IllegalArgumentException("B02_Q_SIZE");
            if (!rowColumnNormalized) throw new IllegalArgumentException("B02_Q_NORMALIZATION");
        }

        public String canonical() {
            return String.valueOf(targetIssue)
                    + "|"
                    + arrayCanonicalB02(values)
                    + "|"
                    + String.valueOf(normalization)
                    + "|"
                    + String.valueOf(matrixHash)
                    + "|"
                    + String.valueOf(rowColumnNormalized);
        }

        public String contractHash() {
            return b02Sha256(canonical());
        }
    }

    public static final class B02CrossPositionTensor {
        public final String targetIssue;
        public final int positionCount;
        public final int numberCount;
        public final int lagCount;
        public final double[] values;
        public final String tensorHash;
        public final String owner;

        public B02CrossPositionTensor(
                String targetIssue,
                int positionCount,
                int numberCount,
                int lagCount,
                double[] values,
                String tensorHash,
                String owner) {
            this.targetIssue = safeB02(targetIssue);
            this.positionCount = positionCount;
            this.numberCount = numberCount;
            this.lagCount = lagCount;
            this.values = copyB02(values);
            this.tensorHash = safeB02(tensorHash);
            this.owner = safeB02(owner);
            validate();
        }

        private void validate() {
            if (positionCount != 10 || numberCount != 10)
                throw new IllegalArgumentException("B02_TENSOR_SHAPE");
            if (lagCount <= 0
                    || values == null
                    || values.length != positionCount * numberCount * lagCount)
                throw new IllegalArgumentException("B02_TENSOR_SIZE");
        }

        public String canonical() {
            return String.valueOf(targetIssue)
                    + "|"
                    + String.valueOf(positionCount)
                    + "|"
                    + String.valueOf(numberCount)
                    + "|"
                    + String.valueOf(lagCount)
                    + "|"
                    + arrayCanonicalB02(values)
                    + "|"
                    + String.valueOf(tensorHash)
                    + "|"
                    + String.valueOf(owner);
        }

        public String contractHash() {
            return b02Sha256(canonical());
        }
    }

    public static final class B02FunnelPath {
        public final String targetIssue;
        public final String actionId;
        public final java.util.List<String> stages;
        public final java.util.List<String> passReasons;
        public final java.util.List<String> blockReasons;
        public final String terminalState;
        public final boolean realBetAuthorized;
        public final String pathHash;

        public B02FunnelPath(
                String targetIssue,
                String actionId,
                java.util.List<String> stages,
                java.util.List<String> passReasons,
                java.util.List<String> blockReasons,
                String terminalState,
                boolean realBetAuthorized,
                String pathHash) {
            this.targetIssue = safeB02(targetIssue);
            this.actionId = safeB02(actionId);
            this.stages = immutableListB02(stages);
            this.passReasons = immutableListB02(passReasons);
            this.blockReasons = immutableListB02(blockReasons);
            this.terminalState = safeB02(terminalState);
            this.realBetAuthorized = realBetAuthorized;
            this.pathHash = safeB02(pathHash);
            validate();
        }

        private void validate() {
            if (targetIssue.length() == 0 || actionId.length() == 0)
                throw new IllegalArgumentException("B02_FUNNEL_ID");
            if (realBetAuthorized) throw new IllegalArgumentException("B02_REAL_BET_HARD_DISABLED");
        }

        public String canonical() {
            return String.valueOf(targetIssue)
                    + "|"
                    + String.valueOf(actionId)
                    + "|"
                    + listCanonicalB02(stages)
                    + "|"
                    + listCanonicalB02(passReasons)
                    + "|"
                    + listCanonicalB02(blockReasons)
                    + "|"
                    + String.valueOf(terminalState)
                    + "|"
                    + String.valueOf(realBetAuthorized)
                    + "|"
                    + String.valueOf(pathHash);
        }

        public String contractHash() {
            return b02Sha256(canonical());
        }
    }

    public static final class B02K3Action {
        public final String actionId;
        public final String targetIssue;
        public final int position;
        public final int n1;
        public final int n2;
        public final int n3;
        public final int actionIndex;
        public final double rawProbability;
        public final double calibratedProbability;
        public final double selectedHitP;
        public final String lane;
        public final String evidenceHash;
        public final boolean shadowOnly;

        public B02K3Action(
                String actionId,
                String targetIssue,
                int position,
                int n1,
                int n2,
                int n3,
                int actionIndex,
                double rawProbability,
                double calibratedProbability,
                double selectedHitP,
                String lane,
                String evidenceHash,
                boolean shadowOnly) {
            this.actionId = safeB02(actionId);
            this.targetIssue = safeB02(targetIssue);
            this.position = position;
            this.n1 = n1;
            this.n2 = n2;
            this.n3 = n3;
            this.actionIndex = actionIndex;
            this.rawProbability = rawProbability;
            this.calibratedProbability = calibratedProbability;
            this.selectedHitP = selectedHitP;
            this.lane = safeB02(lane);
            this.evidenceHash = safeB02(evidenceHash);
            this.shadowOnly = shadowOnly;
            validate();
        }

        private void validate() {
            if (position < 1 || position > 10)
                throw new IllegalArgumentException("B02_ACTION_POSITION");
            if (!validK3B02(n1, n2, n3)) throw new IllegalArgumentException("B02_ACTION_K3");
            if (actionIndex < 0 || actionIndex >= 1200)
                throw new IllegalArgumentException("B02_ACTION_INDEX");
            if (!shadowOnly) throw new IllegalArgumentException("B02_ACTION_SHADOW");
        }

        public String canonical() {
            return String.valueOf(actionId)
                    + "|"
                    + String.valueOf(targetIssue)
                    + "|"
                    + String.valueOf(position)
                    + "|"
                    + String.valueOf(n1)
                    + "|"
                    + String.valueOf(n2)
                    + "|"
                    + String.valueOf(n3)
                    + "|"
                    + String.valueOf(actionIndex)
                    + "|"
                    + String.valueOf(rawProbability)
                    + "|"
                    + String.valueOf(calibratedProbability)
                    + "|"
                    + String.valueOf(selectedHitP)
                    + "|"
                    + String.valueOf(lane)
                    + "|"
                    + String.valueOf(evidenceHash)
                    + "|"
                    + String.valueOf(shadowOnly);
        }

        public String contractHash() {
            return b02Sha256(canonical());
        }

        public int mask() {
            return (1 << (n1 - 1)) | (1 << (n2 - 1)) | (1 << (n3 - 1));
        }

        public boolean contains(int number) {
            return number == n1 || number == n2 || number == n3;
        }
    }

    public static final class B02Champion {
        public final String targetIssue;
        public final String actionId;
        public final int rank;
        public final double selectedHitP;
        public final double gate;
        public final double edge;
        public final String lane;
        public final String championHash;
        public final boolean modelProven;
        public final boolean realBetAuthorized;

        public B02Champion(
                String targetIssue,
                String actionId,
                int rank,
                double selectedHitP,
                double gate,
                double edge,
                String lane,
                String championHash,
                boolean modelProven,
                boolean realBetAuthorized) {
            this.targetIssue = safeB02(targetIssue);
            this.actionId = safeB02(actionId);
            this.rank = rank;
            this.selectedHitP = selectedHitP;
            this.gate = gate;
            this.edge = edge;
            this.lane = safeB02(lane);
            this.championHash = safeB02(championHash);
            this.modelProven = modelProven;
            this.realBetAuthorized = realBetAuthorized;
            validate();
        }

        private void validate() {
            if (rank < 1) throw new IllegalArgumentException("B02_CHAMPION_RANK");
            if (modelProven || realBetAuthorized)
                throw new IllegalArgumentException("B02_CHAMPION_LOCK");
        }

        public String canonical() {
            return String.valueOf(targetIssue)
                    + "|"
                    + String.valueOf(actionId)
                    + "|"
                    + String.valueOf(rank)
                    + "|"
                    + String.valueOf(selectedHitP)
                    + "|"
                    + String.valueOf(gate)
                    + "|"
                    + String.valueOf(edge)
                    + "|"
                    + String.valueOf(lane)
                    + "|"
                    + String.valueOf(championHash)
                    + "|"
                    + String.valueOf(modelProven)
                    + "|"
                    + String.valueOf(realBetAuthorized);
        }

        public String contractHash() {
            return b02Sha256(canonical());
        }
    }

    public static final class B02SelectedHitP {
        public final String targetIssue;
        public final String actionId;
        public final double rawP;
        public final double calibratedP;
        public final double domainP;
        public final double certificateP;
        public final double finalSelectedHitP;
        public final String calibrationHash;
        public final String certificateHash;
        public final boolean modelProven;

        public B02SelectedHitP(
                String targetIssue,
                String actionId,
                double rawP,
                double calibratedP,
                double domainP,
                double certificateP,
                double finalSelectedHitP,
                String calibrationHash,
                String certificateHash,
                boolean modelProven) {
            this.targetIssue = safeB02(targetIssue);
            this.actionId = safeB02(actionId);
            this.rawP = rawP;
            this.calibratedP = calibratedP;
            this.domainP = domainP;
            this.certificateP = certificateP;
            this.finalSelectedHitP = finalSelectedHitP;
            this.calibrationHash = safeB02(calibrationHash);
            this.certificateHash = safeB02(certificateHash);
            this.modelProven = modelProven;
            validate();
        }

        private void validate() {
            if (modelProven) throw new IllegalArgumentException("B02_MODEL_NOT_PROVEN");
            if (!probabilityB02(rawP)
                    || !probabilityB02(calibratedP)
                    || !probabilityB02(finalSelectedHitP))
                throw new IllegalArgumentException("B02_P_RANGE");
        }

        public String canonical() {
            return String.valueOf(targetIssue)
                    + "|"
                    + String.valueOf(actionId)
                    + "|"
                    + String.valueOf(rawP)
                    + "|"
                    + String.valueOf(calibratedP)
                    + "|"
                    + String.valueOf(domainP)
                    + "|"
                    + String.valueOf(certificateP)
                    + "|"
                    + String.valueOf(finalSelectedHitP)
                    + "|"
                    + String.valueOf(calibrationHash)
                    + "|"
                    + String.valueOf(certificateHash)
                    + "|"
                    + String.valueOf(modelProven);
        }

        public String contractHash() {
            return b02Sha256(canonical());
        }
    }

    public static final class B02Certificate {
        public final String certificateId;
        public final String targetIssue;
        public final String domainId;
        public final int sample;
        public final double oosHitRate;
        public final double oosCoverage;
        public final double ece;
        public final java.util.List<String> blockResults;
        public final String artifactHash;
        public final String certificateHash;
        public final boolean proven;
        public final boolean shadowOnly;

        public B02Certificate(
                String certificateId,
                String targetIssue,
                String domainId,
                int sample,
                double oosHitRate,
                double oosCoverage,
                double ece,
                java.util.List<String> blockResults,
                String artifactHash,
                String certificateHash,
                boolean proven,
                boolean shadowOnly) {
            this.certificateId = safeB02(certificateId);
            this.targetIssue = safeB02(targetIssue);
            this.domainId = safeB02(domainId);
            this.sample = sample;
            this.oosHitRate = oosHitRate;
            this.oosCoverage = oosCoverage;
            this.ece = ece;
            this.blockResults = immutableListB02(blockResults);
            this.artifactHash = safeB02(artifactHash);
            this.certificateHash = safeB02(certificateHash);
            this.proven = proven;
            this.shadowOnly = shadowOnly;
            validate();
        }

        private void validate() {
            if (certificateId.length() == 0 || targetIssue.length() == 0)
                throw new IllegalArgumentException("B02_CERT_ID");
            if (proven || !shadowOnly) throw new IllegalArgumentException("B02_CERT_LOCK");
        }

        public String canonical() {
            return String.valueOf(certificateId)
                    + "|"
                    + String.valueOf(targetIssue)
                    + "|"
                    + String.valueOf(domainId)
                    + "|"
                    + String.valueOf(sample)
                    + "|"
                    + String.valueOf(oosHitRate)
                    + "|"
                    + String.valueOf(oosCoverage)
                    + "|"
                    + String.valueOf(ece)
                    + "|"
                    + listCanonicalB02(blockResults)
                    + "|"
                    + String.valueOf(artifactHash)
                    + "|"
                    + String.valueOf(certificateHash)
                    + "|"
                    + String.valueOf(proven)
                    + "|"
                    + String.valueOf(shadowOnly);
        }

        public String contractHash() {
            return b02Sha256(canonical());
        }
    }

    public static final class B02DiagnosticFinding {
        public final String findingId;
        public final String targetIssue;
        public final String severity;
        public final String owner;
        public final String code;
        public final String summary;
        public final String evidenceHash;
        public final String repairAction;
        public final long createdAt;
        public final boolean blocksProduction;

        public B02DiagnosticFinding(
                String findingId,
                String targetIssue,
                String severity,
                String owner,
                String code,
                String summary,
                String evidenceHash,
                String repairAction,
                long createdAt,
                boolean blocksProduction) {
            this.findingId = safeB02(findingId);
            this.targetIssue = safeB02(targetIssue);
            this.severity = safeB02(severity);
            this.owner = safeB02(owner);
            this.code = safeB02(code);
            this.summary = safeB02(summary);
            this.evidenceHash = safeB02(evidenceHash);
            this.repairAction = safeB02(repairAction);
            this.createdAt = createdAt;
            this.blocksProduction = blocksProduction;
            validate();
        }

        private void validate() {
            if (findingId.length() == 0 || code.length() == 0)
                throw new IllegalArgumentException("B02_FINDING_ID");
            if (createdAt <= 0L) throw new IllegalArgumentException("B02_FINDING_TIME");
        }

        public String canonical() {
            return String.valueOf(findingId)
                    + "|"
                    + String.valueOf(targetIssue)
                    + "|"
                    + String.valueOf(severity)
                    + "|"
                    + String.valueOf(owner)
                    + "|"
                    + String.valueOf(code)
                    + "|"
                    + String.valueOf(summary)
                    + "|"
                    + String.valueOf(evidenceHash)
                    + "|"
                    + String.valueOf(repairAction)
                    + "|"
                    + String.valueOf(createdAt)
                    + "|"
                    + String.valueOf(blocksProduction);
        }

        public String contractHash() {
            return b02Sha256(canonical());
        }
    }

    public static final class B02DrawRecoveryRecord {
        public final String recoveryId;
        public final String gameCode;
        public final String fromIssue;
        public final String toIssue;
        public final String requestDate;
        public final int attempt;
        public final long nextAttemptAt;
        public final String state;
        public final String resultHash;
        public final java.util.List<String> recoveredIssues;
        public final java.util.List<String> conflictIssues;
        public final boolean complete;

        public B02DrawRecoveryRecord(
                String recoveryId,
                String gameCode,
                String fromIssue,
                String toIssue,
                String requestDate,
                int attempt,
                long nextAttemptAt,
                String state,
                String resultHash,
                java.util.List<String> recoveredIssues,
                java.util.List<String> conflictIssues,
                boolean complete) {
            this.recoveryId = safeB02(recoveryId);
            this.gameCode = safeB02(gameCode);
            this.fromIssue = safeB02(fromIssue);
            this.toIssue = safeB02(toIssue);
            this.requestDate = safeB02(requestDate);
            this.attempt = attempt;
            this.nextAttemptAt = nextAttemptAt;
            this.state = safeB02(state);
            this.resultHash = safeB02(resultHash);
            this.recoveredIssues = immutableListB02(recoveredIssues);
            this.conflictIssues = immutableListB02(conflictIssues);
            this.complete = complete;
            validate();
        }

        private void validate() {
            if (!"Binance1-PK10".equals(gameCode))
                throw new IllegalArgumentException("B02_RECOVERY_GAME");
            if (attempt < 0 || nextAttemptAt < 0L)
                throw new IllegalArgumentException("B02_RECOVERY_ATTEMPT");
        }

        public String canonical() {
            return String.valueOf(recoveryId)
                    + "|"
                    + String.valueOf(gameCode)
                    + "|"
                    + String.valueOf(fromIssue)
                    + "|"
                    + String.valueOf(toIssue)
                    + "|"
                    + String.valueOf(requestDate)
                    + "|"
                    + String.valueOf(attempt)
                    + "|"
                    + String.valueOf(nextAttemptAt)
                    + "|"
                    + String.valueOf(state)
                    + "|"
                    + String.valueOf(resultHash)
                    + "|"
                    + listCanonicalB02(recoveredIssues)
                    + "|"
                    + listCanonicalB02(conflictIssues)
                    + "|"
                    + String.valueOf(complete);
        }

        public String contractHash() {
            return b02Sha256(canonical());
        }
    }

    public static boolean b02ModelsSelfTest() {
        java.util.List<B02K3Action> actions = b02EnumerateActions("202608050001");
        if (actions.size() != 1200) return false;
        if (actions.get(0).actionIndex != 0 || actions.get(1199).actionIndex != 1199) return false;
        if (b02ActionIndex(1, 1, 2, 3) != 0) return false;
        if (b02ActionIndex(10, 8, 9, 10) != 1199) return false;
        int[] numbers = new int[] {1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
        B02DrawIdentity draw =
                new B02DrawIdentity(
                        B02_GAME_CODE,
                        "p",
                        "g",
                        "i",
                        1L,
                        1L,
                        1L,
                        "PRIMARY",
                        b02Sha256("draw"),
                        b02Sha256("raw"),
                        numbers);
        return draw.contractHash().length() == 64;
    }

public enum ReasonCode {
        NONE,
        NO_VALID_CERTIFICATE,
        NO_MAINLINE,
        STALE_HISTORY,
        PIT_INVALID,
        MISSING_SNAPSHOT,
        MODEL_MISMATCH,
        TARGET_SUPERSEDED,
        TARGET_ALREADY_DRAWN,
        COMMIT_FAILED,
        TIME_BUDGET_EXCEEDED,
        LEASE_LOST,
        DB_BUSY,
        SCHEDULE_GAP,
        CAPTURE_CAPACITY_FULL,
        WAIT_CALIBRATION,
        CALIBRATION_QUALITY_FAILED,
        WINDOW_INCOMPLETE,
        INSUFFICIENT_FUTURE_N,
        MULTIPLE_TESTING_FAILED,
        RESOURCE_UNVERIFIED,
        DRIFT_LIMIT,
        CERT_EXPIRED,
        CERT_REVOKED,
        INTEGRITY_FAILED,
        INPUT_DRIFT,
        NO_NEW_LABEL,
        RESOURCE_YIELD,
        NO_OP,
        AST_INVALID,
        MISSING_ATOM_BINDING,
        BACKGROUND_RESTRICTED,
        UNKNOWN_READ_FAILED,
        AUTH_INVALID,
        BUSY,
        CANCELLED_BEFORE_COMMIT,
        CERTIFICATE_INVALID,
        CHECKPOINT_INVALID,
        CHECKPOINT_WRITE_FAILED,
        DB_UNAVAILABLE,
        DRAW_CONFLICT,
        EVIDENCE_MISSING,
        GATE_FAILED,
        IDENTITY_DRIFT,
        INCONCLUSIVE,
        INPUT_INVALID,
        INPUT_MISSING,
        MISSING_PREDRAW,
        NO_PREDICTION,
        OWNER_NOT_COMPLETED,
        RESTART_NOT_ELIGIBLE,
        RETENTION_PINNED,
        VALIDATION_FAILED,
        WINDOW_GAP,
        REPORT_MEMORY_BUDGET,
        REPORT_DISK_BUDGET,
        REPORT_CACHE_BUDGET,
        REPORT_CANCELLED,
        REPORT_TIME_BUDGET,
        REPORT_BACKUP_FAILED,
        REPORT_CODEC_FAILED,
        REPORT_MANIFEST_INVALID,
        REPORT_MEMBER_CHANGED,
        REPORT_MISSING_PART,
        CHECK_NOT_REEVALUATED,
        REPORT_OBSERVATION_MISSING,
        REPORT_NEEDS_BASE,
        REPORT_PARTIAL_SELECTION,
        RETENTION_INPUT_MATERIALIZATION_PAUSE,
        AGGREGATE_KEY_CAPACITY,
        ARCHIVE_CORRUPT,
        STORAGE_PRESSURE,
        WAIT_BINDING,
        FROZEN_IDENTITY_MISMATCH,
        RECIPE_INCOMPLETE,
        TRAIN_INPUT_CAPACITY,
        WAIT_ARCHIVE_CAS,
        REPORT_CATALOG_LIMIT,
        REPORT_VALUE_REQUIRES_RAW_REFERENCE,
        REPORT_SCALAR_UNKNOWN,
        REPORT_REALTIME_PREEMPT,
        REPORT_USER_CANCEL,
        REPORT_STAGE_TIMEOUT,
        CHECK_UNREGISTERED,
        NO_OP_UNSUPPORTED_OPERATOR,
        ATOM_REGISTRY_IDENTITY_DRIFT
    }
}
