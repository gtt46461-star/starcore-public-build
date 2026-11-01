package com.ethk3.novaonepro;
import java.util.*;
/** Bounded, process-scoped observations. Only publish() after the writer returns takes a monitor. */
public final class FtrlWriteTraceR30 {
    private FtrlWriteTraceR30(){}
    public enum Phase { TAIL_READ, TARGET_DEDUP, ARTIFACT_INSERT, MATCH_READ, RUNTIME_EVENT_APPEND, LEARNED_LINK_APPEND }
    static final ThreadLocal<Trace> ACTIVE=new ThreadLocal<>();
    static final ArrayDeque<Map<String,Object>> RECENT=new ArrayDeque<>();
    static long observed;
    public static final class Trace implements AutoCloseable {
        final String issue,identity;final long encodedBytes;final long[] nanos=new long[Phase.values().length],calls=new long[nanos.length];
        final Map<String,Object> fields=new LinkedHashMap<>();String outcome="NOT_RETURNED";boolean closed;final boolean enabled;
        Trace(String issue,String identity,long bytes,boolean enabled){this.issue=issue;this.identity=identity;encodedBytes=bytes;this.enabled=enabled;}
        public void outcome(String value){outcome=value;}
        @Override public void close(){if(closed)return;closed=true;if(!enabled)return;if(ACTIVE.get()==this)ACTIVE.remove();publish(this);}
    }
    public static Trace open(String issue,String learningIdentity,long encodedBytes){Trace trace=new Trace(issue,learningIdentity,encodedBytes,ACTIVE.get()==null);if(trace.enabled)ACTIVE.set(trace);return trace;}
    public static long start(){return ACTIVE.get()==null?0:System.nanoTime();}
    public static void end(Phase phase,long start){Trace trace=ACTIVE.get();if(trace!=null&&start!=0){int i=phase.ordinal();trace.nanos[i]+=Math.max(0,System.nanoTime()-start);trace.calls[i]++;}}
    public static void field(String key,Object value){Trace trace=ACTIVE.get();if(trace!=null&&trace.fields.size()<12)trace.fields.put(key,value);}
    static synchronized void publish(Trace trace){Map<String,Object> row=new LinkedHashMap<>();row.put("issue",trace.issue);row.put("learningIdentity",trace.identity);row.put("encodedBytes",trace.encodedBytes);row.put("outcome",trace.outcome);row.put("fields",new LinkedHashMap<>(trace.fields));Map<String,Object> phases=new LinkedHashMap<>();for(Phase p:Phase.values()){Map<String,Object> value=new LinkedHashMap<>();value.put("calls",trace.calls[p.ordinal()]);value.put("totalNanos",trace.nanos[p.ordinal()]);phases.put(p.name(),value);}row.put("phases",phases);row.put("publishedWallMs",System.currentTimeMillis());RECENT.addLast(row);while(RECENT.size()>5)RECENT.removeFirst();observed++;}
    @SuppressWarnings("unchecked") public static synchronized Map<String,Object> snapshot(){Map<String,Object> result=new LinkedHashMap<>();result.put("version","FTRL_WRITE_PHASES_R30_REPAIR_1");result.put("scope","CURRENT_PROCESS_BOUNDED_OBSERVATION_NOT_DB_SNAPSHOT_NOT_24H_SLO");result.put("processSession",com.ethk3.novaonepro.strategyv2.WriterHoldLedgerR30.PROCESS_SESSION);result.put("observedTransactions",observed);result.put("shown",RECENT.size());result.put("omitted",Math.max(0,observed-RECENT.size()));result.put("recent",new ArrayList<>(RECENT));result.put("measurementLimitation","No actual large-device database rerun, index-cost or long-window acceptance is implied");return (Map<String,Object>)com.ethk3.novaonepro.strategyv2.JsonR27.parse(com.ethk3.novaonepro.strategyv2.JsonR27.write(result));}
}
