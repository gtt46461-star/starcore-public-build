"""HOST platform adapters for actual owned RuntimeR27 fixtures; no business implementation is replaced."""
from pathlib import Path
import os
import subprocess


def compile_owned_platform(root, output, classes, sdk):
    root, output, classes, sdk = map(Path, (root, output, classes, sdk))
    sources = output / "sources"
    platform = output / "platform"
    sources.mkdir(parents=True)
    platform.mkdir()
    files = []
    for original in sorted((root / "tools/r29/qual_host/shim").rglob("*.java")):
        target = sources / original.relative_to(root / "tools/r29/qual_host/shim")
        target.parent.mkdir(parents=True, exist_ok=True)
        text = original.read_text()
        if original.name == "SQLiteDatabase.java":
            at = text.index("    static final class Rows")
            text = text[:at] + '''
    private boolean successful;
    public Runnable beforeSuccessful;
    public static final int OPEN_READONLY=1,NO_LOCALIZED_COLLATORS=16;
    public interface CursorFactory {}
    public static SQLiteDatabase openDatabase(String path,CursorFactory factory,int flags){try{return new SQLiteDatabase(java.sql.DriverManager.getConnection("jdbc:sqlite:file:"+path+"?mode=ro"));}catch(java.sql.SQLException failure){throw new SQLiteException(failure.getMessage());}}
    public void beginTransaction(){try{jdbc.setAutoCommit(false);successful=false;}catch(java.sql.SQLException failure){throw new SQLiteException(failure.getMessage());}}
    public void setTransactionSuccessful(){if(beforeSuccessful!=null)beforeSuccessful.run();successful=true;}
    public void endTransaction(){try{if(successful)jdbc.commit();else jdbc.rollback();}catch(java.sql.SQLException failure){try{jdbc.rollback();}catch(java.sql.SQLException ignored){}throw new SQLiteException(failure.getMessage());}finally{successful=false;try{jdbc.setAutoCommit(true);}catch(java.sql.SQLException failure){throw new SQLiteException(failure.getMessage());}}}
    public String getPath(){try{return jdbc.getMetaData().getURL().substring("jdbc:sqlite:".length());}catch(java.sql.SQLException failure){throw new SQLiteException(failure.getMessage());}}
''' + text[at:]
        target.write_text(text)
        files.append(target)
    adapters = {
        "android/content/Context.java": 'package android.content;public class Context{public java.io.File getDatabasePath(String name){throw new IllegalStateException("HOST_PATH_REQUIRED");}public Context getApplicationContext(){return this;}}',
        "android/app/Application.java": 'package android.app;public class Application extends android.content.Context{private final java.io.File path;public Application(java.io.File path){this.path=path;}@Override public java.io.File getDatabasePath(String name){return path;}}',
        "android/os/SystemClock.java": 'package android.os;public final class SystemClock{public static long elapsedRealtime(){return System.nanoTime()/1000000L;}public static long uptimeMillis(){return elapsedRealtime();}public static long currentThreadTimeMillis(){return java.lang.management.ManagementFactory.getThreadMXBean().getCurrentThreadCpuTime()/1000000L;}}',
        "android/os/Debug.java": 'package android.os;public final class Debug{public static long threadCpuTimeNanos(){return java.lang.management.ManagementFactory.getThreadMXBean().getCurrentThreadCpuTime();}}',
        "android/os/Process.java": 'package android.os;public final class Process{public static final int THREAD_PRIORITY_BACKGROUND=10;public static void setThreadPriority(int priority){}}',
        "android/os/Looper.java": 'package android.os;public final class Looper{public static Looper myLooper(){return null;}public static Looper getMainLooper(){return null;}}',
        "android/util/Log.java": 'package android.util;public final class Log{public static int w(String t,String m){return 0;}public static int w(String t,String m,Throwable e){return 0;}public static int e(String t,String m){return 0;}public static int e(String t,String m,Throwable e){return 0;}public static int d(String t,String m){return 0;}public static int i(String t,String m){return 0;}}',
        "com/ethk3/novaonepro/DbHelper.java": 'package com.ethk3.novaonepro;import android.database.sqlite.SQLiteDatabase;public final class DbHelper{private final SQLiteDatabase db;public Runnable beforeWritable;public DbHelper(SQLiteDatabase db){this.db=db;}public SQLiteDatabase getReadableDatabase(){return db;}public SQLiteDatabase getWritableDatabase(){if(beforeWritable!=null)beforeWritable.run();return db;}public void close(){}}',
        "com/ethk3/novaonepro/DbStore.java": 'package com.ethk3.novaonepro;import android.database.sqlite.SQLiteDatabase;public final class DbStore{private final DatabaseCoordinator coordinator;private final SQLiteDatabase db;private final android.content.Context context;public DbStore(DatabaseCoordinator coordinator,SQLiteDatabase db,android.content.Context context){this.coordinator=coordinator;this.db=db;this.context=context;}public DatabaseCoordinator coordinatorForResearchR952(){return coordinator;}public SQLiteDatabase readable(){return db;}public android.content.Context contextForR26(){return context;}}',
    }
    for name, text in adapters.items():
        target = sources / name
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_text(text)
        files.append(target)
    subprocess.run(["javac", "-nowarn", "-encoding", "UTF-8", "-cp", os.pathsep.join(map(str, (sdk, classes))), "-d", str(platform), *map(str, files)], cwd=root, check=True)
    # Compile the exact current producer, including reviewed PIT DDL removal, ahead of old support classes.
    subprocess.run(["javac", "-nowarn", "-encoding", "UTF-8", "-cp", os.pathsep.join(map(str, (platform, classes, sdk))), "-d", str(platform), str(root / "app/src/main/java/com/ethk3/novaonepro/strategyv2/LiveShadowsR27.java")], cwd=root, check=True)
    return platform
