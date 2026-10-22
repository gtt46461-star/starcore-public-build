package com.ethk3.novaonepro;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

/** Compiled against original 8e188bd R980 classes only. Isolated synthetic facts, never a customer database. */
public final class PopulateBaselineR980 {
    public static void main(String[] args) throws Exception {
        SQLiteDatabase db = new SQLiteDatabase(args[0], args[1]);
        try {
            DbStore store = HostProductR974.store(db);
            store.setState("R986_ISOLATED_MIGRATION_SENTINEL", "user-state-must-survive-actual-upgrade");
            for (int i = 1; i <= 3; i++) {
                Models.Draw draw = new Models.Draw();
                draw.issue = String.format(java.util.Locale.ROOT, "20260923%04d", i);
                draw.openTime = java.time.Instant.parse("2026-09-23T00:00:00Z").toEpochMilli() + i * 60000;
                draw.fetchTime = System.currentTimeMillis();
                draw.sourceLine = AppConfig.API_LINE_PRIMARY;
                draw.rawJson = "EXPLICIT_ISOLATED_R980_MIGRATION_FIXTURE";
                draw.numbers = new int[10];
                for (int p = 0; p < 10; p++) draw.numbers[p] = (p + i) % 10 + 1;
                if (store.saveDraw(draw) <= 0) throw new AssertionError("actual R980 draw owner rejected " + draw.issue);
            }
            db.beginTransaction();
            try {
                ResearchMultiplicityR932.registerCandidate(db, "ISOLATED-R986-OLD-RESEARCH", "FAMILY", V51ChecksumEngine.sha256("ISOLATED_R980_STRUCTURE"), "202609230001");
                ResearchMultiplicityR932.registerLook(db, "ISOLATED-R986-OLD-RESEARCH", "W1", 1, "202609230002", "202609230121");
                ResearchTaskQueueR967.enqueue(db, "UNIT", "ISOLATED-R986-OLD-RESEARCH", "202609230001", "SEQ", V51ChecksumEngine.sha256("ISOLATED_R980_INPUT"), "EXPLICIT_SYNTHETIC_UPGRADE_RECEIPT");
                db.setTransactionSuccessful();
            } finally { db.endTransaction(); }
            try (Cursor c = db.rawQuery("SELECT COUNT(*) FROM draws WHERE issue BETWEEN '202609230001' AND '202609230003'", null)) {
                if (!c.moveToFirst() || c.getInt(0) != 3) throw new AssertionError("draw facts");
            }
            if (!"user-state-must-survive-actual-upgrade".equals(store.getState("R986_ISOLATED_MIGRATION_SENTINEL", ""))) throw new AssertionError("old owner state");
            System.out.println("PASS ORIGINAL_R980_PRODUCT_OWNERS_POPULATED_SYNTHETIC_DB draws=3 originalCommit=8e188bd4726ad205f971428876967de5f89fd633");
        } finally { db.shutdown(); }
    }
}
