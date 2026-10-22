package com.ethk3.novaonepro;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public final class ManualDeepBacktestEngine {

    private static final int DEFAULT_MIN_TRAIN = 240;
    private static final int DEFAULT_MAX_TEST = 2000;
    private static final int DEFAULT_TOP_KEEP = 80;

    private static final double UNIT_COST = 3.0d;
    private static final double WIN_RETURN = 9.9d;
    private static final double WIN_PROFIT = WIN_RETURN - UNIT_COST;
    private static final double LOSE_PROFIT = -UNIT_COST;

    private static final double MIN_BACKTEST_FINAL_P = 0.360d;
    private static final double MIN_BACKTEST_EVIDENCE = 0.420d;
    private static final double MAX_BACKTEST_RISK = 0.780d;

    private final ProbabilityAuditEngine probabilityAuditEngine;
    private final PromotionGateEngine promotionGateEngine;
    private final EvidenceRankerEngine evidenceRankerEngine;
    private final AiEnsembleJudgeEngine aiEnsembleJudgeEngine;
    private final AttributionEngine attributionEngine;

    public ManualDeepBacktestEngine() {
        probabilityAuditEngine = new ProbabilityAuditEngine();
        promotionGateEngine = new PromotionGateEngine();
        evidenceRankerEngine = new EvidenceRankerEngine();
        aiEnsembleJudgeEngine = new AiEnsembleJudgeEngine();
        attributionEngine = new AttributionEngine();
    }

    public Models.ManualDeepBacktestReport run(
            List<Models.Draw> history, Models.ManualBacktestTask task) {
        ManualBacktestResult result = runInternal(history, task);
        return toReport(result);
    }

    public ManualBacktestResult runInternal(
            List<Models.Draw> history, Models.ManualBacktestTask task) {
        ManualBacktestResult result = new ManualBacktestResult();
        result.createdAt = System.currentTimeMillis();

        ArrayList<Models.Draw> clean = cleanHistory(history);

        result.totalHistory = clean.size();

        if (clean.size() < DEFAULT_MIN_TRAIN + 10) {
            result.status = "HISTORY_WEAK";
            result.summaryText = "手动深度回测：历史不足，至少需要" + (DEFAULT_MIN_TRAIN + 10) + "期";
            return result;
        }

        BacktestRequest req = buildRequest(task, clean.size());

        result.startIndex = req.startIndex;
        result.endIndex = req.endIndex;
        result.minTrain = req.minTrain;
        result.maxTest = req.maxTest;
        result.topKeep = req.topKeep;
        result.taskName = req.taskName;

        double equity = 0.0d;
        double peak = 0.0d;
        double maxDrawdown = 0.0d;

        int processed = 0;

        for (int i = req.startIndex; i <= req.endIndex; i++) {
            if (i <= 0 || i >= clean.size()) {
                continue;
            }

            if (processed >= req.maxTest) {
                break;
            }

            ArrayList<Models.Draw> known = slice(clean, 0, i);
            Models.Draw actual = clean.get(i);

            if (known.size() < req.minTrain || actual == null || !actual.isValid()) {
                result.holdCount++;
                result.holdReasonText = append(result.holdReasonText, "训练样本不足或开奖号无效");
                processed++;
                continue;
            }

            OneStepResult step = simulateOneStep(known, actual, req);

            result.testCount++;
            result.holdCount += step.hold == 1 ? 1 : 0;
            result.shadowCount += step.shadow == 1 ? 1 : 0;

            if (step.bet == 1) {
                result.betCount++;

                if (step.hit == 1) {
                    result.hitCount++;
                    result.winCount++;
                    equity += WIN_PROFIT;
                } else {
                    result.missCount++;
                    result.loseCount++;
                    equity += LOSE_PROFIT;
                }

                result.totalProfit = equity;

                if (equity > peak) {
                    peak = equity;
                }

                double dd = peak - equity;
                if (dd > maxDrawdown) {
                    maxDrawdown = dd;
                }

                result.maxDrawdown = maxDrawdown;

                if (step.attribution != null) {
                    if (step.hit == 1) {
                        result.hitReasonText =
                                append(result.hitReasonText, step.attribution.mainReasonCode);
                    } else {
                        result.missReasonText =
                                append(result.missReasonText, step.attribution.mainReasonCode);
                    }
                }
            } else {
                result.totalProfit = equity;

                if (step.holdReason != null && step.holdReason.length() > 0) {
                    result.holdReasonText = append(result.holdReasonText, step.holdReason);
                }
            }

            if (step.bestCandidate != null) {
                result.lastBestText = buildCandidateText(step.bestCandidate);
            }

            if (step.stepSummary != null && step.stepSummary.length() > 0) {
                result.lastStepSummary = step.stepSummary;
            }

            processed++;
        }

        finalizeResult(result);

        return result;
    }

    public OneStepResult simulateOneStep(
            List<Models.Draw> knownHistory, Models.Draw actualDraw, BacktestRequest req) {
        OneStepResult step = new OneStepResult();

        step.createdAt = System.currentTimeMillis();

        ArrayList<Models.Draw> known = cleanHistory(knownHistory);

        if (actualDraw == null || !actualDraw.isValid()) {
            step.hold = 1;
            step.holdReason = "实际开奖无效";
            step.stepSummary = "手动回测单期：开奖无效，HOLD";
            return step;
        }

        if (known.size() < req.minTrain) {
            step.hold = 1;
            step.holdReason = "训练样本不足";
            step.stepSummary = "手动回测单期：训练样本不足，HOLD";
            return step;
        }

        ArrayList<Models.ComboCandidate> baseCandidates = generateCandidates(known, req.topKeep);

        if (baseCandidates.size() == 0) {
            step.hold = 1;
            step.holdReason = "候选池为空";
            step.stepSummary = "手动回测单期：无候选，HOLD";
            return step;
        }

        ArrayList<Models.ComboCandidate> audited =
                probabilityAuditEngine.applyToCandidates(baseCandidates);
        ArrayList<Models.ComboCandidate> promoted = promotionGateEngine.applyToCandidates(audited);
        ArrayList<Models.ComboCandidate> ranked =
                evidenceRankerEngine.rankAndApply(known, promoted);
        ArrayList<Models.ComboCandidate> judged =
                aiEnsembleJudgeEngine.applyToCandidates(known, ranked);

        if (judged.size() == 0) {
            step.hold = 1;
            step.holdReason = "增强后候选为空";
            step.stepSummary = "手动回测单期：增强后无候选，HOLD";
            return step;
        }

        Models.ComboCandidate best = judged.get(0);
        step.bestCandidate = best.copy();

        Models.AiJudgeResult judge = aiEnsembleJudgeEngine.judge(known, judged);
        step.aiJudge = judge;

        String gateReason = finalBacktestGate(best, judge);

        if (gateReason.length() > 0) {
            step.hold = 1;
            step.shadow = 1;
            step.holdReason = gateReason;
            step.stepSummary = "手动回测单期：HOLD，原因=" + gateReason + "，候选=" + buildCandidateText(best);
            return step;
        }

        step.bet = 1;
        step.hold = 0;
        step.shadow = 0;

        int actualNumber = actualDraw.getNumberAtPosition(best.position);
        step.actualNumber = actualNumber;
        step.hit = Models.maskContains(best.k3Mask, actualNumber) ? 1 : 0;

        step.attribution = attributionEngine.analyzeCandidate(best, actualDraw);

        step.stepSummary =
                "手动回测单期：BET "
                        + buildCandidateText(best)
                        + "，实际="
                        + UiFormatter.number(actualNumber)
                        + "，结果="
                        + (step.hit == 1 ? "中" : "不中");

        return step;
    }

    public ArrayList<Models.ComboCandidate> generateCandidates(
            List<Models.Draw> history, int topKeep) {
        ArrayList<Models.ComboCandidate> out = new ArrayList<Models.ComboCandidate>();

        ArrayList<Models.Draw> clean = cleanHistory(history);

        if (clean.size() < 30) {
            return out;
        }

        if (topKeep <= 0) {
            topKeep = DEFAULT_TOP_KEEP;
        }

        String baseIssue = "";
        String targetIssue = "";

        if (clean.size() > 0) {
            Models.Draw latest = clean.get(clean.size() - 1);
            baseIssue = latest.issue == null ? "" : latest.issue;
            targetIssue = AppConfig.nextIssueText(baseIssue);
        }

        for (int position = 1; position <= AppConfig.POSITION_COUNT; position++) {
            ArrayList<NumberScore> numberScores = scoreNumbers(clean, position);

            if (numberScores.size() < AppConfig.FIXED_K) {
                continue;
            }

            int limit = Math.min(8, numberScores.size());

            for (int a = 0; a < limit - 2; a++) {
                for (int b = a + 1; b < limit - 1; b++) {
                    for (int c = b + 1; c < limit; c++) {
                        int n1 = numberScores.get(a).number;
                        int n2 = numberScores.get(b).number;
                        int n3 = numberScores.get(c).number;

                        Models.ComboCandidate candidate =
                                buildCandidate(
                                        clean,
                                        baseIssue,
                                        targetIssue,
                                        position,
                                        n1,
                                        n2,
                                        n3,
                                        numberScores.get(a).score,
                                        numberScores.get(b).score,
                                        numberScores.get(c).score);

                        if (candidate != null && candidate.isValidK3()) {
                            out.add(candidate);
                        }
                    }
                }
            }
        }

        sortCandidates(out);

        if (out.size() > topKeep) {
            ArrayList<Models.ComboCandidate> cut = new ArrayList<Models.ComboCandidate>();

            for (int i = 0; i < topKeep; i++) {
                cut.add(out.get(i));
            }

            return cut;
        }

        return out;
    }

    private Models.ComboCandidate buildCandidate(
            ArrayList<Models.Draw> history,
            String baseIssue,
            String targetIssue,
            int position,
            int n1,
            int n2,
            int n3,
            double s1,
            double s2,
            double s3) {
        if (!AppConfig.isValidPosition(position)) {
            return null;
        }

        if (!AppConfig.isValidK3Numbers(n1, n2, n3)) {
            return null;
        }

        Models.ComboCandidate c = new Models.ComboCandidate();

        c.baseIssue = baseIssue;
        c.targetIssue = targetIssue;
        c.position = position;
        c.n1 = n1;
        c.n2 = n2;
        c.n3 = n3;
        c.k3Mask = Models.buildK3Mask(n1, n2, n3);

        c.effectiveSample = history.size();
        c.comboSample = Math.min(history.size(), 500);

        double comboRate64 = comboRate(history, position, c.k3Mask, 64);
        double comboRate160 = comboRate(history, position, c.k3Mask, 160);
        double comboRate500 = comboRate(history, position, c.k3Mask, 500);

        double baseP = ((double) AppConfig.FIXED_K) / ((double) AppConfig.NUMBER_COUNT);

        double numberScore = (s1 + s2 + s3) / 3.0d;

        c.comboHitRate = comboRate160;
        c.similarHitRate = comboRate64;

        c.rawP =
                AppConfig.clampProbability(
                        baseP * 0.58d
                                + comboRate64 * 0.22d
                                + comboRate160 * 0.14d
                                + comboRate500 * 0.06d
                                + (numberScore - 50.0d) / 1000.0d);

        c.calibratedP = AppConfig.clampProbability(c.rawP * 0.94d + comboRate160 * 0.06d);
        c.finalP = AppConfig.clampProbability(c.calibratedP);

        c.dynamicGate = dynamicGate(history, position, comboRate64, comboRate160);
        c.edge = c.finalP - c.dynamicGate;

        c.modelP = c.rawP;
        c.shapeP = comboRate64;
        c.comboP = comboRate160;
        c.recoveryP = comboRate500;
        c.similarP = comboRate64;
        c.memoryP = comboRate160;

        c.trendLife = trendLife(history, position, c.k3Mask);
        c.driftRisk = driftRisk(history, position);
        c.breakDepth = breakDepth(history, position, c.k3Mask);
        c.recoveryScore = recoveryScore(history, position, c.k3Mask);
        c.championGap = championGap(s1, s2, s3);
        c.comboGap = comboGap(comboRate64, comboRate160, baseP);
        c.hotReverseRisk = hotReverseRisk(history, position, c.k3Mask);
        c.falseStrongRisk = falseStrongRisk(c);

        c.calibrationError = Math.abs(c.rawP - c.calibratedP);

        c.deepNumberScore = AppConfig.clampScore01(numberScore / 100.0d);
        c.deepPositionScore = AppConfig.clampScore01(c.trendLife / 100.0d);
        c.deepScore =
                AppConfig.clampScore01(
                        (numberScore * 0.40d
                                        + c.trendLife * 0.24d
                                        + c.recoveryScore * 0.18d
                                        + Math.max(0.0d, 100.0d - c.driftRisk) * 0.18d)
                                / 100.0d);

        c.deepConfidence = AppConfig.clampScore01(sampleScore(c.effectiveSample) / 100.0d);
        c.evidenceScore = c.deepScore;
        c.evidenceRisk =
                AppConfig.clampScore01(
                        (c.driftRisk * 0.44d + c.breakDepth * 0.34d + c.falseStrongRisk * 0.22d)
                                / 100.0d);

        c.stateMode = stateMode(c);
        c.blockCode = "";
        c.reasonSummary =
                "手动回测候选生成："
                        + UiFormatter.positionAndNumbers(position, n1, n2, n3)
                        + "，rawP="
                        + UiFormatter.probability(c.rawP)
                        + "，finalP="
                        + UiFormatter.probability(c.finalP)
                        + "，gate="
                        + UiFormatter.probability(c.dynamicGate)
                        + "，edge="
                        + UiFormatter.signedProbability(c.edge);

        return c;
    }

    private ArrayList<NumberScore> scoreNumbers(ArrayList<Models.Draw> history, int position) {
        ArrayList<NumberScore> list = new ArrayList<NumberScore>();

        if (history == null || history.size() == 0 || !AppConfig.isValidPosition(position)) {
            return list;
        }

        for (int n = AppConfig.MIN_NUMBER; n <= AppConfig.MAX_NUMBER; n++) {
            NumberScore s = new NumberScore();
            s.position = position;
            s.number = n;

            double f32 = numberRate(history, position, n, 32);
            double f64 = numberRate(history, position, n, 64);
            double f160 = numberRate(history, position, n, 160);
            double f500 = numberRate(history, position, n, 500);

            int gap = lastGap(history, position, n);
            double gapScore = gapScore(gap);

            s.score =
                    AppConfig.clampScore100(
                            f32 * 100.0d * 0.28d
                                    + f64 * 100.0d * 0.24d
                                    + f160 * 100.0d * 0.18d
                                    + f500 * 100.0d * 0.10d
                                    + gapScore * 0.20d);

            s.recentRate = f32;
            s.midRate = f160;
            s.longRate = f500;
            s.gap = gap;

            list.add(s);
        }

        Collections.sort(
                list,
                new Comparator<NumberScore>() {
                    @Override
                    public int compare(NumberScore a, NumberScore b) {
                        double sa = a == null ? -999.0d : a.score;
                        double sb = b == null ? -999.0d : b.score;

                        if (sa < sb) return 1;
                        if (sa > sb) return -1;

                        int na = a == null ? 999 : a.number;
                        int nb = b == null ? 999 : b.number;

                        return na - nb;
                    }
                });

        return list;
    }

    private String finalBacktestGate(Models.ComboCandidate c, Models.AiJudgeResult judge) {
        if (c == null) {
            return "候选为空";
        }

        if (!c.isValidK3()) {
            return "候选不是固定K3";
        }

        if (c.finalP < MIN_BACKTEST_FINAL_P) {
            return "FinalP低于回测最低线";
        }

        if (c.finalP < AppConfig.MIN_REAL_BET_PROBABILITY) {
            return "FinalP低于真实出手最低线";
        }

        if (c.finalP < c.dynamicGate) {
            return "FinalP未过动态门槛";
        }

        if (c.evidenceScore < MIN_BACKTEST_EVIDENCE) {
            return "证据分不足";
        }

        if (c.evidenceRisk > MAX_BACKTEST_RISK) {
            return "证据风险过高";
        }

        if (c.auditPenalty >= 0.320d && c.finalP < 0.430d) {
            return "概率审计惩罚过高";
        }

        if (c.driftRisk >= AppConfig.DRIFT_RISK_HARD_BLOCK) {
            return "漂移硬风险";
        }

        if (c.breakDepth >= AppConfig.BREAK_DEPTH_HARD_BLOCK) {
            return "破位硬风险";
        }

        if (c.effectiveSample < AppConfig.EFFECTIVE_SAMPLE_MIN) {
            return "有效样本不足";
        }

        if (c.comboSample < AppConfig.COMBO_SAMPLE_MIN) {
            return "组合样本不足";
        }

        if (judge != null) {
            if (AppConfig.AI_JUDGE_HOLD.equals(judge.betAdvice)
                    && judge.holdReason != null
                    && judge.holdReason.length() > 0
                    && c.finalP < 0.430d) {
                return "AI裁决HOLD：" + judge.holdReason;
            }

            if (judge.conflictLevel > AppConfig.AI_JUDGE_CONFLICT_MAX && c.finalP < 0.430d) {
                return "AI冲突偏高";
            }
        }

        return "";
    }

    private void finalizeResult(ManualBacktestResult r) {
        if (r == null) {
            return;
        }

        if (r.testCount <= 0) {
            r.status = "EMPTY";
            r.summaryText = "手动深度回测：没有完成任何测试期";
            return;
        }

        r.betRate = ((double) r.betCount) / ((double) r.testCount);
        r.holdRate = ((double) r.holdCount) / ((double) r.testCount);
        r.shadowRate = ((double) r.shadowCount) / ((double) r.testCount);

        if (r.betCount > 0) {
            r.hitRate = ((double) r.hitCount) / ((double) r.betCount);
            r.avgProfitPerBet = r.totalProfit / ((double) r.betCount);
        } else {
            r.hitRate = 0.0d;
            r.avgProfitPerBet = 0.0d;
        }

        r.expectedBreakEvenHitRate = UNIT_COST / WIN_RETURN;

        if (r.betCount <= 0) {
            r.status = "NO_BET";
        } else if (r.hitRate >= 0.400d
                && r.betRate >= 0.080d
                && r.betRate <= 0.180d
                && r.totalProfit > 0.0d) {
            r.status = "GOOD";
        } else if (r.hitRate >= 0.370d && r.betRate >= 0.060d && r.totalProfit >= 0.0d) {
            r.status = "USABLE";
        } else if (r.hitRate >= r.expectedBreakEvenHitRate && r.totalProfit >= 0.0d) {
            r.status = "WEAK_USABLE";
        } else {
            r.status = "WEAK";
        }

        r.summaryText = buildResultSummary(r);
    }

    private Models.ManualDeepBacktestReport toReport(ManualBacktestResult r) {
        Models.ManualDeepBacktestReport report = new Models.ManualDeepBacktestReport();

        if (r == null) {
            return report;
        }

        set(report, "taskName", r.taskName);
        set(report, "createdAt", r.createdAt);
        set(report, "startIndex", r.startIndex);
        set(report, "endIndex", r.endIndex);
        set(report, "minTrain", r.minTrain);
        set(report, "maxTest", r.maxTest);
        set(report, "totalHistory", r.totalHistory);
        set(report, "testCount", r.testCount);
        set(report, "betCount", r.betCount);
        set(report, "holdCount", r.holdCount);
        set(report, "shadowCount", r.shadowCount);
        set(report, "hitCount", r.hitCount);
        set(report, "missCount", r.missCount);
        set(report, "winCount", r.winCount);
        set(report, "loseCount", r.loseCount);
        set(report, "hitRate", r.hitRate);
        set(report, "betRate", r.betRate);
        set(report, "holdRate", r.holdRate);
        set(report, "shadowRate", r.shadowRate);
        set(report, "totalProfit", r.totalProfit);
        set(report, "maxDrawdown", r.maxDrawdown);
        set(report, "avgProfitPerBet", r.avgProfitPerBet);
        set(report, "expectedBreakEvenHitRate", r.expectedBreakEvenHitRate);
        set(report, "status", r.status);
        set(report, "summaryText", r.summaryText);
        set(report, "holdReasonText", r.holdReasonText);
        set(report, "hitReasonText", r.hitReasonText);
        set(report, "missReasonText", r.missReasonText);
        set(report, "lastBestText", r.lastBestText);
        set(report, "lastStepSummary", r.lastStepSummary);

        return report;
    }

    private BacktestRequest buildRequest(Models.ManualBacktestTask task, int size) {
        BacktestRequest r = new BacktestRequest();

        r.taskName = stringField(task, "taskName", "手动深度回测");
        r.minTrain = intField(task, "minTrain", DEFAULT_MIN_TRAIN);
        r.maxTest = intField(task, "maxTest", DEFAULT_MAX_TEST);
        r.topKeep = intField(task, "topKeep", DEFAULT_TOP_KEEP);

        int start = intField(task, "startIndex", r.minTrain);
        int end = intField(task, "endIndex", size - 1);

        if (start < r.minTrain) {
            start = r.minTrain;
        }

        if (start < 1) {
            start = 1;
        }

        if (end >= size) {
            end = size - 1;
        }

        if (end < start) {
            end = start;
        }

        if (r.maxTest <= 0) {
            r.maxTest = DEFAULT_MAX_TEST;
        }

        if (r.topKeep <= 0) {
            r.topKeep = DEFAULT_TOP_KEEP;
        }

        r.startIndex = start;
        r.endIndex = end;

        return r;
    }

    private double numberRate(
            ArrayList<Models.Draw> history, int position, int number, int window) {
        if (history == null
                || history.size() == 0
                || !AppConfig.isValidPosition(position)
                || !AppConfig.isValidNumber(number)) {
            return 0.0d;
        }

        int start = history.size() - window;
        if (start < 0) {
            start = 0;
        }

        int total = 0;
        int hit = 0;

        for (int i = start; i < history.size(); i++) {
            Models.Draw d = history.get(i);

            if (d == null || !d.isValid()) {
                continue;
            }

            int n = d.getNumberAtPosition(position);

            if (AppConfig.isValidNumber(n)) {
                total++;

                if (n == number) {
                    hit++;
                }
            }
        }

        if (total <= 0) {
            return 0.0d;
        }

        return ((double) hit) / ((double) total);
    }

    private double comboRate(ArrayList<Models.Draw> history, int position, int mask, int window) {
        if (history == null
                || history.size() == 0
                || !AppConfig.isValidPosition(position)
                || !Models.isValidK3Mask(mask)) {
            return 0.0d;
        }

        int start = history.size() - window;
        if (start < 0) {
            start = 0;
        }

        int total = 0;
        int hit = 0;

        for (int i = start; i < history.size(); i++) {
            Models.Draw d = history.get(i);

            if (d == null || !d.isValid()) {
                continue;
            }

            int n = d.getNumberAtPosition(position);

            if (AppConfig.isValidNumber(n)) {
                total++;

                if (Models.maskContains(mask, n)) {
                    hit++;
                }
            }
        }

        if (total <= 0) {
            return 0.0d;
        }

        return ((double) hit) / ((double) total);
    }

    private int lastGap(ArrayList<Models.Draw> history, int position, int number) {
        if (history == null || history.size() == 0) {
            return 999;
        }

        int gap = 0;

        for (int i = history.size() - 1; i >= 0; i--) {
            Models.Draw d = history.get(i);

            if (d == null || !d.isValid()) {
                continue;
            }

            int n = d.getNumberAtPosition(position);

            if (n == number) {
                return gap;
            }

            gap++;
        }

        return gap;
    }

    private double gapScore(int gap) {
        if (gap <= 0) {
            return 48.0d;
        }

        if (gap <= 3) {
            return 56.0d;
        }

        if (gap <= 8) {
            return 66.0d;
        }

        if (gap <= 14) {
            return 62.0d;
        }

        if (gap <= 24) {
            return 52.0d;
        }

        return 42.0d;
    }

    private double dynamicGate(
            ArrayList<Models.Draw> history, int position, double comboRate64, double comboRate160) {
        double gate = 0.370d;

        if (history.size() < 500) {
            gate += 0.010d;
        }

        if (comboRate64 < 0.280d) {
            gate += 0.008d;
        }

        if (comboRate160 < 0.300d) {
            gate += 0.006d;
        }

        double drift = driftRisk(history, position);
        if (drift > AppConfig.DRIFT_RISK_MAX) {
            gate += 0.010d;
        }

        if (gate < 0.350d) {
            gate = 0.350d;
        }

        if (gate > 0.430d) {
            gate = 0.430d;
        }

        return gate;
    }

    private double trendLife(ArrayList<Models.Draw> history, int position, int mask) {
        double r32 = comboRate(history, position, mask, 32);
        double r64 = comboRate(history, position, mask, 64);
        double r160 = comboRate(history, position, mask, 160);

        double score = 50.0d;
        score += (r32 - 0.300d) * 120.0d;
        score += (r64 - 0.300d) * 80.0d;
        score += (r160 - 0.300d) * 40.0d;

        if (r32 >= r64 && r64 >= r160) {
            score += 5.0d;
        }

        return AppConfig.clampScore100(score);
    }

    private double driftRisk(ArrayList<Models.Draw> history, int position) {
        double[] f32 = frequency(history, position, 32);
        double[] f160 = frequency(history, position, 160);

        double sum = 0.0d;

        for (int n = AppConfig.MIN_NUMBER; n <= AppConfig.MAX_NUMBER; n++) {
            sum += Math.abs(f32[n] - f160[n]);
        }

        return AppConfig.clampScore100(sum * 100.0d);
    }

    private double breakDepth(ArrayList<Models.Draw> history, int position, int mask) {
        double r16 = comboRate(history, position, mask, 16);
        double r64 = comboRate(history, position, mask, 64);
        double r160 = comboRate(history, position, mask, 160);

        double risk = 50.0d;

        if (r16 < r64) {
            risk += (r64 - r16) * 120.0d;
        }

        if (r64 < r160) {
            risk += (r160 - r64) * 60.0d;
        }

        if (r16 >= r64 && r64 >= r160) {
            risk -= 10.0d;
        }

        return AppConfig.clampScore100(risk);
    }

    private double recoveryScore(ArrayList<Models.Draw> history, int position, int mask) {
        double r16 = comboRate(history, position, mask, 16);
        double r64 = comboRate(history, position, mask, 64);
        double r160 = comboRate(history, position, mask, 160);

        double score = 50.0d;
        score += (r16 - r64) * 90.0d;
        score += (r64 - r160) * 50.0d;

        if (r16 >= 0.300d && r64 >= 0.300d) {
            score += 6.0d;
        }

        return AppConfig.clampScore100(score);
    }

    private double championGap(double s1, double s2, double s3) {
        double max = Math.max(s1, Math.max(s2, s3));
        double min = Math.min(s1, Math.min(s2, s3));

        return AppConfig.clampScore01((max - min + 12.0d) / 100.0d);
    }

    private double comboGap(double r64, double r160, double baseP) {
        double edge = ((r64 * 0.58d + r160 * 0.42d) - baseP);
        return AppConfig.clampScore01((edge + 0.100d) / 0.300d);
    }

    private double hotReverseRisk(ArrayList<Models.Draw> history, int position, int mask) {
        double r8 = comboRate(history, position, mask, 8);
        double r32 = comboRate(history, position, mask, 32);

        double risk = 0.0d;

        if (r8 >= 0.620d) {
            risk += 34.0d;
        }

        if (r8 > r32 + 0.220d) {
            risk += 22.0d;
        }

        return AppConfig.clampScore100(risk);
    }

    private double falseStrongRisk(Models.ComboCandidate c) {
        if (c == null) {
            return 100.0d;
        }

        double risk = 0.0d;

        if (c.finalP >= 0.410d && c.evidenceScore < 0.500d) {
            risk += 24.0d;
        }

        if (c.driftRisk > AppConfig.DRIFT_RISK_MAX) {
            risk += 18.0d;
        }

        if (c.breakDepth > AppConfig.BREAK_DEPTH_MAX) {
            risk += 18.0d;
        }

        if (c.effectiveSample < AppConfig.EFFECTIVE_SAMPLE_MIN) {
            risk += 12.0d;
        }

        return AppConfig.clampScore100(risk);
    }

    private String stateMode(Models.ComboCandidate c) {
        if (c == null) {
            return AppConfig.MODE_CHAOS;
        }

        if (c.driftRisk >= AppConfig.DRIFT_RISK_HARD_BLOCK) {
            return AppConfig.MODE_REVERSING;
        }

        if (c.breakDepth >= AppConfig.BREAK_DEPTH_HARD_BLOCK) {
            return AppConfig.MODE_BROKEN;
        }

        if (c.trendLife >= 68.0d && c.recoveryScore >= 52.0d) {
            return AppConfig.MODE_CONFIRMED;
        }

        if (c.trendLife >= 62.0d) {
            return AppConfig.MODE_FORMING;
        }

        if (c.recoveryScore >= 58.0d) {
            return AppConfig.MODE_RECOVERING;
        }

        return AppConfig.MODE_WEAKENING;
    }

    private double[] frequency(ArrayList<Models.Draw> history, int position, int window) {
        double[] out = new double[AppConfig.NUMBER_COUNT + 1];

        if (history == null || history.size() == 0 || !AppConfig.isValidPosition(position)) {
            return out;
        }

        int start = history.size() - window;
        if (start < 0) {
            start = 0;
        }

        int total = 0;

        for (int i = start; i < history.size(); i++) {
            Models.Draw d = history.get(i);

            if (d == null || !d.isValid()) {
                continue;
            }

            int n = d.getNumberAtPosition(position);

            if (AppConfig.isValidNumber(n)) {
                out[n] += 1.0d;
                total++;
            }
        }

        if (total <= 0) {
            return out;
        }

        for (int n = AppConfig.MIN_NUMBER; n <= AppConfig.MAX_NUMBER; n++) {
            out[n] = out[n] / ((double) total);
        }

        return out;
    }

    private double sampleScore(int sample) {
        if (sample <= 0) {
            return 0.0d;
        }

        if (sample >= AppConfig.EFFECTIVE_SAMPLE_HIGH) {
            return 100.0d;
        }

        return AppConfig.clampScore100(
                ((double) sample) / ((double) AppConfig.EFFECTIVE_SAMPLE_HIGH) * 100.0d);
    }

    private ArrayList<Models.Draw> cleanHistory(List<Models.Draw> history) {
        ArrayList<Models.Draw> out = new ArrayList<Models.Draw>();

        if (history == null || history.size() == 0) {
            return out;
        }

        for (int i = 0; i < history.size(); i++) {
            Models.Draw d = history.get(i);

            if (d != null && d.isValid()) {
                out.add(d);
            }
        }

        return out;
    }

    private ArrayList<Models.Draw> slice(ArrayList<Models.Draw> list, int start, int endExclusive) {
        ArrayList<Models.Draw> out = new ArrayList<Models.Draw>();

        if (list == null || list.size() == 0) {
            return out;
        }

        if (start < 0) {
            start = 0;
        }

        if (endExclusive > list.size()) {
            endExclusive = list.size();
        }

        for (int i = start; i < endExclusive; i++) {
            Models.Draw d = list.get(i);

            if (d != null && d.isValid()) {
                out.add(d);
            }
        }

        return out;
    }

    private void sortCandidates(List<Models.ComboCandidate> list) {
        if (list == null || list.size() <= 1) {
            return;
        }

        Collections.sort(
                list,
                new Comparator<Models.ComboCandidate>() {
                    @Override
                    public int compare(Models.ComboCandidate a, Models.ComboCandidate b) {
                        double sa = a == null ? -999.0d : candidateSortScore(a);
                        double sb = b == null ? -999.0d : candidateSortScore(b);

                        if (sa < sb) return 1;
                        if (sa > sb) return -1;

                        int pa = a == null ? 999 : a.position;
                        int pb = b == null ? 999 : b.position;

                        return pa - pb;
                    }
                });

        for (int i = 0; i < list.size(); i++) {
            Models.ComboCandidate c = list.get(i);

            if (c != null) {
                c.rank = i + 1;
            }
        }
    }

    private double candidateSortScore(Models.ComboCandidate c) {
        if (c == null) {
            return -999.0d;
        }

        double score = c.finalP * 100.0d;
        score += Math.max(0.0d, c.edge) * 100.0d * 0.20d;
        score += c.deepScore * 100.0d * 0.14d;
        score += c.evidenceScore * 100.0d * 0.12d;
        score -= c.evidenceRisk * 100.0d * 0.10d;
        score -= c.driftRisk * 0.04d;
        score -= c.breakDepth * 0.04d;

        return score;
    }

    private String buildCandidateText(Models.ComboCandidate c) {
        if (c == null) {
            return "候选为空";
        }

        return UiFormatter.positionAndNumbers(c.position, c.n1, c.n2, c.n3)
                + " FinalP="
                + UiFormatter.probability(c.finalP)
                + " Gate="
                + UiFormatter.probability(c.dynamicGate)
                + " Evidence="
                + UiFormatter.probability(c.evidenceScore)
                + " Risk="
                + UiFormatter.probability(c.evidenceRisk);
    }

    private String buildResultSummary(ManualBacktestResult r) {
        if (r == null) {
            return "手动深度回测：暂无";
        }

        StringBuilder sb = new StringBuilder();

        sb.append("手动深度回测：状态=");
        sb.append(r.status);

        sb.append("，测试=");
        sb.append(r.testCount);

        sb.append("，BET=");
        sb.append(r.betCount);

        sb.append("，HOLD=");
        sb.append(r.holdCount);

        sb.append("，SHADOW=");
        sb.append(r.shadowCount);

        sb.append("，频率=");
        sb.append(UiFormatter.probability(r.betRate));

        sb.append("，胜率=");
        sb.append(UiFormatter.probability(r.hitRate));

        sb.append("，盈亏=");
        sb.append(UiFormatter.score(r.totalProfit));

        sb.append("，最大回撤=");
        sb.append(UiFormatter.score(r.maxDrawdown));

        sb.append("，单注均值=");
        sb.append(UiFormatter.score(r.avgProfitPerBet));

        sb.append("，保本胜率=");
        sb.append(UiFormatter.probability(r.expectedBreakEvenHitRate));

        if (r.lastBestText != null && r.lastBestText.length() > 0) {
            sb.append("，最后候选=");
            sb.append(r.lastBestText);
        }

        return sb.toString();
    }

    private void set(Object target, String fieldName, Object value) {
        if (target == null || fieldName == null || fieldName.length() == 0) {
            return;
        }

        try {
            Field f = target.getClass().getField(fieldName);
            Class<?> type = f.getType();

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

    private int intField(Object target, String fieldName, int def) {
        if (target == null || fieldName == null || fieldName.length() == 0) {
            return def;
        }

        try {
            Field f = target.getClass().getField(fieldName);
            Object v = f.get(target);

            if (v instanceof Number) {
                return ((Number) v).intValue();
            }
        } catch (Throwable ignore) {
        }

        return def;
    }

    private String stringField(Object target, String fieldName, String def) {
        if (target == null || fieldName == null || fieldName.length() == 0) {
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

    public static final class BacktestRequest {
        public String taskName;
        public int startIndex;
        public int endIndex;
        public int minTrain;
        public int maxTest;
        public int topKeep;

        public BacktestRequest() {
            taskName = "";
            startIndex = 0;
            endIndex = 0;
            minTrain = DEFAULT_MIN_TRAIN;
            maxTest = DEFAULT_MAX_TEST;
            topKeep = DEFAULT_TOP_KEEP;
        }
    }

    private static final class NumberScore {
        public int position;
        public int number;
        public double score;
        public double recentRate;
        public double midRate;
        public double longRate;
        public int gap;

        public NumberScore() {
            position = 0;
            number = 0;
            score = 0.0d;
            recentRate = 0.0d;
            midRate = 0.0d;
            longRate = 0.0d;
            gap = 0;
        }
    }

    public static final class OneStepResult {
        public long createdAt;
        public int bet;
        public int hold;
        public int shadow;
        public int hit;
        public int actualNumber;
        public String holdReason;
        public String stepSummary;
        public Models.ComboCandidate bestCandidate;
        public Models.AiJudgeResult aiJudge;
        public Models.AttributionResult attribution;

        public OneStepResult() {
            createdAt = 0L;
            bet = 0;
            hold = 0;
            shadow = 0;
            hit = 0;
            actualNumber = 0;
            holdReason = "";
            stepSummary = "";
            bestCandidate = null;
            aiJudge = null;
            attribution = null;
        }
    }

    public static final class ManualBacktestResult {
        public String taskName;
        public long createdAt;

        public int totalHistory;
        public int startIndex;
        public int endIndex;
        public int minTrain;
        public int maxTest;
        public int topKeep;

        public int testCount;
        public int betCount;
        public int holdCount;
        public int shadowCount;
        public int hitCount;
        public int missCount;
        public int winCount;
        public int loseCount;

        public double hitRate;
        public double betRate;
        public double holdRate;
        public double shadowRate;

        public double totalProfit;
        public double maxDrawdown;
        public double avgProfitPerBet;
        public double expectedBreakEvenHitRate;

        public String status;
        public String summaryText;
        public String holdReasonText;
        public String hitReasonText;
        public String missReasonText;
        public String lastBestText;
        public String lastStepSummary;

        public ManualBacktestResult() {
            taskName = "";
            createdAt = 0L;

            totalHistory = 0;
            startIndex = 0;
            endIndex = 0;
            minTrain = 0;
            maxTest = 0;
            topKeep = 0;

            testCount = 0;
            betCount = 0;
            holdCount = 0;
            shadowCount = 0;
            hitCount = 0;
            missCount = 0;
            winCount = 0;
            loseCount = 0;

            hitRate = 0.0d;
            betRate = 0.0d;
            holdRate = 0.0d;
            shadowRate = 0.0d;

            totalProfit = 0.0d;
            maxDrawdown = 0.0d;
            avgProfitPerBet = 0.0d;
            expectedBreakEvenHitRate = 0.0d;

            status = "";
            summaryText = "";
            holdReasonText = "";
            hitReasonText = "";
            missReasonText = "";
            lastBestText = "";
            lastStepSummary = "";
        }
    }
}
