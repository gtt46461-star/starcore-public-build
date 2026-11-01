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
        in.observersTracking = ChartCacheR27.count(db, "SELECT count(*) FROM r27_live_shadow WHERE status='" + RuntimeR27.OBSERVE + "'");
        // R29-003: complete census and independent units (not just the 60 displayed observers).
        try (Cursor c = db.rawQuery("SELECT status,count(*) FROM r27_live_shadow GROUP BY status ORDER BY status", null)) { while (c.moveToNext()) in.shadowStatusCensus.put(c.getString(0), c.getLong(1)); }
        try (Cursor c = db.rawQuery("SELECT count(DISTINCT f.policy_hash),count(*),coalesce(sum(f.hit=1),0),count(DISTINCT f.target_issue),coalesce(sum(f.act=1),0),coalesce(sum(f.act=1 AND f.hit=1),0),count(DISTINCT CASE WHEN f.act=1 THEN f.target_issue END) FROM r27_live_frozen f JOIN r27_live_shadow s ON s.policy_hash=f.policy_hash WHERE f.status='FROZEN' AND f.hit>=0 AND s.status LIKE 'OBSERVE%'", null)) {
            if (c.moveToFirst()) { in.observerStrategiesTotal = c.getLong(0); in.observerRowsTotal = c.getLong(1); in.observerHitsTotal = c.getLong(2); in.observerDistinctIssues = c.getLong(3); in.observerActRowsTotal = c.getLong(4); in.observerActHitsTotal = c.getLong(5); in.observerActDistinctIssues = c.getLong(6); }
        }
        try (Cursor c = db.rawQuery("SELECT s.policy_hash,s.base_policy,s.status,s.spec_json,count(*),sum(f.hit=1),sum(f.act=1),sum(f.act=1 AND f.hit=1),min(f.target_issue),max(f.target_issue) FROM r27_live_shadow s JOIN r27_live_frozen f ON f.policy_hash=s.policy_hash AND f.status='FROZEN' AND f.hit>=0 WHERE s.status LIKE 'OBSERVE%' GROUP BY s.policy_hash ORDER BY count(*) DESC LIMIT 60", null)) {
            while (c.moveToNext()) {
                Map<String, Object> o = new LinkedHashMap<>(); long n = c.getLong(4), k = c.getLong(5), a = c.getLong(6), ak = c.getLong(7);
                double[] w = StatsR27.wilson(k, n, StatsR27.Z95);
                o.put("observer", c.getString(0)); o.put("policy", c.getString(1)); o.put("status", c.getString(2)); o.put("periods", n); o.put("hits", k);
                o.put("hitRate", n == 0 ? null : (Object) (k / (double) n)); o.put("wilsonLow", n == 0 ? null : (Object) w[0]); o.put("wilsonHigh", n == 0 ? null : (Object) w[1]);
                o.put("acts", a); o.put("actHits", ak); o.put("from", c.getString(8)); o.put("to", c.getString(9));
                try { o.put("describe", PolicySpecR27.parse(c.getString(3)).describeZh()); } catch (RuntimeException e) { o.put("describe", "spec unreadable"); }
                in.observers.add(o);
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

    public static Map<String,Object> capabilityInventoryR30(SQLiteDatabase db){return CapabilityRegistryStoreR30.report(db);}

    public static List<Map<String,Object>> chaptersR30(SQLiteDatabase db){return ReportR29.chapters(db);}
    /** Summary candidates become previous evidence only after the verified export is published. */
    public static Map<String,Object> successfulSummaryR30(ReportR27.R30Input in){
        Map<String,Object> out=new LinkedHashMap<>();out.put("schemaVersion","R30_REPAIR_SUCCESSFUL_SUMMARY_1");out.put("definitionVersion",MetricDefinitionsR29.CURRENT_VERSION);
        out.put("sourceCommit",in.watermark.get("sourceCommit"));out.put("jobId",in.watermark.get("reportJobId"));out.put("snapshotSha256",snapshotHashR30(in));
        List<Object> objects=new ArrayList<>();for(Map<String,Object> metric:in.metricResults){Map<String,Object> row=new LinkedHashMap<>();for(String key:new String[]{"metricId","role","population","value","numerator","denominator","sampleCount","status","unit"})row.put(key,metric.get(key));row.put("objectId",metric.get("metricId"));row.put("objectKind","METRIC");row.put("counterScope","PERSISTED_SNAPSHOT_OR_EXPLICIT_MEASUREMENT");objects.add(row);}
        Map<String,Object> index=MetricEvaluatorR29.map(in.data.get("objectIndex"));for(String category:new String[]{"legacyCharts","researchCharts","matrices","capabilities"}){
            Object rows=MetricEvaluatorR29.map(index.get(category)).get("objects");if(rows instanceof List)for(Object raw:(List<?>)rows){Map<String,Object> row=new LinkedHashMap<>();Map<String,Object> object=MetricEvaluatorR29.map(raw);for(String key:new String[]{"objectId","objectKind","implemented","callable","inputAvailable","produced","consumed","effective","retainedCount"})row.put(key,object.get(key));row.put("role","STATIC_BINDING_AND_PERSISTED_ARTIFACT");objects.add(row);}
        }out.put("objects",objects);return ReportR27.freezeMap(out);
    }
    static Object snapshotHashR30(ReportR27.R30Input in){Map<String,Object> source=MetricEvaluatorR29.map(in.watermark.get("sourceManifest"));for(String key:new String[]{"snapshotSha256","snapshotSha","snapshot_sha256","snapshotHash"})if(source.get(key)!=null)return source.get(key);return null;}
    public static Map<String,Object> compareSuccessfulR30(ReportR27.R30Input in){return compareSuccessfulR30(successfulSummaryR30(in),MetricEvaluatorR29.map(in.data.get("previousSuccessfulReport")));}
    public static Map<String,Object> compareSuccessfulR30(Map<String,Object> current,Map<String,Object> previous){
        Map<String,Object> out=new LinkedHashMap<>();out.put("schemaVersion","R30_REPAIR_COMPARISON_1");out.put("currentJobId",current.get("jobId"));out.put("previousJobId",previous.get("jobId"));out.put("independentPeriodsAdded",0);
        String blocked=previous.isEmpty()?"NO_PREVIOUS_SUCCESSFUL_REPORT":current.get("jobId")==null||previous.get("jobId")==null?"UNKNOWN_JOB_ID":current.get("jobId").equals(previous.get("jobId"))?"DUPLICATE_JOB_NOT_NEW_PERIOD":!java.util.Objects.equals(current.get("sourceCommit"),previous.get("sourceCommit"))||!java.util.Objects.equals(current.get("definitionVersion"),previous.get("definitionVersion"))?"INCOMPARABLE_SOURCE_OR_DEFINITION_VERSION":current.get("snapshotSha256")==null||previous.get("snapshotSha256")==null?"UNKNOWN_SNAPSHOT_IDENTITY":current.get("snapshotSha256").equals(previous.get("snapshotSha256"))?"SAME_SNAPSHOT_NOT_NEW_PERIOD":null;
        out.put("periodUnit","DISTINCT_SUCCESSFUL_REPORT_SNAPSHOT_NOT_INDEPENDENT_DRAW_OR_QUALITY_SAMPLE");
        if(blocked!=null){out.put("status",blocked);out.put("action","只比较同源码/同定义/同对象/同角色的两个已成功发布且身份不同的快照；重复B不算第三时段");return out;}
        Map<String,Map<String,Object>> prior=new LinkedHashMap<>();Object old=previous.get("objects");if(old instanceof List)for(Object x:(List<?>)old){Map<String,Object> row=MetricEvaluatorR29.map(x);prior.put(compareKeyR30(row),row);}
        List<Object> changes=new ArrayList<>();Object now=current.get("objects");if(now instanceof List)for(Object x:(List<?>)now){Map<String,Object> row=MetricEvaluatorR29.map(x),before=prior.get(compareKeyR30(row));if(before==null)continue;
            Map<String,Object> change=new LinkedHashMap<>();change.put("objectId",row.get("objectId"));change.put("objectKind",row.get("objectKind"));change.put("role",row.get("role"));change.put("previous",before);change.put("current",row);
            boolean comparable=java.util.Objects.equals(row.get("population"),before.get("population"))&&java.util.Objects.equals(row.get("unit"),before.get("unit"))&&!"PROCESS_LIFETIME".equals(row.get("counterScope"))&&!"PROCESS_LIFETIME".equals(before.get("counterScope"));
            Number a=MetricEvaluatorR29.number(before.get("value")),b=MetricEvaluatorR29.number(row.get("value"));change.put("delta",comparable&&a!=null&&b!=null?b.doubleValue()-a.doubleValue():null);change.put("deltaMeaning","描述性快照变化；窗口分母/来源见原件，不能推断趋势或质量改善");changes.add(change);
        }out.put("status","COMPARABLE_TWO_SUCCESSFUL_SNAPSHOTS");out.put("independentPeriodsAdded",1);out.put("objects",changes);out.put("productEffectClaim",false);return out;
    }
    static String compareKeyR30(Map<String,Object> row){return row.get("objectKind")+"|"+row.get("objectId")+"|"+row.get("role");}
    /** Preserved R27 attachment API. The primary R30 report uses chaptersR30 and its detached read view. */
    public static List<Map<String, Object>> chapters(SQLiteDatabase db) {
        ReportR27.Input in = load(db); if (in.previous == null) in.previous = previousSummary; List<Map<String, Object>> out = new ArrayList<>();
        out.add(ReportR27.truth(in)); out.add(ReportR27.evolution(in)); out.add(ReportR27.runtime(in)); out.add(ReportR27.upgrade(in));
        out.add(ReportR29.chapter(db));
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
            case "ATOMS": return "SELECT s.atom_id,s.state,s.n_select,s.gain_select,s.lcb_select,s.n_holdout,s.gain_holdout,s.lcb_holdout,s.missing_rate,s.derived,s.definition,c.n_periods AS contribution_periods,c.mean_gain AS paired_brier_gain,c.lcb95 AS paired_brier_lcb95_one_sided FROM r27_atom_score s LEFT JOIN r30_atom_contribution c ON c.atom_id=s.atom_id AND c.dataset_hash=s.dataset_hash ORDER BY s.derived,s.atom_index";
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

