package com.ethk3.novaonepro.strategyv2;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.ethk3.novaonepro.r952.StrictCanonicalJson;
import java.util.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public final class TrainingParityDeviceSU1Test {
    private int checked;
    private void same(double actual,Object expected){checked++;double value=Double.valueOf((String)expected);assertEquals(value,actual,1e-10*(1+Math.abs(value)));}
    private void matrix(double[][] actual,List<?> expected){int i=0;for(double[] row:actual)for(double value:row)same(value,expected.get(i++));assertEquals(expected.size(),i);}
    @Test public void pythonLayersAllGradientsAndOptimizerUpdateMatchOnAndroid()throws Exception{
        Map<String,Object> fixture;
        try(java.io.InputStream input=InstrumentationRegistry.getInstrumentation().getContext().getAssets().open("su1_training_parity.json")){
            java.io.ByteArrayOutputStream bytes=new java.io.ByteArrayOutputStream();byte[] block=new byte[4096];int n;while((n=input.read(block))!=-1)bytes.write(block,0,n);fixture=StrictCanonicalJson.readContextObject(bytes.toString("UTF-8"));
        }
        TrainingCheckpointSU1 cp=TrainingCheckpointSU1.decode((String)fixture.get("checkpoint"));PortableArtifactSU1 portable=PortableArtifactSU1.decode((String)fixture.get("portable"));assertNotNull(portable.fitted);assertEquals(cp.hash,portable.fitted.hash);
        List<?> rows=(List<?>)fixture.get("history"),label=(List<?>)fixture.get("label");int[][] history=new int[rows.size()][10];int[] actual=new int[10];for(int t=0;t<history.length;t++)for(int p=0;p<10;p++)history[t][p]=((Number)((List<?>)rows.get(t)).get(p)).intValue();for(int p=0;p<10;p++)actual[p]=((Number)label.get(p)).intValue();
        Map<?,?> layers=(Map<?,?>)fixture.get("layers");RelationalStateInference.predict(cp.weights,history,(name,step,values)->matrix(values,(List<?>)layers.get(name+":"+step)));
        RelationalTrainerSU1.Gradient gradient=RelationalTrainerSU1.gradient(cp.weights,history,actual,new BudgetTokenSU1(60000,1000000,null));same(gradient.loss,fixture.get("loss"));
        String[] keys={"E","U","R","Wx","Wm","Wh","Wo"};double[][][] gradients={gradient.e,gradient.u,gradient.r,gradient.wx,gradient.wm,gradient.wh,gradient.wo};Map<?,?> golden=(Map<?,?>)fixture.get("gradients");for(int i=0;i<keys.length;i++)matrix(gradients[i],(List<?>)golden.get(keys[i]));for(int i=0;i<gradient.b.length;i++)same(gradient.b[i],((List<?>)golden.get("b")).get(i));for(int i=0;i<10;i++)same(gradient.c[i],((List<?>)golden.get("c")).get(i));
        RelationalStateInference.Weights next=RelationalTrainerSU1.update(cp.weights,gradient,.03,1);double[][][] weights={next.e,next.u,next.r,next.wx,next.wm,next.wh,next.wo};golden=(Map<?,?>)fixture.get("updated");for(int i=0;i<keys.length;i++)matrix(weights[i],(List<?>)golden.get(keys[i]));for(int i=0;i<next.b.length;i++)same(next.b[i],((List<?>)golden.get("b")).get(i));for(int i=0;i<10;i++)same(next.c[i],((List<?>)golden.get("c")).get(i));
        assertTrue(checked>1000);
        TrainingCheckpointSU1 adam=TrainingCheckpointSU1.decode((String)fixture.get("adamCheckpoint"));TrainingCheckpointSU1 adamNext=adam.continueOne(history,actual,adam.datasetHash,"202610210004",new BudgetTokenSU1(60000,1000000,null));next=adamNext.weights;weights=new double[][][]{next.e,next.u,next.r,next.wx,next.wm,next.wh,next.wo};golden=(Map<?,?>)fixture.get("adamUpdated");for(int i=0;i<keys.length;i++)matrix(weights[i],(List<?>)golden.get(keys[i]));for(int i=0;i<next.b.length;i++)same(next.b[i],((List<?>)golden.get("b")).get(i));for(int i=0;i<10;i++)same(next.c[i],((List<?>)golden.get("c")).get(i));
        TrainingCheckpointSU1 adamResume=TrainingCheckpointSU1.decode(adamNext.wire);assertEquals(adamNext.continueOne(history,actual,adam.datasetHash,"202610210005",new BudgetTokenSU1(60000,1000000,null)).wire,adamResume.continueOne(history,actual,adam.datasetHash,"202610210005",new BudgetTokenSU1(60000,1000000,null)).wire);
        TrainingCheckpointSU1 restored=TrainingCheckpointSU1.decode(cp.wire);TrainingCheckpointSU1 a=cp.continueOne(history,actual,cp.datasetHash,"202610210004",new BudgetTokenSU1(60000,1000000,null)),b=restored.continueOne(history,actual,cp.datasetHash,"202610210004",new BudgetTokenSU1(60000,1000000,null));assertEquals(a.wire,b.wire);assertNotEquals(cp.weightsHash,a.weightsHash);
    }
}
