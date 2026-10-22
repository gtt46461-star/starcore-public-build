package com.ethk3.novaonepro;
import android.os.Handler;import android.os.Looper;import java.util.concurrent.*;import java.util.concurrent.atomic.*;
/** Real CountdownClock, deterministic Android Handler adapter; not a phone scheduling test. */
public final class ClockLockHarnessR978 {
 static int n,fail;static void check(boolean b,String s){n++;if(!b)fail++;System.out.println((b?"PASS ":"FAIL ")+s);}
 public static void main(String[] args)throws Exception {
  CountdownClock c=new CountdownClock(new Looper());Handler h=Handler.latest;
  AtomicBoolean held=new AtomicBoolean(true);c.setListener((ms,next,interval,fast)->held.set(Thread.holdsLock(c)));
  c.tickOnceForTestOrManualRefresh();check(!held.get(),"MANUAL_CALLBACK_OUTSIDE_CLOCK_MONITOR");
  c.start();h.runNext();check(!held.get(),"SCHEDULED_CALLBACK_OUTSIDE_CLOCK_MONITOR");c.stop();
  CountDownLatch entered=new CountDownLatch(1),release=new CountDownLatch(1),read=new CountDownLatch(1);
  c.setListener((ms,next,interval,fast)->{entered.countDown();try{release.await(3,TimeUnit.SECONDS);}catch(InterruptedException e){Thread.currentThread().interrupt();}});
  Thread callback=new Thread(()->c.tickOnceForTestOrManualRefresh());callback.start();check(entered.await(1,TimeUnit.SECONDS),"SLOW_LISTENER_ENTERED");
  Thread getter=new Thread(()->{c.getCountdownMs();read.countDown();});getter.start();
  check(read.await(200,TimeUnit.MILLISECONDS),"SLOW_LISTENER_DOES_NOT_BLOCK_CLOCK_READ");release.countDown();callback.join(1000);getter.join(1000);
  check(!callback.isAlive()&&!getter.isAlive(),"TEST_THREADS_JOINED");
  System.out.println("R978_CLOCK_LOCK checks="+n+" failures="+fail+" ANDROID_HANDLER_ADAPTER_NOT_DEVICE");if(fail>0)throw new AssertionError("failures="+fail);
 }
}
