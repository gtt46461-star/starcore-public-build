package com.ethk3.novaonepro.r341;

import static org.junit.Assert.*;
import android.app.Instrumentation;
import android.content.Context;
import android.util.Log;
import androidx.test.platform.app.InstrumentationRegistry;
import com.ethk3.novaonepro.BuildConfig;
import com.ethk3.novaonepro.strategyv2.R341ResearchTestCases;
import com.ethk3.novaonepro.strategyv2.R335DatasetJournalAndroidTest;
import com.ethk3.novaonepro.strategyv2.R335CursorRecoveryPublicationTest;
import com.ethk3.novaonepro.strategyv2.SchemaAndLiveSU1Test;
import com.ethk3.novaonepro.strategyv2.R335AppliedBudgetTableTest;
import com.ethk3.novaonepro.strategyv2.R26PublicationDeviceTest;
import com.ethk3.novaonepro.ReleaseManifestR926;
import com.ethk3.novaonepro.strategyv2.JsonR27;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.Test;

public final class R341B08ContractTest {
    private interface CheckedCase { void run(Context context) throws Exception; }

    private void execute(String number, CheckedCase checks) throws Exception {
        Instrumentation runner = InstrumentationRegistry.getInstrumentation();
        Context app = runner.getTargetContext().getApplicationContext();
        String scope = InstrumentationRegistry.getArguments().getString("r30Sandbox");
        assertTrue(app.getApplicationInfo().dataDir.endsWith("/r30-instrumentation/" + scope + "/" + app.getPackageName()));
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("case_id", "R341-B08-" + number.substring(0, 2) + "-A" + number.substring(2));
        row.put("source_commit", BuildConfig.SOURCE_COMMIT_R26);
        row.put("runner", runner.getClass().getName());
        row.put("selected_class", getClass().getName());
        row.put("selected_method", "test_r341_b08_" + number.substring(0, 2) + "_a" + number.substring(2));
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

    @Test public void test_r341_b08_01_a01() throws Exception {
        execute("0101", R341ResearchTestCases::stagedDatasetRestarts);
    }

    @Test public void test_r341_b08_01_a02() throws Exception {
        execute("0102", app -> new R335DatasetJournalAndroidTest().strictlyOutsideRollingChangesSurviveAndKnowledgeTimeCorrectionsInvalidate());
    }

    @Test public void test_r341_b08_01_a03() throws Exception {
        execute("0103", R341ResearchTestCases::matricesHaveIndependentInputs);
    }

    @Test public void test_r341_b08_02_a01() throws Exception {
        execute("0201", R341ResearchTestCases::chartsRetainSpecificMissingSources);
    }

    @Test public void test_r341_b08_02_a02() throws Exception {
        execute("0202", R341ResearchTestCases::isolateOnlyViewOwnedErrors);
    }

    @Test public void test_r341_b08_02_a03() throws Exception {
        execute("0203", R341ResearchTestCases::formalReadOnlyEvidenceCannotTriggerAction);
    }

    @Test public void test_r341_b08_03_a01() throws Exception {
        execute("0301", R341ResearchTestCases::matricesHaveIndependentInputs);
    }

    @Test public void test_r341_b08_03_a02() throws Exception {
        execute("0302", R341ResearchTestCases::refusedTensorLeavesOldOriginal);
    }

    @Test public void test_r341_b08_03_a03() throws Exception {
        execute("0303", R341ResearchTestCases::onlyActualTopologyEditGetsCounterfactual);
    }

    @Test public void test_r341_b08_04_a01() throws Exception {
        execute("0401", app -> new R26PublicationDeviceTest().failedLookCreatesFreshBoundedResearchAndAtomicCursorWithoutInheritedQuality());
    }

    @Test public void test_r341_b08_04_a02() throws Exception {
        execute("0402", app -> new R335CursorRecoveryPublicationTest().recoveryOriginsRemainAtomicAndPinsKeepTheirSourceFences());
    }

    @Test public void test_r341_b08_04_a03() throws Exception {
        execute("0403", app -> new R335AppliedBudgetTableTest().originalOldAndCurrentBudgetRowsReopenBesideTheR16Frame());
    }

    @Test public void test_r341_b08_05_a01() throws Exception {
        execute("0501", R341ResearchTestCases::onlyActualTopologyEditGetsCounterfactual);
    }

    @Test public void test_r341_b08_05_a02() throws Exception {
        execute("0502", app -> new R26PublicationDeviceTest().candidateSchedulingUsesMeasuredCpuAndCorruptPeerIsQuarantinedAlone());
    }

    @Test public void test_r341_b08_05_a03() throws Exception {
        execute("0503", R341ResearchTestCases::formalReadOnlyEvidenceCannotTriggerAction);
    }

    @Test public void test_r341_b08_06_a01() throws Exception {
        execute("0601", R341ResearchTestCases::selectedProgramsUseOnlyEffectiveInputs);
    }

    @Test public void test_r341_b08_06_a02() throws Exception {
        execute("0602", R341ResearchTestCases::selectedProgramsUseOnlyEffectiveInputs);
    }

    @Test public void test_r341_b08_06_a03() throws Exception {
        execute("0603", app -> new SchemaAndLiveSU1Test().fittedGraphParametersArePublishedAndActuallyConsumedWithoutInheritedEvidence());
    }

    @Test public void test_r341_b08_07_a01() throws Exception {
        execute("0701", R341ResearchTestCases::fixedPriorsKeepFutureEvidenceOut);
    }

    @Test public void test_r341_b08_07_a02() throws Exception {
        execute("0702", R341ResearchTestCases::fixedPriorsKeepFutureEvidenceOut);
    }

    @Test public void test_r341_b08_07_a03() throws Exception {
        execute("0703", R341ResearchTestCases::fixedPriorsKeepFutureEvidenceOut);
    }

}
