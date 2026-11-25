package com.ethk3.novaonepro;

import static org.junit.Assert.*;

import com.ethk3.novaonepro.strategyv2.CheckMetricCrosswalkR31;
import java.util.LinkedHashMap;
import java.util.Map;

public final class R342ReportAssertions {
    private R342ReportAssertions() {}

    public static void certificateLanguage() {
        String view=AlgorithmLifecycleReportR927.capture(null,false);
        ResearchGatePlanR967.Policy current=ResearchGatePlanR967.installed();
        assertTrue(view.contains("policy_sha256="+current.hash));
        assertTrue(view.contains("consumer_cap="+current.consumerCap));
        for(int size:new int[]{360,600,840,1200})
            assertTrue(view.contains(size+"="+CertificateServiceV2R929.historicalRankingCapR342(size)));
        assertFalse(view.contains("600/840/1200对应12%/20%/30%"));
    }

    public static void layerEvidenceUnavailable() {
        K3FailureDiagnosisEngine engine=new K3FailureDiagnosisEngine();
        var staticDeclaration=engine.artifactLayerSnapshotCP4(null,K3FailureDiagnosisEngine.Layer.RANK,
                true,"fixture declaration","fixture original missing","replay");
        assertEquals(K3FailureDiagnosisEngine.State.WATCH,staticDeclaration.state);
        assertEquals("UNBOUND_RUNTIME_SUMMARY",staticDeclaration.evidenceScope);
        assertEquals("NO_STATISTICAL_CONFIDENCE",staticDeclaration.confidenceKind);
        assertEquals(0d,staticDeclaration.confidence,0d);
    }

    public static void completedChecksDoNotPassBusiness() throws Exception {
        Map<String,Object> checks=new LinkedHashMap<>();
        for(var check:CheckMetricCrosswalkR31.checks())checks.put(check.id, Map.of(
                "execution_state","EXECUTED","domain_outcome","OBSERVED_EMPTY","health","UNKNOWN"));
        Map<String,Object> overview=ReportSemanticVerifierR31.assessChecksR342(checks);
        assertEquals(96,overview.get("registeredCheckDenominator"));
        assertEquals(96,((Map<?,?>)overview.get("execution")).get("EXECUTED"));
        assertEquals(96,((Map<?,?>)overview.get("evidenceConclusion")).get("OBSERVED_EMPTY"));
        assertEquals(96,((Map<?,?>)overview.get("affectedMetricIdsByCheck")).size());
        assertEquals("INDEPENDENT_METRIC_AND_FIELD_RECEIPTS_REQUIRED",overview.get("businessAcceptance"));
    }
}
