package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.os.SystemClock;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.*;
import java.io.*;
import java.util.*;
import java.util.function.Supplier;
import java.util.zip.*;

/** Fixed retained-history campaign, private model states and sample-level checkpoints.
 * Scores are research selection only. The original live league supplies future evidence. */
public final class FoundryCampaignR26 {
    private FoundryCampaignR26() {}
    static final class Snapshot {
        static final int MAGIC=0x26360001;
        long[] ordinal,issues,opened,available;int[][] draws;long through;
        byte[] encode()throws IOException{ByteArrayOutputStream b=new ByteArrayOutputStream();try(DataOutputStream o=new DataOutputStream(new CooperativeOutputStreamR26(b))){o.writeInt(MAGIC);o.writeInt(draws.length);for(int i=0;i<draws.length;i++){o.writeLong(ordinal[i]);o.writeLong(issues[i]);o.writeLong(opened[i]);o.writeLong(available[i]);for(int x:draws[i])o.writeByte(x);}}return b.toByteArray();}
        static Snapshot decode(byte[] b)throws IOException{Snapshot s=new Snapshot();try(DataInputStream in=new DataInputStream(new CooperativeInputStreamR26(new ByteArrayInputStream(b)))){int header=in.readInt();boolean complete=header==MAGIC;int n=complete?in.readInt():header;if(n<32||n>5000)throw new IOException("R26_SNAPSHOT_SIZE");s.ordinal=new long[n];s.issues=new long[n];s.opened=new long[n];s.available=new long[n];s.draws=new int[n][10];for(int i=0;i<n;i++){s.ordinal[i]=in.readLong();if(complete){s.issues[i]=in.readLong();s.opened[i]=in.readLong();s.available[i]=in.readLong();}if(i>0&&s.ordinal[i]<=s.ordinal[i-1])throw new IOException("R26_SNAPSHOT_ORDER");for(int p=0;p<10;p++)s.draws[i][p]=in.readUnsignedByte();OnlineModelsSU2.requireDraw(s.draws[i]);}if(in.read()!=-1)throw new IOException("R26_SNAPSHOT_TRAILING");}s.through=s.ordinal[s.ordinal.length-1];return s;}
    }
    static Snapshot capture(DbStore store){List<Models.Draw> ds=store.getRecentDraws(4096);if(ds.size()<64)return null;Snapshot s=new Snapshot();s.ordinal=new long[ds.size()];s.issues=new long[ds.size()];s.opened=new long[ds.size()];s.available=new long[ds.size()];s.draws=new int[ds.size()][10];for(int i=0;i<ds.size();i++){ExecutionBudgetR26.poll();Models.Draw d=ds.get(i);if(!d.isValid())throw new IllegalStateException("R26_BAD_SOURCE");s.ordinal[i]=TargetIssueResolver.sequenceOrdinalSU1(d.issue);s.issues[i]=Long.parseLong(d.issue);s.opened[i]=d.openTime;s.available[i]=d.fetchTime;for(int p=0;p<10;p++)s.draws[i][p]=d.getNumberAtPosition(p+1);}s.through=s.ordinal[ds.size()-1];return s;}
    static List<ModelGenomeSU2> savedParents(SQLiteDatabase db){
        List<ModelGenomeSU2> parents=new ArrayList<>();
        // The protected XI method has its own wire, not a mutable ModelGenomeSU2 program.
        try(Cursor c=db.rawQuery("SELECT canonical_text FROM su2_model_genome WHERE state IN ('LEAGUE','HALL_OF_FAME') AND origin_op<>'INCUMBENT' AND substr(canonical_text,1,4)='SU2|' AND instr(canonical_text,'_warm_kernel=')=0 ORDER BY coalesce(val_fitness,-1) DESC,genome_key LIMIT 3",null)){while(c.moveToNext())parents.add(ModelGenomeSU2.parse(c.getString(0)));}return parents;
    }
    /** No model training is executed inside the writer. Returns a count of committed samples. */
    public static long step(DbStore store,Supplier<RuntimeCoordinatorR26.Claim> claims,BudgetTokenSU1 budget)throws IOException{
        ModelPublicationR31.requireSchemaR31(store.readable());pruneFinishedR31(store,claims.get());final String numericalAbi=ResearchSemanticsGeneratedR967.TRAINING_RULE_SHA256;
        try(WarmCandidateR26.PreparedAbiR31 abi=WarmCandidateR26.prepareAbiR31(store.readable(),numericalAbi);
            WarmCandidateR26.PreparedPreservationR31 warm=WarmCandidateR26.preparePreservationR31(store.readable(),abi.campaigns())){
            store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("R26_CAMPAIGN_ABI_GUARD",db->{RuntimeCoordinatorR26.requireFence(db,claims.get());WarmCandidateR26.applyPreparedAbiR31(db,abi);WarmCandidateR26.applyPreparedPreservationR31(db,warm);RuntimeCoordinatorR26.requireFence(db,claims.get());return 0;});
        }
        SQLiteDatabase r=store.readable();String campaign=null;byte[] snapshotBytes=null;long through=0;
        try(Cursor c=r.rawQuery("SELECT f.campaign_id,f.snapshot,f.source_through FROM r26_foundry f WHERE f.status='RUNNING' ORDER BY CASE WHEN EXISTS(SELECT 1 FROM r26_candidate c WHERE c.campaign_id=f.campaign_id AND c.status='READY') THEN 1 ELSE 0 END,(SELECT coalesce(sum(CASE WHEN c.status='READY' THEN c.service_cpu_ms ELSE 0 END),0)*1.0/max(1,sum(CASE WHEN c.status='READY' THEN 1 ELSE 0 END)) FROM r26_candidate c WHERE c.campaign_id=f.campaign_id),f.created_at,f.campaign_id LIMIT 1",null)){if(c.moveToFirst()){campaign=c.getString(0);snapshotBytes=c.getBlob(1);through=c.getLong(2);}}
        Models.Draw latest=store.getLatestDraw();if(latest==null)return 0;long latestOrdinal=TargetIssueResolver.sequenceOrdinalSU1(latest.issue);
        if(campaign==null){
            try(Cursor c=r.rawQuery("SELECT max(source_through) FROM r26_foundry WHERE status='DONE' AND numerical_abi=?",new String[]{numericalAbi})){if(c.moveToFirst()&&!c.isNull(0)&&c.getLong(0)>=latestOrdinal)return 0;}
            budget.checkpointOrYield();long snapshotGeneration=EventFrameBuilderSU1.generation(r);Snapshot s=capture(store);if(s==null)return 0;snapshotBytes=s.encode();String hash=PolicyIdentityR26.sha256(Base64.getEncoder().encodeToString(snapshotBytes));campaign=HashingSU1.fields("R26_CAMPAIGN_V2",hash,numericalAbi);through=s.through;
            List<ModelGenomeSU2> seeds=ModelEvolutionSU2.centralPrograms(true,true);
            for(String family:FamilyRegistryR26.EXECUTABLE)seeds.add(FamilyRegistryR26.genome(family));
            // The exact unary matching head is a real trainable candidate, not a renamed fallback.
            seeds.add(new ModelGenomeSU2(Collections.singletonList(new ModelGenomeSU2.Member("MATCHING",Collections.singletonMap("lr","0.02"),1)),"STATIC",false,"MASS",0.15,0.2,false));
            List<StructuralProposalR26.Proposal> proposals=new ArrayList<>();
            for(ModelGenomeSU2 seed:seeds){ModelGenomeSU2 bound=seed.bindBirth(hash,through);proposals.add(new StructuralProposalR26.Proposal(bound,"","REGISTERED_SEED","REGISTERED_SEED"));List<StructuralProposalR26.Proposal> graphChildren=StructuralProposalR26.mechanismChildren(bound);if(!graphChildren.isEmpty()){proposals.add(graphChildren.get(0));if(bound.members.get(0).kind.equals("R26F20"))proposals.addAll(graphChildren.subList(1,graphChildren.size()));if(bound.members.size()==1&&(bound.members.get(0).kind.equals("R26F01")||bound.members.get(0).kind.equals("R26F02")||bound.members.get(0).kind.equals("R26F08")))proposals.add(graphChildren.get(1));}}
            // Preserve actual saved parents; do not inherit their hits or qualification.
            List<ModelGenomeSU2> parents=savedParents(r);
            for(int i=0;i<parents.size();i++)for(StructuralProposalR26.Proposal proposal:StructuralProposalR26.from(parents.get(i),seeds.get(i%seeds.size()).members.get(0)))proposals.add(new StructuralProposalR26.Proposal(proposal.genome.bindBirth(hash,through),proposal.parent,proposal.operation,proposal.category));
            final List<FailureDirectedResearchR26.Batch> failureResearch;try{failureResearch=FailureDirectedResearchR26.select(r,hash,through);}catch(org.json.JSONException malformed){throw new IOException("R26_FAILURE_RESEARCH_METADATA",malformed);}for(FailureDirectedResearchR26.Batch batch:failureResearch)proposals.addAll(batch.proposals);
            final String id=campaign;final byte[] frozen=snapshotBytes;final long source=through;
            if(EventFrameBuilderSU1.generation(r)!=snapshotGeneration)throw new IOException("R31_FOUNDRY_SOURCE_CHANGED_DURING_PREPARE");
            String publicationId="FOUNDRY_BIRTH:"+id;ModelPublicationR31.Plan plan=ModelPublicationR31.existing(r,publicationId);
            try{if(plan==null){plan=new ModelPublicationR31.Plan(publicationId,"FOUNDRY_BIRTH",hash,snapshotGeneration,-1,System.currentTimeMillis());String raw=plan.rawPayload(frozen);
                plan.put("FOUNDRY_ROOT",id,id,hash,source,raw,"RUNNING",plan.now,numericalAbi,s.ordinal[0],frozen.length);
                int index=0;for(StructuralProposalR26.Proposal proposal:proposals){ExecutionBudgetR26.poll();ModelGenomeSU2 g=proposal.genome;ModelStoreSU2.prepareIdentityR31(plan,g.text(),g.key());if(plan.item("CANDIDATE",g.key())==null)plan.put("CANDIDATE",g.key(),id,g.key(),g.text(),proposal.parent,proposal.operation);plan.put("ATTEMPT",id+":"+index,id+":"+(index++),id,g.key(),proposal.parent,proposal.operation,proposal.category,StructuralProposalR26.structure(g));}
                for(FailureDirectedResearchR26.Batch batch:failureResearch)plan.put("FAILURE_CURSOR",batch.parent,batch.parent,batch.oldDigest,batch.oldEnd,batch.digest,batch.windowEnd,batch.payload,batch.reason,batch.text);
            }
            ModelPublicationR31.stageAndPublish(store,claims.get(),plan);
            }finally{if(plan!=null)plan.close();}
            return 0;
        }
        final String id=campaign;
        // Snapshots cannot pin raw history indefinitely beyond the rolling retention window.
        if(latestOrdinal-Snapshot.decode(snapshotBytes).ordinal[0]>=5000){try(WarmCandidateR26.PreparedPreservationR31 warm=WarmCandidateR26.preparePreservationR31(store.readable(),Collections.singletonList(id))){store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("R26_CAMPAIGN_EXPIRE",db->{RuntimeCoordinatorR26.requireFence(db,claims.get());db.execSQL("UPDATE r26_foundry SET status='EXPIRED_RETENTION',snapshot=X'' WHERE campaign_id=?",new Object[]{id});WarmCandidateR26.applyPreparedPreservationR31(db,warm);RuntimeCoordinatorR26.requireFence(db,claims.get());return 0;});}return 0;}
        String text=null,key=null,parentHash="",expectedStateHash="";byte[] state=null;int cursor=0,dn=0,dk=0,vn=0,vk=0;double loss=0;
        try(Cursor c=r.rawQuery("SELECT canonical_text,policy_hash,cursor,state,dev_n,dev_k,val_n,val_k,loss_sum,checkpoint_hash,state_hash FROM r26_candidate candidate WHERE campaign_id=? AND status='READY' ORDER BY CASE WHEN operation IN ('CUT_CROSS_POSITION_LAG_EDGES','REMOVE_PAIRWISE_FACTORS','REMOVE_HIDDEN_TRANSITION_MEMORY','REMOVE_STATE_FRAGMENT_MEMORY','MISMATCH_STATE_FRAGMENT_RETRIEVAL','EXPIRE_STATE_FRAGMENT_RETRIEVAL','REMOVE_TENSOR_POSITION_MODE','REMOVE_TENSOR_STATE_MODE') THEN CASE WHEN EXISTS(SELECT 1 FROM r26_candidate unserved JOIN r26_capability cold ON cold.kind='FAMILY' AND cold.last_progress=0 AND instr(unserved.canonical_text,'R26'||cold.mechanism)>0 WHERE unserved.campaign_id=candidate.campaign_id AND unserved.status='READY' AND unserved.operation NOT IN ('CUT_CROSS_POSITION_LAG_EDGES','REMOVE_PAIRWISE_FACTORS','REMOVE_HIDDEN_TRANSITION_MEMORY','REMOVE_STATE_FRAGMENT_MEMORY','MISMATCH_STATE_FRAGMENT_RETRIEVAL','EXPIRE_STATE_FRAGMENT_RETRIEVAL','REMOVE_TENSOR_POSITION_MODE','REMOVE_TENSOR_STATE_MODE')) THEN 2 ELSE 1 END WHEN EXISTS(SELECT 1 FROM r26_capability cap WHERE cap.kind='FAMILY' AND cap.last_progress=0 AND instr(canonical_text,'R26'||cap.mechanism)>0) THEN 0 ELSE 1 END,service_cpu_ms,last_served,policy_hash LIMIT 1",new String[]{id})){if(c.moveToFirst()){text=c.getString(0);key=c.getString(1);cursor=c.getInt(2);state=c.isNull(3)?null:c.getBlob(3);dn=c.getInt(4);dk=c.getInt(5);vn=c.getInt(6);vk=c.getInt(7);loss=c.getDouble(8);parentHash=c.getString(9);expectedStateHash=c.getString(10);}}
        if(text==null){finish(store,claims,id,through);return 0;}
        Snapshot snap=Snapshot.decode(snapshotBytes);final long serviceCpuStart=SystemClock.currentThreadTimeMillis();ModelGenomeSU2 g;ModelLiveSU2.Hub hub;ModelLiveSU2 live;
        try{g=ModelGenomeSU2.parse(text);hub=new ModelLiveSU2.Hub();live=new ModelLiveSU2(g,hub,ModelLiveLoopSU2.START_META,ModelLiveLoopSU2.WINDOW,ModelLiveLoopSU2.REFIT,state!=null);
        if(state!=null){state=CheckpointArtifactsR26.resolve(store,state);if(!PolicyIdentityR26.sha256(Base64.getEncoder().encodeToString(state)).equals(expectedStateHash))throw new IOException("R26_CHECKPOINT_HASH_MISMATCH");try(DataInputStream in=new DataInputStream(new BufferedInputStream(new CooperativeInputStreamR26(new GZIPInputStream(new ByteArrayInputStream(state))),32768))){hub.readInto(in);live.readInto(in);if(in.read()!=-1)throw new IOException("R26_CHECKPOINT_TRAILING");}}
        }catch(IOException|IllegalArgumentException invalid){final int failedCursor=cursor;final String policy=key,previous=parentHash,why=invalid.getClass().getSimpleName()+":"+String.valueOf(invalid.getMessage());
            store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("R26_RESEARCH_MODEL_QUARANTINE",db->{RuntimeCoordinatorR26.requireFence(db,claims.get());db.execSQL("UPDATE r26_candidate SET status='BLOCKED_CHECKPOINT_OR_PROGRAM' WHERE campaign_id=? AND policy_hash=? AND checkpoint_hash=?",new Object[]{id,policy,previous});db.execSQL("INSERT OR REPLACE INTO r26_live_failure VALUES(?,?,?,?)",new Object[]{policy,snap.ordinal[Math.min(Math.max(0,failedCursor),snap.ordinal.length-1)],"RESEARCH_CHECKPOINT_OR_PROGRAM:"+why,System.currentTimeMillis()});return 0;});claims.get().reason("RESEARCH_MODEL_QUARANTINED:"+policy+":"+why);return 0;
        }
        byte[] lastGood=state==null?encode(hub,live):state;boolean diagnostics=TrainingStepStoreR26.supported(hub);List<TrainingStepStoreR26.Pending> trainingSteps=new ArrayList<>();String diagnosticSource=diagnostics?LiveStateR26.hash(snapshotBytes):"";
        // Give each unserved family a complete learned checkpoint before longer batches.
        int sampleLimit=16;if(cursor==0)for(ModelGenomeSU2.Member member:g.members){String family=member.kind.equals("MATCHING")?"F17":member.kind.startsWith("R26F")?member.kind.substring(3):"";if(!family.isEmpty())try(Cursor served=r.rawQuery("SELECT last_progress FROM r26_capability WHERE mechanism=?",new String[]{family})){if(served.moveToFirst()&&served.getLong(0)==0)sampleLimit=1;}}
        int before=cursor;long stop=budget.activeNanos()+160_000_000L;int devEnd=snap.draws.length*3/5;
        while(cursor<snap.draws.length&&cursor-before<sampleLimit&&budget.activeNanos()<stop){
            try{budget.checkpointOrYield();}catch(BudgetTokenSU1.Yield yield){break;}
            // A missing issue is never represented as an adjacent observed transition.
            if(cursor>0&&snap.ordinal[cursor]!=snap.ordinal[cursor-1]+1&&!TemporalResetR26.reset(hub,live)){
                final String blockedPolicy=key;store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("R26_GAP_POLICY_BLOCK",db->{RuntimeCoordinatorR26.requireFence(db,claims.get());db.execSQL("UPDATE r26_candidate SET status='BLOCKED_HISTORY_GAP' WHERE campaign_id=? AND policy_hash=?",new Object[]{id,blockedPolicy});return 0;});return 0;
            }
            LegacyTeacherOwnerR26.enter(snap.issues,snap.opened,snap.available,snap.draws,cursor);
            try {
            Map<String,Long> beforeDiagnostics=diagnostics?TrainingStepStoreR26.counters(hub):Collections.emptyMap();
            ModelLiveSU2.Decision decision=live.decide();float[][] p=live.pendingTable(hub.t);double sampleLoss=0;for(int j=0;j<10;j++)sampleLoss-=Math.log(Math.max(1e-12,p[j][snap.draws[cursor][j]-1]));
            boolean hit=live.settle(snap.draws[cursor]);hub.update(snap.draws[cursor]);
            // Serialization can yield too. Advance neither the cursor nor its metrics
            // until a complete checkpoint exists for this exact sample.
            byte[] completed=encode(hub,live);
            if(diagnostics)trainingSteps.addAll(TrainingStepStoreR26.capture(g.key(),hub,id,diagnosticSource,"FROZEN_FOUNDRY_SNAPSHOT",snap.ordinal[cursor],snap.issues[cursor],cursor>0&&snap.ordinal[cursor]!=snap.ordinal[cursor-1]+1,LiveStateR26.hash(lastGood),LiveStateR26.hash(completed),beforeDiagnostics));
            if(cursor>=32){loss+=sampleLoss/10;if(decision.act){if(cursor<devEnd){dn++;if(hit)dk++;}else{vn++;if(hit)vk++;}}}cursor++;lastGood=completed;
            } catch(BudgetTokenSU1.Yield yielded) { break; } finally { LegacyTeacherOwnerR26.leave(); }
        }
        if(cursor==before)return 0;
        byte[] checkpoint=lastGood;if(checkpoint.length>4<<20)throw new IOException("R26_CHECKPOINT_CAPACITY");
        final String parent=parentHash, stateHash=PolicyIdentityR26.sha256(Base64.getEncoder().encodeToString(checkpoint));
        final String checkpointHash=PolicyIdentityR26.sha256("STARCORE_CHECKPOINT_R26_V1|"+key+"|"+id+"|"+parent+"|"+cursor+"|"+stateHash);
        final String policy=key;final int after=cursor,fdn=dn,fdk=dk,fvn=vn,fvk=vk;final double fl=loss;final String status=after==snap.draws.length?"DONE":"READY";
        try(CheckpointArtifactsR26.Prepared file=CheckpointArtifactsR26.prepareLease(store,checkpoint)){store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("R26_CANDIDATE_STEP",db->{RuntimeCoordinatorR26.requireFence(db,claims.get());try(Cursor sourceGuard=db.rawQuery("SELECT 1 FROM r26_foundry WHERE campaign_id=? AND status='RUNNING' AND numerical_abi=? AND length(snapshot)>0",new String[]{id,numericalAbi})){if(!sourceGuard.moveToFirst())throw new IllegalStateException("R26_CANDIDATE_SOURCE_EXPIRED");}db.execSQL("UPDATE r26_candidate SET cursor=?,state=?,state_hash=?,checkpoint_hash=?,dev_n=?,dev_k=?,val_n=?,val_k=?,loss_sum=?,status=?,service_cpu_ms=service_cpu_ms+?,last_served=? WHERE campaign_id=? AND policy_hash=? AND cursor=? AND checkpoint_hash=?",new Object[]{after,file.reference(),stateHash,checkpointHash,fdn,fdk,fvn,fvk,fl,status,Math.max(1,SystemClock.currentThreadTimeMillis()-serviceCpuStart),System.currentTimeMillis(),id,policy,before,parent});try(Cursor c=db.rawQuery("SELECT changes()",null)){if(!c.moveToFirst()||c.getInt(0)!=1)throw new IllegalStateException("R26_CANDIDATE_CAS");}
            CheckpointManifestR26.record(db,checkpointHash,policy,id,parent,snap.ordinal[after-1],file.reference());
            TrainingStepStoreR26.record(db,trainingSteps,checkpointHash);
            db.execSQL("INSERT INTO r26_checkpoint_receipt VALUES(?,?,?,?,?,?,'EXPOSED_HISTORY',?)",new Object[]{checkpointHash,policy,id,parent,after,stateHash,System.currentTimeMillis()});
            for(int sample=before;sample<after;sample++)try(Cursor event=db.rawQuery("SELECT event_id,payload FROM r26_feedback WHERE issue_ordinal=?",new String[]{String.valueOf(snap.ordinal[sample])})){if(event.moveToFirst()){
                String[] payload=event.getString(1).split(",");if(payload.length!=11)throw new IllegalStateException("R26_EVENT_PAYLOAD");for(int p=0;p<10;p++)if(Integer.parseInt(payload[p+1])!=snap.draws[sample][p])throw new IllegalStateException("R26_EVENT_SOURCE_CHANGED");
                db.execSQL("INSERT OR IGNORE INTO r26_consumption VALUES(?,?,?,'EXPOSED_HISTORY')",new Object[]{event.getString(0),policy,checkpointHash});
            }}
            if(!StructuralProposalR26.control(g))for(ModelGenomeSU2.Member member:g.members)if(member.kind.equals("MATCHING")||member.kind.startsWith("R26"))db.execSQL("UPDATE r26_capability SET state='TRAINED',last_progress=?,reason='Actual sample checkpoint committed; complete experiments and natural qualification pending' WHERE mechanism=?",new Object[]{System.currentTimeMillis(),member.kind.equals("MATCHING")?"F17":member.kind.substring(3)});
            return 0;});}return after-before;
    }
    private static byte[] encode(ModelLiveSU2.Hub hub,ModelLiveSU2 live)throws IOException{ByteArrayOutputStream bytes=new ByteArrayOutputStream();try(DataOutputStream out=new DataOutputStream(new BufferedOutputStream(new CooperativeOutputStreamR26(new GZIPOutputStream(bytes)),32768))){hub.write(out);live.write(out);}return bytes.toByteArray();}
    static void publishBirthPreparedR31(SQLiteDatabase db,ModelPublicationR31.Plan p){
        ModelPublicationR31.Item root=null;for(ModelPublicationR31.Item item:p.items.values())if(item.kind.equals("FOUNDRY_ROOT"))root=item;if(root==null)throw new IllegalStateException("R31_FOUNDRY_ROOT_MISSING");String campaign=(String)root.values[0];
        try(Cursor c=db.rawQuery("SELECT 1 FROM r26_foundry WHERE campaign_id=?",new String[]{campaign})){if(c.moveToFirst())throw new IllegalStateException("R31_FOUNDRY_BIRTH_ALREADY_EXISTS");}
        ModelStoreSU2.projectPreparedR31(db,p);
        String rows=ModelPublicationR31.select(p,"FOUNDRY_ROOT","c0,c1,c2,c3,c4,c5,c6,c7,c8");
        db.execSQL("INSERT INTO r26_foundry(campaign_id,snapshot_hash,source_through,snapshot,status,created_at,numerical_abi,source_first) SELECT c0,c1,c2,"+ModelPublicationR31.rawExpression((String)root.values[3])+",c4,c5,c6,c7 FROM ("+rows+")");
        db.execSQL("INSERT OR IGNORE INTO r26_candidate(campaign_id,policy_hash,canonical_text,parent,operation) "+ModelPublicationR31.select(p,"CANDIDATE","c0,c1,c2,c3,c4"));
        db.execSQL("INSERT OR IGNORE INTO r26_evolution(attempt_id,campaign_id,policy_hash,parent,operation,category,structure_hash) "+ModelPublicationR31.select(p,"ATTEMPT","c0,c1,c2,c3,c4,c5,c6"));
        if(p.count("FAILURE_CURSOR")>0){String attempts=ModelPublicationR31.select(p,"ATTEMPT","c0,c1,c2,c3,c4");try(Cursor c=db.rawQuery("SELECT 1 FROM ("+attempts+") a WHERE instr(a.c4,'FAILURE_GUIDED_V1:')=1 AND NOT EXISTS(SELECT 1 FROM r26_candidate child JOIN r26_evolution e ON e.campaign_id=child.campaign_id AND e.policy_hash=child.policy_hash WHERE child.campaign_id=a.c1 AND child.policy_hash=a.c2 AND e.parent IS a.c3 AND e.operation IS a.c4) LIMIT 1",null)){if(c.moveToFirst())throw new IllegalStateException("R26_FAILURE_CHILD_NOT_REGISTERED");}}
        // The original failure cursor CAS is published with all of its children.
        for(ModelPublicationR31.Item item:p.items.values())if(item.kind.equals("FAILURE_CURSOR")){
            Object[] v=item.values;try(Cursor c=db.rawQuery("SELECT 1 FROM r26_qualification_result q JOIN r26_live_model l ON l.policy_hash=q.policy_hash WHERE q.policy_hash=? AND q.evidence_digest=? AND q.reason=? AND q.window_end=? AND l.canonical_text=? AND l.status='ACTIVE'",new String[]{(String)v[0],(String)v[3],(String)v[6],String.valueOf(v[4]),(String)v[7]})){if(!c.moveToFirst())throw new IllegalStateException("R26_FAILURE_EVIDENCE_CHANGED");}
            db.execSQL("INSERT OR IGNORE INTO r26_failure_research_cursor VALUES(?,'',-1,'','')",new Object[]{v[0]});db.execSQL("UPDATE r26_failure_research_cursor SET evidence_digest=?,window_end=?,campaign_id=?,proposal_json=? WHERE parent_policy=? AND evidence_digest=? AND window_end=?",new Object[]{v[3],v[4],campaign,v[5],v[0],v[1],v[2]});try(Cursor c=db.rawQuery("SELECT changes()",null)){if(!c.moveToFirst()||c.getInt(0)!=1)throw new IllegalStateException("R26_FAILURE_RESEARCH_STALE_CURSOR");}
        }
    }
    static void publishFinishPreparedR31(SQLiteDatabase db,ModelPublicationR31.Plan p){
        ModelPublicationR31.Item meta=null;for(ModelPublicationR31.Item item:p.items.values())if(item.kind.equals("FOUNDRY_FINISH"))meta=item;if(meta==null)throw new IllegalStateException("R31_FOUNDRY_FINISH_META");String campaign=(String)meta.values[0];
        try(Cursor c=db.rawQuery("SELECT snapshot_hash,status FROM r26_foundry WHERE campaign_id=?",new String[]{campaign})){if(!c.moveToFirst()||!p.source.equals(c.getString(0))||!c.getString(1).equals("RUNNING"))throw new IllegalStateException("R31_FOUNDRY_FINISH_CAS");}
        ModelStoreSU2.projectPreparedR31(db,p);
        if(p.count("ACCEPTED")>0)db.execSQL("UPDATE r26_evolution SET research_accepted=1 WHERE campaign_id=? AND attempt_id IN ("+ModelPublicationR31.select(p,"ACCEPTED","c0")+")",new Object[]{campaign});
        db.execSQL("UPDATE r26_foundry SET status='DONE' WHERE campaign_id=? AND status='RUNNING'",new Object[]{campaign});try(Cursor c=db.rawQuery("SELECT changes()",null)){if(!c.moveToFirst()||c.getInt(0)!=1)throw new IllegalStateException("R31_FOUNDRY_FINISH_CAS");}
    }
    private static void pruneFinishedR31(DbStore store,RuntimeCoordinatorR26.Claim claim){
        List<String[]> rows=new ArrayList<>();String old="campaign_id IN (SELECT campaign_id FROM r26_foundry WHERE status<>'RUNNING' ORDER BY created_at DESC LIMIT -1 OFFSET 2)";
        try(Cursor c=store.readable().rawQuery("SELECT campaign_id,policy_hash FROM r26_candidate WHERE "+old+" AND (state IS NULL OR length(state)=0 OR EXISTS(SELECT 1 FROM r26_live_model l WHERE l.policy_hash=r26_candidate.policy_hash)) ORDER BY campaign_id,policy_hash LIMIT 8",null)){while(c.moveToNext())rows.add(new String[]{c.getString(0),c.getString(1)});}
        if(rows.isEmpty())return;store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("R31_FOUNDRY_RETENTION_SLICE",db->{RuntimeCoordinatorR26.requireFence(db,claim);for(String[] row:rows)db.execSQL("DELETE FROM r26_candidate WHERE campaign_id=? AND policy_hash=? AND "+old+" AND (state IS NULL OR length(state)=0 OR EXISTS(SELECT 1 FROM r26_live_model l WHERE l.policy_hash=r26_candidate.policy_hash))",row);db.execSQL("DELETE FROM r26_foundry WHERE campaign_id IN (SELECT f.campaign_id FROM r26_foundry f WHERE f.status<>'RUNNING' AND NOT EXISTS(SELECT 1 FROM r26_candidate c WHERE c.campaign_id=f.campaign_id) AND f.campaign_id NOT IN (SELECT campaign_id FROM r26_foundry ORDER BY created_at DESC LIMIT 2) ORDER BY f.created_at,f.campaign_id LIMIT 2)");RuntimeCoordinatorR26.requireFence(db,claim);return 0;});
    }
    private static void finish(DbStore store,Supplier<RuntimeCoordinatorR26.Claim> claims,String campaign,long through){
        List<ModelEvolutionSU2.Ranked> best=new ArrayList<>();List<double[]> scores=new ArrayList<>();
        try(Cursor c=store.readable().rawQuery("SELECT canonical_text,dev_n,dev_k,val_n,val_k FROM r26_candidate WHERE campaign_id=? AND status='DONE' ORDER BY CASE WHEN val_n>0 THEN 1.0*val_k/val_n ELSE -1 END DESC,policy_hash LIMIT 3",new String[]{campaign})){while(c.moveToNext()){ModelGenomeSU2 g=ModelGenomeSU2.parse(c.getString(0));best.add(new ModelEvolutionSU2.Ranked(g,"R26_RESEARCH_SELECTION","","EXPOSED_HISTORY; NOT A QUALITY CERTIFICATE"));scores.add(new double[]{c.getInt(1)==0?-1:c.getDouble(2)/c.getInt(1),c.getInt(3)==0?-1:c.getDouble(4)/c.getInt(3)});}}
        SQLiteDatabase r=store.readable();String sourceHash;try(Cursor c=r.rawQuery("SELECT snapshot_hash FROM r26_foundry WHERE campaign_id=?",new String[]{campaign})){if(!c.moveToFirst())throw new IllegalStateException("R31_FOUNDRY_FINISH_SOURCE_MISSING");sourceHash=c.getString(0);}
        long publicationGeneration=EventFrameBuilderSU1.generation(r);String publicationId="FOUNDRY_FINISH:"+campaign+":"+publicationGeneration;ModelPublicationR31.Plan plan;
        try{plan=ModelPublicationR31.existing(r,publicationId);}catch(IOException invalid){throw new IllegalStateException("R31_FOUNDRY_PLAN_RECOVERY",invalid);}
        try{if(plan==null){plan=new ModelPublicationR31.Plan(publicationId,"FOUNDRY_FINISH",sourceHash,publicationGeneration,-1,System.currentTimeMillis());plan.put("FOUNDRY_FINISH",campaign,campaign);
            for(int i=0;i<best.size();i++){ModelEvolutionSU2.Ranked x=best.get(i);ModelStoreSU2.prepareGenomeR31(plan,x,campaign,through,scores.get(i)[0],scores.get(i)[1],"RESEARCH");try(Cursor c=r.rawQuery("SELECT attempt_id FROM r26_evolution WHERE campaign_id=? AND policy_hash=? ORDER BY attempt_id",new String[]{campaign,x.g.key()})){while(c.moveToNext())plan.put("ACCEPTED",c.getString(0),c.getString(0),x.g.key());}}
        }
        ModelPublicationR31.stageAndPublish(store,claims.get(),plan);
        }catch(IOException invalid){throw new IllegalStateException("R31_FOUNDRY_FINISH_RECOVERY",invalid);}finally{if(plan!=null)plan.close();}
        pruneFinishedR31(store,claims.get());
    }
}
