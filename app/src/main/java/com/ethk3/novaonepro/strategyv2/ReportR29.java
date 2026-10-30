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
        Map<String,MetricEvaluatorR29.Probe> probes=probes(baseline,preservation);
        probes.putAll(qualificationProbes(qualification));
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
    static long num(Object o) { return o instanceof Number ? ((Number)o).longValue() : 0; }

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
