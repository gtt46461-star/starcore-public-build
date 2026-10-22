package com.ethk3.novaonepro.strategyv2;

import com.ethk3.novaonepro.r952.StrictCanonicalJson;
import java.util.*;
import java.nio.file.*;

public final class TrainingParitySU1Test {
    private static int checks;
    private static void same(double value,Object expected){checks++;double want=Double.valueOf((String)expected);if(Math.abs(value-want)>1e-10*(1+Math.abs(want)))throw new AssertionError("training numeric mismatch "+value+" vs "+want);}
    private static void matrix(double[][] a,List<?> expected){int k=0;for(double[] row:a)for(double v:row)same(v,expected.get(k++));if(k!=expected.size())throw new AssertionError("shape");}
    public static void main(String[] args)throws Exception{
        Map<String,Object> fixture=StrictCanonicalJson.readContextObject(Files.readString(Paths.get("reference/fixtures/training_parity.json")));
        TrainingCheckpointSU1 cp=TrainingCheckpointSU1.decode((String)fixture.get("checkpoint"));
        PortableArtifactSU1 portable=PortableArtifactSU1.decode((String)fixture.get("portable"));if(portable.fitted==null||!portable.fitted.hash.equals(cp.hash))throw new AssertionError("portable checkpoint identity");
        List<?> history=(List<?>)fixture.get("history"),label=(List<?>)fixture.get("label");int[][] d=new int[history.size()][10];int[] actual=new int[10];for(int t=0;t<d.length;t++)for(int p=0;p<10;p++)d[t][p]=((Number)((List<?>)history.get(t)).get(p)).intValue();for(int p=0;p<10;p++)actual[p]=((Number)label.get(p)).intValue();
        Map<?,?> layers=(Map<?,?>)fixture.get("layers");RelationalStateInference.predict(cp.weights,d,(name,step,values)->matrix(values,(List<?>)layers.get(name+":"+step)));
        RelationalTrainerSU1.Gradient g=RelationalTrainerSU1.gradient(cp.weights,d,actual,new BudgetTokenSU1(60000,1000000,null));same(g.loss,fixture.get("loss"));
        String[] keys={"E","U","R","Wx","Wm","Wh","Wo"};double[][][] gradients={g.e,g.u,g.r,g.wx,g.wm,g.wh,g.wo};Map<?,?> golden=(Map<?,?>)fixture.get("gradients");
        for(int i=0;i<keys.length;i++)matrix(gradients[i],(List<?>)golden.get(keys[i]));for(int i=0;i<g.b.length;i++)same(g.b[i],((List<?>)golden.get("b")).get(i));for(int i=0;i<10;i++)same(g.c[i],((List<?>)golden.get("c")).get(i));
        RelationalStateInference.Weights next=RelationalTrainerSU1.update(cp.weights,g,.03,1);double[][][] weights={next.e,next.u,next.r,next.wx,next.wm,next.wh,next.wo};golden=(Map<?,?>)fixture.get("updated");for(int i=0;i<keys.length;i++)matrix(weights[i],(List<?>)golden.get(keys[i]));for(int i=0;i<next.b.length;i++)same(next.b[i],((List<?>)golden.get("b")).get(i));for(int i=0;i<10;i++)same(next.c[i],((List<?>)golden.get("c")).get(i));
        System.out.println("SU1_TRAINING_PARITY_PASS values="+checks);
    }
}
