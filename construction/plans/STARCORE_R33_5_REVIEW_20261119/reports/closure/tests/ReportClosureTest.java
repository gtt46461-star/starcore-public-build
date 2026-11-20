package com.ethk3.novaonepro.strategyv2;
import java.nio.file.*;
import java.util.*;

/** Executes production capture, binding, classification and report builders.
 * Original 033 report data is a retrospective readback, not a new device run. */
public final class ReportClosureTest {
    static int assertions;
    static void ok(boolean value,String name){assertions++;if(!value)throw new AssertionError(name);}
    @SuppressWarnings("unchecked") static Map<String,Object> map(Object value){return value instanceof Map?(Map<String,Object>)value:new LinkedHashMap<>();}
    static Map<String,Object> m(Object... values){Map<String,Object> out=new LinkedHashMap<>();for(int i=0;i<values.length;i+=2)out.put((String)values[i],values[i+1]);return out;}
    static List<?> l(Object value){return value instanceof List?(List<?>)value:Collections.emptyList();}
    static String commit="a".repeat(40);
    public static void existing(String value){}
    static Map<String,Object> endpoint(String name,String method,String... parameters){return m("class",name,"method",method,"parameters",Arrays.asList(parameters));}
    static Map<String,Object> need(Object... owners){return m("needKey","CHECKED_REQUIREMENT","requiredOwners",Arrays.asList(owners),"relatedObjects",Collections.emptyList());}
    static String className=ReportClosureTest.class.getName();
    static Map<String,Object> assessment(Map<String,Object> index,Map<String,Object> requirement){return CapabilityNeedDiagnosticsR335.assess(index,Collections.singletonList(requirement),ReportClosureTest.class.getClassLoader(),commit);}
    static String state(Map<String,Object> assessment){return String.valueOf(map(l(assessment.get("needResults")).get(0)).get("classification"));}
    static void needs(){
        Map<String,Object> index=m("sourceCommit",commit,"sourceSnapshot",100L);
        Map<String,Object> present=endpoint(className,"existing","java.lang.String"),absent=endpoint("com.ethk3.novaonepro.NoSuchOwnerR335","produce");
        ok(state(assessment(index,need(present,present))).equals("EXISTING_CAPABILITY_DECLARED_RUNTIME_UNVERIFIED"),"present declarations do not prove runtime");
        Map<String,Object> proposal=assessment(index,need(absent,absent));
        ok(state(proposal).equals("NEW_CAPABILITY_PROPOSAL")&&l(proposal.get("candidateCapabilityProposalsR33")).size()==1,"genuinely missing scoped owners propose");
        ok(state(assessment(index,need(present,absent))).equals("EXISTING_CAPABILITY_BINDING_GAP"),"partial binding");
        ok(state(assessment(index,need(endpoint("invalid","x"),absent))).equals("EVIDENCE_MISSING"),"inspection unknown never absent");
        ok(state(assessment(m("sourceCommit","unknown","sourceSnapshot",100L),need(absent))).equals("EVIDENCE_MISSING"),"missing source identity");
        Map<String,Object> missing=need(absent);missing.put("relatedObjects",List.of("capabilities:F01"));
        ok(state(assessment(index,missing)).equals("EVIDENCE_MISSING"),"missing object observation never absence proof");
        ok(state(assessment(index,need(endpoint(className,"existing","int")))).equals("NEW_CAPABILITY_PROPOSAL"),"exact signature required");
        boolean failed=false;try{CapabilityNeedDiagnosticsR335.assess(index,List.of(need(present),need(present)),ReportClosureTest.class.getClassLoader(),commit);}catch(IllegalArgumentException expected){failed=true;}
        ok(failed,"duplicate need refusal");
        ok(state(CapabilityNeedDiagnosticsR335.assess(index,List.of(need(absent)),ReportClosureTest.class.getClassLoader(),"b".repeat(40))).equals("EVIDENCE_MISSING"),"inspection commit must match report");
        List<?> defaults=CapabilityNeedRegistryR335.needs();ok(defaults.size()==8,"versioned finite needs");
        for(Object value:defaults)for(Object owner:l(map(value).get("requiredOwners"))){Map<String,Object> actual=CapabilityNeedDiagnosticsR335.inspect(map(owner),ReportClosureTest.class.getClassLoader());ok("PRESENT_DECLARATION_ONLY".equals(actual.get("status")),"production exact declaration: "+JsonR27.write(actual));}
    }
    static void traces(){
        Map<String,Object> watermark=m("sourceCommit",commit,"factsReadAt",100L);
        Map<String,Object> task=m("sourceTable","r26_task","task_id","foundry-su2","owner","owner-A","fence",2L,"status","RUNNING","blocked_reason","DEPENDENCY:CHECKPOINT_MISSING");
        Map<String,Object> fault=m("sourceTable","r31_runtime_fault","event_id","event-1","task_id","foundry-su2","owner_epoch","owner-A","fence",2L,"operation","TRAIN","category","DEPENDENCY","phase","ADMISSION");
        Map<String,Object> data=m("tasks",List.of(task),"r31RuntimeFaults",List.of(fault));
        Map<String,Object> packet=DiagnosisTraceReaderR33.captureSingleSelect(watermark,data,null);
        ok(DiagnosisTraceReaderR33.validPacket(packet,commit,100L),"source packet integrity");
        ok(!DiagnosisTraceReaderR33.validPacket(DiagnosisTraceReaderR33.captureSingleSelect(m("sourceCommit","UNKNOWN","factsReadAt",100L),data,null),"UNKNOWN",100L),"unknown source cannot bind a verified observation");
        Map<String,Object> trace=DiagnosisTraceReaderR33.forObject(packet,"capabilities","F01",commit,100L);
        ok(!trace.isEmpty(),"real owner producer attached");
        ok("OBSERVED_SHARED_OWNER_REASON_NOT_OBJECT_ROOT_CAUSE".equals(DiagnosisProofR33.classify(trace,"capabilities","F01",commit,100L)),"shared owner cannot be causal proof");
        ok(Boolean.TRUE.equals(map(l(map(l(trace.get("ownerObservations")).get(0)).get("faultReferences")).get(0)).get("currentOwnerAndFenceMatch")),"current owner and fence join");
        fault.put("fence",1L);Map<String,Object> stale=DiagnosisTraceReaderR33.forObject(DiagnosisTraceReaderR33.captureSingleSelect(watermark,data,null),"capabilities","F01",commit,100L);
        ok(Boolean.FALSE.equals(map(l(map(l(stale.get("ownerObservations")).get(0)).get("faultReferences")).get(0)).get("currentOwnerAndFenceMatch")),"old fence not current evidence");
        ok(DiagnosisTraceReaderR33.forObject(packet,"capabilities","F01",commit,101L).isEmpty(),"wrong watermark refused");
        ok(DiagnosisTraceReaderR33.forObject(packet,"logicalTables","T16",commit,100L).isEmpty(),"unknown owner remains unknown");
        trace.put("ownerObservations",List.of(m("invented",true)));
        ok("UNCONFIRMED_ORIGINAL_ENVELOPE_INVALID".equals(DiagnosisProofR33.classify(trace,"capabilities","F01",commit,100L)),"trace tamper refused");
        map(l(packet.get("originals")).get(0)).put("sha256","0".repeat(64));
        ok(!DiagnosisTraceReaderR33.validPacket(packet,commit,100L),"original hash tamper refused");
        task.put("blocked_reason","z".repeat(40000));Map<String,Object> bounded=DiagnosisTraceReaderR33.captureSingleSelect(watermark,data,null);
        ok(((Number)map(map(bounded.get("coverage")).get("tasks")).get("rejectedOrOverBudget")).intValue()==1,"oversize raw row omitted with evidence gap");
        ok(((Number)bounded.get("projectedBytes")).intValue()<=DiagnosisTraceReaderR33.MAX_BYTES,"bounded capture");
        List<Object> many=new ArrayList<>();for(int i=0;i<101;i++)many.add(m("sourceTable","r26_task","task_id","task"+i));
        bounded=DiagnosisTraceReaderR33.captureSingleSelect(watermark,m("tasks",many),null);
        ok(((Number)map(map(bounded.get("coverage")).get("tasks")).get("retainedRows")).intValue()==100,"task row cap");
        ok(Boolean.FALSE.equals(map(map(bounded.get("coverage")).get("r31RuntimeFaults")).get("sourceAvailable")),"missing faults not zero");
    }
    static Map<String,Object> actualReport(String path)throws Exception{
        Map<String,Object> report=map(JsonR27.parse(Files.readString(Path.of(path))));
        Map<String,Object> complete=map(map(l(report.get("chapters")).get(11)).get("completeSnapshotEvidenceR31"));
        Map<String,Object> data=map(complete.get("sources")),watermark=map(complete.get("snapshotWatermark"));
        Map<String,Object> packet=DiagnosisTraceReaderR33.captureSingleSelect(watermark,data,null);
        Map<String,Object> index=map(report.get("objectIndex"));DiagnosisTraceReaderR33.attach(index,packet);
        Map<String,Object> needs=CapabilityNeedDiagnosticsR335.capture(index);index.put("capabilityNeedDiagnosticsR335",needs);
        for(Object raw:l(needs.get("needResults")))ok("EVIDENCE_MISSING".equals(map(raw).get("classification")),"new code reflection cannot masquerade as original033 code");
        Map<String,Object> diagnosis=RepairDiagnosisR32.build(index);
        ok(l(diagnosis.get("rows")).size()==165,"actual report exact165 preserved");
        ok(map(diagnosis.get("improvementCasesR335")).get("count").equals(165),"cases remain165");
        ok(l(map(diagnosis.get("capabilityNeedDiagnosticsR335")).get("needResults")).size()==8,"needs reach production diagnosis");
        int observed=0,shared=0;for(Object raw:l(diagnosis.get("rows"))){String s=String.valueOf(map(map(raw).get("evidenceContract")).get("rootCauseStatus"));if(s.startsWith("OBSERVED_"))observed++;if(s.contains("SHARED_OWNER"))shared++;ok(!s.equals("ROOT_CAUSE_REPRODUCED"),"original report cannot fabricate root cause");}
        ok(observed>0&&shared>0,"original report owner evidence actually consumed");
        String[] formats=RepairDiagnosisR32.attach(new String[]{"# legacy", "{\"findings\":[]}"},diagnosis);
        ok(formats[0].contains("有限需求目录与能力缺口")&&formats[1].contains("candidateCapabilityProposalsR33"),"markdown/json both wired");
        return m("reportPath",path,"sourceCommit",watermark.get("sourceCommit"),"sourceWatermark",watermark.get("factsReadAt"),"sourcePacketSha256",packet.get("sourcePacketSha256"),"originalRows",l(packet.get("originals")).size(),"attachedObjects",index.get("faultTraceAttachedObjectsR335"),"observedOwnerRows",observed,"sharedOwnerRows",shared,"needResults",needs.get("needResults"),"scope","HOST_RETROSPECTIVE_READBACK_OF_ORIGINAL_033_REPORT; NO_DEVICE_OR_CAUSAL_REPLAY");
    }
    public static void main(String[] args)throws Exception{
        needs();traces();List<Object> reports=new ArrayList<>();for(String arg:args)reports.add(actualReport(arg));
        System.out.println(JsonR27.write(m("status","PASS","assertions",assertions,"reportReadbacks",reports,"scope","PRODUCTION_JAVA_HOST_EXECUTION; ANDROID_SQL_CAPTURE_AND_DEVICE_NOT_RUN")));
    }
}
