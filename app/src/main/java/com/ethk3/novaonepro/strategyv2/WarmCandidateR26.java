package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.*;
import java.io.*;
import java.util.*;
import java.util.function.Supplier;

/** Expiring a raw source never erases an unpublished learned kernel. A fenced P2
 * operation creates a new policy from a verified initializer and a strictly later source. */
public final class WarmCandidateR26 {
    public static final String WAITING="WAITING_WARM_FORK";
    private WarmCandidateR26(){}
    private static long scalar(SQLiteDatabase db,String sql,String...args){try(Cursor c=db.rawQuery(sql,args.length==0?null:args)){return c.moveToFirst()&&!c.isNull(0)?c.getLong(0):0;}}
    public static boolean installed(SQLiteDatabase db){return scalar(db,"SELECT count(*) FROM sqlite_master WHERE type='table' AND name='r26_warm_initialization'")==1;}
    /** The caller already owns the existing atomic writer. No numerical work here. */
    public static void preserveExpired(SQLiteDatabase db){
        if(!db.inTransaction())throw new IllegalStateException("R26_WARM_RETENTION_WRITER");
        String expired="SELECT campaign_id FROM r26_foundry WHERE status IN ('EXPIRED_RETENTION','ABANDONED_NUMERICAL_ABI')";
        String learned="length(state)>0 AND (cursor>0 OR instr(canonical_text,'_warm_kernel=')>0)";
        String notPublished="NOT EXISTS(SELECT 1 FROM r26_live_model l WHERE l.policy_hash=r26_candidate.policy_hash)";
        db.execSQL("UPDATE r26_candidate SET status='"+WAITING+"' WHERE campaign_id IN ("+expired+") AND "+learned+" AND "+notPublished+" AND status NOT LIKE 'BLOCKED_WARM_%' AND campaign_id IN (SELECT campaign_id FROM r26_foundry WHERE numerical_abi=?)",new Object[]{ResearchSemanticsGeneratedR967.TRAINING_RULE_SHA256});
        db.execSQL("UPDATE r26_candidate SET status='BLOCKED_WARM_NUMERICAL_ABI_PRESERVED' WHERE campaign_id IN ("+expired+") AND "+learned+" AND "+notPublished+" AND campaign_id IN (SELECT campaign_id FROM r26_foundry WHERE numerical_abi<>?)",new Object[]{ResearchSemanticsGeneratedR967.TRAINING_RULE_SHA256});
        db.execSQL("DELETE FROM r26_candidate WHERE campaign_id IN ("+expired+") AND (state IS NULL OR length(state)=0 OR cursor=0 AND instr(canonical_text,'_warm_kernel=')=0 OR EXISTS(SELECT 1 FROM r26_live_model l WHERE l.policy_hash=r26_candidate.policy_hash))");
    }
    public static String seedCheckpoint(String policy,String campaign,String parent,String initializer,String state,long through){return HashingSU1.fields("R26_WARM_SEED_CHECKPOINT_V1",policy,campaign,parent,initializer,state,Long.toString(through));}
    private static final class Parent {String campaign,policy,text,hash,checkpoint,operation,manifestParent,birthHash;byte[] reference;int cursor;long through,exposure;}
    private static Parent parent(SQLiteDatabase db){
        try(Cursor c=db.rawQuery("SELECT c.campaign_id,c.policy_hash,c.canonical_text,c.state,c.state_hash,c.checkpoint_hash,c.cursor,c.operation,f.source_through,f.snapshot_hash FROM r26_candidate c JOIN r26_foundry f ON f.campaign_id=c.campaign_id WHERE c.status=? AND f.status='EXPIRED_RETENTION' AND f.numerical_abi=? AND NOT EXISTS(SELECT 1 FROM r26_live_model l WHERE l.policy_hash=c.policy_hash) ORDER BY f.created_at,c.policy_hash LIMIT 1",new String[]{WAITING,ResearchSemanticsGeneratedR967.TRAINING_RULE_SHA256})){
            if(!c.moveToFirst())return null;Parent p=new Parent();p.campaign=c.getString(0);p.policy=c.getString(1);p.text=c.getString(2);p.reference=c.getBlob(3);p.hash=c.getString(4);p.checkpoint=c.getString(5);p.cursor=c.getInt(6);p.operation=c.getString(7);p.exposure=c.getLong(8);p.birthHash=c.getString(9);return p;
        }
    }
    private static void authenticate(SQLiteDatabase db,Parent p)throws IOException{
        ModelGenomeSU2 g=ModelGenomeSU2.parse(p.text);if(!CurrentPolicyR26.matches(p.text,p.policy)||!g.key().equals(p.policy))throw new IOException("R26_WARM_PARENT_POLICY");
        try(Cursor c=db.rawQuery("SELECT artifact_hash,policy_hash,source_hash,parent_hash,trained_through FROM r26_checkpoint_artifact WHERE checkpoint_hash=?",new String[]{p.checkpoint})){
            if(!c.moveToFirst()||!c.getString(0).equals(CheckpointArtifactsR26.referenceHash(p.reference))||!p.policy.equals(c.getString(1))||!p.campaign.equals(c.getString(2)))throw new IOException("R26_WARM_PARENT_MANIFEST");p.manifestParent=c.getString(3);p.through=c.getLong(4);
        }
        for(ModelGenomeSU2.Member member:g.members)if(!p.birthHash.equals(member.params.get("_birth_snapshot"))||!Long.toString(p.exposure).equals(member.params.get("_birth_through")))throw new IOException("R26_WARM_PARENT_SOURCE_BINDING");
        if(p.cursor<0||p.through<0||p.exposure<p.through)throw new IOException("R26_WARM_PARENT_CURSOR");
        if(p.cursor==0){
            if(!WarmModelSeedR26.has(g)||p.through!=Long.parseLong(g.members.get(0).params.get("_warm_through")))throw new IOException("R26_WARM_SEED_PARENT_CURSOR");
            String expected=seedCheckpoint(p.policy,p.campaign,g.members.get(0).params.get("_warm_checkpoint"),g.members.get(0).params.get("_warm_kernel"),p.hash,p.through);
            if(!expected.equals(p.checkpoint)||scalar(db,"SELECT count(*) FROM r26_warm_initialization WHERE policy_hash=? AND seed_checkpoint=? AND initializer_hash=?",p.policy,p.checkpoint,g.members.get(0).params.get("_warm_kernel"))!=1)throw new IOException("R26_WARM_SEED_PARENT_MANIFEST");
        }else{
            String expected=PolicyIdentityR26.sha256("STARCORE_CHECKPOINT_R26_V1|"+p.policy+"|"+p.campaign+"|"+p.manifestParent+"|"+p.cursor+"|"+p.hash);
            if(!expected.equals(p.checkpoint))throw new IOException("R26_WARM_PARENT_CHECKPOINT_BINDING");
        }
    }
    static FoundryCampaignR26.Snapshot retainedSuffix(FoundryCampaignR26.Snapshot source,long through){
        if(source==null)return null;List<Integer> keep=new ArrayList<>();long cut=source.through-4999;
        for(int i=0;i<source.draws.length;i++)if(source.ordinal[i]>through&&source.ordinal[i]>=cut)keep.add(i);
        if(keep.size()<64)return null;int n=keep.size();FoundryCampaignR26.Snapshot s=new FoundryCampaignR26.Snapshot();s.ordinal=new long[n];s.issues=new long[n];s.opened=new long[n];s.available=new long[n];s.draws=new int[n][];
        for(int i=0;i<n;i++){int k=keep.get(i);s.ordinal[i]=source.ordinal[k];s.issues[i]=source.issues[k];s.opened[i]=source.opened[k];s.available[i]=source.available[k];s.draws[i]=source.draws[k].clone();}s.through=s.ordinal[n-1];return s;
    }
    public static long step(DbStore store,Supplier<RuntimeCoordinatorR26.Claim> claims,BudgetTokenSU1 budget)throws IOException{
        budget.checkpointOrYield();SQLiteDatabase db=LegacyResearchScopeR29.observationReader(store);Parent p=parent(db);if(p==null)return 0;
        FoundryCampaignR26.Snapshot source;WarmModelSeedR26.Seed seed;byte[] snapshot;String sourceHash;
        try{
            authenticate(db,p);source=retainedSuffix(FoundryCampaignR26.capture(store),p.exposure);
            if(source==null){claims.get().reason("WAITING_64_RETAINED_CANONICAL_LABELS_STRICTLY_AFTER_PARENT; WEIGHTS_PRESERVED");return 0;}
            snapshot=source.encode();sourceHash=LiveStateR26.hash(snapshot);byte[] parentState=CheckpointArtifactsR26.resolve(store,p.reference);
            seed=WarmModelSeedR26.prepare(p.text,parentState,p.hash,p.checkpoint,p.through,sourceHash,source.ordinal[0],source.through);
            if(seed.parentSamples!=p.cursor||seed.parentExposedThrough!=p.exposure)throw new IOException("R26_WARM_PARENT_SAMPLE_OR_EXPOSURE_COUNT");
        }catch(IOException|IllegalArgumentException invalid){
            final String reason="WARM_PARENT_PRESERVED:"+invalid.getClass().getSimpleName()+":"+String.valueOf(invalid.getMessage());
            store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("R26_WARM_PARENT_BLOCKED",w->{RuntimeCoordinatorR26.requireFence(w,claims.get());w.execSQL("UPDATE r26_candidate SET status='BLOCKED_WARM_CHECKPOINT_PRESERVED' WHERE campaign_id=? AND policy_hash=? AND checkpoint_hash=? AND status=?",new Object[]{p.campaign,p.policy,p.checkpoint,WAITING});w.execSQL("INSERT OR REPLACE INTO r26_live_failure VALUES(?,?,?,?)",new Object[]{p.policy,p.through,reason,System.currentTimeMillis()});return 0;});claims.get().reason(reason);return 0;
        }
        byte[] initialized=seed.state(),initializer=seed.initializer();String stateHash=LiveStateR26.hash(initialized),child=seed.genome.key();String campaign=HashingSU1.fields("R26_WARM_CAMPAIGN_V1",p.policy,p.checkpoint,sourceHash,child);
        String checkpoint=seedCheckpoint(child,campaign,p.checkpoint,seed.initializerHash,stateHash,p.through);budget.checkpointOrYield();
        try(CheckpointArtifactsR26.Prepared stateFile=CheckpointArtifactsR26.prepareLease(store,initialized);CheckpointArtifactsR26.Prepared initFile=CheckpointArtifactsR26.prepareLease(store,initializer)){
            return store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("R26_WARM_CHILD_BIRTH",w->{
                RuntimeCoordinatorR26.requireFence(w,claims.get());
                try(Cursor current=w.rawQuery("SELECT 1 FROM r26_candidate c JOIN r26_foundry f ON f.campaign_id=c.campaign_id WHERE c.campaign_id=? AND c.policy_hash=? AND c.checkpoint_hash=? AND c.state_hash=? AND c.cursor=? AND c.status=? AND f.status='EXPIRED_RETENTION' AND f.numerical_abi=? AND f.snapshot_hash=? AND f.source_through=? AND NOT EXISTS(SELECT 1 FROM r26_live_model l WHERE l.policy_hash=c.policy_hash)",new String[]{p.campaign,p.policy,p.checkpoint,p.hash,Integer.toString(p.cursor),WAITING,ResearchSemanticsGeneratedR967.TRAINING_RULE_SHA256,p.birthHash,Long.toString(p.exposure)})){if(!current.moveToFirst())throw new IllegalStateException("R26_WARM_PARENT_CAS");}
                try(Cursor latest=w.rawQuery("SELECT issue FROM draws WHERE dirty=0 ORDER BY issue DESC LIMIT 1",null)){if(!latest.moveToFirst()||TargetIssueResolver.sequenceOrdinalSU1(latest.getString(0))-source.ordinal[0]>=5000)throw new IllegalStateException("R26_WARM_SOURCE_EXPIRED_BEFORE_BIRTH");}
                long now=System.currentTimeMillis();PolicyIdentityLedgerR26.record(w,p.text,p.policy);PolicyIdentityLedgerR26.record(w,seed.genome.text(),child);
                w.execSQL("INSERT INTO r26_foundry(campaign_id,snapshot_hash,source_through,snapshot,status,created_at,numerical_abi,source_first) VALUES(?,?,?,?,'RUNNING',?,?,?)",new Object[]{campaign,sourceHash,source.through,snapshot,now,ResearchSemanticsGeneratedR967.TRAINING_RULE_SHA256,source.ordinal[0]});
                String operation=StructuralProposalR26.control(seed.genome)?p.operation:"WARM_KERNEL_CONTINUATION";
                w.execSQL("INSERT INTO r26_candidate(campaign_id,policy_hash,canonical_text,cursor,state,state_hash,checkpoint_hash,status,parent,operation) VALUES(?,?,?,0,?,?,?,'READY',?,?)",new Object[]{campaign,child,seed.genome.text(),stateFile.reference(),stateHash,checkpoint,p.policy,operation});
                w.execSQL("INSERT INTO r26_warm_initialization(policy_hash,parent_policy,parent_checkpoint,parent_state_hash,parent_trained_through,parent_exposed_through,initializer_hash,state,child_campaign,seed_checkpoint,numerical_abi,created_at) VALUES(?,?,?,?,?,?,?,?,?,?,?,?)",new Object[]{child,p.policy,p.checkpoint,p.hash,p.through,p.exposure,seed.initializerHash,initFile.reference(),campaign,checkpoint,ResearchSemanticsGeneratedR967.TRAINING_RULE_SHA256,now});
                CheckpointManifestR26.record(w,checkpoint,child,campaign,p.checkpoint,p.through,stateFile.reference());
                w.execSQL("INSERT INTO r26_evolution(attempt_id,campaign_id,policy_hash,parent,operation,category,structure_hash) VALUES(?,?,?,?,?,'VERIFIED_PAST_KERNEL_INITIALIZATION',?)",new Object[]{campaign+":0",campaign,child,p.policy,operation,StructuralProposalR26.structure(seed.genome)});
                w.execSQL("DELETE FROM r26_candidate WHERE campaign_id=? AND policy_hash=? AND checkpoint_hash=? AND status=?",new Object[]{p.campaign,p.policy,p.checkpoint,WAITING});if(scalar(w,"SELECT changes()")!=1)throw new IllegalStateException("R26_WARM_TRANSFER_CAS");
                return 1L;
            });
        }
    }
    public static void pruneInitializers(SQLiteDatabase db){
        if(!db.inTransaction())throw new IllegalStateException("R26_WARM_INITIALIZER_RETENTION_WRITER");
        if(!installed(db))return;
        db.execSQL("DELETE FROM r26_warm_initialization WHERE policy_hash IN (SELECT policy_hash FROM r26_warm_initialization i WHERE NOT EXISTS(SELECT 1 FROM r26_candidate c WHERE c.policy_hash=i.policy_hash AND length(c.state)>0) AND NOT EXISTS(SELECT 1 FROM r26_live_model l WHERE l.policy_hash=i.policy_hash) LIMIT 64)");
    }
}
