package com.ethk3.novaonepro.strategyv2;

import java.util.concurrent.*;
import java.util.function.*;

/** A private compute continuation, parked outside DB critical sections between service quanta.
 * Durable numerical progress remains the repository's checkpoint, never this thread's existence. */
public final class CooperativeContinuationR26<T> implements AutoCloseable,BudgetTokenSU1.PauseClock {
    private static final ThreadLocal<CooperativeContinuationR26<?>> CURRENT=new ThreadLocal<>();
    private final Object monitor=new Object();private final Callable<T> body;private final BooleanSupplier preempt;private final Runnable resume;private final LongSupplier cpuClock;
    private final Thread thread;private volatile boolean closed,done,paused=true;private volatile long endNanos,pausedSince,totalPaused,cpuTotal,cpuOrigin,maxPollGap,lastPoll;private int criticalDepth;private volatile Throwable error;private volatile T result;
    public static final class Slice<T>{public final boolean done;public final T result;public final Throwable error;public final long cpuNanos,maxPollGapNanos;Slice(boolean done,T result,Throwable error,long cpu,long gap){this.done=done;this.result=result;this.error=error;cpuNanos=cpu;maxPollGapNanos=gap;}}
    public CooperativeContinuationR26(String name,Callable<T> body,BooleanSupplier preempt,Runnable resume,LongSupplier cpuClock){this.body=body;this.preempt=preempt;this.resume=resume;this.cpuClock=cpuClock;thread=new Thread(this::run,name);thread.setDaemon(true);pausedSince=System.nanoTime();thread.start();}
    public Slice<T> slice(long millis)throws InterruptedException{
        if(millis<1||millis>200)throw new IllegalArgumentException("R26_CONTINUATION_QUANTUM");
        synchronized(monitor){long cpuBefore=cpuTotal;if(!done){endNanos=System.nanoTime()+millis*1000000L;paused=false;monitor.notifyAll();long hard=System.nanoTime()+(millis+1000)*1000000L;
            while(!done&&!paused){long remaining=hard-System.nanoTime();if(remaining<=0){close();throw new IllegalStateException("R26_UNSLICED_COMPUTATION_CAPACITY");}TimeUnit.NANOSECONDS.timedWait(monitor,remaining);}}
            return new Slice<>(done,result,error,Math.max(0,cpuTotal-cpuBefore),maxPollGap);
        }
    }
    private void run(){
        CURRENT.set(this);BudgetTokenSU1.setPauseClock(this);cpuOrigin=cpuClock.getAsLong();
        try{awaitPermit();ExecutionBudgetR26.enter(new BudgetTokenSU1(3600000,Long.MAX_VALUE/4,()->closed));lastPoll=System.nanoTime();result=body.call();}
        catch(Throwable failure){error=failure;}
        finally{ExecutionBudgetR26.leave();BudgetTokenSU1.setPauseClock(null);CURRENT.remove();synchronized(monitor){cpuTotal=Math.max(0,cpuClock.getAsLong()-cpuOrigin);done=true;paused=true;monitor.notifyAll();}}
    }
    private void awaitPermit()throws InterruptedException{
        synchronized(monitor){while((paused||endNanos==0)&&!closed)monitor.wait();if(closed)throw new InterruptedException("R26_CONTINUATION_CANCELLED");long now=System.nanoTime();if(pausedSince!=0){totalPaused+=now-pausedSince;pausedSince=0;}if(resume!=null)resume.run();lastPoll=now;}
    }
    private void checkpoint(){
        if(closed||Thread.currentThread().isInterrupted())throw new BudgetTokenSU1.Yield("R26_CONTINUATION_CANCELLED");long now=System.nanoTime();maxPollGap=Math.max(maxPollGap,now-lastPoll);lastPoll=now;
        if(criticalDepth>0)return;
        if(now<endNanos&&(preempt==null||!preempt.getAsBoolean()))return;
        long cpu=cpuClock.getAsLong();
        synchronized(monitor){paused=true;pausedSince=System.nanoTime();cpuTotal=Math.max(0,cpu-cpuOrigin);monitor.notifyAll();}
        try{awaitPermit();}catch(InterruptedException stopped){Thread.currentThread().interrupt();throw new BudgetTokenSU1.Yield("R26_CONTINUATION_CANCELLED");}
    }
    public static void poll(){CooperativeContinuationR26<?> task=CURRENT.get();if(task!=null)task.checkpoint();}
    public static boolean installed(){return CURRENT.get()!=null;}
    /** Acquired after the writer lock: no waiting/yield is allowed while a DB transaction or lock is held. */
    public static final class Critical implements AutoCloseable {private final CooperativeContinuationR26<?> task;private Critical(CooperativeContinuationR26<?> task){this.task=task;if(task!=null)task.criticalDepth++;}public void close(){if(task!=null)task.criticalDepth--;}}
    public static Critical critical(){return new Critical(CURRENT.get());}
    @Override public long pausedNanos(){return totalPaused+(pausedSince==0?0:System.nanoTime()-pausedSince);}
    public boolean isDone(){return done;}
    @Override public void close(){closed=true;synchronized(monitor){monitor.notifyAll();}thread.interrupt();}
}
