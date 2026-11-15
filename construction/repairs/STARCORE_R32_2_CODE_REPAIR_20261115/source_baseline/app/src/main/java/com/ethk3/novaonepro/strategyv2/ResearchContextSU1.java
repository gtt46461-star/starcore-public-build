package com.ethk3.novaonepro.strategyv2;

/** Development-only descriptors, computed before the first scored target. No validation labels are read. */
public final class ResearchContextSU1 {
    public final String key,sourceHash;
    public final int observed;
    private ResearchContextSU1(String key,String sourceHash,int observed){this.key=key;this.sourceHash=sourceHash;this.observed=observed;}
    public static ResearchContextSU1 from(int[][] draws,int boundary){
        if(boundary<2||boundary>draws.length)throw new IllegalArgumentException("SU1_RESEARCH_CONTEXT_BOUNDARY");
        int first=Math.max(1,boundary-256),same=0,total=0;StringBuilder b=new StringBuilder();
        for(int t=first;t<boundary;t++)for(int p=0;p<10;p++){same+=draws[t][p]==draws[t-1][p]?1:0;total++;b.append(draws[t][p]).append(',');}
        String persistence=same/(double)total>.16?"STICKY":same/(double)total<.06?"ANTIPERSISTENT":"BASE_RATE";
        return new ResearchContextSU1(persistence+"|"+(boundary>=1500?"FULL_HISTORY":"SHORT_HISTORY"),HashingSU1.text(b.toString()),boundary-first);
    }
}
