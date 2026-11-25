package com.ethk3.novaonepro.r342;

import static org.junit.Assert.*;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import androidx.test.platform.app.InstrumentationRegistry;
import com.ethk3.novaonepro.BuildConfig;
import com.ethk3.novaonepro.ServiceHeartbeatStore;
import com.ethk3.novaonepro.strategyv2.JsonR27;
import com.ethk3.novaonepro.strategyv2.R342StartupTests;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.Test;

public final class R342B03ContractTest {
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

    @Test public void test_r342_007() throws Exception {run("R342-007", R342StartupTests::checkpoint);}
    @Test public void test_r342_008() throws Exception {
        run("R342-008", app -> {
            ServiceHeartbeatStore state=new ServiceHeartbeatStore(app);
            state.markActualProcessStart("R342_RUNNING_BUT_WAITING");
            long progress = state.getLearningPumpAt();
            state.markResearchStartupR342("WAIT_DEPENDENCY","CHECKPOINT_GUARD_PENDING");
            state.markLearningAttempt("SCHEDULER_POSTED","NO_COMMIT");
            assertTrue(state.getLearningAttemptAt() > 0);
            assertEquals(progress,state.getLearningPumpAt());
            assertEquals("RESEARCH_OWNER_WAIT_DEPENDENCY:CHECKPOINT_GUARD_PENDING",state.getResearchStaleReason());
        });
    }
    @Test public void test_r342_009() throws Exception {run("R342-009", R342StartupTests::isolatedSchema);}
    @Test public void test_r342_010() throws Exception {run("R342-010", R342StartupTests::taskAdmission);}
    @Test public void test_r342_011() throws Exception {run("R342-011", R342StartupTests::circuit);}
}
