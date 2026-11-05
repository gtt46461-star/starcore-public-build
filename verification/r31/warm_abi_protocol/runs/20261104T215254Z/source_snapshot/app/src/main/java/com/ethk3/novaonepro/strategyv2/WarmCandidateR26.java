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
    public static final int PRESERVATION_ROWS_R31=8;
    public static final class PreparedAbiR31 implements AutoCloseable {
        private final List<String[]> rows;private final long version;private MemoryBudgetR31.Lease lease;
        private PreparedAbiR31(List<String[]> rows,long version,MemoryBudgetR31.Lease lease){this.rows=Collections.unmodifiableList(rows);this.version=version;this.lease=lease;}
        public List<String> campaigns(){List<String> out=new ArrayList<>();for(String[] row:rows)out.add(row[0]);return Collections.unmodifiableList(out);}
        public void close(){if(lease!=null){lease.close();lease=null;}}
    }
    public static PreparedAbiR31 prepareAbiR31(SQLiteDatabase read,String currentAbi){
        MemoryBudgetR31.Admission admission=MemoryBudgetR31.processBudget().tryAcquire("warm-campaign-abi",MemoryBudgetR31.Kind.QUEUED,65536,()->Thread.currentThread().isInterrupted());if(!admission.acquired())throw new MemoryBudgetR31.CapacityWait(admission.reason);boolean retained=false;
        try{long version=scalar(read,"PRAGMA schema_version");WarmPublicationR31.requireFoundryArchiveR31(read);List<String[]> rows=new ArrayList<>();try(Cursor c=read.rawQuery("SELECT campaign_id,numerical_abi,snapshot_hash,source_through,length(snapshot) FROM r26_foundry WHERE status='RUNNING' AND numerical_abi<>? ORDER BY created_at,campaign_id LIMIT 4",new String[]{currentAbi})){while(c.moveToNext()){if(c.getLong(4)>524288)throw new ModelPublicationR31.PublicationDependencyR31("WARM_OLD_SNAPSHOT_OUTSIDE_VALID_BOUND");rows.add(new String[]{c.getString(0),c.getString(1),c.getString(2),c.getString(3)});}}if(scalar(read,"PRAGMA schema_version")!=version)throw new DatabaseCoordinator.SchemaPendingR31("R31_WARM_ABI","SCHEMA_CHANGED_DURING_PREPARE");PreparedAbiR31 prepared=new PreparedAbiR31(rows,version,admission.lease);retained=true;return prepared;}finally{if(!retained)admission.lease.close();}
    }
    public static void applyPreparedAbiR31(SQLiteDatabase db,PreparedAbiR31 prepared){
        if(!db.inTransaction()||prepared==null||prepared.lease==null)throw new IllegalStateException("R31_WARM_PREPARED_ABI_WRITER_REQUIRED");if(scalar(db,"PRAGMA schema_version")!=prepared.version)throw new DatabaseCoordinator.SchemaPendingR31("R31_WARM_ABI","SCHEMA_CHANGED_AFTER_PREPARE");
        for(String[] row:prepared.rows)db.execSQL("UPDATE r26_foundry SET status='ABANDONED_NUMERICAL_ABI',snapshot=X'' WHERE campaign_id=? AND status='RUNNING' AND numerical_abi=? AND snapshot_hash=? AND source_through=?",new Object[]{row[0],row[1],row[2],Long.parseLong(row[3])});
    }
    public static final class PreparedPreservationR31 implements AutoCloseable {
        private final List<String[]> rows;public final boolean more;private final long schemaVersion;private MemoryBudgetR31.Lease lease;
        private PreparedPreservationR31(List<String[]> rows,boolean more,long schemaVersion,MemoryBudgetR31.Lease lease){this.rows=Collections.unmodifiableList(rows);this.more=more;this.schemaVersion=schemaVersion;this.lease=lease;}
        public int rows(){return rows.size();}public void close(){if(lease!=null){lease.close();lease=null;}}
    }
    /** Read-side preparation for all live callers. The ninth row is only a continuation probe. */
    public static PreparedPreservationR31 preparePreservationR31(SQLiteDatabase read){
        return preparePreservationR31(read,Collections.emptyList());
    }
    /** Expiry/ABI callers may include the at-most-four campaigns they will transition in their transaction. */
    public static PreparedPreservationR31 preparePreservationR31(SQLiteDatabase read,List<String> expiringCampaigns){
        if(expiringCampaigns==null||expiringCampaigns.size()>4)throw new IllegalArgumentException("R31_WARM_EXPIRY_CAMPAIGN_BOUND");
        MemoryBudgetR31.Admission admission=MemoryBudgetR31.processBudget().tryAcquire("warm-preservation",MemoryBudgetR31.Kind.QUEUED,262144,()->Thread.currentThread().isInterrupted());if(!admission.acquired())throw new MemoryBudgetR31.CapacityWait(admission.reason);boolean retained=false;
        try{
            long version=scalar(read,"PRAGMA schema_version");WarmPublicationR31.requireCandidateArchiveR31(read);
            String learned="length(c.state)>0 AND (c.cursor>0 OR instr(c.canonical_text,'_warm_kernel=')>0)",published="EXISTS(SELECT 1 FROM r26_live_model l WHERE l.policy_hash=c.policy_hash)";
            String action="CASE WHEN c.state IS NULL OR length(c.state)=0 OR c.cursor=0 AND instr(c.canonical_text,'_warm_kernel=')=0 OR "+published+" THEN 3 WHEN "+learned+" AND NOT "+published+" AND f.numerical_abi<>? AND c.status<>'BLOCKED_WARM_NUMERICAL_ABI_PRESERVED' THEN 2 WHEN "+learned+" AND NOT "+published+" AND f.numerical_abi=? AND c.status NOT LIKE 'BLOCKED_WARM_%' AND c.status<>'"+WAITING+"' THEN 1 ELSE 0 END";
            List<String[]> rows=new ArrayList<>();boolean more=false;String abi=ResearchSemanticsGeneratedR967.TRAINING_RULE_SHA256;List<String> args=new ArrayList<>(Arrays.asList(abi,abi));String eligibility="f.status IN ('EXPIRED_RETENTION','ABANDONED_NUMERICAL_ABI')";if(!expiringCampaigns.isEmpty()){eligibility+=" OR f.campaign_id IN ("+String.join(",",Collections.nCopies(expiringCampaigns.size(),"?"))+")";args.addAll(expiringCampaigns);}
            try(Cursor c=read.rawQuery("SELECT * FROM (SELECT c.campaign_id,c.policy_hash,c.checkpoint_hash,c.state_hash,c.cursor,c.status,f.numerical_abi,f.snapshot_hash,f.source_through,f.created_at,length(c.state) AS state_bytes,"+action+" AS action FROM r26_candidate c JOIN r26_foundry f ON f.campaign_id=c.campaign_id WHERE "+eligibility+") WHERE action>0 ORDER BY created_at,campaign_id,policy_hash LIMIT 9",args.toArray(new String[0]))){
                while(c.moveToNext()){if(rows.size()==PRESERVATION_ROWS_R31){more=true;break;}if(!c.isNull(10)&&c.getLong(10)>(4L<<20))throw new ModelPublicationR31.PublicationDependencyR31("WARM_OLD_STATE_OUTSIDE_DECLARED_VALID_BOUND");String[] row=new String[12];long bytes=0;for(int i=0;i<row.length;i++){row[i]=c.getString(i);if(row[i]!=null)bytes+=row[i].getBytes(java.nio.charset.StandardCharsets.UTF_8).length;}if(bytes>2048)throw new ModelPublicationR31.PublicationDependencyR31("WARM_PRESERVATION_METADATA_BOUND");rows.add(row);}
            }
            if(scalar(read,"PRAGMA schema_version")!=version)throw new DatabaseCoordinator.SchemaPendingR31("R31_WARM_PRESERVATION","SCHEMA_CHANGED_DURING_PREPARE");PreparedPreservationR31 prepared=new PreparedPreservationR31(rows,more,version,admission.lease);retained=true;return prepared;
        }finally{if(!retained)admission.lease.close();}
    }
    public static final class PreparedInitializerPruneR31 implements AutoCloseable {
        private final List<String[]> rows;private final long schemaVersion;public final boolean more;private MemoryBudgetR31.Lease lease;
        PreparedInitializerPruneR31(List<String[]> rows,long version,boolean more,MemoryBudgetR31.Lease lease){this.rows=Collections.unmodifiableList(rows);schemaVersion=version;this.more=more;this.lease=lease;}
        public int rows(){return rows.size();}public void close(){if(lease!=null){lease.close();lease=null;}}
    }
    private static final String NO_INITIALIZER_OWNER_R31="NOT EXISTS(SELECT 1 FROM r26_candidate c INDEXED BY r31_warm_candidate_state_owner WHERE c.policy_hash=i.policy_hash AND length(c.state)>0) AND NOT EXISTS(SELECT 1 FROM r26_live_model l WHERE l.policy_hash=i.policy_hash) AND NOT EXISTS(SELECT 1 FROM su2_model_genome g WHERE g.genome_key=i.policy_hash) AND NOT EXISTS(SELECT 1 FROM r26_cohort q WHERE q.genome_key=i.policy_hash)";
    public static PreparedInitializerPruneR31 prepareInitializerPruneR31(SQLiteDatabase read){
        MemoryBudgetR31.Admission admission=MemoryBudgetR31.processBudget().tryAcquire("warm-initializer-prune",MemoryBudgetR31.Kind.QUEUED,65536,()->Thread.currentThread().isInterrupted());if(!admission.acquired())throw new MemoryBudgetR31.CapacityWait(admission.reason);boolean retained=false;
        try{long version=scalar(read,"PRAGMA schema_version");requireInitializerPruneSchemaR31(read);List<String[]> rows=new ArrayList<>();boolean more=false;try(Cursor c=read.rawQuery("SELECT policy_hash,initializer_hash,seed_checkpoint FROM r26_warm_initialization i WHERE "+NO_INITIALIZER_OWNER_R31+" ORDER BY policy_hash LIMIT 9",null)){while(c.moveToNext()){if(rows.size()==8){more=true;break;}rows.add(new String[]{c.getString(0),c.getString(1),c.getString(2)});}}if(scalar(read,"PRAGMA schema_version")!=version)throw new DatabaseCoordinator.SchemaPendingR31("R31_WARM_PRUNE","SCHEMA_CHANGED_DURING_PREPARE");PreparedInitializerPruneR31 prepared=new PreparedInitializerPruneR31(rows,version,more,admission.lease);retained=true;return prepared;}finally{if(!retained)admission.lease.close();}
    }
    private static void requireInitializerPruneSchemaR31(SQLiteDatabase db){WarmPublicationR31.requireIndexR31(db);DatabaseCoordinator.requireSchemaObjectsR31(db,"R31_WARM_INITIALIZER_RETENTION",new String[][]{{"r26_warm_initialization","policy_hash,initializer_hash,seed_checkpoint"},{"su2_model_genome","genome_key"},{"r26_cohort","genome_key"}},new String[0][]);}
    public static int applyPreparedInitializerPruneR31(SQLiteDatabase db,PreparedInitializerPruneR31 prepared){
        if(!db.inTransaction())throw new IllegalStateException("R26_WARM_INITIALIZER_RETENTION_WRITER");if(prepared==null||prepared.lease==null)throw new IllegalStateException("R31_WARM_PREPARATION_REQUIRED");if(scalar(db,"PRAGMA schema_version")!=prepared.schemaVersion)throw new DatabaseCoordinator.SchemaPendingR31("R31_WARM_PRUNE","SCHEMA_CHANGED_AFTER_PREPARE");int changed=0;
        for(String[] row:prepared.rows){db.execSQL("DELETE FROM r26_warm_initialization WHERE policy_hash=? AND initializer_hash=? AND seed_checkpoint=? AND policy_hash IN (SELECT policy_hash FROM r26_warm_initialization i WHERE i.policy_hash=? AND "+NO_INITIALIZER_OWNER_R31+")",new Object[]{row[0],row[1],row[2],row[0]});changed+=(int)scalar(db,"SELECT changes()");}return changed;
    }
    /** Each statement targets one immutable prepared PK and rechecks its source. No DDL or codec work. */
    public static int applyPreparedPreservationR31(SQLiteDatabase db,PreparedPreservationR31 prepared){
        if(!db.inTransaction())throw new IllegalStateException("R26_WARM_RETENTION_WRITER");if(prepared==null||prepared.lease==null)throw new IllegalStateException("R31_WARM_PREPARATION_REQUIRED");if(scalar(db,"PRAGMA schema_version")!=prepared.schemaVersion)throw new DatabaseCoordinator.SchemaPendingR31("R31_WARM_PRESERVATION","SCHEMA_CHANGED_AFTER_PREPARE");int changed=0;
        String source=" AND EXISTS(SELECT 1 FROM r26_foundry f WHERE f.campaign_id=r26_candidate.campaign_id AND f.status IN ('EXPIRED_RETENTION','ABANDONED_NUMERICAL_ABI') AND f.numerical_abi=? AND f.snapshot_hash=? AND f.source_through=?)";
        for(String[] row:prepared.rows){
            int action=Integer.parseInt(row[11]);String conditions=" WHERE campaign_id=? AND policy_hash=? AND checkpoint_hash=? AND state_hash=? AND cursor=? AND status=?"+source;Object[] args={row[0],row[1],row[2],row[3],Long.parseLong(row[4]),row[5],row[6],row[7],Long.parseLong(row[8])};
            if(action==3)db.execSQL("DELETE FROM r26_candidate"+conditions+" AND (state IS NULL OR length(state)=0 OR cursor=0 AND instr(canonical_text,'_warm_kernel=')=0 OR EXISTS(SELECT 1 FROM r26_live_model l WHERE l.policy_hash=r26_candidate.policy_hash))",args);
            else{String state=action==1?WAITING:"BLOCKED_WARM_NUMERICAL_ABI_PRESERVED";Object[] values=new Object[args.length+1];values[0]=state;System.arraycopy(args,0,values,1,args.length);db.execSQL("UPDATE r26_candidate SET status=?"+conditions+" AND length(state)>0 AND (cursor>0 OR instr(canonical_text,'_warm_kernel=')>0) AND NOT EXISTS(SELECT 1 FROM r26_live_model l WHERE l.policy_hash=r26_candidate.policy_hash)",values);}
            changed+=(int)scalar(db,"SELECT changes()");
        }return changed;
    }
    /** The caller already owns the existing atomic writer. No numerical work here. */
    public static void preserveExpired(SQLiteDatabase db){
        if(!db.inTransaction())throw new IllegalStateException("R26_WARM_RETENTION_WRITER");
        // Compatibility adapter. Live callers prepare on their read worker and call applyPrepared directly.
        try(PreparedPreservationR31 prepared=preparePreservationR31(db)){applyPreparedPreservationR31(db,prepared);}
    }
    public static String seedCheckpoint(String policy,String campaign,String parent,String initializer,String state,long through){return HashingSU1.fields("R26_WARM_SEED_CHECKPOINT_V1",policy,campaign,parent,initializer,state,Long.toString(through));}
    static final class Parent {String campaign,policy,text,hash,checkpoint,operation,manifestParent,birthHash;byte[] reference;int cursor;long through,exposure;}
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
        budget.checkpointOrYield();SQLiteDatabase db=store.readable();WarmPublicationR31.requireSchemaR31(db);ModelPublicationR31.requireSchemaR31(db);
        RuntimeCoordinatorR26.Claim claim=claims.get();long total=scalar(db,"SELECT completed FROM r26_work_total WHERE task_id='warm-candidates'");if(total>=claim.cursor)claim.advanceCursorTo(total);long carry=Math.max(0,total-claim.cursor);
        Parent p=parent(db);if(p==null)return carry;
        long sourceGeneration=EventFrameBuilderSU1.generation(db);FoundryCampaignR26.Snapshot source;byte[] snapshot;String sourceHash;WarmModelSeedR26.OwnedSeedR31 owned=null;
        try{
            authenticate(db,p);source=retainedSuffix(FoundryCampaignR26.capture(store),p.exposure);
            if(source==null){claim.reason("WAITING_64_RETAINED_CANONICAL_LABELS_STRICTLY_AFTER_PARENT; WEIGHTS_PRESERVED");return carry;}
            snapshot=source.encode();sourceHash=LiveStateR26.hash(snapshot);byte[] parentState=CheckpointArtifactsR26.resolve(store,p.reference);
            owned=WarmModelSeedR26.prepareOwnedR31(p.text,parentState,p.hash,p.checkpoint,p.through,sourceHash,source.ordinal[0],source.through);
            WarmModelSeedR26.Seed seed=owned.seed();if(seed.parentSamples!=p.cursor||seed.parentExposedThrough!=p.exposure)throw new IOException("R26_WARM_PARENT_SAMPLE_OR_EXPOSURE_COUNT");
        }catch(IOException|IllegalArgumentException invalid){
            if(owned!=null)owned.close();final String reason="WARM_PARENT_PRESERVED:"+invalid.getClass().getSimpleName()+":"+String.valueOf(invalid.getMessage());
            store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("R26_WARM_PARENT_BLOCKED",w->{RuntimeCoordinatorR26.requireFence(w,claims.get());w.execSQL("UPDATE r26_candidate SET status='BLOCKED_WARM_CHECKPOINT_PRESERVED' WHERE campaign_id=? AND policy_hash=? AND checkpoint_hash=? AND status=?",new Object[]{p.campaign,p.policy,p.checkpoint,WAITING});w.execSQL("INSERT OR REPLACE INTO r26_live_failure VALUES(?,?,?,?)",new Object[]{p.policy,p.through,reason,System.currentTimeMillis()});return 0;});claim.reason(reason);return carry;
        }
        WarmPublicationR31.Prepared publication;
        try(WarmModelSeedR26.OwnedSeedR31 actual=owned){budget.checkpointOrYield();if(EventFrameBuilderSU1.generation(db)!=sourceGeneration)throw new ModelPublicationR31.PublicationDependencyR31("WARM_SOURCE_CHANGED_DURING_PREPARE");publication=WarmPublicationR31.prepareR31(store,claim,p,actual.seed(),source,snapshot,sourceHash,sourceGeneration);}
        // Numerical objects and their working declaration exit before the persistence pipeline.
        try(WarmPublicationR31.Prepared actual=publication){long committed=actual.publish(store,claim);claim.advanceCursorTo(Math.max(claim.cursor,committed));return Math.max(0,committed-claim.cursor);}
    }
    public static void pruneInitializers(SQLiteDatabase db){
        if(!db.inTransaction())throw new IllegalStateException("R26_WARM_INITIALIZER_RETENTION_WRITER");
        try(PreparedInitializerPruneR31 prepared=prepareInitializerPruneR31(db)){applyPreparedInitializerPruneR31(db,prepared);}
    }
}
