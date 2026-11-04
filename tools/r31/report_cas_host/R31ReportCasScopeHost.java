package com.ethk3.novaonepro;

import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.strategyv2.CheckpointArtifactsR26;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

/** The real export helper, real file pins and CAS resolver; no Android Service lifecycle is simulated. */
public final class R31ReportCasScopeHost {
    static final java.util.concurrent.atomic.AtomicInteger assertions=new java.util.concurrent.atomic.AtomicInteger();static final List<Map<String,Object>> cases=new ArrayList<>();static File files,root,work;static byte[] raw,reference;
    static void need(boolean value,String reason){assertions.incrementAndGet();if(!value)throw new AssertionError(reason);}
    static void record(String name){Map<String,Object> row=new LinkedHashMap<>();row.put("case",name);row.put("status","ACTUAL_NATIVE_HELPER_ASSERTIONS_COMPLETED");cases.add(row);}
    static File database(String name)throws Exception{File path=new File(work,"jobs/"+name+"/snapshot.db");Files.createDirectories(path.getParentFile().toPath());try(Connection c=DriverManager.getConnection("jdbc:sqlite:"+path);Statement s=c.createStatement()){s.execute("CREATE TABLE fixture(id INTEGER PRIMARY KEY)");}return path;}
    static SQLiteDatabase open(File path){SQLiteDatabase db=SQLiteDatabase.openDatabase(path.getPath(),null,SQLiteDatabase.OPEN_READONLY|SQLiteDatabase.NO_LOCALIZED_COLLATORS);need(db.isReadOnly(),"actual JDBC native database connection is read only");return db;}
    static void same(SQLiteDatabase db){need(Arrays.equals(raw,CheckpointArtifactsR26.resolveSerializedR31(db,reference)),"actual CAS manifest/chunk bytes resolved through explicit snapshot mapping");}
    static void dependency(SQLiteDatabase db){try{CheckpointArtifactsR26.resolveSerializedR31(db,reference);throw new AssertionError("UNREGISTERED_SNAPSHOT_GUESSED_CAS_ROOT");}catch(CheckpointArtifactsR26.ArtifactReadR31 expected){need(expected.failureCategory().equals("DEPENDENCY"),"missing source is typed DEPENDENCY, not corrupt model");}}
    static void pinProtected()throws Exception{ReportInputsLeaseCP12 exclusive=ReportInputsLeaseCP12.tryAcquireExclusive(files);if(exclusive!=null)exclusive.close();need(exclusive==null,"actual report input file pin still protects CAS after metadata scope exit");}
    static void released()throws Exception{try(ReportInputsLeaseCP12 exclusive=ReportInputsLeaseCP12.tryAcquireExclusive(files)){need(exclusive!=null,"actual input pin released after readers exit");}}
    static void ordinaryAndNested()throws Exception{
        File path=database("nested");try(SQLiteDatabase db=open(path)){try{db.execSQL("INSERT INTO fixture(id) VALUES(1)");throw new AssertionError("NATIVE_SNAPSHOT_CONNECTION_WRITABLE");}catch(android.database.sqlite.SQLiteException expected){need(expected.getMessage().contains("READONLY"),"native SQLite itself rejects writes on the actual readonly snapshot");}dependency(db);try{ReportExportServiceCP4.openCheckpointSourceR31(db,files);throw new AssertionError("REPORT_SOURCE_REGISTERED_WITHOUT_INPUT_PIN");}catch(IOException expected){need(expected.getMessage().equals("REPORT_CHECKPOINT_INPUT_PIN_REQUIRED"),"production helper requires actual enclosing input pin");}
            try(ReportInputsLeaseCP12 pin=ReportInputsLeaseCP12.acquire(files)){
                ReportExportServiceCP4.CheckpointSourceScopeR31 outer=ReportExportServiceCP4.openCheckpointSourceR31(db,files);need(outer.state.equals("SCOPED_TO_PINNED_CAS_ROOT_OBJECTS_UNVERIFIED"),"source registration never claims model validation");same(db);
                try(ReportExportServiceCP4.CheckpointSourceScopeR31 inner=ReportExportServiceCP4.openCheckpointSourceR31(db,files)){same(db);}
                same(db);outer.close();outer.close();dependency(db);need(!db.isClosed(),"scope ended before actual snapshot connection closes");pinProtected();
            }released();}
        record("actual-read-nested-refcount-idempotent-close-pin-held");
    }
    static void missingAndConflict()throws Exception{
        File absent=new File(work,"missing-app/files"),path=database("missing");Files.createDirectories(absent.toPath());try(SQLiteDatabase db=open(path);ReportInputsLeaseCP12 pin=ReportInputsLeaseCP12.acquire(absent);ReportExportServiceCP4.CheckpointSourceScopeR31 scope=ReportExportServiceCP4.openCheckpointSourceR31(db,absent)){
            need(scope.state.equals("DEPENDENCY_CAS_SOURCE_DIRECTORY_UNAVAILABLE")&&!new File(absent,"r26-checkpoints").exists(),"production helper never creates an empty source directory");need(CheckpointArtifactsR26.resolveSerializedR31(db,raw)==raw,"legacy inline state remains readable without a CAS directory");dependency(db);
        }
        File other=new File(work,"other-app/files");Files.createDirectories(new File(other,"r26-checkpoints").toPath());path=database("conflict");try(SQLiteDatabase db=open(path);ReportInputsLeaseCP12 pin=ReportInputsLeaseCP12.acquire(files);ReportExportServiceCP4.CheckpointSourceScopeR31 original=ReportExportServiceCP4.openCheckpointSourceR31(db,files);ReportInputsLeaseCP12 otherPin=ReportInputsLeaseCP12.acquire(other)){
            try{ReportExportServiceCP4.openCheckpointSourceR31(db,other);throw new AssertionError("SAME_SNAPSHOT_ACCEPTED_DIFFERENT_CAS_DIRECTORY");}catch(IOException expected){need(expected.getMessage().equals("R31_ARTIFACT_DATABASE_ROOT_CONFLICT"),"conflicting explicit root is rejected");}same(db);
        }
        record("missing-root-inline-compatible-no-guess-conflict-rejects");
    }
    static void exceptionAndSlots()throws Exception{
        File path=database("exception");try{try(ReportInputsLeaseCP12 pin=ReportInputsLeaseCP12.acquire(files);SQLiteDatabase db=open(path);ReportExportServiceCP4.CheckpointSourceScopeR31 scope=ReportExportServiceCP4.openCheckpointSourceR31(db,files)){same(db);throw new IOException("EXPLICIT_HOST_CONSUMER_FAILURE");}}catch(IOException expected){need(expected.getMessage().equals("EXPLICIT_HOST_CONSUMER_FAILURE"),"actual consumer exception retained through resource cleanup");}
        try(SQLiteDatabase db=open(path)){dependency(db);}released();
        try(ReportInputsLeaseCP12 pin=ReportInputsLeaseCP12.acquire(files)){for(int i=0;i<40;i++){File next=database("slot-"+i);try(SQLiteDatabase db=open(next)){try(ReportExportServiceCP4.CheckpointSourceScopeR31 scope=ReportExportServiceCP4.openCheckpointSourceR31(db,files)){same(db);}dependency(db);}}pinProtected();}released();record("consumer-exception-finally-and40-report-paths-no-map-leak");
    }
    static void corruptObject()throws Exception{
        File path=database("corrupt"),manifest=new File(root,CheckpointArtifactsR26.referenceHash(reference)+".bin");byte[] original=Files.readAllBytes(manifest.toPath()),bad=original.clone();bad[0]^=1;
        try(ReportInputsLeaseCP12 pin=ReportInputsLeaseCP12.acquire(files);SQLiteDatabase db=open(path);ReportExportServiceCP4.CheckpointSourceScopeR31 scope=ReportExportServiceCP4.openCheckpointSourceR31(db,files)){
            try{Files.write(manifest.toPath(),bad);try{same(db);throw new AssertionError("CORRUPT_CAS_OBJECT_REPORTED_AVAILABLE");}catch(CheckpointArtifactsR26.ArtifactReadR31 expected){need(expected.failureCategory().equals("INTEGRITY"),"checksum corruption stays INTEGRITY rather than missing-source UNKNOWN");}}finally{Files.write(manifest.toPath(),original);}same(db);
        }record("real-checksum-corruption-distinguished-from-missing-source");
    }
    static void cancellation()throws Exception{
        File path=database("cancel");CountDownLatch ready=new CountDownLatch(1);AtomicReference<Throwable> failure=new AtomicReference<>();
        Thread reader=new Thread(()->{try(ReportInputsLeaseCP12 pin=ReportInputsLeaseCP12.acquire(files);SQLiteDatabase db=open(path);ReportExportServiceCP4.CheckpointSourceScopeR31 scope=ReportExportServiceCP4.openCheckpointSourceR31(db,files)){same(db);ready.countDown();new CountDownLatch(1).await(5,TimeUnit.SECONDS);throw new AssertionError("CONTROLLED_CANCEL_NOT_DELIVERED");}catch(InterruptedException expected){Thread.currentThread().interrupt();}catch(Throwable error){failure.set(error);}finally{ready.countDown();}},"actual-report-cas-cancel-reader");
        reader.start();try{need(ready.await(3,TimeUnit.SECONDS)&&failure.get()==null,"actual reader reached live CAS scope");pinProtected();reader.interrupt();}finally{reader.interrupt();reader.join(4000);}
        need(!reader.isAlive()&&failure.get()==null,"cancelled actual thread exited before resources declared released");try(SQLiteDatabase db=open(path)){dependency(db);}released();record("actual-thread-interrupt-scope-finally-exit");
    }
    static void concurrent()throws Exception{
        File path=database("concurrent");CountDownLatch both=new CountDownLatch(2),firstExit=new CountDownLatch(1),releaseFirst=new CountDownLatch(1),releaseLast=new CountDownLatch(1),afterFirst=new CountDownLatch(1);AtomicReference<Throwable> failure=new AtomicReference<>();
        Thread first=new Thread(()->{try(ReportInputsLeaseCP12 pin=ReportInputsLeaseCP12.acquire(files);SQLiteDatabase db=open(path);ReportExportServiceCP4.CheckpointSourceScopeR31 scope=ReportExportServiceCP4.openCheckpointSourceR31(db,files)){same(db);both.countDown();if(!releaseFirst.await(5,TimeUnit.SECONDS))throw new AssertionError("FIRST_READER_DEADLINE");}catch(Throwable t){failure.compareAndSet(null,t);}finally{both.countDown();firstExit.countDown();}},"actual-report-cas-first");
        Thread last=new Thread(()->{try(ReportInputsLeaseCP12 pin=ReportInputsLeaseCP12.acquire(files);SQLiteDatabase db=open(path);ReportExportServiceCP4.CheckpointSourceScopeR31 scope=ReportExportServiceCP4.openCheckpointSourceR31(db,files)){same(db);both.countDown();if(!firstExit.await(5,TimeUnit.SECONDS))throw new AssertionError("FIRST_EXIT_DEADLINE");same(db);afterFirst.countDown();if(!releaseLast.await(5,TimeUnit.SECONDS))throw new AssertionError("LAST_READER_DEADLINE");}catch(Throwable t){failure.compareAndSet(null,t);}finally{both.countDown();afterFirst.countDown();}},"actual-report-cas-last");
        first.start();last.start();try{need(both.await(3,TimeUnit.SECONDS)&&failure.get()==null,"two actual readers hold same snapshot mapping");releaseFirst.countDown();need(afterFirst.await(3,TimeUnit.SECONDS)&&failure.get()==null,"second actual reader still resolves after first scope ends");pinProtected();}finally{releaseFirst.countDown();releaseLast.countDown();first.join(4000);last.join(4000);}
        need(!first.isAlive()&&!last.isAlive()&&failure.get()==null,"both reader threads actually terminated");try(SQLiteDatabase db=open(path)){dependency(db);}released();record("two-native-readers-same-db-last-scope-controls-release");
    }
    public static void main(String[] args)throws Exception{
        work=new File(args[0]);File output=new File(args[1]);Class.forName("org.sqlite.JDBC");files=new File(work,"app/files");Files.createDirectories(files.toPath());root=new File(files,"r26-checkpoints");CheckpointArtifactsR26 store=new CheckpointArtifactsR26(root);raw=new byte[131071];for(int i=0;i<raw.length;i++)raw[i]=(byte)(i*31+7);try(CheckpointArtifactsR26.Prepared saved=store.prepareState(stream->stream.write(raw))){reference=saved.reference();}
        ordinaryAndNested();missingAndConflict();exceptionAndSlots();corruptObject();cancellation();concurrent();
        Map<String,Object> result=new LinkedHashMap<>();result.put("scope","ACTUAL_EXPORT_STATIC_HELPER_REAL_SQLITE_READONLY_REAL_CAS_REAL_FILE_PIN_HOST_ONLY_NOT_SERVICE_ART_NOT_DEVICE");result.put("assertions",assertions.get());result.put("cases",cases);result.put("status","TARGETED_NATIVE_SCOPE_ASSERTIONS_COMPLETED");result.put("rawPayloadSha256",CheckpointArtifactsR26.sha(raw));result.put("rawPayloadBytes",raw.length);Files.write(new File(output,"RESULT.json").toPath(),(com.ethk3.novaonepro.strategyv2.JsonR27.write(result)+"\n").getBytes(StandardCharsets.UTF_8));System.out.println(com.ethk3.novaonepro.strategyv2.JsonR27.write(result));
    }
}
