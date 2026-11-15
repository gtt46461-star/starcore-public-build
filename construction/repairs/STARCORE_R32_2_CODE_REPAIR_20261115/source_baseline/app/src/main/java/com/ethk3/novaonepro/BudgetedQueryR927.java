package com.ethk3.novaonepro;

import android.database.Cursor;
import android.database.CursorWrapper;
import android.database.sqlite.SQLiteDatabase;
import android.os.CancellationSignal;
import android.os.OperationCanceledException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/** Cancels SQLite itself, including lazy cursor fill; a loop deadline cannot stop blocked SQL. */
public final class BudgetedQueryR927 {
    private static final ScheduledExecutorService TIMER=Executors.newSingleThreadScheduledExecutor(new ThreadFactory(){
        public Thread newThread(Runnable r){Thread t=new Thread(r,"r927-sql-deadline");t.setDaemon(true);return t;}
    });
    private BudgetedQueryR927(){}
    public static Cursor query(SQLiteDatabase db,String sql,String[] args){return query(db,sql,args,1000L);}
    public static Cursor query(SQLiteDatabase db,String sql,String[] args,long limitMs){
        RuntimeResourceAuditR927.checkpoint("BEFORE_SQL");
        ResearchWorkBudgetR927.checkpoint();
        if (RuntimeResourceAuditR927.remainingMs()==Long.MAX_VALUE && ResearchWorkBudgetR927.remainingMs()==Long.MAX_VALUE) return db.rawQuery(sql,args);
        final CancellationSignal signal=new CancellationSignal();
        long research=ResearchWorkBudgetR927.remainingMs();
        final boolean researchQuery=research!=Long.MAX_VALUE;
        // Respect the caller's SQL limit and BOTH enclosing scopes. A hidden 100ms cap
        // made otherwise valid 350/850ms research stages restart the same query forever.
        long budget=queryBudgetMs(limitMs,RuntimeResourceAuditR927.remainingMs(),research);
        final ScheduledFuture<?> timeout=TIMER.schedule(new Runnable(){public void run(){signal.cancel();}},budget,TimeUnit.MILLISECONDS);
        try {
            final Cursor cursor=db.rawQuery(sql,args,signal);
            return new CursorWrapper(cursor){
                private boolean closed;
                @Override public boolean moveToFirst(){try{return super.moveToFirst();}catch(OperationCanceledException e){throw cancellation(signal,e,researchQuery);}}
                @Override public boolean moveToNext(){try{return super.moveToNext();}catch(OperationCanceledException e){throw cancellation(signal,e,researchQuery);}}
                @Override public boolean moveToPosition(int position){try{return super.moveToPosition(position);}catch(OperationCanceledException e){throw cancellation(signal,e,researchQuery);}}
                @Override public int getCount(){try{return super.getCount();}catch(OperationCanceledException e){throw cancellation(signal,e,researchQuery);}}
                @Override public void close(){if(closed)return;closed=true;timeout.cancel(false);super.close();}
            };
        }catch(OperationCanceledException error){timeout.cancel(false);throw cancellation(signal,error,researchQuery);}
        catch(RuntimeException error){timeout.cancel(false);throw error;}
    }
    static long queryBudgetMs(long requested,long realtime,long research){
        return Math.max(1L,Math.min(Math.max(1L,requested),Math.min(realtime,research)));
    }
    private static RuntimeException cancellation(CancellationSignal signal,OperationCanceledException error,boolean researchQuery){
        // Only our own deadline cancellation is a cooperative yield. External cancellation
        // keeps its original failure identity; neither path marks a training job completed.
        if(!researchQuery||!signal.isCanceled())return error;
        ResearchWorkBudgetR927.Yield yielded=new ResearchWorkBudgetR927.Yield("SQL_DEADLINE_EXHAUSTED");
        yielded.initCause(error);return yielded;
    }

public static Cursor querySnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, SQLiteDatabase db,String sql,String[] args){return querySnapshotCP4(report, db,sql,args,1000L);}

public static Cursor querySnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, SQLiteDatabase db,String sql,String[] args,long limitMs){
        // The diagnostics owner owns its cancellation/deadline. Main-process model/research budgets are not consulted.
        if(db!=report.snapshotReadOnly())throw new IllegalArgumentException("REPORT_QUERY_SNAPSHOT_IDENTITY");
        return report.select(sql,args);
    }

public static RuntimeException cancellationSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, CancellationSignal signal,OperationCanceledException error,boolean researchQuery){
        // Only our own deadline cancellation is a cooperative yield. External cancellation
        // keeps its original failure identity; neither path marks a training job completed.
        if(!researchQuery||!signal.isCanceled())return error;
        ResearchWorkBudgetR927.Yield yielded=new ResearchWorkBudgetR927.Yield("SQL_DEADLINE_EXHAUSTED");
        yielded.initCause(error);return yielded;
    }

static long queryBudgetMsSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, long requested,long realtime,long research){
        return Math.max(1L,Math.min(Math.max(1L,requested),Math.min(realtime,research)));
    }
}
