package com.ethk3.novaonepro.strategyv2;
import java.io.*;
import java.util.*;
/** Causal filtered HMM experts. Explicit graph variants bind their state count and transition-memory cut. */
public final class FamilyF08KernelR26 implements OnlineModelsSU2.Model {
    private static final int MAGIC=0x26080001;
    private final int states;private final boolean transitionMemory,legacyCodec;
    private final OnlineModelsSU2.Hmm model;
    public FamilyF08KernelR26(){this(Integer.parseInt(ModelEvolutionSU2.centralParams("HMM").get("K")),true,true);}
    public FamilyF08KernelR26(int states,boolean memory){this(states,memory,false);}
    private FamilyF08KernelR26(int count,boolean memory,boolean legacy){
        if(count<2||count>6)throw new IllegalArgumentException("F08_STATE_GRAPH");
        states=count;transitionMemory=memory;legacyCodec=legacy;Map<String,String> p=ModelEvolutionSU2.centralParams("HMM");
        model=new OnlineModelsSU2.Hmm(count,memory?Double.parseDouble(p.get("stay")):1.0/count,Double.parseDouble(p.get("lr")),Long.parseLong(p.get("seed")),Double.parseDouble(p.get("temper")));
    }
    public int stateWidth(){return model.stateWidth();}
    public void predict(float[][] out,float[] state){model.predict(out,state);}
    public void update(int[] draw){model.update(draw);}
    public void write(DataOutput out)throws IOException{if(!legacyCodec){out.writeInt(MAGIC);out.writeInt(states);out.writeBoolean(transitionMemory);}model.write(out);}
    public void read(DataInput in)throws IOException{if(!legacyCodec&&(in.readInt()!=MAGIC||in.readInt()!=states||in.readBoolean()!=transitionMemory))throw new IOException("F08_CHECKPOINT_GRAPH");model.read(in);}
}
