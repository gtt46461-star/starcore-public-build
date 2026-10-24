package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.GZIPOutputStream;

/** Research predictions have their own ledger; they never consume CONFIRM ordinals or create REAL. */
public final class LiveShadowLoopSU1 {
    private LiveShadowLoopSU1() {}
    public static long sequence(String issue){return TargetIssueResolver.sequenceOrdinalSU1(issue);}
    private static String packed(EventFrameSU1 frame){
        try{ByteArrayOutputStream bytes=new ByteArrayOutputStream();try(GZIPOutputStream gzip=new GZIPOutputStream(bytes)){gzip.write(frame.payload().getBytes(StandardCharsets.UTF_8));}
            return Base64.getEncoder().encodeToString(bytes.toByteArray());
        }catch(IOException impossible){throw new IllegalStateException(impossible);}
    }
    private static void bootstrap(DbStore store,EventFrameSU1 frame){
        if(StrategyRepositorySU1.exists(store.readable(),"SELECT 1 FROM su1_runtime_program WHERE kernel_hash=?",TrainingCodeIdentityR964.SHA256))return;
        store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("SU1_SEED_POOL",db->{
            if(StrategyRepositorySU1.exists(db,"SELECT 1 FROM su1_runtime_program WHERE kernel_hash=?",TrainingCodeIdentityR964.SHA256))return 0;
            long now=System.currentTimeMillis(),through=sequence(String.valueOf(frame.targetIssue))-1;
            String campaign=StrategyRepositorySU1.beginCampaign(db,"LIVE_REFRESH",through,"{\"kind\":\"BOOTSTRAP_EXECUTABLE_PROGRAMS\",\"revision\":2,\"kernel\":\""+TrainingCodeIdentityR964.SHA256+"\"}",now);
            for(int e=0;e<8;e++)StrategyRepositorySU1.candidate(db,"EXPERT("+e+")",1,false,"EXPERT_MIX",null,campaign,through,frame.sourceVersionHash,-1,now);
            for(int off=0;off<10;off++)StrategyRepositorySU1.candidate(db,"XT("+off+")",1,false,"MATRIX",null,campaign,through,frame.sourceVersionHash,-1,now);
            for(int seed=0;seed<2;seed++)StrategyRepositorySU1.candidate(db,"GDEEP(2,4,"+seed+")",1,false,"DEEP",null,campaign,through,frame.sourceVersionHash,-1,now);
            db.execSQL("UPDATE su1_campaign SET status='DONE',finished_at=?,programs_evaluated=0 WHERE campaign_id=? AND status='RUNNING'",new Object[]{now,campaign});
            db.execSQL("UPDATE su1_runtime_control SET mode='SHADOW',contract_hash=?,updated_at=?,reason='SU1_RESEARCH_ENABLED_MAINLINE_CLOSED' WHERE singleton=1 AND mode='DISABLED'",new Object[]{StrategyRepositorySU1.ACTION,now});
            return 20;
        });
    }
    private static final class Prepared {
        final StrategyRepositorySU1.Member member;final int position,mask;final String outcome,detail;
        Prepared(StrategyRepositorySU1.Member m,int p,int mask,String outcome,String detail){member=m;position=p;this.mask=mask;this.outcome=outcome;this.detail=detail;}
    }
    public static int capture(DbStore store,String target){
        if(!StrategySchemaSU1.ready(store.readable()))return 0;
        if(!ArchiveStoreSU1.canCreate(store.readable()))return 0;
        try(Cursor c=store.readable().rawQuery("SELECT mode FROM su1_runtime_control WHERE singleton=1",null)){if(c.moveToFirst()&&("RESEARCH".equals(c.getString(0))||"DISABLED".equals(c.getString(0))&&StrategyRepositorySU1.count(store.readable(),"SELECT count(*) FROM su1_research_pool")>0))return 0;}
        KernelTransitionSU1.ensure(store,sequence(target));EventFrameSU1 frame=EventFrameBuilderSU1.capture(store,target);bootstrap(store,frame);
        long seq=sequence(target);List<StrategyRepositorySU1.Member> members=StrategyRepositorySU1.active(store.readable());
        List<Prepared> prepared=new ArrayList<>();int[][] history=frame.history();int t=history.length;
        BudgetTokenSU1 budget=new BudgetTokenSU1(250,300000,SettlementExecutionGateR819::isActive);
        ScoreHistoryCacheSU1 cache=new ScoreHistoryCacheSU1(2048,frame);
        try(AtomLiftSU1 atoms=new AtomLiftSU1(history)){
            for(int i=0;i<members.size();i++){
                StrategyRepositorySU1.Member m=members.get((i+(int)(seq%members.size()))%members.size());
                if(StrategyRepositorySU1.exists(store.readable(),"SELECT 1 FROM su1_live_attempt WHERE strategy_id=? AND sequence_ordinal=?",m.id,String.valueOf(seq)))continue;
                try{
                    if(!budget.tryConsume(1000)){prepared.add(new Prepared(m,0,0,"BUDGET_SKIP","DEADLINE_BUDGET"));continue;}
                    ProgramTreeSU1.Node program=ProgramTreeSU1.parse(m.text);Map<String,ProgramTreeSU1.Fitted> fitted=ModelAssetsSU1.load(store.readable(),m.text,frame);Map<String,ProgramTreeSU1.GraphFitted> graph=ModelAssetsSU1.loadGraph(store.readable(),m.text,frame);double[][] scores=program.score(new ProgramTreeSU1.Context(history,t,atoms,cache,budget).withFitted(fitted).withGraphFitted(graph));
                    int[] action=ProgramTreeSU1.action(scores);boolean act=true;
                    if(m.coverage<.999){
                        int block=t-(int)(seq%50),lo=Math.max(0,block-720);double[] gate=new double[block];
                        if(block<=lo)act=false;
                        else{for(int u=lo;u<block;u++){double[][] prior=program.score(new ProgramTreeSU1.Context(history,u,atoms,cache,budget).withFitted(fitted).withGraphFitted(graph));int[] a=ProgramTreeSU1.action(prior);gate[u]=HistoricalReplayEngineSU1.gateValue(prior,a[0],m.margin);}
                            act=HistoricalReplayEngineSU1.gateValue(scores,action[0],m.margin)>=HistoricalReplayEngineSU1.quantileLinear(gate,lo,block,1-m.coverage);}
                    }
                    int k3mask=(1<<(action[1]-1))|(1<<(action[2]-1))|(1<<(action[3]-1));
                    // R13-P06: a GATE_SKIP period keeps its would-be action, so it is settled and trains the gate too
                    prepared.add(new Prepared(m,action[0],k3mask,act?"FROZEN":"GATE_SKIP","POS="+action[0]+"|MASK="+k3mask+"|RAW_K3_HEX="+Double.toHexString(HistoricalReplayEngineSU1.positionMasses(scores)[action[0]-1])));
                }catch(BudgetTokenSU1.Yield yield){prepared.add(new Prepared(m,0,0,"BUDGET_SKIP",yield.getMessage()));}
                catch(IllegalArgumentException failure){prepared.add(new Prepared(m,0,0,"COMPUTE_FAILED",failure.toString()));}
            }
        }
        if(prepared.isEmpty())return 0;SnapshotPayloadSU1 packed=SnapshotPayloadSU1.prepare(frame);
        return store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("SU1_LIVE_FREEZE",db->{
            if(!EventFrameBuilderSU1.unchanged(store,frame))return 0;
            String snapshot=frame.contentHash;
            try(Cursor c=db.rawQuery("SELECT t.snapshot_hash,s.source_version_hash FROM su1_live_target t JOIN su1_snapshot s ON s.snapshot_hash=t.snapshot_hash WHERE t.sequence_ordinal=?",new String[]{String.valueOf(seq)})){
                if(c.moveToFirst()){if(!frame.sourceVersionHash.equals(c.getString(1)))throw new IllegalStateException("SU1_FROZEN_SOURCE_REVISION_CONFLICT");snapshot=c.getString(0);}
                else{StrategyRepositorySU1.snapshot(db,frame,packed);db.execSQL("INSERT INTO su1_live_target(sequence_ordinal,target_issue,target_open_time,snapshot_hash) VALUES(?,?,?,?)",new Object[]{seq,target,frame.targetOpenTime,snapshot});}
            }
            int count=0;long now=System.currentTimeMillis();if(now>=frame.targetOpenTime)return 0;
            StrategyRepositorySU1.relationAudit(db,snapshot,cache.relationAudit(),now);
            for(Prepared p:prepared){
                if(StrategyRepositorySU1.exists(db,"SELECT 1 FROM su1_live_attempt WHERE strategy_id=? AND sequence_ordinal=?",p.member.id,String.valueOf(seq)))continue;
                if(!StrategyRepositorySU1.exists(db,"SELECT 1 FROM su1_research_pool WHERE strategy_id=? AND state='ACTIVE' AND born_sequence<=?",p.member.id,String.valueOf(seq)))continue;
                if(p.outcome.equals("FROZEN")){db.execSQL("INSERT INTO su1_live_shadow(strategy_id,sequence_ordinal,day_key,position,number_mask,captured_at,target_open_time) VALUES(?,?,?,?,?,?,?)",new Object[]{p.member.id,seq,Integer.parseInt(target.substring(0,8)),p.position,p.mask,now,frame.targetOpenTime});count++;}
                db.execSQL("INSERT INTO su1_live_attempt VALUES(?,?,?,?,?)",new Object[]{p.member.id,seq,p.outcome,p.detail,now});
            }
            return count;
        });
    }
    /** Only called inside the original post-COMPLETED owner transaction, or its verified recovery. */
    public static int settleWithin(SQLiteDatabase db,Models.Draw draw,String settlementHash,long completedAt){
        if(!StrategySchemaSU1.ready(db))return 0;
        requireCompleted(db,draw,settlementHash,completedAt);
        long seq=sequence(draw.issue);
        try(Cursor prior=db.rawQuery("SELECT legacy_settlement_hash,completed_at FROM su1_live_target WHERE sequence_ordinal=?",new String[]{String.valueOf(seq)})){
            if(!prior.moveToFirst())return 0;
            if(!prior.isNull(0)){if(!settlementHash.equals(prior.getString(0))||completedAt!=prior.getLong(1))throw new IllegalStateException("SU1_SETTLEMENT_CONFLICT");return 0;}
        }
        Object[] args=new Object[13];for(int p=0;p<10;p++)args[p]=draw.getNumberAtPosition(p+1);args[10]=completedAt;args[11]=seq;args[12]=completedAt;
        db.execSQL("UPDATE su1_live_shadow SET hit=(number_mask>>(CASE position WHEN 1 THEN ? WHEN 2 THEN ? WHEN 3 THEN ? WHEN 4 THEN ? WHEN 5 THEN ? WHEN 6 THEN ? WHEN 7 THEN ? WHEN 8 THEN ? WHEN 9 THEN ? ELSE ? END-1))&1,settled_at=? WHERE sequence_ordinal=? AND hit IS NULL AND target_open_time<=?",args);
        int n=StrategyRepositorySU1.count(db,"SELECT changes()");
        DirectorLiveFeedbackSU1.settled(db,seq);
        db.execSQL("UPDATE su1_live_target SET legacy_settlement_hash=?,completed_at=? WHERE sequence_ordinal=? AND completed_at IS NULL",new Object[]{settlementHash,completedAt,seq});
        return n;
    }
    public static void requireCompleted(SQLiteDatabase db,Models.Draw draw,String settlementHash,long completedAt){
        if(!db.inTransaction())throw new IllegalStateException("SU1_SETTLEMENT_OWNER_TRANSACTION_REQUIRED");
        if(draw==null||!draw.isValid()||draw.dirty!=0)throw new IllegalArgumentException("SU1_CANONICAL_DRAW_REQUIRED");
        try(Cursor terminal=db.rawQuery("SELECT status,settlement_hash,completed_at,draw_hash FROM "+DbHelper.TABLE_R36_SETTLEMENT_TASK+" WHERE target_issue=?",new String[]{draw.issue})){
            if(!terminal.moveToFirst()||!"COMPLETED".equals(terminal.getString(0))||!settlementHash.equals(terminal.getString(1))||completedAt!=terminal.getLong(2)||!DrawHashR36.matchesPersisted(terminal.getString(3),DrawHashR36.LEGACY_VERSION,draw))throw new IllegalStateException("SU1_CANONICAL_COMPLETED_REQUIRED");
        }
        try(Cursor canonical=db.rawQuery("SELECT dirty,open_time,n1,n2,n3,n4,n5,n6,n7,n8,n9,n10 FROM "+DrawDependencyInputCP12.SOURCE+" WHERE issue=?",new String[]{draw.issue})){
            if(!canonical.moveToFirst()||canonical.getInt(0)!=0||canonical.getLong(1)!=draw.openTime)throw new IllegalStateException("SU1_CANONICAL_DRAW_MISMATCH");
            for(int p=0;p<10;p++)if(canonical.getInt(p+2)!=draw.getNumberAtPosition(p+1))throw new IllegalStateException("SU1_CANONICAL_DRAW_MISMATCH");
        }
    }
    public static int recover(DbStore store,int limit){
        if(!StrategySchemaSU1.ready(store.readable()))return 0;List<String[]> pending=new ArrayList<>();
        try(Cursor c=store.readable().rawQuery("SELECT t.target_issue,l.settlement_hash,l.completed_at FROM su1_live_target t JOIN "+DbHelper.TABLE_R36_SETTLEMENT_TASK+" l ON l.target_issue=t.target_issue WHERE t.completed_at IS NULL AND l.status='COMPLETED' ORDER BY t.sequence_ordinal LIMIT ?",new String[]{String.valueOf(Math.max(1,Math.min(32,limit)))})){while(c.moveToNext())pending.add(new String[]{c.getString(0),c.getString(1),c.getString(2)});}
        int total=0;for(String[] r:pending){Models.Draw d=store.getDrawByIssue(r[0]);if(d==null||!d.isValid()||d.dirty!=0)continue;total+=store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("SU1_LIVE_RECOVER",db->settleWithin(db,d,r[1],Long.parseLong(r[2])));}
        return total;
    }
    public static void closeDays(SQLiteDatabase db,int beforeDay){
        db.execSQL("INSERT INTO su1_live_daily(strategy_id,day_key,actions,hits,periods_observed,closed_at) SELECT s.strategy_id,s.day_key,count(*),sum(s.hit),(SELECT count(*) FROM su1_live_attempt a JOIN su1_live_target t ON t.sequence_ordinal=a.sequence_ordinal WHERE a.strategy_id=s.strategy_id AND substr(t.target_issue,1,8)=CAST(s.day_key AS TEXT)),? FROM su1_live_shadow s WHERE s.day_key<? AND NOT EXISTS(SELECT 1 FROM su1_live_shadow p WHERE p.strategy_id=s.strategy_id AND p.day_key=s.day_key AND p.hit IS NULL) AND NOT EXISTS(SELECT 1 FROM su1_live_daily d WHERE d.strategy_id=s.strategy_id AND d.day_key=s.day_key) GROUP BY s.strategy_id,s.day_key",new Object[]{System.currentTimeMillis(),beforeDay});
        List<String> weak=new ArrayList<>();
        try(Cursor c=db.rawQuery("SELECT strategy_id,sum(actions),sum(hits) FROM su1_live_daily GROUP BY strategy_id HAVING sum(actions)>=200",null)){while(c.moveToNext())if(FoldStabilityFitnessSU1.wilsonLower(c.getInt(2),c.getInt(1))<.27)weak.add(c.getString(0));}
        for(String id:weak)db.execSQL("UPDATE su1_research_pool SET state='RETIRED',retired_sequence=max(born_sequence,(SELECT coalesce(max(sequence_ordinal),born_sequence) FROM su1_live_target)),retire_reason='LIVE_WILSON_BELOW_027' WHERE strategy_id=? AND state='ACTIVE' AND NOT EXISTS(SELECT 1 FROM su1_sealed_plan z WHERE z.strategy_id=su1_research_pool.strategy_id AND NOT EXISTS(SELECT 1 FROM su1_sealed_result q WHERE q.plan_campaign=z.campaign_id))",new Object[]{id});
    }
}
