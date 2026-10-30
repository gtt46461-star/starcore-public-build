package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.TargetIssueResolver;
import com.ethk3.novaonepro.TimeSourceR811;

/** Immutable predraw source/clock fence. No computation or write is performed while capturing it. */
public final class IssueCommitR29 {
    public static final long MARGIN_MS=5000,SKEW_MS=2000;
    private IssueCommitR29(){}
    static final class Head{
        String issue,hash;long open,seq;
    }
    static Head head(SQLiteDatabase realtime){
        Head h=new Head();
        try(Cursor c=realtime.rawQuery("SELECT issue,n1,n2,n3,n4,n5,n6,n7,n8,n9,n10,open_time FROM draws WHERE dirty=0 ORDER BY issue DESC LIMIT 1",null)){
            if(!c.moveToFirst())throw new Rejected("NO_CANONICAL_SOURCE");h.issue=c.getString(0);int[] numbers=new int[10];for(int i=0;i<10;i++)numbers[i]=c.getInt(i+1);h.open=c.getLong(11);
            h.hash=ResearchEventBridgeR29.Event.draw(0,h.issue,numbers,"FRAME_CAPTURE",0).hash;
        }
        h.seq=MigrationCopyR29.scalar(realtime,"SELECT coalesce(max(seq),0) FROM r29_event_outbox WHERE kind='DRAW_COMMITTED'",null);return h;
    }
    public static final class Rejected extends IllegalStateException{public final String reason;Rejected(String reason){super("R29_ISSUE_COMMIT_REJECTED:"+reason);this.reason=reason;}}
    static final class Fence{
        final String inputIssue,inputHash,session;final long inputOrdinal,sourceSeq,sourceOpen,deadlineWall,deadlineMono,preparedWall,preparedMono;
        Fence(Head head,String expected,long ordinal,long deadline,long wall,long mono,String session){
            if(!head.issue.equals(expected)||TargetIssueResolver.sequenceOrdinalSU1(expected)!=ordinal)throw new Rejected("SOURCE_CHANGED_BEFORE_PREPARATION");
            if(deadline<=wall+MARGIN_MS||head.open<=0||deadline!=head.open+60_000L)throw new Rejected("DEADLINE_OR_SOURCE_SCHEDULE");
            inputIssue=expected;inputOrdinal=ordinal;inputHash=head.hash;sourceSeq=head.seq;sourceOpen=head.open;deadlineWall=deadline;deadlineMono=mono+(deadline-wall);preparedWall=wall;preparedMono=mono;this.session=session;
        }
        void clock(long wall,long mono){
            if(wall<preparedWall-SKEW_MS||mono<preparedMono||Math.abs((wall-preparedWall)-(mono-preparedMono))>SKEW_MS)throw new Rejected("CLOCK_DISCONTINUITY");
            if(wall>=deadlineWall-MARGIN_MS||mono>=deadlineMono-MARGIN_MS)throw new Rejected("DEADLINE_MARGIN");
        }
        void verify(SQLiteDatabase realtime,long wall,long mono){
            clock(wall,mono);Head current=head(realtime);
            if(!inputIssue.equals(current.issue)||!inputHash.equals(current.hash)||sourceSeq!=current.seq||sourceOpen!=current.open)throw new Rejected("SOURCE_CHANGED_OR_RESULT_ALREADY_KNOWN");
        }
    }
    static Fence capture(SQLiteDatabase realtime,String issue,long ordinal,long deadline,String session){return new Fence(head(realtime),issue,ordinal,deadline,System.currentTimeMillis(),TimeSourceR811.monoNowMs(),session);}
    static Rejected rejected(Throwable error){for(Throwable c=error;c!=null;c=c.getCause())if(c instanceof Rejected)return (Rejected)c;return null;}
}
