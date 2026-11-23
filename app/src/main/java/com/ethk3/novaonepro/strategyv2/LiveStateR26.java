package com.ethk3.novaonepro.strategyv2;
import java.io.*;
import java.util.*;
import java.util.zip.*;
/** Complete private learner snapshot used identically by replay, publication and live training. */
public final class LiveStateR26 implements AutoCloseable {
    public final ModelGenomeSU2 genome;public final ModelLiveSU2.Hub hub;public final ModelLiveSU2 live;
    public LiveStateR26(ModelGenomeSU2 g){this(g,false);}
    private LiveStateR26(ModelGenomeSU2 g,boolean verifiedRestore){
        if(!verifiedRestore&&WarmModelSeedR26.has(g))throw new IllegalArgumentException("R26_WARM_INITIALIZER_REQUIRED");
        genome=g;
        hub=new ModelLiveSU2.Hub();
        try {
            live=new ModelLiveSU2(g,hub,1500,2500,250,verifiedRestore);
        } catch(RuntimeException|Error failure) {
            closeAfterFailure(failure);
            throw failure;
        }
    }
    private boolean closed;
    @Override public void close(){
        if(closed)return;
        closed=true;
        Throwable failure=null;
        for(OnlineModelsSU2.Model model:hub.models.values()){
            try {
                FamilyRegistryR26.closeR31(model);
            } catch(RuntimeException|Error closeFailure) {
                if(failure==null)failure=closeFailure;
                else if(failure!=closeFailure)failure.addSuppressed(closeFailure);
            }
        }
        hub.models.clear();
        hub.cal.clear();
        hub.p.clear();
        hub.st.clear();
        hub.pc.clear();
        if(failure instanceof RuntimeException)throw (RuntimeException)failure;
        if(failure instanceof Error)throw (Error)failure;
    }
    void closeAfterFailure(Throwable failure){
        try {close();}
        catch(RuntimeException|Error closeFailure){if(failure!=closeFailure)failure.addSuppressed(closeFailure);}
    }
    static LiveStateR26 blankForVerifiedRestore(ModelGenomeSU2 g){return new LiveStateR26(g,true);}
    public long samples(){return hub.t;}
    public static String hash(byte[] b){return PolicyIdentityR26.sha256(Base64.getEncoder().encodeToString(b));}
    public static LiveStateR26 decode(String canonical,byte[] bytes,String expected)throws IOException{
        if(bytes==null||bytes.length>4<<20||!hash(bytes).equals(expected))throw new IOException("R26_LIVE_HASH");
        LiveStateR26 state=blankForVerifiedRestore(ModelGenomeSU2.parse(canonical));
        try(DataInputStream in=new DataInputStream(new BufferedInputStream(new CooperativeInputStreamR26(new GZIPInputStream(new ByteArrayInputStream(bytes))),32768))){
            state.hub.readInto(in);
            state.live.readInto(in);
            if(in.read()!=-1)throw new IOException("R26_LIVE_TRAILING");
        } catch(IOException|RuntimeException|Error failure){
            state.closeAfterFailure(failure);
            throw failure;
        }
        return state;
    }
    public byte[] encode()throws IOException{ByteArrayOutputStream b=new ByteArrayOutputStream();try(DataOutputStream out=new DataOutputStream(new BufferedOutputStream(new CooperativeOutputStreamR26(new GZIPOutputStream(b)),32768))){hub.write(out);live.write(out);}if(b.size()>4<<20)throw new IOException("R26_LIVE_CAPACITY");return b.toByteArray();}
    public void onGap()throws IOException{if(!TemporalResetR26.reset(hub,live))throw new IOException("R26_GAP_CONTEXT_UNSUPPORTED");}
    public ModelLiveSU2.Decision forecast(){return live.decide();}
    public void learn(int[] draw){if(!live.hasPending())live.decide();live.settle(draw);hub.update(draw);}
    public float[][] table(){return live.pendingTable(hub.t);}
}
