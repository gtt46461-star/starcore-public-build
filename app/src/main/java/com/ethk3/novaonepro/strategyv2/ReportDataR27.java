package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** R27-051/052/053 report data loader (docs/10 10.3): fills ReportR27.Input from the frozen report snapshot and
 * provides data sources for the 19 chapters that were NOT_RUN in R26. Empty sources become INSUFFICIENT with the
 * missing table named; only DEVICE may remain NOT_RUN (with its reason). */
public final class ReportDataR27 {
    private ReportDataR27() {}
    public static final String DEVICE_REASON = "真机设备矩阵（docs/12 12.3：V2353A/API34 + 另一品牌；亮屏/息屏/锁屏/充电/断网/覆盖安装/强杀/低内存/24小时）尚未执行；HOST 测试不能替代。";

    static boolean table(SQLiteDatabase db, String name) { try (Cursor c = db.rawQuery("SELECT 1 FROM sqlite_master WHERE type='table' AND name=?", new String[] {name})) { return c.moveToFirst(); } }

    @SuppressWarnings("unchecked")
    public static ReportR27.Input load(SQLiteDatabase db) {
        ReportR27.Input in = new ReportR27.Input(); long now = System.currentTimeMillis();
        if (!table(db, "r27_migration")) return in;
        try (Cursor c = db.rawQuery("SELECT s.policy_hash,s.status,s.spec_json,s.base_policy,e.acts,e.hits,e.n,e.log_e_up,e.alpha,e.state,e.sum_u,e.sum_u2 FROM r27_live_shadow s LEFT JOIN r27_eprocess e ON e.policy_hash=s.policy_hash WHERE s.status IN ('ACTIVE','CONTROL_I00','CONTROL_NULL') ORDER BY s.global_index", null)) {
            while (c.moveToNext()) {
                ReportR27.Shadow s = new ReportR27.Shadow(); s.id = c.getString(0); s.status = c.getString(1);
                try { s.describe = PolicySpecR27.parse(c.getString(2)).describeZh(); } catch (RuntimeException e) { s.describe = "spec unreadable"; }
                s.acts = c.isNull(4) ? 0 : c.getLong(4); s.hits = c.isNull(5) ? 0 : c.getLong(5); s.n = c.isNull(6) ? 0 : c.getLong(6);
                s.logE = c.isNull(7) ? Double.NaN : c.getDouble(7); s.threshold = c.isNull(8) ? Double.NaN : Math.log(1 / c.getDouble(8)); s.state = c.isNull(9) ? "" : c.getString(9);
                if (!c.isNull(10) && s.n > 1) { double m = c.getDouble(10) / s.n, v = Math.max(1e-6, c.getDouble(11) / s.n - m * m); s.expectedRemaining = m <= 0 ? Double.NaN : Math.max(0, (s.threshold - s.logE) / (m * m / (2 * v))); }
                String base = c.getString(3) == null || c.getString(3).isEmpty() ? s.id : c.getString(3);
                try (Cursor r = db.rawQuery("SELECT split,hits,n_act FROM r27_replay_score WHERE policy_hash=? ORDER BY computed_at DESC", new String[] {base})) {
                    while (r.moveToNext()) { double hr = r.getInt(2) == 0 ? Double.NaN : r.getInt(1) / (double) r.getInt(2); if (r.getString(0).equals("H_SELECT") && Double.isNaN(s.replayHr)) s.replayHr = hr; if (r.getString(0).equals("H_HOLDOUT") && Double.isNaN(s.holdoutHr)) s.holdoutHr = hr; }
                }
                in.shadows.add(s);
            }
        }
        List<Double> sel = new ArrayList<>();
        try (Cursor c = db.rawQuery("SELECT r.hits,r.n_act FROM r27_replay_score r JOIN r27_policy p ON p.policy_hash=r.policy_hash WHERE r.split='H_SELECT' AND p.state IN ('ACTIVE','LIVE') AND r.n_act>0", null)) { while (c.moveToNext()) sel.add(c.getInt(0) / (double) c.getInt(1)); }
        in.replaySelectHr = new double[sel.size()]; for (int i = 0; i < sel.size(); i++) in.replaySelectHr[i] = sel.get(i);
        try (Cursor c = db.rawQuery("SELECT detail_json,champion_holdout_hr FROM r27_generation WHERE loop='MAIN' ORDER BY generation DESC LIMIT 40", null)) {
            while (c.moveToNext()) { Map<String, Object> g = (Map<String, Object>) JsonR27.parse(c.getString(0)); in.generations.add(0, g); if (in.lastGeneration == null) { in.lastGeneration = g; in.holdoutChampionHr = c.isNull(1) ? Double.NaN : c.getDouble(1); } }
        }
        try (Cursor c = db.rawQuery("SELECT payload_json FROM r27_chart_cache WHERE panel_id='report' AND key='previous_summary'", null)) { if (c.moveToFirst()) in.previous = (Map<String, Object>) JsonR27.parse(c.getString(0)); }
        try (Cursor c = db.rawQuery("SELECT CASE WHEN wait_kind='' THEN 'UNRECORDED' ELSE wait_kind END,count(*) FROM r26_task GROUP BY 1 ORDER BY 1", null)) { while (c.moveToNext()) in.waitKinds.put(c.getString(0), c.getLong(1)); } catch (RuntimeException e) { }
        for (WaitKindR27.Health h : RuntimeR27.health(db, now)) { if (h.stuck) in.stuck.add(h.taskId + ":" + h.detail); if (h.neverRun) in.neverRun.add(h.taskId); }
        double cpu = 0, wall = 0;
        try (Cursor c = db.rawQuery("SELECT cpu_ms,wall_ms FROM r27_thread_cpu WHERE session_id=(SELECT session_id FROM r27_thread_cpu ORDER BY updated_at DESC LIMIT 1)", null)) { while (c.moveToNext()) { cpu += c.getLong(0); wall += c.getLong(1); in.researchThreads++; } }
        in.researchCpuShare = wall <= 0 ? Double.NaN : cpu / wall;
        in.writeWaitsOver200msHour = ChartCacheR27.count(db, "SELECT coalesce(sum(waits_over_200ms),0) FROM r27_write_wait WHERE hour_start>" + (now - 3_600_000L));
        in.freezeScheduled = ChartCacheR27.count(db, "SELECT coalesce(sum(scheduled),0) FROM r27_opportunity WHERE recorded_at>" + (now - 86_400_000L));
        in.freezeMissed = ChartCacheR27.count(db, "SELECT coalesce(sum(deadline_missed),0) FROM r27_opportunity WHERE recorded_at>" + (now - 86_400_000L));
        in.generationsDay = ChartCacheR27.count(db, "SELECT count(*) FROM r27_generation WHERE loop='MAIN' AND finished_at>" + (now - 86_400_000L));
        in.replayP95Ms = in.lastGeneration == null ? null : in.lastGeneration.get("candidateReplayP95Ms");
        try (Cursor c = db.rawQuery("SELECT policy_hash,old_build,new_build,result,first_diff_issue,first_diff_field,checked_at FROM r27_abi_continuity ORDER BY checked_at DESC LIMIT 64", null)) {
            while (c.moveToNext()) { Map<String, Object> r = new LinkedHashMap<>(); r.put("policy", c.getString(0)); r.put("oldBuild", c.getString(1)); r.put("newBuild", c.getString(2)); r.put("result", c.getString(3)); r.put("firstDiffIssue", c.getString(4)); r.put("firstDiffField", c.getString(5)); in.abi.add(r); }
        }
        in.pendingAbiChecks = ChartCacheR27.count(db, "SELECT count(*) FROM r27_live_shadow s WHERE s.status IN ('ACTIVE','CONTROL_I00','CONTROL_NULL') AND NOT EXISTS(SELECT 1 FROM r27_semantic_fingerprint f WHERE f.policy_hash=s.policy_hash AND f.build='" + BuildConfig.SOURCE_COMMIT_R26 + "')");
        in.r26ArchivedBySourceAbi = ChartCacheR27.count(db, "SELECT count(*) FROM r26_live_model WHERE status='BLOCKED_NUMERICAL_ABI_PRESERVED'");
        in.legacyBlocked = ChartCacheR27.count(db, "SELECT count(*) FROM r26_legacy_runtime WHERE status='BLOCKED_NUMERICAL_ABI_PRESERVED'");
        return in;
    }

    /** Summary of the previous exported report (set by the export service from files/r27/last_report_summary.json). */
    public static volatile Map<String, Object> previousSummary;

    public static List<Map<String, Object>> chapters(SQLiteDatabase db) {
        ReportR27.Input in = load(db); if (in.previous == null) in.previous = previousSummary; List<Map<String, Object>> out = new ArrayList<>();
        out.add(ReportR27.truth(in)); out.add(ReportR27.evolution(in)); out.add(ReportR27.runtime(in)); out.add(ReportR27.upgrade(in));
        return out;
    }

    /** Data source for the 19 R26 chapters that had none. Null = no SQL source (handled by the caller). */
    public static String query(String id) {
        switch (id) {
            case "IDENTITY": case "BUILD_IDENTITY": case "SUPPLY_CHAIN": return "SELECT key,payload_json,updated_at FROM r27_chart_cache WHERE panel_id='asset' AND key='build-identity' UNION ALL SELECT 'migration',revision||'@'||source_commit,applied_at FROM r27_migration";
            case "REQUIREMENTS": return "SELECT key,payload_json,updated_at FROM r27_chart_cache WHERE panel_id='asset' AND key='r27_cards'";
            case "SOURCE": return "SELECT key,payload_json,updated_at FROM r27_chart_cache WHERE panel_id='asset' AND key='source-audit'";
            case "MERGE": return "SELECT key,payload_json,updated_at FROM r27_chart_cache WHERE panel_id='asset' AND key='merge-status'";
            case "DATA": return "SELECT count(*) AS rows,count(DISTINCT issue) AS issues,sum(dirty<>0) AS dirty_rows,min(issue) AS first_issue,max(issue) AS last_issue FROM draws";
            case "AVAILABILITY": return "SELECT count(*) AS n,min(fetch_time-open_time) AS min_first_seen_ms,max(fetch_time-open_time) AS max_first_seen_ms,avg(fetch_time-open_time) AS mean_first_seen_ms FROM draws WHERE dirty=0 AND fetch_time>0 AND open_time>0";
            case "OPPORTUNITY": return "SELECT count(*) AS targets,sum(scheduled) AS n_sched,sum(frozen) AS n_frozen,sum(acted) AS n_act,sum(settled) AS n_settled,sum(deadline_missed) AS deadline_missed FROM r27_opportunity HAVING count(*)>0";
            case "FETCH": return "SELECT reason,count,last_issue,last_at FROM r27_fetch_reject ORDER BY count DESC";
            case "ATOMS": return "SELECT atom_id,state,n_select,gain_select,lcb_select,n_holdout,gain_holdout,lcb_holdout,missing_rate,derived,definition FROM r27_atom_score ORDER BY derived,atom_index";
            case "FAILURES": return "SELECT task_id,error_class,message,recorded_at FROM r27_task_error ORDER BY error_id DESC";
            case "ABLATION": return "SELECT generation,champion_hash,node_index,removed_child,branch,delta_u,delta_logloss FROM r27_ablation ORDER BY generation DESC,node_index";
            case "RANKING": return "SELECT key,payload_json,updated_at FROM r27_chart_cache WHERE panel_id='report' AND key='ranking'";
            case "ACTION_SURFACE": return "SELECT key,payload_json,updated_at FROM r27_chart_cache WHERE panel_id='report' AND key='action_surface'";
            case "OWNER": return "SELECT count(*) AS real_rows,count(DISTINCT target_issue) AS real_issues,(SELECT count(*) FROM (SELECT target_issue FROM trades WHERE is_realtime_real=1 GROUP BY target_issue HAVING count(*)>1)) AS issues_with_more_than_one_real FROM trades WHERE is_realtime_real=1";
            case "UI": return "SELECT state_key,state_value FROM app_state WHERE state_key LIKE 'r27_ui_%'";
            default: return null;
        }
    }
    public static boolean r27Sourced(String id) { return query(id) != null || id.equals("DEVICE") || id.equals("CONCLUSION"); }
}
