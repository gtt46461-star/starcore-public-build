package com.ethk3.novaonepro;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.SystemClock;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public final class ResearchExtensionSnapshotProbe {
    private static final String SOURCES = "'r952_candidate','r952_research_outbox','research_historical_genome_archive_r967','research_exact_probability_f05','r952_candidate_funnel'";

    private static long count(SQLiteDatabase db, String query) {
        try (Cursor row = db.rawQuery(query, null)) {
            if (!row.moveToFirst()) throw new IllegalStateException("Missing aggregate");
            return row.getLong(0);
        }
    }

    public static void main(String[] args) throws Exception {
        String file = args[0];
        int budget = Integer.parseInt(args[1]);
        int maximum = Integer.parseInt(args[2]);
        Method advance = ResearchSchemaCoordinatorR926.class.getDeclaredMethod("advance", SQLiteDatabase.class);
        advance.setAccessible(true);
        long present = 0;
        for (int i = 0; i < maximum && present < 5; i++) {
            try (SQLiteDatabase db = SQLiteDatabase.openDatabase(file, null, SQLiteDatabase.OPEN_READWRITE | SQLiteDatabase.NO_LOCALIZED_COLLATORS)) {
                long started = SystemClock.elapsedRealtime();
                ResearchWorkBudgetR927.Scope scope = ResearchWorkBudgetR927.enter(budget);
                String outcome = "RETURNED";
                try {
                    advance.invoke(null, db);
                } catch (InvocationTargetException e) {
                    Throwable cause=e.getCause();
                    outcome=cause.getClass().getSimpleName()+" "+cause.getMessage();
                    if (i<2 || i%16==0) cause.printStackTrace(System.out);
                } finally {
                    ResearchWorkBudgetR927.leave(scope);
                }
                long elapsed = SystemClock.elapsedRealtime()-started;
                present=count(db, "SELECT count(*) FROM sqlite_master WHERE type='table' AND name IN ("+SOURCES+")");
                long total=count(db,"SELECT count(*) FROM sqlite_master");
                String detail=ResearchSchemaCoordinatorR926.summary();
                System.out.println("SLICE "+i+" sources="+present+" objects="+total+" elapsedMs="+elapsed+" result="+outcome+" state="+detail);
            }
        }
        try (SQLiteDatabase db = SQLiteDatabase.openDatabase(file, null, SQLiteDatabase.OPEN_READONLY | SQLiteDatabase.NO_LOCALIZED_COLLATORS);
             Cursor row=db.rawQuery("PRAGMA quick_check",null)) {
            if (!row.moveToFirst() || !"ok".equals(row.getString(0))) throw new AssertionError("Damaged copy");
        }
        if (present!=5) throw new AssertionError("Required report source tables missing after the observed migration slices");
        System.out.println("ALL_FIVE_REPORT_TABLES_AFTER_ORIGINAL_MIGRATION_REOPEN");
    }
}
