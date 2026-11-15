package com.ethk3.novaonepro.strategyv2;
import java.util.*;
public final class AssessmentBoundaryProbe {
 static int checks,failures;static final String H="a".repeat(64);
 static Map<String,Object> m(Object...kv){Map<String,Object>r=new LinkedHashMap<>();for(int i=0;i<kv.length;i+=2)r.put((String)kv[i],kv[i+1]);return r;}
 static Map<?,?> map(Object o){return (Map<?,?>)o;}
 static void check(boolean x,String s){checks++;if(!x){failures++;System.out.println("FAIL "+s);}}
 static Map<String,Object> def(String id){for(Map<String,Object>d:MetricDefinitionsR29.r31Definitions())if(id.equals(d.get("id")))return d;throw new AssertionError(id);}
 static List<Map<String,Object>> passing(){List<Map<String,Object>> out=new ArrayList<>();for(Map<String,Object>d:MetricDefinitionsR29.r31Definitions()){
  String id=(String)d.get("id"),role="Q08".equals(id)?"SYNTHETIC":id.startsWith("Q")?"NATURAL_FUTURE":"ENGINEERING_RUNTIME";long n="Q08".equals(id)?120:10000;
  Map<String,Object> e=MetricEvaluatorR29.evaluateR31(d,new MetricEvaluatorR29.MeasurementR31(H,role,n,1.0,true));
  out.add(m("metricId",id,"status","PASS","role",role,"sourceReceiptSha256",H,"sampleCount",n,"value",1.0,"definitionVersion",d.get("version"),"r31Definition",d,"r31Evaluation",e));}return out;}
 static Map<String,Object> row(List<Map<String,Object>>r,String id){for(Map<String,Object>x:r)if(id.equals(x.get("metricId")))return x;throw new AssertionError(id);}
 static Map<String,Object> assess(List<Map<String,Object>>r){return ReportAssessmentR33.evaluate(r,Collections.emptyList());}
 static boolean passes(List<Map<String,Object>>r){return "PASS_REPORTED_METRIC_EVIDENCE".equals(assess(r).get("overallAcceptance"));}
 public static void main(String[]args){
  List<Map<String,Object>> r=passing();check(passes(r),"all exact metric receipts can advance");
  r=passing();row(r,"Q08").put("status","FAIL");check("FAIL_RECORDED_METRICS".equals(assess(r).get("overallAcceptance")),"Q08 failure contributes to overall");
  r=passing();r.remove(row(r,"Q08"));check(!passes(r),"missing Q08 blocks overall pass");
  r=passing();row(r,"M01").put("value",99.0);check(!passes(r),"top value must match evaluated value");
  r=passing();row(r,"M01").put("sampleCount",9999);check(!passes(r),"top sample count must match receipt");
  r=passing();((Map<String,Object>)row(r,"M01").get("r31Evaluation")).put("id","M02");check(!passes(r),"wrong receipt metric ID rejected");
  r=passing();((Map<String,Object>)row(r,"M01").get("r31Definition")).put("id","M02");check(!passes(r),"wrong definition metric ID rejected");
  r=passing();((Map<String,Object>)row(r,"M01").get("r31Evaluation")).put("minimumSample",0);check(!passes(r),"minimum sample must be positive and definition matched");
  r=passing();((Map<String,Object>)row(r,"M01").get("r31Evaluation")).put("version","foreign");check(!passes(r),"foreign evaluation version rejected");
  r=passing();row(r,"M01").put("status","FAIL");r.remove(row(r,"M02"));Map<String,Object>a=assess(r);check("FAIL".equals(map(a.get("engineering")).get("status")),"known failure not obscured by missing metric");check("FAIL_RECORDED_METRICS".equals(a.get("overallAcceptance")),"known failure visible overall with missing metric");
  r=passing();row(r,"M34").put("status","FAIL");check("FAIL".equals(map(assess(r).get("physicalDevice")).get("status")),"physical device group includes upgrade install");
  r=passing();row(r,"M33").put("status","FAIL");check("FAIL".equals(map(assess(r).get("physicalDevice")).get("status")),"physical device group includes chart UI");
  r=passing();row(r,"Q01").put("role","SYNTHETIC");check(!passes(r),"synthetic cannot pass natural quality");
  r=passing();row(r,"Q08").put("role","NATURAL_FUTURE");check(!passes(r),"natural period counts cannot masquerade as synthetic trials");
  r=passing();check(!"PASS_REPORTED_METRIC_EVIDENCE".equals(ReportAssessmentR33.evaluate(r,null).get("overallAcceptance")),"unknown error collection blocks pass");
  Map<String,Object>d=def("Q08");check(((Number)d.get("minimumSample")).longValue()==120,"Q08 minimum is registered six arms times twenty seeds");check("SYNTHETIC_ONLY".equals(d.get("role")),"Q08 correct definition role");check(String.valueOf(d.get("denominator")).contains("ARM_SEED"),"Q08 trial denominator explicit");
  check(CheckMetricCrosswalkR31.metric("Q08").requiredEvidence.startsWith("REGISTERED_SYNTHETIC"),"Q08 crosswalk aligns source");
  Map<String,Object>e=MetricEvaluatorR29.evaluateR31(d,new MetricEvaluatorR29.MeasurementR31(H,"SYNTHETIC",119,1.0,true));check("INSUFFICIENT_BELOW_MINIMUM_SAMPLE".equals(e.get("status")),"incomplete trial set not pass");
  e=MetricEvaluatorR29.evaluateR31(d,new MetricEvaluatorR29.MeasurementR31(H,"SYNTHETIC",120,1.0,false));check("FAIL".equals(e.get("status")),"complete count cannot replace original protocol outcome");
  for(int n=1;n<=7;n++){d=def(String.format(Locale.ROOT,"Q%02d",n));check(((Number)d.get("minimumSample")).longValue()==(n==5?200:n==7?1:250)&&"NATURAL_FUTURE_ONLY".equals(d.get("role")),"natural role unchanged and typed report support Q"+n);}
  e=MetricEvaluatorR29.evaluateR31(def("Q01"),new MetricEvaluatorR29.MeasurementR31(H,"SYNTHETIC",10000,1.0,true));check("NOT_PROVEN_NOT_NATURAL_ROLE".equals(e.get("status")),"natural evaluator excludes synthetic role");
  e=MetricEvaluatorR29.evaluateR31(def("M01"),new MetricEvaluatorR29.MeasurementR31(H,"ENGINEERING_RUNTIME",10000,Double.NaN,true));check("INVALID_NONFINITE_OR_MISSING_VALUE".equals(e.get("status")),"nonfinite value cannot pass metric evaluator");
  System.out.println("RESULT checks="+checks+" failures="+failures);if(failures!=0)System.exit(1);
 }
}
