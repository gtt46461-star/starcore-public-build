package com.ethk3.novaonepro;

import android.database.Cursor;
import android.system.Os;
import com.ethk3.novaonepro.DatabaseCoordinator.WriteClass;
import com.ethk3.novaonepro.DatabaseCoordinator.WriteContext;
import com.ethk3.novaonepro.Field173NativeEvidence.Checks;
import com.ethk3.novaonepro.strategyv2.CheckpointArtifactsR26;
import com.ethk3.novaonepro.strategyv2.CheckpointClosureR324;
import com.ethk3.novaonepro.strategyv2.ChunkedCheckpointR26;
import com.ethk3.novaonepro.strategyv2.ColdCheckpointR324;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import org.json.JSONObject;
import org.junit.Test;

/** An actual Android CAS split-state load, snapshot closure, and diagnostic readback. */
public final class R343CheckpointContinuityInstrumentedTest {
    private static byte[] fixtureState() {
        byte[] state = new byte[96 * 1024];
        new Random(343017).nextBytes(state);
        return state;
    }

    private static byte[] persistedReference(Checks checks, DbStore store,
            CheckpointArtifactsR26.Prepared prepared) {
        store.coordinatorForResearchR952().runAtomicWriteOrThrowF01(
                WriteContext.of(WriteClass.MIGRATION, "R343_TEST_CHECKPOINT_REFERENCE"), writer -> {
                    writer.execSQL("CREATE TABLE r343_test_checkpoint_reference(reference BLOB NOT NULL)");
                    writer.execSQL("INSERT INTO r343_test_checkpoint_reference VALUES(?)",
                            new Object[]{prepared.reference()});
                    return null;
                });
        try (Cursor row = store.readable().rawQuery("SELECT reference FROM r343_test_checkpoint_reference", null)) {
            checks.require(row.moveToFirst(), "Exact reference committed in the isolated SQLite database");
            byte[] reference = row.getBlob(0);
            checks.require(!row.moveToNext(), "One original reference remains");
            checks.require(Arrays.equals(prepared.reference(), reference), "The reference bytes survive readback");
            return reference;
        }
    }

    private static CheckpointClosureR324.Result strictClosure(File workspace, File root,
            String manifest) throws Exception {
        try (CheckpointClosureR324 out = new CheckpointClosureR324(workspace, root, () -> true,
                (index, maximum) -> {})) {
            return out.traverse(add -> add.accept(manifest), (verified, bytes) -> {});
        }
    }

    private static void bind(Checks checks, DbStore store) throws Exception {
        String db = RuntimeObservationScopeR341.databaseIdentity(store.readable());
        checks.require(!RuntimeObservationScopeR341.UNRESOLVED.equals(db), "Database identity resolves");
        checks.databaseWithoutTarget("ANDROID_ENGINEERING_CHECKPOINT_FIXTURE", db,
                "Exact-byte CAS closure is verified independently of a prediction target or deadline");
    }

    private static void put(File file, byte[] bytes) throws IOException {
        try (FileOutputStream out = new FileOutputStream(file)) {
            out.write(bytes);
            out.getFD().sync();
        }
    }

    @Test public void verifiesExactChunksBeforeProductionModelReadback() throws Exception {
        Field173NativeEvidence.run((app, checks) -> {
            DbStore store = new DbStore(app);
            File root = new File(app.getCacheDir(), "r343-checkpoint");
            CheckpointArtifactsR26 cas = new CheckpointArtifactsR26(root);
            byte[] state = fixtureState();
            try (CheckpointArtifactsR26.Prepared prepared = cas.prepareState(out -> out.write(state))) {
                byte[] reference = persistedReference(checks, store, prepared);
                Set<String> children = ChunkedCheckpointR26.children(cas.readHash(prepared.hash()));
                checks.require(!children.isEmpty(), "Real checkpoint writer published child identities");
                long expectedObjects = (long) children.size() + 1;
                CheckpointClosureR324.Result verified = strictClosure(app.getCacheDir(), root, prepared.hash());
                checks.equal(expectedObjects, verified.objects, "Snapshot closure fully verifies the root and every child");
                try (CheckpointArtifactsR26.DatabaseRootLeaseR31 ignored =
                             CheckpointArtifactsR26.openDatabaseRootR31(store.readable(), root)) {
                    byte[] body = CheckpointArtifactsR26.resolveSerializedR31(store.readable(), reference);
                    checks.require(Arrays.equals(state, body), "The normal checkpoint consumer recovers original bytes");
                }
                StringWriter original = new StringWriter();
                JSONObject audit = CheckpointReportAuditR32.observe(root, Set.of(prepared.hash()), original, () -> true);
                checks.equal(expectedObjects, audit.getLong("verifiedObjects"), "Report traversed the same verified closure");
                checks.require(audit.getBoolean("sourceClosureVerified"), "Verified fixture report closes every edge");
                checks.require(!audit.getBoolean("gcOrModelUseAuthorized"), "A report never grants model or GC authority");
                bind(checks, store);
                checks.record("exactCheckpoint", Map.of("root", prepared.hash(), "plainSha256",
                        CheckpointArtifactsR26.sha(state), "verifiedObjects", verified.objects,
                        "verifiedBytes", verified.bytes, "originalReport", original.toString()));
            } finally {
                store.close();
            }
        });
    }

    @Test public void refusesMissingOrCorruptChildAndPreservesOtherAuditClasses() throws Exception {
        Field173NativeEvidence.run((app, checks) -> {
            DbStore store = new DbStore(app);
            File root = new File(app.getCacheDir(), "r343-checkpoint");
            CheckpointArtifactsR26 cas = new CheckpointArtifactsR26(root);
            byte[] state = fixtureState();
            try (CheckpointArtifactsR26.Prepared prepared = cas.prepareState(out -> out.write(state))) {
                byte[] reference = persistedReference(checks, store, prepared);
                String child = ChunkedCheckpointR26.children(cas.readHash(prepared.hash())).iterator().next();
                File childPath = ColdCheckpointR324.hotFile(root, child);
                byte[] withheld = cas.readHash(child);
                File withheldPath = new File(app.getCacheDir(), "literal-original-withheld.bin");
                checks.require(childPath.renameTo(withheldPath), "Missing case preserves the exact bytes independently");
                try (CheckpointArtifactsR26.DatabaseRootLeaseR31 ignored =
                             CheckpointArtifactsR26.openDatabaseRootR31(store.readable(), root)) {
                    requireBlocked(checks, store, reference, "DEPENDENCY");
                    requireIncompleteClosure(checks, app.getCacheDir(), root, prepared.hash());
                    StringWriter missingRaw = new StringWriter();
                    JSONObject missing = CheckpointReportAuditR32.observe(
                            root, Set.of(prepared.hash()), missingRaw, () -> true);
                    checks.equal(1L, missing.getLong("missingObjects"), "Report separates absent child bytes");
                    checks.require(!missing.getBoolean("sourceClosureVerified"), "Missing child leaves the closure open");

                    byte[] corrupt = withheld.clone();
                    corrupt[0] ^= 0x01;
                    put(childPath, corrupt);
                    requireBlocked(checks, store, reference, "INTEGRITY");
                    requireIncompleteClosure(checks, app.getCacheDir(), root, prepared.hash());
                    String overBudget = "f".repeat(64);
                    try (RandomAccessFile oversized = new RandomAccessFile(
                            ColdCheckpointR324.hotFile(root, overBudget), "rw")) {
                        oversized.setLength((8L << 20) + 1);
                    }
                    String unreadable = "e".repeat(64);
                    Os.symlink(withheldPath.getPath(), new File(root, unreadable + ".bin").getPath());
                    StringWriter mixedRaw = new StringWriter();
                    JSONObject mixed = CheckpointReportAuditR32.observe(
                            root, Set.of(prepared.hash(), overBudget, unreadable), mixedRaw, () -> true);
                    checks.equal(1L, mixed.getLong("corruptObjects"), "Report records a corrupt object independently");
                    checks.equal(1L, mixed.getLong("unreadableObjects"), "Report records an unsafe unreadable source");
                    checks.equal(1L, mixed.getLong("byteVerificationNotRun"), "Report separates exhausted budget from missing");
                    checks.equal(0L, mixed.getLong("missingObjects"), "Unreadable/untested/wrong hash never become missing");
                    checks.require(!mixed.getBoolean("sourceClosureVerified"), "Partial observations make no closure claim");
                    checks.require(!mixed.getBoolean("gcOrModelUseAuthorized"), "Failed audit grants no deletion authority");
                    checks.require(withheldPath.isFile() && childPath.isFile(), "Failure retains source and wrong-byte evidence");

                    File rejectedPath = new File(app.getCacheDir(), "literal-corrupt-child.bin");
                    checks.require(childPath.renameTo(rejectedPath), "Corrupt fixture bytes stay available for reinspection");
                    put(childPath, withheld);
                    byte[] readable = CheckpointArtifactsR26.resolveSerializedR31(store.readable(), reference);
                    checks.require(Arrays.equals(state, readable), "Only the exact original restores this fixture reference");
                    checks.equal(child, CheckpointArtifactsR26.sha(cas.readHash(child)), "Child returns under its old SHA");
                    CheckpointClosureR324.Result restored = strictClosure(app.getCacheDir(), root, prepared.hash());
                    checks.require(restored.objects > 1, "Strict closure reopens only after the exact child is present");
                    bind(checks, store);
                    checks.record("checkpointCases", Map.of("root", prepared.hash(), "child", child,
                            "missingReportOriginal", missingRaw.toString(), "mixedReportOriginal", mixedRaw.toString(),
                            "restoredFixtureBytes", withheld.length, "verifiedFixtureObjects", restored.objects,
                            "retainedWrongByteSha256", CheckpointArtifactsR26.sha(corrupt),
                            "fieldRecoveryReceipt", "NOT_CLAIMED"));
                }
            } finally {
                store.close();
            }
        });
    }

    private static void requireBlocked(Checks checks, DbStore store, byte[] reference, String category) {
        try {
            CheckpointArtifactsR26.resolveSerializedR31(store.readable(), reference);
            throw new AssertionError("Incomplete or altered checkpoint was consumed");
        } catch (CheckpointArtifactsR26.ArtifactReadR31 unavailable) {
            checks.equal(category, unavailable.failureCategory(), "Actual consumer exposes the typed failure");
        }
    }

    private static void requireIncompleteClosure(Checks checks, File workspace, File root,
            String manifest) throws Exception {
        try {
            strictClosure(workspace, root, manifest);
            throw new AssertionError("Incomplete source became a usable checkpoint closure");
        } catch (IOException unavailable) {
            checks.require(!unavailable.getMessage().isEmpty(), "Strict closure preserves the original failure");
        }
    }
}
