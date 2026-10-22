package com.ethk3.novaonepro;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * PointInTimeSnapshotEngine.java
 *
 * <p>时间点快照引擎。
 *
 * <p>核心目的： 1. 手动全面深度回测时，每一步只能看到当时之前的数据。 2. 防止未来开奖、未来画像、未来校准、未来结算结果混入训练窗口。 3. 为每一次模拟决策生成可追踪、可校验的
 * point-in-time 快照。 4. 用 dataHash 保证快照内容可复核。
 *
 * <p>注意： - Java 7 / Android API 21+ / 无第三方库。 - 本文件不下注、不结算、不写真实盈亏。 - 真实统计仍然只由 trades + settled=1 +
 * BET + REAL_TRADE 决定。
 */
public final class PointInTimeSnapshotEngine {

    public static final int DEFAULT_MAX_VISIBLE_DRAWS = 2880;
    public static final int MIN_REASONABLE_VISIBLE_DRAWS = 30;
    public static final int COMPACT_ISSUE_LIMIT = 18;
    public static final int TAIL_BRIEF_LIMIT = 8;

    public static final String STATUS_OK = "OK";
    public static final String STATUS_WARNING = "WARNING";
    public static final String STATUS_ERROR = "ERROR";

    private static final int HASH_TEXT_LIMIT = 512;
    private static final int STATE_TEXT_LIMIT = 900;

    public PointInTimeSnapshotEngine() {}

    /** 原始入口：通过 SnapshotInput 构建时间点快照。 */
    public PointInTimeSnapshot buildSnapshotBeforeDecision(SnapshotInput input) {
        if (input == null) {
            return PointInTimeSnapshot.error("input 为空，无法构建时间点快照");
        }

        String baseIssue = safe(input.baseIssue);
        String targetIssue = safe(input.targetIssue);

        if (baseIssue.length() == 0) {
            return PointInTimeSnapshot.error("baseIssue 为空，无法构建时间点快照");
        }

        if (targetIssue.length() == 0) {
            return PointInTimeSnapshot.error("targetIssue 为空，无法构建时间点快照");
        }

        if (compareIssue(targetIssue, baseIssue) <= 0) {
            return PointInTimeSnapshot.error("targetIssue 必须大于 baseIssue，禁止同期开奖或倒序回测");
        }

        int maxWindow = input.maxVisibleDraws;
        if (maxWindow <= 0) {
            maxWindow = DEFAULT_MAX_VISIBLE_DRAWS;
        }

        List<DrawRecord> visible = buildVisibleDrawWindow(input.allDraws, baseIssue, maxWindow);

        String trainStartIssue = "";
        String trainEndIssue = "";

        if (visible.size() > 0) {
            trainStartIssue = visible.get(0).issue;
            trainEndIssue = visible.get(visible.size() - 1).issue;
        }

        String featureSummary = safe(input.featureSummary);
        String candidateSummary = safe(input.candidateSummary);
        String gateSummary = safe(input.gateSummary);
        String decisionSummary = safe(input.decisionSummary);

        String dataHash =
                buildDataHash(
                        baseIssue,
                        targetIssue,
                        visible,
                        input.visibleTradeCount,
                        input.visibleProfileVersion,
                        input.visibleCalibrationVersion,
                        featureSummary,
                        candidateSummary,
                        gateSummary,
                        decisionSummary);

        PointInTimeSnapshot snapshot = new PointInTimeSnapshot();

        snapshot.snapshotId = buildSnapshotId(baseIssue, targetIssue, dataHash);
        snapshot.baseIssue = baseIssue;
        snapshot.targetIssue = targetIssue;
        snapshot.trainStartIssue = trainStartIssue;
        snapshot.trainEndIssue = trainEndIssue;
        snapshot.visibleDrawCount = visible.size();
        snapshot.visibleTradeCount = Math.max(0, input.visibleTradeCount);
        snapshot.visibleProfileVersion = Math.max(0L, input.visibleProfileVersion);
        snapshot.visibleCalibrationVersion = Math.max(0L, input.visibleCalibrationVersion);
        snapshot.featureSummary = featureSummary;
        snapshot.candidateSummary = candidateSummary;
        snapshot.gateSummary = gateSummary;
        snapshot.decisionSummary = decisionSummary;
        snapshot.dataHash = dataHash;
        snapshot.visibleIssueListCompact = buildCompactIssueList(visible, COMPACT_ISSUE_LIMIT);
        snapshot.visibleTailBrief = buildVisibleTailBrief(visible, TAIL_BRIEF_LIMIT);
        snapshot.createdAtMs = input.nowMs > 0L ? input.nowMs : System.currentTimeMillis();

        LeakCheckResult check = verifyNoFutureLeak(snapshot, visible, input.targetDraw);

        snapshot.noFutureLeakOk = check.ok;
        snapshot.warningText = check.message;
        snapshot.status = check.ok ? STATUS_OK : STATUS_ERROR;

        if (check.warning) {
            snapshot.status = STATUS_WARNING;
        }

        if (visible.size() < MIN_REASONABLE_VISIBLE_DRAWS) {
            snapshot.status = STATUS_WARNING;
            snapshot.warningText =
                    appendText(
                            snapshot.warningText,
                            "可见开奖数量不足 " + MIN_REASONABLE_VISIBLE_DRAWS + "，深度特征可信度较低");
        }

        return snapshot;
    }

    /** 项目兼容入口：直接使用 Models.Draw 构建快照。 */
    public PointInTimeSnapshot buildSnapshotBeforeDecision(
            List<Models.Draw> allDraws,
            String baseIssue,
            String targetIssue,
            Models.Draw targetDraw,
            String featureSummary,
            String candidateSummary,
            String gateSummary,
            String decisionSummary) {
        SnapshotInput input = new SnapshotInput();

        input.allDraws = toDrawRecords(allDraws);
        input.targetDraw = toDrawRecord(targetDraw);
        input.baseIssue = safe(baseIssue);
        input.targetIssue = safe(targetIssue);
        input.featureSummary = safe(featureSummary);
        input.candidateSummary = safe(candidateSummary);
        input.gateSummary = safe(gateSummary);
        input.decisionSummary = safe(decisionSummary);
        input.maxVisibleDraws = DEFAULT_MAX_VISIBLE_DRAWS;
        input.nowMs = System.currentTimeMillis();

        return buildSnapshotBeforeDecision(input);
    }

    /** 项目兼容入口：通过 Decision 构建快照。 */
    public PointInTimeSnapshot buildSnapshotBeforeDecision(
            List<Models.Draw> allDraws,
            Models.Decision decision,
            Models.Draw targetDraw,
            String featureSummary,
            String candidateSummary,
            String gateSummary,
            String decisionSummary) {
        if (decision == null) {
            return PointInTimeSnapshot.error("Decision 为空，无法构建时间点快照");
        }

        String baseIssue = safe(decision.baseIssue);
        String targetIssue = safe(decision.targetIssue);

        if (baseIssue.length() == 0 && allDraws != null && allDraws.size() > 0) {
            Models.Draw latestVisible = findLatestBeforeTarget(allDraws, targetIssue);
            if (latestVisible != null) {
                baseIssue = safe(latestVisible.issue);
            }
        }

        if (featureSummary == null || featureSummary.length() == 0) {
            featureSummary =
                    "DecisionFeature:P"
                            + decision.position
                            + " K3="
                            + two(decision.n1)
                            + "/"
                            + two(decision.n2)
                            + "/"
                            + two(decision.n3)
                            + " state="
                            + safe(decision.stateMode);
        }

        if (candidateSummary == null || candidateSummary.length() == 0) {
            candidateSummary =
                    "Candidate:P"
                            + decision.position
                            + " mask="
                            + decision.k3Mask
                            + " finalP="
                            + decision.finalP
                            + " gate="
                            + decision.dynamicGate;
        }

        if (gateSummary == null || gateSummary.length() == 0) {
            gateSummary =
                    "Gate:finalP="
                            + decision.finalP
                            + " dynamicGate="
                            + decision.dynamicGate
                            + " edge="
                            + decision.edge;
        }

        if (decisionSummary == null || decisionSummary.length() == 0) {
            decisionSummary =
                    safe(decision.decision)
                            + " target="
                            + targetIssue
                            + " block="
                            + safe(decision.blockCode)
                            + " reason="
                            + safe(decision.reasonSummary);
        }

        return buildSnapshotBeforeDecision(
                allDraws,
                baseIssue,
                targetIssue,
                targetDraw,
                featureSummary,
                candidateSummary,
                gateSummary,
                decisionSummary);
    }

    /** 恢复某一步的可见窗口，并重算 dataHash。 */
    public RestoredSnapshot restoreSnapshotForBacktestStep(
            PointInTimeSnapshot snapshot, List<DrawRecord> allDraws, int maxVisibleDraws) {
        RestoredSnapshot restored = new RestoredSnapshot();

        if (snapshot == null) {
            restored.ok = false;
            restored.message = "snapshot 为空，无法恢复";
            restored.visibleDraws = new ArrayList<DrawRecord>();
            return restored;
        }

        if (safe(snapshot.baseIssue).length() == 0) {
            restored.ok = false;
            restored.message = "snapshot.baseIssue 为空，无法恢复";
            restored.visibleDraws = new ArrayList<DrawRecord>();
            return restored;
        }

        int maxWindow = maxVisibleDraws;
        if (maxWindow <= 0) {
            maxWindow = DEFAULT_MAX_VISIBLE_DRAWS;
        }

        List<DrawRecord> visible = buildVisibleDrawWindow(allDraws, snapshot.baseIssue, maxWindow);

        String recalculatedHash =
                buildDataHash(
                        snapshot.baseIssue,
                        snapshot.targetIssue,
                        visible,
                        snapshot.visibleTradeCount,
                        snapshot.visibleProfileVersion,
                        snapshot.visibleCalibrationVersion,
                        snapshot.featureSummary,
                        snapshot.candidateSummary,
                        snapshot.gateSummary,
                        snapshot.decisionSummary);

        restored.visibleDraws = visible;
        restored.snapshot = snapshot;
        restored.recalculatedHash = recalculatedHash;
        restored.ok = safe(snapshot.dataHash).equals(recalculatedHash);

        if (restored.ok) {
            restored.message = "快照恢复成功，dataHash 一致";
        } else {
            restored.message = "快照恢复完成，但 dataHash 不一致，可能数据窗口或摘要被修改";
        }

        return restored;
    }

    /** Models.Draw 兼容恢复入口。 */
    public RestoredSnapshot restoreSnapshotForBacktestStepFromModels(
            PointInTimeSnapshot snapshot, List<Models.Draw> allDraws, int maxVisibleDraws) {
        return restoreSnapshotForBacktestStep(snapshot, toDrawRecords(allDraws), maxVisibleDraws);
    }

    /** 按 baseIssue 构建可见开奖窗口。 只保留 issue <= baseIssue 的开奖。 返回结果按 issue 从旧到新排序。 */
    public List<DrawRecord> buildVisibleDrawWindow(
            List<DrawRecord> allDraws, String baseIssue, int maxVisibleDraws) {
        ArrayList<DrawRecord> visible = new ArrayList<DrawRecord>();

        if (allDraws == null || allDraws.size() == 0 || safe(baseIssue).length() == 0) {
            return visible;
        }

        for (int i = 0; i < allDraws.size(); i++) {
            DrawRecord d = allDraws.get(i);

            if (d == null) {
                continue;
            }

            if (safe(d.issue).length() == 0) {
                continue;
            }

            if (compareIssue(d.issue, baseIssue) <= 0) {
                visible.add(d.copyLight());
            }
        }

        Collections.sort(
                visible,
                new Comparator<DrawRecord>() {
                    @Override
                    public int compare(DrawRecord a, DrawRecord b) {
                        if (a == null && b == null) {
                            return 0;
                        }

                        if (a == null) {
                            return -1;
                        }

                        if (b == null) {
                            return 1;
                        }

                        return compareIssue(a.issue, b.issue);
                    }
                });

        int maxWindow = maxVisibleDraws;

        if (maxWindow <= 0) {
            maxWindow = DEFAULT_MAX_VISIBLE_DRAWS;
        }

        if (visible.size() > maxWindow) {
            ArrayList<DrawRecord> trimmed = new ArrayList<DrawRecord>();
            int start = visible.size() - maxWindow;

            for (int i = start; i < visible.size(); i++) {
                trimmed.add(visible.get(i));
            }

            return trimmed;
        }

        return visible;
    }

    /** 检查快照有没有未来泄漏。 */
    public LeakCheckResult verifyNoFutureLeak(
            PointInTimeSnapshot snapshot, List<DrawRecord> visibleDraws, DrawRecord targetDraw) {
        if (snapshot == null) {
            return LeakCheckResult.error("snapshot 为空");
        }

        String baseIssue = safe(snapshot.baseIssue);
        String targetIssue = safe(snapshot.targetIssue);

        if (baseIssue.length() == 0) {
            return LeakCheckResult.error("baseIssue 为空");
        }

        if (targetIssue.length() == 0) {
            return LeakCheckResult.error("targetIssue 为空");
        }

        if (compareIssue(targetIssue, baseIssue) <= 0) {
            return LeakCheckResult.error("targetIssue 必须大于 baseIssue，当前存在未来顺序错误");
        }

        if (visibleDraws != null) {
            for (int i = 0; i < visibleDraws.size(); i++) {
                DrawRecord d = visibleDraws.get(i);

                if (d == null) {
                    continue;
                }

                String issue = safe(d.issue);

                if (issue.length() == 0) {
                    continue;
                }

                if (compareIssue(issue, baseIssue) > 0) {
                    return LeakCheckResult.error(
                            "可见窗口中发现未来期号：" + issue + " > baseIssue " + baseIssue);
                }

                if (sameIssue(issue, targetIssue)) {
                    return LeakCheckResult.error("可见窗口中混入 targetIssue：" + targetIssue + "，属于偷看未来");
                }
            }
        }

        if (targetDraw != null) {
            String targetDrawIssue = safe(targetDraw.issue);

            if (targetDrawIssue.length() > 0 && !sameIssue(targetDrawIssue, targetIssue)) {
                return LeakCheckResult.error("targetDraw.issue 与 snapshot.targetIssue 不一致");
            }

            String openCode = normalizeCode(targetDraw.openCode);

            if (openCode.length() >= 10) {
                String allSummary =
                        safe(snapshot.featureSummary)
                                + "\n"
                                + safe(snapshot.candidateSummary)
                                + "\n"
                                + safe(snapshot.gateSummary)
                                + "\n"
                                + safe(snapshot.decisionSummary);

                String normalizedSummary = normalizeCode(allSummary);

                if (normalizedSummary.indexOf(openCode) >= 0) {
                    return LeakCheckResult.error("摘要中疑似包含目标期实际开奖号码，禁止用于回测决策");
                }
            }
        }

        if (snapshot.visibleDrawCount < 0) {
            return LeakCheckResult.error("visibleDrawCount 小于 0");
        }

        if (visibleDraws != null && snapshot.visibleDrawCount != visibleDraws.size()) {
            return LeakCheckResult.warning("快照 visibleDrawCount 与当前窗口大小不一致，建议重新生成快照");
        }

        return LeakCheckResult.ok("未发现未来数据泄漏");
    }

    /** Models.Draw 兼容泄漏检查。 */
    public LeakCheckResult verifyNoFutureLeakFromModels(
            PointInTimeSnapshot snapshot, List<Models.Draw> visibleDraws, Models.Draw targetDraw) {
        return verifyNoFutureLeak(snapshot, toDrawRecords(visibleDraws), toDrawRecord(targetDraw));
    }

    /** 生成 dataHash。 使用稳定 FNV-1a 64-bit。 */
    public String buildDataHash(
            String baseIssue,
            String targetIssue,
            List<DrawRecord> visibleDraws,
            int visibleTradeCount,
            long visibleProfileVersion,
            long visibleCalibrationVersion,
            String featureSummary,
            String candidateSummary,
            String gateSummary,
            String decisionSummary) {
        StringBuilder sb = new StringBuilder();

        sb.append("base=").append(safe(baseIssue)).append('|');
        sb.append("target=").append(safe(targetIssue)).append('|');
        sb.append("trade=").append(Math.max(0, visibleTradeCount)).append('|');
        sb.append("profile=").append(Math.max(0L, visibleProfileVersion)).append('|');
        sb.append("calibration=").append(Math.max(0L, visibleCalibrationVersion)).append('|');

        if (visibleDraws != null) {
            for (int i = 0; i < visibleDraws.size(); i++) {
                DrawRecord d = visibleDraws.get(i);

                if (d == null) {
                    continue;
                }

                sb.append("D{");
                sb.append(safe(d.issue)).append(',');
                sb.append(normalizeCode(d.openCode)).append(',');
                sb.append(d.openTimeMs).append(',');
                sb.append(safe(d.sourceTag));
                sb.append("}|");
            }
        }

        sb.append("F=").append(limitForHash(featureSummary, HASH_TEXT_LIMIT)).append('|');
        sb.append("C=").append(limitForHash(candidateSummary, HASH_TEXT_LIMIT)).append('|');
        sb.append("G=").append(limitForHash(gateSummary, HASH_TEXT_LIMIT)).append('|');
        sb.append("R=").append(limitForHash(decisionSummary, HASH_TEXT_LIMIT)).append('|');

        long hash = fnv1a64(sb.toString());

        return Long.toHexString(hash);
    }

    public String buildSnapshotId(String baseIssue, String targetIssue, String dataHash) {
        String raw = "PIT|" + safe(baseIssue) + "|" + safe(targetIssue) + "|" + safe(dataHash);
        long hash = fnv1a64(raw);

        return "PIT_"
                + cleanIdPart(baseIssue)
                + "_"
                + cleanIdPart(targetIssue)
                + "_"
                + Long.toHexString(hash);
    }

    public String buildCompactIssueList(List<DrawRecord> visibleDraws, int limit) {
        if (visibleDraws == null || visibleDraws.size() == 0) {
            return "";
        }

        int realLimit = limit <= 0 ? COMPACT_ISSUE_LIMIT : limit;
        int start = visibleDraws.size() - realLimit;

        if (start < 0) {
            start = 0;
        }

        StringBuilder sb = new StringBuilder();

        for (int i = start; i < visibleDraws.size(); i++) {
            DrawRecord d = visibleDraws.get(i);

            if (d == null) {
                continue;
            }

            if (sb.length() > 0) {
                sb.append(',');
            }

            sb.append(safe(d.issue));
        }

        return sb.toString();
    }

    public String buildVisibleTailBrief(List<DrawRecord> visibleDraws, int limit) {
        if (visibleDraws == null || visibleDraws.size() == 0) {
            return "无可见开奖";
        }

        int realLimit = limit <= 0 ? TAIL_BRIEF_LIMIT : limit;
        int start = visibleDraws.size() - realLimit;

        if (start < 0) {
            start = 0;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("可见尾部开奖：");

        for (int i = start; i < visibleDraws.size(); i++) {
            DrawRecord d = visibleDraws.get(i);

            if (d == null) {
                continue;
            }

            if (i > start) {
                sb.append(" | ");
            }

            sb.append(safe(d.issue));
            sb.append(":");
            sb.append(limitForHash(d.openCode, 40));
        }

        return sb.toString();
    }

    /** 将快照状态写入 DbStore，用于 UI / 回测审计显示。 */
    public void persistSnapshotState(DbStore store, PointInTimeSnapshot snapshot) {
        if (store == null || snapshot == null) {
            return;
        }

        try {
            store.setState("pit_snapshot_status", safe(snapshot.status));
            store.setState("pit_snapshot_id", safe(snapshot.snapshotId));
            store.setState("pit_snapshot_base_issue", safe(snapshot.baseIssue));
            store.setState("pit_snapshot_target_issue", safe(snapshot.targetIssue));
            store.setState("pit_snapshot_data_hash", safe(snapshot.dataHash));
            store.setState(
                    "pit_snapshot_visible_draw_count", String.valueOf(snapshot.visibleDrawCount));
            store.setState("pit_snapshot_no_future_leak", snapshot.noFutureLeakOk ? "1" : "0");
            store.setState("pit_snapshot_warning", safe(snapshot.warningText));
            store.setState(
                    "pit_snapshot_summary", limitText(snapshot.toSummaryText(), STATE_TEXT_LIMIT));
            store.setState(
                    "point_in_time_snapshot_summary",
                    limitText(snapshot.toSummaryText(), STATE_TEXT_LIMIT));
            store.setState("future_leak_guard_status", snapshot.noFutureLeakOk ? "OK" : "ERROR");
            store.setState(
                    "future_leak_guard_summary", limitText(snapshot.warningText, STATE_TEXT_LIMIT));
        } catch (Throwable e) {
        }
    }

    /** 期号比较。 优先提取数字比较，避免字符串 100 < 99。 */
    public static int compareIssue(String a, String b) {
        String da = digitsOnly(a);
        String db = digitsOnly(b);

        if (da.length() > 0 && db.length() > 0) {
            da = stripLeadingZero(da);
            db = stripLeadingZero(db);

            if (da.length() != db.length()) {
                return da.length() < db.length() ? -1 : 1;
            }

            int cmp = da.compareTo(db);

            if (cmp < 0) {
                return -1;
            }

            if (cmp > 0) {
                return 1;
            }

            return 0;
        }

        String sa = safe(a);
        String sb = safe(b);
        int cmp2 = sa.compareTo(sb);

        if (cmp2 < 0) {
            return -1;
        }

        if (cmp2 > 0) {
            return 1;
        }

        return 0;
    }

    public static boolean sameIssue(String a, String b) {
        return compareIssue(a, b) == 0;
    }

    public static String digitsOnly(String value) {
        String v = safe(value);
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < v.length(); i++) {
            char c = v.charAt(i);

            if (c >= '0' && c <= '9') {
                sb.append(c);
            }
        }

        return sb.toString();
    }

    public static String stripLeadingZero(String value) {
        String v = safe(value);
        int i = 0;

        while (i < v.length() - 1 && v.charAt(i) == '0') {
            i++;
        }

        return v.substring(i);
    }

    public static String normalizeCode(String code) {
        String c = safe(code);
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < c.length(); i++) {
            char ch = c.charAt(i);

            if (ch >= '0' && ch <= '9') {
                sb.append(ch);
            }
        }

        return sb.toString();
    }

    public ArrayList<DrawRecord> toDrawRecords(List<Models.Draw> draws) {
        ArrayList<DrawRecord> out = new ArrayList<DrawRecord>();

        if (draws == null || draws.size() == 0) {
            return out;
        }

        for (int i = 0; i < draws.size(); i++) {
            DrawRecord r = toDrawRecord(draws.get(i));

            if (r != null && r.issue.length() > 0 && r.openCode.length() > 0) {
                out.add(r);
            }
        }

        return out;
    }

    public DrawRecord toDrawRecord(Models.Draw draw) {
        if (draw == null) {
            return null;
        }

        DrawRecord r = new DrawRecord();

        r.issue = safe(draw.issue);
        r.openCode = buildOpenCode(draw);
        r.openTimeMs = draw.openTime;
        r.sourceTag = "Models.Draw";
        r.rawBrief = r.toBriefText();

        return r;
    }

    public String buildOpenCode(Models.Draw draw) {
        if (draw == null || draw.numbers == null || draw.numbers.length <= 0) {
            return "";
        }

        StringBuilder sb = new StringBuilder();

        int limit = Math.min(draw.numbers.length, AppConfig.NUMBER_COUNT);

        for (int i = 0; i < limit; i++) {
            if (i > 0) {
                sb.append(",");
            }

            int n = draw.numbers[i];

            if (n < 10) {
                sb.append("0");
            }

            sb.append(n);
        }

        return sb.toString();
    }

    private Models.Draw findLatestBeforeTarget(List<Models.Draw> allDraws, String targetIssue) {
        if (allDraws == null || allDraws.size() == 0 || safe(targetIssue).length() == 0) {
            return null;
        }

        Models.Draw best = null;

        for (int i = 0; i < allDraws.size(); i++) {
            Models.Draw d = allDraws.get(i);

            if (d == null || d.issue == null || d.issue.length() == 0) {
                continue;
            }

            if (compareIssue(d.issue, targetIssue) >= 0) {
                continue;
            }

            if (best == null || compareIssue(d.issue, best.issue) > 0) {
                best = d;
            }
        }

        return best;
    }

    private static String limitForHash(String value, int maxLen) {
        String v = safe(value);

        if (maxLen <= 0) {
            return "";
        }

        if (v.length() <= maxLen) {
            return v;
        }

        return v.substring(0, maxLen);
    }

    private static String limitText(String value, int maxLen) {
        String v = safe(value);

        if (maxLen <= 0) {
            return "";
        }

        if (v.length() <= maxLen) {
            return v;
        }

        return v.substring(0, maxLen);
    }

    private static long fnv1a64(String value) {
        String v = safe(value);
        long hash = 0xcbf29ce484222325L;
        long prime = 0x100000001b3L;

        for (int i = 0; i < v.length(); i++) {
            hash ^= v.charAt(i);
            hash *= prime;
        }

        return hash;
    }

    private static String appendText(String oldText, String addText) {
        String a = safe(oldText);
        String b = safe(addText);

        if (a.length() == 0) {
            return b;
        }

        if (b.length() == 0) {
            return a;
        }

        if (a.indexOf(b) >= 0) {
            return a;
        }

        return a + "；" + b;
    }

    private static String cleanIdPart(String value) {
        String v = safe(value);
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < v.length(); i++) {
            char c = v.charAt(i);

            if ((c >= '0' && c <= '9') || (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')) {
                sb.append(c);
            } else {
                sb.append("_");
            }
        }

        String out = sb.toString();

        if (out.length() > 48) {
            out = out.substring(out.length() - 48);
        }

        return out;
    }

    private static String safe(String value) {
        if (value == null) {
            return "";
        }

        return value;
    }

    private static String two(int n) {
        if (n <= 0) {
            return "00";
        }

        if (n < 10) {
            return "0" + n;
        }

        return String.valueOf(n);
    }

    public static final class SnapshotInput {
        public List<DrawRecord> allDraws;
        public DrawRecord targetDraw;

        public String baseIssue;
        public String targetIssue;

        public int maxVisibleDraws;
        public int visibleTradeCount;

        public long visibleProfileVersion;
        public long visibleCalibrationVersion;
        public long nowMs;

        public String featureSummary;
        public String candidateSummary;
        public String gateSummary;
        public String decisionSummary;

        public SnapshotInput() {
            allDraws = new ArrayList<DrawRecord>();
            targetDraw = null;
            baseIssue = "";
            targetIssue = "";
            maxVisibleDraws = DEFAULT_MAX_VISIBLE_DRAWS;
            visibleTradeCount = 0;
            visibleProfileVersion = 0L;
            visibleCalibrationVersion = 0L;
            nowMs = 0L;
            featureSummary = "";
            candidateSummary = "";
            gateSummary = "";
            decisionSummary = "";
        }
    }

    public static final class DrawRecord {
        public String issue;
        public String openCode;
        public long openTimeMs;
        public String sourceTag;
        public String rawBrief;

        public DrawRecord() {
            issue = "";
            openCode = "";
            openTimeMs = 0L;
            sourceTag = "";
            rawBrief = "";
        }

        public DrawRecord(String issue, String openCode, long openTimeMs) {
            this.issue = safe(issue);
            this.openCode = safe(openCode);
            this.openTimeMs = openTimeMs;
            this.sourceTag = "";
            this.rawBrief = "";
        }

        public DrawRecord copyLight() {
            DrawRecord d = new DrawRecord();

            d.issue = safe(issue);
            d.openCode = safe(openCode);
            d.openTimeMs = openTimeMs;
            d.sourceTag = safe(sourceTag);
            d.rawBrief = limitForHash(rawBrief, 120);

            return d;
        }

        public String toBriefText() {
            return safe(issue) + ":" + safe(openCode);
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

        public String featureSummary;
        public String candidateSummary;
        public String gateSummary;
        public String decisionSummary;

        public String dataHash;
        public String visibleIssueListCompact;
        public String visibleTailBrief;

        public long createdAtMs;

        public boolean noFutureLeakOk;
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
            featureSummary = "";
            candidateSummary = "";
            gateSummary = "";
            decisionSummary = "";
            dataHash = "";
            visibleIssueListCompact = "";
            visibleTailBrief = "";
            createdAtMs = 0L;
            noFutureLeakOk = false;
            status = STATUS_WARNING;
            warningText = "";
        }

        public static PointInTimeSnapshot error(String message) {
            PointInTimeSnapshot s = new PointInTimeSnapshot();

            s.status = STATUS_ERROR;
            s.noFutureLeakOk = false;
            s.warningText = safe(message);
            s.createdAtMs = System.currentTimeMillis();

            return s;
        }

        public String toSummaryText() {
            StringBuilder sb = new StringBuilder();

            sb.append("PIT快照 ");
            sb.append(status);
            sb.append(" | base=").append(baseIssue);
            sb.append(" -> target=").append(targetIssue);
            sb.append(" | train=").append(trainStartIssue).append("~").append(trainEndIssue);
            sb.append(" | draws=").append(visibleDrawCount);
            sb.append(" | trades=").append(visibleTradeCount);
            sb.append(" | profileVer=").append(visibleProfileVersion);
            sb.append(" | calVer=").append(visibleCalibrationVersion);
            sb.append(" | hash=").append(dataHash);

            if (visibleIssueListCompact != null && visibleIssueListCompact.length() > 0) {
                sb.append(" | visible=").append(visibleIssueListCompact);
            }

            if (warningText != null && warningText.length() > 0) {
                sb.append(" | ").append(warningText);
            }

            return sb.toString();
        }
    }

    public static final class RestoredSnapshot {
        public boolean ok;
        public String message;
        public PointInTimeSnapshot snapshot;
        public List<DrawRecord> visibleDraws;
        public String recalculatedHash;

        public RestoredSnapshot() {
            ok = false;
            message = "";
            snapshot = null;
            visibleDraws = new ArrayList<DrawRecord>();
            recalculatedHash = "";
        }
    }

    public static final class LeakCheckResult {
        public boolean ok;
        public boolean warning;
        public String status;
        public String message;

        public LeakCheckResult() {
            ok = false;
            warning = false;
            status = STATUS_ERROR;
            message = "";
        }

        public static LeakCheckResult ok(String message) {
            LeakCheckResult r = new LeakCheckResult();

            r.ok = true;
            r.warning = false;
            r.status = STATUS_OK;
            r.message = safe(message);

            return r;
        }

        public static LeakCheckResult warning(String message) {
            LeakCheckResult r = new LeakCheckResult();

            r.ok = true;
            r.warning = true;
            r.status = STATUS_WARNING;
            r.message = safe(message);

            return r;
        }

        public static LeakCheckResult error(String message) {
            LeakCheckResult r = new LeakCheckResult();

            r.ok = false;
            r.warning = false;
            r.status = STATUS_ERROR;
            r.message = safe(message);

            return r;
        }
    }

    /* STARCORE_TIANSHU_B04_PIT_FRAME_CONTRACT */
    public static final String B04_FRAME_SCHEMA = "TIANSHU_PIT_FRAME_SCHEMA_B04_V1";

    /** Compact immutable frame-only PIT certificate used by the V51 live path. */
    public FrameSnapshot buildFrameSnapshotBeforeDecision(
            V51Models.PredictionContext context, V51Models.DrawFrame[] source) {
        if (context == null) {
            return FrameSnapshot.error("CONTEXT_NULL");
        }
        if (source == null || source.length == 0) {
            return FrameSnapshot.error("HISTORY_EMPTY");
        }
        int start = Math.max(0, source.length - DEFAULT_MAX_VISIBLE_DRAWS);
        String first = "";
        String last = "";
        String previous = "";
        int accepted = 0;
        int rejectedFuture = 0;
        int rejectedInvalid = 0;
        StringBuilder canonical = new StringBuilder();
        canonical
                .append(B04_FRAME_SCHEMA)
                .append('|')
                .append(context.baseIssue)
                .append('|')
                .append(context.targetIssue)
                .append('|')
                .append(context.historyHash);
        for (int i = start; i < source.length; i++) {
            V51Models.DrawFrame frame = source[i];
            if (frame == null
                    || frame.issue == null
                    || frame.issue.length() == 0
                    || !V51Math.validPermutation(frame.permutation)) {
                rejectedInvalid++;
                continue;
            }
            if (compareIssue(frame.issue, context.baseIssue) > 0
                    || compareIssue(frame.issue, context.targetIssue) >= 0) {
                rejectedFuture++;
                continue;
            }
            if (previous.length() > 0 && compareIssue(frame.issue, previous) <= 0) {
                rejectedInvalid++;
                continue;
            }
            if (accepted == 0) {
                first = frame.issue;
            }
            last = frame.issue;
            previous = frame.issue;
            accepted++;
            canonical.append('|').append(frame.issue).append(':');
            for (int p = 0; p < frame.permutation.length; p++) {
                canonical.append(frame.permutation[p]).append(',');
            }
        }
        boolean ok =
                accepted > 0
                        && rejectedFuture == 0
                        && last.equals(context.baseIssue)
                        && compareIssue(context.targetIssue, context.baseIssue) > 0;
        String hash = V51ChecksumEngine.sha256(canonical.toString());
        String detail =
                "accepted="
                        + accepted
                        + "|invalid="
                        + rejectedInvalid
                        + "|future="
                        + rejectedFuture
                        + "|first="
                        + first
                        + "|last="
                        + last;
        return new FrameSnapshot(
                ok,
                first,
                last,
                context.targetIssue,
                accepted,
                rejectedInvalid,
                rejectedFuture,
                hash,
                detail);
    }

    public static final class FrameSnapshot {
        public final boolean noFutureLeak;
        public final String firstIssue;
        public final String lastIssue;
        public final String targetIssue;
        public final int acceptedRows;
        public final int rejectedInvalidRows;
        public final int rejectedFutureRows;
        public final String featureInputHash;
        public final String detail;

        FrameSnapshot(
                boolean noFutureLeak,
                String firstIssue,
                String lastIssue,
                String targetIssue,
                int acceptedRows,
                int rejectedInvalidRows,
                int rejectedFutureRows,
                String featureInputHash,
                String detail) {
            this.noFutureLeak = noFutureLeak;
            this.firstIssue = safe(firstIssue);
            this.lastIssue = safe(lastIssue);
            this.targetIssue = safe(targetIssue);
            this.acceptedRows = Math.max(0, acceptedRows);
            this.rejectedInvalidRows = Math.max(0, rejectedInvalidRows);
            this.rejectedFutureRows = Math.max(0, rejectedFutureRows);
            this.featureInputHash = safe(featureInputHash);
            this.detail = safe(detail);
        }

        static FrameSnapshot error(String detail) {
            return new FrameSnapshot(
                    false,
                    "",
                    "",
                    "",
                    0,
                    0,
                    0,
                    V51ChecksumEngine.sha256(B04_FRAME_SCHEMA + '|' + safe(detail)),
                    safe(detail));
        }

        public String replayHash() {
            return V51ChecksumEngine.sha256(
                    B04_FRAME_SCHEMA
                            + '|'
                            + noFutureLeak
                            + '|'
                            + firstIssue
                            + '|'
                            + lastIssue
                            + '|'
                            + targetIssue
                            + '|'
                            + acceptedRows
                            + '|'
                            + rejectedInvalidRows
                            + '|'
                            + rejectedFutureRows
                            + '|'
                            + featureInputHash
                            + '|'
                            + detail);
        }
    }
}
