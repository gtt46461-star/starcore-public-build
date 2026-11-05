package com.ethk3.novaonepro.strategyv2;

import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;

public class R31FaultObservationContractTest {
    static final class Typed extends RuntimeException implements FailureDispositionR31.ClassifiedFailure {
        private final String operation,phase;
        Typed(String operation,String phase){super("same root message");this.operation=operation;this.phase=phase;}
        public String failureCategory(){return "DEPENDENCY";}
        public String operation(){return operation;}
        public String phase(){return phase;}
        public String commitOutcomeName(){return "COMMITTED_DIAGNOSTIC_FAILED";}
    }
    static Map<String,Object> row(Object...pairs){Map<String,Object> m=new LinkedHashMap<>();for(int i=0;i<pairs.length;i+=2)m.put((String)pairs[i],pairs[i+1]);return m;}
    @SuppressWarnings("unchecked") static Map<String,Object> first(Map<String,Object> result){return (Map<String,Object>)((List<?>)result.get("tasks")).get(0);}
    @Test public void declaredTransactionPhaseSurvivesWrapperAndSeparatesAggregation(){
        RuntimeFaultEvidenceR31 a=RuntimeFaultEvidenceR31.capture(new IllegalStateException("wrapper",new Typed("PUBLISH","END_ACK")),"TASK_STEP");
        RuntimeFaultEvidenceR31 b=RuntimeFaultEvidenceR31.capture(new IllegalStateException("wrapper",new Typed("PUBLISH","DIAGNOSTIC")),"TASK_STEP");
        RuntimeFaultEvidenceR31 c=RuntimeFaultEvidenceR31.capture(new IllegalStateException("wrapper",new Typed("SETTLE","END_ACK")),"TASK_STEP");
        assertEquals("END_ACK",a.phase);assertEquals("PUBLISH",a.operation);
        assertEquals("COMMITTED_DIAGNOSTIC_FAILED",a.commitOutcome);
        assertNotEquals(a.fingerprint,b.fingerprint);assertNotEquals(a.fingerprint,c.fingerprint);
        assertNull(RuntimeFaultEvidenceR31.capture(new IllegalStateException("untyped"),"TASK_STEP").phase);
    }
    @Test public void boundedRedactionCannotExposeUserinfoWhenItsTerminatorIsCut(){
        for(int size:new int[]{16,32,64,128,1024}) {
            String input="https://alice:"+String.join("",Collections.nCopies(2000,"synthetic-credential"))+"@example.invalid/path";
            String clean=RuntimeFaultEvidenceR31.clean(input,size);
            assertFalse(clean.contains("alice"));assertFalse(clean.contains("synthetic"));
            assertTrue(clean.getBytes(java.nio.charset.StandardCharsets.UTF_8).length<=size);
        }
        assertFalse(RuntimeFaultEvidenceR31.clean("https://alice:synthetic@example.invalid/path",128).contains("synthetic"));
        assertFalse(RuntimeFaultEvidenceR31.clean("https://用户:密钥密钥密钥密钥密钥@example.invalid/path",32).contains("密钥"));
        assertEquals("ordinary diagnostic",RuntimeFaultEvidenceR31.clean("ordinary diagnostic",128));
    }
    @Test public void eventLinkMatchesBothOwnerAndFenceAndKeepsRawQuarantine(){
        Map<String,Object> task=row("task_id","research","owner","new","fence",7L,"status","QUARANTINED");
        Map<String,Object> old=row("event_id","old-event","task_id","research","owner_epoch","old","fence",6L);
        Map<String,Object> now=row("event_id","current-event","task_id","research","owner_epoch","new","fence",7L);
        Map<String,Object> r=first(RuntimeDiagnosticLinksR31.summarize(Arrays.asList(task),Arrays.asList(old,now)));
        assertEquals("QUARANTINED",r.get("rawSchedulerState"));assertEquals("MATCHED_DISPLAYED_SAMPLES",r.get("currentClaimEvidence"));
        List<?> refs=(List<?>)r.get("faultReferences");assertEquals(2,refs.size());
        assertEquals(false,((Map<?,?>)refs.get(0)).get("currentOwnerAndFenceMatch"));
        assertEquals(true,((Map<?,?>)refs.get(1)).get("currentOwnerAndFenceMatch"));
        assertEquals("current-event",((Map<?,?>)refs.get(1)).get("event_id"));
    }
    @Test public void absentAndUnclaimedSamplesNeverProveNoError(){
        Map<String,Object> task=row("task_id","research","owner","new","fence",7L,"status","RUNNING");
        Map<String,Object> unclaimed=row("event_id","claim-failure","task_id","research","owner_epoch","new","fence",null);
        Map<String,Object> r=first(RuntimeDiagnosticLinksR31.summarize(Arrays.asList(task),Arrays.asList(unclaimed)));
        assertEquals("UNKNOWN_OR_OUTSIDE_SAMPLE",r.get("currentClaimEvidence"));
        assertNull(((Map<?,?>)((List<?>)r.get("faultReferences")).get(0)).get("currentOwnerAndFenceMatch"));
        assertEquals("UNKNOWN_OR_OUTSIDE_SAMPLE",first(RuntimeDiagnosticLinksR31.summarize(Arrays.asList(task),null)).get("currentClaimEvidence"));
        assertEquals(false,RuntimeDiagnosticLinksR31.summarize(null,null).get("taskSourceAvailable"));
    }
    @Test public void diagnosticProjectionHasHardDisplayBoundsWithoutChangingInput(){
        List<Object> tasks=new ArrayList<>(),faults=new ArrayList<>();
        for(int i=0;i<150;i++)tasks.add(row("task_id","research","owner","new","fence",7L,"status","ERROR"));
        for(int i=0;i<50;i++)faults.add(row("event_id","event-"+i,"task_id","research","owner_epoch","new","fence",7L));
        Map<String,Object> r=RuntimeDiagnosticLinksR31.summarize(tasks,faults);
        assertEquals(100,((List<?>)r.get("tasks")).size());assertEquals(20,((List<?>)first(r).get("faultReferences")).size());
        assertEquals(150,tasks.size());assertEquals(50,faults.size());
    }
    @Test public void actualReportTaskChapterExportsFaultsLinksAndIncompleteCounters(){
        Map<String,Object> links=RuntimeDiagnosticLinksR31.summarize(null,null);
        Map<String,Object> telemetry=row("telemetry_incomplete",true,"observationWritesFailed",3L);
        Map<String,Object> data=row("runtimeDiagnosticLinks",links,"r31RuntimeFaults",Arrays.asList(row("event_id","persisted-event")),"r31ProcessTelemetry",telemetry);
        ReportR27.R30Input in=new ReportR27.R30Input(new LinkedHashMap<>(),new LinkedHashMap<>(),data,Collections.emptyList());
        Map<?,?> chapter=ReportR27.chaptersR30(in).get(3);Map<?,?> exported=(Map<?,?>)chapter.get("data");
        assertEquals(links,exported.get("runtimeDiagnosticLinks"));assertEquals(telemetry,exported.get("r31ProcessTelemetry"));
        assertEquals("persisted-event",((Map<?,?>)((List<?>)exported.get("r31RuntimeFaults")).get(0)).get("event_id"));
    }
}
