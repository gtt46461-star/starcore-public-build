package com.ethk3.novaonepro;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.util.*;

/** One fresh catalogue read for exact DDL checks; never caches schema readiness. */
public final class SchemaObjectSnapshotR26 {
    private final Map<String,String> first=new HashMap<>();
    private final Set<String> repeated=new HashSet<>();
    private SchemaObjectSnapshotR26(){}
    private void add(String name,String sql){if(first.containsKey(name))repeated.add(name);else first.put(name,sql);}
    public boolean contains(String name){return first.containsKey(name);}
    public String firstSql(String name){return first.get(name);}
    public boolean matches(Map<String,String> expected){
        for(Map.Entry<String,String> entry:expected.entrySet())if(!contains(entry.getKey())||repeated.contains(entry.getKey())||!entry.getValue().equals(firstSql(entry.getKey())))return false;
        return true;
    }
    public static SchemaObjectSnapshotR26 read(SQLiteDatabase db,Collection<String> names){
        if(db==null||names==null)throw new IllegalArgumentException("R26_SCHEMA_CENSUS_INPUT");
        names=new LinkedHashSet<>(names);
        SchemaObjectSnapshotR26 result=new SchemaObjectSnapshotR26();
        if(names.isEmpty())return result;
        if(names.size()>499){
            // Keep the original bound equality for oversized contracts.
            for(String name:names)try(Cursor c=db.rawQuery("SELECT sql FROM sqlite_master WHERE name=?",new String[]{name})){while(c.moveToNext())result.add(name,c.getString(0));}
            return result;
        }
        StringBuilder marks=new StringBuilder();for(int i=0;i<names.size();i++){if(i>0)marks.append(',');marks.append('?');}
        try(Cursor c=db.rawQuery("SELECT name,sql FROM sqlite_master WHERE name IN ("+marks+")",names.toArray(new String[0]))){while(c.moveToNext())result.add(c.getString(0),c.getString(1));}
        return result;
    }
}
