package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Stable migration cursors for ordinary and WITHOUT ROWID tables.
 * The latter use an integer rank of their complete primary key, never a truncated key or a hash.
 * SQLite 3.25+ uses ROW_NUMBER; the correlated fallback works on the oldest supported Android SQLite.
 * The fallback can be quadratic and is only for a quiescent, budgeted migration snapshot.
 * This is not a live query or a training operation. */
public final class ResearchRowsR29 {
    private ResearchRowsR29(){}
    static final int CHUNK_BYTES=128<<10;
    private static final Map<SQLiteDatabase,Charset> ENCODINGS=java.util.Collections.synchronizedMap(new java.util.WeakHashMap<>());

    /** Bound the entire small-cell page below CursorWindow limits. Larger cells retain chunked reads.
     * Inline bytes are BLOBs even for TEXT, preserving strict source-encoding validation and embedded NUL. */
    static List<MigrationCopyR29.Row> readPage(SQLiteDatabase db,MigrationCopyR29.Table table,Long after,String predicate,int limit,long pageBytes,long sliceNanos,Runnable check){
        int width=Math.max(1,table.columns.size()),rowsPerPage=Math.min(limit,Math.max(1,(256<<10)/(width*64)));
        int inlineBytes=Math.min(512,Math.max(1,(512<<10)/(width*rowsPerPage)));
        List<Integer> primary=withoutRowid(table)?primaryIndices(db,table):java.util.Collections.emptyList();
        String from=cursorSource(db,table);StringBuilder select=new StringBuilder("rowid");
        for(String column:table.columns){String q=MigrationCopyR29.id(column);select.append(",typeof(").append(q).append("),CASE WHEN typeof(").append(q).append(") IN ('text','blob') THEN length(CAST(").append(q).append(" AS BLOB)) ELSE 0 END,CASE WHEN typeof(").append(q).append(") IN ('integer','real') THEN ").append(q).append(" ELSE NULL END,CASE WHEN typeof(").append(q).append(") IN ('text','blob') AND length(CAST(").append(q).append(" AS BLOB))<=").append(inlineBytes).append(" THEN CAST(").append(q).append(" AS BLOB) ELSE NULL END");}
        String where=" WHERE ("+predicate+")"+(after==null?"":" AND rowid>?");String[] args=after==null?null:new String[]{String.valueOf(after)};
        List<MigrationCopyR29.Row> rows=new ArrayList<>();long started=System.nanoTime(),bytes=0;
        try(Cursor metadata=db.rawQuery("SELECT "+select+" FROM "+from+where+" ORDER BY rowid LIMIT "+rowsPerPage,args)){
            while(metadata.moveToNext()){
                check.run();long rowid=metadata.getLong(0);byte[] payload=encode(db,table,from,rowid,metadata,inlineBytes,primary,check);rows.add(new MigrationCopyR29.Row(rowid,payload,StatsR27.sha256(payload)));bytes+=payload.length;
                if(bytes>=pageBytes||System.nanoTime()-started>=sliceNanos)break;
            }
        }
        return rows;
    }

    private static final class Lookup{final String from,where;final String[] args;Lookup(String from,String where,String[] args){this.from=from;this.where=where;this.args=args;}}
    private static List<Integer> primaryIndices(SQLiteDatabase db,MigrationCopyR29.Table table){
        Map<Integer,Integer> order=new TreeMap<>();try(Cursor c=db.rawQuery("PRAGMA table_info("+MigrationCopyR29.id(table.name)+")",null)){while(c.moveToNext())if(c.getInt(5)>0)order.put(c.getInt(5),table.columns.indexOf(c.getString(1)));}return new ArrayList<>(order.values());
    }
    private static byte[] utf8(SQLiteDatabase db,byte[] source){try{return textEncoding(db).newDecoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(source)).toString().getBytes(StandardCharsets.UTF_8);}catch(CharacterCodingException malformed){throw new IllegalStateException("R29_MIGRATION_TEXT_ENCODING",malformed);}}
    private static Lookup lookup(SQLiteDatabase db,MigrationCopyR29.Table table,String ranked,long rowid,Cursor metadata,List<Integer> primary){
        if(primary.isEmpty())return new Lookup(ranked,"rowid=?",new String[]{String.valueOf(rowid)});
        StringBuilder where=new StringBuilder();List<String> args=new ArrayList<>();
        for(int column:primary){int at=1+4*column;String type=metadata.getString(at),name=MigrationCopyR29.id(table.columns.get(column));if(where.length()>0)where.append(" AND ");
            if(type.equals("integer")){where.append(name).append("=CAST(? AS INTEGER)");args.add(String.valueOf(metadata.getLong(at+2)));}
            else if(type.equals("real")){where.append(name).append("=CAST(? AS REAL)");args.add(String.valueOf(metadata.getDouble(at+2)));}
            else if((type.equals("text")||type.equals("blob"))&&!metadata.isNull(at+3)){
                byte[] value=metadata.getBlob(at+3);
                if(type.equals("text")){where.append(name).append("=?");args.add(new String(utf8(db,value),StandardCharsets.UTF_8));}
                else{where.append(name).append("=X'");for(byte b:value)where.append("0123456789ABCDEF".charAt((b>>>4)&15)).append("0123456789ABCDEF".charAt(b&15));where.append('\'');}
            }else return new Lookup(ranked,"rowid=?",new String[]{String.valueOf(rowid)});
        }
        return new Lookup(MigrationCopyR29.id(table.name),where.toString(),args.toArray(new String[0]));
    }
    private static byte[] encode(SQLiteDatabase db,MigrationCopyR29.Table table,String from,long rowid,Cursor metadata,int inlineBytes,List<Integer> primary,Runnable check){
        try{
            ByteArrayOutputStream bytes=new ByteArrayOutputStream();DataOutputStream out=new DataOutputStream(bytes);out.writeInt(0x29DB0001);out.writeInt(table.columns.size());
            Lookup row=lookup(db,table,from,rowid,metadata,primary);
            for(int i=0;i<table.columns.size();i++){
                check.run();int index=1+4*i;String kind=metadata.getString(index);int type;
                if("null".equals(kind))type=Cursor.FIELD_TYPE_NULL;else if("integer".equals(kind))type=Cursor.FIELD_TYPE_INTEGER;else if("real".equals(kind))type=Cursor.FIELD_TYPE_FLOAT;else if("text".equals(kind))type=Cursor.FIELD_TYPE_STRING;else if("blob".equals(kind))type=Cursor.FIELD_TYPE_BLOB;else throw new IllegalStateException("R29_SQLITE_VALUE_TYPE");
                out.writeByte(type);
                if(type==Cursor.FIELD_TYPE_INTEGER)out.writeLong(metadata.getLong(index+2));
                else if(type==Cursor.FIELD_TYPE_FLOAT)out.writeLong(Double.doubleToLongBits(metadata.getDouble(index+2)));
                else if(type==Cursor.FIELD_TYPE_STRING||type==Cursor.FIELD_TYPE_BLOB){
                    long length=metadata.getLong(index+1),maximum=type==Cursor.FIELD_TYPE_STRING?2L*MigrationCopyR29.MAX_ROW_BYTES:MigrationCopyR29.MAX_ROW_BYTES;
                    if(length<0||length>maximum)throw new IllegalStateException("R29_MIGRATION_ROW_BUDGET");
                    byte[] cell;
                    if(length<=inlineBytes){if(metadata.isNull(index+3))throw new IllegalStateException("R29_MIGRATION_INLINE_CELL_MISSING");cell=metadata.getBlob(index+3);if(cell.length!=length)throw new IllegalStateException("R29_MIGRATION_INLINE_CELL_LENGTH");}
                    else cell=cell(db,row.from,table.columns.get(i),row.where,row.args,(int)length,kind,maximum,check);
                    if(type==Cursor.FIELD_TYPE_STRING)cell=utf8(db,cell);
                    MigrationCopyR29.blob(out,cell);
                }
                if(bytes.size()>MigrationCopyR29.MAX_ROW_BYTES)throw new IllegalStateException("R29_MIGRATION_ROW_BUDGET");
            }
            out.close();return bytes.toByteArray();
        }catch(IOException impossible){throw new IllegalStateException("R29_TYPED_ROW_ENCODE",impossible);}
    }

    /** Independent destination rehash uses the same bounded raw-cell path, without wrapping its encoded bytes. */
    static byte[] readBlob(SQLiteDatabase db,String table,String column,String where,String[] args,int length,Runnable check){return cell(db,MigrationCopyR29.id(table),column,where,args,length,"blob",MigrationCopyR29.MAX_ROW_BYTES,check);}

    private static byte[] cell(SQLiteDatabase db,String from,String column,String where,String[] args,int length,String kind,long maximum,Runnable check){
        if(length<0||length>maximum)throw new IllegalStateException("R29_MIGRATION_ROW_BUDGET");byte[] bytes=new byte[length];String name=MigrationCopyR29.id(column);
        for(int offset=0;offset<length;){
            check.run();int size=Math.min(CHUNK_BYTES,length-offset),n=args==null?0:args.length;String[] parameters=new String[n+4];parameters[0]=String.valueOf(offset+1);parameters[1]=String.valueOf(size);if(n>0)System.arraycopy(args,0,parameters,2,n);parameters[n+2]=String.valueOf(length);parameters[n+3]=kind;
            try(Cursor chunk=db.rawQuery("SELECT substr(CAST("+name+" AS BLOB),?,?) FROM "+from+" WHERE ("+where+") AND length(CAST("+name+" AS BLOB))=CAST(? AS INTEGER) AND typeof("+name+")=?",parameters)){
                if(!chunk.moveToFirst()||chunk.isNull(0))throw new IllegalStateException("R29_MIGRATION_CELL_CHANGED_OR_MISSING");byte[] value=chunk.getBlob(0);if(value.length!=size||chunk.moveToNext())throw new IllegalStateException("R29_MIGRATION_CELL_CHUNK_LENGTH_OR_IDENTITY");System.arraycopy(value,0,bytes,offset,size);
            }
            offset+=size;
        }
        return bytes;
    }

    private static Charset textEncoding(SQLiteDatabase db){Charset known=ENCODINGS.get(db);if(known!=null)return known;try(Cursor encoding=db.rawQuery("PRAGMA encoding",null)){if(!encoding.moveToFirst())throw new IllegalStateException("R29_SQLITE_TEXT_ENCODING_UNKNOWN");String name=encoding.getString(0);Charset value;if("UTF-8".equalsIgnoreCase(name))value=StandardCharsets.UTF_8;else if("UTF-16le".equalsIgnoreCase(name))value=StandardCharsets.UTF_16LE;else if("UTF-16be".equalsIgnoreCase(name))value=StandardCharsets.UTF_16BE;else throw new IllegalStateException("R29_SQLITE_TEXT_ENCODING_UNSUPPORTED:"+name);ENCODINGS.put(db,value);return value;}}

    static boolean withoutRowid(MigrationCopyR29.Table table){
        // Ignore literals, quoted identifiers and comments so DEFAULT 'WITHOUT ROWID' is not an option.
        String sql=unquoted(table.createSql);int end=sql.lastIndexOf(')');
        return end>=0&&java.util.regex.Pattern.compile("(?i)\\bWITHOUT\\s+ROWID\\b").matcher(sql.substring(end+1)).find();
    }

    static String cursorSource(SQLiteDatabase database,MigrationCopyR29.Table table){
        boolean windows=false;
        if(withoutRowid(table))try(Cursor version=database.rawQuery("SELECT sqlite_version()",null)){if(version.moveToFirst())windows=windowFunctionsAvailable(version.getString(0));}
        return cursorSource(database,table,windows);
    }

    /** Portable fallback is separately exercised even when the HOST SQLite supports windows. */
    static String cursorSourcePortable(SQLiteDatabase database,MigrationCopyR29.Table table){return cursorSource(database,table,false);}

    static boolean windowFunctionsAvailable(String version){
        if(version==null)return false;String[] parts=version.split("\\.");if(parts.length<2)return false;
        try{int major=Integer.parseInt(parts[0]),minor=Integer.parseInt(parts[1]);return major>3||major==3&&minor>=25;}catch(NumberFormatException malformed){return false;}
    }

    private static String cursorSource(SQLiteDatabase database,MigrationCopyR29.Table table,boolean windows){
        String name=MigrationCopyR29.id(table.name);
        if(!withoutRowid(table))return name;
        for(String column:table.columns)if("rowid".equalsIgnoreCase(column))throw new IllegalStateException("R29_VIRTUAL_ROWID_COLUMN_CONFLICT:"+table.name);
        Map<Integer,String> primary=new TreeMap<>();
        try(Cursor columns=database.rawQuery("PRAGMA table_info("+name+")",null)){
            while(columns.moveToNext())if(columns.getInt(5)>0)primary.put(columns.getInt(5),columns.getString(1));
        }
        if(primary.isEmpty())throw new IllegalStateException("R29_WITHOUT_ROWID_PRIMARY_KEY_MISSING:"+table.name);
        if(windows){
            StringBuilder order=new StringBuilder();for(String column:primary.values()){if(order.length()>0)order.append(',');order.append("r29_current.").append(MigrationCopyR29.id(column)).append(" COLLATE BINARY ASC");}
            return "(SELECT CAST(ROW_NUMBER() OVER (ORDER BY "+order+") AS INTEGER) AS rowid,r29_current.* FROM "+name+" AS r29_current)";
        }
        StringBuilder less=new StringBuilder(),equal=new StringBuilder();
        for(String column:primary.values()){
            String quoted=MigrationCopyR29.id(column),prior="r29_prior."+quoted+" COLLATE BINARY",current="r29_current."+quoted+" COLLATE BINARY";
            if(less.length()>0)less.append(" OR ");
            less.append('(').append(equal).append(prior).append('<').append(current).append(')');
            equal.append(prior).append('=').append(current).append(" AND ");
        }
        // CAST preserves INTEGER affinity in the derived column: Android binds rawQuery arguments as text.
        return "(SELECT CAST(1+(SELECT count(*) FROM "+name+" AS r29_prior WHERE "+less+") AS INTEGER) AS rowid,r29_current.* FROM "+name+" AS r29_current)";
    }

    private static String unquoted(String sql){
        if(sql==null)return "";StringBuilder out=new StringBuilder();int n=sql.length();
        for(int i=0;i<n;i++){
            char c=sql.charAt(i);
            if(c=='\''||c=='\"'||c=='`'||c=='['){char close=c=='['?']':c;for(i++;i<n;i++)if(sql.charAt(i)==close){if(close!=']'&&i+1<n&&sql.charAt(i+1)==close){i++;continue;}break;}out.append(' ');}
            else if(c=='-'&&i+1<n&&sql.charAt(i+1)=='-'){while(i<n&&sql.charAt(i)!='\n')i++;out.append(' ');}
            else if(c=='/'&&i+1<n&&sql.charAt(i+1)=='*'){i+=2;while(i+1<n&&!(sql.charAt(i)=='*'&&sql.charAt(i+1)=='/'))i++;i++;out.append(' ');}
            else out.append(c);
        }
        return out.toString();
    }
}
