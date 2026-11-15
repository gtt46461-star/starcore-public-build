package com.ethk3.novaonepro;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
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
 * ReasonCausalMatrixEngine.java
 *
 * <p>第8/17 新增核心引擎。
 *
 * <p>作用： 1. 判断开奖前原因到底是真有效、伪有效、衰退、还是只能解释。 2. 必须实现 Wilson 下界。 3. 输出 Models.ReasonCausalProfile。 4. 后续
 * ProbabilityEngine 只能给 A/B 加分，不能给 C 加分，D/E 必须降权。
 *
 * <p>评级： A_VALID 样本足、Wilson下界高、近期不衰退 B_USEFUL 有帮助，小幅加分 C_EXPLAIN_ONLY 只能解释，不能加分 D_DECAY 历史有效，近期衰退
 * E_FALSE 伪原因，禁止加分
 *
 * <p>注意： 本文件只做因果评级，不直接出号，不直接BET，不结算，不写真实盈亏。
 */
public final class ReasonCausalMatrixEngine {

    public static final String A_VALID = "A_VALID";
    public static final String B_USEFUL = "B_USEFUL";
    public static final String C_EXPLAIN_ONLY = "C_EXPLAIN_ONLY";
    public static final String D_DECAY = "D_DECAY";
    public static final String E_FALSE = "E_FALSE";

    public static final String SOURCE_PREDRAW_REASON = "PREDRAW_REASON";
    public static final String SOURCE_CONTRASTIVE = "CONTRASTIVE_REASON";
    public static final String SOURCE_PROFILE_CACHE = "PROFILE_CACHE";

    private static final double BASE_K3_RATE = 0.300d;
    private static final double WILSON_Z_95 = 1.96d;

    private static final int DEFAULT_MIN_SAMPLE = 30;
    private static final int DEFAULT_STRONG_SAMPLE = 120;
    private static final int DEFAULT_RECENT_WINDOW = 80;

    private static final double A_WILSON_MIN = 0.365d;
    private static final double A_RECENT_MIN = 0.350d;
    private static final double B_WILSON_MIN = 0.330d;
    private static final double FALSE_WILSON_MAX = 0.285d;
    private static final double DECAY_DROP_MIN = 0.075d;

    public ReasonCausalMatrixEngine() {}

    public Models.ReasonCausalProfile evaluate(
            Models.PreDrawReason reason, Models.ContrastiveProfile contrastive, DbStore dbStore) {
        return evaluate("", reason, contrastive, dbStore);
    }

    public Models.ReasonCausalProfile evaluate(
            String targetIssue,
            Models.PreDrawReason reason,
            Models.ContrastiveProfile contrastive,
            DbStore dbStore) {
        Models.ReasonCausalProfile out = new Models.ReasonCausalProfile();

        if (reason == null) {
            fillEmpty(out, targetIssue, "EMPTY_REASON");
            return out;
        }

        int position = getInt(reason, "position", 0);
        int number = getInt(reason, "number", 0);

        String realTargetIssue = firstNonEmpty(targetIssue, getString(reason, "targetIssue", ""));
        String baseIssue = getString(reason, "baseIssue", "");
        String stateKey = getString(reason, "stateKey", "");
        String reasonKey = getString(reason, "reasonKey", position + "_" + number);
        String fingerprint = getString(reason, "reasonFingerprint", "");

        LinkedHashSet<String> positiveAtoms = parseAtoms(getString(reason, "positiveAtoms", ""));
        LinkedHashSet<String> negativeAtoms = parseAtoms(getString(reason, "negativeAtoms", ""));
        LinkedHashSet<String> contradictionAtoms =
                parseAtoms(getString(reason, "contradictionAtoms", ""));
        LinkedHashSet<String> allAtoms = parseAtoms(getString(reason, "reasonAtoms", ""));

        if (allAtoms.size() == 0) {
            addAll(allAtoms, positiveAtoms);
            addAll(allAtoms, negativeAtoms);
            addAll(allAtoms, contradictionAtoms);
        }

        ArrayList<AtomEval> evals = new ArrayList<AtomEval>();

        for (String atom : positiveAtoms) {
            AtomEval e = evaluateAtom(atom, true, false, false, reason, contrastive, dbStore);
            evals.add(e);
        }

        for (String atom : negativeAtoms) {
            AtomEval e = evaluateAtom(atom, false, true, false, reason, contrastive, dbStore);
            evals.add(e);
        }

        for (String atom : contradictionAtoms) {
            AtomEval e = evaluateAtom(atom, false, false, true, reason, contrastive, dbStore);
            evals.add(e);
        }

        if (evals.size() == 0) {
            for (String atom : allAtoms) {
                AtomEval e = evaluateAtom(atom, false, false, false, reason, contrastive, dbStore);
                evals.add(e);
            }
        }

        MatrixCalc calc = aggregate(evals, reason, contrastive);

        String finalLevel = judgeFinalLevel(calc, evals, reason, contrastive);
        String profileKey =
                buildProfileKey(stateKey, fingerprint, position, number, calc.majorAtomSignature);
        String summary = buildSummary(realTargetIssue, position, number, finalLevel, calc, evals);

        putString(out, "profileKey", profileKey);
        putString(out, "reasonKey", reasonKey);
        putString(out, "baseIssue", baseIssue);
        putString(out, "targetIssue", realTargetIssue);
        putString(out, "stateKey", stateKey);
        putString(out, "reasonFingerprint", fingerprint);
        putString(out, "validLevel", finalLevel);
        putString(out, "causalLevel", finalLevel);
        putString(out, "majorAtomSignature", calc.majorAtomSignature);
        putString(out, "validReasonAtoms", joinAtoms(calc.validAtoms));
        putString(out, "usefulReasonAtoms", joinAtoms(calc.usefulAtoms));
        putString(out, "explainOnlyAtoms", joinAtoms(calc.explainAtoms));
        putString(out, "decayAtoms", joinAtoms(calc.decayAtoms));
        putString(out, "falseAtoms", joinAtoms(calc.falseAtoms));
        putString(out, "atomEvaluationText", buildAtomText(evals));
        putString(out, "summaryText", summary);
        putString(out, "causalSummary", summary);
        putString(out, "sourceType", SOURCE_PREDRAW_REASON);

        putInt(out, "position", position);
        putInt(out, "number", number);
        putLong(out, "createdAt", System.currentTimeMillis());
        putInt(out, "sampleCount", calc.sampleCount);
        putInt(out, "hitCount", calc.hitCount);
        putInt(out, "missCount", calc.missCount);
        putInt(out, "recentSample", calc.recentSample);
        putInt(out, "recentHit", calc.recentHit);
        putInt(out, "recentMiss", calc.recentMiss);
        putInt(out, "validReasonCount", calc.validAtoms.size());
        putInt(out, "usefulReasonCount", calc.usefulAtoms.size());
        putInt(out, "explainOnlyCount", calc.explainAtoms.size());
        putInt(out, "decayReasonCount", calc.decayAtoms.size());
        putInt(out, "falseReasonCount", calc.falseAtoms.size());
        putInt(out, "probabilityAllowBoost", probabilityAllowBoost(finalLevel));
        putInt(out, "mustPenalty", mustPenalty(finalLevel));

        putDouble(out, "hitRate", calc.hitRate);
        putDouble(out, "recentHitRate", calc.recentHitRate);
        putDouble(out, "wilsonLower", calc.wilsonLower);
        putDouble(out, "recentWilsonLower", calc.recentWilsonLower);
        putDouble(out, "causalLift", calc.causalLift);
        putDouble(out, "causalConfidence", calc.confidence);
        putDouble(out, "confidence", calc.confidence);
        putDouble(out, "decayScore", calc.decayScore);
        putDouble(out, "falseScore", calc.falseScore);
        putDouble(out, "riskScore", calc.riskScore);
        putDouble(out, "supportScore", calc.supportScore);
        putDouble(out, "probabilityBoostScale", probabilityBoostScale(finalLevel, calc));
        putDouble(out, "probabilityPenaltyScale", probabilityPenaltyScale(finalLevel, calc));

        saveProfileIfPossible(dbStore, out);

        return out;
    }

    public Map<String, Models.ReasonCausalProfile> evaluateAll(
            Map<String, Models.PreDrawReason> reasons,
            Map<String, Models.ContrastiveProfile> contrastiveMap,
            DbStore dbStore) {
        return evaluateAll("", reasons, contrastiveMap, dbStore);
    }

    public Map<String, Models.ReasonCausalProfile> evaluateAll(
            String targetIssue,
            Map<String, Models.PreDrawReason> reasons,
            Map<String, Models.ContrastiveProfile> contrastiveMap,
            DbStore dbStore) {
        LinkedHashMap<String, Models.ReasonCausalProfile> out =
                new LinkedHashMap<String, Models.ReasonCausalProfile>();

        if (reasons == null || reasons.size() == 0) {
            return out;
        }

        ArrayList<Models.PreDrawReason> list = new ArrayList<Models.PreDrawReason>();

        for (Models.PreDrawReason r : reasons.values()) {
            if (r != null) {
                list.add(r);
            }
        }

        Collections.sort(
                list,
                new Comparator<Models.PreDrawReason>() {
                    public int compare(Models.PreDrawReason a, Models.PreDrawReason b) {
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
            Models.PreDrawReason r = list.get(i);
            int position = getInt(r, "position", 0);
            int number = getInt(r, "number", 0);
            String key = keyOf(position, number);

            Models.ContrastiveProfile cp = null;

            if (contrastiveMap != null) {
                cp = contrastiveMap.get(key);
            }

            Models.ReasonCausalProfile p = evaluate(targetIssue, r, cp, dbStore);
            out.put(key, p);
        }

        return out;
    }

    public ArrayList<Models.ReasonCausalProfile> toSortedProfileList(
            Map<String, Models.ReasonCausalProfile> map) {
        ArrayList<Models.ReasonCausalProfile> list = new ArrayList<Models.ReasonCausalProfile>();

        if (map == null || map.size() == 0) {
            return list;
        }

        for (Models.ReasonCausalProfile p : map.values()) {
            if (p != null) {
                list.add(p);
            }
        }

        Collections.sort(
                list,
                new Comparator<Models.ReasonCausalProfile>() {
                    public int compare(Models.ReasonCausalProfile a, Models.ReasonCausalProfile b) {
                        double ea = getDouble(a, "causalLift", 0.0d);
                        double eb = getDouble(b, "causalLift", 0.0d);

                        if (ea > eb) {
                            return -1;
                        }

                        if (ea < eb) {
                            return 1;
                        }

                        double wa = getDouble(a, "wilsonLower", 0.0d);
                        double wb = getDouble(b, "wilsonLower", 0.0d);

                        if (wa > wb) {
                            return -1;
                        }

                        if (wa < wb) {
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

    public static String keyOf(int position, int number) {
        return String.valueOf(position) + "_" + String.valueOf(number);
    }

    public double wilsonLowerBound(int hit, int total) {
        return wilsonLower(hit, total);
    }

    public static double wilsonLower(int hit, int total) {
        if (total <= 0 || hit <= 0) {
            return 0.0d;
        }

        if (hit > total) {
            hit = total;
        }

        double n = (double) total;
        double phat = ((double) hit) / n;
        double z = WILSON_Z_95;
        double z2 = z * z;

        double numerator =
                phat
                        + z2 / (2.0d * n)
                        - z * Math.sqrt((phat * (1.0d - phat) + z2 / (4.0d * n)) / n);
        double denominator = 1.0d + z2 / n;

        return clamp01(numerator / denominator);
    }

    private void fillEmpty(Models.ReasonCausalProfile out, String targetIssue, String reason) {
        putString(out, "profileKey", "EMPTY_CAUSAL_PROFILE");
        putString(out, "targetIssue", safe(targetIssue));
        putString(out, "validLevel", C_EXPLAIN_ONLY);
        putString(out, "causalLevel", C_EXPLAIN_ONLY);
        putString(out, "summaryText", "原因因果矩阵失败：" + safe(reason));
        putString(out, "causalSummary", "原因因果矩阵失败：" + safe(reason));

        putInt(out, "sampleCount", 0);
        putInt(out, "hitCount", 0);
        putInt(out, "missCount", 0);
        putInt(out, "recentSample", 0);
        putInt(out, "recentHit", 0);
        putInt(out, "recentMiss", 0);
        putInt(out, "probabilityAllowBoost", 0);
        putInt(out, "mustPenalty", 0);

        putDouble(out, "hitRate", 0.0d);
        putDouble(out, "recentHitRate", 0.0d);
        putDouble(out, "wilsonLower", 0.0d);
        putDouble(out, "recentWilsonLower", 0.0d);
        putDouble(out, "causalLift", 0.0d);
        putDouble(out, "causalConfidence", 0.0d);
        putDouble(out, "confidence", 0.0d);
        putDouble(out, "decayScore", 0.0d);
        putDouble(out, "falseScore", 0.0d);
        putDouble(out, "riskScore", 0.0d);
        putDouble(out, "supportScore", 0.0d);
        putLong(out, "createdAt", System.currentTimeMillis());
    }

    private AtomEval evaluateAtom(
            String atom,
            boolean positiveAtom,
            boolean negativeAtom,
            boolean contradictionAtom,
            Models.PreDrawReason reason,
            Models.ContrastiveProfile contrastive,
            DbStore dbStore) {
        AtomEval e = new AtomEval();

        e.atom = safe(atom);
        e.positiveAtom = positiveAtom;
        e.negativeAtom = negativeAtom;
        e.contradictionAtom = contradictionAtom;

        int position = getInt(reason, "position", 0);
        int number = getInt(reason, "number", 0);
        String stateKey = getString(reason, "stateKey", "");
        String fingerprint = getString(reason, "reasonFingerprint", "");

        Object cached =
                loadCachedAtomProfile(dbStore, atom, stateKey, fingerprint, position, number);

        e.sampleCount = getProfileInt(cached, "sampleCount", "sample_count", 0);
        e.hitCount = getProfileInt(cached, "hitCount", "hit_count", 0);
        e.missCount = getProfileInt(cached, "missCount", "miss_count", 0);
        e.recentSample = getProfileInt(cached, "recentSample", "recent_sample", 0);
        e.recentHit = getProfileInt(cached, "recentHit", "recent_hit", 0);
        e.recentMiss = getProfileInt(cached, "recentMiss", "recent_miss", 0);

        double cachedHitRate = getProfileDouble(cached, "hitRate", "hit_rate", -1.0d);
        double cachedRecentHitRate =
                getProfileDouble(cached, "recentHitRate", "recent_hit_rate", -1.0d);

        if (e.sampleCount <= 0) {
            e.sampleCount = getInt(contrastive, "sampleCount", 0);
            e.hitCount = getInt(contrastive, "hitSample", 0);
            e.missCount = getInt(contrastive, "missSample", 0);
            e.recentSample = e.sampleCount;
            e.recentHit = e.hitCount;
            e.recentMiss = e.missCount;
        }

        if (e.hitCount < 0) {
            e.hitCount = 0;
        }

        if (e.missCount < 0) {
            e.missCount = 0;
        }

        if (e.sampleCount <= 0) {
            e.sampleCount = e.hitCount + e.missCount;
        }

        if (e.missCount <= 0 && e.sampleCount > e.hitCount) {
            e.missCount = e.sampleCount - e.hitCount;
        }

        if (e.recentSample <= 0) {
            e.recentSample = Math.min(e.sampleCount, getRecentWindow());
            e.recentHit = Math.min(e.hitCount, e.recentSample);
            e.recentMiss = Math.max(0, e.recentSample - e.recentHit);
        }

        if (cachedHitRate >= 0.0d) {
            e.hitRate = clamp01(cachedHitRate);
        } else {
            e.hitRate =
                    e.sampleCount > 0
                            ? clamp01(((double) e.hitCount) / ((double) e.sampleCount))
                            : 0.0d;
        }

        if (cachedRecentHitRate >= 0.0d) {
            e.recentHitRate = clamp01(cachedRecentHitRate);
        } else {
            e.recentHitRate =
                    e.recentSample > 0
                            ? clamp01(((double) e.recentHit) / ((double) e.recentSample))
                            : e.hitRate;
        }

        e.wilsonLower = wilsonLower(e.hitCount, e.sampleCount);
        e.recentWilsonLower = wilsonLower(e.recentHit, e.recentSample);
        e.causalLift = e.wilsonLower - BASE_K3_RATE;
        e.recentLift = e.recentWilsonLower - BASE_K3_RATE;
        e.decayScore =
                calcDecayScore(e.hitRate, e.recentHitRate, e.wilsonLower, e.recentWilsonLower);
        e.falseScore = calcFalseScore(e, contrastive);
        e.confidence = confidenceFromSample(e.sampleCount, e.recentSample);
        e.supportScore =
                clamp01(
                        e.causalLift * 2.2d
                                + e.confidence * 0.25d
                                + Math.max(0.0d, e.recentLift) * 1.5d);
        e.riskScore =
                clamp01(
                        e.falseScore * 0.55d
                                + e.decayScore * 0.30d
                                + Math.max(0.0d, -e.causalLift) * 1.2d);

        e.level = judgeAtomLevel(e, positiveAtom, negativeAtom, contradictionAtom, contrastive);

        return e;
    }

    private Object loadCachedAtomProfile(
            DbStore dbStore,
            String atom,
            String stateKey,
            String fingerprint,
            int position,
            int number) {
        if (dbStore == null || atom == null || atom.length() == 0) {
            return null;
        }

        String profileKey = buildAtomProfileKey(stateKey, fingerprint, position, number, atom);

        Object v =
                call(
                        dbStore,
                        "loadReasonCausalProfile",
                        new Class[] {String.class},
                        new Object[] {profileKey});

        if (v != null) {
            return v;
        }

        v =
                call(
                        dbStore,
                        "getReasonCausalProfileValues",
                        new Class[] {String.class},
                        new Object[] {profileKey});

        if (v != null) {
            return v;
        }

        v =
                call(
                        dbStore,
                        "loadReasonCausalProfile",
                        new Class[] {
                            String.class, String.class, int.class, int.class, String.class
                        },
                        new Object[] {
                            stateKey,
                            fingerprint,
                            Integer.valueOf(position),
                            Integer.valueOf(number),
                            atom
                        });

        if (v != null) {
            return v;
        }

        v =
                call(
                        dbStore,
                        "loadReasonCausalProfile",
                        new Class[] {String.class, int.class, int.class, String.class},
                        new Object[] {
                            stateKey, Integer.valueOf(position), Integer.valueOf(number), atom
                        });

        if (v != null) {
            return v;
        }

        v =
                call(
                        dbStore,
                        "loadReasonCausalProfileByAtom",
                        new Class[] {String.class, int.class, int.class, String.class},
                        new Object[] {
                            stateKey, Integer.valueOf(position), Integer.valueOf(number), atom
                        });

        return v;
    }

    private MatrixCalc aggregate(
            ArrayList<AtomEval> evals,
            Models.PreDrawReason reason,
            Models.ContrastiveProfile contrastive) {
        MatrixCalc c = new MatrixCalc();

        if (evals == null || evals.size() == 0) {
            return c;
        }

        int sampleWeighted = 0;
        int hitWeighted = 0;
        int missWeighted = 0;
        int recentSampleWeighted = 0;
        int recentHitWeighted = 0;
        int recentMissWeighted = 0;

        double wilsonSum = 0.0d;
        double recentWilsonSum = 0.0d;
        double liftSum = 0.0d;
        double confidenceSum = 0.0d;
        double decaySum = 0.0d;
        double falseSum = 0.0d;
        double supportSum = 0.0d;
        double riskSum = 0.0d;
        double weightSum = 0.0d;

        ArrayList<String> sigParts = new ArrayList<String>();

        for (int i = 0; i < evals.size(); i++) {
            AtomEval e = evals.get(i);

            if (e == null) {
                continue;
            }

            double w = atomWeight(e);

            sampleWeighted += (int) Math.round(e.sampleCount * w);
            hitWeighted += (int) Math.round(e.hitCount * w);
            missWeighted += (int) Math.round(e.missCount * w);
            recentSampleWeighted += (int) Math.round(e.recentSample * w);
            recentHitWeighted += (int) Math.round(e.recentHit * w);
            recentMissWeighted += (int) Math.round(e.recentMiss * w);

            wilsonSum += e.wilsonLower * w;
            recentWilsonSum += e.recentWilsonLower * w;
            liftSum += e.causalLift * w;
            confidenceSum += e.confidence * w;
            decaySum += e.decayScore * w;
            falseSum += e.falseScore * w;
            supportSum += e.supportScore * w;
            riskSum += e.riskScore * w;
            weightSum += w;

            if (A_VALID.equals(e.level)) {
                c.validAtoms.add(e.atom);
            } else if (B_USEFUL.equals(e.level)) {
                c.usefulAtoms.add(e.atom);
            } else if (C_EXPLAIN_ONLY.equals(e.level)) {
                c.explainAtoms.add(e.atom);
            } else if (D_DECAY.equals(e.level)) {
                c.decayAtoms.add(e.atom);
            } else if (E_FALSE.equals(e.level)) {
                c.falseAtoms.add(e.atom);
            }

            sigParts.add(e.atom + "=" + e.level);
        }

        if (weightSum <= 0.0d) {
            weightSum = 1.0d;
        }

        c.sampleCount = sampleWeighted;
        c.hitCount = hitWeighted;
        c.missCount = missWeighted;
        c.recentSample = recentSampleWeighted;
        c.recentHit = recentHitWeighted;
        c.recentMiss = recentMissWeighted;

        if (c.sampleCount <= 0) {
            c.sampleCount = getInt(contrastive, "sampleCount", 0);
            c.hitCount = getInt(contrastive, "hitSample", 0);
            c.missCount = getInt(contrastive, "missSample", 0);
        }

        c.hitRate =
                c.sampleCount > 0
                        ? clamp01(((double) c.hitCount) / ((double) c.sampleCount))
                        : 0.0d;
        c.recentHitRate =
                c.recentSample > 0
                        ? clamp01(((double) c.recentHit) / ((double) c.recentSample))
                        : c.hitRate;

        c.wilsonLower = clamp01(wilsonSum / weightSum);
        c.recentWilsonLower = clamp01(recentWilsonSum / weightSum);
        c.causalLift = clampSigned(liftSum / weightSum);
        c.confidence = clamp01(confidenceSum / weightSum);
        c.decayScore = clamp01(decaySum / weightSum);
        c.falseScore = clamp01(falseSum / weightSum);
        c.supportScore = clamp01(supportSum / weightSum);
        c.riskScore = clamp01(riskSum / weightSum);

        double hitSimilarity = getDouble(contrastive, "hitSimilarityScore", 0.0d);
        double missSimilarity = getDouble(contrastive, "missSimilarityScore", 0.0d);
        double reasonEdge =
                getDouble(contrastive, "reasonEdge", getDouble(reason, "reasonEdge", 0.0d));

        if (hitSimilarity > 0.0d || missSimilarity > 0.0d) {
            c.supportScore =
                    clamp01(
                            c.supportScore * 0.72d
                                    + hitSimilarity * 0.20d
                                    + Math.max(0.0d, reasonEdge) * 0.08d);
            c.riskScore =
                    clamp01(
                            c.riskScore * 0.72d
                                    + missSimilarity * 0.20d
                                    + Math.max(0.0d, -reasonEdge) * 0.08d);
        }

        c.majorAtomSignature = "CAUSAL|" + shortMd5(joinWith(sigParts, "|"));

        return c;
    }

    private double atomWeight(AtomEval e) {
        if (e == null) {
            return 0.0d;
        }

        double w = 1.0d;

        if (e.positiveAtom) {
            w += 0.20d;
        }

        if (e.negativeAtom) {
            w += 0.12d;
        }

        if (e.contradictionAtom) {
            w += 0.18d;
        }

        if (e.sampleCount >= getStrongSample()) {
            w += 0.20d;
        } else if (e.sampleCount >= getMinSample()) {
            w += 0.10d;
        }

        return w;
    }

    private String judgeAtomLevel(
            AtomEval e,
            boolean positiveAtom,
            boolean negativeAtom,
            boolean contradictionAtom,
            Models.ContrastiveProfile contrastive) {
        if (e == null) {
            return C_EXPLAIN_ONLY;
        }

        int minSample = getMinSample();
        int strongSample = getStrongSample();

        double missSimilarity = getDouble(contrastive, "missSimilarityScore", 0.0d);

        if (negativeAtom || contradictionAtom) {
            if (e.sampleCount >= minSample
                    && (e.wilsonLower <= FALSE_WILSON_MAX
                            || missSimilarity >= 0.52d
                            || e.falseScore >= 0.55d)) {
                return E_FALSE;
            }

            if (e.sampleCount >= minSample && e.decayScore >= 0.45d) {
                return D_DECAY;
            }

            return C_EXPLAIN_ONLY;
        }

        if (e.sampleCount < minSample) {
            return C_EXPLAIN_ONLY;
        }

        if (e.wilsonLower <= FALSE_WILSON_MAX || e.falseScore >= 0.62d) {
            return E_FALSE;
        }

        if (e.decayScore >= 0.50d) {
            return D_DECAY;
        }

        if (e.sampleCount >= strongSample
                && e.wilsonLower >= A_WILSON_MIN
                && e.recentWilsonLower >= A_RECENT_MIN
                && e.decayScore <= 0.24d
                && e.falseScore <= 0.32d) {
            return A_VALID;
        }

        if (e.wilsonLower >= B_WILSON_MIN && e.falseScore <= 0.45d && e.decayScore <= 0.38d) {
            return B_USEFUL;
        }

        return C_EXPLAIN_ONLY;
    }

    private String judgeFinalLevel(
            MatrixCalc c,
            ArrayList<AtomEval> evals,
            Models.PreDrawReason reason,
            Models.ContrastiveProfile contrastive) {
        if (c == null) {
            return C_EXPLAIN_ONLY;
        }

        int minSample = getMinSample();
        int strongSample = getStrongSample();

        double missSimilarity = getDouble(contrastive, "missSimilarityScore", 0.0d);
        double hitSimilarity = getDouble(contrastive, "hitSimilarityScore", 0.0d);
        double contrastiveEdge = getDouble(contrastive, "reasonEdge", 0.0d);
        int falseReasonCount = getInt(reason, "falseReasonCount", 0);
        double reasonEdge = getDouble(reason, "reasonEdge", 0.0d);

        if (c.falseAtoms.size() > 0
                || falseReasonCount >= 3
                || c.falseScore >= 0.62d
                || missSimilarity >= 0.55d) {
            return E_FALSE;
        }

        if (c.decayAtoms.size() > 0 || c.decayScore >= 0.48d) {
            return D_DECAY;
        }

        if (c.sampleCount < minSample) {
            return C_EXPLAIN_ONLY;
        }

        if (c.sampleCount >= strongSample
                && c.validAtoms.size() >= 2
                && c.wilsonLower >= A_WILSON_MIN
                && c.recentWilsonLower >= A_RECENT_MIN
                && c.causalLift > 0.035d
                && c.riskScore <= 0.42d
                && missSimilarity <= 0.42d
                && reasonEdge >= 0.02d) {
            return A_VALID;
        }

        if ((c.validAtoms.size() >= 1 || c.usefulAtoms.size() >= 2)
                && c.wilsonLower >= B_WILSON_MIN
                && c.causalLift > 0.010d
                && c.riskScore <= 0.52d
                && missSimilarity <= 0.48d
                && contrastiveEdge > -0.04d) {
            return B_USEFUL;
        }

        if (hitSimilarity > 0.0d || missSimilarity > 0.0d) {
            if (hitSimilarity <= missSimilarity || contrastiveEdge <= -0.04d) {
                return C_EXPLAIN_ONLY;
            }
        }

        return C_EXPLAIN_ONLY;
    }

    private double calcDecayScore(
            double hitRate, double recentHitRate, double wilson, double recentWilson) {
        double decay = 0.0d;

        if (hitRate - recentHitRate >= DECAY_DROP_MIN) {
            decay += (hitRate - recentHitRate) * 2.4d;
        }

        if (wilson - recentWilson >= 0.055d) {
            decay += (wilson - recentWilson) * 2.6d;
        }

        if (recentWilson < BASE_K3_RATE && wilson > BASE_K3_RATE) {
            decay += 0.24d;
        }

        return clamp01(decay);
    }

    private double calcFalseScore(AtomEval e, Models.ContrastiveProfile contrastive) {
        if (e == null) {
            return 0.0d;
        }

        double score = 0.0d;

        if (e.sampleCount >= getMinSample() && e.wilsonLower < FALSE_WILSON_MAX) {
            score += 0.38d;
        }

        if (e.hitRate < BASE_K3_RATE - 0.035d && e.sampleCount >= getMinSample()) {
            score += 0.24d;
        }

        if (e.recentHitRate < BASE_K3_RATE - 0.045d
                && e.recentSample >= Math.max(10, getRecentWindow() / 3)) {
            score += 0.22d;
        }

        double missSimilarity = getDouble(contrastive, "missSimilarityScore", 0.0d);

        if (missSimilarity >= 0.52d) {
            score += 0.24d;
        } else if (missSimilarity >= 0.40d) {
            score += 0.12d;
        }

        return clamp01(score);
    }

    private double confidenceFromSample(int sample, int recentSample) {
        double a;

        if (sample >= getStrongSample()) {
            a = 1.0d;
        } else if (sample <= 0) {
            a = 0.0d;
        } else {
            a = ((double) sample) / ((double) getStrongSample());
        }

        double b;

        if (recentSample >= getRecentWindow()) {
            b = 1.0d;
        } else if (recentSample <= 0) {
            b = 0.0d;
        } else {
            b = ((double) recentSample) / ((double) getRecentWindow());
        }

        return clamp01(a * 0.70d + b * 0.30d);
    }

    private int probabilityAllowBoost(String level) {
        if (A_VALID.equals(level) || B_USEFUL.equals(level)) {
            return 1;
        }

        return 0;
    }

    private int mustPenalty(String level) {
        if (D_DECAY.equals(level) || E_FALSE.equals(level)) {
            return 1;
        }

        return 0;
    }

    private double probabilityBoostScale(String level, MatrixCalc calc) {
        if (calc == null) {
            return 0.0d;
        }

        if (A_VALID.equals(level)) {
            return clamp01(
                    0.60d + calc.confidence * 0.30d + Math.max(0.0d, calc.causalLift) * 2.0d);
        }

        if (B_USEFUL.equals(level)) {
            return clamp01(
                    0.22d + calc.confidence * 0.18d + Math.max(0.0d, calc.causalLift) * 1.2d);
        }

        return 0.0d;
    }

    private double probabilityPenaltyScale(String level, MatrixCalc calc) {
        if (calc == null) {
            return 0.0d;
        }

        if (E_FALSE.equals(level)) {
            return clamp01(0.65d + calc.falseScore * 0.30d + calc.riskScore * 0.20d);
        }

        if (D_DECAY.equals(level)) {
            return clamp01(0.35d + calc.decayScore * 0.35d + calc.riskScore * 0.15d);
        }

        return 0.0d;
    }

    private String buildSummary(
            String targetIssue,
            int position,
            int number,
            String finalLevel,
            MatrixCalc c,
            ArrayList<AtomEval> evals) {
        StringBuilder sb = new StringBuilder();

        sb.append("【原因因果矩阵】");
        sb.append("目标期=");
        sb.append(safe(targetIssue));
        sb.append("｜位置=");
        sb.append(position);
        sb.append("｜号码=");
        sb.append(number);
        sb.append("\n");

        sb.append("【评级】");
        sb.append(finalLevel);
        sb.append("｜样本=");
        sb.append(c.sampleCount);
        sb.append("｜命中=");
        sb.append(c.hitCount);
        sb.append("｜未中=");
        sb.append(c.missCount);
        sb.append("｜命中率=");
        sb.append(percent(c.hitRate));
        sb.append("｜Wilson下界=");
        sb.append(percent(c.wilsonLower));
        sb.append("｜近期Wilson=");
        sb.append(percent(c.recentWilsonLower));
        sb.append("\n");

        sb.append("【因果强度】");
        sb.append("causalLift=");
        sb.append(signed(c.causalLift));
        sb.append("｜confidence=");
        sb.append(percent(c.confidence));
        sb.append("｜decayScore=");
        sb.append(percent(c.decayScore));
        sb.append("｜falseScore=");
        sb.append(percent(c.falseScore));
        sb.append("｜riskScore=");
        sb.append(percent(c.riskScore));
        sb.append("\n");

        sb.append("【A_VALID】");
        sb.append(c.validAtoms.size() > 0 ? joinAtoms(c.validAtoms) : "无");
        sb.append("\n");

        sb.append("【B_USEFUL】");
        sb.append(c.usefulAtoms.size() > 0 ? joinAtoms(c.usefulAtoms) : "无");
        sb.append("\n");

        sb.append("【C_EXPLAIN_ONLY】");
        sb.append(c.explainAtoms.size() > 0 ? joinAtoms(c.explainAtoms) : "无");
        sb.append("\n");

        sb.append("【D_DECAY】");
        sb.append(c.decayAtoms.size() > 0 ? joinAtoms(c.decayAtoms) : "无");
        sb.append("\n");

        sb.append("【E_FALSE】");
        sb.append(c.falseAtoms.size() > 0 ? joinAtoms(c.falseAtoms) : "无");
        sb.append("\n");

        sb.append("【结论】");
        if (A_VALID.equals(finalLevel)) {
            sb.append("原因样本足、Wilson下界高、近期不衰退，后续Probability允许正常加分。");
        } else if (B_USEFUL.equals(finalLevel)) {
            sb.append("原因有帮助但强度不足，后续Probability只能小幅加分。");
        } else if (C_EXPLAIN_ONLY.equals(finalLevel)) {
            sb.append("原因只能解释，样本或下界不足，后续Probability不能加分。");
        } else if (D_DECAY.equals(finalLevel)) {
            sb.append("原因历史有效但近期衰退，后续必须降权。");
        } else if (E_FALSE.equals(finalLevel)) {
            sb.append("原因更像伪原因或反证过强，后续必须强降权或禁止加分。");
        }
        sb.append("\n");

        sb.append("说明=本引擎只做因果评级，不直接BET；最终是否出手交给 DecisionGateEngine。");

        return sb.toString();
    }

    private String buildAtomText(ArrayList<AtomEval> evals) {
        if (evals == null || evals.size() == 0) {
            return "";
        }

        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < evals.size(); i++) {
            AtomEval e = evals.get(i);

            if (e == null) {
                continue;
            }

            if (sb.length() > 0) {
                sb.append("；");
            }

            sb.append(e.atom);
            sb.append("=");
            sb.append(e.level);
            sb.append("(样本=");
            sb.append(e.sampleCount);
            sb.append(",Wilson=");
            sb.append(percent(e.wilsonLower));
            sb.append(",近期=");
            sb.append(percent(e.recentWilsonLower));
            sb.append(",衰退=");
            sb.append(percent(e.decayScore));
            sb.append(",伪因=");
            sb.append(percent(e.falseScore));
            sb.append(")");
        }

        return sb.toString();
    }

    private void saveProfileIfPossible(DbStore dbStore, Models.ReasonCausalProfile profile) {
        if (dbStore == null || profile == null) {
            return;
        }

        Object v =
                call(
                        dbStore,
                        "saveReasonCausalProfile",
                        new Class[] {Models.ReasonCausalProfile.class},
                        new Object[] {profile});

        if (v != null) {
            return;
        }

        call(
                dbStore,
                "saveReasonCausalProfile",
                new Class[] {Object.class},
                new Object[] {profile});
    }

    private String buildProfileKey(
            String stateKey,
            String fingerprint,
            int position,
            int number,
            String majorAtomSignature) {
        String raw =
                safe(stateKey)
                        + "|FP="
                        + safe(fingerprint)
                        + "|P"
                        + position
                        + "|N"
                        + number
                        + "|A="
                        + safe(majorAtomSignature);

        return "RCA|" + shortMd5(raw);
    }

    private String buildAtomProfileKey(
            String stateKey, String fingerprint, int position, int number, String atom) {
        String raw =
                safe(stateKey)
                        + "|FP="
                        + safe(fingerprint)
                        + "|P"
                        + position
                        + "|N"
                        + number
                        + "|ATOM="
                        + safe(atom);

        return "ATOM_CAUSAL|" + shortMd5(raw);
    }

    private Object call(Object target, String methodName, Class[] types, Object[] args) {
        if (target == null || methodName == null || methodName.length() == 0) {
            return null;
        }

        try {
            Method m = target.getClass().getMethod(methodName, types);
            return m.invoke(target, args);
        } catch (Throwable e) {
            return null;
        }
    }

    private int getProfileInt(Object obj, String camel, String snake, int fallback) {
        int v = getInt(obj, camel, Integer.MIN_VALUE);

        if (v != Integer.MIN_VALUE) {
            return v;
        }

        v = getInt(obj, snake, Integer.MIN_VALUE);

        if (v != Integer.MIN_VALUE) {
            return v;
        }

        return fallback;
    }

    private double getProfileDouble(Object obj, String camel, String snake, double fallback) {
        double v = getDouble(obj, camel, Double.NaN);

        if (!Double.isNaN(v)) {
            return v;
        }

        v = getDouble(obj, snake, Double.NaN);

        if (!Double.isNaN(v)) {
            return v;
        }

        return fallback;
    }

    private int getMinSample() {
        return readAppInt("REASON_MIN_SAMPLE", DEFAULT_MIN_SAMPLE);
    }

    private int getStrongSample() {
        return readAppInt("REASON_STRONG_SAMPLE", DEFAULT_STRONG_SAMPLE);
    }

    private int getRecentWindow() {
        return readAppInt("REASON_RECENT_WINDOW", DEFAULT_RECENT_WINDOW);
    }

    private int readAppInt(String field, int fallback) {
        if (field == null || field.length() == 0) {
            return fallback;
        }

        try {
            Field f = AppConfig.class.getField(field);
            Object v = f.get(null);

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

    private LinkedHashSet<String> parseAtoms(String text) {
        LinkedHashSet<String> set = new LinkedHashSet<String>();

        if (text == null || text.length() == 0) {
            return set;
        }

        String normalized = text.replace('|', ',');
        normalized = normalized.replace(';', ',');
        normalized = normalized.replace('，', ',');

        String[] arr = normalized.split(",");

        for (int i = 0; i < arr.length; i++) {
            String s = cleanAtom(arr[i]);

            if (s.length() > 0) {
                set.add(s);
            }
        }

        return set;
    }

    private String cleanAtom(String s) {
        if (s == null) {
            return "";
        }

        String out = s.trim();

        if (out.length() <= 0) {
            return "";
        }

        out = out.replace("\n", "");
        out = out.replace("\r", "");
        out = out.replace("\t", "");

        if (out.length() > 80) {
            out = out.substring(0, 80);
        }

        return out;
    }

    private void addAll(LinkedHashSet<String> out, LinkedHashSet<String> add) {
        if (out == null || add == null) {
            return;
        }

        for (String s : add) {
            if (s != null && s.length() > 0) {
                out.add(s);
            }
        }
    }

    private String joinAtoms(LinkedHashSet<String> set) {
        if (set == null || set.size() == 0) {
            return "";
        }

        StringBuilder sb = new StringBuilder();

        for (String s : set) {
            if (s == null || s.length() == 0) {
                continue;
            }

            if (sb.length() > 0) {
                sb.append(",");
            }

            sb.append(s);
        }

        return sb.toString();
    }

    private String joinWith(List<String> list, String sep) {
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

    private String shortMd5(String raw) {
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
            return Integer.toHexString(raw.hashCode());
        }
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

    private double clampSigned(double v) {
        if (v < -1.0d) {
            return -1.0d;
        }

        if (v > 1.0d) {
            return 1.0d;
        }

        return v;
    }

    private String percent(double v) {
        return String.format(Locale.US, "%.1f%%", Double.valueOf(clamp01(v) * 100.0d));
    }

    private String signed(double v) {
        String sign = v >= 0.0d ? "+" : "";
        return sign + String.format(Locale.US, "%.3f", Double.valueOf(v));
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }

    private String firstNonEmpty(String a, String b) {
        if (a != null && a.length() > 0) {
            return a;
        }

        if (b != null) {
            return b;
        }

        return "";
    }

    private static int getInt(Object obj, String fieldName, int fallback) {
        if (obj == null || fieldName == null || fieldName.length() == 0) {
            return fallback;
        }

        try {
            if (obj instanceof Map) {
                Object v = ((Map) obj).get(fieldName);
                return parseIntObj(v, fallback);
            }

            Method getMethod = obj.getClass().getMethod("get", new Class[] {String.class});
            Object v = getMethod.invoke(obj, new Object[] {fieldName});
            return parseIntObj(v, fallback);
        } catch (Throwable e) {
        }

        try {
            Field f = obj.getClass().getField(fieldName);
            Object v = f.get(obj);
            return parseIntObj(v, fallback);
        } catch (Throwable e) {
        }

        return fallback;
    }

    private static long getLong(Object obj, String fieldName, long fallback) {
        if (obj == null || fieldName == null || fieldName.length() == 0) {
            return fallback;
        }

        try {
            if (obj instanceof Map) {
                Object v = ((Map) obj).get(fieldName);
                return parseLongObj(v, fallback);
            }

            Method getMethod = obj.getClass().getMethod("get", new Class[] {String.class});
            Object v = getMethod.invoke(obj, new Object[] {fieldName});
            return parseLongObj(v, fallback);
        } catch (Throwable e) {
        }

        try {
            Field f = obj.getClass().getField(fieldName);
            Object v = f.get(obj);
            return parseLongObj(v, fallback);
        } catch (Throwable e) {
        }

        return fallback;
    }

    private static double getDouble(Object obj, String fieldName, double fallback) {
        if (obj == null || fieldName == null || fieldName.length() == 0) {
            return fallback;
        }

        try {
            if (obj instanceof Map) {
                Object v = ((Map) obj).get(fieldName);
                return parseDoubleObj(v, fallback);
            }

            Method getMethod = obj.getClass().getMethod("get", new Class[] {String.class});
            Object v = getMethod.invoke(obj, new Object[] {fieldName});
            return parseDoubleObj(v, fallback);
        } catch (Throwable e) {
        }

        try {
            Field f = obj.getClass().getField(fieldName);
            Object v = f.get(obj);
            return parseDoubleObj(v, fallback);
        } catch (Throwable e) {
        }

        return fallback;
    }

    private static String getString(Object obj, String fieldName, String fallback) {
        if (obj == null || fieldName == null || fieldName.length() == 0) {
            return fallback;
        }

        try {
            if (obj instanceof Map) {
                Object v = ((Map) obj).get(fieldName);
                return v == null ? fallback : String.valueOf(v);
            }

            Method getMethod = obj.getClass().getMethod("get", new Class[] {String.class});
            Object v = getMethod.invoke(obj, new Object[] {fieldName});
            return v == null ? fallback : String.valueOf(v);
        } catch (Throwable e) {
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

    private static int parseIntObj(Object v, int fallback) {
        if (v == null) {
            return fallback;
        }

        try {
            if (v instanceof Integer) {
                return ((Integer) v).intValue();
            }

            if (v instanceof Long) {
                return (int) ((Long) v).longValue();
            }

            if (v instanceof Number) {
                return ((Number) v).intValue();
            }

            return Integer.parseInt(String.valueOf(v));
        } catch (Throwable e) {
            return fallback;
        }
    }

    private static long parseLongObj(Object v, long fallback) {
        if (v == null) {
            return fallback;
        }

        try {
            if (v instanceof Long) {
                return ((Long) v).longValue();
            }

            if (v instanceof Integer) {
                return ((Integer) v).longValue();
            }

            if (v instanceof Number) {
                return ((Number) v).longValue();
            }

            return Long.parseLong(String.valueOf(v));
        } catch (Throwable e) {
            return fallback;
        }
    }

    private static double parseDoubleObj(Object v, double fallback) {
        if (v == null) {
            return fallback;
        }

        try {
            if (v instanceof Double) {
                return ((Double) v).doubleValue();
            }

            if (v instanceof Float) {
                return ((Float) v).doubleValue();
            }

            if (v instanceof Number) {
                return ((Number) v).doubleValue();
            }

            return Double.parseDouble(String.valueOf(v));
        } catch (Throwable e) {
            return fallback;
        }
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

    private static final class AtomEval {
        String atom;
        String level;

        boolean positiveAtom;
        boolean negativeAtom;
        boolean contradictionAtom;

        int sampleCount;
        int hitCount;
        int missCount;
        int recentSample;
        int recentHit;
        int recentMiss;

        double hitRate;
        double recentHitRate;
        double wilsonLower;
        double recentWilsonLower;
        double causalLift;
        double recentLift;
        double confidence;
        double decayScore;
        double falseScore;
        double supportScore;
        double riskScore;
    }

    private static final class MatrixCalc {
        int sampleCount;
        int hitCount;
        int missCount;
        int recentSample;
        int recentHit;
        int recentMiss;

        double hitRate;
        double recentHitRate;
        double wilsonLower;
        double recentWilsonLower;
        double causalLift;
        double confidence;
        double decayScore;
        double falseScore;
        double supportScore;
        double riskScore;

        String majorAtomSignature = "";

        LinkedHashSet<String> validAtoms = new LinkedHashSet<String>();
        LinkedHashSet<String> usefulAtoms = new LinkedHashSet<String>();
        LinkedHashSet<String> explainAtoms = new LinkedHashSet<String>();
        LinkedHashSet<String> decayAtoms = new LinkedHashSet<String>();
        LinkedHashSet<String> falseAtoms = new LinkedHashSet<String>();
    }
}
