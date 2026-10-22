package com.ethk3.novaonepro;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.util.List;
import org.json.JSONObject;

/** Lossless metadata JSON with bounded CursorWindow cells and Java text buffers.
 * The caller supplies an immutable read-only report snapshot, never the live writer.
 * SQL NULL, empty text and binary payload descriptors retain distinct identities. */
public final class ReportMetadataStreamR81 {
    private static final int INLINE_BYTES=512, CHUNK_BYTES=32768;
    private ReportMetadataStreamR81() {}

    public static long writeRows(SQLiteDatabase db,String table,List<String> columns,
            Writer out,ReportNativeCP4.Control control)throws IOException {
        if(db==null||!db.isReadOnly()||columns==null||columns.isEmpty()
                ||columns.size()>1024||out==null||control==null)
            throw new IOException("REPORT_METADATA_INPUT");
        Charset encoding=encoding(db);
        String id=ident(table),rowid=rowid(columns);
        StringBuilder sql=new StringBuilder("SELECT ").append(rowid);
        for(String column:columns) {
            sql.append(',').append(descriptor(ident(column)));
        }
        sql.append(" FROM ").append(id).append(" ORDER BY ").append(rowid);
        long count=0;
        try(Cursor rows=db.rawQuery(sql.toString(),null)) {
            while(rows.moveToNext()) {
                check(control);long key=rows.getLong(0);
                if(count>0)out.write(',');out.write('{');
                for(int i=0;i<columns.size();i++) {
                    if(i>0)out.write(',');out.write(JSONObject.quote(columns.get(i)));out.write(':');
                    writeValue(db,id,rowid,key,ident(columns.get(i)),rows.getString(i+1),encoding,out,control);
                }
                out.write('}');count++;
            }
        }
        return count;
    }

    /** GROUP BY values are legal TEXT with no schema length limit. Transport only
     * a representative row id and count; stream its exact value from the same snapshot.
     * Groups are written one at a time, rather than accumulated in a Java JSONArray. */
    static long writeGroups(SQLiteDatabase db,String table,String column,Writer out,
            ReportNativeCP4.Control control,boolean precedingGroups)throws IOException {
        Charset encoding=encoding(db);String id=ident(table),c=ident(column);
        long groups=0;
        try(Cursor rows=db.rawQuery("SELECT MIN(rowid),COUNT(*) FROM "+id+" GROUP BY "+c,null)) {
            while(rows.moveToNext()) {
                check(control);long key=rows.getLong(0),count=rows.getLong(1);
                if(precedingGroups||groups>0)out.write(',');
                out.write("{\"column\":");out.write(JSONObject.quote(column));out.write(",\"value\":");
                writeCell(db,id,"rowid",key,c,encoding,out,control);
                out.write(",\"rows\":");out.write(Long.toString(count));out.write('}');groups++;
            }
        }
        return groups;
    }

    /** Fixed allowlisted app_state value. A key's exact body may be arbitrarily
     * long; unknown keys are selected by the caller's fixed list, never enumerated. */
    static void writeState(SQLiteDatabase db,String key,Writer out,ReportNativeCP4.Control control)throws IOException {
        check(control);out.write("{\"key\":");out.write(JSONObject.quote(key));
        try(Cursor c=db.rawQuery("SELECT rowid,updated_at FROM app_state WHERE state_key=?",new String[]{key})) {
            if(!c.moveToFirst()){out.write(",\"value\":null,\"status\":\"NOT_RECORDED\"}");return;}
            long row=c.getLong(0),at=c.getLong(1);out.write(",\"value\":");
            writeCell(db,"\"app_state\"","rowid",row,"\"state_value\"",encoding(db),out,control);
            out.write(",\"updatedAt\":");out.write(Long.toString(at));out.write(",\"status\":\"READ_OK\"}");
            if(c.moveToNext())throw new IOException("REPORT_METADATA_STATE_DUPLICATE");
        }
    }

    private static String descriptor(String c) {
        String bytes="length(CAST("+c+" AS BLOB))";
        // Prefixes are transport tags, not metadata values. Even a value beginning
        // with N/B/L/S is unambiguous, because every inline value receives S.
        return "CASE WHEN "+c+" IS NULL THEN 'N' WHEN typeof("+c+")='blob' THEN 'B'||"+bytes
            +" WHEN "+bytes+">"+INLINE_BYTES+" THEN 'L'||"+bytes+" ELSE 'S'||CAST("+c+" AS TEXT) END";
    }
    private static void writeCell(SQLiteDatabase db,String table,String rowid,long key,String column,
            Charset encoding,Writer out,ReportNativeCP4.Control control)throws IOException {
        check(control);
        try(Cursor c=db.rawQuery("SELECT "+descriptor(column)+" FROM "+table+" WHERE "+rowid+"=?",new String[]{Long.toString(key)})) {
            if(!c.moveToFirst())throw new IOException("REPORT_METADATA_CELL_CHANGED");
            writeValue(db,table,rowid,key,column,c.getString(0),encoding,out,control);
            if(c.moveToNext())throw new IOException("REPORT_METADATA_CELL_CHANGED");
        }
    }
    private static void writeValue(SQLiteDatabase db,String table,String rowid,long key,String column,
            String value,Charset encoding,Writer out,ReportNativeCP4.Control control)throws IOException {
        if(value==null||value.isEmpty())throw new IOException("REPORT_METADATA_TAG");
        char tag=value.charAt(0);
        if(tag=='N'&&value.length()==1)out.write("null");
        else if(tag=='S')out.write(JSONObject.quote(value.substring(1)));
        else if(tag=='B')out.write(JSONObject.quote("[BINARY_PAYLOAD_NOT_INCLUDED bytes="+length(value)+"]"));
        else if(tag=='L')writeText(db,table,rowid,key,column,length(value),encoding,out,control);
        else throw new IOException("REPORT_METADATA_TAG");
    }

    private static String rowid(List<String> columns)throws IOException {
        for(String alias:new String[]{"rowid","_rowid_","oid"}) {
            boolean shadowed=false;for(String column:columns)if(alias.equalsIgnoreCase(column)){shadowed=true;break;}
            // Fixed SQLite aliases need no quoting. On a WITHOUT ROWID table an
            // absent alias must fail as an identifier, never become a DQS string.
            if(!shadowed)return alias;
        }
        throw new IOException("REPORT_METADATA_ROWID_SHADOWED");
    }
    private static long length(String value)throws IOException {
        try {long length=Long.parseLong(value.substring(1));if(length<0)throw new NumberFormatException();return length;}
        catch(NumberFormatException invalid){throw new IOException("REPORT_METADATA_LENGTH",invalid);}
    }
    private static Charset encoding(SQLiteDatabase db)throws IOException {
        try(Cursor c=db.rawQuery("PRAGMA encoding",null)) {
            if(!c.moveToFirst())throw new IOException("REPORT_METADATA_ENCODING");
            String name=c.getString(0);
            if("UTF-8".equalsIgnoreCase(name))return Charset.forName("UTF-8");
            if("UTF-16le".equalsIgnoreCase(name))return Charset.forName("UTF-16LE");
            if("UTF-16be".equalsIgnoreCase(name))return Charset.forName("UTF-16BE");
            throw new IOException("REPORT_METADATA_ENCODING");
        }
    }
    private static void writeText(SQLiteDatabase db,String table,String rowid,long key,
            String column,long length,Charset encoding,Writer out,ReportNativeCP4.Control control)throws IOException {
        out.write('"');
        try(Reader in=new InputStreamReader(new CellBytes(db,table,rowid,key,column,length,control),
                encoding.newDecoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT))) {
            char[] buffer=new char[4096];int n;
            while((n=in.read(buffer))!=-1) {
                check(control);
                // Escaping never assembles the full cell or row. Writer keeps UTF-16
                // surrogate pairs across buffer boundaries when encoding the JSON file.
                int begin=0;
                for(int i=0;i<n;i++) {
                    char c=buffer[i];
                    if(c=='"'||c=='\\'||c<32) {
                        if(i>begin)out.write(buffer,begin,i-begin);
                        if(c=='"'||c=='\\'){out.write('\\');out.write(c);}
                        else {out.write("\\u00");out.write("0123456789abcdef".charAt(c>>>4));out.write("0123456789abcdef".charAt(c&15));}
                        begin=i+1;
                    }
                }
                if(n>begin)out.write(buffer,begin,n-begin);
            }
        }
        out.write('"');
    }
    private static final class CellBytes extends InputStream {
        private final SQLiteDatabase db;private final String sql;private final long row,length;
        private final ReportNativeCP4.Control control;private long read;private byte[] chunk=new byte[0];private int offset;
        CellBytes(SQLiteDatabase db,String table,String rowid,long row,String column,long length,ReportNativeCP4.Control control) {
            this.db=db;this.row=row;this.length=length;this.control=control;
            sql="SELECT substr(CAST("+column+" AS BLOB),?,?) FROM "+table+" WHERE "+rowid+"=?";
        }
        private boolean fill()throws IOException {
            if(offset<chunk.length)return true;
            if(read==length)return false;check(control);
            int wanted=(int)Math.min(CHUNK_BYTES,length-read);
            try(Cursor c=db.rawQuery(sql,new String[]{Long.toString(read+1),Integer.toString(wanted),Long.toString(row)})) {
                if(!c.moveToFirst()||c.isNull(0))throw new IOException("REPORT_METADATA_CELL_CHANGED");
                chunk=c.getBlob(0);if(chunk==null||chunk.length!=wanted||c.moveToNext())throw new IOException("REPORT_METADATA_CELL_CHANGED");
            }
            offset=0;read+=chunk.length;return true;
        }
        @Override public int read()throws IOException{return fill()?chunk[offset++]&255:-1;}
        @Override public int read(byte[] buffer,int start,int length)throws IOException {
            if(buffer==null)throw new NullPointerException();
            if(start<0||length<0||start>buffer.length-length)throw new IndexOutOfBoundsException();
            if(length==0)return 0;if(!fill())return -1;
            int n=Math.min(length,chunk.length-offset);System.arraycopy(chunk,offset,buffer,start,n);offset+=n;return n;
        }
    }
    private static String ident(String value)throws IOException {
        if(value==null||value.isEmpty())throw new IOException("REPORT_METADATA_IDENTIFIER");
        return '"'+value.replace("\"","\"\"")+'"';
    }
    private static void check(ReportNativeCP4.Control control)throws IOException {
        if(Thread.currentThread().isInterrupted()||!control.shouldContinue())throw new InterruptedIOException("REPORT_CANCELLED");
    }
}
