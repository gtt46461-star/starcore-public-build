package android.content;

import java.util.Map;
import java.util.Set;

/**
 * Compile-time API-28 surface only.  Method descriptors intentionally match
 * android.content.ContentValues; this class must never be packaged in the APK.
 */
public final class ContentValues {
    public ContentValues() {}
    public ContentValues(int size) {}
    public ContentValues(ContentValues from) {}

    public void put(String key, String value) {}
    public void put(String key, Byte value) {}
    public void put(String key, Short value) {}
    public void put(String key, Integer value) {}
    public void put(String key, Long value) {}
    public void put(String key, Float value) {}
    public void put(String key, Double value) {}
    public void put(String key, Boolean value) {}
    public void put(String key, byte[] value) {}
    public void putNull(String key) {}

    public Object get(String key) { return null; }
    public String getAsString(String key) { return null; }
    public Long getAsLong(String key) { return null; }
    public Integer getAsInteger(String key) { return null; }
    public Short getAsShort(String key) { return null; }
    public Byte getAsByte(String key) { return null; }
    public Double getAsDouble(String key) { return null; }
    public Float getAsFloat(String key) { return null; }
    public Boolean getAsBoolean(String key) { return null; }
    public byte[] getAsByteArray(String key) { return null; }
    public boolean containsKey(String key) { return false; }
    public int size() { return 0; }
    public void remove(String key) {}
    public void clear() {}
    public Set<Map.Entry<String, Object>> valueSet() { return null; }
}
