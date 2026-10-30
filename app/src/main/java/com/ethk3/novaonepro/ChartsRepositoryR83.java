package com.ethk3.novaonepro;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.CancellationSignal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import static com.ethk3.novaonepro.ResearchVisualDataR82.*;

/**
 * Read-only decision charts for the 走势图表 page. Every panel is computed from committed rows
 * of one read-only connection; each panel fails or stays empty on its own, never as zero.
 * K3 on one position covers 3 of 10 numbers, so a random pick hits 30%; the hit-rate charts
 * are judged against that baseline.
 */
final class ChartsRepositoryR83 {
    static final String PAGE = "charts";
    static final double BASELINE = 0.30;
    static final double BREAK_EVEN = AppConfig.MISS_LOSS / (AppConfig.WIN_NET_PROFIT + AppConfig.MISS_LOSS);
    private ChartsRepositoryR83() {}

    static List<Panel> load(SQLiteDatabase original, CancellationSignal cancel) {
        SQLiteDatabase db=com.ethk3.novaonepro.strategyv2.ResearchRouteR29.reader(original);
        List<Panel> out = new ArrayList<>();
        int position = latestPosition(db, cancel);
        List<String> drawIssues = new ArrayList<>();
        List<int[]> draws = draws(db, cancel, drawIssues);
        out.add(safe("全部算法逐期实盘榜", () -> r27AlgorithmBoard(db, cancel)));
        out.add(safe("算法实盘排行榜", () -> algorithmBoard(db, cancel)));
        out.add(safe("最佳算法累计命中率", () -> bestAlgorithmCurve(db, cancel)));
        out.add(safe("出手资金K线", () -> actionCandles(db, cancel)));
        out.add(safe("全部预测命中率走势", () -> hitTrend(db, cancel, false)));
        out.add(safe("出手命中率走势", () -> hitTrend(db, cancel, true)));
        out.add(safe("第" + position + "位置号码K线", () -> numberCandles(draws, drawIssues, position)));
        out.add(safe("第" + position + "位置号码轨迹", () -> numberLine(draws, drawIssues, position)));
        out.add(safe("号码遗漏热力图", () -> omissionHeat(draws)));
        out.add(safe("第" + position + "位置冷热号", () -> hotCold(draws, position)));
        out.add(safe("预测概率校准", () -> calibration(db, cancel)));
        out.add(safe("主链算法参与权重", () -> mainlineWeight(db, cancel)));
        out.add(safe("算法迭代进化事件", () -> evolutionEvents(db, cancel)));
        out.add(safe("深度学习训练误差", () -> networkBrier(db, cancel)));
        out.add(safe("算法父子谱系", () -> lineage(db, cancel)));
        return out;
    }

    interface Builder { Panel build(); }

    private static Panel safe(String title, Builder builder) {
        try {
            return builder.build();
        } catch (android.os.OperationCanceledException canceled) {
            throw canceled;
        } catch (RuntimeException failure) {
            return new Panel(PAGE, title, "STATUS", "", "", "读取失败：" + failure.getClass().getSimpleName() + "；不按0处理。",
                    "READ_ERROR", null, null);
        }
    }

    private static int latestPosition(SQLiteDatabase db, CancellationSignal cancel) {
        try (Cursor c = db.rawQuery("SELECT position FROM " + DbHelper.TABLE_R35_SHADOW_OBSERVATION
                + " WHERE position BETWEEN 1 AND 10 ORDER BY target_issue DESC LIMIT 1", null, cancel)) {
            return c.moveToFirst() ? c.getInt(0) : 1;
        } catch (RuntimeException missing) {
            return 1;
        }
    }

    /** Oldest-first clean draws (at most 600) as positions 1..10. */
    private static List<int[]> draws(SQLiteDatabase db, CancellationSignal cancel, List<String> issues) {
        List<int[]> rows = new ArrayList<>();
        try (Cursor c = db.rawQuery("SELECT issue,n1,n2,n3,n4,n5,n6,n7,n8,n9,n10 FROM draws WHERE dirty=0 ORDER BY issue DESC LIMIT 600",
                null, cancel)) {
            while (c.moveToNext()) {
                int[] n = new int[10];
                for (int i = 0; i < 10; i++) n[i] = c.getInt(i + 1);
                rows.add(0, n);
                issues.add(0, c.getString(0));
            }
        }
        return rows;
    }

    private static Panel actionCandles(SQLiteDatabase db, CancellationSignal cancel) {
        List<Double> pnl = new ArrayList<>();
        List<String> issues = new ArrayList<>();
        try (Cursor c = db.rawQuery("SELECT e.target_issue,o.hit_label FROM " + ExplorationLaneR83.TABLE + " e JOIN "
                + DbHelper.TABLE_R35_SHADOW_OBSERVATION + " o ON o.target_issue=e.target_issue WHERE e.action<>'NONE'"
                + " AND o.settled=1 AND o.hit_label>=0 ORDER BY e.target_issue DESC LIMIT 600", null, cancel)) {
            while (c.moveToNext()) {
                issues.add(0, c.getString(0));
                pnl.add(0, c.getInt(1) == 1 ? AppConfig.WIN_NET_PROFIT : -AppConfig.MISS_LOSS);
            }
        } catch (android.database.sqlite.SQLiteException noTableYet) {
            // The exploration table is created with the first frozen observation.
        }
        List<Point> points = new ArrayList<>();
        int per = Math.max(1, (pnl.size() + MAX_POINTS - 1) / MAX_POINTS);
        per = Math.max(per, 5);
        double equity = 0;
        for (int i = 0; i < pnl.size(); i += per) {
            double open = equity, high = equity, low = equity;
            int end = Math.min(pnl.size(), i + per), wins = 0;
            for (int j = i; j < end; j++) {
                equity += pnl.get(j);
                high = Math.max(high, equity);
                low = Math.min(low, equity);
                if (pnl.get(j) > 0) wins++;
            }
            points.add(new Point(issues.get(end - 1), equity, low, high, end - i,
                    issues.get(i) + "—" + issues.get(end - 1) + "：" + (end - i) + "次出手，中" + wins + "，本根盈亏 "
                            + fmt(equity - open) + "，累计 " + fmt(equity), open));
        }
        return new Panel(PAGE, "出手资金K线（每" + per + "次出手一根）", "CANDLE", ExplorationLaneR83.TABLE, "最近" + pnl.size() + "次已结算出手",
                "按单注 命中+" + AppConfig.WIN_NET_PROFIT + " / 未中-" + AppConfig.MISS_LOSS + " 累计。K线持续走低说明当前出手在亏钱，应少跟或不跟；"
                        + "持续走高且样本够多才有参考价值。累计 " + fmt(equity) + "。", points.isEmpty() ? "EMPTY_NEEDS_SETTLED_ACTIONS" : "READY", points, null);
    }

    private static Panel hitTrend(SQLiteDatabase db, CancellationSignal cancel, boolean actionsOnly) {
        String sql = actionsOnly
                ? "SELECT o.target_issue,o.hit_label FROM " + ExplorationLaneR83.TABLE + " e JOIN " + DbHelper.TABLE_R35_SHADOW_OBSERVATION
                        + " o ON o.target_issue=e.target_issue WHERE e.action<>'NONE' AND o.settled=1 AND o.hit_label>=0 ORDER BY o.target_issue DESC LIMIT 139"
                : "SELECT target_issue,hit_label FROM " + DbHelper.TABLE_R35_SHADOW_OBSERVATION
                        + " WHERE settled=1 AND hit_label>=0 ORDER BY target_issue DESC LIMIT 139";
        List<String> issues = new ArrayList<>();
        List<Integer> hits = new ArrayList<>();
        try (Cursor c = db.rawQuery(sql, null, cancel)) {
            while (c.moveToNext()) {
                issues.add(0, c.getString(0));
                hits.add(0, c.getInt(1) == 1 ? 1 : 0);
            }
        } catch (android.database.sqlite.SQLiteException noTableYet) {
            // Empty until the first exploration decision.
        }
        int window = 20, total = 0;
        for (int h : hits) total += h;
        List<Point> points = new ArrayList<>();
        for (int i = window - 1; i < hits.size(); i++) {
            int k = 0;
            for (int j = i - window + 1; j <= i; j++) k += hits.get(j);
            double[] w = wilson(k, window);
            points.add(new Point(issues.get(i), k / (double) window, w[0], w[1], window,
                    "截至" + issues.get(i) + "近" + window + "次命中" + k + "；随机基线30%"));
        }
        String overall = hits.isEmpty() ? "无" : String.format(Locale.ROOT, "%.1f%%（%d/%d）", 100.0 * total / hits.size(), total, hits.size());
        return new Panel(PAGE, (actionsOnly ? "出手命中率走势" : "全部预测命中率走势") + "（滚动20次，基线30%）", "LINE",
                DbHelper.TABLE_R35_SHADOW_OBSERVATION, "最近" + hits.size() + "次已结算",
                "竖线是95%置信区间。只有曲线和区间下沿长期高于30%，才说明算法真的比随机强；在30%上下波动就等于随机。整体命中 " + overall + "。",
                points.isEmpty() ? "EMPTY_NEEDS_20_SETTLED" : "READY", points, null);
    }

    private static Panel numberCandles(List<int[]> draws, List<String> issues, int position) {
        List<Point> points = new ArrayList<>();
        int per = Math.max(5, (draws.size() + MAX_POINTS - 1) / MAX_POINTS);
        for (int i = 0; i < draws.size(); i += per) {
            int end = Math.min(draws.size(), i + per), high = 0, low = 11;
            double sum = 0;
            for (int j = i; j < end; j++) {
                int v = draws.get(j)[position - 1];
                high = Math.max(high, v);
                low = Math.min(low, v);
                sum += v;
            }
            int open = draws.get(i)[position - 1], close = draws.get(end - 1)[position - 1];
            points.add(new Point(issues.get(end - 1), close, low, high, end - i, issues.get(i) + "—" + issues.get(end - 1) + "：开" + open
                    + " 收" + close + " 高" + high + " 低" + low + " 均" + fmt(sum / (end - i)), open));
        }
        return new Panel(PAGE, "第" + position + "位置号码K线（每" + per + "期一根）", "CANDLE", "draws", "最近" + draws.size() + "期",
                "号码1–10的开/收/高/低。看该位置号码区间是否偏大或偏小、波动是否收窄；它描述走势，不单独证明下一期可预测。",
                points.isEmpty() ? "EMPTY" : "READY", points, null);
    }

    private static Panel numberLine(List<int[]> draws, List<String> issues, int position) {
        List<Point> points = new ArrayList<>();
        int start = Math.max(0, draws.size() - MAX_POINTS);
        for (int i = start; i < draws.size(); i++) {
            int v = draws.get(i)[position - 1];
            points.add(new Point(issues.get(i), v, Double.NaN, Double.NaN, 1, issues.get(i) + "期第" + position + "位置开出" + v));
        }
        return new Panel(PAGE, "第" + position + "位置号码轨迹（最近120期）", "LINE", "draws", "最近" + points.size() + "期",
                "当前预测所在位置的逐期开奖号码，用来对照出号是否追着热区、是否在震荡区间内。", points.isEmpty() ? "EMPTY" : "READY", points, null);
    }

    private static Panel omissionHeat(List<int[]> draws) {
        List<Point> points = new ArrayList<>();
        if (!draws.isEmpty()) {
            for (int position = 1; position <= 10; position++) {
                for (int number = 1; number <= 10; number++) {
                    int miss = -1;
                    for (int i = draws.size() - 1; i >= 0; i--) {
                        if (draws.get(i)[position - 1] == number) {
                            miss = draws.size() - 1 - i;
                            break;
                        }
                    }
                    points.add(new Point("位置" + position + "·号码" + number, miss < 0 ? Double.NaN : miss, Double.NaN, Double.NaN, draws.size(),
                            "第" + position + "位置号码" + number + (miss < 0 ? "在窗口内未出现" : "已遗漏" + miss + "期")));
                }
            }
        }
        return new Panel(PAGE, "号码遗漏热力图（行=位置1–10，列=号码1–10）", "HEATMAP", "draws", "最近" + draws.size() + "期",
                "颜色越亮遗漏越久。点格子看具体遗漏期数；长遗漏不代表马上会出（每期独立），用于发现异常偏离。",
                points.isEmpty() ? "EMPTY" : "READY", points, null);
    }

    private static Panel hotCold(List<int[]> draws, int position) {
        List<Point> points = new ArrayList<>();
        int start = Math.max(0, draws.size() - 100), n = draws.size() - start;
        if (n > 0) {
            int[] counts = new int[11];
            for (int i = start; i < draws.size(); i++) counts[draws.get(i)[position - 1]]++;
            for (int number = 1; number <= 10; number++)
                points.add(new Point("号码" + number, counts[number] - n / 10.0, Double.NaN, Double.NaN, n,
                        "近" + n + "期出现" + counts[number] + "次，期望" + fmt(n / 10.0) + "次"));
        }
        return new Panel(PAGE, "第" + position + "位置冷热号（近100期，相对期望次数）", "BAR", "draws", "最近" + n + "期",
                "柱子向上=比期望多（热），向下=少（冷）。偏离在±5以内基本属于正常随机波动。", points.isEmpty() ? "EMPTY" : "READY", points, null);
    }

    private static Panel calibration(SQLiteDatabase db, CancellationSignal cancel) {
        int[] n = new int[10], hit = new int[10];
        double[] sumP = new double[10];
        try (Cursor c = db.rawQuery("SELECT selected_hit_p,hit_label FROM " + DbHelper.TABLE_R35_SHADOW_OBSERVATION
                + " WHERE settled=1 AND hit_label>=0 AND selected_hit_p>0 AND selected_hit_p<=1 ORDER BY target_issue DESC LIMIT 2000", null, cancel)) {
            while (c.moveToNext()) {
                double p = c.getDouble(0);
                int b = Math.min(9, (int) (p * 10));
                n[b]++;
                sumP[b] += p;
                hit[b] += c.getInt(1) == 1 ? 1 : 0;
            }
        }
        List<Point> points = new ArrayList<>();
        for (int b = 0; b < 10; b++) {
            if (n[b] == 0) continue;
            double[] w = wilson(hit[b], n[b]);
            double mean = sumP[b] / n[b];
            points.add(new Point("箱" + (b + 1), hit[b] / (double) n[b], w[0], w[1], n[b],
                    "预测概率均值" + fmt(mean) + "，实际命中" + hit[b] + "/" + n[b], Double.NaN, mean));
        }
        return new Panel(PAGE, "预测概率校准（对角线=说多少中多少）", "RELIABILITY", DbHelper.TABLE_R35_SHADOW_OBSERVATION, "最近2000次已结算",
                "点在对角线下方=软件把概率说高了，不能按显示概率下注；贴近对角线才说明概率可信。", points.isEmpty() ? "EMPTY_NEEDS_SETTLED_P" : "READY", points, null);
    }

    private static Panel mainlineWeight(SQLiteDatabase db, CancellationSignal cancel) {
        List<Point> points = new ArrayList<>();
        try (Cursor c = db.rawQuery("SELECT target_issue,candidate_name,candidate_id,applied,influence_weight,status FROM "
                + ResearchMainlineContributionAuditR922.TABLE + " ORDER BY target_issue DESC LIMIT 120", null, cancel)) {
            while (c.moveToNext()) {
                String name = c.getString(1) == null || c.getString(1).isEmpty() ? c.getString(2) : c.getString(1);
                double w = c.getInt(3) == 1 ? 100.0 * c.getDouble(4) : 0.0;
                points.add(0, new Point(c.getString(0), w, Double.NaN, Double.NaN, 1,
                        c.getString(0) + "期：" + (c.getInt(3) == 1 ? "参与算法 " + name + "，权重" + fmt(w) + "%" : "创造算法未参与（" + c.getString(5) + "）")));
            }
        }
        return new Panel(PAGE, "主链算法参与权重（%）", "LINE", ResearchMainlineContributionAuditR922.TABLE, "最近" + points.size() + "期",
                "研究出的新算法每期在主链出号里占多大权重。长期为0说明新算法还没通过证据门，出号仍是基础模型。", points.isEmpty() ? "EMPTY" : "READY", points, null);
    }

    private static Panel evolutionEvents(SQLiteDatabase db, CancellationSignal cancel) {
        LinkedHashMap<String, int[]> perIssue = new LinkedHashMap<>();
        try (Cursor c = db.rawQuery("SELECT event_issue,operation,COUNT(*) FROM research_evolution_event_v41 WHERE event_issue IN "
                + "(SELECT DISTINCT event_issue FROM research_evolution_event_v41 ORDER BY event_issue DESC LIMIT 120) GROUP BY event_issue,operation ORDER BY event_issue",
                null, cancel)) {
            while (c.moveToNext()) {
                int[] v = perIssue.get(c.getString(0));
                if (v == null) perIssue.put(c.getString(0), v = new int[1]);
                v[0] += c.getInt(2);
            }
        }
        List<Point> points = new ArrayList<>();
        for (Map.Entry<String, int[]> e : perIssue.entrySet())
            points.add(new Point(e.getKey(), e.getValue()[0], Double.NaN, Double.NaN, e.getValue()[0], e.getKey() + "期：迭代/变异/融合事件" + e.getValue()[0] + "个"));
        return new Panel(PAGE, "算法迭代进化事件（每期）", "BAR", "research_evolution_event_v41", "最近" + points.size() + "个有事件的期",
                "看迭代是否在持续跟随开奖进行。事件多不等于变强；变强要看上面的命中率曲线和下面的训练误差。", points.isEmpty() ? "EMPTY" : "READY", points, null);
    }

    private static Panel networkBrier(SQLiteDatabase db, CancellationSignal cancel) {
        List<String> issues = new ArrayList<>();
        List<double[]> rows = new ArrayList<>();
        try (Cursor c = db.rawQuery("SELECT target_issue,network_probability,hit FROM " + AtomicToolWorldR943.CONTEXT
                + " WHERE hit>=0 AND network_probability>=0 AND network_probability<=1 ORDER BY target_issue DESC LIMIT 139", null, cancel)) {
            while (c.moveToNext()) {
                issues.add(0, c.getString(0));
                rows.add(0, new double[] {c.getDouble(1), c.getInt(2)});
            }
        }
        List<Point> points = new ArrayList<>();
        int window = 20;
        for (int i = window - 1; i < rows.size(); i++) {
            double brier = 0;
            for (int j = i - window + 1; j <= i; j++) brier += Math.pow(rows.get(j)[0] - rows.get(j)[1], 2);
            brier /= window;
            points.add(new Point(issues.get(i), brier, Double.NaN, Double.NaN, window, "截至" + issues.get(i) + "近20期Brier " + fmt(brier) + "（随机基线0.21）"));
        }
        return new Panel(PAGE, "深度学习训练误差（滚动20期Brier，越低越好）", "LINE", AtomicToolWorldR943.CONTEXT, "最近" + rows.size() + "期已结算",
                "神经网络每期赛前概率的真实误差。曲线持续下降并低于0.21，才说明它在越学越准；上升说明在退化。", points.isEmpty() ? "EMPTY_NEEDS_20_SETTLED" : "READY", points, null);
    }

    private static Panel lineage(SQLiteDatabase db, CancellationSignal cancel) {
        LinkedHashSet<String> nodes = new LinkedHashSet<>();
        List<Edge> edges = new ArrayList<>();
        try (Cursor c = db.rawQuery("SELECT child_id,parent_id,parent_role FROM r974_model_lineage ORDER BY created_issue DESC LIMIT 60", null, cancel)) {
            while (c.moveToNext() && nodes.size() < MAX_POINTS - 2) {
                nodes.add(c.getString(1));
                nodes.add(c.getString(0));
                edges.add(new Edge(c.getString(1), c.getString(0), c.getString(2)));
            }
        }
        List<Point> points = new ArrayList<>();
        for (String node : nodes) points.add(new Point(node, Double.NaN, Double.NaN, Double.NaN, -1, "算法 " + node));
        return new Panel(PAGE, "算法父子谱系（最近60条）", "GRAPH", "r974_model_lineage", "最近" + edges.size() + "条",
                "箭头从父代指向子代，点节点看是谁。子代要在同一批开奖上全面不差于父代才可替换父代。", points.isEmpty() ? "EMPTY" : "READY", points, edges);
    }

    /**
     * R27.4: every R27 strategy that is frozen each period (observers = current best evolved candidates, shadows that
     * passed the holdout gate, and the two negative controls), with its real per-period record and the history replay it
     * was trained/selected on. With M strategies watched at once the best one is lucky by chance, so the bar for
     * "real edge" uses a Bonferroni-corrected lower bound.
     */
    private static Panel r27AlgorithmBoard(SQLiteDatabase db, CancellationSignal cancel) {
        List<Point> points = new ArrayList<>();
        List<Object[]> rows = new ArrayList<>();
        try (Cursor c = db.rawQuery("SELECT s.policy_hash,s.base_policy,s.spec_json,s.status,COUNT(f.target_ordinal),"
                + "SUM(CASE WHEN f.hit=1 THEN 1 ELSE 0 END),SUM(CASE WHEN f.act=1 THEN 1 ELSE 0 END),SUM(CASE WHEN f.act=1 AND f.hit=1 THEN 1 ELSE 0 END),"
                + "MIN(f.target_issue),MAX(f.target_issue) FROM r27_live_shadow s JOIN r27_live_frozen f ON f.policy_hash=s.policy_hash"
                + " AND f.status='FROZEN' AND f.hit>=0 GROUP BY s.policy_hash ORDER BY COUNT(f.target_ordinal) DESC LIMIT 60", null, cancel)) {
            while (c.moveToNext())
                rows.add(new Object[] {c.getString(0), c.getString(1), c.getString(2), c.getString(3), c.getInt(4), c.getInt(5), c.getInt(6), c.getInt(7), c.getString(8), c.getString(9)});
        }
        int m = Math.max(1, rows.size());
        double zBonf = normalQuantile(1 - 0.05 / m);
        int observers = 0, scheduled = 0;
        try (Cursor c = db.rawQuery("SELECT COUNT(*) FROM r27_live_shadow WHERE status IN ('OBSERVE','ACTIVE','CONTROL_I00','CONTROL_NULL')", null, cancel)) {
            if (c.moveToFirst()) scheduled = c.getInt(0);
        }
        for (Object[] r : rows) {
            String id = (String) r[0], base = r[1] == null || ((String) r[1]).isEmpty() ? id : (String) r[1], status = (String) r[3];
            int n = (Integer) r[4], k = (Integer) r[5], acts = (Integer) r[6], actHits = (Integer) r[7];
            if (status.startsWith("OBSERVE")) observers++;
            double[] w = wilson(k, n);
            double lowerBonf = wilsonLower(k, n, zBonf);
            String family = "", describe = "";
            try { com.ethk3.novaonepro.strategyv2.PolicySpecR27 spec = com.ethk3.novaonepro.strategyv2.PolicySpecR27.parse((String) r[2]);
                family = spec.meta == null || spec.meta.idea == null ? "" : spec.meta.idea; describe = spec.describeZh(); }
            catch (RuntimeException unparsable) { describe = "（规格无法解析）"; }
            StringBuilder replay = new StringBuilder();
            try (Cursor c = db.rawQuery("SELECT split,n_sched,n_act,hits FROM r27_replay_score WHERE policy_hash=? ORDER BY computed_at DESC LIMIT 6", new String[] {base}, cancel)) {
                java.util.HashSet<String> seen = new java.util.HashSet<>();
                while (c.moveToNext()) {
                    if (!seen.add(c.getString(0))) continue;
                    int na = c.getInt(2);
                    replay.append(replay.length() == 0 ? "" : "；").append(splitZh(c.getString(0))).append(" ").append(c.getInt(1)).append("期，出手").append(na)
                            .append(na == 0 ? "" : "，命中" + pct(c.getInt(3) / (double) na));
                }
            }
            String label = familyZh(family) + "·" + base.substring(0, Math.min(6, base.length()));
            String verdict = lowerBonf > BREAK_EVEN ? "多重比较校正后仍显著优于保本" : w[0] > BREAK_EVEN ? "单看显著，但同时观察" + m + "个算法时可能只是运气" : "仍在随机波动范围内";
            points.add(new Point(label, k / (double) n, w[0], w[1], n, label + "｜" + statusZh(status) + "\n" + describe
                    + "\n逐期实盘（" + r[8] + "—" + r[9] + "）：每期都预测，共" + n + "期，命中" + k + "期 = " + pct(k / (double) n) + "，95%区间 " + pct(w[0]) + "–" + pct(w[1])
                    + "\n其中按自身规则出手 " + acts + " 期" + (acts == 0 ? "" : "，命中 " + actHits + "（" + pct(actHits / (double) acts) + "）")
                    + "\n历史回放训练：" + (replay.length() == 0 ? "尚未记录" : replay.toString()) + "（用于训练/挑选，不作证明）"
                    + "\n同时观察" + m + "个算法，校正后下沿 " + pct(lowerBonf) + "；需高于保本 " + pct(BREAK_EVEN) + "。结论：" + verdict));
        }
        points.sort((a, b) -> Double.compare(b.value, a.value));
        String status = points.isEmpty() ? (scheduled == 0 ? "EMPTY_WAITING_FIRST_GENERATION" : "EMPTY_WAITING_FIRST_SETTLEMENT") : "READY";
        return new Panel(PAGE, "全部算法逐期实盘榜（每期冻结·开奖结算）", "BAR", "r27_live_frozen", rows.size() + "个算法有结算（其中观察" + observers + "个），当前每期跟踪" + scheduled + "个",
                "进化出的最好" + com.ethk3.novaonepro.strategyv2.RuntimeR27.OBSERVE_CAP + "个算法和通过留出检验的影子，每一期开奖前都冻结预测、开奖后结算，"
                        + "并在最多一万期历史上回放训练。柱=逐期命中率（随机30%），竖线=95%区间。同时看" + m + "个算法，最好的那个偏高是正常的；"
                        + "多重比较校正后下沿仍高于" + pct(BREAK_EVEN) + "才算真优势。观察算法不参与真实出手，资格与证书规则不变。",
                status, points, null);
    }

    private static String familyZh(String id) {
        if (id != null) for (com.ethk3.novaonepro.strategyv2.IdeaFamilyR27 f : com.ethk3.novaonepro.strategyv2.EvolutionLoopR27.FAMILIES)
            if (f.id().equals(id)) return f.nameZh();
        return id == null || id.isEmpty() ? "算法" : id;
    }

    private static String statusZh(String status) {
        switch (status) {
            case "OBSERVE": return "逐期观察中";
            case "OBSERVE_RETIRED": return "已退出前" + com.ethk3.novaonepro.strategyv2.RuntimeR27.OBSERVE_CAP + "（记录保留）";
            case "ACTIVE": return "留出检验通过·实时影子";
            case "CONTROL_I00": return "随机基线对照";
            case "CONTROL_NULL": return "打乱标签对照";
            default: return status;
        }
    }

    private static String splitZh(String split) {
        return "H_TRAIN".equals(split) ? "训练段" : "H_SELECT".equals(split) ? "挑选段" : "H_HOLDOUT".equals(split) ? "留出段" : split;
    }

    private static double wilsonLower(int k, int n, double z) {
        if (n <= 0) return Double.NaN;
        double p = k / (double) n, d = 1 + z * z / n, c = p + z * z / (2 * n), m = z * Math.sqrt(p * (1 - p) / n + z * z / (4.0 * n * n));
        return (c - m) / d;
    }

    /** Acklam's inverse normal CDF (|error| < 1.2e-9), for the Bonferroni-corrected bound. */
    private static double normalQuantile(double p) {
        if (p <= 0) return Double.NEGATIVE_INFINITY;
        if (p >= 1) return Double.POSITIVE_INFINITY;
        double[] a = {-3.969683028665376e+01, 2.209460984245205e+02, -2.759285104469687e+02, 1.383577518672690e+02, -3.066479806614716e+01, 2.506628277459239e+00};
        double[] b = {-5.447609879822406e+01, 1.615858368580409e+02, -1.556989798598866e+02, 6.680131188771972e+01, -1.328068155288572e+01};
        double[] c = {-7.784894002430293e-03, -3.223964580411365e-01, -2.400758277161838e+00, -2.549732539343734e+00, 4.374664141464968e+00, 2.938163982698783e+00};
        double[] d = {7.784695709041462e-03, 3.224671290700398e-01, 2.445134137142996e+00, 3.754408661907416e+00};
        double q, r;
        if (p < 0.02425) { q = Math.sqrt(-2 * Math.log(p)); return (((((c[0] * q + c[1]) * q + c[2]) * q + c[3]) * q + c[4]) * q + c[5]) / ((((d[0] * q + d[1]) * q + d[2]) * q + d[3]) * q + 1); }
        if (p > 1 - 0.02425) { q = Math.sqrt(-2 * Math.log(1 - p)); return -(((((c[0] * q + c[1]) * q + c[2]) * q + c[3]) * q + c[4]) * q + c[5]) / ((((d[0] * q + d[1]) * q + d[2]) * q + d[3]) * q + 1); }
        q = p - 0.5; r = q * q;
        return (((((a[0] * r + a[1]) * r + a[2]) * r + a[3]) * r + a[4]) * r + a[5]) * q / (((((b[0] * r + b[1]) * r + b[2]) * r + b[3]) * r + b[4]) * r + 1);
    }

    /** Live record of every active invented algorithm from its own frozen pre-draw rows. */
    private static Panel algorithmBoard(SQLiteDatabase db, CancellationSignal cancel) {
        List<Point> points = new ArrayList<>();
        int algorithms = 0;
        try (Cursor c = db.rawQuery("SELECT g.genome_id,g.display_name_zh,g.status,COUNT(p.target_issue),SUM(CASE WHEN p.hit_label=1 THEN 1 ELSE 0 END),g.dev_n,g.dev_hit_rate FROM "
                + DbHelper.TABLE_R908_ALGORITHM_GENOME + " g JOIN " + DbHelper.TABLE_R908_ALGORITHM_PREDRAW
                + " p ON p.genome_id=g.genome_id AND p.hit_label>=0 GROUP BY g.genome_id ORDER BY COUNT(p.target_issue) DESC LIMIT 60", null, cancel)) {
            while (c.moveToNext()) {
                algorithms++;
                int n = c.getInt(3), k = c.getInt(4);
                double[] w = wilson(k, n);
                double needed = BREAK_EVEN + 1.96 * Math.sqrt(BASELINE * (1 - BASELINE) / n);
                String name = c.getString(1) == null || c.getString(1).isEmpty() ? c.getString(0) : c.getString(1);
                String verdict = w[0] > BREAK_EVEN ? "已显著优于保本" : "仍在随机波动范围内";
                points.add(new Point(shortName(name), k / (double) n, w[0], w[1], n, name + "｜" + c.getString(2) + "\n实盘 " + k + "/" + n + " = "
                        + pct(k / (double) n) + "，95%区间 " + pct(w[0]) + "–" + pct(w[1]) + "\n在" + n + "期样本下需≥" + pct(needed) + "才算真优于保本30.3%；结论："
                        + verdict + (c.getInt(5) > 0 ? "\n挑选时的历史开发回放：" + c.getInt(5) + "期 " + pct(c.getDouble(6)) + "（用于挑选，不作证明；与实盘差距即过拟合程度）" : "\n挑选时的历史开发回放：该旧算法未登记回放期数（R908旧记录缺字段，不是0命中）")
                        + "\n120期需≥" + pct(BREAK_EVEN + 1.96 * Math.sqrt(0.21 / 120)) + "，300期需≥" + pct(BREAK_EVEN + 1.96 * Math.sqrt(0.21 / 300))));
            }
        }
        points.sort((a, b) -> Double.compare(b.value, a.value));
        return new Panel(PAGE, "旧版R908算法实盘榜（仅主链冻结过的）", "BAR", DbHelper.TABLE_R908_ALGORITHM_PREDRAW, algorithms + "个算法",
                "旧链R908：只有被主链当期选中并冻结的算法才有记录，所以期数少；全部算法的逐期成绩看上一张图。每根柱是一个算法的真实命中率，竖线是95%区间。同时观察" + algorithms
                        + "个算法时，最好的那个短期偏高是正常现象，只有区间下沿高于30.3%才是真优势。", points.isEmpty() ? "EMPTY" : "READY", points, null);
    }

    private static Panel bestAlgorithmCurve(SQLiteDatabase db, CancellationSignal cancel) {
        String best = null, name = "";
        try (Cursor c = db.rawQuery("SELECT p.genome_id,COALESCE(g.display_name_zh,p.genome_id),COUNT(*),SUM(CASE WHEN p.hit_label=1 THEN 1 ELSE 0 END) FROM "
                + DbHelper.TABLE_R908_ALGORITHM_PREDRAW + " p LEFT JOIN " + DbHelper.TABLE_R908_ALGORITHM_GENOME
                + " g ON g.genome_id=p.genome_id WHERE p.hit_label>=0 GROUP BY p.genome_id HAVING COUNT(*)>=20 ORDER BY 1.0*SUM(CASE WHEN p.hit_label=1 THEN 1 ELSE 0 END)/COUNT(*) DESC LIMIT 1",
                null, cancel)) {
            if (c.moveToFirst()) {
                best = c.getString(0);
                name = c.getString(1);
            }
        }
        List<Point> points = new ArrayList<>();
        if (best != null) {
            List<String> issues = new ArrayList<>();
            List<Integer> hits = new ArrayList<>();
            try (Cursor c = db.rawQuery("SELECT target_issue,hit_label FROM " + DbHelper.TABLE_R908_ALGORITHM_PREDRAW
                    + " WHERE genome_id=? AND hit_label>=0 ORDER BY target_issue", new String[] {best}, cancel)) {
                while (c.moveToNext()) {
                    issues.add(c.getString(0));
                    hits.add(c.getInt(1) == 1 ? 1 : 0);
                }
            }
            int step = Math.max(1, (hits.size() + MAX_POINTS - 1) / MAX_POINTS), k = 0;
            for (int i = 0; i < hits.size(); i++) {
                k += hits.get(i);
                if ((i + 1) % step != 0 && i != hits.size() - 1) continue;
                double[] w = wilson(k, i + 1);
                points.add(new Point(issues.get(i), k / (double) (i + 1), w[0], w[1], i + 1,
                        "截至" + issues.get(i) + "累计" + k + "/" + (i + 1) + "=" + pct(k / (double) (i + 1)) + "，区间下沿" + pct(w[0])));
            }
        }
        return new Panel(PAGE, "最佳算法累计命中率：" + shortName(name), "LINE", DbHelper.TABLE_R908_ALGORITHM_PREDRAW, points.isEmpty() ? "" : "共" + points.get(points.size() - 1).n + "期",
                "选出当前实盘命中率最高的算法，看它随期数增加是稳在高位还是回落到30%。区间下沿越过30.3%并保持，才说明它是真的。",
                points.isEmpty() ? "EMPTY_NEEDS_20_SETTLED" : "READY", points, null);
    }

    private static String shortName(String name) {
        if (name == null) return "";
        int dot = name.indexOf('·');
        String head = dot > 0 ? name.substring(0, dot) : name;
        return head.length() > 12 ? head.substring(0, 12) : head;
    }

    private static String pct(double v) {
        return String.format(Locale.ROOT, "%.1f%%", 100.0 * v);
    }

    private static double[] wilson(int k, int n) {
        if (n <= 0) return new double[] {Double.NaN, Double.NaN};
        double z = 1.96, p = k / (double) n, d = 1 + z * z / n, c = p + z * z / (2 * n), m = z * Math.sqrt(p * (1 - p) / n + z * z / (4.0 * n * n));
        return new double[] {(c - m) / d, (c + m) / d};
    }

    private static String fmt(double v) {
        return String.format(Locale.ROOT, "%.2f", v);
    }
}
