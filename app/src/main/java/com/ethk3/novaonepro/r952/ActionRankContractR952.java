package com.ethk3.novaonepro.r952;

import java.util.Arrays;
import java.util.Comparator;

public final class ActionRankContractR952 {
    public static final int ACTIONS=1200;
    private ActionRankContractR952(){}
    public static int[] rank(final double[] scores){if(scores==null||scores.length!=ACTIONS)throw new IllegalArgumentException("R952_RANK_1200_REQUIRED");Integer[] ids=new Integer[ACTIONS];for(int i=0;i<ACTIONS;i++){if(Double.isNaN(scores[i])||Double.isInfinite(scores[i]))throw new IllegalArgumentException("R952_RANK_NONFINITE");ids[i]=Integer.valueOf(i);}Arrays.sort(ids,new Comparator<Integer>(){public int compare(Integer a,Integer b){int c=Double.compare(scores[b.intValue()],scores[a.intValue()]);return c!=0?c:a.intValue()-b.intValue();}});int[] out=new int[ACTIONS];for(int i=0;i<ACTIONS;i++)out[i]=ids[i].intValue();return out;}
}
