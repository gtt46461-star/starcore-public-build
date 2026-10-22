package com.ethk3.novaonepro;

import java.util.Locale;

/**
 * R929 immutable hypothesis/alpha/look registry.  Renaming, archiving or splitting a family does
 * not restore statistical budget because identity is derived from semantic content hashes.
 */
public final class ResearchExperimentRegistryR929 {
    public static final String REVISION = "R929_EXPERIMENT_REGISTRY_V1";
    public static final double TOTAL_ALPHA = 0.05d;
    private static final int MAX_FAMILIES = 4096;
    private ResearchExperimentRegistryR929() {}

    public static final class Registration {
        public final String experimentId, familyHash, candidateHash, sourceHash, schemaHash;
        public final int familyOrdinal, candidateOrdinal;
        public final double familyAlpha, candidateAlpha;
        public final String canonical;
        Registration(String experimentId,String familyHash,String candidateHash,String sourceHash,
                     String schemaHash,int familyOrdinal,int candidateOrdinal,double familyAlpha,
                     double candidateAlpha,String canonical){
            this.experimentId=experimentId;this.familyHash=familyHash;this.candidateHash=candidateHash;
            this.sourceHash=sourceHash;this.schemaHash=schemaHash;this.familyOrdinal=familyOrdinal;
            this.candidateOrdinal=candidateOrdinal;this.familyAlpha=familyAlpha;
            this.candidateAlpha=candidateAlpha;this.canonical=canonical;
        }
    }

    public static Registration register(DbStore store,String familySemantic,String candidateSemantic,
                                        String sourceHash,String schemaHash,int familyOrdinal,
                                        int candidateOrdinal,int frozenFamilyBudget,int frozenCandidateBudget){
        if(store==null)throw new IllegalArgumentException("R929_REGISTRY_STORE");
        if(familyOrdinal<0||familyOrdinal>=MAX_FAMILIES||candidateOrdinal<0)
            throw new IllegalArgumentException("R929_REGISTRY_ORDINAL");
        if(frozenFamilyBudget<=0||frozenCandidateBudget<=0)
            throw new IllegalArgumentException("R929_REGISTRY_BUDGET");
        String familyHash=V51ChecksumEngine.sha256("FAMILY|"+required(familySemantic));
        String candidateHash=V51ChecksumEngine.sha256("CANDIDATE|"+required(candidateSemantic));
        String src=hash(sourceHash),schema=hash(schemaHash);
        // Pre-registered summable schedule: alpha_i = total/(2^(i+1)); inside family split equally
        // across the frozen candidate budget. The infinite family series is <= TOTAL_ALPHA.
        double familyAlpha=TOTAL_ALPHA/Math.pow(2.0d,familyOrdinal+1.0d);
        double candidateAlpha=familyAlpha/frozenCandidateBudget;
        String canonical=REVISION+"|family="+familyHash+"|candidate="+candidateHash+"|source="+src
                +"|schema="+schema+"|familyOrdinal="+familyOrdinal+"|candidateOrdinal="+candidateOrdinal
                +"|familyBudget="+frozenFamilyBudget+"|candidateBudget="+frozenCandidateBudget
                +"|familyAlpha="+fmt(familyAlpha)+"|candidateAlpha="+fmt(candidateAlpha);
        String id=V51ChecksumEngine.sha256(canonical);
        String key="r929_exp_"+id.substring(0,24);
        String prior=store.getState(key,"");
        if(prior.length()>0&&!prior.equals(canonical))throw new IllegalStateException("R929_EXPERIMENT_IDENTITY_DRIFT");
        if(prior.length()==0)store.setState(key,canonical);
        return new Registration(id,familyHash,candidateHash,src,schema,familyOrdinal,candidateOrdinal,
                familyAlpha,candidateAlpha,canonical);
    }

    public static String registerLook(DbStore store,Registration registration,int lookId,
                                      String windowId,String endpointIssue){
        if(store==null||registration==null)throw new IllegalArgumentException("R929_LOOK_INPUT");
        if(lookId<1)throw new IllegalArgumentException("R929_LOOK_ID");
        String canonical=registration.experimentId+"|look="+lookId+"|window="+required(windowId)
                +"|endpoint="+required(endpointIssue);
        String id=V51ChecksumEngine.sha256(canonical);String key="r929_look_"+id.substring(0,24);
        String prior=store.getState(key,"");
        if(prior.length()>0&&!prior.equals(canonical))throw new IllegalStateException("R929_LOOK_DRIFT");
        if(prior.length()==0)store.setState(key,canonical);
        return id;
    }

    public static double bonferroniAdjustedP(double rawP,int registeredHypotheses){
        if(Double.isNaN(rawP)||Double.isInfinite(rawP)||rawP<0||rawP>1||registeredHypotheses<=0)
            return Double.NaN;
        return Math.min(1.0d,rawP*registeredHypotheses);
    }

    /** Benjamini-Hochberg q values for reporting only; callers must not switch protocol post hoc. */
    public static double[] bhQ(double[] rawP){
        if(rawP==null||rawP.length==0)return new double[0];
        final int n=rawP.length;Integer[] order=new Integer[n];for(int i=0;i<n;i++){order[i]=i;if(Double.isNaN(rawP[i])||Double.isInfinite(rawP[i])||rawP[i]<0||rawP[i]>1)throw new IllegalArgumentException("R929_RAW_P");}
        java.util.Arrays.sort(order,new java.util.Comparator<Integer>(){public int compare(Integer a,Integer b){return Double.compare(rawP[a],rawP[b]);}});
        double[] q=new double[n];double next=1.0d;
        for(int rank=n;rank>=1;rank--){int idx=order[rank-1];double v=Math.min(1.0d,rawP[idx]*n/rank);next=Math.min(next,v);q[idx]=next;}
        return q;
    }

    public static boolean passesRegisteredAlpha(double rawP,Registration r){
        return r!=null&&!Double.isNaN(rawP)&&rawP>=0&&rawP<=1&&rawP<=r.candidateAlpha;
    }

    private static String required(String x){String v=x==null?"":x.trim();if(v.length()==0)throw new IllegalArgumentException("R929_REGISTRY_REQUIRED");return v;}
    private static String hash(String x){String v=required(x).toLowerCase(Locale.US);if(!v.matches("[0-9a-f]{64}"))throw new IllegalArgumentException("R929_REGISTRY_HASH");return v;}
    private static String fmt(double x){return String.format(Locale.US,"%.12g",x);}
}
