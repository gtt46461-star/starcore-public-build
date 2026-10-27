package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.*;
import java.io.*;
import java.util.*;
import java.util.zip.*;

/** Fixed retained-history campaign, private model states and sample-level checkpoints.
 * Scores are research selection only. The original live league supplies future evidence. */
public final class FoundryCampaignR26 {
    private FoundryCampaignR26() {}
    static final class Snapshot {
        long[] ordinal;int[][] draws;String hash;long through;
        byte[] encode()throws IOException{ByteArrayOutputStream b=new ByteArrayOutputStream();try(DataOutputStream o=new DataOutputStream(b)){o.writeInt(draws.length);for(int i=0;i<draws.length;i++){o.writeLong(ordinal[i]);for(int x:draws[i])o.writeByte(x);}}return b.toByteArray();}
        static Snapshot decode(byte[] b)throws IOException{Snapshot s=new Snapshot();try(DataInputStream in=new DataInputStream(new ByteArrayInputStream(b))){int n=in.readInt();if(n<32||n>5000)throw new IOException("R26_SNAPSHOT_SIZE");s.ordinal=new long[n];s.draws=new int[n][10];for(int i=0;i<n;i++){s.ordinal[i]=in.readLong();if(i>0&&s.ordinal[i]<=s.ordinal[i-1])throw new IOException("R26_SNAPSHOT_ORDER");for(int p=0;p<10;p++)s.draws[i][p]=in.readUnsignedByte();OnlineModelsSU2.requireDraw(s.draws[i]);}if(in.read()!=-1)throw new IOException("R26_SNAPSHOT_TRAILING");}s.through=s.ordinal[s.ordinal.length-1];return s;}
    }
    private static Snapshot capture(DbStore store){List<Models.Draw> ds=store.getRecentDraws(5000);if(ds.size()<64)return null;Snapshot s=new Snapshot();s.ordinal=new long[ds.size()];s.draws=new int[ds.size()][10];for(int i=0;i<ds.size();i++){Models.Draw d=ds.get(i);if(!d.isValid())throw new IllegalStateException("R26_BAD_SOURCE");s.ordinal[i]=TargetIssueResolver.sequenceOrdinalSU1(d.issue);for(int p=0;p<10;p++)s.draws[i][p]=d.getNumberAtPosition(p+1);}s.through=s.ordinal[ds.size()-1];return s;}
    /** No model training is executed inside the writer. Returns a count of committed samples. */
    public static long step(DbStore store,RuntimeCoordinatorR26.Claim claim,BudgetTokenSU1 budget)throws IOException{
        SQLiteDatabase r=store.readable();String campaign=null;byte[] snapshotBytes=null;long through=0;
        try(Cursor c=r.rawQuery("SELECT campaign_id,snapshot,source_through FROM r26_foundry WHERE status='RUNNING' ORDER BY created_at LIMIT 1",null)){if(c.moveToFirst()){campaign=c.getString(0);snapshotBytes=c.getBlob(1);through=c.getLong(2);}}
        Models.Draw latest=store.getLatestDraw();if(latest==null)return 0;long latestOrdinal=TargetIssueResolver.sequenceOrdinalSU1(latest.issue);
        if(campaign==null){
            try(Cursor c=r.rawQuery("SELECT max(source_through) FROM r26_foundry WHERE status='DONE'",null)){if(c.moveToFirst()&&!c.isNull(0)&&c.getLong(0)>=latestOrdinal)return 0;}
            budget.checkpointOrYield();Snapshot s=capture(store);if(s==null)return 0;snapshotBytes=s.encode();String hash=PolicyIdentityR26.sha256(Base64.getEncoder().encodeToString(snapshotBytes));campaign=hash;through=s.through;
            List<ModelGenomeSU2> seeds=ModelEvolutionSU2.centralPrograms(true,true);
            for(String family:FamilyRegistryR26.EXECUTABLE)seeds.add(FamilyRegistryR26.genome(family));
            // The exact unary matching head is a real trainable candidate, not a renamed fallback.
            seeds.add(new ModelGenomeSU2(Collections.singletonList(new ModelGenomeSU2.Member("MATCHING",Collections.singletonMap("lr","0.02"),1)),"STATIC",false,"MASS",0.15,0.2,false));
            List<StructuralProposalR26.Proposal> proposals=new ArrayList<>();
            for(ModelGenomeSU2 seed:seeds)proposals.add(new StructuralProposalR26.Proposal(seed,"","REGISTERED_SEED","REGISTERED_SEED"));
            // Preserve actual saved parents; do not inherit their hits or qualification.
            List<ModelGenomeSU2> parents=new ArrayList<>();
            try(Cursor c=r.rawQuery("SELECT canonical_text FROM su2_model_genome WHERE state IN ('LEAGUE','HALL_OF_FAME') AND substr(canonical_text,1,4)='SU2|' ORDER BY coalesce(val_fitness,-1) DESC,genome_key LIMIT 3",null)){while(c.moveToNext())parents.add(ModelGenomeSU2.parse(c.getString(0)));}
            for(int i=0;i<parents.size();i++)proposals.addAll(StructuralProposalR26.from(parents.get(i),seeds.get(i%seeds.size()).members.get(0)));
            final String id=campaign;final byte[] frozen=snapshotBytes;final long source=through;
            store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("R26_CAMPAIGN_BIRTH",db->{RuntimeCoordinatorR26.requireFence(db,claim);db.execSQL("INSERT OR IGNORE INTO r26_foundry VALUES(?,?,?,?,'RUNNING',?)",new Object[]{id,hash,source,frozen,System.currentTimeMillis()});int index=0;for(StructuralProposalR26.Proposal proposal:proposals){ModelGenomeSU2 g=proposal.genome;
                db.execSQL("INSERT OR IGNORE INTO r26_candidate(campaign_id,policy_hash,canonical_text,parent,operation) VALUES(?,?,?,?,?)",new Object[]{id,g.key(),g.text(),proposal.parent,proposal.operation});
                db.execSQL("INSERT OR IGNORE INTO r26_evolution(attempt_id,campaign_id,policy_hash,parent,operation,category,structure_hash) VALUES(?,?,?,?,?,?,?)",new Object[]{id+":"+(index++),id,g.key(),proposal.parent,proposal.operation,proposal.category,StructuralProposalR26.structure(g)});
            }return 0;});
            return 0;
        }
        final String id=campaign;
        // Snapshots cannot pin raw history indefinitely beyond the rolling retention window.
        if(latestOrdinal-through>=5000){store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("R26_CAMPAIGN_EXPIRE",db->{RuntimeCoordinatorR26.requireFence(db,claim);db.execSQL("UPDATE r26_foundry SET status='EXPIRED_RETENTION' WHERE campaign_id=?",new Object[]{id});db.execSQL("DELETE FROM r26_candidate WHERE campaign_id=?",new Object[]{id});return 0;});return 0;}
        String text=null,key=null,parentHash="",expectedStateHash="";byte[] state=null;int cursor=0,dn=0,dk=0,vn=0,vk=0;double loss=0;
        try(Cursor c=r.rawQuery("SELECT canonical_text,policy_hash,cursor,state,dev_n,dev_k,val_n,val_k,loss_sum,checkpoint_hash,state_hash FROM r26_candidate WHERE campaign_id=? AND status='READY' ORDER BY cursor,policy_hash LIMIT 1",new String[]{id})){if(c.moveToFirst()){text=c.getString(0);key=c.getString(1);cursor=c.getInt(2);state=c.isNull(3)?null:c.getBlob(3);dn=c.getInt(4);dk=c.getInt(5);vn=c.getInt(6);vk=c.getInt(7);loss=c.getDouble(8);parentHash=c.getString(9);expectedStateHash=c.getString(10);}}
        if(text==null){finish(store,claim,id,through);return 0;}
        Snapshot snap=Snapshot.decode(snapshotBytes);ModelGenomeSU2 g=ModelGenomeSU2.parse(text);ModelLiveSU2.Hub hub=new ModelLiveSU2.Hub();ModelLiveSU2 live=new ModelLiveSU2(g,hub,ModelLiveLoopSU2.START_META,ModelLiveLoopSU2.WINDOW,ModelLiveLoopSU2.REFIT);
        if(state!=null){if(!PolicyIdentityR26.sha256(Base64.getEncoder().encodeToString(state)).equals(expectedStateHash))throw new IOException("R26_CHECKPOINT_HASH_MISMATCH");try(DataInputStream in=new DataInputStream(new GZIPInputStream(new ByteArrayInputStream(state)))){hub.readInto(in);live.readInto(in);if(in.read()!=-1)throw new IOException("R26_CHECKPOINT_TRAILING");}}
        byte[] lastGood=encode(hub,live);
        int before=cursor;long stop=System.nanoTime()+160_000_000L;int devEnd=snap.draws.length*3/5;
        while(cursor<snap.draws.length&&cursor-before<16&&System.nanoTime()<stop){
            try{budget.checkpointOrYield();}catch(BudgetTokenSU1.Yield yield){break;}
            // A missing issue is never represented as an adjacent observed transition.
            if(cursor>0&&snap.ordinal[cursor]!=snap.ordinal[cursor-1]+1){hub=new ModelLiveSU2.Hub();live=new ModelLiveSU2(g,hub,ModelLiveLoopSU2.START_META,ModelLiveLoopSU2.WINDOW,ModelLiveLoopSU2.REFIT);}
            try {
            ModelLiveSU2.Decision decision=live.decide();float[][] p=live.pendingTable(hub.t);double sampleLoss=0;for(int j=0;j<10;j++)sampleLoss-=Math.log(Math.max(1e-12,p[j][snap.draws[cursor][j]-1]));
            boolean hit=live.settle(snap.draws[cursor]);hub.update(snap.draws[cursor]);
            if(cursor>=32){loss+=sampleLoss/10;if(decision.act){if(cursor<devEnd){dn++;if(hit)dk++;}else{vn++;if(hit)vk++;}}}cursor++;lastGood=encode(hub,live);
            } catch(BudgetTokenSU1.Yield yielded) { break; }
        }
        if(cursor==before)return 0;
        byte[] checkpoint=lastGood;if(checkpoint.length>4<<20)throw new IOException("R26_CHECKPOINT_CAPACITY");
        final String parent=parentHash, stateHash=PolicyIdentityR26.sha256(Base64.getEncoder().encodeToString(checkpoint));
        final String checkpointHash=PolicyIdentityR26.sha256("STARCORE_CHECKPOINT_R26_V1|"+key+"|"+id+"|"+parent+"|"+cursor+"|"+stateHash);
        final String policy=key;final int after=cursor,fdn=dn,fdk=dk,fvn=vn,fvk=vk;final double fl=loss;final String status=after==snap.draws.length?"DONE":"READY";
        store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("R26_CANDIDATE_STEP",db->{RuntimeCoordinatorR26.requireFence(db,claim);db.execSQL("UPDATE r26_candidate SET cursor=?,state=?,state_hash=?,checkpoint_hash=?,dev_n=?,dev_k=?,val_n=?,val_k=?,loss_sum=?,status=? WHERE campaign_id=? AND policy_hash=? AND cursor=? AND checkpoint_hash=?",new Object[]{after,checkpoint,stateHash,checkpointHash,fdn,fdk,fvn,fvk,fl,status,id,policy,before,parent});try(Cursor c=db.rawQuery("SELECT changes()",null)){if(!c.moveToFirst()||c.getInt(0)!=1)throw new IllegalStateException("R26_CANDIDATE_CAS");}
            db.execSQL("INSERT INTO r26_checkpoint_receipt VALUES(?,?,?,?,?,?,'EXPOSED_HISTORY',?)",new Object[]{checkpointHash,policy,id,parent,after,stateHash,System.currentTimeMillis()});
            for(int sample=before;sample<after;sample++)try(Cursor event=db.rawQuery("SELECT event_id,payload FROM r26_feedback WHERE issue_ordinal=?",new String[]{String.valueOf(snap.ordinal[sample])})){if(event.moveToFirst()){
                String[] payload=event.getString(1).split(",");if(payload.length!=11)throw new IllegalStateException("R26_EVENT_PAYLOAD");for(int p=0;p<10;p++)if(Integer.parseInt(payload[p+1])!=snap.draws[sample][p])throw new IllegalStateException("R26_EVENT_SOURCE_CHANGED");
                db.execSQL("INSERT OR IGNORE INTO r26_consumption VALUES(?,?,?,'EXPOSED_HISTORY')",new Object[]{event.getString(0),policy,checkpointHash});
            }}
            for(ModelGenomeSU2.Member member:g.members)if(member.kind.equals("MATCHING")||member.kind.startsWith("R26"))db.execSQL("UPDATE r26_capability SET state='TRAINED',last_progress=?,reason='Actual sample checkpoint committed; complete experiments and natural qualification pending' WHERE mechanism=?",new Object[]{System.currentTimeMillis(),member.kind.equals("MATCHING")?"F17":member.kind.substring(3)});
            return 0;});return after-before;
    }
    private static byte[] encode(ModelLiveSU2.Hub hub,ModelLiveSU2 live)throws IOException{ByteArrayOutputStream bytes=new ByteArrayOutputStream();try(DataOutputStream out=new DataOutputStream(new GZIPOutputStream(bytes))){hub.write(out);live.write(out);}return bytes.toByteArray();}
    private static void finish(DbStore store,RuntimeCoordinatorR26.Claim claim,String campaign,long through){
        List<ModelEvolutionSU2.Ranked> best=new ArrayList<>();List<double[]> scores=new ArrayList<>();
        try(Cursor c=store.readable().rawQuery("SELECT canonical_text,dev_n,dev_k,val_n,val_k FROM r26_candidate WHERE campaign_id=? AND status='DONE' ORDER BY CASE WHEN val_n>0 THEN 1.0*val_k/val_n ELSE -1 END DESC,policy_hash LIMIT 3",new String[]{campaign})){while(c.moveToNext()){ModelGenomeSU2 g=ModelGenomeSU2.parse(c.getString(0));best.add(new ModelEvolutionSU2.Ranked(g,"R26_RESEARCH_SELECTION","","EXPOSED_HISTORY; NOT A QUALITY CERTIFICATE"));scores.add(new double[]{c.getInt(1)==0?-1:c.getDouble(2)/c.getInt(1),c.getInt(3)==0?-1:c.getDouble(4)/c.getInt(3)});}}
        store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("R26_CAMPAIGN_FINISH",db->{RuntimeCoordinatorR26.requireFence(db,claim);long now=System.currentTimeMillis();for(int i=0;i<best.size();i++){ModelEvolutionSU2.Ranked x=best.get(i);ModelStoreSU2.saveGenome(db,x,campaign,through,scores.get(i)[0],scores.get(i)[1],"RESEARCH",now);/* A new member waits in RESEARCH until cooperative live-state publication is verified.
                Never trigger an unbounded genesis replay on the pre-draw thread. */db.execSQL("UPDATE r26_evolution SET research_accepted=1 WHERE campaign_id=? AND policy_hash=?",new Object[]{campaign,x.g.key()});}db.execSQL("UPDATE r26_foundry SET status='DONE' WHERE campaign_id=?",new Object[]{campaign});
            // Keep two bounded campaign receipts; full label copies and optimizer blobs expire.
            db.execSQL("DELETE FROM r26_candidate WHERE campaign_id IN (SELECT campaign_id FROM r26_foundry WHERE status<>'RUNNING' ORDER BY created_at DESC LIMIT -1 OFFSET 2)");db.execSQL("DELETE FROM r26_foundry WHERE campaign_id NOT IN (SELECT campaign_id FROM r26_foundry ORDER BY created_at DESC LIMIT 2) AND status<>'RUNNING'");return 0;});
    }
}
