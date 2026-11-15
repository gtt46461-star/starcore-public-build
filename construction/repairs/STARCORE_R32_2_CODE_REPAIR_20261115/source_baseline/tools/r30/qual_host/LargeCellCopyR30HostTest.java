package com.ethk3.novaonepro.strategyv2;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.*;
import java.security.*;
import java.util.*;
/** Actual SQLite plus an explicitly modeled 2MiB window. Real Android coverage is a separate mandatory gate. */
public final class LargeCellCopyR30HostTest {
    static void require(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    static byte[] digest(byte[] bytes)throws Exception{return MessageDigest.getInstance("SHA-256").digest(bytes);}
    static byte[] cellHash(SQLiteDatabase db,String table,String col,String suffix)throws Exception{
        MessageDigest hash=MessageDigest.getInstance("SHA-256");long length;try(Cursor c=db.rawQuery("SELECT length(CAST("+TableCopierR30.q(col)+" AS BLOB)) FROM "+TableCopierR30.q(table)+suffix,null)){require(c.moveToFirst(),"MISSING_CELL");length=c.getLong(0);}
        for(long at=1;at<=length;at+=65536)try(Cursor c=db.rawQuery("SELECT substr(CAST("+TableCopierR30.q(col)+" AS BLOB),"+at+",65536) FROM "+TableCopierR30.q(table)+suffix,null)){require(c.moveToFirst(),"MISSING_CHUNK");byte[] b=c.getBlob(0);require(b.length==Math.min(65536,length-at+1),"SHORT_CHUNK");hash.update(b);}return hash.digest();
    }
    public static void main(String[] args)throws Exception{
        Class.forName("org.sqlite.JDBC");Path sourceFile=Files.createTempFile("r30-copy-source-",".db"),targetFile=Files.createTempFile("r30-copy-target-",".db");
        try(Connection sc=DriverManager.getConnection("jdbc:sqlite:"+sourceFile);Connection tc=DriverManager.getConnection("jdbc:sqlite:"+targetFile)){
            SQLiteDatabase source=new SQLiteDatabase(sc),target=new SQLiteDatabase(tc);
            source.execSQL("CREATE TABLE large_rowid(k INTEGER PRIMARY KEY,checkpoint BLOB,optimizer BLOB,detail TEXT,dynamic,a REAL,b INTEGER,c)");
            byte[] checkpoint=new byte[2*1024*1024],optimizer=new byte[4097];new Random(30).nextBytes(checkpoint);new Random(31).nextBytes(optimizer);
            String text=String.join("",Collections.nCopies(150000,"中🙂\0尾"));byte[] textBytes=text.getBytes(StandardCharsets.UTF_8);
            source.execSQL("INSERT INTO large_rowid VALUES(1,?,?,?, ?,?, ?,NULL)",new Object[]{checkpoint,optimizer,text,new byte[]{0,-1,1,0},.0123456789012345,Long.MAX_VALUE});
            source.execSQL("INSERT INTO large_rowid VALUES(?,NULL,NULL,'min-rowid',NULL,0,0,NULL)",new Object[]{Long.MIN_VALUE});
            source.execSQL("CREATE TABLE large_without(k BLOB,n INTEGER,checkpoint BLOB,detail TEXT,PRIMARY KEY(k,n)) WITHOUT ROWID");
            source.execSQL("INSERT INTO large_without VALUES(?,7,?,?)",new Object[]{checkpoint,optimizer,text});
            source.execSQL("CREATE TABLE small_types(k INTEGER PRIMARY KEY,v)");
            source.execSQL("INSERT INTO small_types VALUES(1,CAST(x'610062FF63' AS TEXT))");source.execSQL("INSERT INTO small_types VALUES(2,?)",new Object[]{new byte[0]});source.execSQL("INSERT INTO small_types VALUES(3,NULL)");source.execSQL("INSERT INTO small_types VALUES(4,?)",new Object[]{Long.MIN_VALUE});source.execSQL("INSERT INTO small_types VALUES(5,?)",new Object[]{.12345678901234567});
            TableCopierR30.Writer writer=(op,body)->{tc.setAutoCommit(false);try{body.run(target);tc.commit();}catch(Exception e){tc.rollback();throw e;}finally{tc.setAutoCommit(true);}};
            for(String table:new String[]{"large_rowid","large_without","small_types"}){TableCopierR30.Outcome out=TableCopierR30.copy(source,target,writer,table,"SYNTHETIC_LARGE_COPY","HOST_WINDOW_MODEL");require(out.state.equals("COPIED")&&out.sourceRows==out.copiedRows,"COUNTS:"+table+":"+out.state);}
            require(Arrays.equals(digest(checkpoint),cellHash(target,"large_rowid","checkpoint"," WHERE k=1")),"CHECKPOINT_BYTES_LOST");require(Arrays.equals(digest(optimizer),cellHash(target,"large_rowid","optimizer"," WHERE k=1")),"OPTIMIZER_BYTES_LOST");require(Arrays.equals(digest(textBytes),cellHash(target,"large_rowid","detail"," WHERE k=1")),"UTF8_NUL_BOUNDARIES_LOST");
            require(Arrays.equals(digest(checkpoint),cellHash(target,"large_without","k","")),"LARGE_BLOB_PRIMARY_KEY_LOST");require(Arrays.equals(digest(textBytes),cellHash(target,"large_without","detail","")),"WITHOUT_ROWID_TEXT_LOST");
            try(Cursor c=target.rawQuery("SELECT typeof(checkpoint),typeof(detail),a,b,typeof(c),typeof(dynamic) FROM large_rowid WHERE k=1",null)){require(c.moveToFirst(),"ROW_MISSING");require(c.getString(0).equals("blob")&&c.getString(1).equals("text")&&c.getString(4).equals("null")&&c.getString(5).equals("blob"),"STORAGE_TYPES_CHANGED");require(Double.compare(c.getDouble(2),.0123456789012345)==0&&c.getLong(3)==Long.MAX_VALUE,"NUMERIC_PRECISION_CHANGED");}
            try(Cursor c=target.rawQuery("SELECT detail FROM large_rowid WHERE k=?",new String[]{Long.toString(Long.MIN_VALUE)})){require(c.moveToFirst()&&c.getString(0).equals("min-rowid"),"LONG_MIN_ROWID_SKIPPED");}
            try(Cursor c=target.rawQuery("SELECT hex(CAST(v AS BLOB)),typeof(v) FROM small_types WHERE k=1",null)){require(c.moveToFirst()&&c.getString(0).equals("610062FF63")&&c.getString(1).equals("text"),"RAW_TEXT_BYTES_NORMALIZED");}
            TableCopierR30.copy(source,target,writer,"large_without","SYNTHETIC_RETRY","HOST_WINDOW_MODEL");require(TableCopierR30.count(target,"large_without")==1,"RETRY_DUPLICATES");
            System.out.println("R30_LARGE_COPY|ACTUAL_SQLITE|HOST_MODELED_2048KIB_WINDOW_NOT_ANDROID|2MIB_CHECKPOINT_AND_OPTIMIZER|UTF8_NUL_AND_RAW_TEXT_BYTES|LARGE_BLOB_PK_WITHOUT_ROWID|LONG_MIN_ROWID|EXACT_TYPES_AND_DOUBLE|IDEMPOTENT_RETRY|PASS");
        }finally{Files.deleteIfExists(sourceFile);Files.deleteIfExists(targetFile);}
    }
}
