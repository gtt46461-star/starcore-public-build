package com.ethk3.novaonepro.strategyv2;

import java.util.*;
import org.junit.Test;
import com.ethk3.novaonepro.ReportJobJournalCP12;
import com.ethk3.novaonepro.ReportSemanticVerifierR31;
import static org.junit.Assert.*;

/** Real production input -> chapter objects -> final document. Filesystem/ART
 * delivery is verified separately; these tests never claim device or quality. */
public final class R31ProductionReportTest {
    static Map<String,Object> map(Object... fields){Map<String,Object> out=new LinkedHashMap<>();for(int i=0;i<fields.length;i+=2)out.put((String)fields[i],fields[i+1]);return out;}
    static ReportR27.R30Input input(List<Object> sources){return new ReportR27.R30Input(map(),map("factsReadAt",123L,"latestIssue","202611120001","sourceCommit","source"),map("crossSourceObservations",sources),Collections.emptyList());}
    static Map<String,Object> source(String id,String role,String water,Object value){return map("source",id,"owner",id+"Owner","role",role,"scope","frozenCount","capturedAt",123L,"watermark",water,"value",value);}
    static Map<String,Object> metric(ReportR27.R30Input input,String id){for(Map<String,Object> row:input.metricResults)if(id.equals(row.get("metricId")))return row;throw new AssertionError(id);}
    @Test public void missingCrossSourceFactsNeverBecomeZero(){
        ReportR27.R30Input input=input(Collections.emptyList());Map<String,Object> m=metric(input,"M32");assertNull(m.get("value"));assertEquals("UNKNOWN_SOURCE_MISSING",m.get("status"));assertEquals(0L,m.get("denominator"));
    }
    @Test public void actualContradictionReachesProductionDocument(){
        ReportR27.R30Input input=input(Arrays.asList(source("sql","ENGINEERING_RUNTIME","same",1L),source("cache","ENGINEERING_RUNTIME","same",2L)));
        Map<String,Object> result=metric(input,"M32");assertEquals("DATA_CONFLICT",result.get("status"));assertEquals(1,((Number)result.get("value")).intValue());
        List<Map<String,Object>> chapters=ReportR29.fromSnapshotR30(input);Map<String,Object> document=ReportR29.document(chapters);
        assertSame(input.metricResults,document.get("metricResults"));assertEquals(12,chapters.size());assertEquals(44,input.metricResults.size());assertEquals(MetricDefinitionsR29.R31_VERSION,document.get("metricRegistryVersion"));
        String encoded=JsonR27.write(document);assertTrue(encoded.contains("VALUE_CONFLICT_SAME_SCOPE_AND_WATERMARK"));assertTrue(encoded.contains("r31Definition"));assertTrue(encoded.contains("r31Evaluation"));assertTrue(encoded.contains("supportingChecks"));
        for(Map<String,Object> row:input.metricResults){assertNotNull(row.get("r31Definition"));assertNotNull(row.get("r31Evaluation"));assertEquals(MetricDefinitionsR29.R31_VERSION,row.get("definitionVersion"));}
    }
    @Test public void otherRolesAndCaptureEpochsAreNotNumericConflicts(){
        ReportR27.R30Input input=input(Arrays.asList(source("sql","ENGINEERING_RUNTIME","current",1L),source("old","ENGINEERING_RUNTIME","old",9L),source("replay","REPLAY","current",4L)));
        Map<String,Object> m=metric(input,"M32");assertNull(m.get("value"));assertEquals("UNKNOWN_SOURCE_MISSING",m.get("status"));assertEquals("STALE_OR_SCOPE_CONFLICT",m.get("crossSourceState"));
    }
    @Test public void lifecycleDoesNotTurnAStoredFrameIntoAnAdvantage(){
        Map<String,Object> index=ReportObjectIndexR30.buildProductionR31(map("researchProduced:R01",1L),map("sourceCommit","current"),map());
        assertEquals(29,((Number)((Map<?,?>)index.get("legacyCharts")).get("expected")).intValue());assertEquals(20,((Number)((Map<?,?>)index.get("matrices")).get("expected")).intValue());assertEquals(16,((Number)((Map<?,?>)index.get("logicalTables")).get("expected")).intValue());
        for(Object raw:(List<?>)index.get("lifecycleEntriesR31")){Map<?,?> row=(Map<?,?>)raw;assertEquals("NOT_PASS",row.get("status"));}
    }
    @Test public void everyCapturedProductionSourceAndCurrentRepairActionReachTheFinalReport(){
        Map<String,Object> sources=map("r31FeedbackEvents",Arrays.asList(map("event_id","actual-event")),
                "r31FetchTiming",Arrays.asList(map("request_id","actual-request")),
                "r31TaskControls",Arrays.asList(map("task_id","actual-task")),
                "futureSourceAddedLater",map("status","UNKNOWN_WITH_ORIGINAL_REASON"));
        ReportR27.R30Input input=new ReportR27.R30Input(map("draws",12L),map("factsReadAt",123L),sources,Arrays.asList("SOURCE_UNAVAILABLE:fixture"));
        List<Map<String,Object>> chapters=ReportR29.fromSnapshotR30(input);Map<String,Object> doc=ReportR29.document(chapters);
        Map<?,?> complete=(Map<?,?>)chapters.get(11).get("completeSnapshotEvidenceR31");
        assertSame(input.data,complete.get("sources"));assertSame(input.facts,complete.get("facts"));
        assertEquals(input.errors,complete.get("sourceErrors"));assertEquals(input.watermark,complete.get("snapshotWatermark"));
        String json=JsonR27.write(doc),readable=ReportR27.markdownR30(chapters);
        for(String key:sources.keySet()){assertTrue(json.contains(key));assertTrue(readable.contains(key));}
        Map<?,?> diagnosis=(Map<?,?>)doc.get("diagnosis");
        for(Object raw:(List<?>)diagnosis.get("findings")){
            Map<?,?> action=(Map<?,?>)((Map<?,?>)raw).get("action");
            assertEquals("STARCORE_R31_UNIFIED_FINAL_20261112",action.get("currentPlan"));
            assertFalse(((List<?>)action.get("currentRepairCards")).isEmpty());
        }
    }
    @Test public void displayFrameChecksActualAbiHashAndFreshness(){
        Map<String,Object> frame=map("points",Collections.emptyList(),"status","READY");ChartCacheR27.sealR31(frame,"issue",1000L);
        assertNull(ChartCacheR27.validateR31(frame,"issue",1000L,2000L));assertEquals("STALE_SOURCE_WATERMARK",ChartCacheR27.validateR31(frame,"next",1000L,2000L));
        assertEquals("STALE_SOURCE_WATERMARK",ChartCacheR27.validateR31(frame,"issue",1000L,100000L));
        frame.put("status","FALSIFIED_READY");assertEquals("READ_ERROR_FRAME_HASH",ChartCacheR27.validateR31(frame,"issue",1000L,2000L));
        ChartCacheR27.sealR31(frame,"issue",1000L);frame.put("frameSchema","old");assertEquals("STALE_FRAME_ABI",ChartCacheR27.validateR31(frame,"issue",1000L,2000L));
    }
    @Test public void deliveryContractRequiresEveryObjectAndTheExactJobSnapshot()throws Exception {
        String id=StatsR27.sha256("semantic-job"),sha=StatsR27.sha256("semantic-snapshot");
        ReportJobJournalCP12.Job job=new ReportJobJournalCP12.Job(id,id,StatsR27.sha256("release"),"FULL_REPORT",1L);job.snapshotSha=sha;
        Map<String,Object> watermark=map("reportJobId",id,"sourceManifest",map("snapshotSha256",sha));
        Map<String,Object> data=map();List<Object> caps=new ArrayList<>(),tables=new ArrayList<>();
        for(CapabilityRegistryR30.Definition d:CapabilityRegistryR30.all())caps.add(d.json());
        for(String table:ResearchTableAdaptersR30R1.ids())tables.add(map("id",table,"status","NOT_RUN"));
        data.put("capabilities",caps);data.put("researchTablesR31",tables);data.put("objectIndex",ReportObjectIndexR30.buildProductionR31(map(),watermark,data));
        ReportR27.R30Input input=new ReportR27.R30Input(map(),watermark,data,Collections.emptyList());
        Map<String,Object> doc=ReportR29.document(ReportR29.fromSnapshotR30(input));ReportSemanticVerifierR31.validateDocument(doc,job);
        Map<String,Object> duplicate=new LinkedHashMap<>(doc);
        List<Object> chapterCopies=new ArrayList<>((List<?>)doc.get("chapters"));chapterCopies.set(11,chapterCopies.get(0));duplicate.put("chapters",chapterCopies);
        try{ReportSemanticVerifierR31.validateDocument(duplicate,job);fail("twelve entries with a duplicate chapter are incomplete");}catch(java.io.IOException expected){assertTrue(expected.getMessage().contains("CHAPTER_DUPLICATE_OR_MISSING_ID"));}
        duplicate=new LinkedHashMap<>(doc);List<Object> tableCopies=new ArrayList<>((List<?>)doc.get("researchTables"));tableCopies.set(15,tableCopies.get(0));duplicate.put("researchTables",tableCopies);
        try{ReportSemanticVerifierR31.validateDocument(duplicate,job);fail("sixteen entries with a missing table are incomplete");}catch(java.io.IOException expected){assertTrue(expected.getMessage().contains("TABLE_READER_DUPLICATE_OR_MISSING_ID"));}
        duplicate=new LinkedHashMap<>(doc);Map<String,Object> objectCopies=new LinkedHashMap<>((Map<String,Object>)doc.get("objectIndex"));Map<String,Object> categoryCopy=new LinkedHashMap<>((Map<String,Object>)objectCopies.get("capabilities"));
        List<Object> capabilityCopies=new ArrayList<>((List<?>)categoryCopy.get("objects"));capabilityCopies.set(81,capabilityCopies.get(0));categoryCopy.put("objects",capabilityCopies);objectCopies.put("capabilities",categoryCopy);duplicate.put("objectIndex",objectCopies);
        try{ReportSemanticVerifierR31.validateDocument(duplicate,job);fail("same capability count must not hide a missing expert");}catch(java.io.IOException expected){assertTrue(expected.getMessage().contains("OBJECT:capabilities_DUPLICATE_OR_MISSING_ID"));}
        job.snapshotSha=StatsR27.sha256("other-snapshot");
        try{ReportSemanticVerifierR31.validateDocument(doc,job);fail("different backup must not share a receipt");}catch(java.io.IOException expected){assertTrue(expected.getMessage().contains("SNAPSHOT_ID"));}
        job.snapshotSha=sha;doc.put("researchTables",tables.subList(0,15));
        try{ReportSemanticVerifierR31.validateDocument(doc,job);fail("missing table reader must not close the report");}catch(java.io.IOException expected){assertTrue(expected.getMessage().contains("TABLE_READ_RECEIPTS"));}
    }
}
