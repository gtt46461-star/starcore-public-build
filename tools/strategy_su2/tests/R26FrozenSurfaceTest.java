package com.ethk3.novaonepro.strategyv2;
import java.util.*;
/** Frozen expansion/selection invariants, independent action enumeration and invalid producer counterexamples. */
public final class R26FrozenSurfaceTest {
    static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    public static void main(String[] args){
        float[][] table=new float[10][10];for(float[] row:table)Arrays.fill(row,.1f);int mask=(1<<1)|(1<<4)|(1<<8);
        FrozenSurfaceR26 surface=new FrozenSurfaceR26(table,7,mask);check(surface.actions.size()==1200,"incomplete action space");FrozenSurfaceR26.Action head=surface.actions.get(0);check(head.position==7&&head.mask==mask&&head.rank==1,"replaced frozen choice with new inference");
        Set<String> identities=new HashSet<>();Set<Integer> ordinals=new HashSet<>();int expectedRank=1;for(FrozenSurfaceR26.Action a:surface.actions){check(a.a<a.b&&a.b<a.c&&Integer.bitCount(a.mask)==3,"invalid K3");check(identities.add(a.position+":"+a.mask)&&ordinals.add(a.ordinal),"duplicate action identity");check(a.rank==expectedRank++,"discontinuous ranks");check(Math.abs(a.probability-.3)<1e-7,"K3 is not a union marginal");}
        for(int p=1;p<=10;p++)for(int a=1;a<=8;a++)for(int b=a+1;b<=9;b++)for(int c=b+1;c<=10;c++)check(identities.contains(p+":"+((1<<(a-1))|(1<<(b-1))|(1<<(c-1)))),"enumeration omitted action");
        table[0][0]=.9f;check(Math.abs(surface.table()[0][0]-.1)<1e-6,"external map mutated frozen evidence");float[][] copy=surface.table();copy[1][1]=.9f;check(Math.abs(surface.table()[1][1]-.1)<1e-6,"caller modified frozen table");
        try{new FrozenSurfaceR26(table,7,mask);throw new AssertionError("illegal marginals accepted");}catch(IllegalArgumentException expected){}
        table=surface.table();table[2][3]=Float.NaN;try{new FrozenSurfaceR26(table,7,mask);throw new AssertionError("nonfinite probability accepted");}catch(IllegalArgumentException expected){}
        try{new FrozenSurfaceR26(surface.table(),7,3);throw new AssertionError("non-K3 selected mask accepted");}catch(IllegalArgumentException expected){}
        check(ReportMetricContractR26.sections()==40,"report section denominator changed");int metrics=0;for(String id:new String[]{"IDENTITY","ACTION_SURFACE","EVOLUTION","FAMILIES","EXPERTS"})metrics+=ReportMetricContractR26.metrics(id).size();check(metrics>25,"required metrics omitted");
        System.out.println("R26_FROZEN_SURFACE_100_1200_BOUND_CHOICE_IMMUTABILITY_PASS");
    }
}
