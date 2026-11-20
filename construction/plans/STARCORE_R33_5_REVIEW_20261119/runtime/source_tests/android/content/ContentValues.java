package android.content;

import java.util.HashMap;

/** Host recording surface for the exact ContentValues overloads used by the writer. */
public final class ContentValues extends HashMap<String,Object> {
    public void put(String key, String value) { super.put(key,value); }
    public void put(String key, Integer value) { super.put(key,value); }
    public void put(String key, Long value) { super.put(key,value); }
    public void put(String key, Double value) { super.put(key,value); }
}
