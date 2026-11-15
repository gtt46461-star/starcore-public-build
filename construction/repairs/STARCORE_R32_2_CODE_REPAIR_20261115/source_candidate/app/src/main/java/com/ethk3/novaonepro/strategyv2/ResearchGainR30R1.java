package com.ethk3.novaonepro.strategyv2;

import java.util.*;

/** Fixed, predeclared calendar blocks. Descriptive research evidence, never a certificate test. */
public final class ResearchGainR30R1 {
    public enum Loss { MULTICLASS_BRIER_HALF, BINARY_BRIER, OPPORTUNITY_K3_UTILITY_LOSS }
    private final Loss loss;
    private final long from, to; private final int blockSize,minimumBlocks,minimumIssues;
    private final TreeMap<Long,Double> differences=new TreeMap<>();
    public ResearchGainR30R1(long from,long to,int blockSize,int minBlocks,int minIssues){this(from,to,blockSize,minBlocks,minIssues,Loss.MULTICLASS_BRIER_HALF);}
    public ResearchGainR30R1(long from,long to,int blockSize,int minBlocks,int minIssues,Loss loss){
        this.loss=Objects.requireNonNull(loss);
        if(from<0||to<from||blockSize<1||minBlocks<2||minIssues<1)throw new IllegalArgumentException("GAIN_WINDOW");
        this.from=from;this.to=to;this.blockSize=blockSize;minimumBlocks=minBlocks;minimumIssues=minIssues;
    }
    public void add(long issueOrdinal,double baselineLoss,double candidateLoss){
        if(issueOrdinal<from||issueOrdinal>to||!Double.isFinite(baselineLoss)||!Double.isFinite(candidateLoss)
                ||baselineLoss<0||baselineLoss>1||candidateLoss<0||candidateLoss>1)throw new IllegalArgumentException("GAIN_OBSERVATION");
        // One calendar issue, already clustered over positions/components. Never count 100 cells as 100 issues.
        if(differences.putIfAbsent(issueOrdinal,baselineLoss-candidateLoss)!=null)
            throw new IllegalStateException("DUPLICATE_CALENDAR_ISSUE");
    }
    public static final class Estimate {
        public final Loss loss;public final long fromOrdinal,throughOrdinal;
        public final int blockSize,observedCalendarIssues,evaluatedIssues,completeBlocks,incompleteBlocks,incompleteFullBlocks,excludedTailIssues,missingIssues;
        public final Double meanBlockGain,lower95,upper95;
        public final String analysisContractHash;
        public final String status="DESCRIPTIVE_RESEARCH_ONLY";
        Estimate(Loss loss,long from,long to,int size,int minBlocks,int minIssues,int n,int evaluated,int blocks,int incomplete,int incompleteFull,int tail,int missing,Double mean,Double lower,Double upper){this.loss=loss;fromOrdinal=from;throughOrdinal=to;blockSize=size;observedCalendarIssues=n;evaluatedIssues=evaluated;completeBlocks=blocks;incompleteBlocks=incomplete;incompleteFullBlocks=incompleteFull;excludedTailIssues=tail;missingIssues=missing;meanBlockGain=mean;lower95=lower;upper95=upper;analysisContractHash=ResearchContractR30R1.hash("PAIRED_BLOCK_V1|"+from+'|'+to+'|'+size+'|'+minBlocks+'|'+minIssues+"|TAIL=EXCLUDE_FIXED_REMAINDER|LOSS="+loss.name()+"|STAT=MEAN_BLOCK_T95_DESCRIPTIVE");}
        public boolean supported(){return lower95!=null&&upper95!=null;}
    }
    public Estimate estimate(){
        List<Double> means=new ArrayList<>();int incomplete=0,incompleteFull=0,tail=0,completeIssues=0;
        for(long start=from;start<=to;){
            long remaining=to-start+1;int expected=(int)Math.min((long)blockSize,remaining);
            double sum=0;int n=0;for(int j=0;j<expected;j++){Double d=differences.get(start+j);if(d!=null){sum+=d;n++;}}
            // Partial final blocks are disclosed and excluded using the registered rule, never their score.
            if(expected==blockSize&&n==expected){means.add(sum/n);completeIssues+=n;}else {incomplete++;if(expected==blockSize)incompleteFull++;else tail=expected;}
            if(remaining<=blockSize)break;start+=blockSize;
        }
        long planned=to-from+1;int missing=(int)Math.min(Integer.MAX_VALUE,planned-differences.size());
        if(means.size()<minimumBlocks||completeIssues<minimumIssues)
            return new Estimate(loss,from,to,blockSize,minimumBlocks,minimumIssues,differences.size(),completeIssues,means.size(),incomplete,incompleteFull,tail,missing,null,null,null);
        double mean=0;for(double x:means)mean+=x;mean/=means.size();double ss=0;for(double x:means)ss+=(x-mean)*(x-mean);
        double se=Math.sqrt(ss/(means.size()-1)/means.size()),t=critical95(means.size()-1);
        return new Estimate(loss,from,to,blockSize,minimumBlocks,minimumIssues,differences.size(),completeIssues,means.size(),incomplete,incompleteFull,tail,missing,mean,mean-t*se,mean+t*se);
    }
    public List<Map<String,Object>> blocks(){List<Map<String,Object>> rows=new ArrayList<>();for(long start=from;start<=to;){int expected=(int)Math.min((long)blockSize,to-start+1);int present=0;double sum=0;for(int i=0;i<expected;i++){Double d=differences.get(start+i);if(d!=null){present++;sum+=d;}}Map<String,Object> m=new LinkedHashMap<>();m.put("fromOrdinal",start);m.put("throughOrdinal",start+expected-1);m.put("expectedIssues",expected);m.put("presentIssues",present);m.put("included",expected==blockSize&&present==expected);m.put("meanGain",present==0?null:sum/present);rows.add(m);if(to-start+1<=blockSize)break;start+=blockSize;}return Collections.unmodifiableList(rows);}
    /** R31-B08-C03#A4: a development interval found after a search is descriptive; it can never be relabelled as a
     * certificate or a formal qualification. Formal promotion goes through independent confirmation / future gates. */
    public static String labelR31(Estimate e,String requested){
        if(e==null)throw new IllegalArgumentException("R31_GAIN_ESTIMATE_REQUIRED");
        if(!"DESCRIPTIVE".equals(requested)&&!"DESCRIPTIVE_RESEARCH_ONLY".equals(requested))throw new IllegalStateException("R31_DESCRIPTIVE_RESEARCH_NOT_CERTIFIABLE:"+requested);
        return e.status;
    }
    static double critical95(int df){double[] t={0,12.706,4.303,3.182,2.776,2.571,2.447,2.365,2.306,2.262,2.228,2.201,2.179,2.160,2.145,2.131,2.120,2.110,2.101,2.093,2.086,2.080,2.074,2.069,2.064,2.060,2.056,2.052,2.048,2.045,2.042};if(df<1)throw new IllegalArgumentException("DF");return df<t.length?t[df]:2.042;}
}
