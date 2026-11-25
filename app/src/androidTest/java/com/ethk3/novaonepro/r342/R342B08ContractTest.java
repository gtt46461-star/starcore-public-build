package com.ethk3.novaonepro.r342;

import static org.junit.Assert.*;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import androidx.test.platform.app.InstrumentationRegistry;
import com.ethk3.novaonepro.BuildConfig;
import com.ethk3.novaonepro.strategyv2.R342LearningEvidenceTests;
import com.ethk3.novaonepro.strategyv2.R341LearningTestCases;
import com.ethk3.novaonepro.strategyv2.R341ResearchTestCases;
import com.ethk3.novaonepro.strategyv2.R335DatasetJournalAndroidTest;
import com.ethk3.novaonepro.strategyv2.R26PublicationDeviceTest;
import com.ethk3.novaonepro.strategyv2.JsonR27;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.Test;

public final class R342B08ContractTest {
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

    @Test public void test_r342_031() throws Exception {run("R342-031", R342LearningEvidenceTests::optimizerParameterTransition);}
    @Test public void test_r342_032() throws Exception {run("R342-032", R341LearningTestCases::labelNeedsMatchingTerminal);}
    @Test public void test_r342_033() throws Exception {run("R342-033", R341LearningTestCases::trainingRoles);}
    @Test public void test_r342_034() throws Exception {run("R342-034", R341ResearchTestCases::fixedPriorsKeepFutureEvidenceOut);}
    @Test public void test_r342_035() throws Exception {run("R342-035", R341ResearchTestCases::matricesHaveIndependentInputs);}
    @Test public void test_r342_036() throws Exception {run("R342-036", R341ResearchTestCases::refusedTensorLeavesOldOriginal);}
    @Test public void test_r342_037() throws Exception {run("R342-037", R341ResearchTestCases::stagedDatasetRestarts);}
    @Test public void test_r342_038() throws Exception {run("R342-038", app -> new R335DatasetJournalAndroidTest().strictlyOutsideRollingChangesSurviveAndKnowledgeTimeCorrectionsInvalidate());}
    @Test public void test_r342_039() throws Exception {run("R342-039", app -> new R26PublicationDeviceTest().failedLookCreatesFreshBoundedResearchAndAtomicCursorWithoutInheritedQuality());}
    @Test public void test_r342_040() throws Exception {run("R342-040", R341ResearchTestCases::onlyActualTopologyEditGetsCounterfactual);}
    @Test public void test_r342_041() throws Exception {run("R342-041", R341ResearchTestCases::selectedProgramsUseOnlyEffectiveInputs);}
    @Test public void test_r342_042() throws Exception {run("R342-042", R342LearningEvidenceTests::sameTypedProbePastOnly);}
}
