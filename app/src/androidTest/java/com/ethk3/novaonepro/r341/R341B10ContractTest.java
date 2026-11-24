package com.ethk3.novaonepro.r341;

import static org.junit.Assert.*;
import android.app.Instrumentation;
import android.content.Context;
import android.util.Log;
import androidx.test.platform.app.InstrumentationRegistry;
import com.ethk3.novaonepro.BuildConfig;
import com.ethk3.novaonepro.R341StorageTestCases;
import com.ethk3.novaonepro.R31StorageProtocolInstrumentedTest;
import com.ethk3.novaonepro.ReportJoinedZipR34Test;
import com.ethk3.novaonepro.ReleaseManifestR926;
import com.ethk3.novaonepro.strategyv2.JsonR27;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.Test;

public final class R341B10ContractTest {
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


    @Test public void test_a_b10_01a() throws Exception {
        execute("A-B10-01a", "test_a_b10_01a", R341StorageTestCases::protectedDebtAndSnapshotAdmission);
    }

    @Test public void test_a_b10_01b() throws Exception {
        execute("A-B10-01b", "test_a_b10_01b", R341StorageTestCases::parallelPhysicalReservation);
    }

    @Test public void test_a_b10_02a() throws Exception {
        execute("A-B10-02a", "test_a_b10_02a", R341StorageTestCases::uniqueReportsSurvive);
    }

    @Test public void test_a_b10_02b() throws Exception {
        execute("A-B10-02b", "test_a_b10_02b", R341StorageTestCases::missingIndexProtectsSnapshot);
    }

    @Test public void test_a_b10_02c() throws Exception {
        execute("A-B10-02c", "test_a_b10_02c", R341StorageTestCases::fetchTraceArchiveGuard);
    }

    @Test public void test_a_b10_03a() throws Exception {
        execute("A-B10-03a", "test_a_b10_03a", R341StorageTestCases::checkpointAuditSeparatesMissingAndVerified);
    }

    @Test public void test_a_b10_03b() throws Exception {
        execute("A-B10-03b", "test_a_b10_03b", R341StorageTestCases::checkpointMigrationDoesNotInventRecovery);
    }

    @Test public void test_a_b10_03c() throws Exception {
        execute("A-B10-03c", "test_a_b10_03c", R341StorageTestCases::checkpointAuditSeparatesMissingAndVerified);
    }

    @Test public void test_a_b10_04a() throws Exception {
        execute("A-B10-04a", "test_a_b10_04a", R341StorageTestCases::historyGapsAndCrossDay);
    }

    @Test public void test_a_b10_04b() throws Exception {
        execute("A-B10-04b", "test_a_b10_04b", R341StorageTestCases::lateHistoryChangesSourceIdentity);
    }

    @Test public void test_a_b10_05a() throws Exception {
        execute("A-B10-05a", "test_a_b10_05a", app -> new R31StorageProtocolInstrumentedTest().productionBlockRequirementAndInjectedSpaceDenialKeepOriginal());
    }

    @Test public void test_a_b10_05b() throws Exception {
        execute("A-B10-05b", "test_a_b10_05b", app -> new ReportJoinedZipR34Test().partsSaveAsOneReopenableZipAndRejectDamagedSourcesAndDestinations());
    }
}
