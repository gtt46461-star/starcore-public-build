package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.ReleaseManifestR926;
import org.json.*;
import java.util.*;

/** Forty explicit result envelopes from the frozen report DB; missing evidence is never PASS. */
public final class RuntimeReportR26 {
    private RuntimeReportR26() {}
    private static final String[][] SECTIONS={
        {"IDENTITY","版本与原件"},
        {"REQUIREMENTS","历史要求保全"},
        {"DATA","开奖与缺口"},
        {"OPPORTUNITY","全期机会与覆盖"},
        {"FETCH","抓取时序"},
        {"SETTLEMENT","正式结算"},
        {"FREEZE","赛前冻结与延后证据"},
        {"AUTHORITY","主链资格与REAL"},
        {"ATOMS","原子世界"},
        {"FAMILIES","24家族"},
        {"EXPERTS","16专家"},
        {"TRAINING","持续训练与恢复"},
        {"EVOLUTION","代际与谱系"},
        {"FAILURES","原九类失败树"},
        {"ABLATION","消融与冲突"},
        {"EVIDENCE","历史与自然证据"},
        {"CALIBRATION","概率与校准"},
        {"RANKING","Rank1/2/3"},
        {"ACTION_SURFACE","100面与1200动作"},
        {"OWNER","Owner红线"},
        {"PERFORMANCE","资源与时延"},
        {"BACKGROUND","后台与活性"},
        {"RETENTION","淘汰与磁盘"},
        {"RECOVERY","异常与恢复"},
        {"UI","界面与排行"},
        {"SOURCE","源码审计覆盖"},
        {"DEVICE","真机矩阵"},
        {"CONCLUSION","目标差距和修复"},
        {"POLICY_IDENTITY","政策更新身份"},
        {"AVAILABILITY","数据可见时间"},
        {"EXPERIMENT_LEDGER","研究实验总账"},
        {"TEST_BUDGET","检验预算"},
        {"PUBLICATION_RECOVERY","发布恢复"},
        {"BUILD_IDENTITY","构建物身份"},
        {"SUPPLY_CHAIN","供应链清单"},
        {"SLO_BUDGET","SLO误差预算"},
        {"MERGE","双方案覆盖与冲突"},
        {"CONTINUOUS_RUNTIME","持续运行与调度"},
        {"FAIR_CAPABILITY_SERVICE","全家族专家防饥饿"},
        {"LOSSLESS_IDENTITY","无损身份与缺陷回归"}
    };
    private static JSONArray rows(SQLiteDatabase db,String sql)throws JSONException {
        JSONArray out=new JSONArray();try(Cursor c=db.rawQuery(sql,null)){while(out.length()<64&&c.moveToNext()){
            if(Thread.currentThread().isInterrupted())throw new IllegalStateException("REPORT_CANCELLED");
            JSONObject row=new JSONObject();for(int i=0;i<c.getColumnCount();i++){Object v=c.isNull(i)?JSONObject.NULL:c.getType(i)==Cursor.FIELD_TYPE_INTEGER?c.getLong(i):c.getType(i)==Cursor.FIELD_TYPE_FLOAT?c.getDouble(i):c.getString(i);row.put(c.getColumnName(i),v);}out.put(row);
        }}return out;
    }
    private static String query(String id){switch(id){
        case "CONTINUOUS_RUNTIME":case "BACKGROUND":return "SELECT task_id,lane,cursor,status,blocked_reason,last_progress,steps,last_cpu_ms FROM r26_task ORDER BY task_id";
        case "TRAINING":case "PUBLICATION_RECOVERY":return "SELECT campaign_id,policy_hash,cursor,dev_n,dev_k,val_n,val_k,status FROM r26_candidate ORDER BY campaign_id,policy_hash";
        case "FAMILIES":return "SELECT * FROM r26_capability WHERE kind='FAMILY' ORDER BY mechanism";
        case "EXPERTS":return "SELECT * FROM r26_capability WHERE kind='EXPERT' ORDER BY mechanism";
        case "FAIR_CAPABILITY_SERVICE":return "SELECT * FROM r26_capability ORDER BY mechanism";
        case "EVOLUTION":return "SELECT count(*) attempts,count(DISTINCT policy_hash) uniquePolicies,sum(category='STRUCTURAL_CANDIDATE') structuralCandidates,sum(behavior_novel) behaviorNovel,sum(research_accepted) researchAccepted,sum(prospective_qualified) prospectiveQualified,sum(verified_improvement) verifiedImprovements FROM r26_evolution";
        case "EXPERIMENT_LEDGER":return "SELECT campaign_id,snapshot_hash,source_through,status,created_at FROM r26_foundry ORDER BY created_at";
        case "TEST_BUDGET":return "SELECT genome_key,cohort,evidence_digest,look,family_alpha FROM r26_look ORDER BY genome_key,look";
        case "EVIDENCE":return "SELECT * FROM r26_cohort ORDER BY genome_key";
        case "SETTLEMENT":return "SELECT count(*) event_count,min(issue_ordinal) first_ordinal,max(issue_ordinal) last_ordinal FROM r26_feedback";
        case "PERFORMANCE":case "SLO_BUDGET":return "SELECT task_id,count(*) slices,sum(cpu_ms) cpu_ms,max(cpu_ms) max_cpu_ms,sum(blocked_reason LIKE 'CAPACITY_FAIL%') capacity_fail FROM r26_progress GROUP BY task_id";
        case "POLICY_IDENTITY":case "LOSSLESS_IDENTITY":return "SELECT policy_hash,canonical_text,parent,operation FROM r26_candidate ORDER BY policy_hash";
        default:return null;
    }}
    public static String json(SQLiteDatabase db){try{
        long start=System.nanoTime();JSONObject root=new JSONObject();root.put("schema","R26_REPORT_1");root.put("overallAcceptance","INCOMPLETE");root.put("naturalQuality","INSUFFICIENT");root.put("physicalDevice","NOT_RUN");JSONArray sections=new JSONArray();
        for(String[] spec:SECTIONS){long chapterStart=System.nanoTime();JSONObject s=new JSONObject();s.put("sectionId",spec[0]);s.put("title",spec[1]);s.put("schemaVersion","R26_REPORT_1");s.put("evidenceLevel","ANDROID_DATABASE_SNAPSHOT");s.put("sourceSnapshotId","REPORT_FROZEN_DB");s.put("sourceCommit","SEE_DELIVERY_RECEIPT");s.put("apkHash","SEE_DELIVERY_RECEIPT");s.put("asOfIssue",JSONObject.NULL);s.put("policyHash",JSONObject.NULL);s.put("metricDefinition","Persisted receipts; history selection and natural qualification remain separate");s.put("repairCardIds",new JSONArray());s.put("evidenceRefs",new JSONArray().put("catalog/").put("UPGRADE_DIAGNOSIS.json"));s.put("truncated",false);s.put("errorChain",new JSONArray());
            String sql=query(spec[0]);JSONArray data=new JSONArray();long expectedRows=0;String status="NOT_RUN",reason="Full chapter acceptance evidence has not been collected; existing report catalog is retained";
            if(sql!=null&&RuntimeSchemaR26.ready(db))try{try(Cursor count=db.rawQuery("SELECT count(*) FROM ("+sql+")",null)){if(count.moveToFirst())expectedRows=count.getLong(0);}data=rows(db,sql);status="PARTIAL";reason="Actual persisted records exported; mechanism, latency and natural-quality criteria require separate validation";}catch(Exception e){status="ERROR";reason=e.toString();JSONArray chain=new JSONArray();for(Throwable x=e;x!=null;x=x.getCause())chain.put(x.getClass().getName()+":"+x.getMessage());s.put("errorChain",chain);}
            s.put("status",status);s.put("reason",reason);s.put("denominator",data.length());s.put("expectedRows",spec[0].equals("FAMILIES")?24:spec[0].equals("EXPERTS")?16:spec[0].equals("FAIR_CAPABILITY_SERVICE")?40:expectedRows);s.put("exportedRows",data.length());s.put("omittedRows",Math.max(0,s.getLong("expectedRows")-data.length()));s.put("truncated",s.getLong("omittedRows")>0);s.put("metrics",data);s.put("elapsedMs",(System.nanoTime()-chapterStart)/1000000);sections.put(s);
        }root.put("sections",sections);root.put("expectedSections",40);root.put("exportedSections",sections.length());return root.toString(2);
    }catch(JSONException e){throw new IllegalStateException("R26_REPORT_ENVELOPE",e);}}
    public static String render(SQLiteDatabase db){
        StringBuilder b=new StringBuilder("\nR26 持续研究\n");if(!RuntimeSchemaR26.ready(db))return b.append("等待运行会话初始化；工程/能力/自然质量/真机分别验收。\n").toString();
        try(Cursor c=db.rawQuery("SELECT task_id,lane,cursor,status,coalesce(blocked_reason,''),steps,last_cpu_ms FROM r26_task ORDER BY lane,task_id",null)){while(c.moveToNext())b.append(c.getString(1)).append(' ').append(c.getString(0)).append("：").append(c.getString(3)).append("；已提交游标 ").append(c.getLong(2)).append("；切片 ").append(c.getLong(5)).append("；CPU ").append(c.getLong(6)).append("ms ").append(c.getString(4)).append('\n');}
        b.append("\n家族与专家实际能力状态\n");try(Cursor c=db.rawQuery("SELECT mechanism,state,reason FROM r26_capability ORDER BY mechanism",null)){while(c.moveToNext())b.append(c.getString(0)).append(" · ").append(c.getString(1)).append(" · ").append(c.getString(2)).append('\n');}
        return b.append("\n没有自然前瞻资格的候选仅进入影子研究；35%/37%/40%尚无新增合格证据。\n").toString();
    }
}
