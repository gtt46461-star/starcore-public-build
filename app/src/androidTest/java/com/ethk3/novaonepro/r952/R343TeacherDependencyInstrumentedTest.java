package com.ethk3.novaonepro.r952;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.SystemClock;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.ethk3.novaonepro.DbStore;
import com.ethk3.novaonepro.Field173NativeEvidence;
import com.ethk3.novaonepro.Field173NativeEvidence.Checks;
import com.ethk3.novaonepro.Models;
import com.ethk3.novaonepro.ResearchEvaluationRunR967;
import com.ethk3.novaonepro.ResearchSchemaCoordinatorR926;
import com.ethk3.novaonepro.ResearchTaskQueueR967;
import com.ethk3.novaonepro.RuntimeObservationScopeR341;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.junit.Test;
import org.junit.runner.RunWith;

/** Teacher-dependent families must wait without holding authentic atom-backed foundation creation. */
@RunWith(AndroidJUnit4.class)
public final class R343TeacherDependencyInstrumentedTest {
    @Test public void r343_030_teacher_wait_preserves_foundation() throws Exception {
        Field173NativeEvidence.run((app, checks) -> {
            DbStore store = new DbStore(app);
            try {
                long deadline = System.nanoTime() + 150_000_000_000L;
                while (!ResearchSchemaCoordinatorR926.ensureToReady(store.coordinatorForResearchR952(), 8)) {
                    requireTime(deadline, ResearchSchemaCoordinatorR926.summary());
                    SystemClock.sleep(15L);
                }
                SQLiteDatabase db = store.readable();
                checks.equal(0L, count(db, "SELECT count(*) FROM " + ResearchEvaluationRunR967.EVAL), "no frozen teacher in the cold fixture");
                for (CandidateAstR952.Family family : new CandidateAstR952.Family[]{CandidateAstR952.Family.BANDIT,
                        CandidateAstR952.Family.EXPERT, CandidateAstR952.Family.ENSEMBLE}) {
                    try {
                        ModernFamilyFactoryCP12.prepare(db, family, 0);
                        throw new AssertionError("Missing frozen teacher was treated as available for " + family);
                    } catch (DevelopmentProductionCP12.Wait waiting) {
                        checks.require(waiting.reason.startsWith("WAIT_FROZEN_TEACHER"), family + " retains the original frozen teacher dependency");
                    }
                }
                while (!AtomCatalogImportCP12.completeCP12(db)) {
                    requireTime(deadline, "pinned atom catalog import failed to finish");
                    AtomCatalogImportCP12.drainOneCP12(store, deadline);
                }
                AtomCatalogImportCP12.ImportCursor imported = AtomCatalogImportCP12.readCursorCP12(db);
                checks.equal(AtomCatalogPinnedCP12.SHA256, imported.prefixHash, "verified immutable source catalog identity");
                checks.equal(AtomCatalogPinnedCP12.ROWS, imported.row, "verified catalog row count");

                List<Models.Draw> history = history();
                checks.equal(history.size(), store.saveDraws(history), "legal synthetic draw history committed");
                String outcome = ModernFamilyFactoryCP12.maintain(store, deadline);
                checks.require(outcome.matches("[0-9a-f]{64}"), "non-teacher family commits a real training input: " + outcome);
                try (Cursor rows = db.rawQuery("SELECT e.candidate_id,c.structural_hash,p.input_hash,q.status,c.future_n,c.may_create_real"
                        + " FROM " + ModernFamilyFactoryCP12.CREATION + " e JOIN r952_candidate c ON c.candidate_id=e.candidate_id"
                        + " JOIN research_training_protocol_v2 p ON p.candidate_id=c.candidate_id"
                        + " JOIN " + ResearchTaskQueueR967.TABLE + " q ON q.input_hash=p.input_hash AND q.kind='TRAIN_CANDIDATE'"
                        + " WHERE e.family='FREQUENCY' AND e.state='REGISTERED'", null)) {
                    checks.require(rows.moveToFirst(), "real candidate, protocol and TRAIN task join through persisted identity");
                    checks.require(rows.getString(1).matches("[0-9a-f]{64}"), "verified executable structural identity");
                    checks.require(rows.getString(2).matches("[0-9a-f]{64}"), "exact training input hash");
                    checks.equal(0, rows.getInt(4), "new foundation has no inherited future result");
                    checks.equal(0, rows.getInt(5), "new foundation cannot authorize REAL");
                    checks.record("candidateId", rows.getString(0));
                    checks.record("structuralHash", rows.getString(1));
                    checks.record("trainInputHash", rows.getString(2));
                    checks.record("trainTaskStatus", rows.getString(3));
                    checks.require(!rows.moveToNext(), "one independent foundation original");
                }
                checks.equal(0L, count(db, "SELECT count(*) FROM " + ResearchEvaluationRunR967.EVAL), "foundation creation does not fabricate teacher originals");
                checks.databaseWithoutTarget("SYNTHETIC_FOUNDATION_DEVELOPMENT", RuntimeObservationScopeR341.databaseIdentity(db),
                        "Training input uses retained historical draws instead of a field decision target");
                checks.record("atomCatalogSha256", imported.prefixHash);
                checks.record("registrationOutcome", outcome);
            } finally {
                store.close();
            }
        });
    }

    private static List<Models.Draw> history() {
        List<Models.Draw> rows = new ArrayList<>();
        long now = System.currentTimeMillis();
        for (int i = 1; i <= 520; i++) {
            Models.Draw draw = new Models.Draw();
            draw.issue = String.format(Locale.ROOT, "20261125%04d", i);
            draw.openTime = now - 2000L - (520 - i) * 60_000L;
            draw.fetchTime = draw.openTime + 100L;
            for (int j = 0; j < 10; j++) draw.numbers[j] = (i + j) % 10 + 1;
            rows.add(draw);
        }
        return rows;
    }

    private static void requireTime(long deadline, String detail) {
        if (System.nanoTime() >= deadline) throw new AssertionError(detail);
    }

    private static long count(SQLiteDatabase db, String sql) {
        try (Cursor rows = db.rawQuery(sql, null)) {
            if (!rows.moveToFirst()) throw new AssertionError("No database readback: " + sql);
            return rows.getLong(0);
        }
    }
}
