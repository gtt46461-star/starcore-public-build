package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.*;
import com.ethk3.novaonepro.r952.StrictCanonicalJson;
import java.util.*;

/** Exact model projection into the existing R974 lane. This class never creates a REAL trade. */
public final class MainlineAdapterSU1 {
    public static final String CONTRACT=HashingSU1.text("SU1_MAINLINE_V1|ORIGINAL_R974_GATE_AND_DBSTORE_OWNER|EXACT_HEAD_V2|PRECOMMITTED_1200|FROZEN_CALIBRATION|NO_COVERAGE_QUOTA");
    private MainlineAdapterSU1() {}
    /** Preserved Root rows predate ownership cutover. They cannot authorize a new owner binding without a
     * versioned certificate+sealed-prediction projection. Never read RP while Root's writer owns a transaction. */
    static boolean projectionRequired(SQLiteDatabase db){return !ResearchRouteR29.isResearchDatabase(db)&&ResearchRouteR29.active(db);}
    public static CertificateSU1.Authority authority(DbStore store,String candidate,String structure,String target,String certificate){
        boolean projected=projectionRequired(store.readable());
        if(candidate==null||!candidate.startsWith(CertificateSU1.PREFIX)||!TargetIssueResolver.isValid(target)||!target.equals(ModelBundleRuntimeR932.currentTarget())||store.hasDrawIssue(target))return null;
        String strategy=candidate.substring(CertificateSU1.PREFIX.length());HashingSU1.sha(strategy);
        if(projected){CertificateSU1.Authority a=LegacyAuthorityProjectionR29.su1ForStrategy(store.readable(),strategy,certificate,System.currentTimeMillis());return a!=null&&a.program.equals(structure)&&a.issued<ModelBundleRuntimeR932.capturedAt()?a:null;}
        try(Cursor c=store.readable().rawQuery("SELECT certificate_id FROM su1_certificate WHERE strategy_id=? AND (?='' OR certificate_id=?) ORDER BY issued_at DESC,certificate_id LIMIT 1",new String[]{strategy,certificate,certificate})){
            if(!c.moveToFirst())return null;CertificateSU1.Authority a=CertificateSU1.read(store.readable(),c.getString(0),System.currentTimeMillis(),true);
            return a!=null&&a.program.equals(structure)&&a.issued<ModelBundleRuntimeR932.capturedAt()?a:null;
        }
    }
    public static CertificateSU1.Authority first(DbStore store,String target){
        if(projectionRequired(store.readable())){
            CertificateSU1.Authority a;try(LegacyResearchScopeR29.Scope scope=LegacyResearchScopeR29.enter(store)){a=first(store,target);}
            LegacyAuthorityProjectionR29.drain(store);return a==null?null:authority(store,a.candidate(),a.program,target,a.certificate);
        }
        if(!StrategySchemaSU1.ready(store.readable())||!StrategyRepositorySU1.exists(store.readable(),"SELECT 1 FROM su1_runtime_control WHERE singleton=1 AND mode='MAINLINE_ADAPTER' AND contract_hash=?",CONTRACT))return null;
        MainlineHeadSU1.maintain(store);
        List<String[]> rows=new ArrayList<>();try(Cursor c=store.readable().rawQuery("SELECT c.certificate_id,c.strategy_id,v.program_hash FROM su1_mainline_head_event h JOIN su1_certificate c ON c.certificate_id=h.certificate_id JOIN su1_strategy_version v ON v.strategy_id=c.strategy_id WHERE h.generation=(SELECT max(generation) FROM su1_mainline_head_event)",null)){while(c.moveToNext())rows.add(new String[]{c.getString(0),c.getString(1),c.getString(2)});}
        for(String[] r:rows){CertificateSU1.Authority a=authority(store,CertificateSU1.PREFIX+r[1],r[2],target,r[0]);if(a==null)continue;freeze(store,a,target);return a;}return null;
    }
    private static void freeze(DbStore store,CertificateSU1.Authority a,String target){
        if(StrategyRepositorySU1.exists(store.readable(),"SELECT 1 FROM su1_prediction p JOIN su1_opportunity o ON o.opportunity_id=p.opportunity_id JOIN su1_eval_cohort c ON c.cohort_id=o.cohort_id WHERE p.strategy_id=? AND CAST(p.target_issue AS TEXT)=? AND c.evidence_role='RESEARCH_FUTURE'",a.strategy,target))return;
        store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("SU1_EXACT_RESEARCH_FREEZE_REGISTER",db->{
            if(CertificateSU1.read(db,a.certificate,System.currentTimeMillis(),true)==null)throw new IllegalStateException("SU1_CERT_CHANGED_BEFORE_FREEZE");
            if(!StrategyRepositorySU1.exists(db,"SELECT 1 FROM su1_eval_cohort WHERE strategy_id=? AND CAST(first_issue AS TEXT)=? AND evidence_role='RESEARCH_FUTURE'",a.strategy,target))PromotionPipelineSU1.register(db,a.strategy,null,a.family,target,LiveShadowLoopSU1.sequence(target)-1,1,"PARENT_CHILD","RESEARCH_FUTURE");return 0;
        });
        EvidenceLoopSU1.capture(store,target);
    }
    public static final class Prepared {
        public final CertificateSU1.Authority authority;
        public final V51Models.PositionAction[] actions;
        public final V51Models.PositionAction rankOne;
        public final String recipe;
        public final double selectedHitP;
        Prepared(CertificateSU1.Authority a,V51Models.PositionAction[] actions,V51Models.PositionAction rankOne,String recipe,double p){authority=a;this.actions=actions;this.rankOne=rankOne;this.recipe=recipe;selectedHitP=p;}
    }
    public static Prepared prepare(DbStore store,CertificateSU1.Authority a,V51Models.PredictionContext context,V51Models.PositionAction[] actions){
        if(projectionRequired(store.readable())){
            Prepared p;try(LegacyResearchScopeR29.Scope scope=LegacyResearchScopeR29.enter(store)){p=prepare(store,a,context,actions);}
            LegacyAuthorityProjectionR29.drain(store);return p;
        }
        K3ActionSpaceEngine.validateOrThrow(actions);EventFrameSU1 frame=EventFrameBuilderSU1.capture(store,context.targetIssue);
        if(!String.valueOf(frame.sourceMaxIssue).equals(context.baseIssue))throw new IllegalStateException("SU1_EXACT_SOURCE_SCOPE");
        String text;try(Cursor c=store.readable().rawQuery("SELECT canonical_text FROM su1_runtime_program WHERE strategy_id=? AND kernel_hash=?",new String[]{a.strategy,TrainingCodeIdentityR964.SHA256})){if(!c.moveToFirst())throw new IllegalStateException("SU1_EXACT_PROGRAM");text=c.getString(0);}
        int[][] history=frame.history();double[][] scores;try(AtomLiftSU1 atoms=new AtomLiftSU1(history)){scores=ProgramTreeSU1.parse(text).score(new ProgramTreeSU1.Context(history,history.length,atoms,new ScoreHistoryCacheSU1(2048,frame),new BudgetTokenSU1(250,1000000,SettlementExecutionGateR819::isActive)).withFitted(ModelAssetsSU1.load(store.readable(),text,frame)).withGraphFitted(ModelAssetsSU1.loadGraph(store.readable(),text,frame)));}
        int[] head=ProgramTreeSU1.action(scores);Arrays.sort(head,1,4);int headOrdinal=K3ActionSpaceEngine.globalOrdinal(head[0],head[1],head[2],head[3]);
        double[][] probabilities=new double[10][10];for(int p=0;p<10;p++){double max=-Double.MAX_VALUE,total=0;for(double v:scores[p])max=Math.max(max,v);for(int n=0;n<10;n++){probabilities[p][n]=Math.exp(scores[p][n]-max);total+=probabilities[p][n];}for(int n=0;n<10;n++)probabilities[p][n]/=total;}
        double[] mass=new double[1200];int headIndex=-1;for(int i=0;i<1200;i++){V51Models.PositionAction action=actions[i];int ordinal=K3ActionSpaceEngine.globalOrdinal(action.position,action.n1,action.n2,action.n3);if(ordinal==headOrdinal)headIndex=i;double[] p=probabilities[action.position-1];mass[i]=Math.round((p[action.n1-1]+p[action.n2-1]+p[action.n3-1])*1e9)/1e9;}
        final int selectedIndex=headIndex;Integer[] order=new Integer[1200];for(int i=0;i<1200;i++)order[i]=i;Arrays.sort(order,(x,y)->{if(x==selectedIndex)return y==selectedIndex?0:-1;if(y==selectedIndex)return 1;int d=Double.compare(mass[y],mass[x]);return d!=0?d:Integer.compare(K3ActionSpaceEngine.globalOrdinal(actions[x].position,actions[x].n1,actions[x].n2,actions[x].n3),K3ActionSpaceEngine.globalOrdinal(actions[y].position,actions[y].n1,actions[y].n2,actions[y].n3));});
        double raw=HistoricalReplayEngineSU1.positionMasses(scores)[head[0]-1],p=CalibrationSU1.decode(CampaignStateSU1.localBlob(store.readable(),a.calibration)).predict(raw);String prediction,record;
        try(Cursor c=store.readable().rawQuery("SELECT p.prediction_id,p.record_hash,p.position,p.n1,p.n2,p.n3,p.raw_k3_score,p.selected_hit_p,s.source_version_hash FROM su1_prediction p JOIN su1_snapshot s ON s.snapshot_hash=p.snapshot_hash JOIN su1_opportunity o ON o.opportunity_id=p.opportunity_id JOIN su1_eval_cohort q ON q.cohort_id=o.cohort_id WHERE p.strategy_id=? AND CAST(p.target_issue AS TEXT)=? AND q.evidence_role='RESEARCH_FUTURE' ORDER BY p.captured_at LIMIT 1",new String[]{a.strategy,context.targetIssue})){
            if(!c.moveToFirst()||c.getInt(2)!=head[0]||c.getInt(3)!=head[1]||c.getInt(4)!=head[2]||c.getInt(5)!=head[3]||Double.compare(c.getDouble(6),raw)!=0||c.isNull(7)||Double.compare(c.getDouble(7),p)!=0||!frame.sourceVersionHash.equals(c.getString(8)))throw new IllegalStateException("SU1_EXACT_FROZEN_ACTION_CHANGED");prediction=c.getString(0);record=c.getString(1);
        }
        Map<String,Object> recipe=new TreeMap<>();recipe.put("contract",CONTRACT);recipe.put("strategy",a.strategy);recipe.put("certificate",a.certificate);recipe.put("program",a.program);recipe.put("artifact",a.artifact);recipe.put("selector",a.selector);recipe.put("calibration",a.calibration);recipe.put("target",context.targetIssue);recipe.put("source",frame.sourceVersionHash);recipe.put("generation",frame.sourceGeneration);recipe.put("prediction",prediction);recipe.put("record",record);String wire=StrictCanonicalJson.write(recipe),digest=HashingSU1.text(wire);
        LegacyAuthorityProjectionR29.sealSu1(store,a,frame,prediction,record);
        V51Models.PositionAction[] out=new V51Models.PositionAction[1200];for(int rank=0;rank<1200;rank++){int i=order[rank];V51Models.PositionAction x=actions[i];out[i]=new V51Models.PositionAction(x.position,x.n1,x.n2,x.n3,x.n4,mass[i],mass[i],rank==1199?0:Math.max(0,mass[i]-mass[order[rank+1]]),x.thirdMargin,x.entropy,x.stability,x.metaFeatures,x.fingerprint,rank+1,V51ChecksumEngine.sha256(CertifiedPredictionR974.LANE+'|'+x.checksum+'|'+digest+'|'+i+'|'+rank+'|'+mass[i]));}K3ActionSpaceEngine.validateOrThrow(out);return new Prepared(a,out,out[headIndex],wire,p);
    }
    public static boolean verifyFrozen(SQLiteDatabase db,CertifiedPredictionR974.Proof proof,V51Models.PositionAction action){
        if(projectionRequired(db))return LegacyAuthorityProjectionR29.verifySu1(db,proof,action);
        Map<String,Object> r=StrictCanonicalJson.readObject(proof.recipe);if(!CONTRACT.equals(r.get("contract"))||!proof.candidate.equals(CertificateSU1.PREFIX+r.get("strategy"))||!proof.certificate.equals(r.get("certificate"))||!proof.target.equals(r.get("target"))||!proof.modelHash.equals(r.get("artifact"))||!proof.calibrationHash.equals(r.get("calibration"))||!proof.selectionHash.equals(r.get("selector")))return false;
        if(!MainlineHeadSU1.isHead(db,(String)r.get("strategy"),proof.certificate))return false;
        if(!(r.get("generation") instanceof Number))return false;
        try(Cursor clock=db.rawQuery("SELECT generation FROM su1_source_clock WHERE singleton=1",null)){if(!clock.moveToFirst()||clock.getLong(0)!=((Number)r.get("generation")).longValue())return false;}
        try(Cursor c=db.rawQuery("SELECT p.record_hash,p.position,p.n1,p.n2,p.n3,p.selected_hit_p,p.calibration_hash,s.source_version_hash FROM su1_prediction p JOIN su1_snapshot s ON s.snapshot_hash=p.snapshot_hash WHERE p.prediction_id=? AND p.strategy_id=? AND CAST(p.target_issue AS TEXT)=?",new String[]{(String)r.get("prediction"),(String)r.get("strategy"),proof.target})){
            if(!c.moveToFirst()||!r.get("record").equals(c.getString(0))||c.isNull(5)||Double.compare(proof.selectedHitP,c.getDouble(5))!=0||!proof.calibrationHash.equals(c.getString(6))||!r.get("source").equals(c.getString(7)))return false;
            return action==null||action.position==c.getInt(1)&&action.n1==c.getInt(2)&&action.n2==c.getInt(3)&&action.n3==c.getInt(4);
        }
    }
    public static void bind(SQLiteDatabase db,CertifiedPredictionR974.Proof proof,String ownerReceipt){
        if(!proof.candidate.startsWith(CertificateSU1.PREFIX))return;if(!db.inTransaction()||!verifyFrozen(db,proof,null))throw new IllegalStateException("SU1_CANONICAL_OWNER_BINDING");Map<String,Object> r=StrictCanonicalJson.readObject(proof.recipe);
        StrategyRepositorySU1.row(db,"su1_mainline_binding","target_issue",Long.parseLong(proof.target),"strategy_id",r.get("strategy"),"certificate_id",proof.certificate,"prediction_id",r.get("prediction"),"adapter_contract_hash",CONTRACT,"original_owner_receipt_hash",HashingSU1.sha(ownerReceipt),"bound_at",System.currentTimeMillis());
    }
}
