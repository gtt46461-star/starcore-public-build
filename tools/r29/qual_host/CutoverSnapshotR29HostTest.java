package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.lang.reflect.*;

/** Actual production typed page/restore/root on concurrent real WAL files. The HOST shim is not an Android
 * connection-pool emulator; the matching API34 whole-App run remains a separate acceptance requirement. */
public final class CutoverSnapshotR29HostTest {
    static int checks;
    static void check(String name,boolean ok){System.out.println("R29_CUTOVER_SNAPSHOT|"+name+"|"+(ok?"OK":"FAILED"));if(!ok)throw new AssertionError(name);checks++;}
    static long n(SQLiteDatabase db,String sql){try(Cursor c=db.rawQuery(sql,null)){return c.moveToFirst()?c.getLong(0):0;}}
    static String s(SQLiteDatabase db,String sql){try(Cursor c=db.rawQuery(sql,null)){return c.moveToFirst()?c.getString(0):null;}}
    static class Fixture implements AutoCloseable {
        final Path dir,sourceFile;final SQLiteDatabase writer,reader,target;final ExecutorService executor;
        Fixture()throws Exception{
            executor=Executors.newSingleThreadExecutor();dir=Files.createTempDirectory("r29-cutover-snapshot-");sourceFile=dir.resolve("root.db");
            writer=SQLiteDatabase.openOrCreateDatabase(sourceFile.toFile(),null);writer.execSQL("PRAGMA journal_mode=WAL");writer.execSQL("PRAGMA synchronous=FULL");
            writer.execSQL("CREATE TABLE app_state(state_key TEXT PRIMARY KEY,state_value TEXT,updated_at INTEGER DEFAULT 0)");
            writer.execSQL("CREATE TABLE draws(_id INTEGER PRIMARY KEY,issue TEXT NOT NULL UNIQUE,n1 INTEGER,raw_json TEXT,dirty INTEGER DEFAULT 0)");
            writer.execSQL("CREATE TABLE r26_frozen(freeze_id TEXT PRIMARY KEY,payload BLOB,probability REAL,unavailable TEXT)");
            writer.beginTransactionNonExclusive();try{
                for(int i=0;i<96;i++)writer.execSQL("INSERT INTO app_state VALUES(?,?,?)",new Object[]{String.format(Locale.ROOT,"key%03d",i),"汉字\u0000old:"+i,i});
                writer.execSQL("INSERT INTO draws VALUES(1,'202610300001',2,'OLD_CANONICAL',0)");
                writer.execSQL("INSERT INTO r26_frozen VALUES('F1',?,.125,NULL)",new Object[]{new byte[]{0,1,2,(byte)255}});writer.setTransactionSuccessful();
            }finally{writer.endTransaction();}
            reader=SQLiteDatabase.openDatabase(sourceFile.toString(),null,SQLiteDatabase.OPEN_READONLY);reader.execSQL("PRAGMA query_only=1");
            target=SQLiteDatabase.openOrCreateDatabase(dir.resolve("candidate.db").toFile(),null);target.execSQL("PRAGMA journal_mode=WAL");target.execSQL("PRAGMA synchronous=FULL");
        }
        Fixture(Fixture original,SQLiteDatabase snapshot){executor=original.executor;dir=original.dir;sourceFile=original.sourceFile;writer=original.writer;reader=snapshot;target=original.target;}
        long mutate(boolean replace)throws Exception {
            Future<Long> pending=executor.submit(()->{long start=System.nanoTime();writer.beginTransactionNonExclusive();try{
                if(replace)writer.execSQL("INSERT OR REPLACE INTO app_state VALUES('key000','NEW_REPLACED_MOVED_ROWID',1000)");
                else writer.execSQL("UPDATE app_state SET state_value='NEW_UPDATED_SAME_ROWID',updated_at=1000 WHERE state_key='key000'");
                writer.execSQL("UPDATE draws SET n1=9,raw_json='NEW_CANONICAL' WHERE _id=1");
                writer.execSQL("UPDATE r26_frozen SET payload=?,probability=.875 WHERE freeze_id='F1'",new Object[]{new byte[]{9,8,7,0}});
                writer.setTransactionSuccessful();
            }finally{writer.endTransaction();}return (System.nanoTime()-start)/1000000;});
            long ms=pending.get(2,TimeUnit.SECONDS);
            System.out.println("R29_CUTOVER_SNAPSHOT|ROOT_WRITER_COMMITTED_DURING_COPY|milliseconds="+ms);return ms;
        }
        public void close()throws Exception{
            executor.shutdownNow();executor.awaitTermination(2,TimeUnit.SECONDS);target.close();reader.close();writer.close();
            try(java.util.stream.Stream<Path> paths=Files.walk(dir)){paths.sorted(Comparator.reverseOrder()).forEach(p->{try{Files.deleteIfExists(p);}catch(Exception e){throw new RuntimeException(e);}});}
        }
    }
    static final class Copied {
        final List<MigrationCopyR29.Table> tables;final String before,source,target;final long rows;
        Copied(List<MigrationCopyR29.Table> t,String b,String s,String d,long n){tables=t;before=b;source=s;target=d;rows=n;}
    }
    static Copied streamingCopy(Fixture f,boolean replace)throws Exception{
        List<MigrationCopyR29.Table> tables=ResearchCutoverR29.tables(f.reader);String before=ResearchCutoverR29.root(f.reader,tables,Long.MAX_VALUE,()->false);
        for(MigrationCopyR29.Table t:tables)f.target.execSQL(t.createSql);
        long copied=0;boolean mutated=false;
        for(MigrationCopyR29.Table t:tables){Long after=null;for(;;){List<MigrationCopyR29.Row> page=ResearchCutoverR29.page(f.reader,t,after);if(page.isEmpty())break;
            f.target.beginTransactionNonExclusive();try{ResearchCutoverR29.restorePage(f.target,t,page);f.target.setTransactionSuccessful();}finally{f.target.endTransaction();}
            copied+=page.size();after=page.get(page.size()-1).rowid;
            if(t.name.equals("app_state")&&!mutated){check("S1_APPSTATE_EXERCISES_REAL_32_ROW_PAGE_BOUNDARY",page.size()==32);f.mutate(replace);mutated=true;}
        }}
        check("S2_REALTIME_MUTATION_OCCURRED_DURING_COPY",mutated);
        return new Copied(tables,before,ResearchCutoverR29.root(f.reader,tables,Long.MAX_VALUE,()->false),ResearchCutoverR29.root(f.target,tables,Long.MAX_VALUE,()->false),copied);
    }
    interface Body<T>{T run(SQLiteDatabase source)throws Exception;}
    static boolean helperUsed;
    @SuppressWarnings("unchecked") static <T>T pinned(SQLiteDatabase authority,Body<T> body)throws Exception{
        Class<?> api;
        try{api=Class.forName("com.ethk3.novaonepro.strategyv2.ResearchCutoverR29$SnapshotWork");}
        catch(ClassNotFoundException historical){
            // The actual immutable e3dd1043 cutover has no snapshot boundary. Exercise its original
            // production page/restore/root methods, rather than failing a missing method assertion.
            System.out.println("R29_CUTOVER_SNAPSHOT|HISTORICAL_UNPINNED_STREAMING_BOUNDARY");return body.run(authority);
        }
        Method method=ResearchCutoverR29.class.getDeclaredMethod("withSourceSnapshot",SQLiteDatabase.class,api);method.setAccessible(true);
        Object work=Proxy.newProxyInstance(api.getClassLoader(),new Class<?>[]{api},(proxy,called,args)->{
            if(called.getName().equals("run"))return body.run((SQLiteDatabase)args[0]);
            if(called.getName().equals("toString"))return "HOST_REAL_WAL_SNAPSHOT_BODY";
            throw new IllegalStateException("UNEXPECTED_SNAPSHOT_CALLBACK");
        });
        try{helperUsed=true;return (T)method.invoke(null,authority,work);}
        catch(InvocationTargetException failure){Throwable cause=failure.getCause();if(cause instanceof Exception)throw (Exception)cause;if(cause instanceof Error)throw (Error)cause;throw new RuntimeException(cause);}
    }
    static Copied copySnapshot(Fixture f,boolean replace)throws Exception{
        return pinned(f.reader,snapshot->{
            check("S3_PRIVATE_SOURCE_IS_FILE_READONLY_SAME_FILE_AND_SESSION_TX",snapshot!=f.reader&&snapshot.isReadOnly()&&snapshot.getPath().equals(f.reader.getPath())&&n(snapshot,"PRAGMA query_only")==0&&snapshot.inTransaction());
            boolean writeRejected=false;try{snapshot.execSQL("UPDATE app_state SET state_value='FORBIDDEN' WHERE state_key='key001'");}catch(android.database.sqlite.SQLiteException expected){writeRejected=String.valueOf(expected.getMessage()).toLowerCase(Locale.ROOT).contains("readonly");}
            check("S3B_TRUE_FILE_READONLY_REJECTS_DML_WITHOUT_QUERY_ONLY_OVERRIDE",writeRejected);
            // Execute the exact same production methods against the helper's actual pinned connection.
            return streamingCopy(new View(f,snapshot),replace);
        });
    }
    // A view delegates only the reader identity. The real independent WAL writer, target, and mutation thread
    // stay unchanged; no mock snapshot values or test-only production branches are introduced.
    static final class View extends Fixture {
        final Fixture original;
        View(Fixture original,SQLiteDatabase snapshot)throws Exception {super(original,snapshot);this.original=original;}
        @Override long mutate(boolean replace)throws Exception{return original.mutate(replace);}
        @Override public void close(){}
    }
    public static void main(String[]args)throws Exception{
        boolean historical=args.length>0&&args[0].equals("--historical-unpinned-control");
        boolean oldUpdate=args.length>0&&args[0].equals("--historical-unpinned-update-control");
        if(oldUpdate){try(Fixture f=new Fixture()){Copied old=streamingCopy(f,false);if(!old.source.equals(old.target))throw new AssertionError("CUTOVER_UNPINNED_REALTIME_UPDATE_MIXED_TYPED_ROOT");}throw new AssertionError("HISTORICAL_UPDATE_NEGATIVE_UNEXPECTED_PASS");}
        try(Fixture f=new Fixture()){
            Copied copied;
            try{copied=historical?streamingCopy(f,true):copySnapshot(f,true);}
            catch(android.database.sqlite.SQLiteException failure){
                if(String.valueOf(failure.getMessage()).contains("UNIQUE constraint failed: app_state.state_key"))throw new AssertionError("CUTOVER_MUTABLE_APPSTATE_KEY_REAPPEARED_WITHOUT_SNAPSHOT",failure);
                throw failure;
            }
            check("S4_COMPLETE_TYPED_COPY_AND_BOTH_HASHES_AGREE_DURING_REPLACE",copied.rows==98&&copied.before.equals(copied.source)&&copied.source.equals(copied.target));
            check("S5_TEXT_PK_ROWID_REPLACEMENT_DID_NOT_DUPLICATE_COPIED_KEY",n(f.target,"SELECT count(*) FROM app_state")==96&&n(f.target,"SELECT rowid FROM app_state WHERE state_key='key000'")==1&&s(f.target,"SELECT state_value FROM app_state WHERE state_key='key000'").equals("汉字"+(char)0+"old:0"));
            check("S6_CONCURRENT_ROOT_WRITES_COMMITTED_WITHOUT_RESEARCH_COPY_AUTHORITY",n(f.writer,"SELECT rowid FROM app_state WHERE state_key='key000'")==97&&n(f.writer,"SELECT n1 FROM draws WHERE _id=1")==9&&s(f.writer,"SELECT state_value FROM app_state WHERE state_key='key000'").equals("NEW_REPLACED_MOVED_ROWID"));
            check("S7_SNAPSHOT_PRESERVES_CROSS_TABLE_DRAW_AND_CRITICAL_INPUT_IMAGE",n(f.target,"SELECT n1 FROM draws WHERE _id=1")==2&&n(f.target,"SELECT count(*) FROM r26_frozen WHERE probability=.125 AND unavailable IS NULL AND hex(payload)='000102FF'")==1);
            String current=ResearchCutoverR29.root(f.reader,copied.tables,Long.MAX_VALUE,()->false);
            check("S8_RELEASED_SNAPSHOT_NEXT_OBSERVATION_SEES_REAL_COMMITTED_ROOT",!current.equals(copied.before)&&n(f.reader,"SELECT n1 FROM draws WHERE _id=1")==9);
            String next=pinned(f.reader,db->ResearchCutoverR29.root(db,copied.tables,Long.MAX_VALUE,()->false));
            check("S9_FRESH_SNAPSHOT_REBINDS_REAL_CURRENT_ROOT",next.equals(current));
            f.target.execSQL("UPDATE r26_frozen SET probability=.375 WHERE freeze_id='F1'");
            check("S10_GENUINE_TARGET_CORRUPTION_STILL_FAILS_FULL_TYPED_ROOT",!copied.before.equals(ResearchCutoverR29.root(f.target,copied.tables,Long.MAX_VALUE,()->false)));
        }
        try(Fixture f=new Fixture()){
            Copied copied=copySnapshot(f,false);
            check("S11_NON_MOVING_UPDATE_CANNOT_CREATE_MIXED_CROSS_TABLE_SNAPSHOT",copied.before.equals(copied.source)&&copied.source.equals(copied.target)&&n(f.writer,"SELECT rowid FROM app_state WHERE state_key='key000'")==1);
        }
        try(Fixture f=new Fixture()){
            boolean thrown=false;SQLiteDatabase[] observed={null};
            try{pinned(f.reader,db->{observed[0]=db;f.mutate(false);throw new IllegalStateException("HOST_CONTROLLED_COPY_CANCELLATION");});}
            catch(IllegalStateException expected){thrown=expected.getMessage().equals("HOST_CONTROLLED_COPY_CANCELLATION");}
            check("S12_FAILURE_PRESERVES_ERROR_AND_RELEASES_PRIVATE_SNAPSHOT",thrown&&observed[0]!=null&&!observed[0].isOpen()&&f.reader.isOpen()&&n(f.reader,"SELECT n1 FROM draws WHERE _id=1")==9);
        }
        check("S13_ACTUAL_PRODUCT_SNAPSHOT_HELPER_EXECUTED",helperUsed);
        System.out.println("R29_CUTOVER_SNAPSHOT|ALL_PASS|checks="+checks+"|REAL_WAL_CONCURRENT_ROOT_WRITER_HOST_ONLY_ANDROID_SNAPSHOT_ACCEPTANCE_SEPARATE");
    }
}
