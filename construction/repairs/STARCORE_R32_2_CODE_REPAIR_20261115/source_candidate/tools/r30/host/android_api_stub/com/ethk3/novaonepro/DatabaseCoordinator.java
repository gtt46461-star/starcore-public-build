package com.ethk3.novaonepro; /** HOST compile stub: signatures verified against CP06 DatabaseCoordinator.java:85-95,309,773. */
public class DatabaseCoordinator { public interface WriteCallable<T>{T call(android.database.sqlite.SQLiteDatabase db) throws Exception;}
 public interface WriteRunnable{void run(android.database.sqlite.SQLiteDatabase db) throws Exception;}
 public android.database.sqlite.SQLiteDatabase readable(){return null;} public <T> T runAtomicWriteOrThrowF01(String op, WriteCallable<T> t){return null;} }
