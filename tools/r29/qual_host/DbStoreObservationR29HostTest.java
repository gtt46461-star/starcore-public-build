package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import com.ethk3.novaonepro.*;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

/** Actual DbStore getter bodies and independent real WAL handles; the shared Android RW pool monitor is
 * explicitly modelled. JDBC does not reproduce Android pool internals or prove device latency. */
public final class DbStoreObservationR29HostTest {
    static int checks;
    static void check(String name,boolean ok){System.out.println("R29_DBSTORE_OBSERVATION|"+name+"|"+(ok?"OK":"FAILED"));if(!ok)throw new AssertionError(name);checks++;}
    static final class Primary extends SQLiteDatabase {
        final ReentrantLock monitor=new ReentrantLock();final AtomicInteger reads=new AtomicInteger();
        Primary(SQLiteDatabase original){super(original.jdbc);}
        @Override public Cursor rawQuery(String sql,String[] args){boolean lease=sql.contains("draws")||sql.contains("app_state");if(lease){reads.incrementAndGet();try{monitor.lockInterruptibly();}catch(InterruptedException cancelled){Thread.currentThread().interrupt();throw new IllegalStateException("SYNTHETIC_PRIMARY_CANCELLED",cancelled);}}try{return super.rawQuery(sql,args);}finally{if(lease)monitor.unlock();}}
    }
    static void drawColumns(SQLiteDatabase db){
        db.execSQL("ALTER TABLE draws ADD COLUMN _id INTEGER DEFAULT 1");db.execSQL("ALTER TABLE draws ADD COLUMN open_time INTEGER DEFAULT 1000");db.execSQL("ALTER TABLE draws ADD COLUMN fetch_time INTEGER DEFAULT 1100");db.execSQL("ALTER TABLE draws ADD COLUMN source_line TEXT DEFAULT 'SYNTHETIC'");db.execSQL("ALTER TABLE draws ADD COLUMN dirty_reason TEXT DEFAULT ''");db.execSQL("ALTER TABLE draws ADD COLUMN raw_json TEXT DEFAULT ''");db.execSQL("CREATE VIEW "+DrawDependencyInputCP12.SOURCE+" AS SELECT * FROM draws");
    }
    static void getters(DbStore store){
        check("D01_LATEST_AND_FIRST_ACTUAL_GETTERS",store.getLatestDraw().issue.equals("202610300001")&&store.getFirstDraw().issue.equals("202610300001"));
        check("D02_ISSUE_AND_RECENT_ACTUAL_GETTERS",store.hasDrawIssue("202610300001")&&store.getDrawByIssue("202610300001").numbers[9]==10&&store.getRecentDraws(4).size()==1&&store.getDrawsAsc(4).size()==1);
        check("D03_BOUNDED_RANGE_AND_COUNT_GETTERS",store.getDrawsBeforeIssue("202610300002",4).size()==1&&store.getDrawsBetweenIssues("202610300001","202610300002",4).size()==1&&store.countDraws()==1);
        check("D04_STATE_AND_PREFIX_ACTUAL_GETTERS",store.getState("current_issue","").equals("ROOT_CURRENT")&&store.readStatePrefixR923("current_",4).get("current_issue").equals("ROOT_CURRENT"));
        HashMap<String,String> ui=new HashMap<>();check("D05_UI_SNAPSHOT_ACTUAL_GETTER",store.readUiStateSnapshotR816(ui,64,256)&&ui.get("current_issue").equals("ROOT_CURRENT"));ui.clear();store.readRealtimeUiStateR71(ui);check("D06_FIXED_UI_KEYS_ACTUAL_GETTER",ui.get("current_issue").equals("ROOT_CURRENT"));
    }
    public static void main(String[]args)throws Exception{
        try(EventYieldR29HostTest.Fixture f=new EventYieldR29HostTest.Fixture()){
            drawColumns(f.root);drawColumns(f.research);f.root.execSQL("INSERT INTO app_state VALUES('current_issue','ROOT_CURRENT',1)");f.research.execSQL("INSERT INTO app_state VALUES('current_issue','OLD_UNPUBLISHED_RP',1)");
            EventYieldR29HostTest.set(f.store,"uiStateReadR945",new ThreadLocal<Map<String,String>>());Primary primary=new Primary(f.root);DbHelper helper=(DbHelper)EventYieldR29HostTest.get(f.coordinator,"helper");SQLiteOpenHelper.bind(helper,primary);
            ExecutorService reader=Executors.newSingleThreadExecutor();primary.monitor.lock();Future<?> result=null;
            try{long start=System.nanoTime();result=reader.submit(()->getters(f.store));try{result.get(1000,TimeUnit.MILLISECONDS);}catch(TimeoutException blocked){throw new AssertionError("DBSTORE_GETTERS_BORROWED_BLOCKED_ROOT_RW_POOL",blocked);}long wall=(System.nanoTime()-start)/1000000;System.out.println("R29_DBSTORE_OBSERVATION|PROFILE|getterGroupWallMs="+wall+"|sharedRootRwReads="+primary.reads.get()+"|HOST_MODEL_ONLY");check("D07_ALL_GETTERS_FINISH_WITH_SHARED_RW_POOL_HELD",wall<1000&&primary.reads.get()==0);}
            finally{if(result!=null&&!result.isDone())result.cancel(true);primary.monitor.unlock();reader.shutdownNow();reader.awaitTermination(2,TimeUnit.SECONDS);}
            check("D08_PUBLIC_READABLE_API_REMAINS_ORIGINAL_RW",f.store.readable()==primary&&!f.store.readable().isReadOnly());
            Method select=DbStore.class.getDeclaredMethod("observationReaderR29");select.setAccessible(true);SQLiteDatabase observation=(SQLiteDatabase)select.invoke(f.store);
            check("D09_GETTER_HANDLE_IS_READONLY_SAME_ROOT_FILE",observation!=primary&&observation.isReadOnly()&&observation.getPath().equals(primary.getPath())&&EventYieldR29HostTest.n(observation,"PRAGMA query_only")==1);
            f.root.beginTransaction();try{f.root.execSQL("UPDATE app_state SET state_value='UNCOMMITTED_OWN_WRITE' WHERE state_key='current_issue'");check("D10_ROOT_TRANSACTION_READS_OWN_UNCOMMITTED_WRITE",select.invoke(f.store)==primary&&f.store.getState("current_issue","").equals("UNCOMMITTED_OWN_WRITE"));}finally{f.root.endTransaction();}
            check("D11_ROLLBACK_RETAINS_ORIGINAL_BUSINESS_STATE",f.store.getState("current_issue","").equals("ROOT_CURRENT"));
            try(LegacyResearchScopeR29.Scope scope=LegacyResearchScopeR29.enter(f.store)){check("D11A_UNPUBLISHED_RESEARCH_SCOPE_RETAINS_ROOT",f.store.getState("current_issue","").equals("ROOT_CURRENT"));}
            Path rpPath=Paths.get(primary.getPath()).getParent().resolve(ResearchRouteR29.FILE_NAME);
            try(SQLiteDatabase rp=SQLiteDatabase.openOrCreateDatabase(rpPath.toFile(),null)){
                rp.execSQL("PRAGMA journal_mode=WAL");rp.execSQL("CREATE TABLE app_state(state_key TEXT PRIMARY KEY,state_value TEXT,updated_at INTEGER)");rp.execSQL("INSERT INTO app_state VALUES('current_issue','ACTIVE_RP_CURRENT',1)");rp.execSQL(ResearchRouteR29.OWNER_DDL);rp.execSQL("INSERT INTO r29_research_owner VALUES(1,'READY','WRONG_SCHEMA','ROOT',9,1)");f.root.execSQL(ResearchRouteR29.ROUTE_DDL);f.root.execSQL("INSERT INTO r29_database_route VALUES(1,'ACTIVE',?,'SCHEMA','ROOT',9,1)",new Object[]{ResearchRouteR29.FILE_NAME});
                check("D12_OUTSIDE_RESEARCH_SCOPE_STAYS_REAL_ROOT_AUTHORITY",f.store.getState("current_issue","").equals("ROOT_CURRENT"));
                boolean wrongOwner=false;try(LegacyResearchScopeR29.Scope scope=LegacyResearchScopeR29.enter(f.store)){try{f.store.getState("current_issue","");}catch(IllegalStateException expected){wrongOwner=expected.getMessage().contains("OWNER_MISMATCH");}}check("D12A_ACTIVE_WRONG_OWNER_REJECTS_WITHOUT_ROOT_FALLBACK",wrongOwner);rp.execSQL("UPDATE r29_research_owner SET schema_hash='SCHEMA'");
                try(LegacyResearchScopeR29.Scope scope=LegacyResearchScopeR29.enter(f.store)){SQLiteDatabase selected=(SQLiteDatabase)select.invoke(f.store);check("D13_ACTIVE_RESEARCH_SCOPE_RETAINS_VERIFIED_RP_ROUTE",selected.getPath().equals(rpPath.toString())&&EventYieldR29HostTest.n(selected,"PRAGMA query_only")==1&&f.store.getState("current_issue","").equals("ACTIVE_RP_CURRENT"));}
                check("D14_RESEARCH_SCOPE_CLOSE_RESTORES_ROOT_AUTHORITY",f.store.getState("current_issue","").equals("ROOT_CURRENT"));
            }
            f.root.execSQL("DROP TABLE draws");boolean missing=false;try{f.store.getLatestDraw();}catch(android.database.sqlite.SQLiteException expected){missing=expected.getMessage().contains("no such table");}check("D15_REAL_GETTER_SQL_ERROR_NOT_CONVERTED_TO_EMPTY_DATA",missing);
            check("D16_ORIGINAL_ROOT_ROWS_RETAINED",EventYieldR29HostTest.n(f.root,"SELECT count(*) FROM app_state WHERE state_key='current_issue' AND state_value='ROOT_CURRENT'")==1);
        }
        System.out.println("R29_DBSTORE_OBSERVATION|ALL_PASS|checks="+checks+"|ACTUAL_GETTERS_REAL_SQLITE_MODELLED_ANDROID_POOL_NOT_DEVICE_ACCEPTANCE");
    }
}
