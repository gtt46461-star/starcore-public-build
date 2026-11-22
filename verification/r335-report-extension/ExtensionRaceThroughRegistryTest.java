package com.ethk3.novaonepro;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/** A registered migration is prepared, an unrelated owner commits DDL, then the original owner resumes. */
public final class ExtensionRaceThroughRegistryTest {
    private static long scalar(SQLiteDatabase db, String sql) {
        try (Cursor row = db.rawQuery(sql, null)) {
            if (!row.moveToFirst()) throw new AssertionError("Missing result");
            return row.getLong(0);
        }
    }

    private static boolean advance(SQLiteDatabase db, Method method) throws Exception {
        try {
            return (Boolean) method.invoke(null, db, SchemaDomainCoordinatorR926.researchContract(), false);
        } catch (InvocationTargetException wrapped) {
            if (wrapped.getCause() instanceof Exception) throw (Exception) wrapped.getCause();
            throw wrapped;
        }
    }

    public static void main(String[] args) {
        try {
            verify(args[0]);
        } catch (Throwable failure) {
            failure.printStackTrace(System.out);
            System.exit(1);
        }
    }

    private static void verify(String file) throws Exception {
        Method method = ResearchSchemaCoordinatorR926.class.getDeclaredMethod("advanceExtensionOneStep",
                SQLiteDatabase.class, SchemaDomainCoordinatorR926.Contract.class, boolean.class);
        method.setAccessible(true);
        String journal = "SELECT COUNT(*) FROM " + SchemaDomainCoordinatorR926.TABLE_JOURNAL
                + " WHERE domain='RESEARCH' AND status='COMPLETED'";
        String changed = "SELECT COUNT(*) FROM sqlite_master WHERE type='view' AND name='qa_independent_schema_change'";
        long before;
        try (SQLiteDatabase db = SQLiteDatabase.openDatabase(file, null, SQLiteDatabase.OPEN_READWRITE)) {
            before = scalar(db, journal);
            if (advance(db,method)) throw new AssertionError("Preparation incorrectly marked migration complete");
        }
        try (SQLiteDatabase secondOwner = SQLiteDatabase.openDatabase(file, null, SQLiteDatabase.OPEN_READWRITE)) {
            secondOwner.execSQL("CREATE VIEW qa_independent_schema_change AS SELECT 1 AS original");
        }
        Exception failure = null;
        try (SQLiteDatabase db = SQLiteDatabase.openDatabase(file, null, SQLiteDatabase.OPEN_READWRITE)) {
            try {
                if (advance(db,method)) throw new AssertionError("Stale inspection was accepted");
            } catch (Exception unexpected) {
                failure = unexpected;
            }
            if (scalar(db, changed) != 1 || scalar(db, journal) != before)
                throw new AssertionError("Refused write changed an original or published a child");
        }
        if (failure != null) throw new AssertionError("A changed inspection must yield to the next registered slice", failure);
        for (int i=0; i<8; i++) {
            try (SQLiteDatabase db = SQLiteDatabase.openDatabase(file, null, SQLiteDatabase.OPEN_READWRITE)) {
                advance(db,method);
                if (scalar(db,journal)>before && scalar(db,changed)==1) {
                    System.out.println("EXTENSION_REGISTRY_RACE_RECOVERED");
                    return;
                }
            }
        }
        throw new AssertionError("Fresh inspection did not resume the same migration");
    }
}
