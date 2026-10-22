package com.ethk3.novaonepro;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import com.ethk3.novaonepro.LiveResearchDiagnosisOrchestrator.*;
/** Actual StageArtifact -> deriveFindings path. Metrics are explicit fixtures, not a device run. */
public final class runtime_diagnosis {
    static int checks;
    static void check(boolean ok,String label){if(!ok)throw new AssertionError(label);checks++;System.out.println("PASS|"+label);}
    static Metric m(String key,String value,boolean available){return new Metric(key,value,"fixture","R76_REPORT_METRIC_FIXTURE",available);}
    static ArrayList<Metric> metrics(String business,String root,String failed,String p95,String concurrency,String oom){
        ArrayList<Metric> out=new ArrayList<Metric>();out.add(m("serviceAlive","1",true));out.add(m("businessAlive",business,true));out.add(m("rootCause",root,true));
        out.add(m("latestAttemptFailed",failed,true));out.add(m("requestToCommitP95",p95,true));out.add(m("maxConcurrency",concurrency,true));out.add(m("oomCount",oom,true));return out;
    }
    @SuppressWarnings("unchecked") static Finding finding(Status status,List<Metric> metrics)throws Exception {
        StageArtifact stage=new StageArtifact(Stage.RUNTIME,status,"ACTUAL_OWNER","EXPLICIT_METRIC_FIXTURE","HEALTHY|NONE|OK","","INJECTED_READ_ERROR",1L,metrics,Collections.<Evidence>emptyList());
        Method derive=LiveResearchDiagnosisOrchestrator.class.getDeclaredMethod("deriveFindings",List.class);derive.setAccessible(true);
        ArrayList<Finding> findings=(ArrayList<Finding>)derive.invoke(new LiveResearchDiagnosisOrchestrator(),Collections.singletonList(stage));
        check(findings.size()==1&&findings.get(0).status==status,"FINDING_PRESERVES_SUPPLIED_STAGE_STATUS_"+status);return findings.get(0);
    }
    static Metric latest(String read,String type,long at,long last)throws Exception {
        ModelEffectivenessAnalyzerR35.RuntimeSummary summary=new ModelEffectivenessAnalyzerR35.RuntimeSummary(read,9,26214L,1,0,"SUCCESS","202610130484",last,type,"STATUS","202610130484",at,"",1L,9,26214L,1,0,"");
        Method method=LiveResearchDiagnosisOrchestrator.class.getDeclaredMethod("latestAttemptMetricR76",ModelEffectivenessAnalyzerR35.RuntimeSummary.class);method.setAccessible(true);return (Metric)method.invoke(null,summary);
    }
    public static void main(String[] args)throws Exception {
        Finding sla=finding(Status.FAIL,metrics("1","NONE","0","26214","1","0"));
        check(sla.cause.contains("requestToCommitP95=26214")&&sla.cause.contains("5000"),"REPORT_P95_FAILURE_NAMES_ACTUAL_METRIC_AND_THRESHOLD");
        check(sla.cause.contains("businessAlive=1")&&sla.cause.contains("rootCause=NONE"),"HEALTHY_BUSINESS_CONTEXT_REMAINS_VISIBLE");
        check(sla.impact.contains("当前服务与业务存活")&&sla.impact.contains("时延")&&!sla.impact.contains("service failure"),"SLA_FAILURE_DOES_NOT_CLAIM_STOPPED_SERVICE");
        Finding all=finding(Status.FAIL,metrics("1","NONE","1","7000","2","3"));
        for(String item:new String[]{"latestAttemptFailed=1","requestToCommitP95=7000","maxConcurrency=2","oomCount=3"})check(all.cause.contains(item),"EVERY_OBSERVED_RUNTIME_FAILURE_REPORTED_"+item);
        Finding chain=finding(Status.FAIL,metrics("0","RUNTIME_CHAIN","0","1000","1","0"));
        check(chain.cause.contains("businessAlive=0")&&chain.cause.contains("rootCause=RUNTIME_CHAIN")&&chain.impact.contains("业务链异常"),"ACTUAL_BUSINESS_FAILURE_IS_DISTINCT");
        Finding settle=finding(Status.FAIL,metrics("1","SETTLEMENT_OOS","0","1000","1","0"));
        check(settle.cause.contains("rootCause=SETTLEMENT_OOS")&&settle.impact.contains("业务链异常"),"SETTLEMENT_ROOT_CAUSE_NOT_HIDDEN_BY_ALIVE_FLAG");
        ArrayList<Metric> unknown=metrics("1","NONE","0","26214","1","0");unknown.set(4,m("requestToCommitP95","26214",false));
        Finding missing=finding(Status.FAIL,unknown);
        check(!missing.cause.contains("26214")&&missing.cause.contains("未知")&&missing.cause.contains("失败指标"),"UNAVAILABLE_VALUE_CANNOT_BECOME_HEALTHY_OR_SPECIFIC_FAILURE");
        Finding read=finding(Status.READ_ERROR,Collections.<Metric>emptyList());
        check(read.cause.contains("INJECTED_READ_ERROR")&&read.impact.contains("读取失败"),"READ_ERROR_STAYS_EXPLICIT");
        Finding boundary=finding(Status.PASS,metrics("1","NONE","0","5000","1","0"));
        check(!boundary.impact.contains("service failure")&&!boundary.impact.contains("时延超出"),"EXISTING_PASS_BOUNDARY_NOT_CHANGED");
        check("0".equals(latest("OK","ANALYSIS_ATTEMPT_FAILED",99L,100L).value),"OLDER_FAILURE_NOT_MARKED_LATEST");
        check("1".equals(latest("OK","ANALYSIS_ATTEMPT_FAILED",100L,100L).value),"EXACT_LATEST_FAILURE_PREDICATE_MATCHES_EXISTING_GATE");
        check("0".equals(latest("OK","ANALYSIS_DURABLE_RETURNED_R923",101L,100L).value),"LATER_SUCCESS_IS_NOT_LATEST_FAILURE");
        check(!latest("READ_ERROR","ANALYSIS_ATTEMPT_FAILED",101L,100L).available,"UNREAD_RUNTIME_METRIC_UNAVAILABLE");
        String result="{\"status\":\"PASS\",\"checks\":"+checks+",\"scope\":\"ACTUAL_STAGE_ARTIFACT_DERIVE_FINDINGS_EXPLICIT_METRICS_NOT_DEVICE\"}\n";
        Files.writeString(Path.of(args[0]),result);System.out.print(result);
    }
}
