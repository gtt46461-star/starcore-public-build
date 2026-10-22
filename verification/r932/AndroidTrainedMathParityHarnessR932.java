package com.ethk3.novaonepro;

import java.io.*;
import org.json.*;

/** Executes the exact production TrainedModelMathR932 under Android Runtime. */
public final class AndroidTrainedMathParityHarnessR932 {
    private static byte[] readAll(String path)throws Exception{
        ByteArrayOutputStream out=new ByteArrayOutputStream();
        InputStream in=new BufferedInputStream(new FileInputStream(path));
        try{byte[] buffer=new byte[16384];for(int n;(n=in.read(buffer))>=0;)if(n>0)out.write(buffer,0,n);}finally{in.close();}
        return out.toByteArray();
    }
    public static void main(String[] args)throws Exception{
        JSONObject root=new JSONObject(new String(readAll(args[0]),"UTF-8"));
        JSONArray models=root.getJSONArray("rankers"),cals=root.getJSONArray("calibrators");
        DataOutputStream out=new DataOutputStream(new BufferedOutputStream(new FileOutputStream(args[1])));
        try{
            for(int k=0;k<models.length();k++){
                TrainedModelMathR932.Ranker model=new TrainedModelMathR932.Ranker(models.getJSONObject(k));
                for(int t=0;t<100;t++)for(int a=0;a<1200;a++){
                    double[] x=new double[96];
                    for(int i=0;i<96;i++)x[i]=((t*37+a*19+i*13)%997)/996.0;
                    out.writeDouble(model.score(x,x[0]));
                }
            }
            for(int k=0;k<cals.length();k++){
                TrainedModelMathR932.Calibration cal=new TrainedModelMathR932.Calibration(cals.getJSONObject(k));
                for(int i=0;i<1000;i++)out.writeDouble(cal.predict((i-500)/20.0));
            }
        }finally{out.close();}
        System.out.println("R932_TRAINED_MATH_ANDROID_COMPLETE|ranker_values="+(models.length()*120000)
                +"|scope=ANDROID_RUNTIME_EMULATOR_SYNTHETIC_INPUTS_NOT_PHYSICAL_DEVICE_NOT_EFFECT");
    }
}
