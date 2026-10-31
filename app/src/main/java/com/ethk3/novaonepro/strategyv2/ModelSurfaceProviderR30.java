package com.ethk3.novaonepro.strategyv2;

import java.util.*;

/** Actual AtomNet number head, immutable model/outputs, and the same strict PIT guard used by policy freezing. */
public final class ModelSurfaceProviderR30 {
    private ModelSurfaceProviderR30(){}
    public static final int HISTORY_ROWS=480;
    public static final class FrozenModelRef {
        public final String modelHash; public final long trainedThroughOrdinal,availableAt;
        private final byte[] weights;
        public FrozenModelRef(byte[] weights,long through,long available){
            this.weights=weights.clone();modelHash=StatsR27.sha256(this.weights);trainedThroughOrdinal=through;availableAt=available;
            AtomNetR27.decode(this.weights);if(available<=0)throw new IllegalArgumentException("MODEL_AVAILABILITY_UNKNOWN");
        }
        public byte[] weights(){return weights.clone();}
    }
    public static final class Pit {
        public final long targetOrdinal,featureThroughOrdinal,featureAvailableAt,preparedAt,frozenAt,deadlineAt;
        public final int position; public final String historyHash; private final double[] inputs;
        public Pit(long target,long featureThrough,int position,String historyHash,double[] inputs,long featureAt,long prepared,long frozen,long deadline){
            targetOrdinal=target;featureThroughOrdinal=featureThrough;this.position=position;this.historyHash=historyHash;this.inputs=inputs.clone();
            featureAvailableAt=featureAt;preparedAt=prepared;frozenAt=frozen;deadlineAt=deadline;
        }
    }
    public static final class Surface {
        public final String modelHash,historyHash,probabilityHash;
        public final long targetOrdinal,trainedThroughOrdinal,modelAvailableAt,featureThroughOrdinal,featureAvailableAt,preparedAt,frozenAt,deadlineAt;
        public final int position;private final double[] p;
        private Surface(String model,String history,double[] probabilities,long target,long through,long available,long featureThrough,long featureAt,int pos,long prepared,long frozen,long deadline){
            modelHash=model;historyHash=history;p=probabilities.clone();targetOrdinal=target;trainedThroughOrdinal=through;modelAvailableAt=available;
            featureThroughOrdinal=featureThrough;featureAvailableAt=featureAt;position=pos;preparedAt=prepared;frozenAt=frozen;deadlineAt=deadline;
            probabilityHash=StatsR27.sha256(Storeless.bytes(p));validate(this);
        }
        public double[] probabilities(){return p.clone();}
    }
    public static final class Blocked extends IllegalStateException { public Blocked(String reason){super("R30_MODEL_BLOCKED:"+reason);} }
    public static Surface predict(Pit pit,FrozenModelRef model){
        if(pit==null||model==null)throw new Blocked("NO_FROZEN_MODEL");
        AtomNetR27 net=AtomNetR27.decode(model.weights);
        if(pit.inputs.length!=net.in)throw new Blocked("FEATURE_ABI_MISMATCH");
        double[] p=NumberRankerAdapterR30.probabilities(net,pit.inputs);
        return new Surface(model.modelHash,pit.historyHash,p,pit.targetOrdinal,model.trainedThroughOrdinal,model.availableAt,pit.featureThroughOrdinal,pit.featureAvailableAt,pit.position,pit.preparedAt,pit.frozenAt,pit.deadlineAt);
    }
    /** Only the database adapter uses archived, pre-deadline probability receipts; it must verify history and hash. */
    static Surface archived(String model,String history,double[] probabilities,long target,long through,long available,long featureThrough,long featureAt,int pos,long prepared,long frozen,long deadline){
        return new Surface(model,history,probabilities,target,through,available,featureThrough,featureAt,pos,prepared,frozen,deadline);
    }
    static void validate(Surface s){
        if(s.modelHash==null||!s.modelHash.matches("[0-9a-f]{64}")||s.historyHash==null||s.historyHash.isEmpty()||s.modelAvailableAt<=0||s.featureAvailableAt<=0)throw new Blocked("MISSING_PROVENANCE");
        PitGuardR30.Freeze f=new PitGuardR30.Freeze();f.targetOrdinal=s.targetOrdinal;f.trainedThroughOrdinal=s.trainedThroughOrdinal;f.featureThroughOrdinal=s.featureThroughOrdinal;
        f.position=s.position;f.probabilities=s.p;f.modelAvailableAt=s.modelAvailableAt;f.featureAvailableAt=s.featureAvailableAt;f.preparedAt=s.preparedAt;f.frozenAt=s.frozenAt;f.deadlineAt=s.deadlineAt;
        if(s.p.length==10){int[] order=new int[10];ReplayEvaluatorR27.top3(s.p,order);f.k3=new int[]{order[0]+1,order[1]+1,order[2]+1};}
        List<String> violations=PitGuardR30.check(f);if(!violations.isEmpty())throw new Blocked(String.join(",",violations));
    }
    public static String historyHash(ReplayDatasetR27 d,int targetRow){
        StringBuilder b=new StringBuilder("R30_MODEL_HISTORY_480_V1");
        for(int t=Math.max(0,targetRow-HISTORY_ROWS);t<targetRow;t++){
            b.append('|').append(d.issues[t]).append(':').append(d.ordinals[t]);for(int n:d.draws[t])b.append(':').append(n);
        }
        return StatsR27.sha256(b.toString());
    }
    public interface Source { Surface at(int row,int position); String reason(int row,int position); }
    public static final Source NONE=new Source(){public Surface at(int r,int p){return null;}public String reason(int r,int p){return "NO_ARCHIVED_OR_FROZEN_MODEL";}};
    public static Source source(Map<Long,Surface> rows,Map<Long,String> blocked){
        Map<Long,Surface> immutable=Collections.unmodifiableMap(new HashMap<>(rows));Map<Long,String> reasons=Collections.unmodifiableMap(new HashMap<>(blocked));
        return new Source(){public Surface at(int r,int p){return immutable.get(key(r,p));}public String reason(int r,int p){String why=reasons.get(key(r,p));if(why==null)why=reasons.get(-1L);return why==null?"NO_ARCHIVED_OR_FROZEN_MODEL":why;}};
    }
    static long key(int row,int position){return row*10L+position-1;}
    static final class Storeless {
        static byte[] bytes(double[] p){java.nio.ByteBuffer b=java.nio.ByteBuffer.allocate(p.length*8);for(double x:p)b.putDouble(x);return b.array();}
    }
    /** ABI probe exercises real inference, not absent historical models or synthetic quality. */
    public static String goldenHash(){
        AtomNetR27 n=new AtomNetR27(12,30);StringBuilder b=new StringBuilder("R30_MODEL_SURFACE_ABI_V1");
        for(int i=0;i<64;i++){double[] x=AtomNetR27.positionedInput(new double[]{i/64.,(i%7)/7.},i%10+1);for(double p:NumberRankerAdapterR30.probabilities(n,x))b.append('|').append(Double.toHexString(p));}
        return StatsR27.sha256(b.toString());
    }
}
