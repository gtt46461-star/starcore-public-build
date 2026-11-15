package com.ethk3.novaonepro.strategyv2;
import java.util.*;

/** Expand the exact frozen marginal table; the selected frozen action remains rank one. No outcomes are inputs. */
public final class FrozenSurfaceR26 {
    public static final class Action {
        public final int ordinal,position,a,b,c,mask,rank;public final double probability;
        private Action(int ordinal,int position,int a,int b,int c,int rank,double probability){this.ordinal=ordinal;this.position=position;this.a=a;this.b=b;this.c=c;mask=(1<<(a-1))|(1<<(b-1))|(1<<(c-1));this.rank=rank;this.probability=probability;}
    }
    private final float[][] table;public final List<Action> actions;
    public FrozenSurfaceR26(float[][] input,int selectedPosition,int selectedMask){
        if(selectedPosition<1||selectedPosition>10||Integer.bitCount(selectedMask)!=3||(selectedMask&~1023)!=0||input==null||input.length!=10)throw new IllegalArgumentException("R26_FROZEN_SURFACE_IDENTITY");
        table=new float[10][10];for(int p=0;p<10;p++){
            if(input[p]==null||input[p].length!=10)throw new IllegalArgumentException("R26_FROZEN_SURFACE_SHAPE");
            for(int n=0;n<10;n++){float q=input[p][n];if(!Float.isFinite(q)||q<0||q>1)throw new IllegalArgumentException("R26_FROZEN_SURFACE_PROBABILITY");table[p][n]=q;}
        }
        for(int p=0;p<10;p++){double row=0,col=0;for(int n=0;n<10;n++){row+=table[p][n];col+=table[n][p];}if(Math.abs(row-1)>1e-4||Math.abs(col-1)>1e-4)throw new IllegalArgumentException("R26_FROZEN_SURFACE_MARGINALS");}
        List<Action> all=new ArrayList<>();int ordinal=0;
        for(int p=1;p<=10;p++)for(int a=1;a<=8;a++)for(int b=a+1;b<=9;b++)for(int c=b+1;c<=10;c++)all.add(new Action(ordinal++,p,a,b,c,0,(double)table[p-1][a-1]+table[p-1][b-1]+table[p-1][c-1]));
        all.sort((a,b)->{boolean selectedA=a.position==selectedPosition&&a.mask==selectedMask,selectedB=b.position==selectedPosition&&b.mask==selectedMask;if(selectedA!=selectedB)return selectedA?-1:1;int probability=Double.compare(b.probability,a.probability);return probability!=0?probability:Integer.compare(a.ordinal,b.ordinal);});
        List<Action> ranked=new ArrayList<>();for(int rank=0;rank<all.size();rank++){Action a=all.get(rank);ranked.add(new Action(a.ordinal,a.position,a.a,a.b,a.c,rank+1,a.probability));}actions=Collections.unmodifiableList(ranked);
    }
    public float[][] table(){float[][] copy=new float[10][];for(int p=0;p<10;p++)copy[p]=table[p].clone();return copy;}
}
