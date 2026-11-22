package com.ethk3.novaonepro;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.SystemClock;
import java.io.File;

/** Execute with app_process on an isolated database, including closes between callbacks. */
public final class ExtensionInspectionRollbackTest {
    private static final String GUARD = "CREATE TRIGGER IF NOT EXISTS cp8_test_extension BEFORE INSERT ON draws BEGIN SELECT 1; END";
    private static final String[] DECLARATIONS = {GUARD};
    private static final String[] REPLACEMENTS = {};
    private final String path;

    ExtensionInspectionRollbackTest(String path) { this.path = path; }

    private SQLiteDatabase open() {
        return SQLiteDatabase.openDatabase(path, null, SQLiteDatabase.OPEN_READWRITE
                | SQLiteDatabase.CREATE_IF_NECESSARY | SQLiteDatabase.NO_LOCALIZED_COLLATORS);
    }

    private static void require(boolean value, String reason) {
        if (!value) throw new AssertionError(reason);
    }

    private boolean advance(SchemaDomainCoordinatorR926.Step child) {
        try (SQLiteDatabase db = open()) {
            db.beginTransaction();
            try {
                boolean ran = SchemaDomainCoordinatorR926.prepareExtensionInspection(db, DECLARATIONS, REPLACEMENTS);
                if (ran) SchemaDomainCoordinatorR926.installInspectedAttachments(db, DECLARATIONS, child);
                db.setTransactionSuccessful();
                return ran;
            } finally { db.endTransaction(); }
        }
    }

    private void prepare() {
        require(!advance(db -> { throw new AssertionError("Unprepared child ran"); }), "Expected inspection");
    }

    private static void writeReceipt(SQLiteDatabase db) {
        db.execSQL("INSERT INTO observed VALUES (1)");
    }

    private long scalar(String sql) {
        try (SQLiteDatabase db = open(); Cursor value = db.rawQuery(sql, null)) {
            require(value.moveToFirst(), "Missing result");
            return value.getLong(0);
        }
    }

    private void expectRefusal(Class<? extends RuntimeException> expected, SchemaDomainCoordinatorR926.Step child) {
        try {
            advance(child);
            throw new AssertionError("Expected " + expected.getSimpleName());
        } catch (RuntimeException failure) {
            if (!expected.isInstance(failure)) throw failure;
        }
    }

    private void run() {
        try (SQLiteDatabase db = open()) {
            SchemaDomainCoordinatorR926.ensureInfrastructure(db);
            db.execSQL("CREATE TABLE draws (id INTEGER PRIMARY KEY)");
            db.execSQL("CREATE TABLE observed (id INTEGER)");
            db.execSQL("CREATE TABLE main.r335_extension_catalog (original_value INTEGER)");
            db.execSQL("INSERT INTO main.r335_extension_catalog VALUES (721)");
            db.execSQL(GUARD);
        }
        prepare();
        final long version = scalar("PRAGMA schema_version");
        try (SQLiteDatabase db = open()) {
            db.execSQL("CREATE VIEW unrelated AS SELECT id FROM draws");
            db.execSQL("PRAGMA schema_version=" + version);
        }
        expectRefusal(SchemaDomainCoordinatorR926.ChangedExtensionPreimage.class, ExtensionInspectionRollbackTest::writeReceipt);
        require(scalar("SELECT count(*) FROM observed") == 0, "Stale catalogue published a receipt");
        require(scalar("SELECT count(*) FROM sqlite_master WHERE name='unrelated'") == 1, "Original view changed");
        prepare();
        try (SQLiteDatabase db = open()) {
            db.execSQL("INSERT INTO " + SchemaDomainCoordinatorR926.TABLE_JOURNAL
                    + "(domain,revision,step_id,status,attempt,cursor,started_at,completed_at)"
                    + " VALUES(?,?,'test','RUNNING',1,'',1,0)",
                    new Object[]{"CRITICAL", SchemaDomainCoordinatorR926.criticalContract().revision});
        }
        expectRefusal(SchemaDomainCoordinatorR926.ChangedExtensionPreimage.class, ExtensionInspectionRollbackTest::writeReceipt);
        require(scalar("SELECT count(*) FROM observed") == 0, "Stale journal published a receipt");
        prepare();
        ResearchWorkBudgetR927.Scope budget = ResearchWorkBudgetR927.enter(1);
        try {
            SystemClock.sleep(20);
            expectRefusal(ResearchWorkBudgetR927.Yield.class, db -> {
                ResearchWorkBudgetR927.checkpoint();
                writeReceipt(db);
            });
        } finally { ResearchWorkBudgetR927.leave(budget); }
        require(scalar("SELECT count(*) FROM observed") == 0, "Expired admission wrote a receipt");
        expectRefusal(DatabaseCoordinator.SchemaPendingR31.class, db -> {
            writeReceipt(db);
            db.execSQL("DROP TRIGGER cp8_test_extension");
            db.execSQL(GUARD.replace("SELECT 1", "SELECT 2"));
        });
        require(scalar("SELECT count(*) FROM observed") == 0, "A failed definition check committed");
        require(advance(ExtensionInspectionRollbackTest::writeReceipt), "Verified child failed to commit");
        require(scalar("SELECT count(*) FROM observed") == 1, "Reopen lost verified child");
        require(scalar("SELECT original_value FROM main.r335_extension_catalog") == 721,
                "Temporary storage changed the original owner");
        try (SQLiteDatabase db = open()) {
            // Exercise the reused writer connection as well as closes between callback attempts.
            for (int pass = 0; pass < 2; pass++) {
                db.beginTransaction();
                try {
                    if (SchemaDomainCoordinatorR926.prepareExtensionInspection(db, DECLARATIONS, REPLACEMENTS))
                        SchemaDomainCoordinatorR926.installInspectedAttachments(db, DECLARATIONS, value -> {});
                    db.setTransactionSuccessful();
                } finally { db.endTransaction(); }
            }
            for (String table : new String[]{"r335_extension_catalog", "r335_extension_seal", "r335_extension_journal"}) {
                try (Cursor row = db.rawQuery("SELECT count(*) FROM temp." + table, null)) {
                    require(row.moveToFirst() && row.getLong(0) == 0, "Temporary rows retained in " + table);
                }
            }
            db.execSQL("DROP TRIGGER cp8_test_extension");
            db.execSQL(GUARD.replace("SELECT 1", "SELECT 3"));
        }
        expectRefusal(DatabaseCoordinator.SchemaPendingR31.class, ExtensionInspectionRollbackTest::writeReceipt);
        require(scalar("SELECT count(*) FROM observed") == 1, "Preexisting drift was published");
        System.out.println("EXTENSION_CUSTODY_PASS catalogue_drift=rollback critical_journal_drift=rollback"
                + " elapsed_admission=yield unknown_before=refused unknown_after=rollback verified_child=1 reopened=true temp_rows=0 original_owner=721");
    }

    public static void main(String[] args) {
        if (args.length != 1 || new File(args[0]).exists()) throw new IllegalArgumentException("New test path required");
        new ExtensionInspectionRollbackTest(args[0]).run();
    }
}
