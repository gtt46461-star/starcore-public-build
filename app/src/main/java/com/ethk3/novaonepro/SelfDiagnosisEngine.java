package com.ethk3.novaonepro;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public final class SelfDiagnosisEngine {

    public static final String LEVEL_OK = "OK";
    public static final String LEVEL_WARN = "WARN";
    public static final String LEVEL_BAD = "BAD";
    public static final String LEVEL_FATAL = "FATAL";

    public static final String CODE_OK = "OK";
    public static final String CODE_DATA_EMPTY = "DATA_EMPTY";
    public static final String CODE_DATA_WEAK = "DATA_WEAK";
    public static final String CODE_DATA_INVALID = "DATA_INVALID";
    public static final String CODE_DUPLICATE_ISSUE = "DUPLICATE_ISSUE";
    public static final String CODE_CANDIDATE_EMPTY = "CANDIDATE_EMPTY";
    public static final String CODE_CANDIDATE_INVALID = "CANDIDATE_INVALID";
    public static final String CODE_PROBABILITY_LOW = "PROBABILITY_LOW";
    public static final String CODE_EVIDENCE_WEAK = "EVIDENCE_WEAK";
    public static final String CODE_RISK_HIGH = "RISK_HIGH";
    public static final String CODE_DRIFT_HIGH = "DRIFT_HIGH";
    public static final String CODE_BREAK_HIGH = "BREAK_HIGH";
    public static final String CODE_AUDIT_HIGH = "AUDIT_HIGH";
    public static final String CODE_AI_CONFLICT = "AI_CONFLICT";
    public static final String CODE_PROFILE_WEAK = "PROFILE_WEAK";
    public static final String CODE_STATS_MIXED = "STATS_MIXED";
    public static final String CODE_LONG_HOLD = "LONG_HOLD";
    public static final String CODE_RUNTIME_WEAK = "RUNTIME_WEAK";
    public static final String CODE_REAL_TRADE_ERROR = "REAL_TRADE_ERROR";
    public static final String CODE_SETTLEMENT_ERROR = "SETTLEMENT_ERROR";

    private static final int MIN_HISTORY_FATAL = 30;
    private static final int MIN_HISTORY_USABLE = 240;
    private static final int MIN_HISTORY_STRONG = 500;

    private static final double MIN_REAL_BET_RATE = 0.030d;
    private static final double MAX_RISK_WARN = 0.680d;
    private static final double MAX_RISK_BAD = 0.780d;

    public SelfDiagnosisEngine() {}

    public Models.SelfDiagnosisResult diagnoseToModel(
            List<Models.Draw> history,
            List<Models.ComboCandidate> candidates,
            List<Models.Trade> trades,
            List<Models.DeepProfile> profiles,
            Models.Decision lastDecision,
            Object runtimeState) {
        DiagnosisReport report =
                diagnoseAll(history, candidates, trades, profiles, lastDecision, runtimeState);
        return toModel(report);
    }

    public DiagnosisReport diagnoseAll(
            List<Models.Draw> history,
            List<Models.ComboCandidate> candidates,
            List<Models.Trade> trades,
            List<Models.DeepProfile> profiles,
            Models.Decision lastDecision,
            Object runtimeState) {
        DiagnosisReport report = new DiagnosisReport();
        report.createdAt = System.currentTimeMillis();

        diagnoseData(report, history);
        diagnoseCandidates(report, candidates);
        diagnoseTrades(report, trades);
        diagnoseProfiles(report, profiles);
        diagnoseDecision(report, lastDecision);
        diagnoseRuntime(report, runtimeState);

        finalizeReport(report);
        return report;
    }

    public DiagnosisReport diagnoseDataOnly(List<Models.Draw> history) {
        DiagnosisReport r = new DiagnosisReport();
        r.createdAt = System.currentTimeMillis();
        diagnoseData(r, history);
        finalizeReport(r);
        return r;
    }

    public DiagnosisReport diagnoseCandidatesOnly(List<Models.ComboCandidate> candidates) {
        DiagnosisReport r = new DiagnosisReport();
        r.createdAt = System.currentTimeMillis();
        diagnoseCandidates(r, candidates);
        finalizeReport(r);
        return r;
    }

    public DiagnosisReport diagnoseTradesOnly(List<Models.Trade> trades) {
        DiagnosisReport r = new DiagnosisReport();
        r.createdAt = System.currentTimeMillis();
        diagnoseTrades(r, trades);
        finalizeReport(r);
        return r;
    }

    public DiagnosisReport diagnoseRuntimeOnly(Object runtimeState) {
        DiagnosisReport r = new DiagnosisReport();
        r.createdAt = System.currentTimeMillis();
        diagnoseRuntime(r, runtimeState);
        finalizeReport(r);
        return r;
    }

    public void applyToDecision(Models.Decision decision, DiagnosisReport report) {
        if (decision == null || report == null) {
            return;
        }

        decision.selfDiagnosisText = report.summaryText;

        if (LEVEL_BAD.equals(report.level) || LEVEL_FATAL.equals(report.level)) {
            if (decision.blockCode == null || decision.blockCode.length() == 0) {
                decision.blockCode = inferBlockCode(report.mainCode);
            }

            if (decision.reasonText == null || decision.reasonText.length() == 0) {
                decision.reasonText = report.summaryText;
            } else {
                decision.reasonText = append(decision.reasonText, report.summaryText);
            }

            if (decision.reasonSummary == null || decision.reasonSummary.length() == 0) {
                decision.reasonSummary = report.summaryText;
            } else {
                decision.reasonSummary = append(decision.reasonSummary, report.summaryText);
            }
        }
    }

    public void applyToCandidate(Models.ComboCandidate candidate, DiagnosisReport report) {
        if (candidate == null || report == null) {
            return;
        }

        candidate.reasonSummary =
                append(
                        candidate.reasonSummary,
                        "自诊断："
                                + safe(report.level)
                                + "，主因="
                                + safe(report.mainCode)
                                + "，说明="
                                + safe(report.shortText));

        if (LEVEL_FATAL.equals(report.level) && candidate.finalP < 0.430d) {
            candidate.blockCode =
                    pickBlockCode(candidate.blockCode, inferBlockCode(report.mainCode));
        }

        if (LEVEL_BAD.equals(report.level)
                && (CODE_STATS_MIXED.equals(report.mainCode)
                        || CODE_REAL_TRADE_ERROR.equals(report.mainCode)
                        || CODE_SETTLEMENT_ERROR.equals(report.mainCode))) {
            candidate.blockCode =
                    pickBlockCode(candidate.blockCode, inferBlockCode(report.mainCode));
        }
    }

    public ArrayList<Models.ComboCandidate> applyToCandidates(
            List<Models.ComboCandidate> candidates, DiagnosisReport report) {
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
            applyToCandidate(copy, report);
            out.add(copy);
        }

        return out;
    }

    public String buildSummary(DiagnosisReport report) {
        if (report == null) {
            return "自诊断：暂无";
        }

        if (report.summaryText == null || report.summaryText.length() == 0) {
            return "自诊断：" + safe(report.level) + " " + safe(report.mainCode);
        }

        return report.summaryText;
    }

    /** R811 read-only infrastructure diagnosis; READ_ERROR always wins over numeric findings. */
    public String diagnoseR811Infrastructure(DbStore store) {
        if (store == null) return "READ_ERROR|STORE_NULL";
        android.database.Cursor cursor = null;
        try {
            R811SchemaIntegrityVerifier.Report schema =
                    R811SchemaIntegrityVerifier.verifyCritical(store.readable());
            if (!schema.passed) return "READ_ERROR|HARD_FAIL_R811_SCHEMA|" + schema.detail;
            cursor =
                    store.readable()
                            .rawQuery(
                                    "SELECT task_class,COUNT(1) FROM "
                                            + DbHelper.TABLE_R36_SETTLEMENT_TASK
                                            + " GROUP BY task_class ORDER BY task_class",
                                    null);
            StringBuilder classes = new StringBuilder();
            while (cursor.moveToNext()) {
                if (classes.length() > 0) classes.append(',');
                classes.append(cursor.getString(0)).append('=').append(cursor.getInt(1));
            }
            cursor.close();
            cursor = null;
            cursor =
                    store.readable()
                            .rawQuery(
                                    "SELECT MIN(cursor_version),MIN(next_action_ordinal),"
                                            + "MAX(next_action_ordinal) FROM "
                                            + DbHelper.TABLE_R35_DEFERRED_EVIDENCE_JOB,
                                    null);
            String cursorState = "EMPTY";
            if (cursor.moveToFirst()) {
                cursorState =
                        "version="
                                + cursor.getInt(0)
                                + "|min="
                                + cursor.getInt(1)
                                + "|max="
                                + cursor.getInt(2);
            }
            RuntimeHealthLedgerR36.BusinessTruth truth =
                    RuntimeHealthLedgerR36.businessTruth(store);
            return "status="
                    + truth.status
                    + "|rootCause="
                    + truth.rootCause
                    + "|schemaRevision="
                    + store.getState("r811_schema_revision", "MISSING")
                    + "|settlementClasses="
                    + classes
                    + "|deferredCursor="
                    + cursorState
                    + "|hotPathDdl=FORBIDDEN_AND_STATIC_GATE_REQUIRED";
        } catch (Throwable e) {
            return "READ_ERROR|" + e.getClass().getSimpleName();
        } finally {
            if (cursor != null) cursor.close();
        }
    }

    private void diagnoseData(DiagnosisReport r, List<Models.Draw> history) {
        if (r == null) {
            return;
        }

        if (history == null || history.size() == 0) {
            addIssue(r, LEVEL_FATAL, CODE_DATA_EMPTY, "历史数据为空，无法生成有效候选");
            r.dataText = append(r.dataText, "历史数据为空");
            return;
        }

        int valid = 0;
        int invalid = 0;
        int duplicate = 0;
        String latestIssue = "";
        HashSet<String> issueSet = new HashSet<String>();

        for (int i = 0; i < history.size(); i++) {
            Models.Draw d = history.get(i);

            if (d == null || !d.isValid()) {
                invalid++;
                continue;
            }

            valid++;

            String issue = safe(d.issue).trim();

            if (issue.length() > 0) {
                latestIssue = issue;

                if (issueSet.contains(issue)) {
                    duplicate++;
                } else {
                    issueSet.add(issue);
                }
            } else {
                invalid++;
            }
        }

        r.historyCount = history.size();
        r.validDrawCount = valid;
        r.invalidDrawCount = invalid;
        r.duplicateIssueCount = duplicate;
        r.latestIssue = latestIssue;

        if (valid < MIN_HISTORY_FATAL) {
            addIssue(r, LEVEL_FATAL, CODE_DATA_WEAK, "有效历史少于30期，所有深度模块都不可靠");
        } else if (valid < MIN_HISTORY_USABLE) {
            addIssue(r, LEVEL_BAD, CODE_DATA_WEAK, "有效历史少于30期，只能观察；30期后仅进入受控PROBE，不代表模型已稳定");
        } else if (valid < MIN_HISTORY_STRONG) {
            addIssue(r, LEVEL_WARN, CODE_DATA_WEAK, "有效历史少于500期，画像和长期学习仍偏弱");
        }

        if (invalid > 0) {
            addIssue(r, LEVEL_WARN, CODE_DATA_INVALID, "存在无效开奖数据：" + invalid + "条");
        }

        if (duplicate > 0) {
            addIssue(r, LEVEL_WARN, CODE_DUPLICATE_ISSUE, "存在重复期号：" + duplicate + "条");
        }

        r.dataText =
                "数据诊断：总历史="
                        + history.size()
                        + "，有效="
                        + valid
                        + "，无效="
                        + invalid
                        + "，重复期号="
                        + duplicate
                        + "，最新期="
                        + latestIssue;
    }

    private void diagnoseCandidates(DiagnosisReport r, List<Models.ComboCandidate> candidates) {
        if (r == null) {
            return;
        }

        if (candidates == null || candidates.size() == 0) {
            addIssue(r, LEVEL_BAD, CODE_CANDIDATE_EMPTY, "候选池为空，当前无法进入BET判断");
            r.candidateText = "候选诊断：候选池为空";
            return;
        }

        int valid = 0;
        int invalid = 0;
        int overGate = 0;
        int lowProbability = 0;
        int weakEvidence = 0;
        int highRisk = 0;
        int driftHigh = 0;
        int breakHigh = 0;
        int auditHigh = 0;
        int aiConflict = 0;
        int blocked = 0;
        int illegalK3 = 0;

        Models.ComboCandidate best = null;

        for (int i = 0; i < candidates.size(); i++) {
            Models.ComboCandidate c = candidates.get(i);

            if (c == null) {
                invalid++;
                continue;
            }

            normalizeCandidateK3(c);

            if (!c.isValidK3()) {
                invalid++;
                illegalK3++;
                continue;
            }

            valid++;

            if (best == null || c.sortScore() > best.sortScore()) {
                best = c;
            }

            double gate = c.dynamicGate > 0.0d ? c.dynamicGate : AppConfig.DEFAULT_DYNAMIC_GATE;

            if (c.finalP >= gate && c.finalP >= AppConfig.MIN_REAL_BET_PROBABILITY) {
                overGate++;
            }

            if (c.finalP < AppConfig.MIN_REAL_BET_PROBABILITY) {
                lowProbability++;
            }

            if (c.evidenceScore < 0.420d) {
                weakEvidence++;
            }

            if (c.evidenceRisk >= MAX_RISK_WARN) {
                highRisk++;
            }

            if (c.driftRisk >= AppConfig.DRIFT_RISK_MAX) {
                driftHigh++;
            }

            if (c.breakDepth >= AppConfig.BREAK_DEPTH_MAX) {
                breakHigh++;
            }

            if (c.auditPenalty >= 0.300d) {
                auditHigh++;
            }

            if (c.conflictLevel >= AppConfig.AI_JUDGE_CONFLICT_MAX) {
                aiConflict++;
            }

            if (c.blockCode != null && c.blockCode.length() > 0) {
                blocked++;
            }
        }

        r.candidateCount = candidates.size();
        r.validCandidateCount = valid;
        r.invalidCandidateCount = invalid;
        r.overGateCandidateCount = overGate;
        r.blockedCandidateCount = blocked;

        if (valid <= 0) {
            addIssue(r, LEVEL_FATAL, CODE_CANDIDATE_INVALID, "候选全部非法，不符合单位置固定K3");
        }

        if (invalid > 0) {
            addIssue(r, LEVEL_WARN, CODE_CANDIDATE_INVALID, "存在非法候选：" + invalid + "个");
        }

        if (illegalK3 > 0) {
            addIssue(r, LEVEL_BAD, CODE_CANDIDATE_INVALID, "存在非单位置K3候选：" + illegalK3 + "个");
        }

        if (valid > 0 && overGate <= 0) {
            addIssue(r, LEVEL_BAD, CODE_PROBABILITY_LOW, "没有候选同时超过最低概率和动态门槛");
        }

        if (valid > 0 && weakEvidence >= Math.max(1, valid / 2)) {
            addIssue(r, LEVEL_WARN, CODE_EVIDENCE_WEAK, "超过一半候选证据分偏弱");
        }

        if (valid > 0 && highRisk >= Math.max(1, valid / 2)) {
            addIssue(r, LEVEL_BAD, CODE_RISK_HIGH, "超过一半候选风险偏高");
        }

        if (driftHigh > 0) {
            addIssue(r, LEVEL_WARN, CODE_DRIFT_HIGH, "存在漂移风险偏高候选：" + driftHigh + "个");
        }

        if (breakHigh > 0) {
            addIssue(r, LEVEL_WARN, CODE_BREAK_HIGH, "存在破位风险偏高候选：" + breakHigh + "个");
        }

        if (auditHigh > 0) {
            addIssue(r, LEVEL_WARN, CODE_AUDIT_HIGH, "存在概率审计惩罚偏高候选：" + auditHigh + "个");
        }

        if (aiConflict > 0) {
            addIssue(r, LEVEL_WARN, CODE_AI_CONFLICT, "存在AI冲突偏高候选：" + aiConflict + "个");
        }

        if (best != null) {
            double bestGate =
                    best.dynamicGate > 0.0d ? best.dynamicGate : AppConfig.DEFAULT_DYNAMIC_GATE;

            r.bestCandidateText =
                    UiFormatter.positionAndNumbers(best.position, best.n1, best.n2, best.n3)
                            + " FinalP="
                            + UiFormatter.probability(best.finalP)
                            + " Gate="
                            + UiFormatter.probability(bestGate)
                            + " Evidence="
                            + UiFormatter.probability(best.evidenceScore)
                            + " Risk="
                            + UiFormatter.probability(best.evidenceRisk)
                            + " Block="
                            + UiFormatter.blockReasonText(best.blockCode);
        }

        r.candidateText =
                "候选诊断：总数="
                        + candidates.size()
                        + "，合法="
                        + valid
                        + "，非法="
                        + invalid
                        + "，非K3="
                        + illegalK3
                        + "，过门="
                        + overGate
                        + "，低概率="
                        + lowProbability
                        + "，证据弱="
                        + weakEvidence
                        + "，高风险="
                        + highRisk
                        + "，漂移高="
                        + driftHigh
                        + "，破位高="
                        + breakHigh
                        + "，审计惩罚="
                        + auditHigh
                        + "，AI冲突="
                        + aiConflict
                        + "，已拦截="
                        + blocked;
    }

    private void diagnoseTrades(DiagnosisReport r, List<Models.Trade> trades) {
        if (r == null) {
            return;
        }

        if (trades == null || trades.size() == 0) {
            r.tradeText = "交易统计诊断：暂无交易，不影响运行";
            return;
        }

        int real = 0;
        int shadow = 0;
        int hold = 0;
        int unknownScope = 0;

        int realSettled = 0;
        int realHit = 0;
        int realMiss = 0;
        int realDataError = 0;
        int realOpen = 0;

        int mixed = 0;
        int illegalReal = 0;
        int resultButNotSettled = 0;
        int settledButNoResult = 0;
        int pnlError = 0;

        double realPnl = 0.0d;

        for (int i = 0; i < trades.size(); i++) {
            Models.Trade t = trades.get(i);

            if (t == null) {
                continue;
            }

            String scope = normalizeScope(stringValue(t, "statScope", ""));
            String result = normalizeResult(stringValue(t, "result", ""));
            String action = normalizeDecisionText(stringValue(t, "action", ""));
            int hitValue = intValue(t, "hit", 0);
            int settled = intValue(t, "settled", 0);
            double pnl = doubleValue(t, "pnl", 0.0d);

            boolean isReal = AppConfig.STAT_SCOPE_REAL_TRADE.equals(scope);
            boolean isShadow = AppConfig.STAT_SCOPE_SHADOW.equals(scope);
            boolean isHold = AppConfig.STAT_SCOPE_HOLD.equals(scope);

            if (isReal) {
                real++;
            } else if (isShadow) {
                shadow++;
            } else if (isHold) {
                hold++;
            } else {
                unknownScope++;
            }

            boolean hasWinMiss =
                    AppConfig.RESULT_WIN.equals(result) || AppConfig.RESULT_MISS.equals(result);

            if (!isReal && hasWinMiss) {
                mixed++;
            }

            if (!isReal && Math.abs(pnl) > 0.000001d) {
                mixed++;
            }

            if (isReal) {
                if (!AppConfig.DECISION_BET.equals(action) && action.length() > 0) {
                    illegalReal++;
                }

                if (!isValidTradeK3(t)) {
                    illegalReal++;
                }

                if (settled == 1) {
                    realSettled++;

                    if (AppConfig.RESULT_WIN.equals(result) || hitValue == 1) {
                        realHit++;
                    } else if (AppConfig.RESULT_MISS.equals(result)) {
                        realMiss++;
                    } else if (AppConfig.RESULT_DATA_ERROR.equals(result)) {
                        realDataError++;
                    } else {
                        settledButNoResult++;
                    }

                    realPnl += pnl;
                } else {
                    realOpen++;

                    if (hasWinMiss || AppConfig.RESULT_DATA_ERROR.equals(result)) {
                        resultButNotSettled++;
                    }
                }

                if (AppConfig.RESULT_WIN.equals(result)) {
                    if (Math.abs(pnl - AppConfig.WIN_NET_PROFIT) > 0.01d) {
                        pnlError++;
                    }
                }

                if (AppConfig.RESULT_MISS.equals(result)) {
                    if (Math.abs(pnl - AppConfig.LOSE_NET_PROFIT) > 0.01d) {
                        pnlError++;
                    }
                }
            }
        }

        r.realTradeCount = real;
        r.shadowTradeCount = shadow;
        r.holdTradeCount = hold;
        r.tradeHitCount = realHit;
        r.tradeMissCount = realMiss;
        r.mixedStatCount = mixed;
        r.realSettledCount = realSettled;
        r.realOpenCount = realOpen;
        r.realDataErrorCount = realDataError;
        r.realPnl = realPnl;

        if (mixed > 0) {
            addIssue(r, LEVEL_BAD, CODE_STATS_MIXED, "发现HOLD/SHADOW被写入胜负或盈亏统计：" + mixed + "条");
        }

        if (illegalReal > 0) {
            addIssue(
                    r,
                    LEVEL_BAD,
                    CODE_REAL_TRADE_ERROR,
                    "真实交易存在非BET或非单位置K3记录：" + illegalReal + "条");
        }

        if (resultButNotSettled > 0) {
            addIssue(
                    r,
                    LEVEL_BAD,
                    CODE_SETTLEMENT_ERROR,
                    "交易已有结果但settled未置1：" + resultButNotSettled + "条");
        }

        if (settledButNoResult > 0) {
            addIssue(
                    r,
                    LEVEL_WARN,
                    CODE_SETTLEMENT_ERROR,
                    "交易settled=1但结果为空或异常：" + settledButNoResult + "条");
        }

        if (pnlError > 0) {
            addIssue(r, LEVEL_BAD, CODE_SETTLEMENT_ERROR, "真实交易盈亏与赔率口径不一致：" + pnlError + "条");
        }

        if (unknownScope > 0) {
            addIssue(r, LEVEL_WARN, CODE_STATS_MIXED, "存在未知统计范围交易：" + unknownScope + "条");
        }

        if (trades.size() >= 100) {
            double realRate = ((double) real) / ((double) trades.size());

            if (realRate < MIN_REAL_BET_RATE) {
                addIssue(r, LEVEL_WARN, CODE_LONG_HOLD, "长期真实BET频率过低，可能一直HOLD");
            }
        }

        double hitRate = realSettled > 0 ? ((double) realHit) / ((double) realSettled) : 0.0d;

        r.tradeText =
                "交易统计诊断：总记录="
                        + trades.size()
                        + "，REAL="
                        + real
                        + "，REAL已结="
                        + realSettled
                        + "，REAL未结="
                        + realOpen
                        + "，SHADOW="
                        + shadow
                        + "，HOLD="
                        + hold
                        + "，真实命中="
                        + realHit
                        + "，真实未中="
                        + realMiss
                        + "，真实数据错="
                        + realDataError
                        + "，真实胜率="
                        + UiFormatter.probability(hitRate)
                        + "，真实盈亏="
                        + UiFormatter.money(realPnl)
                        + "，混计="
                        + mixed
                        + "，非法真实="
                        + illegalReal
                        + "，结算异常="
                        + (resultButNotSettled + settledButNoResult)
                        + "，盈亏异常="
                        + pnlError;
    }

    private void diagnoseProfiles(DiagnosisReport r, List<Models.DeepProfile> profiles) {
        if (r == null) {
            return;
        }

        if (profiles == null || profiles.size() == 0) {
            addIssue(r, LEVEL_WARN, CODE_PROFILE_WEAK, "暂无深度画像，早期会依赖回退画像和基础证据");
            r.profileText = "画像诊断：暂无画像";
            return;
        }

        int total = 0;
        int active = 0;
        int stale = 0;
        int recover = 0;
        int weakSample = 0;
        int failHigh = 0;

        for (int i = 0; i < profiles.size(); i++) {
            Models.DeepProfile p = profiles.get(i);

            if (p == null) {
                continue;
            }

            total++;

            try {
                p.recalc();
            } catch (Throwable e) {
            }

            if (p.activeFlag == 1) {
                active++;
            }

            if (p.staleFlag == 1) {
                stale++;
            }

            if (p.recoverFlag == 1) {
                recover++;
            }

            if (p.totalSample < AppConfig.PROMOTION_BASE_SAMPLE_MIN) {
                weakSample++;
            }

            if (p.failCount >= 8) {
                failHigh++;
            }
        }

        r.profileCount = total;
        r.activeProfileCount = active;
        r.staleProfileCount = stale;
        r.weakProfileCount = weakSample;

        if (total <= 0) {
            addIssue(r, LEVEL_WARN, CODE_PROFILE_WEAK, "画像列表为空");
        } else {
            if (active <= 0) {
                addIssue(r, LEVEL_BAD, CODE_PROFILE_WEAK, "没有可用活跃画像");
            }

            if (weakSample >= Math.max(1, total / 2)) {
                addIssue(r, LEVEL_WARN, CODE_PROFILE_WEAK, "超过一半画像样本不足");
            }

            if (stale >= Math.max(1, total / 2)) {
                addIssue(r, LEVEL_WARN, CODE_PROFILE_WEAK, "超过一半画像已过期");
            }

            if (failHigh > 0) {
                addIssue(r, LEVEL_WARN, CODE_PROFILE_WEAK, "存在连续失败画像：" + failHigh + "个");
            }
        }

        r.profileText =
                "画像诊断：总画像="
                        + total
                        + "，活跃="
                        + active
                        + "，过期="
                        + stale
                        + "，恢复="
                        + recover
                        + "，样本弱="
                        + weakSample
                        + "，连续失败高="
                        + failHigh;
    }

    private void diagnoseDecision(DiagnosisReport r, Models.Decision d) {
        if (r == null) {
            return;
        }

        if (d == null) {
            r.decisionText = "决策诊断：暂无上一条Decision";
            return;
        }

        String decision = normalizeDecisionText(safe(d.decision));
        String action = normalizeDecisionText(safe(d.action));
        String scope = normalizeScope(safe(d.statScope));
        String block = safe(d.blockCode);
        String result = normalizeResult(safe(d.result));

        if (decision.length() == 0 && action.length() > 0) {
            decision = action;
        }

        if (action.length() == 0 && decision.length() > 0) {
            action = decision;
        }

        if (AppConfig.DECISION_HOLD.equals(decision) || AppConfig.ACTION_HOLD.equals(action)) {
            if (block.length() > 0) {
                addIssue(
                        r,
                        LEVEL_WARN,
                        CODE_LONG_HOLD,
                        "上一条决策为HOLD，原因=" + UiFormatter.blockReasonText(block));
            } else {
                addIssue(r, LEVEL_WARN, CODE_LONG_HOLD, "上一条决策为HOLD，但没有清晰blockCode");
            }

            if (AppConfig.RESULT_WIN.equals(result) || AppConfig.RESULT_MISS.equals(result)) {
                addIssue(r, LEVEL_BAD, CODE_STATS_MIXED, "上一条HOLD决策被写入胜负结果");
            }
        }

        if (AppConfig.DECISION_SHADOW.equals(decision) || AppConfig.ACTION_SHADOW.equals(action)) {
            if (AppConfig.RESULT_WIN.equals(result) || AppConfig.RESULT_MISS.equals(result)) {
                addIssue(r, LEVEL_BAD, CODE_STATS_MIXED, "上一条SHADOW决策被写入真实胜负结果");
            }
        }

        if (AppConfig.DECISION_BET.equals(decision) || AppConfig.ACTION_BET.equals(action)) {
            normalizeDecisionK3(d);

            if (!AppConfig.STAT_SCOPE_REAL_TRADE.equals(scope)) {
                addIssue(r, LEVEL_BAD, CODE_REAL_TRADE_ERROR, "上一条BET不是REAL_TRADE，真实统计会错");
            }

            if (d.position < AppConfig.MIN_POSITION
                    || d.position > AppConfig.MAX_POSITION
                    || !Models.isValidK3Mask(d.k3Mask)) {
                addIssue(r, LEVEL_FATAL, CODE_CANDIDATE_INVALID, "上一条BET决策不是合法单位置K3");
            }

            if (d.finalP < AppConfig.MIN_REAL_BET_PROBABILITY) {
                addIssue(r, LEVEL_BAD, CODE_PROBABILITY_LOW, "上一条BET低于最低真实概率线");
            }

            double gate = d.dynamicGate > 0.0d ? d.dynamicGate : AppConfig.DEFAULT_DYNAMIC_GATE;

            if (d.finalP < gate) {
                addIssue(r, LEVEL_BAD, CODE_PROBABILITY_LOW, "上一条BET未通过动态门槛");
            }
        }

        if (d.evidenceRisk >= MAX_RISK_BAD && d.finalP < 0.430d) {
            addIssue(r, LEVEL_BAD, CODE_RISK_HIGH, "上一条决策证据风险过高");
        }

        if (d.conflictLevel >= AppConfig.AI_JUDGE_CONFLICT_MAX) {
            addIssue(r, LEVEL_WARN, CODE_AI_CONFLICT, "上一条决策AI冲突偏高");
        }

        double gateText = d.dynamicGate > 0.0d ? d.dynamicGate : AppConfig.DEFAULT_DYNAMIC_GATE;

        r.decisionText =
                "决策诊断：decision="
                        + decision
                        + "，action="
                        + action
                        + "，scope="
                        + scope
                        + "，候选="
                        + UiFormatter.positionAndNumbers(d.position, d.n1, d.n2, d.n3)
                        + "，FinalP="
                        + UiFormatter.probability(d.finalP)
                        + "，Gate="
                        + UiFormatter.probability(gateText)
                        + "，Evidence="
                        + UiFormatter.probability(d.evidenceScore)
                        + "，Risk="
                        + UiFormatter.probability(d.evidenceRisk)
                        + "，Result="
                        + result
                        + "，Block="
                        + UiFormatter.blockReasonText(block);
    }

    private void diagnoseRuntime(DiagnosisReport r, Object runtimeState) {
        if (r == null) {
            return;
        }

        if (runtimeState == null) {
            r.runtimeText = "运行诊断：暂无运行状态对象";
            return;
        }

        long now = System.currentTimeMillis();

        long lastFetchAt = longValue(runtimeState, "lastFetchAt", 0L);
        long lastSuccessAt = longValue(runtimeState, "lastSuccessAt", 0L);
        long latestDrawAt = longValue(runtimeState, "latestDrawAt", 0L);
        int running = intValue(runtimeState, "running", 1);
        int backgroundAlive = intValue(runtimeState, "backgroundAlive", 1);
        int countdownSeconds = intValue(runtimeState, "countdownSeconds", -1);
        int consecutiveError = intValue(runtimeState, "consecutiveError", 0);
        String apiStatus = stringValue(runtimeState, "apiStatus", "");
        String serviceStatus = stringValue(runtimeState, "serviceStatus", "");

        if (running == 0) {
            addIssue(r, LEVEL_BAD, CODE_RUNTIME_WEAK, "运行状态显示未启动");
        }

        if (backgroundAlive == 0) {
            addIssue(r, LEVEL_BAD, CODE_RUNTIME_WEAK, "后台保活状态异常");
        }

        if (countdownSeconds < -1) {
            addIssue(r, LEVEL_WARN, CODE_RUNTIME_WEAK, "倒计时状态异常");
        }

        if (consecutiveError >= 5) {
            addIssue(r, LEVEL_BAD, CODE_RUNTIME_WEAK, "接口连续错误次数过多：" + consecutiveError);
        } else if (consecutiveError >= 2) {
            addIssue(r, LEVEL_WARN, CODE_RUNTIME_WEAK, "接口存在连续错误：" + consecutiveError);
        }

        if (lastSuccessAt > 0L && now - lastSuccessAt > 10L * 60L * 1000L) {
            addIssue(r, LEVEL_WARN, CODE_RUNTIME_WEAK, "超过10分钟没有成功抓取");
        }

        if (latestDrawAt > 0L && now - latestDrawAt > 10L * 60L * 1000L) {
            addIssue(r, LEVEL_WARN, CODE_RUNTIME_WEAK, "最新开奖时间可能滞后");
        }

        r.runtimeText =
                "运行诊断：running="
                        + running
                        + "，background="
                        + backgroundAlive
                        + "，倒计时="
                        + countdownSeconds
                        + "，连续错误="
                        + consecutiveError
                        + "，API="
                        + apiStatus
                        + "，Service="
                        + serviceStatus
                        + "，lastFetch="
                        + lastFetchAt
                        + "，lastSuccess="
                        + lastSuccessAt;
    }

    private void addIssue(DiagnosisReport r, String level, String code, String text) {
        if (r == null) {
            return;
        }

        DiagnosisIssue issue = new DiagnosisIssue();
        issue.level = level == null ? LEVEL_WARN : level;
        issue.code = code == null ? CODE_OK : code;
        issue.text = text == null ? "" : text;
        issue.createdAt = System.currentTimeMillis();

        r.issues.add(issue);

        if (LEVEL_FATAL.equals(issue.level)) {
            r.fatalCount++;
        } else if (LEVEL_BAD.equals(issue.level)) {
            r.badCount++;
        } else if (LEVEL_WARN.equals(issue.level)) {
            r.warnCount++;
        }
    }

    private void finalizeReport(DiagnosisReport r) {
        if (r == null) {
            return;
        }

        if (r.fatalCount > 0) {
            r.level = LEVEL_FATAL;
        } else if (r.badCount > 0) {
            r.level = LEVEL_BAD;
        } else if (r.warnCount > 0) {
            r.level = LEVEL_WARN;
        } else {
            r.level = LEVEL_OK;
        }

        DiagnosisIssue main = pickMainIssue(r);

        if (main != null) {
            r.mainCode = main.code;
            r.shortText = main.text;
        } else {
            r.mainCode = CODE_OK;
            r.shortText = "系统诊断正常";
        }

        r.repairAdvice = buildRepairAdvice(r);
        r.summaryText = buildReportSummary(r);
    }

    private DiagnosisIssue pickMainIssue(DiagnosisReport r) {
        if (r == null || r.issues.size() == 0) {
            return null;
        }

        DiagnosisIssue best = null;

        for (int i = 0; i < r.issues.size(); i++) {
            DiagnosisIssue issue = r.issues.get(i);

            if (issue == null) {
                continue;
            }

            if (best == null || issueWeight(issue.level) > issueWeight(best.level)) {
                best = issue;
            }
        }

        return best;
    }

    private int issueWeight(String level) {
        if (LEVEL_FATAL.equals(level)) {
            return 4;
        }

        if (LEVEL_BAD.equals(level)) {
            return 3;
        }

        if (LEVEL_WARN.equals(level)) {
            return 2;
        }

        return 1;
    }

    private String buildRepairAdvice(DiagnosisReport r) {
        if (r == null) {
            return "";
        }

        String advice = "";

        if (hasCode(r, CODE_DATA_EMPTY) || hasCode(r, CODE_DATA_WEAK)) {
            advice = append(advice, "先抓取/回补至少30期进入受控启动；稳定性仍需120/240/500期严格OOS验证");
        }

        if (hasCode(r, CODE_CANDIDATE_EMPTY)) {
            advice = append(advice, "检查候选生成链路，确认 ProbabilityEngine 输出不为空");
        }

        if (hasCode(r, CODE_PROBABILITY_LOW)) {
            advice = append(advice, "当前不是强出手窗口，允许HOLD，不要强行降低门槛");
        }

        if (hasCode(r, CODE_EVIDENCE_WEAK)) {
            advice = append(advice, "等待位置强度、号码证据、周期、转移至少三层共振");
        }

        if (hasCode(r, CODE_RISK_HIGH)
                || hasCode(r, CODE_DRIFT_HIGH)
                || hasCode(r, CODE_BREAK_HIGH)) {
            advice = append(advice, "风险偏高时优先保胜率，等待漂移和破位恢复");
        }

        if (hasCode(r, CODE_STATS_MIXED)) {
            advice = append(advice, "立即检查Trade/Decision写入逻辑，REAL_TRADE、HOLD、SHADOW必须分开统计");
        }

        if (hasCode(r, CODE_REAL_TRADE_ERROR)) {
            advice = append(advice, "真实交易必须只允许BET + REAL_TRADE + 单位置K3");
        }

        if (hasCode(r, CODE_SETTLEMENT_ERROR)) {
            advice = append(advice, "检查SettlementEngine和DbStore，settled/result/pnl必须同步更新");
        }

        if (hasCode(r, CODE_PROFILE_WEAK)) {
            advice = append(advice, "画像样本不足属于早期正常现象，先使用fallback画像，不要直接当作强证据");
        }

        if (hasCode(r, CODE_RUNTIME_WEAK)) {
            advice = append(advice, "检查前台服务、WakeLock、倒计时和接口连续错误");
        }

        if (advice.length() == 0) {
            advice = "系统结构正常，继续按单位置K3、动态门槛、证据过滤运行";
        }

        return advice;
    }

    private boolean hasCode(DiagnosisReport r, String code) {
        if (r == null || code == null) {
            return false;
        }

        for (int i = 0; i < r.issues.size(); i++) {
            DiagnosisIssue issue = r.issues.get(i);

            if (issue != null && code.equals(issue.code)) {
                return true;
            }
        }

        return false;
    }

    private String buildReportSummary(DiagnosisReport r) {
        if (r == null) {
            return "自诊断：暂无";
        }

        StringBuilder sb = new StringBuilder();

        sb.append("自诊断：");
        sb.append(r.level);

        sb.append("，主因=");
        sb.append(r.mainCode);

        sb.append("，说明=");
        sb.append(r.shortText);

        sb.append("，历史=");
        sb.append(r.validDrawCount);
        sb.append("/");
        sb.append(r.historyCount);

        sb.append("，候选=");
        sb.append(r.validCandidateCount);
        sb.append("/");
        sb.append(r.candidateCount);

        sb.append("，过门候选=");
        sb.append(r.overGateCandidateCount);

        sb.append("，REAL=");
        sb.append(r.realTradeCount);

        sb.append("，REAL已结=");
        sb.append(r.realSettledCount);

        sb.append("，SHADOW=");
        sb.append(r.shadowTradeCount);

        sb.append("，HOLD=");
        sb.append(r.holdTradeCount);

        sb.append("，真实盈亏=");
        sb.append(UiFormatter.money(r.realPnl));

        sb.append("，画像=");
        sb.append(r.activeProfileCount);
        sb.append("/");
        sb.append(r.profileCount);

        if (r.bestCandidateText != null && r.bestCandidateText.length() > 0) {
            sb.append("，最佳候选=");
            sb.append(r.bestCandidateText);
        }

        sb.append("，建议=");
        sb.append(r.repairAdvice);

        return sb.toString();
    }

    public Models.SelfDiagnosisResult toModel(DiagnosisReport r) {
        Models.SelfDiagnosisResult m = new Models.SelfDiagnosisResult();

        if (r == null) {
            return m;
        }

        set(m, "createdAt", Long.valueOf(r.createdAt));
        set(m, "level", r.level);
        set(m, "mainCode", r.mainCode);
        set(m, "mainReasonCode", r.mainCode);
        set(m, "shortText", r.shortText);
        set(m, "summaryText", r.summaryText);
        set(m, "repairAdvice", r.repairAdvice);

        set(m, "historyCount", Integer.valueOf(r.historyCount));
        set(m, "validDrawCount", Integer.valueOf(r.validDrawCount));
        set(m, "invalidDrawCount", Integer.valueOf(r.invalidDrawCount));
        set(m, "duplicateIssueCount", Integer.valueOf(r.duplicateIssueCount));
        set(m, "latestIssue", r.latestIssue);

        set(m, "candidateCount", Integer.valueOf(r.candidateCount));
        set(m, "validCandidateCount", Integer.valueOf(r.validCandidateCount));
        set(m, "invalidCandidateCount", Integer.valueOf(r.invalidCandidateCount));
        set(m, "overGateCandidateCount", Integer.valueOf(r.overGateCandidateCount));
        set(m, "blockedCandidateCount", Integer.valueOf(r.blockedCandidateCount));

        set(m, "realTradeCount", Integer.valueOf(r.realTradeCount));
        set(m, "shadowTradeCount", Integer.valueOf(r.shadowTradeCount));
        set(m, "holdTradeCount", Integer.valueOf(r.holdTradeCount));
        set(m, "tradeHitCount", Integer.valueOf(r.tradeHitCount));
        set(m, "tradeMissCount", Integer.valueOf(r.tradeMissCount));
        set(m, "mixedStatCount", Integer.valueOf(r.mixedStatCount));

        set(m, "realSettledCount", Integer.valueOf(r.realSettledCount));
        set(m, "realOpenCount", Integer.valueOf(r.realOpenCount));
        set(m, "realDataErrorCount", Integer.valueOf(r.realDataErrorCount));
        set(m, "realPnl", Double.valueOf(r.realPnl));

        set(m, "profileCount", Integer.valueOf(r.profileCount));
        set(m, "activeProfileCount", Integer.valueOf(r.activeProfileCount));
        set(m, "staleProfileCount", Integer.valueOf(r.staleProfileCount));
        set(m, "weakProfileCount", Integer.valueOf(r.weakProfileCount));

        set(m, "fatalCount", Integer.valueOf(r.fatalCount));
        set(m, "badCount", Integer.valueOf(r.badCount));
        set(m, "warnCount", Integer.valueOf(r.warnCount));

        set(m, "dataText", r.dataText);
        set(m, "candidateText", r.candidateText);
        set(m, "tradeText", r.tradeText);
        set(m, "profileText", r.profileText);
        set(m, "decisionText", r.decisionText);
        set(m, "runtimeText", r.runtimeText);
        set(m, "bestCandidateText", r.bestCandidateText);

        return m;
    }

    private String inferBlockCode(String code) {
        if (code == null) {
            return AppConfig.BLOCK_COMBO_WEAK;
        }

        if (CODE_DATA_EMPTY.equals(code)
                || CODE_DATA_WEAK.equals(code)
                || CODE_DATA_INVALID.equals(code)) {
            return AppConfig.BLOCK_HISTORY_WEAK;
        }

        if (CODE_PROBABILITY_LOW.equals(code)) {
            return AppConfig.BLOCK_LOW_PROBABILITY;
        }

        if (CODE_EVIDENCE_WEAK.equals(code)
                || CODE_CANDIDATE_EMPTY.equals(code)
                || CODE_CANDIDATE_INVALID.equals(code)) {
            return AppConfig.BLOCK_COMBO_WEAK;
        }

        if (CODE_DRIFT_HIGH.equals(code)) {
            return AppConfig.BLOCK_DRIFT_HIGH;
        }

        if (CODE_BREAK_HIGH.equals(code)) {
            return AppConfig.BLOCK_BREAK_HIGH;
        }

        if (CODE_PROFILE_WEAK.equals(code)) {
            return AppConfig.BLOCK_BACKTEST_WEAK;
        }

        if (CODE_STATS_MIXED.equals(code)
                || CODE_REAL_TRADE_ERROR.equals(code)
                || CODE_SETTLEMENT_ERROR.equals(code)) {
            return AppConfig.BLOCK_DATA_ERROR;
        }

        return AppConfig.BLOCK_COMBO_WEAK;
    }

    private String pickBlockCode(String oldCode, String newCode) {
        if (oldCode != null
                && oldCode.length() > 0
                && !AppConfig.BLOCK_LOW_PROBABILITY.equals(oldCode)
                && !AppConfig.BLOCK_COMBO_WEAK.equals(oldCode)) {
            return oldCode;
        }

        return newCode == null || newCode.length() == 0 ? AppConfig.BLOCK_COMBO_WEAK : newCode;
    }

    private void normalizeCandidateK3(Models.ComboCandidate c) {
        if (c == null) {
            return;
        }

        if (AppConfig.isValidK3Numbers(c.n1, c.n2, c.n3)) {
            c.k3Mask = Models.buildK3Mask(c.n1, c.n2, c.n3);
        }

        if (c.dynamicGate <= 0.0d) {
            c.dynamicGate = AppConfig.DEFAULT_DYNAMIC_GATE;
        }
    }

    private void normalizeDecisionK3(Models.Decision d) {
        if (d == null) {
            return;
        }

        if (AppConfig.isValidK3Numbers(d.n1, d.n2, d.n3)) {
            d.k3Mask = Models.buildK3Mask(d.n1, d.n2, d.n3);
        }

        if (d.dynamicGate <= 0.0d) {
            d.dynamicGate = AppConfig.DEFAULT_DYNAMIC_GATE;
        }
    }

    private boolean isValidTradeK3(Models.Trade t) {
        if (t == null) {
            return false;
        }

        if (!AppConfig.isValidPosition(t.position)) {
            return false;
        }

        if (AppConfig.isValidK3Numbers(t.n1, t.n2, t.n3)) {
            int mask = Models.buildK3Mask(t.n1, t.n2, t.n3);
            return Models.isValidK3Mask(mask);
        }

        return Models.isValidK3Mask(t.k3Mask);
    }

    private String normalizeScope(String scope) {
        if (scope == null) {
            return AppConfig.STAT_SCOPE_REAL_TRADE;
        }

        String v = scope.trim();

        if (v.length() == 0) {
            return AppConfig.STAT_SCOPE_REAL_TRADE;
        }

        String upper = v.toUpperCase(java.util.Locale.ROOT);

        if ("REAL".equals(upper)
                || "REAL_TRADE".equals(upper)
                || "BET".equals(upper)
                || "TRADE".equals(upper)
                || "真实".equals(v)
                || "真实交易".equals(v)) {
            return AppConfig.STAT_SCOPE_REAL_TRADE;
        }

        if ("SHADOW".equals(upper) || "影子".equals(v)) {
            return AppConfig.STAT_SCOPE_SHADOW;
        }

        if ("HOLD".equals(upper) || "等待".equals(v) || "不出手".equals(v)) {
            return AppConfig.STAT_SCOPE_HOLD;
        }

        return v;
    }

    private String normalizeDecisionText(String value) {
        if (value == null) {
            return "";
        }

        String v = value.trim();

        if (v.length() == 0) {
            return "";
        }

        String upper = v.toUpperCase(java.util.Locale.ROOT);

        if ("BET".equals(upper) || "下注".equals(v) || "真实出手".equals(v)) {
            return AppConfig.DECISION_BET;
        }

        if ("SHADOW".equals(upper) || "影子".equals(v) || "观察".equals(v)) {
            return AppConfig.DECISION_SHADOW;
        }

        if ("HOLD".equals(upper) || "等待".equals(v) || "不出手".equals(v)) {
            return AppConfig.DECISION_HOLD;
        }

        return v;
    }

    private String normalizeResult(String value) {
        if (value == null) {
            return "";
        }

        String v = value.trim();

        if (v.length() == 0) {
            return "";
        }

        String upper = v.toUpperCase(java.util.Locale.ROOT);

        if ("WIN".equals(upper) || "HIT".equals(upper) || "命中".equals(v) || "中".equals(v)) {
            return AppConfig.RESULT_WIN;
        }

        if ("MISS".equals(upper) || "LOSE".equals(upper) || "未中".equals(v) || "不中".equals(v)) {
            return AppConfig.RESULT_MISS;
        }

        if ("WAIT".equals(upper)
                || "PENDING".equals(upper)
                || "OPEN".equals(upper)
                || "等待".equals(v)) {
            return AppConfig.RESULT_WAIT;
        }

        if ("DATA_ERROR".equals(upper) || "数据异常".equals(v)) {
            return AppConfig.RESULT_DATA_ERROR;
        }

        return v;
    }

    private void set(Object target, String fieldName, Object value) {
        if (target == null || fieldName == null || fieldName.length() == 0) {
            return;
        }

        try {
            Field f = target.getClass().getField(fieldName);
            Class type = f.getType();

            if (type == int.class) {
                if (value instanceof Number) {
                    f.setInt(target, ((Number) value).intValue());
                }
            } else if (type == long.class) {
                if (value instanceof Number) {
                    f.setLong(target, ((Number) value).longValue());
                }
            } else if (type == double.class) {
                if (value instanceof Number) {
                    f.setDouble(target, ((Number) value).doubleValue());
                }
            } else if (type == String.class) {
                f.set(target, value == null ? "" : String.valueOf(value));
            } else {
                f.set(target, value);
            }
        } catch (Throwable ignore) {
        }
    }

    private int intValue(Object target, String fieldName, int def) {
        if (target == null || fieldName == null) {
            return def;
        }

        try {
            Field f = target.getClass().getField(fieldName);
            Object v = f.get(target);

            if (v instanceof Number) {
                return ((Number) v).intValue();
            }

            if (v != null) {
                return Integer.parseInt(String.valueOf(v));
            }
        } catch (Throwable ignore) {
        }

        return def;
    }

    private long longValue(Object target, String fieldName, long def) {
        if (target == null || fieldName == null) {
            return def;
        }

        try {
            Field f = target.getClass().getField(fieldName);
            Object v = f.get(target);

            if (v instanceof Number) {
                return ((Number) v).longValue();
            }

            if (v != null) {
                return Long.parseLong(String.valueOf(v));
            }
        } catch (Throwable ignore) {
        }

        return def;
    }

    private double doubleValue(Object target, String fieldName, double def) {
        if (target == null || fieldName == null) {
            return def;
        }

        try {
            Field f = target.getClass().getField(fieldName);
            Object v = f.get(target);

            if (v instanceof Number) {
                return ((Number) v).doubleValue();
            }

            if (v != null) {
                return Double.parseDouble(String.valueOf(v));
            }
        } catch (Throwable ignore) {
        }

        return def;
    }

    private String stringValue(Object target, String fieldName, String def) {
        if (target == null || fieldName == null) {
            return def;
        }

        try {
            Field f = target.getClass().getField(fieldName);
            Object v = f.get(target);

            if (v != null) {
                return String.valueOf(v);
            }
        } catch (Throwable ignore) {
        }

        return def;
    }

    private String append(String oldText, String addText) {
        if (addText == null || addText.length() == 0) {
            return oldText == null ? "" : oldText;
        }

        if (oldText == null || oldText.length() == 0) {
            return addText;
        }

        if (oldText.indexOf(addText) >= 0) {
            return oldText;
        }

        return oldText + "；" + addText;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    public static final class DiagnosisReport {
        public long createdAt;

        public String level;
        public String mainCode;
        public String shortText;
        public String summaryText;
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

        public int realSettledCount;
        public int realOpenCount;
        public int realDataErrorCount;
        public double realPnl;

        public int profileCount;
        public int activeProfileCount;
        public int staleProfileCount;
        public int weakProfileCount;

        public int fatalCount;
        public int badCount;
        public int warnCount;

        public String dataText;
        public String candidateText;
        public String tradeText;
        public String profileText;
        public String decisionText;
        public String runtimeText;
        public String bestCandidateText;

        public ArrayList<DiagnosisIssue> issues;

        public DiagnosisReport() {
            createdAt = 0L;

            level = LEVEL_OK;
            mainCode = CODE_OK;
            shortText = "";
            summaryText = "";
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

            realSettledCount = 0;
            realOpenCount = 0;
            realDataErrorCount = 0;
            realPnl = 0.0d;

            profileCount = 0;
            activeProfileCount = 0;
            staleProfileCount = 0;
            weakProfileCount = 0;

            fatalCount = 0;
            badCount = 0;
            warnCount = 0;

            dataText = "";
            candidateText = "";
            tradeText = "";
            profileText = "";
            decisionText = "";
            runtimeText = "";
            bestCandidateText = "";

            issues = new ArrayList<DiagnosisIssue>();
        }

        public boolean isOk() {
            return LEVEL_OK.equals(level);
        }

        public boolean hasFatal() {
            return fatalCount > 0;
        }

        public boolean hasBad() {
            return badCount > 0;
        }
    }

    public static final class DiagnosisIssue {
        public long createdAt;
        public String level;
        public String code;
        public String text;

        public DiagnosisIssue() {
            createdAt = 0L;
            level = LEVEL_OK;
            code = CODE_OK;
            text = "";
        }
    }
}
