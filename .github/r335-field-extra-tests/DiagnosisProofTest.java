package com.ethk3.novaonepro.strategyv2;
import java.util.*;
public class DiagnosisProofTest {
    static void check(String expected, Map<String,Object> m) {
        String actual=DiagnosisProofR33.classify(m,"researchCharts","R06","033test",100L);
        if (!expected.equals(actual)) throw new AssertionError(expected+" != "+actual);
    }
    public static void main(String[] args) {
        Map<String,Object> m=new HashMap<>();
        check("UNCONFIRMED_REPLAY_REQUIRED",m);
        m.put("group","researchCharts");m.put("objectId","R06");
        m.put("readWatermark",101L);m.put("sourceCommit","033test");
        check("TRACE_IDENTITY_MISMATCH",m);m.put("readWatermark",100L);
        check("UNCONFIRMED_ORIGINAL_OR_INPUT_MISSING",m);
        String h="1".repeat(64), o="2".repeat(64);
        m.put("recordLocator","table:key42");m.put("originalReadVerified",true);
        m.put("expectedInputSha256",h);m.put("actualInputSha256",h);
        m.put("firstFailingCall","step");m.put("reasonCode","INPUT_MISSING");
        check("OBSERVED_BLOCKER_REPLAY_UNCONFIRMED",m);
        m.put("isolatedReplay",true);m.put("replayLocator","receipt:test1");m.put("originalOutputSha256",o);
        m.put("replayOutputSha256",o);m.put("replayReasonCode","DIFFERENT");
        check("OBSERVED_BLOCKER_REPLAY_UNCONFIRMED",m);m.put("replayReasonCode","INPUT_MISSING");
        check("OBSERVED_BLOCKER_REPLAY_UNCONFIRMED",m);m.put("actualInputSha256",o);
        check("UNCONFIRMED_ORIGINAL_OR_INPUT_MISSING",m);
        Set<String> a=new HashSet<>();for(int i=1;i<=18;i++)a.add(ResearchChartActionsR33.action(String.format(Locale.ROOT,"R%02d",i)));
        if(a.size()!=18)throw new AssertionError("repeated plan");
        try{ResearchChartActionsR33.action("R00");throw new AssertionError();}catch(IllegalArgumentException expected){}
        Map<String,Object> evidence=new HashMap<>();evidence.put("sourceObjectSha256",h);evidence.put("rootCauseStatus","UNCONFIRMED_REPLAY_REQUIRED");
        Map<String,Object> row=new HashMap<>();row.put("group","researchCharts");row.put("id","R06");row.put("firstKnownGap","INPUT_UNAVAILABLE");row.put("evidenceContract",evidence);
        Map<String,Object> first=ReportRepairCaseIndexR335.capture(Collections.singletonList(row),"a".repeat(40),100L);
        Map<?,?> c=(Map<?,?>)((List<?>)first.get("cases")).get(0);
        row.put("firstKnownGap","NO_RECORDED_OUTPUT");Map<String,Object> next=ReportRepairCaseIndexR335.capture(Collections.singletonList(row),"b".repeat(40),200L);
        Map<?,?> n=(Map<?,?>)((List<?>)next.get("cases")).get(0);
        if(!c.get("caseKey").equals(n.get("caseKey"))||c.get("occurrenceId").equals(n.get("occurrenceId")))throw new AssertionError("case/occurrence identity");
        if(!"NO_ENGINEERING_RECEIPT_ATTACHED".equals(n.get("engineeringStatus"))||!Boolean.FALSE.equals(n.get("mayAuthorizeReal")))throw new AssertionError("invented work or qualification");
        try{ReportRepairCaseIndexR335.capture(Arrays.asList(row,row),"a".repeat(40),100L);throw new AssertionError("duplicate case");}catch(IllegalArgumentException expected){}
        System.out.println("PASS: 7 cause-envelope checks (self-asserted replay rejected); 18 distinct actions; case/occurrence identities and duplicate rejection; no work/quality inference");
    }
}
