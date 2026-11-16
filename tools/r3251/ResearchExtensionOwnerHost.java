package com.ethk3.novaonepro;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/** Native SQLite counterexamples for the original coordinator, not device evidence. */
public final class ResearchExtensionOwnerHost {
    /** Android nests transactions on one connection; only the outermost commits. */
    public static class NestedDatabase extends SQLiteDatabase {
        static final java.util.ArrayList<String> ddl = new java.util.ArrayList<>();
        static String lastUnit = "";
        static boolean failCutoverJournal;
        private final java.util.ArrayDeque<Boolean> transactions = new java.util.ArrayDeque<>();
        private boolean childFailed;
        public NestedDatabase(java.sql.Connection connection) { super(connection); }
        @Override public void execSQL(String sql,Object[] arguments) {
            if(failCutoverJournal&&sql.contains("INTO "+SchemaDomainCoordinatorR926.TABLE_JOURNAL)
                    &&arguments!=null&&arguments.length>2
                    &&String.valueOf(arguments[2]).startsWith("R3251_RESEARCH_ATTACHMENT_CUTOVER_V1:"))
                throw new android.database.sqlite.SQLiteException("INJECTED_ORIGINAL_RESEARCH_CUTOVER_JOURNAL_FAILURE");
            String normalized=sql.trim().replaceAll("\\s+"," ");
            if(normalized.matches("(?is)^(?:CREATE|ALTER|DROP).*"))ddl.add(normalized);
            if(sql.contains("INTO "+SchemaDomainCoordinatorR926.TABLE_JOURNAL)
                    &&arguments!=null&&arguments.length>2)lastUnit=String.valueOf(arguments[2]);
            super.execSQL(sql,arguments);
        }
        @Override public void beginTransaction() {
            if (transactions.isEmpty()) { super.beginTransaction(); childFailed = false; }
            transactions.push(false);
        }
        @Override public void setTransactionSuccessful() {
            if (transactions.isEmpty()) throw new IllegalStateException("NO_TRANSACTION");
            if (transactions.pop()) throw new IllegalStateException("ALREADY_SUCCESSFUL");
            transactions.push(true);
        }
        @Override public void endTransaction() {
            if (transactions.isEmpty()) throw new IllegalStateException("NO_TRANSACTION");
            if (!transactions.pop()) childFailed = true;
            if (transactions.isEmpty()) {
                if (!childFailed) super.setTransactionSuccessful();
                super.endTransaction();
            }
        }
    }
    private static final String UNIT = "R64_EXTENSION_SCHEDULER_9";
    private static final String TABLE = "r974_computation_receipt";
    private static long count(SQLiteDatabase db, String sql) {
        try (Cursor rows = db.rawQuery(sql, null)) {
            if (!rows.moveToFirst()) throw new AssertionError(sql);
            return rows.getLong(0);
        }
    }
    private static boolean advance(Method owner, SQLiteDatabase db) throws Throwable {
        try { return (Boolean) owner.invoke(null, db); }
        catch (InvocationTargetException failed) { throw failed.getCause(); }
    }
    private static String sql(SQLiteDatabase db,String name) {
        try(Cursor rows=db.rawQuery("SELECT sql FROM sqlite_master WHERE name=?",new String[]{name})) {
            return rows.moveToFirst()?rows.getString(0):null;
        }
    }
    private static String criticalSeal(SQLiteDatabase db) {
        try(Cursor rows=db.rawQuery("SELECT fingerprint FROM "+SchemaDomainCoordinatorR926.TABLE_DOMAIN
                +" WHERE domain='CRITICAL'",null)) {
            if(!rows.moveToFirst())throw new AssertionError("CRITICAL_SEAL_ABSENT");return rows.getString(0);
        }
    }
    private static void unsealExtensionFixture(SQLiteDatabase db,String unit) {
        SchemaDomainCoordinatorR926.Contract contract=SchemaDomainCoordinatorR926.researchContract();
        db.execSQL("DELETE FROM "+SchemaDomainCoordinatorR926.TABLE_JOURNAL
                +" WHERE domain=? AND revision=? AND step_id IN (?,?)",
                new Object[]{contract.domain,contract.revision,"R952_EXTENSION",unit});
    }
    private static void resume(Method owner,SQLiteDatabase db)throws Throwable {
        boolean ready=false;for(int i=0;i<8&&!ready;i++)ready=advance(owner,db);
        if(!ready)throw new AssertionError("ORIGINAL_OWNER_FAILED_TO_RESUME:"+ResearchSchemaCoordinatorR926.summary());
    }
    public static void run(SQLiteDatabase db) throws Throwable {
        Method owner = ResearchSchemaCoordinatorR926.class.getDeclaredMethod("advance", SQLiteDatabase.class);
        owner.setAccessible(true);
        String originalGuard=sql(db,"trg_b03_draw_quarantine_no_delete");
        if(originalGuard==null)throw new AssertionError("ORIGINAL_COLD_GUARD_ABSENT");
        boolean ready = false;
        int callbacks = 0;
        for (; callbacks < 1000 && !ready; callbacks++) {
            ready = advance(owner, db);
            if (ResearchSchemaCoordinatorR926.summary().startsWith("FAILED")) {
                System.err.println("ORIGINAL_UNIT="+NestedDatabase.lastUnit);
                for(String sql:NestedDatabase.ddl.subList(Math.max(0,NestedDatabase.ddl.size()-20),NestedDatabase.ddl.size()))
                    System.err.println("ORIGINAL_DDL="+sql.substring(0,Math.min(180,sql.length())));
                throw new AssertionError(ResearchSchemaCoordinatorR926.summary());
            }
        }
        if (!ready) throw new AssertionError("COMPLETE_RESEARCH_OWNER_DID_NOT_CONVERGE");
        SchemaDomainCoordinatorR926.Contract contract = SchemaDomainCoordinatorR926.researchContract();
        if (!SchemaDomainCoordinatorR926.matches(db, contract)) throw new AssertionError("CORE_SEAL_MISSING");
        if (!SchemaDomainCoordinatorR926.stepComplete(db, contract, "R952_EXTENSION"))
            throw new AssertionError("EXTENSION_ROOT_NOT_COMMITTED");
        if (count(db, "SELECT count(*) FROM sqlite_master WHERE name='" + TABLE + "'") != 1)
            throw new AssertionError("ORIGINAL_COMPUTATION_TABLE_MISSING");

        // Disposable native fixture only: reproduce an old core seal with an
        // absent extension unit. Business rows and production migrations are not replaced.
        db.execSQL("DROP TABLE " + TABLE);
        db.execSQL("DELETE FROM " + SchemaDomainCoordinatorR926.TABLE_JOURNAL
                + " WHERE domain=? AND revision=? AND step_id IN (?,?)",
                new Object[]{contract.domain, contract.revision, UNIT, "R952_EXTENSION"});
        if (!SchemaDomainCoordinatorR926.matches(db, contract))
            throw new AssertionError("FIXTURE_CHANGED_CORE_FINGERPRINT");
        if (advance(owner, db)) throw new AssertionError("PREMATURE_READY_WITH_MISSING_EXTENSION");
        if (count(db, "SELECT count(*) FROM sqlite_master WHERE name='" + TABLE + "'") != 1)
            throw new AssertionError("ORIGINAL_EXTENSION_OWNER_NOT_CALLED");
        if (!SchemaDomainCoordinatorR926.stepComplete(db, contract, UNIT))
            throw new AssertionError("ORIGINAL_EXTENSION_CHILD_NOT_COMMITTED");
        if (SchemaDomainCoordinatorR926.stepComplete(db, contract, "R952_EXTENSION"))
            throw new AssertionError("EXTENSION_ROOT_SEALED_BEFORE_FINAL_CALLBACK");
        if (advance(owner, db)) throw new AssertionError("READY_PUBLISHED_INSIDE_ROOT_COMMIT_CALLBACK");
        if (!SchemaDomainCoordinatorR926.stepComplete(db, contract, "R952_EXTENSION"))
            throw new AssertionError("EXTENSION_ROOT_NOT_RESUMED");
        if (!advance(owner, db)) throw new AssertionError("COMMITTED_EXTENSION_NEVER_BECOMES_READY");
        if (!SchemaDomainCoordinatorR926.matches(db, contract)) throw new AssertionError("CORE_SEAL_LOST");

        String sealed=criticalSeal(db);
        db.execSQL("CREATE INDEX r3251_unknown_critical_fixture ON draws(issue)");
        unsealExtensionFixture(db,"NO_REAL_CHILD_REMOVED");
        if(advance(owner,db)||!ResearchSchemaCoordinatorR926.summary().contains("UNTRUSTED_CRITICAL"))
            throw new AssertionError("UNKNOWN_CRITICAL_DRIFT_NOT_REFUSED");
        if(!sealed.equals(criticalSeal(db)))throw new AssertionError("UNKNOWN_DRIFT_WAS_RESTAMPED");
        db.execSQL("DROP INDEX r3251_unknown_critical_fixture");resume(owner,db);

        String definition=com.ethk3.novaonepro.r952.ResearchMigrationRegistryR952.criticalSourceAttachmentsR3251()[0];
        java.util.regex.Matcher named=java.util.regex.Pattern.compile("^CREATE TRIGGER IF NOT EXISTS ([A-Za-z0-9_]+)\\s").matcher(definition);
        if(!named.find())throw new AssertionError("ORIGINAL_CP8_DECLARATION_MISSING");
        String name=named.group(1),original=sql(db,name);sealed=criticalSeal(db);
        db.execSQL("DROP TRIGGER "+name);
        db.execSQL("CREATE TRIGGER "+name+" AFTER INSERT ON draws BEGIN SELECT 1; END");
        unsealExtensionFixture(db,"NO_REAL_CHILD_REMOVED");
        if(advance(owner,db)||!ResearchSchemaCoordinatorR926.summary().contains("UNKNOWN_TRIGGER_DEFINITION"))
            throw new AssertionError("CHANGED_DECLARED_CP8_TRIGGER_NOT_REFUSED");
        if(!sealed.equals(criticalSeal(db)))throw new AssertionError("CHANGED_CP8_TRIGGER_WAS_RESTAMPED");
        db.execSQL("DROP TRIGGER "+name);db.execSQL(original);resume(owner,db);

        // The original CP4 unit 48 ends with a DROP whose CREATE starts unit
        // 54. Re-execution must commit both statements and retain a valid seal.
        unsealExtensionFixture(db,"R64_CP4_DDL_48");
        if(advance(owner,db)||!SchemaDomainCoordinatorR926.stepComplete(db,contract,"R64_CP4_DDL_48")
                ||!SchemaDomainCoordinatorR926.matches(db,SchemaDomainCoordinatorR926.criticalContract()))
            throw new AssertionError("BOUNDARY_CP4_PAIR_DID_NOT_COMMIT_WITH_VALID_CRITICAL_SEAL");
        resume(owner,db);

        // Reopen one actual CP8 unit with its declared trigger absent. The
        // fixture stamps this exact preimage, then fails the new cutover receipt.
        java.lang.reflect.Field cp8Field=com.ethk3.novaonepro.r952.ResearchMigrationRegistryR952.class.getDeclaredField("CP8_DDL");
        cp8Field.setAccessible(true);String[] cp8=(String[])cp8Field.get(null);int index=-1;
        for(int i=0;i<cp8.length;i++)if(cp8[i].trim().equals(definition)){index=i;break;}
        if(index<0)throw new AssertionError("DECLARED_CP8_UNIT_NOT_FOUND");
        String faultUnit="R64_CP8_DDL_"+(index/6*6);
        db.execSQL("DROP TRIGGER "+name);
        SchemaDomainCoordinatorR926.verifyAndStamp(db,SchemaDomainCoordinatorR926.criticalContract());
        sealed=criticalSeal(db);unsealExtensionFixture(db,faultUnit);
        NestedDatabase.failCutoverJournal=true;
        try {
            if(advance(owner,db)||!ResearchSchemaCoordinatorR926.summary().contains("INJECTED_ORIGINAL_RESEARCH_CUTOVER_JOURNAL_FAILURE"))
                throw new AssertionError("CUTOVER_JOURNAL_FAILURE_NOT_PRESERVED:"+ResearchSchemaCoordinatorR926.summary());
        } finally {NestedDatabase.failCutoverJournal=false;}
        if(sql(db,name)!=null||!sealed.equals(criticalSeal(db)))
            throw new AssertionError("CUTOVER_JOURNAL_FAILURE_DID_NOT_ROLL_BACK_GUARD_AND_SEAL");
        if(SchemaDomainCoordinatorR926.stepComplete(db,contract,faultUnit))
            throw new AssertionError("FAILED_CUTOVER_LEFT_COMPLETED_CHILD");
        resume(owner,db);
        System.out.println("R3251_COMPLETE_RESEARCH_OWNER_NATIVE_PASS callbacks=" + callbacks
                + " incompleteExtensionRefused=true originalUnitResumed=true rootPublishedAfterCommit=true"
                + " unknownCriticalDriftRefused=true changedCp8DefinitionRefused=true"
                + " cutoverJournalFailureRollsBackGuardAndChildAndSeal=true boundaryPairRetryResumes=true"
                + " scope=HOST_NATIVE_NOT_ANDROID_NOT_NATURAL");
    }
}
