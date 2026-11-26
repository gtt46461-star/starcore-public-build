package com.ethk3.novaonepro.r952;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.ethk3.novaonepro.DatabaseCoordinator;
import com.ethk3.novaonepro.DbStore;
import com.ethk3.novaonepro.Field173NativeEvidence;
import com.ethk3.novaonepro.ResearchEvaluationRunR967;
import com.ethk3.novaonepro.ResearchTaskQueueR967;
import com.ethk3.novaonepro.TargetIssueResolver;

import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;

import static com.ethk3.novaonepro.r952.R343RollingNativeTest.scalar;
import static com.ethk3.novaonepro.r952.R343TrainingNativeTest.TARGET;
import static com.ethk3.novaonepro.r952.R343TrainingNativeTest.addCandidate;
import static com.ethk3.novaonepro.r952.R343TrainingNativeTest.database;
import static com.ethk3.novaonepro.r952.R343TrainingNativeTest.hash;
import static com.ethk3.novaonepro.r952.R343TrainingNativeTest.open;
import static org.junit.Assert.assertThrows;

@RunWith(AndroidJUnit4.class)
public final class R343RetiredRollingOfferInstrumentedTest {
    @Test
    public void retiredParentReleasesUnpublishedRawWithoutRetiringFormalOrQueuedOffers() throws Exception {
        Field173NativeEvidence.run((app, checks) -> {
            DbStore store = open(app);
            try {
                DatabaseCoordinator owner = store.coordinatorForResearchR952();
                SQLiteDatabase db = store.readable();
                String later = TargetIssueResolver.next(TARGET).targetIssue;
                String latest = TargetIssueResolver.next(later).targetIssue;
                String parent = "ROLLING-RETIRED-PARENT-FIXTURE";
                owner.runAtomicWriteOrThrowF01("R343_TEST_RETIRED_OFFER_SETUP", write -> {
                    addCandidate(write, parent, "RESOURCE_RETIRED");
                    addVersion(write, "v01-formal", parent, TARGET);
                    addVersion(write, "v02-orphan", parent, later);
                    addVersion(write, "v03-queued", parent, latest);
                    write.execSQL("INSERT INTO " + RollingLearnerR343.HEAD
                                    + "(candidate_id,version_id,through_issue,model_hash,updated_at) VALUES(?,?,?,?,0)",
                            new Object[]{parent, "v02-orphan", later, hash("model|v02-orphan")});
                    String taskIssue = TargetIssueResolver.next(latest).targetIssue;
                    ResearchTaskQueueR967.enqueue(write, RollingFrozenPublisherR343.KIND, "v03-queued", taskIssue,
                            hash("protocol|v03-queued"), hash("input|v03-queued"), "v03-queued");
                    String frozen = "published-formal-fixture";
                    String model = hash("model|v01-formal");
                    write.execSQL("INSERT INTO " + ResearchEvaluationRunR967.EVAL + " VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,0)",
                            new Object[]{frozen, parent, "fixture-ast", "formal frozen payload", hash(frozen),
                                    model, TARGET, later, "source-fixture", 0, "run-fixture", "rule-fixture", "epoch-fixture"});
                    write.execSQL("INSERT INTO " + RollingFrozenPublisherR343.TABLE
                                    + " VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                            new Object[]{"v01-formal", frozen, "run-fixture", parent, later, hash(parent), "rule-fixture",
                                    300, "source-fixture", "split-fixture", "epoch-fixture", model, "normalizer-fixture",
                                    "manifest-fixture", hash("protocol|v01-formal"), hash("input|v01-formal"),
                                    hash("receipt|v01-formal"), "checkpoint-fixture", "prefix-fixture", "{}", hash("publication-fixture"), 0});
                    return null;
                });

                checks.require(RollingFrozenPublisherR343.pinsRawVersion(db, "v02-orphan"),
                        "Unresolved offer originally pins its working bytes");
                assertThrows(IllegalStateException.class, () ->
                        owner.runAtomicWriteOrThrowF01("R343_TEST_RETIRED_OFFER_ROLLBACK", write -> {
                            checks.require(RollingFrozenPublisherR343.rejectRetiredUnpublishedR343(write, "v02-orphan"),
                                    "Exact retired orphan is recognized");
                            throw new IllegalStateException("R343_TEST_FORCE_ROLLBACK");
                        }));
                checks.equal(0L, scalar(db, "SELECT count(*) FROM " + RollingFrozenPublisherR343.REJECT, null),
                        "An aborted receipt cannot unpin the original");
                checks.require(RollingFrozenPublisherR343.pinsRawVersion(db, "v02-orphan"),
                        "Original stays pinned after rollback");

                for (int page = 0; page < 8; page++) owner.runAtomicWriteOrThrowF01(
                        "R343_TEST_RETIRED_OFFER_CLEANUP", RollingLearnerR343::cleanup);
                checks.equal(1L, scalar(db, "SELECT count(*) FROM " + RollingFrozenPublisherR343.REJECT, null),
                        "Only the never-enqueued and never-published offer is rejected");
                try (Cursor receipt = db.rawQuery("SELECT reason,receipt_json,receipt_hash FROM " + RollingFrozenPublisherR343.REJECT
                        + " WHERE version_id='v02-orphan'", null)) {
                    checks.require(receipt.moveToFirst(), "Rejection receipt persisted");
                    checks.equal("PARENT_RETIRED_UNPUBLISHED", receipt.getString(0), "No training or quality failure fabricated");
                    checks.equal(hash(receipt.getString(1)), receipt.getString(2), "Actual receipt hash read back");
                    JSONObject proof = new JSONObject(receipt.getString(1));
                    checks.equal("RESOURCE_RETIRED", proof.getString("candidateState"), "Original parent state proved");
                    checks.equal(0, proof.getInt("formalFutureCount"), "No inherited future sample");
                    checks.equal(false, proof.getBoolean("realAuthority"), "No REAL authorization");
                    checks.record("rejectionReceipt", receipt.getString(2));
                }
                checks.equal(1L, scalar(db, "SELECT count(*) FROM " + RollingLearnerR343.VERSION
                        + " WHERE version_id='v02-orphan' AND input_blob IS NULL AND checkpoint IS NULL AND model_payload='model-bytes'", null),
                        "Unpublished raw bytes reclaimed and original HEAD model preserved");
                checks.equal(2L, scalar(db, "SELECT count(*) FROM " + RollingLearnerR343.VERSION
                        + " WHERE version_id IN('v01-formal','v03-queued') AND input_blob IS NOT NULL AND checkpoint IS NOT NULL", null),
                        "Formal and in-flight original bytes remain pinned");
                checks.equal(1L, scalar(db, "SELECT count(*) FROM " + ResearchEvaluationRunR967.EVAL
                        + " WHERE candidate_id='published-formal-fixture' AND model_payload='formal frozen payload' AND run_hash='run-fixture'", null),
                        "Earlier frozen run bytes remain intact");
                checks.equal(1L, scalar(db, "SELECT count(*) FROM " + ResearchTaskQueueR967.TABLE
                        + " WHERE authority_ref='v03-queued' AND status='PENDING'", null),
                        "Cleanup cannot replace the original publication owner");
                checks.record("retainedFormalRun", "run-fixture");
                database(checks, db, later);
            } finally {
                store.close();
            }
        });
    }

    private static void addVersion(SQLiteDatabase db, String version, String candidate, String cutoff) {
        String model = hash("model|" + version);
        String receipt = hash("receipt|" + version);
        db.execSQL("INSERT INTO " + RollingLearnerR343.VERSION
                        + "(version_id,candidate_id,predecessor,cutoff_issue,input_first_issue,ast_hash,protocol_hash,"
                        + "protocol_json,input_hash,input_bytes,input_blob,train_n,train_first,train_last,"
                        + "validation_first,validation_last,seed,state,prefix_hash,model_payload,created_at,updated_at,"
                        + "checkpoint,model_hash,receipt_hash)"
                        + " VALUES(?,?,'',?,?,?,?,?,?,?,?,300,?,?,?,?,0,'COMPLETE',?,'model-bytes',0,0,?,?,?)",
                new Object[]{version, candidate, cutoff, cutoff, hash(candidate), hash("protocol|" + version),
                        "{}", hash("input|" + version), 4, new byte[4], cutoff, cutoff, cutoff, cutoff,
                        hash("prefix|" + version), new byte[]{1, 2}, model, receipt});
        db.execSQL("INSERT INTO " + RollingLearnerR343.OFFER
                        + " VALUES(?,?,?,?,?,'NEEDS_INDEPENDENT_PUBLICATION_ADAPTER',0)",
                new Object[]{version, candidate, cutoff, model, receipt});
    }
}
