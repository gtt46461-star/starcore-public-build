package com.ethk3.novaonepro.strategyv2;

import java.io.*;
import java.util.Base64;

/** Exact bounded binary transport; tensor dimensions and finite values are checked by Weights. */
public final class RelationalModelCodecSU1 {
    private RelationalModelCodecSU1() {}
    public static String encode(RelationalStateInference.Weights w){
        try{ByteArrayOutputStream bytes=new ByteArrayOutputStream();try(DataOutputStream out=new DataOutputStream(bytes)){out.writeInt(0x53553152);out.writeInt(1);out.writeInt(w.d);for(double[][] a:new double[][][]{w.e,w.u,w.r,w.wx,w.wm,w.wh})matrix(out,a);vector(out,w.b);matrix(out,w.wo);vector(out,w.c);}return Base64.getEncoder().encodeToString(bytes.toByteArray());}
        catch(IOException impossible){throw new IllegalStateException(impossible);}
    }
    public static RelationalStateInference.Weights decode(String payload,String expectedHash){
        if(payload==null||payload.length()>262144||!HashingSU1.text(payload).equals(HashingSU1.sha(expectedHash)))throw new IllegalArgumentException("SU1_MODEL_HASH_OR_SIZE");
        try(DataInputStream in=new DataInputStream(new ByteArrayInputStream(Base64.getDecoder().decode(payload)))){
            if(in.readInt()!=0x53553152||in.readInt()!=1)throw new IllegalArgumentException("SU1_MODEL_PROTOCOL");int d=in.readInt();if(d<2||d>64)throw new IllegalArgumentException("SU1_MODEL_WIDTH");
            RelationalStateInference.Weights w=new RelationalStateInference.Weights(matrix(in,10,d),matrix(in,10,d),matrix(in,10,10),matrix(in,d,d),matrix(in,d,d),matrix(in,d,d),vector(in,d),matrix(in,d,10),vector(in,10));
            if(in.read()!=-1)throw new IllegalArgumentException("SU1_MODEL_TRAILING_DATA");return w;
        }catch(IOException|IllegalArgumentException invalid){throw new IllegalArgumentException("SU1_MODEL_CORRUPT",invalid);}
    }
    private static void vector(DataOutputStream out,double[] a)throws IOException{for(double v:a)out.writeDouble(v);}
    private static void matrix(DataOutputStream out,double[][] a)throws IOException{for(double[] row:a)vector(out,row);}
    private static double[] vector(DataInputStream in,int n)throws IOException{double[] a=new double[n];for(int i=0;i<n;i++)a[i]=in.readDouble();return a;}
    private static double[][] matrix(DataInputStream in,int a,int b)throws IOException{double[][] x=new double[a][];for(int i=0;i<a;i++)x[i]=vector(in,b);return x;}
}
