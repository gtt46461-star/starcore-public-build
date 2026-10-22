package com.ethk3.novaonepro;
import java.io.*;
import java.nio.file.*;
import org.json.*;

public final class UpstreamParityHarnessR932 {
    public static void main(String[] args)throws Exception {
        JSONObject root=new JSONObject(new String(Files.readAllBytes(Paths.get(args[0])),"UTF-8"));
        UpstreamModelMathR932.Number[] models=new UpstreamModelMathR932.Number[4];for(int i=0;i<4;i++)models[i]=new UpstreamModelMathR932.Number(root.getJSONArray("numbers").getJSONObject(i));
        UpstreamModelMathR932.Gate gate=new UpstreamModelMathR932.Gate(root.getJSONObject("gate"));
        double[] slow=new double[UpstreamModelMathR932.NUMBER_INPUT];int issues=0;
        try(BufferedReader reader=new BufferedReader(new InputStreamReader(new FileInputStream(args[1]),"UTF-8"));DataOutputStream out=new DataOutputStream(new BufferedOutputStream(new FileOutputStream(args[2])))) {
            String line;while((line=reader.readLine())!=null&&issues<100) {
                JSONObject row=new JSONObject(line);JSONArray proofs=row.getJSONArray("proofs");JSONObject raw=new JSONObject(proofs.getString(0).substring(RawTrainingInputR932.PREFIX.length()));
                double[][] state=TrainedModelMathR932.matrix(raw.getJSONArray("states"),10,4);double[][][] experts=new double[9][][];
                for(int e=0;e<9;e++)experts[e]=TrainedModelMathR932.matrix(raw.getJSONArray("expert_logits").getJSONArray(e),10,10);
                double[] features=UpstreamModelMathR932.features(state,experts);for(double x:features)out.writeDouble(x);
                for(UpstreamModelMathR932.Number model:models){double[][] logits=model.logits(features,slow,experts);V51SinkhornEngine.ProjectionResult projection=new V51SinkhornEngine().projectDetailed(logits,1,240,1e-10);if(!projection.productionValid())throw new AssertionError("SINKHORN:"+projection.diagnostics);for(double[] p:projection.matrix.probability)for(double x:p)out.writeDouble(x);}
                double[] weights=gate.weights(state,experts);for(double x:weights)out.writeDouble(x);
                for(int i=0;i<slow.length;i++)slow[i]=.95*slow[i]+.05*features[i];issues++;
            }
        }
        if(issues!=100)throw new AssertionError("ISSUE_COUNT");System.out.println("R932_UPSTREAM_JAVA_INFERENCE_COMPLETE|issues="+issues);
    }
}
