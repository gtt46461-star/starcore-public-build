package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.BinanceGameContract;
import com.ethk3.novaonepro.KeyProvider;
import java.util.*;

/** All rates originate in immutable freeze/settlement ledgers. No training score enters this ranking. */
public final class ResearchDashboardSU1 {
    private ResearchDashboardSU1() {}
    private static long scalar(SQLiteDatabase db,String sql,String...args){try(Cursor c=db.rawQuery(sql,args)){return c.moveToFirst()&&!c.isNull(0)?c.getLong(0):0;}}
    private static String shortId(String id){return id.substring(0,Math.min(10,id.length()));}
    private static String percent(long n,long denominator){return denominator==0?"未知":String.format(Locale.CHINA,"%.2f%%",100d*n/denominator);}
    private static final class Row {
        String id,text,arm,state;long born,attempts,frozen,gates,budgets,failed,n,k,calendar;int depth;
        double lower(){return n==0?0:FoldStabilityFitnessSU1.wilsonLower(Math.toIntExact(k),Math.toIntExact(n));}
    }
    public static String render(SQLiteDatabase db){
        BudgetTokenSU1 budget=new BudgetTokenSU1(3000,2000,()->Thread.currentThread().isInterrupted());
        StringBuilder out=new StringBuilder("接口\n").append(BinanceGameContract.primary().endpoint()).append('\n')
                .append(BinanceGameContract.backup().endpoint()).append('\n')
                .append(KeyProvider.isKeyReady()?"访问凭据已就绪":"地址已内置；访问凭据尚未就绪").append("\n\n");
        try(Cursor c=db.rawQuery("SELECT mode,reason FROM su1_runtime_control WHERE singleton=1",null)){
            if(c.moveToFirst())out.append("研究状态：").append(c.getString(0)).append(" · ").append(c.getString(1)).append('\n');
        }
        out.append("历史活动：").append(scalar(db,"SELECT count(*) FROM su1_campaign"))
                .append("；已完成实际检验：").append(scalar(db,"SELECT count(*) FROM su1_search_attempt WHERE result='EVALUATED'"))
                .append("；拒绝：").append(scalar(db,"SELECT count(*) FROM su1_search_attempt WHERE result='REJECTED'")).append('\n');
        out.append("廉价开发检验已完成 ").append(scalar(db,"SELECT count(*) FROM su1_search_attempt WHERE detail LIKE '%SCOUT_N=%'"))
                .append("；廉价检验拒绝 ").append(scalar(db,"SELECT count(*) FROM su1_search_attempt WHERE result='REJECTED' AND detail LIKE '%SCOUT_UPPER_BELOW025%'")).append("（仅开发筛查，不计正式证据）\n");
        out.append("已提炼结构：").append(scalar(db,"SELECT count(*) FROM su1_macro WHERE state='ACTIVE'"))
                .append("；新家族：").append(scalar(db,"SELECT count(*) FROM su1_emergent_family")).append("\n\n");

        List<Row> rows=new ArrayList<>();long last=scalar(db,"SELECT max(sequence_ordinal) FROM su1_live_target");
        String sql="SELECT r.strategy_id,r.canonical_text,p.arm,p.state,p.born_sequence,coalesce(d.n,0)+coalesce(s.n,0),coalesce(d.k,0)+coalesce(s.k,0),coalesce(a.attempts,0),coalesce(a.frozen,0),coalesce(a.gates,0),coalesce(a.budgets,0),coalesce(a.failed,0) FROM su1_research_pool p JOIN su1_runtime_program r ON r.strategy_id=p.strategy_id LEFT JOIN (SELECT strategy_id,sum(actions) n,sum(hits) k FROM su1_live_daily GROUP BY strategy_id)d ON d.strategy_id=r.strategy_id LEFT JOIN (SELECT strategy_id,count(*) n,sum(hit) k FROM su1_live_shadow WHERE hit IS NOT NULL AND NOT EXISTS(SELECT 1 FROM su1_live_daily q WHERE q.strategy_id=su1_live_shadow.strategy_id AND q.day_key=su1_live_shadow.day_key) GROUP BY strategy_id)s ON s.strategy_id=r.strategy_id LEFT JOIN (SELECT strategy_id,count(*) attempts,sum(outcome='FROZEN') frozen,sum(outcome='GATE_SKIP') gates,sum(outcome='BUDGET_SKIP') budgets,sum(outcome='COMPUTE_FAILED') failed FROM su1_live_attempt GROUP BY strategy_id)a ON a.strategy_id=r.strategy_id WHERE p.state='ACTIVE' ORDER BY r.strategy_id LIMIT 32";
        try(Cursor c=db.rawQuery(sql,null)){while(c.moveToNext()){
            budget.checkpointOrYield();Row row=new Row();row.id=c.getString(0);row.text=c.getString(1);row.arm=c.getString(2);row.state=c.getString(3);row.born=c.getLong(4);row.n=c.getLong(5);row.k=c.getLong(6);row.attempts=c.getLong(7);row.frozen=c.getLong(8);row.gates=c.getLong(9);row.budgets=c.getLong(10);row.failed=c.getLong(11);row.calendar=Math.max(0,last-row.born+1);
            row.depth=(int)scalar(db,"WITH RECURSIVE ancestry(id,depth) AS (SELECT ?,0 UNION SELECT l.parent_strategy_id,a.depth+1 FROM su1_lineage l JOIN ancestry a ON l.child_strategy_id=a.id WHERE a.depth<63 LIMIT 4096) SELECT max(depth) FROM ancestry",row.id);rows.add(row);
        }}
        rows.sort(Comparator.comparingDouble(Row::lower).reversed().thenComparing(Comparator.comparingLong((Row r)->r.n).reversed()).thenComparing(r->r.id));
        out.append("实时研究池排行（").append(rows.size()).append(" / 32）\n仅按真实结算的描述性下界排序；90%区间不授予正式资格。\n冻结覆盖=冻结数/已观察窗口内日历机会；结算率=结算数/冻结数。\n\n");
        int rank=0;
        for(Row r:rows){
            budget.checkpointOrYield();out.append(++rank).append(" · ").append(shortId(r.id)).append(" · ").append(r.arm).append(" · ").append(r.state).append('\n');
            out.append("命中 ").append(r.k).append(" / 结算 ").append(r.n).append(" = ").append(percent(r.k,r.n));
            if(r.n>0){double hi=1-FoldStabilityFitnessSU1.wilsonLower(Math.toIntExact(r.n-r.k),Math.toIntExact(r.n));out.append(String.format(Locale.CHINA,"；90%%区间 [%.2f%%, %.2f%%]",100*r.lower(),100*hi));}
            out.append('\n').append("日历机会 ").append(r.calendar).append("；已尝试 ").append(r.attempts).append("；冻结 ").append(r.frozen).append("；待结算 ").append(Math.max(0,r.frozen-r.n)).append('\n')
                    .append("覆盖 ").append(percent(r.frozen,r.calendar)).append("；结算率 ").append(percent(r.n,r.frozen)).append("；门控拒绝 ").append(r.gates).append("；预算拒绝 ").append(r.budgets).append("；计算失败 ").append(r.failed).append('\n')
                    .append("记录谱系深度 ").append(r.depth).append("；版本 ").append(shortId(r.id)).append('\n').append(r.text).append("\n\n");
        }
        out.append("独立未来验证\n已锁定 ").append(scalar(db,"SELECT count(*) FROM su1_sealed_plan")).append("；已完成 ").append(scalar(db,"SELECT count(*) FROM su1_sealed_result")).append('\n');
        String formal="SELECT c.strategy_id,c.claim_kind,c.evidence_role,e.planned_opportunities,e.paired_n,e.unpaired_n,e.child_hits,e.lower_bound,e.coverage,e.result_state FROM su1_evidence_look e JOIN su1_eval_cohort c ON c.cohort_id=e.cohort_id ORDER BY e.recorded_at DESC,e.evidence_hash LIMIT 16";
        try(Cursor c=db.rawQuery(formal,null)){while(c.moveToNext()){
            budget.checkpointOrYield();out.append(shortId(c.getString(0))).append(' ').append(c.getString(1)).append(' ').append(c.getString(2)).append('\n')
                    .append("计划机会 ").append(c.getLong(3)).append("；配对 ").append(c.getLong(4)).append("；不完整 ").append(c.getLong(5)).append("；配对命中 ").append(c.getLong(6)).append('\n')
                    .append(String.format(Locale.CHINA,"保守改善下界 %.4f；配对覆盖 %.2f%%；%s\n",c.getDouble(7),100*c.getDouble(8),c.getString(9)));
        }}
        out.append("\n历史活动对照（独立封存重放，属于研究数据）\n");
        try(Cursor c=db.rawQuery("SELECT a.source_campaign,a.variant,CASE WHEN EXISTS(SELECT 1 FROM su1_campaign_audit_cancelled x WHERE x.source_campaign=a.source_campaign AND x.variant=a.variant) THEN '旧版本活动已停止' ELSE a.phase END,o.report_hash FROM su1_campaign_audit a LEFT JOIN su1_campaign_audit_open o ON o.source_campaign=a.source_campaign AND o.variant=a.variant ORDER BY a.updated_at DESC,a.variant LIMIT 15",null)){
            while(c.moveToNext()){
                budget.checkpointOrYield();out.append(shortId(c.getString(0))).append(' ').append(c.getString(1)).append(" · ").append(c.getString(2)).append('\n');
                if(!c.isNull(3)){
                    Map<String,Object> report=com.ethk3.novaonepro.r952.StrictCanonicalJson.readContextObject(CampaignStateSU1.localBlob(db,c.getString(3)));
                    for(String split:new String[]{"dev","val","sealed"})if(report.get(split) instanceof Map){Map<?,?> metrics=(Map<?,?>)report.get(split);long n=((Number)metrics.get("actions")).longValue(),k=((Number)metrics.get("hits")).longValue(),calendar=((Number)metrics.get("opportunities")).longValue();out.append(split.toUpperCase(Locale.ROOT)).append(' ').append(k).append('/').append(n).append(" = ").append(percent(k,n)).append("；机会 ").append(calendar).append("；覆盖 ").append(percent(n,calendar)).append('\n');}
                }
            }
        }
        out.append("\n模型共同样本对照（历史研究，不授予资格）\n");
        try(Cursor c=db.rawQuery("SELECT source_campaign,phase,report_hash FROM su1_model_ablation ORDER BY updated_at DESC,source_campaign LIMIT 3",null)){
            while(c.moveToNext()){
                budget.checkpointOrYield();out.append(shortId(c.getString(0))).append(" · ").append(c.getString(1)).append('\n');
                if(!c.isNull(2)){
                    Map<String,Object> report=com.ethk3.novaonepro.r952.StrictCanonicalJson.readContextObject(CampaignStateSU1.localBlob(db,c.getString(2)));
                    for(Object item:(List<?>)report.get("variants")){Map<?,?> row=(Map<?,?>)item;long n=((Number)row.get("actions")).longValue(),k=((Number)row.get("hits")).longValue();
                        out.append(row.get("variant")).append("：").append(k).append('/').append(n).append(" = ").append(percent(k,n)).append("；配对机会 ").append(row.get("pairedN")).append("；真实训练更新 ").append(row.get("trainUpdates")).append('\n')
                           .append("平均概率损失 ").append(String.format(Locale.CHINA,"%.4f",Double.valueOf((String)row.get("meanLogLossHex")))).append("；计算毫秒 ").append(((Number)row.get("computeElapsedMicros")).longValue()/1000).append("；训练模型相对本组胜/负 ").append(row.get("trainedWinsComparator")).append('/').append(row.get("trainedLossesComparator")).append('\n');
                    }
                }
            }
        }
        out.append("\n能力链实际回执\n输入快照 ").append(scalar(db,"SELECT count(*) FROM su1_snapshot"))
                .append("；关系切片审计 ").append(scalar(db,"SELECT count(*) FROM su1_dependency WHERE reason='RELATION_SUPPORT_AUDIT'"))
                .append("；已训练参数模型 ").append(scalar(db,"SELECT count(*) FROM su1_model_artifact WHERE model_kind='SU1_FITTED_RELSTATE10'")).append('\n')
                .append("已冻结预测 ").append(scalar(db,"SELECT count(*) FROM su1_prediction"))
                .append("；已核验结算 ").append(scalar(db,"SELECT count(*) FROM su1_settlement"))
                .append("；已消费反馈 ").append(scalar(db,"SELECT count(*) FROM su1_outbox WHERE state='DONE'"))
                .append("；未消费反馈 ").append(scalar(db,"SELECT count(*) FROM su1_outbox WHERE state<>'DONE'")).append('\n');
        out.append("可检索研究记录 ").append(scalar(db,"SELECT count(*) FROM su1_research_knowledge")).append("；包含情境、假设、动作、估算操作成本及结果。\n");
        long claims=scalar(db,"SELECT count(*) FROM su1_eval_cohort WHERE evidence_role='CONFIRM'");
        out.append("\n自然资格：须以有效证书核验；历史成绩和排行均不等于正式资格。\n确认声明数 ").append(claims).append("；证书记录 ").append(scalar(db,"SELECT count(*) FROM su1_certificate")).append('\n');
        MainlineHeadSU1.Head head=MainlineHeadSU1.read(db);
        out.append("发布主线第 ").append(head.generation).append(" 次变更；当前：").append(head.strategy==null?"尚无合格策略":shortId(head.strategy)).append('\n');
        try(Cursor c=db.rawQuery("SELECT coalesce(sum(amount),0) FROM su1_alpha_reservation",null)){if(c.moveToFirst())out.append(String.format(Locale.CHINA,"本数据库终身错误预算占用 %.10f / 0.05\n",c.getDouble(0)));}
        try(Cursor c=db.rawQuery("SELECT family_alpha,global_claim_ordinal,planned_opportunities FROM su1_eval_cohort WHERE evidence_role='CONFIRM' ORDER BY created_at DESC,global_claim_ordinal DESC LIMIT 1",null)){if(c.moveToFirst()){
            double j=c.getDouble(1),alpha=c.getDouble(0)/(j*(j+1)*2);long needed=(long)Math.ceil(2*Math.log(1/alpha)/(.02*.02));
            out.append(String.format(Locale.CHINA,"最近声明单项 α %.12f；固定计划 %d机会\n预设改善2%%，保守下界半径≤2%%需约%d个完整日历机会；不足保持 WAIT。\n",alpha,c.getInt(2),needed));
        }}
        out.append(LeagueBoardSU2.render(db));   // R13-P30: research / shadow / real boards with N, bound, frequency
        try(Cursor c=db.rawQuery("SELECT phase,detail FROM su1_health_event ORDER BY id DESC LIMIT 4",null)){while(c.moveToNext())out.append("最近阻断：").append(c.getString(0)).append(' ').append(c.getString(1)).append('\n');}
        return out.toString();
    }
}
