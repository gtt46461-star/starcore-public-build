package com.ethk3.novaonepro.r342;

import static org.junit.Assert.*;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import androidx.test.platform.app.InstrumentationRegistry;
import com.ethk3.novaonepro.BuildConfig;
import com.ethk3.novaonepro.R342QualificationTestCases;
import com.ethk3.novaonepro.strategyv2.JsonR27;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.Test;

public final class R342B09ContractTest {
    private interface Checked {void run(Context context) throws Exception;}

    private void run(String issue, Checked test) throws Exception {
        Bundle arguments = InstrumentationRegistry.getArguments();
        Context app = InstrumentationRegistry.getInstrumentation().getTargetContext().getApplicationContext();
        String scope = arguments.getString("r30Sandbox");
        assertTrue(app.getApplicationInfo().dataDir.contains("/r30-instrumentation/" + scope + "/"));
        Map<String,Object> row = new LinkedHashMap<>();
        row.put("caseId",issue + "-A");
        row.put("sourceCommit",BuildConfig.SOURCE_COMMIT_R26);
        row.put("scope",scope);
        row.put("phase",arguments.getString("r342Phase","normal"));
        row.put("pid",android.os.Process.myPid());
        row.put("event","CASE_ENTER");
        Log.i("STARCORE_R342","R342_INSTRUMENTATION " + JsonR27.write(row));
        test.run(app);
        row.put("event","CASE_ASSERTIONS_COMPLETE");
        Log.i("STARCORE_R342","R342_INSTRUMENTATION " + JsonR27.write(row));
    }

    @Test public void test_r342_043() throws Exception {run("R342-043", R342QualificationTestCases::scheduledCalibrationFence);}
    @Test public void test_r342_044() throws Exception {run("R342-044", R342QualificationTestCases::settledIdentityFence);}
}
