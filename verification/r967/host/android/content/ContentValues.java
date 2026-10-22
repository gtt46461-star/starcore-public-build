package android.content;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/** Host-test adapter only. Mirrors the small Android ContentValues surface used by production SQL. */
public final class ContentValues {
    private final LinkedHashMap<String,Object> values;

    public ContentValues(){ values=new LinkedHashMap<String,Object>(); }
    public ContentValues(int size){ values=new LinkedHashMap<String,Object>(Math.max(0,size)); }
    public ContentValues(ContentValues from){
        values=new LinkedHashMap<String,Object>();
        if(from!=null) values.putAll(from.values);
    }

    public void put(String key,String value){ values.put(key,value); }
    public void put(String key,Byte value){ values.put(key,value); }
    public void put(String key,Short value){ values.put(key,value); }
    public void put(String key,Integer value){ values.put(key,value); }
    public void put(String key,Long value){ values.put(key,value); }
    public void put(String key,Float value){ values.put(key,value); }
    public void put(String key,Double value){ values.put(key,value); }
    public void put(String key,Boolean value){ values.put(key,value==null?null:(value.booleanValue()?Integer.valueOf(1):Integer.valueOf(0))); }
    public void put(String key,byte[] value){ values.put(key,value); }
    public void putNull(String key){ values.put(key,null); }
    public void putAll(ContentValues other){ if(other!=null) values.putAll(other.values); }

    public Object get(String key){ return values.get(key); }
    public String getAsString(String key){ Object v=values.get(key); return v==null?null:String.valueOf(v); }
    public Long getAsLong(String key){ Number n=number(key); return n==null?null:Long.valueOf(n.longValue()); }
    public Integer getAsInteger(String key){ Number n=number(key); return n==null?null:Integer.valueOf(n.intValue()); }
    public Short getAsShort(String key){ Number n=number(key); return n==null?null:Short.valueOf(n.shortValue()); }
    public Byte getAsByte(String key){ Number n=number(key); return n==null?null:Byte.valueOf(n.byteValue()); }
    public Double getAsDouble(String key){ Number n=number(key); return n==null?null:Double.valueOf(n.doubleValue()); }
    public Float getAsFloat(String key){ Number n=number(key); return n==null?null:Float.valueOf(n.floatValue()); }
    public Boolean getAsBoolean(String key){
        Object v=values.get(key); if(v==null)return null;
        if(v instanceof Boolean)return (Boolean)v;
        if(v instanceof Number)return ((Number)v).intValue()!=0;
        String s=String.valueOf(v); return "1".equals(s)||"true".equalsIgnoreCase(s);
    }
    public byte[] getAsByteArray(String key){ Object v=values.get(key); return v instanceof byte[]?(byte[])v:null; }

    private Number number(String key){
        Object v=values.get(key); if(v==null)return null;
        if(v instanceof Number)return (Number)v;
        try{return Double.valueOf(String.valueOf(v));}catch(NumberFormatException e){return null;}
    }

    public boolean containsKey(String key){ return values.containsKey(key); }
    public int size(){ return values.size(); }
    public void clear(){ values.clear(); }
    public void remove(String key){ values.remove(key); }
    public Set<Map.Entry<String,Object>> valueSet(){ return values.entrySet(); }
    public Set<String> keySet(){ return values.keySet(); }
    @Override public boolean equals(Object o){ return o instanceof ContentValues && values.equals(((ContentValues)o).values); }
    @Override public int hashCode(){ return values.hashCode(); }
    @Override public String toString(){ return values.toString(); }
}
