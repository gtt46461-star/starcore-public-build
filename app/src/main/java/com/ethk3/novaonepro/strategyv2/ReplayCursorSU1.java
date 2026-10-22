package com.ethk3.novaonepro.strategyv2;
import java.io.*;

/** A period cursor survives both budget yields and process death. The current period commits last. */
public final class ReplayCursorSU1 implements AutoCloseable {
    public final String text;
    public final double coverage;
    public final boolean margin;
    public final int start,end;
    public final int probeEnd;
    public int cursor;
    public final HistoricalReplayEngineSU1.Replay replay;
    private final int[][] draws;
    private final ProgramTreeSU1.Node program;
    private final AtomLiftSU1 atoms;
    private final ScoreHistoryCacheSU1 cache=new ScoreHistoryCacheSU1(2048);
    private ProgramTreeSU1.Context root;
    private boolean done;
    public ReplayCursorSU1(int[][] draws,String text,double coverage,boolean margin,int start,int end){
        this(draws,text,coverage,margin,start,end,end);
    }
    public ReplayCursorSU1(int[][] draws,String text,double coverage,boolean margin,int start,int end,int probeEnd){
        if(start<1||end<=start||end>draws.length||draws.length>20000)throw new IllegalArgumentException("SU1_REPLAY_CURSOR");
        this.draws=draws;this.text=ProgramTextSU1.canonical(text);this.coverage=coverage;this.margin=margin;this.start=start;this.end=end;
        if(probeEnd<=start||probeEnd>end)throw new IllegalArgumentException("SU1_PROBE_RANGE");this.probeEnd=probeEnd;
        cursor=Math.max(0,start-720);program=ProgramTreeSU1.parse(text);atoms=new AtomLiftSU1(draws);replay=new HistoricalReplayEngineSU1.Replay(end);
    }
    public boolean advance(BudgetTokenSU1 budget){
        if(done)return true;root=new ProgramTreeSU1.Context(draws,cursor,atoms,cache,budget);
        for(;cursor<end;cursor++){
            budget.checkpointOrYield();double[][] scores=program.score(root.atTarget(cursor));int[] action=ProgramTreeSU1.action(scores);
            double gate=HistoricalReplayEngineSU1.gateValue(scores,action[0],margin);int mask=(1<<(action[1]-1))|(1<<(action[2]-1))|(1<<(action[3]-1));
            replay.gate[cursor]=gate;replay.position[cursor]=action[0];replay.mask[cursor]=mask;replay.hit[cursor]=(mask&(1<<(draws[cursor][action[0]-1]-1)))!=0;
            int probe=BehaviorSignatureSU1.probeIndex(cursor,start,probeEnd);if(probe>=0)replay.probeScores[probe]=BehaviorSignatureSU1.scoreHash(scores);
        }
        for(int block=start;block<end;block+=50){budget.checkpointOrYield();double threshold=coverage>=.999?Double.NEGATIVE_INFINITY:HistoricalReplayEngineSU1.quantileLinear(replay.gate,Math.max(0,block-720),block,1-coverage);for(int t=block;t<Math.min(block+50,end);t++)replay.act[t]=replay.gate[t]>=threshold;}
        done=true;return true;
    }
    public void write(DataOutput out)throws IOException{
        out.writeUTF(text);out.writeDouble(coverage);out.writeBoolean(margin);out.writeInt(start);out.writeInt(end);out.writeInt(probeEnd);out.writeInt(cursor);out.writeBoolean(done);
        for(int t=Math.max(0,start-720);t<cursor;t++){out.writeDouble(replay.gate[t]);out.writeByte(replay.position[t]);out.writeShort(replay.mask[t]);out.writeBoolean(replay.hit[t]);out.writeBoolean(replay.act[t]);}
        for(String hash:replay.probeScores)out.writeUTF(hash==null?"":hash);
    }
    public static ReplayCursorSU1 read(DataInput in,int[][] draws)throws IOException{
        ReplayCursorSU1 c=new ReplayCursorSU1(draws,in.readUTF(),in.readDouble(),in.readBoolean(),in.readInt(),in.readInt(),in.readInt());int cursor=in.readInt();boolean done=in.readBoolean();
        if(cursor<c.cursor||cursor>c.end)throw new IOException("SU1_REPLAY_CHECKPOINT_RANGE");
        for(int t=c.cursor;t<cursor;t++){c.replay.gate[t]=in.readDouble();c.replay.position[t]=in.readByte();c.replay.mask[t]=in.readShort();c.replay.hit[t]=in.readBoolean();c.replay.act[t]=in.readBoolean();}
        for(int i=0;i<64;i++){String hash=in.readUTF();c.replay.probeScores[i]=hash.isEmpty()?null:HashingSU1.sha(hash);}
        c.cursor=cursor;c.done=done;return c;
    }
    @Override public void close(){atoms.close();cache.clear();}
}
