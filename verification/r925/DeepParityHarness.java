package com.ethk3.novaonepro;
import java.io.*;
public final class DeepParityHarness {
    static double[] vector(BufferedReader r)throws Exception{String[] v=r.readLine().split("\\t");double[] a=new double[v.length];for(int i=0;i<v.length;i++)a[i]=Double.parseDouble(v[i]);return a;}
    public static void main(String[] args)throws Exception{
        BufferedReader r=new BufferedReader(new FileReader(args[0]));String kind;
        while((kind=r.readLine())!=null){double[] mean=vector(r),scale=vector(r),x=vector(r),slow=vector(r);double[][] w=new double[3][],b=new double[3][];
            for(int k=0;k<3;k++){r.readLine();w[k]=vector(r);b[k]=vector(r);}
            double[] out=DeepCandidateRuntimeR925.forward(kind,x,slow,mean,scale,w,b);for(int i=0;i<out.length;i++){if(i>0)System.out.print("\t");System.out.print(out[i]);}System.out.println();
            java.io.File folder=new java.io.File(args[0]).getParentFile();
            ResearchArtifactCodecR811.Artifact artifact=ResearchArtifactCodecR811.decode(java.nio.file.Files.readAllBytes(new java.io.File(folder,"M2_"+kind+".bin").toPath()));
            String target=TargetIssueResolver.next(artifact.trainedUntilIssue).targetIssue;
            double[] artifactScores=DeepCandidateRuntimeR925.logits(artifact,x,slow,target);
            for(int i=0;i<out.length;i++)if(Math.abs(out[i]-artifactScores[i])>1e-12)throw new AssertionError("BINARY_ARTIFACT_PARITY");
            if(!DeepCandidateRuntimeR925.infer(artifact,x,slow,target).productionValid())throw new AssertionError("SINKHORN_INVALID");
            try{DeepCandidateRuntimeR925.logits(artifact,x,slow,artifact.trainedUntilIssue);throw new AssertionError("PIT_ACCEPTED");}catch(IllegalArgumentException expected){}
            x[0]=Double.NaN;try{DeepCandidateRuntimeR925.forward(kind,x,slow,mean,scale,w,b);throw new AssertionError("NAN_ACCEPTED");}catch(IllegalArgumentException expected){}
        }r.close();
    }
}
