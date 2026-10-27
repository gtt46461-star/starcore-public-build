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
        try(Cursor c=db.rawQuery("SELECT canonical_text FROM su2_model_genome WHERE state IN ('LEAGUE','HALL_OF_FAME') AND origin_op<>'INCUMBENT' AND substr(canonical_text,1,4)='SU2|' ORDER BY coalesce(val_fitness,-1) DESC,genome_key LIMIT 3",null)){while(c.moveToNext())parents.add(ModelGenomeSU2.parse(c.getString(0)));}return parents;
    }
    /** No model training is executed inside the writer. Returns a count of committed samples. */
    public static long step(DbStore store,Supplier<RuntimeCoordinatorR26.Claim> claims,BudgetTokenSU1 budget)throws IOException{
        final String numericalAbi=ResearchSemanticsGeneratedR967.TRAINING_RULE_SHA256;
        store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("R26_CAMPAIGN_ABI_GUARD",db->{RuntimeCoordinatorR26.requireFence(db,claims.get());db.execSQL("UPDATE r26_foundry SET status='ABANDONED_NUMERICAL_ABI',snapshot=X'' WHERE status='RUNNING' AND numerical_abi<>?",new Object[]{numericalAbi});return 0;});
        SQLiteDatabase r=store.readable();String campaign=null;byte[] snapshotBytes=null;long through=0;
        try(Cursor c=r.rawQuery("SELECT campaign_id,snapshot,source_through FROM r26_foundry WHERE status='RUNNING' ORDER BY created_at LIMIT 1",null)){if(c.moveToFirst()){campaign=c.getString(0);snapshotBytes=c.getBlob(1);through=c.getLong(2);}}
        Models.Draw latest=store.getLatestDraw();if(latest==null)return 0;long latestOrdinal=TargetIssueResolver.sequenceOrdinalSU1(latest.issue);
        if(campaign==null){
            try(Cursor c=r.rawQuery("SELECT max(source_through) FROM r26_foundry WHERE status='DONE' AND numerical_abi=?",new String[]{numericalAbi})){if(c.moveToFirst()&&!c.isNull(0)&&c.getLong(0)>=latestOrdinal)return 0;}
            budget.checkpointOrYield();Snapshot s=capture(store);if(s==null)return 0;snapshotBytes=s.encode();String hash=PolicyIdentityR26.sha256(Base64.getEncoder().encodeToString(snapshotBytes));campaign=HashingSU1.fields("R26_CAMPAIGN_V2",hash,numericalAbi);through=s.through;
            List<ModelGenomeSU2> seeds=ModelEvolutionSU2.centralPrograms(true,true);
            for(String family:FamilyRegistryR26.EXECUTABLE)seeds.add(FamilyRegistryR26.genome(family));
            // The exact unary matching head is a real trainable candidate, not a renamed fallback.
            seeds.add(new ModelGenomeSU2(Collections.singletonList(new ModelGenomeSU2.Member("MATCHING",Collections.singletonMap("lr","0.02"),1)),"STATIC",false,"MASS",0.15,0.2,false));
            List<StructuralProposalR26.Proposal> proposals=new ArrayList<>();
            for(ModelGenomeSU2 seed:seeds){ModelGenomeSU2 bound=seed.bindBirth(hash,through);proposals.add(new StructuralProposalR26.Proposal(bound,"","REGISTERED_SEED","REGISTERED_SEED"));List<StructuralProposalR26.Proposal> graphChildren=StructuralProposalR26.mechanismChildren(bound);if(!graphChildren.isEmpty()){proposals.add(graphChildren.get(0));if(bound.members.get(0).kind.equals("R26F20"))proposals.addAll(graphChildren.subList(1,graphChildren.size()));if(bound.members.size()==1&&(bound.members.get(0).kind.equals("R26F01")||bound.members.get(0).kind.equals("R26F02")||bound.members.get(0).kind.equals("R26F08")))proposals.add(graphChildren.get(1));}}
            // Preserve actual saved parents; do not inherit their hits or qualification.
            List<ModelGenomeSU2> parents=savedParents(r);
            for(int i=0;i<parents.size();i++)for(StructuralProposalR26.Proposal proposal:StructuralProposalR26.from(parents.get(i),seeds.get(i%seeds.size()).members.get(0)))proposals.add(new StructuralProposalR26.Proposal(proposal.genome.bindBirth(hash,through),proposal.parent,proposal.operation,proposal.category));
            final String id=campaign;final byte[] frozen=snapshotBytes;final long source=through;
            store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("R26_CAMPAIGN_BIRTH",db->{RuntimeCoordinatorR26.requireFence(db,claims.get());db.execSQL("INSERT OR IGNORE INTO r26_foundry(campaign_id,snapshot_hash,source_through,snapshot,status,created_at,numerical_abi,source_first) VALUES(?,?,?,?,'RUNNING',?,?,?)",new Object[]{id,hash,source,frozen,System.currentTimeMillis(),numericalAbi,s.ordinal[0]});int index=0;for(StructuralProposalR26.Proposal proposal:proposals){ModelGenomeSU2 g=proposal.genome;PolicyIdentityLedgerR26.record(db,g.text(),g.key());
                db.execSQL("INSERT OR IGNORE INTO r26_candidate(campaign_id,policy_hash,canonical_text,parent,operation) VALUES(?,?,?,?,?)",new Object[]{id,g.key(),g.text(),proposal.parent,proposal.operation});
                db.execSQL("INSERT OR IGNORE INTO r26_evolution(attempt_id,campaign_id,policy_hash,parent,operation,category,structure_hash) VALUES(?,?,?,?,?,?,?)",new Object[]{id+":"+(index++),id,g.key(),proposal.parent,proposal.operation,proposal.category,StructuralProposalR26.structure(g)});
            }return 0;});
            return 0;
        }
        final String id=campaign;
        // Snapshots cannot pin raw history indefinitely beyond the rolling retention window.
        if(latestOrdinal-Snapshot.decode(snapshotBytes).ordinal[0]>=5000){store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("R26_CAMPAIGN_EXPIRE",db->{RuntimeCoordinatorR26.requireFence(db,claims.get());db.execSQL("UPDATE r26_foundry SET status='EXPIRED_RETENTION',snapshot=X'' WHERE campaign_id=?",new Object[]{id});db.execSQL("DELETE FROM r26_candidate WHERE campaign_id=?",new Object[]{id});return 0;});return 0;}
        String text=null,key=null,parentHash="",expectedStateHash="";byte[] state=null;int cursor=0,dn=0,dk=0,vn=0,vk=0;double loss=0;
        try(Cursor c=r.rawQuery("SELECT canonical_text,policy_hash,cursor,state,dev_n,dev_k,val_n,val_k,loss_sum,checkpoint_hash,state_hash FROM r26_candidate candidate WHERE campaign_id=? AND status='READY' ORDER BY CASE WHEN operation IN ('CUT_CROSS_POSITION_LAG_EDGES','REMOVE_PAIRWISE_FACTORS','REMOVE_HIDDEN_TRANSITION_MEMORY','REMOVE_STATE_FRAGMENT_MEMORY','MISMATCH_STATE_FRAGMENT_RETRIEVAL','EXPIRE_STATE_FRAGMENT_RETRIEVAL') THEN CASE WHEN EXISTS(SELECT 1 FROM r26_candidate unserved JOIN r26_capability cold ON cold.kind='FAMILY' AND cold.last_progress=0 AND instr(unserved.canonical_text,'R26'||cold.mechanism)>0 WHERE unserved.campaign_id=candidate.campaign_id AND unserved.status='READY' AND unserved.operation NOT IN ('CUT_CROSS_POSITION_LAG_EDGES','REMOVE_PAIRWISE_FACTORS','REMOVE_HIDDEN_TRANSITION_MEMORY','REMOVE_STATE_FRAGMENT_MEMORY','MISMATCH_STATE_FRAGMENT_RETRIEVAL','EXPIRE_STATE_FRAGMENT_RETRIEVAL')) THEN 2 ELSE 1 END WHEN EXISTS(SELECT 1 FROM r26_capability cap WHERE cap.kind='FAMILY' AND cap.last_progress=0 AND instr(canonical_text,'R26'||cap.mechanism)>0) THEN 0 ELSE 1 END,service_cpu_ms,last_served,policy_hash LIMIT 1",new String[]{id})){if(c.moveToFirst()){text=c.getString(0);key=c.getString(1);cursor=c.getInt(2);state=c.isNull(3)?null:c.getBlob(3);dn=c.getInt(4);dk=c.getInt(5);vn=c.getInt(6);vk=c.getInt(7);loss=c.getDouble(8);parentHash=c.getString(9);expectedStateHash=c.getString(10);}}
        if(text==null){finish(store,claims,id,through);return 0;}
        Snapshot snap=Snapshot.decode(snapshotBytes);final long serviceCpuStart=SystemClock.currentThreadTimeMillis();ModelGenomeSU2 g;ModelLiveSU2.Hub hub;ModelLiveSU2 live;
        try{g=ModelGenomeSU2.parse(text);hub=new ModelLiveSU2.Hub();live=new ModelLiveSU2(g,hub,ModelLiveLoopSU2.START_META,ModelLiveLoopSU2.WINDOW,ModelLiveLoopSU2.REFIT);
        if(state!=null){state=CheckpointArtifactsR26.resolve(store,state);if(!PolicyIdentityR26.sha256(Base64.getEncoder().encodeToString(state)).equals(expectedStateHash))throw new IOException("R26_CHECKPOINT_HASH_MISMATCH");try(DataInputStream in=new DataInputStream(new BufferedInputStream(new CooperativeInputStreamR26(new GZIPInputStream(new ByteArrayInputStream(state))),32768))){hub.readInto(in);live.readInto(in);if(in.read()!=-1)throw new IOException("R26_CHECKPOINT_TRAILING");}}
        }catch(IOException|IllegalArgumentException invalid){final int failedCursor=cursor;final String policy=key,previous=parentHash,why=invalid.getClass().getSimpleName()+":"+String.valueOf(invalid.getMessage());
            store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("R26_RESEARCH_MODEL_QUARANTINE",db->{RuntimeCoordinatorR26.requireFence(db,claims.get());db.execSQL("UPDATE r26_candidate SET status='BLOCKED_CHECKPOINT_OR_PROGRAM' WHERE campaign_id=? AND policy_hash=? AND checkpoint_hash=?",new Object[]{id,policy,previous});db.execSQL("INSERT OR REPLACE INTO r26_live_failure VALUES(?,?,?,?)",new Object[]{policy,snap.ordinal[Math.min(Math.max(0,failedCursor),snap.ordinal.length-1)],"RESEARCH_CHECKPOINT_OR_PROGRAM:"+why,System.currentTimeMillis()});return 0;});claims.get().reason("RESEARCH_MODEL_QUARANTINED:"+policy+":"+why);return 0;
        }
        byte[] lastGood=state==null?encode(hub,live):state;
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
            ModelLiveSU2.Decision decision=live.decide();float[][] p=live.pendingTable(hub.t);double sampleLoss=0;for(int j=0;j<10;j++)sampleLoss-=Math.log(Math.max(1e-12,p[j][snap.draws[cursor][j]-1]));
            boolean hit=live.settle(snap.draws[cursor]);hub.update(snap.draws[cursor]);
            if(cursor>=32){loss+=sampleLoss/10;if(decision.act){if(cursor<devEnd){dn++;if(hit)dk++;}else{vn++;if(hit)vk++;}}}cursor++;lastGood=encode(hub,live);
            } catch(BudgetTokenSU1.Yield yielded) { break; } finally { LegacyTeacherOwnerR26.leave(); }
        }
        if(cursor==before)return 0;
        byte[] checkpoint=lastGood;if(checkpoint.length>4<<20)throw new IOException("R26_CHECKPOINT_CAPACITY");
        final String parent=parentHash, stateHash=PolicyIdentityR26.sha256(Base64.getEncoder().encodeToString(checkpoint));
        final String checkpointHash=PolicyIdentityR26.sha256("STARCORE_CHECKPOINT_R26_V1|"+key+"|"+id+"|"+parent+"|"+cursor+"|"+stateHash);
        final String policy=key;final int after=cursor,fdn=dn,fdk=dk,fvn=vn,fvk=vk;final double fl=loss;final String status=after==snap.draws.length?"DONE":"READY";
        try(CheckpointArtifactsR26.Prepared file=CheckpointArtifactsR26.prepareLease(store,checkpoint)){store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("R26_CANDIDATE_STEP",db->{RuntimeCoordinatorR26.requireFence(db,claims.get());try(Cursor sourceGuard=db.rawQuery("SELECT 1 FROM r26_foundry WHERE campaign_id=? AND status='RUNNING' AND numerical_abi=? AND length(snapshot)>0",new String[]{id,numericalAbi})){if(!sourceGuard.moveToFirst())throw new IllegalStateException("R26_CANDIDATE_SOURCE_EXPIRED");}db.execSQL("UPDATE r26_candidate SET cursor=?,state=?,state_hash=?,checkpoint_hash=?,dev_n=?,dev_k=?,val_n=?,val_k=?,loss_sum=?,status=?,service_cpu_ms=service_cpu_ms+?,last_served=? WHERE campaign_id=? AND policy_hash=? AND cursor=? AND checkpoint_hash=?",new Object[]{after,file.reference(),stateHash,checkpointHash,fdn,fdk,fvn,fvk,fl,status,Math.max(1,SystemClock.currentThreadTimeMillis()-serviceCpuStart),System.currentTimeMillis(),id,policy,before,parent});try(Cursor c=db.rawQuery("SELECT changes()",null)){if(!c.moveToFirst()||c.getInt(0)!=1)throw new IllegalStateException("R26_CANDIDATE_CAS");}
            CheckpointManifestR26.record(db,checkpointHash,policy,id,parent,snap.ordinal[after-1],file.reference());
            db.execSQL("INSERT INTO r26_checkpoint_receipt VALUES(?,?,?,?,?,?,'EXPOSED_HISTORY',?)",new Object[]{checkpointHash,policy,id,parent,after,stateHash,System.currentTimeMillis()});
            for(int sample=before;sample<after;sample++)try(Cursor event=db.rawQuery("SELECT event_id,payload FROM r26_feedback WHERE issue_ordinal=?",new String[]{String.valueOf(snap.ordinal[sample])})){if(event.moveToFirst()){
                String[] payload=event.getString(1).split(",");if(payload.length!=11)throw new IllegalStateException("R26_EVENT_PAYLOAD");for(int p=0;p<10;p++)if(Integer.parseInt(payload[p+1])!=snap.draws[sample][p])throw new IllegalStateException("R26_EVENT_SOURCE_CHANGED");
                db.execSQL("INSERT OR IGNORE INTO r26_consumption VALUES(?,?,?,'EXPOSED_HISTORY')",new Object[]{event.getString(0),policy,checkpointHash});
            }}
            if(!StructuralProposalR26.control(g))for(ModelGenomeSU2.Member member:g.members)if(member.kind.equals("MATCHING")||member.kind.startsWith("R26"))db.execSQL("UPDATE r26_capability SET state='TRAINED',last_progress=?,reason='Actual sample checkpoint committed; complete experiments and natural qualification pending' WHERE mechanism=?",new Object[]{System.currentTimeMillis(),member.kind.equals("MATCHING")?"F17":member.kind.substring(3)});
            return 0;});}return after-before;
    }
    private static byte[] encode(ModelLiveSU2.Hub hub,ModelLiveSU2 live)throws IOException{ByteArrayOutputStream bytes=new ByteArrayOutputStream();try(DataOutputStream out=new DataOutputStream(new BufferedOutputStream(new CooperativeOutputStreamR26(new GZIPOutputStream(bytes)),32768))){hub.write(out);live.write(out);}return bytes.toByteArray();}
    private static void finish(DbStore store,Supplier<RuntimeCoordinatorR26.Claim> claims,String campaign,long through){
        List<ModelEvolutionSU2.Ranked> best=new ArrayList<>();List<double[]> scores=new ArrayList<>();
        try(Cursor c=store.readable().rawQuery("SELECT canonical_text,dev_n,dev_k,val_n,val_k FROM r26_candidate WHERE campaign_id=? AND status='DONE' ORDER BY CASE WHEN val_n>0 THEN 1.0*val_k/val_n ELSE -1 END DESC,policy_hash LIMIT 3",new String[]{campaign})){while(c.moveToNext()){ModelGenomeSU2 g=ModelGenomeSU2.parse(c.getString(0));best.add(new ModelEvolutionSU2.Ranked(g,"R26_RESEARCH_SELECTION","","EXPOSED_HISTORY; NOT A QUALITY CERTIFICATE"));scores.add(new double[]{c.getInt(1)==0?-1:c.getDouble(2)/c.getInt(1),c.getInt(3)==0?-1:c.getDouble(4)/c.getInt(3)});}}
        store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("R26_CAMPAIGN_FINISH",db->{RuntimeCoordinatorR26.requireFence(db,claims.get());long now=System.currentTimeMillis();for(int i=0;i<best.size();i++){ModelEvolutionSU2.Ranked x=best.get(i);ModelStoreSU2.saveGenome(db,x,campaign,through,scores.get(i)[0],scores.get(i)[1],"RESEARCH",now);/* A new member waits in RESEARCH until cooperative live-state publication is verified.
                Never trigger an unbounded genesis replay on the pre-draw thread. */db.execSQL("UPDATE r26_evolution SET research_accepted=1 WHERE campaign_id=? AND policy_hash=?",new Object[]{campaign,x.g.key()});}db.execSQL("UPDATE r26_foundry SET status='DONE' WHERE campaign_id=?",new Object[]{campaign});
            // Keep two bounded campaign receipts; full label copies and optimizer blobs expire.
            db.execSQL("DELETE FROM r26_candidate WHERE campaign_id IN (SELECT campaign_id FROM r26_foundry WHERE status<>'RUNNING' ORDER BY created_at DESC LIMIT -1 OFFSET 2)");db.execSQL("DELETE FROM r26_foundry WHERE campaign_id NOT IN (SELECT campaign_id FROM r26_foundry ORDER BY created_at DESC LIMIT 2) AND status<>'RUNNING'");return 0;});
    }
}
