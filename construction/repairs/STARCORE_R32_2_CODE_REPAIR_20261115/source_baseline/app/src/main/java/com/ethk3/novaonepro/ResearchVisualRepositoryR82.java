package com.ethk3.novaonepro;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.os.CancellationSignal;
import android.os.OperationCanceledException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import org.json.JSONArray;
import org.json.JSONObject;

/** Bounded diagnostics only. Call off the UI thread on a dedicated OPEN_READONLY database. */
public final class ResearchVisualRepositoryR82 {
    private ResearchVisualRepositoryR82() { }
    private static final long BUDGET_NS=750000000L;
    private static final ScheduledExecutorService TIMER=Executors.newSingleThreadScheduledExecutor(new ThreadFactory(){public Thread newThread(Runnable r){Thread t=new Thread(r,"research-chart-deadline");t.setDaemon(true);return t;}});
    private interface Read { ResearchVisualDataR82.Panel run(); }
    private static final class Context {
        SQLiteDatabase db;CancellationSignal cancel;long deadline;String asOf,id,key,learner,version;
        void check(){cancel.throwIfCanceled();if(System.nanoTime()>=deadline)throw new OperationCanceledException("CHART_BUDGET");}
        Cursor query(String sql,String[] args){check();return db.rawQuery(sql,args,cancel);}
    }
    public static ResearchVisualDataR82.Dashboard load(SQLiteDatabase db,String page,String asOf,String exactCandidate,CancellationSignal cancellation){
        if(db==null||!db.isOpen()||!db.isReadOnly()||db.inTransaction())throw new IllegalArgumentException("DEDICATED_READ_ONLY_DB_REQUIRED");
        if(!("overview".equals(page)||"deep".equals(page)||"evolution".equals(page)||"creation".equals(page)||"atoms".equals(page)))throw new IllegalArgumentException("CHART_PAGE");
        Context x=new Context();x.db=db;x.cancel=cancellation==null?new CancellationSignal():cancellation;x.deadline=System.nanoTime()+BUDGET_NS;
        ScheduledFuture<?> timeout=TIMER.schedule(new Runnable(){public void run(){x.cancel.cancel();}},750,TimeUnit.MILLISECONDS);
        List<ResearchVisualDataR82.Panel> panels=new ArrayList<ResearchVisualDataR82.Panel>();String status="READY",note="只读诊断；图表不授予资格，不代替独立Future验收。";String initial="";long generated=System.currentTimeMillis();
        try {
            initial=version(x);x.version=initial;
            String latest=scalar(x,"SELECT COALESCE(MAX(issue),'') FROM "+DbHelper.TABLE_DRAWS+" WHERE dirty=0",null);
            x.asOf=asOf==null||asOf.length()==0?latest:asOf;if(!latest.isEmpty()&&x.asOf.compareTo(latest)>0)x.asOf=latest;
            if(x.asOf.length()>0&&!x.asOf.matches("[0-9]{12}"))throw new IllegalArgumentException("CHART_AS_OF_ISSUE");
            String requested=exactCandidate==null?"":exactCandidate.trim();if(requested.length()>266)throw new IllegalArgumentException("CHART_CANDIDATE_LENGTH");
            String raw=requested.startsWith("algorithm:")?requested.substring(10):requested;x.id="";x.learner="";
            String frozenSql="SELECT candidate_id,learner_id FROM "+ResearchEvaluationRunR967.ALL+" WHERE created_issue<=?"+(raw.isEmpty()?"":" AND (candidate_id=? OR learner_id=?)")+" ORDER BY created_issue DESC,candidate_id LIMIT 1";
            try(Cursor f=x.query(frozenSql,raw.isEmpty()?new String[]{x.asOf}:new String[]{x.asOf,raw,raw})){if(f.moveToFirst()){x.id=f.getString(0);x.learner=f.getString(1);}}
            if(x.learner.isEmpty())x.learner=raw.isEmpty()?scalar(x,"SELECT candidate_id FROM r952_candidate WHERE created_issue<=? ORDER BY created_issue DESC,candidate_id LIMIT 1",new String[]{x.asOf}):raw;
            x.key=x.id.isEmpty()?"":"algorithm:"+x.id;
            if(wants(page,"deep")){
                add(x,panels,"deep","训练更新","r952_artifact",new Read(){public ResearchVisualDataR82.Panel run(){return training(x);}});
                add(x,panels,"deep","校准验证","research_candidate_cal_model_r932",new Read(){public ResearchVisualDataR82.Panel run(){return calibration(x);}});
                add(x,panels,"deep","冻结输入稳定性","research_candidate_execution_r932",new Read(){public ResearchVisualDataR82.Panel run(){return execution(x,false);}});
                add(x,panels,"deep","Future概率可靠度","research_formal_observation_cp12",new Read(){public ResearchVisualDataR82.Panel run(){return reliability(x);}});
            }
            if(wants(page,"evolution")){
                add(x,panels,"evolution","共同Future配对差值","research_formal_observation_cp12",new Read(){public ResearchVisualDataR82.Panel run(){return paired(x);}});
                add(x,panels,"evolution","实际主链贡献","v57_research_mainline_contribution_r922",new Read(){public ResearchVisualDataR82.Panel run(){return contribution(x);}});
                add(x,panels,"evolution","动作与独立端点结果","research_action_v2/research_result_v2",new Read(){public ResearchVisualDataR82.Panel run(){return results(x);}});
            }
            if(wants(page,"creation")){
                add(x,panels,"creation","真实父子谱系","r952_candidate",new Read(){public ResearchVisualDataR82.Panel run(){return lineage(x);}});
                add(x,panels,"creation","教师冻结学习见证","research_child_teacher_lineage_cp12",new Read(){public ResearchVisualDataR82.Panel run(){return teachers(x);}});
            }
            if(wants(page,"atoms")){
                add(x,panels,"atoms","原子样本与缺失","atomic_tool_quality_r943",new Read(){public ResearchVisualDataR82.Panel run(){return atoms(x,false);}});
                add(x,panels,"atoms","原子原始实测成本","research_candidate_execution_r932.atom_cost_r82",new Read(){public ResearchVisualDataR82.Panel run(){return execution(x,true);}});
                add(x,panels,"atoms","原子Brier增益","atomic_tool_quality_r943",new Read(){public ResearchVisualDataR82.Panel run(){return atoms(x,true);}});
            }
            x.check();if(!initial.equals(version(x))){panels.clear();status="STALE_RETRY";note="读取期间数据库版本发生变化；本次图表整体丢弃，请刷新。";}
        }catch(OperationCanceledException e){panels.clear();status=System.nanoTime()>=x.deadline?"BUDGET_EXCEEDED":"CANCELLED";note="图表读取已终止，未返回部分水位数据。";}
        catch(RuntimeException e){panels.clear();status="UNAVAILABLE";note="图表快照不可用："+e.getClass().getSimpleName();}
        finally{timeout.cancel(false);}
        String identity=page+'|'+safe(x.asOf)+'|'+safe(x.key)+'|'+initial+'|'+generated;
        for(ResearchVisualDataR82.Panel p:panels){identity+='|'+p.page+'|'+p.title+'|'+p.status;for(ResearchVisualDataR82.Point pnt:p.points)identity+='|'+pnt.label+'|'+Double.toHexString(pnt.value)+'|'+pnt.details;}
        return new ResearchVisualDataR82.Dashboard(page,status,safe(x.asOf),safe(x.id),safe(x.key),initial,"R82_READ_ONLY_BOUNDED_V1",note,V51ChecksumEngine.sha256(identity),generated,panels);
    }
    private static boolean wants(String requested,String page){return "overview".equals(requested)||page.equals(requested);}
    private static String safe(String s){return s==null?"":s;}
    private static String version(Context x){return scalar(x,"PRAGMA data_version",null)+":"+scalar(x,"PRAGMA schema_version",null)+":"+scalar(x,"SELECT total_changes()",null);}
    private static String scalar(Context x,String sql,String[]args){try(Cursor c=x.query(sql,args)){x.check();return c.moveToFirst()&&!c.isNull(0)?c.getString(0):"";}}
    private static void add(Context x,List<ResearchVisualDataR82.Panel>out,String page,String title,String source,Read read){
        x.check();try{out.add(read.run());}catch(OperationCanceledException e){throw e;}catch(SQLiteException e){out.add(panel(page,title,"STATUS",source,"", "UNAVAILABLE","当前库缺少此项所需表或列；其他面板独立读取。",Collections.<ResearchVisualDataR82.Point>emptyList(),Collections.<ResearchVisualDataR82.Edge>emptyList()));}
    }
    private static ResearchVisualDataR82.Panel panel(String page,String title,String kind,String source,String window,String status,String note,List<ResearchVisualDataR82.Point>points,List<ResearchVisualDataR82.Edge>edges){return new ResearchVisualDataR82.Panel(page,title,kind,source,window,note,points.isEmpty()&&edges.isEmpty()&&"READY".equals(status)?"EMPTY":status,points,edges);}
    private static ResearchVisualDataR82.Point point(String label,double value,long n,String detail){return new ResearchVisualDataR82.Point(label,value,Double.NaN,Double.NaN,n,detail);}
    private static ResearchVisualDataR82.Panel training(Context x){
        List<ResearchVisualDataR82.Point>p=new ArrayList<ResearchVisualDataR82.Point>();
        try(Cursor c=x.query("SELECT target_issue,numeric_updates,payload_hash,artifact_hash FROM r952_artifact WHERE candidate_id=? AND target_issue<=? ORDER BY target_issue DESC,artifact_hash LIMIT 120",new String[]{x.learner,x.asOf})){while(c.moveToNext()){x.check();p.add(point(c.getString(0),c.getLong(1),c.getLong(1),"learner="+x.learner+"；model_hash="+c.getString(2)+"；artifact="+c.getString(3)+"；这是累计参数更新数，不是胜率或资格。"));}}
        Collections.reverse(p);return panel("deep","训练参数更新","BAR","r952_artifact","最近120个产物","READY","每点使用独立模型版本，跨版本不连线；无产物即空态。",p,Collections.<ResearchVisualDataR82.Edge>emptyList());
    }
    private static ResearchVisualDataR82.Panel calibration(Context x){
        List<ResearchVisualDataR82.Point>p=new ArrayList<ResearchVisualDataR82.Point>();String note="旧模型仅有箱计数时画BAR；原始均值/命中/Wilson齐备才画可靠度。";String status="READY",kind="BAR";
        try(Cursor c=x.query("SELECT m.model_json,m.model_hash,m.validation_until,q.state,q.reason,q.ece,q.brier,q.baseline_brier FROM research_candidate_cal_model_r932 m LEFT JOIN research_calibration_quality_cp12 q ON q.study_hash=m.study_hash AND q.model_hash=m.model_hash WHERE m.candidate_key=? AND m.validation_until<=? AND length(CAST(m.model_json AS BLOB))<=65536 ORDER BY m.validation_until DESC,m.study_hash LIMIT 1",new String[]{x.key,x.asOf})){if(c.moveToFirst()){
            x.check();String json=c.getString(0),modelHash=c.getString(1);if(!modelHash.equals(V51ChecksumEngine.sha256(json)))return panel("deep","校准验证","STATUS","research_candidate_cal_model_r932","外层120","UNAVAILABLE","模型哈希不匹配，拒绝绘图。",p,Collections.<ResearchVisualDataR82.Edge>emptyList());
            String detail="model_hash="+modelHash+"；validation_until="+c.getString(2)+"；state="+safe(c.getString(3))+"；reason="+safe(c.getString(4));
            try{
                JSONObject j=new JSONObject(json);String comparison=String.valueOf(j.optJSONArray("comparison"));if(comparison.length()>1600)comparison=comparison.substring(0,1600)+"…";
                detail+="；selected_kind="+j.optString("selected_kind","LEGACY_LOGISTIC")+"；earlier_fit_comparison="+comparison+"；outer_validation_n="+j.optInt("validation_n",120)+"；ECE="+j.optDouble("validation_ece",Double.NaN)+"；Brier="+j.optDouble("validation_brier",Double.NaN)+"；NLL="+j.optDouble("validation_nll",Double.NaN)+"；champion_probability_n="+j.optInt("validation_champion_probability_n",-1)+"；champion_status="+j.optString("validation_champion_probability_status","UNKNOWN_ORIGINAL_PROBABILITY");
                int championN=j.optInt("validation_champion_probability_n",-1);if(championN==120&&!j.isNull("validation_champion_brier")&&!j.isNull("validation_champion_nll"))detail+="；champion_Brier="+j.getDouble("validation_champion_brier")+"；champion_NLL="+j.getDouble("validation_champion_nll");
                JSONArray counts=j.optJSONArray("validation_bin_counts"),sumP=j.optJSONArray("validation_bin_sum_p"),sumY=j.optJSONArray("validation_bin_sum_y"),lower=j.optJSONArray("validation_bin_wilson95_lower"),upper=j.optJSONArray("validation_bin_wilson95_upper");
                if(counts!=null){
                    if(counts.length()!=10)throw new IllegalArgumentException("CAL_BIN_COUNT");int total=0;for(int i=0;i<10;i++){int n=counts.getInt(i);if(n<0||n>120)throw new IllegalArgumentException("CAL_BIN_N");total+=n;}if(total!=120)throw new IllegalArgumentException("CAL_BIN_TOTAL");
                    boolean complete=sumP!=null&&sumY!=null&&lower!=null&&upper!=null;
                    if(complete){if(sumP.length()!=10||sumY.length()!=10||lower.length()!=10||upper.length()!=10)throw new IllegalArgumentException("CAL_BIN_EVIDENCE_LENGTH");kind="RELIABILITY";}
                    for(int i=0;i<10;i++){int n=counts.getInt(i);if(!complete){p.add(point("验证箱"+i+"数量",n,n,detail+"；原模型只有计数，不生成比例。"));continue;}
                        double ps=sumP.getDouble(i),ys=sumY.getDouble(i);if(ps<0||ps>n||ys<0||ys>n||ys!=Math.rint(ys))throw new IllegalArgumentException("CAL_BIN_EVIDENCE_RANGE");double[]ci=ResearchResultMetricsR81.wilson((int)ys,n);double lo=n==0?Double.NaN:lower.getDouble(i),hi=n==0?Double.NaN:upper.getDouble(i);if(n>0&&(!unit(lo)||!unit(hi)||Math.abs(lo-ci[0])>1e-9||Math.abs(hi-ci[1])>1e-9))throw new IllegalArgumentException("CAL_BIN_WILSON_MISMATCH");
                        p.add(new ResearchVisualDataR82.Point("验证箱"+i+":"+(n==0?"NaN":Double.toString(ps/n)),n==0?Double.NaN:ys/n,lo,hi,n,detail+"；mean_predicted="+(n==0?"UNKNOWN":Double.toString(ps/n)),Double.NaN,n==0?Double.NaN:ps/n));
                    }
                }else if(!c.isNull(5)){p.add(point("ECE",c.getDouble(5),120,detail));p.add(point("Brier",c.getDouble(6),120,detail));p.add(point("基线Brier",c.getDouble(7),120,detail));}
            }catch(Exception bad){p.clear();status="UNAVAILABLE";note="校准JSON证据无效，拒绝推造或混用可靠度曲线。";}
        }}return panel("deep",kind.equals("RELIABILITY")?"外层120校准可靠度":"校准指标或验证箱计数",kind,"research_candidate_cal_model_r932","外层120",status,note,p,Collections.<ResearchVisualDataR82.Edge>emptyList());
    }
    private static String latestLife(Context x){return scalar(x,"SELECT lifetime_id FROM research_cp4_frozen_lifetime WHERE candidate_key=? AND first_issue<=? ORDER BY first_issue DESC,lifetime_id LIMIT 1",new String[]{x.key,x.asOf});}
    private static ResearchVisualDataR82.Panel reliability(Context x){
        String life=latestLife(x);int[]n=new int[10],hits=new int[10];double[]sum=new double[10];String first="",last="";
        try(Cursor c=x.query("SELECT target_issue,selected_hit_p,hit FROM research_formal_observation_cp12 WHERE lifetime_id=? AND target_issue<=? ORDER BY target_issue DESC LIMIT 120",new String[]{life,x.asOf})){while(c.moveToNext()){x.check();double pr=c.getDouble(1);int h=c.getInt(2);if(!unit(pr)||(h!=0&&h!=1))continue;if(last.isEmpty())last=c.getString(0);first=c.getString(0);int b=Math.min(9,(int)(pr*10));n[b]++;hits[b]+=h;sum[b]+=pr;}}
        List<ResearchVisualDataR82.Point>p=new ArrayList<ResearchVisualDataR82.Point>();for(int i=0;i<10;i++){double[]w=ResearchResultMetricsR81.wilson(hits[i],n[i]);p.add(new ResearchVisualDataR82.Point("箱"+i+":"+(n[i]==0?"NaN":Double.toString(sum[i]/n[i])),n[i]==0?Double.NaN:(double)hits[i]/n[i],w[0],w[1],n[i],"lifetime="+life+"；window="+first+"—"+last+"；mean_predicted="+(n[i]==0?"UNKNOWN":Double.toString(sum[i]/n[i])),Double.NaN,n[i]==0?Double.NaN:sum[i]/n[i]));}
        return panel("deep","同一冻结版本Future可靠度","RELIABILITY","research_formal_observation_cp12",first+"—"+last,first.isEmpty()?"EMPTY":"READY","各箱Wilson95%区间；空箱NaN，不补0；最多120个共同版本观察。",p,Collections.<ResearchVisualDataR82.Edge>emptyList());
    }
    private static ResearchVisualDataR82.Panel paired(Context x){
        List<ResearchVisualDataR82.Point>p=new ArrayList<ResearchVisualDataR82.Point>();
        try(Cursor c=x.query("SELECT a.target_issue,a.hit-b.hit,a.source_hash,b.source_hash,a.settlement_hash,a.actual_digit,a.label_hash,b.label_hash FROM research_formal_observation_cp12 a JOIN research_action_result_binding_cp12 r ON r.child_lifetime_id=a.lifetime_id AND r.comparator_kind='PARENT_LIFETIME' JOIN research_formal_observation_cp12 b ON b.lifetime_id=r.parent_lifetime_id AND b.target_issue=a.target_issue AND b.position=a.position AND b.actual_digit=a.actual_digit AND b.settlement_hash=a.settlement_hash WHERE a.lifetime_id=? AND a.target_issue<=? GROUP BY a.target_issue,a.source_hash,b.source_hash ORDER BY a.target_issue DESC LIMIT 120",new String[]{latestLife(x),x.asOf})){while(c.moveToNext()){x.check();String childLabel=V51ChecksumEngine.sha256("R958_EVAL_LABEL|"+c.getString(2)+'|'+c.getString(4)+'|'+c.getInt(5)),parentLabel=V51ChecksumEngine.sha256("R958_EVAL_LABEL|"+c.getString(3)+'|'+c.getString(4)+'|'+c.getInt(5));if(!childLabel.equals(c.getString(6))||!parentLabel.equals(c.getString(7)))continue;p.add(point(c.getString(0),c.getDouble(1),1,"共同Future单期命中差[-1,0,1]；child_source="+c.getString(2)+"；parent_source="+c.getString(3)+"；不等于已确认提升。"));}}
        Collections.reverse(p);return panel("evolution","同位置共同Future差值","BAR","research_formal_observation_cp12","最近120对","READY","只配对同期、同位置、同真实标签的正式冻结记录；缺配对不画。",p,Collections.<ResearchVisualDataR82.Edge>emptyList());
    }
    private static ResearchVisualDataR82.Panel contribution(Context x){
        List<ResearchVisualDataR82.Point>p=new ArrayList<ResearchVisualDataR82.Point>();
        try(Cursor c=x.query("SELECT target_issue,influence_weight,applied,status,evidence_hash,paired_n,paired_delta FROM v57_research_mainline_contribution_r922 WHERE candidate_id=? AND target_issue<=? ORDER BY target_issue DESC LIMIT 120",new String[]{x.id,x.asOf})){while(c.moveToNext()){x.check();p.add(point(c.getString(0),c.getDouble(1),c.getLong(5),"applied="+c.getInt(2)+"；status="+c.getString(3)+"；paired_delta="+c.getDouble(6)+"；evidence="+c.getString(4)+"；applied不是REAL授权。"));}}
        Collections.reverse(p);return panel("evolution","实际主链影响权重","BAR","v57_research_mainline_contribution_r922","最近120期","READY","只呈现已记录的真实贡献，不把候选存在算作应用。",p,Collections.<ResearchVisualDataR82.Edge>emptyList());
    }
    private static ResearchVisualDataR82.Panel results(Context x){
        List<ResearchVisualDataR82.Point>p=new ArrayList<ResearchVisualDataR82.Point>();
        try(Cursor c=x.query("SELECT a.action_id,a.action_type,a.operator,a.state,a.last_error,r.endpoint_n,r.paired_n,r.verdict,r.evidence_hash FROM research_action_v2 a JOIN research_finding_v2 f ON f.finding_id=a.finding_id LEFT JOIN research_result_v2 r ON r.action_id=a.action_id WHERE (a.child_id=? OR f.candidate_version=?) AND f.source_issue<=? ORDER BY f.source_issue DESC,a.action_id,r.endpoint_n DESC LIMIT 120",new String[]{x.learner,x.key,x.asOf})){while(c.moveToNext()){x.check();p.add(point(c.getString(0),c.isNull(6)?Double.NaN:c.getDouble(6),c.isNull(6)?0:c.getLong(6),"type="+c.getString(1)+"；operator="+c.getString(2)+"；state="+c.getString(3)+"；reason="+c.getString(4)+"；endpoint="+c.getString(5)+"；verdict="+c.getString(7)+"；evidence="+c.getString(8)+"；NO_OP不增代。"));}}
        return panel("evolution","动作及120/360独立端点","STATUS","research_action_v2/research_result_v2","最近120条","READY","固定端点样本、阻断原因和实际结论；无结果保持未知。",p,Collections.<ResearchVisualDataR82.Edge>emptyList());
    }
    private static ResearchVisualDataR82.Panel lineage(Context x){
        List<ResearchVisualDataR82.Point>p=new ArrayList<ResearchVisualDataR82.Point>();List<ResearchVisualDataR82.Edge>e=new ArrayList<ResearchVisualDataR82.Edge>();
        try(Cursor c=x.query("SELECT candidate_id,parent_id,state,created_issue,structural_hash,future_n FROM r952_candidate WHERE created_issue<=? AND (candidate_id=? OR parent_id=? OR candidate_id=(SELECT parent_id FROM r952_candidate WHERE candidate_id=?)) ORDER BY created_issue DESC,candidate_id LIMIT 60",new String[]{x.asOf,x.learner,x.learner,x.learner})){while(c.moveToNext()){x.check();String id=c.getString(0),parent=c.getString(1);p.add(point(id,Double.NaN,c.getLong(5),"state="+c.getString(2)+"；created_issue="+c.getString(3)+"；structure="+c.getString(4)));if(parent!=null&&!parent.isEmpty())e.add(new ResearchVisualDataR82.Edge(parent,id,"registered parent"));}}
        return panel("creation","直接父子谱系","GRAPH","r952_candidate","当前候选与直接子代≤60","READY","边来自真实parent_id；不把世代数当质量，不伪造变异/融合。",p,e);
    }
    private static ResearchVisualDataR82.Panel teachers(Context x){
        List<ResearchVisualDataR82.Point>p=new ArrayList<ResearchVisualDataR82.Point>();List<ResearchVisualDataR82.Edge>e=new ArrayList<ResearchVisualDataR82.Edge>();
        try(Cursor c=x.query("SELECT teacher_candidate_id,teacher_freeze_hash,teacher_model_hash,teacher_manifest_hash,witness_issue,receipt_hash FROM research_child_teacher_lineage_cp12 WHERE child_id=? AND witness_issue<=? ORDER BY witness_issue DESC,teacher_freeze_hash LIMIT 60",new String[]{x.learner,x.asOf})){while(c.moveToNext()){x.check();String teacher=c.getString(0);p.add(point(teacher,Double.NaN,1,"witness_issue="+c.getString(4)+"；teacher="+teacher+"；freeze="+c.getString(1)+"；model="+c.getString(2)+"；manifest="+c.getString(3)+"；receipt="+c.getString(5)));e.add(new ResearchVisualDataR82.Edge(teacher,x.learner,"冻结教师学习见证"));}}
        if(!p.isEmpty())p.add(point(x.learner,Double.NaN,-1,"child_learner="+x.learner+"；独立Future不能继承教师成绩。"));
        return panel("creation","教师学习冻结见证","GRAPH","research_child_teacher_lineage_cp12","最近60条","READY","只读见证不等于教师提升已证实；必须独立Future比较。",p,e);
    }
    private static ResearchVisualDataR82.Panel atoms(Context x,boolean gain){
        List<ResearchVisualDataR82.Point>p=new ArrayList<ResearchVisualDataR82.Point>();
        try(Cursor c=x.query("SELECT atom_id,n,missing,gain_sum,state,last_issue FROM atomic_tool_quality_r943 WHERE last_issue<=? ORDER BY atom_index LIMIT 120",new String[]{x.asOf})){while(c.moveToNext()){x.check();long n=c.getLong(1),missing=c.getLong(2);double value=gain?(n>0?c.getDouble(3)/n:Double.NaN):(n+missing>0?(double)missing/(n+missing):Double.NaN);p.add(point(c.getString(0),value,gain?n:n+missing,"n="+n+"；missing="+missing+"；state="+c.getString(4)+"；last_issue="+c.getString(5)+"；"+(gain?"Brier增益，不是命中率。":"缺失率分母为有效n+missing。")));}}
        return panel("atoms",gain?"原子平均Brier增益":"原子缺失率","BAR","atomic_tool_quality_r943","前120个已记录原子","READY","n=0保留NaN；原子工具质量不直接授予主链证书。",p,Collections.<ResearchVisualDataR82.Edge>emptyList());
    }
    private static ResearchVisualDataR82.Panel execution(Context x,boolean atomCosts){
        String page=atomCosts?"atoms":"deep",title=atomCosts?"逐原子原始实测成本(ns)":"冻结输入扰动敏感度",source=atomCosts?"research_candidate_execution_r932.atom_cost_r82":"research_candidate_execution_r932";
        List<ResearchVisualDataR82.Point>points=new ArrayList<ResearchVisualDataR82.Point>();String status="READY",note="仅消费原冻结execution proof内诊断；缺原测量为NaN。";
        try(Cursor c=x.query("SELECT payload,proof_hash,target_issue,prediction_hash,structural_hash,input_hash FROM research_candidate_execution_r932 WHERE candidate_key=? AND target_issue<=? AND length(CAST(payload AS BLOB))<=65536 ORDER BY target_issue DESC LIMIT "+(atomCosts?"1":"20"),new String[]{x.key,x.asOf})){
            while(c.moveToNext()){
                x.check();String payload=c.getString(0),proof=c.getString(1),target=c.getString(2);
                try{
                    JSONObject j=new JSONObject(payload);if(!proof.equals(V51ChecksumEngine.sha256(payload))||!x.key.equals(j.getString("candidate_key"))||!target.equals(j.getString("target_issue"))||!c.getString(3).equals(j.getString("prediction_hash"))||!c.getString(4).equals(j.getString("structural_hash"))||!c.getString(5).equals(j.getString("input_hash")))throw new IllegalArgumentException("EXECUTION_ORIGINAL_IDENTITY");
                    String field=atomCosts?"atom_cost_r82":"frozen_input_stability_r82",hashField=atomCosts?"atom_cost_sha256_r82":"frozen_input_stability_sha256";JSONObject diagnostic=j.optJSONObject(field);
                    if(diagnostic==null){points.add(point(target,Double.NaN,-1,"原冻结记录未采集"+field+"；execution_proof="+proof));continue;}
                    if(!V51ChecksumEngine.sha256(diagnostic.toString()).equals(j.getString(hashField)))throw new IllegalArgumentException("EXECUTION_DIAGNOSTIC_HASH");
                    if(atomCosts){
                        if(!target.equals(diagnostic.getString("target_issue")))throw new IllegalArgumentException("ATOM_COST_TARGET");JSONArray rows=diagnostic.getJSONArray("records");int cap=Math.min(120,rows.length());
                        for(int i=0;i<cap;i++){x.check();JSONObject r=rows.getJSONObject(i);String maxSource=r.optString("max_source_issue","");if(!target.equals(r.getString("target_issue"))||(!maxSource.isEmpty()&&maxSource.compareTo(target)>=0))throw new IllegalArgumentException("ATOM_COST_PIT");double cost=r.isNull("cost_ns")?Double.NaN:r.getDouble("cost_ns");if(!Double.isNaN(cost)&&(!(cost>0)||Double.isInfinite(cost)))throw new IllegalArgumentException("ATOM_COST_RANGE");points.add(point(r.getString("atom_id"),cost,Double.isNaN(cost)?-1:1,"issue="+target+"；spec_hash="+r.optString("spec_hash","")+"；node_id="+r.optString("node_id","")+"；input_hash="+r.optString("input_hash","")+"；output_hash="+r.optString("output_hash","")+"；max_source_issue="+maxSource+"；status="+r.optString("status","")+"；execution_proof="+proof));}
                        note="仅单原子原始evaluate计时，不含准备和其他节点。展示前"+cap+"/"+rows.length()+"项；无测量留空，完整记录见execution proof。";
                    }else{
                        long attempted=diagnostic.optLong("attempted_n",-1);double value=diagnostic.isNull("max_l1")?Double.NaN:diagnostic.getDouble("max_l1");String observations=String.valueOf(diagnostic.optJSONArray("observations"));if(observations.length()>1600)observations=observations.substring(0,1600)+"…";
                        points.add(point(target,value,attempted,"status="+diagnostic.optString("status","")+"；missing_reason="+diagnostic.optString("missing_reason","")+"；model_hash="+diagnostic.optString("model_hash","")+"；input_hash="+diagnostic.optString("input_hash","")+"；max_selected_k3_mass_abs_delta="+String.valueOf(diagnostic.opt("max_selected_k3_mass_abs_delta"))+"；observations="+observations+"；selected_raw_score="+String.valueOf(j.opt("selected_score"))+"；execution_proof="+proof+"；这是同输入扰动敏感度，不是提升或因果结论。"));
                    }
                }catch(Exception bad){points.clear();status="UNAVAILABLE";note="原冻结execution或内嵌诊断hash/身份无效，拒绝绘图。";break;}
            }
        }
        if(!atomCosts)Collections.reverse(points);return panel(page,title,"BAR",source,atomCosts?"最近1个冻结上下文≤120原子":"最近20个同冻结候选上下文",status,note,points,Collections.<ResearchVisualDataR82.Edge>emptyList());
    }
    private static boolean unit(double x){return !Double.isNaN(x)&&!Double.isInfinite(x)&&x>=0&&x<=1;}
}
