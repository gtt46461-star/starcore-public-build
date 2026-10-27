package com.ethk3.novaonepro.strategyv2;

import java.io.*;
import java.util.*;
import java.util.zip.*;

/** One complete legacy program/committee/stage cohort; no DB or wall-clock effects in numerical steps. */
public final class LegacyReplayStateR26 {
    public static final int MAX_BYTES=32<<20,MAX_MEMBERS=32,MAX_CALIBRATORS=48;
    public final List<String> canonical;
    public final ModelLiveSU2.Hub hub;
    public final LinkedHashMap<String,LiveStrategySU2> lives=new LinkedHashMap<>();
    public final long genesis;
    private long gapStart,through;

    public LegacyReplayStateR26(List<String> canonical,long genesis){
        if(canonical==null||canonical.size()>MAX_MEMBERS||genesis<0)throw new IllegalArgumentException("R26_LEGACY_ROSTER");
        this.canonical=Collections.unmodifiableList(new ArrayList<>(canonical));this.genesis=genesis;this.through=genesis-1;this.hub=new ModelLiveSU2.Hub();
        for(String text:canonical){String key=key(text);if(lives.put(key,make(text,hub,genesis))!=null)throw new IllegalArgumentException("R26_LEGACY_DUPLICATE");}
        if(hub.cal.size()>MAX_CALIBRATORS)throw new IllegalArgumentException("R26_LEGACY_CALIBRATOR_CAPACITY");
    }
    /** Import complete original states, retaining their learned parameters and ordered history cursor. */
    static LegacyReplayStateR26 imported(List<String> canonical,long genesis,long through,ModelLiveSU2.Hub hub,Map<String,LiveStrategySU2> live){return new LegacyReplayStateR26(canonical,genesis,through,hub,live);}
    private LegacyReplayStateR26(List<String> canonical,long genesis,long through,ModelLiveSU2.Hub hub,Map<String,LiveStrategySU2> live){this.canonical=Collections.unmodifiableList(new ArrayList<>(canonical));this.genesis=genesis;this.through=through;this.hub=hub;this.lives.putAll(live);if(hub.t<0||through<genesis-1||lives.size()!=canonical.size()||hub.cal.size()>MAX_CALIBRATORS)throw new IllegalArgumentException("R26_LEGACY_IMPORT_SHAPE");}
    public static String key(String text){return StageStrategySU2.isStage(text)?StageStrategySU2.keyOf(text):CommitteeSU2.isCommittee(text)?CommitteeSU2.parse(text).key():ModelGenomeSU2.parse(text).key();}
    public static LiveStrategySU2 make(String text,ModelLiveSU2.Hub hub,long genesis){
        if(StageStrategySU2.isStage(text))return new StageStrategySU2(StageStrategySU2.definition(text),hub,genesis,1500,2500,250);
        if(CommitteeSU2.isCommittee(text))return new CommitteeLiveSU2(CommitteeSU2.parse(text),hub,1500,2500,250);
        return new ModelLiveSU2(ModelGenomeSU2.parse(text),hub,1500,2500,250);
    }
    public long samples(){return hub.t;}
    public long through(){return through;}
    public void advance(int[] draw){try{advance(draw,through+1);}catch(IOException gap){throw new IllegalStateException(gap);}}
    public void advance(int[] draw,long ordinal)throws IOException{
        OnlineModelsSU2.requireDraw(draw);if(ordinal<=through)throw new IOException("R26_LEGACY_ORDINAL_BACKWARDS");if(ordinal>through+1&&gapStart!=hub.t)onGap();
        for(LiveStrategySU2 live:lives.values())alignOrdinal(live,ordinal);
        for(LiveStrategySU2 live:lives.values()){ExecutionBudgetR26.poll();live.decide();live.settle(draw);}
        hub.update(draw);through=ordinal;
    }
    private static void alignOrdinal(LiveStrategySU2 live,long ordinal){if(live instanceof StageStrategySU2){StageStrategySU2 stage=(StageStrategySU2)live;if(ordinal>stage.def.through&&stage.t<stage.start)stage.start=stage.t;for(LiveStrategySU2 child:stage.subs)alignOrdinal(child,ordinal);}}
    public void onGap()throws IOException {
        for(OnlineModelsSU2.Model model:hub.models.values())if(!TemporalResetR26.supported(model))throw new IOException("R26_LEGACY_GAP_MODEL:"+model.getClass().getSimpleName());
        for(OnlineModelsSU2.Model model:hub.models.values())TemporalResetR26.reset(model);
        hub.predicted=false;hub.p.clear();hub.pc.clear();hub.st.clear();gapStart=hub.t;
        for(LiveStrategySU2 live:lives.values())resetContext(live,gapStart);
    }
    private static void resetContext(LiveStrategySU2 live,long start){
        if(live instanceof ModelLiveSU2)((ModelLiveSU2)live).resetGateContextR26();
        else if(live instanceof CommitteeLiveSU2){CommitteeLiveSU2 c=(CommitteeLiveSU2)live;c.contextStartR26=start;c.curP1=-1;c.lastPooled=null;c.lastPooledT=-1;c.thrValid=false;Arrays.fill(c.gsRing,-1);for(ModelLiveSU2 sub:c.subs)resetContext(sub,start);}
        else if(live instanceof StageStrategySU2){StageStrategySU2 s=(StageStrategySU2)live;s.contextStartR26=start;s.pending=null;s.pooled=null;s.historyN=0;Arrays.fill(s.history,null);Arrays.fill(s.scores,-1);for(boolean[] row:s.hits)Arrays.fill(row,false);Arrays.fill(s.filter.a,1.0/s.filter.a.length);for(LiveStrategySU2 sub:s.subs)resetContext(sub,start);}
        else throw new IllegalArgumentException("R26_LEGACY_GAP_STRATEGY");
    }
    private static void restoreContextStart(LiveStrategySU2 live,long start){
        if(live instanceof ModelLiveSU2)((ModelLiveSU2)live).restoreGateContextStartR26(start);
        else if(live instanceof CommitteeLiveSU2){CommitteeLiveSU2 c=(CommitteeLiveSU2)live;c.contextStartR26=start;for(ModelLiveSU2 sub:c.subs)restoreContextStart(sub,start);}
        else if(live instanceof StageStrategySU2){StageStrategySU2 s=(StageStrategySU2)live;s.contextStartR26=start;for(LiveStrategySU2 sub:s.subs)restoreContextStart(sub,start);}
    }
    private static void writeContext(DataOutput out,LiveStrategySU2 live)throws IOException {
        if(live instanceof ModelLiveSU2){out.writeInt(0);out.writeLong(((ModelLiveSU2)live).gateContextStartR26());}
        else if(live instanceof CommitteeLiveSU2){CommitteeLiveSU2 c=(CommitteeLiveSU2)live;out.writeInt(1);out.writeLong(c.contextStartR26);for(ModelLiveSU2 sub:c.subs)writeContext(out,sub);}
        else if(live instanceof StageStrategySU2){StageStrategySU2 s=(StageStrategySU2)live;out.writeInt(2);out.writeLong(s.contextStartR26);out.writeLong(s.start);for(LiveStrategySU2 sub:s.subs)writeContext(out,sub);}
    }
    private static void readContext(DataInput in,LiveStrategySU2 live)throws IOException {
        int type=in.readInt();long start=in.readLong();
        if(live instanceof ModelLiveSU2&&type==0){if(start<0||start>((ModelLiveSU2)live).t)throw new IOException("R26_LEGACY_GATE_CONTEXT");((ModelLiveSU2)live).restoreGateContextStartR26(start);}
        else if(live instanceof CommitteeLiveSU2&&type==1){CommitteeLiveSU2 c=(CommitteeLiveSU2)live;if(start<0||start>c.t)throw new IOException("R26_LEGACY_COMMITTEE_CONTEXT");c.contextStartR26=start;for(ModelLiveSU2 sub:c.subs)readContext(in,sub);}
        else if(live instanceof StageStrategySU2&&type==2){StageStrategySU2 s=(StageStrategySU2)live;if(start<0||start>s.t)throw new IOException("R26_LEGACY_STAGE_CONTEXT");s.contextStartR26=start;s.start=in.readLong();for(LiveStrategySU2 sub:s.subs)readContext(in,sub);}
        else throw new IOException("R26_LEGACY_CONTEXT_SHAPE");
    }
    public byte[] encode()throws IOException {
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();
        try(DataOutputStream out=new DataOutputStream(new GZIPOutputStream(bytes){{def.setLevel(Deflater.BEST_SPEED);}})){
            out.writeInt(0x264c5301);out.writeLong(genesis);out.writeLong(through);out.writeLong(gapStart);out.writeInt(canonical.size());
            for(String text:canonical){out.writeUTF(text);if(StageStrategySU2.isStage(text)){byte[] bundle=((StageStrategySU2)lives.get(key(text))).def.bytes();out.writeInt(bundle.length);out.write(bundle);}}
            hub.write(out);for(LiveStrategySU2 live:lives.values()){live.write(out);writeContext(out,live);}
        }
        if(bytes.size()>MAX_BYTES)throw new IOException("R26_LEGACY_STATE_CAPACITY");return bytes.toByteArray();
    }
    public static LegacyReplayStateR26 decode(byte[] bytes)throws IOException {
        if(bytes==null||bytes.length>MAX_BYTES)throw new IOException("R26_LEGACY_STATE_CAPACITY");
        try(DataInputStream in=new DataInputStream(new GZIPInputStream(new ByteArrayInputStream(bytes)))){
            if(in.readInt()!=0x264c5301)throw new IOException("R26_LEGACY_STATE_ABI");long genesis=in.readLong(),through=in.readLong(),gap=in.readLong();int n=in.readInt();if(genesis<0||n<0||n>MAX_MEMBERS)throw new IOException("R26_LEGACY_STATE_SHAPE");List<String> canonical=new ArrayList<>();
            for(int i=0;i<n;i++){String text=in.readUTF();canonical.add(text);if(StageStrategySU2.isStage(text)){int size=in.readInt();if(size<1||size>StageStrategySU2.MAX_BUNDLE_BYTES)throw new IOException("R26_LEGACY_STAGE_SIZE");byte[] bundle=new byte[size];in.readFully(bundle);StageStrategySU2.Definition def=StageStrategySU2.Definition.read(bundle);if(!def.text().equals(text))throw new IOException("R26_LEGACY_STAGE_IDENTITY");StageStrategySU2.register(def);}}
            LegacyReplayStateR26 state=new LegacyReplayStateR26(canonical,genesis);state.hub.readInto(in);for(LiveStrategySU2 live:state.lives.values()){live.readInto(in);readContext(in,live);}
            if(gap<0||gap>state.samples()||through<genesis-1||in.read()!=-1)throw new IOException("R26_LEGACY_STATE_CURSOR");state.gapStart=gap;state.through=through;

            return state;
        }catch(IllegalArgumentException bad){throw new IOException("R26_LEGACY_STATE_SPEC",bad);}
    }
}
