package com.ethk3.novaonepro;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.os.CancellationSignal;
import com.ethk3.novaonepro.strategyv2.CheckMetricCrosswalkR31;
import com.ethk3.novaonepro.strategyv2.ReportAssessmentR33;
import org.json.JSONObject;
import org.junit.Test;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Report readers and acceptance boundaries on the exact B12 candidate. */
public final class Field173B12ReportTest {
    @Test public void reportPositive() throws Exception { run("POS"); }
    @Test public void reportNegative() throws Exception { run("NEG"); }
    @Test public void reportPreservation() throws Exception { run("KEEP"); }

    private static String scopeText(int length) {
        StringBuilder value=new StringBuilder(length);
        for(int i=0;i<length;i++)value.append("字段/\u2605".charAt(i%4));
        return value.toString()+"尾";
    }

    private void run(String mode) throws Exception {
        Field173NativeEvidence.run((app, checks) -> {
            checks.require(java.util.Arrays.equals(CompleteReportFileR927.deliveryActions(true,false),
                    new String[]{"一次保存完整ZIP文件","分享完整ZIP","保存到文件夹"}),"Published compact single-ZIP delivery actions");
            checks.equal(96, CheckMetricCrosswalkR31.checks().size(), "Registered check denominator");
            checks.equal(44, CheckMetricCrosswalkR31.definitions().size(), "Registered metric denominator");
            Map<String,Object> rows = new LinkedHashMap<>();
            for (CheckMetricCrosswalkR31.Check definition : CheckMetricCrosswalkR31.checks()) {
                rows.put(definition.id, Map.of("execution_state", "EXECUTED",
                        "domain_outcome", "OBSERVED_EMPTY", "health", "UNKNOWN"));
            }
            Map<String,Object> checksOnly = ReportSemanticVerifierR31.assessChecksR342(rows);
            checks.equal("INDEPENDENT_METRIC_AND_FIELD_RECEIPTS_REQUIRED", checksOnly.get("businessAcceptance"),
                    "Executed checks remain distinct from metrics and field acceptance");

            Map<String,Object> unobserved = ReportAssessmentR33.evaluate(List.of(), List.of());
            checks.equal("INCOMPLETE", unobserved.get("overallAcceptance"), "Missing originals remain visible");
            checks.equal(List.of(), ((Map<?,?>) unobserved.get("naturalQuality")).get("verifiedMetricIds"),
                    "Synthetic and missing samples leave natural evidence pending");

            File image = new File(app.getCacheDir(), "field173-report-source.sqlite");
            try (SQLiteDatabase db = SQLiteDatabase.openOrCreateDatabase(image, null)) {
                db.beginTransaction();
                try {
                    RuntimeObservationScopeR341.installWithinTransaction(db);
                    db.execSQL("CREATE TABLE report_payload(key TEXT PRIMARY KEY, value INTEGER NOT NULL)");
                    db.execSQL("INSERT INTO report_payload VALUES('expected', 37)");
                    db.execSQL("CREATE TABLE scope_cells(id INTEGER PRIMARY KEY,status TEXT COLLATE NOCASE,value,body BLOB)");
                    for(int i=1;i<=5;i++)db.execSQL("INSERT INTO scope_cells VALUES(?,?,?,zeroblob(913))",
                            new Object[]{i,i==1?"SYNC":"sync",i==1?null:i==2?"":scopeText(i==3?511:i==4?4097:32771)});
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
            }
            String hash = ReportArchiveCP4.hash(image, () -> true);
            try (SQLiteDatabase snapshot = SQLiteDatabase.openDatabase(image.getPath(), null, SQLiteDatabase.OPEN_READONLY)) {
                try(android.database.Cursor columns=snapshot.rawQuery("PRAGMA table_info(scope_cells)",null)) {
                    checks.require(columns.moveToFirst(),"Native schema row available");
                    ReportDiagnosticEvidenceR64.Column id=new ReportDiagnosticEvidenceR64.Column(columns);
                    checks.require("id".equals(id.name) && "INTEGER".equals(id.type) && id.primaryKeyOrder==1,"Compact catalog preserves type and key order");
                    checks.require(columns.moveToNext(),"Nullable state schema available");
                    ReportDiagnosticEvidenceR64.Column status=new ReportDiagnosticEvidenceR64.Column(columns);
                    checks.require("status".equals(status.name) && status.notNull==0 && status.primaryKeyOrder==0,"Compact catalog preserves nullability");
                }
                java.io.StringWriter cells=new java.io.StringWriter();
                long count=ReportMetadataStreamR81.writeRows(snapshot,"scope_cells",java.util.Arrays.asList("id","value","body"),cells,()->true);
                org.json.JSONArray evidence=new org.json.JSONArray("["+cells+"]");
                checks.equal(5L,count,"Row count through inline and streaming paths");
                checks.require(evidence.getJSONObject(0).isNull("value") && evidence.getJSONObject(1).getString("value").isEmpty(),"Null and empty string remain distinct");
                for(int i=2;i<5;i++)checks.equal(scopeText(i==2?511:i==3?4097:32771),evidence.getJSONObject(i).getString("value"),"Exact mixed-width text row "+i);
                checks.equal("[BINARY_PAYLOAD_NOT_INCLUDED bytes=913]",evidence.getJSONObject(4).getString("body"),"Binary descriptor");
                java.io.StringWriter groups=new java.io.StringWriter();
                ReportMetadataStreamR81.writeGroups(snapshot,"scope_cells","status",groups,()->true,false);
                org.json.JSONArray grouped=new org.json.JSONArray("["+groups+"]");
                checks.require(grouped.length()==1 && grouped.getJSONObject(0).getLong("rows")==5 && "SYNC".equals(grouped.getJSONObject(0).getString("value")),"Exact grouped state and binary-minimum representative");
                String database = RuntimeObservationScopeR341.databaseIdentity(snapshot);
                checks.require(database.matches("[a-f0-9-]{36}"), "Actual immutable observation database");
                checks.databaseWithoutTarget("PINNED_REPORT_SQLITE", database, "Report profile and rows have no draw target or PIT cutoff");
                JSONObject compact = selection(hash, true);
                try (ReportReadContextCP4 reader = context(snapshot, hash, compact)) {
                    checks.require(reader.compactDiagnosticR64(), "Recognized compact profile");
                    checks.equal(37L, reader.scalarLong("SELECT value FROM report_payload WHERE key=?", new String[]{"expected"}),
                            "Opened SQLite image retains original value");
                }
                if (mode.equals("NEG")) {
                    try (ReportReadContextCP4 reader = context(snapshot, hash, compact)) {
                        try {
                            reader.runCheck("slow-test-query", input -> {
                                reader.scalarLong("WITH RECURSIVE n(x) AS (VALUES(0) UNION ALL SELECT x+1 FROM n WHERE x<1000000000) SELECT sum(x) FROM n", null);
                                return null;
                            }, null, 100);
                            throw new AssertionError("Slow SQL exceeded its deadline without a timeout");
                        } catch (ReportReadContextCP4.ReportReadFailureCP4 expected) {
                            checks.require(expected.code.startsWith("REPORT_CHECK_TIMEOUT|slow-test-query|"), "SQLite check cancelled with its own identity");
                            checks.record("timedOutCheck", expected.code);
                        }
                        ReportCatalogCP4.CheckResult next = reader.runCheck("following-check", input ->
                                new ReportCatalogCP4.CheckResult("OBSERVED","OBSERVED", Long.toString(reader.scalarLong(
                                        "SELECT value FROM report_payload WHERE key=?",new String[]{"expected"}))), null, 2000);
                        checks.equal("37", next.evidence, "Next check reads the same snapshot after timeout");
                    }
                    JSONObject corrupt = selection(hash, false);
                    corrupt.getJSONObject("reproductionR342").put("sourceCommit", "4d1de9d83692635b5c5ce39144fece1c8a31c8ac");
                    try (SQLiteDatabase reread = readOnly(image);
                         ReportReadContextCP4 reader = context(reread, hash, corrupt)) {
                        try {
                            reader.compactDiagnosticR64();
                            throw new AssertionError("Mismatched source accepted");
                        } catch (ReportReadContextCP4.ReportReadFailureCP4 error) {
                            checks.equal("REPORT_REPRODUCTION_SCOPE_CONFLICT", error.code, "Profile with incorrect owner rejected");
                            checks.record("rejectedScope", error.code);
                        }
                    }
                } else if (mode.equals("KEEP")) {
                    try (SQLiteDatabase reread = readOnly(image);
                         ReportReadContextCP4 reader = context(reread, hash, selection(hash, false))) {
                        checks.require(!reader.compactDiagnosticR64(), "Full raw profile distinguished");
                        checks.equal("snapshot.db", reader.evidenceMemberR64(), "Full raw names the exact raw original");
                        checks.equal(37L, reader.scalarLong("SELECT value FROM report_payload WHERE key=?", new String[]{"expected"}),
                                "Both profiles use identical original");
                    }
                    R342ReportAssertions.largeComparisonSummaryRetainsOriginals(app);
                } else {
                    R341ReportTestCases.fullRawMustContainExactSnapshot(app);
                }
                checks.record("diagnosticDatabaseIdentity", database);
                checks.record("snapshotSha256", hash);
                checks.record("reportAcceptance", unobserved);
                checks.record("checkClosure", checksOnly.get("businessAcceptance"));
            }
        });
    }

    private static SQLiteDatabase readOnly(File image) {
        return SQLiteDatabase.openDatabase(image.getPath(), null, SQLiteDatabase.OPEN_READONLY);
    }

    private static JSONObject selection(String hash, boolean compact) throws Exception {
        String source = SourceManifestR815.observationSourceCommit();
        return new JSONObject().put("sourceCommit", source).put("snapshotSha256", hash)
                .put("reportProfile", ReportAttachmentClosureCP12.reportProfileR80(compact))
                .put("rawEvidenceIncluded", !compact).put("reproductionR342", new JSONObject()
                        .put("sourceCommit", source).put("snapshotSha256", hash)
                        .put("includesSqlite", !compact).put("includesInstalledApk", !compact));
    }

    private static ReportReadContextCP4 context(SQLiteDatabase snapshot, String hash, JSONObject manifest) {
        return new ReportReadContextCP4(snapshot, "FIELD173_B12", hash, System.currentTimeMillis(),
                new ReportReadContextCP4.RuntimeObservationCP4("{}"),
                new ReportReadContextCP4.SourceManifestCP4(manifest.toString()), new CancellationSignal(), 8L << 20);
    }
}
