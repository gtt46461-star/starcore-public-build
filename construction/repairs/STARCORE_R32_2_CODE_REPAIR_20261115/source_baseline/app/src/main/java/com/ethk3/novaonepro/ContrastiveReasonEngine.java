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
 * ContrastiveReasonEngine.java
 *
 * <p>第7/17 新增核心引擎。
 *
 * <p>作用： 1. 中挂差异反推。 2. 输入当前候选开奖前原因 + 历史相似样本。 3. 输出 Models.ContrastiveProfile。 4. 判断当前候选更像历史 HIT
 * 组，还是更像历史 MISS 组，还是样本不足只能 SHADOW。
 *
 * <p>三级回退查样本： 1. 完整 reason_fingerprint 2. state_key + position + number 3. position + number +
 * major_reason_atoms
 *
 * <p>必须计算： hit_similarity_score miss_similarity_score reason_edge hit_factor_list miss_factor_list
 * difference_signature
 *
 * <p>注意： 本文件只做相似度与差异反推。 不直接出号、不直接BET、不结算、不写真实盈亏。
 */
public final class ContrastiveReasonEngine {

    public static final String LEVEL_READY = "READY";
    public static final String LEVEL_SHADOW = "SHADOW";
    public static final String LEVEL_RISK = "RISK";
    public static final String LEVEL_NO_SAMPLE = "NO_SAMPLE";

    public static final String LOOKUP_FINGERPRINT = "FINGERPRINT";
    public static final String LOOKUP_STATE_POSITION_NUMBER = "STATE_POSITION_NUMBER";
    public static final String LOOKUP_MAJOR_ATOMS = "MAJOR_ATOMS";
    public static final String LOOKUP_NONE = "NONE";

    private static final int DEFAULT_LIMIT = 240;
    private static final int MIN_READY_SAMPLE = 20;
    private static final int MIN_SHADOW_SAMPLE = 8;

    private static final double READY_HIT_SIMILARITY = 0.62d;
    private static final double READY_MISS_MAX = 0.36d;
    private static final double READY_REASON_EDGE = 0.16d;

    private static final double RISK_MISS_SIMILARITY = 0.52d;
    private static final double RISK_REASON_EDGE = -0.08d;

    public ContrastiveReasonEngine() {}

    public Models.ContrastiveProfile evaluate(Models.PreDrawReason reason, DbStore dbStore) {
        return evaluate("", reason, dbStore, DEFAULT_LIMIT);
    }

    public Models.ContrastiveProfile evaluate(
            String targetIssue, Models.PreDrawReason reason, DbStore dbStore) {
        return evaluate(targetIssue, reason, dbStore, DEFAULT_LIMIT);
    }

    public Models.ContrastiveProfile evaluate(
            String targetIssue, Models.PreDrawReason reason, DbStore dbStore, int limit) {
        Models.ContrastiveProfile profile = new Models.ContrastiveProfile();

        if (reason == null) {
            fillEmpty(profile, targetIssue, "EMPTY_REASON");
            return profile;
        }

        if (limit <= 0) {
            limit = DEFAULT_LIMIT;
        }

        int position = getInt(reason, "position", 0);
        int number = getInt(reason, "number", 0);
        String reasonKey = getString(reason, "reasonKey", position + "_" + number);
        String baseIssue = getString(reason, "baseIssue", "");
        String realTargetIssue = firstNonEmpty(targetIssue, getString(reason, "targetIssue", ""));
        String stateKey = getString(reason, "stateKey", "");
        String fingerprint = getString(reason, "reasonFingerprint", "");
        String positiveAtoms = getString(reason, "positiveAtoms", "");
        String negativeAtoms = getString(reason, "negativeAtoms", "");
        String contradictionAtoms = getString(reason, "contradictionAtoms", "");
        String reasonAtoms = getString(reason, "reasonAtoms", "");

        ArrayList<CaseRow> cases =
                loadSimilarCasesThreeStage(
                        dbStore,
                        fingerprint,
                        stateKey,
                        position,
                        number,
                        reasonAtoms,
                        positiveAtoms,
                        negativeAtoms,
                        contradictionAtoms,
                        limit);

        ContrastiveCalc calc = calculate(reason, cases);

        String differenceSignature =
                buildDifferenceSignature(
                        position,
                        number,
                        fingerprint,
                        calc.hitSimilarity,
                        calc.missSimilarity,
                        calc.reasonEdge,
                        calc.hitFactors,
                        calc.missFactors,
                        calc.lookupMode);

        String validLevel = judgeLevel(calc);

        String summary =
                buildSummary(
                        realTargetIssue, position, number, calc, validLevel, differenceSignature);

        putString(
                profile,
                "profileKey",
                buildProfileKey(realTargetIssue, position, number, fingerprint));
        putString(profile, "reasonKey", reasonKey);
        putString(profile, "baseIssue", baseIssue);
        putString(profile, "targetIssue", realTargetIssue);
        putString(profile, "stateKey", stateKey);
        putString(profile, "reasonFingerprint", fingerprint);
        putString(profile, "lookupMode", calc.lookupMode);
        putString(profile, "validLevel", validLevel);
        putString(profile, "hitFactorList", joinAtoms(calc.hitFactors));
        putString(profile, "missFactorList", joinAtoms(calc.missFactors));
        putString(profile, "differenceSignature", differenceSignature);
        putString(profile, "summaryText", summary);
        putString(profile, "contrastiveSummary", summary);
        putString(profile, "reasonAtoms", reasonAtoms);
        putString(profile, "positiveAtoms", positiveAtoms);
        putString(profile, "negativeAtoms", negativeAtoms);
        putString(profile, "contradictionAtoms", contradictionAtoms);

        putInt(profile, "position", position);
        putInt(profile, "number", number);
        putLong(profile, "createdAt", System.currentTimeMillis());
        putInt(profile, "sampleCount", calc.sampleCount);
        putInt(profile, "hitSample", calc.hitSample);
        putInt(profile, "missSample", calc.missSample);
        putInt(profile, "sameFingerprintSample", calc.sameFingerprintSample);
        putInt(profile, "sameStateSample", calc.sameStateSample);
        putInt(profile, "majorAtomSample", calc.majorAtomSample);

        putDouble(profile, "hitSimilarityScore", calc.hitSimilarity);
        putDouble(profile, "missSimilarityScore", calc.missSimilarity);
        putDouble(profile, "reasonEdge", calc.reasonEdge);
        putDouble(profile, "hitGroupAvgSimilarity", calc.hitGroupAvgSimilarity);
        putDouble(profile, "missGroupAvgSimilarity", calc.missGroupAvgSimilarity);
        putDouble(profile, "hitForwardRate", calc.hitForwardRate);
        putDouble(profile, "missForwardRate", calc.missForwardRate);
        putDouble(profile, "confidence", calc.confidence);
        putDouble(profile, "riskScore", calc.riskScore);
        putDouble(profile, "supportScore", calc.supportScore);
        putDouble(profile, "contradictionScore", getDouble(reason, "contradictionScore", 0.0d));

        return profile;
    }

    public Map<String, Models.ContrastiveProfile> evaluateAll(
            Map<String, Models.PreDrawReason> reasons, DbStore dbStore) {
        return evaluateAll("", reasons, dbStore, DEFAULT_LIMIT);
    }

    public Map<String, Models.ContrastiveProfile> evaluateAll(
            String targetIssue,
            Map<String, Models.PreDrawReason> reasons,
            DbStore dbStore,
            int limit) {
        LinkedHashMap<String, Models.ContrastiveProfile> out =
                new LinkedHashMap<String, Models.ContrastiveProfile>();

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
            Models.ContrastiveProfile p = evaluate(targetIssue, r, dbStore, limit);
            out.put(keyOf(getInt(r, "position", 0), getInt(r, "number", 0)), p);
        }

        return out;
    }

    public ArrayList<Models.ContrastiveProfile> evaluateList(
            String targetIssue,
            Map<String, Models.PreDrawReason> reasons,
            DbStore dbStore,
            int limit) {
        Map<String, Models.ContrastiveProfile> map =
                evaluateAll(targetIssue, reasons, dbStore, limit);
        return toSortedProfileList(map);
    }

    public ArrayList<Models.ContrastiveProfile> toSortedProfileList(
            Map<String, Models.ContrastiveProfile> map) {
        ArrayList<Models.ContrastiveProfile> list = new ArrayList<Models.ContrastiveProfile>();

        if (map == null || map.size() == 0) {
            return list;
        }

        for (Models.ContrastiveProfile p : map.values()) {
            if (p != null) {
                list.add(p);
            }
        }

        Collections.sort(
                list,
                new Comparator<Models.ContrastiveProfile>() {
                    public int compare(Models.ContrastiveProfile a, Models.ContrastiveProfile b) {
                        double ea = getDouble(a, "reasonEdge", 0.0d);
                        double eb = getDouble(b, "reasonEdge", 0.0d);

                        if (ea > eb) {
                            return -1;
                        }

                        if (ea < eb) {
                            return 1;
                        }

                        double ha = getDouble(a, "hitSimilarityScore", 0.0d);
                        double hb = getDouble(b, "hitSimilarityScore", 0.0d);

                        if (ha > hb) {
                            return -1;
                        }

                        if (ha < hb) {
                            return 1;
                        }

                        double ma = getDouble(a, "missSimilarityScore", 0.0d);
                        double mb = getDouble(b, "missSimilarityScore", 0.0d);

                        if (ma < mb) {
                            return -1;
                        }

                        if (ma > mb) {
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

    private void fillEmpty(Models.ContrastiveProfile profile, String targetIssue, String reason) {
        putString(profile, "profileKey", "EMPTY_CONTRASTIVE");
        putString(profile, "targetIssue", safe(targetIssue));
        putString(profile, "lookupMode", LOOKUP_NONE);
        putString(profile, "validLevel", LEVEL_NO_SAMPLE);
        putString(profile, "summaryText", "中挂差异反推失败：" + safe(reason));
        putString(profile, "contrastiveSummary", "中挂差异反推失败：" + safe(reason));
        putString(profile, "differenceSignature", "EMPTY|" + safe(reason));

        putInt(profile, "sampleCount", 0);
        putInt(profile, "hitSample", 0);
        putInt(profile, "missSample", 0);
        putDouble(profile, "hitSimilarityScore", 0.0d);
        putDouble(profile, "missSimilarityScore", 0.0d);
        putDouble(profile, "reasonEdge", 0.0d);
        putDouble(profile, "confidence", 0.0d);
        putDouble(profile, "riskScore", 1.0d);
        putDouble(profile, "supportScore", 0.0d);
        putLong(profile, "createdAt", System.currentTimeMillis());
    }

    private ArrayList<CaseRow> loadSimilarCasesThreeStage(
            DbStore dbStore,
            String fingerprint,
            String stateKey,
            int position,
            int number,
            String reasonAtoms,
            String positiveAtoms,
            String negativeAtoms,
            String contradictionAtoms,
            int limit) {
        ArrayList<CaseRow> out = new ArrayList<CaseRow>();

        if (dbStore == null) {
            return out;
        }

        if (limit <= 0) {
            limit = DEFAULT_LIMIT;
        }

        ArrayList<CaseRow> stage1 = loadByFingerprint(dbStore, fingerprint, limit);

        if (stage1.size() >= MIN_SHADOW_SAMPLE) {
            markLookupMode(stage1, LOOKUP_FINGERPRINT);
            return stage1;
        }

        ArrayList<CaseRow> stage2 =
                loadByStatePositionNumber(dbStore, stateKey, position, number, limit);

        if (stage1.size() > 0) {
            out.addAll(stage1);
        }

        if (stage2.size() > 0) {
            mergeCases(out, stage2);
        }

        if (out.size() >= MIN_SHADOW_SAMPLE) {
            markLookupMode(
                    out, stage1.size() > 0 ? LOOKUP_FINGERPRINT : LOOKUP_STATE_POSITION_NUMBER);
            return trimCases(out, limit);
        }

        String majorAtoms =
                buildMajorAtoms(reasonAtoms, positiveAtoms, negativeAtoms, contradictionAtoms);
        ArrayList<CaseRow> stage3 = loadByMajorAtoms(dbStore, position, number, majorAtoms, limit);

        if (stage3.size() > 0) {
            mergeCases(out, stage3);
        }

        if (out.size() > 0) {
            markLookupMode(
                    out,
                    stage1.size() > 0
                            ? LOOKUP_FINGERPRINT
                            : (stage2.size() > 0
                                    ? LOOKUP_STATE_POSITION_NUMBER
                                    : LOOKUP_MAJOR_ATOMS));
        }

        return trimCases(out, limit);
    }

    private ArrayList<CaseRow> loadByFingerprint(DbStore dbStore, String fingerprint, int limit) {
        ArrayList<CaseRow> list = new ArrayList<CaseRow>();

        if (dbStore == null || fingerprint == null || fingerprint.length() == 0) {
            return list;
        }

        Object result =
                call(
                        dbStore,
                        "loadSimilarReasonCases",
                        new Class[] {String.class, int.class},
                        new Object[] {fingerprint, Integer.valueOf(limit)});

        if (result == null) {
            result =
                    call(
                            dbStore,
                            "loadSimilarReasonCasesByFingerprint",
                            new Class[] {String.class, int.class},
                            new Object[] {fingerprint, Integer.valueOf(limit)});
        }

        appendRows(list, result, LOOKUP_FINGERPRINT);
        return list;
    }

    private ArrayList<CaseRow> loadByStatePositionNumber(
            DbStore dbStore, String stateKey, int position, int number, int limit) {
        ArrayList<CaseRow> list = new ArrayList<CaseRow>();

        if (dbStore == null
                || stateKey == null
                || stateKey.length() == 0
                || position <= 0
                || number <= 0) {
            return list;
        }

        Object result =
                call(
                        dbStore,
                        "loadSimilarReasonCases",
                        new Class[] {String.class, int.class, int.class, int.class},
                        new Object[] {
                            stateKey,
                            Integer.valueOf(position),
                            Integer.valueOf(number),
                            Integer.valueOf(limit)
                        });

        if (result == null) {
            result =
                    call(
                            dbStore,
                            "loadSimilarReasonCasesByState",
                            new Class[] {String.class, int.class, int.class, int.class},
                            new Object[] {
                                stateKey,
                                Integer.valueOf(position),
                                Integer.valueOf(number),
                                Integer.valueOf(limit)
                            });
        }

        appendRows(list, result, LOOKUP_STATE_POSITION_NUMBER);
        return list;
    }

    private ArrayList<CaseRow> loadByMajorAtoms(
            DbStore dbStore, int position, int number, String majorAtoms, int limit) {
        ArrayList<CaseRow> list = new ArrayList<CaseRow>();

        if (dbStore == null
                || position <= 0
                || number <= 0
                || majorAtoms == null
                || majorAtoms.length() == 0) {
            return list;
        }

        Object result =
                call(
                        dbStore,
                        "loadSimilarReasonCases",
                        new Class[] {int.class, int.class, String.class, int.class},
                        new Object[] {
                            Integer.valueOf(position),
                            Integer.valueOf(number),
                            majorAtoms,
                            Integer.valueOf(limit)
                        });

        if (result == null) {
            result =
                    call(
                            dbStore,
                            "loadSimilarReasonCasesByMajorAtoms",
                            new Class[] {int.class, int.class, String.class, int.class},
                            new Object[] {
                                Integer.valueOf(position),
                                Integer.valueOf(number),
                                majorAtoms,
                                Integer.valueOf(limit)
                            });
        }

        appendRows(list, result, LOOKUP_MAJOR_ATOMS);
        return list;
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

    private void appendRows(ArrayList<CaseRow> out, Object result, String lookupMode) {
        if (out == null || result == null) {
            return;
        }

        if (result instanceof List) {
            List list = (List) result;

            for (int i = 0; i < list.size(); i++) {
                CaseRow row = toCaseRow(list.get(i), lookupMode);

                if (row != null) {
                    out.add(row);
                }
            }

            return;
        }

        if (result instanceof Map) {
            Map map = (Map) result;

            for (Object value : map.values()) {
                CaseRow row = toCaseRow(value, lookupMode);

                if (row != null) {
                    out.add(row);
                }
            }

            return;
        }

        CaseRow row = toCaseRow(result, lookupMode);

        if (row != null) {
            out.add(row);
        }
    }

    private CaseRow toCaseRow(Object obj, String lookupMode) {
        if (obj == null) {
            return null;
        }

        CaseRow row = new CaseRow();

        row.lookupMode = safe(lookupMode);
        row.caseKey = firstNonEmpty(getString(obj, "caseKey", ""), getString(obj, "sampleKey", ""));
        row.targetIssue = getString(obj, "targetIssue", "");
        row.baseIssue = getString(obj, "baseIssue", "");
        row.stateKey = getString(obj, "stateKey", "");
        row.reasonFingerprint =
                firstNonEmpty(
                        getString(obj, "reasonFingerprint", ""),
                        getString(obj, "preDrawReasonFingerprint", ""));
        row.reasonAtoms = getString(obj, "reasonAtoms", "");
        row.positiveAtoms = getString(obj, "positiveAtoms", "");
        row.negativeAtoms = getString(obj, "negativeAtoms", "");
        row.contradictionAtoms = getString(obj, "contradictionAtoms", "");
        row.mainReasonCode =
                firstNonEmpty(
                        getString(obj, "mainReasonCode", ""),
                        getString(obj, "mainPositiveAtom", ""));
        row.affectedModule = getString(obj, "affectedModule", "");

        row.position = getInt(obj, "position", 0);
        row.number = firstNonZero(getInt(obj, "number", 0), getInt(obj, "actualNumber", 0));
        row.actualNumber = getInt(obj, "actualNumber", 0);
        row.k3Mask = firstNonZero(getInt(obj, "k3Mask", 0), getInt(obj, "predictedK3", 0));
        row.hit = readHit(obj);
        row.result = normalizeResult(getString(obj, "result", ""));

        row.reasonScore = getDouble(obj, "reasonScore", 0.0d);
        row.reasonEdge = getDouble(obj, "reasonEdge", 0.0d);
        row.positiveScore = getDouble(obj, "positiveScore", 0.0d);
        row.negativeScore = getDouble(obj, "negativeScore", 0.0d);
        row.contradictionScore = getDouble(obj, "contradictionScore", 0.0d);

        row.freqSlope = getDouble(obj, "freqSlope", 0.0d);
        row.positionDrift = getDouble(obj, "positionDrift", 0.0d);
        row.positionNoiseScore = getDouble(obj, "positionNoiseScore", 0.0d);
        row.gapZ = getDouble(obj, "gapZ", 0.0d);
        row.transitionLift = getDouble(obj, "transitionLift", 0.0d);
        row.transitionConflict = getDouble(obj, "transitionConflict", 0.0d);
        row.recoveryQuality = getDouble(obj, "recoveryQuality", 0.0d);
        row.falseRecoveryRisk = getDouble(obj, "falseRecoveryRisk", 0.0d);
        row.hotExhaustion = getDouble(obj, "hotExhaustion", 0.0d);
        row.breakDepth = getDouble(obj, "breakDepth", 0.0d);

        row.createdAt = getLong(obj, "createdAt", 0L);

        if (row.caseKey.length() == 0) {
            row.caseKey = buildCaseKey(row);
        }

        if (row.reasonAtoms.length() == 0) {
            row.reasonAtoms =
                    mergeAtomText(row.positiveAtoms, row.negativeAtoms, row.contradictionAtoms);
        }

        if (row.reasonFingerprint.length() == 0 && row.reasonAtoms.length() == 0) {
            return null;
        }

        return row;
    }

    private int readHit(Object obj) {
        int h = getInt(obj, "hit", -999);

        if (h == 1 || h == 0) {
            return h;
        }

        int isHit = getInt(obj, "isHit", -999);

        if (isHit == 1 || isHit == 0) {
            return isHit;
        }

        String result = normalizeResult(getString(obj, "result", ""));

        if ("WIN".equals(result) || "HIT".equals(result) || "命中".equals(result)) {
            return 1;
        }

        if ("MISS".equals(result) || "LOSE".equals(result) || "未中".equals(result)) {
            return 0;
        }

        return 0;
    }

    private ContrastiveCalc calculate(Models.PreDrawReason reason, ArrayList<CaseRow> cases) {
        ContrastiveCalc calc = new ContrastiveCalc();

        calc.lookupMode = LOOKUP_NONE;

        if (cases == null || cases.size() == 0) {
            calc.sampleCount = 0;
            calc.hitSample = 0;
            calc.missSample = 0;
            calc.hitSimilarity = 0.0d;
            calc.missSimilarity = 0.0d;
            calc.reasonEdge = 0.0d;
            calc.confidence = 0.0d;
            calc.riskScore = 1.0d;
            calc.supportScore = 0.0d;
            return calc;
        }

        String currentFingerprint = getString(reason, "reasonFingerprint", "");
        String currentState = getString(reason, "stateKey", "");
        int position = getInt(reason, "position", 0);
        int number = getInt(reason, "number", 0);

        LinkedHashSet<String> currentAll = parseAtoms(getString(reason, "reasonAtoms", ""));
        LinkedHashSet<String> currentPositive = parseAtoms(getString(reason, "positiveAtoms", ""));
        LinkedHashSet<String> currentNegative = parseAtoms(getString(reason, "negativeAtoms", ""));
        LinkedHashSet<String> currentCon = parseAtoms(getString(reason, "contradictionAtoms", ""));

        double hitSimilaritySum = 0.0d;
        double missSimilaritySum = 0.0d;
        double hitWeightSum = 0.0d;
        double missWeightSum = 0.0d;

        int hitSample = 0;
        int missSample = 0;
        int sameFingerprint = 0;
        int sameState = 0;
        int majorAtom = 0;

        LinkedHashMap<String, FactorScore> hitFactorMap = new LinkedHashMap<String, FactorScore>();
        LinkedHashMap<String, FactorScore> missFactorMap = new LinkedHashMap<String, FactorScore>();

        String firstLookup = "";

        for (int i = 0; i < cases.size(); i++) {
            CaseRow row = cases.get(i);

            if (row == null) {
                continue;
            }

            if (firstLookup.length() == 0
                    && row.lookupMode != null
                    && row.lookupMode.length() > 0) {
                firstLookup = row.lookupMode;
            }

            if (currentFingerprint.length() > 0
                    && currentFingerprint.equals(row.reasonFingerprint)) {
                sameFingerprint++;
            }

            if (currentState.length() > 0 && currentState.equals(row.stateKey)) {
                sameState++;
            }

            if (LOOKUP_MAJOR_ATOMS.equals(row.lookupMode)) {
                majorAtom++;
            }

            double sim =
                    similarity(
                            reason,
                            row,
                            currentAll,
                            currentPositive,
                            currentNegative,
                            currentCon,
                            position,
                            number);

            double weight = sampleWeight(row, sim);

            if (row.hit == 1) {
                hitSample++;
                hitSimilaritySum += sim * weight;
                hitWeightSum += weight;
                collectFactors(
                        hitFactorMap, row, currentPositive, currentNegative, currentCon, sim);
            } else {
                missSample++;
                missSimilaritySum += sim * weight;
                missWeightSum += weight;
                collectFactors(
                        missFactorMap, row, currentPositive, currentNegative, currentCon, sim);
            }
        }

        calc.sampleCount = hitSample + missSample;
        calc.hitSample = hitSample;
        calc.missSample = missSample;
        calc.sameFingerprintSample = sameFingerprint;
        calc.sameStateSample = sameState;
        calc.majorAtomSample = majorAtom;
        calc.lookupMode = firstLookup.length() == 0 ? LOOKUP_NONE : firstLookup;

        calc.hitSimilarity = hitWeightSum > 0.0d ? clamp01(hitSimilaritySum / hitWeightSum) : 0.0d;
        calc.missSimilarity =
                missWeightSum > 0.0d ? clamp01(missSimilaritySum / missWeightSum) : 0.0d;

        calc.hitGroupAvgSimilarity = calc.hitSimilarity;
        calc.missGroupAvgSimilarity = calc.missSimilarity;

        calc.hitForwardRate =
                calc.sampleCount > 0 ? ((double) hitSample) / ((double) calc.sampleCount) : 0.0d;
        calc.missForwardRate =
                calc.sampleCount > 0 ? ((double) missSample) / ((double) calc.sampleCount) : 0.0d;

        calc.reasonEdge =
                clampSigned(
                        (calc.hitSimilarity - calc.missSimilarity) * 0.78d
                                + (calc.hitForwardRate - calc.missForwardRate) * 0.22d);

        calc.confidence =
                confidenceFromSample(
                        calc.sampleCount, sameFingerprint, sameState, hitSample, missSample);

        calc.supportScore =
                clamp01(
                        calc.hitSimilarity * 0.55d
                                + calc.hitForwardRate * 0.25d
                                + Math.max(0.0d, calc.reasonEdge) * 0.20d);

        calc.riskScore =
                clamp01(
                        calc.missSimilarity * 0.52d
                                + calc.missForwardRate * 0.24d
                                + Math.max(0.0d, -calc.reasonEdge) * 0.24d);

        calc.hitFactors = topFactors(hitFactorMap, 6);
        calc.missFactors = topFactors(missFactorMap, 6);

        return calc;
    }

    private double similarity(
            Models.PreDrawReason current,
            CaseRow row,
            LinkedHashSet<String> currentAll,
            LinkedHashSet<String> currentPositive,
            LinkedHashSet<String> currentNegative,
            LinkedHashSet<String> currentCon,
            int position,
            int number) {
        if (row == null) {
            return 0.0d;
        }

        double score = 0.0d;
        double weight = 0.0d;

        String curFp = getString(current, "reasonFingerprint", "");
        String curState = getString(current, "stateKey", "");

        if (curFp.length() > 0 && curFp.equals(row.reasonFingerprint)) {
            score += 1.0d * 0.22d;
            weight += 0.22d;
        } else {
            weight += 0.22d;
        }

        if (curState.length() > 0 && curState.equals(row.stateKey)) {
            score += 1.0d * 0.14d;
            weight += 0.14d;
        } else {
            weight += 0.14d;
        }

        if (position > 0 && row.position == position) {
            score += 1.0d * 0.07d;
        }

        weight += 0.07d;

        if (number > 0 && row.number == number) {
            score += 1.0d * 0.07d;
        }

        weight += 0.07d;

        double allJ = jaccard(currentAll, parseAtoms(row.reasonAtoms));
        double posJ = jaccard(currentPositive, parseAtoms(row.positiveAtoms));
        double negJ = jaccard(currentNegative, parseAtoms(row.negativeAtoms));
        double conJ = jaccard(currentCon, parseAtoms(row.contradictionAtoms));

        score += allJ * 0.18d;
        score += posJ * 0.17d;
        score += negJ * 0.08d;
        score += conJ * 0.06d;
        weight += 0.49d;

        double numeric = numericSimilarity(current, row);
        score += numeric * 0.21d;
        weight += 0.21d;

        if (weight <= 0.0d) {
            return 0.0d;
        }

        return clamp01(score / weight);
    }

    private double numericSimilarity(Models.PreDrawReason current, CaseRow row) {
        if (current == null || row == null) {
            return 0.0d;
        }

        double s = 0.0d;
        double w = 0.0d;

        s += nearScore(getDouble(current, "freqSlope", 0.0d), row.freqSlope, 0.35d) * 0.12d;
        w += 0.12d;

        s += nearScore(getDouble(current, "positionDrift", 0.0d), row.positionDrift, 0.45d) * 0.10d;
        w += 0.10d;

        s +=
                nearScore(
                                getDouble(current, "positionNoiseScore", 0.0d),
                                row.positionNoiseScore,
                                0.45d)
                        * 0.12d;
        w += 0.12d;

        s += nearScore(getDouble(current, "gapZ", 0.0d), row.gapZ, 2.20d) * 0.10d;
        w += 0.10d;

        s +=
                nearScore(getDouble(current, "transitionLift", 0.0d), row.transitionLift, 0.45d)
                        * 0.12d;
        w += 0.12d;

        s +=
                nearScore(
                                getDouble(current, "transitionConflict", 0.0d),
                                row.transitionConflict,
                                0.45d)
                        * 0.12d;
        w += 0.12d;

        s +=
                nearScore(getDouble(current, "recoveryQuality", 0.0d), row.recoveryQuality, 0.45d)
                        * 0.14d;
        w += 0.14d;

        s +=
                nearScore(
                                getDouble(current, "falseRecoveryRisk", 0.0d),
                                row.falseRecoveryRisk,
                                0.45d)
                        * 0.14d;
        w += 0.14d;

        s += nearScore(getDouble(current, "hotExhaustion", 0.0d), row.hotExhaustion, 0.45d) * 0.07d;
        w += 0.07d;

        s += nearScore(getDouble(current, "breakDepth", 0.0d), row.breakDepth, 0.45d) * 0.07d;
        w += 0.07d;

        if (w <= 0.0d) {
            return 0.0d;
        }

        return clamp01(s / w);
    }

    private double sampleWeight(CaseRow row, double similarity) {
        if (row == null) {
            return 0.0d;
        }

        double w = 1.0d;

        if (LOOKUP_FINGERPRINT.equals(row.lookupMode)) {
            w += 0.35d;
        } else if (LOOKUP_STATE_POSITION_NUMBER.equals(row.lookupMode)) {
            w += 0.18d;
        } else if (LOOKUP_MAJOR_ATOMS.equals(row.lookupMode)) {
            w += 0.08d;
        }

        if (similarity >= 0.70d) {
            w += 0.22d;
        } else if (similarity >= 0.55d) {
            w += 0.10d;
        }

        if (row.createdAt > 0L) {
            long age = System.currentTimeMillis() - row.createdAt;
            long day = 24L * 60L * 60L * 1000L;

            if (age >= 0L && age <= day * 3L) {
                w += 0.10d;
            }
        }

        return w;
    }

    private void collectFactors(
            LinkedHashMap<String, FactorScore> map,
            CaseRow row,
            LinkedHashSet<String> curPositive,
            LinkedHashSet<String> curNegative,
            LinkedHashSet<String> curCon,
            double similarity) {
        if (map == null || row == null) {
            return;
        }

        addFactorSet(map, parseAtoms(row.positiveAtoms), curPositive, similarity, 1.00d);
        addFactorSet(map, parseAtoms(row.negativeAtoms), curNegative, similarity, 0.88d);
        addFactorSet(map, parseAtoms(row.contradictionAtoms), curCon, similarity, 0.78d);

        if (row.mainReasonCode != null && row.mainReasonCode.length() > 0) {
            addFactor(map, row.mainReasonCode, similarity * 0.70d);
        }

        if (row.affectedModule != null && row.affectedModule.length() > 0) {
            addFactor(map, "MODULE_" + row.affectedModule, similarity * 0.50d);
        }
    }

    private void addFactorSet(
            LinkedHashMap<String, FactorScore> map,
            LinkedHashSet<String> sampleAtoms,
            LinkedHashSet<String> currentAtoms,
            double similarity,
            double scale) {
        if (map == null || sampleAtoms == null || sampleAtoms.size() == 0) {
            return;
        }

        for (String atom : sampleAtoms) {
            if (atom == null || atom.length() == 0) {
                continue;
            }

            double w = similarity * scale;

            if (currentAtoms != null && currentAtoms.contains(atom)) {
                w += 0.25d;
            }

            addFactor(map, atom, w);
        }
    }

    private void addFactor(LinkedHashMap<String, FactorScore> map, String atom, double score) {
        if (map == null || atom == null || atom.length() == 0) {
            return;
        }

        FactorScore fs = map.get(atom);

        if (fs == null) {
            fs = new FactorScore();
            fs.atom = atom;
            map.put(atom, fs);
        }

        fs.count++;
        fs.score += score;
    }

    private LinkedHashSet<String> topFactors(LinkedHashMap<String, FactorScore> map, int limit) {
        LinkedHashSet<String> out = new LinkedHashSet<String>();

        if (map == null || map.size() == 0) {
            return out;
        }

        ArrayList<FactorScore> list = new ArrayList<FactorScore>();

        for (FactorScore f : map.values()) {
            if (f != null && f.atom != null && f.atom.length() > 0) {
                list.add(f);
            }
        }

        Collections.sort(
                list,
                new Comparator<FactorScore>() {
                    public int compare(FactorScore a, FactorScore b) {
                        double sa = a.score + a.count * 0.12d;
                        double sb = b.score + b.count * 0.12d;

                        if (sa > sb) {
                            return -1;
                        }

                        if (sa < sb) {
                            return 1;
                        }

                        return a.atom.compareTo(b.atom);
                    }
                });

        int max = limit <= 0 ? list.size() : Math.min(limit, list.size());

        for (int i = 0; i < max; i++) {
            out.add(list.get(i).atom);
        }

        return out;
    }

    private String judgeLevel(ContrastiveCalc calc) {
        if (calc == null || calc.sampleCount <= 0) {
            return LEVEL_NO_SAMPLE;
        }

        if (calc.sampleCount < MIN_SHADOW_SAMPLE) {
            return LEVEL_NO_SAMPLE;
        }

        if (calc.missSimilarity >= RISK_MISS_SIMILARITY || calc.reasonEdge <= RISK_REASON_EDGE) {
            return LEVEL_RISK;
        }

        if (calc.sampleCount >= MIN_READY_SAMPLE
                && calc.hitSimilarity >= READY_HIT_SIMILARITY
                && calc.missSimilarity <= READY_MISS_MAX
                && calc.reasonEdge >= READY_REASON_EDGE
                && calc.hitSample >= 5) {
            return LEVEL_READY;
        }

        return LEVEL_SHADOW;
    }

    private String buildSummary(
            String targetIssue,
            int position,
            int number,
            ContrastiveCalc calc,
            String level,
            String differenceSignature) {
        StringBuilder sb = new StringBuilder();

        sb.append("【中挂差异反推】");
        sb.append("目标期=");
        sb.append(safe(targetIssue));
        sb.append("｜位置=");
        sb.append(position);
        sb.append("｜号码=");
        sb.append(number);
        sb.append("\n");

        sb.append("【相似样本】");
        sb.append("lookup=");
        sb.append(calc.lookupMode);
        sb.append("｜总样本=");
        sb.append(calc.sampleCount);
        sb.append("｜HIT=");
        sb.append(calc.hitSample);
        sb.append("｜MISS=");
        sb.append(calc.missSample);
        sb.append("｜同指纹=");
        sb.append(calc.sameFingerprintSample);
        sb.append("｜同状态=");
        sb.append(calc.sameStateSample);
        sb.append("\n");

        sb.append("【HIT/MISS相似度】");
        sb.append("hit_similarity=");
        sb.append(percent(calc.hitSimilarity));
        sb.append("｜miss_similarity=");
        sb.append(percent(calc.missSimilarity));
        sb.append("｜reason_edge=");
        sb.append(signed(calc.reasonEdge));
        sb.append("｜confidence=");
        sb.append(percent(calc.confidence));
        sb.append("\n");

        sb.append("【更像哪一组】");
        if (LEVEL_READY.equals(level)) {
            sb.append("更像历史HIT组，且MISS相似度低，可作为后续概率/最终门的正向证据。");
        } else if (LEVEL_RISK.equals(level)) {
            sb.append("更像历史MISS组或反证过强，后续应压概率或提高门槛。");
        } else if (LEVEL_NO_SAMPLE.equals(level)) {
            sb.append("样本不足，只能解释或SHADOW，不能直接抬概率。");
        } else {
            sb.append("有一定HIT相似，但样本或边际不足，只能作为SHADOW级证据。");
        }
        sb.append("\n");

        sb.append("【HIT共同因】");
        sb.append(calc.hitFactors.size() > 0 ? joinAtoms(calc.hitFactors) : "无稳定HIT共同因");
        sb.append("\n");

        sb.append("【MISS共同因】");
        sb.append(calc.missFactors.size() > 0 ? joinAtoms(calc.missFactors) : "无稳定MISS共同因");
        sb.append("\n");

        sb.append("【difference_signature】");
        sb.append(differenceSignature);
        sb.append("\n");

        sb.append("说明=本引擎只做中挂差异反推，不直接BET，最终是否出手交给 DecisionGateEngine。");

        return sb.toString();
    }

    private String buildDifferenceSignature(
            int position,
            int number,
            String fingerprint,
            double hitSimilarity,
            double missSimilarity,
            double reasonEdge,
            LinkedHashSet<String> hitFactors,
            LinkedHashSet<String> missFactors,
            String lookupMode) {
        ArrayList<String> parts = new ArrayList<String>();

        parts.add("P" + position);
        parts.add("N" + number);
        parts.add("L=" + safe(lookupMode));
        parts.add("HIT=" + bucketSim(hitSimilarity));
        parts.add("MISS=" + bucketSim(missSimilarity));
        parts.add("EDGE=" + bucketEdge(reasonEdge));

        if (fingerprint != null && fingerprint.length() > 0) {
            parts.add("FP=" + shortMd5(fingerprint));
        }

        if (hitFactors != null && hitFactors.size() > 0) {
            parts.add("HF=" + shortMd5(joinAtoms(hitFactors)));
        }

        if (missFactors != null && missFactors.size() > 0) {
            parts.add("MF=" + shortMd5(joinAtoms(missFactors)));
        }

        String raw = joinWith(parts, "|");
        return "DS|" + raw + "|H=" + shortMd5(raw);
    }

    private String buildProfileKey(
            String targetIssue, int position, int number, String fingerprint) {
        String raw =
                safe(targetIssue) + "|P" + position + "|N" + number + "|FP=" + safe(fingerprint);
        return "CP|" + safe(targetIssue) + "|P" + position + "|N" + number + "|H=" + shortMd5(raw);
    }

    private String buildMajorAtoms(
            String reasonAtoms,
            String positiveAtoms,
            String negativeAtoms,
            String contradictionAtoms) {
        LinkedHashSet<String> all = new LinkedHashSet<String>();

        addLimitedAtoms(all, parseAtoms(positiveAtoms), 4);
        addLimitedAtoms(all, parseAtoms(negativeAtoms), 3);
        addLimitedAtoms(all, parseAtoms(contradictionAtoms), 2);

        if (all.size() <= 0) {
            addLimitedAtoms(all, parseAtoms(reasonAtoms), 5);
        }

        return joinAtoms(all);
    }

    private void addLimitedAtoms(
            LinkedHashSet<String> out, LinkedHashSet<String> atoms, int limit) {
        if (out == null || atoms == null || atoms.size() == 0 || limit <= 0) {
            return;
        }

        int count = 0;

        for (String s : atoms) {
            if (s == null || s.length() == 0) {
                continue;
            }

            out.add(s);
            count++;

            if (count >= limit) {
                break;
            }
        }
    }

    private void mergeCases(ArrayList<CaseRow> base, ArrayList<CaseRow> add) {
        if (base == null || add == null || add.size() == 0) {
            return;
        }

        LinkedHashSet<String> seen = new LinkedHashSet<String>();

        for (int i = 0; i < base.size(); i++) {
            CaseRow r = base.get(i);

            if (r != null) {
                seen.add(r.caseKey);
            }
        }

        for (int i = 0; i < add.size(); i++) {
            CaseRow r = add.get(i);

            if (r == null) {
                continue;
            }

            if (r.caseKey == null || r.caseKey.length() == 0) {
                r.caseKey = buildCaseKey(r);
            }

            if (seen.contains(r.caseKey)) {
                continue;
            }

            seen.add(r.caseKey);
            base.add(r);
        }
    }

    private ArrayList<CaseRow> trimCases(ArrayList<CaseRow> cases, int limit) {
        if (cases == null) {
            return new ArrayList<CaseRow>();
        }

        if (limit <= 0 || cases.size() <= limit) {
            return cases;
        }

        ArrayList<CaseRow> out = new ArrayList<CaseRow>();

        for (int i = 0; i < limit; i++) {
            out.add(cases.get(i));
        }

        return out;
    }

    private void markLookupMode(ArrayList<CaseRow> cases, String lookupMode) {
        if (cases == null) {
            return;
        }

        for (int i = 0; i < cases.size(); i++) {
            CaseRow r = cases.get(i);

            if (r != null && (r.lookupMode == null || r.lookupMode.length() == 0)) {
                r.lookupMode = lookupMode;
            }
        }
    }

    private double confidenceFromSample(int sample, int sameFp, int sameState, int hit, int miss) {
        if (sample <= 0) {
            return 0.0d;
        }

        double sampleScore = sample >= 80 ? 1.0d : ((double) sample) / 80.0d;
        double fpScore = sameFp >= 20 ? 1.0d : ((double) sameFp) / 20.0d;
        double stateScore = sameState >= 50 ? 1.0d : ((double) sameState) / 50.0d;
        double balance = 0.0d;

        if (hit > 0 && miss > 0) {
            balance = 1.0d;
        } else if (hit > 0 || miss > 0) {
            balance = 0.55d;
        }

        return clamp01(
                sampleScore * 0.48d + fpScore * 0.20d + stateScore * 0.18d + balance * 0.14d);
    }

    private double jaccard(LinkedHashSet<String> a, LinkedHashSet<String> b) {
        if ((a == null || a.size() == 0) && (b == null || b.size() == 0)) {
            return 0.0d;
        }

        if (a == null || b == null || a.size() == 0 || b.size() == 0) {
            return 0.0d;
        }

        int inter = 0;
        int union = 0;

        LinkedHashSet<String> all = new LinkedHashSet<String>();

        for (String s : a) {
            if (s != null && s.length() > 0) {
                all.add(s);
            }
        }

        for (String s : b) {
            if (s != null && s.length() > 0) {
                all.add(s);
            }
        }

        union = all.size();

        for (String s : a) {
            if (s != null && b.contains(s)) {
                inter++;
            }
        }

        if (union <= 0) {
            return 0.0d;
        }

        return clamp01(((double) inter) / ((double) union));
    }

    private double nearScore(double a, double b, double scale) {
        if (scale <= 0.0d) {
            scale = 1.0d;
        }

        double diff = Math.abs(a - b);
        return clamp01(1.0d - diff / scale);
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

    private String mergeAtomText(String a, String b, String c) {
        LinkedHashSet<String> set = new LinkedHashSet<String>();

        addAllAtoms(set, parseAtoms(a));
        addAllAtoms(set, parseAtoms(b));
        addAllAtoms(set, parseAtoms(c));

        return joinAtoms(set);
    }

    private void addAllAtoms(LinkedHashSet<String> out, LinkedHashSet<String> add) {
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

    private String bucketSim(double v) {
        if (v >= 0.72d) {
            return "VERY_HIGH";
        }

        if (v >= 0.62d) {
            return "HIGH";
        }

        if (v >= 0.45d) {
            return "MID";
        }

        if (v >= 0.25d) {
            return "LOW";
        }

        return "NONE";
    }

    private String bucketEdge(double v) {
        if (v >= 0.22d) {
            return "EDGE_STRONG";
        }

        if (v >= 0.10d) {
            return "EDGE_POS";
        }

        if (v <= -0.16d) {
            return "EDGE_NEG_STRONG";
        }

        if (v <= -0.06d) {
            return "EDGE_NEG";
        }

        return "EDGE_FLAT";
    }

    private String normalizeResult(String r) {
        if (r == null) {
            return "";
        }

        if ("WIN".equals(r) || "HIT".equals(r) || "命中".equals(r)) {
            return "WIN";
        }

        if ("MISS".equals(r) || "LOSE".equals(r) || "未中".equals(r)) {
            return "MISS";
        }

        return r;
    }

    private int firstNonZero(int a, int b) {
        return a != 0 ? a : b;
    }

    private String buildCaseKey(CaseRow row) {
        if (row == null) {
            return "";
        }

        String raw =
                safe(row.targetIssue)
                        + "|P"
                        + row.position
                        + "|N"
                        + row.number
                        + "|FP="
                        + safe(row.reasonFingerprint)
                        + "|A="
                        + safe(row.reasonAtoms);

        return "CASE|" + shortMd5(raw);
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

    private double clamp01(double v) {
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

    private static final class CaseRow {
        String lookupMode;
        String caseKey;
        String targetIssue;
        String baseIssue;
        String stateKey;
        String reasonFingerprint;
        String reasonAtoms;
        String positiveAtoms;
        String negativeAtoms;
        String contradictionAtoms;
        String mainReasonCode;
        String affectedModule;
        String result;

        int position;
        int number;
        int actualNumber;
        int k3Mask;
        int hit;

        double reasonScore;
        double reasonEdge;
        double positiveScore;
        double negativeScore;
        double contradictionScore;

        double freqSlope;
        double positionDrift;
        double positionNoiseScore;
        double gapZ;
        double transitionLift;
        double transitionConflict;
        double recoveryQuality;
        double falseRecoveryRisk;
        double hotExhaustion;
        double breakDepth;

        long createdAt;
    }

    private static final class FactorScore {
        String atom;
        int count;
        double score;
    }

    private static final class ContrastiveCalc {
        String lookupMode;

        int sampleCount;
        int hitSample;
        int missSample;
        int sameFingerprintSample;
        int sameStateSample;
        int majorAtomSample;

        double hitSimilarity;
        double missSimilarity;
        double reasonEdge;
        double hitGroupAvgSimilarity;
        double missGroupAvgSimilarity;
        double hitForwardRate;
        double missForwardRate;
        double confidence;
        double riskScore;
        double supportScore;

        LinkedHashSet<String> hitFactors = new LinkedHashSet<String>();
        LinkedHashSet<String> missFactors = new LinkedHashSet<String>();
    }

    // V57_B08_EVIDENCE_ONLY_FULL_IMPLEMENTATION

    public static final String V57_B08_OWNER = "EVIDENCE_ONLY:ContrastiveReasonEngine";

    public static final class V57ContrastEvidence {
        public String moduleA = "";
        public String moduleB = "";
        public double gainA;
        public double gainB;
        public double gainTogether;
        public double correlation;
        public double overlap;
        public String relation = "UNKNOWN";
        public String action = "WATCH";
        public String hash = "";

        public String canonical() {
            return moduleA
                    + '|'
                    + moduleB
                    + '|'
                    + gainA
                    + '|'
                    + gainB
                    + '|'
                    + gainTogether
                    + '|'
                    + correlation
                    + '|'
                    + overlap
                    + '|'
                    + relation
                    + '|'
                    + action;
        }
    }

    public static V57ContrastEvidence v57b08Contrast(
            String a,
            String b,
            double gainA,
            double gainB,
            double together,
            double correlation,
            double overlap) {
        V57ContrastEvidence e = new V57ContrastEvidence();
        e.moduleA = a == null ? "" : a;
        e.moduleB = b == null ? "" : b;
        e.gainA = gainA;
        e.gainB = gainB;
        e.gainTogether = together;
        e.correlation = correlation;
        e.overlap = overlap;
        if (together < Math.max(gainA, gainB) - 0.005d) {
            e.relation = "CONFLICT";
            e.action = "STATE_ROUTE_OR_DOWNWEIGHT";
        } else if (overlap >= 0.75d || Math.abs(correlation) >= 0.85d) {
            e.relation = "REDUNDANT";
            e.action = "DEDUPLICATE";
        } else if (together > Math.max(gainA, gainB) + 0.005d) {
            e.relation = "COMPLEMENTARY";
            e.action = "KEEP_IF_OOS_PROVEN";
        } else {
            e.relation = "INDEPENDENT_UNCLEAR";
            e.action = "MORE_OOS";
        }
        e.hash = v57b08Hash(e.canonical());
        return e;
    }

    public static boolean v57b08CanDirectlyAddFinalProbability() {
        return false;
    }

    private static String v57b08Hash(String text) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] b = md.digest((text == null ? "" : text).getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder(64);
            for (int i = 0; i < b.length; i++) {
                int v = b[i] & 255;
                if (v < 16) sb.append('0');
                sb.append(Integer.toHexString(v));
            }
            return sb.toString();
        } catch (Throwable t) {
            return "HASH_ERROR";
        }
    }
}
