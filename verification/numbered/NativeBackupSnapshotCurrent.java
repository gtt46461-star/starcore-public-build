package com.ethk3.novaonepro;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.io.File;

/** Real WAL writes interleaved with production backup, including cancellation after copying. */
public final class NativeBackupSnapshotCurrent {
    private static int assertions;
    private static void require(boolean condition,String message) {
        assertions++;
        if(!condition) throw new AssertionError(message);
    }
    private static long scalar(SQLiteDatabase db,String sql) {
        try(Cursor c=db.rawQuery(sql,null)) {
            require(c.moveToFirst(),"scalar missing");
            return c.getLong(0);
        }
    }
    public static void main(String[] args) throws Exception {
        File source=new File(args[1]), directory=new File(args[2]);
        if(!directory.mkdir()) throw new AssertionError("fresh output directory required");
        ReportNativeCP4.loadForDiagnosticProcess("host:diagnostics","host");
        SQLiteDatabase writer=new SQLiteDatabase(args[0],source.getPath());
        try {
            require(scalar(writer,"SELECT revision FROM marker_a")==0,"fresh fixture required");
            final long[] progress={Long.MAX_VALUE,0};
            File snapshot=new File(directory,"consistent.db");
            ReportNativeCP4.backup(source,snapshot,4L<<20,new ReportNativeCP4.Control() {
                public boolean shouldContinue(){return true;}
                public void onBackupProgress(long total,long remaining,int code) {
                    require(remaining<progress[0],"backup restarted under WAL writer");
                    progress[0]=remaining;
                    progress[1]++;
                    writer.beginTransaction();
                    try {
                        writer.execSQL("UPDATE marker_a SET revision=revision+1");
                        writer.execSQL("UPDATE marker_b SET revision=revision+1");
                        writer.setTransactionSuccessful();
                    } finally {writer.endTransaction();}
                }
            });
            require(progress[1]>3,"multiple live commits required");
            require(scalar(writer,"SELECT revision FROM marker_a")==progress[1],"writer did not progress");
            SQLiteDatabase copy=new SQLiteDatabase(args[0],snapshot.getPath());
            try {
                require(scalar(copy,"SELECT revision FROM marker_a")==0,"snapshot includes later WAL commit");
                require(scalar(copy,"SELECT revision FROM marker_b")==0,"cross-table snapshot mismatch");
                try(Cursor c=copy.rawQuery("PRAGMA journal_mode",null)) {
                    require(c.moveToFirst() && "delete".equalsIgnoreCase(c.getString(0)),"copied database not normalized");
                }
            } finally {copy.shutdown();}
            final boolean[] copied={false};
            File cancelled=new File(directory,"cancelled.db");
            try {
                ReportNativeCP4.backup(source,cancelled,4L<<20,new ReportNativeCP4.Control() {
                    public boolean shouldContinue(){return !copied[0];}
                    public void onBackupProgress(long total,long remaining,int code){if(remaining==0)copied[0]=true;}
                });
                throw new AssertionError("late cancellation accepted");
            } catch(ReportNativeCP4.Failure expected) {
                require(expected.nativeCode==-1001,"typed cancellation required");
            }
            require(copied[0],"copy-complete boundary not reached");
            require(!cancelled.exists(),"cancelled copy retained");
            ReportNativeCP4.backup(source,cancelled,4L<<20,()->true);
            require(cancelled.isFile(),"fresh retry failed after cancellation");
            System.out.println("PASS|assertions="+assertions+"|liveWalCommits="+progress[1]+"|real native backup, host SQLite bridge; device lifecycle unverified");
        } finally {writer.shutdown();}
    }
}
