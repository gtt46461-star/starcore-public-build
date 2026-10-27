package com.ethk3.novaonepro.strategyv2;
import java.io.*;
/** Private complete runtime for a family, evolved program, committee or fitted stage. */
public final class PolicyRuntimeR26 {
    private final LiveStateR26 family;private final LegacyReplayStateR26 legacy;private final LiveStrategySU2 live;
    private PolicyRuntimeR26(LiveStateR26 state){family=state;legacy=null;live=state.live;}
    private PolicyRuntimeR26(LegacyReplayStateR26 state){if(state.canonical.size()!=1)throw new IllegalArgumentException("R26_POLICY_PRIVATE_STATE");family=null;legacy=state;live=state.lives.values().iterator().next();}
    public static PolicyRuntimeR26 legacy(LegacyReplayStateR26 state){return new PolicyRuntimeR26(state);}
    public static PolicyRuntimeR26 decode(String text,byte[] bytes,String hash)throws IOException {
        if(bytes==null||!LiveStateR26.hash(bytes).equals(hash))throw new IOException("R26_POLICY_RUNTIME_HASH");
        // Existing family wire starts with Hub.t (a nonnegative long), new private legacy wire has its own magic.
        try(DataInputStream in=new DataInputStream(new java.util.zip.GZIPInputStream(new ByteArrayInputStream(bytes)))){
            if(in.readInt()==0x264c5301){LegacyReplayStateR26 state=LegacyReplayStateR26.decode(bytes);if(state.canonical.size()!=1||!state.canonical.get(0).equals(text))throw new IOException("R26_POLICY_RUNTIME_IDENTITY");return new PolicyRuntimeR26(state);}
        }
        return new PolicyRuntimeR26(LiveStateR26.decode(text,bytes,hash));
    }
    public ModelLiveSU2.Decision forecast(){return live.decide();}
    public void learn(int[] draw,long ordinal)throws IOException {if(family!=null)family.learn(draw);else legacy.advance(draw,ordinal);}
    public void onGap()throws IOException {if(family!=null)family.onGap();else legacy.onGap();}
    public byte[] encode()throws IOException{return family!=null?family.encode():legacy.encode();}
    public LegacyReplayStateR26 legacyState(){return legacy;}
    public float[][] table(){return live.pendingTable(family!=null?family.samples():legacy.samples());}
}
