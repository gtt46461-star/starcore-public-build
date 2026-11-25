package com.ethk3.novaonepro.r342;

import static org.junit.Assert.*;

import android.content.Context;
import android.util.Log;
import androidx.test.platform.app.InstrumentationRegistry;
import com.ethk3.novaonepro.BuildConfig;
import com.ethk3.novaonepro.strategyv2.R26ReportDeviceTest;
import com.ethk3.novaonepro.R341ReportTestCases;
import com.ethk3.novaonepro.R342ReportAssertions;
import com.ethk3.novaonepro.strategyv2.JsonR27;
import com.ethk3.novaonepro.strategyv2.R342RepairLedgerChecks;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.Test;

public final class R342B11ContractTest {
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

    @Test public void test_r342_049() throws Exception {run("R342-049",app -> R342RepairLedgerChecks.appendedCommitReadback());}
    @Test public void test_r342_050() throws Exception {run("R342-050",app -> {
        R341ReportTestCases.chartUnknownDoesNotPresentZero(app);
        R341ReportTestCases.tablePageKeepsItsExactDenominator(app);
    });}
    @Test public void test_r342_051() throws Exception {run("R342-051",app -> R342ReportAssertions.certificateLanguage());}
    @Test public void test_r342_052() throws Exception {run("R342-052",app -> R342ReportAssertions.layerEvidenceUnavailable());}
    @Test public void test_r342_053() throws Exception {run("R342-053",app -> R342ReportAssertions.completedChecksDoNotPassBusiness());}
    @Test public void test_r342_054() throws Exception {run("R342-054",app ->
            new R26ReportDeviceTest().allRequiredReportMetricsHaveEvidenceOrExplicitMissingStateAndBoundIdentity());}
    @Test public void test_r342_055() throws Exception {run("R342-055",app -> {
        R342RepairLedgerChecks.scopedMetricComparison();
        R341ReportTestCases.coverageUsesSameSqlWindow(app);
    });}
}
