package com.ethk3.novaonepro.r952;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

/** Pure lock-free preparation for the twenty canonical matrix types. No clock or DB authority. */
public final class MatrixProjectionCP12 {
    private MatrixProjectionCP12() {}
    public static final int ELIGIBLE=1, OBSERVED=2, HIT_A=4, HIT_B=8, MISSING=16, INTEGRITY=32, PAIRED=64, RECOVERED=128;
    public static final int MAX_BYTES=262144, MAX_RING=5000;
    public enum Type { TransitionMatrix,ConditionalTransitionMatrix,CoOccurrenceMatrix,FrequencyMatrix,OmissionMatrix,
        AtomTargetMatrix,AtomRegimeMatrix,CandidateWindowMatrix,CandidateErrorMatrix,CandidateSimilarityMatrix,
        CandidateDiversityMatrix,ParentChildMatrix,DriftMatrix,ConfidenceMatrix,LatencyMatrix,TeacherMatrix,
        RuleHealthMatrix,FreezeMatrix,FailureMatrix,ResidualMatrix }
    public static final String SCHEMA_HASH=hash("SCM4/BINARY_V1/CP12/20_TYPES/FIXED43_RING/STATS_DOUBLE/FREEZE_UTF8/V1");
    public static final class Spec {
        public final Type type; public final String partition,manifest,matrixId; public final int window;
        public Spec(Type type,String partition,int window,String manifest) {
            need(type!=null&&partition!=null&&!partition.isEmpty()&&partition.getBytes(StandardCharsets.UTF_8).length<=256,"MATRIX_SPEC");
            need(window>=1&&window<=5000&&hashOk(manifest),"MATRIX_SPEC");
            need(type!=Type.DriftMatrix||window<=2500,"DRIFT_TWO_WINDOW_BOUND");
            need(type!=Type.ConditionalTransitionMatrix||partition.endsWith("|regimePolicy:"+hash("CP12_ATOM_REGIME_ARGMAX5_V1")),"CONDITIONAL_REGIME_POLICY_REQUIRED");
            need(type!=Type.LatencyMatrix||window<=300,"LATENCY_WINDOW_BOUND");
            need(type!=Type.FreezeMatrix||window==1,"FREEZE_WINDOW");
            this.type=type;this.partition=partition;this.window=window;this.manifest=manifest;
            matrixId=hash("MATRIX_CP4|"+type+"|"+partition+"|"+window+"|"+manifest);
        }
    }
    /** All digits and bins are zero based. flags never turn missing/system failures into model misses. */
    public static final class Sample {
        public final long ordinal; public final int flags,a,b,c,maskA,maskB; public final double x,y,z;
        public Sample(long ordinal,int flags,int a,int b,int c,int maskA,int maskB,double x,double y,double z) {
            need(ordinal>=0&&(flags&~255)==0&&a>=0&&a<=255&&b>=0&&b<=255&&c>=0&&c<=255&&maskA>=0&&maskA<=1023&&maskB>=0&&maskB<=1023,"MATRIX_SAMPLE");
            finite(x,y,z);this.ordinal=ordinal;this.flags=flags;this.a=a;this.b=b;this.c=c;this.maskA=maskA;this.maskB=maskB;this.x=x;this.y=y;this.z=z;
        }
        public boolean has(int flag){return (flags&flag)!=0;}
    }
    public static final class State {
        public final Spec spec; private long through=-1; private final ArrayDeque<Sample> ring=new ArrayDeque<>();
        private final double[] s; private final ArrayDeque<Long>[] occurrences;
        private final TreeMap<String,String> components=new TreeMap<>();
        private final ArrayDeque<Sample> recentDrift=new ArrayDeque<>();
        @SuppressWarnings("unchecked") private State(Spec spec){this.spec=Objects.requireNonNull(spec);s=new double[fields(spec.type)];occurrences=new ArrayDeque[10];for(int i=0;i<10;i++)occurrences[i]=new ArrayDeque<>();}
        public long throughOrdinal(){return through;} public int samples(){return ring.size();}
        public double[] sufficientStatistics(){return s.clone();}
        public Map<String,String> frozenComponents(){return Collections.unmodifiableMap(new TreeMap<>(components));}
        public List<Sample> samplesCopy(){return Collections.unmodifiableList(new ArrayList<>(ring));}
    }
    public static State empty(Spec spec){return new State(spec);}
    public static void apply(State state,long sourceOrdinal,Sample sample,Map<String,String> freezeComponents) {
        Objects.requireNonNull(state);Objects.requireNonNull(sample);need(sourceOrdinal>=state.through&&sourceOrdinal>=sample.ordinal,"MATRIX_ORDINAL_ORDER");
        validate(state.spec.type,sample);
        need(state.spec.type==Type.FailureMatrix&&sample.has(RECOVERED)||sample.ordinal==sourceOrdinal,"MATRIX_SOURCE_SAMPLE_ORDINAL");
        if(state.spec.type==Type.FreezeMatrix){need(freezeComponents!=null&&!freezeComponents.isEmpty()&&freezeComponents.size()<=64,"FREEZE_COMPONENTS");
            for(Map.Entry<String,String> e:freezeComponents.entrySet())need(e.getKey().matches("[A-Za-z][A-Za-z0-9_]{0,63}")&&hashOk(e.getValue()),"FREEZE_COMPONENT");
            if(!state.components.isEmpty()){need(state.components.equals(freezeComponents),"FREEZE_IDENTITY_DRIFT");return;}
            state.components.putAll(freezeComponents);state.ring.add(sample);state.through=sourceOrdinal;return;}
        int span=state.spec.type==Type.DriftMatrix?state.spec.window*2:state.spec.window;
        long minimum=Math.max(0,sourceOrdinal-span+1);
        boolean eventWindow=state.spec.type==Type.LatencyMatrix||state.spec.type==Type.FailureMatrix;
        if(!eventWindow)while(!state.ring.isEmpty()&&state.ring.peekFirst().ordinal<minimum)removeFirst(state);
        else if(!sample.has(RECOVERED))while(state.ring.size()>=span)removeFirst(state);
        if(state.spec.type==Type.FailureMatrix&&sample.has(RECOVERED)){
            Sample prior=null;for(Sample row:state.ring)if(row.ordinal==sample.ordinal){prior=row;break;}
            need(prior!=null&&!prior.has(RECOVERED)&&prior.a==sample.a,"UNMATCHED_OR_DUPLICATE_RECOVERY");
            ArrayDeque<Sample> replacement=new ArrayDeque<>();for(Sample row:state.ring){if(row==prior){delta(state,row,-1);replacement.add(sample);delta(state,sample,1);}else replacement.add(row);}
            state.ring.clear();state.ring.addAll(replacement);
        }else{
            need(state.ring.isEmpty()||sample.ordinal>state.ring.peekLast().ordinal,"DUPLICATE_MATRIX_SLOT");
            if(state.spec.type==Type.ResidualMatrix&&sample.has(OBSERVED)&&!state.ring.isEmpty()){Sample last=state.ring.peekLast();if(last.has(OBSERVED)&&last.ordinal+1==sample.ordinal)edge(state,last,sample,1);}
            state.ring.addLast(sample);delta(state,sample,1);
        }
        state.through=sourceOrdinal;
        if(state.spec.type==Type.DriftMatrix)driftRepartition(state);
        need(state.ring.size()<=MAX_RING,"MATRIX_RING_BOUND");for(double v:state.s)finite(v);
    }
    /** Proven draw watermark with no applicable conditional observation; does not create a numeric sample. */
    public static void advanceThroughCP12(State state,long sourceOrdinal){
        need(state.spec.type==Type.ConditionalTransitionMatrix&&sourceOrdinal>=state.through,"MATRIX_NO_INPUT_ADVANCE");
        long minimum=Math.max(0,sourceOrdinal-state.spec.window+1);
        while(!state.ring.isEmpty()&&state.ring.peekFirst().ordinal<minimum)removeFirst(state);
        state.through=sourceOrdinal;
    }
    private static void removeFirst(State state){Sample old=state.ring.removeFirst();
        if(state.spec.type==Type.ResidualMatrix&&!state.ring.isEmpty()){Sample next=state.ring.peekFirst();if(old.has(OBSERVED)&&next.has(OBSERVED)&&old.ordinal+1==next.ordinal)edge(state,old,next,-1);}
        delta(state,old,-1);
    }
    private static int fields(Type t){switch(t){case TransitionMatrix:return 100;case ConditionalTransitionMatrix:return 500;case CoOccurrenceMatrix:return 4;case FrequencyMatrix:return 10;case OmissionMatrix:return 30;case AtomTargetMatrix:return 27;case AtomRegimeMatrix:return 5;case CandidateWindowMatrix:return 34;case CandidateErrorMatrix:return 12;case CandidateSimilarityMatrix:return 2;case CandidateDiversityMatrix:return 9;case ParentChildMatrix:return 3;case DriftMatrix:return 20;case ConfidenceMatrix:return 32;case LatencyMatrix:return 1;case TeacherMatrix:return 3;case RuleHealthMatrix:return 13;case FreezeMatrix:return 0;case FailureMatrix:return 7;case ResidualMatrix:return 9;default:throw new AssertionError(t);}}
    private static void delta(State st,Sample r,int sign){double[] s=st.s;double ha=r.has(HIT_A)?1:0,hb=r.has(HIT_B)?1:0,ra=ha-r.x,rb=hb-r.y;
        switch(st.spec.type){
        case TransitionMatrix:s[r.a*10+r.b]+=sign;break;
        case ConditionalTransitionMatrix:s[r.c*100+r.a*10+r.b]+=sign;break;
        case CoOccurrenceMatrix:s[(int)(2*ha+hb)]+=sign;break;
        case FrequencyMatrix:s[r.a]+=sign;break;
        case OmissionMatrix:{ArrayDeque<Long> q=st.occurrences[r.a];int k=r.a*3;
            if(sign>0){if(!q.isEmpty()){double gap=r.ordinal-q.peekLast();s[k]++;s[k+1]+=gap;s[k+2]+=gap*gap;}q.addLast(r.ordinal);}
            else{need(!q.isEmpty()&&q.peekFirst()==r.ordinal,"OMISSION_QUEUE_CORRUPT");q.removeFirst();if(!q.isEmpty()){double gap=q.peekFirst()-r.ordinal;s[k]--;s[k+1]-=gap;s[k+2]-=gap*gap;}}break;}
        case AtomTargetMatrix:if(r.has(MISSING)){s[6]+=sign;break;}s[0]+=sign;s[1]+=sign*r.x;s[2]+=sign*ha;s[3]+=sign*r.x*r.x;s[4]+=sign*ha;s[5]+=sign*r.x*ha;s[7+r.c*2+(int)ha]+=sign;break;
        case AtomRegimeMatrix:s[0]+=sign;if(r.has(MISSING))s[1]+=sign;if(r.has(PAIRED)){s[2]+=sign;s[3]+=sign*r.z;s[4]+=sign*r.z*r.z;}break;
        case CandidateWindowMatrix:if(r.has(ELIGIBLE))s[0]+=sign;if(r.has(OBSERVED)){s[1]+=sign;s[2]+=sign*ha;s[3]+=sign*ra*ra;int b=bin(r.x);s[4+b]+=sign;s[14+b]+=sign*r.x;s[24+b]+=sign*ha;}break;
        case CandidateErrorMatrix:if(r.has(MISSING))s[1]+=sign;if(r.has(OBSERVED)){s[0]+=sign;if(!r.has(HIT_A))s[2+r.a]+=sign;}break;
        case CandidateSimilarityMatrix:s[0]+=sign;s[1]+=sign*MatrixMetricsCP11.jaccard(r.maskA,r.maskB);break;
        case CandidateDiversityMatrix:s[0]+=sign;s[1]+=sign*(1-MatrixMetricsCP11.jaccard(r.maskA,r.maskB));s[2]+=sign*((1-ha)*(1-hb));s[3]+=sign*ra;s[4]+=sign*rb;s[5]+=sign*ra*ra;s[6]+=sign*rb*rb;s[7]+=sign*ra*rb;s[8]+=sign*MatrixMetricsCP11.jaccard(r.maskA,r.maskB);break;
        case ParentChildMatrix:{double d=hb-ha;s[0]+=sign;s[1]+=sign*d;s[2]+=sign*d*d;break;}
        case DriftMatrix:
            if(sign>0){st.s[10+r.c]++;st.recentDrift.addLast(r);}
            else if(!st.recentDrift.isEmpty()&&st.recentDrift.peekFirst()==r){st.recentDrift.removeFirst();st.s[10+r.c]--;}
            else st.s[r.c]--;
            break;
        case ConfidenceMatrix:{if(!r.has(OBSERVED))break;int b=bin(r.x);s[b]+=sign;s[10+b]+=sign*r.x;s[20+b]+=sign*ha;s[30]+=sign;s[31]+=sign*ra*ra;break;}
        case LatencyMatrix:s[0]+=sign;break;
        case TeacherMatrix:s[0]+=sign;s[1]+=sign*(r.maskA==r.maskB?1:0);s[2]+=sign*(rb*rb-ra*ra);break;
        case RuleHealthMatrix:s[0]+=sign;if(r.has(MISSING))s[1]+=sign;if(r.has(INTEGRITY))s[2]+=sign;if(!r.has(MISSING))s[3+r.c]+=sign;break;
        case FailureMatrix:s[0]+=sign;s[1+r.a]+=sign;if(r.has(RECOVERED)){s[5]+=sign*r.z;s[6]+=sign;}break;
        case ResidualMatrix:if(r.has(OBSERVED)){s[0]+=sign;s[1]+=sign*ra;s[2]+=sign*ra*ra;}break;
        default:throw new AssertionError(st.spec.type);
        }
        for(int i=0;i<s.length;i++)if(Math.abs(s[i])<1e-12)s[i]=0;
    }
    private static void edge(State st,Sample a,Sample b,int sign){double x=(a.has(HIT_A)?1:0)-a.x,y=(b.has(HIT_A)?1:0)-b.x;
        st.s[3]+=sign;st.s[4]+=sign*x;st.s[5]+=sign*y;st.s[6]+=sign*x*x;st.s[7]+=sign*y*y;st.s[8]+=sign*x*y;}
    private static void driftRepartition(State st){long split=st.through-st.spec.window+1;
        while(!st.recentDrift.isEmpty()&&st.recentDrift.peekFirst().ordinal<split){Sample old=st.recentDrift.removeFirst();st.s[10+old.c]--;st.s[old.c]++;}}
    private static int bin(double p){return Math.min(9,(int)Math.floor(p*10));}
    private static void validate(Type t,Sample r){
        if(t==Type.TransitionMatrix||t==Type.ConditionalTransitionMatrix)need(r.a<10&&r.b<10,"TRANSITION_DIGIT");
        if(t==Type.ConditionalTransitionMatrix)need(r.c<5,"REGIME_RANGE");
        if(t==Type.FrequencyMatrix||t==Type.OmissionMatrix||t==Type.CandidateErrorMatrix)need(r.a<10,"ACTUAL_DIGIT");
        if(t==Type.AtomTargetMatrix||t==Type.DriftMatrix||t==Type.RuleHealthMatrix)need(r.c<10,"FIXED_BIN_RANGE");
        if(t==Type.CandidateWindowMatrix)need(!r.has(OBSERVED)||r.has(ELIGIBLE),"OBSERVED_NOT_ELIGIBLE");
        if(t==Type.CoOccurrenceMatrix||t==Type.CandidateDiversityMatrix||t==Type.ParentChildMatrix||t==Type.TeacherMatrix)need(r.has(PAIRED)&&r.has(OBSERVED),"PAIRED_PROOF_REQUIRED");
        if(t==Type.CandidateSimilarityMatrix||t==Type.CandidateDiversityMatrix||t==Type.TeacherMatrix)MatrixMetricsCP11.jaccard(r.maskA,r.maskB);
        if(t==Type.CandidateWindowMatrix||t==Type.CandidateDiversityMatrix||t==Type.ConfidenceMatrix||t==Type.TeacherMatrix||t==Type.ResidualMatrix)need(r.x>=0&&r.x<=1,"ORIGINAL_PREDRAW_PROBABILITY");
        if(t==Type.CandidateDiversityMatrix||t==Type.TeacherMatrix)need(r.y>=0&&r.y<=1,"ORIGINAL_PAIRED_PROBABILITY");
        if(t==Type.CandidateDiversityMatrix&&r.c==255)need(r.z>=0&&r.z<=3&&r.z==Math.rint(r.z),"MATRIX_VERIFIED_REGIME");
        if(t==Type.ConfidenceMatrix&&r.c==255)need(r.y>=0&&r.y<=1,"ORIGINAL_RAW_CONFIDENCE");
        if(t==Type.LatencyMatrix)need(r.x>=0&&r.x<=9007199254740991d&&r.x==Math.rint(r.x),"MONOTONIC_MICROS");
        if(t==Type.FailureMatrix)need(r.a<4&&(!r.has(RECOVERED)||r.z>=0),"FAILURE_RECOVERY");
        if(t==Type.AtomTargetMatrix||t==Type.AtomRegimeMatrix)need(Math.abs(r.x)<=1e100&&Math.abs(r.z)<=1e100,"MOMENT_OVERFLOW_BOUND");
    }
    /** Missing metrics are omitted, never silently encoded as zero or a prediction miss. */
    public static Map<String,double[]> metrics(State st){Map<String,double[]> out=new TreeMap<>();double[] s=st.s;int n=st.ring.size();
        switch(st.spec.type){
        case TransitionMatrix:case ConditionalTransitionMatrix:{int regimes=st.spec.type==Type.TransitionMatrix?1:5;boolean completePrior=st.spec.type==Type.TransitionMatrix&&n==st.spec.window&&contiguous(st.ring);for(int r=0;r<regimes;r++)for(int a=0;a<10;a++){double[] row=Arrays.copyOfRange(s,r*100+a*10,r*100+a*10+10);if(sum(row)>0)out.put("row"+r+"_"+a,MatrixMetricsCP11.smoothed(row,.5));
            // An explicit prior-backed metric keeps sparse rows executable. Existing
            // observed-row metrics remain absent without observations; no support is invented.
            if(completePrior)out.put("smoothedRow0_"+a,MatrixMetricsCP11.smoothed(row,.5));}break;}
        case CoOccurrenceMatrix:if(n>0){put(out,"jointMiss",s[0]/n);double[] rates=s.clone();for(int i=0;i<4;i++)rates[i]/=n;out.put("eventRates",rates);}break;
        case FrequencyMatrix:if(n==st.spec.window&&contiguous(st.ring)){out.put("probabilities",MatrixMetricsCP11.smoothed(s,0));}break;
        case OmissionMatrix:for(int d=0;d<10;d++){ArrayDeque<Long> q=st.occurrences[d];if(!q.isEmpty())put(out,"omission"+d,st.through-q.peekLast());int k=d*3;if(s[k]>0)put(out,"gapVariance"+d,MatrixMetricsCP11.meanVariance((long)s[k],s[k+1],s[k+2]));}break;
        case AtomTargetMatrix:put(out,"missing",s[6]);if(s[2]>0&&s[0]>s[2])put(out,"meanDifference",s[5]/s[2]-(s[1]-s[5])/(s[0]-s[2]));if(s[0]>0){put(out,"mutualInformation",MatrixMetricsCP11.mutualInformation(Arrays.copyOfRange(s,7,27),10,2));try{put(out,"correlation",MatrixMetricsCP11.correlation((long)s[0],s[1],s[2],s[3],s[4],s[5]));}catch(IllegalArgumentException e){if(!e.getMessage().equals("ZERO_VARIANCE")&&!e.getMessage().equals("CORRELATION_SUPPORT"))throw e;}}break;
        case AtomRegimeMatrix:if(s[0]>0){put(out,"availability",(s[0]-s[1])/s[0]);long observed=0;double total=0,sq=0;for(Sample row:st.ring)if(!row.has(MISSING)){observed++;total+=row.x;sq+=row.x*row.x;}if(observed>0){put(out,"scoreMean",total/observed);put(out,"scoreVariance",MatrixMetricsCP11.meanVariance(observed,total,sq));}}if(s[2]>0)put(out,"ablationMean",s[3]/s[2]);if(s[2]>1)put(out,"ablationSE",MatrixMetricsCP11.pairedSE((long)s[2],s[3],s[4]));break;
        case CandidateWindowMatrix:if(s[0]>0)put(out,"coverage",s[1]/s[0]);if(s[1]>0){put(out,"hitRate",s[2]/s[1]);put(out,"wilsonLower",MatrixMetricsCP11.wilsonLower((long)s[2],(long)s[1],1.959963984540054));put(out,"brier",s[3]/s[1]);put(out,"ece",ece(s,4,14,24));put(out,"maxLossStreak",lossStreak(st.ring));}break;
        case CandidateErrorMatrix:put(out,"missingCapture",s[1]);if(s[0]>0){double[] errors=Arrays.copyOfRange(s,2,12);for(int i=0;i<10;i++)errors[i]/=s[0];out.put("missBuckets",errors);}break;
        case CandidateSimilarityMatrix:if(s[0]>0)put(out,"jaccard",s[1]/s[0]);break;
        case CandidateDiversityMatrix:if(s[0]>0){put(out,"diversity",s[1]/s[0]);put(out,"jointMiss",s[2]/s[0]);try{put(out,"residualCorrelation",MatrixMetricsCP11.correlation((long)s[0],s[3],s[4],s[5],s[6],s[7]));}catch(IllegalArgumentException e){if(!e.getMessage().equals("ZERO_VARIANCE")&&!e.getMessage().equals("CORRELATION_SUPPORT"))throw e;}}break;
        case ParentChildMatrix:if(s[0]>0){put(out,"mean",s[1]/s[0]);put(out,"pairedTailBound",Math.min(1,2*Math.exp(-s[1]*s[1]/(2*s[0]))));}if(s[0]>1){double se=MatrixMetricsCP11.pairedSE((long)s[0],s[1],s[2]);put(out,"pairedSE",se);put(out,"ci95Lower",Math.max(-1,s[1]/s[0]-1.959963984540054*se));put(out,"ci95Upper",Math.min(1,s[1]/s[0]+1.959963984540054*se));}break;
        case DriftMatrix:{double[] ref=Arrays.copyOfRange(s,0,10),recent=Arrays.copyOfRange(s,10,20);if(sum(ref)>0&&sum(recent)>0){double[] p=MatrixMetricsCP11.smoothed(ref,.5),q=MatrixMetricsCP11.smoothed(recent,.5);put(out,"js",MatrixMetricsCP11.js(p,q));put(out,"psi",MatrixMetricsCP11.psi(p,q));}break;}
        case ConfidenceMatrix:if(s[30]>0){put(out,"ece",ece(s,0,10,20));put(out,"brier",s[31]/s[30]);}break;
        case LatencyMatrix:if(n>0){long[] a=new long[n];int i=0;for(Sample r:st.ring)a[i++]=(long)r.x;put(out,"p50Micros",MatrixMetricsCP11.nearestRank(a,.50));put(out,"p95Micros",MatrixMetricsCP11.nearestRank(a,.95));put(out,"p99Micros",MatrixMetricsCP11.nearestRank(a,.99));put(out,"count",n);}break;
        case TeacherMatrix:if(s[0]>0){put(out,"agreement",s[1]/s[0]);put(out,"brierDelta",s[2]/s[0]);}break;
        case RuleHealthMatrix:if(s[0]>0){put(out,"missingRate",s[1]/s[0]);put(out,"coverage",1-s[1]/s[0]);put(out,"successRate",1-s[1]/s[0]);put(out,"integrityRate",s[2]/s[0]);out.put("bins",Arrays.copyOfRange(s,3,13));}break;
        case FreezeMatrix:put(out,"componentCount",st.components.size());break;
        case FailureMatrix:put(out,"failureCount",s[0]);out.put("severityCounts",Arrays.copyOfRange(s,1,5));if(s[6]>0)put(out,"recoveryLagMean",s[5]/s[6]);break;
        case ResidualMatrix:if(s[0]>0){put(out,"mean",s[1]/s[0]);put(out,"bias",s[1]/s[0]);put(out,"variance",MatrixMetricsCP11.meanVariance((long)s[0],s[1],s[2]));}if(s[3]>1){try{put(out,"lag1Correlation",MatrixMetricsCP11.correlation((long)s[3],s[4],s[5],s[6],s[7],s[8]));}catch(IllegalArgumentException e){if(!e.getMessage().equals("ZERO_VARIANCE"))throw e;}}break;
        }
        enrichR82(st,out);
        return Collections.unmodifiableMap(out);
    }
    /** Additional views are recomputed from the bounded immutable ring. The SCM4V1
     * sufficient-statistic field counts, old metric values and 43-byte rows stay unchanged.
     * Marker c=255 is emitted only by R82 sources after validating the original receipt;
     * legacy c=0 does not assert that an unavailable confidence/resource value was zero. */
    private static void enrichR82(State st,Map<String,double[]> out){
        int n=st.ring.size();double[] s=st.s;
        switch(st.spec.type){
        case TransitionMatrix:case ConditionalTransitionMatrix:
            for(int regime=0;regime<(st.spec.type==Type.TransitionMatrix?1:5);regime++)for(int a=0;a<10;a++){
                double[] counts=Arrays.copyOfRange(s,regime*100+a*10,regime*100+a*10+10);
                out.put("rowCounts"+regime+"_"+a,counts);put(out,"rowSupport"+regime+"_"+a,sum(counts));
            }break;
        case CoOccurrenceMatrix:out.put("eventCounts",s.clone());put(out,"N",n);break;
        case FrequencyMatrix:
            out.put("count",s.clone());put(out,"N",n);
            if(n>0){double[] rate=s.clone(),z=new double[10];for(int d=0;d<10;d++){rate[d]/=n;z[d]=(s[d]-.1*n)/Math.sqrt(.09*n);}out.put("rate",rate);out.put("z",z);}break;
        case OmissionMatrix:
            for(int d=0;d<10;d++){ArrayDeque<Long> q=st.occurrences[d];int k=d*3;put(out,"gapSupport"+d,s[k]);
                if(!q.isEmpty()){put(out,"lastSeen"+d,q.peekLast());put(out,"gap"+d,st.through-q.peekLast());}
                if(s[k]>0)put(out,"gapMean"+d,s[k+1]/s[k]);
            }break;
        case AtomTargetMatrix:{put(out,"N",s[0]);double total=0,squares=0;int paired=0;for(Sample r:st.ring)if(r.has(PAIRED)&&!r.has(MISSING)){total+=r.z;squares+=r.z*r.z;paired++;}put(out,"contributionN",paired);if(paired>0)put(out,"oosContribution",total/paired);if(paired>1)put(out,"oosContributionSE",MatrixMetricsCP11.pairedSE(paired,total,squares));break;}
        case AtomRegimeMatrix:{put(out,"N",s[0]-s[1]);ArrayList<Double> older=new ArrayList<>(),recent=new ArrayList<>();long split=st.through-st.spec.window/2+1;
            for(Sample r:st.ring)if(!r.has(MISSING))(r.ordinal<split?older:recent).add(r.x);
            put(out,"driftReferenceN",older.size());put(out,"driftRecentN",recent.size());if(!older.isEmpty()&&!recent.isEmpty())put(out,"scoreKSLikeDrift",ksSamplesR82(older,recent));break;}
        case CandidateWindowMatrix:put(out,"N",s[1]);put(out,"eligibleN",s[0]);break;
        case CandidateErrorMatrix:{double[] counts=new double[10],severity=new double[10],severityN=new double[10];
            for(Sample r:st.ring)if(r.has(OBSERVED)&&!r.has(HIT_A)){counts[r.a]++;severity[r.a]+=r.x*r.x;severityN[r.a]++;}
            put(out,"N",s[0]);out.put("missCounts",counts);out.put("missSeverityN",severityN);
            for(int d=0;d<10;d++)if(severityN[d]>0)put(out,"missSeverity"+d,severity[d]/severityN[d]);break;}
        case CandidateSimilarityMatrix:put(out,"N",n);correlationR82(out,"predictionCorrelation",st.ring,0);break;
        case CandidateDiversityMatrix:{put(out,"N",n);double disagreement=0,unionMiss=0;for(Sample r:st.ring){boolean a=r.has(HIT_A),b=r.has(HIT_B);if(a!=b)disagreement++;if(!a||!b)unionMiss++;}
            if(n>0)put(out,"disagreement",disagreement/n);if(unionMiss>0)put(out,"complementarity",disagreement/unionMiss);
            correlationR82(out,"predictionCorrelation",st.ring,0);correlationR82(out,"errorCorrelation",st.ring,1);
            double[] support=new double[4],rescue=new double[4],missUnion=new double[4];int verified=0;for(Sample r:st.ring)if(r.c==255){int group=(int)r.z;need(group>=0&&group<4&&group==r.z,"MATRIX_VERIFIED_REGIME");support[group]++;verified++;boolean a=r.has(HIT_A),b=r.has(HIT_B);if(a!=b)rescue[group]++;if(!a||!b)missUnion[group]++;}out.put("regimeSupport",support);put(out,"verifiedRegimeN",verified);for(int g=0;g<4;g++)if(missUnion[g]>0)put(out,"regimeComplementarity"+g,rescue[g]/missUnion[g]);break;}
        case ParentChildMatrix:{put(out,"N",n);double elapsed=0,heap=0;int known=0;for(Sample r:st.ring)if(r.c==255){elapsed+=r.x;heap+=r.y;known++;}
            put(out,"resourceN",known);if(known>0){put(out,"resourceElapsedNanosDelta",elapsed/known);put(out,"resourceHeapBytesDelta",heap/known);}break;}
        case DriftMatrix:{double[] ref=Arrays.copyOfRange(s,0,10),recent=Arrays.copyOfRange(s,10,20);put(out,"referenceN",sum(ref));put(out,"recentN",sum(recent));
            if(sum(ref)>0&&sum(recent)>0)put(out,"ksLike",ksBinsR82(ref,recent));break;}
        case ConfidenceMatrix:{put(out,"N",s[30]);double cal=0,hits=0,raw=0;int known=0;for(Sample r:st.ring)if(r.has(OBSERVED)){cal+=r.x;if(r.has(HIT_A))hits++;if(r.c==255){raw+=r.y;known++;}}
            put(out,"rawConfidenceN",known);if(s[30]>0){put(out,"cal_conf",cal/s[30]);put(out,"LCB",MatrixMetricsCP11.wilsonLower((long)hits,(long)s[30],1.959963984540054));}if(known>0)put(out,"raw_conf",raw/known);break;}
        case TeacherMatrix:put(out,"support",s[0]);if(s[0]>0){put(out,"utility",s[2]/s[0]);double h=0;for(Sample r:st.ring)h+=(r.has(HIT_A)?1:0)-(r.has(HIT_B)?1:0);put(out,"hitUtility",h/s[0]);}break;
        case RuleHealthMatrix:{double[] old=new double[10],recent=new double[10];long split=st.through-st.spec.window/2+1;
            for(Sample r:st.ring)if(!r.has(MISSING))(r.ordinal<split?old:recent)[r.c]++;
            put(out,"driftReferenceN",sum(old));put(out,"driftRecentN",sum(recent));if(sum(old)>0&&sum(recent)>0){put(out,"drift",MatrixMetricsCP11.js(MatrixMetricsCP11.smoothed(old,.5),MatrixMetricsCP11.smoothed(recent,.5)));put(out,"driftKSLike",ksBinsR82(old,recent));}break;}
        default:break;
        }
    }
    private static double ksBinsR82(double[] a,double[] b){double na=sum(a),nb=sum(b),ca=0,cb=0,result=0;for(int i=0;i<a.length;i++){ca+=a[i]/na;cb+=b[i]/nb;result=Math.max(result,Math.abs(ca-cb));}return Math.min(1,result);}
    private static double ksSamplesR82(ArrayList<Double> a,ArrayList<Double> b){Collections.sort(a);Collections.sort(b);int i=0,j=0;double result=0;while(i<a.size()||j<b.size()){double x=i>=a.size()?b.get(j):j>=b.size()?a.get(i):Math.min(a.get(i),b.get(j));while(i<a.size()&&Double.compare(a.get(i),x)<=0)i++;while(j<b.size()&&Double.compare(b.get(j),x)<=0)j++;result=Math.max(result,Math.abs(i/(double)a.size()-j/(double)b.size()));}return result;}
    private static void correlationR82(Map<String,double[]> out,String name,Collection<Sample> rows,int kind){long n=0;double sx=0,sy=0,sxx=0,syy=0,sxy=0;for(Sample r:rows){double x=kind==0?r.x:(r.has(HIT_A)?0:1),y=kind==0?r.y:(r.has(HIT_B)?0:1);n++;sx+=x;sy+=y;sxx+=x*x;syy+=y*y;sxy+=x*y;}if(n<2)return;
        try{put(out,name,MatrixMetricsCP11.correlation(n,sx,sy,sxx,syy,sxy));}catch(IllegalArgumentException e){if(!"ZERO_VARIANCE".equals(e.getMessage())&&!"CORRELATION_SUPPORT".equals(e.getMessage()))throw e;}}
    public static final String POLICY_VIEWS_HASH=hash("CP12_POLICY_MATRICES_MASTER300/DERIVED20_50_100_300/V1");
    public static Map<String,double[]> metricsWindow(State master,int window){
        need(master.spec.window==300&&(window==20||window==50||window==100||window==300),"MATRIX_POLICY_VIEW_WINDOW");
        need(master.spec.type==Type.CandidateWindowMatrix||master.spec.type==Type.CandidateErrorMatrix||master.spec.type==Type.ConfidenceMatrix||master.spec.type==Type.ResidualMatrix,"MATRIX_POLICY_VIEW_TYPE");
        State view=empty(new Spec(master.spec.type,master.spec.partition,window,master.spec.manifest));long minimum=master.through-window+1;
        for(Sample row:master.ring)if(row.ordinal>=minimum)apply(view,row.ordinal,row,null);view.through=master.through;
        return metrics(view);
    }
    public static String viewHash(String matrixId,String payloadHash,long version,int window){need(hashOk(matrixId)&&hashOk(payloadHash)&&version>=0&&(window==20||window==50||window==100||window==300),"MATRIX_VIEW_IDENTITY");return hash("CP12_MATRIX_VIEW_V1|"+POLICY_VIEWS_HASH+'|'+matrixId+'|'+payloadHash+'|'+version+'|'+window);}
    private static double ece(double[] s,int n0,int p0,int h0){long[] n=new long[10];double[] p=new double[10],h=new double[10];for(int i=0;i<10;i++){n[i]=(long)s[n0+i];p[i]=s[p0+i];h[i]=s[h0+i];}return MatrixMetricsCP11.ece(n,p,h);}
    private static boolean contiguous(ArrayDeque<Sample> rows){long p=-1;for(Sample r:rows){if(p>=0&&r.ordinal!=p+1)return false;p=r.ordinal;}return true;}
    private static int lossStreak(ArrayDeque<Sample> rows){long[] o=new long[rows.size()];int[] h=new int[rows.size()];int i=0;for(Sample r:rows){o[i]=r.ordinal;h[i++]=r.has(OBSERVED)?(r.has(HIT_A)?1:0):-1;}return MatrixMetricsCP11.maxLossStreak(o,h);}
    private static double sum(double[] a){double s=0;for(double x:a)s+=x;return s;}
    private static void put(Map<String,double[]> m,String key,double value){finite(value);m.put(key,new double[]{value});}
    public static byte[] encode(State st){try{ByteArrayOutputStream bytes=new ByteArrayOutputStream();DataOutputStream out=new DataOutputStream(bytes);
        out.writeInt(0x53434D34);out.writeShort(1);out.writeShort(st.spec.type.ordinal());out.writeShort(st.spec.window);out.writeLong(st.through);
        // One statistics entry; typed FreezeMatrix component values remain UTF8 hashes.
        out.writeInt(1);out.writeShort(5);out.write("state".getBytes(StandardCharsets.UTF_8));out.writeShort(0);out.writeShort(st.s.length+st.components.size());
        for(double x:st.s){out.writeByte(2);out.writeDouble(x);}for(Map.Entry<String,String> e:st.components.entrySet()){out.writeByte(3);writeText(out,e.getKey()+"="+e.getValue());}
        out.writeInt(st.ring.size());for(Sample r:st.ring)writeSample(out,r);out.flush();byte[] result=bytes.toByteArray();need(result.length<=MAX_BYTES,"MATRIX_CHECKPOINT_BYTE_BOUND");return result;
    }catch(IOException impossible){throw new AssertionError(impossible);}}
    public static State decode(Spec spec,byte[] bytes){need(bytes!=null&&bytes.length<=MAX_BYTES,"MATRIX_DECODE_BOUND");
        try{DataInputStream in=new DataInputStream(new ByteArrayInputStream(bytes));need(in.readInt()==0x53434D34&&in.readUnsignedShort()==1&&in.readUnsignedShort()==spec.type.ordinal()&&in.readUnsignedShort()==spec.window,"MATRIX_CODEC_IDENTITY");long through=in.readLong();need(through>=-1,"MATRIX_CODEC_ORDINAL");
            need(in.readInt()==1&&readText(in).equals("state")&&readText(in).isEmpty(),"MATRIX_CODEC_ENTRY");int fc=in.readUnsignedShort();need(fc>=fields(spec.type)&&fc<=fields(spec.type)+64,"MATRIX_CODEC_FIELDS");double[] stored=new double[fields(spec.type)];for(int i=0;i<stored.length;i++){need(in.readUnsignedByte()==2,"MATRIX_CODEC_FIELD_TYPE");stored[i]=in.readDouble();finite(stored[i]);}
            Map<String,String> comps=new TreeMap<>();for(int i=stored.length;i<fc;i++){need(in.readUnsignedByte()==3,"FREEZE_FIELD_TYPE");String kv=readText(in);int cut=kv.indexOf('=');need(cut>0&&kv.substring(0,cut).matches("[A-Za-z][A-Za-z0-9_]{0,63}")&&hashOk(kv.substring(cut+1))&&comps.put(kv.substring(0,cut),kv.substring(cut+1))==null,"FREEZE_CODEC_COMPONENT");}
            need(spec.type==Type.FreezeMatrix||comps.isEmpty(),"MATRIX_UNEXPECTED_COMPONENT");int count=in.readInt();need(count>=0&&count<=MAX_RING&&((long)count)*43<=in.available(),"MATRIX_CODEC_RING");State state=empty(spec);
            for(int i=0;i<count;i++){Sample r=readSample(in);validate(spec.type,r);need(r.ordinal<=through&&(state.ring.isEmpty()||r.ordinal>state.ring.peekLast().ordinal),"MATRIX_CODEC_RING_ORDER");
                if(spec.type==Type.FreezeMatrix){need(count==1&&!comps.isEmpty(),"FREEZE_CODEC_RING");state.components.putAll(comps);state.ring.add(r);}
                else{if(spec.type==Type.ResidualMatrix&&r.has(OBSERVED)&&!state.ring.isEmpty()){Sample last=state.ring.peekLast();if(last.has(OBSERVED)&&last.ordinal+1==r.ordinal)edge(state,last,r,1);}state.ring.add(r);delta(state,r,1);}}
            need(in.available()==0&&through>=state.through,"MATRIX_CODEC_TRAILING_OR_ORDER");state.through=through;if(spec.type==Type.DriftMatrix)driftRepartition(state);
            for(int i=0;i<stored.length;i++)need(Math.abs(stored[i]-state.s[i])<=1e-9*Math.max(1,Math.abs(stored[i])),"MATRIX_CODEC_STAT_DRIFT");
            System.arraycopy(stored,0,state.s,0,stored.length);need(Arrays.equals(bytes,encode(state)),"MATRIX_NONCANONICAL_BYTES");return state;
        }catch(IOException e){throw new IllegalArgumentException("MATRIX_TRUNCATED",e);}}
    public static byte[] sampleBytes(Sample row,Map<String,String> components){try{ByteArrayOutputStream b=new ByteArrayOutputStream();DataOutputStream out=new DataOutputStream(b);writeSample(out,row);Map<String,String> c=components==null?Collections.emptyMap():new TreeMap<>(components);need(c.size()<=64,"FREEZE_COMPONENT_COUNT");out.writeShort(c.size());for(Map.Entry<String,String> e:c.entrySet()){writeText(out,e.getKey());writeText(out,e.getValue());}return b.toByteArray();}catch(IOException e){throw new AssertionError(e);}}
    public static Sample sampleFromBytes(byte[] bytes){need(bytes!=null&&bytes.length>=45&&bytes.length<=16384,"MATRIX_INPUT_BOUND");try{return readSample(new DataInputStream(new ByteArrayInputStream(bytes)));}catch(IOException e){throw new IllegalArgumentException("MATRIX_INPUT_TRUNCATED",e);}}
    public static Map<String,String> componentsFromBytes(byte[] bytes){need(bytes!=null&&bytes.length>=45&&bytes.length<=16384,"MATRIX_INPUT_BOUND");try{DataInputStream in=new DataInputStream(new ByteArrayInputStream(bytes));readSample(in);int count=in.readUnsignedShort();need(count<=64,"FREEZE_COMPONENT_COUNT");Map<String,String> c=new TreeMap<>();for(int i=0;i<count;i++)need(c.put(readText(in),readText(in))==null,"DUPLICATE_FREEZE_COMPONENT");need(in.available()==0,"MATRIX_INPUT_TRAILING");return c;}catch(IOException e){throw new IllegalArgumentException("MATRIX_INPUT_TRUNCATED",e);}}
    private static void writeSample(DataOutputStream out,Sample r)throws IOException{out.writeLong(r.ordinal);out.writeInt(r.flags);out.writeByte(r.a);out.writeByte(r.b);out.writeByte(r.c);out.writeShort(r.maskA);out.writeShort(r.maskB);out.writeDouble(r.x);out.writeDouble(r.y);out.writeDouble(r.z);}
    private static Sample readSample(DataInputStream in)throws IOException{return new Sample(in.readLong(),in.readInt(),in.readUnsignedByte(),in.readUnsignedByte(),in.readUnsignedByte(),in.readUnsignedShort(),in.readUnsignedShort(),in.readDouble(),in.readDouble(),in.readDouble());}
    private static void writeText(DataOutputStream out,String text)throws IOException{byte[] b=text.getBytes(StandardCharsets.UTF_8);need(b.length<=256,"MATRIX_TEXT_BOUND");out.writeShort(b.length);out.write(b);}
    private static String readText(DataInputStream in)throws IOException{int n=in.readUnsignedShort();need(n<=256&&n<=in.available(),"MATRIX_TEXT_BOUND");byte[] b=new byte[n];in.readFully(b);String s=new String(b,StandardCharsets.UTF_8);need(Arrays.equals(b,s.getBytes(StandardCharsets.UTF_8)),"MATRIX_UTF8");return s;}
    public static String bytesHash(byte[] bytes){try{byte[] h=MessageDigest.getInstance("SHA-256").digest(bytes);StringBuilder s=new StringBuilder(64);for(byte v:h){s.append(Character.forDigit((v>>>4)&15,16));s.append(Character.forDigit(v&15,16));}return s.toString();}catch(java.security.NoSuchAlgorithmException e){throw new AssertionError(e);}}
    public static String hash(String s){return bytesHash(s.getBytes(StandardCharsets.UTF_8));}
    private static boolean hashOk(String s){return s!=null&&s.matches("[0-9a-f]{64}");}
    private static void finite(double... values){for(double v:values)need(Double.isFinite(v),"NON_FINITE_MATRIX_STATISTIC");}
    private static void need(boolean condition,String code){if(!condition)throw new IllegalArgumentException(code);}
}
