package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.*;
import java.io.*;
import java.util.*;
import java.util.function.Supplier;
import org.json.*;

/** Legacy creation/bootstrap/catch-up is P2 work. P0 reads only the fully committed cohort. */
public final class LegacyReplayR26 {
    private LegacyReplayR26() {}
    private static volatile Prepared active;
    private static LegacyReplayStateR26 working;
    private static String workingCheckpoint="";
    private static final Object FORECAST_LOCK=new Object();
    private static final class Row {
        String roster,source,checkpoint,parent,stateHash,abi,status;byte[] snapshot,state;long sourceThrough,through,genesis;int cursor;
    }
    private static final class Prepared {
        final String checkpoint,roster;final LegacyReplayStateR26 state;
        Prepared(Row row,LegacyReplayStateR26 state){checkpoint=row.checkpoint;roster=row.roster;this.state=state;}
    }
    private static Row row(SQLiteDatabase db){
        try(Cursor c=db.rawQuery("SELECT roster_hash,snapshot_hash,snapshot,source_through,genesis_ordinal,through_ordinal,cursor,state,state_hash,checkpoint_hash,parent_hash,numerical_abi,status FROM r26_legacy_runtime WHERE singleton=1",null)){
            if(!c.moveToFirst())return null;Row r=new Row();r.roster=c.getString(0);r.source=c.getString(1);r.snapshot=c.getBlob(2);r.sourceThrough=c.getLong(3);r.genesis=c.getLong(4);r.through=c.getLong(5);r.cursor=c.getInt(6);r.state=c.getBlob(7);r.stateHash=c.getString(8);r.checkpoint=c.getString(9);r.parent=c.getString(10);r.abi=c.getString(11);r.status=c.getString(12);return r;
        }
    }
    private static String roster(List<String> canonical){return HashingSU1.fields("R26_LEGACY_ROSTER_V1",ResearchSemanticsGeneratedR967.TRAINING_RULE_SHA256,PolicyIdentityR26.canonical(canonical));}
    private static LegacyReplayStateR26 decode(DbStore store,Row r)throws IOException {
        LegacyReplayStateR26 state=CheckpointArtifactsR26.forStore(store).restoreLegacy(r.state,r.stateHash);
        if(state.through()!=r.through||state.genesis!=r.genesis)throw new IOException("R26_LEGACY_CHECKPOINT_CURSOR");return state;
    }
    static void clearWorking(){working=null;workingCheckpoint="";}
    public static long step(DbStore store,Supplier<RuntimeCoordinatorR26.Claim> claims,BudgetTokenSU1 budget)throws IOException,JSONException {
        Models.Draw latest=store.getLatestDraw();if(latest==null)return 0;long latestOrdinal=TargetIssueResolver.sequenceOrdinalSU1(latest.issue);Row r=row(store.readable());
        List<String> wanted=ModelLiveLoopSU2.liveGenomes(store.readable());String desired=roster(wanted),abi=ResearchSemanticsGeneratedR967.TRAINING_RULE_SHA256;
        if(wanted.isEmpty()){active=null;working=null;return 0;}
        if(r!=null&&!abi.equals(r.abi)){return adoptAbi(store,claims,r,abi,wanted);}
        if(r!=null&&r.status.equals("EXPIRED_RETENTION"))return resumeExpired(store,claims,budget);
        if(r==null||(!desired.equals(r.roster)&&r.status.equals("LIVE"))){
            FoundryCampaignR26.Snapshot snapshot=FoundryCampaignR26.capture(store);if(snapshot==null)return 0;budget.checkpointOrYield();
            ModelLiveLoopSU2.Runtime imported=r==null?ModelLiveLoopSU2.restore(store.readable(),wanted,snapshot.draws,snapshot.ordinal[0]):null;
            LegacyReplayStateR26 state=imported!=null&&imported.through<=latestOrdinal?LegacyReplayStateR26.imported(wanted,imported.genesisSeq,imported.through,imported.hub,imported.lives):new LegacyReplayStateR26(wanted,snapshot.ordinal[0]);boolean restored=imported!=null&&imported.through<=latestOrdinal;String source=LiveStateR26.hash(snapshot.encode());String previous=r==null?"":r.checkpoint;
            try(CheckpointArtifactsR26.Prepared file=CheckpointArtifactsR26.forStore(store).prepareLegacy(state)){
                String hash=LiveStateR26.hash(CheckpointArtifactsR26.forStore(store).resolve(file.reference())),checkpoint=HashingSU1.fields("R26_LEGACY_CHECKPOINT_V1",desired,source,"0",hash);
                final byte[] snapshotBytes=snapshot.encode();final String old=previous;
                store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("R26_LEGACY_BOOTSTRAP_BIRTH",db->{RuntimeCoordinatorR26.requireFence(db,claims.get());Row current=row(db);if(!old.equals(current==null?"":current.checkpoint))throw new IllegalStateException("R26_LEGACY_BIRTH_CAS");
                    db.execSQL("INSERT OR REPLACE INTO r26_legacy_runtime(singleton,roster_hash,snapshot_hash,snapshot,source_through,genesis_ordinal,through_ordinal,cursor,state,state_hash,checkpoint_hash,parent_hash,numerical_abi,status,updated_at) VALUES(1,?,?,?,?,?,?,0,?,?,?,'',?,?,?)",new Object[]{desired,source,restored?new byte[0]:snapshotBytes,snapshot.through,state.genesis,state.through(),file.reference(),hash,checkpoint,abi,restored?"LIVE":"BOOTSTRAP",System.currentTimeMillis()});
                    CheckpointManifestR26.record(db,checkpoint,"LEGACY:"+desired,source,"",state.through(),file.reference());return 0;});
                working=state;workingCheckpoint=checkpoint;return 1;
            }
        }
        if(!abi.equals(r.abi)||r.status.startsWith("BLOCKED"))return 0;
        final Row previous=r;
        if(r.status.equals("BOOTSTRAP")&&latestOrdinal-FoundryCampaignR26.Snapshot.decode(r.snapshot).ordinal[0]>=5000){store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("R26_LEGACY_BOOTSTRAP_EXPIRE",db->{RuntimeCoordinatorR26.requireFence(db,claims.get());db.execSQL("UPDATE r26_legacy_runtime SET status='EXPIRED_RETENTION',snapshot=X'' WHERE singleton=1 AND checkpoint_hash=?",new Object[]{previous.checkpoint});return 0;});working=null;return 1;}
        LegacyReplayStateR26 state=working!=null&&workingCheckpoint.equals(r.checkpoint)?working:decode(store,r);
        FoundryCampaignR26.Snapshot snapshot=r.status.equals("BOOTSTRAP")?FoundryCampaignR26.Snapshot.decode(r.snapshot):null;
        int before=r.cursor,after=before;long oldThrough=r.through,newThrough=oldThrough;CheckpointArtifactsR26.Prepared lastGood=null;List<String[]> events=new ArrayList<>();
        boolean interrupted=false;long stop=budget.activeNanos()+140_000_000L;
        try{
            while(after-before<1&&budget.activeNanos()<stop){budget.checkpointOrYield();int[] draw;long ordinal;
                if(snapshot!=null&&after<snapshot.draws.length){draw=snapshot.draws[after];ordinal=snapshot.ordinal[after];}
                else{
                    String[] event=nextInput(store,newThrough);if(event==null)break;if(event[0].isEmpty()){claims.get().reason("WAITING_TERMINAL_FEEDBACK");break;}ordinal=Long.parseLong(event[3]);Models.Draw actual=store.getDrawByIssue(event[1]);if(actual==null||!DrawHashR36.sha256V2(actual).equals(event[2]))throw new IOException("R26_LEGACY_EVENT_SOURCE");draw=actual.numbers.clone();if(!event[0].isEmpty())events.add(event);
                }
                state.advance(draw,ordinal);lastGood=CheckpointArtifactsR26.forStore(store).prepareLegacy(state);after++;newThrough=ordinal;
                if(snapshot!=null&&after==snapshot.draws.length)break;
            }
        }catch(BudgetTokenSU1.Yield yielded){working=null;interrupted=true;}
        catch(IOException|IllegalArgumentException invalid){working=null;if(String.valueOf(invalid.getMessage()).contains("CAPACITY_WAIT_FOR_RETENTION")){claims.get().reason("CHECKPOINT_CAPACITY_WAIT_FOR_RETENTION");return 0;}store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("R26_LEGACY_BLOCK_INPUT",db->{RuntimeCoordinatorR26.requireFence(db,claims.get());db.execSQL("UPDATE r26_legacy_runtime SET status='BLOCKED_INPUT_OR_STATE' WHERE singleton=1 AND checkpoint_hash=?",new Object[]{previous.checkpoint});db.execSQL("INSERT OR REPLACE INTO r26_live_failure VALUES(?,?,?,?)",new Object[]{"LEGACY:"+previous.roster,previous.through+1,invalid.toString(),System.currentTimeMillis()});return 0;});return 0;}
        if(lastGood==null){working=interrupted?null:state;workingCheckpoint=r.checkpoint;if(r.status.equals("LIVE")&&r.through==latestOrdinal)activate(store,r);return 0;}
        // Discard any partially updated sample after the last complete encoding.
        final int committedCursor=after;final long committedThrough=newThrough;final boolean bootDone=snapshot!=null&&after>=snapshot.draws.length;
        try(CheckpointArtifactsR26.Prepared file=lastGood){
            final String hash=LiveStateR26.hash(CheckpointArtifactsR26.forStore(store).resolve(file.reference())),checkpoint=HashingSU1.fields("R26_LEGACY_CHECKPOINT_V1",r.roster,r.source,r.checkpoint,String.valueOf(committedThrough),hash);
            store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("R26_LEGACY_REPLAY_COMMIT",db->{RuntimeCoordinatorR26.requireFence(db,claims.get());
                db.execSQL("UPDATE r26_legacy_runtime SET cursor=?,through_ordinal=?,state=?,state_hash=?,parent_hash=?,checkpoint_hash=?,status=?,snapshot=CASE WHEN ?=1 THEN X'' ELSE snapshot END,updated_at=? WHERE singleton=1 AND checkpoint_hash=? AND through_ordinal=?",new Object[]{committedCursor,committedThrough,file.reference(),hash,previous.checkpoint,checkpoint,bootDone?"LIVE":previous.status,bootDone?1:0,System.currentTimeMillis(),previous.checkpoint,oldThrough});
                try(Cursor c=db.rawQuery("SELECT changes()",null)){if(!c.moveToFirst()||c.getInt(0)!=1)throw new IllegalStateException("R26_LEGACY_PARENT_CAS");}
                CheckpointManifestR26.record(db,checkpoint,"LEGACY:"+previous.roster,previous.source,previous.checkpoint,committedThrough,file.reference());
                for(String[] event:events){long ordinal=Long.parseLong(event[3]);if(ordinal>committedThrough)continue;try(Cursor c=db.rawQuery("SELECT 1 FROM r26_feedback WHERE event_id=? AND source_hash=?",new String[]{event[0],event[2]})){if(!c.moveToFirst())throw new IllegalStateException("R26_LEGACY_EVENT_CAS");}for(String text:state.canonical)db.execSQL("INSERT OR IGNORE INTO r26_consumption VALUES(?,?,?,'REVEALED_LIVE_LABEL')",new Object[]{event[0],LegacyReplayStateR26.key(text),checkpoint});}
                return 0;});
            working=state;workingCheckpoint=checkpoint;
            Row committed=row(store.readable());if(committed!=null&&committed.status.equals("LIVE")&&committed.through==latestOrdinal)activate(store,committed);
            return after-before;
        }
    }
    /** R27-012 (docs/08): a different source-tree ABI no longer blocks the learned legacy kernels forever. The R27
     * semantic check (LegacyContinuityR27, run on the research pool) compares the golden-set trajectory of the legacy
     * kernels under the previous and the current build. EQUIVALENT → the same runtime continues; CHANGED (including
     * "no prior fingerprint", e.g. the first upgrade from 1713d11) → ABI_REBIRTH: weights and optimizer state are
     * inherited, the runtime is re-keyed to the current ABI, and no qualification or FUTURE evidence is carried over
     * (the old cohort stays archived). Until the check has run, the task waits with a DEPENDENCY reason. */
    private static long adoptAbi(DbStore store,Supplier<RuntimeCoordinatorR26.Claim> claims,Row r,String abi,List<String> wanted){
        String decision=LegacyContinuityR27.decision(store.readable(),r.abi,abi);
        if(decision==null){claims.get().reason("WAITING_R27_SEMANTIC_ABI_CHECK:old="+r.abi.substring(0,12)+",new="+abi.substring(0,12));return 0;}
        final String desired=roster(wanted.isEmpty()?decodeCanonical(store,r):wanted);
        store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("R27_LEGACY_ABI_CONTINUITY",db->{RuntimeCoordinatorR26.requireFence(db,claims.get());
            db.execSQL("UPDATE r26_legacy_runtime SET numerical_abi=?,roster_hash=?,status=CASE WHEN status LIKE 'BLOCKED%' THEN 'LIVE' ELSE status END WHERE singleton=1 AND checkpoint_hash=? AND numerical_abi=?",new Object[]{abi,desired,r.checkpoint,r.abi});
            // Re-key the retained checkpoint manifest to the continuing roster so warm resume keeps the weights.
            db.execSQL("UPDATE r26_checkpoint_artifact SET policy_hash=? WHERE checkpoint_hash=? AND policy_hash=?",new Object[]{"LEGACY:"+desired,r.checkpoint,"LEGACY:"+r.roster});
            LegacyContinuityR27.recordMintedRoster(db,r.checkpoint,r.roster);
            if(!LegacyContinuityR27.EQUIVALENT.equals(decision))LegacyResearchTransferR29.lineage(db,"LEGACY:"+desired,"LEGACY:"+r.roster,System.currentTimeMillis());
            return 0;});
        working=null;active=null;claims.get().reason("COOPERATIVE_R27_ABI_"+decision+"_WEIGHTS_CONTINUE_NO_EVIDENCE_TRANSFER");return 1;
    }
    private static List<String> decodeCanonical(DbStore store,Row r){try{return decode(store,r).canonical;}catch(IOException e){throw new IllegalStateException("R27_LEGACY_ABI_DECODE",e);}}
    private static void preserveBlocked(DbStore store,Supplier<RuntimeCoordinatorR26.Claim> claims,Row prior,String status){
        if(prior.status.equals(status))return;
        store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("R26_LEGACY_PRESERVE_BLOCKED",db->{RuntimeCoordinatorR26.requireFence(db,claims.get());db.execSQL("UPDATE r26_legacy_runtime SET status=? WHERE singleton=1 AND checkpoint_hash=? AND state_hash=?",new Object[]{status,prior.checkpoint,prior.stateHash});return 0;});
        working=null;active=null;claims.get().reason(status+":FULL_LEARNED_CHECKPOINT_RETAINED");
    }
    /** Resume an unpublished bootstrap after its raw window expires. Keep every learned
     * model/optimizer and lifetime clock; clear raw/gate/calibration context and use only
     * retained labels beyond ALL prior bootstrap exposure. No qualification is copied. */
    static long resumeExpired(DbStore store,Supplier<RuntimeCoordinatorR26.Claim> claims,BudgetTokenSU1 budget)throws IOException {
        Row prior=row(store.readable());if(prior==null||!prior.status.equals("EXPIRED_RETENTION"))return 0;
        if(!ResearchSemanticsGeneratedR967.TRAINING_RULE_SHA256.equals(prior.abi)){List<String> wanted=ModelLiveLoopSU2.liveGenomes(store.readable());return adoptAbi(store,claims,prior,ResearchSemanticsGeneratedR967.TRAINING_RULE_SHA256,wanted);}
        budget.checkpointOrYield();LegacyReplayStateR26 state;FoundryCampaignR26.Snapshot source;
        try{
            // R27-012: a checkpoint minted before an ABI re-key carries the roster it was minted under.
            String idRoster=LegacyContinuityR27.mintedRoster(store,prior.checkpoint,prior.roster);
            String expected=prior.parent.isEmpty()?HashingSU1.fields("R26_LEGACY_CHECKPOINT_V1",idRoster,prior.source,"0",prior.stateHash):HashingSU1.fields("R26_LEGACY_CHECKPOINT_V1",idRoster,prior.source,prior.parent,Long.toString(prior.through),prior.stateHash);
            if(!expected.equals(prior.checkpoint))throw new IOException("R26_LEGACY_WARM_CHECKPOINT_IDENTITY");
            try(Cursor manifest=store.readable().rawQuery("SELECT artifact_hash,policy_hash,source_hash,parent_hash,trained_through FROM r26_checkpoint_artifact WHERE checkpoint_hash=?",new String[]{prior.checkpoint})){
                if(!manifest.moveToFirst()||!CheckpointArtifactsR26.referenceHash(prior.state).equals(manifest.getString(0))||!("LEGACY:"+prior.roster).equals(manifest.getString(1))||!prior.source.equals(manifest.getString(2))||!prior.parent.equals(manifest.getString(3))||prior.through!=manifest.getLong(4))throw new IOException("R26_LEGACY_WARM_MANIFEST");
            }
            state=decode(store,prior);if(!roster(state.canonical).equals(prior.roster))throw new IOException("R26_LEGACY_WARM_ROSTER");
            source=WarmCandidateR26.retainedSuffix(FoundryCampaignR26.capture(store),Math.max(prior.sourceThrough,prior.through));
            if(source==null){claims.get().reason("LEGACY_WAITING_64_RETAINED_LABELS_AFTER_ALL_PARENT_EXPOSURE; WEIGHTS_PRESERVED");return 0;}
            state.onGap();
        }catch(IOException|IllegalArgumentException invalid){preserveBlocked(store,claims,prior,"BLOCKED_WARM_STATE_PRESERVED");claims.get().reason("LEGACY_WARM_STATE_PRESERVED:"+invalid.getClass().getSimpleName()+":"+String.valueOf(invalid.getMessage()));return 0;}
        byte[] snapshot=source.encode();String sourceHash=LiveStateR26.hash(snapshot);budget.checkpointOrYield();
        try(CheckpointArtifactsR26.Prepared file=CheckpointArtifactsR26.forStore(store).prepareLegacy(state)){
            String hash=LiveStateR26.hash(CheckpointArtifactsR26.forStore(store).resolve(file.reference())),checkpoint=HashingSU1.fields("R26_LEGACY_CHECKPOINT_V1",prior.roster,sourceHash,prior.checkpoint,Long.toString(prior.through),hash);
            store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("R26_LEGACY_WARM_BOOTSTRAP",db->{RuntimeCoordinatorR26.requireFence(db,claims.get());
                try(Cursor latest=db.rawQuery("SELECT issue FROM draws WHERE dirty=0 ORDER BY issue DESC LIMIT 1",null)){if(!latest.moveToFirst()||TargetIssueResolver.sequenceOrdinalSU1(latest.getString(0))-source.ordinal[0]>=5000)throw new IllegalStateException("R26_LEGACY_WARM_SOURCE_EXPIRED");}
                db.execSQL("UPDATE r26_legacy_runtime SET snapshot_hash=?,snapshot=?,source_through=?,cursor=0,state=?,state_hash=?,checkpoint_hash=?,parent_hash=?,status='BOOTSTRAP',updated_at=? WHERE singleton=1 AND checkpoint_hash=? AND state_hash=? AND status='EXPIRED_RETENTION' AND numerical_abi=? AND source_through=?",new Object[]{sourceHash,snapshot,source.through,file.reference(),hash,checkpoint,prior.checkpoint,System.currentTimeMillis(),prior.checkpoint,prior.stateHash,prior.abi,prior.sourceThrough});
                try(Cursor changed=db.rawQuery("SELECT changes()",null)){if(!changed.moveToFirst()||changed.getInt(0)!=1)throw new IllegalStateException("R26_LEGACY_WARM_PARENT_CAS");}
                CheckpointManifestR26.record(db,checkpoint,"LEGACY:"+prior.roster,sourceHash,prior.checkpoint,prior.through,file.reference());return 0;
            });working=state;workingCheckpoint=checkpoint;active=null;claims.get().reason("LEGACY_LEARNED_KERNELS_CONTINUE_ON_STRICTLY_LATER_RETAINED_SOURCE; NO_QUALIFICATION_TRANSFER");return 1;
        }
    }
    static String[] nextInput(DbStore store,long through){
        String after=TargetIssueResolver.fromSequenceOrdinalSU1(Math.max(1,through+1));String issue=null;
        try(Cursor c=store.readable().rawQuery("SELECT issue FROM draws WHERE dirty=0 AND issue>=? ORDER BY issue LIMIT 1",new String[]{after})){if(c.moveToFirst())issue=c.getString(0);}if(issue==null)return null;
        Models.Draw draw=store.getDrawByIssue(issue);if(draw==null||!draw.isValid())throw new IllegalStateException("R26_LEGACY_CANONICAL_INPUT");String hash=DrawHashR36.sha256V2(draw);long ordinal=TargetIssueResolver.sequenceOrdinalSU1(issue);String event="";
        try(Cursor c=store.readable().rawQuery("SELECT event_id,source_hash FROM r26_feedback WHERE issue_ordinal=?",new String[]{String.valueOf(ordinal)})){if(c.moveToFirst()){if(!hash.equals(c.getString(1)))throw new IllegalStateException("R26_LEGACY_CANONICAL_CONFLICT");event=c.getString(0);}}
        return new String[]{event,issue,hash,String.valueOf(ordinal)};
    }
    private static void activate(DbStore store,Row row)throws IOException {Prepared p=active;if(p!=null&&p.checkpoint.equals(row.checkpoint))return;LegacyReplayStateR26 ready=decode(store,row);active=new Prepared(row,ready);}
    public static int capture(DbStore store,String target){
        if(!RuntimeSchemaR26.ready(store.readable()))return 0;long seq=TargetIssueResolver.sequenceOrdinalSU1(target);Row current=row(store.readable());if(current==null||!current.status.equals("LIVE")||current.through!=seq-1||!current.abi.equals(ResearchSemanticsGeneratedR967.TRAINING_RULE_SHA256))return 0;
        try{activate(store,current);}catch(IOException bad){android.util.Log.w("STARCORE_R26","legacy restore blocked",bad);return 0;}
        EventFrameSU1 frame=EventFrameBuilderSU1.captureWithGapsR26(store,target);Prepared ready=active;Map<String,ModelLiveSU2.Decision> decisions=new LinkedHashMap<>();
        synchronized(FORECAST_LOCK){if(ready==null||!ready.checkpoint.equals(current.checkpoint))return 0;BudgetTokenSU1 budget=new BudgetTokenSU1(1500,3000000,()->SettlementExecutionGateR819.isActive());ExecutionBudgetR26.enter(budget);
            try{List<Map.Entry<String,LiveStrategySU2>> order=new ArrayList<>(ready.state.lives.entrySet());Collections.rotate(order,order.isEmpty()?0:(int)(seq%order.size()));for(Map.Entry<String,LiveStrategySU2> e:order){budget.checkpointOrYield();decisions.put(e.getKey(),e.getValue().decide());}}
            catch(BudgetTokenSU1.Yield exhausted){}finally{ExecutionBudgetR26.leave();}}
        if(decisions.isEmpty())return 0;
        return store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("R26_LEGACY_FREEZE",db->{if(!EventFrameBuilderSU1.unchanged(store,frame))return 0;Row now=row(db);if(now==null||!now.checkpoint.equals(current.checkpoint)||now.through!=seq-1||!now.status.equals("LIVE"))return 0;long wall=System.currentTimeMillis();if(wall>=frame.targetOpenTime)return 0;for(Map.Entry<String,ModelLiveSU2.Decision> e:decisions.entrySet())ModelStoreSU2.insertDecision(db,e.getKey(),seq,e.getValue(),wall,frame.targetOpenTime);return decisions.size();});
    }
    public static float[][] pendingTable(String policy,long seq){synchronized(FORECAST_LOCK){Prepared p=active;if(p==null||p.state.through()!=seq-1)return null;LiveStrategySU2 live=p.state.lives.get(policy);if(live==null)return null;float[][] table=live.pendingTable(p.state.samples());if(table==null)return null;float[][] copy=new float[10][];for(int i=0;i<10;i++)copy[i]=table[i].clone();return copy;}}
}
