package com.ethk3.novaonepro.strategyv2;

import java.util.*;

/** Per-object counters read in ReportSnapshotR30's existing single SELECT.
 * Rows are attributed by an exact expert/idea ID or a delimited family member
 * reference. Membership in a composite never becomes an independent strategy,
 * a validated artifact, a numeric contribution, or a natural-quality sample. */
public final class CapabilityRuntimeEvidenceR33 {
    private CapabilityRuntimeEvidenceR33() {}
    private static final String PREFIX="capabilityRuntimeR33:";
    private static final String[] FAMILY={"candidateRows","progressedCandidateRows","developmentScoredRows",
            "checkpointReferenceRows","blockedCandidateRows","readyCandidateRows","consumptionReceiptRows"};
    private static final String[] EXPERT={"auditRows","experimentRows","progressedExperimentRows",
            "blockedExperimentRows","proposalRows","proposalWithChildRows"};
    private static final String[] IDEA={"policyRows","replayScoreRows","frozenRows"};
    private static String id(String prefix,int value){return String.format(Locale.ROOT,prefix+"%02d",value);}
    private static String familyWhere(String alias,String family) {
        String c=alias+"canonical_text";
        return "(instr("+c+",'|M=R26"+family+"{')>0 OR instr("+c+",';R26"+family+"{')>0)";
    }
    private static void count(ReportSnapshotR30.Query q,String object,String field,String table,String where,String... columns) {
        q.add(PREFIX+object+":"+field,q.has(table,columns)?"(SELECT count(*) FROM "+table+(where==null?"":" WHERE "+where)+")":"NULL",true);
    }
    static void register(ReportSnapshotR30.Query q) {
        for(int n=1;n<=24;n++) {
            String f=id("F",n),w=familyWhere("",f);
            count(q,f,FAMILY[0],"r26_candidate",w,"canonical_text");
            count(q,f,FAMILY[1],"r26_candidate",w+" AND cursor>0","canonical_text","cursor");
            count(q,f,FAMILY[2],"r26_candidate",w+" AND (dev_n>0 OR val_n>0)","canonical_text","dev_n","val_n");
            count(q,f,FAMILY[3],"r26_candidate",w+" AND length(state_hash)=64 AND length(checkpoint_hash)=64","canonical_text","state_hash","checkpoint_hash");
            count(q,f,FAMILY[4],"r26_candidate",w+" AND status LIKE 'BLOCKED%'","canonical_text","status");
            count(q,f,FAMILY[5],"r26_candidate",w+" AND status='READY'","canonical_text","status");
            q.add(PREFIX+f+":firstBlockedStatus",q.has("r26_candidate","canonical_text","status")
                    ?"(SELECT min(status) FROM r26_candidate WHERE "+w+" AND status LIKE 'BLOCKED%')":"NULL",false);
            String sql="(SELECT count(*) FROM r26_consumption c WHERE EXISTS(SELECT 1 FROM r26_candidate p WHERE p.policy_hash=c.policy_hash AND "+familyWhere("p.",f)+"))";
            q.add(PREFIX+f+":"+FAMILY[6],q.has("r26_consumption","policy_hash")&&q.has("r26_candidate","policy_hash","canonical_text")?sql:"NULL",true);
        }
        for(int n=1;n<=16;n++) {
            String e=id("E",n),w="expert_id='"+e+"'";
            count(q,e,EXPERT[0],"r26_expert_audit",w,"expert_id");
            count(q,e,EXPERT[1],"r26_expert_experiment",w,"expert_id");
            count(q,e,EXPERT[2],"r26_expert_experiment",w+" AND cursor>0","expert_id","cursor");
            count(q,e,EXPERT[3],"r26_expert_experiment",w+" AND status LIKE 'BLOCKED%'","expert_id","status");
            q.add(PREFIX+e+":firstBlockedStatus",q.has("r26_expert_experiment","expert_id","status")
                    ?"(SELECT min(status) FROM r26_expert_experiment WHERE "+w+" AND status LIKE 'BLOCKED%')":"NULL",false);
            count(q,e,EXPERT[4],"r27_expert_proposal","expert='"+e+"'","expert");
            String sql="(SELECT count(*) FROM r27_expert_proposal p WHERE p.expert='"+e+"' AND EXISTS(SELECT 1 FROM r27_policy child WHERE child.policy_hash=p.policy_hash))";
            q.add(PREFIX+e+":"+EXPERT[5],q.has("r27_expert_proposal","expert","policy_hash")&&q.has("r27_policy","policy_hash")?sql:"NULL",true);
        }
        for(int n=0;n<=20;n++) {
            String i=id("I",n),w="idea_family='"+i+"'";
            count(q,i,IDEA[0],"r27_policy",w,"idea_family");
            for(int k=1;k<IDEA.length;k++) {
                String table=k==1?"r27_replay_score":"r27_live_frozen";
                String sql="(SELECT count(*) FROM "+table+" evidence WHERE EXISTS(SELECT 1 FROM r27_policy p WHERE p.idea_family='"+i+"' AND p.policy_hash=evidence.policy_hash))";
                q.add(PREFIX+i+":"+IDEA[k],q.has(table,"policy_hash")&&q.has("r27_policy","policy_hash","idea_family")?sql:"NULL",true);
            }
        }
        StringJoiner attributed=new StringJoiner(" OR ");for(int n=1;n<=24;n++)attributed.add(familyWhere("",id("F",n)));
        count(q,"UNATTRIBUTED","candidateRows","r26_candidate","canonical_text IS NULL OR NOT("+attributed+")","canonical_text");
    }
    private static Long number(Object value) {
        if(!(value instanceof Number))return null;
        double x=((Number)value).doubleValue();return Double.isFinite(x)&&x>=0&&x==Math.rint(x)?((Number)value).longValue():null;
    }
    private static Map<String,Object> object(Map<String,Object> values,String id,String[] fields,String attribution) {
        Map<String,Object> row=new LinkedHashMap<>(),counts=new LinkedHashMap<>();
        int known=0;for(String key:fields){Long value=number(values.get(PREFIX+id+":"+key));counts.put(key,value);if(value!=null)known++;}
        row.put("id",id);row.put("counters",counts);row.put("sourceStatus",known==fields.length?"ALL_COUNTER_SOURCES_READ":"COUNTER_SOURCE_UNAVAILABLE");
        row.put("counterScope","ALL_RETAINED_MATCHING_SOURCE_ROWS; NOT_CUMULATIVE_LIFETIME_AFTER_ARCHIVAL");
        row.put("attribution",attribution);row.put("artifactBytesVerified",null);row.put("numericalEffectValidated",null);
        row.put("firstBlockedStatus",values.get(PREFIX+id+":firstBlockedStatus"));
        row.put("blockedStatusScope","LEXICOGRAPHIC_FIRST_RETAINED_STATE; NOT_FIRST_IN_TIME_OR_COMPLETE_REASON_DISTRIBUTION");
        row.put("counterReceiptSha256",StatsR27.sha256(JsonR27.write(counts)));
        return row;
    }
    static Map<String,Object> captured(Map<String,Object> values,Map<String,Object> watermark) {
        Map<String,Object> out=new LinkedHashMap<>();List<Object> objects=new ArrayList<>();
        for(int n=1;n<=24;n++)objects.add(object(values,id("F",n),FAMILY,
                "DELIMITED_R26Fxx_CANONICAL_MEMBER_REFERENCE; COMPOSITES_MAY_HAVE_MULTIPLE_MEMBERS; NOT_AST_OR_ARTIFACT_VALIDATION"));
        for(int n=1;n<=16;n++)objects.add(object(values,id("E",n),EXPERT,"EXACT_PERSISTED_EXPERT_ID; AUDIT_AND_CHILD_PROPOSAL_COUNTS_SEPARATE"));
        for(int n=0;n<=20;n++)objects.add(object(values,id("I",n),IDEA,"EXACT_PERSISTED_IDEA_FAMILY_AND_POLICY_HASH_JOIN; I00_CONTROL_ONLY"));
        out.put("version","R33_OBJECT_RUNTIME_COUNTERS_1");out.put("objects",objects);
        out.put("sourceSnapshot",ReportEvidenceProjectionR33.snapshot(watermark));
        out.put("unattributedCandidateRows",number(values.get(PREFIX+"UNATTRIBUTED:candidateRows")));
        out.put("sourceFiles",Arrays.asList("r26_candidate","r26_consumption","r26_expert_audit","r26_expert_experiment","r27_expert_proposal","r27_policy","r27_replay_score","r27_live_frozen"));
        out.put("sourceScope","SAME_SINGLE_SELECT_AS_FACTS; NO_PER_OBJECT_OR_LIVE_REQUERY");
        out.put("effectBoundary","Row/cursor/hash references do not prove checkpoint bytes, gradients, causal contribution, natural advantage, or REAL authority.");
        return out;
    }
    private static Map<?,?> map(Object x){return x instanceof Map?(Map<?,?>)x:Collections.emptyMap();}
    public static Map<String,Map<?,?>> index(Object source) {
        Map<String,Map<?,?>> out=new LinkedHashMap<>();Object raw=map(source).get("objects");
        if(raw instanceof List)for(Object x:(List<?>)raw){Map<?,?> row=map(x);Object id=row.get("id");if(id instanceof String)out.put((String)id,row);}
        return out;
    }
    public static String firstObservedGap(Map<?,?> evidence) {
        if(evidence.isEmpty())return null;
        if(evidence.containsKey("canonicalObjectReference"))return "REFER_CANONICAL_MATRIX_EVIDENCE";
        Map<?,?> counters=map(evidence.get("counters"));
        for(String key:new String[]{"blockedCandidateRows","blockedExperimentRows"}) {
            Long n=number(counters.get(key));if(n!=null&&n>0)return "PERSISTED_OBJECT_BLOCKER_RECORDED";
        }
        if("COUNTER_SOURCE_UNAVAILABLE".equals(evidence.get("sourceStatus")))return "RUNTIME_COUNTER_SOURCE_UNAVAILABLE";
        int known=0;
        for(Object value:counters.values()) {
            Long n=number(value);if(n!=null){known++;if(n>0)return "RUNTIME_ROWS_RECORDED_VERIFY_ARTIFACT_AND_EFFECT";}
        }
        return known==0?"RUNTIME_COUNTER_SOURCE_UNAVAILABLE":"NO_MATCHING_RUNTIME_ROWS_IN_CAPTURE";
    }
}
