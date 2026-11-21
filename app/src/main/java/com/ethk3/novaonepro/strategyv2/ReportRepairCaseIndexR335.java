package com.ethk3.novaonepro.strategyv2;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

/** Read-only case identities from the actual report object rows. No work or effect is invented. */
public final class ReportRepairCaseIndexR335 {
    private ReportRepairCaseIndexR335(){}
    static String text(Object x){return x instanceof String?(String)x:x==null?"":String.valueOf(x);}
    static String key(String... fields){
        try{MessageDigest d=MessageDigest.getInstance("SHA-256");
            for(String field:fields){byte[] value=field.getBytes(StandardCharsets.UTF_8);d.update((value.length+":").getBytes(StandardCharsets.US_ASCII));d.update(value);}
            StringBuilder out=new StringBuilder();for(byte b:d.digest())out.append(String.format(Locale.ROOT,"%02x",b&255));return out.toString();
        }catch(java.security.NoSuchAlgorithmException impossible){throw new IllegalStateException(impossible);}
    }
    public static Map<String,Object> capture(List<? extends Map<?,?>> rows,Object commit,Object watermark){
        if(rows==null||rows.size()>165)throw new IllegalArgumentException("R335_CASE_INDEX_BOUND");
        String source=text(commit),snapshot=text(watermark);List<Map<String,Object>> cases=new ArrayList<>();Set<String> seen=new HashSet<>();
        for(Map<?,?> row:rows){String group=text(row.get("group")),id=text(row.get("id"));
            if(group.isEmpty()||id.isEmpty()||!seen.add(group+"\0"+id))throw new IllegalArgumentException("R335_CASE_OBJECT_IDENTITY");
            Map<?,?> evidence=row.get("evidenceContract") instanceof Map?(Map<?,?>)row.get("evidenceContract"):Collections.emptyMap();
            String original=text(evidence.get("sourceObjectSha256"));
            String gap=text(row.get("firstKnownGap"));String caseKey=key("R335_CASE_V1",group,id);
            Map<String,Object> item=new LinkedHashMap<>();item.put("schema","starcore.r335.repair-case.v1");
            item.put("caseKey",caseKey);item.put("needKey",group+":"+id);item.put("group",group);item.put("objectId",id);
            item.put("sourceCommit",source);item.put("readWatermark",snapshot);item.put("sourceObjectSha256",original.isEmpty()?null:original);
            item.put("occurrenceId",key("R335_OCCURRENCE_V1",caseKey,source,snapshot,original,gap));
            item.put("firstObservedGap",gap);item.put("sourceFile",row.get("sourceFile"));item.put("sourceMethod",row.get("sourceMethod"));
            item.put("owner",evidence.get("nextOwner"));item.put("nextAction",row.get("nextAction"));item.put("verificationEntry",row.get("verificationEntry"));
            item.put("rootCauseStatus",evidence.get("rootCauseStatus"));
            item.put("caseState",!source.matches("[0-9a-f]{40}")||snapshot.isEmpty()||!original.matches("[0-9a-f]{64}")?"EVIDENCE_MISSING":"OBSERVED_CASE_AWAITING_WORK_AND_REPLAY");
            item.put("engineeringStatus","NO_ENGINEERING_RECEIPT_ATTACHED");item.put("effectStatus","NO_NEW_INDEPENDENT_EFFECT_EVIDENCE");
            item.put("mayAuthorizeReal",false);cases.add(Collections.unmodifiableMap(item));
        }
        Map<String,Object> out=new LinkedHashMap<>();out.put("schema","starcore.r335.repair-case-index.v1");out.put("sourceCommit",source);out.put("readWatermark",snapshot);
        out.put("cases",Collections.unmodifiableList(cases));out.put("count",cases.size());out.put("scope","ACTUAL_CAPTURE_CASE_IDENTITIES_ONLY; NO_AUTOMATIC_WORK_OR_CAUSAL_VERDICT");
        return Collections.unmodifiableMap(out);
    }
}
