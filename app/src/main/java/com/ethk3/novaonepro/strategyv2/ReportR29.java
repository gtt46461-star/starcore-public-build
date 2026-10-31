package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.BuildConfig;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Read-only engineering diagnosis is available before natural confirmation has enough samples. */
public final class ReportR29 {
    private ReportR29() {}
    public static Map<String,Object> chapter(SQLiteDatabase db) {
        Map<String,Object> out=new LinkedHashMap<>(), snapshot=new LinkedHashMap<>();
        snapshot.put("sourceCommit",BuildConfig.SOURCE_COMMIT_R26); snapshot.put("capturedAt",System.currentTimeMillis());
        snapshot.put("timeRange",timeRange(db)); snapshot.put("session",null); snapshot.put("planCommit",BaselineIdentityR29.PLAN_COMMIT);
        Map<String,Object> baseline=cached(db,"baseline-identity"), preservation=cached(db,"legacy-preservation");
        out.put("sectionId","R29_12_ACCEPTANCE_GAPS"); out.put("title","R29.1 实施、运行与验收差距"); out.put("schemaVersion","R29_REPORT_1");
        out.put("status","PARTIAL"); out.put("overall","IMPLEMENTATION_IN_PROGRESS"); out.put("evidenceLevel","ANDROID_DATABASE_SNAPSHOT");
        out.put("reason","52卡/44指标逐项验收；缺少回执不能作为0或PASS。设备长测和未来质量独立验收。");
        out.put("baselineIdentity",baseline); out.put("legacyPreservation",preservation);
        Map<String,Object> qualification=QualificationInboxR29.diagnostics(db);
        out.put("settlementQualification",qualification);
        out.put("findings",FindingsR29.fromQualification(qualification));
        Map<String,Object> model = modelCheckpoint(db);
        out.put("modelContinuation", model);
        out.put("runtimeHealth", cachedPanel(db, "health", "summary"));
        out.put("writerHoldR30", writerHold(db, System.currentTimeMillis() - 24L * 3600_000L));
        out.put("issueTimelineR30", issueTimeline(db));
        Map<String,Object> fetch = new LinkedHashMap<>(); // R30-B02-02 (sub-card B02-02-a): current process values
        fetch.put("rejectedConcurrent", com.ethk3.novaonepro.AutoFetchService.FETCH_R30.rejectedConcurrent());
        fetch.put("consecutiveFailures", com.ethk3.novaonepro.AutoFetchService.FETCH_R30.consecutiveFailures());
        fetch.put("inFlight", com.ethk3.novaonepro.AutoFetchService.FETCH_R30.inFlight());
        fetch.put("scope", "本进程自启动以来的计数；进程重启归零，不是持久化统计");
        out.put("fetchBackoffR30", fetch);
        Map<String,MetricEvaluatorR29.Probe> probes=probes(baseline,preservation);
        probes.putAll(qualificationProbes(qualification));
        if ("RECORDED".equals(model.get("status"))) {
            List<String> refs = new ArrayList<>(); refs.add("r29_model_checkpoint:ATOMNET_FTRL"); refs.add("R29ModelArtifactTest");
            probes.put("M20", new MetricEvaluatorR29.Probe("PARTIAL", "AtomNet/Adam/FTRL/RNG/replay完整续训状态已保存；主机逐字节续训测试与Android杀进程恢复分别验收，后者NOT_RUN",
                    "EXISTING_ROTATING_POSITION_ATOMNET_FTRL", null, null, null, 1, refs));
        }
        out.put("metricResults",MetricEvaluatorR29.evaluate(probes,snapshot));
        return out;
    }

    /** Probes from measured snapshot records only. Pure (host-testable): a missing record leaves the metric NOT_RUN. */
    public static Map<String,MetricEvaluatorR29.Probe> probes(Map<String,Object> baseline, Map<String,Object> preservation) {
        Map<String,MetricEvaluatorR29.Probe> probes=new LinkedHashMap<>();
        if (baseline!=null) {
            boolean measured=BaselineIdentityR29.consistent(baseline);
            List<String> ev=new ArrayList<>(); ev.add("r27_chart_cache:r29/baseline-identity");
            probes.put("M01",new MetricEvaluatorR29.Probe(measured?"PARTIAL":"FAIL",
                    measured?"设备实测源码提交、安装包SHA-256与签名证书已记录；需与CI回执delivery.json逐项互证后才可PASS":"身份字段缺失："+baseline.get("installedApkReason")+"/"+baseline.get("signingCertificateReason"),
                    "INSTALLED_PACKAGE",null,null,null,1,ev));
        }
        if (preservation!=null && preservation.get("requirements") instanceof Number) {
            long total=num(preservation.get("requirements")), missing=num(preservation.get("missingStatusOwnerOrEvidencePath")), unfounded=num(preservation.get("completionClaimsWithoutReceipt")), receipts=num(preservation.get("regressionReceipts"));
            boolean complete=Boolean.TRUE.equals(preservation.get("ledgerComplete"));
            List<String> ev=new ArrayList<>(); ev.add("r27_chart_cache:r29/legacy-preservation"); ev.add("assets:"+LegacyContinuityR27.PRESERVATION_ASSET);
            probes.put("M02",new MetricEvaluatorR29.Probe(!complete||unfounded>0?"FAIL":"PARTIAL",
                    "记录覆盖 "+(total-missing)+"/"+total+"，无回执的完成声明 "+unfounded+"；回归复核回执 "+receipts+"/"+total+"（未复核的不计完成）",
                    "LEGACY_REQUIREMENTS_1832_AND_FILE_CARDS_299",total-missing,total,total==0?null:(Number)((total-missing)/(double)total),total,ev));
        }
        // M32: JSON and Markdown are rendered from the same chapter objects of one snapshot; the UI comparison needs a device receipt.
        List<String> ev=new ArrayList<>(); ev.add("ReportDataR27.chapters: one snapshot, one object per metric for JSON/Markdown");
        probes.put("M32",new MetricEvaluatorR29.Probe("PARTIAL","JSON/Markdown同一快照同一对象；UI同值对照需设备回执(NOT_RUN)","REPORT_RENDERINGS",null,null,null,0,ev));
        return probes;
    }
    /** M27 from the R29 contiguous qualification state; PASS needs the device receipt, so measured data is PARTIAL at best. */
    public static Map<String,MetricEvaluatorR29.Probe> qualificationProbes(Map<String,Object> q) {
        Map<String,MetricEvaluatorR29.Probe> probes=new LinkedHashMap<>();
        if (q==null || !(q.get("receipts") instanceof Number)) return probes;
        long receipts=num(q.get("receipts")), mismatch=num(q.get("receiptStateMismatch")), conflicts=num(q.get("conflictsOpen")), minus2=num(q.get("legacyHitMinus2Remaining"));
        List<String> ev=new ArrayList<>(); ev.add("r29_qual_receipt/r29_qual_cursor/r29_qual_conflict/r29_pending_draw"); ev.add("tools/r29/run_qual_host.sh Q1-Q6");
        boolean bad=mismatch>0;
        probes.put("M27",new MetricEvaluatorR29.Probe(bad?"FAIL":"PARTIAL",
                "回执 "+receipts+"，回执与e-process不一致 "+mismatch+"，未解决冲突 "+conflicts+"，遗留hit=-2 "+minus2+"，待补抓开奖 "+q.get("pendingDrawOpen")+"；设备重启/杀进程回执前不计PASS",
                "LIVE_EXPERIMENT_RECEIPTS",null,null,null,receipts,ev));
        return probes;
    }
    /** R30-B01-02 第3章 运行与时延：后台超时持有前50 + 关键写等待（来自 r30_writer_hold，近24小时）。 */
    static Map<String,Object> writerHold(SQLiteDatabase db, long since) {
        Map<String,Object> m = new LinkedHashMap<>();
        m.put("chapter", "3 运行与时延"); m.put("source", "r30_writer_hold"); m.put("windowSince", since);
        List<Map<String,Object>> top = new ArrayList<>(), critical = new ArrayList<>();
        long windows;
        try {
            try (Cursor c = db.rawQuery("SELECT count(*) FROM r30_writer_hold WHERE window_end>=?", new String[] {Long.toString(since)})) { windows = c.moveToFirst() ? c.getLong(0) : 0; }
            try (Cursor c = db.rawQuery(HoldLedgerFlushR30.REPORT_TOP, new String[] {Long.toString(since)})) {
                while (c.moveToNext()) {
                    Map<String,Object> r = new LinkedHashMap<>(); r.put("operation", c.getString(0)); r.put("count", c.getLong(1)); r.put("holdMaxMs", c.getLong(2));
                    r.put("holdP95Ms", c.isNull(3) ? null : c.getLong(3)); r.put("overBudget", c.getLong(4)); r.put("overHard", c.getLong(5)); top.add(r);
                }
            }
            try (Cursor c = db.rawQuery(HoldLedgerFlushR30.REPORT_CRITICAL, new String[] {Long.toString(since)})) {
                while (c.moveToNext()) {
                    Map<String,Object> r = new LinkedHashMap<>(); r.put("operation", c.getString(0)); r.put("count", c.getLong(1));
                    r.put("waitP95Ms", c.isNull(2) ? null : c.getLong(2)); r.put("waitMaxMs", c.getLong(3)); r.put("holdMaxMs", c.getLong(4)); critical.add(r);
                }
            }
        } catch (RuntimeException missingTable) { windows = 0; }
        m.put("windows", windows);
        if (windows <= 0) { m.put("status", "INSUFFICIENT"); m.put("reason", "INSUFFICIENT：r30_writer_hold 尚无窗口"); }
        else m.put("status", "MEASURED");
        m.put("backgroundOverBudgetTop50", top); m.put("criticalWriteWait", critical);
        return m;
    }
    /** R30-B02-01 第3章 实时链分段：最近1440期，开奖期 d 的观测/校验/落库/结算 + 下一目标期 t 的请求/准备/冻结/截止（docs/B02 §2）。 */
    static final String TIMELINE_PAIRS_R30 = "SELECT d.issue,t.issue,"
            + "d.wall_first_observed,d.mono_first_observed,d.boot_first_observed,d.wall_validated,d.mono_validated,d.boot_validated,"
            + "d.wall_draw_committed,d.mono_draw_committed,d.boot_draw_committed,d.wall_settled,d.mono_settled,d.boot_settled,"
            + "t.wall_next_requested,t.mono_next_requested,t.boot_next_requested,t.wall_next_prepared,t.mono_next_prepared,t.boot_next_prepared,"
            + "t.wall_next_frozen,t.mono_next_frozen,t.boot_next_frozen,t.wall_next_deadline "
            + "FROM r30_issue_timeline d LEFT JOIN r30_issue_timeline t ON t.issue=(SELECT min(issue) FROM r30_issue_timeline x WHERE x.issue>d.issue AND x.wall_next_requested IS NOT NULL) "
            + "WHERE d.wall_first_observed IS NOT NULL ORDER BY d.issue DESC LIMIT 1440";
    static Map<String,Object> issueTimeline(SQLiteDatabase db) {
        Map<String,Object> m = new LinkedHashMap<>();
        m.put("chapter", "3 运行与时延：实时链分段"); m.put("source", "r30_issue_timeline"); m.put("window", "最近1440个开奖期");
        List<IssueTimelineR30.Row> rows = new ArrayList<>();
        IssueTimelineR30.Stage[] stages = {IssueTimelineR30.Stage.FIRST_OBSERVED, IssueTimelineR30.Stage.VALIDATED, IssueTimelineR30.Stage.DRAW_COMMITTED, IssueTimelineR30.Stage.SETTLED,
                IssueTimelineR30.Stage.NEXT_REQUESTED, IssueTimelineR30.Stage.NEXT_PREPARED, IssueTimelineR30.Stage.NEXT_FROZEN};
        try (Cursor c = db.rawQuery(TIMELINE_PAIRS_R30, null)) {
            while (c.moveToNext()) {
                IssueTimelineR30.Row r = new IssueTimelineR30.Row(c.getString(0));
                for (int i = 0; i < stages.length; i++) {
                    int k = 2 + 3 * i;
                    r.set(stages[i], c.isNull(k) ? null : c.getLong(k), c.isNull(k + 1) ? null : c.getLong(k + 1), c.isNull(k + 2) ? null : c.getString(k + 2));
                }
                if (!c.isNull(23)) r.set(IssueTimelineR30.Stage.NEXT_DEADLINE, c.getLong(23), null, null);
                rows.add(r);
            }
        } catch (RuntimeException missingTable) { m.put("status", "INSUFFICIENT"); m.put("reason", "INSUFFICIENT：r30_issue_timeline 尚未建立"); return m; }
        m.put("periods", rows.size());
        if (rows.isEmpty()) { m.put("status", "INSUFFICIENT"); m.put("reason", "INSUFFICIENT：r30_issue_timeline 尚无开奖期"); return m; }
        m.put("status", "MEASURED"); m.put("segments", IssueTimelineR30.summarize(rows));
        m.put("environmentNote", "时延门槛只以真机为准（docs/02 §4）；模拟器/CI 数值只记录不判定");
        return m;
    }
    static long num(Object o) { return o instanceof Number ? ((Number)o).longValue() : 0; }

    static Map<String,Object> modelCheckpoint(SQLiteDatabase db) {
        Map<String,Object> m = new LinkedHashMap<>();
        m.put("status", "NEVER_RUN"); m.put("I08SurfaceIntegration", "NOT_IMPLEMENTED");
        m.put("scope", "Existing rotating-position hit experiment; not a 10x10 number-surface model");
        m.put("legacyPartialStatesPreserved", QualificationInboxR29.count(db, "SELECT count(*) FROM r29_legacy_model_checkpoint"));
        try (Cursor c = db.rawQuery("SELECT feature_abi,dataset_hash,trained_through,state_hash,length(artifact),updated_at FROM r29_model_checkpoint WHERE owner='ATOMNET_FTRL'", null)) {
            if (c.moveToFirst()) {
                m.put("status", "RECORDED"); m.put("featureAbi", c.getString(0)); m.put("datasetHash", c.getString(1)); m.put("trainedThrough", c.getString(2));
                m.put("stateHash", c.getString(3)); m.put("bytes", c.getLong(4)); m.put("committedAt", c.getLong(5));
            }
            m.put("forecastStateReferences", QualificationInboxR29.count(db, "SELECT count(*) FROM r29_atomnet_forecast"));
        } catch (RuntimeException unavailable) { m.put("status", "SCHEMA_PENDING"); m.put("reason", unavailable.getClass().getSimpleName()); }
        return m;
    }

    @SuppressWarnings("unchecked")
    static Map<String,Object> cachedPanel(SQLiteDatabase db, String panel, String key) {
        try (Cursor c = db.rawQuery("SELECT payload_json FROM r27_chart_cache WHERE panel_id=? AND key=?", new String[] {panel,key})) {
            return c.moveToFirst() ? (Map<String,Object>) JsonR27.parse(c.getString(0)) : null;
        } catch (RuntimeException unavailable) { return null; }
    }

    @SuppressWarnings("unchecked")
    static Map<String,Object> cached(SQLiteDatabase db, String key) {
        try (Cursor c=db.rawQuery("SELECT payload_json FROM r27_chart_cache WHERE panel_id='r29' AND key=?",new String[]{key})) {
            return c.moveToFirst() ? (Map<String,Object>)JsonR27.parse(c.getString(0)) : null;
        } catch (RuntimeException schemaPending) { return null; }
    }
    static Map<String,Object> timeRange(SQLiteDatabase db) {
        try (Cursor c=db.rawQuery("SELECT min(issue),max(issue),count(*) FROM draws WHERE dirty=0",null)) {
            if (!c.moveToFirst() || c.getLong(2)==0) return null;
            Map<String,Object> m=new LinkedHashMap<>(); m.put("fromIssue",c.getString(0)); m.put("toIssue",c.getString(1)); m.put("draws",c.getLong(2)); return m;
        } catch (RuntimeException schemaPending) { return null; }
    }
}
