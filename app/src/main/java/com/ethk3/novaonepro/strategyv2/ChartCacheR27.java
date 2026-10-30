package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** R27-049 chart pre-aggregation (docs/10 10.1). The research pool writes one JSON payload per panel into
 * r27_chart_cache; the UI only reads these rows (ChartsRepositoryR27), so the chart page never runs heavy queries.
 * Every panel states its source table, sample size/range and how (not) to read it. Empty data is INSUFFICIENT,
 * never a zero line. REPLAY, HOLDOUT and FUTURE are always separate series. */
public final class ChartCacheR27 {
    private ChartCacheR27() {}
    public static final String[] PANELS = {"p01_three_scores", "p02_generation_holdout", "p03_evolution_events", "p04_seven_counters", "p05_lineage", "p06_idea_survival",
            "p07_funnel", "p08_bss", "p09_rolling200", "p10_train_holdout_loss", "p11_eprocess", "p12_mainline_weight", "p13_runtime_health", "p14_atom_scores"};

    static Map<String, Object> panel(String title, String kind, String source, String window, String note, List<Object> points) {
        Map<String, Object> m = new LinkedHashMap<>(); m.put("title", title); m.put("kind", kind); m.put("source", source); m.put("window", window); m.put("note", note);
        m.put("status", points.isEmpty() ? "INSUFFICIENT" : "READY"); m.put("points", points); return m;
    }
    static Map<String, Object> pt(String label, Object value, Object lower, Object upper, long n, String details) {
        Map<String, Object> m = new LinkedHashMap<>(); m.put("label", label); m.put("value", value); m.put("lower", lower); m.put("upper", upper); m.put("n", n); m.put("details", details); return m;
    }
    static Object f(double d) { return Double.isFinite(d) ? (Object) d : null; }
    static String pct(double d) { return Double.isFinite(d) ? String.format(java.util.Locale.ROOT, "%.1f%%", 100 * d) : "—"; }

    public static ResearchWriteQueueR27.Batch build(SQLiteDatabase db, RuntimeR27 rt) {
        ResearchWriteQueueR27.Batch b = new ResearchWriteQueueR27.Batch(); long now = System.currentTimeMillis();
        List<Map<String, Object>> panels = new ArrayList<>();
        panels.add(safe("三类成绩并列（每个实时影子策略）", () -> threeScores(db)));
        panels.add(safe("每代冠军留出命中率：主进化 vs 打乱标签 vs 随机基线", () -> generationHoldout(db)));
        panels.add(safe("算法迭代进化事件（每代）", () -> evolutionEvents(db)));
        panels.add(safe("进化七计数累计", () -> sevenCounters(db)));
        panels.add(safe("算法父子谱系（最近60条）", () -> lineage(db)));
        panels.add(safe("思路族存活率", () -> ideaSurvival(db)));
        panels.add(safe("淘汰漏斗", () -> funnel(db)));
        panels.add(safe("深度学习图A：累计BSS与95%置信带", () -> bss(db)));
        panels.add(safe("深度学习图B：滚动200期Brier与噪声带", () -> rolling(db)));
        panels.add(safe("深度学习图C：训练损失与留出损失", () -> trainHoldout(db)));
        panels.add(safe("序贯检验进度（log e / 阈值）", () -> eprocess(db)));
        panels.add(safe("主链算法参与权重", () -> mainline(db)));
        panels.add(safe("研究运行健康", () -> health(db, rt)));
        panels.add(safe("原子回放评分分布", () -> atoms(db)));
        try { b.add("INSERT OR REPLACE INTO r27_chart_cache(panel_id,key,payload_json,updated_at) VALUES('report','ranking',?,?)", JsonR27.write(ranking(db)), now); } catch (RuntimeException e) { }
        try { b.add("INSERT OR REPLACE INTO r27_chart_cache(panel_id,key,payload_json,updated_at) VALUES('report','action_surface',?,?)", JsonR27.write(actionSurface(db)), now); } catch (RuntimeException e) { }
        for (int i = 0; i < panels.size(); i++) b.add("INSERT OR REPLACE INTO r27_chart_cache(panel_id,key,payload_json,updated_at) VALUES(?,?,?,?)", PANELS[i], "panel", JsonR27.write(panels.get(i)), now);
        return b;
    }
    interface Builder { Map<String, Object> build(); }
    static Map<String, Object> safe(String title, Builder bld) {
        try { CooperativeContinuationR26.poll();Map<String, Object> m = bld.build(); m.put("title", title); return m; }
        catch (RuntimeException e) { List<Object> none = new ArrayList<>(); Map<String, Object> m = panel(title, "STATUS", "", "", "读取失败：" + e.getClass().getSimpleName() + "；不按0处理。", none); m.put("status", "READ_ERROR"); return m; }
    }

    static Map<String, Object> threeScores(SQLiteDatabase db) {
        List<Object> pts = new ArrayList<>(); int k = 0; double nMean = 0;
        try (Cursor c = db.rawQuery("SELECT s.policy_hash,s.spec_json,s.status,s.base_policy FROM r27_live_shadow s WHERE s.status IN ('ACTIVE','CONTROL_I00','CONTROL_NULL') ORDER BY s.global_index LIMIT 40", null)) {
            while (c.moveToNext()) {CooperativeContinuationR26.poll();
                String id = c.getString(0), base = c.getString(3); double rep = Double.NaN, hold = Double.NaN; long fn = 0, fh = 0;
                try (Cursor r = db.rawQuery("SELECT split,hits,n_act FROM r27_replay_score WHERE policy_hash=? ORDER BY computed_at DESC", new String[] {base.isEmpty() ? id : base})) {
                    while (r.moveToNext()) {CooperativeContinuationR26.poll(); double hr = r.getInt(2) == 0 ? Double.NaN : r.getInt(1) / (double) r.getInt(2); if (r.getString(0).equals("H_SELECT") && Double.isNaN(rep)) rep = hr; if (r.getString(0).equals("H_HOLDOUT") && Double.isNaN(hold)) hold = hr; }
                }
                try (Cursor r = db.rawQuery("SELECT count(*),coalesce(sum(hit),0) FROM r27_live_frozen WHERE policy_hash=? AND status='FROZEN' AND act=1 AND hit>=0", new String[] {id})) { if (r.moveToFirst()) { fn = r.getLong(0); fh = r.getLong(1); } }
                double fut = fn == 0 ? Double.NaN : fh / (double) fn; double[] w = StatsR27.wilson(fh, fn, StatsR27.Z95); nMean += fn; k++;
                Map<String, Object> p = pt(c.getString(2).startsWith("CONTROL") ? c.getString(2) : "S" + id.substring(0, 6), f(fut), fn == 0 ? null : (Object) w[0], fn == 0 ? null : (Object) w[1], fn,
                        "FUTURE " + pct(fut) + "（N=" + fn + "，Wilson " + pct(w[0]) + "—" + pct(w[1]) + "）；REPLAY(H_SELECT) " + pct(rep) + "；HOLDOUT " + pct(hold) + (fn < 120 ? "；样本不足" : "") + "\n" + PolicySpecR27.parse(c.getString(1)).describeZh());
                p.put("open", f(rep)); p.put("expected", f(hold)); pts.add(p);
            }
        }
        double luck = k == 0 ? Double.NaN : StatsR27.luckCeiling((int) Math.max(1, nMean / k), k, 27, 400);
        Map<String, Object> m = panel("", "TRIPLE", "r27_replay_score, r27_live_frozen, r27_eprocess", pts.size() + "个影子/对照", "三根柱：REPLAY(H_SELECT) / HOLDOUT / FUTURE，竖线=FUTURE Wilson区间。灰线30%=随机基线，虚线=运气上限" + pct(luck) + "。只有FUTURE能进入资格；REPLAY高只说明拟合得好。", pts);
        List<Object> refs = new ArrayList<>(); refs.add(0.30); if (Double.isFinite(luck)) refs.add(luck); m.put("refs", refs); return m;
    }

    static Map<String, Object> generationHoldout(SQLiteDatabase db) {
        List<Object> pts = new ArrayList<>();
        try (Cursor c = db.rawQuery("SELECT generation,champion_holdout_hr,null_champion_holdout_hr,i00_champion_holdout_hr,verified FROM r27_generation WHERE loop='MAIN' ORDER BY generation DESC LIMIT 120", null)) {
            while (c.moveToNext()) {CooperativeContinuationR26.poll();
                Map<String, Object> p = pt("G" + c.getLong(0), c.isNull(1) ? null : c.getDouble(1), null, null, 1, "第" + c.getLong(0) + "代：主冠军留出 " + pct(c.isNull(1) ? Double.NaN : c.getDouble(1)) + "；打乱标签 " + pct(c.isNull(2) ? Double.NaN : c.getDouble(2)) + "；随机基线 " + pct(c.isNull(3) ? Double.NaN : c.getDouble(3)) + (c.getInt(4) == 1 ? "；VERIFIED" : "；未验证"));
                p.put("open", c.isNull(2) ? null : c.getDouble(2)); p.put("expected", c.isNull(3) ? null : c.getDouble(3)); pts.add(0, p);
            }
        }
        Map<String, Object> m = panel("", "LINE3", "r27_generation", "最近" + pts.size() + "代", "三条线：蓝=主进化冠军、橙=打乱标签对照冠军、灰=随机基线族冠军（同一留出块）。主线显著高于另两条才说明进化有效。", pts);
        m.put("series", list("主进化", "打乱标签", "随机基线")); m.put("refs", list(0.30)); return m;
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> evolutionEvents(SQLiteDatabase db) {
        List<Object> pts = new ArrayList<>();
        try (Cursor c = db.rawQuery("SELECT generation,births,rejected,no_op,duplicates,retired,survivors,verified FROM r27_generation WHERE loop='MAIN' ORDER BY generation DESC LIMIT 60", null)) {
            while (c.moveToNext()) {CooperativeContinuationR26.poll();
                Map<String, Object> p = pt("G" + c.getLong(0), (double) c.getLong(1), null, null, c.getLong(1),
                        "出生" + c.getLong(1) + "；拒绝" + c.getLong(2) + "；NO_OP" + c.getLong(3) + "；重复" + c.getLong(4) + "；淘汰" + c.getLong(5) + "；存活" + c.getLong(6) + "；晋升" + c.getInt(7));
                p.put("stack", list((double) c.getLong(2), (double) c.getLong(3), (double) c.getLong(4), (double) c.getLong(5), (double) c.getLong(6), (double) c.getInt(7))); pts.add(0, p);
            }
        }
        Map<String, Object> m = panel("", "STACK", "r27_generation", "最近" + pts.size() + "代", "每代一柱：拒绝/NO_OP/重复/淘汰/存活/晋升分色。事件多不等于变强，变强只看留出验证。", pts);
        m.put("series", list("拒绝", "NO_OP", "重复", "淘汰", "存活", "晋升")); return m;
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> sevenCounters(SQLiteDatabase db) {
        List<Object> pts = new ArrayList<>();
        try (Cursor c = db.rawQuery("SELECT generation,detail_json FROM r27_generation WHERE loop='MAIN' ORDER BY generation DESC LIMIT 120", null)) {
            while (c.moveToNext()) {CooperativeContinuationR26.poll();
                Map<String, Object> d = (Map<String, Object>) JsonR27.parse(c.getString(1)); Map<String, Object> cs = (Map<String, Object>) d.get("counters"); if (cs == null) continue;
                List<Object> v = new ArrayList<>(); StringBuilder s = new StringBuilder();
                for (String k : EvolutionLoopR27.COUNTERS) { Object x = cs.get(k); v.add(x == null ? 0.0 : ((Number) x).doubleValue()); s.append(k).append('=').append(x).append("；"); }
                long r26 = 0; Map<String, Object> p = pt("G" + c.getLong(0), ((Number) v.get(6)).doubleValue(), null, null, 1, s.toString()); p.put("stack", v); pts.add(0, p);
            }
        }
        Map<String, Object> m = panel("", "LINES", "r27_generation(+r26_evolution)", "最近" + pts.size() + "代", "attempts…verifiedImprovements 七条累计线；只有 verifiedImprovements 代表在留出数据上被证实的变强。", pts);
        m.put("series", list((Object[]) EvolutionLoopR27.COUNTERS)); return m;
    }

    static Map<String, Object> lineage(SQLiteDatabase db) {
        List<Object> pts = new ArrayList<>(), edges = new ArrayList<>(); java.util.LinkedHashSet<String> nodes = new java.util.LinkedHashSet<>();
        try (Cursor c = db.rawQuery("SELECT child_hash,parent_hash,role,operator,generation FROM r27_lineage ORDER BY created_at DESC LIMIT 60", null)) {
            while (c.moveToNext()) {CooperativeContinuationR26.poll();
                String a = c.getString(1), ch = c.getString(0); nodes.add(a); nodes.add(ch);
                Map<String, Object> e = new LinkedHashMap<>(); e.put("from", a.substring(0, Math.min(10, a.length()))); e.put("to", ch.substring(0, Math.min(10, ch.length())));
                e.put("label", (c.getString(2).equals("CHAMPION_VERIFIED") ? "VERIFIED " : "") + c.getString(2) + "/" + c.getString(3) + "/G" + c.getLong(4)); edges.add(e);
            }
        }
        for (String n : nodes) {
            String desc = n; Map<String, Object> scores = new LinkedHashMap<>();
            try (Cursor c = db.rawQuery("SELECT spec_json,state,state_reason FROM r27_policy WHERE policy_hash=?", new String[] {n})) { if (c.moveToFirst()) { try { desc = PolicySpecR27.parse(c.getString(0)).describeZh(); } catch (RuntimeException ignored) { } desc += "\n状态：" + c.getString(1) + "（" + c.getString(2) + "）"; } }
            try (Cursor c = db.rawQuery("SELECT split,hits,n_act FROM r27_replay_score WHERE policy_hash=?", new String[] {n})) { while (c.moveToNext()) scores.put(c.getString(0), c.getInt(2) == 0 ? "—" : pct(c.getInt(1) / (double) c.getInt(2)) + "/N" + c.getInt(2)); }
            long fn = 0, fh = 0; try (Cursor c = db.rawQuery("SELECT count(*),coalesce(sum(hit),0) FROM r27_live_frozen WHERE (policy_hash=? OR policy_hash IN (SELECT policy_hash FROM r27_live_shadow WHERE base_policy=? AND status NOT LIKE 'OBSERVE%')) AND status='FROZEN' AND act=1 AND hit>=0", new String[] {n, n})) { if (c.moveToFirst()) { fn = c.getLong(0); fh = c.getLong(1); } }
            pts.add(pt(n.substring(0, Math.min(10, n.length())), null, null, null, -1, desc + "\nREPLAY " + scores.getOrDefault("H_SELECT", "—") + "；HOLDOUT " + scores.getOrDefault("H_HOLDOUT", "—") + "；FUTURE " + (fn == 0 ? "—" : pct(fh / (double) fn) + "/N" + fn)));
            if (pts.size() >= 118) break;
        }
        Map<String, Object> m = panel("", "GRAPH", "r27_lineage, r27_policy, r27_replay_score", "最近" + edges.size() + "条", "箭头父→子。实线=留出验证过的冠军接替(VERIFIED)，虚线=未验证。点节点看中文描述、算子与 REPLAY/HOLDOUT/FUTURE 三列成绩。", pts);
        m.put("edges", edges); return m;
    }

    static Map<String, Object> ideaSurvival(SQLiteDatabase db) {
        List<Object> pts = new ArrayList<>();
        try (Cursor c = db.rawQuery("SELECT idea_family,count(*),sum(CASE WHEN state IN ('ACTIVE','LIVE') THEN 1 ELSE 0 END),sum(CASE WHEN state='REJECTED' THEN 1 ELSE 0 END) FROM r27_policy GROUP BY idea_family ORDER BY idea_family", null)) {
            while (c.moveToNext()) {CooperativeContinuationR26.poll(); long n = c.getLong(1), a = c.getLong(2); pts.add(pt(c.getString(0), n == 0 ? null : (Object) (a / (double) n), null, null, n, c.getString(0) + "：登记" + n + "，存活" + a + "，出生被拒" + c.getLong(3))); }
        }
        return panel("", "BAR", "r27_policy", pts.size() + "个思路族", "每个思路族在回放上存活的比例；长期为0的族说明这个方向在数据上没有可用信号。", pts);
    }

    static Map<String, Object> funnel(SQLiteDatabase db) {
        long r0 = 0, r1 = 0, r2 = 0, surv = 0;
        try (Cursor c = db.rawQuery("SELECT detail_json FROM r27_generation WHERE loop='MAIN' ORDER BY generation DESC LIMIT 1", null)) {
            if (c.moveToFirst()) { Object f = ((Map<?, ?>) JsonR27.parse(c.getString(0))).get("funnel"); if (f instanceof List && ((List<?>) f).size() == 4) { List<?> l = (List<?>) f; r0 = ((Number) l.get(0)).longValue(); r1 = ((Number) l.get(1)).longValue(); r2 = ((Number) l.get(2)).longValue(); surv = ((Number) l.get(3)).longValue(); } }
        }
        long pop = count(db, "SELECT count(*) FROM r27_policy WHERE state='ACTIVE'"), live = count(db, "SELECT count(*) FROM r27_live_shadow WHERE status='ACTIVE'"),
                qual = count(db, "SELECT count(*) FROM r27_eprocess WHERE state='" + SequentialQualificationR27.QUALIFIED + "'"), cert = count(db, "SELECT count(*) FROM r27_certificate WHERE status='ACTIVE'");
        List<Object> pts = new ArrayList<>();
        Object[][] steps = {{"R0", r0}, {"R1", r1}, {"R2", r2}, {"存活", surv}, {"种群", pop}, {"影子池", live}, {"资格", qual}, {"证书", cert}};
        if (r0 + pop + live > 0) for (Object[] s : steps) pts.add(pt((String) s[0], (double) (Long) s[1], null, null, (Long) s[1], s[0] + "：" + s[1]));
        return panel("", "BAR", "r27_generation, r27_policy, r27_live_shadow, r27_eprocess, r27_certificate", "最新一代", "R0→R1→R2→种群→影子池→资格→证书，每一级都是硬门。", pts);
    }

    static double[][] pairs(SQLiteDatabase db, String source, String model, int limit) {
        List<double[]> l = new ArrayList<>();
        try (Cursor c = db.rawQuery("SELECT p_model,hit FROM r27_brier_pair WHERE source=? AND model=? ORDER BY target_issue DESC LIMIT " + limit, new String[] {source, model})) { while (c.moveToNext()) l.add(0, new double[] {c.getDouble(0), c.getInt(1)}); }
        double[][] out = new double[2][l.size()]; for (int i = 0; i < l.size(); i++) { out[0][i] = l.get(i)[0]; out[1][i] = l.get(i)[1]; } return out;
    }
    static Map<String, Object> bss(SQLiteDatabase db) {
        double[][] f = pairs(db, "FUTURE", "ATOMNET", 20000); int[] y = new int[f[1].length]; for (int i = 0; i < y.length; i++) y[i] = (int) f[1][i];
        List<BrierSkillR27.Point> c = BrierSkillR27.cumulative(f[0], y); List<Object> pts = new ArrayList<>(); int stride = Math.max(1, c.size() / 110);
        for (int i = 0; i < c.size(); i += stride) { BrierSkillR27.Point p = c.get(i); Map<String, Object> m = pt("#" + (p.index + 1), p.bss, p.low, p.high, p.index + 1, "累计BSS " + p.bss + "；95%带 " + p.low + "—" + p.high + "；EWMA " + p.ewma); pts.add(m); }
        Map<String, Object> m = panel("", "LINE", "r27_brier_pair(FUTURE, ATOMNET)", "FUTURE样本 " + f[0].length + (f[0].length < BrierSkillR27.MIN_N ? "（<200：样本不足，不画线）" : ""), "BSS=1−Brier模型/Brier(常数0.30)。置信带整体在0以上才说明比“随便猜30%”更准；含0=还看不出区别；在0以下=更差。结论：" + BrierSkillR27.verdict(c), pts);
        m.put("refs", list(0.0)); return m;
    }
    static Map<String, Object> rolling(SQLiteDatabase db) {
        double[][] f = pairs(db, "FUTURE", "ATOMNET", 20000); String src = "FUTURE";
        if (f[0].length < BrierSkillR27.WINDOW) { f = pairs(db, "REPLAY", "ATOMNET", 5000); src = "REPLAY(留出块，非FUTURE)"; }
        int[] y = new int[f[1].length]; for (int i = 0; i < y.length; i++) y[i] = (int) f[1][i];
        List<double[]> r = BrierSkillR27.rolling(f[0], y, Math.max(1, (f[0].length - 199) / 100), 27); List<Object> pts = new ArrayList<>();
        for (double[] x : r) pts.add(pt("#" + (long) (x[0] + 1), x[1], x[2], x[3], 200, "滚动200期Brier " + x[1] + "；无预测能力模型95%波动带 " + x[2] + "—" + x[3]));
        return panel("", "LINE", "r27_brier_pair(" + src + ")", "样本" + f[0].length, "竖线=同样概率下“无预测能力”时的95%波动带。曲线落在带内=波动只是噪声；超出带子才有意义。旧“滚动20期”图降级为短期波动参考。", pts);
    }
    static Map<String, Object> trainHoldout(SQLiteDatabase db) {
        List<Object> pts = new ArrayList<>(); String run = null;
        try (Cursor c = db.rawQuery("SELECT run_id FROM r27_atomnet_epoch ORDER BY recorded_at DESC LIMIT 1", null)) { if (c.moveToFirst()) run = c.getString(0); }
        if (run != null) try (Cursor c = db.rawQuery("SELECT epoch,split,logloss FROM r27_atomnet_epoch WHERE run_id=? ORDER BY epoch,split", new String[] {run})) {
            Map<Long, double[]> by = new java.util.TreeMap<>();
            while (c.moveToNext()) {CooperativeContinuationR26.poll(); double[] v = by.computeIfAbsent(c.getLong(0), k -> new double[] {Double.NaN, Double.NaN, Double.NaN}); String s = c.getString(1); v[s.equals("H_TRAIN") ? 0 : s.equals("H_SELECT") ? 1 : 2] = c.getDouble(2); }
            for (Map.Entry<Long, double[]> e : by.entrySet()) { double[] v = e.getValue(); boolean over = Double.isFinite(v[0]) && Double.isFinite(v[1]) && v[1] - v[0] > 0.02;
                Map<String, Object> p = pt("E" + e.getKey(), f(v[0]), null, null, 1, "训练 " + v[0] + "；挑选(H_SELECT) " + v[1] + "；留出 " + v[2] + (over ? "；差值>0.02 过拟合" : "")); p.put("stack", list(f(v[0]), f(v[1]), f(v[2]))); if (over) p.put("flag", "RED"); pts.add(p); }
        }
        Map<String, Object> m = panel("", "LINES", "r27_atomnet_epoch", run == null ? "尚未预训练" : "run " + run, "训练损失降、留出不降=过拟合（差值>0.02标红）。", pts); m.put("series", list("训练", "挑选", "留出")); return m;
    }
    static Map<String, Object> eprocess(SQLiteDatabase db) {
        List<Object> pts = new ArrayList<>();
        try (Cursor c = db.rawQuery("SELECT e.policy_hash,e.log_e_up,e.log_e_down,e.alpha,e.n,e.acts,e.hits,e.state,s.status FROM r27_eprocess e JOIN r27_live_shadow s ON s.policy_hash=e.policy_hash ORDER BY e.global_index DESC LIMIT 60", null)) {
            while (c.moveToNext()) {CooperativeContinuationR26.poll(); double th = Math.log(1 / c.getDouble(3)); pts.add(0, pt(c.getString(0).substring(0, 8), c.getDouble(1) / th, -c.getDouble(2) / th, null, c.getLong(4), "log e(上)=" + c.getDouble(1) + "，阈值 ln(1/α)=" + th + "；log e(下)=" + c.getDouble(2) + "；FUTURE " + c.getLong(6) + "/" + c.getLong(5) + "；状态 " + c.getString(7) + "（" + c.getString(8) + "）")); }
        }
        Map<String, Object> m = panel("", "BAR", "r27_eprocess", pts.size() + "个", "到达1.0（上阈值）=进入资格；下方负值到达−1.0=判定不优于目标并退出影子池。按 e 值排序，不按少样本命中率排序。", pts);
        m.put("refs", list(1.0, -1.0)); return m;
    }
    static Map<String, Object> mainline(SQLiteDatabase db) {
        List<Object> pts = new ArrayList<>();
        try (Cursor c = db.rawQuery("SELECT target_issue,candidate_name,candidate_id,applied,influence_weight,status FROM " + ResearchMainlineContributionAuditR922.TABLE + " ORDER BY target_issue DESC LIMIT 120", null)) {
            while (c.moveToNext()) {CooperativeContinuationR26.poll(); double wgt = c.getInt(3) == 1 ? 100.0 * c.getDouble(4) : 0.0; pts.add(0, pt(c.getString(0), wgt, null, null, 1, c.getString(0) + "期：" + (c.getInt(3) == 1 ? "参与 " + c.getString(1) : "未参与（" + c.getString(5) + "）"))); }
        } catch (RuntimeException missingLegacyTable) { }
        Map<String, Object> m = panel("", "LINE", ResearchMainlineContributionAuditR922.TABLE, "最近" + pts.size() + "期", "长期为0说明没有合格策略（允许为0）。主链规则不变：唯一授权 DecisionGateEngine。", pts);
        if (pts.isEmpty()) m.put("status", "EMPTY_ALLOWED_ZERO"); return m;
    }
    static Map<String, Object> health(SQLiteDatabase db, RuntimeR27 rt) {
        List<Object> pts = new ArrayList<>(); long now = System.currentTimeMillis();
        double cpu = 0, wall = 0; int threads = 0;
        try (Cursor c = db.rawQuery("SELECT thread_name,cpu_ms,wall_ms FROM r27_thread_cpu WHERE updated_at>?", new String[] {String.valueOf(now - 120_000)})) { while (c.moveToNext()) {CooperativeContinuationR26.poll(); cpu += c.getLong(1); wall += c.getLong(2); threads++; } }
        try (Cursor c = db.rawQuery("SELECT cpu_ms,wall_ms FROM r26_service_total ORDER BY rowid DESC LIMIT 1", null)) { if (c.moveToFirst()) { cpu += c.getLong(0); wall += c.getLong(1); threads++; } } catch (RuntimeException ignored) { }
        double share = wall <= 0 ? Double.NaN : cpu / wall;
        int stuck = 0, never = 0; for (WaitKindR27.Health h : RuntimeR27.health(db, now)) { if (h.stuck) stuck++; if (h.neverRun) never++; }
        long gens = count(db, "SELECT count(*) FROM r27_generation WHERE loop='MAIN' AND finished_at>" + (now - 86_400_000L));
        Object p95 = null; try (Cursor c = db.rawQuery("SELECT detail_json FROM r27_generation WHERE loop='MAIN' ORDER BY generation DESC LIMIT 1", null)) { if (c.moveToFirst()) p95 = ((Map<?, ?>) JsonR27.parse(c.getString(0))).get("candidateReplayP95Ms"); }
        long waits = count(db, "SELECT coalesce(sum(waits_over_200ms),0) FROM r27_write_wait WHERE hour_start>" + (now - 3_600_000L));
        long freezeAll = count(db, "SELECT coalesce(sum(scheduled),0) FROM r27_opportunity WHERE recorded_at>" + (now - 86_400_000L)), missed = count(db, "SELECT coalesce(sum(deadline_missed),0) FROM r27_opportunity WHERE recorded_at>" + (now - 86_400_000L));
        pts.add(pt("CPU/线程", f(threads == 0 ? Double.NaN : share), null, null, threads, "研究线程CPU/墙钟（" + threads + "个线程，最近2分钟）=" + pct(share) + "；目标≥50%×线程数"));
        pts.add(pt("卡住", (double) stuck, null, null, stuck, "卡住>30分钟任务数=" + stuck));
        pts.add(pt("从未运行", (double) never, null, null, never, "注册60分钟仍游标0的任务数=" + never));
        pts.add(pt("代/24h", (double) gens, null, null, gens, "最近24小时完成代数=" + gens + "（目标≥100）"));
        pts.add(pt("回放P95ms", p95 instanceof Number ? (Object) ((Number) p95).doubleValue() : null, null, null, -1, "新候选完整回放 P95=" + p95 + "ms（目标≤10000）"));
        pts.add(pt("写等待>200ms/h", (double) waits, null, null, waits, "最近1小时写等待>200ms次数=" + waits + "（目标≤5）"));
        pts.add(pt("冻结缺失率", f(freezeAll == 0 ? Double.NaN : missed / (double) freezeAll), null, null, freezeAll, "24小时 DEADLINE_MISSED " + missed + "/" + freezeAll));
        return panel("", "BAR", "r27_thread_cpu, r26_service_total, r26_task, r27_generation, r27_write_wait, r27_opportunity", "实时", "研究CPU占比、卡住/从未运行任务、每日代数、回放吞吐、写等待、冻结准时率。", pts);
    }
    static Map<String, Object> atoms(SQLiteDatabase db) {
        List<Object> pts = new ArrayList<>();
        try (Cursor c = db.rawQuery("SELECT state,count(*),min(n_select),max(n_select) FROM r27_atom_score GROUP BY state ORDER BY state", null)) {
            while (c.moveToNext()) pts.add(pt(c.getString(0), (double) c.getLong(1), null, null, c.getLong(1), c.getString(0) + "：" + c.getLong(1) + "项；回放样本 " + c.getLong(2) + "—" + c.getLong(3)));
        }
        return panel("", "BAR", "r27_atom_score", "165项可评分原子+派生", "USEFUL=挑选段与留出段下界都>0；NEUTRAL=区间含0；HARMFUL=上界<0；MASKED=缺失>10%；DERIVED=派生原子。", pts);
    }

    static long count(SQLiteDatabase db, String sql) { try (Cursor c = db.rawQuery(sql, null)) { return c.moveToFirst() ? c.getLong(0) : 0; } catch (RuntimeException e) { return 0; } }
    static List<Object> list(Object... xs) { List<Object> l = new ArrayList<>(); for (Object x : xs) l.add(x); return l; }

    /** R27-053 RANKING: FUTURE hit rate of the Rank1/2/3 numbers of each shadow's frozen surface. */
    static Map<String, Object> ranking(SQLiteDatabase db) {
        Map<String, Object> out = new LinkedHashMap<>(); List<Object> rows = new ArrayList<>();
        try (Cursor c = db.rawQuery("SELECT policy_hash FROM r27_live_shadow WHERE status IN ('ACTIVE','CONTROL_I00','CONTROL_NULL')", null)) {
            while (c.moveToNext()) {CooperativeContinuationR26.poll();
                String id = c.getString(0); long[] hit = new long[3]; long n = 0;
                try (Cursor f = db.rawQuery("SELECT f.position,f.probabilities,d.n1,d.n2,d.n3,d.n4,d.n5,d.n6,d.n7,d.n8,d.n9,d.n10 FROM r27_live_frozen f JOIN draws d ON d.issue=f.target_issue AND d.dirty=0 WHERE f.policy_hash=? AND f.status='FROZEN' ORDER BY f.target_ordinal DESC LIMIT 2000", new String[] {id})) {
                    while (f.moveToNext()) {CooperativeContinuationR26.poll();
                        double[] p = StoreR27.unpack(f.getBlob(1)); int pos = f.getInt(0), actual = f.getInt(1 + pos);
                        int[] r = MatrixConsistencyR27.rank3(MatrixConsistencyR27.surface(pos, p), pos);
                        for (int k = 0; k < 3; k++) if (r[k] == actual) hit[k]++; n++;
                    }
                }
                Map<String, Object> row = new LinkedHashMap<>(); row.put("policy", id); row.put("n", n);
                for (int k = 0; k < 3; k++) row.put("rank" + (k + 1) + "HitRate", n == 0 ? null : (Object) (hit[k] / (double) n));
                row.put("randomExpectation", 0.10); rows.add(row);
            }
        }
        out.put("rows", rows); out.put("source", "r27_live_frozen ⨝ draws (FUTURE only)"); return out;
    }

    /** R27-053 ACTION_SURFACE: 100-surface / 1200-action consistency of the latest frozen rows + freeze completeness. */
    static Map<String, Object> actionSurface(SQLiteDatabase db) {
        Map<String, Object> out = new LinkedHashMap<>(); double worst = 0; long checked = 0;
        try (Cursor c = db.rawQuery("SELECT position,probabilities FROM r27_live_frozen WHERE status='FROZEN' ORDER BY target_ordinal DESC LIMIT 500", null)) {
            while (c.moveToNext()) {CooperativeContinuationR26.poll(); double[][] sf = MatrixConsistencyR27.surface(c.getInt(0), StoreR27.unpack(c.getBlob(1))); worst = Math.max(worst, MatrixConsistencyR27.error(sf, MatrixConsistencyR27.actions(sf))); checked++; }
        }
        long sched = count(db, "SELECT coalesce(sum(scheduled),0) FROM r27_opportunity"), frozen = count(db, "SELECT coalesce(sum(frozen),0) FROM r27_opportunity");
        out.put("checkedSurfaces", checked); out.put("maxAbsError", checked == 0 ? null : (Object) worst); out.put("tolerance", MatrixConsistencyR27.TOLERANCE);
        out.put("withinTolerance", checked == 0 ? null : (Object) (worst < MatrixConsistencyR27.TOLERANCE)); out.put("freezeCompleteness", sched == 0 ? null : (Object) (frozen / (double) sched));
        return out;
    }
}
