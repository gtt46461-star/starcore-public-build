package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.sql.DriverManager;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Real typed bytes, large-cell paging and independent destination checksum; not an Android execution receipt. */
public final class LargeResearchRowsR29HostTest {
    static int checks;
    static void check(String name,boolean valid){System.out.println("R29_LARGE_ROWS|"+name+"|"+(valid?"OK":"FAIL"));if(!valid)throw new AssertionError(name);checks++;}
    static final class Bounded extends SQLiteDatabase {
        int maxBlob,chunks;Bounded(SQLiteDatabase source){super(source.jdbc);}
        public Cursor rawQuery(String sql,String[] args){final Cursor c=super.rawQuery(sql,args);return new Cursor(){
            public boolean moveToNext(){return c.moveToNext();}public boolean moveToFirst(){return c.moveToFirst();}public long getLong(int i){return c.getLong(i);}public int getInt(int i){return c.getInt(i);}public double getDouble(int i){return c.getDouble(i);}public boolean isNull(int i){return c.isNull(i);}public int getCount(){return c.getCount();}public int getType(int i){return c.getType(i);}public void close(){c.close();}
            public byte[] getBlob(int i){byte[] b=c.getBlob(i);if(b!=null){maxBlob=Math.max(maxBlob,b.length);chunks++;if(b.length>ResearchRowsR29.CHUNK_BYTES)throw new AssertionError("whole BLOB read escaped into the HOST guard");}return b;}
            public String getString(int i){String s=c.getString(i);if(s!=null&&s.getBytes(StandardCharsets.UTF_8).length>ResearchRowsR29.CHUNK_BYTES)throw new AssertionError("whole TEXT read escaped into the HOST guard");return s;}
        };}
    }
    static SQLiteDatabase open()throws Exception{return new SQLiteDatabase(DriverManager.getConnection("jdbc:sqlite::memory:"));}
    static MigrationCopyR29.Table table(SQLiteDatabase db,String name){try(Cursor c=db.rawQuery("SELECT sql FROM sqlite_master WHERE name=?",new String[]{name})){if(!c.moveToFirst())throw new AssertionError(name);return MigrationCopyR29.table(db,name,c.getString(0));}}
    static byte[] direct(SQLiteDatabase db,MigrationCopyR29.Table table,long rowid){StringBuilder columns=new StringBuilder();for(String col:table.columns){if(columns.length()>0)columns.append(',');columns.append(MigrationCopyR29.id(col));}try(Cursor c=db.rawQuery("SELECT "+columns+" FROM "+ResearchRowsR29.cursorSource(db,table)+" WHERE rowid=?",new String[]{String.valueOf(rowid)})){if(!c.moveToFirst())throw new AssertionError();return MigrationCopyR29.encode(c,0,table.columns.size());}}
    static void tx(SQLiteDatabase db,MigrationCopyR29.Body body)throws Exception{db.beginTransactionNonExclusive();try{body.run(db);db.setTransactionSuccessful();}finally{db.endTransaction();}}
    static long n(SQLiteDatabase db,String sql){try(Cursor c=db.rawQuery(sql,null)){if(!c.moveToFirst())throw new AssertionError(sql);return c.getLong(0);}}
    static String text(){StringBuilder b=new StringBuilder();for(int i=0;i<240000;i++)b.append("汉字\u0000🚀");return b.toString();}
    public static void main(String[] args)throws Exception{
        byte[] blob=new byte[(3<<20)+71];for(int i=0;i<blob.length;i++)blob[i]=(byte)(i*31+7);String text=text();
        check("G1_FIXTURE_CONTAINS_REAL_LARGE_BLOB_AND_NUL_EMOJI_TEXT",blob.length>(2<<20)&&text.getBytes(StandardCharsets.UTF_8).length>(2<<20)&&text.indexOf('\0')>0);
        try(SQLiteDatabase source=open();SQLiteDatabase destination=open();SQLiteDatabase stage=open()){
            source.execSQL("CREATE TABLE r29_large_rows(id INTEGER PRIMARY KEY,payload BLOB,note TEXT,r REAL,empty_blob BLOB,empty_text TEXT,absent TEXT)");
            source.execSQL("INSERT INTO r29_large_rows VALUES(-37,?,?,0.125,X'','',NULL)",new Object[]{blob,text});source.execSQL("INSERT INTO r29_large_rows VALUES(9,X'01','tail',-3.5,X'','',NULL)");MigrationCopyR29.Table descriptor=table(source,"r29_large_rows");
            byte[] expected=direct(source,descriptor,-37);Bounded guarded=new Bounded(source);
            List<MigrationCopyR29.Row> first=MigrationCopyR29.readPage(guarded,descriptor,null);
            check("G2_CHUNKED_TYPED_ABI_MATCHES_ORIGINAL_COMPLETE_CURSOR_BYTES",first.size()==1&&Arrays.equals(expected,first.get(0).payload)&&StatsR27.sha256(expected).equals(first.get(0).hash));
            check("G3_EVERY_CURSOR_BLOB_CELL_STAYS_AT_MOST_128K",guarded.chunks>32&&guarded.maxBlob==ResearchRowsR29.CHUNK_BYTES);
            List<MigrationCopyR29.Row> second=MigrationCopyR29.readPage(guarded,descriptor,first.get(0).rowid);
            check("G4_LARGE_ROW_PAGE_RESUMES_AT_EXACT_PHYSICAL_CURSOR",first.get(0).rowid==-37&&second.size()==1&&second.get(0).rowid==9&&MigrationCopyR29.readPage(guarded,descriptor,9L).isEmpty());
            List<MigrationCopyR29.Row> cutover=ResearchCutoverR29.page(guarded,descriptor,null);check("G5_CUTOVER_USES_IDENTICAL_FULL_LARGE_TYPED_BYTES",cutover.size()==1&&Arrays.equals(expected,cutover.get(0).payload));
            destination.execSQL(descriptor.createSql);tx(destination,db->ResearchCutoverR29.restorePage(db,descriptor,cutover));tx(destination,db->ResearchCutoverR29.restorePage(db,descriptor,second));Bounded target=new Bounded(destination);
            check("G6_RESTORED_LARGE_ROW_REHASH_IS_EXACT",Arrays.equals(expected,MigrationCopyR29.readPage(target,descriptor,null).get(0).payload));
            byte[] damaged=expected.clone();damaged[100]^=1;boolean rejected=false;try{tx(destination,db->ResearchCutoverR29.restorePage(db,descriptor,Collections.singletonList(new MigrationCopyR29.Row(-37,damaged,first.get(0).hash))));}catch(IllegalStateException wanted){rejected=wanted.getMessage().contains("SOURCE_CHECKSUM");}
            check("G7_DAMAGED_TRANSFER_BYTES_FAIL_BEFORE_RESTORE",rejected&&Arrays.equals(expected,MigrationCopyR29.readPage(target,descriptor,null).get(0).payload));
            tx(source,db->MigrationCopyR29.trackSource(db,descriptor));for(String ddl:MigrationCopyR29.DESTINATION_DDL)stage.execSQL(ddl);tx(stage,db->MigrationCopyR29.register(db,Collections.singletonList(descriptor)));Bounded stageReader=new Bounded(stage);
            long now=System.currentTimeMillis();for(int i=0;i<20&&n(stage,"SELECT count(*) FROM r29_copy_manifest WHERE state='VERIFIED_BASELINE'")==0;i++){final long clock=now+i*2000;MigrationCopyR29.step(guarded,stageReader,Collections.singletonList(descriptor),body->tx(stage,body),clock);}
            check("G8_STAGED_DESTINATION_VERIFICATION_ALSO_CHUNKS_ENCODED_PAYLOAD",n(stage,"SELECT count(*) FROM r29_copy_manifest WHERE state='VERIFIED_BASELINE' AND copied_rows=2 AND verified_rows=2")==1&&stageReader.chunks>0&&stageReader.maxBlob<=ResearchRowsR29.CHUNK_BYTES);
            long[] capturedBytes={0};List<byte[]> captured=LegacyDomainUpgradeR29.rows(guarded,descriptor,new LegacyDomainUpgradeR29.Check(()->0,Long.MAX_VALUE,()->false),capturedBytes,32L<<20);
            check("G9_OLD_DOMAIN_UPGRADE_USES_THE_SAME_SAFE_TYPED_READER",captured.size()==2&&Arrays.equals(expected,captured.get(0))&&capturedBytes[0]==captured.get(0).length+captured.get(1).length);
            boolean cancelled=false;try{ResearchRowsR29.readPage(guarded,descriptor,null,"1=1",32,2L<<20,Long.MAX_VALUE,()->{throw new IllegalStateException("SYNTHETIC_CANCEL");});}catch(IllegalStateException wanted){cancelled=wanted.getMessage().equals("SYNTHETIC_CANCEL");}
            check("G10_CANCEL_BEFORE_OR_BETWEEN_CHUNKS_NEVER_RETURNS_PARTIAL_ROW",cancelled);
            boolean changed=false;source.execSQL("UPDATE r29_large_rows SET payload=X'00' WHERE id=-37");try{ResearchRowsR29.readBlob(guarded,"r29_large_rows","payload","id=?",new String[]{"-37"},blob.length,()->{});}catch(IllegalStateException wanted){changed=wanted.getMessage().contains("CELL_CHANGED_OR_MISSING");}
            check("G11_STALE_BLOB_LENGTH_IS_A_REAL_FAILURE",changed);
        }
        try(SQLiteDatabase db=open()){
            db.execSQL("CREATE TABLE r29_big_composite(a TEXT,b INTEGER,payload BLOB,note TEXT,PRIMARY KEY(b,a)) WITHOUT ROWID");db.execSQL("INSERT INTO r29_big_composite VALUES('z',-9,?,?)",new Object[]{blob,text});db.execSQL("INSERT INTO r29_big_composite VALUES('a',4,X'01','tail')");MigrationCopyR29.Table t=table(db,"r29_big_composite");byte[] expected=direct(db,t,1);Bounded guarded=new Bounded(db);List<MigrationCopyR29.Row> page=ResearchRowsR29.readPage(guarded,t,null,"1=1",32,2L<<20,Long.MAX_VALUE,()->{});
            check("G12_LARGE_WITHOUT_ROWID_COMPOSITE_KEYS_KEEP_COMPLETE_PAYLOAD",page.size()==1&&page.get(0).rowid==1&&Arrays.equals(expected,page.get(0).payload)&&ResearchRowsR29.readPage(guarded,t,1L,"1=1",32,2L<<20,Long.MAX_VALUE,()->{}).get(0).rowid==2);
        }
        try(SQLiteDatabase db=open()){
            db.execSQL("PRAGMA encoding='UTF-16le'");db.execSQL("CREATE TABLE r29_utf16(k INTEGER PRIMARY KEY,t TEXT)");db.execSQL("INSERT INTO r29_utf16 VALUES(1,?)",new Object[]{"汉字\u0000🚀"});MigrationCopyR29.Table t=table(db,"r29_utf16");
            check("G13_SQLITE_TEXT_STORAGE_ENCODING_DOES_NOT_CHANGE_UTF8_TYPED_ABI",Arrays.equals(direct(db,t,1),ResearchRowsR29.readPage(new Bounded(db),t,null,"1=1",1,Long.MAX_VALUE,Long.MAX_VALUE,()->{}).get(0).payload));
        }
        System.out.println("R29_LARGE_ROWS|ALL_PASS|checks="+checks+"|HOST_TYPED_SQL_ONLY_ANDROID_CURSORWINDOW_TEST_PENDING");
    }
}
