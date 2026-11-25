package com.ethk3.novaonepro.strategyv2;

import java.util.*;

/** Read-only aggregation of the current report's authoritative metric states.
 * This class cannot certify a policy, convert a row count to PASS, or authorize REAL. */
public final class ReportAssessmentR33 {
    private ReportAssessmentR33() {}
    private static boolean failed(String status) {
        return "FAIL".equals(status)||"DATA_CONFLICT".equals(status)||"BLOCKED".equals(status)
                ||status.startsWith("INVALID")||status.endsWith("_VIOLATION");
    }
    private static Map<?,?> map(Object value){return value instanceof Map?(Map<?,?>)value:Collections.emptyMap();}
    private static Double finite(Object value) {
        if(!(value instanceof Number))return null;
        double n=((Number)value).doubleValue();return Double.isFinite(n)?n:null;
    }
    private static boolean verifiedPass(Map<?,?> row,String id){
        if("SNAPSHOT_PROJECTION_ONLY".equals(row.get("sourceReceiptKind")))return false;
        Object receipt=row.get("sourceReceiptSha256");
        Map<?,?> evaluation=map(row.get("r31Evaluation")),definition=map(row.get("r31Definition"));
        Double samples=finite(row.get("sampleCount")),value=finite(row.get("value"));
        Double evaluatedSamples=finite(evaluation.get("samples")),evaluatedValue=finite(evaluation.get("value"));
        Double minimum=finite(evaluation.get("minimumSample")),declaredMinimum=finite(definition.get("minimumSample"));
        if(!(receipt instanceof String)||!((String)receipt).matches("[0-9a-f]{64}")
                ||samples==null||value==null||evaluatedSamples==null||evaluatedValue==null||minimum==null||declaredMinimum==null)return false;
        boolean role="Q08".equals(id)?"SYNTHETIC".equals(row.get("role"))
                :!id.startsWith("Q")||"NATURAL_FUTURE".equals(row.get("role"));
        Object version=row.get("definitionVersion");
        return samples>0&&samples==Math.rint(samples)&&samples.equals(evaluatedSamples)
                &&value.equals(evaluatedValue)&&minimum>0&&minimum==Math.rint(minimum)
                &&minimum.equals(declaredMinimum)&&samples>=minimum
                &&id.equals(evaluation.get("id"))&&id.equals(definition.get("id"))
                &&version instanceof String&&version.equals(evaluation.get("version"))&&version.equals(definition.get("version"))
                &&"PASS".equals(evaluation.get("status"))&&receipt.equals(evaluation.get("receiptHash"))&&role;
    }
    private static Map<String,Object> group(Map<String,Map<?,?>> rows,List<String> wanted,boolean device,boolean conflicts,boolean natural) {
        List<String> missing=new ArrayList<>(),failures=new ArrayList<>(),passed=new ArrayList<>(),pending=new ArrayList<>();
        Map<String,Long> states=new LinkedHashMap<>();int observed=0;
        for(String id:wanted) {
            Map<?,?> row=rows.get(id);
            if(row==null){missing.add(id);continue;}
            String state=String.valueOf(row.get("status"));states.put(state,states.getOrDefault(state,0L)+1);
            if(failed(state))failures.add(id);
            else if("PASS".equals(state)) {
                // A bare PASS string is insufficient even for a report aggregation.
                if(verifiedPass(row,id))passed.add(id);
                else pending.add(id);
            } else pending.add(id);
            if(row.get("value") instanceof Number||"DESCRIPTIVE_ONLY".equals(state)||failed(state))observed++;
        }
        String status=!failures.isEmpty()?"FAIL":!missing.isEmpty()?"UNKNOWN_INCOMPLETE_METRIC_SET"
                :conflicts?"INCOMPLETE_SOURCE_OR_METRIC_CONFLICT"
                :passed.size()==wanted.size()?"PASS_REPORTED_METRIC_EVIDENCE"
                :device&&observed==0&&passed.isEmpty()?"NOT_RUN_OR_RECEIPT_UNAVAILABLE"
                :natural?"NOT_PROVEN"
                :observed>0||!passed.isEmpty()?"PARTIAL":"INSUFFICIENT";
        Map<String,Object> out=new LinkedHashMap<>();out.put("status",status);out.put("expected",wanted.size());
        out.put("stateCounts",states);out.put("missingIds",missing);out.put("failedIds",failures);
        out.put("verifiedMetricIds",passed);out.put("pendingIds",pending);
        out.put("basis","CURRENT_METRIC_DEFINITION_AND_R31_EVALUATION_AGREE_ON_ID_VERSION_VALUE_SAMPLES_MINIMUM_AND_RECEIPT; Q01_Q07_NATURAL_FUTURE; Q08_SYNTHETIC; NOT_NEW_VALIDATION");
        return out;
    }
    public static Map<String,Object> evaluate(Object metricObject,Object errorObject) {
        Map<String,Map<?,?>> rows=new LinkedHashMap<>();List<String> duplicates=new ArrayList<>();
        if(metricObject instanceof List)for(Object value:(List<?>)metricObject)if(value instanceof Map) {
            Map<?,?> row=(Map<?,?>)value;String id=String.valueOf(row.get("metricId"));
            if(rows.put(id,row)!=null)duplicates.add(id);
        }
        List<String> engineering=new ArrayList<>(),quality=new ArrayList<>();
        for(int n=1;n<=36;n++)engineering.add(String.format(Locale.ROOT,"M%02d",n));
        for(int n=1;n<=7;n++)quality.add(String.format(Locale.ROOT,"Q%02d",n));
        List<?> errors=errorObject instanceof List?(List<?>)errorObject:Collections.emptyList();
        boolean conflicts=!duplicates.isEmpty()||!errors.isEmpty()||!(errorObject instanceof List);
        Map<String,Object> eng=group(rows,engineering,false,conflicts,false),natural=group(rows,quality,false,conflicts,true);
        Map<String,Object> device=group(rows,Arrays.asList("M13","M14","M28","M33","M34"),true,conflicts,false);
        Map<String,Object> synthetic=group(rows,Collections.singletonList("Q08"),false,conflicts,false);
        String overall="FAIL".equals(eng.get("status"))||"FAIL".equals(natural.get("status"))||"FAIL".equals(synthetic.get("status"))?"FAIL_RECORDED_METRICS"
                :conflicts?"INCOMPLETE_SOURCE_OR_METRIC_CONFLICT"
                :"PASS_REPORTED_METRIC_EVIDENCE".equals(eng.get("status"))
                    &&"PASS_REPORTED_METRIC_EVIDENCE".equals(natural.get("status"))
                    &&"PASS_REPORTED_METRIC_EVIDENCE".equals(device.get("status"))
                    &&"PASS_REPORTED_METRIC_EVIDENCE".equals(synthetic.get("status"))?"PASS_REPORTED_METRIC_EVIDENCE":"INCOMPLETE";
        Map<String,Object> out=new LinkedHashMap<>();out.put("version","R33_METRIC_EVIDENCE_ASSESSMENT_2");
        out.put("engineering",eng);out.put("naturalQuality",natural);out.put("physicalDevice",device);out.put("syntheticValidation",synthetic);
        out.put("expectedDistinctMetrics",44);out.put("errorCollectionAvailable",errorObject instanceof List);
        out.put("overallAcceptance",overall);out.put("duplicateMetricIds",duplicates);out.put("sourceErrors",errors);
        out.put("notEvaluatedAsNaturalQuality",Collections.singletonList("Q08_SYNTHETIC_CONTROL"));
        out.put("authority","REPORT_ONLY; ORIGINAL_CERTIFICATE_AND_DECISION_GATES_UNCHANGED");
        return out;
    }
}
