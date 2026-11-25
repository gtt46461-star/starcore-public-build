package com.ethk3.novaonepro.r342;

import static org.junit.Assert.assertTrue;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import androidx.test.platform.app.InstrumentationRegistry;
import com.ethk3.novaonepro.BuildConfig;
import com.ethk3.novaonepro.R342WriterFaultTestCases;
import com.ethk3.novaonepro.strategyv2.JsonR27;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.Test;

public final class R342B02ContractTest {
    private interface Checked { void run(Context context, String phase) throws Exception; }

    private void run(String issue, Checked test) throws Exception {
        Bundle args = InstrumentationRegistry.getArguments();
        Context app = InstrumentationRegistry.getInstrumentation().getTargetContext().getApplicationContext();
        String scope = args.getString("r30Sandbox");
        String phase = args.getString("r342Phase", "normal");
        assertTrue(app.getApplicationInfo().dataDir.contains("/r30-instrumentation/" + scope + "/"));
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("caseId", issue + "-A");
        row.put("sourceCommit", BuildConfig.SOURCE_COMMIT_R26);
        row.put("scope", scope);
        row.put("phase", phase);
        row.put("pid", android.os.Process.myPid());
        row.put("event", "CASE_ENTER");
        Log.i("STARCORE_R342", "R342_INSTRUMENTATION " + JsonR27.write(row));
        test.run(app, phase);
        row.put("event", "CASE_ASSERTIONS_COMPLETE");
        Log.i("STARCORE_R342", "R342_INSTRUMENTATION " + JsonR27.write(row));
    }

    @Test public void test_r342_003() throws Exception {
        run("R342-003", (context, phase) -> R342WriterFaultTestCases.startupOriginal(context));
    }
    @Test public void test_r342_004() throws Exception {
        run("R342-004", R342WriterFaultTestCases::drainAndBackpressure);
    }
    @Test public void test_r342_005() throws Exception {
        run("R342-005", (context, phase) -> R342WriterFaultTestCases.mainProjection(context));
    }
    @Test public void test_r342_006() throws Exception {
        run("R342-006", (context, phase) -> R342WriterFaultTestCases.coldAudit(context));
    }
}
