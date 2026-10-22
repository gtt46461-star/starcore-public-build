package com.ethk3.novaonepro;
import java.lang.reflect.*;import java.util.*;import java.util.concurrent.atomic.AtomicLong;
/** Executes the actual Activity methods; skips Android lifecycle only. */
public final class UiContinuityR996 {
 static int checks,failures;static void check(boolean b,String why){checks++;if(!b)failures++;System.out.println((b?"PASS ":"FAIL ")+why);}
 static final sun.misc.Unsafe U;
 static{try{Field f=sun.misc.Unsafe.class.getDeclaredField("theUnsafe");f.setAccessible(true);U=(sun.misc.Unsafe)f.get(null);}catch(Exception e){throw new ExceptionInInitializerError(e);}}
 static void set(Object o,String key,Object v)throws Exception{Field f=o.getClass().getDeclaredField(key);f.setAccessible(true);f.set(o,v);}
 static Object get(Object o,String key)throws Exception{Field f=o.getClass().getDeclaredField(key);f.setAccessible(true);return f.get(o);}
 static Object call(Object o,String method,Class<?>[] types,Object... args)throws Exception{Method m=o.getClass().getDeclaredMethod(method,types);m.setAccessible(true);return m.invoke(o,args);}
 static Object snapshot(String target,long revision)throws Exception{Class<?> c=Class.forName("com.ethk3.novaonepro.MainActivity$LightSnapshot");Constructor<?> x=c.getDeclaredConstructor();x.setAccessible(true);Object s=x.newInstance();set(s,"targetIssue",target);set(s,"currentIssue","202610050554");set(s,"uiRevision",revision);set(s,"loadedAt",System.currentTimeMillis()-500);return s;}
 public static void main(String[] args)throws Exception{
  MainActivity a=(MainActivity)U.allocateInstance(MainActivity.class);AtomicLong rev=new AtomicLong(5);set(a,"uiRevision",rev);set(a,"lightReadRunning",true);
  for(int i=0;i<100;i++)call(a,"refreshLightOnly",new Class<?>[0]);
  check(rev.get()==5,"100 invalidations preserve running read revision");check(Boolean.TRUE.equals(get(a,"lightReadDirty")),"dirty follow-up retained");
  Models.AppRuntimeState state=new Models.AppRuntimeState();state.currentIssue="202610050553";state.targetIssue="202610050554";Models.Draw draw=new Models.Draw();draw.issue="202610050554";
  Class<?>[] resolve={Models.AppRuntimeState.class,HashMap.class,Models.Draw.class};
  check("202610050554".equals(call(a,"resolveCurrentIssue",resolve,state,new HashMap<>(),draw)),"saved latest draw wins stale runtime current");
  check("202610050555".equals(call(a,"resolveTargetIssue",resolve,state,new HashMap<>(),draw)),"next target derived from same saved latest draw");
  set(a,"uiLockedTargetIssue","202610050555");set(a,"uiLockedCurrentIssue","202610050554");set(a,"lastAuthoritativeUiSignalAt",System.currentTimeMillis());
  Object s=snapshot("202610050555",rev.get());set(s,"error","TEST_COMPLETED_READ_MARKER");call(a,"renderLightSnapshot",new Class<?>[]{s.getClass()},s);
  check(((Long)get(a,"lastLightRefreshAt"))>0,"same-target completed read survives newer heartbeat");
  set(a,"lastLightRefreshAt",0L);Object old=snapshot("202610050554",rev.get());set(old,"error","TEST_OLD_TARGET");call(a,"renderLightSnapshot",new Class<?>[]{old.getClass()},old);
  check(((Long)get(a,"lastLightRefreshAt"))==0,"old target remains rejected");
  call(a,"handleTargetIssueChanged",new Class<?>[]{String.class,String.class,String.class},"202610050553","202610050554","TEST_OLD_BROADCAST");
  check("202610050554".equals(get(a,"uiLockedCurrentIssue")),"old broadcast cannot regress saved current");
  HashMap<String,String> states=(HashMap<String,String>)get(s,"states");states.put("analysis_status","ERROR");states.put("r365_last_analysis_failure_target","202610050555");states.put("r365_last_analysis_failure","EXPLICIT_TEST_FAILURE_996");states.put("r365_last_analysis_transition","RETRY_WAIT");
  String text=(String)call(a,"getNoDecisionText",new Class<?>[]{s.getClass()},s);check(text.contains("EXPLICIT_TEST_FAILURE_996")&&text.contains("RETRY_WAIT"),"exact current-target failure visible");
  states.put("r365_last_analysis_failure_target","202610050553");text=(String)call(a,"getNoDecisionText",new Class<?>[]{s.getClass()},s);check(!text.contains("EXPLICIT_TEST_FAILURE_996"),"prior-target failure not attributed to current target");
  states.put("analysis_status","WAITING");states.put("analysis_execution_target","202610050554");states.put("analysis_execution_stage","NUMBER_MODEL");text=(String)call(a,"getNoDecisionText",new Class<?>[]{s.getClass()},s);check(text.contains("NUMBER_MODEL")&&text.contains("202610050554"),"actual old owner stage remains visible while next waits");
  System.out.println("UI_CONTINUITY_R996 checks="+checks+" failures="+failures+" scope=ACTUAL_ACTIVITY_METHODS_HOST_NO_DEVICE");if(failures>0)System.exit(1);
 }
}
