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
    /** Shared report projection. An absent adapter is a named missing source, never a healthy zero. */
    public static java.util.Map<String,Object> summarize(Object scheduled,Object committed,long calendar){
        java.util.Map<String,java.util.Map<String,Object>> owners=index(scheduled,"task_id"),facts=index(committed,"task_id");
        java.util.List<Object> rows=new java.util.ArrayList<>();long stalled=0,unknown=0,maxLag=0,trainingUnknown=0;
        for(ResearchTaskRegistryR30R1.Entry entry:ResearchTaskRegistryR30R1.all().values()){
            java.util.Map<String,Object> owner=owners.get(entry.id),fact=facts.get(entry.id),row=new java.util.LinkedHashMap<>();
            row.put("taskId",entry.id);row.put("owner",entry.owner);row.put("workUnit",entry.unit.name());row.put("inputSemantics",entry.input);row.put("trainingOwner",entry.training);
            String reason=owner==null?"WAITING_REGISTRATION":String.valueOf(owner.get("blocked_reason"));String status=owner==null?"":String.valueOf(owner.get("status"));
            row.put("entryState",owner==null?"WAITING":status.equals("BLOCKED")||status.equals("QUARANTINED")?"BLOCKED":status.equals("RUNNING")||status.equals("YIELDED")?"ACTIVE":"WAITING");row.put("blockedReason",reason);
            State state=State.UNKNOWN;Long lag=null;
            if(fact!=null&&entry.unit.name().equals(fact.get("work_unit"))){
                row.put("committedFacts",fact);long pending=n(fact.get("pending")),clock=n(fact.get("last_commit_clock_ordinal")),oldest=n(fact.get("oldest_pending_ordinal"));
                row.put("pending",pending<0?null:pending);row.put("periodsSinceCommit",clock<0||calendar<clock?null:calendar-clock);
                if(pending>=0){try{TaskProgressR30R1 p=new TaskProgressR30R1(entry.unit,n(fact.get("work_cursor")),n(fact.get("known_latest")),pending,oldest,clock);state=p.state(calendar,reason.startsWith("DEPENDENCY")||reason.startsWith("WAITING_SELF_CONTAINED")||reason.startsWith("WAITING_LEGACY_RESEARCH_SCHEMA"),status.equals("RUNNING"));lag=p.oldestLag(calendar);}catch(IllegalArgumentException invalid){row.put("sourceFailure","INVALID_TYPED_PROGRESS_UNITS");}}
                else row.put("sourceFailure","OWNER_HAS_NOT_MATERIALIZED_PENDING_COUNT");
            }else row.put("sourceFailure",fact==null?"NO_ARTIFACT_BOUND_TYPED_PROGRESS":"OWNER_WORK_UNIT_CHANGED_REQUIRES_CURRENT_RECEIPT");
            row.put("state",state.name());row.put("oldestPendingCalendarLag",lag);if(state==State.STALLED)stalled++;if(state==State.UNKNOWN)unknown++;
            if(entry.training){if(state==State.UNKNOWN||n(fact==null?null:fact.get("pending"))>0&&lag==null)trainingUnknown++;if(lag!=null)maxLag=Math.max(maxLag,lag);}
            rows.add(row);
        }
        java.util.Map<String,Object> summary=new java.util.LinkedHashMap<>();summary.put("tasks",rows.size());summary.put("originalEntrances",57);summary.put("additionalRepairTasks",ResearchTaskRegistryR30R1.ADDITIONAL.size());summary.put("stalled",stalled);summary.put("unknown",unknown);summary.put("m12",unknown>0?null:stalled);summary.put("oldestBacklogIssues",trainingUnknown>0?null:maxLag);summary.put("observedOldestBacklogIssues",maxLag);summary.put("trainingTasksWithUnknownSource",trainingUnknown);
        java.util.Map<String,Object> result=new java.util.LinkedHashMap<>();result.put("version",ResearchTaskRegistryR30R1.VERSION);result.put("tasks",rows);result.put("summary",summary);result.put("scope","57 original entrances plus 2 repair tasks; artifact transaction facts only. Work units never subtracted from calendar. Missing facts remain UNKNOWN.");return result;
    }
    private static long n(Object value){return value instanceof Number?((Number)value).longValue():-1;}
    @SuppressWarnings("unchecked") private static java.util.Map<String,java.util.Map<String,Object>> index(Object rows,String key){java.util.Map<String,java.util.Map<String,Object>> result=new java.util.LinkedHashMap<>();if(rows instanceof java.util.List)for(Object item:(java.util.List<?>)rows)if(item instanceof java.util.Map){java.util.Map<String,Object> row=(java.util.Map<String,Object>)item;result.put(String.valueOf(row.get(key)),row);}return result;}
}
