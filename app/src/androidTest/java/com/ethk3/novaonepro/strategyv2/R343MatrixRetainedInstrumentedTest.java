package com.ethk3.novaonepro.strategyv2;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.ethk3.novaonepro.BuildConfig;
import com.ethk3.novaonepro.Field173NativeEvidence;
import com.ethk3.novaonepro.Field173NativeEvidence.Checks;
import com.ethk3.novaonepro.ResearchEvolutionStoreR930;
import com.ethk3.novaonepro.TargetIssueResolver;
import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;

/** Evidence from production R930 numeric generation and retained originals on synthetic inputs. */
@RunWith(AndroidJUnit4.class)
public final class R343MatrixRetainedInstrumentedTest {
    private static final String COMPATIBILITY = "NUMBER_POSITION_COMPATIBILITY";
    private static final String FLOW = "CROSS_POSITION_FLOW";
    private static final long REVISION = 3L;

    @Test public void r343_031_typed_matrix_readback() throws Exception {
        Field173NativeEvidence.run((app, checks) -> {
            File file = database(app);
            try (EvalContextR27 eval = inputs()) {
                MatrixTensorR30R1.Tensor compatibility = tensor(checks, COMPATIBILITY, eval);
                MatrixTensorR30R1.Tensor flow = tensor(checks, FLOW, eval);
                checks.require(!compatibility.hash.equals(flow.hash), "different matrix kinds retain different originals");
                persist(file, compatibility, flow);
                String firstSha = databaseHash(file);
                persist(file, compatibility, flow);
                try (SQLiteDatabase read = SQLiteDatabase.openDatabase(file.getPath(), null, SQLiteDatabase.OPEN_READONLY)) {
                    readBack(checks, read, compatibility);
                    readBack(checks, read, flow);
                    try (Cursor rows = read.rawQuery("SELECT count(*) FROM research_matrix_snapshot_r930", null)) {
                        checks.require(rows.moveToFirst(), "typed matrix rows counted");
                        checks.equal(2L, rows.getLong(0), "original tensor writes are idempotent on redelivery");
                    }
                }
                checks.database("SELECT_SYNTHETIC_MATRIX", compatibility.targetIssue, compatibility.throughOrdinal, databaseHash(file));
                checks.record("datasetHash", eval.data.datasetHash);
                checks.record("matrixKinds", List.of(compatibility.kind, flow.kind));
                checks.record("matrixHashes", List.of(compatibility.hash, flow.hash));
                checks.record("firstDatabaseSha256", firstSha);
                checks.record("postReplayDatabaseSha256", databaseHash(file));
            } finally {
                SQLiteDatabase.deleteDatabase(file);
            }
        });
    }

    @Test public void r343_031_rejected_matrix_preserves_original() throws Exception {
        Field173NativeEvidence.run((app, checks) -> {
            File file = database(app);
            try (EvalContextR27 eval = inputs()) {
                MatrixTensorR30R1.Tensor prior = tensor(checks, COMPATIBILITY, eval);
                persist(file, prior);
                String before = databaseHash(file);
                MatrixResearchR30R1.OutcomeR31 mismatch = MatrixResearchR30R1.produceR31(COMPATIBILITY, false,
                        eval.data.datasetHash, REVISION, () -> MatrixResearchR30R1.build(FLOW, eval, null, REVISION));
                checks.equal("MATRIX_PRODUCER_KIND_MISMATCH", mismatch.reason, "wrong-type matrix rejected");
                checks.equal(null, mismatch.tensor, "rejected payload cannot replace the original");
                MatrixResearchR30R1.OutcomeR31 missing = MatrixResearchR30R1.produceR31("STUDENT_ERROR", false,
                        eval.data.datasetHash, REVISION, () -> { throw new AssertionError("unavailable student ran"); });
                checks.equal(MatrixResearchR30R1.StatusR31.WAITING_STUDENT, missing.status, "student dependency is local");
                checks.equal(before, databaseHash(file), "rejected or waiting typed product left original bytes intact");
                try (SQLiteDatabase read = SQLiteDatabase.openDatabase(file.getPath(), null, SQLiteDatabase.OPEN_READONLY)) {
                    readBack(checks, read, prior);
                }
                checks.database("SELECT_SYNTHETIC_MATRIX", prior.targetIssue, prior.throughOrdinal, before);
                checks.record("retainedMatrixHash", prior.hash);
                checks.record("rejection", mismatch.reason);
                checks.record("waitStatus", missing.status.name());
            } finally {
                SQLiteDatabase.deleteDatabase(file);
            }
        });
    }

    private static EvalContextR27 inputs() {
        ReplayDatasetR27 sample = ReplayDatasetR27.synthetic(1800, 343031L, new int[0]);
        long start = TargetIssueResolver.sequenceOrdinalSU1("202611220001");
        List<ReplayDatasetR27.Row> rows = new ArrayList<>();
        for (int i = 0; i < sample.size(); i++) {
            long ordinal = start + i;
            rows.add(new ReplayDatasetR27.Row(TargetIssueResolver.fromSequenceOrdinalSU1(ordinal), ordinal, sample.draws[i]));
        }
        ReplayDatasetR27 data = ReplayDatasetR27.of(rows);
        return new EvalContextR27(new FeatureCacheR27(data), SplitR27.of(data, null), null);
    }

    private static File database(Context app) throws Exception {
        File file = File.createTempFile("r343-matrices-", ".sqlite", app.getCacheDir());
        try (SQLiteDatabase db = SQLiteDatabase.openOrCreateDatabase(file, null)) {
            for (String ddl : ResearchSchemaR30R1.DDL) db.execSQL(ddl);
            ResearchEvolutionStoreR930.createSchema(db);
        }
        return file;
    }

    private static void persist(File file, MatrixTensorR30R1.Tensor... tensors) throws Exception {
        try (SQLiteDatabase db = SQLiteDatabase.openDatabase(file.getPath(), null, SQLiteDatabase.OPEN_READWRITE)) {
            ResearchWriteQueueR27.Batch batch = new ResearchWriteQueueR27.Batch();
            for (MatrixTensorR30R1.Tensor tensor : tensors) MatrixTensorR30R1.append(batch, tensor, 1001L);
            db.beginTransaction();
            try {
                ResearchWriteQueueR27.execute(db, batch, "R343_MATRIX_ORIGINAL_TEST");
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        }
    }

    private static MatrixTensorR30R1.Tensor tensor(Checks checks, String kind, EvalContextR27 eval) throws Exception {
        MatrixResearchR30R1.OutcomeR31 outcome = MatrixResearchR30R1.produceR31(kind, false,
                eval.data.datasetHash, REVISION, () -> MatrixResearchR30R1.build(kind, eval, null, REVISION));
        checks.equal(MatrixResearchR30R1.StatusR31.PRODUCED, outcome.status, kind + " produced by original numeric kernel");
        checks.require(outcome.tensor != null && outcome.tensor.presentCells > 0, kind + " has actual supported values");
        checks.require(outcome.tensor.throughOrdinal < TargetIssueResolver.sequenceOrdinalSU1(outcome.tensor.targetIssue), "PIT source predates target");
        return outcome.tensor;
    }

    private static void readBack(Checks checks, SQLiteDatabase db, MatrixTensorR30R1.Tensor tensor) {
        try (Cursor rows = db.rawQuery("SELECT m.payload_json,m.kind,m.role,m.source_revision,m.max_source_issue,"
                + "m.target_issue,m.present_cells,r.dataset_hash,r.source_commit,r.status"
                + " FROM research_matrix_snapshot_r930 m JOIN r30r1_analysis_run r ON r.run_id=m.analysis_run_id"
                + " WHERE m.matrix_hash=?", new String[]{tensor.hash})) {
            checks.require(rows.moveToFirst(), tensor.kind + " durable original exists");
            checks.equal(tensor.hash, StatsR27.sha256(rows.getString(0)), "retained payload hash");
            checks.equal(tensor.kind, rows.getString(1), "original matrix kind");
            checks.equal("SELECT", rows.getString(2), "development role is retained");
            checks.equal(tensor.revision, rows.getLong(3), "source revision is retained");
            checks.equal(tensor.throughIssue, rows.getString(4), "exact input through issue");
            checks.equal(tensor.targetIssue, rows.getString(5), "exact effective issue");
            checks.equal((long) tensor.presentCells, rows.getLong(6), "supported cells remain the original values");
            checks.equal(tensor.datasetHash, rows.getString(7), "report source binds exact dataset");
            checks.equal(BuildConfig.SOURCE_COMMIT_R26, rows.getString(8), "report source binds exact commit");
            checks.equal("COMMITTED", rows.getString(9), "original matrix and source run share a write batch");
            checks.require(!rows.moveToNext(), "typed original is unique");
        }
    }

    private static String databaseHash(File file) throws Exception { return StatsR27.sha256(Files.readAllBytes(file.toPath())); }
}
