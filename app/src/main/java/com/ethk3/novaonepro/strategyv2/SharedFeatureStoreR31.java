package com.ethk3.novaonepro.strategyv2;

import java.util.*;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/** Immutable primitive input blocks, scoped to one PID. Mutable model/optimizer objects are not accepted. */
public final class SharedFeatureStoreR31 implements AutoCloseable {
    public static final class Key {
        public final String datasetHash,featureAbi,normalizer,featureKey;public final long pitCutoff;
        public Key(String datasetHash,long pitCutoff,String featureAbi,String normalizer,String featureKey){
            if(datasetHash==null||!datasetHash.matches("[0-9a-f]{64}")||pitCutoff<0)throw new IllegalArgumentException("R31_FEATURE_DATASET_PIT");
            this.datasetHash=datasetHash;this.pitCutoff=pitCutoff;this.featureAbi=field(featureAbi,192);this.normalizer=field(normalizer,192);this.featureKey=field(featureKey,256);
        }
        private static String field(String value,int max){if(value==null||value.isEmpty()||value.length()>max)throw new IllegalArgumentException("R31_FEATURE_KEY");return value;}
        @Override public boolean equals(Object value){if(!(value instanceof Key))return false;Key k=(Key)value;return pitCutoff==k.pitCutoff&&datasetHash.equals(k.datasetHash)&&featureAbi.equals(k.featureAbi)&&normalizer.equals(k.normalizer)&&featureKey.equals(k.featureKey);}
        @Override public int hashCode(){return Objects.hash(datasetHash,pitCutoff,featureAbi,normalizer,featureKey);}
    }
    public enum Type { DOUBLE(8),FLOAT(4),INT(4),BYTE(1);final int width;Type(int width){this.width=width;} }
    private static final class Backing {
        final Type type;final int length;final long bytes,id;final MemoryBudgetR31.Lease lease;final boolean reusable;Object values;
        Backing(Type type,int length,long bytes,long id,Object values,MemoryBudgetR31.Lease lease,boolean reusable){this.type=type;this.length=length;this.bytes=bytes;this.id=id;this.values=values;this.lease=lease;this.reusable=reusable;}
    }
    private static final class Entry {final Key key;final Type type;final int length;final long bytes,id;final Backing backing;Object values;int readers;Entry(Key key,long id,Backing backing){this.key=key;this.type=backing.type;this.length=backing.length;this.bytes=backing.bytes;this.id=id;this.values=backing.values;this.backing=backing;}}
    private static final class Production {final long bytes;Backing backing;MemoryBudgetR31.Lease lease;boolean active=true;Production(long bytes,Backing backing){this.bytes=bytes;this.backing=backing;this.lease=backing==null?null:backing.lease;}}
    interface DoubleTargetProducerR31 {void fill(double[] target);}
    private static final class WorkingReference extends java.lang.ref.WeakReference<double[]> {
        final long bytes,id;final MemoryBudgetR31.Lease lease;
        WorkingReference(Backing backing,java.lang.ref.ReferenceQueue<double[]> queue){super((double[])backing.values,queue);bytes=backing.bytes;id=backing.id;lease=backing.lease;}
    }
    /** Exclusive private built-in scratch. Mutable arrays never leave the internal scoped evaluator. */
    static final class PrivateWorkingR31 implements AutoCloseable {
        private SharedFeatureStoreR31 store;private Backing backing;private WorkingReference reference;private final boolean allocated;
        private PrivateWorkingR31(SharedFeatureStoreR31 store,Backing backing,WorkingReference reference,boolean allocated){this.store=store;this.backing=backing;this.reference=reference;this.allocated=allocated;}
        synchronized double[] exclusiveValuesR31(){if(backing==null)throw new IllegalStateException("R31_PRIVATE_WORKING_CLOSED");return(double[])backing.values;}
        synchronized long privateBackingIdentityR31(){if(backing==null)throw new IllegalStateException("R31_PRIVATE_WORKING_CLOSED");return backing.id;}
        boolean allocatedBackingR31(){return allocated;}
        @Override public synchronized void close(){if(backing!=null){Backing owned=backing;backing=null;store.releaseWorking(reference,owned);reference=null;store=null;}}
    }

    public static final class Publication {
        public final Reader reader;public final MemoryBudgetR31.WaitReason reason;public final boolean stored;
        private Publication(Reader reader,MemoryBudgetR31.WaitReason reason,boolean stored){this.reader=reader;this.reason=reason;this.stored=stored;}
        public boolean acquired(){return reader!=null;}
    }
    /** Reader owns the reference. No raw array, buffer or entry object can escape after close. */
    public static final class Reader implements AutoCloseable {
        private final SharedFeatureStoreR31 store;public final String owner;private Entry entry;
        private Reader(SharedFeatureStoreR31 store,String owner,Entry entry){this.store=store;this.owner=owner;this.entry=entry;}
        private Entry open(){if(entry==null)throw new IllegalStateException("R31_FEATURE_READER_CLOSED");return entry;}
        public synchronized int length(){return open().length;}
        public synchronized Type type(){return open().type;}
        public synchronized long storageIdentity(){return open().id;}
        public synchronized long privateBackingIdentityR31(){return open().backing.id;}
        public synchronized double doubleAt(int index){return ((double[])typed(Type.DOUBLE).values)[index];}
        public synchronized float floatAt(int index){return ((float[])typed(Type.FLOAT).values)[index];}
        public synchronized int intAt(int index){return ((int[])typed(Type.INT).values)[index];}
        public synchronized byte byteAt(int index){return ((byte[])typed(Type.BYTE).values)[index];}
        private Entry typed(Type type){Entry e=open();if(e.type!=type)throw new IllegalStateException("R31_FEATURE_PRIMITIVE_TYPE");return e;}
        public synchronized void copyRangeTo(int sourceOffset,double[] target,int targetOffset,int length){Entry e=typed(Type.DOUBLE);System.arraycopy(e.values,sourceOffset,target,targetOffset,length);}
        public synchronized void copyTo(double[] target,int offset){Entry e=typed(Type.DOUBLE);System.arraycopy(e.values,0,target,offset,e.length);}
        public synchronized void copyTo(float[] target,int offset){Entry e=typed(Type.FLOAT);System.arraycopy(e.values,0,target,offset,e.length);}
        public synchronized void copyTo(int[] target,int offset){Entry e=typed(Type.INT);System.arraycopy(e.values,0,target,offset,e.length);}
        public synchronized void copyTo(byte[] target,int offset){Entry e=typed(Type.BYTE);System.arraycopy(e.values,0,target,offset,e.length);}
        @Override public synchronized void close(){if(entry!=null){Entry prior=entry;entry=null;store.release(prior);}}
    }
    private final MemoryBudgetR31 memory;private final long capacityBytes;
    private final LinkedHashMap<Key,Entry> entries=new LinkedHashMap<>(16,.75f,true);
    private final ArrayDeque<Backing> idle=new ArrayDeque<>();
    private final Set<Production> productions=Collections.newSetFromMap(new IdentityHashMap<Production,Boolean>());
    private final java.lang.ref.ReferenceQueue<double[]> workingQueue=new java.lang.ref.ReferenceQueue<>();
    private final Set<WorkingReference> working=Collections.newSetFromMap(new IdentityHashMap<WorkingReference,Boolean>());
    private static final java.util.concurrent.atomic.AtomicLong STORE_IDS=new java.util.concurrent.atomic.AtomicLong();
    private final long storeId=STORE_IDS.incrementAndGet();
    private long bytes,ownedBytes,idleBytes,inflightBytes,workingBytes,nextId,nextBackingId,declaredReleased,backingAllocations,backingReuses,recycledEntries,collectedWorkingOwners;
    private int readers;private boolean closed;
    public SharedFeatureStoreR31(MemoryBudgetR31 memory,long capacityBytes){if(capacityBytes<1)throw new IllegalArgumentException("R31_FEATURE_CAPACITY");this.memory=Objects.requireNonNull(memory);this.capacityBytes=capacityBytes;}
    public synchronized Reader acquire(Key key,String readerOwner){reapWorking();Objects.requireNonNull(key);validOwner(readerOwner);if(closed)return null;Entry e=entries.get(key);return e==null?null:pin(e,readerOwner);}
    private Reader pin(Entry entry,String owner){if(readers>=1024)return null;entry.readers++;readers++;return new Reader(this,owner,entry);}
    public Publication publishDoubles(String owner,Key key,double[] values){return publishDoubles(owner,key,values,()->false);}
    public Publication publishDoubles(String owner,Key key,double[] values,BooleanSupplier cancel){return publish(owner,key,values,Type.DOUBLE,values.length,cancel,null,null,false);}
    public Publication publishFloats(String owner,Key key,float[] values,BooleanSupplier cancel){return publish(owner,key,values,Type.FLOAT,values.length,cancel,null,null,false);}
    public Publication publishInts(String owner,Key key,int[] values,BooleanSupplier cancel){return publish(owner,key,values,Type.INT,values.length,cancel,null,null,false);}
    public Publication publishBytes(String owner,Key key,byte[] values,BooleanSupplier cancel){return publish(owner,key,values,Type.BYTE,values.length,cancel,null,null,false);}
    /** Compatibility for a trusted fresh private supplier. Its result is never pooled. */
    Publication computePrivateDoublesR31(String owner,Key key,int length,Supplier<double[]> producer,BooleanSupplier cancel){return computePrivateDoublesR31(owner,key,length,producer,cancel,false);}
    Publication computePrivateDoublesR31(String owner,Key key,int length,Supplier<double[]> producer,BooleanSupplier cancel,boolean critical){
        if(length<0)throw new IllegalArgumentException("R31_FEATURE_LENGTH");return publish(owner,key,null,Type.DOUBLE,length,cancel,Objects.requireNonNull(producer),null,critical);
    }
    /** Only built-in producers may fill this exclusive private target. Neither target nor aliases may escape. */
    Publication fillPrivateDoublesR31(String owner,Key key,int length,DoubleTargetProducerR31 producer,BooleanSupplier cancel,boolean critical){
        if(length<0)throw new IllegalArgumentException("R31_FEATURE_LENGTH");return publish(owner,key,null,Type.DOUBLE,length,cancel,null,Objects.requireNonNull(producer),critical);
    }
    private Publication existing(Entry entry,Type type,int length,String owner){
        if(entry.type!=type||entry.length!=length)throw new IllegalStateException("R31_IMMUTABLE_FEATURE_KEY_COLLISION");Reader reader=pin(entry,owner);return reader==null?denied(MemoryBudgetR31.WaitReason.OWNER_LIMIT):new Publication(reader,MemoryBudgetR31.WaitReason.NONE,false);
    }
    private Backing takeIdle(Type type,int length){Iterator<Backing> it=idle.iterator();while(it.hasNext()){Backing b=it.next();if(b.type==type&&b.length==length){it.remove();idleBytes-=b.bytes;backingReuses++;return b;}}return null;}
    private Backing recycleMatching(Type type,int length){
        Iterator<Map.Entry<Key,Entry>> it=entries.entrySet().iterator();while(it.hasNext()){Entry e=it.next().getValue();if(e.readers==0&&e.backing.reusable&&e.type==type&&e.length==length){it.remove();bytes-=e.bytes;e.values=null;recycledEntries++;backingReuses++;return e.backing;}}return null;
    }
    private Production reserve(Type type,int length,long needed,boolean reusable,boolean reuseOnly){
        reapWorking();if(productions.size()+working.size()>=1024)return null;
        Backing b=reusable?takeIdle(type,length):null;
        if(b==null&&reusable&&(reuseOnly||ownedBytes>capacityBytes-needed))b=recycleMatching(type,length);
        if(b==null){if(reuseOnly)return null;dropIdleUntil(capacityBytes-needed);Iterator<Map.Entry<Key,Entry>> it=entries.entrySet().iterator();while(ownedBytes>capacityBytes-needed&&it.hasNext()){Entry e=it.next().getValue();if(e.readers==0){it.remove();bytes-=e.bytes;e.values=null;drop(e.backing);}}if(ownedBytes>capacityBytes-needed)return null;ownedBytes+=needed;}
        Production production=new Production(needed,b);productions.add(production);inflightBytes+=needed;return production;
    }
    private void drop(Backing b){if(b.values==null)throw new IllegalStateException("R31_PRIVATE_BACKING_DOUBLE_RELEASE");b.values=null;ownedBytes-=b.bytes;declaredReleased+=b.bytes;b.lease.close();}
    private void dropIdleUntil(long target){while(ownedBytes>target&&!idle.isEmpty()){Backing b=idle.removeFirst();idleBytes-=b.bytes;drop(b);}}
    private void finish(Production production,boolean adopted){
        if(!production.active)return;production.active=false;productions.remove(production);inflightBytes-=production.bytes;
        if(adopted)return;
        Backing b=production.backing;
        if(b==null){ownedBytes-=production.bytes;if(production.lease!=null)production.lease.close();}
        else if(b.reusable&&!closed&&idle.size()<1024){idle.addLast(b);idleBytes+=b.bytes;}
        else drop(b);
    }
    private void reapWorking(){for(int i=0;i<1024;i++){WorkingReference ref=(WorkingReference)workingQueue.poll();if(ref==null)break;if(working.remove(ref)){workingBytes-=ref.bytes;inflightBytes-=ref.bytes;ownedBytes-=ref.bytes;declaredReleased+=ref.bytes;collectedWorkingOwners++;ref.lease.close();}}}
    private synchronized void releaseWorking(WorkingReference reference,Backing backing){
        if(!working.remove(reference))throw new IllegalStateException("R31_PRIVATE_WORKING_DOUBLE_RELEASE");reference.clear();workingBytes-=backing.bytes;inflightBytes-=backing.bytes;
        if(!closed&&idle.size()<1024){idle.addLast(backing);idleBytes+=backing.bytes;}else drop(backing);
    }
    PrivateWorkingR31 borrowPrivateWorkingR31(String owner,int length,BooleanSupplier cancel,boolean critical){
        validOwner(owner);if(length<0)throw new IllegalArgumentException("R31_FEATURE_LENGTH");long needed=272L+8L*length;Production production;
        synchronized(this){if(closed)throw new MemoryBudgetR31.CapacityWait(MemoryBudgetR31.WaitReason.CLOSED_LEASE);if(needed>capacityBytes)throw new MemoryBudgetR31.CapacityWait(MemoryBudgetR31.WaitReason.DECLARED_CAPACITY);production=reserve(Type.DOUBLE,length,needed,true,false);if(production==null)throw new MemoryBudgetR31.CapacityWait(MemoryBudgetR31.WaitReason.DECLARED_CAPACITY);}
        boolean allocated=false;try{
            if(production.backing==null){MemoryBudgetR31.Admission admission=memory.tryAcquire("private-shared-work:"+storeId,critical?MemoryBudgetR31.Kind.REALTIME:MemoryBudgetR31.Kind.FEATURE_CACHE,needed,cancel);if(!admission.acquired()){synchronized(this){finish(production,false);production=null;if(!closed&&admission.reason==MemoryBudgetR31.WaitReason.DECLARED_CAPACITY)production=reserve(Type.DOUBLE,length,needed,true,true);}if(production==null)throw new MemoryBudgetR31.CapacityWait(admission.reason);}else production.lease=admission.lease;}
            if(cancel!=null&&cancel.getAsBoolean())throw new MemoryBudgetR31.CapacityWait(MemoryBudgetR31.WaitReason.CANCELLED);
            if(production.backing==null){double[] values=new double[length];production.lease.allocatedPrimitiveArrayR31(values);production.lease.closeWhenPrivateBackingCollectedR31(values);synchronized(this){production.backing=new Backing(Type.DOUBLE,length,needed,++nextBackingId,values,production.lease,true);backingAllocations++;allocated=true;}}
            Arrays.fill((double[])production.backing.values,Double.NaN);
            synchronized(this){if(closed)throw new MemoryBudgetR31.CapacityWait(MemoryBudgetR31.WaitReason.CLOSED_LEASE);WorkingReference reference=new WorkingReference(production.backing,workingQueue);PrivateWorkingR31 result=new PrivateWorkingR31(this,production.backing,reference,allocated);working.add(reference);workingBytes+=needed;finish(production,true);inflightBytes+=needed;return result;}
        }finally{synchronized(this){if(production!=null)finish(production,false);}}
    }
    private Publication publish(String owner,Key key,Object source,Type type,int length,BooleanSupplier cancel,Supplier<double[]> privateProducer,DoubleTargetProducerR31 targetProducer,boolean critical){
        validOwner(owner);Objects.requireNonNull(key);long needed=272L+(long)length*type.width;boolean reusable=targetProducer!=null;Production production;
        synchronized(this){if(closed)return denied(MemoryBudgetR31.WaitReason.CLOSED_LEASE);Entry old=entries.get(key);if(old!=null)return existing(old,type,length,owner);if(needed>capacityBytes)return denied(MemoryBudgetR31.WaitReason.DECLARED_CAPACITY);production=reserve(type,length,needed,reusable,false);if(production==null)return denied(MemoryBudgetR31.WaitReason.DECLARED_CAPACITY);}
        boolean adopted=false;
        try{
            if(production.backing==null){MemoryBudgetR31.Admission admission=memory.tryAcquire(reusable?"private-feature-backing:"+storeId:owner,critical?MemoryBudgetR31.Kind.REALTIME:MemoryBudgetR31.Kind.FEATURE_CACHE,needed,cancel);
                if(!admission.acquired()){
                    synchronized(this){finish(production,false);production=null;if(!closed&&reusable&&admission.reason==MemoryBudgetR31.WaitReason.DECLARED_CAPACITY)production=reserve(type,length,needed,true,true);}
                    if(production==null)return denied(admission.reason);
                }else production.lease=admission.lease;
            }
            if(cancel!=null&&cancel.getAsBoolean())return denied(MemoryBudgetR31.WaitReason.CANCELLED);
            if(production.backing==null){Object values;
                if(reusable)values=new double[length];
                else if(privateProducer!=null){double[] result=Objects.requireNonNull(privateProducer.get());if(result.length!=length)throw new IllegalStateException("R31_LEAF_LENGTH");values=result;}
                else switch(type){case DOUBLE:values=((double[])source).clone();break;case FLOAT:values=((float[])source).clone();break;case INT:values=((int[])source).clone();break;default:values=((byte[])source).clone();}
                production.lease.allocatedPrimitiveArrayR31(values);production.lease.closeWhenPrivateBackingCollectedR31(values);
                synchronized(this){production.backing=new Backing(type,length,needed,++nextBackingId,values,production.lease,reusable);backingAllocations++;}
            }
            if(reusable){double[] target=(double[])production.backing.values;Arrays.fill(target,Double.NaN);targetProducer.fill(target);}
            if(cancel!=null&&cancel.getAsBoolean())return denied(MemoryBudgetR31.WaitReason.CANCELLED);
            synchronized(this){if(closed)return denied(MemoryBudgetR31.WaitReason.CLOSED_LEASE);Entry old=entries.get(key);if(old!=null)return existing(old,type,length,owner);if(entries.size()>=1024||readers>=1024)return denied(MemoryBudgetR31.WaitReason.OWNER_LIMIT);Entry entry=new Entry(key,++nextId,production.backing);finish(production,true);entries.put(key,entry);bytes+=needed;adopted=true;return new Publication(pin(entry,owner),MemoryBudgetR31.WaitReason.NONE,true);}
        }finally{synchronized(this){if(production!=null)finish(production,adopted);}}
    }
    private static void validOwner(String owner){if(owner==null||owner.isEmpty()||owner.length()>192)throw new IllegalArgumentException("R31_FEATURE_OWNER");}
    private static Publication denied(MemoryBudgetR31.WaitReason reason){return new Publication(null,reason,false);}
    private synchronized void release(Entry entry){if(entry.readers<1)throw new IllegalStateException("R31_FEATURE_REFERENCE_ACCOUNTING");entry.readers--;readers--;if(closed&&entry.readers==0)remove(entry,false);}
    private void remove(Entry entry,boolean recycle){if(entries.remove(entry.key,entry)){bytes-=entry.bytes;entry.values=null;if(recycle&&entry.backing.reusable&&!closed&&idle.size()<1024){idle.addLast(entry.backing);idleBytes+=entry.bytes;recycledEntries++;}else drop(entry.backing);}}
    /** Below high-water, recycle private backing without claiming any heap was freed. */
    public synchronized Map<String,Object> recycleUnpinnedR31(){reapWorking();for(Entry e:new ArrayList<>(entries.values()))if(e.readers==0)remove(e,true);Map<String,Object> out=new LinkedHashMap<>();out.put("idlePoolBytes",idleBytes);out.put("retainedBytes",ownedBytes);out.put("actualHeapReleasedBytes",null);out.put("releaseEvidence","PRIVATE_BACKING_RETAINED_FOR_RECOMPUTATION_NOT_HEAP_FREE");return out;}
    public synchronized Map<String,Object> evictTo(long targetBytes){
        reapWorking();if(targetBytes<0)throw new IllegalArgumentException("R31_FEATURE_EVICTION_TARGET");long before=ownedBytes;dropIdleUntil(targetBytes);for(Entry e:new ArrayList<>(entries.values()))if(ownedBytes>targetBytes&&e.readers==0)remove(e,false);Map<String,Object> out=new LinkedHashMap<>();out.put("declaredBytesReleased",before-ownedBytes);out.put("retainedBytes",ownedBytes);out.put("targetReached",ownedBytes<=targetBytes);out.put("releaseEvidence","REFERENCES_DROPPED_AND_DECLARATION_RELEASED; ACTUAL_HEAP_REQUIRES_GC_OBSERVATION");return out;
    }
    public synchronized Map<String,Object> snapshot(){reapWorking();Map<String,Object> out=new LinkedHashMap<>();long pinned=0;for(Entry e:entries.values())if(e.readers>0)pinned+=e.bytes;out.put("entries",entries.size());out.put("readerLeases",readers);out.put("declaredBytes",ownedBytes);out.put("entryBytes",bytes);out.put("idlePoolBytes",idleBytes);out.put("inflightBytes",inflightBytes);out.put("workingBytesIncludedInInflight",workingBytes);out.put("workingLoans",working.size());out.put("workingOwnersCollected",collectedWorkingOwners);out.put("activeProductions",productions.size());out.put("idleBackings",idle.size());out.put("pinnedBytes",pinned);out.put("capacityBytes",capacityBytes);out.put("declaredBytesReleased",declaredReleased);out.put("backingAllocations",backingAllocations);out.put("backingReuses",backingReuses);out.put("recycledEntries",recycledEntries);out.put("scope","PER_PID_IMMUTABLE_PRIMITIVE_BLOCKS_AND_PRIVATE_REUSE");out.put("actualHeapReleasedBytes",null);out.put("closed",closed);return out;}
    @Override public synchronized void close(){closed=true;evictTo(0);}
}
