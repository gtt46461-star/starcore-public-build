package com.ethk3.novaonepro.r341;

import static org.junit.Assert.*;
import android.app.Instrumentation;
import android.content.Context;
import android.util.Log;
import androidx.test.platform.app.InstrumentationRegistry;
import com.ethk3.novaonepro.BuildConfig;
import com.ethk3.novaonepro.R341ObservationTestCases;
import com.ethk3.novaonepro.ReleaseManifestR926;
import com.ethk3.novaonepro.strategyv2.JsonR27;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;

public final class R341B01ContractTest {
    private interface CheckedCase { void run(Context context) throws Exception; }

    private void execute(String number, CheckedCase checks) throws Exception {
        Instrumentation runner = InstrumentationRegistry.getInstrumentation();
        Context app = runner.getTargetContext().getApplicationContext();
        String scope = InstrumentationRegistry.getArguments().getString("r30Sandbox");
        assertTrue(app.getApplicationInfo().dataDir.endsWith("/r30-instrumentation/" + scope + "/" + app.getPackageName()));
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("case_id", "R341-B01-" + number.substring(0, 2) + "-T" + number.substring(2));
        row.put("source_commit", BuildConfig.SOURCE_COMMIT_R26);
        row.put("runner", runner.getClass().getName());
        row.put("selected_class", getClass().getName());
        row.put("selected_method", "test_r341_b01_" + number.substring(0, 2) + "_t" + number.substring(2));
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

    @Test public void test_r341_b01_02_t01() throws Exception {
        execute("0201", ctx -> R341ObservationTestCases.runCohort(ctx, false));
    }

    @Test public void test_r341_b01_02_t02() throws Exception {
        execute("0202", ctx -> R341ObservationTestCases.verifyScopeBoundaries());
    }

    @Test public void test_r341_b01_03_t01() throws Exception {
        execute("0301", ctx -> R341ObservationTestCases.startup(ctx, false));
    }

    @Test public void test_r341_b01_03_t02() throws Exception {
        execute("0302", ctx -> R341ObservationTestCases.startup(ctx, true));
    }

    @Test public void test_r341_b01_04_t01() throws Exception {
        execute("0401", ctx -> {
            R341ObservationTestCases.canonicalObservationDoesNotRefreshWithoutNewCommit(ctx);
            Map<?, ?> counts = (Map<?, ?>) R341ObservationTestCases.runCohort(ctx, false).get("releaseProgress");
            assertEquals(1L, counts.get("confirmedLearningEvents"));
            assertEquals(2L, counts.get("canonicalCommits"));
        });
    }

    @Test public void test_r341_b01_04_t02() throws Exception {
        execute("0402", ctx -> {
            Map<?, ?> counts = (Map<?, ?>) R341ObservationTestCases.runCohort(ctx, false).get("releaseProgress");
            assertEquals(3L, counts.get("recoveryObservations"));
            assertEquals(2L, counts.get("canonicalCommits"));
        });
    }

    @Test public void test_r341_b01_05_t01() throws Exception {
        execute("0501", ctx -> {
            List<?> rows = (List<?>) R341ObservationTestCases.runCohort(ctx, true).get("rows");
            assertEquals(8, rows.size());
            assertEquals("NO_REQUEST", ((Map<?, ?>) rows.get(7)).get("request"));
        });
    }

    @Test public void test_r341_b01_05_t02() throws Exception {
        execute("0502", R341ObservationTestCases::rejectMutableOrForeignSnapshot);
    }
}
