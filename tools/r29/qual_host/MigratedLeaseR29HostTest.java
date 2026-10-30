package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.*;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

/** Actual migration lease release and original fenced claim, on a copied real WAL database.
 * This does not simulate Android service timing or claim natural/device acceptance. */
public final class MigratedLeaseR29HostTest {
    static int checks;
    static final String SESSION="SYNTHETIC_DRAINED_LEASE_OWNER",SCHEMA=StatsR27.sha256("SCHEMA"),SOURCE=StatsR27.sha256("ROOT");
    static void check(String id,boolean ok){System.out.println("R29_MIGRATED_LEASE|"+id+"|"+(ok?"OK":"FAILED"));if(!ok)throw new AssertionError(id);checks++;}
    static Object get(Object o,String name)throws Exception{Field f=o instanceof Class?((Class<?>)o).getDeclaredField(name):o.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(o instanceof Class?null:o);}
    static void set(Object o,String name,Object v)throws Exception{Field f=o instanceof Class?((Class<?>)o).getDeclaredField(name):o.getClass().getDeclaredField(name);f.setAccessible(true);f.set(o instanceof Class?null:o,v);}
    static Object invoke(Object o,String name)throws Exception{Method m=o.getClass().getDeclaredMethod(name);m.setAccessible(true);try{return m.invoke(o);}catch(InvocationTargetException e){Throwable t=e.getCause();if(t instanceof Exception)throw(Exception)t;throw(Error)t;}}
    static long n(SQLiteDatabase db,String sql){return EventYieldR29HostTest.n(db,sql);}
    static String rows(SQLiteDatabase db,String sql){StringBuilder b=new StringBuilder();try(Cursor c=db.rawQuery(sql,null)){while(c.moveToNext()){for(int i=0;i<c.getColumnCount();i++){if(c.isNull(i))b.append("NULL");else if(c.getType(i)==Cursor.FIELD_TYPE_BLOB)b.append(Base64.getEncoder().encodeToString(c.getBlob(i)));else b.append(c.getString(i));b.append('|');}b.append('\n');}}return b.toString();}
    static String tasks(SQLiteDatabase db){return rows(db,"SELECT * FROM r26_task ORDER BY task_id");}
    static String preserved(SQLiteDatabase db){return rows(db,"SELECT * FROM r26_progress ORDER BY receipt_id")+rows(db,"SELECT * FROM r26_live_model ORDER BY policy_hash")+rows(db,"SELECT * FROM r26_session ORDER BY session_id");}
    static final class Fixture implements AutoCloseable {
        final EventYieldR29HostTest.Fixture f=new EventYieldR29HostTest.Fixture();final RuntimeCoordinatorR26 runtime;
        final SQLiteDatabase rp;final Path path;final long lease=System.currentTimeMillis()+30000;final Object previous;
        Fixture()throws Exception{
            previous=get(RuntimeCoordinatorR26.class,"active");
            f.root.beginTransaction();try{
                RuntimeSchemaR26.ensure(f.root);LegacyCapabilityAdapterR29.initialize(f.root,SESSION);
                f.root.execSQL("INSERT INTO r26_session VALUES(?,1,'HOST','RUNNING',1,19)",new Object[]{SESSION});
                for(String id:new String[]{"schema","backoff","foreign-session","changed-fence","changed-cursor","changed-lease","root-already-yielded","r27-running","rp-quarantined"}){
                    String owner=id.equals("foreign-session")?"FOREIGN_SESSION":SESSION,lane=id.equals("r27-running")?"R27":"P1";
                    f.root.execSQL("INSERT INTO r26_task(task_id,lane,mechanism,cursor,fence,lease_until,owner,status,next_at,last_progress,steps,last_cpu_ms) VALUES(?,?,?,267,7,?,?,'RUNNING',?,123,17,8)",new Object[]{id,lane,id,lease,owner,id.equals("backoff")?System.currentTimeMillis()+300000:0});
                }
                f.root.execSQL("UPDATE r26_task SET status='YIELDED' WHERE task_id='root-already-yielded'");
                f.root.execSQL("INSERT INTO r26_progress(task_id,fence,cursor_before,cursor_after,cpu_ms,blocked_reason,recorded_at) VALUES('schema',6,266,267,8,NULL,123)");
                f.root.execSQL("INSERT INTO r26_live_model(policy_hash,canonical_text,state,state_hash,checkpoint_hash,parent_hash,through_ordinal,genesis_ordinal,born_ordinal,updated_at) VALUES('SYNTHETIC_MODEL','SYNTHETIC_WIRE',X'0001ff','HASH','CKPT','PARENT',267,1,268,123)");
                f.root.setTransactionSuccessful();
            }finally{f.root.endTransaction();}
            try(Cursor c=f.root.rawQuery("PRAGMA wal_checkpoint(TRUNCATE)",null)){if(!c.moveToFirst()||c.getInt(0)!=0)throw new AssertionError("FIXTURE_CHECKPOINT_REQUIRED");}
            path=Paths.get(f.root.getPath()).getParent().resolve(ResearchRouteR29.FILE_NAME);Files.copy(Paths.get(f.root.getPath()),path);
            rp=SQLiteDatabase.openDatabase(path.toString(),null,SQLiteDatabase.OPEN_READWRITE);rp.execSQL("PRAGMA journal_mode=WAL");rp.execSQL(ResearchRouteR29.OWNER_DDL);rp.execSQL("INSERT INTO r29_research_owner VALUES(1,'READY',?,?,17,1)",new Object[]{SCHEMA,SOURCE});
            rp.execSQL("UPDATE r26_task SET fence=9 WHERE task_id='changed-fence'");rp.execSQL("UPDATE r26_task SET cursor=268 WHERE task_id='changed-cursor'");rp.execSQL("UPDATE r26_task SET lease_until=lease_until+1 WHERE task_id='changed-lease'");rp.execSQL("UPDATE r26_task SET status='RUNNING' WHERE task_id='root-already-yielded'");
            rp.execSQL("UPDATE r26_task SET status='QUARANTINED' WHERE task_id='rp-quarantined'");
            rp.execSQL("INSERT INTO r26_task(task_id,lane,mechanism,cursor,fence,lease_until,owner,status) VALUES('rp-only','P1','rp-only',267,7,?,?,'RUNNING')",new Object[]{lease,SESSION});
            f.root.execSQL(ResearchRouteR29.ROUTE_DDL);f.root.execSQL("INSERT INTO r29_database_route VALUES(1,'ACTIVE',?,?,?,17,1)",new Object[]{ResearchRouteR29.FILE_NAME,SCHEMA,SOURCE});
            f.root.execSQL(ResearchCutoverR29.PUBLICATION);f.root.execSQL("INSERT INTO r29_migration_publication VALUES(1,17,'candidate','PUBLISHED','baseline',?,?,'',1)",new Object[]{StatsR27.sha256("BASELINE"),SOURCE});
            f.root.beginTransaction();try{LegacyCapabilityAdapterR29.publishWithin(f.root,SESSION);f.root.setTransactionSuccessful();}finally{f.root.endTransaction();}
            runtime=(RuntimeCoordinatorR26)EventYieldR29HostTest.allocate(RuntimeCoordinatorR26.class);set(runtime,"store",f.store);set(runtime,"owner",SESSION);set(runtime,"worker",Thread.currentThread());set(runtime,"legacySlice",new ReentrantLock());set(runtime,"signal",new Object());set(runtime,"service",new CapabilityServiceR26());set(RuntimeCoordinatorR26.class,"active",runtime);
        }
        long release(boolean tolerateOld)throws Exception{try{return(Long)invoke(runtime,"releaseMigratedLeases");}catch(NoSuchMethodException old){if(tolerateOld)return 0;throw old;}}
        RuntimeCoordinatorR26.Claim claim()throws Exception{
            set(runtime,"poolDispatcher",true);
            try(LegacyResearchScopeR29.Scope scope=LegacyResearchScopeR29.enter(f.store)){
                return ResearchRouteR29.write(f.store,"SYNTHETIC_NESTED_CLAIM",db->{
                    // Bind the actual writer scope used by nested legacy callbacks. No claim SQL or
                    // scheduler decision is replaced; normal mirror bootstrap is a separate suite.
                    @SuppressWarnings("unchecked") ThreadLocal<SQLiteDatabase> writer=(ThreadLocal<SQLiteDatabase>)get(LegacyResearchScopeR29.class,"WRITER");SQLiteDatabase prior=writer.get();writer.set(db);
                    try{return(RuntimeCoordinatorR26.Claim)invoke(runtime,"claim");}finally{if(prior==null)writer.remove();else writer.set(prior);}
                });
            }
        }
        public void close()throws Exception{set(RuntimeCoordinatorR26.class,"active",previous);SQLiteDatabase.hostBeforeQuery=null;rp.close();f.close();}
    }
    interface Operation {void run()throws Exception;}
    static void rejectedWithoutChange(Fixture f,String id,Operation action)throws Exception{String before=tasks(f.rp);boolean rejected=false;try{action.run();}catch(IllegalStateException expected){rejected=true;}check(id,rejected&&before.equals(tasks(f.rp)));}
    public static void main(String[]args)throws Exception{
        boolean old=args.length>0&&args[0].equals("--old-control");
        try(Fixture f=new Fixture()){
            String rootTasks=tasks(f.f.root),rootData=preserved(f.f.root),rpData=preserved(f.rp);
            if(!old){
                f.f.root.execSQL("UPDATE r29_scheduler_owner SET dispatcher='LEGACY' WHERE singleton=1");rejectedWithoutChange(f,"M2_OLD_DISPATCHER_NOT_PUBLISHED_CANNOT_RELEASE",()->f.release(false));f.f.root.execSQL("UPDATE r29_scheduler_owner SET dispatcher='RESEARCH_POOL' WHERE singleton=1");
                set(f.runtime,"worker",new Thread());rejectedWithoutChange(f,"M3_OTHER_THREAD_CANNOT_RELEASE_LIVE_DISPATCHER_LEASES",()->f.release(false));set(f.runtime,"worker",Thread.currentThread());
                ReentrantLock lock=(ReentrantLock)get(f.runtime,"legacySlice");lock.lock();try{rejectedWithoutChange(f,"M4_UNDRAINED_POOL_SLICE_CANNOT_RELEASE",()->f.release(false));}finally{lock.unlock();}
                RuntimeCoordinatorR26.bindBackgroundClaim(new RuntimeCoordinatorR26.Claim("schema",SESSION,7,267));try{rejectedWithoutChange(f,"M5_IN_FLIGHT_WRITING_CLAIM_CANNOT_RELEASE",()->f.release(false));}finally{RuntimeCoordinatorR26.bindBackgroundClaim(null);}
                set(f.runtime,"cancelled",true);rejectedWithoutChange(f,"M6_CANCELLED_RUNTIME_CANNOT_RELEASE",()->f.release(false));set(f.runtime,"cancelled",false);
                f.rp.execSQL("UPDATE r29_research_owner SET source_root=? WHERE singleton=1",new Object[]{StatsR27.sha256("WRONG_ROOT")});rejectedWithoutChange(f,"M7_WRONG_RESEARCH_OWNER_REJECTED_WITHOUT_TASK_MUTATION",()->f.release(false));f.rp.execSQL("UPDATE r29_research_owner SET source_root=? WHERE singleton=1",new Object[]{SOURCE});
                f.rp.execSQL("UPDATE r29_research_owner SET epoch=18 WHERE singleton=1");rejectedWithoutChange(f,"M15_NEW_RESEARCH_EPOCH_CANNOT_RELEASE_OLD_COPIED_LEASE",()->f.release(false));f.rp.execSQL("UPDATE r29_research_owner SET epoch=17 WHERE singleton=1");
                f.f.root.beginTransaction();try{rejectedWithoutChange(f,"M16_ROOT_TRANSACTION_CANNOT_ENTER_RESEARCH_LEASE_WRITER",()->f.release(false));}finally{f.f.root.endTransaction();}
                rejectedWithoutChange(f,"M17_RESEARCH_WRITER_CANNOT_REENTER_LEASE_RELEASE",()->ResearchRouteR29.write(f.f.store,"SYNTHETIC_EXISTING_RESEARCH_WRITER",db->f.release(false)));
                AtomicInteger injected=new AtomicInteger();String before=tasks(f.rp);SQLiteDatabase.hostBeforeQuery=(db,sql)->{if(db.getPath().equals(f.path.toString())&&sql.equals("SELECT changes()")&&injected.getAndIncrement()==0)throw new IllegalStateException("SYNTHETIC_SQL_FAILURE_AFTER_FIRST_LEASE_UPDATE");};boolean failed=false;
                try{f.release(false);}catch(IllegalStateException expected){failed=true;}finally{SQLiteDatabase.hostBeforeQuery=null;}
                check("M8_REAL_TRANSACTION_FAILURE_ROLLS_BACK_EVERY_RELEASE",failed&&injected.get()>0&&before.equals(tasks(f.rp)));
            }
            long released=f.release(old);RuntimeCoordinatorR26.Claim claim=f.claim();
            check("M1_COPY_DRAIN_HANDOFF_CLAIMABLE_WITHOUT_30S_IDLE",claim!=null&&claim.task.equals("schema")&&claim.cursor==267&&claim.fence==9&&f.lease>System.currentTimeMillis());
            if(old)return;
            check("M9_ONLY_EXACT_COPIED_LEASES_RELEASED",released==2&&n(f.rp,"SELECT count(*) FROM r26_task WHERE task_id IN ('foreign-session','changed-fence','changed-cursor','changed-lease','root-already-yielded','rp-only','r27-running') AND status='RUNNING' AND lease_until>0")==7);
            check("M10_BACKOFF_RETAINED_AFTER_LEASE_RELEASE",n(f.rp,"SELECT count(*) FROM r26_task WHERE task_id='backoff' AND status='YIELDED' AND owner IS NULL AND lease_until=0 AND fence=8 AND next_at>0 AND cursor=267 AND last_progress=123")==1);
            check("M14_QUARANTINED_COPIED_TASK_NEVER_REVIVED",n(f.rp,"SELECT count(*) FROM r26_task WHERE task_id='rp-quarantined' AND status='QUARANTINED' AND fence=7 AND lease_until>0")==1);
            boolean stale=false;try{RuntimeCoordinatorR26.requireFence(f.rp,new RuntimeCoordinatorR26.Claim("schema",SESSION,7,267));}catch(IllegalStateException expected){stale=true;}
            check("M11_OLD_FENCE_REJECTED_AND_NEW_FENCE_VALID",stale);RuntimeCoordinatorR26.requireFence(f.rp,claim);
            check("M12_ROOT_SNAPSHOT_AND_ALL_MODEL_PROGRESS_BYTES_RETAINED",rootTasks.equals(tasks(f.f.root))&&rootData.equals(preserved(f.f.root))&&rpData.equals(preserved(f.rp)));
            check("M13_REPEATED_RELEASE_CANNOT_RESET_A_NEW_CLAIM",f.release(false)==0&&n(f.rp,"SELECT fence FROM r26_task WHERE task_id='schema'")==9);
        }
        System.out.println("R29_MIGRATED_LEASE|ALL_PASS|checks="+checks+"|REAL_COPIED_SQLITE_LEASE_AND_FENCED_CLAIM_HOST_NOT_DEVICE_TIMING");
    }
}
