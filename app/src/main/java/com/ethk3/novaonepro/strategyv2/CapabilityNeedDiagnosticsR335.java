package com.ethk3.novaonepro.strategyv2;

import java.lang.reflect.Method;
import java.util.*;

/** Finite requirement-to-owner diagnosis. The registered scope is explicit;
 * missing observations cannot establish an absent capability or a benefit. */
public final class CapabilityNeedDiagnosticsR335 {
    private CapabilityNeedDiagnosticsR335() {}
    private static Map<?,?> map(Object x){return x instanceof Map?(Map<?,?>)x:Collections.emptyMap();}
    private static List<?> list(Object x){return x instanceof List?(List<?>)x:Collections.emptyList();}
    private static String text(Object x){return x instanceof String?(String)x:"";}
    private static final int MAX_NEEDS=64,MAX_OWNERS=8;

    static Map<String,Object> inspect(Map<?,?> endpoint,ClassLoader loader){
        Map<String,Object> result=new LinkedHashMap<>();String owner=text(endpoint.get("class")),name=text(endpoint.get("method"));
        List<?> expected=list(endpoint.get("parameters"));result.put("class",owner);result.put("method",name);result.put("parameters",expected);
        if(!owner.matches("com\\.ethk3\\.novaonepro\\.[A-Za-z0-9_.$]+")||!name.matches("[A-Za-z_][A-Za-z0-9_]*")||expected.size()>12){result.put("status","UNKNOWN_INVALID_OWNER_CONTRACT");return result;}
        try{
            Class<?> type=Class.forName(owner,false,loader);List<String> observed=new ArrayList<>();boolean match=false;
            for(Method method:type.getDeclaredMethods())if(method.getName().equals(name)){
                List<String> parameters=new ArrayList<>();for(Class<?> parameter:method.getParameterTypes())parameters.add(parameter.getName());
                observed.add(method.getName()+parameters.toString());if(parameters.equals(expected))match=true;
            }
            Collections.sort(observed);result.put("observedNamedSignatures",observed);
            result.put("status",match?"PRESENT_DECLARATION_ONLY":"ABSENT_EXACT_DECLARATION_IN_REQUIRED_OWNER");
        }catch(ClassNotFoundException absent){result.put("status","ABSENT_REQUIRED_OWNER_CLASS");}
        catch(LinkageError|SecurityException unreadable){result.put("status","UNKNOWN_OWNER_INSPECTION_FAILURE");result.put("exceptionType",unreadable.getClass().getName());}
        return result;
    }
    private static boolean identity(Map<?,?> index){Object at=index.get("sourceSnapshot");return text(index.get("sourceCommit")).matches("[0-9a-f]{40}")&&(at instanceof Long||at instanceof Integer)&&((Number)at).longValue()>0;}
    static String classification(boolean identity,boolean coverage,int present,int absent,int unknown,boolean blocked){
        if(!identity||!coverage||unknown>0)return "EVIDENCE_MISSING";
        if(present==0&&absent>0)return "NEW_CAPABILITY_PROPOSAL";
        if(absent>0)return "EXISTING_CAPABILITY_BINDING_GAP";
        return blocked?"EXISTING_CAPABILITY_BLOCKED":"EXISTING_CAPABILITY_DECLARED_RUNTIME_UNVERIFIED";
    }
    static Map<String,Object> assess(Map<?,?> index,List<?> needs,ClassLoader loader,String inspectionCommit){
        if(needs.size()>MAX_NEEDS)throw new IllegalArgumentException("R335_CAPABILITY_NEED_BOUND");
        Map<String,Map<?,?>> objects=new LinkedHashMap<>();Set<String> duplicates=new HashSet<>();
        for(String group:new String[]{"capabilities","matrices","researchCharts","legacyCharts","logicalTables"})
            for(Object value:list(map(index.get(group)).get("objects"))){Map<?,?> row=map(value);String id=text(row.get("objectId"));if(id.isEmpty())id=text(row.get("id"));String key=group+":"+id;if(objects.put(key,row)!=null)duplicates.add(key);}
        List<Object> results=new ArrayList<>(),candidates=new ArrayList<>(),ownerManifest=new ArrayList<>();Set<String> seen=new HashSet<>();
        for(Object value:needs){Map<?,?> need=map(value);String key=text(need.get("needKey"));
            if(key.isEmpty()||!seen.add(key))throw new IllegalArgumentException("R335_CAPABILITY_NEED_IDENTITY");
            List<?> endpoints=list(need.get("requiredOwners"));if(endpoints.isEmpty()||endpoints.size()>MAX_OWNERS)throw new IllegalArgumentException("R335_CAPABILITY_OWNER_BOUND");
            int present=0,absent=0,unknown=0;List<Object> owners=new ArrayList<>(),runtime=new ArrayList<>();boolean blocked=false,coverage=true;
            for(Object raw:endpoints){Map<String,Object> inspected=inspect(map(raw),loader);String status=text(inspected.get("status"));
                if(status.startsWith("PRESENT"))present++;else if(status.startsWith("ABSENT"))absent++;else unknown++;owners.add(inspected);}
            for(Object ref:list(need.get("relatedObjects"))){String refKey=text(ref);Map<?,?> row=objects.get(refKey);
                if(row==null||duplicates.contains(refKey)){coverage=false;continue;}
                Map<String,Object> observation=new LinkedHashMap<>();observation.put("objectKey",refKey);
                for(String field:new String[]{"implemented","callable","inputAvailable","produced","consumed","effective","notProducedReason","executionBlockedReason","status"})observation.put(field,row.get(field));
                Map<?,?> evidence=map(row.get("runtimeEvidenceR33"));String blocker=text(evidence.get("firstBlockedStatus"));
                observation.put("persistedObjectBlocker",blocker.isEmpty()?null:blocker);
                // Static report-process bindings and a normal wait are not runtime failure proof.
                boolean objectBlocked=!blocker.isEmpty()||Boolean.FALSE.equals(row.get("callable"))&&text(row.get("executionBlockedReason")).length()>0;
                observation.put("hasExplicitBlocker",objectBlocked);
                if(!Boolean.FALSE.equals(need.get("runtimeBlockerApplicable")))blocked|=objectBlocked;
                runtime.add(observation);
            }
            String state=classification(identity(index)&&Objects.equals(index.get("sourceCommit"),inspectionCommit),coverage,present,absent,unknown,blocked);
            Map<String,Object> item=new LinkedHashMap<>();for(String field:new String[]{"needKey","title","ownerBatch","sourceContract","requiredInputs","producerPurpose","consumerPurpose","nextAction","positiveVerification","negativeVerification","closestExistingObjects"})item.put(field,need.get(field));
            item.put("classification",state);item.put("requiredOwnerInspections",owners);item.put("relatedObjectObservations",runtime);
            item.put("scope","ONLY_ENUMERATED_REQUIRED_OWNER_CLASSES_AND_EXACT_DECLARED_SIGNATURES; NOT_ALL_POSSIBLE_IMPLEMENTATIONS");
            item.put("reportSourceCommit",index.get("sourceCommit"));item.put("inspectedSourceCommit",inspectionCommit);item.put("sourceSnapshot",index.get("sourceSnapshot"));
            item.put("inspectionMatchesReport",Objects.equals(index.get("sourceCommit"),inspectionCommit));
            item.put("runtimeBlockerApplicable",!Boolean.FALSE.equals(need.get("runtimeBlockerApplicable")));
            item.put("registryEntrySha256",StatsR27.sha256(JsonR27.write(need)));item.put("ownerInspectionSha256",StatsR27.sha256(JsonR27.write(owners)));
            item.put("effectStatus","NO_EFFECT_INFERRED_FROM_DECLARATIONS_OR_BLOCKER_COUNTS");item.put("mayAuthorizeReal",false);
            item.put("proposalId",state.equals("NEW_CAPABILITY_PROPOSAL")?StatsR27.sha256("R335_NEED|"+key+"|"+index.get("sourceCommit")+"|"+item.get("registryEntrySha256")):null);
            results.add(item);ownerManifest.add(Collections.singletonMap(key,owners));
            if(state.equals("NEW_CAPABILITY_PROPOSAL"))candidates.add(item);
        }
        Map<String,Object> out=new LinkedHashMap<>();out.put("schema","R335_CAPABILITY_NEED_DIAGNOSIS_1");out.put("sourceCommit",index.get("sourceCommit"));out.put("inspectedSourceCommit",inspectionCommit);out.put("sourceSnapshot",index.get("sourceSnapshot"));
        out.put("registryHash",StatsR27.sha256(JsonR27.write(needs)));out.put("ownerManifestHash",StatsR27.sha256(JsonR27.write(ownerManifest)));
        out.put("needCount",needs.size());out.put("needResults",results);out.put("candidateCapabilityProposalsR33",candidates);
        out.put("coverageBoundary","VERSIONED_FINITE_NEED_CATALOG; NO_CLAIM_TO_ENUMERATE_UNKNOWN_FUTURE_CAPABILITIES");out.put("mayAuthorizeReal",false);return out;
    }
    public static Map<String,Object> capture(Map<String,Object> index){
        return assess(index,CapabilityNeedRegistryR335.needs(),CapabilityNeedDiagnosticsR335.class.getClassLoader(),com.ethk3.novaonepro.BuildConfig.SOURCE_COMMIT_R26);
    }
}
