package com.ethk3.novaonepro.strategyv2;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;
public final class R30ChartSnapshotTest {
    static Map<String,Object> m(Object... values){Map<String,Object> out=new LinkedHashMap<>();for(int i=0;i<values.length;i+=2)out.put((String)values[i],values[i+1]);return out;}
    static Map<String,Object> generation(int generation,double stored,double detail,boolean verified,Object source){Map<String,Object> h=m("taskKey","frozen-task","datasetHash","d","confirmationOutsideEvolution",true,"pairedLcbVsPrevious",detail,"verified",verified,"status","COMPLETED");if(source!=null)h.put("sourceGeneration",source);return m("generation",(long)generation,"loop","MAIN","dataset_hash","d","paired_lcb",stored,"verified",verified?1L:0L,"detail_json",JsonR27.write(m("holdout",h)));}
    @Test public void pairedLowerUsesIndependentCapturedEvidenceAndNeverRecountsReusedPacket(){
        ReportR27.R30Input in=new ReportR27.R30Input(m("futureActs",12L,"qualifiedExperiments",0L),m("latestIssue","real-captured-issue","factsSha256","captured-id"),m("evolution",Arrays.asList(generation(2,.02,.02,false,1L),generation(1,.02,.02,true,null))),Collections.emptyList());
        Map<String,Object> curve=ReportChartsR30.evolution(in);assertSame(in.watermark,curve.get("snapshotWatermark"));assertEquals(1,curve.get("plottedGenerations"));assertEquals(1,curve.get("verifiedIndependentImprovements"));assertEquals(1,curve.get("reusedEvidenceNotCounted"));assertNull(curve.get("pairedSampleCount"));assertEquals(12L,((Map<?,?>)curve.get("commonMetrics")).get("futureActs"));
        Map<?,?> first=(Map<?,?>)((List<?>)curve.get("points")).get(0);assertEquals(.02,(Double)first.get("value"),0);assertEquals(-1L,first.get("n"));assertNull(first.get("lower"));assertNull(first.get("upper"));
        Map<String,Object> packet=ReportChartsR30.packet(in);assertSame(in.watermark,packet.get("snapshotWatermark"));assertEquals(curve,((List<?>)packet.get("panels")).get(0));
    }
    @Test public void missingOrConflictingEvidenceShowsUnknownOrEmptyInsteadOfZeroCurve(){
        ReportR27.R30Input missing=new ReportR27.R30Input(Collections.emptyMap(),Collections.emptyMap(),Collections.emptyMap(),Collections.emptyList());Map<String,Object> curve=ReportChartsR30.evolution(missing);assertEquals("UNKNOWN_SOURCE_MISSING",curve.get("status"));assertNull(curve.get("plottedGenerations"));assertTrue(((List<?>)curve.get("points")).isEmpty());
        ReportR27.R30Input bad=new ReportR27.R30Input(Collections.emptyMap(),Collections.emptyMap(),m("evolution",Collections.singletonList(generation(1,.02,.03,true,null))),Collections.emptyList());curve=ReportChartsR30.evolution(bad);assertEquals("DATA_CONFLICT",curve.get("status"));assertTrue(((List<?>)curve.get("points")).isEmpty());assertEquals(1,curve.get("sourceConflicts"));
    }
    @Test public void codeCoverageCategoriesAreDisjointAndEffectivenessRemainsUnknown(){
        List<Object> rows=Arrays.asList(m("kind","FAMILY","id","F01","implemented",true,"executable",true),m("kind","FAMILY","id","F23","implemented",true,"executable",false,"executionBlockedReason","ORIGINAL_TEACHER_NOT_BOUND"),m("kind","MODEL","id","NN","implemented",false,"executable",false));
        ReportR27.R30Input in=new ReportR27.R30Input(m("registeredCapabilities",3L),Collections.emptyMap(),m("capabilities",rows),Collections.emptyList());Map<String,Object> coverage=ReportChartsR30.capabilities(in);assertEquals(3,coverage.get("classifiedObjects"));assertNull(coverage.get("validatedEffectiveCount"));assertEquals("UNKNOWN_NOT_VALIDATED",coverage.get("effectiveStatus"));
        long total=0;for(Object item:(List<?>)coverage.get("points")){Map<?,?> p=(Map<?,?>)item;long sum=0;for(Object count:(List<?>)p.get("stack"))sum+=((Number)count).longValue();assertEquals(((Number)p.get("value")).longValue(),sum);total+=sum;}assertEquals(3,total);
    }
    @Test public void realZeroReferenceIsVisibleWithoutInventingAPointOrInterval(){
        double[] range=com.ethk3.novaonepro.r952.ChartGeometryR82.range(new double[]{.11,.12,Double.NaN},Arrays.asList(0.0,Double.NaN));assertEquals(0,range[0],0);assertEquals(.12,range[1],0);assertEquals(.11,com.ethk3.novaonepro.r952.ChartGeometryR82.range(new double[]{.11,.12})[0],0);
    }
}
