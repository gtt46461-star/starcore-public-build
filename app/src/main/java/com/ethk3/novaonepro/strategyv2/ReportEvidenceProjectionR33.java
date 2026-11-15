package com.ethk3.novaonepro.strategyv2;

import java.util.*;

/** Small, explicitly labelled references to originals retained once in the same report.
 * A projection never creates a metric, changes its status or replaces its evidence. */
public final class ReportEvidenceProjectionR33 {
    private ReportEvidenceProjectionR33() {}
    public static final String FORMAT="R33_SHARED_EVIDENCE_REFERENCE_1";
    public static Map<String,Object> reference(String path,Object original) {
        Map<String,Object> out=new LinkedHashMap<>();out.put("format","R33_EXACT_IN_DOCUMENT_REFERENCE_1");
        out.put("reference","R30_REPORT.json#"+path);
        out.put("sha256",StatsR27.sha256(JsonR27.write(original)));
        out.put("scope","EXACT_JSON_VALUE_AT_REFERENCE; NOT_A_MISSING_OR_OMITTED_SOURCE");
        return Collections.unmodifiableMap(out);
    }
    public static Map<String,Object> snapshot(Map<?,?> watermark) {
        Map<String,Object> identity=new LinkedHashMap<>();
        for(String key:new String[]{"schemaVersion","sourceCommit","reportJobId","factsReadAt","latestIssue"})
            identity.put(key,watermark.get(key));
        Object manifest=watermark.get("sourceManifest");
        identity.put("snapshotSha256",manifest instanceof Map?((Map<?,?>)manifest).get("snapshotSha256"):null);
        Map<String,Object> out=new LinkedHashMap<>(identity);
        out.put("identitySha256",StatsR27.sha256(JsonR27.write(identity)));
        out.put("reference","R30_REPORT.json#/snapshotWatermark");
        out.put("scope","IDENTITY_PROJECTION_FULL_WATERMARK_RETAINED_AT_REFERENCE");
        return Collections.unmodifiableMap(out);
    }
    public static List<Map<String,Object>> metrics(List<Map<String,Object>> originals) {
        List<Map<String,Object>> result=new ArrayList<>();
        for(Map<String,Object> original:originals) {
            Map<String,Object> row=new LinkedHashMap<>();
            for(String key:new String[]{"id","metricId","name","definitionVersion","kind","role","status","value",
                    "numerator","denominator","sampleCount","unit","population","missingReason","sourceReceiptSha256"})
                if(original.containsKey(key))row.put(key,original.get(key));
            Object snapshot=original.get("sourceSnapshot");
            if(snapshot instanceof Map)row.put("sourceSnapshot",snapshot((Map<?,?>)snapshot));
            row.put("evidenceRef","R30_REPORT.json#/metricResults[metricId="+original.get("metricId")+"]");
            row.put("evidenceSha256",StatsR27.sha256(JsonR27.write(original)));
            row.put("payloadScope","VALUE_PROJECTION_FULL_DEFINITION_AND_EVIDENCE_AT_REFERENCE");
            result.add(Collections.unmodifiableMap(row));
        }
        return Collections.unmodifiableList(result);
    }
    /** Export only: the in-memory source view remains immutable and reusable. */
    @SuppressWarnings("unchecked")
    public static List<Map<String,Object>> chapters(List<Map<String,Object>> originals,Object index,Object metrics) {
        List<Map<String,Object>> result=new ArrayList<>();
        for(Map<String,Object> original:originals) {
            Map<String,Object> row=new LinkedHashMap<>(original);
            if(row.containsKey("allMetricResults"))row.put("allMetricResults",reference("/metricResults",metrics));
            if(row.containsKey("objectIndex"))row.put("objectIndex",reference("/objectIndex",index));
            Object value=row.get("completeSnapshotEvidenceR31");
            if(value instanceof Map) {
                Map<String,Object> complete=new LinkedHashMap<>((Map<String,Object>)value);
                Object captured=complete.get("sources");
                if(captured instanceof Map) {
                    Map<String,Object> sources=new LinkedHashMap<>((Map<String,Object>)captured);
                    if(sources.containsKey("objectIndex"))sources.put("objectIndex",reference("/objectIndex",index));
                    complete.put("sources",sources);
                }
                complete.put("sourceProjection","R33_EXACT_REFERENCES; ALL_OTHER_CAPTURED_SOURCES_UNCHANGED");
                row.put("completeSnapshotEvidenceR31",complete);
            }
            result.add(row);
        }
        return result;
    }
    private static Object resolve(Map<String,Object> document,String path) {
        if(path.equals("R30_REPORT.json#/objectIndex"))return document.get("objectIndex");
        if(path.equals("R30_REPORT.json#/metricResults"))return document.get("metricResults");
        String prefix="R30_REPORT.json#/metricResults[metricId=";
        if(path.startsWith(prefix)&&path.endsWith("]")) {
            String id=path.substring(prefix.length(),path.length()-1);Object found=null;int matches=0;
            for(Object value:(List<?>)document.get("metricResults"))if(value instanceof Map&&id.equals(((Map<?,?>)value).get("metricId"))){found=value;matches++;}
            if(matches==1)return found;
        }
        prefix="R30_REPORT.json#/objectIndex/matrices/objects[objectId=";
        if(path.startsWith(prefix)&&path.endsWith("]")) {
            String id=path.substring(prefix.length(),path.length()-1);Object found=null;int matches=0;
            Map<?,?> index=(Map<?,?>)document.get("objectIndex"),category=(Map<?,?>)index.get("matrices");
            for(Object value:(List<?>)category.get("objects"))if(value instanceof Map&&id.equals(((Map<?,?>)value).get("objectId"))){found=value;matches++;}
            if(matches==1)return found;
        }
        throw new IllegalStateException("R33_REFERENCE_NOT_UNIQUE_OR_UNSUPPORTED:"+path);
    }
    private static void hash(Map<String,Object> document,Object path,Object digest,Map<String,String> cache) {
        if(!(path instanceof String)||!(digest instanceof String)||!((String)digest).matches("[0-9a-f]{64}"))throw new IllegalStateException("R33_REFERENCE_PATH_OR_HASH_MISSING");
        String key=(String)path,expected=cache.get(key);
        if(expected==null){expected=StatsR27.sha256(JsonR27.write(resolve(document,key)));cache.put(key,expected);}
        if(!expected.equals(digest))throw new IllegalStateException("R33_REFERENCE_HASH_MISMATCH:"+key);
    }
    private static void validateNode(Map<String,Object> document,Object node,Map<String,String> hashes,int depth) {
        if(depth>128)throw new IllegalStateException("R33_REFERENCE_NESTING_BUDGET");
        if(node instanceof Map) {
            Map<?,?> row=(Map<?,?>)node;
            if("R33_EXACT_IN_DOCUMENT_REFERENCE_1".equals(row.get("format")))hash(document,row.get("reference"),row.get("sha256"),hashes);
            if(row.containsKey("evidenceRef")&&"VALUE_PROJECTION_FULL_DEFINITION_AND_EVIDENCE_AT_REFERENCE".equals(row.get("payloadScope"))) {
                hash(document,row.get("evidenceRef"),row.get("evidenceSha256"),hashes);
                Map<?,?> full=(Map<?,?>)resolve(document,String.valueOf(row.get("evidenceRef")));
                for(String key:new String[]{"id","metricId","name","definitionVersion","kind","role","status","value","numerator","denominator","sampleCount","unit","population","missingReason","sourceReceiptSha256"})
                    if(row.containsKey(key)&&!Objects.equals(row.get(key),full.get(key)))throw new IllegalStateException("R33_METRIC_PROJECTION_VALUE_MISMATCH:"+key);
            }
            if("IDENTITY_PROJECTION_FULL_WATERMARK_RETAINED_AT_REFERENCE".equals(row.get("scope"))) {
                if(!row.equals(snapshot((Map<?,?>)document.get("snapshotWatermark"))))throw new IllegalStateException("R33_SNAPSHOT_IDENTITY_PROJECTION_MISMATCH");
            }
            for(Object value:row.values())validateNode(document,value,hashes,depth+1);
        } else if(node instanceof List)for(Object value:(List<?>)node)validateNode(document,value,hashes,depth+1);
    }
    /** Old reports have no referenceFormat and keep their original full payload contract. */
    private static boolean hasNewReference(Object value,int depth) {
        if(depth>128)throw new IllegalStateException("R33_REFERENCE_NESTING_BUDGET");
        if(value instanceof Map){Map<?,?> row=(Map<?,?>)value;
            if("R33_EXACT_IN_DOCUMENT_REFERENCE_1".equals(row.get("format"))||"VALUE_PROJECTION_FULL_DEFINITION_AND_EVIDENCE_AT_REFERENCE".equals(row.get("payloadScope"))||"IDENTITY_PROJECTION_FULL_WATERMARK_RETAINED_AT_REFERENCE".equals(row.get("scope")))return true;
            for(Object child:row.values())if(hasNewReference(child,depth+1))return true;
        }else if(value instanceof List)for(Object child:(List<?>)value)if(hasNewReference(child,depth+1))return true;
        return false;
    }
    public static void validateDocument(Map<String,Object> document) {
        Object format=document.get("referenceFormat");if(format==null){if(hasNewReference(document,0))throw new IllegalStateException("R33_REFERENCE_FORMAT_MISSING");return;}
        if(!FORMAT.equals(format))throw new IllegalStateException("R33_REFERENCE_FORMAT_UNSUPPORTED");
        validateNode(document,document,new HashMap<>(),0);
    }
}
