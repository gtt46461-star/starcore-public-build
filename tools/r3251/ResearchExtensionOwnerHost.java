package com.ethk3.novaonepro;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/** Native SQLite counterexamples for the original coordinator, not device evidence. */
public final class ResearchExtensionOwnerHost {
    private static final String UNIT = "R64_EXTENSION_SCHEDULER_6";
    private static final String TABLE = "v57_research_task_r967";
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
    public static void run(SQLiteDatabase db) throws Throwable {
        Method owner = ResearchSchemaCoordinatorR926.class.getDeclaredMethod("advance", SQLiteDatabase.class);
        owner.setAccessible(true);
        boolean ready = false;
        int callbacks = 0;
        for (; callbacks < 1000 && !ready; callbacks++) {
            ready = advance(owner, db);
            if (ResearchSchemaCoordinatorR926.summary().startsWith("FAILED"))
                throw new AssertionError(ResearchSchemaCoordinatorR926.summary());
        }
        if (!ready) throw new AssertionError("COMPLETE_RESEARCH_OWNER_DID_NOT_CONVERGE");
        SchemaDomainCoordinatorR926.Contract contract = SchemaDomainCoordinatorR926.researchContract();
        if (!SchemaDomainCoordinatorR926.matches(db, contract)) throw new AssertionError("CORE_SEAL_MISSING");
        if (!SchemaDomainCoordinatorR926.stepComplete(db, contract, "R952_EXTENSION"))
            throw new AssertionError("EXTENSION_ROOT_NOT_COMMITTED");
        if (count(db, "SELECT count(*) FROM sqlite_master WHERE name='" + TABLE + "'") != 1)
            throw new AssertionError("ORIGINAL_SCHEDULER_TABLE_MISSING");

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
        System.out.println("R3251_COMPLETE_RESEARCH_OWNER_NATIVE_PASS callbacks=" + callbacks
                + " incompleteExtensionRefused=true originalUnitResumed=true rootPublishedAfterCommit=true"
                + " scope=HOST_NATIVE_NOT_ANDROID_NOT_NATURAL");
    }
}
