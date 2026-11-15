package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.io.*;
import java.lang.reflect.*;
import java.nio.file.*;
import java.sql.*;
import java.util.*;

/** Real production learner/codecs/recovery and native SQLite; synthetic engineering
 * inputs do not assert natural quality. Only Android platform APIs are adapted. */
public final class AtomNetRecoveryR32Host {
    static int assertions;
    static void need(boolean ok,String message){assertions++;if(!ok)throw new AssertionError(message);}
    static RuntimeR27 runtime()throws Exception{
        Class<?> type=Class.forName("sun.misc.Unsafe");Field f=type.getDeclaredField("theUnsafe");f.setAccessible(true);
        return (RuntimeR27)type.getMethod("allocateInstance",Class.class).invoke(f.get(null),RuntimeR27.class);
    }
    static Object allocate(Class<?> type)throws Exception{Class<?> u=Class.forName("sun.misc.Unsafe");Field f=u.getDeclaredField("theUnsafe");f.setAccessible(true);return u.getMethod("allocateInstance",Class.class).invoke(f.get(null),type);}
    static void field(Object owner,String name,Object value)throws Exception{Field f=owner.getClass().getDeclaredField(name);f.setAccessible(true);f.set(owner,value);}
    static com.ethk3.novaonepro.DbStore nativeStore(Fixture f)throws Exception{
        android.content.Context context=new android.app.Application(new File(f.db.getPath()));
        android.database.sqlite.SQLiteOpenHelper.bindExistingForHostR31(new File(f.db.getPath()),f.db);
        com.ethk3.novaonepro.DatabaseCoordinator coordinator=(com.ethk3.novaonepro.DatabaseCoordinator)allocate(com.ethk3.novaonepro.DatabaseCoordinator.class);
        field(coordinator,"helper",new com.ethk3.novaonepro.DbHelper(context));field(coordinator,"app",context);
        com.ethk3.novaonepro.DbStore store=(com.ethk3.novaonepro.DbStore)allocate(com.ethk3.novaonepro.DbStore.class);field(store,"researchAppCP12",context);field(store,"coordinator",coordinator);return store;
    }
    static AtomNetSessionR30 copy(AtomNetSessionR30 state){byte[] b=state.encode();return AtomNetSessionR30.decode(b,StatsR27.sha256(b));}
    static String issue(int n){return "20261115"+String.format(Locale.ROOT,"%04d",n);}
    static long ordinal(int n){return com.ethk3.novaonepro.TargetIssueResolver.sequenceOrdinalSU1(issue(n));}
    static AtomNetSessionR30 complete(){
        AtomNetSessionR30 s=new AtomNetSessionR30(StatsR27.sha256("R32_ENGINEERING_DATASET"),issue(10),ordinal(10),12,20,20,10);
        s.phase="ONLINE";s.completeIssue=issue(10);s.onlineObservedOrdinal=ordinal(10);s.materializedSamples=40;s.cachePeriod=com.ethk3.novaonepro.AtomReplayR27.MIN_ROWS+4;return s;
    }
    static AtomNetSessionR30 next(AtomNetSessionR30 state,int position){
        AtomNetSessionR30 n=copy(state);double[] x=AtomNetR27.positionedInput(new double[]{.25,.75},position);
        n.online(new AtomNetTrainerR27.Sample(x,position%3==0?1:0,position-1,AtomNetSessionR30.eventKey(issue(11),position)),issue(11),ordinal(11),position);return n;
    }
    static byte[] ref(CheckpointArtifactsR26 files,byte[] bytes)throws Exception{
        try(CheckpointArtifactsR26.Prepared p=files.prepareState(o->o.write(bytes))){return p.reference();}
    }
    static String save(SQLiteDatabase db,CheckpointArtifactsR26 files,AtomNetSessionR30 s,String version,long at,boolean model)throws Exception{
        byte[] encoded=s.encode(),stateRef=ref(files,encoded);String hash=StatsR27.sha256(encoded),id=StatsR27.sha256(version+"|"+hash+"|"+at);
        byte[] weights=s.trainer.net.encode(),weightsRef=model?files.prepare(weights):null;
        ModelSurfaceProviderR30.FrozenModelRef f=model?new ModelSurfaceProviderR30.FrozenModelRef(weights,s.trainedThroughOrdinal(),at):null;
        db.execSQL("INSERT INTO r31_atomnet_publication VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,'PUBLISHED',?,?)",new Object[]{id,version,AtomNetSessionR30.OWNER,s.datasetHash,"",hash,stateRef,f==null?null:f.modelHash,f==null?null:f.weightsHash,weightsRef,f==null?null:at,s.trainedThroughOrdinal(),s.materializedSamples,0,0,0,StatsR27.sha256(version),null,null,at,at});return id;
    }
    static void checkpoint(SQLiteDatabase db,CheckpointArtifactsR26 files,AtomNetSessionR30 s)throws Exception{
        byte[] encoded=s.encode();db.execSQL("INSERT OR REPLACE INTO r29_model_checkpoint VALUES(?,?,?,?,?,?,?)",new Object[]{AtomNetSessionR30.OWNER,AtomNetSessionR30.ABI,s.datasetHash,s.completeIssue.isEmpty()?s.observedThrough:s.trainedThrough(),StatsR27.sha256(encoded),ref(files,encoded),1000L});
    }
    static final class Fixture implements AutoCloseable {
        final Path root;final SQLiteDatabase db;final CheckpointArtifactsR26 files;
        Fixture(String name)throws Exception{
            root=Files.createTempDirectory("r32-atom-"+name);Path database=root.resolve("databases/main.db");Files.createDirectories(database.getParent());
            db=new SQLiteDatabase(DriverManager.getConnection("jdbc:sqlite:"+database));
            for(String ddl:AtomNetPublicationR31.DDL)db.execSQL(ddl);
            db.execSQL("CREATE TABLE r29_model_checkpoint(owner TEXT PRIMARY KEY,feature_abi TEXT,dataset_hash TEXT,trained_through TEXT,state_hash TEXT,artifact BLOB,updated_at INTEGER)");
            files=new CheckpointArtifactsR26(root.resolve("files/r26-checkpoints").toFile());CheckpointArtifactsR26.registerDatabaseRootR31(db,root.resolve("files/r26-checkpoints").toFile());
        }
        public void close()throws Exception{CheckpointArtifactsR26.unregisterDatabaseRootR31(db);db.close();}
    }
    static void partialRestart()throws Exception{
        try(Fixture f=new Fixture("partial")){
            AtomNetSessionR30 s=complete();save(f.db,f.files,s,"R31_ATOMNET_PUBLICATION_1",1000,true);String expected=StatsR27.sha256(s.trainer.net.encode());
            for(int p=1;p<=9;p++){
                s=next(s,p);save(f.db,f.files,s,"R31_ATOMNET_PUBLICATION_1",1000+p,true);checkpoint(f.db,f.files,s);
                AtomNetSessionR30 restored=AtomNetStoreR30.restore(f.db);ModelSurfaceProviderR30.FrozenModelRef model=AtomNetStoreR30.publicationModelR31(f.db,restored);
                need(model!=null&&expected.equals(model.weightsHash),"cold restart P"+p+" restores earlier complete original");
                need(model.trainedThroughOrdinal==ordinal(10),"complete watermark never advances to partial issue");
                RuntimeR27 fresh=runtime();fresh.restoreAtomStateR32(restored,model);need(fresh.atomPublicationR30.model==model,"cold runtime retains verified complete model");
                need(restored.nextPosition==p+1&&restored.trainer.onlineSteps==p,"optimizer cursor resumes without repeated sample");
            }
            s=next(s,10);save(f.db,f.files,s,AtomNetPublicationR31.VERSION,2000,true);checkpoint(f.db,f.files,s);
            ModelSurfaceProviderR30.FrozenModelRef model=AtomNetStoreR30.publicationModelR31(f.db,AtomNetStoreR30.restore(f.db));
            need(model.trainedThroughOrdinal==ordinal(11)&&!expected.equals(model.weightsHash),"position10 advances complete model with actual changed weights");
            Map<String,Object> health=AtomNetPublicationR31.storageHealthR32(f.db);need(((Number)health.get("publicationRows")).longValue()==11,"capacity census reads actual committed rows");need(Boolean.TRUE.equals(health.get("coldArchiveReclamationImplemented")),"archive resolver implementation is declared separately from execution");need(((Number)health.get("coldSampleArchiveSegments")).longValue()==0,"no archived execution inferred from implementation availability");
        }
    }
    static void validationRestart()throws Exception{
        try(Fixture f=new Fixture("validation")){
            AtomNetSessionR30 s=complete();save(f.db,f.files,s,AtomNetPublicationR31.VERSION,1000,true);String expected=StatsR27.sha256(s.trainer.net.encode());
            for(int p=1;p<=10;p++)s=next(s,p);
            s.phase="EVAL_ONLINE_DEVELOPMENT_VALIDATION";save(f.db,f.files,s,AtomNetPublicationR31.VERSION,2000,false);checkpoint(f.db,f.files,s);
            AtomNetSessionR30 restored=AtomNetStoreR30.restore(f.db);ModelSurfaceProviderR30.FrozenModelRef model=AtomNetStoreR30.publicationModelR31(f.db,restored);need(expected.equals(model.weightsHash),"EVAL restart keeps last complete published model");RuntimeR27 fresh=runtime();fresh.restoreAtomStateR32(restored,model);need(fresh.atomPublicationR30.model!=null,"validation cannot erase restored inference publication");
            need("ONLINE".equals(complete().report().get("displayPhase")),"materialization display ignores unused formal holdout");
        }
    }
    static void rejectedCorruptions()throws Exception{
        try(Fixture f=new Fixture("wrongweights")){
            AtomNetSessionR30 s=complete();String id=save(f.db,f.files,s,AtomNetPublicationR31.VERSION,1000,true);checkpoint(f.db,f.files,s);f.db.execSQL("UPDATE r31_atomnet_publication SET weights_hash=? WHERE publication_id=?",new Object[]{StatsR27.sha256("wrong"),id});
            boolean rejected=false;try{AtomNetStoreR30.publicationModelR31(f.db,s);}catch(IllegalStateException e){rejected=e.getMessage().startsWith("R32_ATOM_RECOVERY_WEIGHTS_MISMATCH");}need(rejected,"weights metadata corruption rejected");
        }
        try(Fixture f=new Fixture("v2partial")){
            AtomNetSessionR30 s=complete();save(f.db,f.files,s,AtomNetPublicationR31.VERSION,1000,true);s=next(s,1);String id=save(f.db,f.files,s,AtomNetPublicationR31.VERSION,2000,true);s.completeIssue=issue(11); // force row into eligible bound only; the stored row remains partial
            boolean rejected=false;try{AtomNetStoreR30.publicationModelR31(f.db,s);}catch(IllegalStateException e){rejected=e.getMessage().startsWith("R32_ATOM_COMPLETE_PUBLICATION_CONTAINS_PARTIAL_STATE");}need(rejected,"V2 contract claiming a partial publication is corruption");
        }
        try(Fixture f=new Fixture("missing")){
            AtomNetSessionR30 s=complete();String id=save(f.db,f.files,s,AtomNetPublicationR31.VERSION,1000,true);checkpoint(f.db,f.files,s);String hash;
            try(Cursor c=f.db.rawQuery("SELECT state_ref FROM r31_atomnet_publication WHERE publication_id=?",new String[]{id})){need(c.moveToFirst(),"source manifest exists");hash=CheckpointArtifactsR26.referenceHash(c.getBlob(0));}
            Files.delete(f.root.resolve("files/r26-checkpoints/"+hash+".bin"));boolean rejected=false;try{AtomNetStoreR30.publicationModelR31(f.db,s);}catch(IllegalStateException e){rejected=true;}need(rejected,"missing original cannot become invented model");
        }
    }
    static void transactionInterruption()throws Exception{
        try(Fixture f=new Fixture("rollback")){
            AtomNetSessionR30 a=complete();save(f.db,f.files,a,AtomNetPublicationR31.VERSION,1000,true);checkpoint(f.db,f.files,a);String previous=StatsR27.sha256(a.trainer.net.encode());
            AtomNetSessionR30 b=a;for(int p=1;p<=10;p++)b=next(b,p);
            f.db.beginTransaction();try{save(f.db,f.files,b,AtomNetPublicationR31.VERSION,2000,true);checkpoint(f.db,f.files,b);}finally{f.db.endTransaction();}
            AtomNetSessionR30 recovered=AtomNetStoreR30.restore(f.db);need(recovered.completeIssue.equals(issue(10)),"uncommitted learner CAS does not survive rollback");need(AtomNetStoreR30.publicationModelR31(f.db,recovered).weightsHash.equals(previous),"prepared files alone do not publish a model after interruption");
        }
    }
    static long scalar(SQLiteDatabase db,String sql){try(Cursor c=db.rawQuery(sql,null)){need(c.moveToFirst(),"scalar available");return c.getLong(0);}}
    static void activeCapacity()throws Exception{
        try(Fixture f=new Fixture("capacity")){
            AtomNetSessionR30 state=complete();String source=save(f.db,f.files,state,AtomNetPublicationR31.VERSION,1000,true);checkpoint(f.db,f.files,state);
            f.db.beginTransaction();try{
                for(int i=0;i<4100;i++)f.db.execSQL("INSERT INTO r31_atomnet_publication SELECT ?,operator_version,owner,dataset_hash,previous_state_hash,state_hash,state_ref,NULL,NULL,NULL,NULL,through_ordinal,materialized_samples,first_sample,0,0,sample_digest,dataset_ref,dataset_payload_hash,'PUBLISHED',created_at,published_at FROM r31_atomnet_publication WHERE publication_id=?",new Object[]{"history-"+i,source});
                f.db.setTransactionSuccessful();
            }finally{f.db.endTransaction();}
            long all=scalar(f.db,"SELECT count(*) FROM r31_atomnet_publication");need(all==4101,"more than4096 original historical manifests retained");
            AtomNetPublicationR31.requireReservationCapacityR32(f.db,state.datasetHash,"current-parent",120,4096);
            need(AtomNetPublicationR31.reservationR32(f.db,state.datasetHash,"current-parent").rows==0,"no-sample published history is not an active reservation");
            f.db.execSQL("INSERT INTO r31_atomnet_publication SELECT 'active-material',operator_version,owner,dataset_hash,previous_state_hash,state_hash,state_ref,NULL,NULL,NULL,NULL,through_ordinal,materialized_samples,first_sample,99990,1000,sample_digest,dataset_ref,dataset_payload_hash,'PUBLISHED',created_at,published_at FROM r31_atomnet_publication WHERE publication_id=?",new Object[]{source});
            boolean rejected=false;try{AtomNetPublicationR31.requireReservationCapacityR32(f.db,state.datasetHash,"current-parent",11,1);}catch(AtomNetPublicationR31.Dependency expected){rejected=true;}need(rejected,"active dataset sample reservation over100000 rejected");
            AtomNetPublicationR31.requireReservationCapacityR32(f.db,StatsR27.sha256("newdataset"),"",120,4096);need(true,"old dataset retained sample reservations do not consume new dataset slots");
            f.db.execSQL("INSERT INTO r31_atomnet_publication SELECT 'old-prepared',operator_version,owner,dataset_hash,'old-parent',state_hash,state_ref,NULL,NULL,NULL,NULL,through_ordinal,materialized_samples,first_sample,100000,?,sample_digest,dataset_ref,dataset_payload_hash,'PREPARED',created_at,NULL FROM r31_atomnet_publication WHERE publication_id=?",new Object[]{AtomNetPublicationR31.MAX_RESERVED_SAMPLE_BYTES,source});
            AtomNetPublicationR31.requireReservationCapacityR32(f.db,state.datasetHash,"current-parent",10,1);need(true,"obsolete prepared parent remains original but no longer consumes current-parent slots");
            rejected=false;try{AtomNetPublicationR31.requireReservationCapacityR32(f.db,state.datasetHash,"old-parent",1,1);}catch(AtomNetPublicationR31.Dependency expected){rejected=true;}need(rejected,"current-parent prepared reservations are counted");
            Map<String,Object> health=AtomNetPublicationR31.storageHealthR32(f.db);need(Boolean.FALSE.equals(health.get("historicalRowsAreAdmissionQuota")),"report labels history counter separately from active quota");need(scalar(f.db,"SELECT count(*) FROM r31_atomnet_publication")==all+2,"capacity accounting never deletes historical evidence");
        }
    }
    static void actualPreparedPublication()throws Exception{
        try(Fixture f=new Fixture("prepare")){
            AtomNetSessionR30 a=complete();String id=save(f.db,f.files,a,AtomNetPublicationR31.VERSION,1000,true);checkpoint(f.db,f.files,a);String parent=StatsR27.sha256(a.encode());com.ethk3.novaonepro.DbStore store=nativeStore(f);
            AtomNetSessionR30 b=next(a,1);
            try(AtomNetPublicationR31.Prepared p=AtomNetPublicationR31.prepare(store,f.db,new ResearchWriteQueueR27.Batch(),b,2000,parent)){
                need(p.model==null,"actual production prepare never emits P1 partial inference weights");need(p.sampleCount()==0,"optimizer checkpoint without materialization has no sample reservation");
            }
            for(int position=2;position<=10;position++)b=next(b,position);
            try(AtomNetPublicationR31.Prepared p=AtomNetPublicationR31.prepare(store,f.db,new ResearchWriteQueueR27.Batch(),b,3000,parent)){
                need(p.model!=null&&p.model.trainedThroughOrdinal==ordinal(11),"actual production prepare emits the complete P10 immutable model");need(p.model.weightsHash.equals(StatsR27.sha256(b.trainer.net.encode())),"prepared complete weights are exact learner weights");
            }
        }
    }
    static void oldSchemaMigration()throws Exception{
        try(Fixture f=new Fixture("schema")){
            AtomNetSessionR30 a=complete();String id=save(f.db,f.files,a,"R31_ATOMNET_PUBLICATION_1",1000,true);
            f.db.execSQL("DROP INDEX r32_atomnet_active_material");f.db.execSQL("DROP INDEX r32_atomnet_pending_parent");f.db.execSQL("DROP INDEX r32_atomnet_complete_recovery");
            f.db.beginTransaction();try{com.ethk3.novaonepro.SchemaDomainCoordinatorR926.ensureInfrastructure(f.db);com.ethk3.novaonepro.SchemaDomainCoordinatorR926.migrateR31AtomNetPublication(f.db);f.db.setTransactionSuccessful();}finally{f.db.endTransaction();}
            AtomNetPublicationR31.requireReady(f.db);need(scalar(f.db,"SELECT count(*) FROM sqlite_master WHERE name IN ('r32_atomnet_active_material','r32_atomnet_pending_parent','r32_atomnet_complete_recovery')")==3,"existing migration owner installs all reservation and recovery indexes");need(scalar(f.db,"SELECT count(*) FROM r31_atomnet_publication")==1,"additive index migration preserves legacy publication");
        }
    }
    static void identityAndLookupBoundaries()throws Exception{
        try(Fixture f=new Fixture("identity")){
            AtomNetSessionR30 a=complete();String id=save(f.db,f.files,a,AtomNetPublicationR31.VERSION,1000,true);String expected=StatsR27.sha256(a.trainer.net.encode());
            AtomNetSessionR30 other=new AtomNetSessionR30(StatsR27.sha256("DIFFERENT_DATASET"),issue(10),ordinal(10),12,20,20,10);other.phase="ONLINE";other.completeIssue=issue(10);other.onlineObservedOrdinal=ordinal(10);save(f.db,f.files,other,AtomNetPublicationR31.VERSION,5000,true);
            need(AtomNetStoreR30.publicationModelR31(f.db,a).weightsHash.equals(expected),"different dataset never wins a newer publication timestamp");
            AtomNetSessionR30 future=a;for(int p=1;p<=10;p++)future=next(future,p);save(f.db,f.files,future,AtomNetPublicationR31.VERSION,6000,true);
            need(AtomNetStoreR30.publicationModelR31(f.db,a).trainedThroughOrdinal==ordinal(10),"model later than restored complete watermark cannot leak into recovery");
            boolean indexed=false,sort=false;try(Cursor c=f.db.rawQuery("EXPLAIN QUERY PLAN SELECT publication_id FROM r31_atomnet_publication WHERE owner=? AND dataset_hash=? AND status='PUBLISHED' AND published_at IS NOT NULL AND model_hash IS NOT NULL AND through_ordinal<=? ORDER BY through_ordinal DESC,published_at DESC,publication_id DESC LIMIT 65",new String[]{AtomNetSessionR30.OWNER,a.datasetHash,Long.toString(ordinal(10))})){while(c.moveToNext()){String detail=c.getString(3);indexed|=detail.contains("r32_atomnet_complete_recovery");sort|=detail.contains("TEMP B-TREE");}}need(indexed&&!sort,"recovery uses scoped ordered index without full history sort");
            f.db.execSQL("UPDATE r31_atomnet_publication SET published_at=999 WHERE publication_id=?",new Object[]{id});boolean rejected=false;try{AtomNetStoreR30.publicationModelR31(f.db,a);}catch(IllegalStateException e){rejected=e.getMessage().startsWith("R32_ATOM_RECOVERY_PUBLICATION_CLOCK_ORDER");}need(rejected,"publication cannot claim availability after its committed publication time");
        }
        try(Fixture f=new Fixture("wrong-index")){
            f.db.execSQL("DROP INDEX r32_atomnet_complete_recovery");f.db.execSQL("CREATE INDEX r32_atomnet_complete_recovery ON r31_atomnet_publication(state_hash)");boolean rejected=false;
            f.db.beginTransaction();try{com.ethk3.novaonepro.SchemaDomainCoordinatorR926.ensureInfrastructure(f.db);com.ethk3.novaonepro.SchemaDomainCoordinatorR926.migrateR31AtomNetPublication(f.db);f.db.setTransactionSuccessful();}catch(IllegalStateException e){rejected=true;}finally{f.db.endTransaction();}need(rejected,"migration does not accept wrong definition under expected index name");
        }
    }
    public static void main(String[] args)throws Exception{
        Class.forName("org.sqlite.JDBC");partialRestart();validationRestart();rejectedCorruptions();transactionInterruption();activeCapacity();actualPreparedPublication();oldSchemaMigration();identityAndLookupBoundaries();
        Map<String,Object> result=new LinkedHashMap<>();result.put("status","PASS");result.put("assertions",assertions);result.put("scope","PRODUCTION_LEARNER_AND_CODECS_NATIVE_SQLITE_HOST_NOT_ANDROID_OR_NATURAL_QUALITY");result.put("scenarios",Arrays.asList("P1_TO_P9_COLD_RESTART","POSITION10_SWITCH","VALIDATION_RESTART","V1_PARTIAL_SKIP","V2_PARTIAL_REJECT","WRONG_WEIGHTS_REJECT","MISSING_ORIGINAL_REJECT","TRANSACTION_ROLLBACK","CAPACITY_DIAGNOSIS","UNUSED_HOLDOUT_MATERIALIZATION_REMOVED","MORE_THAN4096_HISTORICAL_MANIFESTS_WITH_NEW_ACTIVE_ADMISSION","ACTIVE_DATASET_CAP_REJECTS","OBSOLETE_PREPARED_PARENT_PRESERVED","ACTUAL_PREPARE_COMPLETE_ONLY","EXISTING_MIGRATION_OWNER_ADDS_INDEXES","DIFFERENT_DATASET_EXCLUDED","FUTURE_WATERMARK_EXCLUDED","ORDERED_SCOPE_INDEX_USED","PUBLICATION_CLOCK_REJECTS","WRONG_INDEX_DEFINITION_REJECTS"));System.out.println(JsonR27.write(result));
    }
}
