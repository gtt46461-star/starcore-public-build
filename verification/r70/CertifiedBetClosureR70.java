package com.ethk3.novaonepro;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.util.ArrayList;
import java.util.Locale;

/** Isolated synthetic certificate fixture; actual selector, gate, DAO and settlement owner. */
public final class CertifiedBetClosureR70 {
    static int checks;
    static final String H=V51ChecksumEngine.sha256("R70_SYNTHETIC_CERTIFIED_BET_CLOSURE");
    static void need(boolean value,String name){if(!value)throw new AssertionError(name);checks++;System.out.println("PASS|"+name);}
    static long count(SQLiteDatabase db,String table){try(Cursor c=db.rawQuery("SELECT COUNT(*) FROM "+table,null)){c.moveToFirst();return c.getLong(0);}}
    static V51Models.PositionAction[] actions(){
        ArrayList<V51Models.PositionAction> all=new ArrayList<V51Models.PositionAction>();
        for(int p=1;p<=10;p++)for(int a=1;a<=8;a++)for(int b=a+1;b<=9;b++)for(int c=b+1;c<=10;c++){
            int d=1;while(d==a||d==b||d==c)d++;
            all.add(new V51Models.PositionAction(p,a,b,c,d,.3,.3,0,0,.99,.1,new double[0],H,
                    all.size()+1,V51ChecksumEngine.sha256(p+":"+a+":"+b+":"+c)));
        }
        return all.toArray(new V51Models.PositionAction[0]);
    }
    static long insert(SQLiteDatabase db,DbStore store,V51RuntimeResult result){
        db.beginTransaction();try{
            long row=store.insertAuthorizedTradeWithinCanonicalR915(db,result,System.currentTimeMillis());
            db.setTransactionSuccessful();return row;
        }finally{db.endTransaction();}
    }
    public static void main(String[] args)throws Exception{
        SQLiteDatabase db=new SQLiteDatabase(args[0],args[1]);
        try{
            DbStore store=HostProductR974.store(db);
            store.setState(BuildIdentity.STATE_STATUS,BuildIdentity.STATUS_OK);
            int committed=0;
            for(int i=361;i<=363;i++){
                String target=String.format(Locale.US,"20260923%04d",i),base=String.format(Locale.US,"20260923%04d",i-1);
                long now=System.currentTimeMillis();
                try(RecipeBudgetScopeCP12 recipe=RecipeBudgetScopeCP12.open();
                        ModelBundleRuntimeR932.Scope scope=ModelBundleRuntimeR932.open(store,target,now)){
                    V51Models.PredictionContext context=new V51Models.PredictionContext(base,target,base,H,
                            V51BuildManifest.COHORT_ID,ModelBundleRuntimeR932.current().hash,now,120);
                    need(UnifiedResearchChoiceR932.freeze(store,target),"CERTIFIED_CHOICE_FROZEN_"+i);
                    AnalysisSingleFlightR36.Request request=new AnalysisSingleFlightR36.Request(base,target,1,now,now+120000,"R70_SYNTHETIC",H,H);
                    CertifiedPredictionR974.Prepared prepared=CertifiedPredictionR974.prepare(store,context,actions(),request);
                    if(i==362){
                        // The fixture contains an intentionally signed DELTA_NUMERIC atom in
                        // a FIXED_GRAPH score slot: 4 -> 1 must reject, 1 -> 1 recovers next issue.
                        need(prepared==null,"INVALID_SIGNED_SCORE_HAS_NO_CERTIFIED_PREDICTION");
                        need(UnifiedResearchChoiceR932.currentChoiceReasonCP12(store,target).contains("NEGATIVE_GRAPH_SCORE"),
                                "INVALID_EXECUTION_HAS_DURABLE_DIAGNOSTIC");
                        need(count(db,"trades")==committed,"INVALID_EXECUTION_CREATES_NO_REAL");
                    }else{
                    need(prepared!=null,"FROZEN_MODEL_ACTUALLY_SCORED_"+i);
                    V51Models.SelectedActionEnvelope selected=new V51Models.SelectedActionEnvelope(context,prepared.rankOne,
                            prepared.proof.selectedHitP,null,null,V51Enums.Lane.CONTROLLED_PROBE,1,1,H,prepared.proof.selectionHash);
                    V51CompiledActionContract compiled=new V51CompiledActionContract(selected,
                            new V51SelectiveRiskController.Result(V51Enums.Lane.CONTROLLED_PROBE,V51Enums.RejectCode.NONE,null,true,H),
                            null,H,H,H,H,H,prepared.proof);
                    V51CandidateRealGateR915.Evaluation gate=V51CandidateRealGateR915.evaluate(store,compiled);
                    need(gate.eligible,"EXACT_CALIBRATED_GATE_ELIGIBLE_"+i+"|"+gate.code);
                    V51Models.AuthorizedDecision decision=new DecisionGateEngine().decideV51B12(store,compiled);
                    need(decision.action==V51Enums.DecisionAction.BET,"SOLE_DECISION_GATE_AUTHORIZED_"+i);
                    V51RuntimeResult result=new V51RuntimeResult(true,context,null,null,null,null,null,prepared.actions,
                            selected,decision,compiled,"R70_SYNTHETIC",1,H);
                    long row=insert(db,store,result);
                    committed++;
                    need(row>0&&count(db,"trades")==committed,"ONE_REAL_ROW_COMMITTED_"+i);
                    need(insert(db,store,result)==row&&count(db,"trades")==committed,"REPLAY_NO_DUPLICATE_REAL_"+i);
                    need(!V51CandidateRealGateR915.evaluate(store,compiled).eligible,"DUPLICATE_AUTHORIZATION_REJECTED_"+i);
                    // Explicit synthetic frozen observation for the actual settlement coordinator.
                    // Full canonical snapshot serialization is covered by the separate R36 suite.
                    V51Models.PositionAction action=selected.action;
                    db.execSQL("INSERT INTO v57_shadow_observation_r35(target_issue,game_code,base_issue,position,n1,n2,n3,lane,decision_action,reject_code,bundle_hash,contract_hash,authorization_hash,record_hash,created_at,commit_at,pre_draw_frozen,history_hash,snapshot_hash,action_space_hash,history_max_issue,selected_hit_p) VALUES(?,?,?,?,?,?,?,'CONTROLLED_PROBE','BET','',?,?,?,?,?,?,1,?,?,?,?,?)",
                            new Object[]{target,V51Repository.GAME_CODE,base,action.position,action.n1,action.n2,action.n3,
                                context.artifactBundleHash,H,decision.authorizationHash,V51ChecksumEngine.sha256(H+'|'+target),now,now,H,H,H,base,selected.selectedHitP});
                    }
                }
                long openedAt=System.currentTimeMillis()+1;
                db.execSQL("INSERT INTO draws(issue,open_time,fetch_time,first_seen_at,dirty,n1,n2,n3,n4,n5,n6,n7,n8,n9,n10) VALUES(?,?,?,?,0,1,2,3,4,5,6,7,8,9,10)",
                        new Object[]{target,openedAt,openedAt+1,openedAt+1});
                if(i!=362){
                    Models.Draw draw=new Models.Draw();draw.issue=target;draw.openTime=openedAt;
                    for(int n=0;n<10;n++)draw.numbers[n]=n+1;
                    SettlementEngine.SettlementReport settlement=SettlementCoordinatorR815.settleNow(store,draw,SettlementCoordinatorR815.Reason.LEGACY_API);
                    need(settlement.ok==1&&settlement.settledCount==1&&settlement.shadowSettledCount==1,
                            "SOLE_OWNER_REAL_AND_OOS_SETTLED_"+i+"|"+settlement.detail);
                    SettlementEngine.SettlementReport replay=SettlementCoordinatorR815.settleNow(store,draw,SettlementCoordinatorR815.Reason.LEGACY_API);
                    need(replay.ok==1&&replay.settledCount==0,"SETTLEMENT_REPLAY_NO_DUPLICATE_"+i);
                    try(Cursor c=db.rawQuery("SELECT settled,actual_number,hit,pnl,n1,n2,n3 FROM trades WHERE target_issue=?",new String[]{target})){
                        need(c.moveToFirst()&&c.getInt(0)==1&&c.getInt(1)==1,"SETTLEMENT_VALUES_COMMITTED_"+i);
                        boolean hit=c.getInt(4)==1||c.getInt(5)==1||c.getInt(6)==1;
                        need(c.getInt(2)==(hit?1:0)&&Double.compare(c.getDouble(3),hit?AppConfig.WIN_PNL_K3:AppConfig.MISS_PNL_K3)==0,
                                "EXACT_K3_PNL_"+i);
                    }
                }
            }
            need(count(db,"r974_exact_real_receipt")==2,"EXACT_RECEIPTS_COMMITTED_WITH_BOTH_TRADES");
            need(count(db,DbHelper.TABLE_R35_OOS_LABEL)==2,"TWO_MATCHING_OOS_LABELS_WITHOUT_REPLAY_DUPLICATES");
            System.out.println("R70_CERTIFIED_BET_CLOSURE_PASS|checks="+checks+"|scope=SYNTHETIC_HOST_ACTUAL_GATE_NO_USER_BET");
        }finally{db.shutdown();}
    }
}
