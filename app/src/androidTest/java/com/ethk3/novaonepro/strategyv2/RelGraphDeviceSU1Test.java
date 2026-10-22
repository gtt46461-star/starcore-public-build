package com.ethk3.novaonepro.strategyv2;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.ethk3.novaonepro.r952.StrictCanonicalJson;
import java.util.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public final class RelGraphDeviceSU1Test {
    private int checked;
    private void same(double actual,Object expected,double tolerance){checked++;double value=Double.valueOf((String)expected);assertEquals(value,actual,tolerance*(1+Math.abs(value)));}
    private void vector(double[] values,List<?> expected,double tolerance){assertEquals(values.length,expected.size());for(int i=0;i<values.length;i++)same(values[i],expected.get(i),tolerance);}
    @Test public void realRelationGraphLayersAllGradientsAndOptimizerResumeMatchIndependentPython()throws Exception{
        Map<String,Object> f;try(java.io.InputStream input=InstrumentationRegistry.getInstrumentation().getContext().getAssets().open("su1_relgraph_parity.json")){
            java.io.ByteArrayOutputStream bytes=new java.io.ByteArrayOutputStream();byte[] block=new byte[4096];int n;while((n=input.read(block))!=-1)bytes.write(block,0,n);f=StrictCanonicalJson.readContextObject(bytes.toString("UTF-8"));
        }
        RelGraphModelSU1.Input input=RelGraphModelSU1.decodeInput((String)f.get("input"),(String)f.get("inputHash"));RelGraphOptimizerSU1 state=RelGraphCodecSU1.decode((String)f.get("state"),(String)f.get("stateHash"));Map<?,?> layers=(Map<?,?>)f.get("layers");
        double[][] p=RelGraphModelSU1.predict(state.weights,input,new BudgetTokenSU1(60000,1000000,null),(name,step,values)->vector(values,(List<?>)layers.get(name+":"+step),1e-10));List<?> expected=(List<?>)f.get("probability");for(int pos=0;pos<10;pos++)for(int n=0;n<10;n++)same(p[pos][n],expected.get(pos*10+n),1e-10);
        List<?> labels=(List<?>)f.get("label");int[] actual=new int[10];for(int i=0;i<10;i++)actual[i]=((Number)labels.get(i)).intValue();RelGraphModelSU1.Gradient gradient=RelGraphModelSU1.gradient(state.weights,input,actual,new BudgetTokenSU1(60000,1000000,null));same(gradient.loss,f.get("loss"),1e-10);
        Map<?,?> golden=(Map<?,?>)f.get("gradients");for(String key:RelGraphModelSU1.GROUPS){vector(gradient.tensors.get(key),(List<?>)golden.get(key),2e-7);assertTrue(gradient.norm(key)>0);}
        RelGraphModelSU1.Weights updated=RelGraphModelSU1.update(state.weights,gradient,.03,1);golden=(Map<?,?>)f.get("updated");for(String key:RelGraphModelSU1.GROUPS)vector(updated.get(key),(List<?>)golden.get(key),1e-8);
        RelGraphOptimizerSU1 learned=state.apply(gradient,.001,1);String wire=RelGraphCodecSU1.encode(learned);RelGraphOptimizerSU1 restored=RelGraphCodecSU1.decode(wire,HashingSU1.text(wire));assertEquals(wire,RelGraphCodecSU1.encode(restored));RelGraphModelSU1.Gradient next=RelGraphModelSU1.gradient(learned.weights,input,actual,new BudgetTokenSU1(60000,1000000,null));assertEquals(RelGraphCodecSU1.encode(learned.apply(next,.001,1)),RelGraphCodecSU1.encode(restored.apply(next,.001,1)));
        assertTrue(checked>3500);
        int[][] history=new int[120][10],future=new int[120][10];for(int t=0;t<120;t++)for(int pos=0;pos<10;pos++){history[t][pos]=(t+pos)%10+1;future[t][pos]=history[t][pos];}for(int t=100;t<120;t++){int v=future[t][0];future[t][0]=future[t][1];future[t][1]=v;}
        double[][] a=ProgramTreeSU1.parse("GDEEP(2,4,19)").score(new ProgramTreeSU1.Context(history,100)),b=ProgramTreeSU1.parse("GDEEP(2,4,19)").score(new ProgramTreeSU1.Context(future,100));for(int pos=0;pos<10;pos++)assertArrayEquals(a[pos],b[pos],0);
    }
}
