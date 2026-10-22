package com.ethk3.novaonepro;

import java.util.ArrayList;
import java.util.List;

public final class ShapeStateEngine {

    private static final String REPAIR_TAG = "STATE_REPAIRED_BY_STRONG_STRUCTURE";
    public static final String B06_CONTRACT_VERSION =
            "STARCORE_TIANSHU_B06_SHAPE_STATE_EVIDENCE_V1";
    public static final String B06_OWNER = "SHAPE_STATE_EVIDENCE_ONLY";

    private static final double REPAIR_TREND_LIFE_MIN = 70.0d;
    private static final double REPAIR_TREND_LIFE_STRONG = 80.0d;
    private static final double REPAIR_DRIFT_MAX = 30.0d;
    private static final double REPAIR_DRIFT_STRONG_MAX = 22.0d;
    private static final double REPAIR_BREAK_MAX = 36.0d;
    private static final double REPAIR_BREAK_STRONG_MAX = 30.0d;
    private static final double REPAIR_RECOVERY_MIN = 62.0d;
    private static final double REPAIR_RECOVERY_STRONG = 70.0d;

    public ShapeStateEngine() {}

    public List<Models.ShapeVector> refineAll(List<Models.ShapeVector> input) {
        ArrayList<Models.ShapeVector> out = new ArrayList<Models.ShapeVector>();

        if (input == null || input.size() == 0) {
            return out;
        }

        for (int i = 0; i < input.size(); i++) {
            Models.ShapeVector v = input.get(i);
            if (v == null) {
                continue;
            }

            Models.ShapeVector refined = refine(v);
            if (refined != null) {
                out.add(refined);
            }
        }

        return out;
    }

    public Models.ShapeVector refine(Models.ShapeVector source) {
        if (source == null) {
            return null;
        }

        Models.ShapeVector v = source.copyLight();

        if (source.numberProbs != null) {
            v.numberProbs = new double[source.numberProbs.length];
            for (int i = 0; i < source.numberProbs.length; i++) {
                v.numberProbs[i] = source.numberProbs[i];
            }
        }

        if (source.markovProbs != null) {
            v.markovProbs = new double[source.markovProbs.length];
            for (int i = 0; i < source.markovProbs.length; i++) {
                v.markovProbs[i] = source.markovProbs[i];
            }
        }

        normalizeVector(v);

        String rawMode = decideMode(v);
        String repairedMode = repairModeByStrongStructure(v, rawMode);

        v.stateMode = repairedMode;
        v.shapeKey = ShapeMath.buildShapeKey(v);

        if (!safeEquals(rawMode, repairedMode) && isStrongStructureRepairCandidate(v)) {
            v.shapeKey = appendShapeKey(v.shapeKey, REPAIR_TAG);
        }
        B06ShapeStateEvidence evidence = buildB06Evidence(v);
        v.reasonFingerprint = evidence.fingerprint;

        return v;
    }

    private void normalizeVector(Models.ShapeVector v) {
        if (v == null) {
            return;
        }

        v.trendLife = ShapeMath.clamp100(v.trendLife);
        v.driftRisk = ShapeMath.clamp100(v.driftRisk);
        v.breakDepth = ShapeMath.clamp100(v.breakDepth);
        v.recoveryScore = ShapeMath.clamp100(v.recoveryScore);
        v.hotReverseRisk = ShapeMath.clamp100(v.hotReverseRisk);
        v.falseStrongRisk = ShapeMath.clamp100(v.falseStrongRisk);

        if (v.championGap < 0.0d) {
            v.championGap = 0.0d;
        }

        if (v.comboGap < 0.0d) {
            v.comboGap = 0.0d;
        }

        if (v.top3Mass16 < 0.0d) {
            v.top3Mass16 = 0.0d;
        }

        if (v.top3Mass64 < 0.0d) {
            v.top3Mass64 = 0.0d;
        }

        if (v.top3Mass240 < 0.0d) {
            v.top3Mass240 = 0.0d;
        }

        if (v.top3Mass16 > 1.0d) {
            v.top3Mass16 = 1.0d;
        }

        if (v.top3Mass64 > 1.0d) {
            v.top3Mass64 = 1.0d;
        }

        if (v.top3Mass240 > 1.0d) {
            v.top3Mass240 = 1.0d;
        }
    }

    public String decideMode(Models.ShapeVector v) {
        if (v == null) {
            return AppConfig.MODE_CHAOS;
        }

        if (isDataChaos(v)) {
            return AppConfig.MODE_CHAOS;
        }

        if (isHardReverse(v)) {
            return AppConfig.MODE_REVERSING;
        }

        if (isHardBroken(v)) {
            return AppConfig.MODE_BROKEN;
        }

        if (isAccelerating(v)) {
            return AppConfig.MODE_ACCELERATING;
        }

        if (isConfirmed(v)) {
            return AppConfig.MODE_CONFIRMED;
        }

        if (isRecovering(v)) {
            return AppConfig.MODE_RECOVERING;
        }

        if (isWeakening(v)) {
            return AppConfig.MODE_WEAKENING;
        }

        if (isPullback(v)) {
            return AppConfig.MODE_PULLBACK;
        }

        return AppConfig.MODE_FORMING;
    }

    private String repairModeByStrongStructure(Models.ShapeVector v, String rawMode) {
        if (v == null) {
            return AppConfig.MODE_CHAOS;
        }

        if (rawMode == null || rawMode.length() == 0) {
            rawMode = AppConfig.MODE_CHAOS;
        }

        if (!isStrongStructureRepairCandidate(v)) {
            return rawMode;
        }

        if (AppConfig.MODE_BROKEN.equals(rawMode) || AppConfig.MODE_REVERSING.equals(rawMode)) {
            if (!isExtremeHardRisk(v)) {
                if (v.recoveryScore >= REPAIR_RECOVERY_STRONG
                        && v.driftRisk <= REPAIR_DRIFT_STRONG_MAX
                        && v.breakDepth <= REPAIR_BREAK_STRONG_MAX) {
                    return AppConfig.MODE_RECOVERING;
                }
            }
            return rawMode;
        }

        if (AppConfig.MODE_CHAOS.equals(rawMode)
                || AppConfig.MODE_FORMING.equals(rawMode)
                || rawMode.length() == 0) {
            if (v.trendLife >= REPAIR_TREND_LIFE_STRONG
                    && v.driftRisk <= REPAIR_DRIFT_STRONG_MAX
                    && v.breakDepth <= REPAIR_BREAK_STRONG_MAX
                    && v.recoveryScore >= REPAIR_RECOVERY_STRONG) {
                return AppConfig.MODE_CONFIRMED;
            }

            if (v.recoveryScore >= REPAIR_RECOVERY_MIN && v.breakDepth <= REPAIR_BREAK_MAX) {
                return AppConfig.MODE_RECOVERING;
            }

            return AppConfig.MODE_CONFIRMED;
        }

        if (AppConfig.MODE_PULLBACK.equals(rawMode) || AppConfig.MODE_WEAKENING.equals(rawMode)) {
            if (v.trendLife >= REPAIR_TREND_LIFE_STRONG
                    && v.recoveryScore >= REPAIR_RECOVERY_STRONG
                    && v.driftRisk <= REPAIR_DRIFT_MAX
                    && v.breakDepth <= REPAIR_BREAK_MAX) {
                return AppConfig.MODE_RECOVERING;
            }
        }

        return rawMode;
    }

    public boolean canEnterBetZone(Models.ShapeVector v) {
        if (v == null) {
            return false;
        }

        if (isHardBlock(v)) {
            return false;
        }

        if (isStrongStructureRepairCandidate(v)) {
            return true;
        }

        if (AppConfig.MODE_CONFIRMED.equals(v.stateMode)) {
            return true;
        }

        if (AppConfig.MODE_ACCELERATING.equals(v.stateMode)) {
            return true;
        }

        if (AppConfig.MODE_RECOVERING.equals(v.stateMode)
                && v.recoveryScore >= AppConfig.RECOVERY_SCORE_STRONG
                && v.breakDepth <= 34.0d
                && v.driftRisk <= 32.0d) {
            return true;
        }

        return false;
    }

    public boolean isHardBlock(Models.ShapeVector v) {
        if (v == null) {
            return true;
        }

        if (isExtremeHardRisk(v)) {
            return true;
        }

        if (AppConfig.MODE_CHAOS.equals(v.stateMode)) {
            return !isStrongStructureRepairCandidate(v);
        }

        if (AppConfig.MODE_BROKEN.equals(v.stateMode)) {
            if (isStrongStructureRepairCandidate(v) && v.recoveryScore >= REPAIR_RECOVERY_STRONG) {
                return false;
            }
            return true;
        }

        if (AppConfig.MODE_REVERSING.equals(v.stateMode)) {
            return true;
        }

        if (v.driftRisk >= AppConfig.DRIFT_RISK_HARD_BLOCK) {
            return true;
        }

        if (v.breakDepth >= 58.0d) {
            return true;
        }

        if (v.hotReverseRisk >= 60.0d) {
            return true;
        }

        if (v.falseStrongRisk >= 62.0d) {
            return true;
        }

        return false;
    }

    public String blockCode(Models.ShapeVector v) {
        if (v == null) {
            return AppConfig.BLOCK_DATA_ERROR;
        }

        if (isStrongStructureRepairCandidate(v) && !isExtremeHardRisk(v)) {
            if (v.championGap < AppConfig.CHAMPION_GAP_MIN
                    && v.comboGap < AppConfig.COMBO_GAP_MIN) {
                return AppConfig.BLOCK_POSITION_WEAK;
            }
            return "";
        }

        if (v.driftRisk >= AppConfig.DRIFT_RISK_HARD_BLOCK) {
            return AppConfig.BLOCK_DRIFT_HIGH;
        }

        if (v.breakDepth >= AppConfig.BREAK_DEPTH_MAX) {
            return AppConfig.BLOCK_BREAK_HIGH;
        }

        if (v.trendLife < AppConfig.TREND_LIFE_MIN) {
            return AppConfig.BLOCK_TREND_WEAK;
        }

        if (v.recoveryScore < AppConfig.RECOVERY_SCORE_MIN
                && (AppConfig.MODE_PULLBACK.equals(v.stateMode)
                        || AppConfig.MODE_WEAKENING.equals(v.stateMode)
                        || AppConfig.MODE_BROKEN.equals(v.stateMode))) {
            return AppConfig.BLOCK_RECOVERY_WEAK;
        }

        if (v.championGap < AppConfig.CHAMPION_GAP_MIN) {
            return AppConfig.BLOCK_POSITION_WEAK;
        }

        if (v.comboGap < AppConfig.COMBO_GAP_MIN) {
            return AppConfig.BLOCK_COMBO_WEAK;
        }

        return "";
    }

    public double shapeQualityScore(Models.ShapeVector v) {
        if (v == null) {
            return 0.0d;
        }

        double score = 0.0d;

        score += v.trendLife * 0.24d;
        score += v.recoveryScore * 0.18d;
        score += (100.0d - v.driftRisk) * 0.20d;
        score += (100.0d - v.breakDepth) * 0.16d;
        score += v.top3Mass64 * 100.0d * 0.08d;
        score += v.championGap * 100.0d * 0.08d;
        score += v.comboGap * 100.0d * 0.06d;

        if (AppConfig.MODE_ACCELERATING.equals(v.stateMode)) {
            score += 6.0d;
        } else if (AppConfig.MODE_CONFIRMED.equals(v.stateMode)) {
            score += 5.0d;
        } else if (AppConfig.MODE_RECOVERING.equals(v.stateMode)) {
            score += 4.0d;
        } else if (AppConfig.MODE_FORMING.equals(v.stateMode)) {
            score += 1.0d;
        } else if (AppConfig.MODE_PULLBACK.equals(v.stateMode)) {
            score -= 5.0d;
        } else if (AppConfig.MODE_WEAKENING.equals(v.stateMode)) {
            score -= 8.0d;
        } else if (AppConfig.MODE_BROKEN.equals(v.stateMode)) {
            score -= 18.0d;
        } else if (AppConfig.MODE_REVERSING.equals(v.stateMode)) {
            score -= 22.0d;
        } else if (AppConfig.MODE_CHAOS.equals(v.stateMode)) {
            if (isStrongStructureRepairCandidate(v)) {
                score -= 3.0d;
            } else {
                score -= 25.0d;
            }
        }

        if (isStrongStructureRepairCandidate(v)) {
            score += 8.0d;

            if (v.trendLife >= REPAIR_TREND_LIFE_STRONG
                    && v.recoveryScore >= REPAIR_RECOVERY_STRONG) {
                score += 4.0d;
            }
        }

        if (v.hotReverseRisk > AppConfig.HOT_REVERSE_RISK_MAX) {
            score -= (v.hotReverseRisk - AppConfig.HOT_REVERSE_RISK_MAX) * 0.45d;
        }

        if (v.falseStrongRisk > AppConfig.FALSE_STRONG_RISK_MAX) {
            score -= (v.falseStrongRisk - AppConfig.FALSE_STRONG_RISK_MAX) * 0.40d;
        }

        if (v.entropy16 >= 90.0d && !isStrongStructureRepairCandidate(v)) {
            score -= (v.entropy16 - 90.0d) * 0.70d;
        }

        return ShapeMath.clamp100(score);
    }

    public double betReadinessScore(Models.ShapeVector v) {
        if (v == null) {
            return 0.0d;
        }

        double score = shapeQualityScore(v);

        if (v.trendLife >= AppConfig.TREND_LIFE_HIGH) {
            score += 4.0d;
        }

        if (v.recoveryScore >= AppConfig.RECOVERY_SCORE_STRONG) {
            score += 3.0d;
        }

        if (v.driftRisk <= 25.0d) {
            score += 3.0d;
        }

        if (v.breakDepth <= 25.0d) {
            score += 3.0d;
        }

        if (v.championGap >= AppConfig.CHAMPION_GAP_HIGH) {
            score += 2.0d;
        }

        if (v.comboGap >= AppConfig.COMBO_GAP_HIGH) {
            score += 2.0d;
        }

        if (isStrongStructureRepairCandidate(v)) {
            score += 8.0d;
        }

        if (isHardBlock(v)) {
            score = Math.min(score, 45.0d);
        }

        return ShapeMath.clamp100(score);
    }

    public double stateGateAdjustment(Models.ShapeVector v) {
        /* B06: compatibility API only; evidence modules cannot own the formal gate. */
        return 0.0d;
    }

    public double riskPenalty(Models.ShapeVector v) {
        if (v == null) {
            return 0.08d;
        }

        double penalty = 0.0d;

        if (v.driftRisk > AppConfig.DRIFT_RISK_MAX) {
            penalty += (v.driftRisk - AppConfig.DRIFT_RISK_MAX) / 100.0d * 0.70d;
        }

        if (v.breakDepth > AppConfig.BREAK_DEPTH_MAX) {
            penalty += (v.breakDepth - AppConfig.BREAK_DEPTH_MAX) / 100.0d * 0.60d;
        }

        if (v.hotReverseRisk > AppConfig.HOT_REVERSE_RISK_MAX) {
            penalty += (v.hotReverseRisk - AppConfig.HOT_REVERSE_RISK_MAX) / 100.0d * 0.55d;
        }

        if (v.falseStrongRisk > AppConfig.FALSE_STRONG_RISK_MAX) {
            penalty += (v.falseStrongRisk - AppConfig.FALSE_STRONG_RISK_MAX) / 100.0d * 0.50d;
        }

        if (v.entropy16 > 88.0d && !isStrongStructureRepairCandidate(v)) {
            penalty += (v.entropy16 - 88.0d) / 100.0d * 0.35d;
        }

        if (isStrongStructureRepairCandidate(v)) {
            penalty *= 0.45d;
        }

        if (penalty < 0.0d) {
            penalty = 0.0d;
        }

        if (penalty > 0.12d) {
            penalty = 0.12d;
        }

        return penalty;
    }

    public String explainState(Models.ShapeVector v) {
        if (v == null) {
            return "形态为空，无法判断";
        }

        StringBuilder sb = new StringBuilder();

        sb.append("状态=");
        sb.append(v.stateMode == null ? AppConfig.MODE_CHAOS : v.stateMode);

        sb.append("，TrendLife=");
        sb.append(UiFormatter.score(v.trendLife));

        sb.append("，DriftRisk=");
        sb.append(UiFormatter.score(v.driftRisk));

        sb.append("，BreakDepth=");
        sb.append(UiFormatter.score(v.breakDepth));

        sb.append("，Recovery=");
        sb.append(UiFormatter.score(v.recoveryScore));

        sb.append("，ChampionGap=");
        sb.append(UiFormatter.probability(v.championGap));

        sb.append("，ComboGap=");
        sb.append(UiFormatter.probability(v.comboGap));

        if (isStrongStructureRepairCandidate(v)) {
            sb.append("，强结构修正=是");
        }

        if (isHardBlock(v)) {
            sb.append("，结论=强制保护HOLD");
        } else if (canEnterBetZone(v)) {
            sb.append("，结论=允许进入BET候选");
        } else {
            sb.append("，结论=观察或继续HOLD");
        }

        return sb.toString();
    }

    public String shortChineseState(String mode) {
        if (AppConfig.MODE_FORMING.equals(mode)) {
            return "成形";
        }
        if (AppConfig.MODE_CONFIRMED.equals(mode)) {
            return "确认";
        }
        if (AppConfig.MODE_ACCELERATING.equals(mode)) {
            return "加速";
        }
        if (AppConfig.MODE_PULLBACK.equals(mode)) {
            return "回调";
        }
        if (AppConfig.MODE_RECOVERING.equals(mode)) {
            return "恢复";
        }
        if (AppConfig.MODE_WEAKENING.equals(mode)) {
            return "转弱";
        }
        if (AppConfig.MODE_BROKEN.equals(mode)) {
            return "破位";
        }
        if (AppConfig.MODE_REVERSING.equals(mode)) {
            return "反转";
        }
        return "混乱";
    }

    public Models.ShapeVector pickBestState(List<Models.ShapeVector> vectors) {
        if (vectors == null || vectors.size() == 0) {
            return null;
        }

        Models.ShapeVector best = null;
        double bestScore = -1.0d;

        for (int i = 0; i < vectors.size(); i++) {
            Models.ShapeVector v = vectors.get(i);
            if (v == null) {
                continue;
            }

            double score = betReadinessScore(v);

            if (best == null || score > bestScore) {
                best = v;
                bestScore = score;
            }
        }

        return best;
    }

    public double bestSecondGap(List<Models.ShapeVector> vectors) {
        if (vectors == null || vectors.size() < 2) {
            return 0.0d;
        }

        double first = -1.0d;
        double second = -1.0d;

        for (int i = 0; i < vectors.size(); i++) {
            Models.ShapeVector v = vectors.get(i);
            if (v == null) {
                continue;
            }

            double score = betReadinessScore(v);

            if (score > first) {
                second = first;
                first = score;
            } else if (score > second) {
                second = score;
            }
        }

        if (first < 0.0d || second < 0.0d) {
            return 0.0d;
        }

        return first - second;
    }

    private boolean isDataChaos(Models.ShapeVector v) {
        if (v == null) {
            return true;
        }

        if (!AppConfig.isValidPosition(v.position)) {
            return true;
        }

        if (v.numberProbs == null || v.numberProbs.length < AppConfig.NUMBER_COUNT + 1) {
            return true;
        }

        if (isStrongStructureRepairCandidate(v)) {
            return false;
        }

        if (v.entropy16 >= 94.0d && v.entropy64 >= 92.0d) {
            return true;
        }

        if (v.driftRisk >= 65.0d) {
            return true;
        }

        if (v.top3Mass64 <= 0.315d && v.championGap <= 0.010d) {
            return true;
        }

        return false;
    }

    private boolean isHardReverse(Models.ShapeVector v) {
        if (v == null) {
            return false;
        }

        if (isStrongStructureRepairCandidate(v)
                && v.hotReverseRisk < 70.0d
                && v.falseStrongRisk < 74.0d) {
            return false;
        }

        if (v.hotReverseRisk >= 62.0d) {
            return true;
        }

        if (v.falseStrongRisk >= 68.0d) {
            return true;
        }

        if (v.slopeShort < -0.030d && v.driftRisk >= 45.0d && v.breakDepth >= 45.0d) {
            return true;
        }

        return false;
    }

    private boolean isHardBroken(Models.ShapeVector v) {
        if (v == null) {
            return false;
        }

        if (isStrongStructureRepairCandidate(v)
                && v.recoveryScore >= REPAIR_RECOVERY_MIN
                && v.breakDepth < 54.0d) {
            return false;
        }

        if (v.breakDepth >= 58.0d) {
            return true;
        }

        if (v.trendLife <= 55.0d && v.driftRisk >= 42.0d && v.recoveryScore <= 52.0d) {
            return true;
        }

        if (v.slopeShort < -0.035d
                && v.slopeMid < -0.015d
                && v.top3Mass16 < v.top3Mass240 - 0.060d) {
            return true;
        }

        return false;
    }

    private boolean isAccelerating(Models.ShapeVector v) {
        if (v == null) {
            return false;
        }

        return v.trendLife >= AppConfig.TREND_LIFE_HIGH
                && v.recoveryScore >= AppConfig.RECOVERY_SCORE_STRONG
                && v.driftRisk <= 30.0d
                && v.breakDepth <= 30.0d
                && v.slopeShort >= 0.010d
                && v.top3Mass64 >= 0.355d
                && v.championGap >= AppConfig.CHAMPION_GAP_MIN
                && v.hotReverseRisk <= AppConfig.HOT_REVERSE_RISK_MAX;
    }

    private boolean isConfirmed(Models.ShapeVector v) {
        if (v == null) {
            return false;
        }

        if (v.trendLife >= AppConfig.TREND_LIFE_MIN
                && v.driftRisk <= AppConfig.DRIFT_RISK_MAX
                && v.breakDepth <= AppConfig.BREAK_DEPTH_MAX
                && v.recoveryScore >= 64.0d
                && v.top3Mass64 >= 0.345d
                && v.championGap >= 0.018d
                && v.falseStrongRisk <= AppConfig.FALSE_STRONG_RISK_MAX + 8.0d) {
            return true;
        }

        if (isStrongStructureRepairCandidate(v)
                && v.trendLife >= REPAIR_TREND_LIFE_STRONG
                && v.recoveryScore >= REPAIR_RECOVERY_STRONG
                && v.driftRisk <= REPAIR_DRIFT_STRONG_MAX
                && v.breakDepth <= REPAIR_BREAK_STRONG_MAX) {
            return true;
        }

        return false;
    }

    private boolean isRecovering(Models.ShapeVector v) {
        if (v == null) {
            return false;
        }

        if (v.breakDepth >= 24.0d
                && v.breakDepth <= 48.0d
                && v.recoveryScore >= AppConfig.RECOVERY_SCORE_MIN
                && v.slopeShort > 0.0d
                && v.driftRisk <= 40.0d
                && v.hotReverseRisk <= 50.0d) {
            return true;
        }

        if (isStrongStructureRepairCandidate(v)
                && v.recoveryScore >= REPAIR_RECOVERY_MIN
                && v.breakDepth <= REPAIR_BREAK_MAX) {
            return true;
        }

        return false;
    }

    private boolean isWeakening(Models.ShapeVector v) {
        if (v == null) {
            return false;
        }

        if (isStrongStructureRepairCandidate(v)) {
            return false;
        }

        return v.slopeShort < -0.012d
                && v.slopeMid <= 0.0d
                && v.recoveryScore < AppConfig.RECOVERY_SCORE_MIN
                && v.breakDepth >= 30.0d;
    }

    private boolean isPullback(Models.ShapeVector v) {
        if (v == null) {
            return false;
        }

        if (isStrongStructureRepairCandidate(v)
                && v.recoveryScore >= REPAIR_RECOVERY_MIN
                && v.breakDepth <= REPAIR_BREAK_MAX) {
            return false;
        }

        return v.slopeShort < 0.0d
                && v.breakDepth >= 22.0d
                && v.breakDepth < 50.0d
                && v.driftRisk <= 45.0d;
    }

    private boolean isStrongStructureRepairCandidate(Models.ShapeVector v) {
        if (v == null) {
            return false;
        }

        if (!AppConfig.isValidPosition(v.position)) {
            return false;
        }

        if (v.trendLife < REPAIR_TREND_LIFE_MIN) {
            return false;
        }

        if (v.driftRisk > REPAIR_DRIFT_MAX) {
            return false;
        }

        if (v.breakDepth > REPAIR_BREAK_MAX) {
            return false;
        }

        if (v.recoveryScore < REPAIR_RECOVERY_MIN) {
            return false;
        }

        if (v.hotReverseRisk >= 70.0d) {
            return false;
        }

        if (v.falseStrongRisk >= 74.0d) {
            return false;
        }

        return true;
    }

    private boolean isExtremeHardRisk(Models.ShapeVector v) {
        if (v == null) {
            return true;
        }

        if (v.driftRisk >= AppConfig.DRIFT_RISK_HARD_BLOCK) {
            return true;
        }

        if (v.breakDepth >= 58.0d) {
            return true;
        }

        if (v.hotReverseRisk >= 70.0d) {
            return true;
        }

        if (v.falseStrongRisk >= 74.0d) {
            return true;
        }

        if (v.trendLife <= 45.0d && v.recoveryScore <= 45.0d) {
            return true;
        }

        return false;
    }

    private String appendShapeKey(String oldKey, String tag) {
        String key = oldKey == null ? "" : oldKey;
        String t = tag == null ? "" : tag;

        if (t.length() == 0) {
            return key;
        }

        if (key.indexOf(t) >= 0) {
            return key;
        }

        if (key.length() == 0) {
            return t;
        }

        return key + "|" + t;
    }

    private boolean safeEquals(String a, String b) {
        String x = a == null ? "" : a;
        String y = b == null ? "" : b;
        return x.equals(y);
    }

    public B06ShapeStateEvidence buildB06Evidence(Models.ShapeVector v) {
        B06ShapeStateEvidence e = new B06ShapeStateEvidence();
        if (v == null) {
            e.reason = "形态向量为空；证据不可用，不直接HOLD/BET";
            e.fingerprint = V51ChecksumEngine.sha256(e.stablePayload());
            return e;
        }
        e.baseIssue = safeText(v.baseIssue);
        e.targetIssue = safeText(v.targetIssue);
        e.position = v.position;
        e.stateMode = safeText(v.stateMode);
        e.shapeKey = safeText(v.shapeKey);
        e.support =
                clamp01(
                        normalizePercent(v.trendLife) * 0.22d
                                + normalizePercent(v.recoveryScore) * 0.18d
                                + clamp01(v.markov1Support) * 0.12d
                                + clamp01(v.markov2Support) * 0.10d
                                + clamp01(v.top3Mass32) * 0.18d
                                + clamp01(v.championGap * 8.0d) * 0.20d);
        e.counterEvidence =
                clamp01(
                        normalizePercent(v.driftRisk) * 0.30d
                                + normalizePercent(v.breakDepth) * 0.30d
                                + normalizePercent(v.hotReverseRisk) * 0.20d
                                + normalizePercent(v.falseStrongRisk) * 0.20d);
        e.risk =
                clamp01(
                        e.counterEvidence * 0.70d
                                + clamp01(Math.abs(v.driftShort - v.driftLong)) * 0.15d
                                + clamp01(Math.abs(v.slopeShort - v.slopeLong)) * 0.15d);
        e.confidence =
                clamp01(
                        (1.0d - e.risk) * 0.35d
                                + clamp01(v.top6Mass240) * 0.20d
                                + clamp01(v.markov1Support + v.markov2Support) * 0.20d
                                + clamp01(normalizePercent(v.trendLife)) * 0.25d);
        e.freshness = clamp01(1.0d - Math.min(1.0d, Math.abs(v.slopeShort - v.slopeMid)));
        e.sampleMaturity = clamp01(Math.max(v.top3Mass240, v.top6Mass240));
        e.evidenceReady = AppConfig.isValidPosition(v.position) && e.confidence >= 0.35d ? 1 : 0;
        e.ablationGroup = "B06_SHAPE_STATE";
        e.reason = "支持来自趋势寿命/恢复/Markov/集中度，反证来自漂移/破位/反热/假强；只写审计指纹";
        e.fingerprint = V51ChecksumEngine.sha256(e.stablePayload());
        return e;
    }

    public String explainB06Evidence(Models.ShapeVector v) {
        B06ShapeStateEvidence e = buildB06Evidence(v);
        return "B06形态状态证据：支持="
                + UiFormatter.probability(e.support)
                + "，反证="
                + UiFormatter.probability(e.counterEvidence)
                + "，风险="
                + UiFormatter.probability(e.risk)
                + "，置信="
                + UiFormatter.probability(e.confidence)
                + "，所有权=EvidenceOnly，hash="
                + shortHash(e.fingerprint);
    }

    private double normalizePercent(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) return 0.0d;
        if (value > 1.5d) return clamp01(value / 100.0d);
        return clamp01(value);
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

    public B06ShapeStateAblation auditB06Ablation(Models.ShapeVector v) {
        B06ShapeStateAblation a = new B06ShapeStateAblation();
        B06ShapeStateEvidence e = buildB06Evidence(v);
        a.baseIssue = e.baseIssue;
        a.targetIssue = e.targetIssue;
        a.position = e.position;
        a.fullSupport = e.support;
        a.fullCounterEvidence = e.counterEvidence;
        if (v == null) {
            a.status = "NO_VECTOR";
            a.reason = "形态向量为空，不能进行消融";
            a.fingerprint = V51ChecksumEngine.sha256(a.stablePayload());
            return a;
        }
        a.trendContribution = clamp01(normalizePercent(v.trendLife) * 0.22d);
        a.recoveryContribution = clamp01(normalizePercent(v.recoveryScore) * 0.18d);
        a.markovContribution =
                clamp01(clamp01(v.markov1Support) * 0.12d + clamp01(v.markov2Support) * 0.10d);
        a.massContribution =
                clamp01(clamp01(v.top3Mass32) * 0.18d + clamp01(v.championGap * 8.0d) * 0.20d);
        a.driftCounter = clamp01(normalizePercent(v.driftRisk) * 0.30d);
        a.breakCounter = clamp01(normalizePercent(v.breakDepth) * 0.30d);
        a.reverseCounter =
                clamp01(
                        normalizePercent(v.hotReverseRisk) * 0.20d
                                + normalizePercent(v.falseStrongRisk) * 0.20d);
        double positive =
                a.trendContribution
                        + a.recoveryContribution
                        + a.markovContribution
                        + a.massContribution;
        double largest =
                Math.max(
                        Math.max(a.trendContribution, a.recoveryContribution),
                        Math.max(a.markovContribution, a.massContribution));
        a.concentration = positive <= 0.0d ? 1.0d : clamp01(largest / positive);
        a.correlationPenalty =
                clamp01(
                        Math.min(a.trendContribution, a.recoveryContribution) * 0.22d
                                + Math.min(a.markovContribution, a.massContribution) * 0.18d
                                + clamp01(Math.abs(v.slopeShort - v.slopeMid)) * 0.10d);
        a.independentSupport = clamp01(e.support - a.correlationPenalty);
        a.leaveTrendOut = clamp01(e.support - a.trendContribution);
        a.leaveRecoveryOut = clamp01(e.support - a.recoveryContribution);
        a.leaveMarkovOut = clamp01(e.support - a.markovContribution);
        a.leaveMassOut = clamp01(e.support - a.massContribution);
        a.minimumLeaveOneOut =
                Math.min(
                        Math.min(a.leaveTrendOut, a.leaveRecoveryOut),
                        Math.min(a.leaveMarkovOut, a.leaveMassOut));
        a.fragility =
                clamp01(
                        (e.support - a.minimumLeaveOneOut) * 1.35d
                                + a.concentration * 0.35d
                                + a.correlationPenalty * 0.30d);
        if (e.evidenceReady == 0) {
            a.status = "INSUFFICIENT";
            a.reason = "样本、置信或位置合同不足；不得进入正式概率";
        } else if (a.fragility >= 0.68d || a.concentration >= 0.62d) {
            a.status = "DOWNWEIGHT";
            a.reason = "证据过度集中或消融脆弱，应降权并等待跨块复现";
        } else if (a.independentSupport <= e.counterEvidence) {
            a.status = "FREEZE";
            a.reason = "去相关后的独立支持不高于反证，冻结该形态证据";
        } else {
            a.status = "KEEP_EVIDENCE";
            a.reason = "去相关后仍有独立支持；仅作为Evidence交给统一所有者";
        }
        a.fingerprint = V51ChecksumEngine.sha256(a.stablePayload());
        return a;
    }

    public static final class B06ShapeStateAblation {
        public String contractVersion = B06_CONTRACT_VERSION;
        public String owner = B06_OWNER;
        public String baseIssue = "";
        public String targetIssue = "";
        public int position = 0;
        public double fullSupport = 0.0d;
        public double fullCounterEvidence = 1.0d;
        public double trendContribution = 0.0d;
        public double recoveryContribution = 0.0d;
        public double markovContribution = 0.0d;
        public double massContribution = 0.0d;
        public double driftCounter = 0.0d;
        public double breakCounter = 0.0d;
        public double reverseCounter = 0.0d;
        public double correlationPenalty = 0.0d;
        public double independentSupport = 0.0d;
        public double concentration = 1.0d;
        public double leaveTrendOut = 0.0d;
        public double leaveRecoveryOut = 0.0d;
        public double leaveMarkovOut = 0.0d;
        public double leaveMassOut = 0.0d;
        public double minimumLeaveOneOut = 0.0d;
        public double fragility = 1.0d;
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
                    + position
                    + "|"
                    + Double.doubleToLongBits(fullSupport)
                    + "|"
                    + Double.doubleToLongBits(fullCounterEvidence)
                    + "|"
                    + Double.doubleToLongBits(trendContribution)
                    + "|"
                    + Double.doubleToLongBits(recoveryContribution)
                    + "|"
                    + Double.doubleToLongBits(markovContribution)
                    + "|"
                    + Double.doubleToLongBits(massContribution)
                    + "|"
                    + Double.doubleToLongBits(correlationPenalty)
                    + "|"
                    + Double.doubleToLongBits(independentSupport)
                    + "|"
                    + Double.doubleToLongBits(concentration)
                    + "|"
                    + Double.doubleToLongBits(minimumLeaveOneOut)
                    + "|"
                    + Double.doubleToLongBits(fragility)
                    + "|"
                    + status
                    + "|"
                    + reason;
        }
    }

    public static final class B06ShapeStateEvidence {
        public String contractVersion = B06_CONTRACT_VERSION;
        public String owner = B06_OWNER;
        public String baseIssue = "";
        public String targetIssue = "";
        public int position = 0;
        public String stateMode = AppConfig.MODE_CHAOS;
        public String shapeKey = "";
        public double support = 0.0d;
        public double counterEvidence = 1.0d;
        public double risk = 1.0d;
        public double confidence = 0.0d;
        public double freshness = 0.0d;
        public double sampleMaturity = 0.0d;
        public int evidenceReady = 0;
        public String ablationGroup = "B06_SHAPE_STATE";
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
                    + stateMode
                    + "|"
                    + shapeKey
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
                    + Double.doubleToLongBits(sampleMaturity)
                    + "|"
                    + evidenceReady
                    + "|"
                    + ablationGroup;
        }
    }
}
