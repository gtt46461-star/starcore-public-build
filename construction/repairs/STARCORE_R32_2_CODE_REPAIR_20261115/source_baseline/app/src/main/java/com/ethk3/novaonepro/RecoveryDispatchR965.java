package com.ethk3.novaonepro;

import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/** Receiver ingress is a bounded CAS mailbox, never an executor queue operation. */
final class RecoveryDispatchR965 {
    private static final int CAPACITY=64;
    private static final AtomicReferenceArray<Entry> MAILBOX=new AtomicReferenceArray<>(CAPACITY);
    private static final AtomicInteger TICKET=new AtomicInteger();
    private static final AtomicBoolean STARTED=new AtomicBoolean();
    private RecoveryDispatchR965(){}
    private static final class Entry {
        final Runnable work,finish; final long deadline;
        final AtomicBoolean claimed=new AtomicBoolean(),done=new AtomicBoolean();
        Entry(Runnable w,Runnable f,long ms){work=w;finish=f;deadline=android.os.SystemClock.elapsedRealtime()+Math.max(1L,Math.min(8000L,ms));}
        void finishOnce(){if(done.compareAndSet(false,true))try{finish.run();}catch(RuntimeException e){android.util.Log.e("CP4Recovery","FINISH_FAILED",e);}}
    }
    /** Called first on the existing StartupHealth background thread, before artifact hashing. */
    static void warmUpOffMain(){
        if(android.os.Looper.myLooper()==android.os.Looper.getMainLooper())
            throw new IllegalStateException("CP4_RECOVERY_WARMUP_ON_MAIN");
        if(!STARTED.compareAndSet(false,true))return;
        final ThreadPoolExecutor work=new ThreadPoolExecutor(1,1,30,TimeUnit.SECONDS,
                new ArrayBlockingQueue<Runnable>(1), r->{Thread t=new Thread(r,"CP4-recovery-work");t.setDaemon(true);return t;},
                new ThreadPoolExecutor.AbortPolicy());
        work.allowCoreThreadTimeOut(true);
        Thread pump=new Thread(()->{
            while(true){
                long now=android.os.SystemClock.elapsedRealtime();
                for(int i=0;i<CAPACITY;i++){
                    final Entry e=MAILBOX.get(i);if(e==null)continue;
                    if(now>=e.deadline)e.finishOnce();
                    if(e.done.get()){MAILBOX.compareAndSet(i,e,null);continue;}
                    if(e.claimed.compareAndSet(false,true))try{
                        work.execute(()->{
                            // A queued request can expire while the mailbox pump is completing
                            // another callback. Recheck on the worker before any recovery IO.
                            if(e.done.get()||android.os.SystemClock.elapsedRealtime()>=e.deadline){
                                e.finishOnce();return;
                            }
                            try{e.work.run();}
                            catch(RuntimeException failure){android.util.Log.e("CP4Recovery","WORK_FAILED",failure);}
                            finally{e.finishOnce();}});
                    }catch(RejectedExecutionException full){e.finishOnce();}
                }
                try{Thread.sleep(10L);}catch(InterruptedException stopped){Thread.currentThread().interrupt();return;}
            }
        },"CP4-recovery-mailbox");
        pump.setDaemon(true);pump.start();
    }
    static void submit(Runnable work,Runnable finish){submit(work,finish,8000L);}
    static void submit(Runnable work,Runnable finish,long limitMs){
        if(work==null||finish==null)throw new IllegalArgumentException("CP4_RECOVERY_CALLBACK_NULL");
        Entry e=new Entry(work,finish,limitMs);int start=TICKET.getAndIncrement()&63;
        for(int step=0;step<CAPACITY;step++)if(MAILBOX.compareAndSet((start+step)&63,null,e))return;
        // Framework completion is mandatory on overload; no DB, scheduler, new thread, or work inline.
        e.finishOnce();
        android.util.Log.w("CP4Recovery","MAILBOX_FULL_EXISTING_KICK_RETAINED");
    }
}
