package android.content;
import java.util.*;
/** Host adapter for SQLite bind values only, never packaged in an APK. */
public final class ContentValues {
    private final LinkedHashMap<String,Object> values=new LinkedHashMap<>();
    public void put(String key,String value){values.put(key,value);}public void put(String key,Integer value){values.put(key,value);}public void put(String key,Long value){values.put(key,value);}public void put(String key,Double value){values.put(key,value);}public void put(String key,Float value){values.put(key,value);}public void put(String key,Boolean value){values.put(key,value==null?null:value?1:0);}public void put(String key,byte[] value){values.put(key,value);}public void putNull(String key){values.put(key,null);}
    public Set<Map.Entry<String,Object>> valueSet(){return values.entrySet();}public int size(){return values.size();}public Object get(String key){return values.get(key);}
}
