package android.content;
import java.util.*;
/** Functional HOST-only ContentValues. Never packaged into APK. */
public final class ContentValues {
 private final Map<String,Object> values=new LinkedHashMap<>();
 public ContentValues(){} public ContentValues(int size){} public ContentValues(ContentValues other){values.putAll(other.values);}
 public void put(String k,String v){values.put(k,v);} public void put(String k,Byte v){values.put(k,v);} public void put(String k,Short v){values.put(k,v);} public void put(String k,Integer v){values.put(k,v);} public void put(String k,Long v){values.put(k,v);} public void put(String k,Float v){values.put(k,v);} public void put(String k,Double v){values.put(k,v);} public void put(String k,Boolean v){values.put(k,v);} public void put(String k,byte[]v){values.put(k,v);} public void putNull(String k){values.put(k,null);} public void putAll(ContentValues v){values.putAll(v.values);}
 public Object get(String k){return values.get(k);} public String getAsString(String k){Object v=get(k);return v==null?null:v.toString();} public Long getAsLong(String k){Object v=get(k);return v==null?null:v instanceof Number?((Number)v).longValue():Long.valueOf(v.toString());} public Integer getAsInteger(String k){Long v=getAsLong(k);return v==null?null:v.intValue();} public Double getAsDouble(String k){Object v=get(k);return v==null?null:v instanceof Number?((Number)v).doubleValue():Double.valueOf(v.toString());} public byte[]getAsByteArray(String k){return (byte[])get(k);}
 public Set<Map.Entry<String,Object>> valueSet(){return values.entrySet();} public Set<String>keySet(){return values.keySet();} public int size(){return values.size();} public boolean containsKey(String k){return values.containsKey(k);} public void clear(){values.clear();} public void remove(String k){values.remove(k);}
}
