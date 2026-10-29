package com.ethk3.novaonepro.strategyv2;
import java.io.*;
import java.nio.*;
import java.security.*;
import java.util.*;
/** Old default unframed HMM and explicit graph codecs both retain complete numeric projections. */
public final class WireParity {
    static FamilyF08KernelR26 make(int states,boolean memory){return states==0?new FamilyF08KernelR26():new FamilyF08KernelR26(states,memory);}
    static byte[] project(FamilyF08KernelR26 m)throws IOException{ByteArrayOutputStream b=new ByteArrayOutputStream();m.write(new DataOutputStream(b));byte[] bytes=b.toByteArray();if(ByteBuffer.wrap(bytes).getInt()!=0x26080002)return bytes;int k=ByteBuffer.wrap(bytes).getInt(4);byte[] body=Arrays.copyOfRange(bytes,10,bytes.length-9-4-k*k*8);if(bytes[9]!=0)return body;ByteArrayOutputStream old=new ByteArrayOutputStream();DataOutputStream out=new DataOutputStream(old);out.writeInt(0x26080001);out.writeInt(k);out.writeBoolean(bytes[8]!=0);out.write(body);return old.toByteArray();}
    static int[] draw(Random random){int[] d=new int[10];for(int i=0;i<10;i++)d[i]=i+1;for(int i=9;i>0;i--){int q=random.nextInt(i+1),v=d[q];d[q]=d[i];d[i]=v;}return d;}
    public static void main(String[] args)throws Exception{
        for(int k=0;k<=6;k++){if(k==1)continue;for(boolean memory:k==0?new boolean[]{true}:new boolean[]{false,true}){
            FamilyF08KernelR26 model=make(k,memory);Random random=new Random(2608+k);ByteArrayOutputStream bytes=new ByteArrayOutputStream();DataOutputStream out=new DataOutputStream(bytes);
            for(int t=0;t<200;t++){if(t==23||t==140)model.resetTemporalContext();float[][] p=new float[10][10];float[] state=new float[model.stateWidth()];model.predict(p,state);for(float[] row:p)for(float x:row)out.writeFloat(x);for(float x:state)out.writeFloat(x);model.update(draw(random));if(t==75){ByteArrayOutputStream b=new ByteArrayOutputStream();model.write(new DataOutputStream(b));FamilyF08KernelR26 copy=make(k,memory);copy.read(new DataInputStream(new ByteArrayInputStream(b.toByteArray())));model=copy;}}
            out.write(project(model));StringBuilder hex=new StringBuilder();for(byte x:MessageDigest.getInstance("SHA-256").digest(bytes.toByteArray()))hex.append(String.format(Locale.ROOT,"%02x",x&255));System.out.println("F08_OLD_NUMERIC_PROJECTION|states="+k+"|memory="+memory+"|sha256="+hex);
        }}
    }
}
