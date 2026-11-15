package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.io.*;
import java.lang.reflect.*;
import java.nio.file.*;
import java.sql.DriverManager;
import java.util.*;

/** Native SQLite/real file fixture. Only Android platform APIs use host adapters. */
final class ArchiveLivenessFixtureR325 implements AutoCloseable {
    static final String BAD=String.format(Locale.ROOT,"%064x",1),GOOD=String.format(Locale.ROOT,"%064x",2);
    static final String DATA=StatsR27.sha256("archive-liveness-dataset"),OLD_STATE=StatsR27.sha256("old-archive-state");
    final Path root,database;SQLiteDatabase db;final CheckpointArtifactsR26 files;
    ArchiveLivenessFixtureR325(Path parent)throws Exception {
        root=Files.createTempDirectory(parent,"archive-liveness-");database=root.resolve("databases/main.db");Files.createDirectories(database.getParent());
        db=new SQLiteDatabase(DriverManager.getConnection("jdbc:sqlite:"+database));
        tx(()->{for(String sql:AtomNetPublicationR31.DDL)db.execSQL(sql);for(String sql:AtomNetStoreR30.DDL)db.execSQL(sql);db.execSQL("CREATE TABLE r29_model_checkpoint(owner TEXT PRIMARY KEY,feature_abi TEXT,dataset_hash TEXT,trained_through TEXT,state_hash TEXT,artifact BLOB,updated_at INTEGER)");});
        files=new CheckpointArtifactsR26(root.resolve("files/r26-checkpoints").toFile());CheckpointArtifactsR26.registerDatabaseRootR31(db,files.rootDirectoryR324());
    }
    interface Work {void run()throws Exception;}
    void tx(Work body)throws Exception {db.beginTransaction();try{body.run();db.setTransactionSuccessful();}finally{db.endTransaction();}}
    void batch(String op,ResearchWriteQueueR27.Batch batch)throws Exception {tx(()->ResearchWriteQueueR27.execute(db,batch,op));}
    long scalar(String sql){try(Cursor c=db.rawQuery(sql,null)){if(!c.moveToFirst())throw new AssertionError("missing scalar");return c.getLong(0);}}
    String text(String sql){try(Cursor c=db.rawQuery(sql,null)){if(!c.moveToFirst())throw new AssertionError("missing text");return c.getString(0);}}
    byte[] features(int n)throws Exception{ByteArrayOutputStream b=new ByteArrayOutputStream();try(DataOutputStream d=new DataOutputStream(b)){for(int i=0;i<12;i++)d.writeDouble((n+1.0)/(i+2));}return b.toByteArray();}
    Object[] row(String id,int i)throws Exception {byte[] features=features(i);String issue="202611150001",hash=AtomNetPublicationR31.sampleHash(new Object[]{DATA,i,issue,i%10+1,features,i%2,i%10});return new Object[]{id,i,issue,i%10+1,features,i%2,i%10,hash};}
    void insert(String id,int actualRows,long at)throws Exception{
        StringBuilder digest=new StringBuilder(AtomNetPublicationR31.VERSION);long bytes=0;
        for(int i=0;i<10;i++){Object[] row=row(id,i);digest.append('|').append(row[7]);bytes+=((byte[])row[4]).length+3L*((String)row[2]).length()+128;}
        final long reserved=bytes;final String expected=StatsR27.sha256(digest.toString());
        tx(()->{db.execSQL("INSERT INTO r31_atomnet_publication(publication_id,operator_version,owner,dataset_hash,previous_state_hash,state_hash,state_ref,through_ordinal,materialized_samples,first_sample,sample_count,sample_bytes,sample_digest,status,created_at,published_at) VALUES(?,?,?,?,?,?,?,100,10,0,10,?,?,'PUBLISHED',?,?)",new Object[]{id,AtomNetPublicationR31.VERSION,AtomNetSessionR30.OWNER,DATA,"",OLD_STATE,("R26CAS1:"+OLD_STATE).getBytes("US-ASCII"),reserved,expected,at,at});for(int i=0;i<actualRows;i++)db.execSQL("INSERT INTO r31_atomnet_sample_stage VALUES(?,?,?,?,?,?,?,?)",row(id,i));});
    }
    static Object allocate(Class<?> type)throws Exception {Class<?> u=Class.forName("sun.misc.Unsafe");Field f=u.getDeclaredField("theUnsafe");f.setAccessible(true);return u.getMethod("allocateInstance",Class.class).invoke(f.get(null),type);}
    static void field(Object owner,String name,Object value)throws Exception {Field f=owner.getClass().getDeclaredField(name);f.setAccessible(true);f.set(owner,value);}
    com.ethk3.novaonepro.DbStore store()throws Exception{
        android.content.Context context=new android.app.Application(database.toFile());android.database.sqlite.SQLiteOpenHelper.bindExistingForHostR31(database.toFile(),db);
        com.ethk3.novaonepro.DatabaseCoordinator coordinator=(com.ethk3.novaonepro.DatabaseCoordinator)allocate(com.ethk3.novaonepro.DatabaseCoordinator.class);field(coordinator,"helper",new com.ethk3.novaonepro.DbHelper(context));field(coordinator,"app",context);
        com.ethk3.novaonepro.DbStore store=(com.ethk3.novaonepro.DbStore)allocate(com.ethk3.novaonepro.DbStore.class);field(store,"researchAppCP12",context);field(store,"coordinator",coordinator);return store;
    }
    RuntimeR27 runtime()throws Exception {return (RuntimeR27)allocate(RuntimeR27.class);}
    ResearchPoolR27.Worker worker()throws Exception {ResearchPoolR27 pool=(ResearchPoolR27)allocate(ResearchPoolR27.class);field(pool,"store",store());return pool.new Worker(db,"archive-liveness-host");}
    AtomNetSessionR30 current()throws Exception{
        List<ReplayDatasetR27.Row> rows=new ArrayList<>();for(int i=1;i<=10;i++){String issue="20261115"+String.format(Locale.ROOT,"%04d",i);rows.add(new ReplayDatasetR27.Row(issue,com.ethk3.novaonepro.TargetIssueResolver.sequenceOrdinalSU1(issue),new int[]{1,2,3,4,5,6,7,8,9,10}));}
        ReplayDatasetR27 data=ReplayDatasetR27.of(rows);String observed=rows.get(rows.size()-1).issue;long ordinal=data.ordinals[data.size()-1];
        AtomNetSessionR30 s=new AtomNetSessionR30(data.datasetHash,observed,ordinal,12,20,20,10);s.phase="ONLINE";s.completeIssue=observed;s.onlineObservedOrdinal=ordinal;s.materializedSamples=40;s.cachePeriod=com.ethk3.novaonepro.AtomReplayR27.MIN_ROWS+4;
        ResearchWriteQueueR27.Batch b=new ResearchWriteQueueR27.Batch();AtomNetStoreR30.dataset(b,data,1000);batch("FIXTURE_DATA",b);
        byte[] encoded=s.encode(),stateRef;try(CheckpointArtifactsR26.Prepared p=files.prepareState(o->o.write(encoded))){stateRef=p.reference();}
        byte[] weights=s.trainer.net.encode(),weightsRef=files.prepare(weights);String hash=StatsR27.sha256(encoded);ModelSurfaceProviderR30.FrozenModelRef model=new ModelSurfaceProviderR30.FrozenModelRef(weights,s.trainedThroughOrdinal(),1000);
        tx(()->{db.execSQL("INSERT INTO r31_atomnet_publication VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,'PUBLISHED',?,?)",new Object[]{StatsR27.sha256("current-model"),AtomNetPublicationR31.VERSION,AtomNetSessionR30.OWNER,s.datasetHash,"",hash,stateRef,model.modelHash,model.weightsHash,weightsRef,1000L,s.trainedThroughOrdinal(),s.materializedSamples,0,0,0,StatsR27.sha256(AtomNetPublicationR31.VERSION),null,null,1000L,1000L});db.execSQL("INSERT INTO r29_model_checkpoint VALUES(?,?,?,?,?,?,?)",new Object[]{AtomNetSessionR30.OWNER,AtomNetSessionR30.ABI,s.datasetHash,s.trainedThrough(),hash,stateRef,1000L});});return s;
    }
    void reopen()throws Exception {CheckpointArtifactsR26.unregisterDatabaseRootR31(db);db.close();db=new SQLiteDatabase(DriverManager.getConnection("jdbc:sqlite:"+database));CheckpointArtifactsR26.registerDatabaseRootR31(db,files.rootDirectoryR324());}
    public void close()throws Exception{CheckpointArtifactsR26.unregisterDatabaseRootR31(db);db.close();}
}
