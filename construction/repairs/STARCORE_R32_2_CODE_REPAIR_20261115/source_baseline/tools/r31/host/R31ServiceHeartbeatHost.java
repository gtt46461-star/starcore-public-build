package com.ethk3.novaonepro;

import android.content.SharedPreferences;
import com.ethk3.novaonepro.strategyv2.RuntimeCoordinatorR26.RuntimeHealthSnapshot;
import com.ethk3.novaonepro.strategyv2.IssueTimelineWriterR30;
import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Executes production preference/recovery semantics with an in-memory Android platform adapter. */
final class R31ServiceHeartbeatHost {
    private static void need(boolean value,String reason){if(!value)throw new AssertionError(reason);}
    private static final class PreferencesApp extends android.app.Application {
        final Map<String,Object> values=new LinkedHashMap<>();
        final SharedPreferences prefs;
        PreferencesApp(File path){super(path);prefs=(SharedPreferences)Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{SharedPreferences.class},(proxy,method,args)->{
            String name=method.getName();if(name.equals("contains"))return values.containsKey(args[0]);if(name.equals("getAll"))return Collections.unmodifiableMap(values);
            if(name.startsWith("get"))return values.getOrDefault(args[0],args[1]);
            if(name.equals("edit")){Map<String,Object> changes=new LinkedHashMap<>();return Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{SharedPreferences.Editor.class},(editor,operation,params)->{
                String op=operation.getName();if(op.startsWith("put")){changes.put((String)params[0],params[1]);return editor;}if(op.equals("remove")){changes.put((String)params[0],null);return editor;}if(op.equals("clear")){values.clear();return editor;}
                if(op.equals("apply")||op.equals("commit")){for(Map.Entry<String,Object> item:changes.entrySet())if(item.getValue()==null)values.remove(item.getKey());else values.put(item.getKey(),item.getValue());return op.equals("commit")?Boolean.TRUE:null;}return null;
            });}return null;
        });}
        @Override public SharedPreferences getSharedPreferences(String name,int mode){return prefs;}
    }
    private static RuntimeHealthSnapshot snapshot(String health,String artifact,Long ordinal,int unknown)throws Exception{
        Constructor<RuntimeHealthSnapshot> constructor=RuntimeHealthSnapshot.class.getDeclaredConstructor(String.class,String.class,long.class,long.class,String.class,String.class,String.class,Long.class,Long.class,String.class,int.class,int.class,int.class,int.class,int.class,java.util.List.class);constructor.setAccessible(true);
        return constructor.newInstance("HOST_OWNER",IssueTimelineWriterR30.bootId(),System.currentTimeMillis(),android.os.SystemClock.elapsedRealtime(),"ALIVE_OWNER_RUNNING",health,"HOST_ENGINEERING_FIXTURE",100L,ordinal,artifact,0,0,0,unknown,0,Collections.emptyList());
    }
    static void verify(File path)throws Exception{
        PreferencesApp app=new PreferencesApp(path);ServiceHeartbeatStore state=new ServiceHeartbeatStore(app);
        state.setPendingSettlementCount(9);state.markSettlementTerminal("202611040100","COMPLETED","HOST",System.currentTimeMillis());need(app.values.get("pending_settlement_count").equals(9),"one terminal must not zero global pending");
        state.setPendingSettlementCount(-1);need(app.values.get("pending_settlement_count").equals(-1)&&Boolean.FALSE.equals(app.values.get("pending_settlement_known")),"missing pending remains unknown, never zero");
        need(state.tryAcquireRecoveryLease("HOST_FIRST"),"first monotonic lease");app.values.put("recovery_lease_until",-1L);need(!state.tryAcquireRecoveryLease("HOST_WALL_ROLLBACK"),"wall-clock edit cannot steal current boot/epoch monotonic lease");state.releaseRecoveryLease("HOST_ACTUAL_RELEASE");need(state.tryAcquireRecoveryLease("HOST_SECOND"),"explicit release enables next request");
        state.markServiceReady("HOST_SERVICE_READY");need(app.values.get("r31_recovery_state").equals("STARTED_AWAITING_PROGRESS"),"service-ready is not research recovery");
        state.markResearchHealthR31(snapshot("UNKNOWN","OLD_CHECKPOINT",90L,1));need(!app.values.get("r31_recovery_state").equals("RECOVERED_ARTIFACT_CONFIRMED")&&state.getResearchStaleReason().startsWith("RESEARCH_UNKNOWN"),"first cached old checkpoint does not prove recovered and unknown stays unknown");
        state.noteResearchRecoveryRequestR31(snapshot("STALLED","OLD_CHECKPOINT",90L,0),"HOST_CONTROL");state.markResearchHealthR31(snapshot("STALLED","OLD_CHECKPOINT",90L,0));need(app.values.get("r31_recovery_state").equals("CONTROL_REQUESTED_AWAITING_PROGRESS"),"control wake cannot prove artifact recovery");
        state.markResearchHealthR31(snapshot("PROGRESS_PENDING","NEW_DURABLE_ARTIFACT",91L,0));need(app.values.get("r31_recovery_state").equals("RECOVERED_ARTIFACT_CONFIRMED")&&state.getResearchStaleReason().isEmpty(),"new same-owner committed artifact proves progress separately from service heartbeat");
        app.values.put("r31_research_sample_elapsed",android.os.SystemClock.elapsedRealtime()-16000);need(state.getResearchStaleReason().equals("RESEARCH_HEALTH_UNKNOWN_STALE_SAMPLE"),"stale cached health does not prove current health");
        System.out.println("R31_SERVICE_HEARTBEAT_HOST_PASS unknown_pending=true terminal_not_global_zero=true monotonic_recovery_lease=true service_ready_not_recovered=true cached_checkpoint_not_new_progress=true actual_new_artifact_required=true stale_sample_unknown=true scope=HOST_PLATFORM_ADAPTER_NOT_ANDROID_SERVICE_OR_BOOT_DELIVERY");
    }
}
