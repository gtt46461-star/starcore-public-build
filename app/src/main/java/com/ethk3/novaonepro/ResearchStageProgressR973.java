package com.ethk3.novaonepro;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.io.IOException;
import java.io.Writer;

/** Export-worker-only, bounded read-only stage receipts. Never creates tables or evidence.
 * Each query has its own read time, not a cross-table transaction snapshot. SQL is emitted
 * verbatim so missing/wrong schema cannot be confused with a measured zero.
 */
public final class ResearchStageProgressR973 {
    private ResearchStageProgressR973() {}
    public static void write(SQLiteDatabase db, Writer out) throws IOException {
        out.write("\n【R974逐段执行收据：只读、各查询独立时点、不是整库事务快照】\n");
        out.write("训练续接=" + TrainingContinuationR973.scope() + "\n");
        readRows(db,out,"R979逐算法训练与真实父代（最多64个）","SELECT c.candidate_id,c.parent_id,c.state,c.future_n,l.trained_until_issue,a.numeric_updates,a.payload_hash FROM r952_candidate c LEFT JOIN r952_learning_cursor l ON l.consumer_id='R952:'||c.candidate_id LEFT JOIN r952_artifact a ON a.artifact_hash=l.artifact_hash ORDER BY c.created_at,c.candidate_id LIMIT 64",64);
        readRows(db,out,"R979历史开发检查点（训练不等于未来样本）","SELECT state_key,state_value FROM app_state WHERE state_key LIKE 'r979_development_%' OR state_key LIKE 'r959_development_job_%' ORDER BY state_key LIMIT 68",68);
        readRows(db,out,"R979各冻结模型校准状态","SELECT candidate_key,state,captured_n,settled_n,first_issue,study_hash FROM research_candidate_cal_study_r932 WHERE candidate_key LIKE 'algorithm:R958-V%' ORDER BY first_issue DESC LIMIT 32",32);
        read(db,out,"实际原子/时序矩阵/分子特征被模型消费","SELECT target_issue,candidate_id,model_hash,world_input_hash,world_output_hash,prediction_mask,receipt_hash FROM r974_computation_receipt ORDER BY target_issue DESC,candidate_id LIMIT 1");
        read(db,out,"真实公式双亲血缘","SELECT child_id,parent_id,parent_role,evidence_hash,created_issue FROM r974_model_lineage ORDER BY created_issue DESC,child_id,parent_role LIMIT 1");
        read(db,out,"合格冻结模型实际REAL写入","SELECT target_issue,trade_id,candidate_id,model_hash,certificate_hash,proof_hash,committed_at FROM r974_exact_real_receipt ORDER BY target_issue DESC LIMIT 1");
        out.write("R974：新训练流逐期训练；冻结模型独立逐期评估；旧V2奇偶计划不回写。时序关联矩阵不是干预因果证明。\n");
        read(db,out,"待选模型执行失败与换选原因","SELECT target_issue,candidate_id,certificate_hash,reason FROM r974_prediction_rejection ORDER BY target_issue DESC,recorded_at DESC LIMIT 1");
        read(db,out,"有效开奖","SELECT issue FROM draws WHERE dirty=0 ORDER BY issue DESC LIMIT 1");
        read(db,out,"实际数值训练提交","SELECT candidate_id,target_issue,artifact_hash,payload_hash,numeric_updates FROM r952_artifact ORDER BY target_issue DESC,candidate_id LIMIT 1");
        read(db,out,"训练未完成原因","SELECT candidate_id,target_issue,state,last_error,delivery_hash FROM r952_research_outbox WHERE state NOT IN ('COMPLETED','DEPENDENCY_RETIRED') ORDER BY target_issue,candidate_id LIMIT 1");
        read(db,out,"训练后研究已完成","SELECT candidate_id,target_issue,status,result_hash,last_error FROM v57_research_task_r967 WHERE kind='POST_TRAIN' AND status='COMPLETED' ORDER BY target_issue DESC,candidate_id LIMIT 1");
        read(db,out,"维护研究已完成","SELECT candidate_id,target_issue,status,result_hash,last_error FROM v57_research_task_r967 WHERE kind='MAINTENANCE' AND status='COMPLETED' ORDER BY target_issue DESC,candidate_id LIMIT 1");
        read(db,out,"研究队列当前阻断","SELECT kind,candidate_id,target_issue,status,last_error FROM v57_research_task_r967 WHERE status NOT IN ('COMPLETED','EXPIRED','FAILED_TERMINAL') ORDER BY target_issue,candidate_id LIMIT 1");
        read(db,out,"有公式身份的子代登记","SELECT candidate_id,parent_id,created_issue,structural_hash,intent_id,future_n FROM r952_candidate WHERE parent_id<>'' ORDER BY created_issue DESC,candidate_id LIMIT 1");
        read(db,out,"独立冻结模型建立","SELECT candidate_id,learner_id,created_issue,trained_until_issue,model_hash,run_hash,rule_hash,update_endpoint FROM r967_frozen_evaluation_v2 ORDER BY created_issue DESC,candidate_id LIMIT 1");
        read(db,out,"逐候选最近审核","SELECT candidate_key,last_issue,state,reason,first_prune_reason,future_n,paired_n,evidence_hash FROM research_lifecycle_r932 ORDER BY last_issue DESC,reviewed_at DESC LIMIT 1");
        read(db,out,"证书最近签发（不是直接REAL授权）","SELECT candidate_id,valid_from_issue,state,level,maximum_weight,all_gates_pass,reason,certificate_hash FROM research_certificate_r929 ORDER BY issued_at DESC,candidate_id LIMIT 1");
        read(db,out,"已记录正权重模型消费","SELECT target_issue,candidate_id,certificate_hash,bundle_hash,authorized_weight,status,reason,record_hash FROM research_mainline_contribution_r929 WHERE authorized_weight>0 ORDER BY target_issue DESC,candidate_id LIMIT 1");
        read(db,out,"实际主链排名影响","SELECT target_issue,candidate_id,status,influence_weight,record_hash FROM v57_research_mainline_contribution_r922 WHERE applied=1 AND influence_weight>0 ORDER BY target_issue DESC LIMIT 1");
        out.write("主链排名影响不是REAL记录；证书/独立窗口见同报告原始表。\n");
        out.write("六研究适配器端到端消费证明=NOT_ESTABLISHED；原子/分子注册行数不能替代计算与消费收据。\n");
        out.write("新包真机及时出号、息屏、未来命中与全过程晋升=NOT_RUN_BY_BUILD_TESTS。\n");
    }
    static void read(SQLiteDatabase db,Writer out,String stage,String sql)throws IOException {
        long started=System.currentTimeMillis();
        out.write("stage="+stage+"|readStartedAt="+started+"|sql="+sql+"\n");
        if(db==null){out.write("result=UNKNOWN|reason=DATABASE_NOT_READY\n");return;}
        try(Cursor c=db.rawQuery(sql,null)) {
            if(!c.moveToFirst()){out.write("result=NO_MATCHING_RECEIPT|successIssue=NONE|model=NONE\n");return;}
            out.write("result=RECORDED_ROW");
            for(int i=0;i<c.getColumnCount();i++)out.write("|"+clean(c.getColumnName(i))+"="+clean(c.getString(i)));
            out.write("\n");
        }catch(RuntimeException error){
            out.write("result=UNKNOWN|reason="+error.getClass().getSimpleName()+":"+clean(error.getMessage())+"\n");
        }
    }
    static void readRows(SQLiteDatabase db,Writer out,String stage,String sql,int limit)throws IOException {
        out.write("stage="+stage+"|readStartedAt="+System.currentTimeMillis()+"|sql="+sql+"\n");
        if(db==null){out.write("result=UNKNOWN|reason=DATABASE_NOT_READY\n");return;}
        try(Cursor c=db.rawQuery(sql,null)) {
            int n=0;
            while(n<limit&&c.moveToNext()) {out.write("result=RECORDED_ROW");for(int i=0;i<c.getColumnCount();i++)out.write("|"+clean(c.getColumnName(i))+"="+clean(c.getString(i)));out.write("\n");n++;}
            if(n==0)out.write("result=NO_MATCHING_RECEIPT\n");
            out.write("rows="+n+"|limit="+limit+"|bounded=true\n");
        }catch(RuntimeException error){out.write("result=UNKNOWN|reason="+error.getClass().getSimpleName()+":"+clean(error.getMessage())+"\n");}
    }
    private static String clean(String value){
        if(value==null)return "NULL";
        value=value.replace("\r","\\r").replace("\n","\\n").replace("|","\\|");
        return value.length()>1024?value.substring(0,1024)+"...[TRUNCATED]":value;
    }

public static void writeSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, SQLiteDatabase db, Writer out) throws IOException {
        out.write("\n【R974逐段执行收据：只读、各查询独立时点、不是整库事务快照】\n");
        out.write("训练续接=" + TrainingContinuationR973.scope() + "\n");
        readRowsSnapshotCP4(report, db,out,"R979逐算法训练与真实父代（最多64个）","SELECT c.candidate_id,c.parent_id,c.state,c.future_n,l.trained_until_issue,a.numeric_updates,a.payload_hash FROM r952_candidate c LEFT JOIN r952_learning_cursor l ON l.consumer_id='R952:'||c.candidate_id LEFT JOIN r952_artifact a ON a.artifact_hash=l.artifact_hash ORDER BY c.created_at,c.candidate_id LIMIT 64",64);
        readRowsSnapshotCP4(report, db,out,"R979历史开发检查点（训练不等于未来样本）","SELECT state_key,state_value FROM app_state WHERE state_key LIKE 'r979_development_%' OR state_key LIKE 'r959_development_job_%' ORDER BY state_key LIMIT 68",68);
        readRowsSnapshotCP4(report, db,out,"R979各冻结模型校准状态","SELECT candidate_key,state,captured_n,settled_n,first_issue,study_hash FROM research_candidate_cal_study_r932 WHERE candidate_key LIKE 'algorithm:R958-V%' ORDER BY first_issue DESC LIMIT 32",32);
        readSnapshotCP4(report, db,out,"实际原子/时序矩阵/分子特征被模型消费","SELECT target_issue,candidate_id,model_hash,world_input_hash,world_output_hash,prediction_mask,receipt_hash FROM r974_computation_receipt ORDER BY target_issue DESC,candidate_id LIMIT 1");
        readSnapshotCP4(report, db,out,"真实公式双亲血缘","SELECT child_id,parent_id,parent_role,evidence_hash,created_issue FROM r974_model_lineage ORDER BY created_issue DESC,child_id,parent_role LIMIT 1");
        readSnapshotCP4(report, db,out,"合格冻结模型实际REAL写入","SELECT target_issue,trade_id,candidate_id,model_hash,certificate_hash,proof_hash,committed_at FROM r974_exact_real_receipt ORDER BY target_issue DESC LIMIT 1");
        out.write("R974：新训练流逐期训练；冻结模型独立逐期评估；旧V2奇偶计划不回写。时序关联矩阵不是干预因果证明。\n");
        readSnapshotCP4(report, db,out,"待选模型执行失败与换选原因","SELECT target_issue,candidate_id,certificate_hash,reason FROM r974_prediction_rejection ORDER BY target_issue DESC,recorded_at DESC LIMIT 1");
        readSnapshotCP4(report, db,out,"有效开奖","SELECT issue FROM draws WHERE dirty=0 ORDER BY issue DESC LIMIT 1");
        readSnapshotCP4(report, db,out,"实际数值训练提交","SELECT candidate_id,target_issue,artifact_hash,payload_hash,numeric_updates FROM r952_artifact ORDER BY target_issue DESC,candidate_id LIMIT 1");
        readSnapshotCP4(report, db,out,"训练未完成原因","SELECT candidate_id,target_issue,state,last_error,delivery_hash FROM r952_research_outbox WHERE state NOT IN ('COMPLETED','DEPENDENCY_RETIRED') ORDER BY target_issue,candidate_id LIMIT 1");
        readSnapshotCP4(report, db,out,"训练后研究已完成","SELECT candidate_id,target_issue,status,result_hash,last_error FROM v57_research_task_r967 WHERE kind='POST_TRAIN' AND status='COMPLETED' ORDER BY target_issue DESC,candidate_id LIMIT 1");
        readSnapshotCP4(report, db,out,"维护研究已完成","SELECT candidate_id,target_issue,status,result_hash,last_error FROM v57_research_task_r967 WHERE kind='MAINTENANCE' AND status='COMPLETED' ORDER BY target_issue DESC,candidate_id LIMIT 1");
        readSnapshotCP4(report, db,out,"研究队列当前阻断","SELECT kind,candidate_id,target_issue,status,last_error FROM v57_research_task_r967 WHERE status NOT IN ('COMPLETED','EXPIRED','FAILED_TERMINAL') ORDER BY target_issue,candidate_id LIMIT 1");
        readSnapshotCP4(report, db,out,"有公式身份的子代登记","SELECT candidate_id,parent_id,created_issue,structural_hash,intent_id,future_n FROM r952_candidate WHERE parent_id<>'' ORDER BY created_issue DESC,candidate_id LIMIT 1");
        readSnapshotCP4(report, db,out,"独立冻结模型建立","SELECT candidate_id,learner_id,created_issue,trained_until_issue,model_hash,run_hash,rule_hash,update_endpoint FROM r967_frozen_evaluation_v2 ORDER BY created_issue DESC,candidate_id LIMIT 1");
        readSnapshotCP4(report, db,out,"逐候选最近审核","SELECT candidate_key,last_issue,state,reason,first_prune_reason,future_n,paired_n,evidence_hash FROM research_lifecycle_r932 ORDER BY last_issue DESC,reviewed_at DESC LIMIT 1");
        readSnapshotCP4(report, db,out,"证书最近签发（不是直接REAL授权）","SELECT candidate_id,valid_from_issue,state,level,maximum_weight,all_gates_pass,reason,certificate_hash FROM research_certificate_r929 ORDER BY issued_at DESC,candidate_id LIMIT 1");
        readSnapshotCP4(report, db,out,"已记录正权重模型消费","SELECT target_issue,candidate_id,certificate_hash,bundle_hash,authorized_weight,status,reason,record_hash FROM research_mainline_contribution_r929 WHERE authorized_weight>0 ORDER BY target_issue DESC,candidate_id LIMIT 1");
        readSnapshotCP4(report, db,out,"实际主链排名影响","SELECT target_issue,candidate_id,status,influence_weight,record_hash FROM v57_research_mainline_contribution_r922 WHERE applied=1 AND influence_weight>0 ORDER BY target_issue DESC LIMIT 1");
        out.write("主链排名影响不是REAL记录；证书/独立窗口见同报告原始表。\n");
        out.write("六研究适配器端到端消费证明=NOT_ESTABLISHED；原子/分子注册行数不能替代计算与消费收据。\n");
        out.write("新包真机及时出号、息屏、未来命中与全过程晋升=NOT_RUN_BY_BUILD_TESTS。\n");
    }

static void readSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, SQLiteDatabase db,Writer out,String stage,String sql)throws IOException {
        long started=report.nowMs();
        out.write("stage="+stage+"|readStartedAt="+started+"|sql="+sql+"\n");
        if(db==null){out.write("result=UNKNOWN|reason=DATABASE_NOT_READY\n");return;}
        try(Cursor c=report.select(sql,null)) {
            if(!c.moveToFirst()){out.write("result=NO_MATCHING_RECEIPT|successIssue=NONE|model=NONE\n");return;}
            out.write("result=RECORDED_ROW");
            for(int i=0;i<c.getColumnCount();i++)out.write("|"+cleanSnapshotCP4(report, c.getColumnName(i))+"="+cleanSnapshotCP4(report, c.getString(i)));
            out.write("\n");
        }catch(RuntimeException error){throw new com.ethk3.novaonepro.ReportReadContextCP4.ReportReadFailureCP4("REPORT_QUERY_OR_FORMAT_FAILED",error);}
    }

public static String cleanSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, String value){
        if(value==null)return "NULL";
        value=value.replace("\r","\\r").replace("\n","\\n").replace("|","\\|");
        return value.length()>1024?value.substring(0,1024)+"...[TRUNCATED]":value;
    }

static void readRowsSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, SQLiteDatabase db,Writer out,String stage,String sql,int limit)throws IOException {
        out.write("stage="+stage+"|readStartedAt="+report.nowMs()+"|sql="+sql+"\n");
        if(db==null){out.write("result=UNKNOWN|reason=DATABASE_NOT_READY\n");return;}
        try(Cursor c=report.select(sql,null)) {
            int n=0;
            while(n<limit&&c.moveToNext()) {out.write("result=RECORDED_ROW");for(int i=0;i<c.getColumnCount();i++)out.write("|"+cleanSnapshotCP4(report, c.getColumnName(i))+"="+cleanSnapshotCP4(report, c.getString(i)));out.write("\n");n++;}
            if(n==0)out.write("result=NO_MATCHING_RECEIPT\n");
            out.write("rows="+n+"|limit="+limit+"|bounded=true\n");
        }catch(RuntimeException error){throw new com.ethk3.novaonepro.ReportReadContextCP4.ReportReadFailureCP4("REPORT_QUERY_OR_FORMAT_FAILED",error);}
    }
}
