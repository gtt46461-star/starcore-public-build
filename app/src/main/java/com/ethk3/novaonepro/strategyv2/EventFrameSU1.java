package com.ethk3.novaonepro.strategyv2;

/** Immutable, availability-aware PIT input. History is strictly chronological. */
public final class EventFrameSU1 {
    public final long targetIssue,targetOpenTime,cutoff,sourceMaxIssue,sourceAvailableAt,sourceGeneration;
    public final String sourceEpoch,sourceVersionHash,contentHash;
    private final int[][] history;
    private final long[] issues,available;
    public EventFrameSU1(long target,long open,long cutoff,String epoch,long[] issues,long[] available,int[][] history) {
        this(target,open,cutoff,epoch,issues,available,history,0);
    }
    public EventFrameSU1(long target,long open,long cutoff,String epoch,long[] issues,long[] available,int[][] history,long generation) {
        if(target<1||open<=cutoff||cutoff<=0||epoch==null||epoch.isEmpty()||history==null||history.length<20||history.length>5000
                ||issues.length!=history.length||available.length!=history.length)throw new IllegalArgumentException("SU1_FRAME");
        this.targetIssue=target;this.targetOpenTime=open;this.cutoff=cutoff;sourceEpoch=epoch;sourceGeneration=generation;
        this.issues=issues.clone();this.available=available.clone();this.history=new int[history.length][];
        long maximumAvailable=0;
        StringBuilder source=new StringBuilder("SU1_SOURCE_V1|");
        for(int i=0;i<history.length;i++) {
            ExecutionBudgetR26.poll();
            if(issues[i]>=target||issues[i]<1||(i>0&&issues[i]<=issues[i-1])||available[i]<=0||available[i]>cutoff)
                throw new IllegalArgumentException("SU1_FUTURE_OR_UNAVAILABLE_SOURCE");
            RelationalStateInference.validateDraw(history[i]);this.history[i]=history[i].clone();
            maximumAvailable=Math.max(maximumAvailable,available[i]);source.append(issues[i]).append('@').append(available[i]).append(':');
            for(int v:history[i])source.append(v).append(',');source.append(';');
        }
        sourceMaxIssue=issues[issues.length-1];sourceAvailableAt=maximumAvailable;
        sourceVersionHash=HashingSU1.fields(epoch,String.valueOf(generation),source.toString());
        contentHash=HashingSU1.fields("SU1_FRAME_V1",String.valueOf(target),String.valueOf(open),String.valueOf(cutoff),sourceVersionHash);
    }
    public int size(){return history.length;}
    public long issue(int index){return issues[index];}
    public long availableAt(int index){return available[index];}
    public int[][] history(){int[][] out=new int[history.length][];for(int i=0;i<out.length;i++){ExecutionBudgetR26.poll();out[i]=history[i].clone();}return out;}
    public String payload(){StringBuilder b=new StringBuilder();for(int i=0;i<history.length;i++){b.append(issues[i]).append('@').append(available[i]);for(int v:history[i])b.append(',').append(v);b.append('\n');}return b.toString();}
}
