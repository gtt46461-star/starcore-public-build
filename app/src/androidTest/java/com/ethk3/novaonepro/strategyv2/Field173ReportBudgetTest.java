package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.BuildConfig;
import com.ethk3.novaonepro.Field173NativeEvidence;
import com.ethk3.novaonepro.RuntimeObservationScopeR341;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.json.JSONObject;
import org.junit.Test;

public final class Field173ReportBudgetTest {
    private static final long CUTOFF = 1795651440000L;
    private static final int KIB = 1024;
    private static final String LEGACY = "chartObjectIndex";
    private static final String FRAMES = "repairFrameIndex";
    private static final String HEADS = "researchCurrentHeadsR342";
    private static final String ACTIONS = "researchCurrentActionsR342";
    private static final Map<String, Integer> RESERVED = Map.of(
            LEGACY, 16 * KIB, FRAMES, 32 * KIB, HEADS, 32 * KIB, ACTIONS, 32 * KIB);

    @Test public void identityIndexesSurviveProductionQueryBudget() throws Exception {
        execute(false, false);
    }

    @Test public void oversizedIndexKeepsExactOmittedDenominator() throws Exception {
        execute(true, false);
    }

    @Test public void reportBudgetPreservesOriginalRowsAndFrameHashes() throws Exception {
        execute(false, true);
    }

    private static void execute(boolean oversizedIndex, boolean preserve) throws Exception {
        Field173NativeEvidence.run((app, checks) -> {
            File file = new File(app.getCacheDir(), "field173-report-inline-budget.db");
            String database = UUID.randomUUID().toString();
            try {
                try (SQLiteDatabase write = SQLiteDatabase.openOrCreateDatabase(file, null)) {
                    fixture(write, database, oversizedIndex);
                }
                String sourceSha = StatsR27.sha256(Files.readAllBytes(file.toPath()));
                try (SQLiteDatabase db = SQLiteDatabase.openDatabase(file.getPath(), null, SQLiteDatabase.OPEN_READONLY)) {
                    checks.database("SYNTHETIC", "202611260002", CUTOFF, RuntimeObservationScopeR341.databaseIdentity(db));
                    checks.equal(database, RuntimeObservationScopeR341.databaseIdentity(db), "Exact persisted fixture database");
                    Map<String, Object> pressure = pressure(db, checks);
                    ReportR27.R30Input input = ReportSnapshotR30.capture(db);
                    checks.require(input.errors.isEmpty(), "Production report capture: " + input.errors);
                    Map<?, ?> report = ReportR29.document(ReportR29.fromSnapshotR30(input));
                    Map<?, ?> index = (Map<?, ?>) report.get("objectIndex");
                    Map<?, ?> bounded = (Map<?, ?>) index.get("boundedPayloads");
                    Map<?, ?> legacy = (Map<?, ?>) bounded.get(LEGACY);
                    Map<?, ?> frames = (Map<?, ?>) bounded.get(FRAMES);
                    Map<?, ?> bulk = (Map<?, ?>) bounded.get("runtimeCaches");
                    checks.require(((Number) input.watermark.get("inlineRowsetEncodedBytes")).longValue() <= 768 * KIB,
                            "Production report obeys the combined inline budget");
                    checks.equal(17L, frames.get("total"), "All 17 frame identities remain in the denominator");
                    checks.equal(17, frames.get("shown"), "All 17 frame identities appear");
                    checks.equal(0L, frames.get("omitted"), "No valid identity frame lost to bulk payload");
                    checks.require(Boolean.TRUE.equals(bulk.get("inlineByteBudgetExceeded")),
                            "Real bulk rowset exceeded the allocated budget");
                    checks.require(((Number) bulk.get("omitted")).longValue() > 0, "Bulk omissions are explicit");
                    if (oversizedIndex) {
                        int shown = ((Number) legacy.get("shown")).intValue();
                        checks.equal(100L, legacy.get("total"), "Over-limit original row count is retained");
                        checks.require(shown > 0 && shown < 100, "Only complete rows fit in the reserved identity allocation");
                        checks.equal(100L - shown, legacy.get("omitted"), "Omitted original count remains exact");
                        checks.require(Boolean.TRUE.equals(legacy.get("inlineByteBudgetExceeded")),
                                "Identity byte truncation is explicitly measured");
                        checks.equal("DISPLAY_BYTE_BUDGET_COMPLETE_ROWS_ONLY_FULL_ORIGINAL_RETAINED",
                                legacy.get("omissionReason"), "Machine-readable omission reason");
                    } else {
                        checks.equal(14L, legacy.get("total"), "Exact legacy chart index denominator");
                        checks.equal(14, legacy.get("shown"), "All 14 original cached panels appear");
                        checks.equal(0L, legacy.get("omitted"), "No valid cached panel lost to bulk payload");
                    }
                    if (preserve) {
                        List<?> exported = (List<?>) input.data.get(FRAMES);
                        List<String> raw = new ArrayList<>();
                        try (Cursor rows = db.rawQuery("SELECT payload_hash FROM r30r1_chart_cache ORDER BY view_id", null)) {
                            while (rows.moveToNext()) raw.add(rows.getString(0));
                        }
                        List<String> hashes = new ArrayList<>();
                        for (Object row : exported) hashes.add((String) ((Map<?, ?>) row).get("payload_hash"));
                        checks.equal(raw, hashes, "Published index retains exact frame hashes and order");
                        checks.equal(sourceSha, StatsR27.sha256(Files.readAllBytes(file.toPath())),
                                "Production capture does not rewrite its database source");
                    }
                    for (String key : RESERVED.keySet()) checks.require(bounded.containsKey(key), "Index registered: " + key);
                    checks.record("allocations", new JSONObject(pressure));
                    checks.record("legacyIndex", new JSONObject(legacy));
                    checks.record("frameIndex", new JSONObject(frames));
                    checks.record("runtimeBulk", new JSONObject(bulk));
                    checks.record("immutableFixtureSha256", sourceSha);
                }
            } finally {
                SQLiteDatabase.deleteDatabase(file);
            }
        });
    }

    private static Map<String, Object> pressure(SQLiteDatabase db, Field173NativeEvidence.Checks checks) {
        ReportSnapshotR30.Query query = new ReportSnapshotR30.Query(ReportSnapshotR30.metadata(db), new ArrayList<>());
        // Drive the report's actual rowset encoder and SQLite SELECT with 100 oversized requests.
        for (int i = 0; i < 100; i++) {
            query.rows("bulkFixture" + i, "r27_chart_cache", new String[]{"key", "payload_json"},
                    "key", 100, "panel_id='health'", 16384);
        }
        ReportSnapshotR30.rowsets(query);

        int requestedBulk = 0;
        int allocated = 0;
        int bulkAllocated = 0;
        int encoded = 0;
        Map<String, Integer> observedReserves = new LinkedHashMap<>();
        try (Cursor row = db.rawQuery(query.sql(), null)) {
            checks.require(row.moveToFirst(), "Native report query returned one bounded document row");
            for (int i = 0; i < query.cells.size(); i++) {
                ReportSnapshotR30.Cell cell = query.cells.get(i);
                if (cell.inlineEncodedByteBudget < 0) continue;
                int bytes = row.isNull(i) ? 0 : row.getString(i).getBytes(StandardCharsets.UTF_8).length;
                encoded += bytes;
                allocated += cell.inlineEncodedByteBudget;
                checks.require(bytes <= cell.inlineEncodedByteBudget, "Cell allocated-byte bound: " + cell.key);
                String key = cell.key.substring(5);
                if (key.startsWith("bulkFixture")) {
                    requestedBulk++;
                    bulkAllocated += cell.inlineEncodedByteBudget;
                }
                if (RESERVED.containsKey(key)) observedReserves.put(key, cell.inlineEncodedByteBudget);
            }
        }
        checks.equal(100, requestedBulk, "All 100 large datasets even after ordinary pressure");
        checks.equal(656 * KIB, bulkAllocated, "Ordinary dataset allocation is exhausted");
        checks.equal(RESERVED, observedReserves, "All four protected allocations");
        checks.equal(768 * KIB, allocated, "Total allocated inline bytes");
        checks.require(encoded <= allocated, "Measured encoded data is bounded");
        return Map.of("requestedBulkRows", requestedBulk, "allocatedOrdinaryBytes", bulkAllocated,
                "allocatedTotalBytes", allocated, "actualEncodedBytes", encoded, "reservedBytes", observedReserves);
    }

    private static void fixture(SQLiteDatabase db, String database, boolean oversizedIndex) {
        db.beginTransaction();
        try {
            db.execSQL("CREATE TABLE r341_observation_database(singleton INTEGER PRIMARY KEY,identity TEXT NOT NULL)");
            db.execSQL("INSERT INTO r341_observation_database VALUES(1,?)", new Object[]{database});
            db.execSQL("CREATE TABLE r27_chart_cache (panel_id TEXT NOT NULL,key TEXT NOT NULL,"
                    + "payload_json TEXT NOT NULL,updated_at INTEGER NOT NULL,PRIMARY KEY(panel_id,key))");
            for (String ddl : ResearchSchemaR30R1.DDL) db.execSQL(ddl);
            for (String ddl : ResearchActionSchemaR33.DDL) db.execSQL(ddl);

            int cacheRows = oversizedIndex ? 100 : ChartCacheR27.PANELS.length;
            for (int i = 0; i < cacheRows; i++) {
                String panel = oversizedIndex ? "long-chart-" + i + "-" + "a".repeat(212) : ChartCacheR27.PANELS[i];
                db.execSQL("INSERT INTO r27_chart_cache VALUES(?,'panel','{}',?)", new Object[]{panel, CUTOFF - 200});
            }
            String large = JsonR27.write(Map.of("fixture", "a".repeat(15800)));
            for (int i = 0; i < 100; i++) {
                db.execSQL("INSERT INTO r27_chart_cache VALUES('health',?,?,?)",
                        new Object[]{"bulk-" + i, large, CUTOFF - i});
            }
            String dataset = StatsR27.sha256("budget dataset");
            for (int i = 1; i <= 17; i++) {
                String view = String.format(Locale.ROOT, "R%02d", i);
                String run = "run-" + view;
                String payload = JsonR27.write(Map.of("frame", view, "payload", large));
                String hash = StatsR27.sha256(payload);
                db.execSQL("INSERT INTO r30r1_analysis_run VALUES(?,?,?,'SELECT',?,1,10,25,?,'COMMITTED','{}',?)",
                        new Object[]{run, dataset, hash, BuildConfig.SOURCE_COMMIT_R26, hash, CUTOFF});
                db.execSQL("INSERT INTO r30r1_chart_cache VALUES(?,?,?,?,?,?)",
                        new Object[]{view, "frame-" + view, run, payload, hash, CUTOFF});
                db.execSQL("INSERT INTO r33_research_input_head VALUES("
                                + "?,?,'WAITING_SOURCE','ENGINEERING_FIXTURE',?,'fixture',NULL,NULL,?)",
                        new Object[]{view, hash, run, CUTOFF});
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }
}
