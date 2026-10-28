package com.ethk3.novaonepro.strategyv2;
import java.io.*;
import java.security.*;
import java.util.*;
/** Run against pre/post classpaths; compare all digests without calling the new diagnostics. */
public final class WireParity {
    public static void main(String[] args)throws Exception{
        for(FamilyF20KernelR26.Retrieval mode:FamilyF20KernelR26.Retrieval.values())for(int slots:new int[]{8,32}){
            FamilyF20KernelR26 model=new FamilyF20KernelR26(slots,mode);MessageDigest hash=MessageDigest.getInstance("SHA-256");Random random=new Random(262020);ByteArrayOutputStream all=new ByteArrayOutputStream();DataOutputStream out=new DataOutputStream(all);
            for(int t=0;t<1000;t++){
                if(t==113||t==600)model.resetTemporalContext();int[] draw=new int[10];for(int p=0;p<10;p++)draw[p]=p+1;for(int p=9;p>0;p--){int k=random.nextInt(p+1),value=draw[p];draw[p]=draw[k];draw[k]=value;}
                float[][] probability=new float[10][10];float[] state=new float[model.stateWidth()];model.predict(probability,state);for(float[] row:probability)for(float value:row)out.writeFloat(value);for(float value:state)out.writeFloat(value);model.update(draw);
                if(t==500){ByteArrayOutputStream bytes=new ByteArrayOutputStream();model.write(new DataOutputStream(bytes));FamilyF20KernelR26 restored=new FamilyF20KernelR26(slots,mode);restored.read(new DataInputStream(new ByteArrayInputStream(bytes.toByteArray())));model=restored;}
            }model.write(out);out.flush();byte[] digest=hash.digest(all.toByteArray());StringBuilder hex=new StringBuilder();for(byte b:digest)hex.append(String.format(java.util.Locale.ROOT,"%02x",b&255));System.out.println("F20_WIRE_DIGEST|mode="+mode+"|slots="+slots+"|sha256="+hex);
        }
    }
}
