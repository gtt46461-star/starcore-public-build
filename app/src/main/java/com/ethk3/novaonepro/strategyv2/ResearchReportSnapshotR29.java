package com.ethk3.novaonepro.strategyv2;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.StatFs;
import com.ethk3.novaonepro.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Merges two pinned COPIES into the private report snapshot, so both Java and native report readers see the same
 * authoritative research rows. It never attaches or writes a live database. Root realtime tables remain unchanged. */
public final class ResearchReportSnapshotR29 {
    private ResearchReportSnapshotR29(){}
    private static final String FRAME="CREATE TABLE IF NOT EXISTS r29_report_frame(singleton INTEGER PRIMARY KEY CHECK(singleton=1),frame_json TEXT NOT NULL)";
    public static void prepare(Context context,File realtimeSnapshot,File directory,long nativeBudget,ReportNativeCP4.Control control)throws Exception{
        if(realtimeSnapshot.getCanonicalPath().equals(context.getDatabasePath(AppConfig.DB_NAME).getCanonicalPath())||realtimeSnapshot.getCanonicalPath().equals(context.getDatabasePath(ResearchRouteR29.FILE_NAME).getCanonicalPath())||!realtimeSnapshot.getParentFile().getCanonicalFile().equals(directory.getCanonicalFile()))throw new IOException("REPORT_R29_PRIVATE_COPY_REQUIRED");
        File frame=new File(directory,"R29_SNAPSHOT_FRAME.json");
        try(SQLiteDatabase prior=SQLiteDatabase.openDatabase(realtimeSnapshot.getPath(),null,SQLiteDatabase.OPEN_READONLY)){
            try(Cursor exists=prior.rawQuery("SELECT 1 FROM sqlite_master WHERE type='table' AND name='r29_report_frame'",null)){
                if(exists.moveToFirst())try(Cursor c=prior.rawQuery("SELECT frame_json FROM r29_report_frame WHERE singleton=1",null)){if(c.moveToFirst()){write(frame,c.getString(0));return;}}
            }
            if(!ResearchRouteR29.active(prior))return;
        }
        File live=context.getDatabasePath(ResearchRouteR29.FILE_NAME),copy=new File(directory,"research-snapshot.db");
        if(!live.isFile()||new StatFs(directory.getPath()).getAvailableBytes()<2*live.length()+(64L<<20))throw new IOException("REPORT_R29_RESEARCH_DISK_BUDGET");
        for(String suffix:new String[]{"","-wal","-shm","-journal"}){File part=new File(copy.getPath()+suffix);if(part.exists()&&!part.delete())throw new IOException("REPORT_R29_RETRY_COPY_DELETE_FAILED");}
        ReportNativeCP4.backup(live,copy,nativeBudget,control);
        try(FileOutputStream out=new FileOutputStream(copy,true)){out.getFD().sync();}
        String researchHash=ReportArchiveCP4.hash(copy,control);Map<String,Object> evidence=new LinkedHashMap<>();
        try(SQLiteDatabase merged=SQLiteDatabase.openDatabase(realtimeSnapshot.getPath(),null,SQLiteDatabase.OPEN_READWRITE)){
            merged.execSQL("ATTACH DATABASE ? AS research",new Object[]{copy.getPath()});
            evidence.put("researchSha256",researchHash);evidence.put("researchBytes",copy.length());
            mergeAttachedCopies(merged,evidence,control);
        }
        try(FileOutputStream out=new FileOutputStream(realtimeSnapshot,true)){out.getFD().sync();}
        write(frame,JsonR27.write(evidence));
    }
    /** SQL merge over private copies only; also exercised against a real SQLite engine on HOST. */
    static void mergeAttachedCopies(SQLiteDatabase merged,Map<String,Object> evidence,ReportNativeCP4.Control control)throws Exception{
            long epoch=MigrationCopyR29.scalar(merged,"SELECT epoch FROM main.r29_database_route WHERE singleton=1",null);
            if(epoch!=MigrationCopyR29.scalar(merged,"SELECT epoch FROM research.r29_research_owner WHERE singleton=1 AND phase='READY'",null))throw new IOException("REPORT_RESEARCH_FRAME_RETRY");
            long received=0;try(Cursor c=merged.rawQuery("SELECT last_seq FROM research.r29_event_cursor WHERE source='REALTIME'",null)){if(c.moveToFirst())received=c.getLong(0);}
            long posted=MigrationCopyR29.scalar(merged,"SELECT coalesce(max(seq),0) FROM main.r29_event_outbox",null);
            String latest=RuntimeR27.latestIssue(merged);if(latest==null||received>posted)throw new IOException("REPORT_RESEARCH_FRAME_RETRY");
            long ordinal=com.ethk3.novaonepro.TargetIssueResolver.sequenceOrdinalSU1(latest);
            for(String[] cursor:new String[][]{{"r26_live_model","through_ordinal"},{"r26_legacy_runtime","through_ordinal"},{"r26_foundry","source_through"},{"r26_legacy_campaign","source_through"}})
                if(researchColumn(merged,cursor[0],cursor[1])&&MigrationCopyR29.scalar(merged,"SELECT coalesce(max("+MigrationCopyR29.id(cursor[1])+"),0) FROM research."+MigrationCopyR29.id(cursor[0]),null)>ordinal)throw new IOException("REPORT_RESEARCH_FRAME_RETRY");
            try(Cursor c=merged.rawQuery("SELECT max(trained_through) FROM research.r29_model_checkpoint",null)){if(c.moveToFirst()&&c.getString(0)!=null&&c.getString(0).compareTo(latest)>0)throw new IOException("REPORT_RESEARCH_FRAME_RETRY");}
            if(MigrationCopyR29.scalar(merged,"SELECT coalesce(max(target_ordinal),0) FROM research.r27_live_frozen",null)>ordinal+1)throw new IOException("REPORT_RESEARCH_FRAME_RETRY");
            try(Cursor c=merged.rawQuery("SELECT max(to_issue) FROM research.r27_replay_dataset",null)){if(c.moveToFirst()&&c.getString(0)!=null&&c.getString(0).compareTo(latest)>0)throw new IOException("REPORT_RESEARCH_FRAME_RETRY");}
            List<String[]> schema=new ArrayList<>();Set<String> owned=new HashSet<>();
            try(Cursor c=merged.rawQuery("SELECT name,sql FROM research.sqlite_master WHERE type='table' AND sql IS NOT NULL ORDER BY name",null)){
                while(c.moveToNext()){String name=c.getString(0);if(MigrationCopyR29.ownedResearch(name)||name.equals("r27_chart_cache")||name.equals("r27_fetch_reject")){owned.add(name);schema.add(new String[]{name,c.getString(1)});}}
            }
            List<String> triggers=new ArrayList<>();
            try(Cursor c=merged.rawQuery("SELECT name,tbl_name FROM main.sqlite_master WHERE type='trigger'",null)){while(c.moveToNext())if(owned.contains(c.getString(1)))triggers.add(c.getString(0));}
            evidence.put("schema","R29_REPORT_PAIRED_SNAPSHOT_2");evidence.put("epoch",epoch);evidence.put("researchReceivedThrough",received);evidence.put("realtimePostedThrough",posted);evidence.put("realtimeAsOfIssue",latest);evidence.put("scope","All migrated R26/R27 research rows come from the research copy; realtime draw, terminal feedback, formal freeze and decision authority remain in the realtime copy; newer research work is separate");
            merged.execSQL("PRAGMA foreign_keys=OFF");merged.beginTransactionNonExclusive();
            try{
                // These DROP/DELETE operations affect only the job-owned snapshot copy, never either live database.
                for(String trigger:triggers)merged.execSQL("DROP TRIGGER "+MigrationCopyR29.id(trigger));
                for(String[] table:schema){
                    if(!control.shouldContinue())throw new IOException("REPORT_USER_CANCEL");
                    String name=MigrationCopyR29.id(table[0]);
                    // Recreate the private-copy table so a newer research schema cannot be merged into an
                    // older Root schema with missing columns. Live files and their old tables are untouched.
                    merged.execSQL("DROP TABLE IF EXISTS main."+name);merged.execSQL(table[1]);
                    copyTable(merged,table[0],table[1],control);
                }
                ResearchReportAppStateR29.mergeCopies(merged,evidence,control);
                merged.execSQL(FRAME);merged.execSQL("INSERT INTO r29_report_frame VALUES(1,?)",new Object[]{JsonR27.write(evidence)});
                merged.setTransactionSuccessful();
            }finally{merged.endTransaction();}
            try(Cursor c=merged.rawQuery("PRAGMA integrity_check",null)){if(!c.moveToFirst()||!"ok".equals(c.getString(0)))throw new IOException("REPORT_R29_MERGED_SNAPSHOT_INTEGRITY");}
    }
    private static boolean researchColumn(SQLiteDatabase merged,String table,String column){try(Cursor c=merged.rawQuery("PRAGMA research.table_info("+MigrationCopyR29.id(table)+")",null)){while(c.moveToNext())if(column.equals(c.getString(1)))return true;return false;}}
    /** Both source files are pinned job copies, so bounded OFFSET pages have a fixed row set. WITHOUT ROWID
     * tables are ordered by their declared composite key; their absence of a rowid is never guessed away. */
    static void copyTable(SQLiteDatabase merged,String table,String ddl,ReportNativeCP4.Control control)throws Exception{
        String name=MigrationCopyR29.id(table);boolean withoutRowid=ddl.toUpperCase(java.util.Locale.ROOT).contains("WITHOUT ROWID");
        List<String> columns=new ArrayList<>();SortedMap<Integer,String> primary=new TreeMap<>();
        try(Cursor c=merged.rawQuery("PRAGMA research.table_info("+name+")",null)){while(c.moveToNext()){String column=MigrationCopyR29.id(c.getString(1));columns.add(column);if(c.getInt(5)>0)primary.put(c.getInt(5),column);}}
        if(columns.isEmpty()||withoutRowid&&primary.isEmpty())throw new IOException("REPORT_R29_TABLE_KEY_REQUIRED:"+table);
        if(!withoutRowid)columns.add(0,"rowid");String projection=String.join(",",columns),order=withoutRowid?String.join(",",primary.values()):"rowid";
        for(long offset=0;;offset+=128){
            if(!control.shouldContinue())throw new IOException("REPORT_USER_CANCEL");
            merged.execSQL("INSERT INTO main."+name+"("+projection+") SELECT "+projection+" FROM research."+name+" ORDER BY "+order+" LIMIT 128 OFFSET ?",new Object[]{offset});
            if(MigrationCopyR29.scalar(merged,"SELECT changes()",null)<128)break;
        }
    }
    private static void write(File file,String text)throws IOException{try(FileOutputStream out=new FileOutputStream(file)){out.write(text.getBytes(StandardCharsets.UTF_8));out.getFD().sync();}}
}
