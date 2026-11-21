package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;

public final class ResearchViewRotationR335Host {
    private static void require(boolean ok,String message) {
        if (!ok) throw new AssertionError(message);
    }

    private static long count(SQLiteDatabase db,String table) {
        try (Cursor q=db.rawQuery("SELECT count(*) FROM "+table,null)) {
            require(q.moveToFirst(),table);
            return q.getLong(0);
        }
    }

    public static void main(String[] args) throws Exception {
        Class.forName("org.sqlite.JDBC");
        Path directory=Files.createTempDirectory("r335-view-cursor");
        Path file=directory.resolve("main.db");
        String group="OBSERVATORY";
        String lastInput;
        try (Connection connection=DriverManager.getConnection("jdbc:sqlite:"+file)) {
            SQLiteDatabase db=new SQLiteDatabase(connection);
            EvolutionPublicationProtocolR31Host.schema(db);
            try (R31OwnedRuntimeFixture owner=new R31OwnedRuntimeFixture(db,"r27-observatory-r1")) {
                owner.owned(()->{
                    ResearchObservatoryR30R1.ViewCursor first=ResearchObservatoryR30R1.ViewCursor.read(db,group,18);
                    require(first.index==0,"fresh scheduler");
                    long before=count(db,"r31_commit_receipt");
                    first.scan(owner.worker,null,1,null,null);
                    first.scan(owner.worker,null,2,"R01","stale");
                    ResearchObservatoryR30R1.ViewCursor second=ResearchObservatoryR30R1.ViewCursor.read(db,group,18);
                    require(second.index==1,"stale retry advanced twice");
                    second.scan(owner.worker,null,2,"R02","source absent");
                    ResearchObservatoryR30R1.ViewCursor third=ResearchObservatoryR30R1.ViewCursor.read(db,group,18);
                    require(third.index==2,"fresh scan stalled");
                    require(!second.snapshot("same source").equals(third.snapshot("same source")),"distinct cursor inputs");
                    require(count(db,"r31_commit_receipt")==before,"control scans grew the scientific receipt log");
                    require(count(db,"r27_chart_cache")==2,"cursor and one source diagnostic expected");
                    ResearchWriteQueueR27.Batch stale=new ResearchWriteQueueR27.Batch();
                    first.advance(stale,5,System.currentTimeMillis());
                    boolean rejected=false;
                    try {
                        RuntimeR27.submit(owner.worker,"TEST_STALE_VIEW_DATA",stale,false,first.snapshot("original"));
                    } catch (RuntimeException error) {
                        if (!ResearchStoreR30R1.stale(error)) throw error;
                        rejected=true;
                    }
                    require(rejected,"stale publication overwrote a later cursor");
                    return null;
                });
                lastInput=ResearchObservatoryR30R1.ViewCursor.read(db,group,18).snapshot("same source");
            }
        }
        try (Connection connection=DriverManager.getConnection("jdbc:sqlite:"+file)) {
            SQLiteDatabase reopened=new SQLiteDatabase(connection);
            ResearchObservatoryR30R1.ViewCursor cursor=ResearchObservatoryR30R1.ViewCursor.read(reopened,group,18);
            require(cursor.index==2 && lastInput.equals(cursor.snapshot("same source")),"restart lost cursor identity");
        }
        System.out.println("PASS bounded scan, repeated delivery, stale fenced product and reopened cursor");
    }
}
