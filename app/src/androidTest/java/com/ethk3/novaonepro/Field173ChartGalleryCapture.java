package com.ethk3.novaonepro;

import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.os.CancellationSignal;
import android.os.SystemClock;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import androidx.test.platform.app.InstrumentationRegistry;
import com.ethk3.novaonepro.r952.ResearchChartViewR82;
import com.ethk3.novaonepro.strategyv2.ChartCacheR27;
import com.ethk3.novaonepro.strategyv2.JsonR27;
import com.ethk3.novaonepro.strategyv2.RepairAnalysisAndroidFixture;
import com.ethk3.novaonepro.strategyv2.ResearchTableAdaptersR30R1;
import com.ethk3.novaonepro.strategyv2.ResearchWriteQueueR27;
import com.ethk3.novaonepro.strategyv2.StatsR27;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.json.JSONObject;
import org.junit.Test;

public final class Field173ChartGalleryCapture {
    private static final String TARGET = "202610240025";
    private static final long CUTOFF = 1792772640000L;
    private static final String RESEARCH_FRAME = "r30r1_chart_cache";
    private static final String LEGACY_FRAME = "r27_chart_cache";

    /** Diagnostic instrumentation using the current APK. This run has no device-acceptance authority. */
    @Test public void collectGalleryAndPagedSqlFromActualAndroidOwners() throws Exception {
        collect("CURRENT");
    }

    @Test public void staleFrameAndMissingTableRemainExplicitInNativeGallery() throws Exception {
        collect("STALE_AND_MISSING");
    }

    @Test public void galleryRefreshPreservesOriginalSourceRows() throws Exception {
        collect("PRESERVATION");
    }

    private static void collect(String scenario) throws Exception {
        Field173NativeEvidence.run((app, checks) -> {
            Instrumentation android = InstrumentationRegistry.getInstrumentation();
            String sandbox = InstrumentationRegistry.getArguments().getString("r30Sandbox");
            checks.require(sandbox != null && sandbox.matches("R343_[a-f0-9]{32}"), "Unique engineering case sandbox");
            File output = app.getExternalFilesDir("field173-galleries");
            checks.require(output != null, "Android external evidence directory");
            File gallery = new File(output, sandbox);
            checks.require(gallery.mkdir(), "Fresh gallery directory");
            DbStore store = new DbStore(app);
            try {
                installDrawInputs(store, checks);
                Map<String, String> produced = RepairAnalysisAndroidFixture.create(store);
                checks.equal(18, produced.size(), "Original research producers issued per-ID outcomes");
                ResearchWriteQueueR27 queue = new ResearchWriteQueueR27(store);
                checks.require(queue.submit("CHART_CACHE", ChartCacheR27.build(store.readable(), null), () -> false),
                        "Original legacy cache writer committed");
                if ("STALE_AND_MISSING".equals(scenario)) staleLegacyFrameAndHideOneTable(store);
                File database = app.getDatabasePath(AppConfig.DB_NAME);
                List<Map<String, Object>> chartRows = new ArrayList<>();
                List<Map<String, Object>> logicalTables = new ArrayList<>();
                try (SQLiteDatabase db = SQLiteDatabase.openDatabase(database.getPath(), null, SQLiteDatabase.OPEN_READONLY)) {
                    String identity = RuntimeObservationScopeR341.databaseIdentity(db);
                    checks.require(identity.matches("[a-f0-9-]{36}"), "Exact gallery SQLite identity");
                    checks.database("SYNTHETIC", TARGET, CUTOFF, identity);
                    long versionBeforeUi = scalar(db, "PRAGMA data_version");
                    String sourceBefore = chartSourceDigest(db);
                    legacy(android, app, db, gallery, checks, chartRows, "PRESERVATION".equals(scenario));
                    research(android, app, db, gallery, checks, chartRows);
                    String sourceAfter = chartSourceDigest(db);
                    checks.equal(sourceBefore, sourceAfter, "Owned charts and draw inputs remain byte-identical during UI");
                    long tablesVersion = scalar(db, "PRAGMA data_version");
                    for (String id : ResearchTableAdaptersR30R1.ids()) {
                        Map<String, Object> page = ResearchTableAdaptersR30R1.readPageR31(db, id, Map.of(), 0, 8, null);
                        checks.equal(id, page.get("id"), "Actual logical table reader returned " + id);
                        checks.require(hex(page.get("receiptSha256")), "Native pagination receipt for " + id);
                        if ("EMPTY_NO_ROWS".equals(page.get("status"))) {
                            checks.equal(0L, page.get("totalMatchingRows"), "Zero SQL rows for " + id);
                        }
                        if ("STALE_AND_MISSING".equals(scenario) && "T08".equals(id)) {
                            checks.equal("READ_ERROR", page.get("status"), "One intentionally missing source table");
                            checks.equal(null, page.get("totalMatchingRows"), "Missing SQL source is not zero");
                            checks.require(String.valueOf(page.get("reason")).contains("r27_expert_proposal"),
                                    "The failed table retains its real missing SQL source");
                        }
                        ResearchTableAdaptersR30R1.Page query = ResearchTableAdaptersR30R1.page(id, Map.of(), 0, 8);
                        logicalTables.add(Map.of("id", id, "sourceSql", ResearchTableAdaptersR30R1.query(id),
                                "pageQuery", query.sql, "bindArgs", List.of(query.args), "receipt", page));
                    }
                    checks.equal(tablesVersion, scalar(db, "PRAGMA data_version"), "All paged SQL runs saw a stable source read");
                    if ("STALE_AND_MISSING".equals(scenario)) {
                        checks.equal("STALE_FRAME_ABI", find(chartRows, "p01_three_scores").get("status"),
                                "Legacy chart receives a real prior-version frame and rejects it");
                        checks.equal(0, find(chartRows, "p01_three_scores").get("repositoryFinitePoints"),
                                "Stale-frame error does not manufacture plotted output");
                    }
                    for (String ready : List.of("r83_number_line", "R01", "R02", "R03")) {
                        Map<String, Object> screen = find(chartRows, ready);
                        checks.require(screen.get("rendererFinitePoints") instanceof Number
                                && ((Number) screen.get("rendererFinitePoints")).longValue() > 0L,
                                "Available known fixture inputs yield actual rendered values: " + ready);
                    }
                    checks.equal(47, chartRows.size(), "All 47 actually rendered card screenshots");
                    checks.equal(16, logicalTables.size(), "All 16 logical SQL readers actually called");
                    Map<String, Object> original = Map.of("scope", "SYNTHETIC_ANDROID_GALLERY_COLLECTION",
                            "sourceCommit", BuildConfig.SOURCE_COMMIT_R26, "databaseIdentity", identity,
                            "scenario", scenario, "sourceVersionBeforeUi", versionBeforeUi,
                            "sourceVersionForTables", tablesVersion, "chartSourceSha256", sourceAfter,
                            "charts", chartRows, "logicalTables", logicalTables);
                    byte[] raw = JsonR27.write(original).getBytes(StandardCharsets.UTF_8);
                    Files.write(new File(gallery, "gallery.json").toPath(), raw,
                            java.nio.file.StandardOpenOption.CREATE_NEW, java.nio.file.StandardOpenOption.WRITE);
                    checks.record("gallerySha256", StatsR27.sha256(raw));
                    checks.record("galleryDirectory", gallery.getPath());
                    checks.record("chartCount", chartRows.size());
                    checks.record("logicalTableCount", logicalTables.size());
                    checks.record("scope", "SYNTHETIC_ANDROID_GALLERY_COLLECTION");
                    checks.record("scenario", scenario);
                }
            } finally {
                store.close();
            }
        });
    }

    private static Map<String, Object> find(List<Map<String, Object>> rows, String id) {
        for (Map<String, Object> row : rows) if (id.equals(row.get("id"))) return row;
        throw new AssertionError("Missing actual chart ID " + id);
    }

    private static String chartSourceDigest(SQLiteDatabase db) {
        List<Object> groups = new ArrayList<>();
        for (String sql : List.of(
                "SELECT issue,dirty,n1,n2,n3,n4,n5,n6,n7,n8,n9,n10 FROM draws ORDER BY issue",
                "SELECT panel_id,key,payload_json,updated_at FROM r27_chart_cache ORDER BY panel_id,key",
                "SELECT view_id,frame_id,run_id,payload_hash,payload_json FROM r30r1_chart_cache ORDER BY view_id,frame_id",
                "SELECT run_id,source_commit,dataset_hash,summary_json FROM r30r1_analysis_run ORDER BY run_id")) {
            List<Object> rows = new ArrayList<>();
            try (Cursor cursor = db.rawQuery(sql, null)) {
                while (cursor.moveToNext()) {
                    List<String> row = new ArrayList<>();
                    for (int col = 0; col < cursor.getColumnCount(); col++) row.add(cursor.getString(col));
                    rows.add(row);
                }
            }
            groups.add(rows);
        }
        return StatsR27.sha256(JsonR27.write(groups));
    }

    @SuppressWarnings("unchecked")
    private static void staleLegacyFrameAndHideOneTable(DbStore store) {
        store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("R30_CHART_SCREENSHOT_FIXTURE", db -> {
            Map<String, Object> previous;
            try (Cursor row = db.rawQuery("SELECT payload_json FROM r27_chart_cache WHERE panel_id=? AND key='panel'",
                    new String[]{"p01_three_scores"})) {
                if (!row.moveToFirst()) throw new AssertionError("Exact legacy panel cache frame missing");
                previous = (Map<String, Object>) JsonR27.parse(row.getString(0));
            }
            previous.put("buildVersionCode", (long) BuildConfig.VERSION_CODE - 1);
            previous.remove("payloadSha256");
            previous.put("payloadSha256", StatsR27.sha256(JsonR27.write(previous)));
            db.execSQL("UPDATE r27_chart_cache SET payload_json=? WHERE panel_id=? AND key='panel'",
                    new Object[]{JsonR27.write(previous), "p01_three_scores"});
            db.execSQL("ALTER TABLE r27_expert_proposal RENAME TO r343_fixture_hidden_expert_proposal");
            return 0;
        });
    }

    private static void installDrawInputs(DbStore store, Field173NativeEvidence.Checks checks) {
        long first = TargetIssueResolver.sequenceOrdinalSU1("202610240001");
        for (int i = 0; i < 24; i++) {
            Models.Draw draw = new Models.Draw();
            draw.issue = TargetIssueResolver.fromSequenceOrdinalSU1(first + i);
            draw.openTime = CUTOFF - (24L - i) * 60_000L;
            draw.fetchTime = draw.openTime + 1L;
            for (int pos = 0; pos < draw.numbers.length; pos++) draw.numbers[pos] = (i + pos) % 10 + 1;
            checks.require(store.saveDraw(draw) >= 0, "Real draw persistence for synthetic issue " + draw.issue);
        }
    }

    private static void legacy(Instrumentation android, Context app, SQLiteDatabase db, File gallery,
                               Field173NativeEvidence.Checks checks, List<Map<String, Object>> receipts, boolean refresh) throws Exception {
        List<String> ids = ChartRegistryR31.visibleLegacyIds();
        ChartsActivityR83 activity = (ChartsActivityR83) android.startActivitySync(
                new Intent(app, ChartsActivityR83.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        try {
            LinearLayout cards = R30ChartsAndroidTest.cards(activity);
            awaitCards(android, cards, ids.size(), null);
            if (refresh) {
                AtomicReference<View> previous = new AtomicReference<>();
                R30GalleryCaptureR30.onMain(android, () -> {
                    previous.set(cards.getChildAt(0));
                    Button button = refreshButton(activity.getWindow().getDecorView());
                    if (button == null || !button.performClick()) throw new AssertionError("Actual chart refresh button unavailable");
                });
                awaitCards(android, cards, ids.size(), previous.get());
            }
            ChartsRepositoryR27.Page owner = new ChartsRepositoryR27.Page(db);
            for (int i = 0; i < ids.size(); i++) {
                String id = ids.get(i);
                ResearchVisualDataR82.Panel panel = owner.read(id, new CancellationSignal());
                receipts.add(capture(android, cards, i, id, null, panel, db, gallery, checks));
            }
        } finally {
            android.runOnMainSync(activity::finish);
            android.waitForIdleSync();
        }
    }

    private static void research(Instrumentation android, Context app, SQLiteDatabase db, File gallery,
                                 Field173NativeEvidence.Checks checks, List<Map<String, Object>> receipts) throws Exception {
        ResearchWorkbenchActivityR30R1 activity = (ResearchWorkbenchActivityR30R1) android.startActivitySync(
                new Intent(app, ResearchWorkbenchActivityR30R1.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        try {
            for (int group = 0; group < 3; group++) {
                int next = group;
                android.runOnMainSync(() -> R30RepairChartsAndroidTest.group(activity, next));
                R30RepairChartsAndroidTest.ready(android, activity, group);
                List<ResearchVisualDataR82.Panel> panels = ResearchWorkbenchRepositoryR30R1.load(db, group, new CancellationSignal());
                List<String> ids = ChartRegistryR31.researchGroup(group);
                checks.equal(ids.size(), panels.size(), "Research workbench group " + group);
                for (int i = 0; i < ids.size(); i++) {
                    receipts.add(capture(android, R30RepairChartsAndroidTest.cards(activity), i, ids.get(i), ids.get(i),
                            panels.get(i), db, gallery, checks));
                }
            }
        } finally {
            R30RepairChartsAndroidTest.finishAndAwait(android, activity);
        }
    }

    private static Button refreshButton(View view) {
        if (view instanceof Button && "刷新图表".contentEquals(((Button) view).getText())) return (Button) view;
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                Button found = refreshButton(group.getChildAt(i));
                if (found != null) return found;
            }
        }
        return null;
    }

    private static void awaitCards(Instrumentation android, LinearLayout cards, int count, View retiringCard) {
        long end = SystemClock.elapsedRealtime() + 120_000L;
        AtomicReference<Integer> actual = new AtomicReference<>();
        AtomicReference<View> first = new AtomicReference<>();
        do {
            R30GalleryCaptureR30.onMain(android, () -> {
                actual.set(cards.getChildCount());
                first.set(cards.getChildAt(0));
            });
            if (Integer.valueOf(count).equals(actual.get()) && first.get() != retiringCard) return;
            SystemClock.sleep(75L);
        } while (SystemClock.elapsedRealtime() < end);
        throw new AssertionError("Chart page did not render all cards: " + actual.get() + " / " + count);
    }

    private static Map<String, Object> capture(Instrumentation android, LinearLayout cards, int index,
                                                String id, String tag, ResearchVisualDataR82.Panel expected,
                                                SQLiteDatabase db, File folder, Field173NativeEvidence.Checks checks) throws Exception {
        R30GalleryCaptureR30.Frame frame = R30GalleryCaptureR30.capture(android, cards, index, tag, expected.title);
        File screenshot = new File(folder, id + ".png");
        try (FileOutputStream stream = new FileOutputStream(screenshot)) {
            checks.require(frame.bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream), "Exact UI capture for " + id);
        } finally {
            frame.bitmap.recycle();
        }
        AtomicReference<ResearchVisualDataR82.Panel> rendered = new AtomicReference<>();
        R30GalleryCaptureR30.onMain(android, () -> {
            ResearchChartViewR82 chart = R30GalleryCaptureR30.findChart(cards.getChildAt(index));
            if (chart != null) rendered.set((ResearchVisualDataR82.Panel) R30RepairChartsAndroidTest.field(chart, "panel"));
        });
        boolean expectedChart = !"STATUS".equals(expected.kind) && "HAS_DATA".equals(expected.displayState);
        checks.equal(expectedChart, rendered.get() != null, "Repository result reaches native renderer: " + id);
        if (expectedChart) {
            checks.equal(expected.kind, rendered.get().kind, "Rendered native chart kind for " + id);
            checks.equal(expected.series, rendered.get().series, "Rendered series for " + id);
            checks.equal(finite(expected), finite(rendered.get()), "Actual renderer finite points for " + id);
        } else {
            checks.require("STATUS".equals(expected.kind) || !expected.reasonCode.isBlank(),
                    "An unavailable chart has an explicit cause " + id);
        }
        ChartRegistryR31.Entry catalog = ChartRegistryR31.find(id);
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", id);
        row.put("status", expected.status);
        row.put("reason", expected.reasonCode);
        row.put("source", expected.source);
        row.put("sourceOwner", catalog.sourceOwner);
        row.put("scope", catalog.scope);
        row.put("role", catalog.role);
        row.put("series", expected.series.size());
        row.put("repositoryFinitePoints", finite(expected));
        row.put("rendererFinitePoints", rendered.get() == null ? null : finite(rendered.get()));
        row.put("screenshotFile", screenshot.getName());
        row.put("screenshotSha256", StatsR27.sha256(Files.readAllBytes(screenshot.toPath())));
        row.put("capture", frame.evidence);
        Map<String, Object> trace = sourceTrace(db, catalog, expected);
        checks.require((trace.get("sourceRows") instanceof Number && trace.get("filterRows") instanceof Number)
                || trace.containsKey("traceError"), "Source/filtered denominators or their actual SQLite error: " + id);
        row.put("sourceTrace", trace);
        return row;
    }

    private static int finite(ResearchVisualDataR82.Panel panel) {
        int count = 0;
        for (ResearchVisualDataR82.Point p : panel.points) {
            boolean present = Double.isFinite(p.value) || Double.isFinite(p.open)
                    || Double.isFinite(p.lower) || Double.isFinite(p.upper);
            if (p.stack != null) for (double value : p.stack) present |= Double.isFinite(value);
            if (present) count++;
        }
        return count;
    }

    private static Map<String, Object> sourceTrace(SQLiteDatabase db, ChartRegistryR31.Entry catalog,
                                                   ResearchVisualDataR82.Panel expected) {
        Map<String, Object> evidence = new LinkedHashMap<>();
        for (String key : List.of("sourceRows", "filterRows", "cacheABI", "frameHash")) evidence.put(key, null);
        try {
            if (catalog.group == ChartRegistryR31.Group.RESEARCH) {
                String where = " FROM " + RESEARCH_FRAME + " WHERE view_id=?";
                evidence.put("sourceRows", scalar(db, "SELECT count(*)" + where, catalog.id));
                String commitSql = " FROM " + RESEARCH_FRAME + " c JOIN r30r1_analysis_run r ON r.run_id=c.run_id"
                        + " WHERE c.view_id=? AND r.status='COMMITTED'";
                evidence.put("filterRows", scalar(db, "SELECT count(*)" + commitSql, catalog.id));
                evidence.put("filterScope", "COMMITTED_BEFORE_PIN_AND_ORIGINAL_VALIDATION");
                try (Cursor query = db.rawQuery("SELECT c.payload_hash,r.source_commit,r.dataset_hash,r.role,r.run_id,"
                        + "CASE WHEN length(r.summary_json)<=262144 THEN r.summary_json ELSE NULL END"
                        + commitSql + " ORDER BY (r.source_commit=?) DESC,c.updated_at DESC,c.frame_id LIMIT 1",
                        new String[]{catalog.id, BuildConfig.SOURCE_COMMIT_R26})) {
                    if (query.moveToFirst()) {
                        evidence.put("frameHash", query.getString(0));
                        evidence.put("frameSourceCommit", query.getString(1));
                        evidence.put("datasetHash", query.getString(2));
                        evidence.put("frameRole", query.getString(3));
                        evidence.put("sourceRunId", query.getString(4));
                        if (!query.isNull(5)) {
                            Object summary = JsonR27.parse(query.getString(5));
                            Object contract = summary instanceof Map ? ((Map<?, ?>) summary).get("sourceContract") : null;
                            if (contract instanceof Map) evidence.put("cacheABI", ((Map<?, ?>) contract).get("abi"));
                        }
                    }
                }
            } else if (catalog.group == ChartRegistryR31.Group.LEGACY_R83) {
                r83Source(db, catalog.id, evidence);
            } else {
                String panelId = catalog.group == ChartRegistryR31.Group.LEGACY_CACHED ? catalog.id : "r30-charts";
                String key = catalog.group == ChartRegistryR31.Group.LEGACY_CACHED ? "panel" : "snapshot";
                if (catalog.group == ChartRegistryR31.Group.LEGACY_STATUS && "r27_chart_cache".equals(expected.source)) {
                    panelId = "mainline";
                    key = "choice";
                }
                evidence.put("sourceRows", scalar(db, "SELECT count(*) FROM " + LEGACY_FRAME + " WHERE panel_id=?", panelId));
                evidence.put("filterRows", scalar(db, "SELECT count(*) FROM " + LEGACY_FRAME + " WHERE panel_id=? AND key=?", panelId, key));
                evidence.put("filterScope", "SQL_PANEL_KEY_BEFORE_ABI_HASH_AND_FRESHNESS_CHECKS");
                try (Cursor query = db.rawQuery("SELECT payload_json FROM " + LEGACY_FRAME + " WHERE panel_id=? AND key=?",
                        new String[]{panelId, key})) {
                    if (query.moveToFirst()) {
                        String raw = query.getString(0);
                        evidence.put("frameHash", StatsR27.sha256(raw));
                        Object json = JsonR27.parse(raw);
                        if (json instanceof Map) {
                            Map<?, ?> fields = (Map<?, ?>) json;
                            evidence.put("cacheABI", fields.get("frameSchema"));
                            evidence.put("frameVersionCode", fields.get("buildVersionCode"));
                            evidence.put("declaredPayloadHash", fields.get("payloadSha256"));
                            evidence.put("sourceIssue", fields.get("sourceIssue"));
                        }
                    }
                }
            }
        } catch (RuntimeException unavailable) {
            evidence.put("traceError", unavailable.getClass().getSimpleName() + ":" + unavailable.getMessage());
        }
        evidence.put("repositoryStatus", expected.status);
        if (evidence.get("frameHash") == null) {
            evidence.putIfAbsent("frameHashReason", "NO_READABLE_PERSISTED_FRAME_FOR_THIS_ID");
        }
        if (evidence.get("cacheABI") == null) {
            evidence.put("cacheABIReason", "NO_READABLE_ABI_IN_PERSISTED_ORIGINAL");
        }
        return evidence;
    }

    private static void r83Source(SQLiteDatabase db, String id, Map<String, Object> evidence) {
        evidence.put("cacheABI", "DIRECT_R83_REPOSITORY_NO_CACHE_ABI");
        evidence.put("frameHashReason", "PRODUCED_BY_DIRECT_SQL_NO_PERSISTED_FRAME");
        String shadow = DbHelper.TABLE_R35_SHADOW_OBSERVATION;
        String genome = DbHelper.TABLE_R908_ALGORITHM_GENOME;
        String prediction = DbHelper.TABLE_R908_ALGORITHM_PREDRAW;
        switch (id) {
            case "r83_algorithm_all":
                countR83(db, evidence, "r27_live_frozen", "POLICY_GROUPS_LIMIT_60",
                        "SELECT s.policy_hash FROM r27_live_shadow s JOIN r27_live_frozen f ON f.policy_hash=s.policy_hash"
                                + " AND f.status='FROZEN' AND f.hit>=0 GROUP BY s.policy_hash ORDER BY COUNT(f.target_ordinal) DESC LIMIT 60");
                return;
            case "r83_algorithm_rank":
                countR83(db, evidence, prediction, "GENOME_GROUPS_LIMIT_60",
                        "SELECT g.genome_id FROM " + genome + " g JOIN " + prediction + " p"
                                + " ON p.genome_id=g.genome_id AND p.hit_label>=0 GROUP BY g.genome_id"
                                + " ORDER BY COUNT(p.target_issue) DESC LIMIT 60");
                return;
            case "r83_best_curve":
                countR83(db, evidence, prediction, "SETTLED_ROWS_OF_ORIGINAL_SELECTED_GENOME",
                        "SELECT target_issue FROM " + prediction + " WHERE hit_label>=0 AND genome_id=("
                                + "SELECT p.genome_id FROM " + prediction + " p LEFT JOIN " + genome + " g ON g.genome_id=p.genome_id"
                                + " WHERE p.hit_label>=0 GROUP BY p.genome_id HAVING COUNT(*)>=20"
                                + " ORDER BY 1.0*SUM(CASE WHEN p.hit_label=1 THEN 1 ELSE 0 END)/COUNT(*) DESC LIMIT 1)");
                return;
            case "r83_action_candles":
            case "r83_all_hit":
            case "r83_act_hit":
                boolean explore = !"r83_all_hit".equals(id);
                String join = explore ? " JOIN " + ExplorationLaneR83.TABLE + " e ON e.target_issue=o.target_issue" : "";
                String paired = explore
                        ? "sum(CASE WHEN e.position=o.position AND e.n1=o.n1 AND e.n2=o.n2 AND e.n3=o.n3 THEN 1 ELSE 0 END)"
                        : "count(*)";
                String filters = " WHERE o.pre_draw_frozen=1 AND o.settled=1 AND o.hit_label IN (0,1)"
                        + (explore ? " AND e.action='EXPLORE'" : "");
                int limit = "r83_action_candles".equals(id) ? 600 : 139;
                countR83(db, evidence, shadow, "UNIQUE_MATCHING_SHADOW_PAIRS_AFTER_ORIGINAL_BOUNDED_QUERY",
                        "SELECT target_issue FROM (SELECT o.target_issue,count(*) AS joined," + paired + " AS paired FROM "
                                + shadow + " o" + join + filters + " GROUP BY o.target_issue ORDER BY o.target_issue DESC LIMIT "
                                + limit + ") WHERE joined=1 AND paired=1");
                return;
            case "r83_calibration":
                countR83(db, evidence, shadow, "VALID_SETTLED_PROBABILITY_ROWS_LIMIT_2000",
                        "SELECT selected_hit_p FROM " + shadow + " WHERE settled=1 AND hit_label>=0"
                                + " AND selected_hit_p>0 AND selected_hit_p<=1 ORDER BY target_issue DESC LIMIT 2000");
                return;
            case "r83_brier20":
                countR83(db, evidence, AtomicToolWorldR943.CONTEXT, "LEGACY_ATOMIC_VALID_SETTLEMENTS_LIMIT_139",
                        "SELECT target_issue FROM " + AtomicToolWorldR943.CONTEXT + " WHERE hit>=0"
                                + " AND network_probability>=0 AND network_probability<=1 ORDER BY target_issue DESC LIMIT 139");
                return;
            default:
                if (!ChartRegistryR31.R83_DRAW_INPUT.contains(id)) throw new IllegalStateException("UNMAPPED_R83_PANEL:" + id);
                evidence.put("sourceRows", scalar(db, "SELECT count(*) FROM draws"));
                ChartsRepositoryR83.ReadContext source = new ChartsRepositoryR83.ReadContext(db, new CancellationSignal());
                if (source.drawFailure != null) throw source.drawFailure;
                evidence.put("filterRows", source.drawIssues.size());
                evidence.put("filterScope", "ACTUAL_R83_CLEAN_DRAWS_LIMIT_600");
        }
    }

    private static void countR83(SQLiteDatabase db, Map<String, Object> evidence, String table,
                                 String filterScope, String filteredSql) {
        evidence.put("filterScope", filterScope);
        evidence.put("filterSql", filteredSql);
        evidence.put("sourceSql", "SELECT count(*) FROM " + table);
        evidence.put("sourceRows", scalar(db, "SELECT count(*) FROM " + table));
        evidence.put("filterRows", scalar(db, "SELECT count(*) FROM (" + filteredSql + ")"));
    }

    private static long scalar(SQLiteDatabase db, String sql, String... args) {
        try (Cursor row = db.rawQuery(sql, args)) {
            if (!row.moveToFirst()) throw new IllegalStateException("Missing source result: " + sql);
            return row.getLong(0);
        }
    }

    private static boolean hex(Object value) {
        return value instanceof String && ((String) value).matches("[a-f0-9]{64}");
    }
}
