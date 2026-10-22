package com.ethk3.novaonepro;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.r952.StrictCanonicalJson;
import java.nio.file.*;
import java.util.*;

/** Read projection only: report metadata is not a writable production/qualification fixture. */
public final class pipeline_status_R76 {
 static int checks;
 static void ok(boolean b,String message){if(!b)throw new AssertionError(message);checks++;System.out.println("PASS "+message);}
 static final class ReadDb extends SQLiteDatabase {
  final List<String> queries=new ArrayList<>();long longestNs;
  ReadDb(String bridge,String path)throws Exception{super(bridge,path);}
  @Override public synchronized Cursor rawQuery(String sql,String[] args){queries.add(sql);long started=System.nanoTime();try{return super.rawQuery(sql,args);}finally{longestNs=Math.max(longestNs,System.nanoTime()-started);}}
 }
 static String all(ResearchPipelineStatusR76.Snapshot s){return s.summary+'\n'+s.training+'\n'+s.evolution+'\n'+s.atomsMatrices+'\n'+s.mainline;}
 public static void main(String[] a)throws Exception{
  try(ReadDb db=new ReadDb(a[0],a[1])){
   long start=System.nanoTime();ResearchPipelineStatusR76.Snapshot s=ResearchPipelineStatusR76.readDatabase(db,1791878660000L,System.nanoTime()+350_000_000L);String text=all(s);
   Files.writeString(Path.of(a[3]+".txt"),text);
   if(a[2].equals("failure")){
    ok(s.training.contains("本段读取失败")&&!s.training.contains("训练验证完成 0"),"MISSING_TABLE_IS_UNKNOWN_NOT_ZERO");
    ok(s.evolution.contains("已登记11个动作"),"READ_FAILURE_ISOLATED_TO_SECTION");
   }else ok(!text.contains("本段读取失败"),"ALL_PROJECTION_SECTIONS_READ");
   ok(!text.contains("null"),"NO_NULL_RENDERING");
   if(a[2].equals("report")||a[2].equals("false-complete")){
    ok(s.training.contains("已登记6个算法"),"REPORT_SIX_PROTOCOLS");
    ok(s.training.contains("有特征进度 2，训练验证完成 0，已冻结 0"),"QUEUED_FEATURES_ARE_NOT_TRAINED");
    ok(s.training.contains("特征 3/1320")&&s.training.contains("特征 16/1320"),"COMMITTED_FEATURE_CURSORS_EXPOSED");
    ok(s.evolution.contains("已登记11个动作")&&s.evolution.contains("已生成子代 6"),"ACTUAL_ACTION_CHILD_PROGRESS");
    ok(s.evolution.contains("已完成独立评价 0"),"WAIT_FUTURE_IS_NOT_EVALUATED");
    ok(s.atomsMatrices.contains("25350/25350")&&s.atomsMatrices.contains("4200/4200"),"PINNED_IMPORT_CURSOR_COUNTS");
    ok(s.atomsMatrices.contains("矩阵定义26个，其中有数值投影 15个"),"DEFINITION_AND_PROJECTION_SEPARATED");
    ok(s.mainline.contains("赛前已预存 2/241，已结算 0/241"),"CURRENT_WINDOW_ONLY_NOT_LIFETIME_TOTAL");
    ok(s.mainline.contains("尚未形成校准模型，胜率/稳定性未评估"),"NO_MODEL_NOT_FALSE_LOW_WINRATE");
    if(a[2].equals("false-complete"))ok(s.training.contains("任务标记完成，但缺少匹配的训练验证收据"),"TASK_COMPLETE_WITHOUT_RECEIPT_IS_EXPLICIT");
   }else if(a[2].equals("repair")){
    ok(s.training.contains("训练验证完成 1，已冻结 1"),"ACTUAL_OWNER_ARTIFACT_RECEIPT_AND_FREEZE");
    ok(s.training.contains("特征 600/600")&&s.training.contains("开发数据截止 202609230421"),"ACTUAL_OWNER_PROGRESS_AND_CUTOFF");
   }else if(a[2].equals("online")){
    ok(s.training.contains("202610130483：FTRL产物 已提交1个"),"FTRL_PER_ISSUE_ARTIFACT_DISTINCT");
    ok(s.training.contains("有特征进度 2，训练验证完成 0"),"ONLINE_SUCCESS_DOES_NOT_PROMOTE_MODERN_TRAINING");
   }else if(a[2].equals("empty")){
    ok(s.sourceIssue.equals("未知")&&s.training.contains("尚无现代开发训练协议"),"EMPTY_DATABASE_IS_NOT_FINISHED");
    ok(s.mainline.contains("当前未见活动校准预约"),"EMPTY_QUALITY_NOT_FALSE_LOW_WINRATE");
   }else if(a[2].equals("cap")){
    ok(s.evolution.contains("最近32个动作")&&!s.evolution.contains("已登记108个动作"),"CAPPED_ACTION_SCOPE_NOT_GLOBAL_COUNT");
   }
   ok(db.queries.stream().noneMatch(q->q.toUpperCase(Locale.ROOT).contains("COUNT(")),"NO_FULL_COUNT");
   List<Map<String,Object>> plans=new ArrayList<>();List<String> originals=new ArrayList<>(db.queries);
   for(String q:originals){Map<String,Object> p=new LinkedHashMap<>();p.put("sql",q);int binds=0;for(int i=0;i<q.length();i++)if(q.charAt(i)=='?')binds++;String[] params=new String[binds];Arrays.fill(params,"0");List<String> plan=new ArrayList<>();try(Cursor c=db.rawQuery("EXPLAIN QUERY PLAN "+q,params)){while(c.moveToNext())plan.add(c.getString(3));}catch(RuntimeException missing){if(!a[2].equals("failure"))throw missing;plan.add("EXPECTED_MISSING_TABLE");}p.put("plan",plan);plans.add(p);}
   int before=db.queries.size();ResearchPipelineStatusR76.Snapshot exhausted=ResearchPipelineStatusR76.readDatabase(db,1,System.nanoTime()-1);
   ok(db.queries.size()==before,"EXHAUSTED_BUDGET_DOES_NOT_QUERY");ok(exhausted.training.contains("时间已用完"),"EXHAUSTED_NOT_ZERO");
   ok(ResearchPipelineStatusR76.read(null).training.contains("尚未就绪"),"NULL_STORE_NOT_ZERO");
   ok(ResearchPipelineStatusR76.reason(null).contains("尚无"),"NULL_REASON_NOT_SUCCESS");
   ok(ResearchPipelineStatusR76.reason("PERIOD_PENDING|REVIEW_ONLY").contains("尚不能代表数值训练完成"),"MAINTENANCE_NOT_NUMERIC_SUCCESS");
   ok(ResearchPipelineStatusR76.reason("TRAIN_FEATURE_PAGE").contains("等待继续训练"),"FEATURE_PAGE_NOT_TRAINING_COMPLETION");
   Map<String,Object> out=new LinkedHashMap<>();out.put("status","PASS");out.put("checks",checks);out.put("mode",a[2]);out.put("elapsedMs",(System.nanoTime()-start)/1_000_000L);out.put("longestQueryMs",db.longestNs/1_000_000L);out.put("queryPlans",plans);out.put("readOnly",true);
   Files.writeString(Path.of(a[3]+".json"),StrictCanonicalJson.writeContext(out));System.out.println("PASS checks="+checks);
  }
 }
}
