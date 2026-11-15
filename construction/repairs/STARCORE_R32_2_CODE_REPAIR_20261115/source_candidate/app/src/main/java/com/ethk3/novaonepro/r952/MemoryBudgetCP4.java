package com.ethk3.novaonepro.r952;
import java.util.*;
/** Conservative bytes include object/array overhead; reserve before allocation, evict only unpinned immutable values. */
public final class MemoryBudgetCP4 {
 public final long limit;private long used;
 public MemoryBudgetCP4(long limit){if(limit<0)throw new IllegalArgumentException("MEMORY_LIMIT");this.limit=limit;}
 public synchronized boolean reserve(long bytes){if(bytes<0||bytes>limit-used)return false;used+=bytes;return true;}
 public synchronized void release(long bytes){if(bytes<0||bytes>used)throw new IllegalStateException("MEMORY_ACCOUNTING");used-=bytes;}
 public synchronized long used(){return used;}
 /** Reserve before allocation; the returned token owns exactly one release. */
 public Lease acquire(long bytes){if(bytes<0)throw new IllegalArgumentException("MEMORY_RESERVATION");if(!reserve(bytes))throw new IllegalStateException("ALGORITHM_MEMORY_BUDGET");return new Lease(this,bytes);}
 public static final class Lease implements AutoCloseable {
  private MemoryBudgetCP4 owner;public final long bytes;
  private Lease(MemoryBudgetCP4 owner,long bytes){this.owner=owner;this.bytes=bytes;}
  public synchronized void close(){if(owner!=null){owner.release(bytes);owner=null;}}
 }

 public static long algorithmTotal(long maxMemory){return Math.min(16L<<20,maxMemory/16);}
 public static final class Cache<K,V> {
  private static final class Entry<V>{final V value;final long bytes;int pins;Entry(V v,long b){value=v;bytes=b;}}
  private final MemoryBudgetCP4 budget;private final LinkedHashMap<K,Entry<V>> entries=new LinkedHashMap<>(16,.75f,true);
  public Cache(MemoryBudgetCP4 budget){this.budget=budget;}
  public synchronized V pin(K key){Entry<V>e=entries.get(key);if(e==null)return null;e.pins++;return e.value;}
  public synchronized void unpin(K key){Entry<V>e=entries.get(key);if(e==null||e.pins<1)throw new IllegalStateException("CACHE_PIN");e.pins--;}
  public synchronized boolean put(K key,V value,long bytes){if(entries.containsKey(key))throw new IllegalArgumentException("IMMUTABLE_CACHE_KEY_REUSE");if(bytes<64||bytes>budget.limit)return false;
   if(!budget.reserve(bytes)){trimTo(Math.max(0,budget.limit-bytes));if(!budget.reserve(bytes))return false;}
   entries.put(key,new Entry<>(value,bytes));return true;}
  public synchronized long trimTo(long target){Iterator<Map.Entry<K,Entry<V>>>it=entries.entrySet().iterator();while(budget.used()>target&&it.hasNext()){Entry<V>e=it.next().getValue();if(e.pins==0){it.remove();budget.release(e.bytes);}}return budget.used();}
  public synchronized int size(){return entries.size();}
 }
}
