package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.*;
import java.io.*;
import java.lang.reflect.*;
import java.sql.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import sun.misc.Unsafe;

/** Calls the actual Runtime retention task dispatch, actual live fence and typed
 * DatabaseCoordinator. HOST platform boundaries use native SQLite, no business
 * producer or task implementation is substituted. */
public final class GapRuntimeEntryR31Host {
    static int assertions,tx;static boolean recording;static ReentrantLock lock;
    static void need(boolean ok,String why){assertions++;if(!ok)throw new AssertionError(why);}
    static void set(Object o,String field,Object value)throws Exception{Field f=o.getClass().getDeclaredField(field);f.setAccessible(true);f.set(o,value);}
    static String claimReason(RuntimeCoordinatorR26.Claim claim)throws Exception{Field f=claim.getClass().getDeclaredField("reason");f.setAccessible(true);return String.valueOf(f.get(claim));}
    static long n(SQLiteDatabase db,String sql){return KeepMigrationStoreR31.scalar(db,sql);}
    static final class Db extends SQLiteDatabase {
        int ddl;Db(Connection c){super(c);}
        @Override public void beginTransaction(){if(recording){need(lock.isHeldByCurrentThread(),"ACTUAL_COORDINATOR_OWNS_WRITER");tx++;}super.beginTransaction();}
        @Override public void execSQL(String sql,Object[] args){if(recording&&sql.matches("(?is)^\\s*(CREATE|ALTER|DROP|VACUUM)\\b.*"))ddl++;super.execSQL(sql,args);}
    }
    interface Body {void run(SQLiteDatabase db)throws Exception;}
    static void direct(SQLiteDatabase db,Body body)throws Exception{db.beginTransaction();try{body.run(db);db.setTransactionSuccessful();}finally{db.endTransaction();}}
    static long execute(RuntimeCoordinatorR26 runtime,RuntimeCoordinatorR26.Claim claim)throws Exception {
        Method m=RuntimeCoordinatorR26.class.getDeclaredMethod("execute",RuntimeCoordinatorR26.Claim.class,BudgetTokenSU1.class);m.setAccessible(true);
        try{return ((Number)m.invoke(runtime,claim,new BudgetTokenSU1(60000,3000000,()->false))).longValue();}catch(InvocationTargetException e){Throwable c=e.getCause();if(c instanceof Exception)throw(Exception)c;throw(Error)c;}
    }
    public static void main(String[] args)throws Exception{
        Class.forName("org.sqlite.JDBC");File path=new File(args[0],"databases/runtime.db"),root=new File(args[0],"files/r31-keep");path.getParentFile().mkdirs();root.mkdirs();
        try(Db db=new Db(DriverManager.getConnection("jdbc:sqlite:"+path))){
            // All source shapes below come from the actual production declarations.
            SchemaDomainCoordinatorR926.ensureInfrastructure(db);
            for(String ddl:RuntimeSchemaR26.DDL)if(ddl.startsWith("CREATE TABLE IF NOT EXISTS r26_gap_receipt(")||ddl.startsWith("CREATE TABLE IF NOT EXISTS r26_task(")||ddl.startsWith("CREATE TABLE IF NOT EXISTS r26_session("))db.execSQL(ddl);
            for(String ddl:SchemaR27.DDL)if(ddl.startsWith("CREATE TABLE IF NOT EXISTS r31_task_lease(")||ddl.startsWith("CREATE TABLE IF NOT EXISTS r27_chart_cache("))db.execSQL(ddl);
            for(String ddl:SchemaR27.R31_WRITE_DDL)db.execSQL(ddl);
            for(String ddl:QualificationInboxR29.DDL)if(ddl.startsWith("CREATE TABLE IF NOT EXISTS r29_qual_conflict(")||ddl.startsWith("CREATE TABLE IF NOT EXISTS r29_qual_receipt("))db.execSQL(ddl);
            for(String ddl:ModelSurfaceStoreR30.DDL)if(ddl.startsWith("CREATE TABLE IF NOT EXISTS r30_model_call("))db.execSQL(ddl);
            for(String ddl:AtomNetPublicationR31.DDL)if(ddl.startsWith("CREATE TABLE IF NOT EXISTS r31_atomnet_publication("))db.execSQL(ddl);
            direct(db,actual->{Method install=ResearchRetentionStoreR30.class.getDeclaredMethod("ensureProtectedObjectsR31",SQLiteDatabase.class);install.setAccessible(true);install.invoke(null,actual);SchemaDomainCoordinatorR926.migrateR31MainlineChoice(actual);SchemaDomainCoordinatorR926.migrateR31QualificationReceipts(actual);SchemaDomainCoordinatorR926.migrateR31AtomNetPublication(actual);});
            // P05 legal fallback protocol is completed without a REAL certificate.
            direct(db,actual->{MainlineChoiceStoreR31.appendChoice(actual,"","",CertificateGateR27.GATE_VERSION,"","",1,1,1,1);actual.execSQL("INSERT INTO r27_chart_cache VALUES('mainline','choice',?,1)",new Object[]{JsonR27.write(Collections.singletonMap("choice",MainlineChoiceStoreR31.FALLBACK))});MainlineChoiceStoreR31.switchToTypedRead(actual,1);});
            for(int i=0;i<256&&!LifecycleSchemaR31.ready(db);i++)direct(db,actual->LifecycleSchemaR31.advanceWithinMigration(actual));
            need(LifecycleSchemaR31.ready(db),"REAL_OWNER_ALL_GAP_DECLARATIONS_READY");
            KeepMigrationStoreR31.Writer writer=(op,body)->{try{direct(db,body::run);}catch(Exception e){throw new IllegalStateException(e);}};
            KeepMigrationStoreR31 storeAdapter=new KeepMigrationStoreR31(db,root,writer);
            for(int i=0;i<12;i++)KeepMigrationR31.step(storeAdapter,GapReferenceR31.ARCHIVE);
            for(int i=0;i<8&&!GapReferenceR31.active(db);i++)direct(db,actual->LifecycleSchemaR31.advanceWithinMigration(actual));
            need(GapReferenceR31.active(db),"REAL_OWNER_P05_GAP_REFERENCE_CUTOVER:"+JsonR27.write(KeepMigrationStoreR31.report(db)));
            for(int i=0;i<2060;i++)db.execSQL("INSERT INTO r26_gap_receipt VALUES(?,?,?,1,'CONTROLLED_NATIVE_ORIGINAL',?)",new Object[]{"fixture-"+i,i,i+2,i});
            Map<String,Object> cp=new LinkedHashMap<>();cp.put("version",RetentionR26.PHASE_VERSION);cp.put("phase",20);cp.put("cycleSequence",50000);cp.put("lastCompletedSequence",49999);cp.put("workCursor",0);db.execSQL("INSERT INTO r27_chart_cache VALUES(?,?,?,1)",new Object[]{RetentionR26.PHASE_PANEL,RetentionR26.PHASE_KEY,JsonR27.write(cp)});
            StringJoiner cols=new StringJoiner(",");for(String col:DrawHistoryProjectionR817.COLUMNS)cols.add('"'+col+'"'+(Arrays.asList("issue","source_line","dirty_reason").contains(col)?" TEXT":" INTEGER"));db.execSQL("CREATE TABLE "+DbHelper.TABLE_DRAWS+"("+cols+")");db.execSQL("INSERT INTO "+DbHelper.TABLE_DRAWS+"(_id,issue,open_time,fetch_time,source_line,n1,n2,n3,n4,n5,n6,n7,n8,n9,n10,dirty) VALUES(1,'202001010001',1,1,'CONTROLLED_NATIVE',1,2,3,4,5,6,7,8,9,10,0)");
            String owner="native-runtime-owner";db.execSQL("INSERT INTO r26_session VALUES(?,1,?,'RUNNING',1,1)",new Object[]{owner,IssueTimelineWriterR30.bootId()});db.execSQL("INSERT INTO r26_task(task_id,lane,mechanism,fence,owner,status) VALUES('retention','P3','ORIGINAL_RUNTIME',7,?,'RUNNING')",new Object[]{owner});db.execSQL("INSERT INTO r31_task_lease VALUES('retention',?,7,?,?,0)",new Object[]{owner,IssueTimelineWriterR30.bootId(),android.os.SystemClock.elapsedRealtime()+600000});
            Field u=Unsafe.class.getDeclaredField("theUnsafe");u.setAccessible(true);Unsafe unsafe=(Unsafe)u.get(null);DatabaseCoordinator coordinator=(DatabaseCoordinator)unsafe.allocateInstance(DatabaseCoordinator.class);Field l=DatabaseCoordinator.class.getDeclaredField("WRITE_LOCK_CP3");l.setAccessible(true);lock=(ReentrantLock)l.get(null);
            android.app.Application app=new android.app.Application(path);android.database.sqlite.SQLiteOpenHelper.bindExistingForHostR31(path,db);DbHelper helper=new DbHelper(app);set(coordinator,"writeLock",lock);set(coordinator,"helper",helper);set(coordinator,"app",app);set(coordinator,"waitingWriters",new AtomicInteger());set(coordinator,"criticalBusinessWaitersR914",new AtomicInteger());DbStore store=(DbStore)unsafe.allocateInstance(DbStore.class);set(store,"coordinator",coordinator);set(store,"helper",helper);set(store,"researchAppCP12",app);RuntimeCoordinatorR26 runtime=(RuntimeCoordinatorR26)unsafe.allocateInstance(RuntimeCoordinatorR26.class);set(runtime,"store",store);set(runtime,"retentionProtectionReadyR31",true);set(runtime,"strategyStorageReady",true);RuntimeCoordinatorR26.Claim claim=new RuntimeCoordinatorR26.Claim("retention",owner,7,0,"P3");
            recording=true;
            // Missing new schema on a persisted cutover: original Runtime task
            // returns WAIT, performs no DML, and never reaches the legacy DELETE.
            recording=false;db.execSQL("DROP INDEX idx_r31_gap_reference_pending");recording=true;long changes=n(db,"SELECT total_changes()"),work=execute(runtime,claim);need(work==0&&claimReason(claim).contains("SCHEMA_PENDING:R31_GAP_REFERENCE"),"REAL_RUNTIME_TASK_MISSING_SCHEMA_WAIT");need(changes==n(db,"SELECT total_changes()")&&tx==0,"MISSING_GAP_SCHEMA_DOES_ZERO_BUSINESS_DML");recording=false;direct(db,actual->LifecycleSchemaR31.advanceWithinMigration(actual));for(int i=0;i<16&&!LifecycleSchemaR31.ready(db);i++)direct(db,actual->LifecycleSchemaR31.advanceWithinMigration(actual));recording=true;
            // Stale live task fence rejects the actual coordinator transaction.
            db.execSQL("UPDATE r26_task SET fence=8 WHERE task_id='retention'");changes=n(db,"SELECT total_changes()");boolean rejected=false;try{execute(runtime,claim);}catch(Exception expected){rejected=expected.toString().contains("STALE_FENCE")||expected.getCause()!=null&&expected.getCause().toString().contains("STALE_FENCE");}need(rejected,"REAL_RUNTIME_REJECTS_STALE_FENCE");need(n(db,"SELECT count(*) FROM r31_gap_reference_batch")==0&&n(db,"SELECT count(*) FROM r26_gap_receipt")==2060,"STALE_FENCE_PRESERVES_SOURCE_AND_PLAN");db.execSQL("UPDATE r26_task SET fence=7 WHERE task_id='retention'");
            int slices=0;for(;slices<32&&RetentionR26.checkpoint(db).phase==20;slices++){need(execute(runtime,claim)>0,"ACTUAL_RUNTIME_GAP_COMMITTED_WORK");need(!db.inTransaction()&&!lock.isHeldByCurrentThread(),"ACTUAL_RUNTIME_RELEASES_WRITER_BETWEEN_SLICES");try(Connection critical=DriverManager.getConnection("jdbc:sqlite:"+path)){critical.createStatement().execute("CREATE TABLE IF NOT EXISTS critical_probe(id INTEGER)");critical.createStatement().execute("INSERT INTO critical_probe VALUES(1)");}}
            need(RetentionR26.checkpoint(db).phase==21,"REAL_RUNTIME_ORIGINAL_PHASE_ADVANCED_ATOMICALLY");need(n(db,"SELECT count(*) FROM r26_gap_receipt")==2048&&n(db,"SELECT count(*) FROM r31_gap_reference_original")==12,"REAL_RUNTIME_ORIGINAL_TAIL_REFERENCE_ONLY");need(n(db,"SELECT count(*) FROM r30_keep_r26_gap_receipt")==0,"REAL_RUNTIME_NO_LEGACY_COPY");need(db.ddl==0,"REAL_RUNTIME_RETENTION_PERFORMS_ZERO_DDL");need(tx>1,"REAL_COORDINATOR_MULTIPLE_SHORT_COMMITS");need(n(db,"SELECT count(*) FROM critical_probe")==slices,"CRITICAL_NATIVE_COMMITS_BETWEEN_REAL_RUNTIME_SLICES");
            try(Cursor c=db.rawQuery("SELECT archive_id FROM r31_gap_reference_original",null)){int originals=0;while(c.moveToNext()){Object[] row=KeepMigrationStoreR31.readOriginal(db,GapReferenceR31.ARCHIVE,c.getLong(0),GapReferenceR31.COLUMNS.split(","));need(String.valueOf(row[0]).startsWith("fixture-"),"REAL_RUNTIME_CAS_ORIGINAL_READBACK");originals++;}need(originals==12,"ALL_RUNTIME_CAS_ORIGINALS_READ_BACK");}
            System.out.println("GAP_REAL_RUNTIME_ENTRY_NATIVE_PASS assertions="+assertions+" originalTask=retention phaseBefore=20 phaseAfter=21 realCoordinatorTransactions="+tx+" committedSlices="+slices+" sourceRows=2048 referenceOriginals=12 schemaWaitDml=0 retentionDdl=0 device=NOT_RUN naturalQuality=NOT_PROVEN");
        }
    }
}
