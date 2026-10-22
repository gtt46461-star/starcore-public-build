package com.ethk3.novaonepro;
import java.io.*;
public final class GateParityHarness {
    public static void main(String[] args)throws Exception{
        ResearchArtifactCodecR811.Artifact a=ResearchArtifactCodecR811.decode(java.nio.file.Files.readAllBytes(new File(args[0]).toPath()));
        BufferedReader r=new BufferedReader(new FileReader(args[1]));double[] x=DeepParityHarness.vector(r),flat=DeepParityHarness.vector(r);r.close();double[][][] experts=new double[9][10][10];
        for(int e=0;e<9;e++)for(int p=0;p<10;p++)for(int n=0;n<10;n++)experts[e][p][n]=flat[e*100+p*10+n];
        V51SinkhornEngine.ProjectionResult result=DeepCandidateRuntimeR925.gate(a,x,experts,"202609060100");
        if(!result.productionValid())throw new AssertionError("GATE_PROJECTION_INVALID");
        for(double[] row:result.matrix.probability)for(double v:row)System.out.println(v);
        try{DeepCandidateRuntimeR925.gate(a,x,experts,a.trainedUntilIssue);throw new AssertionError("GATE_PIT_ACCEPTED");}catch(IllegalArgumentException expected){}
        experts[0][0][0]=Double.NaN;
        try{DeepCandidateRuntimeR925.gate(a,x,experts,"202609060100");throw new AssertionError("GATE_NONFINITE_ACCEPTED");}catch(IllegalArgumentException expected){}
    }
}
