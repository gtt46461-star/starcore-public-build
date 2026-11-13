package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.*;

/** Native SQLite exercises the actual table reader; synthetic originals only.
 * A valid pair of cells can exceed the page budget even when neither cell does. */
public final class R31ResearchTableBudgetHost {
    private static int assertions;
    private static void check(boolean ok,String code){assertions++;if(!ok)throw new AssertionError(code);}
    private static String text(int count){char[] value=new char[count];Arrays.fill(value,'x');return new String(value);}
    private static void insert(SQLiteDatabase db,String key,String packet,String result,long created){
        db.execSQL("INSERT INTO r30_frozen_confirm VALUES(?, 'policy', 'dataset', 1, 'COMPLETED', 1, 'REGISTERED', 'experiment', ?, ?, ?)",new Object[]{key,packet,result,created});
    }
    private static Map<String,Object> actualRow(SQLiteDatabase db){
        Map<String,Object> row=new LinkedHashMap<>();
        try(Cursor c=db.rawQuery(ResearchTableAdaptersR30R1.query("T13"),null)){
            check(c.moveToFirst(),"SYNTHETIC_ROW_PRESENT");
            for(int i=0;i<c.getColumnCount();i++)row.put(c.getColumnName(i),c.getType(i)==Cursor.FIELD_TYPE_INTEGER?(Object)c.getLong(i):c.getString(i));
        }
        return row;
    }
    private static int bytes(Object row){return JsonR27.write(row).getBytes(StandardCharsets.UTF_8).length;}
    private static Map<String,Object> read(SQLiteDatabase db,long offset){return ResearchTableAdaptersR30R1.readPageR31(db,"T13",Collections.emptyMap(),offset,20,null);}
    private static long number(Map<String,Object> value,String key){return ((Number)value.get(key)).longValue();}
    private static String originals(SQLiteDatabase db){
        StringBuilder values=new StringBuilder();
        try(Cursor c=db.rawQuery("SELECT task_key,packet_json,result_json FROM r30_frozen_confirm ORDER BY task_key",null)){
            while(c.moveToNext())values.append(c.getString(0)).append('|').append(StatsR27.sha256(c.getString(1))).append('|').append(StatsR27.sha256(c.getString(2))).append('\n');
        }
        return StatsR27.sha256(values.toString());
    }
    public static void main(String[] args)throws Exception{
        Class.forName("org.sqlite.JDBC");
        try(Connection connection=DriverManager.getConnection("jdbc:sqlite::memory:")){
            SQLiteDatabase db=new SQLiteDatabase(connection);
            db.execSQL("CREATE TABLE r30_frozen_confirm(task_key TEXT PRIMARY KEY,policy_hash TEXT,dataset_hash TEXT,block_id INTEGER,state TEXT,verified INTEGER,registration_state TEXT,registration_experiment_id TEXT,packet_json TEXT,result_json TEXT,created_at INTEGER)");
            String packet=text(262144);
            insert(db,"oversized-first",packet,"",20);
            int resultBytes=524289-bytes(actualRow(db));
            check(resultBytes>0&&resultBytes<=262144,"BOTH_CELLS_WITHIN_ORIGINAL_256_KIB_BOUND");
            db.execSQL("UPDATE r30_frozen_confirm SET result_json=? WHERE task_key='oversized-first'",new Object[]{text(resultBytes)});
            check(bytes(actualRow(db))==524289,"ACTUAL_SQLITE_FIRST_ROW_ONE_BYTE_OVER_512_KIB");
            insert(db,"next-readable","packet","result",19);
            String before=originals(db);
            Map<String,Object> tooLarge=read(db,0);
            check("READ_ERROR".equals(tooLarge.get("status")),"OVERSIZED_FIRST_ROW_MUST_NOT_BE_EMPTY_NO_ROWS");
            check("TABLE_FIRST_ROW_BYTE_BUDGET".equals(tooLarge.get("reason")),"FIRST_ROW_CAPACITY_REASON_EXPLICIT");
            check("TABLE_PAGE_BYTE_BUDGET".equals(tooLarge.get("truncatedReason")),"ORIGINAL_PAGE_BOUND_RETAINED");
            check(number(tooLarge,"shown")==0&&number(tooLarge,"bytes")==0,"NO_PARTIAL_OR_OVERSIZED_ROW_RETURNED");
            check(Boolean.TRUE.equals(tooLarge.get("hasMore")),"BLOCKED_ROW_NOT_MISTAKEN_FOR_END_OF_SOURCE");
            check(Boolean.TRUE.equals(tooLarge.get("readOnly")),"READ_ONLY_RECEIPT");
            check(((String)tooLarge.get("receiptSha256")).matches("[0-9a-f]{64}"),"FAILURE_RECEIPT_HASHED");
            check(before.equals(originals(db)),"OVERSIZED_ORIGINAL_UNCHANGED");
            Map<String,Object> next=read(db,1);
            check("READ_OK".equals(next.get("status"))&&number(next,"shown")==1,"EXPLICIT_NEXT_OFFSET_REMAINS_READABLE");
            check(number(next,"bytes")>0&&number(next,"bytes")<=524288,"FOLLOWING_PAGE_WITHIN_BOUND");
            Map<String,Object> empty=read(db,2);
            check("EMPTY_NO_ROWS".equals(empty.get("status")),"ONLY_REAL_EMPTY_PAGE_REPORTED_EMPTY");
            check(!empty.containsKey("reason")&&!empty.containsKey("truncatedReason"),"REAL_EMPTY_HAS_NO_CAPACITY_REASON");
            check(number(empty,"shown")==0&&number(empty,"bytes")==0&&!Boolean.TRUE.equals(empty.get("hasMore")),"REAL_EMPTY_END_OF_SOURCE");
            check(before.equals(originals(db)),"ALL_READS_PRESERVE_NATIVE_SQLITE_ORIGINALS");

            db.execSQL("DELETE FROM r30_frozen_confirm");
            for(int i=0;i<3;i++)insert(db,"partial-"+i,text(100000),text(100000),30-i);
            before=originals(db);Map<String,Object> partial=read(db,0);
            check("READ_OK".equals(partial.get("status"))&&number(partial,"shown")==2,"LATER_OVERFLOW_RETURNS_TWO_REAL_ROWS");
            check(number(partial,"bytes")<=524288&&Boolean.TRUE.equals(partial.get("hasMore")),"PARTIAL_PAGE_BOUND_AND_CONTINUATION");
            check("TABLE_PAGE_BYTE_BUDGET".equals(partial.get("truncatedReason")),"PARTIAL_PAGE_TRUNCATION_EXPLICIT");
            check(!partial.containsKey("reason"),"READABLE_PARTIAL_PAGE_NOT_FIRST_ROW_FAILURE");
            check("READ_OK".equals(read(db,2).get("status")),"PARTIAL_REMAINDER_READABLE_BY_OFFSET");
            check(before.equals(originals(db)),"PARTIAL_PAGE_ORIGINALS_UNCHANGED");

            db.execSQL("DELETE FROM r30_frozen_confirm");insert(db,"cell-too-large",text(262145),"",40);
            before=originals(db);Map<String,Object> cell=read(db,0);
            check("READ_ERROR".equals(cell.get("status")),"ORIGINAL_SINGLE_CELL_BOUND_STILL_ENFORCED");
            check(String.valueOf(cell.get("reason")).contains("TABLE_CELL_BYTE_BUDGET"),"SINGLE_CELL_REASON_DISTINCT");
            check(before.equals(originals(db)),"SINGLE_CELL_FAILURE_PRESERVES_ORIGINAL");
        }
        System.out.println("R31_TABLE_READER_NATIVE_PASS assertions="+assertions+" firstRowBytes=524289 pageBound=524288 cellBound=262144 originals=UNCHANGED scope=HOST_SYNTHETIC_NOT_ANDROID_NOT_NATURAL");
    }
}
