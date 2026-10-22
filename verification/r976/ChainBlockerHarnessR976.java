package com.ethk3.novaonepro;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.r952.ResearchMigrationRegistryR952;

/** Regression tests execute product coordinator/migrations against actual SQLite.
 * The Android-only transport is replaced by the same explicit host adapter as R974.
 */
public final class ChainBlockerHarnessR976 {
    private static int checks, failures;
    private static void check(boolean result, String name) {
        checks++;
        if (!result) failures++;
        System.out.println((result ? "PASS " : "FAIL ") + name);
    }
    static final class CountDb extends SQLiteDatabase {
        long ddl, queries;
        boolean uncertainCommit;
        CountDb(String bridge) throws Exception { super(bridge); }
        private void count(String sql) {
            String s=sql.trim().toUpperCase(java.util.Locale.US);
            if(s.startsWith("CREATE ")||s.startsWith("ALTER ")||s.startsWith("DROP ")) ddl++;
        }
        @Override public synchronized void execSQL(String sql) { count(sql); super.execSQL(sql); }
        @Override public synchronized void execSQL(String sql,Object[] args) { count(sql); super.execSQL(sql,args); }
        @Override public synchronized Cursor rawQuery(String sql,String[] args) { queries++; return super.rawQuery(sql,args); }
        @Override public void endTransaction() {
            super.endTransaction();
            if (uncertainCommit) { uncertainCommit=false; throw new IllegalStateException("HOST_UNCERTAIN_COMMIT_RETURN"); }
        }
    }
    private static boolean ensure(DbStore store) {
        return store.coordinatorForResearchR952().runAtomicWrite("R976_SCHEMA_CACHE_REGRESSION", new DatabaseCoordinator.WriteRunnable() {
            public void run(SQLiteDatabase db) { ResearchMigrationRegistryR952.ensure(db); }
        });
    }
    private static boolean exists(SQLiteDatabase db,String name) {
        try(Cursor c=db.rawQuery("SELECT 1 FROM sqlite_master WHERE name=?",new String[]{name})) { return c.moveToFirst(); }
    }
    private static void pending() {
        String[] incomplete={"LIFECYCLE_PENDING|reviewed=24", "HISTORICAL_IMPORT=DONE|LIFECYCLE_PENDING|reviewed=24",
            "HISTORICAL_IMPORT=DONE|result=LIFECYCLE_PENDING|remaining=4", "stage:DEV_RUNNING|generation=2",
            "result=EVOLUTION_PENDING|completed=0", "R952_PENDING|advanced=1", "wrapper=ATOMIC_PENDING",
            "wrapper=DEV_REGISTERED", "wrapper=DEV_ROUND_PENDING", "wrapper=CURRENT_PENDING", "", "ERROR:SQL", "YIELD:PREEMPT"};
        for(String s:incomplete) check(!ResearchLearningContinuityR912.terminalReviewOutcome(s),"PENDING_NOT_COMPLETED:"+s);
        for(String s:new String[]{"HISTORICAL_IMPORT=DONE|LIFECYCLE_REVIEWED|reviewed=24", "FIXED_WINDOWS_REVIEWED", "R952_DRAINED|advanced=2", "CREATED_N0|child=F1"})
            check(ResearchLearningContinuityR912.terminalReviewOutcome(s),"COMPLETED_RECOGNIZED:"+s);
    }
    private static void cache(String bridge) throws Exception {
        CountDb db=new CountDb(bridge);
        try {
            DbStore store=HostProductR974.store(db); HostProductR974.schema(db,store);
            check(ensure(store),"INITIAL_COORDINATOR_ENSURE");
            long before=db.ddl,queries=db.queries,start=System.nanoTime();
            for(int i=0;i<20;i++) check(ensure(store),"REPEATED_ENSURE_"+i);
            long repeated=db.ddl-before;
            System.out.println("MEASURE repeated_transactions=20 ddl="+repeated+" queries="+(db.queries-queries)+" host_elapsed_ms="+((System.nanoTime()-start)/1000000));
            check(repeated==0,"COMMITTED_DDL_NOT_REPLAYED_PER_TRANSACTION");

            db.execSQL("DROP INDEX idx_r952_outbox_state");
            check(!exists(db,"idx_r952_outbox_state"),"EXTERNAL_DROP_OCCURRED");
            before=db.ddl; check(ensure(store)&&exists(db,"idx_r952_outbox_state")&&db.ddl>before,"SCHEMA_VERSION_INVALIDATES_COMMITTED_CACHE");

            check(store.coordinatorForResearchR952().runAtomicWrite("R976_SAME_TX_DROP",new DatabaseCoordinator.WriteRunnable(){
                public void run(SQLiteDatabase d){
                    ResearchMigrationRegistryR952.ensure(d); d.execSQL("DROP INDEX idx_r952_outbox_state");
                    ResearchMigrationRegistryR952.ensure(d);
                    if(!exists(d,"idx_r952_outbox_state"))throw new IllegalStateException("STALE_SAME_TX_SCHEMA");
                }
            }),"SCHEMA_VERSION_INVALIDATES_SAME_TRANSACTION_CACHE");

            db.execSQL("DROP INDEX idx_r952_outbox_state");
            boolean aborted=store.coordinatorForResearchR952().runAtomicWrite("R976_INJECT_ROLLBACK",new DatabaseCoordinator.WriteRunnable(){
                public void run(SQLiteDatabase d){ResearchMigrationRegistryR952.ensure(d);throw new IllegalStateException("HOST_ROLLBACK_AFTER_SCHEMA");}
            });
            check(!aborted&&!exists(db,"idx_r952_outbox_state"),"ROLLBACK_REMOVES_UNCOMMITTED_SCHEMA");
            before=db.ddl;
            check(ensure(store)&&exists(db,"idx_r952_outbox_state")&&db.ddl>before,"ROLLBACK_CANNOT_PUBLISH_READINESS");

            db.execSQL("DROP INDEX idx_r952_outbox_state");
            db.uncertainCommit=true;
            check(!ensure(store),"COMMIT_EXCEPTION_REPORTED");
            before=db.ddl;
            check(ensure(store)&&db.ddl>before,"COMMIT_EXCEPTION_CANNOT_PUBLISH_READINESS");
        } finally { db.shutdown(); }
        // A new connection is not allowed to inherit another database's schema stamp.
        CountDb other=new CountDb(bridge);
        try {
            DbStore store=HostProductR974.store(other);HostProductR974.schema(other,store);
            long before=other.ddl;
            check(ensure(store)&&other.ddl>before,"DATABASE_IDENTITY_ISOLATION");
        } finally { other.shutdown(); }
    }
    public static void main(String[] args)throws Exception {
        pending();cache(args[0]);
        System.out.println("R976_CHAIN_BLOCKERS|checks="+checks+"|failures="+failures+"|scope=PRODUCT_CODE_REAL_SQLITE_HOST_NOT_DEVICE");
        if(failures>0)throw new AssertionError("REGRESSION_FAILURES="+failures);
    }
}
