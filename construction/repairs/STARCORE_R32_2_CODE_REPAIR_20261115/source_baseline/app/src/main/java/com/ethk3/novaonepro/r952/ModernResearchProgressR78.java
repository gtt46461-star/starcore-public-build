package com.ethk3.novaonepro.r952;
import android.database.Cursor;import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.*;
/** Read-only progress. Historical pending predictions are never called training samples. */
public final class ModernResearchProgressR78 {
 private ModernResearchProgressR78(){}
 private interface Reader {Cursor query(String sql);}
 public static String render(SQLiteDatabase db){return render(sql->db.rawQuery(sql,null),false);}
 public static String render(ReportReadContextCP4 report,SQLiteDatabase db){return render(sql->report.select(sql,null),true);}
 private static String render(Reader r,boolean strict){
  StringBuilder b=new StringBuilder("当前算法创造与训练\n");
  append(b,r,strict,"现代候选／可继续研究／已隔离","SELECT COUNT(*),COALESCE(SUM(CASE WHEN state NOT IN('QUARANTINED','QUALITY_PRUNED','RESOURCE_RETIRED') THEN 1 ELSE 0 END),0),COALESCE(SUM(CASE WHEN state='QUARANTINED' THEN 1 ELSE 0 END),0) FROM r952_candidate WHERE ast LIKE 'AST3:%'");
  append(b,r,strict,"历史训练完成／处理中／失败隔离","SELECT COALESCE(SUM(CASE WHEN status='COMPLETED' THEN 1 ELSE 0 END),0),COALESCE(SUM(CASE WHEN status NOT IN('COMPLETED','EXPIRED','FAILED_TERMINAL') THEN 1 ELSE 0 END),0),COALESCE(SUM(CASE WHEN status='FAILED_TERMINAL' THEN 1 ELSE 0 END),0) FROM v57_research_task_r967 WHERE kind='TRAIN_CANDIDATE'");
  append(b,r,strict,"冻结任务完成／处理中","SELECT COALESCE(SUM(CASE WHEN status='COMPLETED' THEN 1 ELSE 0 END),0),COALESCE(SUM(CASE WHEN status NOT IN('COMPLETED','EXPIRED','FAILED_TERMINAL') THEN 1 ELSE 0 END),0) FROM v57_research_task_r967 WHERE kind='FREEZE_CANDIDATE'");
  append(b,r,strict,"逐期训练待处理／训练后反馈待处理","SELECT (SELECT COUNT(*) FROM r952_research_outbox WHERE state NOT IN('COMPLETED','DEPENDENCY_RETIRED')),(SELECT COUNT(*) FROM v57_research_task_r967 WHERE kind='POST_TRAIN' AND status NOT IN('COMPLETED','EXPIRED','FAILED_TERMINAL'))");
  append(b,r,strict,"校准研究／仍在收集／READY","SELECT COUNT(*),COALESCE(SUM(CASE WHEN state='COLLECTING' THEN 1 ELSE 0 END),0),COALESCE(SUM(CASE WHEN state='READY' THEN 1 ELSE 0 END),0) FROM research_candidate_cal_study_r932");
  append(b,r,strict,"正式窗口／有效合格证书／精确出手收据","SELECT (SELECT COUNT(*) FROM research_fixed_window_r932),(SELECT COUNT(*) FROM research_certificate_r929 WHERE state='ACTIVE' AND all_gates_pass=1),(SELECT COUNT(*) FROM r974_exact_real_receipt)");
  b.append("历史预测待结算与训练样本分别统计；每个新子代的未来样本从0开始。\n");return b.toString();
 }
 private static void append(StringBuilder b,Reader r,boolean strict,String label,String sql){
  b.append(label).append('=');try(Cursor c=r.query(sql)){if(!c.moveToFirst())throw new IllegalStateException("NO_PROGRESS_ROW");for(int i=0;i<c.getColumnCount();i++){if(i>0)b.append('/');b.append(c.isNull(i)?"UNKNOWN":c.getString(i));}}
  catch(ResearchWorkBudgetR927.Yield yield){throw yield;}
  catch(RuntimeResourceAuditR927.BudgetExceeded budget){throw budget;}
  catch(RuntimeException unknown){if(strict)throw unknown;b.append("UNKNOWN");}b.append('\n');
 }
}
