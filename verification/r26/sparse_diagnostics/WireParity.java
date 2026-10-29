package com.ethk3.novaonepro.strategyv2;
import java.io.*;
import java.lang.reflect.*;
import java.nio.*;
import java.security.*;
import java.util.*;
/** Compare all lags/cross modes, shrink-to-zero, observation-only context and full old numeric bytes. */
public final class WireParity {
    static byte[] projected(FamilyF01KernelR26 model)throws Exception{ByteArrayOutputStream b=new ByteArrayOutputStream();model.write(new DataOutputStream(b));byte[] bytes=b.toByteArray();int header=ByteBuffer.wrap(bytes).getInt();if(header==0x26010003){bytes=Arrays.copyOf(bytes,bytes.length-9);ByteBuffer.wrap(bytes).putInt(0x26010002);}else if(header!=0x26010002)throw new AssertionError("Unexpected codec");return bytes;}
    static int[] draw(Random random){int[] d=new int[10];for(int i=0;i<10;i++)d[i]=i+1;for(int i=9;i>0;i--){int q=random.nextInt(i+1),v=d[q];d[q]=d[i];d[i]=v;}return d;}
    public static void main(String[] args)throws Exception{
        for(int lags=1;lags<=8;lags++)for(boolean cross:new boolean[]{false,true})for(int seed:new int[]{2601,2602}){
            FamilyF01KernelR26 model=new FamilyF01KernelR26(lags,cross);Random random=new Random(seed);if(seed==2602)for(String name:new String[]{"bias","w"}){Field f=FamilyF01KernelR26.class.getDeclaredField(name);f.setAccessible(true);for(double[] row:(double[][])f.get(model))for(int i=0;i<row.length;i++)row[i]=(random.nextBoolean()?1:-1)*(name.equals("w")?1e-7:.2);}
            ByteArrayOutputStream bytes=new ByteArrayOutputStream();DataOutputStream out=new DataOutputStream(bytes);
            for(int t=0;t<160;t++){
                if(t==23||t==95)model.resetTemporalContext();if(t==17||t==50)model.observeOnly(draw(random));float[][] p=new float[10][10];float[] features=new float[model.stateWidth()];model.predict(p,features);for(float[] row:p)for(float value:row)out.writeFloat(value);for(float value:features)out.writeFloat(value);model.update(draw(random));
                if(t==70){ByteArrayOutputStream b=new ByteArrayOutputStream();model.write(new DataOutputStream(b));FamilyF01KernelR26 copy=new FamilyF01KernelR26(lags,cross);copy.read(new DataInputStream(new ByteArrayInputStream(b.toByteArray())));model=copy;}
            }
            byte[] numeric=projected(model);out.writeInt(numeric.length);out.write(numeric);out.flush();StringBuilder hex=new StringBuilder();for(byte value:MessageDigest.getInstance("SHA-256").digest(bytes.toByteArray()))hex.append(String.format(Locale.ROOT,"%02x",value&255));System.out.println("F01_LEGACY_NUMERIC_PROJECTION_DIGEST|lags="+lags+"|cross="+cross+"|seed="+seed+"|sha256="+hex);
        }
    }
}
