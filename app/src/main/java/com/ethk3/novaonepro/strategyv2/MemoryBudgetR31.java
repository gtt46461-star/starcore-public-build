package com.ethk3.novaonepro.strategyv2;

import java.io.*;
import java.lang.reflect.Method;
import java.lang.ref.WeakReference;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.util.*;
import java.util.function.BooleanSupplier;

/** Per-PID declared working-set admission. Reservations are not measured Java/native/PSS bytes. */
public final class MemoryBudgetR31 {
    public static final double HIGH_FRACTION=.75, RESUME_FRACTION=.60, EMERGENCY_FRACTION=.90, REALTIME_FRACTION=.15;
    public static final long OPTIONAL_RECOVERY_AGE_MS=120_000,OPTIONAL_RECOVERY_COOLDOWN_MS=300_000;
    private static final Object HEAP_RECOVERY_LOCK_R31=new Object();
    private static boolean heapRecoveryInFlightR31;
    private static long heapRecoveryLastRequestNanosR31;
    private static String heapRecoveryOwnerR31;
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
        /** Admission basis: heap occupancy after the latest observed collection, never above raw used heap.
         * Allocations completed after {@link #liveObservedAtNanos} are not covered by it and stay pending promises. */
        public final long liveHeapBytes,liveObservedAtNanos;public final String liveHeapBasis;
        public final String processName; public final Boolean osLowMemory;
        /** Caller samples (synthetic probes) have no collector observation: live basis equals the raw sample. */
        public Measurement(long pid,String processName,long maxHeapBytes,long heapUsedBytes,long nativeAllocatedBytes,long pssBytes,long rssBytes,Boolean osLowMemory,long observedAtNanos,long nativeObservedAtNanos){
            this(pid,processName,maxHeapBytes,heapUsedBytes,nativeAllocatedBytes,pssBytes,rssBytes,osLowMemory,observedAtNanos,nativeObservedAtNanos,heapUsedBytes,observedAtNanos,"RAW_USED_CALLER_SAMPLE");
        }
        public Measurement(long pid,String processName,long maxHeapBytes,long heapUsedBytes,long nativeAllocatedBytes,long pssBytes,long rssBytes,Boolean osLowMemory,long observedAtNanos,long nativeObservedAtNanos,long liveHeapBytes,long liveObservedAtNanos,String liveHeapBasis){
            if(maxHeapBytes<=0||heapUsedBytes<0||nativeAllocatedBytes< -1||pssBytes< -1||rssBytes< -1)throw new IllegalArgumentException("R31_MEMORY_MEASUREMENT");
            if(liveHeapBytes<0||liveHeapBytes>heapUsedBytes||liveObservedAtNanos-observedAtNanos>0||liveHeapBasis==null||liveHeapBasis.isEmpty())throw new IllegalArgumentException("R31_MEMORY_LIVE_BASIS");
            this.pid=pid;this.processName=processName;this.maxHeapBytes=maxHeapBytes;this.heapUsedBytes=heapUsedBytes;this.nativeAllocatedBytes=nativeAllocatedBytes;this.pssBytes=pssBytes;this.rssBytes=rssBytes;this.osLowMemory=osLowMemory;this.observedAtNanos=observedAtNanos;this.nativeObservedAtNanos=nativeObservedAtNanos;
            this.liveHeapBytes=liveHeapBytes;this.liveObservedAtNanos=liveObservedAtNanos;this.liveHeapBasis=bounded(liveHeapBasis,96);
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
        private WeakReference<Object> allocatedPrimitive;private long allocatedPayloadBytes,allocatedAtNanos;private boolean fixedPrivateBacking,allocatedPrimitiveGraph;
        private Lease(MemoryBudgetR31 budget,String owner,long fence,Kind kind,long bytes,BooleanSupplier cancellation){this.budget=budget;this.owner=owner;this.fence=fence;this.kind=kind;this.bytes=bytes;this.cancellation=cancellation;}
        public long bytes(){synchronized(budget){return bytes;}}
        public Admission grow(long delta){return budget.grow(this,delta);}
        /** Only a completed primitive backing allocation may stop being a future reservation.
         * The owner/declaration remains live; no measured heap or released-byte claim is changed. */
        void allocatedPrimitiveArrayR31(Object array){budget.allocatedPrimitiveArray(this,array);}
        /** Constructor owner supplies only its independently enumerated, private primitive backings.
         * Object metadata, shared teacher state and future model scratch remain reservations. */
        void allocatedOwnedPrimitiveGraphR31(Object root,Collection<Object> arrays){budget.allocatedOwnedPrimitiveGraph(this,root,arrays);}
        /** Only the pool's fixed private array plus bounded metadata has this exact lifetime. */
        void closeWhenPrivateBackingCollectedR31(Object array){budget.trackFixedPrivateBacking(this,array);}
        public boolean isClosed(){synchronized(budget){return closed;}}
        @Override public void close(){budget.release(this);}
    }
    private final Probe probe;private final Config config;
    private long heapRecoveryRequestsR31,heapRecoverySkippedR31;
    private Map<String,Object> lastHeapRecoveryAttemptR31=Collections.emptyMap(),lastHeapCollectionR31=Collections.emptyMap();
    private final Set<Lease> leases=new LinkedHashSet<>();
    private final ReferenceQueue<Object> collectedPrivateBackings=new ReferenceQueue<>();private final Map<Reference<?>,Lease> privateBackingOwners=new IdentityHashMap<>();private long collectedPrivateOwners;
    private final EnumMap<Kind,Long> nativeCaps=new EnumMap<>(Kind.class),declaredByKind=new EnumMap<>(Kind.class);
    private final EnumMap<WaitReason,Long> waits=new EnumMap<>(WaitReason.class);
    private final Map<Kind,String> nativeBasis=new EnumMap<>(Kind.class);
    private final Set<Runnable> reclaimers=Collections.newSetFromMap(new IdentityHashMap<Runnable,Boolean>());
    private final Map<String,Runnable> pressureReclaimers=new LinkedHashMap<>();private boolean pressureReclaiming;private long pressureReclaims;private String pressureReclaimFailure="";
    private Boolean systemLowMemory;private long systemLowMemoryObservedAt;
    private long optionalWaitSince,optionalLastDeniedAt,optionalLastRequestedBytes,optionalLastDeniedHeap;private Kind optionalLastDeniedKind;private WaitReason optionalLastDeniedReason=WaitReason.NONE;
    private Measurement observed;private boolean softPressure,emergency,baselineCapacityUnmet;private String pressureReason="UNOBSERVED",sampleFailure="";
    public MemoryBudgetR31(Probe probe,Config config){this.probe=Objects.requireNonNull(probe);this.config=Objects.requireNonNull(config);nativeCaps.putAll(config.nativeCapacities);for(Kind k:nativeCaps.keySet())nativeBasis.put(k,"CALLER_REGISTERED_NOT_DEVICE_CALIBRATION");update(probe.measure(),true);}
    public synchronized void registerReclaimer(Runnable reclaimer){if(reclaimers.size()>=8&&!reclaimers.contains(reclaimer))throw new IllegalStateException("R31_RECLAIMER_BOUND");reclaimers.add(Objects.requireNonNull(reclaimer));}
    synchronized void registerPressureReclaimerR31(String name,Runnable reclaimer){if(name==null||name.isEmpty()||name.length()>80)throw new IllegalArgumentException("R31_PRESSURE_RECLAIMER_NAME");if(pressureReclaimers.size()>=8&&!pressureReclaimers.containsKey(name))throw new IllegalStateException("R31_RECLAIMER_BOUND");pressureReclaimers.putIfAbsent(name,Objects.requireNonNull(reclaimer));}
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
        Admission admission=acquireOnce(owner,fence,kind,estimatedBytes,cancellation);
        return reclaimForAdmission(admission)?acquireOnce(owner,fence,kind,estimatedBytes,cancellation):admission;
    }
    private Admission acquireOnce(String owner,long fence,Kind kind,long estimatedBytes,BooleanSupplier cancellation){
        Measurement current=readProbe();synchronized(this){if(current==null)return denied(WaitReason.UNKNOWN_CAPACITY);update(current,false);WaitReason reason=admissible(kind,estimatedBytes,cancellation);if(reason!=WaitReason.NONE)return deniedRequest(kind,estimatedBytes,reason);if(leases.size()>=1024)return denied(WaitReason.OWNER_LIMIT);
            Lease lease=new Lease(this,owner,fence,kind,estimatedBytes,cancellation);leases.add(lease);declaredByKind.put(kind,safeAdd(declaredByKind.getOrDefault(kind,0L),estimatedBytes));return new Admission(lease,WaitReason.NONE);}
    }
    private Admission grow(Lease lease,long delta){
        if(delta<0)throw new IllegalArgumentException("R31_MEMORY_GROW_NEGATIVE");Admission admission=growOnce(lease,delta);return reclaimForAdmission(admission)?growOnce(lease,delta):admission;
    }
    private Admission growOnce(Lease lease,long delta){
        Measurement current=readProbe();
        synchronized(this){if(current==null)return denied(WaitReason.UNKNOWN_CAPACITY);update(current,false);if(lease.closed)return denied(WaitReason.CLOSED_LEASE);if(lease.fixedPrivateBacking&&delta!=0)throw new IllegalStateException("R31_PRIVATE_BACKING_CANNOT_GROW");WaitReason reason=admissible(lease.kind,delta,lease.cancellation);if(reason!=WaitReason.NONE)return deniedRequest(lease.kind,delta,reason);
            lease.bytes=safeAdd(lease.bytes,delta);declaredByKind.put(lease.kind,safeAdd(declaredByKind.getOrDefault(lease.kind,0L),delta));return new Admission(lease,WaitReason.NONE);}
    }
    private boolean reclaimForAdmission(Admission admission){
        if(admission.acquired()||admission.reason!=WaitReason.DECLARED_CAPACITY&&admission.reason!=WaitReason.HEAP_PRESSURE&&admission.reason!=WaitReason.EMERGENCY_PRESSURE)return false;
        java.util.List<Runnable> callbacks;synchronized(this){if(pressureReclaiming||pressureReclaimers.isEmpty())return false;pressureReclaiming=true;callbacks=new ArrayList<>(pressureReclaimers.values());}
        try{for(Runnable callback:callbacks)try{callback.run();}catch(RuntimeException failure){RuntimeProbe.rethrowProcessFatal(failure);synchronized(this){pressureReclaimFailure=bounded(failure.getClass().getName()+":"+failure.getMessage(),192);}}}
        finally{synchronized(this){pressureReclaiming=false;pressureReclaims++;}}
        // Dropped cache references are not subtracted from heap. Only a new real observation can
        // make the second (and final) attempt admissible. Never request collection on this path.
        return true;
    }
    /** Called only by the background resource controller, outside transactions and realtime windows.
     * This synchronous collector request has no hard timeout. It is never called from admission,
     * training or ordinary cache trim. A request is not proof of collection or business progress. */
    public Map<String,Object> recoverAgedOptionalHeapR31(String controllerOwner,BooleanSupplier safeBackgroundWindow){
        if(controllerOwner==null||controllerOwner.isEmpty()||controllerOwner.length()>192)throw new IllegalArgumentException("R31_RECOVERY_CONTROLLER_OWNER");
        Objects.requireNonNull(safeBackgroundWindow);long started=System.nanoTime();
        String unsafe=unsafeRecoveryWindowR31(safeBackgroundWindow);
        if(unsafe!=null)return skippedRecoveryR31(controllerOwner,unsafe,started);
        if(!(probe instanceof RuntimeProbe))return skippedRecoveryR31(controllerOwner,"REAL_RUNTIME_PROBE_REQUIRED",started);
        Measurement current=readProbe();String ineligible;
        synchronized(this){if(current!=null)update(current,false);ineligible=recoveryIneligibleR31(System.nanoTime());}
        if(ineligible!=null)return skippedRecoveryR31(controllerOwner,ineligible,started);
        String coordinationSkip=null;
        synchronized(HEAP_RECOVERY_LOCK_R31){
            if(heapRecoveryInFlightR31)coordinationSkip="COLLECTION_ALREADY_IN_FLIGHT";
            else if(heapRecoveryLastRequestNanosR31!=0&&System.nanoTime()-heapRecoveryLastRequestNanosR31<OPTIONAL_RECOVERY_COOLDOWN_MS*1_000_000L)coordinationSkip="COLLECTION_COOLDOWN";
            else{heapRecoveryInFlightR31=true;heapRecoveryOwnerR31=controllerOwner;}
        }
        if(coordinationSkip!=null)return skippedRecoveryR31(controllerOwner,coordinationSkip,started);
        try{
            // The first observation may have raced with a normal owner release or a new critical window.
            unsafe=unsafeRecoveryWindowR31(safeBackgroundWindow);
            if(unsafe!=null)return skippedRecoveryR31(controllerOwner,unsafe,started);
            current=readProbe();Map<String,Object> before;
            synchronized(this){if(current!=null)update(current,false);ineligible=recoveryIneligibleR31(System.nanoTime());before=recoveryMeasurementR31();}
            if(ineligible!=null)return skippedRecoveryR31(controllerOwner,ineligible,started);
            Map<String,Object> gcBefore=collectorObservationR31();
            unsafe=unsafeRecoveryWindowR31(safeBackgroundWindow);
            if(unsafe!=null)return skippedRecoveryR31(controllerOwner,unsafe,started);
            long requestAt=System.nanoTime();
            synchronized(HEAP_RECOVERY_LOCK_R31){heapRecoveryLastRequestNanosR31=requestAt;}
            synchronized(this){heapRecoveryRequestsR31++;}
            Throwable requestFailure=null;long collectorElapsed;
            try{Runtime.getRuntime().gc();}
            catch(RuntimeException failure){RuntimeProbe.rethrowProcessFatal(failure);requestFailure=failure;}
            finally{collectorElapsed=Math.max(0,System.nanoTime()-requestAt);}
            current=readProbe();Map<String,Object> after;boolean available;WaitReason stillWaiting;
            synchronized(this){if(current!=null)update(current,false);after=recoveryMeasurementR31();stillWaiting=optionalRequestStatus();available=sampleFailure.isEmpty()&&current!=null&&stillWaiting==WaitReason.NONE;}
            Map<String,Object> gcAfter=collectorObservationR31();
            String outcome=requestFailure!=null?"REQUEST_FAILED":current==null?"MEASUREMENT_UNKNOWN":available?"ADMISSION_AVAILABLE":"CAPACITY_WAIT";
            String reason=requestFailure!=null?bounded(requestFailure.getClass().getName()+":"+requestFailure.getMessage(),192):available?"CURRENT_REQUEST_FITS_ON_FRESH_MEASUREMENT":current==null?"AFTER_MEASUREMENT_UNAVAILABLE":"STILL_"+stillWaiting+"; LIVE_HEAP_OR_PENDING_PROMISES_OR_SYSTEM_PRESSURE_REMAIN";
            Map<String,Object> result=new LinkedHashMap<>();result.put("attempted",true);result.put("outcome",outcome);result.put("reason",reason);result.put("controllerOwner",controllerOwner);result.put("requestedAtNanos",requestAt);result.put("collectorCallElapsedNanos",collectorElapsed);result.put("controlElapsedNanos",Math.max(0,System.nanoTime()-started));result.put("before",before);result.put("after",after);result.put("gcBefore",gcBefore);result.put("gcAfter",gcAfter);
            Long oldCount=longValueR31(gcBefore.get("count")),newCount=longValueR31(gcAfter.get("count"));result.put("gcCountDelta",oldCount!=null&&newCount!=null&&newCount>=oldCount&&Objects.equals(gcBefore.get("basis"),gcAfter.get("basis"))?newCount-oldCount:null);
            long heapBefore=((Number)before.get("heapUsedBytes")).longValue(),heapAfter=((Number)after.get("heapUsedBytes")).longValue();result.put("observedHeapDecreaseBytes",current==null?null:Math.max(0,heapBefore-heapAfter));result.put("heapDeltaMeaning","OBSERVED_BEFORE_MINUS_AFTER; CONCURRENT_ALLOCATIONS_POSSIBLE; NOT_GC_ATTRIBUTION_OR_OWNER_RELEASE_BYTES");result.put("requestMeaning","RUNTIME_GC_REQUEST_ONLY; COLLECTOR_MAY_IGNORE_OR_COMPLETE_LATER; NO_HARD_TIMEOUT");result.put("businessProgress",false);result.put("capacityDebt",!available);result.put("noBenefitAction",available?"RESUME_ONLY_BY_NORMAL_ADMISSION; BUSINESS_PROGRESS_REQUIRES_DURABLE_ARTIFACT":"KEEP_BUDGETED_RESOURCE_WAIT_AND_CAPACITY_DEBT; DO_NOT_RELEASE_LIVE_OWNERS");result.put("nextAllowedRequestNanos",requestAt+OPTIONAL_RECOVERY_COOLDOWN_MS*1_000_000L);result.put("cooldownNanos",OPTIONAL_RECOVERY_COOLDOWN_MS*1_000_000L);synchronized(this){result.put("attemptCount",heapRecoveryRequestsR31);}
            Map<String,Object> immutable=Collections.unmodifiableMap(result);synchronized(this){lastHeapRecoveryAttemptR31=immutable;lastHeapCollectionR31=immutable;}return immutable;
        }finally{synchronized(HEAP_RECOVERY_LOCK_R31){heapRecoveryInFlightR31=false;heapRecoveryOwnerR31=null;}}
    }
    private String recoveryIneligibleR31(long now){
        if(!sampleFailure.isEmpty())return "MEASUREMENT_UNAVAILABLE";
        if(optionalWaitSince==0||optionalLastDeniedAt==0||optionalLastDeniedKind==null||optionalLastRequestedBytes<=0||optionalRequestStatus()==WaitReason.NONE)return "NO_ACTIVE_OPTIONAL_DENIAL";
        if(now-optionalWaitSince<0||now-optionalLastDeniedAt<0||optionalLastDeniedAt-optionalWaitSince<0)return "DENIAL_CLOCK_INVALID";
        if(now-optionalWaitSince<OPTIONAL_RECOVERY_AGE_MS*1_000_000L)return "OPTIONAL_DENIAL_NOT_AGED";
        if(optionalLastDeniedKind.nativeMemory()||optionalLastDeniedKind==Kind.REALTIME||optionalLastDeniedKind==Kind.CHECKPOINT)return "OPTIONAL_JAVA_HEAP_WAIT_REQUIRED";
        WaitReason status=optionalRequestStatus();
        if(status!=WaitReason.DECLARED_CAPACITY&&status!=WaitReason.HEAP_PRESSURE&&status!=WaitReason.EMERGENCY_PRESSURE)return "MEASURED_HEAP_CAPACITY_WAIT_REQUIRED";
        // If promises alone cannot fit even at heap=0, collecting cannot repair their debt.
        long cap=(long)(observed.maxHeapBytes*HIGH_FRACTION);long reserved=Math.max((long)Math.ceil(observed.maxHeapBytes*REALTIME_FRACTION),config.realtimePeakBytes);cap=Math.max(0,cap-Math.min(cap,reserved));if(optionalLastDeniedKind!=Kind.UI)cap=Math.max(0,cap-Math.min(cap,config.uiPeakBytes));
        long pending=0;for(Lease lease:leases)if(!lease.kind.nativeMemory())pending=safeAdd(pending,pendingJavaBytes(lease));
        if(optionalLastRequestedBytes>Math.max(0,cap-Math.min(Math.max(0,cap),pending)))return "CAPACITY_DEBT_PENDING_PROMISES_OR_OVERSIZED_REQUEST";
        long admissionHeap=admissionHeapBytes(observed);if(admissionHeap<=0||status==WaitReason.EMERGENCY_PRESSURE&&admissionHeap<observed.maxHeapBytes*HIGH_FRACTION)return "SYSTEM_PRESSURE_WITHOUT_MEASURED_HEAP_CONSTRAINT";
        return null;
    }
    private Map<String,Object> recoveryMeasurementR31(){
        long declared=0,pending=0;for(Lease lease:leases)if(!lease.kind.nativeMemory()){declared=safeAdd(declared,lease.bytes);pending=safeAdd(pending,pendingJavaBytes(lease));}
        Map<String,Object> out=new LinkedHashMap<>();out.put("pid",observed.pid);out.put("sampleAtNanos",observed.observedAtNanos);out.put("heapUsedBytes",observed.heapUsedBytes);out.put("liveHeapBytes",observed.liveHeapBytes);out.put("admissionHeapBytes",admissionHeapBytes(observed));out.put("liveHeapBasis",observed.liveHeapBasis);out.put("maxHeapBytes",observed.maxHeapBytes);out.put("nativeAllocatedBytes",observed.nativeAllocatedBytes);out.put("pssBytes",observed.pssBytes);out.put("rssBytes",observed.rssBytes);out.put("sampleFailure",sampleFailure);out.put("heapPressure",emergency?"EMERGENCY":softPressure?"SOFT":"NORMAL");out.put("osLowMemory",effectiveLowMemory());out.put("optionalUnallocatedHeadroomBytes",availableJavaBytes(Kind.RESEARCH,false));out.put("declaredJavaWorkingBytes",declared);out.put("pendingJavaAllocationBytes",pending);out.put("activeLeaseCount",leases.size());out.put("privateOwnersClosedAfterObservedCollection",collectedPrivateOwners);out.put("optionalWaitSinceNanos",optionalWaitSince);out.put("optionalLastDeniedAtNanos",optionalLastDeniedAt);out.put("optionalLastDeniedRequestedBytes",optionalLastRequestedBytes);out.put("optionalLastDeniedKind",optionalLastDeniedKind==null?null:optionalLastDeniedKind.name());out.put("currentRequestStatus",optionalRequestStatus().name());return Collections.unmodifiableMap(out);
    }
    private Map<String,Object> skippedRecoveryR31(String owner,String reason,long started){
        Map<String,Object> out=new LinkedHashMap<>();out.put("attempted",false);out.put("outcome","SKIPPED");out.put("reason",reason);out.put("controllerOwner",owner);out.put("controlElapsedNanos",Math.max(0,System.nanoTime()-started));out.put("businessProgress",false);out.put("requestMeaning","NO_COLLECTOR_REQUEST");Map<String,Object> immutable=Collections.unmodifiableMap(out);synchronized(this){heapRecoverySkippedR31++;lastHeapRecoveryAttemptR31=immutable;}return immutable;
    }
    private static String unsafeRecoveryWindowR31(BooleanSupplier safe){
        if(Thread.currentThread().isInterrupted())return "CONTROL_THREAD_INTERRUPTED";
        String vm=System.getProperty("java.vm.name","");
        if(vm.equals("Dalvik")||vm.equals("ART"))try{
            Class<?> looper=Class.forName("android.os.Looper");Object main=looper.getMethod("getMainLooper").invoke(null),current=looper.getMethod("myLooper").invoke(null);if(main!=null&&main==current)return "UI_THREAD_FORBIDDEN";
        }catch(ReflectiveOperationException|RuntimeException unavailable){RuntimeProbe.rethrowProcessFatal(unavailable);return "UI_THREAD_OBSERVATION_UNAVAILABLE";}
        try{return safe.getAsBoolean()?null:"UNSAFE_BACKGROUND_WINDOW";}catch(RuntimeException failure){RuntimeProbe.rethrowProcessFatal(failure);return "BACKGROUND_WINDOW_CHECK_FAILED:"+bounded(failure.getClass().getName(),96);}
    }
    private static Long longValueR31(Object value){if(value instanceof Number)return ((Number)value).longValue();if(value instanceof String)try{return Long.valueOf((String)value);}catch(NumberFormatException unavailable){}return null;}
    private static Map<String,Object> collectorObservationR31(){
        Map<String,Object> out=new LinkedHashMap<>();String vm=System.getProperty("java.vm.name","");
        try{if(vm.equals("Dalvik")||vm.equals("ART")){
            Object value=Class.forName("android.os.Debug").getMethod("getRuntimeStats").invoke(null);Map<?,?> stats=value instanceof Map?(Map<?,?>)value:Collections.emptyMap();out.put("basis","ANDROID_ART_RUNTIME_STATS");out.put("count",longValueR31(stats.get("art.gc.gc-count")));out.put("collectionTimeMs",null);Object raw=stats.get("art.gc.gc-time");out.put("collectionTimeRaw",raw==null?null:bounded(String.valueOf(raw),96));out.put("collectionTimeRawUnit","PLATFORM_STAT_FORMAT; NOT_ASSUMED_MILLISECONDS");
        }else{
            Object beans=Class.forName("java.lang.management.ManagementFactory").getMethod("getGarbageCollectorMXBeans").invoke(null);Class<?> api=Class.forName("java.lang.management.GarbageCollectorMXBean");long count=0,time=0;boolean known=false;
            if(beans instanceof List)for(Object bean:(List<?>)beans){long n=((Number)api.getMethod("getCollectionCount").invoke(bean)).longValue(),ms=((Number)api.getMethod("getCollectionTime").invoke(bean)).longValue();if(n>=0){count+=n;known=true;}if(ms>=0)time+=ms;}
            out.put("basis","HOST_JMX_GARBAGE_COLLECTOR_MXBEANS");out.put("count",known?count:null);out.put("collectionTimeMs",known?time:null);
        }}catch(ReflectiveOperationException|RuntimeException unavailable){RuntimeProbe.rethrowProcessFatal(unavailable);out.put("basis","UNAVAILABLE");out.put("count",null);out.put("collectionTimeMs",null);out.put("unavailable",bounded(unavailable.getClass().getName(),96));}
        out.put("status",out.get("count")!=null||out.get("collectionTimeRaw")!=null?"OBSERVED":"UNSUPPORTED_OR_UNKNOWN");return Collections.unmodifiableMap(out);
    }
    private Map<String,Object> heapRecoveryObservationR31(){
        Map<String,Object> out=new LinkedHashMap<>();out.put("scope","PER_PID_EXPLICIT_BACKGROUND_CONTROLLER_ONLY");out.put("minOptionalWaitMs",OPTIONAL_RECOVERY_AGE_MS);out.put("cooldownMs",OPTIONAL_RECOVERY_COOLDOWN_MS);out.put("cooldownNanos",OPTIONAL_RECOVERY_COOLDOWN_MS*1_000_000L);out.put("attemptCount",heapRecoveryRequestsR31);out.put("skippedCount",heapRecoverySkippedR31);out.put("lastDecision",lastHeapRecoveryAttemptR31);out.put("lastActualAttempt",lastHeapCollectionR31);
        synchronized(HEAP_RECOVERY_LOCK_R31){out.put("inFlight",heapRecoveryInFlightR31);out.put("inFlightControllerOwner",heapRecoveryOwnerR31);out.put("lastProcessRequestNanos",heapRecoveryLastRequestNanosR31);}return Collections.unmodifiableMap(out);
    }
    private synchronized void allocatedPrimitiveArray(Lease lease,Object array){
        if(lease.closed)throw new IllegalStateException("R31_MEMORY_CLOSED_ALLOCATION");
        if(lease.kind.nativeMemory())throw new IllegalArgumentException("R31_MEMORY_NATIVE_IS_NOT_JAVA_ARRAY");
        long payload;
        if(array instanceof double[])payload=8L*((double[])array).length;
        else if(array instanceof float[])payload=4L*((float[])array).length;
        else if(array instanceof int[])payload=4L*((int[])array).length;
        else if(array instanceof byte[])payload=((byte[])array).length;
        else throw new IllegalArgumentException("R31_MEMORY_UNPROVEN_ALLOCATION");
        if(payload>lease.bytes)throw new IllegalArgumentException("R31_MEMORY_ALLOCATION_EXCEEDS_RESERVATION");
        if(lease.allocatedPrimitive!=null){if(lease.allocatedPrimitive.get()!=array)throw new IllegalStateException("R31_MEMORY_ALLOCATION_ALREADY_RECORDED");return;}
        lease.allocatedPrimitive=new WeakReference<>(array);lease.allocatedPayloadBytes=payload;lease.allocatedAtNanos=System.nanoTime();
    }
    private synchronized void allocatedOwnedPrimitiveGraph(Lease lease,Object root,Collection<Object> arrays){
        if(lease.closed)throw new IllegalStateException("R31_MEMORY_CLOSED_ALLOCATION");
        if(lease.kind!=Kind.MODEL&&lease.kind!=Kind.REALTIME)throw new IllegalArgumentException("R31_MODEL_PRIVATE_GRAPH_OWNER_REQUIRED");
        if(root==null||root.getClass().isArray()||arrays==null||arrays.size()>200000)throw new IllegalArgumentException("R31_MODEL_PRIVATE_GRAPH_BOUND");
        if(lease.allocatedPrimitive!=null)throw new IllegalStateException("R31_MEMORY_ALLOCATION_ALREADY_RECORDED");
        IdentityHashMap<Object,Boolean> seen=new IdentityHashMap<>();long payload=0;
        for(Object array:arrays){
            if(array==null||!array.getClass().isArray()||!array.getClass().getComponentType().isPrimitive()||seen.put(array,Boolean.TRUE)!=null)throw new IllegalArgumentException("R31_MODEL_PRIVATE_PRIMITIVE_WITNESS");
            Class<?> component=array.getClass().getComponentType();int width=component==double.class||component==long.class?8:component==int.class||component==float.class?4:component==short.class||component==char.class?2:1;
            payload=Math.addExact(payload,Math.multiplyExact((long)java.lang.reflect.Array.getLength(array),width));
            if(payload>lease.bytes)throw new IllegalArgumentException("R31_MEMORY_ALLOCATION_EXCEEDS_RESERVATION");
        }
        // The model has already been constructed. A future probe must start after this marker
        // before its primitive payload can be covered by measured heap instead of a promise.
        lease.allocatedPrimitive=new WeakReference<>(root);lease.allocatedPayloadBytes=payload;lease.allocatedPrimitiveGraph=true;lease.allocatedAtNanos=System.nanoTime();
    }
    private long pendingJavaBytes(Lease lease){
        // A probe sampled before allocation must still count the whole promise. This also closes
        // the admission race when another thread finishes its allocation after readProbe().
        long covered=lease.allocatedAtNanos!=0&&lease.allocatedAtNanos<=observed.observedAtNanos?lease.allocatedPayloadBytes:0;
        return lease.bytes-covered;
    }
    /** Admission heap basis: post-collection live heap plus proven payload allocated after that collection
     * and covered by the raw sample. Never above the raw sample, so garbage awaiting collection is not admitted debt. */
    private long admissionHeapBytes(Measurement m){
        long since=0;for(Lease lease:leases)if(!lease.kind.nativeMemory()&&lease.allocatedAtNanos!=0&&lease.allocatedAtNanos-m.liveObservedAtNanos>0&&lease.allocatedAtNanos<=m.observedAtNanos)since=safeAdd(since,lease.allocatedPayloadBytes);
        return Math.min(m.heapUsedBytes,safeAdd(m.liveHeapBytes,since));
    }
    private synchronized void trackFixedPrivateBacking(Lease lease,Object array){
        if(lease.closed||lease.allocatedPrimitiveGraph||lease.allocatedPrimitive==null||lease.allocatedPrimitive.get()!=array||lease.bytes>lease.allocatedPayloadBytes+512)throw new IllegalStateException("R31_PRIVATE_BACKING_LIFETIME_UNPROVEN");
        if(lease.fixedPrivateBacking)return;
        lease.fixedPrivateBacking=true;lease.allocatedPrimitive=new WeakReference<>(array,collectedPrivateBackings);privateBackingOwners.put(lease.allocatedPrimitive,lease);
    }
    private synchronized void reapCollectedPrivateBackings(){
        for(int i=0;i<1024;i++){Reference<?> ref=collectedPrivateBackings.poll();if(ref==null)break;Lease lease=privateBackingOwners.remove(ref);if(lease!=null){collectedPrivateOwners++;release(lease);}}
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
        return bytes<=availableJavaBytes(kind,critical)?WaitReason.NONE:WaitReason.DECLARED_CAPACITY;
    }
    private long availableJavaBytes(Kind kind,boolean critical){
        long high=(long)(observed.maxHeapBytes*(critical?EMERGENCY_FRACTION:HIGH_FRACTION));
        long reserved=critical?0:Math.max((long)Math.ceil(observed.maxHeapBytes*REALTIME_FRACTION),config.realtimePeakBytes);
        long declared=0;for(Lease lease:leases)if(!lease.kind.nativeMemory())declared=safeAdd(declared,pendingJavaBytes(lease));
        long available=Math.max(0,high-Math.min(high,admissionHeapBytes(observed)));available=Math.max(0,available-Math.min(available,reserved));if(!critical&&kind!=Kind.UI)available=Math.max(0,available-Math.min(available,config.uiPeakBytes));available=Math.max(0,available-Math.min(available,declared));
        return available;
    }
    /** Leases whose lifetime is an explicit owner close (not a fixed private backing that closes after collection). */
    public synchronized int explicitOwnerLeaseCountR31(){reapCollectedPrivateBackings();int n=0;for(Lease lease:leases)if(!lease.fixedPrivateBacking)n++;return n;}
    public synchronized List<String> explicitOwnerLeasesR31(int limit){List<String> out=new ArrayList<>();for(Lease lease:leases)if(!lease.fixedPrivateBacking&&out.size()<Math.max(0,Math.min(64,limit)))out.add(lease.owner+"/"+lease.kind+"/"+lease.bytes);return Collections.unmodifiableList(out);}
    private synchronized void release(Lease lease){if(lease.closed)return;lease.closed=true;leases.remove(lease);if(lease.fixedPrivateBacking)privateBackingOwners.remove(lease.allocatedPrimitive);long prior=declaredByKind.getOrDefault(lease.kind,0L);if(lease.bytes>prior)throw new IllegalStateException("R31_MEMORY_ACCOUNTING");declaredByKind.put(lease.kind,prior-lease.bytes);}
    private Admission denied(WaitReason reason){waits.put(reason,waits.getOrDefault(reason,0L)+1);return new Admission(null,reason);}
    private Admission deniedRequest(Kind kind,long bytes,WaitReason reason){
        if(!kind.nativeMemory()&&kind!=Kind.REALTIME&&kind!=Kind.CHECKPOINT&&(reason==WaitReason.DECLARED_CAPACITY||reason==WaitReason.HEAP_PRESSURE||reason==WaitReason.EMERGENCY_PRESSURE)){
            long now=System.nanoTime();if(optionalWaitSince==0)optionalWaitSince=now;optionalLastDeniedAt=now;optionalLastRequestedBytes=bytes;optionalLastDeniedKind=kind;optionalLastDeniedReason=reason;optionalLastDeniedHeap=admissionHeapBytes(observed);
        }
        return denied(reason);
    }
    private WaitReason optionalRequestStatus(){return optionalLastDeniedKind==null?WaitReason.NONE:admissible(optionalLastDeniedKind,optionalLastRequestedBytes,null);}
    private synchronized void update(Measurement measurement,boolean initial){
        Objects.requireNonNull(measurement);if(observed!=null&&measurement.observedAtNanos<observed.observedAtNanos)return;observed=measurement;sampleFailure="";double fraction=admissionHeapBytes(measurement)/(double)measurement.maxHeapBytes;
        if(initial&&fraction>=HIGH_FRACTION)baselineCapacityUnmet=true;
        emergency=fraction>=EMERGENCY_FRACTION||Boolean.TRUE.equals(effectiveLowMemory());
        if(emergency||fraction>=HIGH_FRACTION)softPressure=true;
        else if(fraction<=RESUME_FRACTION&&!Boolean.TRUE.equals(effectiveLowMemory()))softPressure=false;
        pressureReason=emergency?"EMERGENCY_HEAP_OR_OS_PRESSURE":softPressure?(baselineCapacityUnmet?"BASELINE_CAPACITY_UNMET":"HEAP_HIGH_WATER_LATCHED"):"NORMAL";
        if(optionalWaitSince!=0&&optionalRequestStatus()==WaitReason.NONE)optionalWaitSince=0;
    }
    private Measurement readProbe(){reapCollectedPrivateBackings();List<Runnable> cleanup;synchronized(this){cleanup=new ArrayList<>(reclaimers);}for(Runnable reclaimer:cleanup)reclaimer.run();try{return probe.measure();}catch(RuntimeException failure){synchronized(this){sampleFailure=failure.getClass().getName();}return null;}}
    public Map<String,Object> sample(){Measurement current=readProbe();synchronized(this){if(current!=null)update(current,false);return measurementMap();}}
    public synchronized Map<String,Object> snapshot(){
        reapCollectedPrivateBackings();
        Map<String,Object> out=measurementMap();long java=0,nativeBytes=0,queued=declaredByKind.getOrDefault(Kind.QUEUED,0L);Map<String,Object> kinds=new LinkedHashMap<>();
        for(Map.Entry<Kind,Long> e:declaredByKind.entrySet()){kinds.put(e.getKey().name(),e.getValue());if(e.getKey().nativeMemory())nativeBytes=safeAdd(nativeBytes,e.getValue());else java=safeAdd(java,e.getValue());}
        long pending=0;for(Lease lease:leases)if(!lease.kind.nativeMemory())pending=safeAdd(pending,pendingJavaBytes(lease));
        out.put("optionalUnallocatedHeadroomBytes",availableJavaBytes(Kind.RESEARCH,false));out.put("criticalUnallocatedHeadroomBytes",availableJavaBytes(Kind.REALTIME,true));
        out.put("privateBackingOwnersAwaitingCollectionOrClose",privateBackingOwners.size());out.put("privateOwnersClosedAfterObservedCollection",collectedPrivateOwners);
        out.put("declaredJavaWorkingBytes",java);out.put("pendingJavaAllocationBytes",pending);out.put("allocatedJavaPayloadCoveredByHeapSampleBytes",java-pending);out.put("allocatedPayloadMeaning","PROVEN_PRIMITIVE_PAYLOAD_ONLY; NOT_LIVE_HEAP_OR_RELEASE_EVIDENCE");out.put("declaredNativeWorkingBytes",nativeBytes);out.put("declaredQueuedBytes",queued);out.put("declaredByKind",kinds);out.put("activeLeaseCount",leases.size());out.put("explicitOwnerLeaseCount",explicitOwnerLeaseCountR31());out.put("pressureReclaimAttempts",pressureReclaims);out.put("pressureReclaimFailure",pressureReclaimFailure);
        List<Lease> largest=new ArrayList<>(leases);largest.sort((a,b)->Long.compare(b.bytes,a.bytes));List<Object> owners=new ArrayList<>();for(int i=0;i<Math.min(32,largest.size());i++){Lease l=largest.get(i);Map<String,Object> row=new LinkedHashMap<>();row.put("owner",l.owner);row.put("fence",l.fence);row.put("kind",l.kind.name());row.put("declaredBytes",l.bytes);row.put("pendingJavaAllocationBytes",l.kind.nativeMemory()?null:pendingJavaBytes(l));row.put("provenAllocatedPrimitivePayloadBytes",l.allocatedPayloadBytes);row.put("primitiveAllocationBasis",l.allocatedPrimitive==null?"NOT_MATERIALIZED":l.allocatedPrimitiveGraph?"OWNER_ENUMERATED_PRIVATE_MODEL_ARRAYS":"SINGLE_PRIMITIVE_BACKING");row.put("measurement","ESTIMATED_WORKING_SET_NOT_ACTUAL_HEAP");owners.add(row);}out.put("largestOwners",owners);out.put("ownersTruncated",largest.size()>32);
        Map<String,Object> caps=new LinkedHashMap<>();for(Kind kind:nativeCaps.keySet()){Map<String,Object> row=new LinkedHashMap<>();row.put("bytes",nativeCaps.get(kind));row.put("basis",nativeBasis.get(kind));caps.put(kind.name(),row);}out.put("nativeDeclaredCapacities",caps);out.put("nativeHardCapacityVerdict","UNMANAGED_NATIVE_OWNERS_NOT_COVERED");Map<String,Object> failures=new LinkedHashMap<>();for(Map.Entry<WaitReason,Long> e:waits.entrySet())failures.put(e.getKey().name(),e.getValue());out.put("admissionWaitCounts",failures);return out;
    }
    private Map<String,Object> measurementMap(){
        WaitReason optionalStatus=optionalRequestStatus();boolean recheckBlocked=optionalLastDeniedAt!=0&&optionalStatus!=WaitReason.NONE,optionalDenied=optionalWaitSince!=0&&recheckBlocked;
        Map<String,Object> optional=new LinkedHashMap<>();optional.put("optionalAdmissionStatus",optionalDenied?"RESOURCE_WAIT:MEMORY_"+optionalStatus:emergency?"RESOURCE_WAIT:MEMORY_EMERGENCY_PRESSURE":softPressure?"RESOURCE_WAIT:MEMORY_HEAP_PRESSURE":availableJavaBytes(Kind.RESEARCH,false)==0?"RESOURCE_WAIT:MEMORY_DECLARED_CAPACITY":"AVAILABLE");optional.put("optionalWaitSinceNanos",optionalDenied?optionalWaitSince:0);optional.put("optionalLastDeniedAtNanos",optionalLastDeniedAt);optional.put("optionalLastDeniedRequestedBytes",optionalLastRequestedBytes);optional.put("optionalLastDeniedKind",optionalLastDeniedKind==null?null:optionalLastDeniedKind.name());optional.put("optionalLastDeniedReason",optionalLastDeniedReason.name());optional.put("optionalLastDeniedObservedHeapBytes",optionalLastDeniedHeap);optional.put("optionalDenialStillBlocking",optionalDenied);optional.put("optionalLastRequestRecheckBlocked",recheckBlocked);optional.put("optionalUnallocatedHeadroomBytes",availableJavaBytes(Kind.RESEARCH,false));
        Map<String,Object> out=new LinkedHashMap<>();out.putAll(optional);out.put("optionalHeapRecovery",heapRecoveryObservationR31());out.put("scope","PER_PID");out.put("pid",observed.pid);out.put("processName",observed.processName);out.put("crossProcessBudget","NOT_COORDINATED");out.put("coverage","REGISTERED_OWNERS_ONLY_NOT_ALL_APPLICATION_ALLOCATIONS");out.put("maxHeapBytes",observed.maxHeapBytes);out.put("heapUsedBytes",observed.heapUsedBytes);out.put("liveHeapBytes",observed.liveHeapBytes);out.put("admissionHeapBytes",admissionHeapBytes(observed));out.put("liveHeapBasis",observed.liveHeapBasis);out.put("liveObservedAtNanos",observed.liveObservedAtNanos);out.put("nativeAllocatedBytes",observed.nativeAllocatedBytes);out.put("pssBytes",observed.pssBytes);out.put("rssBytes",observed.rssBytes);out.put("osLowMemory",effectiveLowMemory());out.put("systemLowMemoryObservedAtNanos",systemLowMemoryObservedAt);out.put("observedAtNanos",observed.observedAtNanos);out.put("nativeObservedAtNanos",observed.nativeObservedAtNanos);out.put("clock","PROCESS_MONOTONIC_NS");out.put("highFraction",HIGH_FRACTION);out.put("resumeFraction",RESUME_FRACTION);out.put("emergencyFraction",EMERGENCY_FRACTION);out.put("realtimeReservedBytes",Math.max((long)Math.ceil(observed.maxHeapBytes*REALTIME_FRACTION),config.realtimePeakBytes));out.put("uiReservedBytes",config.uiPeakBytes);out.put("pressure",emergency?"EMERGENCY":softPressure?"SOFT":"NORMAL");out.put("reason",sampleFailure.isEmpty()?pressureReason:"MEMORY_MEASUREMENT_UNAVAILABLE");out.put("sampleFailure",sampleFailure);out.put("baselineCapacityUnmetObserved",baselineCapacityUnmet);out.put("resumeCondition","liveHeap/maxHeap <= 0.60 AND osLowMemory != true; owners must release actual work");out.put("allowOptionalWork",!softPressure&&sampleFailure.isEmpty()&&!optionalDenied);out.put("recommendedParallelism",softPressure||optionalDenied?1:2);out.put("accounting","ADMISSION_USES_POST_OBSERVED_GC_LIVE_HEAP_PLUS_PENDING_ALLOCATION_PROMISES; ADMISSION_HEAP=min(RAW_USED,POST_GC_LIVE+PROVEN_PAYLOAD_ALLOCATED_SINCE_COLLECTION); PROVEN_ARRAY_PAYLOAD_REQUIRES_POST_ALLOCATION_SAMPLE; RAW_USED_HEAP_REPORTED_NOT_ADMITTED; NEVER_SUM_HEAP_NATIVE_PSS");return out;
    }
    private static long safeAdd(long a,long b){if(b<0||a>Long.MAX_VALUE-b)throw new IllegalArgumentException("R31_MEMORY_SIZE_OVERFLOW");return a+b;}
    private static String bounded(String text,int limit){return text.substring(0,Math.min(text.length(),limit)).replace('\n',' ').replace('\r',' ');}
    /** Java heap is fresh per request; potentially unavailable native/proc observations refresh at most once/s. */
    public static final class RuntimeProbe implements Probe {
        private long sampledNativeAt,nativeBytes=-1,pss=-1,rss=-1;private final long pid=pid();private final String processName=processName();
        private final LiveHeapObserver live=new LiveHeapObserver();
        public synchronized Measurement measure(){
            Runtime runtime=Runtime.getRuntime();long now=System.nanoTime();if(sampledNativeAt==0||now-sampledNativeAt>=1_000_000_000L){nativeBytes=nativeAllocated();long[] proc=procMemory();pss=proc[0];rss=proc[1];sampledNativeAt=now;}
            long used=Math.max(0,runtime.totalMemory()-runtime.freeMemory());long[] observedLive=live.observe(used,now);
            return new Measurement(pid,processName,runtime.maxMemory(),used,nativeBytes,pss,rss,null,now,sampledNativeAt,observedLive[0],observedLive[1],live.basis);
        }
        private static long nativeAllocated(){String vm=System.getProperty("java.vm.name","");if(!vm.equals("Dalvik")&&!vm.equals("ART"))return -1;try{Class<?> debug=Class.forName("android.os.Debug");Method method=debug.getMethod("getNativeHeapAllocatedSize");return ((Number)method.invoke(null)).longValue();}catch(ReflectiveOperationException|SecurityException unavailable){rethrowProcessFatal(unavailable);return -1;}}
        private static void rethrowProcessFatal(Throwable failure){for(int depth=0;failure!=null&&depth<32;depth++,failure=failure.getCause()){if(failure instanceof VirtualMachineError)throw(VirtualMachineError)failure;if(failure instanceof ThreadDeath)throw(ThreadDeath)failure;if(failure instanceof LinkageError)throw(LinkageError)failure;}}
        private static long pid(){try{return Long.parseLong(new File("/proc/self").getCanonicalFile().getName());}catch(IOException|NumberFormatException unavailable){return -1;}}
        private static String processName(){try(InputStream in=new FileInputStream("/proc/self/cmdline")){byte[] buffer=new byte[192];int length=in.read(buffer);if(length<=0)return "UNKNOWN";int end=0;while(end<length&&buffer[end]!=0)end++;return new String(buffer,0,end,java.nio.charset.StandardCharsets.UTF_8);}catch(IOException unavailable){return "UNKNOWN";}}
        private static long[] procMemory(){long pss=-1,rss=-1;for(String path:new String[]{"/proc/self/smaps_rollup","/proc/self/status"})try(BufferedReader reader=new BufferedReader(new FileReader(path))){String line;int chars=0;while((line=reader.readLine())!=null&&(chars+=line.length())<=65536){if(line.startsWith("Pss:"))pss=kib(line);if(line.startsWith("Rss:")||rss<0&&line.startsWith("VmRSS:"))rss=kib(line);}}catch(IOException|NumberFormatException unavailable){/* unknown remains -1 */}return new long[]{pss,rss};}
        /** Post-collection heap occupancy, observed and never requested here. HotSpot: heap pools after the
         * most recent completed collection (GcInfo). ART: the first sample after the runtime GC count advanced.
         * Without any collector observation the raw used heap remains the basis. Result is clamped to raw used:
         * both are upper bounds of the live set at their own observation time. */
        static final class LiveHeapObserver {
            String basis="RAW_USED_NO_COLLECTION_OBSERVED";private long lastCount=-1,postGcBytes=-1,postGcAtNanos;private boolean unsupported;
            long[] observe(long used,long now){
                long nursery=-1;
                if(!unsupported)try{if(art())observeArt(used,now);else{observeHotSpot(now);nursery=nurseryUsedHotSpot();}}catch(ReflectiveOperationException|RuntimeException failure){rethrowProcessFatal(failure);unsupported=true;postGcBytes=-1;nursery=-1;basis="RAW_USED_COLLECTOR_OBSERVATION_UNAVAILABLE";}
                if(postGcBytes<0||postGcAtNanos-now>0)return new long[]{used,now};
                if(postGcBytes>=used)return new long[]{used,now};
                // Growth outside the nursery since the collection (old/humongous regions: large arrays,
                // promoted data) is retained until a later collection proves otherwise, so it counts now.
                // Only nursery allocations, bounded by the young generation and collected next, stay
                // unproven. Never above raw used; no collection is requested here.
                if(nursery>=0){long outside=Math.max(0,used-nursery);if(outside>postGcBytes)return new long[]{Math.min(used,outside),now};}
                return new long[]{postGcBytes,postGcAtNanos};
            }
            private static long nurseryUsedHotSpot()throws ReflectiveOperationException{
                Class<?> factory=Class.forName("java.lang.management.ManagementFactory"),pool=Class.forName("java.lang.management.MemoryPoolMXBean"),usage=Class.forName("java.lang.management.MemoryUsage");
                Object heapType=Class.forName("java.lang.management.MemoryType").getField("HEAP").get(null);long eden=-1;
                for(Object p:(List<?>)factory.getMethod("getMemoryPoolMXBeans").invoke(null)){
                    if(!heapType.equals(pool.getMethod("getType").invoke(p)))continue;
                    if(!String.valueOf(pool.getMethod("getName").invoke(p)).contains("Eden"))continue;
                    Object current=pool.getMethod("getUsage").invoke(p);if(current==null)continue;
                    eden=Math.max(0,eden)+Math.max(0,((Number)usage.getMethod("getUsed").invoke(current)).longValue());
                }
                return eden;
            }
            private static boolean art(){String vm=System.getProperty("java.vm.name","");return vm.equals("Dalvik")||vm.equals("ART");}
            private void observeArt(long used,long now)throws ReflectiveOperationException{
                Object raw=Class.forName("android.os.Debug").getMethod("getRuntimeStat",String.class).invoke(null,"art.gc.gc-count");
                long count=raw==null?-1:Long.parseLong(String.valueOf(raw).trim());if(count<0){basis="RAW_USED_ART_GC_COUNT_UNAVAILABLE";return;}
                if(lastCount>=0&&count!=lastCount){postGcBytes=used;postGcAtNanos=now;basis="ART_FIRST_SAMPLE_AFTER_GC_COUNT_ADVANCE";}
                lastCount=count;
            }
            private void observeHotSpot(long now)throws ReflectiveOperationException{
                Class<?> factory=Class.forName("java.lang.management.ManagementFactory");Object beans=factory.getMethod("getGarbageCollectorMXBeans").invoke(null);
                Class<?> gcApi=Class.forName("java.lang.management.GarbageCollectorMXBean");long count=0;
                for(Object bean:(List<?>)beans){long n=((Number)gcApi.getMethod("getCollectionCount").invoke(bean)).longValue();if(n>0)count+=n;}
                if(count==lastCount)return;lastCount=count;if(count==0)return;
                Class<?> ext=Class.forName("com.sun.management.GarbageCollectorMXBean"),info=Class.forName("com.sun.management.GcInfo"),usage=Class.forName("java.lang.management.MemoryUsage"),pool=Class.forName("java.lang.management.MemoryPoolMXBean");
                Set<String> heapPools=new HashSet<>();Object heapType=Class.forName("java.lang.management.MemoryType").getField("HEAP").get(null);
                for(Object p:(List<?>)factory.getMethod("getMemoryPoolMXBeans").invoke(null))if(heapType.equals(pool.getMethod("getType").invoke(p)))heapPools.add((String)pool.getMethod("getName").invoke(p));
                long bestEnd=-1,bestBytes=-1;
                for(Object bean:(List<?>)beans){if(!ext.isInstance(bean))continue;Object last=ext.getMethod("getLastGcInfo").invoke(bean);if(last==null)continue;
                    long end=((Number)info.getMethod("getEndTime").invoke(last)).longValue();if(end<bestEnd)continue;long bytes=0;
                    for(Map.Entry<?,?> e:((Map<?,?>)info.getMethod("getMemoryUsageAfterGc").invoke(last)).entrySet())if(heapPools.contains(String.valueOf(e.getKey())))bytes+=Math.max(0,((Number)usage.getMethod("getUsed").invoke(e.getValue())).longValue());
                    bestEnd=end;bestBytes=bytes;}
                if(bestEnd<0){basis="RAW_USED_NO_GC_INFO";return;}
                long uptime=((Number)Class.forName("java.lang.management.RuntimeMXBean").getMethod("getUptime").invoke(factory.getMethod("getRuntimeMXBean").invoke(null))).longValue();
                postGcBytes=bestBytes;postGcAtNanos=now-Math.max(0,uptime-bestEnd)*1_000_000L;basis="HOTSPOT_HEAP_POOLS_AFTER_LATEST_GC";
            }
        }
        private static long kib(String line){String[] parts=line.trim().split("\\s+");long value=Long.parseLong(parts[1]);return value<0||value>Long.MAX_VALUE/1024?-1:value*1024;}
    }
}
