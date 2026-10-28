package com.ethk3.novaonepro.strategyv2;
import java.io.*;
import java.security.*;
import java.nio.*;
import java.util.*;
/** Compare predictions/features and the complete old numeric wire projection. V3 adds counter provenance only. */
public final class WireParity {
    static byte[] projected(NeuralFamilyR26 model)throws Exception{ByteArrayOutputStream b=new ByteArrayOutputStream();model.write(new DataOutputStream(b));byte[] bytes=b.toByteArray();int header=ByteBuffer.wrap(bytes).getInt();if(header==0x26220003){bytes=Arrays.copyOf(bytes,bytes.length-9);ByteBuffer.wrap(bytes).putInt(0x26220002);}else if(header!=0x26220002)throw new AssertionError("Unexpected codec");return bytes;}
    public static void main(String[] args)throws Exception{
        for(String family:new String[]{"F03","F04","F05","F06","F07"})for(int variant=0;variant<3;variant++){
            NeuralFamilyR26 model=new NeuralFamilyR26(family,variant);Random random=new Random(260307);ByteArrayOutputStream bytes=new ByteArrayOutputStream();DataOutputStream out=new DataOutputStream(bytes);
            for(int t=0;t<300;t++){
                if(t==37||t==220)model.resetTemporalContext();int[] draw=new int[10];for(int p=0;p<10;p++)draw[p]=p+1;for(int p=9;p>0;p--){int q=random.nextInt(p+1),value=draw[p];draw[p]=draw[q];draw[q]=value;}
                float[][] probability=new float[10][10];float[] features=new float[model.stateWidth()];model.predict(probability,features);for(float[] row:probability)for(float value:row)out.writeFloat(value);for(float value:features)out.writeFloat(value);model.update(draw);
                if(t==150){ByteArrayOutputStream checkpoint=new ByteArrayOutputStream();model.write(new DataOutputStream(checkpoint));NeuralFamilyR26 restored=new NeuralFamilyR26(family,variant);restored.read(new DataInputStream(new ByteArrayInputStream(checkpoint.toByteArray())));model=restored;}
            }byte[] numeric=projected(model);out.writeInt(numeric.length);out.write(numeric);out.flush();StringBuilder hex=new StringBuilder();for(byte value:MessageDigest.getInstance("SHA-256").digest(bytes.toByteArray()))hex.append(String.format(Locale.ROOT,"%02x",value&255));System.out.println("NEURAL_LEGACY_NUMERIC_PROJECTION_DIGEST|family="+family+"|variant="+variant+"|sha256="+hex);
        }
    }
}
