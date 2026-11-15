package android.content;
import java.util.*;
/** HOST platform adapter, never app source. */
public final class ContentValues {
    private final LinkedHashMap<String,Object> values=new LinkedHashMap<>();
    public ContentValues(){}public ContentValues(int size){}public ContentValues(ContentValues other){values.putAll(other.values);}
    public void put(String key,String value){values.put(key,value);}public void put(String key,Integer value){values.put(key,value);}public void put(String key,Long value){values.put(key,value);}public void put(String key,Double value){values.put(key,value);}public void put(String key,Float value){values.put(key,value);}public void put(String key,Boolean value){values.put(key,value);}public void put(String key,byte[] value){values.put(key,value);}public void putNull(String key){values.put(key,null);}
    public void putAll(ContentValues other){values.putAll(other.values);}public Object get(String key){return values.get(key);}public String getAsString(String key){Object value=get(key);return value==null?null:String.valueOf(value);}public Integer getAsInteger(String key){Object value=get(key);return value==null?null:((Number)value).intValue();}public Long getAsLong(String key){Object value=get(key);return value==null?null:((Number)value).longValue();}public Set<Map.Entry<String,Object>> valueSet(){return values.entrySet();}public int size(){return values.size();}public void clear(){values.clear();}public void remove(String key){values.remove(key);}public boolean containsKey(String key){return values.containsKey(key);}
}
