package com.ethk3.novaonepro.strategyv2;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** The same result object is exported to JSON/HTML. Missing evidence never becomes a number or PASS. */
public final class MetricEvaluatorR29 {
    private MetricEvaluatorR29() {}
    public static final class Probe {
        public final String status, reason, population;
        public final Number numerator, denominator, value;
        public final long samples;
        public final List<String> evidence;
        public Probe(String status, String reason, String population, Number numerator, Number denominator, Number value, long samples, List<String> evidence) {
            if (samples < 0) throw new IllegalArgumentException("R29_NEGATIVE_SAMPLE_COUNT");
            if ("PASS".equals(status) && (value == null || samples == 0 || evidence == null || evidence.isEmpty())) throw new IllegalArgumentException("R29_PASS_WITHOUT_EVIDENCE");
            this.status=status; this.reason=reason; this.population=population; this.numerator=numerator; this.denominator=denominator; this.value=value; this.samples=samples; this.evidence=evidence;
        }
    }
    @SuppressWarnings("unchecked")
    public static List<Map<String,Object>> evaluate(Map<String,Probe> probes, Map<String,Object> snapshot) {
        List<Map<String,Object>> out=new ArrayList<>();
        for(Object obj:(List<Object>)JsonR27.parse(MetricDefinitionsR29.JSON)) {
            Map<String,Object> def=(Map<String,Object>)obj; String id=(String)def.get("id"); Probe p=probes.get(id);
            Map<String,Object> r=new LinkedHashMap<>(def);
            r.put("metricId", id); r.put("definitionVersion", "R29.1"); r.put("sourceSnapshot", snapshot);
            r.put("status",p==null ? (id.equals("Q02")?"NOT_DEPLOYED":id.startsWith("Q")?"INSUFFICIENT":"NOT_RUN") : p.status);
            r.put("population",p==null?null:p.population); r.put("numerator",p==null?null:p.numerator); r.put("denominator",p==null?null:p.denominator);
            r.put("value",p==null?null:p.value); r.put("sampleCount",p==null?null:p.samples);
            r.put("missingReason",p==null?"No source-bound execution receipt for "+def.get("source"):p.reason);
            r.put("evidenceRefs",p==null||p.evidence==null?new ArrayList<>():p.evidence);
            r.put("timeRange",snapshot.get("timeRange")); r.put("uncertainty",null);
            r.put("nextTrigger",p==null?"Collect the specified runtime/device/quality source; do not infer completion from compilation":"Re-evaluate on the next committed snapshot");
            List<String> repair=new ArrayList<>();
            for(int i=1;i<=52;i++) if (cardMetrics(i).contains("|"+id+"|")) repair.add(String.format(java.util.Locale.ROOT,"R29-%03d",i));
            r.put("repairCardIds",repair); out.add(r);
        }
        return out;
    }
    private static String cardMetrics(int card) { return MetricCardMapR29.VALUES[card-1]; }
}
