package com.ethk3.novaonepro.strategyv2;
import java.util.*;
import java.nio.file.*;
public final class ResearchTableAuditProbe {
    static int checks;
    static void check(boolean value,String name){checks++;if(!value)throw new AssertionError(name);}
    static Map<String,Object> row(Object... fields){Map<String,Object> x=new LinkedHashMap<>();for(int i=0;i<fields.length;i+=2)x.put((String)fields[i],fields[i+1]);return x;}
    static Map<String,Object> page(Map<String,Object>... rows){return row("status","READ_OK","rows",Arrays.asList(rows),"offset",0,"hasMore",false,"receiptSha256","source-receipt");}
    static Map<?,?> audit(String id,Map<String,Object>... rows){return ResearchTableAuditR32.audit(id,page(rows));}
    static boolean code(Map<?,?> report,String code){return ((Map<?,?>)report.get("observationCounts")).containsKey(code);}
    public static void main(String[] args)throws Exception {
        Map<?,?> a=audit("T03",row("frozen_at",12,"deadline_at",11,"settled_at",20));
        check(code(a,"TEMPORAL_ORDER_VIOLATION:frozen_at:deadline_at"),"late freeze rejected");
        check("PAGE_INVARIANT_VIOLATION".equals(a.get("status")),"late freeze status");
        check(((Number)a.get("denominator")).intValue()==1,"page denominator");
        check("NONE_READ_ONLY".equals(a.get("qualityGateEffect")),"audit cannot grant qualification");
        a=audit("T03",row("frozen_at",10,"deadline_at",11));
        check(code(a,"FROZEN_NOT_SETTLED"),"missing settlement stays unobserved");
        a=audit("T11",row("trained_through_ordinal",100,"target_ordinal",100,"feature_through_ordinal",99,"model_available_at",9,"frozen_at",10,"deadline_at",11,"model_hash","abc"));
        check(code(a,"TEMPORAL_ORDER_VIOLATION:trained_through_ordinal:target_ordinal"),"target label leak rejected");
        a=audit("T11",row("trained_through_ordinal",98,"target_ordinal",100,"feature_through_ordinal",99,"model_available_at",9,"frozen_at",10,"deadline_at",11,"model_hash","abc"));
        check("PAGE_CHECKS_CLEAR_EFFECT_NOT_PROVEN".equals(a.get("status")),"clean PIT is not quality pass");
        a=audit("T10",row("payload_json","{\"frame\":{\"status\":\"BLOCKED_DEPENDENCY\",\"reason\":\"NO_PARENT\",\"points\":[]}}"));
        check(code(a,"FRAME_BLOCKED_DEPENDENCY"),"fusion blocked frame detected");
        a=audit("T06",row("payload_json","{"));
        check(code(a,"PAYLOAD_MALFORMED:payload_json"),"malformed payload reported");
        a=audit("T05",row("mean_gain",0.3,"lower95",0.1,"upper95",0.2,"evaluated_calendar_issues",12));
        check(code(a,"PAIRED_INTERVAL_INVALID"),"wrong interval detected");
        a=audit("T05",row("mean_gain",0.01,"lower95",-0.02,"upper95",0.04,"evaluated_calendar_issues",12));
        check(code(a,"PAIRED_ADVANTAGE_UNPROVEN"),"negative lower bound preserved");
        a=audit("T15",row("proposal_id","p"));
        check(code(a,"PROPOSAL_CONSUMPTION_NOT_RECORDED"),"missing receipt does not count consumption");
        a=audit("T15",row("receipt_hash","r"));
        check(code(a,"CONSUMPTION_CHILD_REFERENCE_MISSING"),"orphan child reference detected");
        a=audit("T07",row("kept_policy","same","dropped_policy","same"));
        check(code(a,"DEDUP_SELF_DROP"),"self deletion detected");
        a=audit("T02",row("missing_rate",1.1,"n_select",0));
        check(code(a,"ATOM_SUPPORT_VALUE_INVALID")&&code(a,"ATOM_SELECT_SUPPORT_UNAVAILABLE"),"invalid support and no support separated");
        a=ResearchTableAuditR32.audit("T01",row("status","READ_ERROR","rows",List.of()));
        check("PAGE_NOT_READABLE".equals(a.get("status")),"query failure never empty success");
        a=audit("T16",row("original_allow",true,"certificate_active",0));
        check("PAGE_CHECKS_CLEAR_EFFECT_NOT_PROVEN".equals(a.get("status"))&&code(a,"ALLOW_WITHOUT_ACTIVE_CERTIFICATE_RECHECK_GATE"),"original allow is observation not REAL violation");
        Map<String,Object> p=page(row("proposal_id","p"));String before=JsonR27.write(p);
        ResearchTableAuditR32.audit("T15",p);check(before.equals(JsonR27.write(p)),"source page immutable");
        Object fixture=JsonR27.parse(Files.readString(Path.of(args[0])));
        Map<String,Object> detail=RepairDiagnosisR32.build(fixture);
        List<?> rows=(List<?>)detail.get("rows");check(rows.size()==165,"all registered objects preserved");
        int tables=0;
        for(Object o:rows){Map<?,?> r=(Map<?,?>)o;check(r.get("evidenceContract") instanceof Map,"per object evidence contract");
            Map<?,?> evidence=(Map<?,?>)r.get("evidenceContract");
            check(String.valueOf(evidence.get("sourceObjectSha256")).matches("[0-9a-f]{64}"),"real source object digest");
            if("logicalTables".equals(r.get("group"))){tables++;check(r.get("pageEvidenceAudit") instanceof Map,"table audit connected");}}
        check(tables==16,"16 logical table audits");
        Map<?,?> source=(Map<?,?>)fixture;Map<String,Object> damaged=new LinkedHashMap<>();for(Map.Entry<?,?> e:source.entrySet())damaged.put((String)e.getKey(),e.getValue());
        damaged.remove("logicalTables");Map<String,Object> missing=RepairDiagnosisR32.build(damaged);
        check(((List<?>)missing.get("rows")).size()==165,"missing source still emits all registered diagnostic rows");
        int absent=0;for(Object x:(List<?>)missing.get("rows"))if("SOURCE_OBJECT_MISSING".equals(((Map<?,?>)x).get("firstKnownGap")))absent++;
        check(absent==16,"missing tables explicitly unobserved");
        String[] attached=RepairDiagnosisR32.attach(new String[]{"legacy","{}"},detail);
        check(attached[0].contains("页内审计="),"markdown presents actual page audit");
        check(attached[1].contains("evidenceContract"),"JSON same evidence object");
        System.out.println("PASS "+checks+" assertions; production RepairDiagnosisR32 + ResearchTableAuditR32");
    }
}
