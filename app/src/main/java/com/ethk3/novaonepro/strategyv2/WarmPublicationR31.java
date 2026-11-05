package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.DatabaseCoordinator;
import com.ethk3.novaonepro.DbStore;
import com.ethk3.novaonepro.ResearchSemanticsGeneratedR967;
import com.ethk3.novaonepro.TargetIssueResolver;
import java.io.*;
import java.util.*;

/** The existing warm fork, prepared on its original worker and atomically
 * published through the closed R31 model publication dispatcher. No business DDL. */
public final class WarmPublicationR31 {
    private WarmPublicationR31(){}
    public static final int REVISION=1;
    public static final String INDEX_DDL_R31="CREATE INDEX IF NOT EXISTS r31_warm_candidate_state_owner ON r26_candidate(policy_hash) WHERE length(state)>0";
    public static String[][] schemaObjectsR31(){return new String[][]{{"index","r31_warm_candidate_state_owner"}};}
    public static String contractHashR31(){return CheckpointArtifactsR26.sha(INDEX_DDL_R31.getBytes(java.nio.charset.StandardCharsets.UTF_8));}
    /** Called only by the existing startup migration owner, after protected archives. */
    public static void ensureSchemaR31(SQLiteDatabase db){if(!db.inTransaction())throw new IllegalStateException("R31_WARM_MIGRATION_OWNER_REQUIRED");DatabaseCoordinator.requireSchemaObjectsR31(db,"R31_WARM_INDEX",new String[][]{{"r26_candidate","policy_hash,state"}},new String[0][]);db.execSQL(INDEX_DDL_R31);requireSchemaR31(db);}
    static void requireIndexR31(SQLiteDatabase db){try(Cursor c=db.rawQuery("SELECT sql FROM sqlite_master WHERE type='index' AND name='r31_warm_candidate_state_owner'",null)){if(!c.moveToFirst()||!INDEX_DDL_R31.replace("IF NOT EXISTS ","").equals(c.getString(0)))throw new DatabaseCoordinator.SchemaPendingR31("R31_WARM_INDEX","EXACT_INDEX_REQUIRED");}}
    static final String COLUMNS="campaign_id,policy_hash,canonical_text,cursor,state,state_hash,checkpoint_hash,dev_n,dev_k,val_n,val_k,loss_sum,status,parent,operation,service_cpu_ms,last_served";
    public static void requireSchemaR31(SQLiteDatabase db){DatabaseCoordinator.requireSchemaObjectsR31(db,"R31_WARM_PUBLICATION",new String[][]{
        {"r26_candidate",COLUMNS},{"r26_foundry","campaign_id,snapshot_hash,source_through,snapshot,status,created_at,numerical_abi,source_first"},
        {"r26_warm_initialization","policy_hash,parent_policy,parent_checkpoint,parent_state_hash,parent_trained_through,parent_exposed_through,initializer_hash,state,child_campaign,seed_checkpoint,numerical_abi,created_at"},
        {"r26_checkpoint_artifact","checkpoint_hash,artifact_hash,policy_hash,source_hash,parent_hash,trained_through,created_at"},
        {"r26_work_total","task_id,completed"},{"r30_keep_r26_candidate","__r30_id,"+COLUMNS}},schemaObjectsR31());requireIndexR31(db);requireCandidateArchiveR31(db);}
    /** Reuses the existing R30 archive owner format; business only verifies it. */
    static void requireCandidateArchiveR31(SQLiteDatabase db){requireProtectedArchiveR31(db,"r26_candidate");}
    static void requireFoundryArchiveR31(SQLiteDatabase db){requireProtectedArchiveR31(db,"r26_foundry");}
    private static void requireProtectedArchiveR31(SQLiteDatabase db,String source){
        String target="r30_keep_"+source;List<String[]> columns=ResearchRetentionStoreR30.columns(db,source);List<List<String>> metadata=new ArrayList<>();for(String[] column:columns)metadata.add(Arrays.asList(column));String encoded=JsonR27.write(metadata),hash=StatsR27.sha256(encoded),prefix=target+"_"+hash.substring(0,12);
        try(Cursor c=db.rawQuery("SELECT 1 FROM r30_retention_archive_index WHERE source_table=? AND archive_table=? AND schema_hash=? AND columns_json=?",new String[]{source,target,hash,encoded})){if(!c.moveToFirst())throw new DatabaseCoordinator.SchemaPendingR31("R31_WARM_ARCHIVE","R30_OWNER_INDEX_NOT_READY");}
        StringJoiner names=new StringJoiner(","),values=new StringJoiner(","),clears=new StringJoiner(" OR "),payload=new StringJoiner(",");for(String[] column:columns){String name=column[0],q=ResearchRetentionStoreR30.q(name);names.add(q);values.add("OLD."+q);if(column[1].toUpperCase(Locale.ROOT).contains("BLOB")||name.equals("snapshot")||name.endsWith("_json")){payload.add(q);clears.add("(length(OLD."+q+")>0 AND (NEW."+q+" IS NULL OR length(NEW."+q+")=0))");}}
        String now="CAST((julianday('now')-2440587.5)*86400000 AS INTEGER)",enabled="(SELECT schema_hash FROM r30_retention_archive_index WHERE source_table="+ResearchRetentionStoreR30.lit(source)+")="+ResearchRetentionStoreR30.lit(hash);
        String body="INSERT INTO "+ResearchRetentionStoreR30.q(target)+"(__r30_at,__r30_reason,"+names+") VALUES("+now+","+ResearchRetentionStoreR30.lit("BEFORE_ORIGINAL_DELETE")+","+values+"); UPDATE r30_retention_archive_index SET retained_rows=retained_rows+1,updated_at="+now+" WHERE source_table="+ResearchRetentionStoreR30.lit(source)+";";
        String delete="CREATE TRIGGER "+ResearchRetentionStoreR30.q(prefix+"_delete")+" BEFORE DELETE ON "+ResearchRetentionStoreR30.q(source)+" WHEN "+enabled+" BEGIN "+body+" END";
        List<String[]> expected=new ArrayList<>();expected.add(new String[]{prefix+"_delete",delete});if(payload.length()>0){String clearBody=body.replace(ResearchRetentionStoreR30.lit("BEFORE_ORIGINAL_DELETE"),ResearchRetentionStoreR30.lit("BEFORE_ORIGINAL_PAYLOAD_CLEAR"));expected.add(new String[]{prefix+"_clear","CREATE TRIGGER "+ResearchRetentionStoreR30.q(prefix+"_clear")+" BEFORE UPDATE OF "+payload+" ON "+ResearchRetentionStoreR30.q(source)+" WHEN "+enabled+" AND ("+clears+") BEGIN "+clearBody+" END"});}for(String action:new String[]{"UPDATE","DELETE"})expected.add(new String[]{target+"_immutable_"+action.toLowerCase(Locale.ROOT),"CREATE TRIGGER "+ResearchRetentionStoreR30.q(target+"_immutable_"+action.toLowerCase(Locale.ROOT))+" BEFORE "+action+" ON "+ResearchRetentionStoreR30.q(target)+" BEGIN SELECT RAISE(ABORT,'R30_PROTECTED_ARCHIVE_IMMUTABLE'); END"});
        for(String[] item:expected)try(Cursor c=db.rawQuery("SELECT sql FROM sqlite_master WHERE type='trigger' AND name=?",new String[]{item[0]})){if(!c.moveToFirst()||!item[1].equals(c.getString(0).replace("IF NOT EXISTS ","").trim()))throw new DatabaseCoordinator.SchemaPendingR31("R31_WARM_ARCHIVE","EXACT_R30_GUARD:"+item[0]);}
    }
    private static Object[] row(Cursor c){Object[] out=new Object[c.getColumnCount()];for(int i=0;i<out.length;i++)out[i]=c.isNull(i)?null:c.getType(i)==4?c.getBlob(i):c.getType(i)==1?c.getLong(i):c.getType(i)==2?c.getDouble(i):c.getString(i);return out;}
    private static boolean same(Object[] a,Object[] b){if(a.length!=b.length)return false;for(int i=0;i<a.length;i++){if(a[i] instanceof byte[]&&b[i] instanceof byte[]){if(!Arrays.equals((byte[])a[i],(byte[])b[i]))return false;}else if(!Objects.equals(a[i],b[i]))return false;}return true;}
    private static long scalar(SQLiteDatabase db,String sql,String...args){try(Cursor c=db.rawQuery(sql,args)){if(!c.moveToFirst())throw new IllegalStateException("R31_WARM_SCALAR_MISSING");return c.getLong(0);}}
    public static final class Prepared implements AutoCloseable {
        final ModelPublicationR31.Plan plan;private CheckpointArtifactsR26.Prepared state,initializer;
        Prepared(ModelPublicationR31.Plan plan,CheckpointArtifactsR26.Prepared state,CheckpointArtifactsR26.Prepared initializer){this.plan=plan;this.state=state;this.initializer=initializer;}
        public long publish(DbStore store,RuntimeCoordinatorR26.Claim claim)throws IOException{ModelPublicationR31.stageAndPublish(store,claim,plan);return scalar(store.readable(),"SELECT completed FROM r26_work_total WHERE task_id='warm-candidates'");}
        public void close(){plan.close();if(initializer!=null){initializer.close();initializer=null;}if(state!=null){state.close();state=null;}}
    }
    static Prepared prepareR31(DbStore store,RuntimeCoordinatorR26.Claim claim,WarmCandidateR26.Parent parent,WarmModelSeedR26.Seed seed,FoundryCampaignR26.Snapshot source,byte[] snapshot,String sourceHash,long sourceGeneration)throws IOException{
        requireSchemaR31(store.readable());String child=seed.genome.key(),campaign=HashingSU1.fields("R26_WARM_CAMPAIGN_V1",parent.policy,parent.checkpoint,sourceHash,child);
        String stateHash=LiveStateR26.hash(seed.state()),checkpoint=WarmCandidateR26.seedCheckpoint(child,campaign,parent.checkpoint,seed.initializerHash,stateHash,parent.through);
        String publicationId="WARM:"+campaign+":"+sourceGeneration;ModelPublicationR31.Plan p=ModelPublicationR31.existing(store.readable(),publicationId);CheckpointArtifactsR26.Prepared stateFile=null,initializerFile=null;
        try{
            stateFile=CheckpointArtifactsR26.prepareLease(store,seed.state());initializerFile=CheckpointArtifactsR26.prepareLease(store,seed.initializer());
            if(p!=null){ModelPublicationR31.Item saved=p.item("WARM_CANDIDATE",child),init=p.item("WARM_INITIALIZER",child);if(saved==null||init==null||!Arrays.equals((byte[])saved.values[3],stateFile.reference())||!Arrays.equals((byte[])init.values[7],initializerFile.reference()))throw new ModelPublicationR31.PublicationDependencyR31("WARM_DURABLE_PLAN_NUMERICAL_MISMATCH");return new Prepared(p,stateFile,initializerFile);}
            p=new ModelPublicationR31.Plan(publicationId,"WARM_FORK",sourceHash,sourceGeneration,-1,System.currentTimeMillis());
            String raw=p.rawPayload(snapshot),abi=ResearchSemanticsGeneratedR967.TRAINING_RULE_SHA256;
            ModelStoreSU2.prepareIdentityR31(p,parent.text,parent.policy);ModelStoreSU2.prepareIdentityR31(p,seed.genome.text(),child);
            p.put("FOUNDRY_ROOT",campaign,campaign,sourceHash,source.through,raw,"RUNNING",p.now,abi,source.ordinal[0],snapshot.length);
            String operation=StructuralProposalR26.control(seed.genome)?parent.operation:"WARM_KERNEL_CONTINUATION";
            p.put("WARM_CANDIDATE",child,campaign,child,seed.genome.text(),stateFile.reference(),stateHash,checkpoint,parent.policy,operation);
            p.put("WARM_INITIALIZER",child,child,parent.policy,parent.checkpoint,parent.hash,parent.through,parent.exposure,seed.initializerHash,initializerFile.reference(),campaign,checkpoint,abi,p.now);
            p.put("WARM_ARTIFACT",checkpoint,checkpoint,stateFile.hash(),child,campaign,parent.checkpoint,parent.through,p.now,CheckpointArtifactsR26.referenceHash(parent.reference));
            p.put("WARM_ATTEMPT",campaign+":0",campaign+":0",campaign,child,parent.policy,operation,"VERIFIED_PAST_KERNEL_INITIALIZATION",StructuralProposalR26.structure(seed.genome));
            p.put("WARM_META","1",parent.campaign,parent.policy,parent.checkpoint,parent.hash,parent.cursor,WarmCandidateR26.WAITING,parent.through,parent.exposure,parent.birthHash,abi,campaign,child,checkpoint,source.ordinal[0],source.through,parent.reference);
            Object[] original;try(Cursor c=store.readable().rawQuery("SELECT "+COLUMNS+" FROM r26_candidate WHERE campaign_id=? AND policy_hash=?",new String[]{parent.campaign,parent.policy})){if(!c.moveToFirst())throw new ModelPublicationR31.PublicationDependencyR31("WARM_PARENT_CHANGED_DURING_PREPARE");original=row(c);}
            p.put("WARM_PARENT","0",Arrays.copyOf(original,16));p.put("WARM_PARENT","1",original[16]);
            Long prior=null;try(Cursor c=store.readable().rawQuery("SELECT completed FROM r26_work_total WHERE task_id='warm-candidates'",null)){if(c.moveToFirst())prior=c.getLong(0);}long base=Math.max(claim.cursor,prior==null?claim.cursor:prior);
            p.put("WARM_PROGRESS","1",prior,base,Math.addExact(base,1));
            return new Prepared(p,stateFile,initializerFile);
        }catch(IOException|RuntimeException|Error failure){if(p!=null)p.close();if(initializerFile!=null)initializerFile.close();if(stateFile!=null)stateFile.close();throw failure;}
    }
    private static Object[] expectedParent(ModelPublicationR31.Plan p){Object[] out=new Object[17];System.arraycopy(p.item("WARM_PARENT","0").values,0,out,0,16);out[16]=p.item("WARM_PARENT","1").values[0];return out;}
    static void publishR31(SQLiteDatabase db,ModelPublicationR31.Plan p){
        if(p.count("WARM_META")!=1||p.count("WARM_CANDIDATE")!=1||p.count("WARM_INITIALIZER")!=1||p.count("WARM_ARTIFACT")!=1||p.count("WARM_ATTEMPT")!=1||p.count("FOUNDRY_ROOT")!=1||p.count("WARM_PARENT")!=2||p.count("WARM_PROGRESS")!=1)throw new IllegalStateException("R31_WARM_MANIFEST_SHAPE");
        Object[] m=p.item("WARM_META","1").values;Object[] expected=expectedParent(p);
        try(Cursor c=db.rawQuery("SELECT "+COLUMNS+" FROM r26_candidate WHERE campaign_id=? AND policy_hash=?",new String[]{(String)m[0],(String)m[1]})){if(!c.moveToFirst()||!same(row(c),expected))throw new ModelPublicationR31.PublicationDependencyR31("WARM_PARENT_ROW_CHANGED");}
        try(Cursor c=db.rawQuery("SELECT 1 FROM r26_candidate c JOIN r26_foundry f ON f.campaign_id=c.campaign_id WHERE c.campaign_id=? AND c.policy_hash=? AND c.checkpoint_hash=? AND c.state_hash=? AND c.cursor=? AND c.status=? AND f.status='EXPIRED_RETENTION' AND f.numerical_abi=? AND f.snapshot_hash=? AND f.source_through=? AND NOT EXISTS(SELECT 1 FROM r26_live_model l WHERE l.policy_hash=c.policy_hash)",new String[]{(String)m[0],(String)m[1],(String)m[2],(String)m[3],String.valueOf(m[4]),(String)m[5],(String)m[9],(String)m[8],String.valueOf(m[7])})){if(!c.moveToFirst())throw new ModelPublicationR31.PublicationDependencyR31("WARM_PARENT_SOURCE_OR_PUBLICATION_CHANGED");}
        try(Cursor latest=db.rawQuery("SELECT issue FROM draws WHERE dirty=0 ORDER BY issue DESC LIMIT 1",null)){if(!latest.moveToFirst()||TargetIssueResolver.sequenceOrdinalSU1(latest.getString(0))-((Number)m[13]).longValue()>=5000)throw new ModelPublicationR31.PublicationDependencyR31("WARM_SOURCE_EXPIRED_BEFORE_BIRTH");}
        Object[] artifact=null,root=null;for(ModelPublicationR31.Item item:p.items.values()){if(item.kind.equals("WARM_ARTIFACT"))artifact=item.values;if(item.kind.equals("FOUNDRY_ROOT"))root=item.values;}
        try(Cursor c=db.rawQuery("SELECT 1 FROM r26_checkpoint_artifact WHERE checkpoint_hash=? AND artifact_hash=? AND policy_hash=? AND source_hash=? AND trained_through=?",new String[]{(String)m[2],(String)artifact[7],(String)m[1],(String)m[0],String.valueOf(m[6])})){if(!c.moveToFirst())throw new ModelPublicationR31.PublicationDependencyR31("WARM_PARENT_MANIFEST_CHANGED");}
        ModelStoreSU2.projectPreparedR31(db,p);
        db.execSQL("INSERT INTO r26_foundry(campaign_id,snapshot_hash,source_through,snapshot,status,created_at,numerical_abi,source_first) SELECT c0,c1,c2,"+ModelPublicationR31.rawExpression((String)root[3])+",c4,c5,c6,c7 FROM ("+ModelPublicationR31.select(p,"FOUNDRY_ROOT","c0,c1,c2,c3,c4,c5,c6,c7")+")");
        db.execSQL("INSERT INTO r26_candidate(campaign_id,policy_hash,canonical_text,cursor,state,state_hash,checkpoint_hash,status,parent,operation) SELECT c0,c1,c2,0,c3,c4,c5,'READY',c6,c7 FROM ("+ModelPublicationR31.select(p,"WARM_CANDIDATE","c0,c1,c2,c3,c4,c5,c6,c7")+")");
        db.execSQL("INSERT INTO r26_warm_initialization(policy_hash,parent_policy,parent_checkpoint,parent_state_hash,parent_trained_through,parent_exposed_through,initializer_hash,state,child_campaign,seed_checkpoint,numerical_abi,created_at) "+ModelPublicationR31.select(p,"WARM_INITIALIZER","c0,c1,c2,c3,c4,c5,c6,c7,c8,c9,c10,c11"));
        db.execSQL("INSERT INTO r26_checkpoint_artifact(checkpoint_hash,artifact_hash,policy_hash,source_hash,parent_hash,trained_through,created_at) "+ModelPublicationR31.select(p,"WARM_ARTIFACT","c0,c1,c2,c3,c4,c5,c6"));
        db.execSQL("INSERT INTO r26_evolution(attempt_id,campaign_id,policy_hash,parent,operation,category,structure_hash) "+ModelPublicationR31.select(p,"WARM_ATTEMPT","c0,c1,c2,c3,c4,c5,c6"));
        long archiveBefore=scalar(db,"SELECT coalesce(max(__r30_id),0) FROM r30_keep_r26_candidate");
        db.execSQL("DELETE FROM r26_candidate WHERE campaign_id=? AND policy_hash=? AND checkpoint_hash=? AND status=?",new Object[]{m[0],m[1],m[2],m[5]});if(scalar(db,"SELECT changes()")!=1)throw new IllegalStateException("R31_WARM_TRANSFER_CAS");
        // Verify the original complete row before allowing its canonical removal to commit.
        try(Cursor c=db.rawQuery("SELECT "+COLUMNS+" FROM r30_keep_r26_candidate WHERE __r30_id>? ORDER BY __r30_id LIMIT 2",new String[]{Long.toString(archiveBefore)})){if(!c.moveToFirst()||!same(row(c),expected)||c.moveToNext())throw new ModelPublicationR31.PublicationDependencyR31("WARM_PARENT_ARCHIVE_NOT_PROVEN");}
        Object[] progress=p.item("WARM_PROGRESS","1").values;long base=((Number)progress[1]).longValue(),next=((Number)progress[2]).longValue();db.execSQL("INSERT OR IGNORE INTO r26_work_total(task_id,completed) VALUES('warm-candidates',?)",new Object[]{base});db.execSQL("UPDATE r26_work_total SET completed=? WHERE task_id='warm-candidates' AND completed=?",new Object[]{next,progress[0]==null?base:progress[0]});if(scalar(db,"SELECT changes()")!=1)throw new ModelPublicationR31.PublicationDependencyR31("WARM_PROGRESS_CAS");
    }
}
