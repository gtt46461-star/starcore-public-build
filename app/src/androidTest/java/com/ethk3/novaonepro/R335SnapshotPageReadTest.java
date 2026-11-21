package com.ethk3.novaonepro;

import android.database.sqlite.SQLiteDatabase;
import android.os.CancellationSignal;
import androidx.test.platform.app.InstrumentationRegistry;
import com.ethk3.novaonepro.strategyv2.ChartCacheR27;
import com.ethk3.novaonepro.strategyv2.JsonR27;
import com.ethk3.novaonepro.strategyv2.ReportChartsR30;
import com.ethk3.novaonepro.strategyv2.ResearchTableAdaptersR30R1;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Test;

public final class R335SnapshotPageReadTest {
    private static void require(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
    }

    private static Map<String,Object> snapshot(String issue) {
        Map<String,Object> packet = new LinkedHashMap<>();
        Map<String,Object> mark = Collections.singletonMap("latestIssue", issue);
        Map<String,Object> metrics = Collections.singletonMap("qualifiedExperiments", 1L);
        List<Map<String,Object>> panels = new ArrayList<>();
        for (String id : ReportChartsR30.IDS) {
            Map<String,Object> row = new LinkedHashMap<>();
            row.put("id", id);
            row.put("snapshotWatermark", mark);
            row.put("commonMetrics", metrics);
            row.put("status", "READY");
            row.put("points", Collections.singletonList(Collections.singletonMap("value", 7L)));
            panels.add(row);
        }
        packet.put("panels", panels);
        packet.put("version", "R30_CHART_SNAPSHOT_1");
        packet.put("snapshotWatermark", mark);
        packet.put("commonMetrics", metrics);
        return packet;
    }

    @Test public void authenticatedOlderSnapshotRetainsPointsAndDoesNotSupplyCurrentQualification() {
        try (SQLiteDatabase db = SQLiteDatabase.create(null)) {
            db.execSQL("CREATE TABLE draws(issue TEXT,dirty INTEGER)");
            db.execSQL("CREATE TABLE r27_chart_cache(panel_id TEXT,key TEXT,payload_json TEXT,updated_at INTEGER)");
            String period = "202611210803";
            db.execSQL("INSERT INTO draws VALUES(?,0)", new Object[]{period});
            Map<String,Object> packet = snapshot(period);
            long now = System.currentTimeMillis();
            ChartCacheR27.sealR31(packet, period, now);
            db.execSQL("INSERT INTO r27_chart_cache VALUES('r30-charts','snapshot',?,?)", new Object[]{JsonR27.write(packet),now});
            CancellationSignal cancel = new CancellationSignal();
            Map<String,Object> current = ChartsRepositoryR27.snapshotR30(db,cancel);
            require("READY".equals(ChartsRepositoryR27.snapshotPanelR30(current,0).status),"current snapshot failed");
            db.execSQL("INSERT INTO draws VALUES('202611210804',0)");
            Map<String,Object> historical = ChartsRepositoryR27.snapshotR30(db,cancel);
            for (int i=0; i<2; i++) {
                ResearchVisualDataR82.Panel panel = ChartsRepositoryR27.snapshotPanelR30(historical,i);
                require("HISTORICAL_FRAME".equals(panel.status) && panel.points.size()==1,
                        "valid historical panel "+i+" became "+panel.status+" with "+panel.points.size()+" points");
                require(panel.window.contains(period),"missing original period");
            }
            require("HISTORICAL_FRAME".equals(ChartsRepositoryR27.headlineSnapshotR30(historical).status),
                    "historical qualification displayed as current");
            packet.put("payloadSha256", "0000000000000000000000000000000000000000000000000000000000000000");
            db.execSQL("UPDATE r27_chart_cache SET payload_json=?",new Object[]{JsonR27.write(packet)});
            ResearchVisualDataR82.Panel invalid=ChartsRepositoryR27.snapshotPanelR30(ChartsRepositoryR27.snapshotR30(db,cancel),1);
            require("READ_ERROR_FRAME_HASH".equals(invalid.status) && invalid.points.isEmpty(),"corrupt frame displayed");
        }
    }

    @Test public void unrelatedWalCommitDoesNotEraseDecisionRowsOrTheirTotal() throws Exception {
        org.junit.Assume.assumeTrue(android.os.Build.VERSION.SDK_INT>=30);
        File dir=InstrumentationRegistry.getInstrumentation().getTargetContext().getCacheDir();
        File database=File.createTempFile("decision-page-", ".db", dir);
        try { checkPage(database); } finally { SQLiteDatabase.deleteDatabase(database); }
    }

    @android.annotation.TargetApi(30)
    static void checkPage(File database) throws Exception {
        try (SQLiteDatabase reader=SQLiteDatabase.openOrCreateDatabase(database,null)) {
            require(reader.enableWriteAheadLogging(),"WAL required");
            reader.execSQL("CREATE TABLE originals(issue TEXT PRIMARY KEY,recorded_at INTEGER)");
            reader.execSQL("CREATE TABLE unrelated(value INTEGER)");
            reader.execSQL("INSERT INTO originals VALUES('202611210801',1),('202611210802',2),('202611210803',3)");
            CountDownLatch started=new CountDownLatch(1), committed=new CountDownLatch(1);
            AtomicBoolean observed=new AtomicBoolean();
            AtomicReference<Throwable> error=new AtomicReference<>();
            reader.setCustomScalarFunction("observe_page",value -> {
                if (observed.compareAndSet(false,true)) {
                    started.countDown();
                    try { require(committed.await(10,TimeUnit.SECONDS),"writer did not commit during read"); }
                    catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); throw new AssertionError(interrupted); }
                }
                return value;
            });
            reader.execSQL("CREATE VIEW r30_decision_explain AS SELECT issue,issue AS decision_key,'gate' AS entry_point,"
                    +"0 AS original_allow,'observed' AS original_reason,0 AS certificate_active,'{}' AS items_json,"
                    +"observe_page(issue) AS source_json,recorded_at FROM originals");
            try (SQLiteDatabase writer=SQLiteDatabase.openDatabase(database.getPath(),null,SQLiteDatabase.OPEN_READWRITE)) {
                Thread change=new Thread(() -> {
                    try {
                        require(started.await(10,TimeUnit.SECONDS),"reader did not reach query");
                        writer.execSQL("INSERT INTO unrelated VALUES(1)");
                    } catch (Throwable failure) { error.set(failure); } finally { committed.countDown(); }
                });
                change.start();
                Map<String,Object> page;
                try { page=ResearchTableAdaptersR30R1.readPageR31(reader,"T16",Collections.emptyMap(),0,2,new CancellationSignal()); }
                finally { change.join(12000); }
                require(error.get()==null && observed.get() && !change.isAlive(),"writer protocol failed "+error.get());
                require("READ_OK".equals(page.get("status")) && ((Number)page.get("shown")).intValue()==2,
                        "unrelated durable commit cleared T16: "+page.get("status"));
                require(((Number)page.get("totalMatchingRows")).intValue()==3 && Boolean.TRUE.equals(page.get("hasMore")),"page total");
                require("202611210803".equals(((Map<?,?>)((List<?>)page.get("rows")).get(0)).get("issue")),"page ordering");
                Map<String,Object> past=ResearchTableAdaptersR30R1.readPageR31(reader,"T16",Collections.emptyMap(),4,2,null);
                require("EMPTY_NO_ROWS".equals(past.get("status")) && ((Number)past.get("totalMatchingRows")).intValue()==3,
                        "empty page lost full denominator");
            }
        }
    }
}
