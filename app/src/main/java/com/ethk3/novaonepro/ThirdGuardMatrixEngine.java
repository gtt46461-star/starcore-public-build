package com.ethk3.novaonepro;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * ThirdGuardMatrixEngine.java
 *
 * <p>第11/25 新增核心引擎。
 *
 * <p>作用： 1. 第三防线矩阵，只做候选风控审计。 2. 接在概率/因链/教师号码/反事实排序之后。 3. 检查： - 伪强候选 - 热尽风险 - 冷补假恢复 - 位置漂移 - 教师号码冲突
 * - 反事实排名冲突 - 相近候选冲突 - 样本不足却被放大的风险 - 连挂风险 4. 输出 Models.ThirdGuardStat。 5. 将 thirdGuardScore /
 * thirdGuardRisk / thirdGuardPass / thirdGuardSummary 回写 CandidateK3 与 CauseChain。
 *
 * <p>红线： - 不创建BET。 - 不结算。 - 不写真实账本。 - 不写真实校准。 - 不降低门槛。 - 不绕过 DecisionGateEngine。
 */
public final class ThirdGuardMatrixEngine {
    public static final String OUMAN_GALAXY_REBUILD_VERSION =
            AppConfig.OUMAN_GALAXY_REBUILD_VERSION;

    private static final int POSITION_MIN = 1;
    private static final int POSITION_MAX = 10;
    private static final int NUMBER_MIN = 1;
    private static final int NUMBER_MAX = 10;

    private static final double PASS_SCORE_MIN = 0.48d;
    private static final double PASS_RISK_MAX = 0.62d;
    private static final double HARD_RISK = 0.78d;

    public static final String THIRD_GUARD_MAIN_CHAIN_VERSION =
            "V44_NOVA_DIAGNOSIS_FREQUENCY_STABILITY_20260720_44157296";
    public static final String THIRD_GUARD_CLOSED_LOOP_VERSION =
            "V44_NOVA_DIAGNOSIS_FREQUENCY_STABILITY_20260720_44157296";
    private static final double THIRD_QUALITY_READY = 0.520d;
    private static final double THIRD_QUALITY_WEAK = 0.380d;
    private static final double THIRD_DRAG_RISK_LINE = 0.620d;
    private static final double THIRD_HARD_DRAG_LINE = 0.780d;

    private static final String LINK_V2_VERSION =
            "V44_NOVA_DIAGNOSIS_FREQUENCY_STABILITY_20260720_44157296";
    private static final int REALTIME_FULL_LIMIT = 40;
    private static final int REALTIME_AUDIT_LIMIT = 120;
    private static final long REALTIME_BUDGET_MS = 900L;
    private static final String THIRD_FRESH_HOT = "HOT_REALTIME";
    private static final String THIRD_FRESH_LIGHT = "HOT_LIGHT_AUDIT";
    private static final String THIRD_FRESH_DEFERRED = "DEFERRED_BACKGROUND";
    private static final String THIRD_STATUS_WATCH_NEUTRAL = "WATCH_NEUTRAL";

    public ThirdGuardMatrixEngine() {}

    /** 第13/22 实时主链入口：支持 ComboCandidate / 反射候选。 只写第三号守门、反证、拒绝码、动作建议字段，不创建 BET，不创建 Trade。 */
    public ArrayList<Models.ComboCandidate> applyToAll(
            DbStore store,
            ArrayList<Models.ComboCandidate> candidates,
            List<Models.Draw> clean,
            String baseIssue,
            String targetIssue) {
        return applyRealtimeThirdGuard(store, candidates, baseIssue, targetIssue, "applyToAll");
    }

    public ArrayList<Models.ComboCandidate> enrichCandidates(
            DbStore store,
            ArrayList<Models.ComboCandidate> candidates,
            List<Models.Draw> clean,
            String baseIssue,
            String targetIssue) {
        return applyRealtimeThirdGuard(
                store, candidates, baseIssue, targetIssue, "enrichCandidates");
    }

    public ArrayList<Models.ComboCandidate> analyzeCandidates(
            DbStore store,
            ArrayList<Models.ComboCandidate> candidates,
            List<Models.Draw> clean,
            String baseIssue,
            String targetIssue) {
        return applyRealtimeThirdGuard(
                store, candidates, baseIssue, targetIssue, "analyzeCandidates");
    }

    public ArrayList<Models.ComboCandidate> applyRealtime(
            ArrayList<Models.ComboCandidate> candidates,
            List<Models.Draw> clean,
            String baseIssue,
            String targetIssue) {
        return applyRealtimeThirdGuard(null, candidates, baseIssue, targetIssue, "applyRealtime");
    }

    public ArrayList<Models.ComboCandidate> runRealtime(
            ArrayList<Models.ComboCandidate> candidates,
            List<Models.Draw> clean,
            String baseIssue,
            String targetIssue) {
        return applyRealtimeThirdGuard(null, candidates, baseIssue, targetIssue, "runRealtime");
    }

    public int applyAndCount(DbStore store, List candidates, String baseIssue, String targetIssue) {
        return applyThirdGuardToRawCandidates(
                store, candidates, baseIssue, targetIssue, "applyAndCount");
    }

    private ArrayList<Models.ComboCandidate> applyRealtimeThirdGuard(
            DbStore store,
            ArrayList<Models.ComboCandidate> candidates,
            String baseIssue,
            String targetIssue,
            String source) {
        ArrayList<Models.ComboCandidate> out =
                candidates == null ? new ArrayList<Models.ComboCandidate>() : candidates;
        applyThirdGuardToRawCandidates(store, out, baseIssue, targetIssue, source);
        return out;
    }

    private int applyThirdGuardToRawCandidates(
            DbStore store, List candidates, String baseIssue, String targetIssue, String source) {
        if (candidates == null || candidates.size() == 0) {
            writeThirdGuardState(store, source, 0, 0, 0, 0, "第三号守门跳过：候选为空");
            return 0;
        }
        long startAt = System.currentTimeMillis();
        int applied = 0;
        int passCount = 0;
        int weakThird = 0;
        int hardRisk = 0;
        int lightAudit = 0;
        int deferred = 0;
        String first = "";
        ArrayList<Object> ranked = new ArrayList<Object>();
        for (int i = 0; i < candidates.size(); i++) {
            Object o = candidates.get(i);
            if (o != null) {
                ranked.add(o);
            }
        }
        Collections.sort(
                ranked,
                new Comparator<Object>() {
                    public int compare(Object a, Object b) {
                        double sa = rawCandidateScore(a);
                        double sb = rawCandidateScore(b);
                        if (sa > sb) return -1;
                        if (sa < sb) return 1;
                        return getInt(a, "position", 0) - getInt(b, "position", 0);
                    }
                });
        for (int i = 0; i < ranked.size(); i++) {
            Object c = ranked.get(i);
            if (c == null) {
                continue;
            }
            boolean overBudget = System.currentTimeMillis() - startAt >= REALTIME_BUDGET_MS;
            if (i >= REALTIME_AUDIT_LIMIT || overBudget) {
                applyDeferredThirdGuard(
                        c,
                        i + 1,
                        THIRD_FRESH_DEFERRED,
                        overBudget ? "THIRD_GUARD_BUDGET_DEFERRED" : "THIRD_GUARD_RANK_DEFERRED");
                deferred++;
                continue;
            }
            if (i >= REALTIME_FULL_LIMIT) {
                applyLightThirdGuard(c, i + 1);
                lightAudit++;
                applied++;
                continue;
            }
            ThirdGuardObjectResult r =
                    buildObjectThirdGuard(baseIssue, targetIssue, c, i + 1, ranked, source);
            applyThirdGuardObjectResult(c, r);
            putString(c, "thirdFreshness", THIRD_FRESH_HOT);
            putString(c, "thirdGuardFreshness", THIRD_FRESH_HOT);
            putInt(c, "thirdQualityAvailable", r.thirdQuality > 0.0d ? 1 : 0);
            applied++;
            if (r.pass == 1) passCount++;
            if (r.thirdWeakness >= THIRD_QUALITY_WEAK) weakThird++;
            if (r.hardBlock == 1 || r.thirdDragRisk >= THIRD_HARD_DRAG_LINE) hardRisk++;
            if (first.length() == 0) {
                first = r.summary;
            }
        }
        writeThirdGuardState(
                store,
                source,
                applied,
                passCount,
                weakThird,
                hardRisk,
                first
                        + "｜FULL="
                        + Math.min(ranked.size(), REALTIME_FULL_LIMIT)
                        + "｜LIGHT="
                        + lightAudit
                        + "｜DEFER="
                        + deferred
                        + "｜耗时="
                        + (System.currentTimeMillis() - startAt)
                        + "ms"
                        + "｜版本="
                        + LINK_V2_VERSION);
        return applied;
    }

    private ThirdGuardObjectResult buildObjectThirdGuard(
            String baseIssue,
            String targetIssue,
            Object c,
            int rankNo,
            ArrayList<Object> ranked,
            String source) {
        ThirdGuardObjectResult r = new ThirdGuardObjectResult();
        r.position = getInt(c, "position", 0);
        r.n1 = getInt(c, "n1", 0);
        r.n2 = getInt(c, "n2", 0);
        r.n3 = getInt(c, "n3", 0);
        r.k3Mask = getInt(c, "k3Mask", getInt(c, "k3_mask", 0));
        r.baseIssue = safe(baseIssue);
        r.targetIssue = safe(targetIssue);
        r.rankNo = rankNo;
        r.thirdQuality = calcThirdQualityFromObject(c);
        r.thirdWeakness = clamp01(1.0d - r.thirdQuality);
        r.thirdDragRisk = calcThirdDragRiskFromObject(c, r.thirdQuality, ranked);
        r.thirdSupplyScore =
                clamp01(
                        getDouble(
                                                c,
                                                "numberSupplyScore",
                                                getDouble(c, "numberSupplyStrength", 0.0d))
                                        * 0.55d
                                + r.thirdQuality * 0.45d);
        r.thirdFalseStrongRisk =
                clamp01(
                        getDouble(c, "falseStrongRisk", 0.0d) * 0.34d
                                + getDouble(c, "counterfactualRisk", 0.0d) * 0.26d
                                + r.thirdDragRisk * 0.40d);
        r.guardScore =
                clamp01(
                        rawCandidateScore(c) * 0.30d
                                + r.thirdQuality * 0.38d
                                + r.thirdSupplyScore * 0.18d
                                + getDouble(
                                                c,
                                                "coverageQuality",
                                                getDouble(c, "coverageQualityScore", 0.0d))
                                        * 0.14d);
        r.guardRisk =
                clamp01(
                        r.thirdDragRisk * 0.42d
                                + r.thirdWeakness * 0.25d
                                + r.thirdFalseStrongRisk * 0.20d
                                + getDouble(c, "riskScore", 0.0d) * 0.13d);
        r.hardBlock = (r.guardRisk >= HARD_RISK || r.thirdDragRisk >= THIRD_HARD_DRAG_LINE) ? 1 : 0;
        r.pass =
                (r.hardBlock == 0
                                && r.thirdQuality >= THIRD_QUALITY_READY
                                && r.guardRisk <= PASS_RISK_MAX)
                        ? 1
                        : 0;
        r.rejectCode = buildThirdRejectCode(r);
        r.actionAdvice = buildThirdActionAdvice(r);
        r.summary = buildThirdObjectSummary(r, source);
        return r;
    }

    private void applyThirdGuardObjectResult(Object c, ThirdGuardObjectResult r) {
        if (c == null || r == null) {
            return;
        }
        putDouble(c, "thirdQuality", r.thirdQuality);
        putDouble(c, "thirdNumberQuality", r.thirdQuality);
        putDouble(c, "thirdNumberQualityScore", r.thirdQuality);
        putDouble(
                c,
                "needSupplyThirdQuality",
                Math.max(getDouble(c, "needSupplyThirdQuality", 0.0d), r.thirdQuality));
        putDouble(c, "thirdWeakness", r.thirdWeakness);
        putDouble(c, "thirdDragRisk", r.thirdDragRisk);
        putDouble(c, "thirdDragScore", r.thirdDragRisk);
        putDouble(c, "thirdSupplyScore", r.thirdSupplyScore);
        putDouble(c, "thirdFalseStrongRisk", r.thirdFalseStrongRisk);
        putDouble(c, "thirdGuardScore", r.guardScore);
        putDouble(c, "thirdGuardRisk", r.guardRisk);
        putDouble(c, "thirdContribution", clamp01(r.guardScore * (1.0d - r.guardRisk)));
        putDouble(c, "thirdConflict", clamp01(Math.max(r.thirdDragRisk, r.thirdFalseStrongRisk)));
        putInt(c, "thirdGuardPass", r.pass);
        putInt(c, "thirdGuardHardBlock", r.hardBlock);
        putInt(c, "thirdHardRisk", r.hardBlock);
        putInt(c, "thirdQualityAvailable", r.thirdQuality > 0.0d ? 1 : 0);
        putString(c, "thirdFreshness", THIRD_FRESH_HOT);
        putString(c, "thirdGuardFreshness", THIRD_FRESH_HOT);
        putString(c, "thirdRejectCode", r.rejectCode);
        putString(c, "thirdActionAdvice", r.actionAdvice);
        putString(c, "thirdGuardActionAdvice", r.actionAdvice);
        putString(c, "thirdGuardSummary", r.summary);
        putString(c, "thirdGuardVersion", LINK_V2_VERSION);
        putString(c, "fieldChainVersion", THIRD_GUARD_CLOSED_LOOP_VERSION);
        if (r.rejectCode.length() > 0) {
            putString(c, "rejectCode", r.rejectCode);
            putString(c, "finalDecisionRejectCode", r.rejectCode);
        }
        putDouble(
                c,
                "antiEvidenceScore",
                Math.max(getDouble(c, "antiEvidenceScore", 0.0d), r.guardRisk));
        putDouble(
                c,
                "counterEvidenceScore",
                Math.max(getDouble(c, "counterEvidenceScore", 0.0d), r.thirdDragRisk));
        putDouble(
                c,
                "netEvidenceScore",
                clampSigned(
                        getDouble(c, "evidenceScore", 0.0d)
                                - Math.max(getDouble(c, "antiEvidenceScore", 0.0d), r.guardRisk)));
        String old = getString(c, "reasonSummary", "");
        if (old == null || old.length() == 0) {
            putString(c, "reasonSummary", r.summary);
        } else if (old.indexOf("第三号守门") < 0 && old.indexOf("第三防线") < 0) {
            putString(c, "reasonSummary", old + "｜" + r.summary);
        }
    }

    private void applyLightThirdGuard(Object c, int rankNo) {
        if (c == null) return;
        double quality = calcThirdQualityFromObject(c);
        double conflict =
                clamp01(
                        Math.max(
                                getDouble(
                                        c,
                                        "counterfactualRisk",
                                        getDouble(c, "counterfactualWhyNotRisk", 0.0d)),
                                getDouble(c, "thirdDragRisk", 0.0d)));
        double contribution = clamp01(quality * (1.0d - conflict));
        int hard = conflict >= THIRD_HARD_DRAG_LINE ? 1 : 0;
        putDouble(c, "thirdQuality", quality);
        putDouble(c, "thirdNumberQuality", quality);
        putDouble(c, "thirdNumberQualityScore", quality);
        putDouble(c, "thirdContribution", contribution);
        putDouble(c, "thirdConflict", conflict);
        putDouble(c, "thirdGuardRisk", conflict);
        putInt(c, "thirdHardRisk", hard);
        putInt(c, "thirdGuardHardBlock", hard);
        putInt(c, "thirdQualityAvailable", quality > 0.0d ? 1 : 0);
        putString(c, "thirdFreshness", THIRD_FRESH_LIGHT);
        putString(c, "thirdGuardFreshness", THIRD_FRESH_LIGHT);
        putString(c, "thirdGuardStatus", hard == 1 ? "HARD_RISK" : THIRD_STATUS_WATCH_NEUTRAL);
        putString(c, "thirdGuardActionAdvice", hard == 1 ? "HOLD_OR_SHADOW" : "WATCH_NEUTRAL");
        putString(c, "thirdGuardVersion", LINK_V2_VERSION);
        putString(
                c,
                "thirdGuardSummary",
                "第三号轻量审计｜rank="
                        + rankNo
                        + "｜quality="
                        + percent(quality)
                        + "｜conflict="
                        + percent(conflict)
                        + "｜fresh="
                        + THIRD_FRESH_LIGHT
                        + "｜不直接BET");
    }

    private void applyDeferredThirdGuard(Object c, int rankNo, String freshness, String reason) {
        if (c == null) return;
        double quality =
                getDouble(
                        c,
                        "thirdQuality",
                        getDouble(
                                c,
                                "thirdNumberQualityScore",
                                getDouble(c, "needSupplyThirdQuality", 0.0d)));
        putInt(c, "thirdQualityAvailable", quality > 0.0d ? 1 : 0);
        putInt(c, "thirdHardRisk", 0);
        putInt(c, "thirdGuardHardBlock", 0);
        putString(c, "thirdFreshness", freshness);
        putString(c, "thirdGuardFreshness", freshness);
        putString(c, "thirdGuardStatus", THIRD_STATUS_WATCH_NEUTRAL);
        putString(c, "thirdGuardActionAdvice", "BACKGROUND_RECHECK");
        putString(c, "thirdGuardVersion", LINK_V2_VERSION);
        putString(
                c,
                "thirdGuardSummary",
                "第三号守门延后｜rank="
                        + rankNo
                        + "｜reason="
                        + safe(reason)
                        + "｜状态=WATCH_NEUTRAL"
                        + "｜说明=未计算不等于质量为0，不允许以缺失字段进入频率探针");
    }

    private void writeThirdGuardState(
            DbStore store,
            String source,
            int applied,
            int passCount,
            int weakThird,
            int hardRisk,
            String summary) {
        if (store == null) {
            return;
        }
        try {
            store.setState("third_guard_matrix_version", LINK_V2_VERSION);
            store.setState("third_guard_matrix_source", safe(source));
            store.setState("third_guard_matrix_applied", String.valueOf(applied));
            store.setState("third_guard_matrix_pass", String.valueOf(passCount));
            store.setState("third_guard_matrix_weak_third", String.valueOf(weakThird));
            store.setState("third_guard_matrix_hard_risk", String.valueOf(hardRisk));
            store.setState("third_guard_matrix_summary", safe(summary));
            store.setState(
                    "third_guard_matrix_updated_at", String.valueOf(System.currentTimeMillis()));
        } catch (Throwable e) {
        }
    }

    public ArrayList<Models.ThirdGuardStat> buildThirdGuardStats(
            String baseIssue,
            String targetIssue,
            List<Models.CandidateK3> candidates,
            Map<String, Models.NumberTeacherCell> teacherMap,
            List<Models.RankCounterfactual> counterfactuals,
            Map<Integer, Models.PositionHealth> positionHealthMap,
            List<Models.CauseChain> chains,
            List<Models.Trade> recentTrades) {
        ArrayList<Models.ThirdGuardStat> out = new ArrayList<Models.ThirdGuardStat>();

        if (candidates == null || candidates.size() == 0) {
            return out;
        }

        ArrayList<Models.CandidateK3> list = normalizeCandidates(candidates);

        for (int i = 0; i < list.size(); i++) {
            Models.CandidateK3 c = list.get(i);

            if (c == null) {
                continue;
            }

            Models.ThirdGuardStat stat =
                    buildOneStat(
                            baseIssue,
                            targetIssue,
                            c,
                            i + 1,
                            list,
                            teacherMap,
                            counterfactuals,
                            positionHealthMap,
                            chains,
                            recentTrades);

            out.add(stat);
        }

        applyToCandidates(list, out);
        applyToCauseChains(chains, out);

        return out;
    }

    public ArrayList<Models.ThirdGuardStat> buildThirdGuardStats(
            String baseIssue, String targetIssue, List<Models.CandidateK3> candidates) {
        return buildThirdGuardStats(
                baseIssue, targetIssue, candidates, null, null, null, null, null);
    }

    public Models.ThirdGuardStat buildChampionGuard(
            String baseIssue,
            String targetIssue,
            List<Models.CandidateK3> candidates,
            Map<String, Models.NumberTeacherCell> teacherMap,
            List<Models.RankCounterfactual> counterfactuals,
            Map<Integer, Models.PositionHealth> positionHealthMap,
            List<Models.CauseChain> chains,
            List<Models.Trade> recentTrades) {
        ArrayList<Models.ThirdGuardStat> list =
                buildThirdGuardStats(
                        baseIssue,
                        targetIssue,
                        candidates,
                        teacherMap,
                        counterfactuals,
                        positionHealthMap,
                        chains,
                        recentTrades);

        if (list.size() == 0) {
            return null;
        }

        return list.get(0);
    }

    public void applyToCandidates(
            List<Models.CandidateK3> candidates, ArrayList<Models.ThirdGuardStat> stats) {
        if (candidates == null || stats == null || candidates.size() == 0 || stats.size() == 0) {
            return;
        }

        LinkedHashMap<String, Models.ThirdGuardStat> map =
                new LinkedHashMap<String, Models.ThirdGuardStat>();

        for (int i = 0; i < stats.size(); i++) {
            Models.ThirdGuardStat s = stats.get(i);

            if (s == null) {
                continue;
            }

            int p = getInt(s, "position", 0);
            int mask = getInt(s, "k3Mask", getInt(s, "k3_mask", 0));

            map.put(candidateKey(p, mask), s);
        }

        for (int i = 0; i < candidates.size(); i++) {
            Models.CandidateK3 c = candidates.get(i);

            if (c == null) {
                continue;
            }

            int p = getInt(c, "position", 0);
            int mask = getInt(c, "k3Mask", getInt(c, "k3_mask", 0));

            Models.ThirdGuardStat s = map.get(candidateKey(p, mask));

            if (s == null) {
                continue;
            }

            applyOneToCandidate(c, s);
        }

        rerankByThirdGuard(candidates);
    }

    public void applyToCauseChains(
            List<Models.CauseChain> chains, ArrayList<Models.ThirdGuardStat> stats) {
        if (chains == null || stats == null || chains.size() == 0 || stats.size() == 0) {
            return;
        }

        for (int i = 0; i < chains.size(); i++) {
            Models.CauseChain chain = chains.get(i);

            if (chain == null) {
                continue;
            }

            int position = getInt(chain, "position", 0);
            int number = getInt(chain, "number", 0);

            if (!isValidPosition(position) || !isValidNumber(number)) {
                continue;
            }

            Models.ThirdGuardStat best = findBestStatForNumber(stats, position, number);

            if (best == null) {
                continue;
            }

            double guardScore =
                    getDouble(best, "thirdGuardScore", getDouble(best, "guardScore", 0.0d));
            double guardRisk =
                    getDouble(best, "thirdGuardRisk", getDouble(best, "guardRisk", 0.0d));
            int pass = getInt(best, "thirdGuardPass", getInt(best, "pass", 0));

            double oldRisk = getDouble(chain, "riskScore", 0.0d);
            double oldFinal =
                    getDouble(chain, "finalCauseScore", getDouble(chain, "causeScore", 0.0d));
            double guardCause = clampSigned((guardScore - guardRisk) * 1.10d);

            double newFinal = oldFinal;

            if (pass == 1) {
                newFinal = clamp01(oldFinal + posPart(guardCause) * 0.030d);
            } else {
                newFinal = clamp01(oldFinal - Math.max(0.020d, guardRisk * 0.050d));
            }

            double newRisk = clamp01(Math.max(oldRisk, guardRisk));

            putDouble(chain, "thirdGuardScore", guardScore);
            putDouble(chain, "thirdGuardRisk", guardRisk);
            putDouble(chain, "thirdGuardCause", guardCause);
            putInt(chain, "thirdGuardPass", pass);
            putDouble(chain, "riskScore", newRisk);
            putDouble(chain, "finalCauseScore", newFinal);
            putDouble(chain, "causeScore", newFinal);

            String old = getString(chain, "summaryText", "");
            String add =
                    "｜第三防线：guardScore="
                            + percent(guardScore)
                            + "，guardRisk="
                            + percent(guardRisk)
                            + "，pass="
                            + pass
                            + "，说明=只做风险审计，不直接放行BET";

            putString(chain, "summaryText", old + add);
        }
    }

    private Models.ThirdGuardStat buildOneStat(
            String baseIssue,
            String targetIssue,
            Models.CandidateK3 c,
            int rankNo,
            ArrayList<Models.CandidateK3> ranked,
            Map<String, Models.NumberTeacherCell> teacherMap,
            List<Models.RankCounterfactual> counterfactuals,
            Map<Integer, Models.PositionHealth> positionHealthMap,
            List<Models.CauseChain> chains,
            List<Models.Trade> recentTrades) {
        Models.ThirdGuardStat s = new Models.ThirdGuardStat();

        int position = getInt(c, "position", 0);
        int n1 = getInt(c, "n1", 0);
        int n2 = getInt(c, "n2", 0);
        int n3 = getInt(c, "n3", 0);
        int mask = getInt(c, "k3Mask", getInt(c, "k3_mask", 0));

        double baseScore = candidateScore(c);
        double finalP = getDouble(c, "finalP", 0.0d);
        double edge = getDouble(c, "edge", 0.0d);
        double candidateRisk = getDouble(c, "riskScore", 0.0d);
        double causeScore = getDouble(c, "causeScore", 0.0d);

        GuardParts gp = new GuardParts();

        gp.positionHealth = calcPositionHealth(positionHealthMap, position);
        gp.positionRisk = calcPositionRisk(positionHealthMap, position);
        gp.teacherScore = calcTeacherScore(position, n1, n2, n3, teacherMap);
        gp.teacherRisk = calcTeacherRisk(position, n1, n2, n3, teacherMap);
        gp.rankScore = calcRankScore(position, mask, counterfactuals);
        gp.rankRisk = calcRankRisk(position, mask, counterfactuals);
        gp.chainScore = calcChainScore(position, n1, n2, n3, chains);
        gp.chainRisk = calcChainRisk(position, n1, n2, n3, chains);

        gp.falseStrongRisk = calcFalseStrongRisk(c, gp, rankNo, ranked);
        gp.hotExhaustionRisk = calcHotExhaustionRisk(c, teacherMap);
        gp.coldFakeRecoveryRisk = calcColdFakeRecoveryRisk(c, teacherMap);
        gp.positionDriftRisk = gp.positionRisk;
        gp.teacherConflictRisk = calcTeacherConflictRisk(gp);
        gp.rankConflictRisk = calcRankConflictRisk(position, mask, counterfactuals);
        gp.nearCandidateConflictRisk = calcNearCandidateConflictRisk(c, ranked);
        gp.weakSampleAmplifyRisk = calcWeakSampleAmplifyRisk(c, teacherMap, counterfactuals);
        gp.loseStreakRisk = calcLoseStreakRisk(recentTrades);
        gp.thirdQuality = calcThirdQuality(c, teacherMap, counterfactuals);
        gp.thirdWeakness = clamp01(1.0d - gp.thirdQuality);
        gp.thirdSupplyScore = calcThirdSupplyScore(c, gp);
        gp.thirdDragRisk = calcThirdDragRisk(c, gp, ranked);
        gp.thirdFalseStrongRisk =
                clamp01(
                        gp.thirdDragRisk * 0.46d
                                + gp.falseStrongRisk * 0.24d
                                + gp.teacherConflictRisk * 0.16d
                                + gp.rankConflictRisk * 0.14d);
        gp.thirdRejectCode = buildThirdRejectCode(gp);
        gp.thirdActionAdvice = buildThirdActionAdvice(gp);

        double support =
                baseScore * 0.20d
                        + finalP * 0.12d
                        + posPart(edge * 4.0d) * 0.08d
                        + causeScore * 0.14d
                        + gp.positionHealth * 0.12d
                        + gp.teacherScore * 0.12d
                        + gp.rankScore * 0.12d
                        + gp.chainScore * 0.10d
                        + gp.thirdQuality * 0.10d
                        + gp.thirdSupplyScore * 0.06d;

        double risk =
                candidateRisk * 0.12d
                        + gp.positionRisk * 0.10d
                        + gp.teacherRisk * 0.10d
                        + gp.rankRisk * 0.10d
                        + gp.chainRisk * 0.08d
                        + gp.falseStrongRisk * 0.13d
                        + gp.hotExhaustionRisk * 0.07d
                        + gp.coldFakeRecoveryRisk * 0.07d
                        + gp.teacherConflictRisk * 0.08d
                        + gp.rankConflictRisk * 0.08d
                        + gp.nearCandidateConflictRisk * 0.07d
                        + gp.weakSampleAmplifyRisk * 0.06d
                        + gp.loseStreakRisk * 0.08d
                        + gp.thirdDragRisk * 0.10d
                        + gp.thirdFalseStrongRisk * 0.07d;

        double guardScore = clamp01(support - risk * 0.42d + 0.08d);
        double guardRisk = clamp01(risk + Math.max(0.0d, 0.44d - support) * 0.18d);

        int hardBlock = isHardBlock(gp, guardRisk) ? 1 : 0;
        if (gp.thirdDragRisk >= THIRD_HARD_DRAG_LINE || gp.thirdWeakness >= 0.760d) {
            hardBlock = 1;
        }
        int pass =
                (hardBlock == 0 && guardScore >= PASS_SCORE_MIN && guardRisk <= PASS_RISK_MAX)
                        ? 1
                        : 0;

        String level = buildLevel(pass, hardBlock, guardScore, guardRisk);
        String summary =
                buildSummary(
                        rankNo,
                        position,
                        n1,
                        n2,
                        n3,
                        baseScore,
                        guardScore,
                        guardRisk,
                        pass,
                        hardBlock,
                        level,
                        gp);

        putString(s, "guardKey", buildGuardKey(targetIssue, position, mask));
        putString(s, "statKey", buildGuardKey(targetIssue, position, mask));
        putString(s, "baseIssue", safe(baseIssue));
        putString(s, "targetIssue", safe(targetIssue));
        putLong(s, "createdAt", System.currentTimeMillis());

        putInt(s, "rankNo", rankNo);
        putInt(s, "rank", rankNo);
        putInt(s, "position", position);
        putInt(s, "n1", n1);
        putInt(s, "n2", n2);
        putInt(s, "n3", n3);
        putInt(s, "k3Mask", mask);
        putInt(s, "k3_mask", mask);

        putDouble(s, "baseScore", baseScore);
        putDouble(s, "finalP", finalP);
        putDouble(s, "edge", edge);
        putDouble(s, "causeScore", causeScore);
        putDouble(s, "candidateRisk", candidateRisk);

        putDouble(s, "thirdGuardScore", guardScore);
        putDouble(s, "guardScore", guardScore);
        putDouble(s, "thirdGuardRisk", guardRisk);
        putDouble(s, "guardRisk", guardRisk);
        putInt(s, "thirdGuardPass", pass);
        putInt(s, "pass", pass);
        putInt(s, "hardBlock", hardBlock);

        putDouble(s, "positionHealth", gp.positionHealth);
        putDouble(s, "positionRisk", gp.positionRisk);
        putDouble(s, "teacherScore", gp.teacherScore);
        putDouble(s, "teacherRisk", gp.teacherRisk);
        putDouble(s, "rankScore", gp.rankScore);
        putDouble(s, "rankRisk", gp.rankRisk);
        putDouble(s, "chainScore", gp.chainScore);
        putDouble(s, "chainRisk", gp.chainRisk);

        putDouble(s, "falseStrongRisk", gp.falseStrongRisk);
        putDouble(s, "hotExhaustionRisk", gp.hotExhaustionRisk);
        putDouble(s, "coldFakeRecoveryRisk", gp.coldFakeRecoveryRisk);
        putDouble(s, "positionDriftRisk", gp.positionDriftRisk);
        putDouble(s, "teacherConflictRisk", gp.teacherConflictRisk);
        putDouble(s, "rankConflictRisk", gp.rankConflictRisk);
        putDouble(s, "nearCandidateConflictRisk", gp.nearCandidateConflictRisk);
        putDouble(s, "weakSampleAmplifyRisk", gp.weakSampleAmplifyRisk);
        putDouble(s, "loseStreakRisk", gp.loseStreakRisk);
        putDouble(s, "thirdQuality", gp.thirdQuality);
        putDouble(s, "thirdNumberQuality", gp.thirdQuality);
        putDouble(s, "thirdNumberQualityScore", gp.thirdQuality);
        putDouble(s, "thirdWeakness", gp.thirdWeakness);
        putDouble(s, "thirdDragRisk", gp.thirdDragRisk);
        putDouble(s, "thirdDragScore", gp.thirdDragRisk);
        putDouble(s, "thirdSupplyScore", gp.thirdSupplyScore);
        putDouble(s, "thirdFalseStrongRisk", gp.thirdFalseStrongRisk);
        putString(s, "thirdRejectCode", gp.thirdRejectCode);
        putString(s, "thirdActionAdvice", gp.thirdActionAdvice);
        putString(s, "thirdGuardVersion", THIRD_GUARD_MAIN_CHAIN_VERSION);
        putString(s, "fieldChainVersion", THIRD_GUARD_CLOSED_LOOP_VERSION);

        putString(s, "level", level);
        putString(s, "status", level);
        putString(s, "summaryText", summary);
        putString(s, "reasonText", summary);

        return s;
    }

    private ArrayList<Models.CandidateK3> normalizeCandidates(List<Models.CandidateK3> candidates) {
        ArrayList<Models.CandidateK3> list = new ArrayList<Models.CandidateK3>();

        if (candidates == null) {
            return list;
        }

        for (int i = 0; i < candidates.size(); i++) {
            Models.CandidateK3 c = candidates.get(i);

            if (c == null) {
                continue;
            }

            int p = getInt(c, "position", 0);
            int n1 = getInt(c, "n1", 0);
            int n2 = getInt(c, "n2", 0);
            int n3 = getInt(c, "n3", 0);

            if (!isValidPosition(p)
                    || !isValidNumber(n1)
                    || !isValidNumber(n2)
                    || !isValidNumber(n3)) {
                continue;
            }

            list.add(c);
        }

        Collections.sort(
                list,
                new Comparator<Models.CandidateK3>() {
                    public int compare(Models.CandidateK3 a, Models.CandidateK3 b) {
                        double sa = candidateScore(a);
                        double sb = candidateScore(b);

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

                        int ma = getInt(a, "k3Mask", getInt(a, "k3_mask", 0));
                        int mb = getInt(b, "k3Mask", getInt(b, "k3_mask", 0));

                        return ma - mb;
                    }
                });

        for (int i = 0; i < list.size(); i++) {
            Models.CandidateK3 c = list.get(i);
            putInt(c, "rankNo", i + 1);
            putInt(c, "rank", i + 1);
        }

        return list;
    }

    private static double candidateScore(Models.CandidateK3 c) {
        if (c == null) {
            return 0.0d;
        }

        double finalP = getDouble(c, "finalP", 0.0d);
        double edge = getDouble(c, "edge", 0.0d);
        double cause = getDouble(c, "causeScore", 0.0d);
        double teacher =
                getDouble(c, "teacherNumberScore", getDouble(c, "teacherComboScore", 0.0d));
        double rank =
                getDouble(c, "counterfactualScore", getDouble(c, "counterfactualRankScore", 0.0d));
        double position =
                getDouble(c, "positionCauseScore", getDouble(c, "positionHealthScore", 0.0d));
        double confidence =
                getDouble(c, "confidence", getDouble(c, "teacherNumberConfidence", 0.0d));
        double risk = getDouble(c, "riskScore", 0.0d);
        double teacherRisk = getDouble(c, "teacherNumberRisk", 0.0d);
        double rankRisk = getDouble(c, "counterfactualRisk", 0.0d);

        double score = 0.0d;

        score += finalP * 0.24d;
        score += posPart(edge * 4.0d) * 0.10d;
        score += cause * 0.20d;
        score += teacher * 0.12d;
        score += rank * 0.12d;
        score += position * 0.10d;
        score += confidence * 0.06d;

        score -= risk * 0.12d;
        score -= teacherRisk * 0.06d;
        score -= rankRisk * 0.06d;

        return clamp01(score);
    }

    private double calcPositionHealth(Map<Integer, Models.PositionHealth> map, int position) {
        if (map == null) {
            return 0.0d;
        }

        Models.PositionHealth h = map.get(Integer.valueOf(position));

        if (h == null) {
            return 0.0d;
        }

        return getDouble(h, "healthScore", getDouble(h, "positionHealthScore", 0.0d));
    }

    private double calcPositionRisk(Map<Integer, Models.PositionHealth> map, int position) {
        if (map == null) {
            return 0.0d;
        }

        Models.PositionHealth h = map.get(Integer.valueOf(position));

        if (h == null) {
            return 0.0d;
        }

        double risk = getDouble(h, "riskScore", getDouble(h, "positionRiskScore", 0.0d));
        double wrong = getDouble(h, "wrongRisk", getDouble(h, "positionWrongRisk", 0.0d));
        double drift = getDouble(h, "driftRisk", getDouble(h, "positionDriftRisk", 0.0d));
        double falseStrong =
                getDouble(h, "falseStrongRisk", getDouble(h, "positionFalseStrongRisk", 0.0d));

        return clamp01(risk * 0.32d + wrong * 0.28d + drift * 0.22d + falseStrong * 0.18d);
    }

    private double calcTeacherScore(
            int position, int n1, int n2, int n3, Map<String, Models.NumberTeacherCell> map) {
        if (map == null) {
            return 0.0d;
        }

        int[] nums = new int[] {n1, n2, n3};
        double sum = 0.0d;
        int used = 0;

        for (int i = 0; i < nums.length; i++) {
            Models.NumberTeacherCell c = getTeacherCell(map, position, nums[i]);

            if (c == null) {
                continue;
            }

            sum += getDouble(c, "teacherScore", getDouble(c, "numberTeacherScore", 0.0d));
            used++;
        }

        if (used <= 0) {
            return 0.0d;
        }

        return clamp01(sum / used);
    }

    private double calcTeacherRisk(
            int position, int n1, int n2, int n3, Map<String, Models.NumberTeacherCell> map) {
        if (map == null) {
            return 0.0d;
        }

        int[] nums = new int[] {n1, n2, n3};
        double sum = 0.0d;
        int used = 0;

        for (int i = 0; i < nums.length; i++) {
            Models.NumberTeacherCell c = getTeacherCell(map, position, nums[i]);

            if (c == null) {
                continue;
            }

            sum += getDouble(c, "teacherRisk", getDouble(c, "numberTeacherRisk", 0.0d));
            used++;
        }

        if (used <= 0) {
            return 0.0d;
        }

        return clamp01(sum / used);
    }

    private Models.NumberTeacherCell getTeacherCell(
            Map<String, Models.NumberTeacherCell> map, int position, int number) {
        if (map == null) {
            return null;
        }

        Models.NumberTeacherCell c = map.get(keyOf(position, number));

        if (c != null) {
            return c;
        }

        c = map.get(String.valueOf(position) + "|" + String.valueOf(number));

        if (c != null) {
            return c;
        }

        return map.get("P" + position + "_N" + number);
    }

    private double calcRankScore(int position, int mask, List<Models.RankCounterfactual> list) {
        Models.RankCounterfactual r = findRank(position, mask, list);

        if (r == null) {
            return 0.0d;
        }

        return getDouble(r, "rankScore", getDouble(r, "counterfactualScore", 0.0d));
    }

    private double calcRankRisk(int position, int mask, List<Models.RankCounterfactual> list) {
        Models.RankCounterfactual r = findRank(position, mask, list);

        if (r == null) {
            return 0.0d;
        }

        double missed = getDouble(r, "missedStrongRisk", 0.0d);
        double near = getDouble(r, "nearConflictRisk", 0.0d);
        double falseChampion = getDouble(r, "falseChampionRisk", 0.0d);
        double same = getDouble(r, "samePositionReplaceRisk", 0.0d);
        double cross = getDouble(r, "crossPositionReplaceRisk", 0.0d);

        return clamp01(
                missed * 0.28d
                        + near * 0.22d
                        + falseChampion * 0.24d
                        + same * 0.13d
                        + cross * 0.13d);
    }

    private Models.RankCounterfactual findRank(
            int position, int mask, List<Models.RankCounterfactual> list) {
        if (list == null) {
            return null;
        }

        for (int i = 0; i < list.size(); i++) {
            Models.RankCounterfactual r = list.get(i);

            if (r == null) {
                continue;
            }

            int p = getInt(r, "position", getInt(r, "chosenPosition", 0));
            int m = getInt(r, "k3Mask", getInt(r, "chosenK3Mask", 0));

            if (p == position && m == mask) {
                return r;
            }
        }

        return null;
    }

    private double calcChainScore(
            int position, int n1, int n2, int n3, List<Models.CauseChain> chains) {
        if (chains == null) {
            return 0.0d;
        }

        double sum = 0.0d;
        int used = 0;

        for (int i = 0; i < chains.size(); i++) {
            Models.CauseChain c = chains.get(i);

            if (c == null) {
                continue;
            }

            int p = getInt(c, "position", 0);
            int n = getInt(c, "number", 0);

            if (p != position) {
                continue;
            }

            if (n != n1 && n != n2 && n != n3) {
                continue;
            }

            sum += getDouble(c, "finalCauseScore", getDouble(c, "causeScore", 0.0d));
            used++;
        }

        if (used <= 0) {
            return 0.0d;
        }

        return clamp01(sum / used);
    }

    private double calcChainRisk(
            int position, int n1, int n2, int n3, List<Models.CauseChain> chains) {
        if (chains == null) {
            return 0.0d;
        }

        double sum = 0.0d;
        int used = 0;

        for (int i = 0; i < chains.size(); i++) {
            Models.CauseChain c = chains.get(i);

            if (c == null) {
                continue;
            }

            int p = getInt(c, "position", 0);
            int n = getInt(c, "number", 0);

            if (p != position) {
                continue;
            }

            if (n != n1 && n != n2 && n != n3) {
                continue;
            }

            sum += getDouble(c, "riskScore", 0.0d);
            used++;
        }

        if (used <= 0) {
            return 0.0d;
        }

        return clamp01(sum / used);
    }

    private double calcFalseStrongRisk(
            Models.CandidateK3 c, GuardParts gp, int rankNo, ArrayList<Models.CandidateK3> ranked) {
        double risk = 0.0d;

        double finalP = getDouble(c, "finalP", 0.0d);
        double dynamicGate = getDouble(c, "dynamicGate", getDouble(c, "gate", 0.0d));
        double edge = getDouble(c, "edge", finalP - dynamicGate);
        double cause = getDouble(c, "causeScore", 0.0d);
        double rawRisk = getDouble(c, "riskScore", 0.0d);

        if (finalP > 0.0d && dynamicGate > 0.0d && edge < 0.010d) {
            risk += 0.12d;
        }

        if (rankNo == 1 && ranked != null && ranked.size() > 1) {
            double first = candidateScore(ranked.get(0));
            double second = candidateScore(ranked.get(1));

            if (first - second < 0.035d) {
                risk += 0.16d;
            }
        }

        if (finalP >= 0.40d && cause < 0.42d) {
            risk += 0.14d;
        }

        if (gp.teacherScore < 0.36d && gp.teacherRisk > 0.50d) {
            risk += 0.14d;
        }

        if (gp.rankRisk > 0.55d) {
            risk += 0.12d;
        }

        if (gp.positionRisk > 0.58d) {
            risk += 0.12d;
        }

        risk += rawRisk * 0.18d;

        return clamp01(risk);
    }

    private double calcHotExhaustionRisk(
            Models.CandidateK3 c, Map<String, Models.NumberTeacherCell> teacherMap) {
        int position = getInt(c, "position", 0);
        int[] nums = new int[] {getInt(c, "n1", 0), getInt(c, "n2", 0), getInt(c, "n3", 0)};

        double sumHotRisk = 0.0d;
        int used = 0;

        for (int i = 0; i < nums.length; i++) {
            Models.NumberTeacherCell cell = getTeacherCell(teacherMap, position, nums[i]);

            if (cell == null) {
                continue;
            }

            double hotCold = getDouble(cell, "hotColdAdjust", 0.0d);
            double teacherRisk =
                    getDouble(cell, "teacherRisk", getDouble(cell, "numberTeacherRisk", 0.0d));
            double hitRate = getDouble(cell, "hitRate", 0.0d);
            double recent = getDouble(cell, "recentHitRate", 0.0d);

            double local = 0.0d;

            if (hotCold > 0.20d && recent < hitRate) {
                local += 0.18d;
            }

            if (teacherRisk > 0.55d) {
                local += teacherRisk * 0.26d;
            }

            sumHotRisk += clamp01(local);
            used++;
        }

        if (used <= 0) {
            return getDouble(c, "hotReverseRisk", 0.0d);
        }

        return clamp01((sumHotRisk / used) + getDouble(c, "hotReverseRisk", 0.0d) * 0.35d);
    }

    private double calcColdFakeRecoveryRisk(
            Models.CandidateK3 c, Map<String, Models.NumberTeacherCell> teacherMap) {
        int position = getInt(c, "position", 0);
        int[] nums = new int[] {getInt(c, "n1", 0), getInt(c, "n2", 0), getInt(c, "n3", 0)};

        double risk = 0.0d;
        int used = 0;

        for (int i = 0; i < nums.length; i++) {
            Models.NumberTeacherCell cell = getTeacherCell(teacherMap, position, nums[i]);

            if (cell == null) {
                continue;
            }

            double returnSupport = getDouble(cell, "returnSupport", 0.0d);
            double transition = getDouble(cell, "transitionSupport", 0.0d);
            double falsePenalty = getDouble(cell, "falseStrongPenalty", 0.0d);
            double teacherRisk = getDouble(cell, "teacherRisk", 0.0d);

            double local = 0.0d;

            if (returnSupport > 0.42d && transition < 0.16d) {
                local += 0.16d;
            }

            local += falsePenalty * 0.28d;
            local += teacherRisk * 0.16d;

            risk += clamp01(local);
            used++;
        }

        if (used <= 0) {
            return getDouble(c, "falseStrongRisk", 0.0d);
        }

        return clamp01((risk / used) + getDouble(c, "falseStrongRisk", 0.0d) * 0.28d);
    }

    private double calcTeacherConflictRisk(GuardParts gp) {
        if (gp == null) {
            return 0.0d;
        }

        double risk = 0.0d;

        if (gp.teacherScore < 0.38d && gp.teacherRisk > 0.48d) {
            risk += 0.22d;
        }

        if (gp.teacherRisk > gp.teacherScore + 0.16d) {
            risk += 0.22d;
        }

        if (gp.teacherScore > 0.55d && gp.chainScore < 0.36d) {
            risk += 0.12d;
        }

        return clamp01(risk);
    }

    private double calcRankConflictRisk(
            int position, int mask, List<Models.RankCounterfactual> list) {
        Models.RankCounterfactual r = findRank(position, mask, list);

        if (r == null) {
            return 0.0d;
        }

        double missed = getDouble(r, "missedStrongRisk", 0.0d);
        double near = getDouble(r, "nearConflictRisk", 0.0d);
        double falseChampion = getDouble(r, "falseChampionRisk", 0.0d);
        double same = getDouble(r, "samePositionReplaceRisk", 0.0d);
        double cross = getDouble(r, "crossPositionReplaceRisk", 0.0d);

        return clamp01(
                missed * 0.26d
                        + near * 0.22d
                        + falseChampion * 0.22d
                        + same * 0.15d
                        + cross * 0.15d);
    }

    private double calcNearCandidateConflictRisk(
            Models.CandidateK3 current, ArrayList<Models.CandidateK3> ranked) {
        if (current == null || ranked == null || ranked.size() <= 1) {
            return 0.0d;
        }

        double s0 = candidateScore(current);
        int position = getInt(current, "position", 0);
        int mask = getInt(current, "k3Mask", getInt(current, "k3_mask", 0));

        int near = 0;
        double risk = 0.0d;

        for (int i = 0; i < ranked.size(); i++) {
            Models.CandidateK3 c = ranked.get(i);

            if (c == null || c == current) {
                continue;
            }

            double s = candidateScore(c);
            double gap = Math.abs(s0 - s);

            if (gap > 0.055d) {
                continue;
            }

            int p = getInt(c, "position", 0);
            int m = getInt(c, "k3Mask", getInt(c, "k3_mask", 0));

            near++;

            if (p != position) {
                risk += 0.09d;
            } else {
                int diff = maskDiffCount(mask, m);

                if (diff <= 4) {
                    risk += 0.07d;
                } else {
                    risk += 0.05d;
                }
            }

            if (s > s0) {
                risk += 0.08d;
            }
        }

        if (near >= 3) {
            risk += 0.14d;
        }

        return clamp01(risk);
    }

    private double calcWeakSampleAmplifyRisk(
            Models.CandidateK3 c,
            Map<String, Models.NumberTeacherCell> teacherMap,
            List<Models.RankCounterfactual> counterfactuals) {
        int position = getInt(c, "position", 0);
        int[] nums = new int[] {getInt(c, "n1", 0), getInt(c, "n2", 0), getInt(c, "n3", 0)};

        double finalP = getDouble(c, "finalP", 0.0d);
        double cause = getDouble(c, "causeScore", 0.0d);
        double teacherConfidenceSum = 0.0d;
        int used = 0;
        int sampleSum = 0;

        for (int i = 0; i < nums.length; i++) {
            Models.NumberTeacherCell cell = getTeacherCell(teacherMap, position, nums[i]);

            if (cell == null) {
                continue;
            }

            teacherConfidenceSum +=
                    getDouble(cell, "confidence", getDouble(cell, "sampleConfidence", 0.0d));
            sampleSum += getInt(cell, "sampleCount", 0);
            used++;
        }

        double teacherConfidence = used <= 0 ? 0.0d : teacherConfidenceSum / used;
        int mask = getInt(c, "k3Mask", getInt(c, "k3_mask", 0));
        Models.RankCounterfactual r = findRank(position, mask, counterfactuals);
        double rankScore =
                r == null
                        ? 0.0d
                        : getDouble(r, "rankScore", getDouble(r, "counterfactualScore", 0.0d));

        double risk = 0.0d;

        if (finalP >= 0.39d && teacherConfidence < 0.22d) {
            risk += 0.20d;
        }

        if (cause >= 0.55d && teacherConfidence < 0.20d) {
            risk += 0.18d;
        }

        if (rankScore >= 0.55d && sampleSum < 30) {
            risk += 0.14d;
        }

        if (used <= 0) {
            risk += 0.10d;
        }

        return clamp01(risk);
    }

    private double calcLoseStreakRisk(List<Models.Trade> trades) {
        if (trades == null || trades.size() == 0) {
            return 0.0d;
        }

        int streak = 0;
        int checked = 0;

        for (int i = trades.size() - 1; i >= 0; i--) {
            Models.Trade t = trades.get(i);

            if (t == null) {
                continue;
            }

            String result = getString(t, "result", "");

            if (result == null) {
                result = "";
            }

            checked++;

            if ("MISS".equals(result) || "LOSE".equals(result) || "未中".equals(result)) {
                streak++;
            } else if ("WIN".equals(result) || "HIT".equals(result) || "命中".equals(result)) {
                break;
            }

            if (checked >= 12) {
                break;
            }
        }

        if (streak <= 0) {
            return 0.0d;
        }

        if (streak >= 5) {
            return 0.72d;
        }

        return clamp01(((double) streak) / 7.0d);
    }

    private boolean isHardBlock(GuardParts gp, double guardRisk) {
        if (guardRisk >= HARD_RISK) {
            return true;
        }

        if (gp == null) {
            return false;
        }

        if (gp.falseStrongRisk >= 0.78d) {
            return true;
        }

        if (gp.teacherConflictRisk >= 0.76d) {
            return true;
        }

        if (gp.rankConflictRisk >= 0.80d) {
            return true;
        }

        if (gp.loseStreakRisk >= 0.72d && gp.weakSampleAmplifyRisk >= 0.50d) {
            return true;
        }

        return false;
    }

    private String buildLevel(int pass, int hardBlock, double score, double risk) {
        if (hardBlock == 1) {
            return "HARD_RISK";
        }

        if (pass == 1 && score >= 0.64d && risk <= 0.46d) {
            return "CLEAN_PASS";
        }

        if (pass == 1) {
            return "PASS";
        }

        if (risk >= 0.70d) {
            return "RISK";
        }

        if (score >= 0.44d) {
            return "WATCH";
        }

        return "WEAK";
    }

    private void applyOneToCandidate(Models.CandidateK3 c, Models.ThirdGuardStat s) {
        if (c == null || s == null) {
            return;
        }

        double guardScore = getDouble(s, "thirdGuardScore", getDouble(s, "guardScore", 0.0d));
        double guardRisk = getDouble(s, "thirdGuardRisk", getDouble(s, "guardRisk", 0.0d));
        int pass = getInt(s, "thirdGuardPass", getInt(s, "pass", 0));

        double oldCause = getDouble(c, "causeScore", 0.0d);
        double oldRisk = getDouble(c, "riskScore", 0.0d);

        double newCause;
        if (pass == 1) {
            newCause = clamp01(oldCause * 0.90d + guardScore * 0.10d);
        } else {
            newCause = clamp01(oldCause * 0.94d - guardRisk * 0.055d);
        }

        double newRisk = clamp01(Math.max(oldRisk, guardRisk));

        putDouble(c, "thirdGuardScore", guardScore);
        putDouble(c, "thirdGuardRisk", guardRisk);
        putInt(c, "thirdGuardPass", pass);
        putInt(c, "thirdGuardHardBlock", getInt(s, "hardBlock", 0));
        putString(c, "thirdGuardLevel", getString(s, "level", getString(s, "status", "")));
        putString(c, "thirdGuardSummary", getString(s, "summaryText", ""));

        putDouble(
                c,
                "falseStrongRisk",
                Math.max(
                        getDouble(c, "falseStrongRisk", 0.0d),
                        getDouble(s, "falseStrongRisk", 0.0d)));
        putDouble(c, "hotExhaustionRisk", getDouble(s, "hotExhaustionRisk", 0.0d));
        putDouble(c, "coldFakeRecoveryRisk", getDouble(s, "coldFakeRecoveryRisk", 0.0d));
        putDouble(c, "teacherConflictRisk", getDouble(s, "teacherConflictRisk", 0.0d));
        putDouble(c, "rankConflictRisk", getDouble(s, "rankConflictRisk", 0.0d));
        putDouble(c, "weakSampleAmplifyRisk", getDouble(s, "weakSampleAmplifyRisk", 0.0d));
        putDouble(c, "loseStreakRisk", getDouble(s, "loseStreakRisk", 0.0d));
        putDouble(c, "thirdQuality", getDouble(s, "thirdQuality", 0.0d));
        putDouble(c, "thirdNumberQuality", getDouble(s, "thirdNumberQuality", 0.0d));
        putDouble(c, "thirdNumberQualityScore", getDouble(s, "thirdNumberQualityScore", 0.0d));
        putDouble(c, "thirdWeakness", getDouble(s, "thirdWeakness", 0.0d));
        putDouble(c, "thirdDragRisk", getDouble(s, "thirdDragRisk", 0.0d));
        putDouble(c, "thirdDragScore", getDouble(s, "thirdDragRisk", 0.0d));
        putDouble(c, "thirdSupplyScore", getDouble(s, "thirdSupplyScore", 0.0d));
        putDouble(c, "thirdFalseStrongRisk", getDouble(s, "thirdFalseStrongRisk", 0.0d));
        putString(c, "thirdRejectCode", getString(s, "thirdRejectCode", ""));
        putString(c, "thirdActionAdvice", getString(s, "thirdActionAdvice", ""));
        putString(c, "thirdGuardActionAdvice", getString(s, "thirdActionAdvice", ""));
        putString(c, "thirdGuardVersion", THIRD_GUARD_MAIN_CHAIN_VERSION);
        putString(c, "fieldChainVersion", THIRD_GUARD_CLOSED_LOOP_VERSION);
        if (getString(s, "thirdRejectCode", "").length() > 0) {
            putString(c, "rejectCode", getString(s, "thirdRejectCode", ""));
            putString(c, "finalDecisionRejectCode", getString(s, "thirdRejectCode", ""));
        }
        putDouble(
                c,
                "antiEvidenceScore",
                Math.max(getDouble(c, "antiEvidenceScore", 0.0d), guardRisk));
        putDouble(
                c,
                "counterEvidenceScore",
                Math.max(
                        getDouble(c, "counterEvidenceScore", 0.0d),
                        getDouble(s, "thirdDragRisk", 0.0d)));
        putDouble(
                c,
                "netEvidenceScore",
                clampSigned(
                        getDouble(c, "evidenceScore", 0.0d)
                                - Math.max(getDouble(c, "antiEvidenceScore", 0.0d), guardRisk)));

        putDouble(c, "causeScore", newCause);
        putDouble(c, "riskScore", newRisk);

        appendReason(c, getString(s, "summaryText", ""));
    }

    private void rerankByThirdGuard(List<Models.CandidateK3> candidates) {
        if (candidates == null || candidates.size() <= 1) {
            return;
        }

        Collections.sort(
                candidates,
                new Comparator<Models.CandidateK3>() {
                    public int compare(Models.CandidateK3 a, Models.CandidateK3 b) {
                        double sa = guardedCandidateScore(a);
                        double sb = guardedCandidateScore(b);

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

                        return getInt(a, "k3Mask", getInt(a, "k3_mask", 0))
                                - getInt(b, "k3Mask", getInt(b, "k3_mask", 0));
                    }
                });

        for (int i = 0; i < candidates.size(); i++) {
            Models.CandidateK3 c = candidates.get(i);

            if (c != null) {
                putInt(c, "rankNo", i + 1);
                putInt(c, "rank", i + 1);
            }
        }
    }

    private static double guardedCandidateScore(Models.CandidateK3 c) {
        double base = candidateScore(c);
        double guard = getDouble(c, "thirdGuardScore", 0.0d);
        double risk = getDouble(c, "thirdGuardRisk", 0.0d);
        int pass = getInt(c, "thirdGuardPass", 0);
        int hard = getInt(c, "thirdGuardHardBlock", 0);

        double thirdQuality =
                getDouble(c, "thirdQuality", getDouble(c, "thirdNumberQualityScore", 0.0d));
        double thirdDrag = getDouble(c, "thirdDragRisk", getDouble(c, "thirdDragScore", 0.0d));
        double score =
                base * 0.78d
                        + guard * 0.16d
                        + thirdQuality * 0.08d
                        - risk * 0.10d
                        - thirdDrag * 0.08d;

        if (pass == 1) {
            score += 0.018d;
        }

        if (hard == 1) {
            score -= 0.080d;
        }

        return clamp01(score);
    }

    private Models.ThirdGuardStat findBestStatForNumber(
            ArrayList<Models.ThirdGuardStat> stats, int position, int number) {
        Models.ThirdGuardStat best = null;
        double bestScore = -1.0d;

        for (int i = 0; i < stats.size(); i++) {
            Models.ThirdGuardStat s = stats.get(i);

            if (s == null) {
                continue;
            }

            int p = getInt(s, "position", 0);

            if (p != position) {
                continue;
            }

            int n1 = getInt(s, "n1", 0);
            int n2 = getInt(s, "n2", 0);
            int n3 = getInt(s, "n3", 0);

            if (number != n1 && number != n2 && number != n3) {
                continue;
            }

            double sc = getDouble(s, "thirdGuardScore", getDouble(s, "guardScore", 0.0d));

            if (best == null || sc > bestScore) {
                best = s;
                bestScore = sc;
            }
        }

        return best;
    }

    private double calcThirdQuality(
            Models.CandidateK3 c,
            Map<String, Models.NumberTeacherCell> teacherMap,
            List<Models.RankCounterfactual> counterfactuals) {
        if (c == null) return 0.0d;
        double base = calcThirdQualityFromObject(c);
        int position = getInt(c, "position", 0);
        int third = getInt(c, "n3", 0);
        Models.NumberTeacherCell cell = getTeacherCell(teacherMap, position, third);
        double teacherScore =
                cell == null
                        ? getDouble(c, "teacherNumberScore", 0.0d)
                        : getDouble(
                                cell, "teacherScore", getDouble(cell, "numberTeacherScore", 0.0d));
        double teacherRisk =
                cell == null
                        ? getDouble(c, "teacherNumberRisk", 0.0d)
                        : getDouble(
                                cell, "teacherRisk", getDouble(cell, "numberTeacherRisk", 0.0d));
        double recent =
                cell == null
                        ? getDouble(c, "teacherRecentValidity", 0.0d)
                        : getDouble(cell, "recentValidity", getDouble(cell, "recentHitRate", 0.0d));
        double sample =
                cell == null
                        ? getDouble(c, "teacherSampleReliability", 0.0d)
                        : getDouble(
                                cell, "sampleReliability", getDouble(cell, "sampleTrust", 0.0d));
        int mask = getInt(c, "k3Mask", getInt(c, "k3_mask", 0));
        Models.RankCounterfactual r = findRank(position, mask, counterfactuals);
        double replace =
                r == null
                        ? getDouble(c, "replaceGain", 0.0d)
                        : getDouble(
                                r,
                                "oneNumberReplaceRisk",
                                getDouble(r, "samePositionReplaceRisk", 0.0d));
        return clamp01(
                base * 0.36d
                        + teacherScore * 0.20d
                        + recent * 0.14d
                        + sample * 0.12d
                        + (1.0d - teacherRisk) * 0.10d
                        + (1.0d - replace) * 0.08d);
    }

    private double calcThirdQualityFromObject(Object c) {
        if (c == null) return 0.0d;
        double v = getDouble(c, "thirdQuality", -1.0d);
        if (v < 0.0d) v = getDouble(c, "thirdNumberQuality", -1.0d);
        if (v < 0.0d) v = getDouble(c, "thirdNumberQualityScore", -1.0d);
        if (v < 0.0d) v = getDouble(c, "needSupplyThirdQuality", -1.0d);
        if (v < 0.0d)
            v =
                    getDouble(c, "coverageQuality", getDouble(c, "coverageQualityScore", 0.0d))
                                    * 0.60d
                            + getDouble(c, "numberSupplyScore", 0.0d) * 0.40d;
        return clamp01(v);
    }

    private double calcThirdSupplyScore(Models.CandidateK3 c, GuardParts gp) {
        if (c == null || gp == null) return 0.0d;
        double supply =
                getDouble(c, "numberSupplyScore", getDouble(c, "numberSupplyStrength", 0.0d));
        double need = getDouble(c, "positionNeedScore", getDouble(c, "positionNeedStrength", 0.0d));
        double coverage =
                getDouble(c, "coverageQuality", getDouble(c, "coverageQualityScore", 0.0d));
        return clamp01(gp.thirdQuality * 0.42d + supply * 0.28d + need * 0.14d + coverage * 0.16d);
    }

    private double calcThirdDragRisk(
            Models.CandidateK3 c, GuardParts gp, ArrayList<Models.CandidateK3> ranked) {
        if (c == null || gp == null) return 0.0d;
        double risk = 0.0d;
        risk += gp.thirdWeakness * 0.36d;
        risk += Math.max(0.0d, THIRD_QUALITY_READY - gp.thirdQuality) * 0.42d;
        risk += getDouble(c, "counterfactualRisk", 0.0d) * 0.16d;
        risk += getDouble(c, "replaceGain", 0.0d) * 0.18d;
        risk += getDouble(c, "rankConflictRisk", 0.0d) * 0.12d;
        int position = getInt(c, "position", 0);
        int mask = getInt(c, "k3Mask", getInt(c, "k3_mask", 0));
        double current = candidateScore(c);
        double bestOneReplace = 0.0d;
        if (ranked != null) {
            for (int i = 0; i < ranked.size(); i++) {
                Models.CandidateK3 alt = ranked.get(i);
                if (alt == null || alt == c) continue;
                int p = getInt(alt, "position", 0);
                int m = getInt(alt, "k3Mask", getInt(alt, "k3_mask", 0));
                if (p != position || m == mask) continue;
                if (maskDiffCount(mask, m) == 2) {
                    double gain = candidateScore(alt) - current;
                    if (gain > bestOneReplace) bestOneReplace = gain;
                }
            }
        }
        if (bestOneReplace > 0.0d) {
            risk += clamp01(bestOneReplace / 0.15d) * 0.26d;
        }
        return clamp01(risk);
    }

    private double calcThirdDragRiskFromObject(
            Object c, double thirdQuality, ArrayList<Object> ranked) {
        if (c == null) return 0.0d;
        double risk = 0.0d;
        risk += clamp01(1.0d - thirdQuality) * 0.36d;
        risk += Math.max(0.0d, THIRD_QUALITY_READY - thirdQuality) * 0.42d;
        risk += getDouble(c, "counterfactualRisk", 0.0d) * 0.18d;
        risk += getDouble(c, "replaceGain", 0.0d) * 0.18d;
        int position = getInt(c, "position", 0);
        int mask = getInt(c, "k3Mask", getInt(c, "k3_mask", 0));
        double current = rawCandidateScore(c);
        if (ranked != null) {
            for (int i = 0; i < ranked.size(); i++) {
                Object alt = ranked.get(i);
                if (alt == null || alt == c) continue;
                int p = getInt(alt, "position", 0);
                int m = getInt(alt, "k3Mask", getInt(alt, "k3_mask", 0));
                if (p == position && m != mask && maskDiffCount(mask, m) == 2) {
                    double gain = rawCandidateScore(alt) - current;
                    if (gain > 0.0d) risk += clamp01(gain / 0.15d) * 0.20d;
                }
            }
        }
        return clamp01(risk);
    }

    private String buildThirdRejectCode(GuardParts gp) {
        if (gp == null) return "THIRD_GUARD_UNKNOWN";
        if (gp.thirdDragRisk >= THIRD_HARD_DRAG_LINE) return "THIRD_GUARD_HARD_DRAG";
        if (gp.thirdQuality < THIRD_QUALITY_WEAK) return "THIRD_GUARD_THIRD_WEAK";
        if (gp.thirdFalseStrongRisk >= 0.680d) return "THIRD_GUARD_FALSE_STRONG";
        if (gp.thirdSupplyScore < 0.320d) return "THIRD_GUARD_LOW_SUPPLY";
        if (gp.rankConflictRisk >= 0.700d) return "THIRD_GUARD_RANK_CONFLICT";
        return "";
    }

    private String buildThirdRejectCode(ThirdGuardObjectResult r) {
        if (r == null) return "THIRD_GUARD_UNKNOWN";
        if (r.thirdDragRisk >= THIRD_HARD_DRAG_LINE) return "THIRD_GUARD_HARD_DRAG";
        if (r.thirdQuality < THIRD_QUALITY_WEAK) return "THIRD_GUARD_THIRD_WEAK";
        if (r.thirdFalseStrongRisk >= 0.680d) return "THIRD_GUARD_FALSE_STRONG";
        if (r.thirdSupplyScore < 0.320d) return "THIRD_GUARD_LOW_SUPPLY";
        return "";
    }

    private String buildThirdActionAdvice(GuardParts gp) {
        if (gp == null) return "THIRD_WATCH_UNKNOWN";
        if (gp.thirdRejectCode != null && gp.thirdRejectCode.length() > 0)
            return "THIRD_FREEZE_OR_SHADOW：第三号拖累或虚强，禁止本层救援BET";
        if (gp.thirdQuality >= 0.620d && gp.thirdDragRisk <= 0.420d)
            return "THIRD_PASS：第三号供给合格，可送DecisionGate复核";
        if (gp.thirdQuality >= THIRD_QUALITY_READY) return "THIRD_WATCH：第三号基本可用，仍需最终门确认";
        return "THIRD_WATCH_WEAK：第三号偏弱，只建议SHADOW观察";
    }

    private String buildThirdActionAdvice(ThirdGuardObjectResult r) {
        if (r == null) return "THIRD_WATCH_UNKNOWN";
        if (r.rejectCode != null && r.rejectCode.length() > 0)
            return "THIRD_FREEZE_OR_SHADOW：第三号拖累或虚强，禁止本层救援BET";
        if (r.thirdQuality >= 0.620d && r.thirdDragRisk <= 0.420d)
            return "THIRD_PASS：第三号供给合格，可送DecisionGate复核";
        if (r.thirdQuality >= THIRD_QUALITY_READY) return "THIRD_WATCH：第三号基本可用，仍需最终门确认";
        return "THIRD_WATCH_WEAK：第三号偏弱，只建议SHADOW观察";
    }

    private String buildThirdObjectSummary(ThirdGuardObjectResult r, String source) {
        if (r == null) return "第三号守门：无结果";
        return "V20第三号守门矩阵｜入口="
                + safe(source)
                + "｜rank="
                + r.rankNo
                + "｜位置="
                + r.position
                + "｜K3="
                + r.n1
                + ","
                + r.n2
                + ","
                + r.n3
                + "｜thirdQuality="
                + percent(r.thirdQuality)
                + "｜thirdDragRisk="
                + percent(r.thirdDragRisk)
                + "｜thirdWeakness="
                + percent(r.thirdWeakness)
                + "｜guardScore="
                + percent(r.guardScore)
                + "｜guardRisk="
                + percent(r.guardRisk)
                + "｜pass="
                + r.pass
                + "｜reject="
                + safe(r.rejectCode)
                + "｜建议="
                + safe(r.actionAdvice)
                + "｜说明=只做第三号守门，不创建BET，最终仍由DecisionGateEngine裁决";
    }

    private static double rawCandidateScore(Object c) {
        if (c == null) return 0.0d;
        double finalP = getDouble(c, "finalP", 0.0d);
        double edge = getDouble(c, "edge", 0.0d);
        double cause = getDouble(c, "causeScore", 0.0d);
        double teacher =
                getDouble(
                        c,
                        "teacherNumberScore",
                        getDouble(
                                c, "teacherComboScore", getDouble(c, "teacherMatrixScore", 0.0d)));
        double rank =
                getDouble(c, "counterfactualScore", getDouble(c, "counterfactualRankScore", 0.0d));
        double position =
                getDouble(c, "positionCauseScore", getDouble(c, "positionHealthScore", 0.0d));
        double coverage =
                getDouble(c, "coverageQuality", getDouble(c, "coverageQualityScore", 0.0d));
        double third =
                getDouble(
                        c,
                        "thirdQuality",
                        getDouble(
                                c,
                                "thirdNumberQualityScore",
                                getDouble(c, "needSupplyThirdQuality", 0.0d)));
        double risk = getDouble(c, "riskScore", 0.0d);
        return clamp01(
                finalP * 0.20d
                        + posPart(edge * 4.0d) * 0.08d
                        + cause * 0.18d
                        + teacher * 0.10d
                        + rank * 0.10d
                        + position * 0.08d
                        + coverage * 0.12d
                        + third * 0.14d
                        - risk * 0.12d);
    }

    private String buildSummary(
            int rankNo,
            int position,
            int n1,
            int n2,
            int n3,
            double baseScore,
            double guardScore,
            double guardRisk,
            int pass,
            int hardBlock,
            String level,
            GuardParts gp) {
        StringBuilder sb = new StringBuilder();

        sb.append("V20第三防线矩阵｜rank=");
        sb.append(rankNo);
        sb.append("｜位置=");
        sb.append(position);
        sb.append("｜K3=");
        sb.append(n1);
        sb.append(",");
        sb.append(n2);
        sb.append(",");
        sb.append(n3);
        sb.append("｜基础分=");
        sb.append(percent(baseScore));
        sb.append("｜防线分=");
        sb.append(percent(guardScore));
        sb.append("｜防线风险=");
        sb.append(percent(guardRisk));
        sb.append("｜pass=");
        sb.append(pass);
        sb.append("｜hard=");
        sb.append(hardBlock);
        sb.append("｜等级=");
        sb.append(level);
        sb.append("｜位置风险=");
        sb.append(percent(gp.positionRisk));
        sb.append("｜教师风险=");
        sb.append(percent(gp.teacherRisk));
        sb.append("｜排名风险=");
        sb.append(percent(gp.rankRisk));
        sb.append("｜因链风险=");
        sb.append(percent(gp.chainRisk));
        sb.append("｜伪强=");
        sb.append(percent(gp.falseStrongRisk));
        sb.append("｜热尽=");
        sb.append(percent(gp.hotExhaustionRisk));
        sb.append("｜假冷补=");
        sb.append(percent(gp.coldFakeRecoveryRisk));
        sb.append("｜教师冲突=");
        sb.append(percent(gp.teacherConflictRisk));
        sb.append("｜排名冲突=");
        sb.append(percent(gp.rankConflictRisk));
        sb.append("｜近邻冲突=");
        sb.append(percent(gp.nearCandidateConflictRisk));
        sb.append("｜弱样本放大=");
        sb.append(percent(gp.weakSampleAmplifyRisk));
        sb.append("｜连挂风险=");
        sb.append(percent(gp.loseStreakRisk));
        sb.append("｜第三号质量=");
        sb.append(percent(gp.thirdQuality));
        sb.append("｜第三号拖拽=");
        sb.append(percent(gp.thirdDragRisk));
        sb.append("｜第三号建议=");
        sb.append(gp.thirdActionAdvice);
        if (gp.thirdRejectCode != null && gp.thirdRejectCode.length() > 0) {
            sb.append("｜第三号拒绝=");
            sb.append(gp.thirdRejectCode);
        }
        sb.append("｜说明=第三防线只审计风险，不直接创建BET，最终仍由DecisionGateEngine裁决");

        return sb.toString();
    }

    private String buildGuardKey(String targetIssue, int position, int mask) {
        return safe(targetIssue) + "|P" + position + "|K3_" + mask;
    }

    private String candidateKey(int position, int mask) {
        return "P" + position + "|K3_" + mask;
    }

    public static String keyOf(int position, int number) {
        return String.valueOf(position) + "_" + String.valueOf(number);
    }

    private int maskDiffCount(int a, int b) {
        int diff = 0;

        for (int n = NUMBER_MIN; n <= NUMBER_MAX; n++) {
            boolean aa = maskContains(a, n);
            boolean bb = maskContains(b, n);

            if (aa != bb) {
                diff++;
            }
        }

        return diff;
    }

    private boolean maskContains(int mask, int number) {
        if (!isValidNumber(number) || mask <= 0) {
            return false;
        }

        try {
            return (mask & (1 << number)) != 0;
        } catch (Throwable e) {
            return false;
        }
    }

    private boolean isValidPosition(int p) {
        return p >= POSITION_MIN && p <= POSITION_MAX;
    }

    private boolean isValidNumber(int n) {
        return n >= NUMBER_MIN && n <= NUMBER_MAX;
    }

    private static double posPart(double v) {
        if (v <= 0.0d) {
            return 0.0d;
        }

        if (v > 1.0d) {
            return 1.0d;
        }

        return v;
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

    private String safe(String s) {
        return s == null ? "" : s;
    }

    private void appendReason(Models.CandidateK3 c, String add) {
        if (c == null || add == null || add.length() == 0) {
            return;
        }

        String old = getString(c, "reasonSummary", "");

        if (old == null || old.length() == 0) {
            putString(c, "reasonSummary", add);
        } else {
            putString(c, "reasonSummary", old + "｜" + add);
        }
    }

    private static int getInt(Object obj, String field, int fallback) {
        if (obj == null || field == null || field.length() == 0) {
            return fallback;
        }

        try {
            Field f = obj.getClass().getField(field);
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

    private static double getDouble(Object obj, String field, double fallback) {
        if (obj == null || field == null || field.length() == 0) {
            return fallback;
        }

        try {
            Field f = obj.getClass().getField(field);
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

    private static String getString(Object obj, String field, String fallback) {
        if (obj == null || field == null || field.length() == 0) {
            return fallback;
        }

        try {
            Field f = obj.getClass().getField(field);
            Object v = f.get(obj);

            if (v != null) {
                return String.valueOf(v);
            }
        } catch (Throwable e) {
        }

        return fallback;
    }

    private static void putInt(Object obj, String field, int value) {
        if (obj == null || field == null || field.length() == 0) {
            return;
        }

        try {
            Field f = obj.getClass().getField(field);
            f.setInt(obj, value);
        } catch (Throwable e) {
            try {
                Field f = obj.getClass().getField(field);
                f.set(obj, Integer.valueOf(value));
            } catch (Throwable ignored) {
            }
        }
    }

    private static void putLong(Object obj, String field, long value) {
        if (obj == null || field == null || field.length() == 0) {
            return;
        }

        try {
            Field f = obj.getClass().getField(field);
            f.setLong(obj, value);
        } catch (Throwable e) {
            try {
                Field f = obj.getClass().getField(field);
                f.set(obj, Long.valueOf(value));
            } catch (Throwable ignored) {
            }
        }
    }

    private static void putDouble(Object obj, String field, double value) {
        if (obj == null || field == null || field.length() == 0) {
            return;
        }

        try {
            Field f = obj.getClass().getField(field);
            f.setDouble(obj, value);
        } catch (Throwable e) {
            try {
                Field f = obj.getClass().getField(field);
                f.set(obj, Double.valueOf(value));
            } catch (Throwable ignored) {
            }
        }
    }

    private static void putString(Object obj, String field, String value) {
        if (obj == null || field == null || field.length() == 0) {
            return;
        }

        try {
            Field f = obj.getClass().getField(field);
            f.set(obj, value == null ? "" : value);
        } catch (Throwable e) {
        }
    }

    private static final class GuardParts {
        double positionHealth;
        double positionRisk;
        double teacherScore;
        double teacherRisk;
        double rankScore;
        double rankRisk;
        double chainScore;
        double chainRisk;

        double falseStrongRisk;
        double hotExhaustionRisk;
        double coldFakeRecoveryRisk;
        double positionDriftRisk;
        double teacherConflictRisk;
        double rankConflictRisk;
        double nearCandidateConflictRisk;
        double weakSampleAmplifyRisk;
        double loseStreakRisk;
        double thirdQuality;
        double thirdWeakness;
        double thirdSupplyScore;
        double thirdDragRisk;
        double thirdFalseStrongRisk;
        String thirdRejectCode;
        String thirdActionAdvice;
    }

    private static final class ThirdGuardObjectResult {
        String baseIssue;
        String targetIssue;
        int rankNo;
        int position;
        int n1;
        int n2;
        int n3;
        int k3Mask;
        double thirdQuality;
        double thirdWeakness;
        double thirdDragRisk;
        double thirdSupplyScore;
        double thirdFalseStrongRisk;
        double guardScore;
        double guardRisk;
        int pass;
        int hardBlock;
        String rejectCode;
        String actionAdvice;
        String summary;
    }
}
