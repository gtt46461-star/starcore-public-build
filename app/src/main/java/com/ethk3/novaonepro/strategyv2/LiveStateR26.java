package com.ethk3.novaonepro.strategyv2;
import java.io.*;
import java.util.*;
import java.util.zip.*;
/** Complete private learner snapshot used identically by replay, publication and live training. */
public final class LiveStateR26 {
    public final ModelGenomeSU2 genome;public final ModelLiveSU2.Hub hub;public final ModelLiveSU2 live;
    public LiveStateR26(ModelGenomeSU2 g){genome=g;hub=new ModelLiveSU2.Hub();live=new ModelLiveSU2(g,hub,1500,2500,250);}
    public long samples(){return hub.t;}
    public static String hash(byte[] b){return PolicyIdentityR26.sha256(Base64.getEncoder().encodeToString(b));}
    public static LiveStateR26 decode(String canonical,byte[] bytes,String expected)throws IOException{if(bytes==null||bytes.length>4<<20||!hash(bytes).equals(expected))throw new IOException("R26_LIVE_HASH");LiveStateR26 state=new LiveStateR26(ModelGenomeSU2.parse(canonical));try(DataInputStream in=new DataInputStream(new GZIPInputStream(new ByteArrayInputStream(bytes)))){state.hub.readInto(in);state.live.readInto(in);if(in.read()!=-1)throw new IOException("R26_LIVE_TRAILING");}return state;}
    public byte[] encode()throws IOException{ByteArrayOutputStream b=new ByteArrayOutputStream();try(DataOutputStream out=new DataOutputStream(new GZIPOutputStream(b))){hub.write(out);live.write(out);}if(b.size()>4<<20)throw new IOException("R26_LIVE_CAPACITY");return b.toByteArray();}
    public void onGap()throws IOException{if(!TemporalResetR26.reset(hub,live))throw new IOException("R26_GAP_CONTEXT_UNSUPPORTED");}
    public ModelLiveSU2.Decision forecast(){return live.decide();}
    public void learn(int[] draw){if(!live.hasPending())live.decide();live.settle(draw);hub.update(draw);}
    public float[][] table(){return live.pendingTable(hub.t);}
}
