package com.ethk3.novaonepro.strategyv2;

import java.util.*;

/** Isolated historical controls. Their synthetic ordinals and labels can never enter a live evidence ledger. */
public final class CampaignControlsSU1 {
    public enum Variant { REAL, TIME_SHUFFLED, PLANTED_SIGNAL, NO_DIRECTOR, NO_MEMORY }
    private CampaignControlsSU1() {}
    public static int[][] transform(int[][] source,Variant variant,long seed){
        if(source.length<4840||source.length>20000)throw new IllegalArgumentException("SU1_CONTROL_DATASET_LENGTH");
        int[][] out=new int[source.length][10];for(int t=0;t<source.length;t++){RelationalStateInference.validateDraw(source[t]);out[t]=source[t].clone();}
        Random random=new Random(seed^0x535531434f4e5452L);
        if(variant==Variant.TIME_SHUFFLED){for(int t=out.length-1;t>0;t--){int j=random.nextInt(t+1);int[] swap=out[t];out[t]=out[j];out[j]=swap;}}
        if(variant==Variant.PLANTED_SIGNAL)for(int t=0;t<out.length;t++){
            for(int p=0;p<10;p++)out[t][p]=p+1;for(int p=9;p>0;p--){int q=random.nextInt(p+1),swap=out[t][p];out[t][p]=out[t][q];out[t][q]=swap;}
            if(t>0&&random.nextDouble()<.15){int wanted=out[t-1][3],at=0;while(out[t][at]!=wanted)at++;int swap=out[t][3];out[t][3]=wanted;out[t][at]=swap;}
        }
        return out;
    }
}
