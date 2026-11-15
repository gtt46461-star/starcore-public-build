package com.ethk3.novaonepro.strategyv2;

import java.io.*;
import java.util.*;

/** Closed graph/shape/feature protocol with parameters, all Adam moments, and update ordinal. */
public final class RelGraphCodecSU1 {
    private RelGraphCodecSU1() {}
    public static String encode(RelGraphOptimizerSU1 state){
        try{ByteArrayOutputStream bytes=new ByteArrayOutputStream();try(DataOutputStream out=new DataOutputStream(bytes)){
            RelGraphModelSU1.Weights w=state.weights;out.writeInt(0x53553147);out.writeInt(1);out.writeUTF(RelGraphModelSU1.PROTOCOL);out.writeUTF(w.featureSchemaHash);out.writeInt(w.d);out.writeInt(w.channels);out.writeInt(w.roleCount);out.writeInt(w.typeCount);out.writeLong(state.steps);
            for(RelGraphModelSU1.Weights values:new RelGraphModelSU1.Weights[]{w,state.first,state.second})for(String key:RelGraphModelSU1.GROUPS){out.writeUTF(key);out.writeInt(values.get(key).length);for(double v:values.get(key))out.writeDouble(v);}
        }String wire=Base64.getEncoder().encodeToString(bytes.toByteArray());if(wire.length()>262144)throw new IllegalArgumentException("SU1_RELGRAPH_CODEC_BUDGET");return wire;}catch(IOException impossible){throw new IllegalStateException(impossible);}
    }
    public static RelGraphOptimizerSU1 decode(String wire,String expectedHash){
        if(wire==null||wire.length()>262144||!HashingSU1.text(wire).equals(HashingSU1.sha(expectedHash)))throw new IllegalArgumentException("SU1_RELGRAPH_HASH_OR_SIZE");
        try(DataInputStream in=new DataInputStream(new ByteArrayInputStream(Base64.getDecoder().decode(wire)))){
            if(in.readInt()!=0x53553147||in.readInt()!=1||!RelGraphModelSU1.PROTOCOL.equals(in.readUTF()))throw new IOException("SU1_RELGRAPH_PROTOCOL");String schema=HashingSU1.sha(in.readUTF());int d=in.readInt(),c=in.readInt(),roles=in.readInt(),types=in.readInt();long steps=in.readLong();if(d<2||d>16||c<1||c>16||roles<1||roles>16||types<1||types>16)throw new IOException("SU1_RELGRAPH_SHAPES");
            Map<String,Integer> sizes=RelGraphModelSU1.sizes(d,c,roles,types);RelGraphModelSU1.Weights[] all=new RelGraphModelSU1.Weights[3];
            for(int group=0;group<3;group++){Map<String,double[]> tensors=new LinkedHashMap<>();for(String key:RelGraphModelSU1.GROUPS){if(!key.equals(in.readUTF())||in.readInt()!=sizes.get(key))throw new IOException("SU1_RELGRAPH_PARAMETER_MANIFEST");double[] values=new double[sizes.get(key)];for(int i=0;i<values.length;i++)values[i]=in.readDouble();tensors.put(key,values);}all[group]=new RelGraphModelSU1.Weights(d,c,roles,types,schema,tensors);}
            if(in.read()!=-1)throw new IOException("SU1_RELGRAPH_TAIL");return new RelGraphOptimizerSU1(all[0],all[1],all[2],steps);
        }catch(IOException|IllegalArgumentException bad){throw new IllegalArgumentException("SU1_RELGRAPH_CORRUPT",bad);}
    }
}
