package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.DbStore;
import java.util.*;

/** Read replicas for the legacy SQL kernels. Authority stays in Root; a research writer never attaches it.
 * Normal appends/repairs copy only issues named by new canonical events; unexplained source revisions rebuild
 * the bounded hot-history replica. Capture occurs before the independent transaction and is generation-fenced. */
public final class ResearchInputMirrorR29 {
    private ResearchInputMirrorR29(){}
    public static final String DDL="CREATE TABLE IF NOT EXISTS r29_input_mirror(singleton INTEGER PRIMARY KEY CHECK(singleton=1),source_generation INTEGER NOT NULL,source_seq INTEGER NOT NULL,updated_at INTEGER NOT NULL)";
    static boolean table(SQLiteDatabase db,String name){try(Cursor c=db.rawQuery("SELECT 1 FROM sqlite_master WHERE type='table' AND name=?",new String[]{name})){return c.moveToFirst();}}
    static long generation(SQLiteDatabase db){return table(db,"su1_source_clock")?MigrationCopyR29.scalar(db,"SELECT generation FROM su1_source_clock WHERE singleton=1",null):0;}
    static long sequence(SQLiteDatabase db){return MigrationCopyR29.scalar(db,"SELECT coalesce(max(seq),0) FROM r29_event_outbox WHERE kind='DRAW_COMMITTED'",null);}
    static void initialized(SQLiteDatabase target,SQLiteDatabase root){target.execSQL(DDL);target.execSQL("INSERT OR REPLACE INTO r29_input_mirror VALUES(1,?,?,?)",new Object[]{generation(root),sequence(root),System.currentTimeMillis()});}
    static MigrationCopyR29.Table descriptor(SQLiteDatabase root,String name){try(Cursor c=root.rawQuery("SELECT sql FROM sqlite_master WHERE type='table' AND name=?",new String[]{name})){if(!c.moveToFirst())return null;return MigrationCopyR29.table(root,name,c.getString(0));}}
    static List<MigrationCopyR29.Row> rows(SQLiteDatabase source,MigrationCopyR29.Table table,String predicate,String[] args){
        StringBuilder cols=new StringBuilder("rowid");for(String c:table.columns)cols.append(',').append(MigrationCopyR29.id(c));List<MigrationCopyR29.Row> out=new ArrayList<>();
        try(Cursor c=source.rawQuery("SELECT "+cols+" FROM "+ResearchRowsR29.cursorSource(source,table)+" WHERE "+predicate+" ORDER BY rowid",args)){while(c.moveToNext()){byte[] b=MigrationCopyR29.encode(c,1,table.columns.size());out.add(new MigrationCopyR29.Row(c.getLong(0),b,StatsR27.sha256(b)));}}return out;
    }
    public static void refresh(DbStore store){
        SQLiteDatabase authority=LegacyResearchScopeR29.rawRealtime(store);if(!ResearchRouteR29.active(authority))return;
        SQLiteDatabase root=ResearchRouteR29.realtimeReadOnly(authority);
        // Terminal labels may arrive after the canonical draw clock stopped changing.
        CriticalInputMirrorR29.refresh(store);
        SQLiteDatabase research=ResearchRouteR29.reader(root);long gen=generation(root),seq=sequence(root),oldGen=-1,oldSeq=-1;
        if(table(research,"r29_input_mirror"))try(Cursor c=research.rawQuery("SELECT source_generation,source_seq FROM r29_input_mirror WHERE singleton=1",null)){if(c.moveToFirst()){oldGen=c.getLong(0);oldSeq=c.getLong(1);}}
        if(gen==oldGen&&seq==oldSeq)return;
        boolean full=oldSeq<0||seq<=oldSeq;Set<String> changed=new LinkedHashSet<>();
        if(!full)try(Cursor c=root.rawQuery("SELECT issue FROM r29_event_outbox WHERE kind='DRAW_COMMITTED' AND seq>? AND seq<=? ORDER BY seq LIMIT 513",new String[]{String.valueOf(oldSeq),String.valueOf(seq)})){while(c.moveToNext())changed.add(c.getString(0));if(changed.size()>512)full=true;}
        MigrationCopyR29.Table draw=descriptor(root,"draws"),clock=descriptor(root,"su1_source_clock");if(draw==null)throw new IllegalStateException("R29_CANONICAL_MIRROR_SOURCE_MISSING");
        List<MigrationCopyR29.Row> captured=new ArrayList<>();
        if(full)captured.addAll(rows(root,draw,"1=1",null));else for(String issue:changed)captured.addAll(rows(root,draw,"issue=?",new String[]{issue}));
        String floor=null;try(Cursor c=root.rawQuery("SELECT min(issue) FROM draws",null)){if(c.moveToFirst()&&!c.isNull(0))floor=c.getString(0);}
        List<MigrationCopyR29.Row> sourceClock=clock==null?Collections.emptyList():rows(root,clock,"1=1",null);
        if(gen!=generation(root)||seq!=sequence(root))throw new IllegalStateException("CP3_DB_BUSY_R29_SOURCE_CHANGED_DURING_MIRROR_CAPTURE");
        final boolean replace=full;final String first=floor;final long expectedGen=oldGen,expectedSeq=oldSeq;
        ResearchRouteR29.write(store,"R29_CANONICAL_INPUT_MIRROR",db->{
            db.execSQL(DDL);long actualGen=-1,actualSeq=-1;try(Cursor c=db.rawQuery("SELECT source_generation,source_seq FROM r29_input_mirror WHERE singleton=1",null)){if(c.moveToFirst()){actualGen=c.getLong(0);actualSeq=c.getLong(1);}}
            if(actualGen!=expectedGen||actualSeq!=expectedSeq)throw new IllegalStateException("CP3_DB_BUSY_R29_STALE_MIRROR_CAPTURE");
            if(!table(db,"draws"))db.execSQL(draw.createSql);
            if(replace)db.execSQL("DELETE FROM draws");else for(String issue:changed)db.execSQL("DELETE FROM draws WHERE issue=?",new Object[]{issue});
            if(first!=null)db.execSQL("DELETE FROM draws WHERE issue<?",new Object[]{first});
            ResearchCutoverR29.restorePage(db,draw,captured);
            if(clock!=null){if(!table(db,"su1_source_clock"))db.execSQL(clock.createSql);db.execSQL("DELETE FROM su1_source_clock");ResearchCutoverR29.restorePage(db,clock,sourceClock);}
            db.execSQL("INSERT OR REPLACE INTO r29_input_mirror VALUES(1,?,?,?)",new Object[]{gen,seq,System.currentTimeMillis()});return 0;
        });
    }
}
