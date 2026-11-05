package com.ethk3.novaonepro.strategyv2;
import java.util.*;

/** Exhaustive singles first, then systematic conditional combinations of the measured top eight. */
public final class AtomWorldScreenerSU1 {
    private AtomWorldScreenerSU1() {}
    public static List<String> singles(){
        List<String> out=new ArrayList<>();for(int e=0;e<8;e++){out.add("EXPERT("+e+")");out.add("NEG(EXPERT("+e+"))");}
        for(String k:new String[]{"SAME","PLUS1","MINUS1","MIRROR"})for(int l=1;l<=3;l++)for(int o=0;o<10;o++)out.add("REL("+k+","+l+","+o+",1)");
        for(int l=1;l<=2;l++)for(int o=0;o<10;o++)for(int o2=1;o2<10;o2++)out.add("REL(SUM,"+l+","+o+","+o2+")");
        for(int o=0;o<10;o++)out.add("XT("+o+")");for(String atom:AtomCatalogSU1.scoreAtoms()){out.add(atom);out.add("NEG("+atom+")");}return out;
    }
    public static List<String> conditionals(String atom,long seed){
        Random rng=new Random(seed);String zero="SUB("+atom+","+atom+")";List<String> conds=new ArrayList<>(),out=new ArrayList<>();
        for(int w:new int[]{30,60,120})conds.add("HOTT("+w+","+atom+")");
        for(int q=0;q<10;q++)for(int lag=1;lag<=2;lag++){conds.add("ODD("+q+","+lag+")");conds.add("BIG("+q+","+lag+")");}
        for(int e=0;e<8;e++)conds.add("HOT("+e+")");
        for(int i=0;i<20;i++)conds.add("GT("+rng.nextInt(10)+","+(1+rng.nextInt(2))+","+rng.nextInt(10)+","+(1+rng.nextInt(2))+")");
        conds.addAll(AtomCatalogSU1.sampleConditions(rng,20));for(String c:conds){out.add("IF("+c+","+atom+","+zero+")");out.add("IF("+c+","+zero+","+atom+")");}return out;
    }
}
