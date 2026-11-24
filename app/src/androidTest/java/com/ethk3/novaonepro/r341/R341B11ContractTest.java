package com.ethk3.novaonepro.r341;

import static org.junit.Assert.*;
import android.app.Instrumentation;
import android.content.Context;
import android.util.Log;
import androidx.test.platform.app.InstrumentationRegistry;
import com.ethk3.novaonepro.BuildConfig;
import com.ethk3.novaonepro.R341ReportTestCases;
import com.ethk3.novaonepro.R30ReportSnapshotAndroidTest;
import com.ethk3.novaonepro.strategyv2.R26ReportDeviceTest;
import com.ethk3.novaonepro.ReportJoinedZipR34Test;
import com.ethk3.novaonepro.ReleaseManifestR926;
import com.ethk3.novaonepro.strategyv2.JsonR27;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.Test;

public final class R341B11ContractTest {
    private interface CheckedCase { void run(Context context) throws Exception; }

    private void execute(String id, String method, CheckedCase checks) throws Exception {
        Instrumentation runner = InstrumentationRegistry.getInstrumentation();
        Context app = runner.getTargetContext().getApplicationContext();
        String scope = InstrumentationRegistry.getArguments().getString("r30Sandbox");
        assertTrue(app.getApplicationInfo().dataDir.endsWith("/r30-instrumentation/" + scope + "/" + app.getPackageName()));
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("case_id", id);
        row.put("source_commit", BuildConfig.SOURCE_COMMIT_R26);
        row.put("runner", runner.getClass().getName());
        row.put("selected_class", getClass().getName());
        row.put("selected_method", method);
        row.put("r30Sandbox", scope);
        row.put("r31qRealService", false);
        row.put("target_data_dir", app.getApplicationInfo().dataDir);
        row.put("package", ReleaseManifestR926.APPLICATION_ID);
        row.put("event", "CASE_ENTER");
        Log.i("STARCORE_R341", "R341_INSTRUMENTATION " + JsonR27.write(row));
        checks.run(app);
        row.put("event", "CASE_ASSERTIONS_COMPLETE");
        Log.i("STARCORE_R341", "R341_INSTRUMENTATION " + JsonR27.write(row));
    }



    @Test public void test_a_b11_01a() throws Exception {
        execute("A-B11-01a", "test_a_b11_01a", R341ReportTestCases::missingSourcesReachOverview);
    }

    @Test public void test_a_b11_01b() throws Exception {
        execute("A-B11-01b", "test_a_b11_01b", R341ReportTestCases::sourceReasonsStayDistinct);
    }

    @Test public void test_a_b11_02a() throws Exception {
        execute("A-B11-02a", "test_a_b11_02a", app -> new R26ReportDeviceTest().allRequiredReportMetricsHaveEvidenceOrExplicitMissingStateAndBoundIdentity());
    }

    @Test public void test_a_b11_02b() throws Exception {
        execute("A-B11-02b", "test_a_b11_02b", R341ReportTestCases::coverageUsesSameSqlWindow);
    }

    @Test public void test_a_b11_03a() throws Exception {
        execute("A-B11-03a", "test_a_b11_03a", R341ReportTestCases::missingSourcesReachOverview);
    }

    @Test public void test_a_b11_03b() throws Exception {
        execute("A-B11-03b", "test_a_b11_03b", app -> new R26ReportDeviceTest().repeatedExpertHistoryCannotHideAnUnservedRegisteredExpert());
    }

    @Test public void test_a_b11_04a() throws Exception {
        execute("A-B11-04a", "test_a_b11_04a", R341ReportTestCases::chartUnknownDoesNotPresentZero);
    }

    @Test public void test_a_b11_04b() throws Exception {
        execute("A-B11-04b", "test_a_b11_04b", R341ReportTestCases::tablePageKeepsItsExactDenominator);
    }

    @Test public void test_a_b11_05a() throws Exception {
        execute("A-B11-05a", "test_a_b11_05a", R341ReportTestCases::effectMigrationDoesNotPrepopulatePass);
    }

    @Test public void test_a_b11_05b() throws Exception {
        execute("A-B11-05b", "test_a_b11_05b", R341ReportTestCases::effectMigrationDoesNotPrepopulatePass);
    }

    @Test public void test_a_b11_06a() throws Exception {
        execute("A-B11-06a", "test_a_b11_06a", app -> new R30ReportSnapshotAndroidTest().twelveChaptersKeepOneViewWhileTheWriterAdvances());
    }

    @Test public void test_a_b11_06b() throws Exception {
        execute("A-B11-06b", "test_a_b11_06b", app -> {
            R341ReportTestCases.fullRawMustContainExactSnapshot(app);
            ReportJoinedZipR34Test.runPartsFixture(java.nio.file.Files.createTempDirectory(app.getCacheDir().toPath(),"r341-zip-readback-").toFile());
        });
    }
}
