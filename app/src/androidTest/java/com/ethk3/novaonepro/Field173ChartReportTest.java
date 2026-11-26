package com.ethk3.novaonepro;

import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.strategyv2.JsonR27;
import com.ethk3.novaonepro.strategyv2.ReportR27;
import com.ethk3.novaonepro.strategyv2.ReportR29;
import com.ethk3.novaonepro.strategyv2.ReportSnapshotR30;
import com.ethk3.novaonepro.strategyv2.ResearchActionSchemaR33;
import com.ethk3.novaonepro.strategyv2.ResearchSchemaR30R1;
import com.ethk3.novaonepro.strategyv2.StatsR27;
import java.io.File;
import java.util.List;
import java.util.Map;
import org.json.JSONObject;
import org.junit.Test;

public final class Field173ChartReportTest {
    private static final String TARGET = "202611260002";
    private static final long CUTOFF = 1795651440000L;
    private static final String OLD_SOURCE = "4d1de9d83692635b5c5ce39144fece1c8a31c8ac";
    private static final List<String> FORMAL = List.of("R11", "R14", "R18");

    @Test public void measuredEmptySource() throws Exception {
        tableCount(true, false);
    }

    @Test public void missingSourceHasNoCount() throws Exception {
        tableCount(false, false);
    }

    @Test public void reportReadKeepsExistingRows() throws Exception {
        tableCount(true, true);
    }

    private static void tableCount(boolean installSource, boolean populateSource) throws Exception {
        Field173NativeEvidence.run((app, checks) -> {
            File file = new File(app.getCacheDir(), "field173-report-table.db");
            try {
                try (SQLiteDatabase write = SQLiteDatabase.openOrCreateDatabase(file, null)) {
                    installIdentity(write);
                    if (installSource) {
                        for (String ddl : ResearchSchemaR30R1.DDL) write.execSQL(ddl);
                        for (String ddl : ResearchActionSchemaR33.DDL) write.execSQL(ddl);
                        write.execSQL("CREATE TABLE r27_policy("
                                + "policy_hash TEXT PRIMARY KEY, spec_json TEXT, dataset_hash TEXT, birth_generation INTEGER)");
                    }
                    if (populateSource) {
                        write.execSQL("CREATE TABLE draws(issue TEXT PRIMARY KEY, dirty INTEGER NOT NULL)");
                        write.execSQL("INSERT INTO draws VALUES(?,0)", new Object[]{"202611260001"});
                        String hash = StatsR27.sha256("WAITING_FOR_EXACT_RESEARCH_INPUT");
                        write.execSQL("INSERT INTO r33_research_input_head VALUES("
                                        + "'R06',?,'WAITING_SOURCE','WAITING_FOR_EXACT_RESEARCH_INPUT',NULL,"
                                        + "'researchStudy',NULL,NULL,?)",
                                new Object[]{hash, CUTOFF});
                    }
                }
                try (SQLiteDatabase read = SQLiteDatabase.openDatabase(file.getPath(), null, SQLiteDatabase.OPEN_READONLY)) {
                    ReportR27.R30Input snapshot = capture(read, checks);
                    Long expected = installSource ? 0L : null;
                    Map<?, ?> tables = (Map<?, ?>) snapshot.watermark.get("tables");
                    Map<?, ?> heads = (Map<?, ?>) tables.get("r33_research_input_head");
                    Map<?, ?> actions = (Map<?, ?>) tables.get("r33_research_applied");
                    checks.equal(installSource, heads.get("available"), "Research head source presence");
                    checks.equal(installSource, actions.get("available"), "Action receipt source presence");
                    checks.equal(populateSource ? Long.valueOf(1L) : expected, heads.get("rows"), "Research head row count");
                    checks.equal(expected, actions.get("rows"), "Action receipt row count");
                    checks.equal(expected, snapshot.facts.get("researchProduced:R06"), "Report current frame count");
                    checks.equal(expected, snapshot.facts.get("researchConsumed:R06"), "Report current action row count");
                    checks.equal(populateSource ? "202611260001" : null, snapshot.watermark.get("latestIssue"),
                            "Latest issue preserves the immutable source");
                    Map<String, Object> document = document(snapshot, checks);
                    Map<?, ?> researchChart = chart(document, "R06");
                    checks.equal(expected, researchChart.get("currentSourceProducedFrames"), "Export current frame count");
                    checks.equal(expected, researchChart.get("actualSourceConsumptionReceipts"), "Export actual BORN receipts");
                    checks.equal(expected, researchChart.get("actualSourceNonBornActionReceipts"), "Export non-BORN receipts");
                    Map<?, ?> evidence = (Map<?, ?>) researchChart.get("researchEvidenceR342");
                    if (populateSource) {
                        Map<?, ?> head = (Map<?, ?>) evidence.get("latestDependencyHead");
                        checks.equal("WAITING_SOURCE", head.get("state"), "Existing waiting reason is retained");
                        checks.equal("WAITING_FOR_EXACT_RESEARCH_INPUT", researchChart.get("notProducedReason"),
                                "Waiting head is distinguished from a produced frame");
                    } else {
                        checks.equal(null, evidence.get("latestDependencyHead"), "No synthetic in-memory registration");
                    }
                    checks.equal(12, document.get("exportedChapters"), "Inherited complete report chapter count");
                    checks.database("SYNTHETIC", TARGET, CUTOFF, RuntimeObservationScopeR341.databaseIdentity(read));
                    checks.record("researchHeads", new JSONObject(heads));
                    checks.record("researchActions", new JSONObject(actions));
                    checks.record("researchChart", new JSONObject(researchChart));
                    checks.record("latestIssue", snapshot.watermark.get("latestIssue"));
                    checks.record("reportFactsSha256", snapshot.watermark.get("factsSha256"));
                    checks.record("exportedChapters", document.get("exportedChapters"));
                }
            } finally {
                SQLiteDatabase.deleteDatabase(file);
            }
        });
    }

    @Test public void currentFrameWithoutOptimizerHead() throws Exception {
        frames("CURRENT");
    }

    @Test public void oldOwnerFrameIsNotCurrent() throws Exception {
        frames("OLD");
    }

    @Test public void formalFramesDoNotClaimOptimizerEffect() throws Exception {
        frames("FORMAL");
    }

    @Test public void retainedOldFrameDoesNotSayCommitted() throws Exception {
        frames("OLD");
    }

    @Test public void unknownFrameCountDoesNotBecomeZero() throws Exception {
        frames("MISSING");
    }

    @Test public void formalReadOnlyDoesNotPassWithoutFiniteValues() throws Exception {
        frames("FORMAL");
    }

    private static void frames(String scenario) throws Exception {
        Field173NativeEvidence.run((app, checks) -> {
            File file = new File(app.getCacheDir(), "field173-research-frames.db");
            try {
                try (SQLiteDatabase write = SQLiteDatabase.openOrCreateDatabase(file, null)) {
                    installIdentity(write);
                    if (!"MISSING".equals(scenario)) {
                        for (String ddl : ResearchSchemaR30R1.DDL) write.execSQL(ddl);
                        for (String ddl : ResearchActionSchemaR33.DDL) write.execSQL(ddl);
                    }
                    if ("CURRENT".equals(scenario)) {
                        saveFrame(write, "R06", BuildConfig.SOURCE_COMMIT_R26);
                    } else if ("OLD".equals(scenario)) {
                        saveFrame(write, "R06", OLD_SOURCE);
                    } else if ("FORMAL".equals(scenario)) {
                        for (String view : FORMAL) {
                            String hash = saveFrame(write, view, BuildConfig.SOURCE_COMMIT_R26);
                            write.execSQL("INSERT INTO r33_research_input_head "
                                            + "(view_id,dependency_key,state,reason,source_run,producer_method,input_hash,output_hash,observed_at)"
                                            + " VALUES(?,?,'FORMAL_READ_ONLY',? ,?,'formalReview',?,?,?)",
                                    new Object[]{view, hash, "INDEPENDENT_FORMAL_SOURCE", "run-" + view, hash, hash, CUTOFF});
                        }
                    }
                }

                try (SQLiteDatabase read = SQLiteDatabase.openDatabase(file.getPath(), null, SQLiteDatabase.OPEN_READONLY)) {
                    ReportR27.R30Input snapshot = capture(read, checks);
                    Map<String, Object> document = document(snapshot, checks);
                    checks.database("SYNTHETIC", TARGET, CUTOFF, RuntimeObservationScopeR341.databaseIdentity(read));
                    List<String> views = "FORMAL".equals(scenario) ? FORMAL : List.of("R06");
                    for (String view : views) {
                        Map<?, ?> chart = chart(document, view);
                        Map<?, ?> layers = (Map<?, ?>) chart.get("lifecycleR31");
                        Map<?, ?> exact = (Map<?, ?>) chart.get("researchEvidenceR342");
                        Long expected = "MISSING".equals(scenario) ? null : "OLD".equals(scenario) ? 0L : 1L;
                        checks.equal(expected, snapshot.facts.get("researchProduced:" + view), view + " source count");
                        checks.equal(expected, chart.get("currentSourceProducedFrames"), view + " report count");
                        boolean current = expected != null && expected > 0;
                        checks.equal(current ? "COMMITTED" : "UNKNOWN", layers.get("ran"), view + " run layer");
                        checks.equal(expected != null && expected == 0 ? "NO" : "UNKNOWN", layers.get("produced"),
                                view + " finite values require separate evidence");
                        checks.equal("NOT_PASS", layers.get("status"), view + " status without measured finite values");
                        if (FORMAL.contains(view)) {
                            Map<?, ?> head = (Map<?, ?>) exact.get("latestDependencyHead");
                            checks.equal("FORMAL_READ_ONLY", head.get("state"), view + " retained formal head");
                            checks.equal("NOT_APPLICABLE", layers.get("consumed"), view + " optimizer is inapplicable");
                            checks.equal("NOT_APPLICABLE", layers.get("effective"), view + " optimizer quality is inapplicable");
                        } else {
                            checks.equal(null, exact.get("latestDependencyHead"), view + " has no dependency head");
                        }
                        if ("OLD".equals(scenario)) {
                            Map<?, ?> original = (Map<?, ?>) chart.get("runtimeReference");
                            checks.equal(frameHash(view), original.get("payload_hash"), view + " old frame reference retained");
                        }
                        checks.record(view, new JSONObject(chart));
                    }
                    checks.record("scenario", scenario);
                    checks.record("reportFactsSha256", snapshot.watermark.get("factsSha256"));
                }
            } finally {
                SQLiteDatabase.deleteDatabase(file);
            }
        });
    }

    private static void installIdentity(SQLiteDatabase write) {
        write.beginTransaction();
        try {
            RuntimeObservationScopeR341.installWithinTransaction(write);
            write.setTransactionSuccessful();
        } finally {
            write.endTransaction();
        }
    }

    private static ReportR27.R30Input capture(SQLiteDatabase read, Field173NativeEvidence.Checks checks) {
        ReportR27.R30Input snapshot = ReportSnapshotR30.capture(read);
        checks.require(snapshot.errors.isEmpty(), "Production snapshot errors: " + snapshot.errors);
        checks.equal(BuildConfig.SOURCE_COMMIT_R26, snapshot.watermark.get("sourceCommit"), "snapshot source");
        checks.require(!read.inTransaction(), "Snapshot releases its read transaction");
        return snapshot;
    }

    private static Map<String, Object> document(ReportR27.R30Input snapshot,
                                                Field173NativeEvidence.Checks checks) {
        Map<String, Object> document = ReportR29.document(ReportR29.fromSnapshotR30(snapshot));
        checks.equal(snapshot.watermark, document.get("snapshotWatermark"), "Report and reader share one watermark");
        return document;
    }

    private static Map<?, ?> chart(Map<String, Object> report, String view) {
        Map<?, ?> index = (Map<?, ?>) report.get("objectIndex");
        Map<?, ?> category = (Map<?, ?>) index.get("researchCharts");
        for (Object object : (List<?>) category.get("objects")) {
            Map<?, ?> entry = (Map<?, ?>) object;
            if (view.equals(entry.get("objectId"))) return entry;
        }
        throw new AssertionError("Missing chart " + view);
    }

    private static String saveFrame(SQLiteDatabase db, String view, String commit) {
        String run = "run-" + view;
        String payload = payload(view);
        String hash = StatsR27.sha256(payload);
        db.execSQL("INSERT INTO r30r1_analysis_run "
                        + "(run_id,dataset_hash,policy_hash,role,source_commit,source_revision,from_ordinal,through_ordinal,"
                        + "contract_hash,status,summary_json,recorded_at) VALUES(?,?,?,'SELECT',?,1,10,25,?,'COMMITTED','{}',?)",
                new Object[]{run, StatsR27.sha256("fixture-dataset"), hash, commit, hash, CUTOFF});
        db.execSQL("INSERT INTO r30r1_chart_cache "
                        + "(view_id,frame_id,run_id,payload_json,payload_hash,updated_at) VALUES(?,?,?,?,?,?)",
                new Object[]{view, "frame-" + view, run, payload, hash, CUTOFF});
        return hash;
    }

    private static String payload(String view) {
        return JsonR27.write(Map.of("viewId", view, "scope", "SYNTHETIC_NO_FINITE_VALUES"));
    }

    private static String frameHash(String view) {
        return StatsR27.sha256(payload(view));
    }
}
