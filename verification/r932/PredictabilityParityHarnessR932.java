package com.ethk3.novaonepro;
import java.nio.file.*;
import org.json.*;
public final class PredictabilityParityHarnessR932 {
    public static void main(String[] args)throws Exception {
        JSONObject fixture=new JSONObject(new String(Files.readAllBytes(Paths.get(args[0])),"UTF-8"));
        TrainedModelMathR932.Predictability model=new TrainedModelMathR932.Predictability(fixture.getJSONObject("model"));
        JSONArray input=fixture.getJSONArray("inputs"),expected=fixture.getJSONArray("expected");double error=0;
        for(int i=0;i<input.length();i++)error=Math.max(error,Math.abs(model.predict(TrainedModelMathR932.vector(input.getJSONArray(i),12,false))-expected.getDouble(i)));
        if(error>1e-12)throw new AssertionError("PYTHON_PHONE_PREDICTABILITY_DRIFT:"+error);
        boolean rejected=false;try{model.predict(new double[]{Double.NaN});}catch(IllegalArgumentException yes){rejected=true;}
        if(!rejected)throw new AssertionError("MISSING_PREDICTABILITY_INPUT_ACCEPTED");
        System.out.println("R932_PREDICTABILITY_PARITY_PASS|n="+input.length()+"|maxError="+error+"|scope=SYNTHETIC_HOST_NOT_DEVICE");
    }
}
