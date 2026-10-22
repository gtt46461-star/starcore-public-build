package com.ethk3.novaonepro;
import android.os.Handler;
import android.os.Looper;
public final class CountdownLoopRegressionR973 {
 static int n;static void ok(boolean v,String s){if(!v)throw new AssertionError(s);n++;System.out.println("PASS "+s);}
 public static void main(String[] args){
  Looper lane=new Looper();CountdownClock clock=new CountdownClock(lane);Handler h=Handler.latest;
  ok(h.owner==lane,"CALLER_SUPPLIED_CLOCK_LANE_NOT_MAIN_LOOPER");
  final int[] calls={0};clock.setListener((ms,next,fetch,fast)->{calls[0]++;if(calls[0]==1)throw new IllegalStateException("INJECT_LISTENER_FAILURE");});
  clock.start();clock.start();ok(h.pending()==1,"DUPLICATE_START_NOT_DUPLICATE_TICK");
  h.runNext();ok(calls[0]==1&&h.pending()==1,"LISTENER_EXCEPTION_REARMS_CLOCK");
  h.runNext();ok(calls[0]==2&&h.pending()==1,"NEXT_TICK_SURVIVES_PRIOR_FAILURE");
  clock.stop();ok(h.pending()==0&&!clock.isRunning(),"STOP_CLEARS_PENDING_CALLBACK");
  clock.setListener((ms,next,fetch,fast)->clock.stop());clock.start();h.runNext();
  ok(h.pending()==0&&!clock.isRunning(),"STOP_IN_CALLBACK_NOT_REARMED");
  try{new CountdownClock(null);throw new AssertionError("NULL_LOOPER_ACCEPTED");}catch(IllegalArgumentException expected){ok(true,"NULL_CLOCK_LOOPER_REJECTED");}
  System.out.println("R973_CLOCK_LOOP_PASS checks="+n+"; HANDLER_QUEUE_TEST_DOUBLE; PHONE_SCHEDULING_NOT_RUN");
 }
}
