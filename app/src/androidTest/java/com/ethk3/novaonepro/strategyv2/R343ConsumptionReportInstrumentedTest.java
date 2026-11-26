package com.ethk3.novaonepro.strategyv2;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.ethk3.novaonepro.BuildConfig;
import com.ethk3.novaonepro.Field173NativeEvidence;
import com.ethk3.novaonepro.Field173NativeEvidence.Checks;
import java.io.File;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.junit.runner.RunWith;

/** Isolated synthetic original records through the Android report snapshot and document entry. */
@RunWith(AndroidJUnit4.class)
public final class R343ConsumptionReportInstrumentedTest {
    private static final String SOURCE = BuildConfig.SOURCE_COMMIT_R26;
    private static final String DATASET = hash("R343-038 synthetic SELECT dataset");
    private static final String ABI = FrozenConfirmationR30.currentSemantics();
    private static final String INPUT = hash("isolated original research input");
    private static final String CONSUMER = "R343_038_SYNTHETIC_BIRTH";

    @Test public void r343_038_positive() throws Exception {
        Field173NativeEvidence.run((app, checks) -> {
            File file = fixture(app);
            try {
                try (SQLiteDatabase db = write(file)) {
                    born(db, "actual", 2, Defect.NONE);
                    action(db, "budget", false);
                }
                Map<String,Object> doc = report(file);
                counts(checks, doc, "R01", 1L, 0L, Boolean.TRUE);
                counts(checks, doc, "R02", 0L, 1L, Boolean.TRUE);
                counts(checks, doc, "R03", 0L, 0L, Boolean.FALSE);
                checkNoAuthority(checks, doc);
                evidence(checks, file, doc);
            } finally {
                SQLiteDatabase.deleteDatabase(file);
            }
        });
    }

    @Test public void r343_038_negative() throws Exception {
        Field173NativeEvidence.run((app, checks) -> {
            File file = fixture(app);
            try {
                try (SQLiteDatabase db = write(file)) {
                    born(db, "wrong-origin", 1, Defect.OLD_SOURCE);
                    born(db, "wrong-original", 2, Defect.WRONG_HASH);
                    born(db, "missing-child", 3, Defect.MISSING_CHILD);
                    born(db, "legacy", 4, Defect.UNVALIDATED);
                    born(db, "wrong-target", 5, Defect.WRONG_TARGET);
                    born(db, "late-label", 6, Defect.LATE_LABEL);
                    born(db, "wrong-key", 7, Defect.WRONG_KEY);
                    born(db, "oversized", 8, Defect.OVERSIZE);
                    action(db, "invalid-budget-original", true);
                }
                Map<String,Object> zero = report(file);
                Map<String,Object> born = section(zero, "born");
                checks.equal(7L, number(born.get("scannedRows")), "old-source original excluded by query");
                checks.equal(6L, number(born.get("invalidOrUnvalidatedRows")), "six rejected current-source receipts");
                checks.equal(1L, number(born.get("oversizedRows")), "oversized source is tracked as partial");
                checks.equal("PARTIAL_VALIDATION", born.get("status"), "truncated read status");
                checks.equal(null, born.get("exactValidatedReceiptsByView"), "unavailable full denominator");
                counts(checks, zero, "R01", null, 0L, null);
                counts(checks, zero, "R02", null, 0L, null);
                Map<String,Object> row = chart(zero, "R01");
                checks.equal(0L, evidenceCount(row, "bornLowerBound"), "partial zero lower bound");
                checks.equal(1L, number(section(zero, "nonBorn").get("invalidOrUnvalidatedRows")), "bad budget original rejected");

                try (SQLiteDatabase db = write(file)) { born(db, "actual-with-partial", 9, Defect.NONE); }
                Map<String,Object> partial = report(file);
                counts(checks, partial, "R01", null, 0L, Boolean.TRUE);
                checks.equal(1L, evidenceCount(chart(partial, "R01"), "bornLowerBound"), "one proved receipt in partial read");
                checks.equal(Boolean.FALSE, section(partial, "born").get("complete"), "positive partial read stays incomplete");
                checkNoAuthority(checks, partial);
                checks.record("beforeExactBorn", section(zero, "born").get("exactValidatedReceiptsByView"));
                checks.record("beforePartialLowerBound", evidenceCount(row, "bornLowerBound"));
                evidence(checks, file, partial);
            } finally {
                SQLiteDatabase.deleteDatabase(file);
            }
        });
    }

    @Test public void r343_038_preservation() throws Exception {
        Field173NativeEvidence.run((app, checks) -> {
            File file = fixture(app);
            try {
                try (SQLiteDatabase db = write(file)) {
                    born(db, "retained-after-retirement", 2, Defect.NONE);
                    action(db, "budget", false);
                    db.execSQL("UPDATE r27_policy SET state='RETIRED' WHERE birth_generation=1");
                }
                String before = fileHash(file);
                Map<String,Object> first = report(file);
                Map<String,Object> reopened = report(file);
                counts(checks, reopened, "R01", 1L, 0L, Boolean.TRUE);
                counts(checks, reopened, "R02", 0L, 1L, Boolean.TRUE);
                checks.equal(section(first, "born"), section(reopened, "born"), "retained BORN original readback after reopening");
                checks.equal(section(first, "nonBorn"), section(reopened, "nonBorn"), "unchanged budget readback after reopening");
                checks.equal(before, fileHash(file), "read-only report left original SQLite bytes intact");
                checkNoAuthority(checks, reopened);
                checks.record("originalDatabaseBeforeReportsSha256", before);
                evidence(checks, file, reopened);
            } finally {
                SQLiteDatabase.deleteDatabase(file);
            }
        });
    }

    private enum Defect { NONE, OLD_SOURCE, WRONG_HASH, MISSING_CHILD, UNVALIDATED, WRONG_TARGET, LATE_LABEL, WRONG_KEY, OVERSIZE }

    private static File fixture(Context app) throws Exception {
        File file = File.createTempFile("r343-report-consumption-", ".sqlite", app.getCacheDir());
        try (SQLiteDatabase db = SQLiteDatabase.openOrCreateDatabase(file, null)) {
            db.setForeignKeyConstraintsEnabled(true);
            db.execSQL("CREATE TABLE draws(issue TEXT PRIMARY KEY,dirty INTEGER NOT NULL)");
            for (String ddl : ResearchSchemaR30R1.DDL) db.execSQL(ddl);
            for (String ddl : ResearchActionSchemaR33.DDL) db.execSQL(ddl);
            for (String ddl : SchemaR27.DDL) {
                if (ddl.startsWith("CREATE TABLE IF NOT EXISTS r27_policy(")) db.execSQL(ddl);
            }
        }
        return file;
    }

    private static SQLiteDatabase write(File file) {
        SQLiteDatabase db = SQLiteDatabase.openDatabase(file.getPath(), null,
                SQLiteDatabase.OPEN_READWRITE | SQLiteDatabase.NO_LOCALIZED_COLLATORS);
        db.setForeignKeyConstraintsEnabled(true);
        return db;
    }

    private static Map<String,Object> report(File file) {
        try (SQLiteDatabase db = SQLiteDatabase.openDatabase(file.getPath(), null,
                SQLiteDatabase.OPEN_READONLY | SQLiteDatabase.NO_LOCALIZED_COLLATORS)) {
            ReportR27.R30Input snapshot = ReportSnapshotR30.capture(db);
            return ReportR29.document(ReportR29.fromSnapshotR30(snapshot));
        }
    }

    private static String original(SQLiteDatabase db, String name, String view, String source) {
        String run = hash("run:" + name);
        Map<String,Object> contract = map("source", source, "dataset", DATASET, "revision", 3L,
                "role", "SELECT", "abi", ABI, "input", INPUT, "through", 100L, "available", 1000L);
        String raw = JsonR27.write(map("sourceContract", contract, "viewId", view, "inputHash", INPUT));
        db.execSQL("INSERT INTO r30r1_analysis_run VALUES(?,?,?,?,?,?,?,?,?,?,?,?)",
                new Object[]{run, DATASET, hash("original policy:" + name), "SELECT", source,
                        3, 1, 100, hash(raw), "COMMITTED", raw, 1000L});
        measure(db, run, view, "STRUCTURE_STATE", hash("original policy:" + name), "fixture", "{}");
        return raw;
    }

    private static void born(SQLiteDatabase db, String name, int position, Defect defect) {
        String run = hash("run:" + name);
        String raw = original(db, name, "R01", defect == Defect.OLD_SOURCE ? hash("prior source").substring(0, 40) : SOURCE);
        PolicySpecR27 policy = new PolicySpecR27(PolicyAstR27.parse("(zscore (freq 1 32))"), position,
                PolicySpecR27.Rule.ALWAYS, 0, 1, 1, false, .01, 0,
                new PolicySpecR27.Meta("I04", "FIXTURE", Collections.emptyList(), 1, 0, "Synthetic SELECT"));
        String child = policy.policyHash();
        String proposal = hash("proposal:" + name);
        String receipt = hash(proposal + '|' + child + "|1|" + CONSUMER);
        String proposalJson = JsonR27.write(map("candidateHash", child));
        db.execSQL("INSERT INTO r30r1_proposal VALUES(?,?,?,?,?,?,?,?,?)", new Object[]{proposal, run,
                hash("original policy:" + name), "STRUCTURE_STATE", proposalJson, hash(proposalJson), "CONSUMED", "fixture", 1001L});
        db.execSQL("INSERT INTO r30r1_consume VALUES(?,?,?,?,?,?)",
                new Object[]{proposal, child, 1, CONSUMER, receipt, 1001L});
        if (defect != Defect.MISSING_CHILD) {
            db.execSQL("INSERT INTO r27_policy VALUES(?,?,?,?,?,?,?,?,?,?)", new Object[]{child, policy.fullJson(),
                    "I04", 1, "202611260001", DATASET, "BORN", "fixture", 1001L, 1001L});
        }
        String product = defect == Defect.WRONG_HASH ? hash("substituted original") : hash(raw);
        String key = hash(ResearchResultContractR31.VERSION + '|' + run + '|' + CONSUMER + '|' + child + '|' + product);
        Map<String,Object> receipts = map("source_run", run, "input_hash", INPUT, "role", "SELECT", "abi", ABI,
                "source_revision", 3L, "consumer_version", ResearchConsumptionStoreR31.VERSION + ":" + CONSUMER,
                "before_hash", hash("R31_ABSENT_CANDIDATE_BEFORE_BIRTH|" + child), "after_hash", child,
                "budget", 1L, "product_hash", product);
        Map<String,Object> typed = map("validated", defect != Defect.UNVALIDATED,
                "state", "ACTUAL_BORN_CONSUMPTION_NOT_EFFECTIVENESS", "view", "R01", "receipts", receipts,
                "consumeKey", defect == Defect.WRONG_KEY ? hash("wrong typed key") : key,
                "target", "POSITION:" + (defect == Defect.WRONG_TARGET ? position + 1 : position),
                "datasetHash", DATASET, "decisionOrdinal", defect == Defect.LATE_LABEL ? 100L : 101L, "latestLabelOrdinal", 100L);
        String payload = JsonR27.write(map("scope", "ACTUAL_ORIGINAL_BORN_CHILD_SAME_TRANSACTION_NOT_QUALITY_PROOF",
                "proposalId", proposal, "childPolicyHash", child, "consumer", CONSUMER, "generation", 1L,
                "receiptHash", receipt, "resultContractR31", typed));
        if (defect == Defect.OVERSIZE) {
            char[] tooLong = new char[ReportResearchConsumptionR343.MAX_CELL_CHARS + 1];
            Arrays.fill(tooLong, 'a');
            payload = new String(tooLong);
        }
        measure(db, run, "CONSUMED_BY:" + proposal, "RESEARCH_SOURCE_CONSUMPTION", child, "ORIGINAL_BIRTH_GATE_RECEIPT", payload);
    }

    private static void action(SQLiteDatabase db, String name, boolean wrongProduct) {
        String run = hash("run:" + name);
        String raw = original(db, name, "R02", SOURCE);
        String after = "{\"budget\":2}", beforeHash = hash("{\"budget\":1}"), afterHash = hash(after);
        String key = hash("R33_ACTION1|" + run + "|R02|BUDGET_APPLIED|budget|" + afterHash);
        db.execSQL("INSERT INTO r33_research_applied VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                new Object[]{key, run, "R02", "BUDGET_APPLIED", "SELECT", DATASET, 3, SOURCE, ABI, INPUT,
                        wrongProduct ? hash("wrong budget original") : hash(raw), beforeHash, afterHash,
                        "budget", 1, 101, 100, after, 1001L});
    }

    private static void measure(SQLiteDatabase db, String run, String key, String kind, String parent, String block, String payload) {
        db.execSQL("INSERT INTO r30r1_research_measure VALUES(?,?,?,?,?,?,?,?,?,?,?,?)",
                new Object[]{run, key, kind, parent, block, 1, 100, 0, null, null, null, payload});
    }

    private static void counts(Checks checks, Map<String,Object> doc, String id, Long born, Long nonBorn, Boolean consumed) {
        Map<String,Object> row = chart(doc, id);
        checks.equal(born, number(row.get("actualSourceConsumptionReceipts")), id + " exact BORN");
        checks.equal(nonBorn, number(row.get("actualSourceNonBornActionReceipts")), id + " exact non-BORN");
        checks.equal(consumed, row.get("consumed"), id + " consumed tri-state");
    }

    private static void checkNoAuthority(Checks checks, Map<String,Object> doc) {
        checks.equal(null, chart(doc, "R01").get("effective"), "BORN is not an effect estimate");
        checks.equal(null, chart(doc, "R02").get("effective"), "budget change is not an effect estimate");
        checks.equal(null, section(doc, "born").get("qualityEffective"), "BORN quality remains unmeasured");
        checks.equal(null, section(doc, "nonBorn").get("qualityEffective"), "non-BORN quality remains unmeasured");
        checks.require(!String.valueOf(doc.get("naturalQuality")).startsWith("PASS"), "isolated engineering receipts cannot certify natural quality");
    }

    private static void evidence(Checks checks, File file, Map<String,Object> doc) throws Exception {
        String identity = fileHash(file);
        checks.databaseWithoutTarget("SELECT_SYNTHETIC_ORIGINALS", identity, "Report summary across synthetic targets");
        checks.record("originalDatabaseSha256", identity);
        checks.record("documentSha256", hash(JsonR27.write(doc)));
        checks.record("born", section(doc, "born"));
        checks.record("nonBorn", section(doc, "nonBorn"));
        checks.record("R01", chart(doc, "R01"));
        checks.record("R02", chart(doc, "R02"));
    }

    private static String fileHash(File file) throws Exception { return StatsR27.sha256(Files.readAllBytes(file.toPath())); }
    private static String hash(String value) { return StatsR27.sha256(value); }
    private static Long number(Object value) { return value == null ? null : ((Number) value).longValue(); }
    private static Long evidenceCount(Map<String,Object> row, String key) { return number(object(row.get("consumptionEvidenceR343")).get(key)); }

    private static Map<String,Object> section(Map<String,Object> doc, String kind) {
        Map<String,Object> lifecycle = object(doc.get("lifecycleEvidenceR31"));
        Map<String,Object> receipt = object(lifecycle.get("researchConsumptionR343"));
        return object(object(receipt.get("value")).get(kind));
    }

    private static Map<String,Object> chart(Map<String,Object> doc, String id) {
        Map<String,Object> index = object(doc.get("objectIndex"));
        for (Object row : (List<?>) object(index.get("researchCharts")).get("objects")) {
            Map<String,Object> chart = object(row);
            if (id.equals(chart.get("id"))) return chart;
        }
        throw new AssertionError("Missing research chart " + id);
    }

    @SuppressWarnings("unchecked") private static Map<String,Object> object(Object value) {
        if (!(value instanceof Map)) throw new AssertionError("Expected report map: " + value);
        return (Map<String,Object>) value;
    }

    private static Map<String,Object> map(Object... pairs) {
        Map<String,Object> map = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) map.put((String) pairs[i], pairs[i+1]);
        return map;
    }
}
