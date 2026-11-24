package com.ethk3.novaonepro.strategyv2;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Paired forecasts on the original SELECT calendar, including periods with no action. */
public final class FusionStudyR30R1 {
    private FusionStudyR30R1(){}

    public static double loss(double[] p,int actual){
        if(p==null||p.length!=10||actual<1||actual>10)return Double.NaN;
        double sum=0,loss=0;
        for(int i=0;i<10;i++){
            if(!Double.isFinite(p[i])||p[i]<0)return Double.NaN;
            sum+=p[i];
            double error=p[i]-(i==actual-1?1:0);
            loss+=.5*error*error;
        }
        return Math.abs(sum-1)<1e-8?loss:Double.NaN;
    }

    public static final class Pair {
        public final int n,bothMiss,eitherMiss,leftOnlyHit,rightOnlyHit;
        public final long expected;
        public final double residualCorrelation;
        Pair(int n,long expected,int both,int either,int leftOnly,int rightOnly,double correlation){
            this.n=n;this.expected=expected;bothMiss=both;eitherMiss=either;
            leftOnlyHit=leftOnly;rightOnlyHit=rightOnly;residualCorrelation=correlation;
        }
        public boolean complementary(){
            return n>=200&&n==expected&&leftOnlyHit>0&&rightOnlyHit>0
                &&Double.isFinite(residualCorrelation)&&Math.abs(residualCorrelation)<FusionOpsR27.MAX_CORRELATION;
        }
        public Map<String,Object> json(){
            Map<String,Object> m=new LinkedHashMap<>();
            m.put("calendarIssues",n);m.put("expectedCalendarIssues",expected);m.put("missingCalendarIssues",expected-n);
            m.put("residualCorrelation",Double.isFinite(residualCorrelation)?residualCorrelation:null);
            m.put("bothMiss",bothMiss);m.put("eitherMiss",eitherMiss);
            m.put("leftOnlyHit",leftOnlyHit);m.put("rightOnlyHit",rightOnlyHit);
            m.put("commonFailureRate",eitherMiss==0?null:bothMiss/(double)eitherMiss);
            m.put("complementaryResearchOnly",complementary());return m;
        }
    }

    public static Pair pair(PolicySpecR27 left,PolicySpecR27 right,EvalContextR27 context){
        requireTarget(left,right,context);
        ReplayEvaluatorR27.Trace x=trace(left,context),y=trace(right,context);
        double dot=0,xx=0,yy=0,sx=0,sy=0;
        int paired=0,both=0,either=0,leftOnly=0,rightOnly=0;
        long expected=context.split.selectFrom<context.split.selectTo
            ?context.data.ordinals[context.split.selectTo-1]-context.data.ordinals[context.split.selectFrom]+1:0;
        for(int row=context.split.selectFrom;row<context.split.selectTo;row++){
            if((row&31)==0)CooperativeContinuationR26.poll();
            int actual=context.data.draws[row][left.position-1]-1;
            double[] p=x.probabilityRows[row],q=y.probabilityRows[row];
            if(x.skipped[row]||y.skipped[row]||!Double.isFinite(loss(p,actual+1))||!Double.isFinite(loss(q,actual+1)))continue;
            double u=x.pHit[row]-x.hit[row],v=y.pHit[row]-y.hit[row];
            paired++;
            dot+=u*v;xx+=u*u;yy+=v*v;sx+=u;sy+=v;
            boolean leftMiss=x.hit[row]==0,rightMiss=y.hit[row]==0;
            if(leftMiss&&rightMiss)both++;
            if(leftMiss||rightMiss)either++;
            if(!leftMiss&&rightMiss)leftOnly++;
            if(leftMiss&&!rightMiss)rightOnly++;
        }
        double correlation=Double.NaN;
        if(paired>0){
            double vx=xx-sx*sx/paired,vy=yy-sy*sy/paired;
            if(left.policyHash().equals(right.policyHash()))correlation=1;
            else if(vx>0&&vy>0)correlation=Math.max(-1,Math.min(1,(dot-sx*sy/paired)/Math.sqrt(vx*vy)));
        }
        return new Pair(paired,expected,both,either,leftOnly,rightOnly,correlation);
    }

    static Map<String,Object> componentAblationR341(PolicySpecR27 full,PolicySpecR27 reduced,EvalContextR27 context){
        requireTarget(full,reduced,context);
        double temperature=context.temperature(full);
        ReplayEvaluatorR27.Trace observed=ReplayEvaluatorR27.selectTraceWithProbabilities(full,context,temperature);
        ReplayEvaluatorR27.Trace counter=ReplayEvaluatorR27.selectTraceWithProbabilities(reduced,context,temperature);
        int shared=0,selectionChanges=0,actionChanges=0;
        String first=null;
        StringBuilder measurements=new StringBuilder("R341_FUSION_COMPONENT_REMOVAL|"+context.data.datasetHash);
        for(int row=context.split.selectFrom;row<context.split.selectTo;row++){
            if((row&31)==0)CooperativeContinuationR26.poll();
            if(observed.skipped[row]||counter.skipped[row])continue;
            shared++;
            int a=0,b=0;
            for(int digit=0;digit<3;digit++){
                a|=1<<observed.k3[row*3+digit];
                b|=1<<counter.k3[row*3+digit];
            }
            boolean changed=a!=b&&(observed.act[row]||counter.act[row]);
            if(observed.act[row]!=counter.act[row])actionChanges++;
            if(changed){selectionChanges++;if(first==null)first=context.data.issues[row];}
            measurements.append('|').append(context.data.ordinals[row]).append(':').append(a).append(':').append(b)
                    .append(':').append(observed.act[row]).append(':').append(counter.act[row]);
        }
        Map<String,Object> evidence=new LinkedHashMap<>();
        evidence.put("remainingExecutable",reduced.policyHash());evidence.put("temperature",temperature);
        evidence.put("sameValidOpportunities",shared);evidence.put("selectionChanges",selectionChanges);
        evidence.put("actionChanges",actionChanges);evidence.put("firstChangedIssue",first);
        evidence.put("orderedK3AndActionHash",StatsR27.sha256(measurements.toString()));
        return evidence;
    }

    private static void requireTarget(PolicySpecR27 a,PolicySpecR27 b,EvalContextR27 c){
        if(a.position!=b.position)throw new IllegalArgumentException("FUSION_DIFFERENT_TARGET");
        if(c.labels!=c.data.draws)throw new IllegalArgumentException("FUSION_ORIGINAL_SELECT_ONLY");
    }
    static ReplayEvaluatorR27.Trace trace(PolicySpecR27 p,EvalContextR27 c){
        return ReplayEvaluatorR27.selectTraceWithProbabilities(p,c,c.temperature(p));
    }
    private static ResearchGainR30R1 gainWindow(EvalContextR27 c){
        return new ResearchGainR30R1(c.data.ordinals[c.split.selectFrom],
            c.data.ordinals[c.split.selectTo-1],64,4,200);
    }
    public static ResearchGainR30R1 gain(PolicySpecR27 base,PolicySpecR27 child,EvalContextR27 c){
        requireTarget(base,child,c);
        ResearchGainR30R1 result=gainWindow(c);
        ReplayEvaluatorR27.Trace a=trace(base,c),b=trace(child,c);
        for(int row=c.split.selectFrom;row<c.split.selectTo;row++){
            int actual=c.data.draws[row][base.position-1];
            double x=loss(a.probabilityRows[row],actual),y=loss(b.probabilityRows[row],actual);
            if(Double.isFinite(x)&&Double.isFinite(y))result.add(c.data.ordinals[row],x,y);
        }
        return result;
    }
    public static boolean improvesEveryParent(List<PolicySpecR27> parents,PolicySpecR27 child,EvalContextR27 c){
        return comparison(parents,child,c)!=null;
    }
    /** Use the same complete blocks for every parent and the unweighted probability control. */
    static Map<String,Object> comparison(List<PolicySpecR27> parents,PolicySpecR27 child,EvalContextR27 c){
        if(parents.size()<2||c.split.selectTo<=c.split.selectFrom)return null;
        List<ReplayEvaluatorR27.Trace> inputs=new ArrayList<>();
        ResearchGainR30R1[] comparisons=new ResearchGainR30R1[parents.size()+1];
        for(int i=0;i<parents.size();i++){
            requireTarget(parents.get(i),child,c);
            inputs.add(trace(parents.get(i),c));
            comparisons[i]=gainWindow(c);
        }
        comparisons[parents.size()]=gainWindow(c);
        ReplayEvaluatorR27.Trace candidate=trace(child,c);
        double[] average=new double[10],parentLoss=new double[parents.size()];
        for(int row=c.split.selectFrom;row<c.split.selectTo;row++){
            if((row&31)==0)CooperativeContinuationR26.poll();
            int actual=c.data.draws[row][child.position-1];
            double childLoss=loss(candidate.probabilityRows[row],actual);
            if(!Double.isFinite(childLoss))continue;
            java.util.Arrays.fill(average,0);
            boolean complete=true;
            for(int i=0;i<inputs.size();i++){
                double[] p=inputs.get(i).probabilityRows[row];
                parentLoss[i]=loss(p,actual);
                if(!Double.isFinite(parentLoss[i])){complete=false;break;}
                for(int digit=0;digit<10;digit++)average[digit]+=p[digit]/inputs.size();
            }
            if(!complete)continue;
            for(int i=0;i<parentLoss.length;i++)comparisons[i].add(c.data.ordinals[row],parentLoss[i],childLoss);
            comparisons[parentLoss.length].add(c.data.ordinals[row],loss(average,actual),childLoss);
        }
        Map<String,Object> evidence=new LinkedHashMap<>();
        for(int i=0;i<comparisons.length;i++){
            ResearchGainR30R1.Estimate estimate=comparisons[i].estimate();
            if(!estimate.supported()||estimate.incompleteFullBlocks!=0||estimate.lower95<=0)return null;
            evidence.put(i<parents.size()?parents.get(i).policyHash():"equalProbabilityAverage",
                java.util.Arrays.asList(estimate.evaluatedIssues,estimate.meanBlockGain,estimate.lower95,estimate.analysisContractHash));
        }
        return evidence;
    }
}
