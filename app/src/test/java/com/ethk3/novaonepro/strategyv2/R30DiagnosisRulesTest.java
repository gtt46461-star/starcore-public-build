package com.ethk3.novaonepro.strategyv2;
import java.util.*;
import java.lang.reflect.Method;
import org.junit.Test;
import static org.junit.Assert.*;
public final class R30DiagnosisRulesTest {
    @SuppressWarnings("unchecked") static List<Map<String,Object>> findings(Map<String,Object> report){return(List<Map<String,Object>>)report.get("findings");}
    static Map<String,Object> find(Map<String,Object> report,String id){for(Map<String,Object> row:findings(report))if(id.equals(row.get("id")))return row;throw new AssertionError(id);}
    @Test public void missingAndInvalidCountsProduceUnknownWithConcreteCollectionActions(){
        Map<String,Object> facts=new HashMap<>();facts.put("writerHardViolations",Double.NaN);facts.put("receiptStateMismatches",-1L);facts.put("modelCalls",.5);
        Map<String,Object> report=DiagnosisRulesR30.evaluate(facts,Collections.emptyMap(),Collections.emptyMap(),Collections.emptyList());
        assertEquals(DiagnosisRulesR30.rules().size(),report.get("unknown"));assertEquals(1.0,(Double)report.get("actionCoverage"),0);
        for(Map<String,Object> row:findings(report)){assertEquals("UNKNOWN",row.get("status"));assertNotNull(row.get("collectionAction"));Map<?,?> action=(Map<?,?>)row.get("action");assertTrue(action.get("code").toString().contains(".java:"));assertEquals(row.get("collectionAction"),action.get("instruction"));assertNotNull(action.get("card"));assertNotNull(action.get("verification"));}
    }
    @Test public void actualConditionsFindDefectsWithoutPromotingRecordCountsToQuality(){
        Map<String,Object> facts=new HashMap<>();facts.put("writerHardViolations",3L);facts.put("receiptStateMismatches",2L);facts.put("unsafeDecisionMismatches",1L);facts.put("pitViolations",4L);facts.put("calibratedPolicies",40L);facts.put("independentFutureIssues",10000L);facts.put("modelCheckpoints",0L);facts.put("openQualificationConflicts",0L);
        Map<String,Object> mark=Collections.singletonMap("factsReadAt",123L);
        Map<String,Object> report=DiagnosisRulesR30.evaluate(facts,Collections.emptyMap(),mark,Collections.emptyList());
        assertEquals("DETECTED",find(report,"WRITER_HARD").get("status"));assertEquals("DETECTED",find(report,"RECEIPT_STATE").get("status"));assertEquals("DETECTED",find(report,"UNSAFE_DECISION").get("status"));assertEquals("REVIEW_GUARD_REJECTIONS",find(report,"PIT_REJECTIONS").get("status"));
        assertEquals("RECORDED_UNVALIDATED",find(report,"CALIBRATION").get("status"));assertEquals("RECORDED_UNVALIDATED",find(report,"NATURAL_SAMPLE").get("status"));assertEquals("INSUFFICIENT",find(report,"MODEL_STATE").get("status"));assertEquals("CLEAR_IN_CAPTURED_SCOPE",find(report,"QUAL_CONFLICT").get("status"));assertFalse(find(report,"QUAL_CONFLICT").get("conclusion").toString().contains("仍有未解决"));
        assertSame(mark,((Map<?,?>)find(report,"WRITER_HARD").get("evidence")).get("snapshotWatermark"));
    }
    @Test public void unknownTasksAndCaptureFailureCannotBecomeHealthy(){
        Map<String,Object> live=new HashMap<>();Map<String,Object> summary=new HashMap<>();summary.put("m12",null);live.put("summary",summary);
        Map<String,Object> data=Collections.singletonMap("taskLiveness",live);
        Map<String,Object> report=DiagnosisRulesR30.evaluate(Collections.emptyMap(),data,Collections.emptyMap(),Collections.singletonList("SQLITE_CAPTURE_FAILED"));
        assertEquals("UNKNOWN",find(report,"TASK_STALLED").get("status"));assertEquals("DETECTED",find(report,"SNAPSHOT_CAPTURE").get("status"));
        summary.put("m12",2);report=DiagnosisRulesR30.evaluate(Collections.emptyMap(),data,Collections.emptyMap(),Collections.emptyList());assertEquals("DETECTED",find(report,"TASK_STALLED").get("status"));
        summary.put("m12",0);report=DiagnosisRulesR30.evaluate(Collections.emptyMap(),data,Collections.emptyMap(),Collections.emptyList());assertEquals("CLEAR_IN_CAPTURED_SCOPE",find(report,"TASK_STALLED").get("status"));
    }
    @Test public void everyPrescriptionNamesAnExistingOwnerMethodAndValidation() throws Exception {
        Set<String> ids=new HashSet<>();for(DiagnosisRulesR30.Rule r:DiagnosisRulesR30.rules()){
            assertTrue(ids.add(r.id));assertFalse(r.repair.isEmpty());assertFalse(r.collect.isEmpty());assertFalse(r.verification.isEmpty());assertTrue(r.card.startsWith("R30-B"));
            String owner=r.file.substring("app/src/main/java/".length(),r.file.length()-5).replace('/','.');
            Class<?> c=Class.forName(owner,false,getClass().getClassLoader());boolean exists=false;for(Method m:c.getDeclaredMethods())exists|=m.getName().equals(r.method);assertTrue(r.file+":"+r.method,exists);
        }
    }
}
