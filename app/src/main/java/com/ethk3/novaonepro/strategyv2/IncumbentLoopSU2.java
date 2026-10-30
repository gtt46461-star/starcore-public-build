package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.*;
import java.io.*;
import java.util.function.Supplier;

/** The protected original method uses the same fenced, preemptible research owner as its challengers. */
public final class IncumbentLoopSU2 {
    private IncumbentLoopSU2() {}
    public static final String KEY=XiIncumbentSU2.GENOME_KEY,MEMBER="XI_INCUMBENT:"+KEY;
    /** Settlement wakes the owner; it never creates a second numerical executor. */
    public static void scheduleAfterSettlement(DbStore store,String settledIssue){if(TargetIssueResolver.isValid(settledIssue))RuntimeCoordinatorR26.wake();}

    public static void seed(DbStore store,long seq){store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("SU2_INCUMBENT_SEED",db->{seedWithin(db,seq);return 0;});}
    /** Own-strategy metadata only: no dependency on the complete legacy research overlay. */
    public static void seedWithin(android.database.sqlite.SQLiteDatabase db,long seq){
        if(!db.inTransaction())throw new IllegalStateException("SU2_INCUMBENT_SEED_WRITER");
        // Register the protected method; historical scores grant no certificate.
        db.execSQL("INSERT OR IGNORE INTO su2_model_genome(genome_key,canonical_text,formula,tier,origin_op,parent_key,repair_note,born_campaign,born_sequence,dev_fitness,val_fitness,state,created_at) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?)",
            new Object[]{KEY, XiIncumbentSU2.CANONICAL, "XI = 用户原方法：8 专家 Top6/7/8/9 交叉交集支持度 -> 岭回归 Logistic 适用门 -> 25% 基池 -> 15% 出手", "F15", "INCUMBENT", null, "", "INCUMBENT", seq, null, null, "LEAGUE", System.currentTimeMillis()});
        ModelStoreSU2.joinLeague(db, MEMBER, "XI_INCUMBENT", "用户原方法·多位置多号码交叉交集", KEY, seq);
    }


    public static long prepareNext(DbStore store,Supplier<RuntimeCoordinatorR26.Claim> claims,BudgetTokenSU1 budget,long seq)throws IOException{
        if(!StrategySchemaSU2.ready(store.readable())){claims.get().reason("SU2_SCHEMA_PENDING");return 0;}
        try(Cursor c=store.readable().rawQuery("SELECT 1 FROM su2_live_decision WHERE genome_key=? AND sequence_ordinal=?",new String[]{KEY,String.valueOf(seq)})){if(c.moveToFirst()){claims.get().reason("INCUMBENT_ALREADY_FROZEN; MISSING_OLD_PREFERENCES_NOT_RECONSTRUCTED");return 0;}}
        Models.Draw latest=store.getLatestDraw();if(latest==null||TargetIssueResolver.sequenceOrdinalSU1(latest.issue)!=seq-1){claims.get().reason("INCUMBENT_SUPERSEDED_TARGET");return 0;}
        long deadline=TargetIssueResolver.predrawDeadlineWallR20261008(latest);if(System.currentTimeMillis()>=deadline){claims.get().reason("INCUMBENT_MISSING_AFTER_OPEN");return 0;}
        budget.checkpointOrYield();EventFrameSU1 frame;
        try{frame=EventFrameBuilderSU1.capture(store,TargetIssueResolver.fromSequenceOrdinalSU1(seq));}catch(IllegalArgumentException crossed){if(System.currentTimeMillis()>=deadline){claims.get().reason("INCUMBENT_MISSING_AFTER_OPEN");return 0;}throw crossed;}
        if(System.currentTimeMillis()>=frame.targetOpenTime){claims.get().reason("INCUMBENT_MISSING_AFTER_OPEN");return 0;}
        XiIncumbentSU2.Decision x=XiIncumbentSU2.decideNextFull(frame.history());
        if(x==null){claims.get().reason("INCUMBENT_HISTORY_SHORT");return 0;}
        budget.checkpointOrYield();return commitPrepared(store,claims,frame,x);
    }
    /** Original decision and original preferences commit together, only before the source-bound deadline. */
    static long commitPrepared(DbStore store,Supplier<RuntimeCoordinatorR26.Claim> claims,EventFrameSU1 frame,XiIncumbentSU2.Decision x)throws IOException{
        final byte[] preferences=LegacyPreferenceTableR26.encode(x.table);final String hash=LiveStateR26.hash(preferences);final long seq=TargetIssueResolver.sequenceOrdinalSU1(String.valueOf(frame.targetIssue));
        final ModelLiveSU2.Decision d=new ModelLiveSU2.Decision(seq,x.position,x.mask(),x.act,Double.NaN,x.phat);
        SQLiteDatabase realtime=LegacyResearchScopeR29.realtime(store.readable());
        SQLiteDatabase research=ResearchRouteR29.reader(realtime);
        RuntimeCoordinatorR26.requireFence(research,claims.get());
        // Metadata is research-owned and commits before the separate realtime freeze. It grants no authority.
        seed(store,seq);
        final String claimOwner=claims.get().owner;
        return LegacyResearchScopeR29.realtimeWrite(store,"R26_INCUMBENT_FREEZE",db->{
            long now=System.currentTimeMillis();
            if(ResearchRouteR29.active(db))try(Cursor owner=db.rawQuery("SELECT 1 FROM r29_scheduler_owner WHERE singleton=1 AND session_id=? AND dispatcher='RESEARCH_POOL'",new String[]{claimOwner})){if(!owner.moveToFirst())throw new IllegalStateException("R29_INCUMBENT_STALE_SCHEDULER_OWNER");}
            if(EventFrameBuilderSU1.generation(db)!=frame.sourceGeneration||now>=frame.targetOpenTime-5000){claims.get().reason("INCUMBENT_SOURCE_CHANGED_OR_DEADLINE");return 0L;}
            try(Cursor arrived=db.rawQuery("SELECT 1 FROM draws WHERE issue=? AND dirty=0",new String[]{Long.toString(frame.targetIssue)})){if(arrived.moveToFirst())return 0L;}
            try(Cursor c=db.rawQuery("SELECT 1 FROM su2_live_decision WHERE genome_key=? AND sequence_ordinal=?",new String[]{KEY,String.valueOf(seq)})){if(c.moveToFirst())return 0L;}
            ModelStoreSU2.insertDecision(db,KEY,seq,d,now,frame.targetOpenTime);
            db.execSQL("INSERT INTO r26_incumbent_frozen VALUES(?,?,?,?,?,?,?,?,?)",new Object[]{seq,seq-1,frame.sourceVersionHash,frame.sourceGeneration,ResearchSemanticsGeneratedR967.TRAINING_RULE_SHA256,now,frame.targetOpenTime,preferences,hash});
            return 1L;
        });
    }
    /** Read persisted original preferences after restart; never project them into claimed joint marginals. */
    public static float[][] pendingTable(SQLiteDatabase db,long seq){
        db=LegacyResearchScopeR29.realtime(db);
        if(!RuntimeSchemaR26.ready(db))return null;
        try(Cursor installed=db.rawQuery("SELECT 1 FROM sqlite_master WHERE type='table' AND name='r26_incumbent_frozen'",null)){if(!installed.moveToFirst())return null;}
        try(Cursor c=db.rawQuery("SELECT f.preference_table,f.payload_hash,f.source_generation FROM r26_incumbent_frozen f JOIN su2_live_decision d ON d.genome_key=? AND d.sequence_ordinal=f.target_ordinal AND d.frozen_at=f.frozen_at AND d.target_open_time=f.deadline_at WHERE f.target_ordinal=? AND f.input_through=f.target_ordinal-1 AND f.frozen_at<f.deadline_at AND f.numerical_abi=?",new String[]{KEY,String.valueOf(seq),ResearchSemanticsGeneratedR967.TRAINING_RULE_SHA256})){
            if(!c.moveToFirst()||c.getLong(2)!=EventFrameBuilderSU1.generation(db))return null;byte[] bytes=c.getBlob(0);if(!LiveStateR26.hash(bytes).equals(c.getString(1)))throw new IOException("R26_INCUMBENT_PREFERENCE_HASH");return LegacyPreferenceTableR26.decode(bytes);
        }catch(IOException corrupt){android.util.Log.w("STARCORE_R26","original preference freeze corrupt",corrupt);return null;}
    }
}
