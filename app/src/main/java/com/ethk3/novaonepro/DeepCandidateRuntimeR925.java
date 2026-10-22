package com.ethk3.novaonepro;

/** D0/D1/D2/D3 artifact inference. Research only; a successful forward pass is not a certificate. */
public final class DeepCandidateRuntimeR925 {
    public static final String FEATURE_CONTRACT="R925_FROZEN_META96_ISSUE_MEAN_V1";
    private DeepCandidateRuntimeR925(){}
    public static double[] logits(ResearchArtifactCodecR811.Artifact artifact,double[] input,double[] pastSlow,String target){
        if(artifact==null||!TargetIssueResolver.isValid(target)||!TargetIssueResolver.isValid(artifact.trainedUntilIssue)||TargetIssueResolver.compare(artifact.trainedUntilIssue,target)>=0)throw new IllegalArgumentException("DEEP_ARTIFACT_PIT");
        String kind=artifact.type.replace("R925_","");
        if(!kind.matches("D[0-3]"))throw new IllegalArgumentException("DEEP_KIND");
        double[][] weights=new double[3][],bias=new double[3][];
        for(int k=0;k<3;k++){weights[k]=artifact.fields.get("W"+(k+1));bias[k]=artifact.fields.get("b"+(k+1));}
        return forward(kind,input,pastSlow,artifact.fields.get("feature_mean"),artifact.fields.get("feature_scale"),weights,bias);
    }
    public static V51SinkhornEngine.ProjectionResult infer(ResearchArtifactCodecR811.Artifact artifact,double[] input,double[] pastSlow,String target){
        double[] logits=logits(artifact,input,pastSlow,target);double[][] matrix=new double[10][10];
        for(int p=0;p<10;p++)System.arraycopy(logits,p*10,matrix[p],0,10);
        return new V51SinkhornEngine().projectDetailed(matrix,1,240,1e-10);
    }
    public static double[] forward(String kind,double[] input,double[] pastSlow,double[] mean,double[] scale,double[][] weights,double[][] bias){
        if(!kind.matches("D[0-3]")||input==null||input.length!=96||mean==null||mean.length!=(kind.equals("D2")?192:96)||scale==null||scale.length!=mean.length||weights==null||weights.length!=3||bias==null||bias.length!=3)throw new IllegalArgumentException("DEEP_DIMENSION");
        boolean dual=kind.equals("D2"),residual=kind.equals("D1");
        if(dual&&(pastSlow==null||pastSlow.length!=96))throw new IllegalArgumentException("DEEP_SLOW_STATE_REQUIRED");
        double[] a=new double[dual?192:96];
        for(int i=0;i<96;i++){
            finite(input[i]);finite(mean[i]);finite(scale[i]);if(scale[i]<=0)throw new IllegalArgumentException("DEEP_SCALE");
            a[i]=Math.max(-8,Math.min(8,(input[i]-mean[i])/scale[i]));
            if(dual){finite(pastSlow[i]);finite(mean[i+96]);finite(scale[i+96]);if(scale[i+96]<=0)throw new IllegalArgumentException("DEEP_SLOW_SCALE");a[i+96]=Math.max(-8,Math.min(8,(pastSlow[i]-mean[i+96])/scale[i+96]));}
        }
        int[] widths=residual?new int[]{96,48,100}:new int[]{64,32,100};
        for(int k=0;k<3;k++){
            if(bias[k]==null||bias[k].length!=widths[k]||weights[k]==null||weights[k].length!=widths[k]*a.length)throw new IllegalArgumentException("DEEP_LAYER_DIMENSION");
            double[] next=new double[widths[k]];
            for(int o=0;o<next.length;o++){
                double sum=finite(bias[k][o]);for(int i=0;i<a.length;i++)sum+=a[i]*finite(weights[k][o*a.length+i]);
                if(residual&&k==0)sum+=a[o];finite(sum);next[o]=k<2?Math.max(0,sum):sum;
            }a=next;
        }return a;
    }
    public static V51SinkhornEngine.ProjectionResult gate(ResearchArtifactCodecR811.Artifact artifact,double[] input,double[][][] experts,String target){
        if(artifact==null||!"R925_GATE9".equals(artifact.type)||!TargetIssueResolver.isValid(target)||!TargetIssueResolver.isValid(artifact.trainedUntilIssue)||TargetIssueResolver.compare(artifact.trainedUntilIssue,target)>=0)throw new IllegalArgumentException("GATE_ARTIFACT_PIT");
        double[] mean=artifact.fields.get("feature_mean"),scale=artifact.fields.get("feature_scale"),w=artifact.fields.get("gate_W"),b=artifact.fields.get("gate_b");
        if(input==null||input.length!=96||mean==null||mean.length!=96||scale==null||scale.length!=96||w==null||w.length!=864||b==null||b.length!=9||experts==null||experts.length!=9)throw new IllegalArgumentException("GATE_SHAPE");
        double[] weights=new double[9];double max=-Double.MAX_VALUE,sum=0;
        for(int e=0;e<9;e++){double v=finite(b[e]);for(int i=0;i<96;i++){finite(scale[i]);if(scale[i]<=0)throw new IllegalArgumentException("GATE_SCALE");double z=Math.max(-8,Math.min(8,(finite(input[i])-finite(mean[i]))/scale[i]));v+=z*finite(w[e*96+i]);}weights[e]=finite(v);max=Math.max(max,v);}
        for(int e=0;e<9;e++){weights[e]=Math.exp(weights[e]-max);sum+=weights[e];}
        double[][] logits=new double[10][10];
        for(int e=0;e<9;e++){if(experts[e]==null||experts[e].length!=10)throw new IllegalArgumentException("GATE_EXPERT_SHAPE");for(int p=0;p<10;p++){if(experts[e][p]==null||experts[e][p].length!=10)throw new IllegalArgumentException("GATE_NUMBER_SHAPE");for(int n=0;n<10;n++)logits[p][n]+=weights[e]/sum*finite(experts[e][p][n]);}}
        return new V51SinkhornEngine().projectDetailed(logits,1,240,1e-10);
    }
    private static double finite(double x){if(Double.isNaN(x)||Double.isInfinite(x))throw new IllegalArgumentException("DEEP_NONFINITE");return x;}
}
