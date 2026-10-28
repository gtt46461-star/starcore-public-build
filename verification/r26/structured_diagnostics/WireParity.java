package com.ethk3.novaonepro.strategyv2;
import java.io.*;
import java.lang.reflect.*;
import java.security.*;
import java.util.*;
/** Compare complete original predictions/features/raw bytes, including near-clipping weights. */
public final class WireParity {
    static void fill(Object array,Random random){if(array instanceof double[]){double[] a=(double[])array;for(int i=0;i<a.length;i++)a[i]=random.nextBoolean()?7.99:-7.99;}else for(Object child:(Object[])array)fill(child,random);}
    static void plant(TemporalModelR26 model)throws Exception{Random random=new Random(2618);for(String name:model instanceof FamilyF02KernelR26?new String[]{"unary","context","pair"}:new String[]{"bias","context","prefix"}){Field f=model.getClass().getDeclaredField(name);f.setAccessible(true);fill(f.get(model),random);}}
    public static void main(String[] args)throws Exception{
        for(String family:new String[]{"F02","F18"})for(int graph=0;graph<(family.equals("F02")?3:1);graph++)for(boolean factors:family.equals("F02")?new boolean[]{true,false}:new boolean[]{true})for(int seed:new int[]{2617,2618}){
            TemporalModelR26 model=family.equals("F02")?new FamilyF02KernelR26(graph,factors):new FamilyF18KernelR26();if(seed==2618)plant(model);Random random=new Random(seed);ByteArrayOutputStream bytes=new ByteArrayOutputStream();DataOutputStream out=new DataOutputStream(bytes);
            for(int t=0;t<96;t++){
                if(t==13||t==60)model.resetTemporalContext();int[] draw=new int[10];for(int p=0;p<10;p++)draw[p]=p+1;for(int p=9;p>0;p--){int q=random.nextInt(p+1),value=draw[p];draw[p]=draw[q];draw[q]=value;}
                float[][] probability=new float[10][10];float[] state=new float[model.stateWidth()];model.predict(probability,state);for(float[] row:probability)for(float value:row)out.writeFloat(value);for(float value:state)out.writeFloat(value);model.update(draw);
                if(t==40){ByteArrayOutputStream b=new ByteArrayOutputStream();model.write(new DataOutputStream(b));TemporalModelR26 restored=family.equals("F02")?new FamilyF02KernelR26(graph,factors):new FamilyF18KernelR26();restored.read(new DataInputStream(new ByteArrayInputStream(b.toByteArray())));model=restored;}
            }model.write(out);out.flush();StringBuilder hex=new StringBuilder();for(byte value:MessageDigest.getInstance("SHA-256").digest(bytes.toByteArray()))hex.append(String.format(Locale.ROOT,"%02x",value&255));System.out.println("STRUCTURED_WIRE_DIGEST|family="+family+"|graph="+graph+"|factors="+factors+"|seed="+seed+"|sha256="+hex);
        }
    }
}
