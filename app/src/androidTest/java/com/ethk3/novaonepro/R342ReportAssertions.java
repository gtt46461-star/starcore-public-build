package com.ethk3.novaonepro;

import static org.junit.Assert.*;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.os.CancellationSignal;
import com.ethk3.novaonepro.strategyv2.CheckMetricCrosswalkR31;
import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;
import org.json.JSONObject;

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

    public static void exactReproductionScope(Context app) throws Exception {
        File input=new File(app.getCacheDir(),"r342-reproduction-snapshot.db");
        try(SQLiteDatabase seed=SQLiteDatabase.openOrCreateDatabase(input,null)){
            seed.execSQL("CREATE TABLE IF NOT EXISTS original_fixture(issue TEXT PRIMARY KEY)");
        }
        String hash=ReportArchiveCP4.hash(input,()->true), source=BuildConfig.SOURCE_COMMIT_R26;
        for(boolean compact:new boolean[]{true,false}){
            JSONObject manifest=new JSONObject()
                    .put("sourceCommit",source).put("snapshotSha256",hash)
                    .put("reportProfile",ReportAttachmentClosureCP12.reportProfileR80(compact))
                    .put("rawEvidenceIncluded",!compact);
            JSONObject coverage=new JSONObject().put("sourceCommit",source).put("snapshotSha256",hash)
                    .put("includesSqlite",!compact).put("includesInstalledApk",!compact);
            manifest.put("reproductionR342",coverage);
            assertEquals(compact,readReportProfile(input,hash,manifest));
            coverage.put("includesInstalledApk",compact);
            ReportReadContextCP4.ReportReadFailureCP4 failure=assertThrows(
                    ReportReadContextCP4.ReportReadFailureCP4.class,()->readReportProfile(input,hash,manifest));
            assertEquals("REPORT_REPRODUCTION_SCOPE_CONFLICT",failure.code);
        }
    }

    private static boolean readReportProfile(File file,String hash,JSONObject manifest) throws Exception {
        try (SQLiteDatabase snapshot=SQLiteDatabase.openDatabase(file.getPath(),null,SQLiteDatabase.OPEN_READONLY);
             ReportReadContextCP4 context=new ReportReadContextCP4(snapshot,"R342_PROFILE",hash,
                     System.currentTimeMillis(),new ReportReadContextCP4.RuntimeObservationCP4("{}"),
                     new ReportReadContextCP4.SourceManifestCP4(manifest.toString()),new CancellationSignal(),8L<<20)) {
            return context.compactDiagnosticR64();
        }
    }

    public static void largeComparisonSummaryRetainsOriginals(Context app)throws Exception {
        File output=new File(app.getCacheDir().getCanonicalFile(),"R30_SUCCESSFUL_SUMMARY_CANDIDATE.json");
        java.util.List<Object> trace=new java.util.ArrayList<>();
        for(int i=0;i<144;i++){
            trace.add(Map.of("objectId","R342-"+i,"observationScopeR341",
                    Map.of("sourceCommit",BuildConfig.SOURCE_COMMIT_R26,"windowDefinition","赛前证据范围".repeat(42))));
        }
        Map<String,Object> payload=Map.of("objects",trace,"jobId","fixture-"+"1".repeat(64));
        String original=com.ethk3.novaonepro.strategyv2.JsonR27.write(payload);
        assertTrue(original.getBytes(java.nio.charset.StandardCharsets.UTF_8).length>ReportJobJournalCP12.MAX_JSON);
        assertThrows(java.io.IOException.class,()->ReportJobJournalCP12.writeAtomic(output,original));
        ReportJsonWriterR31.writeReportArtifactAtomic(output,payload);
        assertEquals(original,ReportJobJournalCP12.readAtomic(output,1024*1024));
        assertThrows(IllegalArgumentException.class,
                ()->ReportJsonWriterR31.writeReportArtifactAtomic(output,Map.of("invalid",new Object())));
        assertEquals(original,ReportJobJournalCP12.readAtomic(output,1024*1024));
    }
}
