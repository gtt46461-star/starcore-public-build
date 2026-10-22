package com.ethk3.novaonepro;

import java.lang.reflect.Field;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * PreDrawReasonEngine.java
 *
 * <p>第6/17 新增核心引擎。
 *
 * <p>作用： 1. 把 PreDrawFeatureEngine 生成的开奖前数值特征，转换成开奖前原因。 2. 只生成原因，不出号、不结算、不创建BET、不修改真实盈亏。 3.
 * 原因必须在开奖前生成，开奖后不得修改 reason_fingerprint。 4. 输出 Map<String, Models.PreDrawReason>，key 固定为 position +
 * "_" + number。
 *
 * <p>必须生成： reason_atoms positive_atoms negative_atoms contradiction_atoms reason_fingerprint
 * reason_text_pre_draw
 *
 * <p>正因： LOW_DRIFT POS_UP POSITION_CONVERGING GAP_RETURN_VALID TRANSITION_SUPPORT RECOVERY_VALID
 * MID_STABLE ENTROPY_STABLE_OR_DOWN
 *
 * <p>反因： DRIFT_RISING POS_DECAY POSITION_NOISE_HIGH GAP_OVERDUE_NOISE TRANSITION_CONFLICT
 * FALSE_RECOVERY HOT_EXHAUSTED ENTROPY_RISING
 */
public final class PreDrawReasonEngine {

    public static final String POS_LOW_DRIFT = "LOW_DRIFT";
    public static final String POS_POS_UP = "POS_UP";
    public static final String POS_POSITION_CONVERGING = "POSITION_CONVERGING";
    public static final String POS_GAP_RETURN_VALID = "GAP_RETURN_VALID";
    public static final String POS_TRANSITION_SUPPORT = "TRANSITION_SUPPORT";
    public static final String POS_RECOVERY_VALID = "RECOVERY_VALID";
    public static final String POS_MID_STABLE = "MID_STABLE";
    public static final String POS_ENTROPY_STABLE_OR_DOWN = "ENTROPY_STABLE_OR_DOWN";

    public static final String NEG_DRIFT_RISING = "DRIFT_RISING";
    public static final String NEG_POS_DECAY = "POS_DECAY";
    public static final String NEG_POSITION_NOISE_HIGH = "POSITION_NOISE_HIGH";
    public static final String NEG_GAP_OVERDUE_NOISE = "GAP_OVERDUE_NOISE";
    public static final String NEG_TRANSITION_CONFLICT = "TRANSITION_CONFLICT";
    public static final String NEG_FALSE_RECOVERY = "FALSE_RECOVERY";
    public static final String NEG_HOT_EXHAUSTED = "HOT_EXHAUSTED";
    public static final String NEG_ENTROPY_RISING = "ENTROPY_RISING";

    private static final String LEVEL_STRONG = "STRONG";
    private static final String LEVEL_NORMAL = "NORMAL";
    private static final String LEVEL_WEAK = "WEAK";
    private static final String LEVEL_RISK = "RISK";

    private static final double EPS = 0.0000001d;

    public PreDrawReasonEngine() {}

    public Map<String, Models.PreDrawReason> buildReasons(
            Map<String, Models.PreDrawFeature> features) {
        return buildReasons("", "", features);
    }

    public Map<String, Models.PreDrawReason> buildReasons(
            String baseIssue, String targetIssue, Map<String, Models.PreDrawFeature> features) {
        LinkedHashMap<String, Models.PreDrawReason> out =
                new LinkedHashMap<String, Models.PreDrawReason>();

        if (features == null || features.size() == 0) {
            return out;
        }

        ArrayList<Models.PreDrawFeature> list = new ArrayList<Models.PreDrawFeature>();

        for (Models.PreDrawFeature f : features.values()) {
            if (f != null) {
                list.add(f);
            }
        }

        Collections.sort(
                list,
                new Comparator<Models.PreDrawFeature>() {
                    public int compare(Models.PreDrawFeature a, Models.PreDrawFeature b) {
                        int pa = getInt(a, "position", 0);
                        int pb = getInt(b, "position", 0);

                        if (pa != pb) {
                            return pa - pb;
                        }

                        int na = getInt(a, "number", 0);
                        int nb = getInt(b, "number", 0);

                        return na - nb;
                    }
                });

        for (int i = 0; i < list.size(); i++) {
            Models.PreDrawFeature f = list.get(i);

            if (f == null) {
                continue;
            }

            Models.PreDrawReason r = buildReason(baseIssue, targetIssue, f);
            out.put(keyOf(getInt(r, "position", 0), getInt(r, "number", 0)), r);
        }

        return out;
    }

    public ArrayList<Models.PreDrawReason> buildReasonList(
            String baseIssue, String targetIssue, Map<String, Models.PreDrawFeature> features) {
        Map<String, Models.PreDrawReason> map = buildReasons(baseIssue, targetIssue, features);
        return toSortedReasonList(map);
    }

    public ArrayList<Models.PreDrawReason> buildReasonList(
            Map<String, Models.PreDrawFeature> features) {
        return buildReasonList("", "", features);
    }

    public Models.PreDrawReason buildReason(Models.PreDrawFeature feature) {
        return buildReason("", "", feature);
    }

    public Models.PreDrawReason buildReason(
            String baseIssue, String targetIssue, Models.PreDrawFeature feature) {
        Models.PreDrawReason r = new Models.PreDrawReason();

        if (feature == null) {
            putString(r, "reasonTextPreDraw", "开奖前原因生成失败：PreDrawFeature为空");
            putString(r, "reasonFingerprint", "EMPTY_FEATURE");
            putString(r, "reasonAtoms", "");
            putString(r, "positiveAtoms", "");
            putString(r, "negativeAtoms", "");
            putString(r, "contradictionAtoms", "");
            putString(r, "validLevel", LEVEL_RISK);
            return r;
        }

        int position = getInt(feature, "position", 0);
        int number = getInt(feature, "number", 0);

        String realBaseIssue = firstNonEmpty(baseIssue, getString(feature, "baseIssue", ""));
        String realTargetIssue = firstNonEmpty(targetIssue, getString(feature, "targetIssue", ""));

        ReasonBag bag = analyzeFeature(feature);

        double positiveScore = bag.positiveScore;
        double negativeScore = bag.negativeScore;
        double contradictionScore = bag.contradictionScore;
        double reasonEdge = clampSigned(positiveScore - negativeScore - contradictionScore * 0.35d);
        double reasonScore = clamp01(0.50d + reasonEdge * 0.50d);

        int validReasonCount = bag.positiveAtoms.size();
        int falseReasonCount = bag.negativeAtoms.size() + bag.contradictionAtoms.size();

        String validLevel =
                judgeValidLevel(
                        positiveScore,
                        negativeScore,
                        contradictionScore,
                        validReasonCount,
                        falseReasonCount,
                        feature);

        String positiveJoined = joinAtoms(bag.positiveAtoms);
        String negativeJoined = joinAtoms(bag.negativeAtoms);
        String contradictionJoined = joinAtoms(bag.contradictionAtoms);
        String allAtoms = joinAtoms(bag.reasonAtoms);

        String fingerprint =
                buildFingerprint(
                        position,
                        number,
                        bag.positiveAtoms,
                        bag.negativeAtoms,
                        bag.contradictionAtoms,
                        validLevel);

        String text =
                buildReasonText(
                        realTargetIssue,
                        position,
                        number,
                        feature,
                        bag,
                        validLevel,
                        reasonScore,
                        reasonEdge);

        putString(r, "reasonKey", keyOf(position, number));
        putString(r, "featureKey", getString(feature, "featureKey", keyOf(position, number)));
        putString(r, "baseIssue", realBaseIssue);
        putString(r, "targetIssue", realTargetIssue);
        putString(r, "stateKey", getString(feature, "stateKey", ""));
        putString(r, "reasonFingerprint", fingerprint);
        putString(r, "reasonTextPreDraw", text);
        putString(r, "reasonText", text);
        putString(
                r,
                "reasonSummary",
                buildReasonSummary(
                        position,
                        number,
                        validLevel,
                        reasonEdge,
                        validReasonCount,
                        falseReasonCount));
        putString(r, "reasonAtoms", allAtoms);
        putString(r, "positiveAtoms", positiveJoined);
        putString(r, "negativeAtoms", negativeJoined);
        putString(r, "contradictionAtoms", contradictionJoined);
        putString(r, "validLevel", validLevel);
        putString(r, "mainPositiveAtom", bag.mainPositiveAtom);
        putString(r, "mainNegativeAtom", bag.mainNegativeAtom);
        putString(r, "mainContradictionAtom", bag.mainContradictionAtom);
        putString(r, "sourceType", "PREDRAW_REASON");

        putInt(r, "position", position);
        putInt(r, "number", number);
        putLong(r, "createdAt", System.currentTimeMillis());
        putInt(r, "validReasonCount", validReasonCount);
        putInt(r, "falseReasonCount", falseReasonCount);
        putInt(r, "positiveCount", bag.positiveAtoms.size());
        putInt(r, "negativeCount", bag.negativeAtoms.size());
        putInt(r, "contradictionCount", bag.contradictionAtoms.size());
        putInt(r, "noFutureLeakOk", 1);

        putDouble(r, "reasonScore", reasonScore);
        putDouble(r, "reasonEdge", reasonEdge);
        putDouble(r, "positiveScore", positiveScore);
        putDouble(r, "negativeScore", negativeScore);
        putDouble(r, "contradictionScore", contradictionScore);
        putDouble(r, "confidence", calcReasonConfidence(feature, bag));
        putDouble(r, "hitSimilarity", 0.0d);
        putDouble(r, "missSimilarity", 0.0d);
        putDouble(r, "causalLift", 0.0d);
        putDouble(r, "causalConfidence", 0.0d);
        putDouble(r, "forwardReasonHitRate", 0.0d);
        putDouble(r, "reasonWilsonScore", 0.0d);

        copyFeatureFields(feature, r);

        return r;
    }

    public static String keyOf(int position, int number) {
        return String.valueOf(position) + "_" + String.valueOf(number);
    }

    public static ArrayList<Models.PreDrawReason> toSortedReasonList(
            Map<String, Models.PreDrawReason> map) {
        ArrayList<Models.PreDrawReason> list = new ArrayList<Models.PreDrawReason>();

        if (map == null || map.size() == 0) {
            return list;
        }

        for (Models.PreDrawReason r : map.values()) {
            if (r != null) {
                list.add(r);
            }
        }

        Collections.sort(
                list,
                new Comparator<Models.PreDrawReason>() {
                    public int compare(Models.PreDrawReason a, Models.PreDrawReason b) {
                        double ea = getDouble(a, "reasonEdge", 0.0d);
                        double eb = getDouble(b, "reasonEdge", 0.0d);

                        if (ea > eb) {
                            return -1;
                        }

                        if (ea < eb) {
                            return 1;
                        }

                        double sa = getDouble(a, "reasonScore", 0.0d);
                        double sb = getDouble(b, "reasonScore", 0.0d);

                        if (sa > sb) {
                            return -1;
                        }

                        if (sa < sb) {
                            return 1;
                        }

                        int pa = getInt(a, "position", 0);
                        int pb = getInt(b, "position", 0);

                        if (pa != pb) {
                            return pa - pb;
                        }

                        int na = getInt(a, "number", 0);
                        int nb = getInt(b, "number", 0);

                        return na - nb;
                    }
                });

        return list;
    }

    private ReasonBag analyzeFeature(Models.PreDrawFeature f) {
        ReasonBag bag = new ReasonBag();

        double freq16 = getDouble(f, "freq16", 0.0d);
        double freq32 = getDouble(f, "freq32", 0.0d);
        double freq64 = getDouble(f, "freq64", 0.0d);
        double freq120 = getDouble(f, "freq120", 0.0d);
        double freq240 = getDouble(f, "freq240", 0.0d);

        double freqSlope = getDouble(f, "freqSlope", 0.0d);
        double freqAccel = getDouble(f, "freqAccel", 0.0d);
        double shortVsMid = getDouble(f, "shortVsMid", 0.0d);
        double midVsLong = getDouble(f, "midVsLong", 0.0d);
        double trendConsistency = getDouble(f, "trendConsistency", 0.0d);

        double positionEntropy = getDouble(f, "positionEntropy", 1.0d);
        double positionTop3Mass = getDouble(f, "positionTop3Mass", 0.0d);
        double positionStrengthSlope = getDouble(f, "positionStrengthSlope", 0.0d);
        double positionDrift = getDouble(f, "positionDrift", 0.0d);
        double positionNoiseScore = getDouble(f, "positionNoiseScore", 1.0d);

        int currentGap = getInt(f, "currentGap", 0);
        double avgGap = getDouble(f, "avgGap", 10.0d);
        double gapZ = getDouble(f, "gapZ", 0.0d);
        double gapPhase = getDouble(f, "gapPhase", 0.0d);

        double hotPressure = getDouble(f, "hotPressure", 0.0d);
        double hotExhaustion = getDouble(f, "hotExhaustion", 0.0d);
        double coldRepairPower = getDouble(f, "coldRepairPower", 0.0d);
        double returnZoneScore = getDouble(f, "returnZoneScore", 0.0d);

        double transitionLift = getDouble(f, "transitionLift", 0.0d);
        double transitionConflict = getDouble(f, "transitionConflict", 0.0d);
        double pairCoSupport = getDouble(f, "pairCoSupport", 0.0d);
        double pairConflict = getDouble(f, "pairConflict", 0.0d);

        double breakDepth = getDouble(f, "breakDepth", 0.0d);
        double recoverySpeed = getDouble(f, "recoverySpeed", 0.0d);
        double recoveryQuality = getDouble(f, "recoveryQuality", 0.0d);
        double falseRecoveryRisk = getDouble(f, "falseRecoveryRisk", 0.0d);

        if (positionDrift <= 0.22d && positionNoiseScore <= 0.45d) {
            addPositive(bag, POS_LOW_DRIFT, 0.16d + (0.22d - positionDrift) * 0.28d);
        }

        if (freqSlope >= 0.055d || shortVsMid >= 0.035d || freqAccel >= 0.080d) {
            addPositive(
                    bag,
                    POS_POS_UP,
                    0.14d + positivePart(freqSlope) * 0.20d + positivePart(freqAccel) * 0.10d);
        }

        if (positionStrengthSlope >= 0.035d
                && positionTop3Mass >= 0.32d
                && positionNoiseScore <= 0.58d) {
            addPositive(
                    bag,
                    POS_POSITION_CONVERGING,
                    0.13d + positionStrengthSlope * 0.20d + positionTop3Mass * 0.08d);
        }

        if (returnZoneScore >= 0.42d
                && gapZ >= -0.25d
                && gapZ <= 1.80d
                && currentGap >= Math.max(3, (int) Math.round(avgGap * 0.45d))) {
            addPositive(bag, POS_GAP_RETURN_VALID, 0.14d + returnZoneScore * 0.20d);
        }

        if (transitionLift >= 0.18d && transitionConflict <= 0.28d) {
            addPositive(
                    bag,
                    POS_TRANSITION_SUPPORT,
                    0.14d + transitionLift * 0.22d - transitionConflict * 0.08d);
        }

        if (recoveryQuality >= 0.42d && falseRecoveryRisk <= 0.42d && breakDepth <= 0.58d) {
            addPositive(
                    bag,
                    POS_RECOVERY_VALID,
                    0.15d + recoveryQuality * 0.22d - falseRecoveryRisk * 0.10d);
        }

        if (freq64 >= 0.070d
                && freq64 <= 0.155d
                && freq120 >= 0.065d
                && freq240 >= 0.055d
                && Math.abs(midVsLong) <= 0.055d) {
            addPositive(
                    bag,
                    POS_MID_STABLE,
                    0.10d + (1.0d - Math.min(1.0d, Math.abs(midVsLong) * 10.0d)) * 0.08d);
        }

        if (positionEntropy <= 0.94d && positionNoiseScore <= 0.58d && positionDrift <= 0.38d) {
            addPositive(
                    bag,
                    POS_ENTROPY_STABLE_OR_DOWN,
                    0.10d + (1.0d - positionEntropy) * 0.10d + (0.38d - positionDrift) * 0.10d);
        }

        if (positionDrift >= 0.42d || positionNoiseScore >= 0.68d) {
            addNegative(
                    bag,
                    NEG_DRIFT_RISING,
                    0.15d + positionDrift * 0.20d + positionNoiseScore * 0.12d);
        }

        if (freqSlope <= -0.060d || shortVsMid <= -0.040d || freqAccel <= -0.080d) {
            addNegative(
                    bag,
                    NEG_POS_DECAY,
                    0.14d + negativePart(freqSlope) * 0.22d + negativePart(freqAccel) * 0.10d);
        }

        if (positionNoiseScore >= 0.62d || positionEntropy >= 0.965d) {
            addNegative(bag, NEG_POSITION_NOISE_HIGH, 0.13d + positionNoiseScore * 0.18d);
        }

        if (gapZ >= 1.90d && returnZoneScore < 0.35d) {
            addNegative(bag, NEG_GAP_OVERDUE_NOISE, 0.13d + Math.min(0.28d, gapZ * 0.07d));
        }

        if (transitionConflict >= 0.30d && transitionLift < 0.18d) {
            addNegative(bag, NEG_TRANSITION_CONFLICT, 0.13d + transitionConflict * 0.24d);
        }

        if (falseRecoveryRisk >= 0.45d || (recoverySpeed >= 0.35d && recoveryQuality < 0.32d)) {
            addNegative(bag, NEG_FALSE_RECOVERY, 0.14d + falseRecoveryRisk * 0.24d);
        }

        if (hotExhaustion >= 0.45d || (hotPressure >= 0.55d && freqSlope < -0.035d)) {
            addNegative(
                    bag, NEG_HOT_EXHAUSTED, 0.12d + hotExhaustion * 0.22d + hotPressure * 0.08d);
        }

        if (positionEntropy >= 0.970d && positionDrift >= 0.30d) {
            addNegative(
                    bag,
                    NEG_ENTROPY_RISING,
                    0.12d + positionEntropy * 0.08d + positionDrift * 0.16d);
        }

        if (pairConflict >= 0.35d && pairCoSupport < 0.15d) {
            addNegative(bag, "PAIR_CONFLICT", 0.10d + pairConflict * 0.18d);
        }

        if (breakDepth >= 0.62d && recoveryQuality < 0.38d) {
            addNegative(bag, "BREAK_NOT_REPAIRED", 0.12d + breakDepth * 0.18d);
        }

        detectContradictions(bag, f);

        if (bag.positiveAtoms.size() == 0 && bag.negativeAtoms.size() == 0) {
            addNegative(bag, "NO_VALID_PREDRAW_REASON", 0.18d);
        }

        normalizeBag(bag);

        return bag;
    }

    private void detectContradictions(ReasonBag bag, Models.PreDrawFeature f) {
        double freqSlope = getDouble(f, "freqSlope", 0.0d);
        double hotExhaustion = getDouble(f, "hotExhaustion", 0.0d);
        double transitionLift = getDouble(f, "transitionLift", 0.0d);
        double transitionConflict = getDouble(f, "transitionConflict", 0.0d);
        double recoveryQuality = getDouble(f, "recoveryQuality", 0.0d);
        double falseRecoveryRisk = getDouble(f, "falseRecoveryRisk", 0.0d);
        double returnZoneScore = getDouble(f, "returnZoneScore", 0.0d);
        double positionNoiseScore = getDouble(f, "positionNoiseScore", 0.0d);
        double gapZ = getDouble(f, "gapZ", 0.0d);
        double positionDrift = getDouble(f, "positionDrift", 0.0d);

        if (hasAtom(bag.positiveAtoms, POS_POS_UP) && hotExhaustion >= 0.45d) {
            addContradiction(bag, "POS_UP_BUT_HOT_EXHAUSTED", 0.14d + hotExhaustion * 0.16d);
        }

        if (hasAtom(bag.positiveAtoms, POS_TRANSITION_SUPPORT) && transitionConflict >= 0.35d) {
            addContradiction(bag, "TRANS_SUPPORT_BUT_CONFLICT", 0.15d + transitionConflict * 0.18d);
        }

        if (hasAtom(bag.positiveAtoms, POS_RECOVERY_VALID) && falseRecoveryRisk >= 0.42d) {
            addContradiction(
                    bag, "RECOVERY_VALID_BUT_FALSE_RISK", 0.15d + falseRecoveryRisk * 0.20d);
        }

        if (hasAtom(bag.positiveAtoms, POS_GAP_RETURN_VALID)
                && gapZ >= 2.20d
                && returnZoneScore < 0.50d) {
            addContradiction(
                    bag, "GAP_RETURN_BUT_OVERDUE_NOISE", 0.14d + Math.min(0.25d, gapZ * 0.06d));
        }

        if (hasAtom(bag.positiveAtoms, POS_LOW_DRIFT)
                && (positionNoiseScore >= 0.62d || positionDrift >= 0.42d)) {
            addContradiction(
                    bag,
                    "LOW_DRIFT_BUT_POSITION_NOISE",
                    0.14d + Math.max(positionNoiseScore, positionDrift) * 0.16d);
        }

        if (recoveryQuality >= 0.50d && freqSlope <= -0.08d) {
            addContradiction(bag, "RECOVERY_BUT_FREQ_DECAY", 0.13d + Math.abs(freqSlope) * 0.18d);
        }

        if (transitionLift >= 0.25d && positionNoiseScore >= 0.70d) {
            addContradiction(
                    bag, "TRANS_SUPPORT_BUT_POSITION_NOISY", 0.12d + positionNoiseScore * 0.15d);
        }
    }

    private void normalizeBag(ReasonBag bag) {
        if (bag == null) {
            return;
        }

        bag.positiveScore = clamp01(bag.positiveScore);
        bag.negativeScore = clamp01(bag.negativeScore);
        bag.contradictionScore = clamp01(bag.contradictionScore);

        bag.mainPositiveAtom = firstAtom(bag.positiveAtoms);
        bag.mainNegativeAtom = firstAtom(bag.negativeAtoms);
        bag.mainContradictionAtom = firstAtom(bag.contradictionAtoms);
    }

    private String judgeValidLevel(
            double positiveScore,
            double negativeScore,
            double contradictionScore,
            int validReasonCount,
            int falseReasonCount,
            Models.PreDrawFeature f) {
        double reasonEdge = positiveScore - negativeScore - contradictionScore * 0.35d;

        double positionNoiseScore = getDouble(f, "positionNoiseScore", 1.0d);
        double falseRecoveryRisk = getDouble(f, "falseRecoveryRisk", 1.0d);
        double transitionConflict = getDouble(f, "transitionConflict", 0.0d);

        if (falseReasonCount >= 4 || negativeScore >= 0.72d || contradictionScore >= 0.55d) {
            return LEVEL_RISK;
        }

        if (positionNoiseScore >= 0.78d
                || falseRecoveryRisk >= 0.72d
                || transitionConflict >= 0.68d) {
            return LEVEL_RISK;
        }

        if (validReasonCount >= 4
                && reasonEdge >= 0.18d
                && positiveScore >= 0.46d
                && negativeScore <= 0.34d
                && contradictionScore <= 0.28d) {
            return LEVEL_STRONG;
        }

        if (validReasonCount >= 3
                && reasonEdge >= 0.08d
                && positiveScore >= 0.32d
                && negativeScore <= 0.48d) {
            return LEVEL_NORMAL;
        }

        if (validReasonCount >= 1 && reasonEdge > -0.05d) {
            return LEVEL_WEAK;
        }

        return LEVEL_RISK;
    }

    private double calcReasonConfidence(Models.PreDrawFeature f, ReasonBag bag) {
        if (f == null || bag == null) {
            return 0.0d;
        }

        double featureConfidence = getDouble(f, "confidence", 0.0d);
        int valid = bag.positiveAtoms.size();
        int bad = bag.negativeAtoms.size() + bag.contradictionAtoms.size();

        double atomScore = valid <= 0 ? 0.0d : Math.min(1.0d, ((double) valid) / 5.0d);
        double badPenalty = Math.min(0.50d, ((double) bad) * 0.08d);

        return clamp01(featureConfidence * 0.55d + atomScore * 0.40d - badPenalty);
    }

    private String buildReasonText(
            String targetIssue,
            int position,
            int number,
            Models.PreDrawFeature f,
            ReasonBag bag,
            String validLevel,
            double reasonScore,
            double reasonEdge) {
        StringBuilder sb = new StringBuilder();

        sb.append("【开奖前原因】");
        sb.append("目标期=");
        sb.append(safe(targetIssue));
        sb.append("｜位置=");
        sb.append(position);
        sb.append("｜号码=");
        sb.append(number);
        sb.append("\n");

        sb.append("【正因】");
        if (bag.positiveAtoms.size() > 0) {
            sb.append(joinAtoms(bag.positiveAtoms));
        } else {
            sb.append("无强正因");
        }
        sb.append("\n");

        sb.append("【反因】");
        if (bag.negativeAtoms.size() > 0) {
            sb.append(joinAtoms(bag.negativeAtoms));
        } else {
            sb.append("无明显反因");
        }
        sb.append("\n");

        sb.append("【矛盾因】");
        if (bag.contradictionAtoms.size() > 0) {
            sb.append(joinAtoms(bag.contradictionAtoms));
        } else {
            sb.append("无明显矛盾");
        }
        sb.append("\n");

        sb.append("【数值摘要】");
        sb.append("freq16=");
        sb.append(percent(getDouble(f, "freq16", 0.0d)));
        sb.append("｜freq64=");
        sb.append(percent(getDouble(f, "freq64", 0.0d)));
        sb.append("｜freq240=");
        sb.append(percent(getDouble(f, "freq240", 0.0d)));
        sb.append("｜freqSlope=");
        sb.append(signed(getDouble(f, "freqSlope", 0.0d)));
        sb.append("｜gapZ=");
        sb.append(format(getDouble(f, "gapZ", 0.0d)));
        sb.append("｜transitionLift=");
        sb.append(percent(getDouble(f, "transitionLift", 0.0d)));
        sb.append("｜transitionConflict=");
        sb.append(percent(getDouble(f, "transitionConflict", 0.0d)));
        sb.append("｜recoveryQuality=");
        sb.append(percent(getDouble(f, "recoveryQuality", 0.0d)));
        sb.append("｜falseRecoveryRisk=");
        sb.append(percent(getDouble(f, "falseRecoveryRisk", 0.0d)));
        sb.append("\n");

        sb.append("【原因强度】");
        sb.append("validLevel=");
        sb.append(validLevel);
        sb.append("｜reasonScore=");
        sb.append(percent(reasonScore));
        sb.append("｜reasonEdge=");
        sb.append(signed(reasonEdge));
        sb.append("｜validReasonCount=");
        sb.append(bag.positiveAtoms.size());
        sb.append("｜falseReasonCount=");
        sb.append(bag.negativeAtoms.size() + bag.contradictionAtoms.size());
        sb.append("\n");

        sb.append(
                "说明=本原因在开奖前生成，只作为后续 Contrastive / Causal / Probability / DecisionGate"
                    + " 的输入，不直接出号，不直接BET。");

        return sb.toString();
    }

    private String buildReasonSummary(
            int position,
            int number,
            String validLevel,
            double reasonEdge,
            int validReasonCount,
            int falseReasonCount) {
        return "开奖前原因｜P"
                + position
                + " N"
                + number
                + "｜级别="
                + validLevel
                + "｜reasonEdge="
                + signed(reasonEdge)
                + "｜有效因="
                + validReasonCount
                + "｜反证因="
                + falseReasonCount;
    }

    private String buildFingerprint(
            int position,
            int number,
            LinkedHashSet<String> positive,
            LinkedHashSet<String> negative,
            LinkedHashSet<String> contradiction,
            String validLevel) {
        ArrayList<String> atoms = new ArrayList<String>();

        atoms.add("P" + position);
        atoms.add("N" + number);
        atoms.add("L=" + safe(validLevel));

        addSortedAtoms(atoms, "POS", positive);
        addSortedAtoms(atoms, "NEG", negative);
        addSortedAtoms(atoms, "CON", contradiction);

        String raw = joinWith(atoms, "|");
        String hash = shortMd5(raw);

        return "RFP|" + raw + "|H=" + hash;
    }

    private void copyFeatureFields(Models.PreDrawFeature f, Models.PreDrawReason r) {
        if (f == null || r == null) {
            return;
        }

        copyDouble(f, r, "freq16");
        copyDouble(f, r, "freq32");
        copyDouble(f, r, "freq64");
        copyDouble(f, r, "freq120");
        copyDouble(f, r, "freq240");
        copyDouble(f, r, "freq500");
        copyDouble(f, r, "freqSlope");
        copyDouble(f, r, "freqAccel");
        copyDouble(f, r, "shortVsMid");
        copyDouble(f, r, "midVsLong");
        copyDouble(f, r, "trendConsistency");

        copyDouble(f, r, "positionEntropy");
        copyDouble(f, r, "positionTop3Mass");
        copyDouble(f, r, "positionStrengthSlope");
        copyDouble(f, r, "positionDrift");
        copyDouble(f, r, "positionNoiseScore");

        copyInt(f, r, "currentGap");
        copyDouble(f, r, "avgGap");
        copyDouble(f, r, "gapZ");
        copyDouble(f, r, "gapPhase");

        copyDouble(f, r, "hotPressure");
        copyDouble(f, r, "hotExhaustion");
        copyDouble(f, r, "coldRepairPower");
        copyDouble(f, r, "returnZoneScore");

        copyDouble(f, r, "transitionLift");
        copyDouble(f, r, "transitionConflict");
        copyDouble(f, r, "pairCoSupport");
        copyDouble(f, r, "pairConflict");

        copyDouble(f, r, "breakDepth");
        copyDouble(f, r, "recoverySpeed");
        copyDouble(f, r, "recoveryQuality");
        copyDouble(f, r, "falseRecoveryRisk");

        copyDouble(f, r, "featureScore");
        copyDouble(f, r, "confidence");

        copyString(f, r, "trendBucket");
        copyString(f, r, "gapBucket");
        copyString(f, r, "riskBucket");
        copyString(f, r, "transitionBucket");
        copyString(f, r, "featureSummary");
    }

    private void copyDouble(Object from, Object to, String field) {
        putDouble(to, field, getDouble(from, field, 0.0d));
    }

    private void copyInt(Object from, Object to, String field) {
        putInt(to, field, getInt(from, field, 0));
    }

    private void copyString(Object from, Object to, String field) {
        putString(to, field, getString(from, field, ""));
    }

    private void addPositive(ReasonBag bag, String atom, double weight) {
        if (bag == null || atom == null || atom.length() == 0) {
            return;
        }

        bag.positiveAtoms.add(atom);
        bag.reasonAtoms.add(atom);
        bag.positiveScore += clamp01(weight);
    }

    private void addNegative(ReasonBag bag, String atom, double weight) {
        if (bag == null || atom == null || atom.length() == 0) {
            return;
        }

        bag.negativeAtoms.add(atom);
        bag.reasonAtoms.add(atom);
        bag.negativeScore += clamp01(weight);
    }

    private void addContradiction(ReasonBag bag, String atom, double weight) {
        if (bag == null || atom == null || atom.length() == 0) {
            return;
        }

        bag.contradictionAtoms.add(atom);
        bag.reasonAtoms.add(atom);
        bag.contradictionScore += clamp01(weight);
    }

    private boolean hasAtom(LinkedHashSet<String> set, String atom) {
        return set != null && atom != null && set.contains(atom);
    }

    private String firstAtom(LinkedHashSet<String> set) {
        if (set == null || set.size() == 0) {
            return "";
        }

        for (String s : set) {
            return s == null ? "" : s;
        }

        return "";
    }

    private String joinAtoms(LinkedHashSet<String> set) {
        if (set == null || set.size() == 0) {
            return "";
        }

        ArrayList<String> list = new ArrayList<String>();

        for (String s : set) {
            if (s != null && s.length() > 0) {
                list.add(s);
            }
        }

        return joinWith(list, ",");
    }

    private static void addSortedAtoms(
            ArrayList<String> out, String prefix, LinkedHashSet<String> set) {
        if (out == null || prefix == null || set == null || set.size() == 0) {
            return;
        }

        ArrayList<String> list = new ArrayList<String>();

        for (String s : set) {
            if (s != null && s.length() > 0) {
                list.add(s);
            }
        }

        Collections.sort(list);

        for (int i = 0; i < list.size(); i++) {
            out.add(prefix + "_" + list.get(i));
        }
    }

    private static String joinWith(List<String> list, String sep) {
        if (list == null || list.size() == 0) {
            return "";
        }

        if (sep == null) {
            sep = ",";
        }

        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < list.size(); i++) {
            String s = list.get(i);

            if (s == null || s.length() == 0) {
                continue;
            }

            if (sb.length() > 0) {
                sb.append(sep);
            }

            sb.append(s);
        }

        return sb.toString();
    }

    private static double positivePart(double v) {
        return v > 0.0d ? v : 0.0d;
    }

    private static double negativePart(double v) {
        return v < 0.0d ? -v : 0.0d;
    }

    private static double clamp01(double v) {
        if (v < 0.0d) {
            return 0.0d;
        }

        if (v > 1.0d) {
            return 1.0d;
        }

        return v;
    }

    private static double clampSigned(double v) {
        if (v < -1.0d) {
            return -1.0d;
        }

        if (v > 1.0d) {
            return 1.0d;
        }

        return v;
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    private static String firstNonEmpty(String a, String b) {
        if (a != null && a.length() > 0) {
            return a;
        }

        if (b != null) {
            return b;
        }

        return "";
    }

    private static String percent(double v) {
        return String.format(Locale.US, "%.1f%%", Double.valueOf(clamp01(v) * 100.0d));
    }

    private static String signed(double v) {
        String sign = v >= 0.0d ? "+" : "";
        return sign + String.format(Locale.US, "%.3f", Double.valueOf(v));
    }

    private static String format(double v) {
        return String.format(Locale.US, "%.2f", Double.valueOf(v));
    }

    private static String shortMd5(String raw) {
        if (raw == null) {
            raw = "";
        }

        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] bytes = md.digest(raw.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();

            for (int i = 0; i < bytes.length; i++) {
                int v = bytes[i] & 0xff;

                if (v < 16) {
                    sb.append("0");
                }

                sb.append(Integer.toHexString(v));
            }

            String s = sb.toString();

            if (s.length() > 12) {
                return s.substring(0, 12);
            }

            return s;
        } catch (Throwable e) {
            int h = raw.hashCode();
            return Integer.toHexString(h);
        }
    }

    private static int getInt(Object obj, String fieldName, int fallback) {
        if (obj == null || fieldName == null || fieldName.length() == 0) {
            return fallback;
        }

        try {
            Field f = obj.getClass().getField(fieldName);
            Object v = f.get(obj);

            if (v instanceof Integer) {
                return ((Integer) v).intValue();
            }

            if (v instanceof Long) {
                return (int) ((Long) v).longValue();
            }

            if (v instanceof Number) {
                return ((Number) v).intValue();
            }

            if (v != null) {
                return Integer.parseInt(String.valueOf(v));
            }
        } catch (Throwable e) {
        }

        return fallback;
    }

    private static long getLong(Object obj, String fieldName, long fallback) {
        if (obj == null || fieldName == null || fieldName.length() == 0) {
            return fallback;
        }

        try {
            Field f = obj.getClass().getField(fieldName);
            Object v = f.get(obj);

            if (v instanceof Long) {
                return ((Long) v).longValue();
            }

            if (v instanceof Integer) {
                return ((Integer) v).longValue();
            }

            if (v instanceof Number) {
                return ((Number) v).longValue();
            }

            if (v != null) {
                return Long.parseLong(String.valueOf(v));
            }
        } catch (Throwable e) {
        }

        return fallback;
    }

    private static double getDouble(Object obj, String fieldName, double fallback) {
        if (obj == null || fieldName == null || fieldName.length() == 0) {
            return fallback;
        }

        try {
            Field f = obj.getClass().getField(fieldName);
            Object v = f.get(obj);

            if (v instanceof Double) {
                return ((Double) v).doubleValue();
            }

            if (v instanceof Float) {
                return ((Float) v).doubleValue();
            }

            if (v instanceof Number) {
                return ((Number) v).doubleValue();
            }

            if (v != null) {
                return Double.parseDouble(String.valueOf(v));
            }
        } catch (Throwable e) {
        }

        return fallback;
    }

    private static String getString(Object obj, String fieldName, String fallback) {
        if (obj == null || fieldName == null || fieldName.length() == 0) {
            return fallback;
        }

        try {
            Field f = obj.getClass().getField(fieldName);
            Object v = f.get(obj);

            if (v != null) {
                return String.valueOf(v);
            }
        } catch (Throwable e) {
        }

        return fallback;
    }

    private static void putInt(Object obj, String fieldName, int value) {
        if (obj == null || fieldName == null || fieldName.length() == 0) {
            return;
        }

        try {
            Field f = obj.getClass().getField(fieldName);
            f.setInt(obj, value);
        } catch (Throwable e) {
            try {
                Field f = obj.getClass().getField(fieldName);
                f.set(obj, Integer.valueOf(value));
            } catch (Throwable ignored) {
            }
        }
    }

    private static void putLong(Object obj, String fieldName, long value) {
        if (obj == null || fieldName == null || fieldName.length() == 0) {
            return;
        }

        try {
            Field f = obj.getClass().getField(fieldName);
            f.setLong(obj, value);
        } catch (Throwable e) {
            try {
                Field f = obj.getClass().getField(fieldName);
                f.set(obj, Long.valueOf(value));
            } catch (Throwable ignored) {
            }
        }
    }

    private static void putDouble(Object obj, String fieldName, double value) {
        if (obj == null || fieldName == null || fieldName.length() == 0) {
            return;
        }

        try {
            Field f = obj.getClass().getField(fieldName);
            f.setDouble(obj, value);
        } catch (Throwable e) {
            try {
                Field f = obj.getClass().getField(fieldName);
                f.set(obj, Double.valueOf(value));
            } catch (Throwable ignored) {
            }
        }
    }

    private static void putString(Object obj, String fieldName, String value) {
        if (obj == null || fieldName == null || fieldName.length() == 0) {
            return;
        }

        try {
            Field f = obj.getClass().getField(fieldName);
            f.set(obj, value == null ? "" : value);
        } catch (Throwable e) {
        }
    }

    private static final class ReasonBag {
        LinkedHashSet<String> reasonAtoms = new LinkedHashSet<String>();
        LinkedHashSet<String> positiveAtoms = new LinkedHashSet<String>();
        LinkedHashSet<String> negativeAtoms = new LinkedHashSet<String>();
        LinkedHashSet<String> contradictionAtoms = new LinkedHashSet<String>();

        double positiveScore;
        double negativeScore;
        double contradictionScore;

        String mainPositiveAtom = "";
        String mainNegativeAtom = "";
        String mainContradictionAtom = "";
    }
}
