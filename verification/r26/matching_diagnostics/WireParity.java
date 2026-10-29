package com.ethk3.novaonepro.strategyv2;
import java.io.*;
import java.security.*;
import java.util.*;
/** No diagnostic API calls: compare old/new predictions, features and raw checkpoints. */
public final class WireParity {
    public static void main(String[] args)throws Exception{
        for(double lr:new double[]{.01,.02,.05})for(int seed:new int[]{2617,817}){
            PermutationModelR26 model=new PermutationModelR26(lr);Random random=new Random(seed);ByteArrayOutputStream bytes=new ByteArrayOutputStream();DataOutputStream out=new DataOutputStream(bytes);
            for(int t=0;t<1000;t++){
                if(t==113||t==600)model.resetTemporalContext();int[] draw=new int[10];for(int p=0;p<10;p++)draw[p]=p+1;for(int p=9;p>0;p--){int q=random.nextInt(p+1),value=draw[p];draw[p]=draw[q];draw[q]=value;}
                float[][] probability=new float[10][10];float[] state=new float[model.stateWidth()];model.predict(probability,state);for(float[] row:probability)for(float value:row)out.writeFloat(value);for(float value:state)out.writeFloat(value);model.update(draw);
                if(t==500){ByteArrayOutputStream checkpoint=new ByteArrayOutputStream();model.write(new DataOutputStream(checkpoint));PermutationModelR26 restored=new PermutationModelR26(lr);restored.read(new DataInputStream(new ByteArrayInputStream(checkpoint.toByteArray())));model=restored;}
            }model.write(out);out.flush();StringBuilder hex=new StringBuilder();for(byte value:MessageDigest.getInstance("SHA-256").digest(bytes.toByteArray()))hex.append(String.format(Locale.ROOT,"%02x",value&255));System.out.println("F17_WIRE_DIGEST|lr="+lr+"|seed="+seed+"|sha256="+hex);
        }
    }
}
