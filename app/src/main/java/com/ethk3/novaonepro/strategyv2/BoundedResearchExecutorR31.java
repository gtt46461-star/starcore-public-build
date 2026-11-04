package com.ethk3.novaonepro.strategyv2;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/** Bounded per-PID execution. Rejection is a durable scheduler WAIT, never CallerRuns or silent drop. */
public final class BoundedResearchExecutorR31 extends AbstractExecutorService implements AutoCloseable {
    public static final class QueueCapacityWait extends RejectedExecutionException {
        public final String reason;QueueCapacityWait(String reason){super("QUEUE_CAPACITY_WAIT:"+reason);this.reason=reason;}
    }
    private static final AtomicLong IDS=new AtomicLong();
    private static final ConcurrentMap<String,OwnedTask<?>> OWNERS=new ConcurrentHashMap<>();
    private final long id=IDS.incrementAndGet();private final AtomicLong sequence=new AtomicLong();
    private final Object admission=new Object();private final ThreadPoolExecutor pool;private final MemoryBudgetR31 memory;
    private final int workers,queueCapacity;private final long maxQueuedBytes;
    private long queuedBytes;private int queuedCount;private final AtomicInteger active=new AtomicInteger();
    public BoundedResearchExecutorR31(int workers,long maxQueuedBytes,MemoryBudgetR31 memory,ThreadFactory threadFactory){
        if(workers<1||workers>32||maxQueuedBytes<1)throw new IllegalArgumentException("R31_EXECUTOR_CAPACITY");this.workers=workers;this.queueCapacity=2*workers;this.maxQueuedBytes=maxQueuedBytes;this.memory=Objects.requireNonNull(memory);
        pool=new ThreadPoolExecutor(workers,workers,30,TimeUnit.SECONDS,new ArrayBlockingQueue<Runnable>(queueCapacity),Objects.requireNonNull(threadFactory),new ThreadPoolExecutor.AbortPolicy());
    }
    public BoundedResearchExecutorR31(int workers,long maxQueuedBytes,MemoryBudgetR31 memory){this(workers,maxQueuedBytes,memory,r->{Thread thread=new Thread(r,"starcore-r31-bounded-research");thread.setDaemon(true);return thread;});}
    private static final class OwnedTask<T> extends FutureTask<T> {
        final BoundedResearchExecutorR31 executor;final String owner;final long payloadBytes,workingBytes;final MemoryBudgetR31.Lease payload,working;
        final AtomicInteger state=new AtomicInteger();final AtomicBoolean released=new AtomicBoolean();boolean counted;
        OwnedTask(BoundedResearchExecutorR31 executor,String owner,long payloadBytes,long workingBytes,Callable<T> body,MemoryBudgetR31.Lease payload,MemoryBudgetR31.Lease working){super(body);this.executor=executor;this.owner=owner;this.payloadBytes=payloadBytes;this.workingBytes=workingBytes;this.payload=payload;this.working=working;}
        @Override public void run(){
            if(!state.compareAndSet(0,1))return;executor.dequeue(this);executor.active.incrementAndGet();
            try{ExecutionBudgetR26.enter(new BudgetTokenSU1(3600000,Long.MAX_VALUE/4,()->isCancelled()||Thread.currentThread().isInterrupted()));super.run();}
            finally{ExecutionBudgetR26.leave();executor.active.decrementAndGet();state.set(2);release();}
        }
        @Override public boolean cancel(boolean interrupt){boolean cancelled=super.cancel(interrupt);if(cancelled&&state.compareAndSet(0,2)){executor.pool.remove(this);executor.dequeue(this);release();}return cancelled;}
        void release(){if(released.compareAndSet(false,true)){if(working!=null)working.close();payload.close();OWNERS.remove(owner,this);}}
    }
    private <T> OwnedTask<T> prepare(String owner,long payloadBytes,long workingBytes,Callable<T> body){
        if(owner==null||owner.isEmpty()||owner.length()>192||payloadBytes<1||workingBytes<0)throw new IllegalArgumentException("R31_EXECUTOR_OWNER_BYTES");Objects.requireNonNull(body);
        synchronized(admission){
            if(pool.isShutdown())throw new QueueCapacityWait("SHUTDOWN");
            if(queuedCount>=queueCapacity+workers||payloadBytes>maxQueuedBytes-queuedBytes)throw new QueueCapacityWait("QUEUED_BYTES_OR_COUNT");
            if(OWNERS.containsKey(owner))throw new QueueCapacityWait("OWNER_DRAINING");
            MemoryBudgetR31.Admission p=memory.tryAcquire(owner,MemoryBudgetR31.Kind.QUEUED,payloadBytes,()->false);if(!p.acquired())throw new QueueCapacityWait("MEMORY_"+p.reason);
            MemoryBudgetR31.Lease work=null;
            if(workingBytes>0){MemoryBudgetR31.Admission w;try{w=memory.tryAcquire(owner,MemoryBudgetR31.Kind.RESEARCH,workingBytes,()->false);}catch(RuntimeException|Error failed){p.lease.close();throw failed;}if(!w.acquired()){p.lease.close();throw new QueueCapacityWait("MEMORY_"+w.reason);}work=w.lease;}
            OwnedTask<T> task;try{task=new OwnedTask<>(this,owner,payloadBytes,workingBytes,body,p.lease,work);}catch(RuntimeException|Error failure){if(work!=null)work.close();p.lease.close();throw failure;}
            if(OWNERS.putIfAbsent(owner,task)!=null){task.release();throw new QueueCapacityWait("OWNER_DRAINING");}
            task.counted=true;queuedCount++;queuedBytes+=payloadBytes;return task;
        }
    }
    private void dequeue(OwnedTask<?> task){synchronized(admission){if(task.counted){task.counted=false;queuedCount--;queuedBytes-=task.payloadBytes;}}}
    private void schedule(OwnedTask<?> task){try{pool.execute(task);}catch(RuntimeException|Error rejected){task.cancel(true);if(rejected instanceof Error)throw(Error)rejected;if(rejected instanceof QueueCapacityWait)throw(RuntimeException)rejected;if(rejected instanceof RejectedExecutionException){QueueCapacityWait wait=new QueueCapacityWait("EXECUTOR_QUEUE_FULL_OR_CLOSED");wait.initCause(rejected);throw wait;}throw(RuntimeException)rejected;}}
    public <T> Future<T> submit(String owner,long queuedBytes,long workingBytes,Callable<T> body){OwnedTask<T> task=prepare(owner,queuedBytes,workingBytes,body);schedule(task);return task;}
    @Override protected <T> RunnableFuture<T> newTaskFor(Callable<T> callable){return prepare("unscoped:"+id+":"+sequence.incrementAndGet(),512,1,callable);}
    @Override protected <T> RunnableFuture<T> newTaskFor(Runnable runnable,T result){return newTaskFor(Executors.callable(runnable,result));}
    @Override public void execute(Runnable command){if(command instanceof OwnedTask&&((OwnedTask<?>)command).executor==this)schedule((OwnedTask<?>)command);else{OwnedTask<Void> task=prepare("unscoped:"+id+":"+sequence.incrementAndGet(),512,1,Executors.callable(command,null));schedule(task);}}
    public boolean cancelQueued(String owner){OwnedTask<?> task=OWNERS.get(owner);return task!=null&&task.executor==this&&task.state.get()==0&&task.cancel(false);}
    public boolean ownerTerminated(String owner){return !OWNERS.containsKey(owner);}
    public Map<String,Object> snapshot(){Map<String,Object> out=new LinkedHashMap<>();synchronized(admission){out.put("queuedBytes",queuedBytes);out.put("registeredQueuedCount",queuedCount);}long declaredActive=0;for(OwnedTask<?> task:OWNERS.values())if(task.executor==this&&task.state.get()==1)declaredActive+=task.payloadBytes+task.workingBytes;out.put("workers",workers);out.put("queueCapacity",queueCapacity);out.put("queueSize",pool.getQueue().size());out.put("maxQueuedBytes",maxQueuedBytes);out.put("activeCount",active.get());out.put("activeDeclaredBytes",declaredActive);out.put("payloadLeaseLifecycle","QUEUED_DECLARATION_RETAINED_THROUGH_RUNNING_FINALLY_TO_OWN_CLOSURE");out.put("unscopedSubmissionEstimate","512_DESCRIPTOR_BYTES_PLUS_1_WORKING_BYTE_NOT_MEASURED_PAYLOAD; EXPLICIT_OWNER_ESTIMATES_REQUIRED_FOR_LARGE_WORK");out.put("scope","PER_PID");out.put("shutdown",pool.isShutdown());out.put("terminated",pool.isTerminated());return out;}
    @Override public void shutdown(){pool.shutdown();}
    @Override public List<Runnable> shutdownNow(){List<Runnable> pending=pool.shutdownNow();for(Runnable item:pending)if(item instanceof OwnedTask)((OwnedTask<?>)item).cancel(false);for(OwnedTask<?> task:OWNERS.values())if(task.executor==this)task.cancel(true);return pending;}
    @Override public boolean isShutdown(){return pool.isShutdown();}
    @Override public boolean isTerminated(){return pool.isTerminated();}
    @Override public boolean awaitTermination(long timeout,TimeUnit unit)throws InterruptedException{return pool.awaitTermination(timeout,unit);}
    @Override public void close(){shutdownNow();}
}
