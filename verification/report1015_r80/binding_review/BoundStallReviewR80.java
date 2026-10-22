package com.ethk3.novaonepro;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.r952.*;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import org.json.JSONObject;

/** Independent stalled-registration regression against the normal product classes.
 * Start from an isolated clone of BoundChainReviewR80's READY checkpoint, before
 * register/train. Every later prediction is the actual original source producer.
 * Draws remain explicitly synthetic authoritative host fixtures. The draw cache
 * uses the real ingress-cap owner; source archival uses real JNI SCROW/CAS.
 * This neither manufactures qualifications nor establishes live Future evidence.
 *
 * Each mode is a separate JVM:
 *   later|register|resume bridge isolated.db files assets result.json archive-root
 * Use the native SQLite transport/classpath used by BoundRetentionReviewR80 and
 * -Djava.library.path=<verified JNI directory>, with the normal owner AST property.
 * The caller owns the READY clone and must not reuse its ordinary chain.db here.
 */
public final class BoundStallReviewR80 {
    private static final int FIRST_LATER = 1404, LAST_LATER = 6404;
    private static final String SCOPE =
            "ACTUAL_PRODUCT_STALLED_BOUND_REGISTRATION_5001_ORIGINAL_TARGETS_SYNTHETIC_HOST";
    private static int checks;

    private static void ok(boolean value, String description) {
        if (!value) throw new AssertionError(description);
        checks++;
        System.out.println("PASS " + description);
    }

    private static long n(SQLiteDatabase db, String sql) { return BoundChainReviewR80.n(db, sql); }
    private static String text(SQLiteDatabase db, String sql) { return BoundChainReviewR80.text(db, sql); }
    private static String issue(int index) { return BoundChainReviewR80.issue(index); }
    private static void tx(SQLiteDatabase db, Runnable work) { BoundChainReviewR80.tx(db, work); }

    /** Length-framed streaming digest of all selected values, including nulls. */
    private static String digest(SQLiteDatabase db, String sql) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (Cursor rows = db.rawQuery(sql, null)) {
            while (rows.moveToNext()) {
                digest.update((byte) 1);
                for (int column = 0; column < rows.getColumnCount(); column++) {
                    String value = rows.getString(column);
                    byte[] bytes = value == null ? new byte[0] : value.getBytes(StandardCharsets.UTF_8);
                    digest.update((value == null ? "N:" : "S" + bytes.length + ":")
                            .getBytes(StandardCharsets.US_ASCII));
                    digest.update(bytes);
                }
                digest.update((byte) 0);
            }
        }
        StringBuilder out = new StringBuilder();
        for (byte value : digest.digest()) out.append(String.format("%02x", value & 255));
        return out.toString();
    }

    private static String prefixSources(SQLiteDatabase db) throws Exception {
        return digest(db, "SELECT * FROM " + BoundAtomLifecycleR80.SOURCE
                + " WHERE target_issue<='" + issue(1403) + "' ORDER BY target_issue,candidate_id");
    }

    private static String prefixInputs(SQLiteDatabase db) throws Exception {
        return digest(db, "SELECT * FROM " + BoundAtomLifecycleR80.INPUT + " ORDER BY target_issue");
    }

    private static String prefixDraws(SQLiteDatabase db) throws Exception {
        return digest(db, "SELECT i.target_issue,d.open_time,d.fetch_time,d.first_seen_at,"
                + "d.draw_hash_version,d.draw_hash_v2,d.n1,d.n2,d.n3,d.n4,d.n5,d.n6,d.n7,d.n8,d.n9,d.n10"
                + " FROM " + BoundAtomLifecycleR80.INPUT + " i JOIN " + DrawDependencyInputCP12.SOURCE
                + " d ON d.issue=i.target_issue ORDER BY i.target_issue");
    }

    private static File baselineFile(File files) { return new File(files, "bound-stall-baseline.json"); }

    private static JSONObject loadBaseline(File files) throws Exception {
        return new JSONObject(Files.readString(baselineFile(files).toPath()));
    }

    private static JSONObject createBaseline(SQLiteDatabase db, File files) throws Exception {
        BoundAtomLifecycleR80.Cohort c = BoundAtomLifecycleR80.activeR80(db);
        ok(c != null && c.state.equals("READY") && c.ready == 421
                        && c.selectionIssue.equals(issue(1403)),
                "isolated checkpoint is READY with the original fixed421 prefix");
        ok(n(db, "SELECT COUNT(*) FROM " + BoundAtomLifecycleR80.SOURCE) == 1442
                        && n(db, "SELECT COUNT(*) FROM " + BoundAtomLifecycleR80.INPUT) == 721,
                "checkpoint contains1442 actual sources and721 original typed inputs");
        ok(n(db, "SELECT COUNT(*) FROM " + BoundAtomLifecycleR80.INPUT + " WHERE available=1") == 421
                        && text(db, "SELECT MIN(target_issue) FROM " + BoundAtomLifecycleR80.INPUT
                        + " WHERE available=1").equals(issue(983)),
                "available original prefix starts at983 and includes421 exact targets");
        ok(n(db, "SELECT COUNT(*) FROM draws WHERE dirty=0") == 1403
                        && text(db, "SELECT MAX(issue) FROM draws WHERE dirty=0").equals(issue(1403)),
                "READY clone has only the original1403 fixture draws");
        JSONObject baseline = new JSONObject()
                .put("cohort", c.hash).put("selection", c.selectionIssue)
                .put("sourceDigest", prefixSources(db)).put("inputDigest", prefixInputs(db))
                .put("drawDigest", prefixDraws(db))
                .put("formalRows", n(db, "SELECT COUNT(*) FROM " + FrozenCandidateAdapterR958.PREDRAW))
                .put("calibrationRows", n(db, "SELECT COUNT(*) FROM " + CandidateCalibrationR932.OBS));
        Files.writeString(baselineFile(files).toPath(), baseline.toString() + "\n");
        return baseline;
    }

    private static void verifyOriginalPrefix(SQLiteDatabase db, JSONObject baseline) throws Exception {
        BoundAtomLifecycleR80.Cohort c = BoundAtomLifecycleR80.readR80(db, baseline.getString("cohort"));
        ok(c != null && c.ready == 421 && c.selectionIssue.equals(baseline.getString("selection")),
                "original ready count and selection boundary remain fixed");
        ok(baseline.getString("sourceDigest").equals(prefixSources(db)),
                "every byte of the fixed original source prefix remains unchanged");
        ok(baseline.getString("inputDigest").equals(prefixInputs(db)),
                "every byte of all721 original typed receipts remains unchanged");
        ok(n(db, "SELECT COUNT(*) FROM " + FrozenCandidateAdapterR958.PREDRAW)
                        == baseline.getLong("formalRows")
                        && n(db, "SELECT COUNT(*) FROM " + CandidateCalibrationR932.OBS)
                        == baseline.getLong("calibrationRows"),
                "later bound role creates no formal or calibration observations");
    }

    private static void verifyEvictedPrefix(SQLiteDatabase db, JSONObject baseline) throws Exception {
        ok(n(db, "SELECT COUNT(*) FROM draws WHERE dirty=0") == 5000,
                "real ingress owner caps hot draw history at5000");
        ok(text(db, "SELECT MIN(issue) FROM draws WHERE dirty=0").equals(issue(1405))
                        && text(db, "SELECT MAX(issue) FROM draws WHERE dirty=0").equals(issue(6404)),
                "hot tail contains exactly1405 through6404 after5001 later targets");
        ok(n(db, "SELECT COUNT(*) FROM " + BoundAtomLifecycleR80.INPUT
                        + " i JOIN draws d ON d.issue=i.target_issue") == 0,
                "no raw registration-prefix draw remains in the hot history");
        ok(n(db, "SELECT COUNT(*) FROM " + BoundAtomLifecycleR80.INPUT + " i JOIN "
                        + DrawDependencyInputCP12.TABLE + " d ON d.issue=i.target_issue") == 721,
                "all721 exact original input draws survive through the existing dependency owner");
        ok(n(db, "SELECT COUNT(*) FROM " + BoundAtomLifecycleR80.INPUT + " i JOIN "
                        + DrawDependencyInputCP12.TABLE
                        + " d ON d.issue=i.target_issue WHERE i.available=1") == 421,
                "all421 exact available prefix draws survive through the existing dependency owner");
        ok(baseline.getString("drawDigest").equals(prefixDraws(db)),
                "all721 retained prefix draws preserve full ten-number values, hashes and original clocks");
        ok(n(db, "SELECT COUNT(*) FROM " + DrawDependencyInputCP12.SOURCE + " d JOIN "
                        + BoundAtomLifecycleR80.INPUT
                        + " i ON i.target_issue=d.issue") == 721,
                "unified dependency source exposes each original prefix draw exactly once");
    }

    private static long eligible(SQLiteDatabase db, String table, String additional) {
        RetentionTableRegistryCP12.Rule rule = RetentionTableRegistryCP12.require(table);
        long cutoffAt = n(db, "SELECT open_time FROM draws WHERE issue='" + issue(1405) + "'");
        try (Cursor rows = db.rawQuery("SELECT COUNT(*) FROM " + table + " WHERE ("
                + rule.eligibleSql + ")" + additional, rule.args(issue(1405), cutoffAt))) {
            if (!rows.moveToFirst()) throw new AssertionError("eligibility count missing");
            return rows.getLong(0);
        }
    }

    private static void verifyEligibleBoundary(SQLiteDatabase db, String state) {
        ok(eligible(db, BoundAtomLifecycleR80.SOURCE, "") == 2,
                state + " permits exactly the two settled post-prefix1404 source originals");
        ok(eligible(db, BoundAtomLifecycleR80.SOURCE,
                        " AND target_issue<='" + issue(1403) + "'") == 0,
                state + " excludes the fixed original source prefix from CAS pruning");
        ok(eligible(db, BoundAtomLifecycleR80.INPUT, "") == 0,
                state + " excludes all original input receipts from CAS pruning");
    }

    private static void verifyRetainedTamperGuard(SQLiteDatabase db, JSONObject baseline) throws Exception {
        boolean rejected = false;
        db.beginTransaction();
        try {
            // Swap two non-selected positions, retaining a valid permutation but
            // changing the full draw hash if this immutable guard were absent.
            db.execSQL("UPDATE " + DrawDependencyInputCP12.TABLE
                    + " SET n2=n3,n3=n2 WHERE issue=?", new Object[]{issue(983)});
        } catch (RuntimeException immutable) {
            rejected = String.valueOf(immutable.getMessage()).contains("CP12_DRAW_DEPENDENCY_IMMUTABLE");
        } finally { db.endTransaction(); }
        ok(rejected, "retained full-draw mutation is rejected by the existing immutable owner guard");
        ok(baseline.getString("drawDigest").equals(prefixDraws(db)),
                "rollback-only negative mutation leaves every retained prefix draw unchanged");
    }

    private static void later(SQLiteDatabase db, DbStore store, File database,
                              File files, File archives) throws Exception {
        JSONObject baseline = createBaseline(db, files);
        long modelMemory = RuntimeMemoryGuardR811.algorithmModelBudget().used();
        long atomMemory = RuntimeMemoryGuardR811.algorithmAtomMatrixBudget().used();
        String bundle = ModelBundleRuntimeR932.current().hash;
        String previous = issue(FIRST_LATER - 1), target = issue(FIRST_LATER);
        for (int index = FIRST_LATER; index <= LAST_LATER; index++) {
            final int drawIndex = index;
            final String drawIssue = target;
            DeferredEvidenceSnapshotR36.Snapshot snapshot = MainlineCaptureR75.snapshot(previous, target, bundle);
            int captured = FrozenCandidateAdapterR958.captureBoundSourcesR80(store, snapshot);
            if (captured != 2) throw new AssertionError("actual later capture " + target + " produced " + captured);
            // Arrival follows prediction. The only label authority fixture is the
            // existing canonical draw + completed settlement owner helper.
            long drawAt = System.currentTimeMillis() + 1;
            Thread.sleep(2);
            tx(db, () -> {
                if (!DataRetentionGovernorR900.reserveTailSlotWithinWriteCP12(db, drawIssue))
                    throw new AssertionError("real draw-ingress capacity denied " + drawIssue);
                BoundChainReviewR80.draw(db, drawIndex, drawAt, true);
            });
            BoundAtomLifecycleR80.Cohort c = BoundAtomLifecycleR80.activeR80(db);
            if (c == null || !c.hash.equals(baseline.getString("cohort")) || !c.state.equals("READY")
                    || c.ready != 421 || !c.selectionIssue.equals(baseline.getString("selection")))
                throw new AssertionError("stalled original cohort changed at " + target);
            if (n(db, "SELECT COUNT(*) FROM draws WHERE dirty=0") > 5000)
                throw new AssertionError("hot draw cap exceeded at " + target);
            if (index % 250 == 0 || index == LAST_LATER)
                System.out.println("BOUND_STALLED_ORIGINAL_PRODUCER target=" + target
                        + " laterTargets=" + (index - FIRST_LATER + 1) + " state=" + c.state
                        + " hotDraws=" + n(db, "SELECT COUNT(*) FROM draws WHERE dirty=0"));
            previous = target;
            target = TargetIssueResolver.next(target).targetIssue;
        }
        ok(n(db, "SELECT COUNT(*) FROM " + BoundAtomLifecycleR80.SOURCE
                        + " WHERE target_issue>'" + issue(1403) + "' AND settled=1 AND proof_error=''") == 10002,
                "5001 later targets produce10002 actual immutable settled source originals");
        ok(RuntimeMemoryGuardR811.algorithmModelBudget().used() == modelMemory
                        && RuntimeMemoryGuardR811.algorithmAtomMatrixBudget().used() == atomMemory,
                "all5001 original captures release model and atom memory leases");
        verifyOriginalPrefix(db, baseline);
        verifyEvictedPrefix(db, baseline);
        verifyRetainedTamperGuard(db, baseline);
        verifyEligibleBoundary(db, "READY");

        // The first normal maintenance turn publishes the actual fixed-boundary
        // proposal; registration intentionally resumes in a fresh JVM below.
        String status = BoundAtomLifecycleR80.maintainR80(store, System.nanoTime() + 10_000_000_000L);
        BoundAtomLifecycleR80.Cohort proposed = BoundAtomLifecycleR80.activeR80(db);
        ok(proposed.state.equals("PROPOSED") && !proposed.child.isEmpty()
                        && "BOUND_FRESH_ACTION_PROPOSED".equals(status),
                "normal owner proposes the original fixed child after the long READY stall");
        verifyEligibleBoundary(db, "PROPOSED");
        db.beginTransaction();
        try {
            ResearchTaskQueueR967.enqueue(db, "TRAIN_CANDIDATE", proposed.child, issue(6405),
                    "R80_STALL_ROLLBACK_ONLY_ACTIVE_TASK_GUARD",
                    BoundChainReviewR80.h("R80_STALL_NEGATIVE_GUARD_INPUT"), "NEGATIVE_GUARD_ONLY");
            ok(eligible(db, BoundAtomLifecycleR80.SOURCE, "") == 0,
                    "explicit unfinished child task protects even eligible later source originals");
        } finally { db.endTransaction(); }
        verifyEligibleBoundary(db, "PROPOSED_AFTER_ROLLBACK");

        RetentionArchiveProtocolCP12.Page page =
                DataRetentionGovernorR900.prepareArchivePageCP12(db, BoundAtomLifecycleR80.SOURCE);
        ok(page != null && page.rows.size() == 2 && page.cutoffIssue.equals(issue(1405)),
                "actual retention owner prepares the two old post-prefix source originals");
        for (RetentionArchiveProtocolCP12.RowVersion row : page.rows)
            ok(text(db, "SELECT target_issue FROM " + BoundAtomLifecycleR80.SOURCE
                            + " WHERE rowid=" + row.rowId).equals(issue(1404)),
                    "prepared source archive member is post-prefix1404");
        RetentionArchiveProtocolCP12.Receipt receipt =
                BoundRetentionReviewR80.archive(db, database, archives, page);
        long before = n(db, "SELECT COUNT(*) FROM " + BoundAtomLifecycleR80.SOURCE);
        ok(BoundRetentionReviewR80.prune(db, receipt, false) == 2
                        && n(db, "SELECT COUNT(*) FROM " + BoundAtomLifecycleR80.SOURCE) == before,
                "verified source prune rolls back both originals and its owner metadata atomically");
        ok(BoundRetentionReviewR80.prune(db, receipt, true) == 2,
                "verified native CAS owner prunes both old later source originals");
        ok(BoundRetentionReviewR80.prune(db, receipt, true) == 0,
                "published source archive replay is idempotent");
        ok(n(db, "SELECT COUNT(*) FROM research_evidence_locator_v2 WHERE archive_id='"
                        + receipt.archiveId + "'") == 2,
                "durable audit locators preserve both removed original source records");
        ok(n(db, "SELECT COUNT(*) FROM " + BoundAtomLifecycleR80.SOURCE
                        + " WHERE target_issue>'" + issue(1403) + "'") == 10000,
                "post-prefix source hot tail is exactly5000 targets for two source parents");
        ok(n(db, "SELECT COUNT(*) FROM research_retention_permit_v2") == 0,
                "source archive leaves no retention permits");
        baseline.put("archiveRequest", page.requestId).put("archiveHash", receipt.archiveHash)
                .put("archiveId", receipt.archiveId);
        Files.writeString(baselineFile(files).toPath(), baseline.toString() + "\n");
        verifyOriginalPrefix(db, baseline);
        verifyEvictedPrefix(db, baseline);
    }

    private static void verifyArchive(SQLiteDatabase db, File archives, JSONObject baseline) throws Exception {
        File file = new File(archives, "retention-replies-cp12/" + baseline.getString("archiveRequest") + ".receipt");
        RetentionArchiveProtocolCP12.Receipt receipt = RetentionArchiveIoCP12.readReceipt(file);
        ok(RetentionArchiveIoCP12.validatePublishedProof(archives, file, receipt).archiveHash
                        .equals(baseline.getString("archiveHash")),
                "fresh JVM verifies durable source archive publication metadata");
        RetentionArchiveProtocolCP12.Receipt verified = RetentionArchiveIoCP12.verifyPreparedFile(
                RetentionArchiveProtocolCP12.casFile(archives, receipt.archiveHash),
                receipt.page, receipt.snapshotHash, () -> true);
        ok(verified.archiveHash.equals(receipt.archiveHash)
                        && verified.manifestHash.equals(receipt.manifestHash),
                "fresh JVM fully rehashes unchanged source archive bytes and original member manifest");
        ok(n(db, "SELECT COUNT(*) FROM research_evidence_locator_v2 WHERE archive_id='"
                        + baseline.getString("archiveId") + "'") == 2,
                "fresh JVM preserves both original source audit locators");
    }

    private static void register(SQLiteDatabase db, DbStore store, File files, File archives) throws Exception {
        JSONObject baseline = loadBaseline(files);
        BoundAtomLifecycleR80.Cohort before = BoundAtomLifecycleR80.activeR80(db);
        ok(before != null && before.state.equals("PROPOSED"),
                "separate JVM resumes the actual stalled fixed proposal");
        verifyArchive(db, archives, baseline);
        verifyOriginalPrefix(db, baseline);
        verifyEvictedPrefix(db, baseline);
        BoundChainReviewR80.register(db, store);
        verifyRegistered(db, baseline);
        ok(eligible(db, BoundAtomLifecycleR80.SOURCE, "") == 0
                        && eligible(db, BoundAtomLifecycleR80.INPUT, "") == 0,
                "the actual unfinished child training task protects its original source and receipt evidence");
        long released = 0;
        while (n(db, "SELECT COUNT(*) FROM " + DrawDependencyInputCP12.TABLE + " d JOIN "
                + BoundAtomLifecycleR80.INPUT + " i ON i.target_issue=d.issue") > 0) {
            int[] removed = new int[1];
            tx(db, () -> removed[0] = DrawDependencyInputCP12.releaseConsumedWithinWrite(db, 32));
            if (removed[0] == 0) throw new AssertionError("registered raw-prefix dependency failed to release");
            released += removed[0];
        }
        ok(released >= 721, "existing dependency owner releases the finite original raw prefix after CAS registration");
        ok(n(db, "SELECT COUNT(*) FROM " + DrawDependencyInputCP12.SOURCE + " d JOIN "
                        + BoundAtomLifecycleR80.INPUT + " i ON i.target_issue=d.issue") == 0,
                "all721 consumed original prefix draws leave the dependency source after publication");
        verifyRegistered(db, baseline);
    }

    private static void verifyRegistered(SQLiteDatabase db, JSONObject baseline) throws Exception {
        BoundAtomLifecycleR80.Cohort c = BoundAtomLifecycleR80.readR80(db, baseline.getString("cohort"));
        ok(c != null && c.state.equals("REGISTERED"),
                "actual child registration succeeds after5001 later original source targets");
        ok(n(db, "SELECT future_n FROM r952_candidate WHERE candidate_id='" + c.child + "'") == 0,
                "late-registered fresh child retains Future0");
        ok(n(db, "SELECT COUNT(*) FROM research_training_registration_row_cp12 WHERE action_id='"
                        + c.action + "'") == 420,
                "registration publishes300 train and120 validation original receipts");
        ok(n(db, "SELECT COUNT(*) FROM draws WHERE dirty=0") <= 5000,
                "registration does not reload old prefix draws into the hot cache");
        verifyOriginalPrefix(db, baseline);
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 7)
            throw new IllegalArgumentException("mode bridge isolated.db files assets result.json archive-root");
        String mode = args[0];
        File database = new File(args[2]), files = new File(args[3]), archives = new File(args[6]);
        files.mkdirs();
        archives.mkdirs();
        RealtimePressureGateR811.resetForTests();
        ReportNativeCP4.loadForDiagnosticProcess("host:diagnostics", "host");
        try (SQLiteDatabase db = new SQLiteDatabase(args[1], database.getPath())) {
            db.setForeignKeyConstraintsEnabled(true);
            DbStore store = BoundChainReviewR80.store(db, files.getPath(), args[4]);
            ok(BoundAtomLifecycleR80.readyR80(db), "exact fresh product bound schema is installed");
            if (mode.equals("later")) later(db, store, database, files, archives);
            else if (mode.equals("register")) register(db, store, files, archives);
            else if (mode.equals("resume")) {
                JSONObject baseline = loadBaseline(files);
                verifyArchive(db, archives, baseline);
                verifyRegistered(db, baseline);
                ok(n(db, "SELECT COUNT(*) FROM " + DrawDependencyInputCP12.TABLE + " d JOIN "
                                + BoundAtomLifecycleR80.INPUT + " i ON i.target_issue=d.issue") == 0,
                        "separate JVM confirms consumed raw-prefix dependencies stay released");
            } else throw new IllegalArgumentException(mode);
            ok(n(db, "SELECT COUNT(*) FROM research_certificate_r929") == 0
                            && n(db, "SELECT COUNT(*) FROM trades") == 0,
                    "stalled registration manufactures no certificate or REAL trade");
            JSONObject result = new JSONObject().put("status", "PASS").put("mode", mode)
                    .put("scope", SCOPE).put("checks", checks).put("chainChecks", BoundChainReviewR80.checks)
                    .put("laterTargets", 5001).put("hotDraws", n(db, "SELECT COUNT(*) FROM draws WHERE dirty=0"))
                    .put("sourceRows", n(db, "SELECT COUNT(*) FROM " + BoundAtomLifecycleR80.SOURCE))
                    .put("typedInputs", n(db, "SELECT COUNT(*) FROM " + BoundAtomLifecycleR80.INPUT))
                    .put("dependencyDraws", n(db, "SELECT COUNT(*) FROM " + DrawDependencyInputCP12.TABLE));
            Files.writeString(Path.of(args[5]), result.toString() + "\n");
            System.out.println(result);
        }
    }
}
