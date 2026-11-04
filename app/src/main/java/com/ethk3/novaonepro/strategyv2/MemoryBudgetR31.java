package com.ethk3.novaonepro.strategyv2;

import java.io.*;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.BooleanSupplier;

/** Per-PID declared working-set admission. Reservations are not measured Java/native/PSS bytes. */
public final class MemoryBudgetR31 {
    public static final double HIGH_FRACTION=.75, RESUME_FRACTION=.60, EMERGENCY_FRACTION=.90, REALTIME_FRACTION=.15;
    public enum Kind {
        REALTIME, CHECKPOINT, RESEARCH, MODEL, FEATURE_CACHE, QUEUED, UI,
        NATIVE_DECODER, NATIVE_BACKUP, NATIVE_BUFFER, NATIVE_SQLITE, NATIVE_BITMAP;
        public boolean nativeMemory(){return name().startsWith("NATIVE_");}
    }
    public enum WaitReason { NONE, CANCELLED, HEAP_PRESSURE, EMERGENCY_PRESSURE, DECLARED_CAPACITY, UNKNOWN_CAPACITY, NATIVE_DECLARED_CAPACITY, OWNER_LIMIT, CLOSED_LEASE }
    public interface Probe { Measurement measure(); }
    public static final class CapacityWait extends java.util.concurrent.RejectedExecutionException {
        public final WaitReason reason;public CapacityWait(WaitReason reason){super("RESOURCE_WAIT:MEMORY_"+reason);this.reason=reason;}
    }
    public static final class Measurement {
        public final long pid,maxHeapBytes,heapUsedBytes,nativeAllocatedBytes,pssBytes,rssBytes,observedAtNanos,nativeObservedAtNanos;
        public final String processName; public final Boolean osLowMemory;
        public Measurement(long pid,String processName,long maxHeapBytes,long heapUsedBytes,long nativeAllocatedBytes,long pssBytes,long rssBytes,Boolean osLowMemory,long observedAtNanos,long nativeObservedAtNanos){
            if(maxHeapBytes<=0||heapUsedBytes<0||nativeAllocatedBytes< -1||pssBytes< -1||rssBytes< -1)throw new IllegalArgumentException("R31_MEMORY_MEASUREMENT");
            this.pid=pid;this.processName=processName;this.maxHeapBytes=maxHeapBytes;this.heapUsedBytes=heapUsedBytes;this.nativeAllocatedBytes=nativeAllocatedBytes;this.pssBytes=pssBytes;this.rssBytes=rssBytes;this.osLowMemory=osLowMemory;this.observedAtNanos=observedAtNanos;this.nativeObservedAtNanos=nativeObservedAtNanos;
        }
    }
    public static final class Config {
        public final long realtimePeakBytes,uiPeakBytes;public final Map<Kind,Long> nativeCapacities;
        public Config(){this(0,0,Collections.emptyMap());}
        public Config(long realtimePeakBytes,long uiPeakBytes,Map<Kind,Long> nativeCapacities){
            if(realtimePeakBytes<0||uiPeakBytes<0)throw new IllegalArgumentException("R31_MEMORY_HEADROOM");
            this.realtimePeakBytes=realtimePeakBytes;this.uiPeakBytes=uiPeakBytes;
            EnumMap<Kind,Long> copy=new EnumMap<>(Kind.class);
            for(Map.Entry<Kind,Long> e:nativeCapacities.entrySet()){if(!e.getKey().nativeMemory()||e.getValue()<0)throw new IllegalArgumentException("R31_NATIVE_CAPACITY");copy.put(e.getKey(),e.getValue());}
            this.nativeCapacities=Collections.unmodifiableMap(copy);
        }
    }
    private static final class ProcessHolder {static final MemoryBudgetR31 INSTANCE=new MemoryBudgetR31(new RuntimeProbe(),new Config());}
    public static MemoryBudgetR31 processBudget(){return ProcessHolder.INSTANCE;}
    public static final class Admission {
        public final Lease lease;public final WaitReason reason;
        private Admission(Lease lease,WaitReason reason){this.lease=lease;this.reason=reason;}
        public boolean acquired(){return lease!=null&&reason==WaitReason.NONE;}
    }
    public static final class Lease implements AutoCloseable {
        private final MemoryBudgetR31 budget;public final String owner;public final long fence;public final Kind kind;
        private final BooleanSupplier cancellation;private long bytes;private boolean closed;
        private Lease(MemoryBudgetR31 budget,String owner,long fence,Kind kind,long bytes,BooleanSupplier cancellation){this.budget=budget;this.owner=owner;this.fence=fence;this.kind=kind;this.bytes=bytes;this.cancellation=cancellation;}
        public long bytes(){synchronized(budget){return bytes;}}
        public Admission grow(long delta){return budget.grow(this,delta);}
        public boolean isClosed(){synchronized(budget){return closed;}}
        @Override public void close(){budget.release(this);}
    }
    private final Probe probe;private final Config config;
    private final Set<Lease> leases=new LinkedHashSet<>();
    private final EnumMap<Kind,Long> nativeCaps=new EnumMap<>(Kind.class),declaredByKind=new EnumMap<>(Kind.class);
    private final EnumMap<WaitReason,Long> waits=new EnumMap<>(WaitReason.class);
    private final Map<Kind,String> nativeBasis=new EnumMap<>(Kind.class);
    private final Set<Runnable> reclaimers=Collections.newSetFromMap(new IdentityHashMap<Runnable,Boolean>());
    private Boolean systemLowMemory;private long systemLowMemoryObservedAt;
    private Measurement observed;private boolean softPressure,emergency,baselineCapacityUnmet;private String pressureReason="UNOBSERVED",sampleFailure="";
    public MemoryBudgetR31(Probe probe,Config config){this.probe=Objects.requireNonNull(probe);this.config=Objects.requireNonNull(config);nativeCaps.putAll(config.nativeCapacities);for(Kind k:nativeCaps.keySet())nativeBasis.put(k,"CALLER_REGISTERED_NOT_DEVICE_CALIBRATION");update(probe.measure(),true);}
    public synchronized void registerReclaimer(Runnable reclaimer){if(reclaimers.size()>=8&&!reclaimers.contains(reclaimer))throw new IllegalStateException("R31_RECLAIMER_BOUND");reclaimers.add(Objects.requireNonNull(reclaimer));}
    public synchronized void observeSystemLowMemory(Boolean observed){systemLowMemory=observed;systemLowMemoryObservedAt=System.nanoTime();String priorFailure=sampleFailure;update(this.observed,false);sampleFailure=priorFailure;}
    private Boolean effectiveLowMemory(){return systemLowMemory!=null?systemLowMemory:observed.osLowMemory;}
    public synchronized void registerNativeCapacity(Kind kind,long bytes,String basis){
        if(!kind.nativeMemory()||bytes<0||basis==null||basis.isEmpty())throw new IllegalArgumentException("R31_NATIVE_CAPACITY");
        Long prior=nativeCaps.get(kind);if(prior!=null&&prior!=bytes)throw new IllegalStateException("R31_NATIVE_CAPACITY_CONFIG_LOCKED");
        nativeCaps.put(kind,bytes);nativeBasis.put(kind,bounded(basis,160));
    }
    public Admission tryAcquire(String owner,Kind kind,long estimatedBytes,BooleanSupplier cancellation){return tryAcquire(owner,-1,kind,estimatedBytes,cancellation);}
    public Admission tryAcquire(String owner,long fence,Kind kind,long estimatedBytes,BooleanSupplier cancellation){
        if(owner==null||owner.isEmpty()||owner.length()>192||estimatedBytes<1)throw new IllegalArgumentException("R31_MEMORY_OWNER_BYTES");Objects.requireNonNull(kind);
        Measurement current=readProbe();synchronized(this){if(current==null)return denied(WaitReason.UNKNOWN_CAPACITY);update(current,false);WaitReason reason=admissible(kind,estimatedBytes,cancellation);if(reason!=WaitReason.NONE)return denied(reason);if(leases.size()>=1024)return denied(WaitReason.OWNER_LIMIT);
            Lease lease=new Lease(this,owner,fence,kind,estimatedBytes,cancellation);leases.add(lease);declaredByKind.put(kind,safeAdd(declaredByKind.getOrDefault(kind,0L),estimatedBytes));return new Admission(lease,WaitReason.NONE);}
    }
    private Admission grow(Lease lease,long delta){
        if(delta<0)throw new IllegalArgumentException("R31_MEMORY_GROW_NEGATIVE");Measurement current=readProbe();
        synchronized(this){if(current==null)return denied(WaitReason.UNKNOWN_CAPACITY);update(current,false);if(lease.closed)return denied(WaitReason.CLOSED_LEASE);WaitReason reason=admissible(lease.kind,delta,lease.cancellation);if(reason!=WaitReason.NONE)return denied(reason);
            lease.bytes=safeAdd(lease.bytes,delta);declaredByKind.put(lease.kind,safeAdd(declaredByKind.getOrDefault(lease.kind,0L),delta));return new Admission(lease,WaitReason.NONE);}
    }
    private WaitReason admissible(Kind kind,long bytes,BooleanSupplier cancellation){
        if(cancellation!=null&&cancellation.getAsBoolean())return WaitReason.CANCELLED;
        if(kind.nativeMemory()){
            Long cap=nativeCaps.get(kind);if(cap==null)return WaitReason.UNKNOWN_CAPACITY;
            if(bytes>cap-declaredByKind.getOrDefault(kind,0L))return WaitReason.NATIVE_DECLARED_CAPACITY;
            if(emergency)return WaitReason.EMERGENCY_PRESSURE;
            return WaitReason.NONE;
        }
        boolean critical=kind==Kind.REALTIME||kind==Kind.CHECKPOINT;
        if(!critical&&emergency)return WaitReason.EMERGENCY_PRESSURE;
        if(!critical&&softPressure)return WaitReason.HEAP_PRESSURE;
        long high=(long)(observed.maxHeapBytes*(critical?EMERGENCY_FRACTION:HIGH_FRACTION));
        long reserved=critical?0:Math.max((long)Math.ceil(observed.maxHeapBytes*REALTIME_FRACTION),config.realtimePeakBytes);
        long declared=0;for(Map.Entry<Kind,Long> e:declaredByKind.entrySet())if(!e.getKey().nativeMemory())declared=safeAdd(declared,e.getValue());
        long available=Math.max(0,high-Math.min(high,observed.heapUsedBytes));available=Math.max(0,available-Math.min(available,reserved));if(!critical&&kind!=Kind.UI)available=Math.max(0,available-Math.min(available,config.uiPeakBytes));available=Math.max(0,available-Math.min(available,declared));
        return bytes<=available?WaitReason.NONE:WaitReason.DECLARED_CAPACITY;
    }
    private synchronized void release(Lease lease){if(lease.closed)return;lease.closed=true;leases.remove(lease);long prior=declaredByKind.getOrDefault(lease.kind,0L);if(lease.bytes>prior)throw new IllegalStateException("R31_MEMORY_ACCOUNTING");declaredByKind.put(lease.kind,prior-lease.bytes);}
    private Admission denied(WaitReason reason){waits.put(reason,waits.getOrDefault(reason,0L)+1);return new Admission(null,reason);}
    private synchronized void update(Measurement measurement,boolean initial){
        Objects.requireNonNull(measurement);if(observed!=null&&measurement.observedAtNanos<observed.observedAtNanos)return;observed=measurement;sampleFailure="";double fraction=measurement.heapUsedBytes/(double)measurement.maxHeapBytes;
        if(initial&&fraction>=HIGH_FRACTION)baselineCapacityUnmet=true;
        emergency=fraction>=EMERGENCY_FRACTION||Boolean.TRUE.equals(effectiveLowMemory());
        if(emergency||fraction>=HIGH_FRACTION)softPressure=true;
        else if(fraction<=RESUME_FRACTION&&!Boolean.TRUE.equals(effectiveLowMemory()))softPressure=false;
        pressureReason=emergency?"EMERGENCY_HEAP_OR_OS_PRESSURE":softPressure?(baselineCapacityUnmet?"BASELINE_CAPACITY_UNMET":"HEAP_HIGH_WATER_LATCHED"):"NORMAL";
    }
    private Measurement readProbe(){List<Runnable> cleanup;synchronized(this){cleanup=new ArrayList<>(reclaimers);}for(Runnable reclaimer:cleanup)reclaimer.run();try{return probe.measure();}catch(RuntimeException failure){synchronized(this){sampleFailure=failure.getClass().getName();}return null;}}
    public Map<String,Object> sample(){Measurement current=readProbe();synchronized(this){if(current!=null)update(current,false);return measurementMap();}}
    public synchronized Map<String,Object> snapshot(){
        Map<String,Object> out=measurementMap();long java=0,nativeBytes=0,queued=declaredByKind.getOrDefault(Kind.QUEUED,0L);Map<String,Object> kinds=new LinkedHashMap<>();
        for(Map.Entry<Kind,Long> e:declaredByKind.entrySet()){kinds.put(e.getKey().name(),e.getValue());if(e.getKey().nativeMemory())nativeBytes=safeAdd(nativeBytes,e.getValue());else java=safeAdd(java,e.getValue());}
        out.put("declaredJavaWorkingBytes",java);out.put("declaredNativeWorkingBytes",nativeBytes);out.put("declaredQueuedBytes",queued);out.put("declaredByKind",kinds);out.put("activeLeaseCount",leases.size());
        List<Lease> largest=new ArrayList<>(leases);largest.sort((a,b)->Long.compare(b.bytes,a.bytes));List<Object> owners=new ArrayList<>();for(int i=0;i<Math.min(32,largest.size());i++){Lease l=largest.get(i);Map<String,Object> row=new LinkedHashMap<>();row.put("owner",l.owner);row.put("fence",l.fence);row.put("kind",l.kind.name());row.put("declaredBytes",l.bytes);row.put("measurement","ESTIMATED_WORKING_SET_NOT_ACTUAL_HEAP");owners.add(row);}out.put("largestOwners",owners);out.put("ownersTruncated",largest.size()>32);
        Map<String,Object> caps=new LinkedHashMap<>();for(Kind kind:nativeCaps.keySet()){Map<String,Object> row=new LinkedHashMap<>();row.put("bytes",nativeCaps.get(kind));row.put("basis",nativeBasis.get(kind));caps.put(kind.name(),row);}out.put("nativeDeclaredCapacities",caps);out.put("nativeHardCapacityVerdict","UNMANAGED_NATIVE_OWNERS_NOT_COVERED");Map<String,Object> failures=new LinkedHashMap<>();for(Map.Entry<WaitReason,Long> e:waits.entrySet())failures.put(e.getKey().name(),e.getValue());out.put("admissionWaitCounts",failures);return out;
    }
    private Map<String,Object> measurementMap(){
        Map<String,Object> out=new LinkedHashMap<>();out.put("scope","PER_PID");out.put("pid",observed.pid);out.put("processName",observed.processName);out.put("crossProcessBudget","NOT_COORDINATED");out.put("coverage","REGISTERED_OWNERS_ONLY_NOT_ALL_APPLICATION_ALLOCATIONS");out.put("maxHeapBytes",observed.maxHeapBytes);out.put("heapUsedBytes",observed.heapUsedBytes);out.put("nativeAllocatedBytes",observed.nativeAllocatedBytes);out.put("pssBytes",observed.pssBytes);out.put("rssBytes",observed.rssBytes);out.put("osLowMemory",effectiveLowMemory());out.put("systemLowMemoryObservedAtNanos",systemLowMemoryObservedAt);out.put("observedAtNanos",observed.observedAtNanos);out.put("nativeObservedAtNanos",observed.nativeObservedAtNanos);out.put("clock","PROCESS_MONOTONIC_NS");out.put("highFraction",HIGH_FRACTION);out.put("resumeFraction",RESUME_FRACTION);out.put("emergencyFraction",EMERGENCY_FRACTION);out.put("realtimeReservedBytes",Math.max((long)Math.ceil(observed.maxHeapBytes*REALTIME_FRACTION),config.realtimePeakBytes));out.put("uiReservedBytes",config.uiPeakBytes);out.put("pressure",emergency?"EMERGENCY":softPressure?"SOFT":"NORMAL");out.put("reason",sampleFailure.isEmpty()?pressureReason:"MEMORY_MEASUREMENT_UNAVAILABLE");out.put("sampleFailure",sampleFailure);out.put("baselineCapacityUnmetObserved",baselineCapacityUnmet);out.put("resumeCondition","heapUsed/maxHeap <= 0.60 AND osLowMemory != true; owners must release actual work");out.put("allowOptionalWork",!softPressure&&sampleFailure.isEmpty());out.put("recommendedParallelism",softPressure?1:2);out.put("accounting","ADMISSION_USES_HEAP_PLUS_DECLARATIONS_CONSERVATIVELY; MAY_DOUBLE_COUNT; NEVER_SUM_HEAP_NATIVE_PSS");return out;
    }
    private static long safeAdd(long a,long b){if(b<0||a>Long.MAX_VALUE-b)throw new IllegalArgumentException("R31_MEMORY_SIZE_OVERFLOW");return a+b;}
    private static String bounded(String text,int limit){return text.substring(0,Math.min(text.length(),limit)).replace('\n',' ').replace('\r',' ');}
    /** Java heap is fresh per request; potentially unavailable native/proc observations refresh at most once/s. */
    public static final class RuntimeProbe implements Probe {
        private long sampledNativeAt,nativeBytes=-1,pss=-1,rss=-1;private final long pid=pid();private final String processName=processName();
        public synchronized Measurement measure(){
            Runtime runtime=Runtime.getRuntime();long now=System.nanoTime();if(sampledNativeAt==0||now-sampledNativeAt>=1_000_000_000L){nativeBytes=nativeAllocated();long[] proc=procMemory();pss=proc[0];rss=proc[1];sampledNativeAt=now;}
            return new Measurement(pid,processName,runtime.maxMemory(),Math.max(0,runtime.totalMemory()-runtime.freeMemory()),nativeBytes,pss,rss,null,now,sampledNativeAt);
        }
        private static long nativeAllocated(){String vm=System.getProperty("java.vm.name","");if(!vm.equals("Dalvik")&&!vm.equals("ART"))return -1;try{Class<?> debug=Class.forName("android.os.Debug");Method method=debug.getMethod("getNativeHeapAllocatedSize");return ((Number)method.invoke(null)).longValue();}catch(ReflectiveOperationException|SecurityException unavailable){rethrowProcessFatal(unavailable);return -1;}}
        private static void rethrowProcessFatal(Throwable failure){for(int depth=0;failure!=null&&depth<32;depth++,failure=failure.getCause()){if(failure instanceof VirtualMachineError)throw(VirtualMachineError)failure;if(failure instanceof ThreadDeath)throw(ThreadDeath)failure;if(failure instanceof LinkageError)throw(LinkageError)failure;}}
        private static long pid(){try{return Long.parseLong(new File("/proc/self").getCanonicalFile().getName());}catch(IOException|NumberFormatException unavailable){return -1;}}
        private static String processName(){try(InputStream in=new FileInputStream("/proc/self/cmdline")){byte[] buffer=new byte[192];int length=in.read(buffer);if(length<=0)return "UNKNOWN";int end=0;while(end<length&&buffer[end]!=0)end++;return new String(buffer,0,end,java.nio.charset.StandardCharsets.UTF_8);}catch(IOException unavailable){return "UNKNOWN";}}
        private static long[] procMemory(){long pss=-1,rss=-1;for(String path:new String[]{"/proc/self/smaps_rollup","/proc/self/status"})try(BufferedReader reader=new BufferedReader(new FileReader(path))){String line;int chars=0;while((line=reader.readLine())!=null&&(chars+=line.length())<=65536){if(line.startsWith("Pss:"))pss=kib(line);if(line.startsWith("Rss:")||rss<0&&line.startsWith("VmRSS:"))rss=kib(line);}}catch(IOException|NumberFormatException unavailable){/* unknown remains -1 */}return new long[]{pss,rss};}
        private static long kib(String line){String[] parts=line.trim().split("\\s+");long value=Long.parseLong(parts[1]);return value<0||value>Long.MAX_VALUE/1024?-1:value*1024;}
    }
}
