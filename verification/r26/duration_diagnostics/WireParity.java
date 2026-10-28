package com.ethk3.novaonepro.strategyv2;
import java.io.*;
import java.lang.reflect.*;
import java.security.*;
import java.util.*;
/** Preserve every prediction, feature and complete original codec, including clipping and gaps. */
public final class WireParity {
    public static void main(String[] args)throws Exception{
        for(int seed:new int[]{2609,2610}){
            FamilyF09KernelR26 model=new FamilyF09KernelR26();Random random=new Random(seed);if(seed==2610){Field f=FamilyF09KernelR26.class.getDeclaredField("logits");f.setAccessible(true);for(double[] row:(double[][])f.get(model))for(int i=0;i<row.length;i++)row[i]=random.nextBoolean()?7.99:-7.99;}
            ByteArrayOutputStream bytes=new ByteArrayOutputStream();DataOutputStream out=new DataOutputStream(bytes);
            for(int t=0;t<300;t++){
                if(t==23||t==195)model.resetTemporalContext();int[] draw=new int[10];for(int p=0;p<10;p++)draw[p]=p+1;for(int p=9;p>0;p--){int q=random.nextInt(p+1),v=draw[p];draw[p]=draw[q];draw[q]=v;}float[][] p=new float[10][10];float[] features=new float[model.stateWidth()];model.predict(p,features);for(float[] row:p)for(float value:row)out.writeFloat(value);for(float value:features)out.writeFloat(value);model.update(draw);
                if(t==150){ByteArrayOutputStream b=new ByteArrayOutputStream();model.write(new DataOutputStream(b));FamilyF09KernelR26 copy=new FamilyF09KernelR26();copy.read(new DataInputStream(new ByteArrayInputStream(b.toByteArray())));model=copy;}
            }model.write(out);out.flush();StringBuilder hex=new StringBuilder();for(byte value:MessageDigest.getInstance("SHA-256").digest(bytes.toByteArray()))hex.append(String.format(Locale.ROOT,"%02x",value&255));System.out.println("F09_RAW_WIRE_DIGEST|seed="+seed+"|sha256="+hex);
        }
    }
}
