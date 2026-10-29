package com.ethk3.novaonepro.strategyv2;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** R27-051/052 report chapters built automatically from data (docs/10 10.3). Pure: the Android loader
 * (ReportDataR27) fills {@link Input}; tests feed synthetic inputs (H17). Status vocabulary: OBSERVED (data
 * present, never "PASS"), INSUFFICIENT (empty or too small, with what is missing), NOT_RUN (only DEVICE).
 * The R27-01 conclusion is chosen by the data among exactly three templates; it is never written by hand. */
public final class ReportR27 {
    private ReportR27() {}
    public static final String SCHEMA = "R27_REPORT_1";
    public static final String T_QUALIFIED = "存在 FUTURE 序贯检验通过的策略", T_NONE = "未发现在 FUTURE 上显著优于随机的策略", T_INSUFFICIENT = "样本不足";
    public static final int MIN_DECISION_PERIODS = 720;

    /** One live shadow / control as seen by the report. */
    public static final class Shadow {
        public String id, status, describe; public long acts, hits, n; public double logE = Double.NaN, threshold = Double.NaN, expectedRemaining = Double.NaN; public String state = "";
        public double replayHr = Double.NaN, holdoutHr = Double.NaN;
        public double hr() { return acts == 0 ? Double.NaN : hits / (double) acts; }
    }
    public static final class Input {
        public final List<Shadow> shadows = new ArrayList<>();
        public double[] replaySelectHr = new double[0]; public double holdoutChampionHr = Double.NaN;
        public Map<String, Object> previous; // summary of the previous report, or null
        public List<Map<String, Object>> generations = new ArrayList<>(); public Map<String, Object> lastGeneration;
        public Map<String, Long> waitKinds = new LinkedHashMap<>(); public List<String> stuck = new ArrayList<>(), neverRun = new ArrayList<>();
        public double researchCpuShare = Double.NaN; public long researchThreads, writeWaitsOver200msHour, freezeScheduled, freezeMissed, generationsDay = -1; public Object replayP95Ms;
        public List<Map<String, Object>> abi = new ArrayList<>(); public long pendingAbiChecks, r26ArchivedBySourceAbi, legacyBlocked;
        /** R27.4 observers: per-period FUTURE record of the best evolved candidates (no e-process, never qualifies). */
        public final List<Map<String, Object>> observers = new ArrayList<>(); public long observersTracking;
    }

    static Object f(double d) { return Double.isFinite(d) ? (Object) d : null; }
    static String pct(double d) { return Double.isFinite(d) ? String.format(Locale.ROOT, "%.2f%%", 100 * d) : "—"; }
    static Map<String, Object> chapter(String id, String title, String status, String reason) {
        Map<String, Object> m = new LinkedHashMap<>(); m.put("sectionId", id); m.put("title", title); m.put("schemaVersion", SCHEMA); m.put("status", status); m.put("reason", reason); m.put("evidenceLevel", "ANDROID_DATABASE_SNAPSHOT"); return m;
    }

    // ------------------------------------------------------------------ R27-01 胜率真相与差距
    public static Map<String, Object> truth(Input in) {
        List<Shadow> cand = new ArrayList<>(), controls = new ArrayList<>();
        for (Shadow s : in.shadows) (s.status != null && s.status.startsWith("CONTROL") ? controls : cand).add(s);
        cand.sort((a, b) -> Double.compare(Double.isNaN(b.logE) ? -1e9 : b.logE, Double.isNaN(a.logE) ? -1e9 : a.logE));
        List<Object> rows = new ArrayList<>(); long maxN = 0, totalActs = 0; double best = Double.NaN; int qualified = 0; Shadow top = null;
        for (Shadow s : cand) {
            double[] w = StatsR27.wilson(s.hits, s.acts, StatsR27.Z95);
            Map<String, Object> r = new LinkedHashMap<>(); r.put("policy", s.id); r.put("futureHitRate", f(s.hr())); r.put("futureActs", s.acts); r.put("futurePeriods", s.n);
            r.put("wilsonLow", s.acts == 0 ? null : (Object) w[0]); r.put("wilsonHigh", s.acts == 0 ? null : (Object) w[1]); r.put("logE", f(s.logE)); r.put("threshold", f(s.threshold)); r.put("sequentialState", s.state);
            r.put("replayHitRate", f(s.replayHr)); r.put("holdoutHitRate", f(s.holdoutHr)); r.put("expectedRemainingPeriods", f(s.expectedRemaining)); r.put("describe", s.describe); rows.add(r);
            maxN = Math.max(maxN, s.n); totalActs += s.acts; if (SequentialQualificationR27.QUALIFIED.equals(s.state)) qualified++;
            if (s.acts >= 1 && (top == null || s.hr() > best)) { best = s.hr(); top = s; }
        }
        List<Object> ctl = new ArrayList<>();
        for (Shadow s : controls) { Map<String, Object> r = new LinkedHashMap<>(); r.put("control", s.status); r.put("policy", s.id); r.put("futureHitRate", f(s.hr())); r.put("futureActs", s.acts); r.put("futurePeriods", s.n); ctl.add(r); }
        int k = cand.size(); long meanActs = k == 0 ? 0 : totalActs / k;
        double luck = k == 0 || meanActs == 0 ? Double.NaN : StatsR27.luckCeiling((int) meanActs, k, 27, 400);
        double[] q = quantiles(in.replaySelectHr);
        String template, text;
        if (k == 0 || maxN < MIN_DECISION_PERIODS) {
            template = T_INSUFFICIENT;
            double need = Double.NaN; for (Shadow s : cand) if (Double.isFinite(s.expectedRemaining)) need = Double.isNaN(need) ? s.expectedRemaining : Math.min(need, s.expectedRemaining);
            text = T_INSUFFICIENT + "：FUTURE 已结算 " + maxN + " 期（最多的影子），需约 " + (Double.isFinite(need) ? String.format(Locale.ROOT, "%.0f", need) : String.valueOf(Math.max(0, MIN_DECISION_PERIODS - maxN)) + "+") + " 期才能判断（按当前效应估计；无优势时按序贯检验反向判退）。";
        } else if (qualified > 0) {
            template = T_QUALIFIED; text = T_QUALIFIED + "：" + qualified + " 个（e 值越过阈值）。是否授证另需质量门（34%/250/ECE/最差块/连挂/语义ABI/优于负对照）。";
        } else {
            template = T_NONE; text = T_NONE + "；排行榜最高者 " + pct(best) + (Double.isFinite(luck) ? (best <= luck ? " 低于/接近" : " 高于") + "运气上限 " + pct(luck) : "") + "（" + k + " 个策略，平均出手 " + meanActs + " 次）。";
        }
        Map<String, Object> ch = chapter("R27_01_WIN_RATE_TRUTH", "胜率真相与差距", k == 0 ? "INSUFFICIENT" : "OBSERVED", k == 0 ? "实时影子池为空：尚无 FUTURE 样本；REPLAY/HOLDOUT 成绩不代表未来。" : "数据自动生成");
        ch.put("futureShadows", rows); ch.put("sameperiodControls", ctl); ch.put("luckCeiling", f(luck)); ch.put("luckCeilingInputs", "k=" + k + ",meanActs=" + meanActs);
        Map<String, Object> rep = new LinkedHashMap<>(); rep.put("survivorsSelectHitRateQuantiles", list(f(q[0]), f(q[1]), f(q[2]), f(q[3]), f(q[4]))); rep.put("n", (long) in.replaySelectHr.length); rep.put("holdoutChampionHitRate", f(in.holdoutChampionHr));
        rep.put("note", "REPLAY 成绩只说明在看过的数据上拟合得好，不进入资格。"); ch.put("replay", rep);
        Map<String, Object> obs = new LinkedHashMap<>(); long obsPeriods = 0, obsHits = 0;
        for (Map<String, Object> o : in.observers) { obsPeriods += ((Number) o.get("periods")).longValue(); obsHits += ((Number) o.get("hits")).longValue(); }
        obs.put("tracking", in.observersTracking); obs.put("withSettlement", (long) in.observers.size()); obs.put("settledPeriodsTotal", obsPeriods);
        obs.put("pooledHitRate", obsPeriods == 0 ? null : (Object) (obsHits / (double) obsPeriods)); obs.put("rows", new ArrayList<Object>(in.observers));
        obs.put("note", "观察池=每代 H_SELECT 最好的候选，每期赛前冻结、开奖后结算；不建 e 过程、不占 α、不授证、不进主链。只用于看清每个算法的真实逐期成绩。");
        ch.put("observers", obs);
        ch.put("conclusionTemplate", template); ch.put("conclusion", text);
        ch.put("comparisonWithPrevious", compare(in.previous, maxN, best, qualified));
        ch.put("summary", summary(maxN, best, qualified, k));
        return ch;
    }
    static Map<String, Object> summary(long maxN, double best, int qualified, int k) { Map<String, Object> m = new LinkedHashMap<>(); m.put("maxFuturePeriods", maxN); m.put("bestFutureHr", f(best)); m.put("qualified", (long) qualified); m.put("shadows", (long) k); return m; }
    static Map<String, Object> compare(Map<String, Object> prev, long maxN, double best, int qualified) {
        Map<String, Object> m = new LinkedHashMap<>();
        if (prev == null) { m.put("status", "INSUFFICIENT"); m.put("reason", "没有上一份报告的摘要"); return m; }
        m.put("previous", prev); m.put("maxFuturePeriodsDelta", maxN - num(prev.get("maxFuturePeriods")));
        Object pb = prev.get("bestFutureHr");
        if (pb instanceof Number && Double.isFinite(best)) {
            double d = best - ((Number) pb).doubleValue(), se = Math.sqrt(0.21 / Math.max(1, maxN)) * 1.96;
            m.put("bestHitRateDelta", d); m.put("beyondNoise", Math.abs(d) > se); m.put("noiseHalfWidth", se);
        }
        m.put("qualifiedDelta", qualified - num(prev.get("qualified")));
        return m;
    }
    static long num(Object o) { return o instanceof Number ? ((Number) o).longValue() : 0; }
    static double[] quantiles(double[] x) {
        double[] y = x.clone(); java.util.Arrays.sort(y); double[] q = {0.05, 0.25, 0.5, 0.75, 0.95}, out = new double[5];
        for (int i = 0; i < 5; i++) out[i] = y.length == 0 ? Double.NaN : y[Math.min(y.length - 1, (int) Math.floor(q[i] * (y.length - 1) + 0.5))];
        return out;
    }

    // ------------------------------------------------------------------ R27-02 进化真实性
    @SuppressWarnings("unchecked")
    public static Map<String, Object> evolution(Input in) {
        Map<String, Object> ch = chapter("R27_02_EVOLUTION_TRUTH", "进化真实性", in.generations.isEmpty() ? "INSUFFICIENT" : "OBSERVED", in.generations.isEmpty() ? "尚无完成的进化代（r27_generation 为空）" : "数据自动生成");
        Map<String, Object> last = in.lastGeneration;
        ch.put("sevenCounters", last == null ? null : last.get("counters"));
        List<Object> lines = new ArrayList<>(); long verified = 0, noop = 0, births = 0;
        for (Map<String, Object> g : in.generations) {
            Map<String, Object> l = new LinkedHashMap<>(); l.put("generation", g.get("generation")); Map<String, Object> h = (Map<String, Object>) g.get("holdout");
            l.put("main", h == null ? null : hr(h.get("championHoldout"))); l.put("shuffledLabelControl", h == null ? null : hr(h.get("nullHoldout"))); l.put("i00RandomBaseline", h == null ? null : hr(h.get("i00Holdout")));
            l.put("verified", h != null && Boolean.TRUE.equals(h.get("verified"))); l.put("reason", h == null ? null : h.get("reason")); lines.add(l);
            if (Boolean.TRUE.equals(l.get("verified"))) verified++; noop += num(g.get("noOp")); births += num(g.get("births"));
        }
        ch.put("championLines", lines); ch.put("verifiedGenerations", verified);
        ch.put("stagnationDiagnosis", last == null ? null : last.get("diagnosis")); ch.put("familySurvivors", last == null ? null : last.get("familySurvivors"));
        ch.put("rejectReasons", last == null ? null : last.get("rejectReasons")); ch.put("noOpRatio", births == 0 ? null : (Object) (noop / (double) births));
        ch.put("reading", "verifiedImprovements 才代表变强；训练段/挑选段上升在打乱标签对照中同样会出现（docs/01 B/C）。");
        return ch;
    }
    @SuppressWarnings("unchecked") static Object hr(Object o) { return o instanceof Map ? ((Map<String, Object>) o).get("hitRate") : null; }

    // ------------------------------------------------------------------ R27-03 持续运行与饥饿
    public static Map<String, Object> runtime(Input in) {
        boolean any = !in.waitKinds.isEmpty();
        Map<String, Object> ch = chapter("R27_03_CONTINUOUS_RUNTIME", "持续运行与饥饿", any ? "OBSERVED" : "INSUFFICIENT", any ? "数据自动生成" : "r26_task 没有 wait_kind 记录（R27 运行时尚未执行切片）");
        ch.put("researchCpuShare", f(in.researchCpuShare)); ch.put("researchThreads", in.researchThreads); ch.put("cpuTarget", "≥50% × 线程数（真机60分钟）");
        ch.put("waitKindDistribution", new LinkedHashMap<String, Object>(in.waitKinds)); ch.put("stuckTasks", new ArrayList<Object>(in.stuck)); ch.put("neverRunTasks", new ArrayList<Object>(in.neverRun));
        ch.put("writeWaitsOver200msLastHour", in.writeWaitsOver200msHour); ch.put("freezeScheduled24h", in.freezeScheduled); ch.put("deadlineMissed24h", in.freezeMissed);
        ch.put("freezeOnTimeRate", in.freezeScheduled == 0 ? null : (Object) (1 - in.freezeMissed / (double) in.freezeScheduled));
        ch.put("generationsLast24h", in.generationsDay < 0 ? null : (Object) in.generationsDay); ch.put("candidateReplayP95Ms", in.replayP95Ms);
        return ch;
    }

    // ------------------------------------------------------------------ R27-04 升级连续性
    public static Map<String, Object> upgrade(Input in) {
        Map<String, Object> ch = chapter("R27_04_UPGRADE_CONTINUITY", "升级连续性", in.abi.isEmpty() && in.pendingAbiChecks == 0 ? "INSUFFICIENT" : "OBSERVED",
                in.abi.isEmpty() ? "尚无跨版本的语义等价记录（首次安装或检查未完成）" : "数据自动生成");
        long eq = 0, chg = 0; for (Map<String, Object> r : in.abi) { if ("EQUIVALENT".equals(r.get("result"))) eq++; else if ("CHANGED".equals(r.get("result"))) chg++; }
        ch.put("equivalent", eq); ch.put("changed", chg); ch.put("rows", new ArrayList<Object>(in.abi)); ch.put("pendingChecks", in.pendingAbiChecks);
        ch.put("legacyRuntimeBlockedByNumericalAbi", in.legacyBlocked); ch.put("r26LiveModelsArchivedBySourceAbi", in.r26ArchivedBySourceAbi);
        ch.put("scope", "R27 影子按语义指纹继承 FUTURE/e 过程；R26 遗留运行时按语义检查 ABI_REBIRTH 继续（不再永久阻塞）；R26 live_model 的身份仍含源码ABI（单列计数）。");
        return ch;
    }

    /** Readable Markdown rendering (R27_REPORT.md). */
    @SuppressWarnings("unchecked")
    public static String markdown(List<Map<String, Object>> chapters) {
        StringBuilder b = new StringBuilder("# STARCORE R27 检测报告（自动生成）\n\n空数据只显示 INSUFFICIENT；本报告不含人工撰写的通过结论，所有结论由数据自动选择。\n");
        for (Map<String, Object> c : chapters) {
            b.append("\n## ").append(c.get("sectionId")).append(" ").append(c.get("title")).append("\n\n状态：").append(c.get("status")).append("（").append(c.get("reason")).append("）\n\n");
            if (c.containsKey("conclusion")) b.append("**结论（数据自动选择）**：").append(c.get("conclusion")).append("\n\n");
            Map<String, Object> rest = new LinkedHashMap<>(c); for (String k : new String[] {"sectionId", "title", "status", "reason", "conclusion", "schemaVersion", "evidenceLevel"}) rest.remove(k);
            b.append("```json\n").append(JsonR27.write(rest)).append("\n```\n");
        }
        return b.toString();
    }
    static List<Object> list(Object... xs) { List<Object> l = new ArrayList<>(); for (Object x : xs) l.add(x); return l; }
}
