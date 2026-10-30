package com.ethk3.novaonepro.strategyv2;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.*;
import java.io.*;
import java.util.*;
import java.util.function.Supplier;
/** Pretrained private snapshots. P0 only forecasts; feedback training stays in the fenced P2 queue. */
public final class LivePoolR26 {
    private LivePoolR26() {}
    private static final int LIMIT=ShadowAdmissionR26.LIMIT;
    private static final class Row {String policy,text,hash,checkpoint,family;byte[] state;long through,genesis,born,projectionRevision,projectionEpoch;}
    private static Row row(Cursor c){Row r=new Row();r.policy=c.getString(0);r.text=c.getString(1);r.state=c.getBlob(2);r.hash=c.getString(3);r.checkpoint=c.getString(4);r.through=c.getLong(5);r.genesis=c.getLong(6);r.born=c.getLong(7);return r;}
    private static final String COLUMNS="policy_hash,canonical_text,state,state_hash,checkpoint_hash,through_ordinal,genesis_ordinal,born_ordinal";
    private static int changes(SQLiteDatabase db){try(Cursor c=db.rawQuery("SELECT changes()",null)){return c.moveToFirst()?c.getInt(0):0;}}
    /** One complete state is verified off-writer and atomically installed. Existing family parents stay active. */
    public static long publishOne(DbStore store,Supplier<RuntimeCoordinatorR26.Claim> claims,BudgetTokenSU1 budget)throws IOException{
        Models.Draw newest=store.getLatestDraw();if(newest!=null){long target=TargetIssueResolver.sequenceOrdinalSU1(newest.issue)+1;long retired=store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("R26_SHADOW_LIFECYCLE",db->{RuntimeCoordinatorR26.requireFence(db,claims.get());return ShadowAdmissionR26.retireOne(db,target,System.currentTimeMillis());});if(retired>0)return retired;}
        long legacyPublished=LegacyPublicationR26.publishOne(store,claims,budget);if(legacyPublished>0)return legacyPublished;
        if(ShadowAdmissionR26.count(store.readable(),null)>=LIMIT){claims.get().reason("LIVE_SHADOW_CAPACITY_WAIT_FOR_PREREGISTERED_WINDOW; ACTIVE_PARENTS_PRESERVED");return 0;}
        Row prepared=null;String campaign=null,snapshotHash=null;int snapshotSamples=0;long through=0;String abi=ResearchSemanticsGeneratedR967.TRAINING_RULE_SHA256;
        try(Cursor c=store.readable().rawQuery("SELECT a.policy_hash,a.canonical_text,a.state,a.state_hash,a.checkpoint_hash,f.source_through,f.campaign_id,f.snapshot,a.cursor,f.snapshot_hash FROM r26_candidate a JOIN r26_foundry f ON f.campaign_id=a.campaign_id WHERE a.status='DONE' AND f.status IN ('RUNNING','DONE') AND f.numerical_abi=? AND a.canonical_text LIKE '%R26F%' AND NOT EXISTS(SELECT 1 FROM r26_live_model l WHERE l.policy_hash=a.policy_hash) ORDER BY CASE WHEN EXISTS(SELECT 1 FROM r26_live_model l WHERE l.status='ACTIVE' AND instr(a.canonical_text,'R26'||l.family_id)>0) THEN 1 ELSE 0 END,f.created_at DESC,CASE WHEN a.val_n>0 THEN 1.0*a.val_k/a.val_n ELSE -1 END DESC,a.policy_hash",new String[]{abi})){while(c.moveToNext()){
            budget.checkpointOrYield();ModelGenomeSU2 g=ModelGenomeSU2.parse(c.getString(1));if(StructuralProposalR26.control(g)||!g.sink||!g.key().equals(c.getString(0))||!g.legalReadoutR26())continue;String family=ShadowAdmissionR26.family(g);if(family.isEmpty()||!ShadowAdmissionR26.canAdmit(store.readable(),family))continue;
            LiveStateR26 state=LiveStateR26.decode(c.getString(1),CheckpointArtifactsR26.resolve(store,c.getBlob(2)),c.getString(3));if(state.samples()<128)continue;byte[] sourceBytes=c.getBlob(7);if(!LiveStateR26.hash(sourceBytes).equals(c.getString(9))){claims.get().reason("PUBLICATION_SNAPSHOT_HASH_MISMATCH");continue;}FoundryCampaignR26.Snapshot source=FoundryCampaignR26.Snapshot.decode(sourceBytes);boolean birthBound=true;for(ModelGenomeSU2.Member member:g.members)birthBound&=c.getString(9).equals(member.params.get("_birth_snapshot"))&&Long.toString(source.through).equals(member.params.get("_birth_through"));if(state.samples()!=source.draws.length||c.getInt(8)!=source.draws.length||source.through!=c.getLong(5)||!birthBound){claims.get().reason("PUBLICATION_INCOMPLETE_BOOTSTRAP_OR_BINDING");continue;}
            Row r=new Row();r.policy=g.key();r.text=g.text();r.state=c.getBlob(2);r.hash=c.getString(3);r.checkpoint=c.getString(4);r.through=c.getLong(5);r.genesis=source.ordinal[0];r.born=r.through+1;r.family=family;prepared=r;through=r.through;campaign=c.getString(6);snapshotHash=c.getString(9);snapshotSamples=source.draws.length;break;
        }}
        if(prepared==null)return 0;budget.checkpointOrYield();final Row r=prepared;final String birthCampaign=campaign;final long sourceThrough=through;final String sourceHash=snapshotHash;final int sourceSamples=snapshotSamples;
        return store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("R26_LIVE_PUBLICATION",db->{RuntimeCoordinatorR26.requireFence(db,claims.get());long now=System.currentTimeMillis();
            // Freeze birth against the writer's current raw and terminal cursors, never the historical bootstrap.
            try(Cursor latest=LegacyResearchScopeR29.realtime(db).rawQuery("SELECT issue FROM draws WHERE dirty=0 ORDER BY issue DESC LIMIT 1",null)){if(latest.moveToFirst())r.born=Math.max(r.born,TargetIssueResolver.sequenceOrdinalSU1(latest.getString(0))+1);}
            try(Cursor latest=LegacyResearchScopeR29.realtime(db).rawQuery("SELECT max(issue_ordinal) FROM r26_feedback",null)){if(latest.moveToFirst()&&!latest.isNull(0))r.born=Math.max(r.born,latest.getLong(0)+1);}
            try(Cursor capacity=db.rawQuery("SELECT count(*) FROM r26_live_model WHERE status='ACTIVE'",null)){capacity.moveToFirst();if(capacity.getInt(0)>=LIMIT)return 0L;}
            if(!ShadowAdmissionR26.canAdmit(db,r.family))return 0L;
            try(Cursor c=db.rawQuery("SELECT 1 FROM r26_candidate a JOIN r26_foundry f ON f.campaign_id=a.campaign_id WHERE a.campaign_id=? AND a.policy_hash=? AND a.state_hash=? AND a.checkpoint_hash=? AND a.status='DONE' AND f.status IN ('RUNNING','DONE') AND f.numerical_abi=? AND f.source_through=? AND f.snapshot_hash=? AND a.cursor=? AND length(f.snapshot)>0",new String[]{birthCampaign,r.policy,r.hash,r.checkpoint,abi,String.valueOf(sourceThrough),sourceHash,String.valueOf(sourceSamples)})){if(!c.moveToFirst())throw new IllegalStateException("R26_PUBLICATION_CAS");}
            db.execSQL("INSERT INTO r26_live_model(policy_hash,canonical_text,state,state_hash,checkpoint_hash,parent_hash,through_ordinal,genesis_ordinal,born_ordinal,updated_at,family_id) VALUES(?,?,?,?,?,'',?,?,?,?,?)",new Object[]{r.policy,r.text,r.state,r.hash,r.checkpoint,r.through,r.genesis,r.born,now,r.family});
            ModelGenomeSU2 g=ModelGenomeSU2.parse(r.text);ModelEvolutionSU2.Ranked ranked=new ModelEvolutionSU2.Ranked(g,"R26_LIVE","","SHADOW ONLY UNTIL NEW FUTURE QUALIFICATION");ModelStoreSU2.saveGenome(db,ranked,birthCampaign,sourceThrough,0,0,"RESEARCH",now);db.execSQL("UPDATE su2_model_genome SET origin_op='R26_LIVE' WHERE genome_key=?",new Object[]{r.policy});ModelStoreSU2.joinLeague(db,"SU2:"+r.policy,"SU2",r.family+"·R26未来影子·待资格",r.policy,r.born);CertificateSU2.preregister(db,r.policy,r.born);String parent="";try(Cursor origin=db.rawQuery("SELECT parent FROM r26_candidate WHERE campaign_id=? AND policy_hash=?",new String[]{birthCampaign,r.policy})){if(origin.moveToFirst()&&!origin.isNull(0))parent=origin.getString(0);}ShadowAdmissionR26.register(db,r.policy,r.family,r.born,parent);return 1L;});
    }
    public static long trainOne(DbStore store,Supplier<RuntimeCoordinatorR26.Claim> claims,BudgetTokenSU1 budget)throws IOException{
        Models.Draw latest=store.getLatestDraw();if(latest==null)return 0;long latestOrdinal=TargetIssueResolver.sequenceOrdinalSU1(latest.issue);
        Row r=null;String[] selectedInput=null;boolean waitingForTerminal=false;try(Cursor c=store.readable().rawQuery("SELECT "+COLUMNS+" FROM r26_live_model WHERE status='ACTIVE' AND through_ordinal<? ORDER BY updated_at,policy_hash LIMIT ?",new String[]{String.valueOf(latestOrdinal),String.valueOf(LIMIT)})){while(c.moveToNext()){budget.checkpointOrYield();Row candidate=row(c);if(!CurrentPolicyR26.matches(candidate.text,candidate.policy)){r=candidate;break;}String[] next=LegacyReplayR26.nextInput(store,candidate.through);if(next==null)continue;if(Long.parseLong(next[3])>=candidate.born&&next[0].isEmpty()){waitingForTerminal=true;continue;}r=candidate;selectedInput=next;break;}}
        if(r==null){if(waitingForTerminal)claims.get().reason("WAITING_TERMINAL_FEEDBACK");return 0;}
        final Row failed=r;
        try{return trainRow(store,claims,budget,r,selectedInput);}catch(BudgetTokenSU1.Yield yielded){throw yielded;}catch(IOException|IllegalArgumentException failure){
            if(String.valueOf(failure.getMessage()).contains("CAPACITY_WAIT_FOR_RETENTION")){claims.get().reason("CHECKPOINT_CAPACITY_WAIT_FOR_RETENTION");return 0;}
            store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("R26_LIVE_QUARANTINE",db->{RuntimeCoordinatorR26.requireFence(db,claims.get());db.execSQL("UPDATE r26_live_model SET status='BLOCKED_INPUT_OR_STATE' WHERE policy_hash=? AND checkpoint_hash=?",new Object[]{failed.policy,failed.checkpoint});db.execSQL("INSERT OR REPLACE INTO r26_live_failure VALUES(?,?,?,?)",new Object[]{failed.policy,failed.through+1,failure.toString(),System.currentTimeMillis()});return 0;});return 0;
        }
    }
    private static long trainRow(DbStore store,Supplier<RuntimeCoordinatorR26.Claim> claims,BudgetTokenSU1 budget,Row r,String[] selectedInput)throws IOException{
        final Row previous=r;if(!CurrentPolicyR26.matches(r.text,r.policy)){store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("R26_LIVE_ABI_BLOCK",db->{RuntimeCoordinatorR26.requireFence(db,claims.get());db.execSQL("UPDATE r26_live_model SET status='NUMERICAL_ABI_CHANGED' WHERE policy_hash=?",new Object[]{previous.policy});return 0;});return 0;}
        String[] input=selectedInput==null?LegacyReplayR26.nextInput(store,r.through):selectedInput;if(input==null)return 0;if(Long.parseLong(input[3])>=r.born&&input[0].isEmpty()){claims.get().reason("WAITING_TERMINAL_FEEDBACK");return 0;}
        final long ordinal=Long.parseLong(input[3]);final boolean gap=ordinal>r.through+1;String event=input[0],source=input[2];
        Models.Draw actual=store.getDrawByIssue(input[1]);if(actual==null||!source.equals(DrawHashR36.sha256V2(actual)))throw new IOException("R26_LIVE_SOURCE_MISSING_OR_REVISED");int[] draw=actual.numbers.clone();
        PolicyRuntimeR26 learner=PolicyCheckpointR26.restore(CheckpointArtifactsR26.forStore(store),r.text,r.state,r.hash);Map<String,Long> diagnosticCounters=TrainingStepStoreR26.counters(learner.diagnosticHub());budget.checkpointOrYield();if(gap)learner.onGap();
        List<Models.Draw> history=store.getDrawsBeforeIssue(actual.issue,480);long[] issues=new long[history.size()+1],open=new long[issues.length],available=new long[issues.length];int[][] d=new int[issues.length][10];for(int i=0;i<history.size();i++){Models.Draw h=history.get(i);issues[i]=Long.parseLong(h.issue);open[i]=h.openTime;available[i]=h.fetchTime;d[i]=h.numbers.clone();}issues[history.size()]=Long.parseLong(actual.issue);open[history.size()]=actual.openTime;available[history.size()]=actual.fetchTime;d[history.size()]=draw;
        LegacyTeacherOwnerR26.enter(issues,open,available,d,history.size());try{ModelLiveSU2.Decision replayed=learner.forecast();
            try(Cursor c=store.readable().rawQuery("SELECT position,number_mask,act,phat FROM su2_live_decision WHERE genome_key=? AND sequence_ordinal=?",new String[]{r.policy,String.valueOf(ordinal)})){if(c.moveToFirst()&&(c.getInt(0)!=replayed.position||c.getInt(1)!=replayed.mask||c.getInt(2)!=(replayed.act?1:0)||(!c.isNull(3)&&Math.abs(c.getDouble(3)-replayed.phat)>1e-8)))throw new IOException("R26_FROZEN_REPLAY_MISMATCH");}
            learner.learn(draw,ordinal);
        }finally{LegacyTeacherOwnerR26.leave();}
        final ForecastSnapshotR26 nextForecast=new ForecastSnapshotR26(learner.forecast(),learner.table());
        final String eventId=event,sourceHash=source;
        try(CheckpointArtifactsR26.Prepared file=PolicyCheckpointR26.prepare(CheckpointArtifactsR26.forStore(store),learner)){final String stateHash=LiveStateR26.hash(CheckpointArtifactsR26.forStore(store).resolve(file.reference())),checkpoint=HashingSU1.fields("R26_LIVE_CHECKPOINT_V1",r.policy,r.checkpoint,event,stateHash);final List<TrainingStepStoreR26.Pending> trainingSteps=TrainingStepStoreR26.capture(r.policy,learner.diagnosticHub(),"LIVE:"+r.policy,sourceHash,eventId.isEmpty()?"PRE_BIRTH_CAUSAL_DRAW":"CANONICAL_TERMINAL_DRAW",ordinal,Long.parseLong(actual.issue),gap,r.hash,stateHash,diagnosticCounters);store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("R26_LIVE_LEARN_COMMIT",db->{RuntimeCoordinatorR26.requireFence(db,claims.get());
            db.execSQL("UPDATE r26_live_model SET state=?,state_hash=?,checkpoint_hash=?,parent_hash=?,through_ordinal=?,updated_at=? WHERE policy_hash=? AND checkpoint_hash=? AND through_ordinal=? AND status='ACTIVE'",new Object[]{file.reference(),stateHash,checkpoint,previous.checkpoint,ordinal,System.currentTimeMillis(),previous.policy,previous.checkpoint,previous.through});if(changes(db)!=1)throw new IllegalStateException("R26_LIVE_PARENT_CAS");
            if(!eventId.isEmpty())try(Cursor c=LegacyResearchScopeR29.realtime(db).rawQuery("SELECT 1 FROM r26_feedback WHERE event_id=? AND source_hash=?",new String[]{eventId,sourceHash})){if(!c.moveToFirst())throw new IllegalStateException("R26_LIVE_EVENT_CAS");}
            CheckpointManifestR26.record(db,checkpoint,previous.policy,sourceHash,previous.checkpoint,ordinal,file.reference());
            TrainingStepStoreR26.record(db,trainingSteps,checkpoint);
            db.execSQL("UPDATE r26_capability SET state='LIVE_TRAINING_PROGRESS',last_progress=?,reason='Canonical revealed label and full live checkpoint committed' WHERE kind='FAMILY' AND mechanism=(SELECT family_id FROM r26_live_model WHERE policy_hash=?)",new Object[]{System.currentTimeMillis(),previous.policy});
            if(!PreparedForecastStoreR26.install(db,previous.policy,checkpoint,stateHash,ordinal,nextForecast))throw new IllegalStateException("R26_NEXT_FORECAST_CHECKPOINT_CAS");
            if(!eventId.isEmpty())db.execSQL("INSERT INTO r26_consumption VALUES(?,?,?,'REVEALED_LIVE_LABEL')",new Object[]{eventId,previous.policy,checkpoint});
            if(gap){db.execSQL("INSERT OR IGNORE INTO r26_gap_receipt VALUES(?,?,?,?,?,?)",new Object[]{previous.policy,previous.through,ordinal,ordinal-previous.through-1,"UNKNOWN_LABELS; PARAMETERS_RETAINED; TEMPORAL_CONTEXT_REBUILT",System.currentTimeMillis()});db.execSQL("INSERT OR IGNORE INTO r26_certificate_revocation SELECT certificate_id,'DATA_GAP_REQUIRES_NEW_QUALIFICATION',? FROM su2_certificate WHERE genome_key=?",new Object[]{System.currentTimeMillis(),previous.policy});}
            return 0;});}return 1;
    }
    public static int capture(DbStore store,String target){
        SQLiteDatabase realtime=LegacyResearchScopeR29.realtime(store.readable());
        boolean independent=ResearchRouteR29.active(realtime);
        if(independent)LegacyLiveProjectionR29.drain(store);
        SQLiteDatabase research=ResearchRouteR29.reader(realtime);
        if(!RuntimeSchemaR26.ready(research))return 0;
        long seq=TargetIssueResolver.sequenceOrdinalSU1(target);
        EventFrameSU1 frame=EventFrameBuilderSU1.captureWithGapsR26(store,target);
        long epoch=independent?MigrationCopyR29.scalar(research,"SELECT epoch FROM main.r29_research_owner WHERE singleton=1 AND phase='READY'",null):0;
        List<Row> rows=new ArrayList<>();
        try(Cursor c=research.rawQuery("SELECT "+COLUMNS+" FROM r26_live_model WHERE status='ACTIVE' AND through_ordinal=? AND born_ordinal<=? ORDER BY policy_hash LIMIT ?",new String[]{String.valueOf(seq-1),String.valueOf(seq),String.valueOf(LIMIT)})){
            while(c.moveToNext()){Row r=row(c);r.projectionEpoch=epoch;r.projectionRevision=independent?LegacyLiveProjectionR29.revision(research,r.policy):0;rows.add(r);}
        }
        Collections.rotate(rows,rows.isEmpty()?0:(int)(seq%rows.size()));
        Map<Row,ModelLiveSU2.Decision> decisions=new LinkedHashMap<>();Map<String,byte[]> tables=new HashMap<>();long stop=System.nanoTime()+1_000_000_000L;
        BudgetTokenSU1 forecastBudget=new BudgetTokenSU1(1000,3000000,()->SettlementExecutionGateR819.isActive());
        ExecutionBudgetR26.enter(forecastBudget);
        try{for(Row r:rows){
            if(System.nanoTime()>stop||SettlementExecutionGateR819.isActive())break;
            try{
                if(!CurrentPolicyR26.matches(r.text,r.policy))continue;
                ForecastSnapshotR26 forecast=PreparedForecastStoreR26.read(research,r.policy,r.checkpoint,r.hash,r.through);
                if(forecast==null){PolicyRuntimeR26 state=PolicyCheckpointR26.restore(CheckpointArtifactsR26.forStore(store),r.text,r.state,r.hash);forecast=new ForecastSnapshotR26(state.forecast(),state.table());}
                tables.put(r.policy,forecast.tableBytes());decisions.put(r,forecast.decision);
            }catch(BudgetTokenSU1.Yield yield){break;}catch(IOException|IllegalArgumentException bad){android.util.Log.w("STARCORE_R26","live forecast blocked",bad);}
        }}finally{ExecutionBudgetR26.leave();}
        if(decisions.isEmpty())return 0;
        // The realtime transaction reads only realtime source/version references. It never opens or waits for RP.
        return LegacyResearchScopeR29.realtimeWrite(store,"R26_LIVE_FREEZE",db->{
            long now=System.currentTimeMillis();
            if(EventFrameBuilderSU1.generation(db)!=frame.sourceGeneration||now>=frame.targetOpenTime-5000)return 0;
            try(Cursor arrived=db.rawQuery("SELECT 1 FROM draws WHERE issue=? AND dirty=0",new String[]{Long.toString(frame.targetIssue)})){if(arrived.moveToFirst())return 0;}
            int count=0;
            for(Map.Entry<Row,ModelLiveSU2.Decision> e:decisions.entrySet()){
                Row r=e.getKey();
                if(independent){if(!LegacyLiveProjectionR29.matches(db,r.policy,r.checkpoint,r.hash,r.through,r.projectionRevision,r.projectionEpoch))continue;}
                else try(Cursor c=db.rawQuery("SELECT 1 FROM r26_live_model WHERE policy_hash=? AND checkpoint_hash=? AND through_ordinal=? AND status='ACTIVE'",new String[]{r.policy,r.checkpoint,String.valueOf(seq-1)})){if(!c.moveToFirst())continue;}
                try(Cursor existing=db.rawQuery("SELECT 1 FROM r26_frozen WHERE policy_hash=? AND target_ordinal=?",new String[]{r.policy,Long.toString(seq)})){if(existing.moveToFirst())continue;}
                db.execSQL("INSERT INTO r26_frozen VALUES(?,?,?,?,?,?,?,?,?,?)",new Object[]{r.policy,seq,seq-1,r.checkpoint,r.hash,frame.sourceVersionHash,frame.sourceGeneration,now,frame.targetOpenTime,tables.get(r.policy)});
                ModelStoreSU2.insertDecision(db,r.policy,seq,e.getValue(),now,frame.targetOpenTime);count++;
            }
            return count;
        });
    }
    private static byte[] encodeTable(float[][] table)throws IOException{return ForecastSnapshotR26.encodeTable(table);}
    public static float[][] frozenTable(SQLiteDatabase db,String policy,long seq){db=LegacyResearchScopeR29.realtime(db);if(!RuntimeSchemaR26.ready(db))return null;try(Cursor c=db.rawQuery("SELECT probability_table FROM r26_frozen WHERE policy_hash=? AND target_ordinal=? AND input_through<? AND frozen_at<deadline_at",new String[]{policy,String.valueOf(seq),String.valueOf(seq)})){if(!c.moveToFirst())return null;try(DataInputStream in=new DataInputStream(new ByteArrayInputStream(c.getBlob(0)))){float[][] p=new float[10][10];for(int i=0;i<10;i++)for(int j=0;j<10;j++)p[i][j]=in.readFloat();encodeTable(p);return p;}}catch(IOException bad){throw new IllegalStateException("R26_FROZEN_TABLE_CORRUPT",bad);}}
}
