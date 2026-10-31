package com.ethk3.novaonepro.strategyv2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * R30-B02-01 每期实时链时间线（M03/M04/M05/M07 的唯一测量来源）。
 *
 * 每期一行 r30_issue_timeline。每个阶段只记“第一次”到达时间（先写者赢），不覆盖、不补造。
 * 墙钟 wall_* 用于跨进程对账；单调钟 mono_* + boot_id 用于同一次开机内求差（防止调时间）。
 * 跨开机的两个阶段之间不求差，记为 null（报告里显示“跨重启”），不允许用墙钟替代。
 *
 * 阶段（顺序即链路顺序）：
 *  SOURCE_PUBLISHED   源端公布时间（接口给出才填；否则永远 null，不能用首次看到冒充）
 *  FIRST_OBSERVED     本机第一次拿到这期开奖的响应
 *  VALIDATED          期号/10个号码/去重/冲突校验通过
 *  DRAW_COMMITTED     开奖写入实时表并提交
 *  SETTLED            该期所有已冻结预测正式结算提交
 *  NEXT_REQUESTED     下一期分析请求创建
 *  NEXT_PREPARED      下一期候选计算完成（事务外）
 *  NEXT_FROZEN        下一期赛前冻结提交
 *  NEXT_DEADLINE      下一期截止时间（来自期号解析器，是计划值不是测量值）
 */
public final class IssueTimelineR30 {
    public static final String VERSION = "R30_ISSUE_TIMELINE_V1";
    private IssueTimelineR30() {}

    public enum Stage { SOURCE_PUBLISHED, FIRST_OBSERVED, VALIDATED, DRAW_COMMITTED, SETTLED, NEXT_REQUESTED, NEXT_PREPARED, NEXT_FROZEN, NEXT_DEADLINE }

    public static final String DDL =
            "CREATE TABLE IF NOT EXISTS r30_issue_timeline(issue TEXT PRIMARY KEY,boot_id TEXT NOT NULL,"
            + "wall_source_published INTEGER,wall_first_observed INTEGER,wall_validated INTEGER,wall_draw_committed INTEGER,"
            + "wall_settled INTEGER,wall_next_requested INTEGER,wall_next_prepared INTEGER,wall_next_frozen INTEGER,wall_next_deadline INTEGER,"
            + "mono_first_observed INTEGER,mono_validated INTEGER,mono_draw_committed INTEGER,mono_settled INTEGER,"
            + "mono_next_requested INTEGER,mono_next_prepared INTEGER,mono_next_frozen INTEGER,"
            + "boot_first_observed TEXT,boot_validated TEXT,boot_draw_committed TEXT,boot_settled TEXT,"
            + "boot_next_requested TEXT,boot_next_prepared TEXT,boot_next_frozen TEXT,"
            + "outcome TEXT NOT NULL DEFAULT 'OPEN',updated_at INTEGER NOT NULL)";
    public static final String INDEX = "CREATE INDEX IF NOT EXISTS r30_issue_timeline_updated ON r30_issue_timeline(updated_at)";
    /** 先插入空行（不存在时）。 */
    public static final String SQL_ENSURE = "INSERT OR IGNORE INTO r30_issue_timeline(issue,boot_id,updated_at) VALUES(?,?,?)";

    /** 列名白名单（不拼接调用方字符串）。 */
    static String wallColumn(Stage s) { return "wall_" + s.name().toLowerCase(java.util.Locale.ROOT); }
    static boolean hasMono(Stage s) { return s != Stage.SOURCE_PUBLISHED && s != Stage.NEXT_DEADLINE; }

    /** 先写者赢：只在该列为 NULL 时写入。参数顺序：wall, mono, boot, now, issue。 */
    public static String sqlMark(Stage s) {
        String c = s.name().toLowerCase(java.util.Locale.ROOT);
        if (!hasMono(s)) return "UPDATE r30_issue_timeline SET wall_" + c + "=?,updated_at=? WHERE issue=? AND wall_" + c + " IS NULL";
        return "UPDATE r30_issue_timeline SET wall_" + c + "=?,mono_" + c + "=?,boot_" + c + "=?,updated_at=? WHERE issue=? AND wall_" + c + " IS NULL";
    }

    /** 绑定参数，与 {@link #sqlMark} 配套。 */
    public static Object[] markArgs(Stage s, String issue, long wall, long mono, String boot, long now) {
        if (!hasMono(s)) return new Object[] {wall, now, issue};
        return new Object[] {wall, mono, boot, now, issue};
    }

    /** 链路分段（用于P95）：from→to 都在同一 boot 内才计算。 */
    public static final class Segment {
        public final String id, metric; public final Stage from, to; public final long targetP95Ms;
        Segment(String id, String metric, Stage from, Stage to, long targetP95Ms) { this.id = id; this.metric = metric; this.from = from; this.to = to; this.targetP95Ms = targetP95Ms; }
    }
    public static final List<Segment> SEGMENTS = Arrays.asList(
            new Segment("observe_to_validate", "M04", Stage.FIRST_OBSERVED, Stage.VALIDATED, 200),
            new Segment("validate_to_commit", "M04", Stage.VALIDATED, Stage.DRAW_COMMITTED, 300),
            new Segment("observe_to_settled", "M04", Stage.FIRST_OBSERVED, Stage.SETTLED, 1000),
            new Segment("request_to_frozen", "M03", Stage.NEXT_REQUESTED, Stage.NEXT_FROZEN, 5000),
            new Segment("settled_to_frozen", "M03", Stage.SETTLED, Stage.NEXT_FROZEN, 5000),
            new Segment("prepare_to_frozen", "M07", Stage.NEXT_PREPARED, Stage.NEXT_FROZEN, 200));

    /** 一行时间线（从库读出后组装）。缺失值用 null。 */
    public static final class Row {
        public final String issue; final Long[] mono = new Long[Stage.values().length]; final String[] boot = new String[Stage.values().length];
        final Long[] wall = new Long[Stage.values().length];
        public Row(String issue) { this.issue = issue; }
        public Row set(Stage s, Long wallMs, Long monoMs, String bootId) { wall[s.ordinal()] = wallMs; mono[s.ordinal()] = monoMs; boot[s.ordinal()] = bootId; return this; }
        public Long wall(Stage s) { return wall[s.ordinal()]; }
        /** 同一次开机的单调钟差；任一缺失或跨开机返回 null。 */
        public Long elapsed(Stage from, Stage to) {
            Long a = mono[from.ordinal()], b = mono[to.ordinal()];
            String ba = boot[from.ordinal()], bb = boot[to.ordinal()];
            if (a == null || b == null || ba == null || !ba.equals(bb)) return null;
            return b - a;
        }
        /** 赛前余量：截止 − 冻结（墙钟，截止是计划值）。 */
        public Long freezeMarginMs() {
            Long d = wall[Stage.NEXT_DEADLINE.ordinal()], f = wall[Stage.NEXT_FROZEN.ordinal()];
            return d == null || f == null ? null : d - f;
        }
    }

    /** 最近窗口统计：每段 n、P50、P95、max、跨重启数；M05 赛前有效覆盖（余量≥5000ms 的期数 / 有截止的期数）。 */
    public static Map<String, Object> summarize(List<Row> rows) {
        Map<String, Object> out = new LinkedHashMap<>();
        for (Segment seg : SEGMENTS) {
            List<Long> v = new ArrayList<>();
            int crossBoot = 0, missing = 0;
            for (Row r : rows) {
                Long e = r.elapsed(seg.from, seg.to);
                if (e != null) v.add(e);
                else if (r.mono[seg.from.ordinal()] != null && r.mono[seg.to.ordinal()] != null) crossBoot++;
                else missing++;
            }
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("metric", seg.metric);
            m.put("n", v.size());
            m.put("p50", pct(v, 0.50));
            m.put("p95", pct(v, 0.95));
            m.put("max", v.isEmpty() ? null : java.util.Collections.max(v));
            m.put("targetP95", seg.targetP95Ms);
            Long p95 = pct(v, 0.95);
            m.put("pass", p95 == null ? null : (Object) (p95 <= seg.targetP95Ms));
            m.put("crossBoot", crossBoot);
            m.put("missing", missing);
            out.put(seg.id, m);
        }
        int withDeadline = 0, onTime = 0;
        for (Row r : rows) {
            if (r.wall(Stage.NEXT_DEADLINE) == null) continue;
            withDeadline++;
            Long margin = r.freezeMarginMs();
            if (margin != null && margin >= 5000L) onTime++;
        }
        Map<String, Object> m05 = new LinkedHashMap<>();
        m05.put("periodsWithDeadline", withDeadline);
        m05.put("frozenAtLeast5sBefore", onTime);
        m05.put("coverage", withDeadline == 0 ? null : (Object) (onTime / (double) withDeadline));
        out.put("M05", m05);
        return out;
    }

    /** 最近秩分位数（nearest-rank）；空集合返回 null。 */
    static Long pct(List<Long> values, double q) {
        if (values.isEmpty()) return null;
        List<Long> s = new ArrayList<>(values);
        java.util.Collections.sort(s);
        int rank = (int) Math.ceil(q * s.size());
        return s.get(Math.max(0, Math.min(s.size() - 1, rank - 1)));
    }
}
