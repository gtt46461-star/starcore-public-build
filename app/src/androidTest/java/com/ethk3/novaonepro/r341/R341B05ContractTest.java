package com.ethk3.novaonepro.r341;

import static org.junit.Assert.*;
import android.app.Instrumentation;
import android.content.Context;
import android.util.Log;
import androidx.test.platform.app.InstrumentationRegistry;
import com.ethk3.novaonepro.BuildConfig;
import com.ethk3.novaonepro.R341BackgroundTestCases;
import com.ethk3.novaonepro.ReleaseManifestR926;
import com.ethk3.novaonepro.strategyv2.JsonR27;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.Test;

public final class R341B05ContractTest {
    private interface CheckedCase { void run(Context context) throws Exception; }

    private void execute(String number, CheckedCase checks) throws Exception {
        Instrumentation runner = InstrumentationRegistry.getInstrumentation();
        Context app = runner.getTargetContext().getApplicationContext();
        String scope = InstrumentationRegistry.getArguments().getString("r30Sandbox");
        assertTrue(app.getApplicationInfo().dataDir.endsWith("/r30-instrumentation/" + scope + "/" + app.getPackageName()));
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("case_id", "R341-B05-" + number.substring(0, 2) + "-T" + number.substring(2));
        row.put("source_commit", BuildConfig.SOURCE_COMMIT_R26);
        row.put("runner", runner.getClass().getName());
        row.put("selected_class", getClass().getName());
        row.put("selected_method", "test_r341_b05_" + number.substring(0, 2) + "_t" + number.substring(2));
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

    @Test public void test_r341_b05_01_t01() throws Exception {
        execute("0101", R341BackgroundTestCases::durableRequestAndSettlement);
    }

    @Test public void test_r341_b05_01_t02() throws Exception {
        execute("0102", R341BackgroundTestCases::generationRejectsCancelledOwner);
    }

    @Test public void test_r341_b05_02_t01() throws Exception {
        execute("0201", R341BackgroundTestCases::userStopBlocksDelivery);
    }

    @Test public void test_r341_b05_02_t02() throws Exception {
        execute("0202", R341BackgroundTestCases::undeliveredThirdAttemptAndNewBoot);
    }

    @Test public void test_r341_b05_03_t01() throws Exception {
        execute("0301", com.ethk3.novaonepro.strategyv2.R341TimingFixture::independentLimitsAndUnknowns);
    }

    @Test public void test_r341_b05_04_t01() throws Exception {
        execute("0401", R341BackgroundTestCases::noReconcileFromDisabledUiLifecycle);
    }

    @Test public void test_r341_b05_04_t02() throws Exception {
        execute("0402", R341BackgroundTestCases::stoppedStateDoesNotRepresentBusinessProgress);
    }

}
