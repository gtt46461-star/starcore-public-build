package com.ethk3.novaonepro;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.CancellationSignal;
import android.util.Xml;
import androidx.test.platform.app.InstrumentationRegistry;
import com.ethk3.novaonepro.strategyv2.RepairAnalysisAndroidFixture;
import java.io.File;
import java.io.FileInputStream;
import java.util.List;
import org.junit.Test;
import org.xmlpull.v1.XmlPullParser;

/** Disposable signed integration test. Emit predefined categories only, with no preference content. */
public final class R335AppBoundaryTest {
    private static void record(String k, String v) {
        System.out.println("R335_BOUNDED " + k + " " + v);
    }

    @Test public void explicitStartAndFrameworkPersistence() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        ServiceHeartbeatStore service = new ServiceHeartbeatStore(context);
        service.setUserEnabled(true);
        service.markUiStartRequested("r335_disposable_probe");
        SharedPreferences prefs = context.getSharedPreferences("v20_service_runtime_light", Context.MODE_PRIVATE);
        String status = prefs.getString(EnabledWindowJournalR335.STATUS_KEY, "");
        record("recorded", "RECORDED".equals(status) ? "YES" : status.isEmpty() ? "ABSENT" : "OTHER");
        record("enabled", prefs.getBoolean("user_enabled", false) ? "YES" : "NO");
        String original = prefs.getString(EnabledWindowJournalR335.PREF_KEY, "");
        try {
            boolean valid = !EnabledWindowJournalR335.parse(original).events.isEmpty();
            record("memoryJournal", valid ? "VALID" : "EMPTY");
        } catch (IllegalArgumentException bad) {
            record("memoryJournal", "INVALID");
        }
        File file = new File(context.getApplicationInfo().dataDir, "shared_prefs/v20_service_runtime_light.xml");
        try (FileInputStream f = new FileInputStream(file)) {
            boolean binary = f.read() == 'A' && f.read() == 'B' && f.read() == 'X' && f.read() == 0;
            record("frameworkFormat", binary ? "ANDROID_BINARY_XML" : "TEXT_OR_OTHER");
        }
        try (FileInputStream f = new FileInputStream(file)) {
            XmlPullParser xml = Xml.newPullParser();
            xml.setInput(f, "UTF-8");
            EnabledWindowCaptureProviderR335.projection(xml);
            record("projection", "PASS");
        } catch (Exception error) {
            String reason = String.valueOf(error.getMessage());
            String category = error instanceof org.xmlpull.v1.XmlPullParserException ? "XML_PARSE" : "OTHER";
            for (String known : new String[]{"PREF_ROOT","JOURNAL_TYPE_OR_DUPLICATE","STATUS_TYPE_OR_DUPLICATE","ASSIGNMENT_TYPE_OR_DUPLICATE","ASSIGNMENT_VALUE","ASSIGNMENT_INCOMPLETE","ASSIGNMENT_HISTORY_MISMATCH","JOURNAL_TRUNCATED","JOURNAL_HASH","JOURNAL_HEADER","JOURNAL_IDENTITY","JOURNAL_ELAPSED_REVERSED","JOURNAL_CLOCK_DISCONTINUITY"}) if (known.equals(reason)) category = known;
            record("projection", category);
        }
    }

    @Test public void actualResearchFrameAndUiReader() throws Exception {
        Context ctx = InstrumentationRegistry.getInstrumentation().getTargetContext();
        DbStore store = new DbStore(ctx);
        try {
            RepairAnalysisAndroidFixture.create(store);
            List<ResearchVisualDataR82.Panel> panels = ResearchWorkbenchRepositoryR30R1.load(store.readable(), 0, new CancellationSignal());
            for (int i = 0; i < 3; i++) {
                ResearchVisualDataR82.Panel panel = panels.get(i);
                String reason = panel.kind.equals("STATUS") ? "UNCLASSIFIED_STATUS" : "CHART";
                for (String k : new String[]{"FRAME_BYTE_BUDGET","CURRENT_ORIGINAL_RESEARCH_SOURCE_REQUIRED","FRAME_HASH_MISMATCH","RESEARCH_CACHE_NOT_BOUND_TO_ORIGINAL","RESEARCH_FRAME_REQUIRES_PAGINATION","WAITING_ANALYSIS","R27_JSON","CursorWindow","RESEARCH_SERIES_LIMIT","NullPointerException","IllegalArgumentException"}) if ((panel.note+" "+panel.status).contains(k)) { reason=k; break; }
                record("chart"+(i+1), reason);
            }
            try (Cursor cursor = store.readable().rawQuery("SELECT c.view_id,length(CAST(r.summary_json AS BLOB)),length(CAST(c.payload_json AS BLOB)) FROM r30r1_chart_cache c JOIN r30r1_analysis_run r ON r.run_id=c.run_id WHERE c.view_id IN ('R01','R02','R03') ORDER BY c.view_id", null)) {
                while (cursor.moveToNext()) {
                    String id=cursor.getString(0);
                    if ("R01".equals(id) || "R02".equals(id) || "R03".equals(id)) {
                        record(id+"OriginalBytes", Integer.toString(cursor.getInt(1)));
                        record(id+"PageBytes", Integer.toString(cursor.getInt(2)));
                    }
                }
            }
        } finally { store.close(); }
    }
}
