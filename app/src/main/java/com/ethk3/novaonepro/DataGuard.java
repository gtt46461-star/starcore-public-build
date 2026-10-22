package com.ethk3.novaonepro;

import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public final class DataGuard {

    public static final String STATUS_CURRENT_DRAW_STALE = "CURRENT_DRAW_STALE";
    public static final String STATUS_TARGET_NOT_EXACT_NEXT = "TARGET_NOT_EXACT_NEXT";
    public static final String STATUS_HISTORY_GAP = "HISTORY_GAP";
    public static final String STATUS_BACKFILL_ONLY = "BACKFILL_ONLY";

    private static final long DEFAULT_ROUND_INTERVAL_MS = 60L * 1000L;
    private static final long MIN_REASONABLE_INTERVAL_MS = 20L * 1000L;
    private static final long MAX_REASONABLE_INTERVAL_MS = 10L * 60L * 1000L;

    private static final int MAX_ISSUE_STEP_SAFE = 10000;

    private static final int FUTURE_LEAK_CODE_OK = 0;
    private static final int FUTURE_LEAK_CODE_EMPTY = 1;
    private static final int FUTURE_LEAK_CODE_FOUND = 2;
    private static final int FUTURE_LEAK_CODE_DUPLICATE = 3;
    private static final int FUTURE_LEAK_CODE_DIRTY = 4;

    public DataGuard() {}

    public Models.DataCheck checkLatest(Models.Draw draw) {
        if (draw == null) {
            return Models.DataCheck.block(AppConfig.BLOCK_DATA_ERROR, "接口没有返回最新开奖");
        }

        normalizeDrawBeforeCheck(draw);

        Models.DataCheck b02Contract = checkBinanceDrawContract(draw, System.currentTimeMillis());
        if (!b02Contract.ok) return b02Contract;

        if (draw.issue == null || draw.issue.length() == 0) {
            return Models.DataCheck.block(AppConfig.BLOCK_DATA_ERROR, "最新开奖缺少期号");
        }

        if (!isValidPk10Numbers(draw.numbers)) {
            return Models.DataCheck.block(AppConfig.BLOCK_DATA_ERROR, "最新开奖号码不合法");
        }

        if (draw.openTime <= 0L) {
            /*
             * 多趣个别响应只给betNo/openNumber，不给openTime。期号和10个号码合法时，
             * 时间缺失属于可修复元数据，不能把整条候选链误判为DATA_ERROR。
             */
            long repaired = draw.fetchTime > 0L ? draw.fetchTime : System.currentTimeMillis();
            draw.openTime = repaired;
            draw.fetchTime = draw.fetchTime > 0L ? draw.fetchTime : repaired;
        }

        Models.DataCheck pass = Models.DataCheck.pass();
        pass.currentIssue = draw.issue;
        pass.targetIssue = buildNextIssue(draw.issue);
        pass.actualIssue = draw.issue;
        pass.message = "最新开奖合法｜当前期=" + draw.issue + "｜目标期=" + pass.targetIssue;
        return pass;
    }

    public Models.DataCheck checkHistory(List<Models.Draw> history, int minCount) {
        if (history == null || history.size() == 0) {
            return Models.DataCheck.block(AppConfig.BLOCK_HISTORY_WEAK, "历史开奖为空");
        }

        ArrayList<Models.Draw> clean = normalizeHistory(history);

        Models.DataCheck b02History =
                checkBinanceHistoryContract(clean, System.currentTimeMillis());
        if (!b02History.ok) return b02History;

        if (clean.size() < minCount) {
            return Models.DataCheck.block(
                    AppConfig.BLOCK_HISTORY_WEAK,
                    "历史不足，需要" + minCount + "期，当前" + clean.size() + "期");
        }

        HistoryQuality quality = buildHistoryQualityReport(history);

        if (!quality.ok) {
            return Models.DataCheck.block(quality.blockCode, quality.message);
        }

        Models.Draw last = clean.get(clean.size() - 1);

        Models.DataCheck pass = Models.DataCheck.pass();
        if (quality.issueGapCount > 0) pass.code = STATUS_HISTORY_GAP;
        pass.currentIssue = last.issue;
        pass.targetIssue = buildNextIssue(last.issue);
        pass.actualIssue = last.issue;
        pass.message = quality.message;
        return pass;
    }

    public Models.DataCheck checkCanAnalyze(List<Models.Draw> history) {
        if (history == null) {
            return Models.DataCheck.block(AppConfig.BLOCK_HISTORY_WEAK, "历史为空，无法分析");
        }

        int size = normalizeHistory(history).size();

        if (size < AppConfig.MIN_DRAWS_ANALYZE) {
            return Models.DataCheck.block(
                    AppConfig.BLOCK_HISTORY_WEAK,
                    "历史少于" + AppConfig.MIN_DRAWS_ANALYZE + "期，只允许HOLD");
        }

        Models.DataCheck pass = Models.DataCheck.pass();
        pass.message = "历史达到最低分析要求｜有效=" + size + "期";
        return pass;
    }

    public Models.DataCheck checkCanFullRun(List<Models.Draw> history) {
        if (history == null) {
            return Models.DataCheck.block(AppConfig.BLOCK_HISTORY_WEAK, "历史为空，无法满血运行");
        }

        int size = normalizeHistory(history).size();

        if (size < AppConfig.MIN_DRAWS_SHADOW_ONLY) {
            return Models.DataCheck.block(
                    AppConfig.BLOCK_HISTORY_WEAK,
                    "历史少于" + AppConfig.MIN_DRAWS_SHADOW_ONLY + "期，只做保护分析");
        }

        if (size < AppConfig.MIN_DRAWS_LOW_FREQ) {
            Models.DataCheck warn = Models.DataCheck.pass();
            warn.code = AppConfig.REASON_WARN;
            warn.message = "历史少于" + AppConfig.MIN_DRAWS_LOW_FREQ + "期，只允许低频BET";
            warn.shouldHold = 0;
            return warn;
        }

        Models.DataCheck pass = Models.DataCheck.pass();
        pass.message = "历史满足动态学习运行｜有效=" + size + "期";
        return pass;
    }

    public Models.DataCheck checkRealtimeReady(Models.Draw latest, List<Models.Draw> history) {
        Models.DataCheck latestCheck = checkLatest(latest);

        if (!latestCheck.ok) {
            return latestCheck;
        }

        Models.DataCheck analyzeCheck = checkCanAnalyze(history);

        if (!analyzeCheck.ok) {
            return analyzeCheck;
        }

        ArrayList<Models.Draw> clean = normalizeHistory(history);

        if (clean.size() > 0) {
            Models.Draw last = clean.get(clean.size() - 1);

            if (last != null && last.issue != null && latest.issue != null) {
                if (isIssueAfter(last.issue, latest.issue)) {
                    return Models.DataCheck.block(
                            STATUS_CURRENT_DRAW_STALE,
                            "本地历史最新期超过接口最新期，疑似接口错位｜本地=" + last.issue + "｜接口=" + latest.issue);
                }
                if (isIssueAfter(latest.issue, last.issue)
                        && !latest.issue.equals(buildNextIssue(last.issue))) {
                    return Models.DataCheck.block(
                            STATUS_HISTORY_GAP,
                            "最新开奖与本地历史不连续，只允许先补数据｜本地="
                                    + last.issue
                                    + "｜接口="
                                    + latest.issue);
                }
            }
        }

        Models.DataCheck pass = Models.DataCheck.pass();
        pass.currentIssue = latest.issue;
        pass.targetIssue = buildNextIssue(latest.issue);
        pass.actualIssue = latest.issue;
        pass.message = "实时数据可分析｜当前期=" + latest.issue + "｜目标期=" + pass.targetIssue;
        return pass;
    }

    public Models.DataCheck checkDecisionTarget(Models.Decision decision, Models.Draw latestDraw) {
        if (decision == null) {
            return Models.DataCheck.block(AppConfig.BLOCK_DATA_ERROR, "没有待校验决策");
        }

        if (latestDraw == null || !latestDraw.isValid()) {
            return Models.DataCheck.block(AppConfig.BLOCK_DATA_ERROR, "没有可用最新开奖");
        }

        if (decision.targetIssue == null || decision.targetIssue.length() == 0) {
            return Models.DataCheck.block(AppConfig.BLOCK_DATA_ERROR, "决策缺少目标期");
        }

        Models.DataCheck pass = Models.DataCheck.pass();
        pass.currentIssue = latestDraw.issue;
        pass.targetIssue = decision.targetIssue;
        pass.actualIssue = latestDraw.issue;

        if (isSameIssue(decision.targetIssue, latestDraw.issue)) {
            pass.message = "目标期已开奖，可以结算";
            pass.shouldHold = 0;
            return pass;
        }

        if (isIssueAfter(latestDraw.issue, decision.targetIssue)) {
            pass.ok = false;
            pass.code = STATUS_CURRENT_DRAW_STALE;
            pass.reason =
                    "最新开奖已超过目标期，进入错位保护｜目标=" + decision.targetIssue + "｜最新=" + latestDraw.issue;
            pass.message = pass.reason;
            pass.shouldHold = 1;
            return pass;
        }

        String exactNext = buildNextIssue(latestDraw.issue);
        if (!isSameIssue(decision.targetIssue, exactNext)) {
            return Models.DataCheck.block(
                    STATUS_TARGET_NOT_EXACT_NEXT,
                    "待开奖决策目标必须是最新开奖的精确下一期｜最新="
                            + latestDraw.issue
                            + "｜期望="
                            + exactNext
                            + "｜实际="
                            + decision.targetIssue);
        }

        pass.message = "目标期未开奖，继续等待｜目标=" + decision.targetIssue + "｜最新=" + latestDraw.issue;
        pass.shouldHold = 1;
        return pass;
    }

    /** Marks an old recovered draw as data repair only; it can never authorize a predraw row. */
    public Models.DataCheck checkBackfillOnly(
            Models.Draw recovered, Models.Draw authoritativeLatest) {
        if (recovered == null || !recovered.isValid()) {
            return Models.DataCheck.block(AppConfig.BLOCK_DATA_ERROR, "回补开奖号不合法");
        }
        if (authoritativeLatest != null
                && authoritativeLatest.isValid()
                && !isIssueAfter(authoritativeLatest.issue, recovered.issue)) {
            return Models.DataCheck.block(
                    AppConfig.BLOCK_DATA_ERROR, "回补期必须严格早于authoritative current draw");
        }
        Models.DataCheck pass = Models.DataCheck.pass();
        pass.code = STATUS_BACKFILL_ONLY;
        pass.currentIssue = recovered.issue;
        pass.actualIssue = recovered.issue;
        pass.targetIssue = "";
        pass.message = "BACKFILL_ONLY｜只修复draw，不生成历史predraw canonical";
        return pass;
    }

    public Models.DataCheck checkDecisionBeforeSave(
            Models.Decision decision, Models.Trade openTrade) {
        if (decision == null) return Models.DataCheck.block(AppConfig.BLOCK_DATA_ERROR, "决策为空");
        if (decision.targetIssue == null || decision.targetIssue.length() == 0)
            return Models.DataCheck.block(AppConfig.BLOCK_DATA_ERROR, "决策缺少目标期");
        decision.decision = normalizeDecisionText(decision.decision);
        decision.action = normalizeDecisionText(decision.action);
        if (decision.action.length() == 0) decision.action = decision.decision;
        if (AppConfig.DECISION_HOLD.equals(decision.decision)) {
            decision.statScope = AppConfig.STAT_SCOPE_HOLD;
            decision.decisionQualityPath = AppConfig.DECISION_QUALITY_HOLD;
            Models.DataCheck p = Models.DataCheck.pass();
            p.currentIssue = decision.baseIssue;
            p.targetIssue = decision.targetIssue;
            p.message = "HOLD允许保存，不计真实统计";
            return p;
        }
        if (AppConfig.DECISION_SHADOW.equals(decision.decision)) {
            decision.statScope = AppConfig.STAT_SCOPE_SHADOW;
            decision.decisionQualityPath = AppConfig.DECISION_QUALITY_SHADOW;
            Models.DataCheck p = Models.DataCheck.pass();
            p.currentIssue = decision.baseIssue;
            p.targetIssue = decision.targetIssue;
            p.message = "SHADOW允许保存，完整反事实学习，不计真实统计";
            return p;
        }
        if (!AppConfig.DECISION_BET.equals(decision.decision))
            return Models.DataCheck.block(
                    AppConfig.BLOCK_DATA_ERROR, "未知决策类型：" + safe(decision.decision));
        if (decision.statScope != null
                && decision.statScope.length() > 0
                && !AppConfig.STAT_SCOPE_REAL_TRADE.equals(decision.statScope))
            return Models.DataCheck.block(AppConfig.BLOCK_DATA_ERROR, "BET不是REAL_TRADE");
        if (openTrade != null && openTrade.isOpen())
            return Models.DataCheck.block(AppConfig.BLOCK_OPEN_TRADE_EXISTS, "已有未结算真实交易");
        decision.statScope = AppConfig.STAT_SCOPE_REAL_TRADE;
        normalizeDecisionK3(decision);
        if (!decision.hasValidBetContent())
            return Models.DataCheck.block(AppConfig.BLOCK_COMBO_WEAK, "BET必须是单位置K3");
        double gate =
                decision.riskAdjustedGate > 0.0d
                        ? decision.riskAdjustedGate
                        : (decision.dynamicGate > 0.0d
                                ? decision.dynamicGate
                                : AppConfig.DEFAULT_DYNAMIC_GATE);
        double raw = decision.rawP > 0.0d ? decision.rawP : decision.calibratedP;
        double edge = decision.finalP - gate;
        double floorQualityEdge = AppConfig.qualifiedFloorQualityEdge(decision.finalP);
        boolean floor = AppConfig.isQualifiedFloorPath(decision.betDomain, decision.decisionPath);
        if (floor) {
            if (!AppConfig.isQualifiedFloorQuality(
                    decision.finalP,
                    raw,
                    floorQualityEdge,
                    decision.coverageQuality > 0.0d
                            ? decision.coverageQuality
                            : decision.coverageQualityScore,
                    decision.thirdQuality > 0.0d
                            ? decision.thirdQuality
                            : decision.thirdNumberQualityScore,
                    decision.counterRisk > 0.0d
                            ? decision.counterRisk
                            : decision.counterEvidenceScore))
                return Models.DataCheck.block(
                        AppConfig.BLOCK_LOW_PROBABILITY,
                        "QUALIFIED_FLOOR_BET未达到FinalP/RawP/Edge/Coverage/Third质量底线");
            if (decision.hardBlockFlag == 1
                    || AppConfig.isNonOverridableFloorBlock(decision.hardRiskCode))
                return Models.DataCheck.block(
                        AppConfig.BLOCK_DATA_ERROR, "QUALIFIED_FLOOR_BET存在不可覆盖硬风险");
            decision.betDomain = AppConfig.BET_DOMAIN_QUALIFIED_FLOOR;
            decision.decisionPath = AppConfig.DECISION_PATH_QUALIFIED_FLOOR;
            decision.decisionQualityPath = AppConfig.DECISION_QUALITY_FLOOR;
        } else {
            if (decision.finalP
                    < Math.max(
                            AppConfig.MIN_REAL_BET_PROBABILITY,
                            AppConfig.QUALIFIED_FLOOR_MIN_FINAL_P))
                return Models.DataCheck.block(
                        AppConfig.BLOCK_LOW_PROBABILITY, "NORMAL_BET低于35%真实质量底线");
            if (decision.finalP < gate || edge < 0.0d)
                return Models.DataCheck.block(
                        AppConfig.BLOCK_LOW_PROBABILITY, "NORMAL_BET未通过动态门槛或Edge<0");
            decision.decisionQualityPath = AppConfig.DECISION_QUALITY_NORMAL;
        }
        Models.freezeStableEvidenceDomains(decision, decision.targetIssue);
        decision.moduleBuildVersion = AppConfig.ACTIVE_QUALITY_BUILD_VERSION;
        Models.DataCheck pass = Models.DataCheck.pass();
        pass.currentIssue = decision.baseIssue;
        pass.targetIssue = decision.targetIssue;
        pass.message =
                "BET安全检查通过｜"
                        + decision.decisionQualityPath
                        + "｜P"
                        + decision.position
                        + "｜K3="
                        + decision.k3Mask;
        return pass;
    }

    public Models.DataCheck checkCandidateForBet(
            Models.ComboCandidate candidate, Models.Trade openTrade, double currentFreq100) {
        if (candidate == null) return Models.DataCheck.block(AppConfig.BLOCK_COMBO_WEAK, "候选为空");
        if (openTrade != null && openTrade.isOpen())
            return Models.DataCheck.block(AppConfig.BLOCK_OPEN_TRADE_EXISTS, "已有未结算交易");
        normalizeCandidateK3(candidate);
        if (!candidate.isValidK3())
            return Models.DataCheck.block(AppConfig.BLOCK_COMBO_WEAK, "候选不是合法单位置K3");
        double gate =
                candidate.riskAdjustedGate > 0.0d
                        ? candidate.riskAdjustedGate
                        : (candidate.dynamicGate > 0.0d
                                ? candidate.dynamicGate
                                : AppConfig.DEFAULT_DYNAMIC_GATE);
        double edge = candidate.finalP - gate;
        double floorQualityEdge = AppConfig.qualifiedFloorQualityEdge(candidate.finalP);
        boolean floor = AppConfig.isQualifiedFloorPath(candidate.betDomain, candidate.decisionPath);
        if (floor) {
            double raw = candidate.rawP > 0.0d ? candidate.rawP : candidate.calibratedP;
            double cover =
                    candidate.coverageQuality > 0.0d
                            ? candidate.coverageQuality
                            : candidate.coverageQualityScore;
            double third =
                    candidate.thirdQuality > 0.0d
                            ? candidate.thirdQuality
                            : candidate.thirdNumberQualityScore;
            double counter =
                    candidate.counterRisk > 0.0d
                            ? candidate.counterRisk
                            : candidate.counterEvidenceScore;
            if (!AppConfig.isQualifiedFloorQuality(
                    candidate.finalP, raw, floorQualityEdge, cover, third, counter))
                return Models.DataCheck.block(AppConfig.BLOCK_LOW_PROBABILITY, "合格补频候选质量不足");
        } else {
            if (candidate.finalP
                            < Math.max(
                                    AppConfig.MIN_REAL_BET_PROBABILITY,
                                    AppConfig.QUALIFIED_FLOOR_MIN_FINAL_P)
                    || candidate.finalP < gate
                    || edge < 0.0d)
                return Models.DataCheck.block(
                        AppConfig.BLOCK_LOW_PROBABILITY, "NORMAL候选未过35%/Gate/Edge");
        }
        if (candidate.hardBlockFlag == 1
                || AppConfig.isNonOverridableFloorBlock(candidate.hardRiskCode))
            return Models.DataCheck.block(AppConfig.BLOCK_DATA_ERROR, "结构化硬风险");
        Models.DataCheck pass = Models.DataCheck.pass();
        pass.currentIssue = candidate.baseIssue;
        pass.targetIssue = candidate.targetIssue;
        pass.message =
                "候选通过BET安全检查｜"
                        + (floor
                                ? AppConfig.DECISION_QUALITY_FLOOR
                                : AppConfig.DECISION_QUALITY_NORMAL);
        return pass;
    }

    public Models.DataCheck checkCandidateList(
            List<Models.ComboCandidate> candidates, int minCount) {
        if (candidates == null || candidates.size() == 0) {
            return Models.DataCheck.block(AppConfig.BLOCK_COMBO_WEAK, "候选列表为空");
        }

        int valid = 0;
        HashSet<String> unique = new HashSet<String>();

        for (int i = 0; i < candidates.size(); i++) {
            Models.ComboCandidate c = candidates.get(i);

            if (c == null) {
                continue;
            }

            normalizeCandidateK3(c);

            if (!c.isValidK3()) {
                continue;
            }

            String key = c.position + "_" + c.k3Mask;

            if (unique.contains(key)) {
                continue;
            }

            unique.add(key);
            valid++;
        }

        if (valid < minCount) {
            return Models.DataCheck.block(AppConfig.BLOCK_COMBO_WEAK, "有效候选不足，当前" + valid + "个");
        }

        Models.DataCheck pass = Models.DataCheck.pass();
        pass.message = "候选列表有效，去重后" + valid + "个";
        return pass;
    }

    public Models.DataCheck checkOpenTradeSafety(Models.Decision decision, Models.Trade openTrade) {
        if (openTrade == null || !openTrade.isOpen()) {
            return Models.DataCheck.pass();
        }

        if (decision == null) {
            return Models.DataCheck.block(AppConfig.BLOCK_OPEN_TRADE_EXISTS, "已有未结算交易，禁止新BET");
        }

        String decisionText = normalizeDecisionText(decision.decision);

        if (AppConfig.DECISION_BET.equals(decisionText)) {
            return Models.DataCheck.block(AppConfig.BLOCK_OPEN_TRADE_EXISTS, "已有未结算交易，禁止重复BET");
        }

        Models.DataCheck pass = Models.DataCheck.pass();
        pass.message = "已有未结算交易，但当前不是BET";
        pass.shouldHold = 0;
        return pass;
    }

    public Models.DataCheck checkSettlementSafety(Models.Trade trade, Models.Draw actualDraw) {
        if (trade == null) {
            return Models.DataCheck.block(AppConfig.BLOCK_DATA_ERROR, "结算交易为空");
        }

        if (actualDraw == null || !actualDraw.isValid()) {
            return Models.DataCheck.block(AppConfig.BLOCK_DATA_ERROR, "结算开奖无效");
        }

        if (!isRealTradeScope(trade.statScope)) {
            return Models.DataCheck.block(
                    AppConfig.BLOCK_DATA_ERROR, "非真实BET，不进入真实结算：" + safe(trade.statScope));
        }

        if (trade.targetIssue == null || trade.targetIssue.length() == 0) {
            return Models.DataCheck.block(AppConfig.BLOCK_DATA_ERROR, "交易缺少目标期");
        }

        if (actualDraw.issue == null || actualDraw.issue.length() == 0) {
            return Models.DataCheck.block(AppConfig.BLOCK_DATA_ERROR, "开奖缺少期号");
        }

        if (isSameIssue(trade.targetIssue, actualDraw.issue)) {
            Models.DataCheck pass = Models.DataCheck.pass();
            pass.targetIssue = trade.targetIssue;
            pass.actualIssue = actualDraw.issue;
            pass.message = "结算期号匹配";
            pass.shouldHold = 0;
            return pass;
        }

        if (isIssueAfter(actualDraw.issue, trade.targetIssue)) {
            return Models.DataCheck.block(
                    AppConfig.BLOCK_DATA_ERROR,
                    "开奖已经越过目标期，交易进入错位保护｜目标=" + trade.targetIssue + "｜开奖=" + actualDraw.issue);
        }

        Models.DataCheck wait = Models.DataCheck.pass();
        wait.targetIssue = trade.targetIssue;
        wait.actualIssue = actualDraw.issue;
        wait.message = "目标期尚未开奖，等待结算｜目标=" + trade.targetIssue + "｜开奖=" + actualDraw.issue;
        wait.shouldHold = 1;
        return wait;
    }

    public FutureLeakCheck checkNoFutureLeak(
            List<Models.Draw> history, String decisionIssue, long decisionTimeMs) {
        FutureLeakCheck result = new FutureLeakCheck();

        if (history == null || history.size() == 0) {
            result.ok = false;
            result.code = FUTURE_LEAK_CODE_EMPTY;
            result.message = "历史为空，无法做时间点快照";
            return result;
        }

        if (decisionIssue == null) {
            decisionIssue = "";
        }

        HashSet<String> seen = new HashSet<String>();

        for (int i = 0; i < history.size(); i++) {
            Models.Draw d = history.get(i);

            if (d == null || !d.isValid()) {
                result.dirtyCount++;
                continue;
            }

            String issueKey = normalizeIssueKey(d.issue);

            if (seen.contains(issueKey)) {
                result.duplicateCount++;
                continue;
            }

            seen.add(issueKey);

            boolean futureByIssue = false;
            boolean futureByTime = false;

            if (decisionIssue.length() > 0) {
                if (!isIssueBefore(d.issue, decisionIssue)) {
                    futureByIssue = true;
                }
            }

            if (decisionTimeMs > 0L && d.openTime >= decisionTimeMs) {
                futureByTime = true;
            }

            if (futureByIssue || futureByTime) {
                result.futureLeakCount++;
                result.futureIssues.add(d.issue);
            } else {
                result.visibleCount++;
            }
        }

        if (result.dirtyCount > 0) {
            result.ok = false;
            result.code = FUTURE_LEAK_CODE_DIRTY;
            result.message = "历史含脏数据，禁止深测";
            return result;
        }

        if (result.duplicateCount > 0) {
            result.ok = false;
            result.code = FUTURE_LEAK_CODE_DUPLICATE;
            result.message = "历史含重复期号，禁止深测";
            return result;
        }

        if (result.futureLeakCount > 0) {
            result.ok = false;
            result.code = FUTURE_LEAK_CODE_FOUND;
            result.message = "发现未来数据泄露：" + result.futureLeakCount + "期";
            return result;
        }

        result.ok = true;
        result.code = FUTURE_LEAK_CODE_OK;
        result.message = "未发现未来数据泄露，可用于时间点回测";
        return result;
    }

    public Models.PointInTimeSnapshot buildPointInTimeSnapshot(
            List<Models.Draw> visibleHistory,
            String baseIssue,
            String targetIssue,
            int visibleTradeCount,
            long profileVersion,
            long calibrationVersion) {
        ArrayList<Models.Draw> clean = normalizeHistory(visibleHistory);

        Models.PointInTimeSnapshot s = new Models.PointInTimeSnapshot();
        s.baseIssue = baseIssue == null ? "" : baseIssue;
        s.targetIssue = targetIssue == null ? "" : targetIssue;
        s.visibleDrawCount = clean.size();
        s.visibleTradeCount = visibleTradeCount;
        s.visibleProfileVersion = profileVersion;
        s.visibleCalibrationVersion = calibrationVersion;
        s.trainStartIssue = clean.size() > 0 ? clean.get(0).issue : "";
        s.trainEndIssue = clean.size() > 0 ? clean.get(clean.size() - 1).issue : "";
        s.dataHash = buildDataHash(clean);
        s.createdAt = System.currentTimeMillis();
        s.noFutureLeakOk = 1;
        s.status = AppConfig.BACKTEST_STATUS_OK;
        s.warningText = "";
        s.featureSummary = "visibleDraws=" + s.visibleDrawCount;
        s.gateSummary =
                "profileVersion=" + profileVersion + ",calibrationVersion=" + calibrationVersion;
        s.candidateSummary = "";
        s.decisionSummary = "";
        s.snapshotId = buildSnapshotId(s.baseIssue, s.targetIssue, s.dataHash, s.createdAt);
        return s;
    }

    public String buildDataHash(List<Models.Draw> history) {
        ArrayList<Models.Draw> clean = normalizeHistory(history);
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < clean.size(); i++) {
            Models.Draw d = clean.get(i);

            sb.append(d.issue);
            sb.append('|');
            sb.append(d.openTime);
            sb.append('|');

            if (d.numbers != null) {
                for (int j = 0; j < d.numbers.length; j++) {
                    if (j > 0) {
                        sb.append(',');
                    }

                    sb.append(d.numbers[j]);
                }
            }

            sb.append('\n');
        }

        return sha256(sb.toString());
    }

    public ArrayList<Models.Draw> normalizeHistory(List<Models.Draw> input) {
        ArrayList<Models.Draw> list = new ArrayList<Models.Draw>();

        if (input == null) {
            return list;
        }

        HashMap<String, Models.Draw> map = new HashMap<String, Models.Draw>();

        for (int i = 0; i < input.size(); i++) {
            Models.Draw d = input.get(i);

            if (d == null || !d.isValid()) {
                continue;
            }

            String key = normalizeIssueKey(d.issue);

            if (key.length() == 0) {
                continue;
            }

            Models.Draw old = map.get(key);

            if (old == null || shouldReplaceDuplicateDraw(old, d)) {
                map.put(key, d.copy());
            }
        }

        for (String key : map.keySet()) {
            Models.Draw d = map.get(key);

            if (d != null && d.isValid()) {
                list.add(d);
            }
        }

        Collections.sort(
                list,
                new Comparator<Models.Draw>() {
                    public int compare(Models.Draw a, Models.Draw b) {
                        if (a == null && b == null) {
                            return 0;
                        }

                        if (a == null) {
                            return -1;
                        }

                        if (b == null) {
                            return 1;
                        }

                        int issueCompare = compareIssue(a.issue, b.issue);

                        if (issueCompare != 0) {
                            return issueCompare;
                        }

                        if (a.openTime > 0L && b.openTime > 0L && a.openTime != b.openTime) {
                            return a.openTime < b.openTime ? -1 : 1;
                        }

                        String ia = a.issue == null ? "" : a.issue;
                        String ib = b.issue == null ? "" : b.issue;
                        return ia.compareTo(ib);
                    }
                });

        return list;
    }

    public HistoryQuality buildHistoryQualityReport(List<Models.Draw> history) {
        HistoryQuality q = new HistoryQuality();

        if (history == null || history.size() == 0) {
            q.ok = false;
            q.blockCode = AppConfig.BLOCK_HISTORY_WEAK;
            q.message = "历史为空";
            return q;
        }

        ArrayList<Models.Draw> clean = normalizeHistory(history);
        q.totalInput = history.size();
        q.validCount = clean.size();

        if (clean.size() == 0) {
            q.ok = false;
            q.blockCode = AppConfig.BLOCK_HISTORY_WEAK;
            q.message = "历史无有效开奖";
            return q;
        }

        HashSet<String> issues = new HashSet<String>();
        int dirty = 0;
        int duplicate = 0;

        for (int i = 0; i < history.size(); i++) {
            Models.Draw d = history.get(i);

            if (d == null || !d.isValid()) {
                dirty++;
                continue;
            }

            String issueKey = normalizeIssueKey(d.issue);

            if (issues.contains(issueKey)) {
                duplicate++;
            } else {
                issues.add(issueKey);
            }
        }

        q.dirtyCount = dirty;
        q.duplicateIssueCount = duplicate;
        q.timeDisorder = hasLargeTimeDisorder(clean) ? 1 : 0;
        q.issueGapCount = countIssueGaps(clean);
        q.intervalMs = inferRoundInterval(clean);
        q.firstIssue = clean.get(0).issue;
        q.latestIssue = clean.get(clean.size() - 1).issue;

        if (dirty > 0) {
            q.ok = false;
            q.blockCode = AppConfig.BLOCK_DATA_ERROR;
            q.message = "历史存在脏数据：" + dirty + "条";
            return q;
        }

        if (duplicate > 0) {
            q.ok = false;
            q.blockCode = AppConfig.BLOCK_DATA_ERROR;
            q.message = "历史存在重复期号：" + duplicate + "条";
            return q;
        }

        if (q.timeDisorder > 0) {
            q.ok = false;
            q.blockCode = AppConfig.BLOCK_DATA_ERROR;
            q.message = "历史时间顺序异常";
            return q;
        }

        q.ok = true;
        q.blockCode = AppConfig.REASON_PASS;
        q.message = "历史质量通过｜有效" + q.validCount + "期｜缺口" + q.issueGapCount + "期";
        return q;
    }

    public String getRunStage(List<Models.Draw> history) {
        int size = normalizeHistory(history).size();

        if (size < AppConfig.MIN_DRAWS_ANALYZE) {
            return "HOLD_ONLY";
        }

        if (size < AppConfig.MIN_DRAWS_SHADOW_ONLY) {
            return "ANALYZE_ONLY";
        }

        if (size < AppConfig.MIN_DRAWS_LOW_FREQ) {
            return "LOW_FREQ";
        }

        if (size < AppConfig.MIN_DRAWS_FULL_RUN) {
            return "NORMAL";
        }

        return "FULL";
    }

    public boolean isShadowOnlyStage(List<Models.Draw> history) {
        String stage = getRunStage(history);
        return "HOLD_ONLY".equals(stage) || "ANALYZE_ONLY".equals(stage);
    }

    public boolean isLowFreqStage(List<Models.Draw> history) {
        return "LOW_FREQ".equals(getRunStage(history));
    }

    public boolean isFullRunStage(List<Models.Draw> history) {
        String stage = getRunStage(history);
        return "NORMAL".equals(stage) || "FULL".equals(stage);
    }

    /**
     * Legacy compatibility facade. Active R3.6 code calls TargetIssueResolver directly; this
     * delegation prevents old observers from getting 1441 at a UTC day boundary.
     */
    @Deprecated
    public String buildNextIssue(String currentIssue) {
        TargetIssueResolver.Resolution result = TargetIssueResolver.next(currentIssue);
        return result.valid ? result.targetIssue : "";
    }

    public long estimateNextOpenTime(List<Models.Draw> history) {
        ArrayList<Models.Draw> clean = normalizeHistory(history);

        if (clean.size() == 0) {
            return System.currentTimeMillis() + DEFAULT_ROUND_INTERVAL_MS;
        }

        Models.Draw last = clean.get(clean.size() - 1);
        long interval = inferRoundInterval(clean);

        if (last.openTime <= 0L) {
            return System.currentTimeMillis() + interval;
        }

        long next = last.openTime + interval;
        long now = System.currentTimeMillis();

        while (next < now - interval) {
            next += interval;
        }

        return next;
    }

    public long inferRoundInterval(List<Models.Draw> history) {
        ArrayList<Models.Draw> clean = normalizeHistory(history);

        if (clean.size() < 3) {
            return DEFAULT_ROUND_INTERVAL_MS;
        }

        ArrayList<Long> intervals = new ArrayList<Long>();

        for (int i = 1; i < clean.size(); i++) {
            long a = clean.get(i - 1).openTime;
            long b = clean.get(i).openTime;

            if (a <= 0L || b <= 0L) {
                continue;
            }

            long diff = b - a;

            if (diff >= MIN_REASONABLE_INTERVAL_MS && diff <= MAX_REASONABLE_INTERVAL_MS) {
                intervals.add(Long.valueOf(diff));
            }
        }

        if (intervals.size() == 0) {
            return DEFAULT_ROUND_INTERVAL_MS;
        }

        Collections.sort(intervals);

        int mid = intervals.size() / 2;
        long median;

        if (intervals.size() % 2 == 1) {
            median = intervals.get(mid).longValue();
        } else {
            long a = intervals.get(mid - 1).longValue();
            long b = intervals.get(mid).longValue();
            median = (a + b) / 2L;
        }

        if (median < MIN_REASONABLE_INTERVAL_MS || median > MAX_REASONABLE_INTERVAL_MS) {
            return DEFAULT_ROUND_INTERVAL_MS;
        }

        return median;
    }

    public boolean isNewDraw(Models.Draw latest, Models.Draw oldLatest) {
        if (latest == null || !latest.isValid() || !TargetIssueResolver.isValid(latest.issue)) {
            return false;
        }

        if (oldLatest == null
                || !oldLatest.isValid()
                || !TargetIssueResolver.isValid(oldLatest.issue)) {
            return true;
        }

        // The only business advancement is the validated 12-digit issue.  A same-issue open-time
        // correction is metadata and must never create a second settlement/prediction cycle.
        return TargetIssueResolver.isAfter(latest.issue, oldLatest.issue);
    }

    public boolean isIssueAfter(String issueA, String issueB) {
        return compareIssue(issueA, issueB) > 0;
    }

    public boolean isIssueBefore(String issueA, String issueB) {
        return compareIssue(issueA, issueB) < 0;
    }

    public boolean isSameIssue(String a, String b) {
        if (a == null || b == null) {
            return false;
        }

        String aa = a.trim();
        String bb = b.trim();

        if (aa.equals(bb)) {
            return true;
        }

        IssuePart pa = parseIssuePart(aa);
        IssuePart pb = parseIssuePart(bb);

        if (!pa.valid || !pb.valid) {
            return false;
        }

        if (!pa.prefix.equals(pb.prefix)) {
            return false;
        }

        return pa.number == pb.number;
    }

    public int issueStepDistance(String fromIssue, String toIssue) {
        IssuePart a = parseIssuePart(fromIssue);
        IssuePart b = parseIssuePart(toIssue);

        if (!a.valid || !b.valid) {
            return 0;
        }

        if (!a.prefix.equals(b.prefix)) {
            return 0;
        }

        long diff = b.number - a.number;

        if (diff < 0L) {
            diff = -diff;
        }

        if (diff > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }

        return (int) diff;
    }

    public boolean isIssueStepSafe(String fromIssue, String toIssue) {
        int d = issueStepDistance(fromIssue, toIssue);
        return d >= 0 && d < MAX_ISSUE_STEP_SAFE;
    }

    public long countdownMs(long nextOpenTime) {
        long now = System.currentTimeMillis();
        long left = nextOpenTime - now;

        if (left < 0L) {
            return 0L;
        }

        return left;
    }

    public boolean shouldFetchFast(long countdownMs) {
        return countdownMs <= AppConfig.FETCH_FAST_WINDOW_SEC * 1000L;
    }

    public boolean shouldFetchMid(long countdownMs) {
        return countdownMs <= AppConfig.FETCH_MID_WINDOW_SEC * 1000L;
    }

    public int nextFetchIntervalSeconds(long countdownMs) {
        if (shouldFetchFast(countdownMs)) {
            return AppConfig.FETCH_FAST_INTERVAL_SEC;
        }

        if (shouldFetchMid(countdownMs)) {
            return AppConfig.FETCH_MID_INTERVAL_SEC;
        }

        return AppConfig.FETCH_SLOW_INTERVAL_SEC;
    }

    public Models.Decision holdDecision(
            String baseIssue, String targetIssue, String blockCode, String reason) {
        Models.Decision d = new Models.Decision();
        d.baseIssue = baseIssue == null ? "" : baseIssue;
        d.targetIssue = targetIssue == null ? "" : targetIssue;
        d.createdAt = System.currentTimeMillis();
        d.decision = AppConfig.DECISION_HOLD;
        d.action = AppConfig.DECISION_HOLD;
        d.position = 0;
        d.setNumbers(0, 0, 0);
        d.k3Mask = 0;
        d.rawP = 0.0d;
        d.calibratedP = 0.0d;
        d.finalP = 0.0d;
        d.dynamicGate = AppConfig.DEFAULT_DYNAMIC_GATE;
        d.edge = -AppConfig.DEFAULT_DYNAMIC_GATE;
        d.blockCode = blockCode == null ? AppConfig.BLOCK_DATA_ERROR : blockCode;
        d.reasonSummary = reason == null ? "" : reason;
        d.reasonText = d.reasonSummary;
        d.dataOk = 0;
        d.stateMode = AppConfig.MODE_CHAOS;
        d.shapeKey = "";
        d.result = AppConfig.RESULT_WAIT;
        d.settled = 0;
        d.statScope = AppConfig.STAT_SCOPE_HOLD;
        return d;
    }

    public Models.Decision shadowDecisionFromCandidate(Models.ComboCandidate c, String reason) {
        if (c == null) {
            return holdDecision("", "", AppConfig.BLOCK_COMBO_WEAK, reason);
        }

        normalizeCandidateK3(c);

        Models.Decision d = c.toDecision(AppConfig.DECISION_SHADOW);
        d.decision = AppConfig.DECISION_SHADOW;
        d.action = AppConfig.DECISION_SHADOW;
        d.blockCode = c.blockCode == null ? "" : c.blockCode;
        d.reasonSummary = reason == null ? c.reasonSummary : reason;
        d.reasonText = d.reasonSummary;
        d.statScope = AppConfig.STAT_SCOPE_SHADOW;
        d.dataOk = 1;
        d.settled = 0;
        d.result = AppConfig.RESULT_WAIT;
        return d;
    }

    public boolean isValidPk10Numbers(int[] numbers) {
        if (numbers == null || numbers.length != AppConfig.NUMBER_COUNT) {
            return false;
        }

        boolean[] seen = new boolean[AppConfig.NUMBER_COUNT + 1];

        for (int i = 0; i < numbers.length; i++) {
            int n = numbers[i];

            if (!AppConfig.isValidNumber(n)) {
                return false;
            }

            if (seen[n]) {
                return false;
            }

            seen[n] = true;
        }

        return true;
    }

    private void normalizeDrawBeforeCheck(Models.Draw draw) {
        if (draw == null) {
            return;
        }

        if (draw.issue != null) {
            draw.issue = draw.issue.trim();
        }
    }

    private boolean shouldReplaceDuplicateDraw(Models.Draw oldDraw, Models.Draw newDraw) {
        if (oldDraw == null) {
            return true;
        }

        if (newDraw == null) {
            return false;
        }

        int oldDirty = getDrawDirty(oldDraw);
        int newDirty = getDrawDirty(newDraw);

        if (oldDirty != newDirty) {
            return newDirty == 0;
        }

        long oldFetch = getDrawFetchTime(oldDraw);
        long newFetch = getDrawFetchTime(newDraw);

        if (newFetch > oldFetch) {
            return true;
        }

        if (newFetch < oldFetch) {
            return false;
        }

        return newDraw.openTime > oldDraw.openTime;
    }

    private int getDrawDirty(Models.Draw d) {
        if (d == null) {
            return 1;
        }

        try {
            return d.dirty;
        } catch (Throwable e) {
            return 0;
        }
    }

    private long getDrawFetchTime(Models.Draw d) {
        if (d == null) {
            return 0L;
        }

        try {
            return d.fetchTime;
        } catch (Throwable e) {
            return 0L;
        }
    }

    private void normalizeDecisionK3(Models.Decision d) {
        if (d == null) {
            return;
        }

        if (AppConfig.isValidK3Numbers(d.n1, d.n2, d.n3)) {
            d.k3Mask = Models.buildK3Mask(d.n1, d.n2, d.n3);
        }
    }

    private void normalizeCandidateK3(Models.ComboCandidate c) {
        if (c == null) {
            return;
        }

        if (AppConfig.isValidK3Numbers(c.n1, c.n2, c.n3)) {
            c.k3Mask = Models.buildK3Mask(c.n1, c.n2, c.n3);
        }
    }

    private boolean isRealTradeScope(String scope) {
        if (scope == null || scope.length() == 0) {
            return true;
        }

        return AppConfig.STAT_SCOPE_REAL_TRADE.equals(scope);
    }

    private String normalizeDecisionText(String value) {
        if (value == null) {
            return "";
        }

        String v = value.trim().toUpperCase(java.util.Locale.ROOT);

        if ("BET".equals(v) || "下注".equals(value) || "真实出手".equals(value)) {
            return AppConfig.DECISION_BET;
        }

        if ("SHADOW".equals(v) || "影子".equals(value) || "观察".equals(value)) {
            return AppConfig.DECISION_SHADOW;
        }

        if ("HOLD".equals(v) || "等待".equals(value) || "不出手".equals(value)) {
            return AppConfig.DECISION_HOLD;
        }

        return value.trim();
    }

    private int countIssueGaps(ArrayList<Models.Draw> clean) {
        if (clean == null || clean.size() < 2) {
            return 0;
        }

        int gaps = 0;

        for (int i = 1; i < clean.size(); i++) {
            int diff = issueStepDistance(clean.get(i - 1).issue, clean.get(i).issue);

            if (diff > 1 && diff < MAX_ISSUE_STEP_SAFE) {
                gaps += diff - 1;
            }
        }

        return gaps;
    }

    private boolean hasLargeTimeDisorder(ArrayList<Models.Draw> clean) {
        if (clean == null || clean.size() < 3) {
            return false;
        }

        long last = 0L;
        int disorder = 0;

        for (int i = 0; i < clean.size(); i++) {
            Models.Draw d = clean.get(i);

            if (d.openTime <= 0L) {
                continue;
            }

            if (last > 0L && d.openTime < last) {
                disorder++;
            }

            last = d.openTime;
        }

        return disorder > 1;
    }

    private int compareIssue(String issueA, String issueB) {
        if (issueA == null && issueB == null) {
            return 0;
        }

        if (issueA == null) {
            return -1;
        }

        if (issueB == null) {
            return 1;
        }

        String aText = issueA.trim();
        String bText = issueB.trim();

        if (aText.equals(bText)) {
            return 0;
        }

        IssuePart a = parseIssuePart(aText);
        IssuePart b = parseIssuePart(bText);

        if (a.valid && b.valid && a.prefix.equals(b.prefix)) {
            if (a.number == b.number) {
                return 0;
            }

            return a.number < b.number ? -1 : 1;
        }

        Long fullA = parseFullLong(aText);
        Long fullB = parseFullLong(bText);

        if (fullA != null && fullB != null) {
            if (fullA.longValue() == fullB.longValue()) {
                return 0;
            }

            return fullA.longValue() < fullB.longValue() ? -1 : 1;
        }

        return aText.compareTo(bText);
    }

    private IssuePart parseIssuePart(String issue) {
        IssuePart out = new IssuePart();

        if (issue == null) {
            return out;
        }

        String s = issue.trim();

        if (s.length() == 0) {
            return out;
        }

        int end = s.length() - 1;

        while (end >= 0) {
            char ch = s.charAt(end);

            if (ch < '0' || ch > '9') {
                break;
            }

            end--;
        }

        if (end == s.length() - 1) {
            out.valid = false;
            out.prefix = s;
            out.number = 0L;
            return out;
        }

        String prefix = s.substring(0, end + 1);
        String digits = s.substring(end + 1);

        try {
            out.valid = true;
            out.prefix = prefix;
            out.number = Long.parseLong(digits);
            out.digitLength = digits.length();
            return out;
        } catch (Throwable e) {
            out.valid = false;
            out.prefix = s;
            out.number = 0L;
            return out;
        }
    }

    private Long parseFullLong(String issue) {
        if (issue == null) {
            return null;
        }

        try {
            String s = issue.trim();

            if (s.length() == 0) {
                return null;
            }

            for (int i = 0; i < s.length(); i++) {
                char ch = s.charAt(i);

                if (ch < '0' || ch > '9') {
                    return null;
                }
            }

            return Long.valueOf(Long.parseLong(s));
        } catch (Throwable e) {
            return null;
        }
    }

    private String normalizeIssueKey(String issue) {
        if (issue == null) {
            return "";
        }

        return issue.trim();
    }

    private String buildSnapshotId(
            String baseIssue, String targetIssue, String hash, long createdAt) {
        String raw = safe(baseIssue) + "|" + safe(targetIssue) + "|" + safe(hash) + "|" + createdAt;
        return "PIT_" + shortHash(raw);
    }

    private String sha256(String text) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] bytes = md.digest((text == null ? "" : text).getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();

            for (int i = 0; i < bytes.length; i++) {
                int v = bytes[i] & 0xff;

                if (v < 16) {
                    sb.append('0');
                }

                sb.append(Integer.toHexString(v));
            }

            return sb.toString();
        } catch (Throwable e) {
            return shortHash(text);
        }
    }

    private String shortHash(String text) {
        if (text == null) {
            text = "";
        }

        int h = 17;

        for (int i = 0; i < text.length(); i++) {
            h = h * 31 + text.charAt(i);
        }

        String out = Integer.toHexString(h);

        if (out.length() < 8) {
            while (out.length() < 8) {
                out = "0" + out;
            }
        }

        return out;
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }

    private static final class IssuePart {
        boolean valid;
        String prefix;
        long number;
        int digitLength;

        IssuePart() {
            valid = false;
            prefix = "";
            number = 0L;
            digitLength = 0;
        }
    }

    public static final class HistoryQuality {
        public boolean ok;
        public String blockCode;
        public String message;
        public int totalInput;
        public int validCount;
        public int dirtyCount;
        public int duplicateIssueCount;
        public int issueGapCount;
        public int timeDisorder;
        public long intervalMs;
        public String firstIssue;
        public String latestIssue;

        public HistoryQuality() {
            ok = false;
            blockCode = AppConfig.BLOCK_DATA_ERROR;
            message = "";
            totalInput = 0;
            validCount = 0;
            dirtyCount = 0;
            duplicateIssueCount = 0;
            issueGapCount = 0;
            timeDisorder = 0;
            intervalMs = DEFAULT_ROUND_INTERVAL_MS;
            firstIssue = "";
            latestIssue = "";
        }

        public String toText() {
            StringBuilder sb = new StringBuilder();

            sb.append(ok ? "OK" : "BLOCK");
            sb.append("｜有效=");
            sb.append(validCount);
            sb.append("｜脏=");
            sb.append(dirtyCount);
            sb.append("｜重复=");
            sb.append(duplicateIssueCount);
            sb.append("｜缺口=");
            sb.append(issueGapCount);
            sb.append("｜首期=");
            sb.append(firstIssue);
            sb.append("｜最新=");
            sb.append(latestIssue);

            if (message != null && message.length() > 0) {
                sb.append("｜");
                sb.append(message);
            }

            return sb.toString();
        }
    }

    public static final class FutureLeakCheck {
        public boolean ok;
        public int code;
        public String message;
        public int visibleCount;
        public int futureLeakCount;
        public int duplicateCount;
        public int dirtyCount;
        public ArrayList<String> futureIssues;

        public FutureLeakCheck() {
            ok = false;
            code = FUTURE_LEAK_CODE_EMPTY;
            message = "";
            visibleCount = 0;
            futureLeakCount = 0;
            duplicateCount = 0;
            dirtyCount = 0;
            futureIssues = new ArrayList<String>();
        }

        public String toText() {
            StringBuilder sb = new StringBuilder();

            sb.append(ok ? "NO_FUTURE_LEAK" : "FUTURE_LEAK_BLOCK");
            sb.append("｜visible=");
            sb.append(visibleCount);
            sb.append("｜future=");
            sb.append(futureLeakCount);
            sb.append("｜duplicate=");
            sb.append(duplicateCount);
            sb.append("｜dirty=");
            sb.append(dirtyCount);

            if (message != null && message.length() > 0) {
                sb.append("｜");
                sb.append(message);
            }

            return sb.toString();
        }
    }

    public Models.Decision preserveAsShadowOnSaveBlock(
            Models.Decision original, Models.DataCheck check) {
        if (original == null) return holdDecision("", "", AppConfig.BLOCK_DATA_ERROR, "空决策");
        String code = check == null ? AppConfig.BLOCK_DATA_ERROR : safe(check.code);
        if (AppConfig.isNonOverridableFloorBlock(code)) {
            return holdDecision(
                    original.baseIssue,
                    original.targetIssue,
                    code,
                    check == null ? "不可恢复的数据/账本错误" : check.reason);
        }
        Models.Decision d = original;
        d.decision = AppConfig.DECISION_SHADOW;
        d.action = AppConfig.DECISION_SHADOW;
        d.statScope = AppConfig.STAT_SCOPE_SHADOW;
        d.isRealtimeReal = 0;
        d.realTrade = 0;
        d.blockCode = code;
        d.reasonSummary =
                (check == null ? "保存安全检查未通过" : safe(check.reason)) + "｜保留完整候选为SHADOW，不清零证据";
        d.reasonText = d.reasonSummary;
        return d;
    }

    /* STARCORE_TIANSHU_B02_MATERIALIZED_20260805 */
    private static final DrawIntegrityRecoveryEngine TIANSHU_B02_INTEGRITY =
            new DrawIntegrityRecoveryEngine();

    public Models.DataCheck checkBinanceDrawContract(Models.Draw draw, long nowMs) {
        DrawIntegrityRecoveryEngine.VerificationResult result =
                TIANSHU_B02_INTEGRITY.verifySingle(draw, AppConfig.LOTTERY_CODE, nowMs);
        if (!result.accepted) {
            Models.DataCheck blocked =
                    Models.DataCheck.block(
                            AppConfig.BLOCK_DATA_ERROR,
                            "B02开奖完整性阻断｜" + result.code + "｜" + result.publicMessage);
            blocked.currentIssue = result.issue;
            blocked.actualIssue = result.issue;
            blocked.targetIssue = result.targetIssue;
            return blocked;
        }
        Models.DataCheck pass = Models.DataCheck.pass();
        pass.currentIssue = result.issue;
        pass.actualIssue = result.issue;
        pass.targetIssue = result.targetIssue;
        pass.message = "B02开奖完整性通过｜hash=" + result.canonicalHash + "｜source=" + result.sourceLine;
        return pass;
    }

    public Models.DataCheck checkBinanceHistoryContract(List<Models.Draw> history, long nowMs) {
        DrawIntegrityRecoveryEngine.HistoryVerification result =
                TIANSHU_B02_INTEGRITY.verifyHistory(history, AppConfig.LOTTERY_CODE, nowMs);
        if (!result.accepted) {
            return Models.DataCheck.block(
                    AppConfig.BLOCK_DATA_ERROR,
                    "B02历史完整性阻断｜" + result.code + "｜" + result.publicMessage);
        }
        if (result.conflictCount > 0) {
            return Models.DataCheck.block(
                    AppConfig.BLOCK_DATA_ERROR, "B02同期开奖冲突隔离｜conflicts=" + result.conflictCount);
        }
        Models.DataCheck pass = Models.DataCheck.pass();
        pass.currentIssue = result.latestIssue;
        pass.actualIssue = result.latestIssue;
        pass.targetIssue = result.targetIssue;
        pass.message =
                "B02历史完整性通过｜verified="
                        + result.verifiedCount
                        + "｜duplicates="
                        + result.duplicateCount
                        + "｜gaps="
                        + result.gaps.size()
                        + "｜historyHash="
                        + result.historyHash;
        return pass;
    }

    public DrawIntegrityRecoveryEngine.HistoryVerification b02ReplayIntegrity(
            List<Models.Draw> history, long frozenNowMs) {
        return TIANSHU_B02_INTEGRITY.verifyHistory(history, AppConfig.LOTTERY_CODE, frozenNowMs);
    }

    public DrawIntegrityRecoveryEngine.RecoveryPlan b02BuildRecoveryPlan(
            List<Models.Draw> history, long frozenNowMs) {
        DrawIntegrityRecoveryEngine.HistoryVerification verified =
                b02ReplayIntegrity(history, frozenNowMs);
        return TIANSHU_B02_INTEGRITY.buildRecoveryPlan(verified, frozenNowMs);
    }

    public Models.DataCheck checkTargetIssueFreshness(
            String currentIssue,
            String targetIssue,
            long providerOpenTime,
            long receivedAt,
            long nowMs) {
        DrawIntegrityRecoveryEngine.TargetFreshness freshness =
                TIANSHU_B02_INTEGRITY.checkTargetFreshness(
                        currentIssue, targetIssue, providerOpenTime, receivedAt, nowMs);
        if (!freshness.fresh) {
            return Models.DataCheck.block(
                    AppConfig.BLOCK_DATA_ERROR,
                    "B02目标期时钟阻断｜" + freshness.code + "｜" + freshness.publicMessage);
        }
        Models.DataCheck pass = Models.DataCheck.pass();
        pass.currentIssue = currentIssue == null ? "" : currentIssue;
        pass.actualIssue = pass.currentIssue;
        pass.targetIssue = targetIssue == null ? "" : targetIssue;
        pass.message =
                "B02目标期时钟通过｜ageMs="
                        + freshness.ageMs
                        + "｜untilOpenMs="
                        + freshness.untilOpenMs
                        + "｜fingerprint="
                        + freshness.fingerprint;
        return pass;
    }

    public static String b02IntegrityOwner() {
        return DrawIntegrityRecoveryEngine.OWNER;
    }

    public static boolean b02IntegritySelfTest() {
        return DrawIntegrityRecoveryEngine.selfTest();
    }
}
