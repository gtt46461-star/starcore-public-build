package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.*;
import java.util.*;

/** Fixed, pre-registered calendar opportunities include budget misses and both-missing outcomes. */
public final class EvidenceLoopSU1 {
    private EvidenceLoopSU1() {}
    private static final class Cohort {
        final String id,child,parent,role,epoch;final long registered,created,first;final int planned;
        Cohort(Cursor c){id=c.getString(0);child=c.getString(1);parent=c.getString(2);role=c.getString(3);registered=c.getLong(4);planned=c.getInt(5);created=c.getLong(6);first=LiveShadowLoopSU1.sequence(c.getString(7));epoch="CANONICAL_PK10_V1";}
    }
    private static List<Cohort> cohorts(SQLiteDatabase db,String target){List<Cohort> out=new ArrayList<>();try(Cursor c=db.rawQuery("SELECT c.cohort_id,c.strategy_id,c.parent_strategy_id,c.evidence_role,c.registered_sequence,c.planned_opportunities,c.created_at,c.first_issue FROM su1_eval_cohort c JOIN su1_cohort_runtime r ON r.cohort_id=c.cohort_id WHERE c.first_issue<=? AND r.last_issue>=? AND (r.state='OPEN' OR EXISTS(SELECT 1 FROM su1_opportunity o WHERE o.cohort_id=c.cohort_id AND CAST(o.target_issue AS TEXT)=?)) ORDER BY c.created_at,c.cohort_id LIMIT 32",new String[]{target,target,target})){while(c.moveToNext())out.add(new Cohort(c));}return out;}
    private static String opportunity(SQLiteDatabase db,Cohort c,String target,long open){
        long seq=LiveShadowLoopSU1.sequence(target),ordinal=seq-c.first+1;if(ordinal<1||ordinal>c.planned)throw new IllegalStateException("SU1_OPPORTUNITY_ORDINAL");String id=HashingSU1.fields("SU1_OPPORTUNITY_V1",c.id,target);
        if(!StrategyRepositorySU1.exists(db,"SELECT 1 FROM su1_opportunity WHERE opportunity_id=?",id))StrategyRepositorySU1.row(db,"su1_opportunity","opportunity_id",id,"cohort_id",c.id,"target_issue",Long.parseLong(target),"sequence_ordinal",seq,"opportunity_ordinal",ordinal,"declared_at",c.created,"deadline_at",open,"source_epoch",c.epoch,"eligibility_hash",HashingSU1.fields(PromotionPipelineSU1.POLICY,c.id,target),"role",c.role);return id;
    }
    private static final class Prediction {
        final String id,artifact,probabilities,state,calibration;final long trained;final int position,mask;final int[] numbers;final double raw;final Double calibrated;
        Prediction(String id,String artifact,long trained,int position,int[] numbers,double raw,String probabilities,String state,String cal,Double p){this.id=id;this.artifact=artifact;this.trained=trained;this.position=position;this.numbers=numbers;this.raw=raw;this.probabilities=probabilities;this.state=state;calibration=cal;calibrated=p;mask=(1<<(numbers[0]-1))|(1<<(numbers[1]-1))|(1<<(numbers[2]-1));}
    }
    private static Prediction prepare(SQLiteDatabase db,String id,EventFrameSU1 frame,int[][] history,AtomLiftSU1 atoms,ScoreHistoryCacheSU1 cache,BudgetTokenSU1 budget){
        String kind,artifact,calibration,seed,text;long trained;
        try(Cursor c=db.rawQuery("SELECT m.model_kind,v.artifact_hash,m.trained_through_sequence,v.calibration_hash,w.local_locator,r.canonical_text FROM su1_strategy_version v JOIN su1_model_artifact m ON m.artifact_hash=v.artifact_hash JOIN su1_blob w ON w.blob_hash=m.weights_hash LEFT JOIN su1_runtime_program r ON r.strategy_id=v.strategy_id WHERE v.strategy_id=?",new String[]{id})){if(!c.moveToFirst())throw new IllegalStateException("SU1_STRATEGY_MISSING");kind=c.getString(0);artifact=c.getString(1);trained=c.getLong(2);calibration=c.getString(3);seed=c.getString(4);text=c.getString(5);}
        int position;int[] numbers;double raw;double[][] probabilities=new double[10][10];
        if(kind.equals("NULL_BASELINE")){NullBaseline.Choice a=NullBaseline.choose(seed,frame.targetIssue);position=a.position;numbers=a.numbers();raw=.3;for(double[] row:probabilities)Arrays.fill(row,.1);}
        else{budget.checkpointOrYield();double[][] scores=kind.equals("SU1_SHUFFLED_LABEL_CONTROL")?ShuffledLabelControlSU1.score(history,seed,budget):ProgramTreeSU1.parse(text).score(new ProgramTreeSU1.Context(history,history.length,atoms,cache,budget).withFitted(ModelAssetsSU1.load(db,text,frame)).withGraphFitted(ModelAssetsSU1.loadGraph(db,text,frame)));int[] a=ProgramTreeSU1.action(scores);position=a[0];numbers=new int[]{a[1],a[2],a[3]};Arrays.sort(numbers);raw=HistoricalReplayEngineSU1.positionMasses(scores)[position-1];for(int p=0;p<10;p++){double max=-Double.MAX_VALUE;for(double v:scores[p])max=Math.max(max,v);double total=0;for(int n=0;n<10;n++){probabilities[p][n]=Math.exp(scores[p][n]-max);total+=probabilities[p][n];}for(int n=0;n<10;n++)probabilities[p][n]/=total;}}
        StringBuilder wire=new StringBuilder("SU1_PROBABILITY_V1|");for(double[] row:probabilities)for(double value:row)wire.append(Double.toHexString(value)).append(',');Double calibrated=null;
        if(calibration!=null){CalibrationSU1 cal=CalibrationSU1.decode(CampaignStateSU1.localBlob(db,calibration));if(!cal.valid())throw new IllegalStateException("SU1_CALIBRATION_NOT_VALIDATED");calibrated=cal.predict(raw);}
        return new Prediction(id,artifact,trained,position,numbers,raw,wire.toString(),"SU1_WINDOW_RESET_V3|"+id+'|'+frame.sourceVersionHash+"|assetTrainedThrough="+trained+"|runtimeFitKnowledgeThrough="+((kind.equals("CAUSAL_WINDOW_PROGRAM")||kind.equals("SU1_SHUFFLED_LABEL_CONTROL"))?LiveShadowLoopSU1.sequence(String.valueOf(frame.sourceMaxIssue)):trained),calibration,calibrated);
    }
    public static int capture(DbStore store,String target){
        if(!StrategySchemaSU1.ready(store.readable()))return 0;
        if(!ArchiveStoreSU1.canCreate(store.readable()))return 0;List<Cohort> cohorts=cohorts(store.readable(),target);cohorts.removeIf(c->!StrategyRepositorySU1.exists(store.readable(),"SELECT 1 FROM su1_cohort_runtime WHERE cohort_id=? AND state='OPEN'",c.id));if(cohorts.isEmpty())return 0;
        EventFrameSU1 frame=EventFrameBuilderSU1.capture(store,target);SnapshotPayloadSU1 payload=SnapshotPayloadSU1.prepare(frame);Map<String,Prediction> predictions=new LinkedHashMap<>();int[][] history=frame.history();BudgetTokenSU1 budget=new BudgetTokenSU1(250,1000000,SettlementExecutionGateR819::isActive);ScoreHistoryCacheSU1 cache=new ScoreHistoryCacheSU1(2048,frame);
        try(AtomLiftSU1 atoms=new AtomLiftSU1(history)){for(Cohort c:cohorts)for(String id:new String[]{c.parent,c.child})if(id!=null&&!predictions.containsKey(id)){try{predictions.put(id,prepare(store.readable(),id,frame,history,atoms,cache,budget));}catch(BudgetTokenSU1.Yield exhausted){break;}}}
        return store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("SU1_CONFIRM_FREEZE",db->{
            if(!EventFrameBuilderSU1.unchanged(store,frame))return 0;StrategyRepositorySU1.snapshot(db,frame,payload);int count=0;
            for(Cohort c:cohorts){String opportunity=opportunity(db,c,target,frame.targetOpenTime);for(String id:new String[]{c.parent,c.child}){Prediction p=predictions.get(id);if(p==null)continue;
                String probability=StrategyRepositorySU1.blob(db,p.probabilities,"SU1_PROBABILITY_HEX_V1",frame.cutoff),state=StrategyRepositorySU1.blob(db,p.state,"SU1_PERIOD_STATE_V1",frame.cutoff);
                String prediction=HashingSU1.fields("SU1_PREDICTION_V1",opportunity,id),record=HashingSU1.fields(prediction,frame.sourceVersionHash,p.artifact,probability,state,String.valueOf(p.position),String.valueOf(p.mask),String.valueOf(p.calibration),String.valueOf(p.calibrated));
                try(Cursor old=db.rawQuery("SELECT record_hash FROM su1_prediction WHERE prediction_id=?",new String[]{prediction})){if(old.moveToFirst()){if(!record.equals(old.getString(0)))throw new IllegalStateException("SU1_PREDICTION_REPLAY_CONFLICT");continue;}}
                long captured=System.currentTimeMillis();if(captured>=frame.targetOpenTime)continue;
                StrategyRepositorySU1.row(db,"su1_prediction","prediction_id",prediction,"opportunity_id",opportunity,"strategy_id",id,"snapshot_hash",frame.contentHash,"target_issue",frame.targetIssue,"captured_at",captured,"position",p.position,"n1",p.numbers[0],"n2",p.numbers[1],"n3",p.numbers[2],"prediction_mask",p.mask,"raw_k3_score",p.raw,"selected_hit_p",p.calibrated,"calibration_hash",p.calibration,"probability_hash",probability,"period_state_hash",state,"trained_through_sequence",p.trained,"input_through_sequence",LiveShadowLoopSU1.sequence(String.valueOf(frame.sourceMaxIssue)),"record_hash",record);count++;
            }}return count;
        });
    }
    public static int settleWithin(SQLiteDatabase db,Models.Draw draw,String legacy,long completed){
        if(!StrategySchemaSU1.ready(db))return 0;LiveShadowLoopSU1.requireCompleted(db,draw,legacy,completed);List<Cohort> cohorts=cohorts(db,draw.issue);if(cohorts.isEmpty())return 0;
        StringBuilder canonical=new StringBuilder("SU1_CANONICAL_DRAW_V1|").append(draw.issue).append('|').append(draw.openTime);for(int p=1;p<=10;p++)canonical.append('|').append(draw.getNumberAtPosition(p));String blob=StrategyRepositorySU1.blob(db,canonical.toString(),"SU1_CANONICAL_DRAW_V1",completed);int count=0;
        for(Cohort c:cohorts){String opportunity=opportunity(db,c,draw.issue,draw.openTime);
            if(!StrategyRepositorySU1.exists(db,"SELECT 1 FROM su1_opportunity_outcome WHERE opportunity_id=?",opportunity))StrategyRepositorySU1.row(db,"su1_opportunity_outcome","opportunity_id",opportunity,"target_issue",Long.parseLong(draw.issue),"canonical_draw_hash",blob,"legacy_settlement_hash",legacy,"source_epoch",c.epoch,"opened_at",draw.openTime,"completed_at",completed);
            try(Cursor rows=db.rawQuery("SELECT p.prediction_id,p.position,p.prediction_mask FROM su1_prediction p WHERE p.opportunity_id=? AND NOT EXISTS(SELECT 1 FROM su1_settlement s WHERE s.prediction_id=p.prediction_id)",new String[]{opportunity})){while(rows.moveToNext()){String prediction=rows.getString(0);int actual=draw.getNumberAtPosition(rows.getInt(1)),hit=(rows.getInt(2)&(1<<(actual-1)))!=0?1:0;String label=HashingSU1.fields("SU1_LABEL_V1",prediction,blob,legacy,String.valueOf(actual));StrategyRepositorySU1.row(db,"su1_settlement","settlement_id",HashingSU1.fields("SU1_SETTLEMENT_V1",label),"prediction_id",prediction,"target_issue",Long.parseLong(draw.issue),"canonical_draw_hash",blob,"legacy_settlement_hash",legacy,"actual",actual,"hit",hit,"label_hash",label,"committed_at",completed);count++;}}
        }return count;
    }
    public static int recover(DbStore store){
        if(!StrategySchemaSU1.ready(store.readable()))return 0;List<String[]> targets=new ArrayList<>();
        try(Cursor c=store.readable().rawQuery("SELECT DISTINCT l.target_issue,l.settlement_hash,l.completed_at FROM "+DbHelper.TABLE_R36_SETTLEMENT_TASK+" l JOIN su1_eval_cohort c ON CAST(l.target_issue AS INTEGER)>=c.first_issue JOIN su1_cohort_runtime r ON r.cohort_id=c.cohort_id AND l.target_issue<=r.last_issue WHERE l.status='COMPLETED' AND (r.state='OPEN' OR EXISTS(SELECT 1 FROM su1_opportunity p WHERE p.cohort_id=c.cohort_id AND CAST(p.target_issue AS TEXT)=l.target_issue)) AND NOT EXISTS(SELECT 1 FROM su1_opportunity o JOIN su1_opportunity_outcome a ON a.opportunity_id=o.opportunity_id WHERE o.cohort_id=c.cohort_id AND CAST(o.target_issue AS TEXT)=l.target_issue) ORDER BY l.target_issue LIMIT 32",null)){while(c.moveToNext())targets.add(new String[]{c.getString(0),c.getString(1),c.getString(2)});}
        int n=0;for(String[] row:targets){Models.Draw draw=store.getDrawByIssue(row[0]);if(draw!=null&&draw.isValid()&&draw.dirty==0)n+=store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("SU1_CONFIRM_RECOVER",db->{LiveShadowLoopSU1.settleWithin(db,draw,row[1],Long.parseLong(row[2]));return settleWithin(db,draw,row[1],Long.parseLong(row[2]));});}return n;
    }
}
