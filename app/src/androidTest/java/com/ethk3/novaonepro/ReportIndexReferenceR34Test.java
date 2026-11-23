package com.ethk3.novaonepro;

import android.database.sqlite.SQLiteDatabase;
import android.os.CancellationSignal;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import java.io.File;
import java.util.Collections;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public final class ReportIndexReferenceR34Test {
    private static final String INDEX = RuntimeChainDiagnosisR975.INDEX;
    private static final String DIGEST = String.join("", Collections.nCopies(64, "a"));
    private static final String APK = String.join("", Collections.nCopies(64, "b"));
    private static final String SOURCE = String.join("", Collections.nCopies(40, "c"));

    private static String read(SQLiteDatabase db, JSONObject doc) {
        return new ReportReadContextCP4(db, "test", APK, 1,
                new ReportReadContextCP4.RuntimeObservationCP4("{}"),
                new ReportReadContextCP4.SourceManifestCP4(doc.toString()), new CancellationSignal(),
                4L << 20).assetReference(INDEX);
    }

    @Test public void checkCompactBindingAndHistoricalRawReferences() throws Exception {
        File cache = InstrumentationRegistry.getInstrumentation().getTargetContext().getCacheDir();
        File file = File.createTempFile("report-index-", ".db", cache);
        SQLiteDatabase.openOrCreateDatabase(file, null).close();
        try (SQLiteDatabase db = SQLiteDatabase.openDatabase(file.getPath(), null, SQLiteDatabase.OPEN_READONLY)) {
            JSONObject binding = new JSONObject().put("scope", "EXACT_INSTALLED_APK_ASSET")
                    .put("assetPath", INDEX).put("sha256", DIGEST).put("bytes", 4096)
                    .put("apkSha256", APK).put("sourceCommit", SOURCE);
            JSONObject compact = new JSONObject().put("reportProfile", ReportReadContextCP4.COMPLETE_DIAGNOSTIC_PROFILE_R68)
                    .put("sourceIndexAsset", INDEX).put("sourceIndexSha256", DIGEST).put("sourceIndexBytes", 4096)
                    .put("apkSha256", APK).put("sourceCommit", SOURCE).put("sourceIndexReference", binding);
            assertEquals("INSTALLED_APK_ASSET_REFERENCE|asset="+INDEX+"|sha256="+DIGEST+"|bytes=4096|sourceCommit="+SOURCE+"|apkSha256="+APK, read(db, compact));
            for (String key : new String[]{"scope", "assetPath", "sha256", "bytes", "apkSha256", "sourceCommit"}) {
                JSONObject bad = new JSONObject(compact.toString());
                bad.getJSONObject("sourceIndexReference").put(key, "bytes".equals(key) ? 7 : "wrong");
                try { read(db, bad); fail("accepted invalid "+key); }
                catch (ReportReadContextCP4.ReportReadFailureCP4 expected) { assertEquals("REPORT_SOURCE_INDEX_BINDING_MISMATCH", expected.code); }
            }
            compact.remove("sourceIndexReference");
            try { read(db, compact); fail("accepted absent index"); }
            catch (ReportReadContextCP4.ReportReadFailureCP4 expected) { assertEquals("REPORT_ASSET_REFERENCE_MISSING", expected.code); }
            JSONObject raw = new JSONObject().put("assets", new JSONObject().put(INDEX,
                    new JSONObject().put("member", "index.tsv").put("sha256", DIGEST).put("bytes", 4096)));
            String expected = "RAW_ASSET_REFERENCE|asset="+INDEX+"|member=index.tsv|sha256="+DIGEST+"|bytes=4096";
            assertEquals(expected, read(db, raw));
            raw.put("reportProfile", ReportReadContextCP4.RAW_EVIDENCE_PROFILE).put("rawEvidenceIncluded", true);
            assertEquals(expected, read(db, raw));
        } finally { SQLiteDatabase.deleteDatabase(file); }
    }
}
