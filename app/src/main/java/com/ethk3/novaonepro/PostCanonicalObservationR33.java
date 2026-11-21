package com.ethk3.novaonepro;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/** Optional post-commit observations. Never waits for admission and never reruns a business body. */
public final class PostCanonicalObservationR33 {
    private PostCanonicalObservationR33() {}
    private static final AtomicLong offered=new AtomicLong(),completed=new AtomicLong(),rejected=new AtomicLong(),failed=new AtomicLong();
    private static volatile String lastError="";
    private static final ThreadPoolExecutor worker=new ThreadPoolExecutor(1,1,30,TimeUnit.SECONDS,
        new ArrayBlockingQueue<Runnable>(8),r->{Thread t=new Thread(r,"r33-post-canonical-observation");t.setDaemon(true);return t;},new ThreadPoolExecutor.AbortPolicy());
    static {worker.allowCoreThreadTimeOut(true);}
    public static boolean offer(final Runnable observation) {
        if(observation==null)throw new IllegalArgumentException("R33_OBSERVATION_REQUIRED");
        offered.incrementAndGet();
        try{worker.execute(()->{try{observation.run();completed.incrementAndGet();}
            catch(VirtualMachineError|ThreadDeath|LinkageError fatal){throw fatal;}
            catch(Throwable error){failed.incrementAndGet();lastError=error.getClass().getName()+":"+String.valueOf(error.getMessage());}});return true;}
        catch(java.util.concurrent.RejectedExecutionException full){rejected.incrementAndGet();lastError="R33_OBSERVATION_QUEUE_FULL";return false;}
    }
    public static Map<String,Object> snapshot(){Map<String,Object> m=new LinkedHashMap<>();m.put("offered",offered.get());m.put("completed",completed.get());m.put("rejected",rejected.get());m.put("failed",failed.get());m.put("queueDepth",worker.getQueue().size());m.put("lastError",lastError);m.put("evidenceScope","PROCESS_LOCAL_OPTIONAL_OBSERVATIONS_NOT_DURABLE_BUSINESS_OR_LATENCY_SUCCESS");return m;}
}
