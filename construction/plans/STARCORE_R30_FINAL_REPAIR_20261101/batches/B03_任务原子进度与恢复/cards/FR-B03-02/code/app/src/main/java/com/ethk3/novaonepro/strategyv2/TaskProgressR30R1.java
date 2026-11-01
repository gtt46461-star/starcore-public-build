package com.ethk3.novaonepro.strategyv2;

/** Work cursor, source watermark and commit clock are different units. */
public final class TaskProgressR30R1 {
    public enum Unit { ISSUE, POSITION_SAMPLE, GENERATION, PHASE, POLICY, TIME_TICK }
    public enum State { UNKNOWN, BLOCKED_DEPENDENCY, IDLE_WAITING_INPUT, RUNNING, BACKLOG, STALLED }
    public final Unit unit;public final long cursor,latest,pending,oldestPendingOrdinal,commitClockOrdinal;
    public TaskProgressR30R1(Unit unit,long cursor,long latest,long pending,long oldest,long committedClock){
        this.unit=java.util.Objects.requireNonNull(unit);this.cursor=cursor;this.latest=latest;this.pending=pending;oldestPendingOrdinal=oldest;commitClockOrdinal=committedClock;
        if(cursor<0||latest<cursor||pending<0||oldest< -1||committedClock< -1)throw new IllegalArgumentException("TASK_PROGRESS_UNITS");
        if(pending==0&&oldest!=-1)throw new IllegalArgumentException("TASK_EMPTY_HAS_OLDEST");
    }
    public State state(long observedCalendarOrdinal,boolean dependency,boolean running){
        if(dependency)return State.BLOCKED_DEPENDENCY;
        if(pending==0)return State.IDLE_WAITING_INPUT;
        if(commitClockOrdinal<0||observedCalendarOrdinal<commitClockOrdinal)return State.UNKNOWN;
        if(observedCalendarOrdinal-commitClockOrdinal>=5)return State.STALLED;
        return running?State.RUNNING:State.BACKLOG;
    }
    public Long oldestLag(long observedCalendarOrdinal){return pending==0?null:oldestPendingOrdinal<0||observedCalendarOrdinal<oldestPendingOrdinal?null:observedCalendarOrdinal-oldestPendingOrdinal;}
}
