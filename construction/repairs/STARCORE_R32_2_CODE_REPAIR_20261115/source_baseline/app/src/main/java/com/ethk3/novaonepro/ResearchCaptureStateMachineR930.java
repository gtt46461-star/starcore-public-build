package com.ethk3.novaonepro;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.util.LinkedHashMap;
import java.util.Locale;

/**
 * R930/C04 transaction-safe predraw capture lifecycle.
 * planned -> system_available -> eligible -> selected -> execution_started -> frozen.
 * Every opportunity stays in the denominator; system pressure/failure has an explicit terminal code.
 */
public final class ResearchCaptureStateMachineR930 {
    public static final String REVISION="R930_CAPTURE_STATE_V1";
    private ResearchCaptureStateMachineR930(){}

    public enum Stage { PLANNED,SYSTEM_AVAILABLE,ELIGIBLE,SELECTED,EXECUTION_STARTED,FROZEN,PAUSED_SYSTEM,FAILED_EXECUTION }
    public static final class Snapshot {
        public final String key,targetIssue,candidateId; public final Stage stage; public final String reason,inputHash,outputHash,eventHash;
        public final long updatedAt;
        Snapshot(String key,String issue,String candidate,Stage stage,String reason,String inputHash,String outputHash,String eventHash,long updatedAt){this.key=key;targetIssue=issue;candidateId=candidate;this.stage=stage;this.reason=reason;this.inputHash=inputHash;this.outputHash=outputHash;this.eventHash=eventHash;this.updatedAt=updatedAt;}
        public boolean frozen(){return stage==Stage.FROZEN;}
    }

    public static Snapshot transition(final DbStore store,final String targetIssue,final String candidateId,
                                      final Stage expected,final Stage next,final String reason,
                                      final String inputHash,final String outputHash){
        if(store==null)throw new IllegalArgumentException("R930_CAPTURE_STORE");
        final String issue=required(targetIssue),candidate=required(candidateId),why=required(reason);
        final String in=optionalHash(inputHash),out=optionalHash(outputHash);
        validateTransition(expected,next,in,out);
        final String identity=V51ChecksumEngine.sha256(REVISION+'|'+issue+'|'+candidate);
        final String prefix="r930_capture_"+identity.substring(0,24);
        final Snapshot[] result=new Snapshot[1];
        store.coordinatorR36().runAtomicWrite("R930_CAPTURE_CAS",new DatabaseCoordinator.WriteRunnable(){
            public void run(SQLiteDatabase db){
                String current=read(db,prefix+"_stage","");
                Stage actual=current.length()==0?null:Stage.valueOf(current);
                if(actual!=expected)throw new IllegalStateException("R930_CAPTURE_CAS|expected="+expected+"|actual="+actual);
                long now=System.currentTimeMillis();
                String event=V51ChecksumEngine.sha256(identity+'|'+String.valueOf(expected)+"->"+next+'|'+why+'|'+in+'|'+out);
                LinkedHashMap<String,String> state=new LinkedHashMap<String,String>();
                state.put(prefix+"_issue",issue);state.put(prefix+"_candidate",candidate);state.put(prefix+"_stage",next.name());
                state.put(prefix+"_reason",why);state.put(prefix+"_input",in);state.put(prefix+"_output",out);state.put(prefix+"_event",event);state.put(prefix+"_updated",String.valueOf(now));
                state.put("r930_capture_event_"+event.substring(0,24),identity+'|'+next+'|'+why+'|'+now);
                increment(db,state,"r930_capture_count_planned",next==Stage.PLANNED?1:0);
                increment(db,state,"r930_capture_count_system_available",next==Stage.SYSTEM_AVAILABLE?1:0);
                increment(db,state,"r930_capture_count_eligible",next==Stage.ELIGIBLE?1:0);
                increment(db,state,"r930_capture_count_selected",next==Stage.SELECTED?1:0);
                increment(db,state,"r930_capture_count_execution_started",next==Stage.EXECUTION_STARTED?1:0);
                increment(db,state,"r930_capture_count_frozen",next==Stage.FROZEN?1:0);
                increment(db,state,"r930_capture_count_paused_system",next==Stage.PAUSED_SYSTEM?1:0);
                increment(db,state,"r930_capture_count_failed_execution",next==Stage.FAILED_EXECUTION?1:0);
                if(next==Stage.PLANNED)increment(db,state,"r930_capture_opportunity_denominator",1);
                if(next==Stage.FROZEN){state.put(prefix+"_frozen_hash",out);state.put(prefix+"_terminal","SUCCESS");}
                else if(next==Stage.PAUSED_SYSTEM||next==Stage.FAILED_EXECUTION)state.put(prefix+"_terminal","NON_SUCCESS|"+next.name());
                DbStore.putStatesR36(db,state);
                result[0]=new Snapshot(prefix,issue,candidate,next,why,in,out,event,now);
            }
        });
        if(result[0]==null)throw new IllegalStateException("R930_CAPTURE_WRITE_NOT_CONFIRMED");
        return result[0];
    }

    public static Snapshot plan(DbStore store,String issue,String candidate,String inputHash){return transition(store,issue,candidate,null,Stage.PLANNED,"PRE_REGISTERED_OPPORTUNITY",inputHash,"");}
    public static Snapshot systemAvailable(DbStore store,String issue,String candidate,String inputHash){return transition(store,issue,candidate,Stage.PLANNED,Stage.SYSTEM_AVAILABLE,"SYSTEM_RESOURCE_AVAILABLE",inputHash,"");}
    public static Snapshot eligible(DbStore store,String issue,String candidate,String inputHash){return transition(store,issue,candidate,Stage.SYSTEM_AVAILABLE,Stage.ELIGIBLE,"ALGORITHM_ELIGIBLE",inputHash,"");}
    public static Snapshot selected(DbStore store,String issue,String candidate,String inputHash){return transition(store,issue,candidate,Stage.ELIGIBLE,Stage.SELECTED,"FROZEN_LABEL_FREE_SCHEDULE",inputHash,"");}
    public static Snapshot executionStarted(DbStore store,String issue,String candidate,String inputHash){return transition(store,issue,candidate,Stage.SELECTED,Stage.EXECUTION_STARTED,"EXECUTION_STARTED",inputHash,"");}
    public static Snapshot frozen(DbStore store,String issue,String candidate,String inputHash,String outputHash){return transition(store,issue,candidate,Stage.EXECUTION_STARTED,Stage.FROZEN,"PREDRAW_OUTPUT_FROZEN",inputHash,outputHash);}
    public static Snapshot pauseSystem(DbStore store,String issue,String candidate,Stage expected,String reason,String inputHash){return transition(store,issue,candidate,expected,Stage.PAUSED_SYSTEM,"SYSTEM:"+required(reason),inputHash,"");}
    public static Snapshot failExecution(DbStore store,String issue,String candidate,String reason,String inputHash){return transition(store,issue,candidate,Stage.EXECUTION_STARTED,Stage.FAILED_EXECUTION,"EXECUTION:"+required(reason),inputHash,"");}

    /** Exact denominators can be rebuilt from persisted counters without deleting system rejects. */
    public static String denominatorSummary(DbStore store){if(store==null)return "N/A";return "opportunity="+store.getState("r930_capture_opportunity_denominator","0")+"|available="+store.getState("r930_capture_count_system_available","0")+"|eligible="+store.getState("r930_capture_count_eligible","0")+"|selected="+store.getState("r930_capture_count_selected","0")+"|started="+store.getState("r930_capture_count_execution_started","0")+"|frozen="+store.getState("r930_capture_count_frozen","0")+"|systemPause="+store.getState("r930_capture_count_paused_system","0")+"|execFail="+store.getState("r930_capture_count_failed_execution","0");}

    private static void validateTransition(Stage expected,Stage next,String input,String output){
        if(next==null)throw new IllegalArgumentException("R930_CAPTURE_NEXT");
        boolean ok=(expected==null&&next==Stage.PLANNED)||(expected==Stage.PLANNED&&(next==Stage.SYSTEM_AVAILABLE||next==Stage.PAUSED_SYSTEM))||(expected==Stage.SYSTEM_AVAILABLE&&(next==Stage.ELIGIBLE||next==Stage.PAUSED_SYSTEM))||(expected==Stage.ELIGIBLE&&(next==Stage.SELECTED||next==Stage.PAUSED_SYSTEM))||(expected==Stage.SELECTED&&(next==Stage.EXECUTION_STARTED||next==Stage.PAUSED_SYSTEM))||(expected==Stage.EXECUTION_STARTED&&(next==Stage.FROZEN||next==Stage.FAILED_EXECUTION||next==Stage.PAUSED_SYSTEM));
        if(!ok)throw new IllegalArgumentException("R930_CAPTURE_TRANSITION:"+expected+"->"+next);
        if(input.length()!=64)throw new IllegalArgumentException("R930_CAPTURE_INPUT_HASH_REQUIRED");
        if(next==Stage.FROZEN&&output.length()!=64)throw new IllegalArgumentException("R930_CAPTURE_OUTPUT_HASH_REQUIRED");
        if(next!=Stage.FROZEN&&output.length()!=0)throw new IllegalArgumentException("R930_CAPTURE_OUTPUT_ONLY_WHEN_FROZEN");
    }
    private static void increment(SQLiteDatabase db,LinkedHashMap<String,String> pending,String key,int delta){if(delta==0)return;long prior=parseLong(read(db,key,"0"),0);pending.put(key,String.valueOf(Math.max(0,prior+delta)));}
    private static String read(SQLiteDatabase db,String key,String def){Cursor c=db.rawQuery("SELECT state_value FROM "+DbHelper.TABLE_APP_STATE+" WHERE state_key=? LIMIT 1",new String[]{key});try{return c.moveToFirst()?c.getString(0):def;}finally{c.close();}}
    private static long parseLong(String x,long d){try{return Long.parseLong(x);}catch(Throwable e){return d;}}
    private static String required(String x){String v=x==null?"":x.trim();if(v.length()==0)throw new IllegalArgumentException("R930_CAPTURE_REQUIRED");return v;}
    private static String optionalHash(String x){String v=x==null?"":x.trim().toLowerCase(Locale.US);if(v.length()==0)return "";if(!v.matches("[0-9a-f]{64}"))throw new IllegalArgumentException("R930_CAPTURE_HASH");return v;}

public static String denominatorSummarySnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, com.ethk3.novaonepro.ReportReadContextCP4 store){if(store==null)return "N/A";return "opportunity="+store.getState("r930_capture_opportunity_denominator","0")+"|available="+store.getState("r930_capture_count_system_available","0")+"|eligible="+store.getState("r930_capture_count_eligible","0")+"|selected="+store.getState("r930_capture_count_selected","0")+"|started="+store.getState("r930_capture_count_execution_started","0")+"|frozen="+store.getState("r930_capture_count_frozen","0")+"|systemPause="+store.getState("r930_capture_count_paused_system","0")+"|execFail="+store.getState("r930_capture_count_failed_execution","0");}
}
