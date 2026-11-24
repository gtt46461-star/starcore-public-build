package com.ethk3.novaonepro.strategyv2;

import com.ethk3.novaonepro.TargetIssueResolver;
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
    public enum EventRole { LIVE, BACKFILL, REPLAY, UNKNOWN }
    public enum EventKind { DRAW, PREDICTION }

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
    public static final String EVENT_TABLE="r27_chart_cache",EVENT_NAMESPACE="r30-timeline-r1:";
    public static final String EVENT_RAW_PANEL=EVENT_NAMESPACE+"RAW",EVENT_DRAW_PANEL=EVENT_NAMESPACE+"DRAW",EVENT_PRED_PANEL=EVENT_NAMESPACE+"PRED",EVENT_HEAD_PANEL=EVENT_NAMESPACE+"HEAD";
    public static final String SQL_ENSURE="INSERT OR IGNORE INTO r30_issue_timeline(issue,boot_id,updated_at) VALUES(?,?,?)";
    /** Called only by the original SchemaR27 writer; no new telemetry physical table. */
    public static void ensureEventSchema(android.database.sqlite.SQLiteDatabase db){
        if(!db.inTransaction())throw new IllegalStateException("R30_TIMELINE_SCHEMA_WRITER_REQUIRED");
        db.execSQL(DDL);db.execSQL(INDEX);db.execSQL("CREATE INDEX IF NOT EXISTS r30_timeline_cache_window ON r27_chart_cache(panel_id,updated_at,key)");
    }

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
            return b<a?null:b-a;
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
            int crossBoot = 0, missing = 0, negative = 0;
            for (Row r : rows) {
                Long e = r.elapsed(seg.from, seg.to);
                if (e != null) v.add(e);
                else if (r.mono[seg.from.ordinal()] != null && r.mono[seg.to.ordinal()] != null) {if(java.util.Objects.equals(r.boot[seg.from.ordinal()],r.boot[seg.to.ordinal()])&&r.boot[seg.from.ordinal()]!=null)negative++;else crossBoot++;}
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
            m.put("negativeMonotonicDifference",negative);
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

    /** Reporting view over every closed target between two independently supplied calendar anchors. */
    static Map<String, Object> closedOpportunityTiming(String firstBase, String lastOpened, List<Object> events) {
        Map<String, Object> out = new LinkedHashMap<>();
        int count = TargetIssueResolver.distance(firstBase, lastOpened);
        out.put("anchorBase", firstBase);
        out.put("lastOpened", lastOpened);
        if (count < 0 || count > 5000) {
            out.put("status", "NOT_MEASURABLE_CALENDAR_BOUNDS");
            return out;
        }
        Map<String, Map<String, Object>> draws = new LinkedHashMap<>();
        Map<String, List<Map<String, Object>>> freezes = new LinkedHashMap<>();
        for (Object value : events) {
            Map<String, Object> row = MetricEvaluatorR29.map(value);
            if (!"LIVE".equals(row.get("event_role")) || "EVENT_IDENTITY_CONFLICT".equals(row.get("outcome"))) continue;
            String issue = String.valueOf(row.get("issue"));
            if (!TargetIssueResolver.isValid(issue)) continue;
            if ("DRAW".equals(row.get("event_kind")) && Boolean.TRUE.equals(row.get("canonical_draw_head"))) {
                draws.putIfAbsent(issue, row);
            } else if ("PREDICTION".equals(row.get("event_kind"))) {
                freezes.computeIfAbsent(issue, ignored -> new ArrayList<>()).add(row);
            }
        }
        List<Object> rows = new ArrayList<>();
        TimingCounts firstToSettle = new TimingCounts(1000L);
        TimingCounts committedToFreeze = new TimingCounts(5000L);
        TimingCounts publicationToSettle = new TimingCounts(1000L);
        TimingCounts publicationToFreeze = new TimingCounts(5000L);
        List<Long> margins = new ArrayList<>();
        int marginPass = 0;
        String base = firstBase;
        for (int i = 0; i < count; i++) {
            String target = TargetIssueResolver.next(base).targetIssue;
            Map<String, Object> previous = draws.getOrDefault(base, java.util.Collections.emptyMap());
            Map<String, Object> draw = draws.getOrDefault(target, java.util.Collections.emptyMap());
            Map<String, Object> freeze = sameSourceFreeze(freezes.get(target), previous, base);
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("base", base);
            row.put("target", target);
            row.put("drawEvent", draw.get("event_id"));
            row.put("freezeEvent", freeze.get("event_id"));
            row.put("drawCommitToFreezeMs", committedToFreeze.record(elapsed(previous, "draw_durable_mono",
                    "draw_durable_boot", freeze, "mono_next_frozen", "boot_next_frozen")));
            row.put("receivedToSettleMs", firstToSettle.record(elapsed(draw, "mono_first_observed",
                    "boot_first_observed", draw, "mono_settled", "boot_settled")));
            row.put("publicationToFreezeMs", publicationToFreeze.record(sincePublished(previous, freeze, "next_frozen")));
            row.put("publicationToSettleMs", publicationToSettle.record(sincePublished(draw, draw, "settled")));
            Long deadline = numeric(freeze, "wall_next_deadline"), frozen = numeric(freeze, "wall_next_frozen");
            Long margin = deadline == null || frozen == null ? null : deadline - frozen;
            if (margin != null) {
                margins.add(margin);
                if (margin >= 5000L) marginPass++;
            }
            row.put("deadlineMarginMs", margin);
            row.put("freezeState", frozen != null ? "RETURN_OBSERVED"
                    : freeze.isEmpty() ? "MISSING_UNIQUE_FREEZE_FROM_EXACT_BASE" : "UNKNOWN_NO_COMMIT_RETURN");
            rows.add(row);
            base = target;
        }
        out.put("status", "MEASURED_WITH_EXPLICIT_UNKNOWN_ROWS");
        out.put("opportunities", count);
        out.put("nextPendingTarget", TargetIssueResolver.next(lastOpened).targetIssue);
        out.put("coverageFrozenAtLeast5sBefore", count == 0 ? null : marginPass / (double) count);
        out.put("marginMeasurable", margins.size());
        out.put("marginAtLeast5s", marginPass);
        out.put("marginMissing", count - margins.size());
        out.put("drawCommitToFreeze", committedToFreeze.summary(count));
        out.put("receivedToSettlement", firstToSettle.summary(count));
        out.put("publicationToFreeze", publicationToFreeze.summary(count));
        out.put("publicationToSettlement", publicationToSettle.summary(count));
        out.put("rows", rows);
        return out;
    }

    private static Map<String, Object> sameSourceFreeze(List<Map<String, Object>> candidates,
            Map<String, Object> previousDraw, String base) {
        Object hash = previousDraw.get("draw_hash");
        Map<String, Object> found = java.util.Collections.emptyMap();
        if (candidates == null || hash == null) return found;
        for (Map<String, Object> candidate : candidates) {
            if (!hash.equals(candidate.get("source_draw_hash")) || !base.equals(candidate.get("source_issue"))) continue;
            if (numeric(candidate, "precommit_wall_next_frozen") == null) continue;
            if (!found.isEmpty() && !java.util.Objects.equals(found.get("event_id"), candidate.get("event_id"))) {
                return java.util.Collections.emptyMap();
            }
            found = candidate;
        }
        return found;
    }

    private static Long numeric(Map<String, Object> row, String column) {
        Object value = row.get(column);
        return value instanceof Number ? ((Number) value).longValue() : null;
    }

    private static Long elapsed(Map<String, Object> first, String from, String fromBoot,
            Map<String, Object> last, String to, String toBoot) {
        Long a = numeric(first, from), b = numeric(last, to);
        Object boot = first.get(fromBoot);
        return a == null || b == null || a < 0 || b < a || boot == null || !boot.equals(last.get(toBoot)) ? null : b - a;
    }

    private static Long sincePublished(Map<String, Object> draw, Map<String, Object> end, String stage) {
        Long source = numeric(draw, "wall_source_published"), receiveWall = numeric(draw, "wall_first_observed");
        Long localElapsed = elapsed(draw, "mono_first_observed", "boot_first_observed", end,
                "mono_" + stage, "boot_" + stage);
        if (source == null || receiveWall == null || localElapsed == null || source <= 0L || receiveWall < source) return null;
        return receiveWall - source + localElapsed;
    }

    private static final class TimingCounts {
        private final long limit;
        private final List<Long> measures = new ArrayList<>();
        private int passed;

        TimingCounts(long limit) { this.limit = limit; }

        Long record(Long value) {
            if (value != null) {
                measures.add(value);
                if (value <= limit) passed++;
            }
            return value;
        }

        Map<String, Object> summary(int count) {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("limitMs", limit);
            result.put("passed", passed);
            result.put("overLimit", measures.size() - passed);
            result.put("notMeasurable", count - measures.size());
            result.put("p50", pct(measures, .50));
            result.put("p95", pct(measures, .95));
            result.put("max", measures.isEmpty() ? null : java.util.Collections.max(measures));
            return result;
        }
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
