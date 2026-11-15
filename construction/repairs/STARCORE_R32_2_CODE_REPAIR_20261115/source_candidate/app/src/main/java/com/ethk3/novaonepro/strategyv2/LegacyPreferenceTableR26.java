package com.ethk3.novaonepro.strategyv2;
import java.io.*;
/** Exact original row-normalized preferences. This is not a legal-permutation marginal table. */
public final class LegacyPreferenceTableR26 {
    private LegacyPreferenceTableR26() {}
    public static byte[] encode(float[][] table)throws IOException{
        if(table==null||table.length!=10)throw new IOException("R26_LEGACY_PREFERENCE_SHAPE");
        ByteArrayOutputStream bytes=new ByteArrayOutputStream(400);
        try(DataOutputStream out=new DataOutputStream(bytes)){for(float[] row:table){
            if(row==null||row.length!=10)throw new IOException("R26_LEGACY_PREFERENCE_SHAPE");double sum=0;
            for(float x:row){if(!Float.isFinite(x)||x<0||x>1)throw new IOException("R26_LEGACY_PREFERENCE_RANGE");sum+=x;out.writeFloat(x);}
            if(Math.abs(sum-1)>1e-5)throw new IOException("R26_LEGACY_PREFERENCE_ROW_SUM");
        }}return bytes.toByteArray();
    }
    public static float[][] decode(byte[] bytes)throws IOException{
        if(bytes==null||bytes.length!=400)throw new IOException("R26_LEGACY_PREFERENCE_BYTES");
        float[][] table=new float[10][10];try(DataInputStream in=new DataInputStream(new ByteArrayInputStream(bytes))){for(int p=0;p<10;p++)for(int n=0;n<10;n++)table[p][n]=in.readFloat();}encode(table);return table;
    }
}
