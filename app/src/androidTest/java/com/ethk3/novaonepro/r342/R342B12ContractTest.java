package com.ethk3.novaonepro.r342;

import static org.junit.Assert.*;

import android.content.Context;
import android.util.Log;
import androidx.test.platform.app.InstrumentationRegistry;
import com.ethk3.novaonepro.BuildConfig;
import com.ethk3.novaonepro.R341ReportTestCases;
import com.ethk3.novaonepro.R342ReportAssertions;
import com.ethk3.novaonepro.R342QualificationTestCases;
import com.ethk3.novaonepro.strategyv2.JsonR27;
import com.ethk3.novaonepro.strategyv2.ReportAssessmentR33;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;

public final class R342B12ContractTest {
    private interface Checked {void run(Context app) throws Exception;}
    private void run(String issue,Checked body) throws Exception {
        var args=InstrumentationRegistry.getArguments();
        Context app=InstrumentationRegistry.getInstrumentation().getTargetContext().getApplicationContext();
        String scope=args.getString("r30Sandbox");
        assertTrue(app.getApplicationInfo().dataDir.contains("/r30-instrumentation/"+scope+"/"));
        Map<String,Object> receipt=new LinkedHashMap<>();
        receipt.put("caseId",issue+"-A");
        receipt.put("sourceCommit",BuildConfig.SOURCE_COMMIT_R26);
        receipt.put("scope",scope);
        receipt.put("phase",args.getString("r342Phase","normal"));
        receipt.put("pid",android.os.Process.myPid());
        receipt.put("event","CASE_ENTER");
        Log.i("STARCORE_R342","R342_INSTRUMENTATION "+JsonR27.write(receipt));
        body.run(app);
        receipt.put("event","CASE_ASSERTIONS_COMPLETE");
        Log.i("STARCORE_R342","R342_INSTRUMENTATION "+JsonR27.write(receipt));
    }

    @Test public void test_r342_056() throws Exception {
        run("R342-056",app -> {
            R342QualificationTestCases.scheduledCalibrationFence(app);
            R342QualificationTestCases.settledIdentityFence(app);
            Map<String,Object> snapshot=ReportAssessmentR33.evaluate(List.of(),List.of());
            assertEquals("INCOMPLETE",snapshot.get("overallAcceptance"));
            assertTrue(((List<?>)((Map<?,?>)snapshot.get("naturalQuality")).get("verifiedMetricIds")).isEmpty());
        });
    }

    @Test public void test_r342_057() throws Exception {
        run("R342-057",app -> {
            R342ReportAssertions.exactReproductionScope(app);
            R341ReportTestCases.fullRawMustContainExactSnapshot(app);
        });
    }
}
