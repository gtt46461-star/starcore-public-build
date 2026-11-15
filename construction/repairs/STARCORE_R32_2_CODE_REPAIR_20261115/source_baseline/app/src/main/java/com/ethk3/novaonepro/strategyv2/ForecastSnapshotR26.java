package com.ethk3.novaonepro.strategyv2;
import java.io.*;
/** Small lossless forecast prepared outside P0, bound separately to the complete model checkpoint. */
public final class ForecastSnapshotR26 {
    public final ModelLiveSU2.Decision decision;private final byte[] table;
    public ForecastSnapshotR26(ModelLiveSU2.Decision decision,float[][] marginal)throws IOException{this(decision,encodeTable(marginal));}
    private ForecastSnapshotR26(ModelLiveSU2.Decision d,byte[] b)throws IOException{
        if(d==null||d.t<0||d.position<1||d.position>10||d.mask<0||d.mask>=1024||Integer.bitCount(d.mask)!=3||Double.isInfinite(d.gscore)||Double.isInfinite(d.phat)||Double.isFinite(d.phat)&&(d.phat<0||d.phat>1))throw new IOException("R26_FORECAST_DECISION");
        decision=d;table=b.clone();
    }
    public byte[] tableBytes(){return table.clone();}
    public static byte[] encodeTable(float[][] table)throws IOException{
        if(table==null||table.length!=10)throw new IOException("R26_TABLE_SHAPE");for(float[] row:table)if(row==null||row.length!=10)throw new IOException("R26_TABLE_SHAPE");
        ByteArrayOutputStream bytes=new ByteArrayOutputStream(400);try(DataOutputStream out=new DataOutputStream(bytes)){for(int p=0;p<10;p++){double row=0,col=0;for(int n=0;n<10;n++){float q=table[p][n];if(!Float.isFinite(q)||q<0||q>1)throw new IOException("R26_TABLE_RANGE");row+=q;col+=table[n][p];out.writeFloat(q);}if(Math.abs(row-1)>1e-4||Math.abs(col-1)>1e-4)throw new IOException("R26_TABLE_INCOHERENT");}}return bytes.toByteArray();
    }
    public byte[] bytes()throws IOException{ByteArrayOutputStream bytes=new ByteArrayOutputStream();try(DataOutputStream out=new DataOutputStream(bytes)){out.writeInt(0x26fc0001);out.writeLong(decision.t);out.writeInt(decision.position);out.writeInt(decision.mask);out.writeBoolean(decision.act);out.writeDouble(decision.gscore);out.writeDouble(decision.phat);out.write(table);}return bytes.toByteArray();}
    public static ForecastSnapshotR26 read(byte[] bytes)throws IOException{
        if(bytes==null||bytes.length!=437)throw new IOException("R26_FORECAST_SIZE");try(DataInputStream in=new DataInputStream(new ByteArrayInputStream(bytes))){if(in.readInt()!=0x26fc0001)throw new IOException("R26_FORECAST_ABI");ModelLiveSU2.Decision d=new ModelLiveSU2.Decision(in.readLong(),in.readInt(),in.readInt(),in.readBoolean(),in.readDouble(),in.readDouble());float[][] table=new float[10][10];for(int p=0;p<10;p++)for(int n=0;n<10;n++)table[p][n]=in.readFloat();if(in.read()!=-1)throw new IOException("R26_FORECAST_TRAILING");return new ForecastSnapshotR26(d,table);}
    }
}
