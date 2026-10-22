package com.ethk3.novaonepro;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;

/** Actual Activity private entry points; no renderer/domain replacement or fake clock.
 * View fields are absent: this verifies presentation selection and clock admission,
 * not Android layout, lifecycle construction or physical-device frame timing. */
public final class ui_snapshot {
    static int checks;
    static final sun.misc.Unsafe U;
    static final Class<?> lightType;
    static{try{Field f=sun.misc.Unsafe.class.getDeclaredField("theUnsafe");f.setAccessible(true);U=(sun.misc.Unsafe)f.get(null);lightType=Class.forName("com.ethk3.novaonepro.MainActivity$LightSnapshot");}catch(Exception e){throw new ExceptionInInitializerError(e);}}
    static void check(boolean yes,String label){if(!yes)throw new AssertionError(label);checks++;System.out.println("PASS|"+label);}
    static void set(Object o,Class<?> type,String name,Object value)throws Exception{Field f=type.getDeclaredField(name);f.setAccessible(true);f.set(o,value);}
    static Object get(Object o,Class<?> type,String name)throws Exception{Field f=type.getDeclaredField(name);f.setAccessible(true);return f.get(o);}
    static Object call(Object o,String name,Class<?>[] types,Object...args)throws Exception{Method m=MainActivity.class.getDeclaredMethod(name,types);m.setAccessible(true);return m.invoke(o,args);}
    static MainActivity activity(String target)throws Exception {
        MainActivity a=(MainActivity)U.allocateInstance(MainActivity.class);
        set(a,MainActivity.class,"uiLockedTargetIssue",target);set(a,MainActivity.class,"uiLockedCurrentIssue","");
        set(a,MainActivity.class,"uiRevision",new java.util.concurrent.atomic.AtomicLong());
        set(a,MainActivity.class,"homeTruthPresenterR906",new HomeTruthPresenterR906());
        set(a,MainActivity.class,"frameMetricsR906",new FrameMetricsR906());return a;
    }
    static Object light(String target,RuntimeTruthSnapshotR906 truth)throws Exception {
        Constructor<?> ctor=lightType.getDeclaredConstructor();ctor.setAccessible(true);Object s=ctor.newInstance();
        set(s,lightType,"targetIssue",target);set(s,lightType,"uiRevision",1L);set(s,lightType,"truthR906",truth);return s;
    }
    @SuppressWarnings("unchecked") static String render(MainActivity a,Object s)throws Exception {
        call(a,"renderR906Truth",new Class[]{lightType},s);
        return ((HashMap<String,String>)get(s,lightType,"states")).get("r906_truth_snapshot_id");
    }
    static void pending()throws Exception {
        MainActivity a=activity("202610130484");
        Object empty=light("202610130484",null);
        String id=render(a,empty);
        check(id!=null&&id.startsWith("R76-PENDING-"),"LIGHT_READ_WITH_DIAGNOSTIC_PENDING_IS_NOT_READ_ERROR");
        RuntimeTruthSnapshotR906 error=RuntimeTruthSnapshotR906.error("INJECTED_ACTUAL_READ_FAILURE");
        check(error.snapshotId.equals(render(a,light("202610130484",error))),"ACTUAL_READ_ERROR_REMAINS_VISIBLE");
        Method factory=RuntimeTruthSnapshotR906.class.getMethod("pending",String.class);
        RuntimeTruthSnapshotR906 p=(RuntimeTruthSnapshotR906)factory.invoke(null,"202610130484");
        RuntimeTruthSnapshotR906 again=(RuntimeTruthSnapshotR906)factory.invoke(null,"202610130484");
        check(p.hash.equals(again.hash)&&p.snapshotId.equals(again.snapshotId),"PENDING_IDENTITY_STABLE_WITHOUT_PER_SECOND_REBIND");
        check("PENDING".equals(p.status)&&p.readError.isEmpty(),"PENDING_HAS_NO_FABRICATED_FAILURE");
        check(p.whyCards.length==4&&p.stateBoxes.length==16,"ALL_PENDING_TRUTH_SURFACES_EXPLICIT");
        for(RuntimeTruthSnapshotR906.TruthCard c:p.whyCards)check(c.targetIssue.equals(p.targetIssue)&&"PENDING".equals(c.status),"PENDING_CARD_TARGET_"+c.code);
        for(RuntimeTruthSnapshotR906.StateBox b:p.stateBoxes)check(b.targetIssue.equals(p.targetIssue)&&"PENDING".equals(b.status),"PENDING_BOX_TARGET_"+b.ordinal);
        Object stale=light("202610130485",p);
        check(render(a,stale).startsWith("R76-PENDING-202610130485"),"OTHER_TARGET_TRUTH_NEVER_REUSED_AS_CURRENT");
    }
    static CountdownProjectionR971.Anchor anchor(String target,long revision,long delta,String session) {
        long mono=android.os.SystemClock.elapsedRealtime();
        return CountdownProjectionR971.Anchor.received(target,revision,session,System.currentTimeMillis()+delta,mono+delta,mono);
    }
    static void owner(CountdownProjectionR971.Anchor anchor)throws Exception {
        CountdownClock clock=(CountdownClock)U.allocateInstance(CountdownClock.class);
        set(clock,CountdownClock.class,"running",true);set(clock,CountdownClock.class,"clockAnchorCP12",anchor);
        set(null,CountdownClock.class,"liveOwnerR72",clock);
    }
    static long countdown(MainActivity a)throws Exception{return (Long)call(a,"getBestCountdownFromState",new Class[]{Models.AppRuntimeState.class,lightType},null,null);}
    static void countdown()throws Exception {
        String session=TimeSourceR811.clockSessionCP12();
        MainActivity a=activity("");owner(anchor("202610130484",4,55000L,session));
        long remaining=countdown(a);
        check(remaining>53000L&&remaining<=55000L,"RESUMED_ACTIVITY_IMPORTS_VALID_CLOCK_BEFORE_ANY_DB_READ");
        check("202610130484".equals(get(a,MainActivity.class,"uiLockedTargetIssue")),"CLOCK_TARGET_BINDING_COMES_FROM_OWNER");
        owner(anchor("202610130485",5,55000L,session));
        check(countdown(a)>53000L&&"202610130485".equals(get(a,MainActivity.class,"uiLockedTargetIssue")),"NEW_OWNER_TARGET_RECOVERS_MISSED_BROADCAST");
        CountdownProjectionR971.Anchor accepted=(CountdownProjectionR971.Anchor)get(a,MainActivity.class,"liveClockAnchorCP12");
        owner(anchor("202610130484",9,59000L,session));countdown(a);
        check(accepted==get(a,MainActivity.class,"liveClockAnchorCP12"),"OLDER_TARGET_CANNOT_REPLACE_NEW_CLOCK");
        owner(anchor("202610130485",4,59000L,session));countdown(a);
        check(accepted==get(a,MainActivity.class,"liveClockAnchorCP12"),"OLDER_REVISION_CANNOT_EXTEND_CLOCK");
        owner(anchor("202610130486",6,59000L,"different-process-session"));countdown(a);
        check(accepted==get(a,MainActivity.class,"liveClockAnchorCP12"),"OTHER_SESSION_CLOCK_REJECTED");
        MainActivity rendering=activity("202610130483");owner(anchor("202610130485",5,55000L,session));
        Object olderRead=light("202610130484",null);set(olderRead,lightType,"uiRevision",0L);
        call(rendering,"renderLightSnapshot",new Class[]{lightType},olderRead);
        check("202610130485".equals(get(rendering,MainActivity.class,"uiLockedTargetIssue"))
                &&get(rendering,MainActivity.class,"lastLightSnapshot")==null,"CLOCK_ADOPTED_BEFORE_RENDER_STALE_TARGET_FENCE");
        owner(anchor("202610130486",6,-1000L,session));
        check(countdown(a)==0L,"EXPIRED_VERIFIED_CLOCK_DOES_NOT_CREATE_ANOTHER_MINUTE");
        set(null,CountdownClock.class,"liveOwnerR72",null);
        check(countdown(activity("202610130484"))==0L,"NO_CLOCK_REMAINS_WAITING_NO_WALL_TIME_FALLBACK");
    }
    static void cache()throws Exception {
        MainActivity a=activity("202610130484");
        Class<?> observed=Class.forName("com.ethk3.novaonepro.MainActivity$TruthObservationR76");
        Constructor<?> ctor=observed.getDeclaredConstructor(RuntimeTruthSnapshotR906.class,String.class,long.class);ctor.setAccessible(true);
        RuntimeTruthSnapshotR906 sentinel=RuntimeTruthSnapshotR906.error("ACTUAL_SAME_TARGET_QUERY_ERROR");
        set(a,MainActivity.class,"cachedTruthR907",ctor.newInstance(sentinel,"202610130484",TimeSourceR811.monoNowMs()));
        check(call(a,"loadTruthCachedR907",new Class[]{DbStore.class,String.class},null,"202610130484")==sentinel,
                "HEAVY_AND_DASHBOARD_REUSE_SAME_COMPLETED_OBSERVATION_WITHOUT_READ");
        check(sentinel.snapshotId.equals(render(a,light("202610130484",null))),"LIGHT_RENDER_CAN_CONSUME_NEW_DIAGNOSTIC_BEFORE_HEAVY_HISTORY_FINISHES");
        RuntimeTruthSnapshotR906 next=(RuntimeTruthSnapshotR906)call(a,"loadTruthCachedR907",new Class[]{DbStore.class,String.class},null,"202610130485");
        check(next!=sentinel&&"STORE_NULL".equals(next.readError),"NEW_TARGET_PERFORMS_NEW_READ_INSTEAD_OF_USING_PREVIOUS_RESULT");
    }
    static void renderRace()throws Exception {
        MainActivity a=activity("202610130484");
        ((java.util.concurrent.atomic.AtomicLong)get(a,MainActivity.class,"uiRevision")).set(1L);
        String session=TimeSourceR811.clockSessionCP12();
        CountdownProjectionR971.Anchor next=anchor("202610130485",5,55000L,session);
        owner(anchor("202610130484",4,54000L,session));
        android.os.SystemClock.beforeElapsedRead=()->{
            for(StackTraceElement frame:Thread.currentThread().getStackTrace()) {
                if(frame.getClassName().equals("com.ethk3.novaonepro.MainActivity")&&frame.getMethodName().equals("getBestCountdownFromState")) {
                    android.os.SystemClock.beforeElapsedRead=null;
                    try{owner(next);}catch(Exception e){throw new IllegalStateException(e);}return;
                }
            }
        };
        try{call(a,"renderLightSnapshot",new Class[]{lightType},light("202610130484",null));}
        finally{android.os.SystemClock.beforeElapsedRead=null;set(null,CountdownClock.class,"liveOwnerR72",null);}
        check("202610130485".equals(get(a,MainActivity.class,"uiLockedTargetIssue")),"OWNER_PUBLICATION_BETWEEN_RENDER_READS_ADVANCES_TARGET");
        check(((String)get(a,MainActivity.class,"lastDecisionRenderKey")).startsWith("CLEAR|202610130485|"),
                "OLD_SNAPSHOT_CANNOT_REPAINT_AFTER_SECOND_CLOCK_READ_ADVANCES_HEADER");
    }
    public static void main(String[] args)throws Exception {
        if(args[0].equals("all")||args[0].equals("pending"))pending();
        if(args[0].equals("all")||args[0].equals("countdown"))countdown();
        if(args[0].equals("all"))cache();
        if(args[0].equals("all")||args[0].equals("race"))renderRace();
        String result="{\"status\":\"PASS\",\"checks\":"+checks+",\"scope\":\"ACTUAL_ACTIVITY_PRESENTATION_AND_CLOCK_SELECTION_HOST_NO_LAYOUT_NO_DEVICE\"}\n";
        Files.writeString(Path.of(args[1]),result);System.out.print(result);
    }
}
