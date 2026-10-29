package com.ethk3.novaonepro.strategyv2;
import java.io.*;
import java.util.*;

/** Independent batch-count oracle over 10,000 synthetic labels. Kernel scope only. */
public final class ScaleWindowRetention {
    static void require(boolean value,String message){if(!value)throw new AssertionError(message);}
    static byte[] save(ScaleEncoderR26 model)throws IOException{ByteArrayOutputStream b=new ByteArrayOutputStream();model.write(new DataOutputStream(b));return b.toByteArray();}
    static int[] draw(Random r){int[] d={1,2,3,4,5,6,7,8,9,10};for(int p=9;p>0;p--){int q=r.nextInt(p+1),x=d[p];d[p]=d[q];d[q]=x;}return d;}
    static Object field(ScaleEncoderR26 model,String name)throws Exception{java.lang.reflect.Field f=ScaleEncoderR26.class.getDeclaredField(name);f.setAccessible(true);return f.get(model);}
    static int checks;
    static void oracle(ScaleEncoderR26 model,int[][] history,int observed)throws Exception{
        int[][][] actual=(int[][][])field(model,"counts");int expectedMask=0;
        for(int scale=0;scale<6;scale++){
            int window=ScaleEncoderR26.SCALES[scale];if(observed>=window)expectedMask|=1<<scale;
            int[][] expected=new int[10][10];for(int i=Math.max(0,observed-window);i<observed;i++)for(int p=0;p<10;p++)expected[p][history[i][p]-1]++;
            for(int p=0;p<10;p++)for(int n=0;n<10;n++){checks++;require(actual[scale][p][n]==expected[p][n],"window mismatch at "+observed+" scale="+window+" position="+p+" digit="+n);}
        }
        require(model.configuredScaleMask()==63&&model.availableScaleMask()==expectedMask,"declared mask differs from actual causal support");
        require(((int[][])field(model,"ring")).length==5000,"raw kernel history grew beyond 5000");
    }
    public static void main(String[] args)throws Exception{
        int[][] history=new int[10000][];Random random=new Random(26045000);for(int i=0;i<history.length;i++)history[i]=draw(random);
        ScaleEncoderR26 model=new ScaleEncoderR26(true),copy=null;int initialSize=save(model).length;long start=System.nanoTime();
        Set<Integer> checkpoints=new HashSet<>(Arrays.asList(7,8,31,32,127,128,511,512,2047,2048,4999,5000,5017,6000,10000));
        for(int i=0;i<history.length;i++){
            model.update(history[i]);if(copy!=null)copy.update(history[i]);int observed=i+1;
            if(observed==5017){copy=new ScaleEncoderR26(true);copy.read(new DataInputStream(new ByteArrayInputStream(save(model))));}
            if(checkpoints.contains(observed))oracle(model,history,observed);
            if(observed==6000||observed==10000)require(Arrays.equals(save(model),save(copy)),"exact next-state recovery failed after the raw ring wrapped");
        }
        require(save(model).length==initialSize&&initialSize<256000,"checkpoint grew with lifetime history");double[] learned=((double[])field(model,"logits")).clone();model.resetTemporalContext();require(model.availableScaleMask()==0,"gap retained unavailable raw support");require(Arrays.equals(learned,(double[])field(model,"logits")),"gap discarded learned scale-router parameters");
        for(int i=0;i<32;i++)model.update(history[i]);require(model.availableScaleMask()==3,"gap did not rebuild only the currently observed scales");
        System.out.println("R26_SCALE_10000_KERNEL_PASS|independentCountAssertions="+checks+"|rawRingCapacity=5000|constantCheckpointBytes="+initialSize+"|restoreAfter=5017|allSixScalesObserved=true|wallMs="+(System.nanoTime()-start)/1000000+"|scope=SYNTHETIC_KERNEL_ONLY_NOT_FULL_APP_RETENTION_OR_PHONE_SLO");
    }
}
