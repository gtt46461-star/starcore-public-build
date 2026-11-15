package com.ethk3.novaonepro;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Looper;
import com.ethk3.novaonepro.r952.AtomCatalogPinnedCP12;
import com.ethk3.novaonepro.r952.ModernSeedFactoryCP12;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Bounded diagnostic projection only. Never trains, repairs, migrates, or grants authority.
 * Call once from the existing heavy UI worker and cache the immutable result. Each section
 * states its own evidence scope; a queue heartbeat is never a numerical completion receipt.
 */
public final class ResearchPipelineStatusR76 {
    private ResearchPipelineStatusR76() {}
    private static final int RECENT=32, PER_ISSUE=64;
    private static final long READ_BUDGET_NS=350_000_000L;
    public static final class Snapshot {
        public final long readAt;
        public final String sourceIssue,summary,training,evolution,atomsMatrices,mainline;
        private Snapshot(long at,String issue,String summary,String training,String evolution,String atoms,String mainline){
            readAt=at;sourceIssue=issue;this.summary=summary;this.training=training;this.evolution=evolution;atomsMatrices=atoms;this.mainline=mainline;
        }
    }
    public static Snapshot read(DbStore store){
        long at=System.currentTimeMillis();
        if(Looper.myLooper()!=null&&Looper.myLooper()==Looper.getMainLooper())return unavailable(at,"界面线程不执行数据库统计，等待后台快照");
        if(store==null)return unavailable(at,"数据库尚未就绪");
        try{return readDatabase(store.readable(),at,System.nanoTime()+READ_BUDGET_NS);}
        catch(RuntimeException failure){return unavailable(at,"数据库读取失败（"+failure.getClass().getSimpleName()+"）");}
    }
    private static Snapshot unavailable(long at,String reason){return new Snapshot(at,"未知",reason,"训练："+reason,"反馈与进化："+reason,"原子与矩阵："+reason,"主链资格："+reason);}
    interface Section {String read();}
    private static String section(long deadline,Section section){
        if(System.nanoTime()>=deadline)return "本段尚未刷新：后台读取时间已用完，等待下次快照。";
        try{return section.read();}catch(RuntimeException failure){return "本段读取失败（"+failure.getClass().getSimpleName()+"），不能当作零或已完成。";}
    }
    static Snapshot readDatabase(SQLiteDatabase db,long at,long deadline){
        if(System.nanoTime()>=deadline)return unavailable(at,"后台读取时间已用完，等待下次快照");
        List<String> issues=new ArrayList<>();
        String source="未知";
        try(Cursor c=db.rawQuery("SELECT issue,dirty FROM draws ORDER BY issue DESC LIMIT 3",null)){
            while(c.moveToNext())if(!c.isNull(0)&&!c.isNull(1)&&c.getInt(1)==0)issues.add(c.getString(0));
            if(!issues.isEmpty())source=issues.get(0);
        }catch(RuntimeException ignored){/* Each evidence section remains independently readable. */}
        final Map<String,String> states=new LinkedHashMap<>();
        String scheduler=section(deadline,()->{
            try(Cursor c=db.rawQuery("SELECT state_key,substr(state_value,1,512) FROM app_state WHERE state_key IN ('r65_director_last_slot','r65_director_last_slot_at','r912_research_continuity_status','cp12_matrix_blocker','r75_matrix_registration_phase','r75_matrix_registration_status_0','r75_matrix_registration_status_1','r75_matrix_registration_status_2','r75_matrix_registration_status_3','r75_matrix_registration_status_4')",null)){
                while(c.moveToNext())states.put(c.getString(0),c.isNull(1)?null:c.getString(1));
            }
            String status=states.get("r912_research_continuity_status");
            return "最近调度："+owner(states.get("r65_director_last_slot"))+"（"+time(states.get("r65_director_last_slot_at"))+"）；"+reason(status)
                    +"\n调度尝试不等于训练完成；下列数量依据实际提交记录。";
        });
        String training=section(deadline,()->training(db,issues));
        String evolution=section(deadline,()->evolution(db));
        String atoms=section(deadline,()->atoms(db,states));
        String mainline=section(deadline,()->mainline(db));
        String clock=new SimpleDateFormat("MM-dd HH:mm:ss",Locale.CHINA).format(new java.util.Date(at));
        return new Snapshot(at,source,"研究快照 "+clock+"｜最近开奖 "+source+"\n"+scheduler,training,evolution,atoms,mainline);
    }

    private static String training(SQLiteDatabase db,List<String> issues){
        StringBuilder b=new StringBuilder("【逐期反馈与数值训练】\n");
        try(Cursor c=db.rawQuery("SELECT trained_until_issue,artifact_hash FROM v57_research_artifact_r811 WHERE artifact_type='FTRL_SHADOW' ORDER BY trained_until_issue DESC LIMIT 1",null)){
            if(c.moveToFirst())b.append("逐期在线模型：实际产物已到 ").append(c.getString(0)).append("（FTRL影子学习）\n");
            else b.append("逐期在线模型：尚无FTRL产物记录\n");
        }
        if(issues.isEmpty())b.append("尚无可读取的最新开奖期；不能确认本期是否训练。\n");
        for(String issue:issues){
            boolean online=false;
            try(Cursor c=db.rawQuery("SELECT artifact_hash,status FROM v57_research_artifact_r811 WHERE artifact_type='FTRL_SHADOW' AND trained_until_issue=? LIMIT 1",new String[]{issue})){
                online=c.moveToFirst()&&!c.isNull(0)&&"RESEARCH_ONLY_SHADOW_LOADABLE_B09_ATOMIC".equals(c.getString(1));
            }
            int captured=0,settled=0,trained=0,waiting=0,missing=0;String error="";
            try(Cursor c=db.rawQuery("SELECT p.candidate_id,p.settled,o.state,substr(o.last_error,1,256),a.artifact_hash,a.numeric_updates "
                    +"FROM (SELECT candidate_id,settled FROM r952_candidate_predraw WHERE target_issue=? ORDER BY candidate_id LIMIT 65) p "
                    +"LEFT JOIN r952_research_outbox o ON o.delivery_hash=(SELECT x.delivery_hash FROM r952_research_outbox x WHERE x.candidate_id=p.candidate_id AND x.target_issue=? LIMIT 1) "
                    +"LEFT JOIN r952_artifact a ON a.delivery_hash=o.delivery_hash LIMIT 65",new String[]{issue,issue})){
                while(c.moveToNext()){
                    if(++captured>PER_ISSUE)break;
                    if(!c.isNull(1)&&c.getInt(1)==1)settled++;
                    if(!c.isNull(4)&&!c.isNull(5)&&c.getLong(5)>0&&"COMPLETED".equals(c.getString(2)))trained++;
                    else if(c.isNull(2))missing++;else waiting++;
                    if(error.isEmpty()&&!c.isNull(3)&&!c.getString(3).isEmpty())error=reason(c.getString(3));
                }
            }
            boolean more=captured>PER_ISSUE;int shown=Math.min(captured,PER_ISSUE);
            b.append(issue).append("：FTRL产物 ").append(online?"已提交1个":"尚无该期完成收据").append("；候选在线链 ");
            if(shown==0)b.append("未见该期赛前研究记录，未证实候选数值训练完成");
            else b.append(more?"抽样前64个算法":"核对"+shown+"个算法").append("，已结算 ").append(settled)
                    .append("，已提交数值训练 ").append(trained).append("，待处理反馈 ").append(waiting).append("，尚无反馈任务 ").append(missing);
            if(!error.isEmpty())b.append("；原因：").append(error);b.append('\n');
        }
        b.append("以上两条在线学习分开计；现代候选开发训练另按固定数据批次进行。\n【现代算法开发训练】\n");
        int total=0,features=0,completed=0,frozen=0;List<String> progressing=new ArrayList<>(),pending=new ArrayList<>();
        try(Cursor c=db.rawQuery("SELECT p.candidate_id,p.model_kind,p.train_n,p.validation_n,p.validation_last,t.status,substr(t.last_error,1,256),f.next_index,k.phase,k.cursor,r.receipt_hash,a.artifact_hash,a.validation_pass,"
                +"(SELECT e.candidate_id FROM r967_frozen_evaluation_v2 e WHERE e.learner_id=p.candidate_id ORDER BY e.created_issue DESC LIMIT 1),p.registered_issue,f.updated_at,k.updated_at "
                +"FROM (SELECT candidate_id,model_kind,train_n,validation_n,validation_last,registered_issue,protocol_hash FROM research_training_protocol_v2 ORDER BY rowid DESC LIMIT 33) p "
                +"LEFT JOIN v57_research_task_r967 t ON t.kind='TRAIN_CANDIDATE' AND t.candidate_id=p.candidate_id AND t.target_issue=p.registered_issue AND t.stream_seq=p.protocol_hash "
                +"LEFT JOIN research_training_feature_cursor_cp12 f ON f.task_id=t.task_id "
                +"LEFT JOIN research_task_checkpoint_v2 k ON k.task_id=t.task_id "
                +"LEFT JOIN research_development_receipt_v2 r ON r.protocol_hash=p.protocol_hash "
                +"LEFT JOIN r952_artifact a ON a.artifact_hash=r.artifact_hash AND a.candidate_id=p.candidate_id "
                +"AND a.training_kind='DEVELOPMENT_AST3' AND a.development_receipt_hash=r.receipt_hash AND a.validation_hash=r.validation_hash "
                +"AND a.development_sample_n=p.train_n AND a.validation_sample_n=p.validation_n LIMIT 33",null)){
            while(c.moveToNext()){
                if(++total>RECENT)break;
                boolean feature=!c.isNull(7),done=!c.isNull(10)&&!c.isNull(11)&&!c.isNull(12)&&c.getInt(12)==1;
                if(feature&&c.getLong(7)>0)features++;if(done)completed++;if(!c.isNull(13))frozen++;
                {
                    StringBuilder detail=new StringBuilder();
                    detail.append(model(c.getString(1))).append(" ").append(shortId(c.getString(0))).append("：")
                            .append(done?"训练验证已完成":"COMPLETED".equals(c.getString(5))?"任务标记完成，但缺少匹配的训练验证收据":state(c.getString(5))).append("；特征 ")
                            .append(feature?c.getString(7):"尚无进度收据").append('/').append(c.getInt(2)+c.getInt(3));
                    if(!c.isNull(8))detail.append("；").append(phase(c.getString(8))).append("游标 ").append(c.isNull(9)?"未知":c.getString(9));
                    if(!c.isNull(15))detail.append("；特征最后提交 ").append(time(c.getString(15)));
                    if(!c.isNull(16))detail.append("；数值最后提交 ").append(time(c.getString(16)));
                    detail.append("；开发数据截止 ").append(c.getString(4));
                    if(!c.isNull(6)&&!c.getString(6).isEmpty())detail.append("；").append(reason(c.getString(6)));
                    detail.append('\n');
                    (feature||done?progressing:pending).add(detail.toString());
                }
            }
        }
        if(total==0)b.append("尚无现代开发训练协议，不能声称已完成现代训练。\n");
        else b.append(total>RECENT?"最近登记32个算法（不是总量）":"已登记"+total+"个算法")
                .append("：有特征进度 ").append(features).append("，训练验证完成 ").append(completed).append("，已冻结 ").append(frozen).append("。\n");
        progressing.addAll(pending);for(int i=0;i<Math.min(4,progressing.size());i++)b.append(progressing.get(i));
        return b.append("最多展开4个，有进度者优先；特征行数不是训练完成率，开发验证不等于独立校准。\n").toString();
    }

    private static String evolution(SQLiteDatabase db){
        String feedback="尚无逐期矩阵结算反馈收据";
        try(Cursor c=db.rawQuery("SELECT target_issue FROM r952_matrix_feedback ORDER BY target_issue DESC LIMIT 1",null)){
            if(c.moveToFirst())feedback="逐期矩阵结算反馈已提交到 "+c.getString(0);
        }
        int total=0,children=0,proposed=0,evaluated=0;Map<String,Integer> kinds=new LinkedHashMap<>();StringBuilder details=new StringBuilder();
        try(Cursor c=db.rawQuery("SELECT a.action_type,a.operator,a.state,a.child_id,substr(a.last_error,1,256),f.source_issue "
                +"FROM (SELECT action_type,operator,state,child_id,last_error,finding_id FROM research_action_v2 ORDER BY rowid DESC LIMIT 33) a "
                +"LEFT JOIN research_finding_v2 f ON f.finding_id=a.finding_id LIMIT 33",null)){
            while(c.moveToNext()){
                if(++total>RECENT)break;String kind=operation(c.getString(0),c.getString(1));kinds.put(kind,kinds.containsKey(kind)?kinds.get(kind)+1:1);
                if(!c.isNull(3)&&!c.getString(3).isEmpty())children++;if("PROPOSED".equals(c.getString(2)))proposed++;if("EVALUATED".equals(c.getString(2)))evaluated++;
                if(total<=4){details.append(kind).append("：").append(state(c.getString(2)));
                    if(!c.isNull(3)&&!c.getString(3).isEmpty())details.append("；子代 ").append(shortId(c.getString(3)));
                    if(!c.isNull(4)&&!c.getString(4).isEmpty())details.append("；").append(reason(c.getString(4)));
                    if(!c.isNull(5))details.append("；反馈来源期 ").append(c.getString(5));details.append('\n');}
            }
        }
        StringBuilder b=new StringBuilder("【反馈、定向变异、融合与创造】\n").append(feedback).append("；不等于后续变异已完成。\n");
        if(total==0)return b.append("尚无研究动作收据，未证实这些动作已运行。\n").toString();
        b.append(total>RECENT?"最近32个动作":"已登记"+total+"个动作").append("：待执行 ").append(proposed).append("，已生成子代 ").append(children).append("，已完成独立评价 ").append(evaluated).append("。\n");
        for(Map.Entry<String,Integer> e:kinds.entrySet())b.append(e.getKey()).append(' ').append(e.getValue()).append("；");
        return b.append('\n').append(details).append("提案、产生子代、训练和独立评价分开计；没有融合收据不能宣称融合已运行，产生新结构不代表效果更好。\n").toString();
    }

    private static String atoms(SQLiteDatabase db,Map<String,String> states){
        StringBuilder b=new StringBuilder("【原子、分子与矩阵实际进度】\n");
        try(Cursor c=db.rawQuery("SELECT next_row,expected_rows,state FROM research_atom_catalog_import_cp12 WHERE asset_sha256=?",new String[]{AtomCatalogPinnedCP12.SHA256})){
            if(c.moveToFirst())b.append("原子目录 ").append(c.getLong(0)).append('/').append(c.getLong(1)).append("：").append(state(c.getString(2))).append('\n');
            else b.append("原子目录：尚无本版本导入收据\n");
        }
        try(Cursor c=db.rawQuery("SELECT next_row,expected_rows,state FROM research_molecule_import_cp12 WHERE source_hash=?",new String[]{ModernSeedFactoryCP12.ASSET_SHA})){
            if(c.moveToFirst())b.append("分子目录 ").append(c.getLong(0)).append('/').append(c.getLong(1)).append("：").append(state(c.getString(2))).append('\n');
            else b.append("分子目录：尚无本版本导入收据\n");
        }
        int bindings=0,admissions=0;
        try(Cursor c=db.rawQuery("SELECT binding_hash FROM research_atom_binding_v2 ORDER BY rowid DESC LIMIT 33",null)){while(c.moveToNext())bindings++;}
        try(Cursor c=db.rawQuery("SELECT matrix_id FROM research_atom_admission_cp12 ORDER BY rowid DESC LIMIT 33",null)){while(c.moveToNext())admissions++;}
        b.append("模型原子绑定记录 ").append(bindings>RECENT?"至少33":String.valueOf(bindings)).append("，质量评估记录 ")
                .append(admissions>RECENT?"至少33":String.valueOf(admissions)).append("；评估存在仍不代表已获主链资格。\n");
        int n=0,projected=0;StringBuilder details=new StringBuilder();java.util.Set<String> listed=new java.util.HashSet<>();
        try(Cursor c=db.rawQuery("SELECT s.matrix_type,s.partition_key,c.through_issue,c.sample_count,c.version "
                +"FROM (SELECT matrix_id,matrix_type,partition_key FROM research_matrix_spec_cp12 ORDER BY rowid DESC LIMIT 33) s "
                +"LEFT JOIN research_matrix_checkpoint_v2 c ON c.matrix_id=s.matrix_id LIMIT 33",null)){
            while(c.moveToNext()){
                if(++n>RECENT)break;if(!c.isNull(2))projected++;
                if(listed.size()<4&&listed.add(c.getString(0))){details.append(matrix(c.getString(0))).append("（最近登记分区示例）：");
                    if(c.isNull(2))details.append("已登记，尚无数值投影收据");
                    else details.append("算到 ").append(c.getString(2)).append("，窗口样本 ").append(c.getString(3)).append("，提交版本 ").append(c.getString(4));details.append('\n');}
            }
        }
        b.append(n>RECENT?"最近32个矩阵定义":"矩阵定义"+n+"个").append("，其中有数值投影 ").append(projected).append("个。\n").append(details);
        String blocked=states.get("cp12_matrix_blocker");if(blocked!=null&&!blocked.isEmpty())b.append("当前矩阵阻塞：").append(reason(blocked)).append('\n');
        return b.append("目录导入不等于原子质量准入；投影样本不是所有矩阵总样本，缺少绑定、教师或共同赛前证据时继续等待。\n").toString();
    }

    private static String mainline(SQLiteDatabase db){
        String bestStudy=null,bestKey=null,bestState=null,bestFreeze=null;boolean completedFallback=false;int captured=-1,settled=-1,active=0;
        for(String reservationState:new String[]{"ACTIVE","RESERVED","COMPLETE"}){
            if("COMPLETE".equals(reservationState)&&bestStudy!=null)break;
            boolean completed="COMPLETE".equals(reservationState);
            try(Cursor c=db.rawQuery("SELECT r.study_hash,r.candidate_key,s.captured_n,s.settled_n,s.state,r.freeze_hash "
                    +"FROM (SELECT study_hash,candidate_key,freeze_hash FROM research_capture_reservation_v2 WHERE state=? ORDER BY "
                    +(completed?"next_issue DESC,cost_budget_us DESC LIMIT 4":"next_issue,cost_budget_us LIMIT 8")+") r "
                    +"LEFT JOIN research_candidate_cal_study_r932 s ON s.study_hash=r.study_hash LIMIT 8",new String[]{reservationState})){
                while(c.moveToNext()){
                    active++;if(c.isNull(2)||c.isNull(3))continue;
                    if(c.getInt(3)>settled||(c.getInt(3)==settled&&c.getInt(2)>captured)){
                        bestStudy=c.getString(0);bestKey=c.getString(1);captured=c.getInt(2);settled=c.getInt(3);bestState=c.getString(4);bestFreeze=c.getString(5);completedFallback=completed;
                    }
                }
            }
        }
        StringBuilder b=new StringBuilder("【校准、证书与主链】\n");
        if(bestStudy==null)b.append(active==0?"当前未见活动校准预约；不能判断最佳候选的质量。\n":"活动预约缺少完整校准收据，质量未知。\n");
        else{
            b.append(completedFallback?"最近完成校准采样的候选 ":"活动预约中校准进度最高候选 ").append(shortId(bestKey)).append("（不是胜率排名）\n")
                    .append("赛前已预存 ").append(captured).append("/241，已结算 ").append(settled).append("/241；").append(state(bestState)).append("。\n");
            CandidateCalibrationR932.Qualification q=CandidateCalibrationR932.qualificationForStudyCP12(db,bestStudy);
            if(q.state==CandidateCalibrationR932.QualificationState.UNFITTED)b.append("尚未形成校准模型，胜率/稳定性未评估；先完成同一冻结候选的连续校准。\n");
            else if(q.qualified())b.append("校准质量已通过；仍需独立正式观察、证书和逐期主链授权，不能据此称已真实出手。\n");
            else b.append("已拟合但校准资格未通过：").append(reason(q.reason)).append("；等待失败反馈推动新子代研究。\n");
            if(q.state!=CandidateCalibrationR932.QualificationState.UNFITTED&&!"CALIBRATION_POLICY_MISMATCH".equals(q.reason))
                b.append(String.format(Locale.ROOT,"校准误差 %.4f（要求≤0.04）；Brier %.4f（要求≤0.24且≤基线%.4f）。\n",q.ece,q.brier,q.baselineBrier));
            String consumer=bestKey!=null&&bestKey.startsWith("algorithm:")?"INVENTED_ALGORITHM":bestKey!=null&&bestKey.startsWith("formula:")?"FORMULA_GRAPH":bestKey!=null&&bestKey.startsWith("genesis:")?"GENESIS_ASSET":null;
            if(consumer!=null){
                try(Cursor c=db.rawQuery("SELECT level,maximum_weight,valid_from_issue,valid_until_issue FROM "
                        +"(SELECT level,maximum_weight,valid_from_issue,valid_until_issue,structural_hash FROM research_certificate_r929 "
                        +"WHERE consumer=? AND candidate_id=? AND state='ACTIVE' ORDER BY valid_from_issue DESC,valid_until_issue DESC LIMIT 2) WHERE structural_hash=? LIMIT 2",
                        new String[]{consumer,bestKey.substring(bestKey.indexOf(':')+1),bestFreeze})){
                    if(c.moveToFirst())b.append("已登记活动证书：").append(level(c.getString(0))).append("，权重上限 ").append(c.getString(1))
                            .append("，期号 ").append(c.getString(2)).append("—").append(c.getString(3)).append("；本期可用性仍由主链完整预检核验。\n");
                    else b.append("该候选最近2条活动证书范围内，未见当前冻结身份的证书；本框未证明可进入真实出手。\n");
                }
            }
        }
        try(Cursor c=db.rawQuery("SELECT f.candidate_key,substr(f.reason,1,256),r.next_issue FROM (SELECT candidate_key,reason,study_hash FROM r975_calibration_failure ORDER BY rowid DESC LIMIT 1) f "
                +"LEFT JOIN research_capture_reservation_v2 r ON r.study_hash=f.study_hash LIMIT 1",null)){
            if(c.moveToFirst()){
                b.append("最近一次校准中断（").append(shortId(c.getString(0))).append("）：").append(reason(c.getString(1)));
                if(!c.isNull(2)&&!c.getString(2).isEmpty())b.append("；中断待采样期 ").append(c.getString(2));b.append('\n');
            }
        }
        try(Cursor c=db.rawQuery("SELECT target_issue,substr(candidate_name,1,80),substr(status,1,256),applied,influence_weight FROM v57_research_mainline_contribution_r922 ORDER BY target_issue DESC LIMIT 1",null)){
            if(c.moveToFirst())b.append("最近主链贡献期 ").append(c.getString(0)).append("：")
                    .append(!c.isNull(3)&&c.getInt(3)==1&&!c.isNull(4)&&c.getDouble(4)>0?"已有正权重贡献记录（仍不等于REAL交易）":"未形成正权重主链贡献")
                    .append("；").append(reason(c.getString(2))).append('\n');
            else b.append("尚无研究候选主链贡献记录；未证实已进入真实出手。\n");
        }
        return b.append("训练流累计样本不能充当该冻结模型的校准或正式Future；没有合格证据时不能评出“最佳可出手公式”。\n").toString();
    }

    static String reason(String code){
        if(code==null)return "尚无原因记录";if(code.isEmpty())return "未记录阻塞原因";
        if(code.contains("TRAIN_FEATURE_PAGE"))return "特征已落库，等待继续训练";
        if(code.contains("MONOTONIC_SLICE_EXHAUSTED")||code.contains("TIME_BUDGET_EXCEEDED"))return "本轮计算时间用完，等待续跑";
        if(code.contains("DB_BUSY")||code.contains("database is locked"))return "数据库繁忙，等待重试";
        if(code.contains("SCHEDULE_GAP")||code.contains("CAPTURE_GAP"))return "连续校准缺少赛前预测，原窗口中断（"+shortText(code)+"）";
        if(code.contains("WAIT_FROZEN_TEACHER"))return "等待符合要求的冻结教师";
        if(code.contains("WAIT_HISTORY"))return "可用开发历史不足，等待数据";
        if(code.contains("CALIBRATION_POLICY_MISMATCH"))return "校准模型或质量收据与现行规则不一致";
        if(code.contains("CALIBRATION_QUALITY_BLOCKED"))return "校准误差或Brier未满足质量门槛";
        if(code.contains("WAIT_CALIBRATION_INPUT"))return "等待完整连续校准样本";
        if(code.contains("NO_QUALIFIED")||code.contains("MODEL_NOT_PROVEN"))return "没有已证明合格的候选";
        if(code.contains("PERIOD_PENDING")||code.contains("REVIEW_ONLY"))return "本期维护仍在推进，尚不能代表数值训练完成";
        if(code.contains("WAIT_FUTURE"))return "等待子代独立训练、校准及正式前瞻评价";
        if(code.contains("MISSING_ATOM"))return "缺少可用原子定义";
        if(code.contains("NO_OP"))return "本轮未产生新的合法结构";
        if(code.contains("STALE")||code.contains("RETIRED"))return "依赖已失效或退役（"+shortText(code)+"）";
        if(code.equals("OK")||code.equals("COMPLETED"))return "已有该阶段完成记录";
        return "待核对原因码："+shortText(code);
    }
    private static String owner(String v){if(v==null)return "尚无调度记录";switch(v){case "TRAIN":return "数值训练";case "POST":return "训练后反馈";case "MATRIX":return "矩阵计算";case "RESEARCH":return "反馈研究";case "EVOLVE":return "变异与创造";case "IMPORT":return "原子与分子导入";default:return shortText(v);}}
    private static String state(String v){if(v==null)return "尚无任务记录";switch(v){case "PENDING":case "PROPOSED":return "排队待执行";case "RUNNING":return "任务已领取，完成量以收据为准";case "YIELDED":return "已让出时间片，等待续跑";case "WAIT_INPUT":return "等待输入或重试条件";case "COMPLETED":case "COMPLETE":return "已完成该阶段";case "WAIT_FUTURE":return "已登记子代，等待独立评价";case "CREATED":return "已生成子代";case "EVALUATED":return "已完成独立评价";case "NO_OP":return "未产生新结构";case "REJECTED":return "动作被拒绝";case "FAILED_TERMINAL":return "任务已终止失败";case "EXPIRED":return "任务已过期";case "IMPORTING":return "正在导入";case "COLLECTING":return "收集连续校准样本";case "READY":return "样本就绪，等待拟合";case "FROZEN":return "拟合已冻结，质量另行判定";case "BLOCKED_INVALID_INPUT":return "校准输入无效，原窗口已阻塞";default:return reason(v);}}
    private static String phase(String v){if(v==null)return "尚无数值检查点";if(v.contains("TRAIN"))return "训练";if(v.contains("VALID"))return "验证";if(v.contains("NORMAL"))return "归一化";return "计算阶段（"+shortText(v)+"）";}
    private static String model(String v){if(v==null)return "模型类型未知";switch(v){case "MLP_D32_16_10":return "神经网络";case "SOFTMAX_LINEAR":return "多分类线性模型";case "OVR_LOGISTIC":return "逻辑回归";case "HISTOGRAM_TREE":return "直方图树";case "EXPERT_HEDGE":return "专家融合";case "FIXED_GRAPH":return "公式图";default:return shortText(v);}}
    private static String level(String v){if(v==null)return "等级未知";switch(v){case "L4_CHALLENGER":return "挑战者（不授予主链权重）";case "L5_SHADOW":return "影子观察（不授予主链权重）";case "L6_CONTROLLED_INFLUENCE":return "受控主链贡献";case "L7_CHAMPION":return "冠军";default:return shortText(v);}}
    private static String operation(String type,String operator){if("CALIBRATION_REPAIR".equals(operator))return "校准失败定向修复";if("CREATE".equals(operator)||"NEW_FAMILY".equals(type))return "创造";if("FUSION".equals(type))return "融合";if("CROSSOVER".equals(type))return "交叉变异";if("MUTATE".equals(type))return "定向变异";return "观察保留";}
    private static String matrix(String v){String[] en={"FrequencyMatrix","OmissionMatrix","TransitionMatrix","ConditionalTransitionMatrix","CoOccurrenceMatrix","AtomTargetMatrix","AtomRegimeMatrix","CandidateWindowMatrix","CandidateErrorMatrix","CandidateSimilarityMatrix","CandidateDiversityMatrix","ParentChildMatrix","DriftMatrix","ConfidenceMatrix","LatencyMatrix","TeacherMatrix","RuleHealthMatrix","FreezeMatrix","FailureMatrix","ResidualMatrix"};String[] zh={"频率矩阵","遗漏矩阵","转移矩阵","条件转移矩阵","共同失误矩阵","原子目标矩阵","原子状态矩阵","候选窗口矩阵","候选误差矩阵","候选相似矩阵","候选多样性矩阵","父子矩阵","漂移矩阵","置信矩阵","时延矩阵","教师矩阵","规则健康矩阵","冻结矩阵","失败矩阵","残差矩阵"};for(int i=0;i<en.length;i++)if(en[i].equals(v))return zh[i];return v==null?"未知矩阵":shortText(v);}
    private static String shortId(String v){return v==null?"未知":v.length()<=24?v:v.substring(0,12)+"…"+v.substring(v.length()-8);}
    private static String time(String v){try{long at=Long.parseLong(v);return at<=0?"时间未记录":new SimpleDateFormat("MM-dd HH:mm:ss",Locale.CHINA).format(new java.util.Date(at));}catch(RuntimeException missing){return "时间未记录";}}
    private static String shortText(String v){return v.length()<=120?v:v.substring(0,120)+"…";}
}
