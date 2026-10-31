package com.ethk3.novaonepro.strategyv2;

import static org.junit.Assert.*;
import java.util.*;
import org.junit.Test;

public class R30CalibrationTest {
    @Test public void calibrationPerfectAndBiased(){
        double[] p=new double[1000];int[] y=new int[1000];for(int i=0;i<p.length;i++){p[i]=.3;y[i]=i%10<3?1:0;}
        Map<String,Object> perfect=CalibrationR30.evaluate(p,y,p.length,10,.2,.4);
        assertEquals(0,((Number)perfect.get("eceEqualWidth")).doubleValue(),1e-12);assertEquals(0,((Number)perfect.get("eceEqualMass")).doubleValue(),1e-12);
        assertEquals(.21,((Number)perfect.get("brier")).doubleValue(),1e-12);assertEquals(0,((Number)perfect.get("bss")).doubleValue(),1e-12);assertEquals(true,perfect.get("q05Pass"));
        Arrays.fill(p,.7);Map<String,Object> biased=CalibrationR30.evaluate(p,y,p.length,10,.2,.4);assertEquals(.4,((Number)biased.get("eceEqualMass")).doubleValue(),1e-12);assertEquals(false,biased.get("q05Pass"));assertEquals(0,((Number)biased.get("supportCoverage")).doubleValue(),0);
        assertNull(CalibrationR30.evaluate(p,y,50,10,.2,.4).get("bss"));assertNull(CalibrationR30.evaluate(p,y,50,10,.2,.4).get("q05Pass"));
    }
    static CalibrationRowsR27.Row row(int i,int hit){long open=1_000_000L+i*60_000L;return new CalibrationRowsR27.Row("fixture-"+i,i<150?"REPLAY":"FUTURE",open-5000,open,.2+.1*(i%5),hit,1,true,true,i);}
    static List<CalibrationRowsR27.Row> rows(){List<CalibrationRowsR27.Row> out=new ArrayList<>();for(int i=0;i<650;i++)out.add(row(i,i%10<3?1:0));return out;}
    @Test public void rollingFitIsOrdinalSortedAndCannotUseCurrentOrFutureLabels(){
        List<CalibrationRowsR27.Row> source=rows(),shuffled=new ArrayList<>(source);Collections.shuffle(shuffled,new Random(30));
        CalibrationRowsR27.Result a=CalibrationRowsR27.fitRolling(source),b=CalibrationRowsR27.fitRolling(shuffled);
        assertEquals(500,a.validationRows);assertEquals(120,a.fitRows);assertEquals(JsonR27.write(a.quality),JsonR27.write(b.quality));
        List<CalibrationRowsR27.Row> changed=new ArrayList<>();for(int i=0;i<650;i++)changed.add(row(i,i>=450?(i%10<3?0:1):(i%10<3?1:0)));
        CalibrationRowsR27.Result future=CalibrationRowsR27.fitRolling(changed);
        for(int i=0;i<a.evaluations.size();i++){
            CalibrationRowsR27.Evaluation x=a.evaluations.get(i),z=future.evaluations.get(i);assertTrue(x.fitThroughOrdinal<x.ordinal-1);assertEquals(120,x.fitRows);
            if(x.ordinal<=450){assertEquals(x.probability,z.probability,0);assertEquals(x.fitHash,z.fitHash);}
        }
        assertEquals(0L,a.quality.get("temporalViolations"));assertTrue(a.quality.containsKey("eceEqualMass"));assertTrue(a.quality.containsKey("eceEqualWidth"));assertTrue(a.quality.containsKey("supportCoverage"));
        System.out.println("R30_CALIBRATION|500_ROLLING_FUTURE_EVALUATIONS|120_PRIOR_ROWS_EMBARGO1|CURRENT_AND_FUTURE_LABEL_PERTURBATION_PASS");
    }
    @Test public void identicalConstantProbabilityHistogramsDoNotAcquireOrderDependentBinJitter(){
        List<CalibrationRowsR27.Row> source=new ArrayList<>();
        for(int i=0;i<650;i++){long open=1_000_000L+i*60_000L;source.add(new CalibrationRowsR27.Row("constant-"+i,i<150?"REPLAY":"FUTURE",open-5000,open,.3,i%10<3?1:0,1,true,true,i));}
        CalibrationRowsR27.Result r=CalibrationRowsR27.fitRolling(source);assertEquals(500,r.validationRows);
        double first=r.evaluations.get(0).probability;
        for(CalibrationRowsR27.Evaluation e:r.evaluations){assertEquals(.3,e.probability,1e-12);assertEquals(Double.doubleToLongBits(first),Double.doubleToLongBits(e.probability));}
        assertTrue(((Number)r.quality.get("eceEqualMass")).doubleValue()<=.04);
    }
    @Test public void originalFutureActionWinsOverDifferentCounterfactualReplay(){
        List<CalibrationRowsR27.Row> a=rows(),b=rows();
        b.add(new CalibrationRowsR27.Row("other-k3","REPLAY",0,0,.95,0,1,true,true,400));Collections.shuffle(b,new Random(31));
        CalibrationRowsR27.Result first=CalibrationRowsR27.fitRolling(a),second=CalibrationRowsR27.fitRolling(b);assertEquals(first.evaluations.size(),second.evaluations.size());
        for(int i=0;i<first.evaluations.size();i++){assertEquals(first.evaluations.get(i).probability,second.evaluations.get(i).probability,0);assertEquals(first.evaluations.get(i).fitHash,second.evaluations.get(i).fitHash);}
        assertEquals(0,second.excludedConflicts);assertEquals(1,second.excludedDuplicates);
    }
    @Test public void duplicateConflictUnknownOrdinalAndLateRowsAreExplicitlyExcluded(){
        List<CalibrationRowsR27.Row> rows=rows();rows.add(row(400,1));rows.add(row(401,0));
        rows.add(new CalibrationRowsR27.Row("unknown","FUTURE",9000,14000,.3,1,1,true,true));
        rows.add(new CalibrationRowsR27.Row("late","FUTURE",10000,14999,.3,1,1,true,true,700));
        CalibrationRowsR27.Result r=CalibrationRowsR27.fitRolling(rows);assertEquals(1,r.excludedInvalid);assertEquals(1,r.excludedLate);assertEquals(1,r.excludedConflicts);assertTrue(r.excludedDuplicates>=1);
        for(CalibrationRowsR27.Evaluation e:r.evaluations)assertNotEquals(401,e.ordinal);
        CalibrationRowsR27.Result small=CalibrationRowsR27.fitRolling(rows().subList(0,200));assertEquals("COLLECTING",small.state);assertNull(small.quality.get("bss"));assertNull(small.quality.get("q05Pass"));
    }
}
