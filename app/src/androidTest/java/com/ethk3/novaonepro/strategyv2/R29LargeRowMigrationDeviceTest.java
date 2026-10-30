package com.ethk3.novaonepro.strategyv2;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** Actual Android migration regression for >2MiB BLOB and TEXT, including NUL and supplementary Unicode.
 * Every database is isolated synthetic cache data, never the application database or natural evidence. */
@RunWith(AndroidJUnit4.class)
public final class R29LargeRowMigrationDeviceTest {
    static MigrationCopyR29.Table table(SQLiteDatabase db,String name){try(Cursor c=db.rawQuery("SELECT sql FROM sqlite_master WHERE name=?",new String[]{name})){assertTrue(c.moveToFirst());return MigrationCopyR29.table(db,name,c.getString(0));}}
    static byte[] expected(Object... cells)throws IOException{
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();DataOutputStream out=new DataOutputStream(bytes);out.writeInt(0x29DB0001);out.writeInt(cells.length);
        for(Object value:cells){if(value==null)out.writeByte(Cursor.FIELD_TYPE_NULL);else if(value instanceof Long){out.writeByte(Cursor.FIELD_TYPE_INTEGER);out.writeLong((Long)value);}else if(value instanceof Double){out.writeByte(Cursor.FIELD_TYPE_FLOAT);out.writeLong(Double.doubleToLongBits((Double)value));}else{boolean text=value instanceof String;byte[] cell=text?((String)value).getBytes(StandardCharsets.UTF_8):(byte[])value;out.writeByte(text?Cursor.FIELD_TYPE_STRING:Cursor.FIELD_TYPE_BLOB);out.writeInt(cell.length);out.write(cell);}}
        out.close();return bytes.toByteArray();
    }
    static void tx(SQLiteDatabase db,MigrationCopyR29.Body body)throws Exception{db.beginTransactionNonExclusive();try{body.run(db);db.setTransactionSuccessful();}finally{db.endTransaction();}}
    static long n(SQLiteDatabase db,String sql){try(Cursor c=db.rawQuery(sql,null)){assertTrue(c.moveToFirst());return c.getLong(0);}}
    @Test public void fullTypedMigrationChunksLargeRowsWithoutChangingStateOrHashes()throws Exception{
        Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();File directory=new File(context.getCacheDir(),"r29-large-row-migration-"+UUID.randomUUID());assertTrue(directory.mkdirs());
        Map<String,Object> receipt=new LinkedHashMap<>();receipt.put("scope","SYNTHETIC_ANDROID_LARGE_ROW_MIGRATION_NOT_NATURAL_QUALITY");
        try(SQLiteDatabase source=SQLiteDatabase.openOrCreateDatabase(new File(directory,"source.db"),null);SQLiteDatabase target=SQLiteDatabase.openOrCreateDatabase(new File(directory,"target.db"),null);SQLiteDatabase stage=SQLiteDatabase.openOrCreateDatabase(new File(directory,"stage.db"),null)){
            byte[] blob=new byte[(3<<20)+71];for(int i=0;i<blob.length;i++)blob[i]=(byte)(i*31+7);StringBuilder builder=new StringBuilder();for(int i=0;i<240000;i++)builder.append("汉字\u0000🚀");String text=builder.toString();assertTrue(text.getBytes(StandardCharsets.UTF_8).length>(2<<20));
            source.execSQL("CREATE TABLE r29_large_rows(id INTEGER PRIMARY KEY,payload BLOB,note TEXT,r REAL,empty_blob BLOB,empty_text TEXT,absent TEXT)");source.execSQL("INSERT INTO r29_large_rows VALUES(-37,?,?,0.125,X'','',NULL)",new Object[]{blob,text});source.execSQL("INSERT INTO r29_large_rows VALUES(9,X'01','tail',-3.5,X'','',NULL)");MigrationCopyR29.Table descriptor=table(source,"r29_large_rows");
            boolean failed=false;String failure="";try(Cursor c=source.rawQuery("SELECT payload,note FROM r29_large_rows WHERE id=-37",null)){if(c.moveToFirst())c.getBlob(0);}catch(SQLiteException original){failed=String.valueOf(original.getMessage()).contains("CursorWindow");failure=original.toString();}
            receipt.put("wholeRowNegativeControlRejectedByActualAndroid",failed);receipt.put("wholeRowNegativeControlFailure",failure);assertTrue("The original full-row query must reproduce the real API34 CursorWindow failure",failed);
            byte[] expected=expected(-37L,blob,text,.125,new byte[0],"",null);List<MigrationCopyR29.Row> first=MigrationCopyR29.readPage(source,descriptor,null);assertEquals(1,first.size());assertEquals(-37,first.get(0).rowid);assertArrayEquals(expected,first.get(0).payload);assertEquals(StatsR27.sha256(expected),first.get(0).hash);
            List<MigrationCopyR29.Row> second=MigrationCopyR29.readPage(source,descriptor,-37L);assertEquals(1,second.size());assertEquals(9,second.get(0).rowid);assertTrue(MigrationCopyR29.readPage(source,descriptor,9L).isEmpty());
            List<MigrationCopyR29.Row> cutover=ResearchCutoverR29.page(source,descriptor,null);assertArrayEquals(expected,cutover.get(0).payload);target.execSQL(descriptor.createSql);tx(target,db->ResearchCutoverR29.restorePage(db,descriptor,cutover));tx(target,db->ResearchCutoverR29.restorePage(db,descriptor,second));assertArrayEquals(expected,MigrationCopyR29.readPage(target,descriptor,null).get(0).payload);
            byte[] damaged=expected.clone();damaged[100]^=1;boolean checksumRejected=false;try{tx(target,db->ResearchCutoverR29.restorePage(db,descriptor,Collections.singletonList(new MigrationCopyR29.Row(-37,damaged,first.get(0).hash))));}catch(IllegalStateException wanted){checksumRejected=wanted.getMessage().contains("SOURCE_CHECKSUM");}assertTrue(checksumRejected);assertArrayEquals(expected,MigrationCopyR29.readPage(target,descriptor,null).get(0).payload);
            tx(source,db->MigrationCopyR29.trackSource(db,descriptor));for(String ddl:MigrationCopyR29.DESTINATION_DDL)stage.execSQL(ddl);tx(stage,db->MigrationCopyR29.register(db,Collections.singletonList(descriptor)));
            long now=System.currentTimeMillis();for(int i=0;i<20&&n(stage,"SELECT count(*) FROM r29_copy_manifest WHERE state='VERIFIED_BASELINE'")==0;i++)MigrationCopyR29.step(source,stage,Collections.singletonList(descriptor),body->tx(stage,body),now+i*2000L);
            assertEquals(1,n(stage,"SELECT count(*) FROM r29_copy_manifest WHERE state='VERIFIED_BASELINE' AND copied_rows=2 AND verified_rows=2 AND copy_hash=verify_hash"));
            long[] captured={0};List<byte[]> upgraded=LegacyDomainUpgradeR29.rows(source,descriptor,new LegacyDomainUpgradeR29.Check(()->0,Long.MAX_VALUE,()->false),captured,32L<<20);assertEquals(2,upgraded.size());assertArrayEquals(expected,upgraded.get(0));
            source.execSQL("CREATE TABLE r29_large_composite(k TEXT,v INTEGER,payload BLOB,note TEXT,PRIMARY KEY(v,k)) WITHOUT ROWID");source.execSQL("INSERT INTO r29_large_composite VALUES('first',-9,?,?)",new Object[]{blob,text});source.execSQL("INSERT INTO r29_large_composite VALUES('tail',7,X'00','last')");MigrationCopyR29.Table composite=table(source,"r29_large_composite");List<MigrationCopyR29.Row> ranked=ResearchCutoverR29.page(source,composite,null);assertEquals(1,ranked.size());assertEquals(1,ranked.get(0).rowid);assertArrayEquals(expected("first",-9L,blob,text),ranked.get(0).payload);assertEquals(2,ResearchCutoverR29.page(source,composite,1L).get(0).rowid);
            receipt.put("blobBytes",blob.length);receipt.put("textUtf8Bytes",text.getBytes(StandardCharsets.UTF_8).length);receipt.put("completeTypedRowBytes",expected.length);receipt.put("chunkBytes",ResearchRowsR29.CHUNK_BYTES);receipt.put("typedPayloadAndHashesExact",true);receipt.put("physicalAndCompositePrimaryKeyPaginationExact",true);receipt.put("stagedIndependentRehashPassed",true);receipt.put("legacyDomainUpgradeCapturePassed",true);receipt.put("damagedTransferRejected",true);receipt.put("result","PASS_WITH_STATED_ANDROID_SCOPE");
        }catch(Throwable failure){receipt.put("result","FAIL");receipt.put("error",failure.toString());throw failure;}
        finally{File[] files=directory.listFiles();if(files!=null)for(File file:files){if(file.getName().endsWith(".db"))SQLiteDatabase.deleteDatabase(file);else if(file.exists())assertTrue(file.delete());}if(directory.exists())assertTrue(directory.delete());try(FileOutputStream out=new FileOutputStream(new File(context.getFilesDir(),"r29-large-row-migration-device.json"))){out.write(JsonR27.write(receipt).getBytes(StandardCharsets.UTF_8));out.getFD().sync();}}
    }
}
