package com.ethk3.novaonepro.strategyv2;

import java.util.*;
import java.util.function.BooleanSupplier;

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
    private static final class Entry {final Key key;final Type type;final int length;final long bytes,id;final MemoryBudgetR31.Lease lease;Object values;int readers;Entry(Key key,Type type,int length,long bytes,long id,Object values,MemoryBudgetR31.Lease lease){this.key=key;this.type=type;this.length=length;this.bytes=bytes;this.id=id;this.values=values;this.lease=lease;}}
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
    private long bytes,nextId,declaredReleased;private int readers;private boolean closed;
    public SharedFeatureStoreR31(MemoryBudgetR31 memory,long capacityBytes){if(capacityBytes<1)throw new IllegalArgumentException("R31_FEATURE_CAPACITY");this.memory=Objects.requireNonNull(memory);this.capacityBytes=capacityBytes;}
    public synchronized Reader acquire(Key key,String readerOwner){Objects.requireNonNull(key);validOwner(readerOwner);if(closed)return null;Entry e=entries.get(key);return e==null?null:pin(e,readerOwner);}
    private Reader pin(Entry entry,String owner){if(readers>=1024)return null;entry.readers++;readers++;return new Reader(this,owner,entry);}
    public Publication publishDoubles(String owner,Key key,double[] values){return publishDoubles(owner,key,values,()->false);}
    public Publication publishDoubles(String owner,Key key,double[] values,BooleanSupplier cancel){return publish(owner,key,values,Type.DOUBLE,values.length,cancel);}
    public Publication publishFloats(String owner,Key key,float[] values,BooleanSupplier cancel){return publish(owner,key,values,Type.FLOAT,values.length,cancel);}
    public Publication publishInts(String owner,Key key,int[] values,BooleanSupplier cancel){return publish(owner,key,values,Type.INT,values.length,cancel);}
    public Publication publishBytes(String owner,Key key,byte[] values,BooleanSupplier cancel){return publish(owner,key,values,Type.BYTE,values.length,cancel);}
    private Publication publish(String owner,Key key,Object source,Type type,int length,BooleanSupplier cancel){
        validOwner(owner);Objects.requireNonNull(key);long needed=272L+(long)length*type.width;
        synchronized(this){if(closed)return denied(MemoryBudgetR31.WaitReason.CLOSED_LEASE);Entry existing=entries.get(key);if(existing!=null){if(existing.type!=type||existing.length!=length)throw new IllegalStateException("R31_IMMUTABLE_FEATURE_KEY_COLLISION");Reader reader=pin(existing,owner);return reader==null?denied(MemoryBudgetR31.WaitReason.OWNER_LIMIT):new Publication(reader,MemoryBudgetR31.WaitReason.NONE,false);}if(needed>capacityBytes)return denied(MemoryBudgetR31.WaitReason.DECLARED_CAPACITY);evictLocked(capacityBytes-needed);}
        MemoryBudgetR31.Admission admission=memory.tryAcquire(owner,MemoryBudgetR31.Kind.FEATURE_CACHE,needed,cancel);if(!admission.acquired())return denied(admission.reason);
        Object immutable;
        try{switch(type){case DOUBLE:immutable=((double[])source).clone();break;case FLOAT:immutable=((float[])source).clone();break;case INT:immutable=((int[])source).clone();break;default:immutable=((byte[])source).clone();}}
        catch(RuntimeException|Error failure){admission.lease.close();throw failure;}
        synchronized(this){
            if(closed){admission.lease.close();return denied(MemoryBudgetR31.WaitReason.CLOSED_LEASE);}
            Entry existing=entries.get(key);if(existing!=null){admission.lease.close();if(existing.type!=type||existing.length!=length)throw new IllegalStateException("R31_IMMUTABLE_FEATURE_KEY_COLLISION");Reader reader=pin(existing,owner);return reader==null?denied(MemoryBudgetR31.WaitReason.OWNER_LIMIT):new Publication(reader,MemoryBudgetR31.WaitReason.NONE,false);}
            evictLocked(capacityBytes-needed);if(bytes>capacityBytes-needed||entries.size()>=1024||readers>=1024){admission.lease.close();return denied(MemoryBudgetR31.WaitReason.DECLARED_CAPACITY);}
            Entry entry=new Entry(key,type,length,needed,++nextId,immutable,admission.lease);entries.put(key,entry);bytes+=needed;return new Publication(pin(entry,owner),MemoryBudgetR31.WaitReason.NONE,true);
        }
    }
    private static void validOwner(String owner){if(owner==null||owner.isEmpty()||owner.length()>192)throw new IllegalArgumentException("R31_FEATURE_OWNER");}
    private static Publication denied(MemoryBudgetR31.WaitReason reason){return new Publication(null,reason,false);}
    private synchronized void release(Entry entry){if(entry.readers<1)throw new IllegalStateException("R31_FEATURE_REFERENCE_ACCOUNTING");entry.readers--;readers--;if(closed&&entry.readers==0)remove(entry);}
    private void remove(Entry entry){if(entries.remove(entry.key,entry)){bytes-=entry.bytes;declaredReleased+=entry.bytes;entry.values=null;entry.lease.close();}}
    private void evictLocked(long target){Iterator<Map.Entry<Key,Entry>> iterator=entries.entrySet().iterator();while(bytes>target&&iterator.hasNext()){Entry e=iterator.next().getValue();if(e.readers==0){iterator.remove();bytes-=e.bytes;declaredReleased+=e.bytes;e.values=null;e.lease.close();}}}
    public synchronized Map<String,Object> evictTo(long targetBytes){if(targetBytes<0)throw new IllegalArgumentException("R31_FEATURE_EVICTION_TARGET");long before=bytes;evictLocked(targetBytes);Map<String,Object> out=new LinkedHashMap<>();out.put("declaredBytesReleased",before-bytes);out.put("retainedBytes",bytes);out.put("targetReached",bytes<=targetBytes);out.put("releaseEvidence","REFERENCES_DROPPED_AND_DECLARATION_RELEASED; ACTUAL_HEAP_REQUIRES_GC_OBSERVATION");return out;}
    public synchronized Map<String,Object> snapshot(){Map<String,Object> out=new LinkedHashMap<>();long pinned=0;for(Entry e:entries.values())if(e.readers>0)pinned+=e.bytes;out.put("entries",entries.size());out.put("readerLeases",readers);out.put("declaredBytes",bytes);out.put("pinnedBytes",pinned);out.put("capacityBytes",capacityBytes);out.put("declaredBytesReleased",declaredReleased);out.put("scope","PER_PID_IMMUTABLE_PRIMITIVE_BLOCKS");out.put("actualHeapReleasedBytes",null);out.put("closed",closed);return out;}
    @Override public synchronized void close(){closed=true;evictLocked(0);}
}
