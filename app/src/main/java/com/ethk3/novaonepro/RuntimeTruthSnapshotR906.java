package com.ethk3.novaonepro;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.util.Locale;

/**
 * Immutable display value. Live panels are target-bound read observations; the modern
 * funnel uses one SQL statement. Exported reports use a separate immutable DB snapshot.
 * Live display must never reserve the SQLite writer while rendering legacy diagnostics.
 */
public final class RuntimeTruthSnapshotR906 {
    public static final String VERSION="R65-RUNTIME-TRUTH-READ-OBSERVATION";

    public static final class TruthCard {
        public final String code,title,detail,targetIssue,source,status;
        public TruthCard(String code,String title,String detail,String target,String source,String status){
            this.code=safe(code);this.title=safe(title);this.detail=explicit(detail,"暂无样本");
            targetIssue=safe(target);this.source=explicit(source,"R906_TRUTH");this.status=explicit(status,"AVAILABLE");
        }
        public String canonical(){return code+'|'+title+'|'+detail+'|'+targetIssue+'|'+source+'|'+status;}
    }

    public static final class StateBox {
        public final int ordinal; public final String code,title,value,source,targetIssue,status;
        public StateBox(int ordinal,String code,String title,String value,String source,String target,String status){
            this.ordinal=ordinal;this.code=safe(code);this.title=safe(title);this.value=explicit(value,"不可用｜原因=NO_DATA");
            this.source=explicit(source,"R906_TRUTH");this.targetIssue=safe(target);this.status=explicit(status,"AVAILABLE");
        }
        public String canonical(){return ordinal+'|'+code+'|'+title+'|'+value+'|'+source+'|'+targetIssue+'|'+status;}
    }

    public final String snapshotId,hash,status,readError,targetIssue,latestDrawIssue,latestCanonicalIssue,latestClosureIssue,latestOosIssue;
    public final long generatedAt,dbDataVersion;
    public final TruthCard[] whyCards;
    public final StateBox[] stateBoxes;
    public final SettlementCoverageTruthR906 coverage;
    public final String pipelineSummary,pipelineDetail,shadowSummary,shadowDetail;
    public final String researchSummary,researchDetail,realGateSummary,realDetail;
    public final BusinessFunnelCP12.Snapshot businessFunnel;

    private RuntimeTruthSnapshotR906(String id,String hash,String status,String error,long at,long version,
            String target,String draw,String canonical,String closure,String oos,TruthCard[] cards,StateBox[] boxes,
            SettlementCoverageTruthR906 coverage,String pipelineSummary,String pipelineDetail,
            String shadowSummary,String shadowDetail,String research,String researchDetail,
            String gate,String realDetail,BusinessFunnelCP12.Snapshot funnel){
        businessFunnel=funnel; snapshotId=safe(id);this.hash=safe(hash);this.status=explicit(status,"AVAILABLE");readError=safe(error);generatedAt=at;dbDataVersion=version;
        targetIssue=safe(target);latestDrawIssue=safe(draw);latestCanonicalIssue=safe(canonical);latestClosureIssue=safe(closure);latestOosIssue=safe(oos);
        whyCards=cards==null?new TruthCard[0]:cards;stateBoxes=boxes==null?new StateBox[0]:boxes;
        this.coverage=coverage==null?new SettlementCoverageTruthR906(0,0,0,0,0):coverage;
        this.pipelineSummary=explicit(pipelineSummary,"运行闭环不可用｜原因=NO_DATA");
        this.pipelineDetail=explicit(pipelineDetail,this.pipelineSummary);
        this.shadowSummary=explicit(shadowSummary,"影子结算不可用｜原因=NO_DATA");
        this.shadowDetail=explicit(shadowDetail,this.shadowSummary);
        researchSummary=explicit(research,"Research不可用｜原因=NO_DATA");
        this.researchDetail=explicit(researchDetail,researchSummary);
        realGateSummary=explicit(gate,"MODEL_NOT_PROVEN｜CONTROLLED_CANDIDATE_GATE");
        this.realDetail=explicit(realDetail,realGateSummary);
    }

    public static RuntimeTruthSnapshotR906 capture(DbStore store){
        return capture(store, "",true);
    }

    public static RuntimeTruthSnapshotR906 captureSummary(DbStore store){return capture(store,"",false);}
    /** Target-bound home observation; full diagnostic bodies belong to explicit detail pages. */
    public static RuntimeTruthSnapshotR906 captureSummary(DbStore store,String expectedTarget){return capture(store,expectedTarget,false);}

    /** Reads target-bound display observations without a live database write transaction. */
    public static RuntimeTruthSnapshotR906 capture(DbStore store,String expectedTarget){
        return capture(store,expectedTarget,true);
    }

    private static RuntimeTruthSnapshotR906 capture(DbStore store,String expectedTarget,boolean includeDetails){
        if(store==null)return error("STORE_NULL");
        SQLiteDatabase db=null; long now=System.currentTimeMillis();
        try{
            db=store.readable();
            if(db==null||!db.isOpen())return error("DB_NOT_OPEN");
            BusinessFunnelCP12.Snapshot businessFunnel=BusinessFunnelCP12.capture(db,expectedTarget,now);
            // beginTransactionNonExclusive is BEGIN IMMEDIATE on Android. A read-only
            // page used to occupy the sole writer across hundreds of diagnostic reads.
            // Do not turn a UI observation into a writer reservation.
            if(db.inTransaction())return error("UI_OBSERVATION_CALLER_TRANSACTION_BUSY");
            long dataVersion=longScalar(db,"PRAGMA data_version",null,0L);
            String draw=strScalar(db,"SELECT issue FROM "+DbHelper.TABLE_DRAWS+" WHERE dirty=0 ORDER BY issue DESC LIMIT 1",null,"");
            String canonical=strScalar(db,"SELECT target_issue FROM "+DbHelper.TABLE_R35_SHADOW_OBSERVATION+" WHERE pre_draw_frozen=1 ORDER BY commit_at DESC,target_issue DESC LIMIT 1",null,"");
            String closure=strScalar(db,"SELECT target_issue FROM "+DbHelper.TABLE_R36_SETTLEMENT_TASK+" WHERE status IN ('COMPLETED','NO_PREDRAW_CANONICAL_TERMINAL','FAILED_TERMINAL','INVALID_DRAW_TERMINAL') ORDER BY target_issue DESC LIMIT 1",null,"");
            String oos=strScalar(db,"SELECT target_issue FROM "+DbHelper.TABLE_R35_OOS_LABEL+" WHERE strict_oos=1 ORDER BY target_issue DESC LIMIT 1",null,"");
            String requested=safe(expectedTarget);
            String target=TargetIssueResolver.isValid(requested)
                    ? requested
                    : (canonical.length()>0?canonical:(closure.length()>0?closure:draw));
            SettlementCoverageTruthR906 cov=coverage(db);
            TruthCard[] cards=buildTruthCards(db,target,cov);
            StateBox[] boxes=buildStateBoxes(store,db,target,draw,canonical,closure,oos,cov);
            boxes[14]=modernResearchBoxR65(businessFunnel,target);
            DiagnosticSnapshotR811 diagnostic=DiagnosticSnapshotR811.capture(store);
            String research=modernResearchBoxR65(businessFunnel,target).value+"\n"+businessFunnel.summary()
                    +"\n【旧研究模块统计，独立于当前候选计数】\n"+researchSummary(store,db);
            String researchDetail=includeDetails?researchDetail(store,db,research)+"\n"+businessFunnel.detail():research;
            String gate=realGateSummary(store,db);
            String pipelineSummary=pipelineSummary(store,diagnostic,cov);
            String pipelineDetail=includeDetails?pipelineDetail(store,db,diagnostic,cov,pipelineSummary):pipelineSummary;
            String shadowSummary=shadowSummary(diagnostic);
            String shadowDetail=includeDetails?shadowDetail(db,diagnostic,shadowSummary):shadowSummary;
            String realDetail=includeDetails?realDetail(store,db,gate):gate;
            String payload=canonical(target,draw,canonical,closure,oos,dataVersion,cards,boxes,cov,research,gate)+"|FUNNEL|"+businessFunnel.canonical();
            String hash=V51ChecksumEngine.sha256(payload);
            String id="R922-"+(target.length()==0?"NO_TARGET":target)+"-"+shortHash(hash);
            return new RuntimeTruthSnapshotR906(id,hash,"AVAILABLE","",now,dataVersion,
                    target,draw,canonical,closure,oos,cards,boxes,cov,pipelineSummary,pipelineDetail,
                    shadowSummary,shadowDetail,research,researchDetail,gate,realDetail,businessFunnel);
        }catch(Throwable e){
            return error(e.getClass().getSimpleName()+":"+safe(e.getMessage()));
        }
    }

    private static StateBox modernResearchBoxR65(BusinessFunnelCP12.Snapshot funnel,String target){
        StringBuilder value=new StringBuilder("当前候选流程");boolean known=true;
        String[] stages={"Created","Registered","Training","Post-Train","Validation","Real Applied"};
        String[] labels={"候选","训练登记","训练验收","训练后处理","验证","真实应用"};
        for(int i=0;i<stages.length;i++)for(BusinessFunnelCP12.Row row:funnel.rows)if(stages[i].equals(row.stage)){
            value.append("｜").append(labels[i]).append('=').append(row.count==null?"未知":row.count);
            if(row.count==null){known=false;value.append('(').append(row.reasonCode).append(')');}
        }
        return new StateBox(15,"ALGORITHM_INVENTION","算法创造与迭代",value.toString(),
                "BusinessFunnelCP12_OWNER_RECEIPTS",target,known?"READ_OK":"UNKNOWN");
    }

    /** A light read intentionally carries no new diagnostic result until its worker finishes.
     * This is a pending observation, never a database failure or a successful zero result. */
    public static RuntimeTruthSnapshotR906 pending(String expectedTarget){
        String target=safe(expectedTarget),detail="本期诊断读取中，尚未取得结果；不使用上一期结果。";
        TruthCard[] cards=new TruthCard[]{
            new TruthCard("WHY_HIT","为什么中",detail,target,"HOME_DIAGNOSTIC_WORKER","PENDING"),
            new TruthCard("WHY_MISS","为什么不中",detail,target,"HOME_DIAGNOSTIC_WORKER","PENDING"),
            new TruthCard("WHY_NO_BET","为什么不出手",detail,target,"HOME_DIAGNOSTIC_WORKER","PENDING"),
            new TruthCard("SETTLEMENT","逐期结算统计",detail,target,"HOME_DIAGNOSTIC_WORKER","PENDING")};
        String[] titles={"目标期冻结","开奖真值","结算任务","命中真值","原始K3评分（未校准）","动作Rank","第三边界","动作稳定性","号码风险","形态状态","窗口共识","跨位支持","选择后校准","深度学习","算法发明迭代","DecisionGate"};
        StateBox[] boxes=new StateBox[titles.length];
        for(int i=0;i<boxes.length;i++)boxes[i]=new StateBox(i+1,"STATE_"+(i+1),titles[i],detail,"HOME_DIAGNOSTIC_WORKER",target,"PENDING");
        String hash=V51ChecksumEngine.sha256("R76_HOME_PENDING|"+target);
        return new RuntimeTruthSnapshotR906("R76-PENDING-"+(target.isEmpty()?"NO_TARGET":target),hash,"PENDING","",
                System.currentTimeMillis(),0L,target,"","","","",cards,boxes,null,
                detail,detail,detail,detail,detail,detail,detail,detail,null);
    }

    public static RuntimeTruthSnapshotR906 error(String reason){
        long now=System.currentTimeMillis(); String r=explicit(reason,"READ_ERROR");
        TruthCard[] cards=new TruthCard[]{
            new TruthCard("WHY_HIT","为什么中","暂无样本｜原因="+r,"","R906_TRUTH","READ_ERROR"),
            new TruthCard("WHY_MISS","为什么不中","暂无样本｜原因="+r,"","R906_TRUTH","READ_ERROR"),
            new TruthCard("WHY_NO_BET","为什么不出手","SHADOW｜原因="+r,"","DecisionGateEngine","READ_ERROR"),
            new TruthCard("SETTLEMENT","逐期结算统计","不可用｜原因="+r,"","SettlementTaskStoreR36+OosAfterDrawWriterR36","READ_ERROR")};
        StateBox[] boxes=new StateBox[16];
        String[] titles={"目标期冻结","开奖真值","结算任务","命中真值","原始K3评分（未校准）","动作Rank","第三边界","动作稳定性","号码风险","形态状态","窗口共识","跨位支持","选择后校准","深度学习","算法发明迭代","DecisionGate"};
        for(int i=0;i<boxes.length;i++)boxes[i]=new StateBox(i+1,"STATE_"+(i+1),titles[i],"不可用｜原因="+r,"R906_TRUTH","","READ_ERROR");
        String hash=V51ChecksumEngine.sha256("R906_ERROR|"+r+'|'+now);
        String failure="READ_ERROR:"+r;
        return new RuntimeTruthSnapshotR906("R922-READ-ERROR-"+shortHash(hash),hash,"READ_ERROR",r,
                now,0L,"","","","","",cards,boxes,new SettlementCoverageTruthR906(0,0,0,0,0),
                "运行闭环｜"+failure,failure,"影子结算｜"+failure,failure,
                "Research｜"+failure,failure,"真实出手硬门｜"+failure,failure,null);
    }

    private static TruthCard[] buildTruthCards(SQLiteDatabase db,String target,SettlementCoverageTruthR906 cov){
        TruthRow hit=latestSettled(db,1),miss=latestSettled(db,0),current=latestCanonical(db);
        String candidateStatus=stateValue(db,"r915_candidate_gate_status");
        String candidateDetail=stateValue(db,"r915_candidate_gate_detail");
        String hitText=hit==null?"暂无已结算命中样本":mechanicalSettlementWhy(db,hit,true);
        String missText=miss==null?"暂无已结算未中样本":mechanicalSettlementWhy(db,miss,false);
        String noBet;
        if(current==null)noBet="等待赛前冻结决策";
        else if("BET".equals(current.action))
            noBet="本期已受控出手｜CONTROLLED_CANDIDATE｜MODEL_NOT_PROVEN｜不把raw当校准胜率";
        else noBet=explicit(current.action,"SHADOW")+"｜拒绝原因="
                    +explicit(current.reject,"未达到R915受控候选门")
                    +"｜受控候选精确门="+explicit(candidateStatus,"NO_GATE_EVALUATION")
                    +"｜门值="+clip(explicit(candidateDetail,"NO_GATE_DETAIL"),420)
                    +"｜下期重新独立评估，不追单不补频率";
        String settle;
        if(cov.drawTaskTotal==0&&cov.predictedEligible==0)settle="等待首个合法开奖任务";
        else settle="开奖任务终态="+cov.drawTaskTerminal+"/"+cov.drawTaskTotal
                +"｜开奖任务待闭环="+cov.drawTaskOpen
                +"｜有赛前预测应统计="+cov.predictedEligible
                +"｜命中统计已结算="+cov.settledPredictions
                +"｜预测待结算="+cov.openEligible
                +"｜无赛前预测排除="+cov.excludedNoPredraw
                +"｜数据阻断="+cov.blocked;
        return new TruthCard[]{
            new TruthCard("WHY_HIT","为什么中",hitText,hit==null?target:hit.issue,"frozen_predraw+exact_draw",hit==null?"NO_SAMPLE":"AVAILABLE"),
            new TruthCard("WHY_MISS","为什么不中",missText,miss==null?target:miss.issue,"frozen_predraw+exact_draw",miss==null?"NO_SAMPLE":"AVAILABLE"),
            new TruthCard("WHY_NO_BET","为什么不出手",noBet,current==null?target:current.issue,"DecisionGateEngine","AVAILABLE"),
            new TruthCard("SETTLEMENT","逐期结算统计",settle,target,"SettlementTaskStoreR36+OosAfterDrawWriterR36","AVAILABLE")};
    }

    private static String mechanicalSettlementWhy(SQLiteDatabase db,TruthRow row,boolean hit){
        Evidence e=evidence(db,row.issue);
        ExplanationEvidence explanation=explanationEvidence(db,row.issue);
        String direct=(hit?"命中":"未中")+"｜期"+row.issue+"｜位置"+row.position
                +"｜实际"+two(row.actual)+(hit?"∈":"∉")+"赛前冻结K3("+k3(row)+")";
        String frozen=e.recordHash.length()==0
                ? "赛前量化证据行不可用"
                : "赛前量化：原始评分(未校准)="+metric(e.raw,"SCORE")
                +"，Rank="+(e.rankNo>0?e.rankNo+"/1200":"N/A")
                +"，动作稳定性="+metric(e.stability,"PROBABILITY")
                +"，号码风险="+metric(e.entropy,"PROBABILITY");
        String settled=hit?explanation.whyHit:explanation.whyMiss;
        if(safe(settled).length()==0)settled=hit
                ?"结构命中：实际号码落入同一期赛前冻结K3；教师/趋势/形态仅解释贡献，不改写冻结结果"
                :"结构未中：实际号码落在冻结K3之外；反推层检查第三边界、熵风险、教师反证和跨位分配是否误导";
        return direct+"｜"+frozen
                +"｜赛前Teacher/矩阵="+clip(explicit(explanation.preDraw,"N/A"),420)
                +"｜结算归因="+clip(explicit(settled,"机械归因：实际号码与冻结K3的包含关系"),420)
                +"｜开奖后反哺="+clip(explicit(explanation.postSettlement,"N/A"),300)
                +"｜时间边界=赛前证据与开奖后学习分栏，禁止后者冒充赛前理由";
    }

    private static ExplanationEvidence explanationEvidence(SQLiteDatabase db,String issue){Cursor c=null;try{
        c=db.rawQuery("SELECT teacher_matrix_summary,v20_decision_reason,dynamic_causal_summary,"
                        +"why_hit,why_miss,miss_reason_detail,learning_summary,reason_summary,pre_draw_reason_text "
                        +"FROM "+DbHelper.TABLE_DECISIONS+" WHERE target_issue=? ORDER BY created_at DESC LIMIT 1",
                new String[]{issue});
        if(!c.moveToFirst())return bridgeExplanation(db,issue,new ExplanationEvidence());
        ExplanationEvidence out=new ExplanationEvidence();
        out.preDraw=joinNonEmpty("；",safe(c.getString(0)),safe(c.getString(1)),safe(c.getString(2)),safe(c.getString(8)));
        out.whyHit=safe(c.getString(3));
        out.whyMiss=joinNonEmpty("；",safe(c.getString(4)),safe(c.getString(5)));
        out.postSettlement=joinNonEmpty("；",safe(c.getString(6)),safe(c.getString(7)));
        return bridgeExplanation(db,issue,out);
    }catch(Throwable ignored){return bridgeExplanation(db,issue,new ExplanationEvidence());}finally{if(c!=null)c.close();}}

    private static ExplanationEvidence bridgeExplanation(
            SQLiteDatabase db,String issue,ExplanationEvidence out){
        LegacyPredrawContextBridgeR915.Row row=LegacyPredrawContextBridgeR915.read(db,issue);
        if(row==null){
            if(safe(out.preDraw).length()==0)
                out.preDraw=LegacyPredrawContextBridgeR915.missingReason(db,issue,false);
        }else if(safe(out.preDraw).length()==0)out.preDraw=joinNonEmpty("；",
                    "教师矩阵="+row.teacher,"正向推理="+row.reasoning,"反推矩阵="+row.reverse,
                    "因果矩阵="+row.causal,"趋势="+row.trend,"形态="+row.shape,"关联="+row.relation);
        LegacyPredrawContextBridgeR915.PostRow post=LegacyPredrawContextBridgeR915.readPost(db,issue);
        if(post==null){
            if(safe(out.postSettlement).length()==0)
                out.postSettlement=LegacyPredrawContextBridgeR915.missingReason(db,issue,true);
        }else{
            if(post.hitLabel==1&&safe(out.whyHit).length()==0)out.whyHit=post.attribution;
            if(post.hitLabel==0&&safe(out.whyMiss).length()==0)out.whyMiss=post.attribution;
            if(safe(out.postSettlement).length()==0)out.postSettlement=post.feedback;
        }
        return out;
    }

    private static StateBox[] buildStateBoxes(DbStore store,SQLiteDatabase db,String target,String draw,String canonical,String closure,String oos,SettlementCoverageTruthR906 cov){
        StateBox[] out=new StateBox[16];
        Evidence e=evidence(db,target);
        String frozen="target="+explicit(target,"N/A")+"｜frozenAt="+(e.frozenAt>0?e.frozenAt:"N/A")+"｜hash="+shortHash(e.recordHash);
        String drawHash=strScalar(db,"SELECT draw_hash_v2 FROM "+DbHelper.TABLE_DRAWS+" WHERE issue=? LIMIT 1",new String[]{draw},"");
        out[0]=box(1,"目标期冻结",frozen,"selected_evidence",target,e.recordHash.length()>0?"AVAILABLE":"UNAVAILABLE");
        out[1]=box(2,"开奖真值","issue="+explicit(draw,"N/A")+"｜hash="+shortHash(drawHash)+"｜source=draws","draws",draw,draw.length()>0?"AVAILABLE":"UNAVAILABLE");
        String core=strScalar(db,"SELECT status FROM "+DbHelper.TABLE_R36_SETTLEMENT_TASK+" WHERE target_issue=? LIMIT 1",new String[]{target},"");
        String err=strScalar(db,"SELECT COALESCE(terminal_code,'')||CASE WHEN COALESCE(last_error,'')<>'' THEN ':'||last_error ELSE '' END FROM "+DbHelper.TABLE_R36_SETTLEMENT_TASK+" WHERE target_issue=? LIMIT 1",new String[]{target},"");
        out[2]=box(3,"结算任务","当前期="+explicit(core,"WAITING")+"｜reason="+explicit(err,"NONE")
                +"｜开奖任务终态="+cov.drawTaskTerminal+"/"+cov.drawTaskTotal
                +"｜预测结算="+cov.settledPredictions+"/"+cov.predictedEligible,
                "SettlementTaskStoreR36",target,core.length()>0?"AVAILABLE":"UNAVAILABLE");
        TruthRow exact=settledFor(db,target);
        out[3]=box(4,"命中真值",exact==null?"不可用｜原因=NOT_SETTLED":"actual="+two(exact.actual)+"｜K3="+k3(exact)+"｜hit="+(exact.hit==1?"HIT":"MISS"),"OosAfterDrawWriterR36",target,exact==null?"UNAVAILABLE":"AVAILABLE");
        out[4]=box(5,"原始K3评分（未校准）",metric(e.raw,"SCORE")
                +"｜有效校准样本不足时禁止解释为真实命中概率",
                "selected_evidence.raw_action_p",target,e.rawStatus);
        out[5]=box(6,"动作Rank",e.rankNo>0?(e.rankNo+"/1200｜score="+number(e.rankScore)):"不可用｜原因=RANK_NOT_FROZEN","selected_evidence.rank",target,e.rankNo>0?"AVAILABLE":"UNAVAILABLE");
        out[6]=box(7,"第三边界",metric(e.third,"SCORE"),"selected_evidence.third_margin",target,e.thirdStatus);
        out[7]=box(8,"动作稳定性",metric(e.stability,"PROBABILITY"),"selected_evidence.stability",target,e.stabilityStatus);
        out[8]=box(9,"号码风险",metric(e.entropy,"PROBABILITY")+"｜口径=归一化熵风险","selected_evidence.entropy_risk",target,e.entropyStatus);
        String shape=shapeTruth(db,target);
        out[9]=box(10,"形态状态",shape,"v51_selected_action.shape_*",target,shape.indexOf("不可用")<0?"AVAILABLE":"UNAVAILABLE");
        out[10]=box(11,"窗口共识",metric(e.window,"PROBABILITY"),"selected_evidence.window_consensus",target,e.windowStatus);
        out[11]=box(12,"跨位支持",metric(e.assignment,"PROBABILITY")+"｜仅作特征，不改变单位置玩法","selected_evidence.assignment_support",target,e.assignmentStatus);
        String selectedP=strScalar(db,"SELECT selected_hit_p FROM "+DbHelper.TABLE_R35_SHADOW_OBSERVATION+" WHERE target_issue=? LIMIT 1",new String[]{target},"");
        String ece=store.getState("strict_oos_ece",""),brier=store.getState("strict_oos_brier","");
        out[12]=box(13,"选择后校准",selectedP.length()==0?"不可用｜原因=SELECTED_P_MISSING":"SelectedHitP="+selectedP+"｜ECE="+explicit(ece,"N/A")+"｜Brier="+explicit(brier,"N/A"),"selected_oos",target,selectedP.length()>0?"AVAILABLE":"UNAVAILABLE");
        String ftrlUpdates=firstState(store,"r811_ftrl_updates","r817_research_adaptive_updates","0");
        String ftrlUntil=firstState(store,"r811_ftrl_trained_until","r817_research_adaptive_trained_until","");
        String deepUpdates=store.getState("r909_tiny_deep_updates","0");
        String deepUntil=store.getState("r909_tiny_deep_trained_until","");
        String continuity=store.getState("r912_research_continuity_status","");
        String researchTick=store.getState("r908_research_tick_status","");
        out[13]=box(14,"深度学习","FTRL更新="+explicit(ftrlUpdates,"0")
                +"｜until="+explicit(ftrlUntil,"N/A")
                +"｜TinyDeep更新="+explicit(deepUpdates,"0")
                +"｜until="+explicit(deepUntil,"N/A")
                +"｜连续学习="+clip(explicit(continuity,"WAITING"),120)
                +"｜研究脉冲="+clip(explicit(researchTick,"WAITING"),120),
                "ResearchLearningContinuityR912",target,"AVAILABLE");
        int genomes=intScalar(db,"SELECT COUNT(1) FROM "+DbHelper.TABLE_R908_ALGORITHM_GENOME,null,0);
        int future=intScalar(db,"SELECT COALESCE(SUM(future_n),0) FROM "+DbHelper.TABLE_R908_ALGORITHM_GENOME,null,0);
        int archived=intScalar(db,"SELECT COUNT(1) FROM "+DbHelper.TABLE_R908_ALGORITHM_GENOME
                +" WHERE status LIKE 'PRUNED%' OR status LIKE 'REJECTED%' OR status LIKE 'REVOKED%'",null,0);
        int paused=intScalar(db,"SELECT COUNT(1) FROM "+DbHelper.TABLE_R908_ALGORITHM_GENOME
                +" WHERE status='PAUSED_CAPACITY_R912'",null,0);
        int active=Math.max(0,genomes-archived-paused);
        int assets=intScalar(db,"SELECT COUNT(1) FROM "+AssetGenesisEngineR915.TABLE_ASSET_GENOME,null,0);
        int assetFuture=intScalar(db,"SELECT COALESCE(SUM(future_n),0) FROM "+AssetGenesisEngineR915.TABLE_ASSET_GENOME,null,0);
        int assetPruned=intScalar(db,"SELECT COUNT(1) FROM "+AssetGenesisEngineR915.TABLE_ASSET_GENOME+" WHERE status LIKE 'PRUNED%'",null,0);
        out[14]=box(15,"算法发明迭代","创生资产="+assets+"｜资产FutureOOS="+assetFuture+"｜资产淘汰="+assetPruned
                +"｜算法总数="+genomes+"｜活跃="+active+"｜证据档案="+archived+"｜容量暂停="+paused
                +"｜算法FutureOOS="+future+"｜创生最佳="+assetTruth(db)
                +"｜活跃算法最佳="+algorithmTruth(db)+"｜归档高证据="+algorithmArchiveTruth(db),
                "AssetGenesisEngineR915+AlgorithmInventionLabR908+LifecycleR912",target,
                assets+genomes>0?"AVAILABLE":"NO_SAMPLE");
        String action=currentAction(db,target),reject=currentReject(db,target);
        out[15]=box(16,"DecisionGate",explicit(action,"SHADOW")+"｜reject="+explicit(reject,V51BuildManifest.MODEL_PROVEN?"NONE":"MODEL_NOT_PROVEN"),"DecisionGateEngine",target,"AVAILABLE");
        return out;
    }

    private static StateBox box(int i,String title,String value,String source,String target,String status){return new StateBox(i,"STATE_"+i,title,value,source,target,status);}

    private static String researchSummary(DbStore store,SQLiteDatabase db){
        int legacyExpected=LegacyFixedAssetRegistryR915.entries().length;
        int legacyRegistered=intScalar(db,"SELECT COUNT(1) FROM "+AssetGenesisEngineR915.TABLE_LEGACY_REGISTRY,null,0);
        int legacyLocked=intScalar(db,"SELECT COUNT(1) FROM "+AssetGenesisEngineR915.TABLE_LEGACY_REGISTRY+" WHERE locked=1",null,0);
        int formulaAssets=intScalar(db,"SELECT COUNT(1) FROM "+DbHelper.TABLE_R906_RESEARCH_ASSET,null,0);
        int formulaExec=intScalar(db,"SELECT COUNT(1) FROM "+DbHelper.TABLE_R906_RESEARCH_ASSET+" WHERE runtime_executable=1",null,0);
        int formulaFrozen=intScalar(db,"SELECT COALESCE(SUM(frozen_n),0) FROM "+DbHelper.TABLE_R906_RESEARCH_EVAL_COVERAGE,null,0);
        int formulaSettled=intScalar(db,"SELECT COALESCE(SUM(settled_n),0) FROM "+DbHelper.TABLE_R906_RESEARCH_EVAL_COVERAGE,null,0);
        int atomProposals=intScalar(db,"SELECT COUNT(1) FROM "+AssetGenesisEngineR915.TABLE_ATOM_PROPOSAL,null,0);
        int genesis=intScalar(db,"SELECT COUNT(1) FROM "+AssetGenesisEngineR915.TABLE_ASSET_GENOME,null,0);
        int genesisFrozen=intScalar(db,"SELECT COUNT(1) FROM "+AssetGenesisEngineR915.TABLE_ASSET_FREEZE,null,0);
        int genesisSettled=intScalar(db,"SELECT COUNT(1) FROM "+AssetGenesisEngineR915.TABLE_ASSET_FREEZE+" WHERE hit_label IN(0,1)",null,0);
        int genesisEligible=intScalar(db,"SELECT COUNT(1) FROM "+AssetGenesisEngineR915.TABLE_ASSET_FREEZE
                +" f WHERE EXISTS(SELECT 1 FROM "+DbHelper.TABLE_DRAWS+" d WHERE d.issue=f.target_issue AND d.dirty=0)",null,0);
        int genesisCertified=intScalar(db,"SELECT COUNT(1) FROM "+AssetGenesisEngineR915.TABLE_ASSET_CERTIFICATE+" WHERE status='ACTIVE'",null,0);
        int genesisPruned=intScalar(db,"SELECT COUNT(1) FROM "+AssetGenesisEngineR915.TABLE_ASSET_GENOME+" WHERE status LIKE 'PRUNED%' OR status LIKE 'REJECTED%' OR status LIKE 'REVOKED%'",null,0);
        int contributions=intScalar(db,"SELECT COUNT(1) FROM "+CertifiedResearchMixerR915.TABLE_CONTRIBUTION+" WHERE status='CERTIFIED_NEW_ASSET_INFLUENCE'",null,0);
        int invented=intScalar(db,"SELECT COUNT(1) FROM "+DbHelper.TABLE_R908_ALGORITHM_GENOME,null,0);
        int algorithmFrozen=intScalar(db,"SELECT COUNT(1) FROM "+DbHelper.TABLE_R908_ALGORITHM_PREDRAW,null,0);
        int algorithmSettled=intScalar(db,"SELECT COUNT(1) FROM "+DbHelper.TABLE_R908_ALGORITHM_PREDRAW+" WHERE hit_label IN(0,1)",null,0);
        int algorithmCertified=intScalar(db,"SELECT COUNT(1) FROM "+DbHelper.TABLE_R908_ALGORITHM_GENOME+" WHERE status='CHALLENGER_FUTURE_OOS'",null,0);
        int algorithmPruned=intScalar(db,"SELECT COUNT(1) FROM "+DbHelper.TABLE_R908_ALGORITHM_GENOME+" WHERE status LIKE 'PRUNED%' OR status LIKE 'REJECTED%' OR status LIKE 'REVOKED%'",null,0);
        int algorithmPaused=intScalar(db,"SELECT COUNT(1) FROM "+DbHelper.TABLE_R908_ALGORITHM_GENOME+" WHERE status='PAUSED_CAPACITY_R912'",null,0);
        int algorithmActive=Math.max(0,invented-algorithmPruned-algorithmPaused);
        int algorithmPending=intScalar(db,"SELECT COUNT(1) FROM "+DbHelper.TABLE_R908_ALGORITHM_PREDRAW+" WHERE hit_label=-1",null,0);
        int genesisPending=intScalar(db,"SELECT COUNT(1) FROM "+AssetGenesisEngineR915.TABLE_ASSET_FREEZE+" WHERE hit_label=-1",null,0);
        int formulaPending=intScalar(db,"SELECT COUNT(1) FROM "+DbHelper.TABLE_R817_RESEARCH_PREDRAW+" WHERE hit_label=-1",null,0);
        int feedbackPending=intScalar(db,"SELECT COUNT(1) FROM "+DbHelper.TABLE_R36_SETTLEMENT_TASK
                +" t WHERE t.status='COMPLETED' AND NOT EXISTS(SELECT 1 FROM "
                +LegacyPredrawContextBridgeR915.POST_TABLE+" p WHERE p.target_issue=t.target_issue)",null,0);
        String ftrl=firstState(store,"r811_ftrl_updates","r817_research_adaptive_updates","0");
        String trained=firstState(store,"r811_ftrl_trained_until","r817_research_adaptive_trained_until","");
        String deepUpdates=store.getState("r909_tiny_deep_updates","0");
        String generation=explicit(store.getState("r915_asset_genesis_tick",""),"NO_TICK_RECORDED");
        String bootstrap=explicit(store.getState("r922_dynamic_bootstrap_truth",""),"NO_R922_BOOTSTRAP_PULSE");
        String capture=explicit(store.getState("r914_research_predraw_recovery_status",""),
                explicit(store.getState("r907_research_async_status",""),"NO_PREDRAW_CAPTURE_RECORDED"));
        String algorithmCapture=explicit(store.getState("r919_algorithm_capture_status",""),
                "NO_ALGORITHM_CAPTURE_ATTEMPT");
        String integrity=explicit(store.getState("r915_legacy_integrity",""),"WAIT_VERIFY");
        String mixer=explicit(store.getState("r915_asset_mixer_status",""),"NO_CERTIFIED_NEW_ASSET");
        return com.ethk3.novaonepro.r952.ModernResearchProgressR78.render(db)+"\n历史研究档案（以下待结算不是现代训练样本）\nR923创造能力｜Legacy="+legacyRegistered+'/'+legacyExpected+"｜锁定="+legacyLocked
                +"｜Integrity="+integrity
                +"\n因子/原子登记="+GenesisSchemaRegistryR915.atomCount()
                +"｜生产评分语义实装="+GenesisSchemaRegistryR915.productionScoreEligibleCount()
                +"｜研究代理隔离="+GenesisSchemaRegistryR915.researchProxyCount()
                +"｜文本审计身份="+GenesisSchemaRegistryR915.auditIdentityCount()
                +"｜开奖后证据="+GenesisSchemaRegistryR915.settlementEvidenceCount()
                +"｜导航家族="+GenesisSchemaRegistryR915.familyCount()
                +"｜新原子提案="+atomProposals
                +"\n12组原子="+atomGroupAtlas()
                +"\n创生资产="+genesis+"｜赛前冻结="+genesisFrozen+"｜已结算="+genesisSettled+'/'+genesisEligible
                +"｜证书="+genesisCertified+"｜淘汰="+genesisPruned+"｜主链贡献="+contributions
                +"\n最佳创生="+assetTruth(db)
                +"\n算法总数="+invented+"｜活跃="+algorithmActive+"｜证据档案="+algorithmPruned
                +"｜容量暂停="+algorithmPaused+"｜赛前冻结="+algorithmFrozen+"｜待结算="+algorithmPending
                +"｜已结算="+algorithmSettled+"｜挑战者="+algorithmCertified
                +"｜活跃最佳="+algorithmTruth(db)+"｜归档高证据="+algorithmArchiveTruth(db)
                +"\n公式资产="+formulaAssets+"｜可执行="+formulaExec+"｜冻结/待结算/结算="+formulaFrozen+'/'+formulaPending+'/'+formulaSettled
                +"｜FTRL更新="+explicit(ftrl,"0")+"@"+explicit(trained,"N/A")
                +"｜TinyDeep更新="+explicit(deepUpdates,"0")
                +"\n队列=创生待结算:"+genesisPending+"｜算法待结算:"+algorithmPending
                +"｜公式待结算:"+formulaPending+"｜教师/推理/反推/因果反馈待补:"+feedbackPending
                +"\n本轮创造="+clip(generation,260)+"｜赛前捕获="+clip(capture,260)
                +"｜算法捕获="+clip(algorithmCapture,180)
                +"｜生产混合="+clip(mixer,160)
                +"\n空实验室启动="+clip(bootstrap,360);
    }

    private static String researchDetail(DbStore store,SQLiteDatabase db,String summary){
        StringBuilder b=new StringBuilder(16384);
        b.append("【R923深度学习・动态迭代・算法创造唯一真值】\n").append(summary).append('\n');
        b.append("快照Owner=RuntimeTruthSnapshotR906｜Legacy只读、GENESIS_NEW可进化、Research无直接REAL权限。\n");
        b.append("原子执行器=").append(GenesisAtomicWorldR920.VERSION)
                .append("｜生产评分语义实装=").append(GenesisSchemaRegistryR915.productionScoreEligibleCount())
                .append("｜A10/A11研究代理=").append(GenesisSchemaRegistryR915.researchProxyCount())
                .append("（新建/变异/制证链已隔离）｜禁止再声明240/240全部实装\n");
        b.append("原子边界=240项逐项登记datatype/range/window/lag/support/leakage/updateTiming/cost/version；"
                +"新建可制证程序只可选择").append(GenesisSchemaRegistryR915.productionScoreEligibleCount())
                .append("项生产评分语义原子；A9开奖后证据").append(GenesisSchemaRegistryR915.settlementEvidenceCount())
                .append("项只能进入生命周期账本，禁止作为本期评分输入。\n");
        b.append("算法失败向量=").append(explicit(store.getState("r919_algorithm_failure_vector",""),"NO_PARENT_OR_NOT_RUN")).append('\n');
        b.append("算法创造原因=").append(explicit(store.getState("r919_algorithm_creation_reasons",""),"NO_SURVIVOR_OR_NOT_RUN")).append('\n');
        b.append("R923 Regime支持审计=").append(explicit(
                store.getState("r923_algorithm_regime_support_audit",""),
                "WAIT_NEXT_ALGORITHM_LIFECYCLE")).append('\n');
        b.append("连续真实未中分析=").append(explicit(
                store.getState("r923_miss_streak_analysis",""),"NO_PROVEN_REAL_SAMPLE"))
                .append("｜研究步骤=").append(explicit(
                        store.getState("r923_miss_streak_research_step",""),"IDLE")).append('\n');
        b.append("研究连续性=").append(explicit(store.getState("r912_research_continuity_status",""),"NO_PULSE_RECORDED")).append('\n');
        b.append("研究调度=").append(explicit(store.getState("r908_research_tick_status",""),"NO_TICK_RECORDED"))
                .append("｜阶段=").append(explicit(store.getState("r908_research_phase",""),"0"))
                .append("｜成本=").append(explicit(store.getState("r908_research_tick_cost_ms",""),"N/A")).append("ms\n");
        b.append("R923动态启动=").append(explicit(store.getState("r922_dynamic_bootstrap_truth",""),
                "NO_BOOTSTRAP_PULSE_RECORDED")).append('\n');
        b.append("R947逐期动态进化=").append(explicit(store.getState("r947_evolution_heartbeat",""),
                explicit(store.getState("r947_evolution_status",""),"NO_EVOLUTION_HEARTBEAT")))
                .append("｜完成到=").append(explicit(store.getState("r947_evolution_completed_through",""),"N/A"))
                .append("｜review总数=").append(explicit(store.getState("r947_evolution_reviews_total",""),"0"))
                .append("｜stage尝试=").append(explicit(store.getState("r947_evolution_stage_attempts_total",""),"0"))
                .append("｜新子代=").append(explicit(store.getState("r947_evolution_children_created_total",""),"0"))
                .append("｜backlog=").append(explicit(store.getState("r947_evolution_backlog",""),"0")).append('\n');
        b.append("本轮变化=").append(explicit(store.getState("r922_research_last_delta",""),
                "NO_DELTA_SNAPSHOT")).append("｜at=")
                .append(explicit(store.getState("r922_research_last_delta_at",""),"N/A")).append('\n');
        b.append("最近实质变化=").append(explicit(store.getState("r922_research_last_material_change",""),
                "NO_MATERIAL_CHANGE_YET")).append("｜at=")
                .append(explicit(store.getState("r922_research_last_material_change_at",""),"N/A")).append('\n');
        b.append("创生维护=").append(explicit(store.getState("r915_asset_genesis_tick",""),"NO_TICK_RECORDED"))
                .append("｜最近生成=").append(explicit(store.getState("r915_genesis_last_generated_names",""),"NO_GENERATION"))
                .append("｜失败向量=").append(explicit(store.getState("r915_genesis_last_failure",""),"NO_FAILURE_VECTOR"))
                .append("｜需求计划=").append(explicit(store.getState("r915_genesis_last_need",""),"NO_NEED_PLAN")).append('\n');
        b.append("赛前冻结=").append(explicit(store.getState("r915_genesis_predraw_target",""),"NO_TARGET"))
                .append('/').append(explicit(store.getState("r915_genesis_predraw_frozen",""),"0"))
                .append("｜最近结算=").append(explicit(store.getState("r915_genesis_last_settled_issue",""),"NO_SETTLEMENT"))
                .append('/').append(explicit(store.getState("r915_genesis_last_settled_rows",""),"0")).append('\n');
        b.append("算法捕获=").append(explicit(store.getState("r919_algorithm_capture_status",""),"NO_ATTEMPT"))
                .append("｜算法逐期结算=").append(explicit(store.getState("r919_algorithm_post_commit_status",""),"NO_SETTLEMENT"))
                .append('/').append(explicit(store.getState("r919_algorithm_post_commit_issue",""),"NO_ISSUE"))
                .append('/').append(explicit(store.getState("r919_algorithm_post_commit_rows",""),"0")).append('\n');
        b.append("公式逐期结算=").append(explicit(store.getState("r919_formula_post_commit_status",""),"NO_SETTLEMENT"))
                .append('/').append(explicit(store.getState("r919_formula_post_commit_issue",""),"NO_ISSUE"))
                .append('/').append(explicit(store.getState("r919_formula_post_commit_rows",""),"0"))
                .append("｜创生逐期结算=").append(explicit(store.getState("r915_genesis_post_commit_status",""),"NO_SETTLEMENT"))
                .append('/').append(explicit(store.getState("r915_genesis_post_commit_issue",""),"NO_ISSUE"))
                .append('/').append(explicit(store.getState("r915_genesis_post_commit_rows",""),"0")).append('\n');
        b.append("Teacher反哺逐期结算=").append(explicit(store.getState("r919_teacher_post_commit_status",""),"NO_SETTLEMENT"))
                .append('/').append(explicit(store.getState("r919_teacher_post_commit_issue",""),"NO_ISSUE"))
                .append('/').append(explicit(store.getState("r919_teacher_post_commit_rows",""),"0")).append('\n');
        appendAtomRegistry(b);
        appendGenesisAssets(b,db,12);
        appendAlgorithms(b,db,12);
        appendArchivedAlgorithms(b,db,12);
        appendAtomProposals(b,db,8);
        appendContributions(b,db,8);
        b.append("\n【R923逐期预测主链贡献审计】\n")
                .append(ResearchMainlineContributionAuditR922.recentDetail(store,12));
        b.append("\n晋级合同=Future OOS N>=240、配对>=120、Wilson/FDR/ECE/Brier/覆盖/块/Regime/连挂全部通过后才制证；未制证贡献严格为0。\n");
        b.append("动态迭代=失败向量→结构生成/变异→赛前冻结→exact draw逐期结算→状态更新→证据审查→保留/淘汰/制证；开发回放不计Future OOS。\n");
        b.append("REAL边界=创生资产最多8%限幅、算法家族最多30%限幅；二者都不能创建BET，最终仍由DecisionGateEngine唯一授权。");
        return b.toString();
    }

    private static String atomGroupAtlas(){
        return "A0时点真值/A1排列身份/A2时间记忆/A3关系图/A4数值变换/A5信息统计/"
                +"A6状态反馈/A7风险不确定/A8动作几何/A9开奖后证据/A10资源健康/A11进化互补";
    }

    static void appendAtomRegistry(StringBuilder b){
        b.append("\n【240项底层因子/原子完整登记】\n");
        int group=-1;
        for(GenesisSchemaRegistryR915.AtomSpec atom:GenesisSchemaRegistryR915.atoms()){
            int next=parseAtomGroup(atom.group);
            if(next!=group){group=next;b.append("\n[").append(atom.group).append("] ")
                    .append(atomGroupName(group)).append('\n');}
            b.append(atom.id).append("｜").append(atom.name)
                    .append("｜semanticTier=").append(atom.semanticTier())
                    .append("｜type=").append(atom.valueType)
                    .append("｜range=").append(atom.valueRange)
                    .append("｜window=").append(atom.windowSpec)
                    .append("｜lag<=").append(atom.maxLag)
                    .append("｜support>=").append(atom.minSupport)
                    .append("｜stage=").append(atom.evidenceStage)
                    .append("｜leakage=").append(atom.leakagePolicy)
                    .append("｜update=").append(atom.updateTiming)
                    .append("｜cost=").append(atom.costClass)
                    .append("｜v").append(atom.version).append('\n');
        }
    }

    private static int parseAtomGroup(String group){
        try{return Integer.parseInt(safe(group).substring(1));}catch(Throwable ignored){return -1;}
    }

    private static String atomGroupName(int group){
        String[] names={"时点与数据真值","排列/位置/号码身份","时间与生命周期","关系图与迁移",
                "数值变换与交互","统计与信息","可学习状态与反馈","不确定性/风险/反证",
                "1200动作几何","Future-OOS开奖后证据","资源与后台健康","进化互补与信用分配"};
        return group>=0&&group<names.length?names[group]:"未知组";
    }

    private static String algorithmTruth(SQLiteDatabase db){Cursor c=null;try{
        c=db.rawQuery("SELECT display_name_zh,status,future_n,future_hit_rate,future_wilson_lower,"
                        +"future_brier,future_ece,future_coverage,paired_mainline_n,paired_mainline_delta,"
                        +"future_fdr_q,lifecycle_reason,mutation_intent,creation_reason,mechanism_spec_zh,updated_at FROM "+DbHelper.TABLE_R908_ALGORITHM_GENOME
                        +" WHERE status NOT LIKE 'PRUNED%' AND status NOT LIKE 'REJECTED%'"
                        +" AND status NOT LIKE 'REVOKED%' AND status<>'PAUSED_CAPACITY_R912'"
                        +" ORDER BY CASE status WHEN 'CHALLENGER_FUTURE_OOS' THEN 0 WHEN 'OBSERVING_FUTURE_OOS' THEN 1 "
                        +"WHEN 'IDEA_WAIT_FUTURE_OOS' THEN 2 ELSE 3 END,future_wilson_lower DESC,future_n DESC LIMIT 1",null);
        if(!c.moveToFirst())return "暂无活跃算法；请查看证据归档榜";
        return explicit(c.getString(0),"未命名")+"｜"+explicit(c.getString(1),"UNKNOWN")
                +"｜N="+c.getInt(2)+"｜HR="+percent(c.getDouble(3))+"｜Wilson下界="+percent(c.getDouble(4))
                +"｜Brier="+number(c.getDouble(5))+"｜ECE="+number(c.getDouble(6))
                +"｜覆盖="+percent(c.getDouble(7))+"｜相对主链="+c.getInt(8)+":"+percentSigned(c.getDouble(9))
                +"｜FDR="+percent(c.getDouble(10))+"｜生命周期="+explicit(c.getString(11),"WAITING")
                +"｜结构意图="+explicit(c.getString(12),"OPEN_EXPLORATION")
                +"｜创造原因="+clip(explicit(c.getString(13),"NO_FAILURE_VECTOR"),180)
                +"｜"+clip(explicit(c.getString(14),"机制待生成"),220)
                +"｜updatedAt="+c.getLong(15);
    }catch(Throwable e){return "不可用｜原因="+e.getClass().getSimpleName();}finally{if(c!=null)c.close();}}

    private static String algorithmArchiveTruth(SQLiteDatabase db){Cursor c=null;try{
        c=db.rawQuery("SELECT display_name_zh,status,future_n,future_hit_rate,future_wilson_lower,"
                        +"future_brier,future_ece,future_coverage,future_regime_min,lifecycle_reason,updated_at FROM "
                        +DbHelper.TABLE_R908_ALGORITHM_GENOME
                        +" WHERE status LIKE 'PRUNED%' OR status LIKE 'REJECTED%' OR status LIKE 'REVOKED%'"
                        +" ORDER BY future_wilson_lower DESC,future_n DESC,updated_at DESC LIMIT 1",null);
        if(!c.moveToFirst())return "暂无证据归档";
        return explicit(c.getString(0),"未命名")+"｜"+explicit(c.getString(1),"UNKNOWN")
                +"｜N="+c.getInt(2)+"｜HR="+percent(c.getDouble(3))+"｜Wilson="+percent(c.getDouble(4))
                +"｜Brier/ECE="+number(c.getDouble(5))+'/'+number(c.getDouble(6))
                +"｜覆盖="+percent(c.getDouble(7))+"｜Regime="+percent(c.getDouble(8))
                +"｜归档原因="+explicit(c.getString(9),"UNKNOWN")+"｜updatedAt="+c.getLong(10);
    }catch(Throwable e){return "不可用｜原因="+e.getClass().getSimpleName();}finally{if(c!=null)c.close();}}

    private static String assetTruth(SQLiteDatabase db){Cursor c=null;try{
        c=db.rawQuery("SELECT display_name_zh,status,future_n,future_hit_rate,future_wilson_lower,"
                        +"future_coverage,paired_mainline_n,paired_mainline_delta,fdr_q,lifecycle_reason,updated_ms "
                        +"FROM "+AssetGenesisEngineR915.TABLE_ASSET_GENOME
                        +" ORDER BY CASE status WHEN 'CERTIFIED_FUTURE_OOS' THEN 0 WHEN 'OBSERVING_FUTURE_OOS' THEN 1 "
                        +"WHEN 'IDEA_WAIT_FUTURE_OOS' THEN 2 ELSE 3 END,future_wilson_lower DESC,future_n DESC LIMIT 1",null);
        if(!c.moveToFirst())return "暂无已注册创生资产";
        return explicit(c.getString(0),"未命名")+"｜"+explicit(c.getString(1),"UNKNOWN")
                +"｜N="+c.getInt(2)+"｜HR="+percent(c.getDouble(3))+"｜Wilson="+percent(c.getDouble(4))
                +"｜覆盖="+percent(c.getDouble(5))+"｜配对="+c.getInt(6)+":"+percentSigned(c.getDouble(7))
                +"｜FDR="+percent(c.getDouble(8))+"｜原因="+explicit(c.getString(9),"WAITING")
                +"｜updatedAt="+c.getLong(10);
    }catch(Throwable e){return "不可用｜原因="+e.getClass().getSimpleName();}finally{if(c!=null)c.close();}}

    static void appendGenesisAssets(StringBuilder b,SQLiteDatabase db,int limit){
        b.append("\n【GENESIS_NEW创生资产榜】\n");Cursor c=null;int n=0;try{
            c=db.rawQuery("SELECT display_name_zh,family_id,status,future_n,future_hit,future_hit_rate,"
                    +"future_wilson_lower,future_brier,future_ece,future_coverage,future_block_min,"
                    +"future_regime_min,future_max_miss,paired_mainline_n,paired_mainline_delta,fdr_q,"
                    +"lifecycle_reason,mechanism_spec_zh,structural_hash,behavior_hash,updated_ms FROM "
                    +AssetGenesisEngineR915.TABLE_ASSET_GENOME
                    +" ORDER BY CASE status WHEN 'CERTIFIED_FUTURE_OOS' THEN 0 WHEN 'OBSERVING_FUTURE_OOS' THEN 1 "
                    +"WHEN 'IDEA_WAIT_FUTURE_OOS' THEN 2 ELSE 3 END,future_wilson_lower DESC,future_n DESC LIMIT ?",
                    new String[]{String.valueOf(Math.max(1,Math.min(20,limit)))});
            while(c.moveToNext()){n++;b.append(n).append("｜").append(explicit(c.getString(0),"中文机制名缺失"))
                    .append("｜家族=").append(explicit(c.getString(1),"UNKNOWN"))
                    .append("｜").append(explicit(c.getString(2),"UNKNOWN"))
                    .append("｜N/H=").append(c.getInt(3)).append('/').append(c.getInt(4))
                    .append("｜HR=").append(percent(c.getDouble(5)))
                    .append("｜Wilson=").append(percent(c.getDouble(6)))
                    .append("｜Brier/ECE=").append(number(c.getDouble(7))).append('/').append(number(c.getDouble(8)))
                    .append("｜Coverage=").append(percent(c.getDouble(9)))
                    .append("｜Block/Regime=").append(percent(c.getDouble(10))).append('/').append(percent(c.getDouble(11)))
                    .append("｜MaxMiss=").append(c.getInt(12))
                    .append("｜Paired=").append(c.getInt(13)).append(':').append(percentSigned(c.getDouble(14)))
                    .append("｜FDR=").append(percent(c.getDouble(15)))
                    .append("\n  WHY=").append(explicit(c.getString(16),"WAIT_EVIDENCE"))
                    .append("\n  机制=").append(clip(explicit(c.getString(17),"MECHANISM_MISSING"),900))
                    .append("\n  结构/行为=").append(shortHash(c.getString(18))).append('/').append(shortHash(c.getString(19)))
                    .append("｜updatedAt=").append(c.getLong(20)).append('\n');}
        }catch(Throwable e){b.append("READ_ERROR:").append(e.getClass().getSimpleName()).append('\n');}
        finally{if(c!=null)c.close();}if(n==0)b.append("NO_SAMPLE:NO_REGISTERED_GENESIS_ASSET\n");
    }

    static void appendAlgorithms(StringBuilder b,SQLiteDatabase db,int limit){
        b.append("\n【活跃算法家族Future OOS榜】\n");Cursor c=null;int n=0;try{
            c=db.rawQuery("SELECT display_name_zh,status,future_n,future_hit,future_hit_rate,future_wilson_lower,"
                    +"future_brier,future_ece,future_coverage,future_block_min,future_regime_min,future_max_miss,"
                    +"paired_mainline_n,paired_mainline_delta,future_fdr_q,lifecycle_reason,mechanism_spec_zh,"
                    +"mutation_intent,creation_reason,structural_hash,behavior_hash,updated_at FROM "+DbHelper.TABLE_R908_ALGORITHM_GENOME
                    +" WHERE status NOT LIKE 'PRUNED%' AND status NOT LIKE 'REJECTED%'"
                    +" AND status NOT LIKE 'REVOKED%' AND status<>'PAUSED_CAPACITY_R912'"
                    +" ORDER BY CASE status WHEN 'CHALLENGER_FUTURE_OOS' THEN 0 WHEN 'OBSERVING_FUTURE_OOS' THEN 1 "
                    +"WHEN 'IDEA_WAIT_FUTURE_OOS' THEN 2 ELSE 3 END,future_wilson_lower DESC,future_n DESC LIMIT ?",
                    new String[]{String.valueOf(Math.max(1,Math.min(20,limit)))});
            while(c.moveToNext()){n++;b.append(n).append("｜").append(explicit(c.getString(0),"中文机制名缺失"))
                    .append("｜").append(explicit(c.getString(1),"UNKNOWN"))
                    .append("｜N/H=").append(c.getInt(2)).append('/').append(c.getInt(3))
                    .append("｜HR=").append(percent(c.getDouble(4))).append("｜Wilson=").append(percent(c.getDouble(5)))
                    .append("｜Brier/ECE=").append(number(c.getDouble(6))).append('/').append(number(c.getDouble(7)))
                    .append("｜Coverage=").append(percent(c.getDouble(8)))
                    .append("｜Block/Regime=").append(percent(c.getDouble(9))).append('/').append(percent(c.getDouble(10)))
                    .append("｜MaxMiss=").append(c.getInt(11))
                    .append("｜Paired=").append(c.getInt(12)).append(':').append(percentSigned(c.getDouble(13)))
                    .append("｜FDR=").append(percent(c.getDouble(14)))
                    .append("\n  WHY=").append(explicit(c.getString(15),"WAIT_EVIDENCE"))
                    .append("\n  机制=").append(clip(explicit(c.getString(16),"MECHANISM_MISSING"),900))
                    .append("\n  结构意图=").append(explicit(c.getString(17),"OPEN_EXPLORATION"))
                    .append("｜创造原因=").append(clip(explicit(c.getString(18),"NO_FAILURE_VECTOR"),700))
                    .append("\n  结构/行为=").append(shortHash(c.getString(19))).append('/').append(shortHash(c.getString(20)))
                    .append("｜updatedAt=").append(c.getLong(21)).append('\n');}
        }catch(Throwable e){b.append("READ_ERROR:").append(e.getClass().getSimpleName()).append('\n');}
        finally{if(c!=null)c.close();}if(n==0)b.append("NO_SAMPLE:NO_ACTIVE_ALGORITHM_FAMILY\n");
    }

    static void appendArchivedAlgorithms(StringBuilder b,SQLiteDatabase db,int limit){
        b.append("\n【算法证据归档榜（淘汰不删除）】\n");Cursor c=null;int n=0;try{
            c=db.rawQuery("SELECT display_name_zh,status,future_n,future_hit,future_hit_rate,future_wilson_lower,"
                    +"future_brier,future_ece,future_coverage,future_block_min,future_regime_min,future_max_miss,"
                    +"paired_mainline_n,paired_mainline_delta,future_fdr_q,lifecycle_reason,mechanism_spec_zh,"
                    +"mutation_intent,creation_reason,structural_hash,behavior_hash,updated_at FROM "+DbHelper.TABLE_R908_ALGORITHM_GENOME
                    +" WHERE status LIKE 'PRUNED%' OR status LIKE 'REJECTED%' OR status LIKE 'REVOKED%'"
                    +" ORDER BY future_wilson_lower DESC,future_n DESC,updated_at DESC LIMIT ?",
                    new String[]{String.valueOf(Math.max(1,Math.min(20,limit)))});
            while(c.moveToNext()){n++;b.append(n).append("｜").append(explicit(c.getString(0),"中文机制名缺失"))
                    .append("｜").append(explicit(c.getString(1),"UNKNOWN"))
                    .append("｜N/H=").append(c.getInt(2)).append('/').append(c.getInt(3))
                    .append("｜HR=").append(percent(c.getDouble(4))).append("｜Wilson=").append(percent(c.getDouble(5)))
                    .append("｜Brier/ECE=").append(number(c.getDouble(6))).append('/').append(number(c.getDouble(7)))
                    .append("｜Coverage=").append(percent(c.getDouble(8)))
                    .append("｜Block/Regime=").append(percent(c.getDouble(9))).append('/').append(percent(c.getDouble(10)))
                    .append("｜MaxMiss=").append(c.getInt(11))
                    .append("｜Paired=").append(c.getInt(12)).append(':').append(percentSigned(c.getDouble(13)))
                    .append("｜FDR=").append(percent(c.getDouble(14)))
                    .append("\n  ARCHIVE_WHY=").append(explicit(c.getString(15),"UNKNOWN"))
                    .append("\n  机制=").append(clip(explicit(c.getString(16),"MECHANISM_MISSING"),900))
                    .append("\n  结构意图=").append(explicit(c.getString(17),"OPEN_EXPLORATION"))
                    .append("｜创造原因=").append(clip(explicit(c.getString(18),"NO_FAILURE_VECTOR"),700))
                    .append("\n  结构/行为=").append(shortHash(c.getString(19))).append('/').append(shortHash(c.getString(20)))
                    .append("｜updatedAt=").append(c.getLong(21)).append('\n');}
        }catch(Throwable e){b.append("READ_ERROR:").append(e.getClass().getSimpleName()).append('\n');}
        finally{if(c!=null)c.close();}if(n==0)b.append("NO_SAMPLE:NO_ARCHIVED_ALGORITHM_FAMILY\n");
        b.append("归档语义=状态迁移保留完整Future-OOS证据，不作为活跃候选继续赛前冻结；"
                +"界面榜单分栏，避免高样本算法因淘汰状态看似‘消失’。\n");
    }

    static void appendAtomProposals(StringBuilder b,SQLiteDatabase db,int limit){
        b.append("\n【失败驱动的新原子提案】\n");Cursor c=null;int n=0;try{
            c=db.rawQuery("SELECT proposed_name,status,reject_reason,consumer_family,source_atom_ids,definition_spec,created_issue FROM "
                    +AssetGenesisEngineR915.TABLE_ATOM_PROPOSAL+" ORDER BY created_ms DESC LIMIT ?",
                    new String[]{String.valueOf(Math.max(1,Math.min(20,limit)))});
            while(c.moveToNext()){n++;b.append(n).append("｜").append(explicit(c.getString(0),"UNNAMED_ATOM"))
                    .append("｜").append(explicit(c.getString(1),"UNKNOWN"))
                    .append("｜拒绝=").append(explicit(c.getString(2),"NONE"))
                    .append("｜消费家族=").append(explicit(c.getString(3),"UNKNOWN"))
                    .append("｜来源=").append(clip(explicit(c.getString(4),"NONE"),240))
                    .append("｜定义=").append(clip(explicit(c.getString(5),"MISSING"),520))
                    .append("｜创建期=").append(explicit(c.getString(6),"N/A")).append('\n');}
        }catch(Throwable e){b.append("READ_ERROR:").append(e.getClass().getSimpleName()).append('\n');}
        finally{if(c!=null)c.close();}if(n==0)b.append("NO_SAMPLE:NO_FAILURE_DRIVEN_ATOM_PROPOSAL\n");
    }

    static void appendContributions(StringBuilder b,SQLiteDatabase db,int limit){
        b.append("\n【证书化主链贡献】\n");Cursor c=null;int n=0;try{
            c=db.rawQuery("SELECT target_issue,display_name_zh,influence_weight,top_evaluated,status,evidence_hash FROM "
                    +CertifiedResearchMixerR915.TABLE_CONTRIBUTION+" ORDER BY created_ms DESC LIMIT ?",
                    new String[]{String.valueOf(Math.max(1,Math.min(20,limit)))});
            while(c.moveToNext()){n++;b.append(n).append("｜期=").append(explicit(c.getString(0),"N/A"))
                    .append("｜").append(explicit(c.getString(1),"中文机制名缺失"))
                    .append("｜影响=").append(percent(c.getDouble(2)))
                    .append("｜评估动作=").append(c.getInt(3))
                    .append("｜").append(explicit(c.getString(4),"UNKNOWN"))
                    .append("｜证据=").append(shortHash(c.getString(5))).append('\n');}
        }catch(Throwable e){b.append("READ_ERROR:").append(e.getClass().getSimpleName()).append('\n');}
        finally{if(c!=null)c.close();}if(n==0)b.append("NO_SAMPLE:NO_CERTIFIED_CONTRIBUTION_PRODUCTION_FALLBACK_100_PERCENT\n");
    }

    private static String pipelineSummary(DbStore store,DiagnosticSnapshotR811 d,SettlementCoverageTruthR906 cov){
        RuntimeHealthLedgerR36.Metrics m=RuntimeHealthLedgerR36.metrics(store,RuntimeHealthLedgerR36.DURABLE_RETURN_EVENT_R923,96);
        return "R923运行闭环｜开奖="+explicit(d.latestDraw,"N/A")+"｜预测="+explicit(d.latestCanonical,"N/A")
                +"｜结算="+explicit(d.latestSettled,"N/A")+"｜严格OOS="+explicit(d.latestOos,"N/A")
                +"\n开奖任务终态="+cov.drawTaskTerminal+'/'+cov.drawTaskTotal+"｜开放="+cov.drawTaskOpen
                +"｜预测结算="+cov.settledPredictions+'/'+cov.predictedEligible+"｜无赛前预测排除="+cov.excludedNoPredraw
                +"｜阻断="+cov.blocked+"｜延后证据="+d.deferredPending
                +"｜分析并发="+AnalysisSingleFlightR36.shared().activeCount()
                +"｜请求至事务返回P95(最近96)="+(m==null||m.sample<=0||m.p95Ms<0?"N/A":m.p95Ms+"ms")
                +"｜实时压力="+(d.realtimePressure?"YES":"NO");
    }

    private static String pipelineDetail(DbStore store,SQLiteDatabase db,DiagnosticSnapshotR811 d,
            SettlementCoverageTruthR906 cov,String summary){
        StringBuilder b=new StringBuilder(6144);b.append("【R923运行闭环唯一真值】\n").append(summary).append('\n');
        b.append("目标期=").append(explicit(d.targetIssue,"N/A")).append("｜CORE=").append(explicit(d.coreStatus,"N/A"))
                .append("｜FULL=").append(explicit(d.fullStatus,"N/A")).append('\n');
        b.append("结算任务分布=").append(taskDistribution(db)).append('\n');
        b.append("结算Owner=SettlementEngine_ONLY｜预测Owner=AnalysisSingleFlightR36_ONLY｜Research失败不得回滚核心结算。\n");
        b.append("后台系统能力=状态=").append(explicit(store.getState("r919_background_capability_status",""),"NOT_OBSERVED"))
                .append("｜电池无限制=").append("1".equals(store.getState("r919_background_battery_unrestricted","0"))?"YES":"NO")
                .append("｜精确闹钟=").append("1".equals(store.getState("r919_background_exact_alarm_granted","0"))?"YES":"NO")
                .append("｜权限=").append(explicit(store.getState("r919_background_permission_summary",""),"N/A"))
                .append("｜观察时间=").append(explicit(store.getState("r919_background_capability_at",""),"N/A"))
                .append("；任一系统授权缺失时界面按钮必须引导用户补齐，不能伪称已验证后台。\n");
        b.append("延后追赶=").append(explicit(store.getState("r902_deferred_drain_status",""),"NO_DRAIN_RECORDED"))
                .append("｜紧急追赶=").append(explicit(store.getState("r902_deferred_urgent_status",""),"NO_URGENT_RECORDED"))
                .append("｜研究结算=").append(explicit(store.getState("r902_research_settlement_status",""),"NO_RESEARCH_SETTLEMENT_RECORDED")).append('\n');
        b.append("延后队列收敛=").append(DeferredEvidenceWriterR36.backlogStats(store).summary()).append('\n');
        b.append("后台自恢复=loops=").append(explicit(store.getState("r908_background_loops",""),"NOT_ARMED"))
                .append("｜taskRemoved=").append(explicit(store.getState("r922_task_removed_reconcile",""),"NOT_OBSERVED"))
                .append("｜research=").append(explicit(store.getState("r908_research_tick_status",""),"NO_PULSE"))
                .append("｜settlement=").append(explicit(store.getState("r908_settlement_loop_status",""),"NO_PULSE"))
                .append("｜限制=Android强制停止/OEM电池限制不可绕过\n");
        b.append("最近12个冻结预测：\n");Cursor c=null;int n=0;try{
            c=db.rawQuery("SELECT s.target_issue,s.position,s.n1,s.n2,s.n3,s.settled,s.actual_number,s.hit_label,COALESCE(t.status,'NO_TASK') FROM "
                    +DbHelper.TABLE_R35_SHADOW_OBSERVATION+" s LEFT JOIN "+DbHelper.TABLE_R36_SETTLEMENT_TASK
                    +" t ON t.target_issue=s.target_issue ORDER BY s.target_issue DESC LIMIT 12",null);
            while(c.moveToNext()){n++;b.append(n).append("｜期=").append(c.getString(0)).append("｜P").append(c.getInt(1))
                    .append("｜K3=").append(two(c.getInt(2))).append('/').append(two(c.getInt(3))).append('/').append(two(c.getInt(4)))
                    .append("｜settled=").append(c.getInt(5)).append("｜actual=").append(c.getInt(6))
                    .append("｜hit=").append(c.getInt(7)).append("｜task=").append(explicit(c.getString(8),"NO_TASK")).append('\n');}
        }catch(Throwable e){b.append("READ_ERROR:").append(e.getClass().getSimpleName()).append('\n');}
        finally{if(c!=null)c.close();}if(n==0)b.append("NO_SAMPLE:NO_FROZEN_PREDICTION\n");return b.toString();
    }

    private static String shadowSummary(DiagnosticSnapshotR811 d){
        int pending=Math.max(0,d.shadowPredicted-d.shadowSettled);
        String rate=d.shadowSettled<=0?"N/A":percent(d.shadowHit/(double)d.shadowSettled);
        String coverage=d.shadowPredicted<=0?"N/A":percent(d.shadowSettled/(double)d.shadowPredicted);
        return "R923影子预测统计｜预测="+d.shadowPredicted+"｜已结算="+d.shadowSettled+"｜待结算="+pending
                +"｜预测结算完成率="+coverage+"｜命中/未中="+d.shadowHit+'/'+d.shadowMiss+"｜命中率="+rate
                +"（命中÷已结算，不是命中÷未中）"
                +"｜SHADOW不计真实盈亏";
    }

    private static String shadowDetail(SQLiteDatabase db,DiagnosticSnapshotR811 d,String summary){
        StringBuilder b=new StringBuilder(3072);b.append("【R923严格Future OOS统计】\n").append(summary).append('\n');
        appendWindow(b,"最近30",d.recent30);appendWindow(b,"最近60",d.recent60);appendWindow(b,"最近120",d.recent120);
        b.append("严格OOS=").append(intScalar(db,"SELECT COUNT(1) FROM "+DbHelper.TABLE_R35_OOS_LABEL+" WHERE strict_oos=1",null,0))
                .append("｜合法选择后概率=").append(intScalar(db,"SELECT COUNT(1) FROM "+DbHelper.TABLE_R35_OOS_LABEL+" WHERE strict_oos=1 AND selected_hit_p BETWEEN 0 AND 1",null,0)).append('\n');
        b.append("命中率公式=").append(d.shadowHit).append("÷(").append(d.shadowHit).append('+')
                .append(d.shadowMiss).append(")；命中÷未中是命中未中比，不是命中率。\n");
        b.append("口径=赛前冻结canonical+exact draw；预测结算完成率不等于实际开奖预测覆盖率；"
                +"最近窗口按目标期号排序，回补旧期不会冒充最近一期；开奖后禁止重算；小样本不得显示为模型证明。");return b.toString();
    }

    private static void appendWindow(StringBuilder b,String name,DiagnosticSnapshotR811.WindowStats w){
        b.append(name).append("：N=").append(w==null?0:w.sample);
        if(w==null||!w.available()){b.append("｜HR/Wilson/Brier=N/A\n");return;}
        b.append("｜Hit=").append(w.hit).append("｜HR=").append(percent(w.hitRate))
                .append("｜Wilson下界=").append(percent(w.wilsonLower)).append("｜Brier=").append(number(w.brier)).append('\n');
    }

    private static String realGateSummary(DbStore store,SQLiteDatabase db){
        Models.RuntimeStats s=store.getRuntimeStats();
        int settled=s==null?0:s.betCount,hit=s==null?0:s.hitCount,miss=s==null?0:s.missCount;
        int open=s==null?0:s.openTrades;double pnl=s==null?0.0d:s.pnl;
        String rate=settled<=0?"N/A":percent(hit/(double)settled);
        ControlledRealContractAuditR922.Report contract=ControlledRealContractAuditR922.capture(store);
        ControlledCandidateOpportunityAuditR922.Report funnel=
                ControlledCandidateOpportunityAuditR922.capture(store,120);
        return "R923真实出手统计｜已结算REAL="+settled+"｜命中/未中="+hit+'/'+miss
                +"｜真实胜率="+rate+"｜真实盈亏="+signedNumber(pnl)+"｜未结算REAL="+open
                +"｜MODEL_PROVEN="+V51BuildManifest.MODEL_PROVEN
                +"｜REALTIME_PROVEN="+V51BuildManifest.REALTIME_PROVEN
                +"｜SHADOW_ONLY="+V51BuildManifest.SHADOW_ONLY
                +"｜受控候选门="+(V51CandidateRealGateR915.releaseBoundary()?"ACTIVE":"LOCKED")
                +"｜机会频率="+funnel.frequencyText()+"(目标15%仅观察)"
                +"｜REAL合同="+contract.status+"/invalid="+contract.invalid+"/dup="+contract.duplicateTargets
                +"｜DecisionGateEngine_ONLY";
    }

    private static String realDetail(DbStore store,SQLiteDatabase db,String summary){
        Models.RuntimeStats s=store.getRuntimeStats();
        int settled=s==null?0:s.betCount,hit=s==null?0:s.hitCount,miss=s==null?0:s.missCount;
        int open=s==null?0:s.openTrades;double pnl=s==null?0.0d:s.pnl;
        String rate=settled<=0?"N/A":percent(hit/(double)settled);
        String wilson=settled<=0?"N/A":percent(wilsonLower(hit,settled));
        String action=strScalar(db,"SELECT decision_action FROM "+DbHelper.TABLE_R35_SHADOW_OBSERVATION+" WHERE pre_draw_frozen=1 ORDER BY commit_at DESC LIMIT 1",null,"");
        String reject=strScalar(db,"SELECT reject_code FROM "+DbHelper.TABLE_R35_SHADOW_OBSERVATION+" WHERE pre_draw_frozen=1 ORDER BY commit_at DESC LIMIT 1",null,"");
        String candidate=explicit(store.getState("r915_candidate_gate_status",""),"NO_GATE_EVALUATION");
        String candidateDetail=explicit(store.getState("r915_candidate_gate_detail",""),"NO_GATE_DETAIL");
        ControlledRealContractAuditR922.Report contract=ControlledRealContractAuditR922.capture(store);
        ControlledCandidateOpportunityAuditR922.Report funnel=
                ControlledCandidateOpportunityAuditR922.capture(store,120);
        StringBuilder b=new StringBuilder(4096);
        b.append("【R923真实出手统计与硬门唯一真值】\n").append(summary)
                .append("\nREAL结算口径=trades/action=BET/stat_scope=REAL_TRADE/result=WIN|MISS")
                .append("｜样本=").append(settled).append("｜命中=").append(hit)
                .append("｜未中=").append(miss).append("｜胜率=").append(rate)
                .append("｜Wilson95下界=").append(wilson).append("｜盈亏=").append(signedNumber(pnl))
                .append("｜未结算=").append(open)
                .append("｜最新动作=").append(explicit(action,"NO_CANONICAL"))
                .append("｜Reject=").append(explicit(reject,"NONE"))
                .append("\n受控候选精确门=").append(candidate).append('｜').append(candidateDetail);
        b.append("\nR923机会漏斗=").append(funnel.summary());
        b.append("\nR923真实账本合同=").append(contract.summary());
        b.append("\n最近20条REAL结算：\n");
        appendRealSettlements(b,db,20);
        b.append("研究资产没有直接REAL权限；无有效证书时研究贡献为0；不追单、不补频率、不把SHADOW胜率冒充真实盈亏。");
        return b.toString();
    }

    private static void appendRealSettlements(StringBuilder b,SQLiteDatabase db,int limit){
        Cursor c=null;int n=0;try{c=db.rawQuery(
                "SELECT target_issue,position,n1,n2,n3,actual_number,result,hit,pnl,settled_at FROM "
                        +DbHelper.TABLE_TRADES+" WHERE settled=1 AND action='BET'"
                        +" AND stat_scope='REAL_TRADE' AND result IN('WIN','MISS')"
                        +" ORDER BY settled_at DESC,target_issue DESC LIMIT ?",
                new String[]{String.valueOf(Math.max(1,Math.min(50,limit)))});
            while(c.moveToNext()){n++;b.append(n).append("｜期=").append(explicit(c.getString(0),"N/A"))
                    .append("｜P").append(c.getInt(1)).append("｜K3=").append(two(c.getInt(2)))
                    .append('/').append(two(c.getInt(3))).append('/').append(two(c.getInt(4)))
                    .append("｜actual=").append(two(c.getInt(5))).append("｜result=")
                    .append(explicit(c.getString(6),c.getInt(7)==1?"WIN":"MISS"))
                    .append("｜pnl=").append(signedNumber(c.getDouble(8)))
                    .append("｜settledAt=").append(c.getLong(9)).append('\n');}
        }catch(Throwable e){b.append("READ_ERROR:").append(e.getClass().getSimpleName()).append('\n');}
        finally{if(c!=null)c.close();}if(n==0)b.append("NO_SAMPLE:NO_SETTLED_REAL_TRADE\n");
    }

    private static String taskDistribution(SQLiteDatabase db){
        Cursor c=null;StringBuilder b=new StringBuilder();try{c=db.rawQuery("SELECT status,task_class,COUNT(1) FROM "
                +DbHelper.TABLE_R36_SETTLEMENT_TASK+" GROUP BY status,task_class ORDER BY status,task_class",null);
            while(c.moveToNext()){if(b.length()>0)b.append(';');b.append(explicit(c.getString(0),"UNKNOWN"))
                    .append('/').append(explicit(c.getString(1),"UNKNOWN")).append('=').append(c.getInt(2));}
        }catch(Throwable e){return "READ_ERROR:"+e.getClass().getSimpleName();}finally{if(c!=null)c.close();}
        return b.length()==0?"NO_SAMPLE:NO_SETTLEMENT_TASK":b.toString();
    }

    private static SettlementCoverageTruthR906 coverage(SQLiteDatabase db){
        Cursor c=null;try{
            // R912 denominator truth: every frozen canonical prediction whose exact draw exists
            // is eligible, whether or not its task has managed to complete yet. R906 previously
            // counted COMPLETED tasks as the denominator, making a badly stuck queue look healthy.
            c=db.rawQuery("SELECT "+
                    "(SELECT COUNT(1) FROM "+DbHelper.TABLE_R35_SHADOW_OBSERVATION+" s WHERE s.pre_draw_frozen=1 AND EXISTS(SELECT 1 FROM "+DbHelper.TABLE_DRAWS+" d WHERE d.issue=s.target_issue AND d.dirty=0)),"+
                    "(SELECT COUNT(1) FROM "+DbHelper.TABLE_R35_SHADOW_OBSERVATION+" s WHERE s.pre_draw_frozen=1 AND s.settled=1 AND EXISTS(SELECT 1 FROM "+DbHelper.TABLE_R35_OOS_LABEL+" l WHERE l.target_issue=s.target_issue AND l.strict_oos=1)),"+
                    "(SELECT COUNT(1) FROM "+DbHelper.TABLE_R36_SETTLEMENT_TASK+" t WHERE t.status='NO_PREDRAW_CANONICAL_TERMINAL'),"+
                    "(SELECT COUNT(1) FROM "+DbHelper.TABLE_R36_SETTLEMENT_TASK+" t WHERE t.status IN ('FAILED_TERMINAL','INVALID_DRAW_TERMINAL')),"+
                    "(SELECT COUNT(1) FROM "+DbHelper.TABLE_R36_SETTLEMENT_TASK+" t WHERE EXISTS(SELECT 1 FROM "+DbHelper.TABLE_DRAWS+" d WHERE d.issue=t.target_issue AND d.dirty=0)),"+
                    "(SELECT COUNT(1) FROM "+DbHelper.TABLE_R36_SETTLEMENT_TASK+" t WHERE t.status IN ('COMPLETED','NO_PREDRAW_CANONICAL_TERMINAL','FAILED_TERMINAL','INVALID_DRAW_TERMINAL') AND EXISTS(SELECT 1 FROM "+DbHelper.TABLE_DRAWS+" d WHERE d.issue=t.target_issue AND d.dirty=0))",null);
            if(!c.moveToFirst())return new SettlementCoverageTruthR906(0,0,0,0,0);
            int eligible=c.getInt(0),settled=c.getInt(1),excluded=c.getInt(2),blocked=c.getInt(3),tasks=c.getInt(4),terminal=c.getInt(5);
            return new SettlementCoverageTruthR906(eligible,settled,excluded,blocked,Math.max(0,eligible-settled),tasks,terminal,Math.max(0,tasks-terminal));
        }catch(Throwable e){return new SettlementCoverageTruthR906(0,0,0,0,0);}
        finally{if(c!=null)c.close();}
    }

    private static TruthRow latestSettled(SQLiteDatabase db,int hit){return truthRow(db," WHERE o.pre_draw_frozen=1 AND o.settled=1 AND o.hit_label="+hit+" ORDER BY o.settled_at DESC,o.target_issue DESC LIMIT 1",null);}
    private static TruthRow settledFor(SQLiteDatabase db,String issue){return truthRow(db," WHERE o.pre_draw_frozen=1 AND o.settled=1 AND o.target_issue=? LIMIT 1",new String[]{issue});}
    private static TruthRow latestCanonical(SQLiteDatabase db){return truthRow(db," WHERE o.pre_draw_frozen=1 ORDER BY o.commit_at DESC,o.target_issue DESC LIMIT 1",null);}
    private static TruthRow truthRow(SQLiteDatabase db,String suffix,String[] args){Cursor c=null;try{
        c=db.rawQuery("SELECT o.target_issue,o.position,o.n1,o.n2,o.n3,o.actual_number,o.hit_label,o.decision_action,o.reject_code FROM "+DbHelper.TABLE_R35_SHADOW_OBSERVATION+" o"+suffix,args);
        if(!c.moveToFirst())return null; return new TruthRow(c.getString(0),c.getInt(1),c.getInt(2),c.getInt(3),c.getInt(4),c.getInt(5),c.getInt(6),c.getString(7),c.getString(8));
    }catch(Throwable e){return null;}finally{if(c!=null)c.close();}}

    private static Evidence evidence(SQLiteDatabase db,String target){Cursor c=null;try{
        c=db.rawQuery("SELECT record_hash,frozen_at,rank_no,raw_action_p_value,raw_action_p_status,rank_score_value,rank_status,third_margin_value,third_margin_status,entropy_risk_value,entropy_risk_status,stability_value,stability_status,window_consensus_value,window_consensus_status,assignment_support_value,assignment_support_status FROM "+DbHelper.TABLE_R906_SELECTED_EVIDENCE+" WHERE target_issue=? LIMIT 1",new String[]{target});
        if(!c.moveToFirst())return new Evidence(); Evidence e=new Evidence();e.recordHash=safe(c.getString(0));e.frozenAt=c.getLong(1);e.rankNo=c.isNull(2)?0:c.getInt(2);
        e.raw=nullable(c,3);e.rawStatus=safe(c.getString(4));e.rankScore=nullable(c,5);e.rankStatus=safe(c.getString(6));e.third=nullable(c,7);e.thirdStatus=safe(c.getString(8));e.entropy=nullable(c,9);e.entropyStatus=safe(c.getString(10));e.stability=nullable(c,11);e.stabilityStatus=safe(c.getString(12));e.window=nullable(c,13);e.windowStatus=safe(c.getString(14));e.assignment=nullable(c,15);e.assignmentStatus=safe(c.getString(16));return e;
    }catch(Throwable x){return new Evidence();}finally{if(c!=null)c.close();}}

    private static String currentAction(SQLiteDatabase db,String target){return strScalar(db,"SELECT decision_action FROM "+DbHelper.TABLE_R35_SHADOW_OBSERVATION+" WHERE target_issue=? LIMIT 1",new String[]{target},"");}
    private static String currentReject(SQLiteDatabase db,String target){return strScalar(db,"SELECT reject_code FROM "+DbHelper.TABLE_R35_SHADOW_OBSERVATION+" WHERE target_issue=? LIMIT 1",new String[]{target},"");}
    private static String stateValue(SQLiteDatabase db,String key){return strScalar(db,
            "SELECT state_value FROM "+DbHelper.TABLE_APP_STATE+" WHERE state_key=? LIMIT 1",
            new String[]{key},"");}

    private static String shapeTruth(SQLiteDatabase db,String target){Cursor c=null;try{
        c=db.rawQuery("SELECT shape_lifecycle,shape_prototype,shape_run_length,shape_change_risk,shape_confidence FROM "+V51Repository.SELECTED+" WHERE target_issue=? LIMIT 1",new String[]{target});
        if(!c.moveToFirst())return "不可用｜原因=SHAPE_NOT_FROZEN";
        String lifecycle=explicit(c.getString(0),"UNKNOWN"),prototype=explicit(c.getString(1),"N/A");
        String change=c.getDouble(3)<0?"N/A":String.format(Locale.US,"%.2f%%",c.getDouble(3)*100d);
        String confidence=c.getDouble(4)<0?"N/A":String.format(Locale.US,"%.2f%%",c.getDouble(4)*100d);
        return "生命周期="+lifecycle+"｜原型="+prototype+"｜连续="+Math.max(0,c.getInt(2))+"｜变局风险="+change+"｜置信="+confidence;
    }catch(Throwable e){return "不可用｜原因=SHAPE_SCHEMA_OR_ROW_MISSING";}finally{if(c!=null)c.close();}}

    private static String firstState(DbStore store,String primary,String legacy,String fallback){
        if(store==null)return fallback;String one=safe(store.getState(primary,""));
        return one.length()>0?one:explicit(store.getState(legacy,""),fallback);
    }

    private static String canonical(String target,String draw,String canonical,String closure,String oos,long version,TruthCard[] cards,StateBox[] boxes,SettlementCoverageTruthR906 cov,String research,String gate){
        StringBuilder b=new StringBuilder(4096);b.append(VERSION).append('|').append(target).append('|').append(draw).append('|').append(canonical).append('|').append(closure).append('|').append(oos).append('|').append(version).append('|').append(cov.canonical()).append('|').append(research).append('|').append(gate);
        for(TruthCard c:cards)b.append('|').append(c.canonical());for(StateBox s:boxes)b.append('|').append(s.canonical());return b.toString();
    }
    private static int intScalar(SQLiteDatabase db,String sql,String[] args,int d){Cursor c=null;try{c=db.rawQuery(sql,args);return c.moveToFirst()&&!c.isNull(0)?c.getInt(0):d;}catch(Throwable e){return d;}finally{if(c!=null)c.close();}}
    private static long longScalar(SQLiteDatabase db,String sql,String[] args,long d){Cursor c=null;try{c=db.rawQuery(sql,args);return c.moveToFirst()&&!c.isNull(0)?c.getLong(0):d;}catch(Throwable e){return d;}finally{if(c!=null)c.close();}}
    private static String strScalar(SQLiteDatabase db,String sql,String[] args,String d){Cursor c=null;try{c=db.rawQuery(sql,args);return c.moveToFirst()&&!c.isNull(0)?safe(c.getString(0)):d;}catch(Throwable e){return d;}finally{if(c!=null)c.close();}}
    private static double nullable(Cursor c,int i){return c.isNull(i)?Double.NaN:c.getDouble(i);}
    private static String metric(double v,String unit){if(Double.isNaN(v)||Double.isInfinite(v))return "不可用｜原因=VALUE_MISSING";if("PROBABILITY".equals(unit))return String.format(Locale.US,"%.2f%%",v*100d);return String.format(Locale.US,"%.5f",v);}
    private static String number(double v){return Double.isNaN(v)||Double.isInfinite(v)?"N/A":String.format(Locale.US,"%.5f",v);}
    private static String signedNumber(double v){return Double.isNaN(v)||Double.isInfinite(v)?"N/A":String.format(Locale.US,"%+.2f",v);}
    private static String percent(double v){return Double.isNaN(v)||Double.isInfinite(v)?"N/A":String.format(Locale.US,"%.2f%%",v*100d);}
    private static String percentSigned(double v){return Double.isNaN(v)||Double.isInfinite(v)?"N/A":String.format(Locale.US,"%+.2f%%",v*100d);}
    private static double wilsonLower(int hit,int n){if(n<=0)return 0.0d;double z=1.959963984540054d,p=hit/(double)n,
        den=1.0d+z*z/n,center=p+z*z/(2.0d*n),margin=z*Math.sqrt((p*(1.0d-p)+z*z/(4.0d*n))/n);
        return Math.max(0.0d,Math.min(1.0d,(center-margin)/den));}
    private static String clip(String s,int max){String x=safe(s);int n=Math.max(8,max);return x.length()<=n?x:x.substring(0,n)+"…";}
    private static String joinNonEmpty(String separator,String... values){StringBuilder b=new StringBuilder();if(values!=null)for(String value:values){String x=safe(value);if(x.length()==0)continue;if(b.length()>0)b.append(separator);b.append(x);}return b.toString();}
    private static String k3(TruthRow r){return two(r.n1)+"/"+two(r.n2)+"/"+two(r.n3);} private static String two(int n){return n<10?"0"+n:String.valueOf(n);}
    private static String shortHash(String s){String x=safe(s);return x.length()==0?"N/A":(x.length()<=12?x:x.substring(0,12));}
    private static String explicit(String s,String d){String x=safe(s);return x.length()==0?d:x;} private static String safe(String s){return s==null?"":s.trim();}
    private static final class TruthRow{final String issue,action,reject;final int position,n1,n2,n3,actual,hit;TruthRow(String i,int p,int a,int b,int c,int actual,int hit,String action,String reject){issue=safe(i);position=p;n1=a;n2=b;n3=c;this.actual=actual;this.hit=hit;this.action=safe(action);this.reject=safe(reject);}}
    private static final class Evidence{String recordHash="",rawStatus="UNAVAILABLE",rankStatus="UNAVAILABLE",thirdStatus="UNAVAILABLE",entropyStatus="UNAVAILABLE",stabilityStatus="UNAVAILABLE",windowStatus="UNAVAILABLE",assignmentStatus="UNAVAILABLE";long frozenAt;int rankNo;double raw=Double.NaN,rankScore=Double.NaN,third=Double.NaN,entropy=Double.NaN,stability=Double.NaN,window=Double.NaN,assignment=Double.NaN;}
    private static final class ExplanationEvidence{String preDraw="",whyHit="",whyMiss="",postSettlement="";}

    // CP12 source-bound immutable-snapshot overload: buildTruthCards
public static TruthCard[] buildTruthCardsSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, SQLiteDatabase db,String target,SettlementCoverageTruthR906 cov){
        TruthRow hit=latestSettledSnapshotCP4(report, db,1),miss=latestSettledSnapshotCP4(report, db,0),current=latestCanonicalSnapshotCP4(report, db);
        String candidateStatus=stateValueSnapshotCP4(report, db,"r915_candidate_gate_status");
        String candidateDetail=stateValueSnapshotCP4(report, db,"r915_candidate_gate_detail");
        String hitText=hit==null?"暂无已结算命中样本":mechanicalSettlementWhySnapshotCP4(report, db,hit,true);
        String missText=miss==null?"暂无已结算未中样本":mechanicalSettlementWhySnapshotCP4(report, db,miss,false);
        String noBet;
        if(current==null)noBet="等待赛前冻结决策";
        else if("BET".equals(current.action))
            noBet="本期已受控出手｜CONTROLLED_CANDIDATE｜MODEL_NOT_PROVEN｜不把raw当校准胜率";
        else noBet=explicitSnapshotCP4(report, current.action,"SHADOW")+"｜拒绝原因="
                    +explicitSnapshotCP4(report, current.reject,"未达到R915受控候选门")
                    +"｜受控候选精确门="+explicitSnapshotCP4(report, candidateStatus,"NO_GATE_EVALUATION")
                    +"｜门值="+clipSnapshotCP4(report, explicitSnapshotCP4(report, candidateDetail,"NO_GATE_DETAIL"),420)
                    +"｜下期重新独立评估，不追单不补频率";
        String settle;
        if(cov.drawTaskTotal==0&&cov.predictedEligible==0)settle="等待首个合法开奖任务";
        else settle="开奖任务终态="+cov.drawTaskTerminal+"/"+cov.drawTaskTotal
                +"｜开奖任务待闭环="+cov.drawTaskOpen
                +"｜有赛前预测应统计="+cov.predictedEligible
                +"｜命中统计已结算="+cov.settledPredictions
                +"｜预测待结算="+cov.openEligible
                +"｜无赛前预测排除="+cov.excludedNoPredraw
                +"｜数据阻断="+cov.blocked;
        return new TruthCard[]{
            new TruthCard("WHY_HIT","为什么中",hitText,hit==null?target:hit.issue,"frozen_predraw+exact_draw",hit==null?"NO_SAMPLE":"AVAILABLE"),
            new TruthCard("WHY_MISS","为什么不中",missText,miss==null?target:miss.issue,"frozen_predraw+exact_draw",miss==null?"NO_SAMPLE":"AVAILABLE"),
            new TruthCard("WHY_NO_BET","为什么不出手",noBet,current==null?target:current.issue,"DecisionGateEngine","AVAILABLE"),
            new TruthCard("SETTLEMENT","逐期结算统计",settle,target,"SettlementTaskStoreR36+OosAfterDrawWriterR36","AVAILABLE")};
    }

    // CP12 source-bound immutable-snapshot overload: explicit
public static String explicitSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, String s,String d){String x=safeSnapshotCP4(report, s);return x.length()==0?d:x;}

    // CP12 source-bound immutable-snapshot overload: safe
public static String safeSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, String s){return s==null?"":s.trim();}

    // CP12 source-bound immutable-snapshot overload: clip
public static String clipSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, String s,int max){String x=safeSnapshotCP4(report, s);int n=Math.max(8,max);return x.length()<=n?x:x.substring(0,n)+"…";}

    // CP12 source-bound immutable-snapshot overload: mechanicalSettlementWhy
public static String mechanicalSettlementWhySnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, SQLiteDatabase db,TruthRow row,boolean hit){
        Evidence e=evidenceSnapshotCP4(report, db,row.issue);
        ExplanationEvidence explanation=explanationEvidenceSnapshotCP4(report, db,row.issue);
        String direct=(hit?"命中":"未中")+"｜期"+row.issue+"｜位置"+row.position
                +"｜实际"+twoSnapshotCP4(report, row.actual)+(hit?"∈":"∉")+"赛前冻结K3("+k3SnapshotCP4(report, row)+")";
        String frozen=e.recordHash.length()==0
                ? "赛前量化证据行不可用"
                : "赛前量化：原始评分(未校准)="+metricSnapshotCP4(report, e.raw,"SCORE")
                +"，Rank="+(e.rankNo>0?e.rankNo+"/1200":"N/A")
                +"，动作稳定性="+metricSnapshotCP4(report, e.stability,"PROBABILITY")
                +"，号码风险="+metricSnapshotCP4(report, e.entropy,"PROBABILITY");
        String settled=hit?explanation.whyHit:explanation.whyMiss;
        if(safeSnapshotCP4(report, settled).length()==0)settled=hit
                ?"结构命中：实际号码落入同一期赛前冻结K3；教师/趋势/形态仅解释贡献，不改写冻结结果"
                :"结构未中：实际号码落在冻结K3之外；反推层检查第三边界、熵风险、教师反证和跨位分配是否误导";
        return direct+"｜"+frozen
                +"｜赛前Teacher/矩阵="+clipSnapshotCP4(report, explicitSnapshotCP4(report, explanation.preDraw,"N/A"),420)
                +"｜结算归因="+clipSnapshotCP4(report, explicitSnapshotCP4(report, settled,"机械归因：实际号码与冻结K3的包含关系"),420)
                +"｜开奖后反哺="+clipSnapshotCP4(report, explicitSnapshotCP4(report, explanation.postSettlement,"N/A"),300)
                +"｜时间边界=赛前证据与开奖后学习分栏，禁止后者冒充赛前理由";
    }

    // CP12 source-bound immutable-snapshot overload: metric
public static String metricSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, double v,String unit){if(Double.isNaN(v)||Double.isInfinite(v))return "不可用｜原因=VALUE_MISSING";if("PROBABILITY".equals(unit))return String.format(Locale.US,"%.2f%%",v*100d);return String.format(Locale.US,"%.5f",v);}

    // CP12 source-bound immutable-snapshot overload: k3
public static String k3SnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, TruthRow r){return twoSnapshotCP4(report, r.n1)+"/"+twoSnapshotCP4(report, r.n2)+"/"+twoSnapshotCP4(report, r.n3);}

    // CP12 source-bound immutable-snapshot overload: two
public static String twoSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, int n){return n<10?"0"+n:String.valueOf(n);}

    // CP12 source-bound immutable-snapshot overload: explanationEvidence
public static ExplanationEvidence explanationEvidenceSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, SQLiteDatabase db,String issue){Cursor c=null;try{
        c=report.select("SELECT teacher_matrix_summary,v20_decision_reason,dynamic_causal_summary,"
                        +"why_hit,why_miss,miss_reason_detail,learning_summary,reason_summary,pre_draw_reason_text "
                        +"FROM "+DbHelper.TABLE_DECISIONS+" WHERE target_issue=? ORDER BY created_at DESC LIMIT 1",
                new String[]{issue});
        if(!c.moveToFirst())return bridgeExplanationSnapshotCP4(report, db,issue,new ExplanationEvidence());
        ExplanationEvidence out=new ExplanationEvidence();
        out.preDraw=joinNonEmptySnapshotCP4(report, "；",safeSnapshotCP4(report, c.getString(0)),safeSnapshotCP4(report, c.getString(1)),safeSnapshotCP4(report, c.getString(2)),safeSnapshotCP4(report, c.getString(8)));
        out.whyHit=safeSnapshotCP4(report, c.getString(3));
        out.whyMiss=joinNonEmptySnapshotCP4(report, "；",safeSnapshotCP4(report, c.getString(4)),safeSnapshotCP4(report, c.getString(5)));
        out.postSettlement=joinNonEmptySnapshotCP4(report, "；",safeSnapshotCP4(report, c.getString(6)),safeSnapshotCP4(report, c.getString(7)));
        return bridgeExplanationSnapshotCP4(report, db,issue,out);
    }catch(Throwable ignored){throw new com.ethk3.novaonepro.ReportReadContextCP4.ReportReadFailureCP4("REPORT_QUERY_OR_FORMAT_FAILED",ignored);}finally{if(c!=null)c.close();}}

    // CP12 source-bound immutable-snapshot overload: bridgeExplanation
public static ExplanationEvidence bridgeExplanationSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, 
            SQLiteDatabase db,String issue,ExplanationEvidence out){
        LegacyPredrawContextBridgeR915.Row row=LegacyPredrawContextBridgeR915.readSnapshotCP4(report, db,issue);
        if(row==null){
            if(safeSnapshotCP4(report, out.preDraw).length()==0)
                out.preDraw=LegacyPredrawContextBridgeR915.missingReasonSnapshotCP4(report, db,issue,false);
        }else if(safeSnapshotCP4(report, out.preDraw).length()==0)out.preDraw=joinNonEmptySnapshotCP4(report, "；",
                    "教师矩阵="+row.teacher,"正向推理="+row.reasoning,"反推矩阵="+row.reverse,
                    "因果矩阵="+row.causal,"趋势="+row.trend,"形态="+row.shape,"关联="+row.relation);
        LegacyPredrawContextBridgeR915.PostRow post=LegacyPredrawContextBridgeR915.readPostSnapshotCP4(report, db,issue);
        if(post==null){
            if(safeSnapshotCP4(report, out.postSettlement).length()==0)
                out.postSettlement=LegacyPredrawContextBridgeR915.missingReasonSnapshotCP4(report, db,issue,true);
        }else{
            if(post.hitLabel==1&&safeSnapshotCP4(report, out.whyHit).length()==0)out.whyHit=post.attribution;
            if(post.hitLabel==0&&safeSnapshotCP4(report, out.whyMiss).length()==0)out.whyMiss=post.attribution;
            if(safeSnapshotCP4(report, out.postSettlement).length()==0)out.postSettlement=post.feedback;
        }
        return out;
    }

    // CP12 source-bound immutable-snapshot overload: joinNonEmpty
public static String joinNonEmptySnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, String separator,String... values){com.ethk3.novaonepro.ReportTextCP12 b=new com.ethk3.novaonepro.ReportTextCP12(report);if(values!=null)for(String value:values){String x=safeSnapshotCP4(report, value);if(x.length()==0)continue;if(b.length()>0)b.append(separator);b.append(x);}return b.toString();}

    // CP12 source-bound immutable-snapshot overload: evidence
public static Evidence evidenceSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, SQLiteDatabase db,String target){Cursor c=null;try{
        c=report.select("SELECT record_hash,frozen_at,rank_no,raw_action_p_value,raw_action_p_status,rank_score_value,rank_status,third_margin_value,third_margin_status,entropy_risk_value,entropy_risk_status,stability_value,stability_status,window_consensus_value,window_consensus_status,assignment_support_value,assignment_support_status FROM "+DbHelper.TABLE_R906_SELECTED_EVIDENCE+" WHERE target_issue=? LIMIT 1",new String[]{target});
        if(!c.moveToFirst())return new Evidence(); Evidence e=new Evidence();e.recordHash=safeSnapshotCP4(report, c.getString(0));e.frozenAt=c.getLong(1);e.rankNo=c.isNull(2)?0:c.getInt(2);
        e.raw=nullableSnapshotCP4(report, c,3);e.rawStatus=safeSnapshotCP4(report, c.getString(4));e.rankScore=nullableSnapshotCP4(report, c,5);e.rankStatus=safeSnapshotCP4(report, c.getString(6));e.third=nullableSnapshotCP4(report, c,7);e.thirdStatus=safeSnapshotCP4(report, c.getString(8));e.entropy=nullableSnapshotCP4(report, c,9);e.entropyStatus=safeSnapshotCP4(report, c.getString(10));e.stability=nullableSnapshotCP4(report, c,11);e.stabilityStatus=safeSnapshotCP4(report, c.getString(12));e.window=nullableSnapshotCP4(report, c,13);e.windowStatus=safeSnapshotCP4(report, c.getString(14));e.assignment=nullableSnapshotCP4(report, c,15);e.assignmentStatus=safeSnapshotCP4(report, c.getString(16));return e;
    }catch(Throwable x){throw new com.ethk3.novaonepro.ReportReadContextCP4.ReportReadFailureCP4("REPORT_QUERY_OR_FORMAT_FAILED",x);}finally{if(c!=null)c.close();}}

    // CP12 source-bound immutable-snapshot overload: nullable
public static double nullableSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, Cursor c,int i){return c.isNull(i)?Double.NaN:c.getDouble(i);}

    // CP12 source-bound immutable-snapshot overload: stateValue
public static String stateValueSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, SQLiteDatabase db,String key){return strScalarSnapshotCP4(report, db,
            "SELECT state_value FROM "+DbHelper.TABLE_APP_STATE+" WHERE state_key=? LIMIT 1",
            new String[]{key},"");}

    // CP12 source-bound immutable-snapshot overload: strScalar
public static String strScalarSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, SQLiteDatabase db,String sql,String[] args,String d){Cursor c=null;try{c=report.select(sql,args);return c.moveToFirst()&&!c.isNull(0)?safeSnapshotCP4(report, c.getString(0)):d;}catch(Throwable e){throw new com.ethk3.novaonepro.ReportReadContextCP4.ReportReadFailureCP4("REPORT_QUERY_OR_FORMAT_FAILED",e);}finally{if(c!=null)c.close();}}

    // CP12 source-bound immutable-snapshot overload: latestCanonical
public static TruthRow latestCanonicalSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, SQLiteDatabase db){return truthRowSnapshotCP4(report, db," WHERE o.pre_draw_frozen=1 ORDER BY o.commit_at DESC,o.target_issue DESC LIMIT 1",null);}

    // CP12 source-bound immutable-snapshot overload: truthRow
public static TruthRow truthRowSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, SQLiteDatabase db,String suffix,String[] args){Cursor c=null;try{
        c=report.select("SELECT o.target_issue,o.position,o.n1,o.n2,o.n3,o.actual_number,o.hit_label,o.decision_action,o.reject_code FROM "+DbHelper.TABLE_R35_SHADOW_OBSERVATION+" o"+suffix,args);
        if(!c.moveToFirst())return null; return new TruthRow(c.getString(0),c.getInt(1),c.getInt(2),c.getInt(3),c.getInt(4),c.getInt(5),c.getInt(6),c.getString(7),c.getString(8));
    }catch(Throwable e){throw new com.ethk3.novaonepro.ReportReadContextCP4.ReportReadFailureCP4("REPORT_QUERY_OR_FORMAT_FAILED",e);}finally{if(c!=null)c.close();}}

    // CP12 source-bound immutable-snapshot overload: latestSettled
public static TruthRow latestSettledSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, SQLiteDatabase db,int hit){return truthRowSnapshotCP4(report, db," WHERE o.pre_draw_frozen=1 AND o.settled=1 AND o.hit_label="+hit+" ORDER BY o.settled_at DESC,o.target_issue DESC LIMIT 1",null);}

    // CP12 source-bound immutable-snapshot overload: realDetail
public static String realDetailSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, com.ethk3.novaonepro.ReportReadContextCP4 store,SQLiteDatabase db,String summary){
        Models.RuntimeStats s=store.getRuntimeStats();
        int settled=s==null?0:s.betCount,hit=s==null?0:s.hitCount,miss=s==null?0:s.missCount;
        int open=s==null?0:s.openTrades;double pnl=s==null?0.0d:s.pnl;
        String rate=settled<=0?"N/A":percentSnapshotCP4(report, hit/(double)settled);
        String wilson=settled<=0?"N/A":percentSnapshotCP4(report, wilsonLowerSnapshotCP4(report, hit,settled));
        String action=strScalarSnapshotCP4(report, db,"SELECT decision_action FROM "+DbHelper.TABLE_R35_SHADOW_OBSERVATION+" WHERE pre_draw_frozen=1 ORDER BY commit_at DESC LIMIT 1",null,"");
        String reject=strScalarSnapshotCP4(report, db,"SELECT reject_code FROM "+DbHelper.TABLE_R35_SHADOW_OBSERVATION+" WHERE pre_draw_frozen=1 ORDER BY commit_at DESC LIMIT 1",null,"");
        String candidate=explicitSnapshotCP4(report, store.getState("r915_candidate_gate_status",""),"NO_GATE_EVALUATION");
        String candidateDetail=explicitSnapshotCP4(report, store.getState("r915_candidate_gate_detail",""),"NO_GATE_DETAIL");
        ControlledRealContractAuditR922.Report contract=ControlledRealContractAuditR922.captureSnapshotCP4(report, store);
        ControlledCandidateOpportunityAuditR922.Report funnel=
                ControlledCandidateOpportunityAuditR922.captureSnapshotCP4(report, store,120);
        com.ethk3.novaonepro.ReportTextCP12 b=new com.ethk3.novaonepro.ReportTextCP12(report, 4096);
        b.append("【R923真实出手统计与硬门唯一真值】\n").append(summary)
                .append("\nREAL结算口径=trades/action=BET/stat_scope=REAL_TRADE/result=WIN|MISS")
                .append("｜样本=").append(settled).append("｜命中=").append(hit)
                .append("｜未中=").append(miss).append("｜胜率=").append(rate)
                .append("｜Wilson95下界=").append(wilson).append("｜盈亏=").append(signedNumberSnapshotCP4(report, pnl))
                .append("｜未结算=").append(open)
                .append("｜最新动作=").append(explicitSnapshotCP4(report, action,"NO_CANONICAL"))
                .append("｜Reject=").append(explicitSnapshotCP4(report, reject,"NONE"))
                .append("\n受控候选精确门=").append(candidate).append('｜').append(candidateDetail);
        b.append("\nR923机会漏斗=").append(funnel.summary());
        b.append("\nR923真实账本合同=").append(contract.summary());
        b.append("\n最近20条REAL结算：\n");
        appendRealSettlementsSnapshotCP4(report, b,db,20);
        b.append("研究资产没有直接REAL权限；无有效证书时研究贡献为0；不追单、不补频率、不把SHADOW胜率冒充真实盈亏。");
        return b.toString();
    }

    // CP12 source-bound immutable-snapshot overload: appendRealSettlements
public static void appendRealSettlementsSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, com.ethk3.novaonepro.ReportTextCP12 b,SQLiteDatabase db,int limit){
        Cursor c=null;int n=0;try{c=report.select(
                "SELECT target_issue,position,n1,n2,n3,actual_number,result,hit,pnl,settled_at FROM "
                        +DbHelper.TABLE_TRADES+" WHERE settled=1 AND action='BET'"
                        +" AND stat_scope='REAL_TRADE' AND result IN('WIN','MISS')"
                        +" ORDER BY settled_at DESC,target_issue DESC LIMIT ?",
                new String[]{String.valueOf(Math.max(1,Math.min(50,limit)))});
            while(c.moveToNext()){n++;b.append(n).append("｜期=").append(explicitSnapshotCP4(report, c.getString(0),"N/A"))
                    .append("｜P").append(c.getInt(1)).append("｜K3=").append(twoSnapshotCP4(report, c.getInt(2)))
                    .append('/').append(twoSnapshotCP4(report, c.getInt(3))).append('/').append(twoSnapshotCP4(report, c.getInt(4)))
                    .append("｜actual=").append(twoSnapshotCP4(report, c.getInt(5))).append("｜result=")
                    .append(explicitSnapshotCP4(report, c.getString(6),c.getInt(7)==1?"WIN":"MISS"))
                    .append("｜pnl=").append(signedNumberSnapshotCP4(report, c.getDouble(8)))
                    .append("｜settledAt=").append(c.getLong(9)).append('\n');}
        }catch(Throwable e){throw new com.ethk3.novaonepro.ReportReadContextCP4.ReportReadFailureCP4("REPORT_QUERY_OR_FORMAT_FAILED",e);}
        finally{if(c!=null)c.close();}if(n==0)b.append("NO_SAMPLE:NO_SETTLED_REAL_TRADE\n");
    }

    // CP12 source-bound immutable-snapshot overload: signedNumber
public static String signedNumberSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, double v){return Double.isNaN(v)||Double.isInfinite(v)?"N/A":String.format(Locale.US,"%+.2f",v);}

    // CP12 source-bound immutable-snapshot overload: wilsonLower
public static double wilsonLowerSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, int hit,int n){if(n<=0)return 0.0d;double z=1.959963984540054d,p=hit/(double)n,
        den=1.0d+z*z/n,center=p+z*z/(2.0d*n),margin=z*Math.sqrt((p*(1.0d-p)+z*z/(4.0d*n))/n);
        return Math.max(0.0d,Math.min(1.0d,(center-margin)/den));}

    // CP12 source-bound immutable-snapshot overload: percent
public static String percentSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, double v){return Double.isNaN(v)||Double.isInfinite(v)?"N/A":String.format(Locale.US,"%.2f%%",v*100d);}

    // CP12 source-bound immutable-snapshot overload: researchDetail
public static String researchDetailSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, com.ethk3.novaonepro.ReportReadContextCP4 store,SQLiteDatabase db,String summary){
        com.ethk3.novaonepro.ReportTextCP12 b=new com.ethk3.novaonepro.ReportTextCP12(report, 16384);
        b.append("【R923深度学习・动态迭代・算法创造唯一真值】\n").append(summary).append('\n');
        b.append("快照Owner=RuntimeTruthSnapshotR906｜Legacy只读、GENESIS_NEW可进化、Research无直接REAL权限。\n");
        b.append("原子执行器=").append(GenesisAtomicWorldR920.VERSION)
                .append("｜生产评分语义实装=").append(GenesisSchemaRegistryR915.productionScoreEligibleCount())
                .append("｜A10/A11研究代理=").append(GenesisSchemaRegistryR915.researchProxyCount())
                .append("（新建/变异/制证链已隔离）｜禁止再声明240/240全部实装\n");
        b.append("原子边界=240项逐项登记datatype/range/window/lag/support/leakage/updateTiming/cost/version；"
                +"新建可制证程序只可选择").append(GenesisSchemaRegistryR915.productionScoreEligibleCount())
                .append("项生产评分语义原子；A9开奖后证据").append(GenesisSchemaRegistryR915.settlementEvidenceCount())
                .append("项只能进入生命周期账本，禁止作为本期评分输入。\n");
        b.append("算法失败向量=").append(explicitSnapshotCP4(report, store.getState("r919_algorithm_failure_vector",""),"NO_PARENT_OR_NOT_RUN")).append('\n');
        b.append("算法创造原因=").append(explicitSnapshotCP4(report, store.getState("r919_algorithm_creation_reasons",""),"NO_SURVIVOR_OR_NOT_RUN")).append('\n');
        b.append("R923 Regime支持审计=").append(explicitSnapshotCP4(report, 
                store.getState("r923_algorithm_regime_support_audit",""),
                "WAIT_NEXT_ALGORITHM_LIFECYCLE")).append('\n');
        b.append("连续真实未中分析=").append(explicitSnapshotCP4(report, 
                store.getState("r923_miss_streak_analysis",""),"NO_PROVEN_REAL_SAMPLE"))
                .append("｜研究步骤=").append(explicitSnapshotCP4(report, 
                        store.getState("r923_miss_streak_research_step",""),"IDLE")).append('\n');
        b.append("研究连续性=").append(explicitSnapshotCP4(report, store.getState("r912_research_continuity_status",""),"NO_PULSE_RECORDED")).append('\n');
        b.append("研究调度=").append(explicitSnapshotCP4(report, store.getState("r908_research_tick_status",""),"NO_TICK_RECORDED"))
                .append("｜阶段=").append(explicitSnapshotCP4(report, store.getState("r908_research_phase",""),"0"))
                .append("｜成本=").append(explicitSnapshotCP4(report, store.getState("r908_research_tick_cost_ms",""),"N/A")).append("ms\n");
        b.append("R923动态启动=").append(explicitSnapshotCP4(report, store.getState("r922_dynamic_bootstrap_truth",""),
                "NO_BOOTSTRAP_PULSE_RECORDED")).append('\n');
        b.append("R947逐期动态进化=").append(explicitSnapshotCP4(report, store.getState("r947_evolution_heartbeat",""),
                explicitSnapshotCP4(report, store.getState("r947_evolution_status",""),"NO_EVOLUTION_HEARTBEAT")))
                .append("｜完成到=").append(explicitSnapshotCP4(report, store.getState("r947_evolution_completed_through",""),"N/A"))
                .append("｜review总数=").append(explicitSnapshotCP4(report, store.getState("r947_evolution_reviews_total",""),"0"))
                .append("｜stage尝试=").append(explicitSnapshotCP4(report, store.getState("r947_evolution_stage_attempts_total",""),"0"))
                .append("｜新子代=").append(explicitSnapshotCP4(report, store.getState("r947_evolution_children_created_total",""),"0"))
                .append("｜backlog=").append(explicitSnapshotCP4(report, store.getState("r947_evolution_backlog",""),"0")).append('\n');
        b.append("本轮变化=").append(explicitSnapshotCP4(report, store.getState("r922_research_last_delta",""),
                "NO_DELTA_SNAPSHOT")).append("｜at=")
                .append(explicitSnapshotCP4(report, store.getState("r922_research_last_delta_at",""),"N/A")).append('\n');
        b.append("最近实质变化=").append(explicitSnapshotCP4(report, store.getState("r922_research_last_material_change",""),
                "NO_MATERIAL_CHANGE_YET")).append("｜at=")
                .append(explicitSnapshotCP4(report, store.getState("r922_research_last_material_change_at",""),"N/A")).append('\n');
        b.append("创生维护=").append(explicitSnapshotCP4(report, store.getState("r915_asset_genesis_tick",""),"NO_TICK_RECORDED"))
                .append("｜最近生成=").append(explicitSnapshotCP4(report, store.getState("r915_genesis_last_generated_names",""),"NO_GENERATION"))
                .append("｜失败向量=").append(explicitSnapshotCP4(report, store.getState("r915_genesis_last_failure",""),"NO_FAILURE_VECTOR"))
                .append("｜需求计划=").append(explicitSnapshotCP4(report, store.getState("r915_genesis_last_need",""),"NO_NEED_PLAN")).append('\n');
        b.append("赛前冻结=").append(explicitSnapshotCP4(report, store.getState("r915_genesis_predraw_target",""),"NO_TARGET"))
                .append('/').append(explicitSnapshotCP4(report, store.getState("r915_genesis_predraw_frozen",""),"0"))
                .append("｜最近结算=").append(explicitSnapshotCP4(report, store.getState("r915_genesis_last_settled_issue",""),"NO_SETTLEMENT"))
                .append('/').append(explicitSnapshotCP4(report, store.getState("r915_genesis_last_settled_rows",""),"0")).append('\n');
        b.append("算法捕获=").append(explicitSnapshotCP4(report, store.getState("r919_algorithm_capture_status",""),"NO_ATTEMPT"))
                .append("｜算法逐期结算=").append(explicitSnapshotCP4(report, store.getState("r919_algorithm_post_commit_status",""),"NO_SETTLEMENT"))
                .append('/').append(explicitSnapshotCP4(report, store.getState("r919_algorithm_post_commit_issue",""),"NO_ISSUE"))
                .append('/').append(explicitSnapshotCP4(report, store.getState("r919_algorithm_post_commit_rows",""),"0")).append('\n');
        b.append("公式逐期结算=").append(explicitSnapshotCP4(report, store.getState("r919_formula_post_commit_status",""),"NO_SETTLEMENT"))
                .append('/').append(explicitSnapshotCP4(report, store.getState("r919_formula_post_commit_issue",""),"NO_ISSUE"))
                .append('/').append(explicitSnapshotCP4(report, store.getState("r919_formula_post_commit_rows",""),"0"))
                .append("｜创生逐期结算=").append(explicitSnapshotCP4(report, store.getState("r915_genesis_post_commit_status",""),"NO_SETTLEMENT"))
                .append('/').append(explicitSnapshotCP4(report, store.getState("r915_genesis_post_commit_issue",""),"NO_ISSUE"))
                .append('/').append(explicitSnapshotCP4(report, store.getState("r915_genesis_post_commit_rows",""),"0")).append('\n');
        b.append("Teacher反哺逐期结算=").append(explicitSnapshotCP4(report, store.getState("r919_teacher_post_commit_status",""),"NO_SETTLEMENT"))
                .append('/').append(explicitSnapshotCP4(report, store.getState("r919_teacher_post_commit_issue",""),"NO_ISSUE"))
                .append('/').append(explicitSnapshotCP4(report, store.getState("r919_teacher_post_commit_rows",""),"0")).append('\n');
        appendAtomRegistrySnapshotCP4(report, b);
        appendGenesisAssetsSnapshotCP4(report, b,db,12);
        appendAlgorithmsSnapshotCP4(report, b,db,12);
        appendArchivedAlgorithmsSnapshotCP4(report, b,db,12);
        appendAtomProposalsSnapshotCP4(report, b,db,8);
        appendContributionsSnapshotCP4(report, b,db,8);
        b.append("\n【R923逐期预测主链贡献审计】\n")
                .append(ResearchMainlineContributionAuditR922.recentDetailSnapshotCP4(report, store,12));
        b.append("\n晋级合同=Future OOS N>=240、配对>=120、Wilson/FDR/ECE/Brier/覆盖/块/Regime/连挂全部通过后才制证；未制证贡献严格为0。\n");
        b.append("动态迭代=失败向量→结构生成/变异→赛前冻结→exact draw逐期结算→状态更新→证据审查→保留/淘汰/制证；开发回放不计Future OOS。\n");
        b.append("REAL边界=创生资产最多8%限幅、算法家族最多30%限幅；二者都不能创建BET，最终仍由DecisionGateEngine唯一授权。");
        return b.toString();
    }

    // CP12 source-bound immutable-snapshot overload: appendContributions
static void appendContributionsSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, com.ethk3.novaonepro.ReportTextCP12 b,SQLiteDatabase db,int limit){
        b.append("\n【证书化主链贡献】\n");Cursor c=null;int n=0;try{
            c=report.select("SELECT target_issue,display_name_zh,influence_weight,top_evaluated,status,evidence_hash FROM "
                    +CertifiedResearchMixerR915.TABLE_CONTRIBUTION+" ORDER BY created_ms DESC LIMIT ?",
                    new String[]{String.valueOf(Math.max(1,Math.min(20,limit)))});
            while(c.moveToNext()){n++;b.append(n).append("｜期=").append(explicitSnapshotCP4(report, c.getString(0),"N/A"))
                    .append("｜").append(explicitSnapshotCP4(report, c.getString(1),"中文机制名缺失"))
                    .append("｜影响=").append(percentSnapshotCP4(report, c.getDouble(2)))
                    .append("｜评估动作=").append(c.getInt(3))
                    .append("｜").append(explicitSnapshotCP4(report, c.getString(4),"UNKNOWN"))
                    .append("｜证据=").append(shortHashSnapshotCP4(report, c.getString(5))).append('\n');}
        }catch(Throwable e){throw new com.ethk3.novaonepro.ReportReadContextCP4.ReportReadFailureCP4("REPORT_QUERY_OR_FORMAT_FAILED",e);}
        finally{if(c!=null)c.close();}if(n==0)b.append("NO_SAMPLE:NO_CERTIFIED_CONTRIBUTION_PRODUCTION_FALLBACK_100_PERCENT\n");
    }

    // CP12 source-bound immutable-snapshot overload: shortHash
public static String shortHashSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, String s){String x=safeSnapshotCP4(report, s);return x.length()==0?"N/A":(x.length()<=12?x:x.substring(0,12));}

    // CP12 source-bound immutable-snapshot overload: appendAtomProposals
static void appendAtomProposalsSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, com.ethk3.novaonepro.ReportTextCP12 b,SQLiteDatabase db,int limit){
        b.append("\n【失败驱动的新原子提案】\n");Cursor c=null;int n=0;try{
            c=report.select("SELECT proposed_name,status,reject_reason,consumer_family,source_atom_ids,definition_spec,created_issue FROM "
                    +AssetGenesisEngineR915.TABLE_ATOM_PROPOSAL+" ORDER BY created_ms DESC LIMIT ?",
                    new String[]{String.valueOf(Math.max(1,Math.min(20,limit)))});
            while(c.moveToNext()){n++;b.append(n).append("｜").append(explicitSnapshotCP4(report, c.getString(0),"UNNAMED_ATOM"))
                    .append("｜").append(explicitSnapshotCP4(report, c.getString(1),"UNKNOWN"))
                    .append("｜拒绝=").append(explicitSnapshotCP4(report, c.getString(2),"NONE"))
                    .append("｜消费家族=").append(explicitSnapshotCP4(report, c.getString(3),"UNKNOWN"))
                    .append("｜来源=").append(clipSnapshotCP4(report, explicitSnapshotCP4(report, c.getString(4),"NONE"),240))
                    .append("｜定义=").append(clipSnapshotCP4(report, explicitSnapshotCP4(report, c.getString(5),"MISSING"),520))
                    .append("｜创建期=").append(explicitSnapshotCP4(report, c.getString(6),"N/A")).append('\n');}
        }catch(Throwable e){throw new com.ethk3.novaonepro.ReportReadContextCP4.ReportReadFailureCP4("REPORT_QUERY_OR_FORMAT_FAILED",e);}
        finally{if(c!=null)c.close();}if(n==0)b.append("NO_SAMPLE:NO_FAILURE_DRIVEN_ATOM_PROPOSAL\n");
    }

    // CP12 source-bound immutable-snapshot overload: appendArchivedAlgorithms
static void appendArchivedAlgorithmsSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, com.ethk3.novaonepro.ReportTextCP12 b,SQLiteDatabase db,int limit){
        b.append("\n【算法证据归档榜（淘汰不删除）】\n");Cursor c=null;int n=0;try{
            c=report.select("SELECT display_name_zh,status,future_n,future_hit,future_hit_rate,future_wilson_lower,"
                    +"future_brier,future_ece,future_coverage,future_block_min,future_regime_min,future_max_miss,"
                    +"paired_mainline_n,paired_mainline_delta,future_fdr_q,lifecycle_reason,mechanism_spec_zh,"
                    +"mutation_intent,creation_reason,structural_hash,behavior_hash,updated_at FROM "+DbHelper.TABLE_R908_ALGORITHM_GENOME
                    +" WHERE status LIKE 'PRUNED%' OR status LIKE 'REJECTED%' OR status LIKE 'REVOKED%'"
                    +" ORDER BY future_wilson_lower DESC,future_n DESC,updated_at DESC LIMIT ?",
                    new String[]{String.valueOf(Math.max(1,Math.min(20,limit)))});
            while(c.moveToNext()){n++;b.append(n).append("｜").append(explicitSnapshotCP4(report, c.getString(0),"中文机制名缺失"))
                    .append("｜").append(explicitSnapshotCP4(report, c.getString(1),"UNKNOWN"))
                    .append("｜N/H=").append(c.getInt(2)).append('/').append(c.getInt(3))
                    .append("｜HR=").append(percentSnapshotCP4(report, c.getDouble(4))).append("｜Wilson=").append(percentSnapshotCP4(report, c.getDouble(5)))
                    .append("｜Brier/ECE=").append(numberSnapshotCP4(report, c.getDouble(6))).append('/').append(numberSnapshotCP4(report, c.getDouble(7)))
                    .append("｜Coverage=").append(percentSnapshotCP4(report, c.getDouble(8)))
                    .append("｜Block/Regime=").append(percentSnapshotCP4(report, c.getDouble(9))).append('/').append(percentSnapshotCP4(report, c.getDouble(10)))
                    .append("｜MaxMiss=").append(c.getInt(11))
                    .append("｜Paired=").append(c.getInt(12)).append(':').append(percentSignedSnapshotCP4(report, c.getDouble(13)))
                    .append("｜FDR=").append(percentSnapshotCP4(report, c.getDouble(14)))
                    .append("\n  ARCHIVE_WHY=").append(explicitSnapshotCP4(report, c.getString(15),"UNKNOWN"))
                    .append("\n  机制=").append(clipSnapshotCP4(report, explicitSnapshotCP4(report, c.getString(16),"MECHANISM_MISSING"),900))
                    .append("\n  结构意图=").append(explicitSnapshotCP4(report, c.getString(17),"OPEN_EXPLORATION"))
                    .append("｜创造原因=").append(clipSnapshotCP4(report, explicitSnapshotCP4(report, c.getString(18),"NO_FAILURE_VECTOR"),700))
                    .append("\n  结构/行为=").append(shortHashSnapshotCP4(report, c.getString(19))).append('/').append(shortHashSnapshotCP4(report, c.getString(20)))
                    .append("｜updatedAt=").append(c.getLong(21)).append('\n');}
        }catch(Throwable e){throw new com.ethk3.novaonepro.ReportReadContextCP4.ReportReadFailureCP4("REPORT_QUERY_OR_FORMAT_FAILED",e);}
        finally{if(c!=null)c.close();}if(n==0)b.append("NO_SAMPLE:NO_ARCHIVED_ALGORITHM_FAMILY\n");
        b.append("归档语义=状态迁移保留完整Future-OOS证据，不作为活跃候选继续赛前冻结；"
                +"界面榜单分栏，避免高样本算法因淘汰状态看似‘消失’。\n");
    }

    // CP12 source-bound immutable-snapshot overload: percentSigned
public static String percentSignedSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, double v){return Double.isNaN(v)||Double.isInfinite(v)?"N/A":String.format(Locale.US,"%+.2f%%",v*100d);}

    // CP12 source-bound immutable-snapshot overload: number
public static String numberSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, double v){return Double.isNaN(v)||Double.isInfinite(v)?"N/A":String.format(Locale.US,"%.5f",v);}

    // CP12 source-bound immutable-snapshot overload: appendAlgorithms
static void appendAlgorithmsSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, com.ethk3.novaonepro.ReportTextCP12 b,SQLiteDatabase db,int limit){
        b.append("\n【活跃算法家族Future OOS榜】\n");Cursor c=null;int n=0;try{
            c=report.select("SELECT display_name_zh,status,future_n,future_hit,future_hit_rate,future_wilson_lower,"
                    +"future_brier,future_ece,future_coverage,future_block_min,future_regime_min,future_max_miss,"
                    +"paired_mainline_n,paired_mainline_delta,future_fdr_q,lifecycle_reason,mechanism_spec_zh,"
                    +"mutation_intent,creation_reason,structural_hash,behavior_hash,updated_at FROM "+DbHelper.TABLE_R908_ALGORITHM_GENOME
                    +" WHERE status NOT LIKE 'PRUNED%' AND status NOT LIKE 'REJECTED%'"
                    +" AND status NOT LIKE 'REVOKED%' AND status<>'PAUSED_CAPACITY_R912'"
                    +" ORDER BY CASE status WHEN 'CHALLENGER_FUTURE_OOS' THEN 0 WHEN 'OBSERVING_FUTURE_OOS' THEN 1 "
                    +"WHEN 'IDEA_WAIT_FUTURE_OOS' THEN 2 ELSE 3 END,future_wilson_lower DESC,future_n DESC LIMIT ?",
                    new String[]{String.valueOf(Math.max(1,Math.min(20,limit)))});
            while(c.moveToNext()){n++;b.append(n).append("｜").append(explicitSnapshotCP4(report, c.getString(0),"中文机制名缺失"))
                    .append("｜").append(explicitSnapshotCP4(report, c.getString(1),"UNKNOWN"))
                    .append("｜N/H=").append(c.getInt(2)).append('/').append(c.getInt(3))
                    .append("｜HR=").append(percentSnapshotCP4(report, c.getDouble(4))).append("｜Wilson=").append(percentSnapshotCP4(report, c.getDouble(5)))
                    .append("｜Brier/ECE=").append(numberSnapshotCP4(report, c.getDouble(6))).append('/').append(numberSnapshotCP4(report, c.getDouble(7)))
                    .append("｜Coverage=").append(percentSnapshotCP4(report, c.getDouble(8)))
                    .append("｜Block/Regime=").append(percentSnapshotCP4(report, c.getDouble(9))).append('/').append(percentSnapshotCP4(report, c.getDouble(10)))
                    .append("｜MaxMiss=").append(c.getInt(11))
                    .append("｜Paired=").append(c.getInt(12)).append(':').append(percentSignedSnapshotCP4(report, c.getDouble(13)))
                    .append("｜FDR=").append(percentSnapshotCP4(report, c.getDouble(14)))
                    .append("\n  WHY=").append(explicitSnapshotCP4(report, c.getString(15),"WAIT_EVIDENCE"))
                    .append("\n  机制=").append(clipSnapshotCP4(report, explicitSnapshotCP4(report, c.getString(16),"MECHANISM_MISSING"),900))
                    .append("\n  结构意图=").append(explicitSnapshotCP4(report, c.getString(17),"OPEN_EXPLORATION"))
                    .append("｜创造原因=").append(clipSnapshotCP4(report, explicitSnapshotCP4(report, c.getString(18),"NO_FAILURE_VECTOR"),700))
                    .append("\n  结构/行为=").append(shortHashSnapshotCP4(report, c.getString(19))).append('/').append(shortHashSnapshotCP4(report, c.getString(20)))
                    .append("｜updatedAt=").append(c.getLong(21)).append('\n');}
        }catch(Throwable e){throw new com.ethk3.novaonepro.ReportReadContextCP4.ReportReadFailureCP4("REPORT_QUERY_OR_FORMAT_FAILED",e);}
        finally{if(c!=null)c.close();}if(n==0)b.append("NO_SAMPLE:NO_ACTIVE_ALGORITHM_FAMILY\n");
    }

    // CP12 source-bound immutable-snapshot overload: appendGenesisAssets
static void appendGenesisAssetsSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, com.ethk3.novaonepro.ReportTextCP12 b,SQLiteDatabase db,int limit){
        b.append("\n【GENESIS_NEW创生资产榜】\n");Cursor c=null;int n=0;try{
            c=report.select("SELECT display_name_zh,family_id,status,future_n,future_hit,future_hit_rate,"
                    +"future_wilson_lower,future_brier,future_ece,future_coverage,future_block_min,"
                    +"future_regime_min,future_max_miss,paired_mainline_n,paired_mainline_delta,fdr_q,"
                    +"lifecycle_reason,mechanism_spec_zh,structural_hash,behavior_hash,updated_ms FROM "
                    +AssetGenesisEngineR915.TABLE_ASSET_GENOME
                    +" ORDER BY CASE status WHEN 'CERTIFIED_FUTURE_OOS' THEN 0 WHEN 'OBSERVING_FUTURE_OOS' THEN 1 "
                    +"WHEN 'IDEA_WAIT_FUTURE_OOS' THEN 2 ELSE 3 END,future_wilson_lower DESC,future_n DESC LIMIT ?",
                    new String[]{String.valueOf(Math.max(1,Math.min(20,limit)))});
            while(c.moveToNext()){n++;b.append(n).append("｜").append(explicitSnapshotCP4(report, c.getString(0),"中文机制名缺失"))
                    .append("｜家族=").append(explicitSnapshotCP4(report, c.getString(1),"UNKNOWN"))
                    .append("｜").append(explicitSnapshotCP4(report, c.getString(2),"UNKNOWN"))
                    .append("｜N/H=").append(c.getInt(3)).append('/').append(c.getInt(4))
                    .append("｜HR=").append(percentSnapshotCP4(report, c.getDouble(5)))
                    .append("｜Wilson=").append(percentSnapshotCP4(report, c.getDouble(6)))
                    .append("｜Brier/ECE=").append(numberSnapshotCP4(report, c.getDouble(7))).append('/').append(numberSnapshotCP4(report, c.getDouble(8)))
                    .append("｜Coverage=").append(percentSnapshotCP4(report, c.getDouble(9)))
                    .append("｜Block/Regime=").append(percentSnapshotCP4(report, c.getDouble(10))).append('/').append(percentSnapshotCP4(report, c.getDouble(11)))
                    .append("｜MaxMiss=").append(c.getInt(12))
                    .append("｜Paired=").append(c.getInt(13)).append(':').append(percentSignedSnapshotCP4(report, c.getDouble(14)))
                    .append("｜FDR=").append(percentSnapshotCP4(report, c.getDouble(15)))
                    .append("\n  WHY=").append(explicitSnapshotCP4(report, c.getString(16),"WAIT_EVIDENCE"))
                    .append("\n  机制=").append(clipSnapshotCP4(report, explicitSnapshotCP4(report, c.getString(17),"MECHANISM_MISSING"),900))
                    .append("\n  结构/行为=").append(shortHashSnapshotCP4(report, c.getString(18))).append('/').append(shortHashSnapshotCP4(report, c.getString(19)))
                    .append("｜updatedAt=").append(c.getLong(20)).append('\n');}
        }catch(Throwable e){throw new com.ethk3.novaonepro.ReportReadContextCP4.ReportReadFailureCP4("REPORT_QUERY_OR_FORMAT_FAILED",e);}
        finally{if(c!=null)c.close();}if(n==0)b.append("NO_SAMPLE:NO_REGISTERED_GENESIS_ASSET\n");
    }

    // CP12 source-bound immutable-snapshot overload: appendAtomRegistry
static void appendAtomRegistrySnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, com.ethk3.novaonepro.ReportTextCP12 b){
        b.append("\n【240项底层因子/原子完整登记】\n");
        int group=-1;
        for(GenesisSchemaRegistryR915.AtomSpec atom:GenesisSchemaRegistryR915.atoms()){
            int next=parseAtomGroupSnapshotCP4(report, atom.group);
            if(next!=group){group=next;b.append("\n[").append(atom.group).append("] ")
                    .append(atomGroupNameSnapshotCP4(report, group)).append('\n');}
            b.append(atom.id).append("｜").append(atom.name)
                    .append("｜semanticTier=").append(atom.semanticTier())
                    .append("｜type=").append(atom.valueType)
                    .append("｜range=").append(atom.valueRange)
                    .append("｜window=").append(atom.windowSpec)
                    .append("｜lag<=").append(atom.maxLag)
                    .append("｜support>=").append(atom.minSupport)
                    .append("｜stage=").append(atom.evidenceStage)
                    .append("｜leakage=").append(atom.leakagePolicy)
                    .append("｜update=").append(atom.updateTiming)
                    .append("｜cost=").append(atom.costClass)
                    .append("｜v").append(atom.version).append('\n');
        }
    }

    // CP12 source-bound immutable-snapshot overload: atomGroupName
public static String atomGroupNameSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, int group){
        String[] names={"时点与数据真值","排列/位置/号码身份","时间与生命周期","关系图与迁移",
                "数值变换与交互","统计与信息","可学习状态与反馈","不确定性/风险/反证",
                "1200动作几何","Future-OOS开奖后证据","资源与后台健康","进化互补与信用分配"};
        return group>=0&&group<names.length?names[group]:"未知组";
    }

    // CP12 source-bound immutable-snapshot overload: parseAtomGroup
public static int parseAtomGroupSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, String group){
        try{return Integer.parseInt(safeSnapshotCP4(report, group).substring(1));}catch(Throwable ignored){throw new com.ethk3.novaonepro.ReportReadContextCP4.ReportReadFailureCP4("REPORT_QUERY_OR_FORMAT_FAILED",ignored);}
    }

    // CP12 source-bound immutable-snapshot overload: shadowDetail
public static String shadowDetailSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, SQLiteDatabase db,DiagnosticSnapshotR811 d,String summary){
        com.ethk3.novaonepro.ReportTextCP12 b=new com.ethk3.novaonepro.ReportTextCP12(report, 3072);b.append("【R923严格Future OOS统计】\n").append(summary).append('\n');
        appendWindowSnapshotCP4(report, b,"最近30",d.recent30);appendWindowSnapshotCP4(report, b,"最近60",d.recent60);appendWindowSnapshotCP4(report, b,"最近120",d.recent120);
        b.append("严格OOS=").append(intScalarSnapshotCP4(report, db,"SELECT COUNT(1) FROM "+DbHelper.TABLE_R35_OOS_LABEL+" WHERE strict_oos=1",null,0))
                .append("｜合法选择后概率=").append(intScalarSnapshotCP4(report, db,"SELECT COUNT(1) FROM "+DbHelper.TABLE_R35_OOS_LABEL+" WHERE strict_oos=1 AND selected_hit_p BETWEEN 0 AND 1",null,0)).append('\n');
        b.append("命中率公式=").append(d.shadowHit).append("÷(").append(d.shadowHit).append('+')
                .append(d.shadowMiss).append(")；命中÷未中是命中未中比，不是命中率。\n");
        b.append("口径=赛前冻结canonical+exact draw；预测结算完成率不等于实际开奖预测覆盖率；"
                +"最近窗口按目标期号排序，回补旧期不会冒充最近一期；开奖后禁止重算；小样本不得显示为模型证明。");return b.toString();
    }

    // CP12 source-bound immutable-snapshot overload: intScalar
public static int intScalarSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, SQLiteDatabase db,String sql,String[] args,int d){Cursor c=null;try{c=report.select(sql,args);return c.moveToFirst()&&!c.isNull(0)?c.getInt(0):d;}catch(Throwable e){throw new com.ethk3.novaonepro.ReportReadContextCP4.ReportReadFailureCP4("REPORT_QUERY_OR_FORMAT_FAILED",e);}finally{if(c!=null)c.close();}}

    // CP12 source-bound immutable-snapshot overload: appendWindow
public static void appendWindowSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, com.ethk3.novaonepro.ReportTextCP12 b,String name,DiagnosticSnapshotR811.WindowStats w){
        b.append(name).append("：N=").append(w==null?0:w.sample);
        if(w==null||!w.available()){b.append("｜HR/Wilson/Brier=N/A\n");return;}
        b.append("｜Hit=").append(w.hit).append("｜HR=").append(percentSnapshotCP4(report, w.hitRate))
                .append("｜Wilson下界=").append(percentSnapshotCP4(report, w.wilsonLower)).append("｜Brier=").append(numberSnapshotCP4(report, w.brier)).append('\n');
    }

    // CP12 source-bound immutable-snapshot overload: pipelineDetail
public static String pipelineDetailSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, com.ethk3.novaonepro.ReportReadContextCP4 store,SQLiteDatabase db,DiagnosticSnapshotR811 d,
            SettlementCoverageTruthR906 cov,String summary){
        com.ethk3.novaonepro.ReportTextCP12 b=new com.ethk3.novaonepro.ReportTextCP12(report, 6144);b.append("【R923运行闭环唯一真值】\n").append(summary).append('\n');
        b.append("目标期=").append(explicitSnapshotCP4(report, d.targetIssue,"N/A")).append("｜CORE=").append(explicitSnapshotCP4(report, d.coreStatus,"N/A"))
                .append("｜FULL=").append(explicitSnapshotCP4(report, d.fullStatus,"N/A")).append('\n');
        b.append("结算任务分布=").append(taskDistributionSnapshotCP4(report, db)).append('\n');
        b.append("结算Owner=SettlementEngine_ONLY｜预测Owner=AnalysisSingleFlightR36_ONLY｜Research失败不得回滚核心结算。\n");
        b.append("后台系统能力=状态=").append(explicitSnapshotCP4(report, store.getState("r919_background_capability_status",""),"NOT_OBSERVED"))
                .append("｜电池无限制=").append("1".equals(store.getState("r919_background_battery_unrestricted","0"))?"YES":"NO")
                .append("｜精确闹钟=").append("1".equals(store.getState("r919_background_exact_alarm_granted","0"))?"YES":"NO")
                .append("｜权限=").append(explicitSnapshotCP4(report, store.getState("r919_background_permission_summary",""),"N/A"))
                .append("｜观察时间=").append(explicitSnapshotCP4(report, store.getState("r919_background_capability_at",""),"N/A"))
                .append("；任一系统授权缺失时界面按钮必须引导用户补齐，不能伪称已验证后台。\n");
        b.append("延后追赶=").append(explicitSnapshotCP4(report, store.getState("r902_deferred_drain_status",""),"NO_DRAIN_RECORDED"))
                .append("｜紧急追赶=").append(explicitSnapshotCP4(report, store.getState("r902_deferred_urgent_status",""),"NO_URGENT_RECORDED"))
                .append("｜研究结算=").append(explicitSnapshotCP4(report, store.getState("r902_research_settlement_status",""),"NO_RESEARCH_SETTLEMENT_RECORDED")).append('\n');
        b.append("延后队列收敛=").append(DeferredEvidenceWriterR36.backlogStatsSnapshotCP4(report, store).summary()).append('\n');
        b.append("后台自恢复=loops=").append(explicitSnapshotCP4(report, store.getState("r908_background_loops",""),"NOT_ARMED"))
                .append("｜taskRemoved=").append(explicitSnapshotCP4(report, store.getState("r922_task_removed_reconcile",""),"NOT_OBSERVED"))
                .append("｜research=").append(explicitSnapshotCP4(report, store.getState("r908_research_tick_status",""),"NO_PULSE"))
                .append("｜settlement=").append(explicitSnapshotCP4(report, store.getState("r908_settlement_loop_status",""),"NO_PULSE"))
                .append("｜限制=Android强制停止/OEM电池限制不可绕过\n");
        b.append("最近12个冻结预测：\n");Cursor c=null;int n=0;try{
            c=report.select("SELECT s.target_issue,s.position,s.n1,s.n2,s.n3,s.settled,s.actual_number,s.hit_label,COALESCE(t.status,'NO_TASK') FROM "
                    +DbHelper.TABLE_R35_SHADOW_OBSERVATION+" s LEFT JOIN "+DbHelper.TABLE_R36_SETTLEMENT_TASK
                    +" t ON t.target_issue=s.target_issue ORDER BY s.target_issue DESC LIMIT 12",null);
            while(c.moveToNext()){n++;b.append(n).append("｜期=").append(c.getString(0)).append("｜P").append(c.getInt(1))
                    .append("｜K3=").append(twoSnapshotCP4(report, c.getInt(2))).append('/').append(twoSnapshotCP4(report, c.getInt(3))).append('/').append(twoSnapshotCP4(report, c.getInt(4)))
                    .append("｜settled=").append(c.getInt(5)).append("｜actual=").append(c.getInt(6))
                    .append("｜hit=").append(c.getInt(7)).append("｜task=").append(explicitSnapshotCP4(report, c.getString(8),"NO_TASK")).append('\n');}
        }catch(Throwable e){throw new com.ethk3.novaonepro.ReportReadContextCP4.ReportReadFailureCP4("REPORT_QUERY_OR_FORMAT_FAILED",e);}
        finally{if(c!=null)c.close();}if(n==0)b.append("NO_SAMPLE:NO_FROZEN_PREDICTION\n");return b.toString();
    }

    // CP12 source-bound immutable-snapshot overload: taskDistribution
public static String taskDistributionSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, SQLiteDatabase db){
        Cursor c=null;com.ethk3.novaonepro.ReportTextCP12 b=new com.ethk3.novaonepro.ReportTextCP12(report);try{c=report.select("SELECT status,task_class,COUNT(1) FROM "
                +DbHelper.TABLE_R36_SETTLEMENT_TASK+" GROUP BY status,task_class ORDER BY status,task_class",null);
            while(c.moveToNext()){if(b.length()>0)b.append(';');b.append(explicitSnapshotCP4(report, c.getString(0),"UNKNOWN"))
                    .append('/').append(explicitSnapshotCP4(report, c.getString(1),"UNKNOWN")).append('=').append(c.getInt(2));}
        }catch(Throwable e){throw new com.ethk3.novaonepro.ReportReadContextCP4.ReportReadFailureCP4("REPORT_QUERY_OR_FORMAT_FAILED",e);}finally{if(c!=null)c.close();}
        return b.length()==0?"NO_SAMPLE:NO_SETTLEMENT_TASK":b.toString();
    }
public static RuntimeTruthSnapshotR906 captureSummarySnapshotCP4(ReportReadContextCP4 report, ReportReadContextCP4 store){return captureSnapshotCP4(report, store,"",false);}

public static RuntimeTruthSnapshotR906 errorSnapshotCP4(ReportReadContextCP4 report, String reason){
        long now=report.nowMs(); String r=explicitSnapshotCP4(report, reason,"READ_ERROR");
        TruthCard[] cards=new TruthCard[]{
            new TruthCard("WHY_HIT","为什么中","暂无样本｜原因="+r,"","R906_TRUTH","READ_ERROR"),
            new TruthCard("WHY_MISS","为什么不中","暂无样本｜原因="+r,"","R906_TRUTH","READ_ERROR"),
            new TruthCard("WHY_NO_BET","为什么不出手","SHADOW｜原因="+r,"","DecisionGateEngine","READ_ERROR"),
            new TruthCard("SETTLEMENT","逐期结算统计","不可用｜原因="+r,"","SettlementTaskStoreR36+OosAfterDrawWriterR36","READ_ERROR")};
        StateBox[] boxes=new StateBox[16];
        String[] titles={"目标期冻结","开奖真值","结算任务","命中真值","原始K3评分（未校准）","动作Rank","第三边界","动作稳定性","号码风险","形态状态","窗口共识","跨位支持","选择后校准","深度学习","算法发明迭代","DecisionGate"};
        for(int i=0;i<boxes.length;i++)boxes[i]=new StateBox(i+1,"STATE_"+(i+1),titles[i],"不可用｜原因="+r,"R906_TRUTH","","READ_ERROR");
        String hash=com.ethk3.novaonepro.ReportTextCP12.sha256(report, "R906_ERROR|"+r+'|'+now);
        String failure="READ_ERROR:"+r;
        return new RuntimeTruthSnapshotR906("R922-READ-ERROR-"+shortHashSnapshotCP4(report, hash),hash,"READ_ERROR",r,
                now,0L,"","","","","",cards,boxes,new SettlementCoverageTruthR906(0,0,0,0,0),
                "运行闭环｜"+failure,failure,"影子结算｜"+failure,failure,
                "Research｜"+failure,failure,"真实出手硬门｜"+failure,failure,null);
    }

public static StateBox[] buildStateBoxesSnapshotCP4(ReportReadContextCP4 report, ReportReadContextCP4 store,SQLiteDatabase db,String target,String draw,String canonical,String closure,String oos,SettlementCoverageTruthR906 cov){
        StateBox[] out=new StateBox[16];
        Evidence e=evidenceSnapshotCP4(report, db,target);
        String frozen="target="+explicitSnapshotCP4(report, target,"N/A")+"｜frozenAt="+(e.frozenAt>0?e.frozenAt:"N/A")+"｜hash="+shortHashSnapshotCP4(report, e.recordHash);
        String drawHash=strScalarSnapshotCP4(report, db,"SELECT draw_hash_v2 FROM "+DbHelper.TABLE_DRAWS+" WHERE issue=? LIMIT 1",new String[]{draw},"");
        out[0]=boxSnapshotCP4(report, 1,"目标期冻结",frozen,"selected_evidence",target,e.recordHash.length()>0?"AVAILABLE":"UNAVAILABLE");
        out[1]=boxSnapshotCP4(report, 2,"开奖真值","issue="+explicitSnapshotCP4(report, draw,"N/A")+"｜hash="+shortHashSnapshotCP4(report, drawHash)+"｜source=draws","draws",draw,draw.length()>0?"AVAILABLE":"UNAVAILABLE");
        String core=strScalarSnapshotCP4(report, db,"SELECT status FROM "+DbHelper.TABLE_R36_SETTLEMENT_TASK+" WHERE target_issue=? LIMIT 1",new String[]{target},"");
        String err=strScalarSnapshotCP4(report, db,"SELECT COALESCE(terminal_code,'')||CASE WHEN COALESCE(last_error,'')<>'' THEN ':'||last_error ELSE '' END FROM "+DbHelper.TABLE_R36_SETTLEMENT_TASK+" WHERE target_issue=? LIMIT 1",new String[]{target},"");
        out[2]=boxSnapshotCP4(report, 3,"结算任务","当前期="+explicitSnapshotCP4(report, core,"WAITING")+"｜reason="+explicitSnapshotCP4(report, err,"NONE")
                +"｜开奖任务终态="+cov.drawTaskTerminal+"/"+cov.drawTaskTotal
                +"｜预测结算="+cov.settledPredictions+"/"+cov.predictedEligible,
                "SettlementTaskStoreR36",target,core.length()>0?"AVAILABLE":"UNAVAILABLE");
        TruthRow exact=settledForSnapshotCP4(report, db,target);
        out[3]=boxSnapshotCP4(report, 4,"命中真值",exact==null?"不可用｜原因=NOT_SETTLED":"actual="+twoSnapshotCP4(report, exact.actual)+"｜K3="+k3SnapshotCP4(report, exact)+"｜hit="+(exact.hit==1?"HIT":"MISS"),"OosAfterDrawWriterR36",target,exact==null?"UNAVAILABLE":"AVAILABLE");
        out[4]=boxSnapshotCP4(report, 5,"原始K3评分（未校准）",metricSnapshotCP4(report, e.raw,"SCORE")
                +"｜有效校准样本不足时禁止解释为真实命中概率",
                "selected_evidence.raw_action_p",target,e.rawStatus);
        out[5]=boxSnapshotCP4(report, 6,"动作Rank",e.rankNo>0?(e.rankNo+"/1200｜score="+numberSnapshotCP4(report, e.rankScore)):"不可用｜原因=RANK_NOT_FROZEN","selected_evidence.rank",target,e.rankNo>0?"AVAILABLE":"UNAVAILABLE");
        out[6]=boxSnapshotCP4(report, 7,"第三边界",metricSnapshotCP4(report, e.third,"SCORE"),"selected_evidence.third_margin",target,e.thirdStatus);
        out[7]=boxSnapshotCP4(report, 8,"动作稳定性",metricSnapshotCP4(report, e.stability,"PROBABILITY"),"selected_evidence.stability",target,e.stabilityStatus);
        out[8]=boxSnapshotCP4(report, 9,"号码风险",metricSnapshotCP4(report, e.entropy,"PROBABILITY")+"｜口径=归一化熵风险","selected_evidence.entropy_risk",target,e.entropyStatus);
        String shape=shapeTruthSnapshotCP4(report, db,target);
        out[9]=boxSnapshotCP4(report, 10,"形态状态",shape,"v51_selected_action.shape_*",target,shape.indexOf("不可用")<0?"AVAILABLE":"UNAVAILABLE");
        out[10]=boxSnapshotCP4(report, 11,"窗口共识",metricSnapshotCP4(report, e.window,"PROBABILITY"),"selected_evidence.window_consensus",target,e.windowStatus);
        out[11]=boxSnapshotCP4(report, 12,"跨位支持",metricSnapshotCP4(report, e.assignment,"PROBABILITY")+"｜仅作特征，不改变单位置玩法","selected_evidence.assignment_support",target,e.assignmentStatus);
        String selectedP=strScalarSnapshotCP4(report, db,"SELECT selected_hit_p FROM "+DbHelper.TABLE_R35_SHADOW_OBSERVATION+" WHERE target_issue=? LIMIT 1",new String[]{target},"");
        String ece=store.getState("strict_oos_ece",""),brier=store.getState("strict_oos_brier","");
        out[12]=boxSnapshotCP4(report, 13,"选择后校准",selectedP.length()==0?"不可用｜原因=SELECTED_P_MISSING":"SelectedHitP="+selectedP+"｜ECE="+explicitSnapshotCP4(report, ece,"N/A")+"｜Brier="+explicitSnapshotCP4(report, brier,"N/A"),"selected_oos",target,selectedP.length()>0?"AVAILABLE":"UNAVAILABLE");
        String ftrlUpdates=firstStateSnapshotCP4(report, store,"r811_ftrl_updates","r817_research_adaptive_updates","0");
        String ftrlUntil=firstStateSnapshotCP4(report, store,"r811_ftrl_trained_until","r817_research_adaptive_trained_until","");
        String deepUpdates=store.getState("r909_tiny_deep_updates","0");
        String deepUntil=store.getState("r909_tiny_deep_trained_until","");
        String continuity=store.getState("r912_research_continuity_status","");
        String researchTick=store.getState("r908_research_tick_status","");
        out[13]=boxSnapshotCP4(report, 14,"深度学习","FTRL更新="+explicitSnapshotCP4(report, ftrlUpdates,"0")
                +"｜until="+explicitSnapshotCP4(report, ftrlUntil,"N/A")
                +"｜TinyDeep更新="+explicitSnapshotCP4(report, deepUpdates,"0")
                +"｜until="+explicitSnapshotCP4(report, deepUntil,"N/A")
                +"｜连续学习="+clipSnapshotCP4(report, explicitSnapshotCP4(report, continuity,"WAITING"),120)
                +"｜研究脉冲="+clipSnapshotCP4(report, explicitSnapshotCP4(report, researchTick,"WAITING"),120),
                "ResearchLearningContinuityR912",target,"AVAILABLE");
        int genomes=intScalarSnapshotCP4(report, db,"SELECT COUNT(1) FROM "+DbHelper.TABLE_R908_ALGORITHM_GENOME,null,0);
        int future=intScalarSnapshotCP4(report, db,"SELECT COALESCE(SUM(future_n),0) FROM "+DbHelper.TABLE_R908_ALGORITHM_GENOME,null,0);
        int archived=intScalarSnapshotCP4(report, db,"SELECT COUNT(1) FROM "+DbHelper.TABLE_R908_ALGORITHM_GENOME
                +" WHERE status LIKE 'PRUNED%' OR status LIKE 'REJECTED%' OR status LIKE 'REVOKED%'",null,0);
        int paused=intScalarSnapshotCP4(report, db,"SELECT COUNT(1) FROM "+DbHelper.TABLE_R908_ALGORITHM_GENOME
                +" WHERE status='PAUSED_CAPACITY_R912'",null,0);
        int active=Math.max(0,genomes-archived-paused);
        int assets=intScalarSnapshotCP4(report, db,"SELECT COUNT(1) FROM "+AssetGenesisEngineR915.TABLE_ASSET_GENOME,null,0);
        int assetFuture=intScalarSnapshotCP4(report, db,"SELECT COALESCE(SUM(future_n),0) FROM "+AssetGenesisEngineR915.TABLE_ASSET_GENOME,null,0);
        int assetPruned=intScalarSnapshotCP4(report, db,"SELECT COUNT(1) FROM "+AssetGenesisEngineR915.TABLE_ASSET_GENOME+" WHERE status LIKE 'PRUNED%'",null,0);
        out[14]=boxSnapshotCP4(report, 15,"算法发明迭代","创生资产="+assets+"｜资产FutureOOS="+assetFuture+"｜资产淘汰="+assetPruned
                +"｜算法总数="+genomes+"｜活跃="+active+"｜证据档案="+archived+"｜容量暂停="+paused
                +"｜算法FutureOOS="+future+"｜创生最佳="+assetTruthSnapshotCP4(report, db)
                +"｜活跃算法最佳="+algorithmTruthSnapshotCP4(report, db)+"｜归档高证据="+algorithmArchiveTruthSnapshotCP4(report, db),
                "AssetGenesisEngineR915+AlgorithmInventionLabR908+LifecycleR912",target,
                assets+genomes>0?"AVAILABLE":"NO_SAMPLE");
        String action=currentActionSnapshotCP4(report, db,target),reject=currentRejectSnapshotCP4(report, db,target);
        out[15]=boxSnapshotCP4(report, 16,"DecisionGate",explicitSnapshotCP4(report, action,"SHADOW")+"｜reject="+explicitSnapshotCP4(report, reject,V51BuildManifest.MODEL_PROVEN?"NONE":"MODEL_NOT_PROVEN"),"DecisionGateEngine",target,"AVAILABLE");
        return out;
    }

public static StateBox boxSnapshotCP4(ReportReadContextCP4 report, int i,String title,String value,String source,String target,String status){return new StateBox(i,"STATE_"+i,title,value,source,target,status);}

public static String researchSummarySnapshotCP4(ReportReadContextCP4 report, ReportReadContextCP4 store,SQLiteDatabase db){
        int legacyExpected=LegacyFixedAssetRegistryR915.entries().length;
        int legacyRegistered=intScalarSnapshotCP4(report, db,"SELECT COUNT(1) FROM "+AssetGenesisEngineR915.TABLE_LEGACY_REGISTRY,null,0);
        int legacyLocked=intScalarSnapshotCP4(report, db,"SELECT COUNT(1) FROM "+AssetGenesisEngineR915.TABLE_LEGACY_REGISTRY+" WHERE locked=1",null,0);
        int formulaAssets=intScalarSnapshotCP4(report, db,"SELECT COUNT(1) FROM "+DbHelper.TABLE_R906_RESEARCH_ASSET,null,0);
        int formulaExec=intScalarSnapshotCP4(report, db,"SELECT COUNT(1) FROM "+DbHelper.TABLE_R906_RESEARCH_ASSET+" WHERE runtime_executable=1",null,0);
        int formulaFrozen=intScalarSnapshotCP4(report, db,"SELECT COALESCE(SUM(frozen_n),0) FROM "+DbHelper.TABLE_R906_RESEARCH_EVAL_COVERAGE,null,0);
        int formulaSettled=intScalarSnapshotCP4(report, db,"SELECT COALESCE(SUM(settled_n),0) FROM "+DbHelper.TABLE_R906_RESEARCH_EVAL_COVERAGE,null,0);
        int atomProposals=intScalarSnapshotCP4(report, db,"SELECT COUNT(1) FROM "+AssetGenesisEngineR915.TABLE_ATOM_PROPOSAL,null,0);
        int genesis=intScalarSnapshotCP4(report, db,"SELECT COUNT(1) FROM "+AssetGenesisEngineR915.TABLE_ASSET_GENOME,null,0);
        int genesisFrozen=intScalarSnapshotCP4(report, db,"SELECT COUNT(1) FROM "+AssetGenesisEngineR915.TABLE_ASSET_FREEZE,null,0);
        int genesisSettled=intScalarSnapshotCP4(report, db,"SELECT COUNT(1) FROM "+AssetGenesisEngineR915.TABLE_ASSET_FREEZE+" WHERE hit_label IN(0,1)",null,0);
        int genesisEligible=intScalarSnapshotCP4(report, db,"SELECT COUNT(1) FROM "+AssetGenesisEngineR915.TABLE_ASSET_FREEZE
                +" f WHERE EXISTS(SELECT 1 FROM "+DbHelper.TABLE_DRAWS+" d WHERE d.issue=f.target_issue AND d.dirty=0)",null,0);
        int genesisCertified=intScalarSnapshotCP4(report, db,"SELECT COUNT(1) FROM "+AssetGenesisEngineR915.TABLE_ASSET_CERTIFICATE+" WHERE status='ACTIVE'",null,0);
        int genesisPruned=intScalarSnapshotCP4(report, db,"SELECT COUNT(1) FROM "+AssetGenesisEngineR915.TABLE_ASSET_GENOME+" WHERE status LIKE 'PRUNED%' OR status LIKE 'REJECTED%' OR status LIKE 'REVOKED%'",null,0);
        int contributions=intScalarSnapshotCP4(report, db,"SELECT COUNT(1) FROM "+CertifiedResearchMixerR915.TABLE_CONTRIBUTION+" WHERE status='CERTIFIED_NEW_ASSET_INFLUENCE'",null,0);
        int invented=intScalarSnapshotCP4(report, db,"SELECT COUNT(1) FROM "+DbHelper.TABLE_R908_ALGORITHM_GENOME,null,0);
        int algorithmFrozen=intScalarSnapshotCP4(report, db,"SELECT COUNT(1) FROM "+DbHelper.TABLE_R908_ALGORITHM_PREDRAW,null,0);
        int algorithmSettled=intScalarSnapshotCP4(report, db,"SELECT COUNT(1) FROM "+DbHelper.TABLE_R908_ALGORITHM_PREDRAW+" WHERE hit_label IN(0,1)",null,0);
        int algorithmCertified=intScalarSnapshotCP4(report, db,"SELECT COUNT(1) FROM "+DbHelper.TABLE_R908_ALGORITHM_GENOME+" WHERE status='CHALLENGER_FUTURE_OOS'",null,0);
        int algorithmPruned=intScalarSnapshotCP4(report, db,"SELECT COUNT(1) FROM "+DbHelper.TABLE_R908_ALGORITHM_GENOME+" WHERE status LIKE 'PRUNED%' OR status LIKE 'REJECTED%' OR status LIKE 'REVOKED%'",null,0);
        int algorithmPaused=intScalarSnapshotCP4(report, db,"SELECT COUNT(1) FROM "+DbHelper.TABLE_R908_ALGORITHM_GENOME+" WHERE status='PAUSED_CAPACITY_R912'",null,0);
        int algorithmActive=Math.max(0,invented-algorithmPruned-algorithmPaused);
        int algorithmPending=intScalarSnapshotCP4(report, db,"SELECT COUNT(1) FROM "+DbHelper.TABLE_R908_ALGORITHM_PREDRAW+" WHERE hit_label=-1",null,0);
        int genesisPending=intScalarSnapshotCP4(report, db,"SELECT COUNT(1) FROM "+AssetGenesisEngineR915.TABLE_ASSET_FREEZE+" WHERE hit_label=-1",null,0);
        int formulaPending=intScalarSnapshotCP4(report, db,"SELECT COUNT(1) FROM "+DbHelper.TABLE_R817_RESEARCH_PREDRAW+" WHERE hit_label=-1",null,0);
        int feedbackPending=intScalarSnapshotCP4(report, db,"SELECT COUNT(1) FROM "+DbHelper.TABLE_R36_SETTLEMENT_TASK
                +" t WHERE t.status='COMPLETED' AND NOT EXISTS(SELECT 1 FROM "
                +LegacyPredrawContextBridgeR915.POST_TABLE+" p WHERE p.target_issue=t.target_issue)",null,0);
        String ftrl=firstStateSnapshotCP4(report, store,"r811_ftrl_updates","r817_research_adaptive_updates","0");
        String trained=firstStateSnapshotCP4(report, store,"r811_ftrl_trained_until","r817_research_adaptive_trained_until","");
        String deepUpdates=store.getState("r909_tiny_deep_updates","0");
        String generation=explicitSnapshotCP4(report, store.getState("r915_asset_genesis_tick",""),"NO_TICK_RECORDED");
        String bootstrap=explicitSnapshotCP4(report, store.getState("r922_dynamic_bootstrap_truth",""),"NO_R922_BOOTSTRAP_PULSE");
        String capture=explicitSnapshotCP4(report, store.getState("r914_research_predraw_recovery_status",""),
                explicitSnapshotCP4(report, store.getState("r907_research_async_status",""),"NO_PREDRAW_CAPTURE_RECORDED"));
        String algorithmCapture=explicitSnapshotCP4(report, store.getState("r919_algorithm_capture_status",""),
                "NO_ALGORITHM_CAPTURE_ATTEMPT");
        String integrity=explicitSnapshotCP4(report, store.getState("r915_legacy_integrity",""),"WAIT_VERIFY");
        String mixer=explicitSnapshotCP4(report, store.getState("r915_asset_mixer_status",""),"NO_CERTIFIED_NEW_ASSET");
        return com.ethk3.novaonepro.r952.ModernResearchProgressR78.render(report,db)+"\n历史研究档案（以下待结算不是现代训练样本）\nR923创造能力｜Legacy="+legacyRegistered+'/'+legacyExpected+"｜锁定="+legacyLocked
                +"｜Integrity="+integrity
                +"\n因子/原子登记="+GenesisSchemaRegistryR915.atomCount()
                +"｜生产评分语义实装="+GenesisSchemaRegistryR915.productionScoreEligibleCount()
                +"｜研究代理隔离="+GenesisSchemaRegistryR915.researchProxyCount()
                +"｜文本审计身份="+GenesisSchemaRegistryR915.auditIdentityCount()
                +"｜开奖后证据="+GenesisSchemaRegistryR915.settlementEvidenceCount()
                +"｜导航家族="+GenesisSchemaRegistryR915.familyCount()
                +"｜新原子提案="+atomProposals
                +"\n12组原子="+atomGroupAtlasSnapshotCP4(report)
                +"\n创生资产="+genesis+"｜赛前冻结="+genesisFrozen+"｜已结算="+genesisSettled+'/'+genesisEligible
                +"｜证书="+genesisCertified+"｜淘汰="+genesisPruned+"｜主链贡献="+contributions
                +"\n最佳创生="+assetTruthSnapshotCP4(report, db)
                +"\n算法总数="+invented+"｜活跃="+algorithmActive+"｜证据档案="+algorithmPruned
                +"｜容量暂停="+algorithmPaused+"｜赛前冻结="+algorithmFrozen+"｜待结算="+algorithmPending
                +"｜已结算="+algorithmSettled+"｜挑战者="+algorithmCertified
                +"｜活跃最佳="+algorithmTruthSnapshotCP4(report, db)+"｜归档高证据="+algorithmArchiveTruthSnapshotCP4(report, db)
                +"\n公式资产="+formulaAssets+"｜可执行="+formulaExec+"｜冻结/待结算/结算="+formulaFrozen+'/'+formulaPending+'/'+formulaSettled
                +"｜FTRL更新="+explicitSnapshotCP4(report, ftrl,"0")+"@"+explicitSnapshotCP4(report, trained,"N/A")
                +"｜TinyDeep更新="+explicitSnapshotCP4(report, deepUpdates,"0")
                +"\n队列=创生待结算:"+genesisPending+"｜算法待结算:"+algorithmPending
                +"｜公式待结算:"+formulaPending+"｜教师/推理/反推/因果反馈待补:"+feedbackPending
                +"\n本轮创造="+clipSnapshotCP4(report, generation,260)+"｜赛前捕获="+clipSnapshotCP4(report, capture,260)
                +"｜算法捕获="+clipSnapshotCP4(report, algorithmCapture,180)
                +"｜生产混合="+clipSnapshotCP4(report, mixer,160)
                +"\n空实验室启动="+clipSnapshotCP4(report, bootstrap,360);
    }

public static String atomGroupAtlasSnapshotCP4(ReportReadContextCP4 report){
        return "A0时点真值/A1排列身份/A2时间记忆/A3关系图/A4数值变换/A5信息统计/"
                +"A6状态反馈/A7风险不确定/A8动作几何/A9开奖后证据/A10资源健康/A11进化互补";
    }

public static String algorithmTruthSnapshotCP4(ReportReadContextCP4 report, SQLiteDatabase db){Cursor c=null;try{
        c=report.select("SELECT display_name_zh,status,future_n,future_hit_rate,future_wilson_lower,"
                        +"future_brier,future_ece,future_coverage,paired_mainline_n,paired_mainline_delta,"
                        +"future_fdr_q,lifecycle_reason,mutation_intent,creation_reason,mechanism_spec_zh,updated_at FROM "+DbHelper.TABLE_R908_ALGORITHM_GENOME
                        +" WHERE status NOT LIKE 'PRUNED%' AND status NOT LIKE 'REJECTED%'"
                        +" AND status NOT LIKE 'REVOKED%' AND status<>'PAUSED_CAPACITY_R912'"
                        +" ORDER BY CASE status WHEN 'CHALLENGER_FUTURE_OOS' THEN 0 WHEN 'OBSERVING_FUTURE_OOS' THEN 1 "
                        +"WHEN 'IDEA_WAIT_FUTURE_OOS' THEN 2 ELSE 3 END,future_wilson_lower DESC,future_n DESC LIMIT 1",null);
        if(!c.moveToFirst())return "暂无活跃算法；请查看证据归档榜";
        return explicitSnapshotCP4(report, c.getString(0),"未命名")+"｜"+explicitSnapshotCP4(report, c.getString(1),"UNKNOWN")
                +"｜N="+c.getInt(2)+"｜HR="+percentSnapshotCP4(report, c.getDouble(3))+"｜Wilson下界="+percentSnapshotCP4(report, c.getDouble(4))
                +"｜Brier="+numberSnapshotCP4(report, c.getDouble(5))+"｜ECE="+numberSnapshotCP4(report, c.getDouble(6))
                +"｜覆盖="+percentSnapshotCP4(report, c.getDouble(7))+"｜相对主链="+c.getInt(8)+":"+percentSignedSnapshotCP4(report, c.getDouble(9))
                +"｜FDR="+percentSnapshotCP4(report, c.getDouble(10))+"｜生命周期="+explicitSnapshotCP4(report, c.getString(11),"WAITING")
                +"｜结构意图="+explicitSnapshotCP4(report, c.getString(12),"OPEN_EXPLORATION")
                +"｜创造原因="+clipSnapshotCP4(report, explicitSnapshotCP4(report, c.getString(13),"NO_FAILURE_VECTOR"),180)
                +"｜"+clipSnapshotCP4(report, explicitSnapshotCP4(report, c.getString(14),"机制待生成"),220)
                +"｜updatedAt="+c.getLong(15);
    }catch(Throwable e){throw new com.ethk3.novaonepro.ReportReadContextCP4.ReportReadFailureCP4("REPORT_QUERY_OR_FORMAT_FAILED",e);}finally{if(c!=null)c.close();}}

public static String algorithmArchiveTruthSnapshotCP4(ReportReadContextCP4 report, SQLiteDatabase db){Cursor c=null;try{
        c=report.select("SELECT display_name_zh,status,future_n,future_hit_rate,future_wilson_lower,"
                        +"future_brier,future_ece,future_coverage,future_regime_min,lifecycle_reason,updated_at FROM "
                        +DbHelper.TABLE_R908_ALGORITHM_GENOME
                        +" WHERE status LIKE 'PRUNED%' OR status LIKE 'REJECTED%' OR status LIKE 'REVOKED%'"
                        +" ORDER BY future_wilson_lower DESC,future_n DESC,updated_at DESC LIMIT 1",null);
        if(!c.moveToFirst())return "暂无证据归档";
        return explicitSnapshotCP4(report, c.getString(0),"未命名")+"｜"+explicitSnapshotCP4(report, c.getString(1),"UNKNOWN")
                +"｜N="+c.getInt(2)+"｜HR="+percentSnapshotCP4(report, c.getDouble(3))+"｜Wilson="+percentSnapshotCP4(report, c.getDouble(4))
                +"｜Brier/ECE="+numberSnapshotCP4(report, c.getDouble(5))+'/'+numberSnapshotCP4(report, c.getDouble(6))
                +"｜覆盖="+percentSnapshotCP4(report, c.getDouble(7))+"｜Regime="+percentSnapshotCP4(report, c.getDouble(8))
                +"｜归档原因="+explicitSnapshotCP4(report, c.getString(9),"UNKNOWN")+"｜updatedAt="+c.getLong(10);
    }catch(Throwable e){throw new com.ethk3.novaonepro.ReportReadContextCP4.ReportReadFailureCP4("REPORT_QUERY_OR_FORMAT_FAILED",e);}finally{if(c!=null)c.close();}}

public static String assetTruthSnapshotCP4(ReportReadContextCP4 report, SQLiteDatabase db){Cursor c=null;try{
        c=report.select("SELECT display_name_zh,status,future_n,future_hit_rate,future_wilson_lower,"
                        +"future_coverage,paired_mainline_n,paired_mainline_delta,fdr_q,lifecycle_reason,updated_ms "
                        +"FROM "+AssetGenesisEngineR915.TABLE_ASSET_GENOME
                        +" ORDER BY CASE status WHEN 'CERTIFIED_FUTURE_OOS' THEN 0 WHEN 'OBSERVING_FUTURE_OOS' THEN 1 "
                        +"WHEN 'IDEA_WAIT_FUTURE_OOS' THEN 2 ELSE 3 END,future_wilson_lower DESC,future_n DESC LIMIT 1",null);
        if(!c.moveToFirst())return "暂无已注册创生资产";
        return explicitSnapshotCP4(report, c.getString(0),"未命名")+"｜"+explicitSnapshotCP4(report, c.getString(1),"UNKNOWN")
                +"｜N="+c.getInt(2)+"｜HR="+percentSnapshotCP4(report, c.getDouble(3))+"｜Wilson="+percentSnapshotCP4(report, c.getDouble(4))
                +"｜覆盖="+percentSnapshotCP4(report, c.getDouble(5))+"｜配对="+c.getInt(6)+":"+percentSignedSnapshotCP4(report, c.getDouble(7))
                +"｜FDR="+percentSnapshotCP4(report, c.getDouble(8))+"｜原因="+explicitSnapshotCP4(report, c.getString(9),"WAITING")
                +"｜updatedAt="+c.getLong(10);
    }catch(Throwable e){throw new com.ethk3.novaonepro.ReportReadContextCP4.ReportReadFailureCP4("REPORT_QUERY_OR_FORMAT_FAILED",e);}finally{if(c!=null)c.close();}}

public static String pipelineSummarySnapshotCP4(ReportReadContextCP4 report, ReportReadContextCP4 store,DiagnosticSnapshotR811 d,SettlementCoverageTruthR906 cov){
        RuntimeHealthLedgerR36.Metrics m=RuntimeHealthLedgerR36.metricsSnapshotCP4(report,store,RuntimeHealthLedgerR36.DURABLE_RETURN_EVENT_R923,96);
        return "R923运行闭环｜开奖="+explicitSnapshotCP4(report, d.latestDraw,"N/A")+"｜预测="+explicitSnapshotCP4(report, d.latestCanonical,"N/A")
                +"｜结算="+explicitSnapshotCP4(report, d.latestSettled,"N/A")+"｜严格OOS="+explicitSnapshotCP4(report, d.latestOos,"N/A")
                +"\n开奖任务终态="+cov.drawTaskTerminal+'/'+cov.drawTaskTotal+"｜开放="+cov.drawTaskOpen
                +"｜预测结算="+cov.settledPredictions+'/'+cov.predictedEligible+"｜无赛前预测排除="+cov.excludedNoPredraw
                +"｜阻断="+cov.blocked+"｜延后证据="+d.deferredPending
                +"｜分析并发="+AnalysisSingleFlightR36.shared().activeCount()
                +"｜请求至事务返回P95(最近96)="+(m==null||m.sample<=0||m.p95Ms<0?"N/A":m.p95Ms+"ms")
                +"｜实时压力="+(d.realtimePressure?"YES":"NO");
    }

public static String shadowSummarySnapshotCP4(ReportReadContextCP4 report, DiagnosticSnapshotR811 d){
        int pending=Math.max(0,d.shadowPredicted-d.shadowSettled);
        String rate=d.shadowSettled<=0?"N/A":percentSnapshotCP4(report, d.shadowHit/(double)d.shadowSettled);
        String coverage=d.shadowPredicted<=0?"N/A":percentSnapshotCP4(report, d.shadowSettled/(double)d.shadowPredicted);
        return "R923影子预测统计｜预测="+d.shadowPredicted+"｜已结算="+d.shadowSettled+"｜待结算="+pending
                +"｜预测结算完成率="+coverage+"｜命中/未中="+d.shadowHit+'/'+d.shadowMiss+"｜命中率="+rate
                +"（命中÷已结算，不是命中÷未中）"
                +"｜SHADOW不计真实盈亏";
    }

public static String realGateSummarySnapshotCP4(ReportReadContextCP4 report, ReportReadContextCP4 store,SQLiteDatabase db){
        Models.RuntimeStats s=store.getRuntimeStats();
        int settled=s==null?0:s.betCount,hit=s==null?0:s.hitCount,miss=s==null?0:s.missCount;
        int open=s==null?0:s.openTrades;double pnl=s==null?0.0d:s.pnl;
        String rate=settled<=0?"N/A":percentSnapshotCP4(report, hit/(double)settled);
        ControlledRealContractAuditR922.Report contract=ControlledRealContractAuditR922.captureSnapshotCP4(report,store);
        ControlledCandidateOpportunityAuditR922.Report funnel=
                ControlledCandidateOpportunityAuditR922.captureSnapshotCP4(report,store,120);
        return "R923真实出手统计｜已结算REAL="+settled+"｜命中/未中="+hit+'/'+miss
                +"｜真实胜率="+rate+"｜真实盈亏="+signedNumberSnapshotCP4(report, pnl)+"｜未结算REAL="+open
                +"｜MODEL_PROVEN="+V51BuildManifest.MODEL_PROVEN
                +"｜REALTIME_PROVEN="+V51BuildManifest.REALTIME_PROVEN
                +"｜SHADOW_ONLY="+V51BuildManifest.SHADOW_ONLY
                +"｜受控候选门="+(V51CandidateRealGateR915.releaseBoundary()?"ACTIVE":"LOCKED")
                +"｜机会频率="+funnel.frequencyText()+"(目标15%仅观察)"
                +"｜REAL合同="+contract.status+"/invalid="+contract.invalid+"/dup="+contract.duplicateTargets
                +"｜DecisionGateEngine_ONLY";
    }

public static SettlementCoverageTruthR906 coverageSnapshotCP4(ReportReadContextCP4 report, SQLiteDatabase db){
        Cursor c=null;try{
            // R912 denominator truth: every frozen canonical prediction whose exact draw exists
            // is eligible, whether or not its task has managed to complete yet. R906 previously
            // counted COMPLETED tasks as the denominator, making a badly stuck queue look healthy.
            c=report.select("SELECT "+
                    "(SELECT COUNT(1) FROM "+DbHelper.TABLE_R35_SHADOW_OBSERVATION+" s WHERE s.pre_draw_frozen=1 AND EXISTS(SELECT 1 FROM "+DbHelper.TABLE_DRAWS+" d WHERE d.issue=s.target_issue AND d.dirty=0)),"+
                    "(SELECT COUNT(1) FROM "+DbHelper.TABLE_R35_SHADOW_OBSERVATION+" s WHERE s.pre_draw_frozen=1 AND s.settled=1 AND EXISTS(SELECT 1 FROM "+DbHelper.TABLE_R35_OOS_LABEL+" l WHERE l.target_issue=s.target_issue AND l.strict_oos=1)),"+
                    "(SELECT COUNT(1) FROM "+DbHelper.TABLE_R36_SETTLEMENT_TASK+" t WHERE t.status='NO_PREDRAW_CANONICAL_TERMINAL'),"+
                    "(SELECT COUNT(1) FROM "+DbHelper.TABLE_R36_SETTLEMENT_TASK+" t WHERE t.status IN ('FAILED_TERMINAL','INVALID_DRAW_TERMINAL')),"+
                    "(SELECT COUNT(1) FROM "+DbHelper.TABLE_R36_SETTLEMENT_TASK+" t WHERE EXISTS(SELECT 1 FROM "+DbHelper.TABLE_DRAWS+" d WHERE d.issue=t.target_issue AND d.dirty=0)),"+
                    "(SELECT COUNT(1) FROM "+DbHelper.TABLE_R36_SETTLEMENT_TASK+" t WHERE t.status IN ('COMPLETED','NO_PREDRAW_CANONICAL_TERMINAL','FAILED_TERMINAL','INVALID_DRAW_TERMINAL') AND EXISTS(SELECT 1 FROM "+DbHelper.TABLE_DRAWS+" d WHERE d.issue=t.target_issue AND d.dirty=0))",null);
            if(!c.moveToFirst())return new SettlementCoverageTruthR906(0,0,0,0,0);
            int eligible=c.getInt(0),settled=c.getInt(1),excluded=c.getInt(2),blocked=c.getInt(3),tasks=c.getInt(4),terminal=c.getInt(5);
            return new SettlementCoverageTruthR906(eligible,settled,excluded,blocked,Math.max(0,eligible-settled),tasks,terminal,Math.max(0,tasks-terminal));
        }catch(Throwable e){throw new com.ethk3.novaonepro.ReportReadContextCP4.ReportReadFailureCP4("REPORT_QUERY_OR_FORMAT_FAILED",e);}
        finally{if(c!=null)c.close();}
    }

public static TruthRow settledForSnapshotCP4(ReportReadContextCP4 report, SQLiteDatabase db,String issue){return truthRowSnapshotCP4(report, db," WHERE o.pre_draw_frozen=1 AND o.settled=1 AND o.target_issue=? LIMIT 1",new String[]{issue});}

public static String currentActionSnapshotCP4(ReportReadContextCP4 report, SQLiteDatabase db,String target){return strScalarSnapshotCP4(report, db,"SELECT decision_action FROM "+DbHelper.TABLE_R35_SHADOW_OBSERVATION+" WHERE target_issue=? LIMIT 1",new String[]{target},"");}

public static String currentRejectSnapshotCP4(ReportReadContextCP4 report, SQLiteDatabase db,String target){return strScalarSnapshotCP4(report, db,"SELECT reject_code FROM "+DbHelper.TABLE_R35_SHADOW_OBSERVATION+" WHERE target_issue=? LIMIT 1",new String[]{target},"");}

public static String shapeTruthSnapshotCP4(ReportReadContextCP4 report, SQLiteDatabase db,String target){Cursor c=null;try{
        c=report.select("SELECT shape_lifecycle,shape_prototype,shape_run_length,shape_change_risk,shape_confidence FROM "+V51Repository.SELECTED+" WHERE target_issue=? LIMIT 1",new String[]{target});
        if(!c.moveToFirst())return "不可用｜原因=SHAPE_NOT_FROZEN";
        String lifecycle=explicitSnapshotCP4(report, c.getString(0),"UNKNOWN"),prototype=explicitSnapshotCP4(report, c.getString(1),"N/A");
        String change=c.getDouble(3)<0?"N/A":String.format(Locale.US,"%.2f%%",c.getDouble(3)*100d);
        String confidence=c.getDouble(4)<0?"N/A":String.format(Locale.US,"%.2f%%",c.getDouble(4)*100d);
        return "生命周期="+lifecycle+"｜原型="+prototype+"｜连续="+Math.max(0,c.getInt(2))+"｜变局风险="+change+"｜置信="+confidence;
    }catch(Throwable e){throw new com.ethk3.novaonepro.ReportReadContextCP4.ReportReadFailureCP4("REPORT_QUERY_OR_FORMAT_FAILED",e);}finally{if(c!=null)c.close();}}

public static String firstStateSnapshotCP4(ReportReadContextCP4 report, ReportReadContextCP4 store,String primary,String legacy,String fallback){
        if(store==null)return fallback;String one=safeSnapshotCP4(report, store.getState(primary,""));
        return one.length()>0?one:explicitSnapshotCP4(report, store.getState(legacy,""),fallback);
    }

public static String canonicalSnapshotCP4(ReportReadContextCP4 report, String target,String draw,String canonical,String closure,String oos,long version,TruthCard[] cards,StateBox[] boxes,SettlementCoverageTruthR906 cov,String research,String gate){
        ReportTextCP12 b=new ReportTextCP12(report, 4096);b.append(VERSION).append('|').append(target).append('|').append(draw).append('|').append(canonical).append('|').append(closure).append('|').append(oos).append('|').append(version).append('|').append(cov.canonical()).append('|').append(research).append('|').append(gate);
        for(TruthCard c:cards)b.append('|').append(c.canonical());for(StateBox s:boxes)b.append('|').append(s.canonical());return b.toString();
    }

public static long longScalarSnapshotCP4(ReportReadContextCP4 report, SQLiteDatabase db,String sql,String[] args,long d){Cursor c=null;try{c=report.select(sql,args);return c.moveToFirst()&&!c.isNull(0)?c.getLong(0):d;}catch(Throwable e){throw new com.ethk3.novaonepro.ReportReadContextCP4.ReportReadFailureCP4("REPORT_QUERY_OR_FORMAT_FAILED",e);}finally{if(c!=null)c.close();}}
public static RuntimeTruthSnapshotR906 captureSnapshotCP4(ReportReadContextCP4 report, ReportReadContextCP4 store){
        return captureSnapshotCP4(report, store, "",true);
    }

public static RuntimeTruthSnapshotR906 captureSnapshotCP4(ReportReadContextCP4 report, ReportReadContextCP4 store,String expectedTarget){
        return captureSnapshotCP4(report, store,expectedTarget,true);
    }

public static RuntimeTruthSnapshotR906 captureSnapshotCP4(ReportReadContextCP4 report, ReportReadContextCP4 store,String expectedTarget,boolean includeDetails){
        if(store==null)return errorSnapshotCP4(report, "STORE_NULL");
        SQLiteDatabase db=null; boolean own=false; long now=report.nowMs();
        try{
            db=store.readable();
            if(db==null||!db.isOpen())return errorSnapshotCP4(report, "DB_NOT_OPEN");
            if (!db.isReadOnly()) throw new ReportReadContextCP4.ReportReadFailureCP4("REPORT_WRITABLE_CAPTURE");
            long dataVersion=longScalarSnapshotCP4(report, db,"PRAGMA data_version",null,0L);
            String draw=strScalarSnapshotCP4(report, db,"SELECT issue FROM "+DbHelper.TABLE_DRAWS+" WHERE dirty=0 ORDER BY issue DESC LIMIT 1",null,"");
            String canonical=strScalarSnapshotCP4(report, db,"SELECT target_issue FROM "+DbHelper.TABLE_R35_SHADOW_OBSERVATION+" WHERE pre_draw_frozen=1 ORDER BY commit_at DESC,target_issue DESC LIMIT 1",null,"");
            String closure=strScalarSnapshotCP4(report, db,"SELECT target_issue FROM "+DbHelper.TABLE_R36_SETTLEMENT_TASK+" WHERE status IN ('COMPLETED','NO_PREDRAW_CANONICAL_TERMINAL','FAILED_TERMINAL','INVALID_DRAW_TERMINAL') ORDER BY target_issue DESC LIMIT 1",null,"");
            String oos=strScalarSnapshotCP4(report, db,"SELECT target_issue FROM "+DbHelper.TABLE_R35_OOS_LABEL+" WHERE strict_oos=1 ORDER BY target_issue DESC LIMIT 1",null,"");
            String requested=safeSnapshotCP4(report, expectedTarget);
            String target=TargetIssueResolver.isValid(requested)
                    ? requested
                    : (canonical.length()>0?canonical:(closure.length()>0?closure:draw));
            SettlementCoverageTruthR906 cov=coverageSnapshotCP4(report, db);
            TruthCard[] cards=buildTruthCardsSnapshotCP4(report, db,target,cov);
            StateBox[] boxes=buildStateBoxesSnapshotCP4(report, store,db,target,draw,canonical,closure,oos,cov);
            DiagnosticSnapshotR811 diagnostic=DiagnosticSnapshotR811.captureSnapshotCP4(report,store);
            BusinessFunnelCP12.Snapshot businessFunnel=BusinessFunnelCP12.captureSnapshotCP12(report,target);
            boxes[14]=modernResearchBoxR65(businessFunnel,target);
            String research=modernResearchBoxR65(businessFunnel,target).value+"\n"+businessFunnel.summary()
                    +"\n【旧研究模块统计，独立于当前候选计数】\n"+researchSummarySnapshotCP4(report, store,db);
            String researchDetail=includeDetails?researchDetailSnapshotCP4(report, store,db,research)+"\n"+businessFunnel.detail():research;
            String gate=realGateSummarySnapshotCP4(report, store,db);
            String pipelineSummary=pipelineSummarySnapshotCP4(report, store,diagnostic,cov);
            String pipelineDetail=includeDetails?pipelineDetailSnapshotCP4(report, store,db,diagnostic,cov,pipelineSummary):pipelineSummary;
            String shadowSummary=shadowSummarySnapshotCP4(report, diagnostic);
            String shadowDetail=includeDetails?shadowDetailSnapshotCP4(report, db,diagnostic,shadowSummary):shadowSummary;
            String realDetail=includeDetails?realDetailSnapshotCP4(report, store,db,gate):gate;
            String payload=canonicalSnapshotCP4(report, target,draw,canonical,closure,oos,dataVersion,cards,boxes,cov,research,gate)+"|FUNNEL|"+businessFunnel.canonical();
            String hash=com.ethk3.novaonepro.ReportTextCP12.sha256(report, payload);
            String id="R922-"+(target.length()==0?"NO_TARGET":target)+"-"+shortHashSnapshotCP4(report, hash);
            
            return new RuntimeTruthSnapshotR906(id,hash,"AVAILABLE","",now,dataVersion,
                    target,draw,canonical,closure,oos,cards,boxes,cov,pipelineSummary,pipelineDetail,
                    shadowSummary,shadowDetail,research,researchDetail,gate,realDetail,businessFunnel);
        }catch(Throwable e){throw new com.ethk3.novaonepro.ReportReadContextCP4.ReportReadFailureCP4("REPORT_QUERY_OR_FORMAT_FAILED",e);}
    }
}
