package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.TargetIssueResolver;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/** Restartable preparation owned exclusively by the existing replay dataset task. */
final class ReplayDatasetBuildR341 {
    private static final String PANEL="r341-replay-dataset-build";
    private static final String HEAD="active", LAST_RESTART="last-restart";
    private static final int PAGE=512,MAX_PAGES=12;
    final String raw,first,last,cursor,phase;
    final ResearchSourceJournalR31.DatasetRead source;
    final int pages,rows;

    private ReplayDatasetBuildR341(String raw,String first,String last,String cursor,String phase,
            ResearchSourceJournalR31.DatasetRead source,int pages,int rows){
        this.raw=raw;this.first=first;this.last=last;this.cursor=cursor;this.phase=phase;
        this.source=source;this.pages=pages;this.rows=rows;
    }

    private static String slot(int page){return String.format(Locale.ROOT,"page-%02d",page);}
    private static String value(SQLiteDatabase db,String key){
        try(Cursor c=db.rawQuery("SELECT payload_json FROM r27_chart_cache WHERE panel_id=? AND key=?",new String[]{PANEL,key})){
            return c.moveToFirst()?c.getString(0):null;
        }
    }
    static ReplayDatasetBuildR341 read(SQLiteDatabase db){
        String raw=value(db,HEAD);
        if(raw==null)return null;
        Object json=JsonR27.parse(raw);
        if(!(json instanceof Map))throw new IllegalStateException("R341_REPLAY_STAGE_HEAD_TYPE");
        Map<?,?> map=(Map<?,?>)json;
        String first=(String)map.get("first"),last=(String)map.get("last"),cursor=(String)map.get("cursor"),phase=(String)map.get("phase");
        long revision=((Number)map.get("revision")).longValue(),event=((Number)map.get("sourceEvent")).longValue();
        int pages=((Number)map.get("pages")).intValue(),rows=((Number)map.get("rows")).intValue();
        if(!TargetIssueResolver.isValid(first)||!TargetIssueResolver.isValid(last)||first.compareTo(last)>0
                ||!TargetIssueResolver.isValid(cursor)||revision<0||event<0||pages<0||pages>MAX_PAGES
                ||rows<1||rows>RuntimeR27.DATASET_ROWS||pages==0
                ||!("HOT".equals(phase)||"IMPORT".equals(phase)||"COMPLETE".equals(phase)))
            throw new IllegalStateException("R341_REPLAY_STAGE_HEAD_IDENTITY");
        return new ReplayDatasetBuildR341(raw,first,last,cursor,phase,new ResearchSourceJournalR31.DatasetRead(revision,event),pages,rows);
    }
    static ReplayDatasetBuildR341 start(SQLiteDatabase db,String newest){
        ResearchSourceJournalR31.DatasetRead source=ResearchSourceJournalR31.DatasetRead.capture(db);
        try(Cursor c=db.rawQuery("SELECT min(issue) FROM (SELECT issue FROM draws WHERE dirty=0 AND issue<=? ORDER BY issue DESC LIMIT ?)" ,
                new String[]{newest,Integer.toString(RuntimeR27.DATASET_ROWS)})){
            if(!c.moveToFirst()||c.isNull(0))return null;
            return new ReplayDatasetBuildR341(null,c.getString(0),newest,"","HOT",source,0,0);
        }
    }
    boolean complete(){return "COMPLETE".equals(phase);}
    boolean currentSource(SQLiteDatabase db){
        String low=cursor.isEmpty()||cursor.compareTo(first)>=0?first:cursor;
        return source.valid(db,TargetIssueResolver.sequenceOrdinalSU1(low),TargetIssueResolver.sequenceOrdinalSU1(last));
    }
    private void guard(ResearchWriteQueueR27.Batch batch){
        batch.guardBefore(db->Objects.equals(raw,value(db,HEAD))?0:-1);
    }
    private Map<String,Object> next(String issue,String nextPhase,int nextPages,int nextRows){
        Map<String,Object> state=new LinkedHashMap<>();
        state.put("first",first);state.put("last",last);state.put("cursor",issue);state.put("phase",nextPhase);
        state.put("revision",source.revision);state.put("sourceEvent",source.event);
        state.put("pages",nextPages);state.put("rows",nextRows);
        return state;
    }
    private static void cache(ResearchWriteQueueR27.Batch batch,String slot,Object data,long wall){
        batch.add("INSERT OR REPLACE INTO r27_chart_cache VALUES(?,?,?,?)",PANEL,slot,JsonR27.write(data),wall);
    }
    void reset(ResearchWriteQueueR27.Batch batch,String reason,long wall){
        guard(batch);
        Map<String,Object> failure=next(cursor,phase,pages,rows);
        failure.put("reason",reason);failure.put("preservedHead",raw);
        cache(batch,LAST_RESTART,failure,wall);
        clear(batch);
    }
    void retire(ResearchWriteQueueR27.Batch batch){
        guard(batch);
        clear(batch);
    }
    private void clear(ResearchWriteQueueR27.Batch b){
        b.add("DELETE FROM r27_chart_cache WHERE panel_id=? AND key<>?",PANEL,LAST_RESTART);
    }
    int preparePage(SQLiteDatabase db,ResearchWriteQueueR27.Batch batch,long now){
        if(complete()||pages>=MAX_PAGES)throw new IllegalStateException("R341_REPLAY_STAGE_PAGE_BOUND");
        int limit=Math.min(PAGE,RuntimeR27.DATASET_ROWS-rows);
        ReplayDatasetR27 page;
        if("HOT".equals(phase))page=StoreR27.loadDatasetPageR341(db,limit,last,first,cursor);
        else{
            List<ReplayDatasetR27.Row> previous=new ArrayList<>(limit);
            HistoryImportR27.appendOlder(db,previous,cursor,limit);
            page=ReplayDatasetR27.of(previous);
        }
        if(page.rejectedRows!=0)throw new IllegalStateException("R341_REPLAY_STAGE_REJECTED_SOURCE_ROW");
        String low=page.size()==0?cursor:page.issues[0];
        String afterPhase=phase;
        if("HOT".equals(phase)&&first.equals(low))afterPhase="IMPORT";
        if(("IMPORT".equals(phase)&&page.size()<limit)||rows+page.size()==RuntimeR27.DATASET_ROWS)afterPhase="COMPLETE";
        if("HOT".equals(phase)&&page.size()==0)throw new ResearchStoreR30R1.StaleResearch();
        if(page.size()>0&&(!cursor.isEmpty()&&page.issues[page.size()-1].compareTo(cursor)>=0
                ||cursor.isEmpty()&&!last.equals(page.issues[page.size()-1])))throw new ResearchStoreR30R1.StaleResearch();
        guard(batch);
        long from=TargetIssueResolver.sequenceOrdinalSU1(low.compareTo(first)<0?low:first),through=TargetIssueResolver.sequenceOrdinalSU1(last);
        batch.guardBefore(check->source.valid(check,from,through)?0:-1);
        int totalPages=pages;
        if(page.size()>0){
            byte[] original=page.originalBytesR31();
            Map<String,Object> stored=new LinkedHashMap<>();
            stored.put("revision",source.revision);stored.put("sourceEvent",source.event);
            stored.put("from",page.issues[0]);stored.put("through",page.issues[page.size()-1]);stored.put("rows",page.size());
            stored.put("dataset",page.datasetHash);stored.put("sha256",StatsR27.sha256(original));
            stored.put("bytes",Base64.getEncoder().encodeToString(original));
            cache(batch,slot(pages),stored,now);totalPages++;
        }
        cache(batch,HEAD,next(low,afterPhase,totalPages,rows+page.size()),now);
        return page.size();
    }
    ReplayDatasetR27 assemble(SQLiteDatabase db){
        if(!complete()||!currentSource(db))throw new ResearchStoreR30R1.StaleResearch();
        List<ReplayDatasetR27.Row> combined=new ArrayList<>(rows);
        String previous=null;
        for(int pageIndex=0;pageIndex<pages;pageIndex++){
            ExecutionBudgetR26.poll();
            String record=value(db,slot(pageIndex));
            if(record==null||record.length()>65536)throw new IllegalStateException("R341_REPLAY_STAGE_PAGE_MISSING_OR_OVERSIZE");
            Map<?,?> page=(Map<?,?>)JsonR27.parse(record);
            byte[] bytes=Base64.getDecoder().decode((String)page.get("bytes"));
            if(((Number)page.get("revision")).longValue()!=source.revision||((Number)page.get("sourceEvent")).longValue()!=source.event
                    ||!StatsR27.sha256(bytes).equals(page.get("sha256")))throw new IllegalStateException("R341_REPLAY_STAGE_PAGE_CHECKSUM");
            ReplayDatasetR27 data=ReplayDatasetR27.originalBytesR31(bytes);
            String high=data.issues[data.size()-1];
            if(data.size()!=((Number)page.get("rows")).intValue()||data.size()>PAGE||!data.datasetHash.equals(page.get("dataset"))
                    ||!data.issues[0].equals(page.get("from"))||!high.equals(page.get("through"))
                    ||(previous==null?!last.equals(high):high.compareTo(previous)>=0))throw new IllegalStateException("R341_REPLAY_STAGE_PAGE_BINDING");
            for(int row=data.size()-1;row>=0;row--)combined.add(data.rowR31(row));
            previous=data.issues[0];
        }
        ReplayDatasetR27 result=ReplayDatasetR27.of(combined);
        if(result.size()!=rows||!cursor.equals(result.issues[0])||!last.equals(result.issues[rows-1])||!currentSource(db))
            throw new ResearchStoreR30R1.StaleResearch();
        return result;
    }
}
