package com.ethk3.novaonepro;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.util.*;

/** One read-only SQL statement supplies the summary denominators. Unknown is never zero. */
public final class ResearchFunnelR967 {
    private ResearchFunnelR967(){}
    private static final class Definition {
        final String key,table,expression,predicate,meaning;
        Definition(String k,String t,String e,String p,String m){key=k;table=t;expression=e;predicate=p;meaning=m;}
        String query(){return "SELECT "+expression+" FROM "+table+(predicate.isEmpty()?"":" WHERE "+predicate);}
    }
    private static final Definition[] DEFINITIONS={
        new Definition("immutable_formal_plans",ResearchGatePlanR967.PLAN,"COUNT(*)","","事前冻结的正式实验计划；不是合格证书数"),
        new Definition("immutable_gate_policies",ResearchGatePlanR967.POLICY,"COUNT(*)","","不可变统计政策版本，旧窗口不随新模板改阈值"),
        new Definition("research_registered",ResearchMultiplicityR932.IDENTITY,"COUNT(*)","","研究登记，不代表正式试验或主链授权"),
        new Definition("paid_hypotheses",ResearchMultiplicityR932.HYPOTHESIS,"COUNT(*)","","正式假设累计支出，历史不清零"),
        new Definition("formal_looks",ResearchMultiplicityR932.LOOK,"COUNT(*)","","已登记正式窗口，非合格证书数"),
        new Definition("calibration_studies",CandidateCalibrationR932.STUDY,"COUNT(*)","","候选校准研究数"),
        new Definition("calibration_settled",CandidateCalibrationR932.STUDY,"COALESCE(SUM(settled_n),0)","","各研究已结算观测之和，不是独立算法数"),
        new Definition("calibration_models",CandidateCalibrationR932.MODEL,"COUNT(*)","","已冻结候选校准模型"),
        new Definition("historical_archived_rows",HistoricalGenomeImportR967.ARCHIVE,"COUNT(*)","","历史原始档案；不等于恢复训练或合格算法"),
        new Definition("historical_seed_eligible",HistoricalGenomeImportR967.ARCHIVE,"COUNT(*)","verdict='ACCEPT_RESEARCH_SEED'","当前VM可作为新研究初始化的档案；旧Future不继承"),
        new Definition("historical_rejected_rows",HistoricalGenomeImportR967.ARCHIVE,"COUNT(*)","verdict='REJECT'","原淘汰/隔离/损坏/身份冲突等拒绝档案；原因永久保留"),
        new Definition("historical_seed_created_n0",HistoricalGenomeImportR967.USE,"COUNT(*)","state='CREATED_N0'","经唯一创建Owner登记的新研究种子；不是证书或真实训练数"),
        new Definition("historical_seed_use_rejected",HistoricalGenomeImportR967.USE,"COUNT(*)","state='REJECTED'","创建前再次校验拒绝或重复；不合并旧成绩"),
        new Definition("training_runs_v2",ResearchEvaluationRunR967.RUN,"COUNT(*)","","事前登记的独立训练流，不继承旧评价样本"),
        new Definition("frozen_versions_v2",ResearchEvaluationRunR967.EVAL,"COUNT(*)","","不可变评价版本，不等于合格算法数量"),
        new Definition("online_future","r952_candidate_predraw","COUNT(*)","settled=1 AND evidence_role IN ('ONLINE_TRAINING','TRAIN_STREAM_V2') AND proof_error=''","在线角色的合法未来标签"),
        new Definition("sealed_future","r958_frozen_predraw","COUNT(*)","settled=1 AND proof_error=''","封存评价标签，不允许训练反馈"),
        new Definition("numeric_update_executions","r952_artifact","COUNT(*)","numeric_updates>0 AND model_payload<>''","实际数值更新执行记录；不等于胜率提高或每次参数必变"),
        new Definition("training_pending","r952_research_outbox","COUNT(*)","state NOT IN ('COMPLETED','DEPENDENCY_RETIRED')","尚未完成的既有训练投递"),
        new Definition("training_dependency_retired","r952_research_outbox","COUNT(*)","state='DEPENDENCY_RETIRED'","旧规则停用投递；非完成、非训练、非质量淘汰"),
        new Definition("training_legacy_terminal","r952_research_outbox","COUNT(*)","state='TERMINAL_FAILED'","历史终止投递，保留原因、不冒充低胜率"),
        new Definition("post_training_pending",ResearchTaskQueueR967.TABLE,"COUNT(*)","kind='POST_TRAIN' AND status NOT IN ('COMPLETED','EXPIRED','FAILED_TERMINAL')","训练提交后待反哺任务"),
        new Definition("maintenance_pending",ResearchTaskQueueR967.TABLE,"COUNT(*)","kind='MAINTENANCE' AND status NOT IN ('COMPLETED','EXPIRED','FAILED_TERMINAL')","维护通道任务，不是数值训练样本"),
        new Definition("protected_inputs",ResearchTaskQueueR967.PIN,"COUNT(*)","","未完成任务持有的来源引用"),
        new Definition("challenger_states",ResearchLifecycleR932.TABLE,"COUNT(*)","state='CHALLENGER'","持久化挑战者阶段数，不推断有正式授权"),
        new Definition("formal_certificate_rows",ResearchPlatformStoreR929.TABLE_CERTIFICATE,"COUNT(*)","","正式证书表记录数，含失效历史"),
        new Definition("active_certificate_rows",ResearchPlatformStoreR929.TABLE_CERTIFICATE,"COUNT(*)","state='ACTIVE'","表内ACTIVE，不等于当前消费者重新验证通过"),
        new Definition("positive_weight_contributions",ResearchPlatformStoreR929.TABLE_CONTRIBUTION,"COUNT(*)","authorized_weight>0","实际正权重贡献记录，不等于REAL出手数"),
        new Definition("world_atom_rows","r952_world_atom","COUNT(*)","","原子登记行，不代表已被程序消费"),
        new Definition("world_compiled_molecules","r952_world_molecule","COUNT(*)","compiled_hash<>'' AND resolved_semantic_hash<>'' AND canonical_version='WORLD_DAG_V2'","具有真实编译身份的分子登记")
    };
    public static final class Snapshot {
        public final long startedAt,finishedAt;
        public final String drawWatermark,id,error,sql;
        public final Map<String,Long> values;
        Snapshot(long start,long end,String mark,Map<String,Long> data,String failure,String query){
            startedAt=start;finishedAt=end;drawWatermark=mark;values=Collections.unmodifiableMap(new LinkedHashMap<String,Long>(data));error=failure;sql=query;
            id=V51ChecksumEngine.sha256("R967_READ_SNAPSHOT|"+start+'|'+end+'|'+mark+'|'+data+'|'+failure);
        }
        public String render() {
            StringBuilder b=new StringBuilder("R967_RESEARCH_FUNNEL|snapshot=").append(id).append("|statementSnapshot=1|startedAt=").append(startedAt).append("|finishedAt=").append(finishedAt).append("|drawWatermark=").append(drawWatermark).append("|status=").append(error.isEmpty()?"READ_OK":"READ_ERROR:"+error);
            for(Definition d:DEFINITIONS){Long value=values.get(d.key);b.append('\n').append(d.key).append('=').append(value==null?"UNKNOWN_OR_NOT_READY":value).append("|meaning=").append(d.meaning).append("|source=").append(d.table);}
            b.append("\ncurrent_compatible_selected_applied=REQUIRES_CONSUMER_RECEIPTS_NOT_INFERRED_FROM_ACTIVE_COUNT");
            b.append("\nCP12_SCOPE=CANONICAL_SIX_ADAPTER_CODE_CONTRACT|device=NOT_VERIFIED|future_health=READ_ACTUAL_EVIDENCE|model_quality=READ_ACTUAL_EVIDENCE|device_runtime=NOT_VERIFIED|future_effect=NOT_PROVEN");
            b.append("\nrecompute_sql_hash=").append(V51ChecksumEngine.sha256(sql));return b.toString();
        }
    }
    public static Snapshot capture(DbStore store) {
        if(store==null)return failure(System.currentTimeMillis(),"STORE_NULL");
        try{return capture(store.readable());}catch(RuntimeException error){return failure(System.currentTimeMillis(),error.getClass().getSimpleName()+":"+String.valueOf(error.getMessage()));}
    }
    static Snapshot capture(SQLiteDatabase db) {
        long start=System.currentTimeMillis();Map<String,Long> values=new LinkedHashMap<String,Long>();String query="";
        try {
            Set<String> present=new HashSet<String>();
            try(Cursor c=db.rawQuery("SELECT name FROM sqlite_master WHERE type IN ('table','view')",null)){while(c.moveToNext())present.add(c.getString(0));}
            StringBuilder sql=new StringBuilder("SELECT ");
            sql.append(present.contains(DbHelper.TABLE_DRAWS)?"(SELECT COALESCE(MAX(issue),'') FROM "+DbHelper.TABLE_DRAWS+" WHERE dirty=0)":"NULL");
            for(Definition d:DEFINITIONS)sql.append(',').append(present.contains(d.table)?"("+d.query()+")":"NULL");
            query=sql.toString();String mark;
            try(Cursor c=db.rawQuery(query,null)) {
                if(!c.moveToFirst())throw new IllegalStateException("EMPTY_SUMMARY_STATEMENT");
                mark=c.isNull(0)?"UNKNOWN":c.getString(0);
                for(int i=0;i<DEFINITIONS.length;i++)values.put(DEFINITIONS[i].key,c.isNull(i+1)?null:Long.valueOf(c.getLong(i+1)));
            }
            return new Snapshot(start,System.currentTimeMillis(),mark,values,"",query);
        }catch(RuntimeException error) {
            for(Definition d:DEFINITIONS)values.put(d.key,null);
            return new Snapshot(start,System.currentTimeMillis(),"UNKNOWN",values,error.getClass().getSimpleName()+":"+String.valueOf(error.getMessage()),query);
        }
    }
    private static Snapshot failure(long now,String error){Map<String,Long> values=new LinkedHashMap<String,Long>();for(Definition d:DEFINITIONS)values.put(d.key,null);return new Snapshot(now,now,"UNKNOWN",values,error,"");}

public static Snapshot captureSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, com.ethk3.novaonepro.ReportReadContextCP4 store) {
        if(store==null)return failureSnapshotCP4(report, report.nowMs(),"STORE_NULL");
        try{return captureSnapshotCP4(report, store.readable());}catch(RuntimeException error){throw new com.ethk3.novaonepro.ReportReadContextCP4.ReportReadFailureCP4("REPORT_QUERY_OR_FORMAT_FAILED",error);}
    }

static Snapshot captureSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, SQLiteDatabase db) {
        long start=report.nowMs();Map<String,Long> values=new LinkedHashMap<String,Long>();String query="";
        try {
            Set<String> present=new HashSet<String>();
            try(Cursor c=report.select("SELECT name FROM sqlite_master WHERE type IN ('table','view')",null)){while(c.moveToNext())present.add(c.getString(0));}
            com.ethk3.novaonepro.ReportTextCP12 sql=new com.ethk3.novaonepro.ReportTextCP12(report, "SELECT ");
            sql.append(present.contains(DbHelper.TABLE_DRAWS)?"(SELECT COALESCE(MAX(issue),'') FROM "+DbHelper.TABLE_DRAWS+" WHERE dirty=0)":"NULL");
            for(Definition d:DEFINITIONS)sql.append(',').append(present.contains(d.table)?"("+d.query()+")":"NULL");
            query=sql.toString();String mark;
            try(Cursor c=report.select(query,null)) {
                if(!c.moveToFirst())throw new IllegalStateException("EMPTY_SUMMARY_STATEMENT");
                mark=c.isNull(0)?"UNKNOWN":c.getString(0);
                for(int i=0;i<DEFINITIONS.length;i++)values.put(DEFINITIONS[i].key,c.isNull(i+1)?null:Long.valueOf(c.getLong(i+1)));
            }
            return new Snapshot(start,report.nowMs(),mark,values,"",query);
        }catch(RuntimeException error) {throw new com.ethk3.novaonepro.ReportReadContextCP4.ReportReadFailureCP4("REPORT_QUERY_OR_FORMAT_FAILED",error);}
    }

public static Snapshot failureSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, long now,String error){Map<String,Long> values=new LinkedHashMap<String,Long>();for(Definition d:DEFINITIONS)values.put(d.key,null);return new Snapshot(now,now,"UNKNOWN",values,error,"");}
}
